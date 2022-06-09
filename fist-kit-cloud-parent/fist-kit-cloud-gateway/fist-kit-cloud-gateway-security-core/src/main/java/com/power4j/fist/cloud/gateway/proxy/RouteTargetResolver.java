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

package com.power4j.fist.cloud.gateway.proxy;

import com.power4j.fist.cloud.gateway.authorization.domain.RouteTarget;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/13
 * @since 1.0
 */
public interface RouteTargetResolver {

	/**
	 * 解析路由
	 * @param route 与当前请求匹配的路由
	 * @param exchange ServerWebExchange
	 * @return 返回路由目标
	 */
	@Nullable
	RouteTarget resolve(@Nullable RouteInfo route, ServerWebExchange exchange);

}
