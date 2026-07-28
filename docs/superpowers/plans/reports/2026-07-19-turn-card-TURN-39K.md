# TURN-39K - Exact-HWND Background Keyboard Capability

## Canonical Status

- status: `READY / ZERO OWNER`
- type: `INTEGRATION / SOURCE+TEST`
- dependsOn: `TURN-39P1 Review #15 PASSED` and the user's confirmed invariant on 2026-07-18:
  **all keyboard input is exact-HWND background input; only mouse input may require foreground**.
- claim rule: the first Worker that can finish the whole card must append a canonical whole-card claim at this
  physical EOF before editing. The ledger does not assign this card.

## Approved Runtime Invariant

- Keyboard never focuses a window and never calls real/focused `InputProvider` keyboard methods.
- Every keyboard operation targets the action's frozen `WindowNativeBinding` HWND. Unsupported delivery is a
  typed terminal failure before focus or desktop input; there is no foreground fallback.
- Keyboard actions execute directly on each window's turn thread and may run in parallel across different HWNDs.
  They do not enter or wait for the global mouse/input worker.
- Mouse keeps the existing foreground, exact-window, globally serialized queue mechanics. A turn containing both
  mechanics preserves its step order, but only its mouse steps enter that queue.
- Preserve original per-window action order, delays, stop/pause/drift checks and COMPLETED-only truth. Do not create
  a keyboard queue/store, retry path, clipboard authority, protocol or copied business algorithm.

## Frozen Production Write Set

All paths are in `D:\mavenProject\DHXY-cr271`:

1. `src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`
2. `src/main/java/com/bot/dhxy/cloud/turn/TurnKeyMapper.java`
3. `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`

`InputProvider.java`, `InputActionWorker.java`, `InputActionQueue.java`, `TurnInputActionMapper.java`, and both
repositories' `TurnInputAction.java`/`TurnProtocolValidator.java` are READ-ONLY. The existing protocol already
expresses `KEY_TAP`, `KEY_DOWN`, `KEY_UP` and `TEXT_INPUT`; do not add `PRESS_CTRL_A` to the wire enum.

## Required Mechanical Closure

- Exact-binding background delivery covers all active forms needed by the frozen callers: all existing Alt
  shortcuts, Ctrl down/up, Ctrl+U, Ctrl+A, Unicode text and Enter.
- `TurnKeyMapper` maps accepted wire key spellings to the closed exact-HWND background operation vocabulary.
- `TurnInputStepExecutor` executes keyboard steps directly through `BoundWindowKeyboardService` on the calling
  window turn thread. It must not submit a keyboard step to `InputActionQueue`, acquire the global input lock, or
  focus any window. Two different window bindings must be able to reach background delivery concurrently.
- Mouse mapping, queue submission, coordinates and timing remain byte-equivalent in `TurnInputActionMapper` and
  the existing mouse branch of `TurnInputStepExecutor`.
- Legacy `PASTE_TEXT` has no active business caller in this card. It must never fall back to foreground; retain a
  typed terminal rejection unless an existing background-equivalent implementation is proven without adding a
  clipboard authority.

## Frozen Test Write Set

