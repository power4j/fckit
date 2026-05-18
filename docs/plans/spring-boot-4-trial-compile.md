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

## Gateway OAuth2 opaque-token 配置装配验证

继续补充 Gateway 运行级证据。此前 `RouteGuardConfigurationStartupTest` 为避免远程 OAuth2 introspection 依赖，手工提供了 `UserIntrospectClient`，因此没有覆盖 `Oauth2Configuration` 基于配置项创建 `UserIntrospectClient` 的路径。

主要处理：

- 新增 `Oauth2ConfigurationTest`，使用 `ApplicationContextRunner` 装配 `Oauth2Configuration`。
- 验证未设置 `fist.oauth2.resourceserver.opaque-token.introspection-uri` 时不创建 `UserIntrospectClient`。
- 验证设置 `fist.oauth2.resourceserver.opaque-token.introspection-uri`、`client-id` 和 `client-secret` 后可创建 `UserIntrospectClient`。
- 同时确认 `lbWebClientBuilder` 仍在上下文中，保留 `lb://` introspection URI 的 LoadBalancer 客户端入口。

验证命令：

```powershell
cmd /c mvn -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am "-Dtest=Oauth2ConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am spring-javaformat:validate
```

结果：

- `Oauth2ConfigurationTest` 定向测试为 `BUILD SUCCESS`，测试 2 个，失败 0，错误 0，跳过 0。
- `fist-cloud-gateway-acl -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

边界：

- 本轮只验证配置项绑定、条件装配和 `UserIntrospectClient` 创建，不发起真实 introspection HTTP 请求。
- 真实认证服务器响应结构、错误处理和 `UserInfo` 解析仍由后续应用级或外部环境联调覆盖。

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

## Data `InTenant` 废弃注解入口移除

日期：2026-05-13
执行者：Codex

目标：

- 移除仓库内无实际业务使用的 `InTenant` / `InTenantAspect` 旧租户注解入口。
- 保留并验证 `TenantInvokeAspect` 自动配置。
- 消除 `fist-boot-data` 生产代码对自有废弃 API 的残留调用。

变更：

- `DataAutoConfiguration` 删除 `inTenantAspect` bean。
- 删除 `InTenantAspect` 和 `InTenant`。
- 删除已经失去意义的 `InTenantDeprecationTest`。
- `DataAutoConfigurationTest` 改为断言 `TenantInvokeAspect` 存在，且 `inTenantAspect` 不存在。

执行命令：

```powershell
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-data -am "-Dtest=DataAutoConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-data spring-javaformat:apply
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-data -am "-Dtest=DataAutoConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
rg -n "InTenant|InTenantAspect" fist-kit-app/fist-data/fist-boot-data
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-data -am test
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
cmd /c mvn -pl fist-kit-app/fist-data/fist-boot-data -am spring-javaformat:validate
```

结果：

- RED：第一次格式修复后，`DataAutoConfigurationTest` 在旧实现下失败，原因是上下文仍注册 `inTenantAspect`。
- 实施后第一次 GREEN 被 `spring-javaformat:validate` 拦截，`DataAutoConfiguration.java` 存在格式差异；运行 `spring-javaformat:apply` 后恢复。
- GREEN：`DataAutoConfigurationTest` 为 `BUILD SUCCESS`，1 个用例，失败 0。
- 复扫：`fist-boot-data` 内 `InTenant` / `InTenantAspect` 无残留引用。
- 模块测试：`fist-boot-data -am test` 为 `BUILD SUCCESS`，`fist-support-data` 12 个用例、`fist-boot-data` 2 个用例失败 0。
- deprecation 明细编译：`fist-boot-data -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，不再出现 `InTenant` / `InTenantAspect` 相关提示。剩余可见项为既有 `PageData` unchecked 提示和 `fist-kit-api` 的 `ElementType.MODULE` 注解环境提示。
- 格式校验：`fist-boot-data -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

影响：

- 这是主版本内的破坏性 API 删除。下游仍使用 `@InTenant` 的代码需要迁移到当前租户调用模型。
- 本仓库未发现除旧切面自注册、声明级废弃测试和文档记录外的业务使用。

## Web `Breaker` 废弃工具类移除

日期：2026-05-13
执行者：Codex

目标：

- 删除自有废弃异常工具类 `Breaker`。
- 删除只为 `Breaker` 注入国际化依赖的 `MessageConfiguration`。
- 消除 `fist-boot-web-app` 生产代码对自有废弃 API 的最后调用点。

变更：

- 删除 `Breaker` 和 `BreakerDeprecationTest`。
- 删除 `MessageConfiguration`。
- `CommonConfiguration` 不再导入 `MessageConfiguration`。
- 新增 `BreakerRemovalTest`，断言旧 `Breaker` 类不可加载。
- 新增 `CommonConfigurationTest`，断言 `CommonConfiguration` 不再导入旧消息配置。

执行命令：

```powershell
cmd /c mvn -pl fist-kit-app/fist-web/fist-support-web -am "-Dtest=BreakerRemovalTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-web/fist-boot-web-app -am "-Dtest=CommonConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-web/fist-boot-web-app spring-javaformat:apply
cmd /c mvn -pl fist-kit-app/fist-web/fist-boot-web-app -am test
cmd /c mvn -pl fist-kit-app/fist-web/fist-boot-web-app -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
cmd /c mvn -pl fist-kit-app/fist-web/fist-boot-web-app -am spring-javaformat:validate
```

结果：

- RED：`BreakerRemovalTest` 在旧实现下失败，原因是 `Breaker` 仍可加载。
- RED：`CommonConfigurationTest` 在旧实现下失败，原因是 `CommonConfiguration` 仍导入 `MessageConfiguration`。
- GREEN：两个目标测试均为 `BUILD SUCCESS`，各 1 个用例，失败 0。
- 错误尝试：删除 `MessageConfiguration` 导入后，`CommonConfigurationTest` 首次 GREEN 被 `spring-javaformat:validate` 拦截；运行 `spring-javaformat:apply` 后通过。
- 组合测试：`fist-boot-web-app -am test` 为 `BUILD SUCCESS`，`fist-support-web` 8 个用例、`fist-support-security` 20 个用例、`fist-boot-web-app` 1 个用例失败 0。
- deprecation 明细编译：`fist-boot-web-app -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，不再出现 `Breaker` / `MessageConfiguration` 相关生产提示。剩余可见项为既有 `ElementType.MODULE` 注解环境提示。
- 格式校验：`fist-boot-web-app -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

影响：

- 这是主版本破坏性 API 删除。下游仍调用 `Breaker` 的代码需要改用 `CommonErrors` / `MsgBundleRejectedException` 等当前异常模型。
- `fist-boot-web-app` 的 `i18n/common*.properties` 仍被当前异常模型使用，本轮未删除资源文件。

## 外部资源条件测试入口

日期：2026-05-13
执行者：Codex

目标：

- 为 OAuth2 授权服务 LOB 读写补充真实 JDBC 驱动验证入口。
- 为 Redisson 队列订阅补充真实 Redis 验证入口。
- 保证外部资源信息不进入仓库，默认构建不依赖外部服务。

测试入口：

| 模块 | 测试类 | 启用条件 | 默认行为 |
| --- | --- | --- | --- |
| `fist-boot-security-oauth2-server` | `ExternalJdbcAuthorizationServiceTest` | 设置 `FIST_IT_JDBC_URL` | 编译测试类，未设置环境变量时跳过 |
| `fist-redisson` | `ExternalRedisQueueConsumerTest` | 设置 `FIST_IT_REDIS_URL` | 编译测试类，未设置环境变量时跳过 |

JDBC 环境变量：

| 变量名 | 用途 |
| --- | --- |
| `FIST_IT_JDBC_URL` | JDBC URL，同时作为启用条件 |
| `FIST_IT_JDBC_USERNAME` | JDBC 用户名 |
| `FIST_IT_JDBC_PASSWORD` | JDBC 密码 |
| `FIST_IT_JDBC_DRIVER_CLASS` | 可选，外部 JDBC 驱动类名 |
| `FIST_IT_JDBC_DRIVER_JAR` | 可选，本地 JDBC 驱动 JAR 路径 |
| `FIST_IT_JDBC_INIT_SCHEMA` | 可选，显式设置为 `true` 时允许测试建表 |
| `FIST_IT_JDBC_LOB_CMP_FUNC` | 可选，数据库 LOB 比较函数 |

Redis 环境变量：

| 变量名 | 用途 |
| --- | --- |
| `FIST_IT_REDIS_URL` | Redisson single server address，同时作为启用条件 |
| `FIST_IT_REDIS_PASSWORD` | 可选，Redis 密码 |
| `FIST_IT_REDIS_DATABASE` | 可选，Redis database，默认 `0` |

执行命令：

```powershell
cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=ExternalJdbcAuthorizationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-infra/fist-redisson -am "-Dtest=ExternalRedisQueueConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- OAuth2 JDBC 条件测试为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 1 个。
- Redisson Redis 条件测试为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 1 个。
- MySQL 8 真实环境验证为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 0。
- PostgreSQL 16 首次真实环境验证失败，失败原因是官方 schema 使用 `blob` 类型；加入 PostgreSQL schema 变体后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 0。
- Redis 7 首次真实环境验证失败，失败原因是测试写入队列名与 `@QueueConsumer("test")` 订阅队列名不一致；加入测试专用 `NameGenerator` 后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 0。
- Redis 条件测试无环境变量回归为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 1 个。
- 达梦首次真实验证因账号认证状态失败；更新测试凭据后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 0。

