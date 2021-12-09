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

package com.power4j.fist.cloud.gateway.auth.util;

import com.power4j.coca.kit.common.exception.RuntimeFaultException;
import com.power4j.fist.cloud.gateway.auth.api.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.auth.api.GatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/6
 * @since 1.0
 */
@Slf4j
public class DefaultGatewayAuthFilterChain implements GatewayAuthFilterChain {

	private final List<GatewayAuthFilter> filters;

	private final int index;

	public DefaultGatewayAuthFilterChain(List<GatewayAuthFilter> filters, int index) {
		this.filters = filters;
		this.index = index;
	}

	@Override
	public void runNext(AuthContext context) {
		if (index < filters.size()) {
			final GatewayAuthFilter filter = filters.get(index);
			try {
				filter.handle(context, new DefaultGatewayAuthFilterChain(filters, index + 1));
			}
			catch (Exception e) {
				String msg = String.format("%s throw Exception:%s", filter.getClass().getName(), e.getMessage());
				log.error(msg, e);
				throw new RuntimeFaultException(e);
			}
		}
	}

}
