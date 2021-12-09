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

package com.power4j.fist.cloud.gateway.auth.infra.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.coca.kit.common.exception.RuntimeFaultException;
import com.power4j.coca.kit.common.lang.Result;
import com.power4j.coca.kit.common.text.StringPool;
import com.power4j.fist.cloud.core.constant.ServiceConstant;
import com.power4j.fist.cloud.gateway.auth.entity.PermDefinition;
import com.power4j.fist.cloud.gateway.auth.infra.service.RemotePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/16
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class RemotePermissionServiceImpl implements RemotePermissionService {

	private final static String LOAD_PERMISSIONS_URL_TEMPLATE = "http://" + ServiceConstant.SERVICE_NAME_IAM
			+ "/v1/pl/api-info/service/%s?method=%s";

	private final TypeReference<Result<List<PermDefinition>>> permissionListResultType = new TypeReference<Result<List<PermDefinition>>>() {
	};

	private final ObjectMapper objectMapper;

	private final RestTemplate lbRestTemplate;

	@Override
	public Result<List<PermDefinition>> loadPermission(String serviceName, @Nullable String method) {
		final String url = String.format(LOAD_PERMISSIONS_URL_TEMPLATE, serviceName,
				Objects.isNull(method) ? StringPool.EMPTY : method);
		ResponseEntity<String> responseEntity = lbRestTemplate.getForEntity(url, String.class);
		try {
			return objectMapper.readValue(responseEntity.getBody(), permissionListResultType);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeFaultException(e);
		}
	}

}
