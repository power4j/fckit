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

package com.power4j.fist.cloud.gateway.filter;

import cn.hutool.core.lang.UUID;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.boot.web.constant.HttpConstant;
import com.power4j.fist.boot.web.reactive.constant.ContextConstant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/18
 * @since 1.0
 */
public class PreProcessFilter implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		final String requestId = UUID.fastUUID().toString();
		ServerHttpRequest request = exchange.getRequest().mutate().headers(this::removeHeader)
				.header(HttpConstant.Header.KEY_REQUEST_ID, requestId).build();
		return Mono.just(chain).contextWrite(context -> context.put(ContextConstant.KEY_MDC, requestId))
				.flatMap(c -> c.filter(exchange.mutate().request(request).build()));
	}

	private void removeHeader(HttpHeaders httpHeaders) {
		// 处理一些特殊的HTTP头，其他的可以通过SCG配置属性处理
		httpHeaders.remove(SecurityConstant.HEADER_JWT_INNER);
		httpHeaders.remove(SecurityConstant.HEADER_USER_TOKEN_INNER);
	}

}
