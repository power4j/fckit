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

package com.power4j.fist.boot.common.error;

import org.springframework.http.HttpStatus;

/**
 * 国际化支持
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/9
 * @since 1.0
 */
public class MsgBundleRejectedException extends RejectedException {

	private final String msgKey;

	private final Object[] msgArg;

	/**
	 * 构造函数
	 * @param code 错误代码
	 * @param msgKey 错误消息key
	 * @param msgArg 错误消息参数
	 */
	public MsgBundleRejectedException(String code, String msgKey, Object... msgArg) {
		super(code, msgKey, null);
		this.msgKey = msgKey;
		this.msgArg = msgArg;
	}

	/**
	 * 构造函数
	 * @param status 状态码
	 * @param code 错误代码
	 * @param msgKey 错误消息key
	 * @param msgArg 错误消息参数
	 */
	public MsgBundleRejectedException(HttpStatus status, String code, String msgKey, Object... msgArg) {
		super(status, code, msgKey, null);
		this.msgKey = msgKey;
		this.msgArg = msgArg;
	}

	public String getMsgKey() {
		return msgKey;
	}

	public Object[] getMsgArg() {
		return msgArg;
	}

}
