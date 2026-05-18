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
- Groovy 测试文件共 5 个，已迁移到 Java/JUnit 5，并删除 Groovy/Spock 构建链路。
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

下游影响：

- `fist-boot-crud-mybatis` 的传递依赖从 `mybatis-plus-spring-boot3-starter` 切换为 `mybatis-plus-spring-boot4-starter`，测试依赖同步切换为 `mybatis-plus-spring-boot4-starter-test`。
- 下游若显式排除、覆盖或直接依赖 MyBatis Plus Boot starter，需要同步使用 Boot 4 starter 坐标。
- `mybatis-spring.version` 升级到 `4.0.0`，下游若依赖旧版 `mybatis-spring` 3.x，需要跟随 Spring Framework 7 组合升级。
- Context7 `/baomidou/mybatis-plus-doc` 文档确认 Spring Boot 4 及以上版本使用 `mybatis-plus-spring-boot4-starter`，该 starter 自 MyBatis Plus `3.5.13` 开始提供。
- 依赖树验证显示当前模块解析到 `mybatis-plus-spring-boot4-starter:3.5.16`、`mybatis-plus-spring-boot4-starter-test:3.5.16` 和 `mybatis-spring:4.0.0`，未解析到 Boot 3 starter。

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

处理决策：

- Boot 4 目标只保留 SpringDoc 3，移除 Springfox 支持。
- 删除 `springfox-oas` 可选依赖、Springfox 插件类、Springfox 自动配置类，以及自动配置注册文件中的 Springfox 入口。
- API 文档模块保留 SpringDoc 主路径，`ApiTraitOperationCustomizer` 继续负责写入 `ApiTrait` 扩展信息。

当前进展：

- `DocParser` 中的 Jackson 2 `JsonNode.fields()` 已迁移到 `JsonNode.properties()`。
- `HandlerMethodResource` 和 `RestResource` 中的 `Validate.notNull(T)` 已迁移到 `Objects.requireNonNull(T)`。
- Springfox 插件和自动配置分支已删除；当前 API Doc 只保留 SpringDoc 3 扩展路径。

处理依据：

