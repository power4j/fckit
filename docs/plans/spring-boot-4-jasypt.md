# Jasypt 迁移至 Spring Boot 4.2

基线为 Kit v4 `m1/boot42-kit@bc03d43b`。Jasypt 源码和历史合成示例取自 Kit v3
`dev@c20176b5`。本轮使用 reactor 的 `com.power4j.fist3` 组。

1. 迁入 core 和 CLI。执行 core、CLI 单元测试及独立 JAR 集成测试。
2. 迁入 Boot 4 starter、配置文档和基础示例。验证旧密文、密钥来源选择及失败契约。
3. 安装到工作区 Maven 仓库。用独立工程离线导入 BOM 并启动 Boot 4.2。

## 实施结果

| 提交 | 内容 |
| --- | --- |
| `02243d94` | 迁入 core、CLI 和测试，补齐 core BOM 条目。 |
| `5e333273` | 迁入 Boot 4 starter、基础示例和接入文档，补齐 starter BOM 条目。 |

core 沿用 v3 的 Java 包、公开方法和 GMENC v1 格式。Tile 对齐 v3 的
`1.8.3`，提供 core 使用的 `HmacSm3Util`。
CLI 沿用参数和独立可执行 JAR。

Spring Boot 4.2 移除了旧的 `EnvironmentPostProcessor` 类型。
上游 Jasypt Boot `4.0.4` 引用了该类型。
Kit starter 直接适配 Boot 4.2，依赖 Jasypt `1.9.3` 的 `StringEncryptor`。
保留 `jasyptStringEncryptor` 命名和覆盖约定、`fist.jasypt.*`、
显式密文边界和主密钥来源优先级。
适配层的范围是应用上下文中的配置读取和绑定。上游 starter 的日志重初始化及
其他扩展属于独立能力。
参见 [Boot 4.2 发布说明][boot-release]、[上游适配 PR][upstream] 和
[主密钥配置](../public/develop/jasypt-master-key.md)。

## 本地验收

使用 Temurin JDK `17.0.16`、仓库 Maven Wrapper、Spring Boot `4.2.0`
和工作区本地 Maven 仓库。

| 验证 | 结果 |
| --- | --- |
| `spring-javaformat:validate`，含示例与基准模块 | 62/62 模块通过。 |
| Jasypt、示例及 `fist-support-security` 定向安装 | 22/22 模块、120/120 用例通过。 |
| Jasypt 用例 | core 5，starter 28，CLI 单测 9，CLI 打包集成测试 1，全部通过。 |
| 独立消费者离线 `test` | 6/6 通过，从 Maven 仓库的 JAR 加载 core 和 starter。 |
| 示例 JAR 和 CLI JAR | 示例用 `raw` 和 `file` 均启动通过。CLI 解密历史样本通过。 |
| Markdown 和 Git | 新增文档 Markdown lint、相对链接和 Git 差异检查通过。 |

独立消费者导入 Boot BOM 与
`com.power4j.fist3:fist-kit-dependencies:4.0.0-SNAPSHOT`，
省略 core 和 starter 的依赖版本。使用 v3 示例 Git blob 中的固定合成密文和
配套文件，验证 `Environment`、`@Value`、`@ConfigurationProperties`、
`StringEncryptor` 读取结果。旧文件优先用例同时设置了错误的旧属性值。

| 历史样本用例 | 结果 |
| --- | --- |
| 显式 `file`，同时配置无关原值 | 通过 |
| 显式 `raw`，同时配置不存在的文件 | 通过 |
| 旧文件优先，且配置了旧属性 | 通过 |
| 默认旧环境变量来源 | 通过 |
| 自定义名称的旧环境变量来源 | 通过 |
| core 读取固定密文，错误密钥被拒绝 | 通过 |

工作区本地过程目录为 `temp/m1-jasypt/`，保留独立消费者与 Maven 日志。
本轮使用公开合成数据。实际 Nacos 和 PAC 服务集成留待应用阶段验证。
统一 Maven 组 `com.power4j.fist4` 属于下一阶段。

[boot-release]: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.2-Release-Notes
[upstream]: https://github.com/ulisesbocchio/jasypt-spring-boot/pull/439
