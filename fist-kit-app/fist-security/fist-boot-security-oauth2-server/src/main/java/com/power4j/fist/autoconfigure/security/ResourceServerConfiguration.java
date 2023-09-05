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

package com.power4j.fist.autoconfigure.security;

import com.power4j.coca.kit.common.exception.RuntimeFaultException;
import com.power4j.fist.boot.security.config.JwtTokenExtractor;
import com.power4j.fist.boot.security.config.ResourceServerConfig;
import com.power4j.fist.boot.security.config.ResourceServerSecurityConfig;
import com.power4j.fist.boot.security.config.SecurityProperties;
import com.power4j.fist.boot.security.oauth2.AuthUserConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/5
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class ResourceServerConfiguration extends WebSecurityConfigurerAdapter {

	private final SecurityProperties securityProperties;

	@Nullable
	private JwtAccessTokenConverter accessTokenConverter;

	@Nullable
	private TokenStore tokenStore;

	@Nullable
	private JwtTokenExtractor tokenExtractor;

	private List<ResourceServerConfig> configs = Collections.emptyList();

	@Autowired(required = false)
	public void setAccessTokenConverter(JwtAccessTokenConverter accessTokenConverter) {
		this.accessTokenConverter = accessTokenConverter;
	}

	@Autowired(required = false)
	public void setTokenStore(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	@Autowired(required = false)
	public void setTokenExtractor(JwtTokenExtractor tokenExtractor) {
		this.tokenExtractor = tokenExtractor;
	}

	@Autowired(required = false)
	public void setConfigs(List<ResourceServerConfig> configs) {
		this.configs = configs;
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/actuator/**", "/monitor/**", "/v3/api-docs/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		ResourceServerSecurityConfig configurer = new ResourceServerSecurityConfig();
		configurer.setResourceServerTokenServices(resourceServerTokenServices());
		if (Objects.isNull(tokenExtractor)) {
			configurer.setTokenExtractor(new JwtTokenExtractor(securityProperties.getTokenHeader()));
		}
		else {
			configurer.setTokenExtractor(tokenExtractor);
		}

		for (ResourceServerConfig config : configs) {
			config.configure(configurer);
		}

		http.httpBasic().disable().csrf().disable();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.apply(configurer);
		for (ResourceServerConfig config : configs) {
			// 业务服务在这里自定义 authorizeRequests()
			config.configure(http);
		}
		// Add anyRequest() last as a fall back ?
	}

	@Bean
	@ConditionalOnMissingBean
	public ResourceServerTokenServices resourceServerTokenServices() {
		DefaultTokenServices tokenServices = new DefaultTokenServices();
		tokenServices.setTokenStore(tokenStore == null ? defaultTokenStore() : tokenStore);
		try {
			tokenServices.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeFaultException(e);
		}
		return tokenServices;
	}

	TokenStore defaultTokenStore() {
		return new JwtTokenStore(accessTokenConverter == null ? defaultAccessTokenConverter() : accessTokenConverter);
	}

	JwtAccessTokenConverter defaultAccessTokenConverter() {
		DefaultAccessTokenConverter defaultAccessTokenConverter = new DefaultAccessTokenConverter();
		defaultAccessTokenConverter.setUserTokenConverter(new AuthUserConverter());
		JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
		tokenConverter.setSigningKey(securityProperties.getJwt().getKey());
		tokenConverter.setAccessTokenConverter(defaultAccessTokenConverter);
		try {
			tokenConverter.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeFaultException(e);
		}
		return tokenConverter;
	}

}
