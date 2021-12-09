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

package com.power4j.fist.cloud.gateway.auth.infra.service;

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/16
 * @since 1.0
 */
public interface RemotePermissionService {

	/**
	 * 获取 Permission 记录
	 * @param serviceName 服务名
	 * @param method HTTP 方法
	 * @return Result
	 */
	Result<List<PermDefinition>> loadPermission(String serviceName, @Nullable String method);

}
