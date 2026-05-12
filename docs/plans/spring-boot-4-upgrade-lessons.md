# Spring Boot 4 升级经验记录

文档版本：`0.1`
日期：2026-05-12

## 文档定位

本文档记录 Spring Boot 4 升级过程中的可复用经验、已验证方案、错误尝试和剩余注意事项。它是升级交付文档的一部分，后续每完成一个升级批次都需要追加记录。

本文档不替代以下文档：

- `docs/plans/spring-boot-4-research-report-0.1.md`：第一版调研报告。
- `docs/plans/spring-boot-4-research-report-0.2.md`：评审修订版调研报告。
- `docs/plans/spring-boot-4-code-impact.md`：代码影响和模块级处理记录。
- `docs/plans/spring-boot-4-trial-compile.md`：试编译、验证命令和日志索引。

## 记录规则

- 每条经验需要包含「问题」「错误尝试或风险」「有效方案」「验证证据」「后续复用规则」。
- 错误尝试即使已修正也需要保留，避免后续重复踩坑。
- 只记录可复用结论，不粘贴大段日志。日志原文保存在 `.codex/`，不提交。
- 涉及外部框架行为时，优先记录官方文档、Context7 文档、本地 JAR 或 `javap` 证据。
- 涉及测试结果时，记录命令范围、是否实际运行目标测试、失败数和未覆盖项。

## 经验 1：先试编译，再人工扫描

问题：

Spring Boot 4 升级涉及 Spring Framework 7、Spring Security 7、Spring Authorization Server 7、Spring Cloud 2025、Jackson 3、Commons Lang 3.20 等多条升级线。只靠人工扫描容易遗漏真实编译错误。

错误尝试或风险：

- 若先按文档逐项扫描，容易把精力放在未实际触发的风险上。
- OpenRewrite dry-run 能发现方向，但补丁范围可能过大，例如 API Doc 模块中 Springfox 相关内容会被工具建议删除，不能直接套用。

有效方案：

- 先做版本和生态准入核验。
- 再做「只改版本」试编译，优先收集 Maven 解析错误和真实编译错误。
- 之后用编译错误驱动源码扫描，再用 OpenRewrite dry-run、Context7、JAR 反编译和人工检索交叉验证。

验证证据：

- `docs/plans/spring-boot-4-trial-compile.md` 已记录各模块试编译结果。
- `target/rewrite/rewrite.patch` 作为 OpenRewrite dry-run 证据保留在本地。

后续复用规则：

- 新模块进入迁移前，先跑目标模块 `-am -DskipTests` 编译，再决定是否做源码迁移。
- 自动化工具只作为线索来源，不直接作为最终补丁来源。

## 经验 2：PowerShell 下 Maven 参数必须显式加引号

问题：

Windows PowerShell 会把部分 `-D` 参数拆分，导致 Maven 误判生命周期阶段。

错误尝试或风险：

- 曾执行未加引号的 `-Dsurefire.failIfNoSpecifiedTests=false`，Maven 报错：`Unknown lifecycle phase ".failIfNoSpecifiedTests=false"`。
- 曾只指定目标模块运行测试但未加 `-am`，Maven 尝试从远程仓库解析本地 `4.0.0-SNAPSHOT` 依赖，导致依赖解析失败。

有效方案：

在 PowerShell 中统一使用以下写法：

```powershell
.\mvnw.cmd -pl <module> -am "-Dtest=SomeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

验证证据：

- OAuth2 LOB 迁移中，修正命令后 `DefaultAuthorizationServiceTest` 运行 2 个用例，失败 0。
- OAuth2 定向测试组合运行 5 个用例，失败 0。

后续复用规则：

- 目标模块依赖仓库内快照模块时，默认加 `-am`。
- Surefire、编译器和其他带点号的 `-D` 参数在 PowerShell 中统一加引号。

## 经验 3：Spring 空值注解迁移不能只做文本替换

问题：

Spring Framework 7 下 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields` 进入 deprecation 范围，项目需要统一迁移到 JSpecify。

错误尝试或风险：

- 机械替换 `@Nullable` 容易丢失类型使用位置语义。
- 数组和泛型尤其容易出错，例如 `Object @Nullable []` 与 `@Nullable Object[]` 表达的可空对象不同。

有效方案：

