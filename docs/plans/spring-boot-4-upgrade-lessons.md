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

## 经验 32：配置项 migrator 的价值主要在应用侧

问题：

Spring Boot 4 配置项迁移不只涉及代码编译，还可能涉及 `application.yml` / `application.properties`、环境变量、启动参数和外部配置中心中的废弃或重命名配置。Spring Boot 官方提供 `spring-boot-properties-migrator`，但该依赖会在运行时分析环境并临时迁移配置，不应长期留在生产依赖中。

错误尝试或风险：

- 如果验证后忘记移除 migrator，会给生产运行带来不必要的启动分析和临时迁移行为。
- `examples/fist-sde` 不在根 reactor 中，使用流程验证仍要沿用「先确认本地快照依赖可解析」的示例验证前提。
- 仓库示例通常只有有限配置，不能代表内部应用的完整运行配置；仅用示例验证容易低估配置迁移风险。
- Maven `dependency:tree` 中 `-pl` 需要使用 artifactId 选择器 `:example-sde-server`；只写 `example-sde-server` 会找不到 reactor 项目。
- `-Dincludes=org.springframework.boot:spring-boot-properties-migrator` 在 `cmd /c mvn` 下必须加引号，否则冒号会导致 Maven 误解析插件前缀。

有效方案：

- 临时在内部应用或示例的 POM 中加入 `org.springframework.boot:spring-boot-properties-migrator`，scope 使用 `runtime`。
- 优先在内部应用的真实启动路径运行，包括真实 `application.yml`、环境变量、启动参数和配置中心配置。
- 仓库示例只用于验证 migrator 接入、依赖确认、日志检查和依赖移除流程。
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
- 当前仓库结论只覆盖 SDE 示例的工具使用流程和示例基线；内部项目仍需要用实际配置单独运行 migrator。

## 经验 33：组合示例要覆盖真实 HTTP 请求

问题：

Web/Data/Security 的最小上下文测试可以证明自动配置类能装配，但不能证明真实 Servlet 容器、过滤器顺序、MVC 参数解析、Jackson 自动配置、数据库访问和安全上下文在同一次请求中能协同工作。Boot 4 升级后，这类风险需要用示例应用补齐。

错误尝试或风险：

- 第一次测试复用了 `ApplicationContextRunner` 风格的 `hasSingleBean` 断言，但 `@SpringBootTest` 注入的是普通 `ApplicationContext`，AssertJ 只提供 `ObjectAssert`，导致测试编译失败。
- 第二次测试忽略了 FIST Web 的 Jackson 数字转字符串模块，直接断言 `long id` 为数字，导致运行期断言失败。
- examples 父模块当前跳过 spring-javaformat 校验，格式命令通过不等于新增示例代码被格式插件实际检查。

有效方案：

- 在 `examples/fist-web-data-security/example-servlet` 中建立真实 Boot 4 Servlet 示例，而不是继续扩大最小上下文测试。
- 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 启动 Tomcat 11，通过 `RestClient` 发起 GET / POST 请求。
- GET 请求覆盖 H2 查询、`Pageable` 参数解析、请求 ID 读取、可信用户头解析和用户上下文读取。
- POST 请求覆盖 `ObjectValidator` 在真实请求中的调用。
- 自动配置 bean 断言使用 `applicationContext.getBeansOfType(...).hasSize(1)`，避免混用不同测试工具的断言扩展。

验证证据：

- RED：第一次测试编译失败，原因是误用 `hasSingleBean`。
- RED：第二次测试运行失败，原因是 `long id` 被 FIST Jackson 自动配置序列化为字符串。
- GREEN：`cmd /c mvn -l .codex/web-data-security-example-test-3.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am test` 为 `BUILD SUCCESS`，新示例测试 1 个用例，失败 0，错误 0，跳过 0。
- 格式命令：`cmd /c mvn -l .codex/web-data-security-example-format-validate.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am spring-javaformat:validate` 为 `BUILD SUCCESS`，但 examples 模块配置为跳过格式校验。

后续复用规则：

- 组合 starter 风险收敛后，应优先补真实请求示例，而不是继续堆叠最小上下文测试。
- 示例断言要尊重项目已有 Jackson、过滤器和参数解析约定，不能按裸 Spring Boot 默认行为假设响应形态。
- examples 模块的格式跳过配置需要在交付说明中单独说明，不能把它当作格式插件已实际检查新增示例源码。

## 经验 34：API Doc 示例验证要补齐 SpringDoc WebMVC 入口

问题：

`fist-boot-apidoc` 自身只声明 `springdoc-openapi-starter-common` 为 optional 依赖，最小上下文测试可以验证 `OpenAPI` bean 和 `OperationCustomizer`，但不能证明应用侧 `/v3/api-docs` endpoint 在 Boot 4 + SpringDoc 3 下真实可访问。

错误尝试或风险：

- 如果示例只引入 `fist-boot-apidoc`，应用侧未必具备 SpringDoc WebMVC endpoint。
- 只断言 `OpenAPI` bean 会漏掉 WebMVC starter、endpoint 暴露、路径生成和安全方案序列化问题。
- 本次首次加入 `springdoc-openapi-starter-webmvc-api:3.0.3` 时 Maven 从镜像仓库下载依赖，后续离线环境需要确认本地仓库已有该 artifact。

有效方案：

- 在组合示例中同时引入 `fist-boot-apidoc` 和 `springdoc-openapi-starter-webmvc-api`。
- 使用真实 HTTP 请求访问 `/v3/api-docs`，而不是只读取 `OpenAPI` bean。
- 断言 `components.securitySchemes.inner_auth` 中的 `type`、`in` 和 `name`，确认 FIST API Doc 自动配置的 `X-USER-IN` header 已进入最终 OpenAPI JSON。
- 断言示例 controller 的 `/api/profiles/{id}` 和 `/api/profiles` 路径已进入文档，确认 endpoint 真正扫描了应用路由。

验证证据：

- `cmd /c mvn -l .codex/web-data-security-apidoc-example-test.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am test` 为 `BUILD SUCCESS`，新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- `cmd /c mvn -l .codex/web-data-security-apidoc-example-format-validate.log -Pexamples -pl examples/fist-web-data-security/example-servlet -am spring-javaformat:validate` 为 `BUILD SUCCESS`，但 examples 模块配置为跳过格式校验。

后续复用规则：

- API Doc 的应用级验证应以 `/v3/api-docs` JSON 为准，最小上下文测试只作为自动配置入口的补充证据。
- 示例侧需要显式提供 SpringDoc WebMVC 或 WebFlux endpoint starter，基础库不应把该依赖强行变成传递必选依赖。
- 记录依赖下载行为，避免把首次联网解析误判为代码变更风险。

## 经验 35：OAuth2 Server 示例要接入真实 Authorization Server 端点

问题：

`fist-boot-security-oauth2-server` 的模块测试已覆盖自动配置 Bean、`DefaultAuthorizationService` 与 H2 schema，但缺少应用级 HTTP 请求。Boot 4 升级后，仅靠上下文测试不能证明 Spring Authorization Server 7 的 endpoint matcher、token endpoint converter、client authentication、授权保存和 token claims 回写能在真实 Servlet 容器中一起工作。

错误尝试或风险：

- 第一次测试编译失败，原因是将 metadata 响应中的 `grant_types_supported` / `token_endpoint_auth_methods_supported` 直接转为 `Iterable<?>` 后调用 AssertJ `contains(String)`，泛型捕获导致编译器无法匹配 varargs。
- 第二次测试启动前失败，原因是示例最终依赖树缺少 `commons-logging:commons-logging`，`SpringExtension` 初始化 `TestContextManager` 时无法加载 `org.apache.commons.logging.LogFactory`。
- `mvn dependency:tree -Dincludes=org.springframework:spring-jcl,commons-logging:commons-logging` 在 `cmd /c mvn` 下如果不加引号，会被 Maven 误解析成插件前缀。
- 如果只验证 `/.well-known/oauth-authorization-server`，仍不能证明 FIST 自定义 `OAuth2TokenGenerator` 参与 token 颁发。

有效方案：

