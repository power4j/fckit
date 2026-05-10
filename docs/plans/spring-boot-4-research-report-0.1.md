# Spring Boot 4 升级调研报告

文档版本：`0.1`
日期：2026-05-10
执行者：Codex

## 结论摘要

当前具备进入试编译调研的条件，但不建议直接开始源码迁移。第一阶段应先完成依赖版本矩阵和自动化工具 dry-run，再按错误类型拆分迁移任务。

初步判断：

- Spring Boot 目标版本应使用当前最新 GA：`4.0.6`。Maven Central 已出现 `4.1.0-RC1`，但不是 GA。
- Spring Cloud 已有 `2025.1.1`，Spring Cloud Alibaba 已有 `2025.1.0.0`，Cloud 兼容性风险从「未知」降为「需要试编译验证」。
- Jackson 3、Groovy/Spock 去除、Feign 路线选择、Spring Security 7、starter 模块化是本仓库的主要风险。
- OpenRewrite、Arconia CLI、`spring-boot-properties-migrator` 应作为调研工具链，但必须先 dry-run 和人工复核。
- Spring Boot 4.1 的虚拟线程与 HikariCP/LazyConnection 风险应进入后续 Java 21 调研，不进入本轮 GA 升级目标。
- 后续用户侧升级手册需要引入一个机密下游应用样本。公开报告只保留泛化结论，不记录样本名称、路径、业务结构、依赖清单、配置内容、代码片段或错误日志原文。

## 保密边界

机密下游应用样本仅用于验证 FIST Kit v3 升级后对真实使用方的影响，并辅助编写用户侧升级手册。

允许记录：

- 通用升级步骤。
- 抽象后的问题类型。
- 不包含项目识别信息的依赖类别。
- 不包含业务字段和路径的配置迁移规则。
- 不包含代码片段和日志原文的验证结论。

禁止记录：

- 样本项目名称和本地路径。
- 包名、模块名、服务名、业务域名、数据库名、配置键值和环境标识。
- 代码片段、完整错误日志、接口路径、账号信息、内网地址和业务数据。
- 能够反推出参考项目身份的依赖组合、目录结构或截图。

## 当前环境

| 项目 | 当前值 |
| --- | --- |
| 分支 | `cj-spring-boot-4-research` |
| Maven | `3.9.7` |
| Java | `17.0.7`，GraalVM Community |
| OS | Windows 11 |
| 根版本 | `3.14.0-SNAPSHOT` |

## 当前依赖基线

| 依赖 | 当前版本 | 候选版本 / 目标线 | 初步判断 |
| --- | --- | --- | --- |
| Spring Boot | `3.5.14` | `4.0.6` GA | 可进入试编译 |
| Spring Cloud | `2025.0.2` | `2025.1.1` | 需要同步升级 |
| Spring Cloud Alibaba | `2023.0.3.4` | `2025.1.0.0` | 需要重点验证 Sentinel、LoadBalancer |
| Spring Authorization Server | `1.5.7` | `7.0.5` GA | 高风险，涉及 OAuth2 扩展 |
| SpringDoc OpenAPI | `2.8.17` | `3.0.3` | 需要验证 Swagger/SpringDoc 扩展 |
| Spring Boot Admin | `3.5.8` | `4.0.4` | 需要验证 BOM 与 starter |
| Redisson | `3.52.0` | `4.3.1` | 需要验证 starter 与 Boot 4 |
| MyBatis Plus | `3.5.16` | `3.5.16` | 暂无新 BOM，需试编译 |
| OpenRewrite Spring | 未接入 | `6.30.4` | 用于 dry-run |
| rewrite-maven-plugin | 未接入 | `6.39.0` | 用于 dry-run |

## 仓库影响面

### Jackson 3

扫描结果显示，`com.fasterxml.jackson` 出现在 40 个 Java 文件中。重点模块：

- `fist-kit-infra/fist-jackson`
- `fist-kit-infra/fist-sde`
- `fist-kit-app/fist-security`
- `fist-kit-app/fist-data`
- `examples/fist-sde`

