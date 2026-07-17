# Internal Worker AM - RX3 Simplification Classification

## Parent Inventory Task / `W-RX3-CLASSIFY-1` - 2026-07-13T20:38:00-04:00

只做源码盘点，不写 Design、不改 Java。先完整读取：

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- `D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-13-direct-service-input-bundle-migration.md`
- `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-generic-exclusive-projection-worker-ab.md`
- 两仓最新 `git status`

用户已批准简化路线：Task/Service 业务原样迁 Cloud，普通物理序列用一个 `InputBundle`；只有输入中途依赖
capture/template/OCR 的少数流程保留本地宏。禁止继续 per-Service owner/permit/ledger/parent-child/
compaction/durable workflow/TTL/自动 retry。共享层只允许 scope/window/taskRun/runRevision fence、稳定
request/action identity、closed action allowlist、单一 input queue 与 terminal outcome。

任务：从 AB 报告、两仓 git diff/status 与源码引用中枚举 `W-TTR-RX3-IMP1` 实际新增/修改的每个 Java/schema
文件，逐文件机械分类：

1. `KEEP_SHARED_SAFETY`：确属上述仍允许的全 Service 共用安全边界；
2. `OBSOLETE_BY_SIMPLIFICATION`：属于 generic/per-Service exclusive owner、permit、parent-child、额外 ledger/
   workflow，不能被后续直接迁移 Service 依赖；
3. `PREEXISTING_OR_UNCERTAIN`：不是 AB 独有或无法从证据安全归属，必须保护。

每项必须列绝对/仓内路径、当前 tracked/untracked 状态、关键类型/方法、main 源引用点与引用数量、是否被当前
`CloudGameClient`/`InputSequences`/AK/AL 直接路径引用。只形成后续人工保留/隔离依据，不删除、不回滚、不改名、
不运行 Maven、不碰任何其它报告。唯一写集是本 append-only 报告。交付标题：
`Internal Worker AM - W-RX3-CLASSIFY-1 Inventory #1`。自审只算 QA。

## CLAIMED

- task=`W-RX3-CLASSIFY-1`
- uniqueWriteSet=本 append-only 报告；两仓源码只读

## Internal Worker AM - W-RX3-CLASSIFY-1 Inventory #1

- completedAt=`2026-07-13T20:51:43-04:00`
- scope=`W-TTR-RX3-IMP1` Java/schema source inventory only
- result=`37 files inventoried: KEEP_SHARED_SAFETY=0, OBSOLETE_BY_SIMPLIFICATION=7, PREEXISTING_OR_UNCERTAIN=30`
- mutation=`仅追加本报告；零 Java、零 schema 改写、零 Maven、零测试、零 Git mutation`

### 1. Evidence snapshot and counting rule

- DHXY: root=`D:\mavenProject\DHXY`, branch=`thin-client-design`, HEAD=
  `0114604e1ff5f15491d2910959c45252e893d04f`。最新 `git status --short --branch` 仍有大量并行
  dirty/untracked；AB 涉及的 `cloud/remote` 整包显示 `??`，`InputActionRequest/Queue/Worker` 与 protocol
  schema 显示 tracked `M`。四个 tracked 文件相对 HEAD 的 `git diff --numstat` 分别为：schema
  `+704/-130`、queue `+534/-15`、request `+744/-10`、worker `+222/-14`，不能把这些整文件差异只归给 AB。
