package com.power4j.fist.cloud.security.oauth2.client;

import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.core.UserInfoExtractor;
import com.power4j.fist.boot.security.oauth2.Oauth2AuthenticatedPrincipal;
import com.power4j.fist.boot.security.oauth2.Oauth2UserInfoExtractor;
import com.power4j.fist.boot.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class UserIntrospectClient {

	private final ReactiveOpaqueTokenIntrospector reactiveOpaqueTokenIntrospector;

	@Setter
	private UserInfoExtractor userInfoExtractor = new Oauth2UserInfoExtractor();

	public Mono<UserInfo> loadUser(String token) {
		// @formatter:off
		return reactiveOpaqueTokenIntrospector.introspect(token)
				.onErrorResume( ex -> {
					log.warn("Token introspect error: {},message = {},token = {}",ex.getClass().getName(),ex.getMessage(),token);
					return Mono.empty();
				})
				.flatMap(this::convert);
		// @formatter:on
	}

	protected Mono<UserInfo> convert(Oauth2AuthenticatedPrincipal principal) {
		UserInfo user = userInfoExtractor.extractAuthUser(principal.getAttributes()).orElse(null);
		return Mono.justOrEmpty(user);
	}

}