- 方法返回值、参数、字段按 JSpecify 类型使用位置迁移。
- 包级默认非空使用 `@NullMarked` 的 `package-info.java`。
- 逐模块迁移，并配合模块级编译和测试验证。

验证证据：

- 全仓 `*.java` 复扫未命中 `org.springframework.lang.Nullable`、`NonNullApi`、`NonNullFields`。
- Gateway、OAuth2、API Doc、Data、Redisson 等模块均已完成对应批次验证。

后续复用规则：

- 新增公共 API 包优先使用 `@NullMarked`。
- 泛型、数组和可选返回值需要人工判断语义，不能批量替换。

## 经验 4：Commons Lang 3.20 迁移优先使用官方替代项

问题：

Commons Lang 3.20 暴露了多处 deprecation，包括 `Validate.notNull(T)`、`ObjectUtils.defaultIfNull(...)`、`StringUtils.equals(...)`、`StringUtils.startsWith(...)`、`StringUtils.equalsIgnoreCase(...)`、`MutableObject#getValue()` 等。

错误尝试或风险：

- 只替换代码中直接报 warning 的模块，会遗漏依赖链编译日志暴露的同类问题。
- `getValue()` 名称在项目枚举、Map.Entry、表达式对象中也存在，不能全仓机械替换。

有效方案：

- `Validate.notNull(T)` 迁移到 `Objects.requireNonNull(T)`。
- `ObjectUtils.defaultIfNull(...)` 迁移到 `ObjectUtils.getIfNull(...)`。
- `StringUtils.startsWith(...)` 迁移到 `Strings.CS.startsWith(...)`。
- `StringUtils.equals(...)` 迁移到 `Strings.CS.equals(...)`。
- `StringUtils.equalsIgnoreCase(...)` 和 `String#equalsIgnoreCase(...)` 迁移到 `Strings.CI.equals(...)`。
- `MutableObject#getValue()` 迁移到 `MutableObject#get()`，但只处理 Commons Lang `MutableObject` 实例。

验证证据：

- Gateway/OAuth2 Commons Lang 迁移后，目标调用复扫未命中，Gateway 模块测试和 OAuth2 定向测试通过。
- Web/Data Commons Lang 迁移后，目标调用复扫未命中；`fist-support-data` Java 测试 6 个用例失败 0，`fist-boot-data` 测试 2 个用例失败 0。
- Context7 `/websites/commons_apache_proper_commons-lang_apidocs` 确认 `ObjectUtils.defaultIfNull(T, T)` 推荐迁移到 `ObjectUtils.getIfNull(Object, Object)`。
- 本地 `commons-lang3-3.20.0.jar` 的 `javap` 确认 `Mutable` 接口提供 `get()` 默认方法，`MutableObject#getValue()` 带 `@Deprecated`。

后续复用规则：

- 搜索 `getValue()` 时必须结合类型判断。
- `Strings.CS` 用于大小写敏感比较，`Strings.CI` 用于大小写不敏感比较。
- 替换完成后必须用 `rg` 对目标调用做复扫。

## 经验 5：Spring JDBC LOB API 需要用当前 Javadoc 和本地字节码复核

问题：

OAuth2 `DefaultAuthorizationService` 依赖 `LobHandler`、`LobCreator`、`DefaultLobHandler`。Spring Framework 7 编译日志显示这些 API 已弃用。

错误尝试或风险：

- Spring Framework 参考文档中仍能看到 LOB Handler 示例，若只看示例容易误判为无需处理。
- 该类包含达梦等数据库的 BLOB 比较兼容逻辑，不能只按官方 `JdbcOAuth2AuthorizationService` 机械替换。

有效方案：

- 使用 Spring Framework 当前 Javadoc 确认 `LobCreator` / `LobHandler` 自 6.2 起弃用，替代项为 `SqlBinaryValue` / `SqlCharacterValue`。
- 使用本地 `spring-jdbc-7.0.7.jar` 的 `javap` 复核 `@Deprecated(since="6.2")`。
- 写入 BLOB 时使用 `SqlBinaryValue`，写入 CLOB 时使用 `SqlCharacterValue`。
- 读取 BLOB 时使用 `ResultSet#getBytes(...)`，读取 CLOB 时使用 `ResultSet#getString(...)`。
- 删除公开 `LobHandler` 构造器和 setter，作为 Boot 4 主版本迁移中的破坏性 API 清理记录。

验证证据：

