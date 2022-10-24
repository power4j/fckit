package com.power4j.fist.boot.security.oauth2.server.resource.introspection;

import com.power4j.fist.boot.security.oauth2.Oauth2AuthenticatedPrincipal;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public interface ReactiveOpaqueTokenIntrospector {

	/**
	 * Introspect and verify the given token, returning its attributes.
	 *
	 * Returning a {@link Map} is indicative that the token is valid.
	 * @param token the token to introspect
	 * @return the token's attributes
	 * @throws Oauth2IntrospectionException
	 */
	Mono<Oauth2AuthenticatedPrincipal> introspect(String token);

}