- 新增 `examples/fist-oauth2-server/example-authorization-server`，使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 启动真实 Tomcat 11。
- 通过 `OAuth2AuthorizationServerConfigurer` 手工接入项目自动配置提供的 `OAuth2TokenGenerator` 和 `AuthenticationConverter`，而不是只导入官方最小默认配置。
- 使用 H2 和 Spring Authorization Server 7 官方 `oauth2-authorization-schema.sql`，示例中提供 `DefaultAuthorizationService`，使 HTTP token 请求落到 FIST 授权服务实现。
- `RegisteredClient` 显式使用 `OAuth2TokenFormat.REFERENCE`，确保 FIST 的 `DefaultOAuth2AccessTokenGenerator` 负责 access token 生成。
- 测试先请求 `/.well-known/oauth-authorization-server`，再通过 `client_secret_basic` 请求 `/oauth2/token`，最后用 `authorizationService.findByToken(...)` 回查 token claims。
- 示例 POM 显式声明 `commons-logging`，保证 Spring 7 测试上下文和应用启动具备 `LogFactory`。

验证证据：

- RED：`cmd /c mvn -l .codex/oauth2-server-example-test.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 编译失败，原因是 AssertJ 泛型断言不匹配。
- RED：`cmd /c mvn -l .codex/oauth2-server-example-test-2.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 运行失败，原因是缺少 `org.apache.commons.logging.LogFactory`。
- GREEN：`cmd /c mvn -l .codex/oauth2-server-example-test-3.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 为 `BUILD SUCCESS`，新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- 回归：`cmd /c mvn -l .codex/oauth2-server-example-test-4.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 为 `BUILD SUCCESS`，新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- 最终回归：`cmd /c mvn -l .codex/oauth2-server-example-test-5.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 为 `BUILD SUCCESS`，新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- 最终断言补强后回归：`cmd /c mvn -l .codex/oauth2-server-example-test-6.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am test` 为 `BUILD SUCCESS`，新示例测试 2 个用例，失败 0，错误 0，跳过 0。
- 格式命令：`cmd /c mvn -l .codex/oauth2-server-example-format-validate.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。
- 最终格式命令：`cmd /c mvn -l .codex/oauth2-server-example-format-validate-2.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。
- 最终断言补强后格式命令：`cmd /c mvn -l .codex/oauth2-server-example-format-validate-3.log -Pexamples -pl examples/fist-oauth2-server/example-authorization-server -am spring-javaformat:validate` 为 `BUILD SUCCESS`；examples 继承配置会跳过格式校验。

后续复用规则：

- OAuth2 Server 应用级验证至少覆盖公开 metadata 和一次 token 请求；只启动上下文不够。
- 自定义 token 生成器的验证应回查授权服务中的 token claims，而不是只检查 HTTP token 响应中的标准字段。
- Spring Authorization Server 7 示例应优先复用官方 `OAuth2AuthorizationServerConfigurer`，项目扩展通过 configurer 明确注入。
- Maven 命令中带冒号的 `-Dincludes` 等参数在 Windows `cmd /c mvn` 下必须加引号。

## 当前剩余高优先级事项

1. 当前仓库内已有 SDE 示例完成配置项 migrator 使用流程验证，未发现 Boot 4 配置迁移诊断；内部应用侧真实配置验证仍是独立工作。
2. Web/Data/Security/API Doc 与 OAuth2 Server 示例已经补齐真实 HTTP 请求覆盖；下一步可转入内部应用配置迁移验证，或继续补充更完整的 OAuth2 授权码 / OIDC 流程示例。

## 经验 36：应用侧配置迁移验证不应强依赖 Nacos

记录日期：2026-05-14；记录者：Codex。

问题：

内部应用的生产默认路径通过 `spring.config.import` 加载 Nacos 配置，但 Boot 4 配置项迁移验证需要频繁修改、启动和回看 migrator 诊断。如果每轮都先导入 Nacos，会把验证节奏绑定到远程配置中心，也容易让临时配置污染共享环境。

错误尝试或风险：

- 直接把 `docs/config-template/spring-config-server` 下的模板导入 Nacos 后再验证，会引入额外环境步骤；每次调整模板都需要重新发布配置。
- 只启动应用本地 `application.yml` / `features.yml` 不够，因为真实业务配置主要位于配置模板或配置中心，migrator 看不到这些配置时诊断不完整。
- 在公开交付文档中记录 `.env.local` 的真实值、Nacos namespace、group 或配置内容，会扩大敏感信息暴露面。

有效方案：

- 将应用侧配置迁移验证拆成「本地配置源优先」和「Nacos 回归」两段。
- 高频验证阶段优先使用 Spring Boot Config Data 的 `spring.config.import=optional:file:...`，直接导入配置模板目录中的 `application.yml` 和服务级 YAML。
- `config-server-spring` profile 属于遗留配置，本地没有 Spring Cloud Config Server，因此不作为当前可执行验证路径。
- 只有在本地配置源验证稳定后，再用 Nacos 做一次接近生产路径的回归验证。
- `spring-boot-properties-migrator` 仍只作为 runtime 临时依赖；处理完诊断后必须移除。

验证证据：

- Spring Boot 4.0.3 官方文档确认 `spring.config.import` 可导入 `optional:file:./dev.properties`，导入配置会参与环境构建，且导入文件优先级高于声明它的文件。
- Spring Boot 4.0.3 官方文档确认 `spring.config.import` 支持 `optional:` 前缀，适合本地验证时容忍缺失文件。
- 内部应用 `features.yml` 已存在 `config-server-spring` profile，但该配置为遗留路径，本地没有可用 Spring Cloud Config Server。
- 内部配置模板目录已存在 `application.yml`、`fist-auth.yml`、`fist-iam.yml`、`fist-iam-task.yml`、`fist-gateway.yml`、`fist-monitor.yml` 和 `hikaricp.yaml`。

后续复用规则：

- 应用侧 migrator 首轮验证不以 Nacos 为前置条件。
- 优先用本地 `file:` 导入跑出配置诊断，再决定是否需要 Nacos 回归。
- 不再投入 Spring Cloud Config Server native/Git 后端验证，除非内部项目重新启用该配置中心路径。
- 公开文档只记录配置文件类别、加载方式和诊断类别，不记录真实环境值、用户名、密码、连接串或配置内容原文。

## 经验 37：内部应用本地 file import 验证要用 start/stop 而不是 run

记录日期：2026-05-14；记录者：Codex。

问题：

内部应用首轮配置迁移验证需要确认 `fist-monitor` 可以在不导入 Nacos、不启动 Config Server 的情况下读取本地配置模板并完成 Spring 环境构建。直接使用 `spring-boot:run` 容易遇到 reactor 执行范围、内部依赖未安装和进程常驻问题。

错误尝试或风险：

- `spring-boot:run` 与 `-am` 同用时，Maven 会先在父 POM 上执行 run goal，父 POM 没有 main class，直接失败。
- 只对 `fist-monitor` 执行 run goal 时，内部依赖 `fist-common:9.2.0` 未安装到本地 Maven 仓库，Maven 会去远程仓库解析内部构件并失败。
- 关闭 Nacos 配置中心导入后，Nacos Config starter 仍会执行 import check；如果不显式关闭，会报缺少 `nacos:` import。
- `spring-boot:run` 会让应用常驻，靠超时中断容易留下 Java 子进程。
- 内部应用启动日志会打印环境变量摘要；接入真实 `.env.local` 后，原始日志不能直接进入公开交付文档。

有效方案：

- 先执行 `cmd /c mvn -pl common/fist-common -am "-P!config-nacos" -DskipTests install`，把内部依赖安装到本地 Maven 仓库。
- 再只对 `fist-monitor` 执行 `spring-boot:start spring-boot:stop`，并显式指定 main class。
- 使用 `boot4-local` profile，避免 `default` profile 激活 Nacos import。
- 通过 `spring.config.import=optional:file:...` 导入公共配置模板和 `fist-monitor` 服务配置模板。
- 同时设置 `spring.cloud.nacos.config.enabled=false`、`spring.cloud.nacos.config.import-check.enabled=false`、`spring.cloud.discovery.enabled=false`、`spring.cloud.nacos.discovery.enabled=false` 和 `spring.cloud.service-registry.auto-registration.enabled=false`。

验证证据：

- 内部依赖安装命令为 `BUILD SUCCESS`。
- `spring-boot:start spring-boot:stop` 命令为 `BUILD SUCCESS`，`MonitorApplication` 使用 `boot4-local` profile 启动并停止。
- 验证完成后复查 Java 进程，仅剩本次验证前已存在的 Java 进程，没有新增残留进程。

后续复用规则：

- 内部应用启动探测优先使用 `spring-boot:start spring-boot:stop`，不要用常驻 `spring-boot:run`。
- 首轮配置迁移验证先使用非 Web 模式确认环境构建；后续需要验证 Servlet 容器时，再改为 Web 模式并使用明确的启动/停止控制。
- 真实 `.env.local` 参与验证时，日志只保留脱敏后的配置迁移诊断和错误类别。

## 经验 38：命令行追加 migrator 要补齐传递依赖

记录日期：2026-05-14；记录者：Codex。

问题：

为了避免修改内部应用 POM，首轮尝试使用 Spring Boot Maven Plugin 的 `spring-boot.run.additional-classpath-elements` 临时追加 `spring-boot-properties-migrator`。该方式适合快速验证，但不会像 Maven dependency 一样自动把传递依赖加入应用 classpath。

错误尝试或风险：

- `mvn help:describe -Dplugin=org.springframework.boot:spring-boot-maven-plugin:3.5.11` 在 Windows `cmd /c mvn` 下未加引号时，插件坐标被错误解析；带冒号的 Maven 参数要加引号。
- 只追加 `spring-boot-properties-migrator` 主 jar 时，启动失败，缺少 `ConfigurationMetadataRepositoryJsonBuilder`。
- 同时追加 migrator 和 `spring-boot-configuration-metadata` 后，启动仍失败，缺少 `org.json.JSONObject`。
- Maven `-l` 只记录 Maven 主进程日志，不能完整记录 forked 应用日志；如果只看 Maven 日志，会漏掉应用侧 migrator 诊断或启动错误。

有效方案：

- 先用 `mvn help:describe` 确认 `run` goal 支持 `spring-boot.run.additional-classpath-elements`。
- 用 `mvn dependency:get` 准备 `spring-boot-properties-migrator`、`spring-boot-configuration-metadata` 和 `com.vaadin.external.google:android-json`。
- 在 `spring-boot:start spring-boot:stop` 中把三个 jar 一起追加到 `spring-boot.run.additional-classpath-elements`。
- 应用日志和控制台输出一起作为诊断来源；公开文档只记录脱敏后的诊断类别。

验证证据：

- 补齐三个 jar 后，内部 `fist-monitor` 在本地 `file:` 配置模板下启动/停止成功。
- 成功日志未出现 `PropertiesMigration`、`deprecated`、`renamed` 或 `replacement` 诊断。
- 该验证运行在 Boot 3.5.11 基线，只证明 migrator 接入机制和本地配置加载路径可用；Boot 4 诊断仍需在应用依赖切换到 Boot 4 后使用匹配版本重跑。

后续复用规则：

- 长期或多服务验证优先使用临时 POM runtime 依赖，减少手工 classpath 遗漏；命令行追加 jar 更适合一次性探测。
- 如果继续使用命令行追加 jar，必须把 migrator 传递依赖显式写入命令模板。
- 每轮应用日志采集前先确认是否加载真实环境变量，避免把 `.env.local` 值写入过程记录。

## 经验 39：内部应用 Boot 4 试编译要先收窄 reactor

记录日期：2026-05-14；记录者：Codex。

问题：

内部应用是多模块 Maven 工程。即使只想验证 `fist-monitor`，Maven 也会先读取 reactor 中所有模块的 POM。Boot 4 下部分旧 starter 坐标不再由父 POM 管理，其他模块的 POM 问题会在目标模块编译前阻塞构建。

错误尝试或风险：

- 只用 `-pl fist-cloud/fist-monitor -am` 不能避免 Maven 读取全 reactor POM。
- 直接将根 POM parent 切到 Boot 4 后，`fist-iam`、`fist-auth` 和示例模块中的 `spring-boot-starter-undertow` 缺少版本，构建在项目读取阶段失败，尚未进入 `fist-monitor`。
- 内部应用引用 FIST Kit `4.0.0-SNAPSHOT` 时，如果本地仓库没有安装对应 jar，Maven 会去远程仓库解析未发布组件并失败。
- 在真实内部项目上直接改 POM 做试编译，会混入大量临时变更，不利于复盘和回滚。

有效方案：

- 复制内部项目到主仓库 `target/` 下的临时副本，排除 `.git`、`.env.local`、`target`、`logs` 和 IDE 目录。
- 在临时副本中收窄 reactor：根模块只保留 `common`、`fist-cloud`，`fist-cloud` 只保留 `fist-monitor`。
- 将临时副本版本矩阵切到 Boot 4：Spring Boot `4.0.6`、Spring Cloud `2025.1.1`、Spring Cloud Alibaba `2025.1.0.0`、FIST Kit `4.0.0-SNAPSHOT`。
- 先在 FIST Kit v4 主仓库安装 `fist-kit-api` 和 `fist-support-web` 及其依赖，再编译内部临时副本。

验证证据：

- FIST Kit v4 定向安装 `fist-kit-api` 与 `fist-support-web` 为 `BUILD SUCCESS`。
- 内部临时副本中执行 `fist-monitor` 最小链路 `test-compile` 为 `BUILD SUCCESS`。
- 编译覆盖 `fist-common` 10 个源码文件和 `fist-monitor` 2 个源码文件。

后续复用规则：

- 首轮应用侧 Boot 4 试编译先收窄到单服务链路，避免全 reactor 旧模块阻塞目标问题定位。
- 全量应用升级前，需要统一处理所有模块的旧 Boot starter 坐标，尤其是 `spring-boot-starter-undertow`。
- SNAPSHOT 基础库必须先本地安装，或使用已发布版本；不能让内部应用试编译依赖远程解析未发布构件。

## 经验 40：Boot 4 启动探测先用非 Web 模式分离配置问题

记录日期：2026-05-14；记录者：Codex。

问题：

`fist-monitor` 作为首个内部应用样本，目标是先确认 Boot 4 版本矩阵、本地配置模板和 migrator 组合能否完成 Spring 环境构建。如果一开始就启动完整 Web 容器和管理端点，数据库、端口、模板目录、Nacos Discovery 等问题会和配置迁移诊断混在一起。

有效方案：

- 在临时副本中使用 `spring-boot:start spring-boot:stop`，并为每轮探测指定独立 JMX port。
- 启动参数设置 `--spring.main.web-application-type=none`，先验证非 Web 环境构建。
- 使用本地 `file:` 配置模板导入公共配置和服务配置。
- 显式关闭 Nacos Config import check、Nacos Config、Discovery 和服务自动注册。
- 追加 Boot 4.0.6 的 `spring-boot-properties-migrator` 和 `spring-boot-configuration-metadata`，执行配置迁移诊断。

验证证据：

- 不带 migrator 的 Boot 4 `fist-monitor` 启动/停止为 `BUILD SUCCESS`。
- 追加 Boot 4.0.6 migrator 后，启动/停止仍为 `BUILD SUCCESS`。
- 应用日志未出现 `PropertiesMigration`、`deprecated`、`renamed` 或 `replacement` 配置迁移诊断。
- 应用日志出现 Thymeleaf 模板目录缺失警告；该警告不是配置迁移诊断，暂不作为 Boot 4 阻塞项。

后续复用规则：

- 单服务 Boot 4 首轮启动探测先使用非 Web 模式，确认配置和自动配置主路径可启动。
- 非 Web 探测通过后，再开启 Web 模式验证 Servlet 容器、端口监听、Actuator 和管理端点。
- migrator 诊断必须读取应用自身日志；Maven `-l` 只记录 Maven 主进程，不能作为唯一证据。
- 真实 `.env.local` 或外部资源参与验证时，先规划日志脱敏方式。

## 经验 41：非 Web 启动通过后必须补 Web 模式探测

记录日期：2026-05-14；记录者：Codex。

问题：

`fist-monitor` 在 Boot 4 非 Web 模式下可以完成环境构建和 migrator 诊断，但非 Web 模式不会启动 Servlet 容器、管理端口和 Web 自动配置的完整分支。只依据非 Web 结果判断内部应用可运行，会漏掉 Web classpath 和运行期 provider 问题。

错误尝试或风险：

- 只跑 `--spring.main.web-application-type=none`，会把 Servlet 容器、Actuator Web 暴露和管理上下文排除在验证范围之外。
- 只看 Maven `-l` 日志只能得到 `BUILD SUCCESS`，看不到应用日志中的 provider 缺失提示。
- 看到 Tomcat 启动成功后直接认为 Web 栈完整可用，可能漏掉 Bean Validation provider 缺失对请求参数校验、方法校验和配置属性校验的影响。

有效方案：

- 非 Web 探测通过后，移除 `--spring.main.web-application-type=none`，使用相同本地 `file:` 配置模板补跑 Web 模式。
- 使用 `--server.port=0` 和 `--management.server.port=0`，避免固定端口冲突。
- 每轮 `spring-boot:start spring-boot:stop` 使用独立 JMX port，确保 stop goal 能控制本轮应用进程。
- Web 模式也补跑一轮 Boot 4.0.6 `spring-boot-properties-migrator`，确认配置诊断与 Web 自动配置共同存在时仍能启动。
- 诊断来源必须包含应用自身日志，重点搜索 `Application run failed`、`PropertiesMigration`、`deprecated`、`renamed`、`replacement` 和 provider 缺失信息。

验证证据：

- Boot 4 Web 模式下，不带 migrator 的 `fist-monitor` 启动/停止为 `BUILD SUCCESS`。
- 追加 Boot 4.0.6 migrator 与 configuration metadata 后，Web 启动/停止仍为 `BUILD SUCCESS`。
- 应用日志显示 Tomcat 11 启动应用端口和管理端口，Actuator 暴露 15 个 endpoint，并完成优雅停止。
- 应用日志未出现 `PropertiesMigration`、`deprecated`、`renamed` 或 `replacement` 配置迁移诊断。
- 应用日志出现 `jakarta.validation.NoProviderFoundException`，说明当前 classpath 缺少 Bean Validation provider；该问题未阻塞启动，但需要作为后续依赖核查项。

后续复用规则：

- 内部应用每个服务的启动验证至少分为非 Web 探测和 Web 探测两层；非 Web 只用于快速分离配置问题。
- Web 探测通过标准不仅是 Maven `BUILD SUCCESS`，还要确认应用日志中没有启动失败、关键 provider 缺失或配置迁移诊断。
- Bean Validation provider 缺失不应在验证报告中被归类为配置项迁移问题；应进入依赖图与 starter 装配核查。
- 真实外部资源参与前，先保持随机端口和本地配置导入方式，减少数据库、Redis、Nacos 与 Web classpath 问题互相干扰。

## 经验 42：Boot 4 Web 应用要显式确认 Bean Validation provider

记录日期：2026-05-14；更新日期：2026-05-18；记录者：Codex。

问题：

`fist-monitor` 在 Boot 4 Web 模式下启动成功，但日志提示 `jakarta.validation.NoProviderFoundException`。这说明 classpath 中存在 `jakarta.validation-api`，但没有 Hibernate Validator 等 Bean Validation provider。该问题不会必然阻塞应用启动，却会影响运行期校验能力。

错误尝试或风险：

- 误认为 `spring-boot-starter-web` 一定会传递引入 Hibernate Validator；Boot 4.0.6 依赖树中没有这个传递关系。
- 只看应用是否启动成功，会漏掉 provider 缺失对 `@Validated`、请求参数校验、方法校验和配置属性校验的影响。
- 把 provider 缺失当成 migrator 配置项问题处理，会偏离真实原因；这属于运行时依赖图问题。

有效方案：

- 先跑 dependency tree，确认 `jakarta.validation-api`、`spring-boot-starter-validation` 和 `hibernate-validator` 的实际来源。
- 在临时副本的应用模块中显式加入 `spring-boot-starter-validation`，不要先改真实内部项目。
- 重跑 Web 启动和 Web + migrator 启动，确认 Hibernate Validator 初始化、Tomcat 启动、Actuator 暴露和优雅停止都通过。
- 检索应用日志中新时间段，确认不再出现新的 `NoProviderFoundException`。

验证证据：

- 修复前 dependency tree 显示 `fist-support-web` 只传递 `jakarta.validation-api:3.1.1`。
- 修复后 dependency tree 显示 `spring-boot-starter-validation:4.0.6 -> spring-boot-validation:4.0.6 -> hibernate-validator:9.0.1.Final`。
- 修复后不带 migrator 的 Web 启动/停止为 `BUILD SUCCESS`，日志出现 `Hibernate Validator 9.0.1.Final`。
- 修复后带 migrator 的 Web 启动/停止为 `BUILD SUCCESS`，新时间段没有 `NoProviderFoundException`，也没有配置迁移诊断。
- 真实内部项目提交 `735754fe fix: add monitor validation provider` 已在 `fist-monitor` 显式加入 `spring-boot-starter-validation`。
- 真实内部项目 `fist-monitor` Boot 4 Web 启动 / 停止为 `BUILD SUCCESS`，日志确认 Hibernate Validator `9.0.1.Final` 初始化、Tomcat `11.0.21` 随机端口启动并完成 graceful shutdown。

后续复用规则：

- 每个内部 Web 服务升级到 Boot 4 时，都应显式核查 validation provider，而不是依赖 Web starter 间接带入。
- 应用确实使用校验能力时，优先在应用侧或内部应用父 POM 声明 `spring-boot-starter-validation`。
- FIST Kit 支撑库如只需要注解和接口编译，保留 `jakarta.validation-api` 即可；是否携带 provider 应由应用运行时决定。

## 经验 43：Boot 4 不应继续保留旧 Undertow starter

记录日期：2026-05-14；记录者：Codex。

问题：

内部应用部分模块在默认打包 profile 中通过 `spring-boot-starter-undertow` 替换默认 Web 服务器。切到 Boot 4 parent 后，Maven 在项目读取阶段报该 starter 缺少版本，导致即使只想验证其他模块，也会被 reactor 中的旧 POM 阻塞。

错误尝试或风险：

- 只用 `-pl <目标模块> -am` 不能完全规避该问题；Maven 仍可能先读取 reactor 中其他模块的 POM。
- 手工给旧 `spring-boot-starter-undertow` 指定版本不是推荐解法。Boot 4 依赖管理不再覆盖它，继续保留会把兼容性风险留到运行期。
- 只收窄 reactor 可以暂时绕过目标模块验证，但不能作为全量内部应用升级方案。

有效方案：

- 先用单模块 `mvn -f <模块 pom> validate` 复现 `spring-boot-starter-undertow` 缺版本错误，确认问题发生在 POM 读取阶段。
- 查 Spring Boot 4 Web server 文档和本地 `spring-boot-dependencies:4.0.6`，确认 Servlet 栈受管理路线是 Tomcat / Jetty，且默认容器是 Tomcat；Reactive 还包括 Reactor Netty。
- 如无外部容器 jar 加载需求，删除 Undertow 路线并使用默认 Tomcat。
- 如需要不打包 Web 容器，关闭默认打包 profile，保留外部路径加载容器 jar 的部署模式。
- 如仍需要非默认 Servlet 容器，将 `spring-boot-starter-undertow` 替换为 `spring-boot-starter-jetty`。

验证证据：

- 修复前，`fist-cloud/fist-iam` 单模块 `validate` 失败，错误为 `spring-boot-starter-undertow` 缺少版本。
- 临时副本中曾将 `fist-cloud/fist-iam`、`fist-cloud/fist-auth-v3` 和 `pac-cloud/demo-order` 的旧 Undertow starter 替换为 `spring-boot-starter-jetty`，验证 Boot 4 受管理 Servlet 容器 starter 可以消除 POM 读取阶段阻塞。
- 真实内部项目落地时，3 个模块的 `pkg-all` 已修正为 `spring-boot-starter-tomcat`，对应「默认打包容器使用 Tomcat；关闭 `pkg-all` 时不打包容器」的项目策略。
- 容器替换后，相关模块单模块 `validate` 均为 `BUILD SUCCESS`。
- `fist-auth-v3` 和 `demo-order` 仍有部分内部 SNAPSHOT POM 未安装 warning；该 warning 属于后续本地安装 / 发布问题，不再是 Web server starter 迁移问题。

后续复用规则：

- 全量内部应用 Boot 4 试编译前，先统一处理所有 `spring-boot-starter-undertow`。
- Web server 迁移应优先采用 Boot 4 受管理 starter，不在应用 POM 中保留未受管理的旧 starter。
- 文档记录时区分「POM 读取阶段阻塞」和「依赖解析 / 编译阶段阻塞」，避免把后续 SNAPSHOT warning 误归因给 Web 容器 starter。

## 经验 44：内部 SNAPSHOT 基础库要以本地 install 为前提

记录日期：2026-05-18；记录者：Codex。

问题：

FIST Kit v4 `4.0.0-SNAPSHOT` 处于大版本升级适配阶段，并未发布到中心仓库。内部应用试编译如果缺少本地 install，Maven 会尝试从远程仓库解析未发布组件，导致错误方向被误判为远程依赖缺失。

错误尝试或风险：

- 把 `4.0.0-SNAPSHOT` 当作已发布依赖处理，会浪费时间排查远程仓库、镜像和 settings。
- 在离线模式下，如果本地仓库缺少 Boot 4 官方依赖或 `_remote.repositories` 记录不匹配，也会产生依赖解析失败；该问题应与 FIST Kit v4 本地 install 问题区分。

有效方案：

- 内部项目验证前，先确认 FIST Kit v4 已本地 install。
- 内部应用试编译使用同一个本地 Maven 仓库和 settings，保证 SNAPSHOT 基础库、Boot 4 官方依赖和外部生态依赖解析路径一致。
- 只在缺少 Spring Boot、SpringDoc、Jetty、DevTools 等外部发布依赖时联网补齐；不把 FIST Kit v4 交给远程仓库解析。

验证证据：

- 用户确认 FIST Kit `4.0.0-SNAPSHOT` 已本地 install 后，内部应用验证继续以本地仓库为准。
- 补齐 `spring-boot-devtools:4.0.6` 后，`fist-auth-v3` 与 `fist-iam` 主代码 `compile` 为 `BUILD SUCCESS`。

后续复用规则：

- 所有内部应用 Boot 4 验证命令都显式指定本地 Maven 仓库。
- 报告中描述 FIST Kit v4 时使用「本地 install 的 `4.0.0-SNAPSHOT`」，不写成「已发布版本」。

## 经验 45：Spring Security 7 与 Authorization Server 7 会同时影响授权服务代码

记录日期：2026-05-18；记录者：Codex。

问题：

`fist-auth-v3` 切到 Boot 4 后，授权服务器配置和自定义登录配置器同时出现编译错误。错误不是单一依赖缺失，而是 Spring Security 7 与 Spring Authorization Server 7 API 包路径和方法签名变化共同造成。

错误尝试或风险：

- 只补 `spring-security-config` 依赖不能解决旧 `OAuth2AuthorizationServerConfigurer` 包路径。
- 只迁移 Authorization Server 包路径后，如果未运行格式校验，会被 Spring Java Format 拦截。
- 只看主代码编译通过，仍会漏掉测试源码中的 Boot 3 自动配置包迁移。

有效方案：

- 参考 FIST Kit v4 OAuth2 Server 示例中的当前写法，迁移 `OAuth2AuthorizationServerConfigurer` 包路径。
- 对继承 `AbstractHttpConfigurer` 的自定义类，按 Spring Security 7 签名调整 `init(HttpSecurity)`。
- 每次 Java 源码改动后先运行 `spring-javaformat:apply` 或格式校验，再继续编译。

验证证据：

- 迁移 `OAuth2AuthorizationServerConfigurer` 到 `org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization` 后，旧包缺失错误消失。
- 调整 `FormIdentityLoginConfigurer#init` 后，Spring Security 7 接口签名错误消失。
- `fist-auth-v3` 主代码最终通过 `compile`。

