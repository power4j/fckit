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

## 经验 16：Gateway 启动验证要补齐应用侧契约，而不是只验证 starter

问题：

依赖树和模块单元测试只能证明 Gateway 5 WebFlux starter 可解析、过滤器类可编译，不能证明项目 ACL 配置能在 Boot 4 Reactive Web Server 中启动。`RouteGuardConfiguration` 会导入鉴权过滤器和 OAuth2 配置，实际启动时依赖应用提供权限定义服务与用户 introspection 能力。

错误尝试或风险：

- 第一次新增启动测试使用 `assertThat(applicationContext).hasSingleBean(...)`，当前 AssertJ 对 `ApplicationContext` 只得到普通 `ObjectAssert`，编译失败。上下文 bean 数量断言改为 `applicationContext.getBeansOfType(...).hasSize(1)`。
- 第二次启动测试失败于 `ReactivePermissionDefinitionService` 缺失。这是有效失败，说明 ACL 配置不是纯 starter 自包含能力。
- 如果只在测试中关闭鉴权配置，会掩盖下游应用必须提供权限定义服务的契约，不适合作为升级验收证据。

有效方案：

- 使用 Boot 4 文档确认的 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 和 `@LocalServerPort` 做真实 Reactive Web Server 启动测试。
- 测试应用显式导入 `RouteGuardConfiguration`。
- 测试应用显式提供最小 `ReactivePermissionDefinitionService<PermissionDefinition>`，返回空权限列表。
- 测试应用显式提供 `UserIntrospectClient`，使用空 `ReactiveOpaqueTokenIntrospector`，避免最小启动测试依赖外部 OAuth2 introspection 配置。
- 断言 `ApiGuardFilter`、`RequestIdGlobalFilter` 和 Gateway 5 的 `RoutePredicateHandlerMapping` 进入上下文。

验证证据：

- Context7 `/spring-projects/spring-boot/v4.0.3` 确认随机端口运行服务器测试方式。
- `RouteGuardConfigurationStartupTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- `fist-cloud-gateway-acl -am test` 为 `BUILD SUCCESS`，目标模块自身 7 个用例，失败 0。
- `fist-cloud-gateway-acl -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- Starter 坐标验证、模块单元测试和最小应用上下文启动应作为三个层级分别记录。
- 对自动配置或手工 `@Import` 配置，启动测试要保留真实应用侧契约，只补最小必要 bean，不通过关闭关键配置换取启动成功。
- Gateway 启动通过不等于 Sentinel Gateway adapter 或 Nacos Discovery 通过；这些仍需要单独测试对象。

## 经验 17：未实际使用的生态集成应移除，而不是继续验证兼容性

问题：

此前因为 `fist-cloud-rpc-feign` 声明了 Sentinel starter，调研将 Spring Cloud Alibaba、Sentinel Gateway adapter 和 Nacos Discovery 视为待验证对象。用户补充 Sentinel 应该没有实际使用后，复查发现当前仓库没有 Sentinel / Nacos 源码、注解、配置或 API 使用点。

错误尝试或风险：

- 只看 BOM 和 starter 声明会把「未使用的传递依赖」误判为「必须升级验证的功能」。
- 如果继续为 Sentinel Gateway adapter 和 Nacos Discovery 建运行级验证，会扩大升级范围，并为未使用能力引入维护成本。
- 未跟踪的 `.flattened-pom.xml` 可能保留旧依赖内容，不能作为实际源码使用点依据；应以 Git 跟踪的 POM 和源码为准。

有效方案：

- 使用 `rg` 复扫 Sentinel / Nacos / Spring Cloud Alibaba 的 Java、XML、YAML、properties 和 Markdown 使用点。
- 移除 `fist-cloud-rpc-feign` 的 `spring-cloud-starter-alibaba-sentinel`。
- 移除根 `spring-cloud-alibaba.version` 属性和 `fist-kit-build` 中的 Spring Cloud Alibaba BOM。
- 将 Sentinel Gateway / Nacos Discovery 从本仓库 Boot 4 高优先级验证项中移除；下游如果实际使用，再由下游应用显式引入并验证。

验证证据：

- Sentinel / Nacos 复扫未发现 Git 跟踪源码、配置或主 POM 使用点。
- 移除 Sentinel starter 后，Feign 模块依赖树未解析到 Sentinel 相关依赖。
- 移除 Spring Cloud Alibaba BOM 后，Cloud 组合依赖树未解析到 Alibaba / Sentinel / Nacos 相关依赖，命令为 `BUILD SUCCESS`。
- `fist-cloud-rpc-feign -am test` 为 `BUILD SUCCESS`，目标模块自身 7 个用例，失败 0。
- `fist-cloud-rpc-feign` + `fist-cloud-gateway-acl` 组合测试为 `BUILD SUCCESS`：Gateway Auth Core 14 个用例、Gateway ACL 7 个用例、Feign 7 个用例失败 0。
- 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 对大版本升级中的生态依赖，先确认实际使用点，再决定是升级验证还是删除。
- 删除未使用 starter 后，必须用依赖树证明传递依赖已消失，并用相关模块测试证明行为不依赖该 starter。
- `.flattened-pom.xml` 如未被 Git 跟踪，只作为本地生成物观察，不作为提交对象。

## 经验 18：第三方废弃 API 要先区分废弃重载和废弃类型

问题：

`fist-redisson` 在 Redisson `4.3.1` 下仍调用 `RBlockingQueue.subscribeOnElements(Consumer<V>)`。该重载已废弃，但 `RBlockingQueue` 本身和同名 `Function<V, CompletionStage<Void>>` 重载仍可用。

错误尝试或风险：

