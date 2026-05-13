# Spring Boot 4 只改版本试编译

日期：2026-05-10
执行者：Codex
阶段：阶段 2「只改版本试编译」

## 结论摘要

阶段 2 已完成第一轮只改版本试编译。当前候选版本组合可以完成 Maven BOM 解析，但不能完成全量编译。

第一轮 Maven 模型构建先暴露 Spring Cloud Gateway 旧坐标问题。将 Gateway 坐标调整为 Spring Cloud `2025.1.x` 管理的新 artifactId 后，构建进入 Java 编译阶段。

当前第一阻塞点是 `fist-kit-app/fist-web/fist-support-web` 的 WebFlux 错误处理 API 包迁移。Spring Boot 4 中相关类已从旧包迁移到新包：

| 类 | 当前代码引用 | Spring Boot 4.0.6 JAR 中位置 |
| --- | --- | --- |
| `DefaultErrorAttributes` | `org.springframework.boot.web.reactive.error.DefaultErrorAttributes` | `org.springframework.boot.webflux.error.DefaultErrorAttributes` |
| `ErrorAttributes` | `org.springframework.boot.web.reactive.error.ErrorAttributes` | `org.springframework.boot.webflux.error.ErrorAttributes` |
| `ErrorWebFluxAutoConfiguration` | `org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration` | `org.springframework.boot.webflux.autoconfigure.error.ErrorWebFluxAutoConfiguration` |

该阻塞点会传递影响 `fist-support-security`、OAuth2 Server、MyBatis CRUD、Cloud OpenFeign 和 Cloud Gateway 等依赖 Web 支撑模块的编译。非 Web 支撑链路中，Redisson、Logback、MyBatis Plus、SDE starter、support-data 和 API Doc 已通过 `compile`。

## 本轮版本变更

本轮只修改版本属性和明显缺失的 Gateway artifactId，不迁移 Java 源码，不应用 OpenRewrite 补丁。

### 版本属性

| 属性 | 原版本 | 试编译版本 |
| --- | --- | --- |
| `spring-boot.version` | `3.5.14` | `4.0.6` |
| `spring-cloud.version` | `2025.0.2` | `2025.1.1` |
| `spring-cloud-alibaba.version` | `2023.0.3.4` | `2025.1.0.0`，后续复查为未使用并移除 |
| `redisson.version` | `3.52.0` | `4.3.1` |
| `springdoc-openapi.version` | `2.8.17` | `3.0.3` |
| `spring-boot-admin.version` | `3.5.8` | `4.0.4` |
| `spring.oauth2-authorization-server.version` | `1.5.7` | `7.0.5` |

暂不修改：

- `mybatis-plus.version`：保持 `3.5.16`。
- `spock-lib.version`：已随去 Groovy 任务删除。
- `spring-security-jwt.version`：保持 `1.1.1.RELEASE`，后续按退役任务删除。

后续试运行修正：

| 属性 | 原试编译版本 | 修正版本 | 原因 |
| --- | --- | --- | --- |
| `mybatis-spring.version` | `3.0.5` | `4.0.0` | MyBatis Plus Boot 4 starter 依赖 `mybatis-spring` 4.0.0，适配 Spring Framework 7 |

### Gateway 坐标

| 模块 | 原坐标 | 试编译坐标 |
| --- | --- | --- |
| `fist-gateway-auth-core` | `spring-cloud-gateway-server` | `spring-cloud-gateway-server-webflux` |
| `fist-cloud-gateway-security-reactive` | `spring-cloud-starter-gateway` | `spring-cloud-starter-gateway-server-webflux` |
| `fist-cloud-gateway-acl` | `spring-cloud-starter-gateway` | `spring-cloud-starter-gateway-server-webflux` |

这些坐标来自 Spring Cloud `2025.1.x` 依赖管理与 OpenRewrite dry-run 输出，属于阶段 2 为继续试编译所需的最小坐标修正。

## 执行记录

### Cloud OpenFeign 第一轮

命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests compile
```

结果：失败，未进入 Java 编译。

失败类型：Maven 模型构建失败。

关键信息：

- `spring-cloud-gateway-server` 缺少版本。
- `spring-cloud-starter-gateway` 缺少版本。

判断：Spring Cloud `2025.1.1` 不再管理这些旧 Gateway artifactId，需要迁移到 WebFlux 后缀的新坐标。

### Cloud OpenFeign 第二轮

命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests compile
```

结果：失败，已进入 Java 编译。

失败模块：`fist-kit-app/fist-web/fist-support-web`

失败类型：Spring Boot WebFlux 错误处理 API 包迁移。

受影响文件：

- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/reactive/error/GlobalErrorAttributes.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/reactive/error/GlobalErrorConfigure.java`
- `fist-kit-app/fist-web/fist-support-web/src/main/java/com/power4j/fist/boot/web/reactive/error/GlobalErrorWebExceptionHandler.java`

### 模块级结果

| 命令 | 结果 | 结论 |
| --- | --- | --- |
| `.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am -DskipTests compile` | 通过 | Redisson `4.3.1` 至少可完成主代码编译 |
| `.\mvnw.cmd -pl fist-kit-infra/fist-logback -am -DskipTests compile` | 通过 | Logback 模块未被 Boot 4 阻塞 |
| `.\mvnw.cmd -pl fist-kit-infra/fist-mybatis-plus -am -DskipTests compile` | 通过 | 基础 MyBatis Plus 支撑模块可编译 |
| `.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am -DskipTests compile` | 通过 | SDE core、extra、web、client、boot-starter 主代码可编译 |
| `.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am -DskipTests compile` | 通过 | support-data 主代码可编译 |
| `.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests compile` | 通过 | SpringDoc `3.0.3` 下 API Doc 主代码可编译，但仍有 Springfox 相关过时警告 |
| `.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am -DskipTests compile` | 失败 | 被 `fist-support-web` 阻塞，未进入 Security 编译 |
| `.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests compile` | 失败 | 被 `fist-support-web` 阻塞，未进入 OAuth2 Server 编译 |
| `.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests compile` | 失败 | 被 `fist-support-web` 阻塞，未进入 MyBatis CRUD 编译 |
| `.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests compile` | 失败 | 第二轮被 `fist-support-web` 阻塞，未进入 OpenFeign 编译 |

## 当前错误归类

### P0：Boot WebFlux 包迁移

当前失败不是业务逻辑错误，而是 Spring Boot 4 模块化后的包路径变化。经本地 JAR 检查，Spring Boot `4.0.6` 的 `spring-boot-webflux` JAR 中存在以下类：

```text
org/springframework/boot/webflux/autoconfigure/error/ErrorWebFluxAutoConfiguration.class
org/springframework/boot/webflux/error/DefaultErrorAttributes.class
org/springframework/boot/webflux/error/ErrorAttributes.class
```

推荐下一步：

1. 将 3 个 WebFlux 错误处理类的 import 更新到 Boot 4 新包。
2. 重新运行 `fist-support-web` 编译。
3. 再继续 Security、OAuth2 Server、MyBatis CRUD、Cloud OpenFeign 和 Cloud Gateway 试编译。

### P1：Gateway 坐标迁移

Gateway 坐标已完成最小修正，后续仍需验证：

- Gateway WebFlux starter 是否能满足当前 Gateway 自动配置和测试夹具。
- Sentinel Gateway adapter 曾列为待验证项；后续复查确认本仓库未实际使用 Sentinel，已改为移除 Spring Cloud Alibaba / Sentinel 依赖。
- Gateway ACL、Reactive Security 与 LoadBalancer 是否存在 API 变化。

### P1：API Doc 与 Springfox

`fist-boot-apidoc` 主代码可编译，但编译输出仍有过时 API 警告，且 OpenRewrite dry-run 中提示会删除 Springfox 依赖。该模块需要后续单独决策：

- 保留 SpringDoc 3 主路径。
- 删除或隔离 Springfox 插件代码。
- 检查用户可见 API 文档能力是否变化。

### P2：Redisson 与 MyBatis Plus

Redisson `4.3.1`、MyBatis Plus `3.5.16` 当前只通过主代码 `compile`，不代表运行时自动配置和测试通过。后续仍需执行模块测试。

## 与 OpenRewrite 的对照

OpenRewrite dry-run 能识别 Gateway artifactId 迁移，但未识别本次暴露的 WebFlux 错误处理包迁移。因此后续源码影响清单不能只依赖 OpenRewrite，需要继续以编译错误为主输入。

## 阶段 2 后续进展

阶段 4 已处理 `fist-support-web` 的 WebFlux 错误处理包迁移，`fist-support-web` 主代码编译通过。随后处理 `fist-support-security` 暴露的 Spring Security 7 与 WebClient API 变化：

- `Sm3PasswordEncoder` 改为实现 `encodedNonNullPassword(CharSequence, byte[])`。
- `DefaultReactiveOpaqueTokenIntrospector` 从 `WebClient.exchange()` 迁移到 `exchangeToMono(...)`。
- 对应测试桩同步迁移到 `exchangeToMono(...)`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am "-Dtest=Sm3PasswordEncoderTest,PasswordEncoderUtilTest,DefaultReactiveOpaqueTokenIntrospectorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`BUILD SUCCESS`，定向测试共 14 个用例，失败 0。

继续运行 OAuth2 Server、MyBatis CRUD、Cloud OpenFeign 等依赖链路后，新的 P0 阻塞点为 `fist-boot-web-app` 的 Jackson 自动配置迁移：

- 旧包 `org.springframework.boot.autoconfigure.jackson` 不再提供 `Jackson2ObjectMapperBuilderCustomizer` 和 `JacksonAutoConfiguration`。
- Spring Boot `4.0.6` 本地 JAR 中存在 `org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration` 和 `JsonMapperBuilderCustomizer`。
- 该迁移牵涉 Jackson 3 的 `tools.jackson.*` 类型，不能只做 import 替换。

该阻塞点已完成最小迁移：

- `fist-jackson` 切换到 Jackson 3 `tools.jackson.core:jackson-databind`，并显式保留 Jackson annotations。
- `DateTimeModule`、`NumberStrModule`、混淆序列化支持和 `JacksonConfig` 已迁移到 `JsonMapper.Builder` 相关 API。
- `fist-boot-web-app` 依赖链主代码编译通过。
- `fist-jackson` 定向测试共 8 个用例，失败 0。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-infra/fist-jackson -am "-Dtest=JacksonSimpleStringObfuscateTest,SimpleStringObfuscateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

继续运行 OAuth2 Server 后，新的 P0 阻塞点为 Spring Security 7 / Authorization Server 7 移除 password grant 相关便捷常量：

- `AuthorizationGrantType.PASSWORD`
- `OAuth2ParameterNames.USERNAME`
- `OAuth2ParameterNames.PASSWORD`

受影响模块：

```text
fist-kit-app/fist-security/fist-boot-security-oauth2-server
```

该阻塞点已完成最小迁移：

- `AuthConstants` 显式定义 password grant 类型值与 `username`、`password` 参数名。
- password grant converter、provider、endpoint helper 与定向测试不再依赖 Spring Security 7 已移除的便捷常量。
- 继续保留当前自定义 Resource Owner Password Grant 扩展的行为，不在本轮调研中扩大为授权模式退役。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=OAuth2ResourceOwnerBaseAuthenticationProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests compile
```

