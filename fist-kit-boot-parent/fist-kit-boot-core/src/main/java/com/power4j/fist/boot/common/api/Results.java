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

package com.power4j.fist.boot.common.api;

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.error.ErrorCode;
import com.power4j.fist.boot.common.error.RejectedException;
import lombok.experimental.UtilityClass;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/6
 * @since 1.0
 */
@UtilityClass
public class Results {

	public final static String META_KEY_HINT = "hint";

	public <T> Result<T> resultWithHint(String code, @Nullable String message, @Nullable T data,
			@Nullable String hint) {
		Result<T> result = Result.create(code, message, data, null);
		if (Objects.nonNull(hint)) {
			Map<String, Object> metadata = new HashMap<>(1);
			metadata.put(META_KEY_HINT, hint);
			result.setMetaInfo(metadata);
		}
		return result;
	}

	public <T> Result<T> ok(@Nullable String message, @Nullable T data) {
		return resultWithHint(ErrorCode.OK, message, data, null);
	}

	public <T> Result<T> ok(@Nullable T data) {
		return resultWithHint(ErrorCode.OK, null, data, null);
	}

	public <T> Result<T> ok() {
		return resultWithHint(ErrorCode.OK, null, null, null);
	}

	// ~ 宏观错误
	// ===================================================================================================

	/**
	 * 用户端错误 (一级宏观错误 A0001)
	 * @param message 错误消息
	 * @param hint 提示
	 * @return Result 对象
	 */
	public <T> Result<T> clientError(@Nullable String message, @Nullable String hint) {
		return resultWithHint(ErrorCode.A0001, message, null, hint);
	}

	/**
	 * 系统执行出错 (一级宏观错误 B0001)
	 * @param message 错误消息
	 * @param hint 提示
	 * @return Result 对象
	 */
	public <T> Result<T> serverError(@Nullable String message, @Nullable String hint) {
		return resultWithHint(ErrorCode.B0001, message, null, hint);
	}

	/**
	 * 调用第三方服务出错 (一级宏观错误 C0001)
	 * @param message 错误消息
	 * @param hint 提示
	 * @return Result 对象
	 */
	public <T> Result<T> externalServiceError(@Nullable String message, @Nullable String hint) {
		return resultWithHint(ErrorCode.C0001, message, null, hint);
	}

	/**
	 * RPC 服务出错 (C0110)
	 * @param message 错误消息
	 * @param hint 提示
	 * @return Result 对象
	 */
	public <T> Result<T> rpcError(@Nullable String message, @Nullable String hint) {
		return resultWithHint(ErrorCode.C0110, message, null, hint);
	}

	/**
	 * 请求参数错误 (二级宏观错误 A0400)
	 * @param message 错误消息
	 * @param hint 提示
	 * @return Result 对象
	 */
	public <T> Result<T> requestParameterError(@Nullable String message, @Nullable String hint) {
		return resultWithHint(ErrorCode.A0400, message, null, hint);
	}

	// ~ 安全相关
	// ===================================================================================================

	/**
	 * 访问未授权 A0301
	 * @param message 错误消息
	 * @param hint 提示
	 * @return Result 对象
	 */
	public <T> Result<T> noPermission(@Nullable String message, @Nullable String hint) {
		return resultWithHint(ErrorCode.A0301, message, null, hint);
	}

	// ~ 数据包装
	// ===================================================================================================

	/**
	 * 包装业务数据
	 * @param data 业务数据
	 * @param hint 提示
	 * @param <T> 业务数据类型
	 * @return data 为 null 返回错误码 A9900
	 */
	public <T> Result<T> requiredData(@Nullable T data, @Nullable String hint) {
		String code = Objects.nonNull(data) ? ErrorCode.OK : ErrorCode.A9900;
		String msg = Objects.nonNull(data) ? null : "Not found";
		return resultWithHint(code, msg, data, hint);
	}

	/**
	 * 包装业务数据
	 * @param data 业务数据
	 * @param <T> 业务数据类型
	 * @return data 为 null 返回错误码 A9900
	 */
	public <T> Result<T> requiredData(@Nullable T data) {
		return requiredData(data, null);
	}

	// ~ 异常处理
	// ===================================================================================================

	/**
	 * 转换异常信息
	 * @param e 异常
	 * @return Result
	 */
	public Result<?> fromError(RejectedException e) {
		return resultWithHint(e.getCode(), e.getMessage(), null, e.getHint());
	}

}
