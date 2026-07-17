# TURN-34AT1 - AutoCombat Stage-1 battle-flag turn contract

## PARENT FROZEN CARD - EXTERNAL-C NEXT - 2026-07-16T09:59:30-04:00

- Card type: bounded real Cloud test implementation slice of TURN-34A; not helper/reviewer work.
- Status: `READY / CLAIM REQUIRED / TEST-START OPEN`.
- Owner after true-EOF claim: CR271 External Worker C.
- Business authority: strict `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no approved behavior difference.

## Exact write set

1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`.
2. This append-only child card.

Initial test: 762 lines, SHA-256
`4b8460b0f88b0d2f917b66663b8b6835d272c287db020e7831b49d094f1d21cc`.
Production `AutoCombatService.java` is read-only at
`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`. POM/resources/callers/collaborators,
TURN-34A/AT0 and every other test/card are read-only.

## Frozen test contract

Reuse the existing public harness, real `PackagedTemplateAssets`, real `BattleRadarService`, scripted
`CloudTurnCommandPort` and in-memory raw PNG. Do not copy a production reducer, add a production hook, use private
reflection/source scans/wall-clock polling, or start runtime/input/capture.

1. Adjust only the normal exact-window test fixture to `TurnWindowRect(100,50,1280,800)`; preserve dedicated
   invalid-ROI cases. From `FREE`, public `probeWindowCombatStateReadOnly(context,"fivering")` receives the real
   committed `flag_battle.png` in Stage-1 ROI and must return/set `IN_COMBAT` after exactly one capture.
2. Assert one canonical fresh UUID, one command, one index-0 `CAPTURE`, no input/local-service/match step, exact
   screen region `(1074,680,51,20)`, `UPLOAD_IMAGE`, timeout 120s, exact current metadata, correlated raw PNG
   width/height/region/sourceStepIndex/SHA, and exhausted scripted replies. Stage-2/3 command count is zero.
3. From `IN_COMBAT`, cover first-capture command statuses `BUSY`, `DUPLICATE_ACTION_ID`,
   `TIMED_OUT_UNCERTAIN`, `INTERRUPTED_UNCERTAIN` and outcome statuses `FAILED`, `STOPPED`,
   `DUPLICATE_OR_UNCERTAIN`. Each case publishes exactly one unique UUID/command, keeps `IN_COMBAT`, and sends no
   Stage-2/3, compensation, retry, fallback or second action. `STOPPED` latest metadata remains stop=false so the
   existing confirmed-stop zero-command tests stay distinct.
4. Preserve all existing 17 tests and assertions. Normal trusted `COMPLETED` blank-frame Stage1->2->3 and
   two-round exit/minimap confirmation belong to later AT2; recovery/maintenance/timing/callers belong AT3+.

No auto retry/replay/resend/session/ledger/TTL/durable workflow. AT1 alone cannot claim TURN-34A test-source pass
or card approval. Parent review, later test tranches, two independent reviews and stable-writer named test/compile
remain required.

## Claim and delivery

External C must append `EXTERNAL-C CLAIMED` here before editing and make a real test increment in its first
five-minute heartbeat window. Delivery is one true-EOF `EXTERNAL-C TEST-SOURCE DELIVERED` with final SHA and exact
case/line evidence, then stop editing. No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/
input or Git mutation while writers are active.

<!-- TRUE_EOF: TURN-34AT1 PARENT FROZEN EXTERNAL-C-NEXT CLAIM-REQUIRED ONE-TEST-FILE STAGE1-BATTLE-FLAG 2026-07-16T09:59:30-04:00 -->

## EXTERNAL-C CLAIMED - 2026-07-16T10:01:07.611-04:00

EXTERNAL-C CLAIMED | card=TURN-34AT1 | lane=CR271-External-Worker-C | role=implementation-worker(not-reviewer) | claimedAt=2026-07-16T10:01:07.611-04:00 | model=claude-opus-4-8 | platformAgentId=<待父级平台真实 ID 校正> | scope=stage1-battle-flag-only

- 写集：仅 `AutoCombatServiceTurnContractTest.java` + 本 append-only 子卡。production `532e6f84...` 只读；POM/resources/callers/collaborators、TURN-34A/AT0 及其它测试/卡只读。
- 起始 test SHA=`4b8460b0f88b0d2f917b66663b8b6835d272c287db020e7831b49d094f1d21cc`（762 行）；**保留全部 17 个既有 @Test 与断言**。
- 仅做 AT1 四项：normal fixture 改 `TurnWindowRect(100,50,1280,800)`（保留 invalid-ROI 专用例）；FREE + Stage-1 真实 `flag_battle.png` → 一次 capture 即 IN_COMBAT；命令面断言（1 UUID/1 command/index-0 CAPTURE/region `(1074,680,51,20)`/UPLOAD_IMAGE/120s/metadata/raw PNG 关联/脚本耗尽/Stage-2·3=0）；IN_COMBAT 下首帧 BUSY/DUPLICATE_ACTION_ID/TIMED_OUT_UNCERTAIN/INTERRUPTED_UNCERTAIN 与 FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN 各保持 IN_COMBAT、零 Stage-2·3/补偿/重试/二次 action，STOPPED 的 latest metadata 保持 stop=false。
- 不复制 reducer、不加 production hook、不用 private reflection/source scan/wall-clock polling、不启 runtime/input/capture；无 auto retry/replay/resend/session/ledger/TTL/durable workflow。
- **不声称** AT1 等同 TURN-34A test-source 通过或卡批准；AT2/AT3+ 仍待。
- 不运行 Maven/JUnit/compile/package；不执行任何 Git mutation；两仓 dirty/untracked 原样保护。

## PARENT CLAIM REGISTRATION - 2026-07-16T10:02:20-04:00

- External C's claim body was appended at `10:01:07` but omitted its own trailing marker. Parent directly verified
  a real first-window test increment at `10:01:16`: 762 -> 767 lines, SHA `4b8460b0... -> 04be925e...`.