结果：上述命令均为 `BUILD SUCCESS`。OAuth2 Server 定向测试共 1 个用例，失败 0。编译期间仍存在 `MappingJackson2HttpMessageConverter`、`SecurityJackson2Modules`、`OAuth2AuthorizationServerJackson2Module` 等待删除警告。

继续运行 Cloud Gateway 试编译后，新的 P0 阻塞点为 Spring Framework 7 `HttpHeaders` 不再暴露 Map 风格访问方法：

- `HttpHeaders.keySet()`
- `HttpHeaders.containsKey(String)`

受影响模块：

```text
fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core
```

失败文件：

```text
fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core/src/main/java/com/power4j/fist/cloud/gateway/AuthUtils.java
```

该阻塞点已完成最小迁移：

- `AuthUtils` 使用 `HttpHeaders.containsHeader(String)` 判断 header 是否存在，保留原有 header 删除行为。
- 继续编译 Gateway Security Reactive 后，Spring Security 7 暴露新的接口签名变化：`ReactiveAuthorizationManager` 要求实现 `authorize(...) -> Mono<AuthorizationResult>`。
- `GatewayAuthorizationManager` 已迁移到 `authorize(...)`，授权结果仍复用 `AuthorizationDecision`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests compile
```

结果：Gateway Auth Core、Gateway ACL、Gateway Security Reactive 主代码编译均为 `BUILD SUCCESS`；三个 Gateway 子模块组合编译为 `BUILD SUCCESS`。

继续执行 Gateway 三个子模块组合 `test-compile` 后，测试源码暴露两类迁移问题：

- `HttpBinCompatibleController` 中 `Collectors.toMap(..., HttpHeaders::new)` 不再适配 Spring Framework 7 的 `HttpHeaders` 类型。
- `PermitAllSecurityConfiguration` 中旧式 `authorizeExchange().anyExchange().permitAll().and().csrf().disable()` 链式 DSL 不再适配 Spring Security 7。

该阻塞点已完成最小迁移：

- 测试控制器显式创建 `HttpHeaders` 并通过 `addAll(...)` 复制 `X-Test-*` header。
- 测试安全配置使用 `authorizeExchange(exchange -> ...)` 和 `csrf(ServerHttpSecurity.CsrfSpec::disable)`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests test-compile
```

结果：Gateway 三个子模块组合 `test-compile` 为 `BUILD SUCCESS`。

继续执行 OAuth2 Server 与 Cloud OpenFeign `test-compile`：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests test-compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests test-compile
```

结果：两条链路均为 `BUILD SUCCESS`。

继续执行 MyBatis CRUD `test-compile` 后，测试源码暴露 Spring Boot 4 JDBC 测试模块拆分问题：

- `AutoConfigureTestDatabase` 不再位于 `org.springframework.boot.test.autoconfigure.jdbc`。
- Spring Boot 4 中该注解位于 `org.springframework.boot.jdbc.test.autoconfigure`，并由 `org.springframework.boot:spring-boot-jdbc-test` 提供。

该阻塞点已完成最小迁移：

- `fist-boot-crud-mybatis` 增加 `spring-boot-jdbc-test` 测试依赖。
- `QueryTest`、`UpdateTest`、`AbstractNodeIdxSupportTest` 的 import 更新到新包。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests test-compile
```

结果：MyBatis CRUD `test-compile` 为 `BUILD SUCCESS`。

随后尝试运行受影响测试：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am "-Dtest=QueryTest,UpdateTest,AbstractNodeIdxSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：第一轮失败。运行期阻塞为 `ApplicationContext` 无法创建 MyBatis mapper，根因栈为 `Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`。

根因定位：

- Spring Boot 4 的 `DataSourceAutoConfiguration` 位于 `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`。
- MyBatis Plus `3.5.16` 的 `mybatis-plus-spring-boot3-starter` 仍将 `MybatisPlusAutoConfiguration` 排在旧包 `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` 之后。
- 在 Boot 4 下旧包顺序失效，`MybatisPlusAutoConfiguration` 的 `@ConditionalOnSingleCandidate(DataSource.class)` 过早评估，因此没有创建 `SqlSessionFactory`。

修正：

- `fist-boot-crud-mybatis` 改用 `mybatis-plus-spring-boot4-starter`。
- 测试依赖改用 `mybatis-plus-spring-boot4-starter-test`。
- 根 `mybatis-spring.version` 升级到 `4.0.0`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests test-compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am "-Dtest=QueryTest,UpdateTest,AbstractNodeIdxSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`test-compile` 为 `BUILD SUCCESS`；定向测试共 12 个用例，失败 0。

下游可见影响：

- `fist-boot-crud-mybatis` 暴露的 MyBatis Plus starter 从 Boot 3 坐标切换为 Boot 4 坐标。
- 下游工程如果显式声明 `mybatis-plus-spring-boot3-starter` 或 `mybatis-plus-spring-boot3-starter-test`，升级 Boot 4 时需要切换到 `mybatis-plus-spring-boot4-starter` 和 `mybatis-plus-spring-boot4-starter-test`。
- 下游工程如果锁定 `mybatis-spring` 3.x，需要升级到 `4.0.0` 以匹配 Spring Framework 7。
- Context7 `/baomidou/mybatis-plus-doc` 文档确认 Spring Boot 4 及以上版本使用 `mybatis-plus-spring-boot4-starter`，该 starter 自 MyBatis Plus `3.5.13` 开始提供。
- 依赖树验证显示当前模块解析到 `mybatis-plus-spring-boot4-starter:3.5.16`、`mybatis-plus-spring-boot4-starter-test:3.5.16` 和 `mybatis-spring:4.0.0`，未解析到 Boot 3 starter。

继续处理 OAuth2 Server 的 Jackson 2 待删除 API 警告：

- `DefaultOauthFailureHandler` 使用的 `MappingJackson2HttpMessageConverter` 已迁移到 Spring Framework 7 的 `JacksonJsonHttpMessageConverter`。
- `DefaultAuthorizationService` 的授权属性与 token metadata 序列化已从 `com.fasterxml.jackson.*`、`SecurityJackson2Modules`、`OAuth2AuthorizationServerJackson2Module` 迁移到 Jackson 3 `tools.jackson.*`、`SecurityJacksonModules`、`OAuth2AuthorizationServerJacksonModule`。
- 新增 `DefaultAuthorizationServiceTest`，覆盖包含 Spring Security principal 的授权属性序列化。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,OAuth2ResourceOwnerBaseAuthenticationProviderTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
```

结果：OAuth2 Server 依赖链主代码编译通过；定向测试共 4 个用例，失败 0；deprecation 明细编译为 `BUILD SUCCESS`，本轮 Jackson 2 旧 API 关键字在日志中未命中。

继续处理 `fist-support-spring` 的包级空值注解 deprecation 警告：

- `fist-support-spring` 增加 `org.jspecify:jspecify` 依赖。
- 5 个 `package-info.java` 从 `@NonNullApi` + `@NonNullFields` 迁移到 JSpecify `@NullMarked`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am test
```

结果：`fist-support-spring` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；模块测试共 5 个用例，失败 0。

继续处理 `fist-support-web` 的包级空值注解 deprecation 警告：

- `fist-support-web` 增加 `org.jspecify:jspecify` 依赖。
- 12 个 `package-info.java` 从 `@NonNullApi` + `@NonNullFields` 迁移到 JSpecify `@NullMarked`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am test
```

结果：`fist-support-web` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；模块测试共 4 个用例，失败 0。本次 `test` 未实际运行已记录的 `PathMatcherTest.groovy`，后续去 Groovy 任务需要单独确认。

继续处理 Security 相关模块的包级空值注解 deprecation 警告：

- `fist-support-security` 增加 `org.jspecify:jspecify` 依赖，11 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-boot-security-oauth2-server` 增加 `org.jspecify:jspecify` 依赖，12 个 `package-info.java` 迁移到 `@NullMarked`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security,fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,OAuth2ResourceOwnerBaseAuthenticationProviderTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`fist-support-security` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；OAuth2 Server deprecation 明细编译为 `BUILD SUCCESS`，剩余 `NonNullApi` / `NonNullFields` 命中来自依赖链上的 `fist-boot-web-app` 与 `fist-support-data`。`fist-support-security` 模块测试共 17 个用例，失败 0；OAuth2 Server 定向测试共 4 个用例，失败 0。

继续处理 Web App 与 Support Data 的包级空值注解 deprecation 警告：

- `fist-boot-web-app` 增加 `org.jspecify:jspecify` 依赖，1 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-support-data` 增加 `org.jspecify:jspecify` 依赖，11 个 `package-info.java` 迁移到 `@NullMarked`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
```

