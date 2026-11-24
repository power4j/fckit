# FIST Kit 4.x 发布说明（草案）

状态为待评审。本文基于本地源码 `c8eaf80b` 和 `4.0.0-SNAPSHOT` 的验收结果。正式版本号、发布日期与制品可用性以正式发布记录为准。

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

- Kit 的 Maven groupId 改为 `com.power4j.fist4`。两代 BOM 中有 30 对 Kit 组件，Jasypt、Trace Context 和 SDE starter 的 artifactId 同时改名。继承 Kit 父 POM 会启用 v3 依赖检查并继承构建插件，单独导入 Kit BOM 的应用可以在自身 POM 中启用相同规则。
- Kit 的 Servlet MVC JSON 路径采用 Jackson 3。默认数字、日期和混淆行为受应用配置影响，SDE 默认 codec 继续使用 Jackson 2。升级时按实际消费方核对线上 JSON 和历史存储数据。
- API 文档使用 Springdoc 3 与 Swagger Core 3，移除 Springfox 支持。MyBatis CRUD 切换到 MyBatis Plus 的 Boot 4 运行与测试 starter。Kit 移除了 Spring Cloud Alibaba BOM 和 Feign 的 Sentinel starter 依赖。
- Trace Context 提供基础和扩展示例。网关在入站、出站追踪头异名时复用当前请求上下文。网关 `lb://` 内省保留服务发现及原始 URI 路径和查询。使用转义路径和查询组合的应用应核对实际下游收到的 URI。
- Jasypt 支持完整 `GMENC(...)` 与密文体占位符，已验证应用上下文中的环境读取、值注入和配置绑定。属性解析异常采用脱敏消息。依赖日志重初始化或其他上游扩展的应用需单独验证这些路径。
- SDE 的 RestClient 和 WebClient 原 runner 已在两代各通过同代回环调用。Kit v4 的两种 HTTP Service 分组代理各通过一项合成成功调用，核对了方法策略、双向加密签名 envelope 和解包结果。跨代互通、错误回包和不同配置的组合由使用方另行验收。
- CRUD 恢复 `AbstractNodeIdxSupport.moveSubtree` 并验证写入异常的事务回滚。调用方按实际数据库校验主表与闭包的一致性，并明确并发结构写约定。

本地验证覆盖了上述用例和离线 Maven 解析。应用的依赖组合、持久化样本及启用的链路可按迁移指南逐项验收。
