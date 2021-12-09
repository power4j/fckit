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
import lombok.Setter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
public class DefaultProxyResolver implements ProxyResolver {

	private final static String SCHEMA_LB = "lb";

	@Setter
	private int strips = 1;

	@Setter
	private Predicate<RouteInfo> routePredicate = r -> SCHEMA_LB.equalsIgnoreCase(r.getUri().getScheme());

	@Override
	public Optional<ApiProxy> resolve(RouteInfo routeInfo, ServerHttpRequest request) {

		String path = request.getURI().getRawPath();
		if (Objects.isNull(request.getMethod()) || !routePredicate.test(routeInfo)) {
			return Optional.empty();
		}
		String newPath = strip(path, strips);
		ApiProxy proxy = ApiProxy.of(routeInfo.getUri().getHost(), request.getMethod(), newPath);
		return Optional.of(proxy);
	}

	static String strip(String path, int parts) {
		String newPath = "/" + Arrays.stream(StringUtils.tokenizeToStringArray(path, "/")).skip(parts)
				.collect(Collectors.joining("/"));
		newPath += (newPath.length() > 1 && path.endsWith("/") ? "/" : "");
		return newPath;
	}

}
