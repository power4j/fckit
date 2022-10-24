package com.power4j.fist.boot.mon;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/9/8
 * @since 1.0
 */
public interface ApiDescriptionResolver {

	/**
	 * API Description
	 * @param method The method
	 * @return return empty means no information found
	 */
	Optional<String> resolve(Method method);

}
