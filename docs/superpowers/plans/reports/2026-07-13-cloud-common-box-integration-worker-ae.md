# CommonBoxService Cloud Integration - Internal Worker AE

Append-only coordination log. Parent thread is the only reviewer/approver. Worker AE only designs or implements the
explicitly published task and never reviews another Worker.

## Parent Task Brief #1 - `W-CBOX-1-D1` - 2026-07-13T17:46:00-04:00

### Goal

Produce one implementation-ready design for migrating the business orchestration of committed DHXY HEAD
`0114604e` `CommonBoxService` to Cloud while permanently retaining local exact-window capture, template match,
coordinate conversion, input queue execution, and input safety. This is the integration wave after the already
`SOURCE APPROVED` dormant leaves:

- Cloud `com.bot.dhxy.config.CloudCommonBoxProperties`
- Cloud `com.yueyunfe.dhxy.cloudbrain.remote.CommonBoxStateGovernor`

### Baseline and required reads

Read in full before writing the Delta:

1. `D:\mavenProject\DHXY\AGENTS.md`
2. `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
3. `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md` top CR271 slice
4. `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-service-migration-matrix.md`
5. committed `0114604e:src/main/java/com/bot/dhxy/service/CommonBoxService.java`
6. current Cloud `CloudCommonBoxProperties.java`, `CommonBoxStateGovernor.java`,
   `CloudTaskServiceExecutionContext.java`, `CloudTaskServicePort.java`, and authority assembly
7. prior fixed report `2026-07-13-cloud-common-box-service-worker-a.md`, especially the final SOURCE APPROVED block

The local source is scoped-clean at dispatch. Protect every dirty/untracked file in both repositories.

### Design must close

1. Exact split of baseline methods `detectLeaderBoxAfterReturnHome`, `detectMemberBoxAfterCombatExit`,
   `consumePendingBoxIfAllowed`, `hasPendingBoxForCurrentWindow`, `clearPendingForRole` into Cloud business authority
   and local retained mechanics. Preserve the existing 30-second pending expiry, role config defaults, stale
   window/identity/taskRun gates, click-failure retention, and asynchronous detect ordering. No business difference.
2. One closed typed operation/facade for the local whole observation and one for consume-click, or one justified
   RX3 transaction if capture+match+click must remain atomic. No raw request/poll/outcome APIs, no caller-minted IDs,
   no second ledger/registry/thread/queue.
3. Exact retained identities and state transitions from governor ticket/reservation through mechanical outcome and
   real final-consumed receipt compaction. UNKNOWN/STOPPED/ACK-loss must never mint a replacement click or advance
   pending state.
4. Exact current scope/taskRun/window tuple/stopEpoch/runRevision fences at Cloud enqueue/final dispatch/local
   pre-side-effect. Resume and terminal handling must reuse current retained state without stale mutation.
5. Exact file table with New/Modify/0, package visibility, constructor ownership, public business API signatures,
   shared schema/digest changes, dependency order, and a first 1-3 file implementation slice that does not touch
   AB's in-flight RX3 Java write set.
6. Capacity/retirement wiring for the existing governor, and the sole composition-root path. Host/Task/caller remains
   dormant; no activation or production switch.

### Hard boundaries

- Design-only: Java/Maven/schema/resources/tests/CR/dashboard are frozen.
- Unique write set is only this append-only report.
- Do not propose moving local capture/template/OCR/pathing/UICleaner/input mechanics to Cloud.
- Do not add TTL beyond the committed CommonBox 30-second pending expiry, retry, fallback, new thread, poller, LRU,
  takeover, or wrapper chains.
- Do not inspect or modify A/B/C/D/AB reports except read-only dependency facts. Do not run Maven or Git mutation.

### Delivery

First append `CLAIMED` with task, claimedAt, and uniqueWriteSet. Then append `Internal Worker AE - Design #1` with
invariants, exact state/identity/late-outcome matrix, API signatures, file/DAG table, and self-QA. Self-QA is not
parent approval.

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source + Design Review #3 - STATIC PASS / D3 DESIGN APPROVED / BUILD PENDING - 2026-07-13T19:14:30-04:00

父级逐行复核 `CommonBoxLocalObservationMechanics.java`、`0114604e` 的 `CommonBoxService`、当前
`WindowNativeBinding`/`BoundWindowCaptureService`/`ImageFinder`，并独立复算 SHA-256：
`7E9F09084495DFA71D83C516EC321E11B77890E902780148626E90A6C540DAFD`。静态源码与批准 Slice 1 一致：
exact HWND binding 七参数 capture、固定 ROI/模板/0.86、WINDOW_CLIENT_PX center、success-only template cache、capture frame
单次 flush、closed null matrix，且无 input/线程/磁盘输出/caller 接线。**静态 P0=0/P1=0/P2=0**。

D3 三项同时 **DESIGN APPROVED，P0=0/P1=0/P2=0**：first UNKNOWN ACK 成功后才以同一 occurrence 启动一次
memoized common-pool completion；late final 保持原 request/action/address/digest且不建 retry queue；Cloud final READY 只重用
现有 route wake，poll 在 wait/IDLE/new command 前优先 claim，同 poll token 防 busy-spin；30 秒唯一 deadline 从 Cloud
governor successful commit 的 injected `Clock` 读取，client matched time仅诊断，不能影响 prune/order/expiry。

本结论**不是最终 SOURCE/BUILD APPROVED**：按 Parent Review #2 明确门禁，AB 共享 Java 稳定后仍须由父级执行
DHXY `mvn -q -DskipTests compile`。编译通过后才把 Slice 1 标为 FINAL APPROVED；在此之前 Slice 2-5 仍不得落码。
Internal AE 当前无返修任务，可关闭会话释放内部槽位；后续实施由父级另行发单，不从本结论推断 activation。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

### Internal Worker AE - DELIVERED_FOR_PARENT_REVIEW - 2026-07-13T18:46:15.283-04:00

