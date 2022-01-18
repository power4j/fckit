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

package com.power4j.fist.boot.common.spel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
class MethodParameterResolverTest {

	static class Hello {

		public void hello(String name) {
			// Nothing
		}

	}

	@Test
	void getVariables() throws NoSuchMethodException {
		String expr = "#name";
		Method method = Hello.class.getMethod("hello", String.class);
		Object[] argv = new Object[] { "fist" };
		Map<String, Object> variables = MethodParameterResolver.of(method, argv).getVariables();
		Assertions.assertEquals("fist", variables.get("name"));
	}

}