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

package com.power4j.fist.cloud.gateway.authorization.filter.simple.impl;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.AbstractAuthFilter;
import com.power4j.fist.security.core.authorization.config.GlobalAuthorizationProperties;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Slf4j
public class UserPermissionFilter extends AbstractAuthFilter {

	private final GlobalAuthorizationProperties authorizationProperties;

	public UserPermissionFilter(GlobalAuthorizationProperties authorizationProperties) {
		this.authorizationProperties = authorizationProperties;
	}

	@Override
	protected boolean process(AuthContext ctx) {
		if (Objects.isNull(ctx.getPermissionDefinition())) {
			return true;
		}
		final AuthenticatedUser userInfo = ctx.getUserInfo();
		if (Objects.isNull(userInfo)) {
			return exitChain(ctx, AuthProblem.PERMISSION_CHECK_DENIED.moreInfo("No user info"));
		}
		return validatePermission(ctx);
	}

	protected boolean validatePermission(AuthContext ctx) {
		AuthenticatedUser userInfo = Objects.requireNonNull(ctx.getUserInfo());
		PermissionDefinition permissionDefinition = Objects.requireNonNull(ctx.getPermissionDefinition());

		final String target = permissionDefinition.getCode();
		if (!userInfo.getPermissions().containsKey(target)) {
			if (log.isDebugEnabled()) {
				log.debug("Permission({}) is not owned by user ({}). action = {}", target, userInfo.getUsername(),
						ctx.getInbound().shortDescription());
			}
			return exitChain(ctx, AuthProblem.PERMISSION_CHECK_DENIED.moreInfo(target));
		}
		return exitChain(ctx, AuthProblem.PERMISSION_CHECK_PASS.moreInfo(target));
	}

}