- Context7 Redisson 文档只返回了普通 `RBlockingQueue` 和 `RReliableQueue` 示例，没有直接给出 `subscribeOnElements` 的废弃替代项。若只根据文档中出现 `RReliableQueue` 就迁移队列类型，会改变当前阻塞队列订阅语义。
- 第一次新增测试时，测试桩误用 `RedisNamespace#getPrefix()`，编译失败。真实 API 是 `getValue()`。
- GREEN 阶段第一次运行被 `spring-javaformat:validate` 拦截，说明生产代码 import 顺序仍需按项目格式插件处理。
- 测试回调实际执行时，嵌套消费类和消费方法的可见性会影响 `ReflectionUtils.invokeMethod(...)`。方法改为 public 后仍失败，原因是嵌套类本身也不是 public。测试夹具需要模拟真实可访问 bean。
- 测试中直接验证废弃 `Consumer` 重载会让测试源码本身产生 deprecation 警告，最终保留对 `Function` 重载的捕获验证即可。

有效方案：

- 先用 Context7 查询 Redisson 文档；文档未覆盖目标 API 时，再检查本地 `redisson-4.3.1.jar`。
- 使用 `javap -v org.redisson.api.RBlockingQueue` 确认只有 `subscribeOnElements(Consumer<V>)` 带 `@Deprecated`，`subscribeOnElements(Function<V, CompletionStage<Void>>)` 未废弃。
- 为 `QueueConsumerBeanProcessor` 增加单元测试，mock `RBlockingQueue`，捕获 `Function<Object, CompletionStage<Void>>` 回调，并执行回调确认 `@QueueConsumer` 方法仍被调用。
- 生产代码改为 `queue.subscribeOnElements(obj -> { ...; return CompletableFuture.completedFuture(null); })`，保持原有同步处理逻辑。

验证证据：

- RED：`QueueConsumerBeanProcessorTest` 期望调用 `Function<Object, CompletionStage<Void>>` 重载，当前代码调用 `Consumer` 重载，测试失败。
- GREEN：迁移后定向测试为 `BUILD SUCCESS`，`QueueConsumerBeanProcessorTest` 1 个用例，失败 0。
- `fist-redisson -am test` 为 `BUILD SUCCESS`，目标模块自身 1 个用例，失败 0。
- `fist-redisson -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，deprecation 明细日志未再出现 `subscribeOnElements` 或过时提示。
- `fist-redisson -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 第三方 API deprecation 不等于功能整体废弃；先定位到类、方法和重载级别。
- 文档检索没有命中具体替代项时，应检查当前依赖版本的本地 JAR、sources JAR 或 Javadoc。
- 单元测试应验证可观察行为和目标 API 形态，避免测试代码自身继续引用废弃 API。

## 经验 19：自有废弃 API 在主版本升级中应优先删除

问题：

`OAuth2ResourceOwnerBaseAuthenticationProvider` 仍暴露废弃字段 `refreshTokenGenerator` 和废弃方法 `setRefreshTokenGenerator(...)`。该入口绕过 Spring Authorization Server 的 `OAuth2TokenGenerator`，属于历史扩展点。

错误尝试或风险：

- 第一次 RED 运行被 `spring-javaformat:validate` 拦截，原因是测试新增 import 后未格式化。
- 删除生产代码后第一次 GREEN 运行再次被格式插件拦截，说明删除 import 和代码块后仍需要执行格式化。
- 仅依赖 `-Dmaven.compiler.showDeprecation=true` 不一定能发现自有废弃 API；如果没有外部调用，编译日志不会把该字段和方法列出来。

有效方案：

- 使用 `rg` 复扫源码中的 `@Deprecated`、`setRefreshTokenGenerator` 和 `refreshTokenGenerator`。
- 使用 Context7 查询 Spring Authorization Server 当前文档，确认自定义 token 生成应通过 `OAuth2TokenGenerator` 或 `DelegatingOAuth2TokenGenerator`。
- 增加反射测试，要求 `OAuth2ResourceOwnerBaseAuthenticationProvider` 不再暴露带 `@Deprecated` 的字段或方法。
- 删除 `refreshTokenGenerator` 字段和 `setRefreshTokenGenerator(...)`，刷新令牌统一由 `this.tokenGenerator.generate(...)` 生成。

验证证据：

- RED：`OAuth2ResourceOwnerBaseAuthenticationProviderTest.shouldNotExposeDeprecatedMembers` 在旧实现下失败。
- GREEN：迁移后定向测试为 `BUILD SUCCESS`，`OAuth2ResourceOwnerBaseAuthenticationProviderTest` 2 个用例，失败 0。
- `fist-boot-security-oauth2-server -am test` 为 `BUILD SUCCESS`，目标模块自身 8 个用例，失败 0。
- `fist-boot-security-oauth2-server -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，本轮目标关键字 `setRefreshTokenGenerator` / `refreshTokenGenerator` 未再出现。
- `fist-boot-security-oauth2-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 主版本升级阶段，自有 `@Deprecated` API 应按「删除或给出保留理由」处理，不能只依赖编译器调用点告警。
- 对 Spring Authorization Server token 相关扩展，优先使用官方 `OAuth2TokenGenerator` 扩展点。
- 反射类测试适合防止公共废弃入口在迁移后残留。

## 经验 20：Javadoc 废弃说明必须同步 Java 级 `@Deprecated`

问题：

`Breaker` 和 `ValidateUtil#check(T, Class<?>...)` 已在 Javadoc 中声明 `@deprecated`，但生产代码缺少 Java 级 `@Deprecated` 注解。Boot 4 迁移阶段开启 deprecation 明细编译后，这类不一致会产生 `deprecated item is not annotated with @Deprecated` 编译提示。

错误尝试或风险：

- 只处理第三方废弃 API 会漏掉自有 API 的注解一致性问题。
- 仅看调用点 deprecation 日志容易误判；该问题来自 API 声明本身，不一定有外部调用点。
- 第一次 GREEN 阶段被 `spring-javaformat:validate` 拦截，说明新增注解后的源码仍需要经过项目格式插件。
- 新增测试直接引用 `Breaker` 这类废弃 API 会让测试源码产生「使用或覆盖了已过时的 API」提示。该提示来自测试意图本身，不代表生产代码仍有同类调用。
- 组合模块测试一度被 Maven Central BOM POM 下载失败阻断，并且失败结果被本地仓库缓存；需要使用 `-U` 或等待仓库更新间隔后重试。提升权限重跑时审批服务返回 503，本轮未继续绕过审批。

