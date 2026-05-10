# Spring Boot 4 升级调研计划

日期：2026-05-10
执行者：Codex
状态：已根据评审意见更新，等待下一步调研执行

## 目标

调研 FIST Kit v3 从 Spring Boot 3.5.x 升级到当前最新 Spring Boot 4.x 的可行性、影响范围和实施顺序。调研阶段不直接完成升级实现，先形成可评审的升级依据、风险清单和迁移任务拆分。

当前评审结论：

- 调研范围覆盖发布组件、`examples` 与 `benchmarks`。
- 目标版本跟随当前最新 Spring Boot 4 GA。2026-05-10 Spring Boot 官方文档索引和 Maven Central 显示当前 GA 为 `4.0.6`。
- 允许后续创建独立调研分支进行「只改版本」试编译。
- 后续计划不再使用 Groovy。当前 Groovy/Spock 仅用于测试代码，迁移到 Java/JUnit 5/Mockito 需要作为升级前置任务。
- 自动化升级工具进入调研范围。优先评估 OpenRewrite、Arconia CLI 和 `spring-boot-properties-migrator`，减少手工迁移和配置参数遗漏。
- Spring Cloud OpenFeign 与 Spring HTTP Service Clients 需要并行评估。OpenFeign 仍是 Spring Cloud 官方项目，但官方文档已标记 feature-complete，并建议新项目使用 Spring HTTP Service Clients。
- 引入一个机密下游应用作为使用侧验证样本，用于编写用户侧升级手册。公开文档和提交信息不得记录该样本的名称、路径、业务结构、依赖清单、配置内容、代码片段或错误日志原文。

## 已确认基线

仓库当前基线来自根 `pom.xml` 与 `fist-kit-build/pom.xml`：

| 项目 | 当前值 | 调研关注点 |
| --- | --- | --- |
| 根版本 | `3.14.0-SNAPSHOT` | Spring Boot 4 可能需要主版本策略评估 |
| JDK | `17` | 满足 Spring Boot 4 的 Java 17+ 要求；可评估是否使用 Java 21 LTS 验证 |
| Spring Boot | `3.5.14` | 已处于最新 3.5.x 基线，符合官方迁移前置建议 |
| Spring Cloud | `2025.0.2` | 官方兼容表显示 Spring Boot 4.0.x 对应 Spring Cloud `2025.1.x` |
| Spring Cloud Alibaba | `2023.0.3.4` | 需确认是否支持 Spring Boot 4 / Spring Cloud 2025.1 |
| Spock/Groovy | `2.4-groovy-5.0` | Spring Boot 4 移除 Boot 的 Spock 集成，且项目计划去 Groovy |
| Jackson | Boot 3 管理的 Jackson 2 + 项目显式引用 | Spring Boot 4 首选 Jackson 3，包名从 `com.fasterxml.jackson` 迁移到 `tools.jackson` |
| Feign | Spring Cloud OpenFeign | 评估继续使用 OpenFeign 5.x，或迁移到 Spring HTTP Service Clients |
| 配置参数 | 多模块 `application.*`、测试属性、示例属性 | 使用 OpenRewrite 和 `spring-boot-properties-migrator` 双重核对 |

## 官方资料入口

- Spring Boot 项目页：`https://spring.io/projects/spring-boot`
- Spring Boot 官方文档索引：`https://docs.spring.io/spring-boot/index.html`
- Spring Boot Maven Central：`https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/`
- Spring Boot 4.0 Migration Guide：`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`
- Spring Boot release notes：`https://github.com/spring-projects/spring-boot/wiki`
- Spring Cloud 项目页与兼容表：`https://spring.io/projects/spring-cloud`
- Spring Framework 7 release notes：`https://github.com/spring-projects/spring-framework/wiki`
- Spring Security 7 migration：`https://docs.spring.io/spring-security/reference/migration-7/index.html`
- OpenRewrite Spring recipes：`https://github.com/openrewrite/rewrite-spring`
- Arconia Migrations：`https://github.com/arconia-io/arconia-migrations`
- Spring Cloud OpenFeign：`https://spring.io/projects/spring-cloud-openfeign`
- Spring Framework HTTP Interface Clients：`https://docs.spring.io/spring-framework/reference/integration/rest-clients.html`
- 用户补充资料：`https://mp.weixin.qq.com/s/o2EFr5PlTv4G56OohmjUgw`
- 用户补充资料：`https://cloud.tencent.com/developer/article/2666029?policyId=1004`

