# Cloud LeftTopStatusSwitchService lift-and-shift - External Worker B

## Parent Task Brief #1 - `W-LTSS-D1` - 2026-07-13T05:09:08-04:00

### 目标

以 DHXY HEAD `0114604e1ff5f15491d2910959c45252e893d04f` 的
`src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` 为唯一业务基线，为整类 Cloud lift-and-shift 形成
implementation-ready Design #1。Cloud 迁业务编排与两模板 CPU 判定；DHXY 保留窗口绑定、capture、temp artifact、task-turn/input queue、
屏幕副作用前安全拒绝及 `WindowRuntimeContext.leftTopStatusSwitchClosePending` 本地标志。无已批准业务差异。

### 领取门

External Worker B 必须在 `2026-07-13T05:29:08-04:00` 前在**本日志**追加：

```text
## External Worker B - CLAIMED - <timestamp>
- task: W-LTSS-D1
- claimedAt: <timestamp>
- uniqueWriteSet: only this append-only report
```

20 分钟只检查领取，不检查完成；领取后可以工作超过 20 分钟。截止仍无 CLAIMED 才由父级内部接管。

### Design #1 必须闭合

1. 先完整读取 `D:/mavenProject/DHXY/AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、迁移矩阵与本日志；
   核对该源文件相对 HEAD 无 diff，并列完整 constructor/public/private/nested API、四个生产入口及 caller 行号。
2. 方法级保持四条入口原语义：leader startup、member startup probe、follower safe-window consume、combat maintenance；列出
   `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED/SKIPPED`、`allowClick`、clicked true/false 与 pending mark/consume/retain 的完整矩阵。
   `isSupportedTaskCode` 只允许 `xiuluo_v2/wubei/wuhuan_v2`；requestedTaskCode 优先于 taskCode；不得改大小写/空值语义。
3. 精确保留 ROI `8,147,16,29`、open/closed templates、threshold `0.90`、margin `0.02`、click settle `120ms`、delay
   `250ms`。两模板必须对**同一 OBSERVED capture**评分；说明 capture-time scale/window 如何把 image-local open center 映射到
   `WINDOW_CLIENT_PX` 并由 DHXY executor 变成屏幕绝对点击，禁止第二次 geometry/scale 读取。
4. Cloud 机械调用只经 retained `CloudTaskServicePort` typed capture/input handles，给每个 public invocation/workflow 与 fresh
   probe/click 的 exact address、stable identity、UNKNOWN/NOT_EXECUTED/STOPPED/revision 处理；同一 logical invocation 重入不铸新 ID，
   不开放 raw request/poll/outcome，不新增 retry/TTL/takeover。
5. `WindowRuntimeContext` pending flag、task-turn/standalone 是否允许前台动作、raw temp path 和 input queue 都留 DHXY；Cloud 不复制
   HWND/window holder/pending authority。特别核对 `docs/业务逻辑.md:43-44`：standalone 不得因迁云新增抢 task turn 的左上角动作。
6. 资源写集固定为零：Cloud 已有两模板且与 DHXY SHA-256 一致：
   `left_top_open.png=24586642CBB30FBE06E859E4402A6E71FD4D6275AC5231EE1CED3DEBAEF32ED5`，
   `left_top_closed.png=E9109BF66A40168F2C1746AD6EAA48BDF657D0AB3BCE24A4E1DD8A1FF64E3D46`。
   Cloud 使用 classpath canonical assets，禁止 cwd；rawPath 不得作为可操作本地路径上云。
7. 给出可编译 DAG、最小独立叶子波、精确 New/Modify 文件表与后续构建门。主体/assembly/caller/host 保持 dormant；
   不新增/恢复 tests，不启动任何运行面。

### 写集与并行边界

本轮唯一写集为本日志 append-only；DHXY/Cloud Java、Maven、schema、resources、tests 全冻结。不得修改 A QuestManager、P Full R0、
S BagService、旧 warning 日志或任何 host/caller。Worker 自审只算 QA，不构成父级批准；交付后等待父级源码/设计复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T05:15:08-04:00
- task: W-LTSS-D1
- claimedAt: 2026-07-13T05:15:08-04:00
- uniqueWriteSet: only this append-only report
- note: 领取在窗内（父级截止 2026-07-13T05:29:08-04:00）。设计-only，不落 Java/resources/tests/Maven/schema；不碰 A QuestManager / P Full R0 / S BagService / 旧 warning 日志 / host/caller。领取后即读 HEAD 源码与 AGENTS.md/DHXY_CONTEXT.md/业务逻辑.md/迁移矩阵，随后追加 Design #1（可超 20 分钟）。

## External Worker B - W-LTSS-D1 Design #1 - 2026-07-13

implementation-ready 迁移蓝图，基线 = DHXY HEAD `0114604e`。以下逐条闭合 brief 7 项。**设计-only，未落任何 Java/resources/tests；等父级源码/设计复审后方可实施。**

### 闭合项 1：基线核对 + 完整 API + 4 入口 + caller 行号

- **HEAD 核对**：`git -C D:/mavenProject/DHXY rev-parse --short HEAD` = `0114604e`（= brief 全哈希 `0114604e1ff5f15491d2910959c45252e893d04f`）；
  `git diff --stat HEAD -- src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` = **空（无 diff）**。源文件 299 行，基线冻结。
- **完整 API（HEAD 逐字）**：
  - 常量：`LEFT_TOP_STATUS_RECT_X_OFFSET=8`、`_Y_OFFSET=147`、`_WIDTH=16`、`_HEIGHT=29`、`LEFT_TOP_OPEN_TEMPLATE="images/template/status/left_top_open.png"`、
    `LEFT_TOP_CLOSED_TEMPLATE=".../left_top_closed.png"`、`LEFT_TOP_STATUS_MATCH_RATE=0.90`、`LEFT_TOP_STATUS_MATCH_MARGIN=0.02`、`CLICK_SETTLE_MS=120`、`CLICK_DELAY_MS=250`。
  - 构造注入（`@RequiredArgsConstructor`）：`GameClientTracker tracker`、`CoordinateHelper coordinateHelper`、`WindowScopedTempPath windowScopedTempPath`、
    `WindowTaskContextHolder windowTaskContextHolder`、`InputSequences inputSequences`。
  - public：`SwitchActionResult handleLeaderStartup(ctx,taskCode)`、`probeMemberStartup(ctx,requestedTaskCode)`、`consumeFollowerSafeWindow(ctx,requestedTaskCode)`、
    `handleCombatMaintenance(ctx,source)`、`boolean isSupportedTaskCode(String)`。
  - private：`checkAndMaybeClose(ctx,taskCode,source,allowClick)`、`detect(source)`、`scoreTemplate(scanPath,templatePath,rect)`、`resolveState(open,closed)`、
    `clearPendingIfResolved(result)`、`resolveTaskCode(ctx)`、static `safe/formatRect/formatPoint`。
  - nested：`enum SwitchState{OPEN,CLOSED,UNKNOWN,CAPTURE_FAILED,SKIPPED}`；`record SwitchActionResult(state,clicked,openScore,closedScore,openCenter,rawPath)`（+static `skipped`/`fromDetection`）；
    private `record DetectionResult(...)`、`record TemplateScore(double,Point)`。
- **4 生产入口 + caller 行号**：
  1. `handleLeaderStartup` ← `window/execution/DefaultWindowTaskStartupInitializer.java:108`（else 分支＝非五环、非 member＝leader，allowClick=true）。
  2. `probeMemberStartup` ← `DefaultWindowTaskStartupInitializer.java:104`（五环后台探测）与 `:106`（member 窗口），均 no-click。
  3. `consumeFollowerSafeWindow` ← `task/AutoBattleTask.java:235`（受 `requireLocalSupportGate && isLocalTeamSupportCapabilityOpen(LEFT_TOP_STATUS)` 门控，allowClick=true）。
  4. `handleCombatMaintenance` ← `service/AutoCombatService.java:1038`（local-support member 且 capability 开）与 `:1049`（非 support/非 pending 分支）。
  - 附加 guard：`isSupportedTaskCode` ← `AutoBattleTask.java:224`（`requestedTeamTask` 判定）。

### 闭合项 2：4 入口方法级语义 + 完整状态矩阵（迁云后逐字等价）

**入口原语义（保持不变）**：
- leader startup：`isSupportedTaskCode(taskCode)` 否→`SKIPPED("unsupported-task")`；是→`checkAndMaybeClose(...,allowClick=true)` 后 `clearPendingIfResolved`。
- member startup probe：门同上（对 `requestedTaskCode`）；`checkAndMaybeClose(...,allowClick=false)`；OPEN→`markLeftTopStatusSwitchClosePending`，CLOSED→`clearLeftTopStatusSwitchClosePending`。
- follower safe-window consume：先读 `pending`；`checkAndMaybeClose(...,allowClick=true)`；OPEN&clicked→`consume…Pending`，CLOSED→`consume…Pending`，否则若 `pending`→`mark…Pending(still-pending)`。
- combat maintenance：`resolveTaskCode(ctx)`（requested 非空优先，否则 taskCode）→门→`checkAndMaybeClose(...,"combat-maintenance:"+safe(source),allowClick=true)` 后 `clearPendingIfResolved`。

**判定核心**：`resolveState(open,closed)`＝ OPEN iff `open>=0.90 && open>=closed+0.02`；CLOSED iff `closed>=0.90 && closed>open`；否则 UNKNOWN。
`checkAndMaybeClose`：仅当 `state==OPEN && allowClick && openCenter!=null` 才点击，其余 `clicked=false` 原样返回。

**完整矩阵（state × allowClick × openCenter → clicked / pending 效果）**：

| 入口 | state | allowClick | openCenter | clicked | pending 效果 |
|---|---|---|---|---|---|
| leader/combat | OPEN | true | 非空 | true(点击成功值透传) | clicked→`clearPendingIfResolved`＝consume |
| leader/combat | OPEN | true | null | false | 无（不满足 clear 条件） |
| leader/combat | CLOSED | true | - | false | `clearPendingIfResolved`＝consume |
| leader/combat | UNKNOWN/CAPTURE_FAILED | true | - | false | 无 |
| leader/combat | SKIPPED(门失败) | - | - | false | 无 |
| probe(member) | OPEN | false | 任意 | false | `mark…Pending("member-startup-probe")` |
| probe(member) | CLOSED | false | - | false | `clear…Pending("member-startup-closed")` |
| probe(member) | UNKNOWN/CAPTURE_FAILED | false | - | false | 无 |
| follower | OPEN | true | 非空 & clicked | true | `consume…Pending("…clicked")` |
| follower | OPEN | true | null 或未点击 | false | 若入口时 pending→`mark…Pending("…still-pending")` |
| follower | CLOSED | true | - | false | `consume…Pending("…closed")` |
| follower | UNKNOWN/CAPTURE_FAILED | true | - | false | 若 pending→`mark…Pending("…still-pending")` |
| 任意 | SKIPPED | - | - | false | 无 |

`isSupportedTaskCode`：**只** `xiuluo_v2`/`wubei`/`wuhuan_v2`，`equalsIgnoreCase`（大小写不敏感逐字保留）；`resolveTaskCode`：`requestedTaskCode` 非 null 且非 blank 优先，否则 `taskCode`；null/blank 语义不改。

### 闭合项 3：几何/阈值/时序精确保留 + 单次 capture 双模板 + 一次 scale 映射

- ROI `getScaledRect(8,147,16,29)`、open/closed 模板、`0.90`/`0.02`、`CLICK_SETTLE_MS=120`、`CLICK_DELAY_MS=250` **逐字迁移，禁止改值**。
- **单一 OBSERVED capture**：DHXY 侧一次 capture 该 ROI，得到**同一张** observed 图；Cloud 对**这同一张图**分别与 open/closed 模板 `TM_CCOEFF_NORMED` 评分（等价 HEAD `scoreTemplate` 两次调用同 `scanPath`）。
- **open center 与坐标映射（禁止二次 geometry/scale 读取）**：HEAD 的 `centerX=rect[0]+round(maxLoc.x+tplW/2)`、`centerY=rect[1]+round(maxLoc.y+tplH/2)` 是 **image-local→窗口相对像素**。
  迁云后：Cloud 用**随 capture 一同带回的 capture-time systemScaleRatio + window rect**（capture 时刻快照，与 ACP `scale 经 capture-time systemScaleRatio` 同纪律）把 image-local open center 映射为 `WINDOW_CLIENT_PX`；
  该 `WINDOW_CLIENT_PX` 点交由 **DHXY executor** 用其既有窗口 geometry 变成屏幕绝对坐标点击。**Cloud 不再第二次读 geometry/scale**，DHXY 不再重算命中点——一次快照贯穿评分与点击。

### 闭合项 4：Cloud 机械调用只经 retained `CloudTaskServicePort` typed handle

- **capture**：`CloudTaskServicePort.capture(CaptureAction, region=ROI(8,147,16,29), imageFormat=v1 PNG, capturePurpose=左上开关探测, timeoutMs)` → `CaptureOutcome`（含 observed 图 + capture-time scale/window 快照）。
- **input（仅 OPEN&allowClick 分支）**：`executeInputBundle(InputBundleAction, "leftTopStatusSwitch:"+safe(source), coordinateSpace=WINDOW_CLIENT_PX, [moveAndClickLeft(point,settle=120,delay=250)], timeoutMs)`。
- **stable identity / 重入不铸新 ID**：每个 public invocation（leader/probe/follower/combat 各一 workflow）与其内的 fresh probe-capture、fresh close-click 各自是**一个 retained semantic action**，
  identity 由 Task 保留态派发（opaque `WindowFactAction/CaptureAction/InputBundleAction`，caller 不可 mint）。**同一 logical invocation 的重入复用同一 action record/identity**（不因重试铸新 ID），
  与 CloudTaskServicePort「每 attempt 独立 occurrence 经 M `RemoteFinalConsumedReceipt` frontier」纪律一致。
- **UNKNOWN/NOT_EXECUTED/STOPPED/revision**：probe（no-click）拿不到 outcome 或 UNKNOWN → 映射 `CAPTURE_FAILED/UNKNOWN`，**不点击**；click 的 `NOT_EXECUTED` → `clicked=false` 原样返回（不改 pending 的既有分支）；
  `STOPPED` 走 **typed stop unwind（非 NOT_EXECUTED）**；`runRevision`/`stopEpoch` 不匹配 → 该 attempt 作废、按 STOPPED/mismatch 处理，不重投。**不新增 retry/TTL/takeover**、不开放 raw request/poll/outcome。

### 闭合项 5：DHXY 保留窗口绑定/capture/temp/input-queue/pending 权威 + standalone 铁律

- 留 DHXY：HWND/window holder、实际 capture 执行、`WindowScopedTempPath` raw temp 文件、task-turn/input queue 串行化、`WindowRuntimeContext.leftTopStatusSwitchClosePending` 的
  `mark/clear/consume/is` 四方法与其**本地权威**。Cloud **不复制** HWND/window holder/pending authority，只回传「是否点击 + 命中点 + state/score」供 DHXY 落 pending 与执行。
- **屏幕副作用前安全拒绝**：DHXY executor 在把 `WINDOW_CLIENT_PX` 变屏幕点击前的既有安全门（task-turn 拥有权、standalone 让权）**原样保留**。
- **业务逻辑.md:43-44 铁律**：standalone 未入 session 且存在 active local-team leader 时**不得**抢 task turn 做左上角状态前台动作。迁云**不新增**任何 standalone 直发左上角点击的路径——
  Cloud 只在既有 caller 门（`DefaultWindowTaskStartupInitializer` 的 leader/member 分流、`AutoBattleTask:232-235` 的 support-gate、`AutoCombatService:1035-1049` 的 support/capability 门）**之后**被调用，门语义逐字不变。

### 闭合项 6：资源写集固定为零

- Cloud 已有两模板且 SHA-256 与 DHXY 一致：`left_top_open.png=24586642…32ED5`、`left_top_closed.png=E9109BF6…E3D46`。
- Cloud 侧模板评分用 **classpath canonical assets**（复用 B 的 CR271 `CloudTemplateAssets`/`PackagedTemplateAssets` 装载纪律），**禁止 cwd 相对路径**；
  DHXY 侧 `rawPath` 仅为本地 temp 诊断路径，**不作为可操作路径上云**、不进 Cloud 决策。resources 写集＝**0**。

### 闭合项 7：可编译 DAG + 最小独立叶子波 + New/Modify 文件表 + 构建门

- **DAG（依赖自底向上）**：`W-LTSS-0`（纯叶子）→ `W-LTSS-1`（Cloud 双模板评分）→ `W-LTSS-2`（编排）→ DHXY 哑执行壳。
  - **W-LTSS-0（最小独立叶子波，纯类型，可独立 javac）**：`LeftTopStatusDecision`——常量 + `SwitchState` + `resolveState(double,double)` + `isSupportedTaskCode` + `resolveTaskCode(requested,task)` +
    pending 分类纯函数（4 入口 × state × clicked → mark/consume/retain/none）+ 值 record（`SwitchActionResult`/`DetectionResult`/`TemplateScore`）。**无 OpenCV/capture/IO/Spring/clock**，byte-exact HEAD。
  - **W-LTSS-1**：`CloudLeftTopTemplateMatcher`——对 observed 图跑 `TM_CCOEFF_NORMED` 双评分 + image-local center 计算。依赖 OpenCV(Cloud) + `CloudTemplateAssets`（classpath 模板）。
  - **W-LTSS-2**：`CloudLeftTopStatusSwitchService`——编排 capture→评分→scale 映射→（OPEN&allowClick）input，全经 `CloudTaskServicePort`；产出等价 `SwitchActionResult`。依赖 M `RemoteFinalConsumedReceipt` frontier / port。
  - **DHXY 壳**：`LeftTopStatusSwitchService` 改为哑执行壳——保留构造注入与 4 public 签名，capture/temp/pending/input-queue 留本地，决策/评分/命中点上云；caller 签名与门**零改动**。
- **精确 New/Modify（本轮 design-only，不落）**：
  - New（Cloud）：`dhxy-cloud-brain/.../service/LeftTopStatusDecision.java`（W-LTSS-0）、`CloudLeftTopTemplateMatcher.java`（W-LTSS-1）、`CloudLeftTopStatusSwitchService.java`（W-LTSS-2）。
  - Modify（DHXY，后波）：`LeftTopStatusSwitchService.java` 改哑壳（4 caller 与 `WindowRuntimeContext` pending 保持）。
  - Resources：**0**；Maven/schema/tests：**0**。
- **构建门**：W-LTSS-0 可用非破坏性 standalone `javac` 独立验证（**不** `mvn clean package`，避免扰动并发 worker 的共享 `target/`）；整体双侧构建由父级统一 `mvn -q clean package` 执行。
- **dormant**：主体/assembly/caller/host 全程 dormant，不新增/恢复 tests，不启动任何运行面。

### 边界与自审

- 写集本轮＝**仅本 append-only 日志**；DHXY/Cloud Java、Maven、schema、resources、tests 全冻结；未碰 A QuestManager / P Full R0 / S BagService / 旧 warning 日志 / host/caller。
- 自审仅 QA，不构成父级批准。交付完毕，等待父级源码/设计复审。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #1 - PARTIAL PASS / Repair #1 Published - 2026-07-13T05:28:00-04:00

### 结论

- `W-LTSS-0` 仅以下纯判定叶子 **DESIGN APPROVED**：常量、`SwitchState`、`resolveState(double,double)`、
  `isSupportedTaskCode(String)`、`resolveTaskCode(String requestedTaskCode,String taskCode)`。该叶子不得含 pending
  authority、raw path、remote handle、capture/input、Spring、OpenCV、线程或 caller。
- `W-LTSS-1/W-LTSS-2/DHXY 壳` 仍 **BLOCKED**。
- P0=0，P1=4，P2=1。Worker 自审不算父级批准。

### P1-1：ROI/命中点被错误引入 scale 变换，会改变 HEAD 像素与点击点

- 证据：HEAD `CoordinateHelper.getScaledRect(...)` 实际只做
  `windowBase + (8,147)`，宽高仍为 `16x29`；`LeftTopStatusSwitchService.scoreTemplate(...)` 的命中点是
  `rect.left/top + imageLocalCenter`，没有除以或乘以 `systemScaleRatio`。DHXY
  `LocalRemoteGameCommandHandler.captureRectangle(...)` 对 `WINDOW_CLIENT_PX` 已只做
  `binding.x/y + region.x/y`。
- 当前设计却写成“Cloud 用 capture-time systemScaleRatio + window rect 把 image-local center 映射到
  WINDOW_CLIENT_PX”，这会在非 1.0 scale 下重复变换，造成错 ROI/错点。
- 影响：可直接点击错误客户区像素，违反基线等价迁移。
- Repair 条件：固定 capture request 为
  `CaptureRegion(WINDOW_CLIENT_PX,8,147,16,29)`；同一 observed PNG 对双模板评分；结果点严格为
  `(8 + round(maxLoc.x + templateWidth/2), 147 + round(maxLoc.y + templateHeight/2))`，仍是
  `WINDOW_CLIENT_PX`。`systemScaleRatio` 与 `observedWindow` 只作 capture 完整性/绑定证据，不参与该点的算术。

### P1-2：现有 v1 输入 wire 明确拒绝 `WINDOW_CLIENT_PX`，文件表却声称 schema/transport 零修改

- 证据：DHXY `RemoteOperationPayloadCodec.readInputBundle()` 当前要求
  `coordinateSpace == SCREEN_ABSOLUTE_PX`，否则抛出 `v1 input coordinateSpace must be SCREEN_ABSOLUTE_PX`；
  `LocalRemoteGameCommandHandler.validateInputCoordinates()` 也按屏幕绝对坐标直接校验。
- 影响：设计中的 `executeInputBundle(..., WINDOW_CLIENT_PX, ...)` 在本地副作用前必定被拒绝，W-LTSS-2 不可运行。
- Repair 条件：Repair #1 必须二选一并形成可编译文件表。推荐在 P2 Full R0 稳定后，补双仓 strict wire 对
  `WINDOW_CLIENT_PX` input 的显式支持：digest/schema/DTO 枚举保持一致，本地在每次副作用前用同一次最新 exact
  binding 把客户区点转屏幕绝对点，再执行既有 registration/runRevision/geometry/input-worker fence；不得让 Cloud
  自己用陈旧 `observedWindow.x/y` 铸屏幕绝对点。若选择维持 v1 `SCREEN_ABSOLUTE_PX`，必须说明为何不违背本 brief
  的 DHXY-side conversion 与错窗门，且不得二次读 geometry 后静默改点。

### P1-3：retained action handle 没有真实铸造/挂载路径，当前三文件 DAG 不可编译接线

- 证据：`CloudTaskServicePort.CaptureAction/InputBundleAction` 构造器 package-private；真正的
  `retainCapture/retainInputBundle` 只存在于 package-private `CloudTaskRetainedActionState`；
  `CloudTaskServiceExecutionContext` 只公开 `servicePort()`，没有任意 action mint API。拟放在业务 service package
  的三个新类无法取得 Design #1 所写 handles。
- 影响：要么编译不通，要么实现者被迫新增 public raw mint/bypass，破坏 retained ledger 单一权威。
- Repair 条件：给出 package-private authority adapter 的精确文件、固定 enum slot 与 assembly/runtime mount；adapter
  只能从 exact `CloudTaskServiceExecutionContext` 的 retained state 为四类 invocation 的 capture/click 固定地址发放
  opaque bundle，不得接收 public free-form phase/slot。明确同一 invocation 对象跨 resume 复用、何时新 occurrence、
  仅可信 `NOT_EXECUTED` 且 compaction 完成后如何 renewal；列出所有 New/Modify 文件。

### P1-4：remote `UNKNOWN` 不能降格为 `clicked=false`

- 证据：本地 input worker 在动作已开始后遇到 binding/timeout/transport 等会返回 `UNKNOWN`；此时 move/click 可能已有
  部分或全部副作用。Design #1 只精确定义 `NOT_EXECUTED -> clicked=false`，却让业务结果仍只有 boolean `clicked`。
- 影响：把“可能已点击”误记成“未点击”会错误保留 pending，并可能在后续 safe window 对同一业务动作再次点击。
- Repair 条件：结果模型必须保留 exact terminal execution state/outcome code。只有 `EXECUTED` 且 bundle 全步骤完成才
  `clicked=true`；可信 `NOT_EXECUTED` 才能是确定未点击并按 retained ledger 规则 renewal；`UNKNOWN/STOPPED` 不得折成
  false、不得自动重投，pending 采用基线保守保留并向上 typed unwind/uncertain 结果。

### P2-1：Cloud/DHXY 编排所有权叙述互相矛盾

- Design #1 同时称 Cloud W-LTSS-2 执行 capture/input，又称 DHXY 壳保留 capture/input 并接收 Cloud 结果落 pending，
  但没有画出真实调用方向、exact context API 或 transport 边界。
- Repair 条件：用一条可编译调用链明确“谁调用谁”。Cloud 业务服务只能经 retained typed port 请求本地机械能力；
  DHXY 不得反向成为第二业务编排脑。若 pending 仍按 brief 暂留 `WindowRuntimeContext`，需明确它由哪个既有本地入口在
  哪个 typed result 后更新，并说明主体 Task/caller 迁云后的最终收口路径，不得新开 raw business endpoint。

### 下一任务与领取门

External Worker B 当前任务为 `W-LTSS-D2 + W-LTSS-0-IMP1`：

1. 在本日志追加 Design Repair #1 Delta，只回答上述 P1/P2，未被点名章节不重抄；
2. 可并行只新建 Cloud `com/bot/dhxy/service/LeftTopStatusDecision.java`，内容严格限于本 review 已批准的五项纯判定；
3. 唯一 Java 写集为该 1 New；P2/A/S、remote/transport/schema、DHXY、assembly/host/caller/resources/tests/Maven 全冻结；
4. 不运行 `mvn clean`，交付源码 diff 与非破坏性编译证据；最终 fresh package 由父级在 P2 稳定后统一执行。

请在 `2026-07-13T05:48:00-04:00` 前追加 `CLAIMED`（task、claimedAt、唯一写集）。20 分钟只检查领取，领取后可继续工作超过 20 分钟。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T05:35:24-04:00
- task: W-LTSS-D2 + W-LTSS-0-IMP1
- claimedAt: 2026-07-13T05:35:24-04:00
- uniqueWriteSet: 本 append-only 日志 + 唯一 1 New Java `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusDecision.java`（仅 review 已批准的五项纯判定）
- note: 领取在窗内（父级截止 2026-07-13T05:48:00-04:00）。P2/A/S、remote/transport/schema、DHXY、assembly/host/caller/resources/tests/Maven 全冻结；不 `mvn clean`，仅非破坏性 javac 证据；fresh package 由父级在 P2 稳定后统一执行。

## External Worker B - W-LTSS-D2 Design Repair #1 Delta - 2026-07-13

只回答 Review #1 的 P1-1..4 / P2-1，未点名章节不重抄。**已 DESIGN APPROVED 不重开**：W-LTSS-0 五项纯判定叶子（常量、`SwitchState`、`resolveState`、
`isSupportedTaskCode`、`resolveTaskCode(String,String)`）。

### P1-1 修正：ROI 与命中点全程 `WINDOW_CLIENT_PX`，零 scale 算术

已核实 HEAD `CoordinateHelper.getScaledRect(8,147,16,29)`（`CoordinateHelper.java:127-134`）= `{baseX+8, baseY+147, +16, +29}`——**名为 scaled 实不缩放**，
`systemScaleRatio` 只出现在无关的 `findImageAbsoluteCoordinate`。故撤回 Design #1「Cloud 用 capture-time systemScaleRatio 映射」一句（错误，会在非 1.0 scale 重复变换）。**修正裁定**：
- capture request 固定 `CaptureRegion(WINDOW_CLIENT_PX, 8, 147, 16, 29)`。
- 双模板对**同一 observed PNG** 评分（等价 HEAD `scoreTemplate` 两次同 `scanPath`）。
- 命中点严格 `(8 + round(maxLoc.x + templateWidth/2), 147 + round(maxLoc.y + templateHeight/2))`，单位仍 **`WINDOW_CLIENT_PX`**，与 HEAD `rect.left/top + imageLocalCenter` 逐字一致。
- `systemScaleRatio` 与 `observedWindow` **仅作 capture 完整性/绑定证据**，不参与该点算术、不参与 ROI。

### P1-2 修正：v1 input wire 现拒 `WINDOW_CLIENT_PX`，选定「补 strict wire + 本地副作用前转屏幕绝对」，schema 非零

已认领：DHXY `RemoteOperationPayloadCodec.readInputBundle()` 现要求 `coordinateSpace==SCREEN_ABSOLUTE_PX`，`LocalRemoteGameCommandHandler.validateInputCoordinates()` 亦按屏幕绝对校验；
故 Design #1「schema/transport 零修改 + `executeInputBundle(...,WINDOW_CLIENT_PX,...)`」自相矛盾，撤回。**修正裁定（采纳父级推荐项）**：
- 在 **P2 Full R0 稳定后**，补**双仓 strict wire 对 `WINDOW_CLIENT_PX` input 的显式支持**：digest 预像、schema、DTO 的 `CoordinateSpace` 枚举**两仓一致**新增该 space；
- **本地在每次屏幕副作用前**，用**同一次最新 exact binding**（既有 `LocalRemoteGameCommandHandler` 持有的 client→screen 绑定）把 `WINDOW_CLIENT_PX` 点转屏幕绝对点，再走既有
  registration / runRevision / geometry / input-worker fence；**Cloud 绝不自己用陈旧 `observedWindow.x/y` 铸屏幕绝对点**。
- 因此 **schema/transport 写集非零**（属 P2 稳定后的双仓 wire 增补，B 不提前落码）；本波 `W-LTSS-0-IMP1` 只落纯判定叶子，不含任何 wire。

### P1-3 修正：新增 package-private authority adapter，从 retained state 定址发放 opaque handle

已认领：`CloudTaskServicePort.CaptureAction/InputBundleAction` 构造器 package-private，`retainCapture/retainInputBundle` 仅在 package-private `CloudTaskRetainedActionState`，
`CloudTaskServiceExecutionContext` 只公开 `servicePort()` 无任意 mint。**修正裁定**：
- 新增 **package-private authority adapter**（置于 remote 包内，与 `CloudTaskRetainedActionState` 同包）：`LeftTopStatusActionAuthority`，
  只从 **exact `CloudTaskServiceExecutionContext` 的 retained state** 为四类 invocation（leader/probe/follower/combat）的 **capture / click 固定地址**发放 opaque bundle，
  **不接收 public free-form phase/slot**。
- **固定 enum slot**：在既有 retained action 地址枚举新增 LTSS 的固定 slot（每 invocation 的 capture 与 click 各一固定地址），不开放可变址。
- **assembly/runtime mount**：adapter 由既有 authority assembly 在装配期挂载，随 `CloudTaskServiceExecutionContext` 生命周期存活。
- **occurrence/renewal**：同一 logical invocation 对象**跨 resume 复用同一 record/identity**；仅当**可信 `NOT_EXECUTED`** 且 compaction 完成后才 renewal 新 occurrence；不因不确定结果铸新 ID、不重投。
- **New/Modify（本波不落，待 P2 稳定后按真实源码方法级合并）**：New `remote/LeftTopStatusActionAuthority.java`；Modify 既有 retained-action 地址枚举（加 LTSS slot）+ authority assembly（挂载）。均属 P2 稳定后写集，B 不提前落码。

### P1-4 修正：结果模型保留 exact terminal execution state，`UNKNOWN/STOPPED` 不折成 false

已认领：本地 input worker 在动作已开始后遇 binding/timeout/transport 会返回 `UNKNOWN`，可能已部分/全部点击。Design #1 只定义 `NOT_EXECUTED→clicked=false` 且业务只有 boolean，欠妥。**修正裁定**：
- 迁云结果模型在 `SwitchActionResult` 之外**保留 exact terminal `ExecutionState` + `OutcomeCode`**（不改 W-LTSS-0 叶子，属 W-LTSS-2 编排层结果）。
- **仅** `EXECUTED` 且 bundle 全步骤完成 → `clicked=true`（等价 HEAD 成功点击）。
- **可信 `NOT_EXECUTED`**（确定未产生副作用）→ 确定 `clicked=false`，按 retained ledger 规则 renewal。
- **`UNKNOWN` / `STOPPED`** → **不折成 false、不自动重投**；pending 采**基线保守保留**（不 consume、不误判 clicked），并向上以 **typed unwind / uncertain** 结果透传，避免把「可能已点击」记成未点击而在后续 safe window 二次点击。

### P2-1 修正：唯一可编译调用链，Cloud 为唯一业务脑，DHXY 只出机械能力 + 持 pending

撤回 Design #1「Cloud 执行 capture/input」与「DHXY 壳保留 capture/input 并落 pending」并列的含糊叙述。**唯一方向**：
```
DHXY 既有 4 caller（DefaultWindowTaskStartupInitializer:104/106/108, AutoBattleTask:235, AutoCombatService:1038/1049，门语义逐字不变）
   └─(既有 task-run 入口，非新 raw endpoint)→ Cloud CloudLeftTopStatusSwitchService（唯一业务脑：编排 capture→双模板评分→resolveState→(OPEN&allowClick)click）
         └─ 只经 retained typed CloudTaskServicePort 请求 DHXY 机械能力（capture / executeInputBundle）
               └─ DHXY 机械层执行 capture / 副作用前转屏幕绝对点击（P1-2），回 typed CaptureOutcome / InputBundleOutcome（含 exact ExecutionState）
   ← Cloud 返回 typed SwitchActionResult（含 execution state, P1-4）
   └─ 由发起的**既有 DHXY 本地入口**在收到 typed result 后更新 `WindowRuntimeContext.leftTopStatusSwitchClosePending`（mark/consume/retain 按矩阵；pending 权威留 DHXY）
