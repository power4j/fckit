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

package com.power4j.fist.autoconfigure.security.oauth2.server.resource;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.power4j.fist.boot.common.matcher.FastPathMatcher;
import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.DefaultGatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.GatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.InternalAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.LoadPermissionDefinitionFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.LoginAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.PrepareAuthFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.PublicAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.SkipAuthorizationFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.UpstreamLocateFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl.UserPermissionFilter;
import com.power4j.fist.cloud.gateway.proxy.ProxyResolver;
import com.power4j.fist.security.core.authentication.UserConverter;
import com.power4j.fist.security.core.authorization.config.GlobalAuthorizationProperties;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.service.reactive.PermissionDefinitionService;
import com.power4j.fist.security.oauth2.server.resource.reactive.GatewayAuthorizationManager;
import com.power4j.fist.security.oauth2.token.DefaultUserConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/29
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ GlobalAuthorizationProperties.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class GatewayAuthorizationAutoConfiguration {

	@RequiredArgsConstructor
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = GlobalAuthorizationProperties.PROP_PREFIX, name = "enabled", matchIfMissing = true)
	static class GatewayAuthFilterConfig {

		static final int BASE_ORDER = 10_000;

		private final GlobalAuthorizationProperties authorizationProperties;

		private final ObjectProvider<RouteLocator> routeLocators;

		private final ObjectProvider<ProxyResolver> proxyResolvers;

		private final ObjectProvider<PathMatcher> pathMatchers;

		private final PermissionDefinitionService<? extends PermissionDefinition> permissionDefinitionService;

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(Caffeine.class)
		public FastPathMatcher fastPathMatcher() {
			return new FastPathMatcher(2048);
		}

		@Bean
		@Order(BASE_ORDER + 100)
		public PrepareAuthFilter prepareAuthFilter() {
			return new PrepareAuthFilter();
		}

		@Bean
		@Order(BASE_ORDER + 200)
		public SkipAuthorizationFilter skipAuthorizationFilter(PathMatcher pathMatcher) {
			return new SkipAuthorizationFilter(authorizationProperties.getSkip(), pathMatcher);
		}

		@Bean
		@Order(BASE_ORDER + 300)
		public UpstreamLocateFilter upstreamLocateFilter() {
			UpstreamLocateFilter filter = new UpstreamLocateFilter(routeLocators.getObject());
			proxyResolvers.ifAvailable(filter::setProxyResolver);
			return filter;
		}

		@Bean
		@Order(BASE_ORDER + 400)
		public LoadPermissionDefinitionFilter loadPermissionDefinitionFilter(PathMatcher pathMatcher) {
			return new LoadPermissionDefinitionFilter(pathMatcher, permissionDefinitionService);
		}

		@Bean
		@Order(BASE_ORDER + 500)
		public InternalAccessFilter internalAccessFilter() {
			return new InternalAccessFilter();
		}

		@Bean
		@Order(BASE_ORDER + 600)
		public PublicAccessFilter publicAccessFilter() {
			return new PublicAccessFilter();
		}

		@Bean
		@Order(BASE_ORDER + 1_100)
		public LoginAccessFilter loginAccessFilter() {
			return new LoginAccessFilter();
		}

		@Bean
		@Order(BASE_ORDER + 1_200)
		public UserPermissionFilter userPermissionFilter() {
			return new UserPermissionFilter(authorizationProperties);
		}

		@Bean
		@ConditionalOnMissingBean
		public GatewayAuthFilterChain gatewayAuthFilterChain(List<GatewayAuthFilter> authFilters) {
			return new DefaultGatewayAuthFilterChain(authFilters, 0);
		}

	}

	@Bean
	@ConditionalOnMissingBean
	public HttpMessageConverters messageConverters() {
		// 解决在SCG 中使用feign报错找不到HttpMessageConverters
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new MappingJackson2HttpMessageConverter());
		return new HttpMessageConverters(converters);
	}

	@Bean
	@ConditionalOnMissingBean
	public UserConverter<AuthenticatedUser> defaultUserConverter() {
		return new DefaultUserConverter();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(GatewayAuthFilterChain.class)
	public GatewayAuthorizationManager gatewayAuthorizationManager(GatewayAuthFilterChain authFilterChain,
			UserConverter<AuthenticatedUser> converter, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		return new GatewayAuthorizationManager(authFilterChain, converter, threadPoolTaskExecutor);
	}

}