- OAuth2 LOB 编译为 `BUILD SUCCESS`，模块自身未再出现 Spring JDBC LOB API deprecation 警告。
- `DefaultAuthorizationServiceTest` 新增 BLOB/CLOB 参数映射测试，确认使用 `SqlBinaryValue` / `SqlCharacterValue`。
- OAuth2 定向测试组合运行 5 个用例，失败 0。
- 后续 H2 集成测试补充后，`DefaultAuthorizationServiceTest` 扩展到 4 个用例，覆盖官方 BLOB schema 和 CLOB schema 变体，失败 0。

后续复用规则：

- Spring 文档示例与 Javadoc / 字节码不一致时，以当前 Javadoc、编译器和本地 JAR 为准。
- 数据库相关迁移需要保留原有 SQL 兼容策略，并补充真实数据库矩阵验证计划。

## 经验 6：Groovy/Spock 当前是测试迁移风险，不是主代码编译风险

问题：

项目后续计划不再使用 Groovy。当前 Groovy 主要用于测试，涉及 5 个 `.groovy` 测试文件以及 gmavenplus / Spock 依赖链。

错误尝试或风险：

- 多次 Maven `test` 验证中，Java 测试运行成功，但部分 `.groovy` 测试没有实际运行。
- 如果只看 `BUILD SUCCESS`，容易误以为 Groovy 测试覆盖仍然有效。

有效方案：

- 先将 5 个 `.groovy` 测试迁移到 Java/JUnit 5。
- 再删除根 `pom.xml` 中 `gmavenplus-plugin`、`spock-lib.version`，以及 `fist-kit-build` / `fist-support-test` 中的 Groovy、Spock 依赖管理。

验证证据：

- 5 个 `.groovy` 测试已迁移到 Java/JUnit 5，迁移后定向测试共 11 个用例，失败 0。
- Web/Security/Data 三个目标模块全量测试为 `BUILD SUCCESS`；目标模块自身测试共 38 个用例，失败 0。
- 仓库内 `*.groovy` 复扫未命中；Groovy/Spock/gmavenplus 构建配置复扫未命中。
- 根 `pom.xml`、`fist-kit-build/pom.xml` 和 `fist-support-test/pom.xml` 已删除 Groovy/Spock/gmavenplus 构建链路。
- 迁移过程中发现 Groovy 属性访问与 JavaBean 方法名不完全等价，例如 `OrgNode.parentId` 迁移到 Java 后需要显式调用 `setPid(...)`。
- 迁移过程中发现 Groovy 对嵌套类辅助方法访问更宽松，Java 迁移后需要将测试辅助基类成员从私有调整为受保护。

后续复用规则：

- 后续测试统一使用 Java/JUnit 5，不再新增 Groovy/Spock 测试。
- Groovy 测试迁移不能逐字翻译，需要核对属性访问、方法可见性和数据表构造方式。
- 删除构建插件前必须完成测试等价迁移并运行目标测试。

## 经验 7：API Doc 不能只按 Springfox 删除处理

问题：

API Doc 模块仍有 Springfox 退役、Jackson 2 API 和 Commons Lang 调用问题。

错误尝试或风险：

- OpenRewrite dry-run 会倾向删除 Springfox 依赖和相关代码，但 API Doc 模块可能存在用户可见功能差异。
- 如果直接删除 Springfox 分支，可能破坏仍依赖旧 Swagger 能力的使用方。

有效方案：

- API Doc 作为单独专项处理。
- 先梳理 Springfox 与 SpringDoc 的现有功能边界，再按 Boot 4 主版本策略移除 Springfox。
- Jackson 2 `JsonNode.fields()` 等问题需要结合 Jackson 3 API 单独验证。

验证证据：

- API Doc 低风险 API 迁移后，`JsonNode.fields()` 和 `Validate.notNull(...)` 复扫未命中。
- 本地 Jackson 2.21.3 字节码确认 `JsonNode` 同时提供 `fields()` 和 `properties()`；本地 Jackson 3.1.2 字节码确认 `JsonNode` 保留 `properties()` 且不再提供 `fields()`。
- `fist-boot-apidoc -am` deprecation 明细编译为 `BUILD SUCCESS`；测试为 `BUILD SUCCESS`，依赖链 `fist-kit-api` 共 6 个用例，失败 0。
- 剩余 API Doc deprecation 明细只来自 Springfox 相关类缺少 `@Deprecated` 注解，需要在 Springfox 去留决策中处理；编译日志另有依赖链 `fist-kit-api` 的 `ElementType.MODULE` 提示，不属于本轮 API Doc deprecation 收敛目标。
- 用户评审后选择移除 Springfox 支持；本轮删除 `springfox-oas` 依赖、Springfox 插件类、Springfox 自动配置类和自动配置注册入口。

