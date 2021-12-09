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

package com.power4j.fist.cloud.gateway.auth.infra.client.feign;

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.cloud.core.constant.ServiceConstant;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
@FeignClient(name = ServiceConstant.SERVICE_NAME_IAM, contextId = "permissionClient")
public interface PermissionClient {

	/**
	 * 获取 Permission 记录
	 * @param serviceName 服务名
	 * @param method HTTP 方法
	 * @return Result
	 */
	@GetMapping("/v1/pl/api-info/service/{serviceName}")
	Result<List<PermDefinition>> loadPermission(@PathVariable(value = "serviceName") String serviceName,
			@RequestParam String method);

	/**
	 * 检查用户是否拥有某个资源的权限
	 * @param userId 用户ID
	 * @param resourceId 全局唯一资源ID
	 * @return ture 有权限 ,false 无权限
	 */
	@GetMapping("/permission-check")
	Result<Boolean> hasPermission(@RequestParam Long userId, @RequestParam String resourceId);

}