- task: `W-CBOX-1-D2`
- deliverable: `Design Repair #1 Delta`
- closure: `P1-1 / P1-2 / P1-3 / P1-4 / P2-1 / P2-2` 已逐项给出增量闭合设计
- status: `DELIVERED_FOR_PARENT_REVIEW`
- approval: 无；Internal Worker AE 不承担 review/approval，任何 self-QA 均不算批准
- frozen: Java/Maven/schema/resources/host/caller 均未修改，未运行 Maven，未执行 Git mutation
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-common-box-integration-worker-ae.md`

## Parent Design Review #2 - PARTIAL DESIGN APPROVED / BLOCKED / `W-CBOX-1-S1-IMP1+D3` - 2026-07-13T18:57:26-04:00

父级已对照 committed `0114604e:CommonBoxService`、当前 `BoundWindowCaptureService`、
`RemoteCommandPollingLoop`、Cloud broker/retained/final-consumption 源码复审 D2。结论：首个 local mechanics
切片 **DESIGN APPROVED**；完整集成仍 `P0=0 / P1=2 / P2=1`，不得进入 Slice 2-5。

### 已批准立即实施：Slice 1（1 New）

唯一 Java 写集：
`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\commonbox\CommonBoxLocalObservationMechanics.java`。

实现必须逐字遵守 D2 的 constructor/public `observe`/nested `Status`/`ObservationResult` 合同，以及：

- exact `WindowNativeBinding` + `BoundWindowCaptureService.captureRegion(binding, baseX, baseY,
  baseX+623, baseY+590, baseX+682, baseY+618)`；该 7 参数签名已由父级核实；
- fixed `images/template/common/leader_box_marker.png`、threshold `0.86`、lazy success-only cache；
- `MATCHED` 返回 `WINDOW_CLIENT_PX = 623/590 + rounded match center`，非 match/failure 严格 null matrix；
- capture frame 恰好一次 `flush()`，cached template 不 flush；不写盘、不上传图片、不发送输入、不新增 thread/
  executor/queue/wrapper；main method JavaDoc 写清坐标空间、nullability 与 exact-binding 前提。

AE 可同时在本日志追加 D3。AB 正连续写共享 Java，因此本切片落码后先做源码自审并报告，**暂不并发运行
Maven**；父级在 AB 稳定后统一执行 DHXY compile。未过该 compile 前不作 SOURCE APPROVED。

### P1-1 - Cloud 多 occurrence 没有恢复本地 committed async execution ordering

- 证据：当前 `RemoteCommandPollingLoop.java:173-181` 是单线同步
  `handler.handle(command) -> transport.submitOutcome(outcome)`；D2 Slice 2 文件表没有 local async completion
  owner/outcome publication seam。Cloud ledger 允许多个 occurrence 只会让命令排队，local capture+match 仍按 poll
  FIFO 串行，不能产生 committed `CompletableFuture.runAsync` 的独立在途/实际完成顺序。
- 影响：第二个显式 milestone 的 capture 时刻和 last-completion-wins 次序被 transport FIFO 改写；D2 的
  `finalAcceptanceSequence` 只能排序已到 Cloud 的 final，不能补回从未并发执行的 local observation。
- D3 条件：给出可编译的 local retained async completion 合同，复用 baseline async 边界且不新建专用线程/
  executor/第二 ledger/第二 queue；每次 occurrence 只执行一次，完成后以 exact same request/action/digest 发布 late
  outcome。若决定串行化，必须明确列为业务差异并等待用户批准，不能称基线等价。

### P1-2 - notification failure 的现有 poll 驱动缺少确定唤醒与优先级

- 证据：D2 只写 `IN_DELIVERY -> READY` 并称“下一次现有 poll/outcome/receipt 驱动”。当前本地 outcome 在
  `RemoteCommandPollingLoop:180` 只提交一次；若 broker 已 ACK final 而 sink 失败，必须保证随后 poll 一定看到该
  READY，不能靠恰好有下一条 command/duplicate outcome。
- 影响：最后一条命令或长 idle 时，retained final 可永久停在 READY，governor/final-consumed 不落地且容量不释放。
- D3 条件：写清 `finishNotificationLocked(failure)` 必须 re-arm existing route wake；`poll` 在等待/返回 IDLE/
  选择新 command 前优先 claim READY notification，锁外执行同 token，同步完成后再继续 poll；sink failure 不向
  local 返回会终止 polling loop 的 transport exception，也不 redispatch mechanics。列出 first outcome ACK、sink、
  poll drive 的精确顺序。

### P2-1 - 30 秒时钟权威仍需在 Slice 4 前闭合

D2 把 local `matchedAtEpochMs` 直接送入 Cloud governor，但 client/cloud 可处于不同时钟。D3 需明确 TTL 的
authoritative clock与 skew/future timestamp 处理，禁止 client timestamp 反向 prune 其它 pending；不得新增 TTL，
也不得默默把 30 秒改成另一时长。此项不阻塞 Slice 1。

### 下一动作

Internal AE 现在执行 `W-CBOX-1-S1-IMP1+D3`：落上述单文件并追加 Design Repair #2 Delta；其它 Java/schema/
resources/host/caller/tests 与 AB/A/B/C/D 写集冻结。AE self-QA 不算批准，父级收到新材料后复审。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Design Review #1 - BLOCKED / `W-CBOX-1-D2` - 2026-07-13T18:31:00-04:00

父级已独立复核 committed `0114604e:CommonBoxService.java`、当前
`CommonBoxStateGovernor`、`RemoteGameClientPort`、`CloudTaskRunCommandExecutor` 与本 Design #1。
本结论是唯一 review/approval；Worker self-QA 不算批准。

### 结论

- `P0=0 / P1=4 / P2=2`
- **BLOCKED；Java/Maven/schema/resources/host/caller 继续冻结。**
- 正确且不要求重开的部分：本地永久保留 HWND capture/template match/坐标与 input safety；Cloud
  只持业务 pending/config；两个 detect 与后续 consume 不合并成一次 RX3；30 秒从 local match
  wall-clock 起算；图片不上云；现有 governor 的 tenant/incarnation/capacity/claim 基础边界。

### P1-1 - 显式 detect 调用被错误合并，破坏 committed 异步调用/完成顺序

**证据：** committed 基线每次通过 cheap gate 的调用都在 `CommonBoxService:273-281` 新建一个独立
`CompletableFuture.runAsync`，并由每个 completion 在 `:340` 对同 key 执行 `put`；因此两个显式
milestone 调用可同时在途，谁后完成谁最后写入 pending。Design #1 `§4` 第 222/229 行却规定每 role
只有一个 open occurrence，前一个 UNKNOWN/ACK 未压缩时重复业务调用只重用旧 request；`§5`
第 237 行也把下一次真实 detect 延后到 compaction 后。

**影响：** 第二个真实 return-home/combat-exit milestone 可能不截图、不匹配，甚至被第一个旧帧的 late
结果代表；这既不是 transport dedupe，也不是基线等价迁移。

**返修条件：** Delta 必须为**每一次通过同步 cheap gate 的显式 detect 调用**分配独立 retained
observation occurrence，并允许多个 occurrence 同时 in-flight；每个只执行一次 local observation，按实际
completion 顺序调用 governor，后完成的 COMMITTED 结果保持基线 last-completion-wins。不得新增第二
ledger/queue/thread/map；若现有 retained ledger 无法并存多个 open occurrence，必须列出精确扩展及有界
容量，而不是静默 coalesce/reuse。

### P1-2 - `RetainedOutcomeSink` 尚无 exactly-once 状态机，可能重复 mutation/final-consume

**证据：** Design #1 第 203-213 行只说 sink 挂在 `PendingCommand` 并在锁外调用；第 233-242 行同时
要求 first UNKNOWN、late non-UNKNOWN、duplicate outcome、ACK 丢失。当前 executor 是同步
`broker.* -> actionLedger.recordOutcome`，没有所述 overload/sink；报告未给 `PendingCommand` 的新增字段、
状态迁移、哪一处先持久化 final、哪一处只通知一次、duplicate/late race 的 CAS/锁内判定，以及 sink
异常后的 retained 状态。

**影响：** first reply 与 late-resolution 路径可对同 ticket 重复 `commitObservation`，或重复
`settleConsume`/`consumeFinal`；也可能先回调再留下 broker/ledger 不一致状态。

**返修条件：** 给出真实类/方法级单写者表：`PendingCommand` 在 broker 锁内从哪个状态到哪个状态、
何时记录 first UNKNOWN、何时接受唯一 late final、何时把一次性 notification token 标为已交付；所有
sink 在锁外调用；duplicate ACK/outcome 只重送 retained envelope，绝不再次触发 governor。明确 sink
抛异常后的 ledger/broker/receipt 可恢复状态和固定锁序，禁止 reentrant broker/ledger deadlock。

### P1-3 - “旧 revision 可收尾”与“current fence 才能 mutation”没有可实现的双通道 API

**证据：** Design #1 第 273-276 行提出不存在于当前源码的 `requireRetainedFinal(...)`，但没有说明它
如何在 current execution gate 已拒绝旧 context 后取得 exact old identity/final；第 235 行又要求用
“最新 current Fence”执行 old ticket commit。第 280-288 行只给概念锁序，没有指出 current runtime
读取、terminal 标记、ledger finalization 与 governor mutation 的原子边界。

**影响：** 若复用 ACTIVE gate，旧 final 永远无法压缩；若放宽 gate 又把它当 mutation authority，旧
revision outcome 可污染新 runtime。两者都违反现有 retained-authority 合同。

**返修条件：** Delta 必须拆成两个闭合步骤并列精确签名：A) 只按 old retained identity 接受/记录唯一
final、允许 receipt compaction，绝不授予 side effect；B) 单独从 assembly current-slot 取 current
CommonBox fence/terminal snapshot，再以 original ticket + current snapshot 调 governor，stale/terminal
只能零写入。说明二者在 pause/resume/terminal/slot replacement 并发下的锁序和失败返回。

### P1-4 - 首个“1 文件”切片不是可编译合同

**证据：** Design #1 第 362-366 行只点名
`CommonBoxLocalObservationMechanics.java`，但全文没有该类 constructor/public method/return type 的精确
签名；wire DTO `RemoteCommonBoxObservationFact` 又被安排到后续 Slice 2。无法判断这个单文件是返回
private result、现有 `WindowFactOutcome`，还是引用尚不存在的 DTO。

**影响：** Worker 无法在不自行发明 API/额外文件的情况下完成 mandatory DHXY compile；首切片也
无法被 handler 稳定复用。

**返修条件：** 给出首切片完整 constructor + 唯一主方法签名 + immutable result 类型归属。若 result
必须是新 DTO，则首切片扩为 2 文件并在表中写清；若嵌套类型，则说明 visibility 和后续 handler 如何
无 wrapper nesting 地消费。列出异常/null/资源释放结果矩阵。

### P2-1 - 本地 matcher 细节未闭合 committed 资源/坐标合同

**证据：** committed 基线 `:352-368` 是单实例 lazy cached template，`:299-303` 传 screen-absolute ROI，
`:347-349` 总是 flush frame。Design #1 第 104-110 行只写“读取模板/窗口相对 ROI”，未写 cache owner、
`BoundWindowCaptureService.captureRegion(binding, baseX, baseY, absX1, absY1, absX2, absY2)` 的精确实参、
frame flush owner，也未定义 `ImageFinder` 抛错与 corrupt template 的 typed result。

**返修条件：** 在首切片签名旁固定 lazy cache 单一 owner、absolute capture 参数、client point 计算、
所有 frame flush 路径和 closed failure mapping；不写临时文件、不上传图像。

### P2-2 - dormant 波次不应提前修改 `CloudServiceHost`

**证据：** Task Brief 要求 host/Task/caller dormant；Design #1 文件表第 327 行却把 future scope-retirement
hook 直接列为本波 `CloudServiceHost Modify`，而第 298 行又声明当前无 activation/caller。

**影响：** 扩大当前共享写集并提前改变 host close lifecycle，且无法在 dormant 波验证真实 owner 顺序。

**返修条件：** W-CBOX-1 将 `CloudServiceHost` 保持 `0`；可保留 package-private scope-retirement
capability/assembly owner及容量释放合同，但实际 host close 接线移到独立 activation cohort。更新文件表与 DAG。

### 下一任务

Internal Worker AE 只追加 `Design Repair #1 Delta`，逐项关闭 P1-1..P1-4/P2-1..P2-2；不得重写历史，
不得改 Java/Maven/schema/resources/其它报告，不运行 Maven/Git mutation。修复后由父级复审。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal Worker AE - CLAIMED - 2026-07-13T17:54:15.237-04:00

