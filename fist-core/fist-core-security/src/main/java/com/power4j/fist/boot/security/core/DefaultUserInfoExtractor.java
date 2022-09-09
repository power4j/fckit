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

package com.power4j.fist.boot.security.core;

import com.power4j.fist.boot.common.utils.MapKit;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Map 转 UserInfo
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/20
 * @since 1.0
 */
public class DefaultUserInfoExtractor implements UserInfoExtractor {

	private final static Typed<Collection<Object>> OBJ_COLLECTION = new TypeLiteral<>() {
	};

	private final static char CHAR_COMMA = ',';

	@Override
	public Optional<UserInfo> extractAuthUser(@Nullable Map<String, ?> userInfoMap) {
		return Optional.ofNullable(extractFromMap(userInfoMap));
	}

	private List<String> getAuthorities(Map<String, ?> map) {
		Object authorities = map.get(AUTHORITIES);
		if (authorities instanceof String) {
			return Arrays.asList(StringUtils.split((String) authorities, CHAR_COMMA));
		}
		if (authorities instanceof Collection) {
			return ((Collection<?>) authorities).stream().map(Object::toString).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected UserInfo extractFromMap(@Nullable Map<String, ?> userInfoMap) {
		if (Objects.isNull(userInfoMap)) {
			return null;
		}
		String nickName = MapUtils.getString(userInfoMap, SecurityConstant.UserProp.KEY_NICK_NAME);
		String avatarUrl = MapUtils.getString(userInfoMap, SecurityConstant.UserProp.KEY_AVATAR_URL);
		String username = MapUtils.getString(userInfoMap, SecurityConstant.UserProp.KEY_USERNAME);
		Long userId = MapUtils.getLong(userInfoMap, SecurityConstant.UserProp.KEY_USER_ID);
		String userSource = MapUtils.getString(userInfoMap, SecurityConstant.UserProp.KEY_USER_SOURCE);
		String dept = MapUtils.getString(userInfoMap, SecurityConstant.UserProp.KEY_DEPT);
		List<String> roleList = MapKit.readAsList(userInfoMap, SecurityConstant.UserProp.KEY_ROLE_LIST,
				Objects::toString);
		List<String> permissions = MapKit.readAsList(userInfoMap, SecurityConstant.UserProp.KEY_PERMISSION_LIST,
				Objects::toString);

		UserInfo info = new UserInfo();
		info.setUsername(username);
		info.setUserId(userId);
		info.setNickName(nickName);
		info.setAvatarUrl(avatarUrl);
		info.putMetaProp(SecurityConstant.UserProp.KEY_ROLE_LIST, roleList);
		info.putMetaProp(SecurityConstant.UserProp.KEY_PERMISSION_LIST, permissions);
		info.putMetaProp(SecurityConstant.UserProp.KEY_USER_SOURCE, userSource);
		info.putMetaProp(SecurityConstant.UserProp.KEY_DEPT, dept);
		return info;
	}

}
