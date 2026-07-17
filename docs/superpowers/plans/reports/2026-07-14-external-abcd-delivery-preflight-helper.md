# External A/B/C/D Delivery Preflight Helper

## CLAIMED - 2026-07-14T13:14:59-04:00

- task: `W-ABCD-DELIVERY-PREFLIGHT-1`
- role: `Internal Delivery Preflight Helper CL`（只读交付预检，不是 reviewer，不替代父级审查）
- scope: 读取 External A/B/C/D 固定日志真实 EOF、声明写集、当前对应源码 diff、`696a12b0` caller 基线与已批准 `UI_CLEAN` 合同；只记录风险候选与父级复核焦点。
- uniqueWriteSet: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-external-abcd-delivery-preflight-helper.md`（create/append-only）
- prohibitions: 不修改 Java、POM、原 Worker 日志、CR、`ACTIVE_WORK`、矩阵或 dashboard；不运行 Maven/test/runtime；不执行 Git mutation。

## PREFLIGHT MATERIAL - 2026-07-14T13:21:30-04:00

> **性质：非批准结论，仅是只读交付预检材料，等待父级最终审查。** 本节未运行 Maven/test/runtime，未执行 Git mutation。

### A/B/C/D 真实交付与实际写集

| Worker | 真实 EOF / 交付 | 当前可见实际写集 | 越界预检 |
|---|---|---|---|
| A `W-696-UI-CLEAN-CALLERS-1` | 日志物理 EOF `4962`；有 Implementation、八点清单及后续父级源码记录 | Cloud `AutoCombatService.java`、`SummonSkillService.java`、`NavigationService.java` + 原日志；三文件均 untracked。逐一 `git diff --no-index` 对 `migration-baseline/696a12b0`，差异仅 3 组 port import/字段替换和约定 8 个 caller | 未见越界证据；当前 active 已无三类中的 `UICleanerService/uiCleanerService` 残留 |
| B `W-696-UI-CLEAN-HANDLER-1` | 日志物理 EOF `7149`；Implementation 后完成一次 JavaDoc 排列返修并有后续父级源码记录 | DHXY `LocalRemoteGameCommandHandler.java` + 原日志；文件 untracked。当前新增派发及 `executeUiCleanMacro`、`executeUiCleanCloseMapSearchInputByX2`、`toUiCleanMacroResultPayload` 均在同一文件 | 未见 protocol/wire 或其它文件越界证据；整文件 untracked，不能用普通 Git diff 独立归因到单一 worker |
| C `W-696-UI-CLEAN-CLOUD-CONTRACT-1` | 日志物理 EOF `3884`；3 New + 7 Modify 均有交付表，当前 SHA-256 与日志前缀全部一致 | Cloud remote：`UiCleanMacroCommand`、`UiCleanMacroResult`、`CloudUiCleanerPort`，以及 `LocalMacroKind/Command/Request/Outcome`、`RemoteProtocolDigests`、`RemoteCommandOutcomeEnvelope`、`RemoteGameCommandBroker` + 原日志；均 untracked | 未见 Service/DHXY 越界证据；文件表与当前 10 个 hash 对齐 |
| D `W-696-UI-CLEAN-DHXY-WIRE-1` | 日志物理 EOF `4498`；2 New + 5 Modify 有交付表和真实 EOF 父级记录 | DHXY remote：`RemoteUiCleanMacroCommandPayload`、`RemoteUiCleanMacroResultPayload`、`RemoteLocalMacroKind/CommandPayload/ResultPayload`、`RemoteOperationPayloadCodec`、`RemoteProtocolDigests` + 原日志；均 untracked | 未见 handler/Service 越界证据；当前 7 个 SHA-256 与日志前缀全部一致 |

### 风险候选与已核不变量

1. **P1 行为/队列候选（待父级重点判定）**：Cloud `NavigationService.closeMapSearchInputAfterRouteDialog` 当前仍在外层 `inputSequences.submitExclusiveAndWait(...)` callback 内同步调用 `cloudUiCleanerPort.closeMapSearchInputByX2(...)`（约 `:2267-2274`），而 DHXY handler 的该 operation 又进入 `submitRemoteExclusiveAndWaitDetailed(...)`（约 `:1166-1178`）。这是 Phase 4 尚未收口的跨进程中间态；父级需确保最终把基线的“x2 direct + 成功后 moveMouseAway”保持为一个 closed 本地原子序列，不能形成同一物理队列的嵌套等待或拆散原顺序。本项是候选，不判定本批 caller 替换错误。
2. **P2 编译/证据候选**：A/B/C/D 对应 Java 当前全是 untracked，普通 `git diff` 对其不可见；Cloud 统一 package 仍被其它依赖闭包阻断，现有材料只能说明未报告 `UI_CLEAN` 专属编译错误，不能替代最终双侧 fresh build。父级复核应保存 no-index/hash 证据，避免“diff 为空”假阴性。
3. **P2 交付追踪候选**：C 的 Implementation 标题时间 `13:05` 晚于其后父级记录时间 `12:58`，但当前 10 文件 mtime 为 `12:44-12:50` 且 hash 与交付表一致。以后自动预检不得只按章节时间排序，应以真实 EOF、task id、文件 hash 三者共同定位最新材料。

已核关键不变量：A 仅替换约定 8 个原调用位置，分支/判断/delay/fallback/state/log 未见额外 diff；B 三种自持队列 cleaner 在 queue 外经 exact-context `callWith`，x2 direct 保留 deadline/pause/safety/runRevision fences，terminal 恰四键且 `cachePoint=null`；C/D 的 4 operation、null/nonblank source 规则、7 state 配对、non-EXECUTED 空 typed result、request/outcome `uiClean` canonical tree逐值一致，既有 BAG/NAV 分支在已读材料中未见被改写。

### 父级重点复核点

- A：Cloud `NavigationService.closeMapSearchInputAfterRouteDialog` / `closeMapSearchInputAfterRouteClick` / stale-panel 分支；`AutoCombatService` 两个 maintenance caller；`SummonSkillService` finish/tail caller。
- B：DHXY `LocalRemoteGameCommandHandler.executeLocalMacro` 派发（`:1017-1022`）、`executeUiCleanMacro`（`:1111`）、`executeUiCleanCloseMapSearchInputByX2`（`:1149`）、`toUiCleanMacroResultPayload`（`:1215`）。
- C：`CloudUiCleanerPort.runMacro/requireExecuted`、`LocalMacroRequest/Outcome` 互斥、`RemoteCommandOutcomeEnvelope.localMacroOutcome`、`RemoteProtocolDigests.withComputedRequestDigest`。
- D：两个 `RemoteUiCleanMacro*Payload` 构造校验、`RemoteOperationPayloadCodec.readUiCleanMacro/readUiCleanMacroResult/readLocalMacroTerminal`、`RemoteProtocolDigests` 的 request/outcome 重建。

### 复用快速预检清单（8 项）

1. 读取固定日志物理总行数与真实 EOF，按 task id 找最后一次 claim/repair/delivery。
2. 抄录唯一写集、依赖等待和禁止项；把 worker 自述与后续父级材料分栏。
3. 对每个声明文件查存在性、tracked/untracked、SHA-256/mtime；tracked 用 `git diff`，untracked 用 baseline `--no-index` 或 hash。
4. Caller 类逐文件对 `696a12b0`，确认差异只在批准调用点且调用数量精确。
5. 跨端逐值对齐 operation、source nullability、state pairing 与非执行态规则。
6. 核对 command allowlist、四键 terminal、`cachePoint=null`、canonical tree 与 digest 重建，抽查旧 BAG/NAV 分支未变。
7. 核对 exact window/context、输入队列所有权、deadline/pause/safety/revision fences、checkpoint 与无 retry/第二 owner；特别扫描嵌套队列。
8. 明确哪些 fresh compile/package 尚待父级，记录风险只为候选并附文件/方法/行；结尾注明“非批准结论，等待父级最终审查”。

**Preflight helper 通知父级：以上材料已收口，可据此执行最终源码与统一构建复核。非批准结论，等待父级最终审查。**

## CLAIMED - W-ABCD-DELIVERY-PREFLIGHT-2 - 2026-07-14T13:38:15-04:00

- role: `Internal Delivery Preflight Helper CL`（只读轮询与候选风险预检，不是 reviewer，不替代父级审查）
- targets: A `LEFT_TOP`；B `TASK_TRACKER_PANEL_RECT`；C `COMMON_BOX`；D `TEAM_RETURN member-button`。
- action: 读取四份固定日志真实 EOF，先记录各单 task id、领取截止、CLAIMED 状态；发现首个新的 Implementation/Repair 后立即核对声明/实际写集、`696a12b0` 限定 diff、typed fact / ordered bundle 映射及 P0/P1/P2 候选。
- uniqueWriteSet: 本固定报告（append-only）。不修改 A/B/C/D 日志、Java、POM 或主文档；不运行 Maven/test/runtime；不执行 Git mutation；逾期未领取只记录事实，不接管。

### CLAIMED STATUS POLL #1 - 2026-07-14T13:39:02-04:00

| Worker | 权威 task / 领取截止 | 真实 EOF 与当前状态 |
|---|---|---|
| A | `W-696-LEFT-TOP-STATUS-TYPED-ADAPT-1` / `13:55:00-04:00` | 物理 EOF `5011`；已于 `13:36:03-04:00` CLAIMED；声明写集为 Cloud `LeftTopStatusSwitchService.java` + A 固定日志；尚无本 task 的 Implementation/Repair |
| B | `W-696-TASK-TRACKER-PANEL-RECT-TYPED-ADAPT-1` / `13:55:00-04:00` | 物理 EOF `7170`；发单位于真实 EOF，截止尚未到；截至本轮未见本 task 的 CLAIMED，也无 Implementation/Repair；只记录，不接管 |
| C | `W-696-COMMON-BOX-TYPED-ADAPT-1` / `13:55:00-04:00` | 物理 EOF `3911`；已于 `13:36:55-04:00` CLAIMED；声明写集为 Cloud `CommonBoxService.java` + C 固定日志；尚无本 task 的 Implementation/Repair |
| D | `W-696-TEAM-RETURN-MEMBER-BUTTON-TYPED-ADAPT-1` / `13:56:00-04:00` | 物理 EOF `4549`；已于 `13:37:13-04:00` CLAIMED；声明写集为 Cloud `TeamReturnService.java` + D 固定日志；尚无本 task 的 Implementation/Repair |

本轮仅登记领取状态；旧 `UI_CLEAN` Implementation/Repair 不计作本轮触发材料。下一轮以四个上述 task id 在各自当前 EOF 之后出现的首个新 Implementation/Repair 为预检触发点。

### CLAIMED STATUS POLL #2 - 2026-07-14T13:43:24-04:00

- B 已在领取截止前于物理 EOF `7176` 追加：`W-696-TASK-TRACKER-PANEL-RECT-TYPED-ADAPT-1`，`claimedAt=2026-07-14T13:40:00-04:00`，声明写集为 Cloud `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java` + B 固定日志。
- 至此 A/B/C/D 四单均已 CLAIMED；截至本轮仍未见任一上述 task id 的新 Implementation/Repair，继续只读轮询。

### EOF STATUS POLL #3 - 2026-07-14T13:45:29-04:00

- B 物理 EOF 已增至 `7197`，新增 `W-696-TASK-TRACKER-PANEL-RECT-TYPED-ADAPT-1` 的“前提缺口报告 #1”，不是 Implementation/Repair。
- B 明确记录 Java 零改动，active blob 仍为 baseline `ad46ec861758737944dda82d784335a9405242f3`；其报告称当前 Service 无可达 `TASK_TRACKER_PANEL_RECT` fact 实例，且 baseline screen-absolute panelRect 与 fact `WINDOW_CLIENT_PX` 之间的转换来源未在写集内闭合，等待父级裁定。
- Helper 仅记录该 EOF 事实，不作最终判断、不接管；首个 Implementation/Repair 触发条件仍未满足。

### B 前提缺口独立预检 - 2026-07-14T13:51:12-04:00

> **性质：非批准结论，仅列候选事实与父级复核点。** 本节只读核对 B 固定日志真实 EOF 与当前源码；未改 Java，未运行构建或测试。

1. **活动调用图候选事实：** `TaskTrackerPanelService` 的 live 公共入口 `findWuhuanNextGreenClickPoint`（`:123`）、`prepareWuhuanPathingLink`（`:152`）、`readWubeiTrackerPanel`（`:188`）、`readXiuluoTrackerPanel`（`:281`）及 `getCroppedTaskDetailInTrackerPanel`（`:377`）均经 `cropTaskDetailInTrackerPanel(...):568-625` 到 `findTitlePoint(...):685-701`，后者在 `:688` 唯一调用 `resolveTrackerPanelRect(source):744-803`。该 resolver 先做本地窄区 anchor、expanded vision 与必要 drag，再用 screen-absolute rect 执行 `tracker.captureToFile`，返回 raw path + screen-absolute origin；replay/snapshot 分支直接走已有图像，不经过该 resolver。
2. **上下文可达性候选事实：** 活动类 `:98-103` 仅有 tracker/coordinate/OCR/temp/input/canonicalizer 六个协作者，调用链只传 `source/templates`，无 `TaskExecutionContext`、`CloudGameClient` 或 fact 参数。`TaskExecutionContext#getGameClient()` 与 `CloudGameClient#readWindowFact(...)` 均已存在；`TaskExecutionContextHolder` 也是 Spring 组件，但全 Cloud 源码未找到任何外部 `callWith(...)` 绑定点，故仅在本文件注入该 ThreadLocal holder 仍不能证明 current context 可用。另有 `CloudTaskRunCurrentContextSlot#current()`，但它是每 run authority 内部能力，当前 Service 无引用/注入来源，构造也不对该包开放。
3. **fact 语义与实例来源候选事实：** DHXY `LocalRemoteGameCommandHandler:802-805` 以 exact access binding 调用 `TaskTrackerPanelRectLocalObservationMechanics.observe(binding)`，再由 `toTaskTrackerPanelRectFact:969-995` 一一映射 `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/REPOSITION_REQUIRED/MECHANICS_FAILED`；wire 解码后才成为 Cloud `WindowFact.TaskTrackerPanelRectFact`。`PRESENT` 携 anchor、panel 四边与 score，全部是 `WINDOW_CLIENT_PX`；其它状态不得携坐标。该本地 mechanics 只截取固定窄区、无 focus/input/drag，越过安全 anchor 范围返回 `REPOSITION_REQUIRED`，并不执行 baseline 的 expanded-search/drag fallback。
4. **写集内可实施性候选判断：** B 所述“当前限定写集内没有可消费 fact 实例”有直接源码支持：单改 resolver 既拿不到已授权 run context，也无法把 client-px rect 诚实填入下游明确为 screen-absolute 的 `TrackerPanelCapture`/click 结果；fact 本身还不带后续 OCR 所需 panel image，且其 `REPOSITION_REQUIRED` 行为不等价于 baseline expanded/drag。若严格同时遵守“只改此文件、只替换定位点、不新增 owner/wrapper、不改 caller/remote/fallback/state/log”，当前材料未显示闭合路径；这是 helper 候选判断，最终范围裁定仍归父级。

**候选风险：** P1 候选为 exact task context/fact 实例不可达，以及直接采用现有 fact 会丢失 expanded-search/drag fallback；P2 候选为 `WINDOW_CLIENT_PX` 与下游 screen-absolute origin 的转换、panel capture artifact 均未定义。未发现 B 修改 Java：活动文件按仓库过滤后的 blob 与 `696a12b0` 均为 `ad46ec861758737944dda82d784335a9405242f3`。

**父级重点复核：** Cloud `TaskTrackerPanelService:98-103,568-625,685-803`；`TaskExecutionContextHolder:19-32` 的实际绑定者缺失；`CloudGameClient#readWindowFact` 的 retained-action 前提；Cloud `WindowFact.TaskTrackerPanelRectFact:254-304`；DHXY `LocalRemoteGameCommandHandler:802-805,969-995` 与 `TaskTrackerPanelRectLocalObservationMechanics#observe`。重点决定真实 typed artifact/context 如何进入 caller、谁负责 client-to-screen 或坐标契约改造、以及 baseline expanded/drag 与一次 panel capture 如何等价保留。

### A/D Implementation #1 独立预检 - 2026-07-14T13:58:20-04:00

> **性质：非批准结论，仅列静态候选事实与父级复核点。** A/D 固定日志真实 EOF 分别为 `5056`/`4582`；本节未改源码，未运行 Maven/test/runtime，未执行 Git mutation。

#### A - `W-696-LEFT-TOP-STATUS-TYPED-ADAPT-1`

- **交付/写集事实：** 声明写集为 Cloud `LeftTopStatusSwitchService.java` + A 固定日志。活动 Java 为 untracked，mtime=`13:44:24`，SHA-256=`252949B5...BB20` 与日志一致；仓库过滤后的 `696a12b0`/active blob 为 `a46fde69...`/`f42e7e52...`。限定 no-index diff 仅该文件，`92 insertions / 98 deletions`：删除 desktop/OpenCV/template/runtime/input 字段与匹配簇，适配 fact、pending API 和 bundle；当前材料未见声明目标以外的可归因交付，但共享 dirty 树不能据全局状态反推作者。
- **fact/coordinate 与 bundle 已核不变量：** `OBSERVED` 下 `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED` 在 `:184-198` 一一映射，score 原值传递；Cloud fact 构造契约要求 `OPEN` 同时携 `clickX/clickY` 且坐标空间只能是 `SCREEN_ABSOLUTE_PX`。`:207-217` 的单 bundle 顺序为 `MOVE_MOUSE(x,y)` -> `SLEEP(120)` -> `CLICK_LEFT(x,y,delay=250)`，无拆分；支持任务 allowlist、public 入口、两条业务日志文本及 pending reason 字符串与 baseline diff 对齐。pending 改用 exact-run `TaskExecutionContext` 四个既有 API，消费/重标条件未见新增 TTL/retry/owner。
- **P1 候选：terminal/stop/interrupt 被业务失败吸收。** `detect:170-182` 把 `NOT_EXECUTED/UNKNOWN/STOPPED` 全折叠为 `CAPTURE_FAILED`；`InterruptedException` 仅恢复 interrupt flag 后也返回 `CAPTURE_FAILED`。`moveAndClickLeft:211-218` 则把所有非 `EXECUTED`（包括 `UNKNOWN/STOPPED`）直接转 `false`，没有 checkpoint 或上抛。相较现有 Cloud terminal 约定（先 `TaskCheckpoint.throwIfStopRequested`，`NOT_EXECUTED` 可折叠，`UNKNOWN`/未确认 `STOPPED` 上抛），这会让 caller 在 stop/unresolved 后继续返回普通业务结果，`probeMemberStartup`/`consumeFollowerSafeWindow` 还可能继续写 pending 状态。
- **P2 候选：`rect` 日志语义误报。** baseline `CoordinateHelper#getScaledRect` 返回 `{windowBaseX+offsetX, windowBaseY+offsetY, xStart+width, yStart+height}` 的 screen-absolute 四边；active `detect:161-166` 改为 `{8,147,11,19}`，而未改的 `formatRect:248-253` 仍把数组解释为 `left,top -> right,bottom`。因此探测日志固定显示 `8,147 -> 11,19`（right < left、bottom < top），既非实际 fact capture rect，也非 baseline screen rect；点击坐标本身仍取 fact，不受此日志候选直接影响。本轮未形成 A 的 P0 候选。
- **父级重点复核：** `LeftTopStatusSwitchService:65-118`（pending）、`:160-182`（fact terminal/interrupt）、`:201-218`（bundle terminal）、`:248-253`（rect）；对照 `ReturnItemPrescanService:340-349` 的 terminal 分流，并决定是否要求 unresolved/stop 统一 unwind、如何在不恢复 desktop 依赖下诚实记录 rect。

#### D - `W-696-TEAM-RETURN-MEMBER-BUTTON-TYPED-ADAPT-1`

- **交付/写集事实：** 声明写集为 Cloud `TeamReturnService.java` + D 固定日志。活动 Java 为 untracked，mtime=`13:46:35`；仓库过滤后的 `696a12b0`/active blob 为 `286c5a85...`/`24108d1e...`，active blob 与日志一致。限定 no-index diff 仅新增所需 imports，并替换 `clickReturnTeamIfPresent(TaskExecutionContext,String)`；其余方法无 diff，统计为 `95 insertions / 25 deletions`。A/D 两份 no-index whitespace 核查均无诊断输出；exit=`1` 是存在内容差异，不等同 whitespace 错误。
- **两次 fresh fact/坐标已核不变量：** 同一 phase `team-return-member-button` 使用独立稳定 slot `detect-return-button` 与 `detect-return-button-refresh`，中间仍只调用一次 `ensureSheYaoXiangActive(context)`；第三 slot 为 `click-return-button`。不同 slot 形成不同 retained address，请求 final-consume 后同 slot 下次调用由 `CloudTaskRetainedActionState:328-347` 递增 occurrence，未见自动 retry。`PRESENT` fact 构造契约强制非空、非负 `SCREEN_ABSOLUTE_PX` 点；点来自第二次 fresh fact，再由 `getRandomizedPoint(Point,3,3)` 对 X/Y 分别均匀取 `[-3,3]`。
- **terminal/bundle 已核不变量：** 两次读均在非 `OBSERVED` 时先 checkpoint；`NOT_EXECUTED` 保持 no-match/disappeared `false`，`UNKNOWN` 或未由 checkpoint 确认为 stop 的其它态抛 `TaskFatalException`。点击非 `EXECUTED` 同序分流；读中断会恢复 interrupt flag 并抛出，零自动 retry。`CloudInputActionMapper` 逐项保序，bundle 恰为 `CLICK_LEFT(x,y,delay=150)` -> `SLEEP(500)`，坐标空间为 `SCREEN_ABSOLUTE_PX`。
- **timestamp/log/state 已核不变量：** 第一次 `PRESENT` 后、香检查前更新 `lastReturnButtonFoundAtByWindow`；仅 bundle `EXECUTED` 后更新 `lastReturnButtonClickedAtByWindow` 并返回 `true`。found、disappeared、ready 三条原日志及 `logReturnButtonNoMatch` 调用位置与 baseline 对齐；leader wait/precheck/pathing、私有 `findReturnTeamButton` 和其它 TeamReturn 方法未改。限定静态材料中未形成 D 的 P0/P1/P2 候选问题；`inputSequences`/旧私有 finder 变为未引用但被刻意保留，属于当前构造器/API 冻结点。
- **父级重点复核：** `TeamReturnService:76-161`；Cloud `WindowFact.TeamReturnButtonFact:172-198`；DHXY `TeamReturnButtonLocalObservationMechanics#observe` 与 `LocalRemoteGameCommandHandler:792-796,891-913`；`CloudInputActionMapper#toDtos`；最终统一编译仍由父级执行。

**Helper 交付说明：以上是非批准结论，等待父级最终审查；固定 preflight 报告保持 append-only，可继续接收后续 A/B/C/D 新交付。**

### C Implementation #1 独立预检 - 2026-07-14T14:02:27-04:00

> **性质：非批准结论，仅列静态候选事实与父级复核点。** 目标为 `W-696-COMMON-BOX-TYPED-ADAPT-1`；C 固定日志真实 EOF=`4004`。未修改 Java，未运行 Maven/test/runtime，未执行 Git mutation。

- **交付/写集事实：** 声明写集为 Cloud `CommonBoxService.java` + C 固定日志。活动 Java 为 untracked，mtime=`13:47:04`，`696a12b0`/active blob=`195c1dbf...`/`58e34496...`，与 Implementation 记录一致；限定 no-index diff 仅该文件，`132 insertions / 131 deletions`。当前材料未见声明目标以外的可归因交付，共享 dirty 树不用于反推作者。
- **`:101` null-context 核查：** `consumePendingBoxIfAllowed:86-105` 虽在 `:101` 直接调用 `context.hasWindow()`，但 `taskRunKey(context):398-406` 对 null 返回 null，调用方在 `:95-100` 已先返回，因此当前控制流下 null context 不可达 `:101`，本项未形成 NPE 候选。`detectBox:226` 与 `hasPendingBoxForCurrentWindow:169-174` 也有相应 null/invalid-run 门。
- **P1 候选：检测 terminal/stop/interrupt 被降为普通 miss。** `detectAndRecord:263-264` 对明确 stop 只 `return`；`:269-272` 把所有非 `OBSERVED`（含 `STOPPED/UNKNOWN`）统一记为 `fact-unavailable` 后返回；`:290-293` 对 `InterruptedException` 仅恢复 interrupt flag、记日志后返回。`:294-297` 的宽 `catch (Exception)` 还会捕获 remote retained/current gate 抛出的 `TaskStopRequestedException`、`TaskCheckpointTransitionException` 等 RuntimeException，并同样降为 detection failure。以上路径均未按现有 Cloud terminal 约定 unwind。
- **P1 候选：点击 terminal/transport 被吞并保留 pending。** `consumeClick:343-350` 仅把 `EXECUTED` 判 true，故 `NOT_EXECUTED/STOPPED/UNKNOWN` 全变成 false；宽 `catch (RuntimeException)` 又把 stop/transition、协议错误及 final-consumption 中断包装异常变成 false。上层 `:138-149` 随后记“click failed; keep pending until TTL”，使 unresolved/stop 表现成普通可重试 pending。`NOT_EXECUTED` 折叠 false 可保留，但 `STOPPED/UNKNOWN` 与中断需由父级重点判定。
- **pending identity/taskRun/TTL 已核不变量：** key 仍为 `windowId|hwnd|role|taskKey|taskRunKey`；window id、normalized native-handle text、role、`playerIdentityEpoch` 和 exact task-run id 均来自同一 `TaskExecutionContext`。baseline 数字 run id 只做“有效性 + 字符串 key/equality”，无大小或顺序运算；active 改为 Cloud 原生非空 String run id 后仍只做 key/equality，因此当前用途类型等价。`PendingCommonBox` 13 字段、staleWindow/staleIdentity/staleTaskRun 门、detect/consume 分离及 `PENDING_TTL_MS=30_000L`、`now + TTL` 均未变。
- **fact/coordinate/bundle 已核不变量：** DHXY handler `:787-791,851-874` 以 exact binding 把 local client point 转为 `SCREEN_ABSOLUTE_PX`；Cloud `CommonBoxFact:137-168` 强制 `MATCHED` 点非负、score>=0.86，非 MATCHED 不携观测字段。active `recordMatched:300-327` 仍以 Cloud 当前 `System.currentTimeMillis()` 建 pending，并以 fact 点同时作为 template/click 点。`consumeClick:334-345` 的一个 bundle 严格为 `MOVE_MOUSE` -> `SLEEP(80)` -> `CLICK_LEFT(delay=120)`，无拆分或重排。
- **P2 候选：方法图超出“不得新增 helper/wrapper”约束。** 限定 diff 新增 `recordMatched:300-327`、`consumeClick:329-352` 与一行路由 `actionSlot:386-388`；其中后两者分别封装原调用点和仅拼接字符串。TTL 未新增/改变，但三个新私有 helper 需父级决定是否要求回收到原方法内。本轮未形成 C 的 P0 候选。
- **父级重点复核：** Cloud `CommonBoxService:86-105,258-352,386-431`；`TaskExecutionContext#getTaskRunId/getNativeWindowHandle/getPlayerIdentityEpoch`；Cloud `WindowFact.CommonBoxFact:137-168`；DHXY `CommonBoxLocalObservationMechanics#observe` 与 `LocalRemoteGameCommandHandler:787-791,851-874`。

**Helper 说明：以上仅为 C 的非绑定预检材料，等待父级最终审查；固定报告继续保持 append-only。**

### ARMED - Whole-Service Cohort - 2026-07-14T14:29:51-04:00

> **性质：非批准结论，仅为一次真实 EOF 快照；不启动会话内轮询。** 四单领取截止均为 `2026-07-14T14:48:00-04:00`；截至本快照未出现任一新 task 的 Implementation/Repair。

| Worker | task / 声明写集 | 固定日志真实 EOF 与领取事实 |
|---|---|---|
| A | `W-696-BATTLE-RADAR-WHOLE-ADAPT-1` / Cloud `BattleRadarService.java` + A 日志 | EOF `5176`；已于 `14:29:36-04:00` CLAIMED，声明写集匹配父级发单 |
| B | `W-696-NPC-CLICK-WHOLE-ADAPT-1` / Cloud `NpcClickService.java` + B 日志 | EOF `7244`；当前 EOF 仍为父级发单，尚无本 task CLAIMED |
| C | `W-696-PLAYER-STATE-WHOLE-ADAPT-1` / Cloud `PlayerStateService.java` + C 日志 | EOF `4225`；当前 EOF 仍为父级发单，尚无本 task CLAIMED |
| D | `W-696-DIALOG-WHOLE-ADAPT-1` / Cloud `DialogService.java` + D 日志 | EOF `4625`；当前 EOF 仍为父级发单，尚无本 task CLAIMED |

Helper 已 armed：后续收到新的显式检查指令时，再读取四份固定日志真实 EOF；若出现 Implementation/Repair，仅做声明/实际写集、desktop import、方法清单及明显 terminal 风险预检并继续 append 本报告。本轮未修改 Java 或 A-D 日志，未运行构建。

### ARMED UPDATE - Whole-Service Cohort Fully Claimed - 2026-07-14T14:35:46-04:00

> **性质：非批准结论，仅为一次新的真实 EOF 快照；不启动会话内轮询。** 四路均已在 `2026-07-14T14:48:00-04:00` 领取截止前 CLAIMED；各自 CLAIMED 之后均未出现新的 Implementation/Repair。

| Worker | task / 声明唯一 Service 写集 | 固定日志真实 EOF 与领取事实 |
|---|---|---|
| A | `W-696-BATTLE-RADAR-WHOLE-ADAPT-1` / Cloud `BattleRadarService.java` | EOF `5176`；`claimedAt=2026-07-14T14:29:36-04:00`；领取记录即 EOF |
| B | `W-696-NPC-CLICK-WHOLE-ADAPT-1` / Cloud `NpcClickService.java` | EOF `7250`；`claimedAt=2026-07-14T14:32:49-04:00`；CLAIMED 后仅有范围说明 |
| C | `W-696-PLAYER-STATE-WHOLE-ADAPT-1` / Cloud `PlayerStateService.java` | EOF `4229`；`claimedAt=2026-07-14T14:30:00-04:00`；领取记录即 EOF |
| D | `W-696-DIALOG-WHOLE-ADAPT-1` / Cloud `DialogService.java` | EOF `4627`；`claimedAt=2026-07-14T14:32:30-04:00`；领取记录即 EOF |

本快照未触发 Service 文件的写集/基线方法图/desktop import/typed boundary/terminal stop 风险预检。下一次仅在固定日志真实 EOF 出现对应 Implementation/Repair 后核对并 append；本轮未修改 Java、POM、主文档或 A-D 日志，未运行 Maven/test/runtime，未执行 Git mutation。

### PREFLIGHT MATERIAL - A/C/D Whole-Adapt Delivery #1 - 2026-07-14T14:48:21-04:00

> **性质：非批准结论，仅为 Delivery Preflight Helper 的静态、非绑定候选材料，等待父级最终审查。** 已连续读取 A/C/D 固定日志各自本轮 task 锚点至真实 EOF，并核对三个 Cloud Service 当前源码、`696a12b0` 同路径 blob、现有 remote typed 合同与可见 producer。未运行 Maven/test/runtime，未修改任何 Java/POM/主文档/A-D 日志，未执行 Git mutation。

| Worker | 固定日志真实 EOF | 可观察实际写集 |
|---|---:|---|
| A / `W-696-BATTLE-RADAR-WHOLE-ADAPT-1` | `5229` | Cloud `BattleRadarService.java` + A 日志。Java mtime=`14:36:51-04:00`，SHA-256=`62f4fcc3...4855c` 与交付声明一致；当前 blob `64b9eaee...` 不等于 696 blob `c5840e59...`。 |
| C / `W-696-PLAYER-STATE-WHOLE-ADAPT-1` | `4307` | C 日志；Java 零改动事实成立：当前/696 blob 均为 `096d8917...`，1483 行，Java mtime=`2026-06-30T01:43:39-04:00`。 |
| D / `W-696-DIALOG-WHOLE-ADAPT-1` | `4686` | D 日志；Java 零改动事实成立：当前/696 blob 均为 `d7b1c71e...`，2524 行，Java mtime=`11:26:01-04:00`。 |

共享 Cloud 树中三个 Service 当前均显示为 untracked，不能据此反推作者；本表只把交付日志、当前内容哈希和 mtime 能直接互证的 task artifact 记为实际写集，未发现三份交付自报写集外的 task-specific artifact。

#### A - BattleRadarService

- **696 方法图/已保留不变量：** 696 的 enter/exit signal、`armExpectedCombatExitWait`、两次 exit miss、15s/1s/4s fast/full-radar cadence、`4000/2000/10000` polling、`GameContext` transition 和 pending timestamp/battleCount 字段仍可逐段对应。指定 desktop imports 已清零；当前仅依赖 `GameContext` 与 `TaskExecutionContext`。
- **P0 候选 - 现有 caller 参数表静态不相容。** 696 API 是 `checkAndSyncCombatState()`、`checkFastExpectedCombatExitByAvatarDiff(String)`、`refreshFastExpectedCombatExitAvatarBaseline(String)`；A 在 `BattleRadarService:75,148,228` 分别强制增加 observation 参数且未保留 overload。当前 `AutoCombatService:146,150,201,225,425` 与 `NpcClickService:262` 仍调用旧参数表；仅凭 Java arity 即可定位编译失败候选，本轮按禁令未运行编译。
- **P0 候选 - observation 没有可达 producer/contract。** `CombatObservation`/`AvatarObservationResult` 只定义、只消费于 `BattleRadarService:50-65,75-189,228-249`，全 Cloud/DHXY caller 搜索没有构造者。`WindowFactKind:3-13` 没有 battle-state/avatar fact；`AUTO_COMBAT_PANEL` 只返回面板中心/marker。`CloudTaskRetainedActionState:576-633` 虽有 dormant battle-radar capture slots，但当前没有调用者，也不产出 A 的两个 DTO。因此本次“typed boundary”尚未连到 exact-window producer。
- **P1 候选 - exact context 生命周期与状态隔离。** A 把 per-run `TaskExecutionContext` 作为 singleton `@Component` 构造字段（`:40-43`）；该类型不是 Spring bean，现有模式是方法入参或 `TaskExecutionContextHolder`。`CloudServiceConfiguration:19-23,34-67` 也未注册此 bean。即使后续人为注入，`:444-449` 仍在 context/null/blank 时回落 `"default"`，与本单禁止 default/global state 且要求 exact current context 的条件冲突，并可能合并窗口状态。
- **P1 候选 - terminal/stop 无表达位。** 六个 boolean observation 和 `UNAVAILABLE` enum 无法表达 remote `STOPPED/UNKNOWN`、final-consumption interruption或 stale/current transition；Service 内也无 checkpoint。producer/caller 后续若把这些终态折成 null/false/UNAVAILABLE，会把 stop/协议不确定降为普通 radar miss。父级需在 producer 边界先 unwind，再允许构造 passive observation；同时复核 selection=`zhaohuan OR chehui`、top=`nu AND yuan` 的 696 映射未被单布尔 producer 改义。
- **父级重点：** `BattleRadarService:40-78,88-131,148-189,228-249,444-449`；`AutoCombatService:132-153,199-225,423-425`；`NpcClickService:257-263`；`WindowFactKind`、`WindowFact.AutoCombatPanelFact`、`CloudTaskRetainedActionState.BattleRadarSemanticSlot`。

#### C - PlayerStateService prerequisite-gap delivery

- **基线/desktop import：** 当前文件与 696 byte-exact，约 50 个方法的身份/位置、first-aid、治疗、香状态与 OCR、pending/state 方法图均未变；`GameClientTracker`、`TextRecognizer`、`InputProvider`、`CoordinateHelper`、`LocationVisionService`、`WindowScopedTempPath`、`WindowTaskContextHolder`、`BagService` 及 AWT/image imports 全部仍在。因此本交付没有引入源码回归，也没有完成 desktop dependency cutover。
- **现有 typed 面核对：** `WindowFactKind` 的 10 类确无 health/supply/location/incense observation；`CaptureOutcome:5-44` 仅给 image bytes/尺寸/scale/binding；`PlayerFirstAidDecision` 与 `CloudPlayerStateStateOwner/Governor` 是 decision/state consumer，不是窗口观测 producer。故 `PlayerStateService:168,689-705,801-940` 的 location/血条像素入口目前没有可直接读取的 typed fact。
- **P1 候选 - C 对 incense typed 能力的表述需要限定。** 仓库确有公开 typed same-process `SheyaoxiangStatusDecisionFacade:21,34,46-72` 与 `SheyaoxiangStatusCloudRequest/Decision`，可消费状态图并返回 present/remaining/action；但它没有 `@Component/@Bean`，依赖的 `DecisionEngine` 实例在 `CloudBrainServer:65` 手工创建且未注册进 `CloudServiceHost`。所以它目前对 `PlayerStateService` **不可达**，但前置工作更准确地可能是 authority/wiring + capture 终态接入，而不一定是重新发明 incense DTO/fact。父级需明确该 facade 是否属于本轮允许复用的既有合同。
- **MainBagSession 缺口成立：** `BAG_USE_INCENSE` port/producer 已可达，但 local handler 固定调用 `BagService.runUseIncenseMacroDirectForExclusive(null)`；该宏自行 open/close bag。696 `PlayerStateService:529-531` 接收外部已打开的 `MainBagSession` 并复用同一 anchor/exclusive section，两者不等价；现有四个 `LocalMacroKind` 无 open-session continuation。
- **terminal/stop 候选：** Java 零改动意味着本交付未新增终态折叠。既有 `CloudBagUseIncensePort:35-59` 对 `STOPPED/UNKNOWN` 走 fatal，方向可复用；后续 capture/facade 接入不得放进 `PlayerStateService:1083-1113` 的宽 `catch (RuntimeException)` 后变成 icon `UNKNOWN`，也不得把 final-consumption interruption降为普通补香 miss。
- **父级重点：** `PlayerStateService:159-188,238-374,506-638,689-705,801-940,1002-1298,1330-1357`；`WindowFactKind`、`CaptureOutcome`、`CloudBagUseIncensePort`、`SheyaoxiangStatusDecisionFacade`、`CloudServiceConfiguration`；DHXY `BagService:155-167,205-216,296-302,1394-1413`。