Spring Boot 4 首选 Jackson 3，包名迁移到 `tools.jackson`。本仓库有自有 Jackson 扩展和 SDE 编解码逻辑，不能只依赖 OpenRewrite 自动替换，需要专项测试覆盖序列化、反序列化、脱敏和安全信封编解码。

### Groovy/Spock

当前 `.groovy` 测试文件共 5 个：

- `fist-kit-app/fist-data/fist-support-data/src/test/java/com/power4j/fist/data/tree/NodeIdxTreeMakerTest.groovy`
- `fist-kit-app/fist-data/fist-support-data/src/test/java/com/power4j/fist/data/tree/AdjacencyTreeMakerTest.groovy`
- `fist-kit-app/fist-web/fist-support-web/src/test/java/com/power4j/fist/boot/common/matcher/PathMatcherTest.groovy`
- `fist-kit-app/fist-data/fist-support-data/src/test/java/com/power4j/fist/boot/common/op/OpTemplateTest.groovy`
- `fist-kit-app/fist-security/fist-support-security/src/test/java/com/power4j/fist/boot/security/inner/DefaultUserCodecTest.groovy`

同时存在 `spock-bom`、`spock-spring`、`groovy`、`groovy-tables` 和 `gmavenplus-plugin`。由于后续计划不再使用 Groovy，建议先把这 5 个测试迁移为 Java/JUnit 5，再移除 Groovy 测试编译链路。

### Feign 与 HTTP 客户端

Feign 相关命中 26 个 Java 文件，影响不只在 `fist-kit-cloud/fist-cloud-rpc-feign`，还包括：

- `fist-kit-infra/fist-sde/fist-sde-client`
- `fist-kit-infra/fist-sde/fist-sde-boot-starter`
- `examples/fist-sde/example-feign`

Spring Cloud OpenFeign 仍是 Spring Cloud 官方项目，但官方文档显示项目已 feature-complete，并建议新项目使用 Spring HTTP Service Clients。第一版结论是：短期先验证 OpenFeign 5.x 能否保持兼容，长期再评估是否迁移到 Spring HTTP Interface Clients。

### Spring Boot starter 模块化

当前使用的高风险 starter：

- `spring-boot-starter-web`
- `spring-boot-starter-webflux`
- `spring-boot-starter-test`
- `spring-boot-starter-security`
- `spring-boot-starter-json`
- `spring-boot-starter-validation`
- `spring-boot-starter-oauth2-*` 间接依赖

OpenRewrite 文档中已覆盖部分 Spring Boot 4 starter artifactId 变更，例如 `spring-boot-starter-web` 到 `spring-boot-starter-webmvc`。本仓库需要 dry-run 后逐项确认 Maven 坐标，而不是手工猜测替换。

### 配置参数

当前配置文件集中在：

- `examples/fist-sde/example-feign/src/main/resources/application.yml`
- `examples/fist-sde/example-restclient/src/main/resources/application.yml`
- `examples/fist-sde/example-webclient/src/main/resources/application.yml`
- `examples/fist-sde/example-server/src/main/resources/application.yml`
- `fist-kit-app/fist-data/fist-boot-crud-mybatis/src/test/resources/application.yml`

本轮扫描未发现 `spring.jackson`、`spring.main.virtual`、`hikari`、`LazyConnection` 等配置命中。后续仍需使用 `spring-boot-properties-migrator` 在示例启动时捕获运行时配置迁移提示。

### 测试注解

当前未扫描到 `@MockBean` 或 `@SpyBean`。这降低了 Spring Boot 4 测试注解迁移风险，但仍需验证 `spring-boot-starter-test` 模块拆分和 AssertJ/JUnit 版本升级。

## 自动化工具策略

### OpenRewrite

用途：

- Spring Boot 4 recipe dry-run。
- starter artifactId 迁移预览。
- 配置属性 key 迁移预览。
- Java / JUnit 迁移预览。

约束：

