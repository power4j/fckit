# FIST Trace Context Examples

本目录提供 `fist-trace-context-spring-boot4-starter` 的两个示例。

| 示例 | 重点 |
| --- | --- |
| [基础示例](example-trace-context-basic/README.md) | 默认 `X-REQ-UID`、`requestId`、`systemCode`、`spanId` 能力 |
| [扩展示例](example-trace-context-extension/README.md) | 自定义 `TraceContextItemFactory` 和自定义 item 配置 |

在仓库根目录构建两个可执行 JAR。

```bash
./mvnw -Pexamples -pl :example-trace-context-basic,:example-trace-context-extension -am package
```
