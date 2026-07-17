# TURN-36 FiveRing Whole-Task HTTPS Turn Readiness Preflight

## REPLACEMENT CLAIMED

- claimedAt: `2026-07-16T03:28:21.2058046-04:00`
- agentIdentity: `TURN-36 replacement readiness helper (current subordinate agent)`
- role: `non-binding readiness helper; not reviewer; no APPROVED/BLOCKED authority`
- card: `TURN-36`
- uniqueWriteSet: `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-36-readiness-preflight-helper.md`
- predecessorSession: `not_found`; no predecessor report existed at claim time
- mutationBoundary: report-only; no Java/config/plan/ACTIVE_WORK/CR/matrix/dashboard edits; no Git mutation; no Maven/JUnit/runtime/input/capture

Preflight evidence and the dispatch brief will be appended below. This claim is not an implementation claim and is not an approval decision.

## IDENTITY CORRECTION

- correctedAt: `2026-07-16T03:31:00-04:00`
- platformAgentId: `019f69d1-0b16-78d3-9a33-1b5d54e73128`
- nickname: `James`
- correctionScope: replaces only the provisional agent identity text in the claim; card, role, timestamp, unique write set, and all mutation boundaries remain unchanged

<!-- TRUE_EOF: TURN-36 readiness replacement identity corrected; agent=James id=019f69d1-0b16-78d3-9a33-1b5d54e73128 -->

## PRECHECK SCOPE AND EVIDENCE SNAPSHOT

- precheckCompletedAt: `2026-07-16T03:44:51.3009439-04:00`
- platformAgent: `James / 019f69d1-0b16-78d3-9a33-1b5d54e73128`
- businessBaseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- baselineFiveRingBlob: `f5c5022162b89953216e1787546f4a0c616e5fe0`
- observedCloudFiveRingSha256:
  `287FF0EBE4F3CECF9820A10D2FFCBF0F7AED2A26BEB7A5F510D92F540E8A4BDB`
- observedCloudFile:
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- line references below refer to that SHA-256 snapshot. Concurrent dirty/untracked content was read in place and
  preserved; no repository mutation was performed.

The precheck read the required repository instructions/context, CR271 top section, authoritative plan Sections
14-19, HTTPS turn protocol specification, the FiveRing section of `docs/业务逻辑.md`, both repository statuses,
the dependency reports, the current fixed reports at true EOF, the `696a12b0` source, and the actual Cloud/DHXY
production sources. This section is evidence and scheduling guidance only; it is not a review decision.

## 1. AUTHORITATIVE CARD SHAPE

The current authoritative row is:

`TURN-36 / PLANNED / S=13C+14+15+23+26+27+28+32+34A / FiveRing complete wiring and removal of the open-main-bag Cloud BagService caller`.

The authoritative exact write-set and test contract are:

1. Production: only
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`.
2. Any necessary DTO must be a private nested type in that same file; no separate production DTO file.
3. Named test: only
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingWholeTaskTurnContractTest.java`.
4. Required profiles: default `BC4+BASE`, plus `TASK+IMG+LS`.
5. The existing
   `FiveRingTaskTrackerTurnContractTest` remains read-only and must continue to pass in the combined cohort.
6. No DHXY production file belongs to TURN-36. Protocol, client, executor, configuration, resources, POM, lifecycle,
   Task factory, activation and runtime files are outside this card.

## 2. DEPENDENCY GATES AT THIS SNAPSHOT

