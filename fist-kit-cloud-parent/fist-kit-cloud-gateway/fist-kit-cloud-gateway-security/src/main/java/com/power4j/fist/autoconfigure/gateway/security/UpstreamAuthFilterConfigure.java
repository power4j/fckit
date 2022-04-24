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

package com.power4j.fist.autoconfigure.gateway.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.power4j.fist.boot.common.matcher.FastPathMatcher;
import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.DefaultGatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.GatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.AuthEndFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.InternalAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.LoadPermissionDefinitionFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.LoginAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.PrepareAuthFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.PublicAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.SafeModeFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.SkipAuthorizationFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.UpstreamLocateFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.impl.UserPermissionFilter;
import com.power4j.fist.cloud.gateway.proxy.ProxyResolver;
import com.power4j.fist.security.core.authorization.config.GlobalAuthorizationProperties;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.service.reactive.PermissionDefinitionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/13
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GlobalAuthorizationProperties.class)
@ConditionalOnProperty(prefix = GlobalAuthorizationProperties.PROP_PREFIX, name = "enabled", matchIfMissing = true)
public class UpstreamAuthFilterConfigure {

	static final int BASE_ORDER = 10_000;

	private final GlobalAuthorizationProperties authorizationProperties;

	private final ObjectProvider<RouteLocator> routeLocators;

	private final ObjectProvider<ProxyResolver> proxyResolvers;

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
	@Order(BASE_ORDER + 110)
	@ConditionalOnProperty(prefix = GlobalAuthorizationProperties.PROP_PREFIX, name = "safe-mode", havingValue = "true")
	public SafeModeFilter safeModeFilter() {
		Set<String> ips = authorizationProperties.getSafeModeIpList();
		if (ObjectUtils.isEmpty(ips)) {
			ips = new HashSet<>();
			ips.add("127.0.0.1");
		}
		return new SafeModeFilter(ips);
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
	@Order(Ordered.LOWEST_PRECEDENCE)
	public AuthEndFilter authEndFilter() {
		return new AuthEndFilter();
	}

	@Bean
	@ConditionalOnMissingBean
	public GatewayAuthFilterChain gatewayAuthFilterChain(List<GatewayAuthFilter> authFilters) {
		return new DefaultGatewayAuthFilterChain(authFilters, 0);
	}

}
