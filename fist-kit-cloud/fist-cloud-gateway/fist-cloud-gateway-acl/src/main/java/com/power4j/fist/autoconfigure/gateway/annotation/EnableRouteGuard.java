package com.power4j.fist.autoconfigure.gateway.annotation;

import com.power4j.fist.autoconfigure.gateway.RouteGuardConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ]
 *
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({ RouteGuardConfiguration.class })
public @interface EnableRouteGuard {

}