后续复用规则：

- 任何用户可见文档能力的删除，都需要记录破坏性变更和替代路径；本轮替代路径为 SpringDoc 3。
- API Doc 专项必须同时更新代码影响文档和试编译文档。

## 经验 8：Jackson 2 到 3 的方法替换需要跨版本验证

问题：

API Doc 中 `DocParser` 使用 Jackson 2 `JsonNode.fields()` 解析 Swagger JSON。Jackson 3 已切换到 `tools.jackson.*` 包，并且 `JsonNode.fields()` 不再是可用 API。

错误尝试或风险：

- 只按 Jackson 2 deprecation 警告替换，可能选到 Jackson 3 不存在的中间 API。
- Context7 返回的 Jackson 3 文档偏通用迁移说明，没有直接给出 `fields()` 的替代项，不能据此直接下结论。
- 如果把 API Doc 中的 Springfox 相关代码和 Jackson 低风险替换混在一个提交里，会增加评审难度。

有效方案：

- 使用本地 Jackson 2.21.3 JAR 验证 `JsonNode.properties()` 已存在，可作为兼容替代项。
- 使用本地 Jackson 3.1.2 JAR 验证 `JsonNode.properties()` 仍存在，且 `fields()` 已不存在。
- 先替换 `fields()` 和 `Validate.notNull(T)`，再单独提交 Springfox 删除批次。

验证证据：

- `javap` 检查 `F:\pkg-maven\repository\com\fasterxml\jackson\core\jackson-databind\2.21.3\jackson-databind-2.21.3.jar`。
- `javap` 检查 `F:\pkg-maven\repository\tools\jackson\core\jackson-databind\3.1.2\jackson-databind-3.1.2.jar`。
- `rg -n "Validate\.notNull|\.fields\(" fist-kit-app/fist-boot-apidoc/src/main/java -g "*.java" -S` 复扫未命中。
- `fist-boot-apidoc -am` 编译和测试均为 `BUILD SUCCESS`。

后续复用规则：

- Jackson 迁移不能只做包名替换，需要确认替代 API 同时覆盖当前版本和目标版本。
- 文档能力模块要把「低风险 API 替换」和「用户可见能力删除」拆分提交。
- Springfox 移除需要同步更新 `CHANGELOG.md`、自动配置注册和升级经验文档。

## 经验 9：Feign/SDE 不能只验证基础 `@FeignClient`

问题：

项目包含 `fist-cloud-rpc-feign` 和 SDE Feign 示例。Spring 官方已发布维护的 Feign 组件，同时 Spring HTTP Service Clients 也是候选路线。

错误尝试或风险：

- 只验证基础 `@FeignClient` 编译，无法覆盖 SDE 的请求体重写、响应体重建、方法元数据和 envelope 处理。
- 直接迁移到 Spring HTTP Service Clients 可能缺少等价的 Encoder / Decoder 适配点。

有效方案：

- 短期先验证 Spring Cloud OpenFeign 5.x 延续方案。
- 中期单独设计 SDE 在 Spring HTTP Service Clients 下的等价适配层。
- Feign/SDE 专项需要用示例工程和端到端行为验证。
- Spring Cloud OpenFeign 5.x 的 `SpringEncoder` / `SpringDecoder` 不再接收 Boot 3 的 `HttpMessageConverters`，示例配置需要改为 `ObjectProvider<FeignHttpMessageConverters>`。
- SDE Feign 生产适配继续保留 `SecureFeignEncoder` / `SecureFeignDecoder`，在 OpenFeign `13.6` 下通过请求体重写、响应体重建和端到端示例验证。

验证证据：

