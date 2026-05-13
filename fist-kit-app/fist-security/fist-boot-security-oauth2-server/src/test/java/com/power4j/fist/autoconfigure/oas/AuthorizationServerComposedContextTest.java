package com.power4j.fist.autoconfigure.oas;

import com.power4j.fist.oauth2.extension.service.DefaultAuthorizationService;
import com.power4j.fist.oauth2.extension.service.ManagedOAuth2AuthorizationService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.sql.Connection;
import java.time.Instant;
import java.util.Set;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServerComposedContextTest {

	private static final String AUTHORIZATION_SCHEMA = "org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AuthorizationServerAutoConfiguration.class))
		.withUserConfiguration(AuthorizationServiceConfiguration.class);

	@Test
	void shouldComposeAutoConfigurationWithJdbcAuthorizationService() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OAuth2TokenGenerator.class)
				.hasSingleBean(RegisteredClientRepository.class)
				.hasSingleBean(ManagedOAuth2AuthorizationService.class);
			assertThat(context.getBean(ManagedOAuth2AuthorizationService.class))
				.isInstanceOf(DefaultAuthorizationService.class);

			RegisteredClient registeredClient = context.getBean(RegisteredClient.class);
			OAuth2Authorization authorization = authorization(registeredClient);
			ManagedOAuth2AuthorizationService authorizationService = context
				.getBean(ManagedOAuth2AuthorizationService.class);

			authorizationService.save(authorization);

			OAuth2Authorization loadedById = authorizationService.findById(authorization.getId());
			assertThat(loadedById).isNotNull();
			assertThat(loadedById.getPrincipalName()).isEqualTo("alice");
			assertThat(loadedById.getAccessToken().getToken().getTokenValue()).isEqualTo("access-token");
			OAuth2Authorization loadedByToken = authorizationService.findByToken("access-token",
					OAuth2TokenType.ACCESS_TOKEN);
			assertThat(loadedByToken).isNotNull();
			assertThat(loadedByToken.getId()).isEqualTo(authorization.getId());
		});
	}

	private static OAuth2Authorization authorization(RegisteredClient registeredClient) {
		Instant issuedAt = Instant.parse("2026-05-12T00:00:00Z");
		return OAuth2Authorization.withRegisteredClient(registeredClient)
			.id("authorization-id")
			.principalName("alice")
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.authorizedScopes(Set.of("read"))
			.accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", issuedAt,
					issuedAt.plusSeconds(60), Set.of("read")))
			.build();
	}

	@Configuration(proxyBeanMethods = false)
	static class AuthorizationServiceConfiguration {

		@Bean
		DataSource dataSource() throws Exception {
			JdbcDataSource dataSource = new JdbcDataSource();
			dataSource.setURL("jdbc:h2:mem:oauth2_server_context_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
			try (Connection connection = dataSource.getConnection()) {
				ScriptUtils.executeSqlScript(connection,
						new EncodedResource(new ClassPathResource(AUTHORIZATION_SCHEMA)));
			}
			return dataSource;
		}

		@Bean
		JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		RegisteredClient registeredClient() {
			return RegisteredClient.withId("client-id")
				.clientId("client")
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("read")
				.build();
		}

		@Bean
		RegisteredClientRepository registeredClientRepository(RegisteredClient registeredClient) {
			return new RegisteredClientRepository() {
				@Override
				public void save(RegisteredClient registeredClient) {
				}

				@Override
				public RegisteredClient findById(String id) {
					return registeredClient.getId().equals(id) ? registeredClient : null;
				}

				@Override
				public RegisteredClient findByClientId(String clientId) {
					return registeredClient.getClientId().equals(clientId) ? registeredClient : null;
				}
			};
		}

		@Bean
		ManagedOAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
				RegisteredClientRepository registeredClientRepository) {
			return new DefaultAuthorizationService(jdbcTemplate, registeredClientRepository);
		}

	}

}
