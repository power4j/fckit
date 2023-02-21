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

package com.power4j.fist.support.spring.spel;

import org.springframework.core.DefaultParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
public class MethodParameterResolver implements VariableProvider {

	private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

	private final Method method;

	private final Object[] arguments;

	public static MethodParameterResolver of(Method method, Object[] arguments) {
		return new MethodParameterResolver(method, arguments);
	}

	MethodParameterResolver(Method method, Object[] arguments) {
		this.method = method;
		this.arguments = arguments;
	}

	@Override
	public Map<String, Object> getVariables() {
		String[] params = discoverer.getParameterNames(method);
		if (null != params) {
			Map<String, Object> map = new HashMap<>(4);
			for (int len = 0; len < params.length; len++) {
				map.put(params[len], arguments[len]);
			}
			return map;
		}
		return Collections.emptyMap();
	}

}
