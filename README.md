[![Maven Central](https://img.shields.io/maven-central/v/com.power4j.fist4/fist-kit-dependencies)](https://central.sonatype.com/search?namespace=com.power4j.fist4&name=fist-kit-dependencies)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/power4j/fckit)

## 主要版本

- JDK: `17`
- spring-boot: `4.2`
- spring-cloud: `2026.0`
- spring-cloud-alibaba: `2025.1.0.0`

## FIST Kit 4.x

FIST Kit 是以 Maven 多模块组织的企业级基础框架库。4.x 面向 Spring Boot 4 独立维护，Maven 坐标为 `com.power4j.fist4`。

### 模块结构

```
fist-kit-build/          # 构建配置与插件管理
fist-kit-dependencies/   # BOM 依赖管理
fist-kit-infra/          # 基础设施层（logback、jackson、jasypt、mybatis-plus、redisson、sde、trace context 等）
fist-kit-app/            # 应用层（web、data、security、apidoc）
fist-kit-cloud/          # 云集成层（gateway、feign RPC）
```

Boot 4 专有 starter 使用 `spring-boot4-starter` 命名（Jasypt、SDE、Trace Context）。

### 文档

- [发布说明](docs/public/develop/release-notes-4.x.md)
- [应用迁移指南](docs/public/develop/migration-guide-4.x.md)

## 配置加密

- [Spring Boot 4.2 Jasypt 主密钥配置](docs/public/develop/jasypt-master-key.md)
- [Jasypt CLI](fist-kit-infra/fist-jasypt/fist-jasypt-cli/README.md)
- [Jasypt 基础示例](examples/fist-jasypt/README.md)
