package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.oauth2.DefaultServerBearerTokenResolver;
import com.power4j.fist.boot.security.oauth2.ServerBearerTokenResolver;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.cloud.security.oauth2.client.UserIntrospectClient;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.GrantedPermission;
import com.power4j.fist.security.core.authorization.domain.SimplePermission;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class Oauth2IntrospectFilter implements GatewayAuthFilter {

	private final UserIntrospectClient userService;

	@Setter
	private ServerBearerTokenResolver bearerTokenResolver = new DefaultServerBearerTokenResolver();

	@Override
	public Mono<Void> filter(AuthContext ctx, ServerAuthFilterChain<AuthContext> chain) {
		return updateUserInfo(ctx).flatMap(o -> doNext(o, chain));
	}

	protected Mono<AuthContext> updateUserInfo(AuthContext ctx) {
		// TODO: enable Anonymous?
		// @formatter:off
		return useAccessToken(ctx)
				.flatMap(userService::loadUser)
				.map(u ->{
					ctx.setPrincipal(u);
					ctx.setUserInfo(new AuthenticatedUserAdapter(u));
					return ctx;
				});
		// @formatter:on
	}

	protected Mono<String> useAccessToken(AuthContext ctx) {
		final String token = bearerTokenResolver.resolve(ctx.getExchange().getRequest());
		if (Objects.isNull(token)) {
			if (log.isDebugEnabled()) {
				log.debug("No token,skip load user permission");
			}
		}
		return Mono.justOrEmpty(token);
	}

	public static class AuthenticatedUserAdapter implements AuthenticatedUser {

		private final UserInfo userInfo;

		AuthenticatedUserAdapter(UserInfo userInfo) {
			this.userInfo = userInfo;
		}

		@Override
		public String getUsername() {
			return userInfo.getUsername();
		}

		@Override
		public String getTenantId() {
			return userInfo.getMetaProp(SecurityConstant.UserProp.KEY_TENANT_ID, String.class).orElse(StringPool.N_A);
		}

		@Override
		public Map<String, GrantedPermission> getPermissions() {
			Typed<Collection<String>> type = new TypeLiteral<>() {
			};
			// @formatter:off
			return userInfo.getMetaProp(SecurityConstant.UserProp.KEY_PERMISSION_LIST,type)
					.orElse(Collections.emptyList())
					.stream()
					.distinct()
					.collect(Collectors.toMap(Function.identity(), SimplePermission::new));
			// @formatter:on
		}

		@Override
		public Map<String, Object> getAdditionalInfo() {
			return Objects.requireNonNull(userInfo.getMeta());
		}

	}

}
