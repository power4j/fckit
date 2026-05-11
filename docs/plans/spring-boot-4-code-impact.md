# Spring Boot 4 源码影响清单

日期：2026-05-10
执行者：Codex
阶段：阶段 4「基于编译错误的源码和测试命中清单」

## 结论摘要

阶段 4 第一版以阶段 2 的真实编译错误为主输入，再补充静态扫描。当前不修改 Java 源码。

当前第一阻塞点 `fist-support-web` 的 Spring Boot WebFlux 错误处理 API 包迁移已处理，`fist-support-web` 主代码编译已通过。继续试编译后，`fist-support-security` 暴露的 Spring Security 7 与 WebClient API 迁移点也已处理，主代码编译和定向测试已通过。

静态扫描显示：

- Jackson 2 包名命中 40 个 Java 文件，仍是最大源码迁移面。
- `org.springframework.lang.Nullable` 命中 89 个 Java 文件，需要和 JSpecify 策略统一处理。
- Feign 命中 26 个 Java 文件，SDE Feign 编解码仍需专项评估。
- Groovy 测试文件共 5 个，后续应迁移到 Java/JUnit 5。
- `@MockBean`、`@SpyBean` 和 `org.springframework.security.jwt` 源码直接引用未命中。
- `javax.*` 命中仅来自 JDK `javax.crypto`，不是 Jakarta EE 迁移问题。

## 已处理：WebFlux 错误处理 API 包迁移

### 编译错误来源

失败模块：

```text
fist-kit-app/fist-web/fist-support-web
```

受影响文件：

- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/reactive/error/GlobalErrorAttributes.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/reactive/error/GlobalErrorConfigure.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/reactive/error/GlobalErrorWebExceptionHandler.java`

### 当前旧引用

```text
org.springframework.boot.web.reactive.error.DefaultErrorAttributes
org.springframework.boot.web.reactive.error.ErrorAttributes
org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
```

### Spring Boot 4.0.6 中的新位置

```text
org.springframework.boot.webflux.error.DefaultErrorAttributes
org.springframework.boot.webflux.error.ErrorAttributes
org.springframework.boot.webflux.autoconfigure.error.ErrorWebFluxAutoConfiguration
```

### 处理结果

已将 3 个文件的 import 更新到 Spring Boot 4.0.6 新包，并运行：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests compile
```

结果：`BUILD SUCCESS`。

说明：OpenRewrite dry-run 未覆盖该迁移点，因此后续不能只依赖自动化补丁。

## 已处理：Spring Security 7 与 WebClient API 迁移

`fist-support-web` 解除阻塞后，`fist-support-security` 暴露 3 个编译错误。

失败模块：

```text
fist-kit-app/fist-security/fist-support-security
```

受影响文件：

- `fist-kit-app/fist-security/fist-support-security/src/main/java/com/power4j/fist/boot/security/crypto/Sm3PasswordEncoder.java`
- `fist-kit-app/fist-security/fist-support-security/src/main/java/com/power4j/fist/boot/security/oauth2/server/resource/introspection/DefaultReactiveOpaqueTokenIntrospector.java`

错误类型：

- `Sm3PasswordEncoder` 继承的 `AbstractPasswordEncoder` 在 Spring Security 7 中要求实现 `encodedNonNullPassword(CharSequence, byte[])`。
- `DefaultReactiveOpaqueTokenIntrospector` 使用的 `WebClient.RequestHeadersSpec.exchange()` 已不可用，需要迁移到 Spring Framework 7 支持的响应交换 API。

处理结果：

- `Sm3PasswordEncoder` 改为实现 Spring Security 7 的 `encodedNonNullPassword(CharSequence, byte[])`。
- `DefaultReactiveOpaqueTokenIntrospector` 将 `WebClient.exchange()` 迁移为 `exchangeToMono(this::adaptToNimbusResponse)`，继续复用原有 `ClientResponse` 状态码、`Content-Type` 和响应体解析逻辑。
- `DefaultReactiveOpaqueTokenIntrospectorTest` 的测试桩同步迁移到 `exchangeToMono(...)`，保留对响应适配逻辑的覆盖。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am "-Dtest=Sm3PasswordEncoderTest,PasswordEncoderUtilTest,DefaultReactiveOpaqueTokenIntrospectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`BUILD SUCCESS`，定向测试共 14 个用例，失败 0。

## 已处理：Jackson 3 与 Boot 4 Jackson 自动配置最小迁移

`fist-support-security` 解除阻塞后，继续编译 OAuth2 Server、MyBatis CRUD、Cloud OpenFeign 等依赖链路时，新的首个阻塞点进入 `fist-boot-web-app` 的 Jackson 自动配置。

失败文件：

- `fist-kit-app/fist-web/fist-boot-web-app/src/main/java/com/power4j/fist/boot/common/jackson/JacksonConfig.java`

错误类型：

- `org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer` 不存在。
- `org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration` 不存在。

本地 Spring Boot `4.0.6` JAR 显示相关类已迁移到 Jackson 3 方向：

```text
org/springframework/boot/jackson/autoconfigure/JacksonAutoConfiguration.class
org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.class
```

处理结果：

- `fist-jackson` 的核心依赖从 `com.fasterxml.jackson.core:jackson-databind` 切换为 `tools.jackson.core:jackson-databind`，并显式保留 `com.fasterxml.jackson.core:jackson-annotations`。
- `Obfuscation`、混淆序列化器、反序列化器和注解内省器迁移到 Jackson 3 的 `tools.jackson.databind.*` API。
- `DateTimeModule`、`NumberStrModule` 迁移到 Jackson 3 内置 `tools.jackson.databind.ext.javatime.*` 与 `tools.jackson.databind.module.SimpleModule`。
- `JacksonConfig` 从 `Jackson2ObjectMapperBuilderCustomizer` 迁移到 `JsonMapperBuilderCustomizer`，使用 `JsonMapper.Builder` 注册 locale、时区、日期格式、模块和混淆注解内省器。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-infra/fist-jackson -am "-Dtest=JacksonSimpleStringObfuscateTest,SimpleStringObfuscateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `fist-boot-web-app` 依赖链主代码编译通过。
- `fist-jackson` 定向测试共 8 个用例，失败 0。

仍需注意：

- 当前只处理 Boot 4 自动配置阻塞涉及的最小 Jackson 3 路径，其他模块中的 Jackson 2 直接引用仍需继续迁移。
- 编译期间仍可见 `MappingJackson2HttpMessageConverter`、`SecurityJackson2Modules`、`OAuth2AuthorizationServerJackson2Module` 等待删除警告，后续应单独处理。

## 已处理：OAuth2 password grant 扩展迁移

Jackson 3 阻塞解除后，继续编译 OAuth2 Server，新的首个阻塞点进入 Resource Owner Password Grant 扩展。

失败模块：

```text
fist-kit-app/fist-security/fist-boot-security-oauth2-server
```

