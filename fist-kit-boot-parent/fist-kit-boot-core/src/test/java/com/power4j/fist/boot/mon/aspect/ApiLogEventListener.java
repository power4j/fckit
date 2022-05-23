package com.power4j.fist.boot.mon.aspect;

import com.power4j.fist.boot.mon.event.ApiLogEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/23
 * @since 1.0
 */
@Component
public class ApiLogEventListener {

	private static ApiLogEvent lastEvent;

	@EventListener
	public void onEvent(PayloadApplicationEvent<ApiLogEvent> event) {
		lastEvent = event.getPayload();
	}

	public static ApiLogEvent getLastEvent() {
		return lastEvent;
	}

}