有效方案：

- 为 `Breaker` 类补充 `@Deprecated`。
- 为 `ValidateUtil#check(T, Class<?>...)` 补充 `@Deprecated`。
- 增加 `BreakerDeprecationTest`，要求类声明保留 Java 级废弃注解。
- 增加 `ValidateUtilDeprecationTest`，通过反射要求旧重载声明保留 Java 级废弃注解。
- 使用 `spring-javaformat:apply` 修复新增注解后的格式问题。

验证证据：

- RED：`BreakerDeprecationTest` 在旧实现下失败，原因是 `Breaker.class.isAnnotationPresent(Deprecated.class)` 为 `false`。
- RED：`ValidateUtilDeprecationTest` 在旧实现下失败，原因是旧 `check(Object, Class[])` 重载缺少 `@Deprecated`。
- GREEN：`BreakerDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- GREEN：`ValidateUtilDeprecationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- `fist-support-web` 和 `fist-support-data` 的 `spring-javaformat:apply` 均为 `BUILD SUCCESS`。
- 组合模块 `test` 首次运行失败原因是 Maven BOM POM 从 Maven Central 传输失败并被缓存，未进入编译或测试阶段。

后续复用规则：

- 对 Javadoc `@deprecated` 与 Java `@Deprecated` 不一致的问题，优先补齐注解；主版本阶段如决定删除 API，则应单独用反射测试防止残留。
- 测试废弃 API 注解时优先使用反射；如果测试必须直接引用废弃类型，需要在验证记录中说明该警告来自测试断言。
- Maven 依赖解析失败要与代码失败分开记录；被本地仓库缓存的下载失败应使用 `-U` 重试，并保留审批或网络阻断原因。

## 经验 21：重叠 Maven 反应堆验证不能并行执行

问题：

继续处理 `InTenant` 和 `AuditFiller` 的 Javadoc `@deprecated` 与 Java `@Deprecated` 不一致问题时，两个目标测试分别覆盖 `fist-boot-data` 和 `fist-boot-crud-mybatis`。后者依赖前者，两个 Maven `-am` 反应堆会同时写入 `fist-boot-data/target`。

错误尝试或风险：

- 第一次并行运行两个目标测试时，`AuditFillerDeprecationTest` 通过，但 `InTenantDeprecationTest` 的 GREEN 阶段出现 Surefire `ClassNotFoundException`。测试类已经编译到 `target/test-classes`，根因是两个重叠反应堆并发写同一目标目录。
- 尝试不带 `-am` 只跑 `fist-boot-data` 时，Maven 转而解析远端 `4.0.0-SNAPSHOT` 上游模块，因本地模块未 install 而失败。
- 普通权限下的组合 `test` 和 `spring-javaformat:validate` 又被 Maven Central BOM POM 下载失败阻断；需要在可联网环境下使用 `-U` 重新解析。

有效方案：

- 对有依赖重叠的 Maven 模块验证，改为顺序执行，或合并为一个反应堆命令执行。
- 保留 `-am`，让 Maven 使用当前工作区上游模块，而不是解析远端 SNAPSHOT。
- 对缓存的 BOM 下载失败，使用 `cmd /c mvn -U ...` 并在需要时提升权限执行。
- 为 `InTenant` 和 `AuditFiller` 补充 `@Deprecated`，并用声明测试锁定。

验证证据：

- RED：`InTenantDeprecationTest` 和 `AuditFillerDeprecationTest` 在旧实现下均失败，断言结果为 `expected: <true> but was: <false>`。
- GREEN：顺序重跑后 `InTenantDeprecationTest` 为 `BUILD SUCCESS`，1 个用例，失败 0；`AuditFillerDeprecationTest` 为 `BUILD SUCCESS`，1 个用例，失败 0。
- Data 组合模块测试在提升权限并加 `-U` 后为 `BUILD SUCCESS`：`fist-boot-data` 3 个用例、`fist-boot-crud-mybatis` 15 个用例失败 0。
- Data 组合 deprecation 明细编译为 `BUILD SUCCESS`，不再出现 `InTenant` / `AuditFiller` 的「未使用 @Deprecated 对已过时的项目进行注释」警告。
- Data 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 不要并行运行写入同一 `target` 目录的 Maven 反应堆。
- 大模块验证优先使用一个合并 `-pl ... -am` 命令；如果拆分执行，也要按依赖顺序串行执行。
- 单模块命令如果依赖未 install 的本仓库 SNAPSHOT，上游必须使用 `-am` 带入当前工作区。

## 经验 22：主版本阶段优先删除无实际使用的废弃注解入口

问题：

`InTenant` 已由 `TenantInvokeAspect` 对应的新租户调用模型替代，仓库内除旧切面自注册、声明级废弃测试和文档记录外没有实际业务使用。继续保留 `InTenantAspect` 会让 `fist-boot-data` 在 deprecation 明细编译中持续报告生产调用点，也会让 Boot 4 主版本升级继续携带旧 API。

错误尝试或风险：

- 只为 `InTenant` 补充 `@Deprecated` 能消除声明侧警告，但不能消除 `InTenantAspect` 使用废弃注解的生产调用点警告。
- `DataAutoConfigurationTest` 的 RED 阶段先被 `spring-javaformat:validate` 拦截；格式修复后，测试按预期失败，原因是应用上下文仍注册 bean 名称 `inTenantAspect`。
- 删除 `DataAutoConfiguration` 中的旧 bean 方法后，目标测试第一次 GREEN 又被 `DataAutoConfiguration.java` 格式校验拦截；需要先运行 `spring-javaformat:apply`。
- 下游应用如果仍使用 `@InTenant`，升级到当前主版本后会编译失败；迁移方案是改用当前保留的 `TenantInvokeAspect` 对应租户调用方式。

