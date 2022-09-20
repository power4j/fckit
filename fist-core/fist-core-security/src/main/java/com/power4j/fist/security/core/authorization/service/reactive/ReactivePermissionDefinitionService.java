package com.power4j.fist.security.core.authorization.service.reactive;

import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/10
 * @since 1.0
 */
public interface ReactivePermissionDefinitionService<T extends PermissionDefinition> {

	/**
	 * 获取权限信息
	 * @param serviceName 服务名
	 * @param method 接口方法
	 * @return 无权限信息返回空集合
	 */
	Mono<List<T>> getPermissionDefinition(String serviceName, HttpMethod method);

}
