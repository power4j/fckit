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

package com.power4j.fist.cloud.gateway.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.power4j.coca.kit.common.lang.Result;
import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.boot.common.error.ErrorCode;
import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;
import com.power4j.fist.cloud.gateway.auth.infra.client.feign.PermissionClient;
import com.power4j.fist.cloud.gateway.auth.infra.service.RemotePermissionService;
import com.power4j.fist.boot.common.matcher.FastPathMatcher;
import com.power4j.fist.cloud.gateway.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

	private final FastPathMatcher pathMatcher;

	private final PermissionClient permissionClient;

	private final RemotePermissionService remotePermissionService;

	@Override
	public Optional<PermDefinition> getPermission(String serviceName, String method, String path) {
		// TODO: 性能瓶颈，需优化
		Result<List<PermDefinition>> result = remotePermissionService.loadPermission(serviceName, method);
		if (result.codeNotEquals(ErrorCode.OK)) {
			log.error("获取权限信息失败:{}", result.simpleDescribe());
			return Optional.empty();
		}
		List<PermDefinition> permDefinitionList = result.getData();
		Assert.notNull(permDefinitionList, "服务端返回成功，但是数据为null");

		List<PermDefinition> hits = pathMatcher.patternFilter(permDefinitionList, path, PermDefinition::getPath);
		if (hits.size() > 1) {
			List<String> patterns = hits.stream().map(PermDefinition::getPath).collect(Collectors.toList());
			log.warn("[{}] {}#{} 匹配到多条规则 {}", serviceName, method, path,
					CharSequenceUtil.join(StringPool.COMMA, patterns));
		}
		return hits.stream().findFirst();
	}

	@Override
	public boolean hasPermission(Long userId, String resourceId) {
		// TODO: 性能瓶颈，需优化
		Result<Boolean> result = permissionClient.hasPermission(userId, resourceId);
		if (result.codeNotEquals(ErrorCode.OK)) {
			log.error("检查用户权限失败:{}", result.simpleDescribe());
			return false;
		}
		return Boolean.TRUE.equals(result.getData());
	}

	@Override
	public PathMatcher getPathMatcher() {
		return pathMatcher;
	}

}
