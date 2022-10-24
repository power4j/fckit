package com.power4j.fist.boot.security.oauth2.server.resource.introspection;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public class Oauth2IntrospectionException extends RuntimeException {

	public Oauth2IntrospectionException(String message) {
		super(message);
	}

	public Oauth2IntrospectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public Oauth2IntrospectionException(Throwable cause) {
		super(cause);
	}

}
