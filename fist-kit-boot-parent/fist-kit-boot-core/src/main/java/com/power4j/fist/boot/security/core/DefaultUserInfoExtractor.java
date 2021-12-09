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
import com.power4j.coca.kit.common.text.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
		String username = MapUtil.getStr(userInfoMap, "username");
		String clientId = MapUtil.getStr(userInfoMap, "client_id", StringPool.EMPTY);
		;
		boolean enabled = MapUtil.getBool(userInfoMap, "enabled");
		boolean accountNonExpired = MapUtil.getBool(userInfoMap, "accountNonExpired");
		boolean credentialsNonExpired = MapUtil.getBool(userInfoMap, "credentialsNonExpired");
		boolean accountNonLocked = MapUtil.getBool(userInfoMap, "accountNonLocked");
		List<String> authorities = getAuthorities(userInfoMap);

		UserInfo info = new UserInfo();
		info.setUsername(username);
		info.setClientId(clientId);
		info.setEnabled(enabled);
		info.setAccountNonExpired(accountNonExpired);
		info.setCredentialsNonExpired(credentialsNonExpired);
		info.setAccountNonLocked(accountNonLocked);
		info.setAuthorities(new HashSet<>(authorities));

		Long userId = MapUtil.getLong(userInfoMap, "userId");
		String tenantId = MapUtil.getStr(userInfoMap, "tenantId");
		String nickName = MapUtil.getStr(userInfoMap, "nickName");
		String avatarUrl = MapUtil.getStr(userInfoMap, "avatarUrl");
		List<Long> roleIdList = MapUtil.get(userInfoMap, "roleIdList", new TypeReference<List<Long>>() {
		}, Collections.emptyList());
		List<String> permissionList = MapUtil.get(userInfoMap, "permissionList", new TypeReference<List<String>>() {
		}, Collections.emptyList());
		Map<String, Object> additionalInfo = MapUtil.get(userInfoMap, "additionalInfo",
				new TypeReference<Map<String, Object>>() {
				}, Collections.emptyMap());

		info.setUserId(userId);
		info.setTenantId(tenantId);
		info.setNickName(nickName);
		info.setAvatarUrl(avatarUrl);
		info.setRoleIdList(roleIdList);
		info.setPermissionList(permissionList);
		info.setAdditionalInfo(additionalInfo);
		return info;
	}

}