- On that non-fabricated claim+source evidence, parent registers C as the sole AT1 implementation owner and forbids
  any replacement/double writer. C must still append a canonical true-EOF delivery or owner return when it stops.

<!-- TRUE_EOF: TURN-34AT1 PARENT CLAIM-REGISTRATION EXTERNAL-C-SOLE-OWNER REAL-FIRST-WINDOW-INCREMENT SHA=04be925e9cdd7ce8d1503fb378abcf812e8bdb9fe4706dc169f41325f9e084c3 2026-07-16T10:02:20-04:00 -->

## EXTERNAL-C TEST-SOURCE DELIVERED - 2026-07-16T10:17:06.119-04:00

TEST-SOURCE DELIVERED | card=TURN-34AT1 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T10:17:06.119-04:00 | scope=stage1-battle-flag-only

- test SHA `4b8460b0f88b0d2f917b66663b8b6835d272c287db020e7831b49d094f1d21cc`(762 行) → **`6be1f3bf0f7037aa34ac9bc95c8245b93e59a88a30966d95ffbc1a77fcb45c68`(963 行)**；production `AutoCombatService.java` 复算 **`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` 字节未动**；写集仅本 test + 本卡。
- `@Test` 17 → **21**：既有 17 个**一个未删**，新增 4 个 AT1 用例。

### §1 normal fixture 与 Stage-1 进战

- `:90` `RECT` 由 `(100,50,820,820)` 改为 **`(100,50,1280,800)`**（附注释依据：Stage-1 ROI 底部 local y=650、Stage-2 右缘 local x=1027，820×820 会令 `isInsideWindow` 判越界→unavailable，Stage-1 根本进不到模板匹配）。invalid-ROI 专用例经 `windowWithRect(...)` 独立构造，**未受影响**。
- `:378 stage1BattleFlagEntersCombatWithExactlyOneCanonicalCapture`：FREE + 真实 committed `flag_battle.png`（经 `PackagedTemplateAssets` 公有加载后绘入 ROI 帧）→ public `probeWindowCombatStateReadOnly(context,"fivering")` 返回 **IN_COMBAT** 且 `gameContext` 置 IN_COMBAT，**恰一次 capture**。

### §2 命令面断言（同一用例内）

`executeCalls==1`、`actions.size()==1`、**scripted replies 耗尽**、device/window 为 exact binding、**单 step**、`index==0`、`TurnStepType.CAPTURE`、`step.input()==null`（无 input/local-service/match step）、`region == STAGE1_SCREEN_REGION`（由 `RECT.left()+974, RECT.top()+630, 51, 20` **推导**，即卡载明的 `(1074,680,51,20)`）、`ResultMode.UPLOAD_IMAGE`、**timeout == `Duration.ofSeconds(120)`**（源：`BattleRadarService:96 TURN_CAPTURE_TIMEOUT` → `:561 client.capture(region,false,TURN_CAPTURE_TIMEOUT)`）、`result.status()==COMPLETED`、outcome 关联 **exact current metadata** 与 actionId、frame `purpose=CAPTURE / contentType=image/png / region / width / height / sourceStepIndex==0`、**`sha256(frame.pngBytes()) == frame.sha256()`**。Stage-2/3 命令数为 **0**（由"脚本耗尽即 AssertionError"结构性保证）。`:431` 另断言 actionId 为 **规范 UUID 且唯一**。

### §3 首帧异常态（IN_COMBAT 起）

- `:456 stage1CommandRejectionsKeepInCombatWithExactlyOneCommand`：`BUSY`/`DUPLICATE_ACTION_ID`/`TIMED_OUT_UNCERTAIN`/`INTERRUPTED_UNCERTAIN` 四态（工厂签名均 `(String actionId)`）——每例**保持 IN_COMBAT**、`executeCalls==1`、唯一 actionId、脚本耗尽、**零 Stage-2·3/补偿/重试/二次 action**。
- `:492 stage1OutcomeFailuresKeepInCombatWithExactlyOneCommand`：`FAILED`/`STOPPED`/`DUPLICATE_OR_UNCERTAIN` 三态同上。**已核实两层语义**：非 COMPLETED 的 *command* 依 `CloudTurnCommandResult` 不变式不得携 outcome，故这三个 *outcome* status 由 **COMPLETED command 携带、frame 缺席**（helper `nonCompletedOutcome`，step 结果 `NOT_RUN`）。用例内 `assertFalse(live.stopRequested())` **显式锁定 latest slot stop=false**，与既有 confirmed-stop 零命令用例保持区分。

### 未运行门与不声称

- **不声称** compile exit 0（writers 活动期间未运行 Maven/JUnit/compile/package）；**不声称** AT1 等同 TURN-34A test-source 通过或卡批准。
- 未复制 reducer、未加 production hook、未用 Mockito/private reflection/source scan/wall-clock polling、未启 runtime/input/capture；无 auto retry/replay/resend/session/ledger/TTL/durable workflow。
- blank-frame Stage1→2→3 与两轮 exit/minimap 确认属 **AT2**；recovery/maintenance/timing/callers 属 **AT3+**。
- 未执行任何 Git mutation；两仓 dirty/untracked 原样保护。已停止编辑，交父级审查，不自批。

<!-- TRUE_EOF: TURN-34AT1 EXTERNAL-C TEST-SOURCE DELIVERED SHA=6be1f3bf0f7037aa34ac9bc95c8245b93e59a88a30966d95ffbc1a77fcb45c68 2026-07-16T10:17:06.119-04:00 -->

## PARENT TEST-SOURCE REVIEW #1 - 2026-07-16T10:23:00-04:00