结果：`fist-boot-web-app` 与 `fist-support-data` deprecation 明细编译均为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；`fist-boot-web-app` 模块自身没有测试源码；`fist-support-data` 模块测试共 6 个用例，失败 0；OAuth2 Server 依赖链 deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`。本次 `fist-support-data -am test` 未实际运行已记录的 Groovy 测试文件，后续去 Groovy 任务需要单独确认。

继续处理数据链路剩余的包级空值注解 deprecation 警告：

- `fist-boot-data` 增加 `org.jspecify:jspecify` 依赖，4 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-boot-crud-mybatis` 增加 `org.jspecify:jspecify` 依赖，8 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解，方法级 `org.springframework.lang.Nullable` deprecation 后续按显式可空契约单独迁移。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis,fist-kit-app/fist-data/fist-boot-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am "-Dtest=QueryTest,UpdateTest,AbstractNodeIdxSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am test
```

结果：`fist-boot-data` 与 `fist-boot-crud-mybatis` deprecation 明细编译均为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；`fist-boot-data` 模块测试共 2 个用例，失败 0；MyBatis CRUD 定向测试共 12 个用例，失败 0；`fist-boot-crud-mybatis` 模块全量测试共 14 个用例，失败 0。

继续处理 Gateway 模块的包级空值注解 deprecation 警告：

- `fist-gateway-auth-core` 增加 `org.jspecify:jspecify` 依赖，5 个 `package-info.java` 迁移到 `@NullMarked`。
- `fist-cloud-gateway-acl` 增加 `org.jspecify:jspecify` 依赖，2 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解，`AuthContext`、`AuthState` 中的方法级 `org.springframework.lang.Nullable` deprecation 后续按显式可空契约单独迁移。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests test-compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am test
```

结果：Gateway Auth Core 与 Gateway ACL deprecation 明细编译均为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；Gateway 三个子模块组合 `test-compile` 为 `BUILD SUCCESS`；Gateway Auth Core 模块测试共 14 个用例，失败 0；Gateway ACL 模块测试共 6 个用例，失败 0。

继续处理 Boot Security 模块的包级空值注解 deprecation 警告：

- `fist-boot-security` 增加 `org.jspecify:jspecify` 依赖，2 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解，`UserConverter` 中的方法级 `org.springframework.lang.Nullable` deprecation 后续按显式可空契约单独迁移。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am test
```

结果：`fist-boot-security` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；`fist-boot-security -am test` 为 `BUILD SUCCESS`，模块自身没有测试源码，依赖链中 `fist-support-security` 测试共 17 个用例，失败 0。

继续处理 Cloud RPC Feign 模块的包级空值注解 deprecation 警告：

- `fist-cloud-rpc-feign` 增加 `org.jspecify:jspecify` 依赖，3 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解，Feign error handler 与 relay handler 中的方法级 `org.springframework.lang.Nullable` deprecation 后续按显式可空契约单独迁移。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests test-compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am test
```

结果：Cloud RPC Feign deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；`test-compile` 为 `BUILD SUCCESS`；`fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`，模块自身测试共 7 个用例，失败 0。

继续处理 API Doc 模块的包级空值注解 deprecation 警告：

- `fist-boot-apidoc` 增加 `org.jspecify:jspecify` 依赖，3 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解，方法级 `org.springframework.lang.Nullable`、Jackson 2 `JsonNode.fields()` 和 Springfox 退役相关警告后续按 API Doc 专项迁移。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am test
```

结果：API Doc deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；`fist-boot-apidoc -am test` 为 `BUILD SUCCESS`，模块自身没有测试源码。

继续处理 Redisson 模块的包级空值注解 deprecation 警告：

- `fist-redisson` 增加 `org.jspecify:jspecify` 依赖，5 个 `package-info.java` 迁移到 `@NullMarked`。
- 本轮仍只处理包级默认非空注解，Redisson bean processor、publisher、queue writer 中的方法级 `org.springframework.lang.Nullable` 和 `RBlockingQueue.subscribeOnElements` deprecation 后续单独迁移。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-redisson spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am test
```

结果：Redisson deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `NonNullApi` / `NonNullFields`；`fist-redisson -am test` 为 `BUILD SUCCESS`，模块自身没有测试源码；全仓 `package-info.java` 复扫未命中 `NonNullApi` / `NonNullFields`。

继续处理 `fist-support-spring` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- 返回值迁移到类型使用位置，例如 `public @Nullable ApplicationContext peek()` 与 `public <T> @Nullable T eval(...)`。
- 可空数组参数使用 `Object @Nullable []`，表达数组本身可为 `null`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-support-spring -am test
```

结果：`fist-support-spring` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`；`fist-support-spring -am test` 为 `BUILD SUCCESS`，模块测试共 5 个用例，失败 0。

继续处理 `fist-boot-security` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- `UserConverter` 从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- `convert(Authentication)` 返回值改为 `@Nullable U`，保留「无用户信息返回 `null`」的公共接口契约。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security -am test
```

结果：`fist-boot-security` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`。`fist-boot-security -am test` 为 `BUILD SUCCESS`，依赖链测试共 40 个用例，失败 0，模块自身没有测试源码。编译日志仍有依赖链上的 `fist-support-web` 与 `fist-support-security` 方法级 `org.springframework.lang.Nullable` 警告，后续按模块继续迁移。

继续处理 `fist-support-security` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 13 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- 字段、返回值和泛型返回值迁移到类型使用位置，例如 `private @Nullable String id`、`public @Nullable String resolve(...)`、`default <A> @Nullable A getAttribute(...)`。
- `final` 参数使用 `final @Nullable Type` 写法。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-support-security -am test
```

结果：`fist-support-security` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`。`fist-support-security -am test` 为 `BUILD SUCCESS`，模块自身测试共 17 个用例，失败 0，依赖链测试共 40 个用例，失败 0。编译日志仍有依赖链上的 `fist-support-web` 方法级 `org.springframework.lang.Nullable` 警告，后续按 Web 支撑模块继续迁移。

继续处理 `fist-support-web` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 20 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- DTO、事件对象和异常类字段迁移到类型使用位置。
- `Breaker` 中可空数组参数使用 `Object @Nullable []`，表达数组本身可为 `null`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am test
```

结果：`fist-support-web` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。剩余 deprecation 明细为 `Breaker` 自身缺少 `@Deprecated` 标注和 Apache Commons Lang `ObjectUtils.defaultIfNull(...)`。`fist-support-web -am test` 为 `BUILD SUCCESS`，模块自身 Java 测试共 4 个用例，失败 0；本次 `test` 仍未实际运行已记录的 `PathMatcherTest.groovy`。

继续处理 `fist-support-data` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 10 个主代码 Java 文件和 2 个测试夹具文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- 字段、返回值和泛型容器迁移到类型使用位置，例如 `private @Nullable List<HandlerInfo<T>> entry`、`public @Nullable OpHandler<T> getHandler()`、`TransmittableThreadLocal<@Nullable String>`。
- `final` 参数使用 `final @Nullable Type` 写法，保留原有可传入 `null` 的租户和错误处理器契约。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-support-data -am test
```

结果：`fist-support-data` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。剩余 deprecation 明细为 Apache Commons Lang `Validate.notNull(...)`、`MutableObject.getValue()`、`ObjectUtils.defaultIfNull(...)` 和既有 `ValidateUtil` 注解问题。`fist-support-data -am test` 为 `BUILD SUCCESS`，模块自身 Java 测试共 6 个用例，失败 0；本次 `test` 仍未实际运行已记录的 3 个 `.groovy` 测试文件。

继续处理 `fist-boot-data` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- Spring MVC 参数解析器覆写方法的返回值改为类型使用位置，例如 `public @Nullable Object resolveArgument(...)`。
- 分页参数模型字段迁移到类型使用位置，例如 `private @Nullable String field` 与 `private @Nullable String order`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-data -am test
```

结果：`fist-boot-data` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。剩余 deprecation 明细为既有 `InTenant` deprecation、Apache Commons Lang `ObjectUtils.defaultIfNull(...)` / `StringUtils.equalsIgnoreCase(...)` 和 `PageData` unchecked 使用。`fist-boot-data -am test` 为 `BUILD SUCCESS`，模块自身测试共 2 个用例，失败 0，依赖链 `fist-support-data` 测试共 6 个用例，失败 0。

继续处理 `fist-cloud-rpc-feign` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- `RelayHandler`、`HeaderRelayHandler`、`UserRelayHandler` 保留 `HttpServletRequest` 可为 `null` 的接口契约。
- `FeignErrorHandler` 的可选 `MessageSourceAccessor` 与 `LocaleResolver` 字段迁移到类型使用位置。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am test
```

结果：`fist-cloud-rpc-feign` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。编译日志中仍有依赖链上的 `fist-boot-web-app` 方法级 `org.springframework.lang.Nullable` 警告。`fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`，模块自身测试共 7 个用例，失败 0；测试编译仍提示 `SecureFeignEncoderPrototypeTest` 使用或覆盖过时 API，后续纳入 Feign/SDE 专项。

继续处理 `fist-boot-web-app` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 2 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- `JacksonCustomizeProperties` 的可选配置属性字段迁移到类型使用位置。
- `MessageConfiguration` 的可选注入字段保留可空契约，setter 参数继续允许 `null`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-boot-web-app -am test
```

结果：`fist-boot-web-app` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。剩余 deprecation 明细为既有 `Breaker` 和 Apache Commons Lang `ObjectUtils.defaultIfNull(...)`。`fist-boot-web-app -am test` 为 `BUILD SUCCESS`，模块自身没有测试源码，依赖链 `fist-support-web` 测试共 4 个用例，`fist-support-security` 测试共 17 个用例，失败 0。

继续处理 `fist-redisson` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 4 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- 可选 `Codec` 字段迁移到类型使用位置，例如 `private final @Nullable Codec codec`。
- `BeanPostProcessor` 覆写方法返回值迁移到类型使用位置，例如 `public @Nullable Object postProcessAfterInitialization(...)`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-redisson spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am test
```

结果：`fist-redisson` deprecation 明细编译为 `BUILD SUCCESS`，日志中未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。剩余 deprecation 明细为 Redisson `RBlockingQueue.subscribeOnElements(...)`。`fist-redisson -am test` 为 `BUILD SUCCESS`，模块自身没有测试源码，依赖链测试共 11 个用例，失败 0。

继续处理 `fist-boot-crud-mybatis` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 7 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- 自动配置和实体字段迁移到类型使用位置，例如 `private @Nullable ApplicationContext applicationContext`、`private @Nullable LocalDateTime delFlag`。
- Repository 内部返回值迁移到类型使用位置，例如 `@Nullable Object loadVersionValueById(...)` 与 `@Nullable Object getFieldValue(...)`。
- 保留「查询条件可为空」「排除 ID 可为空」「数据库字段值可为空」等原有公共接口契约。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am test
```

结果：`fist-boot-crud-mybatis` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。剩余 deprecation 明细为既有 `AuditFiller` 注解问题。`fist-boot-crud-mybatis -am test` 为 `BUILD SUCCESS`，模块自身测试共 14 个用例，失败 0。

继续处理 `fist-boot-apidoc` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 2 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- `DocParser` 保留 JSON 输入可为 `null` 的内部解析契约。
- `HandlerMethodResource` 的可选参数类型列表迁移到 `private @Nullable List<Class<?>> parameterTypes`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am test
```