- Cloud: root=`D:\mavenProject\dhxy-cloud-brain`, branch=`navigation-migration`, HEAD=
  `3b988caa010254973e03342272e6d1d6a9685b01`。最新 status 中整个
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/` 仍显示 `??`，所以 Git 本身不能提供 AB 前后 diff；
  New/Modify 归属以 AB 报告真实 EOF 的 `HALTED_BY_SIMPLIFICATION` exact list 为准。
- 路径缩写：`C:` 表示绝对根 `D:\mavenProject\dhxy-cloud-brain\`，`D:` 表示绝对根
  `D:\mavenProject\DHXY\`；每行 `C:`/`D:` 后内容同时是对应仓内相对路径。
- `refs=x/y` 表示在当前仓 `src/main/java` 对主类型名执行 `rg -n -w`、排除声明文件后得到 `x` 个匹配行、
  `y` 个引用文件；计数包含 import/JavaDoc。`TaskTransactionAction` 在 `BagWorkflowState` 的 1 行仅为 JavaDoc，
  不是可执行调用。表中同时列最高信号引用点。
- Direct tags: `G`=`CloudGameClient`，`I`=Cloud 兼容 `InputSequences`，`AK`=
  `TaskExecutionContext + LeftTopStatusSwitchService`，`AL`=`AutoCombatPanelService`；`D`=这些源文件直接引用，
  `T`=当前 ordinary runtime call path 间接经过，`N`=当前 ordinary path 不经过。

### 2. File-level classification rule

本次按“整文件是否能安全归属”分类，而不是把 mixed file 中的一段 AB 代码冒充整文件所有权。AB 明确标成 New 的
7 个文件只表达 generic retained-exclusive contract，因此可判 `OBSOLETE_BY_SIMPLIFICATION`。AB 明确标成 Modify 的
29 个 Java 文件以及既有 protocol schema 均早于 AB 存在，或当前已混入 AL 等后续 Worker 代码；按任务规则必须判
`PREEXISTING_OR_UNCERTAIN` 并整文件保护。故 file-level `KEEP_SHARED_SAFETY=0`；允许保留的 shared-safety 部分确实存在，
但都位于 mixed files 内，见第 7 节，不能据本 inventory 删除整个 uncertain 文件。

### 3. Cloud Java inventory (21)

| # | Absolute-root/repo path and status | Classification | Key type/method and current main refs | G/I/AK/AL direct path |
|---|---|---|---|---|
| C1 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/ExclusiveInteractionControlRequest.java`; `??`; AB=New | `OBSOLETE_BY_SIMPLIFICATION` | `ExclusiveInteractionControlRequest`, `Command{ACQUIRE,RELEASE,ABORT}`; refs=`48/10`: authority(14), broker(13), retained state(6), gate(5) | `N/N/N/N`; generic control only |
| C2 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/ExclusiveInteractionControlOutcome.java`; `??`; AB=New | `OBSOLETE_BY_SIMPLIFICATION` | typed control outcome, `MechanicalStatus`, `withCommon`; refs=`37/7`: authority(20), broker(9), envelope(3) | `N/N/N/N` |
| C3 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/ExclusiveSessionStepRef.java`; `??`; AB=New | `OBSOLETE_BY_SIMPLIFICATION` | session/generation/step cursor; refs=`14/8`: authority(3), service port(2), gate(2), executor(2), raw port(2) | `N/N/N/N`; ordinary path only observes null |
| C4 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTransactionAction.java`; `??`; AB=New | `OBSOLETE_BY_SIMPLIFICATION` | non-mintable transaction parent/occurrence handle; refs=`16/3`: retained state(11), authority(4), BagWorkflowState JavaDoc(1) | `N/N/N/N`; no executable producer/consumer |
| C5 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | pre-AB whole-pass owner mixed with AB `acquireOrJoinGeneric`, `terminateGeneric`, `bind/accept/consumeGenericStep`; refs=`27/5`: assembly(8), service context(8), service port(4), summon capability(4) | `T/T/T/T`: service port currently performs a generic-active check on every capture/input; generic acquire/terminate each have 0 external callers |
| C6 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionState.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | retained owner state; AB paths include `bindAcquire`, `bindStep`, `parkPaused`, release/abort and unresolved fence; refs=`47/1`, all authority | `N/N/N/N` for ordinary mechanics; authority-only state |
| C7 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | keep candidates `retainWindowFact/retainCapture/retainInputBundle/invoke`; AB additions `declareTaskTransactionAction`, `retainExclusiveControl`; refs=`71/12`: authority(19), service port(14), TaskTransactionAction(9), `CloudGameClient`(4) | `D/T/T/T`; ordinary stable action identity is live |
| C8 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunActionLedger.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared `acquire`, bind/verify request, retained outcome and final-consume state; AB adds exclusive sparse terminal policy; refs=`104/11`: executor(34), gate(17), retained state(13), final coordinator(10) | `T/T/T/T`; shared identity/terminal path is live |
| C9 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteFinalConsumptionCoordinator.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | `consumeFinal`, ACK/receipt/compaction coordination; AB adds non-renewal rule for control; refs=`15/5`: routes(4), assembly(3), service port(2), authority(2) | `T/T/T/T` after non-UNKNOWN ordinary outcomes |
| C10 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | live ordinary `readWindowFact/capture/executeInputBundle/consumeFinal`; AB wraps capture/input with generic step hooks; refs=`71/9`: retained state(34), authority(10), `CloudGameClient`(8), service context(5), AK context(2) | `D/T/D/T`; current ordinary facade is live, generic hook is unwanted contamination |
| C11 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteOperation.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared closed operation enum; AB member=`EXCLUSIVE_INTERACTION_CONTROL`; refs=`116/27`, led by ledger(25), retained state(23), service port(15) | `T/T/T/T`; preserve ordinary members, isolate control member only after exhaustive switch audit |
| C12 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteRequest.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared sealed request root; AB permit=`ExclusiveInteractionControlRequest`; refs=`35/12`: ledger(13), broker(11), request DTOs | `T/T/T/T`; control permit itself is not used by ordinary path |
| C13 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteOutcome.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared sealed outcome root; AB permit=`ExclusiveInteractionControlOutcome`; refs=`78/15`: broker(43), ledger(12), protocol outcomes | `T/T/T/T`; control permit itself is dormant |
| C14 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CaptureRequest.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | ordinary capture request + compatibility constructor; AB optional `sessionRef`; refs=`36/9`: gate(13), executor(6), broker(5), raw port(4), `CloudGameClient`(2) | `D/N/N/N` for current anchors; G exposes capture, AK/AL currently use facts instead |
| C15 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/InputBundleRequest.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | ordinary ordered bundle + compatibility constructor; AB optional `sessionRef`; refs=`16/6`: broker(6), gate(4), executor(2), digests(2) | `T/T/T/T`; current direct services always use null sessionRef |
| C16 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | ordinary fact/capture/input methods; AB session-bound overloads + `executeExclusiveInteractionControl`; refs=`12/6`: service context(4), service port(2), executor(2), authority(2) | `T/T/T/T`; ordinary overloads are live |
| C17 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | ordinary request retain/build/dispatch; AB session overloads + `executeExclusiveInteractionControl`; refs=`4/3`: assembly(2), ledger(1), broker(1) | `T/T/T/T`; ordinary executor is live |
| C18 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | current-context/request binding gate; AB session overloads + `newExclusiveInteractionControlRequest`; refs=`18/8`: service context(3), assembly(3), authority(2), executor(2) | `T/T/T/T`; scope/window/taskRun/runRevision fence is live |
| C19 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared dispatch/dedupe/late-final broker; AB control dispatch, exclusive permit/admission, session payload branches; refs=`34/5`: ledger(13), routes(6), final coordinator(6), executor(5) | `T/T/T/T`; ordinary broker path is live |
| C20 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`; `??`; AB=Modify, later AL=Modify | `PREEXISTING_OR_UNCERTAIN` | `toTypedOutcome`, ordinary decoders; AB `exclusiveControlOutcome`; AL later added `AUTO_COMBAT_PANEL` fact decode; refs=`4/2`: broker(2), routes(2) | `T/T/T/T`; AL fact route proves whole file is not AB-owned |
| C21 | `C:src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared canonical request/outcome/ACK digests; AB includes sessionRef/control canonicalization; refs=`31/12`: gate(11), broker(4), endpoint(3), coordinator(2) | `T/T/T/T`; ordinary digest parity is live |

### 4. DHXY Java inventory (15)

| # | Absolute-root/repo path and status | Classification | Key type/method and current main refs | G/I/AK/AL direct path |
|---|---|---|---|---|
| D1 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteExclusiveInteractionControlCommandPayload.java`; `??`; AB=New | `OBSOLETE_BY_SIMPLIFICATION` | local closed ACQUIRE/RELEASE/ABORT payload; refs=`22/5`: handler(12), registry(5), codec(3), digest(1) | `N/N/N/N` |
| D2 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteExclusiveInteractionControlOutcomePayload.java`; `??`; AB=New | `OBSOLETE_BY_SIMPLIFICATION` | local control result + `MechanicalStatus`; refs=`45/2`: handler(31), codec(14) | `N/N/N/N` |
| D3 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteExclusiveSessionStepRef.java`; `??`; AB=New | `OBSOLETE_BY_SIMPLIFICATION` | local session/generation/step cursor; refs=`9/4`: registry(6), capture payload(1), input payload(1), codec(1) | `N/N/N/N`; ordinary payload has null |
| D4 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOperation.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared local operation enum; AB member=`EXCLUSIVE_INTERACTION_CONTROL`; refs=`50/8`: handler(12), final ACK(12), ledger(9), codec/digests(12 combined) | `T/T/T/T`; ordinary operation switch is live |
| D5 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureCommandPayload.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | ordinary capture payload; AB optional `sessionRef`; refs=`7/2`: handler(5), codec(2) | `T/N/N/N`; only G capture route, current AK/AL use facts |
| D6 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteInputBundleCommandPayload.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | ordinary input bundle payload; AB optional `sessionRef`; refs=`8/2`: handler(5), codec(3) | `T/T/T/T`; current path always null sessionRef |
| D7 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | strict ordinary `readCapture/readInputBundle/toPayloadTree`; AB control/sessionRef decode; refs=`6/3`: digest(4), handler(1), outcome envelope(1) | `T/T/T/T`; strict ordinary codec is live |
| D8 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared `computeRequestDigest/computeOutcomeDigest/sha256Hex`; AB control/sessionRef canonicalization; refs=`12/7`: transport(3), registry(3), handler/ledger(4) | `T/T/T/T`; ordinary cross-repo digest is live |
| D9 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | shared `claim/complete`, exact duplicate and receipt state; AB exclusive-input classification/sparse terminal child; refs=`15/3`: handler(8), polling loop(5), registry(2) | `T/T/T/T`; shared dedupe path is live |
| D10 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunRegistry.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | live admission/lifecycle/window/runRevision fence; pre-AB whole-pass handle mixed with AB `openGenericExclusive`, bind/complete step, terminal snapshot; refs=`31/5`: handler(19), lifecycle(6), polling(3), ledger(2) | `T/T/T/T`; ordinary fence is live, generic handle branch is not |
| D11 | `D:src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`; `??`; AB=Modify, later AL=Modify | `PREEXISTING_OR_UNCERTAIN` | live ordinary capture/fact/input handler; AB sessionRef and control branches; AL later added auto-panel fact handler; refs=`2/2`: leader precheck capability/mechanics | `T/T/T/T`; AL directly extended this live path |
| D12 | `D:src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollingLoop.java`; `??`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | existing `start/stop/runLoop`; AB adds `invalidateExclusiveOwnersForIncarnationChange`; refs=`3/1`, all lifecycle service | `T/T/T/T`; polling remains ordinary transport plumbing |
| D13 | `D:src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`; tracked `M`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | baseline action/exclusive-callback request mixed with AB `retainedSession`, dual `sessionAdmitted/result`, capacity-1 lane and terminal signals; refs=`52/4`: queue(32), worker(13), scope(4), dead letter(3) | `T/T/T/T`; ordinary request is critical, retained-session mode is dormant generic machinery |
| D14 | `D:src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`; tracked `M`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | baseline single queue `submitRemoteAndWaitDetailed/submitExclusiveAndWait`; AB `openRetainedSession`, submit step, terminate session; refs=`17/4`: handler(7), registry(4), `InputSequences`(3), worker(3) | `T/T/T/T`; single ordinary input queue must remain |
| D15 | `D:src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`; tracked `M`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | baseline sole worker/action execution mixed with AB `runRetainedSession`; refs=`2/2`: queue(1), `InputAction` JavaDoc(1) | `T/T/T/T`; ordinary single worker must remain |