- Verdict: **`P0/P1/P2=0/2/0 / REPAIR #1 REQUIRED`**. Delivery identity is real: test SHA
  `6be1f3bf0f7037aa34ac9bc95c8245b93e59a88a30966d95ffbc1a77fcb45c68`, 963 lines and 21 `@Test` methods;
  read-only production `AutoCombatService.java` remains
  `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
- **P1-1 - frozen minimal CAPTURE shape is not actually guarded.** At test lines `403-410`, the delivery only
  asserts `step.input()==null`. `TurnStep` also exposes `inputAction()`, `match()` and `localService()`; the frozen
  card explicitly requires no input/local-service/match step. A regression that adds any of those fields would
  still pass. Repair must assert the full null shape directly (and `waitMs()==null` for this capture-only turn),
  while preserving one index-0 `CAPTURE`, exact region, `UPLOAD_IMAGE` and 120-second timeout.
- **P1-2 - terminal-case UUID evidence is vacuous.** At lines `456-518`, every iteration creates a fresh harness
  containing exactly one action and then checks `distinct().count()==1`; this succeeds for a null/non-canonical ID
  and cannot detect reuse between the four command-status and three outcome-status cases. The separate positive
  test at lines `431-448` also observes only one invocation. Repair must validate every emitted ID as a canonical
  UUID and prove freshness across the exercised invocations (including all seven terminal cases), while retaining
  exactly one command and zero Stage-2/3/retry/fallback per case.
- Accepted evidence retained: real packaged `flag_battle.png`, exact `(1074,680,51,20)` ROI, correlated outcome
  metadata/raw-PNG SHA, one command on the positive path, and terminal/uncertain keep-IN_COMBAT/no-second-command
  behavior. No production change or broader test tranche is requested.
- External C remains the sole test-only Repair #1 owner. Exact write set stays this test plus this child card;
  production/POM/resources/other tests/cards remain read-only. No Maven/JUnit/compile was run while External A is
  an active Java writer.

<!-- TRUE_EOF: TURN-34AT1 PARENT TEST-SOURCE REVIEW-1 REPAIR-1-REQUIRED P0P1P2=0/2/0 EXTERNAL-C-RETAINS-OWNER TEST-ONLY 2026-07-16T10:23:00-04:00 -->

## EXTERNAL-C REPAIR #1 TEST-SOURCE DELIVERED - 2026-07-16T10:27:27.758-04:00

REPAIR #1 TEST-SOURCE DELIVERED | card=TURN-34AT1 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T10:27:27.758-04:00

- test SHA `6be1f3bf0f7037aa34ac9bc95c8245b93e59a88a30966d95ffbc1a77fcb45c68`(963 行) → **`35116f19f57f170a4ca6e56fadf11d9047b76520a8f61f24b86fb63e11ec10a4`(1020 行)**；`@Test` 21 → **22**（既有 17 个一个未删）。production 复算 **`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` 字节未动**；写集仅本 test + 本卡。

### P1-1 修复 — frozen minimal CAPTURE shape 现被真正守住

父级指出属实：原仅断言 `step.input()==null`，而 `TurnStep` 实为 **8 字段** record（`index,type,inputAction,input,waitMs,capture,match,localService`），加 input-action/wait/match/local-service 的回归仍会通过。现 `:409-413` 直接断言完整 null shape：`inputAction()`/`input()`/`waitMs()`/`match()`/`localService()` 全为 null，并保留 `index==0`、`TurnStepType.CAPTURE`、`region==STAGE1_SCREEN_REGION`(=`(1074,680,51,20)`)、`ResultMode.UPLOAD_IMAGE`、`timeout==Duration.ofSeconds(120)`。

### P1-2 修复 — terminal UUID 证据不再空洞

父级指出属实且切中要害：原每轮**新建 harness**、仅含一个 action，`distinct().count()==1` **恒真**（null/非规范 id 亦可通过），无法发现跨 case 复用。现：

- 新增 `:560 collectCanonicalActionIds(harness)`：对**每个**已发布 action 断言 id 非空且 `UUID.fromString(id).toString().equals(id)`（规范 UUID），返回全部 id。
- 新增 `:573 assertFreshCanonicalIds(ids, expected)`：断言**精确条数** + `Set.copyOf(ids).size()==expected`（跨调用零复用）。
- 新增 `:526 everyStage1InvocationEmitsAFreshCanonicalUuidAcrossAllTerminalCases`：**同一 harness/同一 service** 连续驱动**全部 7 个 terminal case**（BUSY→DUPLICATE_ACTION_ID→TIMED_OUT_UNCERTAIN→INTERRUPTED_UNCERTAIN→FAILED→STOPPED→DUPLICATE_OR_UNCERTAIN），每次均 IN_COMBAT，断言 `executeCalls==7`、脚本耗尽、**7 个 id 全为规范 UUID 且互不重复**——即真正证明"每次调用新 UUID、无重试/重发/回退"。
- 原四态用例(`:487`)、三态用例(`:523`)、正例(`:450`)统一改用同一 helper（分别 expected=4/3/1），不再自证式 distinct。

### 未运行门与不声称

- 未运行 Maven/JUnit/compile/package（External A 为活动 Java writer）；**不声称** compile exit 0，**不声称** AT1 等同 TURN-34A test-source 通过或卡批准。
- 未改 production/POM/resources/其它测试或卡；未复制 reducer、未加 production hook、未用 Mockito/private reflection/source scan/wall-clock polling；无 auto retry/replay/resend/session/ledger/TTL/durable workflow。
- blank-frame Stage1→2→3 与两轮 exit/minimap 属 **AT2**；recovery/maintenance/timing/callers 属 **AT3+**。
- 未执行任何 Git mutation；两仓 dirty/untracked 原样。已停止编辑，交父级审查，不自批。

<!-- TRUE_EOF: TURN-34AT1 EXTERNAL-C REPAIR #1 TEST-SOURCE DELIVERED SHA=35116f19f57f170a4ca6e56fadf11d9047b76520a8f61f24b86fb63e11ec10a4 2026-07-16T10:27:27.758-04:00 -->

## PARENT TEST-SOURCE REVIEW #2 - 2026-07-16T10:31:00-04:00

- Verdict: **`P0/P1/P2=0/1/0 / REPAIR #2 REQUIRED`**. Repair #1 correctly closes the full CAPTURE null shape
  and canonical/fresh evidence across all seven terminal cases at test SHA `35116f19...`; production remains
  frozen `532e6f84...`.