结果：`fist-boot-apidoc` deprecation 明细编译为 `BUILD SUCCESS`；本模块未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。剩余 deprecation 明细为 Jackson 2 `JsonNode.fields()`、Apache Commons Lang `Validate.notNull(...)` 和 Springfox 退役相关注解问题。`fist-boot-apidoc -am test` 为 `BUILD SUCCESS`，模块自身没有测试源码，依赖链测试共 6 个用例，失败 0。

继续处理 `fist-boot-security-oauth2-server` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 5 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- 授权查询和 token 生成返回值迁移到类型使用位置，例如 `public @Nullable OAuth2Authorization findById(...)`、`public @Nullable OAuth2AccessToken generate(...)`。
- `OAuth2RefreshTokenAuthenticationConverter` 保留不匹配 refresh token grant 时返回 `null` 的接口契约。
- `OAuth2ResourceOwnerBaseAuthenticationToken` 与 `WebUtil` 保留可空入参契约。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,OAuth2ResourceOwnerBaseAuthenticationProviderTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`fist-boot-security-oauth2-server` deprecation 明细编译为 `BUILD SUCCESS`；本模块主代码未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。定向测试共 4 个用例，失败 0。剩余 deprecation 明细主要为 `DefaultAuthorizationService` 使用 Spring JDBC LOB API，以及 Apache Commons Lang 过时方法，不属于本轮空值注解迁移。

继续处理 `fist-gateway-auth-core` 的方法级 `org.springframework.lang.Nullable` deprecation 警告：

- 11 个 Java 文件从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- `RouteTargetResolver` 和 `DefaultRouteTargetResolver` 的可空返回值迁移到类型使用位置，例如 `@Nullable RouteTarget resolve(...)` 与 `public @Nullable RouteTarget resolve(...)`。
- `AuthContext`、`AuthProblem`、`AuthState` 和 `DefaultGatewayAuthFilterChain` 的可选字段迁移到类型使用位置。
- `AuthUtils`、`ProxyResolver`、`TenantFilter`、`UpstreamLocateFilter` 保留可空入参契约。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core -am test
```

结果：`fist-gateway-auth-core` deprecation 明细编译为 `BUILD SUCCESS`；本模块主代码未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。`fist-gateway-auth-core -am test` 为 `BUILD SUCCESS`，模块自身测试共 14 个用例，失败 0。全仓主代码复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`；当前唯一剩余命中是 `fist-cloud-gateway-acl` 的测试文件 `ApiGuardFilterTest.java`。

继续处理 `fist-cloud-gateway-acl` 测试文件中最后一个 `org.springframework.lang.Nullable` 命中：

- `ApiGuardFilterTest.java` 从 `org.springframework.lang.Nullable` 迁移到 JSpecify `org.jspecify.annotations.Nullable`。
- `makeAuthContext(...)` 测试辅助方法保留 principal 可为 `null` 的语义，用于构造未登录上下文。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am test
rg -n "org\.springframework\.lang\.Nullable|NonNullApi|NonNullFields" fist-kit-app fist-kit-cloud fist-kit-infra examples benchmarks -g "*.java" -S
```

结果：`fist-cloud-gateway-acl` deprecation 明细编译为 `BUILD SUCCESS`；`fist-cloud-gateway-acl -am test` 为 `BUILD SUCCESS`，模块自身测试共 6 个用例，失败 0。全仓 `*.java` 复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。

继续处理 Gateway、Security 与 OAuth2 编译日志中已经暴露的 Apache Commons Lang `3.20.0` 过时 API：

- `Validate.notNull(T)` 按本地源码建议迁移到 `Objects.requireNonNull(T)`。
- `StringUtils.startsWith(...)` 迁移到 `Strings.CS.startsWith(...)`。
- `StringUtils.equals(...)` 迁移到 `Strings.CS.equals(...)`。
- `ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`。
- `DefaultAuthorizationService` 移除继承 `org.apache.commons.lang3.StringUtils` 的内部工具别名，避免触发 Commons Lang 构造器 deprecation。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl,fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway-acl,fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway-acl -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=OAuth2ResourceOwnerBaseAuthenticationProviderTest,DefaultAuthorizationServiceTest,DefaultOauthFailureHandlerTest,WebUtilTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：Gateway 与 OAuth2 组合 deprecation 明细编译为 `BUILD SUCCESS`；本轮目标调用复扫未命中。Gateway 三个模块组合测试为 `BUILD SUCCESS`，Gateway Auth Core 模块自身测试共 14 个用例、Gateway ACL 模块自身测试共 6 个用例，失败 0。OAuth2 Server 定向测试共 4 个用例，失败 0。OAuth2 Server 剩余 deprecation 明细主要为 `DefaultAuthorizationService` 使用 Spring JDBC LOB API；其他 Commons Lang 过时调用来自 Web/Data 等依赖链模块，后续按模块继续处理。

继续处理 OAuth2 `DefaultAuthorizationService` 的 Spring JDBC LOB API deprecation：

- 使用 Context7 查询 Spring Framework 当前 Javadoc，确认 `LobCreator` / `LobHandler` 自 6.2 起弃用，替代项为 `SqlBinaryValue` / `SqlCharacterValue`。
- 使用本地 `spring-jdbc-7.0.7.jar` 的 `javap` 结果复核：`LobHandler`、`LobCreator`、`DefaultLobHandler` 均带 `@Deprecated(since="6.2")`。
- `DefaultAuthorizationService` 写入 BLOB 时改为 `SqlBinaryValue`，写入 CLOB 时改为 `SqlCharacterValue`。
- `OAuth2AuthorizationRowMapper` 读取 BLOB 时改为 `ResultSet#getBytes(...)`，读取 CLOB 时改为 `ResultSet#getString(...)`。
- 删除 `LobHandler`、`LobCreator`、`DefaultLobHandler` 和自定义 `LobCreatorArgumentPreparedStatementSetter`。
- 删除公开构造器 `DefaultAuthorizationService(JdbcOperations, RegisteredClientRepository, LobHandler)` 和 `OAuth2AuthorizationRowMapper#setLobHandler(...)`，作为 Boot 4 主版本迁移中的破坏性 API 清理记录。
- `DefaultAuthorizationServiceTest` 新增 BLOB/CLOB 参数映射测试，确认新实现返回 Spring JDBC `SqlBinaryValue` / `SqlCharacterValue`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
rg -n "LobHandler|LobCreator|DefaultLobHandler|SqlLobValue|LobCreatorArgumentPreparedStatementSetter|setLobHandler|getLobHandler" fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/main/java fist-kit-app/fist-security/fist-boot-security-oauth2-server/src/test -g "*.java" -g "*.groovy"
```

结果：OAuth2 Server LOB 迁移编译为 `BUILD SUCCESS`，模块自身未再出现 Spring JDBC LOB API deprecation 警告；依赖链仍有 `fist-support-data` 的 Commons Lang deprecation 警告。`DefaultAuthorizationServiceTest` 共 2 个用例，失败 0。目标 LOB API 复扫未命中。第一次目标测试未带 `-am` 触发本地快照依赖远程解析失败，第二次命令中 Surefire 参数缺少引号被 PowerShell 拆分为无效生命周期阶段；均已用上述命令修正。

继续处理 Web/Data 编译日志中已经暴露的 Apache Commons Lang `3.20.0` 过时 API：

- `fist-support-data`：`Validate.notNull(T)` 迁移到 `Objects.requireNonNull(T)`；`MutableObject.getValue()` 迁移到 `MutableObject.get()`；`ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`。
- `fist-boot-data`：`ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`；`StringUtils.equalsIgnoreCase(...)` 迁移到 `Strings.CI.equals(...)`；删除未使用的 `MutableObject<Throwable>`。
- `fist-support-web`：`ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`。
- `fist-boot-web-app`：`ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`；`String#equalsIgnoreCase(...)` 统一迁移到 `Strings.CI.equals(...)`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data,fist-kit-app/fist-data/fist-boot-data spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data,fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data,fist-kit-app/fist-data/fist-boot-data -am test
rg -n "defaultIfNull|Validate\.notNull|StringUtils\.equalsIgnoreCase|ret\.getValue\(" fist-kit-app/fist-web/fist-boot-web-app/src/main/java fist-kit-app/fist-web/fist-support-web/src/main/java fist-kit-app/fist-data/fist-boot-data/src/main/java fist-kit-app/fist-data/fist-support-data/src/main/java -g "*.java" -S
```

结果：Web/Data 组合 deprecation 明细编译为 `BUILD SUCCESS`；本轮目标调用复扫未命中。Web 两个模块当前没有自身测试源码；`fist-support-data` 模块自身 Java 测试共 6 个用例，失败 0；`fist-boot-data` 模块自身测试共 2 个用例，失败 0。剩余 deprecation 明细为既有 `Breaker`、`ValidateUtil`、`InTenant` 和 `PageData` unchecked 使用。

继续处理 API Doc 中替代关系明确的低风险 API 警告：

- `DocParser` 将 Jackson 2 `JsonNode.fields()` 迁移到 `JsonNode.properties()`。
- `HandlerMethodResource` 和 `RestResource` 将 Commons Lang `Validate.notNull(T)` 迁移到 JDK `Objects.requireNonNull(T)`。
- Springfox 插件和自动配置分支尚未删除；本轮不改变用户可见 API 文档能力。

验证依据：

- 本地 `jackson-databind-2.21.3.jar` 的 `javap` 结果确认 `JsonNode` 同时提供 `fields()` 和 `properties()`。
- 本地 `tools.jackson.core:jackson-databind:3.1.2` 的 `javap` 结果确认 Jackson 3 `JsonNode` 保留 `properties()`，不再提供 `fields()`。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc spring-javaformat:apply
rg -n "Validate\.notNull|\.fields\(" fist-kit-app/fist-boot-apidoc/src/main/java -g "*.java" -S
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am test
```

