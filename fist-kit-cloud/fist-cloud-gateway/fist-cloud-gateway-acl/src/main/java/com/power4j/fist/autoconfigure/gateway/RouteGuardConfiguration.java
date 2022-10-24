package com.power4j.fist.autoconfigure.gateway;

import com.power4j.fist.boot.security.inner.DefaultUserCodec;
import com.power4j.fist.cloud.gateway.ApiGuardFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilterChain;
import com.power4j.fist.cloud.security.AccessDeniedHandler;
import com.power4j.fist.cloud.security.AccessPermittedHandler;
import com.power4j.fist.cloud.security.DefaultAccessDeniedHandler;
import com.power4j.fist.cloud.security.DefaultAccessPermittedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@Import({ Oauth2Configuration.class, GatewayAuthFilterConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class RouteGuardConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AccessPermittedHandler accessPermittedHandler() {
		return new DefaultAccessPermittedHandler(new DefaultUserCodec());
	}

	@Bean
	@ConditionalOnMissingBean
	public AccessDeniedHandler accessDeniedHandler() {
		return new DefaultAccessDeniedHandler();
	}

	@Bean
	@Order
	@ConditionalOnMissingBean
	public ApiGuardFilter apiGuardFilter(GatewayAuthFilterChain authFilterChain,
			AccessPermittedHandler permittedHandler, AccessDeniedHandler deniedHandler) {
		return new ApiGuardFilter(authFilterChain, permittedHandler, deniedHandler);
	}

}
