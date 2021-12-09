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

package com.power4j.fist.cloud.gateway.auth.filter;

import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.inner.UserCodecException;
import com.power4j.fist.boot.security.inner.UserEncoder;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import com.power4j.fist.cloud.gateway.auth.entity.AuthProblem;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/6
 * @since 1.0
 */
@Slf4j
public class EncodeUserFilter extends AbstractAuthFilter {

	private final UserEncoder encoder;

	public EncodeUserFilter(UserEncoder encoder) {
		this.encoder = encoder;
	}

	@Override
	protected boolean process(AuthContext context) {
		final UserInfo authUser = context.getAuthUser();
		if (Objects.nonNull(authUser)) {
			try {
				String value = encoder.encode(authUser);
				context.getResponseInfo().setUserToken(value);
				return true;
			}
			catch (UserCodecException e) {
				log.error(e.getMessage(), e);
				context.getResponseInfo().setProblem(AuthProblem.AUTH_EXCEPTION);
			}
		}
		return false;
	}

}
