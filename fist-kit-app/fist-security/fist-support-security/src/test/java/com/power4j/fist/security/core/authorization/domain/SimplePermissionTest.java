package com.power4j.fist.security.core.authorization.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
class SimplePermissionTest {

	@Test
	public void equalsTest() {
		SimplePermission auth1 = new SimplePermission("TEST");
		assertThat(auth1).isEqualTo(auth1);
		assertThat(new SimplePermission("TEST")).isEqualTo(auth1);
		assertThat(auth1.equals("TEST")).isFalse();
		SimplePermission auth3 = new SimplePermission("NOT_EQUAL");
		assertThat(!auth1.equals(auth3)).isTrue();
		assertThat(auth1.equals(mock(SimplePermission.class))).isFalse();
		assertThat(auth1.equals(222)).isFalse();
	}

	@Test
	public void toStringReturnsAuthorityValue() {
		SimplePermission auth = new SimplePermission("TEST");
		assertThat(auth.toString()).isEqualTo("TEST");
	}

}