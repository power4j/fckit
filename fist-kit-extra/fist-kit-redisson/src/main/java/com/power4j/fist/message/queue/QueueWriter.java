package com.power4j.fist.message.queue;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public interface QueueWriter {

	/**
	 * Write data to queue
	 * @param channel channel name
	 * @param data the data
	 * @return true: write success; false: queue full
	 */
	<T> boolean write(String channel, T data);

}