#### D - DialogService prerequisite-gap delivery

- **基线/desktop import：** 当前文件与 696 byte-exact，`handleDialog` 分派、prepared-action/fingerprint、option/story/OCR/template 顺序、remembered choice、give-item exclusive flow 与日志/return 均未改；tracker/input/coordinate/OCR/`WindowRuntimeContext`/temp-path imports 全保留。因此没有新增源码终态分支，也没有完成 desktop dependency cutover。
- **现有 typed producer/contract：** `WindowFactKind` 无 dialog/detection/OCR fact，`LocalMacroKind` 仅 `BAG_RETURN_ITEM/BAG_USE_INCENSE/NAVIGATE_IN_CURRENT_MAP/UI_CLEAN`，`CaptureOutcome` 只返回原始图像。Cloud 内虽有 package-private `DialogOptionRecognizer:17,22`，但只由 `DecisionEngine:2557,2585` 调用；当前没有类似 incense facade 的 public same-process typed producer。`DialogPolicyCloudDecision`/pre-click DTO 单独不是 observation producer。因此 D 所列 detection/OCR 可达缺口有源码依据。
- **连续独占缺口成立：** `DialogService:1350-1382` 必须在同一个 `submitExclusiveAndWait` 内完成 give-option detection/random click/800ms wait/`GiveItemService.executeGiveDirectForExclusive`；`:1780-1788` story click 和 `:2283-2364` green-template click也把 capture/analysis/input 放在当前独占段。现有 InputBundle 不能调用 DHXY-local `GiveItemService`，四个 closed macro 中也没有 dialog/give-item whole flow。
- **P2 候选 - “可闭合方法集为零”需父级定义粒度。** `handleDialog:136-143` 的 initial click 可独立映射 InputBundle，`captureDialogValidationImage:1132-1136` 可在取得 exact context 后映射 typed capture；它们不足以清掉类级 desktop imports，也不能闭合完整业务方法图，但说明“没有任何可迁移触点”与“没有可独立完成的 whole method”不是同一命题。父级需决定本单要求整类原子切换还是允许先落无语义漂移的完整触点。
- **P1 候选 - 未来 terminal 不能落入 baseline 宽 catch。** `validatePreparedDialogActionForConsume:1173-1202` 捕获所有 RuntimeException 并返回 null；若 typed capture/final-consumption/stop exception 直接嵌入该 try，会被降为 fingerprint miss。未来 closed macro/InputBundle 也必须区分 `NOT_EXECUTED` 与 `STOPPED/UNKNOWN`，不能一律映成 baseline `INTERRUPTED/FAILED`。
- **父级重点：** `DialogService:129-219,1132-1202,1339-1423,1506-1758,1771-1895,2153-2370,2486-2521`；`WindowFactKind`、`LocalMacroKind`、`CaptureOutcome`、`DialogOptionRecognizer`/`DecisionEngine`；DHXY `GiveItemService.executeGiveDirectForExclusive` 与 local macro handler。

**Helper 边界：以上仅为 PRECHECK/PREFLIGHT 候选事实，不替代父级审查，不给出最终结论。**

### PRECHECK - C BattleRadar Cloud Fact Implementation #1 - 2026-07-14T15:06:13-04:00

> **性质：非批准、非绑定的静态预检材料，等待父级最终审查。** 目标为 `W-696-BATTLE-RADAR-CLOUD-FACT-1`；只读 C 固定日志真实 EOF 和四个 Cloud remote 源码，未运行 Maven/test/runtime，未调用 Git，未修改任何 Java/POM/其它日志。

- **领取/交付/可观察写集：** C 日志 `:4324-4355` 给出父单与 `14:56:00-04:00` CLAIMED，Implementation #1 位于 `:4357-4414`，读取时真实 EOF=`4414`。任务窗口 `14:54-15:04` 内，Cloud `remote/` 目录仅四个文件出现新 mtime：`WindowFactKind.java` `14:58:22`、`RemoteCommandOutcomeEnvelope.java` `14:58:28`、`WindowFactOutcome.java` `14:58:42`、`WindowFact.java` `14:59:04`；其行数和 SHA-256 均与 `:4362-4366` 声明逐项一致。当前可观察 task artifact 因而与“这四个 Java + C 日志”的唯一写集一致；共享 dirty 树下不把其它既存文件反推为 C 所写。
- **7 kind / 3 fact / 3 enum：** `WindowFactKind:4-13` 保持原 10 项顺序，`:14-20` 恰追加父单指定的七项。`WindowFact:3-8` 的 sealed permits 为原 10 fact 加 `BattleRadarSignalFact/BattleRadarMinimapFact/BattleRadarAvatarFact`，共 13 项；新 record 与三个 state enum 位于 `:337-404`，名称、字段及 `4/3/6` 个枚举值均与父单 `:4337-4346` 一致。
- **constructor invariant：** signal/minimap 分别在 `WindowFact:339-341,353-355` 强制 state 非空；avatar 在 `:373-393` 强制 state 非空、六个 nullable `Integer` 坐标 any-to-all 成组，并在成组后要求 `right>left && bottom>top`。半组坐标和零/负面积 rect 均不能通过构造器。
- **17-kind matches/parse 全矩阵：** `WindowFactOutcome:28-52` 与 `RemoteCommandOutcomeEnvelope:391-417` 都是无 `default` 的穷尽 switch。三种 signal kind 同映射 signal fact，minimap 单映射 minimap fact，三种 avatar kind 同映射 avatar fact；旧 10 kind 仍各自映射原 fact。`WindowFactOutcome:8-21` 同时保留 `WINDOW_FACT` operation、禁用 `EXECUTED`、`OBSERVED` 必须携匹配 fact、其它状态必须 fact=null 的边界。
- **既有 10 kind 当前不变量：** enum 的 `BINDING` 至 `TASK_TRACKER_PANEL_RECT` 仍位于 `WindowFactKind:4-13`；matches 位于 `WindowFactOutcome:30-41`；parse 位于 `RemoteCommandOutcomeEnvelope:393-406`，逐项类型未缺失、未重绑。由当前源码可确认语义矩阵无回归；“历史逐字未改”仍主要来自 C 自报 `:4398`，本轮因明确禁用 Git 未建立旧 blob 的独立字节对照。
- **禁区静态证据：** BattleRadar 新增只落在上述 enum/permits/record/matches/parse 区域；没有新增 owner/session/ledger/TTL/retry。既有 envelope 的 digest 校验、scope/session 及 owner 字段属于原共享协议路径，未被新 case 调用或扩展。任务窗口前 `pom.xml` mtime=`13:11:18`、`RemoteProtocolDigests.java`=`12:46:42`、`RemoteGameCommandBroker.java`=`12:48:00`；源码树未找到 codec 命名 Java 文件，未见 task-specific 禁区 artifact。

**候选风险/父级复核点：**

- **P2 候选 - 状态与坐标只做结构约束：** `WindowFact:373-393` 允许任一 avatar state 携完整坐标或完全不携坐标，也未限制 hover 坐标非负。父单只明确“可空、成组、rect 正面积”，因此这不是已证偏差；若后续 producer/consumer 假定 `BASELINE_CAPTURED` 必有坐标或 `NOT_CONFIGURED` 必无坐标，父级需先明确该跨字段不变量。
- **P2 候选 - 交付过程证据自相矛盾：** C 在日志 `:4360,4407` 声称未做 Git，但 `:4362-4366,4402-4403` 同时列出 blob、`git diff --check` 与 `git status` 结果。此项不改变上述源码矩阵，但相关“scoped check”应由父级视为 worker 自报而非一致的独立证据。
- **本轮未形成 P0/P1 静态候选。** 父级重点复核 `WindowFactKind:4-20`、`WindowFact:3-8,337-404`、`WindowFactOutcome:8-21,28-52`、`RemoteCommandOutcomeEnvelope:197-203,391-417`，以及首次 BattleRadar producer 对 avatar 状态/坐标组合的实际构造方式。

**Helper 边界：以上仅列候选风险与当前源码证据，不给出审批结论。**

### PRECHECK - A BattleRadar Whole Adapt R1 Repair #1 - 2026-07-14T15:16:32-04:00

> **性质：非批准、非绑定的静态候选材料，等待父级最终审查。** 目标为 `W-696-BATTLE-RADAR-WHOLE-ADAPT-1-R1`；读取时 A 固定日志真实 EOF=`5327`。本轮只读 A 日志、active/`migration-baseline/696a12b0` BattleRadar、四个 fact 合同与现有 caller/装配点；未运行 build/test/runtime，未调用 Git，未修改 Java 或其它日志。

- **写集证据：** A 在日志 `:5252-5282` 领取的唯一 Java 写集为 Cloud `BattleRadarService.java`；active 文件 mtime=`15:04:09.136-04:00`、525 行、SHA-256=`564eb87...d76d4ef`，与 Implementation `:5284-5286` 一致。`14:58:20-15:06:00` 的 Cloud `service/` 目录窗口内只观察到该 Service 新 mtime；当前可归因 task artifact 为该 Java + A 日志。

**候选问题与证据：**

- **P1 候选 - exact per-run Service 当前没有可见构造/注入路径。** active `BattleRadarService:37-50` 已移除 Spring stereotype，并新增 final `TaskExecutionContext`；但 `CloudServiceConfiguration:19-23` 仍扫描 `com.bot.dhxy.service`，`AutoCombatService:27-40`、`NpcClickService:96-105`、`NavigationService:85-88,178` 均仍是 Spring component 且 constructor-required `BattleRadarService`。当前 `src/main/java` 搜索不到 `new BattleRadarService(...)`、该类型的 `@Bean` 或 per-run factory。即便后续手工构造，Lombok constructor 未校验 context 非空，首个 fact 路径会在 `BattleRadarService:483` 先解引用，早于 `state():503-509` 的 fail-fast。父级需确认本单是否允许把 caller graph 装配列为后续前置，否则三处现有 caller 目前没有可达的 exact-context 实例。
- **P2 候选 - avatar 成功日志未恢复 baseline hover/ROI 诊断。** 696 baseline 的 baseline-captured、changed、refresh-success 日志分别在 `:170-173,192-195,256-258` 输出 hover。active 对应 `BattleRadarService:143-145,161-163,226-227` 均未输出 hover 或 ROI；只有 `:276-282` 的 `UNAVAILABLE/NOT_CONFIGURED/MECHANICS_FAILED` warning 会读取 fact 六坐标。成功 fact 在 `captureFastExpectedExitAvatar` 返回 enum 后丢失坐标，所以上层成功日志无法补出 A 在 `:5315` 自报的 hover/ROI 诊断。
- **P2 候选 - “18 方法”自报与 active 方法图不一致，并出现 typed helper 层链。** 696 baseline 静态方法表为 `11 public + 7 private = 18`；active 为 `11 public + 11 private = 22`。原 18 项职责均有对应点，但另增 `isSignalCaptured:453-455`、`readSignalState:457-466`、`readMinimapState:468-478`、`readFactOrNull:480-501`；当前存在 `isMapViewVisibleForCombatExit -> readMinimapState -> readFactOrNull` 及 signal reader -> generic reader 两层。父级需据 R1 的“不得新增 wrapper 链”判定这些层是否各自形成真实 typed/terminal 边界；A 日志 `:5313-5314` 的“11+7=18”不能描述当前源码。

**已核关键证据：**

- **三 public 签名/caller：** active `:60,126,206` 已恢复 `checkAndSyncCombatState()`、`checkFastExpectedCombatExitByAvatarDiff(String)`、`refreshFastExpectedCombatExitAvatarBaseline(String)`；`AutoCombatService:146,150,201,225,425` 与 `NpcClickService:262` 均按该参数表调用，未再引用旧 observation DTO。
- **七 fact 按需顺序：** Stage 1/2/3 在 active `:61-90` 依次读 AUTO/SELECTION/TOP，任一可见即短路；Stage 4 只在原 IN_COMBAT 分支、第二次 miss 后经 `:94-105,296-300` 读 MINIMAP。avatar baseline 只在 `:135-146` 未 ready 时读，probe 只在 15s 与 1s gate 后于 `:148-160` 读，refresh 只在 IN_COMBAT gate 后于 `:206-216` 读；未观察到后续 stage 预取。四个 fact 源码哈希仍为 C 交付的 `1084fecf.../f6e3783c.../2716ac33.../7983a7f3...`，`WindowFactOutcome:8-20,42-51` 继续强制 OBSERVED 类型矩阵。
- **terminal/type boundary：** `BattleRadarService:480-500` 对 `OBSERVED` 返回 typed outcome、`NOT_EXECUTED` 返回原调用点失败语义；`STOPPED` 先 `TaskCheckpoint`，未确认停止再 fatal；`UNKNOWN` 及协议不可能的 `EXECUTED` 走 fatal；`:485-487` 恢复 interrupt flag 后 fatal。signal/minimap/avatar 在 `:272-274,462-464,474-476` 再做 fact subtype 检查；共享 signal/avatar kind 的请求-响应 kind 等同性由现有 broker `RemoteGameCommandBroker:1773-1776` 强制。
- **state/业务基线：** `state():503-509` 以 constructor-bound context 的 exact `windowId` 为 key，对 null/blank 不再回退 default；该 key 粒度与 696 `:486-490` 的 windowId 粒度一致。enter/exit、两次 miss、15s/1s/4s、4000/2000/10000、pending/timestamp/battleCount 与 signal consumption 方法均有 696 对应；active 未出现 preserved-only 的 `applyExternalCombatStateVerdict`、pause/unconsumed-enter 等方法。desktop symbols/imports（tracker、coordinate、minimap reader、temp path、window holder、ImageFinder、TeamTaskProperties、BufferedImage、Spring stereotype）静态命中数为 0。

**Helper 边界：以上仅列候选问题与证据，不给出审批结论。**

### PRECHECK - A BattleRadar R2 Repair #2 + C PlayerState First-Aid Mechanics #1 - 2026-07-14T15:43:00-04:00

> **性质：非批准、非绑定的独立静态预检材料，等待父级最终审查。** 点名范围为 A `W-696-BATTLE-RADAR-WHOLE-ADAPT-1-R2 Implementation Repair #2` 与 C `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1 Implementation #1`。本轮未运行 Maven/test/runtime，未调用 Git，未修改 Java、A/C 日志或其它文档。

- **真实 EOF/后续状态：** 完整重读时 A 固定日志 EOF=`5482`，R2 交付在 `:5389-5432`，其后父级 Source Review #3 在 `:5434-5452`，再后是另一任务领取；C 固定日志 EOF=`4593`，Implementation #1 在 `:4468-4540`，其后父级 Source Review #1 与 R1 指令在 `:4542-4593`，截至该 EOF 尚无 R1 `CLAIMED`/repair。两份点名源码 hash 仍与各自交付一致。
- **基线/边界：** `docs/DHXY_CONTEXT.md:15-23` 只允许 DHXY exact-window closed mechanics 承担观测/固定输入并返回 typed result，Cloud caller 保留 phase/order/delay/retry/fallback；`docs/业务逻辑.md:946-963,1015-1017` 固定战后急救短窗口边界；本轮方法与顺序对照使用 filesystem `migration-baseline/696a12b0`，未使用工作树其它迁移态作为业务权威。

**实际写集：**

- **A：** 声明为 Cloud `BattleRadarService.java` + A 日志。源码 mtime=`2026-07-14T15:23:31.0965444-04:00`、532 行、SHA-256=`e90e99fb9444bad960bc5c0b648eea51501ced1aaa8ed26b8061f53b46b86405`；`15:21:00-15:26:30` Cloud 全 `src/main/java` mtime 窗口仅该 Java 命中，未观察到点名单写集外源码落点。
- **C：** 声明为新建 DHXY `service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java` + C 日志。源码 mtime=`2026-07-14T15:25:16.6614116-04:00`、483 行、SHA-256=`7a9b82a6400761d2700efc8bf7dc5c8989036bc41c5c56957f7ddd88f747ea93`；`15:15:00-15:27:30` DHXY 全 `src/main/java` mtime 窗口仅该 Java 命中，未观察到点名单写集外源码落点。

**A 候选与已核不变量：**

- **P1 集成前置候选（不是 R2 源码行为偏差）：live caller 仍无法给 holder 建立 authority。** `TaskExecutionContextHolder:17-34` 只由 `callWith` 写 ThreadLocal；Cloud 全 `src/main/java` 搜索仅见该方法声明，没有 producer。现有 `AutoCombatService:126-150,199-202,223-225` 明明持有显式 context，却直接调用无参 radar 方法；因此 `BattleRadarService:505-507` 在真实 tick 上会 fatal，七 fact 与 `state()` 当前均不可达。A 在源码 `:35-39` 和日志 `:5393-5396` 已如实声明 integration pending；父级应重点复核未来 task-entry 是否在同一实际执行线程包住完整 caller graph，不能只让 bean 可构造。
- **方法/业务图保持：** active 仍为 `11 public + 9 private=20`；696 的 11 个 public 签名、Stage 1 AUTO -> Stage 2 SELECTION -> Stage 3 TOP -> 原 IN_COMBAT 第二次 miss 后 MINIMAP、avatar 15s/1s/4s gate、enter/exit signal、pending/timestamp/battleCount、4000/2000/10000 polling 均有原位对应。新增的两个 private 仅为 `readFact:481-503` 与 `currentTaskContext:505-508`；未再出现 R1 的三层 routing helper。
- **fact/terminal/state：** 七处固定 identity 为 `auto-flag/selection/top/minimap/avatar-baseline/avatar-probe/avatar-refresh`（active `:67,76,94,155-156,186-187,254-255,322`），均按需读取且无 poll-index/TTL/retry。`readFact:481-503` 保持 OBSERVED typed、NOT_EXECUTED -> 原 null 失败语义、STOPPED -> checkpoint 后 unresolved fatal、UNKNOWN/EXECUTED/interrupt -> fatal；`state:510-517` 只用 holder context 的非空 `windowId`，无 default/global。
- **日志与 import：** 三条成功日志 `:171-173,190-192,267-268` 输出 typed fact hover，失败日志 `:162-165,199-202` 另输出 ROI。`696a12b0:170-173,192-195,256-258` 的成功日志本就只含 hover；最新 A EOF `:5447-5449` 也已纠正先前 R2 文本中过宽的“成功日志带 ROI”要求，故此项不再列候选。active imports 仅 Cloud context/remote fact/Spring/Lombok/JDK map，desktop tracker/coordinate/image/temp/input import 为零。

**C 候选问题与证据：**

- **P0 候选 - 当前源码存在静态未解析结果类型。** `PlayerStateFirstAidLocalMacroMechanics:89,94,97,102,111,127` 使用并实例化 `FirstAidMechanicalResult`，但本文件 `:417-483` 只声明 intent/toggle、两个 status enum、`TargetOutcome` 与 private target record；整个 DHXY `src/main/java` 搜索也没有同名 class/record/interface/enum，且 imports 中没有该类型。父级统一编译前应先复核这一缺失声明；按当前源码表面无法解析 public 返回类型。
- **P1 候选 - 把 696 两条真实机械边界合成了一个会输入的入口。** 696 no-focus 路径 `PlayerStateService:259-299` 使用 `captureBarsSnapshotNoFocus`，只生成 ordered candidate plan，零 mouse/input；输入在 `performCachedFirstAidPlanNow:307-375` 或独立 `healAll/healAllDirect:445-478` 执行。C `runFirstAid:105-123` 却在一次入口内调用带 mouse-away 的 capture、分类、350ms 复核并直接右击，若接到 background probe 会把只读预检变成 physical input，pending 两阶段语义也无法由 typed result 等价表达。
- **P1 候选 - exact-window/input-worker authority 仅靠调用者假设。** public `runFirstAid:89-128` 只检查 binding 有 handle/geometry，不校验 binding handle/geometry 与 context 的 native window 相同，也不拒绝非 `dhxy-input-action-worker`；`:191` 与 `:387` 直接调用 `InputProvider`。全 `src/main/java` 当前只有本类自身引用该类型，尚无 caller 可证明它位于 `submitExclusiveAndWait` callback。坐标算术本身可等价，但父级接线必须同时证明 binding/context 同源和 worker 已持有，不能把 JavaDoc 当运行时门。
- **P1 候选 - stop/interruption 会被局部结果化或漏过 physical input。** `:96-98,118-120,413-415` 使用可空 context 与本地 boolean wrapper，只看 stop token，不处理 pause/identity suspension/线程 interrupt；候选循环遇 stop 后 `continue`，最终仍返回 snapshot CAPTURED。`:192,318,388` 又忽略 `TaskSleep.sleep` 的 boolean；`TaskSleep:24-34` 在 interrupt 时只恢复 flag 并返回 false，因此 350ms 被打断后当前代码仍可继续 capture/right-click，800ms 被打断后也会继续下一候选。该行为不等价于 696 `healAll(taskContext):474-478` 的 transaction 前后 checkpoint。
- **P2 候选 - typed outcome 顺序不再固定。** `findSupplyTargetsFromSnapshot:238-250` 在扫描时先把 HEALTHY 项写入 outcomes，`runFirstAid:116-123` 随后才追加全部 candidate 结果；人物血需补、人物法健康时会返回“人物法 -> 人物血”，而非固定“人物血 -> 人物法 -> 宝宝血 -> 宝宝法”。实际 candidate 点击间相对顺序虽仍按原 targets，但返回合同与逐目标执行图均已重组。
- **P2 候选 - 两个 closed status 不是实际观测事实。** `isSupplyNeededFromSnapshot:263-266` 的越界 false 会在 `:243-247` 被记为 HEALTHY；`:333-340` 在 area 复核不足但中心像素健康、未点击时记为 SUPPLY_NEEDED。前者应表达 unreadable，后者应表达 no-action/confirmed-center-healthy 一类事实，否则 Cloud consumer 会把“无法读”或“最终未需动作”误作健康/仍需补给。

**C 已核关键不变量：** 常量、threshold 30/50/70、sample radius `(2,1)`、higher `+10`、350/100/800ms 与颜色公式和 696 一致。`CoordinateHelper:127-133` 的所谓 scaled rect 实际也是 `windowBase + raw offset`，故 C `:154-164,305-306` 的 binding base + client-relative ROI/click 在单位上无额外 scale 漂移；mouse obstruction 读取在 `:217-225` 保留 physical/scale -> logical，`InputProvider` 最终再 logical*scale。首次 bars 在 `:114-126`、confirm bars 在 `:319-343` 均有 finally flush，`BoundWindowCaptureService:69-87` 也会释放整窗源图并返回复制 crop。未见 cooldown/task phase/owner/session/ledger/TTL/retry 字段或对现有 Service/schema/handler 的修改。

- **父级重点复核锚：** A `BattleRadarService:35-55,65-130,145-209,246-329,481-517`、Cloud `TaskExecutionContextHolder:17-34`、`AutoCombatService:126-150,199-225`；C `PlayerStateFirstAidLocalMacroMechanics:89-128,154-225,238-344,413-483`、696 `PlayerStateService:259-375,445-478,697-970`、`TaskSleep:24-34`、`BoundWindowCaptureService:46-87`。C EOF 已有 R1 指令但尚无新交付，本材料只描述当前 Implementation #1，不接管 repair。

**Helper 边界：以上仅列候选风险、已核不变量与父级复核点，不给出审批结论。**

### PRECHECK - B TeamReturn Leader Live Fact + D BattleRadar DHXY Fact - 2026-07-14T15:26:04-04:00

> **性质：非批准、非绑定的合并静态预检材料，等待父级最终审查。** 本轮点名范围为 B `W-696-TEAMRETURN-LEADER-LIVE-FACT-ADAPT-1 Implementation #1` 与 D `W-696-BATTLE-RADAR-DHXY-FACT-1 Implementation #1`。未运行 Maven/test/runtime，未调用 Git，未修改任何 Java 或其它日志。

- **四路真实 EOF：** A=`5432`、B=`7362`、C=`4466`、D=`4823`。A 在本轮分析期间新增 R2 repair（`:5385-5432`），已完整读到 EOF，但不扩展本次明确点名的 B/D 范围；C EOF 仍只是 first-aid mechanics 领取。
- **真实写集：** B 领取窗口 `15:11:05-15:15:00` 的 Cloud `service/` 仅 `TeamReturnService.java` 出现新 mtime=`15:13:22.239`，639 行，SHA-256=`ebb92a36...3df9ee`，与 B `:7331-7335` 声明一致。D 领取窗口 `14:58:51-15:13:00` 的 DHXY `src/main/java` 仅六个声明文件出现新 mtime：三个 `RemoteBattleRadar*Fact`、`BattleRadarLocalObservationMechanics`、`RemoteWindowFactKind`、`LocalRemoteGameCommandHandler`；可观察 task artifact 与“六 Java + D 日志”一致。

**候选问题与证据：**

- **P1 候选 - D 的 selection/top 不再保持 696 短路求值。** 696 `BattleRadarService:95-96` 是 `zhaohuan || chehui`，`:112-113` 是 `nu && yuan`，第二模板分别只在第一模板未命中/已命中时读取。D `BattleRadarLocalObservationMechanics:177-184` 先同时加载两模板，再无条件执行两次 `ImageFinder.find`，最后才组合布尔值。结果是 selection 第一模板已命中但第二模板缺失时会返回 `MECHANICS_FAILED` 而非 `VISIBLE`；top 第一模板未命中但第二模板缺失时会返回 `MECHANICS_FAILED` 而非 `NOT_VISIBLE`。D 日志 `:4803` 的“OR/AND 顺序保持”未覆盖这个行为差异。
- **P1 候选 - D 把 capture RuntimeException 折成了 capture unavailable。** `BattleRadarLocalObservationMechanics:263-282` 的 `captureRoi` 对 `captureService.captureRegion` 的 RuntimeException 记录后返回 null；上层 single/dual signal 在 `:148-150,172-174` 把所有 null 统一映为 `CAPTURE_UNAVAILABLE`。父单 D `:4768-4769` 要求异常映 `MECHANICS_FAILED`，仅原 capture 缺失映 `CAPTURE_UNAVAILABLE`；当前实现无法区分这两类。avatar `:217-252` 与 minimap `:108-119` 则已把 RuntimeException 映为 `MECHANICS_FAILED`。
- **P2 候选 - B 的 checkpoint 覆盖范围宽于声明 terminal 矩阵。** `TeamReturnService:228-237` 对所有非 `OBSERVED` 状态先执行 `TaskCheckpoint`，然后才判断 `NOT_EXECUTED` 或抛 fatal；因此 `NOT_EXECUTED/UNKNOWN` 遇到同时发生的 stop/stale transition 时会先抛 stop/transition，而不是 B 日志 `:7349-7353` 所写的固定 false/fatal 类型。该路径不会降为 ABSENT，也不自动重发；父级需决定是否要求 checkpoint 仅置于 `STOPPED` 分支。

**已核关键证据：**

- **B baseline/typed/exact binding：** 696 `TeamReturnService:102-125` 的初检 -> timeout/deadline -> loop checkpoint -> `TaskSleep.sleep` -> 复检 -> disappeared/timeout log/return，在 active `:172-195` 次序和检测次数不变；三个替换点为 `:173,186,360-361`，固定 phase=`team-return-leader-signal`、slot=`wait-initial/wait-poll/member-no-match-diagnostic`，无 poll index。`readLeaderReturnSignalPresent:220-249` 使用参数中的 exact `TaskExecutionContext#getGameClient`，OBSERVED 仅 `PRESENT` 为 true，类型不匹配和 interruption 均 fatal；request/outcome kind 等同性仍由 Cloud broker 强制。DHXY producer 的 `LocalRemoteGameCommandHandler:802-806` 在 exact `access.context()/binding()` 下生成 leader fact。public local detector/precheck 仍在 active `:207-208,261+`；member-button 的 detect -> 摄妖香 -> refresh -> `CLICK_LEFT(150)+SLEEP(500)` 与 found/click timestamp 位于 `:76-161`，未被本单改序。
- **D 7-kind/fact/handler：** `RemoteWindowFactKind:4-20` 保持旧 10 项并追加七项；三个新 fact 的字段与 `4/3/6` 枚举值镜像 Cloud 合同，avatar `:25-58` 强制六坐标 all-or-none 和正面积。handler `:811-847` 的七 case 均位于 `windowTaskContextHolder.callWith(access.context())`，使用 `access.binding()`；avatar 另传 exact windowId/playerIdentityEpoch，mechanics key=`windowId/nativeHandle/playerIdentityEpoch` (`:225-226,383-384`)。handler `:850-875` 保留 timeout、registration/binding re-read 和 OBSERVED terminal，`:1048-1105` 状态映射逐项闭合。
- **D 696 行为/禁区：** 模板路径、阈值、ROI 数值、avatar `isMatch(...,0.35)`、baseline/refresh overwrite 与 probe lazy baseline 均可对应 696；`CoordinateHelper.getScaledRect:127-133` 实际也是 window base + 原始 offset/size，故 D 的 bound-window raw ROI 算术未单独形成 scale 漂移。任务窗口内 `pom.xml`、`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java` mtime 均早于本单；六文件未新增输入、线程、TTL/retry 或新的 owner/session/ledger，handler 内同名字段是既存共享协议设施。当前未形成可由静态源码单独证实的 P0 候选；编译证据按父级禁令未生成。

- **父级重点复核：** Cloud `TeamReturnService:172-249,346-362`；DHXY `BattleRadarLocalObservationMechanics:144-193,195-284`、`LocalRemoteGameCommandHandler:743-875,1040-1105`、三个 `RemoteBattleRadar*Fact` 与 `RemoteWindowFactKind:4-20`。

**Helper 边界：以上仅列候选问题与证据，不给出审批结论。**

### EOF INDEX - W-696 BattleRadar R2 / PlayerState First-Aid #1 - 2026-07-14T15:44:00-04:00

- 本轮完整非绑定 PRECHECK 位于本报告 `:238-270`；A 重点候选/不变量从 `:250` 开始，C 候选从 `:257` 开始，父级复核锚在 `:268`。本索引仅保证父级从真实 EOF 可定位材料，不增加或改变任何审查结论。

### PRECHECK TIMING ADDENDUM - C R1 Claim - 2026-07-14T15:45:00-04:00

- 收口复读时 C 固定日志真实 EOF 已由 `4593` 增至 `4597`：`:4595-4597` 新增 `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1` 领取，声明写集仍为同一 mechanics Java + C 日志。该 Java 仍为 483 行、SHA-256=`7a9b82a6400761d2700efc8bf7dc5c8989036bc41c5c56957f7ddd88f747ea93`，尚无 Implementation Repair；故本条只取代 `:242/:268` 的“尚未领取”时序描述，`:257-266` 的候选仍精确限定于 Implementation #1 旧 hash，不预判后续返修。

**Helper 边界：本补记只同步并发领取事实，不给出审批结论。**

### PRECHECK METADATA CORRECTION

- 主机最终静态复核时点为 `2026-07-14T15:41:13.3521052-04:00`。本轮标题 `:238/:295/:299` 中手填的 `15:43/15:44/15:45` 早于对应主机时钟到达，只可视为段落序号，不作为证据时间；证据时点以本行、源码 mtime/SHA-256 与日志 EOF 为准。此更正不改变 `:238-270` 的候选事实和行锚。

### PRECHECK - A NPC Ctrl / B Tracker Capture / C First-Aid R1 / D Battle-Radar R1

**材料快照与实际写集。** 本段复读四日志真实 EOF：A `:5572`、B `:7527`、C `:4682`、D `:4975`。四个目标 Java 均为 `??`；任务写入时段的 `src/main/java` mtime 窄扫只出现这四个新文件。A=`ae18d7c037427099843a4e921a211d2b01a370c2`，B=`674862c487ef9f8b94f5e3a48e989dca681f3243`，C=`3888b641215ddcb2c6a3495934c117250aa4d970`，D=`0cf6b3d6e982b57d463898942bcfff99700a20ff`，均与各自交付日志相符；共享 dirty tree 外部文件无法仅凭 Git 归属个人，但本时段未见可归因的 Java write-set drift。按禁令未运行编译、测试或 runtime，以下“编译/API”仅为符号与调用签名静态核对。

#### A - `W-696-NPC-CTRL-PROBE-LOCAL-MECHANICS-1`（首版快照）

- **write-set drift：** 声明/实际均为新建 `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java` + A 日志，首版源码 blob=`ae18d7c…`，未见该时段其它 A 可归因 Java 写入。真实 EOF 已在 `:5550-5572` 发布并领取 R1，但尚无新的 Repair；本项只描述首版，不外推到后续版本。
- **编译/API 静态核对：** `GameClientTracker.captureToMemory`、`ImageFinder.isMatch`、`InputProvider`、`TaskSleep` 与 `WindowScopedTempPath.resolve` 均有现存签名，未见直接缺符号 risk；但该类当前只有声明自身的引用，尚无 caller/wiring。
- **P1 risk 候选：exact-window 与注入边界未闭合。** 成员仍为 `GameClientTracker`/`WindowScopedTempPath` (`:29-38`)，public `probe` 不接 `WindowNativeBinding` (`:92-97`)，两帧由隐式 tracker 上下文截取 (`:106-108,:125-127`)；类本身也无 Spring stereotype (`:25`)。父级需复核未来 handler 是否可能证明两帧、输入与命令属于同一 HWND，以及该 collaborator 如何按现有构造注入。
- **P1 risk 候选：所谓 closed intent 实际是可执行回调。** `CtrlMenuKeywordScan` (`:47-50`) 及 `BooleanSupplier` (`:92-97`) 把 OCR/fuzzy/click/verify 留给 caller，实际调用发生在 Ctrl hold 区间 (`:109-146`)；而 696 的连续段还包含 `NpcClickService:515-583` 的 capture-clean/wash/OCR/首命中 move->100ms->click+verify。该函数对象不能作为 typed closed data，且边界允许关键区执行任意 caller 逻辑。
- **P2 risk 候选：结果不变量/不可变性未封闭。** 两个 public record 直接暴露可变 `Point` 与 caller-owned `int[]` (`:53,:73`)，无 defensive copy，也未校验 status/click/rect/reason 组合；返回后事实坐标可被改写。
- **已核 696 不变量：** 首版的 before capture -> hold Ctrl -> 80ms -> move -> 280ms -> stop -> after capture -> diff -> finally release -> 100ms 顺序在 `:106-147`，对应 696 `NpcClickService:381-427`，未见该时序本身漂移。父级重点文件/行：本 mechanics `:25-53,:73,:92-147`；696 `NpcClickService:370-429,515-583`。

#### B - `W-696-TASKTRACKER-PANEL-CAPTURE-LOCAL-MECHANICS-1`

- **write-set drift：** 声明/实际均为新建 `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java` + B 日志，blob=`674862c…`；未见 rect-only mechanics、remote/schema/handler 或 Cloud 文件被本单时段改写。B EOF 后续仅领取另一新单，不改变本交付快照。
- **编译/API 静态核对：** `BoundWindowCaptureService.captureRegion/captureWindow`、`InputProvider.dragAndDrop`、`ImageFinder.find(BufferedImage,BufferedImage,double)` 和 binding getters 均有现存签名；Spring stereotype/构造注入位于 `:33-64`，未见直接缺符号 risk。
- **P1 risk 候选：closed-result API 仍存在 unchecked exception 逸出。** `capturePanel` JavaDoc 承诺 non-null closed result (`:78-81`)，但外层只有 `finally` (`:103-185`)；narrow/expanded 的 `ImageFinder.find` (`:230,:271`)、drag (`:123`) 以及部分 `Math.addExact` (`:116-119,:146-149`) 未统一映射 `MECHANICS_FAILED`。matcher/native 或输入异常可绕过 State 矩阵直接抛到 caller。
- **P2 risk 候选：public canonical constructor 未复制 byte array。** 工厂与 accessor 有 clone (`:383-397`)，但 public compact constructor仅校验、不执行 `panelPngBytes = panelPngBytes.clone()` (`:357-381`)；外部直接 `new CaptureResultDto(...)` 时仍可在构造后改写 payload。
- **已核 696 不变量：** 常量/阈值在 `:37-55`；narrow -> expanded 在 `:192-290`；仅需 drag 时检查 input-worker，随后 direct drag + 500ms (`:111-130`)；严格保留 696 “先 drag、仍以拖前 anchor+offset 取 rect、再最终 capture”的顺序 (`:132-181`，基线 `TaskTrackerPanelService:744-803,1569-1583`)。panel PNG/hash/dimensions/origin 均来自同一最终 frame，template/frame 均有 flush。父级重点文件/行：本 mechanics `:81-185,:192-290,:348-397`。

