package com.samitkumarpatel.springbootms;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Arrays;
import java.util.List;

import static com.samitkumarpatel.springbootms.Utils.ALL;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@SpringBootApplication
@Slf4j
public class SpringbootMsApplication {
	@Value("${web.users.URL}")
	private String userWebURL;

	@Bean
	public WebClient webClient() {
		//TODO can this hardcoded URL be picked from a properties file?
		return WebClient
				.builder()
				.baseUrl(userWebURL)
				.filter((clientRequest, nextFilter) -> {
					log.info("WebClient filter invoked METHOD: {}, URI: {}",clientRequest.method(), clientRequest.url());
					return nextFilter.exchange(clientRequest);
				})
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringbootMsApplication.class, args);
	}
}

@Configuration
class Router {
	@Bean
	public RouterFunction<ServerResponse> route(UserHandler userHandler) {
		return RouterFunctions.route(GET("/user"), userHandler::getUsers);
	}
}

interface UserHandler {
	public Mono<ServerResponse> getUsers(ServerRequest request);
}

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.environment", havingValue = "ao")
class AoClusterUserHandler implements UserHandler {
	private final UserService userService;

	@Override
	public Mono<ServerResponse> getUsers(ServerRequest request) {
		var page = request.queryParam("page").orElse("1");
		var filterWith = request.queryParam("filterWith").orElse(ALL);

		return ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(userService.getAllUser(page,filterWith), User.class);
	}
}

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.environment", havingValue = "az")
class AzClusterUserHandler implements UserHandler {

	private List<User> getUs() {
		return Arrays.asList(
				User.builder().id("1").name("u1").build(),
				User.builder().id("2").name("u2").build()
		);
	}
	@Override
	public Mono<ServerResponse> getUsers(ServerRequest request) {
		return ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(Flux.fromIterable(getUs()),User.class);
	}
}


@Data @Builder @AllArgsConstructor @RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class User {
	private int slNo;
	private String id;
	private String name;
	private String email;
	private String gender;
	private String status;
}

class Utils {
	public static String ALL = "all";
	public static String ACTIVE = "active";
	public static String INACTIVE = "inactive";
}
@Service
@RequiredArgsConstructor
class UserService {

	private final WebClient webClient;
	public Flux<User> getAllUser(String page, String filterWith) {
		return getUsersFromWeb(page)
				.index()
				.map(tuple -> mappedSlNo(tuple))
				.filterWhen(u -> Mono.just(ALL.equals(filterWith) ? true : filterWith.equals(u.getStatus())));
				//.filter(u -> ALL.equals(filterWith) ? true : filterWith.equals(u.getStatus()));
	}

	private User mappedSlNo(Tuple2<Long, User> tuple) {
		User u = tuple.getT2();
		u.setSlNo(tuple.getT1().intValue());
		return u;
	}

	private Flux<User> getUsersFromWeb(String pageNumber) {
		return webClient
				.get()
				.uri( uriBuilder -> uriBuilder.path("/users").queryParam("page",pageNumber).build())
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.retrieve()
				.bodyToFlux(User.class)
				.onErrorReturn(User.builder().build())
		;
	}
}