- task: `W-CBOX-1-D1`
- claimedAt: `2026-07-13T17:54:15.237-04:00`
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-common-box-integration-worker-ae.md`

## Internal Worker AE - Design #1 - 2026-07-13T18:17:58.508-04:00

### 0. 范围、证据与基线

- 角色边界：本节仅给出实现设计，不是 reviewer/approval。除本报告外未写任何文件；未运行 Maven、应用、Task、UI、截图、输入或 Git mutation。
- 已核基线：DHXY committed `0114604e` 的 `CommonBoxService` 五个 public 方法、当前 Cloud `CloudCommonBoxProperties` / `CommonBoxStateGovernor` 两个 SOURCE APPROVED dormant leaf、`CloudTaskServiceExecutionContext`、`CloudTaskServicePort`、`CloudTaskRunAuthorityAssembly`、现有 retained ledger / broker / real final-consumed transaction，以及 prior CommonBox report 最终边界。
- 已核 `docs/业务逻辑.md` 的“通用盒子逻辑”：UI 默认 leader=true/member=false 且互不影响；ROI `(623,590)-(682,618)`；队长回程确认后后台检测、下轮点接任务 NPC 前消费；队员退战后后台检测并遵守既有 task-turn；pending 从识别成功起 30 秒；盒子优先于其它 opportunistic maintenance；不改变五倍/修罗接任务、导航、入战、战斗等待、回程和失败恢复。
- 当前 Cloud 两个 leaf 的批准范围不外推：本设计不会把尚未批准的 adapter/service/assembly/caller/host 说成已实现或已通过。
- 直接结论：不使用 capture+match+click RX3 transaction。基线明确要求“检测只写 pending，后续安全点才消费”，把三者合并会改变业务时机。

### 1. 唯一职责切分

| 基线入口 | Cloud 唯一业务权威 | DHXY 永久保留 mechanics | 返回/时序等价 |
|---|---|---|---|
| `detectLeaderBoxAfterReturnHome(context, sourceTask, source)` | 规范化 task、核 leader role/config、清理 exact stale pending、向 governor 取 `DetectTicket`、登记一个 retained async observation | exact HWND ROI capture、读取本地模板、`ImageFinder.find(...,0.86)`、计算 match/client point、回传 typed fact | cheap business gates 同步完成；只登记既有 broker work 后立即返回；capture+match 不阻塞调用点 |
| `detectMemberBoxAfterCombatExit(...)` | 同上，固定 expected role=`MEMBER` | 同一 local whole observation，不复制第二套算法 | 与 leader 共用 mechanics；pending 仍按 exact window/run/role/task 隔离 |
| `consumePendingBoxIfAllowed(...)` | stop checkpoint、task/role/config/fence、governor atomic reserve、固定 consume action identity、final settlement、boolean 解释 | local pre-side-effect triple fence、`WINDOW_CLIENT_PX` 转当前 screen point、现有全局 input queue 原子执行 move+sleep+click | `EXECUTED` 才 true；可信 `NOT_EXECUTED` 保留 pending；uncertain 不重发点击 |
| `hasPendingBoxForCurrentWindow(...)` | governor 对 exact current fence 的只读 `hasPending` | 无 | 不发 capture/input，不 claim pending |
| `clearPendingForRole(role, source)` | scope-bound governor role clear；toggle-off 仍由 `applyToggleOverride` 在同锁 revision++ + clear | 无 | null role 仍 no-op；leader/member 互不影响 |

DHXY 不再拥有 `pendingByKey`、role/task/config decision 或 TTL。Cloud 不接收图片、不加载模板、不做模板匹配、不做坐标换算、不直接操作 input queue。

### 2. 两个闭合 typed mechanics facade

#### 2.1 Local whole observation

复用 wire operation `WINDOW_FACT`，只新增 enum variant `COMMON_BOX_OBSERVATION`。请求 payload 仍只有 fixed `factKind`，因此 Cloud 不能下发 ROI、模板路径、threshold、算法、坐标空间或任意参数。

DHXY 新建 `CommonBoxLocalObservationMechanics`，一次调用内固定完成：

1. 对 handler 已通过 exact registration + exact binding gate 的 `WindowRuntimeContext/WindowNativeBinding`，用 `BoundWindowCaptureService` 截取窗口相对 `(623,590,59,28)`。
2. 只读取本地 `images/template/common/leader_box_marker.png`，threshold 固定 `0.86`；不写临时文件，不上传 PNG。
3. 命中点严格为 `clientX=623+round(match[0])`、`clientY=590+round(match[1])`，不除 `systemScaleRatio`，不由 Cloud 重算。
4. 返回 closed status：`MATCHED`、`NOT_MATCHED`、`CAPTURE_UNAVAILABLE`、`TEMPLATE_UNAVAILABLE`、`MECHANICS_FAILED`。只有 `MATCHED` 携带 point、score、`matchedAtEpochMs`；所有状态都携带 exact `ObservedWindowBinding`。
5. capture/template/exception negative 仍是一次已完成的本地观察事实，不伪装成业务 match；它们不触发 retry、fallback 或 pending。

下面图片是当前 retained **模板**，不是 live incident，也不是历史事故截图：

![通用盒子 marker 模板](D:/mavenProject/DHXY/images/template/common/leader_box_marker.png)

Cloud typed fact 形状固定为：

```java
public sealed interface WindowFact {
    record CommonBoxObservationFact(
            Status status,
            Integer clientX,
            Integer clientY,
            Double matchScore,
            Long matchedAtEpochMs,
            CoordinateSpace coordinateSpace,
            ObservedWindowBinding observedWindow) implements WindowFact {

        public enum Status {
            MATCHED,
            NOT_MATCHED,
            CAPTURE_UNAVAILABLE,
            TEMPLATE_UNAVAILABLE,
            MECHANICS_FAILED
        }
    }
}
```

`MATCHED` 强制 `coordinateSpace=WINDOW_CLIENT_PX`、非空 point、finite score 且 `score>=0.86`、正 `matchedAtEpochMs`；非 MATCHED 强制这些 match 字段全 null。`observedWindow` 必须与 request window 四元组逐字段一致，否则 broker 拒绝 outcome，governor 零写入。

#### 2.2 Consume click

不新增 raw `COMMON_BOX_CLICK` operation。`CloudCommonBoxRunCapability` 是唯一能取得 fixed consume handle 的 facade，并在内部固定构造一个既有 `EXECUTE_INPUT_BUNDLE`：

```java
List<InputActionDto> actions = List.of(
        new InputActionDto(MOVE_MOUSE, x, y, null, null, null, null, null, null),
        new InputActionDto(SLEEP, null, null, null, null, 80, null, null, null),
        new InputActionDto(CLICK_LEFT, x, y, null, null, 120, null, null, null)
);
```

- `coordinateSpace=WINDOW_CLIENT_PX`；local handler 在 input worker admission 前用当前 exact binding 转 screen absolute，并再次校验 taskRun/window/stopEpoch/runRevision。
- 三个动作是一份不可拆分的 queue request，保持 committed `moveAndClickLeft(...,80,120)` 的 move/click 原子性。
- public business API 不暴露 action handle、坐标、action list、timeout、request/outcome envelope 或 renewal API。
- port 所需 operation deadline 只复用 Full R0 既有 transport deadline 合同，不写入 governor、不暂停/重置 30 秒 pending、不产生业务 retry/fallback，也不是第二个 CommonBox TTL。

### 3. 可编译 public business API 与 constructor ownership

Cloud `com.bot.dhxy.service.CommonBoxService` 是 **per-runtime 普通 final 对象，不是 scope-less Spring singleton**。这样无参 `clearPendingForRole` 仍能安全绑定 exact `CloudServiceScope`，同时保留五个 baseline 方法签名。

```java
public final class CommonBoxService {
    public CommonBoxService(
            TaskExecutionContext boundContext,
            CloudCommonBoxRunCapability capability);

    public void detectLeaderBoxAfterReturnHome(
            TaskExecutionContext context, String sourceTask, String source);

    public void detectMemberBoxAfterCombatExit(
            TaskExecutionContext context, String sourceTask, String source);

    public boolean consumePendingBoxIfAllowed(
            TaskExecutionContext context, String sourceTask, String source);

    public boolean hasPendingBoxForCurrentWindow(
            TaskExecutionContext context, String sourceTask);

    public void clearPendingForRole(CommonBoxRole role, String source);
}
```

- `CloudTaskRunAuthorityAssembly` 是唯一 constructor owner：initial runtime 创建 capability + service；resume 创建新的 revision-bound capability + service，但复用同一个 retained CommonBox workflow 和现有 `CloudTaskRetainedActionState`。
- `CommonBoxService` 要求传入的 `TaskExecutionContext` 与 constructor 的 `boundContext` 为同一 runtime 对象，并调用现有 checkpoint/revalidation；旧 runtime service 不能被新 revision 复用。
- `CloudCommonBoxRunCapability` 是 public final 仅为跨 package 编译，constructor package-private；没有 factory、ID mint、raw port getter 或 arbitrary action API。只有 assembly 能构造真实 capability。
- `CloudTaskServiceExecutionContext`、`TaskExecutionContext.getRemoteGameClient()` 不增加 CommonBox raw surface；业务 service 也不从 public port 自行拼 handle。

capability 只暴露以下 closed domain methods，内部类型/handle/state 都不可见：

```java
public final class CloudCommonBoxRunCapability {
    public CloudCommonBoxProperties properties();
    public void beginObservation(CommonBoxRole role, String canonicalTask, String source);
    public boolean consumePending(CommonBoxRole role, String canonicalTask, String source);
    public boolean hasPending(CommonBoxRole role, String canonicalTask);
    public void clearExactPending(CommonBoxRole role, String canonicalTask, String source);
    public void clearPendingForRole(CommonBoxRole role, String source);
}
```

`beginObservation` 是登记式非阻塞调用。为此只给 package-private retained plumbing 增加：

```java
void beginWindowFact(
        WindowFactAction action,
        WindowFactKind factKind,
        long transportTimeoutMs,
        RetainedOutcomeSink<WindowFactOutcome> sink);