#### C - `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1`

- **write-set drift：** 声明/实际均为 `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java` + C 日志；Repair blob=`3888b641…` 与真实 EOF `:4630-4682` 一致，未见本时段其它 C 可归因 Java 写入。
- **编译/API 静态核对：** R1 已补齐两个 public result family；`TaskCheckpoint` 明确接受 nullable context，capture/input 方法签名均存在，imports/调用处未见直接缺符号 risk。当前全仓无 mechanics caller，故这里只能确认局部 API，不代表完成接线。
- **P2 risk 候选：`HealOutcome` 允许半组 click 坐标。** `:525-531` 的 `hasClick = x != null && y != null` 只比较 `EXECUTED` 与“两者都在”；非 `EXECUTED` 且仅 X 或仅 Y 非 null 时两边同为 false，会通过构造器，和 `:514-516` 的“仅 EXECUTED 携坐标”不变量不一致。
- **无 P0/P1 risk 候选（本 R1 静态范围）。已核不变量：** no-focus 路径 `:101-125` 只有入口 checkpoint、无 mouse-away/输入/state；heal 路径 `:138-174` 有 input-worker 门及整段前后各一 checkpoint、目标间无 gate；固定人物血/人物法/宝宝血/宝宝法顺序在 `:177-183`；逐目标 initial -> +10 -> 350ms confirm -> right-click(100) -> 800ms 在 `:318-370,:402-415`，对应 696 `PlayerStateService:259-299,445-478,697-705`。capture images 均由各自 owner flush。父级重点文件/行：本 mechanics `:101-183,:285-370,:402-415,:487-533`。

#### D - `W-696-BATTLE-RADAR-DHXY-FACT-1-R1`

- **write-set drift：** R1 声明/实际均只改 `src/main/java/com/bot/dhxy/service/battleradar/BattleRadarLocalObservationMechanics.java` + D 日志，blob=`0cf6b3d6…`。原单其余五个 Java blob 仍为 kind=`c347d12a…`、SignalFact=`9d849ee2…`、MinimapFact=`88e9b3a7…`、AvatarFact=`9c7e1a97…`、handler=`b984f683…`；未见 R1 越界改写。
- **编译/API 静态核对：** `ImageIO.read`、`ImageFinder.find/isMatch`、capture APIs、minimap reader、properties getters、handler 构造/七 case/三映射均有现存签名，未见直接缺符号或 forced-switch 漏项 risk。
- **无 P0/P1/P2 risk 候选（本 R1 静态范围）。已核不变量：** selection 的首模板命中跳过第二模板 (`:194-202`)，top 的首模板未中跳过第二模板 (`:208-216`)，对应 696 `BattleRadarService:95-96,112-113`；capture RuntimeException -> `MECHANICS_FAILED`、真实缺图 -> `CAPTURE_UNAVAILABLE` (`:151-181`)；模板只在实际求值点加载并在 `finally` flush (`:223-237`)，短路模板不预载；capture frame 在 `:179-180` flush。avatar baseline/refresh/lazy store 与 probe-current lifetime在 `:240-306` 保持独立，R1 未改 key/ROI/0.35。
- **父级重点文件/行：** 本 mechanics `:89-122,:151-238,:240-306,:319-389`；当前 handler `LocalRemoteGameCommandHandler:811-875,1048-1105`；696 `BattleRadarService:77-140,154-200,237-259,297-315`。

**Helper 边界：以上仅为非绑定 PRECHECK risk 与证据，等待父级最终审查；不构成审批结论。**

### TRUE EOF STATUS CLOSEOUT - A R1 / B Dialog / C Incense / D Story

- **A EOF=`:5572`：** `W-696-NPC-CTRL-PROBE-LOCAL-MECHANICS-1-R1` 已于 `:5570-5572` 领取，声明写集为 `NpcClickCtrlProbeLocalMacroMechanics.java` + A 日志。目标文件仍为领取前的 150 行版本，mtime=`2026-07-14T15:39:49.1209086-04:00`、SHA-256=`94473717e3615426a163db6cc1dbbf223f9dbdca1835b3c3d0ac913bced45f02`；截至该 EOF 无新的 Repair，故本轮没有可追加的新源码 risk。**后续预检锚点：** 只处理 A 日志 `:5572` 之后的新 Repair，并重新核 exact binding/capture、Spring 注入、closed data/result、完整 OCR/click/verify 连续段及首版 risk 是否消失。
- **B EOF=`:7527`：** `W-696-DIALOG-DETECTION-LOCAL-MECHANICS-1` 已于 `:7523-7527` 领取，声明写集为新建 `service/dialog/DialogDetectionLocalMechanics.java` + B 日志；当前目标文件不存在，尚无 Implementation，也没有可归因的 Java write-set drift 或新源码 risk。**后续预检锚点：** 只处理 B 日志 `:7527` 之后的新 Implementation/Repair，核 exact binding、input-worker 内 Alt+4/settle、single-frame ownership、mask stddev -> lower green -> upper row-pattern 顺序及异常 terminal。
- **C EOF=`:4712`：** 父级对 FirstAid R1 的独立源码裁决位于 TRUE EOF `:4684-4695`；下一单 `W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1` 位于 `:4697-4708`，并已在 `:4710-4712` 领取。声明写集为新建 `PlayerStateIncenseStatusLocalObservationMechanics.java` + C 日志；当前目标文件不存在，故无新交付可预检、无可归因 write-set drift。**后续预检锚点：** C 日志 `:4712` 之后首个 Implementation/Repair，核 exact binding、0.85 图标阈值、状态列、青色小时优先于绿色分钟、零 input/state/cache/retry。
- **D EOF=`:5003`：** 父级对 BattleRadar R1 的独立源码裁决位于 TRUE EOF `:4976-4986`；下一单 `W-696-DIALOG-STORY-ADVANCE-LOCAL-MECHANICS-1` 位于 `:4988-5000`，并已在 `:5002-5003` 领取。声明写集为新建 `service/dialog/DialogStoryAdvanceLocalMacroMechanics.java` + D 日志；当前目标文件不存在，故无新交付可预检、无可归因 write-set drift。**后续预检锚点：** D 日志 `:5003` 之后首个 Implementation/Repair，核 input-worker 门、前后 `600+random.nextInt(100)`、scaled rect、bottom offset、`randomizePoint(...,30,10)`、左键 150ms 与 closed result。
- **收口事实：** 当前新材料只有四项领取/父级 TRUE EOF 状态，没有新的 A R1 Repair、B Dialog Implementation、C Incense Implementation 或 D Story Implementation；因此本段不新增 P0/P1/P2 risk 候选，也不把父级结论转写为 helper 自身判断。

**Helper 边界：本段仅为非绑定状态与后续预检索引，等待父级最终审查。**

### PRECHECK - A NPC Ctrl R1 / B Dialog Dependency / D Story #1 / C R2 Status

**证据快照。** 本段以固定日志真实 EOF A=`:5606`、B=`:7573`、C=`:4799`、D=`:5036` 为界；未运行 Maven/test/runtime，编译判断仅来自当前源码符号/API 静态核对。任务写入窗口 `15:57-16:01` 的 DHXY Java mtime 窄扫只出现 A、D 两个目标文件。

#### A - `W-696-NPC-CTRL-PROBE-LOCAL-MECHANICS-1-R1` Repair #1

- **write-set drift：** 声明/实际均为 `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java` + A 日志；当前 349 行，SHA-256=`fa11b1eff11ad9c3c79c2d5f46dbb2f87c062591b64a6b42a61d7f83c2d20542`、blob=`ca53c35516d730e004dbcb7dc3e1cc42d0717c50`，与日志 SHA 相符，窄时段未见 A 可归因的其它 Java 写入。
- **P0 risk 候选（编译闭包）：** 当前树没有 A `:4` 导入的 `com.bot.dhxy.core.TextRecognizer`，也没有 `:9` 的 `com.bot.dhxy.model.ocr.OcrWordResult`；`:13` 导入 `com.bot.dhxy.service.dialog.DialogService`，而实际类型是 `src/main/java/com/bot/dhxy/service/DialogService.java:1,66`；`:218` 调用 `ImagePreprocessor.washYellowText(String,String)`，当前 `ImagePreprocessor:14-135` 无该方法。上述均是当前源码组合下的疑似 javac 缺符号，不以未运行构建替代证据。
- **P1 risk 候选（Ctrl hold 内并非零 Cloud）：** `EXPECTED_DIALOG` 在 A `:300-307` 调当前 `DialogService.handleDialog`；该路径由 `DialogService:128-165,1397-1427` 进入分类，并在 `:1553,:1583,:1609-1614` 调 `ImageProcessorService`。当前实现 `CloudImageProcessor:12-17,115-127,202-232,269-284` 委托 `ImagePreprocessWashedImageClient.wash`，后者 `:38-60` 明确发送 Cloud preprocess。即使修正包名，源码所称 “Ctrl hold 内零 Cloud” 仍不成立；COMBAT verifier 的当前 `BattleRadarService:77-139` 则是本地路径。
- **P1 risk 候选（terminal 降级）：** 首次/重试 click 后的 stop、800/1000ms sleep interruption、combat 350ms interruption均在 A `:260-290,:310-324` 返回 false，caller `:247-254` 一律映为 `Status.NOT_FOUND`。这同时丢失 R1 声明的 `INTERRUPTED` 语义，以及 696 `NpcClickService:566-577` 的 clicked-but-not-verified 区别，可能把已点击或已中断事实当普通未命中。
- **P1/P2 risk 候选（exact binding 与第三帧 truth）：** public entry 同时接独立 `binding` 与 caller-supplied `windowBaseX/Y` (`:138-156`)；`BoundWindowCaptureService:46-84` 实际用该 base 计算绑定 HWND 帧内 crop，未验证 base 等于 binding origin，故 stale/mismatched base 可使 exact-HWND crop 失真。OCR 第三帧 `captureRegionToFile` 的 boolean 又在 A `:211-214` 被忽略，失败后仍洗图/OCR，无法返回 `BINDING_UNAVAILABLE`，并可能消费旧路径内容。
- **已核 696 不变量：** `@Service`/构造注入、primitive result、intent list defensive copy 已落在 `:43-95,:117-126`；before -> hold -> 80 -> move -> 280 -> after -> diff -> OCR/fuzzy -> move -> 100 -> click150 -> verify800 -> retry1000 -> finally release+100，以及 combat 4x350 的常量/顺序对应 696 `NpcClickService:218-270,370-429,507-583`。父级重点行：A `:3-25,:138-218,:247-324` 与上述当前 collaborator。

#### B - `W-696-DIALOG-DETECTION-LOCAL-MECHANICS-1` dependency conflict

- **write-set drift / 状态：** B blocker 位于 `:7529-7549`，目标 `DialogDetectionLocalMechanics.java` 仍不存在，`DialogService.java`、`ImagePreprocessor.java`、`ImageProcessorService.java` mtime 均早于领取，故本轮实际 Java 写集为零。父级在 `:7551-7573` 将后续权威写集扩大为新 mechanics + 定点补 `ImagePreprocessor.java`；截至 EOF 尚无真实 `CLAIMED_SCOPE_AMENDMENT` 或 Implementation。
- **P0 risk 候选（原单依赖冲突成立）：** 696 的本地静态 primitive 位于 `ImagePreprocessor:294/298/333/380/433/542`；当前 `ImagePreprocessor:14-135` 缺少 green/thin-white/stddev/row-pattern API。若在父级补回 helper 前按旧静态调用写新类，当前组合会形成疑似编译失败。
- **P1 risk 候选（不可用当前 Cloud 替代物）：** 当前 `ImageProcessorService:53-85,111-170` 虽公开相同指标，但 concrete `CloudImageProcessor:115-127,202-232,269-284` 会经 `ImagePreprocessWashedImageClient:38-60` 发 Cloud。把它注入 Alt+4 -> capture -> classify 连续段会把本地 mechanics 改成网络依赖；父级 `:7553-7555,:7571-7572` 已明确把此路径排除，后续预检应检查 imports/constructor/调用均无该 collaborator。
- **P1 risk 候选（异常不可降为视觉 miss）：** 当前 `DialogService` 把 processor 输出缺失分别在 `:1555-1559,:1585-1588,:1615-1624` 返回 false，随后分类主链 `:1416-1428` 可落为 NONE。新 closed mechanics 若复制该 terminal 处理，会把依赖 unavailable/exception 伪装成 mask/option/story 普通未中；后续实现需显式区分。已核 696 顺序/常量：mask stddev `<30` -> lower green `>150` -> upper thin-white+green/row-pattern，ROI `250/312/529/208`、small `250/345/529/143`、crop `42/58/161`、story `450/10/40/20/120`、Alt+4 settle `220`，当前 `DialogService:82-104,1405-1428,1485-1650` 与 696 `:1558-1597,1642-1760` 对应。
- **后续预检锚点：** B 日志 `:7573` 后首个真实 scope-amendment 领取与 Implementation；实际允许写集应只有 `DialogDetectionLocalMechanics.java`、`ImagePreprocessor.java`、B 日志，并逐方法比对 baseline helper、Mat release、single-frame ownership 与 typed terminal。

#### D - `W-696-DIALOG-STORY-ADVANCE-LOCAL-MECHANICS-1` Implementation #1

- **write-set drift：** 声明/实际均为新建 `src/main/java/com/bot/dhxy/service/dialog/DialogStoryAdvanceLocalMacroMechanics.java` + D 日志；99 行，SHA-256=`c8b5a805d0ed17fb802c33be6d1701d2458e8c9532314814584576811dfe5925`、blob=`9d7f805ae18281a45c021e1791c98f72afa8eed0`，与日志 blob 相符，窄时段未见 D 可归因的其它 Java 写入。
- **编译/API：** `CoordinateHelper.getScaledRect/getScaleRatio/getRandomizedPoint(Point,int,int)` (`:97-127`)、`InputProvider.clickLeft(int,int,int)` (`:12`) 与 `TaskSleep.sleep(long)` (`:24`) 均有当前签名；Spring bean、构造参数、record 语法未见疑似缺符号。无 P0/P1 risk 候选。
- **P2 risk 候选（typed terminal 粒度）：** 前置 sleep 中断在 D `:64-66` 返回 `INTERRUPTED` 且尚未点击；后置 sleep 中断在 `:74-77` 返回同一状态但点击已发生。该合并保持 696 boolean false 的表面语义，却使 typed caller 无法从 result 判断 side effect 是否发生；父级需确认 terminal 不会触发自动重试/第二次点击。同理 `NOT_ON_INPUT_WORKER` (`:60-63,:88-97`) 是对 696 `handleStoryDialog:1771-1777` 外层排队职责的抽取边界，caller 必须把它当 invocation gate 而非普通业务 miss。
- **已核 696 不变量：** D `:27-39,:60-81` 保持 input-worker gate、两次独立 `600+random.nextInt(100)`、large rect `250/312/529/208`、`cy=bottom-round(40/scale)`、随机半径 `30/10`、左键 150ms，逐项对应 696 `DialogService:1771-1789`；`TaskSleep` 保留 interrupt flag，coordinate/input 的 unchecked exception 未被降成普通 status。父级重点行：D `:41-97`。

#### C - R2 status only

- Incense 单已在 C `:4710-4712` 领取，并于 `:4740-4780` 交付 Java 零改动的 prerequisite report；父级后续状态在 `:4782-4788`。FirstAid `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2` 于 TRUE EOF `:4790-4799` 重发领取要求，但截至本段证据 EOF 只有模板行 `:4793`，**尚无真实 CLAIMED、尚无 Repair #2**。按用户要求，本轮不对 C 源码扩展预检。

**Helper 边界：以上仅为非绑定 PRECHECK risk、写集事实和后续锚点，不构成审批结论。**

### PRECHECK - C FirstAid R2 Repair #2

- **交付锚点：** C 固定日志真实 EOF=`:4845`；R2 在 `:4801-4803` 领取，Implementation Repair #2 位于 `:4805-4845`。当前 `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java` 为 541 行，SHA-256=`915f2b950d0c571ad6e7c45956e770add7f85a116913b2a4cf96b1a82437c3ce`、blob=`56388cf67ed0159e9cd9a9332190dd472f3b50bd`，与交付日志一致。
- **write-set drift：** `16:08:30-16:10:30` 的 `src/main/java` mtime 窄扫只出现该 FirstAid mechanics；目标文件为 `??`，声明写集为该源码 + C 日志。进一步从当前 LF 字节流内存删除新增 pair-check 三行，并把 `hasClick` 恢复为 R1 表达式后，重算得到 SHA-256=`f1e5b65aafe1e2f5210b87ef7a655831be97d15519d032355d1f0b4028ac51fc`、blob=`3888b641215ddcb2c6a3495934c117250aa4d970`，与 R1 完全一致；因此未见夹带 Java 内容或写集漂移。
- **唯一要求核对：** `HealOutcome:525-535` 先在 `:528-530` 用 `(X==null)!=(Y==null)` 拒绝单边非空，再在 `:531-535` 以 `executed = status==EXECUTED`、`hasClick = X!=null` 强制等价关系。静态矩阵为：非 EXECUTED + 双空通过；非 EXECUTED + 双非空拒绝；EXECUTED + 双非空通过；EXECUTED + 双空拒绝；任意 status + 单边非空均先拒绝。该顺序和“且仅 EXECUTED 携完整坐标”要求一致。
- **编译/API risk：** R2 只新增 null 比较与 `java.lang.IllegalArgumentException`，未增加 import、类型、方法调用或枚举值；现有 `HealOutcome` 调用点 `:166,:329,:335,:342,:349,:354,:358,:364,:366` 均传双空或 EXECUTED 双坐标。未发现本次改动引入的疑似编译符号 risk，也未形成新增 P0/P1/P2 risk 候选。按禁令未运行 build/test/runtime。
- **父级重点行：** `PlayerStateFirstAidLocalMacroMechanics.java:514-537`；字节级 R1 反推证据及 C 日志 `:4805-4839`。本 helper 自审仅是非绑定静态材料。

**Helper 边界：本段不作审批结论，等待父级最终审查。**

### PRECHECK - B Dialog Detection #1 / C FirstAid R2 Repair #2 / D Dialog Story R1 Repair #1

- **交付与写集总锚：** B 固定日志真实 EOF=`:7609`，scope-amendment 领取=`:7575-7579`、Implementation #1=`:7581-7609`；C EOF=`:4845`，领取=`:4801-4803`、Repair #2=`:4805-4845`；D EOF=`:5113`，领取=`:5084`、Repair #1=`:5086-5113`。目标源码当前 blob 分别为 B mechanics=`fa1f3840fec7c46333959daf71d7aae174570e53`、`ImagePreprocessor`=`0672cf0c3e13adb08ef9e9c89b18d01bfce31ddb`，C=`56388cf67ed0159e9cd9a9332190dd472f3b50bd`，D=`fe203435823617b8224784ed2232f62525ecd142`，均与各日志声明一致。

#### B - `W-696-DIALOG-DETECTION-LOCAL-MECHANICS-1` Implementation #1

- **write-set / API：** 声明与可归因 mtime 窄窗均只有新建 `src/main/java/com/bot/dhxy/service/dialog/DialogDetectionLocalMechanics.java`、定点修改 `src/main/java/com/bot/dhxy/tools/ImagePreprocessor.java` 及 B 日志，未见 write-set drift。`ImagePreprocessor:143-340` 只加回父单列出的 OpenCV imports/8 个成员；对应 baseline 方法锚为 `ImagePreprocessor:294/298/333/380/396/433/542/576`。当前 Java 21、OpenCV 依赖及 `BoundWindowCaptureService.captureRegion`、`DialogType`、`TaskSleep.sleep`、`InputProvider.pressAlt4` 符号均存在；源码无 `ImageProcessorService` import/call，未见 P0 编译/API risk 候选。
- **P1 risk 候选（terminal）：** `DialogDetectionLocalMechanics:97-99` 把 pre-wait 中断归入 `CAPTURE_UNAVAILABLE`；`:106-108` 发送 Alt+4 后忽略 settle sleep 的 false 并继续 capture。696 baseline `DialogService:1562-1563,1675-1678` 的确分别返回 NONE/忽略 settle，故这不是顺序漂移，但新 closed result 仍无法区分 stop/interruption 与窗口暂不可截；父级需重点核 typed caller 是否会把该状态当可重试 availability。
- **P1 risk 候选（异常分类）：** `DialogDetectionLocalMechanics:124-135` 将 `captureRegion` 抛出的 `RuntimeException` 与 Optional 空都归入 `CAPTURE_UNAVAILABLE`，而同一结果模型另有 `MECHANICS_FAILED`（`:293-297`），且分类/编码异常在 `:161-164` 才进入后者。父级需确认 capture exception 与普通 capture miss 是否应保持可辨，避免 terminal exception 被按 availability 重试。
- **P2 risk 候选（资源/窗口隔离）：** 当前 `ImagePreprocessor.ENABLE_DEBUG_SAVE=true`（`:22`），B 在 mask 调用传 `null`（mechanics `:182`，落到 `images/temp/debug.png`），option/story 又使用固定 `debug_hsv_mask_green.png` / `debug_hsv_mask_white.png`（`:195,:207-208`）；696 caller 原先通过 `windowScopedTempPath` 传窗口级路径（baseline `DialogService:1694-1699,1720,1730-1739`）。多窗口同时分类会覆盖同一诊断文件。另三处 crop 只在 helper 正常返回后 flush（mechanics `:178-183,:191-196,:203-211`），helper 抛异常时 crop 不在 finally；父级可一并核资源 ownership。
- **已核 baseline 不变量 / 父级重点：** `DialogDetectionLocalMechanics:83-168,174-224` 使用 caller exact binding、单次 HWND capture，保持 wait -> 可选 Alt+4+220 -> mask `<30` -> lower green `>150` -> upper story `450/10/40/20/120`，ROI/crop 常量与 696 `DialogService:1558-1597,1642-1760` 一致；frame 在 `:165-167` finally flush。重点复核 result invariant `:299-361`、上述 terminal 映射及 debug path。

#### C - `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2` Implementation Repair #2

- **write-set / risk：** 本次单点证据已落在本报告 `:390-398`：当前 541 行源码反推 R1 blob 完全一致，唯一变化为 `HealOutcome:528-535` 先拒绝 X/Y 单边非空，再强制 `EXECUTED` 当且仅当携双坐标；目标源码 + C 日志之外未见可归因 drift。没有新增 import、类型、调用或枚举，现有构造点均传双空或 EXECUTED 双坐标；未见本次变更新增的 P0/P1/P2 编译、terminal、资源或窗口绑定 risk 候选。父级重点仍为 `PlayerStateFirstAidLocalMacroMechanics:514-537`。

#### D - `W-696-DIALOG-STORY-ADVANCE-LOCAL-MECHANICS-1-R1` Implementation Repair #1

- **write-set / API：** 声明与 `16:16:35-16:20:01` mtime 窄窗均只有 `src/main/java/com/bot/dhxy/service/dialog/DialogStoryAdvanceLocalMacroMechanics.java` + D 日志，未见 drift。`WindowNativeBinding.getX/getY`、`CoordinateHelper.getScaleRatio/getRandomizedPoint(Point,int,int)`、`InputProvider.clickLeft(int,int,int)`、`TaskSleep.sleep(long)` 均有当前签名，未见 P0 编译/API risk 候选。
- **P1 risk 候选（exact binding gate）：** `advanceStoryDialog:64-68` 对 binding 只做 non-null，未检查当前模型已有的 `hasNativeHandle()/hasGeometry()`（`WindowNativeBinding:37-39,57-59`）；随后 `:72-81` 无条件按其 X/Y 计算并发送真实点击。`WindowNativeBinding.empty()` 因而可越过入口，在 input-worker 上生成约 `(514,480)` 附近的点击。父级需确认 caller contract 是否绝对排除 empty/stale geometry，或该 closed mechanics 应在物理输入前拒绝无有效几何的 binding。
- **已核 baseline/terminal 不变量 / 父级重点：** D `:69-84` 保持两次独立 `600+random.nextInt(100)`、large ROI `250/312/529/208`、bottom `40/scale`、随机半径 `30/10`、左键 150ms，逐项对应 696 `DialogService:1780-1788`；pre-click 中断=`INTERRUPTED_BEFORE_CLICK`、post-click 中断=`CLICKED_INTERRUPTED`，side effect 已可辨；无 tracker/getScaledRect、无新增 wrapper/retry/TTL，亦无图像资源。重点复核 `DialogStoryAdvanceLocalMacroMechanics:64-84` 的 binding 前置条件。

**Helper 边界：以上仅为非绑定静态 PRECHECK risk 与证据，不作审批结论；按任务禁令未运行 build/test/runtime，等待父级最终审查。**

### PRECHECK - D StoryAdvance R2

- **真实交付：** D 日志 EOF=`:5202`；R2 领取=`:5165`，Repair #2=`:5167-5201`。当前源码 blob=`f9ca338761288d425ce5ef8e605416639860d35f`，与日志一致；内存反推 R1 blob=`fe203435823617b8224784ed2232f62525ecd142`，表明目标源码只含本单 gate/Javadoc/enum 变化，未见可归因 write-set drift。
- **门序已核：** `DialogStoryAdvanceLocalMacroMechanics:66-73` 的 binding gate 是方法首个可执行分支，早于 input-worker gate、首个 sleep（`:73`）及 click（`:85`）；null、无 handle、`hasGeometry()==false` 均在 side effect 前返回 `BINDING_UNAVAILABLE`。
- **P1 risk 候选（ROI 容量缺失）：** `WindowNativeBinding.hasGeometry():59` 只校验 `width>0 && height>0`；当前入口 `:67` 未校验 `binding.getWidth() >= 250+529 == 779`、`binding.getHeight() >= 312+208 == 520`。因此正宽高但容不下完整 dialog ROI 的 binding 仍会 sleep 并计算/发送真实点击，未满足父单 exact binding/ROI closed gate 的完整条件。
- **closed 状态/API：** `BINDING_UNAVAILABLE` 在 `:68,:95-100` 可达，`Result` 在 `:103-106` 强制非空 status；`getWidth/getHeight/hasNativeHandle/hasGeometry`、`TaskSleep.sleep`、`getRandomizedPoint`、`clickLeft` 当前符号均存在，未见 P0 编译符号 risk。
- **R1 冻结项：** `:73-88` 仍为两次独立 `600+random.nextInt(100)`、ROI `250/312/529/208`、bottom `40/scale`、随机半径 `30/10`、click 150ms；前置中断未点击、后置中断已点击的状态语义保持。除上述 P1 外未见新增 P2 risk 候选。
- **Non-binding Preflight：** 本段只提供候选风险与精确证据，不作裁决；未运行 build/test/runtime，未修改源码、D 日志或主文档。

### PRECHECK - B Dialog Detection R1

- **真实交付/写集：** B 日志 EOF=`:7702`，领取=`:7677-7681`、Repair #1=`:7683-7702`；当前 mechanics 413 行、SHA-256=`12f7a37a5f6227ce53c1a5d5edd299b61cfb1928287a5b15476d26b5e4318f4e`，与日志一致，`ImagePreprocessor` mtime/hash 未变。窄窗另有并行 `TextRecognizer.java` mtime，因共享 dirty 无法归责于 B；目标可核写集未见漂移。
- **P1 risk 候选（未批准行为差异）：** 当前 `DialogDetectionLocalMechanics:121-123` 在 Alt+4 后的 220ms settle sleep 返回 false 时直接产出 `PRE_CAPTURE_INTERRUPTED`，不再 capture；696 baseline `DialogService.hidePlayerNamesBeforeDialogCapture:1675-1678` 调用同一 sleep 后忽略 false 并继续 capture。父单冻结 Alt+4/settle 顺序但未明确批准改变该分支结果，父级需重点复核。
- **P2 risk 候选（safeSource cap 漂移）：** 当前 `safeSource:273-278` 只 trim/sanitize，没有 baseline `safeDebugName:1763-1768` 的 `value.length() <= 120 ? value : value.substring(0,120)`；长 source 会进入 `debugPath:268-271`，形成超长窗口级文件名并可能使诊断写盘失败。
- **terminal 已核：** pre-wait false=`PRE_CAPTURE_INTERRUPTED`（`:104-108`），wrong-thread hide=`NON_INPUT_WORKER`（`:110-113`），input/capture/classify/encode exception=`MECHANICS_FAILED`（`:114-120,:140-149,:177-180`），仅无 frame=`CAPTURE_UNAVAILABLE`（`:150-152`）；除上述 settle 分支外未见 exception/unavailable 混淆。
- **资源/record 已核：** mask/option/story crop 均 finally flush（`:195-204,:212-221,:228-241`），frame finally flush（`:181-183`）；canonical constructor 在 `:360-387` 先 clone bytes，并由 `hasAnyField` 覆盖 top/right/bottom/width/height 及全部 metrics，accessor 在 `:408-411` 再 clone。
- **baseline/API：** ROI/阈值在 `:41-60` 未漂移，single-frame 与 mask -> option -> story 顺序在 `:140-176,:191-254` 保持；`WindowScopedTempPath.resolve`、capture/input/sleep/ImagePreprocessor 符号均存在，未见 P0 编译 risk。候选汇总 P0=0、P1=1、P2=1。
- **Non-binding Preflight：** 仅提供候选风险与行证据，不作裁决；未运行 build/test/runtime，未修改源码、B 日志或主文档。

### PRECHECK - B Dialog Detection R2 - 2026-07-14T16:54:21-04:00

- **真实交付/写集：** B 日志真实 EOF=`:7745`，领取=`:7730-7734`、Repair #1=`:7736-7745`；当前 `DialogDetectionLocalMechanics.java` 417 行、SHA-256=`655cf5ba3bffaef96c8e487fcab5d895e6924e7b97ac5bab6b66ea6edf9b7e32`，与日志一致。`16:45:24-16:46:28` Java mtime 窄窗仅该文件，`ImagePreprocessor` mtime/hash 冻结，未见 write-set drift。
- **定点行为：** pre-wait false 在 `:104-108` 仍返回 `PRE_CAPTURE_INTERRUPTED`；Alt+4 input exception 在 `:114-120` 仍为 `MECHANICS_FAILED`，而 settle 在 `:121-124` 调用 220ms sleep 后忽略返回值并继续 capture，恢复 696 `hidePlayerNamesBeforeDialogCapture:1675-1678` 分支。
- **safeSource：** `:274-281` 先 trim/sanitize，再以 `substring(0,120)` 恢复 baseline 120 字符 cap；`debugPath:269-271` 仍经 `WindowScopedTempPath.resolve`，未改变窗口级路径语义。
- **R1 冻结项：** exception=`MECHANICS_FAILED`、仅无 frame=`CAPTURE_UNAVAILABLE`（`:141-152,:178-183`）；mask/option/story crop 与 frame 均 finally flush（`:182-184,:196-242`）；canonical constructor clone、全 non-captured 字段 invariant 与 accessor clone 保持（`:347-415`）。ROI/阈值/single-frame/mask -> option -> story 未漂移。
- **candidate 汇总：** 未见本次 R2 新增 P0/P1/P2 candidate；现有 `WindowScopedTempPath.resolve`、capture/input/sleep/ImagePreprocessor API 均可定位。此为非绑定静态材料，不作裁决；未运行 build/test/runtime/Git。
### PRECHECK - D StoryAdvance R3
- **真实 EOF/写集：** D 固定日志真实 EOF=`:5255`；R3 领取=`:5227`、Repair #3=`:5229-5254`，声明写集仅 `DialogStoryAdvanceLocalMacroMechanics.java` + D 日志；当前源码 blob=`5899ea56a7b7dc4bef5b4437c4f0b02973b94391` 与交付记录一致。
- **首门已核：** `DialogStoryAdvanceLocalMacroMechanics:67-70` 在 input-worker 门（`:72-74`）、首个 sleep（`:75`）和 click（`:87`）前拒绝 null、无 handle、无 geometry、width `<250+529=779`、height `<312+208=520`。
- **R2 反推：** 相对 R2 记录的首门，R3 仅在现有 guard 新增当前 `:68-69` 两个 ROI 容量条件；门序、返回状态与其余方法体未见新增变化。
- **逐行冻结：** `:75-90` 的两次独立 `600+random.nextInt(100)`、ROI/offset/random point、`clickLeft(...,150)` 及 `ADVANCED/INTERRUPTED_BEFORE_CLICK/CLICKED_INTERRUPTED` 语义保持；`:97-108` 既有 status/result 不变。
- **符号/候选风险：** `WindowNativeBinding` 四个 guard accessor、`TaskSleep.sleep(long)`、`CoordinateHelper.getScaleRatio/getRandomizedPoint`、`InputProvider.clickLeft` 当前签名均存在；除新增两条件外未见新的 P0/P1/P2 候选风险，未运行 build/test/runtime。

### PRECHECK - A AutoBattle Whole Context Chain #1 - 2026-07-14T16:56:11-04:00

- **真实交付/写集：** A 日志真实 EOF=`:5748`，领取=`:5712-5714`、Implementation #1=`:5716-5748`；Cloud `AutoBattleTask`/`BaseTaskTemplate`/`TaskStepExecutor` SHA-256 分别为 `35c4f701...`/`cd39187d...`/`f0da982f...`，与日志一致。`16:34:09-16:43:08` Cloud Java mtime 窄窗仅三目标文件，未见 write-set drift。
- **P0 candidate（当前 builder 符号缺失）：** `AutoBattleTask:208-228` 在 `:212` 调 `TaskMaintenanceRequest.builder().allowFullMaintenanceBroadcastFallback(false)`；当前 `TaskMaintenanceRequest:34-61` 无该字段，因而 Lombok builder 不会生成此方法。全 Cloud 搜索仅 Dialog request 另有同名字段，未见 maintenance request 扩展点，父级需以统一 compile 重点确认。
- **P1 candidate（baseline 清理预算未接回）：** `AutoBattleTask:290-293` 保留 `summonSkillBudgetForRequestedTask`（`xiuluo_v2 -> 2`），但全文件没有调用；maintenance builder `:208-228` 未设置当前模型已有的 `maxSummonSkillCleanersPerTeamRound`（`TaskMaintenanceRequest:52-53`，默认 1）。该 helper 成为死代码，修罗 follower-support 的两窗口清理预算疑似从 2 降为 1。
- **context chain 已核：** `AutoBattleTask:111-116`、`BaseTaskTemplate:59-64` 在 validation 后以同一 explicit context 包住完整 lifecycle；`TaskStepExecutor:35-64` 仅包每次真实 attempt；`TaskExecutionContextHolder:19-30` finally 恢复 previous/remove。当前 main 源仅 AutoBattle 一个 Base 子类，未见构造器扩宽造成的其它 main caller 符号风险。
- **696 行为锚：** startup check -> RUNNING -> first aid -> maintenance init -> combat init -> tick/idle/sleep 在 `AutoBattleTask:116-153`；stop/state 在 `:173-177`；local return/follower-support/fallback 在 `:182-256`；500/3000/dynamic poll 与 no-retry 在 `:280-298`。除上述 builder 与预算两项外，未见方法、delay、fallback、stop/state 或 holder 泄漏 candidate。候选汇总 P0=1、P1=1、P2=0。
- **Non-binding Preflight：** 仅列候选与父级复核行，不作裁决；未运行 build/test/runtime/Git，未修改 Cloud/DHXY 源码或 A 日志。

### PRECHECK - C Local OCR / Incense Cohort #1 - 2026-07-14T16:56:11-04:00

