package com.power4j.fist.cloud.security;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public interface AccessPermittedHandler {

	/**
	 * Do something when access check pass
	 * @param context auth context
	 * @return ServerWebExchange
	 */
	Mono<ServerWebExchange> handleAccessPermitted(AuthContext context);

}
