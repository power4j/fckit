# Spring Boot 4 依赖矩阵

日期：2026-05-10
执行者：Codex
阶段：阶段 1「版本与生态准入核验」

## 结论摘要

当前可进入阶段 2「只改版本试编译」。阶段 1 的结论是「候选坐标准入通过，但功能兼容未通过验证」。

- Spring Boot 最新发布元数据为 `4.1.0-RC1`，但最新 GA 仍按 `4.0.6` 处理；本轮目标版本保持 `4.0.6`。
- Spring Cloud 当前候选为 `2025.1.1`。
- Spring Cloud Alibaba 当前候选为 `2025.1.0.0`，Maven Central 坐标存在，并管理 Nacos `3.1.1`、Sentinel `1.8.9`。
- Spring Cloud Alibaba 不再阻塞进入试编译，但仍是阶段 2 的第一优先级风险。原因是 Sentinel POM 中仍出现 `sentinel-spring-cloud-gateway-v6x-adapter`、`sentinel-spring-webmvc-v6x-adapter` 等 Spring 6.x 命名适配器，需验证其在 Spring Boot 4 / Spring Framework 7 / Spring Cloud Gateway 5 下的真实编译结果。
- `spring-security-jwt` 仅应保留为待删除项，不应作为 Boot 4 目标依赖进入试编译。
- Groovy/Spock 相关依赖已删除，测试改为 Java/JUnit 5。

## 核验方法

- 读取根 `pom.xml`、`fist-kit-build/pom.xml`、`fist-kit-dependencies/pom.xml`。
- 读取 Maven Central 元数据，确认候选版本。
- 直接校验 Maven Central 候选 POM/JAR URL 是否返回 `200`。
- 读取 Spring Cloud Alibaba `2025.1.0.0` POM，确认 Nacos 与 Sentinel 受管版本。
- 使用 `rg` 扫描当前仓库中 Spring Cloud Alibaba、Gateway、OpenFeign、LoadBalancer、SpringDoc、Redisson、MyBatis Plus、Security、Boot starter 的使用点。

说明：曾尝试使用 Maven `dependency:get` 校验候选坐标，但 Maven 会先解析当前多模块工程模型，并被本地仓库中既有 3.x BOM 的缓存传输失败拦截。因此该命令失败不作为候选版本不可用证据。本阶段采用 Maven Central URL 与 POM 元数据作为坐标准入依据，真实工程解析放到阶段 2。

## 核心 BOM 与框架版本

| 项目 | 当前版本 | 候选版本 | 准入结论 | 阶段 2 验证重点 |
| --- | --- | --- | --- | --- |
| 项目根版本 | `4.0.0-SNAPSHOT` | `4.0.0-SNAPSHOT` | 已调整 | 确认 flatten 后发布版本正确 |
| Spring Boot | `3.5.14` | `4.0.6` | 坐标存在 | starter 拆分、Jackson 3、测试依赖、配置属性 |
| Spring Cloud | `2025.0.2` | `2025.1.1` | 坐标存在 | Gateway、OpenFeign、LoadBalancer API |
| Spring Cloud Alibaba | `2023.0.3.4` | `2025.1.0.0` | 坐标存在，功能兼容待验证 | Nacos、Sentinel、Gateway、LoadBalancer、OpenFeign 组合 |
| MyBatis Plus BOM | `3.5.16` | `3.5.16` | 坐标存在，已切换 Boot 4 starter | 下游需使用 `mybatis-plus-spring-boot4-starter` |
| SpringDoc OpenAPI BOM | `2.8.17` | `3.0.3` | 坐标存在 | `springdoc-openapi-starter-common`、WebFlux API 适配 |
| Spring Boot Admin BOM | `3.5.8` | `4.0.4` | 坐标存在 | Admin starter 与 Actuator 端点兼容 |
| OkHttp BOM | `4.12.0` | `4.12.0` | 暂不升级 | Feign OkHttp 传递依赖验证 |
| Spock BOM | `2.4-groovy-5.0` | 移除 | 已移除 | 5 个 Groovy 测试已迁移到 Java/JUnit 5 |
| Coca BOM | `0.4.1` | `0.4.1` | 暂不升级 | 仅随工程解析验证 |
| Tile BOM | `1.8.2` | `1.8.2` | 暂不升级 | 仅随工程解析验证 |

## Spring Cloud Alibaba 准入

