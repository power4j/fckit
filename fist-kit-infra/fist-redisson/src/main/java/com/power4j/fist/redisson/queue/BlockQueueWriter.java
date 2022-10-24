package com.power4j.fist.redisson.queue;

import com.power4j.fist.message.queue.QueueWriter;
import com.power4j.fist.redisson.common.NameGenerator;
import com.power4j.fist.redisson.common.RedisNamespace;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.springframework.lang.Nullable;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@RequiredArgsConstructor
public class BlockQueueWriter implements QueueWriter {

	private final NameGenerator nameGenerator;

	private final RedissonClient redisson;

	@Nullable
	private final Codec codec;

	@Override
	public <T> boolean write(String channel, T data) {
		RBlockingQueue<T> queue = createQueue(makeChannelName(channel));
		return queue.offer(data);
	}

	protected String makeChannelName(String origin) {
		return nameGenerator.generate(RedisNamespace.QUEUE, origin);
	}

	protected <T> RBlockingQueue<T> createQueue(String channel) {
		if (null == codec) {
			return redisson.getBlockingQueue(channel);
		}
		else {
			return redisson.getBlockingQueue(channel, codec);
		}
	}

}