后续复用规则：

- 其他内部授权服务或网关模块升级时，优先检索 `OAuth2AuthorizationServerConfigurer`、`AbstractHttpConfigurer`、`SecurityConfigurer` 和自定义 grant provider。
- 授权服务器配置迁移应同时覆盖主代码、测试代码和示例代码，避免只修运行模块。

## 经验 46：Jackson 3 编译通过不等于授权数据兼容通过

记录日期：2026-05-18；记录者：Codex。

问题：

Boot 4 使用 Jackson 3，包名从 `com.fasterxml.jackson` 迁移到 `tools.jackson`。`fist-auth-v3` 中 OAuth2 授权数据持久化曾通过自定义 RowMapper、mixin 和 `AuthUserDeserializer` 控制 JSON 读写；该部分与 FIST Kit v4 `DefaultAuthorizationService` 的新实现发生 API 不匹配。

错误尝试或风险：

- 继续用 Jackson 2 的 `ObjectMapper` 传给 FIST Kit v4 RowMapper 会出现类型不兼容。
- 继续调用旧 `LobHandler` 构造器、`getLobHandler()` 或 `setLobHandler(...)` 会编译失败。
- 为了让主代码先编译通过而临时移除自定义 RowMapper 覆盖，只能作为探测手段，不能作为正式迁移结论。