- **真实交付/写集：** C 日志真实 EOF=`:4940`，领取=`:4890-4892`、Implementation #1=`:4894-4940`；四文件 SHA-256 `f816d516...`/`a9eb1e91...`/`8536fd1c...`/`7246eb30...` 均与日志一致。交付窗口四目标 mtime 可定位；另有并行 D mechanics mtime，因共享 dirty 不归责于 C，目标可核写集未见 drift。
- **P0 candidate（当前整树符号闭包）：** 新 `TextRecognizer:57-78` 只公开 `getAllTextResultsLocalOnly`，但现存 `NpcClickCtrlProbeLocalMacroMechanics:220-223` 调不存在的 `getAllTextResultsForMatch`；其 `:218` 还调当前 `ImagePreprocessor` 不存在的 `washYellowText(String,String)`。这是已知跨 worker 前置而非四文件夹带，但当前源码组合疑似无法完成 Java 编译。
- **P1 candidate（exception 被并入 unavailable）：** incense `captureRect:204-217` 捕获 `captureRegion` 的 `RuntimeException` 后返回 null，`matchIcon:178-184` 转 sentinel，公开入口 `:129-132` 最终给 `CAPTURE_UNAVAILABLE`，没有进入已有 `MECHANICS_FAILURE`。这与交付要求的 mechanics exception/unavailable 可辨存在冲突候选。
- **P1 candidate（写盘失败可变成 miss/陈旧事实）：** `writeImage:483-488` 吞 IOException 且无成功返回；调用点 `:187-193,:363-379,:416-453` 随后继续 `ImageFinder`/OCR。文件不存在时可降为 template/OCR miss，旧文件存在时可能读取陈旧窗口图，未闭合为 `MECHANICS_FAILURE`。
- **P1 candidate（stop/输入终态）：** 非 input-worker 的 ordered move+300ms 返回值在 `:254-257` 被忽略；direct `sleep:495-500` 仅恢复 interrupt 后继续 capture；`TextRecognizer:99,106-110` 又将 `HttpClient.send` 的 `InterruptedException` 包进 `Optional.empty()`。停止/中断因而可能被呈现为 OCR unavailable 或继续产出视觉结果。
- **P1 candidate（exact rect gate）：** entry `:103-113` 只核 handle/positive geometry/数组长度，未核 rect 正向、位于 caller binding 几何内；鼠标清障在 capture 前执行（`:233-258`）。错误窗口 rect 可先产生真实 move，再由 capture 层返回 unavailable。
- **P2 candidate（record 全字段 invariant）：** `IncenseStatusObservation:566-583` 的 `hasIcon` 仅在四坐标全非空时为 true；非图标态只拒绝 `hasIcon`，因此 public canonical constructor 可让 `CAPTURE_UNAVAILABLE/TEMPLATE_ABSENT/MECHANICS_FAILURE` 携单个或部分 icon 坐标，和“非图标态不携坐标”不变量不等价。
- **P2 candidate（template image ownership）：** `SheyaoxiangDigitTemplateReader:138-164` 每次从 `loadTemplates:271-287` 读取若干 `BufferedImage`，只 flush glyph/scaled copy，未在成功或早退 finally 中 flush template images。其余 status/matched-column/washed 图在 incense `:143-160,:342-466` 可见 finally 释放。
- **已核基线/本地边界：** `TextRecognizer:37-110` 仅 `127.0.0.1` sidecar、无 credential/Cloud/provider fallback，present-empty 与 unavailable 分开；incense 保持 cached narrow -> full（`:115-135`）、exact binding capture（`:204-217`）、template `0.85`（`:54-55,:187-190`）、matched column（`:304-340`）、cyan hour -> green minute（`:342-466`）和毫秒单位。除 P0 跨依赖外，四文件 imports 的 Jackson/input/capture/image/path 符号均存在。
- **candidate 汇总：** P0=1、P1=4、P2=2，均为非绑定候选而非裁决；未运行 build/test/runtime/Git，未修改 Java、C 日志或主文档。

### PRECHECK - D Navigate In Current Map Cloud Caller #2 - 2026-07-14T17:11:00-04:00

- **真实交付/写集：** D 日志真实 EOF=`:5369`，领取=`:5314`、Implementation #1=`:5316-5368`；`16:49:07-17:00:31` Cloud Java mtime 窄窗仅 `CloudNavigateInCurrentMapPort.java` 与 `NavigationService.java`，未见第三个 Java 路径。Port 当前 blob=`877ca5ff...` 与日志一致；NavigationService 当前 blob=`9388a972...`，不等于日志声明的 `915d2f65...`，构成 P1 交付身份/父级重锚候选。
- **P0 编译/API candidate：** Cloud `NavigationService.java:534` 传入 `TaskExecutionContextHolder` 调 `TaskCheckpoint.throwIfStopRequested(...)`；当前 Cloud `TaskCheckpoint.java:19-32` 唯一签名首参是 `TaskExecutionContext`，两类型无继承关系，静态符号疑似不能编译。
- **P1 stop 时点 candidate：** 新 Port `:42-45` 在任何 remote macro 调用前新增 exact-context checkpoint；696 baseline `migration-baseline/696a12b0/.../NavigationService.java:521-537` 先计算 tolerance/latency、读取并记录 map，再于循环首轮 checkpoint，且异常仍进入 `:679-691` 的关小地图/latency finally。入口已 stop 时，新链会在 DHXY 本地方法前退出，因而停止时点与这组 baseline 诊断/cleanup side effect 均提前；父级需确认该差异是否有明确授权。
- **14 字段：** Port `:46-60` 按 command `NavigateInCurrentMapMacroCommand.java:12-26` 的顺序直取全部 14 个 getter；DHXY handler `LocalRemoteGameCommandHandler.java:1376-1392` 逐字段反向 builder，还原 random/radius/keep-turn/tolerance 与 fresh map/X/Y/time/phase-bound，未见漏项或默认重算。
- **一次调用/exact binding：** Port `:42-45,:61-66` 只取 holder 当前 context 并调用一次 `context.getGameClient().executeLocalMacro`，无 default/epoch=0/retry；DHXY handler `:1355-1360` 在 `access.context()` 下、input queue 外只调用一次本地 `navigationService.navigateInCurrentMap(restored)`。
- **10 state/错误 terminal：** Cloud result enum `NavigateInCurrentMapMacroResult.java:17-27` 与 caller `NavigationService.java:542-553` 逐值覆盖 10 状态；envelope 非 EXECUTED 在 `:533-537` 走 checkpoint 后 fatal、无成功/miss/重发（但受上述 P0 API candidate 影响）。DHXY handler `:1361-1373,:1395-1406` 保持 stop envelope 与 10-state typed result 分层。
- **baseline side effect：** Cloud 薄壳未再执行 register/close；唯一 DHXY 本地调用仍在本地 `NavigationService.java:908-909,:988-1002` 注册 pathing intent、关小地图及记录 latency，未见 D 新链重复执行。除 stop-before-call 路径的上述差异外，未见本单误删/重复候选。
- **非绑定边界：** 本段仅为静态候选与精确证据，不作审批结论；未运行 build/test/runtime/Git，未修改 Java 或 D 日志。

### PRECHECK - A AutoBattle Whole Context Chain R1 - 2026-07-14T17:17:13-04:00

- **真实 EOF/写集：** A 日志真实 EOF=`:5825`，领取=`:5791`、Repair #1=`:5793-5825`；当前 `AutoBattleTask.java` SHA-256=`e13bfff7...`、`TaskMaintenanceRequest.java`=`d562734b...`，均与交付一致。`17:03:00-17:07:27` Cloud Java mtime 窄窗仅这两文件；冻结的 `BaseTaskTemplate`/`TaskStepExecutor` 仍为原 `cd39187d...`/`f0da982f...`，未见 write-set drift。
- **P1 定点修复：** `TaskMaintenanceRequest.java:12-16,:45-46` 恢复 Parent Repair 指定 JavaDoc 与 `@Builder.Default boolean allowFullMaintenanceBroadcastFallback=true`；`AutoBattleTask.java:208-228` 仍在轻量 probe 传 `false`，`TaskMaintenanceService.java:600-605` 读取同一 Lombok boolean getter，builder/getter 符号由一个字段闭合。
- **P2 定点修复/冻结：** `AutoBattleTask.java:280-293` 从 polling 直接进入既有 no-retry override，整树无 `summonSkillBudgetForRequestedTask`；模型 `:57-61` 的 round gate/default 1 未改，调用点未新增 cleaners budget。完整 explicit-context lifecycle `AutoBattleTask:111-228` 与两份 holder 文件指纹保持。
- **静态 candidate：** `@Value + @Builder(toBuilder=true)` 与两个现存调用的命名相符；本次未见新增 P0/P1/P2 candidate。此段只提供非绑定证据，不作裁决；未运行 build/test/runtime/Git，未修改源码或 A 日志。

### PRECHECK - C Local OCR / Incense Cohort R1 - 2026-07-14T17:17:13-04:00

- **真实 EOF/写集：** C 日志真实 EOF=`:5031`，领取=`:4987`、Repair #1=`:4989-5031`；当前 incense mechanics SHA-256=`55e90648...` 与交付一致，`17:06:00-17:14:30` DHXY Java mtime 窄窗仅该文件。冻结的 `OcrWordResult`/`TextRecognizer`/`SheyaoxiangDigitTemplateReader` 仍为 `f816d516...`/`a9eb1e91...`/`8536fd1c...`，未见 write-set drift。
- **两条 696 operation：** `probeIncenseStatus:108-166` 只做一次 full capture/time read；`probeIncenseIconPresence:180-208` 在 cached probe `PRESENT/UNKNOWN` 时于 `:196-197` 早退，仅 `ABSENT` 到 `:204` full fallback。模板 `0.85`、matched-column、cyan->green 与时间单位仍在 `:60-64,:125-159`，未见额外 capture/OCR。
- **边界/terminal：** 两入口 rect gate `:109,:185` 早于任何 move/capture `:114-115,:215-216`；`:273-289` 用 long 完成正向且 binding 内比较，cached rect 在 `:193` 再门控。`capture:241-248` 不吞 RuntimeException；status 的 exception/null 分别到 `:116-123` mechanics-failure/capture-unavailable，presence 分别到 `:205-207` unknown-exception 与 `:217-220` unknown-capture。
- **record/API/冻结：** status record `:611-631`、presence record `:681-692` 均拒绝 icon 部分字段并约束 typed state；当前 `captureRegion`、`submitAndWait`、`cropCopy`、`getAllTextResultsLocalOnly`、`recognizeAndLearn` 符号均可定位。Parent Repair 外的 baseline OCR/write-image/input 顺序未动；本次未见新增 P0/P1/P2 candidate。
- **非绑定边界：** 仅列静态材料，不作审批结论；未运行 build/test/runtime/Git，未修改源码、C 日志或其它文档。

### PRECHECK - D Navigate In Current Map Cloud Caller R1 - 2026-07-14T17:29:55-04:00

- **真实 EOF/写集：** D 日志真实 EOF=`:5452`，领取=`:5424`、Repair #1=`:5426-5451`；当前 Cloud `NavigationService.java` 的 CRLF-normalized blob=`687049e7e97705867104058c3417c998240c860f`，与声明一致。`CloudNavigateInCurrentMapPort.java` 已不存在，整份 Cloud main source 无该类型/字段引用。D 交付时窗另有 DialogDetection 命名文件及 shared sum-type 的并行 mtime，仅凭共享树 mtime不能归因给 D；D 两个目标动作本身未见 write-set drift。
- **696 逐行核：** 当前方法 `NavigationService.java:514-698` 为 185 行；删除仅用于当前类型闭合的 `:523-525` 三行 context lookup，并把四处 `taskContext` 名机械还原为 baseline holder 名后，与 `migration-baseline/696a12b0/.../NavigationService.java:512-693` 的 182 行逐行比较差异数为 0。
- **60s/候选/state：** null/坐标门=`:515-521`，60s loop/combat/arrival=`:536-553`，`resolveMiniMapClickPoint` + ordered attempted logical-point set/duplicate increment/exhausted=`:564-588`；fire-and-handoff 与普通 click 顺序=`:590-600`，PATHING_STARTED intent/state=`:601-614`，未见重算、批处理或 ledger 残留。
- **keep-turn/delay/fallback：** keep-turn deadline、STOPPED_AWAY、250ms wait/retry=`:614-650`；normal handoff=`:651-653`；start-exit fail-fast、NO_PATHING/combat、failed-click increment、200ms retry=`:655-678`，均与 696 对应分支和时点一致。
- **4 个 checkpoint：** current `:542/:600/:625/:678` 分别对应 baseline `:537/:595/:620/:673`（loop 首、click 后、keep-turn loop、200ms retry 后）；均传 `TaskExecutionContext taskContext`，匹配 `TaskCheckpoint.java:19` 的唯一签名。`:523-525` 位于 null/坐标门后，只取得 exact holder context、不检查 stop；被删除 port 的 pre-call checkpoint 已退出，因此未见新增 stop 时点。
- **finally：** `:684-696` 保持 PATHING_STARTED 缺 intent 时补注册、已 handoff 不重复关小地图、其它状态 `closeMiniMapIfOpen`、最终 latency/status/source/target；异常仍穿过同一 finally，未见 cleanup/state 漏失或重复。
- **dormant wire：** dedicated Cloud command/result SHA-256 仍为 `d8a9ecc4...`/`f560c97e...`（mtime 10:40），DHXY command/result payload 仍为 `84b4ad30...`/`9626de7c...`（mtime 10:42），handler mtime 15:10、navigate 分支仍在 `LocalRemoteGameCommandHandler:1127-1128,:1349-1418`，均早于 D 领取。shared `LocalMacroKind:7`、`LocalMacroCommand:5` 虽被并行 DialogDetection cohort 扩展，`NAVIGATE_IN_CURRENT_MAP` enum/permit/request/outcome 分支仍保留，未见 D R1 删除或改写该 dormant variant。
- **candidate 汇总：** 本次 R1 定点范围未见新增 P0/P1/P2 risk candidate；以上仅为非绑定静态材料，不作裁决。未运行 build/test/runtime/Git，未修改 Java、D 日志或主文档。

### PRECHECK - B Dialog Detection Closed Macro Chain Implementation #1 - 2026-07-14T18:01:31-04:00

- **材料/写集：** B 固定日志真实 EOF=`:7882`，任务=`:7769-7833`、领取=`:7835-7839`、Implementation #1=`:7841-7882`。本轮 18 个声明 Java 均落盘；17:09-17:46 mtime 窗另见 Cloud `NavigationService.java`（并行 D）、DHXY `PlayerStateIncenseStatusLocalObservationMechanics.java`（并行 C）及 `RemoteGameCommandBroker.java`。Broker 当前 SHA-256=`5b8d6b1c...` 与 B 领取前 C 日志 `:3917` 已记录值一致，故仅见时间戳、未形成可证内容越界；未见其它 B 内容写集漂移。
- **合同/codec 不变量：** Cloud `LocalMacroKind.java:4-9`、`LocalMacroCommand.java:4-6` 与 DHXY `RemoteLocalMacroKind.java:7-12`、两份 sealed permits `RemoteLocalMacroCommandPayload.java:4-9`/`RemoteLocalMacroResultPayload.java:4-9` 恰新增 `DIALOG_DETECTION`。Cloud envelope `RemoteCommandOutcomeEnvelope.java:73-84,:222-318` 与 DHXY codec `RemoteOperationPayloadCodec.java:48-60,:538-661` 对 EXECUTED+DIALOG 使用同一 20-key closed set，非 EXECUTED 仍为原 4-key null terminal；BAG/NAV/UI_CLEAN 分支仍在原 switch 中。
- **digest/frame 不变量：** Cloud `RemoteProtocolDigests.java:95-113` 删除 nested `dialogDetection.framePngBytes`；DHXY `RemoteProtocolDigests.java:188-279` 手工重建同一 NON_NULL typed tree且不放 bytes。local mechanics `DialogDetectionLocalMechanics.java:167-183` 对实际 PNG 求 SHA 并 finally flush frame；handler `LocalRemoteGameCommandHandler.java:1439-1485` 原样发 SHA/bytes/all 20 keys；Cloud port `CloudDialogDetectionPort.java:78-94` 重算 SHA、单次 decode、核尺寸后重建 image。
- **队列/terminal 不变量：** `LocalRemoteGameCommandHandler.java:1367-1380` 的 hide=false 在 queue 外且位于 exact `callWith`；`:1382-1436` 的 hide=true 仅一次 `submitRemoteExclusiveAndWaitDetailed`，callback `:1396-1400` 直接调 mechanics，无 nested queue。completed/stop/unstarted/started-uncertain 分别映 EXECUTED/STOPPED/NOT_EXECUTED/UNKNOWN（`:1410-1436`）；Cloud `CloudDialogDetectionPort.java:48-52,:72-76` 仅 EXECUTED typed result重建、NOT_EXECUTED/non-CAPTURED 映 none，其余 fail-closed且零重发。
- **P0 candidate：** 已读范围未见可直接证成的 P0；双侧 enum、sealed variant、20-key、digest byte exclusion 与 frame SHA 链条静态对齐。
- **P1 candidate - 等待期间 binding 可陈旧：** handler 在 `LocalRemoteGameCommandHandler.java:1364` 固定 `access.binding()`，随后 hide=false mechanics 内先睡眠（`DialogDetectionLocalMechanics.java:104-108`），hide=true 还可能先排队（handler `:1392-1404`）；两路最终均使用该旧 binding 捕获（mechanics `:96-100,:132-145`），且 safety supplier `handler:1403` 未带 geometry snapshot、执行后也无 geometry fence。696 则先 wait（baseline `DialogService.java:1562-1565`）再由 `captureDialogSnapshot -> getDialogRect` 刷新窗口位置；父级需重点判定窗口在 wait/排队中移动时是否会形成错 ROI/错 rect 的 exact-binding 基线漂移。
- **P1 candidate - local endpoint 可达性：** `LocalRemoteGameCommandHandler.java:64,:110-168` 不是 Spring component且新增 mechanics 构造参数；main source 全量符号检索无该 handler 构造/bean site，`RemoteCommandPollingLoop.java:29-42` 仅接收抽象 handler。B 日志 `:7872-7874` 所称“构造参数导致当前模块无法编译”缺少调用点证据，但完整链在 active runtime 是否存在 owner wiring 仍无静态可达证明，需父级区分 dormant owner gate 与本单闭环要求。
- **P2 candidate - nullable source 被绑定为必填 actionSlot：** command 明确允许 nullable source（`DialogDetectionMacroCommand.java:17-25`），但 `DialogService.java:1566-1567` 同时把 `reason` 作为 actionSlot；`CloudTaskRetainedActionState.java:492-506` 要求 actionSlot 非空且无首尾空白。696 public overload 未拒绝 null/空白并由 `safeDebugName` 收敛；当前已见 caller 均传非空字符串，但 public/API 边缘行为不再等价。
- **P2 candidate - 失败解码后的 image 释放：** `CloudDialogDetectionPort.java:82-86` 先 decode 得到 owned image，再用 `require` 核尺寸；尺寸不一致抛出时没有 finally flush。正常成功路径把 image 所有权交给既有 `DialogDetection` caller，696 caller 图与 NONE/fallback 顺序未改；异常帧路径存在单次 image 泄漏候选。
- **696 限定差异：** 非 Git `fc /n` 只见 Cloud `DialogService.java` 新 port import/field及共享 detection implementation `:1560-1578` 替换；`handleDialog:131-221`、三个 public type overload `:1508-1535`、snapshot overload `:1546-1549`、随机默认 wait `:1552-1557`、maintenance direct prefilter `:667-697` 和其余 none/fallback caller 保持。classification 常量与 `wait -> hide -> capture -> mask -> option -> story` 顺序见 mechanics `:41-60,:104-125,:141-177,:192-254`。
- **边界：** 以上仅为非绑定静态预检候选与证据，不含审批或阻断裁决；未运行 build/test/runtime/Git，未修改任何 Java、B 日志或主文档。

### PRECHECK - A NPC Ctrl Probe Local Mechanics R2 - 2026-07-14T18:14:53-04:00

- **材料/写集：** A 日志真实 EOF=`:5945`，领取=`:5887-5889`、Repair #2=`:5891-5945`；唯一声明 Java 为 `NpcClickCtrlProbeLocalMacroMechanics.java`，当前 SHA-256=`7a4b7fe759bfb7719db586dc403efff7d9d1b46500cfcd33bba8443c84916d2c`，与交付 `:5893` 一致。窄范围未见可归因写集漂移；当前 OCR/capture/local-mechanics 调用符号均能在现有源码找到，未发现 P0 编译/API candidate（未运行构建）。
- **P1 candidate - non-empty template 被 generic OPTION 绕过：** 当前 `NpcClickCtrlProbeLocalMacroMechanics.java:382-389` 无条件以 `dialogType==OPTION || raw-frame template match` 判成功。696 在模板非空时由 `DialogHandleRequest.java:255-265` 选择 `VERIFY_GREEN_TEMPLATE`，再由 `DialogService.java:195-207,403-446` 强制 washed image 首命中；generic `OPTION_VISIBLE` 只属于空模板的 `VERIFY_OPTION`。因此当前模板非空仍可仅凭 OPTION 成功，且 `:397-438` raw-frame match 不能恢复该 mandatory gate，存在未批准 false-positive/业务分支漂移候选。
- **P1 candidate - yellow wash 漏清理阶段：** 696 Ctrl OCR 在 `NpcClickService.java:524-533` 调 `ImagePreprocessor.washYellowText`；现有同源实现 `ImagePreprocessor.java:681-699,819-870` 在 RGB predicate 后必经 `cleanYellowTextMask`，去水平线并过滤小/扁连通域。A 当前 `NpcClickCtrlProbeLocalMacroMechanics.java:483-506` 仅逐像素阈值并直接输出黑字白底，没有该 cleanup，故 OCR 输入视觉算法并非 baseline-equivalent；A 日志 `:5896/:5910` 的“exact”声明证据不足。
- **已核关键顺序：** before capture/hold/80/move/280/after/change/OCR=`:210-245,:255-309`，Ctrl release 仍在唯一 `finally :246-249`，点击后 terminal 保留 `clickProduced=true` 于 `:321-332`；上述不抵消两项视觉 candidate。材料仅供父级最终判断，不作批准或阻断裁决。

### PRECHECK - C Cloud Memory Whole Storage Chain - 2026-07-14T18:14:53-04:00

- **材料/写集：** C 日志真实 EOF=`:5200`，领取=`:5153-5155`、Implementation #1=`:5157-5200`；声明三 memory Service + `CloudServiceConfiguration.java`，交付说明实际只改配置。当前配置 SHA-256=`4834db526629097c8d1e674988493c256c7da418c56cccbc6d658c15ff32cb09`，与 `:5160` 一致；三 Service 无源码内容漂移，未见可归因写集越界。
- **bean/path 结论：** `CloudServiceConfiguration.java:20-24` 以 ASSIGNABLE_TYPE 同时排除两个 `@Service`，`:36-50` 仅提供两个对应 Service bean，并分别以内联 `storage.resolvePrivateFile(...)` 注入 `dialog_choice_memory.json`/`world_map_route_result_memory.json`（`:27-28`）；全 Cloud main source 未见第二处实例化。`CloudServiceStorage.java:36-40,52-80,83-88` 将 tenant/user 长度前缀哈希为 scope，并拒绝非单文件名/越界路径，故 reviewed wiring 为 unique scoped stores，不回落共享 `config/*.json`。
- **P2 candidate - “Path bean”措辞需父级确认：** 当前没有 `Path` 返回类型的 Spring bean；实际是两个 Service 返回类型的显式 bean，在构造表达式内取得 scoped `Path`（配置 `:36-50`）。若合同要求字面上的两个 named `Path` beans，则实现形态不符；若要求“两个 store 由 scoped Path 显式构造”，当前形态满足且避免 `Path` 注入歧义。
- **byte-exact 结论：** `WorldMapRouteResultMemoryService.java` 与 `MemoryService.java` 当前/696 raw bytes、SHA-256 均相同；`DialogChoiceMemoryService.java` 仅当前 LF 对 baseline CRLF（14034/14362 bytes），`fc /N` 无源码行差异且 LF-normalized SHA-256 同为 `7da7e1e35b9f5bbc0aec3354534c3e7607cd70e62ed3989ac2cc06c188dec40c`。因此三者内容/blob-normalized 等价，但 `DialogChoice` 不宜表述为 literal filesystem-byte identical；未见 P0/P1 behavior candidate。材料仅供父级最终判断，不作批准或阻断裁决。

### PRECHECK - B Dialog Detection Closed Macro Chain R1 Repair #1 - 2026-07-14T18:27:05-04:00

- **真实 EOF/写集：** B 日志真实 EOF=`:8035`；领取=`:7971-7975`、Implementation Repair #1=`:7977-8012`，其后已有父级独立静态记录=`:8014-8035`。18:09-18:15 Java mtime 窗仅见声明的 `CloudDialogDetectionPort.java`、Cloud `DialogService.java`、DHXY `DialogDetectionLocalMechanics.java`，未见可归因 write-set drift；当前 SHA-256 分别为 `410421da...`、`355332a5...`、`02def520...`。
- **P1-1/P1-2 + API：** `CloudDialogDetectionPort.java:42-62` 将 retained identity 固定为 `dialog/snapshot`，nullable `source` 只进 command；public port 已收敛为三参数，Cloud main source 恰有 `DialogService.java:671-672,:1580` 两个调用点。该文件已无 `TaskCheckpoint` import/call，`NOT_EXECUTED -> none`、其余非 executed terminal fail path 在 `:47-55`；现有 `TaskExecutionContextHolder.current()` 与 `executeLocalMacro(...)` 签名可达，未发现新增 P0 编译/API candidate（未运行构建）。
- **P1-3 stop/finally：** `DialogService.java:1569-1593` 为 baseline `none()` 初始化 + 单次 port 调用 + `finally` result/latency 日志；port/完整性异常仍经 finally，且没有 capture 后新增 checkpoint 丢弃结果。mechanics pre-wait false 独立返回 `PRE_CAPTURE_INTERRUPTED`（`DialogDetectionLocalMechanics.java:108-112`），Alt+4 settle 继续保持 baseline 忽略 sleep false（`:114-129`）。
- **P1-4 live geometry：** 同一 mechanics 在 pre-wait 与可选 Alt+4 settle 完成后、rect 计算和唯一 capture 前调用现有 `refreshGeometry(binding)`（`:131-161`）；empty -> `CAPTURE_UNAVAILABLE`（`:137-140`）。`WindowNativeBindingRefreshService.refreshGeometry` 的真实签名为 `Optional<WindowNativeBinding>`，constructor 注入符号匹配；未见 retry/wrapper 或旧 origin 参与 capture。
- **P1-5 maintenance caller：** `DialogService.java:667-705` 仅调用一次 fixed-slot port，只有 `OPTION` 进入 template 匹配；heal-pet -> repair-equipment 顺序、阈值、结果字符串保持于 `:690-700`，STORY/NONE 直接 miss。旧 Cloud capture/classify helpers 仍为无 caller private code，不改变 active caller 路径。
- **P2-1/image ownership：** DHXY captured frame 在编码后由 `DialogDetectionLocalMechanics.java:171-200` finally flush；Cloud port 在 SHA 校验后单次 decode，只有成功构造后才 handoff，所有 decode 后异常由 `CloudDialogDetectionPort.java:78-106` flush；maintenance handoff image 在 `DialogService.java:701-704` finally flush。既有 handler owner/runtime 可达性仍是 integration pending，本轮三文件未越界伪造 owner。
- **candidate 汇总：** 对 P1-1..P1-5/P2-1 的当前三文件窄核未发现超出父级已见材料的新 P0/P1/P2 candidate；以上仅为非绑定静态预检证据，不作批准或阻断裁决。未运行 build/test/runtime/Git，未修改 Java、B 日志或主文档。

### PRECHECK - A NPC Ctrl Probe Local Mechanics R3 Repair #3 - 2026-07-14T18:45:33-04:00

- **真实 EOF/写集：** A 固定日志真实 EOF=`:6055`，R3 任务=`:5985-5995`、领取=`:5997-5999`、Repair #3=`:6001-6055`；声明并实际落地仅 `NpcClickCtrlProbeLocalMacroMechanics.java` 与 `ImagePreprocessor.java`。当前 SHA-256 分别为 `7e6b2af1...`、`b8a1dc04...`，与日志一致；18:23-18:29 窄 mtime 窗未见第三个 Java。重建 `ImagePreprocessor.java:1-339` 的交付前 blob SHA-1=`0672cf0c...`，与 B 在途基底一致，未见覆盖其 add-back 内容。
- **OPTION/green-template gate：** mechanics `:375-393` 先要求 typed dialog 为 `OPTION`；模板空时才于 `:383-384` 直接 VERIFIED，模板非空则于 `:386-388` 必须进入 ordered matcher。该分流对应 696 `DialogHandleRequest.java:255-265` 的 `VERIFY_OPTION`/`VERIFY_GREEN_TEMPLATE`；此前 generic OPTION 绕过候选在当前 R3 路径中不再存在。
- **washed-image mandatory match：** `NpcClickCtrlProbeLocalMacroMechanics.java:401-450` 解码后调用 `ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite`（`:416`），按输入列表顺序逐模板、阈值 `0.85`（`:421-438`），并在 finally 释放 frame/washed/template；实现与 696 `DialogService.java:403-446` 的 wash、首命中和阈值语义相符。
- **yellow wash/OpenCV exact copy：** Ctrl OCR capture 后于 mechanics `:256-272` 调 `ImagePreprocessor.washYellowText`；新增实现 `ImagePreprocessor.java:349-485` 保持白底黑字 mask、`35x1 MORPH_OPEN`（`:435-440`）、connected-components 条件（`:445-460`）及全部 Mat release（`:476-484`）。抽取的 9 个新增方法与 696-lifted Cloud 同名方法块逐字一致，未再保留手写逐像素近似。
- **重复方法/依赖/API：** 新增 9 个签名在当前类中均仅出现一次；OpenCV imports=`ImagePreprocessor.java:4-9`，`org.openpnp:opencv` 依赖已存在于 DHXY `pom.xml:51-52`。mechanics 的 capture/OCR/input collaborators 均有现存签名，未发现新的 P0 编译/API candidate（未运行构建）。
- **candidate 汇总：** 本次限定 R3 diff 未发现新的 P0/P1/P2 candidate；Ctrl release 唯一 finally、capture/OCR/verify 顺序及 `clickProduced` terminal 映射仍位于 mechanics `:240-324`。以上为非绑定预检材料，仅供父级最终复核。

### PRECHECK - D Task Maintenance Summon Whole Pass Chain Implementation #1 - 2026-07-14T18:45:33-04:00

- **真实 EOF/零 Java：** D 固定日志真实 EOF=`:5670`，任务=`:5594-5629`、领取=`:5632`、Implementation #1=`:5634-5670`；声明 Java 为 Cloud `TaskMaintenanceService.java`、`SummonSkillService.java`，交付称零修改。两文件 mtime/SHA 未随本单变化；非 Git 行级对照显示前者与 696 相同，后者仅保留既有 `CloudUiCleanerPort` 在途替换，故本单未见 write-set drift。
- **P1 candidate - whole-pass 仍未交付：** active 链 `AutoBattleTask.java:208 -> TaskMaintenanceService.java:579-756 -> SummonSkillService.java:166-186` 仍在后者以本地 `submitExclusiveAndWait` 执行 mechanics；`CloudSummonSkillWholePassCapability.summonSkillWholePass()` 在 Cloud main source 仍无业务消费者。故 D 所述 capability/context reachability 缺口真实，零 Java 交付没有完成本单 ownership migration。
- **P2 candidate - “声明写集内不可实施”证据不成立：** exact `TaskExecutionContext` 已由 `runOpportunisticMaintenance(context)` 穿到 `maybeCleanSummonSkill(context)`（`TaskMaintenanceService.java:579-594,625-756`），且 `TaskExecutionContext.getRemoteGameClient()` 与 `CloudTaskServicePort.summonSkillWholePass()` 均已存在。无需 holder 或第三文件：最小穿线只需 `TaskMaintenanceService.java:756` 传现有 context，并在 `SummonSkillService.java:166-186` 增加/改用 context-aware whole-pass 路径，同时保留现有 public overload。
- **terminal 最小映射：** `Executed` 应将 capability 的 9-field value 完整映射为 `SummonSkillCleanupResult`；`NotExecuted` 才可映射普通 failed/miss。`Stopped` 应以 `TaskCheckpoint.throwIfStopRequested(context, ...)` 确认后终止，`Unknown` 不得降为 failed（否则会触发 `TaskMaintenanceService.java:773-780,907-916` 的 backoff/retry 语义），`InterruptedException` 应恢复 interrupt 并转 fatal/unresolved 路径；现有同类范式见 `CommonBoxService.java:152-163,293-300`。
- **基线/finally 锚：** 696/current `TaskMaintenanceService.java:744-797` 的 checkpoint、`INTERACTING` state、success timestamp、ultimate cooldown、unknown backoff 与 finally 恢复 ActionState 均仍原样；后续实现应只替换 `:756` 的执行 ownership，异常 terminal 仍需经过 `:762-785` finally，且不得让 `Unknown` 形成第二次 resend。
- **candidate 汇总：** 未发现本单引入的 P0 当前编译错误，因为 Java 实际未改；当前主要是 P1 active-chain 未接 capability，以及 P2 blocker 论证与现有显式-context 方法图不符。以上为非绑定预检材料，仅供父级最终复核；未运行 build/test/runtime，未修改源码或 D 日志。

### ARMED - PlayerState FirstAid / Summon Whole-Pass Wave - 2026-07-14T18:55:22-04:00

