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

package com.power4j.fist.cloud.core.constant;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
public enum FistCoreServices {

	/**
	 * 网关
	 */
	FGATE(ServiceConstant.SERVICE_NAME_GATEWAY, 18080, "网关"),
	/**
	 * 平台管理
	 */
	FPLAM(ServiceConstant.SERVICE_NAME_PLATFORM, 28310, "平台管理"),
	/**
	 * oauth2认证服务
	 */
	FAUTH(ServiceConstant.SERVICE_NAME_AUTH, 28030, "oauth2认证服务"),
	/**
	 * 身份管理中心
	 */
	FIAMC(ServiceConstant.SERVICE_NAME_IAM, 28090, "身份管理中心"),
	/**
	 * 开发文档
	 */
	FSWAG(ServiceConstant.SERVICE_NAME_SWAGGER, 28110, "开发文档");

	private final String serviceName;

	private final int port;

	private final String description;

	FistCoreServices(String serviceName, Integer port, String description) {
		this.serviceName = serviceName;
		this.port = port;
		this.description = description;
	}

	public String getServiceName() {
		return serviceName;
	}

	public int getPort() {
		return port;
	}

	public String getDescription() {
		return description;
	}

}
