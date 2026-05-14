package com.power4j.fist3.examples.oauth2.server;

import com.power4j.fist.oauth2.extension.service.DefaultAuthorizationService;
import com.power4j.fist.oauth2.extension.service.ManagedOAuth2AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.UUID;

@Configuration(proxyBeanMethods = false)
class AuthorizationServerExampleConfiguration {

	static final String CLIENT_ID = "fist-example-client";

	static final String CLIENT_SECRET = "secret";

	static final String SCOPE = "profile.read";

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
			RegisteredClientRepository registeredClientRepository, OAuth2AuthorizationService authorizationService,
			AuthorizationServerSettings authorizationServerSettings,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
			AuthenticationConverter accessTokenRequestConverter) throws Exception {
		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
		http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
			.with(authorizationServerConfigurer,
					(authorizationServer) -> authorizationServer.registeredClientRepository(registeredClientRepository)
						.authorizationService(authorizationService)
						.authorizationServerSettings(authorizationServerSettings)
						.tokenGenerator(tokenGenerator)
						.tokenEndpoint((tokenEndpoint) -> tokenEndpoint
							.accessTokenRequestConverter(accessTokenRequestConverter)))
			.csrf((csrf) -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()));
		return http.build();
	}

	@Bean
	SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}

	@Bean
	RegisteredClient registeredClient() {
		return RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId(CLIENT_ID)
			.clientSecret("{noop}" + CLIENT_SECRET)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.scope(SCOPE)
			.tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.REFERENCE).build())
			.build();
	}

	@Bean
	RegisteredClientRepository registeredClientRepository(RegisteredClient registeredClient) {
		return new InMemoryRegisteredClientRepository(registeredClient);
	}

	@Bean
	ManagedOAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		return new DefaultAuthorizationService(jdbcTemplate, registeredClientRepository);
	}

}
