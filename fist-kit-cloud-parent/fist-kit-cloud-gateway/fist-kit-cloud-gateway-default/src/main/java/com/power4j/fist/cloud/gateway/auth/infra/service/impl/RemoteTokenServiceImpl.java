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

import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.api.Results;
import com.power4j.fist.boot.common.error.ErrorCode;
import com.power4j.fist.cloud.gateway.auth.infra.service.RemoteTokenService;
import com.power4j.fist.cloud.gateway.auth.oauth2.Oauth2Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/17
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteTokenServiceImpl implements RemoteTokenService {

	private final Oauth2Client oauth2Client;

	@Override
	public Result<Map<String, Object>> checkToken(String token) {
		try {
			Map<String, Object> map = oauth2Client.checkToken(token);
			return Results.ok(map);
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return Result.create(ErrorCode.A0321, e.getMessage(), null);
		}
	}

}
