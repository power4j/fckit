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

package com.power4j.fist.boot.apidoc.swagger.plugin;

import cn.hutool.core.bean.BeanUtil;
import com.power4j.fist.boot.apidoc.ApiDetails;
import com.power4j.fist.boot.apidoc.ApiTrait;
import com.power4j.fist.boot.apidoc.DocConstant;
import com.power4j.fist.boot.apidoc.DocUtil;
import springfox.documentation.service.ObjectVendorExtension;
import springfox.documentation.service.StringVendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.util.Collections;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/16
 * @since 1.0
 * @deprecated
 */
public class ApiTraitOperationPlugin implements OperationBuilderPlugin {

	@Override
	public void apply(OperationContext context) {
		context.findAnnotation(ApiTrait.class).map(DocUtil::createDetails).map(this::toObjectVendorExtension)
				.ifPresent(extension -> context.operationBuilder().extensions(Collections.singletonList(extension)));

	}

	@Override
	public boolean supports(DocumentationType documentationType) {
		return SwaggerPluginSupport.pluginDoesApply(documentationType);
	}

	private ObjectVendorExtension toObjectVendorExtension(ApiDetails details) {
		ObjectVendorExtension extension = new ObjectVendorExtension(DocConstant.SECURE_API_DETAILS_EXTENSION);
		Map<String, Object> props = BeanUtil.beanToMap(details);
		props.forEach((k, v) -> {
			extension.addProperty(new StringVendorExtension(k, v.toString()));
		});
		return extension;
	}

}
