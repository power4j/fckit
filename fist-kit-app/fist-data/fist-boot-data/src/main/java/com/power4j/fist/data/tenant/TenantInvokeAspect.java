package com.power4j.fist.data.tenant;

import com.power4j.fist.boot.apidoc.ApiTrait;
import com.power4j.fist.data.tenant.isolation.TenantBroker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Aspect
public class TenantInvokeAspect {

	@Around("@annotation(annotation)")
	public Object around(ProceedingJoinPoint point, ApiTrait annotation) throws Throwable {
		if (Objects.equals(ApiTrait.ApiLevel.PL, annotation.level())) {
			return TenantBroker.applyAs(null, point::proceed);
		}
		return point.proceed();
	}

}
