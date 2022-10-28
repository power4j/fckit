package com.power4j.fist.data.tenant;

import com.power4j.fist.boot.apidoc.ApiTrait;
import com.power4j.fist.data.tenant.isolation.TenantBroker;
import org.apache.commons.lang3.mutable.MutableObject;
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
		final MutableObject<Throwable> err = new MutableObject<>();
		if (Objects.equals(ApiTrait.ApiLevel.PL, annotation.level())) {
			return TenantBroker.applyAs(null, point::proceed);
		}
		return point.proceed();
	}

}
