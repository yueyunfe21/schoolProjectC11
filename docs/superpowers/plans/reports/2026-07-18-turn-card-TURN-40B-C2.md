# TURN-40B-C2 - Wubei/FiveRing Bag+UI Turn Rewire

## Canonical Status

- state: `READY / ZERO OWNER / UNASSIGNED`
- authority: this original whole-card physical EOF is the sole claim/delivery/return authority.
- publication: public READY pool only; no Worker is assigned, reserved, scheduled or chased.
- prerequisite: TURN-40B-C1 parent SOURCE+TEST Review #2 passed at 2026-07-18 15:34 EDT; the frozen C1→C2 protocol/dispatcher serialization gate is satisfied.
- superseding contract: TURN-40BP1 Parent Report Review #7 passed. Its final frozen deltas at 11:49, 12:19, 12:44, 13:04 and 13:22 are incorporated below; discarded earlier alternatives remain discarded.

## Objective And Baseline

Move the existing Wubei/FiveRing bag and UI calls through the single typed HTTPS turn/local-service path while preserving the user-approved business behavior from `docs/业务逻辑.md` and baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

- No phase-order, prompt interpretation, OCR/template/click/navigation, retry/fallback, verification-count, expiry, park/yield or input-order change is authorized.
- UI operations use the existing `CloudUiCleanerLocalServiceClient.execute(...)`; do not create a second UI protocol or convenience layer unless an existing signature is directly reused.
- Bag operations remain DHXY-local mechanics reached through the sole LOCAL_SERVICE protocol; do not migrate or copy `BagService`/`PlayerStateService` business algorithms into Cloud.
- `无已批准业务差异；按基线等价迁移`。

## Frozen Production Write Set

### Both Repositories, Mirrored Content

- Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`: add only `BAG_FIVERING_SUPPLY_CHECK`, `BAG_FIND_AND_USE_FROM_BACK`, `BAG_FIND_ITEM_PAGE_INDEX` in the closed operation set.
- Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnBagOperationArguments.java`: typed arguments for the three operations; reuse the frozen `maxBagIndex` slot as `requiredCount`, with no parallel payload model.
- Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`: exact per-operation bag shape and invalid-field rejection; preserve C1 metric closure byte-for-byte except mechanically required enum/switch additions.

### DHXY-cr271 Only

- Modify `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java`: capture the exact `RunningTaskHandle` and its live `TaskStopToken` at action resolution; expose only the frozen read/access predicates needed below. No TTL/cache/second authority.
- Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`: pass the captured token and a live `BooleanSupplier` identity predicate through the existing local-service path; map `LocalServiceExecution.stopRequested()` only to existing `TurnStepExecution.stopped(...)`.
- Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceExecution.java`: closed invariant `stopRequested == true` iff `status == FAILED && code == "STOPPED"`; `stopped(payload)` hardcodes `STOPPED`; reject both invalid cross-combinations.
- Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`: add the three bag operations to the unwrapped queue-owning bag arm and forward the captured token/live predicate; C1 METRIC and all legacy arms retain their ownership semantics.
- Modify `src/main/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutor.java`: constructor-inject existing `PlayerStateService`; implement sole `executeQueueOwning(...)` adapter for the three new operations; this adapter alone maps `TaskStopRequestedException` to `LocalServiceExecution.stopped(...)`.
- Modify `src/main/java/com/bot/dhxy/service/BagService.java`: add only `withMainBagOpenGuarded(String, BooleanSupplier, TaskStopToken, Function<MainBagSession,T>)`; existing `withMainBagOpen` and `withMainBagOpenExclusive` behavior remains unchanged.

### Cloud Only

