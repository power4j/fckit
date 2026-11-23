package com.power4j.fist.sde.boot.autoconfigure.client;

import com.power4j.fist.sde.client.SecureExchangeOperations;
import com.power4j.fist.sde.client.httpservice.SecureHttpServiceClientContextProcessor;
import com.power4j.fist.sde.client.restclient.SecureRestClientInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * SDE RestClient 自动配置。
 * <p>
 * 注册请求拦截器并通过 {@link RestClientCustomizer} 挂载到 Spring Boot 管理的 RestClient.Builder。
 */
@AutoConfiguration(after = SdeClientAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.client.RestClient")
@ConditionalOnBean(SecureExchangeOperations.class)
public class SdeRestClientAutoConfiguration {

	/**
	 * 提供 RestClient 请求拦截器。
	 * @param operations SDE 客户端操作入口
	 * @return RestClient 拦截器
	 */
	@Bean
	@ConditionalOnMissingBean
	public SecureRestClientInterceptor secureRestClientInterceptor(SecureExchangeOperations operations) {
		return new SecureRestClientInterceptor(operations);
	}

	/**
	 * 将 SDE 拦截器挂载到 RestClient.Builder。
	 * @param interceptor RestClient 请求拦截器
	 * @return RestClient 定制器
	 */
	@Bean
	@ConditionalOnMissingBean(name = "secureSdeRestClientCustomizer")
	public RestClientCustomizer secureSdeRestClientCustomizer(SecureRestClientInterceptor interceptor) {
		return (builder) -> builder.requestInterceptor(interceptor);
	}

	/**
	 * 将 SDE 拦截器和方法级上下文处理器挂载到 RestClient-backed HTTP Service Clients。
	 * @param interceptor RestClient 请求拦截器
	 * @return HTTP Service Clients 组定制器
	 */
	@Bean
	@ConditionalOnClass(RestClientHttpServiceGroupConfigurer.class)
	@ConditionalOnMissingBean(name = "secureSdeRestClientHttpServiceGroupConfigurer")
	public RestClientHttpServiceGroupConfigurer secureSdeRestClientHttpServiceGroupConfigurer(
			SecureRestClientInterceptor interceptor) {
		SecureHttpServiceClientContextProcessor processor = new SecureHttpServiceClientContextProcessor();
		return (groups) -> groups.forEachGroup((group, clientBuilder, proxyFactoryBuilder) -> {
			clientBuilder.requestInterceptors((interceptors) -> {
				if (!interceptors.contains(interceptor)) {
					interceptors.add(interceptor);
				}
			});
			proxyFactoryBuilder.httpRequestValuesProcessor(processor);
		});
	}

}
