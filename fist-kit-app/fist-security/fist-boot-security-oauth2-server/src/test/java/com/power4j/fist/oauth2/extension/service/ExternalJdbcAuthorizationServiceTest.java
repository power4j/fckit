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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 真实 JDBC 驱动兼容性验证。默认跳过，避免本地测试依赖外部数据库。
 */
@EnabledIfEnvironmentVariable(named = "FIST_IT_JDBC_URL", matches = ".+")
class ExternalJdbcAuthorizationServiceTest {

	private static final String AUTHORIZATION_SCHEMA = "org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql";

	private static final String TABLE_NAME = "oauth2_authorization";

	private String authorizationId;

	@AfterEach
	void cleanAuthorization() {
		if (this.authorizationId == null) {
			return;
		}
		JdbcTemplate jdbcTemplate = createJdbcTemplate();
		jdbcTemplate.update("delete from " + TABLE_NAME + " where id = ?", this.authorizationId);
	}

	@Test
	void shouldPersistAuthorizationLobWithExternalJdbcDriver() throws Exception {
		JdbcTemplate jdbcTemplate = createJdbcTemplate();
		prepareSchema(jdbcTemplate);
		RegisteredClient registeredClient = registeredClient();
		DefaultAuthorizationService service = new TestAuthorizationService(jdbcTemplate,
				registeredClientRepository(registeredClient));
		OAuth2Authorization authorization = authorization(registeredClient);
		this.authorizationId = authorization.getId();

		service.save(authorization);

		OAuth2Authorization loadedById = service.findById(authorization.getId());
		Assertions.assertNotNull(loadedById);
		Assertions.assertEquals("alice", loadedById.getPrincipalName());
		Assertions.assertEquals("access-token-" + authorization.getId(),
				loadedById.getAccessToken().getToken().getTokenValue());
		OAuth2Authorization loadedByToken = service.findByToken(loadedById.getAccessToken().getToken().getTokenValue(),
				OAuth2TokenType.ACCESS_TOKEN);
		Assertions.assertNotNull(loadedByToken);
		Assertions.assertEquals(authorization.getId(), loadedByToken.getId());
	}

	private static JdbcTemplate createJdbcTemplate() {
		String url = requiredEnv("FIST_IT_JDBC_URL");
		String username = requiredEnv("FIST_IT_JDBC_USERNAME");
		String password = requiredEnv("FIST_IT_JDBC_PASSWORD");
		registerDriverIfNecessary();
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return new JdbcTemplate(dataSource);
	}

	private static void prepareSchema(JdbcTemplate jdbcTemplate) throws Exception {
		if (tableExists(jdbcTemplate)) {
			return;
		}
		Assumptions.assumeTrue(Boolean.parseBoolean(env("FIST_IT_JDBC_INIT_SCHEMA")),
				"外部数据库缺少 oauth2_authorization 表，需显式设置 FIST_IT_JDBC_INIT_SCHEMA=true 才自动建表");
		try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource(AUTHORIZATION_SCHEMA)));
		}
	}

	private static boolean tableExists(JdbcTemplate jdbcTemplate) {
		try {
			jdbcTemplate.queryForObject("select count(*) from " + TABLE_NAME + " where 1 = 0", Integer.class);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	private static void registerDriverIfNecessary() {
		String driverClass = env("FIST_IT_JDBC_DRIVER_CLASS");
		if (!StringUtils.hasText(driverClass)) {
			return;
		}
		try {
			String driverJar = env("FIST_IT_JDBC_DRIVER_JAR");
			if (StringUtils.hasText(driverJar)) {
				URLClassLoader classLoader = new URLClassLoader(new URL[] { new File(driverJar).toURI().toURL() },
						ExternalJdbcAuthorizationServiceTest.class.getClassLoader());
				Driver driver = (Driver) Class.forName(driverClass, true, classLoader)
					.getDeclaredConstructor()
					.newInstance();
				DriverManager.registerDriver(new DriverShim(driver));
			}
			else {
				Class.forName(driverClass);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to register external JDBC driver", ex);
		}
	}

	private static String requiredEnv(String name) {
		String value = env(name);
		Assumptions.assumeTrue(StringUtils.hasText(value), "缺少环境变量 " + name);
		return value;
	}

	private static String env(String name) {
		return System.getenv(name);
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
		String id = "fist-it-" + UUID.randomUUID();
		Instant issuedAt = Instant.parse("2026-05-12T00:00:00Z");
		return OAuth2Authorization.withRegisteredClient(registeredClient)
			.id(id)
			.principalName("alice")
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.authorizedScopes(Set.of("read"))
			.accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token-" + id, issuedAt,
					issuedAt.plusSeconds(60), Set.of("read")))
			.build();
	}

	private static final class TestAuthorizationService extends DefaultAuthorizationService {

		TestAuthorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
			super(jdbcTemplate, registeredClientRepository);
		}

		@Override
		protected String getLobCmpFunc() {
			return env("FIST_IT_JDBC_LOB_CMP_FUNC");
		}

	}

	private static final class DriverShim implements Driver {

		private final Driver driver;

		private DriverShim(Driver driver) {
			this.driver = driver;
		}

		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			return this.driver.connect(url, info);
		}

		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return this.driver.acceptsURL(url);
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
			return this.driver.getPropertyInfo(url, info);
		}

		@Override
		public int getMajorVersion() {
			return this.driver.getMajorVersion();
		}

		@Override
		public int getMinorVersion() {
			return this.driver.getMinorVersion();
		}

		@Override
		public boolean jdbcCompliant() {
			return this.driver.jdbcCompliant();
		}

		@Override
		public Logger getParentLogger() {
			return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		}

	}

}
