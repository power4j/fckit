package com.power4j.fist3.examples.oauth2.server;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.power4j.fist.oauth2.core.AuthConstants;
import com.power4j.fist.oauth2.extension.service.DefaultAuthorizationService;
import com.power4j.fist.oauth2.extension.service.ManagedOAuth2AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

@SpringBootTest(classes = AuthorizationServerExampleApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationServerExampleApplicationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ManagedOAuth2AuthorizationService authorizationService;

	@Test
	void shouldExposeAuthorizationServerMetadata() {
		Map<String, Object> metadata = restClient().get()
			.uri("/.well-known/oauth-authorization-server")
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});

		assertThat(metadata).containsEntry("issuer", baseUrl())
			.containsEntry("token_endpoint", baseUrl() + "/oauth2/token");
		assertThat(metadata.get("grant_types_supported")).asInstanceOf(LIST).contains("client_credentials");
		assertThat(metadata.get("token_endpoint_auth_methods_supported")).asInstanceOf(LIST)
			.contains("client_secret_basic");
	}

	@Test
	void shouldIssueClientCredentialsTokenThroughFistGenerator() {
		Map<String, Object> token = restClient().post()
			.uri("/oauth2/token")
			.header("Authorization", basicAuthorization())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(tokenRequest())
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});

		String accessToken = (String) token.get("access_token");
		assertThat(token).containsEntry("token_type", "Bearer")
			.containsEntry("scope", AuthorizationServerExampleConfiguration.SCOPE);
		assertThat(accessToken).isNotBlank();
		assertThat(this.applicationContext.getBeansOfType(OAuth2TokenGenerator.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(AuthenticationConverter.class)).hasSize(1);
		assertThat(this.authorizationService).isInstanceOf(DefaultAuthorizationService.class);

		OAuth2Authorization authorization = this.authorizationService.findByToken(accessToken,
				OAuth2TokenType.ACCESS_TOKEN);
		assertThat(authorization).isNotNull();
		OAuth2AccessToken storedAccessToken = authorization.getAccessToken().getToken();
		Map<String, Object> claims = authorization.getAccessToken().getClaims();
		assertThat(storedAccessToken.getTokenValue()).isEqualTo(accessToken);
		assertThat(claims).containsEntry(AuthConstants.CLIENT_ID, AuthorizationServerExampleConfiguration.CLIENT_ID)
			.containsEntry(AuthConstants.ACTIVE, Boolean.TRUE)
			.containsEntry(AuthConstants.DETAILS_LICENSE, "LC-7616f11");
	}

	private RestClient restClient() {
		return RestClient.create(baseUrl());
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

	private MultiValueMap<String, String> tokenRequest() {
		MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
		request.add("grant_type", "client_credentials");
		request.add("scope", AuthorizationServerExampleConfiguration.SCOPE);
		return request;
	}

	private String basicAuthorization() {
		String credentials = AuthorizationServerExampleConfiguration.CLIENT_ID + ":"
				+ AuthorizationServerExampleConfiguration.CLIENT_SECRET;
		return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}

}
