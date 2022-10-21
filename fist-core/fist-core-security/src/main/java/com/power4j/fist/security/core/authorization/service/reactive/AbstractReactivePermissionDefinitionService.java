package com.power4j.fist.security.core.authorization.service.reactive;

import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.service.CacheHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.cache.Cache;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/10
 * @since 1.0
 */
public abstract class AbstractReactivePermissionDefinitionService<T extends PermissionDefinition>
		implements ReactivePermissionDefinitionService<T> {

	@Override
	public Mono<List<T>> getPermissionDefinition(String serviceName, HttpMethod method) {
		// @formatter:off
		return Mono.justOrEmpty(loadFromCache(serviceName,method))
				.switchIfEmpty(
						fetch(serviceName, method)
								.doOnNext(o -> updateCache(serviceName,method,o))
								.switchIfEmpty(Mono.just(Collections.emptyList()))
				);
		// @formatter:on
	}

	@Nullable
	@SuppressWarnings("unchecked")
	protected List<T> loadFromCache(String serviceName, HttpMethod method) {
		return (List<T>) CacheHelper.loadFromCache(getCache().orElse(null), serviceName, method).orElse(null);
	}

	protected void updateCache(String serviceName, HttpMethod method, List<T> data) {
		if (ObjectUtils.isNotEmpty(data)) {
			CacheHelper.updateCache(getCache().orElse(null), serviceName, method, data);
		}
	}

	protected void removeCache(String serviceName, HttpMethod method) {
		CacheHelper.removeCache(getCache().orElse(null), serviceName, method);
	}

	protected void removeCache(String serviceName) {
		CacheHelper.removeCache(getCache().orElse(null), serviceName);
	}

	/**
	 * 获取数据
	 * @param serviceName
	 * @param method
	 * @return 无数据返回 empty
	 */
	protected abstract Mono<List<T>> fetch(String serviceName, HttpMethod method);

	/**
	 * Cache
	 * @return 返回empty表示关闭缓存
	 */
	protected abstract Optional<Cache> getCache();

}