结果：API Doc 目标调用复扫未命中；API Doc deprecation 明细编译为 `BUILD SUCCESS`；API Doc 测试为 `BUILD SUCCESS`，模块自身没有测试源码，依赖链 `fist-kit-api` 共 6 个用例，失败 0。剩余 deprecation 明细只来自 Springfox 相关类缺少 `@Deprecated` 注解，需要在 Springfox 去留决策中处理。编译日志另有依赖链 `fist-kit-api` 的 `ElementType.MODULE` 提示，不属于本轮 API Doc deprecation 收敛目标。

继续按 Boot 4 主版本破坏性升级策略移除 Springfox 支持：

- 删除 `fist-boot-apidoc` 的 `io.springfox:springfox-oas` 可选依赖。
- 删除 `ApiTraitOperationPlugin`、Springfox 包级 `package-info.java` 和 `SwaggerDocAutoConfiguration`。
- 从 `spring.factories` 和 `AutoConfiguration.imports` 删除 Springfox 自动配置入口，只保留 `SpringDocAutoConfiguration`。
- SpringDoc 3 主路径保持不变。

验证命令：

```powershell
rg -n "springfox|SwaggerDocAutoConfiguration|ApiTraitOperationPlugin|swagger\.plugin" fist-kit-app/fist-boot-apidoc -S
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-boot-apidoc -am test
```

结果：API Doc Springfox 复扫未命中；API Doc deprecation 明细编译为 `BUILD SUCCESS`；API Doc 测试为 `BUILD SUCCESS`，模块自身没有测试源码，依赖链 `fist-kit-api` 共 6 个用例，失败 0。Springfox 相关 deprecation 警告已消失；编译日志仍有依赖链 `fist-kit-api` 的 `ElementType.MODULE` 提示，不属于本轮 API Doc 迁移目标。

继续处理 Groovy/Spock 测试迁移：

- 5 个 `.groovy` 测试迁移为 Java/JUnit 5。
- 根 `pom.xml` 删除 `gmavenplus-plugin.version`、`spock-lib.version` 和 `gmavenplus-plugin` 插件管理。
- `fist-kit-build/pom.xml` 删除 Groovy 测试依赖和 Spock BOM 管理。
- `fist-kit-infra/fist-support-test/pom.xml` 删除 `groovy-tables` 与 `spock-spring`。

验证命令：

```powershell
rg --files -g "*.groovy"
rg -n "groovy|spock|gmavenplus" pom.xml fist-kit-build/pom.xml fist-kit-infra/fist-support-test/pom.xml fist-kit-app fist-kit-cloud fist-kit-infra -g "pom.xml" -g "*.groovy" -S
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-security/fist-support-security,fist-kit-app/fist-data/fist-support-data spring-javaformat:validate
.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-security/fist-support-security,fist-kit-app/fist-data/fist-support-data -am "-Dtest=PathMatcherTest,DefaultUserCodecTest,NodeIdxTreeMakerTest,AdjacencyTreeMakerTest,OpTemplateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：仓库内 `*.groovy` 复扫未命中；Groovy/Spock/gmavenplus 构建配置复扫未命中；目标模块 `spring-javaformat:validate` 为 `BUILD SUCCESS`；5 个迁移后的 Java 测试共 11 个用例，失败 0。Web/Security/Data 三个目标模块全量测试为 `BUILD SUCCESS`；目标模块自身测试共 38 个用例，失败 0。

下一步：

1. 进入 Feign/SDE 专项验证。
2. 为 OAuth2 LOB 迁移补充数据库矩阵验证。

## Feign/SDE 专项验证

继续执行 Feign/SDE 专项，覆盖 Spring Cloud OpenFeign 5.x、SDE Feign 编解码、SDE boot starter、Feign / RestClient / WebClient 示例。

主要处理：

- `SdeRestClientAutoConfiguration` 迁移到 Boot 4 的 `org.springframework.boot.restclient.RestClientCustomizer`。
- `fist-sde-boot-starter` 新增普通依赖 `spring-boot-restclient`。
- `SdeWebClientAutoConfiguration` 不再使用 Boot 3 的 `WebClientCustomizer`，改为在缺少 `WebClient.Builder` 时提供带 SDE filter 的 builder。
- `AutoConfigureMockMvc` 迁移到 `org.springframework.boot.webmvc.test.autoconfigure`，测试依赖补充 `spring-boot-webmvc-test`。
- `DemoSecureFeignConfiguration` 使用 OpenFeign 5 的 `FeignHttpMessageConverters`。
- 示例 runner 使用 Boot 4 的 `org.springframework.boot.web.server.context.WebServerApplicationContext`。

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

- 依赖树解析到 `spring-cloud-openfeign-core:5.0.1`、`feign-core:13.6`、`feign-okhttp:13.6`。
- SDE client、Cloud RPC Feign、SDE boot starter、SDE Feign 示例、SDE RestClient 示例、SDE WebClient 示例均为 `BUILD SUCCESS`。
- 失败尝试已记录到 `spring-boot-4-upgrade-lessons.md`，包括 optional 依赖导致下游 `NoClassDefFoundError`、OpenFeign 5 构造参数变化、Boot 4 测试模块拆分和 WebServer 包迁移。

## Spring HTTP Service Clients 专项验证

继续评估 Spring HTTP Service Clients 后，结论是：当前不把它作为 Feign 的强制替代；SDE 只需要补齐 HTTP service 接口方法级 `@SecureExchange` 到底层 RestClient/WebClient 请求属性的传递。

主要处理：

- 新增 `SecureHttpServiceClientContextProcessor`，实现 Spring Framework 7 `HttpRequestValues.Processor`。
- 新增 `SecureHttpServiceSupport`，统一定义请求属性 key，并从请求属性中解析 `SecureExchangeClientContext`。
- `SecureRestClientInterceptor` 与 `SecureWebClientExchangeFilterFunction` 优先使用请求级 SDE 上下文。
- SDE boot starter 注册 `RestClientHttpServiceGroupConfigurer` 与 `WebClientHttpServiceGroupConfigurer`，为 HTTP Service group 添加方法级上下文处理器，并对拦截器/过滤器挂载做去重。

验证命令：

```powershell
npx ctx7@latest library "Spring Framework" "Spring Framework HTTP Service Clients @HttpExchange RestClientAdapter WebClientAdapter Spring Boot 4 migration"
npx ctx7@latest docs /websites/spring_io_spring-framework_reference "HTTP Interface Clients @HttpExchange RestClientAdapter WebClientAdapter reactive WebClientAdapter migration from OpenFeign"
npx ctx7@latest docs /spring-projects/spring-boot/v4.0.0 "Spring Boot 4 HTTP service clients RestClientBuilderConfigurer WebClient builder declarative HTTP clients"
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-client -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-client,fist-kit-infra/fist-sde/fist-sde-boot-starter -am spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-client -am test
.\mvnw.cmd -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am test
.\mvnw.cmd -Pexamples -pl examples/fist-sde/example-restclient,examples/fist-sde/example-webclient -am test
.\mvnw.cmd -Pexamples -pl fist-kit-infra/fist-sde/fist-sde-client,fist-kit-infra/fist-sde/fist-sde-boot-starter,examples/fist-sde/example-restclient,examples/fist-sde/example-webclient -am spring-javaformat:validate
```

结果：

- Spring Framework 文档确认 HTTP Service Clients 的代理工厂和底层 adapter 模型。
- Spring Boot 文档确认 HTTP Service group 可通过 RestClient/WebClient group configurer 扩展。
- `fist-sde-client -am compile` 与 `fist-sde-boot-starter -am compile` 均为 `BUILD SUCCESS`。
- 首次 `fist-sde-client -am compile` 被 `spring-javaformat:validate` 阻断，格式化后恢复。
- `fist-sde-client -am test` 为 `BUILD SUCCESS`，模块自身测试共 11 个用例，失败 0。
- `fist-sde-boot-starter -am test` 为 `BUILD SUCCESS`，模块自身测试共 29 个用例，失败 0。
- SDE RestClient / WebClient 示例测试为 `BUILD SUCCESS`，两个示例各 1 个用例，失败 0。
- HTTP Service Clients 相关模块与示例的 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

## OAuth2 LOB 本地数据库验证

继续补充 OAuth2 `DefaultAuthorizationService` 的本地数据库验证。该批次不引入 Testcontainers，只使用项目已有风格的 H2 测试依赖，并复用 Spring Authorization Server 7 内置 `oauth2-authorization-schema.sql`。

主要处理：

- `fist-boot-security-oauth2-server` 增加 `com.h2database:h2` 测试依赖。
- `DefaultAuthorizationServiceTest` 增加 H2 集成测试，覆盖官方 BLOB schema 下的 `save(...)`、`findById(...)` 和 access token 查询。
- 同一测试类增加 CLOB schema 变体，将官方 schema 中 `blob DEFAULT NULL` 替换为 `clob DEFAULT NULL`，覆盖 `SqlCharacterValue` 写入和 `ResultSet#getString(...)` 读取。

验证命令：

```powershell
npx ctx7@latest library "Spring Framework" "Spring Framework 7 JDBC SqlBinaryValue SqlCharacterValue LOB support migration from LobHandler"
npx ctx7@latest docs /websites/spring_io_spring-framework_current_javadoc-api "Spring Framework 7 JDBC SqlBinaryValue SqlCharacterValue LobHandler deprecated"
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=DefaultAuthorizationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am spring-javaformat:validate
```

结果：

- Context7 当前 Javadoc 确认 `LobHandler` / `LobCreator` 自 Spring Framework 6.2 起弃用，替代项为 `SqlBinaryValue` / `SqlCharacterValue`。
- 第一次测试命令缺少 `-Dsurefire.failIfNoSpecifiedTests=false`，上游 `fist-support-test` 因没有同名测试失败；修正后继续。
- 第二次测试进入目标模块后被 `spring-javaformat:validate` 拦截，执行 `spring-javaformat:apply` 后恢复。
- 修正后 `DefaultAuthorizationServiceTest` 共 4 个用例，失败 0；H2 官方 BLOB schema 和 CLOB schema 变体均通过。
- OAuth2 Server 模块全量测试为 `BUILD SUCCESS`，模块自身共 7 个用例，失败 0。
- OAuth2 Server 依赖链 `spring-javaformat:validate` 为 `BUILD SUCCESS`。
- MySQL、PostgreSQL、达梦未在本地执行。当前仓库没有对应数据库服务和 Testcontainers 依赖，需后续单独决定是否引入矩阵测试环境。

