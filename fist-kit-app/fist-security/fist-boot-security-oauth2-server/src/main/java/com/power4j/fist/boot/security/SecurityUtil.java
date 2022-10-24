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
import com.power4j.fist.boot.security.core.UserInfo;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/6
 * @since 1.0
 */
@UtilityClass
public class SecurityUtil {

	public Optional<Authentication> getAuthentication() {
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
	}

	/**
	 * 当前登录用户
	 */
	public Optional<UserInfo> getUser() {
		return getAuthentication().map(authentication -> {
			Object principal = authentication.getPrincipal();
			return (principal instanceof UserInfo) ? (UserInfo) principal : null;
		});
	}

	/**
	 * 当前登录用户的用户名
	 */
	public Optional<String> getUsername() {
		return getUser().map(UserInfo::getUsername);
	}

	/**
	 * 当前登录用户的ID
	 */
	public Optional<Long> getUserId() {
		return getUser().map(UserInfo::getUserId);
	}

	/**
	 * 当前登录用户的权限列表，用户会话不存在或者无权限返回空集合
	 */
	public Set<String> getAuthorities() {
		final Typed<List<String>> stringListType = new TypeLiteral<>() {
		};
		// @formatter:off
		return getUser()
				.flatMap(u -> u.getMetaProp(SecurityConstant.UserProp.KEY_ROLE_LIST,stringListType))
				.map(HashSet::new)
				.orElse(new HashSet<>());
		// @formatter:on
	}

}
