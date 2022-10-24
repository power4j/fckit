package com.power4j.fist.autoconfigure.gateway;

import com.power4j.fist.boot.common.prop.PropConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = Oauth2ResourceServerProperties.PROP_PREFIX)
public class Oauth2ResourceServerProperties {

	public static final String PROP_ENTRY = "oauth2.resourceserver";

	public static final String PROP_PREFIX = PropConstant.PROP_ROOT_PREFIX + PROP_ENTRY;

	private OpaqueTokenConfig opaqueToken = new OpaqueTokenConfig();

	@Data
	public static class OpaqueTokenConfig {

		/**
		 * Oauth 服务器地址,支持LB模式,比如 lb://fist-auth/v1/token/auth-user/details
		 */
		private String introspectionUri;

		private String clientId;

		private String clientSecret;

	}

}