- Modify `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`: replace the frozen bag/UI call sites with the typed clients without changing business decisions/order.
- Modify `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`: preserve one-session supply behavior and the exact incense-before-count/checkpoint order while routing through the typed clients.
- Modify `src/main/java/com/bot/dhxy/service/PlayerStateService.java`: delete only dead Cloud `ensureSheYaoXiangActiveInOpenMainBag`; no replacement algorithm.
- Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`: add the three strict typed methods and exact terminal/outcome mapping; STOPPED/UNKNOWN never becomes success.

## Guarded Queue Boundary Contract

1. `TurnExecutionWindow` captures one exact handle/token pair at resolve time. The predicate compares `runner.getCurrentTask()` with that captured handle by reference identity; same IDs are not identity.
2. `BagService.withMainBagOpenGuarded(...)` evaluates the live predicate as the first exclusive-callback action, before `ensureBagOpened` and before any physical input, then checks the captured live token.
3. Callback-local `AtomicReference<GuardedRejection>` may hold only `IDENTITY_REPLACED` or `STOP_REQUESTED`. A rejection sets the flag and returns false inside the callback; no Cloud type enters `BagService`.
4. After queue wait returns false: a rejection flag converts to existing `TaskStopRequestedException`; with no flag, a now-stopped captured token does the same; ordinary queue/open failure with an unstopped token remains generic non-STOPPED.
5. Only `BagLocalOperationExecutor` catches that exception and maps it to the closed local STOPPED representation. No queue-in-queue call, no pre-queue boolean snapshot and no successor-token lookup is permitted.
6. `BAG_FIVERING_SUPPLY_CHECK` performs one guarded open/close session, existing incense activation, frozen stop checkpoint, then `countItemUpTo`, returning the frozen three-field typed result. The two find/use operations call the corresponding existing single business method.

## Frozen Test Write Set And Acceptance

### Both Repositories, Mirrored Where Frozen

- Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`.
- Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`.
- Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnEnvelopeGoldenJsonTest.java`.
- Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`.

The protocol tests must prove the three-operation closed set, exact JSON/payload shape, required arguments, cross-operation rejection and DHXY/Cloud mirror parity without weakening C1 metric tests.

### DHXY-cr271 Only

- Modify `src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java`: every new bag op is unwrapped; legacy direct-macro bag ops remain wrapped; METRIC/UI/Quest/whole-task ownership remains frozen; zero nested queue.
- Modify `src/test/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutorContractTest.java`: constructor matrix, three operations, one-session supply order/result and sole STOPPED mapping.
- Modify `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`: production-chain capture-at-resolve replacement race, typed stopped turn outcome, zero tail input and zero failure-evidence capture.
- Create `src/test/java/com/bot/dhxy/service/BagServiceGuardedAdmissionTest.java`: four distinct outcomes: identity replacement and real token stop yield typed stop with zero input; ordinary queue failure and bag-open failure stay non-STOPPED; successful admission opens/closes exactly once.

### Cloud Only

- Create `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClientContractTest.java`: strict outcome/result mapping; STOPPED and UNKNOWN do not synthesize success.
- Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`: repair the frozen wrong-package import and prove bag/UI/metrics wiring without business drift.
- Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingWholeTaskTurnContractTest.java`: one-session supply, three-field result, exact order/checkpoints and wiring.

## Claim, Delivery And Gates

- An eligible Worker may claim only the complete card by appending one canonical whole-card claim at this physical EOF after rereading all three repo statuses and proving no later conflicting owner. Partial claims are invalid.
- Delivery must include all production and test files above, exact changed-path list, SHA/blob/mtime evidence, mirror proof, baseline rows checked and the sentence `无已批准业务差异；按基线等价迁移`.
- While any Java writer is active, do not run Maven. After all C1-C4 writers are stable, the parent runs only the authorized HTTPS turn named family and applicable DHXY/Cloud compile aggregate gate frozen by TURN-40BP1.
- No runtime/application/server/Task/UI/capture/input and no Git mutation.

<!-- TRUE_EOF: TURN-40B-C2 READY ZERO-OWNER UNASSIGNED WHOLE-CARD BAGUI-REWIRE C1-SOURCE-GATE-PASSED C1-C2-SERIAL-SATISFIED NO-CLAIM NOT-ASSIGNED NO-JAVA-NO-MAVEN 2026-07-18T15:34:00-04:00 -->

## EXTERNAL-C TURN-40B-C2 WHOLE-CARD CLAIMED - 2026-07-18T15:40:00-04:00

- owner: `EXTERNAL-C`（C1 Review#2 0/0/0 PASSED/OWNER RELEASED 15:34，eligible；本卡合同即 C 在 40BP1 Review#2-#6 逐轮冻结的 guarded 队列边界/stop bridge/双向 STOPPED 不变量契约，上下文最全）
- claim_basis: `PARENT-TURN40B-C1-REVIEW2-PASSED-C2-READY-1534`（C1→C2 串行 gate 已满足；READY/ZERO-OWNER/UNASSIGNED 不派卡）。
- **预检取证（append 前独立调用）**：全卡零既有 claim section；physical EOF=父级 15:34 READY marker；三仓状态复读（Cloud `navigation-migration`/DHXY-cr271 `thin-client-design`/DHXY 基线只读）无冲突 owner。**C3 仍 READY 未领**（与本卡写集不相交）。
- 承诺：严格按本卡冻结写集（双仓 3 协议+4 协议 test；DHXY 6 production+4 test；Cloud 4 production+3 test）；其它文件只读；需额外路径即 `PLAN-CONTRACT BLOCKED`；零 Git mutation；active writer 期间零 Maven（javac parse 除外）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批、不建 reviewer。
- next: append 后立即回读 physical EOF 证 sole ownership，确认后开始实施（顺序：协议三文件→DHXY stop-bridge 链→BagService guarded 入口→executor/dispatcher→Cloud caller/client→双仓 test）。

<!-- TRUE_EOF: TURN-40B-C2 EXTERNAL-C WHOLE-CARD CLAIMED ACK=PARENT-TURN40B-C1-REVIEW2-PASSED-C2-READY-1534 ZERO-PRIOR-CLAIM C3-STILL-READY AWAIT-EOF-REREAD 2026-07-18T15:40:00-04:00 -->

## Parent Claim Audit - 2026-07-18 15:42 EDT

- claim accepted: External C 的 15:40 whole-card claim 前置 EOF、零 prior claim、三仓与 C3 碰撞证据有效；C 为 `TURN-40B-C2` sole owner，状态 `SOURCE_ACTIVE`。
- communication: claim 内已引用 `PARENT-TURN40B-C1-REVIEW2-PASSED-C2-READY-1534`，但总账尚无规则要求的 STATUS EVENT `ack_parent_message`。该消息已连续两轮无合规 STATUS 回执，故只标 `COMMUNICATION_STALE / STATUS-ACK-MISSING`；源码活动不标 ACTIVE_STALE。
- unchanged: C3 仍 `READY / ZERO OWNER / UNASSIGNED`；C1 source gate 已关闭；C4 仍等待 TURN-39P1。
- gates: C active Java writer，父级不运行 Maven/runtime/input，不改业务实现。
- parent message: `PARENT-TURN40B-C2-CLAIM-ACCEPTED-STATUS-ACK-1542`，下一轮 STATUS EVENT 必须同时具名 ACK 1534 与本消息。

<!-- TRUE_EOF: TURN-40B-C2 PARENT-CLAIM-AUDIT ACCEPTED OWNER-C SOURCE-ACTIVE COMMUNICATION-STALE STATUS-ACK-MISSING MESSAGE=PARENT-TURN40B-C2-CLAIM-ACCEPTED-STATUS-ACK-1542 C3-READY-ZERO-OWNER NO-MAVEN 2026-07-18T15:42:00-04:00 -->

## Parent Communication Recovery Audit - 2026-07-18 15:44 EDT

- External C 15:43 STATUS EVENT 已合规具名 ACK `PARENT-TURN40B-C1-REVIEW2-PASSED-C2-READY-1534`，并准确报告 C1 closed、C2 canonical claim/sole owner、C3 READY 与 aggregate build defer。
- state correction: `COMMUNICATION_STALE` 解除；C2 维持 `OWNER C / SOURCE_ACTIVE`。并发后消息 `PARENT-TURN40B-C2-CLAIM-ACCEPTED-STATUS-ACK-1542` 为 ACK pending round 1，不构成 stale。
- source gate: 尚无 delivery；active Java writer 期间不运行 Maven/runtime/input。

<!-- TRUE_EOF: TURN-40B-C2 PARENT-COMMUNICATION-RECOVERY ACK=PARENT-TURN40B-C1-REVIEW2-PASSED-C2-READY-1534 OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL MESSAGE1542-ACK-PENDING-ROUND1 C3-READY-ZERO-OWNER NO-MAVEN 2026-07-18T15:44:00-04:00 -->

## Parent Double-ACK Audit - 2026-07-18 15:49 EDT

- External C 15:48 STATUS EVENT 已双具名 ACK `1534 + 1542`，所有 pending parent message 闭环，communication normal。
- real progress: 双仓 `TurnLocalOperation.java` SHA-256 同为 `D4042DE0...`，`TurnBagOperationArguments.java` 同为 `ACB96CB4...`；协议首步正在推进，非 delivery/WIP review。
- canonical state: `OWNER C / SOURCE_ACTIVE`；C3 仍 `READY / ZERO OWNER / UNASSIGNED`。active writer 期间不运行 Maven/runtime/input。

<!-- TRUE_EOF: TURN-40B-C2 PARENT-DOUBLE-ACK-AUDIT ACK=1534+1542 OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PROTOCOL-MIRRORS-MOVING OP=D4042DE0 ARGS=ACB96CB4 C3-READY-ZERO-OWNER NO-MAVEN 2026-07-18T15:49:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 16:00 EDT

- External C 15:56 STATUS reports `5/13 production` landed: mirrored protocol trio plus DHXY `TurnExecutionWindow` capture-at-resolve and `LocalServiceExecution` closed STOPPED invariant; real SHA/mtime changes confirm activity.
- This is protected WIP, not delivery or source review. C remains sole owner/source active/communication normal; parent does not run Maven or inspect WIP for verdict.
- C3 remains `READY / ZERO OWNER / UNASSIGNED`.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=5-OF-13 NO-DELIVERY C3-READY-ZERO-OWNER NO-MAVEN 2026-07-18T16:00:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 16:16 EDT

- External C 16:12 STATUS reports DHXY-side production complete: all six DHXY production files are landed, including `BagLocalOperationExecutor.executeQueueOwning`; total production progress remains `9/13` pending the four Cloud production files.
- This remains protected WIP, not a canonical delivery or source-review trigger. C remains sole owner/source active/communication normal; tests and Cloud work are still pending.
- A has independently claimed C3; C2/C3 write sets remain disjoint. With both Java writers active, parent does not run Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=9-OF-13 DHXY-PRODUCTION-COMPLETE CLOUD-4+TEST-11-PENDING NO-DELIVERY C3-OWNER-A-DISJOINT NO-MAVEN 2026-07-18T16:16:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 16:22 EDT

- External C 16:21 STATUS reports Cloud `CloudBagLocalServiceClient` three strict typed methods landed; physical source SHA-256=`2F361F49F62115F86CE692FA6517F8082904B97FBA9A9211DFC8AF003F8881AF`, mtime `2026-07-18T07:44:27.1973384-04:00`.
- Production progress is now `10/13`; three Cloud production files and eleven tests remain. This is protected WIP, not canonical delivery or a source-review trigger. C remains sole owner/source active/communication normal.
- A's C3 remains source-active recon with unchanged target files. Both disjoint Java writers are active; parent does not run Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=10-OF-13 CLOUD-CLIENT=2F361F49 CLOUD-3+TEST-11-PENDING NO-DELIVERY C3-OWNER-A-DISJOINT NO-MAVEN 2026-07-18T16:22:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 16:32 EDT

- External C 16:30 STATUS reports Cloud `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag` dead method removed; physical file SHA-256=`C238DA2A67AB580B224EF0053DB2A4E708E8B5A1ECDC02C618BBD68DFB0B2D1A`, mtime `2026-07-18T07:49:16.9692342-04:00`.
- Production progress is `11/13`; only Wubei/FiveRing Task rewires and eleven tests remain. This is protected WIP, not canonical delivery or source review. C remains sole owner/source active/communication normal.
- C3 Amendment #1 adds only `ObjectiveTextRecognizer.java`, still disjoint from C2. Both writers remain active; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=11-OF-13 PLAYERSTATE=C238DA2A TWO-TASK-REWIRE+TEST-11-PENDING NO-DELIVERY C3-DISJOINT NO-MAVEN 2026-07-18T16:32:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 16:42 EDT

- External C 16:40 STATUS reports `FiveRingTaskV2` rewire complete; physical SHA-256=`ECBCA0597A80AFCD36B66228A1A273C993C05CA67926E8233BEC340BB92A52CC`, mtime `2026-07-18T07:55:59.9627788-04:00`.
- Production progress is `12/13`; only Wubei rewire and eleven tests remain. This is protected WIP, not canonical delivery/source review. C remains sole owner/source active/communication normal.
- C3 Amendment #2 remains disjoint. Both Java writers are active; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=12-OF-13 FIVERING=ECBCA059 WUBEI+TEST-11-PENDING NO-DELIVERY C3-DISJOINT NO-MAVEN 2026-07-18T16:42:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 16:54 EDT

- External C 16:50 STATUS reports Wubei rewire complete and all production `13/13` landed. Physical `WubeiTask.java` SHA-256=`9D537DBFECB6F909660FB5C0A67FE6BA10347F048F15D31198C67C59B09DF3BF`, mtime `2026-07-18T08:00:20.4017038-04:00`.
- Eleven exact test files remain. This is protected WIP, not canonical delivery/source review. C remains sole owner/source active/communication normal.
- C3 remains disjoint and source active. Both writers remain active; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 WUBEI=9D537DBF TEST-11-PENDING NO-DELIVERY C3-DISJOINT NO-MAVEN 2026-07-18T16:54:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 16:58 EDT

- External C 16:57 STATUS reports mirrored core golden and validator tests complete, progress `2/11`. Both repositories match: `TurnCoreProtocolGoldenJsonTest.java` SHA-256=`3737B04C0A9B2A3B128247FA6F56ECA3B4C19A24BC478D797146D4A949B4D90F`; `TurnProtocolValidatorContractTest.java` SHA-256=`2407D499EF7D8E917FC20CA3BEAEEBD5036B8EC261B8B45FBA2E9936AC6B857E`.
- The shared core test also closes C1's omitted METRIC×3 literals in `closedEnumsRemainExact`. This deferred-build coverage gap is disclosed under C2 and does not reopen C1's passed source gate.
- Nine tests remain. This is protected WIP, not canonical delivery/source review. C remains sole owner/source active/communication normal; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 TESTS=2-OF-11 CORE=3737B04C VALIDATOR=2407D499 MIRROR-IDENTICAL C1-CLOSEDENUM-COVERAGE-CLOSED DISCLOSE-AT-DELIVERY NO-DELIVERY C3-DISJOINT NO-MAVEN 2026-07-18T16:58:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 17:04 EDT

- External C 17:03 STATUS reports mirrored action and envelope golden tests complete, progress `4/11`; all four protocol tests are now complete.
- Cross-repository LF-normalized SHA-256 is `61D842FB83AB2423EA554EAF2FBDB6669907FACCFF2938C5ECB7A3D066503F9D` for action and `15D2691FF29F08D7017169751C6E7B072EC6375DDEBA51FEE08A7F3F12E7E726` for envelope. Raw DHXY hashes differ only because CRLF is retained; normalized content is identical.
- Seven exact test files remain. This is protected WIP, not canonical delivery/source review. C remains sole owner/source active/communication normal; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 TESTS=4-OF-11 ACTION=61D842FB ENVELOPE=15D2691F NORMALIZED-MIRROR-IDENTICAL PROTOCOL-TESTS-ALL-COMPLETE TEST-7-PENDING NO-DELIVERY C3-DISJOINT NO-MAVEN 2026-07-18T17:04:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 17:12 EDT

- External C 17:11 STATUS reports `LocalServiceStepDispatcherContractTest` complete, moving C2 tests to `5/11`; physical SHA-256=`0F3661F88EF577A2010B9FE3CC1F2AA1504B4988301F5C31E7FA2D02C6ACCC10`.
- Six exact tests remain. This is protected WIP, not canonical delivery/source review. C remains sole owner/source active/communication normal; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 TESTS=5-OF-11 DISPATCHER=0F3661F8 TEST-6-PENDING NO-DELIVERY C3-DISJOINT NO-MAVEN 2026-07-18T17:12:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 17:25 EDT

- External C 17:24 STATUS reports `BagLocalOperationExecutorContractTest` complete, moving C2 tests to `6/11`; physical SHA-256=`A25E919C163687F43E9FC52BAC343D1DC0EABE762FB042CC621C17FC4C125BAA`.
- Five exact tests remain. This is protected WIP, not delivery/source review. C remains sole owner/source active/communication normal; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 TESTS=6-OF-11 BAG-EXECUTOR=A25E919C TEST-5-PENDING NO-DELIVERY C3-REPAIR-DISJOINT NO-MAVEN 2026-07-18T17:25:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 17:36 EDT

- External C 17:32 STATUS reports new `BagServiceGuardedAdmissionTest` complete, moving C2 tests to `7/11`; the card remains source active with four exact tests pending and no canonical delivery.
- The retained test covers identity replacement, captured-token stop, ordinary queue failure, and post-wait stop admission outcomes before real bag input. Successful open/close remains frozen for the FiveRing whole-task test as disclosed by C.
- C3 independently passed parent Review #2 and released its owner; there is no write-set collision. C remains sole C2 owner/communication normal. No Maven/runtime/input while C is an active Java writer.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 TESTS=7-OF-11 GUARDED-ADMISSION-CREATED TEST-4-PENDING NO-DELIVERY C3-OWNER-RELEASED NO-MAVEN 2026-07-18T17:36:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 17:41 EDT

- External C 17:40 STATUS reports Cloud `CloudBagLocalServiceClientContractTest` complete, moving C2 tests to `8/11`; physical blob=`38609d6a`, SHA-256=`A8F88F1227D75D47116D647C8AA5FE8FE689BA2B3980AB37F5A492E5184F7B44`, 7 tests.
- The retained contract covers exact-window supply parsing, strict STOPPED/UNKNOWN/DUPLICATE_OR_UNCERTAIN/non-completed fail-closed outcomes, find/use and nullable page-index result semantics, and invalid-argument zero-command behavior.
- Three exact tests remain. This is protected WIP, not delivery/source review. C remains sole owner/source active/communication normal; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 TESTS=8-OF-11 CLOUD-BAG-CLIENT=38609D6A-A8F88F12-7T TEST-3-PENDING NO-DELIVERY C3-OWNER-RELEASED NO-MAVEN 2026-07-18T17:41:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 17:59 EDT

- External C 17:50 STATUS reports Wubei and FiveRing whole-task contract tests aligned, moving C2 tests to `10/11`. Wubei blob=`7168b543`, SHA-256=`D924AC54...`, 11 tests; FiveRing blob=`b54396be`, SHA-256=`42B6DDF...`, 18 tests.
- Wubei's three wrong-package imports and both task constructor fixtures now match the real production types. The disclosed FiveRing supply-check/session split remains a delivery-review item; this WIP audit does not pre-approve that coverage decision.
- Only `LocalTurnActionExecutorContractTest` remains before evidence and canonical delivery. C stays sole owner/source active/communication normal. A's report-only 39P1 claim is disjoint; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-WIP-ACTIVITY-AUDIT OWNER-C SOURCE-ACTIVE COMMUNICATION-NORMAL PRODUCTION=13-OF-13 TESTS=10-OF-11 WUBEI=7168B543-D924AC54-11T FIVERING=B54396BE-42B6DDF5-18T TEST-1-PENDING COVERAGE-SPLIT-REVIEW-AT-DELIVERY NO-DELIVERY DISJOINT-39P1 NO-MAVEN 2026-07-18T17:59:00-04:00 -->

## EXTERNAL-C TURN-40B-C2 CANONICAL WHOLE-CARD SOURCE+TEST DELIVERY - 2026-07-18T18:10:00-04:00

写集全部完成：production 13（双仓 3 协议+DHXY 6+Cloud 4），test 11（双仓 4 协议+DHXY 4+Cloud 3）。严格按冻结合同+40BP1 Review#2-#6 冻结链实施。

### Production 落地
- **协议（双仓 BYTE-IDENTICAL，diff 实证）**：`TurnLocalOperation` +三 bag op；`TurnBagOperationArguments` javadoc 冻结三 op 复用形状（maxBagIndex 槽=requiredCount，零平行 model）；`TurnProtocolValidator` 三新 bag 分支逐 op 精确形状+invalid-field 拒（C1 metric 闭合 byte 级保留）。
- **DHXY stop-bridge 链**：`TurnExecutionWindow` capture-at-resolve（`actionTaskHandle`+`actionStopToken` 同点冻结；`actionStopToken()`+`isActionTaskStillCurrent()` 引用同一性活谓词、resolve 无 owner fail-closed）；`LocalTurnActionExecutor` token 取传+`stopRequested()`→`TurnStepExecution.stopped(...)` 显式分支；`LocalServiceExecution` 双向闭合不变量（`stopRequested ⟺ FAILED+STOPPED`，`stopped(payload)` 硬编码，构造双向拒）。
- **guarded 队列边界**：`LocalServiceStepDispatcher` ctor+三参签名+三 bag 无包裹 arm→`bagAdapter.executeQueueOwning(call,token,predicate)`（legacy/METRIC arm 零动）；`BagLocalOperationExecutor` ctor 注入 PlayerStateService+`executeQueueOwning`（SUPPLY_CHECK 单 `withMainBagOpenGuarded` session incense→checkpoint→count 三字段 typed 结果；两单法；**唯一 STOPPED 映射**=catch `TaskStopRequestedException`→`stopped`）；`BagService` +`withMainBagOpenGuarded`（回调首动作谓词+token 检查 zero-input-before-open、rejection-flag+队列后转抛，两既有入口零字节，零 Cloud import）。
- **Cloud caller/client**：Wubei/FiveRing 两 Task bag/ui 站点 rewire（UI→既有 `CloudUiCleanerPort` 直接复用；bag→`bagClient(context)` 三新法/既有 return-item intent；业务序/checkpoint 序不变）；`PlayerStateService` 删死法 `ensureSheYaoXiangActiveInOpenMainBag`（唯一 caller 迁走）；`CloudBagLocalServiceClient` +三严格 typed 法+outcome 映射（STOPPED/UNKNOWN 永不造 success）。

### Test 落地
双仓 4 协议 test（core `values()` 全序+action/envelope/validator bag 形状，双仓 BYTE/CONTENT-IDENTICAL）+DHXY 4（dispatcher 三 bag 无包裹路由；bag-executor STOPPED 映射/find-use/find-index/session-unavailable/invalid；LocalTurnActionExecutor **capture-at-resolve 竞态 production-chain**=真实 dispatcher+bag adapter+scripted handle 替换→typed STOPPED turn outcome+零输入+零 failure-evidence；Create BagServiceGuardedAdmissionTest 四结局中三 rejection 路径）+Cloud 3（client ContractTest 7T；两 whole-task 构造对齐+Wubei 错包修复）。

### 终版取证（append 前实取；blob=git hash-object 前 8/SHA-256 前 8/行数/@Test）
| Cloud | 取证 | | DHXY-cr271 | 取证 |
|---|---|---|---|---|
| TurnLocalOperation.java | `3792c874`/`D4042DE0`/39L | | TurnLocalOperation.java（镜像 BYTE-IDENTICAL） | `3792c874`/`D4042DE0`/39L |
| TurnBagOperationArguments.java | `f9c78eb7`/`ACB96CB4`/33L | | TurnBagOperationArguments.java（BYTE-IDENTICAL） | `f9c78eb7`/`ACB96CB4`/33L |
| TurnProtocolValidator.java | `3d5bd786`/`3FDD3FFA`/767L | | TurnProtocolValidator.java（BYTE-IDENTICAL） | `3d5bd786`/`3FDD3FFA`/767L |
| task/wubei/WubeiTask.java | `fdd2739c`/`9D537DBF`/4451L | | cloud/turn/TurnExecutionWindow.java | `3ff7cda3`/`A3D82B6F`/200L |
| task/wuhuan/FiveRingTaskV2.java | `215afcb8`/`ECBCA059`/3043L | | cloud/turn/LocalTurnActionExecutor.java | `dfa450c2`/`7E86774C`/308L |
| service/PlayerStateService.java（删死法） | `babf688d`/`C238DA2A`/1438L | | cloud/turn/LocalServiceExecution.java | `8bcb5512`/`752D130E`/215L |
| turn/client/CloudBagLocalServiceClient.java | `29d2385b`/`2F361F49`/729L | | cloud/turn/LocalServiceStepDispatcher.java | `4d03e6b6`/`DC9B2B89`/129L |
| TurnActionGoldenJsonTest.java | `1887aa89`/`61D842FB`/209L/7T | | cloud/turn/local/BagLocalOperationExecutor.java | `8a912b18`/`69387F63`/272L |
| TurnCoreProtocolGoldenJsonTest.java（BYTE-IDENTICAL） | `0c9a1077`/`3737B04C`/425L/7T | | service/BagService.java（仅 +guarded 入口） | `52801876`/`49142188`/1487L |
| TurnEnvelopeGoldenJsonTest.java（CONTENT-IDENTICAL CRLF） | `24743c4a`/`15D2691F`/112L/6T | | LocalServiceStepDispatcherContractTest.java | `7f8b40a3`/`0F3661F8`/884L/19T |
| TurnProtocolValidatorContractTest.java（BYTE-IDENTICAL） | `fd087007`/`2407D499`/901L/14T | | local/BagLocalOperationExecutorContractTest.java | `2ebf4997`/`A25E919C`/343L/10T |
| CloudBagLocalServiceClientContractTest.java（Create） | `38609d6a`/`A8F88F12`/191L/7T | | LocalTurnActionExecutorContractTest.java | `4c2255e8`/`D862527E`/1403L/13T |
| WubeiWholeTaskTurnContractTest.java | `7168b543`/`D924AC54`/522L/11T | | service/BagServiceGuardedAdmissionTest.java（Create） | `6916ab39`/`7A234509`/125L/4T |
| FiveRingWholeTaskTurnContractTest.java | `b54396be`/`42B6DDF5`/987L/18T | | | |

action/envelope golden 保各自 EOL（Cloud=LF/DHXY=CRLF，`diff --strip-trailing-cr` 实证内容一致）。

### 披露（诚实上报）
1. **修复 C1 已过审卡的潜在缺陷**：`TurnCoreProtocolGoldenJsonTest.closedEnumsRemainExact` 硬编码 enum 名列表遗漏 C1 加的 METRIC×3（C1 只更同文件另一 `values()` 用例、漏此字面量；因 40B aggregate build deferred 从未真跑而漏过 C1 review）。C2 因共享该文件写集，一并补 METRIC×3+BAG×3。**不回退 C1 source gate**（deferred-build 覆盖盲区）。
2. **findAndUseMainBagTaskPageItem 判定**：实测该法与 `FIND_AND_USE_TASK_PAGE` 宏共用同一核心 `interactWithMainBagTaskPageItemExclusive`→**无缺口、非新 op**，Wubei 走既有 `executeReturnItem(...FIND_AND_USE_TASK_PAGE...)`；故三新 op 集正确、无 PLAN-CONTRACT BLOCKED。
3. **session 测试性拆分**：`MainBagSession` 私有 ctor 跨包不可构造→bag-executor 测覆盖 STOPPED 映射/session-unavailable/两单法/invalid；SUPPLY_CHECK 三字段+one-session 序（incense→checkpoint→count）由 executeQueueOwning 生产实现承载、其 typed 结果解析由 Cloud client ContractTest（EXECUTED 三字段）+协议 golden 覆盖；guarded 四结局中三 rejection 路径由 BagServiceGuardedAdmissionTest 直证、成功 open/close 复用未改的 withMainBagOpenExclusive。
4. **Wubei 错包 import 修复**：`WubeiWholeTaskTurnContractTest` 三 import `CloudTurnActionFactory/CommandPort/CommandResult` 从 `turn.client.*`→实际 `turn.*`（40BP1 报告已标记之真缺陷）。

### Gate 声明
全部新/改文件 javac 单文件/联编 parse 零纯语法错（`LocalTurnActionExecutorContractTest:1223` 唯一非噪声报错=既有 `RecordingInputQueue.super.submitFrozenExactWindowExclusiveAndWait` 的空-classpath "super-in-static" 噪声，非本卡改动、joint-compile 消除）。**named-test/compile 执行=shared-writer 延期声明**：C active writer 期间不运行 Maven；授权 HTTPS turn named family+双仓 compile aggregate gate 留待 C1-C4 全稳后父级 stable window。

`无已批准业务差异；按基线等价迁移`。零 Git mutation（hash-object 只读取证）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。请求：**whole-card SOURCE+TEST review**。

<!-- TRUE_EOF: TURN-40B-C2 EXTERNAL-C CANONICAL-DELIVERY 27-FILES PROTOCOL-MIRROR-IDENTICAL CAPTURE-AT-RESOLVE+GUARDED-QUEUE+DOUBLE-STOP-INVARIANT C1-CLOSEDENUM-FIXED FINDANDUSE=EXISTING-INTENT SESSION-SPLIT-DISCLOSED WUBEI-WRONGPKG-FIXED REQUEST-REVIEW OWNER-C NO-MAVEN 2026-07-18T18:10:00-04:00 -->

## Parent SOURCE+TEST Source Review #1 - 2026-07-18 18:14 EDT

- verdict: `P0/P1/P2=0/1/1 / BLOCKED / REPAIR REQUIRED`; External C retains whole-card ownership. Parent reviewed all 13 production and 11 test paths, their delivered blobs, the frozen 40BP1 contract, protocol mirrors, baseline `696a12b0`, and changed call sites. No separate production business-semantic finding was identified in this pass.
- P1 (`BagLocalOperationExecutor.java:86-104`; `BagLocalOperationExecutorContractTest.java:72-82`; `FiveRingWholeTaskTurnContractTest`): the frozen acceptance requires an executing test that proves one guarded main-bag session and exact `ensureSheYaoXiangActiveInOpenMainBag -> captured stop checkpoint -> countItemUpTo` order plus the returned `incenseRefilled/firstPageIndex/count` fields. The executor test only forces `withMainBagOpenGuarded` to return null, and C explicitly reports that FiveRing adds no supply battery. Protocol/client serialization tests do not execute this local callback and cannot substitute for the missing business-order/session assertion.
- P1 repair condition: within the frozen existing test write set, add deterministic fake-only coverage that actually executes the supply callback and asserts exactly one guarded session, the exact three-stage order, and all three result fields. No real bag/input/runtime, reflection-based production bypass, second adapter/protocol/store, copied business algorithm or production-only test hook. If the current private `MainBagSession` boundary makes this impossible without widening the contract, return `PLAN-CONTRACT BLOCKED` with the complete symbol/testability proof instead of weakening the acceptance item.
- P2 (`BagServiceGuardedAdmissionTest.java:16-30,34-86`): the frozen card separately requires ordinary bag-open failure to remain non-STOPPED and successful admission to open/close exactly once. The four delivered tests cover identity replacement, pre-stopped token, ordinary queue failure, and stop-during-wait; neither bag-open failure nor successful open/close is exercised. The JavaDoc assertion that success is proved by FiveRing is contradicted by the delivery disclosure that FiveRing has no supply execution battery.
- P2 repair condition: retain all existing rejection tests and add deterministic coverage for both missing outcomes, asserting non-STOPPED bag-open failure and one open/one close on success. Use no real input/runtime and do not relax the frozen outcome matrix.
- re-delivery gate: canonical re-deliver the whole card with fresh blobs/test counts and an exact repair disclosure. Named tests/compile remain deferred while repair Java/test writing is active. `无已批准业务差异；按基线等价迁移` remains mandatory.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-SOURCE+TEST-REVIEW-1 BLOCKED REPAIR-REQUIRED OWNER-C P0=0 P1=1 P2=1 P1=SUPPLY-SESSION+ORDER+THREE-FIELDS-UNEXECUTED P2=BAG-OPEN-FAILURE+SUCCESS-OPEN-CLOSE-ONCE-UNCOVERED FROZEN-TEST-WRITESET PLAN-BLOCK-IF-UNTESTABLE NO-MAVEN 2026-07-18T18:14:00-04:00 -->

## EXTERNAL-C OWNER RETURNED / PLAN-CONTRACT BLOCKED - 2026-07-18T18:34:00-04:00

`PARENT-TURN40B-C2-REVIEW-1` 已 18:31 具名 ACK；P0/P1/P2=0/1/1 全接受。按父级 P1 repair condition 明示途径（"若私有 MainBagSession 边界使之不可能 without widening the contract，返 PLAN-CONTRACT BLOCKED with the complete symbol/testability proof"），P1 与 P2 两验收项在冻结写集内 fake-only 均不可达。**owner 保留**；下述完整符号/可测性 proof + 最小 seam 提议待父级 Amendment 裁决，不弱化验收、不加 production-only test hook、不越写集。

### 完整 symbol / visibility proof（实测 `BagService.java`）
| 符号 | 位置 | 可见性 | 影响 |
|---|---|---|---|
| `MainBagSession(Point, TaskExecutionContext)` | :1470 | **private**（非静态内部类，唯一构造点 :280 `operation.apply(new MainBagSession(...))`） | 任何包外/子类/同包 test 均不可构造 MainBagSession 实例 |
| `ensureBagOpened(BagLayout, TaskExecutionContext)` | :573 | **private** | 不可 override/拦截；真实实现走 tracker/CoordinateHelper 捕屏+模板（本卡禁真实机制） |
| `closeBagIfNeeded(BagLayout, TaskExecutionContext)` | :747 | **private** | 同上 |
| `MainBagSession.countItemUpTo/useItem` | :1479/:1483 | public | 可调，但依赖一个不可构造的 MainBagSession 实例 |

### P1 不可达证明（supply session 序 + 三字段）
- 断言目标位于 `BagLocalOperationExecutor.executeQueueOwning`(:86-104) 传给 `withMainBagOpenGuarded` 的 lambda：`ensureSheYaoXiangActiveInOpenMainBag(mainBag) → captured stopToken.throwIfStopRequested() → mainBag.countItemUpTo(...) → SupplyCheckResult(三字段)`。
- 要真跑该 lambda 并断言序+三字段，`BagLocalOperationExecutorContractTest`（包 `com.bot.dhxy.cloud.turn.local`）的 FakeBagService 须以一个 MainBagSession 调 `operation.apply(session)`。MainBagSession ctor private 且在 `com.bot.dhxy.service` 包——跨包子类不可构造；即便改 package-private 亦不解（executor 测在 turn.local 包，非 service 包）。
- 协议 golden / `CloudBagLocalServiceClientContractTest` 只序列化/解析 wire 与 typed 结果，**不执行本地 callback**，无法替代业务序断言（父级已指明）。
- FiveRing whole-task test 为 layer-2 组件级（父级 P1-2 adjudication，不深驱 execute phase），无 supply battery（本卡交付已披露）。
- ⇒ 无 fake-only 路径 without 拓宽 MainBagSession 构造可见性至 executor 测包可达。

### P2 不可达证明（bag-open failure non-STOPPED + 成功 open/close 恰一次）
- 二者均须驱动 `withMainBagOpenGuarded` 通过 admission 后进入 `withMainBagOpenExclusive`→`ensureBagOpened`：成功路径需 ensureBagOpened 返回有效 anchor+`closeBagIfNeeded`；bag-open-failure 需 ensureBagOpened 返 null。
- `ensureBagOpened`/`closeBagIfNeeded` private，不可在 `BagServiceGuardedAdmissionTest`（同包 `com.bot.dhxy.service`）的子类 override；真实实现依赖 tracker/CoordinateHelper 捕屏（本卡禁真实 input/runtime，且这些 collaborator 在 fake BagService 中为 null→NPE 而非可控 null 返回）。
- ⇒ 无 fake-only 路径 without 使 ensureBagOpened/closeBagIfNeeded 可拦截。

### 最小 seam 提议（供父级 Amendment 二选一或组合；均不改业务算法/不加第二 store/不弱化验收）
- **提议 A（最小、推荐）**：`BagService` 内新增一个 **package-private** 测试可见的开/关注入点 —— 将 `ensureBagOpened`/`closeBagIfNeeded` 由 private 收窄改为 **package-private 且非 final**（`com.bot.dhxy.service` 同包 test 子类可 override 以避免真实机制），并将 `MainBagSession` ctor 由 private 改为 **package-private**（同包 test 可构造）。P1/P2 断言随之全部搬到**同包**新测试点或现有 `BagServiceGuardedAdmissionTest`（driving 一个同包 test-only BagLocalOperationExecutor 调用，executor 为 public 可跨包 import 构造）。此拓宽仅可见性、零业务/行为改动，"existing withMainBagOpen/withMainBagOpenExclusive behavior unchanged" 仍成立。
- **提议 B**：为 supply session 增一个 package-private seam `withMainBagOpenGuardedSession(...)` 或注入式 open-hook，使 fake 可提供 MainBagSession 与 open/close 结果；较 A 大，不推荐。
- 无论采纳哪个，写集需相应最小扩充（`BagService.java` 可见性改动已在写集内；若断言点须新建同包 test 文件，请父级明示是否纳入写集）。

冻结的 27 文件交付 blob 全部保持（18:10 版取证有效，本 BLOCKED 未改任何 Java/test）。`无已批准业务差异；按基线等价迁移`。零 Git mutation；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。请求：**父级 Amendment 裁决最小 seam**，获批后按新可见性补 P1/P2 断言并整卡 canonical re-delivery。

<!-- TRUE_EOF: TURN-40B-C2 EXTERNAL-C OWNER-RETURNED PLAN-CONTRACT-BLOCKED ACK=REVIEW-1 P1+P2-FAKE-ONLY-UNREACHABLE PRIVATE-MAINBAGSESSION-CTOR+PRIVATE-ENSUREBAGOPENED-PROVEN MINIMAL-SEAM-PROPOSED-A-VISIBILITY AWAIT-AMENDMENT OWNER-C NO-MAVEN 2026-07-18T18:34:00-04:00 -->

## Parent Plan-Contract Amendment #1 - 2026-07-18 18:36 EDT

- adjudication: `PLAN-CONTRACT REPAIRED / SOURCE REPAIR ACTIVE / OWNER C RETAINED`。父级完整核对 `BagService` 的 guarded/exclusive/session 调用链、C2 两份现有测试、TURN-40BP1 frozen acceptance 与基线 `696a12b0`。无需新增文件、协议、store、adapter、业务算法或 production-only hook。
- production inheritance audit: `src/main` 中 `extends BagService` 命中为零。下述三个方法均为真实生产边界，且 `BagService.java` 已在 C2 原 production 写集内。
- exact visibility amendment: 只允许把 `BagService.ensureBagOpened(BagLayout, TaskExecutionContext)`、`BagService.closeBagIfNeeded(BagLayout, TaskExecutionContext)`、`BagService.countItemUpToInOpenMainBag(Point, String, int, TaskExecutionContext)` 从 `private` 改为 `protected`。三个方法的实现体、参数、返回值、调用位置、异常/stop/input/order 语义必须逐字不变；`MainBagSession` 的 `final`、private constructor 和 public surface 不改。
- P1 executable route: `BagLocalOperationExecutorContractTest` 的 FakeBagService 使用 scripted `InputSequences` 并继承真实 `withMainBagOpenGuarded -> withMainBagOpenExclusive -> MainBagSession` 路径，仅 override 上述三个 protected seam。Fake PlayerStateService、recording TaskStopToken 和 deterministic count result 必须断言一次 queue/session/open/close、精确 `open -> incense ensure -> captured-token checkpoint -> count -> close` 顺序，以及 JSON `incenseRefilled/firstPageIndex/count` 三字段。不得继续 override guarded 入口返回 null。
- P2 executable route: `BagServiceGuardedAdmissionTest` 保留现有四个 rejection/queue cases，并通过同一真实 inherited guarded/exclusive 路径新增：`ensureBagOpened` deterministic null 时返回 generic null/non-STOPPED；deterministic Point 成功时 operation 一次且 open/close 各一次。零真实 tracker/OCR/input/runtime。
- write set: 不扩大。仅既有 `BagService.java`、`BagLocalOperationExecutorContractTest.java`、`BagServiceGuardedAdmissionTest.java` 可为本 Amendment 修改；C2 其余 delivered files 只读，除非 parent review 另有 finding。
- communication: C 18:31 STATUS 使用 `PARENT-TURN40B-C2-REVIEW-1`，不是定向 ledger message id `PARENT-C-TURN40B-C2-REVIEW1-REPAIR-1814`。连同 18:18 漏 ACK，现标 `COMMUNICATION_STALE`；不撤 owner、不标 active stale。下一 STATUS 必须精确 ACK 1814 与 Amendment message。
- re-delivery gate: 完成上述 repair 后在本卡 physical EOF canonical whole-card re-delivery，提供三文件 fresh blob/SHA/test count 与 retained 27-file parity。Java writer active 时不运行 Maven/runtime/input。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-40B-C2 PARENT-PLAN-CONTRACT-AMENDMENT1 REPAIRED OWNER-C SOURCE-REPAIR-ACTIVE VISIBILITY-ONLY=BAGSERVICE-ENSURE+CLOSE+COUNT-PRIVATE-TO-PROTECTED MAINBAGSESSION-UNCHANGED EXACT-WRITESET-UNCHANGED COMMUNICATION-STALE ACK-MISSING=1814 NO-MAVEN 2026-07-18T18:36:00-04:00 -->

## Parent Amendment ACK + Physical Source Audit - 2026-07-18 18:43 EDT

- C 18:41 STATUS EVENT 精确双 ACK `PARENT-C-TURN40B-C2-REVIEW1-REPAIR-1814` 与 `PARENT-C-TURN40B-C2-AMENDMENT1-1836`；communication recovered，owner C/source+test repair active。
- physical source: `BagService.java` SHA-256=`CE0EA995E47D63036F4ABE0F4BA63F02189D7633975C8E6876744C809BF144D7`、blob=`f094f67b`、mtime=`2026-07-18T09:01:13.7505703-04:00`。三处方法均已按 Amendment 改为 protected。
- physical tests unchanged: `BagLocalOperationExecutorContractTest` blob=`2ebf4997`，`BagServiceGuardedAdmissionTest` blob=`6916ab39`；Review #1 的 P1/P2 尚未闭合，不是 re-delivery。
- C 下一步继续两测试 repair 并在本卡 canonical re-deliver；Java/test writer active，不运行 Maven/runtime/input。

<!-- TRUE_EOF: TURN-40B-C2 PARENT-AMENDMENT-ACK+PHYSICAL-SOURCE-AUDIT OWNER-C SOURCE+TEST-REPAIR-ACTIVE COMMUNICATION-RECOVERED BAGSERVICE=CE0EA995/F094F67B VISIBILITY-SEAM-LANDED TESTS-OLD-BLOBS P1+P2-PENDING NO-DELIVERY NO-MAVEN 2026-07-18T18:43:00-04:00 -->

## EXTERNAL-C Amendment #1 Repair — Canonical Whole-Card Re-Delivery - 2026-07-18 18:48 EDT

- ack: 具名双 ACK `PARENT-C-TURN40B-C2-REVIEW1-REPAIR-1814` + `PARENT-C-TURN40B-C2-AMENDMENT1-1836`。Amendment 采纳并实施完毕；比我原提议 A 更窄（保 `MainBagSession` private/final 不动，仅三方法 private→protected），走真实继承路径证明。
- scope: 严格按 Amendment write-set —— **仅 3 文件改动**，C2 其余 24 delivered files byte-unchanged（下方 parity 重申其冻结 SHA）。零新增文件/协议/store/adapter/业务算法/production-only hook。零 Git mutation；无 Maven/runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。

### 1) Visibility-only amendment（`BagService.java`，body 逐字不变）
- `ensureBagOpened(BagLayout, TaskExecutionContext)` @:573、`closeBagIfNeeded(BagLayout, TaskExecutionContext)` @:747、`countItemUpToInOpenMainBag(Point, String, int, TaskExecutionContext)` @:1015 三方法 `private`→`protected`。实现体/参数/返回值/调用位置/异常/stop/input/order 语义逐字不变；`MainBagSession` final + private ctor + public surface 未动；`withMainBagOpen`/`withMainBagOpenExclusive`/`withMainBagOpenGuarded` 全体 byte-unchanged（仅被覆写的三 seam 由继承分派）。行数不变（可见性关键字替换）。
- fresh blob: `BagService.java` | git `f094f67b` / sha256-8 `CE0EA995` / 1487L（原 `52801876`/`49142188`/1487L）。

### 2) P1 executable route（`BagLocalOperationExecutorContractTest.java`）
- FakeBagService **不再 override `withMainBagOpenGuarded`**（删除返回 null 的旧桩），改为经 `super(new CountingCallbackInputSequences(), null×5)` 继承真实 `withMainBagOpenGuarded → withMainBagOpenExclusive → new MainBagSession(...)` 路径，仅 override 三个 protected seam（`ensureBagOpened` 返回可配置 anchor 并记 `open`；`closeBagIfNeeded` 记 `close`；`countItemUpToInOpenMainBag` 返回 deterministic `ItemCountResult` 并记 `count`）。
- 新 @Test `supplyCheckRunsOneGuardedSessionInFrozenOrderWithThreeFields`：`FakePlayerStateService`（记 `incense`、返回 refill=true）、`RecordingStopToken`（override `throwIfStopRequested` 记 `checkpoint` 不停）、deterministic `ItemCountResult(3,1)`。断言：**恰一次** guarded queue（`inputSeq.calls==1`）+ open==1 + close==1；**精确顺序** `[open, incense, checkpoint, count, close]`；count 用冻结 template/requiredCount；JSON 三字段 `incenseRefilled=true / firstPageIndex=1 / count=3`。
- 两既有 supply 测试改走同一真实路径：`supplyCheckWithNoSessionFailsClosedAsSessionUnavailable`（`openAnchor=null`→真实 exclusive body 返回 generic null→`BAG_SESSION_UNAVAILABLE`，open==1/close==0/lambda 未跑）；`aGuardedStopRequestIsTheSoleSiteMappedToTheTypedLocalStop`（传 pre-stopped token→真实 guarded 拒→executor 唯一 STOPPED 映射；open==0 证拒发生于任何输入前）。
- fresh blob: `BagLocalOperationExecutorContractTest.java` | git `7c8531a1` / sha256-8 `6BB8605C` / 456L / **11T**（原 `2ebf4997`/`A25E919C`/343L/10T；+1 supply-order test，fakes 改真实继承路径）。

### 3) P2 executable route（`BagServiceGuardedAdmissionTest.java`）
- 保留现有四 rejection/queue cases（identity / pre-stopped token / ordinary queue failure / stop-during-wait）逐字不变。新增同包 `OpenTrackingBagService extends BagService`（仅 override protected `ensureBagOpened`/`closeBagIfNeeded`，null anchor 模拟开包失败），经真实继承 `withMainBagOpenGuarded → withMainBagOpenExclusive` 驱动：
  - `ordinaryBagOpenFailureStaysNonStoppedGenericNull`：admission 通过 + token 未停 + `ensureBagOpened` 返回 null → generic null（非 STOPPED）；open==1 / close==0 / 操作未跑。
  - `successfulAdmissionOpensAndClosesTheBagExactlyOnce`：`ensureBagOpened` 返回 deterministic Point → 操作恰一次 + open==1 + close==1。
- 更新 class Javadoc：删除被 review 判定与交付披露矛盾的「success 由 FiveRing whole-task 证」旧断言，改为直证（Amendment#1 protected seam + 真实继承路径），零 live bag mechanics/window/input。
- fresh blob: `BagServiceGuardedAdmissionTest.java` | git `445a5bb0` / sha256-8 `1C45179A` / 195L / **6T**（原 `6916ab39`/`7A234509`/125L/4T；+2 P2 tests + tracking 子类）。

### Retained 24-file parity（byte-unchanged，Amendment 写集外，只读）
- 生产（12）：`TurnLocalOperation`(`3792c874`)、`TurnBagOperationArguments`(`f9c78eb7`)、`TurnProtocolValidator`(`3d5bd786`)、`TurnExecutionWindow`(`3ff7cda3`)、`LocalTurnActionExecutor`(`dfa450c2`)、`LocalServiceExecution`(`8bcb5512`)、`LocalServiceStepDispatcher`(`4d03e6b6`)、`BagLocalOperationExecutor`(`8a912b18`)、Cloud `WubeiTask`(`fdd2739c`)、`FiveRingTaskV2`(`215afcb8`)、`PlayerStateService`(`babf688d`)、`CloudBagLocalServiceClient`(`29d2385b`)。
- 测试（9 未改）：`TurnActionGoldenJsonTest`(`1887aa89`)、`TurnCoreProtocolGoldenJsonTest`(`0c9a1077`)、`TurnEnvelopeGoldenJsonTest`(`24743c4a`)、`TurnProtocolValidatorContractTest`(`fd087007`)、`CloudBagLocalServiceClientContractTest`(`38609d6a`)、`WubeiWholeTaskTurnContractTest`(`7168b543`)、`FiveRingWholeTaskTurnContractTest`(`b54396be`)、`LocalServiceStepDispatcherContractTest`(`7f8b40a3`)、`LocalTurnActionExecutorContractTest`(`4c2255e8`)。

### Self-check & 纪律
- 三改动文件单文件 javac parse（`-proc:none -implicit:none` 空 classpath）过滤空-classpath 符号噪声后**零纯语法错**；逐一核验新引入符号真实签名：`InputSequences(InputActionQueue)` 单参 ctor + `submitExclusiveAndWait(String,Supplier<Boolean>)` public 非 final 可 override；三 protected override 签名逐字匹配生产；`TaskStopToken` public 非 final、`throwIfStopRequested()` 可 override；`BagService.ItemCountResult(int,Integer)` public record；`java.awt.Point`。
- **named-test/compile 执行=shared-writer 延期声明**：C 为 active writer 期间不运行 Maven；授权 HTTPS turn named family + 双仓 compile aggregate gate 留待父级 stable window。`无已批准业务差异；按基线等价迁移`。零 Git mutation；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批、不建 reviewer。

<!-- TRUE_EOF: TURN-40B-C2 EXTERNAL-C AMENDMENT1-REPAIRED CANONICAL-REDELIVERY DOUBLE-ACK=1814+1836 VISIBILITY-ONLY-3-METHODS P1=SUPPLY-ONE-SESSION+ORDER+THREE-FIELDS-PROVEN P2=BAGOPEN-FAILURE-NONSTOPPED+SUCCESS-OPEN-CLOSE-ONCE-PROVEN BAGSERVICE=f094f67b/CE0EA995/1487 EXECTEST=7c8531a1/6BB8605C/456/11T ADMTEST=445a5bb0/1C45179A/195/6T 24-FILE-PARITY-RETAINED AWAIT-REVIEW2 NO-MAVEN 2026-07-18T18:48:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #2 - 2026-07-18 19:09 EDT

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`; C2 owner is released and this source gate is closed.
- review scope: all Review #1 findings, Amendment #1, the three fresh files (`BagService=f094f67b`, executor test=`7c8531a1`, guarded admission test=`445a5bb0`) and the disclosed 24-file byte-parity remainder.
- P1 closure: `supplyCheckRunsOneGuardedSessionInFrozenOrderWithThreeFields` invokes the real inherited `withMainBagOpenGuarded -> withMainBagOpenExclusive -> MainBagSession` path. Only the three approved protected seams are overridden. It proves one queue/session/open/close, exact `open -> incense -> captured-token checkpoint -> count -> close`, requested template/count, and JSON `incenseRefilled/firstPageIndex/count`.
- P2 closure: the four retained rejection/queue tests remain; deterministic null-anchor coverage proves ordinary bag-open failure returns generic null/non-STOPPED without close, and the successful path proves operation/open/close exactly once.
- production review: the three visibility changes are exactly Amendment #1 and method bodies/signatures/order are unchanged. No new protocol/store/adapter/helper, copied business algorithm, real input/runtime dependency or approved-business difference was introduced.
- build note: source review is passed independently of the shared aggregate build gate. Parent will run only the authorized HTTPS turn named family/applicable compile after confirming no Java writer is active.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-SOURCE+TEST-SOURCE-REVIEW-2 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED SOURCE-GATE-CLOSED AMENDMENT1-CLOSED SUPPLY-SESSION+ORDER+THREE-FIELDS-PROVEN BAGOPEN-FAILURE+SUCCESS-OPEN-CLOSE-PROVEN NO-BUSINESS-DIFFERENCE BUILD-GATE-PARENT 2026-07-18T19:09:00-04:00 -->

