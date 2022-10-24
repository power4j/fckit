package com.power4j.fist.cloud.oauth2.server.resource.reactive;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/9
 * @since 1.0
 */
public interface AuthenticationProcessor {

	/**
	 * 处理认证信息
	 * @param origin 原始Web请求
	 * @param props 用户元信息
	 * @return 返回处理后的Web请求
	 */
	Mono<ServerWebExchange> process(ServerWebExchange origin, Map<String, Object> props);

}