有效方案：

- 将 `DataAutoConfigurationTest` 从断言 `InTenantAspect` 存在改为断言 `TenantInvokeAspect` 存在，并断言 `inTenantAspect` 不存在。
- 删除 `DataAutoConfiguration#inTenantAspect()` 自动配置入口。
- 删除 `InTenantAspect`、`InTenant` 和已经失去意义的 `InTenantDeprecationTest`。
- 使用 `rg` 复扫 `fist-boot-data`，确认 `InTenant` / `InTenantAspect` 在模块内无残留引用。

验证证据：

- RED：格式修复后，`DataAutoConfigurationTest` 在旧实现下失败，失败原因为 `ApplicationContext` 仍存在 `inTenantAspect` bean。
- GREEN：删除旧入口和旧源码后，`DataAutoConfigurationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- 复扫：`rg -n "InTenant|InTenantAspect" fist-kit-app/fist-data/fist-boot-data` 未命中。
- 模块测试：`fist-boot-data -am test` 为 `BUILD SUCCESS`，`fist-support-data` 12 个用例、`fist-boot-data` 2 个用例失败 0。
- deprecation 明细编译：`fist-boot-data -am -DskipTests ... clean compile` 为 `BUILD SUCCESS`，不再出现 `InTenant` / `InTenantAspect` 相关提示。
- 格式校验：`fist-boot-data -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 主版本升级阶段，如果废弃 API 在仓库内无实际业务使用，优先删除入口，而不是长期保留 `@Deprecated`。
- 删除自动配置 bean 时，测试应同时覆盖「新 bean 存在」和「旧 bean 不存在」，避免只验证替代能力而遗漏旧入口残留。
- 格式插件拦截属于实施流程问题，应记录到经验文档，但不能与代码语义失败混为一类。

## 经验 23：自有废弃工具类不能只断开生产调用点

问题：

`Breaker` 是自有废弃异常工具类，`MessageConfiguration` 仍在启动时向它注入 `LocaleResolver` 和 `MessageSourceAccessor`。如果只删除 `MessageConfiguration` 中的静态 setter 调用，`Breaker` 会继续作为公开 API 存在，但运行时缺少消息源初始化，形成更隐蔽的迁移风险。Boot 4 主版本阶段应删除旧工具类和只服务旧工具类的自动配置入口。

错误尝试或风险：

- `BreakerDeprecationTest` 只能证明类声明带 `@Deprecated`，不能证明主版本迁移已经清理旧 API。
- `CommonConfigurationTest` 的 GREEN 阶段第一次被 `spring-javaformat:validate` 拦截，原因是删除 `@Import` 元素后 `CommonConfiguration.java` 格式需要重新应用。
- `rg` 复扫时如果不限制到 `src`，会命中 `target/surefire-reports` 和 `target/classes` 中的历史输出，容易误判源码仍有残留。
- 下游应用如果仍调用 `Breaker`，升级当前主版本后会编译失败；迁移方向是改用 `CommonErrors` / `MsgBundleRejectedException` 等当前异常模型。

有效方案：

- 新增 `BreakerRemovalTest`，通过 `Class.forName(...)` 断言旧 `Breaker` 类已移除。
- 新增 `CommonConfigurationTest`，通过反射断言 `CommonConfiguration` 不再导入 `MessageConfiguration`。
- 删除 `Breaker`、`BreakerDeprecationTest` 和 `MessageConfiguration`。
- 从 `CommonConfiguration` 的 `@Import` 中移除 `MessageConfiguration`。

验证证据：

- RED：`BreakerRemovalTest` 在旧实现下失败，原因是 `Breaker` 类仍可加载。
- RED：`CommonConfigurationTest` 在旧实现下失败，原因是 `@Import` 仍包含 `MessageConfiguration`。
- GREEN：`BreakerRemovalTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- GREEN：`CommonConfigurationTest` 定向测试为 `BUILD SUCCESS`，1 个用例，失败 0。
- Web 组合模块测试为 `BUILD SUCCESS`，`fist-support-web` 8 个用例、`fist-support-security` 20 个用例、`fist-boot-web-app` 1 个用例失败 0。
- deprecation 明细编译为 `BUILD SUCCESS`，不再出现 `Breaker` / `MessageConfiguration` 相关生产提示。
- Web 组合 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 删除废弃 API 时，同时清理只为该 API 服务的自动配置、声明级废弃测试和文档中的「剩余项」描述。
- 对类删除类迁移，优先使用字符串反射测试，避免测试源码直接引用即将删除的类型。
- 源码复扫要限定 `src` 或排除 `target`，防止 Maven 历史输出污染判断。

## 经验 24：外部资源验证用条件测试承载，资源值只走环境变量

问题：

OAuth2 授权服务的 LOB 读写和 Redisson 队列订阅已经通过本地 H2 / mock 测试，但 MySQL、PostgreSQL、达梦和 Redis 这类真实资源仍需要运行级验证。测试环境连接信息包含地址、账号和密码，不能写入仓库、文档示例、提交信息或测试源码。

错误尝试或风险：

- 如果把真实连接串写入测试注解、`application-*.yml` 或文档，后续提交会泄露测试环境。
- 真实数据库和 Redis 资源并非开发者本地必备资源，直接纳入默认测试会导致普通 `mvn test` 不稳定。
- 第一次新增 `ExternalJdbcAuthorizationServiceTest` 后，`spring-javaformat:validate` 拦截构建，说明新增测试文件也必须走项目格式插件。
- Redisson `4.3.1` 的 `SingleServerConfig#setPassword(String)` 继承自已废弃的 `BaseConfig#setPassword(String)`，测试源码直接使用会继续产生 deprecation 警告。
- PostgreSQL 16 首次真实验证失败，根因是 Spring Authorization Server 官方 schema 使用 `blob`，PostgreSQL 不支持该类型。
- Redis 7 首次真实验证已成功建立连接，但消费者没有在超时时间内收到消息。根因不是 Redis 连通性，而是测试写入的随机队列名与 `@QueueConsumer("test")` 实际订阅队列名不一致。
- 达梦首次真实验证已加载本地达梦 JDBC 驱动并进入连接阶段，但数据库返回认证失败；再次只调整用户名大小写后触发登录失败次数限制。该问题属于外部账号状态，不是当前测试入口、驱动加载或 Boot 4 代码问题。

