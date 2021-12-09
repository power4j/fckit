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

import lombok.Data;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * POJO of AuthUser so we don't have to import Spring Security
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/13
 * @since 1.0
 */
@Data
public class UserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String username;

	private Set<String> authorities;

	private boolean accountNonExpired;

	private boolean accountNonLocked;

	private boolean credentialsNonExpired;

	private boolean enabled;

	private Long userId;

	private String tenantId;

	@Nullable
	private String nickName;

	@Nullable
	private String avatarUrl;

	@Nullable
	private String clientId;

	private List<Long> roleIdList = Collections.emptyList();

	private List<String> permissionList = Collections.emptyList();

	private Map<String, Object> additionalInfo = Collections.emptyMap();

}