## 后续事项

1. 进入 Gateway / Spring Cloud Alibaba 组合验证。
2. 如决定引入真实数据库矩阵，为 OAuth2 LOB 补充 MySQL、PostgreSQL、达梦的驱动级验证。

## Gateway / Spring Cloud Alibaba 组合验证

继续验证 Gateway、Sentinel、Nacos 和 LoadBalancer 在 Boot 4 组合下的编译与运行边界。当前仓库中，Gateway 能力位于 `fist-cloud-gateway` 三个子模块；Spring Cloud Alibaba Sentinel 通过 `fist-cloud-rpc-feign` 引入。

主要处理：

- 使用 Context7 核对 Spring Cloud Gateway 5 WebFlux starter 坐标，确认 Boot 4 下使用 `spring-cloud-starter-gateway-server-webflux`。
- 使用 Context7 核对 Spring Cloud Alibaba `2025.1.x` 示例，确认 Gateway 示例同样使用 `spring-cloud-starter-gateway-server-webflux`。
- 解析 Gateway、LoadBalancer、Sentinel 和 Nacos 相关依赖树。
- 将 Feign 测试中的 `RequestTemplate.method("POST")` 迁移到 OpenFeign 13.6 推荐的 `RequestTemplate.method(Request.HttpMethod.POST)`。

验证命令：

```powershell
npx ctx7@latest library "Spring Cloud Gateway" "Spring Cloud Gateway 5 Spring Boot 4 webflux starter artifact spring-cloud-starter-gateway-server-webflux filter RouteLocator"
npx ctx7@latest docs /spring-cloud/spring-cloud-gateway/v5.0.0 "Spring Cloud Gateway 5 Spring Boot 4 webflux starter artifact spring-cloud-starter-gateway-server-webflux GlobalFilter GatewayFilter RouteLocator"
npx ctx7@latest library "Spring Cloud Alibaba" "Spring Cloud Alibaba 2025.1.0.0 Sentinel Nacos Spring Boot 4 Spring Cloud 2025 compatibility"
npx ctx7@latest docs /alibaba/spring-cloud-alibaba "Spring Cloud Alibaba 2025.1.0.0 Sentinel Nacos Spring Boot 4 Spring Cloud 2025 compatibility starter sentinel gateway adapter"
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am dependency:tree "-Dincludes=org.springframework.cloud:spring-cloud-gateway-server-webflux,org.springframework.cloud:spring-cloud-starter-gateway-server-webflux,org.springframework.cloud:spring-cloud-gateway-server,org.springframework.cloud:spring-cloud-starter-gateway,org.springframework.cloud:spring-cloud-loadbalancer,org.springframework.cloud:spring-cloud-starter-loadbalancer,com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel,com.alibaba.cloud:spring-cloud-alibaba-sentinel-gateway,com.alibaba.csp:sentinel-spring-cloud-gateway-v6x-adapter,com.alibaba.csp:sentinel-spring-webmvc-v6x-adapter,com.alibaba.nacos:nacos-client"
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign spring-javaformat:apply
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am "-Dtest=SecureFeignEncoderPrototypeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-gateway-auth-core,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-security-reactive,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl,fist-kit-cloud/fist-cloud-rpc-feign -am spring-javaformat:validate
```

结果：

- Gateway 依赖树解析到 `spring-cloud-gateway-server-webflux:5.0.1` 和 `spring-cloud-starter-gateway-server-webflux:5.0.1`，未解析到旧 `spring-cloud-gateway-server` 或 `spring-cloud-starter-gateway`。
- LoadBalancer 解析到 `spring-cloud-loadbalancer:5.0.1` 和 `spring-cloud-starter-loadbalancer:5.0.1`。
- `fist-cloud-rpc-feign` 解析到 `spring-cloud-starter-alibaba-sentinel:2025.1.0.0`，传递 `sentinel-spring-webmvc-v6x-adapter:1.8.9`；当前仓库没有引入 `spring-cloud-alibaba-sentinel-gateway`，也没有解析到 `sentinel-spring-cloud-gateway-v6x-adapter`。
- 组合 clean compile 为 `BUILD SUCCESS`。
- 初次组合 test 为 `BUILD SUCCESS`，但 Feign 测试编译提示 `RequestTemplate.method(String)` 过时；解包 `feign-core-13.6-sources.jar` 后确认替代项为 `RequestTemplate.method(Request.HttpMethod)`。
- 第一次复扫 `RequestTemplate.method("...")` 时使用双引号正则，PowerShell 将 `|` 当作管道解析导致命令失败；改用单引号后复扫成功。
- 替换 Feign 测试调用后，首次定向测试被 `spring-javaformat:validate` 拦截；执行 `spring-javaformat:apply` 后恢复。
- 修正后 Feign encoder 定向测试为 `BUILD SUCCESS`，1 个用例失败 0。
- 最终组合 test 为 `BUILD SUCCESS`：`fist-gateway-auth-core` 14 个用例、`fist-cloud-gateway-acl` 6 个用例、`fist-cloud-rpc-feign` 7 个用例失败 0；`fist-cloud-gateway-security-reactive` 当前无自身测试源码。
- 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

边界：

- 本轮验证确认当前仓库已有 Gateway / LoadBalancer / Feign / Sentinel WebMVC adapter 组合可编译、可测试。
- 后续复查确认 Sentinel 未实际使用，已移除 Sentinel starter 和 Spring Cloud Alibaba BOM；该结论取代本节中的 Sentinel 待验证项。
- 当前仓库未显式使用 Nacos starter，Nacos 只停留在 Spring Cloud Alibaba BOM 准入层，未形成运行级验证。

## 后续事项

1. 评估是否需要补一个最小 Gateway 启动类或示例，验证完整 Boot 4 Gateway 应用上下文。
2. 复查 Sentinel / Nacos 是否有实际使用点；如无实际使用，改为移除未使用依赖。
3. 如决定引入真实数据库矩阵，为 OAuth2 LOB 补充 MySQL、PostgreSQL、达梦的驱动级验证。

## Gateway ACL 最小 Boot 4 上下文启动验证

继续补充 Gateway 运行级证据。本轮不新增生产启动类，先在 `fist-cloud-gateway-acl` 中增加最小 `@SpringBootTest`，验证 Boot 4 Reactive Web Server、Spring Cloud Gateway 5 WebFlux 自动配置和 `RouteGuardConfiguration` 可以在同一个应用上下文启动。

主要处理：

- 新增 `RouteGuardConfigurationStartupTest`，使用 `webEnvironment = RANDOM_PORT` 和 `spring.main.web-application-type=reactive` 启动最小 Boot 4 Reactive Web 上下文。
- 测试应用显式导入 `RouteGuardConfiguration`，并提供应用侧最小依赖：`ReactivePermissionDefinitionService<PermissionDefinition>` 与 `UserIntrospectClient`。
- 断言随机端口已分配，并确认 `ApiGuardFilter`、`RequestIdGlobalFilter` 和 Spring Cloud Gateway 5 的 `RoutePredicateHandlerMapping` 均已进入上下文。

验证命令：

```powershell
npx ctx7@latest library "Spring Boot" "Spring Boot 4 WebFlux SpringBootTest random port application context"
npx ctx7@latest docs /spring-projects/spring-boot/v4.0.3 "Spring Boot 4 WebFlux SpringBootTest random port application context"
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am "-Dtest=RouteGuardConfigurationStartupTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am spring-javaformat:validate
```

结果：

