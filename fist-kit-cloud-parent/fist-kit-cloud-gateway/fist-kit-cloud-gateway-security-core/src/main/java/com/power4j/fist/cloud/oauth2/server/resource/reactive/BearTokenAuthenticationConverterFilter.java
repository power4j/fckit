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

package com.power4j.fist.cloud.oauth2.server.resource.reactive;

import com.power4j.fist.boot.security.core.SecurityConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/6
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class BearTokenAuthenticationConverterFilter implements WebFilter {

	private final AuthenticationProcessor authenticationProcessor;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		// @formatter:off
		return ReactiveSecurityContextHolder.getContext()
				.filter(ctx -> Objects.nonNull(ctx.getAuthentication()))
				.flatMap(ctx -> handleAuthentication(ctx, exchange, chain))
				.switchIfEmpty(Mono.defer(() -> handleNoAuthentication(exchange,chain)));
		// @formatter:on
	}

	private Mono<Void> handleNoAuthentication(ServerWebExchange exchange, WebFilterChain chain) {
		if (log.isTraceEnabled()) {
			log.trace("No authentication : {}", exchange.getRequest().getURI());
		}
		return chain.filter(exchange);
	}

	private Mono<Void> handleAuthentication(@Nullable SecurityContext context, ServerWebExchange exchange,
			WebFilterChain chain) {
		final Authentication authentication = Optional.ofNullable(context).map(SecurityContext::getAuthentication)
				.orElse(null);
		if (authentication instanceof BearerTokenAuthentication) {
			log.debug("Authentication : {},request = {}", authentication.getClass().getSimpleName(),
					exchange.getRequest().getURI());
			BearerTokenAuthentication tokenAuthentication = (BearerTokenAuthentication) authentication;
			return authenticationProcessor.process(exchange, tokenAuthentication.getTokenAttributes()).flatMap(w -> {
				if (log.isTraceEnabled()) {
					log.trace("{} = {}", SecurityConstant.HEADER_USER_TOKEN_INNER,
							w.getRequest().getHeaders().getFirst(SecurityConstant.HEADER_USER_TOKEN_INNER));
				}
				return chain.filter(w);
			});
		}
		return Mono.empty();
	}

}
