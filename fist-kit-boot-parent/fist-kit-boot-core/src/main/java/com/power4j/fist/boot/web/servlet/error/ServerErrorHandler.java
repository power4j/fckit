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

package com.power4j.fist.boot.web.servlet.error;

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.api.Results;
import com.power4j.fist.boot.common.error.MsgBundleRejectedException;
import com.power4j.fist.boot.common.error.RejectedException;
import com.power4j.fist.boot.i18n.LocaleResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/14
 * @since 1.0
 */
@Slf4j
@Order
@RestControllerAdvice
public class ServerErrorHandler extends AbstractExceptionHandler {

	@Nullable
	private MessageSourceAccessor messageSourceAccessor;

	@Nullable
	private LocaleResolver localeResolver;

	@Autowired(required = false)
	public void setMessageSource(MessageSource messageSource) {
		this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
	}

	@Autowired(required = false)
	public void setLocaleResolver(@Nullable LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
	}

	protected Result<?> makeResult(MsgBundleRejectedException e) {
		Locale locale = Optional.ofNullable(localeResolver).map(LocaleResolver::resolve).orElse(Locale.CHINA);
		String msg = Objects.requireNonNull(messageSourceAccessor).getMessage(e.getMsgKey(), e.getMsgArg(),
				e.getMsgKey(), locale);
		return Result.create(e.getCode(), msg, null);
	}

	@ExceptionHandler(MsgBundleRejectedException.class)
	public ResponseEntity<Result<?>> handleException(MsgBundleRejectedException e) {
		return ResponseEntity.status(e.getStatus()).body(makeResult(e));
	}

	@ExceptionHandler(RejectedException.class)
	public ResponseEntity<Result<?>> handleException(RejectedException e) {
		return ResponseEntity.status(e.getStatus()).body(Results.fromError(e));
	}

	@ExceptionHandler(SQLException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Result<Object> handleException(SQLException e) {
		log.error("数据库访问异常,vendorCode = {}", e.getErrorCode(), e);
		doNotify(e);
		return Results.serverError(String.format("数据库访问异常(%s),请联系管理员", e.getClass().getSimpleName()),
				"vendorCode " + e.getErrorCode());
	}

	@ExceptionHandler(Throwable.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Result<Object> handleException(Throwable e) {
		log.error(String.format("发生未处理异常(%s)", e.getClass().getName()), e);
		doNotify(e);
		return Results.serverError(String.format("服务异常(%s),请联系管理员", e.getClass().getSimpleName()), null);
	}

}
