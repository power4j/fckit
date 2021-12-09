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

package com.power4j.fist.cloud.gateway.auth.configure;

import com.power4j.fist.boot.common.prop.PropConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/5
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = FistGatewayProperties.PROP_PREFIX)
public class FistGatewayProperties {

	public static final String PROP_ENTRY = "gateway";

	public static final String PROP_PREFIX = PropConstant.PROP_ROOT_PREFIX + PROP_ENTRY;

	/**
	 * Header 清洗
	 */
	private List<String> headerClean = Collections.emptyList();

	/**
	 * 鉴权配置
	 */
	private PermissionConfig permission = new PermissionConfig();

	@Data
	public static class PermissionConfig {

		private boolean enabled = true;

		private String jwtKey = UUID.randomUUID().toString();

		private String apiTokenName = "x-api-token";

		private List<String> skipPaths = Collections.emptyList();

		private Duration metaCacheTtl = Duration.ofSeconds(60);

		private int metaCacheSize = 0;

		/**
		 * OAuth2 客户端
		 */
		private Oauth2ClientConfig oauthClient;

	}

	@Data
	public static class Oauth2ClientConfig {

		private String clientId;

		private String clientSecret;

		private String tokenInfoUri = "http://fist-auth/oauth/check_token";

	}

}
