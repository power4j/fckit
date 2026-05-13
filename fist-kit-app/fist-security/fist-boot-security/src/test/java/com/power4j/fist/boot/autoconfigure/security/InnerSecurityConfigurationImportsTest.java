package com.power4j.fist.boot.autoconfigure.security;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import com.power4j.fist.boot.autoconfigure.common.CommonConfiguration;
import com.power4j.fist.boot.autoconfigure.data.DataAutoConfiguration;
import com.power4j.fist.boot.autoconfigure.data.WebMvcConfiguration;
import com.power4j.fist.boot.autoconfigure.web.FistWebAutoConfiguration;
import com.power4j.fist.boot.mon.info.TraceInfoResolver;
import com.power4j.fist.boot.security.core.UserInfoSupplier;
import com.power4j.fist.boot.security.inner.UserDecoder;
import com.power4j.fist.data.tenant.TenantInvokeAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class InnerSecurityConfigurationImportsTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CommonConfiguration.class, FistWebAutoConfiguration.class,
				DataAutoConfiguration.class, WebMvcConfiguration.class, InnerSecurityConfiguration.class));

	@Test
	void shouldRegisterInnerSecurityAutoConfiguration() throws Exception {
		URL location = InnerSecurityConfiguration.class.getProtectionDomain().getCodeSource().getLocation();

		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { location }, null)) {
			List<String> candidates = ImportCandidates.load(AutoConfiguration.class, classLoader).getCandidates();

			assertThat(candidates).containsExactly(InnerSecurityConfiguration.class.getName());
		}
	}

	@Test
	void shouldComposeServletWebDataSecurityContext() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(TenantInvokeAspect.class);
			assertThat(context).hasSingleBean(PasswordEncoder.class);
			assertThat(context).hasSingleBean(UserDecoder.class);
			assertThat(context).hasSingleBean(UserInfoSupplier.class);
			assertThat(context).getBeans(TraceInfoResolver.class).hasSize(1);
			assertThat(context).hasBean("tenantContextFilter");
			assertThat(context).hasBean("headerMdcFilter");
			assertThat(context).hasBean("trustedUserFilter");
		});
	}

}
