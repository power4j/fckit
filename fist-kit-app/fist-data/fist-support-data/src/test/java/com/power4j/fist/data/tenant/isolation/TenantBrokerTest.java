package com.power4j.fist.data.tenant.isolation;

import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
class TenantBrokerTest {

	static class MyException extends Exception {

		public MyException() {
			super();
		}

		public MyException(String message) {
			super(message);
		}

	}

	FailableRunnable<Throwable> run = () -> {
	};

	FailableSupplier<String, Throwable> apply = () -> {
		return "ok";
	};

	FailableRunnable<Throwable> runFail_1 = () -> {
		throw new MyException("fake exception");
	};

	FailableSupplier<Integer, Throwable> applyFail_1 = () -> {
		throw new MyException("fake exception");
	};

	FailableRunnable<Throwable> runFail_2 = () -> {
		throw new IllegalStateException("fake exception");
	};

	FailableSupplier<Integer, Throwable> applyFail_2 = () -> {
		throw new IllegalStateException("fake exception");
	};

	@Test
	void runNested() {
		TenantHolderTestSupport.setCurrentTenant("0");
		TenantBroker.runAs("1", () -> TenantBroker.runAs("2", () -> {
		}));
		Assertions.assertEquals("0", TenantHolder.getRequired());
		try {
			TenantBroker.runAs("1", () -> TenantBroker.runAs("2", runFail_1));
		}
		catch (Exception e) {
			// ignore
		}
		Assertions.assertEquals("0", TenantHolder.getRequired());
	}

	@Test
	void applyNested() {
		TenantHolderTestSupport.setCurrentTenant("0");
		TenantBroker.applyAs("1", () -> TenantBroker.applyAs("2", () -> null));
		Assertions.assertEquals("0", TenantHolder.getRequired());
		try {
			TenantBroker.applyAs("1", () -> TenantBroker.applyAs("2", applyFail_1));
		}
		catch (Exception e) {
			// ignore
		}
		Assertions.assertEquals("0", TenantHolder.getRequired());
	}

	@Test
	void errorHandlerTest() {
		RuntimeException runtimeException;
		AtomicInteger intVal = new AtomicInteger(0);

		TenantBroker.runAs(null, runFail_1, ex -> intVal.set(1));
		Assertions.assertEquals(1, intVal.get());

		int ret = TenantBroker.applyAs(null, applyFail_1, (ex) -> 1);
		Assertions.assertEquals(1, ret);

		// 异常处理器抛出受检异常,会包装为运行时异常
		Assertions.assertThrows(RuntimeException.class, () -> TenantBroker.runAs(null, runFail_2, ex -> {
			throw new MyException();
		}));
		Assertions.assertThrows(RuntimeException.class, () -> TenantBroker.applyAs(null, applyFail_2, ex -> {
			throw new MyException();
		}));
	}

	@Test
	void defaultErrorHandlerRethrowTest() {
		RuntimeException runtimeException;
		// 受检异常会包装为 RuntimeException
		runtimeException = Assertions.assertThrows(RuntimeException.class, () -> TenantBroker.runAs(null, runFail_1));
		Assertions.assertNotNull(runtimeException.getCause());
		Assertions.assertEquals(runtimeException.getCause().getClass(), MyException.class);

		runtimeException = Assertions.assertThrows(RuntimeException.class,
				() -> TenantBroker.applyAs(null, applyFail_1));
		Assertions.assertNotNull(runtimeException.getCause());
		Assertions.assertEquals(runtimeException.getCause().getClass(), MyException.class);

		Assertions.assertThrows(IllegalStateException.class, () -> TenantBroker.runAs(null, runFail_2));
		Assertions.assertThrows(IllegalStateException.class, () -> TenantBroker.applyAs(null, applyFail_2));
	}

}