有效方案：

- 新增条件集成测试，默认只编译，不连接外部资源。
- JDBC 验证仅在设置 `FIST_IT_JDBC_URL` 后启用，账号、密码、驱动类、驱动 JAR、建表开关和 LOB 比较函数都通过环境变量传入。
- Redis 验证仅在设置 `FIST_IT_REDIS_URL` 后启用，密码和 database 也通过环境变量传入。
- 测试只清理本次生成的授权 ID 或队列名，避免影响共享测试环境。
- Redisson 密码配置改用 `Config#setPassword(String)`，避开 `BaseConfig#setPassword(String)` 的废弃入口。
- Redis 真实验证使用测试专用 `NameGenerator`，将固定注解值映射到本次运行唯一队列名，避免共享队列污染，同时保证写入队列与订阅队列一致。
- PostgreSQL 初始化 schema 时，将 LOB 字段从 `blob` 转换为 `text`，将时间字段从 `timestamp` 转换为 `timestamptz`。

验证证据：

- `cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=ExternalJdbcAuthorizationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 1 个，跳过 1 个。
- `cmd /c mvn -pl fist-kit-infra/fist-redisson -am "-Dtest=ExternalRedisQueueConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 1 个，跳过 1 个。
- MySQL 8 真实环境验证为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0。
- PostgreSQL 16 首次真实环境验证因 `blob` 类型不兼容失败；加入 PostgreSQL schema 变体后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0。
- Redis 7 首次真实环境验证因队列名不一致失败；加入测试专用 `NameGenerator` 后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0。
- Redis 条件测试无环境变量回归为 `BUILD SUCCESS`，测试 1 个，跳过 1 个。
- 达梦首次真实环境验证因账号认证状态失败；更新测试凭据后重跑为 `BUILD SUCCESS`，测试 1 个，失败 0，跳过 0，覆盖 schema 初始化、授权写入、按 ID 读取和按 access token 查询。
- 本轮没有把达梦、MySQL、PostgreSQL 或 Redis 的真实地址、账号、密码写入仓库文件。

后续复用规则：

- 外部资源测试默认必须跳过，启用条件只能依赖环境变量或等价的本地注入机制。
- 文档只记录环境变量名、用途和测试前置条件，不记录任何真实资源值。
- 对共享数据库测试，清理语句必须限定到测试生成的数据，禁止清空表或改动全局 schema。
- 实际执行 MySQL、PostgreSQL、达梦和 Redis 验证前，先由实施人员在本地 shell 注入环境变量，再运行定向测试。
- 外部数据库返回账号锁定或登录失败次数限制后必须停止重试，先由数据库管理员恢复账号或提供新的测试账号。

## 经验 25：重新做废弃告警审计时，要先清理历史待办，再区分真实阻塞和 `clean compile` 假阻塞

问题：

在 Groovy/Spock 清理、Sentinel 依赖移除、真实数据库矩阵验证都已经完成后，历史调研文档里仍保留了多处「后续处理 Groovy」「后续补 MySQL / PostgreSQL / 达梦」「后续复查 Sentinel / Nacos」等旧阶段表述。如果继续顺着这些旧待办推进，会把已经完成的事项重新排成高优先级，浪费排查时间。

错误尝试或风险：

- 第一轮重新审计直接执行三组 `clean compile`，其中 `fist-boot-apidoc` 和 Web / Data 组合出现失败。若只看这轮结果，容易误判为新的 Boot 4 源码阻塞。
- Web / Data 组合失败日志表现为依赖模块 `target/classes` 下类文件 `NoSuchFileException`，这类失败混入了 reactor 清理副作用，不能直接归因到源码迁移。
- 在不复查源码现状的情况下继续沿用旧文档中的 `JsonNode.fields()`、`ObjectUtils.defaultIfNull(...)`、`Validate.notNull(...)`、`MutableObject.getValue()` 等待办，会把已经处理完的风险重新当作活跃问题。
- 如果不先复扫 Groovy、Sentinel / Nacos 和真实资源验证状态，评审会看到同一文档同时出现「已完成」与「待验证」两套互相冲突的结论。

有效方案：

- 先做一次当前态复核，确认仓库内 `*.groovy`、Groovy/Spock/gmavenplus 构建链路、Sentinel / Nacos 使用点和前述典型废弃 API 已不再命中。
- 第一轮 `clean compile` 只用于发现新的候选阻塞，不直接据此下结论。
- 第二轮改用不带 `clean` 的目标模块 `compile`，专门提取当前源码还能稳定复现的 `deprecated` / `warning`。
- 结合官方当前文档确认边界：Spring Boot Context7 文档 `/spring-projects/spring-boot/v4.0.3` 显示 `spring-boot-properties-migrator` 仍是配置项迁移的官方辅助工具，Jackson 3 已是 Boot 4 默认方向，因此后续重点应转向应用级启动与配置验证，而不是继续泛化代码层 API 扫描。

验证证据：

