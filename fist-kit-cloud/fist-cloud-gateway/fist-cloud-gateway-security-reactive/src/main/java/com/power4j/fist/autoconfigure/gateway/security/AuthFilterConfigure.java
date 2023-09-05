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
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.DefaultGatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.AuthEndFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.InternalAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.LoadPermissionDefinitionFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.LoginAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.PrepareAuthFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.PublicAccessFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.SafeModeFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.SkipAuthorizationFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.TenantFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.UpstreamLocateFilter;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl.UserPermissionFilter;
import com.power4j.fist.cloud.gateway.proxy.ProxyResolver;
import com.power4j.fist.security.core.authorization.config.GlobalAuthorizationProperties;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.service.reactive.ReactivePermissionDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/13
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GlobalAuthorizationProperties.class)
@ConditionalOnProperty(prefix = GlobalAuthorizationProperties.PROP_PREFIX, name = "enabled", matchIfMissing = true)
public class AuthFilterConfigure {

	static final int BASE_ORDER = 10_000;

	private final GlobalAuthorizationProperties authorizationProperties;

	private final ObjectProvider<RouteLocator> routeLocators;

	private final ObjectProvider<ProxyResolver> proxyResolvers;

	private final ReactivePermissionDefinitionService<? extends PermissionDefinition> permissionDefinitionService;

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
	@ConditionalOnProperty(prefix = GlobalAuthorizationProperties.PROP_PREFIX, name = "safe-mode.enabled",
			havingValue = "true")
	public SafeModeFilter safeModeFilter() {
		List<String> whitelist = authorizationProperties.getSafeMode().getWhitelist();
		log.info("{} init with whitelist :{}", SafeModeFilter.class.getSimpleName(), StringUtils.join(whitelist, ","));
		return new SafeModeFilter(whitelist);
	}

	@Bean
	@Order(BASE_ORDER + 200)
	public SkipAuthorizationFilter skipAuthorizationFilter(PathMatcher pathMatcher) {
		Set<String> skip = authorizationProperties.getSkip();
		log.info("{} init with skip list :{}", SkipAuthorizationFilter.class.getSimpleName(),
				StringUtils.join(skip, ","));
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
	@Order(BASE_ORDER + 1_190)
	@ConditionalOnProperty(prefix = GlobalAuthorizationProperties.PROP_FILTERS + ".tenant", name = "enabled",
			matchIfMissing = true)
	public TenantFilter tenantFilter() {
		final String paramKey = authorizationProperties.getAuth().getTenantParameter();
		final String headerKey = authorizationProperties.getAuth().getTenantHeader();
		return new TenantFilter(paramKey, headerKey);
	}

	@Bean
	@Order(BASE_ORDER + 1_200)
	public UserPermissionFilter userPermissionFilter() {
		return new UserPermissionFilter();
	}

	@Bean
	@Order
	public AuthEndFilter authEndFilter() {
		return new AuthEndFilter();
	}

	@Bean
	@ConditionalOnMissingBean
	public GatewayAuthFilterChain gatewayAuthFilterChain(List<GatewayAuthFilter> authFilters) {
		return new DefaultGatewayAuthFilterChain(authFilters);
	}

}
