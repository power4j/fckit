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
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import lombok.extern.slf4j.Slf4j;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Slf4j
public class LoginAccessFilter extends AbstractAuthFilter {

	@Override
	protected boolean process(AuthContext ctx) {
		final PermissionDefinition permissionDefinition = ctx.getPermissionDefinition();
		final AuthenticatedUser user = ctx.getUserInfo();

		if (permissionDefinition != null && permissionDefinition.isLoginAccess()) {
			if (null != user && !user.isAnonymous()) {
				if (log.isTraceEnabled()) {
					log.trace("Login access rule matches. => {}", ctx.getInbound().shortDescription());
				}
				return exitChain(ctx, AuthProblem.LOGIN_ACCESS);
			}
			else {
				if (log.isDebugEnabled()) {
					log.debug("Login access required,but not user info. => {}", ctx.getInbound().shortDescription());
				}
				return exitChain(ctx, AuthProblem.USER_ACCESS_DENIED);
			}
		}
		return true;
	}

}