有效方案：

- 主代码试编译阶段先按 FIST Kit v4 `DefaultAuthorizationService` 新构造器收敛，确认应用主体能通过 `compile`。
- 将授权数据 JSON 读写、`AuthUser` 反序列化、历史 token 数据兼容性列为单独验证项。
- 正式迁移时应基于 Jackson 3 API 重写自定义 mixin / deserializer，或复用 FIST Kit v4 提供的标准授权服务扩展点。

验证证据：

- 移除旧 `LobHandler` 路线并按 FIST Kit v4 构造器调用后，`JdbcAuthorizationService` 主代码编译通过。
- `fist-auth-v3` 主代码通过 `compile`，但仍存在 Jackson 2 相关废弃警告和测试迁移缺口。
- 真实内部项目 `pac-cloud-boot3-10.x` 已补充 `JdbcAuthorizationServiceTest`，使用 H2 和 Spring Authorization Server 7 官方授权表结构覆盖新写入授权记录、按 ID 读取、按 access token 读取和历史 `AuthUser` JSON 读取。
- 定向测试命令 `cmd /c mvn -pl fist-cloud/fist-auth-v3,fist-cloud/fist-iam -am "-P!config-nacos" -DskipTests=false "-Dtest=JdbcAuthorizationServiceTest,OAuth2ErrorMessagePropagationTest,PasswordMfaOauthFailureHandlerTest,PasswordMfaAwarePasswordAuthenticationProviderTest,PasswordMfaGrantAuthenticationProviderTest,PasswordMfaResendAuthenticationProviderTest,PasswordMfaTokenEndpointIntegrationTest,LoginModeManageServiceTest,LoginModeResolverTest,UserAuthControlRepositoryImplIntegrationTest,TaskHistoryPoolIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 为 `BUILD SUCCESS`；`fist-auth-v3` 17 个测试、`fist-iam` 19 个测试均通过。
- 真实内部项目提交 `b3e0f5dc fix: restore auth user authorization json` 已落地：应用侧注册 Jackson 3 `AuthUserJsonModule`，并在 RowMapper 读取后将被 Spring Security module 恢复为基础 `User` 的 principal 替换回内部 `AuthUser`。
- 真实内部项目提交 `d01c71f8 fix: remove auth user jackson2 leftovers` 已删除旧 `AuthUserMixin` 与 Jackson 2 `AuthUserDeserializer`，避免授权 principal 继续存在两套反序列化实现。
- 全 reactor `cmd /c mvn "-P!config-nacos" -DskipTests test-compile` 为 `BUILD SUCCESS`，说明测试源码 Boot 3 自动配置包迁移阻塞在当前真实仓库已解除。

后续复用规则：

- OAuth2 授权服务迁移必须增加数据库级或 H2 级授权数据读写测试。
- 历史授权数据兼容性需要使用真实结构样本或脱敏样本验证，不以编译通过替代。
- 如果应用侧 principal 扩展了 Spring Security `User`，需要同时验证「写出类型信息」和「读回实际子类」；Spring Security Jackson module 能恢复基础用户对象，不代表内部扩展字段仍在。
- FIST Kit v4 的 `DefaultAuthorizationService` 负责标准授权服务 JSON 与 LOB 读写，应用侧自定义 principal 仍应通过应用自己的 Jackson 3 module、RowMapper 或授权服务扩展点补足。
- Boot 4 上下文测试应从 Spring 容器获取 `tools.jackson.databind.ObjectMapper`；继续按 Jackson 2 `com.fasterxml.jackson.databind.ObjectMapper` 取 bean 会得到 `NoSuchBeanDefinitionException`。

## 经验 47：本地 Boot 4 启动 profile 要先刷新 Maven resources

记录日期：2026-05-18；记录者：Codex。

问题：

真实内部项目 `fist-monitor` 新增 `application-boot4-local.yml` 后，直接执行 `spring-boot:start` 曾继续报缺少 `nacos:` import，或仍初始化 Nacos discovery。该现象容易被误判为 profile 配置无效，实际根因是 Maven 直接执行 `spring-boot:start` 时没有先刷新 `target/classes`，新增 resource 尚未进入运行时 classpath。

错误尝试或风险：

- 只新增 profile 文件后立即执行 `spring-boot:start`，可能仍使用旧的 `target/classes`。
- 用很长的 `spring-boot.run.arguments` 排除 Nacos 可以临时启动，但会把可复用配置留在命令行中，不利于后续服务复用。
- 在公开文档或提交中记录 `.env.local`、Nacos namespace、group、连接串或账号密码，会扩大敏感信息暴露面。

有效方案：

- 为本地 Boot 4 启动验证新增独立 profile，例如 `boot4-local`。
- 在 profile 文件中关闭 Nacos Config、Nacos Discovery、服务自动注册和 Nacos health，并排除 Alibaba Nacos 相关 auto-configuration。
- 修改或新增 resource 后，先执行 `process-resources`、`compile` 或其他会刷新 Maven resources 的生命周期阶段，再执行 `spring-boot:start spring-boot:stop`。
- 默认不加载 `.env.local`；需要真实资源验证时只从本机注入环境变量，并只记录资源类型和脱敏结果。

验证证据：

- 真实内部项目提交 `e7b11c60 test: add monitor boot4 local profile` 已新增 `fist-cloud/fist-monitor/src/main/resources/application-boot4-local.yml`。
- `cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" -DskipTests process-resources` 为 `BUILD SUCCESS`，并确认 profile 文件进入 `target/classes`。
- `cmd /c mvn -pl fist-cloud/fist-monitor "-P!config-nacos" -DskipTests "-Dspring-boot.run.profiles=boot4-local" spring-boot:start spring-boot:stop` 为 `BUILD SUCCESS`。
- 启动日志显示 `boot4-local` profile 激活、Hibernate Validator `9.0.1.Final` 初始化、Tomcat `11.0.21` 使用随机业务端口和随机管理端口启动，未出现 Nacos 登录、注册或连接失败日志。
- `cmd /c mvn -pl fist-cloud/fist-monitor -am "-P!config-nacos" -DskipTests test-compile` 为 `BUILD SUCCESS`。

