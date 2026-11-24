# example-trace-context-extension

自定义 item 示例，注册 `tenant-id` processor，并通过配置把 `X-TENANT-ID` 写入 `TraceContext` 和 MDC。

在仓库根目录启动扩展示例。

```bash
java -jar examples/fist-trace-context/example-trace-context-extension/target/example-trace-context-extension-4.0.0-SNAPSHOT.jar
```

```bash
curl -H "X-REQ-UID: REQ-1001" -H "X-TENANT-ID: tenant-a" http://localhost:8080/trace
```

响应和日志均显示请求 ID、租户 ID 和系统代码。省略租户头时，响应中省略 `tenantId`。

新增一种追踪上下文项的最短路径：

1. 实现 `TraceContextItemFactory`。
2. 继承 `AbstractPropDrivenTraceContextItem` 或 `AbstractSingleValueTraceContextItem`。
3. 将 factory 注册为 Spring Bean。
4. 在 `fist.trace-context.items` 中配置 item ID、`processor` 和该 processor 需要的 `props`。