| 能力 | 当前使用点 | 候选版本事实 | 阶段 1 结论 | 阶段 2 动作 |
| --- | --- | --- | --- | --- |
| Sentinel | `fist-cloud-rpc-feign` 使用 `spring-cloud-starter-alibaba-sentinel` | BOM 管理 Sentinel `1.8.9` | 坐标存在，兼容性未确认 | 先编译 `fist-kit-cloud/fist-cloud-rpc-feign` |
| Nacos | 当前仓库主要通过 BOM 暴露能力，真实内部项目样本可能使用 | BOM 管理 Nacos client `3.1.1` | 坐标存在，库侧直接命中较少 | 后续用户侧手册记录通用检查项 |
| Gateway | `fist-cloud-gateway-*` 使用 Spring Cloud Gateway | Alibaba BOM 管理 `sentinel-spring-cloud-gateway-v6x-adapter` | 命名显示仍偏 Spring 6.x，需谨慎 | 编译 Gateway 模块并检查 Sentinel 传递依赖 |
| LoadBalancer | `fist-cloud-rpc-feign`、Gateway 测试使用 LoadBalancer | Spring Cloud 候选为 `2025.1.1` | 坐标存在 | 编译 LoadBalancer 自动配置与 Gateway 测试夹具 |
| OpenFeign | `fist-cloud-rpc-feign`、SDE Feign 示例使用 | Spring Cloud 候选为 `2025.1.1` | 坐标存在，SDE 适配未验证 | 编译 Feign 模块并记录 API 破坏点 |

准入判断：Spring Cloud Alibaba 通过「坐标存在」准入，不再阻塞阶段 2；但没有通过「功能兼容」验收。若阶段 2 在 Alibaba / Sentinel / Nacos / Gateway 组合上出现基础解析或编译失败，需要立即暂停源码迁移，优先给出移除或替换相关集成的方案。

## 项目显式依赖矩阵

| 依赖 | 当前版本 | 候选版本 / 处理方式 | 类型 | 判断 |
| --- | --- | --- | --- | --- |
| `redisson` / `redisson-spring-boot-starter` | `3.52.0` | `4.3.1` | Boot starter / 客户端 | 坐标存在，需验证 starter 自动配置 |
| `spring-security-oauth2-authorization-server` | `1.5.7` | `7.0.5` GA | Security | 高风险，需独立编译安全模块 |
| `spring-security-jwt` | `1.1.1.RELEASE` | 删除 | 退役依赖 | 不进入 Boot 4 目标依赖 |
| `oauth2-oidc-sdk` | `11.37` | 暂不主动升级 | OAuth2 支撑库 | 随 Authorization Server 7 验证 |
| `springdoc-openapi-*` | `2.8.17` | `3.0.3` | API 文档 | 坐标存在，需验证自定义 OperationCustomizer |
| `spring-boot-admin-*` | `3.5.8` | `4.0.4` | 管理端 | 坐标存在，需验证 Actuator 兼容 |
| `mybatis-plus-*` | `3.5.16` | `3.5.16` | 数据访问 | Boot 4 下使用 `mybatis-plus-spring-boot4-starter` 和 `mybatis-plus-spring-boot4-starter-test` |
| `mybatis-spring` | `3.0.5` | `4.0.0` | 数据访问 | 跟随 MyBatis Plus Boot 4 starter，适配 Spring Framework 7 |
| `excel-spring-boot-starter` | `3.4.3` | 待查兼容性 | Boot starter | 需阶段 2 解析和编译确认 |
| `swagger-core-jakarta` | `2.2.49` | 暂不主动升级 | OpenAPI | 先由 SpringDoc 3 BOM 约束验证 |
| `springfox-*` | `3.0.0` | 移除 | 旧 API 文档 | Boot 4 主路径只保留 SpringDoc 3，`fist-boot-apidoc` 已删除 Springfox 支持 |
| `okhttp-bom` | `4.12.0` | 暂不主动升级 | HTTP 客户端 | Feign OkHttp 适配时再确认 |
| `bucket4j_jdk17-*` | `8.18.0` | 暂不主动升级 | 限流 | 与 Boot 4 关联低，阶段 2 编译确认 |
| `jspecify` | `1.0.0` | `1.0.0` | 空值契约 | 保持 |
| `junit-pioneer` | `2.3.0` | 暂不主动升级 | 测试 | 随 Boot 4 测试栈验证 |
| `groovy` / `spock-*` / `groovy-tables` | `2.4-groovy-5.0` 等 | 删除 | 测试链路 | 已删除，测试改为 Java/JUnit 5 |
| `gmavenplus-plugin` | `4.3.1` | 删除 | 构建插件 | 已删除 |