受影响文件：

- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java/com/power4j/fist/oauth2/extension/password/OAuth2ResourceOwnerPasswordAuthenticationConverter.java`
- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java/com/power4j/fist/oauth2/extension/password/OAuth2ResourceOwnerPasswordAuthenticationProvider.java`
- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java/com/power4j/fist/oauth2/extension/utils/OAuth2EndpointUtils.java`

错误类型：

- `AuthorizationGrantType.PASSWORD` 不存在。
- `OAuth2ParameterNames.USERNAME` 不存在。
- `OAuth2ParameterNames.PASSWORD` 不存在。

判断：

- Spring Security 7 / Spring Authorization Server 7 不再保留这些 password grant 便捷常量。
- 本项目已实现自定义 password grant 扩展，短期可通过自定义常量恢复编译，但需要评估是否继续保留该扩展能力。
- 该问题和用户「不再使用 Groovy」无直接关系，应作为 OAuth2 专项处理。

处理结果：

- `AuthConstants` 显式定义 `password` grant 类型值和 `username`、`password` 参数名。
- password grant converter、provider、endpoint helper 与定向测试改为复用项目内常量。
- `AuthorizationGrantType` 继续使用公开构造函数表达自定义授权类型，避免恢复 Spring Security 7 已移除的便捷常量依赖。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=OAuth2ResourceOwnerBaseAuthenticationProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests compile
```

结果：

- OAuth2 Server、MyBatis CRUD、Cloud OpenFeign 主代码编译通过。
- OAuth2 Server 定向测试共 1 个用例，失败 0。
- OAuth2 Server 编译期间仍存在 Jackson 2 相关待删除警告，需后续专项处理。

## 已处理：Gateway `HttpHeaders` 与 Reactive Authorization API 迁移

OAuth2 password grant 阻塞解除后，继续编译 Cloud Gateway ACL，新的首个阻塞点进入 Gateway Auth Core。

失败模块：

```text
fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core
```

失败文件：

```text
fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core/src/main/java/com/power4j/fist/cloud/gateway/AuthUtils.java
```

错误类型：

- `HttpHeaders.keySet()` 不存在。
- `HttpHeaders.containsKey(String)` 不存在。

判断：

- Spring Framework 7 的 `HttpHeaders` 不再暴露这些 Map 风格方法。
- `AuthUtils` 的语义只是判断目标 header 是否存在并按需移除，迁移应优先使用当前 `HttpHeaders` 支持的读取方法，不改变网关请求头清理行为。

处理结果：

- `AuthUtils` 使用 `HttpHeaders.containsHeader(String)` 判断待删除 header 是否存在，继续使用 `remove(String)` 删除 header。
- `GatewayAuthorizationManager` 从旧 `check(...)` 方法迁移到 Spring Security 7 的 `authorize(...)` 方法。
- `GatewayAuthorizationManager` 返回类型从 `Mono<AuthorizationDecision>` 调整为 `Mono<AuthorizationResult>`；`AuthorizationDecision` 仍作为具体授权结果对象复用。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests compile
```

结果：

- Gateway Auth Core、Gateway ACL、Gateway Security Reactive 主代码编译通过。
- 三个 Gateway 子模块组合编译通过。
- 编译期间仍存在部分 deprecation 警告，需后续用 `-Xlint:deprecation` 分类。

## 已处理：Gateway 测试源码 `test-compile` 迁移

Gateway 主代码编译通过后，进入 `test-compile` 暴露测试夹具迁移问题。

失败文件：

```text
fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core/src/test/java/com/power4j/fist/cloud/gateway/test/HttpBinCompatibleController.java
fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core/src/test/java/com/power4j/fist/cloud/gateway/test/PermitAllSecurityConfiguration.java
```

错误类型：

- 测试控制器使用 `Collectors.toMap(..., HttpHeaders::new)`，但 Spring Framework 7 的 `HttpHeaders` 不再满足 `Map` 供应者约束。
- 测试安全配置使用旧式 `http.authorizeExchange().anyExchange().permitAll().and().csrf().disable()` 链式 DSL，Spring Security 7 只保留接收 `Customizer` 的方法。

处理结果：

- `HttpBinCompatibleController` 改为显式创建 `HttpHeaders`，再通过 `addAll(...)` 复制 `X-Test-*` header。
- `PermitAllSecurityConfiguration` 改为 `authorizeExchange(exchange -> ...)` 和 `csrf(ServerHttpSecurity.CsrfSpec::disable)`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests test-compile
```

结果：Gateway 三个子模块组合 `test-compile` 为 `BUILD SUCCESS`。

## 已处理：MyBatis CRUD 测试数据库与 MyBatis Plus Boot 4 starter 迁移

继续执行 MyBatis CRUD `test-compile` 后，测试源码暴露 Spring Boot 4 JDBC 测试模块拆分问题；随后运行受影响测试时，又暴露 MyBatis Plus Boot 3 starter 与 Boot 4 自动配置顺序不兼容问题。

失败文件：

```text
fist-kit-app/fist-data/fist-boot-crud-mybatis/src/test/java/com/power4j/fist/boot/mybaits/crud/QueryTest.java
fist-kit-app/fist-data/fist-boot-crud-mybatis/src/test/java/com/power4j/fist/boot/mybaits/crud/UpdateTest.java
fist-kit-app/fist-data/fist-boot-crud-mybatis/src/test/java/com/power4j/fist/boot/mybaits/tree/AbstractNodeIdxSupportTest.java
```

错误类型：

- `org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase` 不存在。
- 运行 `QueryTest`、`UpdateTest`、`AbstractNodeIdxSupportTest` 时，`ApplicationContext` 无法创建 MyBatis mapper，根因栈为 `Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`。

判断：

- Spring Boot 4 将 JDBC 测试支持拆分到 `org.springframework.boot:spring-boot-jdbc-test`。
- `AutoConfigureTestDatabase` 新包名为 `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`。
- Spring Boot 4 的 `DataSourceAutoConfiguration` 已位于 `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`。
- MyBatis Plus `3.5.16` 的 `mybatis-plus-spring-boot3-starter` 仍声明旧的 `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` 自动配置顺序，导致 `MybatisPlusAutoConfiguration` 过早评估，`@ConditionalOnSingleCandidate(DataSource.class)` 看不到 `DataSource`。
- MyBatis Plus `3.5.16` 已提供 `mybatis-plus-spring-boot4-starter` 和 `mybatis-plus-spring-boot4-starter-test`，其自动配置指向 Boot 4 的 JDBC 新包。

处理结果：

