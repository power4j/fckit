package com.power4j.fist.autoconfigure.gateway;

import com.power4j.fist.cloud.gateway.ApiGuardFilter;
import com.power4j.fist.cloud.gateway.filter.RequestIdGlobalFilter;
import com.power4j.fist.cloud.security.oauth2.client.UserIntrospectClient;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.service.reactive.ReactivePermissionDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RouteGuardConfigurationStartupTest.TestApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.main.web-application-type=reactive")
class RouteGuardConfigurationStartupTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldStartReactiveGatewayContextWithRouteGuardConfiguration() {
		assertThat(this.port).isPositive();
		assertThat(this.applicationContext.getBeansOfType(ApiGuardFilter.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(RequestIdGlobalFilter.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(RoutePredicateHandlerMapping.class)).hasSize(1);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(RouteGuardConfiguration.class)
	static class TestApplication {

		@Bean
		ReactivePermissionDefinitionService<PermissionDefinition> reactivePermissionDefinitionService() {
			return (serviceName, method) -> Mono.just(Collections.emptyList());
		}

		@Bean
		UserIntrospectClient userIntrospectClient() {
			return new UserIntrospectClient((token) -> Mono.empty());
		}

	}

}