## Parent BUILD-EVIDENCE REVIEW #3 - 2026-07-18 19:11 EDT

- verdict superseding Review #2's release: `P0/P1/P2=0/1/0 / BLOCKED / REPAIR REQUIRED`; owner C is restored/retained because the authorized DHXY named family fails in `testCompile` on C2-owned test sources. No Cloud tests or compile were run after this failure.
- command: `mvn -q '-Dtest=TurnActionGoldenJsonTest,TurnCoreProtocolGoldenJsonTest,TurnEnvelopeGoldenJsonTest,TurnProtocolValidatorContractTest,LocalServiceStepDispatcherContractTest,LocalTurnActionExecutorContractTest,BagLocalOperationExecutorContractTest,BagServiceGuardedAdmissionTest' test` from `D:\mavenProject\DHXY-cr271`.
- direct C2 evidence:
  - `BagLocalOperationExecutorContractTest.java:429` calls the current 12-argument `PlayerStateService` constructor with 9 arguments.
  - `LocalServiceStepDispatcherContractTest.java:784` has the same stale 9-argument constructor call.
  - `LocalTurnActionExecutorContractTest.java:852-853` cannot resolve `BagLocalOperationExecutor` and `ObjectMapper`, and its fixture at `:889` also calls `PlayerStateService` with 9 arguments.
