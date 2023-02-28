package com.power4j.fist.autoconfigure.gateway;

import com.power4j.fist.boot.security.oauth2.server.resource.introspection.DefaultReactiveOpaqueTokenIntrospector;
import com.power4j.fist.boot.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import com.power4j.fist.cloud.security.oauth2.client.UserIntrospectClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(Oauth2ResourceServerProperties.class)
public class Oauth2Configuration {

	private final static String LB_SCHEMA = "lb";

	private final Oauth2ResourceServerProperties resourceServerProperties;

	@Lazy
	@LoadBalanced
	@Bean(name = "lbWebClientBuilder")
	WebClient.Builder lbWebClientBuilder() {
		return WebClient.builder();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "fist.oauth2.resourceserver.opaque-token.introspection-uri")
	public UserIntrospectClient userIntrospectClient() {
		final Oauth2ResourceServerProperties.OpaqueTokenConfig config = resourceServerProperties.getOpaqueToken();
		Validate.notNull(config);

		final String uri = config.getIntrospectionUri();
		WebClient client;
		if (StringUtils.startsWith(uri, LB_SCHEMA)) {
			log.info("Use load-balanced client for: {}", uri);
			client = lbWebClientBuilder()
				.defaultHeaders((h) -> h.setBasicAuth(config.getClientId(), config.getClientSecret()))
				.build();
		}
		else {
			client = WebClient.builder()
				.defaultHeaders((h) -> h.setBasicAuth(config.getClientId(), config.getClientSecret()))
				.build();
		}
		ReactiveOpaqueTokenIntrospector opaqueTokenIntrospector = new DefaultReactiveOpaqueTokenIntrospector(uri,
				client);
		return new UserIntrospectClient(opaqueTokenIntrospector);
	}

}
