package com.power4j.fist3.examples.trace.basic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.address=127.0.0.1")
@ExtendWith(OutputCaptureExtension.class)
class TraceContextBasicApplicationTest {

	@LocalServerPort
	private int port;

	@Test
	void shouldPropagateRequestIdAndScopeTheMethodSpan(CapturedOutput output) {
		JsonNode body = traceClient().get()
			.uri("/trace")
			.header("X-REQ-UID", "REQ-BASIC")
			.retrieve()
			.body(JsonNode.class);

		assertThat(body.path("values").path("requestId").asString()).isEqualTo("REQ-BASIC");
		assertThat(body.path("values").path("systemCode").asString()).isEqualTo("trace-basic");
		JsonNode spanId = body.path("spanId");
		assertThat(spanId.isString()).isTrue();
		assertThat(spanId.asString()).isEmpty();
		assertThat(output).contains("[REQ-BASIC,trace-basic,basic.work]");
	}

	@Test
	void shouldGenerateADistinctRequestIdForEachRequestWithoutAHeader() {
		RestClient client = traceClient();
		JsonNode first = client.get().uri("/trace").retrieve().body(JsonNode.class);
		JsonNode second = client.get().uri("/trace").retrieve().body(JsonNode.class);
		String firstId = first.path("values").path("requestId").asString();
		String secondId = second.path("values").path("requestId").asString();

		assertThat(firstId).isNotBlank();
		assertThat(secondId).isNotBlank().isNotEqualTo(firstId);
	}

	private RestClient traceClient() {
		return RestClient.create("http://127.0.0.1:" + this.port);
	}

}
