# Spring Boot 4 自动化迁移工具评估

日期：2026-05-10
执行者：Codex
阶段：阶段 3「自动化迁移工具评估」

## 结论摘要

OpenRewrite 可作为 Spring Boot 4 迁移的主自动化工具，但只能用于生成补丁预览和减少机械修改，不应直接执行全量 `rewrite:run`。

本次 dry-run 已在当前仓库成功执行，生成 `target/rewrite/rewrite.patch`。补丁影响 180 个文件，OpenRewrite 估算节省 `25h 51m`，但其中包含 Jackson 3 包名迁移、JSpecify 注解迁移、starter 坐标替换、Springfox 删除、测试依赖调整和自动配置导入文件更新。这些变化会触及公共 API、测试链路和发布 BOM，必须拆分评审。

Arconia CLI 暂未执行。当前环境未安装 `arconia` 命令，后续可作为 OpenRewrite 的封装入口交叉验证，但不能替代 Maven plugin 的可复现输出。

`spring-boot-properties-migrator` 需要在只改版本试编译和示例启动阶段使用。它依赖应用启动时的环境分析，不适合在当前未迁移源码状态下单独运行。

## 当前前提

| 项目 | 当前值 |
| --- | --- |
| 分支 | `cj-spring-boot-4-research` |
| 根版本 | `4.0.0-SNAPSHOT` |
| Spring Boot 当前版本 | `3.5.14` |
| Spring Boot 目标 GA | `4.0.6` |
| OpenRewrite Spring | `6.30.4` |
| rewrite-maven-plugin | `6.39.0` |

说明：本次只执行 dry-run，没有修改源码或 POM。`target/rewrite/rewrite.patch` 和 `.codex/openrewrite-dryrun.log` 是本地调研产物，不进入正式提交。

## OpenRewrite 执行记录

### recipe 发现

PowerShell 中需要给所有 `-D...` 参数加引号，否则 Maven 可能把参数解析为插件坐标的一部分。

```powershell
.\mvnw.cmd -U org.openrewrite.maven:rewrite-maven-plugin:6.39.0:discover "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.30.4" "-Drewrite.recipe=UpgradeSpringBoot_4_0"
```

可用 recipe 名称：

```text
org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0
```

### dry-run 命令

```powershell
.\mvnw.cmd org.openrewrite.maven:rewrite-maven-plugin:6.39.0:dryRun "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.30.4" "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0" "-Drewrite.failOnDryRunResults=false" | Tee-Object -FilePath ".codex\openrewrite-dryrun.log"
```

执行结果：

| 项目 | 结果 |
| --- | --- |
| Maven 结果 | `BUILD SUCCESS` |
| 总耗时 | `09:16 min` |
| 生成补丁 | `target/rewrite/rewrite.patch` |
| 补丁大小 | `227042` bytes |
| 影响文件 | 180 个 |
| 估算节省 | `25h 51m` |
| 源码是否已修改 | 否 |

## dry-run 影响面

### 按文件类型

| 类型 | 文件数 | 判断 |
| --- | ---: | --- |
| `.java` | 150 | 主要是 Jackson 3、JSpecify、Spring API 和测试代码迁移 |
| `.xml` | 24 | 主要是版本、starter、依赖管理和测试依赖调整 |
| `.imports` | 6 | 自动配置导入文件调整，需要和 `spring.factories` 一起复核 |

### 按顶层模块

| 模块 | 文件数 | 判断 |
| --- | ---: | --- |
| `fist-kit-app` | 100 | Web、Security、Data、API Doc 是主要改动面 |
| `fist-kit-infra` | 44 | Jackson、SDE、Spring 支撑和测试依赖改动明显 |
| `fist-kit-cloud` | 34 | Gateway、OpenFeign、LoadBalancer 相关坐标和测试代码受影响 |
| 根 POM | 1 | 版本属性和依赖管理变化 |
| `fist-kit-build` | 1 | 公共依赖与测试链路变化 |

### 高频变化

| 变化 | 命中次数 | 初步处理 |
| --- | ---: | --- |
| `tools.jackson` | 105 | 不直接全量接受，先专项验证自定义 Jackson 扩展点 |
| `org.jspecify.annotations.Nullable` | 92 | 可作为 JSpecify 迁移候选，但需符合项目现有 `@NullMarked` 策略 |
| `springfox` | 16 | 倾向删除 Springfox，但 API Doc 模块需单独设计替代关系 |
| `mockwebserver3` | 5 | 可纳入测试依赖迁移，需确认 OkHttp 与 Feign 测试兼容 |
| `spring-boot-starter-webmvc` | 5 | 需要按模块确认 Web MVC 与 RestClient 依赖边界 |
| `spring-boot-starter-restclient` | 3 | SDE client 与 Feign 模块需确认是否需要显式 RestClient starter |
| `spring-cloud-starter-gateway-server-webflux` | 2 | Gateway 模块需和 Spring Cloud `2025.1.x` 一起试编译 |
| `spring-boot-starter-security-test` | 1 | 测试 starter 拆分候选，需验证 Security 测试覆盖 |
| `spring-cloud-gateway-server-webflux` | 1 | Gateway server 坐标迁移候选 |

## 可自动化处理的内容

以下内容可优先由 OpenRewrite 生成补丁，再人工审查后拆分提交：