- `fist-boot-crud-mybatis` 增加 `spring-boot-jdbc-test` 测试依赖。
- 3 个测试类的 `AutoConfigureTestDatabase` import 更新到新包。
- `fist-boot-crud-mybatis` 从 `mybatis-plus-spring-boot3-starter` 切换到 `mybatis-plus-spring-boot4-starter`。
- 测试依赖从 `mybatis-plus-spring-boot3-starter-test` 切换到 `mybatis-plus-spring-boot4-starter-test`。
- 根 `mybatis-spring.version` 从 `3.0.5` 升级到 `4.0.0`，与 MyBatis Plus Boot 4 starter 的 Spring 7 依赖组合一致。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests test-compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am "-Dtest=QueryTest,UpdateTest,AbstractNodeIdxSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：MyBatis CRUD `test-compile` 为 `BUILD SUCCESS`；定向运行测试共 12 个用例，失败 0。

## 已处理：OAuth2 Server Jackson 3 与 Spring Framework 7 JSON converter 迁移

MyBatis CRUD 运行期阻塞解除后，继续检查 OAuth2 Server 编译警告，确认以下 Jackson 2 相关 API 属于 Spring 7 / Authorization Server 7 迁移期高风险点：

- `MappingJackson2HttpMessageConverter` 在 Spring Framework 7 中已标记 `forRemoval=true`。
- `SecurityJackson2Modules` 仍使用 Jackson 2 类型。
- `OAuth2AuthorizationServerJackson2Module` 仍使用 Jackson 2 类型。

受影响文件：

- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java/com/power4j/fist/oauth2/extension/handler/DefaultOauthFailureHandler.java`
- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java/com/power4j/fist/oauth2/extension/service/DefaultAuthorizationService.java`

判断：

- Spring Framework 7 提供 `JacksonJsonHttpMessageConverter`，使用 Jackson 3 `JsonMapper` 读写 JSON。
- Spring Security 7 提供 `org.springframework.security.jackson.SecurityJacksonModules`。
- Spring Authorization Server 7 提供 `org.springframework.security.oauth2.server.authorization.jackson.OAuth2AuthorizationServerJacksonModule`。
- Context7 Spring Framework `7.0.5` 文档确认 `JacksonJsonHttpMessageConverter` 基于 Jackson `JsonMapper`，并依赖 `tools.jackson.core:jackson-databind`。
- Spring Security 7 文档片段仍有 Jackson 2 示例，因此该部分以本地 `spring-security-core-7.0.5.jar` 和 `spring-security-oauth2-authorization-server-7.0.5.jar` 字节码为准。

处理结果：

- `DefaultOauthFailureHandler` 从 `MappingJackson2HttpMessageConverter` 切换到 `JacksonJsonHttpMessageConverter`。
- `DefaultAuthorizationService` 的授权属性与 token metadata 序列化从 `com.fasterxml.jackson.*` 迁移到 `tools.jackson.*`。
- `DefaultAuthorizationService` 使用 `JsonMapper.builder()` 注册 `SecurityJacksonModules` 和 `OAuth2AuthorizationServerJacksonModule`，不再调用 Jackson 2 的 `ObjectMapper.registerModules(...)`。
- 新增 `DefaultAuthorizationServiceTest`，覆盖包含 Spring Security principal 的授权属性序列化，避免只验证编译通过。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,OAuth2ResourceOwnerBaseAuthenticationProviderTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
```

结果：

- OAuth2 Server 依赖链主代码编译通过。
- OAuth2 Server 定向测试共 4 个用例，失败 0。
- deprecation 明细编译为 `BUILD SUCCESS`；本轮旧 API 关键字 `MappingJackson2HttpMessageConverter`、`SecurityJackson2Modules`、`OAuth2AuthorizationServerJackson2Module`、`com.fasterxml.jackson`、`jackson2` 在日志中未命中。

## 已验证：`fist-support-spring` 包级空值注解迁移

OAuth2 Server 的 Jackson 2 待删除警告处理后，剩余 deprecation 明细主要来自 `org.springframework.lang.NonNullApi` 和 `NonNullFields`。项目已有 JSpecify 版本管理和 `@NullMarked` 先例，因此先选择编译链路中最早出现、范围较小的 `fist-support-spring` 做迁移验证。

受影响文件：

- `fist-kit-infra/fist-support-spring/pom.xml`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/i18n/package-info.java`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/spel/package-info.java`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/util/package-info.java`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/web/reactive/util/package-info.java`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/web/servlet/util/package-info.java`

处理结果：

- `fist-support-spring` 增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- 5 个 `package-info.java` 从 `@NonNullApi` + `@NonNullFields` 迁移到 `@NullMarked`。
- 该迁移只表达包级默认非空，不处理方法级 `@Nullable`，后续需要对每个模块单独检查显式可空契约。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am test
```

结果：

- `fist-support-spring` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `fist-support-spring` 模块测试共 5 个用例，失败 0。

## 已验证：`fist-support-web` 包级空值注解迁移

在 `fist-support-spring` 验证通过后，继续按相同模式处理 OAuth2 编译链路中大量出现的 `fist-support-web` 空值注解警告。

受影响文件：

- `fist-kit-app/fist-web/fist-support-web/pom.xml`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/common/api/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/common/error/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/common/jackson/module/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/common/matcher/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/mon/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/mon/event/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/mon/info/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/mon/listener/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/event/error/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/reactive/log/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/servlet/package-info.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/servlet/mvc/formatter/package-info.java`

处理结果：

- `fist-support-web` 增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- 12 个 `package-info.java` 从 `@NonNullApi` + `@NonNullFields` 迁移到 `@NullMarked`。
- 当前未扩展到方法级 `@Nullable` 迁移，后续仍需在各模块迁移时检查可空入参、返回值和泛型类型参数。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am test
```

结果：

- `fist-support-web` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `fist-support-web` 模块测试共 4 个用例，失败 0。
- 本次 `test` 未实际运行已记录的 `PathMatcherTest.groovy`，后续去 Groovy 任务需要单独确认该测试是否仍被当前构建链路纳入。

## 已验证：Security 模块包级空值注解迁移

继续处理 Security 相关模块的 `NonNullApi` / `NonNullFields` 警告，迁移范围限定在包级默认非空注解，不扩大到方法级可空契约。

受影响模块：

- `fist-kit-app/fist-security/fist-support-security`
- `fist-kit-app/fist-security/fist-boot-security-oauth2-server`

处理结果：

- 两个模块分别增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- `fist-support-security` 的 11 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-boot-security-oauth2-server` 的 12 个 `package-info.java` 迁移到 `@NullMarked`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security,fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,OAuth2ResourceOwnerBaseAuthenticationProviderTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `fist-support-security` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- OAuth2 Server deprecation 明细编译为 `BUILD SUCCESS`；剩余 `NonNullApi` / `NonNullFields` 命中来自依赖链上的 `fist-boot-web-app` 与 `fist-support-data`，不是 OAuth2 Server 本模块。
- `fist-support-security` 模块测试共 17 个用例，失败 0。
- OAuth2 Server 定向测试共 4 个用例，失败 0。