错误尝试和修正：

- `ExternalJdbcAuthorizationServiceTest` 首次运行被 `spring-javaformat:validate` 拦截，已运行目标模块 `spring-javaformat:apply` 修正。
- `ExternalRedisQueueConsumerTest` 首次编译出现过时 API 提示，根因是 `BaseConfig#setPassword(String)` 已废弃；已改用 `Config#setPassword(String)`。
- `ExternalRedisQueueConsumerTest` 首次 Redis 7 真实验证能连接 Redis，但消费超时。根因是测试写入 `queue:<random>`，处理器实际按名称生成器订阅 `@QueueConsumer("test")` 对应队列；已改为用测试专用 `NameGenerator` 将注解值映射到本次运行唯一队列名。
- 达梦首次验证失败后，只做了一次用户名大小写最小重试；数据库返回登录失败次数限制后停止继续尝试，避免扩大账号锁定影响。
- 两个目标测试按顺序运行，避免重叠 Maven 反应堆并发写入 `target`。
- 两个目标模块组合 `spring-javaformat:validate` 首次被 Maven Central BOM POM 传输失败和本地缓存阻断；使用 `-U` 刷新解析后重跑通过。
- PostgreSQL 16 首次真实验证暴露 `type "blob" does not exist`，已在测试初始化 schema 时按 PostgreSQL 转换为 `text` / `timestamptz`。

安全边界：

- 仓库只记录环境变量名和用途，不记录任何真实连接串、主机、端口、schema、用户名或密码。
- 测试清理只限定到本次生成的授权 ID 或队列名，不执行全表清空或共享资源重置。

## 2026-05-13：废弃告警基线复核

在 MySQL 8、PostgreSQL 16、达梦和 Redis 7 外部资源矩阵完成后，重新做一轮面向当前仓库状态的废弃告警审计。目的不是继续沿用旧文档的「下一步」，而是确认当前代码还剩哪些真实的 Boot 4 迁移问题。

官方资料补充：

- Spring Boot Context7 文档 `/spring-projects/spring-boot/v4.0.3` 明确 `spring-boot-properties-migrator` 仍是配置项迁移的官方辅助工具，适合应用级验证阶段临时接入。
- 同一份文档也说明 Boot 4 默认优先使用 Jackson 3，如需迁移缓冲可临时启用 `spring.jackson.use-jackson2-defaults=true`，但当前仓库本轮没有新增该兼容开关。

本轮先执行三组 `clean compile`：

```powershell
cmd /c mvn -U -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
cmd /c mvn -U -pl fist-kit-infra/fist-sde/fist-sde-client,fist-kit-infra/fist-sde/fist-sde-boot-starter,fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
cmd /c mvn -U -pl fist-kit-app/fist-web/fist-support-web,fist-kit-app/fist-web/fist-boot-web-app,fist-kit-app/fist-data/fist-support-data,fist-kit-app/fist-data/fist-boot-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
```

第一轮结果：

- SDE / Feign 组合为 `BUILD SUCCESS`。
- `fist-boot-apidoc` `clean compile` 失败。
- Web / Data 组合 `clean compile` 失败，日志表现为依赖模块 `target/classes` 下类文件 `NoSuchFileException`。

错误尝试和修正：

- 如果直接把第一次 `clean compile` 的失败当作新的 Boot 4 阻塞点，会把 reactor 清理副作用和真实源码问题混在一起，误导后续排查顺序。
- 复查发现 `fist-boot-apidoc`、`fist-support-web`、`fist-support-data` 当前源码树已经没有此前文档记录的 `JsonNode.fields()`、`ObjectUtils.defaultIfNull(...)`、`Validate.notNull(...)`、`MutableObject.getValue()`、`SecurityJackson2Modules`、`OAuth2AuthorizationServerJackson2Module` 等直观迁移点。
- 因此第二轮改为不带 `clean` 的定向 `compile` 审计，专门验证当前源码是否仍有真实废弃告警。

第二轮执行：

```powershell
cmd /c mvn -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile
cmd /c mvn -pl fist-kit-app/fist-data/fist-support-data -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile
cmd /c mvn -pl fist-kit-app/fist-boot-apidoc -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile
```

第二轮结果：

- 三个目标模块均为 `BUILD SUCCESS`。
- 日志未命中生产代码 `deprecated` 提示。
- 当前唯一稳定复现的编译告警来自 JDK 8 `release` 模块中的 `package-info.java`：`@NullMarked` 元注解包含 `ElementType.MODULE`，在 `javac --release 8` 下会报告「未知的枚举常量 `java.lang.annotation.ElementType.MODULE`」。

当前结论：

- 历史文档中的若干「下一步」已经过时，例如 Groovy 测试迁移、真实数据库矩阵和 Sentinel / Nacos 复查都已完成。
- 当前主代码层面没有新暴露出大批 Boot 4 废弃 API 调用点。
- 升级工作重点应从「继续扫废弃 API」切换到「应用级启动、自动配置装配和跨模块组合验证」。

## 2026-05-13：OAuth2 Server 自动配置最小上下文

在 API Doc 自动配置最小上下文验证完成后，继续为 OAuth2 Server 模块补最小装配证据。该模块此前已有授权服务、失败处理器和 password grant 相关功能测试，但缺少 Boot 4 下的自动配置入口验证。

执行内容：

- 在 `fist-boot-security-oauth2-server` 中补充 `spring-boot-starter-test`。
- 新增 `AuthorizationServerAutoConfigurationTest`，使用 `ApplicationContextRunner` 验证 `AuthorizationServerAutoConfiguration` 默认装配。
- 同时验证 `@Oauth2AuthorizationServer` 注解能否继续正确导入自动配置。

执行命令：

```powershell
cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=AuthorizationServerAutoConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am spring-javaformat:validate
```

结果：

- 首次执行被 `spring-javaformat:validate` 拦截；运行目标模块 `spring-javaformat:apply` 后重跑通过。
- OAuth2 Server 自动配置测试为 `BUILD SUCCESS`，测试 2 个，失败 0，错误 0，跳过 0。
- 本轮确认 `OauthUserPropExporter`、`AuthenticationFailureHandler`、`LogoutSuccessHandler`、`AuthenticationConverter` 和 `OAuth2TokenGenerator` 在最小上下文中均可装配。
- 本轮同时确认 `@Oauth2AuthorizationServer` 注解导入路径未因 Boot 4 升级而漂移。

## 2026-05-13：OAuth2 Server 自动配置与 JDBC 授权服务组合上下文

在 OAuth2 Server 自动配置最小上下文通过后，继续扩大到「自动配置 + 应用侧 JDBC 授权服务」组合验证。该验证不连接外部数据库，使用 H2 和 Spring Authorization Server 官方 schema 复现应用侧授权服务装配。

执行内容：

