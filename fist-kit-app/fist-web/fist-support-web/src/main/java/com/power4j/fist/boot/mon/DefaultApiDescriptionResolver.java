package com.power4j.fist.boot.mon;

import com.power4j.fist.boot.mon.annotation.ApiLog;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/9/8
 * @since 1.0
 */
public class DefaultApiDescriptionResolver implements ApiDescriptionResolver {

	@Override
	public Optional<String> resolve(Method method) {
		ApiLog annotation = AnnotationUtils.findAnnotation(method, ApiLog.class);
		return Optional.ofNullable(annotation).map(a -> (String) AnnotationUtils.getValue(a));
	}

}
