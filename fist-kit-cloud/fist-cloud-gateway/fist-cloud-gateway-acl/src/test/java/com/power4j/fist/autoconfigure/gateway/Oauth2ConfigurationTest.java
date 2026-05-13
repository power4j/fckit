package com.power4j.fist.autoconfigure.gateway;

import com.power4j.fist.cloud.security.oauth2.client.UserIntrospectClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class Oauth2ConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Oauth2Configuration.class));

	@Test
	void shouldStayDisabledWhenIntrospectionUriIsMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(UserIntrospectClient.class);
			assertThat(context).hasBean("lbWebClientBuilder");
		});
	}

	@Test
	void shouldCreateUserIntrospectClientWhenIntrospectionUriIsConfigured() {
		this.contextRunner
			.withPropertyValues("fist.oauth2.resourceserver.opaque-token.introspection-uri=http://localhost/introspect",
					"fist.oauth2.resourceserver.opaque-token.client-id=test-client",
					"fist.oauth2.resourceserver.opaque-token.client-secret=test-secret")
			.run((context) -> {
				assertThat(context).hasSingleBean(UserIntrospectClient.class);
				assertThat(context).getBean("lbWebClientBuilder").isInstanceOf(WebClient.Builder.class);
			});
	}

}
