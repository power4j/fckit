package com.power4j.fist.oauth2.extension.password;

import com.power4j.fist.oauth2.core.AuthConstants;
import com.power4j.fist.oauth2.extension.support.OAuth2ResourceOwnerBaseAuthenticationConverter;
import com.power4j.fist.oauth2.extension.utils.OAuth2EndpointUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * Original code comes from <a href="https://github.com/pig-mesh/pig">pig-mesh/pig</a>
 *
 * @author jumuning
 * @since 3.1
 */
public class OAuth2ResourceOwnerPasswordAuthenticationConverter
		extends OAuth2ResourceOwnerBaseAuthenticationConverter<OAuth2ResourceOwnerPasswordAuthenticationToken> {

	private static final AuthorizationGrantType PASSWORD_GRANT_TYPE = new AuthorizationGrantType(
			AuthConstants.PASSWORD);

	/**
	 * 支持密码模式
	 * @param grantType 授权类型
	 */
	@Override
	public boolean support(String grantType) {
		return PASSWORD_GRANT_TYPE.getValue().equals(grantType);
	}

	@Override
	public OAuth2ResourceOwnerPasswordAuthenticationToken buildToken(Authentication clientPrincipal,
			Set requestedScopes, Map additionalParameters) {
		return new OAuth2ResourceOwnerPasswordAuthenticationToken(PASSWORD_GRANT_TYPE, clientPrincipal, requestedScopes,
				additionalParameters);
	}

	/**
	 * 校验扩展参数 密码模式密码必须不为空
	 * @param request 参数列表
	 */
	@Override
	public void checkParams(HttpServletRequest request) {
		MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);
		// username (REQUIRED)
		String username = parameters.getFirst(AuthConstants.USERNAME_PARAMETER_NAME);
		if (!StringUtils.hasText(username) || parameters.get(AuthConstants.USERNAME_PARAMETER_NAME).size() != 1) {
			OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, AuthConstants.USERNAME_PARAMETER_NAME,
					OAuth2EndpointUtils.ACCESS_TOKEN_REQUEST_ERROR_URI);
		}

		// password (REQUIRED)
		String password = parameters.getFirst(AuthConstants.PASSWORD_PARAMETER_NAME);
		if (!StringUtils.hasText(password) || parameters.get(AuthConstants.PASSWORD_PARAMETER_NAME).size() != 1) {
			OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, AuthConstants.PASSWORD_PARAMETER_NAME,
					OAuth2EndpointUtils.ACCESS_TOKEN_REQUEST_ERROR_URI);
		}
	}

}
