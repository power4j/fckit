package com.power4j.fist.cloud.security;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
class DefaultAccessDeniedHandlerTest {

	@Test
	public void handleAccessDenied() {
		DefaultAccessDeniedHandler handler = new DefaultAccessDeniedHandler();
		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Mono<Void> result = handler.handleAccessDenied(exchange, AuthProblem.INTERNAL_ACCESS_DENIED);
		StepVerifier.create(result).verifyComplete();
		assertThat(exchange.getResponse().isCommitted()).isTrue();
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

}