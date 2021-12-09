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

package com.power4j.fist.boot.security.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证过滤器,完成 {@code OAuth2AuthenticationProcessingFilter} 类似的工作
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/2
 * @since 1.0
 * @see OAuth2AuthenticationProcessingFilter
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final TokenExtractor tokenExtractor;

	private final ResourceServerTokenServices tokenServices;

	private final AuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		final boolean debug = log.isDebugEnabled();
		try {
			Authentication authentication = tokenExtractor.extract(request);
			if (authentication == null) {
				if (isAuthenticated()) {
					if (debug) {
						logger.debug("Clearing security context.");
					}
					SecurityContextHolder.clearContext();
				}
				if (debug) {
					logger.debug("No token in request, will continue chain.");
				}
			}
			else {
				request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, authentication.getPrincipal());
				if (authentication instanceof AbstractAuthenticationToken) {
					AbstractAuthenticationToken needsDetails = (AbstractAuthenticationToken) authentication;
					needsDetails.setDetails(new OAuth2AuthenticationDetails(request));
				}
				Authentication authResult = authenticate(authentication);
				if (debug) {
					logger.debug("Authentication success: " + authResult);
				}
				SecurityContextHolder.getContext().setAuthentication(authResult);
			}
		}
		catch (OAuth2Exception failed) {
			SecurityContextHolder.clearContext();
			if (debug) {
				logger.debug("Authentication request failed: " + failed);
			}
			authenticationEntryPoint.commence(request, response,
					new InsufficientAuthenticationException(failed.getMessage(), failed));
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
	}

	/**
	 * 模拟 {@code OAuth2AuthenticationManager}
	 * @param authentication an authentication request containing an access token value as
	 * the principal
	 * @return an {@link OAuth2Authentication}
	 */
	private Authentication authenticate(@Nullable Authentication authentication) throws AuthenticationException {
		if (authentication == null) {
			throw new InvalidTokenException("Invalid token (token not found)");
		}
		String token = (String) authentication.getPrincipal();
		OAuth2Authentication auth = tokenServices.loadAuthentication(token);
		if (auth == null) {
			throw new InvalidTokenException("Invalid token: " + token);
		}

		if (authentication.getDetails() instanceof OAuth2AuthenticationDetails) {
			OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication.getDetails();
			// Guard against a cached copy of the same details
			if (!details.equals(auth.getDetails())) {
				// Preserve the authentication details from the one loaded by token
				// services
				details.setDecodedDetails(auth.getDetails());
			}
		}
		auth.setDetails(authentication.getDetails());
		auth.setAuthenticated(true);
		return auth;

	}

}
