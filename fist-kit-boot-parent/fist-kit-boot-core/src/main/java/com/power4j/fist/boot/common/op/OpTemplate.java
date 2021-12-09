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

import java.util.function.Consumer;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/12
 * @since 1.0
 */
public class OpTemplate<T> implements OpInvoker<T> {

	private final TemplateId id;

	private final OpHandler<T> preHandler;

	private final OpHandler<T> opHandler;

	OpTemplate(TemplateId id, OpHandler<T> preHandler, OpHandler<T> opHandler) {
		this.id = id;
		this.preHandler = preHandler;
		this.opHandler = opHandler;
	}

	@Override
	public void run(T context, Consumer<T> operation) {
		preHandler.handle(context);
		operation.accept(context);
		opHandler.handle(context);
	}

	public TemplateId getId() {
		return id;
	}

}
