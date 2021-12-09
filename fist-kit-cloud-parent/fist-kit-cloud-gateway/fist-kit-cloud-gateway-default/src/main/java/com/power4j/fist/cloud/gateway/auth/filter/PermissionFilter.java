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

import cn.hutool.core.text.CharSequenceUtil;
import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import com.power4j.fist.cloud.gateway.auth.entity.AuthProblem;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;
import com.power4j.fist.cloud.gateway.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.util.pattern.PathPattern;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * API 鉴权
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionFilter extends AbstractAuthFilter {

	private final static List<String> TENANT_ID_NAMES = Arrays.asList(SecurityConstant.TENANT_ID_PARAMETER_KEYS);

	private final PermissionService permissionService;

	private final PathMatcher pathMatcher;

	@Override
	protected boolean process(AuthContext context) {
		final UserInfo authUser = context.getAuthUser();
		final PermDefinition permDefinition = context.getPermDefinition();
		if (Objects.isNull(authUser) || Objects.isNull(permDefinition)) {
			if (log.isDebugEnabled()) {
				log.debug("No auth user or permission info,Access denied");
			}
			context.getResponseInfo().setProblem(AuthProblem.PERMISSION_CHECK_DENIED);
			return false;
		}

		// 用户是否由API使用权限
		boolean permitted = permissionService.hasPermission(authUser.getUserId(), permDefinition.getResourceName());
		if (!permitted) {
			if (log.isDebugEnabled()) {
				log.debug("Access denied,User id = {},username={},url = {},method = {} ", authUser.getUserId(),
						authUser.getUsername(), context.getRequestInfo().getUri(),
						context.getRequestInfo().getMethod());
			}
			context.getResponseInfo().setProblem(AuthProblem.PERMISSION_CHECK_DENIED);
			return false;
		}
		// 租户值是否在允许是范围内
		return checkTenant(context);
	}

	protected boolean checkTenant(AuthContext context) {
		PermDefinition permDefinition = Objects.requireNonNull(context.getPermDefinition());
		if (!PermDefinition.API_LEVEL_TENANT.equals(permDefinition.getLevel())) {
			return true;
		}
		String backendPath = Optional.ofNullable(context.getHandlerUri()).map(URI::getPath).orElse(null);
		String matchPattern = permDefinition.getPath();
		String tenantId = resolveTenantId(backendPath, matchPattern);
		if (Objects.isNull(tenantId)) {
			tenantId = resolveTenantId(context.getRequestInfo().getHeaders());
		}

		// 租户级接口必须校验租户ID
		// 有多种方式在请求中携带租户值
		if (Objects.isNull(tenantId)) {
			if (log.isDebugEnabled()) {
				log.debug("Access denied,No tenant ID,request = {},definition = {} ", backendPath, matchPattern);
			}
			context.getResponseInfo().setProblem(AuthProblem.TENANT_ID_REQUIRED);
			return false;
		}
		// TODO : 有效的租户ID来自用户角色(用户可以加入多个租户)
		return true;
	}

	@Nullable
	private String resolveTenantId(@Nullable String path, String pathPattern) {
		if (Objects.nonNull(path)) {
			PathPattern.PathMatchInfo matchInfo = pathMatcher.match(pathPattern, path).orElse(null);
			if (Objects.nonNull(matchInfo)) {
				for (String name : TENANT_ID_NAMES) {
					String val = matchInfo.getUriVariables().get(name);
					if (CharSequenceUtil.isNotEmpty(val)) {
						return val;
					}
				}
			}
		}
		return null;
	}

	@Nullable
	private String resolveTenantId(@Nullable HttpHeaders headers) {
		if (Objects.isNull(headers)) {
			return null;
		}
		for (String name : TENANT_ID_NAMES) {
			String val = headers.getFirst(name);
			if (CharSequenceUtil.isNotEmpty(val)) {
				return val;
			}
		}
		return null;
	}

}
