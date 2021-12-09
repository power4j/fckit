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

package com.power4j.fist.security.oauth2.server.resource.reactive;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.GatewayAuthFilterChain;
import com.power4j.fist.security.core.authentication.UserConverter;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/29
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

	private final static AuthorizationDecision GRANTED = new AuthorizationDecision(true);

	private final static AuthorizationDecision DENIED = new AuthorizationDecision(false);

	private final GatewayAuthFilterChain filterChain;

	private final UserConverter<? extends AuthenticatedUser> userConverter;

	// TODO : MDC TaskDecorator
	private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

	@Override
	public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext object) {
		return authentication.flatMap(auth -> doCheck(auth, object));
	}

	private Mono<AuthorizationDecision> doCheck(Authentication authentication, AuthorizationContext object) {
		AuthContext context = new AuthContext(object.getExchange(), authentication);
		context.setUserInfo(userConverter.convert(authentication));
		Object pre = authentication.getDetails();
		return warp(context, filterChain).flatMap(this::decide);
	}

	private Mono<AuthorizationDecision> decide(AuthContext context) {
		final AuthProblem problem = context.getAuthState().getProblem();
		Validate.notNull(problem);
		if (log.isDebugEnabled()) {
			log.debug("[{}] -> {},reason = {}", problem.passStr(), context.getInbound().shortDescription(),
					problem.description());
		}
		return Mono.just(problem.isAuthPass() ? GRANTED : DENIED);
	}

	private Mono<AuthContext> warp(AuthContext context, GatewayAuthFilterChain filterChain) {
		CompletableFuture<AuthContext> future = CompletableFuture.supplyAsync(() -> {
			try {
				filterChain.runNext(context);
			}
			catch (Exception e) {
				context.getAuthState().setProblem(AuthProblem.AUTH_EXCEPTION.moreInfo(e.getMessage()));
			}
			return context;
		}, threadPoolTaskExecutor.getThreadPoolExecutor());
		return Mono.fromCompletionStage(future);
	}

}