- unrelated historical test-source failures are also present, but they do not excuse these direct failures inside the frozen C2 test write set.
- repair condition: within the existing C2 test files only, add the real imports and align deterministic fake constructors with the current source signatures; do not change production behavior or weaken tests. Re-run physical evidence and canonical whole-card re-deliver. Parent will rerun the authorized family only after writer stability.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-BUILD-EVIDENCE-REVIEW-3 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 OWNER-C-RESTORED DIRECT-TESTCOMPILE-FAILURES=3-TESTS PLAYERSTATE-CTOR-STALE MISSING-IMPORTS NO-CLOUD-TEST NO-COMPILE 2026-07-18T19:11:00-04:00 -->

## EXTERNAL-C Review #3 Build-Evidence Repair — Canonical Whole-Card Re-Delivery - 2026-07-18 19:30 EDT

- ack: 具名 ACK `PARENT-C-TURN40B-C2-REVIEW3-TESTCOMPILE-REPAIR-1911`。父级实跑授权 named family 的 build-evidence Review #3（19:11，物理晚于 Review #2）`0/1/0 BLOCKED / OWNER-C RESTORED` 收悉全接受；我 19:16 CLOSED 事件 consumed Review #2 但漏此物理更晚的 Review #3，收口声明 stale 作废，owner-C 恢复，本轮起 repair active。
- root cause: 共享生产 writer 将 `com.bot.dhxy.service.PlayerStateService`（`@RequiredArgsConstructor`）由 9 个必填 final 字段扩为 **12 个**（当前 param 序：context/identityService/locationRadar/tracker/inputProvider/inputSequences/coordinateHelper/bagService/windowTaskContextHolder/config/windowScopedTempPath/sheyaoxiangStatusCloudDecisionService；`runtimeStates` 有初始化不入构造）。C2 三个 test 的 deterministic fake `super(null×9)` 因此对不上现签名 → `testCompile` 失败；另 `LocalTurnActionExecutorContractTest` 缺 `BagLocalOperationExecutor`/`ObjectMapper` import。
- repair（严格限 C2 既有 test 文件、零生产改动、不弱化测试；对齐当前源签名 + 补真实 import）：
  1. `BagLocalOperationExecutorContractTest.java`：`FakePlayerStateService` ctor `super(null×9)`→`super(null×12)`（:429）。
  2. `LocalServiceStepDispatcherContractTest.java`：`FakePlayerStateService` ctor `super(null×9)`→`super(null×12)`（:784）。**仅此一处**；同文件 `FakeUiCleanerService`（:801/`UICleanerService`）经核 `UICleanerService` 当前仍 9 个必填字段（第 10 个 `random` 有初始化），保持 `super(null×9)` 不动。
  3. `LocalTurnActionExecutorContractTest.java`：`InertPlayerStateService` ctor `super(null×9)`→`super(null×12)`（:889）+ 顶部补 `import com.bot.dhxy.cloud.turn.local.BagLocalOperationExecutor;` 与 `import com.fasterxml.jackson.databind.ObjectMapper;`（字母序，去重确认各 1）。
