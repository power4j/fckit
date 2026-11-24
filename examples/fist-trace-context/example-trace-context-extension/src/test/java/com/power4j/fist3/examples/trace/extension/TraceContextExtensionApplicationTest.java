package com.power4j.fist3.examples.trace.extension;

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
class TraceContextExtensionApplicationTest {

	@LocalServerPort
	private int port;

	@Test
	void shouldWriteTheCustomTenantToContextAndMdc(CapturedOutput output) {
		RestClient client = RestClient.create("http://127.0.0.1:" + this.port);
		JsonNode withTenant = client.get()
			.uri("/trace")
			.header("X-REQ-UID", "REQ-EXTENSION")
			.header("X-TENANT-ID", "tenant-a")
			.retrieve()
			.body(JsonNode.class);
		JsonNode withoutTenant = client.get().uri("/trace").retrieve().body(JsonNode.class);

		assertThat(withTenant.path("requestId").asString()).isEqualTo("REQ-EXTENSION");
		assertThat(withTenant.path("tenantId").asString()).isEqualTo("tenant-a");
		assertThat(withTenant.path("systemCode").asString()).isEqualTo("trace-extension");
		assertThat(output).contains("[REQ-EXTENSION,tenant-a,trace-extension,]");
		assertThat(withoutTenant.has("tenantId")).isFalse();
		assertThat(withoutTenant.path("requestId").asString()).isNotBlank().isNotEqualTo("REQ-EXTENSION");
		assertThat(withoutTenant.path("systemCode").asString()).isEqualTo("trace-extension");
	}

}
