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

import cn.hutool.core.lang.TypeReference;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.cloud.gateway.authorization.AuthUtils;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthUser;
import com.power4j.fist.cloud.gateway.authorization.domain.ResourceLevel;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.AbstractAuthFilter;
import com.power4j.fist.security.core.authorization.config.GlobalAuthorizationProperties;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
		final PermissionDefinition permissionDefinition = ctx.getPermissionDefinition();
		if (Objects.isNull(permissionDefinition)) {
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

		final String tenantId = resolveTenantId(ctx.getExchange().getRequest()).orElse(null);
		if (!validateTenant(tenantId, ctx)) {
			return exitChain(ctx, AuthProblem.TENANT_CHECK_DENIED);
		}
		if (!userInfo.getPermissions().containsKey(permissionDefinition.getCode())) {
			if (log.isDebugEnabled()) {
				log.debug("Permission({}) is not owned by user ({}). action = {}", permissionDefinition.getCode(),
						userInfo.getUsername(), ctx.getInbound().shortDescription());
			}
			return exitChain(ctx, AuthProblem.PERMISSION_CHECK_DENIED);
		}
		return true;
	}

	private boolean validateTenant(@Nullable String tenantId, AuthContext ctx) {
		AuthenticatedUser userInfo = Objects.requireNonNull(ctx.getUserInfo());
		assert ctx.getPermissionDefinition() != null;
		if (ResourceLevel.PL.getValue().equals(ctx.getPermissionDefinition().getLevel())) {
			// remove header only
			ctx.setExchange(AuthUtils.eraseHeader(ctx.getExchange(), SecurityConstant.HEADER_USER_TOKEN_INNER));
			return true;
		}
		if (Objects.isNull(tenantId)) {
			if (log.isDebugEnabled()) {
				log.debug("No tenant id found on this request. {}", ctx.getInbound().shortDescription());
			}
			ctx.updateState(AuthProblem.TENANT_ID_REQUIRED);
			return false;
		}
		List<String> orgList = userInfo.getInfo(AuthUser.INFO_KEY_ORG, Collections.emptyList(),
				new TypeReference<List<String>>() {
				});
		if (!orgList.contains(tenantId)) {
			if (log.isDebugEnabled()) {
				log.debug("User({}) is not a member of tenant ({}). {}", userInfo.getUsername(), tenantId,
						ctx.getInbound().shortDescription());
			}
			ctx.updateState(AuthProblem.TENANT_CHECK_DENIED);
			return false;
		}
		return true;
	}

	// ~ Util
	// ===================================================================================================

	protected Optional<String> resolveTenantId(ServerHttpRequest request) {
		String value = request.getQueryParams().getFirst(authorizationProperties.getAuth().getTenantParameter());
		if (ObjectUtils.isEmpty(value)) {
			value = request.getHeaders().getFirst(authorizationProperties.getAuth().getTenantHeader());
		}
		return Optional.ofNullable(value).filter(o -> !o.isEmpty());
	}

}