1. `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
2. `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`

Required proofs: zero queue submission/focus/`InputProvider` call for keyboard; exact frozen binding; all active
Alt/Ctrl/text/Enter forms; two-window keyboard concurrency with a deterministic barrier; same-window turn-step order;
pause/stop/drift before irreversible post; unsupported forms fail without foreground fallback; mouse behavior and
global serialization unchanged.

## Build Gate

- Authorized family only: the two named tests above plus applicable `mvn -q -DskipTests compile` from
  `D:\mavenProject\DHXY-cr271` after Java writers are stable.
- No Cloud source change or Cloud compile gate belongs to 39K.
- No runtime/application/server/Task/UI/capture/input execution.

## Behavior Difference Record

User-approved delivery-mechanics correction: keyboard delivery changes from any focused/real-input or globally
serialized path to per-window parallel exact-HWND background delivery. Only mouse may require foreground and global
serialization. Business decisions, per-window caller order, delays, retry/fallback order outside keyboard delivery,
and result truth remain baseline-equivalent.

<!-- TRUE_EOF: TURN-39K READY ZERO-OWNER WHOLE-CARD PER-WINDOW-PARALLEL-EXACT-HWND-BACKGROUND-KEYBOARD ONLY-MOUSE-FOREGROUND+GLOBAL-SERIAL 3-PRODUCTION 2-TEST NO-KEYBOARD-QUEUE NO-FOREGROUND-KEYBOARD-FALLBACK NO-WIRE-ENUM-CHANGE NO-CLOUD-WRITE 2026-07-19T00:34:00-04:00 -->

## WHOLE-CARD CLAIM - EXTERNAL-A - 2026-07-19T00:17:00-04:00

- owner: `EXTERNAL-A`
- claim_type: canonical whole-card claim (append at physical EOF per card claim rule)
- card: `TURN-39K` — Exact-HWND Background Keyboard Capability
- basis: card status `READY / ZERO OWNER`; parent PLAN-CONTRACT CORRECTION 2026-07-19T00:34 removed the false
  user foreground-keyboard gate; message `PARENT-A-TURN39K-READY-BACKGROUND-PARALLEL-0034` (plan correction, open
  anti-race). Pre-check: full card read + mtime (2026-07-18 12:42:38) done as independent tool call before this append.
- write_set (frozen, DHXY-cr271 only): `driver/BoundWindowKeyboardService.java`, `cloud/turn/TurnKeyMapper.java`,
  `cloud/turn/TurnInputStepExecutor.java` (prod); `cloud/turn/TurnInputStepExecutorContractTest.java`,
  `cloud/turn/LocalTurnActionExecutorContractTest.java` (test). READ-ONLY: InputProvider/InputActionWorker/
  InputActionQueue/TurnInputActionMapper + both repos' TurnInputAction/TurnProtocolValidator. No wire enum change,
  no Cloud write.
- discipline: zero Git mutation; keyboard = per-window parallel exact-HWND background (no queue/focus/foreground
  fallback); mouse path byte-equivalent; no stub/second-store/copied-algorithm; authorized build family only.
- next: read-back this EOF to confirm sole earliest owner; if a physically-earlier claim exists, canonical self-withdraw.

<!-- TRUE_EOF: TURN-39K CLAIM EXTERNAL-A OWNER WHOLE-CARD 3PROD+2TEST ANTI-RACE-CARD-EOF ACK=PARENT-A-TURN39K-READY-BACKGROUND-PARALLEL-0034 2026-07-19T00:17:00-04:00 -->

## EXTERNAL-C WHOLE-CARD CLAIM - 2026-07-19T00:36:00-04:00

- owner: `EXTERNAL-C`（heartbeat `778801ea`）。canonical whole-card claim，append 于本卡 physical EOF（claim rule §9-10）。
- ack: 收悉父级 `PARENT-C-TURN39K-READY-BACKGROUND-PARALLEL-0034`（plan-correction，非 assignment；TURN-39K READY/ZERO-OWNER，anti-race card-EOF claim）。
- precheck: append 前读本卡 physical EOF = `READY ZERO-OWNER`，无既有 owner/claim；`git status` 由父级 00:34 核为 clean。
- scope 接受: 3 生产（`driver/BoundWindowKeyboardService.java`、`cloud/turn/TurnKeyMapper.java`、`cloud/turn/TurnInputStepExecutor.java`）+ 2 test（`cloud/turn/TurnInputStepExecutorContractTest.java`、`cloud/turn/LocalTurnActionExecutorContractTest.java`）。READ-ONLY：InputProvider/InputActionWorker/InputActionQueue/TurnInputActionMapper + 双仓 TurnInputAction/TurnProtocolValidator；不加 PRESS_CTRL_A wire enum、不加 keyboard queue/store/retry/clipboard/protocol/copied-algorithm。
- 合同要点接受: keyboard = exact-HWND 后台交付，直接跑在每 window turn thread，可跨 HWND 并行，永不 focus/入全局 mouse-input worker/无 foreground fallback；unsupported=typed terminal failure；mouse 保持 foreground+global-serial 逐字不变；同 turn 步序保留（仅 mouse 步入队）；per-window 顺序/delay/stop-pause-drift/COMPLETED-only truth 保持；PASTE_TEXT 无活跃 caller，保 typed terminal rejection 不回退 foreground。
- 纪律: 零 Git mutation；其它 Java writer 活动时不运行 Maven（javac 单文件 parse 除外）；DHXY 只读；无 runtime/UI/capture/input；不自批、不建 reviewer。
- next: 若 append 后回读本卡 EOF 我的 claim 在物理 EOF 且无更早并发 claim → 确立 owner，逐文件 physical 取证 recon 后实施；若发现更早并发 claim（如 EXTERNAL-A）→ 立即 canonical 自撤让位。

<!-- TRUE_EOF: TURN-39K EXTERNAL-C WHOLE-CARD-CLAIM OWNER-C ACK=0034 PRECHECK-READY-ZERO-OWNER 3PROD+2TEST-ACCEPTED ANTI-RACE-EOF-REREAD-PENDING HEARTBEAT-778801ea 2026-07-19T00:36:00-04:00 -->

## EXTERNAL-C CANONICAL SELF-WITHDRAW - 2026-07-19T00:38:00-04:00

- action: **撤回上方 EXTERNAL-C 00:36 claim，让位 EXTERNAL-A**。
- reason: append 后 anti-race 回读本卡发现 `## WHOLE-CARD CLAIM - EXTERNAL-A - ...00:17`（line 76）**物理早于**我的 00:36 claim（line 95）。按 anti-race 铁律（双 claim 靠 physical-append-order 裁，不信自署 ts；发现更早 claim 立即 canonical 自撤），A 为唯一最早 owner。我预检时（74 行卡 EOF=READY ZERO-OWNER）A claim 尚未落盘，系预检与 append 间并发 append 的正常竞态，A 物理先落=owner。
- effect: EXTERNAL-C 对 TURN-39K **零所有权、零写入**（本卡仅 append 了 claim+自撤两条 markdown，无任何 Java/test 改动）。TURN-39K owner = **EXTERNAL-A**。
- next: EXTERNAL-C 回 IDLE available，不 reclaim TURN-39K，不干扰 A 的写集。
- 纪律: 零 Git mutation；DHXY 只读；无 Maven/runtime/UI/capture/input；不自批、不建 reviewer。

<!-- TRUE_EOF: TURN-39K EXTERNAL-C SELF-WITHDRAW YIELD-TO-EXTERNAL-A-PHYSICALLY-EARLIER-CLAIM OWNER=EXTERNAL-A C-ZERO-OWNERSHIP-ZERO-WRITE C-IDLE HEARTBEAT-778801ea 2026-07-19T00:38:00-04:00 -->

## PARENT CANONICAL CLAIM AUDIT - 2026-07-19T00:45:00-04:00

- canonical_owner: `EXTERNAL-A`。裁决依据仅为本卡 physical append order：A claim block 物理早于 C claim，
  C 已在随后 physical EOF 明确 self-withdraw；A claim 自署 `00:17` 与其引用的 00:34 父级消息时间不一致，
  该自署时间不作为 owner 依据，也不改变物理先后裁决。
- status: `CLAIMED / EXTERNAL-A SOLE OWNER / SOURCE UNCHANGED / STATUS EVENT STALE`。本轮实测三 production
  与两 test SHA/mtime 均未因领取变化；未达到独立 active-stale 裁决门。
- communication: A/C 均在本卡写了 ACK/claim 结果，但没有按强制双向协议在 ledger 追加下一轮
  `STATUS EVENT`；连续两轮后父级已标 communication/status stale，并在 ledger EOF 发定向修复消息。
- next: A 保持完整卡 sole owner，下一 ledger event 具名 ACK 并报告当前 source 方法/SHA/mtime；C 保持 idle、
  不 reclaim。键盘并行后台与鼠标前台全局串行合同不变。

<!-- TRUE_EOF: TURN-39K PARENT-CANONICAL-CLAIM-AUDIT OWNER=EXTERNAL-A PHYSICAL-EARLIEST C-SELF-WITHDRAWN SOURCE-UNCHANGED STATUS-EVENT-STALE COMMUNICATION-STALE NO-MAVEN NO-RUNTIME 2026-07-19T00:45:00-04:00 -->

## PARENT COMMUNICATION-RACE CORRECTION - 2026-07-19T00:46:00-04:00

- supersedes: 上方 00:45 的 `STATUS EVENT STALE / COMMUNICATION_STALE` 判断。父级 append 与 A/C ledger
  `STATUS EVENT` 并发；总账 physical EOF 现证明 A/C 均已具名 ACK 0034，故 communication normal，0045
  新消息只计 pending round 1。
- owner/source: A sole owner 不变并已进入 source recon；C self-withdraw/idle 不变。A 尚未产生新的 Java/test
  字节，已精确识别 KEY_DOWN/KEY_UP/TEXT_INPUT 接线缺口。
