package com.samitkumarpatel.springbootms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.List;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@SpringBootApplication
public class SpringbootMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootMsApplication.class, args);
	}

}

@Configuration
@Slf4j
class PersonRouter {
	@Bean
	public RouterFunction<ServerResponse> route(PersonHandler handler) {
		return RouterFunctions.route(GET("/person"), handler::allPerson)
				.andRoute(POST("/person"),handler::createPerson);
	}
}

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
class Person {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	private int age;
}
@Configuration
class PersonHandler {

	private PersonService personService;

	PersonHandler(PersonService personService) {
		this.personService = personService;
	}
	Mono<ServerResponse> createPerson(ServerRequest request) {
		return ServerResponse
				.ok()
				.body(
						request.bodyToMono(Person.class)
								.flatMap(person -> personService.create(person)), Person.class);
	}
	Mono<ServerResponse> allPerson(ServerRequest request) {

		return ServerResponse
				.ok()
				.body(Mono.just(personService.allPerson(
						PageRequest.of(Integer.parseInt(request.queryParam("pageNo").orElse("0")),
								Integer.parseInt(request.queryParam("size").orElse("5"))))
				),Person.class);
	}
}

@Service
class PersonService {
	private PersonRepository personRepository;

	@Autowired
	PersonService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	Page<Person> allPerson(Pageable pageable) {
		return personRepository.findAll(pageable);
	}

	public Mono<Person> create(Person person) {
		return Mono.just(personRepository.save(person));
	}
}

@Repository
interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

}

