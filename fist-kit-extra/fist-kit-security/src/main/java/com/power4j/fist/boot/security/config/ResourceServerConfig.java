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

package com.power4j.fist.boot.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/9
 * @since 1.0
 */
public interface ResourceServerConfig {

	/**
	 * 调整 {@code ResourceServerSecurityConfig}，比如自定义 TokenStore
	 * @param config ResourceServerConfiguration 对象
	 * @throws Exception if there is a problem
	 * @see ResourceServerSecurityConfig
	 */
	void configure(ResourceServerSecurityConfig config) throws Exception;

	/**
	 * 调整 {@code HttpSecurity}
	 * @param http the current http filter configuration
	 * @throws Exception if there is a problem
	 */
	void configure(HttpSecurity http) throws Exception;

}
