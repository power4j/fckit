package com.power4j.fist.boot.autoconfigure.redisson.queue;

import com.power4j.fist.boot.common.prop.PropConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * For future use
 *
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = QueueProperties.PROP_PREFIX)
public class QueueProperties {

	public static final String PROP_ENTRY = "cloud.message.queue";

	public static final String PROP_PREFIX = PropConstant.PROP_ROOT_PREFIX + PROP_ENTRY;

	private List<QueueConfig> config;

	@Data
	public static class QueueConfig {

		private static String KEY_CODEC = "codec";

		private static String KEY_SIZE = "size";

		private static String KEY_EXPIRE = "expire";

		private String name;

		private Map<String, Object> parameters = Collections.emptyMap();

	}

}