- `Get-ChildItem -Recurse -Filter *.groovy` 未命中；`rg` 复扫 `gmavenplus`、`spock`、`groovy-tables` 未命中。
- `rg` 复扫 `JsonNode.fields(`、`Springfox`、`ObjectUtils.defaultIfNull`、`Validate.notNull`、`MutableObject.getValue(`、`org.springframework.lang.Nullable`、`SecurityJackson2Modules`、`OAuth2AuthorizationServerJackson2Module`、`MappingJackson2HttpMessageConverter` 在当前主代码范围未命中。
- `cmd /c mvn -U -pl fist-kit-infra/fist-sde/fist-sde-client,fist-kit-infra/fist-sde/fist-sde-boot-starter,fist-kit-cloud/fist-cloud-rpc-feign -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" clean compile` 为 `BUILD SUCCESS`。
- `cmd /c mvn -pl fist-kit-app/fist-web/fist-support-web -am -DskipTests "-Dmaven.compiler.showWarnings=true" "-Dmaven.compiler.showDeprecation=true" compile`、`fist-support-data` 同类命令和 `fist-boot-apidoc` 同类命令均为 `BUILD SUCCESS`，日志未命中新的生产代码 deprecation 提示。
- 当前唯一稳定复现的编译告警来自 JDK 8 `release` 模块中 `package-info.java` 上的 `@NullMarked`，javac 报告「未知的枚举常量 `java.lang.annotation.ElementType.MODULE`」。

后续复用规则：

- 每完成一批升级工作后，必须回写文档，删除已经过时的「下一步」，否则后续审计会被历史状态污染。
- `clean compile` 失败只能作为候选线索，必须用不带 `clean` 的定向 `compile` 或等价最小验证二次确认。
- 升级中后期如果主代码废弃告警已基本清空，下一阶段应转向应用启动、自动配置装配、配置项迁移和跨模块组合验证。

## 经验 26：应用级验证优先补「最小自动配置上下文」，不要一上来就拉完整示例应用

问题：

当主代码编译和主要废弃 API 基本收敛后，Boot 4 升级的剩余风险会转向「自动配置是否还能装配」「注解导入路径是否漂移」「最小上下文能否启动」。如果直接上完整示例应用或完整 `@SpringBootTest` Web 容器，失败面会同时混入端口、数据源、Web 环境和其他模块依赖，排障成本偏高。

错误尝试或风险：

- API Doc 和 OAuth2 Server 模块此前都只有编译通过或功能级单元测试，没有最小自动配置装配证据。
- 新增这类测试时，第一次执行往往先被 `spring-javaformat:validate` 拦截；如果不把这类格式失败单独记录，很容易误判成 Boot 4 语义问题。
- 如果只依赖 `compile` 或已有功能级测试，评审无法直接判断 Boot 4 下 `AutoConfiguration.imports`、`@Import` 元注解和核心默认 bean 是否仍然正确。

有效方案：

- 优先使用 `ApplicationContextRunner` 或等价最小上下文测试，先验证自动配置类本身和注解导入路径。
- API Doc 模块验证 `SpringDocAutoConfiguration` 是否仍能创建 `OpenAPI`、`OperationCustomizer` 和预期的 `SecurityScheme`。
- OAuth2 Server 模块验证 `AuthorizationServerAutoConfiguration` 是否仍能创建 `OauthUserPropExporter`、`AuthenticationFailureHandler`、`LogoutSuccessHandler`、`AuthenticationConverter` 和 `OAuth2TokenGenerator`，并补充 `@Oauth2AuthorizationServer` 注解导入路径验证。
- 通过最小上下文先锁定装配问题，再决定是否继续扩大到完整 `@SpringBootTest` 或示例应用。

验证证据：

- `cmd /c mvn -pl fist-kit-app/fist-boot-apidoc -am "-Dtest=SpringDocAutoConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 1 个，失败 0。
- `cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=AuthorizationServerAutoConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 2 个，失败 0。
- 两个模块首次执行都先被 `spring-javaformat:validate` 拦截；分别运行目标模块 `spring-javaformat:apply` 后重跑通过。

后续复用规则：

- 应用级验证第一步优先补最小自动配置上下文，不直接跳到完整应用启动。
- `ApplicationContextRunner` 适合验证自动配置类、条件装配和注解导入；只有当这一层通过后，再增加更重的 `@SpringBootTest`。
- 格式插件拦截要单独记录为流程性问题，避免污染对 Boot 4 兼容性的判断。

## 经验 27：Gateway OAuth2 配置要单独验证条件装配，不要只靠手工 bean 启动测试

问题：

`RouteGuardConfigurationStartupTest` 为了验证 Gateway Reactive Web 上下文启动，手工提供了 `UserIntrospectClient`。这能覆盖 ACL 主配置和 Gateway 5 WebFlux 自动配置共同启动，但不会覆盖 `Oauth2Configuration` 根据 `fist.oauth2.resourceserver.opaque-token.*` 配置自动创建 `UserIntrospectClient` 的路径。

错误尝试或风险：

- 如果只看 Gateway 最小启动测试通过，容易误以为 OAuth2 opaque-token 配置项绑定和条件装配也已经通过。
- 如果直接调用真实 introspection 服务，测试会引入外部认证服务器依赖，不适合作为默认本地验证。
- `Oauth2Configuration` 同时包含普通 `WebClient` 和 `@LoadBalanced` `WebClient.Builder` 路径，需要至少确认 LoadBalancer 入口 bean 没有在 Boot 4 / Spring Cloud 2025.1 下失效。

有效方案：

- 使用 `ApplicationContextRunner` 单独装配 `Oauth2Configuration`。
- 分别验证未配置 `introspection-uri` 时不创建 `UserIntrospectClient`，配置 URI、client ID 和 client secret 后创建 `UserIntrospectClient`。
- 同时断言 `lbWebClientBuilder` 存在，确认 `lb://` introspection URI 的入口仍可用。
- 不发起真实 HTTP 请求，把远程认证服务器响应和用户信息解析留给后续外部环境联调。

验证证据：

