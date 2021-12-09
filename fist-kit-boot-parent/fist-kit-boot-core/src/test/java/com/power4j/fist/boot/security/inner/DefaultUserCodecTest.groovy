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

package com.power4j.fist.boot.security.inner

import com.power4j.coca.kit.common.io.codec.impl.BufferGz
import com.power4j.fist.boot.security.core.UserInfo
import spock.lang.Specification

import java.util.function.Predicate

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/20
 * @since 1.0
 */
class DefaultUserCodecTest extends Specification {
	class ZipOn implements Predicate<String> {
		private final int size
		public ZipOn(int size){
			this.size = size
		}
		@Override
		boolean test(String s) {
			return s.size() > size
		}
	}

	UserInfo userInfo;
	def setup(){
		userInfo = new UserInfo()
		userInfo.setUsername("admin")
	}

    def "Should not zip"() {
		DefaultUserCodec codec = new DefaultUserCodec()
		given:
		ZipOn never = new ZipOn(Integer.MAX_VALUE)
		codec.setZipPredicate(never)

		when:
		String value = codec.encode(userInfo)

		then:
		System.out.println(value)
		!value.startsWith(BufferGz.NAME)

		when:
		UserInfo decoded = codec.decode(value)

		then:
		userInfo.getUsername() == decoded.getUsername()
    }

	def "Should zip"() {
		DefaultUserCodec codec = new DefaultUserCodec();
		given:
		ZipOn always = new ZipOn(-1)
		codec.setZipPredicate(always)

		when:
		String value = codec.encode(userInfo)

		then:
		System.out.println(value)
		value.startsWith(BufferGz.NAME)

		when:
		UserInfo decoded = codec.decode(value)

		then:
		userInfo.getUsername() == decoded.getUsername()
	}
}
