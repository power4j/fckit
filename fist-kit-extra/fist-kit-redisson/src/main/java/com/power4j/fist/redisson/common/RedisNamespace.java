package com.power4j.fist.redisson.common;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public enum RedisNamespace {

	/**
	 * One
	 */
	TOPIC("topic"),
	/**
	 * Two
	 */
	QUEUE("queue");

	private final String value;

	RedisNamespace(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Parse form value
	 * @param value the value
	 * @return return empty if parse fail
	 */
	public static Optional<RedisNamespace> parse(final String value) {
		if (value == null) {
			return Optional.empty();
		}
		for (RedisNamespace o : RedisNamespace.values()) {
			if (o.getValue().equals(value)) {
				return Optional.of(o);
			}
		}
		return Optional.empty();
	}

	/**
	 * Get RedisNamespace from value
	 * @param value the value
	 * @return 如果解析失败抛出 IllegalArgumentException
	 * @throws IllegalArgumentException The value is invalid
	 */
	public static RedisNamespace fromValue(final String value) throws IllegalArgumentException {
		return parse(value).orElseThrow(() -> new IllegalArgumentException("Invalid value : " + value));
	}

}
