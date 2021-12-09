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

package com.power4j.fist.security.core.authorization.filter.reactive;

import reactor.core.publisher.Mono;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/25
 * @since 1.0
 * @param <C>
 */
public interface ServerAuthFilter<C> {

	/**
	 * 处理请求,调用后续处理链
	 * @param ctx 上下文
	 * @param chain 鉴权链
	 * @return 返回 Mono<Void> 表示处理结束
	 */
	Mono<Void> filter(C ctx, ServerAuthFilterChain<C> chain);

}
