package com.power4j.fist.boot.security.oauth2.server.resource.introspection;

import com.power4j.fist.boot.security.oauth2.DefaultOauth2AuthenticatedPrincipal;
import com.power4j.fist.boot.security.oauth2.Oauth2AuthenticatedPrincipal;
import com.power4j.fist.boot.security.oauth2.Oauth2TokenIntrospectionClaimNames;
import com.power4j.fist.security.core.authorization.domain.GrantedPermission;
import com.power4j.fist.security.core.authorization.domain.SimplePermission;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
public class DefaultReactiveOpaqueTokenIntrospector implements ReactiveOpaqueTokenIntrospector {

	private static final String AUTHORITY_PREFIX = "SCOPE_";

	private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<Map<String, Object>>() {
	};

	private final URI introspectionUri;

	private final WebClient webClient;

	/**
	 * Creates a {@code OpaqueTokenReactiveAuthenticationManager} with the provided
	 * parameters
	 * @param introspectionUri The introspection endpoint uri
	 * @param clientId The client id authorized to introspect
	 * @param clientSecret The client secret for the authorized client
	 */
	public DefaultReactiveOpaqueTokenIntrospector(String introspectionUri, String clientId, String clientSecret) {
		Assert.hasText(introspectionUri, "introspectionUri cannot be empty");
		Assert.hasText(clientId, "clientId cannot be empty");
		Assert.notNull(clientSecret, "clientSecret cannot be null");
		this.introspectionUri = URI.create(introspectionUri);
		this.webClient = WebClient.builder().defaultHeaders((h) -> h.setBasicAuth(clientId, clientSecret)).build();
	}

	/**
	 * Creates a {@code OpaqueTokenReactiveAuthenticationManager} with the provided
	 * parameters
	 * @param introspectionUri The introspection endpoint uri
	 * @param webClient The client for performing the introspection request
	 */
	public DefaultReactiveOpaqueTokenIntrospector(String introspectionUri, WebClient webClient) {
		Assert.hasText(introspectionUri, "introspectionUri cannot be null");
		Assert.notNull(webClient, "webClient cannot be null");
		this.introspectionUri = URI.create(introspectionUri);
		this.webClient = webClient;
	}

	@Override
	public Mono<Oauth2AuthenticatedPrincipal> introspect(String token) {
		// @formatter:off
		return Mono.just(token)
				.flatMap(this::makeRequest)
				.flatMap(this::adaptToNimbusResponse)
				.map(this::convertClaimsSet)
				.onErrorMap((e) -> !(e instanceof Oauth2IntrospectionException), this::onError);
		// @formatter:on
	}

	private Mono<ClientResponse> makeRequest(String token) {
		// @formatter:off
		return this.webClient.post()
				.uri(this.introspectionUri)
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.body(BodyInserters.fromFormData("token", token))
				.exchange();
		// @formatter:on
	}

	private Mono<Map<String, Object>> adaptToNimbusResponse(ClientResponse responseEntity) {
		if (responseEntity.statusCode() != HttpStatus.OK) {
			// @formatter:off
			return responseEntity.bodyToFlux(DataBuffer.class)
					.map(DataBufferUtils::release)
					.then(Mono.error(new Oauth2IntrospectionException(
							"Introspection endpoint responded with " + responseEntity.statusCode()))
					);
			// @formatter:on
		}
		// relying solely on the authorization server to validate this token (not checking
		// 'exp', for example)
		return responseEntity.bodyToMono(STRING_OBJECT_MAP)
			.filter((body) -> (boolean) body.compute(Oauth2TokenIntrospectionClaimNames.ACTIVE, (k, v) -> {
				if (v instanceof String) {
					return Boolean.parseBoolean((String) v);
				}
				if (v instanceof Boolean) {
					return v;
				}
				return false;
			}))
			.switchIfEmpty(Mono.error(() -> new BadOpaqueTokenException("Provided token isn't active")));
	}

	private Oauth2AuthenticatedPrincipal convertClaimsSet(Map<String, Object> claims) {
		claims.computeIfPresent(Oauth2TokenIntrospectionClaimNames.AUD, (k, v) -> {
			if (v instanceof String) {
				return Collections.singletonList(v);
			}
			return v;
		});
		claims.computeIfPresent(Oauth2TokenIntrospectionClaimNames.CLIENT_ID, (k, v) -> v.toString());
		claims.computeIfPresent(Oauth2TokenIntrospectionClaimNames.EXP,
				(k, v) -> Instant.ofEpochSecond(((Number) v).longValue()));
		claims.computeIfPresent(Oauth2TokenIntrospectionClaimNames.IAT,
				(k, v) -> Instant.ofEpochSecond(((Number) v).longValue()));
		// RFC-7662 page 7 directs users to RFC-7519 for defining the values of these
		// issuer fields.
		// https://datatracker.ietf.org/doc/html/rfc7662#page-7
		//
		// RFC-7519 page 9 defines issuer fields as being 'case-sensitive' strings
		// containing
		// a 'StringOrURI', which is defined on page 5 as being any string, but strings
		// containing ':'
		// should be treated as valid URIs.
		// https://datatracker.ietf.org/doc/html/rfc7519#section-2
		//
		// It is not defined however as to whether-or-not normalized URIs should be
		// treated as the same literal
		// value. It only defines validation itself, so to avoid potential ambiguity or
		// unwanted side effects that
		// may be awkward to debug, we do not want to manipulate this value. Previous
		// versions of Spring Security
		// would *only* allow valid URLs, which is not what we wish to achieve here.
		claims.computeIfPresent(Oauth2TokenIntrospectionClaimNames.ISS, (k, v) -> v.toString());
		claims.computeIfPresent(Oauth2TokenIntrospectionClaimNames.NBF,
				(k, v) -> Instant.ofEpochSecond(((Number) v).longValue()));
		Collection<GrantedPermission> authorities = new ArrayList<>();
		claims.computeIfPresent(Oauth2TokenIntrospectionClaimNames.SCOPE, (k, v) -> {
			if (v instanceof String) {
				Collection<String> scopes = Arrays.asList(((String) v).split(" "));
				for (String scope : scopes) {
					authorities.add(new SimplePermission(AUTHORITY_PREFIX + scope));
				}
				return scopes;
			}
			return v;
		});
		return new DefaultOauth2AuthenticatedPrincipal(claims, authorities);
	}

	private BadOpaqueTokenException onError(Throwable ex) {
		return new BadOpaqueTokenException(ex.getMessage(), ex);
	}

}