- collision correction: `LocalTurnActionExecutorContractTest` 在 39K 领取前已有 TURN-40B-C2 passed 累积
  delta（当前 blob `f4e02881...`）；父级 00:34“所有 39K 路径 clean”说明不准确。该字节属于合法前置累积，
  A 必须保留并在其上扩写，不得回滚。

<!-- TRUE_EOF: TURN-39K PARENT-COMMUNICATION-RACE-CORRECTION SUPERSEDES-0045-STALE A+C-ACK-0034 COMMUNICATION-NORMAL 0045-PENDING-R1 OWNER=A SOURCE-RECON C-IDLE C2-TEST-DELTA-PRESERVE NO-MAVEN 2026-07-19T00:46:00-04:00 -->

## PARENT SOURCE PROGRESS AUDIT - 2026-07-19T00:52:00-04:00

- owner/status: `EXTERNAL-A / SOURCE_ACTIVE / FIRST_PRODUCTION_DELTA`；C self-withdraw/idle 不变。
- new bytes: `BoundWindowKeyboardService.java`=`0c29980c5bd111b9f9ae1c5bfe99bd8bd45a796c`，新增 exact-binding
  Ctrl+A/Ctrl+U、Enter 与 ordered `WM_CHAR` Unicode 投递；`TurnKeyMapper.java`=
  `57d9a645b15981ac5f06beb8b8601ce5fee060ae`，新增 closed Ctrl chord/Enter/modifier mapping。
- pending bytes: `TurnInputStepExecutor.java` 与 `TurnInputStepExecutorContractTest.java` 尚未变；
  `LocalTurnActionExecutorContractTest.java=f4e02881...` 仍是领取前 C2 累积 delta，必须保留。
- gate: 当前仅记录 source progress，不构成 delivery/review。Java writer active，禁止 Maven/JUnit/compile；
  未运行 runtime/application/server/Task/UI/capture/input。

<!-- TRUE_EOF: TURN-39K PARENT-SOURCE-PROGRESS OWNER=A FIRST-PRODUCTION-DELTA BOUND-KEYBOARD=0c29980c TURN-KEY-MAPPER=57d9a645 EXECUTOR+TESTS-PENDING C2-DELTA-PRESERVED JAVA-WRITER-ACTIVE NO-MAVEN 2026-07-19T00:52:00-04:00 -->

## PARENT SOURCE PROGRESS AUDIT #2 - 2026-07-19T00:53:00-04:00

- third production delta: `TurnInputStepExecutor.java=77f184a181c47667c91e12afa27d2fe1dad6b784`；
  KEY_TAP/DOWN/UP/TEXT_INPUT 现直接在 calling window turn thread 路由 exact-HWND background service，
  keyboard 不提交 mouse queue、不 focus；现有 mouse branch 未改。
- current set: 三 production 均有增量；`TurnInputStepExecutorContractTest` 尚未变，
  `LocalTurnActionExecutorContractTest=f4e02881...` 仍仅为 C2 累积 delta。无 delivery/review。
- communication/build: C 已双 ACK 0034+0045 并 idle/no-reclaim；A communication normal，0045+0052 待 ACK。
  Java writer active，未运行 Maven/JUnit/compile/runtime/input。

<!-- TRUE_EOF: TURN-39K PARENT-SOURCE-PROGRESS2 OWNER=A THREE-PRODUCTION-DELTAS EXECUTOR=77f184a1 KEYBOARD-DIRECT-EXACT-HWND MOUSE-BRANCH-UNCHANGED TESTS-PENDING C-ACK-0034+0045 JAVA-WRITER-ACTIVE NO-MAVEN 2026-07-19T00:53:00-04:00 -->

## PARENT BUILD-STATE AUDIT - 2026-07-19T00:54:00-04:00

- worker_report: A 报告三 production 实现完成，并运行卡片授权的 DHXY
  `mvn -q -DskipTests compile`，exit 0。此为 production 稳定点的 intermediate compile。
- remaining: 两 named test 尚无本卡新字节；A 进入 test writer 阶段。整卡仍 `SOURCE_ACTIVE`，未 canonical
  delivery，不触发父级 source review。
- communication: A normal，0045+0052 因并发事件 pending round 1；C 双 ACK、idle/no-reclaim。
- safety: 父级未运行 Maven/JUnit/compile；无 runtime/application/server/Task/UI/capture/input。

<!-- TRUE_EOF: TURN-39K PARENT-BUILD-STATE-AUDIT OWNER=A PRODUCTION-DONE INTERMEDIATE-DHXY-COMPILE-GREEN TESTS-ACTIVE NOT-DELIVERED A-COMM-NORMAL C-IDLE NO-RUNTIME 2026-07-19T00:54:00-04:00 -->

## PARENT TEST PROGRESS AUDIT - 2026-07-19T00:58:00-04:00

- test-1: `TurnInputStepExecutorContractTest` 已产生 39K 增量，正在加入 unsupported key/text negatives、
  exact background call recording 与 deterministic two-window `CyclicBarrier` concurrency proof；文件仍在写入。
- test-2: `LocalTurnActionExecutorContractTest` 尚无 39K 新增量，仅保留 C2 累积 delta。
- status/gate: production done/intermediate compile green 不变；test writer active，未 canonical delivery，父级不做
  source review、不运行 Maven/JUnit/compile；无 runtime/input。

<!-- TRUE_EOF: TURN-39K PARENT-TEST-PROGRESS OWNER=A TEST1-ACTIVE NEGATIVES+BACKGROUND-RECORDING+TWO-WINDOW-BARRIER TEST2-PENDING NOT-DELIVERED PARENT-NO-MAVEN NO-RUNTIME 2026-07-19T00:58:00-04:00 -->

## OWNER PROGRESS - EXTERNAL-A - 2026-07-19T00:31:00-04:00

- SOURCE done (3/3 prod), `mvn -q -DskipTests compile` exit 0:
  - `BoundWindowKeyboardService`: `ControlShortcut{CTRL_A,CTRL_U}` + `pressControlShortcut`/`pressEnter`/`typeUnicodeText`
    (exact-HWND background PostMessage/WM_CHAR; explicit frozen binding; no lock/focus/foreground fallback; no clipboard).
  - `TurnKeyMapper`: `findControlShortcut`/`isEnterKey`/`findModifierKey` closed-vocabulary resolvers; Alt resolver unchanged.
  - `TurnInputStepExecutor`: keyboard switch — KEY_TAP(Alt→Ctrl→Enter), KEY_DOWN/UP→`transitionModifier`,
    TEXT_INPUT→`typeUnicodeText`; mouse/queue branch byte-unchanged; no wire-enum/Cloud change.
