package com.power4j.fist.sde.boot.autoconfigure.client;

import com.power4j.fist.sde.client.SecureExchangeOperations;
import com.power4j.fist.sde.client.httpservice.SecureHttpServiceClientContextProcessor;
import com.power4j.fist.sde.client.webclient.SecureWebClientExchangeFilterFunction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * SDE WebClient 自动配置。
 * <p>
 * 注册 ExchangeFilterFunction，并在 Spring Boot 4 不再提供 WebClientCustomizer 时提供带 SDE 过滤器的
 * WebClient.Builder。
 */
@AutoConfiguration(after = SdeClientAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
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
	 * 提供已挂载 SDE 过滤器的 WebClient.Builder。
	 * @param filterFunction WebClient 过滤器
	 * @return WebClient.Builder
	 */
	@Bean
	@ConditionalOnMissingBean(WebClient.Builder.class)
	public WebClient.Builder secureSdeWebClientBuilder(SecureWebClientExchangeFilterFunction filterFunction) {
		return WebClient.builder().filter(filterFunction);
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