后续复用规则：

- 应用侧启动验证优先使用可提交的本地 profile 文件承载稳定配置；命令行参数只保留 profile、端口、临时诊断 jar 等会随验证变化的内容。
- 新增或修改 resource 后，不直接把 `spring-boot:start` 失败判定为配置无效，先确认 `target/classes` 是否已刷新。
- Nacos 回归验证应放在本地配置源启动稳定之后，且不得把 `.env.local` 内容写入文档、测试源码或提交信息。

## 经验 48：Boot 4 授权服务启动要同时补齐安全自动配置和 Jackson 3 应用侧客户端

记录日期：2026-05-18；记录者：Codex。

问题：

`fist-auth-v3` 主代码编译通过后，真实应用启动仍可能失败。编译阶段已覆盖 Spring Authorization Server 7 包路径和自定义授权服务构造器变化，但运行期还需要完整的 Spring Security 自动配置基础，以及应用侧 OAuth 客户端代码使用的 Jackson 3 bean 类型。

错误尝试或风险：

- 只保留 `spring-security-config` 可以让部分配置类编译通过，但 Boot 4 Web 应用启动时仍可能缺少 `HttpSecurity` 等由安全自动配置提供的对象。
- 只迁移授权服务持久化的 Jackson 3 代码，不迁移 `StandardOauthClientImpl` 这类应用侧 HTTP 客户端，会在启动时因为 `ObjectMapper` 类型不匹配而失败。
- 通过命令行临时排除 Druid Boot 3 自动配置只能隔离问题，不能作为最终升级方案。