- **P1-1 - positive-path freshness is still unproved.** The new test JavaDoc at lines `526-529` says it drives
  “all seven terminal cases plus the positive path” through one shared service, but lines `539-556` enqueue and
  execute only the seven terminal replies. The positive test at lines `438-450` still observes one action and
  calls `assertFreshCanonicalIds(...,1)`, which cannot detect UUID reuse across positive invocations. This leaves
  frozen contract item 2's “one canonical fresh UUID” only canonical, not fresh.
- Repair only the existing shared-service freshness test: include one real trusted Stage-1 completed battle-flag
  capture in the same sequence as the seven terminal cases and assert eight invocations, eight commands, exhausted
  replies, eight canonical IDs and eight distinct IDs. Keep the already-passed terminal behavior and all other
  tests byte-equivalent; no production/POM/resource/other-test change.
- External C remains sole test-only Repair #2 owner. No Maven/JUnit/compile was run while CR271 Java writers are
  active.

<!-- TRUE_EOF: TURN-34AT1 PARENT TEST-SOURCE REVIEW-2 REPAIR-2-REQUIRED P0P1P2=0/1/0 EXTERNAL-C-RETAINS-OWNER POSITIVE-FRESHNESS 2026-07-16T10:31:00-04:00 -->

## EXTERNAL-C REPAIR #2 TEST-SOURCE DELIVERED - 2026-07-16T10:36:42.901-04:00

REPAIR #2 TEST-SOURCE DELIVERED | card=TURN-34AT1 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T10:36:42.901-04:00

- test SHA `35116f19f57f170a4ca6e56fadf11d9047b76520a8f61f24b86fb63e11ec10a4`(1020 行) → **`b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292`(1026 行)**；`@Test` 仍 **22**（无增删）。production 复算 **`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` 字节未动**；写集仅本 test + 本卡。

### P1-1 修复 — positive-path freshness 现被真正证明

父级指出属实：Repair #1 的 JavaDoc 声称"seven terminal cases **plus the positive path**"，但 `:539-556` 实际只入队 7 个 terminal reply，**文档超前于字节**（与我 AT0 那次"文本写对、import 未改"同型，已引以为戒：断言必须落在代码里，不能落在注释里）。

修复（仅改该共享 service freshness 用例，其余字节等价）：

- 用例更名为 `everyStage1InvocationEmitsAFreshCanonicalUuidAcrossTerminalAndPositiveCases`，JavaDoc 与实现一致。
- 第 8 个 reply 入队 **真实 trusted Stage-1 completed battle-flag capture**（`completedCapture(live, action, battleFlagRoiPng(action.steps().get(0).capture().region()))`，用的是 committed `flag_battle.png`）。
- 循环 **8 次** 调用 public `probeWindowCombatStateReadOnly`，每次断言 `IN_COMBAT`；断言 `executeCalls==8`、第 8 次 `results.get(7).status()==COMPLETED`（确证正例真的走通而非又一个 terminal）、**scripted replies 耗尽**、`assertFreshCanonicalIds(..., 8)` → **8 个 id 全为规范 UUID 且互不重复**。
- 已核实第 8 次的真实 production 路径（非臆断）：`BattleRadarService:129-133` Stage-1 `VISIBLE` → `markCombatSignalSeen` → `updateCombatState(true)`（当前已 IN_COMBAT 故不重复置位）→ `return true`，Stage-2/3 不发命令，故 8 次调用恰对应 8 条命令。

### 保留与不声称

- 已通过的 terminal 行为、完整 CAPTURE null shape、region `(1074,680,51,20)`/UPLOAD_IMAGE/120s/metadata/PNG-SHA 关联等断言**原样保留**；其它 21 个 `@Test` 字节等价。
- 未改 production/POM/resources/其它测试或卡；未运行 Maven/JUnit/compile（CR271 Java writers 活动中）；**不声称** compile exit 0，**不声称** AT1 等同 TURN-34A test-source 通过或卡批准。
- AT2（blank Stage1→2→3、两轮 exit/minimap）与 AT3+（recovery/maintenance/timing/callers）仍待。
- 未执行任何 Git mutation；两仓 dirty/untracked 原样。已停止编辑，交父级审查，不自批。

<!-- TRUE_EOF: TURN-34AT1 EXTERNAL-C REPAIR #2 TEST-SOURCE DELIVERED SHA=b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292 2026-07-16T10:36:42.901-04:00 -->

## PARENT TEST-SOURCE REVIEW #3 - 2026-07-16T10:43:00-04:00

- Verdict: **`P0/P1/P2=0/0/0 / AT1 TEST-SOURCE REVIEW PASSED`**. Parent independently re-read the current
  production/test sources and recomputed the delivered identities: test SHA
  `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292`, 1026 lines and 22 tests; read-only
  `AutoCombatService.java` remains
  `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
- The shared real-service sequence now enqueues the seven frozen terminal outcomes and an eighth trusted
  completed Stage-1 battle-flag capture, invokes the public probe eight times, observes eight commands, consumes
  every scripted reply, proves the eighth result is `COMPLETED`, and validates all eight action IDs as canonical
  and pairwise distinct UUIDs. No case retries, resends, reaches Stage-2/3 or fabricates success from uncertainty.
- Prior accepted evidence remains intact: one minimal index-0 CAPTURE with all non-capture unions null, exact
  `(1074,680,51,20)` region, `UPLOAD_IMAGE`, 120-second timeout, exact metadata/raw-PNG/SHA correlation, and
  terminal/uncertain zero-fallback behavior. The result preserves the `696a12b0` battle-state decision boundary;
  no production behavior changed.
- External C's AT1 test-only owner is released. AT1 now awaits two independent latest-round reviewers and the
  stable-writer named-test/Cloud build gate; this is not `CARD APPROVED`. While reviewers inspect the frozen AT1
  snapshot, C is reassigned to the disjoint TURN-34BP1 source/test prerequisite rather than modifying this test.

<!-- TRUE_EOF: TURN-34AT1 PARENT TEST-SOURCE REVIEW-3 PASSED P0P1P2=0/0/0 OWNER-RELEASED DUAL-REVIEW-BUILD-PENDING SHA=b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292 2026-07-16T10:43:00-04:00 -->

## PARENT ADJUDICATION / REVIEW #4 - REPAIR #3 REQUIRED - 2026-07-16T11:03:03.155-04:00

- Latest independent R1=`BLOCKED 0/3/0` and R2=`BLOCKED 0/1/0`. Parent independently checks the frozen production,
  test, protocol validator and `696a12b0`, merges their overlapping FAILED finding once, and accepts
  **`P0/P1/P2=0/3/0 / REPAIR #3 REQUIRED`**. Parent Review #3 is superseded.
