package com.power4j.fist.cloud.security;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public interface AccessDeniedHandler {

	/**
	 * Access denied
	 * @param exchange the ServerWebExchange
	 * @param problem auth result
	 * @return a Mono that indicates completion or error
	 */
	Mono<Void> handleAccessDenied(ServerWebExchange exchange, AuthProblem problem);

}
