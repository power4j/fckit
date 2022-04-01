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
import com.power4j.fist.boot.mon.annotation.ApiLog;
import com.power4j.fist.boot.mon.event.ApiLogEvent;
import com.power4j.fist.boot.mon.info.ExceptionInfo;
import com.power4j.fist.boot.mon.info.HttpRequestInfo;
import com.power4j.fist.boot.mon.info.HttpResponseInfo;
import com.power4j.fist.boot.util.ApplicationContextHolder;
import com.power4j.fist.boot.util.SpringEventUtil;
import com.power4j.fist.boot.web.servlet.util.HttpServletRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/3/31
 * @since 1.0
 */
@Slf4j
@Aspect
public class ApiLogAspect {

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
				.operation(getDescription(point, apiLog))
				.requestInfo(requestInfo)
				.time(DateTimeKit.utcNow())
				.build();
		// @formatter:on
		try {
			Object result = point.proceed();
			event.setDuration(Duration.between(event.getTime(), DateTimeKit.utcNow()));
			try {
				handleResult(result, event);
			}
			catch (Throwable any) {
				log.error(any.getMessage(), any);
			}
			return result;

		}
		catch (Exception e) {
			event.setDuration(Duration.between(event.getTime(), DateTimeKit.utcNow()));
			try {
				handleError(e, event);
			}
			catch (Throwable any) {
				log.error(any.getMessage(), any);
			}
			throw e;
		}
	}

	void handleResult(Object result, ApiLogEvent event) {
		HttpResponseInfo responseInfo = new HttpResponseInfo();
		if (result instanceof Result) {
			Result<?> r = (Result<?>) result;
			responseInfo.setCode(r.getCode());
			responseInfo.setMessage(r.getMessage());
		}
		event.setResponseInfo(responseInfo);
		SpringEventUtil.publishEvent(event);
	}

	void handleError(Throwable e, ApiLogEvent event) {
		event.setError(ExceptionInfo.from(e, 2000));
		SpringEventUtil.publishEvent(event);
	}

	String getDescription(ProceedingJoinPoint point, ApiLog annotation) {

		if (ObjectUtils.isNotEmpty(annotation.operation())) {
			return annotation.operation();
		}
		final Method method = AopUtil.getMethod(point);
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
		ApplicationContextHolder.getContextOptional().map(ApplicationContext::getApplicationName)
				.ifPresent(v -> appNameRef.compareAndSet(null, v));
		return appNameRef.get();
	}

}