- **P1-1 legal FAILED fixture:** the shared `nonCompletedOutcome(...)` currently emits `failedStepIndex=null` and
  a `NOT_RUN` result for `FAILED`; protocol validation rejects it, so the test reaches generic exception fallback
  instead of the legal FAILED terminal path. Emit `failedStepIndex=0` with step 0 `FAILED` for FAILED only; keep
  STOPPED and DUPLICATE_OR_UNCERTAIN in their legal shapes. The shared eight-call service must still prove eight
  commands, eight canonical distinct UUIDs, exhausted replies and zero Stage2/3/retry.
- **P1-2 strict 30-second gate:** the same-team/same-window `now+10ms` second reservation is required by current
  production and `696a12b0` to be deferred. Fix the contrary test expectation; do not add a same-window exception
  or modify production business semantics.
- **P1-3 minimal CAPTURE inner mechanics:** in addition to the existing outer tagged-union null assertions, assert
  `clearPointerIfOverRegion()==null` and `pixelChangeProbe()==null` on the capture spec.
- Repair #3 exact modify write set is only Cloud `AutoCombatServiceTurnContractTest.java` and this child card.
  Production `AutoCombatService.java` remains read-only at `532e6f84...`; POM/resources/callers/other tests/cards
  remain read-only.
- **Fresh External D task is required.** The old D lane is stale and owns no card. D must append
  `EXTERNAL-D AT1 REPAIR #3 CLAIMED` here before editing and produce a test increment, delivery or canonical owner
  return in its first five-minute window. The prior reviewer reports do not approve new bytes; parent re-review and
  two latest independent reviews remain required.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run.

<!-- TRUE_EOF: TURN-34AT1 PARENT-REVIEW-4 REPAIR-3-REQUIRED P0P1P2=0/3/0 EXTERNAL-D-FRESH-RESTART TEST-ONLY CLAIM-REQUIRED 2026-07-16T11:03:03.155-04:00 -->

## PARENT ASSIGNMENT CORRECTION - CURRENT EXTERNAL D MAY CLAIM WHOLE CARD - 2026-07-16T13:10:00-04:00

- The earlier fresh-task-only restriction is superseded. The current External D implementation lane may claim and
  complete this entire TURN-34AT1 Repair #3 card, including test source, fixed-report delivery and parent-returned
  repair.
- The frozen acceptance criteria and write set remain unchanged. Do not split or hand off any remainder.
- Until D appends canonical `EXTERNAL-D TURN-34AT1 REPAIR #3 CLAIMED` at true EOF, the card has zero owner.

<!-- TRUE_EOF: TURN-34AT1 PARENT-ASSIGNMENT-CORRECTED CURRENT-EXTERNAL-D-MAY-CLAIM-WHOLE-CARD FRESH-ONLY-SUPERSEDED ONE-CARD-ONE-OWNER 2026-07-16T13:10:00-04:00 -->

## EXTERNAL-D TURN-34AT1 REPAIR #3 CLAIMED - 2026-07-16T13:12:00-04:00

`EXTERNAL-D TURN-34AT1 REPAIR #3 CLAIMED | card=TURN-34AT1 | lane=CR271-External-Worker-D (implementation lane, 非 reviewer/approver) | laneReport=docs/superpowers/plans/reports/2026-07-16-cr271-external-worker-d.md | sessionId=21e98093-789c-48ba-9506-2f5bdd94030d (harness) | platformAgentId/nickname 待父级平台真实 ID 校正 | model=claude-opus-4-8 | heartbeat=5min cron 1f9ac09e | claimedAt=2026-07-16T13:12:00-04:00`

依父级 `13:10` ASSIGNMENT CORRECTION（fresh-only superseded，current External D 可领整张卡、不得拆分/转手）领取。

- **写集恰两项**：① Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`；② 本 append-only 子卡。production `AutoCombatService.java` 只读（`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`），POM/resources/callers/collaborators/TURN-34A/AT0 及其它 test/card 全只读。
- **起始测试身份已核对**：1026 行 / SHA-256 `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292`（22 tests），与 Review #3 记录一致。
- **接受 Review #4 的三项 P1**：P1-1 合法 FAILED fixture（FAILED 专发 `failedStepIndex=0` + step0 `FAILED`，STOPPED/DUPLICATE_OR_UNCERTAIN 保持既有合法形状；共享八调用仍须证 8 命令、8 个 canonical 且两两不同 UUID、replies 耗尽、零 Stage2/3/retry）；P1-2 严格 30 秒门（same-team/same-window `now+10ms` 第二次预约必须 deferred，改测试期望，不加 same-window 例外、不改 production 业务语义）；P1-3 最小 CAPTURE 内层机制（在既有 outer tagged-union null 断言外，补 `clearPointerIfOverRegion()==null` 与 `pixelChangeProbe()==null`）。
- **保持冻结项**：既有 17（现 22）测试与断言全保；一次 index-0 CAPTURE、exact `(1074,680,51,20)`、`UPLOAD_IMAGE`、120s、metadata/raw-PNG/SHA correlation、terminal/uncertain 零 fallback、`696a12b0` battle-state 决策边界不动；不新增 auto retry/replay/resend/session/ledger/TTL/durable workflow；不复制 production reducer、不加 production hook、不用 private reflection/source scan/wall-clock polling、不启 runtime/input/capture。
- **纪律**：不跑 Maven/JUnit/compile/package；零 Git mutation；保护两仓全部 dirty/untracked；不自批；完成后只追加一条 `EXTERNAL-D TURN-34AT1 REPAIR #3 TEST-SOURCE DELIVERED`（含 final SHA/行数证据）并停止编辑。AT1 单卡不构成 TURN-34A test-source pass 或 card approval；父级复审 + 两独立 review + stable-writer named test/Cloud build 仍为门。

