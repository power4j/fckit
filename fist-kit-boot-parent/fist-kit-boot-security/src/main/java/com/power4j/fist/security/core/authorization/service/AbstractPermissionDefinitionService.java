package com.power4j.fist.security.core.authorization.service;

import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.List;
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
					List<T> list = fetch(serviceName, method).orElse(Collections.emptyList());
					if(!list.isEmpty()){
						updateCache(serviceName,method,list);
					}
					return list;
				});
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	protected Optional<List<T>> loadFromCache(String serviceName, HttpMethod method) {
		return CacheHelper.loadFromCache(getCache().orElse(null), serviceName, method);
	}

	protected void updateCache(String serviceName, HttpMethod method, List<T> data) {
		CacheHelper.updateCache(getCache().orElse(null), serviceName, method, data);
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
