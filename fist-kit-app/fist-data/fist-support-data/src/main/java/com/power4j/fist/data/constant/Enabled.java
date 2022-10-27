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

package com.power4j.fist.data.constant;

import java.util.function.Function;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/23
 * @since 1.0
 */
public enum Enabled {

	/**
	 * enabled
	 */
	YES(DataConstant.STATUS_VALUE_ENABLED),
	/**
	 * disabled
	 */
	NO(DataConstant.STATUS_VALUE_DISABLED);

	private final String value;

	Enabled(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static boolean yes(String str) {
		return YES.getValue().equals(str);
	}

	public static boolean not(String str) {
		return NO.getValue().equals(str);
	}

	/**
	 * 解析
	 * @param value 被解析的数据,可以是null
	 * @param defValue 默认值
	 * @return 如果解析失败返回默认值
	 */
	public static Enabled parseOrDefault(final String value, final Enabled defValue) {
		if (value == null) {
			return defValue;
		}
		for (Enabled o : Enabled.values()) {
			if (o.getValue().equals(value)) {
				return o;
			}
		}
		return defValue;
	}

	/**
	 * 解析
	 * @param value 被解析的数据
	 * @return 如果解析失败返回 null
	 */
	public static Enabled parseOrNull(final String value) {
		return parseOrDefault(value, null);
	}

	/**
	 * 解析
	 * @param value 被解析的数据
	 * @param thrower 异常抛出器
	 * @return 如果解析失败抛出异常
	 */
	public static Enabled parseOrThrow(final String value, Function<String, RuntimeException> thrower) {
		Enabled o = parseOrDefault(value, null);
		if (o == null) {
			throw thrower.apply(value);
		}
		return o;
	}

	/**
	 * 解析
	 * @param value 被解析的数据
	 * @return 如果解析失败抛出 IllegalArgumentException
	 */
	public static Enabled parse(final String value) throws IllegalArgumentException {
		return parseOrThrow(value, (v) -> new IllegalArgumentException("Invalid value : " + v));
	}

}
