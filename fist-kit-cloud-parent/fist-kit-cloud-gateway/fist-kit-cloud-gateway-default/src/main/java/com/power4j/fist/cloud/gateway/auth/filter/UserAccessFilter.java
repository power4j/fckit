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

package com.power4j.fist.cloud.gateway.auth.filter;

import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import com.power4j.fist.cloud.gateway.auth.entity.AuthProblem;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/24
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class UserAccessFilter extends AbstractAuthFilter {

	@Override
	protected boolean process(AuthContext context) {
		final UserInfo authUser = context.getAuthUser();
		final PermDefinition permDefinition = context.getPermDefinition();
		if (Objects.isNull(authUser) || Objects.isNull(permDefinition)) {
			log.warn("No user/permission info,Access denied");
			context.getResponseInfo().setProblem(AuthProblem.USER_ACCESS_DENIED);
			return false;
		}
		if (StringPool.ONE.equals(context.getPermDefinition().getUserAccessFlag())) {
			if (log.isDebugEnabled()) {
				log.debug("User access api [{} {}] {},No user,Access denied", context.getPermDefinition().getMethod(),
						context.getPermDefinition().getServiceName(), context.getPermDefinition().getPath());
			}
			context.getResponseInfo().setProblem(AuthProblem.USER_ACCESS_DENIED);
			return false;
		}
		return true;
	}

}
