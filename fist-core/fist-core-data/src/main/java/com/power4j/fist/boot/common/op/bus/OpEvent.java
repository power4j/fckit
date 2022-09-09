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

package com.power4j.fist.boot.common.op.bus;

import lombok.Data;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/17
 * @since 1.0
 */
@Data
public class OpEvent<T> {

	private final T target;

	private final Object[] args;

	/**
	 * 构造方法
	 * @param target 业务对象
	 * @param args 操作参数,一般情况下不建议使用,不易维护
	 */
	public OpEvent(T target, Object... args) {
		this.target = target;
		this.args = args;
	}

	/**
	 * 构造方法
	 * @param target 业务对象
	 */
	public OpEvent(T target) {
		this(target, new Object[0]);
	}

}
