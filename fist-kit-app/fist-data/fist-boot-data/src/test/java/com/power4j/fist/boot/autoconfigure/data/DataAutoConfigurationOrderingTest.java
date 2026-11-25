package com.power4j.fist.boot.autoconfigure.data;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验租户过滤器配置的自动配置排序声明包含 Boot 4.2 实际生效的类名
 *
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
class DataAutoConfigurationOrderingTest {

	@Test
	void tenantServletFilterConfigurationShouldOrderAfterBoot4SecurityFilterConfiguration() {
		AutoConfigureAfter annotation = DataAutoConfiguration.TenantServletFilterConfiguration.class
			.getAnnotation(AutoConfigureAfter.class);

		assertThat(annotation).isNotNull();
		assertThat(annotation.name())
			.contains("org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration");
	}

}
