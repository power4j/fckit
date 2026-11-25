# Changelog

本文件记录 FIST Kit v3 的用户可见变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，并遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Added

- 恢复 `AbstractNodeIdxSupport.moveSubtree`，支持移动子树并维护闭包索引。
- 新增 `fist-jasypt-core` 和独立 Jasypt CLI，支持读取已有 `GMENC(...)` 配置密文。
- 新增 `fist-jasypt-spring-boot4-starter`，支持文件、显式原值及旧属性来源加载主密钥。

- 新增 `fist-trace-context-spring-boot4-starter`，支持 Servlet、WebFlux、Gateway 和 Feign 的请求追踪，并沿用 `fist.trace-context` 配置。

- 新增 `examples/fist-web-data-security/example-servlet`，提供 Web、Data、Security、API Doc starter 在 Spring Boot 4 Servlet 应用中的组合启动示例。
- 新增 `examples/fist-oauth2-server/example-authorization-server`，提供 OAuth2 Authorization Server 在 Spring Boot 4 中的 metadata 和 client credentials token 请求示例。
- 恢复 `examples/fist-trace-context` 的基础与扩展示例，使用 Spring Boot 4 Trace Context starter。

### Changed

- Web 自动配置不再注册旧的 Servlet 请求头 MDC filter。通过 `fist.trace-context.enabled=true` 启用追踪上下文。

- 将 Spring Boot 和 Spring Cloud 构建矩阵升级到 `4.2.0` 和 `2026.0.0`。
- OAuth2 Authorization Server 版本由 Spring Boot BOM 管理。API 文档升级到 Springdoc `3.2.1`，使用 Swagger Core 3 和 Jackson 3。
- Spring Boot 4 升级中为 SDE RestClient/WebClient 适配 Spring HTTP Service Clients，支持通过 HTTP service 接口方法上的 `@SecureExchange` 传递请求级策略上下文。
- Spring Boot 4 升级中适配 SDE boot starter 的 RestClient、WebClient、MockMvc 测试和 SDE 示例工程，Feign 示例迁移到 Spring Cloud OpenFeign 5 的 `FeignHttpMessageConverters`。
- Spring Boot 4 升级中将 `fist-boot-crud-mybatis` 的 MyBatis Plus starter 切换为 `mybatis-plus-spring-boot4-starter`，测试依赖切换为 `mybatis-plus-spring-boot4-starter-test`。
- 将 5 个 Groovy/Spock 测试迁移到 Java/JUnit 5。

### Fixed

- 恢复已删除的 `@InTenant`/`InTenantAspect` 租户注解入口和 `Breaker` 错误工具类，按 v3 原样保留 `@Deprecated`，恢复对应自动装配与国际化注入。
- 修复 `DataAutoConfiguration` 租户过滤器排序声明中失效的 Boot 4 安全自动配置包名。
- 修复 SDE 在 Spring Boot 4 中的 WebClient 自动配置，恢复新包 `WebClientCustomizer` 接入并移除 singleton Builder fallback，保留 Boot 的 prototype Builder、连接器、codec 和其他定制，避免重复挂载 SDE 过滤器。
- 收窄发布父 POM 的生命周期继承。应用继续继承跨代依赖检查，自行配置格式检查、POM 扁平化、源码归档和 SBOM 生成。
- 恢复 Servlet 静态资源缺失时的 HTTP 404 和 `A9900` 错误响应，保留请求方法和资源路径提示。
- 修复网关使用 `lb://` 内省地址时的请求协议转换，保留服务发现及原始 URI 路径和查询。
- 修复网关入站与出站追踪头名称不同时可能重复生成请求 ID 的问题。路由转发复用同一请求的追踪上下文。
- 恢复 Jasypt 对 `GMENC(${cipher.payload})` 的解密支持。占位符解析失败时，异常消息与原因链不包含配置值。

### Removed

- Spring Boot 4 升级中移除 `fist-boot-apidoc` 的 Springfox 支持，API 文档扩展统一保留 SpringDoc 3 路径。
- Spring Boot 4 升级中移除未实际使用的 Spring Cloud Alibaba BOM 和 `fist-cloud-rpc-feign` 的 Sentinel starter 依赖。
- 移除 Groovy、Spock、groovy-tables 和 gmavenplus 构建链路。

## [3.13.1] - 2026-05-09

### Changed

- 将 `tile-crypto` 升级到 `1.8.2`，并调整 `fist-support-security` 的 Bouncy Castle Provider 传递依赖排除项。

## [3.13.0] - 2026-05-09

### Added

- 新增 `fist-sde` 首阶段模块，提供安全 envelope 编解码、签名输入规范化、基础加密签名扩展、Spring MVC Request Body 解密验签、Response Body 加密签名，以及 Spring Boot 3 自动配置入口。
- 新增 `fist-sde` secure envelope 跨语言协议规范文档，说明字段语义、payload 编码、签名输入、请求/响应示例和排查建议。
- 新增 `fist-sde` 首阶段 artifact 的 BOM 版本管理条目。
- 新增 `fist-sde-extra` 国密 SM4-GCM 加解密和 HMAC-SM3 签名实现。
- 新增 `fist-sde` 模块接入说明，覆盖服务端启用、默认算法、国密 Provider、必需 Bean、异常转换和首阶段边界。
- 新增 `fist-sde-extra` SM3 摘要适配实现。

### Changed

- 将 Bouncy Castle Provider 依赖从 `bcprov-jdk15to18` 切换为 `bcprov-jdk18on`，并排除 `tile-crypto` 传递带入的旧 Provider artifact。

### Fixed

- 修正 `fist-sde-web` 入站请求未校验 envelope 协议格式版本的问题，当前实现只接受 `version` 为 `1` 的请求。
- 修正 `fist-sde-extra` 测试级内存重放校验器，使其优先使用当前交换上下文的时间窗口。

### Removed

- 移除 `fist-sde-extra` 中不可用的 SM4 占位实现，保留可用的 `Sm4GcmCryptoHandler`。