- 只先运行 `rewrite:dryRun` 或等价预览命令。
- 不直接接受全量修改。
- dry-run 输出必须按模块归档，人工标记可接受项。

### Arconia CLI

用途：

- 作为 OpenRewrite 的快速入口做交叉验证。
- 验证它对 Maven 多模块项目的识别能力。

约束：

- 只作为辅助工具，不替代 Maven 原生验证。
- 如输出与 OpenRewrite Maven plugin 不一致，以可解释、可复现的 Maven plugin 输出为准。

### spring-boot-properties-migrator

用途：

- 运行示例应用和测试上下文时输出配置属性迁移建议。
- 临时迁移已废弃配置，辅助定位运行时问题。

约束：

- 仅用于迁移期。
- 升级完成后必须移除。

## 风险排序

| 优先级 | 风险 | 依据 | 建议动作 |
| --- | --- | --- | --- |
| P0 | Jackson 3 | 40 个 Java 文件直接引用 Jackson 2 包名，且有自有 Jackson 模块 | 先评估是否短期保留 Jackson 2 兼容模块 |
| P0 | Groovy/Spock | Spring Boot 4 移除 Spock 集成，项目计划去 Groovy | 先迁移 5 个 Groovy 测试 |
| P0 | Spring Authorization Server 7 | 当前从 `1.5.7` 跨到 `7.0.x` | 独立试编译 `fist-app-security` |
| P1 | OpenFeign 路线 | OpenFeign 官方项目 feature-complete，项目已有深度扩展 | 短期验证 OpenFeign 5.x，长期评估 HTTP Interface Clients |
| P1 | Spring Cloud / Alibaba | 已有候选版本，但 Gateway、Sentinel、LoadBalancer 需验证 | 先升级 BOM 后编译 Cloud 模块 |
| P1 | starter 模块化 | Boot 4 改名和拆分 starter | 用 OpenRewrite dry-run 生成坐标变更清单 |
| P2 | 配置属性 | 当前静态扫描风险较低，但运行时属性仍可能迁移 | 使用 properties migrator 跑示例 |
| P2 | Spring Boot 4.1 | 当前为 RC，涉及虚拟线程和连接池优化 | 后续 Java 21 规划单独跟踪 |

## 建议下一步

1. 新建 `docs/plans/spring-boot-4-automation-tools.md`，记录 OpenRewrite 与 Arconia dry-run 命令和输出摘要。
2. 新建试编译分支或在当前调研分支上创建可回退提交，只修改版本属性和必要 BOM。
3. 先运行 `.\mvnw.cmd -pl fist-kit-infra -DskipTests compile`，收集 Jackson、Groovy、starter 坐标的第一批编译错误。
4. 再运行 `.\mvnw.cmd -pl fist-kit-cloud -DskipTests compile`，确认 OpenFeign、Gateway、LoadBalancer、Sentinel 的真实错误。
5. 在库侧试编译完成后，使用机密下游应用样本提炼用户侧升级手册；手册只写通用步骤和泛化结论。
6. 不建议先改源码。应先让工具和编译器给出真实错误边界。

## 资料来源

- Spring Boot 官方文档索引：`https://docs.spring.io/spring-boot/index.html`
- Spring Boot Maven Central：`https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/`
- Spring Boot 4.0 Migration Guide：`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`
- Spring Cloud 项目页：`https://spring.io/projects/spring-cloud`
- Spring Cloud OpenFeign：`https://spring.io/projects/spring-cloud-openfeign`
- Spring Framework HTTP Service Clients：`https://docs.spring.io/spring-framework/reference/integration/rest-clients.html`
- OpenRewrite Spring：`https://github.com/openrewrite/rewrite-spring`
- Arconia Migrations：`https://github.com/arconia-io/arconia-migrations`
- 用户补充资料：`https://mp.weixin.qq.com/s/o2EFr5PlTv4G56OohmjUgw`
- 用户补充资料：`https://cloud.tencent.com/developer/article/2666029?policyId=1004`
