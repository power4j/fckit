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

import cn.hutool.core.collection.CollectionUtil;
import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import com.power4j.fist.cloud.gateway.auth.entity.AuthProblem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/14
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class SkipAuthFilter extends AbstractAuthFilter {

	// TODO : 读取网关配置
	private final Set<String> paths = CollectionUtil.newHashSet("/*/v3/api-docs/**", "/v3/api-docs/**");

	private final PathMatcher pathMatcher;

	@Override
	protected boolean process(AuthContext context) {
		final String gatewayUrl = context.getRequestInfo().getUri().getPath();
		if (pathMatcher.firstMatch(paths, gatewayUrl).isPresent()) {
			if (log.isDebugEnabled()) {
				log.debug("Skip auth: {}", gatewayUrl);
			}
			context.getResponseInfo().setProblem(AuthProblem.SKIP_AUTH);
			return false;
		}
		return true;
	}

}