- Context7 确认 Boot 4 的完整运行服务器测试仍使用 `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`，并通过 `@LocalServerPort` 获取实际端口。
- 第一次新增测试使用 `assertThat(applicationContext).hasSingleBean(...)`，当前 AssertJ 断言类型不支持该方法，编译失败；改为 `applicationContext.getBeansOfType(...).hasSize(1)` 后继续。
- 第二次运行测试失败于真实上下文依赖缺口：`GatewayAuthFilterConfiguration` 构造参数缺少 `ReactivePermissionDefinitionService`。测试应用补充该 bean 后恢复。
- `UserIntrospectClient` 也作为应用侧 bean 显式提供，避免最小上下文依赖 OAuth2 opaque token 自动配置参数。
- 定向启动测试为 `BUILD SUCCESS`，`RouteGuardConfigurationStartupTest` 1 个用例，失败 0。
- `fist-cloud-gateway-acl -am test` 为 `BUILD SUCCESS`，目标模块自身 7 个用例，失败 0；依赖模块测试同步通过。
- `fist-cloud-gateway-acl -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

边界：

- 本轮验证覆盖 Gateway WebFlux 自动配置与项目 ACL 配置的最小应用上下文启动，不覆盖真实路由转发、Sentinel Gateway adapter 或 Nacos Discovery。
- `RouteGuardConfiguration` 不是完全自包含自动配置，下游应用仍需要提供权限定义服务；如果启用 OAuth2 introspection，也需要提供 introspection URI 或自定义 `UserIntrospectClient`。

## 后续事项

1. 复查 Sentinel / Nacos 是否有实际使用点；如无实际使用，改为移除未使用依赖。
2. 如决定引入真实数据库矩阵，为 OAuth2 LOB 补充 MySQL、PostgreSQL、达梦的驱动级验证。
3. 继续按模块处理剩余 Boot 4 编译、弃用 API 与应用级启动验证。

## Sentinel / Spring Cloud Alibaba 未使用依赖清理

根据用户补充信息，复查仓库中 Sentinel、Nacos 和 Spring Cloud Alibaba 的实际使用点。除正式调研文档、`fist-cloud-rpc-feign/pom.xml` 中的 Sentinel starter 声明，以及未跟踪的 `.flattened-pom.xml` 外，未发现 Sentinel / Nacos 源码、注解、配置或 API 使用点。

处理结果：

- 移除 `fist-cloud-rpc-feign` 的 `com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel`。
- 移除根 `spring-cloud-alibaba.version` 属性。
- 移除 `fist-kit-build` 中的 `com.alibaba.cloud:spring-cloud-alibaba-dependencies` BOM。
- 将 Sentinel Gateway / Nacos Discovery 从后续高优先级运行级验证项中移除；如内部应用仍使用，应在应用侧单独引入并验证。

验证命令：

```powershell
rg -n "sentinel|Sentinel|spring-cloud-starter-alibaba-sentinel|spring-cloud-alibaba-sentinel-gateway|nacos|Nacos" -g "*.java" -g "*.xml" -g "*.yml" -g "*.yaml" -g "*.properties" -g "*.md"
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am dependency:tree "-Dincludes=com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel,com.alibaba.csp:sentinel-spring-webmvc-v6x-adapter,com.alibaba.csp:sentinel-spring-cloud-gateway-v6x-adapter"
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign -am test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am dependency:tree "-Dincludes=com.alibaba.cloud:spring-cloud-alibaba-dependencies,com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel,com.alibaba.csp:sentinel-spring-webmvc-v6x-adapter,com.alibaba.csp:sentinel-spring-cloud-gateway-v6x-adapter,com.alibaba.nacos:nacos-client"
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am test
.\mvnw.cmd -pl fist-kit-cloud/fist-cloud-rpc-feign,fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am spring-javaformat:validate
```

结果：

- Sentinel / Nacos 复扫未发现 Git 跟踪源码、配置或主 POM 使用点。
- 移除 Sentinel starter 后，Feign 模块依赖树未解析到 Sentinel 相关依赖。
- 移除 Spring Cloud Alibaba BOM 后，Cloud 组合依赖树未解析到 Alibaba / Sentinel / Nacos 相关依赖，命令为 `BUILD SUCCESS`。
- `fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`，目标模块自身 7 个用例，失败 0。
- `fist-cloud-rpc-feign` + `fist-cloud-gateway-acl` 组合测试为 `BUILD SUCCESS`：Gateway Auth Core 14 个用例、Gateway ACL 7 个用例、Feign 7 个用例失败 0。
- 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

边界：

- 未跟踪的 `fist-cloud-rpc-feign/.flattened-pom.xml` 仍保留旧 Sentinel 内容，本轮不提交该文件；发布时由 flatten 插件重新生成。
- 如果下游应用实际使用 Sentinel 或 Nacos，需要由下游显式引入 Spring Cloud Alibaba 相关依赖，并在应用侧完成 Boot 4 运行级验证。

## 后续事项

1. 如决定引入真实数据库矩阵，为 OAuth2 LOB 补充 MySQL、PostgreSQL、达梦的驱动级验证。
2. 继续按模块处理剩余 Boot 4 编译、弃用 API 与应用级启动验证。

## Redisson 订阅 API 废弃迁移

继续处理 `fist-redisson` 的 `RBlockingQueue.subscribeOnElements(...)` deprecation 警告。

范围：

- `fist-kit-infra/fist-redisson`

调研结论：

- Context7 Redisson 文档未直接给出 `subscribeOnElements` 的替代项，只返回 `RBlockingQueue` 普通队列操作和 `RReliableQueue` listener 示例。
- 本地 Redisson `4.3.1` JAR 显示 `RBlockingQueue.subscribeOnElements(Consumer<V>)` 已废弃。
- 同一接口保留 `RBlockingQueue.subscribeOnElements(Function<V, CompletionStage<Void>>)`，该重载未废弃。

处理结果：

- `QueueConsumerBeanProcessor` 改用 `Function<V, CompletionStage<Void>>` 重载。
- 回调内部继续执行原有类型校验、日志和 `@QueueConsumer` 方法调用。
- 回调正常结束时返回 `CompletableFuture.completedFuture(null)`。
- 新增 `QueueConsumerBeanProcessorTest`，通过 mock `RBlockingQueue` 捕获 `Function` 回调，验证订阅 API 形态和消费方法调用行为。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-infra/fist-redisson -am "-Dtest=QueueConsumerBeanProcessorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

当前结果：

- RED：新增测试在旧实现下失败，原因是旧实现实际调用 `Consumer` 重载。
- GREEN：迁移后定向测试为 `BUILD SUCCESS`，`QueueConsumerBeanProcessorTest` 1 个用例，失败 0。
- `fist-redisson -am test` 为 `BUILD SUCCESS`，目标模块自身 1 个用例，失败 0。
- `fist-redisson -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，deprecation 明细日志未再出现 `subscribeOnElements` 或过时提示。
- `fist-redisson -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

## OAuth2 Resource Owner 自有废弃 API 清理

继续处理 `fist-boot-security-oauth2-server` 中自有 `@Deprecated` 入口：

- `OAuth2ResourceOwnerBaseAuthenticationProvider.refreshTokenGenerator`
- `OAuth2ResourceOwnerBaseAuthenticationProvider#setRefreshTokenGenerator(...)`

调研结论：

- Context7 Spring Authorization Server 文档确认，自定义 token 生成应配置 `OAuth2TokenGenerator` 或 `DelegatingOAuth2TokenGenerator`。
- 当前 `refreshTokenGenerator` 仅在 provider 内部使用，仓库内没有外部调用点。
- 编译 deprecation 日志不会列出未被调用的自有废弃 API，需要源码复扫或反射测试兜住。

处理结果：

- 删除 `refreshTokenGenerator` 字段和 `setRefreshTokenGenerator(...)`。
- 刷新令牌生成统一使用既有 `this.tokenGenerator.generate(...)` 分支。
- `OAuth2ResourceOwnerBaseAuthenticationProviderTest` 增加反射测试，要求 provider 不再暴露带 `@Deprecated` 的字段或方法。

验证命令：

```powershell
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=OAuth2ResourceOwnerBaseAuthenticationProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am test
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
.\mvnw.cmd -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am spring-javaformat:validate
```

结果：

- RED：新增反射测试在旧实现下失败，原因是 provider 仍存在废弃字段或方法。
- GREEN：迁移后定向测试为 `BUILD SUCCESS`，`OAuth2ResourceOwnerBaseAuthenticationProviderTest` 2 个用例，失败 0。
- OAuth2 Server 模块测试为 `BUILD SUCCESS`，目标模块自身 8 个用例，失败 0。
- deprecation 明细编译为 `BUILD SUCCESS`，本轮目标关键字 `setRefreshTokenGenerator` / `refreshTokenGenerator` 未再出现；日志中仍有依赖链既有 `Breaker` 和 `ValidateUtil` 提示。
- 格式校验为 `BUILD SUCCESS`。

## 自有废弃 API 注解一致性

继续处理 deprecation 明细编译中暴露的自有 API 声明一致性问题：

- `Breaker` 已在 Javadoc 标注 `@deprecated`，但类声明缺少 `@Deprecated`。
- `ValidateUtil#check(T, Class<?>...)` 已在 Javadoc 标注 `@deprecated`，但方法声明缺少 `@Deprecated`。

处理结果：

- `Breaker` 类补充 `@Deprecated`。
- `ValidateUtil#check(T, Class<?>...)` 旧重载补充 `@Deprecated`。
- 新增 `BreakerDeprecationTest`，要求类声明包含 Java 级废弃注解。
- 新增 `ValidateUtilDeprecationTest`，通过反射要求旧重载包含 Java 级废弃注解。

验证命令：

```powershell
cmd /c mvn -pl fist-kit-app/fist-web/fist-support-web spring-javaformat:apply
cmd /c mvn -pl fist-kit-app/fist-data/fist-support-data spring-javaformat:apply
cmd /c mvn -pl fist-kit-app/fist-web/fist-support-web -am "-Dtest=BreakerDeprecationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-data/fist-support-data -am "-Dtest=ValidateUtilDeprecationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-data/fist-support-data -am test
```

结果：

- RED：`BreakerDeprecationTest` 在旧实现下失败，原因是 `Breaker.class.isAnnotationPresent(Deprecated.class)` 为 `false`。
- RED：`ValidateUtilDeprecationTest` 在旧实现下失败，原因是旧 `check(Object, Class[])` 重载缺少 `@Deprecated`。
- `fist-support-web` 和 `fist-support-data` 的 `spring-javaformat:apply` 均为 `BUILD SUCCESS`。
- GREEN：`BreakerDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- GREEN：`ValidateUtilDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- `BreakerDeprecationTest` 直接引用废弃类，测试编译阶段出现「使用或覆盖了已过时的 API」提示；该提示来自测试意图，不代表生产代码仍有同类调用。
- 组合模块 `test` 未完成：Maven Central BOM POM 下载失败并被本地仓库缓存，导致 Maven 在模型构建阶段失败；提升权限重跑时审批服务返回 503，本轮未继续重试。

## Data 自有废弃 API 注解一致性补充

继续处理 Data 模块中 Javadoc `@deprecated` 与 Java `@Deprecated` 不一致的声明：

- `InTenant`
- `AuditFiller`

处理结果：

- `InTenant` 注解补充 `@Deprecated`。
- `AuditFiller` 类补充 `@Deprecated`。
- 新增 `InTenantDeprecationTest` 和 `AuditFillerDeprecationTest`，要求声明包含 Java 级废弃注解。

验证命令：

```powershell
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-data -am "-Dtest=InTenantDeprecationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-crud-mybatis -am "-Dtest=AuditFillerDeprecationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -U -pl fist-kit-app/fist-data/fist-boot-data,fist-kit-app/fist-data/fist-boot-crud-mybatis -am test
cmd /c mvn -U -pl fist-kit-app/fist-data/fist-boot-data,fist-kit-app/fist-data/fist-boot-crud-mybatis -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
cmd /c mvn -U -pl fist-kit-app/fist-data/fist-boot-data,fist-kit-app/fist-data/fist-boot-crud-mybatis -am spring-javaformat:validate
```

结果：

- RED：两个新增测试在旧实现下均失败，原因是声明缺少 Java 级 `@Deprecated`。
- GREEN：`InTenantDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- GREEN：`AuditFillerDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- Data 组合模块测试为 `BUILD SUCCESS`，`fist-boot-data` 3 个用例、`fist-boot-crud-mybatis` 15 个用例失败 0。
- deprecation 明细编译为 `BUILD SUCCESS`，`InTenant` / `AuditFiller` 的「未使用 @Deprecated 对已过时的项目进行注释」警告已消失；仍存在 `InTenantAspect` 引用已废弃 `InTenant` 的调用点提示，需要另行决策。
- 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

错误尝试记录：

