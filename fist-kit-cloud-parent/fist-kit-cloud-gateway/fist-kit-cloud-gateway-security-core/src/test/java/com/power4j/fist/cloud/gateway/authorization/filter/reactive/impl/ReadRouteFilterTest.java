package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.RequestInfo;
import com.power4j.fist.cloud.gateway.authorization.domain.RouteTarget;
import com.power4j.fist.cloud.gateway.proxy.RouteTargetResolver;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
class ReadRouteFilterTest {

	private ServerAuthFilterChain<AuthContext> chain = c -> Mono.empty();

	private RouteTargetResolver routeTargetResolver = (r, e) -> new RouteTarget("demo-backend");

	@Test
	public void shouldUpdateContext() {

		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost/iam/demo").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		Route route = Route.async().id("iam").uri("lb://iam/demo").predicate(o -> true).build();
		exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, URI.create("http://localhost/demo"));

		AuthContext context = makeAuthContext(exchange);
		ReadRouteFilter filter = new ReadRouteFilter();
		filter.setRouteTargetResolver(routeTargetResolver);

		Mono<Void> result = filter.filter(context, chain);
		StepVerifier.create(result).verifyComplete();

		assertThat(context.getRoute()).isNotNull();
		assertThat(context.getRoute().getId()).isEqualTo("iam");

		assertThat(context.getUpstream()).isNotNull();
		assertThat(context.getUpstream().getServiceName()).isEqualTo("demo-backend");

	}

	static AuthContext makeAuthContext(ServerWebExchange exchange) {
		AuthContext context = new AuthContext(exchange, null);
		RequestInfo info = new RequestInfo(exchange.getRequest().getHeaders(), exchange.getRequest().getMethod(),
				exchange.getRequest().getURI());
		context.setInbound(info);
		return context;
	}

}