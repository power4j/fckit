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

package com.power4j.fist.cloud.gateway.proxy;

import com.power4j.fist.cloud.gateway.authorization.domain.ApiProxy;
import com.power4j.fist.cloud.gateway.authorization.domain.RouteTarget;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Slf4j
public class DefaultProxyResolver implements ProxyResolver {

	@Setter
	private int strips = 1;

	@Setter
	private RouteTargetResolver routeTargetResolver = new DefaultRouteTargetResolver();

	@Override
	public Optional<ApiProxy> resolve(@Nullable RouteInfo routeInfo, ServerWebExchange exchange) {
		final ServerHttpRequest request = exchange.getRequest();
		RouteTarget routeTarget;
		if (null != request.getMethod() && (routeTarget = routeTargetResolver.resolve(routeInfo, exchange)) != null) {
			String origin = request.getURI().getRawPath();
			String newPath = strip(origin, strips);
			if (log.isDebugEnabled()) {
				log.debug("ApiProxy resolved: {} -> {}", origin, newPath);
			}
			return Optional.of(ApiProxy.of(routeTarget.getServiceName(), request.getMethod(), newPath));
		}
		return Optional.empty();
	}

	static String strip(String path, int parts) {
		String newPath = "/" + Arrays.stream(StringUtils.tokenizeToStringArray(path, "/")).skip(parts)
				.collect(Collectors.joining("/"));
		newPath += (newPath.length() > 1 && path.endsWith("/") ? "/" : "");
		return newPath;
	}

}