### 5. Schema inventory (1)

| # | Absolute-root/repo path and status | Classification | Key section and refs | G/I/AK/AL direct path |
|---|---|---|---|---|
| S1 | `D:docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`; tracked `M`; AB=Modify | `PREEXISTING_OR_UNCERTAIN` | existing protocol contract plus AB-only section `§13 Dormant generic retained exclusive cohort` at current lines 618-751; main Java refs=`0` because Markdown is not imported | `N/N/N/N`; protect whole file, mark only §13 for later manual isolation |

### 6. Current direct-path finding

1. Current direct migrations do not need a generic transaction parent or retained session. `CloudGameClient` directly uses only
   `CloudTaskRetainedActionState.retainWindowFact/retainCapture/retainInputBundle` and ordinary
   `CloudTaskServicePort` methods. Cloud `InputSequences` maps one ordered list and calls
   `CloudGameClient.executeInputBundle(...)`. AK uses `readWindowFact(LEFT_TOP_STATUS)` plus one ordinary bundle; AL uses
   `readWindowFact(AUTO_COMBAT_PANEL/GEOMETRY)` plus ordinary Alt+8/drag bundles.
2. Static activation gap is exact: current main source has no external caller of
   `CloudTaskExclusiveInteractionAuthority.acquireOrJoinGeneric(...)` or `terminateGeneric(...)`, and no caller of
   `CloudTaskRetainedActionState.declareTaskTransactionAction(...)`. Therefore the AB control/session cohort is dormant and is
   not a prerequisite of G/I/AK/AL.