- 新增 `AuthorizationServerComposedContextTest`。
- 使用 `ApplicationContextRunner` 同时加载 `AuthorizationServerAutoConfiguration` 和测试侧 `AuthorizationServiceConfiguration`。
- 测试侧提供 H2 `DataSource`、`JdbcTemplate`、`RegisteredClientRepository` 和 `DefaultAuthorizationService`。
- 验证 `OAuth2TokenGenerator` 来自自动配置，`ManagedOAuth2AuthorizationService` 来自应用侧配置。
- 保存 `OAuth2Authorization` 后，分别按 ID 和 access token 读取。

执行命令：

```powershell
cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=AuthorizationServerComposedContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am spring-javaformat:validate
```

结果：

- 首次执行在测试前被 `spring-javaformat:validate` 拦截，原因为新增测试文件格式未应用项目规则。
- 执行 `cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server spring-javaformat:apply` 后，定向测试重跑通过。
- OAuth2 Server 组合上下文测试为 `BUILD SUCCESS`，测试 1 个，失败 0，错误 0，跳过 0。
- OAuth2 Server 依赖链 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

## 2026-05-13：JDK 8 release 模块 JSpecify package 默认非空告警清理

废弃告警基线复核后，继续处理 `fist-kit-infra` 中要求 Java 8 release 兼容的模块。此前这些模块的部分 `package-info.java` 已迁移到 JSpecify `@NullMarked`，但 `javac --release 8` 会报告「未知的枚举常量 `java.lang.annotation.ElementType.MODULE`」。

根因确认：

- JSpecify `1.0.0` 的 `@NullMarked` classfile 可被 Java 8 读取。
- `javap` 复核显示 `@NullMarked` 的 `@Target` 包含 `ElementType.MODULE`。
- `ElementType.MODULE` 是 Java 9 枚举值，JDK 8 release 编译环境读取该元数据时会产生 classfile 告警。

错误尝试和修正：

- 最小复现先在 `fist-kit-api` 上确认该告警可稳定出现。
- 尝试添加 `-Xlint:-classfile` 后，告警仍然出现，说明它不能解决本问题。
- `-Xlint:none` 和 `-nowarn` 能压掉告警，但范围过大，会同时隐藏后续真实编译告警，因此没有采用。
- 最终处理方式为：仅从 JDK 8 release 模块的 `package-info.java` 移除包级 `@NullMarked`；源码中显式 `@Nullable` 保留，继续表达可空契约。
- 目标模块组合编译首次受沙箱内 Maven 依赖缓存 / 网络访问限制影响，使用已批准的提升权限 Maven 命令重跑后通过。

受影响模块：

- `fist-kit-infra/fist-kit-api`
- `fist-kit-infra/fist-jackson`
- `fist-kit-infra/fist-logback`
- `fist-kit-infra/fist-mybatis-plus`
- `fist-kit-infra/fist-sde/fist-sde-core`
- `fist-kit-infra/fist-sde/fist-sde-client`

执行命令：

```powershell
cmd /c mvn -l .codex/jspecify-release8-fist-kit-api-compile.log -pl fist-kit-infra/fist-kit-api -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
cmd /c mvn -l .codex/jspecify-release8-modules-compile.log -pl fist-kit-infra/fist-kit-api,fist-kit-infra/fist-jackson,fist-kit-infra/fist-logback,fist-kit-infra/fist-mybatis-plus,fist-kit-infra/fist-sde/fist-sde-core,fist-kit-infra/fist-sde/fist-sde-extra,fist-kit-infra/fist-sde/fist-sde-client -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile
cmd /c mvn -l .codex/jspecify-release8-format-validate.log -pl fist-kit-infra/fist-kit-api,fist-kit-infra/fist-jackson,fist-kit-infra/fist-logback,fist-kit-infra/fist-mybatis-plus,fist-kit-infra/fist-sde/fist-sde-core,fist-kit-infra/fist-sde/fist-sde-client spring-javaformat:validate
```

结果：

- `fist-kit-api -am clean compile` 为 `BUILD SUCCESS`。
- JDK 8 release 相关 infra 模块组合 `clean compile` 为 `BUILD SUCCESS`。
- 目标模块 `spring-javaformat:validate` 为 `BUILD SUCCESS`。
- 编译日志复扫未命中 `WARNING` 或 `ElementType.MODULE`。

## 2026-05-13：SDE 示例应用组合测试

在 Gateway、API Doc、OAuth2 Server 和 SDE / Feign 组件级验证之后，继续扩大到 `examples/fist-sde` 示例应用。该验证用于确认 Boot 4 下示例应用能否完成真实启动和部分 HTTP 调用。

执行命令：

```powershell
cmd /c mvn -l .codex/sde-examples-module-test.log -f examples/fist-sde/pom.xml test
cmd /c mvn -l .codex/sde-components-install.log -pl fist-kit-infra/fist-sde/fist-sde-boot-starter -am -DskipTests install
cmd /c mvn -l .codex/sde-examples-module-test-after-install.log -f examples/fist-sde/pom.xml test
cmd /c mvn -l .codex/sde-examples-module-test-after-webserver-import.log -f examples/fist-sde/pom.xml test
```

错误尝试和修正：

- 首次直接执行 `examples/fist-sde` 测试失败，原因是 `examples` 不在根 reactor 中，无法解析仓库内 `4.0.0-SNAPSHOT` 的 SDE 组件。
- 先安装 `fist-sde-boot-starter -am -DskipTests install` 后，示例测试进入编译阶段。
- 第二次失败来自 `example-server` 的 `SdeServerDemoRunner` 仍引用 Boot 3 旧包 `org.springframework.boot.web.context.WebServerApplicationContext`。
- Boot 4 本地 JAR 和同组示例代码都确认新包为 `org.springframework.boot.web.server.context.WebServerApplicationContext`，已做单点迁移。

结果：

- SDE 组件本地安装为 `BUILD SUCCESS`。
- 修正 import 后，`examples/fist-sde` 整组测试为 `BUILD SUCCESS`。
- `SdeServerExampleApplicationTest`、`SdeFeignExampleApplicationTest`、`SdeRestClientExampleApplicationTest`、`SdeWebClientExampleApplicationTest` 各运行 1 个用例，失败 0，错误 0，跳过 0。
- 当前示例验证覆盖 server 与 feign 的真实 HTTP 调用，以及 restclient / webclient 应用上下文启动。

## 2026-05-13：Security 自动配置注册与 Web/Data/Security 组合验证

SDE 示例组合测试通过后，继续补 Web/Data/Security 组合装配验证。复查发现 `fist-boot-security` 的自动配置注册文件仍指向 Web 模块自动配置，没有注册自身的 `InnerSecurityConfiguration`。

执行内容：

- 为 `fist-boot-security` 补充测试依赖：`spring-boot-starter-test`、`spring-boot-starter-web`、`fist-boot-web-app`、`fist-boot-data`。
- 新增 `InnerSecurityConfigurationImportsTest`。
- 将 `InnerSecurityConfiguration` 调整为 `@AutoConfiguration`。
- 将 `fist-boot-security` 的 `AutoConfiguration.imports` 和 `spring.factories` 改为注册 `InnerSecurityConfiguration`。
- 补充 Servlet Web/Data/Security 组合上下文验证，覆盖 `TenantInvokeAspect`、`PasswordEncoder`、`UserDecoder`、`UserInfoSupplier`、`TraceInfoResolver`、`tenantContextFilter`、`headerMdcFilter` 和 `trustedUserFilter`。

执行命令：

```powershell
cmd /c mvn -l .codex/boot-security-imports-red.log -pl fist-kit-app/fist-security/fist-boot-security -am "-Dtest=InnerSecurityConfigurationImportsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -l .codex/boot-security-format-apply.log -pl fist-kit-app/fist-security/fist-boot-security spring-javaformat:apply
cmd /c mvn -l .codex/boot-security-imports-green-after-format.log -pl fist-kit-app/fist-security/fist-boot-security -am "-Dtest=InnerSecurityConfigurationImportsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -l .codex/boot-security-composed-context-test.log -pl fist-kit-app/fist-security/fist-boot-security -am "-Dtest=InnerSecurityConfigurationImportsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -l .codex/boot-security-composed-context-test-2.log -pl fist-kit-app/fist-security/fist-boot-security -am "-Dtest=InnerSecurityConfigurationImportsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cmd /c mvn -l .codex/boot-security-module-test-after-autoconfig.log -pl fist-kit-app/fist-security/fist-boot-security -am test
cmd /c mvn -l .codex/boot-security-format-validate-after-autoconfig.log -pl fist-kit-app/fist-security/fist-boot-security -am spring-javaformat:validate
```

