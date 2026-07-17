# TURN-03A Report - Cloud Template Catalog

## CLAIMED

- 领取时间：`2026-07-15T14:34:12-04:00`
- 状态：`CLAIMED`
- 角色：Internal implementation worker；不是 manager/reviewer，不自批。
- `countUnit`：`N/A (INFRA Cloud template authority)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-00`，其固定报告已写明
  `PARENT APPROVED，P0/P1/P2=0，card CLOSED`。
- 精确写集：
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTemplateCatalog.java`
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-turn-card-TURN-03A.md`
- 只读复用：
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\PackagedTemplateAssets.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\host\CloudTemplateAssets.java`
- 禁止触碰：两仓上述精确写集之外的所有文件；尤其不修改主计划、CR271、`ACTIVE_WORK.md`、dashboard、
  `CloudTemplateHttpHandler.java`、server routes、两个只读资产文件及任何协议 DTO。
- 实施边界：只建立 `templateKey -> PNG bytes + SHA-256 contentHash + ETag` 的单一只读权威；不提供路径、
  listing/枚举、目录扫描、缓存目录、owner/permit/session/ledger/compaction/durable workflow、业务 TTL 或自动 retry。

## 领取基线与两仓状态

### DHXY

- 分支/HEAD：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`。
- 当前分支无 upstream；远端可见基线 `origin/master=0468cc101b383700e224e7e4bf2fee551de930f1`。
- `git status --porcelain=v1`：领取时已有 `config/`、`docs/`、`pom.xml`、23 个既有 Java 文件的 modified/deleted，
  以及计划、报告、spec、模板和多个 Java 目录的 untracked；本报告所在 `plans/reports/` 已是既存 untracked 目录。
- 本卡保护全部既存 dirty/untracked，不回滚、不覆盖、不清理、不提交；DHXY 除本报告外不写。

### dhxy-cloud-brain

