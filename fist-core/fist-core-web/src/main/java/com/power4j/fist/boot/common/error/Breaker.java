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

import com.power4j.fist.boot.i18n.LocaleResolver;
import lombok.experimental.UtilityClass;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/13
 * @since 1.0
 * @deprecated
 */
@UtilityClass
public class Breaker {

	@Nullable
	private static MessageSourceAccessor messageSourceAccessor;

	@Nullable
	private static LocaleResolver localeResolver;

	public static void requestParameterError(@Nullable String hint) throws RejectedException {
		reject(ErrorCode.A0400, "common.bad-parameter", null, hint);
	}

	public static void requestParameterError(String msgKey, @Nullable Object[] args, @Nullable String hint)
			throws RejectedException {
		reject(ErrorCode.A0400, msgKey, args, hint);
	}

	public static void resourceNotExistsError(@Nullable String hint) throws RejectedException {
		requestParameterError("common.resource.not-found", null, hint);
	}

	public static <T> T fastFailOnNotExists(@Nullable T origin, @Nullable String hint) {
		if (Objects.isNull(origin)) {
			resourceNotExistsError(hint);
		}
		assert origin != null;
		return origin;
	}

	public static void permissionDeniedError(@Nullable String hint) throws RejectedException {
		requestParameterError("common.permission.denied", null, hint);
	}

	/**
	 * 抛出 {@code TerminatedException}异常，支持国际化
	 * @param errorCode 错误代码
	 * @param msgKey 消息模板
	 * @param args 消息模板参数
	 */
	public static void reject(String errorCode, String msgKey, @Nullable Object[] args) throws RejectedException {
		reject(errorCode, msgKey, args, null);
	}

	/**
	 * 抛出 {@code TerminatedException}异常，支持国际化
	 * @param errorCode 错误代码
	 * @param msgKey 消息模板
	 * @param args 消息模板参数
	 * @param hint 错误提示
	 */
	public static void reject(String errorCode, String msgKey, @Nullable Object[] args, @Nullable String hint)
			throws RejectedException {
		Locale locale = Optional.ofNullable(localeResolver).map(LocaleResolver::resolve).orElse(Locale.CHINA);
		String msg = Objects.requireNonNull(messageSourceAccessor).getMessage(msgKey, args, msgKey, locale);
		rise(errorCode, msg, hint);
	}

	/**
	 * 抛出 {@code TerminatedException}异常，无国际化处理
	 * @param errorCode 错误代码
	 * @param message 消息内容
	 * @param hint 错误提示
	 */
	public static void rise(String errorCode, String message, @Nullable String hint) {
		throw new RejectedException(errorCode, message, hint);
	}

	public static void setMessageSourceAccessor(MessageSourceAccessor messageSourceAccessor) {
		Breaker.messageSourceAccessor = Objects.requireNonNull(messageSourceAccessor);
	}

	public static void setLocaleResolver(LocaleResolver localeResolver) {
		Breaker.localeResolver = Objects.requireNonNull(localeResolver);
	}

}
