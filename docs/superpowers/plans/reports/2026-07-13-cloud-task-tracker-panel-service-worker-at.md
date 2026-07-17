# Internal Worker AT - TaskTrackerPanelService Cloud Algorithm Extraction

## Parent Task Brief #1 - 2026-07-13T23:22:00-04:00

### Objective

Move the already-running `TRACKER_PANEL_READER` algorithms out of the Cloud `DecisionEngine` monolith into a real
Cloud `TaskTrackerPanelService`, without changing protocol, behavior, thresholds, ordering, diagnostics, or results.
This is direct implementation, not a new design round.

### Business authority

- DHXY committed baseline: `0114604e1ff5f15491d2910959c45252e893d04f`.
- Existing Cloud `DecisionEngine.trackerPanelReader(...)` and all of its tracker-specific helpers are the current
  migrated behavior to preserve exactly.
- User placement: tracker algorithms belong in Cloud; DHXY retains exact-window capture/template/OCR mechanics and
  physical input execution.

### Exclusive write set

- New: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\TaskTrackerPanelService.java`
- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`
- Append only: this report.

Do not modify DHXY Java, Maven files, tests, schemas, wire DTOs, controllers, or any other Cloud file. Other workers
may have dirty/untracked work in both repositories; never reset, checkout, clean, delete, overwrite, or commit it.

### Required implementation

1. Extract the complete `TRACKER_PANEL_READER` implementation from `DecisionEngine`: its route method,
   tracker-only constants, image/title/detail crop logic, green-link segmentation, fingerprint/candidate helpers,
   tracker-specific records, diagnostics serialization, and resource reads that are used only by this path.
2. `TaskTrackerPanelService` must own the real algorithm. `DecisionEngine` keeps only the dispatch adapter needed to
   call it and return the existing `DecisionEngine.Decision` result. Do not create a chain of trivial wrappers.
3. Preserve every externally visible decision string, confidence, algorithm label, diagnostics key/value, coordinate
   space, selected-index rule, task-key rule, image flush/finally behavior, threshold, pixel constant, and fallback.
4. Do not add an owner, permit, session, ledger, TTL, retry, thread, local fallback, new capture, or new input action.
5. Keep runtime host behavior dormant; do not start server/application/Task/UI/input.

### Verification and report

- Run Cloud `mvn -q clean package` without skipping tests after the two-file write set is stable.
- Append `Implementation #1` to this report with exact changed files, extraction summary, build command/exit, suite
  totals, and any concern. Return `DONE`, `DONE_WITH_CONCERNS`, `NEEDS_CONTEXT`, or `BLOCKED` to the parent.
- Do not claim parent approval and do not update CR271/dashboard; the parent performs source review and bookkeeping.

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

CLAIMED | task=Cloud TaskTrackerPanelService algorithm extraction; claimedAt=2026-07-13T23:27:05-04:00; writeSet=`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\TaskTrackerPanelService.java`, `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`, append-only `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-tracker-panel-service-worker-at.md`

## Implementation #1

- 任务：将现有 `TRACKER_PANEL_READER` 算法从 `DecisionEngine` 等价抽取到真正的 `TaskTrackerPanelService`；业务基线为 `0114604e1ff5f15491d2910959c45252e893d04f`。
- 精确变更文件：
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\TaskTrackerPanelService.java`（新增）
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`（修改）
  - 本报告（仅 EOF 追加）。
- 抽取摘要：新服务完整拥有 tracker route、任务/图像模式常量、五环/修罗/五倍标题及 detail crop、绿链分割与候选选择、指纹/模板资源读取、五倍 OCR/任务键辅助、诊断序列化、专属 record，以及原有 image flush/finally 行为。`DecisionEngine` 的 `TRACKER_PANEL_READER` 分发仅委派给 `taskTrackerPanelService.read(request)` 并继续返回原 `DecisionEngine.Decision` 协议结果。
- 构建：`mvn -q clean package`，在 `D:\mavenProject\dhxy-cloud-brain` 执行，exit `0`。
- 套件汇总：Surefire XML `4` 个；tests `21`、failures `0`、errors `0`、skipped `0`。
- 关注项：无已知问题；未启动 Cloud host、应用、任务、UI 或输入，也未修改 CR271/仪表盘或其他工作区文件。

