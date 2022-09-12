package com.power4j.fist.boot.security.oauth2;

import com.power4j.fist.boot.security.core.AuthenticatedObject;
import com.power4j.fist.security.core.authorization.domain.GrantedPermission;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
public interface Oauth2AuthenticatedPrincipal extends AuthenticatedObject {

	/**
	 * Get the OAuth 2.0 token attribute by name
	 * @param name the name of the attribute
	 * @param <A> the type of the attribute
	 * @return the attribute or {@code null} otherwise
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	default <A> A getAttribute(String name) {
		return (A) getAttributes().get(name);
	}

	/**
	 * Get the OAuth 2.0 token attributes
	 * @return the OAuth 2.0 token attributes
	 */
	Map<String, Object> getAttributes();

	/**
	 * Get the {@link Collection} of {@link GrantedPermission}s associated with this OAuth
	 * 2.0 token
	 * @return the OAuth 2.0 token authorities
	 */
	Collection<? extends GrantedPermission> getAuthorities();

}