```

`RetainedOutcomeSink` 作为一个字段挂在现有 broker `PendingCommand` 上，不建立 map/registry。broker 在现有 poll/outcome/late-outcome 调用线程、释放 broker state lock 后调用 sink；不使用 `thenAcceptAsync`，不创建 executor/thread/queue。sink 先让现有 ledger 记录 exact outcome，再对 non-UNKNOWN 调现有 `RemoteFinalConsumptionCoordinator.consumeFinal(...)`。

### 4. Exact retained identity

| 层 | exact identity | mint/owner | 禁止事项 |
|---|---|---|---|
| Run | `RemoteTaskRunScope + taskRunId + taskType + windowId/nativeHandle/processId/playerIdentityEpoch + stopEpoch` | coordinator/assembly | 不用 ThreadLocal/current-run/title search |
| Fence | 上述 stable run + `runRevision` | 当前 `CloudTaskRunExecutionContext` 投影 | 不接受旧 revision/window/identity/session |
| Governor ticket | `scope + role + canonicalTask + captured Fence + tenant incarnation + configRevision` | `beginObservation` | local/caller 不 mint，不从 outcome 重建 |
| Observe address | `phaseCode=common-box`, slot=`observe-leader` 或 `observe-member`, sequential occurrence, attempt=`0` | existing retained action state | observation 无 renewal；UNKNOWN/STOPPED 不建 replacement |
| Pending | full approved `PendingKey` + entry Fence + tenant incarnation + generation + configRevision + client point + matched time/expiry | governor final mutation | DHXY 无 mirror pending；runRevision 不进 key但必须等于 entry Fence |
| Consume reservation | `scope + PendingKey + incarnation + generation + configRevision + exact click/template/matched fields` | governor atomic `UNCLAIMED -> CLAIMED` | 第二 caller 不能 claim；新 observation 不能覆盖 CLAIMED/SEALED |
| Consume address | `phaseCode=common-box`, slot=`consume-leader` 或 `consume-member`, sequential occurrence, attempt starts `0` | existing retained action state | `NOT_EXECUTED` 只可在 receipt compaction 后 attempt+1；不新建 occurrence |
| Wire | requestId/actionId/semanticAddress/requestDigest and exact request bytes | existing ledger | caller/local 不传业务 ID，不重建不同 bytes |
| Final | exact retained identity + same outcome object/digest + final-consumed ACK/receipt | existing final coordinator | 不建 CommonBox ACK 表、outbox、dedupe registry |

一个 role slot 同时只保留一个 open occurrence。正常 final-consumed receipt 后，下一次显式业务调用才让 existing retained state 推进 occurrence。若上一个 occurrence 仍 UNKNOWN、STOPPED 待处理或 ACK/receipt 未压缩，重复调用只重用 exact retained request，不铸造 replacement。这是 transport uncertainty 下的既有 Full R0 安全合同，不是自动 retry，也不合并两个业务调用。

### 5. Observation outcome / late-outcome / final-consumed 矩阵

| exact outcome | ledger | governor final mutation | disposition | 后续 |
|---|---|---|---|---|
| `OBSERVED + MATCHED` | 记录 exact final | 用 **最新 current Fence** 调 `commitObservation(ticket,currentFence,clientX,clientY,matchedAt)` | `OCCURRENCE_COMPLETE` | COMMITTED 才生成/更新 UNCLAIMED pending；busy/sealed/stale/disabled/capacity 均零写入 |
| `OBSERVED + NOT_MATCHED/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED` | 记录 exact final | 无 pending mutation，只写结构化诊断 | `OCCURRENCE_COMPLETE` | 不 retry、不 fallback、不把 mechanics negative 当业务命中 |
| `NOT_EXECUTED` | 记录 exact final | 无 pending mutation | `OCCURRENCE_COMPLETE` | observation 不 renewal；下一次真实 detect 在 compaction 后才是 occurrence+1 |
| `STOPPED` | 记录 exact final | 无 pending mutation | `OCCURRENCE_COMPLETE` | 不 replacement，按 task stop unwind |
| `UNKNOWN` | 保持 unresolved | **不调用** final mutation，不发 final-consumed ACK | 无 | 同 action/request 保留；任何新 detect 不 mint |
| UNKNOWN 后 exact late `OBSERVED/NOT_EXECUTED/STOPPED` | 只允许 broker 的一次 exact late non-UNKNOWN | 按对应行执行一次 | 按对应行 | 不重拍、不新 ticket |
| observedWindow/requestDigest/semantic identity 不匹配 | broker 拒绝 | 零写入 | 无 | local 只能重送同 outcome envelope，不能换 payload |
| ACK 丢失 | 已完成 mutation 的 detail 保留为 notice pending | mutation 不重跑 | 原 disposition 保留 | local poll 继续收到同 ACK；receipt 后才压缩/推进 occurrence |

TTL 使用 local match 时产生的 `matchedAtEpochMs`，governor 仍计算 `matchedAt+30_000`；Cloud 不在收到 outcome 时重置计时、不因 pause/resume 延长、不加 grace。current `commitObservation` 的 wall-clock/prune 语义保持不变。

### 6. Consume / late-outcome / click-failure 矩阵

| exact input outcome | governor | final-consumed | public result / replacement rule |
|---|---|---|---|
| `EXECUTED` 且三步全部完成、post-window exact | `settle(EXECUTED)` exact remove | `OCCURRENCE_COMPLETE` | 返回 true；ACK 丢失不再点击 |
| trusted `NOT_EXECUTED` 且 `startedStepIndex<0` | `settle(NOT_EXECUTED)` release 为 UNCLAIMED | `ATTEMPT_RETIRED_FOR_RENEWAL` | 返回 false；只有 final receipt 已压缩且下一次显式 consume 调用时，same occurrence `attempt+1` 再 reserve/执行 |
| `UNKNOWN` | reservation 保持 CLAIMED，不 settle | 不 final-consume | 返回 false/uncertain unwind；重复调用只查询同 request 的 late resolution，绝不发第二 click |
| UNKNOWN 后 late `EXECUTED` | exact remove | occurrence complete | 不补发 click；业务最终只消费一次 |
| UNKNOWN 后 late `NOT_EXECUTED` | release | renewal disposition | 仍须 receipt compaction + 下一次显式 consume 才允许 same occurrence next attempt |
| `STOPPED` | `settle(UNKNOWN_OR_STOPPED)` 置 SEALED | occurrence complete | false + stop unwind；TTL 前不可 reserve/overwrite |
| toggle/clear/terminal 在机械动作中使 reservation 失效 | settle no-op | exact action 仍完成 final-consumed | 不重建 pending；若 input 已真实 EXECUTED，仍不 replacement |
| ACK 丢失 | settlement 已发生一次 | exact ACK retained | 不以 pending release/remove 作为 mint 新 ID 的依据；只看 retained compaction frontier |

因此“click failed keep pending until TTL”仍成立：可信 pre-side-effect `NOT_EXECUTED` 保留并可由后续显式机会重试；uncertain/STOPPED 仍保留 entry 直至 30 秒 prune，但 claim/SEALED 阻止可能的重复物理点击。

### 7. Fence、resume、terminal 与并发顺序

#### 7.1 三重 fence

1. **Cloud enqueue**：capability 对 bound current context 做 execution-gate revalidation；governor ticket/reservation 使用同一 exact Fence；retained request 固化 scope/taskRun/window/stopEpoch/runRevision。
2. **broker final dispatch**：复用 coordinator `authorizeAndMarkDispatch`，同 request identity 与 current run revision 不匹配即 non-executed/stop outcome。
3. **DHXY pre-side-effect**：local handler 在 decode 后、queue submit 前、input-worker admission 前重读 exact registration + HWND binding + identity epoch + stop/revision；client coordinate 只在最后一道门后转 screen absolute。

observation 还在 capture 前和产出 fact 后各做一次 exact registration/binding gate；post-gate 的 `ObservedWindowBinding` 才进入 digest。

#### 7.2 Resume

- assembly resume 复用同一 `CloudTaskRetainedActionState` 和同一 fixed two-role CommonBox workflow；只发布新的 revision-bound capability/service。
- 旧 observation outcome 可被 ledger/final coordinator按**原 identity**收尾，但 mutation 必须读取 workflow 最新 current Fence。旧 ticket 与新 Fence 不等，`REJECTED_STALE`，不能写新 revision pending。
- 旧 revision pending 不改写、不刷新 TTL。`has/reserve` 对新 Fence 返回 false；新的 current observation 只可覆盖旧 `UNCLAIMED` generation，旧 `CLAIMED/SEALED` 保持到既有 30 秒 prune。
- across-revision finalization 使用 package-private `requireRetainedFinal(...)`：只验证 exact ledger owner/current handle/outcome，不重新授予机械 side effect；因此能压缩旧结果，但不能绕过 current execution gate再派发动作。

#### 7.3 Terminal

在现有 terminal transition owner 内固定顺序：

1. slot 关闭并阻止新 runtime publication；
2. CommonBox workflow 标 terminal，拒绝新 ticket/reservation/action；
3. `governor.removeRunPending(scope, taskRunId)` 只删 exact run pending，不删 scope config；
4. existing ledger `acceptTerminalRun`、final detail/route retirement 按当前 Full R0 顺序继续；
5. late exact outcome仍可 final-consume，mutation 看到 terminal 后仅收尾不重建 pending。

锁顺序固定为 authority transition lock -> CommonBox workflow lock -> governor lock；governor 从不持锁回调 port/broker，所有 capture/input/final transport 都在 governor 锁外。

### 8. Capacity、scope retirement 与 composition root

- governor 仍是 assembly 内唯一实例、唯一 lock、唯一 per-scope config/pending owner。
- assembly 精确注入 `maxTenantStates=1_000`、`maxPendingEntriesPerScope=1_000`，与现有 remote authority global-route / owner-retained-action hard-cap 数量级一致；满额保持现有 typed fail-closed，不 eviction/LRU/额外 TTL。
- CommonBox workflow 每 run 只有 leader/member 两个 observation slot + 两个 consume slot及固定状态字段，无 map、无第二 action registry。
- wire request、outcome、UNKNOWN、ACK、receipt 继续计入现有 ledger/broker cap；CommonBox 不建自己的 queue/outbox/history。
- `CloudCommonBoxScopeLifecycle` 是 assembly 构造的 non-mintable capability。future authenticated `CloudServiceHost` owner 必须按“先 terminal 该 scope 全部 run -> close host -> `retireScope(scope)`”调用；retire 原子释放 config+pending tenant slot并依靠 incarnation 防 ABA。普通 run terminal 绝不 retire scope/config。
- 唯一 composition path 保持 `CloudBrainServer -> RemoteTaskRunRoutes.create -> CloudTaskRunAuthorityAssembly.create`。`RemoteTaskRunRoutes` 只把同一 assembly 的 scope-lifecycle capability放入 `AuthorityRouteBundle`，不创建第二 governor。
- W-CBOX-1 落地后仍 dormant：不新增 HTTP raw endpoint，不注册 Task/caller/UI，不启动 poller/app，不切 production。后续 activation cohort 才能把已有本地 Runner milestone wake 与 per-runtime `CommonBoxService` 接上。

### 9. Shared schema / digest 结论

- contract version 保持当前值；`RemoteOperation` **0 修改**，因为复用 `WINDOW_FACT` 与 `EXECUTE_INPUT_BUNDLE`。
- request outer schema **0 修改**：`WindowFactRequest`/`RemoteWindowFactCommandPayload` 仍只有 `factKind`；新 enum 只有双方同时部署且 feature dormant 时才会出现。
- outcome outer schema **0 形状变化**：仍为 `{factKind,fact}`；只增加 strict nested `CommonBoxObservationFact` variant及 observed-window correlation。
- 两仓 `RemoteProtocolDigests` **0 修改**：现有 canonical JSON 已把 enum、nested fact、point、score、timestamp、observedWindow 纳入 request/outcome digest；没有 image bytes，也没有新的 digest 算法/版本。
- 没有独立 JSON schema 文件需要修改；Java strict DTO/enum 是当前共享 wire schema。Cloud/DHXY 必须同一原子波发布，单边代码不得激活该 fact kind。

### 10. 精确文件表

| 仓库 | 文件 | New/Modify/0 | visibility / constructor owner | 作用与边界 |
|---|---|---:|---|---|
| Cloud | `src/main/java/com/bot/dhxy/service/CommonBoxService.java` | New | public final；assembly per-runtime 构造 | 五个 baseline business API，task/role/config/boolean 解释 |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxRunCapability.java` | New | public final、package-private ctor；assembly owner | fixed slots、governor ticket/reservation、typed mechanics、late/final bridge；private fixed-role state置底 |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxScopeLifecycle.java` | New | public final、package-private ctor；assembly owner | exact `CloudServiceScope` host-close retirement；无 task API |
| Cloud | `.../remote/CommonBoxStateGovernor.java` | Modify | package-private；same assembly | 加 exact-key clear；reservation补 template/matched diagnostics；行为状态机/30s/cap/incarnation不改 |
| Cloud | `.../remote/WindowFactKind.java` | Modify | public enum | 加 `COMMON_BOX_OBSERVATION` |
| Cloud | `.../remote/WindowFact.java` | Modify | public sealed | 加 strict nested observation fact/status |
| Cloud | `.../remote/WindowFactOutcome.java` | Modify | public record | variant matching 增新 fact |
| Cloud | `.../remote/RemoteCommandOutcomeEnvelope.java` | Modify | public wire ingress | strict parse new fact variant |
| Cloud | `.../remote/RemoteGameCommandBroker.java` | Modify | package-private singleton | 既有 PendingCommand 挂一个 retained sink；锁外同步通知 first/UNKNOWN/late/duplicate/synthetic outcome；不加 queue/thread/map |
| Cloud | `.../remote/RemoteGameClientPort.java` | Modify | package-private interface | 增 package-private retained sink overload，无 public raw API |
| Cloud | `.../remote/CloudTaskRunCommandExecutor.java` | Modify | package-private singleton | 非阻塞 register WINDOW_FACT；sink 内 exact recordOutcome；input late outcome亦走同 identity |
| Cloud | `.../remote/CloudTaskServicePort.java` | Modify | public facade，新增方法 package-private | closed begin/final bridge及 across-revision result retirement；现有 public generic API不扩权 |
| Cloud | `.../remote/CloudTaskRetainedActionState.java` | Modify | package-private retained owner | fixed CommonBox addresses、same-occurrence consume renewal、`requireRetainedFinal`；不建第二 registry |
| Cloud | `.../remote/CloudTaskRunAuthorityAssembly.java` | Modify | sole composition root | governor、initial/resume workflow/service、terminal cleanup、caps、scope lifecycle |
| Cloud | `.../remote/RemoteTaskRunRoutes.java` | Modify | public bundle/private internals | 同一 assembly capability anchor；不挂 CommonBox HTTP endpoint |
| Cloud | `.../host/CloudServiceHost.java` | Modify | public lifecycle owner | future activation overload持有 scope lifecycle，close 时按 owner 顺序 retire；当前无调用即 dormant |
| Cloud | `.../config/CloudCommonBoxProperties.java` | 0 | existing public scoped view | 原样复用 |
| Cloud | `.../remote/CloudTaskServiceExecutionContext.java` | 0 | existing non-mintable context | 原 exact getters/servicePort/retained reuse足够，不加 raw CommonBox getter |
| Cloud | `.../remote/RemoteOperation.java` | 0 | existing public enum | 不加 CommonBox operation |
| Cloud | `.../remote/RemoteProtocolDigests.java` | 0 | existing canonical owner | 自动覆盖新 nested fact，无算法改动 |
| DHXY | `src/main/java/com/bot/dhxy/service/commonbox/CommonBoxLocalObservationMechanics.java` | New | public `@Service`；Spring constructor injection | local ROI/template/match/client-point whole observation；无 pending/role/task decision |
| DHXY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommonBoxObservationFact.java` | New | public immutable Lombok DTO | Cloud fact 的 strict local mirror |
| DHXY | `.../cloud/remote/RemoteWindowFactKind.java` | Modify | public enum | 加 `COMMON_BOX_OBSERVATION` |
| DHXY | `.../cloud/remote/LocalRemoteGameCommandHandler.java` | Modify | existing public final；显式 ctor注入 mechanics | window-fact switch 调 whole observation；pre/post exact fence；input path原样复用 |
| DHXY | `.../cloud/remote/RemoteOperationPayloadCodec.java` | 0 | existing strict codec | command payload仍只 factKind；Jackson enum即可 |
| DHXY | `.../cloud/remote/RemoteProtocolDigests.java` | 0 | existing canonical owner | nested fact自动入 digest |
| DHXY | `src/main/java/com/bot/dhxy/service/CommonBoxService.java` | 0（本波） | existing local business class | caller/host activation cohort 前冻结，不在 W-CBOX-1 并行改写 |
| DHXY | template/Maven/resources/tests/callers/UI | 0 | frozen | 模板原样；不新增测试/资源/schema/启动面 |