有效方案：

- `fist-auth-v3` 显式加入 `spring-boot-starter-security`，让 Boot 4 自动配置提供安全过滤器链装配所需基础。
- `AuthorizationServerSettings` 显式声明为 bean，并注入授权服务器过滤器链，避免授权服务器配置只在局部 builder 中临时创建。
- `StandardOauthClientImpl` 及其测试统一使用 `tools.jackson.databind.ObjectMapper` 和 `tools.jackson.core.type.TypeReference`。
- 对本地启动验证，先使用 H2 测试资源、随机端口、禁用 Nacos，并临时排除 Druid Boot 3 自动配置，确认 auth 自身 Boot 4 启动路径。

验证证据：

- 真实内部项目提交 `10bfb34a fix: migrate auth oauth boot4 startup` 已落地上述改动。
- `cmd /c mvn -pl fist-cloud/fist-auth-v3 "-P!config-nacos" -DskipTests ... spring-boot:start spring-boot:stop` 为 `BUILD SUCCESS`，`fist-auth-v3` 使用 `test,boot4-local` profile 启动并停止。
- 启动日志显示 Tomcat `11.0.21`、Hikari / H2 和 Hibernate Validator `9.0.1.Final` 装配成功。
- `cmd /c mvn -pl fist-cloud/fist-auth-v3 -am "-P!config-nacos" -DskipTests test-compile` 为 `BUILD SUCCESS`。
- `StandardOauthClientImplTest`、`ExternalLoginExchangeDtoJacksonContextTest`、`ExternalLoginAccessTokenIssuerImplTest` 共 9 个定向测试全部通过。

后续复用规则：

- 授权服务模块的 Boot 4 迁移要同时做编译验证、`test-compile`、定向单测和本地启动探测。
- 遇到 Druid Boot 3 自动配置引用旧 `org.springframework.boot.autoconfigure.jdbc` 包时，应单独制定数据源依赖策略，不把启动命令中的 exclude 固化为长期配置。
- `.env.local` 仅作为本机真实资源验证输入；公开文档只记录资源类型、验证阶段和脱敏结果。

## 经验 49：IAM 本地启动要把配置源、Jackson 3 和第三方 starter 兼容性一起验证

记录日期：2026-05-18；记录者：Codex。

问题：

`fist-iam` 主代码和 `test-compile` 通过后，真实 Web 启动仍暴露多类运行期问题：本地模块依赖需要先安装、Logbook 旧 starter 仍按 Jackson 2 自动配置、应用代码仍注入 Jackson 2 `ObjectMapper`，以及本地 H2 启动缺少最小业务表结构。单靠编译无法覆盖这些启动期装配风险。

错误尝试或风险：

