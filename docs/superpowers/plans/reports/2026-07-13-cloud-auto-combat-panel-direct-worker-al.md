# Internal Worker AL - Direct AutoCombatPanelService Migration

## Parent Direct Implementation Task / `W-ACP-DIRECT-IMP1` - 2026-07-13T20:20:00-04:00

直接实现，不写 Design。先完整读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、
`docs/superpowers/plans/2026-07-13-direct-service-input-bundle-migration.md`、本报告，以及 committed
`0114604e` 的 `AutoCombatPanelService.java`。保护两仓全部 dirty/untracked，不回滚、覆盖、清理、重命名
或提交他人改动；已有 `AutoCombatPanelDecision`、warning/config/state 材料只能复用，不能推翻或扩张。

目标：直接迁移同包同名 Cloud `AutoCombatPanelService`，保留 baseline public API、判断/调用顺序、
delay、fallback、round 估算、missing 记录与 burst guard。仅把本地 panel template/窗口点读取换成一个
closed typed `AUTO_COMBAT_PANEL` window fact，把 Alt+8/drag bundle 换成 D 的 `InputSequences`。
不得新增 per-Service owner/permit/ledger/parent-child/compaction/TTL/retry/线程/轮询/host/caller。

唯一 Java 写集：

- Cloud New `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`。
- Cloud Modify `WindowFactKind.java`、`WindowFact.java`、`WindowFactOutcome.java`、
  `RemoteCommandOutcomeEnvelope.java`。
- DHXY Modify `RemoteWindowFactKind.java`、`LocalRemoteGameCommandHandler.java`、
  `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`；DHXY New 一个最小 immutable
  `RemoteAutoCombatPanelFact`（放 `com.bot.dhxy.cloud.remote`）。
- 本报告 append-only。

实现约束：

- 本地只增加 read-only typed probe，必须复用 baseline 既有 `findAutoCombatBox()` 及坐标计算，不复制
  template/ROI/threshold 决策，不点击、不改原 public flow、round/missing/metrics 状态。
- fact closed 字段只表达 `FOUND`/`NOT_FOUND`/`CAPTURE_FAILED`、panel center、green marker、
  greenTemplateWidth、detectionSource 与 `SCREEN_ABSOLUTE_PX`；只有 FOUND 可带坐标。禁止 raw path、图片 bytes、
  template 内容或本地 HWND 对象上 wire。
- handler 在 exact admitted `BindingAccess` + `WindowTaskContextHolder.callWith` 内 probe，并保留现有执行后
  registration/binding/runRevision fence；不得 title-search。
- Cloud Service 用 per-run constructor 注入 exact `TaskExecutionContext`、`GameContext`、
  `CloudAutoBattleProperties`、正 `timeoutMs`，以及已有 warning/metrics seam 能表达的最小 powerless callback；
  不创建 Spring singleton，不修改 assembly/caller。调用 AK 约定的 `context.getGameClient()`，不得编辑 context。
- 三处 baseline 物理序列保持原 action 顺序和 delay，一次调用 D `InputSequences`；不得自动 retry。
- 复用 `AutoCombatPanelDecision` 纯决策时，必须证明与 baseline 字节/分支等价；不得保留两套冲突实现。
- 不修改 AK、B、D、AI、AJ 所有文件，不碰 assembly/host/tests，不新增/恢复测试。

完成后向本日志真实 EOF 追加 `Internal Worker AL - W-ACP-DIRECT-IMP1 Implementation #1`，列精确文件、
wire 对称性、baseline 对照和双仓编译状态。B/D 接缝未修时只记 `WAITING_DEPENDENCY`，不扩大写集。
Java 稳定且依赖已齐时运行 DHXY `mvn -q -DskipTests compile`、Cloud `mvn -q compile`（都不 clean）；
父级统一跑 Cloud clean package。自审只算 QA。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED

- Worker: Internal Worker AL
- Task: `W-ACP-DIRECT-IMP1`
- Claimed at: 2026-07-13

## Internal Worker AL - W-ACP-DIRECT-IMP1 Implementation #1

