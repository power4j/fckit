package com.power4j.fist.autoconfigure.oas;

import com.power4j.fist.auth.handler.DefaultFormAuthenticationFailureHandler;
import com.power4j.fist.auth.handler.DefaultSsoLogoutSuccessHandler;
import com.power4j.fist.autoconfigure.oas.annotation.Oauth2AuthorizationServer;
import com.power4j.fist.oauth2.extension.token.OauthUserPropExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AuthorizationServerAutoConfiguration.class));

	@Test
	void shouldRegisterDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OauthUserPropExporter.class)
				.hasSingleBean(AuthenticationFailureHandler.class)
				.hasSingleBean(LogoutSuccessHandler.class)
				.hasSingleBean(AuthenticationConverter.class)
				.hasSingleBean(OAuth2TokenGenerator.class);
			assertThat(context.getBean(OauthUserPropExporter.class)).isSameAs(OauthUserPropExporter.NO_OP);
			assertThat(context.getBean(AuthenticationFailureHandler.class))
				.isInstanceOf(DefaultFormAuthenticationFailureHandler.class);
			assertThat(context.getBean(LogoutSuccessHandler.class)).isInstanceOf(DefaultSsoLogoutSuccessHandler.class);
			assertThat(context.getBean(AuthenticationConverter.class))
				.isInstanceOf(DelegatingAuthenticationConverter.class);
			assertThat(context.getBean("tokenGenerator")).isInstanceOf(DelegatingOAuth2TokenGenerator.class);
		});
	}

	@Test
	void shouldImportAutoConfigurationFromAnnotation() {
		new ApplicationContextRunner().withUserConfiguration(ManualAuthorizationServerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AuthenticationConverter.class)
					.hasSingleBean(OAuth2TokenGenerator.class);
				assertThat(context.getBean(OAuth2TokenGenerator.class))
					.isInstanceOf(DelegatingOAuth2TokenGenerator.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	@Oauth2AuthorizationServer
	static class ManualAuthorizationServerConfiguration {

	}

}