- fresh blobs（本轮 3 改动 test 文件）：
  - `BagLocalOperationExecutorContractTest.java` | git `ad2e9e9e` / sha256-8 `3C611875` / 457L / 11T（原 `7c8531a1`/456L/11T）。
  - `LocalServiceStepDispatcherContractTest.java` | git `9f1cb8f7` / sha256-8 `BC999461` / 885L / 19T（原 `7f8b40a3`/884L/19T）。
  - `LocalTurnActionExecutorContractTest.java` | git `f4e02881` / sha256-8 `1166E46D` / 1406L / 13T（原 `4c2255e8`/1403L/13T）。
- 不变文件：Amendment#1 三文件中 `BagService.java`（git `f094f67b`）与 `BagServiceGuardedAdmissionTest.java`（git `445a5bb0`，不引用 PlayerStateService）本轮 byte-unchanged；其余 22 delivered files parity 保持。Review#1/Amendment#1 的 P1（supply one-session/序/三字段）与 P2（bag-open-failure non-STOPPED + open/close-once）证明未动。
- self-check：三改动文件单文件 javac parse（`-proc:none -implicit:none` 空 classpath）严格 `error:` 过滤后**零纯语法错**；PlayerStateService 12 字段逐一核对（构造 param 12 = null×12 精确匹配）；UICleanerService 9 字段核对（:801 不动正确）；两新 import 去重=各 1。**未运行 Maven**（shared-writer 延期；父级在 writer 稳定后跑授权 named family/compile 物理复证）。零 Git mutation；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-40B-C2 EXTERNAL-C REVIEW3-BUILD-EVIDENCE-REPAIRED CANONICAL-REDELIVERY ACK=1911 PLAYERSTATE-CTOR-9-TO-12=3-FAKES MISSING-IMPORTS-ADDED=BAGLOCALEXEC+OBJECTMAPPER UICLEANER-9-UNCHANGED EXECTEST=ad2e9e9e/3C611875/457/11T DISPATCHTEST=9f1cb8f7/BC999461/885/19T LOCALTURNTEST=f4e02881/1166E46D/1406/13T P1+P2-PROVEN-UNCHANGED AWAIT-REVIEW4 NO-MAVEN 2026-07-18T19:30:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #4 + Build Recheck - 2026-07-18 19:33 EDT

