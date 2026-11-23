package com.power4j.fist.jasypt.boot.autoconfigure;

import com.power4j.fist.jasypt.core.GmConfigCryptoException;
import com.power4j.fist.jasypt.core.GmTextEncryptor;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.util.Throwables.getStackTrace;

class FistJasyptApplicationTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldDecryptPropertyDuringApplicationStartupWithRawMasterKey() {
		String cipher = new GmTextEncryptor().encrypt("hmac-secret", "master-secret");
		SpringApplication application = new SpringApplication(TestApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		try (ConfigurableApplicationContext context = application.run("--fist.jasypt.enabled=true",
				"--fist.jasypt.master-key-source=raw", "--fist.jasypt.master-key-value=master-secret",
				"--fist.jasypt.master-key-file=missing.key", "--demo.hmac-key=" + cipher)) {
			assertThat(context.getBean("decryptedKey", String.class)).isEqualTo("hmac-secret");
			assertThat(context.getBean(DemoProperties.class).hmacKey()).isEqualTo("hmac-secret");
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldDecryptGmencFromConfigData(boolean usePayloadPlaceholder) throws Exception {
		String cipher = new GmTextEncryptor().encrypt("synthetic-value", "test-master");
		Path config = Files.writeString(this.tempDir.resolve("application.properties"), """
				fist.jasypt.enabled=true
				fist.jasypt.master-key-source=raw
				fist.jasypt.master-key-value=test-master
				demo.hmac-key=%s
				""".formatted(usePayloadPlaceholder ? "GMENC(${cipher.payload})" : cipher));
		SpringApplication application = new SpringApplication(TestApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		try (ConfigurableApplicationContext context = application.run("--spring.config.location=" + config.toUri(),
				"--cipher.payload=" + cipher.substring("GMENC(".length(), cipher.length() - 1))) {
			assertThat(context.getEnvironment().getProperty("demo.hmac-key")).isEqualTo("synthetic-value");
			assertThat(context.getBean("decryptedKey", String.class)).isEqualTo("synthetic-value");
			assertThat(context.getBean(DemoProperties.class).hmacKey()).isEqualTo("synthetic-value");
		}
	}

	@Test
	void shouldRejectCircularGmencPayloadReference() {
		SpringApplication application = new SpringApplication(TestApplication.class, CustomEncryptor.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		try (ConfigurableApplicationContext context = application.run("--fist.jasypt.enabled=true",
				"--spring.main.lazy-initialization=true", "--demo.hmac-key=GMENC(${cipher.payload})",
				"--cipher.payload=GMENC(${demo.hmac-key})")) {
			assertThatExceptionOfType(GmConfigCryptoException.class)
				.isThrownBy(() -> context.getEnvironment().getProperty("demo.hmac-key"))
				.withMessage("Failed to resolve GMENC payload placeholders")
				.withNoCause();
		}
	}

	@Test
	void shouldHideGmencPlaceholderFailureDetailsAndRecover() {
		String marker = "synthetic-config-value";
		Map<String, Object> properties = new HashMap<>();
		properties.put("cipher.payload", marker + "${cipher.payload}");
		SpringApplication application = new SpringApplication(TestApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addInitializers((context) -> context.getEnvironment()
			.getPropertySources()
			.addFirst(new MapPropertySource("syntheticCipher", properties)));
		try (ConfigurableApplicationContext context = application.run("--fist.jasypt.enabled=true",
				"--fist.jasypt.master-key-source=raw", "--fist.jasypt.master-key-value=test-master",
				"--spring.main.lazy-initialization=true", "--demo.hmac-key=GMENC(${cipher.payload})")) {
			Stream
				.of(catchThrowable(() -> context.getEnvironment().getProperty("demo.hmac-key")),
						catchThrowable(() -> context.getBean("decryptedKey", String.class)),
						catchThrowable(() -> context.getBean(DemoProperties.class)))
				.forEach((failure) -> {
					assertThat(failure).isNotNull();
					assertThat(getStackTrace(failure)).doesNotContain(marker);
					assertThat(NestedExceptionUtils.getMostSpecificCause(failure))
						.isExactlyInstanceOf(GmConfigCryptoException.class)
						.hasMessage("Failed to resolve GMENC payload placeholders")
						.hasNoCause();
				});

			String cipher = new GmTextEncryptor().encrypt("synthetic-value", "test-master");
			properties.put("cipher.payload", cipher.substring("GMENC(".length(), cipher.length() - 1));
			assertThat(context.getEnvironment().getProperty("demo.hmac-key")).isEqualTo("synthetic-value");
			assertThat(context.getBean("decryptedKey", String.class)).isEqualTo("synthetic-value");
			assertThat(context.getBean(DemoProperties.class).hmacKey()).isEqualTo("synthetic-value");
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldDecryptWithCustomBoundaryFromConfigData(boolean overrideDetector) throws Exception {
		String cipher = new GmTextEncryptor().encrypt("synthetic-value", "test-master", "FILE[", "]");
		Path config = Files.writeString(this.tempDir.resolve("application.properties"), """
				fist.jasypt.enabled=true
				fist.jasypt.master-key-source=raw
				fist.jasypt.master-key-value=test-master
				fist.jasypt.cipher-prefix=%s
				fist.jasypt.cipher-suffix=]
				demo.hmac-key=%s
				%s
				""".formatted(overrideDetector ? "OTHER[" : "FILE[", cipher,
				overrideDetector ? "jasypt.encryptor.property.prefix=FILE[" : ""));
		SpringApplication application = new SpringApplication(TestApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		try (ConfigurableApplicationContext context = application.run("--spring.config.location=" + config.toUri())) {
			assertThat(context.getEnvironment().getProperty("demo.hmac-key")).isEqualTo("synthetic-value");
			assertThat(context.getBean(DemoProperties.class).hmacKey()).isEqualTo("synthetic-value");
			assertThat(context.getBean("decryptedKey", String.class)).isEqualTo("synthetic-value");
		}
	}

	@Test
	void shouldUseApplicationEncryptorWithoutMasterKey() {
		SpringApplication application = new SpringApplication(TestApplication.class, CustomEncryptor.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		try (ConfigurableApplicationContext context = application.run("--fist.jasypt.enabled=true",
				"--demo.hmac-key=GMENC(synthetic)")) {
			assertThat(context.getBean("decryptedKey", String.class)).isEqualTo("synthetic");
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableConfigurationProperties(DemoProperties.class)
	static class TestApplication {

		@Bean
		String decryptedKey(@Value("${demo.hmac-key}") String value) {
			return value;
		}

	}

	@ConfigurationProperties("demo")
	record DemoProperties(String hmacKey) {
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomEncryptor {

		@Bean
		StringEncryptor jasyptStringEncryptor() {
			return new StringEncryptor() {
				@Override
				public String encrypt(String message) {
					return message;
				}

				@Override
				public String decrypt(String encryptedMessage) {
					return encryptedMessage;
				}
			};
		}

	}

}
