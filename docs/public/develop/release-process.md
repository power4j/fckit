# 版本发布流程

## 概述

本项目使用 `${revision}` 统一管理版本号，通过 Git tag 触发 GitHub Actions 自动发布到 Maven Central。

## 分支策略

4.x 线在 `master` 单线开发：`master` 同时是日常开发分支与发布基线，每次发布打 tag。

| 分支 | 用途 |
|---|---|
| `master` | 开发与发布基线（单线），每次发布打 tag |
| `snapshot` | 推送到此分支自动发布 SNAPSHOT |
| 功能分支 | 从 `master` 拉出，完成后合并回 `master` |
| `v3` | v3 线（`com.power4j.fist3`）历史归档，不再更新 |

## CI 触发规则

| 触发条件 | 执行动作 |
|---|---|
| push 到 `master` | 编译 + 测试（ci.yml） |
| push 到 `snapshot` 分支 | 发布 SNAPSHOT 到 Maven Central（oss-deploy.yml） |
| push 任意 tag | 发布正式版到 Maven Central（oss-deploy.yml） |

---

## 正式版发布流程

以发布 `X.Y.Z`（含预发布后缀，如 `4.0.0-rc1`）为例，当前开发版本为 `X.Y-SNAPSHOT`。项目根目录的 `release.sh` 已自动化以下全部步骤，直接运行按提示输入版本号即可；以下为手动流程。

### 第一阶段：发布正式版

**Step 1：确保 master 测试全部通过**

```bash
git checkout master
git pull origin master
mvn test
```

**Step 2：修改版本号为发布版**

编辑根 `pom.xml`：

```xml
<revision>X.Y.Z</revision>
```

**Step 3：提交版本变更**

```bash
git add pom.xml
git commit -m "chore: bump version to X.Y.Z"
```

**Step 4：打 tag 并推送（触发自动发布）**

```bash
git tag vX.Y.Z
git push origin master
git push origin vX.Y.Z
```

推送 tag 后，GitHub Actions 的 `oss-deploy.yml` 会自动执行 GPG 签名并发布到 Maven Central。

---

### 第二阶段：开启下一开发版本

**Step 5：master 上升版本为下一 SNAPSHOT**

编辑根 `pom.xml`：

```xml
<revision>X.Z-SNAPSHOT</revision>
```

> GA 版本发布后通常 Z = 下一维护位，例如发布 4.0.0-rc1 后保持 4.0.0-SNAPSHOT 至 GA，GA 后升为 4.1.0-SNAPSHOT。

**Step 6：提交并推送**

```bash
git add pom.xml
git commit -m "chore: bump version to X.Z-SNAPSHOT"
git push origin master
```

---

## 流程图

```
功能分支 ──merge──► master
                 │
      [修改版本 X.Y.Z，打 tag]
                 │
           vX.Y.Z ──push──► GitHub Actions 自动发布
                 │
      [修改版本 X.Z-SNAPSHOT]
                 │
           push master
```

---

## 注意事项

- 打 tag 前必须确认 `pom.xml` 中版本号已改为正式版（不含 `-SNAPSHOT`）
- `examples/` 和 `benchmarks/` 模块配置了 `maven.deploy.skip=true`，不会被发布
- 发布需要仓库中配置以下 Secrets：`MAVEN_GPG_KEY`、`MAVEN_GPG_PASSPHRASE`、`MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`
- 脚本自动化见项目根目录 `release.sh`，直接运行后按提示输入版本号即可：
  ```bash
  ./release.sh
  # 提示输入发布版本（如 4.0.0-rc1），脚本自动添加 v 前缀生成 tag
  # 提示输入下一版本基础号（如 4.0.0），脚本自动添加 -SNAPSHOT 后缀
  ```