- Completed at: `2026-07-13T20:35:50-04:00`
- Baseline: DHXY branch `thin-client-design`, HEAD `0114604e`;实现前已执行
  `git show 0114604e:src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java` 并逐段对照。
- Cloud workspace: branch `navigation-migration`, HEAD `3b988ca`；全程保护两仓既有 dirty/untracked，
  未 reset/checkout/clean/delete/rename/commit，未修改唯一写集外文件。

### 精确变更

Cloud：

- New `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  - per-run constructor 注入 exact `TaskExecutionContext`、`GameContext`、
    `CloudAutoBattleProperties`、正 `timeoutMs` 与
    `Consumer<RuntimeWarningNotification>` powerless callback；通过 AK
    `context.getGameClient()` 构造 D `InputSequences`，无 Spring singleton/线程/轮询/retry。
  - panel 读取改为 `AUTO_COMBAT_PANEL` typed fact；window base 改读既有 `GEOMETRY` fact；
    非 `OBSERVED` transport/fence 结果直接上抛，不伪装成 `NOT_FOUND`，不触发自动重试。
  - missing transition、round reason、team burst guard 分别委托既有
    `AutoCombatPanelDecision`，warning 使用既有 `RuntimeWarningNotification`/
    `RuntimeWarningIdentity`；未新增第二套冲突决策。
- Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/WindowFactKind.java`
  - 增加 `AUTO_COMBAT_PANEL`。
- Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/WindowFact.java`
  - sealed permits 增加 `AutoCombatPanelFact`；增加 closed fact 与
    `AutoCombatPanelState`，校验状态、坐标 pair、非负 width、非空 source 和
    `SCREEN_ABSOLUTE_PX`。
- Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/WindowFactOutcome.java`
  - 增加 kind/variant 对应校验。
- Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
  - `parseFact` 增加 `AUTO_COMBAT_PANEL -> AutoCombatPanelFact` typed 解码。

DHXY：

- Modify `src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java`
  - 增加 `AUTO_COMBAT_PANEL`。
- New `src/main/java/com/bot/dhxy/cloud/remote/RemoteAutoCombatPanelFact.java`
  - 最小 immutable closed DTO 与 constructor validation；无 raw path、图片 bytes、template
    内容或 HWND 对象。
- Modify `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  - 仅新增 read-only `probeAutoCombatPanelFact()`；直接调用既有
    `findAutoCombatBox()`，复用原 capture/template/`0.80` threshold 与 panel-center 坐标计算；
    不点击，不修改原 public flow、round/missing/metrics/runtime state。
- Modify `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
  - constructor 注入既有本地 `AutoCombatPanelService`；`AUTO_COMBAT_PANEL` case 仅在已准入
    `BindingAccess` 上通过 `WindowTaskContextHolder.callWith(access.context(), ...)` 执行 probe；
    原执行后 timeout、registration/runRevision、bound-window fence 原样保留，无 title-search。

### Wire 对称性

- Cloud/DHXY kind 均为 `AUTO_COMBAT_PANEL`；state 均且仅为 `FOUND`、`NOT_FOUND`、
  `CAPTURE_FAILED`。
- 双端字段名称、类型和顺序一致：`state`、`panelCenterX`、`panelCenterY`、
  `greenMarkerX`、`greenMarkerY`、`greenTemplateWidth`、`detectionSource`、
  `coordinateSpace`。
- 双端 null matrix 一致：panel center 与 green marker 均要求 X/Y 成对；仅 `FOUND` 必须携带
  panel center 且允许携带 marker；非 `FOUND` 禁止所有坐标并要求 width 为 `0`。
- 双端均要求 `greenTemplateWidth >= 0`、非空 `detectionSource`、且 coordinate space 精确为
  `SCREEN_ABSOLUTE_PX`。Cloud outcome 另校验 kind/variant；非 `OBSERVED` outcome 不携带 fact。

### `0114604e` Baseline 对照

- public API 保留：两种 `verifyAndAlignPanel`、`ensurePanelVisible`、
  `resolveRoundsRefreshReason`、`recordCombatExit`、`PanelVerifyMode`、
  `RoundsRefreshReason`、`RefreshDueBurstDecision`、`TeamRefreshDueBurstGuard`。
