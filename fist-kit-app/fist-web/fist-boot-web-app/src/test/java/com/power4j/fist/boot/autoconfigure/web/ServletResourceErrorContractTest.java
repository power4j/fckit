package com.power4j.fist.boot.autoconfigure.web;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServletResourceErrorContractTest {

	private final HttpClient client = HttpClient.newBuilder()
		.proxy(ProxySelector.of(null))
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	private ConfigurableApplicationContext context;

	private int port;

	@BeforeAll
	void start() {
		this.context = SpringApplication.run(TestApplication.class, "--server.address=127.0.0.1", "--server.port=0");
		this.port = this.context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
	}

	@AfterAll
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void shouldServeExistingStaticResource() throws Exception {
		HttpResponse<String> response = get("/fist-404-contract/present.txt");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body().strip()).isEqualTo("static-resource-present");
	}

	@Test
	void shouldReturnKitNotFoundForMissingStaticResource() throws Exception {
		HttpResponse<String> response = get("/fist-404-contract/missing.txt");
		assertThat(response.statusCode()).isEqualTo(404);
		JsonNode body = new JsonMapper().readTree(response.body());
		assertThat(body.path("code").asString()).isEqualTo("A9900");
		assertThat(body.path("message").asString()).isEqualTo("请求资源不存在");
		assertThat(body.path("data").isNull()).isTrue();
		assertThat(body.path("metaInfo").path("hint").asString()).isEqualTo("GET fist-404-contract/missing.txt");
	}

	@Test
	void shouldKeepKitServerErrorForUnhandledException() throws Exception {
		HttpResponse<String> response = get("/fist-404-contract/failure");
		assertThat(response.statusCode()).isEqualTo(500);
		JsonNode body = new JsonMapper().readTree(response.body());
		assertThat(body.path("code").asString()).isEqualTo("B0001");
	}

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + this.port + path))
			.header(HttpHeaders.ACCEPT, "application/json, text/plain")
			.timeout(Duration.ofSeconds(20))
			.GET()
			.build();
		return this.client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@Import(FailureController.class)
	static class TestApplication {

	}

	@RestController
	static class FailureController {

		@GetMapping("/fist-404-contract/failure")
		void fail() {
			throw new IllegalStateException("test server error");
		}

	}

}
