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

package com.power4j.fist.cloud.gateway.auth.service;

import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
public interface PermissionService {

	/**
	 * 获取 Permission 记录
	 * @param serviceName 服务名
	 * @param method HTTP 方法
	 * @param path 请求路径
	 * @return Result
	 */
	Optional<PermDefinition> getPermission(String serviceName, String method, String path);

	/**
	 * 检查用户是否拥有某个资源的权限
	 * @param userId 用户ID
	 * @param resourceId 全局唯一资源ID
	 * @return ture 有权限 ,false 无权限
	 */
	boolean hasPermission(Long userId, String resourceId);

	/**
	 * 共享 PathMatcher
	 * @return PathMatcher
	 */
	PathMatcher getPathMatcher();

}
