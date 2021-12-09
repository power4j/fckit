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

package com.power4j.fist.cloud.gateway.authorization.filter.servlet.impl;

import com.power4j.fist.boot.common.matcher.PathMatcher;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.filter.servlet.AbstractAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.Optional;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class SkipAuthorizationFilter extends AbstractAuthFilter {

	private final Set<String> paths;

	private final PathMatcher pathMatcher;

	@Override
	protected boolean process(AuthContext ctx) {
		Validate.notNull(ctx.getInbound());
		Optional<?> matched = pathMatcher.firstMatch(paths, ctx.getInbound().getRawPath());

		if (matched.isPresent()) {
			if (log.isTraceEnabled()) {
				log.trace("path pattern matched,forward skip auth. => {}", ctx.getInbound().shortDescription());
			}
			return exitChain(ctx, AuthProblem.SKIP_AUTH);
		}
		return true;
	}

}
