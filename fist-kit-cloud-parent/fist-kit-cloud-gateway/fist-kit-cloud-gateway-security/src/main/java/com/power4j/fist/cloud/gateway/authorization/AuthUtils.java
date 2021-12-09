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

package com.power4j.fist.cloud.gateway.authorization;

import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import lombok.experimental.UtilityClass;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collection;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@UtilityClass
public class AuthUtils {

	public boolean match(PathMatcher pathMatcher, String path, @Nullable PermissionDefinition rule) {
		if (rule == null) {
			return false;
		}
		return pathMatcher.matches(rule.getPath(), path);
	}

	public ServerWebExchange eraseHeader(ServerWebExchange original, Collection<String> headerKeys) {
		if (headerKeys.isEmpty()) {
			return original;
		}
		boolean mutate = false;
		for (String key : original.getRequest().getHeaders().keySet()) {
			if (headerKeys.contains(key)) {
				mutate = true;
				break;
			}
		}
		if (!mutate) {
			return original;
		}
		// @formatter:off
		return original
				.mutate()
				.request(r -> r.headers(headers -> headerKeys.forEach(headers::remove)))
				.build();
		// @formatter:on
	}

	public ServerWebExchange eraseHeader(ServerWebExchange original, String headerKey) {
		boolean mutate = false;
		if (!original.getRequest().getHeaders().containsKey(headerKey)) {
			return original;
		}
		// @formatter:off
		return original
				.mutate()
				.request(r -> r.headers(headers -> headers.remove(headerKey)))
				.build();
		// @formatter:on
	}

}