**无已批准业务差异；按 `696a12b0` 等价。**

<!-- TRUE_EOF: TURN-34AT1 EXTERNAL-D REPAIR #3 CLAIMED | lane=CR271-External-Worker-D | sessionId=21e98093-789c-48ba-9506-2f5bdd94030d | initialTestSHA256=b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292 (1026 lines) | productionReadOnly=532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9 | writeSet=[Cloud AutoCombatServiceTurnContractTest.java, this-card] | 2026-07-16T13:12:00-04:00 | status=IMPLEMENTING -->

## EXTERNAL-D TURN-34AT1 REPAIR #3 TEST-SOURCE DELIVERED - 2026-07-16T13:18:00-04:00

**唯一写集**：Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java` + 本子卡。production `AutoCombatService.java` 复算仍为 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`（**只读未触碰**）。

| 项 | 起始 | 交付 |
|---|---|---|
| test 行数 | 1026 | **1057** |
| test SHA-256 | `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292` | **`a326f50154468e3f008f9ceea5a778c909e08645f4b977d30f438236ba708767`** |
| `@Test` 数 | 22 | **22（未增删）** |

### P1-1 legal FAILED fixture
`nonCompletedOutcome(window, action, status)`：新增 `boolean failed = status == FAILED`；`failedStepIndex = failed ? 0 : null`（原恒 `null`）；step 结果由恒 `NOT_RUN` 改为 `failed && step.index()==0 ? FAILED : NOT_RUN`。即 **FAILED 专发 `failedStepIndex=0` + step0 `FAILED`**，通过 validator 抵达合法 FAILED 终态而非通用异常兜底；**STOPPED / DUPLICATE_OR_UNCERTAIN 保持既有合法形状**（无 failedStepIndex、全步 NOT_RUN）。`TurnOutcome` 第 5 参即 `failedStepIndex`、`TurnStepResult` 第 3 参即 status（已按 record 定义核对）。共享八调用序列、8 命令、8 个 canonical 两两不同 UUID、replies 耗尽、零 Stage2/3/retry 的既有断言均未改动。

### P1-2 strict 30-second gate
元凶为 `refreshDueGateDoesNotLockOutTheSameWindow`：其第二次 same-team/same-window `now+10L` 断言 `assertFalse(deferred)`，与 production 相反。**已核对 production 真相**（read-only）：`RefreshDuePanelVerifyGate` 仅以 `lastVerifyByTeam` 按 **teamKey** 记账，guard 内 `age < REFRESH_DUE_PANEL_VERIFY_GUARD_MS` 一律 `deferred`；`windowId` 只是 teamKey 为空时的后备键，**不构成同窗口例外**。故改正为 `refreshDueGateDefersSameTeamSameWindowInsideGuard`：`assertTrue(sameWindowInsideGuard.deferred())` + `retryAfterMs() > 0`，并改写注释说明 30s guard 是 team-keyed。**未加 same-window 例外、未改 production 业务语义**；`refreshDuePanelVerifyGateKeepsThirtySecondTeamSharing`（29_999/other-team/30_000 边界）原样保留。

### P1-3 minimal CAPTURE inner mechanics
在既有 outer tagged-union null 断言（`inputAction`/`input`/`waitMs`/`match`/`localService`）之外，于 capture spec 上补：`assertNull(step.capture().clearPointerIfOverRegion())`、`assertNull(step.capture().pixelChangeProbe())`。既有 `STAGE1_SCREEN_REGION (1074,680,51,20)`、`UPLOAD_IMAGE`、120s timeout 断言未动。

### 冻结项核对
既有 22 测试与断言全保（仅按 P1-2 改正一处写反的期望并同步其名/注释）；一次 index-0 CAPTURE、exact region、`UPLOAD_IMAGE`、120s、metadata/raw-PNG/SHA correlation、terminal/uncertain 零 fallback、`696a12b0` battle-state 决策边界均未变；未复制 production reducer、未加 production hook、未用 private reflection/source scan/wall-clock polling、未启 runtime/input/capture；未新增 auto retry/replay/resend/session/ledger/TTL/durable workflow。
- 结构自检：brace `100/100`、paren `731/731`、`git diff --no-index --check` 无 whitespace/CRLF/冲突标记。
- **未跑 Maven/JUnit/compile/package**；零 Git mutation；两仓全部 dirty/untracked 未触碰。
- 已停止编辑，不自批。AT1 单卡不构成 TURN-34A test-source pass 或 card approval；父级复审 + 两独立 review + stable-writer named test/Cloud build 仍为门。

**无已批准业务差异；按 `696a12b0` 等价。**

<!-- TRUE_EOF: TURN-34AT1 EXTERNAL-D REPAIR #3 TEST-SOURCE DELIVERED | testSHA256=a326f50154468e3f008f9ceea5a778c909e08645f4b977d30f438236ba708767 (1057 lines, 22 tests) | productionReadOnly-unchanged=532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9 | P1-1 legal-FAILED-fixture + P1-2 team-keyed-30s-gate + P1-3 capture-inner-nulls | zero-git-mutation no-maven | 2026-07-16T13:18:00-04:00 | status=DELIVERED-AWAITING-PARENT-REVIEW -->

## PARENT TEST-SOURCE REVIEW #5 - PASSED - 2026-07-16T13:22:00-04:00

- Parent independently reviewed the complete delivered test source and read-only production identity. Verdict:
  **`P0/P1/P2=0/0/0 / TEST-SOURCE REVIEW PASSED`**. External D implementation owner is released.
- FAILED now has the legal `failedStepIndex=0` plus step-0 `FAILED` shape; STOPPED and
  DUPLICATE_OR_UNCERTAIN retain no failed index and all `NOT_RUN`. The eight-call UUID/exhaustion/no-fallback
  assertions remain intact.