## 已验证：Web App 与 Support Data 包级空值注解迁移

Security 模块迁移后，OAuth2 Server 依赖链中剩余的 `NonNullApi` / `NonNullFields` 警告来自 `fist-boot-web-app` 与 `fist-support-data`。

受影响模块：

- `fist-kit-app/fist-web/fist-boot-web-app`
- `fist-kit-app/fist-data/fist-support-data`

处理结果：

- 两个模块分别增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- `fist-boot-web-app` 的 1 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-support-data` 的 11 个 `package-info.java` 迁移到 `@NullMarked`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
```

结果：

- `fist-boot-web-app` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；模块自身没有测试源码。
- `fist-support-data` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `fist-support-data` 模块测试共 6 个用例，失败 0。
- OAuth2 Server 依赖链 deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- 本次 `fist-support-data -am test` 未实际运行已记录的 Groovy 测试文件，后续去 Groovy 任务需要单独确认这些测试是否仍被当前构建链路纳入。

## 已验证：Web App 方法级空值注解迁移

Cloud RPC Feign 方法级迁移后，继续处理依赖链中反复出现的 Web App 显式可空契约。

受影响模块：

- `fist-kit-app/fist-web/fist-boot-web-app`

处理结果：

- 2 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- `JacksonCustomizeProperties` 的可选配置属性字段迁移到类型使用位置。
- `MessageConfiguration` 的可选注入字段保留可空契约，setter 参数继续允许 `null`。
- `fist-boot-web-app` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app/fist-web/fist-boot-web-app/src/main/java -g "*.java" -S
```

结果：

- `fist-boot-web-app` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 剩余 deprecation 明细为既有 `Breaker` 和 Apache Commons Lang `ObjectUtils.defaultIfNull(...)`，不属于本轮空值注解迁移。
- `fist-boot-web-app -am test` 为 `BUILD SUCCESS`；模块自身没有测试源码；依赖链 `fist-support-web` 测试共 4 个用例，`fist-support-security` 测试共 17 个用例，失败 0。

## 已验证：Boot Data 与 MyBatis CRUD 包级空值注解迁移

全仓扫描后，数据链路剩余包级 Spring 空值注解主要位于 `fist-boot-data` 与 `fist-boot-crud-mybatis`。

受影响模块：

- `fist-kit-app/fist-data/fist-boot-data`
- `fist-kit-app/fist-data/fist-boot-crud-mybatis`

处理结果：

- 两个模块分别增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- `fist-boot-data` 的 4 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-boot-crud-mybatis` 的 8 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解；`PageParameter`、`BaseRepository`、`Repository` 中的方法级 `org.springframework.lang.Nullable` deprecation 作为显式可空契约专项保留。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis,fist-kit-app/fist-data/fist-boot-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am "-Dtest=QueryTest,UpdateTest,AbstractNodeIdxSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am test
```

结果：

- `fist-boot-data` 与 `fist-boot-crud-mybatis` deprecation 明细编译均为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `fist-boot-data -am test` 为 `BUILD SUCCESS`；`fist-boot-data` 模块自身测试共 2 个用例，失败 0。
- MyBatis CRUD 定向测试共 12 个用例，失败 0。
- `fist-boot-crud-mybatis -am test` 为 `BUILD SUCCESS`；`fist-boot-crud-mybatis` 模块自身测试共 14 个用例，失败 0。

## 已验证：Gateway 包级空值注解迁移

数据链路处理后，继续迁移已完成 Boot 4 编译和测试源码适配的 Gateway 模块。

受影响模块：

- `fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core`
- `fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl`

处理结果：

- 两个模块分别增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- `fist-gateway-auth-core` 的 5 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-cloud-gateway-acl` 的 2 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解；`AuthContext`、`AuthState` 中的方法级 `org.springframework.lang.Nullable` deprecation 作为显式可空契约专项保留。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests test-compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am test
```

结果：

- Gateway Auth Core 与 Gateway ACL deprecation 明细编译均为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- Gateway 三个子模块组合 `test-compile` 为 `BUILD SUCCESS`。
- Gateway Auth Core 模块测试共 14 个用例，失败 0。
- Gateway ACL 模块测试共 6 个用例，失败 0。

## 已验证：Boot Security 包级空值注解迁移

Gateway 处理后，继续迁移 `fist-boot-security` 中剩余的包级 Spring 空值注解。

受影响模块：

- `fist-kit-app/fist-security/fist-boot-security`

处理结果：

- `fist-boot-security` 增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- 2 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解；`UserConverter` 中的方法级 `org.springframework.lang.Nullable` deprecation 后续按显式可空契约专项处理。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am test
```

结果：

- `fist-boot-security` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `fist-boot-security -am test` 为 `BUILD SUCCESS`；`fist-boot-security` 模块自身没有测试源码，依赖链中 `fist-support-security` 测试共 17 个用例，失败 0。

## 已验证：Boot Security 方法级空值注解迁移

`fist-support-spring` 方法级迁移验证后，继续处理 `fist-boot-security` 中单个公共接口可空返回值。

受影响文件：

- `fist-kit-app/fist-security/fist-boot-security/src/main/java/com/power4j/fist/security/core/authentication/UserConverter.java`

处理结果：

- `UserConverter` 从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- `convert(Authentication)` 的返回值改为 `@Nullable U`，保留「无用户信息返回 `null`」的接口契约。
- `fist-boot-security` 主代码复扫未命中 `org.springframework.lang.Nullable`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am test
rg -n "org\.springframework\.lang\.Nullable" fist-kit-app/fist-security/fist-boot-security/src/main/java -g "*.java" -S
```

结果：

- `fist-boot-security` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`。
- `fist-boot-security -am test` 为 `BUILD SUCCESS`；依赖链测试共 40 个用例，失败 0，`fist-boot-security` 模块自身没有测试源码。
- 编译日志中仍有依赖链上的 `fist-support-web` 与 `fist-support-security` 方法级 `org.springframework.lang.Nullable` 警告，后续按模块继续迁移。

## 已验证：Support Security 方法级空值注解迁移

Boot Security 方法级迁移后，继续处理依赖链中剩余命中较多的 `fist-support-security`。

受影响模块：

- `fist-kit-app/fist-security/fist-support-security`

处理结果：

- 13 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- 字段可空契约改为类型使用位置，例如 `private @Nullable String id`、`protected final @Nullable ServerAuthFilter<C> currentFilter`。
- 方法返回值改为类型使用位置，例如 `public @Nullable String resolve(...)`、`protected @Nullable UserInfo extractFromMap(...)`、`default <A> @Nullable A getAttribute(...)`。
- `final` 参数使用 `final @Nullable Type` 写法，避免继续沿用旧声明注解位置。
- `fist-support-security` 主代码复扫未命中 `org.springframework.lang.Nullable`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am test
rg -n "org\.springframework\.lang\.Nullable" fist-kit-app/fist-security/fist-support-security/src/main/java -g "*.java" -S
```

结果：

- `fist-support-security` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`。
- `fist-support-security -am test` 为 `BUILD SUCCESS`；模块自身测试共 17 个用例，失败 0；依赖链测试共 40 个用例，失败 0。
- 编译日志中仍有依赖链上的 `fist-support-web` 方法级 `org.springframework.lang.Nullable` 警告，后续按 Web 支撑模块继续迁移。

