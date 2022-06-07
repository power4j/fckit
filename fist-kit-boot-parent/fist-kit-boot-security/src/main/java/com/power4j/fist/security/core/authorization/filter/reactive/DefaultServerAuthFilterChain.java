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

import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/25
 * @since 1.0
 */
public class DefaultServerAuthFilterChain<C, F extends ServerAuthFilter<C>> implements ServerAuthFilterChain<C> {

	private final static String CHECK_POINT_TAG = String.format(" [%s]", DefaultServerAuthFilterChain.class.getName());

	private final List<F> filters;

	@Nullable
	protected final ServerAuthFilter<C> currentFilter;

	@Nullable
	protected final DefaultServerAuthFilterChain<C, F> chain;

	public DefaultServerAuthFilterChain(List<F> filters) {
		this.filters = Collections.unmodifiableList(filters);
		DefaultServerAuthFilterChain<C, F> chain = initChain(filters);
		this.currentFilter = chain.currentFilter;
		this.chain = chain.chain;
	}

	private static <C, F extends ServerAuthFilter<C>> DefaultServerAuthFilterChain<C, F> initChain(List<F> filters) {
		DefaultServerAuthFilterChain<C, F> chain = new DefaultServerAuthFilterChain<>(filters, null, null);
		ListIterator<? extends ServerAuthFilter<C>> iterator = filters.listIterator(filters.size());
		while (iterator.hasPrevious()) {
			chain = new DefaultServerAuthFilterChain<>(filters, iterator.previous(), chain);
		}
		return chain;
	}

	private DefaultServerAuthFilterChain(List<F> filters, @Nullable ServerAuthFilter<C> currentFilter,
			@Nullable DefaultServerAuthFilterChain<C, F> chain) {

		this.filters = filters;
		this.currentFilter = currentFilter;
		this.chain = chain;
	}

	@Override
	public Mono<C> filter(C context) {
		// @formatter:off
		return (currentFilter != null && chain != null) ?
				invokeFilter(currentFilter,chain,context) : Mono.just(context);
		// @formatter:on
	}

	public List<F> getFilters() {
		return filters;
	}

	protected Mono<C> invokeFilter(ServerAuthFilter<C> filter, DefaultServerAuthFilterChain<C, F> chain, C context) {
		String currentName = filter.getClass().getName();
		return filter.filter(context, chain).checkpoint(currentName + CHECK_POINT_TAG);
	}

}
