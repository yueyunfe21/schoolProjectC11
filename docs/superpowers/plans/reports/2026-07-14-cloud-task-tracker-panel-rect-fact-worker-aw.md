# Internal Worker AW - Cloud TaskTracker panel-rect fact

## Parent Direct Implementation Task - `W-TTPS-RECT-CLOUD-FACT-IMP1` - 2026-07-14T00:45:00-04:00

直接实现，不写 Design。把已父级批准的本地 `TaskTrackerPanelRectLocalObservationMechanics` 六态结果镜像为
Cloud closed `WINDOW_FACT`，供后续 `RemoteGameClientPort` typed 读取；不得改 TaskTracker 算法或发输入。

### 唯一写集

- Modify Cloud `remote/WindowFactKind.java`
- Modify Cloud `remote/WindowFact.java`
- Modify Cloud `remote/WindowFactOutcome.java`
- Modify Cloud `remote/RemoteCommandOutcomeEnvelope.java`
- Append-only 本报告

共享工作区有其它 Worker；不得回滚、覆盖、清理、删除或提交他人 dirty/untracked。

### 实现合同

1. 新 fact kind `TASK_TRACKER_PANEL_RECT`；sealed fact 新增 `TaskTrackerPanelRectFact`，字段恰为
   `state, anchorClientX, anchorClientY, panelClientLeft, panelClientTop, panelClientRight,
   panelClientBottom, matchScore, coordinateSpace`。
2. closed state 恰为 `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/
   REPOSITION_REQUIRED/MECHANICS_FAILED`；coordinateSpace 恒 `WINDOW_CLIENT_PX`。
3. 仅 PRESENT 携全部 7 个 observation 字段；坐标 non-negative，rectangle right>left/bottom>top，
   score non-null finite。其它状态 7 字段全 null。协议不得写死 0.82，不新增 threshold/timestamp。
4. 补齐 `WindowFactOutcome` kind/type 对应和 `RemoteCommandOutcomeEnvelope` exhaustive parse；不改 digest、
   transport、ledger、input queue、TaskTrackerPanelService 或其它协议。
5. 不新增 retry/TTL/owner/session/permit/thread/运行入口。

完成后 Cloud 运行 `mvn -q package`（不 clean），向 EOF 追加 CLAIMED、Implementation #1、四文件 SHA、
构建结果与 self-QA。自审不算父级 Approved。

保护全部 dirty/untracked；禁止 Git mutation、禁止启动运行面。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED - Internal Worker AW - 2026-07-14T00:44:58-04:00

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md` 与本固定任务日志。
- 仅认领 `W-TTPS-RECT-CLOUD-FACT-IMP1`；将严格限定四个 Cloud `remote` 源码文件与本日志 EOF 追加。
- 已确认 Cloud 共享工作树存在他人 dirty/untracked；不回滚、不覆盖、不清理、不删除、不提交、不做 Git mutation。

## Implementation #1 - Internal Worker AW - 2026-07-14T00:49:38-04:00

### 实现

- `WindowFactKind` 新增 `TASK_TRACKER_PANEL_RECT`。
- sealed `WindowFact` 新增 `TaskTrackerPanelRectFact`，字段严格为
  `state, anchorClientX, anchorClientY, panelClientLeft, panelClientTop, panelClientRight,
  panelClientBottom, matchScore, coordinateSpace`；新增六态 `TaskTrackerPanelRectState`。
- `TaskTrackerPanelRectFact` 强制 `coordinateSpace == WINDOW_CLIENT_PX`；仅 `PRESENT` 允许并要求
  全部 7 个 observation 字段，校验坐标非负、矩形 `right > left` / `bottom > top`、
  `matchScore` non-null finite；其它五态强制 7 字段全 null。
- `WindowFactOutcome.matches(...)` 与 `RemoteCommandOutcomeEnvelope.parseFact(...)` 已补齐新 kind/type
  对应和 exhaustive parse。
- 未写死 `0.82`，未新增 threshold/timestamp/retry/TTL/owner/session/permit/thread/运行入口；
  未修改 digest、transport、ledger、input queue、`TaskTrackerPanelService`、tests、pom 或 schema。
- 无已批准业务差异；按 `0114604e` 基线等价迁移。

### 四文件 SHA-256

- `WindowFactKind.java`: `78A217C8595D686CD32711A4C5816E2A43EA975F86E147D4BE3EA8083DAC467F`
- `WindowFact.java`: `0608575F2F1C3440097E391336CB02906C02DA5F756875DDB17639BA79B1AE9D`
- `WindowFactOutcome.java`: `EC958D47592AC7980F529D8A0EFA75A7EB6A80144047C42425CC4074DE82A53D`
- `RemoteCommandOutcomeEnvelope.java`: `5AF1BFF2521C01BC60D8A793B5FBF656ED68221D45685EF02C175E6F5B8908B2`

### 构建与 self-QA

- Cloud 根目录执行精确命令 `mvn -q package`（未 `clean`）：退出码 `0`，成功，耗时约 `71.2s`。
- 已重读新 fact 记录、六态枚举、kind/type 映射与 parse switch；字段顺序、空值矩阵、
  坐标空间和几何/finite 校验均与本日志合同一致。
- 已复核 Cloud `git status --short --branch`；他人原有 dirty/untracked 保持，本轮未做任何
  Git mutation，未启动任何运行面。
- self-QA 仅为 Worker 自审，不算父级 `Approved`。

## Parent Source Review #1 - APPROVED - 2026-07-14T00:57:00-04:00

父级逐行复核四个 Cloud remote 文件并复算合同，结论 `P0=0 / P1=0 / P2=0`：

- `TASK_TRACKER_PANEL_RECT` 已进入 closed kind 与 sealed fact；字段、六态和 `WINDOW_CLIENT_PX`
  与已批准 AV/AX 完全一致。
- 仅 PRESENT 要求全部七个 observation 字段、non-negative 坐标、正尺寸 rectangle 与 finite score；
  其它五态严格清空全部 observation 字段。协议未写死 0.82，也没有 timestamp/threshold 副本。
- `WindowFactOutcome.matches(...)` 和 `RemoteCommandOutcomeEnvelope.parseFact(...)` 都有唯一穷尽分支；
  digest、transport、ledger、input queue 与 TaskTracker 算法未变。
- Worker Cloud `mvn -q package` exit 0。父级仍会在本波 Java 全部稳定后运行 fresh `mvn -q clean package`。

本 Cloud fact `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