- TEST 1/2 `TurnInputStepExecutorContractTest`: **16/16 PASSED** (new: Ctrl/Enter/Unicode forms; KEY_DOWN/UP modifier
  order; two-window CyclicBarrier concurrency; stop-before-keyboard; reframed unsupported→no foreground fallback).
  Existing mouse/queue/drift/stop tests all still green (mouse behavior unchanged).
- TEST 2/2 `LocalTurnActionExecutorContractTest`: pending (same-window mixed step order + global-serialization-unchanged),
  will EXTEND the existing PASSED TURN-40B-C2 delta in the worktree (no rollback).
- BUILD-GATE QUESTION TO PARENT: the card `mvn -Dtest=... test` gate cannot execute because whole-tree testCompile
  fails on UNRELATED dirty test files from other in-flight cards (`TaskMaintenanceCR138LocalSupportCapabilityTest`,
  `DialogMaintenanceLightweightFallbackPolicyWiringTest`, `XiuluoCR84RouteStateModelTest`,
  `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`, `InputActionPauseCancellationGuardTest`) — not in this write set,
  not touched. Evidence produced instead: main compile exit 0 + isolated `junit-platform-console-standalone` run of the
  named test class (16/16). Is isolate-run evidence acceptable for this card, or should the named gate be re-run after
  the tree stabilizes? Question mirrored in ledger.

<!-- TRUE_EOF: TURN-39K OWNER-A SOURCE-DONE COMPILE-GREEN TEST-1OF2-16PASS TEST-2OF2-PENDING MVN-NAMED-GATE-BLOCKED-BY-UNRELATED-DIRTY-TESTCOMPILE 2026-07-19T00:31:00-04:00 -->

## PARENT BUILD-GATE ADJUDICATION - 2026-07-19T01:13:00-04:00

- decision: 隔离 `junit-platform-console-standalone` 对唯一 named class 的 16/16 green，可作为本卡 test-1
  source-review evidence；它不等价于 Maven named-test gate passed。
- delivery: A 完成 test-2 并以同等隔离范围取证后，可以 canonical `SOURCE+TEST DELIVERED`，父级立即做
  3 production+2 test 全文件 source review。delivery/build 状态必须明确写
  `MAVEN NAMED TEST BLOCKED/PENDING BY OUT-OF-WRITE-SET DIRTY TESTCOMPILE`。
- final build gate: 等共享 test tree 稳定后再补跑卡内两 named Maven tests；在此之前不得宣称 named Maven
  tests passed。禁止修改、删除或回滚列出的五个无关 dirty tests 来规避阻断。
- owner/next: A sole owner 不变，继续 test-2；0045+0052 尚待 ledger ACK。父级未运行 Maven/runtime/input。

<!-- TRUE_EOF: TURN-39K PARENT-BUILD-GATE-ADJUDICATION ISOLATED-TEST1-16OF16-ACCEPTED-FOR-SOURCE-EVIDENCE NOT-MAVEN-PASS TEST2-THEN-SOURCE-DELIVERY-ALLOWED MAVEN-NAMED-GATE-BLOCKED-PENDING NO-TOUCH-UNRELATED-TESTS OWNER=A 2026-07-19T01:13:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR #1 - 2026-07-19T01:17:00-04:00

- adjudication: 批准在本卡已冻结的 `LocalTurnActionExecutorContractTest` 内升级既有
  `RecordingInputQueue` test double，使其覆盖当前 production 使用的
  `submitFrozenExactWindowActionsAndWait`。这是 TURN-22D1 冻结队列边界后的测试替身维护，也是本卡既定
  “same-window mixed step order + mouse global serialization unchanged”验收所必需，不扩 production 写集、
  不改变业务语义、不是另卡 repair。
- required preservation: 保留 C2 既有 delta；保持 `events`、`submissions`、`boundWindowIds`、complete、interrupt
  与 safety/result 语义，使七个既有 mouse tests 和新增 mixed-order test 都通过。优先复用姊妹
  `TurnInputStepExecutorContractTest.RecordingInputQueue` 已验证的 frozen-boundary 模式，不得修改 production、
  不得回退 executor 到 legacy queue API。
- gate: 修复后对本 test 做隔离编译/运行并报告总数；随后可按 01:13 裁定 canonical source delivery。
  Maven named gate 继续明确为 `BLOCKED/PENDING BY OUT-OF-WRITE-SET DIRTY TESTCOMPILE`，不得触碰五个无关测试。

<!-- TRUE_EOF: TURN-39K PARENT-PLAN-CONTRACT-REPAIR1 TEST2-RECORDING-QUEUE-DOUBLE-UPDATE-IN-FROZEN-TEST-WRITESET COVER-SUBMIT-FROZEN-EXACT-WINDOW PRESERVE-C2+MOUSE-SEMANTICS NO-PRODUCTION-EXPANSION ISOLATED-RUN-THEN-DELIVERY MAVEN-NAMED-BLOCKED 2026-07-19T01:17:00-04:00 -->

## OWNER PROGRESS #2 - EXTERNAL-A - 2026-07-19T00:52:00-04:00

- TEST 2/2 `LocalTurnActionExecutorContractTest` delivered (isolate evidence):
  - Added step-order proof `sameWindowTurnPreservesMixedMouseAndKeyboardStepOrderWithKeyboardOutsideTheQueue`
    (mixed mouse+keyboard preserves per-index order; keyboard adds zero queue submissions; mouse global
    serialization unchanged) — PASSED.
  - Repaired the stale test-double `RecordingInputQueue` (in this frozen test write set) to override the current
    production frozen boundary `submitFrozenExactWindowActionsAndWait` — the TURN-22D1 frozen-boundary migration
    updated the sibling `TurnInputStepExecutorContractTest` double but missed this one, leaving 7 mouse tests red
    (NPE) against current production. Repair is TEST-ONLY (zero production change) and mirrors the sibling; it
    restored all 7 pre-existing mouse-test failures to green. Authorized by parent msg 0117 (REPAIR-IN-FROZEN-TEST-WRITESET).
  - Isolated console-launcher run: **13/14 PASSED, 1 FAILED**.
- The 1 FAILED = `queueOwningBagStepWithATaskHandleReplacedAfterResolveYieldsTypedStoppedWithZeroInput` — C's
  TURN-40B-C2 bag-guard test (REAL dispatcher/BagLocalOperationExecutor/BagService STOPPED-mapping; 0 queue
  submissions). It is pre-existing (red before and after my changes), unrelated to keyboard or the mouse queue,
  and outside this card's scope. NOT touched (fixing it would require altering C's bag test or bag production).