| Gate | Evidence at fixed report true EOF | TURN-36 consequence |
|---|---|---|
| TURN-13C | Parent source/test-source review `0/0/0`; named test and Cloud compile are held by shared compile debt | Source API exists, but card completion still joins the later build cohort |
| TURN-14 | Parent source/test-source review `0/0/0`; named test/build pending | `CloudBagLocalServiceClient` exists, but it exposes only return-item and standalone incense operations |
| TURN-15 | Parent source/test-source review `0/0/0`; named test/build pending | Four UI-cleaner operations are available as typed local-Service calls |
| TURN-23 | Parent repair re-review `0/0/0`; build pending | Cloud first-aid/incense decisions exist; the open-main-bag compatibility method is intentionally retained until TURN-36 |
| TURN-26 | Parent source/test-source review `0/0/0`; build pending | Dialog OCR/options/white-story Cloud analysis source is available |
| TURN-27 | Readiness report only; implementation/API not delivered | Navigation production contract is not yet frozen for a TURN-36 implementation owner |
| TURN-28 | Readiness report only; implementation/API not delivered | NPC production contract is not yet frozen; its required mechanics depend on TURN-28P |
| TURN-28P | Replacement implementation claimed by Locke; no SOURCE+TEST delivery at observed true EOF | Alt+A/Alt+C, click timing and exact-HWND pixel probe are not yet a stable usable dependency |
| TURN-32 | Parent source/test-source review `0/0/0`; build pending | FiveRing tracker read is typed and Cloud-owned; direct click mechanics in the Task still remain for TURN-36 |
| TURN-33 | Replacement implementation claimed by Faraday; no SOURCE+TEST delivery at observed true EOF | TURN-34A cannot reach its final API while this predecessor is moving |
| TURN-34A | Readiness report only; implementation/API not delivered | FiveRing combat recovery dependency is not frozen |

Therefore the TURN-36 implementation claim is not dispatchable at this snapshot. The dependency row is also
missing an explicit predecessor for the open-main-bag capability described in Section 7 below. The parent must
freeze that capability and update the DAG/write sets before assigning production implementation.

## 3. REAL CALLER CHAIN

### 3.1 Currently runnable chain

The only production-runnable FiveRing chain currently present is still DHXY-local:

`MainWindowController start action`
`-> WindowTaskControlService.start(...)`
`-> WindowTaskRunner`
`-> DefaultTaskFactory.createTask(..., WUHuan_V2)`
`-> DHXY FiveRingTaskV2.execute(TaskExecutionContext)`.

Evidence:

- DHXY `MainWindowController.java:1017-1031,2424-2425` submits the selected queue to
  `WindowTaskControlService`.
- DHXY `WindowTaskRunner.java:608` calls `taskFactory.createTask(windowContext, taskType)`.
- DHXY `DefaultTaskFactory.java:41` resolves `WUHuan_V2` to the local prototype `FiveRingTaskV2`.
- A Cloud production reference search finds the Cloud `FiveRingTaskV2` class itself and its tests, but no Cloud
  production factory/construction caller.

### 3.2 Intended Cloud chain

The target production chain is:

`TURN-40D DHXY start/control`
`-> TURN-40C HTTPS activation`
`-> TURN-40B CloudTurnTaskRuntime/CloudTurnTaskFactory`
`-> TaskExecutionContext.turnNative(exact device/window/native metadata, bound TurnGameClient)`
`-> FiveRingTaskV2.execute(context)`
`-> typed Cloud Service or Task-owned explicit TurnGameClient action`
`-> one HTTPS JSON action`
`-> exact DHXY window mechanics / permanent-local Service`
`-> typed outcome plus at most one raw PNG frame`
`-> Cloud phase decision`.

TURN-36 can make the Task source turn-native and test it with a real production `TaskExecutionContext.turnNative`
plus a scripted command port. It cannot itself create the real Task factory/runtime or claim runtime reachability;
that belongs to TURN-40B/40C/40D and the final runtime gate.

## 4. QUESTMANAGERSERVICE PERMANENT-LOCAL BOUNDARY

1. The Cloud FiveRing source has zero references to `QuestManagerService`, `CloudQuestLocalServiceClient`,
   `QUEST_ACTIVATE`, or `QUEST_CAPTURE_DETAIL`.
