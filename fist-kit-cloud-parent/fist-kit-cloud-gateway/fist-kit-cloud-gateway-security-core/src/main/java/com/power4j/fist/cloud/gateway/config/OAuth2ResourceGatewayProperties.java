package com.power4j.fist.cloud.gateway.config;

import com.power4j.fist.boot.common.prop.PropConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/9/4
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = OAuth2ResourceGatewayProperties.PROP_PREFIX)
public class OAuth2ResourceGatewayProperties {

	public static final String PROP_ENTRY = "gateway.oauth2.resourceserver";

	public static final String PROP_PREFIX = PropConstant.PROP_ROOT_PREFIX + PROP_ENTRY;

	private final Opaquetoken opaqueToken = new Opaquetoken();

	public static class Opaquetoken {

		/**
		 * Client id used to authenticate with the token introspection endpoint.
		 */
		private String clientId;

		/**
		 * Client secret used to authenticate with the token introspection endpoint.
		 */
		private String clientSecret;

		/**
		 * OAuth 2.0 endpoint through which token introspection is accomplished.
		 */
		private String introspectionUri;

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return this.clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getIntrospectionUri() {
			return this.introspectionUri;
		}

		public void setIntrospectionUri(String introspectionUri) {
			this.introspectionUri = introspectionUri;
		}

	}

}