- **authority/status 快照：** 已读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md` 与当前 333 行 whole-service plan（mtime `18:25:50`）；两仓只读 status 为 DHXY `314` 项（`M=34,D=1,??=279`）、Cloud `461` 项（`M=7,??=454`）。相关 Service/contract 路径已处于共享 dirty/untracked 集，领取后的 Implementation/Repair 尚未落日志，故本 helper 不把这些状态归因成任何 Worker 的实际写集或交付漂移。
- **A：** 固定日志真实 EOF=`:6074`（mtime `18:48:38`）；最新材料仍是 R3 Implementation `:6001-6055` 及父级后续记录 `:6057-6074`，A 写集已释放。EOF 尚无父级新 cohort、无新 CLAIMED/Implementation/Repair；状态仅为等待新单，不计领取延迟或交付停滞。
- **B：** 固定日志真实 EOF=`:8091`（mtime `18:50:54`）；`W-696-PLAYERSTATE-FIRST-AID-DHXY-WHOLE-CHAIN-1` 已于 `:8087-8091` 真实领取（claimedAt `18:49:40`，九 Java + 本日志），EOF 尚无 Implementation/Repair。保留 claimed/in-progress 状态，不提前核当前源码或判写集。
- **C：** 固定日志真实 EOF=`:5267`（mtime `18:49:44`）；`W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1` 已于 `:5265-5267` 真实领取（claimedAt `18:48:00`，十 Java + 本日志），EOF 尚无 Implementation/Repair。保留 claimed/in-progress 状态，不提前核当前源码或判写集。
- **D：** 固定日志真实 EOF=`:5703`（mtime `18:53:29`）；相较调度消息已有更新，`W-696-TASK-MAINTENANCE-SUMMON-WHOLE-PASS-CHAIN-1-R1` 已于 `:5703` 真实领取（claimedAt `18:53:29`，两 Cloud Service + 本日志），不再属于“等待领取”；EOF 尚无 Implementation Repair。
- **触发锚：** 后续仅当 B/C/D 上述 claim 后，或 A 后续新 claim 后，于对应固定日志真实 EOF 出现新的 Implementation/Repair，才读取其声明写集源码并做编译符号、696 分支、terminal 与资源 ownership 非绑定预检。本条只记录时序事实，不作源码风险结论；未运行 build/test/runtime，未修改源码、外部日志或主文档，未作 Git mutation。

### PRECHECK - D Task Maintenance Summon Whole-Pass R1 Repair #1 - 2026-07-14T19:02:02-04:00

- **真实 EOF/写集：** D 固定日志真实 EOF=`:5757`（mtime `18:56:49`），R1 领取=`:5703`、Implementation Repair #1=`:5705-5757`。可归因 Java 仍限 Cloud `TaskMaintenanceService.java` 与 `SummonSkillService.java`：前者 1123 行、SHA-256=`4beaffd0...`，与 migration-baseline 逐字相同且 mtime 未动；后者 1108 行、SHA-256=`2ee437f1...`，non-Git baseline 对照仅见既有两处 UI-clean substitution 与本轮 imports/holder field/non-worker branch/三个 private mapping 方法，未见 D 写集漂移。
- **单例 context/API：** `TaskExecutionContextHolder.java:14-35` 是唯一 `@Component`、以同一 ThreadLocal 暴露 `callWith/current`；Cloud main source 未见第二个 bean、手工 `new TaskExecutionContextHolder` 或 `new SummonSkillService`。新增 constructor field=`SummonSkillService.java:52`，`current().orElseThrow`=`:194-197`，相关 `TaskExecutionContext`、`TaskFatalException(String,Throwable)`、capability `execute(...)` 符号均真实存在，限定静态核未发现 P0 编译/API candidate（未运行构建）。
- **完整 active caller：** `AutoBattleTask.java:111-114` 先 resolve exact context 再以唯一 holder `callWith` 包住整个 patrol；idle caller `:182-208` 将同一 context 传入 `TaskMaintenanceService.runOpportunisticMaintenance`（`:579-594`），最终保持 baseline 调用 `SummonSkillService.cleanSummonSkillsOnce(request)`（`:744-756`）。non-input-worker 分支才于 `SummonSkillService.java:172-206` 取 current context 并恰调用一次 `context.getRemoteGameClient().summonSkillWholePass().execute(intent)`；全 Cloud main source没有其它 `cleanSummonSkillsOnce*` caller。
- **四态/interrupt：** `Executed -> toCleanupResult`（Summon `:211-213`），`NotExecuted -> failed(message)`（`:214-216`）；`Stopped` 与 `Unknown` 分别于 `:217-224` 抛 `TaskFatalException`，不落成普通失败；`InterruptedException` 于 `:205-210` 恢复 interrupt flag 后 fatal unwind。只有前两态能继续原顺序的 finished log -> `cleanLightweightInterruptions` -> return（`:177-182`），无第二次 whole-pass 调用或新增 retry/backoff/TTL。
- **9 字段/5 枚举：** intent 四字段于 Summon `:198-203` 一一取自 request；cleanup 9 字段于 `:233-247` 全量映射，slot map 新建 `LinkedHashMap` 并按 capability 已冻结的 insertion order 遍历。`toSlotStatus :250-257` 穷举 `NORMAL_SKILL/KEEP_SKILL/EMPTY_SLOT/LOCKED_SLOT/UNKNOWN`，无 default、无漏枚举；上游 capability constructor 已拒绝 null value/负 key 并保留 LinkedHashMap 顺序（`CloudSummonSkillWholePassCapability.java:92-117`）。
- **baseline finally/state/order：** `TaskMaintenanceService.java` 与 696 byte-equivalent；`:744-785` 仍是 checkpoint -> 保存 state -> `INTERACTING` -> call -> finally success timestamp/cache、ultimate-before-failure、unknown-backoff、恢复 previous state。Stopped/Unknown/interrupt 抛出时 `cleanupResult` 保持 `:748` 的普通“not attempted”默认值，finally 仅恢复 state，随后 `:787-797` 的 claim release/failed return 不可达，符合本 R1 terminal 约束；Executed/NotExecuted 则继续原 cache/cooldown/order。
- **资源/candidate 汇总：** R1 未新增 image/path/native ownership；retained/exclusive occurrence 仍由现有 capability/authority 冻结，Service 只消费 closed result。限定交付未发现新的 P0/P1/P2 candidate；剩余风险仅是父级统一 compile/runtime integration gate尚未执行，本材料不替代该门。以上仅为非绑定静态预检，未修改 Java、D 日志或主文档，未运行 build/test/runtime/Git。

### ARMED CONTINUATION - A/B/C after D R1 - 2026-07-14T19:03:00-04:00

- D 落档后再次读取真实 EOF：A=`:6074`、B=`:8091`、C=`:5267`，三者均与上一 ARMED 快照一致。A 尚无父级新 task/claim；B/C 分别仍停在 PlayerState FirstAid DHXY/Cloud whole-chain 的真实 CLAIMED，尚无 Implementation/Repair。本 helper 保持 armed，不把领取后的编辑中状态当成交付，也不提前预检源码。

### PRECHECK - B PlayerState FirstAid DHXY Whole Chain Implementation #1 - 2026-07-14T19:24:33-04:00

- **材料/实际写集：** B 固定日志真实 EOF=`:8130`（Implementation #1=`:8093-8130`）。`18:49-19:09` 窄 mtime 窗内 Java 改动恰为声明的 9 文件：两个 PlayerState payload、两个 sealed payload、`RemoteLocalMacroKind`、codec、digest、handler、`PlayerStateFirstAidLocalMacroMechanics`；未见第十个 Java 或声明外写集 candidate。
- **P0 candidate：none（限定静态符号核）。** 三 operation、result 7-key、mechanics 四组 enum 均能解析到现存类型；`@Jacksonized` + constructor `@Builder` 形态与既有 BAG payload 相同。未运行 compile，故这里只记录未发现静态缺符号/API candidate。
- **P2 candidate - command 接受域不完全对称：** DHXY `RemotePlayerStateFirstAidMacroCommandPayload.java:57-65` 以 `hasTargets` 判断，因而 PROBE/HEAL 接受 `targets=[]`；Cloud `PlayerStateFirstAidMacroCommand.java:42-46` 要求这两态 `targets == null`。当前 C 正常 producer 不会产生该值，但 DHXY decoder 的合同接受面比 C constructor 宽。
- **P2 candidate - result name 接受域不完全对称：** DHXY `RemotePlayerStateFirstAidMacroResultPayload.java:100-118,127-157` 仅拒绝 null name；Cloud `PlayerStateFirstAidMacroResult.java:89-94,101-118` 同时拒绝 blank。当前 handler 使用固定非空名称，未形成已知 active-path mismatch；父级可复核是否要求 constructor acceptance-set 也严格 parity。
- **合同/摘要关键不变量：** B kind/permits 六类完整（`RemoteLocalMacroKind.java:7-13`；两个 sealed payload `:4-10`），三 operation、四组 enum 与 C 对应定义逐项一致。B digest request/result 子树分别在 `RemoteProtocolDigests.java:149-186,316-371`，与 C `NON_NULL valueToTree`（Cloud `RemoteProtocolDigests.java:26-36,96-114`）对 contract-valid payload canonical 等价；`frame`/image bytes 不在本合同，未见新增 codec/digest candidate。
- **P1 candidate - HEAL 排队后的 geometry 时效：** handler 在入队前固定 `binding`（`LocalRemoteGameCommandHandler.java:1509`），HEAL exclusive callback 于 `:1532-1551` 仍把该对象交给 mechanics；worker safety supplier 的 geometry 参数为 null（`:1550-1551`，实际 fence `:3041-3068` 仅在非 null 时核 geometry）。`PlayerStateFirstAidLocalMacroMechanics.java:142-153,213-255,393-425` 在 capture/click 前未 live refresh；若排队期间窗口移动，可能使用旧坐标。对照 cached operation 自身于 `:181-191` refresh，此差异需父级重点复核。
- **queue/terminal 不变量：** PROBE 仅在 exact context 的 queue 外 `callWith`（handler `:1511-1525`）；HEAL/CACHED 各仅一次 remote exclusive callback（`:1527-1551`），callback 内为 direct mechanics、未见 nested queue。completed/stop/unstarted/started-uncertain 分别映射 EXECUTED/STOPPED/NOT_EXECUTED/UNKNOWN（`:1553-1584`），interrupt/stop 不降为普通业务 miss。
- **696 cached-plan 不变量：** baseline `PlayerStateService.java:307-375` 的 stored-base -> live geometry/fallback -> mouse-away -> sleep 300 -> ordered right-click 100 + sleep 800 -> interrupted result，在 mechanics `:169-211` 保持同序同常量（常量 `:69-76`）；未发现额外 retry/read/TTL 或 fallback 顺序 candidate。任务文字中的 200 与 696 实码 300 不同，本交付采用 696 的 300。
- **既有 cohort 不变量：** codec local-macro switch 保留 NAV/UI_CLEAN/DIALOG/PLAYER/BAG 路径（`RemoteOperationPayloadCodec.java:369-390,564-585,684-705`）；handler dispatch 保留 NAV/UI_CLEAN/DIALOG 后再接 PLAYER、BAG 仍在后续（`LocalRemoteGameCommandHandler.java:1131-1149`）；digest 新分支在既有 DIALOG 分支后追加。限定静态核未发现 BAG/NAV/UI_CLEAN/DIALOG 行为回归 candidate。
- **P1 candidate - owner/integration gate：** `LocalRemoteGameCommandHandler.java:65,112-173` 仍非 Spring bean，main source 未见其 construction/wiring；`RemoteCommandPollingLoop.java:17,31` 仅依赖 `RemoteCommandHandler` 接口，故本路径及既有 local-macro cohort 仍受 owner 接线门约束。另 C 日志当前 EOF=`:5326` 明示 Cloud whole-chain 为 9/10 partial、active `PlayerStateService` caller 尚未交付；这是整链前置事实，不归为 B 写集漂移。
- **非绑定结语：** 本次 candidate 为 P1 两项（HEAL queue 后坐标时效、owner 接线门）与 P2 两项（constructor 接受域 parity）；P0 none。以上仅供父级最终审查复核，未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未作 Git mutation。

### PRECHECK - A Dialog Story Objective Capture Whole Mechanics Implementation #1 - 2026-07-14T19:29:29-04:00

- **材料/唯一写集：** A 固定日志真实 EOF=`:6189`（task=`:6076-6128`、claim=`:6130-6132`、Implementation #1=`:6134-6189`）。`19:11-19:20` DHXY Java mtime 窄窗仅见 create-new `service/dialog/DialogStoryObjectiveCaptureLocalMechanics.java`（396 行，mtime `19:17:43`，SHA-256=`0b2d0a...f65f0`，与交付声明一致）；未见声明外 Java candidate。
- **P0 candidate：none（限定静态 API 核）。** constructor 的三个依赖均为现存 Spring bean；`detectDialog`、`captureRegion`、`cropAbsoluteRect`、`cropCopy`、`saveImage`、`WindowScopedTempPath.resolve` 的当前签名与调用形状相符。未运行 compile，故不把静态核替代编译门。
- **P1 candidate - op1 fresh/old geometry 混用：** `DialogDetectionLocalMechanics.java:131-155` 会 refresh exact HWND 并把 fresh large rect写入 result；新类却在 `DialogStoryObjectiveCaptureLocalMechanics.java:172-180` 用 detection 的 fresh large rect、同时用入口旧 `binding` 计算 small rect（`:349-359`）。窗口位置若已变化，`cropAbsoluteRect` 会按交集裁剪而非要求完整包含（`ImagePreprocessor.java:66-74`），可能生成偏移/缩水图仍由 `:184,300-325` 报 `CAPTURED`，且 absoluteLeft/Top 也来自旧坐标。
- **P1 candidate - 丢失 696 crop fallback 信息：** 696 `DialogService.java:1472-1478` 在 small crop 失败时返回原 detection image，`:1455-1463` 仍保存 debug 并用原帧做 objective OCR；新 op1 于 `DialogStoryObjectiveCaptureLocalMechanics.java:180-183` 直接闭合为 `CROP_FAILED`，结果又禁止携带 frame bytes（`:104-130`）。未来 caller 无法在不二次 capture 的前提下复现 baseline 原帧 fallback，需父级复核是否为未授权行为差异。
- **P2 candidate - terminal 精度：** op1 入口只检查 null/handle（`:145-149`）；无 geometry、窗口容量不足及 refresh 失败均由 detection `:100-105,137-140` 折成 `CAPTURE_UNAVAILABLE`，不会形成 `BINDING_UNAVAILABLE`。此外 op1 固定传 `wait=0/hide=false`（`:150-151`），故其 `PRE_CAPTURE_INTERRUPTED -> INTERRUPTED` 分支（`:192-194`）在当前调用形状下不可达，已中断线程仍可进入 capture；op2/op3 则在 `:211-215,254-255` 显式区分。
- **P2 candidate - PNG 尺寸来源/自校验：** `capturedFrom` 在编码前从 `cropped` 读取 width/height（`:300-309`），仅 SHA 在 PNG bytes 上重算（`:314-321`）；record constructor `:114-131` 只检查 bytes 非空、hash 非 blank、尺寸为正，不复算 hash，也不从 PNG bytes 解码核尺寸。标准 PNG writer 下通常等价，但未严格满足“SHA/尺寸均由实际 bytes 重算”的交付约束。
- **P2 integration gate - debug scope：** latest/history 均经 `WindowScopedTempPath.resolve`（新类 `:328-335`），`safeSource` 在 `:385-390` 去危险字符并 cap 120，文件名本身安全；但 `WindowScopedTempPath.java:30-35` 仅在 current holder 存在时按 windowId 分目录，否则 `:31-32` 回退共享 temp。该 mechanics 只接 binding、不建立 context，未来 wire 必须保证 exact `callWith` 才能兑现 window-scoped 隔离。
- **同帧/capture/几何已核不变量：** op1 只调用 detection，随后解码其 defensive-copy `framePngBytes`，无第二次 capture（新类 `:150-184`；detection 原帧在 `DialogDetectionLocalMechanics.java:171-199` 已编码后释放）；op2 全类唯一直接 `captureRegion` 在 `:222-240`，固定 large `(250,312,529,208)` 再裁 local `(0,33,529,143)`；op3 `:251-290` 仅从 caller PNG 按 window origin 裁 `(250,345,529,143)`，零 capture。除上述 fresh/old 混用外，未见重复 capture 或固定 offset 写错。
- **debug/ownership 已核不变量：** 成功 crop 后 latest + timestamp history 各保存一次（`:320,328-342`），debug 失败不改 terminal。op1 decoded frame `:185-187`、op2 captured image `:241-243`、op3 decoded snapshot `:291-293`、各 operation 的 independent crop `:300-325` 均在 finally 各 flush 一次；返回的是 cloned PNG bytes（`:114-136`），未见 double-flush、借用 image 外泄或本地 Path 进入 result。
- **非绑定结语：** 当前候选为 P1 两项（fresh/old geometry、696 原帧 fallback 丢失）及 P2 三项/一项 integration gate（terminal 精度、PNG bytes 自校验、context-dependent debug scope）；P0 none。以上只供父级最终审查，不构成审批结论；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未作 Git mutation。

### PRECHECK - C PlayerState FirstAid Cloud Whole Chain Implementation #1 (10/10) - 2026-07-14T19:37:52-04:00

- **材料/写集：** C 固定日志真实 EOF=`:5366`；task=`:5224-5263`、claim=`:5265-5267`、9/10 contract delivery=`:5269-5318`、第 10 文件完成=`:5328-5366`。声明的 10 Java 均存在且当前 SHA/行数与交付相符；`18:48-19:27` Cloud Java mtime 窗除这 10 文件外仅有已归属 D R1 的 `SummonSkillService.java@18:55:29`，未见 C 声明外写集 candidate。
- **P0 candidate - touched class 静态不可编译：** `PlayerStateService.java:13-30,72-84` 仍 import/inject Cloud source 不存在的 `GameClientTracker`、`TextRecognizer`、`InputProvider`、`CoordinateHelper`、`LocationVisionService`、`WindowScopedTempPath`、`WindowTaskContextHolder`、`BagService` 等；Cloud `pom.xml:27-74` 也无 DHXY project artifact dependency。交付日志已自报此事实，但它意味着 10/10 文件当前不能通过 Java compile；新增 remote/port 符号自身未见独立缺符号 candidate（按禁令未运行构建）。
- **P1 candidate - active startup caller 仍被 frozen tracker gate 截断：** `AutoBattleTask.java:111-135` 在 exact holder context 内调用 `performStartupFirstAidCheck`；该方法经 `PlayerStateService.java:233-240` 进入 active `performFirstAidCheck`，但 port 前仍执行缺失 desktop `tracker.getWindowBaseX()` 并可直接 return（`:441-460`，尤其 `:448`）。这不是 dormant-only 引用；即使未来补齐类型，Cloud active startup heal 仍可能在 first-aid macro 前被旧 tracker gate 跳过。
- **P1 candidate - GEOMETRY stop/terminal 被降为普通 UNKNOWN：** `CloudGameClient.java:40-53` 会原样返回 OBSERVED/NOT_EXECUTED/STOPPED/UNKNOWN fact outcome；`PlayerStateService.readWindowBase` 却只接受 OBSERVED，其他状态统一 null，并在 `InterruptedException` 时仅恢复 flag 后返回 null（`:520-536`）。上层随即清 `pendingNoFocusFirstAidPlan` 并返回普通 `UNKNOWN`（`:283-287`），因此 STOPPED/UNKNOWN/interruption 在 first-aid port 之前被吞成业务 miss并发生 state mutation；对照现有 typed-fact 范式 `BattleRadarService.java:481-493` 会对 interrupt/terminal fatal unwind。
- **P1 candidate - 696 geometry/capture 顺序与 occurrence 绑定改变：** 696 `PlayerStateService.java:267-280` 是 base-X gate -> bars capture -> 再读取同一 tracker base；当前 `:283-299` 先单独读 GEOMETRY fact、再发 PROBE macro capture，两个 retained action/观察时点无同一 occurrence 约束。窗口在两次远程动作间移动时，pending plan 可把旧 base 与后一次 sample coordinates 组合；B cached mechanics 的 live refresh（DHXY `PlayerStateFirstAidLocalMacroMechanics.java:179-195`）仅在 refresh 成功时缓解，失败时会回退该 stored base。
- **P1 candidate - baseline `-1` base gate 未保留：** GEOMETRY fact 允许任意 screen-absolute x/y（Cloud `WindowFact.java:29-41`）；`readWindowBase` 对 `x==-1` 不作 baseline gate，仍进入 PROBE。696 于 `:267-270` 会立即清 plan并返回 UNKNOWN；当前可能把无 target 结果判 HEALTHY，随后 `probeAndConsumeHealthyFirstAidNoFocus` 于 `PlayerStateService.java:254-263` 增加检查计数，或先建立一个稍后才被 `:337-340` 拒绝的 plan。
- **P2 candidate - explicit context 与 holder authority 未做同一性校验：** GEOMETRY 使用方法参数 `taskContext`（`:520-526`），三个 macro port 调用却由 `CloudPlayerStateFirstAidPort.java:76-84` 重新取 `TaskExecutionContextHolder.current()`；public probe/cached/heal(context) 没有校验两者 taskRun/runRevision 一致。当前 `AutoBattleTask.java:111-114` 的 active 链确实以同一 context `callWith`，所以这是边界风险而非已证 active mismatch。
- **P2 cross-contract candidate（承接 B 现状）：** C command 对 toggle operation 强制 `targets==null`（`PlayerStateFirstAidMacroCommand.java:42-46`），B constructor 仍接受 `targets=[]`（DHXY payload `:57-65`）；C observation/outcome name 拒 blank（`PlayerStateFirstAidMacroResult.java:89-118`），B 仅拒 null（DHXY result payload `:100-118,127-157`）。有效 producer/handler 常量路径一致，但两侧 constructor acceptance-set 尚非完全 parity。
- **contract/digest 已核不变量：** kind/permits 六类闭合（`LocalMacroKind.java:4-10`、`LocalMacroCommand.java:4-6`）；三 operation、四 toggle、cached base+ordered targets及 probe/heal/cached enum/字段与 B 有效形状一致（command `:19-72`、result `:19-118`）。Envelope 以专用 7-key exact set decode并 strip `macroKind`（`RemoteCommandOutcomeEnvelope.java:87-93,262-267,358-366`）；request/outcome digest 的 NON_NULL nested tree 与 B manual canonical tree对有效 payload 等价（`RemoteProtocolDigests.java:26-36,55-60,96-114`），未见新增 digest/API candidate。
- **caller/state/terminal 已核不变量：** AutoCombat 的三处 probe/cached caller仍在 `AutoCombatService.java:380-406,461-470,569-588`，startup caller仍在 AutoBattle `:135`。probe 的 MAX gate、pending set/clear、target order与 threshold normalize保留（PlayerState `:275-320,539-582`）；cached 仍先 consume plan、COMPLETED/INTERRUPTED/empty 后均 count++/return true（`:329-362`）；heal(context) 前后 checkpoint在 `:504-508`。port 对 EXECUTED 校 operation、NOT_EXECUTED empty、其余 terminal fatal且零 retry（port `:76-104`）。除上列 geometry/stop/startup候选外，未见 state 顺序新增漂移。
- **frozen/既有路径影响：** incense/identity/position及 dormant heal helpers仍在原类，未见本轮主动改写；但其保留的缺失 desktop 类型造成上述全类 P0 前置，不能以“frozen”掩盖 compile 影响。LocalMacroRequest/Outcome 的旧 BAG/NAV/UI_CLEAN/DIALOG 互斥分支和兼容 constructors仍在（request `:25-106`、outcome `:27-105`），Envelope 旧 4-key/20-key dispatch仍在 `:255-337`，限定静态核未发现既有 cohort 新回归 candidate。
- **非绑定结语：** 当前候选为 P0 一项（touched class compile）、P1 四项（active tracker gate、fact stop/interrupt 降级、geometry/capture 分离、`-1` gate 丢失）及 P2 两项（context 同一性、cross-contract 接受域）。以上只供父级最终审查，不构成审批结论；未修改任何源码/External 日志/主文档，未运行 build/test/runtime，也未作 Git mutation。

### PRECHECK - D Dialog Green Template Option Whole Mechanics Implementation #1 - 2026-07-14T19:40:35-04:00

- **材料/唯一写集：** D 固定日志真实 EOF=`:5963`；task=`:5781-5841`、claim=`:5844`、Implementation #1=`:5846-5896`，其后父级已在 `:5898-5962` 发布 R1 条件。D 新文件 `service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java` 为 380 行、mtime `19:26:36`；`19:20-19:36` 窄窗中另三份 `19:35+` Java 均属并行 B PlayerState 写集，未见 D 声明外写集 candidate。
- **P0 candidate：none（限定静态 API 核）。** 四个 constructor 依赖均为现存 bean/type；当前 `washDialogOptionTemplateTextToBlackAndWhite(BufferedImage)`、`ImageFinder.find(BufferedImage,BufferedImage,double)`、`resolveMatchedPointInRect`、四参 randomized point、`clickLeft(int,int,int)`、`captureRegion` 签名与调用相符。按禁令未运行 compile。
- **P1 candidate - prepare supplied-frame 优先级缺失：** 696 `DialogService.java:2178-2213` 的 MATCH_ONLY/prepare caller 可先消费 supplied detection image+rect，存在时零 detect/零 capture；当前 `Command` 只有 operation/verify/specs（新类 `:284-294`），`execute:94-96` 只能 detect 或 capture，无法表达或保留同一 supplied frame。
- **P1 candidate - click verify=true capture 时序改变：** 696 MATCH_AND_CLICK 于 `DialogService.java:2294-2307` 先 detect/type gate，再以 fresh `getDialogRect()` 做一次独立匹配帧 capture；当前两 operation 共用 `observeByDetection`（新类 `:94-104,163-192`），verify=true 直接匹配 detection PNG，省掉 baseline gate 后 capture。选项在两个时点变化时，命中/点击真值会不同。
- **P1 candidate - template unreadable 提前终止 fallback：** 当前任一 template IOException/null 即返回独立 `TEMPLATE_UNAVAILABLE`（`:110-124`）；696 两条 caller-order 循环对 blank/unreadable/miss 都继续后续 spec，仅首个 0.85 hit 短路（baseline `:2222-2235,2319-2342`）。前项坏文件会阻断后项有效模板，且新增 terminal 没有 baseline 业务分支。
- **P1 candidate - public command/spec 又提前拒绝 baseline 可跳项：** 696 循环显式跳过 `spec==null || templatePath==null || blank`（`:2222-2225,2319-2322`）；当前 `Command` 的 `List.copyOf`（新类 `:285-292`）拒绝 null element，`Spec` 又在 `:277-281` 拒绝 null path，均在 loop 前抛出并由 caller侧无法继续后续候选。blank path 本身仍能在 `:111-113` 正确 continue。
- **P1 candidate - capture 使用 stale binding geometry：** verify=false 的 `observeByCapture:196-214` 直接用入口 binding X/Y；696 两条 capture 都经 `getDialogRect`（`:2206-2213,2304-2307`），其 `CoordinateHelper.getScaledRect` 路径会刷新当前窗口。命令或 input 排队期间窗口移动时，新类可能截旧 rect并把 randomized match 映到旧 screen coordinates；未来 verify=true click 恢复第二 capture 时也需在 capture 前 refresh exact HWND。
- **P2 candidate - miss 诊断 pass 丢失：** 当前 0.85 miss 于 `:125-133` 直接 continue；696 每项 miss 后再执行一次 `ImageFinder.find(...,-1.0)` 并记录 best point/score（`:2226-2235,2323-2340`）。该 pass 不改变 first-hit 决策，但缺失会降低现有 template 漂移诊断能力。
- **P2 candidate - public ImageEvidence authority 不闭合：** `ImageEvidence:298-317` 的 public canonical constructor仅检查非空/正数，不解码 bytes、不复算 SHA/尺寸；`of` 又以 pre-encode image width/height 填值（`:309-312`），任意 caller可构造 bytes/hash/dimensions 互相矛盾的 evidence。`Result:324-354` 的 matched/nonmatched 字段互斥与 defensive byte clone本身已闭合。
- **已核不变量：** current loop保持 caller order、blank continue、0.85 唯一业务阈值与 first-hit return（`:110-148`）；MATCH_AND_CLICK 在任何观察前检查 input-worker（`:81-89`），命中后恰一次随机 point计算与 `clickLeft(...,150)`（`:135-146`），MATCH_ONLY 零 input且无 nested queue。verify=false 当前恰一次 capture，verify=true 当前恰一次 detection capture，同一 match frame只 wash一次；这些计数事实成立，但两 operation 的 baseline 时序差异如上。
- **ownership/terminal 已核不变量：** frame、washed 分别在 `:153-160` finally flush，逐 spec template在 `:125-130` finally flush，证据 bytes defensive-copy；未见 double flush或 image外泄。十 terminal与 Result variant field门存在（`:255-267,324-367`），但 `TEMPLATE_UNAVAILABLE` 与 `INTERRUPTED` 的最终业务映射仍需随 R1 caller语义复核。
- **非绑定结语：** 当前候选为 P1 五项（supplied frame、click fresh capture、unreadable fallback、null candidate continuation、fresh geometry）与 P2 两项（best-match diagnostics、ImageEvidence invariant）；P0 none。以上为独立 helper 证据，和日志中已发布的 R1 方向相互印证但不替代父级最终审查；未修改源码/External 日志/主文档，未运行 build/test/runtime，也未作 Git mutation。

### TIMING CORRECTION - C versus B R1 delivery - 2026-07-14T19:41:51-04:00

- C 预检读取 B 后，并行 B 日志又在真实 EOF=`:8249` 追加 R1 Implementation=`:8218-8236` 与后续 scope-amendment claim=`:8238-8249`。已交付 R1 的 DHXY command 现于 `RemotePlayerStateFirstAidMacroCommandPayload.java:63-66` 拒绝 `targets=[]`，result items 于 `RemotePlayerStateFirstAidMacroResultPayload.java:108-109,138-139` 拒绝 blank name；因此上方 C PRECHECK `:631` 的两处 acceptance-set P2 已被后到 B R1 修正，不再计入 C 当前 candidate 数，C 其余 P0/P1及 context P2不变。
- B scope amendment 仅已领取、尚无新的 Implementation/Repair；其拟增加 capture-time `observedBaseX/Y` 与 fixed four-name list invariant，正对应上方 C geometry/capture 分离 candidate。后续只在 B/C 真实 EOF 出现相应新交付时重核双方字段/7-key更新/constructor/canonical parity，本条不预检进行中源码。

### PRECHECK - A Dialog Story Objective Capture Whole Mechanics R1 - 2026-07-14T19:46:37-04:00

- **材料/写集：** A 固定日志真实 EOF=`:6303`；R1 claim=`:6245-6247`、Implementation Repair #1=`:6249-6286`。当前唯一源码 `service/dialog/DialogStoryObjectiveCaptureLocalMechanics.java` 为 435 行、SHA-256=`5b2ce85b52a7feccc576533d2feb5fd7d62f3c4f7c59db7a4bca32d93117b7e9`，与交付声明一致；限定材料内未见源码+本日志之外的 R1 写集漂移 candidate。
- **P0/P1/P2 candidate：none（仅核四项返修及新增 shape）。** fresh rect 在 `:212-226` 全由 detection 的 fresh large origin 推出 `(0,+33,529,143)`，返回坐标同源；op1 不再混入入口 binding 坐标，也没有二次 capture。
- **696 fallback/flush：** 基线 `DialogService.java:1472-1478` 的 crop-null 原帧返回，在当前 `:219-228` 由同一 frame 的 `capturedFrom(...,true,false)`复现；frame 始终由 op1 外层 `finally` flush，正常 cropped image 由 `capturedFrom(...,false,true)` flush（`:224-228,339-367`），未见双 flush 或遗漏。
- **op2/op3 中断门：** 当前 op2 `:251-283`、op3 `:290-330` 已无 `Thread`/checkpoint/入口中断分支，与基线 `:2413-2433,2445-2467` 一致；本类 `INTERRUPTED` 只剩 op1 detection 的 `PRE_CAPTURE_INTERRUPTED` 映射（`:234-235`）。
- **PNG authority invariant：** compact constructor 先 clone bytes、核 CAPTURED/non-CAPTURED 字段形状，再从实际 bytes 解码、核 dimensions、复算 SHA-256并 flush decode image（`:115-169`）；accessor 再 defensive clone（`:171-174`）。
- **`fullFrameFallback` shape：** 非 CAPTURED 携带 `true` 会被拒绝（`:118-136`），所有 terminal factory 固定 `false`（`:432-433`）；当前仅本类四个 CAPTURED factory 使用该字段（`:224,226,279,326,361`），repo scoped search 无类外消费者。`true` 表示保留原 detection frame 的来源语义，故不把尺寸强制为 529x208反而保持 696 对异常原帧也 fallback 的语义；未形成新的具体 risk candidate。
- **非绑定结语：** 本段仅为 helper 静态材料，等待父级最终审查；未修改源码/External 日志/主文档，未运行 build/test/runtime，也未作 Git mutation。

### PRECHECK - B PlayerState FirstAid DHXY R1-S1 Scope Amendment - 2026-07-14T19:59:36-04:00

- **真实 EOF / 实际写集：** B delivery=`:8251-8273`，复读时真实 EOF 已到 `:8302`（父级已发布 R2）。S1 窄 mtime 对应 mechanics `19:41:13`、result payload `19:42:33`、codec `19:42:40`、digest `19:42:47`、handler `19:43:01`；command payload 为前一 R1 的 `19:35:24` 且早于 S1 claim。未见 S1 声明六文件范围外的 B 写集漂移 candidate；既有 handler owner 接入前置仍未改变。
- **跨仓 parity 已核不变量：** B 9-key exact set（`RemoteOperationPayloadCodec.java:67-70`）与 C（`RemoteCommandOutcomeEnvelope.java:90-93`）一致，handler 九字段输出在 `LocalRemoteGameCommandHandler.java:1657-1669`；同帧 base pair、固定四名与三 variant 的 B invariant（result payload `:64-128,147-205`）和 C invariant（Cloud result `:36-97,130-160`）接受域一致。B manual NON_NULL tree（digest `:316-379`）与 C `valueToTree`（Cloud digest `:96-114`）最终都按 key 排序 canonicalize（B `:539-558`、C `:273-292`），未见新增 digest/key-order candidate。
- **P1 candidate - 696 的 capture 前 `baseX==-1` 门仍缺：** 基线 `PlayerStateService.java:267-270` 在 bars capture 前直接清 plan并返回 UNKNOWN；B mechanics `:116-138` 只核 `hasGeometry()`，而 `WindowNativeBinding.java:59` 只核 width/height，故仍可产出 `READABLE + observedBaseX=-1`，B payload `:74-88` 也接受。该候选已与 B 最新 EOF 的 R2 返修锚点一致；P0/P2 new candidate none。
- **terminal/顺序保持项：** PROBE 仍 queue 外且 HEAL/CACHED 各一次 exclusive callback，STOPPED/NOT_EXECUTED/UNKNOWN 分流在 handler `:1511-1584`；cached-plan 的 refresh→stored-base、300/100/800ms 与 ordered targets 未被 S1 改写。以上为限定静态核，未运行构建。

### PRECHECK - C PlayerState FirstAid Cloud R1 - 2026-07-14T19:59:36-04:00

- **真实 EOF / 实际写集：** C delivery=`:5416-5449`，复读时真实 EOF=`:5474`（父级已发布 R2）。本轮实际 mtime 仅 `PlayerStateFirstAidMacroResult.java`、`RemoteCommandOutcomeEnvelope.java`、`PlayerStateService.java` 三文件（`19:42:36/19:42:47/19:44:42`）；允许读取的 Cloud port/digest 保持 `18:54/18:58`，未见声明范围外写集漂移 candidate。
- **P1 candidate - Cloud closed result 同样接受 `observedBaseX=-1`：** Cloud result `:40-59` 只核 pair/presence；caller `PlayerStateService.java:294-310` 随即映射 bars并可返回 HEALTHY或建 plan，直到 cached path `:327-330` 才拒绝 `-1`，晚于基线 `:267-270`。这与 B producer 是同一跨仓候选、不是第二个独立业务问题；当前 C EOF 已给单文件 R2 锚点。
- **P0 standing compile prerequisite（非本次 R1 新增）：** 当前 Cloud `PlayerStateService.java:13-30,68-80` 仍 import/持有 Cloud source tree 不存在的 `GameClientTracker`、`TextRecognizer`、`InputProvider`、`CoordinateHelper`、`LocationVisionService`、`WindowScopedTempPath`，Cloud POM 亦无 DHXY artifact dependency；因此整类静态编译前置仍在。按本单禁令未运行 compile，不把该既存 partial 归因于三文件返修。
- **其余基线/terminal 已核：** 独立 GEOMETRY read 已从 active probe 移除，plan base 只取同一 typed result（service `:277-310`）；fixed-name mapping 已穷尽 fail-on-unknown（`:504-547`）；port 对 EXECUTED/NOT_EXECUTED/其余 fatal 的零 retry 映射仍在 `CloudPlayerStateFirstAidPort.java:76-98`。除同一 `-1` 候选与既存编译前置外，未见新 P2 parity candidate。

### PRECHECK - D Dialog Green Template Option R1 - 2026-07-14T19:59:36-04:00

- **真实 EOF / 实际写集：** D claim=`:5965`、Implementation Repair #1=`:5967-6015`、真实 EOF=`:6016`；实际唯一源码 `service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java` 为 526 行、mtime `19:47:38`，DHXY 窄 mtime 中未见 D 声明外文件 candidate。P0 compile/API candidate none（限定静态符号核，未运行 compile）。
- **P1 candidate - null/异常 template candidate 仍会截断：** 696 两 loop 明确跳过 `spec==null || templatePath==null || blank`（baseline `DialogService.java:2222-2225,2319-2322`）；当前 `Spec:358-362` 拒 null path、`Command:377-383` 的 `List.copyOf` 拒 null spec，无法按 caller order 继续。另 `loadTemplate:282-287` 只 catch `IOException`，`Path.of` 的 invalid-path `RuntimeException` 会落到 execute 总 catch `:154-156` 并提前 `MECHANICS_FAILED`，不满足 unreadable→continue 的返修条件。
- **P2 candidate - supplied frame/rect shape 未校尺寸：** command `:384-402` 只核二者同现、rect 正矩形；observe `:177-190` 解码后直接组合，不核 PNG dimensions 与 rect width/height。基线 supplied detection 在同一对象内耦合 image+rect（`:2194-2213`）；当前 public shape 可让 match local point 越出声明 rect后仍生成 absolute point（`:140-147`）。
- **P2 candidate - evidence decode owner 未释放：** `ImageEvidence:431-453` 解码 PNG并核 dimensions/SHA，但成功和异常路径均无 `decoded.flush()`；本类现有 flush grep 只有 template/frame/washed（`:134,159,162`），与返修要求的 image ownership 尚有差口。
- **已核保持项：** supplied MATCH_ONLY 零 capture、verify=true MATCH_ONLY 复用 detection frame、MATCH_AND_CLICK gate 后 fresh capture、verify=false 一次 fresh capture均在 `:177-272`；0.85 first-hit与 -1.0 diagnostic 顺序在 `:110-153`，click delay=150ms在 `:149`；fresh exact-HWND geometry/capacity gate在 `:239-280`，Result variant和 bytes/hash/dimensions校验主体在 `:431-500`。本段仅列非绑定 candidate，等待父级最终审查；未修改源码/External日志/主文档，未运行 build/test/runtime，也未作 Git mutation。

### PRECHECK - A Auto Combat Panel Visibility Align I1/R1 - 2026-07-14T20:12:17-04:00

- **真实 EOF / 写集：** I1=`A-log:6363-6417`（签名 SHA-256 `652c111f...`）；本次尾读又出现 R1=`:6464-6484`。当前唯一源码 `service/autocombat/AutoCombatPanelVisibilityLocalMacroMechanics.java` 为 391 行、SHA-256=`933b77e...`，与 R1 声明一致；20:00-20:08 两仓窄 mtime 清单仅出现本文件及 B/C/D 各自声明文件，未见 A 写集漂移 candidate。
- **I1 的两项 P1 candidate 及后到修复事实：** I1 `ensureVisibleInternal:235-245` 仅 `NOT_FOUND` 走 Alt+8，遗漏 696 对首次 capture-null 同样执行 Alt+8+wait+第二次观察；I1 `ensureVisibleAndAlign:215-225` 又在 500ms sleep=false 时提前 `INTERRUPTED`，跳过 696 无条件 post-drag observe/fallback。当前 R1 已分别改为 `NOT_FOUND/CAPTURE_UNAVAILABLE` 同流（`:236-250`）及忽略 settle boolean 后恰一次复查（`:215-226`）；无额外 retry/capture。
- **P1 candidate - closed terminal 可被 unchecked exception 绕过：** `refreshGeometry` 在 public 路径 `:199`、`findAutoCombatBox:265` 均位于 catch 外，`pressAlt8:246`、`dragAndDrop:215` 也无 operation 外层 catch；而 `WindowAwareInputCoordinator.java:51-67` 会传播 action RuntimeException。故这些 collaborator 异常可能直接逸出，未收敛为公开 `MECHANICS_FAILED`。请父级重点复核 `ensureVisibleAndAlign/ensureVisibleInternal/findAutoCombatBox` 的 terminal 边界。
- **已核不变量：** anchor 0.80 → green-mask 0.80 的按需顺序、`±30`、drop `(489,726)`、distance `>20`、500ms、未拖拽零额外 capture、拖拽后一次 fallback、frame/template flush 均保持；exact-HWND capture 与 binding 同取 `GetWindowRect` 坐标，当前未形成 DPI 换算的具体 candidate。P0 compile/API candidate none（静态符号核）。

### PRECHECK - B PlayerState FirstAid DHXY R2 - 2026-07-14T20:12:17-04:00

- **真实 EOF / 实际写集：** claim=`B-log:8358-8360`、R2=`:8369-8393`；可观察源码仅 `PlayerStateFirstAidLocalMacroMechanics.java`（SHA-256 `5d795aa1...`）与 `RemotePlayerStateFirstAidMacroResultPayload.java`（`476bbe09...`），与声明两文件一致，未见 drift candidate。
- **P0/P1/P2 new candidate：none（窄返修）。** mechanics 在 fresh geometry 后、bars capture 前执行 `getX()==-1 -> CAPTURE_UNAVAILABLE`（`:116-127`），不 capture、不携 observation/base；DHXY READABLE constructor 同步只拒绝 X 哨兵（payload `:74-91`）。696 `PlayerStateService:267-270` 也是单一 X gate；HEAL/CACHED 顺序、四 bar ordered mapping、9-key/canonical shape未被本轮改写。
- **跨仓 terminal parity：** C 最新 current constructor 同样仅核 `observedBaseX != -1`（Cloud result `:40-64`）；B/C 对 `X有效、Y=-1` 的接受域一致。限定静态符号未见本两文件新增编译 candidate。

### PRECHECK - C PlayerState FirstAid Cloud R2 to R3 - 2026-07-14T20:12:17-04:00

- **真实 EOF / 写集时序：** R2=`C-log:5480-5520`，随后 R3=`:5549-5589`；两次均只声明 Cloud `PlayerStateFirstAidMacroResult.java` + 日志。当前文件 166 行、SHA-256=`d966dfd4...`，20:00-20:08 窄 mtime 未见 C 声明外源码。
- **R2 P1 candidate / 当前修复事实：** R2 同时拒绝 `observedBaseX==-1 || observedBaseY==-1`，扩大了 696 和 B 的单 X 哨兵接受域；R3 当前 `:51-63` 已仅保留 `observedBaseX != -1`，base pair、READABLE fixed-four-bars 与 CAPTURE_UNAVAILABLE 空表不变。当前窄修复无新的 P1/P2 candidate。
- **standing P0 compile-symbol candidate（非 R2/R3 新增）：** active Cloud `PlayerStateService.java:13-30,68-80` 仍 import/注入 Cloud source tree 不存在的 `GameClientTracker/TextRecognizer/InputProvider/CoordinateHelper/LocationVisionService/WindowScopedTempPath` 等 DHXY 类型；本次 result constructor 修复不触该前置，留给父级统一编译/可达复核。

### PRECHECK - D Dialog Green Template Option R2 - 2026-07-14T20:12:17-04:00

- **真实 EOF / 实际写集：** claim=`D-log:6094`、R2=`:6096-6139`；当前唯一源码 `service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java` 为 534 行、SHA-256=`1fda8e28...`，窄 mtime 未见 D 声明外源码。R2 的 `ArrayList/Collections` API 与 multi-catch 均有现存 JDK 符号，P0 compile candidate none。
- **R2 已核闭合项：** caller-order defensive copy现允许 null element（`:382-390`）；loop依次跳过 null spec、null/blank path（`:112-127`）；invalid path/security failure在 `loadTemplate:289-296` 归入 unreadable并继续。0.85 hit/`-1.0` diagnostic、verify capture matrix、fresh geometry、click 150ms、frame/washed/template finally与十态 shape未被改写。
- **P1 candidate - nullable name 在 click 后破坏 terminal：** public `Spec` 明确允许 null name（`:359-368`），但 loop不跳过它；MATCH_AND_CLICK 会先 `clickLeft(...,150)`（`:156`），随后 `Result` constructor因 `specName != null` invariant（`:489-506`）抛出并被外层映为 `MECHANICS_FAILED`。696 `DialogService:2319-2364` 对同一有效 template 会点击并返回 clicked（actionKey可为 null）；当前可能出现“输入已发生但 terminal 失败”。MATCH_ONLY 同输入也由 matched 改为 failure，父级应核 real caller 是否需在 command shape 拒 null name或允许 result null name。
- **P2 retained candidates / 后续锚点：** supplied PNG 解码后未核 image dimensions 等于 rect dimensions（`:185-196,392-408`）；`ImageEvidence` validation decode 未在 finally flush（`:446-461`）。D 最新 EOF 已据此发布 R3，尚无 R3 Implementation；R2 nullable修复本身未改变这两项。

以上均为非绑定静态预检材料，不替代父级最终审查；本轮未修改任何源码/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK - A Auto Combat Panel Rounds Observation I1 - 2026-07-14T20:19:10-04:00

- **真实 EOF / scope/hash：** claim=`A-log:6515`、Implementation #1=`:6517-6556`；声明与可观察实际写集均为 create-new `service/autocombat/AutoCombatPanelRoundsLocalObservationMechanics.java` + A 日志。源码 300 行，SHA-256=`a9a0e985d462bd20e0052c5b932486404cccce4fce842258a9330d0a6b507876`，与交付声明一致；20:13-20:15 窄 mtime 未见 A 声明外 Java。
- **696 两 rect：** 对照 `696a12b0 AutoCombatPanelService:322-341`，marker 分支保持 `left=markerX`、`top=markerY-96`、`right=left+max(1,width/2)`、`bottom=top+30`；fallback 保持 center 减半宽/半高后取 `196x56`（新类 `:138-154`）。
- **单 capture：** rect 后仅一次 fresh `refreshGeometry`，随后全类唯一 `captureRegion`（`:156-173`）；无 input/retry，capture 空与 binding 空分别进入既有 typed 状态。
- **4x wash/OCR：** `:227-270` 与 baseline `:395-455` 保持每像素 4x4 扩展、`r>=130 && g<=120 && b<=120 && r-max(g,b)>=35`、黑像素计数；`:181-209` 保持 OCR 文本非空项原序拼接及首个 `\d{1,2}` 命中，local sidecar 空单独映射 `OCR_UNAVAILABLE`。
- **ownership：** captured raw 与 washed 均由 `:180-224` 同一 `finally` 各 flush 一次；成功删除 washed debug，no-digits/OCR-unavailable/异常保留既有诊断图。限定范围内 P0/P1/P2 candidate none（未运行编译）。

### PRECHECK - D Dialog Green Template Option R3 - 2026-07-14T20:19:10-04:00

- **真实 EOF / scope：** claim=`D-log:6141`、Implementation Repair #3=`:6143-6187`；可观察实际写集仅 `service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java` + D 日志，当前源码 546 行（SHA-256=`1675c9a3...`），未见声明外 Java。
- **supplied 两维 gate：** PNG 解码且非 null 后，`:196-203` 同时要求 `frame.width == rect[2]-rect[0]` 与 `frame.height == rect[3]-rect[1]`；任一维 mismatch 在返回现有 `MECHANICS_FAILED` 前执行一次 `frame.flush()`，且该分支未把 frame交给 execute 外层，未见 double flush；尺寸相符仍零 detect/零 capture并由正常外层 finally释放。
- **ImageEvidence owner：** validation decode 在 `:453-472` 完成；尺寸与 SHA 校验位于 `try`，`finally` 对非 null decoded 恰 flush 一次，校验成功与异常路径均覆盖；null decode不产生 owned image。bytes clone、尺寸/SHA 接受域未改变。
- **限定结论：** 两项 R3 定点均有当前行证据，P0/P1/P2 candidate none（仅静态核，未运行编译）。

以上为非绑定 PRECHECK，不替代父级最终审查；本轮仅追加本 helper 报告，未修改源码/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - C Dialog White Story Template Whole Observation I1 - 2026-07-14T20:28:18-04:00

- **真实 EOF / scope/hash：** claim=`C-log:5619`、Implementation #1=`:5621-5694`；声明写集为 create-new `service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java` + C 日志。当前源码 265 行，SHA-256=`789bf011b36bb43e906835e88c9c3a009f0e39c3460914a6ad5ee4391c37e90e`，与交付一致。20:23-20:27 filesystem 窄窗还出现 `ImagePreprocessor.java` mtime `20:25:33`，而本单声明冻结该文件；共享并发下无法仅凭 mtime归属，记 P2 write-set attribution candidate 供父级核 owner，不归因给 C。
- **P1 candidate - no-supplied baseline fallback 缺失：** 696 `DialogService:978-981` 先取 usable supplied detection，缺失时必调用 `detectDialogSnapshotDirect(...)`；当前 `observeWhiteStoryTemplate:96-104` 对 supplied null/image null/rect null 直接 `CAPTURE_UNAVAILABLE`，且本类无 detection collaborator/capture 路径。故 no-supplied 输入少一次基线检测/capture，可能把可达 STORY/MATCHED 提前终止。
- **P1 candidate - supplied 优先路径新增 binding gate：** current `:86-91` 在检查 supplied detection 前要求 binding handle/geometry；696 usable supplied frame 路径 `:978-999` 不需另行 window gate。有效 supplied STORY frame+rect 在 binding 暂不可用时，baseline仍洗同帧并匹配，当前先返回 `BINDING_UNAVAILABLE`，改变 supplied capture matrix。
- **P1 candidate - native Mat owner 未覆盖 empty/异常：** wash `:176-204` 的所有 `release()` 仅位于成功尾部；`src.empty()` 于 `:179-181` 直接 return 而不 release src，任一 `cvtColor/inRange/erode/subtract/imwrite` 异常也进入 catch 且无 finally，泄漏此前分配的 Mat/kernel。成功路径六个 Mat 各 release 一次，但失败路径不闭合。
- **P1 candidate - brief 所需 defensive evidence 未进入 terminal：** result record `:226-263` 仅携 template name/path 与 rel/abs point，无 supplied/raw/washed frame bytes、hash、dimensions或rect evidence；因此 future Cloud caller无法从 typed result校验“同帧 thin-white observation”，与任务正文的 defensive evidence 要求存在 contract 缺口。另 public spec 若 name null/blank，loop仍可命中但 compact constructor `:235-250` 将其变为 `MECHANICS_FAILED`，而 696 `:472-491` 可返回 visible+nullable actionKey；记同一 terminal-shape P2 candidate。
- **已核保持项：** usable STORY supplied frame优先复用 rawPath，缺失 rawPath时仅 materialize同一 borrowed image（`:114-133`）；STORY gate在 `:106-112`；template按 caller order跳过 null/blank path、0.85、first-hit（`:135-157`）；`resolveMatchedPointInRect` 后 rel=`point-rect origin`、abs=screen point，与 696 `:458-491` 一致。borrowed `detection.image()` 未被本类 flush，成功 wash Mat释放事实成立。P0 compile/API candidate none（仅静态符号核）。

以上为非绑定 PRECHECK/RISK，仅供父级独立审查；未修改源码/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - D NPC Yellow Target Whole Local Observation I1 Zero-Java Delivery - 2026-07-14T20:42:56-04:00

- **真实 EOF / 写集：** D 日志 `:6259-6287` 为 Implementation #1；声明 create-new `service/npc/NpcClickYellowTargetLocalObservationMechanics.java` + D 日志，当前目标 Java 未创建，故本轮实际 Java 写集为零，未见越界 Java。P0 compile/API candidate none（无待编译源码）。
- **P1 candidate - blocker 的“无可对照权威源”前提不完整：** active DHXY 确无 `GameTextLineOcrService/TextCandidate/TextCandidateScanResult`，但 `DHXY-local-baseline` 保留完整源码；其 `NpcClickService.java` SHA-256 与 `migration-baseline/696a12b0` 同为 `cce8f020...3441`，且 `GameTextLineOcrService:182-204` 正是 baseline caller `NpcClickService:2445-2446` 的方法。无需扩写集即可在唯一新 mechanics 内恢复私有纯 shape 闭包。
- **最小 exact 闭包 1，strict mask：** constants `GameTextLineOcrService:54-65`（line merge=8、component pixels `3..1200`、width `1..120`、height `2..48`、limit=3、minScore=25）；`buildFilteredMask:1209-1235` -> `isTargetTextPixel/isNpcTargetYellowTextPixel/isStallVendorGoldPixel:1463-1515,1549-1557` + `collectComponent/shouldKeepComponent:1281-1332` + private `ComponentBox:2233-2272`。
- **最小 exact 闭包 2，shadow/shape：** `includeNearbyYellowShadow(raw,mask,2):1560-1593` -> `copyMask/isYellowShadowPixel:1595-1613`；`groupTextLines:1243-1279` + private `TextLineBox:2274-2344`；`splitLineByHorizontalGaps/addSplitSegment:1771-1824`；`scoreWashedTextLine:1711-1761` -> `countForeground/countComponents/floodLocal/countLongRuns/countLongRunsInWashedImage/isBlackWashedPixel:1826-1942`。active `OcrWindowRegion:39-90` 已提供相同 expand/clamp，无需新增共享 model。
- **最小 exact 闭包 3，washed/debug：** `toTextMaskImage:1958-1968` 产 **黑字白底**；`writeTextMaskImage:1970-1976` 与可选 `writeCandidateOverlay:1978-2007` 只属 evidence/debug。mechanics 可用底部 private records 承载 `ComponentBox/TextLineBox/Candidate`，无需恢复旧 public `TextCandidateScanResult` service/model。
- **P1 candidate - D 拟用当前 ImagePreprocessor 不等价：** current `ImagePreprocessor:353-380` 是 broad yellow、白字黑底并进入 OpenCV cleanup；696 candidate closure 是 strict `YELLOW_NPC_TARGET` RGB/HSB + vendor-gold exclusion + radius-2 shadow + Java connected-component/geometry scoring，且没有 OpenCV 调用。若用前者，阈值、极性、component 集与 frame penalty 均漂移。
- **排序/坐标不可变项：** `findTextLikeCandidates:995-1015` 仅收 `score>=25`，按 score 降序、region `y1` 升序、`x1` 升序，最多 3 个；region 先向四边 expand 4 且 right/bottom exclusive，click local=`((x1+x2)/2, y2-50)`（`:1716-1749`）。`NpcClickService:2447-2488` 按原序映射，abs=`同次 fresh binding base + scanRegion origin + local`，不得重排、重算或 clamp baseline clickY。
- **P1 candidate - whole-call 同帧前处理锚：** fallback 输入不是原始 capture，而是 `prepareNpcOcrScanImage` 后的同一 `scanImage`（`NpcClickService:1947-1973`）；默认全窗 region 在非 direct-combat 时应用 HUD/chat/shortcut mask，skip 分支保留 raw（`:2505-2531`）。新 mechanics 若独立 capture 却不显式承接该 region/skipDefaultMask 选择，会改变候选集，即使 shape 闭包本身逐字等价。
- **owner/cleanup：** baseline shape 方法把 `raw` 视为 borrowed、不 flush；`maskImage` 在 overlay/no-overlay 正常路径 flush（`:190-203`），overlay 与 Graphics 各由 `writeCandidateOverlay` finally/dispose（`:1981-2006`）。新 mechanics 自己 capture 后应拥有 raw，在 raw/washed evidence 编码完成后 finally 各恰一次 flush；washed 写文件抛异常也须由外层 finally 收口，候选用 primitive coordinates 或 defensive copy，不能让 mutable `Point` 逃逸。
- **依赖边界确认：** 不应调用 `CloudImageProcessor`，其 `:20-25,235-249,269-275` 会经 `ImagePreprocessWashedImageClient` 走 Cloud 并返回另一套 candidate contract；也不应调用 `TextRecognizer` 或任何本地 OCR。上述纯 shape 闭包从 `:182-204` 可达的依赖中没有 `textRecognizer`，NPC 名称判断、OCR、region loop、click/verify/fallback继续留在 caller（`NpcClickService:1052-1081,1960-2046`）。

以上为非绑定 PRECHECK/RISK，供父级决定返修范围；未修改 Java/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A Dialog Option OCR Image Whole Observation I1 - 2026-07-14T20:34:49-04:00

- **真实 EOF / scope/hash：** claim=`A-log:6593`、Implementation #1=`:6595-6632`；声明写集为 create-new `service/dialog/DialogOptionOcrImageLocalObservationMechanics.java` + A 日志。当前源码 271 行，SHA-256=`4c7a784cea3d074a3b031dbfba17fcc2d31b1bb097e61586eac900942c4db8af`，与交付一致；claim 后窄 mtime 仅该文件，较早的 WhiteStory/ImagePreprocessor mtime 属并行既有材料，未见 A 写集漂移 candidate。
- **P1 candidate - green wash 方法语义不是 696 baseline：** 696 `GameTextLineOcrService.readDialogOptionWords:130` 的首 pass 调用 `washGreenTextToBlackAndWhite`，只保留 green；新类 `:204-220` 调用 `washDialogOptionTemplateTextToBlackAndWhite`。当前 `ImagePreprocessor:506-531` 明确把 `isOptionGreen || isHighlightedOptionYellow` 都置白，因此 highlighted-yellow 会提前进入 green evidence，可能改变 Cloud 后续 green-first OCR/variant/fingerprint 归类。
- **P2 candidate - yellow 从条件 fallback 变为无条件预计算：** 696 `GameTextLineOcrService:144-162` 先 OCR green，只有 green 未命中 keyword 才执行 `washYellowText`；新类在尚无 OCR 结果时总是于 `:222-234` 生成 yellow。两者仍是 green→yellow 顺序，但 capture 后 CPU/资源时点及“green hit 时零 yellow wash”不再等价；父级需确认此 eager evidence 是否为获准 plumbing 差异。
- **P1 candidate - PNG/rect/dimension/hash authority 未闭合：** supplied intent `:76-88` 仅核 rect 成对，不核正面积、PNG dimensions==rect dimensions；result `:105-139` 只核三 byte[] 非空与 rect 成对，不解码验证三 PNG 同尺寸、不复算/携带 SHA，也不保证 raw/green/yellow 尺寸等于 rect。故 caller 可构造 `CAPTURED` 但 bytes/尺寸/rect互相矛盾，不能满足用户要求的 defensive evidence authority。
- **P1 candidate - non-CAPTURED shape 与自身合同冲突：** record JavaDoc `:100-104` 声明每个非 CAPTURED 状态既无图像也无 rect；constructor `:121-138` 却只禁止图像，允许完整 rect，且内部 `INVALID_SUPPLIED_FRAME/CAPTURE_UNAVAILABLE/MECHANICS_FAILED` 均经 `failureWithRect:259-261` 携 rect。terminal 跨 wire 前需由父级明确是“全无字段”还是“允许诊断 rect”，当前 shape 不唯一。
- **P1 candidate - refresh 异常未收敛：** no-supplied 路径 `refreshGeometry(binding)` 位于 capture try 外（`:179-183`）；collaborator RuntimeException会越过公开 `MECHANICS_FAILED` terminal。capture RuntimeException（`:190-200`）及 wash/encode RuntimeException（`:204-246`）本身已收敛。
- **已核 capture/ownership：** supplied bytes+rect 分支零 capture；no-supplied 用 fresh binding + `[x+250,y+312,529x208]` 且全类唯一一次 `captureRegion`（`:168-202`）。raw在外层 finally、green/yellow各在编码 finally恰 flush一次（`:204-246`），未见本类 owned BufferedImage double flush；screen-absolute rect保持。P0 compile/API candidate none（限定静态符号核）。
- **已核职责边界：** 本类未调用 OCR、未解释 aliases/target、未选择 fallback、未构造 PreparedAction/DialogResult、未发送 input；696 `DialogService:1836-1895,1898-1958,2134-2142` 的 OCR/business/click/fingerprint选择仍未下沉。

以上为非绑定 PRECHECK/RISK，仅供父级独立审查；未修改源码/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK TRUE EOF CONTROL COPY - D NPC Yellow Target Whole Local Observation I1 - 2026-07-14T20:42:56-04:00

- 完整非绑定证据见本报告同名 zero-Java delivery 段；D 真 EOF=`D-log:6259-6287`，目标 mechanics 未建、实际 Java 零改。
- **P1 candidates：** 696 pure-shape 源及全部私有依赖可从 `DHXY-local-baseline GameTextLineOcrService:182-204,987-1015,1209-1332,1463-1613,1711-2007,2233-2344` 在唯一新类内闭包恢复；current broad/白字黑底 `ImagePreprocessor:353-380` 不等价；whole-call 还须保留 `NpcClickService:1947-1973,2505-2531` 的同帧 default-mask/skip 选择。
- candidate order=`score desc,y1 asc,x1 asc`、`score>=25`、最多 3；local region expand 4，click=`centerX,y2-50`，screen abs 使用同次 fresh binding base + scan origin，原序映射（`NpcClickService:2447-2488`）。
- 不调用 `CloudImageProcessor`，不调用 `TextRecognizer`/本地 OCR；raw/washed/overlay 各按 owner 在 finally 恰一次释放。P0 compile/API candidate none（本轮无 Java）。

以上为非绑定材料，等待父级独立裁决；未修改 Java/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK SCOPE DELTA - D NPC Yellow Target Parent Clarification #1 - 2026-07-14T20:46:57-04:00

- **权威增量：** D 真 EOF `:6288-6328` 已把同任务候选源唯一化为 696 `GameTextLineOcrService.findYellowTextCandidateResult` strict-yellow pure-shape 闭包；禁止 Cloud processor、current yellow wash 和本地 OCR。目标 Java 尚未出现，故本轮 P0 compile/API candidate none。
- **P1 candidate - component pass 次数/顺序不可只按 prose 简化：** baseline 先在 `buildFilteredMask:1209-1235` 对 strict predicate mask 做第一次 `collectComponent + shouldKeepComponent`，再 `includeNearbyYellowShadow(...,2):1560-1593`，随后 `groupTextLines:1243-1278` 对扩展后 mask 再 collect/filter 一次。若实现成“predicate -> shadow -> 单次 component filter”或 shadow 后不再过滤，candidate pixels/line boxes/score 均不等价；应以 clarification 引用源码而非条目词序复制。
- **P1 candidate - baseline caller 的 default-mask/skip 仍未落入 scope：** `NpcClickService:1947-1973` 将 capture 经 `prepareNpcOcrScanImage` 后的同一 `scanImage` 交给 exact OCR 与 shape fallback；`:2505-2531` 对默认全窗 region 应用 HUD/chat/shortcut mask，并仅在 direct-combat skip 时保留 raw。clarification `:6300` 写“raw crop 直接执行”，但又声明零业务差异；新 mechanics 若负责 capture，需由父级确认 command 是否携该 mask policy/等价预处理，否则默认 region 候选集会漂移。
- **P1 candidate - 六态的当前 API 映射仍不唯一：** `WindowNativeBindingRefreshService:38-64` 对无 handle、无 live snapshot、无 geometry 都返回 empty；`BoundWindowCaptureService:46-87` 对非法/越界 rect、native capture failure 也统一 empty。clarification 尚未区分何者映射 `BINDING_UNAVAILABLE`、何者映射 `CAPTURE_UNAVAILABLE/MECHANICS_FAILED`；同一 fresh binding 必须同时用于 capture base 与 abs mapping，不能二次 refresh 后混用 geometry。
- **P1 candidate - INTERRUPTED 可达时点需守 baseline：** baseline `NpcClickService:1938` 仅在 yellow strategy 入口做 stop gate，`findYellowTextCandidateResult:182-204` 及 CPU shape loops 无新增 checkpoint，current capture API也不抛 `InterruptedException`。若为使 `INTERRUPTED` 可达而在 component/score 内新增 checkpoint，会改变 baseline stop 时点；父级应重点核入口 checkpoint、异常映射与 interrupt flag 保持，不把 stop 当 `NO_YELLOW_CANDIDATE/MECHANICS_FAILED`。
- **P2 exact-copy anchor：** connected component 是 8 邻域（`:1281-1317`）；line merge 是 sorted components 的 first matching line、tolerance 8（`:1259-1277,2289-2307`）；gap threshold=`max(16,min(24,height*2))` 且条件严格为 `x-lastInkX > threshold`（`:1771-1824`）；score 的 context source 必须是 `toTextMaskImage` 黑字白底 mask，region expand 4、所有 penalty/reason（含 `Locale.ROOT` density）保持（`:1711-1760,1909-1942,1958-1967`）。
- **已闭合项：** limit=3/minScore=25、score/y/x deterministic order、`y2-50` 不 clamp、same-frame raw/mask evidence、CAPTURED 与 empty-candidate evidence shape、decoded/raw/mask finally owner，以及 Cloud/OCR/input/business 禁区，clarification 已明确；未发现其它候选源遗漏。

以上为非绑定 scope-delta PRECHECK，仅供父级后续逐行复核；未修改 Java/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A Dialog Option OCR Image Whole Observation R1 - 2026-07-14T20:51:22-04:00

- **真实 EOF / 写集 / 符号：** A R1=`A-log:6699-6736`；当前 mechanics 368 行，SHA-256=`fc33436501de70aeea1869671ca04ba5b5f0cafc131672e43e75e38866bd4237` 与声明一致。20:35-20:45 Java 窄 mtime 仅 A/C 各自目标文件，未见 A 写集漂移 candidate。current `ImagePreprocessor.washGreenTextToBlackAndWhite(BufferedImage):534-545`、yellow overload `:362-380` 与 capture/refresh API 均可达，P0 compile-symbol candidate none（静态限定核）。
- **P2 candidate - intent “frame 与 rect 同现”仍只实现单向：** `DialogOcrImageIntent:80-97` 拒绝 partial rect，且 frame→full positive rect；但 bytes 为 null 时允许完整 rect，随后 `hasSuppliedFrame=false`，该 rect 被静默忽略并转 fresh capture。Parent Repair `A-log:6683-6684` 要求两者同现，constructor 尚缺 rect→frame 拒绝。
- **P2 candidate - supplied decode 未完全收敛：** `ImageIO.read` 仅 catch `IOException`（`:232-249`）；ImageIO provider/runtime decode exception及 decode 后取 dimensions 的 RuntimeException 位于外层 wash/capture catch 之前，会绕过 `INVALID_SUPPLIED_FRAME/MECHANICS_FAILED`。日志 `:6713-6714` 所称 public entry 无异常逸出因此尚非结构性成立。
- **P2 candidate - ctor decoded owner 不是 finally：** `verifyVariant:178-203` 在 read 成功后直接取宽高并于 `:190` flush；通常路径、尺寸不匹配及后续 SHA 失败均已释放，但 get-dimensions/runtime 异常时没有 finally owner 保证，与交付“decoded 临时图各自 flush”及 defensive owner 范式仍有窄缺口。
- **已核 Repair 保持：** green 已改为 baseline green-only，yellow 次序不变（`:292-323`）；no-supplied 只 refresh 一次、capture 一次（`:250-279`）；CAPTURED 三 PNG 各带 SHA/shared dims/唯一 rect并逐图 decode 核尺寸/hash（`:132-203`），non-CAPTURED 零 evidence/rect（`:137-148,356-359`），raw/green/yellow 主 owner 各 finally 一次（`:281-337`）；零 OCR/verdict/input/retry。除上述 P2 candidates 外未见新 P1 行为候选。

以上为 A R1 非绑定 PRECHECK/RISK，等待父级独立审查。

### PRECHECK / RISK - C Dialog White Story Template Whole Observation R1 - 2026-07-14T20:51:22-04:00

- **真实 EOF / 写集 / 符号：** C R1=`C-log:5765-5853`；当前 mechanics 433 行，SHA-256=`2a94ef1f1e048d5e552ccc3592b9c256cea2c70862ddd5b4cd87a3a4eb112776` 与声明一致；同时间窗除 A/C 目标文件外无其它 Java mtime，未见 C 写集漂移 candidate。`detectDialog(binding,false,0L,source):95-98`、result accessors、`ImageFinder.find` 与 `resolveMatchedPointInRect` 当前均存在，P0 compile-symbol candidate none（静态限定核）。
- **P2 candidate - MATCHED constructor 未落实 Repair 的 evidence 一致性：** `WhiteStoryTemplateObservation:370-403` 只检查 rect length、bytes/hash非空及 dims>0；没有 decode PNG、复算 SHA、核 decoded dimensions、核 `frameWidth/Height == rect right-left/bottom-top`。因此任意 canonical caller 仍可构造 bytes/hash/dims/rect互相矛盾的 MATCHED，未闭合 `C-log:5753-5755` 的明确条件。
- **P2 candidate - supplied same-frame 证明仍依赖未验证 rawPath：** evidence 从 `suppliedDetection.image()` 编码（`:112-117,171-183`），但 supplied `rawPath` 非 blank 时 wash/template match 直接读取该路径（`:188-220`），未核路径 PNG 与 borrowed image bytes/hash一致。fallback 会把 decoded frame materialize 后再 wash，只有该分支结构性同帧；supplied 分支的 matched point 与返回 evidence 仍可能来自不同像素源。
- **P2 candidate - fallback collaborator RuntimeException 可逸出：** `dialogDetectionMechanics.detectDialog(...)` 位于 `:125-127` 且无 catch；当前 collaborator 自身 `refreshGeometry` 调用也可能抛 RuntimeException。C public entry因此可能没有六态返回。另 `coordinateHelper.resolveMatchedPointInRect:220` 的 RuntimeException同样只经过 image finally、不转 `MECHANICS_FAILED`；父级需核 closed-terminal边界。
- **已核 Repair 保持：** usable supplied STORY及 absentAllowed NONE 在任何 binding gate 前复用（`:97-120`）；其余路径只调用 fresh detection一次且 hide=false/wait=0（`:121-157`）；nullable `spec.name()` 保留、caller order/null-path skip/0.85/first-hit及坐标公式保持（`:202-237`）；fallback decoded image finally flush，borrowed supplied不 flush（`:159-248`）；6 Mats在 success/empty/exception finally逐一 release（`:259-309`）；非 MATCHED payload 为零（`:370-420`）。除上述 evidence/terminal candidates 外未见新的 696 顺序候选。

以上为 C R1 非绑定 PRECHECK/RISK；未修改 Java/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A Dialog Option OCR Image R3 - 2026-07-14T21:03:51-04:00

- **真实 EOF / 写集：** A R3=`A-log:6840-6870`；唯一目标源码 390 行，SHA-256=`675c218c9229c89480fc231c4efd6a94fbc0a29561b2b0e71b2c1430680f8fb2` 与声明一致，未见写集漂移 candidate。
- **R3 闭合证据：** supplied decode 同时收敛 checked/runtime exception（`DialogOptionOcrImageLocalObservationMechanics.java:242-251`）；dimension 读取/比较异常与 mismatch 分别在 `:258-270` 释放 owned raw 并返回既有 `INVALID_SUPPLIED_FRAME`，valid raw 才进入 `:303-359` 外层单一 `finally`。
- **冻结项：** frame/rect 严格 iff（`:80-101`）、validation decoded `finally`（`:183-212`）、green 后 yellow 及各 variant owner（`:314-345`）、三图 evidence/terminal shape 未见漂移；当前引用符号静态可达。R3 限定范围未见 P0/P1/P2 candidate。

以上为 A R3 非绑定 PRECHECK/RISK，供父级最终复核。

### PRECHECK / RISK - C Dialog White Story Template R2 - 2026-07-14T21:03:51-04:00

- **真实 EOF / 写集：** Parent Scope Amendment #1=`C-log:5895-5914`，R2 delivery=`C-log:5916-5985`；唯一目标源码 497 行，SHA-256=`45422e03c0826f022056d65cf4c3173c71f8032167edb8de969b91a4a1fe01f7` 与声明一致，未见写集漂移或新增编译符号 candidate。
- **P1 candidate - amendment 未实施：** supplied 分支仍保存并复用 `suppliedDetection.rawPath()`（`DialogWhiteStoryTemplateLocalObservationMechanics.java:108,115`），且仅在 path null/blank 时才把 selected `frameImage` 写入 window-scoped raw artifact（`:196-208`）。这未满足 amendment 要求的“两分支均以 selected frameImage 为唯一像素权威并总是重新 materialize”；supplied evidence 与 wash/template 像素仍可能分源。
- **已核原 R2 修复：** fresh detection、coordinate resolve、MATCHED construction 的 runtime exception 均收敛既有 `MECHANICS_FAILED`（`:125-153,220-263`）；MATCHED constructor 已核 rect/span/坐标公式、PNG dimensions/SHA，probe 在 `finally` 释放（`:396-465`）；borrowed supplied 不释放、fallback owned frame 单次释放（`:269-273`）。

以上为 C R2 非绑定 PRECHECK/RISK；父级需先复核 Scope Amendment #1 的像素权威缺口。

### PRECHECK / RISK - D NPC Yellow Target Whole Local Observation I1 - 2026-07-14T21:03:51-04:00

- **真实 EOF / 写集：** parent clarifications=`D-log:6352-6388`，delivery=`D-log:6391-6443`；唯一目标源码 869 行，SHA-256=`38c56b0148724b8b393acdc31485520025dc39fa131a8aaabce668a6f4d8c6c8` 与声明一致，未见写集漂移或 P0 编译符号 candidate。
- **P1 candidate - default-mask 合同缺失：** `ScanRegion` 仍只有四坐标（`NpcClickYellowTargetLocalObservationMechanics.java:680-686`），没有 `skipDefaultMask`；capture 后直接以 raw 构建 strict-yellow mask/evidence（`:127-144`），未见 false 分支恰一次 `copyWithDefaultMasks`、prepared-source evidence 或 separate prepared-image owner，未闭合 Parent Clarification #2。
- **P1 candidate - terminal/stop 时点不等价：** `refreshGeometry` 位于 try 外（`:92`），empty/invalid fresh 被映射为 `CAPTURE_UNAVAILABLE`（`:93-99`）而非 clarification 指定的 `BINDING_UNAVAILABLE`；capture 后又新增第二次 interrupt gate（`:127-132`），改变“public entry 仅一次 checkpoint”的基线时点。
- **P2 candidate - canonical result 约束偏弱：** evidence terminal 只核 evidence 非空和 rect length（`:750-765`）；未强制 `CAPTURED` candidates 非空，也未把 raw/mask dimensions 彼此及与 scanRect span 绑定，故 public record 仍可构造与注释合同不一致的 shape。
- **已核保持：** strict-yellow/shadow/两轮 component/line-gap/score-sort 闭包及 top-3/min-25 常量存在（`:52-63,133-153,182-635`）；未见 Cloud image processor、本地 OCR 或当前 yellow-wash 替代；raw/mask/probe owner 当前路径分别由 `finally` 释放（`:157-162,697-725`）。

以上为 D I1 非绑定 PRECHECK/RISK；三项均仅为候选证据，等待父级最终审查。未修改 Java/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### COMBINED PRECHECK - A R3 / C R2 / D I1 - 2026-07-14T21:04:52-04:00

- **A R3 / `675c218c...f8fb2`：** 声明/当前 390 行源码 hash 一致，未见写集或编译符号漂移。supplied decode 的 checked/runtime exception、dimension exception/mismatch 均闭合到既有 terminal（`DialogOptionOcrImageLocalObservationMechanics.java:242-270`）；owned raw 在互斥出口或外层 `finally` 恰一次释放（`:258-270,303-359`）。本轮限定范围无 P0/P1/P2 candidate。
- **C R2 / `45422e03...01f7` - P1 candidate：** Parent Scope Amendment #1 未实现。supplied 分支仍取 `suppliedDetection.rawPath()`（`DialogWhiteStoryTemplateLocalObservationMechanics.java:108,115`），并仅在其 null/blank 时 materialize selected `frameImage`（`:196-208`）；因此 supplied evidence 与 wash/template 输入仍可能分源。R2 原定 exception/evidence/owner 修复位于 `:125-153,220-273,396-465`，未见新增符号风险。
- **D I1 / `38c56b0...d8c6c8` - P1 candidates：** `ScanRegion` 无 `skipDefaultMask`（`NpcClickYellowTargetLocalObservationMechanics.java:680-686`），raw 被直接用于 strict-yellow/evidence（`:127-144`），default-mask prepared-source 合同缺失；`refreshGeometry` 未收敛 runtime exception且 empty/invalid fresh 返回 `CAPTURE_UNAVAILABLE`（`:92-99`），capture 后还存在第二 interrupt gate（`:127-132`），不符单入口 interrupt/refresh terminal 条件。
- **D I1 - P2 candidate：** `Result` 仅约束 evidence presence 与四元素 rect（`:750-765`），未强制 CAPTURED candidates 非空，也未绑定 raw/mask dimensions 与 scanRect span；strict-yellow 私有算法闭包及 raw/mask/probe owner 未见替代或泄漏迹象（`:52-63,133-162,182-635,697-725`）。

本段仅为 A R3、C R2、D I1 的合并非绑定 PRECHECK；候选证据交父级最终审查。未修改 Java/External 日志，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - C Dialog White Story Template R3 - 2026-07-14T21:12:41-04:00

- **真实 EOF / 写集 / hash：** C R3=`C-log:6009-6089`，声明唯一目标源码 + C 日志；当前源码 498 行，完整 SHA-256=`7f803880135af69f64dd2112cb27f86e3751091d2c26489f451f40f453d9282a`，与日志 `:6025-6028` 一致，未见写集或静态符号漂移 candidate。任务消息所列 `...9282` 仅 63 位、缺末尾 `a`，记为交付元数据提示而非源码风险。
- **Scope Amendment #1 已落：** `suppliedRawPath` 零命中；supplied/fallback 只选定 `frameImage` 与对应 rect（`DialogWhiteStoryTemplateLocalObservationMechanics.java:97-162`）。两分支随后都无条件将该 frame 写入 window-scoped `RAW_TEMP_FILE`（`:176-203`），wash 与 caller-order template match 只读取该 artifact（`:208-225`），而 PNG/SHA/dims 同取该 frame（`:181-190`），未见双帧、重复 capture 或旧 rawPath 复用。
- **异常闭合：** dimensions、PNG encode/SHA 与 `saveImage` 均处于同一 try，checked/runtime exception 及 save=false 都返回既有 `MECHANICS_FAILED`（`:185-205`）；template/coordinate/constructor runtime exception 仍在 `:218-264` 收敛，未见新增 terminal。
- **R2 冻结项未见回退：** borrowed supplied 不 flush、owned fallback 由外层 `finally` 恰一次 flush（`:164-170,270-274`）；caller-order、null/blank skip、0.85 first-hit 与坐标公式保持（`:211-269`）；MATCHED constructor 仍核 rect/span、relative/absolute、PNG dimensions/SHA，probe 在 `finally` 释放（`:397-467`）；thin-white wash 六个 Mat 仍在 `finally` 各释放（`:286-336`）。本轮限定范围未见 P0/P1/P2 source candidate。

以上为 C R3 非绑定 PRECHECK/RISK，等待父级最终审查；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A NPC Prepared-Point Click Verify Whole Mechanics I1 - 2026-07-14T21:16:41-04:00

- **真实 EOF / 写集 / 符号：** parent task=`A-log:6883-6899`，delivery=`A-log:6901-6954`；唯一 create-new 源码 184 行，SHA-256=`96bb18c3d265462d02750a89a4b4bfdbab2b8cb077b5e22aa5d1f9aa29f0c660` 与声明一致，未见写集漂移或 P0 静态符号 candidate。
- **696 动作链已核：** 对应声明锚 `696a12b0:NpcClickService.java:176-238,3011-3055`，current `clickPreparedPointAndVerify` 对首轮执行 direct `move -> sleep(150) -> clickLeft(hold 150) -> sleep(firstWait) -> verify`，retry 执行同点 `move -> 150 -> click(150) -> 1000 -> verify`（`NpcClickPreparedPointLocalMacroMechanics.java:103-160`）。输入只用 `InputProvider`，queue API 命中为零，未见 queue-in-queue、候选/公式/目标/fallback 重选或额外 capture。
- **P1 candidate - retry 合同未闭合为 0/1：** intent 只拒绝负数，允许任意 `maxRetries`（`:64-78`），循环按该值执行 N 次（`:140-159`）。因此 caller 可触发 2+ 次 click/1000ms verify，超出本单“0/1 optional retry”限定；极大值还扩大输入与等待时长。
- **P2 candidate - input-worker authority 偏宽：** actual worker 的固定线程名为 `InputActionWorker.java:68` 的 `dhxy-input-action-worker`，但 mechanics 以 `Thread.name.contains(...)` 判权（`:33,112-116,168-170`），名称含该 token 的非 worker 线程也可进入 direct physical input。父级需确认这是沿用约定还是应为 exact authority。
- **P2 candidate - public result canonical invariant 偏弱：** 主流程在 click 前 interruption 返回 `clickProduced=false`，click 后 sleep/verify/retry interruption 与 verifier/input exception 保留 true（`:119-165`），运行路径未见降级；但 public record constructor 仅核 status 非空（`:86-96`），可外部构造 `VERIFIED/NOT_VERIFIED + false` 或前置 terminal + true，与 closed result 注释不一致。
- **checkpoint 复核点：** 除 `TaskSleep.sleep(...)` false 外，源码另在 entry、first verify 前、retry 前、retry verify 前读取线程 interrupt（`:115,125-134,140-154`），无 `TaskCheckpoint` 或队列提交。A EOF 将这些描述为 baseline `shouldStop` 等价点；父级仍需对 696 锚逐点确认次数/时点，避免新增 stop boundary。其余六态、borrowed binding、verifier exception 映射和 0/1 输入路径未见新候选。

以上为 A I1 非绑定 PRECHECK/RISK；候选证据等待父级最终审查。未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - D NPC Yellow Target Whole Local Observation R1 - 2026-07-14T21:26:31-04:00

- **真实 EOF / 写集：** Parent Review + R1=`D-log:6444-6531`；声明写集仍为唯一 mechanics + D 日志。当前源码 958 行，SHA-256=`9810f24522c4371aed570beebdf8b98cf740e9682ffc9017ac8083209d30dcbf`；限定读取未见写集漂移或 P0 静态符号 candidate。
- **P1-1 已逐项落到源码：** `ScanRegion` 已含 `skipDefaultMask`（`NpcClickYellowTargetLocalObservationMechanics.java:751-761`）；仅 default full-window 且 flag=false 时创建一次 masked source（`:151-161,197-229`），candidate/source evidence 均读取 prepared source（`:162-180`），未回退 raw、未新增 retry。
- **P1-2/P1-3 已逐项落到源码：** 唯一 refresh 在 try 内，runtime exception=`MECHANICS_FAILED`，empty/invalid fresh=`BINDING_UNAVAILABLE`，唯一 capture empty=`CAPTURE_UNAVAILABLE`，同一 fresh base 用于 capture/坐标映射（`:95-143,172-182`）；interrupt 仅 public entry `:100` 一次，CPU closure 无第二 checkpoint（helper 定义 `:709-711`）。
- **P2-1 已逐项落到源码：** Result 强制 positive 4-rect、raw/mask dimensions 相同且等于 rect span、CAPTURED 非空、NO_YELLOW_CANDIDATE 空、其它 terminal 零 payload（`:818-855`）。
- **696 yellow-shape 冻结核：** strict-yellow/stall-gold 常量与 predicate（`:53-64,253-311`）、第一次 component filter -> radius-2 shadow -> 第二次 component filter（`:253-345,401-430`）、line/gap/score/sort/top-3/min-25/clickY-50（`:381-708`）仍在原顺序；零 OCR、Cloud image processor、input、target verdict。
- **P2 candidate - masked-copy exception owner：** `copyWithDefaultMasks` 在 helper 内先分配 copy（`:203-229`），但 outer `source`/`sourceIsSeparateCopy` 只在 helper 成功返回后赋值（`:147-158`）。若 draw/mask 在分配后抛 RuntimeException，外层会闭合到 `MECHANICS_FAILED`，但 `finally :186-194` 看不到该局部 copy，无法执行声明的 copy flush；成功/普通 terminal 的 raw/source/mask owner 未见问题。

以上为 D R1 非绑定 PRECHECK/RISK；四个 Parent Review 主返修均有源码闭合证据，保留上述 owner candidate 供父级复核。

### PRECHECK / RISK - B Dialog Prepared-Action Validation Full Chain I1 - 2026-07-14T21:26:31-04:00

- **真实 EOF / 写集：** authority/claim/amendment/delivery=`B-log:8047-8099,8412-8520`；Cloud 10 + DHXY 9 + amendment `ImagePreprocessor` 共 20/20 声明路径均存在。限定读取未见第 21 个交付路径或 P0 sealed/符号 candidate；当前 DHXY mechanics 165 行，SHA-256=`0dd17a501ed1cf47a6651610bdd493983cf70cd8fcd7d8c39b85d18ef0f7dbf3`。
- **四方法 exact add-back：** blocker 所记旧 DHXY 文件止于 533 行；current 只在 `ImagePreprocessor.java:534-663` 追加 green、thin-white、fingerprint build、distance 四块。四个方法块分别与 Cloud/696 authority `:92-103,490-540,712-739,749-776` 逐行完全相等，existing prefix 未被该尾部 add-back 穿插。
- **真链与 696 主行为已核：** current caller 保留 null/clickRequired/fingerprint/rect gates，Cloud 只在 `VALIDATED` 用 wall clock 刷新原 action（Cloud `DialogService.java:1161-1212`）；8/16 由 `:120-121,1190-1192,1215-1219` 选择。Port 取 exact `TaskExecutionContext` 并一次 `executeLocalMacro`（`CloudDialogPreparedActionValidationPort.java:40-71`）；flat request -> DHXY codec/handler -> mechanics 单 capture/wash/fingerprint/distance -> 5-key result -> Cloud typed return 的调用点为 `RemoteCommandEnvelope.java:89-95`、DHXY codec `:378-400,587-608`、handler `:1155-1158,1687-1728`、mechanics `:54-105`、Cloud envelope `:240-367`。handler 使用 `callWith` 且不提交 input queue；未见 nested queue、input、新 retry 或 business TTL。
- **key/digest parity 已核：** command exact 8 keys（DHXY codec `:71-78,390-400`），result exact 5 keys（DHXY codec `:587-608`；handler `:1722-1728`；Cloud envelope `:95-98,277-367`）。Cloud NON_NULL typed tree 与 DHXY hand-flatten 分别由 Cloud digest `:26-36,55-60,96-114`、DHXY digest `:187-198,390-405` 构成，subtree 均不含 macroKind，字段名/空值省略一致。
- **P1 candidate - refresh exception 绕过 typed terminal：** DHXY mechanics 的 `refreshGeometry` 位于主 try 之前（`DialogPreparedActionValidationLocalMechanics.java:69-79`）。该调用若抛普通 RuntimeException，不会成为声明的 `MECHANICS_FAILED`；handler 只在局部捕获 task-stop（`:1695-1709`），最终由总入口映射为 transport `UNKNOWN`（`LocalRemoteGameCommandHandler.java:403-411`），Cloud port 随后 fatal，而 696 baseline `DialogService.java:1171-1210` 对 validation RuntimeException 返回 null。
- **P1 candidate - runtime owner gate 仍未形成真可达链：** `LocalRemoteGameCommandHandler` 不是 Spring component（`:66`）且 current `src/main` 仅见其 constructor 定义（`:114-177`），未见 `new`/bean owner 或 polling-loop construction；B 自身也在 `B-log:8518` 标为 integration pending。因此方法级 caller-to-handler 链存在，但当前 runtime owner reachability 仍待父级单独核实。
- **P2 candidate - public baseline null washMode 接受域收窄：** `PreparedDialogAction` model 允许 `washMode=null`（Cloud model `PreparedDialogAction.java:42-65`），696 wash 的 null 会落 default 分支（baseline `DialogService.java:1220-1233`）；新 Cloud command 在 `DialogPreparedActionValidationMacroCommand.java:28-32` 先抛。已观察的 active producers均显式赋 mode，但 public validation 方法的 malformed/legacy 输入语义比 696 更窄。
- **P2 candidate - measured terminal invariant 不完整：** Cloud result `:20-29`、DHXY wire result `:30-50` 与 mechanics result `:143-153` 只核 metrics all-or-none；未自证 fingerprint nonblank、distance/maxDistance 非负，亦未强制 `VALIDATED => distance<=maxDistance`、mismatch反向。正常 mechanics 在 `:91-94` 生成一致值，但 decoder/API仍可表示矛盾 typed terminal；command 两端也只要求 maxDistance>=0，而非 8/16。

以上为 B I1 非绑定 PRECHECK/RISK；协议、四方法与正常 8/16 调用链已有静态对齐证据，候选项等待父级最终审查。未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A NPC Prepared-Point Click Verify R1 - 2026-07-14T21:28:07-04:00

- **真实 EOF / 写集：** Parent Source Review #10、repair task 与 Repair #1=`A-log:6956-7033`；声明仍只改 `NpcClickPreparedPointLocalMacroMechanics.java` + A 日志。current 195 行，SHA-256=`2a5df1befab59d8495e73102b30cf894c37cc03cf4669e76144fe713642f86c9` 与交付一致，未见写集或 P0 符号 candidate。
- **P1 条件已精确闭合：** `PreparedPointClickIntent` 先保持 `firstWaitMs>=0`，再仅接受 `maxRetries==0 || 1`，其它值构造时 fail-fast（`NpcClickPreparedPointLocalMacroMechanics.java:64-80`）；原 retry loop 未改且上界现只可能 0/1（`:151-170`）。
- **P2 条件已精确闭合：** result compact constructor 拒绝 `VERIFIED/NOT_VERIFIED + false` 与 `BINDING_UNAVAILABLE/NON_INPUT_WORKER + true`，同时明确不限制 `INTERRUPTED/MECHANICS_FAILED` 的真实前/后 click 双值（`:88-106`）。producer 的前置两态均 false、verify/not-verified 均在首次 click 后、异常/中断沿 `clickProduced` 传递（`:120-175`），未见新矛盾 shape。
- **冻结项：** direct input-worker 判权、`move -> 150 -> click hold150 -> firstWait -> verify`、可选一次 `move -> 150 -> click hold150 -> 1000 -> verify`、interrupt 时点、verifier exception 映射、borrowed binding 与零 queue API 均保持（`:114-185`）；未新增 retry/checkpoint/wrapper/caller。R1 限定范围未见新的 P0/P1/P2 candidate。

以上为 A R1 非绑定 PRECHECK/RISK，等待父级最终审查；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - C Dialog Option OCR Words Whole Observation I1 - 2026-07-14T21:40:28-04:00

- **真实 EOF / 写集：** C 日志 `:6104-6200` 声明唯一 Java 新文件 `DialogOptionOcrWordsLocalObservationMechanics.java`；current 269 行，SHA-256=`b12c90745744463a8286027bab53a884146666cec567f002a6aab19f09f6fd4e` 与 EOF 一致。未见声明写集漂移或 P0 candidate。
- **P1 candidate - closed terminal 边界：** public flow 只把 `validateVariant` 的 `IllegalArgumentException` 收敛为 `INVALID_IMAGE`（`:87-93`），但 `ImageIO.read` 仅 catch `IOException`（`:181-185`）；ImageIO/provider runtime 可越过五态。另 `windowScopedTempPath.resolve(...)` 位于写盘 try 外（`:96-103`），其 runtime 也会逸出而非 `MECHANICS_FAILED`。父级需复核五态是否要求覆盖这两处。
- **P2 candidate - selected variant 严格性：** `variant == null` 被改写为 label `unknown` 后继续 OCR（`:79-80`），未以 `INVALID_IMAGE` 闭合；父级合同写明单一已选 GREEN/YELLOW variant，需确认 null 是否应拒绝。
- **P2 candidate - strict PNG：** validation 证明 bytes 非空、SHA/dims/rect 自洽且 `ImageIO.read` 可解码（`:158-204`），但未核 PNG signature/reader format，JPEG/GIF 等可解码 bytes 理论上也可通过。rect span 使用 int subtraction（`:172-178`），极端端点还存在 overflow 后误满足 dims 的窄风险。
- **关键不变量已核：** window-scoped artifact 写原始 clone bytes（`:81-83,96-103`）；runtime provider 调用恰一处（`:107-114`）；empty Optional=`OCR_UNAVAILABLE`、present empty=`NO_WORDS`、异常=`MECHANICS_FAILED`（`:107-124`），没有把不可用/异常伪装成视觉 miss。
- **坐标 / owner / result：** OcrWordResult 按 provider 原序复制为 image-local immutable boxes（`:126-149,236-243`），不在本地做 rect-origin、颜色/alias/target/fallback/input；borrowed bytes/rect 均 clone，validation decoded image `finally flush`（`:189-204`），result 仅 `WORDS` 携非空 `List.copyOf` payload（`:247-267`）。与 `696a12b0 DialogService.processOptionsWithOCRDetailed:1792-1895` 保留 Cloud caller 的 alias/坐标平移/fallback/action 职责相容。

以上为 C I1 非绑定 PRECHECK/RISK，仅供父级最终审查；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - B Dialog Prepared-Action Validation Full Chain R1 - 2026-07-14T21:42:16-04:00

- **真实 EOF / 实际写集：** B 日志 `:8541-8607` 声明 Repair #1 只改 5 Java；current mtime 仅 mechanics 与两仓 command/result 落在 `21:36-21:37`，原 20-file cohort 其余 15 文件均为 `21:12:42` 或更早，支持本轮冻结声明。5 文件 SHA-256 分别 mechanics=`b597e587...a3fd344`、DHXY command=`f1eed14d...030fa78`、DHXY result=`a81d3ddb...898332`、Cloud command=`bc07b178...95da1cd`、Cloud result=`34f04c20...f20e81`；未见额外 R1 写集漂移。
- **P1 candidate - null washMode 全链仍不可达：** 两仓 command 已允许 null（Cloud command `:28-35`；DHXY command `:29-58`），mechanics 也会在 null 时走 default wash（mechanics `:110-124`）；但冻结的 DHXY codec 把 `washMode` 列为必需且非 null 字段（`RemoteOperationPayloadCodec.java:71-74,390-400,1019-1036`），冻结的 DHXY request digest 又直接执行 `getWashMode().name()`（`RemoteProtocolDigests.java:187-198`）。同时 Cloud digest 使用 NON_NULL mapper（Cloud `RemoteProtocolDigests.java:26-36`），null 字段会被省略。故 legacy null 在 decode/digest 阶段即失败或 NPE，不能按 696 到达 default/TEMPLATE_SPECIFIC 分支；这与“其余 15 文件冻结”形成 scope 冲突，需父级决定最小返修边界。
- **P1 修复证据：** refresh 已进入主 `try`，fresh Optional 同时重验 native handle 与 geometry，再进行唯一 capture；refresh runtime 由 catch 收敛为 `MECHANICS_FAILED`（mechanics `:69-107`）。未见 refresh/capture 重复或 fresh handle 漏检。
- **P2 修复证据：** 两仓 command 均只接受 `maxDistance=8|16` 且保留 null washMode（Cloud command `:28-35`；DHXY command `:39-58`）。除上述冻结 codec/digest 候选外，当前五文件未见新增 Java 符号/API candidate。
- **三层 measured invariant：** mechanics result `:145-170`、DHXY result `:30-65`、Cloud result `:20-38` 均要求 measured 三字段齐全、fingerprint nonblank、distance>=0、maxDistance=8|16，并以 `(VALIDATED == distance<=maxDistance)` 封闭两种 measured state；非 measured 零 metrics。三层字段与比较方向一致。
- **冻结项：** mechanics 保持 initial binding/rect gate、single capture、四 wash、fingerprint/distance、raw/washed finally flush（`:62-124`）；本轮未触 caller/port/handler/codec/digest、四个 ImagePreprocessor add-back、queue/owner/timestamp。P0 candidate 未见；上列 null-wire/digest 是本轮新增的首要父级复核点。

以上为 B R1 非绑定 PRECHECK/RISK，仅提供候选与证据，不作最终裁决；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - D NPC Yellow Target Whole Local Observation R2 - 2026-07-14T21:43:06-04:00

- **真实 EOF / 写集：** D 日志 `:6615-6654` 为 Repair #2，声明只改 `NpcClickYellowTargetLocalObservationMechanics.java` + D 日志；current 968 行、SHA-256=`96de62f3a096bd71e6134cb3b4ac6b5b1feaaa5d829c0e0bfaa7738b10c38dcb`，本次 mechanics mtime `21:37:09`。未见本轮写集漂移或 P0 candidate。
- **异常 owner：** copy 分配后 `handedOff=false`，draw/mask 全段由 helper 外层 try/finally 包围；任一 runtime 异常退出均走 `!handedOff -> copy.flush()` 恰一次，再由 public catch 映射 `MECHANICS_FAILED`（mechanics `:203-239` 对照 `:150-194`）。outer 此时尚未取得 source，不会二次 flush copy。
- **成功 owner：** 两轮绘制完成后先置 `handedOff=true` 再 return（`:231-239`）；helper 不 flush，outer 在 `sourceIsSeparateCopy=true` 时只于 `:186-194` flush source 一次，同时 raw 独立 flush 一次。非 mask 分支 `source==raw` 且 flag=false，也不存在重复 flush。
- **Graphics2D / terminal 冻结：** draw 与 mask 各保留独立 inner finally dispose（`:210-229`）；helper 不 catch/吞异常，仍由 observe 的 `catch RuntimeException -> MECHANICS_FAILED`（`:183-185`）。成功/异常路径均未见 double dispose/flush candidate。
- **已通过链条窄核：** source 仍唯一进入 strict-yellow mask、shadow、mask evidence 与 candidate pipeline（`:151-182`）；入口 interrupt 仍唯一一处（`:100-102`），fresh binding/capture terminals 与 Result/坐标未在 R2 handoff block 内改变。当前窄域未见新的 P1/P2 candidate；算法逐字冻结的最终比对仍留父级源码审查。

以上为 D R2 非绑定 PRECHECK/RISK，仅供父级最终审查；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A NPC Task Tooltip Whole Local Macro I1 - 2026-07-14T21:52:43-04:00

- **真实 EOF / 写集：** A 日志 `:7073-7135` 声明唯一 create-new `NpcClickTaskTooltipLocalMacroMechanics.java` + A 日志；current 242 行、SHA-256=`5d32bdfb5a90cce18bcdcf4b58127ae87abd92af332b1bf6c8b45dfd73ee441b` 与交付一致，未见声明写集漂移或静态符号 P0 candidate。`InputProvider.moveMouse/clickLeft(int,int,int)`、`TaskSleep.sleep(long)`、`OcrWindowRegion(...).clamp` 与 `CoordinateHelper.findImagesInRegion(String,int[],double,double)` 当前签名均匹配。
- **P1 candidate - exact binding/capture identity 未在 mechanics 内闭合：** public flow 只核传入 binding handle，并缓存其 base 供 learned ROI（mechanics `:145-160,217-228`）；模板匹配调用却不传 binding，只走 `CoordinateHelper.findImagesInRegion`（`:173-174`）。该 API 通过 ambient `GameClientTracker.captureToFile` 截图（`CoordinateHelper.java:246-255`），tracker 再从 `WindowTaskContextHolder.rawCurrent()` 取并 refresh 另一条 context binding（`GameClientTracker.java:300-319,451-489`）。若 caller 未在同一 `callWith`/exclusive context 中保证二者同一，存在从 context A 匹配、按参数 binding B 计算 ROI/发点击的跨窗风险；即便 HWND 相同，capture refresh 后仍用旧参数 base 算 ROI。父级需重点核未来 handler 的 exact-context gate，或要求 mechanics 对 fresh binding 建立单一权威。
- **P2 candidate - binding geometry shape：** 入口只检查 `hasNativeHandle()`，不检查 `hasGeometry()`（mechanics `:150-160`）；无几何或 `-1` base 仍可进入 ambient capture，并把该 base用于 ROI。有效 caller 可能保证 geometry，但 public closed mechanics 本身未自证，需与上述 integration gate 一并复核。
- **696 行为不变量已核：** region caller-order、每 region 内 finder 原序、阈值 `0.82`/dedup `36`（`:39-50,165-179`）对应 baseline `NpcClickService:1170-1207`；每点 direct `move -> sleep150 -> clickLeft hold150 -> sleep1200 -> verify`、first verified 停、其余 exhausted（`:182-210`）对应 baseline `:1218-1258` 与 `executeMoveClickAndVerify:176-216` 的 `maxRetries=0` 路径。无 queue API、无本地 NPC/strategy/fallback/retry。
- **payload / terminal / owner：** record point `Y+90`、window-relative ROI `[-150,-100,+150,+200].clamp(1024,768)` 保持 baseline（mechanics `:217-228`；baseline `:1433-1468`）；7 态 result 对 verify verdict、pre-scan miss、interrupt/failure 的 click/payload shape 有 constructor 守卫（`:69-138`）。模板帧仍由既有 CoordinateHelper/WindowScopedTempPath/ImageFinder 边界管理，本类不持有 image/Mat。input-worker 判权未在本类实现，任务固定 terminal 也无该状态，因此“已处于唯一 remote-exclusive worker”是父级接线时必须核实的不变量。

以上为 A I1 非绑定 PRECHECK/RISK，仅列候选与证据，不作最终裁决。

### PRECHECK / RISK - C Dialog Option OCR Words Whole Observation R1 - 2026-07-14T21:52:43-04:00

- **真实 EOF / 写集：** C 日志 `:6202-6301` 声明只改 `DialogOptionOcrWordsLocalObservationMechanics.java` + C 日志；current 297 行、SHA-256=`208122379e5cc336b22a23d7089f6e90b07e95ebd79a5de9f411dc5561ee7c4d` 与交付一致。未见写集漂移或新 P0/Java 符号 candidate。
- **null variant：** 任何 bytes clone/provider 前即 `variant==null -> INVALID_IMAGE`，非 null 后只取两值 enum name（`:78-90,249-253`）；第三种选择不再可达。
- **ImageIO / resolve runtime：** decode 的 `IOException | RuntimeException` 转为 `IllegalArgumentException`，由 public validation gate 映射 `INVALID_IMAGE`，decoded 非 null 后仍在 finally flush（`:94-100,208-232`）。`resolve`、null/blank path、`Path.of` 与 `Files.write` 全部位于同一 try，并把 IO/runtime 收敛为 `MECHANICS_FAILED`（`:102-116`）；当前未见原两个五态逸出点回退。
- **strict PNG / long rect：** 解码前逐字节核标准 8-byte PNG signature（`:165-193`）；rect span 以 long subtraction 先核正面积，再与 positive int dimensions 比较（`:194-206`），极端 int 端点不再溢出误配。decoded dimensions、原 bytes SHA 与 finally owner 仍保持（`:208-232`）。
- **冻结不变量：** 原始 clone bytes 仍 byte-exact 写 window-scoped artifact（`:88-90,102-116`）；local-only OCR runtime 调用仍恰一处（`:118-127`），empty Optional=`OCR_UNAVAILABLE`、present empty=`NO_WORDS`，异常不伪装 miss（`:128-162`）；word boxes 保持 provider order、image-local、`List.copyOf` closed payload（`:139-162,263-295`），无 retry/input/color/alias/target/fallback。当前 R1 限定范围未见新的 P1/P2 candidate。

以上为 C R1 非绑定 PRECHECK/RISK，仅供父级最终审查。两项均未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - B Dialog Prepared-Action Validation Full Chain R2 - 2026-07-14T21:57:41-04:00

- **真实 EOF / 写集：** B 日志 `:8609-8676` 声明 Repair #2 仅改 Cloud `DialogService.java`、Cloud `DialogPreparedActionValidationMacroCommand.java`、DHXY `RemoteDialogPreparedActionValidationMacroCommandPayload.java` + B 日志。current SHA-256 分别 `f98fefd3...15f84ca0`、`646e80e4...50a33404`、`edf728cb...7ba3d977`，mtime `21:50:29/21:50:47/21:51:02`；其余原 cohort 文件均停留在 `21:37:22` 或更早，支持三文件限定写集，未见 P0/write-set candidate。
- **null -> TEMPLATE_SPECIFIC：** Cloud caller 在唯一 port 调用前以 immutable action getter 归一化：nonnull 取原值、null 取 `TEMPLATE_SPECIFIC`（Cloud `DialogService.java:1190-1197`）。baseline `washPreparedValidationCrop` 对 null 经过三项显式比较后落 default `washDialogOptionTemplateTextToBlackAndWhite`（`696a12b0 DialogService.java:1220-1233`）；现 mechanics 对 `TEMPLATE_SPECIFIC` 同样落 default（DHXY mechanics `:110-124`），语义路径相同。
- **四个 nonnull passthrough：** enum 恰 `GREEN/YELLOW/WHITE/TEMPLATE_SPECIFIC`（`DialogFingerprintWashMode.java:6-10`）；ternary 的 nonnull 分支直接返回 `action.getWashMode()`，没有 remap/switch（Cloud `DialogService.java:1194-1197`）。mechanics 仍分别映射 YELLOW/GREEN/WHITE，TEMPLATE_SPECIFIC 走 default（`:115-124`），未见 mode 次序或 wash 选择漂移。
- **两仓 constructor nonnull：** Cloud command `:28-35` 先要求 `washMode != null`，DHXY payload `:29-60` 同样拒绝 null；两端继续保持 expectedFingerprint nonblank 与 `maxDistance=8|16`。因此冻结 codec 的 required-non-null gate及 DHXY digest `.name()` 重新获得 producer/constructor 前置保证，未新增 wire 字段、key 或 canonical 分支。
- **冻结不变量：** Cloud caller 的 action/clickRequired/fingerprint/rect gates、8/16 计算、唯一 port 调用及仅 VALIDATED 刷新 wall-clock timestamp 保持（Cloud `DialogService.java:1161-1217`）；R1 mechanics refresh/fresh handle、single capture、四 wash、三层 measured result、handler/port/codec/digest与 standing owner integration gate均不在本轮三文件改动窗口。当前限定范围未见新的 P1/P2 或 Java API candidate。

以上为 B R2 非绑定 PRECHECK/RISK，仅供父级最终审查，不作最终裁决；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - D NPC Player Anchor Whole Local Observation I1 - 2026-07-14T22:01:07-04:00

- **真实 EOF / 写集：** D 日志 `:6656-6741` 声明唯一 create-new `NpcClickPlayerAnchorLocalObservationMechanics.java` + D 日志；current 537 行、SHA-256=`ef3e2de3ad57e7b1d54ffa4bb512d1064241edd04b8b57c789c33d0fb53f9ed0`，与 EOF 的 blob `e604010f...`/SHA/行数一致，未见声明写集或 P0/静态符号 candidate。`InputProvider.pressAlt4()`、OpenCV `imdecode/imencode` 与 capture/refresh API 当前符号可达。
- **P1 candidate - Alt+4 sleep/stop 时点不等价：** 696 `captureCleanNameRegionToMemory` 在 exclusive callback 先 stop，再 `pressAlt4 -> TaskSleep.sleep(400)`；sleep=false 立即 return 且不 capture（baseline `NpcClickService.java:3289-3315`）。当前仅入口检查一次 interruption，Alt+4 后明确忽略 `TaskSleep.sleep(400)` 返回值并继续 refresh/capture（mechanics `:109-138`），故 wait 中断可能产出 `CAPTURED/NO_PURPLE_BLOB` 而非 `INTERRUPTED`。696 还在 capture 后及 wash 前各有 stop gate（baseline `:2913-2942`），当前 `:140-220` 无后续 stop gate；本类 `INTERRUPTED` 实际只在 entry 可达。父级需复核本迁移允许的 checkpoint 集，当前静态行为并非逐时点等价。
- **P1 candidate - OpenCV input Mat owner：** purple wash 直接 `Imgcodecs.imdecode(new MatOfByte(sourcePng), ...)`（mechanics `:290-299`）；临时 input `MatOfByte` 无变量、无法在 `finally :310-316` release。`src/hsv/mask/inverted/encoded` 五个 owner 均释放，但每次观察仍遗留这一 native Mat，与“every Mat released”合同不符。
- **P2 candidate - public closed shape：** `PurpleBlob` 无 compact invariant（`:431-436`），而 `Result` 对 CAPTURED 只要求 blob 非 null，不核 rect 正序/位于 scanRect、anchor 与 bbox 中心关系或 darkPixels 阈值（`:484-520`）；外部仍可构造坐标矛盾的 screen mapping。`ImageEvidence` 仅以 `ImageIO.read` 可解码+SHA/dims 自洽判定（`:438-466`），没有 PNG signature/format gate，public API 可把 JPEG/GIF bytes 表示成“PNG evidence”。正常 producer 均生成 PNG 与正确 blob，但 public result 未完全自证。
- **Alt+4 / exact binding / capture 顺序已核：** `prepareAlt4` 时先 input-worker token gate、direct `pressAlt4`、400ms wait，再唯一 `refreshGeometry` 与 fresh handle+geometry gate，最后唯一 exact-binding `captureRegion`（`:118-180`）；无 submit/queue nesting。除上述 sleep/stop candidate，input、refresh、capture runtime 分别收敛 `MECHANICS_FAILED`，fresh/capture unavailable 分离为 `BINDING_UNAVAILABLE/CAPTURE_UNAVAILABLE`。
- **default mask / purple wash / blob：** 仅 region=`0,0,1024,768` 且 skip=false 时制作一次五区白 mask，其他路径 source=raw（`:58-67,182-197,235-281`），对应 baseline `prepareNpcOcrScanImage:2505-2531`。HSV `120,50,50..160,255,255`、BGR2HSV/inRange/invert、dark `<0x303030`、20/8/4/6000/360/140 gates及全局 bbox center均与 baseline `extractPurpleBlobAnchor:3132-3189` 对齐（mechanics `:69-81,284-360`）。
- **evidence / mapping / owner：** 同一 prepared source 生成 source PNG、washed pixels、raw/mask evidence；scan origin 使用同一 fresh binding base并以 `Math.addExact` 映射 screen-absolute rect/anchor（`:156-220,325-360`）。raw、独立 masked source、washed image与 evidence decoded image均有 success/empty/runtime finally owner（`:182-232,240-277,438-477`）；除临时 input Mat candidate外未见 double flush/release。
- **业务边界：** 本地只产 purple pure-shape blob与同帧 evidence；未出现 player-name OCR/provider fallback、identity/keyword verdict、UX/VX/UY/VY map 公式、`-50` first-shot offset、target/click/verify、retry/TTL/fallback选择。eager blob observation 不强制 Cloud采用它，现文件内未见额外本地业务判决。

以上为 D I1 非绑定 PRECHECK/RISK，仅列候选和精确证据，不作最终裁决；未修改 Java/External 日志/主文档，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A NPC Task Tooltip Whole Local Macro R1 - 2026-07-14T22:05:51-04:00

- **真实 EOF / 写集：** A 日志 `:7137-7206` 为 Parent Source Review #12、R1 task/claim/Repair #1；声明只改 `NpcClickTaskTooltipLocalMacroMechanics.java` + A 日志。current 301 行、SHA-256=`ea1f2d52d4b4ba55f41bae9a48f679e787126ed0d05ff4b0428ee50e900da90f` 与交付一致，未见额外写集、P0 或静态符号 candidate；新增 `WindowTaskContextHolder/WindowRuntimeContext` 与 `hasSameGeometry` API 当前均存在。
- **Gate 1 已核：** 任何 binding/template/scan/input 前先以既有线程 token `dhxy-input-action-worker` 判权；失败返回现有 `MECHANICS_FAILED(false,null)`，不会调用 move/click（mechanics `:53-65,153-178,259-261`）。无新增 terminal 或 queue API。
- **Gate 2 / normalized HWND 已核：** 入口 `matchingContextBinding` 要求 raw context、context binding/handle、双方 geometry 与 `hasSameGeometry` 全部成立（`:173-178,263-288`）。虽然 helper 使用 handle string `.equals`（`:278`），`WindowNativeBinding` 构造已通过 `WindowHandleParser` 归一化并保存 unsigned decimal（`WindowNativeBinding.java:19-34,98-108`），故此比较确为 normalized HWND equality。
- **P1 candidate - empty capture result 跳过 Gate 3：** 每 region 的 `findImagesInRegion` 返回后，代码先在 `matchedPoints.isEmpty()` 时 `continue`（mechanics `:192-196`），post-capture context 重读只对 nonempty 结果执行（`:197-205`）。若 ambient capture 的 `refreshAndCommit` 已改变 geometry但该 region 无匹配，本轮既不返回 `BINDING_UNAVAILABLE`，还会以旧 command rect继续下一 region，最终可能收敛成 `NOT_FOUND`；这没有逐次满足 Parent Repair #12“每次 findImagesInRegion 返回后重读、geometry漂移即 BINDING_UNAVAILABLE”的门。父级需确认 Gate 3 是否必须移到 empty 分支之前。
- **post-capture ROI 正向证据：** nonempty region 会在任何 move/click/payload 前重读同一 raw context，要求 normalized HWND、valid geometry及与 command geometry一致；不符即 `BINDING_UNAVAILABLE`，通过后 ROI base 来自 post-capture binding（`:197-226,245-256`），不再使用入口旧 base。
- **冻结不变量：** `0.82/36`、region/point 原序（`:41-52,183-208`）；每点 `move -> sleep150 -> click hold150 -> sleep1200 -> verify`、单次 verify/first hit stop/零 retry（`:210-238`）；record point `Y+90` 与 ROI `[-150,-100,+150,+200].clamp(1024,768)`（`:245-256`）；七态 result/click/payload invariant（`:77-146`）、interruption 时点、CoordinateHelper template/frame owner及零 queue-in-queue均未见回退。除 Gate 3 empty-path candidate 外，当前限定范围未见新 P2 candidate。

以上为 A R1 非绑定 PRECHECK/RISK，仅列候选和证据，不作最终裁决；未修改 Java、A 日志或 CR，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - D NPC Player Anchor Whole Local Observation R1 - 2026-07-14T22:13:37-04:00

- **真实 EOF / 写集：** D 日志 `:6743-6828` 为 Parent Review #16、R1 task/claim/Repair #1；声明只改 `NpcClickPlayerAnchorLocalObservationMechanics.java` + D 日志。current 595 行、SHA-256=`076ef03721d4ae82c6206073f13b707a637db20a065e32caa15401f51fc69c4d` 与 EOF 一致，未见额外写集、P0或静态符号 candidate。
- **interruption fences / owner：** entry gate `:114-116` 保持；Alt+4 后 settle=false 于 `:137-142` 立即 `INTERRUPTED`，尚无 capture owner；单帧 capture 后 gate A `:187-197` 位于 outer try 内，命中由 `finally :241-249` flush raw；source prepare/encode 后、OpenCV wash前 gate B `:210-219` 命中同一 finally flush独立 source（若有）+raw。三项均不产生 evidence/washed owner，不会 double flush，且对应 696 settle、capture后、wash前时点。
- **Mat 主路径 owner 已核：** input `srcBuf` 与 output `encoded` 均具名；imdecode empty、任一 OpenCV runtime、imencode false及成功 return 都经过同一 finally，依次 release `srcBuf`、nonnull `src`、hsv/mask/inverted/encoded（`:307-337`）。HSV/inRange/invert/encode 次序与阈值未变。
- **P2 candidate - native acquisition 窗口：** `srcBuf/hsv/mask/inverted/encoded` 五个 native owner 均在主 `try` 之前构造（`:307-316`）。若 `hsv`、`mask`、`inverted` 或 `encoded` 的 constructor 在后续 acquisition 中抛出，try 尚未进入，已成功构造的前序 Mat 不会走 `:328-337` finally；“所有路径 release”仍有窄异常缺口。正常 decode/processing/return 路径不受影响，父级需决定是否要求 nullable owner + try 内逐项 acquisition。
- **typed invariants 正向证据：** `ImageEvidence` 在 decode 前核标准 8-byte PNG magic，再核 decoded dims与SHA并 finally flush（`:391-406,487-529`）；`PurpleBlob` 核 inclusive bbox、anchor containment及 darkPixels 20..6000（`:469-485`）；`Result` 保持 evidence/terminal、同尺寸、scan span及 CAPTURED/NO blob shape（`:536-579`）。正常 producer 先经过 width 8..360、height 4..140、darkPixels 20..6000，anchor取 inclusive bbox center，且 max pixel至多 scan end-1（`:346-380`），因此新增 guard不拒绝真实 producer。
- **P2 candidate - exclusive containment 上界：** scanRect 以 `span=right-left` 明确为 right/bottom exclusive（`:553-562`），但 CAPTURED containment 只在 `blob.rectRight()>scanRect[2]` / bottom同形时拒绝（`:568-570`），仍允许 public blobRight==exclusive right 或 blobBottom==exclusive bottom。正常 producer恒 `<`，故不影响内部结果；public结构合同仍可表示越界一像素的 payload。
- **冻结项：** fresh binding/唯一 capture、五块 default mask/skip、HSV `120,50,50..160,255,255`、dark blob阈值/排序无关全局 bbox、fresh-base screen mapping、同帧 source/mask evidence、六 terminal及Cloud保留 OCR/provider/map/click verdict边界均保持当前 I1 值与顺序；未见新 P1或本地业务判断。

以上为 D R1 非绑定 PRECHECK/RISK，仅列候选与证据，不作最终裁决；未修改 Java、D 日志或 CR，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - A NPC Task Tooltip Whole Local Macro R2 - 2026-07-14T22:20:09-04:00

- **真实 EOF / 可核写集：** A 日志 `:7209-7266` 含 Parent Source Review #13、R2 task/claim/Repair #2；声明仅 `NpcClickTaskTooltipLocalMacroMechanics.java` + A append-only 日志。current 源码 304 行、SHA-256=`fc4b97d1ab6f078ea6030491a1c85f80545d0a9de5326b4693d9738eeff5efc3` 与 EOF `:7243` 一致，未见本交付声称的额外 Java 写集。
- **R2 精确闭合：** 每次 `findImagesInRegion` 返回于 mechanics `:192-193` 后，立即在 `:201-204` 重取并验证 `matchingContextBinding(binding)`；该门明确早于空列表 `continue :205-206`。因此空分支也只能在 post-capture 同 HWND/有效且相同 geometry 后继续，漂移先收敛为既有 `BINDING_UNAVAILABLE`，不再误落 `NOT_FOUND`。
- **非空分支 binding：** 同一次重验所得 `postCaptureBinding` 在 `:208` 唯一生成 `windowBaseAbs`，随后 `payloadFor :229,248-259` 用该 post-capture origin 计算 learned ROI；无第二份旧 entry origin。entry authority 仍在任何 scan 前由 `matchingContextBinding :176-177` 校验。
- **相关合同证据：** `WindowNativeBinding` constructor 在 `WindowNativeBinding.java:19-35,98-107` 已标准化 HWND，故 mechanics `:281` 的字符串相等比较是 normalized handle 比较；geometry 合同由 `hasGeometry/hasSameGeometry`（该文件 `:59,83-88`）支撑。`CoordinateHelper.findImagesInRegion:246-290` 保持单次 capture、原 score/dedup 顺序及 screen-absolute points，empty/capture-fail 的既有空列表形状未被 R2 改写。
- **冻结不变量：** input-worker gate `:158-163`；`0.82/36` `:44-45,192-193`；region/point 顺序 `:184-210`；direct `move -> sleep150 -> click hold150 -> sleep1200 -> verify` `:216-228`；Y+90 与 ROI `[-150,-100,+150,+200].clamp(1024,768)` `:248-259`；七 terminal `:78-86` 与 result shape `:124-145`；interruption、owner和零 queue nesting均保持。源码无 `InputSequences`/submit 调用、无 retry/checkpoint/新增 terminal。
- **candidate 汇总：** P0=0、P1=0、P2=0；未见空/非空 post-capture binding 分支、静态符号或冻结行为的新风险候选。

以上为 A Tooltip R2 非绑定 PRECHECK/RISK，仅列静态证据，不作最终裁决；未修改 Java、A 日志或 CR，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - D NPC Player Anchor Whole Local Observation R2 - 2026-07-14T22:21:51-04:00

- **真实 EOF / 可核写集：** D 日志 `:6830-6892` 含 Parent Source Review #17、R2 task/claim/Repair #2；声明仅 `NpcClickPlayerAnchorLocalObservationMechanics.java` + D append-only 日志。current 源码 613 行、SHA-256=`0a3f6b5fd5e09b1b8728f25af3f3c1de808aea5e7fcc9799ad7b0ba94f09963a` 与 EOF `:6869` 一致，未见本交付声称的额外 Java 写集。
- **native owner acquisition：** `srcBuf/src/hsv/mask/inverted/encoded` 六个 owner 全在 `:311-316` 先置 `null`，且 `new MatOfByte -> imdecode -> new Mat/cvtColor -> new Mat/inRange -> new Mat/bitwise_not -> new MatOfByte/imencode` 的逐项 acquisition 全位于同一 `try :317-333`。任一后续 constructor 或 OpenCV runtime failure 均已进入该 try，能到达 finally。
- **release exactness：** 唯一 `finally :334-353` 对六个 owner 分别作一次 nonnull guard + 一次 `release()`；decode empty `:320-322`、imencode false `:329-331`、成功 return `:333` 及 processing runtime 均走该 finally。未见匿名 Mat、第二 release 点或 handed-off native owner，Review #17 的 acquisition 窗口已按要求关闭。
- **exclusive upper gate：** `Result :569-578` 以 `right-left/bottom-top` 建立 exclusive scanRect span；CAPTURED containment 在 `:584-588` 对 inclusive `blob.rectRight/rectBottom` 使用 `>= scanRect[2/3]` 拒绝，left/top 的 `<` 规则保持。正常 producer `:390-396` 的 max pixel 恒至多 exclusive upper-1，故结构门不拒绝正常 observation。
- **settle 语义与文字：** 注释 `:118-123,137-139` 均明确 baseline sleep false 为 pre-capture interruption；实际 `TaskSleep.sleep(400)` false 在 `:140-142` 返回既有 `INTERRUPTED`，不会进入 refresh/capture。capture 后 gate `:192-197` 与 source PNG 后/wash 前 gate `:210-216` 保持，owner 分别由 outer `finally :241-249` 恰当释放。
- **冻结项：** fresh geometry + 单 capture `:145-185`、五 default masks `:61-67,252-295`、HSV `120,50,50..160,255,255` 与 wash顺序 `:307-333`、dark blob bounds/mapping `:362-397`、PNG/SHA/dimensions evidence `:503-545`、六 terminal/result shape `:460-468,552-610`、Cloud/local职责边界均未见漂移；未新增 retry/checkpoint/queue submission或本地业务判断。
- **candidate 汇总：** P0=0、P1=0、P2=0；未见 R2 定点修复产生新的静态符号、owner、terminal 或冻结行为风险候选。

以上为 D Player Anchor R2 非绑定 PRECHECK/RISK，仅列静态证据，不作最终裁决；未修改 Java、D 日志或 CR，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - C Dialog Option OCR Full Same-Frame Chain I1 - 2026-07-14T22:50:36-04:00

- **真实 EOF / 可核写集：** C 日志 `:6319-6439` 声明并交付 Cloud 13 + DHXY 10 Java（另本 append-only 日志）；23 个声明路径均存在，当前只读 `path|SHA-256` 清单聚合 SHA-256=`e57dbd51f94f9ef704246cd8bb8e904d82a4629d1c933abe6a9baad2566e8d40`。关键 current SHA：Cloud `DialogService.java`=`4d2e15c64ce385d12ce41b72b4a221cec318c795b2e186e1feb046f93efbf9aa`，DHXY handler=`a5e8bc13438a6f9ba73e4a7a6c1a55a32d0d5444e1ea854c57695e06d5a48379`；未见 EOF 声称的额外写集。因本单禁止 Git，本 helper 不把共享树其它 dirty 归因于 C。
- **P1 candidate - words terminal 被 caller 折成普通 miss：** `DialogService.optionWordsViaPort:1977-1994` 对除 `WORDS` 外的 `NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED` 一律返回 `List.of()`；`readDialogOptionWordsViaPorts:1955-1968` 随后只按空词/keyword miss 判断并调用 YELLOW，最终 merge。故 yellow 并非“仅 alias/keyword miss”后运行：green provider unavailable、invalid image或mechanics failure也走 yellow；两色失败还可继续 baseline fallback/click `:1853-1880`。底层 mechanics 明确区分五态（`DialogOptionOcrWordsLocalObservationMechanics:118-162,255-260`），wire/port也逐名保留，折叠发生在 Cloud caller，父级需复核失败是否应直接闭合而非形成视觉 miss。
- **P1 candidate - supplied image bytes 未被 request identity 覆盖：** Cloud image command字段 `DialogOptionOcrImageMacroCommand:17-23` 与 DHXY payload `RemoteDialogOptionOcrImageMacroCommandPayload:21-28` 均无 supplied-frame SHA；Cloud digest在 `RemoteProtocolDigests:40-43` 删除 `suppliedFramePngBytes`，DHXY digest在同名文件 `:199-213` 同样重建为无 bytes/无 hash。handler `LocalRemoteGameCommandHandler:1484-1495` 直接把收到的 bytes交 mechanics，而 mechanics `DialogOptionOcrImageLocalObservationMechanics:242-270` 只解码/核 dimensions，随后对收到的像素重新生成 output SHA。两侧 canonical parity虽一致，但不同 supplied pixels（同 rect/dims）可拥有同 request digest，存在 retained command identity/integrity候选。
- **P2 candidate - supplied detection 静默降为 fresh frame：** `DialogService.requestDialogOptionOcrImages:1897-1910` 仅在 detection PNG encode 成功且 rect 正面积时填 supplied command；已有 detection image 但 encode 返回 null或 rect异常时，会发送“完全 absent” command，local mechanics据此进入 fresh capture分支 `DialogOptionOcrImageLocalObservationMechanics:272-301`。这会把“detection有帧则复用”改成异常时另一帧 fallback，而不是 closed image failure；父级需对照 696 supplied-frame authority决定是否允许。
- **同帧/owner 已核不变量：** image mechanics supplied分支不 capture，absent分支仅一个 `captureRegion :289-300`；同一 `raw` 依次生成 green再yellow并各自产生 immutable PNG/SHA/dims/rect `:303-359`。result三份 byte[] 在 local/Cloud/DHXY constructors与accessors均 defensive clone；raw/green/yellow image owners均由现有 finally释放。words mechanics只把传入 variant bytes写 window-scoped artifact并恰调用一次 local-only provider `:102-130`，无 capture、input或retry。
- **协议/API 对称已核：** 两仓 kind均新增同名2项（Cloud `LocalMacroKind:4-13`；DHXY `RemoteLocalMacroKind:7-16`），command/result sealed permits分别闭合；Cloud request/outcome 9 typed slots与互斥分支完整（`LocalMacroRequest:9-158`、`LocalMacroOutcome:9-151`）。image flat key set两侧同为16键、words同为3键（Cloud envelope `:100-111`；DHXY codec `:79-100,707-752`）；handler flat map `:1545-1612`、Cloud decode `RemoteCommandOutcomeEnvelope:301-315,435-454` 与 final 11-field `LocalMacroOutcome`对称。未见明显 enum/constructor/permit/字段名静态符号 mismatch；未以构建验证。
- **digest parity 已核：** words request两侧都剥离 `variantPngBytes`且保留 `variantSha256/dims/rect`（Cloud digest `:44-46`；DHXY digest `:214-230`）；image outcome两侧均剥离 raw/green/yellow三份 bytes并保留三 SHA/dims/rect/status（Cloud digest `:121-129`；DHXY digest `:438-475`）。image/words EXECUTED各用独立 exact flat shape，四态 envelope非 EXECUTED仍走原4键规则；未见两仓 canonical字段差异，supplied request完整性例外已单列。
- **696业务边界：** 除上述 terminal/fallback候选外，Cloud caller仍保持 green-first、keyword hit跳过yellow、green后yellow merge顺序、alias循环、screen origin平移、prepared-action与fallback/click顺序（`DialogService:1829-1880,1955-2045`）。DHXY handler/mechanics不含 target/alias/keyword/merge/fallback/click业务选择，且未见新增 owner/permit字段/session/ledger/TTL/retry/wrapper或 queue nesting。
- **candidate 汇总：** P0=0；P1=2（words失败态被普通 miss化、supplied bytes不受request digest/hash约束）；P2=1（已有 detection frame异常时静默 fresh capture）。

以上为 C Same-Frame Chain I1 非绑定 PRECHECK/RISK，仅列候选与静态证据，不作最终裁决；未修改 Java、C 日志或 CR，未运行 build/test/runtime，也未执行 Git 操作。

### PRECHECK / RISK - C Dialog Option OCR Full Same-Frame Chain R1 - 2026-07-14T23:24:13-04:00

- **范围与证据口径：** 已读 C 固定日志真实 EOF `:6441-6533` 的 Parent Source Review #12、R1 task/claim/Implementation Repair #1；按其 25-Java 写集逐项只读当前两仓 command/result/port/request/outcome/envelope/codec/digest/handler/caller 与两份 local mechanics，并对照 `696a12b0:DialogService.java:1792-1895`、`696a12b0:vision/GameTextLineOcrService.java:120-162`。本节为非绑定 PRECHECK/RISK，不作最终裁决。
- **Review #12 supplied SHA 项静态闭合候选：** Cloud command `DialogOptionOcrImageMacroCommand.java:17-48` 与 DHXY payload `RemoteDialogOptionOcrImageMacroCommandPayload.java:21-64` 均加入 `suppliedFrameSha256`，并锁定 frame/SHA 同现同缺、frame 必带完整正面积 rect；Cloud producer `DialogService.java:1916-1932` 对 detection PNG 计算 SHA 后同 command 携带。Cloud request digest `RemoteProtocolDigests.java:36-46` 仅剥离 binary bytes、自然保留 SHA；DHXY rebuild `RemoteProtocolDigests.java:199-217` 显式保留 SHA/rect/source，未把 binary node 放回 canonical tree。DHXY intent 与 image mechanics `DialogOptionOcrImageLocalObservationMechanics.java:80-127,266-287` 在 decode/capture/wash 前重算 supplied bytes SHA 并以 `INVALID_SUPPLIED_FRAME` 拒绝不一致。**边界措辞候选：** Review #12 写的是“调用 mechanics 前重算”，当前重算位于 mechanics public entry 内而非 handler `LocalRemoteGameCommandHandler.java:1477-1496` 调用前；功能上仍早于任何像素消费，但是否要求把信任门前移到 handler，由父级按原审查措辞决定。
- **Review #12 RAW/green/yellow 项存在高风险未闭合候选：** 两仓 image result 已改为 raw 必有、green/yellow 各自可选（Cloud `DialogOptionOcrImageMacroResult.java:31-70`；DHXY `RemoteDialogOptionOcrImageMacroResultPayload.java:54-101`；local mechanics result `:152-211`），local image mechanics也会在 green/yellow wash/encode不可用时返回 null variant，而不直接丢 raw `:355-410`。Cloud caller `DialogService.java:1993-2020` 已恢复 green 不可用走同一 raw、yellow 不可用保留 green、仅 green keyword miss 且 yellow 可用才 OCR yellow。**但是** `CloudDialogOptionOcrImagePort.verifyIntegrity:68-76` 仍无条件执行 `verifySha(greenBytes, greenSha)` 与 `verifySha(yellowBytes, yellowSha)`；任一合法可选 variant 缺席时，`sha256Hex(null)` 在 `:79-89` 会先抛空指针，RAW 路由或“保留 green”路由根本到不了。该点直接复现 Review #12 要消除的 green/yellow availability 中断风险，且 R1 的交付说明未列该 port 为本轮实际修改文件。
- **Review #12 detection rect 三形态静态闭合候选：** Cloud/DHXY command constructor 与 local intent 均允许且只允许 `SUPPLIED(frame+SHA+rect)`、`FRESH_AT_RECT(no frame/SHA + rect)`、`FRESH_DEFAULT(all absent)`；partial rect、SHA-only、frame-without-rect均拒绝。Cloud `DialogService.requestDialogOptionOcrImages:1900-1932` 独立读取有效 `detection.dialogRect()`，即使 detection image不能编码仍发送 rect-only；local mechanics `:317-350` 在 fresh 分支有 rect即按 caller rect单 capture，只有 rect也缺席才用 committed default。scan rect随后贯穿 result、word command和 screen-origin平移，未见退回固定 ROI 的静态路径。
- **两仓合同/codec/digest 对称：** image command字段两侧同为 bytes/SHA/四 rect/source；words variant两侧均为 `GREEN/YELLOW/RAW`；image result两侧同为 raw-required、green/yellow optional、共享 dims/rect，words result五态/box shape未变。DHXY codec command key set含 `suppliedFrameSha256`，image EXECUTED 仍为 exact 16-key flat shape；handler intent映射、result flat map和Cloud envelope decode字段名一致。outcome digest两侧均排除三份 bytes并按 NON_NULL 保留各 SHA/dims/rect/status；DHXY `RemoteProtocolDigests.java:442-479` 对 optional green/yellow SHA按非空加入，与Cloud value-to-tree strip一致。除上述 Cloud port null optional 风险外，未见新的 field/key/enum/permit/canonical tree静态不对称。
- **写集/静态编译风险候选：** 以 `2026-07-14T23:00:00-04:00` claim 为界，观察到 12 个 Java 文件在其后写入，全部属于获准 25-Java 集；未观察到 claim 后写入写集外 Java。C 日志称“涉15文件”，但本 helper不以文件时间戳反推所有权，只记录当前可见的 12-file write-time 子集。未运行 compile；新增 `RAW` 的 Cloud/DHXY enum与 handler `valueOf(name)`静态同名，新增 command constructor参数在可见 producer/codec/handler链上对齐。最明确的运行/编译后验风险仍是 optional variant进入 `verifySha(null, null, ...)`，并非符号缺失。
- **候选汇总：** supplied SHA identity与rect三形态在静态结构上已形成闭合候选；RAW/green/yellow基线回退仍有 1 个高风险候选（Cloud image port无条件校验可选variant）；另有 1 个信任门位置措辞候选（mechanics entry内重算 vs handler调用前）。

以上为 C R1 非绑定 PRECHECK/RISK，仅列候选、证据与影响，不作审查结论或最终裁决；未修改 Java、External C 日志或 CR，未运行 build/test/runtime，也未执行 Git mutation。

### PRECHECK / RISK - C Dialog Option OCR Full Same-Frame Chain R2 - 2026-07-14T23:44:10-04:00

- **范围与真实 EOF：** External C 固定日志物理 EOF=`:6631`；R2 task/claim/Implementation Repair #2 为 `:6567-6631`，其后无覆盖材料。本轮只核 Source Review #13 留下的四个定点返修项和 7-Java 写集，不扩展旧链。
- **7 文件写集证据：** claim `:6577-6579` 列出 Cloud 3 + DHXY 4 Java。领取时间 `23:25:42-23:41:00` 的两仓 Java `LastWriteTime` 扫描仅出现这 7 个路径；当前 blob 依次为 Cloud port `9f941f8cb0b23f3648e6c78dbf39af4725acab0f`、command `096f3ca41d0b4bf7f92b05c119233eefdb34203d`、result `2cff18fa95656cf6cedfdc3d8f8037234f6f4d68`，DHXY handler `e8a1cb4157e0116af26946e7e983cc4d2dfff773`、command payload `d1a4943ac8401ecc0d1d6d57a5689ae33e9a5151`、result payload `57a47c98b4d564f3e8343d3eae375eb5f3dc700c`、image mechanics `9b779e8fdb780dce30562f0aaea07c8af5d12e10`。C 在 `:6583-6584` 声明的 port/mechanics/handler 三个关键 blob 与当前值逐项相同；未见声明写集外的同时段 Java 写入迹象。
- **R2-1 optional SHA pair/null：** Cloud `CloudDialogOptionOcrImagePort.java:68-80` 对 `CAPTURED` 始终校验 raw，green/yellow 分别进 `verifyOptionalSha`；`:82-90` 先强制 bytes/SHA 同现同缺，both-null 不调 `verifySha`，仅成对存在时重算校验。因此合法 optional null 不再进入 `MessageDigest.digest(null)`，单边存在仍严格拒绝，已存在变体不会跳过 SHA。
- **R2-2 wash 异常降级：** DHXY `DialogOptionOcrImageLocalObservationMechanics.java:357-365` 保持 raw encode/SHA 失败为整链 `MECHANICS_FAILED`；`:375-392` 和 `:394-411` 分别把 green/yellow wash + encode + SHA 包入独立 `try/catch (IOException | NoSuchAlgorithmException | RuntimeException)`，异常时只将该 variant bytes/SHA 同时置 null。已获得的 washed image 在 `:380-385` / `:399-404` 的 `finally` flush，raw 在 `:423-424` 统一 flush；green 先、yellow 后的顺序未变。
- **R2-3 handler SHA 前置门：** DHXY `LocalRemoteGameCommandHandler.java:1487-1497` 在构造 intent 和调 mechanics 前取 supplied bytes、重算 SHA；不一致或算法不可用时，`:1491-1495` 直接返回 `EXECUTED + INVALID_SUPPLIED_FRAME` closed typed payload。intent 构造从 `:1498` 开始，mechanics 调用在 `:1508-1510`，证明信任门确实在 collaborator 之前；SHA helper 在 `:1522-1533`。mechanics 内的二次校验保留为 defense-in-depth。
- **R2-4 合同注释：** Cloud command `DialogOptionOcrImageMacroCommand.java:5-19` 和 DHXY command payload `RemoteDialogOptionOcrImageMacroCommandPayload.java:7-21` 都明示 `SUPPLIED / FRESH_AT_RECT / FRESH_DEFAULT`；Cloud result `DialogOptionOcrImageMacroResult.java:3-14`、DHXY result payload `RemoteDialogOptionOcrImageMacroResultPayload.java:7-15` 以及 local result `DialogOptionOcrImageLocalObservationMechanics.java:129-136` 都明示 raw-required、green/yellow OPTIONAL 且各自 bytes/SHA 同现同缺。未见本次 touched public contract 仍宣称 frame/rect 全有全无或三图必须全有。
- **`696a12b0` 等价性证据：** 基线 `vision/GameTextLineOcrService.java:130-136` 是 green wash 不可用则 OCR 同一 raw；`:144-154` 是 green-first 且 yellow 不可用时保留 green；`:156-162` 才读 yellow/合并。R2 mechanics 的两个 optional-unavailable 结果正好恢复这两个基线入口；7 文件写集不含 Cloud `DialogService`，External C `:6612-6614` 也明确冻结 alias/keyword/green-first/yellow 条件/merge/click/prepared-action/fallback/terminal folding。前置 SHA 门只拒绝 supplied bytes 与其已有请求 SHA 不一致的输入，正常同像素路径的分支、顺序和 fallback 不变。无已批准业务差异；按 `696a12b0` 等价迁移。
- **明显编译风险：** 静态检查未见新符号缺失；port helper 签名/调用对齐，mechanics multi-catch 三类型无继承冲突，handler SHA 使用全限定 JDK 类且 typed payload helper 已存在。保留风险是本 helper 按命令禁止 build，因此上述只是源码级明显编译风险预检，不替代父级后续统一编译门。

以上仅为 C R2 非绑定 PRECHECK/风险/证据；未修改 Java、External 日志或 CR271，未运行 build/test/runtime，也未执行 Git mutation。

### Preflight - A/C/D NPC 专用合同 cohort Implementation #1 - 2026-07-15T00:04:05-04:00

本条是 Delivery Preflight Helper 的非绑定写集/风险预检，只给父级源码审查清单，不作交付裁决。已核
whole-service 计划、迁移矩阵 `NpcClickService` 行与 2026-07-14 23:47 EDT 进度、`docs/业务逻辑.md`
NPC Click Cloud FIFO 职责边界，以及 A/C/D 固定日志物理 EOF：A=`:7382`、C=`:6719`、D=`:6998`。

#### External A - tooltip + prepared-point（10 New）

**Observations**

- 声明写集与当前文件集合一致：Cloud 6 个 New + DHXY 4 个 New，A EOF `:7331-7340` 的 10 个 SHA-256
  与当前磁盘逐项一致。A 写入时段为 `23:53:09-23:54:42`；Cloud `NpcClickService.java` 当前写时仍为
  `11:54:50`，tooltip/prepared-point mechanics 写时仍为 `22:14:20/21:25:26`。generic shared 12 的最新
  写时均早于本 cohort（Cloud 最晚 `22:27:40`，DHXY 最晚 `23:32:46`），未见本轮物理改写迹象。
- prepared-point command 的 `screenX/screenY/firstWaitMs/maxRetries/description`、`firstWaitMs>=0`、
  `maxRetries in {0,1}`，以及六态 result 的 `clickProduced` 组合，与 released mechanics 对齐；Cloud/DHXY
  两侧字段、状态名与 constructor 接受域一致。
- tooltip result 七态、`clickProduced/payload` 组合、record point、window-relative ROI 字段与两仓 payload
  形状一致；两个 port 都只声明 caller-facing typed 方法，未提前引用未注册 kind/permit。

**Risks**

- tooltip command 的 constructor 不是 local intent 的同接受域镜像：released
  `TaskTooltipClickIntent` 把 `regions==null` 规范化为 empty，并允许 null/blank `templatePath` 进入 mechanics 后分别
  形成 `NOT_FOUND/TEMPLATE_UNAVAILABLE`；Cloud 与 DHXY 新 command 均在构造期拒绝 null regions，并拒绝
  null/blank template。后续 caller/codec 若保留 baseline nullable 输入，会在到达 typed terminal 前异常；父级需决定
  是合同恢复 local 接受域，还是证明所有 696 reachable caller 已先闭合为 non-null/non-blank，且不改变 terminal folding。
- 两 port 目前是无实现接口，且 command/result 尚未加入 sealed transport。当前 standalone 类型本身未见明显缺符号，
  但 integration 必须同轮补 kind/permits/request/outcome/envelope、两仓 codec/digest/handler、transport implementation
  和 `NpcClickService` caller；不得把接口存在误计为可达链。

**Parent-checklist**

- 逐项裁定 tooltip 的 null/blank/empty 接受域及其 `TEMPLATE_UNAVAILABLE/NOT_FOUND` terminal 是否在 wire 前保留。
- shared integrator 检查 2 kind、4 sealed payload/command permits、2 outcome slot、flat key/canonical digest、handler
  恰一次 mechanics 调用，以及 phaseCode/actionSlot/timeout 的 caller 原值；复核 prepared-point 仍只允许 0/1 retry。

#### External C - yellow-target（5 New）

**Observations**

- 声明的 5 个 New 均存在，写入时段 `23:53:15-23:55:06`；yellow mechanics 写时仍为 `21:37:09`，Cloud
  `NpcClickService` 与 shared 12 写时均未进入本 cohort 时段。当前 SHA-256 为 Cloud command
  `be7adc78502221394af5f19403cbec47b14f58e2b36eb521e9f764a45a7c7094`、result
  `5cb37c9731a9ae702f9fb601f1c8a888dc8be268cbba953e5c4caf09aa3c183d`、port
  `e4ac1c59ade7eafdbc51e67857d9dbcf8793ae926246d9b485783e5fbffb3e7d`，DHXY command/result
  `3539b4c0a72d7c5f5ea2f121efa701a59856b01b6bb1154378e1a91f2bfe444a` / `ccc690c8dbf3db626beb18057789653af1fc3507bc6c340ab38e22ea19c449e4`。
  C EOF `:6673-6680` 标的是短 `blob` 值而非 SHA-256，交付证据名称需父级区分，不能拿短值与文件 SHA-256 直接比。
- command 的 `left/top/right/bottom/skipDefaultMask` 与 positive-area constructor 精确对应 local `ScanRegion`；六态
  status、candidate 几何字段/原序、raw+mask+scanRect 扁平字段、evidence/non-evidence terminal shape 在 Cloud/DHXY
  两侧一致，未下沉 target/OCR/click/verify/fallback 决策。

**Risks**

- local `ImageEvidence` constructor 会解码真实 PNG、核 decoded dimensions、重算 SHA；两个新 result constructor
  只核 bytes/SHA 非空和声明 dimensions/scan span，未解码或重算。Cloud port 仅补 raw/mask SHA 重算，仍不核 PNG
  可解码与 bytes 实际 dimensions；DHXY payload 本身也不核 SHA。正常 mechanics producer 已自证，但 wire/result 的
  public invariant 不是日志所称的“逐字镜像”，父级需确认完整性门应落在 result constructor、port，还是 handler mapping。
- `CloudNpcYellowTargetPort` 是可注入 `@Component`，但 `runYellowTargetMacro` 当前固定抛
  `UnsupportedOperationException`；其 public API 也只有 `observeYellowTargets(command)`，没有 A/D port 所需的显式
  retained `phaseCode/actionSlot/timeout` 输入。当前引用符号静态存在，但一旦 caller 提前注入/调用就是确定失败；后续
  integration 必须改写该类并确认 retained-action 地址来自 baseline caller，而不是临时常量或新策略。

**Parent-checklist**

- 复核 raw/mask PNG decode、actual dimensions 与 SHA 的最终单一权威门，确保 candidate 原序和 same-frame evidence
  不因扁平化而弱化。
- shared integrator 在注册 `NPC_YELLOW_TARGET` 前先冻结 port API/retained address，再补 outcome slot、codec/digest、
  handler 到 released mechanics 的一次调用；确认 integration 后不保留任何 reachable `UnsupportedOperationException`。

#### External D - player-anchor（5 New）

**Observations**

- D 固定日志真实 EOF `:6946-6998`；5 个 New 的行数和 SHA-256 与 `:6951-6955` 当前逐项一致，写入时段
  `23:57:13-23:59:24`。player-anchor mechanics SHA-256 仍为
  `0a3f6b5fd5e09b1b8728f25af3f3c1de808aea5e7fcc9799ad7b0ba94f09963a`、写时 `22:18:02`；Cloud
  `NpcClickService` 与 shared 12 均无本 cohort 时段写入。
- command 的 rect、`prepareAlt4/skipDefaultMask` 和 positive-area gate；六个 terminal；CAPTURED/NO_PURPLE_BLOB
  evidence shape；blob bbox/anchor/darkPixels 20..6000；inclusive blob 对 exclusive scan rect 的严格 containment，
  均在 Cloud/DHXY result 的结构字段上对应 released mechanics。Cloud/DHXY 两 result 都 clone 两份 byte[]，未加入
  identity/OCR/provider/map formula/click/verify/fallback 字段。

**Risks**

- 这 5 个文件虽然没有改 shared 12，却已直接依赖尚未落盘的 shared API：Cloud command
  `implements LocalMacroCommand` 但不在 sealed permits；DHXY 两 payload `implements RemoteLocalMacro*Payload` 但不在
  permits；两侧 enum 均没有 `NPC_PLAYER_ANCHOR`；Cloud `LocalMacroOutcome` 也没有 `npcPlayerAnchor()` slot。当前源码会在
  unified integration 之前形成确定的符号/封闭类型编译缺口，和 A/C 的 standalone contract 形态不一致。
- `CloudNpcPlayerAnchorPort` 还直接 import `com.bot.dhxy.service.npc.NpcClickPlayerAnchorLocalObservationMechanics`
  及其 nested `Result/ImageEvidence/PurpleBlob/Terminal`，但该 local mechanics 只存在 DHXY，不存在 Cloud source tree；
  port 因此不只是等待 kind/permit，还跨仓依赖本地实现类型。integration 需要改成 Cloud 自有 typed result/consumer 映射，
  不能把 DHXY mechanics 类复制到 Cloud 来消除编译错误。
- D result/payload constructor 与 C 同类：结构上核 bytes/hash/dims/rect，但不自行解码 PNG或重算 SHA；port 试图用
  DHXY local `ImageEvidence` 再验正是上述跨仓缺符号来源。父级需同时保留完整 PNG invariant并移除实现类型耦合。
- port 固定 `phaseCode="npc"`、`actionSlot="playerAnchor"`、timeout=120000，并把 `NOT_EXECUTED` 映成
  `CAPTURE_UNAVAILABLE`。这些不是纯字段镜像；shared integration 前须对照 696 caller 与既有 retained-action/terminal
  约定，避免把 transport negative 折成新的 business/mechanical truth。

**Parent-checklist**

- 先决定 D 五文件在 shared wave 前应保持 standalone，或由同一 shared integrator 原子补齐所有 kind/permits/outcome/
  codec/digest/handler；不得留下半注册 sealed 类型。
- 移除 Cloud 对 DHXY-only mechanics nested types 的源码依赖，同时保留 PNG magic/decode/actual dims/SHA、blob 与 Result
  全 invariant；核对 `NOT_EXECUTED/STOPPED/UNKNOWN` 映射和 retained phase/action/timeout 后再接 `NpcClickService` caller。

以上仅为 A/C/D 非绑定 Observations/Risks/Parent-checklist。除本条追加外未修改 Java、构建、测试、runtime、
External 固定日志、主计划、迁移矩阵、CR 或 Git；未运行 build/test/runtime/Git。

## 2026-07-15 A/C/D R1 Delivery Preflight（非绑定）

### Observations

- 范围：仅核对 A/C/D R1 指定的 10 个 contract/port/payload 文件与父级静态结论；本条不替代 reviewer 结论。
- A：Cloud `NpcTaskTooltipMacroCommand` 与 DHXY `RemoteNpcTaskTooltipMacroCommandPayload` 均保留 `templatePath` 原值；`regions == null` 均归一为有序空列表，否则防御性复制。两侧 `ScanRegion(x1,y1,x2,y2)` 形状一致。
- C：`CloudNpcYellowTargetPort` 保持纯接口，方法形状为 caller-supplied `phaseCode`、`actionSlot`、typed command，返回 typed result；未在 port 内引入 bean、transport、retry 或业务判定。
- C：Cloud/DHXY yellow-target result 的 terminal、candidate、raw/mask evidence、SHA、尺寸与 scan rect 字段形状对称；evidence terminal 与 non-evidence terminal 的承载边界对称。
- D：command/result/port 均为 standalone contract；port 保持纯接口。Cloud 与 DHXY result 两侧均执行 8-byte PNG magic、ImageIO decode、decoded dimensions、SHA-256 校验，并在 `finally` 中 flush decoded image。

### Risks

- 写集边界：本轮 10 个文件均属于 standalone contract cohort；若交付包额外改动 shared enum/permit/request/outcome/envelope/codec/digest/handler、Spring wiring、runtime 或 build/test 文件，应视为写集越界并单独核对。
- 编译符号：C port 依赖 `NpcYellowTargetMacroCommand`，D port 依赖 `NpcPlayerAnchorMacroCommand`/`NpcPlayerAnchorMacroResult`；A/C/D 的 shared transport wiring 明确尚未落入本 cohort。交付前应以两项目当前源码编译证据确认符号可见性，旧 runtime 日志不能替代该证据。
- C strict-PNG P2 风险：两侧 `verifyEvidencePng` 仅有 ImageIO decode、decoded dimensions 与 SHA 校验，没有 8-byte PNG magic；ImageIO 可解码的非 PNG 字节仍可能通过，与任务要求的 strict PNG 不完全闭合。
- PNG owner/invariant：C/D 都由 result contract 构造阶段校验并防御性持有 raw/mask bytes；D strict-PNG invariant 两侧闭合，C 的 owner 对称但 strict-PNG invariant 仍缺 magic gate。后续 shared codec/handler 不应绕过构造校验或另造一套漂移规则。
- 接口形状：A/C/D 当前均把 shared transport integration 留作下游；若 Parent Review 期待本 R1 已可经 generic LOCAL_MACRO 实际 dispatch，则 standalone/纯接口形状仍存在集成缺口；若 Parent 范围仅为 contract cohort，则该缺口属于明确 deferred seam。

### Parent-checklist

- [x] A：`templatePath` 原值保留；`regions null -> empty`；顺序与防御性复制保持。
- [x] C：port 为纯接口，caller 提供 retained-action address，Cloud 保留业务决策。
- [ ] C：补齐或明确豁免 8-byte PNG magic，消除 strict-PNG P2 风险。
- [x] D：standalone command/result、纯接口 port；Cloud/DHXY 双侧 PNG magic/decode/dims/SHA/`finally flush` 对称。
- [ ] Delivery：确认最终写集仅含批准 cohort 文件，并提供当前 DHXY 与 cloud-brain 编译符号证据。
