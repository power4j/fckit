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

import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilterChain;
import com.power4j.fist.security.core.authentication.UserConverter;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.gateway.GatewayAuthorizationManager;
import com.power4j.fist.security.oauth2.token.DefaultUserConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/13
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class AuthorizeConfigure {

	private final GatewayAuthFilterChain authFilterChain;

	@Bean
	@ConditionalOnMissingBean
	public UserConverter<AuthenticatedUser> defaultUserConverter() {
		return new DefaultUserConverter();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(GatewayAuthFilterChain.class)
	public GatewayAuthorizationManager gatewayAuthorizationManager(UserConverter<AuthenticatedUser> converter) {
		return new GatewayAuthorizationManager(authFilterChain, converter);
	}

}
