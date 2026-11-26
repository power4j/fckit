# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

FIST Kit 4.x 是基于 Spring Boot 4.x / Spring Cloud 2026.0 的企业级基础框架库，以 Maven 多模块形式组织，发布到 Maven Central。

- JDK 17，Spring Boot 4.2.x，Spring Cloud 2026.0.x，Spring Cloud Alibaba 2025.1.0.0
- Maven 坐标：`com.power4j.fist4`，版本由根 POM 的 `${revision}` 统一管理
- 面向 Spring Boot 4 独立维护；v3 线（`com.power4j.fist3`）在旧仓库归档

## 模块结构

```
fist-kit-build/          # 构建配置与插件管理
fist-kit-dependencies/   # BOM 依赖管理
fist-kit-infra/          # 基础设施层（logback、jackson、jasypt、mybatis-plus、redisson、sde、trace context 等）
fist-kit-app/            # 应用层（web、data、security、apidoc）
fist-kit-cloud/          # 云集成层（gateway、feign RPC）
examples/                # 组合示例（profile: examples）
benchmarks/              # 基准测试（profile: benchmarks）
```

Boot 4 专有 starter 使用 `spring-boot4-starter` 命名（`fist-jasypt-spring-boot4-starter`、`fist-sde-spring-boot4-starter`、`fist-trace-context-spring-boot4-starter`）。

## 常用命令

```bash
# 编译
mvn clean compile

# 运行所有测试
mvn test

# 运行单个模块测试
mvn test -pl fist-kit-infra/fist-jackson

# 运行单个测试类
mvn test -pl fist-kit-infra/fist-jackson -Dtest=ObfuscatedStringSerializerTest

# 完整构建（含测试）
mvn clean package

# 本地安装
./install-local.sh
```

## 代码规范

提交前必须通过 `spring-javaformat-maven-plugin` 格式检查：

```bash
mvn spring-javaformat:apply   # 自动格式化
mvn spring-javaformat:validate # 仅校验
```

## 测试框架

- JUnit 5 + Mockito + AssertJ（单元与上下文测试）
- 条件集成测试：设置环境变量（如 `FIST_IT_JDBC_URL`、`FIST_IT_REDIS_URL`）后启用，默认跳过
- 测试工具模块：`fist-kit-infra/fist-support-test`

## 关键架构说明

- **依赖管理**：所有版本号统一在 `fist-kit-dependencies/pom.xml` 中声明，子模块不写版本号
- **自动配置**：应用层模块（`fist-boot-*`）遵循 Spring Boot 4 starter 模式，通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册
- **Jackson 3**：Servlet MVC JSON 路径使用 `tools.jackson`（Jackson 3）；SDE 默认 codec 仍为 Jackson 2，注意区分
- **分层原则**：`fist-kit-infra` 不依赖 `fist-kit-app`，`fist-kit-app` 不依赖 `fist-kit-cloud`

## 发布

- 版本发布流程见 `docs/public/develop/release-process.md`；变更记录见 `CHANGELOG.md` 与 `docs/public/develop/release-notes-4.x.md`
- push tag 触发 GitHub Actions（`oss-deploy.yml`）发布到 Maven Central（GPG 签名 + central-publishing 插件）
