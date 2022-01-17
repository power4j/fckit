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

package com.power4j.fist.boot.apidoc;

import com.power4j.fist.boot.security.core.SecurityConstant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/30
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ApiTrait {

	/**
	 * 唯一资源ID(服务级) <b>留空自动计算</b>
	 */
	String resourceId() default "";

	/**
	 * 管理级别,仅对SASS环境有效
	 */
	ApiLevel level() default ApiLevel.TN;

	/**
	 * API 访问模式
	 */
	Access access() default Access.DEFAULT;

	/**
	 * 签名校验
	 */
	boolean sign() default false;

	enum ApiLevel {

		/**
		 * 平台级
		 */
		PL(SecurityConstant.Res.LEVEL_SITE),
		/**
		 * 租户级
		 */
		TN(SecurityConstant.Res.LEVEL_TENANT);

		private final String value;

		ApiLevel(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	enum Access {

		/**
		 * 默认
		 */
		DEFAULT("0"),
		/**
		 * 只能内部访问
		 */
		INTERNAL("1"),
		/**
		 * 登录用户即可访问
		 */
		USER("2"),
		/**
		 * 公开访问
		 */
		PUBLIC("3");

		private final String value;

		Access(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

}
