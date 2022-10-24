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

package com.power4j.fist.security.core.authorization.domain;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
public interface PermissionDefinition {

	/**
	 * 权限编码
	 * @return 受保护接口的标识符
	 */
	String getCode();

	/**
	 * 受保护接口所属服务名
	 * @return 返回服务名
	 */
	String getServiceName();

	/**
	 * 受保护接口的HTTP方法
	 * @return 返回 HttpMethod
	 */
	HttpMethod getMethod();

	/**
	 * 受保护接口的路径
	 * @return 返回url的路径
	 */
	String getPath();

	/**
	 * 接口级别
	 * @return 返回接口级别代码
	 */
	String getLevel();

	/**
	 * 公开访问
	 * @return true表示公开访问(无需鉴权)
	 */
	boolean isPublicAccess();

	/**
	 * 登录可访问
	 * @return true表示已登录的用户即可访问
	 */
	boolean isLoginAccess();

	/**
	 * 内部访问
	 * @return true表示只能内部访问
	 */
	boolean isInternalAccess();

	/**
	 * 签名校验算法标识符
	 * @return 返回null表示无需签名校验
	 */
	@Nullable
	String getSignFlag();

}
