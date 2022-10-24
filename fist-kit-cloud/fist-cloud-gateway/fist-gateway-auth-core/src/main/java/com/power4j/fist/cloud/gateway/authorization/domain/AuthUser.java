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

package com.power4j.fist.cloud.gateway.authorization.domain;

import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.GrantedPermission;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/28
 * @since 1.0
 */
@Data
@Builder
public class AuthUser implements AuthenticatedUser {

	public static final String INFO_KEY_ORG = "org";

	private Long userId;

	private String username;

	private String tenantId;

	private Map<String, GrantedPermission> permissions;

	@Builder.Default
	private Map<String, Object> additionalInfo = Collections.emptyMap();

}
