# example-trace-context-basic

在仓库根目录启动基础示例，通过 `GET /trace` 查看当前 `TraceContext`。

```bash
java -jar examples/fist-trace-context/example-trace-context-basic/target/example-trace-context-basic-4.0.0-SNAPSHOT.jar
```

```bash
curl -H "X-REQ-UID: REQ-1001" http://localhost:8080/trace
```

未传 `X-REQ-UID` 时，默认 `correlation-id` item 会生成请求 ID。

响应的 `values` 包含 `requestId` 和 `systemCode`。服务方法的日志中可以看到 `basic.work`。方法返回后 span 已退出，响应中的 `spanId` 为空字符串。
