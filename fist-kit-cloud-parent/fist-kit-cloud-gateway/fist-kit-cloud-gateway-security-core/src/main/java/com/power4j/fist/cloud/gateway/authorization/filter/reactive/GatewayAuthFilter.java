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

package com.power4j.fist.cloud.gateway.authorization.filter.reactive;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthState;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilter;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
public interface GatewayAuthFilter extends ServerAuthFilter<AuthContext> {

	/**
	 * 调试信息
	 * @param context AuthContext
	 * @return 返回状态调试信息
	 */
	default String checkpointDescription(AuthContext context) {
		String prob = Optional.ofNullable(context.getAuthState()).map(AuthState::getProblem).map(AuthProblem::getMsg)
				.orElse("null");
		return String.format("[AuthProblem] %s , [%s]", prob, getClass().getName());
	}

	/**
	 * 结束
	 * @param context
	 * @param reason
	 * @return Mono<AuthContext>
	 */
	default Mono<Void> exitChain(AuthContext context, AuthProblem reason) {
		context.updateState(reason);
		return Mono.empty();
	}

	/**
	 * 执行下一个
	 * @param context
	 * @param chan
	 * @return Mono<AuthContext>
	 */
	default Mono<Void> doNext(AuthContext context, ServerAuthFilterChain<AuthContext> chan) {
		return chan.filter(Objects.requireNonNull(context));
	}

}
