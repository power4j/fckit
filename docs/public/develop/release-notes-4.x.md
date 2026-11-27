# FIST Kit 4.x 发布说明

[4.x 应用迁移指南](migration-guide-4.x.md) · [变更日志](../../../CHANGELOG.md)

## 4.0.0-rc3

发布日期：2026-11-27。

### 依赖升级（相对 rc2）

mybatis-plus `3.5.18.1`（含全表校验绕过与租户拦截器越权两个安全修复）、mybatis-spring `4.1.0`、excel-spring-boot-starter `4.0.0`。

### 升级注意

- `CrudRepository` 由 `com.baomidou.mybatisplus.extension.repository` 迁至 `com.baomidou.mybatisplus.spring.repository`，类 API 未变，调整 import 即可。
- FastExcel（cn.idev.excel）已捐 Apache 更名 Fesod：`org.apache.fesod.sheet`，常用类 API 延续；`excel-spring-boot-starter` 4.0.0 的传递依赖已随之切换。

## 4.0.0-rc2

发布日期：2026-11-27。首个公开预发布版本，已发布至 Maven Central（rc1 因发布流水线配置期间版本号被跳过，内容并入 rc2）。

### 依赖升级（相对 rc1 准备基线）

spring-boot-admin `4.1.5`、spring-javaformat `0.0.48`、guava `33.8.0-jre`、bouncycastle `bcprov-jdk18on 1.87`、nimbusds `11.39`、commons-collections4 `4.6.0`、cyclonedx-maven-plugin `2.9.3`、maven-compiler-plugin `3.16.0`、jacoco `0.8.16`，以及构建与文档工作流组件升级。

### 验证基线

| 组件 | 采用版本 |
| --- | --- |
| Java | JDK 17 |
| Spring Boot | `4.2.0` |
| Spring Cloud | `2026.0.0` |
| Spring Cloud Alibaba | `2025.1.0.0` |
| Springdoc OpenAPI | `3.2.1` |
| Spring Authorization Server | 随 Spring Boot BOM 管理 |

### 应用侧变化

- Kit 的 Maven groupId 改为 `com.power4j.fist4`。两代 BOM 中有 30 对 Kit 组件，Jasypt、Trace Context 和 SDE starter 的 artifactId 同时改名。继承发布的 `fist-kit-parent` 或 `fist-kit-build` 仍在 `validate` 执行 v3 依赖检查，但不再自动执行 Kit 的 Java 格式检查、POM 扁平化、源码归档和 SBOM 生成；这四项由应用按需配置。单独导入 Kit BOM 的应用自行配置 Enforcer 和构建插件。
- Kit 的 Servlet MVC JSON 路径采用 Jackson 3。默认数字、日期和混淆行为受应用配置影响，SDE 默认 codec 继续使用 Jackson 2。升级时按实际消费方核对线上 JSON 和历史存储数据。
- API 文档使用 Springdoc 3 与 Swagger Core 3，移除 Springfox 支持。MyBatis CRUD 切换到 MyBatis Plus 的 Boot 4 运行与测试 starter。Kit 移除了 Spring Cloud Alibaba BOM 和 Feign 的 Sentinel starter 依赖。
- OAuth2 JDBC 授权服务移除了接收 `LobHandler` 的三参构造器及 row mapper 的 `setLobHandler`。读写 mapper 均采用 Jackson 3。自定义映射的应用按指南迁移并复验历史数据。[OAuth2 授权持久化](migration-guide-4.x.md#oauth2-授权持久化)
- 自定义授权 provider 的 `setRefreshTokenGenerator(Supplier<String>)` 已移除。刷新令牌改由构造器传入的 `OAuth2TokenGenerator` 生成，消费者需同时保留访问令牌生成能力。[OAuth2 刷新令牌定制](migration-guide-4.x.md#oauth2-刷新令牌定制)
- `@InTenant`、`InTenantAspect` 和错误工具类 `Breaker` 曾在升级中删除，现已按 v3 原样恢复并保留 `@Deprecated`。恢复内容包括注解、切面与 `DataAutoConfiguration` 的 `inTenantAspect` bean，以及 `Breaker`、`MessageConfiguration` 和 `CommonConfiguration` 对 `MessageConfiguration` 的导入。新代码推荐使用 `TenantBroker`（`com.power4j.fist.data.tenant.isolation`）与 `CommonErrors`、`MsgBundleRejectedException`，旧入口仅为 v3 兼容保留。升级方按实际调用点复验租户切换、错误码与国际化行为。[已移除入口与兼容缺口](migration-guide-4.x.md#已移除入口与兼容缺口)
- Trace Context 提供基础和扩展示例。网关在入站、出站追踪头异名时复用当前请求上下文。`lb://` 内省已验证服务发现和常规 URI。两代 LoadBalancer 均可能在查询值含裸 `=` 且 URI 其他位置已有百分号转义时，将 `%` 再编码为 `%25`。对照样本将值内的 `=` 编码为 `%3D` 后通过。[网关内省 URI](migration-guide-4.x.md#网关内省-uri)
- Jasypt 支持完整 `GMENC(...)` 与密文体占位符，已验证应用上下文中的环境读取、值注入和配置绑定。属性解析异常采用脱敏消息。日志对照中，v3 在重初始化后把明文传给 appender，v4 appender 初始化时收到密文，应用上下文随后可读取明文。指向异名加密器 bean 时，默认 bean 仍需主密钥。Jasypt 早期日志（logback 重初始化前）解密兼容为已知限制，见迁移指南。[上游 Jasypt Boot 4.2 适配 PR #439](https://github.com/ulisesbocchio/jasypt-spring-boot/pull/439)
- Redisson 两代离线消费者通过配置装配、模拟队列与 Topic 回调，以及合成 Codec 样本的双向互读。真实 Redis 交付和部署数据由使用方验收。[Redisson 消息与 Codec](migration-guide-4.x.md#redisson-消息与-codec)
- SDE 的 RestClient 和 WebClient 原 runner 已在两代各通过同代回环调用。RestClient 到 MVC 的跨代成功样本双向通过。Kit v4 的两种 HTTP Service 分组代理通过四项合成回环调用，核对了显式方法策略、默认策略、组级请求头、双向加密签名 envelope 和解包结果。分组代理跨代互通和错误回包由使用方另行验收。
- SDE WebClient 通过 Boot 4 的 `WebClientCustomizer` 为 Boot 注入的 Builder 挂载，保留 Boot prototype Builder 及其他定制。应用自建 Builder 仍需显式挂载 SDE，行为与 v3 一致。[SDE WebClient Builder 装配](migration-guide-4.x.md#sde-webclient-builder-装配)
- CRUD 恢复 `AbstractNodeIdxSupport.moveSubtree` 并验证写入异常的事务回滚。调用方按实际数据库校验主表与闭包的一致性，并明确并发结构写约定。

### 已知限制

- Jasypt 在日志重初始化前的早期日志阶段不解密（appender 只在启动后初始化一次，收到密文）。依赖早期日志明文的应用需调整配置加载顺序或另行处理。
- 预发布版本（rc）不建议用于生产；正式版（GA）将在 rc 验证收口后发布。

本地验证覆盖了上述用例和离线 Maven 解析。应用的依赖组合、持久化样本及启用的链路按迁移指南逐项验收。
