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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.inner.DefaultUserCodec;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/21
 * @since 1.0
 */
public class EncodeUserTest {

	private UserInfo user;

	@Before
	public void init() {
		Map<String, Object> info = new HashMap<>(2);
		info.put("age", 23);
		info.put("source", "delta");
		user = new UserInfo();
		user.setUsername("admin");
		user.setUserId(1L);
		user.setTenantId("23041");
		user.setNickName("coco");
		user.setAdditionalInfo(info);
		user.setAuthorities(Collections.emptySet());
	}

	@Test
	public void testJwtEncode() throws JsonProcessingException {
		String key = "1234567890abcdef1234567890abcdef";
		MacSigner signer = new MacSigner(key);
		String jwt = JwtHelper.encode(new ObjectMapper().writeValueAsString(user), signer).getEncoded();
		System.out.println(jwt);
	}

	@Test
	public void testTokenEncode() throws JsonProcessingException {
		DefaultUserCodec codec = new DefaultUserCodec();
		codec.setZipPredicate(s -> (s.length() > 64));
		String token = codec.encode(user);
		System.out.println(token);
	}

}