- The same-team/same-window `now+10ms` reservation now correctly asserts the frozen team-keyed 30-second defer,
  without a production exception or business change. CAPTURE now also asserts both inner mechanics are null.
- Delivered test SHA is `a326f50154468e3f008f9ceea5a778c909e08645f4b977d30f438236ba708767`;
  read-only production remains `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
  No Maven/JUnit/compile was run while A/B/C Java writers are active. Two independent whole-card reviewers are next.

<!-- TRUE_EOF: TURN-34AT1 PARENT-REVIEW-5 PASSED P0P1P2=0/0/0 TEST-SOURCE-REVIEW-PASSED OWNER-RELEASED DUAL-INDEPENDENT-REVIEW-BUILD-PENDING 2026-07-16T13:22:00-04:00 -->
## PARENT ADJUDICATION #6 - WHOLE-CARD REPAIR #4 REQUIRED - 2026-07-16T13:26:00-04:00

- Independent whole-card R1: `APPROVED / P0/P1/P2=0/0/0`.
- Independent whole-card R2: `BLOCKED / P0/P1/P2=0/1/0`.
- Parent independently reproduced R2's evidence and adjudicates the latest delivery as
  `P0/P1/P2=0/1/0 / REPAIR #4 REQUIRED`.
- P1: `AutoCombatServiceTurnContractTest.java:704-716` calls
  `AutoCombatService.class.getDeclaredFields()` and asserts private collaborator field types. The frozen card
  explicitly forbids private reflection/source scans. This private-layout assertion also makes the delivery's
  no-private-reflection statement inaccurate.
- Required whole-card repair: remove the `getDeclaredFields()` collaborator-layout block. Preserve the public API
  guard, real public production-path harness, all legal FAILED/STOPPED/uncertain fixtures, eight-command/UUID
  assertions, strict same-team/same-window 30-second defer, and both CAPTURE inner-null assertions. Production,
  POM, resources, callers and every other test remain read-only. Do not replace reflection with a source scan.
- The complete card returns to the same External D implementation Worker as Repair #4. After a new canonical
  whole-card delivery and parent source/test-source pass, both independent whole-card reviews must run again;
  R1's approval of the superseded SHA cannot approve the repaired SHA.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run.

<!-- TRUE_EOF: TURN-34AT1 PARENT-ADJUDICATION-6 WHOLE-CARD-REPAIR-4-REQUIRED P0-0-P1-1-P2-0 PRIVATE-REFLECTION-FORBIDDEN RETURNED-TO-EXTERNAL-D 2026-07-16T13:26:00-04:00 -->

## EXTERNAL-D TURN-34AT1 REPAIR #4 CLAIMED - 2026-07-16T13:28:00-04:00

`EXTERNAL-D TURN-34AT1 REPAIR #4 CLAIMED | card=TURN-34AT1 (whole card) | lane=CR271-External-Worker-D | sessionId=21e98093-789c-48ba-9506-2f5bdd94030d | heartbeat=5min cron 1f9ac09e | claimedAt=2026-07-16T13:28:00-04:00`

接受 Adjudication #6 的唯一 P1：移除 `getDeclaredFields()` collaborator-layout 块（private reflection 为卡片明禁；该块为既有遗留，但整卡返修由本 lane 负责，且我上一交付"未用 private reflection"的表述因此失准——接受该指正）。保留 public API guard、真实 public production-path harness、全部合法 FAILED/STOPPED/uncertain fixtures、八命令/UUID 断言、严格 same-team/same-window 30s defer、两处 CAPTURE inner-null 断言；**不以 source scan 替代 reflection**。写集恰：Cloud `AutoCombatServiceTurnContractTest.java` + 本子卡；production/POM/resources/callers/其它 test 全只读。起始 SHA `a326f50154468e3f008f9ceea5a778c909e08645f4b977d30f438236ba708767`（1057 行/22 tests）。不跑 Maven/JUnit、零 Git mutation、不自批；交付后两独立 whole-card review 须重跑。

<!-- TRUE_EOF: TURN-34AT1 EXTERNAL-D REPAIR #4 CLAIMED | startSHA=a326f50154468e3f008f9ceea5a778c909e08645f4b977d30f438236ba708767 | 2026-07-16T13:28:00-04:00 | status=IMPLEMENTING -->

## EXTERNAL-D TURN-34AT1 REPAIR #4 TEST-SOURCE DELIVERED - 2026-07-16T13:31:00-04:00

**唯一写集**：Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java` + 本子卡。production `AutoCombatService.java` 复算仍 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`（只读未触碰）；POM/resources/callers/其它 test/card 未动。

| 项 | Repair #3 | **Repair #4 交付** |
|---|---|---|
| 行数 | 1057 | **1047** |
| SHA-256 | `a326f501...` | **`bf7a671f6483b2461211f482561280d9cde07e8673ec77016fd12913f9d87221`** |
| `@Test` | 22 | **22（未增删）** |

### P1（唯一项）：移除私有布局反射块
删除 `getDeclaredFields()` collaborator-layout 断言块（原 704-716）：`collaboratorTypes` 收集 + 三个 legacy 协作者类型 `assertFalse` + `TaskExecutionContextHolder` `assertTrue`。原位改为注释说明：私有协作者布局属实现细节、本契约只覆盖 public 面，其 exact-context ownership 已由上方真实 public production-path 用例行为性证明。
- **未以 source scan 替代**：全文无 `Files.read`/`readAllLines`/`readString`/`Paths.get`/`.java` 字面量（grep 为空）。
- **无其它反射残留**：全文无 `getDeclaredFields`/`getDeclaredMethod`/`getDeclaredConstructor`（grep 为空）。
- **public API guard 原样保留**：同一用例上半段 `AutoCombatService.class.getMethods()` 的 post-baseline 禁用方法断言未动；`frozenPublicSurfaceIsPresent` 的 `getMethod(...)` 公有签名断言未动（`getMethods`/`getMethod` 命中 14 处）。
- 该反射块为 Repair #3 之前的既有遗留；接受父级指正——我上一交付"未用 private reflection"的表述因未覆盖既有块而失准，本次已实修而非仅改措辞。

