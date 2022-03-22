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

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.power4j.coca.kit.common.text.Chars;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

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
@Slf4j
public class DefaultUserInfoExtractor implements UserInfoExtractor {

	final TypeReference<List<String>> strListType = new TypeReference<List<String>>() {
	};

	@Override
	public Optional<UserInfo> extractAuthUser(@Nullable Map<String, ?> userInfoMap) {
		return Optional.ofNullable(extractFromMap(userInfoMap));
	}

	private List<String> getAuthorities(Map<String, ?> map) {
		Object authorities = map.get(AUTHORITIES);
		if (authorities instanceof String) {
			return CharSequenceUtil.split((String) authorities, Chars.COMMA);
		}
		if (authorities instanceof Collection) {
			return ((Collection<?>) authorities).stream().map(Object::toString).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Nullable
	protected UserInfo extractFromMap(@Nullable Map<String, ?> userInfoMap) {
		if (Objects.isNull(userInfoMap)) {
			return null;
		}
		String nickName = MapUtil.getStr(userInfoMap, SecurityConstant.UserProp.KEY_NICK_NAME);
		String avatarUrl = MapUtil.getStr(userInfoMap, SecurityConstant.UserProp.KEY_AVATAR_URL);
		String username = MapUtil.getStr(userInfoMap, SecurityConstant.UserProp.KEY_USERNAME);
		Long userId = MapUtil.getLong(userInfoMap, SecurityConstant.UserProp.KEY_USER_ID);
		String userSource = MapUtil.getStr(userInfoMap, userInfoMap, SecurityConstant.UserProp.KEY_USER_SOURCE);
		String dept = MapUtil.getStr(userInfoMap, SecurityConstant.UserProp.KEY_DEPT);
		List<String> roleList = MapUtil.get(userInfoMap, SecurityConstant.UserProp.KEY_ROLE_LIST, strListType,
				Collections.emptyList());
		List<String> permissions = MapUtil.get(userInfoMap, SecurityConstant.UserProp.KEY_PERMISSION_LIST, strListType,
				Collections.emptyList());

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
