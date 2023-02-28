package com.power4j.fist.cloud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.fist.boot.common.error.ErrorCode;
import com.power4j.fist.support.spring.web.reactive.util.ServerHttpResponseUtil;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
public class DefaultAccessDeniedHandler implements AccessDeniedHandler {

	static final String KEY_CODE = "code";
	static final String KEY_MESSAGE = "message";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Mono<Void> handleAccessDenied(ServerWebExchange exchange, AuthProblem problem) {
		ServerHttpResponse response = ServerHttpResponseUtil.requireWriteable(exchange.getResponse());
		Map<String, Object> payload = new HashMap<>(4);
		payload.put(KEY_CODE, ErrorCode.A0301);
		payload.put(KEY_MESSAGE, String.format("Access denied(%s)", problem.getCode()));
		return ServerHttpResponseUtil
			.responseWithJsonObject(response, objectMapper, payload, translateHttpStatus(problem))
			.then(Mono.defer(() -> exchange.getResponse().setComplete()));
	}

	static HttpStatus translateHttpStatus(AuthProblem problem) {
		if (problem.codeEquals(AuthProblem.HTTP_PROTOCOL.getCode())) {
			return HttpStatus.NOT_IMPLEMENTED;
		}
		return HttpStatus.FORBIDDEN;
	}

}