- `cmd /c mvn -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am "-Dtest=Oauth2ConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 2 个，失败 0。
- `cmd /c mvn -pl fist-kit-cloud/fist-cloud-gateway/fist-cloud-gateway-acl -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 对手工提供 bean 的启动测试，要反向检查哪些自动配置路径被绕开了。
- 条件装配测试优先验证「未配置不创建」和「配置后创建」两个方向。
- 涉及外部服务的配置装配测试只验证本地 bean 创建；真实远程调用用单独的条件集成测试或环境联调承载。

## 经验 28：组合上下文测试要区分「自动配置提供」和「应用侧提供」

问题：

OAuth2 Server 自动配置最小上下文已经证明 `AuthorizationServerAutoConfiguration` 能创建默认 token 生成器和认证转换器，但它不负责创建 `DefaultAuthorizationService`。如果把授权服务缺失误判为自动配置缺陷，就会把应用侧职责错误迁移到 starter 中，扩大维护面。

错误尝试或风险：

- 只验证 `AuthorizationServerAutoConfigurationTest`，无法证明自动配置默认 bean 与应用侧 JDBC 授权服务能在同一个 Boot 4 上下文中共同工作。
- 如果直接上完整示例应用，失败面会同时混入 Web 容器、外部数据源、客户端注册和资源服务器配置，定位成本偏高。
- 第一次运行新增组合测试仍被 `spring-javaformat:validate` 拦截，说明测试扩展阶段仍要先接受项目格式门禁，而不是把格式失败归为 Boot 4 兼容问题。
- `DefaultAuthorizationService` 的真实数据库 LOB 兼容性已经由条件集成测试覆盖，组合上下文测试不应再次依赖外部数据库。

有效方案：

- 使用 `ApplicationContextRunner` 同时加载 `AuthorizationServerAutoConfiguration` 和测试侧应用配置。
- 测试侧显式提供 H2 `DataSource`、`JdbcTemplate`、测试 `RegisteredClientRepository` 和 `DefaultAuthorizationService`。
- 使用 Spring Authorization Server 官方 `oauth2-authorization-schema.sql` 初始化 H2 表结构。
- 在断言中同时检查 `OAuth2TokenGenerator`、`RegisteredClientRepository`、`ManagedOAuth2AuthorizationService`，并执行授权保存、按 ID 查询、按 access token 查询。

验证证据：

- `cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am "-Dtest=AuthorizationServerComposedContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`，测试 1 个，失败 0。
- `cmd /c mvn -pl fist-kit-app/fist-security/fist-boot-security-oauth2-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 自动配置测试先确认 starter 自己负责创建的 bean；组合测试再显式补齐应用侧必须提供的 bean。
- 组合上下文应优先使用内存数据库和官方 schema，先排除外部资源干扰。
- 已由条件集成测试覆盖的真实资源兼容性，不在默认组合测试中重复引入。

## 经验 29：JDK 8 release 模块不要使用包级 `@NullMarked`

问题：

部分 `fist-kit-infra` 模块仍要求 Java 8 release 兼容。此前为了统一 JSpecify 策略，将这些模块的 `package-info.java` 也迁移到了包级 `@NullMarked`。随后在 `javac --release 8` 编译时稳定出现「未知的枚举常量 `java.lang.annotation.ElementType.MODULE`」告警。

错误尝试或风险：

- 只看 JSpecify `1.0.0` 的 classfile 版本，容易误判为 Java 8 release 模块可以无条件使用包级 `@NullMarked`。
- `-Xlint:-classfile` 无法消除该告警。
- `-Xlint:none` 和 `-nowarn` 能压掉告警，但会同时隐藏真实编译告警，不适合作为 Boot 4 升级阶段的长期处理方式。
- 如果把该告警继续留在基线中，后续 deprecation / warning 审计会被稳定噪声污染。

有效方案：

- 对要求 `maven.compiler.release=8` 的模块，不在 `package-info.java` 上使用包级 `@NullMarked`。
- 保留方法参数、返回值、字段和泛型类型参数上的显式 `@Nullable`，继续表达关键可空契约。
- 非 Java 8 release 模块可以继续使用包级 `@NullMarked`，但新增时需要先确认模块编译 release。

验证证据：

- `javap` 复核 JSpecify `1.0.0` 的 `@NullMarked`，确认其 `@Target` 包含 Java 9 的 `ElementType.MODULE`。
- `fist-kit-api -am clean compile` 为 `BUILD SUCCESS`，日志未命中 `WARNING` 或 `ElementType.MODULE`。
- JDK 8 release 相关 infra 模块组合 `clean compile` 为 `BUILD SUCCESS`，日志未命中 `WARNING` 或 `ElementType.MODULE`。
- 目标模块 `spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 空值注解迁移前先确认模块的 `maven.compiler.release`，不要把包级默认非空策略跨 release 机械复用。
- Java 8 release 模块优先使用显式 `@Nullable` 表达边界契约，不使用包级 `@NullMarked`。
- 不用全局关闭 warning 的方式消除升级噪声；优先移除触发源或缩小处理范围。

## 经验 30：示例应用验证前先确认 Maven reactor 边界

问题：

`examples/fist-sde` 是更接近真实应用的 Boot 4 验证入口，但它不在根 `pom.xml` 的 reactor 中。直接用 `-f examples/fist-sde/pom.xml test` 执行时，Maven 只构建示例 reactor，无法自动构建主工程里的 `4.0.0-SNAPSHOT` 组件。

错误尝试或风险：

- 第一次直接执行示例测试失败，原因是 `fist-sde-boot-starter`、`fist-sde-client` 和 `fist-sde-extra` 本地快照组件无法解析。
- 如果把该失败当作 Boot 4 兼容问题，会误判示例应用启动状态。
- 安装本地依赖后，测试才暴露真实代码问题：`example-server` 仍引用 Boot 3 的 `WebServerApplicationContext` 旧包。

有效方案：

- 对不在根 reactor 的示例工程，先安装其依赖的本地快照组件，再运行示例 reactor 测试。
- 示例应用编译失败时，先区分「本地快照依赖未安装」和「Boot 4 API 迁移缺失」。
- `WebServerApplicationContext` 在 Boot 4 中使用 `org.springframework.boot.web.server.context.WebServerApplicationContext`。

