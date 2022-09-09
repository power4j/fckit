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

package com.power4j.fist.boot.security.oauth2;

import com.power4j.fist.boot.security.core.DefaultUserInfoExtractor;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 解析认证服务器返回的用户信息
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/8
 * @since 1.0
 */
@Slf4j
public class Oauth2UserInfoExtractor extends DefaultUserInfoExtractor {

	protected final static String KEY_CLIENT_ID = "client_id";

	@SuppressWarnings("unchecked")
	@Override
	public Optional<UserInfo> extractAuthUser(@Nullable Map<String, ?> map) {
		if (Objects.isNull(map)) {
			return Optional.empty();
		}
		String clientId = MapUtils.getString(map, KEY_CLIENT_ID);
		Map<String, Object> userInfoMap = (Map<String, Object>) Optional.of(map)
				.map(o -> o.get(SecurityConstant.Auth2.INFO_KEY_USER_INFO)).orElse(null);

		if (Objects.isNull(userInfoMap)) {
			log.debug("client authentication :{}", clientId);
			return Optional.empty();
		}
		UserInfo info = extractFromMap(userInfoMap);
		if (Objects.nonNull(info)) {
			info.putMetaProp(SecurityConstant.UserProp.KEY_CLIENT_ID, clientId);
		}
		return Optional.ofNullable(info);
	}

}
