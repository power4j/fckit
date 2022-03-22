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

package com.power4j.fist.security.core.authorization.domain;

import com.power4j.fist.boot.security.core.SecurityConstant;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/2
 * @since 1.0
 */
public class AnonymousUser implements AuthenticatedUser {

	private final static String USER_NAME = SecurityConstant.ANONYMOUS_USER;

	private final static String TENANT = "Anonymous";

	private final Map<String, GrantedPermission> permissionMap;

	private final Map<String, Object> additionalInfo = new HashMap<>();

	@Nullable
	private String id;

	public AnonymousUser(Map<String, GrantedPermission> permissionMap) {
		this.permissionMap = Collections.unmodifiableMap(permissionMap);
	}

	public AnonymousUser() {
		this(Collections.emptyMap());
	}

	@Override
	public String getUsername() {
		return USER_NAME;
	}

	@Override
	public String getTenantId() {
		return TENANT;
	}

	@Override
	public Map<String, GrantedPermission> getPermissions() {
		return permissionMap;
	}

	@Override
	public Map<String, Object> getAdditionalInfo() {
		return additionalInfo;
	}

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	@Override
	public boolean isAnonymous() {
		return true;
	}

}
