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

package com.power4j.fist.boot.security.inner;

import cn.hutool.core.text.CharSequenceUtil;
import com.power4j.coca.kit.common.exception.WrappedException;
import com.power4j.fist.boot.security.context.UserContextHolder;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/20
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class TrustedUserFilter extends OncePerRequestFilter {

	private final static String USER_ATTR_KEY = "fiamc.trusted-user-info-value";

	private final UserDecoder userDecoder;

	@Setter
	private boolean strictMode = true;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String userValue = request.getHeader(SecurityConstant.HEADER_USER_TOKEN_INNER);
		UserContextHolder.setOriginalValue(userValue);
		request.setAttribute(USER_ATTR_KEY, userValue);
		UserInfo info = null;
		if (CharSequenceUtil.isNotEmpty(userValue) && isTrusted(request)) {
			try {
				info = userDecoder.decode(userValue);
			}
			catch (UserCodecException e) {
				log.warn(e.getMessage(), e);
			}
		}
		try {
			SecurityUtil.act(info, () -> filterChain.doFilter(request, response));
		}
		catch (WrappedException e) {
			if (e.getCause() instanceof ServletException) {
				throw e.getOriginal(ServletException.class);
			}
			else if (e.getCause() instanceof IOException) {
				throw e.getOriginal(IOException.class);
			}
		}
	}

	private boolean isTrusted(HttpServletRequest request) {
		if (strictMode) {
			String ip = request.getRemoteAddr();
			if (!isTrustedIp(ip)) {
				log.warn("Request IP Not trusted : {}", ip);
				return false;
			}
		}
		return true;
	}

	private boolean isTrustedIp(String ip) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(ip);
		}
		catch (UnknownHostException e) {
			log.error(e.getMessage(), e);
			return false;
		}
		return address.isLoopbackAddress() || address.isSiteLocalAddress();
	}

}