- Spring Boot 版本属性更新到 `4.0.6`。
- 部分 Boot starter artifactId 迁移，例如 `spring-boot-starter-web` 到 `spring-boot-starter-webmvc`。
- 部分 Cloud Gateway artifactId 迁移，例如 `spring-cloud-starter-gateway` 到 `spring-cloud-starter-gateway-server-webflux`。
- Jackson 2 到 Jackson 3 的包名与部分方法名迁移。
- `org.springframework.lang.Nullable` 到 JSpecify 注解的机械替换。
- OkHttp MockWebServer 3 的测试依赖坐标迁移。
- 部分 `AutoConfiguration.imports` 内容更新。

这些修改可减少手工成本，但每一类都需要对应的编译和测试验证。

## 必须人工复核的内容

| 类别 | 原因 | 推荐动作 |
| --- | --- | --- |
| Jackson 3 自定义扩展点 | `fist-jackson`、SDE、Web JSON 模块存在自定义 serializer、deserializer、annotation introspector 和 `ObjectMapper` 定制 | 先迁移最小模块并运行 Jackson/SDE 专项测试 |
| Springfox 删除 | OpenRewrite 会删除旧 Springfox 依赖，但 `fist-boot-apidoc` 仍存在 Springfox 插件代码 | 先确认 API Doc 目标只保留 SpringDoc 3，还是做隔离删除 |
| JSpecify 迁移 | 项目已有 JSpecify 约定，不能只做注解包名替换 | 与公共 API 的 `package-info.java`、`@NullMarked` 策略一起审查 |
| 测试依赖 | 补丁中出现 `junit:junit` 相关追加，需要避免把 JUnit 4 重新引入常规测试路径 | 以 JUnit 5、Mockito、AssertJ 为目标重整测试依赖 |
| AutoConfiguration imports | Boot 自动配置注册影响 starter 加载 | 同时检查 `spring.factories` 与 `AutoConfiguration.imports`，避免加载行为变化 |
| Gateway / OpenFeign | starter 坐标能自动改，但 Sentinel、LoadBalancer、SDE Feign 编解码无法自动证明兼容 | 阶段 2 先编译 Cloud 模块，再决定是否接受工具补丁 |

## Arconia CLI 评估

Arconia CLI 适合作为 OpenRewrite 的辅助入口，用于快速触发 Spring Boot 迁移 recipe。当前环境未安装 `arconia` 命令，因此本阶段只记录调研结论，不执行实际命令。

后续执行策略：

1. 在独立调研分支或可回退提交后安装并执行。
2. 只运行 dry-run 或预览模式。
3. 将 Arconia 输出与 Maven `rewrite-maven-plugin:dryRun` 生成的补丁对比。
4. 若两者结果不一致，以 Maven plugin 输出为主，因为它更容易在当前多模块项目中复现。

Arconia 不应成为唯一迁移依据，原因是本仓库需要按 Maven 多模块、发布 BOM、starter 和公共 API 粒度拆分评审。

## spring-boot-properties-migrator 评估

Spring Boot 官方文档建议在升级功能版本时临时加入 `spring-boot-properties-migrator`。该工具会在应用启动时分析环境，输出废弃或迁移配置提示，并可临时迁移部分属性。迁移完成后必须移除该依赖。

推荐使用方式：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-properties-migrator</artifactId>
  <scope>runtime</scope>
</dependency>
```

本仓库建议只在示例和测试应用启动阶段加入：

- `examples/fist-sde/example-feign`
- `examples/fist-sde/example-restclient`
- `examples/fist-sde/example-webclient`
- `examples/fist-sde/example-server`
- 涉及 `application.yml` 的 MyBatis 测试模块

配置迁移输出只记录抽象后的配置类别，不记录真实内部项目样本的配置键值、环境名、服务名或日志原文。

## 推荐工作流

1. 保留当前 OpenRewrite dry-run 结果作为阶段 3 基线。
2. 进入阶段 2「只改版本试编译」，先验证候选版本组合能否解析和编译。
3. 根据真实编译错误反向筛选 OpenRewrite 补丁，避免先接受不必要的大范围改动。
4. 按类别拆分补丁：依赖坐标、starter、Jackson、JSpecify、Gateway、测试依赖、API Doc。
5. 对每个类别建立模块级验证命令，验证通过后再进入正式迁移提交。
6. 在示例应用可启动后加入 `spring-boot-properties-migrator`，记录配置迁移提示并移除该依赖。

## 当前不建议执行的操作

- 不执行 `mvn rewrite:run`。
- 不直接应用 `target/rewrite/rewrite.patch`。
- 不把 Jackson 3 包名迁移和 Springfox 删除合并到同一个提交。
- 不在未完成试编译前删除 Groovy、Spock 或 gmavenplus。
- 不把 `spring-boot-properties-migrator` 留在发布 BOM 或正式 starter 依赖中。

## 资料来源

- Spring Boot Upgrading：`https://docs.spring.io/spring-boot/4.0/upgrading.html`
- Spring Boot JSON：`https://docs.spring.io/spring-boot/4.0/reference/features/json.html`
- Spring Boot 4.0 Migration Guide：`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`
- OpenRewrite Spring Boot 4 recipe：`https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0`
- Arconia CLI Spring Boot migration：`https://docs.arconia.io/arconia-cli/latest/update/spring-boot/`
- Spring Framework REST Clients：`https://docs.spring.io/spring-framework/reference/integration/rest-clients.html`
- 本地 OpenRewrite dry-run 日志：`.codex/openrewrite-dryrun.log`
- 本地 OpenRewrite patch：`target/rewrite/rewrite.patch`