错误尝试和修正：

- RED 测试失败原因符合预期：自动配置候选中缺少 `InnerSecurityConfiguration`，反而包含 Web 自动配置类。
- 修正自动配置注册后，首次 GREEN 被 `spring-javaformat:validate` 拦截，运行目标模块 `spring-javaformat:apply` 后通过。
- 新增 Web/Data 测试依赖后，`ImportCandidates.load(...)` 会同时读取依赖模块的自动配置候选；如果继续断言候选中不包含 Web 自动配置，会误判。
- 修正方式为使用只包含 `fist-boot-security` 自身输出目录的 `URLClassLoader` 读取候选，专门验证当前模块的自动配置注册内容。

结果：

- `InnerSecurityConfigurationImportsTest` 定向测试为 `BUILD SUCCESS`，2 个用例，失败 0，错误 0，跳过 0。
- `fist-boot-security -am test` 为 `BUILD SUCCESS`。
- `fist-boot-security -am spring-javaformat:validate` 为 `BUILD SUCCESS`。
- 当前已具备 Web/Data/Security Servlet 组合上下文证据。

## 2026-05-13：Spring Boot 配置项 migrator 使用流程验证

根据 Spring Boot Context7 文档 `/spring-projects/spring-boot/v4.0.3`，`spring-boot-properties-migrator` 应作为 runtime 依赖临时加入应用，用于启动时分析环境、打印迁移诊断，并在迁移完成后移除。本轮选择已有应用级入口 `examples/fist-sde` 验证 migrator 接入、依赖确认、日志检查和依赖移除流程。

执行内容：

- 临时在 `examples/fist-sde/pom.xml` 加入 `org.springframework.boot:spring-boot-properties-migrator`，scope 为 `runtime`。
- 执行 `examples/fist-sde` 整组测试，覆盖 server、feign、restclient、webclient 四个示例应用。
- 使用 dependency tree 确认 migrator 已进入示例 runtime classpath。
- 验证完成后移除临时 migrator 依赖，不将其保留到源码中。

执行命令：

```powershell
cmd /c mvn -l .codex/properties-migrator-sde-examples.log -f examples/fist-sde/pom.xml test
cmd /c mvn -l .codex/properties-migrator-sde-dependency-tree.log -f examples/fist-sde/pom.xml -pl :example-sde-server dependency:tree "-Dincludes=org.springframework.boot:spring-boot-properties-migrator"
```

错误尝试和修正：

- 第一次执行 dependency tree 时使用 `-pl example-sde-server`，Maven 报告 reactor 中找不到该项目；修正为 `-pl :example-sde-server`。
- 第二次执行 dependency tree 时未给 `-Dincludes=org.springframework.boot:spring-boot-properties-migrator` 加引号，`cmd /c mvn` 将冒号后的内容误解析为插件前缀，报 `No plugin found for prefix '.springframework.boot'`；加引号后通过。
- 验证完成后移除临时依赖时，`examples/fist-sde/pom.xml` 只剩换行脏标记，已恢复到 HEAD。

结果：

- `examples/fist-sde` 在临时接入 migrator 后整组测试为 `BUILD SUCCESS`。
- 四个示例测试各运行 1 个用例，失败 0，错误 0，跳过 0。
- dependency tree 确认 `example-sde-server` runtime classpath 包含 `spring-boot-properties-migrator:4.0.6`。
- migrator 日志未出现 `deprecated`、`renamed`、`replacement` 或 `PropertiesMigration` 运行期诊断。
- 当前 SDE 示例配置未发现需要由 Boot 4 migrator 迁移的配置项；该结论只覆盖仓库示例配置和本次测试启动路径。
- `spring-boot-properties-migrator` 的主要价值在应用侧，尤其是内部项目的真实 `application.yml`、环境变量、启动参数和配置中心配置组合。
- FIST Kit 仓库内验证只能证明 migrator 使用流程可执行，不能替代内部项目的配置迁移验证。

## 2026-05-13：Web/Data/Security/API Doc 示例应用验证

为补齐真实应用级覆盖，本轮新增并扩展 `examples/fist-web-data-security/example-servlet`。该示例组合 `fist-boot-web-app`、`fist-boot-data`、`fist-boot-security` 和 `fist-boot-apidoc`，使用 Boot 4、Tomcat 11、H2、Spring Security、SpringDoc 3 和真实 HTTP 请求验证 starter 装配。

执行内容：

- 新增 examples 子模块 `fist-web-data-security` 和子示例 `example-servlet`。
- 示例应用提供 GET `/api/profiles/{id}`，覆盖 H2 查询、`Pageable` 参数解析、请求 ID 读取和可信用户头解析。
- 示例应用提供 POST `/api/profiles`，覆盖 `ObjectValidator` 在真实请求中的调用。
- 示例测试断言 FIST Web/Data/Security 的关键 bean 和 3 个 filter 已装配。
- 示例测试请求 `/v3/api-docs`，覆盖 SpringDoc WebMVC endpoint、FIST API Doc `OpenAPI` bean 和 `inner_auth` 安全方案。

执行命令：

```powershell
cmd /c mvn -l .codex/web-data-security-example-test-3.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am test
cmd /c mvn -l .codex/web-data-security-example-format-validate.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am spring-javaformat:validate
cmd /c mvn -l .codex/web-data-security-apidoc-example-test.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am test
cmd /c mvn -l .codex/web-data-security-apidoc-example-format-validate.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am spring-javaformat:validate
```

错误尝试和修正：

- 第一次测试编译失败，原因是 `assertThat(applicationContext)` 只得到 AssertJ 普通对象断言，不支持 Spring Boot 的 `hasSingleBean`；改为 `applicationContext.getBeansOfType(...).hasSize(1)`。
- 第二次测试运行失败，原因是 FIST Jackson 自动配置会将 `long id` 序列化为字符串；断言改为 `"1"` / `"2"` 后通过。

结果：

- Web/Data/Security 示例定向测试为 `BUILD SUCCESS`。
- 新示例测试 1 个用例，失败 0，错误 0，跳过 0。
- 该测试启动真实 Servlet Web 环境，并通过 GET / POST HTTP 请求覆盖 Web、Data、Security 自动配置组合。
- 格式命令为 `BUILD SUCCESS`；examples 父模块当前配置为跳过 spring-javaformat 校验。
- 扩展 API Doc 后，示例定向测试仍为 `BUILD SUCCESS`，新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- `/v3/api-docs` 响应包含 `inner_auth` 安全方案、`X-USER-IN` header 和 `/api/profiles/{id}`、`/api/profiles` 路径。

## OAuth2 Server 示例级 HTTP 验证

日期：2026-05-14
执行者：Codex

本轮新增 `examples/fist-oauth2-server/example-authorization-server`，用于覆盖 `fist-boot-security-oauth2-server` 在 Spring Boot 4 + Spring Authorization Server 7 下的应用级端点请求。

验证命令：

```powershell
cmd /c mvn -l .codex/oauth2-server-example-test.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test
cmd /c mvn -l .codex/oauth2-server-example-test-2.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test
cmd /c mvn -l .codex/oauth2-server-example-dependency-tree-3.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am dependency:tree "-Dincludes=org.springframework:spring-jcl,commons-logging:commons-logging"
cmd /c mvn -l .codex/oauth2-server-example-full-dependency-tree.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am dependency:tree
cmd /c mvn -l .codex/oauth2-server-example-test-3.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test
cmd /c mvn -l .codex/oauth2-server-example-format-validate.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate
cmd /c mvn -l .codex/oauth2-server-example-test-4.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test
cmd /c mvn -l .codex/oauth2-server-example-test-5.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test
cmd /c mvn -l .codex/oauth2-server-example-format-validate-2.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate
cmd /c mvn -l .codex/oauth2-server-example-test-6.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test
cmd /c mvn -l .codex/oauth2-server-example-format-validate-3.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate
```

