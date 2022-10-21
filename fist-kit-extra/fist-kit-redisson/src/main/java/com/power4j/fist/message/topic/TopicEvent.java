package com.power4j.fist.message.topic;

import lombok.Getter;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Getter
public class TopicEvent<T> {

	/**
	 * Topic name
	 */
	private final CharSequence name;

	/**
	 * Event payload
	 */
	private final T payload;

	public TopicEvent(CharSequence name, T payload) {
		this.name = name;
		this.payload = payload;
	}

}
