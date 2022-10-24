package com.power4j.fist.message.topic;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public interface TopicPublisher {

	/**
	 * publish msg
	 * @param channel channel name
	 * @param msg the msg
	 */
	void publish(String channel, Object msg);

}
