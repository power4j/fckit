# FIST Kit 4.x 应用迁移指南

本指南随 4.x 发布说明草案评审。验证基线为 Kit `c8eaf80b` 的 `4.0.0-SNAPSHOT`、JDK 17、Spring Boot `4.2.0` 和 Spring Cloud `2026.0.0`。正式发布时以对应版本的 BOM 和发布说明为准。

[FIST Kit 4.x 发布说明（草案）](release-notes-4.x-draft.md)

## 切换 Maven 坐标

将应用父 POM、BOM、直接依赖和传递依赖中的 Kit 坐标一起迁移到 `com.power4j.fist4`。保持应用自己的 parent 时，导入 Kit BOM 来管理 Kit 组件版本，并由应用管理 Boot 和所需的 Cloud BOM。仅导入 BOM 的应用可以绑定 Enforcer，在 `validate` 拒绝直接和传递引入的 Kit v3。

下方 POM 使用本地预装的 SNAPSHOT，适合验证坐标和依赖树。三个 starter 同列用于验证，应用按实际功能保留依赖。使用 Gateway 或 OpenFeign 时，导入示例中的 `org.springframework.cloud:spring-cloud-dependencies:2026.0.0`，并声明相应 starter。无需 Cloud 依赖的应用可以省略该 BOM。已有父 POM 的工程将相应区块合入现有 POM。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>kit4-consumer</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <properties>
    <spring-boot.version>4.2.0</spring-boot.version>
    <spring-cloud.version>2026.0.0</spring-cloud.version>
    <fist-kit.version>4.0.0-SNAPSHOT</fist-kit.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.power4j.fist4</groupId>
        <artifactId>fist-kit-dependencies</artifactId>
        <version>${fist-kit.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.power4j.fist4</groupId>
      <artifactId>fist-jasypt-spring-boot4-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.power4j.fist4</groupId>
      <artifactId>fist-trace-context-spring-boot4-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.power4j.fist4</groupId>
      <artifactId>fist-sde-spring-boot4-starter</artifactId>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <version>3.7.0</version>
        <executions>
          <execution>
            <id>ban-fist-kit-v3</id>
            <phase>validate</phase>
            <goals>
              <goal>enforce</goal>
            </goals>
            <configuration>
              <rules>
                <bannedDependencies>
                  <searchTransitive>true</searchTransitive>
                  <excludes>
                    <exclude>com.power4j.fist3</exclude>
                  </excludes>
                </bannedDependencies>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

从消费者 `pom.xml` 所在目录执行。将命令中的路径替换为已预装 Kit SNAPSHOT、BOM 及其导入项、传递依赖和 Maven 插件的本地仓库。若缓存是在使用镜像或自定义仓库 ID 时准备的，离线复跑沿用相同的 Maven settings。

```sh
mvn -o "-Dmaven.repo.local=/path/to/prepared-maven-cache" validate org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree "-Dincludes=com.power4j.fist3:*,com.power4j.fist4:*"
```

样例实测三项 starter 均按 `4.0.0-SNAPSHOT` 解析，直接或经另一组件引入 v3 都在 `validate` 被拒绝。使用 BOM 且省略 Enforcer 时，依赖树可以同时解析两个 groupId。后续按应用实际依赖执行打包和启动验收。

继承 `fist-kit-parent` 或 `fist-kit-build` 的应用均会继承跨代 Enforcer 规则和 Java 格式检查等构建执行。仅继承 `fist-kit-parent` 时，另外导入 Kit BOM 或显式指定 Kit 依赖版本。`fist-kit-build` 的父链已提供 Kit 版本管理。选择继承前，用 `mvn help:effective-pom` 核对构建执行和原有应用 parent 的差异。

[Maven BOM 导入](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html) · [Maven Enforcer 规则](https://maven.apache.org/enforcer/enforcer-rules/bannedDependencies.html)

## BOM 组件对照

下表覆盖 Kit v3 与 v4 BOM 中的全部 30 对 Kit 组件。列中内容为 artifactId，所有组件都切换 groupId。相同 artifactId 表示坐标对应，应用按后续用例核对接口和行为。

| v3 `com.power4j.fist3` | v4 `com.power4j.fist4` |
| --- | --- |
| `fist-kit-api` | `fist-kit-api` |
| `fist-support-spring` | `fist-support-spring` |
| `fist-support-test` | `fist-support-test` |
| `fist-redisson` | `fist-redisson` |
| `fist-jackson` | `fist-jackson` |
| `fist-logback` | `fist-logback` |
| `fist-mybatis-plus` | `fist-mybatis-plus` |
| `fist-sde-core` | `fist-sde-core` |
| `fist-sde-extra` | `fist-sde-extra` |
| `fist-sde-client` | `fist-sde-client` |
| `fist-sde-web` | `fist-sde-web` |
| `fist-sde-boot-starter` | `fist-sde-spring-boot4-starter` |
| `fist-jasypt-core` | `fist-jasypt-core` |
| `fist-jasypt-spring-boot-starter` | `fist-jasypt-spring-boot4-starter` |
| `fist-trace-context` | `fist-trace-context` |
| `fist-trace-context-spring-boot-starter` | `fist-trace-context-spring-boot4-starter` |
| `fist-boot-apidoc` | `fist-boot-apidoc` |
| `fist-support-security` | `fist-support-security` |
| `fist-boot-security` | `fist-boot-security` |
| `fist-boot-security-oauth2-server` | `fist-boot-security-oauth2-server` |
| `fist-support-web` | `fist-support-web` |
| `fist-boot-web-app` | `fist-boot-web-app` |
| `fist-support-data` | `fist-support-data` |
| `fist-boot-data` | `fist-boot-data` |
| `fist-boot-crud-mybatis` | `fist-boot-crud-mybatis` |
| `fist-cloud-core` | `fist-cloud-core` |
| `fist-cloud-rpc-feign` | `fist-cloud-rpc-feign` |
| `fist-gateway-auth-core` | `fist-gateway-auth-core` |
| `fist-cloud-gateway-security-reactive` | `fist-cloud-gateway-security-reactive` |
| `fist-cloud-gateway-acl` | `fist-cloud-gateway-acl` |

## 调整运行和测试依赖

| 应用使用的能力 | 迁移动作 |
| --- | --- |
| Servlet MVC 和 WebFlux | 按应用实际 Web 栈核对 starter。当前 Kit 的 Servlet 示例仍使用 `spring-boot-starter-web`，Gateway 模块使用 `spring-boot-starter-webflux`。直接使用 WebFlux 错误处理类的代码应核对 Boot 4 API，例如 `org.springframework.boot.webflux.error.ErrorAttributes`。 |
| Gateway 和 OpenFeign | Gateway 使用 `spring-cloud-starter-gateway-server-webflux`。依赖自定义 Feign encoder/decoder 的应用需核对 OpenFeign 5 的 `FeignHttpMessageConverters` 装配，再执行真实 HTTP 回归。应用使用的服务发现和熔断依赖由应用显式管理。 |
| MVC 测试 | 执行 `test-compile` 并启动 MVC 测试上下文。直接使用 `AutoConfigureMockMvc` 时核对 `spring-boot-webmvc-test` 的测试依赖，以及 `org.springframework.boot.webmvc.test.autoconfigure` 下的导入。 |
| MyBatis Plus | 应用显式声明的 Boot 3 starter 和测试 starter 分别换为 `mybatis-plus-spring-boot4-starter` 和 `mybatis-plus-spring-boot4-starter-test`。Kit 基线为 MyBatis Plus `3.5.16`、MyBatis Spring `4.0.0`。运行依赖和测试依赖分别用依赖树核对。 |
| OpenAPI | 使用与 Web 栈对应的 Springdoc 3 starter。Kit 管理 Springdoc `3.2.1`，使用 `io.swagger.core.v3:swagger-core`。检查显式锁定的旧 Swagger 依赖及实际 OpenAPI 文档输出。已有 Springfox 配置应迁移到 Springdoc。 |
| OAuth2 Authorization Server | Authorization Server 版本由 Spring Boot BOM 管理。复查应用的显式版本和自定义认证、转换器及 JDBC 授权服务，按实际启用的授权流程验证。 |

### 调整 `fist-support-test` 依赖

Kit v4 的 `fist-support-test` 继续传递 MockWebServer 和 JUnit Pioneer。
该制品已移除 `org.spockframework:spock-spring` 与 `org.tools4j:groovy-tables`，
随之取消传递引入 Spock Core 和 Groovy。按测试源码使用情况选择迁移路径。

- 只使用 MockWebServer 或 JUnit Pioneer 的应用可继续以 `test` scope
  声明 v4 `fist-support-test`，并用测试依赖树核对解析版本。
- 继续使用 Groovy/Spock 时，由应用以 `test` scope 显式声明所用依赖。
  普通 Spock 用例使用 `spock-core`，Spring 集成用例使用 `spock-spring`。
  使用 Groovy Tables 时另加 `groovy-tables`。核对 Spock 制品对应的 Groovy
  主版本和实际解析的 Groovy 版本。由应用配置 Groovy 测试编译并验证用例发现和执行。
- 迁移到 Java/JUnit 时，用 Java 集合或参数化测试替代 Groovy Tables 数据表，
  将 Spock 用例改为 JUnit 测试。迁移完成后检查测试发现数量和执行结果。

最小消费者用 JDK 17 完成三组对照，共 9/9。v3 默认组确认 Spock
`2.4-groovy-5.0` 会拒绝 Groovy `4.0.32`，测试将该拒绝作为预期结果。
v3 对照组将 Groovy 对齐至 `5.0.3` 后，Spock 样例编译通过并生成两行表数据。
v4 组确认旧类无法加载，MockWebServer 回环 HTTP 与 JUnit Pioneer locale 恢复通过。
保留旧测试栈的 v4 应用需用自己的显式依赖和构建配置验证 `test-compile`、测试发现及执行。

## 按应用使用的契约验收

先用升级前版本保存脱敏的请求、响应和持久化样本。升级后运行 `mvn clean verify` 并启动交付制品。按实际使用的能力执行下列检查。

### JSON 和授权数据

1. 按消费方列出它实际读取的字段和类型。分别对照浏览器或客户端、普通 Feign/RestClient、网关用户信息提取等入口的 Boot 3 和 Boot 4 响应。对登录与 opaque token 内省，核对消费方读取的嵌套对象路径、身份 ID 的字符串或数字类型、角色和权限集合，以及缺失、`null`、空集合的区别。跑通登录、内省、网关转发和下游身份识别。
2. 分别确认实际参与读写的 HTTP converter、mapper 和自定义模块。Servlet 对照从 Jackson 2 `MappingJackson2HttpMessageConverter` 迁移到 Jackson 3 `JacksonJsonHttpMessageConverter`。合成 DTO 中，默认 `Long` 和 `BigDecimal` 序列化为字符串，排除 `NumberToStr` 后为数值。启用混淆定制的样本观察到 `@JsonProperty` 别名处理存在差异。默认配置和应用覆盖应分别核对数值类型和精度、日期与时区、属性别名及混淆字段。
3. 在跨服务 DTO、缓存和值对象上固定对端依赖的 JSON 字段契约。Kit Jackson module 提供服务内默认行为，各服务可以分别覆盖。排查时检查生产者和消费者是否使用 Spring 管理的 mapper，以及手工构建的 mapper 是否注册了同一组模块。对直接调用的 Jackson 2 API 保留显式依赖，SDE 默认 codec 仍使用 Jackson 2。

### OAuth2 授权持久化

1. 将 `DefaultAuthorizationService(JdbcOperations, RegisteredClientRepository, LobHandler)` 改为二参构造器 `DefaultAuthorizationService(JdbcOperations, RegisteredClientRepository)`，删除 row mapper 的 `setLobHandler(...)` 调用。默认实现使用 Spring JDBC 的 `SqlBinaryValue` 和 `SqlCharacterValue` 写入 LOB，通过 `ResultSet` 读取。
2. 自定义持久化 JSON 时，同时迁移读取用的 `OAuth2AuthorizationRowMapper` 和写入用的 `OAuth2AuthorizationParametersMapper`。两者的 `setObjectMapper` 与 `getObjectMapper` 均使用 `tools.jackson.databind.ObjectMapper`。在两侧注册 Spring Security 和 Authorization Server 的 Jackson 3 模块，以及应用需要的模块和 mix-in。可通过 `setAuthorizationRowMapper` 和 `setAuthorizationParametersMapper` 装配自定义映射。需要保留驱动专用 LOB 处理时，在对应映射中实现，并用目标 JDBC 驱动及实际 BLOB 或 CLOB 列类型验收。
3. 使用脱敏的 Boot 3 授权记录及隔离数据库，走实际 JDBC 授权服务读取历史记录并内省，核对结构化用户属性。再写入 Boot 4 新记录、关闭上下文后回读并内省。需要跨版本回退时，让旧版读取新版写出的代表性记录。历史兼容结论以应用自己的 mapper、样本和数据库为准。

### MyBatis 查询结果映射

使用含非空集合或 JSON 列的代表性记录，逐次比较原始数据库值、Mapper 查询结果和 API 输出。对 MyBatis Plus 自动生成查询，字段声明 `@TableField(typeHandler = ...)` 时检查实体是否使用 `@TableName(autoResultMap = true)`。对 XML、自定义查询或 DTO 投影，检查实际使用的 `resultMap` 或结果映射是否显式绑定 handler。分别验证 `null`、空值、非空值和写入后回读。若新旧版本结果不同，沿配置注册、对象创建和查询调用路径定位差异。

### 网关内省 URI

启用 `lb://` 内省时，用实际部署的实例发现配置确认下游收到的原始路径和查询。在 Spring Cloud Commons `4.3.3` 与 `5.1.0` 的对照中，查询值内的裸 `=` 与 URI 其他位置已有的百分号转义同时出现时，LoadBalancer 重建 URI 会将已有的 `%` 再编码为 `%25`。例如将 `?x=a=b&scope=a%2Fb` 中查询值内的 `=` 写为 `%3D`，即 `?x=a%3Db&scope=a%2Fb`，两版均保留了该样本的编码。修改后复验下游实际收到的 URI 和内省结果。

### Redisson 消息与 Codec

Kit 的 `fist-redisson` 将 Redisson starter 声明为可选依赖。按应用实际启用的 Redis 拓扑核对
starter 和 Spring Data 适配器。已验制品从 Redisson `3.52.0` 与 `redisson-spring-data-35`
迁移到 Redisson `4.3.1` 与 `redisson-spring-data-40`。无 Redis 拓扑按实际依赖核对自动配置排除项。
Boot 4 验收使用 `org.redisson.spring.starter.RedissonAutoConfigurationV4` 与
`org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration`。

离线消费者使用模拟客户端核对默认通道名、自定义 `NameGenerator` 和 `Codec`、
队列与 Topic 配置绑定和回调。使用 `StringCodec` 和 `Kryo5Codec` 的固定合成样本双向互读通过。
应用启用 Redis 时，使用部署配置和代表性历史 payload 验证连接、实际 Codec、队列和 Topic 交付、
重连及下游业务回链。

### Trace Context、Jasypt 和 SDE

- Trace Context 按实际开启的 Servlet、WebFlux、Gateway 和 Feign 路径验收。发送带头和缺头请求，在接收端检查透传或生成行为。入站、出站头异名时，核对入口与路由使用同一个请求 ID。Feign relay 和关闭 Trace 的行为按应用配置验收。见 [Trace Context 示例](../../../examples/fist-trace-context/README.md)。
- Jasypt 用合成密文分别验证完整 `GMENC(...)` 和 `GMENC(${cipher.payload})`。检查 `Environment`、`@Value`、`@ConfigurationProperties` 实际读取结果，以及错误配置时的脱敏异常和替换有效配置后的恢复。若 `logback-spring.xml` 引用加密属性，直接检查 appender 初始化时收到的值。合成对照中，v3 的 appender 在日志重初始化后收到明文，v4 的 appender 只启动一次且收到密文，应用上下文中的 `Environment` 随后读到明文。涉及上游扩展的路径分别验收。[Jasypt 主密钥配置](jasypt-master-key.md) · [上游 Jasypt Boot 4.2 适配 PR #439](https://github.com/ulisesbocchio/jasypt-spring-boot/pull/439)
- 自定义 `StringEncryptor` 要替代 Kit 的默认 bean 时，使用名称 `jasyptStringEncryptor`。若仅通过 `jasypt.encryptor.bean` 指向异名 bean，两代样例的默认 bean 仍会初始化，仍需配置其主密钥。按实际 bean 名称验证启动和解密。
- SDE 按实际使用的 Feign、RestClient、WebClient 或 HTTP Service 分组代理走真实出站，核对两端 envelope、策略和解包结果。分组代理分别调用显式注解方法和未注解方法，检查组级请求头及 SDE 默认策略。v4 的两种代理已用合成订单通过四项回环对照。需要双向跨代互通时分别执行 v3 客户端到 v4 服务、v4 客户端到 v3 服务。按应用配置另验类型级策略及失败响应。见 [SDE 示例](../../../examples/fist-sde/README.md) 和 [SDE 协议](sde-protocol.md)。
