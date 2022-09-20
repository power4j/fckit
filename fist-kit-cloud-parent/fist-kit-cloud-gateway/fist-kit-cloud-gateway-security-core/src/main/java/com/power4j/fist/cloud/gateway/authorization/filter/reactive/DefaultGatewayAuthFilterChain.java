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
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Slf4j
public class DefaultGatewayAuthFilterChain implements GatewayAuthFilterChain {

	private final List<GatewayAuthFilter> filters;

	@Nullable
	protected final GatewayAuthFilter currentFilter;

	@Nullable
	protected final DefaultGatewayAuthFilterChain chain;

	public DefaultGatewayAuthFilterChain(List<GatewayAuthFilter> filters) {
		this.filters = Collections.unmodifiableList(filters);
		DefaultGatewayAuthFilterChain chain = initChain(filters);
		this.currentFilter = chain.currentFilter;
		this.chain = chain.chain;
	}

	private static DefaultGatewayAuthFilterChain initChain(List<GatewayAuthFilter> filters) {
		DefaultGatewayAuthFilterChain chain = new DefaultGatewayAuthFilterChain(filters, null, null);
		ListIterator<GatewayAuthFilter> iterator = filters.listIterator(filters.size());
		while (iterator.hasPrevious()) {
			chain = new DefaultGatewayAuthFilterChain(filters, iterator.previous(), chain);
		}
		return chain;
	}

	private DefaultGatewayAuthFilterChain(List<GatewayAuthFilter> filters, @Nullable GatewayAuthFilter currentFilter,
			@Nullable DefaultGatewayAuthFilterChain chain) {

		this.filters = filters;
		this.currentFilter = currentFilter;
		this.chain = chain;
	}

	@Override
	public Mono<Void> filter(AuthContext context) {
		if (currentFilter != null && chain != null) {
			if (log.isTraceEnabled()) {
				log.trace("invoke filter {}", currentFilter.getClass().getName());
			}
			return invokeFilter(currentFilter, chain, context);
		}
		else {
			Assert.notNull(context.getAuthState().getProblem(), "AuthState not update");
			if (log.isDebugEnabled()) {
				log.debug("[{}] end,Auth state:{}", getClass().getSimpleName(),
						context.getAuthState().getProblem().description());
			}
			return Mono.empty();
		}
	}

	public List<GatewayAuthFilter> getFilters() {
		return filters;
	}

	protected Mono<Void> invokeFilter(GatewayAuthFilter filter, DefaultGatewayAuthFilterChain chain,
			AuthContext context) {
		String currentName = filter.getClass().getName();
		return filter.filter(context, chain).checkpoint(currentName);
	}

}
