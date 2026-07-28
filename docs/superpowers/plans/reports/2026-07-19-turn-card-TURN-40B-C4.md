# TURN-40B-C4 - Cloud Navigation Input Caller Closure

## Canonical Status

- status: `READY / ZERO OWNER / UNASSIGNED`
- type: `INTEGRATION / SOURCE+TEST`
- dependsOn: `TURN-40BP1 Report Review #7 PASSED`, `TURN-39P1 Review #15 PASSED`, and
  `TURN-39K Source Review #2 PASSED`.
- claim rule: the first eligible Worker that can finish this whole card may append a canonical claim at this
  physical EOF before editing. The ledger does not assign this card.

## Approved Runtime Invariant

- Migrate the eight frozen active Navigation input caller rows to the existing turn action path. Keyboard uses the
  39K exact-HWND background path and may run concurrently across windows; only mouse remains foreground/global-
  serial. Same-window action order and waits remain sequential.
- Preserve baseline observation, route, cancel/stop, prepared-route, fallback and COMPLETED-only truth semantics.
  No cross-process callback, second queue/store/protocol, foreground keyboard fallback, stub, constant result or
  copied business algorithm is allowed. `无已批准业务差异；按基线等价迁移`.

## Frozen Cloud Production Write Set

1. `src/main/java/com/bot/dhxy/service/NavigationService.java`

This card owns the seven frozen former exclusive input blocks plus the existing `moveAndClickLeft` compatibility
caller, including their already frozen typed observation/stop bridges and exact per-action order. Remove the
retired `InputSequences` ownership only after all eight rows are closed. Pre-claim SHA-256 is `037C5F45...`.

## Frozen Cloud Test Write Set

1. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`
2. `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingTaskTrackerTurnContractTest.java`

Required proofs: all eight Navigation rows preserve action order, exact waits/timing, key vocabulary, prepared-
route behavior, four terminal outcomes and cancel-to-stop behavior; the FiveRing tracker contract remains with C4
and must not be reassigned. Existing tests may be modified but not replaced by source-only or mock-only proof.

## Build Gate

- Run the two named tests and applicable Cloud compile after Java writers are stable.
- Do not run runtime/application/server/Task/UI/capture/input.

<!-- TRUE_EOF: TURN-40B-C4 READY ZERO-OWNER UNASSIGNED WHOLE-CARD CLOUD NAVIGATION-8-CALLERS+2-TEST EXACT-HWND-BACKGROUND-KEYBOARD ONLY-MOUSE-FOREGROUND-GLOBAL-SERIAL NO-BUSINESS-DIFFERENCE 2026-07-19T03:05:00-04:00 -->

## WHOLE-CARD CLAIM - EXTERNAL-C - 2026-07-19T03:10:00-04:00

- owner: `EXTERNAL-C`（heartbeat `778801ea`）。canonical whole-card claim，append 于本卡 physical EOF（claim rule §9-10）。
- card: `TURN-40B-C4` — Cloud Navigation Input Caller Closure。
- basis: card status `READY / ZERO OWNER / UNASSIGNED`；父级 03:07 PHYSICAL-ORDER-RECONCILIATION 确认 39W+C4 READY/ZERO-OWNER 无既有 canonical claim。dependsOn 全满足：TURN-40BP1 Report Review #7 PASSED、TURN-39P1 Review #15 PASSED、TURN-39K Source Review #2 PASSED。C 有 NavigationService 深度 context（TURN-27 whole-card turn 迁移 PASSED）。
- precheck: append 前独立读本卡全文，physical EOF = `TURN-40B-C4 READY ZERO-OWNER UNASSIGNED ...`（line 43），无既有 owner/claim 块；pre-claim SHA-256 `037C5F45`。
- write_set 接受（frozen）: 生产 `src/main/java/com/bot/dhxy/service/NavigationService.java`（迁移 7 个 frozen former exclusive input 块 + 既有 `moveAndClickLeft` 兼容 caller = 8 rows 到既有 turn action 路径；keyboard 用 39K exact-HWND 后台路径可跨窗并行，mouse 保 foreground/global-serial；同窗 action 顺序/wait 保持 sequential；八 rows 全闭合后才移除退役 `InputSequences` ownership）；test `cloudbrain/service/NavigationTurnContractTest.java` + `cloudbrain/task/wuhuan/FiveRingTaskTrackerTurnContractTest.java`（八 Navigation rows 保 action 序/exact waits/key vocab/prepared-route/四终态/cancel-to-stop；FiveRing tracker 合同留 C4 不重指派；既有测试可改不可换成 source-only/mock-only）。
- 合同要点接受: 保 baseline observation/route/cancel-stop/prepared-route/fallback/COMPLETED-only truth；无 cross-process callback、无第二 queue/store/protocol、无 foreground keyboard fallback、无 stub/常量结果/copied algorithm。`无已批准业务差异；按基线等价迁移`。
- discipline: 零 Git mutation；其它 Java writer 活动时不运行 Maven（javac 单文件 parse 除外）；DHXY 只读（注：本卡生产在 DHXY-cr271 worktree，非用户 IntelliJ 的 D:\mavenProject\DHXY）；无 runtime/UI/capture/input；不自批、不建 reviewer。
- next: append 后回读本卡 EOF；我的 claim 在物理 EOF 且无更早并发 claim → 确立 owner，逐文件 physical 取证 recon 后实施；发现更早并发 claim（如 EXTERNAL-A）→ 立即 canonical 自撤让位。

<!-- TRUE_EOF: TURN-40B-C4 EXTERNAL-C WHOLE-CARD-CLAIM OWNER-C PRECHECK-READY-ZERO-OWNER NAV-8-CALLERS+2-TEST PRECLAIM-SHA=037C5F45 ANTI-RACE-EOF-REREAD-PENDING HEARTBEAT-778801ea 2026-07-19T03:10:00-04:00 -->

## PARENT PLAN-CONTRACT CLARIFICATION #1 - 2026-07-19T03:47:00-04:00

- source census correction: the frozen eight rows are exactly
  `moveAndClickLeft@1070` plus seven `submitExclusiveAndWait` callers at
  `1450, 1674, 1968, 2081, 2218, 2231, 2334` in the pre-claim Cloud source. C's 03:42 ledger census described
  `7/8` while treating 1070 as the remaining row, but its earlier list contained only six exclusive callers and
  omitted `closeMiniMapIfOpen@2334`.
- `closeMiniMapIfOpen@2334` is an independent observe -> Alt+1 -> wait 300 -> re-observe -> one retry caller. It
  must be migrated and tested separately from the same-family `cleanupYellowDestinationRouteQueued@1968` path.
- transitive same-file closure: shared `pressAlt1ForMiniMap` must no longer expose a focused `InputProvider.pressAlt1`
  fallback to any migrated caller. Use only the 39K-supported exact-HWND background key vocabulary (`ALT_1` or
  `Alt+1`; `ALT_2`/`CTRL_A`/`Enter` are likewise accepted) and preserve each caller's observation/retry order.
- acceptance: production census is zero remaining `submitExclusiveAndWait`/`moveAndClickLeft` compatibility caller
  in this eight-row cohort and zero focused-keyboard fallback reachable through `pressAlt1ForMiniMap`; tests include
  explicit independent proofs for both 1968 and 2334. No new file, owner, queue, protocol or business behavior.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-PLAN-CONTRACT-CLARIFICATION1 EXACT-8=1070+1450+1674+1968+2081+2218+2231+2334 OMITTED=closeMiniMapIfOpen-2334 SHARED-pressAlt1ForMiniMap-NO-FOCUSED-FALLBACK TEST-1968+2334-INDEPENDENT OWNER-C NO-WRITESET-EXPANSION 2026-07-19T03:47:00-04:00 -->

## PARENT ACK RECONCILIATION - 2026-07-19T03:50:00-04:00

- External C's next ledger STATUS EVENT explicitly ACKed
  `PARENT-C-TURN40B-C4-EXACT8-INCLUDE-2334-0347` and accepted every clarification condition.
- canonical state: `OWNER EXTERNAL-C / SOURCE ACTIVE`; communication normal; exact-eight acceptance is no longer
  pending. No owner, write-set, business, protocol or queue change.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-ACK-RECONCILIATION OWNER-C SOURCE-ACTIVE EXACT8-ACKED 1968+2334-INDEPENDENT NO-FOCUSED-KEYBOARD-FALLBACK COMMUNICATION-NORMAL 2026-07-19T03:50:00-04:00 -->

## PARENT PLAN-CONTRACT ADJUDICATION #2 - 2026-07-19T04:16:00-04:00

- accepted evidence: repository-wide symbol search proves legacy private `closeMiniMapIfOpen@2334` has zero active
  callers. The active baseline successor is `navigateInCurrentMap:finish -> closeMiniMapIfOpenTurn`.
- disposition: delete the dead legacy method and thereby close its retired `submitExclusiveAndWait` row. Do not
  migrate or retain a duplicate dead implementation.
- transferred acceptance: current `closeMiniMapIfOpenTurn` is not yet mechanically equivalent to 696 because it
  performs only one visibility check/toggle. In the existing active method, preserve the baseline sequence
  `observe -> Alt+1 -> WAIT 300 -> re-observe -> retry Alt+1 once -> WAIT 300`, with typed turn terminal handling,
  exact-HWND background keyboard and zero focused fallback.
- test: `NavigationTurnContractTest` must prove this active finish-cleanup retry path independently from row 1968;
  no test of the deleted private dead method is required. The other seven active legacy rows remain in scope.
- boundary: same one-production + two-test write set, owner C, protocol/queue/business baseline and FiveRing test
  ownership. This closes a transitive baseline gap without adding a new algorithm or behavior.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-PLAN-CONTRACT-ADJUDICATION2 DELETE-DEAD-2334 TRANSFER-696-SEQUENCE-TO-ACTIVE-closeMiniMapIfOpenTurn OBSERVE-ALT1-WAIT300-REOBSERVE-RETRYONCE TEST-ACTIVE-FINISH-SEPARATE-FROM-1968 NO-FOCUSED-FALLBACK OWNER-C ACK-PENDING 2026-07-19T04:16:00-04:00 -->

## PARENT COMMUNICATION STATUS - 2026-07-19T04:32:00-04:00

- C's 04:22 and 04:30 STATUS EVENTs both followed the directed 04:16 message but reported no new parent message
  and did not ACK it. Canonical communication state is therefore `COMMUNICATION_STALE`.
- NavigationService has fresh text/scroll builder WIP, so C remains `OWNER / SOURCE ACTIVE` and is not
  `ACTIVE_STALE`. The dead-row transfer adjudication remains the current contract; no return or reassignment.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-COMMUNICATION-STATUS OWNER-C SOURCE-ACTIVE COMMUNICATION_STALE NOT-ACTIVE-STALE MESSAGE-0416-UNACKED-R2 WIP-BUILDERS-PRESENT 2026-07-19T04:32:00-04:00 -->

## PARENT SOURCE-ACTIVE SNAPSHOT - 2026-07-19T04:48:00-04:00

- C's 04:38 and 04:46 events still did not ACK the 0416/0432 messages, so COMMUNICATION_STALE remains. Fresh
  `executeInputTurn`, text/scroll builder bytes and call-graph work prove continued activity; not ACTIVE_STALE.
- The discovered atomicity requirement is accepted as implementation safety within the frozen same-file closure:
  migrate all eight-row dispositions and shared input helpers in one coherent pass, leaving no turn call nested in
  a legacy exclusive callback. The dead-row transfer adjudication and write set remain unchanged.

## PARENT SOURCE-ACTIVE SNAPSHOT #2 - 2026-07-19T05:22:00-04:00

- C's latest canonical STATUS EVENT reports `5/8` rows migrated: `1070, 2218, 2231, 1674, 2081`; remaining legacy
  exclusive rows are `1450, 1968, 2334`. Fresh NavigationService bytes and parse progress keep C SOURCE ACTIVE and
  not ACTIVE_STALE.
- C still ACKs neither `PARENT-C-TURN40B-C4-DEAD2334-TRANSFER-ACTIVE-0416` nor
  `PARENT-C-TURN40B-C4-COMMUNICATION-STALE-0432`, so COMMUNICATION_STALE remains. The 04:16 dead-row transfer to
  active `closeMiniMapIfOpenTurn` is already authoritative, not an unanswered decision.
- No WIP source review and no Maven while both Cloud Java writers are active.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-SNAPSHOT2 OWNER-C SOURCE-ACTIVE ROWS-5-OF-8 COMMUNICATION-STALE NOT-ACTIVE-STALE 0416+0432-UNACKED CONTRACT-ALREADY-ADJUDICATED NO-MAVEN 2026-07-19T05:22:00-04:00 -->

<!-- TRUE_EOF: TURN-40B-C4 PARENT-SOURCE-ACTIVE-SNAPSHOT OWNER-C COMMUNICATION_STALE NOT-ACTIVE-STALE ATOMIC-8ROW+SHARED-HELPER-PASS NO-TURN-IN-EXCLUSIVE WIP-NOT-REVIEWED 2026-07-19T04:48:00-04:00 -->

<!-- TRUE_EOF: TURN-40B-C4 PARENT-SNAPSHOT2 OWNER-C SOURCE-ACTIVE ROWS-5-OF-8 COMMUNICATION-STALE NOT-ACTIVE-STALE CONTRACT-ALREADY-ADJUDICATED NO-MAVEN PHYSICAL-EOF 2026-07-19T05:22:00-04:00 -->

## PARENT SOURCE-ACTIVE SNAPSHOT #3 - 2026-07-19T05:28:00-04:00

- latest canonical event is `6/8`: 1968 is migrated, shared helpers are unpacked, and the file is coherent with no
  turn execution nested inside a legacy exclusive callback. Remaining rows are `1450` and dead-row disposition
  `2334` plus its transferred active finish-cleanup proof.
- `pressAlt1ForMiniMap` no longer exposes focused `inputProvider.pressAlt1()` fallback. This closes the exact-HWND
  background-keyboard invariant without changing observation/retry order.
- C remains SOURCE ACTIVE and not ACTIVE_STALE. The 0416/0432 messages remain unacknowledged, so
  COMMUNICATION_STALE remains. No Maven and no WIP review.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-SNAPSHOT3 OWNER-C SOURCE-ACTIVE ROWS-6-OF-8 REMAIN=1450+2334 COHERENT NO-FOCUSED-ALT1-FALLBACK COMMUNICATION-STALE NOT-ACTIVE-STALE NO-MAVEN PHYSICAL-EOF 2026-07-19T05:28:00-04:00 -->

## PARENT COMMUNICATION RECOVERY - 2026-07-19T05:34:00-04:00

- C's 05:32 STATUS EVENT explicitly ACKed both
  `PARENT-C-TURN40B-C4-DEAD2334-TRANSFER-ACTIVE-0416` and
  `PARENT-C-TURN40B-C4-COMMUNICATION-STALE-0432`; COMMUNICATION_STALE is cleared.
- C accepted deletion of dead legacy 2334, transfer of the full 696 observe/toggle/re-observe/retry-once sequence
  to active `closeMiniMapIfOpenTurn`, zero focused fallback, typed terminal handling, and a proof separate from 1968.
- Canonical state remains OWNER EXTERNAL-C / SOURCE ACTIVE 6/8 / NO BLOCKER / NO DELIVERY. No Maven while A is an
  active Cloud Java writer.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-COMMUNICATION-RECOVERY DOUBLE-ACK=0416+0432 OWNER-C SOURCE-ACTIVE ROWS-6-OF-8 NO-BLOCKER NO-DELIVERY DEAD2334+ACTIVE696-TRANSFER-ACCEPTED PHYSICAL-EOF 2026-07-19T05:34:00-04:00 -->

## PARENT SOURCE-ACTIVE SNAPSHOT #4 - 2026-07-18T15:48:00-04:00

- C's latest canonical STATUS EVENT reports all seven active legacy rows migrated:
  `1070/1450/1674/1968/2081/2218/2231`. Production `inputProvider.*` is zero and the only remaining legacy
  `submitExclusiveAndWait` is the already adjudicated dead method at 2334.
- Remaining card closure is unchanged: delete dead 2334, transfer the full 696 observe/Alt+1/WAIT300/re-observe/
  retry-once sequence into active `closeMiniMapIfOpenTurn`, remove the retired InputSequences ownership, and write
  independent active-finish and row-1968 tests. No blocker, no delivery, communication normal.
- C remains sole owner / SOURCE ACTIVE and not ACTIVE_STALE. External A remains an active repair owner, so no Maven.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-SNAPSHOT4 OWNER-C SOURCE-ACTIVE ALL-7-ACTIVE-ROWS-MIGRATED ONLY-DEAD-2334-REMAINS ACTIVE-696-TRANSFER+2TESTS-PENDING COMMUNICATION-NORMAL NO-BLOCKER NO-DELIVERY NO-MAVEN PHYSICAL-EOF 2026-07-18T15:48:00-04:00 -->

## PARENT SOURCE-ACTIVE SNAPSHOT #5 - PRODUCTION COMPLETE - 2026-07-18T15:55:00-04:00

- C's canonical 05:52 event and physical source confirm all eight dispositions are closed: seven active rows are
  migrated and dead 2334 is deleted. `NavigationService.java` is 3,162 lines / SHA-256
  `8bd13811e8f97a2434babeec1e6819d9bed885a0b9d702d18cf4b790fa1a2eeb`.
- The active `closeMiniMapIfOpenTurn` now owns the 696 sequence
  observe -> exact-HWND Alt+1 -> WAIT300 -> re-observe -> retry once -> WAIT300; non-COMPLETED stops without retry.
  Production `inputProvider.*`, `submitExclusiveAndWait` calls and `InputSequences` ownership are zero.
- Canonical state remains OWNER C / SOURCE ACTIVE / PRODUCTION COMPLETE / TEST SOURCE ACTIVE / NO DELIVERY. The
  frozen two-test closure remains; no WIP review and no Maven while A is also a Cloud Java writer.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-SNAPSHOT5 OWNER-C PRODUCTION-COMPLETE ALL8-DISPOSITIONS-CLOSED NAVSVC=8BD13811/3162 ZERO-LEGACY-INPUT ACTIVE-696-RETRY TESTS-ACTIVE NO-DELIVERY NO-MAVEN PHYSICAL-EOF 2026-07-18T15:55:00-04:00 -->

## PARENT TEST-CONTRACT ADJUDICATION #3 - 2026-07-18T20:04:43Z

- C 的 harness 阻断属实，但不构成业务语义阻断。拒绝 `(B)` production test seam：不扩大 production
  写集，不增加可注入谓词、override 或第二可见性真相。拒绝 `(C)` scope 收敛：冻结八行、1968 与 active
  finish-cleanup 独立证明保持不变，不能退化为纯源码审查。
- 批准精化后的 test-only `(A)`：从 `PackagedTemplateAssets` 加载真实 mini-map checkbox 模板，把模板绘入
  非纯色 `BufferedImage` 后编码为 scripted capture reply；匹配继续走 production
  `OpenCvNativeLoader/ImageFinder`。不得新增自有 native loader、生产 seam、常量结果或复制匹配算法。
- public whole-flow 因当前 parent-scoped `CoordinateHelper/tracker` compile debt 无法稳定触达的 frozen
  world-map caller，可在同一 frozen test 内以 test-only reflection 调用拥有迁移动作的真实 production
  method，并经真实 `TurnGameClient` + scripted `CloudTurnCommandPort` 观察动作。反射只解决入口可达性，
  不得复制 caller 算法、只读源码字符串或以 mock-only 结论替代真实 turn action observation。
- 最低验收：逐一映射七个 active legacy caller 与 active finish-cleanup；锁定 key/text/click/move/scroll
  vocabulary、步骤顺序、696 waits/re-observe/retry-once、prepared-route side effects、四终态与 cancel-to-stop。
  1968 和 finish-cleanup 必须为两个独立测试；`FiveRingTaskTrackerTurnContractTest` 不得删减。
- 状态保持 `OWNER EXTERNAL-C / SOURCE ACTIVE / TEST REPAIR REQUIRED / NO DELIVERY`；无 Maven，待具名 ACK。

<!-- TRUE_EOF: TURN-40B-C4 PARENT-TEST-CONTRACT-ADJUDICATION3 REJECT-B-PRODUCTION-SEAM REJECT-C-SCOPE APPROVE-TESTONLY-PATTERNED-PACKAGED-TEMPLATE+PRODUCTION-OPENCV ALLOW-REFLECTION-REAL-METHOD+REAL-TURN-OBSERVATION ALL8+1968-SEPARATE-FINISH REQUIRED OWNER-C ACK-PENDING NO-MAVEN 2026-07-18T20:04:43Z -->

## PARENT ACK RECONCILIATION #4 - 2026-07-18T20:16:43Z

- C 的 06:36 canonical STATUS EVENT 已具名 ACK
  `PARENT-C-TURN40B-C4-TEST-CONTRACT-2004`，完整接受拒绝 production seam/scope 收敛、test-only patterned
  packaged-template fixture、production OpenCV、必要时反射真实 production method 并观察真实 turn command、
  all-eight named tests、1968/finish-cleanup 独立证明及 FiveRing test 保留。
- `AWAIT PARENT TEST-SCOPE` 阻断解除，communication normal。状态为
  `OWNER EXTERNAL-C / SOURCE ACTIVE / TEST IMPLEMENTATION ACTIVE / NO DELIVERY`；无 Maven，39C1 仍 NOT READY。

<!-- TRUE_EOF: TURN-40B-C4 PARENT-ACK-RECONCILIATION4 ACK=PARENT-C-TURN40B-C4-TEST-CONTRACT-2004 COMMUNICATION-NORMAL TEST-IMPLEMENTATION-ACTIVE OWNER-C NO-DELIVERY NO-MAVEN 39C1-NOT-READY 2026-07-18T20:16:43Z -->

## PARENT PLAN-CONTRACT REPAIR #5 - REMAINING FIVE CALLERS - 2026-07-18T20:31:44Z

- C 的 07:06 filing 证明当前 4/8 进度真实，但剩余五个 caller 不构成不可闭合 blocker。父级从 696
  源码完成传递依赖审计：`CoordinateHelper`、`GameClientTracker`、`GameTextLineOcrService`、
  `WorldMapRouteResultMemoryService` 均为非 final，相关 find/capture/OCR/memory 方法可由 test-only subclass
  覆盖；`WindowRuntimeContext(String, GameContext)` 可真实构造并发布 prepared action。
- 裁决：禁止递归 Unsafe 注入 `CoordinateHelper` 内部 matcher/object graph，也不批准 source-level 降级。
  在 frozen `NavigationTurnContractTest` 内使用普通 test-only subclass，构造器依赖可传 null，但只覆盖到达
  frozen caller 所需的 public observation 方法，返回明确受控的坐标、capture、OCR、memory 或 visible 状态。
  随后 reflection 调用真实 production caller，动作仍由 real `TurnGameClient` + scripted port 观察。
- 五 caller closure：
  `clickRememberedWorldMapRouteResult` 用 test memory service 返回 clean entry；
  `clickDestinationFromWorldMapSearchResults` 用 scripted coordinate/tracker/OCR 返回 scaled rect、capture success、
  matched destination 与 coordinate center；`cleanupYellowDestinationRouteQueued` 用 scripted visibility/title；
  `prepareWorldMapSearchResults` 用 scripted title/xunlu observations；prepared-route caller 用真实
  `WindowRuntimeContext` + `PreparedDialogAction` 状态。各自锁 exact action/order/wait/terminal/cancel truth。
- 已写的 Unsafe helper 不得扩展为递归 object-graph 注入；优先用上述普通 subclass 替换简单 tracker/cleaner
  doubles，最终交付若仍保留 Unsafe 必须逐处说明为何普通构造/subclass 不可行。write set、业务、protocol、
  production source 均不变。状态 `OWNER C / TEST IMPLEMENTATION ACTIVE / PLAN CONTRACT REPAIRED / NO DELIVERY`。

<!-- TRUE_EOF: TURN-40B-C4 PARENT-PLAN-CONTRACT-REPAIR5 REMAINING5=CLOSABLE TESTONLY-SUBCLASS-COORDINATE+TRACKER+OCR+MEMORY REAL-WINDOWRUNTIME+PREPARED REFLECTION-REAL-CALLER+REAL-TURN REJECT-RECURSIVE-UNSAFE REJECT-SOURCE-DOWNGRADE OWNER-C ACK-PENDING NO-MAVEN 2026-07-18T20:31:44Z -->

## PARENT COMMUNICATION STATUS #6 - 2026-07-18T20:41:45Z

- C 的 07:16 与 07:26 两个 canonical STATUS EVENT 均位于 2031 message 之后，却都写 `无新定向
  EXTERNAL-C` 且未 ACK `PARENT-C-TURN40B-C4-REMAINING5-SUBCLASS-2031`，现标 `COMMUNICATION_STALE`。
- C 有 07:26 fresh event，且 NavigationTurnContractTest 最近仍有新字节，因此不是 ACTIVE_STALE；owner、
  production-complete 和 test write set 保持。Repair #5 已明确：ordinary subclass 应正常调用
  `CoordinateHelper(null,null)`、Lombok-generated tracker constructor 的 null collaborators、OCR null constructor
  或真实 runtime constructor；不得再用 `Unsafe.allocate(subclass)` 跳构造来替代该裁决。
- 下一 STATUS EVENT 必须具名双 ACK 2031/2041 并按 Repair #5 推进；不得以未读消息为由选择 source-level 收敛。

## PARENT TEST REVIEW #7 - REPAIR REQUIRED - 2026-07-18T20:51:45Z

- Canonical C events through 07:56 still report `ack_parent_message: NONE` after 2031/2041, so
  `COMMUNICATION_STALE` remains. Fresh test bytes mean `NOT ACTIVE_STALE`; owner C is retained.
- **P1 plan-contract violation:** `NavigationTurnContractTest` imports `sun.misc.Unsafe` and calls
  `allocate(StubCoordinateHelper.class)`, `allocate(StubTracker.class)`, `allocate(StubTempPath.class)`, and
  `allocate(StubProperties.class)`. Those same stubs define callable constructors using `super(null, ...)` (and
  `BotProperties` has a normal no-arg constructor), so `Unsafe.allocateInstance(subclass)` deliberately bypasses
  Repair #5 instead of using ordinary test-only subclass construction.
- **P1 missing frozen success proof:** `clickDestinationFromWorldMapSearchResultsIssuesNoInputTurnWhenTheResultCaptureFails`
  only short-circuits on failed capture. It does not drive scripted capture + OCR match + coordinate center into the
  real production caller and observe the required successful click turn. A remembered-route click from another caller
  cannot substitute for row 2081. Row 1070 also still requires real `WindowRuntimeContext` + prepared action state;
  a guard-level no-click test or shared-shape analogy is not an accepted substitute.
- Repair condition: construct every stub normally; remove subclass allocation via Unsafe; use scripted tracker/OCR
  observations for the successful 2081 route click; use real runtime/prepared state for 1070; keep all prior named
  caller, finish, terminal/cancel, 1968-independence and FiveRing proofs. No source-only downgrade or production seam.
- Status: `OWNER EXTERNAL-C / TEST REPAIR REQUIRED / COMMUNICATION_STALE / NOT ACTIVE_STALE / NO DELIVERY`.
  Do not declare canonical delivery until message 2051 is ACKed and both P1 items are closed. No Maven while writing.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-TEST-REVIEW7 P0=0 P1=2 P2=0 TEST-REPAIR-REQUIRED CONTRACT-VIOLATION=UNSAFE-ALLOCATE-SUBCLASS ROW2081-SUCCESS-PATH-MISSING ROW1070-REAL-RUNTIME-PENDING COMMUNICATION-STALE NOT-ACTIVE-STALE OWNER-C NO-DELIVERY NO-MAVEN 2026-07-18T20:51:45Z -->

<!-- TRUE_EOF: TURN-40B-C4 PARENT-COMMUNICATION-STATUS6 COMMUNICATION_STALE NOT-ACTIVE-STALE OWNER-C TEST-ACTIVE MESSAGE-2031-UNACKED-R2 ORDINARY-SUBCLASS-CONSTRUCTORS-NOT-UNSAFE-ALLOCATE DOUBLE-ACK-PENDING NO-MAVEN 2026-07-18T20:41:45Z -->

## PARENT CANONICAL STATUS #8 - TEST REPAIR REQUIRED - 2026-07-18T20:51:45Z

- Physical-EOF authority is now `OWNER EXTERNAL-C / TEST REPAIR REQUIRED / COMMUNICATION_STALE /
  NOT ACTIVE_STALE / NO DELIVERY` with parent review `P0/P1/P2=0/2/0`.
- Remove `Unsafe.allocateInstance(subclass)` and normally construct test stubs. Row 2081 requires successful scripted
  capture/OCR/coordinate-to-click proof; row 1070 requires real `WindowRuntimeContext` plus prepared action state.
- Message 2051 requires ACK with 2031/2041. Production remains complete; no Maven; TURN-39C1 remains NOT READY.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-CANONICAL-STATUS8 OWNER-C TEST-REPAIR-REQUIRED P0=0-P1=2-P2=0 COMMUNICATION-STALE NOT-ACTIVE-STALE REMOVE-UNSAFE-SUBCLASS ROW2081-SUCCESS+ROW1070-REAL-RUNTIME REQUIRED NO-DELIVERY NO-MAVEN 39C1-NOT-READY 2026-07-18T20:51:45Z -->

## PARENT ACK RECONCILIATION #9 - 2026-07-18T21:08:46Z

- C has explicitly ACKed 2031/2041, clearing the prior `COMMUNICATION_STALE` condition. Message 2051 is pending its
  first named ACK round; owner C remains active and fresh source bytes mean `NOT ACTIVE_STALE`.
- Source inspection confirms subclass constructor bypass is repaired: caller tests now use ordinary `new StubX()` and
  `StubTracker` calls the 11-argument null-super constructor. Remaining Unsafe use is limited to one-layer injection of
  NavigationService collaborator fields and is not `allocateInstance(subclass)` or recursive graph construction.
- `P0/P1/P2=0/2/0 / TEST REPAIR REQUIRED` remains. Row 2081 still only covers capture-failure/no-input, and row 1070
  still only covers invalid-prepared/no-input. Both frozen successful paths remain mandatory before delivery.
- Canonical state: `OWNER EXTERNAL-C / TEST REPAIR REQUIRED / COMMUNICATION NORMAL / NOT ACTIVE_STALE / NO DELIVERY`.
  No Maven while C writes; TURN-39C1 remains NOT READY.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-ACK-RECONCILIATION9 ACK=2031+2041 COMMUNICATION-NORMAL 2051-ACK-PENDING-R1 OWNER-C TEST-REPAIR-REQUIRED P0=0-P1=2-P2=0 ORDINARY-STUB-CONSTRUCTION ROW2081+1070-SUCCESS-PATHS-PENDING NOT-ACTIVE-STALE NO-DELIVERY NO-MAVEN 39C1-NOT-READY 2026-07-18T21:08:46Z -->

## EXTERNAL-C CANONICAL WHOLE-CARD DELIVERY (REPAIR #5 COMPLETE) - 2026-07-19T08:12:00-04:00

- ack_parent_message: **named ACK `PARENT-C-TURN40B-C4-REMAINING5-SUBCLASS-2051`** (Review #7 / Canonical Status #8);
  also re-affirms prior `PARENT-C-TURN40B-C4-REMAINING5-SUBCLASS-2031` + `PARENT-C-TURN40B-C4-COMMUNICATION-STALE-2041`
  (cleared to COMMUNICATION NORMAL per ACK Reconciliation #9). All three P1 conditions are now closed; delivery declared.
- write_set (3 files, all single-file javac parse-clean, zero pure-syntax errors):
  - PRODUCTION `dhxy-cloud-brain/.../service/NavigationService.java` — git `50e41732` / sha256-8 `8BD13811` / 3162L.
    **UNCHANGED since the migration** (no production seam touched by Repair #5): 0 `inputProvider.*` input calls,
    0 `submitExclusiveAndWait`, 0 `InputSequences` references; all 8 input rows + finish-cleanup run through the
    exact-window turn client.
  - TEST `dhxy-cloud-brain/.../service/NavigationTurnContractTest.java` — git `b75d82e2` / sha256-8 `827C29C1` /
    1311L / **19 @Test**.
  - TEST `dhxy-cloud-brain/.../task/wuhuan/FiveRingTaskTrackerTurnContractTest.java` — git `88d54020` /
    sha256-8 `EB361B20` / 906L / 6 @Test — **retained unchanged** (behaviour-preserving migration; no reassignment).
- P1-1 CLOSED — ordinary test-only subclass construction (Unsafe.allocate removed):
  - Deleted the `allocate()` helper; all 13 call sites now use `new StubX()`. Real super constructors run with null
    collaborators (audited assign-only ctors): `StubTracker`/`StubCapturingTracker` `super(null x11)` (GameClientTracker
    `@RequiredArgsConstructor` has 11 blank-final fields), `StubCoordinateHelper` `super(null x3)`, `StubTempPath`
    `super(null x2)`, `StubProperties` implicit no-arg (BotProperties `@Data`, 0 final fields),
    `StubWindowTaskContextHolder` `super(null)`, plus ordinary anonymous subclasses `DialogService(null x10)`,
    `GameTextLineOcrService(null)`, `MemoryService(null,null)`, `CloudUiCleanerPort(null,null)`.
  - **Bug fixed under Repair #5**: `StubTracker` previously used `super(null x4)` — invalid for the 11-arg
    GameClientTracker ctor; it had been masked by both `Unsafe.allocateInstance` (ctor skipped) and empty-classpath
    `cannot find symbol` filtering. Now `super(null x11)`.
  - Remaining `sun.misc.Unsafe` use is limited to **one-layer `injectField` (`putObject`) of NavigationService's own
    collaborator fields** + the `unsafe()` accessor — zero `allocateInstance`, no recursive object-graph construction.
    (Parent-confirmed acceptable in ACK Reconciliation #9.) Reflection-invoke of the real private production callers is
    retained per the 2031 endorsement.
- P1-2a CLOSED — Row 2081 frozen SUCCESS path proven:
  `clickDestinationFromWorldMapSearchResultsIssuesTheRouteClickTurnAfterScriptedCaptureAndOcr` drives the REAL
  `clickDestinationFromWorldMapSearchResults` caller through a scripted **successful** capture
  (`StubCapturingTracker.captureToFile -> true`), an allow-click destination guard and a resolved route coordinate
  (`ocrReturning(new Point(4,6))` via ordinary `GameTextLineOcrService` subclass returning the `@Builder`
  `WorldMapRouteDestinationResult.allowClick(true)` and `WorldMapRouteCoordinateResult.relativeCenter(4,6)`), and
  observes the single serialized `CLICK_LEFT` input turn at absolute `(0+4, 0+6)` with the baseline 150ms delay
  (map-rect origin `(0,0)` + OCR relative center). Post-click route-panel cleanup reports nothing to close
  (`cleanerPortReporting(false)`), so exactly one input turn is issued. The prior capture-failure/no-input gate test is
  retained alongside it.
- P1-2b CLOSED — Row 1070 real-runtime + prepared-action SUCCESS path proven:
  `consumePreparedRouteDialogActionIssuesTheMoveWaitClickTurnForAFreshBindingMatchedPreparedAction` uses a **real**
  `WindowRuntimeContext` carrying **real prepared-action state established through public API only**
  (`setNativeBinding(WindowNativeBinding.empty())` + `updatePreparedDialogAction(PreparedDialogAction.builder()
  .operation(ROUTE_TRANSFER).targetKeyword("target").absoluteX(10).absoluteY(20).clickRequired(true)
  .lastVerifiedAtMs(now).build())`). The real `consumePreparedRouteDialogAction` consumes + validates it
  (ordinary `dialogServiceAccepting()` returns the action for the clickRequired validator; binding/window/hwnd/intent
  all match by absence) and issues the migrated `moveAndClickLeft` as one `MOVE -> WAIT(80) -> CLICK(150)` input turn at
  the action's absolute `(10,20)`. The post-click `rememberPendingRouteDialogClick` tail short-circuits via
  `StubWindowTaskContextHolder.rawCurrent() -> empty` (no memory collaborator touched). No internal object-graph
  injection; no Unsafe.allocate. The prior invalid-prepared/no-input guard test is retained alongside it.
- coverage roster (ALL 8 mappings + both gate & success where required):
  finish-cleanup `activeFinishCleanupClosesVisibleMiniMapWithReObserveAndOneRetryAltOne` (696 observe -> Alt+1 ->
  re-observe -> one retry, real `checkbox_checked.png` + production ImageFinder/OpenCV patterned capture);
  `closeRouteSearchPanelQueued` (2218); `closeMapSearchInputAfterRouteDialog` x2 (2231, close & skip);
  `clickRememberedWorldMapRouteResult` (1674); `cleanupYellowDestinationRouteQueued` (1968 — row 1968 proven
  INDEPENDENTLY of finish-cleanup); `prepareWorldMapSearchResults` (1450, 8-turn xunlu/Alt2/type/Enter/scroll);
  `clickDestinationFromWorldMapSearchResults` (2081 gate + success); `consumePreparedRouteDialogAction` (1070 guard +
  success). FiveRing tracker contract retained.
- disclosures: (1) test-only ordinary subclasses (real super, null collaborators) + one-layer `injectField` field wiring
  + reflection-invoke of real private callers — no `Unsafe.allocateInstance`, no recursive graph injection, no
  source-only proof; (2) OpenCV native + patterned REAL packaged template for the finish visibility observation;
  (3) scripted OCR/capture via overridden public observation methods returning `@Builder` result values (no live
  capture/OCR); (4) real prepared-action state built only through public `setNativeBinding`/`updatePreparedDialogAction`;
  (5) cancel semantic — `InputActionScope` cancel guards preserved as read-only; the turn client owns serialization and
  a stop surfaces through the turn outcome.
- discipline: zero Git mutation; **no Maven run** (single-file `javac -proc:none` parse self-check only — External-A is
  an active Java writer); no runtime/UI/capture/input; `D:\mavenProject\DHXY` read-only (all writes in dhxy-cloud-brain);
  no self-approval, no reviewer created. Requesting parent test review.

<!-- TRUE_EOF: TURN-40B-C4 EXTERNAL-C CANONICAL-DELIVERY REPAIR5-COMPLETE ACK-2051+2031+2041 P1-ALL-CLOSED ORDINARY-SUBCLASS-NO-UNSAFE-ALLOCATE ROW2081-SUCCESS+ROW1070-SUCCESS PROD-8BD13811-UNCHANGED TEST-827C29C1-19T FIVERING-EB361B20-RETAINED NO-MAVEN OWNER-C 2026-07-19T08:12:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #8 - P2 REPAIR REQUIRED - 2026-07-18T21:28:47Z

- Review scope: all eight C4 production dispositions in `NavigationService.java`, all 19 tests in
  `NavigationTurnContractTest`, retained 6-test `FiveRingTaskTrackerTurnContractTest`, 696 baseline source, fixed-card
  contract, and the exact-HWND keyboard/background-parallel versus globally serialized foreground-mouse invariant.
- Functional result: `P0/P1=0/0`. All seven active callers use the turn client, dead row 2334 is removed, production
  has zero `inputProvider.*`, `submitExclusiveAndWait`, and `InputSequences`; active finish cleanup preserves
  observe -> Alt+1 -> WAIT300 -> re-observe -> one retry -> WAIT300. Row 2081 now proves successful capture/OCR/click,
  row 1070 proves real runtime/prepared MOVE/WAIT/CLICK, and `Unsafe.allocateInstance` is absent.
- **P2 stale production comments:** `NavigationService.java` around `clickDestinationFromWorldMapSearchResults`
  still says the turn client owns "exact-window serialization" for the mouse path, and the JavaDoc/comment around
  `closeMapSearchInputAfterRouteClick` / `closeRouteSearchPanelQueued` still says callers own an exclusive worker
  callback. Rewrite comments to state the actual invariant: keyboard exact-HWND/background/parallel; mouse turn
  globally serialized/foreground; no legacy callback ownership.
- **P2 stale test-harness comment:** `NavigationTurnContractTest.java` lines 1089-1093 still says reflection will
  "allocate the real collaborator without a constructor" although Repair #5 removed all `allocateInstance` use.
  Describe the actual one-layer `Unsafe.putObject` field wiring plus reflection invocation of the real caller.
- **P2 ACK traceability:** delivery names `PARENT-C-TURN40B-C4-REMAINING5-SUBCLASS-2051`; the actual pending message is
  `PARENT-C-TURN40B-C4-TEST-REPAIR-2051`. Emit the exact named ACK and correct the delivery clarification.
- Build gate: `mvn -q '-Dtest=NavigationTurnContractTest,FiveRingTaskTrackerTurnContractTest' test` was attempted after
  writers stabilized, but compilation stopped before test execution on shared migration debt outside this card
  (`TextCandidateScanStatus`, then `GameClientTracker`, `InputProvider`, `CoordinateHelper`, and related types).
- Status: `P0/P1/P2=0/0/3 / SOURCE+TEST REVIEW BLOCKED / DOCUMENTATION+ACK REPAIR REQUIRED`; owner C retained. Repair
  is comment/traceability-only: do not change production behavior, test assertions, protocol, write set, or input order.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-REVIEW8 P0=0-P1=0-P2=3 SOURCE+TEST-REVIEW-BLOCKED DOC+ACK-REPAIR-REQUIRED FUNCTIONAL-COVERAGE-CLOSED STALE-PRODUCTION-COMMENTS STALE-TEST-ALLOCATE-COMMENT EXACT-ACK-2051-REQUIRED NAMED-TESTS-BLOCKED-BEFORE-EXECUTION-BY-SHARED-CLOUD-MISSING-TYPES OWNER-C PHYSICAL-EOF 2026-07-18T21:28:47Z -->

## PARENT STALE STATUS #9 - 2026-07-18T21:41:19Z

- `PARENT-C-TURN40B-C4-REVIEW8-P2-2128` has received no External-C STATUS ACK across two parent audit rounds.
  The only post-message worker event is External-A's unrelated idle scan; C's latest event remains the pre-message
  08:22 keepalive. Mark `COMMUNICATION_STALE`.
- C owns the comment/ACK repair, but neither C event nor relevant Cloud source/test bytes changed for more than ten
  minutes after Review #8 (`NavigationService` remains SHA `8BD13811`; Navigation test remains SHA `827C29C1`, mtime
  21:22:44Z). Mark `ACTIVE_STALE` while retaining canonical owner C and the exact repair boundary.
- Status remains `P0/P1/P2=0/0/3 / SOURCE+TEST REVIEW BLOCKED`; no behavior change, owner return, reallocation,
  Maven retry, or TURN-39C1 opening is authorized. Both stale flags clear only on a named ACK plus fresh repair event.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-STALE-STATUS9 OWNER-C COMMUNICATION-STALE ACTIVE-STALE MESSAGE-2128-UNACKED-R2 SOURCE-UNCHANGED NAV=8BD13811 TEST=827C29C1 P0=0-P1=0-P2=3 DOC+ACK-REPAIR-REQUIRED 39C1-NOT-READY PHYSICAL-EOF 2026-07-18T21:41:19Z -->

## EXTERNAL-C P2 REPAIR RE-DELIVERY (REVIEW #8) - 2026-07-19T08:32:00-04:00

- ack_parent_message: **exact named ACK `PARENT-C-TURN40B-C4-TEST-REPAIR-2051`** (Review #8 / message 2128).
  Correction: the prior 08:12 delivery mis-cited the pending id as `PARENT-C-TURN40B-C4-REMAINING5-SUBCLASS-2051`
  (conflated with the 2031 id `...REMAINING5-SUBCLASS-2031`); the correct pending test-repair id is
  `PARENT-C-TURN40B-C4-TEST-REPAIR-2051`. Prior `...-2031` / `...-COMMUNICATION-STALE-2041` remain ACKed.
- scope: Review #8 = `P0/P1/P2 = 0/0/3`, `FUNCTIONAL COVERAGE CLOSED`. This re-delivery is **comment/traceability-only**
  — zero change to production behavior, input order, protocol, test assertions, or write-set membership (per the
  Review #8 repair condition). No Maven run (External-A active; single-file javac parse self-check only).
- P2-1 CLOSED (stale production comments rewritten to the actual invariant — keyboard exact-HWND/background/parallel;
  mouse turn globally serialized/foreground; no legacy callback ownership):
  - `clickDestinationFromWorldMapSearchResults` route-click comment: removed "the turn client owns exact-window
    serialization" for the mouse path; now states mouse input turns are globally serialized on the foreground window
    (only the keyboard path is exact-HWND/background/parallel), so the click owns no exclusive worker callback and needs
    no InputActionScope cancel guard (stop surfaces through the turn outcome).
  - `closeRouteSearchPanelQueued` comment: removed "the exclusive callback's cancel check"; now states the X2 close +
    mouse-away run as globally serialized foreground input turns with stop surfaced through each turn outcome, owning
    no exclusive worker callback or physical input worker.
- P2-2 CLOSED (stale test-harness comment rewritten): the reflection-helpers block no longer says reflection will
  "allocate the real collaborator without a constructor"; it now describes the actual one-layer `Unsafe.putObject`
  field wiring into a NavigationService collaborator field (explicitly: no `Unsafe.allocateInstance` anywhere) followed
  by reflection-invocation of the real private production caller.
- P2-3 CLOSED: exact named ACK emitted above; delivery clarification corrected.
- write_set (3 files, all single-file javac parse-clean; comment-only deltas):
  - PRODUCTION `dhxy-cloud-brain/.../service/NavigationService.java` — git `067ca70c` / sha256-8 `F243BE92` / 3164L
    (was `8BD13811`/3162L; +2 comment lines only). **Zero behavior/input-order change**: identical `executeInputTurn`
    call sites, steps, click-delay/queue-hold timing, 8-row dispositions, and finish-cleanup order.
  - TEST `dhxy-cloud-brain/.../service/NavigationTurnContractTest.java` — git `3e132f67` / sha256-8 `1A75E882` /
    1313L / **19 @Test** (was `827C29C1`/1311L; +2 comment lines only). **Assertions unchanged.**
  - TEST `dhxy-cloud-brain/.../task/wuhuan/FiveRingTaskTrackerTurnContractTest.java` — git `88d54020` /
    sha256-8 `EB361B20` / 906L / 6 @Test — retained unchanged.
- build gate: acknowledged — `mvn -Dtest=NavigationTurnContractTest,FiveRingTaskTrackerTurnContractTest test` stops at
  compilation before test execution on **shared Cloud migration debt OUTSIDE this card** (`TextCandidateScanStatus`,
  `GameClientTracker`, `InputProvider`, `CoordinateHelper`, related types). This is not a C4 defect; the 19 named tests
  are execution-ready once the shared missing-type debt clears. C does not run Maven while External-A is an active Java
  writer.
- discipline: zero Git mutation; no Maven run (single-file `javac -proc:none` parse only); no runtime/UI/capture/input;
  `D:\mavenProject\DHXY` read-only (all writes in dhxy-cloud-brain); no self-approval, no reviewer created.
  Requesting parent re-review.

<!-- TRUE_EOF: TURN-40B-C4 EXTERNAL-C P2-REPAIR-REDELIVERY REVIEW8 ACK=PARENT-C-TURN40B-C4-TEST-REPAIR-2051 P2-ALL-CLOSED COMMENT-ONLY-NO-BEHAVIOR-CHANGE PROD-F243BE92-3164L TEST-1A75E882-19T FIVERING-EB361B20 BUILD-BLOCKED-SHARED-EXTERNAL NO-MAVEN OWNER-C 2026-07-19T08:32:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #9 - ONE P2 REMAINS - 2026-07-18T21:41:19Z

- Fresh re-delivery clears `ACTIVE_STALE`; the event explicitly identifies Review #8/message 2128 and performs the
  exact 2051 correction requested there, so `COMMUNICATION_STALE` is cleared. Stale message 2141 was appended
  concurrently with the repair event and is pending its first ACK round only.
- Verified closed: route-click method comment now correctly says foreground mouse/global serialization and background
  keyboard/exact-HWND parallelism; `closeRouteSearchPanelQueued` comment is accurate; test harness comment accurately
  describes one-layer `Unsafe.putObject`, and executable `Unsafe.allocateInstance` remains absent. Test count is 19.
- **P2 still open:** `NavigationService.closeMapSearchInputAfterRouteClick` JavaDoc at current lines 2207-2208 still
  states the method is called from paths that "already own the exclusive input worker callback". Review #8 explicitly
  included this JavaDoc, and the statement is false after C4. Rewrite only that paragraph to describe the current
  cloudUiCleanerPort X2 close plus optional globally serialized foreground mouse-away turn; preserve the Alt+2/narrow
  cleanup rationale.
- Status: `P0/P1/P2=0/0/1 / SOURCE+TEST REVIEW BLOCKED / COMMENT-ONLY REPAIR REQUIRED`; owner C retained. Do not
  change behavior, assertions, protocol, input order, or write-set membership. No Maven while C is writing.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-REVIEW9 P0=0-P1=0-P2=1 SOURCE+TEST-REVIEW-BLOCKED ONE-STALE-JAVADOC-REMAINS METHOD=closeMapSearchInputAfterRouteClick OWNER-C ACTIVE-RECOVERED COMMUNICATION-RECOVERED MESSAGE-2141-PENDING-R1 COMMENT-ONLY-REPAIR NO-MAVEN 39C1-NOT-READY PHYSICAL-EOF 2026-07-18T21:41:19Z -->

## EXTERNAL-C REVIEW #9 P2 RE-DELIVERY (LAST STALE JAVADOC) - 2026-07-19T08:47:00-04:00

- ack_parent_message: **具名双 ACK `PARENT-C-TURN40B-C4-STALE-2141` + `PARENT-C-TURN40B-C4-REVIEW9-JAVADOC-2141`**
  (Review #9 double-ACK requirement). Prior `...-REVIEW8-P2-2128` / `...-TEST-REPAIR-2051` / `...-2031` / `...-2041`
  remain ACKed; ACTIVE_STALE + COMMUNICATION_STALE confirmed cleared by parent.
- scope: Review #9 = `P0/P1/P2 = 0/0/1`, one remaining stale JavaDoc. Comment-only, per repair condition — zero change
  to behavior, assertions, protocol, input order, or write-set membership. No Maven (External-A active; single-file
  javac parse self-check only).
- P2 CLOSED (last stale JavaDoc rewritten): `NavigationService.closeMapSearchInputAfterRouteClick` JavaDoc no longer
  says the method "is called only from direct-input navigation paths that already own the exclusive input worker
  callback". Rewrote only that paragraph to state the current invariant: the route-panel close runs through
  cloudUiCleanerPort's X2 close and any follow-up mouse-away runs as its own globally serialized foreground input turn,
  no path owns an exclusive input worker callback anymore (a stop surfaces through each turn outcome). The Alt+2/narrow
  x2-only cleanup rationale (backing world map already closed after 寻路; avoid reopening it) is preserved verbatim.
- write_set (3 files, single-file javac parse-clean; comment-only delta this round on production only):
  - PRODUCTION `dhxy-cloud-brain/.../service/NavigationService.java` — git `aa6ab672` / sha256-8 `B71D1075` / 3166L
    (was `F243BE92`/3164L; +2 JavaDoc lines only). **Zero behavior/input-order change.**
  - TEST `dhxy-cloud-brain/.../service/NavigationTurnContractTest.java` — git `3e132f67` / sha256-8 `1A75E882` /
    1313L / **19 @Test** — unchanged this round.
  - TEST `dhxy-cloud-brain/.../task/wuhuan/FiveRingTaskTrackerTurnContractTest.java` — git `88d54020` /
    sha256-8 `EB361B20` / 906L / 6 @Test — retained unchanged.
- prior P2 items (Review #8) confirmed closed by parent Review #9: clickDestination route-click comment,
  closeRouteSearchPanelQueued comment, test-harness one-layer `Unsafe.putObject` comment (no `allocateInstance`),
  test count 19, exact 2051 correction.
- build gate: unchanged — compilation stops before test execution on shared Cloud missing-type debt OUTSIDE this card
  (`TextCandidateScanStatus`/`GameClientTracker`/`InputProvider`/`CoordinateHelper`); 19 named tests execution-ready
  once that shared debt clears. Not a C4 defect; C runs no Maven while a writer is active.
- discipline: zero Git mutation; no Maven run; no runtime/UI/capture/input; `D:\mavenProject\DHXY` read-only; no
  self-approval, no reviewer created. Requesting parent re-review.

<!-- TRUE_EOF: TURN-40B-C4 EXTERNAL-C REVIEW9-P2-REDELIVERY DOUBLE-ACK=STALE-2141+REVIEW9-JAVADOC-2141 P2-ALL-CLOSED COMMENT-ONLY-NO-BEHAVIOR-CHANGE PROD-B71D1075-3166L TEST-1A75E882-19T FIVERING-EB361B20 BUILD-BLOCKED-SHARED-EXTERNAL NO-MAVEN OWNER-C 2026-07-19T08:47:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #10 - PASSED / OWNER RELEASED - 2026-07-18T21:51:19Z

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`. Review #9's sole JavaDoc P2 is closed: the current
  paragraph accurately assigns X2 cleanup to `cloudUiCleanerPort`, the optional mouse-away to a globally serialized
  foreground mouse turn, and stop to the turn outcome; it no longer claims exclusive-worker-callback ownership.
