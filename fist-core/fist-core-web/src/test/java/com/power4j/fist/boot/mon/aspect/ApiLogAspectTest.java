package com.power4j.fist.boot.mon.aspect;

import com.power4j.fist.boot.mon.event.ApiLogEvent;
import com.power4j.fist.boot.util.ApplicationContextHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/23
 * @since 1.0
 */
class ApiLogAspectTest {

	@Test
	void handleError() {
		AnnotationConfigApplicationContext appCtx = new AnnotationConfigApplicationContext();
		appCtx.register(ApiLogConfig.class);
		appCtx.refresh();
		new ApplicationContextHolder().setApplicationContext(appCtx);

		DemoService serviceBean = appCtx.getBean(DemoService.class);
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		serviceBean.produceResult();
		final ApiLogEvent produceResult = ApiLogEventListener.getLastEvent();
		Assertions.assertNotNull(produceResult);
		Assertions.assertEquals("produceResult", produceResult.getOperation());
		Assertions.assertEquals(DemoService.CODE_0, produceResult.getResponseInfo().getCode());
		Assertions.assertEquals(DemoService.RAW_MSG, produceResult.getResponseInfo().getMessage());
		Assertions.assertNull(produceResult.getError());

		try {
			serviceBean.reject();
		}
		catch (Throwable e) {
			System.out.println("catch - " + e.getMessage());
		}
		final ApiLogEvent reject = ApiLogEventListener.getLastEvent();
		Assertions.assertNotNull(reject);
		Assertions.assertEquals("reject", reject.getOperation());
		Assertions.assertEquals(DemoService.CODE_0, reject.getResponseInfo().getCode());
		Assertions.assertEquals(DemoService.RAW_MSG, reject.getResponseInfo().getMessage());
		Assertions.assertNull(reject.getError());

		try {
			serviceBean.rejectMsg();
		}
		catch (Throwable e) {
			System.out.println("catch - " + e.getMessage());
		}
		final ApiLogEvent rejectMsg = ApiLogEventListener.getLastEvent();
		Assertions.assertNotNull(rejectMsg);
		Assertions.assertEquals("rejectMsg", rejectMsg.getOperation());
		Assertions.assertEquals(DemoService.CODE_0, rejectMsg.getResponseInfo().getCode());
		Assertions.assertEquals(DemoService.MSG_KEY, rejectMsg.getResponseInfo().getMessage());
		Assertions.assertNull(rejectMsg.getError());

		serviceBean.produceAny();
		final ApiLogEvent produceAny = ApiLogEventListener.getLastEvent();
		Assertions.assertNotNull(produceAny);
		Assertions.assertEquals("produceAny", produceAny.getOperation());
		Assertions.assertNull(produceAny.getResponseInfo().getCode());
		Assertions.assertNull(produceAny.getError());

		try {
			serviceBean.unexpectedException();
		}
		catch (Throwable e) {
			System.out.println("catch - " + e.getMessage());
		}
		final ApiLogEvent unexpectedException = ApiLogEventListener.getLastEvent();
		Assertions.assertNotNull(unexpectedException);
		Assertions.assertEquals("unexpectedException", unexpectedException.getOperation());
		Assertions.assertNull(produceAny.getResponseInfo().getCode());
		Assertions.assertNotNull(unexpectedException.getError());
		Assertions.assertEquals(RuntimeException.class.getName(), unexpectedException.getError().getEx());
	}

}