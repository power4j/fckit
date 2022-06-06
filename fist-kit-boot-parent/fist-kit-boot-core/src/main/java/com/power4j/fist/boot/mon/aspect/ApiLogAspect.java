/*
 * Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.boot.mon.aspect;

import com.power4j.coca.kit.common.datetime.DateTimeKit;
import com.power4j.coca.kit.common.lang.Result;
import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.boot.common.aop.AopUtil;
import com.power4j.fist.boot.mon.EventUtils;
import com.power4j.fist.boot.mon.annotation.ApiLog;
import com.power4j.fist.boot.mon.event.ApiLogEvent;
import com.power4j.fist.boot.mon.info.ApiResponseInfo;
import com.power4j.fist.boot.mon.info.AuthInfo;
import com.power4j.fist.boot.mon.info.ExceptionInfo;
import com.power4j.fist.boot.mon.info.ExceptionTranslator;
import com.power4j.fist.boot.mon.info.HttpRequestInfo;
import com.power4j.fist.boot.security.core.UserInfoAccessor;
import com.power4j.fist.boot.util.SpringEventUtil;
import com.power4j.fist.boot.web.servlet.util.HttpServletRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/3/31
 * @since 1.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class ApiLogAspect {

	private final ExceptionTranslator exceptionTranslator;

	@Nullable
	private final UserInfoAccessor userInfoAccessor;

	private final AtomicReference<String> appNameRef = new AtomicReference<>(null);

	@Around("@annotation(apiLog)")
	public Object around(ProceedingJoinPoint point, ApiLog apiLog) throws Throwable {
		final HttpRequestInfo requestInfo = HttpRequestInfo.from(HttpServletRequestUtil.getCurrentRequest());
		if (!apiLog.withQuery()) {
			requestInfo.setQueryParam(StringPool.EMPTY);
		}

		// @formatter:off
		ApiLogEvent event = ApiLogEvent.builder()
				.appName(ObjectUtils.defaultIfNull(getAppName(), "unknown"))
				.operation(getDescription(point))
				.requestInfo(requestInfo)
				.time(DateTimeKit.utcNow())
				.authInfo(getAuthInfo())
				.build();
		// @formatter:on
		try {
			Object result = point.proceed();
			event.setDuration(Duration.between(event.getTime(), DateTimeKit.utcNow()));
			try {
				fetchResultInfo(result, event);
				fire(event);
			}
			catch (Throwable any) {
				log.error(any.getMessage(), any);
			}
			return result;

		}
		catch (Exception e) {
			event.setDuration(Duration.between(event.getTime(), DateTimeKit.utcNow()));
			try {
				fetchErrorInfo(e, event);
				fire(event);
			}
			catch (Throwable any) {
				log.error(any.getMessage(), any);
			}
			throw e;
		}
	}

	AuthInfo getAuthInfo() {
		return Optional.ofNullable(userInfoAccessor).flatMap(UserInfoAccessor::getUserInfo).map(AuthInfo::from)
				.orElseGet(AuthInfo::new);
	}

	void fetchResultInfo(Object result, ApiLogEvent event) {
		ApiResponseInfo responseInfo = new ApiResponseInfo();
		if (result instanceof Result) {
			Result<?> r = (Result<?>) result;
			responseInfo.setCode(r.getCode());
			responseInfo.setMessage(r.getMessage());
		}
		event.setResponseInfo(responseInfo);
	}

	void fetchErrorInfo(Throwable e, ApiLogEvent event) {
		exceptionTranslator.translateException(e).ifPresentOrElse(event::setResponseInfo,
				() -> event.setError(ExceptionInfo.from(e, 2000)));
	}

	String getDescription(ProceedingJoinPoint point) {
		final Method method = AopUtil.getMethod(point);
		ApiLog annotation = AnnotationUtils.findAnnotation(method, ApiLog.class);
		final String value = (String) AnnotationUtils.getValue(annotation);
		if (ObjectUtils.isNotEmpty(value)) {
			return value;
		}

		final Operation operation = method.getAnnotation(Operation.class);
		if (Objects.nonNull(operation)) {
			return ObjectUtils.firstNonNull(operation.summary(), operation.description());
		}
		return StringPool.EMPTY;
	}

	@Nullable
	String getAppName() {
		String name = appNameRef.get();
		if (Objects.nonNull(name)) {
			return name;
		}
		EventUtils.getAppName().ifPresent(v -> appNameRef.compareAndSet(null, v));
		return appNameRef.get();
	}

	void fire(ApiLogEvent event) {
		SpringEventUtil.publishEvent(event);
	}

}
