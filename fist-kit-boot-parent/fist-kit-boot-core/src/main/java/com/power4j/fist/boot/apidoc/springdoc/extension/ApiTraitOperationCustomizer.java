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

package com.power4j.fist.boot.apidoc.springdoc.extension;

import com.power4j.fist.boot.apidoc.ApiDetails;
import com.power4j.fist.boot.apidoc.ApiTrait;
import com.power4j.fist.boot.apidoc.DocConstant;
import com.power4j.fist.boot.apidoc.DocUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Operation;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/30
 * @since 1.0
 */
public class ApiTraitOperationCustomizer implements GlobalOperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		Optional.ofNullable(handlerMethod.getMethodAnnotation(ApiTrait.class)).ifPresent(apiTrait -> {
			ApiDetails details = DocUtil.createDetails(apiTrait);
			operation.addExtension(DocConstant.SECURE_API_DETAILS_EXTENSION, details);
			final String tip = String.format("层级:%s | 权限码:%s | 访问类型:%s", apiTrait.level().name(), apiTrait.code(),
					apiTrait.access().name());
			final String desc = operation.getDescription();
			if (StringUtils.isNotEmpty(desc)) {
				operation.description(String.format("%s ( %s )", desc, tip));
			}
			else {
				operation.description(tip);
			}
		});
		return operation;
	}

	protected Optional<Tag> getDocTag(HandlerMethod handlerMethod) {
		Tag tag = handlerMethod.getMethodAnnotation(Tag.class);
		if (Objects.isNull(tag)) {
			tag = handlerMethod.getBeanType().getAnnotation(Tag.class);
		}
		return Optional.ofNullable(tag);
	}

}