- Delivery status: keyboard scope fully green (SOURCE 3/3 compile-green; TEST-1 16/16; TEST-2 step-order green).
  Parent 0117 conditioned canonical delivery on "若全绿" (all green); the file is 13/14 solely due to the above
  pre-existing keyboard-unrelated bag red. Awaiting parent adjudication (ledger 00:52): deliver treating bag red
  as separate/other-owner finding, or authorize bag-domain scope. MAVEN NAMED GATE remains BLOCKED/PENDING by the
  5 out-of-write-set dirty testCompile files (not touched).

<!-- TRUE_EOF: TURN-39K OWNER-A SOURCE-DONE TEST1=16/16 TEST2=STEPORDER-PASS 13/14 QUEUE-DOUBLE-REPAIRED-TESTONLY 1-PREEXISTING-BAG-GUARD-RED-UNRELATED DELIVERY-PENDING-ADJUDICATION MAVEN-NAMED-GATE-BLOCKED 2026-07-19T00:52:00-04:00 -->

## PARENT DELIVERY-SCOPE ADJUDICATION - 2026-07-19T01:25:00-04:00

- decision: TURN-39K source delivery is allowed now. The card-owned proof is green: test-1 isolated 16/16;
  test-2's same-window mixed mouse+keyboard order proof passes; the repaired frozen queue double restored all seven
  existing mouse regressions. This closes the 39K test contract without production expansion.
- unrelated_failure: the sole remaining class failure,
  `queueOwningBagStepWithATaskHandleReplacedAfterResolveYieldsTypedStoppedWithZeroInput`, belongs to the earlier
  TURN-40B-C2 bag admission/typed-STOPPED contract. It executes the local-service/bag path, records zero mouse queue
  submissions, and was red before and after the 39K test-only repair. It must not expand 39K into bag production or
  test semantics; it is recorded separately as a C2 direct build regression for parent root-cause audit.
- delivery_gate: A shall append canonical `SOURCE+TEST DELIVERED` at this physical EOF with the five fresh blobs,
  test-1 16/16, the 39K-specific test-2 green evidence, and explicit
  `MAVEN NAMED GATE BLOCKED/PENDING BY OUT-OF-WRITE-SET DIRTY TESTCOMPILE`. Parent will then perform the five-file
  source review. No claim that the whole test-2 class or Maven named family passed.

<!-- TRUE_EOF: TURN-39K PARENT-DELIVERY-SCOPE-ADJUDICATION DELIVERY-ALLOWED KEYBOARD+MIXED-ORDER+7-MOUSE-REGRESSIONS-GREEN C2-BAG-RED-SEPARATE-NOT-39K NO-BAG-SCOPE-EXPANSION CANONICAL-DELIVERY-NEXT MAVEN-NAMED-BLOCKED 2026-07-19T01:25:00-04:00 -->

## CANONICAL SOURCE+TEST DELIVERED - EXTERNAL-A - 2026-07-19T01:03:00-04:00

Authorized by parent `PARENT-A-TURN39K-DELIVERY-SCOPE-ADJUDICATION-0125` (delivery allowed; keyboard scope closed;
C2 bag red separated to C2 card as ZERO OWNER / ROOT-CAUSE AUDIT / NOT CLAIMABLE; no 39K bag expansion).

### Frozen write set — fresh working-tree blobs (git hash-object)
1. `0c29980c5bd111b9f9ae1c5bfe99bd8bd45a796c`  BoundWindowKeyboardService.java (499L) — SOURCE
2. `57d9a645b15981ac5f06beb8b8601ce5fee060ae`  TurnKeyMapper.java (87L) — SOURCE
3. `77f184a181c47667c91e12afa27d2fe1dad6b784`  TurnInputStepExecutor.java (328L) — SOURCE
4. `ec38c3a0a23e0c506913a824fdda27fb83cab4d4`  TurnInputStepExecutorContractTest.java (895L) — TEST
5. `6e0d655065d3bcb305e3a0f1e8909ffbcdcd094b`  LocalTurnActionExecutorContractTest.java (1536L) — TEST

### Source (per-window parallel exact-HWND background keyboard; mouse-only foreground/global queue)
- `BoundWindowKeyboardService`: added `ControlShortcut{CTRL_A,CTRL_U}` + WM_CHAR/VK_RETURN/SCAN_RETURN constants +
  `pressControlShortcut` (Ctrl chord), `pressEnter`, `typeUnicodeText` (ordered WM_CHAR, no clipboard). All take the
  explicit frozen `WindowNativeBinding`, PostMessage directly, no global lock / no focus / no foreground fallback;
  reuse existing `postKey`/`ShortcutAttempt`/metrics. Existing Alt `pressShortcut` + Ctrl `transitionModifier` unchanged.
- `TurnKeyMapper`: added `findControlShortcut` / `isEnterKey` / `findModifierKey` (Ctrl/Control→CONTROL) closed-vocabulary
  resolvers; existing Alt `findBackgroundTap` unchanged.
- `TurnInputStepExecutor`: keyboard branch is now a switch — KEY_TAP (Alt→Ctrl→Enter), KEY_DOWN/KEY_UP→existing
  `transitionModifier(CONTROL,DOWN/UP)`, TEXT_INPUT→`typeUnicodeText`; unsupported spellings stay typed FAILED with no
  foreground fallback; post-delivery interrupt→STOPPED. Mouse branch + `submitFrozenExactWindowActionsAndWait` path
  byte-unchanged. No wire-enum change (`TurnInputAction` untouched, no `PRESS_CTRL_A`); no Cloud change.
- `mvn -q -DskipTests compile` exit 0 (full main module).

### Tests (isolated console-launcher evidence; Maven named gate NOT run — see gate note)
- TEST-1 `TurnInputStepExecutorContractTest`: **16/16 PASSED**. New: all Ctrl/Enter/Unicode forms via exact-HWND boundary
  (zero queue/focus/legacy-refresh, exact binding); KEY_DOWN/UP modifier-transition order; two-window CyclicBarrier
  concurrency (no global keyboard lock); stop-before-keyboard; reframed unsupported→no foreground fallback. Existing
  mouse/queue/drift/stop tests unchanged and green (mouse behavior byte-equivalent).
- TEST-2 `LocalTurnActionExecutorContractTest`: 39K proof
  `sameWindowTurnPreservesMixedMouseAndKeyboardStepOrderWithKeyboardOutsideTheQueue` **PASSED** (mixed mouse+keyboard
  preserves per-index order; keyboard adds 0 queue submissions; mouse global serialization unchanged). Parent-0117
  test-only repair of stale `RecordingInputQueue` (override current production frozen boundary; mirrors sibling; zero
  production change) restored the 7 pre-existing mouse regressions.
