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

package com.power4j.fist.security.oauth2.token;

import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.core.UserInfoExtractor;
import com.power4j.fist.boot.security.oauth2.Oauth2UserInfoExtractor;
import com.power4j.fist.security.core.authentication.UserConverter;
import com.power4j.fist.security.core.authorization.domain.AnonymousUser;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.GrantedPermission;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/1
 * @since 1.0
 */
public class DefaultUserConverter implements UserConverter<AuthenticatedUser> {

	@Setter
	@Getter
	private UserInfoExtractor userInfoExtractor = new Oauth2UserInfoExtractor();

	@Override
	public AuthenticatedUser convert(Authentication authentication) {
		if (authentication instanceof BearerTokenAuthentication) {
			BearerTokenAuthentication auth = (BearerTokenAuthentication) authentication;
			return extractFromAttribute(auth.getTokenAttributes()).orElse(null);
		}
		return new AnonymousUser();
	}

	protected Optional<AuthenticatedUser> extractFromAttribute(Map<String, Object> prop) {
		return userInfoExtractor.extractAuthUser(prop).map(AuthenticatedUserAdapter::new);
	}

	public static class AuthenticatedUserAdapter implements AuthenticatedUser {

		private final UserInfo userInfo;

		AuthenticatedUserAdapter(UserInfo userInfo) {
			this.userInfo = userInfo;
		}

		@Override
		public String getUsername() {
			return userInfo.getUsername();
		}

		@Override
		public String getTenantId() {
			return userInfo.getMetaProp(SecurityConstant.UserProp.KEY_TENANT_ID, String.class).orElse(StringPool.N_A);
		}

		@Override
		public Map<String, GrantedPermission> getPermissions() {
			Typed<Collection<String>> type = new TypeLiteral<>() {
			};
			// @formatter:off
			return userInfo.getMetaProp(SecurityConstant.UserProp.KEY_PERMISSION_LIST,type)
					.orElse(Collections.emptyList())
					.stream()
					.distinct()
					.collect(Collectors.toMap(Function.identity(), SimpleGrantedPermission::new));
			// @formatter:on
		}

		@Override
		public Map<String, Object> getAdditionalInfo() {
			return Objects.requireNonNull(userInfo.getMeta());
		}

	}

	public static class SimpleGrantedPermission implements GrantedPermission {

		private final String code;

		public SimpleGrantedPermission(String code) {
			this.code = code;
		}

		@Override
		public String getPermissionCode() {
			return code;
		}

	}

}