说明：Context7 当前可解析的 Spring Boot 文档版本最高为 `/spring-projects/spring-boot/v4.0.3`，低于官方文档索引和 Maven Central 显示的 `4.0.6`。正式调研以官方 Spring 文档、Maven Central、Spring Blog 和 GitHub release notes 为准，Context7 只作为辅助索引。

腾讯云文章提到 Spring Boot 4.1 的虚拟线程、HikariCP 与 `LazyConnectionDataSourceProxy` 风险。官方 Spring Blog 当前发布的是 `4.1.0-RC1`，不是 GA；因此 4.1 内容作为前瞻风险跟踪，不作为本轮目标版本。

## 调研范围

### 发布组件

- `fist-kit-build`：BOM、依赖管理、测试依赖、插件版本和 Maven 构建约定。
- `fist-kit-dependencies`：发布 BOM 中暴露的模块依赖。
- `fist-kit-infra`：Jackson、Logback、Spring 支撑工具、MyBatis Plus、Redisson、SDE 相关 starter。
- `fist-kit-app`：Web MVC/WebFlux、数据、API 文档、安全、OAuth2 Authorization Server。
- `fist-kit-cloud`：Spring Cloud Gateway、OpenFeign、LoadBalancer、Sentinel、Reactive Security。

### 非发布验证组件

- `examples`：验证 starter、自动配置和客户端示例是否还能启动。
- `benchmarks`：验证依赖管理和 JMH/日志基准测试是否仍可编译。
- 机密下游应用样本：仅用于验证库升级后对使用方的影响，并提炼通用升级步骤；所有可提交文档只记录泛化结论。

## 重点调研问题

1. Spring Boot 4 模块化后，现有 starter 依赖是否需要替换为新的 `spring-boot-starter-webmvc`、`spring-boot-starter-restclient`、`spring-boot-starter-webclient`、`spring-boot-starter-*-test`。
2. Spring Cloud 需要从 `2025.0.x` 升级到 `2025.1.x`，Spring Cloud Alibaba 是否存在对应兼容版本。
3. Jackson 3 是否要求迁移 `fist-jackson` 和所有 `com.fasterxml.jackson.databind` 直接引用；哪些外部库仍依赖 Jackson 2，是否短期需要 `spring-boot-jackson2` 作为过渡。
4. Groovy/Spock 测试如何迁移到 Java/JUnit 5/Mockito；是否需要先删除 Spock BOM、Groovy 依赖、`gmavenplus-plugin` 中测试编译配置。
5. Spring Security 7、Spring Authorization Server、OAuth2 相关扩展类是否存在 API 或配置 DSL 变化。
6. Spring Boot 测试支持变化是否影响 `@SpringBootTest`、MockMvc、WebTestClient、`@MockBean`、`@SpyBean`、Mockito listener。
7. 自动配置注册、`spring.factories`、`AutoConfiguration.imports`、`EnvironmentPostProcessor` 和 Boot 内部包迁移是否影响 starter 加载。
8. Jakarta EE 11 / Servlet 6.1 是否影响 Servlet 工具、过滤器、网关、Web MVC 参数解析器。
9. `org.springframework.lang.Nullable` 是否需要继续迁移到 JSpecify，尤其是公共 API 和 Actuator 端点相关参数。
10. MyBatis Plus、Redisson、SpringDoc、Swagger、Spring Boot Admin、Excel starter、Sentinel 是否已提供 Spring Boot 4 兼容版本。
11. OpenRewrite 和 Arconia CLI 可自动迁移哪些内容，哪些内容必须人工审查。
12. `spring-boot-properties-migrator` 能诊断哪些配置参数变更，是否覆盖示例和测试属性。
13. `fist-cloud-rpc-feign` 是继续升级 Spring Cloud OpenFeign，还是迁移到 Spring HTTP Service Clients。
14. Spring Boot 4.1 的虚拟线程与连接池优化是否影响后续 Java 21 规划。
15. 使用方项目升级时需要哪些前置检查、依赖替换、配置迁移、测试验证和回退策略。
16. 如何在不暴露机密下游应用信息的前提下，将真实使用方问题提炼为通用升级手册。

