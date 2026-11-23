package com.power4j.fist.autoconfigure.gateway;

import com.power4j.fist.cloud.security.oauth2.client.UserIntrospectClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerBeanPostProcessorAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

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

	@Test
	void shouldSendLoadBalancedIntrospectionToDiscoveredInstanceWithOriginalEncoding() throws IOException {
		assertIntrospectionRequest(true);
	}

	@Test
	void shouldSendDirectIntrospectionWithOriginalEncoding() throws IOException {
		assertIntrospectionRequest(false);
	}

	private void assertIntrospectionRequest(boolean loadBalanced) throws IOException {
		AtomicReference<IntrospectionRequest> received = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", (exchange) -> {
			try (exchange) {
				received.set(new IntrospectionRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
						exchange.getRequestHeaders().getFirst("Authorization"),
						new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
				byte[] response = "{\"active\":false}".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, response.length);
				exchange.getResponseBody().write(response);
			}
		});
		server.start();
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			String instance = "http://127.0.0.1:" + server.getAddress().getPort();
			String target = "/oauth2/%69ntrospect?scope=a%2Fb%20c&part=%252F";
			String endpoint = (loadBalanced ? "lb://introspection-service" : instance) + target;
			TestPropertyValues
				.of("spring.cloud.discovery.client.simple.instances.introspection-service[0].uri=" + instance,
						"fist.oauth2.resourceserver.opaque-token.introspection-uri=" + endpoint,
						"fist.oauth2.resourceserver.opaque-token.client-id=test-client",
						"fist.oauth2.resourceserver.opaque-token.client-secret=test-secret")
				.applyTo(context);
			context.register(UtilAutoConfiguration.class, SimpleDiscoveryClientAutoConfiguration.class,
					LoadBalancerAutoConfiguration.class, ReactorLoadBalancerClientAutoConfiguration.class,
					LoadBalancerBeanPostProcessorAutoConfiguration.class, Oauth2Configuration.class);
			context.refresh();
			assertThat(context.getBean(UserIntrospectClient.class)
				.loadUser("test-token")
				.blockOptional(Duration.ofSeconds(10))).isEmpty();
			assertThat(received.get()).isEqualTo(new IntrospectionRequest("POST", target,
					"Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=", "token=test-token"));
		}
		finally {
			server.stop(0);
		}
	}

	private record IntrospectionRequest(String method, String target, String authorization, String body) {
	}

}