- 单模块执行 `spring-boot:start` 时，如果本地 reactor 依赖未 install，Maven 会尝试从远程仓库解析内部模块。
- `-pl ... -am spring-boot:start` 会把 start goal 执行到父 POM，失败于父 POM 没有 main class。
- Logbook `3.9.0` 会触发 `LogbookAutoConfiguration` 对 Jackson 2 `ObjectMapper` 的依赖，Boot 4 / Jackson 3 默认上下文中没有该 bean。
- 应用侧控制器、导入服务和同步客户端只要仍注入 Jackson 2 `ObjectMapper`，启动就会继续失败。
- 只用命令行长参数禁用 Nacos，不利于反复调整配置；本地启动应收敛为可提交的 profile 或本地 file import。

有效方案：

- 启动前先安装 `fist-iam` 所需的本地 reactor 依赖，再执行单模块 `spring-boot:start spring-boot:stop`。
- 将 Logbook 升级到 `4.0.4`。
- 将 `fist-iam` 主代码和相关测试中的 Jackson 2 `ObjectMapper` / `TypeReference` / `JsonNode` / `JsonProcessingException` 迁移到 Jackson 3 `tools.jackson.*`。
- 将 `JsonNode.fields()` 替换为 `JsonNode.properties()`。
- 新增 `application-boot4-local.yml`，用 H2 和本地 profile 承载 Boot 4 启动隔离配置；新增 `schema-boot4-local.sql`，补齐最小启动表结构。
- 对启动较慢的应用，给 `spring-boot:start` 增加 `-Dspring-boot.start.maxAttempts=180`，避免应用刚启动完成就被 Maven 插件等待超时误判失败。

验证证据：

- 真实内部项目提交 `e63d4a1d fix: migrate iam boot4 startup` 已落地 IAM 主代码迁移、Logbook 4.0.4 和 `boot4-local` profile。
- 真实内部项目提交 `748f94d4 test: migrate iam jackson3 tests` 已迁移受影响测试。
- `cmd /c mvn -pl fist-cloud/fist-iam -am "-P!config-nacos" -DskipTests test-compile` 为 `BUILD SUCCESS`。
- IAM 定向测试 35 个用例全部通过，失败 0，错误 0，跳过 0。
- `cmd /c mvn -pl fist-cloud/fist-iam "-P!config-nacos" -DskipTests "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.profiles=boot4-local" "-Dspring-boot.start.maxAttempts=180" spring-boot:start spring-boot:stop` 为 `BUILD SUCCESS`。
- 启动日志显示 Tomcat 11、H2 / Hikari、MyBatis Plus、FIST Jackson 3、idempotent guard schema check 和 Actuator 管理端口均完成装配。

后续复用规则：

- 内部应用有配置模板时，优先通过本地 profile 或 `spring.config.import=file:` 加载本地配置，便于频繁调整和排障。
- Nacos Config 不作为首轮 Boot 4 启动验证依赖；Nacos Discovery 可以在服务发现专项阶段单独打开。
- 对仍引用 Boot 3 自动配置包的第三方 starter，优先升级到支持 Boot 4 的版本；如果只能通过 exclude 启动，应把它记录为待解决依赖策略，而不是长期配置。
- 本地 H2 启动 profile 只用于启动隔离，不替代真实数据库兼容性验证。

## 经验 49：第三方 starter 与应用代码的 Jackson 2 残留要分层处理

记录日期：2026-05-18；记录者：Codex。

问题：

`fist-iam` Boot 4 本地启动时，旧 Logbook `3.9.0` 首先要求 Spring 容器提供 Jackson 2 `com.fasterxml.jackson.databind.ObjectMapper`。升级 Logbook 后，启动继续暴露内部控制器、同步客户端和数据导入代码仍注入或直接创建 Jackson 2 `ObjectMapper`。这说明 Boot 4/Jackson 3 迁移不能只扫授权服务或测试代码，需要同时覆盖第三方 starter 和应用主代码。

错误尝试或风险：

- 为满足旧 starter 而补一个 Jackson 2 `ObjectMapper` bean，会在 Boot 4 应用内长期保留两套 JSON 运行时，增加行为差异。
- 只升级 Logbook 后立即认为 IAM JSON 迁移完成，会漏掉 `SysDataController`、`TrusfortHttpSyncClient`、`PermissionClient`、`SysDataManagerImpl` 和数据迁移处理器中的 Jackson 2 类型。
- Jackson 3 `JsonNode.properties()` 返回 Set，不再是 Jackson 2 `fields()` 的 Iterator 形态，不能只做方法名替换。

有效方案：

- 将 Logbook 升级到 `4.0.4`，使用当前 Boot 4 / Spring 7 兼容线。
- 应用主代码统一迁移到 `tools.jackson.*`，删除 Jackson 3 下无效的 checked `IOException` / `JsonProcessingException` 捕获。
- `JsonNode.fields()` 改为 `JsonNode.properties()` 后，按 Set 迭代处理。
- 对本地启动 profile，补齐 H2 schema 和 idempotent guard 表名，使启动失败点可以继续前进到真实应用装配。

验证证据：

- 真实内部项目提交 `e63d4a1d fix: migrate iam boot4 startup` 已落地上述改动。
- `cmd /c mvn -pl fist-cloud/fist-iam "-P!config-nacos" spring-javaformat:apply compile` 为 `BUILD SUCCESS`。
- `cmd /c mvn -pl fist-cloud/fist-iam -am "-P!config-nacos" -DskipTests test-compile` 为 `BUILD SUCCESS`。
- `cmd /c mvn -pl fist-cloud/fist-iam "-P!config-nacos" -DskipTests "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.profiles=boot4-local" "-Dspring-boot.start.maxAttempts=120" spring-boot:start spring-boot:stop` 为 `BUILD SUCCESS`。
- 启动日志确认 Tomcat `11.0.21`、Hikari / H2、Hibernate Validator `9.0.1.Final` 和 `fiamc_lock_guard` schema check 均通过。

后续复用规则：

- 每个内部应用启动失败时，按「第三方 starter」「应用主代码」「测试/本地 profile」分层定位 Jackson 2 残留。
- 本地启动 profile 应尽量承载稳定的 Nacos / Druid 隔离和 H2 schema；命令行只保留 profile、test classpath 和启动等待参数。
- 启动耗时接近或超过 30 秒的服务，应显式设置 `spring-boot.start.maxAttempts`，避免误判为应用启动失败。

## 经验 50：Gateway 本地启动优先验证配置源和 Reactive Web 装配

记录日期：2026-05-18；记录者：Codex。

问题：

`fist-gateway` 默认依赖 Nacos Config、Nacos Discovery、Redis / Redisson 和外部下游服务。直接按默认 profile 启动会把配置中心、服务发现和真实资源混在一起，无法快速判断 Boot 4 / Spring Cloud Gateway 自身是否可以装配。

错误尝试或风险：

- 以 Nacos Config 作为首轮启动前置条件，会把配置中心可用性误判为 Boot 4 兼容性问题。
- 在网关模块套用 Servlet 容器策略会产生误判。`fist-gateway` 使用 WebFlux，Boot 4 本地启动容器是 Reactor Netty，不是 Tomcat 或 Jetty。
- 直接加载完整配置模板可能引入真实 OAuth 2.0 introspection、Redis 和服务发现依赖，不适合首轮启动隔离。

有效方案：

- 新增 `application-boot4-local.yml`，使用随机业务端口和随机管理端口。
- 在本地 profile 中关闭 Nacos Config、Nacos Discovery、服务注册和 Nacos health。
- 排除 Nacos、Redis 和 Redisson 自动配置，先验证 Gateway / WebFlux / FIST route guard 的应用装配。
- 使用 `optional:file:./config/application.yml` 与 `optional:file:./config/${spring.application.name}.yml` 作为本地配置输入入口，后续可把配置模板整理到本地 `config/` 目录逐项验证。
- 提供最小本地 route、授权跳过规则和占位 OAuth 2.0 opaque-token 配置，避免依赖配置中心即可完成启动。

验证证据：