### 11. Dependency DAG 与首个无 AB 重叠实施切片

```text
Slice 1 (可立即独立，1 New，零 shared remote 文件)
  DHXY CommonBoxLocalObservationMechanics
    -> AB RX3 shared Java 写集释放并稳定
    -> Slice 2 paired wire schema
       [Cloud WindowFactKind/WindowFact/WindowFactOutcome/OutcomeEnvelope]
       + [DHXY RemoteWindowFactKind/RemoteCommonBoxObservationFact/Local handler mapping]
    -> Slice 3 retained async plumbing
       [Broker PendingCommand sink + RemoteGameClientPort + CommandExecutor
        + ServicePort + RetainedActionState]
    -> Slice 4 Cloud business/state integration
       [governor exact clear/diagnostics + CloudCommonBoxRunCapability + CommonBoxService]
    -> Slice 5 sole assembly lifecycle
       [AuthorityAssembly initial/resume/terminal/caps
        + ScopeLifecycle + Routes bundle + CloudServiceHost close hook]
    -> compile gates after each Java wave, while all host/Task/caller/UI paths remain dormant
    -> later separate activation cohort wires Runner milestones / task callers / UI toggle transport
```

**首个实施切片固定为 1 个文件：**

`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\commonbox\CommonBoxLocalObservationMechanics.java`（New）。

它只依赖 committed `BoundWindowCaptureService`、`ImageFinder`、`WindowRuntimeContext/WindowNativeBinding` 和本地模板；不触碰 AB 的 RX3 remote/ledger/broker/port Java 写集，不接 caller，不产生输入或运行副作用。后续实施 agent 修改 Java 后须按仓库门禁编译；本 Design-only 任务未运行 Maven。

### 12. Self-QA（不构成 approval）

- Baseline 五个入口、leader/member 默认、两任务 gate、role mismatch clear、30 秒 wall-clock 起点、ROI/template/threshold、异步返回、pending replace/prune、click-failure retention、task-turn/caller优先级均有落点。
- local whole observation 不上传图片；Cloud 不加载模板/做匹配/换算；input 仍经 exact binding + single global queue。
- observation 与 click 使用两个 fixed semantic slots，不把 detect/consume 合并；UNKNOWN/STOPPED/ACK-loss 均无 replacement mint。
- final mutation 只经现有 real final-consumed coordinator；无 CommonBox second ledger/registry/outbox/thread/queue/poller。
- resume 复用 retained state但不让旧 revision mutation命中新 pending；terminal 与 scope retirement分离；capacity有唯一 owner及显式释放点。
- shared schema/digest、constructor ownership、package visibility、New/Modify/0 与依赖波次已列明；首切片 1 文件且不与 AB 重叠。
- 当前未发现需要自行批准的业务差异。Self-QA：`P0=0 / P1=0 / P2=0`，仅供 parent review。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Design Review #1 - TRUE PHYSICAL EOF CONTROL POINTER / `W-CBOX-1-D2` - 2026-07-13T18:32:00-04:00

父级完整 `BLOCKED` 结论因 append anchor 重复而物理落在本文件第 70 行起；该历史块正文保持不动。
本 EOF pointer 明确宣布该结论现在生效：`P0=0 / P1=4 / P2=2`，Worker AE 下一任务为
`W-CBOX-1-D2`，只追加 `Design Repair #1 Delta` 关闭上述六项，Java/Maven/schema/resources/host/caller
继续冻结。后续领取、返修和父级复审一律追加在本 pointer 之后，并以真实物理 EOF 为控制面。

## Internal Worker AE - CLAIMED - 2026-07-13T18:34:39.796-04:00

- task: `W-CBOX-1-D2`
- claimedAt: `2026-07-13T18:34:39.796-04:00`
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-common-box-integration-worker-ae.md`

## Internal Worker AE - Design Repair #1 Delta - 2026-07-13T18:42:38.539-04:00

本节只增量修复 Parent Design Review #1 的 `P1-1..P1-4/P2-1..P2-2`。Design #1 中父级明确保留的
方向继续有效；本节点名 supersede 的句子、API、文件状态和 DAG 以本节为准，不改写历史。本节是 Worker
设计/实现输入，不是 review 或 approval。

### P1-1 closure - 每个显式 detect 独立 occurrence，允许并发在途

Design #1 `§4/§5` 中“每个 role 只有一个 open observation occurrence”“UNKNOWN/ACK 未压缩时后续
detect 复用旧 request”两条被本 Delta **撤销**；该限制只继续适用于可能产生物理副作用的 consume
occurrence，不适用于 observation。

每次 `detectLeaderBoxAfterReturnHome` / `detectMemberBoxAfterCombatExit` 通过 committed 同步 cheap gate
后，都按以下顺序执行一次：

1. governor mint 一个只属于本次调用的 `DetectTicket`；
2. `CloudTaskRetainedActionState.beginCommonBoxObservation(...)` 从对应 role 的 observation slot 分配新的
   sequential occurrence；
3. 用该 occurrence 的 existing retained action handle 登记一次 `WINDOW_FACT / COMMON_BOX_OBSERVATION`；
4. sink 闭包只捕获该 occurrence、该 ticket 和 stable run identity。前一 occurrence 的 `UNKNOWN`、ACK-loss
   或 receipt 未压缩都不阻止本次调用；
5. 同一 occurrence 内的 transport duplicate 仍重用 exact request bytes，且 local observation 最多执行一次。

新增的 package-private retained-state 合同固定为：

```java
CommonBoxObservationStart beginCommonBoxObservation(
        CommonBoxRole role,
        String canonicalTask,
        CommonBoxStateGovernor.DetectTicket ticket);
```

`CommonBoxObservationStart` 是 `CloudTaskRetainedActionState` 底部的 package-private immutable nested
record，包含 `status`、existing non-mintable retained action handle、`occurrence` 和 exact ticket；status
只有 `STARTED`、`CAPACITY_REJECTED`、`TERMINAL`。非 `STARTED` 不登记 local command、不 coalesce、不
自动 retry。

现有 owner-retained-action store 已按完整 semantic address 保存多个 entry；扩展只在
`CloudTaskRetainedActionState` 增加 leader/member 各一个 monotonic observation-occurrence scalar，不新增
map/registry/queue/thread/executor。每个 entry 继续计入 assembly 已注入的 existing
owner-retained-action hard cap；达到 hard cap 时返回 `CAPACITY_REJECTED`，不 eviction、不复用旧 occurrence。
final-consumed receipt 后由现有 compaction 删除该 occurrence；`UNKNOWN` 继续占用同一个有界 entry，直到
exact late final、terminal 或既有 retained lifecycle 收尾。

broker 在接受每个 occurrence 的唯一 non-UNKNOWN final 时，在 broker lock 内分配单调
`finalAcceptanceSequence`。每个 MATCHED sink 都调用：

```java
ObservationCommitResult commitObservation(
        DetectTicket originalTicket,
        CommonBoxAuthoritySnapshot currentSnapshot,
        CommonBoxObservationFact fact,
        RetainedFinalIdentity finalIdentity,
        long finalAcceptanceSequence);