- 分支/HEAD：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`。
- 当前分支无 upstream；远端可见基线 `origin/main=48e3781`。
- `git status --porcelain=v1`：领取时已有 `pom.xml`、`CloudBrainServer.java`、`DecisionEngine.java`、
  `ImageAlgorithms.java`、`ObjectiveTextRecognizer.java`、`SmartClickRecognizer.java`、
  `gateway/CloudApiGateway.java`、`src/main/resources/config/maps.json` modified；另有 `logs/`、migration 目录、
  `com/bot/`、host/remote 源码、模板 PNG 等既存 untracked。
- 精确基线证据：目标 `turn/CloudTemplateCatalog.java` 在 HEAD 与工作树均不存在；
  `PackagedTemplateAssets.java`、`host/CloudTemplateAssets.java` 均为领取前已有 untracked，只读消费且不修改。
- 本卡保护全部并发 dirty/untracked，不回滚、不覆盖、不清理、不提交。

## 验证约束

- 不运行 runtime/application/server/Task/poller/UI/capture/input/tests。
- Cloud Java writers 稳定时运行 `mvn -q clean package`；若仍存在并发 writer，则记录精确 status 和未构建原因，
  把 fresh Maven 门留给 Foundation build cohort/父级。
- 交付后只写本报告 `DELIVERED` 与证据，然后停止等待父级审查。

## DELIVERED

- 交付时间：`2026-07-15T14:37:34-04:00`
- 状态：`DELIVERED / CLOUD PACKAGE BLOCKED BY PRE-EXISTING INCOMPLETE MIGRATION SOURCES`
- 完成内容：
  - 新增唯一 `CloudTemplateCatalog`，仅通过构造注入的 `CloudTemplateAssets` 读取一个 canonical
    `images/template/...png` key；未新建第二个 classpath/filesystem loader。
  - 同一次 catalog resolve 从一份 PNG bytes 计算 lowercase SHA-256、`sha256:<hex>` content hash 与
    精确带引号 ETag；成功条目按 key 单次发布，action 与后续 GET consumer 可读取同一权威内容。
  - `TemplateContent` 不允许外部构造，PNG bytes 在构造与读取时均防御性复制，外部不能改写 catalog 权威。
  - key 在读取资产前严格拒绝 null/空白、前后空白、错误前后缀、`..`、反斜杠、非 allowlist 字符、
    空 segment、`.`/`..` segment 与空 PNG 文件名；API 不暴露路径、list、walk 或目录枚举。
- 精确源码证据：
  - `CloudTemplateCatalog.java` SHA-256：
    `D21123B7378826E622979EB5C8A4883F8FF52B63BC1C1EC621DC6FB57417E663`。
  - 只读 `PackagedTemplateAssets.java` SHA-256：
    `5A7000814D3735ABF9CF699CE42D5D9A43BE76337D4EC6D892887A784F823DA7`；仍为领取前既有 untracked。
  - 只读 `host/CloudTemplateAssets.java` SHA-256：
    `59F44AD14B6D7519388A226FD653C0B9E537A49EAF5EF4BD89A916CB3CB0204E`；仍为领取前既有 untracked。
  - scoped 禁用能力扫描 `java.nio.file|java.io.File|.list(|.walk(|DirectoryStream|owner|permit|session|ledger|
    compaction|durable|TTL|@Scheduled|@PostConstruct|retry` 对本卡 Java 源码无命中。
  - scoped status 仅新增本卡 `CloudTemplateCatalog.java`；两个资产文件状态/hash 未被本卡改变。
- Maven 门：已在 Cloud 稳定写入窗口执行精确命令 `mvn -q clean package`，`23.9s` 后 exit `1`，失败于
  `maven-compiler-plugin` 的 `compile` 阶段，tests 尚未开始执行。
- 构建阻断证据：编译错误来自领取前已存在的 untracked 迁移源码：
  - `com/bot/dhxy/service/TaskTrackerPanelService.java` 缺少 `GameClientTracker`、`TextRecognizer`、
    `CoordinateHelper`、`OcrWindowScanService`、`WindowScopedTempPath`；
  - `com/bot/dhxy/task/wubei/WubeiTask.java` 缺少 `GameClientTracker`、`BagService`、`UICleanerService`、
    `TaskTransactionRunner`、`TaskTurnCoordinator`、`WindowRuntimeContext` 等；
  - 既存 `NavigationService.java`、`NpcClickService.java`、`SummonSkillService.java` 也缺尚未迁入的本地依赖。
  上述五个文件在领取前与构建后均为 untracked，本卡未修改；错误输出未指向 `CloudTemplateCatalog.java`。
- 未越界修复这些并发迁移依赖，未修改 Maven/config/server/handler/route/protocol/CR 文档；未回滚、覆盖、
  清理或提交两仓任何既存 dirty/untracked。
- 未运行 runtime/application/server/Task/poller/UI/capture/input；Maven 在 compile 失败，tests 未执行。
- `countUnit`：`N/A (INFRA Cloud template authority)`；`countDelta=0`，不产生 407 ledger 增量。
- 业务差异：无已批准业务差异；按基线等价迁移。
- 下一步：停止实现并等待父级独立源码审查与 Foundation build cohort 门；本 worker 不自批、不写
  `APPROVED/CLOSED`。

## PARENT SOURCE REVIEW

- 审查时间：`2026-07-15T10:39:00-04:00`
- P0：`0`
- P1：`0`
- P2：`0`
- 源码证据：父级独立展开 `CloudTemplateCatalog.java`，确认只接受 canonical
  `images/template/...png` key，不暴露 path/list/walk；同一成功 resolve 的 PNG bytes 同时产生 lowercase SHA-256、
  `contentHash` 与 quoted ETag，返回 bytes 防御性复制；无第二模板权威、TTL、retry 或 durable state。
- 构建判断：worker 的 Cloud package exit 1 仅来自领取前已有的未跟踪 Task/Service 缺依赖，编译错误未指向本卡；
  不允许越界修复。最终 Maven 仍归 Foundation cohort 稳定窗口。
- 影响：为 TURN-03B 与 action factory 提供单一模板权威；不改变业务或 407 ledger。
- 返修条件：无。

**SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING；源码 owner 已释放，可领取 TURN-03B。**
