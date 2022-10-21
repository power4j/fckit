package com.power4j.fist.redisson.topic;

import com.power4j.fist.message.topic.TopicPublisher;
import com.power4j.fist.redisson.common.NameGenerator;
import com.power4j.fist.redisson.common.RedisNamespace;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.springframework.lang.Nullable;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@RequiredArgsConstructor
public class DefaultTopicPublisher implements TopicPublisher {

	private final NameGenerator nameGenerator;

	private final RedissonClient redisson;

	@Nullable
	private final Codec codec;

	@Override
	public void publish(String channel, Object msg) {
		createTopic(makeChannelName(channel)).publish(msg);
	}

	protected String makeChannelName(String origin) {
		return nameGenerator.generate(RedisNamespace.TOPIC, origin);
	}

	protected RTopic createTopic(String channel) {
		if (null == codec) {
			return redisson.getTopic(channel);
		}
		else {
			return redisson.getTopic(channel, codec);
		}
	}

}
