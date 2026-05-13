package com.power4j.fist3.examples.webdatasecurity.servlet;

import java.util.Map;

import com.power4j.fist.boot.mon.info.TraceInfoResolver;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.core.UserInfoSupplier;
import com.power4j.fist.boot.security.inner.DefaultUserCodec;
import com.power4j.fist.boot.security.inner.UserDecoder;
import com.power4j.fist.boot.web.constant.HttpConstant;
import com.power4j.fist.data.crud.validate.ObjectValidator;
import com.power4j.fist.data.tenant.TenantInvokeAspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WebDataSecurityExampleApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebDataSecurityExampleApplicationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldServeRequestThroughWebDataSecurityAutoConfigurations() {
		RestClient client = RestClient.create("http://localhost:" + this.port);
		Map<String, Object> body = client
			.get()
			.uri("/api/profiles/1?pageNumber=2&pageSize=5")
			.header(HttpConstant.Header.KEY_REQUEST_ID, "REQ-BOOT4")
			.header(SecurityConstant.HEADER_USER_TOKEN_INNER, encodeUser("boot4-user"))
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});

		assertThat(body).containsEntry("id", "1")
			.containsEntry("username", "alice")
			.containsEntry("pageNumber", 2)
			.containsEntry("pageSize", 5)
			.containsEntry("requestId", "REQ-BOOT4")
			.containsEntry("currentUser", "boot4-user");
		Map<String, Object> created = client.post()
			.uri("/api/profiles")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("username", "carol"))
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});

		assertThat(created).containsEntry("id", "2").containsEntry("username", "carol");
		assertThat(this.applicationContext.getBeansOfType(TenantInvokeAspect.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(ObjectValidator.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(PasswordEncoder.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(UserDecoder.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(UserInfoSupplier.class)).hasSize(1);
		assertThat(this.applicationContext.getBeansOfType(TraceInfoResolver.class)).hasSize(1);
		assertThat(this.applicationContext.containsBean("tenantContextFilter")).isTrue();
		assertThat(this.applicationContext.containsBean("headerMdcFilter")).isTrue();
		assertThat(this.applicationContext.containsBean("trustedUserFilter")).isTrue();
	}

	private String encodeUser(String username) {
		UserInfo userInfo = new UserInfo();
		userInfo.setUserId(1001L);
		userInfo.setUsername(username);
		return new DefaultUserCodec().encode(userInfo);
	}

}
