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

package com.power4j.fist.cloud.gateway.authorization.domain;

import com.power4j.fist.cloud.gateway.proxy.RouteInfo;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Getter
@Setter
public class AuthContext {

	private final AuthState authState;

	private final Object principal;

	private ServerWebExchange exchange;

	private RequestInfo inbound;

	@Nullable
	private RouteInfo route;

	@Nullable
	private ApiProxy upstream;

	@Nullable
	private Mono<ApiProxy> upstreamMono;

	@Nullable
	private AuthenticatedUser userInfo;

	@Nullable
	private PermissionDefinition permissionDefinition;

	public AuthContext(ServerWebExchange exchange, Object principal) {
		this.exchange = exchange;
		this.principal = principal;
		this.authState = new AuthState();
	}

	public AuthContext(ServerWebExchange exchange, Object principal, AuthState state) {
		this.exchange = exchange;
		this.principal = principal;
		this.authState = state;
	}

	public AuthContext updateState(AuthProblem problem) {
		authState.setProblem(problem);
		return this;
	}

}
