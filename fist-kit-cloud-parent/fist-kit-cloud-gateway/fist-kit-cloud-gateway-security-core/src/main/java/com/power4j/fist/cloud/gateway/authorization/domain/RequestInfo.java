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

package com.power4j.fist.cloud.gateway.authorization.domain;

import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
	 * 请求地址
	 */
	private final URI uri;

	/**
	 * 经常用到
	 */
	private final String rawPath;

	@Getter
	private final Map<String, Object> meta;

	/**
	 * For test
	 * @param uri the uri value
	 * @return RequestInfo with empty headers
	 */
	public static RequestInfo httpGet(String uri) {
		return new RequestInfo(HttpHeaders.EMPTY, HttpMethod.GET, URI.create(uri));
	}

	public RequestInfo(HttpHeaders headers, HttpMethod method, URI uri) {
		this.headers = headers;
		this.method = method;
		this.uri = uri;
		this.rawPath = uri.getRawPath();
		this.meta = new HashMap<>();
	}

	public String shortDescription() {
		return method.name() + " " + rawPath;
	}

}
