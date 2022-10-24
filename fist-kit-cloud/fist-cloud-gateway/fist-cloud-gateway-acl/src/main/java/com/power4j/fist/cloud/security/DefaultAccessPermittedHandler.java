package com.power4j.fist.cloud.security;

import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.inner.UserEncoder;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAccessPermittedHandler implements AccessPermittedHandler {

	private final UserEncoder userEncoder;

	@Override
	public Mono<ServerWebExchange> handleAccessPermitted(AuthContext context) {
		ServerWebExchange exchange = context.getExchange();

		final Object user = context.getPrincipal();
		if (user instanceof UserInfo) {
			String payload = userEncoder.encode((UserInfo) user);
			// @formatter:off
			ServerHttpRequest request = context.getExchange().getRequest()
					.mutate()
					.headers(h -> h.add(SecurityConstant.HEADER_USER_TOKEN_INNER,payload))
					.build();
			// @formatter:on
			exchange = context.getExchange().mutate().request(request).build();
		}
		else {
			if (Objects.nonNull(user)) {
				log.debug(String.format("principal(%s) exists in auth context ,but not %s", ClassUtils.getName(user),
						UserInfo.class.getName()));
			}
		}
		return Mono.just(exchange);
	}

}
