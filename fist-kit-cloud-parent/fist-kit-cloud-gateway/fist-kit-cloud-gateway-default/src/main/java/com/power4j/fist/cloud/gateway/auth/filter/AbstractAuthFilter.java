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

import com.power4j.fist.cloud.gateway.auth.api.GatewayAuthFilter;
import com.power4j.fist.cloud.gateway.auth.api.GatewayAuthFilterChain;
import com.power4j.fist.cloud.gateway.auth.entity.AuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/16
 * @since 1.0
 */
@Slf4j
public abstract class AbstractAuthFilter implements GatewayAuthFilter {

	@Override
	public void handle(AuthContext context, GatewayAuthFilterChain chain) {
		boolean result = process(context);
		if (log.isDebugEnabled()) {
			log.debug("{} -> state : {}", getClass().getName(), context.getResponseInfo().getProblem());
		}
		if (result) {
			chain.runNext(context);
		}
		else {
			Assert.notNull(context.getResponseInfo().getProblem(), "AuthProblem still null");
		}
	}

	/**
	 * 执行处理 <b>注意事项: 终结处理链必须更新 AuthProblem</b>
	 * @param context AuthContext
	 * @return 返回true 继续执行链路 false 不再执行
	 */
	protected abstract boolean process(AuthContext context);

}
