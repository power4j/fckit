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

package com.power4j.fist.cloud.autoconfigure.rpc.feign.error;

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.error.ErrorCode;
import com.power4j.fist.boot.i18n.LocaleResolver;
import com.power4j.fist.boot.web.servlet.error.AbstractExceptionHandler;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/3/16
 * @since 1.0
 */
@Slf4j
@Order(5000)
@RestControllerAdvice
public class FeignErrorHandler extends AbstractExceptionHandler {

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

	protected String translateMessage(String msgKey, Object... args) {
		Locale locale = Optional.ofNullable(localeResolver).map(LocaleResolver::resolve).orElse(Locale.CHINA);
		if (Objects.nonNull(messageSourceAccessor)) {
			return messageSourceAccessor.getMessage(msgKey, args, msgKey, locale);
		}
		else {
			return msgKey;
		}
	}

	protected Result<?> translate(FeignException e) {
		final int status = e.status();
		if (status >= 400 && status < 500) {
			return Result.create(ErrorCode.C0110, translateMessage("common.rpc.err-request"), null);
		}
		else if (status >= 500 && status <= 599) {
			return Result.create(ErrorCode.C0110, translateMessage("common.rpc.err-retry"), null);
		}
		else {
			return Result.create(ErrorCode.B0001, translateMessage("common.server.fail-retry"), null);
		}
	}

	@ExceptionHandler(FeignException.class)
	public ResponseEntity<Result<?>> handleException(FeignException e) {
		// @formatter:off
		log.error("接口调用出错({}). {}",
				e.getClass().getSimpleName(),
				e.getMessage());
		// @formatter:on
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(translate(e));
	}

}