```

CommonBox fixed-role workflow 保存每个 role 的 `lastAppliedMatchedFinalSequence` 与
`lastAppliedMatchedFinalIdentity` 两组 scalar；它们不是第二 ledger。相同 identity 是 idempotent no-op，较小
sequence 返回 `REJECTED_SUPERSEDED`，较大 sequence 才进入 governor mutation。这样即使两个锁外 sink
线程调度反序，唯一 final 后接受的 MATCHED completion 仍最后覆盖同 key 的 `UNCLAIMED` pending，保持
committed last-completion-wins；NOT_MATCHED/closed failure 不清 pending，也不推进该 matched sequence。
现有 `CLAIMED/SEALED`、stale、terminal、disabled、capacity 拒绝规则保持不变。

### P1-2 closure - `PendingCommand` exactly-once notification 状态机与单写者

`RetainedOutcomeSink` 不再是无状态 callback。它与以下字段直接挂在 existing broker `PendingCommand`；
这些都是同一 entry 的 scalar/reference，不建立旁路集合：

```java
private OutcomeState outcomeState;           // WAITING, UNKNOWN_RETAINED, FINAL_RETAINED
private NotificationState notificationState; // NONE, READY, IN_DELIVERY, DELIVERED
private RemoteCommandOutcomeEnvelope retainedOutcome;
private String retainedFinalDigest;
private long finalAcceptanceSequence;
private long notificationToken;
private RetainedOutcomeSink retainedOutcomeSink;
```

所有 first reply、poll-timeout synthetic outcome、late outcome 和 duplicate outcome 都进入
`RemoteGameCommandBroker.acceptOutcomeLocked(...)`；它是 `outcomeState`、retained envelope、digest、sequence
和 token 的唯一 writer，并且只在 broker state lock 内运行：

| current + input | broker-lock transition | sink token / returned envelope |
|---|---|---|
| `WAITING + UNKNOWN` | 保存 exact UNKNOWN，转 `UNKNOWN_RETAINED` | `NONE`；返回 retained UNKNOWN |
| `WAITING + non-UNKNOWN` | 保存唯一 final/digest，分配 sequence，转 `FINAL_RETAINED` | `READY(token)`；返回 retained final |
| `UNKNOWN_RETAINED + exact UNKNOWN` | 不改状态 | 不 mint token；重送 retained UNKNOWN |
| `UNKNOWN_RETAINED + non-UNKNOWN` | 只接受一次 exact late final，替换 retained envelope，转 `FINAL_RETAINED` | `READY(token)` |
| `FINAL_RETAINED + same digest` | 不改状态 | 不 mint token；只重送 retained final |
| `FINAL_RETAINED + conflicting digest/identity` | typed reject，保留原 final | 不通知 governor |
| 任意状态 + final-consumed ACK/receipt duplicate | command outcome 状态不变 | 只由 existing final route 重送 retained notice |

`claimNotificationLocked(...)` 是 `READY -> IN_DELIVERY` 的唯一 writer，并产生一个 immutable
`RetainedFinalNotification(handle,envelope,digest,sequence,token)`；随后立即释放 broker lock。sink 在锁外同步
执行。`finishNotificationLocked(token, result)` 是 notification state 的唯一 completion writer：

- sink 成功仅指 exact final 已被 retained ledger 接受，且 existing
  `RemoteFinalConsumptionCoordinator.consumeFinal(...)` 已保留 domain disposition/final-consumed notice；此时
  `IN_DELIVERY -> DELIVERED`；
- sink 抛异常时保留同 token、同 envelope、同 sequence，`IN_DELIVERY -> READY` 并记录诊断；不生成新
  command/action/request。下一次现有 poll/outcome/receipt 驱动只可重新 claim 该 token，不新增后台
  retry/thread/queue；
- 若第一次 sink 已完成 coordinator transaction、只在返回 broker 前抛错，重投 sink 时 retained ledger
  返回 same accepted final，final coordinator 返回 same retained detail，绝不再次调用 governor；
- concurrent duplicate 在 `IN_DELIVERY` 或 `DELIVERED` 只取得 retained envelope，不能取得 token。

类/方法级单写者固定如下：

| state | sole writer | lock / exactly-once point |
|---|---|---|
| broker outcome + notification token | `RemoteGameCommandBroker.acceptOutcomeLocked` / `claimNotificationLocked` / `finishNotificationLocked` | broker state lock；任何 sink/ledger/governor 调用前释放 |
| exact old action final | `CloudTaskRetainedActionState.acceptRetainedFinal(...)` | existing retained-ledger owner lock；same handle+digest 返回同一 accepted final，conflict 拒绝 |
| final domain-consumption token/detail | `RemoteFinalConsumptionCoordinator.consumeFinal(...)` | existing final transaction；domain callback 在 coordinator lock 外，detail 回写后才允许 final-consumed notice |
| current run snapshot | `CloudTaskRunAuthorityAssembly.snapshotCommonBoxAuthority(...)` | authority transition lock；只读 current slot，不接受 caller mint 的 fence |
| CommonBox pending/config/claim + matched sequence | `CommonBoxStateGovernor.commitObservation(...)` / `settleConsume(...)` | fixed workflow lock -> sole governor lock；无 port/broker callback |
| ACK/receipt 与 occurrence compaction | existing final-consumed receipt owner | exact final identity；不回调 broker，不重新运行 domain mutation |

锁序不形成环：broker lock 独占且不嵌套；retained ledger/final coordinator 在调用 domain finalizer 前释放各自
lock；domain finalizer 只走 `authority transition -> CommonBox workflow -> governor`；返回并释放后 coordinator
才保留 detail，最后 broker 单独重入自身 lock 标 `DELIVERED`。terminal owner 也在释放 governor/workflow 后
才调用 existing ledger terminal path。任何 sink 都禁止调用 broker poll/complete API，消除 reentrant
broker/ledger deadlock。

governor mutation 方法是无外部 callback 的 total typed transition：预先构造 disposition，最后一步才写
scalar/pending 并直接返回。observation 的 fixed-role `lastAppliedMatchedFinalIdentity/Sequence` 使异常恢复后的
same final 不会在 pending 已消费/清理后重新创建；consume settlement 以 exact reservation generation 为
idempotency key，已 remove/release/seal 后重复 settlement 只返回 `ALREADY_SETTLED`。

### P1-3 closure - old retained final 与 current mutation authority 双通道

Design #1 未闭合的 `requireRetainedFinal(...)` 被以下两个 package-private、互不授予对方权限的步骤替代。
新增 value types 均为 owning class 底部 immutable nested records，不扩大 public service/raw port surface。

**A. old identity final channel：**

```java
AcceptedRetainedFinal acceptRetainedFinal(
        RetainedActionHandle originalHandle,
        RemoteCommandOutcomeEnvelope exactOutcome,
        String exactOutcomeDigest,
        long finalAcceptanceSequence);
```

owner 是 `CloudTaskRetainedActionState`。`originalHandle` 是 occurrence 创建时返回并由 broker sink 捕获的
non-mintable handle。该方法只校验 stable run owner、semantic address、request/action identity、request
digest、exact outcome digest，并在 existing retained ledger 记录唯一 final；它**不调用 current execution
gate，不读取 current context/fence，不派发 mechanics/input，不修改 governor**。旧 revision、pause、resume
或 terminal 后仍可得到 `ACCEPTED_NEW` / `ACCEPTED_SAME`，随后进入 existing final-consumed transaction；
identity conflict/unknown handle 返回 typed reject。final-consumed notice 的 exact receipt 仍可压缩 old
occurrence，且 receipt path 不需要 current runtime 存活。

**B. current authority mutation channel：**

```java
CommonBoxAuthoritySnapshot snapshotCommonBoxAuthority(
        RemoteTaskRunScope scope,
        String taskRunId);

ObservationCommitResult commitObservation(
        DetectTicket originalTicket,
        CommonBoxAuthoritySnapshot currentSnapshot,
        CommonBoxObservationFact fact,
        RetainedFinalIdentity finalIdentity,
        long finalAcceptanceSequence);

ConsumeSettlementResult settleConsume(
        ConsumeReservation originalReservation,
        CommonBoxAuthoritySnapshot currentSnapshot,
        RemoteInputBundleOutcome exactOutcome,
        RetainedFinalIdentity finalIdentity);
```

`snapshotCommonBoxAuthority` 的 owner 是 assembly current-slot；它返回 stable run identity、current Fence、
slot generation 与 `CURRENT / TERMINAL / REPLACED / ABSENT`，从不返回 capability/port。pause 且未换 revision
仍是 `CURRENT`：它不授权新 action dispatch，但允许已接受 final 做 Cloud-only commit/settlement；resume 必须
发布新 `runRevision+slotGeneration`。governor 方法同时比较 original ticket/reservation、snapshot 与 workflow
内部 current fence/terminal generation：

- same current nonterminal fence 才按 exact outcome做一次 mutation；
- old revision、slot replacement、identity/session/stop epoch 不同返回 `REJECTED_STALE`，零写入；
- `TERMINAL/ABSENT` 返回 `REJECTED_TERMINAL` / `REJECTED_ABSENT`，零写入；
- 无论 mutation disposition 是 committed、stale、terminal 还是 no-op，都作为同一个 final-consumed detail
  保留，确保 old occurrence 可 ACK/receipt/compact，但不因此获得第二次 side effect。

并发原子边界固定为：resume/terminal/slot replacement 在 assembly transition lock 内先更新 current slot，
再按 `workflow -> governor` 发布 current fence 或 terminal；finalizer 先完成 A 并释放 ledger lock，再读取 immutable
snapshot，然后按同一 `workflow -> governor` 验证。若 finalizer 先赢，terminal 随后会按 existing 顺序删除 exact
run pending；若 terminal/resume 先赢，governor 看到 terminal/new generation 并零写入。任何路径都不同时持有
ledger lock 与 authority/workflow/governor lock。

### P1-4 closure - 首切片完整且单文件可编译

首切片仍固定为一个 New 文件，不依赖 Slice 2 wire DTO：

`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\commonbox\CommonBoxLocalObservationMechanics.java`

其完整 public compile contract 为：

```java
@Service
public final class CommonBoxLocalObservationMechanics {
    public CommonBoxLocalObservationMechanics(BoundWindowCaptureService captureService);

    public ObservationResult observe(WindowNativeBinding binding);

    public enum Status {
        MATCHED,
        NOT_MATCHED,
        CAPTURE_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        MECHANICS_FAILED
    }

    public record ObservationResult(
            Status status,
            Integer clientX,
            Integer clientY,
            Double matchScore,
            Long matchedAtEpochMs) {
    }
}
```

`ObservationResult` 与 `Status` 是该类底部的 public immutable nested types；因此 Slice 1 不需要新 DTO。
后续 `LocalRemoteGameCommandHandler` 在既有 pre-gate 后直接调用一次
`mechanics.observe(access.binding())`，post-gate 后直接把 nested result 映射成
`RemoteCommonBoxObservationFact`，无 `internal/prepare/resolve` wrapper chain。main method JavaDoc 必须注明
binding 是同一次 exact gate 的非空 geometry snapshot，输出 point 是 `WINDOW_CLIENT_PX`，所有 failure 都是
non-null closed result。

返回矩阵固定为：

| condition | result | nullable fields / throw |
|---|---|---|
| valid match，`match.length>=3`、坐标/score finite、score `>=0.86` | `MATCHED` | point/score/time 全非空；不 throw |
| matcher 返回 null | `NOT_MATCHED` | point/score/time 全 null |
| binding null、无 native handle/geometry、capture Optional empty 或 capture 抛错 | `CAPTURE_UNAVAILABLE` | match fields 全 null |
| fixed template missing、`ImageIO.read` 返回 null、decode/corrupt 抛错 | `TEMPLATE_UNAVAILABLE` | match fields 全 null；不缓存失败 |
| matcher 抛错、malformed match、非 finite/越 ROI 坐标、result invariant 异常 | `MECHANICS_FAILED` | match fields 全 null；方法不向外抛 RuntimeException |

### P2-1 closure - cache、absolute capture、client point 与资源 owner

`CommonBoxLocalObservationMechanics` 是 Spring singleton，也是模板 lazy cache 的唯一 owner：

- fixed path 仍为 `images/template/common/leader_box_marker.png`，threshold 仍为 `0.86`；
- `private volatile BufferedImage cachedTemplate` + 单一 private lock 做 lazy load，只 publish 一次成功 decode；
  missing/corrupt/exception 保持 null，下一次**显式 detect**可按 committed 行为重新尝试，不建 timer/backoff；
- cached template 不在每次调用后 flush；单次 capture frame 由本方法 owner 在取得后立即进入 `try/finally`，
  无论 template unavailable、no match、MATCHED、matcher/invariant exception 都在 `finally` 恰好 `flush()` 一次；
  `captureRegion` empty 时没有 frame 需要释放；其内部 full-window frame 继续由 capture service 自己释放。

唯一 capture 调用的实参固定为：

```java
int baseX = binding.getX();
int baseY = binding.getY();
Optional<BoundWindowCaptureService.CaptureResult> captured = captureService.captureRegion(
        binding,
        baseX,
        baseY,
        baseX + 623,
        baseY + 590,
        baseX + 682,
        baseY + 618);
