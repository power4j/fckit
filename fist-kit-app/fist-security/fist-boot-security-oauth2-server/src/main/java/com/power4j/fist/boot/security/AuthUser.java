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

package com.power4j.fist.boot.security;

import com.power4j.fist.boot.security.core.SecurityConstant;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 已认证用户
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/16
 * @since 1.0
 */
@Getter
@Setter
public class AuthUser extends User {

	private Long userId;

	@Nullable
	private String nickName;

	@Nullable
	private String avatarUrl;

	/**
	 * @see com.power4j.fist.boot.security.core.SecurityConstant.UserProp
	 */
	private Map<String, Object> additionInfo = Collections.emptyMap();

	public AuthUser(String username, String password, Collection<? extends GrantedAuthority> authorities) {
		super(username, password, authorities);
	}

	public AuthUser(String username, String password, boolean enabled, boolean accountNonExpired,
			boolean credentialsNonExpired, boolean accountNonLocked,
			Collection<? extends GrantedAuthority> authorities) {
		super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
	}

	public Map<String, Object> expose() {
		Map<String, Object> props = new HashMap<>(8);
		props.put(SecurityConstant.UserProp.KEY_USERNAME, getUsername());
		props.put(SecurityConstant.UserProp.KEY_USER_ID, userId);
		props.put(SecurityConstant.UserProp.KEY_NICK_NAME, nickName);
		props.put(SecurityConstant.UserProp.KEY_AVATAR_URL, avatarUrl);
		if (ObjectUtils.isNotEmpty(additionInfo)) {
			props.putAll(additionInfo);
		}
		return props;
	}

}