2. `FiveRingTaskV2.java:2486-2488` calls the typed Cloud `TaskTrackerPanelService.prepareWuhuanPathingLink(...)`.
3. `TaskTrackerPanelService.java:192-225,519-550` obtains one exact-window raw PNG through the bound
   `TurnGameClient`, performs panel geometry/OCR/selection in Cloud, and returns a typed positive or same-frame
   negative result. It does not use QuestManagerService.
4. The permanent-local QuestManager boundary already exists separately:
   `CloudQuestLocalServiceClient.java:62-82` issues `QUEST_ACTIVATE`; `:93-110` issues
   `QUEST_CAPTURE_DETAIL`; DHXY `QuestLocalOperationExecutor` is the sole adapter to `QuestManagerService`.

TURN-36 must not insert Quest activation/detail calls merely to make FiveRing appear uniform. The approved TURN-32
tracker path stays Cloud-owned. Only a genuine baseline need for task-panel activation or detail capture may use the
existing typed Quest client; direct Cloud construction/import of local QuestManagerService remains forbidden.

## 5. `696a12b0` WHOLE-TASK EQUIVALENCE MAP

The implementation and named test must keep these baseline decisions, order and counts. Moving a step to HTTPS does
not authorize changing it.

| Baseline area | Required equivalent behavior |
|---|---|
| Entry | Startup gate first; then configured run loop; stop maps to STOPPED, unrecoverable phase maps to FAILED |
| Phase machine | Preserve `PREPARE -> BUY_SHOES/HANDOVER_DETECT -> ACCEPT_TASK -> WAIT_PATHING -> HANDLE_DIALOG -> SYNC_TASK_PANEL -> terminal` transitions and existing outside-turn/yield points |
| PREPARE order | `UI clean -> startup first aid -> one-main-bag supply check -> quick-buy fallback if short -> rescan -> HANDOVER_DETECT` |
| Required shoes | Unlimited mode requires one; bounded mode uses remaining configured rounds, clamped to one or two |
| One-bag supply check | In one opened main-bag exclusive section: incense decision/use first, then count shoe template up to required count, returning first page plus count, then one close |
| Quick-buy | Anchor probe; if absent click reveal and re-probe; ordered fast-item templates; right click; shoe template; one/two selection clicks; buy click; UI close; then bag rescan |
| Shop-owner fallback | Preserve exact-entry navigation, watcher/door handling, up to three owner-buy attempts, existing dialog option interpretation, template fallback, return verification and fallback navigation |
| Door/dismount | Auto-enter check; one Alt+C; recheck; flying-state check; only confirmed FLYING gets the second Alt+C; NOT_FLYING/UNKNOWN do not |
| Return from shop | Before ROI capture; each of up to three right-click business attempts checks visual change first and map confirmation second; final navigation fallback remains |
| Handover/tracker | Prepared action priority, typed live tracker read, exact window/HWND/freshness fence, click result and phase/park behavior remain as TURN-32 fixed them |
| Accept/dialog | Existing accept attempt count, already-has-task handling, accept option/template order, story handling and cleanup order remain |
| Give shoe | Existing `DialogService.handleDialog(giveItemIfAvailable(...))` result mapping and `shoeBagIndex` behavior remain; no second ad-hoc GiveItem flow |
| Combat | Existing incidental-combat observation/recovery order remains and delegates to final TURN-34A typed API |
| Completion | Distinguish daily terminal story from one-run-complete story exactly as `docs/业务逻辑.md:1077-1079`; configured one/two/unlimited behavior remains |
| Retry meaning | Existing bounded business attempts remain Cloud phase logic. No command timeout, uncertainty or transport failure may create an automatic resend |

`docs/业务逻辑.md:1058-1060,1084-1085` also fixes route-dialog ownership: `ROUTE_TRANSFER` remains consumed by
NavigationService. TURN-36 must not move that decision into FiveRing.

**无已批准业务差异；目标必须按 `696a12b0` 等价迁移。**

## 6. CURRENT TASK-LOCAL MECHANICS THAT TURN-36 MUST REMOVE

