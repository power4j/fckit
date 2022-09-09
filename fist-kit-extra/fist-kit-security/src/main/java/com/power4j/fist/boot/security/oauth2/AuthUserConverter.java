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

import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.boot.security.core.DefaultUserInfoExtractor;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfoExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/17
 * @since 1.0
 */
@Slf4j
public class AuthUserConverter extends DefaultUserAuthenticationConverter {

	private final UserInfoExtractor authUserExtractor = new DefaultUserInfoExtractor();

	@Override
	public Map<String, ?> convertUserAuthentication(Authentication userAuthentication) {
		return super.convertUserAuthentication(userAuthentication);
	}

	@Nullable
	@Override
	public Authentication extractAuthentication(Map<String, ?> map) {
		Typed<List<String>> type = new TypeLiteral<>() {
		};
		return authUserExtractor.extractAuthUser(map).map(u -> {
			List<String> roles = u.getMetaProp(SecurityConstant.UserProp.KEY_ROLE_LIST, type).orElse(new ArrayList<>());
			return new UsernamePasswordAuthenticationToken(u, StringPool.N_A, getAuthorities(roles));
		}).orElse(null);
	}

	protected List<GrantedAuthority> getAuthorities(Collection<String> values) {
		return values.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}

}
