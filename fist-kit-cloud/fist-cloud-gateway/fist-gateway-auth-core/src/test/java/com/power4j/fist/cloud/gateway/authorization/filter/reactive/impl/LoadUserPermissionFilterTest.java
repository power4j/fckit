package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.security.core.authorization.domain.AnonymousUser;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.filter.reactive.DefaultServerAuthFilterChain;
import com.power4j.fist.security.core.authorization.service.reactive.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/7
 * @since 1.0
 */
class LoadUserPermissionFilterTest {

	UserService<? extends AuthenticatedUser> userService1 = user -> Mono
		.just(new AnonymousUser(Collections.emptyMap()));

	UserService<? extends AuthenticatedUser> userService2 = user -> Mono.empty();

	@Test
	void filter() {
		DefaultServerAuthFilterChain<AuthContext, GatewayAuthFilter> chain = new DefaultServerAuthFilterChain<>(
				Collections.emptyList());
		LoadUserPermissionFilter filter1 = new LoadUserPermissionFilter(userService1);
		AuthContext context1 = new AuthContext(null, "user");
		filter1.filter(context1, chain).block();
		Assertions.assertNotNull(context1.getUserInfo());
		Assertions.assertTrue(context1.getUserInfo().isAnonymous());

		LoadUserPermissionFilter filter2 = new LoadUserPermissionFilter(userService2);
		AuthContext context2 = new AuthContext(null, "user");
		filter2.filter(context2, chain).block();
		Assertions.assertNull(context2.getUserInfo());
	}

}