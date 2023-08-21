package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.authorization.domain.ApiPermDefinition;
import com.power4j.fist.cloud.gateway.authorization.domain.ApiProxy;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.RequestInfo;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import com.power4j.fist.security.core.authorization.service.reactive.ReactivePermissionDefinitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class LoadPermissionDefinitionFilterTest {

	final String requestUri = "http://localhost/iam/demo";

	private MockServerHttpRequest request = MockServerHttpRequest.get(requestUri).build();

	private ServerWebExchange exchange = MockServerWebExchange.from(request);

	@Mock
	private ServerAuthFilterChain<AuthContext> authFilterChain;

	@Mock
	private PathMatcher matcher;

	@Mock
	private ReactivePermissionDefinitionService<PermissionDefinition> service;

	@Spy
	private AuthContext authContext = new AuthContext(exchange, null);

	@Test
	public void shouldExitChainIfNoUpstream() {
		LoadPermissionDefinitionFilter filter = new LoadPermissionDefinitionFilter(matcher, service);
		when(authContext.getUpstream()).thenReturn(null);
		when(authContext.getInbound()).thenReturn(RequestInfo.httpGet(requestUri));
		Mono<Void> result = filter.filter(authContext, authFilterChain);
		StepVerifier.create(result).verifyComplete();
		assertThat(authContext.getAuthState()).isNotNull();
		assertThat(authContext.getAuthState().getProblem()).isNotNull();
		assertThat(authContext.getAuthState().getProblem().isAuthPass()).isFalse();
		verify(authFilterChain, never()).filter(any());
	}

	@Test
	public void shouldCallChainIfNotMatch() {
		LoadPermissionDefinitionFilter filter = new LoadPermissionDefinitionFilter(matcher, service);
		when(authContext.getUpstream()).thenReturn(ApiProxy.of("demo-service", HttpMethod.GET, "/demo"));
		when(authFilterChain.filter(any())).thenReturn(Mono.empty());
		when(service.getPermissionDefinition(any(), any())).thenReturn(Mono.just(Collections.emptyList()));
		Mono<Void> result = filter.filter(authContext, authFilterChain);
		StepVerifier.create(result).verifyComplete();
		verify(authFilterChain, times(1)).filter(any());
	}

	@Test
	public void shouldCallChainIfMatched() {
		ApiPermDefinition apiPermDefinition = new ApiPermDefinition();
		apiPermDefinition.setServiceName("demo");
		apiPermDefinition.setPath("/demo/{id}");

		LoadPermissionDefinitionFilter filter = new LoadPermissionDefinitionFilter(matcher, service);
		when(authContext.getUpstream()).thenReturn(ApiProxy.of("demo-service", HttpMethod.GET, "/demo"));
		when(authContext.getInbound()).thenReturn(RequestInfo.httpGet(requestUri));
		when(authFilterChain.filter(any())).thenReturn(Mono.empty());
		when(matcher.bestMatch(any(), any(), any())).thenReturn(Optional.of(apiPermDefinition));
		when(service.getPermissionDefinition(any(), any()))
			.thenReturn(Mono.just(Collections.singletonList(apiPermDefinition)));
		Mono<Void> result = filter.filter(authContext, authFilterChain);
		StepVerifier.create(result).verifyComplete();
		verify(authFilterChain, only()).filter(any());

	}

}