The observed Cloud Task is not yet a thin Cloud decision owner. The following production-reachable local mechanics
remain in `FiveRingTaskV2.java`:

| Current source evidence | Required replacement boundary |
|---|---|
| `:205,840,1110-1118` direct `BagService`, `findItemPageIndex`, `withMainBagOpen`, `MainBagSession` | Closed typed Bag boundary; no Cloud BagService instance |
| `:1020,1034` direct `InputSequences.pressAltC` | Exact-HWND background `KEY_TAP Alt+C` after TURN-28P |
| `:1146-1151,1179-1184,1287-1292,1414-1421,1451-1456,1480-1485` direct mouse queue | Ordered JSON INPUT steps through the bound TurnGameClient; move/click/waits remain atomic per baseline sequence |
| `:1332-1377` direct tracker capture and local visual comparison | Exact ROI raw PNG turn; decode and compare in Cloud |
| `:1395-1459` local file/template search and click | Packaged Cloud template load plus Cloud in-memory match; a separate explicit input action only after a Cloud hit |
| `:1463-1477` tracker refresh/base coordinate lookup and randomized point | Latest exact `TurnWindowMetadata.windowRect.left/top`; unscaled screen-absolute coordinates; no title search/refresh |
| `:818,983,1009,1024,1031,1038,1306,1820,1834,2047,2546,2601` direct `GameStateUtil` | Final typed Service/turn observation or Task-private Cloud calculation; no missing DHXY utility instance |
| `:2565-2570` direct tracker prepared click | One bound TurnGameClient input action; preserve exact positive-action fence and movement intent/phase result |

The file also imports or declares Cloud-missing DHXY-only types. At the snapshot, these source paths do not exist in
the Cloud repository: `GameClientTracker`, `TextRecognizer`, `CoordinateHelper`, `GameStateUtil`,
`WindowScopedTempPath`, and `OcrWindowScanService`. `BagService` is also absent. TURN-36 must remove its direct
imports/fields/calls rather than copy those classes into Cloud. Unused `TextRecognizer`, `WindowScopedTempPath`,
`OcrWindowScanService`, OCR DTO and other dead imports should be removed within the one-file edit.

TURN-38 later removes old retained task authority. TURN-36 must not opportunistically rewrite
`TaskTransactionRunner`, ready-event, pathing state, pause/stop or lifecycle ownership beyond what is necessary to
route mechanics through the already frozen turn-native APIs.

## 7. MATERIAL OPEN-MAIN-BAG CAPABILITY GAP

### 7.1 Evidence

The plan says TURN-36 will close the open-main-bag boundary, but the current dependency APIs cannot represent it:

1. Baseline and current Task `:1108-1119` open the main bag once, call
   `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag(mainBag, context)`, then call
   `mainBag.countItemUpTo(KEY_ITEM_NAME, requiredShoeCount)` before one close.
2. Current `PlayerStateService.java:556-565` deliberately retains that exact caller-owned session behavior until
   TURN-36.
3. The closed protocol has only `BAG_RETURN_ITEM` and `BAG_USE_INCENSE`. `TurnBagOperationArguments` has return-item
   intent/template/page/cache/source fields only.
4. `CloudBagLocalServiceClient` exposes only `executeReturnItem(...)` and `executeUseIncense(...)`. Its strict result
   shape can return return-item state/cache point or incense `USED/NOT_FOUND`; it cannot return shoe count or first
   shoe page.
5. Standalone `BAG_USE_INCENSE` opens/closes its own bag macro. Calling it and then a second scan would change the
   baseline one-open sequence and is not an equivalent substitute.
6. Even after FiveRing stops calling the compatibility method, the method signature in `PlayerStateService` still
   references the Cloud-missing `BagService.MainBagSession`; Java compilation still requires that type. The current
   one-file TURN-36 write set cannot remove that stale compatibility surface or its TURN-23 source test.
7. A stateless synchronous turn cannot keep an open local Bag session across a Cloud round trip while Cloud examines
   an image and decides whether to use incense. Adding an owner/session/permit/durable workflow to simulate that is
   explicitly forbidden.