## Boot starter 与模块化风险

| 当前依赖 | 当前使用点 | Boot 4 风险 | 处理方式 |
| --- | --- | --- | --- |
| `spring-boot-starter-web` | Web、SDE 示例、SDE client、SDE starter | Boot 4 starter 模块化后可能需迁移到 `spring-boot-starter-webmvc` | 阶段 2 先试编译，阶段 3 用 OpenRewrite dry-run 对照 |
| `spring-boot-starter-webflux` | Web、Gateway、SDE client、示例 | 需确认 WebFlux starter 坐标与测试依赖变化 | 阶段 2 编译 Web/Gateway/SDE |
| `spring-boot-starter-json` | `fist-boot-web-app` | Jackson 3 默认，Jackson 2 兼容仅迁移期使用 | Jackson 专项处理 |
| `spring-boot-starter-test` | build 与示例、数据模块 | Boot 4 测试 starter 拆分可能影响 JUnit/AssertJ/Mockito | 阶段 2 先 `compile`，后续再 `test-compile` |
| `spring-boot-starter-security` | Gateway 测试、安全模块 | Spring Security 7 DSL/API 变化 | Security 专项处理 |
| `spring-boot-starter-validation` | 数据模块 | Jakarta Validation 兼容需确认 | 阶段 2 编译数据模块 |

## 阶段 2 建议版本组合

第一轮只改版本试编译建议使用：

```xml
<spring-boot.version>4.0.6</spring-boot.version>
<spring-cloud.version>2025.1.1</spring-cloud.version>
<spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
<springdoc-openapi.version>3.0.3</springdoc-openapi.version>
<spring-boot-admin.version>4.0.4</spring-boot-admin.version>
<spring.oauth2-authorization-server.version>7.0.5</spring.oauth2-authorization-server.version>
<redisson.version>4.3.1</redisson.version>
```

暂不主动升级：

- `mybatis-plus.version`：保持 `3.5.16`，starter 切换到 `mybatis-plus-spring-boot4-starter`。
- `spock-lib.version`、Groovy、`gmavenplus-plugin`：已删除。
- `spring-security-jwt.version`：不升级，后续删除。
- 低关联工具库：先随 Maven 解析与编译结果处理。

## 阶段 2 优先级

1. 先改 Boot / Cloud / Alibaba / SpringDoc / Boot Admin / Authorization Server / Redisson 候选版本。
2. 先编译 `fist-kit-cloud/fist-cloud-rpc-feign`，验证 Sentinel、OpenFeign、LoadBalancer。
3. 再编译 `fist-kit-cloud/fist-cloud-gateway`，验证 Gateway、Reactive Security、SpringDoc WebFlux。
4. 再编译 `fist-kit-infra/fist-jackson` 和 `fist-kit-infra/fist-sde`，验证 Jackson 3 与 SDE 编解码。
5. 再编译 `fist-kit-app/fist-security`、`fist-kit-app/fist-data`，验证 Security 7、Authorization Server 7、MyBatis Plus。
6. 最后编译 `examples` 与 `benchmarks`，记录示例和基准测试的使用方问题类型。

## 资料来源

- Spring Boot Maven Central：`https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/`
- Spring Cloud Maven Central：`https://repo.maven.apache.org/maven2/org/springframework/cloud/spring-cloud-dependencies/`
- Spring Cloud Alibaba Maven Central：`https://repo.maven.apache.org/maven2/com/alibaba/cloud/spring-cloud-alibaba-dependencies/`
- Spring Cloud Alibaba `2025.1.0.0` POM：`https://repo.maven.apache.org/maven2/com/alibaba/cloud/spring-cloud-alibaba-dependencies/2025.1.0.0/spring-cloud-alibaba-dependencies-2025.1.0.0.pom`
- Spring Security Authorization Server Maven Central：`https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-oauth2-authorization-server/`
- SpringDoc OpenAPI Maven Central：`https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-bom/`
- Spring Boot Admin Maven Central：`https://repo.maven.apache.org/maven2/de/codecentric/spring-boot-admin-dependencies/`
- Redisson Maven Central：`https://repo.maven.apache.org/maven2/org/redisson/redisson/`
- MyBatis Plus BOM Maven Central：`https://repo.maven.apache.org/maven2/com/baomidou/mybatis-plus-bom/`
- MyBatis Plus Context7 文档：`/baomidou/mybatis-plus-doc`，确认 Spring Boot 4 及以上版本使用 `mybatis-plus-spring-boot4-starter`，该 starter 自 `3.5.13` 开始提供
