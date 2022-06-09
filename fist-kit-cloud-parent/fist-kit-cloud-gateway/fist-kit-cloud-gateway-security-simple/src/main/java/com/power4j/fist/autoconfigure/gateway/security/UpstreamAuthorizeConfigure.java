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

import com.power4j.fist.cloud.gateway.authorization.filter.simple.GatewayAuthFilterChain;
import com.power4j.fist.security.core.authentication.UserConverter;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.gateway.UpstreamAuthorizationManager;
import com.power4j.fist.security.oauth2.token.DefaultUserConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/13
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class UpstreamAuthorizeConfigure {

	private final GatewayAuthFilterChain authFilterChain;

	@Bean
	@ConditionalOnMissingBean
	public HttpMessageConverters messageConverters() {
		// 解决在SCG 中使用feign报错找不到HttpMessageConverters
		// FIXME: 不建议使用feign
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
	public UpstreamAuthorizationManager upstreamAuthorizationManager(UserConverter<AuthenticatedUser> converter,
			ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		return new UpstreamAuthorizationManager(authFilterChain, converter, threadPoolTaskExecutor);
	}

}
