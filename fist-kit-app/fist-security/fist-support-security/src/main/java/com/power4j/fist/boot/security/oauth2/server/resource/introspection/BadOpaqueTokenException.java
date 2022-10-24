package com.power4j.fist.boot.security.oauth2.server.resource.introspection;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public class BadOpaqueTokenException extends Oauth2IntrospectionException {

	public BadOpaqueTokenException(String message) {
		super(message);
	}

	public BadOpaqueTokenException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadOpaqueTokenException(Throwable cause) {
		super(cause);
	}

}
