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

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.support.SqlBinaryValue;
import org.springframework.jdbc.core.support.SqlCharacterValue;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Types;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DefaultAuthorizationServiceTest {

	private static final String AUTHORIZATION_SCHEMA = "org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql";

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

	@Test
	void shouldMapLobParametersWithSpringJdbcSqlTypeValue() throws Exception {
		Map<String, Integer> types = new HashMap<>();
		types.put("access_token_value", Types.BLOB);
		types.put("access_token_metadata", Types.CLOB);
		installColumnMetadata(types);

		SqlParameterValue blob = mapToSqlParameter("access_token_value", "token");
		SqlParameterValue clob = mapToSqlParameter("access_token_metadata", "{}");

		Assertions.assertEquals(Types.BLOB, blob.getSqlType());
		Assertions.assertInstanceOf(SqlBinaryValue.class, blob.getValue());
		Assertions.assertEquals(Types.CLOB, clob.getSqlType());
		Assertions.assertInstanceOf(SqlCharacterValue.class, clob.getValue());
	}

	@Test
	void shouldPersistBlobLobColumnsWithH2AuthorizationSchema() throws Exception {
		RegisteredClient registeredClient = registeredClient();
		DefaultAuthorizationService service = new DefaultAuthorizationService(
				createJdbcTemplate("oauth2_lob_blob", false), registeredClientRepository(registeredClient));
		OAuth2Authorization authorization = authorization(registeredClient);

		service.save(authorization);

		OAuth2Authorization loadedById = service.findById(authorization.getId());
		Assertions.assertNotNull(loadedById);
		Assertions.assertEquals("alice", loadedById.getPrincipalName());
		Assertions.assertEquals(Set.of("read"), loadedById.getAuthorizedScopes());
		Assertions.assertEquals("access-token", loadedById.getAccessToken().getToken().getTokenValue());
		OAuth2Authorization loadedByToken = service.findByToken("access-token", OAuth2TokenType.ACCESS_TOKEN);
		Assertions.assertNotNull(loadedByToken);
		Assertions.assertEquals(authorization.getId(), loadedByToken.getId());
	}

	@Test
	void shouldPersistClobLobColumnsWithH2SchemaVariant() throws Exception {
		RegisteredClient registeredClient = registeredClient();
		DefaultAuthorizationService service = new DefaultAuthorizationService(
				createJdbcTemplate("oauth2_lob_clob", true), registeredClientRepository(registeredClient));
		OAuth2Authorization authorization = authorization(registeredClient);

		service.save(authorization);

		OAuth2Authorization loadedById = service.findById(authorization.getId());
		Assertions.assertNotNull(loadedById);
		Assertions.assertEquals("access-token", loadedById.getAccessToken().getToken().getTokenValue());
		Assertions.assertEquals(Set.of("read"), loadedById.getAuthorizedScopes());
	}

	private static void installColumnMetadata() throws Exception {
		installColumnMetadata(Collections.emptyMap());
	}

	private static void installColumnMetadata(Map<String, Integer> types) throws Exception {
		Map<String, DefaultAuthorizationService.ColumnMetadata> metadata = new HashMap<>();
		for (String columnName : Set.of("attributes", "authorization_code_value", "authorization_code_metadata",
				"access_token_value", "access_token_metadata", "oidc_id_token_value", "oidc_id_token_metadata",
				"refresh_token_value", "refresh_token_metadata", "user_code_value", "user_code_metadata",
				"device_code_value", "device_code_metadata")) {
			metadata.put(columnName, columnMetadata(columnName, types.getOrDefault(columnName, Types.VARCHAR)));
		}
		ReflectionTestUtils.setField(DefaultAuthorizationService.class, "columnMetadataMap", metadata);
	}

	private static DefaultAuthorizationService.ColumnMetadata columnMetadata(String columnName, int type)
			throws Exception {
		Constructor<DefaultAuthorizationService.ColumnMetadata> constructor = DefaultAuthorizationService.ColumnMetadata.class
			.getDeclaredConstructor(String.class, int.class);
		constructor.setAccessible(true);
		return constructor.newInstance(columnName, type);
	}

	private static SqlParameterValue mapToSqlParameter(String columnName, String value) throws Exception {
		Method method = DefaultAuthorizationService.class.getDeclaredMethod("mapToSqlParameter", String.class,
				String.class);
		method.setAccessible(true);
		return (SqlParameterValue) method.invoke(null, columnName, value);
	}

	private static JdbcTemplate createJdbcTemplate(String databaseName, boolean clobSchema) throws Exception {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:" + databaseName + "_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		try (Connection connection = dataSource.getConnection()) {
			String schema = StreamUtils.copyToString(new ClassPathResource(AUTHORIZATION_SCHEMA).getInputStream(),
					StandardCharsets.UTF_8);
			if (clobSchema) {
				schema = schema.replace(" blob DEFAULT NULL", " clob DEFAULT NULL");
			}
			ScriptUtils.executeSqlScript(connection,
					new EncodedResource(new ByteArrayResource(schema.getBytes(StandardCharsets.UTF_8))));
		}
		return new JdbcTemplate(dataSource);
	}

	private static RegisteredClient registeredClient() {
		return RegisteredClient.withId("client-id")
			.clientId("client")
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.scope("read")
			.build();
	}

	private static RegisteredClientRepository registeredClientRepository(RegisteredClient registeredClient) {
		return new RegisteredClientRepository() {
			@Override
			public void save(RegisteredClient registeredClient) {
			}

			@Override
			public RegisteredClient findById(String id) {
				return registeredClient.getId().equals(id) ? registeredClient : null;
			}

			@Override
			public RegisteredClient findByClientId(String clientId) {
				return registeredClient.getClientId().equals(clientId) ? registeredClient : null;
			}
		};
	}

	private static OAuth2Authorization authorization(RegisteredClient registeredClient) {
		Instant issuedAt = Instant.parse("2026-05-12T00:00:00Z");
		return OAuth2Authorization.withRegisteredClient(registeredClient)
			.id("authorization-id")
			.principalName("alice")
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.authorizedScopes(Set.of("read"))
			.accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", issuedAt,
					issuedAt.plusSeconds(60), Set.of("read")))
			.build();
	}

}
