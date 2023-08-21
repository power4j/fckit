package com.power4j.fist.boot.security.oauth2;

import com.power4j.fist.support.spring.web.reactive.util.ServerHttpRequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
public class DefaultServerBearerTokenResolver implements ServerBearerTokenResolver {

	private static final Pattern authorizationPattern = Pattern.compile("^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$",
			Pattern.CASE_INSENSITIVE);

	private boolean allowUriQueryParameter = false;

	private String bearerTokenHeaderName = HttpHeaders.AUTHORIZATION;

	@Override
	@Nullable
	public String resolve(ServerHttpRequest request) {
		String authorizationHeaderToken = resolveFromAuthorizationHeader(request.getHeaders());
		String parameterToken = resolveAccessTokenFromRequest(request);

		if (authorizationHeaderToken != null) {
			if (parameterToken != null) {
				log.warn("Found multiple bearer tokens in the request:{}",
						ServerHttpRequestUtil.simpleRequestLine(request));
			}
			return authorizationHeaderToken;
		}
		if (parameterToken != null && isParameterTokenSupportedForRequest(request)) {
			return parameterToken;
		}
		return null;
	}

	@Nullable
	private static String resolveAccessTokenFromRequest(ServerHttpRequest request) {
		List<String> parameterTokens = request.getQueryParams().get("access_token");
		if (CollectionUtils.isEmpty(parameterTokens)) {
			return null;
		}
		if (parameterTokens.size() > 1) {
			log.warn("Found multiple bearer tokens in the request:{}",
					ServerHttpRequestUtil.simpleRequestLine(request));
		}
		return parameterTokens.get(0);

	}

	/**
	 * Set if transport of access token using URI query parameter is supported. Defaults
	 * to {@code false}.
	 *
	 * The spec recommends against using this mechanism for sending bearer tokens, and
	 * even goes as far as stating that it was only included for completeness.
	 * @param allowUriQueryParameter if the URI query parameter is supported
	 */
	public void setAllowUriQueryParameter(boolean allowUriQueryParameter) {
		this.allowUriQueryParameter = allowUriQueryParameter;
	}

	/**
	 * Set this value to configure what header is checked when resolving a Bearer Token.
	 * This value is defaulted to {@link HttpHeaders#AUTHORIZATION}.
	 *
	 * This allows other headers to be used as the Bearer Token source such as
	 * {@link HttpHeaders#PROXY_AUTHORIZATION}
	 * @param bearerTokenHeaderName the header to check when retrieving the Bearer Token.
	 * @since 5.4
	 */
	public void setBearerTokenHeaderName(String bearerTokenHeaderName) {
		this.bearerTokenHeaderName = bearerTokenHeaderName;
	}

	@Nullable
	private String resolveFromAuthorizationHeader(HttpHeaders headers) {
		String authorization = headers.getFirst(this.bearerTokenHeaderName);
		if (!StringUtils.startsWithIgnoreCase(authorization, "bearer")) {
			return null;
		}
		Matcher matcher = authorizationPattern.matcher(authorization);
		if (!matcher.matches()) {
			log.warn("Bearer token  is malformed:{}", authorization);
			return null;
		}
		return matcher.group("token");
	}

	private boolean isParameterTokenSupportedForRequest(ServerHttpRequest request) {
		return this.allowUriQueryParameter && HttpMethod.GET.equals(request.getMethod());
	}

}