### 7.2 Parent freeze required before dispatch

The parent must create/freeze a separate prerequisite or formally amend serial write sets. It must resolve both the
wire capability and the stale PlayerState compatibility API. One possible contract shape for discussion is a closed
Bag operation such as `BAG_FIVE_RING_SUPPLY_CHECK` that receives only explicit Cloud-decided mechanics
(`useIncense`, canonical shoe template, required count, source), opens the main bag once, conditionally uses incense,
counts shoes, closes once, and returns strict JSON with `incenseUsed`, `shoeCount`, and nullable `firstPageIndex`.

That name/shape is a suggestion, not an implementation decision. It still exposes a sequencing question: Cloud must
obtain the incense status decision before invoking the one-bag operation, whereas `696a12b0` probes status from
inside the already-open callback. If the exact open-before-probe order is mandatory, the stateless turn model cannot
both keep Cloud decision ownership and retain the local open session. The parent must present the concrete options
and runtime consequences to the user; neither moving OCR/business into BagService nor silently using two bag-open
cycles is authorized by this precheck.

A prerequisite will likely need a parent-frozen write set spanning both protocol copies/validators, DHXY
`BagLocalOperationExecutor`, Cloud `CloudBagLocalServiceClient`, the deferred `PlayerStateService` API and focused
contract/golden tests. None of those files may be added casually to TURN-36.

## 8. PROPOSED TURN-NATIVE ACTION DESIGN AFTER ALL GATES ARE FROZEN

1. Obtain the exact bound client only from `TaskExecutionContext.getTurnGameClient()`; do not inject/create a second
   client or exchange.
2. Before creating any action/UUID, compare the Task's initial device/window/HWND/process with latest accepted
   metadata. A rebind mismatch produces zero command and zero UUID.
3. Derive every ROI/click from actual `windowRect.left/top`. Baseline window-relative coordinates become
   `left + relativeX`, `top + relativeY`. Do not resize images or scale coordinates.
4. Service-owned behavior stays behind the final typed Service APIs: UI cleaner, PlayerState first aid/incense
   decision, Navigation, NpcClick, Dialog/GiveItem, TaskTracker and AutoCombat. FiveRing must not reconstruct those
   algorithms.
5. Task-specific shoe UI observations use one explicit CAPTURE action per baseline observation. Validate correlation,
   decode the raw PNG in Cloud, load canonical packaged templates through the existing Cloud template asset owner,
   and perform the match in Cloud memory.
6. A Cloud hit may then produce one explicit ordered INPUT action. Keep the actual decision visible in the business
   method; use at most one non-nested private invocation/result boundary rather than a wrapper chain.
7. Preserve each baseline atomic sequence. Examples include
   `MOVE -> WAIT(100/120) -> CLICK_LEFT/RIGHT(with exact click delay) -> queue hold`, and the one/two shoe selection
   clicks with their exact inter-click waits. TURN-28P timing fields, once delivered, must keep click delay and queue
   hold inside the same queue submission.
8. Alt+C is one exact-HWND background `KEY_TAP` action. The second Alt+C remains conditional on Cloud-proven FLYING.
9. A baseline re-probe, bounded shop attempt or fallback is a new explicit business invocation with a new UUID. It is
   not a transport retry. Timeout, duplicate/uncertain or malformed correlation is never resent automatically.
10. Each action returns at most one frame. Captures and verification use raw `image/png` multipart bytes, never Base64.
11. No local template/OCR/business decision is added. The optional local MATCH capability remains available to the
    platform but is not required to replace Cloud-owned FiveRing decisions.

## 9. TERMINAL AND RESULT MAPPING