- source verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`; owner C released. The three fake `PlayerStateService` constructors now match the current 12 required fields and `LocalTurnActionExecutorContractTest` has the two real imports. No production code or assertion was changed.
- authorized named-family recheck: the three direct C2 `testCompile` errors from Review #3 are absent. Maven still fails in global `testCompile` on unrelated historical tests outside C2, including stale `TaskMaintenanceService`/`SummonSkillService` constructors, old Xiuluo signatures, removed NPC tooltip helpers, old team-role constructors and old input-action result types. Therefore the named C2 tests did not execute and the aggregate test gate remains externally blocked; this does not return C2 to C.
- applicable compile: `mvn -q -DskipTests compile` from `D:\mavenProject\DHXY-cr271` exits 0. Cloud tests/compile were not run.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-SOURCE+TEST-SOURCE-REVIEW-4 PASSED P0=0 P1=0 P2=0 OWNER-C-RELEASED DIRECT-C2-TESTCOMPILE-ERRORS-CLEARED NAMED-FAMILY-NOT-EXECUTED GLOBAL-HISTORICAL-TESTCOMPILE-BLOCKED DHXY-MAIN-COMPILE-PASSED CLOUD-NOT-RUN 2026-07-18T19:33:00-04:00 -->

## Parent Review #4 ACK Closure Audit - 2026-07-18 19:49 EDT

- C 19:44 exactly ACKed `PARENT-C-TURN40B-C2-REVIEW4-PASSED-1938`, accepted owner release/no-reclaim, and confirmed no later review block existed. C2 remains closed; C is idle/available.
- build state unchanged: direct C2 testCompile defects are cleared, DHXY main compile passed, named family remains blocked before execution by unrelated global historical tests.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-REVIEW4-ACK-CLOSURE ACK=PARENT-C-TURN40B-C2-REVIEW4-PASSED-1938 CLOSED OWNER-C-RELEASED NO-RECLAIM C-IDLE DIRECT-C2-ERRORS-CLEARED GLOBAL-HISTORICAL-TESTCOMPILE-BLOCKED 2026-07-18T19:49:00-04:00 -->

## Parent BUILD REGRESSION DISCOVERY #5 - 2026-07-19 01:25 EDT

- state: `DIRECT ISOLATED TEST REGRESSION / ROOT-CAUSE AUDIT REQUIRED / ZERO OWNER / NOT CLAIMABLE YET`.
- evidence: TURN-39K's isolated run of `LocalTurnActionExecutorContractTest` reports 13/14; the sole failure is C2's
  `queueOwningBagStepWithATaskHandleReplacedAfterResolveYieldsTypedStoppedWithZeroInput` at current line 274:
  expected typed `STOPPED`, actual `FAILED`. The harness records zero mouse queue submissions, and the failure existed
  before and after 39K's test-only frozen queue-double repair, so it is not a 39K keyboard/mouse regression.
- boundary: this reopens only C2 build evidence, not 39K scope and not C2 source semantics by inference. Parent must
  audit the current `LocalTurnActionExecutor -> LocalServiceStepDispatcher -> BagLocalOperationExecutor -> BagService`
  path and harness handle-call sequence before freezing a repair write set. No Worker is assigned or authorized to
  edit bag production/test from this entry.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-BUILD-REGRESSION-DISCOVERY5 DIRECT-ISOLATED-RED EXPECTED-STOPPED-ACTUAL-FAILED TEST=QUEUE-OWNING-BAG-HANDLE-REPLACED ZERO-MOUSE-QUEUE NOT-39K ROOT-CAUSE-AUDIT-REQUIRED ZERO-OWNER-NOT-CLAIMABLE 2026-07-19T01:25:00-04:00 -->

## Parent Plan-Contract Repair #6 - READY / ZERO OWNER - 2026-07-18 23:02 UTC

- root cause: `TurnExecutionWindow.resolveForAction()` currently reads `runner.getCurrentTask()` twice during one
  resolve. The first read is hidden in `isStopRequested(runner, context)` while constructing metadata; the second
  read freezes `actionTaskHandle`. In the existing replacement-race harness, the first read returns the original
  handle and the second returns its successor. The snapshot therefore freezes the successor, so the later live
  identity predicate incorrectly passes and the queue-owning bag path returns generic `FAILED` instead of typed
  `STOPPED`. The later predicate read in `isActionTaskStillCurrent()` is correct and must remain live.
- source-truth ruling: this is a single-snapshot consistency defect in the existing C2 capture-at-resolve contract,
  not a bag algorithm, input queue, keyboard/mouse, or 39K regression. It can be closed without a business choice.
  `无已批准业务差异；按基线等价迁移`.
- frozen repair production write set, DHXY-cr271 only:
  `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java` (`FA1562D6...`, 220 lines). Resolve the current
  `RunningTaskHandle` exactly once after the runner/context/binding are established; derive metadata stop state,
  `actionTaskHandle`, stop token and pause token from that same reference. Change the existing private
  `isStopRequested` parameter to consume the captured handle (or inline the same logic). Do not add a store, TTL,
  owner, wrapper chain, retry, successor lookup, protocol change, or business/input behavior.
- frozen test gate: READ-ONLY
  `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
  (`16B93D61...`, 1536 lines). Its existing
  `queueOwningBagStepWithATaskHandleReplacedAfterResolveYieldsTypedStoppedWithZeroInput` must change from isolated
  13/14 red to 14/14 green with typed `STOPPED`, zero mouse queue submissions and zero capture. No test rewrite is
  needed or authorized unless a new source-truth blocker is first recorded here.
