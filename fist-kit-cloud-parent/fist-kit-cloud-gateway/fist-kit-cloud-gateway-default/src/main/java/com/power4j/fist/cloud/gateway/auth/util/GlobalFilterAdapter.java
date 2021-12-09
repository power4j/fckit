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

package com.power4j.fist.cloud.gateway.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.coca.kit.common.lang.Obj;
import com.power4j.fist.boot.common.api.Results;
import com.power4j.fist.boot.util.SpringEventUtil;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.web.reactive.util.ServerHttpResponseUtil;
import com.power4j.fist.cloud.gateway.auth.configure.FistGatewayProperties;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import com.power4j.fist.cloud.gateway.auth.entity.AuthProblem;
import com.power4j.fist.cloud.gateway.auth.entity.RequestInfo;
import com.power4j.fist.cloud.gateway.auth.entity.ResponseInfo;
import com.power4j.fist.cloud.gateway.auth.entity.RouteInfo;
import com.power4j.fist.cloud.gateway.auth.event.GatewayAuthEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/6
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class GlobalFilterAdapter implements GlobalFilter {

	private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final FistGatewayProperties fistGatewayProperties;

	private final DefaultGatewayAuthFilterChain authFilterChain;

	/// TODO : MDC
	private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		final HttpMethod method = request.getMethod();
		if (Objects.isNull(method)) {
			log.info("non-standard HTTP method,Skip auth chain");
			return chain.filter(exchange);
		}
		final HttpHeaders headers = request.getHeaders();
		final String token = headers.getFirst(fistGatewayProperties.getPermission().getApiTokenName());

		AuthContext ctx = AuthContext.builder().originalRequest(request)
				.handlerUri(exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR))
				.requestInfo(new RequestInfo(headers, method, request.getURI(), token)).responseInfo(new ResponseInfo())
				.routeInfo(getRouteInfo(exchange)).build();
		return handleAuth(ctx).flatMap(context -> {
			final AuthProblem authProblem = context.getResponseInfo().getProblem();
			// FIXME: msg key to message
			final String message = authProblem.getMsgKey();
			GatewayAuthEvent event = GatewayAuthEvent.builder().apiToken(token)
					.code(Integer.toString(authProblem.getCode())).msg(message).pass(authProblem.isAuthPass())
					.requestUri(request.getURI().toString()).build();
			SpringEventUtil.publishEvent(event);
			if (authProblem.isAuthPass()) {
				ServerWebExchange webExchange = exchange.mutate().request(builder -> builder
						.header(SecurityConstant.HEADER_USER_TOKEN_INNER, context.getResponseInfo().getUserToken()))
						.build();
				return chain.filter(webExchange);
			}
			return ServerHttpResponseUtil.responseWithJsonObject(exchange.getResponse(), OBJECT_MAPPER,
					Results.noPermission(null, null), HttpStatus.FORBIDDEN);
		});

	}

	private Mono<AuthContext> handleAuth(AuthContext context) {
		CompletableFuture<AuthContext> future = CompletableFuture.supplyAsync(() -> {
			authFilterChain.runNext(context);
			return context;
		}, threadPoolTaskExecutor.getThreadPoolExecutor());
		return Mono.fromCompletionStage(future);
	}

	protected RouteInfo getRouteInfo(ServerWebExchange exchange) {
		Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
		RouteInfo info = new RouteInfo();
		info.setId(route.getId());
		info.setServiceId(route.getUri().getHost());
		info.setUri(route.getUri());
		info.setMetadata(Obj.keepIfNotNull(route.getMetadata(), Collections::emptyMap));

		return info;
	}

}
