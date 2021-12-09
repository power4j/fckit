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

import com.power4j.fist.cloud.core.constant.ServiceConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/12
 * @since 1.0
 */
@FeignClient(name = ServiceConstant.SERVICE_NAME_AUTH, contextId = "authClient")
public interface AuthClient {

	/**
	 * 获取认证信息
	 * @param token access token
	 * @return Map
	 */
	@RequestMapping(method = RequestMethod.POST, path = "/oauth/check_token")
	Map<String, Object> checkToken(@RequestParam("token") String token);

}
