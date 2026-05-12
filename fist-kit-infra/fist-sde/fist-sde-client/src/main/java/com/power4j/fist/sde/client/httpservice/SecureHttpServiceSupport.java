package com.power4j.fist.sde.client.httpservice;

import com.power4j.fist.sde.client.SecureExchangeClientContext;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Spring HTTP Service Clients 与底层 HTTP 客户端之间传递 SDE 上下文的工具。
 */
public final class SecureHttpServiceSupport {

	/**
	 * 请求属性中的 SDE 客户端上下文 key。
	 */
	public static final String CLIENT_CONTEXT_ATTRIBUTE = SecureExchangeClientContext.class.getName();

	private SecureHttpServiceSupport() {
	}

	/**
	 * 从请求属性中解析 SDE 客户端上下文。
	 * @param attributes 请求属性
	 * @param fallback 默认上下文
	 * @return 请求级上下文；不存在时返回默认上下文
	 */
	public static @Nullable SecureExchangeClientContext getClientContext(@Nullable Map<String, Object> attributes,
			@Nullable SecureExchangeClientContext fallback) {
		Object value = attributes == null ? null : attributes.get(CLIENT_CONTEXT_ATTRIBUTE);
		return value instanceof SecureExchangeClientContext ? (SecureExchangeClientContext) value : fallback;
	}

}
