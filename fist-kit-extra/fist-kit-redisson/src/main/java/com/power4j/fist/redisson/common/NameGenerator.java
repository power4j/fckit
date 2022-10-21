package com.power4j.fist.redisson.common;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public interface NameGenerator {

	/**
	 * generate key name
	 * @param namespace namespace
	 * @param name original name
	 * @return name to use
	 */
	String generate(RedisNamespace namespace, String name);

}