- `ensurePanelMatchVisible` 顺序不变：首次 probe -> 缺失时一次 open bundle -> 第二次 probe ->
  成功后 reset rounds/clear missing；input failure 与 second miss 的 reason 字符串不变。
- 三个物理 bundle 与 baseline 值/顺序一致且各只调用一次 D `InputSequences`：
  1. `PRESS_ALT_8 -> SLEEP(waitAfterOpenMs)`；
  2. `DRAG_AND_DROP(panelX,panelY,baseX+489,baseY+726) -> SLEEP(500)`；
  3. `PRESS_ALT_8 -> SLEEP(1000)`。
- 对齐距离 `> 20.0`、drag 后复探、`drag-target-fallback`、默认 rounds `25`、低回合阈值
  `10`、每场扣减 `3`、refresh interval `Math.max(0L, configured)`、missing 10 分钟/
  60 秒重复告警和 team 30 秒 burst guard 均保持原值与分支顺序。
- `AutoCombatPanelDecision.resolveRoundsRefreshReason`、`recordMissing`、`clearMissing` 与
  `TeamRefreshDueBurstGuard` 是 `0114604e` 对应代码的纯提取；Cloud service 只做类型映射、日志与
  side-effect 承接，没有保留第二份条件实现。
- 无已批准业务差异；按 `0114604e` 基线等价迁移。

### Compile / QA 证据

- DHXY：`mvn -q -DskipTests compile`，exit `0`，当前源码编译通过（约 `22.2s`）。
- Cloud：实现后 `mvn -q compile`，exit `0`；transport/fence 收紧后再次执行同命令，exit `0`
  （最终一次约 `15.4s`）。
- 按 no-local-test 与任务禁令未新增、恢复或运行 tests/source guards/replay；未启动 runtime、
  application、Task、UI 或 input。
- `WAITING_DEPENDENCY`: 不适用。External B mapper、D `InputSequences` import 与 AK
  `context.getGameClient()` 均已在当前源码可用，双仓 compile 已证明接缝闭合。

### Final verification rerun

- 报告与源码稳定后再次执行 DHXY `mvn -q -DskipTests compile`：exit `0`（约 `3.1s`）。
- 随后再次执行 Cloud `mvn -q compile`：exit `0`（约 `3.7s`）。

## Parent Source Review #1 - APPROVED - 2026-07-13T20:43:00-04:00

父级独立对照 committed `0114604e` 的 `AutoCombatPanelService`、当前 Cloud 实现、双端 fact DTO、
`LocalRemoteGameCommandHandler` 与 outcome 解码链。结论：`P0=0 / P1=0 / P2=0`。

- 原 public API、判断阈值、missing/warning 时序、round 扣减与 team burst 条件均保持；三个物理动作仍分别是
  原顺序的单次有序 bundle，未添加 retry、TTL、额外验证或业务 fallback。
- capture/template 匹配仍由 DHXY 既有 `findAutoCombatBox()` 执行；wire 只传 closed typed fact，不传图片、
  模板、路径或 HWND。双端 kind/state/字段/null matrix/`SCREEN_ABSOLUTE_PX` 校验一致。
- `UNKNOWN`、`NOT_EXECUTED`、stale revision 或中断不会被伪装成 `NOT_FOUND`，因此不会在不确定结果后额外发送
  Alt+8/拖拽；真正的 `NOT_FOUND`/`CAPTURE_FAILED` 仍沿用 baseline 的返回 null 分支。
- `AUTO_COMBAT_PANEL` 的 outcome 解码进入 sealed fact，并由 `WindowFactOutcome` 校验 kind/variant；调用均经 exact
  `TaskExecutionContext` 的 `CloudGameClient` 与 retained stable address。

结论：`W-ACP-DIRECT-IMP1 SOURCE APPROVED`。Worker 的双仓 compile 均为 exit 0；父级下一步执行 fresh DHXY
compile 与 Cloud clean package。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
