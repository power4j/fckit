package com.power4j.fist.cloud.gateway.authorization.filter.reactive.impl;

import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.cloud.gateway.AuthUtils;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthUser;
import com.power4j.fist.cloud.gateway.authorization.domain.ResourceLevel;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilter;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import com.power4j.fist.security.core.authorization.domain.PermissionDefinition;
import com.power4j.fist.security.core.authorization.filter.reactive.ServerAuthFilterChain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/25
 * @since 1.0
 */
@Slf4j
public class TenantFilter implements GatewayAuthFilter {

	private final String paramKey;

	private final String headerKey;

	public TenantFilter(String paramKey, String headerKey) {
		this.paramKey = paramKey;
		this.headerKey = headerKey;
	}

	@Override
	public Mono<Void> filter(AuthContext ctx, ServerAuthFilterChain<AuthContext> chain) {
		final PermissionDefinition permissionDefinition = ctx.getPermissionDefinition();
		if (Objects.isNull(ctx.getPermissionDefinition())) {
			return doNext(ctx, chain);
		}
		final AuthenticatedUser userInfo = ctx.getUserInfo();
		if (Objects.isNull(userInfo)) {
			return exitChain(ctx, AuthProblem.PERMISSION_CHECK_DENIED.moreInfo("No user info"));
		}
		final String tenantId = resolveTenantId(ctx.getExchange().getRequest()).orElse(null);
		if (!validateTenant(tenantId, ctx)) {
			if (log.isDebugEnabled()) {
				log.debug("tenant check fail on resource: {} {}. action = {}", permissionDefinition.getServiceName(),
						permissionDefinition.getPath(), ctx.getInbound().shortDescription());
			}
			return exitChain(ctx, AuthProblem.TENANT_CHECK_DENIED);
		}
		return doNext(ctx, chain);
	}

	private boolean validateTenant(@Nullable String tenantId, AuthContext ctx) {
		AuthenticatedUser userInfo = Objects.requireNonNull(ctx.getUserInfo());
		assert ctx.getPermissionDefinition() != null;
		if (ResourceLevel.PL.getValue().equals(ctx.getPermissionDefinition().getLevel())) {
			// remove header only
			ctx.setExchange(AuthUtils.eraseHeader(ctx.getExchange(), SecurityConstant.HEADER_USER_TOKEN_INNER));
			return true;
		}
		if (Objects.isNull(tenantId)) {
			if (log.isDebugEnabled()) {
				log.debug("No tenant id found on this request. {}", ctx.getInbound().shortDescription());
			}
			ctx.updateState(AuthProblem.TENANT_ID_REQUIRED);
			return false;
		}
		List<String> orgList = userInfo.getInfo(AuthUser.INFO_KEY_ORG, Collections.emptyList(),
				new TypeLiteral<List<String>>() {
				});
		if (!orgList.contains(tenantId)) {
			if (log.isDebugEnabled()) {
				log.debug("User({}) is not a member of tenant ({}). {}", userInfo.getUsername(), tenantId,
						ctx.getInbound().shortDescription());
			}
			ctx.updateState(AuthProblem.TENANT_CHECK_DENIED);
			return false;
		}
		return true;
	}

	// ~ Util
	// ===================================================================================================

	protected Optional<String> resolveTenantId(ServerHttpRequest request) {
		String value = request.getQueryParams().getFirst(paramKey);
		if (ObjectUtils.isEmpty(value)) {
			value = request.getHeaders().getFirst(headerKey);
		}
		return Optional.ofNullable(value).filter(o -> !o.isEmpty());
	}

}
