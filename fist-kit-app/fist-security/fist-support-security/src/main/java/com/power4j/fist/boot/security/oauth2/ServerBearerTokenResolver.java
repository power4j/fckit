package com.power4j.fist.boot.security.oauth2;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public interface ServerBearerTokenResolver {

	/**
	 * Resolve any Bearer Token value from the request.
	 * @param request the request
	 * @return the Bearer Token value or null if none found
	 */
	@Nullable
	String resolve(ServerHttpRequest request);

}
