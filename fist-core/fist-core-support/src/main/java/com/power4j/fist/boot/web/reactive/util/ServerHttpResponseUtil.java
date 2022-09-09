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

package com.power4j.fist.boot.web.reactive.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/16
 * @since 1.0
 */
@UtilityClass
public class ServerHttpResponseUtil {

	private final static ObjectMapper defaultObjectMapper = new ObjectMapper();

	/**
	 * 写JSON对象,并设置HTTP头和HttpStatus
	 * @param response ServerHttpResponse
	 * @param payload Object
	 * @param status HttpStatus
	 * @return Mono
	 */
	public Mono<Void> responseWithJsonObject(ServerHttpResponse response, Object payload, HttpStatus status) {
		return Mono.fromCallable(() -> defaultObjectMapper.writeValueAsString(payload)).flatMap(json -> {
			response.setStatusCode(status);
			response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
			return writeUtf8String(response, Mono.just(json));
		});

	}

	/**
	 * 写JSON对象,并设置HTTP头和HttpStatus
	 * @param response ServerHttpResponse
	 * @param objectMapper ObjectMapper
	 * @param payload Object
	 * @param status HttpStatus
	 * @return Mono
	 */
	public Mono<Void> responseWithJsonObject(ServerHttpResponse response, ObjectMapper objectMapper, Object payload,
			HttpStatus status) {
		return Mono.fromCallable(() -> objectMapper.writeValueAsString(payload)).flatMap(json -> {
			response.setStatusCode(status);
			response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
			return writeUtf8String(response, Mono.just(json));
		});

	}

	/**
	 * 写字符串
	 * @param response ServerHttpResponse
	 * @param str String Mono
	 * @return Mono
	 */
	public Mono<Void> writeUtf8String(ServerHttpResponse response, Mono<String> str) {
		return str.map(s -> s.getBytes(StandardCharsets.UTF_8))
				.flatMap(bytes -> writeBytes(response, Mono.just(bytes)));
	}

	/**
	 * 写入byte
	 * @param response ServerHttpResponse
	 * @param data byte array Mono
	 * @return Mono
	 */
	public Mono<Void> writeBytes(ServerHttpResponse response, Mono<byte[]> data) {
		return data.map(bytes -> response.bufferFactory().wrap(bytes)).flatMap(
				buffer -> response.writeWith(Mono.just(buffer)).doOnError(e -> DataBufferUtils.release(buffer)));
	}

	/**
	 * 写入 ByteBuffer
	 * @param response ServerHttpResponse
	 * @param data ByteBuffer Mono
	 * @return Mono
	 */
	public Mono<Void> writeBuffer(ServerHttpResponse response, Mono<ByteBuffer> data) {

		return data.map(bytes -> response.bufferFactory().wrap(bytes)).flatMap(
				buffer -> response.writeWith(Mono.just(buffer)).doOnError(e -> DataBufferUtils.release(buffer)));
	}

}