3. The ordinary Cloud path is nevertheless coupled to a no-op generic check:
   `CloudTaskServicePort.capture/executeInputBundle/consumeFinal` call `bindGenericStepIfActive`,
   `acceptGenericStepOutcomeIfActive` and `consumeGenericStepFinalIfActive`. This is the precise later isolation point; it does
   not justify deleting `CloudTaskServicePort` or its ordinary methods.
4. The ordinary wire still has compatibility constructors without sessionRef on both `CaptureRequest` and
   `InputBundleRequest`; current G/I/AK/AL calls produce null sessionRef. DHXY `LocalRemoteGameCommandHandler` only enters AB
   generic branches when that field is non-null or operation is `EXCLUSIVE_INTERACTION_CONTROL`.

### 7. Protected shared-safety portions inside uncertain files

- Cloud protect: current-context scope/window/taskRun/runRevision gate; ordinary retained request/action identity;
  ordinary `WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE` closed allowlist; broker exact duplicate/terminal outcome path;
  ordinary request/outcome digest parity. These live across C7-C21 and cannot be removed at file granularity.
- DHXY protect: registration/bound-window/runRevision admission fence; exact request digest and duplicate handling; ordinary
  capture/fact/input decode/handler; the one existing `InputActionQueue` and one `InputActionWorker`; ordinary terminal result.
  These live across D4-D15 and cannot be removed at file granularity.