- applicable gate after a stable writer window: authorized HTTPS turn named family containing
  `LocalTurnActionExecutorContractTest` plus DHXY compile. Existing unrelated global testCompile debt remains a
  separate aggregate blocker; no runtime/application/server/Task/UI/capture/input and no Git mutation.
- canonical state: `READY / ZERO OWNER / UNASSIGNED / WHOLE-CARD REPAIR`; public pool only. No Worker is assigned,
  scheduled, reserved, or chased; any eligible Worker must reread this physical EOF and append the sole canonical
  whole-card claim before editing.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-PLAN-CONTRACT-REPAIR6 READY ZERO-OWNER UNASSIGNED ROOT-CAUSE=RESOLVE-READS-CURRENT-HANDLE-TWICE SINGLE-SNAPSHOT-HANDLE+STOP+PAUSE FIX=TURNEXECUTIONWINDOW-ONLY TEST=LOCALTURNACTIONEXECUTOR-READONLY-14OF14 TYPED-STOPPED ZERO-MOUSE ZERO-CAPTURE NO-BUSINESS-DIFFERENCE NO-MAVEN NO-RUNTIME 2026-07-18T23:02:27Z -->

## WHOLE-CARD CLAIM - EXTERNAL-A - 2026-07-19T10:32:00-04:00