验证证据：

- `fist-sde-boot-starter -am -DskipTests install` 为 `BUILD SUCCESS`。
- 迁移 `SdeServerDemoRunner` import 后，`examples/fist-sde` 整组测试为 `BUILD SUCCESS`。
- server、feign、restclient、webclient 四个示例测试各运行 1 个用例，失败 0。

后续复用规则：

- 应用级样例验证需要先确认样例是否在当前 Maven reactor 内。
- 对独立示例工程，优先使用「安装本地快照组件 + 运行示例 reactor」的两步验证。
- 包迁移问题要和同组已通过示例对照，优先做局部一致性修复。

## 经验 31：自动配置候选测试要隔离当前模块资源

问题：

`fist-boot-security` 的自动配置注册文件误注册了 Web 模块自动配置，没有注册自身的 `InnerSecurityConfiguration`。新增测试依赖后，`ImportCandidates.load(...)` 会读取整个测试 classpath 上的所有 `AutoConfiguration.imports`，导致依赖模块的候选也进入断言结果。

错误尝试或风险：

- 第一个 RED 测试正确暴露了 `InnerSecurityConfiguration` 缺失。
- 修正后新增 Web/Data 组合测试依赖，导入候选断言又看到 `fist-boot-web-app` 和 `fist-boot-data` 的自动配置候选。
- 如果继续用全 classpath 候选断言「不包含 Web 自动配置」，测试会把依赖模块的合法候选误判为当前模块的注册污染。
- 修正生产代码后首次 GREEN 被 `spring-javaformat:validate` 拦截，说明自动配置类注解变更后仍要按项目格式门禁处理。

有效方案：

- 对「当前模块是否正确写入自动配置资源」这类测试，使用只包含当前模块输出目录的 `URLClassLoader` 读取 `ImportCandidates`。
- 对「多个模块能否一起启动」这类测试，单独使用 `WebApplicationContextRunner` 加载明确的自动配置集合。
- `fist-boot-security` 应将 `InnerSecurityConfiguration` 注册为 `@AutoConfiguration`，并在 `AutoConfiguration.imports` 与 `spring.factories` 中只声明自身自动配置。

验证证据：

- RED：`InnerSecurityConfigurationImportsTest` 失败，自动配置候选缺少 `InnerSecurityConfiguration`。
- GREEN：隔离 ClassLoader 后导入候选测试通过。
- Web/Data/Security Servlet 组合上下文测试通过，覆盖 `TenantInvokeAspect`、`PasswordEncoder`、`UserDecoder`、`UserInfoSupplier` 和 3 个 filter / resolver 注册点。
- `fist-boot-security -am test` 为 `BUILD SUCCESS`。
- `fist-boot-security -am spring-javaformat:validate` 为 `BUILD SUCCESS`。

后续复用规则：

- 自动配置资源测试和组合上下文测试要分开：前者验证当前 artifact，后者验证多个 artifact 一起工作。
- 一旦测试 classpath 引入其他 starter，`ImportCandidates.load(...)` 的结果不能再直接等同于当前模块资源内容。
- Boot 4 迁移中发现自动配置文件复制错误时，应同时维护 `AutoConfiguration.imports` 和兼容保留的 `spring.factories`。

## 经验 32：配置项 migrator 只作为临时验证工具

问题：

Spring Boot 4 配置项迁移不只涉及代码编译，还可能涉及 `application.yml` / `application.properties` 中的废弃或重命名配置。Spring Boot 官方提供 `spring-boot-properties-migrator`，但该依赖会在运行时分析环境并临时迁移配置，不应长期留在生产依赖中。

错误尝试或风险：

- 如果验证后忘记移除 migrator，会给生产运行带来不必要的启动分析和临时迁移行为。
- `examples/fist-sde` 不在根 reactor 中，配置迁移验证仍要沿用「先确认本地快照依赖可解析」的示例验证前提。
- Maven `dependency:tree` 中 `-pl` 需要使用 artifactId 选择器 `:example-sde-server`；只写 `example-sde-server` 会找不到 reactor 项目。
- `-Dincludes=org.springframework.boot:spring-boot-properties-migrator` 在 `cmd /c mvn` 下必须加引号，否则冒号会导致 Maven 误解析插件前缀。

有效方案：

- 临时在目标应用或示例的 POM 中加入 `org.springframework.boot:spring-boot-properties-migrator`，scope 使用 `runtime`。
- 运行应用级启动测试，抓取 migrator 输出。
- 使用 dependency tree 确认 migrator 确实进入目标应用 runtime classpath。
- 验证完成后立即移除 migrator 依赖，只在文档中保留验证证据。

验证证据：

- `examples/fist-sde` 临时接入 migrator 后整组测试为 `BUILD SUCCESS`，四个示例测试各运行 1 个用例，失败 0。
- dependency tree 确认 `example-sde-server` runtime classpath 包含 `spring-boot-properties-migrator:4.0.6`。
- migrator 日志未出现 `deprecated`、`renamed`、`replacement` 或 `PropertiesMigration` 运行期诊断。
- 验证完成后，临时依赖已移除，工作区未保留 `examples/fist-sde/pom.xml` 变更。

后续复用规则：

- 配置项迁移验证必须在真实应用启动路径上运行，不能只做静态配置扫描。
- migrator 只作为升级期辅助依赖，不能提交到正式依赖中。
- 当前结论只覆盖仓库内 SDE 示例；下游业务应用仍需要用自己的配置单独运行 migrator。

## 当前剩余高优先级事项

1. 当前仓库内已有 SDE 示例完成配置项 migrator 验证，未发现 Boot 4 配置迁移诊断；下游业务应用仍需用实际配置单独验证。
2. 下一步优先补充更完整的 Web/Data/Security 示例应用或继续扩大现有示例的真实请求覆盖。
