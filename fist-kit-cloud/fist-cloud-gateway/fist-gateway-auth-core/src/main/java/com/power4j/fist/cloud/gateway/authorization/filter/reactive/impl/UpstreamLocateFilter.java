/*
 *  Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.gnu.org/licenses/lgpl.html
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.cloud.gateway.authorization.domain.ApiProxy;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.proxy.DefaultProxyResolver;
import com.power4j.fist.cloud.gateway.proxy.ProxyResolver;
import com.power4j.fist.cloud.gateway.proxy.RouteInfo;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class UpstreamLocateFilter implements GatewayAuthFilter {

	private final static int NO_PORT = -1;

	private final RouteLocator routeLocator;

	@Setter
	private ProxyResolver proxyResolver = new DefaultProxyResolver();

	@Override
	public Mono<Void> filter(AuthContext ctx, ServerAuthFilterChain<AuthContext> chain) {
		return resolveUpstream(ctx, chain);
	}

	// ~ utils
	// ===================================================================================================

	protected Mono<Route> lookupRoute(ServerWebExchange exchange) {
		// @formatter:off
		return this.routeLocator
				.getRoutes()
				.concatMap(route -> Mono.just(route)
						.filterWhen(r -> r.getPredicate().apply(exchange))
						.doOnError(e -> log.error("Error applying predicate for route: " + route.getId(), e))
						.onErrorResume(e -> Mono.empty())
				)
				.next()
				.map(route -> {
					if (log.isDebugEnabled()) {
						log.debug("Route matched: " + route.getId());
					}
					return route;
				});
		// @formatter:on
	}

	Mono<Void> resolveUpstream(AuthContext context, ServerAuthFilterChain<AuthContext> chain) {
		// @formatter:off
		return lookupRoute(context.getExchange())
				.doOnSuccess(o -> {
					if(log.isTraceEnabled()){
						log.trace("route match completed: {}",context.getInbound().shortDescription());
					}
				})
				.flatMap(o -> Mono.just(Optional.of(o)))
				.switchIfEmpty(Mono.fromSupplier(Optional::empty))
				.flatMap(o -> {
					updateApiProxy(o.orElse(null), context);
					return doNext(context, chain);
				});
		// @formatter:on
	}

	void updateApiProxy(@Nullable Route route, AuthContext context) {
		RouteInfo routeInfo = Optional.ofNullable(route).map(RouteInfo::from).orElse(null);
		ApiProxy upstream = proxyResolver.resolve(routeInfo, context.getExchange())
			.orElseGet(() -> makeDefaultApiProxy(context));
		if (log.isTraceEnabled()) {
			log.trace("use upstream : {}", upstream.description());
		}
		context.setUpstream(upstream);
	}

	ApiProxy makeDefaultApiProxy(AuthContext context) {
		final URI uri = context.getInbound().getUri();
		if (log.isDebugEnabled()) {
			log.debug("No route for request : {}", context.getInbound().getRawPath());
		}
		String serviceName = Objects.requireNonNull(uri.getHost());
		if (uri.getPort() != NO_PORT) {
			serviceName += (StringPool.COLON + uri.getPort());
		}
		return ApiProxy.of(serviceName, context.getInbound().getMethod(), context.getInbound().getRawPath());
	}

}
