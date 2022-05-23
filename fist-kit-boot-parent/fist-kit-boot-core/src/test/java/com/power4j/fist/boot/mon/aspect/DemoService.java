package com.power4j.fist.boot.mon.aspect;

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.error.MsgBundleRejectedException;
import com.power4j.fist.boot.common.error.RejectedException;
import com.power4j.fist.boot.mon.annotation.ApiLog;
import org.springframework.stereotype.Component;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/23
 * @since 1.0
 */
@Component
public class DemoService {

	public static final String CODE_0 = "0";

	public static final String OK = "ok";

	public static final String MSG_KEY = "msg.ok";

	public static final String RAW_MSG = "test message";

	@ApiLog("produceResult")
	public Result<?> produceResult() {
		System.out.println(getClass().getName() + " - produceResult");
		return Result.create(CODE_0, RAW_MSG, null);
	}

	@ApiLog(operation = "produceAny")
	Object produceAny() {
		System.out.println(getClass().getName() + " - produceAny");
		return OK;
	}

	@ApiLog(value = "reject")
	Object reject() {
		System.out.println(getClass().getName() + " - reject");
		throw new RejectedException(CODE_0, RAW_MSG, null);
	}

	@ApiLog("rejectMsg")
	Object rejectMsg() {
		System.out.println(getClass().getName() + " - rejectMsg");
		throw new MsgBundleRejectedException(CODE_0, MSG_KEY);
	}

	@ApiLog("unexpectedException")
	Object unexpectedException() {
		System.out.println(getClass().getName() + " - unexpectedException");
		throw new RuntimeException("unexpectedException");
	}

}