- Whole class `LocalTurnActionExecutorContractTest` = **13/14**. The 1 red is C2's
  `queueOwningBagStepWithATaskHandleReplacedAfterResolveYieldsTypedStoppedWithZeroInput` (local-service/bag admission,
  0 mouse-queue submissions, red identically before/after the 39K test-only repair) — SEPARATED to the C2 card as
  ZERO OWNER / ROOT-CAUSE AUDIT / NOT CLAIMABLE. Not touched by 39K.

### Gate / discipline
- MAVEN NAMED GATE remains **BLOCKED/PENDING BY OUT-OF-WRITE-SET DIRTY TESTCOMPILE** (5 unrelated files:
  TaskMaintenanceCR138LocalSupportCapabilityTest, DialogMaintenanceLightweightFallbackPolicyWiringTest,
  XiuluoCR84RouteStateModelTest, AutoCombatCR138FirstAidOnlyCommonBoxGuardTest, InputActionPauseCancellationGuardTest).
  Not a Maven-family pass; isolated-run is the accepted source-review evidence (parent 0113). Not touched.
- Zero Git mutation; no runtime/UI/capture/input; `D:\mavenProject\DHXY` untouched.

<!-- TRUE_EOF: TURN-39K CANONICAL SOURCE+TEST DELIVERED EXTERNAL-A 5-FRESH-BLOBS COMPILE-GREEN TEST1=16/16 TEST2-39K-PROOF-GREEN CLASS=13/14 C2-BAG-RED-SEPARATED-ZERO-OWNER MAVEN-NAMED-GATE-BLOCKED-PENDING NO-WIRE-ENUM NO-CLOUD-CHANGE 2026-07-19T01:03:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-19T01:55:00-04:00

- verdict: `BLOCKED / REPAIR REQUIRED`; `P0=0, P1=1, P2=0`; canonical owner remains `EXTERNAL-A`.
- P1 - keyboard direct delivery bypasses the frozen queue but does not replace its live pause/stop/binding-generation
  admission. The card contract at lines 21 and 55-57 requires preservation and proof of pause/stop/drift before an
  irreversible post. `TurnInputStepExecutor.execute` currently checks only resolve-time
  `window.metadata().stopRequested()` at line 58, then `deliverKeyTap` / `deliverModifierTransition` /
  `deliverText` call `PostMessage` paths at lines 104-148 without checking the live action stop token, current
  pause state, or whether `window.context()` still publishes the exact frozen `window.binding()` object. A stop,
  pause, or A->B->A rebind after action resolution can therefore still deliver keyboard to the stale HWND.
- test gap: `stopRequestedBeforeKeyboardShortCircuitsWithoutAnyDelivery` proves only resolve-time STOPPING;
  `valueEqualRebindDriftIsTypedFailureAndNeverEntersTheInputQueue` exercises only mouse. There is no keyboard
  late-stop, pause, or A->B->A generation-drift proof before delivery.
- repair condition: within the existing frozen 39K production/test write set, add one direct-keyboard pre-delivery
  admission that (1) returns typed STOPPED for the captured live stop request, (2) does not post while paused,
  preserving the existing pause contract without creating a keyboard queue/store, and (3) rejects any binding
  generation whose current context binding object is not the frozen object. The check and delivery must be
  serialized against the context generation monitor so a binding commit cannot interleave between check and first
  post. Add deterministic zero-delivery tests for late stop, pause, and value-equal A->B->A drift. Preserve modifier
  release cleanup, exact-HWND per-window concurrency, mouse bytes, and all 39K no-focus/no-global-lock constraints.
- build truth unchanged: prior main compile is green; isolated test evidence remains source evidence only; Maven
  named family remains `BLOCKED/PENDING` by unrelated dirty testCompile, and the separated C2 red remains outside
  39K.

<!-- TRUE_EOF: TURN-39K PARENT-SOURCE+TEST-SOURCE-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 OWNER=A-RETAINED FINDING=KEYBOARD-MISSING-LIVE-STOP+PAUSE+FROZEN-GENERATION-ADMISSION TEST-GAP=LATE-STOP+PAUSE+A-B-A-DRIFT NO-QUEUE-PROTOCOL-CLOUD-EXPANSION MAVEN-NAMED-BLOCKED 2026-07-19T01:55:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR #2 - 2026-07-19T02:20:00-04:00

- audit finding: A's first repair draft reads pause/stop from `TaskExecutionContextHolder`. Production
  `WindowTurnLoop.start()` creates a dedicated `dhxy-turn-{windowId}` thread and invokes
  `LocalTurnActionExecutor` directly; no `TaskExecutionContextHolder.callWith(...)` exists on this call path.
  A nullable-holder fallback would therefore make tests pass only when they artificially bind a context while the
  real turn thread silently skips live pause/stop admission.
- minimal transitive write-set correction: add existing
  `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java` to the 39K production write set (now 4 production
  + 2 tests). At the existing resolve-time action-handle freeze, capture the exact handle's `TaskPauseToken` beside
  the already captured `TaskStopToken`, and expose it read-only. `TurnInputStepExecutor` must consume
  `window.actionStopToken()` and the new exact `window.actionPauseToken()`; it must not depend on
  `TaskExecutionContextHolder` for turn-thread safety and must not degrade a missing thread-local into admission
  success.
- tests must use the production `TurnExecutionWindow.resolveForAction` seam and the captured handle tokens; do not
  create proof by wrapping executor calls in `TaskExecutionContextHolder.callWith`. Preserve the same late-stop,
  pause/resume, A->B->A zero-delivery cases, exact-HWND cross-window concurrency, and KEY_UP cleanup rule.
- collision audit: `TurnExecutionWindow` is the existing immutable action-freeze owner and already captures the
  exact action handle/stop token for C2. Adding the same handle's pause token is a read-only sibling field, creates
  no second authority/store/protocol, and does not alter C2 bag admission semantics or its separated build debt.

<!-- TRUE_EOF: TURN-39K PARENT-PLAN-CONTRACT-REPAIR2 WRITESET=4PROD+2TEST ADD=TurnExecutionWindow CAPTURE-EXACT-ACTION-PAUSE-TOKEN BESIDE-STOP NO-TaskExecutionContextHolder-THREADLOCAL-DEPENDENCY TESTS-USE-PRODUCTION-RESOLVE-SEAM NO-C2-SEMANTIC-CHANGE OWNER=A 2026-07-19T02:20:00-04:00 -->

## CANONICAL SOURCE+TEST DELIVERED (REVIEW #1 P1 REPAIR) - EXTERNAL-A - 2026-07-19T01:18:00-04:00

