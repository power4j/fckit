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

package com.power4j.fist.cloud.gateway.auth.entity;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/23
 * @since 1.0
 */
@Getter
public class AuthProblem {

	public final static int CODE_AUTH_PASS = 0;

	private final int code;

	private final String msgKey;

	public AuthProblem(int code, String msgKey) {
		this.code = code;
		this.msgKey = msgKey;
	}

	public boolean isAuthPass() {
		return CODE_AUTH_PASS == code;
	}

	/**
	 * 公共接口放行
	 */
	public final static AuthProblem PUB_ACCESS = new AuthProblem(0, "xxx");

	/**
	 * 需跳过鉴权
	 */
	public final static AuthProblem SKIP_AUTH = new AuthProblem(0, "xxx");

	/**
	 * 鉴权正常结束
	 */
	public final static AuthProblem AUTH_END = new AuthProblem(0, "xxx");

	/**
	 * 传入token无效
	 */
	public final static AuthProblem TOKEN_INVALID = new AuthProblem(1, "xxx");

	/**
	 * 用户不存在
	 */
	public final static AuthProblem USER_NOT_EXISTS = new AuthProblem(2, "xxx");

	/**
	 * 内部接口拒绝访问
	 */
	public final static AuthProblem INTERNAL_ACCESS_DENIED = new AuthProblem(3, "xxx");

	/**
	 * 未登录不能访问用户级接口
	 */
	public final static AuthProblem USER_ACCESS_DENIED = new AuthProblem(4, "xxx");

	/**
	 * 无API访问权限
	 */
	public final static AuthProblem PERMISSION_CHECK_DENIED = new AuthProblem(5, "xxx");

	/**
	 * 租户级API必须传入租户ID
	 */
	public final static AuthProblem TENANT_ID_REQUIRED = new AuthProblem(6, "xxx");

	/**
	 * 无租户访问权限
	 */
	public final static AuthProblem TENANT_CHECK_DENIED = new AuthProblem(6, "xxx");

	/**
	 * 认证插件异常
	 */
	public final static AuthProblem AUTH_EXCEPTION = new AuthProblem(6, "xxx");

	/**
	 * 调用外部服务异常
	 */
	public final static AuthProblem RPC_EXCEPTION = new AuthProblem(6, "xxx");

	@Override
	public String toString() {
		return new StringJoiner(", ", AuthProblem.class.getSimpleName() + "[", "]").add("code=" + code)
				.add("msgKey='" + msgKey + "'").toString();
	}

}