- `docs/plans/spring-boot-4-research-report-0.2.md` 已将 Feign 与 SDE 作为评审修订项。
- `docs/plans/spring-boot-4-code-impact.md` 已列出 26 个 Feign 相关 Java 文件命中。
- Context7 `/spring-cloud/spring-cloud-openfeign` 文档确认 OpenFeign 仍支持按 `@FeignClient(configuration = ...)` 注入自定义 Feign 组件，并支持通过 bean / 属性覆盖 `Encoder`、`Decoder`。
- 依赖树确认当前组合为 `spring-cloud-openfeign-core:5.0.1`、`feign-core:13.6`、`feign-okhttp:13.6`。
- `fist-sde-client -am test`、`fist-cloud-rpc-feign -am test`、`examples/fist-sde/example-feign -am test` 均为 `BUILD SUCCESS`。

后续复用规则：

- Feign 迁移不能只看依赖坐标和注解。
- SDE 的编解码行为必须作为功能契约验证。
- Feign 示例和下游自定义配置需要从 Boot 3 `HttpMessageConverters` 迁移到 OpenFeign 5 的 `FeignHttpMessageConverters`。

## 经验 10：Boot 4 客户端和 Web 测试模块化会在 SDE 示例中连续暴露

问题：

SDE boot starter 和 SDE 示例同时覆盖 Spring MVC、RestClient、WebClient 和 OpenFeign。进入示例端到端验证后，Boot 4 模块化和包迁移会先于业务断言失败。

错误尝试或风险：

- 首次直接运行 `examples/fist-sde/example-feign -am test`，失败点并不是 Feign，而是 `fist-sde-boot-starter` 测试源码缺少 Boot 4 的 `AutoConfigureMockMvc` 位置。
- 只运行未 `clean` 的测试可能复用旧 target，掩盖主源码中 `RestClientCustomizer` / `WebClientCustomizer` 的真实编译问题。
- 将 `spring-boot-restclient` 作为 optional 依赖后，SDE boot starter 自身可编译，但下游 RestClient 示例在自动配置条件推断阶段出现 `NoClassDefFoundError: org/springframework/boot/restclient/RestClientCustomizer`。

有效方案：