## 已验证：Support Web 方法级空值注解迁移

Support Security 方法级迁移后，继续处理依赖链中剩余的 `fist-support-web` 方法级 Spring 空值注解。

受影响模块：

- `fist-kit-app/fist-web/fist-support-web`

处理结果：

- 20 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- DTO 和事件对象字段迁移到类型使用位置，例如 `private @Nullable String query`、`private @Nullable ExceptionInfo error`。
- 方法返回值迁移到类型使用位置，例如 `@Nullable Locale resolve()`、`public @Nullable String getHint()`、`private @Nullable String getDescription(...)`。
- `Breaker` 中可空数组参数从 `@Nullable Object[]` 调整为 `Object @Nullable []`，表达数组本身可为 `null`。
- `fist-support-web` 主代码复扫未命中 `org.springframework.lang.Nullable`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am test
rg -n "org\.springframework\.lang\.Nullable" fist-kit-app/fist-web/fist-support-web/src/main/java -g "*.java" -S
```

结果：

- `fist-support-web` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 剩余 deprecation 明细为 `Breaker` 自身缺少 `@Deprecated` 标注和 Apache Commons Lang `ObjectUtils.defaultIfNull(...)`，不属于本轮空值注解迁移。
- `fist-support-web -am test` 为 `BUILD SUCCESS`；模块自身 Java 测试共 4 个用例，失败 0。
- 本次 `test` 仍未实际运行已记录的 `PathMatcherTest.groovy`，后续去 Groovy 任务需要单独处理。

## 已验证：Support Data 方法级空值注解迁移

Support Web 方法级迁移后，继续处理数据支撑模块中剩余的显式可空契约。

受影响模块：

- `fist-kit-app/fist-data/fist-support-data`

处理结果：

- 10 个主代码 Java 文件和 2 个测试夹具文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- 字段和返回值迁移到类型使用位置，例如 `private @Nullable List<HandlerInfo<T>> entry`、`public @Nullable OpHandler<T> getHandler()`。
- 泛型容器显式表达元素可空，例如 `TransmittableThreadLocal<@Nullable String>`。
- `final` 参数使用 `final @Nullable Type` 写法，保留原有可传入 `null` 的租户和错误处理器契约。
- `fist-support-data` Java 源码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app/fist-data/fist-support-data/src -g "*.java" -S
```

结果：

- `fist-support-data` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 剩余 deprecation 明细为 Apache Commons Lang `Validate.notNull(...)`、`MutableObject.getValue()`、`ObjectUtils.defaultIfNull(...)` 和既有 `ValidateUtil` 注解问题，不属于本轮空值注解迁移。
- `fist-support-data -am test` 为 `BUILD SUCCESS`；模块自身 Java 测试共 6 个用例，失败 0。
- 本次 `test` 仍未实际运行已记录的 3 个 `.groovy` 测试文件，后续去 Groovy 任务需要单独处理。

## 已验证：Boot Data 方法级空值注解迁移

Support Data 方法级迁移后，继续处理 `fist-boot-data` 中 Spring MVC 分页参数解析相关的显式可空契约。

受影响模块：

- `fist-kit-app/fist-data/fist-boot-data`

处理结果：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- Spring MVC 参数解析器覆写方法的返回值改为类型使用位置，例如 `public @Nullable Object resolveArgument(...)`。
- 分页参数模型字段迁移到类型使用位置，例如 `private @Nullable String field` 与 `private @Nullable String order`。
- `fist-boot-data` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app/fist-data/fist-boot-data/src/main/java -g "*.java" -S
```

结果：

- `fist-boot-data` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 剩余 deprecation 明细为既有 `InTenant` deprecation、Apache Commons Lang `ObjectUtils.defaultIfNull(...)` / `StringUtils.equalsIgnoreCase(...)` 和 `PageData` unchecked 使用，不属于本轮空值注解迁移。
- `fist-boot-data -am test` 为 `BUILD SUCCESS`；模块自身测试共 2 个用例，失败 0；依赖链 `fist-support-data` 测试共 6 个用例，失败 0。

## 已验证：MyBatis CRUD 方法级空值注解迁移

Redisson 方法级迁移后，继续处理 MyBatis CRUD 模块中的显式可空契约。该模块包含较多公共 Repository 接口，迁移时保留「查询条件可为空」「排除 ID 可为空」「数据库字段值可为空」等原有语义。

受影响模块：

- `fist-kit-app/fist-data/fist-boot-crud-mybatis`

处理结果：

- 7 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- 自动配置和实体字段迁移到类型使用位置，例如 `private @Nullable ApplicationContext applicationContext`、`private @Nullable LocalDateTime delFlag`。
- Repository 内部返回值迁移到类型使用位置，例如 `@Nullable Object loadVersionValueById(...)` 与 `@Nullable Object getFieldValue(...)`。
- `Eq` 的可空匹配值字段迁移到 `private final @Nullable Object value`。
- `fist-boot-crud-mybatis` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app/fist-data/fist-boot-crud-mybatis/src/main/java -g "*.java" -S
```

结果：

- `fist-boot-crud-mybatis` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 剩余 deprecation 明细为既有 `AuditFiller` 注解问题，不属于本轮空值注解迁移。
- `fist-boot-crud-mybatis -am test` 为 `BUILD SUCCESS`；模块自身测试共 14 个用例，失败 0。

## 已验证：Cloud RPC Feign 包级空值注解迁移

Boot Security 处理后，继续迁移 `fist-cloud-rpc-feign` 中剩余的包级 Spring 空值注解。

受影响模块：

- `fist-kit-cloud/fist-cloud-rpc-feign`

处理结果：

- `fist-cloud-rpc-feign` 增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- 3 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解；Feign error handler 与 relay handler 中的方法级 `org.springframework.lang.Nullable` deprecation 作为显式可空契约专项保留。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests test-compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am test
```

结果：

- Cloud RPC Feign deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `test-compile` 为 `BUILD SUCCESS`。
- `fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`；模块自身测试共 7 个用例，失败 0。

## 已验证：Cloud RPC Feign 方法级空值注解迁移

Boot Data 方法级迁移后，继续处理 Feign 模块中请求透传和错误处理器相关的显式可空契约。

受影响模块：

- `fist-kit-cloud/fist-cloud-rpc-feign`

处理结果：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- `RelayHandler`、`HeaderRelayHandler`、`UserRelayHandler` 保留 `HttpServletRequest` 可为 `null` 的接口契约。
- `FeignErrorHandler` 的可选 `MessageSourceAccessor` 与 `LocaleResolver` 字段迁移到类型使用位置。
- `fist-cloud-rpc-feign` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-cloud/fist-cloud-rpc-feign/src/main/java -g "*.java" -S
```

