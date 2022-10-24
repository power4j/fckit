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

import cn.hutool.core.util.RandomUtil;
import com.power4j.fist.boot.common.prop.PropConstant;
import com.power4j.fist.boot.security.core.SecurityConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/2
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = SecurityProperties.PROP_PREFIX)
public class SecurityProperties {

	public static final String PROP_ENTRY = "security";

	public static final String PROP_PREFIX = PropConstant.PROP_ROOT_PREFIX + PROP_ENTRY;

	private JwtConfig jwt = new JwtConfig();

	private String tokenHeader = SecurityConstant.HEADER_JWT_INNER;

	@Data
	public static class JwtConfig {

		/**
		 * 密钥
		 */
		private String key = RandomUtil.randomString(32);

		private String issuer = "J-7616F11";

		private String audience;

		private Duration expiration = Duration.ofMinutes(5L);

	}

}
