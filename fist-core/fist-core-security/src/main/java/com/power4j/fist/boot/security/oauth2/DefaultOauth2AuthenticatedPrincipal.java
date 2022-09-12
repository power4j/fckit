package com.power4j.fist.boot.security.oauth2;

import com.power4j.fist.security.core.authorization.domain.GrantedPermission;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public class DefaultOauth2AuthenticatedPrincipal implements Oauth2AuthenticatedPrincipal {

	private final Map<String, Object> attributes;

	private final Collection<GrantedPermission> authorities;

	private final String name;

	/**
	 * Constructs an {@code DefaultOAuth2AuthenticatedPrincipal} using the provided
	 * parameters.
	 * @param attributes the attributes of the OAuth 2.0 token
	 * @param authorities the authorities of the OAuth 2.0 token
	 */
	public DefaultOauth2AuthenticatedPrincipal(Map<String, Object> attributes,
			Collection<GrantedPermission> authorities) {
		this(null, attributes, authorities);
	}

	/**
	 * Constructs an {@code DefaultOAuth2AuthenticatedPrincipal} using the provided
	 * parameters.
	 * @param name the name attached to the OAuth 2.0 token
	 * @param attributes the attributes of the OAuth 2.0 token
	 * @param authorities the authorities of the OAuth 2.0 token
	 */
	public DefaultOauth2AuthenticatedPrincipal(String name, Map<String, Object> attributes,
			Collection<GrantedPermission> authorities) {
		Assert.notEmpty(attributes, "attributes cannot be empty");
		this.attributes = Collections.unmodifiableMap(attributes);
		this.authorities = (authorities != null) ? Collections.unmodifiableCollection(authorities)
				: Collections.emptyList();
		this.name = (name != null) ? name : (String) this.attributes.get("sub");
	}

	/**
	 * Gets the attributes of the OAuth 2.0 token in map form.
	 * @return a {@link Map} of the attribute's objects keyed by the attribute's names
	 */
	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public Collection<? extends GrantedPermission> getAuthorities() {
		return this.authorities;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
