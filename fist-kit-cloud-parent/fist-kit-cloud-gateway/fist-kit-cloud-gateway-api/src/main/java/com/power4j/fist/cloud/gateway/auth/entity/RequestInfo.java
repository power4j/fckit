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

package com.power4j.fist.cloud.gateway.auth.entity;

import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import java.net.URI;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/23
 * @since 1.0
 */
@Getter
public class RequestInfo {

	/**
	 * HTTP 头
	 */
	private final HttpHeaders headers;

	/**
	 * 请求方法
	 */
	private final HttpMethod method;

	/**
	 * 未处理的原始请求地址
	 */
	private final URI uri;

	/**
	 * token
	 */
	@Nullable
	private final String accessToken;

	public RequestInfo(HttpHeaders headers, HttpMethod method, URI uri, @Nullable String accessToken) {
		this.headers = headers;
		this.method = method;
		this.uri = uri;
		this.accessToken = accessToken;
	}

}
