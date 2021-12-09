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

package com.power4j.fist.boot.apidoc;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.power4j.coca.kit.common.text.StringPool;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/16
 * @since 1.0
 */
@Data
@Builder
public class RestResource implements ResourceDescribe {

	private String name;

	private HttpMethod method;

	private String requestMappingPattern;

	@Override
	public String getResourceSign() {
		Assert.notNull(getMethod());
		Assert.notNull(getRequestMappingPattern());
		String context = getMethod().name().toLowerCase() + StringPool.AT + getRequestMappingPattern();
		String hi = DigestUtil.sha1Hex(context);
		String lo = HexUtil.toHex(crc32(context.getBytes(StandardCharsets.UTF_8)));
		return (hi.substring(0, 8) + StringPool.DASH + lo).toLowerCase();
	}

	@Override
	public String getResourceName() {
		return String.format("%s.%s.%s", name, getMethod().name().toLowerCase(), getResourceSign());
	}

	long crc32(byte[] data) {
		CRC32 crc32 = new CRC32();
		crc32.update(data);
		return crc32.getValue();
	}

}
