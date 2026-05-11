/*
 * Copyright 2026. ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.oauth2.extension.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DefaultAuthorizationServiceTest {

	@Test
	void shouldSerializeAuthorizationWithSpringSecurityPrincipal() throws Exception {
		installColumnMetadata();
		RegisteredClient registeredClient = RegisteredClient.withId("client-id")
			.clientId("client")
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.scope("read")
			.build();
		OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
			.id("authorization-id")
			.principalName("alice")
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.authorizedScopes(Set.of("read"))
			.attribute("principal",
					new UsernamePasswordAuthenticationToken("alice", "N/A",
							AuthorityUtils.createAuthorityList("ROLE_USER")))
			.accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(),
					Instant.now().plusSeconds(60), Set.of("read")))
			.build();

		DefaultAuthorizationService.OAuth2AuthorizationParametersMapper mapper = new DefaultAuthorizationService.OAuth2AuthorizationParametersMapper();

		List<SqlParameterValue> parameters = mapper.apply(authorization);

		Assertions.assertEquals(33, parameters.size());
		Assertions.assertTrue(String.valueOf(parameters.get(5).getValue()).contains("ROLE_USER"));
	}

	private static void installColumnMetadata() throws Exception {
		Map<String, DefaultAuthorizationService.ColumnMetadata> metadata = new HashMap<>();
		for (String columnName : Set.of("attributes", "authorization_code_value", "authorization_code_metadata",
				"access_token_value", "access_token_metadata", "oidc_id_token_value", "oidc_id_token_metadata",
				"refresh_token_value", "refresh_token_metadata", "user_code_value", "user_code_metadata",
				"device_code_value", "device_code_metadata")) {
			metadata.put(columnName, columnMetadata(columnName));
		}
		ReflectionTestUtils.setField(DefaultAuthorizationService.class, "columnMetadataMap", metadata);
	}

	private static DefaultAuthorizationService.ColumnMetadata columnMetadata(String columnName) throws Exception {
		Constructor<DefaultAuthorizationService.ColumnMetadata> constructor = DefaultAuthorizationService.ColumnMetadata.class
			.getDeclaredConstructor(String.class, int.class);
		constructor.setAccessible(true);
		return constructor.newInstance(columnName, Types.CLOB);
	}

}
