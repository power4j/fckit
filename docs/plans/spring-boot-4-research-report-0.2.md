# Spring Boot 4 升级调研报告

文档版本：`0.2`
日期：2026-05-10
执行者：Codex

## 版本说明

本版本用于响应 0.1 调研报告的评审意见，并修订后续调研顺序。0.1 保留为第一版基线，不回写覆盖。

## 评审结论

评审意见整体成立，需进入计划修订。6 个问题中，5 个直接采纳，1 个按事实核验后修正采纳。

| 编号 | 评审问题 | 处理结论 | 依据 |
| --- | --- | --- | --- |
| 1 | Spring Cloud Alibaba 风险低估 | 采纳，升级为阻塞性准入风险 | Maven Central 已有 `2025.1.0.0`，但当前仓库从 `2023.0.3.4` 跨到 Spring Cloud `2025.1.x`，必须先验证 Nacos、Sentinel、LoadBalancer、Gateway 的实际兼容性 |
| 2 | 缺少 `spring-security-jwt` 退役评估 | 采纳 | 根 `pom.xml` 与 `fist-kit-build/pom.xml` 仍管理 `spring-security-jwt:1.1.1.RELEASE`；源码未发现 `org.springframework.security.jwt` 直接引用，结论应偏向依赖管理清理与 `spring-security-oauth2-jose` 统一 |
| 3 | Jackson 3 分析粒度不足 | 采纳 | `fist-jackson` 存在自定义 `StdSerializer`、`StdDeserializer`、`NopAnnotationIntrospector`、`ContextualSerializer`、`ContextualDeserializer` 和 `JacksonAnnotationsInside` 使用点，不能只按包名替换处理 |
| 4 | Feign 迁移缺少 SDE 集成 | 采纳 | SDE Feign 编解码涉及 `SecureFeignEncoder`、`SecureFeignDecoder`、方法元数据、请求体重写和响应体重建；迁移到 Spring HTTP Service Clients 时需要单独设计等价适配 |
| 5 | 试编译顺序应提前 | 采纳 | 编译器错误比人工静态扫描更权威。计划已调整为先做准入核验，再做只改版本试编译，之后用编译错误驱动人工扫描和自动化工具复核 |
| 6 | 目标版本需要重新确认 | 修正采纳 | 需要在阶段 1 执行前重新核实最新 GA；截至本次核验，Spring Boot 当前 GA 仍为 `4.0.6`，`4.1.0-RC1` 是预览版本，不作为本轮目标 |

## 计划修订

后续顺序调整为：

1. 版本与生态准入核验：先确认 Spring Boot 最新 GA、Spring Cloud `2025.1.x`、Spring Cloud Alibaba `2025.1.0.0` 的可用性。
2. 只改版本试编译：先收集 Maven 解析错误和真实编译错误。
3. 自动化迁移工具评估：用 OpenRewrite、Arconia CLI 和 `spring-boot-properties-migrator` 交叉验证编译错误和配置参数迁移。
4. 源码和测试命中清单：以编译错误为主输入，再补充人工扫描。
5. Groovy/Spock 去除：迁移 5 个 `.groovy` 测试后，删除 Groovy、Spock 和 `gmavenplus-plugin` 测试编译链路。
6. Feign 与声明式 HTTP 客户端方案：同时评估 OpenFeign 5.x 延续方案和 Spring HTTP Service Clients 迁移方案，SDE 编解码作为单独子项。
7. 迁移任务拆分与用户侧升级手册：使用库侧、示例、基准测试和机密下游应用样本提炼通用步骤。

## 需要前置处理的阻塞项

### Spring Cloud Alibaba

结论不是「没有兼容版本」，而是「存在候选版本但必须准入验证」。阶段 1 应优先确认：

- `spring-cloud-alibaba-dependencies:2025.1.0.0` 是否可与 Spring Boot 当前 GA 和 Spring Cloud `2025.1.x` 同时解析。
- Nacos discovery/config 是否仍匹配当前自动配置和示例用法。
- Sentinel starter 是否兼容 Gateway、Web MVC/WebFlux 和当前依赖管理。
- LoadBalancer、OpenFeign 和 Gateway 的组合是否存在传递依赖冲突。

