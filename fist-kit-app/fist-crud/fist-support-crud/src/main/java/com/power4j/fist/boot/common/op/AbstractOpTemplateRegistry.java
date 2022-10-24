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

package com.power4j.fist.boot.common.op;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/12
 * @since 1.0
 */
public abstract class AbstractOpTemplateRegistry<T> {

	private final Map<String, OpTemplate<T>> definitions;

	public AbstractOpTemplateRegistry(List<OpHandler<T>> handlers,
			List<OpTemplateConfigure<T, OpTemplateBuilder<T>>> configures) {
		this.definitions = buildMap(handlers, configures);
	}

	public OpTemplate<T> use(String id) {
		if (!definitions.containsKey(id)) {
			throw new OpTemplateException("OpTemplate not defined :" + id);
		}
		return definitions.get(id);
	}

	private Map<String, OpTemplate<T>> buildMap(List<OpHandler<T>> handlers,
			List<OpTemplateConfigure<T, OpTemplateBuilder<T>>> configures) {
		Objects.requireNonNull(handlers);
		OpTemplateBuilder<T> builder = new OpTemplateBuilder<>(handlers);
		configures.forEach(c -> c.config(builder));
		return builder.build();
	}

}
