/*
 *  Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.gnu.org/licenses/lgpl.html
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.power4j.fist.boot.security.crypto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/3/7
 * @since 1.0
 */
class PasswordEncoderUtilTest {

	PasswordEncoder encoder = PasswordEncoderUtil.createDelegatingPasswordEncoder();

	String rawPwd1 = "123";

	String rawPwd2 = "root";

	@Test
	public void testEncode() {
		String encoded1 = encoder.encode(rawPwd1);
		System.out.printf("encode: %s -> %s%n", rawPwd1, encoded1);
		Assertions.assertTrue(encoded1.startsWith("{" + Sm3PasswordEncoder.ID + "}"));
		Assertions.assertTrue(encoder.matches(rawPwd1, encoded1));

		String encoded2 = encoder.encode(rawPwd2);
		System.out.printf("encode: %s -> %s%n", rawPwd2, encoded2);
		Assertions.assertTrue(encoder.matches(rawPwd2, encoded2));
	}

	@Test
	public void testMatchNoOp() {
		final String encoded = "{noop}123";
		boolean matched = encoder.matches("123", encoded);
		Assertions.assertTrue(matched);
	}

}