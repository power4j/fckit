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

## 阶段 2 下一步

阶段 4 已处理 `fist-support-web` 的 WebFlux 错误处理包迁移，`fist-support-web` 主代码编译通过。继续试编译后，新的 P0 阻塞点为 `fist-support-security`。

下一步：

1. 迁移 `Sm3PasswordEncoder` 的 Spring Security 7 抽象方法。
2. 迁移 `DefaultReactiveOpaqueTokenIntrospector` 的 `WebClient.exchange()` 调用。
3. 修复后继续运行：
   - `fist-kit-app/fist-security/fist-support-security`
   - `fist-kit-app/fist-security/fist-boot-security-oauth2-server`
   - `fist-kit-app/fist-data/fist-boot-crud-mybatis`
   - `fist-kit-cloud/fist-cloud-rpc-feign`
   - `fist-kit-cloud/fist-cloud-gateway`
4. 每解决一个 P0 编译阻塞点后，重新记录新的首个失败点。
5. 在主代码 `compile` 有基本通过面后，再进入 test-compile、单元测试和配置迁移诊断。

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
