/*
 * Copyright 2026. ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.data.crud.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @since 4.0
 */
class ValidateUtilDeprecationTest {

	@Test
	void shouldDeclareDeprecatedAnnotationOnLegacyCheckMethod() throws NoSuchMethodException {
		Method method = ValidateUtil.class.getDeclaredMethod("check", Object.class, Class[].class);

		Assertions.assertTrue(method.isAnnotationPresent(Deprecated.class));
		Assertions.assertEquals(Set.class, method.getReturnType());
	}

}
