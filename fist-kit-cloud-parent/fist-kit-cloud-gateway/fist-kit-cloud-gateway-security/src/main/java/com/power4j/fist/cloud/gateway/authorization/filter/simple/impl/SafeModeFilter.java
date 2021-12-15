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

package com.power4j.fist.cloud.gateway.authorization.filter.simple.impl;

import com.power4j.fist.cloud.gateway.authorization.domain.AuthContext;
import com.power4j.fist.cloud.gateway.authorization.domain.AuthProblem;
import com.power4j.fist.cloud.gateway.authorization.filter.simple.AbstractAuthFilter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/15
 * @since 1.0
 */
public class SafeModeFilter extends AbstractAuthFilter {

	@Override
	protected boolean process(AuthContext ctx) {
		boolean isSafe = Optional.ofNullable(ctx.getExchange().getRequest().getRemoteAddress())
				.map(InetSocketAddress::getAddress).map(InetAddress::isLoopbackAddress).orElse(false);

		return exitChain(ctx, isSafe ? AuthProblem.SAFE_MODE_PASS : AuthProblem.SAFE_MODE_DENIED);
	}

}
