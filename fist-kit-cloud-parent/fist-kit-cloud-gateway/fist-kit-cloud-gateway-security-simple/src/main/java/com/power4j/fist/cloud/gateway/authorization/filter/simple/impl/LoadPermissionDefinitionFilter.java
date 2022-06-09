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

import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.authorization.domain.ApiProxy;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.AbstractAuthFilter;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.service.PermissionDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class LoadPermissionDefinitionFilter extends AbstractAuthFilter {

	private final PathMatcher pathMatcher;

	private final PermissionDefinitionService<? extends PermissionDefinition> permissionDefinitionService;

	@Override
	protected boolean process(AuthContext ctx) {
		final ApiProxy upstream = ctx.getUpstream();
		if (null == upstream) {
			if (log.isDebugEnabled()) {
				log.debug("No upstream for this request,block access by default. => {}",
						ctx.getInbound().shortDescription());
			}
			return exitChain(ctx,
					AuthProblem.AUTH_EXCEPTION.moreInfo("No upstream for:" + ctx.getInbound().shortDescription()));
		}
		final String api = upstream.getPath();
		List<? extends PermissionDefinition> definitions = permissionDefinitionService
				.getPermissionDefinition(upstream.getServiceName(), upstream.getMethod());
		pathMatcher.bestMatch(definitions, api, PermissionDefinition::getPath).ifPresent(ctx::setPermissionDefinition);

		if (log.isDebugEnabled()) {
			if (null == ctx.getPermissionDefinition()) {
				log.debug("No permission definition for {}", api);
			}
		}
		return true;
	}

}
