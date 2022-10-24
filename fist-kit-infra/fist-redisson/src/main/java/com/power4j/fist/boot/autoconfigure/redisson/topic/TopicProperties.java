package com.power4j.fist.boot.autoconfigure.redisson.topic;

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
@ConfigurationProperties(prefix = TopicProperties.PROP_PREFIX)
public class TopicProperties {

	public static final String PROP_ENTRY = "cloud.message.topic";

	public static final String PROP_PREFIX = PropConstant.PROP_ROOT_PREFIX + PROP_ENTRY;

	private List<TopicConfig> config;

	@Data
	public static class TopicConfig {

		private static String KEY_CODEC = "codec";

		private String name;

		private Map<String, Object> parameters = Collections.emptyMap();

	}

}
