package com.power4j.fist.cloud.oauth2.server.resource.reactive;

import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.security.core.UserInfoExtractor;
import com.power4j.fist.boot.security.inner.UserEncoder;
import com.power4j.fist.boot.security.oauth2.Oauth2UserInfoExtractor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/9
 * @since 1.0
 */
@RequiredArgsConstructor
public class DefaultAuthenticationProcessor implements AuthenticationProcessor {

	private final UserEncoder encoder;

	@Setter
	private UserInfoExtractor extractor = new Oauth2UserInfoExtractor();

	@Override
	public Mono<ServerWebExchange> process(ServerWebExchange origin, Map<String, Object> props) {
		// @formatter:off
		return extractor
				.extractAuthUser(props)
				.map(encoder::encode)
				.map(val -> Mono.just(withInnerUser(origin, val)))
				.orElseGet(() -> Mono.just(origin));
		// @formatter:on
	}

	protected ServerWebExchange withInnerUser(ServerWebExchange origin, String value) {
		// @formatter:off
		ServerHttpRequest request = origin.getRequest()
				.mutate()
				.headers(h -> h.add(SecurityConstant.HEADER_USER_TOKEN_INNER,value))
				.build();
		return origin.mutate().request(request).build();
		// @formatter:on
	}

}
