# TURN-01A Report - 双仓 core protocol types

## CLAIMED

- 领取时间：`2026-07-15T14:34:00.2242220-04:00`
- 状态：`CLAIMED`
- 角色：Internal implementation worker；不是 manager/reviewer，不自批。
- `countUnit`：`N/A (INFRA core protocol types)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-00`（已由父级写明 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`）
- `approvalDependsOn`：`TURN-01B`、`TURN-01C`、`TURN-01D`
- 业务差异：无已批准业务差异；按基线等价迁移。

## Parent Scope Amendment

- 父级在领取过程中补充 TURN-01A 合同：除原五个 core type 外，两仓新增 byte-identical
  `TurnWindowMetadata.java` 与 `TurnFramePurpose.java`。
- `TurnWindowMetadata` 字段固定为：`String deviceId, String windowId, String windowTitle, String nativeHandle,
  long processId, TurnWindowRect windowRect, boolean stopRequested`。
- `TurnFramePurpose` 枚举固定为：`CAPTURE, MATCH_EVIDENCE, QUEST_DETAIL, FAILURE_EVIDENCE`。
- 不自行增删字段、枚举值、协议层、owner/permit/session/ledger/compaction/durable workflow/business TTL 或自动 retry。

## 精确写集

DHXY：

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnStepType.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnRegion.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowRect.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnFramePurpose.java`
- `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-01A.md`（本报告，只允许追加后续状态/证据）

