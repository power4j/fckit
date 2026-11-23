package com.power4j.fist3.examples.webdatasecurity.servlet;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.power4j.fist.jackson.annotation.Obfuscation;
import com.power4j.fist.jackson.support.obfuscation.SimpleStringObfuscate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.assertj.core.api.Assertions.assertThat;

class MvcJacksonContractTest {

	private static final String PATH = "/test/jackson-contract";

	private static final Contract SAMPLE = new Contract(9007199254740993L, new BigDecimal("123.4500"),
			LocalDateTime.of(2026, 1, 2, 3, 4, 5), Date.from(Instant.parse("2026-01-02T03:04:05Z")), "synthetic",
			"ready");

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldPreserveJsonContractOverHttp(boolean override) throws Exception {
		List<String> properties = new ArrayList<>(List.of("--server.address=127.0.0.1", "--server.port=0"));
		if (override) {
			properties.addAll(List.of("--fist.jackson.customize.exclude-modules[0]=NumberToStr",
					"--fist.jackson.customize.simple-date-format=yyyy/MM/dd HH:mm:ss",
					"--fist.jackson.customize.time-zone-id=GMT+05:00"));
		}
		SpringApplication application = new SpringApplication(WebDataSecurityExampleApplication.class,
				ContractConfiguration.class);
		application.setRegisterShutdownHook(false);
		try (ConfigurableApplicationContext context = application.run(properties.toArray(String[]::new))) {
			String port = context.getEnvironment().getRequiredProperty("local.server.port");
			URI uri = URI.create("http://127.0.0.1:" + port + PATH);
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(10))
				.header("Accept", "application/json");
			HttpResponse<String> get = client.send(requestBuilder.copy().GET().build(),
					HttpResponse.BodyHandlers.ofString());
			String expectedDate = override ? "2026/01/02 08:04:05"
					: DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
						.withZone(ZoneId.systemDefault())
						.format(SAMPLE.legacyDate().toInstant());
			String expectedId = override ? "9007199254740993" : "\"9007199254740993\"";
			String expectedAmount = override ? "123.4500" : "\"123.4500\"";
			String request = """
					{"id":%s,"amount":%s,"localDateTime":"2026-01-02 03:04:05","legacyDate":"%s",
					"masked":"OBF.XOR_V1$c3hsd2xgcm5r","code":"ready"}
					""".formatted(expectedId, expectedAmount, expectedDate);
			HttpResponse<String> post = client.send(requestBuilder.copy()
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(request))
				.build(), HttpResponse.BodyHandlers.ofString());

			String reader = post.headers().firstValue("X-Contract-Reader").orElse("");
			String writer = get.headers().firstValue("X-Contract-Writer").orElse("");
			HttpMessageConverter<?> converter = context.getBean(RequestMappingHandlerAdapter.class)
				.getMessageConverters()
				.stream()
				.filter((candidate) -> candidate.canWrite(Contract.class, MediaType.APPLICATION_JSON))
				.findFirst()
				.orElseThrow();
			String mapper = selectedMapper(converter).getClass().getName();
			System.out.printf("MVC Jackson Boot=%s override=%s reader=%s writer=%s mapper=%s GET=%d %s POST=%d %s%n",
					SpringBootVersion.getVersion(), override, reader, writer, mapper,
					get.statusCode(), get.body(), post.statusCode(), post.body());

			assertThat(get.statusCode()).isEqualTo(200);
			assertThat(post.statusCode()).isEqualTo(200);
			assertThat(reader).isNotEmpty();
			assertThat(writer).isEqualTo(reader);
			assertThat(writer).isEqualTo(converter.getClass().getName());
			for (HttpResponse<String> response : List.of(get, post)) {
				assertThat(response.body()).containsPattern("\"id\":" + Pattern.quote(expectedId) + "[,}]")
					.containsPattern("\"amount\":" + Pattern.quote(expectedAmount) + "[,}]")
					.contains(
						"\"localDateTime\":\"2026-01-02 03:04:05\"", "\"legacyDate\":\"" + expectedDate + "\"",
						"\"masked\":\"OBF.XOR_V1$c3hsd2xgcm5r\"", "\"code\":\"ready\"");
			}
		}
	}

	private static Object selectedMapper(HttpMessageConverter<?> converter) throws ReflectiveOperationException {
		// 两代 MVC converter 的 mapper 访问方法不同，同一探针兼容两代。
		try {
			return converter.getClass().getMethod("getMapper").invoke(converter);
		}
		catch (NoSuchMethodException ex) {
			return converter.getClass().getMethod("getObjectMapper").invoke(converter);
		}
	}

	record Contract(Long id, BigDecimal amount, LocalDateTime localDateTime, Date legacyDate,
			@Obfuscation(mode = SimpleStringObfuscate.MODE_ID) String masked, String code) {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ ContractController.class, ContractAdvice.class })
	static class ContractConfiguration {
	}

	@RestController
	static class ContractController {

		@GetMapping(PATH)
		Contract read() {
			return SAMPLE;
		}

		@PostMapping(PATH)
		Contract echo(@RequestBody Contract value) {
			if (!SAMPLE.equals(value)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decoded DTO differs from the sample");
			}
			return value;
		}

	}

	@ControllerAdvice(assignableTypes = ContractController.class)
	static class ContractAdvice extends RequestBodyAdviceAdapter implements ResponseBodyAdvice<Contract> {

		@Override
		public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
			return true;
		}

		@Override
		public Object afterBodyRead(Object body, HttpInputMessage input, MethodParameter parameter, Type targetType,
				Class<? extends HttpMessageConverter<?>> converter) {
			RequestContextHolder.currentRequestAttributes()
				.setAttribute("contractReader", converter.getName(), RequestAttributes.SCOPE_REQUEST);
			return body;
		}

		@Override
		public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converter) {
			return true;
		}

		@Override
		public Contract beforeBodyWrite(Contract body, MethodParameter returnType, MediaType contentType,
				Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request,
				ServerHttpResponse response) {
			response.getHeaders().set("X-Contract-Writer", converter.getName());
			Object reader = RequestContextHolder.currentRequestAttributes()
				.getAttribute("contractReader", RequestAttributes.SCOPE_REQUEST);
			if (reader instanceof String type) {
				response.getHeaders().set("X-Contract-Reader", type);
			}
			return body;
		}

	}

}
