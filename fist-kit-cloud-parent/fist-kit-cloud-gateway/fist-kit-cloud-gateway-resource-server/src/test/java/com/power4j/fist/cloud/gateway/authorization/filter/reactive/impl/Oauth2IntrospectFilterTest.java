package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.oauth2.ServerBearerTokenResolver;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.security.oauth2.client.UserIntrospectClient;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class Oauth2IntrospectFilterTest {

	final String requestUri = "http://localhost/iam/demo";

	private MockServerHttpRequest request = MockServerHttpRequest.get(requestUri).build();

	private ServerWebExchange exchange = MockServerWebExchange.from(request);

	@Mock
	private ServerAuthFilterChain<AuthContext> authFilterChain;

	@Spy
	private AuthContext authContext = new AuthContext(exchange, null);

	@Mock
	private UserIntrospectClient client;

	@Mock
	private ServerBearerTokenResolver bearerTokenResolver;

	private Oauth2IntrospectFilter filter;

	@BeforeEach
	public void setup() {
		filter = new Oauth2IntrospectFilter(client);
		filter.setBearerTokenResolver(bearerTokenResolver);
	}

	@Test
	public void testNoAccessToken() {
		when(authFilterChain.filter(any())).thenReturn(Mono.empty());
		when(bearerTokenResolver.resolve(any())).thenReturn(null);
		Mono<Void> result = filter.filter(authContext, authFilterChain);

		StepVerifier.create(result).verifyComplete();
		verify(authFilterChain, only()).filter(any());
		assertThat(authContext.getPrincipal()).isNull();
		assertThat(authContext.getUserInfo()).isNull();

	}

	@Test
	public void testLoadUserError() {
		when(authFilterChain.filter(any())).thenReturn(Mono.empty());
		when(bearerTokenResolver.resolve(any())).thenReturn("token");
		when(client.loadUser(any())).thenThrow(new IllegalStateException());
		Mono<Void> result = filter.filter(authContext, authFilterChain);

		StepVerifier.create(result).verifyComplete();
		verify(authFilterChain, only()).filter(any());
		assertThat(authContext.getPrincipal()).isNull();
		assertThat(authContext.getUserInfo()).isNull();
	}

	@Test
	public void testLoadUserSuccess() {
		when(authFilterChain.filter(any())).thenReturn(Mono.empty());
		when(bearerTokenResolver.resolve(any())).thenReturn("token");
		when(client.loadUser(any())).thenReturn(Mono.just(new UserInfo()));
		Mono<Void> result = filter.filter(authContext, authFilterChain);

		StepVerifier.create(result).verifyComplete();
		verify(authFilterChain, only()).filter(any());
		assertThat(authContext.getPrincipal()).isNotNull();
		assertThat(authContext.getUserInfo()).isNotNull();
	}

}