# FIST Kit 4.x 发布说明（草案）

状态为待评审。本文在历史基线 `c8eaf80b` 的 `4.0.0-SNAPSHOT` 验收结果上，补充截至 `027019c0` 的已核证变更，包括 `2e5cb220` 的发布父 POM 继承收窄和 `027019c0` 的 SDE WebClient 装配修复。历史用例结果仍对应原验收基线，不表示已在当前提交全量复跑。正式版本号、发布日期与制品可用性以正式发布记录为准。

[4.x 应用迁移指南](migration-guide-4.x.md) · [变更日志](../../../CHANGELOG.md)

## 验证基线

| 组件 | 采用版本 |
| --- | --- |
| Java | JDK 17 |
| Spring Boot | `4.2.0` |
| Spring Cloud | `2026.0.0` |
| Springdoc OpenAPI | `3.2.1` |
| Spring Authorization Server | 随 Spring Boot BOM 管理 |

## 应用侧变化

- Kit 的 Maven groupId 改为 `com.power4j.fist4`。两代 BOM 中有 30 对 Kit 组件，Jasypt、Trace Context 和 SDE starter 的 artifactId 同时改名。自 `2e5cb220` 起，继承发布的 `fist-kit-parent` 或 `fist-kit-build` 仍在 `validate` 执行 v3 依赖检查，但不再自动执行 Kit 的 Java 格式检查、POM 扁平化、源码归档和 SBOM 生成；这四项由应用按需配置。单独导入 Kit BOM 的应用自行配置 Enforcer 和构建插件。
- Kit 的 Servlet MVC JSON 路径采用 Jackson 3。默认数字、日期和混淆行为受应用配置影响，SDE 默认 codec 继续使用 Jackson 2。升级时按实际消费方核对线上 JSON 和历史存储数据。
- API 文档使用 Springdoc 3 与 Swagger Core 3，移除 Springfox 支持。MyBatis CRUD 切换到 MyBatis Plus 的 Boot 4 运行与测试 starter。Kit 移除了 Spring Cloud Alibaba BOM 和 Feign 的 Sentinel starter 依赖。
- OAuth2 JDBC 授权服务移除了接收 `LobHandler` 的三参构造器及 row mapper 的 `setLobHandler`。读写 mapper 均采用 Jackson 3。自定义映射的应用按指南迁移并复验历史数据。[OAuth2 授权持久化](migration-guide-4.x.md#oauth2-授权持久化)
- 自定义授权 provider 的 `setRefreshTokenGenerator(Supplier<String>)` 已移除。刷新令牌改由构造器传入的 `OAuth2TokenGenerator` 生成，消费者需同时保留访问令牌生成能力。[OAuth2 刷新令牌定制](migration-guide-4.x.md#oauth2-刷新令牌定制)
- 已移除入口与兼容缺口：此前 v4 曾移除 `@InTenant`、`InTenantAspect` 和错误工具类 `Breaker`，现已按 v3 原样恢复并保留 `@Deprecated`。恢复内容包括注解、切面与 `DataAutoConfiguration` 的 `inTenantAspect` bean，以及 `Breaker`、`MessageConfiguration` 和 `CommonConfiguration` 对 `MessageConfiguration` 的导入。新代码推荐使用 `TenantBroker`（`com.power4j.fist.data.tenant.isolation`）与 `CommonErrors`、`MsgBundleRejectedException`，旧入口仅为 v3 兼容保留。升级方按实际调用点复验租户切换、错误码与国际化行为。[已移除入口与兼容缺口](migration-guide-4.x.md#已移除入口与兼容缺口)
- Trace Context 提供基础和扩展示例。网关在入站、出站追踪头异名时复用当前请求上下文。`lb://` 内省已验证服务发现和常规 URI。两代 LoadBalancer 均可能在查询值含裸 `=` 且 URI 其他位置已有百分号转义时，将 `%` 再编码为 `%25`。对照样本将值内的 `=` 编码为 `%3D` 后通过。[网关内省 URI](migration-guide-4.x.md#网关内省-uri)
- Jasypt 支持完整 `GMENC(...)` 与密文体占位符，已验证应用上下文中的环境读取、值注入和配置绑定。属性解析异常采用脱敏消息。日志对照中，v3 在重初始化后把明文传给 appender，v4 appender 初始化时收到密文，应用上下文随后可读取明文。指向异名加密器 bean 时，默认 bean 仍需主密钥。[上游 Jasypt Boot 4.2 适配 PR #439](https://github.com/ulisesbocchio/jasypt-spring-boot/pull/439)
- Redisson 两代离线消费者通过配置装配、模拟队列与 Topic 回调，以及合成 Codec 样本的双向互读。真实 Redis 交付和部署数据由使用方验收。[Redisson 消息与 Codec](migration-guide-4.x.md#redisson-消息与-codec)
- SDE 的 RestClient 和 WebClient 原 runner 已在两代各通过同代回环调用。RestClient 到 MVC 的跨代成功样本双向通过。Kit v4 的两种 HTTP Service 分组代理通过四项合成回环调用，核对了显式方法策略、默认策略、组级请求头、双向加密签名 envelope 和解包结果。分组代理跨代互通和错误回包由使用方另行验收。
- `027019c0` 恢复通过 Boot 4 的 `WebClientCustomizer` 为 Boot 注入的 Builder 挂载 SDE，移除早期 v4 的 singleton Builder fallback，保留 Boot prototype Builder 及其他定制。应用自建 Builder 仍需显式挂载 SDE，行为与 v3 一致。[SDE WebClient Builder 装配](migration-guide-4.x.md#sde-webclient-builder-装配)
- CRUD 恢复 `AbstractNodeIdxSupport.moveSubtree` 并验证写入异常的事务回滚。调用方按实际数据库校验主表与闭包的一致性，并明确并发结构写约定。

本地验证覆盖了上述用例和离线 Maven 解析。应用的依赖组合、持久化样本及启用的链路可按迁移指南逐项验收。
