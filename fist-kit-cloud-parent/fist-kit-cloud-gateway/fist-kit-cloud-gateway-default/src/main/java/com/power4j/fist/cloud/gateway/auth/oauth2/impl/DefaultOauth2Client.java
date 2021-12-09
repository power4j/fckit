/*
 *  Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.gnu.org/licenses/lgpl.html
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.power4j.fist.cloud.gateway.auth.oauth2.impl;

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.fist.cloud.gateway.auth.oauth2.InvalidTokenException;
import com.power4j.fist.cloud.gateway.auth.oauth2.Oauth2Client;
import com.power4j.fist.cloud.gateway.auth.oauth2.Oauth2Exception;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/20
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultOauth2Client implements Oauth2Client {

	private final static String KEY_ERROR = "error";

	private final static String KEY_DESCRIPTION = "error_description";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final TypeReference<Map<String, Object>> checkTokenResultType = new TypeReference<Map<String, Object>>() {
	};

	private final RestTemplate lbRestTemplate;

	private final String checkTokenUrl;

	@Setter
	@Nullable
	private String clientId;

	@Setter
	@Nullable
	private String clientSecret;

	@Override
	public Map<String, Object> checkToken(String token) {
		ResponseEntity<String> responseEntity;
		try {
			responseEntity = lbRestTemplate.exchange(getTokenInfoRequest(token), String.class);
		}
		catch (HttpClientErrorException e) {
			String responseText = e.getResponseBodyAsString();
			Map<String, Object> prop = parseProp(responseText);
			if (Objects.nonNull(prop)) {
				if (prop.containsKey(KEY_ERROR)) {
					log.error("获认证限信息失败:{} {}", prop.get(KEY_ERROR), prop.get(KEY_DESCRIPTION));
					throw new InvalidTokenException(prop.get(KEY_ERROR).toString(), e);
				}
			}
			throw new InvalidTokenException(e);
		}
		catch (RestClientException e) {
			throw new InvalidTokenException(e);
		}
		final String body = responseEntity.getBody();
		Map<String, Object> prop = parseProp(body);
		if (Objects.isNull(prop)) {
			throw new Oauth2Exception("Bad response:" + body);
		}
		return prop;
	}

	@Nullable
	private Map<String, Object> parseProp(@Nullable String context) {
		if (Objects.isNull(context) || context.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(context, checkTokenResultType);
		}
		catch (JsonProcessingException e) {
			log.warn(e.getMessage(), e);
			return null;
		}
	}

	private RequestEntity<Void> getTokenInfoRequest(String token) {
		final String url = checkTokenUrl + "?token=" + token;
		final String authorization = getAuthorizationHeader(clientId, clientSecret);
		return RequestEntity.get(url).header(HttpHeaders.AUTHORIZATION, authorization).build();
	}

	private String getAuthorizationHeader(@Nullable String clientId, @Nullable String clientSecret) {

		if (clientId == null || clientSecret == null) {
			log.warn(
					"Null Client ID or Client Secret detected. Endpoint that requires authentication will reject request with 401 error.");
		}
		String val = String.format("%s:%s", clientId, clientSecret);
		return "Basic " + Base64.encode(val.getBytes(StandardCharsets.UTF_8));
	}

}