```

它保持 committed screen-absolute ROI 传参，同时让 `BoundWindowCaptureService` 按相同 base 裁出 59x28 frame。
MATCHED client point 固定为 `clientX=623+(int)Math.round(match[0])`、
`clientY=590+(int)Math.round(match[1])`，score 为 `match[2]`，`matchedAtEpochMs` 在 match 校验成功时读取
`System.currentTimeMillis()`；不除 scale、不加 screen base。handler 使用同一 binding 做 post-gate并附加
`ObservedWindowBinding`。不调用 `captureRegionToFile`，不写 temp/marked output，不上传 image bytes。

### P2-2 closure - dormant 波 `CloudServiceHost=0`

Design #1 文件表中 `CloudServiceHost Modify` 及 Slice 5 host close hook 被本 Delta **撤销**。W-CBOX-1
保持：

| 文件 | 修正后状态 | 本波边界 |
|---|---:|---|
| Cloud `.../host/CloudServiceHost.java` | **0** | 不注入 scope lifecycle、不改 constructor/start/close |
| Cloud `.../remote/CloudCommonBoxScopeLifecycle.java` | New | package-private final、package-private ctor；assembly 唯一 owner；只提供 exact scope retirement capability |
| Cloud `.../remote/CloudTaskRunAuthorityAssembly.java` | Modify | 构造同一 governor/lifecycle；实现 current snapshot、resume/terminal 与 `retireScope` 内部合同 |
| Cloud `.../remote/RemoteTaskRunRoutes.java` | Modify（原业务 composition 范围） | 不向 host/public bundle 暴露 lifecycle，不增加 close hook |
| DHXY `.../commonbox/CommonBoxLocalObservationMechanics.java` | New | Slice 1 单文件，nested result；不依赖 remote DTO |
| DHXY `.../cloud/remote/RemoteCommonBoxObservationFact.java` | New（Slice 2） | wire mirror；不进入 Slice 1 |

package-private lifecycle 的容量释放合同仍是：authenticated scope owner 必须先 terminal 该 scope 全部 run，
再由 assembly `retireScope(scope)` 原子删除 config+pending tenant state并推进 incarnation；普通 run terminal 不
retire scope。当前 dormant 波没有 owner 调用它，因此不伪造已接线/已验收。实际 owner bridge、routes bundle
closed method 与 `CloudServiceHost.close` 顺序全部移到独立 activation cohort，并与 Runner/Task/caller/UI
activation 一起设计和验证。

修正后的 DAG 为：

```text
Slice 1 (1 New, compile-independent, no AB overlap)
  DHXY CommonBoxLocalObservationMechanics
    -> Slice 2 paired dormant wire fact
       [Cloud WindowFact enum/fact/outcome/parser]
       + [DHXY fact enum/RemoteCommonBoxObservationFact/handler direct mapping]
    -> Slice 3 retained concurrency + exactly-once
       [RetainedActionState multi observation occurrences
        + Broker PendingCommand state/token
        + ClientPort/CommandExecutor sink bridge
        + existing final coordinator integration]
    -> Slice 4 CommonBox authority/state
       [Governor matched-sequence idempotency + exact clear/settle
        + CloudCommonBoxRunCapability + Cloud CommonBoxService]
    -> Slice 5 dormant assembly only
       [AuthorityAssembly current snapshot/resume/terminal/caps
        + package-private ScopeLifecycle + Routes composition]
       CloudServiceHost / Task / caller / UI remain 0
    -> separate activation cohort
       [host owner bridge + scope close wiring + Runner milestones/callers/UI transport]
```

除上述 supersede 项外，Design #1 的 local HWND capture/template/coordinate/input safety、Cloud-only
pending/config、observation 与 consume 分离、30 秒从 local match wall-clock 起算、图片不上云、existing
governor claim/incarnation/capacity、无第二 ledger/queue/thread/map 等方向保持不变。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

### Internal Worker AE - DELIVERED_FOR_PARENT_REVIEW - TRUE PHYSICAL EOF / `W-CBOX-1-D2` - 2026-07-13T18:46:43.246-04:00

先前 `2026-07-13T18:46:15.283-04:00` 的同名交付块因重复 append anchor 物理落在本文件第 70 行；
该历史块保持不动。本真实物理 EOF 控制标记明确宣布以下交付现在生效：

- deliverable: `Design Repair #1 Delta`
- closure: `P1-1 / P1-2 / P1-3 / P1-4 / P2-1 / P2-2` 已逐项给出增量闭合设计
- status: `DELIVERED_FOR_PARENT_REVIEW`
- approval: 无；Internal Worker AE 不承担 review/approval，任何 self-QA 均不算批准
- frozen: Java/Maven/schema/resources/host/caller 均未修改，未运行 Maven，未执行 Git mutation
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-common-box-integration-worker-ae.md`

## Internal Worker AE - CLAIMED - TRUE PHYSICAL EOF / `W-CBOX-1-S1-IMP1+D3` - 2026-07-13T18:58:37.790-04:00

- task: `W-CBOX-1-S1-IMP1+D3`
- claimedAt: `2026-07-13T18:58:37.790-04:00`
- uniqueWriteSet:
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\commonbox\CommonBoxLocalObservationMechanics.java`（仅 New）
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-common-box-integration-worker-ae.md`（仅真实 EOF append）
- frozen: 其它 Java/schema/resources/host/caller/tests 与 AB/A/B/C/D 写集；不运行 Maven；不执行 Git mutation

## Parent Design Review #2 - TRUE PHYSICAL EOF CONTROL COPY / `W-CBOX-1-S1-IMP1+D3` - 2026-07-13T18:58:50-04:00

本文历史第 80 行的完整 `Parent Design Review #2` 由本真实 EOF 控制副本生效：Slice 1 local mechanics
**DESIGN APPROVED**；完整集成仍 `P0=0 / P1=2 / P2=1`。

AE 现在立即执行同一任务的两部分：

1. 仅新建 DHXY `src/main/java/com/bot/dhxy/service/commonbox/CommonBoxLocalObservationMechanics.java`，严格采用
   D2 的 public constructor/`observe`/nested result 合同、7 参数 exact-binding capture、ROI
   `(623,590)-(682,618)`、threshold `0.86`、success-only template cache、client-pixel point、strict null matrix
   与 single frame flush；不写盘/上传/输入，不增 thread/executor/queue/wrapper。
2. 仅向本日志追加 Design Repair #2 Delta：
   - 关闭 P1：当前 `RemoteCommandPollingLoop:173-181` 是同步 handler→submit；Cloud 多 occurrence 不能替代
     committed local `CompletableFuture.runAsync` 的独立执行/实际完成顺序。给出不新建专用线程/executor/第二
     ledger/queue的 exact local async completion + same identity late-outcome 合同；否则必须声明串行化业务差异。
   - 关闭 P1：sink failure `IN_DELIVERY->READY` 后必须 re-arm existing route wake；poll 在 wait/IDLE/new command
     前优先 claim same token，锁外执行，不把会终止 local polling loop 的异常返回给 client，不 redispatch mechanics。
   - 关闭 P2：Slice 4 前明确 client/cloud 不同时钟下 30 秒 TTL 的唯一权威与 future/skew 处理，禁止 client
     timestamp 反向 prune 其它 pending，也不改变 TTL 时长。

AB 正连续修改共享 Java，AE 落单文件后只报告源码与自审，暂不并发 Maven；父级待树稳定后统一跑 DHXY compile。
其余 Java/schema/resources/host/caller/tests 和 A/B/C/D/AB 写集冻结。self-QA 不算批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal Worker AE - Implementation + Design Repair #2 Delta - 2026-07-13T19:06:39.014-04:00

本节以紧邻上方的 `Parent Design Review #2 - TRUE PHYSICAL EOF CONTROL COPY` 为唯一控制输入；只交付
approved Slice 1 源码与 D3 三项设计返修，不把 Worker 自审写成 review/approval。

### 1. Approved Slice 1 implementation

唯一新建 Java 文件：

`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\commonbox\CommonBoxLocalObservationMechanics.java`

- SHA-256: `7e9f09084495dfa71d83c516ec321e11b77890e902780148626e90a6c540dafd`
- size/lines: `7512 bytes / 183 lines`
- public API: exact constructor `CommonBoxLocalObservationMechanics(BoundWindowCaptureService)`、
  `ObservationResult observe(WindowNativeBinding)`、nested public `Status` 与 immutable `ObservationResult`；
- capture: exact binding 的 `baseX/baseY` 与 screen-absolute `(baseX+623,baseY+590)-(baseX+682,baseY+618)`
  传入七参数 `captureRegion`；invalid binding/capture empty/exception 统一 closed
  `CAPTURE_UNAVAILABLE`；
- matcher: fixed template `images/template/common/leader_box_marker.png`、threshold `0.86`，
  `ImageFinder.find(frame,template,threshold)` 的 center/score 做 length、finite、threshold、ROI 验证；
- point: MATCHED 只返回 `WINDOW_CLIENT_PX`：`623+round(match[0])`、`590+round(match[1])`；
  nested record 同时强制 ROI/null/finite/positive-time invariant；
- cache/resource: singleton 内 `volatile + private lock` success-only lazy cache；失败不缓存；cached template 不
  flush；取得的 capture frame 在唯一 `finally` 恰好 flush 一次；
- safety: 不写盘、不上传 image、不发送 input，不创建 thread/executor/queue，不接 caller/handler。

### 2. D3 P1-1 closure - local retained async completion 恢复 committed 独立在途顺序

D2 的 Cloud multi-occurrence 只解决 retained identity；本 Delta 明确 Slice 2 还必须把 local
`RemoteCommandPollingLoop` 的 synchronous `handler.handle -> submitOutcome` 改成 **first UNKNOWN ACK 后启动的
retained async completion**。不允许把 CommonBox observation 留在 poll FIFO 串行执行。

可编译 local 合同固定为修改现有 `RemoteCommandHandler.java`，在同文件底部放 package-private handling value
types，不新增 registry/queue 文件：

```java
public interface RemoteCommandHandler {
    RemoteCommandHandling handle(RemoteGameCommand command) throws Exception;
}

sealed interface RemoteCommandHandling
        permits ImmediateCommandHandling, RetainedAsyncCommandHandling {
}

record ImmediateCommandHandling(RemoteGameOutcomeEnvelope outcome)
        implements RemoteCommandHandling {
}

final class RetainedAsyncCommandHandling implements RemoteCommandHandling {
    RemoteGameOutcomeEnvelope firstUnknown();
    synchronized CompletionStage<RemoteGameOutcomeEnvelope>
            startAfterFirstOutcomeAccepted();
}
```

