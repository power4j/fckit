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

import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;
import com.power4j.fist.cloud.gateway.auth.entity.RouteInfo;
import com.power4j.fist.cloud.gateway.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionInfoFilter extends AbstractAuthFilter {

	private final PermissionService permissionService;

	@Override
	protected boolean process(AuthContext context) {
		final URI handlerUri = context.getHandlerUri();
		final RouteInfo routeInfo = context.getRouteInfo();
		if (Objects.nonNull(handlerUri) && Objects.nonNull(routeInfo)) {
			final String serviceName = routeInfo.getServiceId();
			final String method = context.getRequestInfo().getMethod().name();
			final String path = handlerUri.getPath();
			PermDefinition permDefinition = permissionService.getPermission(serviceName, method, path).orElse(null);
			if (log.isDebugEnabled()) {
				if (Objects.isNull(permDefinition)) {
					log.debug("无匹配的权限规则(服务ID:{}) :{} {}", serviceName, method, path);
				}
				else {
					log.debug("匹配到权限规则(服务ID:{} 方法:{}), {} -> {}", serviceName, method, permDefinition.getPath(), path);
				}
			}
			context.setPermDefinition(permDefinition);
		}
		return true;
	}

}