结果：

- `fist-cloud-rpc-feign` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 编译日志中仍有依赖链上的 `fist-boot-web-app` 方法级 `org.springframework.lang.Nullable` 警告，后续按 Web App 模块单独迁移。
- `fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`；模块自身测试共 7 个用例，失败 0。
- 测试编译仍提示 `SecureFeignEncoderPrototypeTest` 使用或覆盖过时 API；这是后续 Feign/SDE 专项的输入，不属于本轮空值注解迁移。

## 已验证：API Doc 包级空值注解迁移

Cloud RPC Feign 处理后，继续迁移 `fist-boot-apidoc` 中剩余的包级 Spring 空值注解。

受影响模块：

- `fist-kit-app/fist-boot-apidoc`

处理结果：

- `fist-boot-apidoc` 增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- 3 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解；方法级 `org.springframework.lang.Nullable`、Jackson 2 `JsonNode.fields()` 和 Springfox 退役相关警告作为 API Doc 专项保留。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am test
```

结果：

- API Doc deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `fist-boot-apidoc -am test` 为 `BUILD SUCCESS`；`fist-boot-apidoc` 模块自身没有测试源码。

## 已验证：API Doc 方法级空值注解迁移

MyBatis CRUD 方法级迁移后，继续处理 API Doc 模块中的显式可空契约。

受影响模块：

- `fist-kit-app/fist-boot-apidoc`

处理结果：

- 2 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- `DocParser` 保留 JSON 输入可为 `null` 的内部解析契约。
- `HandlerMethodResource` 的可选参数类型列表迁移到 `private @Nullable List<Class<?>> parameterTypes`。
- `fist-boot-apidoc` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app/fist-boot-apidoc/src/main/java -g "*.java" -S
```

结果：

- `fist-boot-apidoc` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 剩余 deprecation 明细为 Jackson 2 `JsonNode.fields()`、Apache Commons Lang `Validate.notNull(...)` 和 Springfox 退役相关注解问题，后续按 API Doc 专项处理。
- `fist-boot-apidoc -am test` 为 `BUILD SUCCESS`；模块自身没有测试源码；依赖链测试共 6 个用例，失败 0。

## 已验证：OAuth2 Server 方法级空值注解迁移

API Doc 方法级迁移后，继续处理 OAuth2 Server 中授权服务、token 生成器和认证转换器相关的显式可空契约。

受影响模块：

- `fist-kit-app/fist-security/fist-boot-security-oauth2-server`

处理结果：

- 5 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- 授权查询和 token 生成返回值迁移到类型使用位置，例如 `public @Nullable OAuth2Authorization findById(...)`、`public @Nullable OAuth2AccessToken generate(...)`。
- `OAuth2RefreshTokenAuthenticationConverter` 的 `convert(...)` 返回值迁移到类型使用位置，保留非 refresh token 请求返回 `null` 的接口契约。
- `OAuth2ResourceOwnerBaseAuthenticationToken` 和 `WebUtil` 保留可空入参契约。
- `fist-boot-security-oauth2-server` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,OAuth2ResourceOwnerBaseAuthenticationProviderTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java -g "*.java" -S
```

结果：

- `fist-boot-security-oauth2-server` deprecation 明细编译为 `BUILD SUCCESS`；本模块主代码未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 定向测试共 4 个用例，失败 0。
- 剩余 deprecation 明细主要为 `DefaultAuthorizationService` 使用 Spring JDBC LOB API，以及 Apache Commons Lang 过时方法，不属于本轮空值注解迁移。

## 已验证：Gateway Auth Core 方法级空值注解迁移

OAuth2 Server 方法级迁移后，继续处理 Gateway Auth Core 中代理解析、鉴权上下文和过滤器链相关的显式可空契约。

受影响模块：

- `fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core`

处理结果：

- 11 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- `RouteTargetResolver` 和 `DefaultRouteTargetResolver` 的可空返回值迁移到类型使用位置，例如 `@Nullable RouteTarget resolve(...)` 与 `public @Nullable RouteTarget resolve(...)`。
- `AuthContext`、`AuthProblem`、`AuthState` 和 `DefaultGatewayAuthFilterChain` 的可选字段迁移到类型使用位置。
- `AuthUtils`、`ProxyResolver`、`TenantFilter`、`UpstreamLocateFilter` 保留可空入参契约。
- `fist-gateway-auth-core` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core/src/main/java -g "*.java" -S
```

结果：

- `fist-gateway-auth-core` deprecation 明细编译为 `BUILD SUCCESS`；本模块主代码未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- `fist-gateway-auth-core -am test` 为 `BUILD SUCCESS`；模块自身测试共 14 个用例，失败 0。
- 剩余 deprecation 明细为依赖链上的 `Breaker`、Apache Commons Lang 过时方法，以及本模块 `SkipAuthorizationFilter` 中 `Validate.notNull(...)`，不属于本轮空值注解迁移。
- 全仓主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`；当前唯一剩余命中是 `fist-cloud-gateway-acl` 的测试文件 `ApiGuardFilterTest.java`。

## 已验证：Gateway ACL 测试空值注解迁移

Gateway Auth Core 主代码迁移后，继续处理全仓最后一个 Spring 空值注解命中。

受影响模块：

- `fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl`

处理结果：

- `ApiGuardFilterTest.java` 从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- `makeAuthContext(...)` 测试辅助方法保留 principal 可为 `null` 的语义，用于构造未登录上下文。
- 全仓 `*.java` 复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app fist-kit-cloud fist-kit-infra examples benchmarks -g "*.java" -S
```

结果：

- `fist-cloud-gateway-acl` deprecation 明细编译为 `BUILD SUCCESS`。
- `fist-cloud-gateway-acl -am test` 为 `BUILD SUCCESS`；模块自身测试共 6 个用例，失败 0。
- 全仓 Spring nullability 注解迁移已清零，后续 deprecation 分类可以转向 Apache Commons Lang、Spring JDBC LOB API、Springfox 和 Redisson 等非空值注解问题。

## 已验证：Gateway 与 OAuth2 Commons Lang 过时 API 收敛

Spring nullability 注解清零后，继续处理 Gateway、Security 和 OAuth2 编译日志中已经暴露的 Apache Commons Lang `3.20.0` 过时 API。该轮只处理替代关系明确、行为可等价保持的调用，不处理 Spring JDBC LOB API 等设计性迁移。

受影响模块：

- `fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core`
- `fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive`
- `fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl`
- `fist-kit-app/fist-security/fist-boot-security-oauth2-server`

处理依据：