错误尝试和修正：

- 第一次测试编译失败，原因是 AssertJ 对 `Iterable<?>` 的 `contains(String)` 泛型捕获不匹配；改用 `asInstanceOf(LIST)` 后通过编译。
- 第二次测试运行失败，原因是目标示例 classpath 缺少 `commons-logging:commons-logging`；示例 POM 显式声明该标准依赖后启动通过。
- 首次执行 dependency tree 时未给 `-Dincludes=org.springframework:spring-jcl,commons-logging:commons-logging` 加引号，Maven 将参数误解析为插件前缀；重跑时加引号。

结果：

- OAuth2 Server 示例定向测试为 `BUILD SUCCESS`。
- 新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- 测试启动真实 Tomcat 11，访问 `/.well-known/oauth-authorization-server` 并断言 issuer、token endpoint、`client_credentials` 和 `client_secret_basic`。
- 测试通过 HTTP POST `/oauth2/token` 完成 client credentials token 请求，并通过 `DefaultAuthorizationService` 回查 access token claims，确认 FIST `DefaultOAuth2AccessTokenGenerator` 已参与颁发。
- 回归测试 `oauth2-server-example-test-4.log`、`oauth2-server-example-test-5.log` 和 `oauth2-server-example-test-6.log` 均为 `BUILD SUCCESS`。
- 格式命令为 `BUILD SUCCESS`；examples 父模块当前配置为跳过 spring-javaformat 校验。

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
- `.codex/gateway-oauth2-config-test.log`
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
- `.codex/external-jdbc-conditional-test.log`
- `.codex/external-redis-conditional-test.log`
- `.codex/boot4-deprecation-apidoc.log`
- `.codex/boot4-deprecation-feign-sde.log`
- `.codex/boot4-deprecation-web-data.log`
- `.codex/boot4-deprecation-support-web.log`
- `.codex/boot4-deprecation-support-data.log`
- `.codex/boot4-deprecation-apidoc-noclean.log`
- `.codex/oauth2-server-autoconfig-test.log`
- `.codex/oauth2-server-composed-context-test.log`
- `.codex/jspecify-release8-fist-kit-api-compile.log`
- `.codex/jspecify-release8-modules-compile.log`
- `.codex/jspecify-release8-format-validate.log`
- `.codex/sde-examples-module-test.log`
- `.codex/sde-components-install.log`
- `.codex/sde-examples-module-test-after-install.log`
- `.codex/sde-examples-module-test-after-webserver-import.log`
- `.codex/boot-security-imports-red.log`
- `.codex/boot-security-format-apply.log`
- `.codex/boot-security-imports-green-after-format.log`
- `.codex/boot-security-composed-context-test.log`
- `.codex/boot-security-composed-context-test-2.log`
- `.codex/boot-security-module-test-after-autoconfig.log`
- `.codex/boot-security-format-validate-after-autoconfig.log`
- `.codex/properties-migrator-sde-examples.log`
- `.codex/properties-migrator-sde-dependency-tree.log`
- `.codex/web-data-security-example-test-3.log`
- `.codex/web-data-security-example-format-validate.log`
- `.codex/web-data-security-apidoc-example-test.log`
- `.codex/web-data-security-apidoc-example-format-validate.log`
- `.codex/oauth2-server-example-test.log`
- `.codex/oauth2-server-example-test-2.log`
- `.codex/oauth2-server-example-test-3.log`
- `.codex/oauth2-server-example-test-4.log`
- `.codex/oauth2-server-example-test-5.log`
- `.codex/oauth2-server-example-test-6.log`
- `.codex/oauth2-server-example-format-validate.log`
- `.codex/oauth2-server-example-format-validate-2.log`
- `.codex/oauth2-server-example-format-validate-3.log`
- `.codex/oauth2-server-example-dependency-tree.log`
- `.codex/oauth2-server-example-dependency-tree-2.log`
- `.codex/oauth2-server-example-dependency-tree-3.log`
- `.codex/oauth2-server-example-full-dependency-tree.log`

## 资料来源

- Spring Boot 4.0 Migration Guide：`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`
- Spring Boot 4.0.6 发布记录：`https://spring.io/blog/2026/04/23/spring-boot-4-0-6-available-now`
- Spring Boot 4.0.3 外部化配置文档：`https://github.com/spring-projects/spring-boot/blob/v4.0.3/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/features/external-config.adoc`，确认 `spring.config.import` 支持 `optional:file:` 和 `optional:configtree:`。
- Spring Boot Context7 文档：`/spring-projects/spring-boot`，`DataSourceAutoConfiguration` 位于 `org.springframework.boot.jdbc.autoconfigure`
- Spring Framework 当前 Javadoc Context7 文档：`/websites/spring_io_spring-framework_current_javadoc-api`，`LobCreator` / `LobHandler` 自 6.2 起弃用，替代项为 `SqlBinaryValue` / `SqlCharacterValue`
- Apache Commons Lang Context7 文档：`/websites/commons_apache_proper_commons-lang_apidocs`，`ObjectUtils.defaultIfNull(T, T)` 弃用并推荐 `ObjectUtils.getIfNull(Object, Object)`
- Spring JDBC `7.0.7` 本地 JAR：`F:\pkg-maven\repository\org\springframework\spring-jdbc\7.0.7\spring-jdbc-7.0.7.jar`
- Spring Authorization Server `7.0.5` 本地 JAR：`F:\pkg-maven\repository\org\springframework\security\spring-security-oauth2-authorization-server\7.0.5\spring-security-oauth2-authorization-server-7.0.5.jar`，确认内置 `oauth2-authorization-schema.sql`
- Spring Authorization Server Context7 文档：`/spring-projects/spring-authorization-server`，确认自定义 token 生成应通过 `OAuth2TokenGenerator` / `DelegatingOAuth2TokenGenerator` 配置。
- Spring Authorization Server Context7 文档：`/spring-projects/spring-authorization-server`，确认最小默认配置可使用 `OAuth2AuthorizationServerConfiguration`，自定义配置可使用 `OAuth2AuthorizationServerConfigurer` 注入 `RegisteredClientRepository`、`OAuth2AuthorizationService`、`AuthorizationServerSettings` 和 `OAuth2TokenGenerator`。
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
- Redisson `4.3.1` 本地 JAR：`F:\pkg-maven\repository\org\redisson\redisson\4.3.1\redisson-4.3.1.jar`，`javap -v` 确认 `BaseConfig#setPassword(String)` 已废弃，并在字节码提示中建议改用 `Config` 的 password 设置。
- Spring Boot Context7 文档：`/spring-projects/spring-boot/v4.0.3`，确认 `spring-boot-properties-migrator` 可作为配置项迁移辅助依赖，并说明 Boot 4 默认优先使用 Jackson 3。
- 本地 OpenRewrite dry-run patch：`target/rewrite/rewrite.patch`

## 内部应用配置迁移验证策略修正

记录日期：2026-05-14；记录者：Codex。

用户反馈不建议从 Nacos 加载配置，因为升级验证需要频繁调整。复核 Spring Boot 4 外部化配置文档和内部应用配置结构后，调整策略如下：

- 不把 Nacos 导入作为 migrator 首轮验证前置条件。
- 内部应用默认 profile 仍保留 Nacos 路径；升级验证新增本地配置源路径，不改变生产配置约定。
- 首轮优先使用 `spring.config.import=optional:file:...` 直接导入配置模板中的公共配置和服务级配置。
- `config-server-spring` profile 属于遗留配置，本地没有可用 Spring Cloud Config Server，因此当前不走 Config Server native/Git 后端。
- Nacos 只放在本地配置验证通过后的回归阶段，用于确认生产式配置中心路径没有额外兼容问题。

内部应用当前结构：

- 服务本地 `application.yml` 统一导入 `classpath:features.yml`。
- 默认 profile 的 `features.yml` 导入 `nacos:application.yml` 和 `nacos:${spring.application.name}.yml`；`fist-iam` 额外导入 `nacos:${spring.application.name}-task.yml`。
- `config-server-spring` profile 已存在，并使用 `spring.config.import: configserver:`；该路径是遗留配置，本地没有对应服务。
- 配置模板位于内部项目的 `docs/config-template/spring-config-server/pac-platform`，包含公共配置、服务配置、任务配置和连接池配置模板。

