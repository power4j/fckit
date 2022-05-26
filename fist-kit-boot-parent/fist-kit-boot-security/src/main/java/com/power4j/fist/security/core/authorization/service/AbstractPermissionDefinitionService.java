package com.power4j.fist.security.core.authorization.service;

import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/26
 * @since 1.0
 */
@Slf4j
public abstract class AbstractPermissionDefinitionService<T extends PermissionDefinition>
		implements PermissionDefinitionService<T> {

	@Override
	public List<T> getPermissionDefinition(String serviceName, HttpMethod method) {
		// @formatter:off
		return loadFromCache(serviceName,method)
				.orElseGet(() -> {
					List<T> list = fetch(serviceName, method).orElse(null);
					if(Objects.nonNull(list)){
						updateCache(serviceName,method,list);
					}
					return list;
				});
		// @formatter:on
	}

	protected String makeCacheKey(String serviceName, HttpMethod method) {
		return serviceName + "::" + method.name();
	}

	@SuppressWarnings("unchecked")
	protected Optional<List<T>> loadFromCache(String serviceName, HttpMethod method) {
		Cache cache = getCache().orElse(null);
		if (Objects.isNull(cache)) {
			return Optional.empty();
		}
		String key = makeCacheKey(serviceName, method);
		try {
			return Optional.ofNullable(cache.get(key)).map(o -> (List<T>) (o.get()));
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return Optional.empty();
		}
	}

	protected void updateCache(String serviceName, HttpMethod method, List<T> data) {
		getCache().ifPresent(cache -> cache.put(makeCacheKey(serviceName, method), data));
	}

	/**
	 * 获取数据
	 * @param serviceName
	 * @param method
	 * @return 无数据返回 empty
	 */
	protected abstract Optional<List<T>> fetch(String serviceName, HttpMethod method);

	/**
	 * Cache
	 * @return 返回empty表示关闭缓存
	 */
	protected abstract Optional<Cache> getCache();

}
