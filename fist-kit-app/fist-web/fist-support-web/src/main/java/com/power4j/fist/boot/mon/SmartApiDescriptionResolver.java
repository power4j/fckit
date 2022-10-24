package com.power4j.fist.boot.mon;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/9/8
 * @since 1.0
 */
public class SmartApiDescriptionResolver extends DefaultApiDescriptionResolver {

	@Override
	public Optional<String> resolve(Method method) {
		return super.resolve(method).or(() -> Optional.ofNullable(getDescription(method)));
	}

	@Nullable
	private String getDescription(Method method) {
		final Operation operation = method.getAnnotation(Operation.class);
		if (Objects.nonNull(operation)) {
			return ObjectUtils.firstNonNull(operation.summary(), operation.description());
		}
		return null;
	}

}
