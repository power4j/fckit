/*
 *  Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.gnu.org/licenses/lgpl.html
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.power4j.fist.cloud.autoconfigure.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.fist.cloud.gateway.auth.api.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.auth.configure.FistGatewayProperties;
import com.power4j.fist.cloud.gateway.auth.filter.BlackListFilter;
import com.power4j.fist.cloud.gateway.auth.filter.WhiteListFilter;
import com.power4j.fist.cloud.gateway.auth.repository.IpRuleRepository;
import com.power4j.fist.cloud.gateway.auth.util.DefaultGatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.auth.util.GlobalFilterAdapter;
import com.power4j.fist.cloud.gateway.filter.PreProcessFilter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/5
 * @since 1.0
 */
@ComponentScan("com.power4j.fist.cloud.gateway")
@MapperScan("com.power4j.fist.cloud.gateway.auth.infra.mapper")
@Configuration(proxyBeanMethods = false)
public class FistGatewayAutoConfiguration {

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@Bean
	public PreProcessFilter preProcessFilter() {
		return new PreProcessFilter();
	}

	@Order(1000)
	@Bean
	@ConditionalOnProperty(prefix = FistGatewayProperties.PROP_PREFIX + ".ip-filter", name = "active",
			havingValue = "blacklist")
	public BlackListFilter blackListFilter(ObjectMapper objectMapper, IpRuleRepository ipRuleRepository) {
		return new BlackListFilter(objectMapper, ipRuleRepository);
	}

	@Order(1000)
	@Bean
	@ConditionalOnProperty(prefix = FistGatewayProperties.PROP_PREFIX + ".ip-filter", name = "active",
			havingValue = "whitelist")
	public WhiteListFilter whiteListFilter(ObjectMapper objectMapper, IpRuleRepository ipRuleRepository) {
		return new WhiteListFilter(objectMapper, ipRuleRepository);
	}

	@Order(2000)
	@Bean
	@ConditionalOnProperty(prefix = FistGatewayProperties.PROP_PREFIX + "permission", name = "enabled",
			matchIfMissing = true)
	public GlobalFilter authFilterChain(ObjectProvider<GatewayAuthFilter> gatewayAuthFilters,
			FistGatewayProperties gatewayProperties, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		List<GatewayAuthFilter> filterList = gatewayAuthFilters.orderedStream().collect(Collectors.toList());
		if (filterList.isEmpty()) {
			throw new IllegalStateException("No GatewayAuthFilter");
		}
		DefaultGatewayAuthFilterChain filterChain = new DefaultGatewayAuthFilterChain(filterList, 0);
		return new GlobalFilterAdapter(gatewayProperties, filterChain, threadPoolTaskExecutor);
	}

}
