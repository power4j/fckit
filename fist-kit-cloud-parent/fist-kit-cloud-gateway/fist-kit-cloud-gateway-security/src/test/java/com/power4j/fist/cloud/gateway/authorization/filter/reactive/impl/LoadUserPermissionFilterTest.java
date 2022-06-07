package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.security.core.authorization.domain.AnonymousUser;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.filter.reactive.DefaultServerAuthFilterChain;
import com.power4j.fist.security.core.authorization.service.reactive.PermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/7
 * @since 1.0
 */
class LoadUserPermissionFilterTest {

	PermissionService<? extends AuthenticatedUser> permissionService1 = user -> Mono
			.just(Optional.of(new AnonymousUser(Collections.emptyMap())));

	PermissionService<? extends AuthenticatedUser> permissionService2 = user -> Mono.just(Optional.empty());

	@Test
	void filter() {
		DefaultServerAuthFilterChain<AuthContext, GatewayAuthFilter> chain = new DefaultServerAuthFilterChain<>(
				Collections.emptyList());
		LoadUserPermissionFilter filter1 = new LoadUserPermissionFilter(permissionService1);
		AuthContext context1 = filter1.filter(new AuthContext(null, "user"), chain).block();
		Assertions.assertNotNull(context1.getUserInfo());
		Assertions.assertTrue(context1.getUserInfo().isAnonymous());

		LoadUserPermissionFilter filter2 = new LoadUserPermissionFilter(permissionService2);
		AuthContext context2 = filter2.filter(new AuthContext(null, "user"), chain).block();
		Assertions.assertNull(context2.getUserInfo());
	}

}