- 真实内部项目提交 `9c9dcf09 test: add gateway boot4 local profile` 已落地上述 profile。
- `cmd /c mvn -nsu -pl fist-cloud/fist-gateway "-P!config-nacos" -DskipTests process-resources` 为 `BUILD SUCCESS`。
- `cmd /c mvn -nsu -pl fist-cloud/fist-gateway "-P!config-nacos" -DskipTests "-Dspring-boot.run.profiles=boot4-local" "-Dspring-boot.start.maxAttempts=180" spring-boot:start spring-boot:stop` 为 `BUILD SUCCESS`。
- 启动日志确认 Reactor Netty 随机业务端口和随机管理端口启动，Gateway route predicate factory、NoOp cache、FIST Jackson 3 和 Gateway auth filter 完成装配。
- `cmd /c mvn -nsu -pl fist-cloud/fist-gateway -am "-P!config-nacos" -DskipTests test-compile` 为 `BUILD SUCCESS`。

后续复用规则：

- 网关的 Boot 4 验证先证明本地 Reactive Web 应用可以启动，再逐步打开本地配置模板、Redis、Nacos Discovery 和真实下游服务。
- Nacos Config 不作为首轮启动验证依赖；Nacos Discovery 只在服务发现专项验证中打开。
- 公开文档只记录资源类型、验证阶段和脱敏结果，不记录 `.env.local`、配置模板中的凭证或真实服务地址。

## 经验 51：本地配置模板和 `.env.local` 要分层使用

记录日期：2026-05-18；记录者：Codex。

问题：

内部项目既有服务配置模板，也有本机 `.env.local`。两者用途不同：配置模板适合反复调整应用配置，`.env.local` 适合提供本机真实资源环境变量。如果混用，容易把真实数据库、Redis、Nacos 等连接信息写入文档、日志或提交内容。

有效方案：

- 首轮 Boot 4 启动验证优先使用本地 profile 和本地配置文件导入。
- 将需要验证的配置模板整理到本地 `config/` 目录，通过 `spring.config.import=file:` 或 `optional:file:` 加载。
- `.env.local` 只作为本机真实资源验证输入，提供数据库、Redis、Nacos 等环境变量。
- 本地 profile 负责关闭 Nacos Config、服务注册和首轮验证不需要的外部资源依赖。
- Nacos Config 不作为首轮启动验证依赖；Nacos Discovery 在服务发现专项阶段单独打开。

记录边界：

- 不读取、不复制、不提交、不记录 `.env.local` 的具体内容。
- 不在公开文档中记录数据库、Redis、Nacos、Maven settings、私服地址或凭据。
- 只记录资源类型、验证阶段、是否使用外部资源、脱敏后的错误类别和解决方案。

后续复用规则：

- 遇到相同需求时，先判断是「配置输入验证」还是「真实资源验证」。
- 配置输入验证走本地 `config/` 文件和 `spring.config.import=file:`。
- 真实资源验证再启用 `.env.local`，并只保留脱敏结果。

## 经验 52：本地 `config/` 目录需要防误提交保护

记录日期：2026-05-18；记录者：Codex。

问题：

内部项目通过 `optional:file:./config/${spring.application.name}.yml` 验证本地配置输入后，`config/` 目录会自然成为放置本地数据库、Redis、Nacos、OAuth 2.0 和下游服务配置的入口。如果没有忽略规则，实施人员容易把本地配置文件误提交。

有效方案：

- 在内部项目 `.gitignore` 中忽略 `fist-cloud/*/config/*.yml`、`*.yaml`、`*.properties`。
- 只提交 `config/README.md` 这类说明文件，记录用途、文件名约定和敏感信息边界。
- 用脱敏本地文件验证 `optional:file:` 导入；验证完成后只提交忽略规则和说明，不提交具体配置文件。

验证证据：

- `fist-gateway` 使用被忽略的本地 `config/fist-gateway.yml` 覆盖 `app-info.version`。
- `spring-boot:start spring-boot:stop` 为 `BUILD SUCCESS`，启动 banner 显示本地文件覆盖后的版本号，说明 `optional:file:./config/${spring.application.name}.yml` 已生效。
- `fist-gateway -am test-compile` 为 `BUILD SUCCESS`。

后续复用规则：

- 后续服务如需本地配置输入，优先沿用 `fist-cloud/<service>/config/`。
- 本地配置文件不提交；提交内容只保留 README、profile 和必要的 ignore 规则。
- 如需真实资源，环境变量来自 `.env.local`，但不读取或记录其具体内容。

补充规则：

- 配置模板只作为模板使用，不作为运行时配置文件直接修改。
- 标准流程是先把配置模板复制到临时位置或本地 `config/` 目录，再修改本地运行时配置。
- 启动和测试都使用本地运行时配置。
- 只有确认模板本身需要变化时，才回写配置模板。

## 经验 53：Gateway 多路由配置可先做启动级验证

记录日期：2026-05-18；记录者：Codex。

问题：

网关配置通常同时包含多路由、CORS、默认过滤器、限流、OAuth 2.0 introspection、服务发现和真实下游地址。如果直接验证真实转发，失败点会混在一起，难以判断是 Boot 4 装配问题、配置绑定问题、限流 bean 问题，还是外部资源问题。

有效方案：

- 使用被 Git 忽略的本地 `config/fist-gateway.yml` 承载脱敏配置。
- 先保留多路由、CORS 和 `RequestRateLimiter` 默认过滤器结构。
- 所有下游 URI 使用本机占位地址，避免真实请求。
- 保持 Nacos Discovery、Redis 和真实 OAuth 2.0 introspection 关闭，只验证 Gateway 配置合成和启动期 bean 装配。

验证证据：

- 本地多路由配置通过 `optional:file:./config/${spring.application.name}.yml` 导入。
- `cmd /c mvn -nsu -pl fist-cloud/fist-gateway "-P!config-nacos" -DskipTests process-resources` 为 `BUILD SUCCESS`。
- `cmd /c mvn -nsu -pl fist-cloud/fist-gateway "-P!config-nacos" -DskipTests "-Dspring-boot.run.profiles=boot4-local" "-Dspring-boot.start.maxAttempts=180" spring-boot:start spring-boot:stop` 为 `BUILD SUCCESS`。
- 启动 banner 显示本地多路由配置覆盖后的版本号，`RequestRateLimiter` 默认过滤器未触发启动期 bean 引用或配置绑定错误。

后续复用规则：

- Gateway 真实转发验证前，先完成启动级配置验证。
- 请求级验证再逐步打开下游服务、OAuth 2.0 introspection、Redis、限流行为和 Nacos Discovery。
- 每次只打开一类外部依赖，并记录脱敏结果。

## 经验 54：Gateway 请求级验证先使用本机 mock 下游

记录日期：2026-05-18；记录者：Codex。

问题：

启动级验证只能证明网关配置可以装配，不能证明路由匹配、过滤器和路径改写能在请求阶段生效。直接连接真实下游会同时引入服务可用性、认证、网络和配置问题，不适合作为请求级首轮验证。

有效方案：

- 先启动本机 mock 下游服务。
- 本地 `config/fist-gateway.yml` 只把待验证路由指向本机 mock 端口。
- 选择已在本地 profile 放行的路径发请求，避免提前引入真实 OAuth 2.0 introspection。
- 固定 gateway 业务端口和管理端口，验证完成后按端口清理进程。

验证证据：

- 本机 mock 下游服务监听 `127.0.0.1:19091`。
- `fist-gateway` 使用 `spring-boot:run` 固定业务端口 `19080`。
- 请求 `http://127.0.0.1:19080/fauth/oauth2/probe` 返回 200。
- mock 下游收到路径 `/oauth2/probe`，确认 `/fauth/**` 路由匹配和 `StripPrefix=1` 生效。

排障规则：

- `spring-boot:run` 适合请求级验证；`spring-boot:start` 在部分固定端口验证中可能未及时返回。
- 清理本机验证进程时优先按端口清理，不按命令行关键字模糊匹配，避免误杀当前脚本。
- `spring-boot.run.arguments` 含多个参数时必须作为单个 Maven 参数传入。

后续复用规则：

- 每次请求级验证只打开一类外部依赖。
- 本机 mock 下游验证通过后，再验证真实下游、OAuth 2.0 introspection、Redis 限流状态和 Nacos Discovery。
- 真实资源仍通过 `.env.local` 或本地运行时配置输入，不记录具体值。