`RetainedAsyncCommandHandling` 每 occurrence 只持 original command、exact UNKNOWN envelope、一个
`Supplier<RemoteGameOutcomeEnvelope>` 与至多一个 memoized `CompletableFuture` reference；不是 map/ledger/
queue。`startAfterFirstOutcomeAccepted()` 第一次调用使用**无 executor 参数**的
`CompletableFuture.supplyAsync(work)`，后续调用返回同一 stage。它因此复用 committed
`CompletableFuture.runAsync` 的 JVM common-pool 边界，不 new/注入专用 executor 或 thread。

`LocalRemoteGameCommandHandler.handle(...)` 的 future Slice 2 规则是：

1. 现有 request digest、registration、exact binding、taskRun/revision cheap gate 仍在 poll thread 同步完成；
2. 所有现有 operation 返回 `ImmediateCommandHandling`，行为不变；只有
   `WINDOW_FACT/COMMON_BOX_OBSERVATION` 返回 `RetainedAsyncCommandHandling`，此时尚未 capture/match；
3. `firstUnknown` 固化 original command 的 contractVersion、operation、scope、taskRun、requestId、actionId、
   semantic address、requestDigest 与 observed-window request identity，execution state 为 existing `UNKNOWN`；
4. work 恰好调用一次 `CommonBoxLocalObservationMechanics.observe(exactBinding)`，然后做 post-binding gate并
   构造唯一 non-UNKNOWN final；future exception 也映射为同 identity 的 closed `MECHANICS_FAILED` final。

`RemoteCommandPollingLoop` 的 future Slice 2 分支顺序固定为：

```text
poll COMMAND
  -> handler.handle(command) returns RetainedAsyncCommandHandling
  -> validate firstUnknown correlation
  -> transport.submitOutcome(firstUnknown) returns ACCEPTED/exact DUPLICATE
  -> only now call startAfterFirstOutcomeAccepted()
  -> poll thread immediately continues its normal loop
  -> common-pool stages for different occurrences may run concurrently
  -> each stage completion validates correlation and calls submitOutcome(exact late final) once
```

若 first UNKNOWN submit 抛 transport exception，work 不启动、mechanics 零执行；不把“是否已经执行”猜成
业务事实。late final 必须复用 original request/action/semantic address/requestDigest 和 command bytes，只有
outcome state/fact/outcomeDigest 按现有 late-outcome 合同变化，绝不 mint replacement。completion callback 的
transport failure只按 existing no-internal-retry transport 合同记录，不创建第二 pending-outcome queue，也不
反向终止已继续运行的 poll thread。

因此两个显式 milestone 各有一个 one-shot future，可真实并发 capture/match；谁先完成谁先提交 late final，
Cloud broker 再按 unique final acceptance sequence 串行化 governor，恢复 committed async completion 与
last-completion-wins，而不是 poll-command FIFO wins。

### 3. D3 P1-2 closure - READY 使用 existing route wake 且 poll 绝对优先

D2 中“outcome request thread 可直接调用 sink”的余地被本 Delta 收紧：first/late non-UNKNOWN outcome endpoint
只做 retained accept/ACK 与 route wake，sink 统一由 existing `poll` drive，不增 worker/thread/queue。

精确顺序固定为：

1. `acceptOutcomeLocked(...)` 在 broker state lock 内保存唯一 final envelope/digest/sequence，状态置
   `FINAL_RETAINED + READY(token)`，生成 retained `ACCEPTED`/exact `DUPLICATE` ACK，并调用 existing
   route-wake generation/condition 的 `signal`；
2. 释放 broker lock，`submitOutcome` 向 local 返回该 ACK；此路径不执行 sink，故 sink exception 永不变成会
   终止 `RemoteCommandPollingLoop` 的 transport exception；
3. local loop 收到 ACK 后继续下一次 existing poll。Cloud `poll` 在 wait、返回 IDLE、选择新 command 之前，
   先在 lock 内调用 `claimNotificationLocked(route)`，唯一 `READY -> IN_DELIVERY` 并取得 same token；
4. poll 释放 lock 后同步运行 sink；sink 只消费 retained final，不 dispatch command，不调用 local mechanics；
5. sink 成功后 `finishNotificationLocked(success)` 写 `DELIVERED`，随后同一次 poll 重新选择，existing
   final-consumed notice 优先于新 command；
6. sink 失败后 `finishNotificationLocked(failure)` 保留 same envelope/token/sequence，写回 `READY`，再次推进
   **同一个 existing route wake generation 并 signal waiter**。本次 poll 不紧循环重 claim 同 token、不选择新
   command，而是返回 normal IDLE/retry 响应；下一次 poll 仍在任何 wait/IDLE/command 前优先 claim 它。

`poll` 调用栈只需一个 ephemeral `attemptedNotificationToken` 防止同一请求 busy-spin；它不进入 retained state、
不跨请求保存，也不是 registry。没有 waiter 时 READY 本身仍是 poll-ready predicate，所以下一次 poll 不进入
long wait；已有 waiter 时 route signal 立即唤醒。duplicate outcome/ACK 只重送 retained envelope/ACK，不 mint
第二 token；READY retry 进入 existing final coordinator 后若 detail 已保留，只返回 same detail，governor 不重跑。

由此最后一条 command 或长 idle 也有确定驱动：final ACK 先成功返回，local loop 必然继续 poll；READY 比新
command/IDLE/wait 更高优先级。sink failure 不 redispatch mechanics，不终止 polling loop，不依赖“恰好还有下一条
command”。

### 4. D3 P2-1 closure - Cloud commit clock 是 30 秒唯一权威

committed `0114604e` 是在 async detection 完成、即将 `pendingByKey.put` 前读取一次
`System.currentTimeMillis()`，再写 `expiresAt=now+30_000`。迁移后对应的 authoritative pending mutation 是
Cloud governor 的 successful `commitObservation`，因此唯一业务时钟改由 assembly 注入 governor 的 Cloud
`Clock` 提供；local `matchedAtEpochMs` 不再参与 deadline、prune、ordering 或 replacement decision。

successful MATCHED commit 在 governor lock 内固定为：

```java
long committedAtEpochMs = clock.millis();
long expiresAtEpochMs = Math.addExact(committedAtEpochMs, 30_000L);
```

- `30_000L` 是唯一 CommonBox TTL 常量；不加 grace、skew allowance、network allowance、pause extension 或
  第二 TTL；`Math.addExact`/非正 Cloud time 失败时 typed fail-closed、零 mutation；
- pending 保存 `committedAtEpochMs`、`expiresAtEpochMs` 和原始
  `clientMatchedAtEpochMsDiagnostic`；client 值只进入 digest/结构化诊断，不命名为 server matched time；
- client timestamp 早于、晚于或远 future 于 Cloud now 时只标记 `CLIENT_CLOCK_BEHIND/AHEAD` 诊断；不 clamp
  server clock、不延长/缩短 30 秒、不因 skew 单独把真实 MATCHED 改成 NOT_MATCHED；
- all prune/has/reserve/consume 使用新的 `clock.millis()` 与 entry 自己的 server `expiresAtEpochMs`；
  `commitObservation` 如需先 prune，也只能传 Cloud now，并只按每个 entry 的 server expiry 判断；
- `clientMatchedAtEpochMsDiagnostic` 禁止传给 `pruneExpired`、禁止与其它 pending 的 expiry 比较、禁止覆盖
  broker `finalAcceptanceSequence`。因此 future client timestamp 不能延寿本 entry，也不能反向 prune 任意其它
  pending；
- resume/pause/ACK-loss 不改 server deadline。30 秒仍从 authoritative pending 创建点开始，等价于 committed
  `put` 前读 now，而不是从跨机器不可比较的 client epoch 开始。

### 5. Source self-QA（不构成 approval）

- 只读核对了 approved constructor、capture 七参数、`ImageFinder.find` 返回 `[centerX,centerY,score]` 与
  `WindowNativeBinding` geometry API；源码未引用 Slice 2 DTO；
- 所有 observe 返回路径均为 non-null typed result；MATCHED fields 全有，四种 negative fields 全 null；
- capture frame 只有一个 owner/一个 `finally flush`；template success-only cache 永不按调用 flush；
- 未发现磁盘输出、图片上传、input、thread/executor/queue/caller wiring；
- 未运行 Maven/测试/应用/Task/UI/capture/input；AB 共享 Java 连续写入期间 compile 明确留给父级稳定后统一执行；
- self-QA 仅说明 Worker 已按 approved contract 做源码检查，**不构成 SOURCE APPROVED 或任何批准**。

### 6. Internal Worker AE - DELIVERED_FOR_PARENT_REVIEW - TRUE PHYSICAL EOF / `W-CBOX-1-S1-IMP1+D3`

- implementation: approved Slice 1 单文件已落盘
- implementation SHA-256: `7e9f09084495dfa71d83c516ec321e11b77890e902780148626e90a6c540dafd`
- design repair: D3 `P1-1 local async completion / P1-2 READY wake+poll priority / P2-1 Cloud TTL clock`
  已逐项给出闭合合同
- status: `DELIVERED_FOR_PARENT_REVIEW`
- compile: `NOT_RUN_BY_PARENT_INSTRUCTION`；待 AB 稳定后由父级统一 DHXY compile，当前不宣称 SOURCE APPROVED
- approval: 无；Internal Worker AE 不承担 reviewer/approval，self-QA 不算批准
- actualWriteSet:
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\commonbox\CommonBoxLocalObservationMechanics.java`（New）
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-common-box-integration-worker-ae.md`（append-only）
- frozenWriteSet: 其它 Java/schema/resources/host/caller/tests 以及 A/B/C/D/AB 写集均未修改

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source + Design Review #3 - TRUE PHYSICAL EOF CONTROL COPY - 2026-07-13T19:14:30-04:00

本文历史位置的完整 Review #3 由本 EOF 控制副本生效：Slice 1
**STATIC PASS，P0=0/P1=0/P2=0**，D3 **DESIGN APPROVED，P0=0/P1=0/P2=0**。实际源码 SHA-256 为
`7E9F09084495DFA71D83C516EC321E11B77890E902780148626E90A6C540DAFD`；exact HWND capture、ROI/模板/
threshold、WINDOW_CLIENT_PX、cache/flush/null matrix 均与批准合同一致。D3 的 async completion、READY route wake/poll
priority 与 Cloud-authoritative 30 秒 clock 合同通过。

这仍不是最终 SOURCE/BUILD APPROVED：AB 稳定后须由父级执行 DHXY `mvn -q -DskipTests compile`；此前 Slice 2-5
不得落码。Internal AE 无返修任务，可关闭释放内部槽位。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
