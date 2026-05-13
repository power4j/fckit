package com.power4j.fist.boot.autoconfigure.apidoc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.power4j.fist.boot.security.core.SecurityConstant;

import static org.assertj.core.api.Assertions.assertThat;

class SpringDocAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SpringDocAutoConfiguration.class));

	@Test
	void shouldRegisterOpenApiBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OperationCustomizer.class).hasSingleBean(OpenAPI.class);
			OpenAPI openApi = context.getBean(OpenAPI.class);
			assertThat(openApi.getSecurity()).singleElement()
				.satisfies((requirement) -> assertThat(requirement.keySet()).containsExactly("inner_auth"));
			SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get("inner_auth");
			assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
			assertThat(scheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
			assertThat(scheme.getName()).isEqualTo(SecurityConstant.HEADER_USER_TOKEN_INNER);
		});
	}

}