- Context7 `/apache/commons-lang` 文档确认 Commons Lang 提供 null-safe 字符串与对象工具。
- 本地 `commons-lang3-3.20.0-sources.jar` 显示：
  - `Validate.notNull(T)` 建议使用 `Objects.requireNonNull(T)`。
  - `StringUtils.startsWith(...)` 建议使用 `Strings.CS.startsWith(...)`。
  - `StringUtils.equals(...)` 建议使用 `Strings.CS.equals(...)`。
  - `ObjectUtils.defaultIfNull(...)` 建议使用 `ObjectUtils.getIfNull(...)`。

处理结果：

- Gateway 鉴权决策中的 `Validate.notNull(...)` 迁移到 `Objects.requireNonNull(...)`。
- Gateway ACL OAuth2 配置中的 `StringUtils.startsWith(...)` 迁移到 `Strings.CS.startsWith(...)`。
- OAuth2 secure parameter filter 中的 `StringUtils.equals(...)` 迁移到 `Strings.CS.equals(...)`。
- OAuth2 Resource Owner provider 中的 `ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`。
- `DefaultAuthorizationService` 移除继承 `org.apache.commons.lang3.StringUtils` 的内部 `StrUtil`，避免触发 Commons Lang 构造器 deprecation；字符串拆分和拼接改为直接调用静态方法。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl,fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway-acl,fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway-acl -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=OAuth2ResourceOwnerBaseAuthenticationProviderTest,DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
rg -n "Validate\.notNull\(|StringUtils\.startsWith\(|StringUtils\.equals\(|ObjectUtils\.defaultIfNull\(|extends org\.apache\.commons\.lang3\.StringUtils" fist-kit-cloud/fist-cloud-gateway fist-kit-app/fist-security/fist-boot-security-oauth2-server -g "*.java" -S
```

结果：

- Gateway 与 OAuth2 组合 deprecation 明细编译为 `BUILD SUCCESS`。
- 本轮目标调用复扫未命中。
- Gateway 三个模块组合测试为 `BUILD SUCCESS`，Gateway Auth Core 模块自身测试共 14 个用例、Gateway ACL 模块自身测试共 6 个用例，失败 0。
- OAuth2 Server 定向测试共 4 个用例，失败 0。
- OAuth2 Server 剩余 deprecation 明细主要为 `DefaultAuthorizationService` 使用 Spring JDBC LOB API；其他 Commons Lang 过时调用来自 Web/Data 等依赖链模块，后续按模块继续处理。

## 已验证：Redisson 包级空值注解迁移

API Doc 处理后，继续迁移 `fist-redisson` 中剩余的包级 Spring 空值注解。

受影响模块：

- `fist-kit-infra/fist-redisson`

处理结果：

- `fist-redisson` 增加 `org.jspecify:jspecify` 依赖，版本继续由根 POM 管理。
- 5 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解；Redisson bean processor、publisher、queue writer 中的方法级 `org.springframework.lang.Nullable` 以及 `RBlockingQueue.subscribeOnElements` deprecation 作为后续专项保留。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-redisson spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am test
```

结果：

- Redisson deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。
- `fist-redisson -am test` 为 `BUILD SUCCESS`；`fist-redisson` 模块自身没有测试源码。
- 全仓 `package-info.java` 复扫未命中 `NonNullApi` / `NonNullFields`。

## 已验证：Redisson 方法级空值注解迁移

Web App 方法级迁移后，继续处理 Redisson 模块的显式可空契约。

受影响模块：

- `fist-kit-infra/fist-redisson`

处理结果：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- 可选 `Codec` 字段迁移到类型使用位置，例如 `private final @Nullable Codec codec`。
- `BeanPostProcessor` 覆写方法返回值迁移到类型使用位置，例如 `public @Nullable Object postProcessAfterInitialization(...)`。
- `fist-redisson` 主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-redisson spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-infra/fist-redisson/src/main/java -g "*.java" -S
```

结果：

- `fist-redisson` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- 剩余 deprecation 明细为 Redisson `RBlockingQueue.subscribeOnElements(...)`，不属于本轮空值注解迁移。
- `fist-redisson -am test` 为 `BUILD SUCCESS`；模块自身没有测试源码；依赖链测试共 11 个用例，失败 0。

## 已验证：Support Spring 方法级空值注解迁移

全仓包级 `NonNullApi` / `NonNullFields` 迁移完成后，进入方法级 `org.springframework.lang.Nullable` 到 JSpecify `@Nullable` 的显式可空契约迁移。第一批选择 `fist-support-spring`，用于验证返回值、入参、数组和泛型返回值的写法。

受影响模块：

- `fist-kit-infra/fist-support-spring`

受影响文件：

- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/i18n/MsgUtil.java`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/spel/SpringElUtil.java`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/util/ApplicationContextHolder.java`
- `fist-kit-infra/fist-support-spring/src/main/java/com/power4j/fist/support/spring/web/servlet/util/HttpServletRequestUtil.java`

