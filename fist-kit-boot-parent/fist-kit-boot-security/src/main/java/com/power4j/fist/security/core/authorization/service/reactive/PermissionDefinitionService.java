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

package com.power4j.fist.security.core.authorization.service.reactive;

import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
public interface PermissionDefinitionService<T extends PermissionDefinition> {

	/**
	 * 获取权限信息
	 * @param serviceName 服务名
	 * @param method 接口方法
	 * @return 无权限信息返回空集合
	 */
	List<T> getPermissionDefinition(String serviceName, HttpMethod method);

}