- Later manual isolation candidates, without authorizing deletion here: C1-C4, D1-D3, enum/permit/control branches for
  `EXCLUSIVE_INTERACTION_CONTROL`, optional non-null `sessionRef` branches, C5/C6 generic state methods, C7 transaction-parent
  methods, C8/D9 exclusive sparse-child policy, D10 generic registry methods, D11 generic control/session branches,
  D13-D15 retained-session lane, and schema §13.
- `CloudTaskExclusiveInteractionAuthority/State`, local `InFlightExclusiveHandle`, and final-consume/ledger files include
  pre-AB Z/shared material. This inventory deliberately does not decide whether that older material is retained as a named local
  macro or simplified separately; deleting it as “AB code” would violate provenance protection.

### 8. Completeness / self-QA

- AB EOF exact list reconciled: Cloud New 4 + Modify 17 = 21; DHXY New 3 + Modify 12 = 15; schema Modify 1;
  total=`37`, no AB-listed Java/schema file omitted or added.
- Explicit exclusions because AB did not list them as writes: `CloudGameClient.java`, Cloud `InputSequences.java`, AK/AL
  Service files, DHXY `InputActionScope.java`, `InputActionExecutionResult.java` and `InputActionSafetyReason.java`. These were
  read only as dependency/baseline anchors.
- No source/schema/resource was changed, no Maven/test/runtime was run, and no reset/checkout/clean/delete/rename/commit was
  performed. Existing dirty/untracked content remains untouched.
- self-QA only; this inventory is retention/isolation evidence, not parent approval and not authorization to delete files.

## Parent Inventory Review #1 - APPROVED - 2026-07-13T20:58:00-04:00

父级按 AB 的 `HALTED_BY_SIMPLIFICATION` exact list、两仓当前引用和 AI/AK/AL 直接路径复核，结论
`P0=0 / P1=0 / P2=0`。37 个文件计数闭合：Cloud 21、DHXY 15、schema 1；文件级分类为
`OBSOLETE_BY_SIMPLIFICATION=7`、`PREEXISTING_OR_UNCERTAIN=30`、`KEEP_SHARED_SAFETY=0`。

批准口径仅为“后续依赖隔离”：C1-C4 与 D1-D3 七个纯 generic-exclusive 新文件不得成为新的直接迁移 Service
依赖；其余 mixed files 含现行 scope/window/taskRun/runRevision fence、stable identity、ordinary
WINDOW_FACT/CAPTURE/INPUT_BUNDLE、digest、dedupe/terminal 与单 input queue，必须整文件保护。当前不删除、不回滚、
不改 enum/permit/sessionRef 分支；如未来清理，必须另开有完整 switch/digest/双仓构建证据的独立切片。

`W-RX3-CLASSIFY-1 APPROVED`。该报告只提供保留/隔离依据，不计同路径迁移进度，不改变任何业务。
