package com.power4j.fist.sde.client.httpservice;

import com.power4j.fist.sde.client.SecureExchangeClientContext;
import com.power4j.fist.sde.core.annotation.SecureExchange;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SecureHttpServiceClientContextProcessorTest {

	private final SecureHttpServiceClientContextProcessor processor = new SecureHttpServiceClientContextProcessor();

	@Test
	void shouldUseMethodPolicyBeforeTypePolicy() throws Exception {
		SecureExchangeClientContext context = process(TestHttpService.class.getDeclaredMethod("methodPolicy"));

		assertThat(context).isNotNull();
		assertThat(context.getPolicyId()).isEqualTo("method-policy");
	}

	@Test
	void shouldUseTypePolicyWhenMethodPolicyMissing() throws Exception {
		SecureExchangeClientContext context = process(TestHttpService.class.getDeclaredMethod("typePolicy"));

		assertThat(context).isNotNull();
		assertThat(context.getPolicyId()).isEqualTo("type-policy");
	}

	@Test
	void shouldIgnoreMethodWithoutSecureExchange() throws Exception {
		SecureExchangeClientContext fallback = new SecureExchangeClientContext("fallback", null);
		Method method = PlainHttpService.class.getDeclaredMethod("plain");
		HttpRequestValues.Builder builder = requestValuesBuilder();

		this.processor.process(method, new org.springframework.core.MethodParameter[0], new Object[0], builder);

		assertThat(SecureHttpServiceSupport.getClientContext(builder.build().getAttributes(), fallback))
			.isSameAs(fallback);
	}

	private SecureExchangeClientContext process(Method method) {
		HttpRequestValues.Builder builder = requestValuesBuilder();
		this.processor.process(method, new org.springframework.core.MethodParameter[0], new Object[0], builder);
		return SecureHttpServiceSupport.getClientContext(builder.build().getAttributes(), null);
	}

	private HttpRequestValues.Builder requestValuesBuilder() {
		return HttpRequestValues.builder().setHttpMethod(HttpMethod.POST).setUriTemplate("/orders");
	}

	@SecureExchange("type-policy")
	interface TestHttpService {

		void typePolicy();

		@SecureExchange("method-policy")
		void methodPolicy();

	}

	interface PlainHttpService {

		void plain();

	}

}