- `AutoConfigureMockMvc` 迁移到 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`，并显式加入 `spring-boot-webmvc-test` 测试依赖。
- `RestClientCustomizer` 迁移到 `org.springframework.boot.restclient.RestClientCustomizer`，并将 `spring-boot-restclient` 作为 SDE boot starter 普通依赖传递给下游。
- Boot 4.0.6 本地 JAR 未提供 `WebClientCustomizer` 等价类；SDE WebClient 自动配置改为在缺少 `WebClient.Builder` 时提供已挂载 SDE filter 的 builder。
- 示例中的 `WebServerApplicationContext` 迁移到 `org.springframework.boot.web.server.context.WebServerApplicationContext`。

验证证据：

- 本地 Boot 4.0.6 JAR 确认 `AutoConfigureMockMvc` 位于 `spring-boot-webmvc-test`，`RestClientCustomizer` 位于 `spring-boot-restclient`，`WebServerApplicationContext` 位于 `spring-boot-web-server`。
- `fist-sde-boot-starter -am clean test-compile` 和 `fist-sde-boot-starter -am test` 均为 `BUILD SUCCESS`。
- `examples/fist-sde/example-restclient,examples/fist-sde/example-webclient -am test` 为 `BUILD SUCCESS`。

后续复用规则：

- Boot 4 模块化问题必须用 `clean test-compile` 暴露，不能只依赖增量测试。
- 公开自动配置方法返回类型所在 artifact 不能随意标记 optional，否则下游在条件推断阶段仍可能因类缺失失败。
- WebClient 相关迁移需要单独写明行为变化：Boot 4.0.6 没有 `WebClientCustomizer`，当前只在缺少 `WebClient.Builder` 时提供带 SDE filter 的 builder。

## 经验 11：HTTP Service Clients 不是 Feign 编解码器的直接替代

问题：

Spring Framework 7 的 HTTP Service Clients 通过 `@HttpExchange` 接口和 `HttpServiceProxyFactory` 生成代理，底层仍委托 `RestClient`、`WebClient` 或 `RestTemplate`。它不是 Feign `Encoder` / `Decoder` 的一对一替换点。

错误尝试或风险：

- 如果直接用 HTTP Service Clients 替换 Feign，会丢失 Feign `MethodMetadata`、`RequestTemplate` 和 `Response` 这些 SDE Feign 适配当前依赖的扩展点。
- 第一次新增 HTTP Service Clients 适配代码后，编译先被 `spring-javaformat:validate` 阻断，说明新增 Java 文件需要先按项目格式化规则处理。
- 为避免重复挂载拦截器补充列表去重逻辑后，`SdeRestClientAutoConfiguration` 与 `SdeWebClientAutoConfiguration` 再次触发格式校验失败，仍需先运行 `spring-javaformat:apply`。
- Boot 4.0.6 已提供 `RestClientCustomizerHttpServiceGroupConfigurer`，会把普通 `RestClientCustomizer` 应用到 RestClient-backed HTTP Service group；如果 SDE 再在 group configurer 中无条件追加同一个拦截器，可能出现重复加密。

有效方案：

- 短期继续保留 Spring Cloud OpenFeign 5.x 路线，不把 HTTP Service Clients 作为强制替代。
- 新增 `SecureHttpServiceClientContextProcessor`，通过 `HttpRequestValues.Processor` 读取 HTTP service 接口方法或接口类型上的 `@SecureExchange`，并写入请求属性。
- `SecureRestClientInterceptor` 和 `SecureWebClientExchangeFilterFunction` 优先读取请求属性中的 `SecureExchangeClientContext`；没有请求级上下文时继续使用构造器传入的默认上下文。
- SDE boot starter 通过 `RestClientHttpServiceGroupConfigurer` 和 `WebClientHttpServiceGroupConfigurer` 注册方法级上下文处理器；挂载拦截器/过滤器时使用列表去重，避免和 Boot 已应用的 builder customizer 重复。

验证证据：

- Context7 `/websites/spring_io_spring-framework_reference` 文档确认 HTTP Service Clients 使用 `HttpServiceProxyFactory`，并支持 `RestClientAdapter`、`WebClientAdapter` 和 `RestTemplateAdapter`。
- Spring Boot Context7 `/spring-projects/spring-boot/v4.0.0` 文档确认 HTTP Service group 可通过 `RestClientHttpServiceGroupConfigurer` / `WebClientHttpServiceGroupConfigurer` 自定义。
- 本地 `spring-web:7.0.7` JAR 确认 `HttpRequestValues.Processor` 可以接收 `Method` 并写入请求属性，`RestClientAdapter` 和 `WebClientAdapter` 均支持请求属性。
- 本地 `spring-boot-restclient:4.0.6` JAR 确认存在 `RestClientCustomizerHttpServiceGroupConfigurer`。
- `fist-sde-client -am test` 为 `BUILD SUCCESS`，新增 `SecureHttpServiceClientContextProcessorTest` 3 个用例，模块自身测试共 11 个用例，失败 0。
- `fist-sde-boot-starter -am test` 为 `BUILD SUCCESS`，模块自身测试共 29 个用例，失败 0。
- SDE RestClient / WebClient 示例测试为 `BUILD SUCCESS`，两个示例各 1 个用例，失败 0。
- HTTP Service Clients 相关模块与示例的 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- HTTP Service Clients 适配优先复用 RestClient/WebClient 底层拦截能力，只补方法级上下文传递。
- 对 Boot 自动配置扩展点要先查本地 JAR，确认是否已有全局 customizer 到 group configurer 的桥接，避免重复挂载同一拦截器。
- 若后续需要完整替换 Feign，需要另起迁移方案，不能把当前 HTTP Service Clients 支持视为等价替换完成。

## 经验 12：文档和提交需要与升级批次同步

问题：

Spring Boot 4 升级周期长，单次批次容易只提交代码，遗漏调研结论、风险和验证证据。

错误尝试或风险：

- 如果只看提交历史，无法知道某些错误尝试和命令修正过程。
- `.codex/` 日志不提交，后续读者需要从正式文档理解验证范围。

有效方案：

- 每个升级批次至少同步更新：
  - `spring-boot-4-code-impact.md`
  - `spring-boot-4-trial-compile.md`
  - 本文档
- 每个批次保持小提交，提交信息说明迁移对象。

验证证据：

- 已形成独立提交：空值注解迁移、Gateway/OAuth2 Commons Lang 迁移、OAuth2 LOB 迁移、Web/Data Commons Lang 迁移。
- 当前分支使用小步提交记录可回溯升级过程。

后续复用规则：

- 新批次完成后，先补文档，再提交。
- 错误命令、未覆盖测试和剩余风险必须写入文档。

## 经验 13：MyBatis Plus Boot 4 starter 是公开依赖坐标变化

问题：

MyBatis CRUD 在 Spring Boot 4 下继续使用 `mybatis-plus-spring-boot3-starter` 时，运行期无法创建 MyBatis mapper，根因是旧 starter 的自动配置顺序仍指向 Boot 3 的 `DataSourceAutoConfiguration` 包名。

错误尝试或风险：

- 只看 `mybatis-plus.version` 仍为 `3.5.16`，容易误判为没有用户可见依赖变化。
- 如果下游显式声明 Boot 3 starter 或锁定 `mybatis-spring` 3.x，升级到 Spring Framework 7 后仍会出现自动配置顺序或类型组合问题。

有效方案：

- `fist-boot-crud-mybatis` 使用 `mybatis-plus-spring-boot4-starter`。
- 测试依赖使用 `mybatis-plus-spring-boot4-starter-test`。
- 根 `mybatis-spring.version` 使用 `4.0.0`。
- `CHANGELOG.md` 记录该公开依赖坐标变化。

验证证据：

- Context7 `/baomidou/mybatis-plus-doc` 文档确认 Spring Boot 4 及以上版本使用 `mybatis-plus-spring-boot4-starter`，该 starter 自 MyBatis Plus `3.5.13` 开始提供。
- 依赖树验证显示当前模块解析到 `mybatis-plus-spring-boot4-starter:3.5.16`、`mybatis-plus-spring-boot4-starter-test:3.5.16` 和 `mybatis-spring:4.0.0`，未解析到 Boot 3 starter。
- `fist-boot-crud-mybatis` `test-compile` 为 `BUILD SUCCESS`。
- `QueryTest`、`UpdateTest`、`AbstractNodeIdxSupportTest` 定向测试共 12 个用例，失败 0。

后续复用规则：

- 依赖版本号不变不代表没有公开依赖变化，starter artifactId 变化必须写入发布说明。
- Boot starter 迁移需要同时检查生产依赖、测试依赖和自动配置顺序。
- 下游升级手册需要明确 Boot 3 starter 到 Boot 4 starter 的替换规则。

## 经验 14：数据库矩阵应先拆成本地可执行层和真实驱动层

问题：

OAuth2 LOB 迁移需要验证数据库行为，但当前仓库没有 MySQL、PostgreSQL、达梦的本地服务或 Testcontainers 依赖。如果直接把所有数据库列为同一批验收项，会阻塞已经可以本地验证的 Spring JDBC 绑定行为。

错误尝试或风险：

- 第一次运行 `DefaultAuthorizationServiceTest` 时只传 `-Dtest=DefaultAuthorizationServiceTest`，上游模块没有同名测试，Surefire 报告 `No tests matching pattern`。带 `-am` 的定向测试需要同时传 `-Dsurefire.failIfNoSpecifiedTests=false`。
- 新增 H2 集成测试后直接运行目标测试，先被 `spring-javaformat:validate` 拦截。测试代码也必须先按项目格式化规则处理。
- 如果只记录「MySQL / PostgreSQL / 达梦待验证」，会遗漏已经通过的 BLOB/CLOB 本地证据，也无法区分代码问题和环境缺口。

有效方案：

- 第一层使用 H2 做本地可执行验证：复用 Spring Authorization Server 7 内置 `oauth2-authorization-schema.sql`，覆盖 BLOB 写入、读取和 access token 查询。
- 第二层使用 H2 schema 变体验证 CLOB 分支：将官方 schema 中 `blob DEFAULT NULL` 替换为 `clob DEFAULT NULL`，覆盖 `SqlCharacterValue` 和 `ResultSet#getString(...)`。
- 第三层保留真实驱动矩阵：MySQL、PostgreSQL、达梦需要外部数据库或 Testcontainers 决策后再执行。

