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

package com.power4j.fist.cloud.gateway.authorization.filter.simple.impl;

import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.cloud.gateway.authorization.domain.ApiProxy;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.AbstractAuthFilter;
import com.power4j.fist.cloud.gateway.proxy.DefaultProxyResolver;
import com.power4j.fist.cloud.gateway.proxy.ProxyResolver;
import com.power4j.fist.cloud.gateway.proxy.RouteInfo;
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
public class UpstreamLocateFilter extends AbstractAuthFilter {

	private final static int NO_PORT = -1;

	private final RouteLocator routeLocator;

	@Setter
	private ProxyResolver proxyResolver = new DefaultProxyResolver();

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

	@Override
	protected boolean process(AuthContext context) {
		resolveUpstream(context).doOnSuccess(context::setUpstream).block();
		return true;
	}

	// ~ utils
	// ===================================================================================================

	Mono<ApiProxy> resolveUpstream(AuthContext context) {
		return lookupRoute(context.getExchange()).map(Optional::of).switchIfEmpty(Mono.just(Optional.empty()))
				.map(o -> getApiProxy(o.orElse(null), context));
	}

	ApiProxy getApiProxy(@Nullable Route route, AuthContext context) {
		RouteInfo routeInfo = Optional.ofNullable(route).map(RouteInfo::from).orElse(null);
		return proxyResolver.resolve(routeInfo, context.getExchange()).orElseGet(() -> makeDefaultApiProxy(context));
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
