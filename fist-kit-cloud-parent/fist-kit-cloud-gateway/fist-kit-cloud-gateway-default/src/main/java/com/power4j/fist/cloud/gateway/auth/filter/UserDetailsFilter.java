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

package com.power4j.fist.cloud.gateway.auth.filter;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.DesensitizedUtil;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import com.power4j.fist.cloud.gateway.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/22
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class UserDetailsFilter extends AbstractAuthFilter {

	private final AuthService authService;

	@Override
	protected boolean process(AuthContext context) {
		final String accessToken = context.getRequestInfo().getAccessToken();
		if (CharSequenceUtil.isEmpty(accessToken)) {
			if (log.isDebugEnabled()) {
				log.debug("accessToken is empty,skip load UserDetails");
			}
			return true;
		}

		UserInfo authUser = authService.loadUserInfo(accessToken).orElse(null);

		if (Objects.nonNull(authUser)) {
			if (log.isDebugEnabled()) {
				log.debug("loadUserDetails successfully,tenant ID = {},user id = {},username = {}",
						authUser.getTenantId(), authUser.getUserId(), authUser.getUsername());
			}
			context.setAuthUser(authUser);
		}
		else {
			final String maskedToken = CharSequenceUtil.desensitized(accessToken,
					DesensitizedUtil.DesensitizedType.MOBILE_PHONE);
			log.debug("access token is invalid:{}", maskedToken);
		}
		return true;
	}

}
