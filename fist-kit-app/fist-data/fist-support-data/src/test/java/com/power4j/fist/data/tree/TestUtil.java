package com.power4j.fist.data.tree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/9/8
 * @since 1.0
 */
@UtilityClass
public class TestUtil {

	private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public @Nullable String jsonPrettyString(@Nullable Object value) throws JsonProcessingException {
		if (Objects.isNull(value)) {
			return null;
		}
		return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
	}

}
