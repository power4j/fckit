package com.power4j.fist.cloud.gateway;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilterChain;
import com.power4j.fist.cloud.security.AccessDeniedHandler;
import com.power4j.fist.cloud.security.AccessPermittedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class ApiGuardFilter implements GlobalFilter {

	private final GatewayAuthFilterChain filterChain;

	private final AccessPermittedHandler accessPermittedHandler;

	private final AccessDeniedHandler accessDeniedHandler;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		AuthContext context = makeAuthContext(exchange);
		return filterChain.filter(context).doOnSuccess(o -> {
			if (Objects.isNull(context.getAuthState().getProblem())) {
				log.warn("auth filter completed without reason: {}", context.getInbound().shortDescription());
			}
		}).then(Mono.defer(() -> decide(context, chain)));
	}

	protected AuthContext makeAuthContext(ServerWebExchange exchange) {
		return new AuthContext(exchange, null);
	}

	private Mono<Void> decide(AuthContext context, GatewayFilterChain chain) {
		final AuthProblem problem = context.getAuthState().getProblem();
		Validate.notNull(problem);
		if (problem.isAuthPass()) {
			if (log.isDebugEnabled()) {
				log.debug("[{}] -> {},reason = {}", problem.passStr(), context.getInbound().shortDescription(),
						problem.description());
			}
			return accessPermittedHandler.handleAccessPermitted(context).flatMap(chain::filter);
		}
		else {
			log.info("[{}] -> {},reason = {}", problem.passStr(), context.getInbound().shortDescription(),
					problem.description());
			return accessDeniedHandler.handleAccessDenied(context.getExchange(), problem);
		}
	}

}