## 工作分解

### 阶段 1：依赖矩阵

产出：`docs/plans/spring-boot-4-dependency-matrix.md`

- 列出所有显式版本属性。
- 标记 Boot 管理依赖、Cloud 管理依赖、项目显式管理依赖。
- 核对 Maven Central / 官方文档中的 Spring Boot 4 兼容版本。
- 输出每个依赖的结论：可升级、需替换、暂不兼容、待验证。

### 阶段 2：自动化迁移工具评估

产出：`docs/plans/spring-boot-4-automation-tools.md`

- 评估 OpenRewrite Spring recipes 是否存在 Spring Boot 4 迁移 recipe。
- 评估 Arconia CLI 对 Maven 多模块项目的适配方式和可重复执行能力。
- 使用 `rewrite:dryRun` 或等价 dry-run 方式生成补丁预览，不直接接受全部修改。
- 对比工具输出与手工扫描结果，标记「可自动处理」「需人工复核」「不适用」。
- 对配置文件启用 `spring-boot-properties-migrator` 诊断策略，记录运行时输出的属性迁移建议。

### 阶段 3：源码和测试命中清单

产出：`docs/plans/spring-boot-4-code-impact.md`

- 扫描 Jackson 2 包名和 Boot Jackson 扩展点。
- 扫描 Groovy/Spock 测试文件和 Groovy 相关构建配置。
- 扫描 Spring Boot 测试注解、`@MockBean`、`@SpyBean`、MockMvc、WebTestClient。
- 扫描 `spring.factories`、`AutoConfiguration.imports`、`EnvironmentPostProcessor`。
- 扫描 `org.springframework.lang.Nullable`、`javax.*`、Servlet API、Security DSL。
- 扫描 Feign client、Feign encoder/decoder、Spring Cloud LoadBalancer、HTTP interface client 可迁移点。

### 阶段 4：只改版本试编译

产出：`docs/plans/spring-boot-4-trial-compile.md`

- 在独立调研分支更新版本属性到当前最新 Spring Boot 4 GA。
- 同步 Spring Cloud 到 `2025.1.x` 最新服务版本。
- 暂不做业务代码迁移，只处理 Maven 坐标和明显缺失依赖。
- 运行模块级编译，收集真实错误。
- 按模块归类错误：构建失败、依赖解析失败、源码编译失败、测试编译失败、测试运行失败。

建议命令顺序：

```powershell
.\mvnw.cmd -pl fist-kit-infra -DskipTests compile
.\mvnw.cmd -pl fist-kit-app -DskipTests compile
.\mvnw.cmd -pl fist-kit-cloud -DskipTests compile
.\mvnw.cmd -pl examples -DskipTests compile
.\mvnw.cmd -pl benchmarks -DskipTests compile
```

### 阶段 5：Groovy/Spock 去除方案

产出：`docs/plans/groovy-test-migration-for-spring-boot-4.md`

- 列出 `.groovy` 测试文件、`spock-spring`、`spock-bom`、`groovy`、`groovy-tables`、`gmavenplus-plugin` 的使用点。
- 将 Spock 规格迁移为 Java JUnit 5 测试。
- 用 AssertJ/JUnit 5 参数化测试替代表格测试。
- 删除 Groovy 测试编译配置前，先保证迁移后的测试覆盖原断言。
- 去除 Groovy 相关依赖后运行相关模块测试。

### 阶段 6：Feign 与声明式 HTTP 客户端方案

产出：`docs/plans/spring-boot-4-http-client-migration.md`

- 盘点 `fist-cloud-rpc-feign`、示例 Feign client、SDE Feign encoder/decoder。
- 评估继续使用 Spring Cloud OpenFeign 5.x 的成本：依赖兼容、LoadBalancer、拦截器、编码器、解码器、异常处理。
- 评估迁移到 Spring HTTP Service Clients 的成本：服务发现、负载均衡、拦截器、编解码、错误处理、SDE 能力移植。
- 给出推荐策略：短期兼容 OpenFeign 或直接迁移官方 HTTP Interface Clients。

