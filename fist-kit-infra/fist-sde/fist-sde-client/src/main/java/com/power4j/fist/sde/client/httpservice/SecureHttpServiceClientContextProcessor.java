package com.power4j.fist.sde.client.httpservice;

import com.power4j.fist.sde.client.SecureExchangeClientContext;
import com.power4j.fist.sde.core.annotation.SecureExchange;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.lang.reflect.Method;

/**
 * Spring HTTP Service Clients 的 SDE 上下文处理器。
 * <p>
 * 处理器读取接口方法或接口类型上的 {@link SecureExchange}，并将策略上下文写入请求属性，供底层 RestClient 或 WebClient 适配器使用。
 */
public class SecureHttpServiceClientContextProcessor implements HttpRequestValues.Processor {

	@Override
	public void process(Method method, MethodParameter[] parameters, Object[] arguments,
			HttpRequestValues.Builder requestValues) {
		SecureExchange annotation = findSecureExchange(method);
		if (annotation == null) {
			return;
		}
		requestValues.addAttribute(SecureHttpServiceSupport.CLIENT_CONTEXT_ATTRIBUTE, clientContext(annotation));
	}

	private @Nullable SecureExchange findSecureExchange(Method method) {
		if (method == null) {
			return null;
		}
		SecureExchange annotation = method.getAnnotation(SecureExchange.class);
		return annotation == null ? method.getDeclaringClass().getAnnotation(SecureExchange.class) : annotation;
	}

	private SecureExchangeClientContext clientContext(SecureExchange annotation) {
		String policyId = hasText(annotation.value()) ? annotation.value() : null;
		return new SecureExchangeClientContext(policyId, null);
	}

	private boolean hasText(@Nullable String value) {
		return value != null && value.trim().length() > 0;
	}

}