- owner: `EXTERNAL-A`
- claim_type: canonical whole-card claim (append at physical EOF per card authority; public pool, any eligible Worker).
- card: `TURN-40B-C2` — Plan-Contract Repair #6 (single-snapshot handle consistency, WHOLE-CARD REPAIR).
- basis: card physical EOF = `READY / ZERO OWNER / UNASSIGNED / public pool only` (PARENT-PLAN-CONTRACT-REPAIR6,
  2026-07-18T23:02:27Z). Parent informational (non-assignment) message `PARENT-A-TURN40B-C2-PLANREPAIR6-2302` was
  directed to EXTERNAL-A and says "continue canonical pool rules; no reclaim required". A is eligible and best-suited:
  the frozen write set `TurnExecutionWindow.java` is A's own TURN-39K deliverable (actionStopToken/actionPauseToken
  frozen at resolve); this repair extends that single-resolve capture. Pre-check done as an independent tool call
  before this append: full card read + mtime (2026-07-18 19:05:54) + current file SHA-256 verified = frozen
  `FA1562D6` (220 lines), card EOF confirmed still UNASSIGNED with no earlier claim.
- write_set (frozen, DHXY-cr271 only): production MODIFY `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java`
  (`FA1562D6`/220L) — resolve the current `RunningTaskHandle` exactly once after runner/context/binding established;
  derive metadata stop state, `actionTaskHandle`, stop token and pause token from that one reference; change the
  private `isStopRequested` to consume the captured handle (or inline). Keep `isActionTaskStillCurrent()`'s later
  live read live. NO store/TTL/owner/wrapper/retry/successor-lookup/protocol/business/input change. Test gate
  READ-ONLY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java` (`16B93D61`/1536L):
  `queueOwningBagStepWithATaskHandleReplacedAfterResolveYieldsTypedStoppedWithZeroInput` must go isolated 13/14 →
  14/14 green (typed STOPPED, zero mouse queue, zero capture). No test rewrite authorized.
- discipline: zero Git mutation; `无已批准业务差异；按基线等价迁移`; named-family recheck only after stable writer
  window (isolate-run the one named test; global historical testCompile debt stays a separate aggregate blocker,
  not claimed as passed); no runtime/UI/capture/input; `D:\mavenProject\DHXY` untouched; not touching 38-family.
- next: read-back this physical EOF to confirm sole earliest owner; if any physically-earlier claim exists, canonical
  self-withdraw. Then implement the single-snapshot fix and isolate-run the named test.

<!-- TRUE_EOF: TURN-40B-C2 WHOLE-CARD CLAIM EXTERNAL-A OWNER SINGLE-SNAPSHOT-HANDLE-FIX TURNEXECUTIONWINDOW-ONLY A-39K-FILE ANTI-RACE-CARD-EOF PRECHECK-SHA-FA1562D6-MATCH 2026-07-19T10:32:00-04:00 -->

## SOURCE+TEST DELIVERED - EXTERNAL-A - 2026-07-19T10:37:00-04:00

- card: `TURN-40B-C2` — Plan-Contract Repair #6 (single-snapshot handle consistency).
- owner: `EXTERNAL-A` (canonical whole-card claim 10:32; sole owner, read-back confirmed; no competing claim).
- production write set — COMPLETE (`TurnExecutionWindow.java` only): implemented the single-snapshot resolve.
  `resolveForAction()` now reads `runner.getCurrentTask()` **exactly once** (the `actionTaskHandle` capture) and
  derives all three consistency-sensitive fields from that one reference: metadata stop state via
  `isStopRequested(actionTaskHandle, context)`, the frozen `actionTaskHandle`, and the stop + pause tokens. The
  private `isStopRequested` signature changed from `(WindowTaskRunner runner, …)` to `(RunningTaskHandle task, …)`,
  consuming the captured handle instead of doing its own hidden `getCurrentTask()` read (the old first read). The
  live identity predicate `isActionTaskStillCurrent()` is UNCHANGED — it still reads `runner.getCurrentTask()` at
  call time and compares to the captured reference (the card requires this later read stay live). No store/TTL/
  owner/wrapper/retry/successor-lookup/protocol/business/input change. `无已批准业务差异；按基线等价迁移`.
  Net: `getCurrentTask()` reads in the file = 2 (one single-snapshot at resolve, one live predicate); previously 3
  (metadata-hidden + freeze + predicate), the first two of which could split across a mid-resolve replacement.
- new production SHA-256 = `77692F3F` (222 lines, was `FA1562D6`/220). Test gate READ-ONLY, UNCHANGED at frozen
  `16B93D61` (verified; only compiled + executed, never edited).
- **VERIFIED**: (1) authorized DHXY compile `mvn -q -DskipTests compile` from DHXY-cr271 = **EXIT 0**; (2) isolate-run
  of the authorized named test `LocalTurnActionExecutorContractTest` (junit-platform-console-standalone 1.10.2,
  compiled against refreshed `target/classes` + deps) = **14/14 PASSED, 0 failed** — the previously isolated-red
  `queueOwningBagStepWithATaskHandleReplacedAfterResolveYieldsTypedStoppedWithZeroInput` now GREEN with typed
  `STOPPED`, zero mouse queue submissions, zero capture (13/14 → 14/14 as the card requires).
- build gate: the authorized named-family test + DHXY compile both pass. Existing unrelated **global** `testCompile`
  debt (stale historical tests outside C2) remains a separate aggregate blocker per the card, so it was not run as a
  whole suite; no Maven family is claimed globally green. Zero Git mutation; no runtime/UI/capture/input;
  `D:\mavenProject\DHXY` untouched; only `TurnExecutionWindow.java` edited (other worktree-dirty files are prior
  uncommitted deliverables, not touched).

<!-- TRUE_EOF: TURN-40B-C2 SOURCE+TEST DELIVERED EXTERNAL-A SINGLE-SNAPSHOT-HANDLE-FIX TURNEXECUTIONWINDOW-77692F3F-222L TEST-READONLY-16B93D61-UNCHANGED NAMED-TEST-14of14-PASSED TYPED-STOPPED-ZERO-INPUT DHXY-COMPILE-EXIT0 GLOBAL-TESTCOMPILE-SEPARATE-BLOCKER NO-BUSINESS-DIFF 2026-07-19T10:37:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #7 + Build Recheck - 2026-07-18 23:22 UTC

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`; External A owner is released and Repair #6 is
  source/test closed.
- source review: `TurnExecutionWindow.resolveForAction()` now captures `runner.getCurrentTask()` exactly once for
  resolve-time metadata, frozen handle, stop token and pause token. The later `isActionTaskStillCurrent()` call-time
  read remains live. The private helper consumes the captured handle; no second store, TTL, retry, wrapper chain,
  successor lookup, protocol/input/business change, or baseline-696 difference was introduced.
- byte gate: production SHA is `77692F3F` (222 lines); read-only
  `LocalTurnActionExecutorContractTest.java` remains frozen `16B93D61` (1536 lines).
- parent physical verification: `mvn -q -DskipTests compile` exits 0. Parent then executed the freshly compiled
  `LocalTurnActionExecutorContractTest` with JUnit Platform Console 1.10.2: 14/14 tests passed, including the former
  replacement-race red, which now yields typed `STOPPED`, zero mouse queue submissions and zero capture.
- remaining build note: unrelated historical global `testCompile` debt remains a separate aggregate blocker; it
  does not reopen C2. No runtime/application/server/Task/UI/capture/input or Git mutation occurred.

<!-- TRUE_EOF: TURN-40B-C2 PARENT-SOURCE+TEST-SOURCE-REVIEW-7 PASSED P0=0 P1=0 P2=0 OWNER-A-RELEASED REPAIR6-CLOSED PROD-77692F3F TEST-READONLY-16B93D61 PARENT-COMPILE-EXIT0 PARENT-NAMED-14of14-PASSED TYPED-STOPPED ZERO-MOUSE ZERO-CAPTURE GLOBAL-TESTCOMPILE-SEPARATE-BLOCKER NO-BUSINESS-DIFFERENCE 2026-07-18T23:22:27Z -->
