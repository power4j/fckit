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

import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfoExtractor;
import com.power4j.fist.boot.security.inner.UserEncoder;
import com.power4j.fist.boot.security.oauth2.Oauth2UserInfoExtractor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/6
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class BearTokenAuthenticationConverterFilter implements WebFilter {

	private final UserEncoder encoder;

	@Setter
	private UserInfoExtractor extractor = new Oauth2UserInfoExtractor();

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return ReactiveSecurityContextHolder.getContext().flatMap(ctx -> handle(ctx, exchange, chain))
				.switchIfEmpty(handleNoAuthentication(exchange, chain));

	}

	private Mono<Void> handleNoAuthentication(ServerWebExchange exchange, WebFilterChain chain) {
		if (log.isTraceEnabled()) {
			log.trace("No authentication : {}", exchange.getRequest().getURI().toString());
		}
		return Mono.empty();
	}

	private Mono<Void> handle(SecurityContext context, ServerWebExchange exchange, WebFilterChain chain) {
		final Authentication authentication = context.getAuthentication();
		if (authentication != null) {
			if (authentication instanceof BearerTokenAuthentication) {
				log.debug("Authentication : {},request = {}", authentication.getClass().getSimpleName(),
						exchange.getRequest().getURI().toString());
				BearerTokenAuthentication tokenAuthentication = (BearerTokenAuthentication) authentication;
				// @formatter:off
				String inner = extractor
						.extractAuthUser(tokenAuthentication.getTokenAttributes())
						.map(encoder::encode)
						.orElse(null);
				// @formatter:on
				if (null != inner) {
					return chain.filter(withInnerUser(exchange, inner));
				}
			}
		}
		return chain.filter(exchange);
	}

	ServerWebExchange withInnerUser(ServerWebExchange origin, String value) {
		// @formatter:off
		return origin
				.mutate()
				.request(b -> b.headers(h -> h.add(SecurityConstant.HEADER_USER_TOKEN_INNER,value)).build())
				.build();
		// @formatter:on
	}

}
