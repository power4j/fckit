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

package com.power4j.fist.boot.security.inner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.coca.kit.common.io.codec.CodecException;
import com.power4j.coca.kit.common.io.codec.impl.BufferGz;
import com.power4j.coca.kit.common.text.obscure.StrObscurer;
import com.power4j.fist.boot.security.core.UserInfo;

import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/20
 * @since 1.0
 */
public class DefaultUserCodec implements UserCodec {

	private Predicate<String> zipPredicate = str -> str.length() > 512;

	private final StrObscurer obscurer;

	private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
			false);

	public DefaultUserCodec() {
		obscurer = StrObscurer.ofEncoders(Collections.singletonList(new BufferGz()));
	}

	public DefaultUserCodec setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		return this;
	}

	public DefaultUserCodec setZipPredicate(Predicate<String> zipPredicate) {
		this.zipPredicate = zipPredicate;
		return this;
	}

	@Override
	public String encode(UserInfo user) {
		String json;
		try {
			json = objectMapper.writeValueAsString(user);
			StrObscurer.EncoderSelector selector = (s,
					l) -> l.stream().filter(enc -> zipPredicate.test(s)).collect(Collectors.toList());
			return obscurer.obscure(json, selector);
		}
		catch (JsonProcessingException | CodecException e) {
			throw new UserCodecException(e.getMessage(), e);
		}
	}

	@Override
	public UserInfo decode(String value) {
		try {
			String json = obscurer.parse(value);
			return objectMapper.readValue(json, UserInfo.class);
		}
		catch (JsonProcessingException | CodecException e) {
			throw new UserCodecException(e.getMessage(), e);
		}
	}

}