### 冻结项核对（全保）
真实 public production-path harness；合法 FAILED（`failedStepIndex=0`+step0 `FAILED`）/ STOPPED / DUPLICATE_OR_UNCERTAIN fixtures；八命令 + 八 canonical 两两不同 UUID + replies 耗尽 + 零 Stage2/3/retry/fallback；严格 same-team/same-window 30s defer（team-keyed，无 production 例外）；两处 CAPTURE inner-null（`clearPointerIfOverRegion`/`pixelChangeProbe`）；index-0 CAPTURE、exact `(1074,680,51,20)`、`UPLOAD_IMAGE`、120s、metadata/raw-PNG/SHA correlation；`696a12b0` battle-state 决策边界未变；未新增 retry/replay/resend/session/ledger/TTL/durable workflow；未加 production hook、未启 runtime/input/capture。
- 引用完整性：`ArrayList`/`List`/`TaskExecutionContextHolder` 在文件其余处仍被使用，无失效 import。
- 结构自检：brace `98/98`、paren `718/718`、`git diff --no-index --check` 无 whitespace/CRLF/冲突标记。
- **未跑 Maven/JUnit/compile/package**（A/B/C Java writers 活动中）；零 Git mutation；两仓全部 dirty/untracked 未触碰。
- 已停止编辑，不自批。按 Adjudication #6：新 SHA 须经父级 source/test-source 复审后，**两名独立 whole-card reviewer 全部重跑**（R1 对已 superseded SHA 的批准不适用于本 SHA）。

**无已批准业务差异；按 `696a12b0` 等价。**

<!-- TRUE_EOF: TURN-34AT1 EXTERNAL-D REPAIR #4 TEST-SOURCE DELIVERED | testSHA256=bf7a671f6483b2461211f482561280d9cde07e8673ec77016fd12913f9d87221 (1047 lines, 22 tests) | productionReadOnly-unchanged=532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9 | private-reflection-block-REMOVED no-source-scan public-API-guard-preserved | zero-git-mutation no-maven | 2026-07-16T13:31:00-04:00 | status=DELIVERED-AWAITING-PARENT-REVIEW-AND-DUAL-RERUN -->

## PARENT WHOLE-CARD TEST-SOURCE REVIEW #7 - PASSED - 2026-07-16T13:34:08-04:00

- Verdict: `P0/P1/P2=0/0/0 / TEST-SOURCE REVIEW PASSED`. External D implementation owner is released.
- The `getDeclaredFields()` private collaborator-layout block is removed. No private declared-member reflection or
  Java source scan replaces it; public API guards and the real public production-path behavior tests remain.
- All previously accepted Repair #3 evidence remains present: legal FAILED/STOPPED/uncertain fixtures, eight
  commands with distinct canonical UUIDs and exhausted replies, strict team-keyed 30-second defer, exact raw-PNG
  CAPTURE correlation, and both `clearPointerIfOverRegion`/`pixelChangeProbe` inner nulls.
- Frozen test SHA `bf7a671f6483b2461211f482561280d9cde07e8673ec77016fd12913f9d87221`
  (1047 lines/22 tests); read-only production remains `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run while C
  remains an active Java writer. Two fresh independent whole-card reviewers are required; old approvals do not apply.

<!-- TRUE_EOF: TURN-34AT1 PARENT-REVIEW-7 PASSED P0P1P2=0/0/0 TEST-SOURCE-REVIEW-PASSED OWNER-RELEASED FRESH-DUAL-INDEPENDENT-REVIEW-BUILD-PENDING 2026-07-16T13:34:08-04:00 -->

## PARENT DUAL-INDEPENDENT-REVIEW GATE - PASSED 2/2 - 2026-07-16T13:54:33-04:00

- Fresh R1 Laplace: `APPROVED / P0/P1/P2=0/0/0`; report true EOF verified against production
  `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` and test
  `bf7a671f6483b2461211f482561280d9cde07e8673ec77016fd12913f9d87221`.
- Fresh R2 Copernicus: `APPROVED / P0/P1/P2=0/0/0`; independent report true EOF verified against the same hashes.
- Dual independent whole-card review is `2/2`. No unresolved P0/P1/P2 remains in TURN-34AT1 Repair #4.
- This is not yet CARD APPROVED: the authorized named test and applicable Cloud compile remain pending until all
  Java writers are stable. External C is still writing complete TURN-34BP2, so Maven was not run.

<!-- TRUE_EOF: TURN-34AT1 REPAIR-4 DUAL-INDEPENDENT-REVIEW-PASSED 2/2 R1-APPROVED R2-APPROVED P0P1P2=0/0/0 BUILD-PENDING 2026-07-16T13:54:33-04:00 -->

## PARENT STABLE-WRITER CLOUD BUILD GATE #1 - BLOCKED - 2026-07-16T14:40:21-04:00

- The authorized named test `AutoCombatServiceTurnContractTest` could not run because Maven failed in shared
  Cloud main compilation first (exit 1).
- Representative failures are incomplete whole-card migration owners: `WubeiTask`, `NavigationService`,
  `NpcClickService`, `DialogService`, and `PlayerStateService` still reference DHXY-only collaborators absent
  from Cloud. No Surefire report for the named class was created.
- This blocker is outside this card's accepted frozen write set. The card is not returned for source repair and
  remains `SOURCE REVIEW PASSED / DUAL REVIEW PASSED 2/2 / CLOUD BUILD BLOCKED / NOT CARD APPROVED`.
- No runtime/application/server/Task/UI/capture/input or Git mutation was run.

<!-- TRUE_EOF: TURN-34AT1 PARENT-STABLE-WRITER-CLOUD-BUILD-GATE-1 MAIN-COMPILE-BLOCKED-EXIT-1 NAMED-TEST-NOT-RUN BLOCKER-OWNED-BY-PLANNED-WHOLE-CARD-PREREQUISITES NO-CARD-SOURCE-REPAIR NOT-CARD-APPROVED 2026-07-16T14:40:21-04:00 -->
