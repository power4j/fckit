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

package com.power4j.fist.cloud.gateway.config;

import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.boot.security.inner.DefaultUserCodec;
import com.power4j.fist.boot.security.inner.UserEncoder;
import com.power4j.fist.cloud.gateway.auth.api.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.auth.configure.FistGatewayProperties;
import com.power4j.fist.cloud.gateway.auth.filter.AuthEndFilter;
import com.power4j.fist.cloud.gateway.auth.filter.EncodeUserFilter;
import com.power4j.fist.cloud.gateway.auth.filter.InternalAccessFilter;
import com.power4j.fist.cloud.gateway.auth.filter.PermissionFilter;
import com.power4j.fist.cloud.gateway.auth.filter.PermissionInfoFilter;
import com.power4j.fist.cloud.gateway.auth.filter.PublicAccessFilter;
import com.power4j.fist.cloud.gateway.auth.filter.SkipAuthFilter;
import com.power4j.fist.cloud.gateway.auth.filter.UserAccessFilter;
import com.power4j.fist.cloud.gateway.auth.filter.UserDetailsFilter;
import com.power4j.fist.boot.common.matcher.FastPathMatcher;
import com.power4j.fist.cloud.gateway.auth.oauth2.Oauth2Client;
import com.power4j.fist.cloud.gateway.auth.oauth2.impl.DefaultOauth2Client;
import com.power4j.fist.cloud.gateway.auth.service.AuthService;
import com.power4j.fist.cloud.gateway.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/13
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FistGatewayProperties.class)
public class GatewayAuthFilterConfig {

	private final FistGatewayProperties gatewayProperties;

	@Bean
	@ConditionalOnMissingBean
	public UserEncoder userEncoder() {
		return new DefaultUserCodec();
	}

	@Bean
	@ConditionalOnMissingBean
	public FastPathMatcher fastPathMatcher() {
		return new FastPathMatcher(1_000);
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 100)
	@Bean
	public SkipAuthFilter skipAuthFilter(PathMatcher pathMatcher) {
		return new SkipAuthFilter(pathMatcher);
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 500)
	@Bean
	public PermissionInfoFilter permissionInfoFilter(PermissionService permissionService) {
		return new PermissionInfoFilter(permissionService);
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 1000)
	@Bean
	public InternalAccessFilter internalAccessFilter() {
		return new InternalAccessFilter();
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 1200)
	@Bean
	public PublicAccessFilter publicAccessFilter() {
		return new PublicAccessFilter();
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 1400)
	@Bean
	public UserDetailsFilter userDetailsFilter(AuthService authService) {
		return new UserDetailsFilter(authService);
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 1500)
	@Bean
	public UserAccessFilter userAccessFilter() {
		return new UserAccessFilter();
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 1600)
	@Bean
	public EncodeUserFilter encodeUserFilter(UserEncoder userEncoder) {
		return new EncodeUserFilter(userEncoder);
	}

	@Order(GatewayAuthFilter.BASE_ORDER + 1800)
	@Bean
	public PermissionFilter permissionFilter(PermissionService permissionService, PathMatcher pathMatcher) {
		return new PermissionFilter(permissionService, pathMatcher);
	}

	@Bean
	public AuthEndFilter authEndFilter() {
		return new AuthEndFilter();
	}

	@Bean
	@ConditionalOnMissingBean
	public Oauth2Client oauth2Client(RestTemplate restTemplate) {
		DefaultOauth2Client client = new DefaultOauth2Client(restTemplate,
				gatewayProperties.getPermission().getOauthClient().getTokenInfoUri());
		client.setClientId(gatewayProperties.getPermission().getOauthClient().getClientId());
		client.setClientSecret(gatewayProperties.getPermission().getOauthClient().getClientSecret());
		return client;
	}

}
