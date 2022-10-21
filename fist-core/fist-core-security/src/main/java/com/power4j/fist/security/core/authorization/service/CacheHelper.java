package com.power4j.fist.security.core.authorization.service;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/10
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class CacheHelper {

	public String makeCacheKey(String serviceName, HttpMethod method) {
		return serviceName + "::" + method.name();
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> loadFromCache(@Nullable Cache cache, String serviceName, HttpMethod method) {
		if (Objects.isNull(cache)) {
			return Optional.empty();
		}
		String key = makeCacheKey(serviceName, method);
		try {
			return Optional.ofNullable(cache.get(key)).map(o -> (T) (o.get()));
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return Optional.empty();
		}
	}

	public <T> void updateCache(@Nullable Cache cache, String serviceName, HttpMethod method, List<T> data) {
		if (Objects.nonNull(cache)) {
			cache.put(makeCacheKey(serviceName, method), data);
		}
	}

	public boolean removeCache(@Nullable Cache cache, String serviceName, HttpMethod method) {
		if (Objects.nonNull(cache)) {
			return cache.evictIfPresent(makeCacheKey(serviceName, method));
		}
		return false;
	}

	public void removeCache(@Nullable Cache cache, String serviceName) {
		if (Objects.nonNull(cache)) {
			Arrays.stream(HttpMethod.values()).forEach(m -> cache.evict(makeCacheKey(serviceName, m)));
		}
	}

}
