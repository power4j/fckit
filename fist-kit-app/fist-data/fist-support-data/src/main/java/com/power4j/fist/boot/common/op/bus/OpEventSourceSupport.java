package com.power4j.fist.boot.common.op.bus;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
public class OpEventSourceSupport<T, E extends OpEvent<T>> {

	private final Bus<T, E> bus;

	public OpEventSourceSupport(List<OpEventSubscriber<T, E>> subscribers) {
		bus = new Bus<>();
		bus.setSubscribers(subscribers);
	}

	public OpEventSourceSupport() {
		this(Collections.emptyList());
	}

	@Autowired(required = false)
	public void setSubscribers(List<OpEventSubscriber<T, E>> subscribers) {
		bus.setSubscribers(subscribers);
	}

	public OpEventSource<T, E> getEventSource() {
		return bus;
	}

}