若该准入失败，后续迁移任务需要先评估移除或替换 Spring Cloud Alibaba 集成。

### spring-security-jwt

当前仅发现依赖管理声明，未发现源码直接引用。调研目标应调整为：

- 确认发布 BOM 是否仍暴露 `spring-security-jwt`。
- 确认下游是否可能依赖该 BOM 引入旧 JWT 库。
- 给出删除 `spring-security-jwt`、统一使用 `spring-security-oauth2-jose` 的迁移说明。

### Jackson 3

Jackson 迁移清单必须单列自定义扩展点：

- `ObfuscatedStringSerializer`
- `ObfuscatedStringDeserializer`
- `ObfuscatedAnnotationIntrospector`
- `Obfuscation`
- `JacksonConfig` 中的 `AnnotationIntrospectorPair`

这些类依赖 Jackson 2 的扩展 API，迁移时需要验证 Jackson 3 的包名、构造器、上下文化接口和注解内省行为。

### Feign 与 SDE

Feign 迁移不能只验证基础 `@FeignClient`。SDE 相关能力是功能契约的一部分：

- `SecureFeignEncoder` 包装委托编码器后重写请求体。
- `SecureFeignDecoder` 包装委托解码器后重建响应体。
- `SecureFeignSupport` 依赖 Feign 方法元数据和请求模板。
- 示例 Feign 客户端验证了 SDE envelope 的端到端行为。

如果迁移到 Spring HTTP Service Clients，需要明确这些能力对应到拦截器、消息转换器、请求属性或客户端装饰器中的哪一层。

### Groovy 与 gmavenplus

去 Groovy 不只是删除 `.groovy` 测试。阶段 5 必须覆盖：

- 根 `pom.xml` 中 `gmavenplus-plugin` 及其 Groovy 依赖。
- 根 `pom.xml` 中 `spock-lib.version`。
- `fist-kit-build/pom.xml` 中 Groovy 和 Spock BOM 管理。
- `fist-kit-infra/fist-support-test/pom.xml` 中 `spock-spring` 与 `groovy-tables`。
- 5 个 `.groovy` 测试迁移到 Java/JUnit 5 后的覆盖关系。

## 对 0.1 的修正

- P0 风险排序中新增 Spring Cloud Alibaba 准入项，并放在 Groovy 前。
- Jackson 3 从「包名迁移」升级为「自定义扩展点重写风险」。
- Feign 路线选择增加 SDE 编解码适配设计。
- `spring-security-jwt` 从遗漏项加入 Security 7 调研范围。
- 阶段顺序从「先扫描、后试编译」改为「先试编译、后补充扫描」。
- 目标版本保留 `4.0.6`，但阶段 1 必须再次核实最新 GA。

## 保密边界

机密下游应用样本只用于验证使用方升级影响，并提炼用户侧升级手册。公开文档只记录泛化结论，不记录样本名称、路径、业务结构、依赖清单、配置内容、代码片段或错误日志原文。

## 资料来源

- Spring Boot 官方文档索引：`https://docs.spring.io/spring-boot/index.html`
- Spring Boot Maven Central：`https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/`
- Spring Boot 4.0 Migration Guide：`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`
- Spring Cloud Alibaba Maven Central：`https://repo.maven.apache.org/maven2/com/alibaba/cloud/spring-cloud-alibaba-dependencies/`
- Spring Security 7 migration：`https://docs.spring.io/spring-security/reference/migration-7/index.html`
- Spring Cloud OpenFeign：`https://spring.io/projects/spring-cloud-openfeign`
- Spring Framework HTTP Service Clients：`https://docs.spring.io/spring-framework/reference/integration/rest-clients.html`
- OpenRewrite Spring：`https://github.com/openrewrite/rewrite-spring`
- Arconia Migrations：`https://github.com/arconia-io/arconia-migrations`