### 阶段 7：迁移任务拆分

产出：`docs/plans/spring-boot-4-upgrade-roadmap.md`

按风险从低到高拆分：

1. 去 Groovy/Spock 测试迁移。
2. 自动化工具 dry-run 和配置参数迁移诊断。
3. Spring Cloud 2025.1 与 Gateway/OpenFeign 兼容迁移。
4. Spring Boot starter 和测试 starter 迁移。
5. Jackson 3 迁移或短期 Jackson 2 兼容策略。
6. Spring Security 7 与 Authorization Server 兼容迁移。
7. Feign 与 Spring HTTP Service Clients 路线决策。
8. 自动配置注册和 Boot 内部 API 迁移。
9. 示例和基准测试恢复。

### 阶段 8：用户侧升级手册

产出：`docs/plans/spring-boot-4-user-upgrade-guide.md`

- 基于发布组件、`examples`、`benchmarks` 和机密下游应用样本，提炼使用方升级步骤。
- 只记录通用操作：版本矩阵、依赖替换、配置参数迁移、常见编译错误类型、测试验证命令和回退策略。
- 不记录机密下游应用的名称、路径、业务结构、依赖清单、配置内容、代码片段或错误日志原文。
- 对必须引用的现象使用抽象描述，例如「某下游应用的网关模块」或「某下游应用的 OAuth 2.0 配置」；不得出现可识别项目的信息。

## 验证策略

调研阶段只要求记录命令和结果，不要求所有命令通过。正式迁移阶段必须按模块恢复本地验证。

基础验证：

```powershell
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd test
.\mvnw.cmd spring-javaformat:validate
```

模块验证：

```powershell
.\mvnw.cmd -pl fist-kit-infra test
.\mvnw.cmd -pl fist-kit-app test
.\mvnw.cmd -pl fist-kit-cloud test
.\mvnw.cmd -pl examples test
.\mvnw.cmd -pl benchmarks test
```

专项验证：

- Jackson：`fist-kit-infra/fist-jackson` 和所有 SDE JSON 编解码测试。
- Web：`fist-kit-app/fist-web`、`fist-kit-app/fist-data/fist-boot-data`。
- Cloud：Gateway、OpenFeign、LoadBalancer 相关模块。
- Security：`fist-kit-app/fist-security` 与 `fist-kit-cloud/fist-cloud-gateway`。
- Groovy 去除：所有原 `.groovy` 测试迁移后的 Java 测试。

## 风险分级

| 等级 | 风险 | 当前判断 |
| --- | --- | --- |
| 高 | Jackson 3 迁移 | 项目存在自有 `fist-jackson` 模块和大量 Jackson 2 直接引用 |
| 高 | Spring Cloud Alibaba 兼容性 | 当前版本线明显落后于 Spring Boot 4 兼容矩阵，需要确认是否有可用版本 |
| 高 | Groovy/Spock 去除 | Spring Boot 4 移除 Spock 集成，且项目计划不再使用 Groovy |
| 高 | Feign 路线选择 | OpenFeign 可继续升级，但官方建议新项目使用 Spring HTTP Service Clients |
| 中 | Spring Security 7 | OAuth2 和 Reactive Security 扩展较多，需要专项编译和测试验证 |
| 中 | Boot starter 模块化 | 依赖声明和测试 starter 需要大范围核对 |
| 中 | 配置参数迁移 | 需要结合 OpenRewrite 和 `spring-boot-properties-migrator`，避免只修编译不修运行参数 |
| 中 | 自动配置注册 | 当前同时存在 `spring.factories` 和 `AutoConfiguration.imports` |
| 中 | Spring Boot 4.1 前瞻变化 | 4.1 当前仍非 GA，但虚拟线程、HikariCP 和 LazyConnection 需要进入后续 Java 21 调研 |
| 低 | Java 基线 | 当前 JDK 17 满足 Spring Boot 4 最低要求 |

## 待评审确认后的下一步

本计划已纳入本轮评审意见。下一步建议进入「阶段 1：依赖矩阵」，先产出依赖兼容结论，再执行只改版本试编译。这样可以避免在外部依赖尚未确认时过早修改源码。
