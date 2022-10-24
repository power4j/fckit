package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.cloud.gateway.authorization.domain.ApiProxy;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.RouteTarget;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.proxy.DefaultRouteTargetResolver;
import com.power4j.fist.cloud.gateway.proxy.RouteInfo;
import com.power4j.fist.cloud.gateway.proxy.RouteTargetResolver;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Read route information from current ServerWebExchange,this filter must run inside
 * gateway filter
 *
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
public class ReadRouteFilter implements GatewayAuthFilter {

	@Setter
	private RouteTargetResolver routeTargetResolver = new DefaultRouteTargetResolver();

	@Override
	public Mono<Void> filter(AuthContext ctx, ServerAuthFilterChain<AuthContext> chain) {
		final ServerWebExchange exchange = ctx.getExchange();
		Object routeAttr = exchange.getAttributes().get(GATEWAY_ROUTE_ATTR);
		if (routeAttr instanceof Route) {
			Route route = (Route) routeAttr;
			ctx.setRoute(RouteInfo.from(route));
		}
		else {
			if (log.isDebugEnabled()) {
				log.debug("No route for client request:{}", ctx.getInbound().shortDescription());
			}
			return chain.filter(ctx);
		}
		Object uriAttr = exchange.getAttributes().get(GATEWAY_REQUEST_URL_ATTR);
		if (uriAttr instanceof URI) {
			URI uri = (URI) uriAttr;
			if (StringUtils.isEmpty(uri.getHost())) {
				log.warn("No host part for upstream:{}", uri);
			}
			assert ctx.getRoute() != null;
			RouteTarget routeTarget = routeTargetResolver.resolve(ctx.getRoute(), ctx.getExchange());
			if (Objects.nonNull(routeTarget)) {
				ctx.setUpstream(ApiProxy.of(routeTarget.getServiceName(), ctx.getInbound().getMethod(), uri.getPath()));
			}
			else {
				log.warn("No route target found for client request: {}", ctx.getInbound().shortDescription());
			}
		}
		else {
			log.debug("No upstream for client request:{}", ctx.getInbound().shortDescription());
		}
		return chain.filter(ctx);
	}

}
