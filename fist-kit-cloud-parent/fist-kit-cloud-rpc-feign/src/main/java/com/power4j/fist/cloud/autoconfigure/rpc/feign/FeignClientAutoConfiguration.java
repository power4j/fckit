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

package com.power4j.fist.cloud.autoconfigure.rpc.feign;

import com.power4j.fist.cloud.rpc.feign.UserRelayInterceptor;
import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackages = "com.power4j.fist")
@ComponentScan(basePackages = { "com.power4j.fist.cloud.autoconfigure.rpc.feign.error" })
public class FeignClientAutoConfiguration {

	// TODO: Add Interceptor for Jwt, tenant-id, request-id

	@Bean
	public RequestInterceptor userRelayInterceptor() {
		return new UserRelayInterceptor();
	}

	// TODO: 统一降级处理

}
