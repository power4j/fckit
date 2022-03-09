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

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/10
 * @since 1.0
 */
@UtilityClass
public class CommonErrors {

	/**
	 * A0400 参数错误
	 * @param msgKey 消息key
	 * @param msgArg 参数
	 * @return MsgBundleRejectedException
	 */
	public MsgBundleRejectedException requestParameterError(String msgKey, Object... msgArg) {
		return new MsgBundleRejectedException(HttpStatus.BAD_REQUEST, ErrorCode.A0400, msgKey, msgArg);
	}

	/**
	 * A0400 参数错误
	 * @return MsgBundleRejectedException
	 */
	public MsgBundleRejectedException requestParameterError() {
		return new MsgBundleRejectedException(HttpStatus.BAD_REQUEST, ErrorCode.A0400, "common.bad-parameter");
	}

	/**
	 * A0400 某个字段的值无效
	 * @param fieldName 字段名称
	 * @return MsgBundleRejectedException
	 */
	public MsgBundleRejectedException invalidFieldValueError(String fieldName) {
		return new MsgBundleRejectedException(HttpStatus.BAD_REQUEST, ErrorCode.A0400, "common.invalid-field-value",
				fieldName);
	}

	/**
	 * 资源不存在
	 * @return MsgBundleRejectedException
	 */
	public MsgBundleRejectedException resourceNotExistsError() {
		return new MsgBundleRejectedException(HttpStatus.NOT_FOUND, ErrorCode.A0400, "common.resource.not-found");
	}

	/**
	 * A0301 权限不足
	 * @return MsgBundleRejectedException
	 */
	public MsgBundleRejectedException permissionDeniedError() {
		return new MsgBundleRejectedException(HttpStatus.FORBIDDEN, ErrorCode.A0301, "common.permission.denied");
	}

	/**
	 * A0401 未认证
	 * @return MsgBundleRejectedException
	 */
	public MsgBundleRejectedException authRequiredError() {
		return new MsgBundleRejectedException(HttpStatus.UNAUTHORIZED, ErrorCode.A0401,
				"common.permission.no-auth-denied");
	}

	/**
	 * B0001 系统执行出错
	 * @return MsgBundleRejectedException
	 */
	public MsgBundleRejectedException serverError() {
		return new MsgBundleRejectedException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.B0001, "common.rpc.err-retry");
	}

}