```
- **DHXY 不反向成第二业务脑**：机械层只出 capture/input 能力，不做 state 决策/编排。
- **pending 更新点**：仍是既有本地入口（4 caller 各自返回路径），在 typed result 后按 Design #1 矩阵落 pending，**不新开 raw business endpoint**。
- **主体 Task/caller 最终收口**：caller 签名/门不变，仅内部把「本地 service 决策」替换为「经 task-run 调 Cloud 业务脑」；主体/assembly/host 迁云后保持 dormant，直至父级统一收口。

### W-LTSS-0-IMP1 实施说明（唯一 1 New Java）

- New：`dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusDecision.java`，**严格仅含 review 已批准五项**：常量、`enum SwitchState`、`resolveState(double,double)`、
  `isSupportedTaskCode(String)`、`resolveTaskCode(String requestedTaskCode, String taskCode)`。**不含** pending authority、raw path、remote handle、capture/input、Spring、OpenCV、线程、caller、
  以及 `SwitchActionResult/DetectionResult/TemplateScore`（未获批，留后波）。`resolveState`/`isSupportedTaskCode` 与 HEAD 逐字；`resolveTaskCode` 取 HEAD 纯逻辑（requested 非 blank 优先，否则 task）。
- 不 `mvn clean`；下附非破坏性 standalone `javac` 证据。P2/A/S、remote/transport/schema、DHXY、assembly/host/caller/resources/tests/Maven 全冻结。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - W-LTSS-0-IMP1 Implementation #1 - 2026-07-13

- **New（唯一 Java 写集）**：`dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusDecision.java`
  - 88 行；SHA-256(16)=`759024B8F150A569`；`git status --porcelain` = `?? .../LeftTopStatusDecision.java`（仅此 1 New，untracked）。
  - 内容严格限 review 已批准五项：常量（`RECT_X/Y/W/H=8/147/16/29`、open/closed 模板、`MATCH_RATE=0.90`、`MATCH_MARGIN=0.02`、`CLICK_SETTLE_MS=120`、`CLICK_DELAY_MS=250`）、
    `enum SwitchState{OPEN,CLOSED,UNKNOWN,CAPTURE_FAILED,SKIPPED}`、`resolveState(double,double)`、`isSupportedTaskCode(String)`、`resolveTaskCode(String requestedTaskCode,String taskCode)`。
  - `resolveState`/`isSupportedTaskCode` 与 HEAD `0114604e` 逐字；`resolveTaskCode` 取 HEAD 纯逻辑（requested 非 null 且非 blank 优先，否则 task）。
  - **零**：无 Spring/OpenCV/I/O/capture/input/remote handle/pending authority/raw path/thread/clock/caller；无 `SwitchActionResult/DetectionResult/TemplateScore`（未获批，留后波）。
- **非破坏性编译证据**（不 `mvn clean`，输出隔离到 scratchpad，不触 `target/`）：
  ```
  javac -d <scratchpad>/ltss-javac src/main/java/com/bot/dhxy/service/LeftTopStatusDecision.java
  JAVAC_OK
  → LeftTopStatusDecision.class + LeftTopStatusDecision$SwitchState.class（零依赖、零告警）
  ```
- **冻结核对**：未 `mvn clean`、未 git add/commit；P2/A/S、remote/transport/schema、DHXY、assembly/host/caller/resources/tests/Maven 均未改动；两仓其余 dirty/untracked 未动。fresh package 待父级在 P2 稳定后统一执行。
- 交付完毕，等待父级源码/设计复审。

**无已批准业务差异；按基线等价迁移。**

## Parent Design/Implementation Review #2 - W-LTSS-0 APPROVED / Full Design BLOCKED - 2026-07-13T05:41:00-04:00

### 结论

- `W-LTSS-0-IMP1`：**父级源码 APPROVED**。`LeftTopStatusDecision.java` 的五项批准内容与 DHXY HEAD `0114604e` 的值、分支顺序、比较方向、task-code 大小写/blank 语义一致；写集仅 1 New，零 I/O/remote/host/caller。fresh Cloud package 仍等待 P2 双仓写入稳定后由父级统一执行。
- `W-LTSS-D2`：P0=0，P1=1，P2=1，**主体设计仍 BLOCKED**。P1-1/P1-2/P1-3/P1-4 的坐标、wire、retained identity、UNKNOWN 语义已基本闭合；P2-1 的最终调用链仍把业务 pending 留在本地，违反全量云端边界。

### P1-1：`leftTopStatusSwitchClosePending` 仍由 DHXY caller 持有，形成残留本地业务权威

- 证据：Repair §P2-1 明确写“pending 权威留 DHXY”，并由四个本地 caller 根据 Cloud 业务结果执行 mark/consume/retain。该 flag 决定后续 safe-window 是否再次执行关闭动作，不是窗口绑定、capture、输入队列或安全拒绝，而是业务状态。
- 影响：Cloud 虽做识别/点击决策，本地仍决定 pending 生命周期；断线/resume/caller 重排时会出现 Cloud action ledger 与本地 pending 两套状态，不能满足“本地只保留机械能力与 UI”的目标。
- Repair 条件：把 pending owner 移入 Cloud retained business state，绑定 exact `(scope,taskRunId,window,stopEpoch)` 并跨 revision 复用同一 owner；probe/leader/combat/safe-window 的 mark/consume/retain 都由 Cloud 同一状态机完成。DHXY 只执行 typed capture/input 并返回 exact outcome，不得根据 outcome 再落业务 flag。主体 caller 最终迁云前可保持 dormant/stub 接线，但不能以本地 flag 作为最终设计权威。`UNKNOWN/STOPPED` 仍不得 consume 或 renewal；可信 `NOT_EXECUTED` 才按 Full R0 合同处理。

### P2-1：两仓 coordinate enum 已有 `WINDOW_CLIENT_PX`，不得重复“新增枚举”

- 证据：Cloud `CoordinateSpace` 与 DHXY `RemoteCoordinateSpace` 当前都已包含 `WINDOW_CLIENT_PX`；真正拒绝点是 Cloud `InputBundleRequest` 与 DHXY `RemoteOperationPayloadCodec` 的 v1 validation，以及本地输入映射仍假定屏幕绝对坐标。
- Repair 条件：修订精确写集为 validator/schema/local conversion path，不改两仓 enum。普通 `SCREEN_ABSOLUTE_PX` request 的 payload/digest 必须逐字不变；相对点只在 DHXY 以同一次 current binding 转为屏幕绝对点后进入既有原子 input bundle 与 revision/safety fence。

### 非阻断清理

`LeftTopStatusDecision` JavaDoc 的“byte-exact copy”不准确（访问级别/静态 API 已为 Cloud leaf 调整）；后续同文件返修时改为“baseline-equivalent extraction”。这不改变本轮源码行为，记 P3，不阻断 W-LTSS-0 源码批准。

### 下一任务 `W-LTSS-D3`

External Worker B 只追加 Design Repair #2 Delta，关闭上述 P1/P2；Java、P2/A/S、remote/wire/schema/assembly/host/caller 全冻结。先追加 `CLAIMED`（任务标题、领取时间、唯一写集）；领取截止为父级发单后 20 分钟，即 `2026-07-13T06:01:00-04:00`。自审不算父级批准。

## External Worker B - CLAIMED - 2026-07-13T05:43:54-04:00
- task: W-LTSS-D3（Design Repair #2 Delta）
- claimedAt: 2026-07-13T05:43:54-04:00
- uniqueWriteSet: 仅本 append-only 日志
- note: 领取在窗内（父级截止 2026-07-13T06:01:00-04:00）。设计-only；Java（含已批准 `LeftTopStatusDecision.java`）、P2/A/S、remote/wire/schema/assembly/host/caller/resources/tests/Maven 全冻结；不 `mvn`、不 git。只关闭 P1-1/P2-1，P3 记录待下次 Java 波次。

## External Worker B - W-LTSS-D3 Design Repair #2 Delta - 2026-07-13

只回答 Review #2 的 P1-1 / P2-1（+ 记录 P3），未点名章节不重抄。**已 APPROVED 不重开**：W-LTSS-0-IMP1 源码（父级源码 APPROVED）、W-LTSS-0 五项纯判定。
Repair #1 已基本闭合的 P1-1(坐标)/P1-2(wire 方向)/P1-3(handle authority)/P1-4(UNKNOWN 语义) 不重抄。

### P1-1 修正：pending owner 迁入 Cloud retained business state，DHXY 只出机械能力不落业务 flag

撤回 Repair #1「pending 权威留 DHXY、由四 caller 落 mark/consume/retain」——`leftTopStatusSwitchClosePending` 决定后续 safe-window 是否再次关闭，是**业务状态**（非窗口绑定/capture/输入队列/安全拒绝），
留本地会形成 Cloud action ledger 与本地 pending 两套状态，违反「本地只留机械能力与 UI」。**修正裁定**：
- **pending owner 移入 Cloud retained business state**，绑定 exact `(scope, taskRunId, window, stopEpoch)`，**跨 revision 复用同一 owner**（不因 revision 变化新建/丢失）。
- **单一状态机**：probe / leader / combat / safe-window 四入口的 `mark / consume / retain` 全由 **Cloud 同一 business state machine** 完成（对应 HEAD `markLeftTopStatusSwitchClosePending`/`clear…`/`consume…` 语义逐字迁入 Cloud）。
- **DHXY 只执行 typed capture/input 并回 exact outcome**，**不得**再依 outcome 落任何业务 flag；`WindowRuntimeContext.leftTopStatusSwitchClosePending` 本地字段在最终迁云后退化为纯 UI/诊断镜像，不再是设计权威。
- **主体 caller 最终迁云前可保持 dormant/stub 接线**，但**不得以本地 flag 作为最终设计权威**。
- **不确定态纪律不变**（承接 Repair #1 P1-4）：`UNKNOWN / STOPPED` 仍**不得 consume 或 renewal**（pending 保守保留）；**仅可信 `NOT_EXECUTED`** 按 Full R0 合同 renewal；仅 `EXECUTED` 全步 → `clicked=true` 才 consume。

### P2-1 修正：两仓 coordinate enum 已含 `WINDOW_CLIENT_PX`，写集改为 validator/schema/local-conversion，不动 enum

已核实：Cloud `CoordinateSpace`（`CoordinateSpace.java:3-6`）与 DHXY `RemoteCoordinateSpace` **均已含** `WINDOW_CLIENT_PX`；真正拒点是
Cloud `InputBundleRequest.java:17-18`（`require(coordinateSpace==SCREEN_ABSOLUTE_PX, "v1 input bundle coordinateSpace must be SCREEN_ABSOLUTE_PX")`）与 DHXY `RemoteOperationPayloadCodec.readInputBundle()` 的同型 v1 校验，以及本地输入映射仍假定屏幕绝对坐标。**修正裁定（撤回 Repair #1「两仓新增枚举」）**：
- **精确写集 = validator + schema/codec + local conversion path，enum 零改动**：
  - Cloud：放宽 `InputBundleRequest` 的 v1 断言，使其在既有 digest 预像规则下**接受 `WINDOW_CLIENT_PX`**（普通 `SCREEN_ABSOLUTE_PX` request 的 payload/digest **逐字不变**，仅新增对 client-px 的合法接受分支）。
  - DHXY：`RemoteOperationPayloadCodec.readInputBundle()` 对应放宽，schema/DTO 保持两仓一致（同一 `CoordinateSpace` 枚举序）。
  - **local conversion**：相对 `WINDOW_CLIENT_PX` 点**只在 DHXY**、以**同一次 current exact binding** 转屏幕绝对点后，**再进既有原子 input bundle + registration/runRevision/geometry/input-worker safety fence**；Cloud 绝不用陈旧 `observedWindow` 自铸屏幕绝对点。
- **零回归保证**：既有一切 `SCREEN_ABSOLUTE_PX` 路径 payload/digest/校验/行为**逐字不变**；新增仅为 client-px 的接受 + 本地转换。
- **写集归属与时序**：此 validator/codec/local-conversion 改动属 **remote/wire/schema**，本轮 D3 冻结、**B 不提前落码**，待 **P2 Full R0 稳定**后按双仓真实源码方法级合并（与 warning transport 同纪律）。

### P3 记录（非阻断，不本轮落码）

父级 P3：`LeftTopStatusDecision` JavaDoc「byte-exact copy」措辞不准确（访问级别/静态 API 已为 Cloud leaf 调整）。**已认领**：下次**同文件 Java 写集解冻**时，把 JavaDoc 改为「baseline-equivalent extraction」（值/分支/比较/大小写语义仍逐字，仅措辞校正）。本轮 D3 Java 冻结，不改源码。

### 不变量重申

- W-LTSS-0-IMP1 源码（APPROVED）、W-LTSS-0 五项纯判定：冻结不重开。
- 本轮 D3 **设计-only**：Java（含 `LeftTopStatusDecision.java`）、P2/A/S、remote/wire/schema/assembly/host/caller/resources/tests/Maven 全冻结；未 `mvn`、未 git、未 review 他方、未启运行面；两仓其余 dirty/untracked 未动。
- P1-1 的 pending→Cloud、P2-1 的 validator/codec/conversion 均属 P2 稳定后写集，B 不提前落码。
- 交付完毕，等待父级源码/设计复审。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #3 - DESIGN APPROVED / Next Task Handoff - 2026-07-13T05:50:00-04:00

### 结论

- `W-LTSS-D3`：**DESIGN APPROVED，P0/P1/P2=0**。pending 的 mark/consume/retain 已归 Cloud retained business
  state，DHXY 只保留 capture/input/outcome 与可选 UI 诊断镜像；镜像不得参与任何业务 gate。两仓既有
  `WINDOW_CLIENT_PX` enum 保持零改，未来只改 validator/schema/local conversion path；普通 screen-absolute bytes/digest/行为
  必须逐字不变。
- 绑定实施门：Cloud pending owner 必须由同一 authority assembly/taskRun runtime 持有，stable key 为 exact
  `(scope,taskRunId,window,stopEpoch)`，resume 复用同一对象；terminal retirement 与 Full R0 outcome/renewal 同锁序收口。
  input client-px 转屏幕绝对点必须使用副作用提交前取得的 current exact binding，再进入既有单 bundle queue 与 revision/safety
  fence；不得在 Cloud 用旧 geometry 算屏幕点。
- `LeftTopStatusDecision` 既有源码批准保持；P3 JavaDoc 仅在下次同文件 Java 写集解冻时修正。主体/remote/wire/assembly/caller
  实施排在 P2 Full R0 稳定后，本轮不提前写重叠文件。

### 下一任务 handoff

External Worker B 转入新固定日志：
`docs/superpowers/plans/reports/2026-07-13-cloud-return-item-prescan-service-worker-b.md`，任务
`W-RIPS-D1`。请先在新日志追加 `CLAIMED`；本日志到此只读，不再承载新任务。

**无已批准业务差异；按基线等价迁移。**
