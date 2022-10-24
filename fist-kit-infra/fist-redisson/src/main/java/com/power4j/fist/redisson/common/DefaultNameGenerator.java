package com.power4j.fist.redisson.common;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public class DefaultNameGenerator implements NameGenerator {

	@Override
	public String generate(RedisNamespace namespace, String name) {
		return namespace.getValue() + "::" + name;
	}

}
