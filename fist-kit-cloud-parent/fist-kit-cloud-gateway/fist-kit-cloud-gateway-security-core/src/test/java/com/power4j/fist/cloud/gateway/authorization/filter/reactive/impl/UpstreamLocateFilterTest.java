package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.RequestInfo;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.security.core.authorization.domain.AnonymousUser;
import com.power4j.fist.security.core.authorization.filter.reactive.DefaultServerAuthFilterChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/13
 * @since 1.0
 */
class UpstreamLocateFilterTest {

	static class TestFilter extends UpstreamLocateFilter implements GatewayAuthFilter {

		public TestFilter() {
			super(null);
		}

		@Override
		protected Mono<Route> lookupRoute(ServerWebExchange exchange) {
			return Mono.empty();
		}

	}

	@Test
	void noRouteTest() throws Exception {
		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		AuthContext context = new AuthContext(exchange, new AnonymousUser());
		context.setInbound(new RequestInfo(HttpHeaders.EMPTY, HttpMethod.GET, new URI("http://localhost")));
		TestFilter filter = new TestFilter();
		DefaultServerAuthFilterChain<AuthContext, GatewayAuthFilter> chain = new DefaultServerAuthFilterChain<>(
				Arrays.asList(filter, new AuthEndFilter()));
		chain.filter(context).block();
		Assertions.assertNotNull(context.getAuthState().getProblem());
		System.out.println(context.getAuthState().getProblem().description());
	}

}