- 两个目标测试第一次并行运行时，因 `fist-boot-crud-mybatis -am` 与 `fist-boot-data -am` 反应堆重叠，`fist-boot-data/target` 发生并发写入，`InTenantDeprecationTest` 的 GREEN 阶段出现 Surefire `ClassNotFoundException`。
- 不带 `-am` 只跑 `fist-boot-data` 会解析远端 `4.0.0-SNAPSHOT` 上游模块，失败于本仓库 SNAPSHOT 依赖不可解析。
- 普通权限组合验证被 Maven Central BOM POM 下载失败和本地缓存阻断；提升权限并加 `-U` 后通过。

## 本地日志

本阶段日志保存在 `.codex/`，不提交：

- `.codex/trial-compile-cloud-rpc-feign.log`
- `.codex/trial-compile-cloud-rpc-feign-2.log`
- `.codex/trial-compile-infra-redisson.log`
- `.codex/trial-compile-infra-logback.log`
- `.codex/trial-compile-infra-mybatis-plus.log`
- `.codex/trial-compile-infra-sde-boot-starter.log`
- `.codex/trial-compile-app-support-data.log`
- `.codex/trial-compile-app-support-security.log`
- `.codex/trial-compile-app-apidoc.log`
- `.codex/trial-compile-app-crud-mybatis.log`
- `.codex/trial-compile-app-oauth2-server.log`
- `.codex/oauth2-lob-compile.log`
- `.codex/oauth2-lob-test.log`
- `.codex/oauth2-lob-h2-test.log`
- `.codex/oauth2-lob-format-apply.log`
- `.codex/oauth2-lob-module-test.log`
- `.codex/oauth2-lob-format-validate.log`
- `.codex/web-data-commons-deprecation-compile.log`
- `.codex/web-data-commons-deprecation-test.log`
- `.codex/apidoc-special-compile-before.log`
- `.codex/apidoc-special-compile.log`
- `.codex/apidoc-special-test.log`
- `.codex/apidoc-remove-springfox-compile.log`
- `.codex/apidoc-remove-springfox-test.log`
- `.codex/mybatis-plus-boot4-dependency-tree.log`
- `.codex/mybatis-plus-boot4-target-test.log`
- `.codex/groovy-removal-target-tests.log`
- `.codex/groovy-removal-module-tests.log`
- `.codex/feign-sde-dependency-tree.log`
- `.codex/feign-sde-client-compile.log`
- `.codex/feign-sde-client-test.log`
- `.codex/feign-sde-cloud-rpc-feign-compile.log`
- `.codex/feign-sde-cloud-rpc-feign-test.log`
- `.codex/sde-boot-starter-clean-test-compile-after-feign-sde.log`
- `.codex/sde-boot-starter-test-after-feign-sde.log`
- `.codex/feign-sde-example-feign-test-after-openfeign5.log`
- `.codex/feign-sde-example-rest-webclient-test-after-restclient-dep.log`
- `.codex/feign-sde-format-validate.log`
- `.codex/http-service-client-compile.log`
- `.codex/http-service-boot-starter-compile.log`
- `.codex/http-service-sde-client-test.log`
- `.codex/http-service-sde-boot-starter-test.log`
- `.codex/http-service-processor-test.log`
- `.codex/http-service-sde-examples-rest-webclient-test.log`
- `.codex/http-service-format-validate.log`
- `.codex/gateway-alibaba-dependency-tree.log`
- `.codex/gateway-alibaba-compile.log`
- `.codex/gateway-alibaba-test.log`
- `.codex/gateway-alibaba-feign-format-apply.log`
- `.codex/gateway-alibaba-feign-encoder-test.log`
- `.codex/gateway-alibaba-test-after-feign-fix.log`
- `.codex/gateway-alibaba-format-validate.log`
- `.codex/gateway-context-startup-red.log`
- `.codex/gateway-context-startup-red-2.log`
- `.codex/gateway-context-startup-green.log`
- `.codex/gateway-context-startup-module-test.log`
- `.codex/gateway-context-startup-format-validate.log`
- `.codex/sentinel-removal-dependency-tree.log`
- `.codex/sentinel-removal-feign-test.log`
- `.codex/sentinel-removal-cloud-dependency-tree.log`
- `.codex/sentinel-removal-cloud-test.log`
- `.codex/sentinel-removal-format-validate.log`
- `.codex/redisson-subscribe-api-green.log`
- `.codex/redisson-subscribe-api-module-test.log`
- `.codex/redisson-subscribe-api-deprecation-compile.log`
- `.codex/redisson-subscribe-api-format-validate.log`
- `.codex/oauth2-resource-owner-deprecated-red.log`
- `.codex/oauth2-resource-owner-deprecated-green.log`
- `.codex/oauth2-resource-owner-deprecated-module-test.log`
- `.codex/oauth2-resource-owner-deprecated-compile.log`
- `.codex/oauth2-resource-owner-deprecated-format-validate.log`

## 资料来源

- Spring Boot 4.0 Migration Guide：`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`
- Spring Boot 4.0.6 发布记录：`https://spring.io/blog/2026/04/23/spring-boot-4-0-6-available-now`
- Spring Boot Context7 文档：`/spring-projects/spring-boot`，`DataSourceAutoConfiguration` 位于 `org.springframework.boot.jdbc.autoconfigure`
- Spring Framework 当前 Javadoc Context7 文档：`/websites/spring_io_spring-framework_current_javadoc-api`，`LobCreator` / `LobHandler` 自 6.2 起弃用，替代项为 `SqlBinaryValue` / `SqlCharacterValue`
- Apache Commons Lang Context7 文档：`/websites/commons_apache_proper_commons-lang_apidocs`，`ObjectUtils.defaultIfNull(T, T)` 弃用并推荐 `ObjectUtils.getIfNull(Object, Object)`
- Spring JDBC `7.0.7` 本地 JAR：`F:\pkg-maven\repository\org\springframework\spring-jdbc\7.0.7\spring-jdbc-7.0.7.jar`
- Spring Authorization Server `7.0.5` 本地 JAR：`F:\pkg-maven\repository\org\springframework\security\spring-security-oauth2-authorization-server\7.0.5\spring-security-oauth2-authorization-server-7.0.5.jar`，确认内置 `oauth2-authorization-schema.sql`
- Spring Authorization Server Context7 文档：`/spring-projects/spring-authorization-server`，确认自定义 token 生成应通过 `OAuth2TokenGenerator` / `DelegatingOAuth2TokenGenerator` 配置。
- Jackson Databind `2.21.3` 本地 JAR：`F:\pkg-maven\repository\com\fasterxml\jackson\core\jackson-databind\2.21.3\jackson-databind-2.21.3.jar`
- Jackson Databind `3.1.2` 本地 JAR：`F:\pkg-maven\repository\tools\jackson\core\jackson-databind\3.1.2\jackson-databind-3.1.2.jar`
- MyBatis Plus `3.5.16` 本地 JAR：`F:\pkg-maven\repository\com\baomidou\mybatis-plus-spring-boot4-starter\3.5.16\mybatis-plus-spring-boot4-starter-3.5.16.jar`
- MyBatis Plus Context7 文档：`/baomidou/mybatis-plus-doc`，Spring Boot 4 及以上版本使用 `mybatis-plus-spring-boot4-starter`
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-webflux\4.0.6\spring-boot-webflux-4.0.6.jar`
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-webmvc-test\4.0.6\spring-boot-webmvc-test-4.0.6.jar`，确认 `AutoConfigureMockMvc` 新位置。
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-restclient\4.0.6\spring-boot-restclient-4.0.6.jar`，确认 `RestClientCustomizer` 新位置。
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-web-server\4.0.6\spring-boot-web-server-4.0.6.jar`，确认 `WebServerApplicationContext` 新位置。
- Spring Cloud OpenFeign Context7 文档：`/spring-cloud/spring-cloud-openfeign`，确认自定义 `Encoder` / `Decoder` 仍可通过 Feign 配置注入。
- Spring Framework Context7 文档：`/websites/spring_io_spring-framework_reference`，确认 HTTP Service Clients 使用 `HttpServiceProxyFactory` 与 RestClient/WebClient adapter。
- Spring Boot Context7 文档：`/spring-projects/spring-boot/v4.0.0`，确认 HTTP Service group 可通过 RestClient/WebClient group configurer 自定义。
- Spring Framework `7.0.7` 本地 JAR：`F:\pkg-maven\repository\org\springframework\spring-web\7.0.7\spring-web-7.0.7.jar`，确认 `HttpRequestValues.Processor`、`RestClientAdapter` 和 HTTP service registry 类型。
- Spring Framework `7.0.7` 本地 JAR：`F:\pkg-maven\repository\org\springframework\spring-webflux\7.0.7\spring-webflux-7.0.7.jar`，确认 `WebClientAdapter` 和 `WebClientHttpServiceGroupConfigurer`。
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-restclient\4.0.6\spring-boot-restclient-4.0.6.jar`，确认 `RestClientCustomizerHttpServiceGroupConfigurer` 会处理 RestClient-backed HTTP Service group。
- Spring Cloud Gateway Context7 文档：`/spring-cloud/spring-cloud-gateway/v5.0.0`，确认 Gateway Server WebFlux starter 坐标为 `spring-cloud-starter-gateway-server-webflux`。
- Spring Boot Context7 文档：`/spring-projects/spring-boot/v4.0.3`，确认运行服务器测试可使用 `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` 与 `@LocalServerPort`。
- Spring Cloud Alibaba Context7 文档：`/alibaba/spring-cloud-alibaba`，确认 `2025.1.x` 示例使用 `spring-cloud-starter-gateway-server-webflux`，Sentinel Gateway 需引入 `spring-cloud-alibaba-sentinel-gateway`。
- OpenFeign `13.6` 本地源码 JAR：`F:\pkg-maven\repository\io\github\openfeign\feign-core\13.6\feign-core-13.6-sources.jar`，确认 `RequestTemplate.method(String)` 弃用并推荐 `RequestTemplate.method(Request.HttpMethod)`。
- Redisson Context7 文档：`/redisson/redisson`，未直接覆盖 `RBlockingQueue.subscribeOnElements` 替代项。
- Redisson `4.3.1` 本地 JAR：`F:\pkg-maven\repository\org\redisson\redisson\4.3.1\redisson-4.3.1.jar`，`javap -v` 确认 `subscribeOnElements(Consumer<V>)` 废弃，`subscribeOnElements(Function<V, CompletionStage<Void>>)` 未废弃。
- 本地 OpenRewrite dry-run patch：`target/rewrite/rewrite.patch`
