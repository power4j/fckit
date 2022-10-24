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

package com.power4j.fist.boot.security.config;

import com.power4j.fist.boot.security.filter.JwtAuthenticationFilter;
import com.power4j.fist.boot.security.support.CustomAuthenticationEntryPoint;
import org.springframework.lang.Nullable;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/8
 * @since 1.0
 */
public class ResourceServerSecurityConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

	@Nullable
	private JwtTokenExtractor tokenExtractor;

	@Nullable
	private ResourceServerTokenServices resourceServerTokenServices;

	public void setResourceServerTokenServices(ResourceServerTokenServices resourceServerTokenServices) {
		this.resourceServerTokenServices = resourceServerTokenServices;
	}

	public void setTokenExtractor(JwtTokenExtractor tokenExtractor) {
		this.tokenExtractor = tokenExtractor;
	}

	@Override
	public void configure(HttpSecurity builder) throws Exception {
		JwtAuthenticationFilter authenticationFilter = new JwtAuthenticationFilter(
				Objects.requireNonNull(tokenExtractor), Objects.requireNonNull(resourceServerTokenServices),
				new CustomAuthenticationEntryPoint());
		builder.addFilterBefore(authenticationFilter, AbstractPreAuthenticatedProcessingFilter.class);
	}

}