| Command/outcome state | Required Task treatment |
|---|---|
| Command not `COMPLETED` | UNKNOWN/uncertain; no fabricated action success and no resend |
| Outcome `DUPLICATE_OR_UNCERTAIN` | UNKNOWN/fatal to the current explicit invocation; no phase success |
| Outcome `STOPPED` | Propagate Task stop after the existing checkpoint boundary |
| Outcome `FAILED` with exact failed step | Mechanical failure; stop remaining steps; consume failure evidence only when contract-valid and let the existing business phase decide its already-approved bounded fallback |
| Outcome `COMPLETED` | Require exact action/device/window/native metadata, exact ordered step correlation, all expected step statuses and exact frame contract before deriving business truth |
| Missing/wrong frame, PNG, SHA, ROI, dimensions, source step or action id | Contract failure; never ordinary template miss, unchanged pixels or successful click |
| Local-Service completed JSON | Strictly parse the exact operation result; unknown/duplicate/missing/trailing/coerced fields fail closed and cause no second command |

## 10. NAMED-TEST ACCEPTANCE MATRIX

The single `FiveRingWholeTaskTurnContractTest` should use production `FiveRingTaskV2`, a turn-native context, the
production action/client boundary and fake in-memory mechanics/command responses. It must inspect serialized JSON
actions as well as typed results; object-only assertions are insufficient for the user-requested wire contract.

| Case family | Minimum assertions |
|---|---|
| Exact binding | Correct device/window/HWND/process reaches the port; wrong latest HWND or process rejects before UUID/action/command |
| JSON action shape | Contract version, UUID actionId, exact device/window, contiguous step indexes, explicit step order, unscaled coordinates, failure-evidence flag and absence of session/ledger/TTL/retry fields |
| Raw PNG | Real PNG signature, metadata/content type/SHA/dimensions/ROI/source step exact; one frame maximum; no Base64 |
| PREPARE order | UI clean before first aid; first aid before supply boundary; supply result drives quick-buy or handover exactly |
| Supply local result | Strict successful count/page/incense mapping plus malformed JSON, FAILED, STOPPED and uncertain cases; no second command. This family waits for the parent-frozen prerequisite contract |
| Quick-buy anchor hit | One capture UUID; Cloud match; no reveal click; next expected action only |
| Quick-buy anchor miss | Capture -> one reveal input action -> one fresh re-probe; three distinct explicit invocation UUIDs; no automatic resend |
| Fast-item templates | Cloud evaluates canonical templates in baseline order; first hit sends one right-click action; all miss sends no click |
| Shoe selection | Required count one/two produces exact number/order of clicks and waits inside one action; invalid count follows baseline clamp |
| Buy button | Template hit uses matched absolute point; miss uses the exact unscaled fallback point; no local matcher decision |
| Shop-owner fallback | Up to three baseline business attempts; NpcClick -> buy option -> shoe -> buy -> close order; each explicit action has one UUID; transport uncertainty ends the invocation rather than consuming another attempt automatically |
| Door/dismount | Auto-enter success sends zero Alt+C; first miss sends one; second only after FLYING; NOT_FLYING/UNKNOWN sends no second Alt+C |
| Return verification | Before capture, right click, fresh after capture, Cloud pixel comparison threshold `0.35`, map confirmation second, bounded three attempts and final navigation fallback |
| Tracker read/click | TURN-32 positive/negative mapping remains; prepared/live positive must match task/window/HWND/source/freshness; click is one ordered turn action; stale/mismatch sends zero input |
| Quest boundary | Whole FiveRing path sends zero `QUEST_ACTIVATE/QUEST_CAPTURE_DETAIL` unless a separately frozen baseline case explicitly needs it |
| Dialog/GiveItem | Accept/already-task/story/give statuses preserve phase transitions and shoe page; typed Give path only, no duplicate local Give flow |
| Completion stories | Daily-terminal story stops the entire task; one-run story obeys configured one/two/unlimited policy |
| STOP/pause | Stop before each command produces zero new UUID/command; STOPPED is not FAILED/SUCCESS; pause uses existing checkpoint semantics and adds no durable state |
| Static active-path gate | No direct `BagService`, `InputSequences`, `GameClientTracker`, `CoordinateHelper`, `GameStateUtil`, local OCR/template path, old remote port, Base64, session, ledger, TTL or automatic retry reference in the production-reachable turn path |