推荐验证顺序：

1. 在内部应用侧临时接入 `spring-boot-properties-migrator`，scope 为 `runtime`。
2. 先选择 `fist-monitor` 做最小服务启动验证，使用本地文件导入配置模板。
3. 根据 migrator 输出和启动错误调整本地模板或启动参数；日志只保留配置键类别和问题类别。
4. 扩展到 `fist-auth`、`fist-iam`、`fist-gateway`。
5. 最后再决定是否做 Nacos 回归验证。

本地 file import 建议：

- 使用独立 profile，例如 `boot4-local`，避免 `default` profile 激活 `features.yml` 中的 Nacos import。
- 启动参数直接声明 `--spring.config.import=optional:file:<模板目录>/application.yml,optional:file:<模板目录>/<服务名>.yml`，不写入真实用户名、密码或连接串。
- `fist-iam` 需要额外导入任务配置模板；其他服务先导入公共配置和对应服务配置。

首个服务建议命令模板：

```powershell
cmd /c mvn -pl common/fist-common -am "-P!config-nacos" -DskipTests install
cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" spring-boot:start spring-boot:stop "-Dspring-boot.run.main-class=com.power4j.fist.monitor.MonitorApplication" "-Dspring-boot.run.profiles=boot4-local" "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --spring.config.import=optional:file:<内部项目根目录>/docs/config-template/spring-config-server/pac-platform/application.yml,optional:file:<内部项目根目录>/docs/config-template/spring-config-server/pac-platform/fist-monitor.yml --spring.cloud.nacos.config.enabled=false --spring.cloud.nacos.config.import-check.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.nacos.discovery.enabled=false --spring.cloud.service-registry.auto-registration.enabled=false --server.port=0"
```

该命令的目的不是验证 Nacos Discovery，而是先验证应用在本地模板配置输入下能否完成环境构建，并为后续临时接入 `spring-boot-properties-migrator` 提供稳定启动路径。`-P!config-nacos` 用于尝试关闭默认激活的 Nacos Config 依赖；运行参数中的 Nacos Config 和 discovery 开关用于避免本地配置迁移阶段尝试读取或注册远程配置中心。

首轮验证结果：

- `cmd /c mvn -pl fist-cloud/fist-monitor -am "-P!config-nacos" spring-boot:run ...` 失败，原因是 `spring-boot:run` 会在 reactor 中先对父 POM 执行，父 POM 没有 main class。
- `cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" spring-boot:run ...` 失败，原因是只运行目标模块时，内部依赖 `fist-common:9.2.0` 尚未安装到本地 Maven 仓库。
- `cmd /c mvn -pl common/fist-common -am "-P!config-nacos" -DskipTests install` 通过，将父 POM、`common` POM 和 `fist-common` 安装到本地 Maven 仓库。
- 关闭 Nacos Config import check 之前，应用启动失败，错误为缺少 `nacos:` import；这说明本地 file import 验证需要同时设置 `spring.cloud.nacos.config.enabled=false` 和 `spring.cloud.nacos.config.import-check.enabled=false`。
- `cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" spring-boot:start spring-boot:stop ...` 通过；`MonitorApplication` 使用 `boot4-local` profile 完成启动，并由 Maven stop goal 停止。
- 启动日志会打印环境变量摘要。本轮没有加载真实 `.env.local` 值；后续接入真实环境变量时必须对日志采集做脱敏，避免记录连接串、用户名或密码。

## 内部应用 fist-monitor 配置 migrator 机制验证

记录日期：2026-05-14；记录者：Codex。

在 Boot 3.5.11 基线下，使用 `spring-boot.run.additional-classpath-elements` 将 `spring-boot-properties-migrator` 临时追加到 `fist-monitor` 启动 classpath，验证应用侧 migrator 接入机制。该验证不修改内部应用 POM，也不代表 Boot 4 最终配置迁移诊断已经完成；Boot 4 诊断应在内部应用依赖升级到 Boot 4 后，使用匹配的 migrator 版本重新执行。

有效命令要点：

```powershell
cmd /c mvn dependency:get "-Dartifact=org.springframework.boot:spring-boot-properties-migrator:3.5.11"
cmd /c mvn dependency:get "-Dartifact=org.springframework.boot:spring-boot-configuration-metadata:3.5.11"
cmd /c mvn dependency:get "-Dartifact=com.vaadin.external.google:android-json:0.0.20131108.vaadin1"
cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" spring-boot:start spring-boot:stop "-Dspring-boot.run.additional-classpath-elements=<migrator.jar>,<configuration-metadata.jar>,<android-json.jar>" ...
```

错误尝试：

- 只追加 `spring-boot-properties-migrator` 主 jar 时，启动失败，缺少 `ConfigurationMetadataRepositoryJsonBuilder`。
- 追加 migrator 和 `spring-boot-configuration-metadata` 后，启动仍失败，缺少 `org.json.JSONObject`。
- `spring-boot.run.additional-classpath-elements` 不会自动解析传递依赖，必须显式列出 migrator、configuration metadata 和 `android-json` 三个 jar。

验证结果：

- 补齐三个 jar 后，`fist-monitor` 在 `boot4-local` profile、本地 `file:` 配置模板、禁用 Nacos Config / Discovery 的条件下启动并停止成功。
- 应用日志未出现 `PropertiesMigration`、`deprecated`、`renamed` 或 `replacement` 诊断。
- Maven `-l` 只能记录 Maven 主进程，不能完整记录 forked 应用日志；migrator 诊断需要从应用自身日志或控制台输出中采集。
- 内部项目工作区保持干净；生成日志和 target 输出不进入 Git 跟踪。

## 内部应用 fist-monitor Boot 4 试编译

记录日期：2026-05-14；记录者：Codex。

为避免直接修改内部项目，先将内部项目复制到主仓库 `target/` 下的临时副本，并排除 `.git`、`target`、`logs`、`.idea` 和 `.env.local`。临时副本只用于试编译，不进入 Git 跟踪。

临时版本矩阵：

- Spring Boot：`4.0.6`
- Spring Cloud：`2025.1.1`
- Spring Cloud Alibaba：`2025.1.0.0`
- FIST Kit：`4.0.0-SNAPSHOT`
- Spring Boot Admin：`4.0.4`
- SpringDoc：`3.0.3`
- Spring Authorization Server：`7.0.5`

错误尝试：

- 根 POM 直接切到 Boot 4 后，即使只指定 `-pl fist-cloud/fist-monitor -am`，Maven 仍会先读取全 reactor。其他模块中的 `spring-boot-starter-undertow` 在 Boot 4 下不再由 Boot parent 管理，导致项目读取阶段失败。
- 为聚焦 `fist-monitor`，临时副本中将根模块收窄为 `common`、`fist-cloud`，并将 `fist-cloud` 子模块收窄为 `fist-monitor`。
- 收窄 reactor 后，编译进入 `fist-common`，但本地 Maven 仓库缺少 FIST Kit v4 的 `fist-kit-api` 和 `fist-support-web` jar，导致依赖解析失败。

有效方案：

- 在 FIST Kit v4 主仓库执行定向安装：`cmd /c mvn -pl fist-kit-infra/fist-kit-api,fist-kit-app/fist-web/fist-support-web -am -DskipTests install`。
- 在临时副本中执行：`cmd /c mvn -pl fist-cloud/fist-monitor -am "-P!config-nacos" -DskipTests test-compile`。

验证结果：

- FIST Kit v4 定向安装为 `BUILD SUCCESS`。
- 内部应用临时副本中，`fist-common` 编译 10 个源码文件，`fist-monitor` 编译 2 个源码文件，最终 `BUILD SUCCESS`。
- 本轮只验证 `fist-monitor` 最小编译链路，不代表 `fist-auth`、`fist-iam`、`fist-gateway` 或示例模块已经可编译。
- 本轮没有运行 Boot 4 启动验证，也没有加载 `.env.local`。

后续处理：

- 其他模块中的 `spring-boot-starter-undertow` 需要迁移到 Boot 4 的新 Web Server starter 坐标，或由应用显式声明版本。
- 后续应在 `fist-monitor` Boot 4 临时编译成功的基础上，继续做 Boot 4 启动探测和 migrator 诊断。