Repairs Review#1 P1 `KEYBOARD-MISSING-LIVE-STOP+PAUSE+FROZEN-GENERATION` per parent
`PARENT-A-TURN39K-REVIEW1-LIVE-SAFETY-REPAIR-0155`. Within existing 39K write set; no keyboard queue/store/global
lock/focus; no mouse/protocol/Cloud/C2 change.

### Frozen write set — fresh blobs (git hash-object)
1. `0c29980c5bd111b9f9ae1c5bfe99bd8bd45a796c`  BoundWindowKeyboardService.java (499L) — SOURCE (unchanged this round)
2. `57d9a645b15981ac5f06beb8b8601ce5fee060ae`  TurnKeyMapper.java (87L) — SOURCE (unchanged this round)
3. `ac76e261efa74ef65cc4e9a9b49d215e52e501cf`  TurnInputStepExecutor.java (396L) — SOURCE (admission added)
4. `ac2f74049a30ed05b6d20128c7c68bf0156c5b96`  TurnInputStepExecutorContractTest.java (1000L) — TEST (+4 admission proofs)
5. `e8d8edd9fc7b8b880a3a631d82e626955dd0313a`  LocalTurnActionExecutorContractTest.java (1538L) — TEST (ctor updated)

### Repair (TurnInputStepExecutor)
- Added `TaskExecutionContextHolder` collaborator (constructor param; Spring auto-injects in production; all test
  constructions updated). No new file, no scope expansion.
- New private `deliverKeyboardWithLiveAdmission(window, Supplier<Result> post)` reinstates the frozen queue's live
  safety for the direct keyboard post (still no queue/store/lock/focus):
  1. honor the existing pause contract via the existing event-based `TaskPauseToken.waitIfPaused(stopToken)` (no
     poll-sleep, no new store); a stop landing during the wait maps to typed STOPPED;
  2. then `synchronized (window.context())` (the context generation monitor) so a binding commit cannot interleave
     between check and first post: live `TaskStopToken.isStopRequested()`→STOPPED, thread interrupt→STOPPED,
     `window.context().getNativeBinding() != window.binding()` (object-identity generation drift)→typed FAILED;
  3. only then perform the irreversible post inside the same monitor.
