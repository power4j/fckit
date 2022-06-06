package com.power4j.fist.boot.mon.aspect;

import com.power4j.fist.boot.mon.info.DefaultExceptionTranslator;
import com.power4j.fist.boot.mon.info.ExceptionTranslator;
import com.power4j.fist.boot.security.core.UserInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/23
 * @since 1.0
 */
@ComponentScan
@EnableAspectJAutoProxy
@Configuration(proxyBeanMethods = false)
public class ApiLogConfig {

	@Bean
	public ExceptionTranslator defaultExceptionTranslator() {
		return new DefaultExceptionTranslator(null);
	}

	@Bean
	public ApiLogAspect apiLogAspect(ExceptionTranslator translator) {
		return new ApiLogAspect(translator, () -> Optional.of(new UserInfo()));
	}

}