验证证据：

- Spring Authorization Server `7.0.5` JAR 内置 `org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql`。
- 官方 schema 注释明确：PostgreSQL 需要将 `blob` 改为 `text`、`timestamp` 改为 `timestamptz`；MySQL 需要在 JDBC URL 增加时间语义相关参数。
- `DefaultAuthorizationServiceTest` 共 4 个用例，失败 0；H2 BLOB schema 和 CLOB schema 变体均通过。
- `fist-boot-security-oauth2-server -am test` 为 `BUILD SUCCESS`，目标模块自身共 7 个用例，失败 0。
- `fist-boot-security-oauth2-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 数据库兼容性验证应先交付本地可执行证据，再明确真实驱动矩阵的环境前置条件。
- Maven reactor 定向测试使用 `-am` 时，若指定 `-Dtest`，同时传 `-Dsurefire.failIfNoSpecifiedTests=false`。
- 数据库 schema 迁移结论必须引用官方 schema、驱动文档或实际测试日志，不能只凭 SQL 类型名称推断。

## 经验 15：Gateway / Alibaba 验证要区分 starter 坐标、adapter 类型和实际使用点

问题：

Spring Cloud Gateway 5 在 Boot 4 下使用新的 WebFlux server starter 坐标。Spring Cloud Alibaba `2025.1.0.0` 虽然可解析，但当前仓库只通过 Feign 模块引入 Sentinel starter，并没有引入 Sentinel Gateway adapter 或 Nacos starter。

错误尝试或风险：

- 只看到 Spring Cloud Alibaba BOM 管理 Sentinel / Nacos 版本，容易误判为 Gateway、Sentinel Gateway 和 Nacos 都已通过运行级验证。
- 依赖树显示 `sentinel-spring-webmvc-v6x-adapter:1.8.9`，这只能说明 Feign / WebMVC 侧 Sentinel starter 可解析，不能代表 Gateway adapter 通过。
- 第一次复扫 `RequestTemplate.method("...")` 时，PowerShell 将双引号正则中的 `|` 当作管道解析。包含管道符的 `rg` 正则在 PowerShell 中应使用单引号。
- OpenFeign 测试迁移 `RequestTemplate.method(String)` 后，首次定向测试被 `spring-javaformat:validate` 拦截；新增 import 后仍需先执行格式化。

有效方案：

- 用依赖树同时包含新旧 Gateway starter、LoadBalancer、Sentinel starter、Sentinel Gateway adapter、Nacos client，明确实际解析结果。
- Gateway 5 WebFlux 坐标以 Context7 `/spring-cloud/spring-cloud-gateway/v5.0.0` 和 Maven 依赖树共同确认。
- Spring Cloud Alibaba 只声明当前已验证范围：`spring-cloud-starter-alibaba-sentinel:2025.1.0.0` + `sentinel-spring-webmvc-v6x-adapter:1.8.9` 可解析，Feign 模块测试通过。
- OpenFeign 13.6 中 `RequestTemplate.method(String)` 迁移到 `RequestTemplate.method(Request.HttpMethod)`。

验证证据：

- Gateway 模块解析到 `spring-cloud-gateway-server-webflux:5.0.1` 和 `spring-cloud-starter-gateway-server-webflux:5.0.1`，未解析到旧 `spring-cloud-gateway-server` / `spring-cloud-starter-gateway`。
- LoadBalancer 解析到 `spring-cloud-loadbalancer:5.0.1` 和 `spring-cloud-starter-loadbalancer:5.0.1`。
- Feign 模块解析到 `spring-cloud-starter-alibaba-sentinel:2025.1.0.0` 和 `sentinel-spring-webmvc-v6x-adapter:1.8.9`，未解析到 `spring-cloud-alibaba-sentinel-gateway`。
- 组合 clean compile 为 `BUILD SUCCESS`。
- 最终组合 test 为 `BUILD SUCCESS`：`fist-gateway-auth-core` 14 个用例、`fist-cloud-gateway-acl` 6 个用例、`fist-cloud-rpc-feign` 7 个用例失败 0。
- 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- Cloud 生态依赖验证需要把「BOM 准入」「依赖树解析」「模块测试」「示例或应用上下文启动」分开记录。
- Sentinel WebMVC adapter、Sentinel Gateway adapter、Nacos Discovery 是不同验证对象，不能互相替代。
- 对 OpenFeign 过时 API，优先解包本地 sources JAR 或查看 Javadoc，按明确替代项迁移。

## 当前剩余高优先级事项

1. 评估是否需要补一个最小 Gateway 启动类或示例，验证完整 Boot 4 Gateway 应用上下文。
2. 如下游实际使用 Sentinel Gateway 或 Nacos Discovery，单独建立对应示例或测试模块。
3. OAuth2 LOB 真实驱动矩阵：如决定引入测试环境，再补充 MySQL、PostgreSQL、达梦验证。
