package com.power4j.fist.sde.boot.autoconfigure.client;

import com.power4j.fist.sde.client.SecureExchangeOperations;
import com.power4j.fist.sde.client.httpservice.SecureHttpServiceClientContextProcessor;
import com.power4j.fist.sde.client.webclient.SecureWebClientExchangeFilterFunction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;

/**
 * SDE WebClient 自动配置。
 * <p>
 * 注册 ExchangeFilterFunction 并通过 {@link WebClientCustomizer} 挂载到 Spring Boot 管理的
 * WebClient.Builder。
 */
@AutoConfiguration(after = SdeClientAutoConfiguration.class)
@ConditionalOnClass(name = { "org.springframework.web.reactive.function.client.WebClient",
		"org.springframework.boot.webclient.WebClientCustomizer" })
@ConditionalOnBean(SecureExchangeOperations.class)
public class SdeWebClientAutoConfiguration {

	/**
	 * 提供 WebClient 交换过滤器。
	 * @param operations SDE 客户端操作入口
	 * @return WebClient 过滤器
	 */
	@Bean
	@ConditionalOnMissingBean
	public SecureWebClientExchangeFilterFunction secureWebClientExchangeFilterFunction(
			SecureExchangeOperations operations) {
		return new SecureWebClientExchangeFilterFunction(operations);
	}

	/**
	 * 将 SDE 过滤器挂载到 Spring Boot 管理的 WebClient.Builder。
	 * @param filterFunction WebClient 过滤器
	 * @return WebClient 定制器
	 */
	@Bean
	@ConditionalOnMissingBean(name = "secureSdeWebClientCustomizer")
	public WebClientCustomizer secureSdeWebClientCustomizer(SecureWebClientExchangeFilterFunction filterFunction) {
		return (builder) -> builder.filters((filters) -> {
			if (!filters.contains(filterFunction)) {
				filters.add(filterFunction);
			}
		});
	}

	/**
	 * 将 SDE 过滤器和方法级上下文处理器挂载到 WebClient-backed HTTP Service Clients。
	 * @param filterFunction WebClient 过滤器
	 * @return HTTP Service Clients 组定制器
	 */
	@Bean
	@ConditionalOnClass(WebClientHttpServiceGroupConfigurer.class)
	@ConditionalOnMissingBean(name = "secureSdeWebClientHttpServiceGroupConfigurer")
	public WebClientHttpServiceGroupConfigurer secureSdeWebClientHttpServiceGroupConfigurer(
			SecureWebClientExchangeFilterFunction filterFunction) {
		SecureHttpServiceClientContextProcessor processor = new SecureHttpServiceClientContextProcessor();
		return (groups) -> groups.forEachGroup((group, clientBuilder, proxyFactoryBuilder) -> {
			clientBuilder.filters((filters) -> {
				if (!filters.contains(filterFunction)) {
					filters.add(filterFunction);
				}
			});
			proxyFactoryBuilder.httpRequestValuesProcessor(processor);
		});
	}

}
