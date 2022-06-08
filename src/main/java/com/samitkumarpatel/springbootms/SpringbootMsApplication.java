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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.util.function.Predicate;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@SpringBootApplication
public class SpringbootMsApplication {
	@Value("${web.users.URL}")
	private String userWebURL;

	@Bean
	public WebClient webClient() {
		//TODO can this hardcoded URL be picked from a properties file?
		return WebClient.builder().baseUrl(userWebURL).build();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringbootMsApplication.class, args);
	}
}

@Configuration
class Router {
	@Bean
	public RouterFunction<ServerResponse> route(UserHandler userHandler) {
		return RouterFunctions.route(GET("/user"), userHandler::bank);
	}
}

@Component
@Slf4j
@RequiredArgsConstructor
class UserHandler {
	private final UserService userService;

	public Mono<ServerResponse> bank(ServerRequest request) {
		var page = request.queryParam("page").orElse("1");
		return ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(userService.getAllUser(), User.class);
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

@Service
@RequiredArgsConstructor
class UserService {
	private final WebClient webClient;
	public Flux<User> getAllUser() {
		return getUsersFromWeb()
				.index()
				.map(tuple -> mappedSlNo(tuple));
	}

	//TODO can this be moved to a mapper ?
	private User mappedSlNo(Tuple2<Long, User> tuple) {
		User u = tuple.getT2();
		u.setSlNo(tuple.getT1().intValue());
		return u;
	}

	private Flux<User> getUsersFromWeb() {
		return webClient
				.get()
				.uri("/users")
				.retrieve()
				.bodyToFlux(User.class)
				//.onErrorReturn(User.builder().build())
		;
	}
}