- 本地 `jackson-databind-2.21.3.jar` 的 `javap` 结果确认 `JsonNode` 同时提供 `fields()` 和 `properties()`。
- 本地 `tools.jackson.core:jackson-databind:3.1.2` 的 `javap` 结果确认 Jackson 3 `JsonNode` 保留 `properties()`，不再提供 `fields()`。
- Commons Lang 迁移沿用前序批次规则：`Validate.notNull(T)` 使用 JDK `Objects.requireNonNull(T)` 替代。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc spring-javaformat:apply
rg -n "Validate\.notNull|\.fields\(" fist-kit-app/fist-boot-apidoc/src/main/java -g "*.java" -S
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am test
```

结果：

- API Doc 目标调用复扫未命中 `Validate.notNull` 和 `.fields(`。
- API Doc deprecation 明细编译为 `BUILD SUCCESS`。
- API Doc 测试为 `BUILD SUCCESS`；模块自身没有测试源码，依赖链 `fist-kit-api` 共 6 个用例，失败 0。
- Springfox 移除后，API Doc 主代码复扫未命中 `springfox`、`SwaggerDocAutoConfiguration`、`ApiTraitOperationPlugin` 和 `swagger.plugin`。
- 编译日志另有依赖链 `fist-kit-api` 的 `ElementType.MODULE` 提示，不属于本轮 API Doc deprecation 收敛目标。

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

## 已处理：Groovy / Spock 测试迁移与构建链路移除

原 `.groovy` 测试文件共 5 个：

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

处理结果：

- `PathMatcherTest.groovy` 迁移为 Java/JUnit 5 `PathMatcherTest.java`。
- `DefaultUserCodecTest.groovy` 迁移为 Java/JUnit 5 `DefaultUserCodecTest.java`。
- `NodeIdxTreeMakerTest.groovy` 迁移为 Java/JUnit 5 `NodeIdxTreeMakerTest.java`，使用 Java 集合替代 `groovy-tables`。
- `AdjacencyTreeMakerTest.groovy` 迁移为 Java/JUnit 5 `AdjacencyTreeMakerTest.java`，使用 Java 集合替代 `groovy-tables`。
- `OpTemplateTest.groovy` 迁移为 Java/JUnit 5 `OpTemplateTest.java`。
- 根 `pom.xml` 删除 `gmavenplus-plugin.version`、`spock-lib.version` 和 `gmavenplus-plugin` 插件管理。
- `fist-kit-build/pom.xml` 删除 Groovy 测试依赖和 Spock BOM 管理。
- `fist-kit-infra/fist-support-test/pom.xml` 删除 `groovy-tables` 与 `spock-spring`。

迁移注意：

- Groovy 属性访问可映射到 JavaBean getter/setter，Java 迁移时需要按真实方法名处理。例如 `OrgNode.parentId` 在 Java 中应使用 `setPid(...)`。
- Groovy 对嵌套类成员访问更宽松，Java 迁移时需要调整测试辅助基类的成员可见性。
- 首次在受限网络沙箱内运行 Maven 时发生 Maven Central 解析失败，属于环境问题；已在可联网环境重新运行验证。

验证命令：

```powershell
rg --files -g "*.groovy"
rg -n "groovy|spock|gmavenplus" pom.xml fist-kit-build/pom.xml fist-kit-infra/fist-support-test/pom.xml fist-kit-app fist-kit-cloud fist-kit-infra -g "pom.xml" -g "*.groovy" -S
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-security/fist-support-security,fist-kit-app/fist-data/fist-support-data spring-javaformat:validate
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-security/fist-support-security,fist-kit-app/fist-data/fist-support-data -am "-Dtest=PathMatcherTest,DefaultUserCodecTest,NodeIdxTreeMakerTest,AdjacencyTreeMakerTest,OpTemplateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- 仓库内 `*.groovy` 复扫未命中。
- Groovy/Spock/gmavenplus 构建配置复扫未命中。
- 目标模块 `spring-javaformat:validate` 为 `BUILD SUCCESS`。
- 5 个迁移后的 Java 测试共 11 个用例，失败 0。
- Web/Security/Data 三个目标模块全量测试为 `BUILD SUCCESS`；目标模块自身测试共 38 个用例，失败 0。

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

## 已验证：Web/Data Commons Lang 过时 API 收敛

继续处理 Web/Data 编译日志中已经暴露的 Apache Commons Lang `3.20.0` 过时 API。该轮只处理替代关系明确的调用，不处理项目自身 deprecation 语义。

受影响模块：

- `fist-kit-app/fist-web/fist-support-web`
- `fist-kit-app/fist-web/fist-boot-web-app`
- `fist-kit-app/fist-data/fist-support-data`
- `fist-kit-app/fist-data/fist-boot-data`

处理依据：

- Context7 `/websites/commons_apache_proper_commons-lang_apidocs` 文档确认 `ObjectUtils.defaultIfNull(T, T)` 已弃用，推荐使用 `ObjectUtils.getIfNull(Object, Object)`。
- 本地 `commons-lang3-3.20.0.jar` 的 `javap` 结果确认 `MutableObject#getValue()` 带 `@Deprecated`，`Mutable` 接口提供 `get()` 默认方法。
- 本地 `commons-lang3-3.20.0.jar` 的 `javap` 结果确认 `Strings.CI.equals(...)` 可用于大小写不敏感比较。

处理结果：

- `Validate.notNull(T)` 迁移到 `Objects.requireNonNull(T)`。
- `ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`。
- `StringUtils.equalsIgnoreCase(...)` 和 `String#equalsIgnoreCase(...)` 迁移到 `Strings.CI.equals(...)`。
- `MutableObject#getValue()` 迁移到 `MutableObject#get()`。
- `TenantInvokeAspect` 删除未使用的 `MutableObject<Throwable>`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data,fist-kit-app/fist-data/fist-boot-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data,fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data,fist-kit-app/fist-data/fist-boot-data -am test
rg -n "defaultIfNull|Validate\.notNull|StringUtils\.equalsIgnoreCase|ret\.getValue\(" fist-kit-app/fist-web/fist-boot-web-app/src/main/java fist-kit-app/fist-web/fist-support-web/src/main/java fist-kit-app/fist-data/fist-boot-data/src/main/java fist-kit-app/fist-data/fist-support-data/src/main/java -g "*.java" -S
```

结果：

- Web/Data 组合 deprecation 明细编译为 `BUILD SUCCESS`。
- 本轮目标调用复扫未命中。
- Web 两个模块当前没有自身测试源码。
- `fist-support-data` 模块自身 Java 测试共 6 个用例，失败 0。
- `fist-boot-data` 模块自身测试共 2 个用例，失败 0。
- 剩余 deprecation 明细为既有 `Breaker`、`ValidateUtil`、`InTenant` 和 `PageData` unchecked 使用。

## 已验证：Gateway / Spring Cloud Alibaba 组合

继续验证 Gateway、Sentinel、Nacos 和 LoadBalancer 在 Boot 4 组合下的编译与运行边界。当前仓库中，Gateway 使用 Spring Cloud Gateway Server WebFlux；Spring Cloud Alibaba Sentinel 只通过 `fist-cloud-rpc-feign` 引入。

受影响模块：

- `fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core`
- `fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive`
- `fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl`
- `fist-kit-cloud/fist-cloud-rpc-feign`

处理结果：

- Gateway 相关依赖已停留在 Boot 4 / Spring Cloud 2025.1 的 WebFlux 新坐标：`spring-cloud-gateway-server-webflux` 与 `spring-cloud-starter-gateway-server-webflux`。
- LoadBalancer 解析到 Spring Cloud `5.0.1`。
- Spring Cloud Alibaba Sentinel starter 解析到 `2025.1.0.0`，传递 `sentinel-spring-webmvc-v6x-adapter:1.8.9`。
- 当前仓库没有引入 `spring-cloud-alibaba-sentinel-gateway`，也没有解析到 `sentinel-spring-cloud-gateway-v6x-adapter`；因此本轮不声明 Sentinel Gateway adapter 已通过。
- Feign 测试中的 `RequestTemplate.method("POST")` 迁移到 OpenFeign 13.6 推荐的 `RequestTemplate.method(Request.HttpMethod.POST)`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am dependency:tree "-Dincludes=org.springframework.cloud:spring-cloud-gateway-server-webflux,org.springframework.cloud:spring-cloud-starter-gateway-server-webflux,org.springframework.cloud:spring-cloud-gateway-server,org.springframework.cloud:spring-cloud-starter-gateway,org.springframework.cloud:spring-cloud-loadbalancer,org.springframework.cloud:spring-cloud-starter-loadbalancer,com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel,com.alibaba.cloud:spring-cloud-alibaba-sentinel-gateway,com.alibaba.csp:sentinel-spring-cloud-gateway-v6x-adapter,com.alibaba.csp:sentinel-spring-webmvc-v6x-adapter,com.alibaba.nacos:nacos-client"
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am "-Dtest=SecureFeignEncoderPrototypeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am spring-javaformat:validate
```

结果：

- 依赖树验证为 `BUILD SUCCESS`，未解析到旧 Gateway starter。
- 组合 clean compile 为 `BUILD SUCCESS`。
- Feign encoder 定向测试为 `BUILD SUCCESS`，1 个用例失败 0。
- 最终组合 test 为 `BUILD SUCCESS`：`fist-gateway-auth-core` 14 个用例、`fist-cloud-gateway-acl` 6 个用例、`fist-cloud-rpc-feign` 7 个用例失败 0；`fist-cloud-gateway-security-reactive` 当前无自身测试源码。
- 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

边界判断：

- 当前仓库已有 Gateway / LoadBalancer / Feign / Sentinel WebMVC adapter 组合可编译、可测试。
- Nacos 当前未被仓库显式引入，只能保持 BOM 准入结论，不能声明运行级验证通过。
- 后续复查确认 Sentinel / Nacos 未实际使用，已移除 Sentinel starter 与 Spring Cloud Alibaba BOM；该结论取代本节中的 Sentinel Gateway adapter 待验证项。

## 下一步

1. 将验证重点从「编译迁移」切换到「应用级启动和组合行为验证」，优先覆盖 Gateway 最小应用上下文、API Doc 自动配置和 OAuth2 Server 组合启动。
2. 继续处理当前仅剩的编译告警基线，重点评估 JDK 8 `release` 模块中 `@NullMarked package-info.java` 触发的 `ElementType.MODULE` 警告是否需要专项消除。
3. 继续保持「`clean compile` 只用于发现新阻塞，定向 `compile` 用于确认真实废弃告警」的审计方式，避免把 reactor 清理副作用误判为 Boot 4 源码问题。

## 已验证：Feign/SDE 与 Boot 4 客户端模块化适配

继续推进 Feign/SDE 专项后，确认当前短期路线可延续 Spring Cloud OpenFeign 5.x；同时，SDE boot starter 和示例工程暴露了 Boot 4 客户端与 Web 测试模块化变化。

受影响模块：

- `fist-kit-infra/fist-sde/fist-sde-boot-starter`
- `examples/fist-sde/example-feign`
- `examples/fist-sde/example-restclient`
- `examples/fist-sde/example-webclient`

处理结果：

- `SdeRestClientAutoConfiguration` 将 `RestClientCustomizer` 迁移到 `org.springframework.boot.restclient.RestClientCustomizer`。
- `fist-sde-boot-starter` 新增普通依赖 `org.springframework.boot:spring-boot-restclient`，避免下游自动配置条件推断阶段缺失返回类型。
- `SdeWebClientAutoConfiguration` 移除 Boot 3 `WebClientCustomizer` 依赖；Boot 4.0.6 本地 JAR 未提供等价类，当前改为在缺少 `WebClient.Builder` 时提供挂载 SDE filter 的 builder。
- SDE MVC 测试将 `AutoConfigureMockMvc` 迁移到 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`，并新增 `spring-boot-webmvc-test` 测试依赖。
- SDE 示例将 `WebServerApplicationContext` 迁移到 `org.springframework.boot.web.server.context.WebServerApplicationContext`。
- SDE Feign 示例将 `SpringEncoder` / `SpringDecoder` 构造参数从 Boot 3 `HttpMessageConverters` 迁移到 OpenFeign 5 `ObjectProvider<FeignHttpMessageConverters>`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am dependency:tree "-Dincludes=org.springframework.cloud:spring-cloud-openfeign-core,org.springframework.cloud:spring-cloud-starter-openfeign,io.github.openfeign:feign-core,io.github.openfeign:feign-okhttp"
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-client -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-client -am test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am test
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am clean test-compile
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am test
.\mvnw.cmd -Pexamples -pl examples/fist-sde/example-feign -am test
.\mvnw.cmd -Pexamples -pl examples/fist-sde/example-restclient,examples/fist-sde/example-webclient -am test
.\mvnw.cmd -Pexamples -pl fist-kit-infra/fist-sde/fist-sde-boot-starter,examples/fist-sde/example-feign,examples/fist-sde/example-restclient,examples/fist-sde/example-webclient -am spring-javaformat:validate
```

结果：

- 依赖树确认当前组合为 `spring-cloud-starter-openfeign:5.0.1`、`spring-cloud-openfeign-core:5.0.1`、`feign-core:13.6`、`feign-okhttp:13.6`。
- `fist-sde-client -am test` 为 `BUILD SUCCESS`，模块自身客户端适配测试共 8 个用例，失败 0。
- `fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`，模块自身 Feign/SDE prototype 行为测试共 7 个用例，失败 0。
- `fist-sde-boot-starter -am test` 为 `BUILD SUCCESS`，模块自身测试共 29 个用例，失败 0。
- `example-sde-feign -am test` 为 `BUILD SUCCESS`，示例端到端测试 1 个用例，失败 0。
- `example-sde-restclient` 和 `example-sde-webclient` 示例测试为 `BUILD SUCCESS`，两个示例各 1 个用例，失败 0。

错误尝试记录：

- 第一次直接运行 `example-sde-feign -am test` 被 SDE boot starter 测试编译阻塞，根因是 Boot 4 的 `AutoConfigureMockMvc` 移入 `spring-boot-webmvc-test`。
- 补充 clean 后发现主源码中 `RestClientCustomizer` 旧包和 `WebClientCustomizer` 缺失；后者在 Boot 4.0.6 本地 JAR 中没有等价类。
- 将 `spring-boot-restclient` 作为 optional 依赖后，starter 自身测试通过，但下游 RestClient 示例运行期因 `RestClientCustomizer` 缺失失败；最终改为普通依赖。
- `example-sde-feign` 进入自身编译后暴露 OpenFeign 5 的 `SpringEncoder` / `SpringDecoder` 构造参数变化。
- RestClient/WebClient 示例进一步暴露 `WebServerApplicationContext` 包迁移。

## 已验证：SDE 与 Spring HTTP Service Clients

Spring Framework 7 的 HTTP Service Clients 通过 `@HttpExchange` 接口和 `HttpServiceProxyFactory` 创建代理，底层使用 `RestClientAdapter`、`WebClientAdapter` 或 `RestTemplateAdapter`。它不是 Feign `Encoder` / `Decoder` 的直接替换点。

受影响模块：

- `fist-kit-infra/fist-sde/fist-sde-client`
- `fist-kit-infra/fist-sde/fist-sde-boot-starter`

处理结果：

- 新增 `SecureHttpServiceClientContextProcessor`，通过 `HttpRequestValues.Processor` 读取 HTTP service 接口方法或接口类型上的 `@SecureExchange`，并写入请求属性。
- 新增 `SecureHttpServiceSupport`，统一请求属性 key，并解析请求级 `SecureExchangeClientContext`。
- `SecureRestClientInterceptor` 与 `SecureWebClientExchangeFilterFunction` 优先读取请求属性中的 SDE 上下文，缺省时保持原有构造器上下文行为。
- `SdeRestClientAutoConfiguration` 注册 `RestClientHttpServiceGroupConfigurer`，为 RestClient-backed HTTP Service group 添加方法级上下文处理器；拦截器挂载做列表去重，避免和 Boot 的 `RestClientCustomizerHttpServiceGroupConfigurer` 重复。
- `SdeWebClientAutoConfiguration` 注册 `WebClientHttpServiceGroupConfigurer`，为 WebClient-backed HTTP Service group 添加方法级上下文处理器，并对 filter 挂载做列表去重。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-client -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-client -am test
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am test
.\mvnw.cmd -Pexamples -pl examples/fist-sde/example-restclient,examples/fist-sde/example-webclient -am test
.\mvnw.cmd -Pexamples -pl fist-kit-infra/fist-sde/fist-sde-client,fist-kit-infra/fist-sde/fist-sde-boot-starter,examples/fist-sde/example-restclient,examples/fist-sde/example-webclient -am spring-javaformat:validate
```

结果：

- `fist-sde-client -am compile` 与 `fist-sde-boot-starter -am compile` 均为 `BUILD SUCCESS`。
- `fist-sde-client -am test` 为 `BUILD SUCCESS`，新增 HTTP service 上下文处理器测试 3 个用例，模块自身测试共 11 个用例，失败 0。
- `fist-sde-boot-starter -am test` 为 `BUILD SUCCESS`，模块自身测试共 29 个用例，失败 0。
- SDE RestClient / WebClient 示例测试为 `BUILD SUCCESS`，两个示例各 1 个用例，失败 0。
- HTTP Service Clients 相关模块与示例的 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

错误尝试记录：

- 第一次新增 HTTP Service Clients 适配后直接编译，`spring-javaformat:validate` 报告新增 Java 文件格式不合规；运行 `spring-javaformat:apply` 后恢复。
- 本地 JAR 复查发现 Boot 4.0.6 已有 `RestClientCustomizerHttpServiceGroupConfigurer`，因此 HTTP Service group 中挂载拦截器/过滤器时必须做去重。

## 已验证：Gateway ACL 最小应用上下文

`fist-cloud-gateway-acl` 已补充 Boot 4 最小应用上下文启动验证。该验证覆盖 Spring Boot 4 Reactive Web Server、Spring Cloud Gateway 5 WebFlux 自动配置和项目 `RouteGuardConfiguration` 的共同启动。

新增测试：

- `RouteGuardConfigurationStartupTest`

验证内容：

- 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 启动 Reactive Web 上下文。
- 显式导入 `RouteGuardConfiguration`。
- 测试应用提供最小应用侧依赖：`ReactivePermissionDefinitionService<PermissionDefinition>` 与 `UserIntrospectClient`。
- 断言 `ApiGuardFilter`、`RequestIdGlobalFilter` 和 `RoutePredicateHandlerMapping` 均进入应用上下文。

影响判断：

- `RouteGuardConfiguration` 在 Boot 4 / Gateway 5 WebFlux 下可以启动。
- 该配置不是完全自包含，下游应用仍必须提供权限定义服务。OAuth2 introspection 相关能力需要配置 `fist.oauth2.resourceserver.opaque-token.introspection-uri` 或提供自定义 `UserIntrospectClient`。
- 本测试不代表 Sentinel Gateway adapter 或 Nacos Discovery 已通过运行验证。

验证结果：

- `RouteGuardConfigurationStartupTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- `fist-cloud-gateway-acl -am test` 为 `BUILD SUCCESS`，目标模块自身 7 个用例，失败 0。
- `fist-cloud-gateway-acl -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

错误尝试记录：

- 第一次测试使用 `assertThat(applicationContext).hasSingleBean(...)`，当前 AssertJ 断言类型不支持该方法，编译失败；改为 `applicationContext.getBeansOfType(...).hasSize(1)`。
- 第二次测试失败于 `ReactivePermissionDefinitionService` 缺失，确认 ACL 自动配置需要应用侧权限定义服务；在测试应用中提供空权限服务后通过。

## 已验证：Gateway OAuth2 opaque-token 配置装配

现有 Gateway 最小应用上下文测试通过手工提供 `UserIntrospectClient`，没有覆盖 `Oauth2Configuration` 根据 `fist.oauth2.resourceserver.opaque-token.*` 配置创建客户端的路径。本轮补充该路径的最小配置装配验证。

受影响文件：

- `fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl/src/test/java/com/power4j/fist/autoconfigure/gateway/Oauth2ConfigurationTest.java`

处理结果：

- 新增 `Oauth2ConfigurationTest`，使用 `ApplicationContextRunner` 装配 `Oauth2Configuration`。
- 验证未设置 `fist.oauth2.resourceserver.opaque-token.introspection-uri` 时不创建 `UserIntrospectClient`。
- 验证设置 introspection URI、client ID 和 client secret 后可创建 `UserIntrospectClient`，并保留 `lbWebClientBuilder`。

影响判断：

- `Oauth2Configuration` 的配置项绑定和条件装配在 Boot 4 下可用。
- 本测试不发起真实远程 introspection 请求，不验证认证服务器响应结构。

验证结果：

- `fist-cloud-gateway-acl -am "-Dtest=Oauth2ConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 2 个，失败 0。
- `fist-cloud-gateway-acl -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 已清理：未实际使用的 Sentinel / Spring Cloud Alibaba 依赖

根据用户补充信息复查后，当前仓库未发现 Sentinel / Nacos 源码、注解、配置或 API 使用点。此前 `fist-cloud-rpc-feign` 仅声明 `spring-cloud-starter-alibaba-sentinel`，但 Feign/SDE 行为测试不依赖该 starter。

受影响文件：

- 根 `pom.xml`
- `fist-kit-build/pom.xml`
- `fist-kit-cloud/fist-cloud-rpc-feign/pom.xml`

处理结果：

- 移除根 `spring-cloud-alibaba.version` 属性。
- 移除 `fist-kit-build` 中的 `spring-cloud-alibaba-dependencies` BOM。
- 移除 `fist-cloud-rpc-feign` 的 `spring-cloud-starter-alibaba-sentinel` 依赖。

影响判断：

- FIST Kit v4 当前不再向下游传递 Spring Cloud Alibaba、Sentinel 或 Nacos 依赖。
- Gateway / Feign / LoadBalancer 当前仍由 Spring Cloud `2025.1.1` 管理。
- 下游如果仍使用 Sentinel 或 Nacos，需要在应用侧显式引入并独立验证 Boot 4 兼容性。

验证结果：

- Sentinel / Nacos 复扫未发现 Git 跟踪源码、配置或主 POM 使用点。
- 移除 Sentinel starter 后，Feign 模块依赖树未解析到 Sentinel 相关依赖。
- 移除 Spring Cloud Alibaba BOM 后，Cloud 组合依赖树未解析到 Alibaba / Sentinel / Nacos 相关依赖，命令为 `BUILD SUCCESS`。
- `fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`，目标模块自身 7 个用例，失败 0。
- `fist-cloud-rpc-feign` + `fist-cloud-gateway-acl` 组合测试为 `BUILD SUCCESS`：Gateway Auth Core 14 个用例、Gateway ACL 7 个用例、Feign 7 个用例失败 0。
- 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

## OAuth2 授权服务 LOB API

`DefaultAuthorizationService` 原先直接依赖 Spring JDBC `LobHandler`、`LobCreator` 和 `DefaultLobHandler`。Spring Framework 当前 Javadoc 标注这些类型自 6.2 起弃用，推荐使用 `SqlBinaryValue` 和 `SqlCharacterValue`；读取侧建议直接使用 `ResultSet`。

已完成迁移：

- 写入 BLOB 字段时使用 `new SqlBinaryValue(value.getBytes(StandardCharsets.UTF_8))`。
- 写入 CLOB 字段时使用 `new SqlCharacterValue(value)`。
- 读取 BLOB 字段时使用 `ResultSet#getBytes(...)`，并按既有约定使用 UTF-8 转回字符串。
- 读取 CLOB 字段时使用 `ResultSet#getString(...)`。
- 删除对 `LobHandler`、`LobCreator`、`DefaultLobHandler` 和自定义 `LobCreatorArgumentPreparedStatementSetter` 的依赖。
- 删除 `DefaultAuthorizationService(JdbcOperations, RegisteredClientRepository, LobHandler)`、`OAuth2AuthorizationRowMapper#setLobHandler(...)` 和相关 protected getter。

影响判断：

- 这是 Boot 4 迁移中的破坏性 API 清理。下游如果显式传入自定义 `LobHandler`，需要改为依赖默认 JDBC 绑定逻辑，或自定义 `authorizationParametersMapper` / `authorizationRowMapper`。
- 达梦等数据库的 BLOB 查询兼容逻辑仍保留：字段类型仍来自数据库元数据，查询 SQL 仍走 `processQuerySql(...)` 中的 LOB 比较函数处理。
- 当前本地验证已覆盖 Spring Authorization Server 7 官方 H2 schema 下的 BLOB 写入、读取和 access token 查询，也覆盖了将 LOB 字段改为 CLOB 后的写入与读取。
- MySQL 8、PostgreSQL 16 和达梦真实驱动矩阵已通过条件集成测试入口验证；当前剩余风险从 JDBC LOB 兼容性转向更完整的应用级集成场景。

本地数据库矩阵：

| 数据库 / schema | 验证方式 | 结果 | 说明 |
| --- | --- | --- | --- |
| H2 + Authorization Server 7 官方 `oauth2-authorization-schema.sql` | `DefaultAuthorizationService.save(...)`、`findById(...)`、`findByToken(..., ACCESS_TOKEN)` | 通过 | 覆盖 `SqlBinaryValue` 写入、`ResultSet#getBytes(...)` 读取和 BLOB token 查询 |
| H2 + CLOB schema 变体 | 将官方 schema 中 `blob DEFAULT NULL` 替换为 `clob DEFAULT NULL` 后执行 `save(...)`、`findById(...)` | 通过 | 覆盖 `SqlCharacterValue` 写入和 `ResultSet#getString(...)` 读取 |
| MySQL 8 | 外部条件集成测试 `ExternalJdbcAuthorizationServiceTest` | 通过 | 覆盖 schema 初始化、`save(...)`、`findById(...)` 和 `findByToken(..., ACCESS_TOKEN)` |
| PostgreSQL 16 | 外部条件集成测试 `ExternalJdbcAuthorizationServiceTest` | 通过 | 初始化 schema 时将 `blob` 转换为 `text`，`timestamp` 转换为 `timestamptz` |
| 达梦 | 外部条件集成测试 `ExternalJdbcAuthorizationServiceTest` | 通过 | 保留既有 `getLobCmpFunc()` 兼容策略，覆盖 schema 初始化、授权写入与读取 |

验证结果：

- `DefaultAuthorizationServiceTest` 定向测试为 `BUILD SUCCESS`，共 4 个用例，失败 0。
- `fist-boot-security-oauth2-server -am test` 为 `BUILD SUCCESS`，目标模块自身共 7 个用例，失败 0。
- `fist-boot-security-oauth2-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`。
- MySQL 8、PostgreSQL 16 和达梦外部条件集成测试均为 `BUILD SUCCESS`，每轮测试 1 个用例，失败 0。

## 已复核：当前废弃告警基线

在 MySQL 8、PostgreSQL 16、达梦和 Redis 7 外部资源矩阵完成后，重新按当前仓库状态执行 Boot 4 废弃告警复核，目的是确认「还剩哪些真实问题」，而不是继续追逐历史文档中的旧待办。

执行结果：

- `fist-support-web`、`fist-support-data`、`fist-boot-apidoc` 不带 `clean` 的定向 `compile` 审计均为 `BUILD SUCCESS`，日志未命中生产代码 `deprecated` 告警。
- `fist-sde-client`、`fist-sde-boot-starter`、`fist-cloud-rpc-feign` 组合 `clean compile` 为 `BUILD SUCCESS`。
- JDK 8 `release` 模块中 `package-info.java` 上的 `@NullMarked` 曾触发 `ElementType.MODULE` 警告，已在后续专项中处理。
- `fist-boot-apidoc` 和 `web/data` 组合第一次 `clean compile` 曾出现编译失败，但复跑不带 `clean` 的定向 `compile` 后均通过，说明这两次失败不能直接视为 Boot 4 生产代码阻塞。

当前判断：

- 当前仓库主代码面上，Boot 4 迁移期最显著的废弃 API 已基本清理完毕。
- JDK 8 兼容模块中的 JSpecify `@NullMarked` 元注解警告已清理，保留显式 `@Nullable` 表达可空契约。
- 升级工作的下一阶段应从「清编译错误 / 清废弃调用」转向「最小应用启动、自动配置装配和跨模块组合行为验证」。

## 已处理：JDK 8 release 模块的 JSpecify package 默认非空告警

在废弃告警基线复核后，继续处理 JDK 8 `release` 模块中 `package-info.java` 上 `@NullMarked` 触发的编译告警。

问题根因：

- JSpecify `1.0.0` 的 `org.jspecify.annotations.NullMarked` 自身 classfile 可被 Java 8 读取。
- 但它的 `@Target` 元数据包含 Java 9 引入的 `ElementType.MODULE`。
- 本项目部分 infra 模块要求 `maven.compiler.release=8`，当这些模块的 `package-info.java` 使用包级 `@NullMarked` 时，`javac --release 8` 会报告「未知的枚举常量 `java.lang.annotation.ElementType.MODULE`」。

处理结果：

- 从 JDK 8 release 模块的 19 个 `package-info.java` 中移除包级 `@NullMarked` 和对应 import。
- 保留源码中已经存在的显式 `org.jspecify.annotations.Nullable`，不改变方法参数、返回值、字段和泛型类型参数上的可空契约。
- 未采用 `-Xlint:none` 或 `-nowarn` 这类全局关闭告警的方式，避免掩盖后续真实迁移问题。

影响范围：

- `fist-kit-infra/fist-kit-api`
- `fist-kit-infra/fist-jackson`
- `fist-kit-infra/fist-logback`
- `fist-kit-infra/fist-mybatis-plus`
- `fist-kit-infra/fist-sde/fist-sde-core`
- `fist-kit-infra/fist-sde/fist-sde-client`

验证结果：

- `fist-kit-api -am clean compile` 为 `BUILD SUCCESS`，日志未命中 `WARNING` 或 `ElementType.MODULE`。
- JDK 8 release 相关 infra 模块组合 `clean compile` 为 `BUILD SUCCESS`，日志未命中 `WARNING` 或 `ElementType.MODULE`。
- 目标模块 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 已验证：SDE 示例应用组合启动

在自动配置最小上下文通过后，继续扩大到 `examples/fist-sde` 示例应用。该验证覆盖真实 Spring Boot 应用启动、HTTP server、Feign 客户端、RestClient 上下文和 WebClient 上下文。

首次执行结果：

- 直接从 `examples/fist-sde/pom.xml` 执行 `test` 时，Maven 无法解析仓库内 `4.0.0-SNAPSHOT` 的 `fist-sde-boot-starter`、`fist-sde-client` 和 `fist-sde-extra`。
- 根因是 `examples` 不在根 `pom.xml` reactor 中，不能自动带上主工程模块。
- 先执行 `fist-sde-boot-starter -am -DskipTests install` 将本地快照组件安装到本地 Maven 仓库后，示例测试进入真实编译阶段。

编译阻塞：

- `example-server` 的 `SdeServerDemoRunner` 仍引用旧包 `org.springframework.boot.web.context.WebServerApplicationContext`。
- Boot 4 中该接口位于 `org.springframework.boot.web.server.context.WebServerApplicationContext`。
- 同组 `example-restclient` 和 `example-webclient` 已使用新包，说明这是 server 示例遗漏的局部迁移点。

处理结果：

- 将 `SdeServerDemoRunner` 的 `WebServerApplicationContext` import 迁移到 Boot 4 新包。

验证结果：

- `examples/fist-sde` 整组测试为 `BUILD SUCCESS`。
- `SdeServerExampleApplicationTest`、`SdeFeignExampleApplicationTest`、`SdeRestClientExampleApplicationTest`、`SdeWebClientExampleApplicationTest` 各运行 1 个用例，失败 0。
- 其中 server 和 feign 示例包含真实 HTTP 调用；restclient 和 webclient 示例覆盖应用上下文启动。

## 已处理：Security 自动配置注册与 Web/Data/Security 组合上下文

SDE 示例验证后，继续扩大到 Web/Data/Security 组合装配。复查 `fist-boot-security` 时发现该模块的自动配置注册文件仍指向 Web 模块自动配置，未注册自身的 `InnerSecurityConfiguration`。

问题根因：

- `fist-boot-security` 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 和 `spring.factories` 误注册了 `CommonConfiguration`、`FistWebAutoConfiguration`、`FistReactiveWebAutoConfiguration`。
- `InnerSecurityConfiguration` 只通过 `@TrustedSecurity` 注解手工导入，没有作为 Boot 自动配置候选注册。
- 这会导致下游仅引入 `fist-boot-security` 时，可信用户过滤器、用户解码器、密码编码器等默认安全 bean 不会按 starter 方式自动装配。

处理结果：

- `InnerSecurityConfiguration` 从普通 `@Configuration` 调整为 `@AutoConfiguration`。
- `fist-boot-security` 的 `AutoConfiguration.imports` 和 `spring.factories` 改为只注册 `InnerSecurityConfiguration`。
- 新增 `InnerSecurityConfigurationImportsTest`，隔离读取 `fist-boot-security` 自身 `target/classes` 下的自动配置候选，避免被测试依赖中的 Web/Data 自动配置污染。
- 同一测试类补充 Servlet Web/Data/Security 组合上下文验证，确认 Web、Data、Security 三组自动配置可以在同一上下文中创建核心 bean 和 filter 注册。

验证结果：

- RED：`InnerSecurityConfigurationImportsTest` 首次失败，原因是候选中缺少 `InnerSecurityConfiguration`。
- GREEN 首次被 `spring-javaformat:validate` 拦截；运行目标模块 `spring-javaformat:apply` 后继续验证。
- 新增 Web/Data 测试依赖后，导入候选测试一度被依赖模块候选污染；改为使用只包含 security 模块输出目录的 `URLClassLoader` 后通过。
- `InnerSecurityConfigurationImportsTest` 定向测试为 `BUILD SUCCESS`，2 个用例，失败 0。
- `fist-boot-security -am test` 为 `BUILD SUCCESS`。
- `fist-boot-security -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 已验证：SDE 示例配置项 migrator 使用流程

按照 Spring Boot 4 文档建议，本轮临时将 `spring-boot-properties-migrator` 作为 runtime 依赖加入 `examples/fist-sde`，用于验证当前示例配置是否存在 Boot 4 废弃或重命名配置项。

处理结果：

- 临时接入 migrator 后，`examples/fist-sde` 整组测试仍为 `BUILD SUCCESS`。
- dependency tree 确认 `example-sde-server` runtime classpath 包含 `spring-boot-properties-migrator:4.0.6`。
- 运行日志未出现配置项迁移诊断。
- 验证完成后已移除临时 migrator 依赖，源码不保留该 runtime 依赖。

影响判断：

- 当前 SDE 示例配置没有发现需要由 Spring Boot 4 migrator 迁移的配置项；该结论只说明仓库示例配置当前没有触发诊断。
- `spring-boot-properties-migrator` 的主要价值在应用侧，尤其是内部项目的真实 `application.yml`、环境变量、启动参数和外部配置中心组合。
- FIST Kit 作为基础库只能验证 migrator 使用流程和示例基线，不能替代内部项目的配置迁移验证。
- 内部项目升级时应在真实启动路径临时接入 migrator，处理完诊断后移除。

## 已验证：API Doc 自动配置最小上下文

为避免 API Doc 模块只有编译通过、没有装配证据，本轮补充了 Boot 4 下的最小自动配置测试。

受影响文件：

- `fist-kit-app/fist-boot-apidoc/pom.xml`
- `fist-kit-app/fist-boot-apidoc/src/test/java/com/power4j/fist/boot/autoconfigure/apidoc/SpringDocAutoConfigurationTest.java`

处理结果：

- `fist-boot-apidoc` 补充 `spring-boot-starter-test` 测试依赖。
- 新增 `SpringDocAutoConfigurationTest`，使用 `ApplicationContextRunner` 装配 `SpringDocAutoConfiguration`。
- 测试断言 `OperationCustomizer` 和 `OpenAPI` bean 可创建，并校验 `inner_auth` 的 `SecurityScheme` 仍使用 `SecurityConstant.HEADER_USER_TOKEN_INNER`。

验证结果：

- 首次执行被 `spring-javaformat:validate` 拦截；运行 `spring-javaformat:apply` 后重跑通过。
- `fist-boot-apidoc -am "-Dtest=SpringDocAutoConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 1 个，失败 0。
- `fist-boot-apidoc -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 已验证：OAuth2 Server 自动配置最小上下文

为避免 OAuth2 Server 模块只保留功能级单元测试、没有 Boot 4 下的自动配置装配证据，本轮补充最小组合启动验证。

受影响文件：

- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/pom.xml`
- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/test/java/com/power4j/fist/autoconfigure/oas/AuthorizationServerAutoConfigurationTest.java`

处理结果：

- `fist-boot-security-oauth2-server` 补充 `spring-boot-starter-test` 测试依赖。
- 新增 `AuthorizationServerAutoConfigurationTest`，使用 `ApplicationContextRunner` 验证 `AuthorizationServerAutoConfiguration` 默认装配。
- 同时验证 `@Oauth2AuthorizationServer` 注解导入路径仍能正确导入自动配置。

验证结果：

- 首次执行被 `spring-javaformat:validate` 拦截；运行 `spring-javaformat:apply` 后重跑通过。
- `fist-boot-security-oauth2-server -am "-Dtest=AuthorizationServerAutoConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 2 个，失败 0。
- `fist-boot-security-oauth2-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 已验证：OAuth2 Server 自动配置与 JDBC 授权服务组合上下文

OAuth2 Server 自动配置最小上下文只能证明默认 bean 可装配，不能证明应用侧 `DefaultAuthorizationService` 与自动配置提供的 `OAuth2TokenGenerator` 能在同一个 Boot 4 上下文中共同工作。本轮补充更完整的组合上下文验证。

受影响文件：

- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/test/java/com/power4j/fist/autoconfigure/oas/AuthorizationServerComposedContextTest.java`

处理结果：

- 新增 `AuthorizationServerComposedContextTest`，使用 `ApplicationContextRunner` 同时加载 `AuthorizationServerAutoConfiguration` 和测试侧应用配置。
- 测试侧使用 H2 `DataSource`、`JdbcTemplate`、Spring Authorization Server 官方 `oauth2-authorization-schema.sql`、测试 `RegisteredClientRepository` 和 `DefaultAuthorizationService`。
- 验证同一上下文中存在 `OAuth2TokenGenerator`、`RegisteredClientRepository` 和 `ManagedOAuth2AuthorizationService`。
- 验证 `DefaultAuthorizationService` 能保存 `OAuth2Authorization`，并能按 ID 与 access token 读取。

影响判断：

- 这是测试能力增强，不改变生产自动配置和默认运行行为。
- 当前证据说明 OAuth2 Server 模块在 Boot 4 / Spring Authorization Server 7 组合下，自动配置默认 token 生成器与应用侧 JDBC 授权服务可共同装配。
- 真实数据库 LOB 兼容性已由条件集成测试覆盖；本测试只验证环境无关的组合启动和基础授权读写。

验证结果：

- 首次执行被 `spring-javaformat:validate` 拦截；运行目标模块 `spring-javaformat:apply` 后重跑通过。
- `fist-boot-security-oauth2-server -am "-Dtest=AuthorizationServerComposedContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 1 个，失败 0。
- `fist-boot-security-oauth2-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## Redisson 队列订阅 API

Redisson `4.3.1` 中，`RBlockingQueue.subscribeOnElements(Consumer<V>)` 已废弃，但 `RBlockingQueue.subscribeOnElements(Function<V, CompletionStage<Void>>)` 仍保留。`fist-redisson` 的队列消费者注册属于方法重载迁移，不需要切换到 `RReliableQueue`。

受影响文件：

- `fist-kit-infra/fist-redisson/src/main/java/com/power4j/fist/boot/autoconfigure/redisson/queue/QueueConsumerBeanProcessor.java`
- `fist-kit-infra/fist-redisson/src/test/java/com/power4j/fist/boot/autoconfigure/redisson/queue/QueueConsumerBeanProcessorTest.java`

处理结果：

- `QueueConsumerBeanProcessor` 改用 `Function<V, CompletionStage<Void>>` 重载。
- 正常消费完成后返回 `CompletableFuture.completedFuture(null)`。
- 新增单元测试覆盖订阅重载选择和 `@QueueConsumer` 方法调用。

影响判断：

- 订阅队列类型、队列名称生成、消息类型校验和反射调用逻辑保持不变。
- 该迁移清除 Redisson 队列订阅处的生产代码 deprecation 来源。

验证结果：

- RED：新增测试在旧实现下失败，原因是旧实现调用 `Consumer` 重载。
- GREEN：迁移后 `QueueConsumerBeanProcessorTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- `fist-redisson -am test` 为 `BUILD SUCCESS`，目标模块自身 1 个用例，失败 0。
- `fist-redisson -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，deprecation 明细日志未再出现 `subscribeOnElements` 或过时提示。
- `fist-redisson -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## OAuth2 Resource Owner 自有废弃 API

`OAuth2ResourceOwnerBaseAuthenticationProvider` 原先保留 `refreshTokenGenerator` 字段和 `setRefreshTokenGenerator(...)` 方法，并已标记 `@Deprecated`。Spring Authorization Server 7 的 token 自定义路径是 `OAuth2TokenGenerator` 或 `DelegatingOAuth2TokenGenerator`，该历史扩展点在 Boot 4 主版本迁移中删除。

受影响文件：

- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java/com/power4j/fist/oauth2/extension/support/OAuth2ResourceOwnerBaseAuthenticationProvider.java`
- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/test/java/com/power4j/fist/oauth2/extension/support/OAuth2ResourceOwnerBaseAuthenticationProviderTest.java`

处理结果：

- 删除 `refreshTokenGenerator` 字段和 `setRefreshTokenGenerator(...)`。
- 刷新令牌生成统一走既有 `OAuth2TokenGenerator`。
- 新增反射测试，防止该 provider 继续暴露带 `@Deprecated` 的字段或方法。

影响判断：

- 这是 Boot 4 主版本迁移中的破坏性 API 清理。下游如果仍调用 `setRefreshTokenGenerator(...)`，需要改为提供自定义 `OAuth2TokenGenerator`。
- 令牌生成流程保留原有 Spring Authorization Server 扩展模型。

验证结果：

- RED：新增反射测试在旧实现下失败，原因是 provider 仍存在废弃字段或方法。
- GREEN：迁移后 `OAuth2ResourceOwnerBaseAuthenticationProviderTest` 定向测试为 `BUILD SUCCESS`，2 个用例，失败 0。
- `fist-boot-security-oauth2-server -am test` 为 `BUILD SUCCESS`，目标模块自身 8 个用例，失败 0。
- `fist-boot-security-oauth2-server -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，本轮目标关键字 `setRefreshTokenGenerator` / `refreshTokenGenerator` 未再出现。
- `fist-boot-security-oauth2-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 自有废弃 API 注解一致性

`Breaker` 和 `ValidateUtil#check(T, Class<?>...)` 已通过 Javadoc 声明废弃，但缺少 Java 级 `@Deprecated` 注解。Boot 4 迁移中的 deprecation 明细编译会把这类不一致暴露为声明侧警告。

受影响文件：

- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/common/error/Breaker.java`
- `fist-kit-app/fist-web/fist-support-web/src/test/java/com/power4j/fist/boot/common/error/BreakerDeprecationTest.java`
- `fist-kit-app/fist-data/fist-support-data/src/main/java/com/power4j/fist/data/crud/validate/ValidateUtil.java`
- `fist-kit-app/fist-data/fist-support-data/src/test/java/com/power4j/fist/data/crud/validate/ValidateUtilDeprecationTest.java`

处理结果：

- `Breaker` 类补充 `@Deprecated`。
- `ValidateUtil#check(T, Class<?>...)` 旧重载补充 `@Deprecated`。
- 新增声明级测试，防止 Javadoc 与 Java 注解再次不一致。

影响判断：

- 本次不删除 API，不改变运行时行为，只修正公共 API 元数据。
- 测试代码引用废弃类型产生的 deprecation 提示属于测试断言副作用，生产代码是否清理仍需以生产编译日志为准。
- 组合模块验证当前被 Maven 依赖解析阻断，需在网络或审批服务恢复后补跑。

验证结果：

- RED：`BreakerDeprecationTest` 在旧实现下失败，原因是 `Breaker` 缺少 Java 级 `@Deprecated`。
- RED：`ValidateUtilDeprecationTest` 在旧实现下失败，原因是旧 `check(Object, Class[])` 重载缺少 Java 级 `@Deprecated`。
- GREEN：`BreakerDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- GREEN：`ValidateUtilDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- `fist-support-web` 和 `fist-support-data` 的 `spring-javaformat:apply` 均为 `BUILD SUCCESS`。
- 组合模块 `test` 未完成：Maven Central BOM POM 下载失败并被本地仓库缓存，提升权限重跑时审批服务返回 503。

## Data 自有废弃 API 注解一致性补充

继续收敛 Data 模块声明侧 deprecation 警告。`InTenant` 和 `AuditFiller` 已在 Javadoc 中声明废弃，但缺少 Java 级 `@Deprecated`。

受影响文件：

- `fist-kit-app/fist-data/fist-boot-data/src/main/java/com/power4j/fist/data/tenant/annotation/InTenant.java`
- `fist-kit-app/fist-data/fist-boot-data/src/test/java/com/power4j/fist/data/tenant/annotation/InTenantDeprecationTest.java`
- `fist-kit-app/fist-data/fist-boot-crud-mybatis/src/main/java/com/power4j/fist/boot/mybaits/handler/AuditFiller.java`
- `fist-kit-app/fist-data/fist-boot-crud-mybatis/src/test/java/com/power4j/fist/boot/mybaits/handler/AuditFillerDeprecationTest.java`

处理结果：

- `InTenant` 注解补充 `@Deprecated`。
- `AuditFiller` 类补充 `@Deprecated`。
- 新增声明级测试，防止 Javadoc 与 Java 注解再次不一致。

影响判断：

- 本次不删除 API，不改变运行时行为，只修正公共 API 元数据。
- `fist-boot-data` 主代码仍通过 `InTenantAspect` 使用废弃 `InTenant`，deprecation 明细编译会继续报告调用点提示；是否删除租户注解和切面属于后续破坏性 API 决策。
- Maven 反应堆验证必须顺序执行或合并执行，避免多个命令并发写入同一 `target` 目录。

验证结果：

- RED：`InTenantDeprecationTest` 和 `AuditFillerDeprecationTest` 在旧实现下均失败，断言结果为 `expected: <true> but was: <false>`。
- GREEN：`InTenantDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- GREEN：`AuditFillerDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- Data 组合模块测试为 `BUILD SUCCESS`，`fist-boot-data` 3 个用例、`fist-boot-crud-mybatis` 15 个用例失败 0。
- Data 组合 deprecation 明细编译为 `BUILD SUCCESS`，`InTenant` / `AuditFiller` 的声明侧缺注解警告已消失。
- Data 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

## Data `InTenant` 废弃注解入口移除

`InTenant` 已经没有仓库内实际业务使用，继续保留会让 `fist-boot-data` 生产代码持续引用自有废弃 API。本轮按主版本破坏性变更处理，删除旧注解入口。

受影响文件：

- `fist-kit-app/fist-data/fist-boot-data/src/main/java/com/power4j/fist/boot/autoconfigure/data/DataAutoConfiguration.java`
- `fist-kit-app/fist-data/fist-boot-data/src/main/java/com/power4j/fist/data/tenant/InTenantAspect.java`
- `fist-kit-app/fist-data/fist-boot-data/src/main/java/com/power4j/fist/data/tenant/annotation/InTenant.java`
- `fist-kit-app/fist-data/fist-boot-data/src/test/java/com/power4j/fist/boot/autoconfigure/data/DataAutoConfigurationTest.java`
- `fist-kit-app/fist-data/fist-boot-data/src/test/java/com/power4j/fist/data/tenant/annotation/InTenantDeprecationTest.java`

处理结果：

- `DataAutoConfiguration` 不再注册 `inTenantAspect`。
- 删除 `InTenantAspect` 和 `InTenant`。
- 删除 `InTenantDeprecationTest`。
- `DataAutoConfigurationTest` 改为覆盖 `TenantInvokeAspect` 保留和旧 bean 移除。

影响判断：

- 下游应用如果仍编译引用 `@InTenant`，升级到当前主版本后会失败，需要迁移到当前租户调用模型。
- 仓库内 `fist-boot-data` 已无 `InTenant` / `InTenantAspect` 残留引用。
- 本轮不改变 `TenantInvokeAspect`、`TenantContextFilter` 或验证器自动配置。

验证结果：

- RED：`DataAutoConfigurationTest` 在旧实现下失败，原因是上下文仍存在 `inTenantAspect` bean。
- 格式拦截：删除旧 bean 后首次目标测试被 `spring-javaformat:validate` 拦截，运行 `spring-javaformat:apply` 修复。
- GREEN：`DataAutoConfigurationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- 复扫：`rg -n "InTenant|InTenantAspect" fist-kit-app/fist-data/fist-boot-data` 未命中。
- 模块测试：`fist-boot-data -am test` 为 `BUILD SUCCESS`，`fist-support-data` 12 个用例、`fist-boot-data` 2 个用例失败 0。
- deprecation 明细编译：`fist-boot-data -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，不再出现 `InTenant` / `InTenantAspect` 相关提示。
- 格式校验：`fist-boot-data -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## Web `Breaker` 废弃工具类移除

`Breaker` 是自有废弃异常工具类，仓库生产代码仅剩 `MessageConfiguration` 为其注入国际化依赖。主版本阶段删除旧工具类和对应自动配置入口，避免继续暴露未使用的旧异常 API。

受影响文件：

- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/common/error/Breaker.java`
- `fist-kit-app/fist-web/fist-support-web/src/test/java/com/power4j/fist/boot/common/error/BreakerDeprecationTest.java`
- `fist-kit-app/fist-web/fist-support-web/src/test/java/com/power4j/fist/boot/common/error/BreakerRemovalTest.java`
- `fist-kit-app/fist-web/fist-boot-web-app/src/main/java/com/power4j/fist/boot/autoconfigure/i18n/MessageConfiguration.java`
- `fist-kit-app/fist-web/fist-boot-web-app/src/main/java/com/power4j/fist/boot/autoconfigure/common/CommonConfiguration.java`
- `fist-kit-app/fist-web/fist-boot-web-app/src/test/java/com/power4j/fist/boot/autoconfigure/common/CommonConfigurationTest.java`

处理结果：

- 删除 `Breaker`。
- 删除只服务 `Breaker` 的 `MessageConfiguration`。
- 删除 `BreakerDeprecationTest`。
- `CommonConfiguration` 不再导入 `MessageConfiguration`。
- 新增删除锁定测试，防止旧工具类和旧自动配置入口回流。

影响判断：

- 下游应用如果仍调用 `Breaker`，升级当前主版本后会编译失败，需要改用 `CommonErrors` / `MsgBundleRejectedException` 等当前异常模型。
- `fist-boot-web-app` 的 `i18n/common*.properties` 仍被当前异常模型使用，本轮不删除资源文件。
- 本轮不改变 `ServerErrorHandler`、`DefaultExceptionTranslator` 和 `AppMonConfiguration` 的消息解析路径。

验证结果：

- RED：`BreakerRemovalTest` 在旧实现下失败，原因是旧类仍可加载。
- RED：`CommonConfigurationTest` 在旧实现下失败，原因是 `@Import` 仍包含 `MessageConfiguration`。
- 格式拦截：删除 `@Import` 元素后，`CommonConfigurationTest` 首次 GREEN 被 `spring-javaformat:validate` 拦截，运行 `spring-javaformat:apply` 修复。
- GREEN：两个目标测试均为 `BUILD SUCCESS`，各 1 个用例，失败 0。
- 组合测试：`fist-boot-web-app -am test` 为 `BUILD SUCCESS`，`fist-support-web` 8 个用例、`fist-support-security` 20 个用例、`fist-boot-web-app` 1 个用例失败 0。
- deprecation 明细编译：`fist-boot-web-app -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，不再出现 `Breaker` / `MessageConfiguration` 相关生产提示。
- 格式校验：`fist-boot-web-app -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 外部资源验证测试入口

本轮新增两个条件集成测试入口，用于后续真实资源矩阵验证。它们默认不访问外部服务，只在设置环境变量时启用。

受影响文件：

- `fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/test/java/com/power4j/fist/oauth2/extension/service/ExternalJdbcAuthorizationServiceTest.java`
- `fist-kit-infra/fist-redisson/src/test/java/com/power4j/fist/boot/autoconfigure/redisson/queue/ExternalRedisQueueConsumerTest.java`

处理结果：

- `ExternalJdbcAuthorizationServiceTest` 使用 `DriverManagerDataSource` 和 `DefaultAuthorizationService`，覆盖授权写入、按 ID 读取和按 access token 读取。
- JDBC 驱动类和驱动 JAR 支持通过环境变量注入，避免把达梦等外部驱动绑定到源码。
- `ExternalJdbcAuthorizationServiceTest` 检测到 PostgreSQL JDBC URL 时，初始化 schema 会将官方 schema 中的 `blob` 转换为 `text`，将 `timestamp` 转换为 `timestamptz`。
- `ExternalRedisQueueConsumerTest` 使用真实 Redisson client 注册 `QueueConsumerBeanProcessor`，向本次生成的队列写入消息并等待消费方法回调。
- `ExternalRedisQueueConsumerTest` 使用测试专用 `NameGenerator` 将 `@QueueConsumer("test")` 映射到本次运行唯一队列名，避免测试写入队列与处理器订阅队列不一致。
- Redisson 密码配置使用 `Config#setPassword(String)`，避免测试源码继续调用 Redisson `4.3.1` 中已废弃的 `BaseConfig#setPassword(String)`。

影响判断：

- 这是测试能力增强，不改变生产代码和默认测试行为。
- 未设置 `FIST_IT_JDBC_URL` 或 `FIST_IT_REDIS_URL` 时，测试类会编译但跳过。
- 真实环境需要在本地 shell 注入环境变量后再执行，不在仓库中固化。

验证结果：

- `ExternalJdbcAuthorizationServiceTest` 定向验证为 `BUILD SUCCESS`，测试 1 个，跳过 1 个。
- `ExternalRedisQueueConsumerTest` 定向验证为 `BUILD SUCCESS`，测试 1 个，跳过 1 个。
- MySQL 8 真实环境验证为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0。
- PostgreSQL 16 首次真实环境验证因官方 schema 的 `blob` 类型失败；加入 PostgreSQL schema 变体后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0。
- Redis 7 首次真实环境验证因队列名不一致导致消费超时；修正队列名映射后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0。
- Redis 条件测试无环境变量回归为 `BUILD SUCCESS`，测试 1 个，跳过 1 个。
- 达梦首次真实验证因账号认证状态失败；更新测试凭据后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0，覆盖 schema 初始化、授权写入、按 ID 读取和按 access token 查询。
- MySQL、PostgreSQL、达梦和 Redis 外部资源矩阵已完成当前测试入口覆盖；后续风险转向更完整的应用级启动和业务集成验证。

## Web/Data/Security/API Doc 示例应用

本轮新增并扩展 `examples/fist-web-data-security/example-servlet`，用于覆盖 Web、Data、Security、API Doc 四类 starter 在 Spring Boot 4 Servlet 应用中的组合启动路径。

受影响文件：

- `examples/pom.xml`
- `examples/README.md`
- `examples/fist-web-data-security/pom.xml`
- `examples/fist-web-data-security/README.md`
- `examples/fist-web-data-security/example-servlet/pom.xml`
- `examples/fist-web-data-security/example-servlet/src/main/java/com/power4j/fist3/examples/webdatasecurity/servlet/*`
- `examples/fist-web-data-security/example-servlet/src/main/resources/application.yml`
- `examples/fist-web-data-security/example-servlet/src/main/resources/schema.sql`
- `examples/fist-web-data-security/example-servlet/src/main/resources/data.sql`
- `examples/fist-web-data-security/example-servlet/src/test/java/com/power4j/fist3/examples/webdatasecurity/servlet/WebDataSecurityExampleApplicationTest.java`

处理结果：

- 示例应用引入 `fist-boot-web-app`、`fist-boot-data`、`fist-boot-security`、`fist-boot-apidoc`、`spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-jdbc`、`springdoc-openapi-starter-webmvc-api` 和 H2。
- `ProfileController` 通过真实 HTTP 请求覆盖 H2 查询、`Pageable` 参数解析、`ObjectValidator` 校验、`UserInfoSupplier` 可信用户头解析和 FIST Web 请求 ID 读取。
- 示例测试使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 启动 Tomcat 11，并通过 `RestClient` 发送 GET / POST / OpenAPI 文档请求。
- 测试断言 `TenantInvokeAspect`、`ObjectValidator`、`PasswordEncoder`、`UserDecoder`、`UserInfoSupplier`、`TraceInfoResolver`、`tenantContextFilter`、`headerMdcFilter` 和 `trustedUserFilter` 已装配。
- 测试请求 `/v3/api-docs`，断言 OpenAPI JSON 包含 `inner_auth` 安全方案、`X-USER-IN` header 和示例业务路径。

影响判断：

- 这是示例和验证覆盖增强，不改变生产模块行为。
- 该示例补上了此前仅有最小 `WebApplicationContextRunner` / `ApplicationContextRunner` 组合上下文、缺少真实 HTTP 和 OpenAPI 文档请求的覆盖缺口。
- 示例使用 H2 内存库，不依赖外部数据库或 Redis 资源。

验证结果：

- 首次测试编译失败，原因是测试误用 `assertThat(applicationContext).hasSingleBean(...)`，当前 AssertJ 类型为普通 `ObjectAssert<ApplicationContext>`；改为 `applicationContext.getBeansOfType(...).hasSize(1)` 后进入运行期。
- 第二次测试运行失败，原因是 FIST Jackson `NumberStrModule` 会将 `long id` 序列化为字符串，断言从 `1` 调整为 `"1"` 后通过。
- `cmd /c mvn -l .codex/web-data-security-example-test-3.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am test` 为 `BUILD SUCCESS`；新示例测试 1 个用例，失败 0，错误 0，跳过 0。
- `cmd /c mvn -l .codex/web-data-security-example-format-validate.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。
- 扩展 API Doc 后，`cmd /c mvn -l .codex/web-data-security-apidoc-example-test.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am test` 为 `BUILD SUCCESS`；新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- API Doc 扩展后的格式命令 `cmd /c mvn -l .codex/web-data-security-apidoc-example-format-validate.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。

## OAuth2 Server 示例应用

本轮新增 `examples/fist-oauth2-server/example-authorization-server`，用于覆盖 OAuth2 Authorization Server 在 Spring Boot 4 Servlet 应用中的真实 metadata 和 token 请求。

受影响文件：

- `examples/pom.xml`
- `examples/README.md`
- `examples/fist-oauth2-server/pom.xml`
- `examples/fist-oauth2-server/README.md`
- `examples/fist-oauth2-server/example-authorization-server/pom.xml`
- `examples/fist-oauth2-server/example-authorization-server/src/main/java/com/power4j/fist3/examples/oauth2/server/*`
- `examples/fist-oauth2-server/example-authorization-server/src/main/resources/application.yml`
- `examples/fist-oauth2-server/example-authorization-server/src/test/java/com/power4j/fist3/examples/oauth2/server/AuthorizationServerExampleApplicationTest.java`

处理结果：

- 示例应用引入 `fist-boot-security`、`fist-boot-security-oauth2-server`、Spring Boot Web / Security / JDBC starter、Spring Authorization Server、Spring Security OAuth2 JOSE、H2 和 `commons-logging`。
- 示例使用 `OAuth2AuthorizationServerConfigurer` 装配 Authorization Server 端点，并注入 FIST 自动配置提供的 `OAuth2TokenGenerator` 与 `AuthenticationConverter`。
- 示例使用 H2 初始化 Spring Authorization Server 7 官方 `oauth2-authorization-schema.sql`，并用 `DefaultAuthorizationService` 保存授权。
- 示例 `RegisteredClient` 使用 `client_secret_basic`、`client_credentials` 和 `OAuth2TokenFormat.REFERENCE`，确保 access token 由 FIST `DefaultOAuth2AccessTokenGenerator` 生成。
- 示例测试使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 启动 Tomcat 11，并通过 `RestClient` 发送 metadata 与 token 请求。
- token 请求后通过 `authorizationService.findByToken(...)` 回查 claims，断言 `client_id`、`active` 和 license 字段存在。

影响判断：

- 这是示例和验证覆盖增强，不改变生产模块行为。
- 该示例补上了此前 OAuth2 Server 只有自动配置和服务级测试、缺少真实 HTTP endpoint 请求的覆盖缺口。
- 示例使用 H2 内存库，不依赖外部数据库、Redis 或真实账号资源。

验证结果：

- 第一次测试编译失败，原因是 AssertJ `Iterable<?>` 泛型断言无法直接匹配 `contains(String)`；改为 `asInstanceOf(LIST)` 后通过编译。
- 第二次测试运行失败，原因是示例 classpath 缺少 `commons-logging:commons-logging`，导致 `TestContextManager` 无法加载 `org.apache.commons.logging.LogFactory`；显式声明依赖后通过。
- 首次 dependency tree 命令因 `-Dincludes` 未加引号被 Maven 误解析为插件前缀，后续加引号重跑成功。
- `cmd /c mvn -l .codex/oauth2-server-example-test-3.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 为 `BUILD SUCCESS`；新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- `cmd /c mvn -l .codex/oauth2-server-example-test-4.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 回归为 `BUILD SUCCESS`；新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- `cmd /c mvn -l .codex/oauth2-server-example-test-5.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 最终回归为 `BUILD SUCCESS`；新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- `cmd /c mvn -l .codex/oauth2-server-example-test-6.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 最终断言补强后回归为 `BUILD SUCCESS`；新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- 格式命令 `cmd /c mvn -l .codex/oauth2-server-example-format-validate.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。
- 最终格式命令 `cmd /c mvn -l .codex/oauth2-server-example-format-validate-2.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。
- 最终断言补强后格式命令 `cmd /c mvn -l .codex/oauth2-server-example-format-validate-3.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。

## 本地证据

- 阶段 2 试编译日志：`.codex/trial-compile-*.log`
- OAuth2 LOB API 迁移编译日志：`.codex/oauth2-lob-compile.log`
- OAuth2 LOB API 迁移测试日志：`.codex/oauth2-lob-test.log`
- OAuth2 LOB H2 集成测试日志：`.codex/oauth2-lob-h2-test.log`
- OAuth2 LOB 格式化日志：`.codex/oauth2-lob-format-apply.log`
- OAuth2 LOB 模块全量测试日志：`.codex/oauth2-lob-module-test.log`
- OAuth2 LOB 格式校验日志：`.codex/oauth2-lob-format-validate.log`
- Web/Data Commons Lang 迁移编译日志：`.codex/web-data-commons-deprecation-compile.log`
- Web/Data Commons Lang 迁移测试日志：`.codex/web-data-commons-deprecation-test.log`
- API Doc 低风险 API 迁移编译日志：`.codex/apidoc-special-compile.log`
- API Doc 低风险 API 迁移测试日志：`.codex/apidoc-special-test.log`
- API Doc Springfox 移除编译日志：`.codex/apidoc-remove-springfox-compile.log`
- API Doc Springfox 移除测试日志：`.codex/apidoc-remove-springfox-test.log`
- MyBatis Plus Boot 4 starter 依赖树日志：`.codex/mybatis-plus-boot4-dependency-tree.log`
- MyBatis Plus Boot 4 starter 定向测试日志：`.codex/mybatis-plus-boot4-target-test.log`
- Groovy/Spock 移除定向测试日志：`.codex/groovy-removal-target-tests.log`
- Groovy/Spock 移除目标模块测试日志：`.codex/groovy-removal-module-tests.log`
- Web/Data/Security 示例验证日志：`.codex/web-data-security-example-test-3.log`
- Web/Data/Security 示例格式命令日志：`.codex/web-data-security-example-format-validate.log`
- Web/Data/Security/API Doc 示例验证日志：`.codex/web-data-security-apidoc-example-test.log`
- Web/Data/Security/API Doc 示例格式命令日志：`.codex/web-data-security-apidoc-example-format-validate.log`
- OAuth2 Server 示例验证日志：`.codex/oauth2-server-example-test-3.log`、`.codex/oauth2-server-example-test-4.log`、`.codex/oauth2-server-example-test-5.log`、`.codex/oauth2-server-example-test-6.log`
- OAuth2 Server 示例格式命令日志：`.codex/oauth2-server-example-format-validate.log`、`.codex/oauth2-server-example-format-validate-2.log`、`.codex/oauth2-server-example-format-validate-3.log`
- OAuth2 Server 示例失败记录日志：`.codex/oauth2-server-example-test.log`、`.codex/oauth2-server-example-test-2.log`
- OAuth2 Server 示例依赖树日志：`.codex/oauth2-server-example-dependency-tree-3.log`、`.codex/oauth2-server-example-full-dependency-tree.log`
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-webflux\4.0.6\spring-boot-webflux-4.0.6.jar`
- Spring Boot 官方发布记录：`https://spring.io/blog/2026/04/23/spring-boot-4-0-6-available-now`
- Spring Boot Context7 文档：`/spring-projects/spring-boot`，`DataSourceAutoConfiguration` 位于 `org.springframework.boot.jdbc.autoconfigure`
- Spring Framework Context7 文档：`/spring-projects/spring-framework/v7.0.5`，`JacksonJsonHttpMessageConverter` 使用 Jackson `JsonMapper`
- Spring Framework 当前 Javadoc Context7 文档：`/websites/spring_io_spring-framework_current_javadoc-api`，`LobCreator` / `LobHandler` 自 6.2 起弃用，替代项为 `SqlBinaryValue` / `SqlCharacterValue`
- Apache Commons Lang Context7 文档：`/websites/commons_apache_proper_commons-lang_apidocs`，`ObjectUtils.defaultIfNull(T, T)` 弃用并推荐 `ObjectUtils.getIfNull(Object, Object)`
- Spring JDBC `7.0.7` 本地 JAR：`F:\pkg-maven\repository\org\springframework\spring-jdbc\7.0.7\spring-jdbc-7.0.7.jar`，`javap` 确认 `LobHandler`、`LobCreator`、`DefaultLobHandler` 带 `@Deprecated(since="6.2")`
- Jackson Databind `2.21.3` 本地 JAR：`F:\pkg-maven\repository\com\fasterxml\jackson\core\jackson-databind\2.21.3\jackson-databind-2.21.3.jar`，`javap` 确认 `JsonNode` 同时提供 `fields()` 和 `properties()`
- Jackson Databind `3.1.2` 本地 JAR：`F:\pkg-maven\repository\tools\jackson\core\jackson-databind\3.1.2\jackson-databind-3.1.2.jar`，`javap` 确认 `JsonNode` 保留 `properties()` 且不再提供 `fields()`
- MyBatis Plus `3.5.16` 本地 JAR：`F:\pkg-maven\repository\com\baomidou\mybatis-plus-spring-boot4-starter\3.5.16\mybatis-plus-spring-boot4-starter-3.5.16.jar`
- MyBatis Plus Context7 文档：`/baomidou/mybatis-plus-doc`，Spring Boot 4 及以上版本使用 `mybatis-plus-spring-boot4-starter`
- Redisson Context7 文档：`/redisson/redisson`，未直接覆盖 `RBlockingQueue.subscribeOnElements` 替代项。
- Redisson `4.3.1` 本地 JAR：`F:\pkg-maven\repository\org\redisson\redisson\4.3.1\redisson-4.3.1.jar`，`javap -v` 确认 `subscribeOnElements(Consumer<V>)` 废弃，`subscribeOnElements(Function<V, CompletionStage<Void>>)` 未废弃。
- Redisson `4.3.1` 本地 JAR：`F:\pkg-maven\repository\org\redisson\redisson\4.3.1\redisson-4.3.1.jar`，`javap -v` 确认 `BaseConfig#setPassword(String)` 已废弃。
- Spring Security `7.0.5` 本地 JAR：`F:\pkg-maven\repository\org\springframework\security\spring-security-core\7.0.5\spring-security-core-7.0.5.jar`
- Spring Authorization Server `7.0.5` 本地 JAR：`F:\pkg-maven\repository\org\springframework\security\spring-security-oauth2-authorization-server\7.0.5\spring-security-oauth2-authorization-server-7.0.5.jar`，确认内置 `org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql`。
- Spring Authorization Server Context7 文档：`/spring-projects/spring-authorization-server`，确认自定义 token 生成应通过 `OAuth2TokenGenerator` / `DelegatingOAuth2TokenGenerator` 配置。
- Spring Authorization Server Context7 文档：`/spring-projects/spring-authorization-server`，确认自定义 Authorization Server 配置可通过 `OAuth2AuthorizationServerConfigurer` 注入 repository、authorization service、settings 和 token generator。
- JSpecify Context7 文档：`/jspecify/jspecify`，确认 `@Nullable` 可用于返回值、参数、泛型类型参数和数组维度。
- OpenRewrite dry-run patch：`target/rewrite/rewrite.patch`

## 内部应用配置加载影响修正

记录日期：2026-05-14；记录者：Codex。

内部应用配置验证不再以 Nacos 导入为首选路线。Spring Boot 4 的 Config Data 机制允许通过 `spring.config.import` 导入本地文件或配置树。内部应用虽然保留了 `config-server-spring` profile，但本地没有可用 Spring Cloud Config Server，该路径属于遗留配置，不作为当前升级验证路线。

影响判断：

- FIST Kit v4 本仓库不重新引入 Nacos 或 Spring Cloud Alibaba；库侧结论仍保持「未实际使用，应用侧按需显式引入」。
- 内部应用 Boot 4 验证需要覆盖配置中心等价输入，但不要求首轮使用 Nacos。
- `spring-boot-properties-migrator` 的实际价值仍在应用启动阶段，需要让它看到完整配置输入；本地 `file:` 导入可以满足首轮诊断需要。

建议的配置源优先级：

1. `file:` 导入配置模板：改动最快，适合首轮 migrator 诊断和启动错误排查。
2. Nacos：放在最终回归阶段，验证生产式配置路径。

需要避免的做法：

- 不把 Nacos 配置导入作为当前阶段阻塞条件。
- 不再投入 Spring Cloud Config Server native/Git 后端验证，除非内部项目重新启用该配置中心路径。
- 不在公共调研文档中记录 `.env.local` 的值、数据库连接串、配置中心 namespace / group 或配置模板原文。
- 不把 `spring-boot-properties-migrator` 写入正式 BOM、starter 或可发布依赖。

首轮内部应用验证补充：

- `fist-monitor` 可以通过 `boot4-local` profile、`spring.config.import=optional:file:...` 和 `spring-boot:start spring-boot:stop` 完成本地配置模板启动探测。
- 该验证只证明本地 Config Data 输入和 Spring 环境构建可行，不代表 Nacos Config、Nacos Discovery 或完整 Web 运行路径已经通过。
- 本地验证阶段需要显式关闭 Nacos Config import check 和服务注册发现，否则 Nacos Config starter 会要求存在 `nacos:` import。
- 在不修改内部应用 POM 的情况下，可以通过 `spring-boot.run.additional-classpath-elements` 临时追加 `spring-boot-properties-migrator`；但必须同时追加 `spring-boot-configuration-metadata` 和 `android-json` 运行时依赖。
- Boot 3.5.11 基线下的 migrator 机制验证已通过，未出现配置迁移诊断；Boot 4 最终诊断仍需等内部应用依赖切换到 Boot 4 后使用匹配版本重跑。
- 在临时副本中将 `fist-monitor` 最小链路切到 Boot 4.0.6、Spring Cloud 2025.1.1、Spring Cloud Alibaba 2025.1.0.0 和 FIST Kit 4.0.0-SNAPSHOT 后，`fist-common` 与 `fist-monitor` 可完成 `test-compile`。
- 试编译暴露出 reactor 级影响：其他模块仍引用 `spring-boot-starter-undertow`，Boot 4 parent 不再管理该旧 starter，导致未收窄 reactor 时 Maven 在项目读取阶段失败。
- FIST Kit v4 SNAPSHOT 应先安装到本地 Maven 仓库，否则内部应用临时试编译会尝试从远程仓库解析未发布组件。
- `fist-monitor` 在 Boot 4 临时副本中可完成非 Web 启动/停止探测；追加 Boot 4.0.6 migrator 后仍可启动，并且未输出配置迁移诊断。
- `fist-monitor` 在 Boot 4 临时副本中也可完成 Servlet Web 模式启动/停止探测；Tomcat 11、随机应用端口、随机管理端口和 15 个 Actuator endpoint 均可装配启动。
- Web 模式下新增发现 classpath 缺少 Bean Validation provider，日志提示 `jakarta.validation.NoProviderFoundException`。该问题当前不阻塞 `fist-monitor` 启动，但可能影响请求参数校验、方法校验、配置属性校验或管理上下文校验，需要在内部应用依赖图中确认 `hibernate-validator` 的引入方式。
- 依赖树确认 `spring-boot-starter-web:4.0.6` 不再为 `fist-monitor` 带入 Bean Validation provider；临时加入 `spring-boot-starter-validation` 后，可带入 `spring-boot-validation:4.0.6` 和 `hibernate-validator:9.0.1.Final`，Web 启动和带 migrator 的 Web 启动均通过且不再出现新的 provider 缺失提示。
- FIST Kit 侧 `fist-support-web` 当前只声明 `jakarta.validation-api`，不强制携带 provider；内部应用正式升级时应在应用侧或应用父 POM 中显式声明 `spring-boot-starter-validation`。
- 内部应用中 `fist-iam`、`fist-auth-v3` 和 `demo-order` 的默认打包 profile 仍声明 `spring-boot-starter-undertow`。Boot 4.0.6 dependency management 不再管理该 starter，Maven 会在项目读取阶段报缺少版本。
- Spring Boot 4 文档和本地 BOM 均指向 Tomcat / Jetty 路线；Servlet Web 默认容器是 Tomcat，Jetty 是明确替换默认容器时的候选项。临时副本中曾将 3 处 Undertow 替换为 `spring-boot-starter-jetty` 验证阻塞可消除；真实内部项目已按项目策略改为 `pkg-all` 默认打包 Tomcat。
- `fist-auth-v3` 和 `demo-order` 在临时替换容器后仍有内部 SNAPSHOT POM 未安装 warning；这不是 Undertow 阻塞，后续全量编译前需要继续补齐 FIST Kit v4 和内部模块本地安装。
- 当前启动验证没有覆盖真实 `.env.local`、数据库、Redis、Nacos、真实业务 HTTP 接口或生产配置中心路径；结论只覆盖本地配置模板输入下的编译、非 Web 启动、Web 容器启动和管理端点装配。
- `fist-auth-v3` 与 `fist-iam` 主代码试编译已推进通过，说明 Boot 4 / FIST Kit v4 组合下的主要应用层编译阻塞集中在依赖坐标、Spring Security 7、Spring Authorization Server 7、Jackson 3 和测试自动配置包迁移。
- Spring Authorization Server 7 中 `OAuth2AuthorizationServerConfigurer` 已迁移到 Spring Security 包 `org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization`；旧包 `org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers` 不能继续使用。
- Spring Security 7 的 `SecurityConfigurer#init` 签名变化会影响继承 `AbstractHttpConfigurer` 的自定义配置器；旧代码中声明 `throws Exception` 的 `init(HttpSecurity)` 需要调整。
- FIST Kit v4 的 `DefaultAuthorizationService` 已移除旧 `LobHandler` 构造器和 RowMapper `setLobHandler` 路线；内部应用中继承并覆盖授权服务 RowMapper 的代码需要重做，而不是简单保留 Boot 3 写法。
- Boot 4 测试源码需要同步迁移自动配置包；`org.springframework.boot.autoconfigure.jdbc`、`org.springframework.boot.autoconfigure.sql.init`、`org.springframework.boot.autoconfigure.jackson` 等 Boot 3 包路径在当前测试编译中已失效。
- 由于 `fist-auth-v3` 的授权服务持久化包含自定义 `AuthUser` 反序列化和 Jackson mixin，Jackson 3 迁移不能只以主代码编译通过为准，后续必须补充授权数据读写和历史数据兼容验证。
