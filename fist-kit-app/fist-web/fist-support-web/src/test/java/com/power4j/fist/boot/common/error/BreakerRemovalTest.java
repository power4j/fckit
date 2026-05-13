package com.power4j.fist.boot.common.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BreakerRemovalTest {

	@Test
	void breakerClassShouldBeRemoved() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertThrows(ClassNotFoundException.class,
				() -> Class.forName("com.power4j.fist.boot.common.error.Breaker", false, classLoader));
	}

}