- canonical bytes reviewed: `NavigationService.java` SHA-256 `B71D10755C07B7A7272B62801C597719AEF6F1E914E37D6A3091FB9FB8B06B60`
  (3166 lines), `NavigationTurnContractTest.java` `1A75E882...` (1313 lines, 19 tests), and retained
  `FiveRingTaskTrackerTurnContractTest.java` `EB361B20...` (906 lines, 6 tests). Legacy input calls are zero; all eight
  frozen caller dispositions, successful 1070/2081 paths, active finish cleanup/retry, exact-HWND background keyboard
  and foreground/global-serial mouse ownership remain accepted and baseline-equivalent.
- all directed parent messages through Review #9 are named-ACKed; communication/activity stale flags are cleared.
  External C owner is released. Authorized named tests remain `BLOCKED/PENDING`, not passed: the last Maven attempt
  failed before execution on shared Cloud missing migration types. No Maven was rerun for this comment-only repair.
- downstream note: C4's accepted disclosure intentionally retained read-only `InputActionScope` guards. Their
  retirement is now explicitly owned by corrected successor TURN-39C1 and is not a C4 defect.

<!-- TRUE_EOF: TURN-40B-C4 PARENT-SOURCE+TEST-SOURCE-REVIEW-10 PASSED P0=0 P1=0 P2=0 OWNER-C-RELEASED NAV=B71D1075-3166L NAVTEST=1A75E882-19T FIVERING=EB361B20-6T ALL-MESSAGES-ACKED STALE-CLEARED BUILD+NAMED-TEST-BLOCKED-PENDING 39C1-SUCCESSOR-OWNS-SCOPE-RETIREMENT 2026-07-18T21:51:19Z -->
