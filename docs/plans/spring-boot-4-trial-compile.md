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
| `spring-cloud-alibaba.version` | `2023.0.3.4` | `2025.1.0.0` |
| `redisson.version` | `3.52.0` | `4.3.1` |
| `springdoc-openapi.version` | `2.8.17` | `3.0.3` |
| `spring-boot-admin.version` | `3.5.8` | `4.0.4` |
| `spring.oauth2-authorization-server.version` | `1.5.7` | `7.0.5` |

暂不修改：

- `mybatis-plus.version`：保持 `3.5.16`。
- `spock-lib.version`：保持 `2.4-groovy-5.0`，后续按去 Groovy 任务删除。
- `spring-security-jwt.version`：保持 `1.1.1.RELEASE`，后续按退役任务删除。

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
- Sentinel Gateway adapter 是否能与 Gateway 5.0.x 组合编译。
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

下一步：

1. 继续对 OAuth2 Server、Cloud OpenFeign、MyBatis CRUD 等已通过 `compile` 的核心链路执行 `test-compile`。
2. 继续检查 `MappingJackson2HttpMessageConverter`、`SecurityJackson2Modules`、`OAuth2AuthorizationServerJackson2Module` 等待删除警告。
3. 使用 `-Xlint:deprecation` 对 Gateway、Security、OAuth2 的 deprecation 警告做分类。
4. 每解决一个 P0 编译阻塞点后，重新记录新的首个失败点。
5. 在主代码 `compile` 有基本通过面后，再进入更完整的单元测试和配置迁移诊断。

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

## 资料来源

- Spring Boot 4.0 Migration Guide：`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-webflux\4.0.6\spring-boot-webflux-4.0.6.jar`
- 本地 OpenRewrite dry-run patch：`target/rewrite/rewrite.patch`