Cloud Brain：

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnStepType.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnRegion.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowRect.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnFramePurpose.java`

禁止触碰：两仓任何其它 protocol、`cloud/remote/**`、Service、server、runner、Maven/config 文件，以及主计划、
CR271、`docs/ACTIVE_WORK.md`、dashboard。不得回滚、覆盖、清理或提交任何既有 dirty/untracked。

## 两仓 Status 与基线

### DHXY

- 当前分支：`thin-client-design`
- HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- 当前分支无 upstream；最新远端参考 `origin/master`：`0468cc101b383700e224e7e4bf2fee551de930f1`。
- 领取时 `git status --short --branch`：存在大量父级/其它 worker 的既有 dirty/untracked，包括 config、docs、
  `pom.xml`、input/service/task 源码、`cloud/remote/`、多个本地 mechanics/model 目录及模板；全部保护，不处置。
- 目标路径基线：上述 7 个协议文件在 working tree、HEAD、`origin/master` 均不存在；本卡报告在领取前也不存在。

### Cloud Brain

- 当前分支：`navigation-migration`
- HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- 当前分支无同名远端；最新远端参考 `origin/main`：`48e37813913094cacd1143fcae02704033eecb93`。
- 领取时 `git status --short --branch`：既有 `pom.xml`、server/engine/algorithm/gateway/maps 修改，以及
  `logs/`、migration 目录、既有 `com/bot/`、turn/host/remote 源码、模板等未跟踪内容；全部保护，不处置。
- 目标路径基线：上述 7 个协议文件在 working tree、HEAD、`origin/main` 均不存在。

## 执行边界

- 严格消费 TURN-00 已锁定值：五种 step、十种 input action、九个 local operation，以及两个不缩放坐标 record。
- 严格消费本报告记录的 parent scope amendment，新增固定 metadata record 与 frame-purpose enum。
- 只做源码静态核对、两仓逐文件 SHA-256/byte parity 与 `git diff --check`。
- 不运行 runtime/application/server/Task/poller/UI/capture/input/tests。
- 本卡属于 Foundation build cohort；若其它协议切片尚未稳定导致整仓编译不可用，交付标记
  `SOURCE DELIVERED / BUILD COHORT PENDING`，不伪造 Maven 通过。
- 交付后停止，等待父级独立审查。

## DELIVERED

- 交付时间：`2026-07-15T14:35:37.2392918-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- 完成内容：
  - 两仓 byte-identical 新增 `TurnStepType`，值严格为 `CAPTURE`、`MATCH_TEMPLATE`、`INPUT`、`WAIT`、
    `LOCAL_SERVICE`。
  - 两仓 byte-identical 新增 `TurnInputAction`，包含 TURN-00 锁定的全部十种 input action，无增删。
  - 两仓 byte-identical 新增 `TurnLocalOperation`，包含 TURN-00 锁定的九个 local operation，无增删。
  - 两仓 byte-identical 新增 `TurnRegion(int x, int y, int width, int height)` 与
    `TurnWindowRect(int left, int top, int width, int height)`，未添加缩放或额外语义。
  - 按 parent scope amendment，两仓 byte-identical 新增固定七字段 `TurnWindowMetadata` 与固定四值
    `TurnFramePurpose`，未自行增删字段或枚举值。
- 逐文件 byte parity / SHA-256：
  - `TurnStepType.java`：`68F1FE11A204D86E028C935BBD0D11178C4E3799D4AAE40A283EDCC151336703`
  - `TurnInputAction.java`：`B727EC909648FA7232AD4DA1C53C75D1FB9F01BB1C16168E22D483599D3C8067`
  - `TurnLocalOperation.java`：`A70DBFA3B60F681776D70D9DEAC518BD4AB3B0B69F5B12E379DD19197583FBD8`
  - `TurnRegion.java`：`F9E0252A367AE18A43115EC5169CB47CF6DD8AA72D1F88EC2C685061F8D707CF`
  - `TurnWindowRect.java`：`5E9AB2FEA88C594F091D7D0E9197EAFE6006402DBB3FFA4789CCBD27338C03B0`
  - `TurnWindowMetadata.java`：`4112B3A01BD1F551DE50E7205E4F74324F5DE29504D96A81E3948A7782ABF4A0`
  - `TurnFramePurpose.java`：`90574E7FAFF4EC8790E43AAEA358C7963C3D198EF1E81321C125E07369C164A1`
  - 每一对均为 `ByteIdentical=True`；两仓对应文件 hash 完全一致。
- 静态证据：目标源码逐文件人工展开核对；trailing whitespace `0`；禁用 machinery 词项
  `owner|permit|session|ledger|compaction|durable|TTL|retry` 命中 `0`。
- 精确 scope 证据：DHXY status 只显示本报告与新 protocol 目录为未跟踪；Cloud Brain status 只显示新 protocol
  目录为未跟踪。本 worker 未修改本卡写集外文件，未回滚、覆盖、清理或提交两仓任何既有 dirty/untracked。
- 构建状态：未运行 Maven。`TURN-01B`、`TURN-01C`、`TURN-01D` 仍是本卡
  `approvalDependsOn`，Foundation Java writers/build cohort 尚未稳定；按主计划只交付 source，不伪造整仓构建通过。
- 未运行：runtime/application/server/Task/poller/UI/capture/input/tests。
- `countDelta`：`0`；未产生 407 ledger 增量。
- 业务差异：无已批准业务差异；按基线等价迁移。
- 下一步：停止实现并等待父级独立源码审查、cohort Maven 门与裁决；本 worker 不自批。

## PARENT SOURCE REVIEW

- 审查时间：`2026-07-15T10:37:00-04:00`
- P0：`0`
- P1：`0`
- P2：`0`
- 源码证据：父级独立展开两仓 14 个文件，五类 step、十类 input、九个 local operation、两个坐标 record、
  七字段 `TurnWindowMetadata` 与四值 `TurnFramePurpose` 均逐项符合锁定合同；七对 SHA-256 全部相等，
  无缩放、retry、owner/session/ledger 等额外逻辑。
- 写集/影响：只新增 TURN-01A 七类及本报告；形成后续 DTO 的共享基础，不改变业务、runtime 或 407 ledger。
- 构建：按 Foundation build cohort 等待 TURN-01B/01C/01D 稳定后由父级统一双仓 Maven；当前不得写 CLOSED。
- 返修条件：无。

**SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING；源码 owner 已释放，可领取下一张 READY。**
