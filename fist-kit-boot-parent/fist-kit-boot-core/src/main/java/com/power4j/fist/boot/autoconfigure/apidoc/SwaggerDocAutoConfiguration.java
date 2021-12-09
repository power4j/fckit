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

package com.power4j.fist.boot.autoconfigure.apidoc;

import com.power4j.fist.boot.apidoc.swagger.plugin.ApiTraitOperationPlugin;
import com.power4j.fist.boot.security.core.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.configuration.OpenApiDocumentationConfiguration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/16
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OpenApiDocumentationConfiguration.class)
public class SwaggerDocAutoConfiguration {

	private final static String SECURITY_REFERENCE_NAME = SecurityConstant.HEADER_USER_TOKEN_INNER;

	@Bean
	public OperationBuilderPlugin apiTraitOperationPlugin() {
		return new ApiTraitOperationPlugin();
	}

	@Bean
	public Docket docket() {
		// FIXME 可配置化
		return new Docket(DocumentationType.OAS_30).apiInfo(apiInfo()).enable(true).securitySchemes(apiKeyList())
				.securityContexts(securityContexts()).select()
				.apis(RequestHandlerSelectors.withMethodAnnotation(Operation.class))
				// .apis(RequestHandlerSelectors.basePackage("com.power4j.fist"))
				.paths(PathSelectors.any()).build();
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("FIST 接口文档")
				.contact(new Contact("power4j", "https://github.com/power4j", "amNsYXp6QG91dGxvb2suY29t"))
				.version("1.0").build();
	}

	private List<SecurityScheme> apiKeyList() {
		ApiKey apiKey = new ApiKey(SECURITY_REFERENCE_NAME, SecurityConstant.HEADER_USER_TOKEN_INNER, "header");
		return Collections.singletonList(apiKey);
	}

	private List<SecurityContext> securityContexts() {
		SecurityContext securityContext = SecurityContext.builder().securityReferences(defaultAuth())
				.operationSelector(c -> true).build();
		return Collections.singletonList(securityContext);
	}

	private List<SecurityReference> defaultAuth() {
		AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
		AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
		authorizationScopes[0] = authorizationScope;
		List<SecurityReference> securityReferenceList = new ArrayList<>();
		securityReferenceList.add(new SecurityReference(SECURITY_REFERENCE_NAME, authorizationScopes));
		return securityReferenceList;
	}

}