The final parent gate must also rerun the already authorized foundation/protocol families and the existing
`FiveRingTaskTrackerTurnContractTest`, then the applicable Cloud compile/build after writers stabilize. This helper
did not run Maven/JUnit/compile.

## 11. FILE-CONFLICT AND SCHEDULING MAP

- No active implementation observed here owns `FiveRingTaskV2.java`; TURN-32's source owner was released. A future
  TURN-36 edit is intentionally downstream of TURN-32, so both tracker and whole-task named tests must run together.
- TURN-28P does not overlap the TURN-36 file, but it changes the exact input/capture contract that TURN-36 must use.
  Starting TURN-36 against its intermediate API would invite rewrite and conflicting assumptions.
- TURN-33 does not overlap the TURN-36 file, but final TURN-34A depends on it and TURN-36 depends on final TURN-34A.
- TURN-27, TURN-28 and TURN-34A readiness helpers are evidence only; their proposed APIs are not implementation
  contracts and must not be guessed by the TURN-36 owner.
- The proposed open-main-bag prerequisite necessarily overlaps protocol/Bag client/PlayerState areas outside
  TURN-36. The parent must serialize those writes with the owners/build cohort and release them before TURN-36.
- The Cloud Task file currently contributes to shared compile debt through missing DHXY-only imports. Copying those
  types to make an intermediate compile pass is forbidden; downstream cutover must remove the imports.

## 12. EXECUTABLE DISPATCH BRIEF FOR THE PARENT

Do not issue an implementation claim until all of the following are true:

1. TURN-27, TURN-28 and TURN-34A have final parent-frozen public APIs and passed source/test-source review.
2. TURN-28P has delivered the exact background key/click/pixel mechanics required by those APIs.
3. The parent has resolved the open-main-bag capability, updated the DAG/exact write sets, and recorded any user-
   approved sequencing difference. With no approved difference, the implementation must remain `696a12b0`-equivalent.
4. The stale `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag(BagService.MainBagSession,...)` compile surface
   has a serial owner and removal/compatibility test plan after its final caller is cut over.
5. No other writer owns the sole production or named-test file.

Then dispatch one implementation Worker with this exact brief:

- Modify only Cloud `FiveRingTaskV2.java`, create only
  `FiveRingWholeTaskTurnContractTest.java`, and append only the parent's fixed TURN-36 implementation report.
- Preserve every Section 5 business decision, phase, count, wait, bounded fallback and terminal distinction.
- Reuse final typed Cloud Service APIs. Use only the context-bound TurnGameClient for Task-owned mechanics.
- Use raw PNG, exact metadata, actual left/top and unscaled coordinates; one action/UUID/command and one frame maximum.
- Remove direct Cloud local-Service/mechanics instances and missing DHXY-only imports. Do not copy local runtime classes.
- Do not add automatic retry/session/owner/permit/ledger/TTL/compaction/durable workflow or another transport/client.
- Deliver production/test SHA-256, exact line evidence, serialized JSON/result matrix and true EOF. Do not claim review
  authority and do not run runtime/UI/capture/input.

## PRECHECK_COMPLETE

TURN-36's caller chain, dependency gates, exact write set, permanent-local QuestManager boundary, `696a12b0`
equivalence points, residual mechanics, terminal contract, named-test matrix and dispatch brief are now documented.
The principal unresolved item is not code complexity inside the FiveRing file; it is the missing stateless typed
representation of the baseline one-open-main-bag incense-plus-shoe-count boundary and its stale PlayerState API.
That capability must be frozen by the parent before implementation is assigned.

<!-- TRUE_EOF: TURN-36 readiness PRECHECK_COMPLETE agent=James id=019f69d1-0b16-78d3-9a33-1b5d54e73128 -->
