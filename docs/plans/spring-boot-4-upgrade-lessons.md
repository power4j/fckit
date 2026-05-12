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

- `docs/plans/spring-boot-4-code-impact.md` 已列出 5 个 `.groovy` 测试文件和构建依赖命中点。
- `docs/plans/spring-boot-4-trial-compile.md` 已标注相关 Maven `test` 未实际运行 Groovy 测试的风险。

后续复用规则：

- 任何涉及 Groovy/Spock 的批次，都要明确区分「Java 测试通过」和「Groovy 测试已运行」。
- 删除 Groovy 前必须先完成测试等价迁移。

## 经验 7：API Doc 不能只按 Springfox 删除处理

问题：

API Doc 模块仍有 Springfox 退役、Jackson 2 API 和 Commons Lang 调用问题。

错误尝试或风险：

- OpenRewrite dry-run 会倾向删除 Springfox 依赖和相关代码，但 API Doc 模块可能存在用户可见功能差异。
- 如果直接删除 Springfox 分支，可能破坏仍依赖旧 Swagger 能力的使用方。

有效方案：

- API Doc 作为单独专项处理。
- 先梳理 Springfox 与 SpringDoc 的现有功能边界，再决定是否完全移除 Springfox。
- Jackson 2 `JsonNode.fields()` 等问题需要结合 Jackson 3 API 单独验证。

验证证据：

- API Doc 低风险 API 迁移后，`JsonNode.fields()` 和 `Validate.notNull(...)` 复扫未命中。
- 本地 Jackson 2.21.3 字节码确认 `JsonNode` 同时提供 `fields()` 和 `properties()`；本地 Jackson 3.1.2 字节码确认 `JsonNode` 保留 `properties()` 且不再提供 `fields()`。
- `fist-boot-apidoc -am` deprecation 明细编译为 `BUILD SUCCESS`；测试为 `BUILD SUCCESS`，依赖链 `fist-kit-api` 共 6 个用例，失败 0。
- 剩余 API Doc deprecation 明细只来自 Springfox 相关类缺少 `@Deprecated` 注解，需要在 Springfox 去留决策中处理；编译日志另有依赖链 `fist-kit-api` 的 `ElementType.MODULE` 提示，不属于本轮 API Doc deprecation 收敛目标。

后续复用规则：

- 任何用户可见文档能力的删除，都需要记录破坏性变更和替代路径。
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
- 本轮只替换 `fields()` 和 `Validate.notNull(T)`，不删除 Springfox 分支。

验证证据：

- `javap` 检查 `F:\pkg-maven\repository\com\fasterxml\jackson\core\jackson-databind\2.21.3\jackson-databind-2.21.3.jar`。
- `javap` 检查 `F:\pkg-maven\repository\tools\jackson\core\jackson-databind\3.1.2\jackson-databind-3.1.2.jar`。
- `rg -n "Validate\.notNull|\.fields\(" fist-kit-app/fist-boot-apidoc/src/main/java -g "*.java" -S` 复扫未命中。
- `fist-boot-apidoc -am` 编译和测试均为 `BUILD SUCCESS`。

后续复用规则：

- Jackson 迁移不能只做包名替换，需要确认替代 API 同时覆盖当前版本和目标版本。
- 文档能力模块要把「低风险 API 替换」和「用户可见能力删除」拆分提交。
- Springfox 去留需要单独记录破坏性变更、替代路径和下游影响。

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

验证证据：

- `docs/plans/spring-boot-4-research-report-0.2.md` 已将 Feign 与 SDE 作为评审修订项。
- `docs/plans/spring-boot-4-code-impact.md` 已列出 26 个 Feign 相关 Java 文件命中。

后续复用规则：

- Feign 迁移不能只看依赖坐标和注解。
- SDE 的编解码行为必须作为功能契约验证。

## 经验 10：文档和提交需要与升级批次同步

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

## 当前剩余高优先级事项

1. API Doc 专项：评审 Springfox 支持的去留，决定完全删除 Springfox 分支，或在过渡期显式标记弃用。
2. Groovy/Spock 去除：迁移 5 个 `.groovy` 测试后删除 gmavenplus / Spock / Groovy 依赖链。
3. Feign/SDE 专项：评估 Spring 官方 Feign、Spring Cloud OpenFeign 5.x 和 Spring HTTP Service Clients 的路线。
4. OAuth2 LOB 数据库矩阵：补充 MySQL、PostgreSQL、达梦等驱动验证。
5. MyBatis Plus Boot 4 starter：确认对下游公开依赖坐标和发布说明的影响。
