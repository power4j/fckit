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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.api.Results;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.ValidationException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ???????????????????????????????????????
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/14
 * @since 1.0
 */
@Slf4j
@Order(1000)
@RestControllerAdvice
public class ClientErrorHandler {

	// ~ ???????????????????????????
	// ===================================================================================================

	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Object> handleException(MissingServletRequestParameterException e) {
		String hint = String.format("%s(%s)", e.getParameterName(), e.getParameterType());
		log.warn("??????????????????:{}", hint);
		return Results.requestParameterError("??????????????????", hint);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Object> handleException(MethodArgumentTypeMismatchException e) {
		log.warn("?????????????????????:{}", e.getMessage());
		return Results.requestParameterError("?????????????????????", e.getName());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Object> handleException(MethodArgumentNotValidException e) {
		Optional<FieldError> error = Optional.ofNullable(e.getBindingResult().getFieldError());
		String errField = error.map(FieldError::getField).orElse(null);
		String msg = error.map(FieldError::getDefaultMessage).orElse("????????????????????????");
		log.warn("????????????????????????:{}", errField);
		return Results.requestParameterError(msg, errField);
	}

	@ExceptionHandler(BindException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Object> handleException(BindException e) {
		Optional<FieldError> error = Optional.ofNullable(e.getBindingResult().getFieldError());
		String errField = error.map(FieldError::getField).orElse(null);
		String msg = error.map(FieldError::getDefaultMessage).orElse("????????????????????????");
		log.warn("????????????????????????:{}", errField);
		return Results.requestParameterError(msg, errField);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Object> handleException(ConstraintViolationException e) {
		ConstraintViolation<?> violation = e.getConstraintViolations().iterator().next();
		String path = ((Path) violation.getPropertyPath()).toString();
		log.warn("????????????????????????:{}", path);
		return Results.requestParameterError(violation.getMessage(), path);
	}

	@ExceptionHandler(ValidationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Object> handleException(ValidationException e) {
		log.warn("????????????????????????:{}", e.getMessage());
		return Results.requestParameterError("????????????????????????", e.getMessage());
	}

	// ~ ?????????????????????
	// ===================================================================================================

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Object> handleException(HttpMessageNotReadableException e) {
		log.error("????????????????????????:{}", e.getMessage());
		return Results.clientError("????????????????????????", e.getMessage());
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Result<Object> handleException(NoHandlerFoundException e) {
		String hint = String.format("%s %s", e.getHttpMethod(), e.getRequestURL());
		log.error("?????????????????????:{}", hint);
		return Results.clientError("?????????????????????", hint);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	public Result<Object> handleException(HttpRequestMethodNotSupportedException e) {
		log.error("?????????????????????:{}", e.getMessage());
		return Results.clientError("?????????????????????", e.getMessage());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public Result<Object> handleException(HttpMediaTypeNotSupportedException e) {
		log.error("??????????????????????????????:{}", e.getMessage());
		String hint = String.format("????????????????????????: %s",
				e.getSupportedMediaTypes().stream().map(MediaType::toString).collect(Collectors.joining(",")));
		return Results.clientError("??????????????????????????????", hint);
	}

	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public Result<Object> handleException(HttpMediaTypeNotAcceptableException e) {
		String required = e.getMessage() + " " + CharSequenceUtil.join(StrUtil.COMMA, e.getSupportedMediaTypes());
		log.error("??????????????????????????????????????????:{}", required);
		String hint = String.format("????????????????????????: %s",
				e.getSupportedMediaTypes().stream().map(MediaType::toString).collect(Collectors.joining(",")));
		return Results.clientError("??????????????????????????????", hint);
	}

}
