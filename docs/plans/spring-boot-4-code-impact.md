# Spring Boot 4 源码影响清单

日期：2026-05-10
执行者：Codex
阶段：阶段 4「基于编译错误的源码和测试命中清单」

## 结论摘要

阶段 4 第一版以阶段 2 的真实编译错误为主输入，再补充静态扫描。当前不修改 Java 源码。

当前第一阻塞点是 `fist-support-web` 的 Spring Boot WebFlux 错误处理 API 包迁移。该问题会阻塞 Security、OAuth2 Server、MyBatis CRUD、Cloud OpenFeign 和 Cloud Gateway 的继续编译，优先级高于 Jackson、Feign、Groovy 等专项迁移。

静态扫描显示：

- Jackson 2 包名命中 40 个 Java 文件，仍是最大源码迁移面。
- `org.springframework.lang.Nullable` 命中 89 个 Java 文件，需要和 JSpecify 策略统一处理。
- Feign 命中 26 个 Java 文件，SDE Feign 编解码仍需专项评估。
- Groovy 测试文件共 5 个，后续应迁移到 Java/JUnit 5。
- `@MockBean`、`@SpyBean` 和 `org.springframework.security.jwt` 源码直接引用未命中。
- `javax.*` 命中仅来自 JDK `javax.crypto`，不是 Jakarta EE 迁移问题。

## P0：WebFlux 错误处理 API 包迁移

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

### 处理建议

1. 单独迁移这 3 个文件的 import。
2. 运行 `.\mvnw.cmd -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests compile`。
3. 通过后继续阶段 2 中被阻塞的模块编译。

说明：OpenRewrite dry-run 未覆盖该迁移点，因此后续不能只依赖自动化补丁。

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

结果：89 个 Java 文件。

处理建议：

- 与项目已有 `@NullMarked` 和 `org.jspecify.annotations.Nullable` 策略统一处理。
- 公共 API 包优先补齐 `package-info.java`。
- 不做单纯机械替换，避免泛型可空语义丢失。

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

1. 先迁移 P0 WebFlux 错误处理包引用。
2. 重新运行 `fist-support-web` 编译。
3. 编译通过后继续 Security、OAuth2 Server、MyBatis CRUD、Cloud OpenFeign、Cloud Gateway。
4. 每次新增编译错误都追加到本清单，避免提前做全仓机械迁移。

## 本地证据

- 阶段 2 试编译日志：`.codex/trial-compile-*.log`
- Spring Boot `4.0.6` 本地 JAR：`F:\pkg-maven\repository\org\springframework\boot\spring-boot-webflux\4.0.6\spring-boot-webflux-4.0.6.jar`
- OpenRewrite dry-run patch：`target/rewrite/rewrite.patch`
