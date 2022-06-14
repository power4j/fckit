package com.power4j.fist.security.gateway;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.filter.reactive.GatewayAuthFilterChain;
import com.power4j.fist.security.core.authentication.UserConverter;
import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/9
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

	private final static AuthorizationDecision GRANTED = new AuthorizationDecision(true);

	private final static AuthorizationDecision DENIED = new AuthorizationDecision(false);

	private final GatewayAuthFilterChain filterChain;

	private final UserConverter<? extends AuthenticatedUser> userConverter;

	@Override
	public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext object) {
		return authentication.flatMap(auth -> doCheck(auth, object));
	}

	private Mono<AuthorizationDecision> doCheck(Authentication authentication, AuthorizationContext object) {
		AuthContext context = new AuthContext(object.getExchange(), authentication);
		context.setUserInfo(userConverter.convert(authentication));
		return filterChain.filter(context).doOnSuccess(o -> {
			if (Objects.isNull(context.getAuthState().getProblem())) {
				log.warn("auth filter completed without data: {}", context.getExchange().getRequest().getURI());
			}
		}).flatMap(o -> decide(context)).switchIfEmpty(Mono.defer(() -> decide(context)));
	}

	private Mono<AuthorizationDecision> decide(AuthContext context) {
		final AuthProblem problem = context.getAuthState().getProblem();
		Validate.notNull(problem);
		if (problem.isAuthPass()) {
			if (log.isDebugEnabled()) {
				log.debug("[{}] -> {},reason = {}", problem.passStr(), context.getInbound().shortDescription(),
						problem.description());
			}
			return Mono.just(GRANTED);
		}
		else {
			log.info("[{}] -> {},reason = {}", problem.passStr(), context.getInbound().shortDescription(),
					problem.description());
			return Mono.just(DENIED);
		}
	}

}
