package com.power4j.fist.sde.boot.autoconfigure.client;

import com.power4j.fist.sde.client.SecureExchangeOperations;
import com.power4j.fist.sde.client.webclient.SecureWebClientExchangeFilterFunction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SdeBootWebClientAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withBean(SecureExchangeOperations.class, () -> mock(SecureExchangeOperations.class))
		.withUserConfiguration(ClientConfiguration.class)
		.withConfiguration(AutoConfigurations.of(SdeWebClientAutoConfiguration.class));

	@Test
	void shouldCustomizeBuilderProvidedByBootBeforeSde() {
		this.runner.withUserConfiguration(WebClientAutoConfiguration.class).run((context) -> {
			assertThat(context.getBeanFactory().isPrototype("webClientBuilder")).isTrue();
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			assertThat(filters(builder)).containsExactly(context.getBean(SecureWebClientExchangeFilterFunction.class));
		});
	}

	@Test
	void shouldPreserveBootPrototypeConnectorCodecsAndOtherCustomizers() {
		this.runner.withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class)).run((context) -> {
			WebClient.Builder first = context.getBean(WebClient.Builder.class);
			WebClient.Builder second = context.getBean(WebClient.Builder.class);
			assertThat(first).isNotSameAs(second);
			assertThat(context.getBeanFactory().isPrototype("webClientBuilder")).isTrue();
			assertThat(filters(first)).containsExactly(context.getBean(SecureWebClientExchangeFilterFunction.class));
			assertThat(filters(second)).containsExactly(context.getBean(SecureWebClientExchangeFilterFunction.class));
			assertThatThrownBy(() -> first.build()
				.get()
				.uri("https://synthetic.invalid/")
				.header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
				.retrieve()
				.bodyToMono(String.class)
				.block(Duration.ofSeconds(5))).hasRootCauseInstanceOf(DataBufferLimitException.class);
			assertThat(context.getBean(ClientConfiguration.class).request.get().getHeaders().getFirst("X-Customizer"))
				.isEqualTo("preserved");
		});
	}

	@Test
	void shouldNotCustomizeApplicationBuilderUnlessExplicitlyRequested() {
		this.runner.withUserConfiguration(ApplicationBuilderConfiguration.class).run((context) -> {
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			assertThat(filters(builder)).isEmpty();
			WebClientCustomizer customizer = context.getBean("secureSdeWebClientCustomizer", WebClientCustomizer.class);
			customizer.customize(builder);
			customizer.customize(builder);
			assertThat(filters(builder)).containsExactly(context.getBean(SecureWebClientExchangeFilterFunction.class));
			assertThat(filters(WebClient.builder())).isEmpty();
		});
	}

	private static List<Object> filters(WebClient.Builder builder) {
		List<Object> result = new ArrayList<>();
		builder.filters(result::addAll);
		return result;
	}

	@Configuration(proxyBeanMethods = false)
	static class ClientConfiguration {

		private final AtomicReference<MockClientHttpRequest> request = new AtomicReference<>();

		@Bean
		ClientHttpConnector clientHttpConnector() {
			return (method, uri, callback) -> {
				MockClientHttpRequest outgoing = new MockClientHttpRequest(method, uri);
				this.request.set(outgoing);
				MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
				response.setBody("codec-limit");
				return callback.apply(outgoing).then(Mono.just(response));
			};
		}

		@Bean
		CodecCustomizer codecCustomizer() {
			return (codecs) -> codecs.defaultCodecs().maxInMemorySize(4);
		}

		@Bean
		WebClientCustomizer applicationCustomizer() {
			return (builder) -> builder.defaultHeader("X-Customizer", "preserved");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ApplicationBuilderConfiguration {

		@Bean
		WebClient.Builder applicationBuilder() {
			return WebClient.builder();
		}

	}

}
