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

import com.power4j.coca.kit.common.text.StringPool;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/16
 * @since 1.0
 */
@Data
@Builder
public class HandlerMethodResource implements ResourceDescribe {

	private Class<?> clazz;

	private String methodName;

	@Nullable
	private List<Class<?>> parameterTypes;

	private Class<?> returnType;

	@Override
	public String getResourceName() {
		return String.format("%s.%s.%s", clazz.getSimpleName(), methodName, getResourceSign());
	}

	@Override
	public String getResourceSign() {
		String context = buildMethodSignature();
		return DigestUtils.sha1Hex(context).toLowerCase();
	}

	private String buildMethodSignature() {
		Validate.notNull(methodName);
		Validate.notNull(returnType);
		StringBuilder builder = new StringBuilder();
		builder.append(StringPool.BRACKET_L).append(clazz.getSimpleName()).append(StringPool.BRACKET_R);
		builder.append(methodName);
		builder.append(StringPool.BRACKET_L);
		if (Objects.nonNull(parameterTypes)) {
			int parameterIndex = 0;
			for (Class<?> parameterType : parameterTypes) {
				if (parameterIndex++ > 0) {
					builder.append(StringPool.COMMA);
				}
				builder.append(parameterType.getSimpleName());
			}
		}
		builder.append(StringPool.BRACKET_R);
		return builder.toString();
	}

}
