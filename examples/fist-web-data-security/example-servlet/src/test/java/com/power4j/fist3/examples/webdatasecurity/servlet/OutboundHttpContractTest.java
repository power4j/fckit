package com.power4j.fist3.examples.webdatasecurity.servlet;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import com.power4j.fist.jackson.annotation.Obfuscation;
import com.power4j.fist.jackson.support.obfuscation.SimpleStringObfuscate;
import com.power4j.fist.trace.context.TraceContexts;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundHttpContractTest {

	private static final String TRACE_HEADER = "X-REQ-UID";

	private static final String FEIGN_RELAY = "com.power4j.fist.cloud.autoconfigure.rpc.feign.FeignClientAutoConfiguration";

	private static final String REQUEST_JSON = """
			{"id":"9007199254740993","amount":"123.4500","at":"2026-01-02 03:04:05","masked":"OBF.XOR_V1$c3hsd2xgcm5r"}
			""";

	private static final String RESPONSE_JSON = """
			{"id":"9007199254740995","amount":"432.1050","at":"2026-02-03 04:05:06","masked":"OBF.XOR_V1$c3hsd2xgcm5r"}
			""";

	private static final Payload REQUEST = new Payload(9007199254740993L, new BigDecimal("123.4500"),
			LocalDateTime.of(2026, 1, 2, 3, 4, 5), "synthetic");

	private static final Payload RESPONSE = new Payload(9007199254740995L, new BigDecimal("432.1050"),
			LocalDateTime.of(2026, 2, 3, 4, 5, 6), "synthetic");

	@ParameterizedTest
	@EnumSource(TraceMode.class)
	void shouldSendAndDecodePlainJsonWithConfiguredTraceRelay(TraceMode mode) throws Exception {
		var captures = new LinkedBlockingQueue<CapturedRequest>();
		var receiver = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		receiver.createContext("/", (exchange) -> {
			try (exchange) {
				captures.add(new CapturedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
						exchange.getRequestHeaders().getFirst("Content-Type"),
						exchange.getRequestHeaders().getOrDefault(TRACE_HEADER, List.of()),
						new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
				byte[] body = RESPONSE_JSON.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		});
		receiver.start();
		try {
			var application = new SpringApplication(WebDataSecurityExampleApplication.class,
					ContractConfiguration.class);
			application.setRegisterShutdownHook(false);
			try (ConfigurableApplicationContext context = application.run("--server.address=127.0.0.1",
					"--server.port=0", "--contract.receiver=http://127.0.0.1:" + receiver.getAddress().getPort(),
					"--fist.trace-context.enabled=" + (mode != TraceMode.TRACE_OFF),
					"--spring.autoconfigure.exclude=" + (mode == TraceMode.FEIGN_RELAY_EXCLUDED ? FEIGN_RELAY : ""))) {
				String port = context.getEnvironment().getRequiredProperty("local.server.port");
				URI entry = URI.create("http://127.0.0.1:" + port + "/test/outbound-contract");
				HttpClient client = HttpClient.newHttpClient();
				assertExchange(client, entry, captures, mode, "REQ-EXTERNAL");
				assertExchange(client, entry, captures, mode, "");
			}
		}
		finally {
			receiver.stop(0);
		}
	}

	private void assertExchange(HttpClient client, URI entry, LinkedBlockingQueue<CapturedRequest> captures,
			TraceMode mode, String inboundTrace) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(entry).timeout(Duration.ofSeconds(15)).GET();
		if (!inboundTrace.isEmpty()) {
			request.header(TRACE_HEADER, inboundTrace);
		}
		HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
		String trace = response.body();
		if (mode == TraceMode.TRACE_OFF) {
			assertThat(trace).isEqualTo("absent");
		}
		else if (inboundTrace.isEmpty()) {
			assertThat(UUID.fromString(trace).toString()).isEqualTo(trace);
		}
		else {
			assertThat(trace).isEqualTo(inboundTrace);
		}

		assertThat(captures.stream().map(CapturedRequest::path)).containsExactly("/feign", "/restclient");
		JsonMapper parser = JsonMapper.builder().build();
		for (CapturedRequest captured : captures) {
			assertThat(captured.method()).isEqualTo("POST");
			assertThat(MediaType.parseMediaType(captured.contentType()).isCompatibleWith(MediaType.APPLICATION_JSON))
				.isTrue();
			assertThat(parser.readTree(captured.body())).isEqualTo(parser.readTree(REQUEST_JSON));
			boolean relay = mode == TraceMode.TRACE_ON
					|| (mode == TraceMode.FEIGN_RELAY_EXCLUDED && captured.path().equals("/restclient"));
			assertThat(captured.traceHeaders()).containsExactlyElementsOf(relay ? List.of(trace) : List.of());
		}
		captures.clear();
	}

	enum TraceMode {

		TRACE_ON, TRACE_OFF, FEIGN_RELAY_EXCLUDED

	}

	record CapturedRequest(String method, String path, String contentType, List<String> traceHeaders, String body) {
	}

	record Payload(Long id, BigDecimal amount, LocalDateTime at,
			@Obfuscation(mode = SimpleStringObfuscate.MODE_ID) String masked) {
	}

	@TestConfiguration(proxyBeanMethods = false)
	@EnableFeignClients(clients = ContractFeignClient.class)
	@Import(ContractController.class)
	static class ContractConfiguration {

	}

	@FeignClient(name = "outbound-contract", url = "${contract.receiver}")
	interface ContractFeignClient {

		@PostMapping(path = "/feign", consumes = MediaType.APPLICATION_JSON_VALUE)
		Payload create(@RequestBody Payload payload);

	}

	@RestController
	static class ContractController {

		private final ContractFeignClient feign;

		private final RestClient restClient;

		ContractController(ContractFeignClient feign, RestClient.Builder builder,
				@Value("${contract.receiver}") String receiver) {
			this.feign = feign;
			this.restClient = builder.baseUrl(receiver).build();
		}

		@GetMapping("/test/outbound-contract")
		String callClients() {
			assertThat(this.feign.create(REQUEST)).as("Feign response body").isEqualTo(RESPONSE);
			assertThat(this.restClient.post()
				.uri("/restclient")
				.contentType(MediaType.APPLICATION_JSON)
				.body(REQUEST)
				.retrieve()
				.body(Payload.class)).as("RestClient response body").isEqualTo(RESPONSE);
			return TraceContexts.current().flatMap((current) -> current.getValue("requestId")).orElse("absent");
		}

	}

}
