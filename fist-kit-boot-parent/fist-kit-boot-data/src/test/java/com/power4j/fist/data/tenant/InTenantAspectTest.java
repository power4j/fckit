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

package com.power4j.fist.data.tenant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/12
 * @since 1.0
 */
public class InTenantAspectTest {

	static class Demo {

		public void hello(String name) {
			//
		}

	}

	@Test
	public void evaluationExprTest() throws NoSuchMethodException {
		InTenantAspect aspectTest = new InTenantAspect();
		String expr = "#name";
		Method method = Demo.class.getMethod("hello", String.class);
		Object[] argv = new Object[] { "fist" };
		String value = aspectTest.evaluationExpr(method, argv, expr, String.class, null);
		Assertions.assertEquals("fist", value);
	}

}