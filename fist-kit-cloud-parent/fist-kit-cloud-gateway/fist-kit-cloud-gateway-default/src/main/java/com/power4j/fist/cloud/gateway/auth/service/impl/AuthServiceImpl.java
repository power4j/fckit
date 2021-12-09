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

package com.power4j.fist.cloud.gateway.auth.service.impl;

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.error.ErrorCode;
import com.power4j.fist.boot.security.oauth2.Oauth2UserInfoExtractor;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.core.UserInfoExtractor;
import com.power4j.fist.cloud.gateway.auth.infra.service.RemoteTokenService;
import com.power4j.fist.cloud.gateway.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/24
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserInfoExtractor userInfoExtractor = new Oauth2UserInfoExtractor();

	private final RemoteTokenService remoteTokenService;

	@Override
	public Optional<UserInfo> loadUserInfo(String token) {
		Result<Map<String, Object>> result = remoteTokenService.checkToken(token);
		if (result.codeNotEquals(ErrorCode.OK)) {
			log.error("获认证限信息失败:{}", result.simpleDescribe());
			return Optional.empty();
		}
		Map<String, Object> prop = result.getData();
		return userInfoExtractor.extractAuthUser(prop);
	}

}