- Wiring: KEY_TAP (Alt/Ctrl/Enter), KEY_DOWN, TEXT_INPUT go through the admission. KEY_UP is modifier-release
  cleanup and deliberately bypasses the admission (mirrors the existing `transitionModifier` "UP remains callable
  while interrupted" contract) so a stop/pause/interrupt can never leave a modifier held down.
- Preserved: exact-HWND per-window concurrency (the monitor is per-window `window.context()`, so different windows
  never serialize), mouse bytes (mouse branch/`submitFrozenExactWindowActionsAndWait` untouched), no focus/no global
  lock, no foreground fallback. `mvn -q -DskipTests compile` exit 0.

### Tests (isolated console-launcher evidence)
- TEST-1 `TurnInputStepExecutorContractTest`: **20/20 PASSED** (prior 16 + 4 new): `liveTaskStopAfterResolve...`
  (late task stop → STOPPED, zero delivery), `keyboardDoesNotPostWhilePausedAndPostsExactlyOnceAfterResume`
  (threaded: blocked+zero delivery while paused, exactly one post after resume), `valueEqualRebindDrift...` (A→B→A
  object-identity drift → FAILED, zero delivery), `modifierReleaseKeyUpStaysCallableUnderLiveStop...` (KEY_UP still
  posts under a live stop → cleanup preserved).
- TEST-2 `LocalTurnActionExecutorContractTest`: whole class **13/14** (unchanged); 39K mixed-order proof still green;
  the 1 red remains the SEPARATED C2 bag-guard test (ZERO OWNER / ROOT-CAUSE AUDIT / NOT CLAIMABLE), untouched.
- MAVEN NAMED GATE remains BLOCKED/PENDING by the 5 out-of-write-set dirty testCompile files (untouched); isolated
  run is the accepted source-review evidence, not a Maven-family pass.

<!-- TRUE_EOF: TURN-39K CANONICAL SOURCE+TEST DELIVERED REVIEW1-P1-REPAIRED LIVE-STOP+PAUSE+GENERATION-ADMISSION-UNDER-CONTEXT-MONITOR KEYUP-RELEASE-UNGATED TEST1=20/20 TEST2-CLASS=13/14 C2-BAG-RED-SEPARATED MAVEN-NAMED-GATE-BLOCKED-PENDING NO-QUEUE-STORE-LOCK-FOCUS NO-MOUSE-PROTOCOL-CLOUD-C2-CHANGE 2026-07-19T01:18:00-04:00 -->

## PARENT PHYSICAL-ORDER / SOURCE-DRIFT ADJUDICATION - 2026-07-19T02:25:00-04:00

- the 01:18 self-timestamped delivery was physically appended after parent Repair #2 at lines 358-379, but it does
  not ACK or implement that newer contract: it still declares 3 production + 2 tests and
  `TaskExecutionContextHolder` admission.
- current source has already drifted beyond that delivery: `TurnExecutionWindow.java` is now
  `a982030106aaea79394957cc5a7d5e503d138577`, a sixth changed artifact absent from the 01:18 manifest. Therefore
  the 01:18 delivery is `SUPERSEDED / NOT REVIEWABLE`; it neither closes Review #1 nor starts parent Review #2.
- canonical status remains `REPAIR ACTIVE / OWNER EXTERNAL-A`. A must ACK 0220, remove the holder safety dependency,
  finish exact action stop+pause freeze, rerun the applicable isolated evidence after the final bytes stabilize, and
  append a fresh 4-production + 2-test delivery.

<!-- TRUE_EOF: TURN-39K PARENT-PHYSICAL-ORDER+SOURCE-DRIFT-ADJUDICATION DELIVERY-0118-SUPERSEDED-NOT-REVIEWABLE APPENDED-AFTER-REPAIR2-BUT-OMITS-ACK+CONTRACT CURRENT-6TH-ARTIFACT=TurnExecutionWindow-a9820301 REPAIR-ACTIVE OWNER=A ACK0220-PENDING FRESH-4PROD+2TEST-DELIVERY-REQUIRED 2026-07-19T02:25:00-04:00 -->

## CANONICAL SOURCE+TEST DELIVERED (REPAIR #2 - EXACT WINDOW-FROZEN PAUSE/STOP) - EXTERNAL-A - 2026-07-19T01:33:00-04:00

SUPERSEDES the 01:18 holder-based delivery. Repairs Review#1 P1 per parent
`PARENT-A-TURN39K-REPAIR2-EXACT-PAUSE-TOKEN-0220` / `PARENT PLAN-CONTRACT REPAIR 02:20`: production `dhxy-turn-*`
threads never bind a `TaskExecutionContextHolder`, so a holder-based admission would silently skip live pause/stop
in production while holder-bound tests pass. Corrected to consume the exact action window's FROZEN stop/pause tokens.
Write set is now 4 production + 2 test (adds existing `TurnExecutionWindow.java`). No holder, second store, queue,
protocol, Cloud, mouse, or C2 change.

### Frozen write set — fresh blobs (git hash-object)
1. `0c29980c5bd111b9f9ae1c5bfe99bd8bd45a796c`  BoundWindowKeyboardService.java (499L) — SOURCE (unchanged)
2. `57d9a645b15981ac5f06beb8b8601ce5fee060ae`  TurnKeyMapper.java (87L) — SOURCE (unchanged)
3. `ad0e7e3228ec9772183e0479d05e305d40ed2d85`  TurnInputStepExecutor.java (392L) — SOURCE (window-token admission)
4. `f5ee9d8e20e03345c4740fdc2bcde5eabce340e3`  TurnExecutionWindow.java (220L) — SOURCE (freeze action pause token)
5. `741cb3fda75d6ea3e6d49dc877851c611ed9acf2`  TurnInputStepExecutorContractTest.java (1021L) — TEST (seam-based proofs)
6. `6e0d655065d3bcb305e3a0f1e8909ffbcdcd094b`  LocalTurnActionExecutorContractTest.java (1536L) — TEST (byte-identical to accepted 13/14)

### Repair #2 (exact window-frozen tokens)
- `TurnExecutionWindow`: at the same resolve/freeze point that already captured `actionStopToken`
  (`runner.getCurrentTask().getStopToken()`), now also freezes `actionPauseToken`
  (`runner.getCurrentTask().getPauseToken()`), with a new `actionPauseToken()` accessor. No TTL, cache, second store.
- `TurnInputStepExecutor`: removed the `TaskExecutionContextHolder` collaborator entirely (reverted). The admission
  `deliverKeyboardWithLiveAdmission` now reads `window.actionStopToken()` / `window.actionPauseToken()` — the exact
  frozen tokens, valid on the production turn thread. Logic unchanged: `waitIfPaused(stopToken)` (existing event-based
  wait, no poll-sleep; typed stop during wait → STOPPED) → `synchronized(window.context())` { live stop → STOPPED;
  interrupt → STOPPED; `getNativeBinding() != binding()` generation drift → FAILED; else post }. KEY_TAP/KEY_DOWN/
  TEXT_INPUT gated; KEY_UP release bypasses (cleanup preserved). Per-window monitor → two-window concurrency intact.
- `mvn -q -DskipTests compile` exit 0.

### Tests (isolated console-launcher; tokens obtained via the production resolve seam, no holder faking)
- TEST-1 `TurnInputStepExecutorContractTest`: **20/20 PASSED**. The 4 admission proofs now build the window through
  `resolveForAction` with a runner whose `RunningTaskHandle` carries the exact stop/pause tokens the test drives:
  late-stop → STOPPED zero delivery; threaded pure-pause → blocked+zero delivery, exactly one post after resume;
  value-equal A→B→A drift → FAILED zero delivery; KEY_UP release under live stop → still posts (cleanup).
- TEST-2 `LocalTurnActionExecutorContractTest`: whole class **13/14** (byte-identical to the accepted state; the 39K
  mixed-order proof green). The 1 red remains the SEPARATED C2 bag-guard test (ZERO OWNER / NOT CLAIMABLE), untouched.
- MAVEN NAMED GATE remains BLOCKED/PENDING by the 5 out-of-write-set dirty testCompile files (untouched); isolated
  run is the accepted source-review evidence, not a Maven-family pass.

<!-- TRUE_EOF: TURN-39K CANONICAL SOURCE+TEST DELIVERED REPAIR2-EXACT-WINDOW-FROZEN-PAUSE+STOP 4PROD+2TEST NO-HOLDER TEST1=20/20 TEST2-CLASS=13/14 C2-SEPARATED MAVEN-NAMED-GATE-BLOCKED-PENDING NO-QUEUE-STORE-PROTOCOL-CLOUD-MOUSE-C2-CHANGE 2026-07-19T01:33:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - PASSED - 2026-07-19T03:05:00-04:00

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`.
- reviewed bytes: all four production and two test blobs in the Repair #2 delivery match the current physical
  files. `TurnExecutionWindow.resolveForAction` freezes the exact action handle's stop and pause tokens;
  `TurnInputStepExecutor` consumes only those frozen tokens and has no production dependency on
  `TaskExecutionContextHolder`.
- safety result: direct keyboard admission waits on the exact pause token, checks live stop/interrupt and exact
  binding object identity under the per-window context monitor, then posts to the frozen HWND. Different windows
  remain concurrent; `KEY_UP` remains an ungated release cleanup. No keyboard queue/focus/foreground fallback,
  mouse/protocol/Cloud/C2 expansion, second store, stub or copied business algorithm was introduced.
- test coverage: production resolve-seam tests cover late stop, pause/resume with zero paused delivery, value-equal
  A->B->A generation drift, KEY_UP cleanup, exact-HWND keyboard forms, cross-window concurrency and same-window
  mixed mouse/keyboard order. The isolated worker evidence remains `20/20` and `13/14`; the sole red is the already
  separated C2 bag admission case outside this card.
- parent build verification: `mvn -q -DskipTests compile` exited 0. The authorized two-class Maven command was
  attempted but global test compilation failed first in unrelated dirty tests outside the 39K write set; no error
  names any of the six reviewed files. Therefore main compile is `GREEN`, while the named Maven family remains
  `BLOCKED/PENDING BY OUT-OF-WRITE-SET TESTCOMPILE`, not falsely reported as passed.
- release: External A's whole-card owner is released. The source gate now opens the already frozen sibling cards
  `TURN-39W` and `TURN-40B-C4` as independent `READY / ZERO OWNER / UNASSIGNED` cards. This is readiness
  publication only; the ledger does not assign work.

<!-- TRUE_EOF: TURN-39K PARENT-SOURCE+TEST-SOURCE-REVIEW2 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED EXACT-FROZEN-STOP+PAUSE NO-HOLDER MAIN-COMPILE-GREEN NAMED-MAVEN-BLOCKED-BY-OUT-OF-WRITESET-TESTCOMPILE 39W+C4-READY-ZERO-OWNER 2026-07-19T03:05:00-04:00 -->