处理结果：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 `org.jspecify.annotations.Nullable`。
- 方法返回值改为类型使用位置，例如 `public @Nullable ApplicationContext peek()` 与 `public <T> @Nullable T eval(...)`。
- `MsgUtil` 的消息参数从 Spring 旧写法 `@Nullable Object[]` 调整为 JSpecify 数组写法 `Object @Nullable []`，表示数组本身可为 `null`，避免误写为「数组元素可空」。
- `fist-support-spring` 主代码复扫未命中 `org.springframework.lang.Nullable`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am test
rg -n "org\.springframework\.lang\.Nullable" fist-kit-infra/fist-support-spring/src/main/java -g "*.java" -S
```

结果：

- `fist-support-spring` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- `fist-support-spring -am test` 为 `BUILD SUCCESS`；模块测试共 5 个用例，失败 0。
- `rg` 复扫 `fist-support-spring` 主代码未命中 `org.springframework.lang.Nullable`。

## Jackson 3

扫描命令：

```powershell
rg -l "com\.fasterxml\.jackson" fist-kit-app fist-kit-cloud fist-kit-infra examples benchmarks -g "*.java"
```

结果：40 个 Java 文件。

重点模块：

- `fist-kit-infra/fist-jackson`
- `fist-kit-infra/fist-sde`
- `fist-kit-app/fist-web`
- `fist-kit-app/fist-security`
- `fist-kit-app/fist-data`
- `fist-kit-cloud/fist-cloud-rpc-feign`

处理建议：

- 不先做全仓包名替换。
- 先迁移 `fist-jackson` 和 SDE JSON 编解码相关类。
- 对自定义 `JsonSerializer`、`JsonDeserializer`、`AnnotationIntrospector`、`ContextualSerializer`、`ContextualDeserializer` 建立专项验证。
- 使用 OpenRewrite 的 Jackson 迁移补丁作为参考，但以编译错误和测试结果为准。

## JSpecify 与空值注解

扫描命令：

```powershell
rg -l "org\.springframework\.lang\.Nullable" fist-kit-app fist-kit-cloud fist-kit-infra examples benchmarks -g "*.java"
```

初始结果：89 个 Java 文件。包级 `NonNullApi` / `NonNullFields` 已完成全仓迁移；方法级 `org.springframework.lang.Nullable` 已开始按模块迁移。

处理建议：

- 与项目已有 `@NullMarked` 和 `org.jspecify.annotations.Nullable` 策略统一处理。
- 公共 API 包优先补齐 `package-info.java`。
- 不做单纯机械替换，避免泛型可空语义丢失。
- 数组类型需要区分数组本身可空与元素可空。`Object @Nullable []` 表示数组可为 `null`；`@Nullable Object[]` 在 JSpecify 语义下表示数组元素可空。

## Springfox 与 API Doc

命中位置：

- `fist-kit-app/fist-boot-apidoc/pom.xml`
- `fist-kit-app/fist-boot-apidoc/src/main/java/com/power4j/fist/boot/apidoc/swagger/plugin/ApiTraitOperationPlugin.java`
- `fist-kit-app/fist-boot-apidoc/src/main/java/com/power4j/fist/boot/autoconfigure/apidoc/SwaggerDocAutoConfiguration.java`

阶段 2 中 `fist-boot-apidoc` 主代码可编译，但仍有过时 API 警告。OpenRewrite dry-run 倾向删除 Springfox 依赖。

处理建议：

- 明确 Boot 4 目标是否只保留 SpringDoc 3。
- 若删除 Springfox，需要同步删除插件类和自动配置分支。
- API 文档模块应单独验证用户可见行为。

## Feign 与 SDE

扫描命令：

```powershell
rg -l "@FeignClient|feign\." fist-kit-app fist-kit-cloud fist-kit-infra examples benchmarks -g "*.java"
```

结果：26 个 Java 文件。

重点模块：

- `fist-kit-cloud/fist-cloud-rpc-feign`
- `fist-kit-infra/fist-sde/fist-sde-client`
- `examples/fist-sde/example-feign`

处理建议：

- 短期先验证 Spring Cloud OpenFeign 5.x 是否能编译和运行。
- SDE Feign 的 `Encoder`、`Decoder`、请求模板和响应体重建逻辑不能自动迁移。
- 迁移到 Spring HTTP Service Clients 前，需要单独设计 SDE 等价适配层。

## Groovy / Spock

当前 `.groovy` 测试文件共 5 个：

- `fist-kit-app/fist-web/fist-support-web/src/test/java/com/power4j/fist/boot/common/matcher/PathMatcherTest.groovy`
- `fist-kit-app/fist-security/fist-support-security/src/test/java/com/power4j/fist/boot/security/inner/DefaultUserCodecTest.groovy`
- `fist-kit-app/fist-data/fist-support-data/src/test/java/com/power4j/fist/data/tree/NodeIdxTreeMakerTest.groovy`
- `fist-kit-app/fist-data/fist-support-data/src/test/java/com/power4j/fist/data/tree/AdjacencyTreeMakerTest.groovy`
- `fist-kit-app/fist-data/fist-support-data/src/test/java/com/power4j/fist/boot/common/op/OpTemplateTest.groovy`

构建链路命中：

- 根 `pom.xml` 中 `gmavenplus-plugin`。
- 根 `pom.xml` 中 `spock-lib.version`。
- `fist-kit-build/pom.xml` 中 Groovy 和 Spock BOM 管理。
- `fist-kit-infra/fist-support-test/pom.xml` 中 `spock-spring` 和 `groovy-tables`。

处理建议：先将 5 个测试迁移到 Java/JUnit 5，再删除 Groovy、Spock 和 gmavenplus。

## Boot starter 与自动配置注册

starter 仍有旧坐标需要后续处理：

- `spring-boot-starter-web`
- `spring-boot-starter-test`
- `spring-boot-starter-json`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`

自动配置注册文件共 16 个，包含 `spring.factories` 与 `AutoConfiguration.imports` 并存的模块：

- API Doc
- Data
- Security
- Web App
- Cloud RPC Feign
- Redisson
- SDE boot starter

处理建议：

- starter 坐标迁移优先使用 OpenRewrite dry-run 结果辅助拆分。
- 自动配置注册不能只按文件存在性判断，需要配合启动测试验证。

## 当前未命中的风险

| 项目 | 扫描结果 | 判断 |
| --- | --- | --- |
| `@MockBean` / `@SpyBean` | 未命中 | 测试注解迁移风险较低 |
| `org.springframework.security.jwt` | 未命中 | `spring-security-jwt` 主要是 BOM 暴露和下游兼容问题 |
| Jakarta `javax.*` | 仅命中 `javax.crypto` | 当前不是 Jakarta EE 迁移风险 |

## 下一步

1. 对 OAuth2 `DefaultAuthorizationService` 的 Spring JDBC LOB API deprecation 做专项评估，确认 Spring Framework 7 的推荐替代方案和数据库兼容影响。
2. 继续按模块收敛 Web/Data 中的 Apache Commons Lang 过时 API。
3. 检查 MyBatis Plus Boot 4 starter 是否对下游公开依赖坐标产生发布说明影响。
4. 继续推进 Feign/SDE 与 Spring 官方 Feign / HTTP Service Clients 的专项评估。

## 本地证据

- 阶段 2 试编译日志：`.codex/trial-compile-*.log`
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-webflux\4.0.6\spring-boot-webflux-4.0.6.jar`
- Spring Boot 官方发布记录：`https://spring.io/blog/2026/04/23/spring-boot-4-0-6-available-now`
- Spring Boot Context7 文档：`/spring-projects/spring-boot`，`DataSourceAutoConfiguration` 位于 `org.springframework.boot.jdbc.autoconfigure`
- Spring Framework Context7 文档：`/spring-projects/spring-framework/v7.0.5`，`JacksonJsonHttpMessageConverter` 使用 Jackson `JsonMapper`
- MyBatis Plus `3.5.16` 本地 JAR：`F:\pkg-maven\repository\com\baomidou\mybatis-plus-spring-boot4-starter\3.5.16\mybatis-plus-spring-boot4-starter-3.5.16.jar`
- Spring Security `7.0.5` 本地 JAR：`F:\pkg-maven\repository\org\springframework\security\spring-security-core\7.0.5\spring-security-core-7.0.5.jar`
- Spring Authorization Server `7.0.5` 本地 JAR：`F:\pkg-maven\repository\org\springframework\security\spring-security-oauth2-authorization-server\7.0.5\spring-security-oauth2-authorization-server-7.0.5.jar`
- JSpecify Context7 文档：`/jspecify/jspecify`，确认 `@Nullable` 可用于返回值、参数、泛型类型参数和数组维度。
- OpenRewrite dry-run patch：`target/rewrite/rewrite.patch`