## 内部应用 fist-monitor Boot 4 启动与 migrator 诊断

记录日期：2026-05-14；记录者：Codex。

在 `fist-monitor` Boot 4 临时试编译通过后，继续使用临时副本验证启动路径。验证仍使用 `boot4-local` profile、本地 `file:` 配置模板、禁用 Nacos Config / Discovery，并使用 `spring-boot:start spring-boot:stop` 避免常驻进程。

启动命令要点：

- 使用独立 JMX port，避免与其他本地 Java 进程或历史探测冲突。
- 使用 `--spring.main.web-application-type=none`，先验证非 Web 环境构建。
- 使用 `--spring.config.import=optional:file:<模板目录>/application.yml,optional:file:<模板目录>/fist-monitor.yml`。
- 显式设置 `spring.cloud.nacos.config.enabled=false`、`spring.cloud.nacos.config.import-check.enabled=false`、`spring.cloud.discovery.enabled=false`、`spring.cloud.nacos.discovery.enabled=false` 和 `spring.cloud.service-registry.auto-registration.enabled=false`。

migrator 命令要点：

```powershell
cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" spring-boot:start spring-boot:stop "-Dspring-boot.run.additional-classpath-elements=<spring-boot-properties-migrator-4.0.6.jar>,<spring-boot-configuration-metadata-4.0.6.jar>" ...
```

验证结果：

- 不带 migrator 的 Boot 4 `fist-monitor` 启动/停止为 `BUILD SUCCESS`。
- 追加 Boot 4.0.6 migrator 与 configuration metadata 后，启动/停止仍为 `BUILD SUCCESS`。
- 应用日志未出现 `PropertiesMigration`、`deprecated`、`renamed` 或 `replacement` 配置迁移诊断。
- 应用日志出现 Thymeleaf 模板目录缺失警告；该警告与 Boot 4 配置迁移无关，属于当前 `fist-monitor` 非 Web 探测下可接受的运行期提示。
- Maven `-l` 仍只记录 Maven 主进程日志；应用启动日志需要从应用自身日志和控制台输出采集。

边界：

- 本轮没有加载 `.env.local`，没有连接真实数据库、Redis 或 Nacos。
- 本轮验证为非 Web 模式，不代表 Servlet 容器、端口监听或管理端点完整通过。
- 真实内部项目工作区未产生 Git 跟踪变更。

## 内部应用 fist-monitor Boot 4 Web 模式启动与 migrator 诊断

记录日期：2026-05-14；记录者：Codex。

在非 Web 启动探测通过后，继续使用同一个临时副本开启 Servlet Web 模式，验证 `fist-monitor` 在 Boot 4 版本矩阵下是否可以启动 Tomcat、应用端口和管理端点。验证仍不加载 `.env.local`，不连接真实数据库、Redis 或 Nacos，不修改真实内部项目。

启动命令要点：

- 继续使用 `boot4-local` profile、本地 `file:` 配置模板和 `spring-boot:start spring-boot:stop`。
- 移除 `--spring.main.web-application-type=none`，让应用按 Web 模式启动。
- 设置 `--server.port=0` 和 `--management.server.port=0`，避免固定端口冲突。
- 继续显式关闭 Nacos Config import check、Nacos Config、Discovery 和服务自动注册。
- migrator 轮次通过 `spring-boot.run.additional-classpath-elements` 追加 `spring-boot-properties-migrator-4.0.6.jar` 和 `spring-boot-configuration-metadata-4.0.6.jar`。

验证结果：

- 不带 migrator 的 Boot 4 Web 启动/停止为 `BUILD SUCCESS`。
- 追加 Boot 4.0.6 migrator 与 configuration metadata 后，Web 启动/停止仍为 `BUILD SUCCESS`。
- 应用日志显示 Tomcat 11 启动，应用端口和管理端口均由随机端口分配。
- Actuator 在管理端口暴露 15 个 endpoint，基础管理端点装配通过。
- 应用日志未出现 `PropertiesMigration`、`deprecated`、`renamed` 或 `replacement` 配置迁移诊断。
- 应用日志继续出现 Thymeleaf 模板目录缺失警告；该警告与配置迁移无关，需结合 `fist-monitor` 是否实际提供页面再判断是否保留 Thymeleaf。
- Web 模式新增暴露 `jakarta.validation.NoProviderFoundException` 提示：classpath 中缺少 Bean Validation provider，例如 Hibernate Validator。当前不阻塞启动，但可能影响 `@Validated`、请求参数校验、配置属性校验或管理上下文中的校验行为。

边界：

- 本轮只证明 `fist-monitor` 在本地模板配置输入下可以启动 Servlet Web 容器和管理端点，不代表业务 HTTP 接口、数据库访问、Redis、Nacos Discovery 或真实配置中心路径已经通过。
- Bean Validation provider 缺失应作为后续依赖图核查项，优先确认内部应用是否应通过 Web starter、validation starter 或业务 starter 引入 `hibernate-validator`。
- migrator 诊断仍以应用自身日志为准；Maven `-l` 日志只能证明 Maven goal 执行成功。

## 内部应用 fist-monitor Bean Validation provider 修复验证

记录日期：2026-05-14；记录者：Codex。

基于 Web 模式启动发现的 `jakarta.validation.NoProviderFoundException`，继续在临时副本中核查依赖树并验证最小修复方式。该验证只修改临时副本，不修改真实内部项目。

依赖树结论：

- 修复前，`fist-monitor -> fist-common -> fist-support-web` 只带入 `jakarta.validation:jakarta.validation-api:3.1.1`。
- 修复前，`spring-boot-starter-web:4.0.6` 没有传递带入 `hibernate-validator`。
- 因此，Web 模式下 Spring 可以看到 Jakarta Validation API，但找不到 Bean Validation provider。

临时修复方式：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

验证结果：

- 在临时副本的 `fist-monitor` 中加入 `spring-boot-starter-validation` 后，依赖树新增 `spring-boot-validation:4.0.6` 和 `hibernate-validator:9.0.1.Final`。
- 不带 migrator 的 Boot 4 Web 启动/停止为 `BUILD SUCCESS`，应用日志出现 `Hibernate Validator 9.0.1.Final` 初始化信息，未再出现新的 `NoProviderFoundException`。
- 追加 Boot 4.0.6 migrator 与 configuration metadata 后，Web 启动/停止仍为 `BUILD SUCCESS`。
- 修复后带 migrator 的 Web 启动日志未出现 `PropertiesMigration`、`deprecated`、`renamed`、`replacement` 或 `Application run failed`。

建议：

- 内部应用正式升级时，Web 应用如使用请求参数校验、方法校验或配置属性校验，应显式声明 `spring-boot-starter-validation`。
- FIST Kit 侧暂不把 Hibernate Validator 作为 `fist-support-web` 的强制传递依赖；`fist-support-web` 当前只需要编译期 API，provider 更适合作为应用侧运行时选择。
- 如果后续发现多个内部 Web 服务都需要同样依赖，可在内部项目的公共应用 BOM 或服务父 POM 中统一声明，而不是逐个服务分散补充。

## 真实内部项目 fist-monitor Boot 4 本地 profile 落地

记录日期：2026-05-18；记录者：Codex。

在真实内部项目 `pac-cloud-boot3-10.x` 的 `10.x` 分支中，已将 `fist-monitor` 的 Boot 4 本地启动隔离方案从长命令参数收敛为可提交的 profile 文件。

落地变更：

- 新增 `fist-cloud/fist-monitor/src/main/resources/application-boot4-local.yml`。
- 使用随机业务端口和随机管理端口。
- 关闭 Nacos Config、Nacos Discovery、服务自动注册和 Nacos health。
- 排除 Alibaba Nacos config、discovery、registry、loadbalancer 相关 auto-configuration。

验证命令：

```powershell
cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" -DskipTests process-resources
cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" -DskipTests "-Dspring-boot.run.profiles=boot4-local" spring-boot:start spring-boot:stop
cmd /c mvn -pl fist-cloud/fist-monitor -am "-P!config-nacos" -DskipTests test-compile
```

验证结果：

