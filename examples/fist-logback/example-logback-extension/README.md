# example-logback-extension

演示通过 SPI 机制扩展 fist-logback，实现自定义的身份证号检测器和脱敏转换器。

## 扩展点

- `IdCardDetector` / `IdCardDetectorProvider` 识别前 17 位为数字、末位为数字或 `X`、`x` 的 18 字符片段。
- `IdCardTransformer` / `IdCardTransformerProvider` 保留前 6 位和后 4 位，中间替换为 8 个 `*`。例如，避开手机号模式的合成证件号脱敏后为 `000000********000X`。

## 规则重叠时的输出

示例先执行手机号规则（`order=0`），再执行身份证规则（`order=1`）。示例中的证件号包含匹配手机号模式的 11 位片段。手机号规则先遮盖其中 4 位，身份证规则随后无法匹配完整的 18 字符片段。实名认证日志中的合成证件号实际显示为 `110101199****11234`。

规则配置见 [logback-masking.properties](src/main/resources/logback-masking.properties)。

## 运行

```bash
mvn spring-boot:run -Pexamples -pl examples/fist-logback/example-logback-extension
```
