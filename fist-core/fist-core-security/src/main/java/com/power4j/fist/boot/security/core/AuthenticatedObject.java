package com.power4j.fist.boot.security.core;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
public interface AuthenticatedObject {

	/**
	 * Returns the name of the authenticated <code>Principal</code>. Never
	 * <code>null</code>.
	 * @return the name of the authenticated <code>Principal</code>
	 */
	String getName();

}