- 3 条命令均为 `BUILD SUCCESS`。
- `spring-boot:start spring-boot:stop` 激活 `boot4-local` profile，Tomcat `11.0.21` 以随机端口启动并正常停止。
- 日志显示 Hibernate Validator `9.0.1.Final` 初始化。
- 未出现 Nacos 登录、注册或连接失败日志。

注意事项：

- 直接执行 `spring-boot:start` 不保证新增 resource 已进入 `target/classes`。新增或修改 profile 文件后，应先执行 `process-resources`、`compile` 或其他会刷新 resources 的生命周期阶段。
- 本轮未加载 `.env.local`，未连接真实数据库、Redis 或 Nacos。真实资源信息仍只允许作为本机验证输入，不写入文档、测试源码或提交信息。
- 真实内部项目提交：`e7b11c60 test: add monitor boot4 local profile`。

## 内部应用 Undertow starter 迁移验证

记录日期：2026-05-14；记录者：Codex。

`fist-monitor` 试编译前曾暴露一个 reactor 读取阶段阻塞：内部项目中仍有模块在默认打包 profile 中声明 `spring-boot-starter-undertow`，Boot 4 parent 不再管理该旧 starter，Maven 在进入目标模块编译前即失败。本轮继续在临时副本中验证替代方案。

官方依据：

- Spring Boot Context7 文档 `/spring-projects/spring-boot/v4.0.3` 的 Web server how-to 说明，Servlet Web 应用默认通过 Tomcat 工作；只有明确需要替换默认容器时，才排除 Tomcat 并加入 `spring-boot-starter-jetty`。
- 同一文档的 embedded server 说明中，Servlet 栈支持 Tomcat 与 Jetty；Reactive 栈支持 Reactor Netty、Tomcat 和 Jetty。
- 本地 `spring-boot-dependencies:4.0.6` 未管理 `spring-boot-starter-undertow`，但管理 `spring-boot-starter-jetty`、`spring-boot-starter-jetty-runtime`、`spring-boot-starter-tomcat` 和 `spring-boot-starter-tomcat-runtime`。

错误复现：

- 在 Boot 4 临时副本中执行 `cmd /c mvn -f fist-cloud/fist-iam/pom.xml "-P!config-nacos" -DskipTests validate`。
- Maven 在项目读取阶段失败，错误为 `dependencies.dependency.version` for `org.springframework.boot:spring-boot-starter-undertow` is missing。

临时验证方式：

- 临时副本曾将 3 处旧 `spring-boot-starter-undertow` 替换为 `spring-boot-starter-jetty`，用于验证 Boot 4 受管理 Servlet 容器 starter 可以消除 POM 读取阶段阻塞：
  - `fist-cloud/fist-iam`
  - `fist-cloud/fist-auth-v3`
  - `pac-cloud/demo-order`
- 真实内部项目落地时按项目原策略修正为 Tomcat：`pkg-all` 表示随包带默认 Web 容器；关闭 `pkg-all` 表示不打包 Web 容器，保留从外部路径加载容器 jar 的部署模式。

验证结果：

- `fist-cloud/fist-iam` 单模块 `validate` 为 `BUILD SUCCESS`。
- `fist-cloud/fist-auth-v3` 单模块 `validate` 为 `BUILD SUCCESS`。
- `pac-cloud/demo-order` 单模块 `validate` 为 `BUILD SUCCESS`。
- `fist-auth-v3` 和 `demo-order` 仍提示部分内部 `4.0.0-SNAPSHOT` POM 未安装，这是后续本地安装或发布问题；本轮验证只确认 Undertow 缺版本阻塞已经消除。

建议：

- 内部应用正式升级到 Boot 4 时，应删除 `spring-boot-starter-undertow` 路线。没有外部容器 jar 加载需求时使用默认 Tomcat；需要不打包容器时关闭 `pkg-all`；确有非 Tomcat Servlet 容器需求时才改为 `spring-boot-starter-jetty`。
- 不建议通过手工指定旧 Undertow starter 版本绕过错误。Boot 4 依赖管理已不覆盖该 starter，继续保留会增加后续 Servlet 6.1 / Spring Framework 7 兼容风险。
- 全量 reactor 试编译前，应先统一处理这 3 处 Web server starter，否则 Maven 会在项目读取阶段提前失败。

## 内部应用 fist-auth / fist-iam Boot 4 主代码试编译

记录日期：2026-05-18；记录者：Codex。

在 `fist-monitor` 启动探测和 Undertow 迁移验证之后，继续扩大内部应用样本到 `fist-auth-v3` 与 `fist-iam`。本轮仍只修改临时副本，不修改真实内部项目；FIST Kit `4.0.0-SNAPSHOT` 以前置本地 install 为准，不再尝试从远程仓库解析未发布组件。

临时版本矩阵：

- Spring Boot `4.0.6`
- Spring Cloud `2025.1.1`
- Spring Cloud Alibaba `2025.1.0.0`
- FIST Kit `4.0.0-SNAPSHOT`
- Spring Authorization Server `7.0.5`
- SpringDoc `3.0.3`
- Spring Boot Admin `4.0.4`
- MyBatis Plus Boot 4 starter

临时适配动作：

- 将内部应用依赖管理中的 `mybatis-plus-spring-boot3-starter` 替换为 `mybatis-plus-spring-boot4-starter`。
- 将 `fist-auth-v3`、`fist-iam` 的 `spring-boot-starter-undertow` 移出默认路线；真实项目落地时 `pkg-all` 使用 `spring-boot-starter-tomcat`，关闭 `pkg-all` 支持不打包 Web 容器。
- 为 `fist-auth-v3` 显式补充 `spring-security-config`，满足 Spring Security 7 下安全配置类编译。
- 将 `OAuth2AuthorizationServerConfigurer` 从旧 Spring Authorization Server 包迁移到 Spring Security 7 包：`org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization`。
- 将 Boot 3 的 `Jackson2ObjectMapperBuilderCustomizer` 迁移到 Boot 4 的 `JsonMapperBuilderCustomizer`，并使用 Jackson 3 的 `tools.jackson.databind` 类型。
- 将 `FormIdentityLoginConfigurer#init(HttpSecurity)` 调整为 Spring Security 7 签名，不再声明 `throws Exception`。
- 将内部 `JdbcAuthorizationService` 调整为 FIST Kit v4 `DefaultAuthorizationService` 的新构造器，移除旧 `LobHandler` 构造参数路径。
- 移除 `fist-iam-data` 测试范围内的 Nacos Config starter；该数据模块编译不需要配置中心依赖，且注释原意也是排除配置中心。

验证结果：

- `fist-auth-v3` 主代码在 Boot 4 / FIST Kit v4 临时矩阵下完成 `compile`。
- `fist-iam` 主代码在补齐本地 Maven 仓库中的 `spring-boot-devtools:4.0.6` 后完成 `compile`。
- 最终命令 `mvn ... -pl fist-cloud/fist-auth-v3,fist-cloud/fist-iam -am "-P!config-nacos" -DskipTests compile` 为 `BUILD SUCCESS`。

仍未通过的范围：

- `test-compile` 仍失败，失败点已从主代码转移到测试源码。
- 测试源码中仍引用 Boot 3 自动配置包，例如 `org.springframework.boot.autoconfigure.jdbc`、`org.springframework.boot.autoconfigure.sql.init`、`org.springframework.boot.autoconfigure.jackson`；Boot 4 需要迁移到新的自动配置包。
- `fist-auth-v3` 编译仍有 Jackson 2 相关废弃警告，例如 `MappingJackson2HttpMessageConverter` 和 Spring Security Jackson 2 模块；这些警告不阻塞主代码编译，但属于后续运行时兼容性风险。

边界：

- 本轮只证明 `fist-auth-v3` 与 `fist-iam` 主代码可以在临时版本矩阵下编译，不代表测试、应用启动、OAuth2 授权流程、数据库、Redis、Nacos 或真实业务接口已经通过。
- 内部 OAuth2 授权数据持久化的 Jackson 2 自定义 RowMapper 覆盖被临时移除以适配 FIST Kit v4 API；正式迁移不能只按编译结果处理，需要单独验证授权数据 JSON 读写、`AuthUser` 反序列化和历史 token 数据兼容性。