## Parent Source Review #1 - BLOCKED - 2026-07-13T23:34:00-04:00

**结论：P0=0 / P1=0 / P2=1。**

- 已通过：`DecisionEngine` 的 `TRACKER_PANEL_READER` 路由只剩
  `taskTrackerPanelService.read(request)`；tracker 专属常量、标题/detail crop、绿链分割、候选选择、五倍 OCR、
  diagnostics、资源读取与专属 records 已归新 `TaskTrackerPanelService`。未发现新增 owner/session/ledger/TTL/
  retry/thread/input/capture，worker package 为 4 suites / 21 tests 全绿。
- **P2-1（代码文档硬门）：** 新文件的唯一主流程 `read(JsonNode request)` 没有顶层 JavaDoc。`AGENTS.md` 要求
  main/high-frequency 方法必须说明输入、输出及坐标/空值语义；该入口接收原始 tracker 请求并返回带窗口相对坐标的
  terminal decision，属于必须覆盖的安全敏感主流程。影响是迁移边界与坐标合同无法从新 Service API 直接审查。

### 精确返修条件

原 AT 仅修改 `TaskTrackerPanelService.java`：为类和 `read(JsonNode request)` 增加简洁 JavaDoc，说明 request 非空、
图像/原点来自 exact-window typed request、返回 `DecisionEngine.Decision`、点击坐标为 WINDOW_RELATIVE、失败返回
NO_ACTION，且方法不 capture/不输入。禁止任何 executable-code、常量、签名或 `DecisionEngine` 变更。返修后重新运行
Cloud `mvn -q clean package` 并向本报告追加 `Implementation Repair #1`。父级随后 fresh package 复验。

## Implementation Repair #1

- 返修范围：仅为 `TaskTrackerPanelService` 类和 `read(JsonNode request)` 增加 P2-1 要求的 JavaDoc。
- JavaDoc 合同：`request` 非空；图像及窗口相对原点来自 exact-window typed request；返回 `DecisionEngine.Decision`；成功点击坐标为 `WINDOW_RELATIVE`；无效/缺失输入返回 `NO_ACTION`；该入口不 capture、不执行物理输入。
- 本次未修改：任何 executable code、常量、方法签名或 `DecisionEngine`。
- 构建：在 `D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q clean package`，exit `0`。
- 套件汇总：Surefire XML `4` 个；tests `21`、failures `0`、errors `0`、skipped `0`。
- 关注项：无；未启动 Cloud host、应用、任务、UI 或输入，待父级 fresh package 复验。

## Parent Source Review #2 - APPROVED - 2026-07-13T23:50:00-04:00

父级复核 Repair #1 与当前真实源码，结论 `P0=0 / P1=0 / P2=0`：

- `TaskTrackerPanelService` 类与主入口 `read(JsonNode request)` 已明确记录 request 非空、exact-window typed image/origin、
  `DecisionEngine.Decision` 返回合同、成功坐标 `WINDOW_RELATIVE`、非法/缺失输入 `NO_ACTION`，以及本入口不 capture、不输入。
- executable code、常量、签名与 `DecisionEngine` dispatch 未因返修改变；`TRACKER_PANEL_READER` 仍只委派
  `taskTrackerPanelService.read(request)`，算法与原 flush/finally 边界继续由新 Service 单一持有。
- 父级 fresh Cloud `mvn -q clean package` exit 0；Surefire 4 suites / 21 tests，0 failures / 0 errors /
  0 skipped。未启动 host/application/Task/UI/input。

本切片 `SOURCE APPROVED`。它完成 TaskTrackerPanel Cloud 算法所有权的首刀抽取，但尚未闭合 exact-window typed capture/
drag 共享端口，因此同路径批准计数暂不增加。Internal AT 可关闭；后续切片不得把算法复制回 DHXY adapter。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
