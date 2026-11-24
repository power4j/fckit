# examples

fist-kit 使用示例集合，通过 Maven profile 独立构建。

```bash
mvn compile -Pexamples
```

## 模块

| 模块 | 说明 |
|------|------|
| [fist-logback/example-logback-basic](fist-logback/example-logback-basic) | 日志脱敏基础接入 |
| [fist-logback/example-logback-extension](fist-logback/example-logback-extension) | 自定义 Detector / Transformer 扩展 |
| [fist-oauth2-server/example-authorization-server](fist-oauth2-server/example-authorization-server) | OAuth2 Authorization Server metadata 与 token 请求 |
| [fist-trace-context](fist-trace-context/README.md) | Trace Context 默认接入与自定义 item |
| [fist-web-data-security/example-servlet](fist-web-data-security/example-servlet) | Web、Data、Security、API Doc starter 组合启动 |
