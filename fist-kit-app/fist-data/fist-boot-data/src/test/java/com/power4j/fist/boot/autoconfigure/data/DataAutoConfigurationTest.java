package com.power4j.fist.boot.autoconfigure.data;

import com.power4j.fist.data.tenant.InTenantAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/8/26
 * @since 1.0
 */
class DataAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataAutoConfiguration.class));

	@Test
	void shouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(InTenantAspect.class).hasSize(1);
		});
	}

}