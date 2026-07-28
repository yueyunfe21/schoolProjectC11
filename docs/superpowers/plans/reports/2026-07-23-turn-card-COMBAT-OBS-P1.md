# COMBAT-OBS-P1 - Local Combat-Entry Mechanics And On-Demand Exit Fallback

## Status

- `READY / ZERO OWNER`
- Parent: `CR226`
- Client authority: `D:\mavenProject\DHXY-cr271`
- Cloud authority: `D:\mavenProject\dhxy-cloud-brain`
- Read-only business baseline: `D:\mavenProject\DHXY`
- Baseline commit: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`

## User-Approved Boundary

Move only the mechanical image recognition for combat entry into the existing per-window local
observation runner. Do not move the Cloud battle state machine, task phases, hysteresis, combat-exit
ownership, wakeup policy, retries, fallbacks, input, OCR, or business decisions.

The existing local `FastExpectedCombatExitProbe` remains byte-for-byte behaviorally unchanged.
The fourth `coordinate-strip` ROI remains Cloud-owned. It is shared by the already-approved typed
pathing observer and the conservative combat-exit fallback, but it must not be uploaded continuously:
Cloud requests it only while an exact pathing intent is active or while the existing combat-exit
fallback needs minimap readability after the frozen consecutive combat-signal misses.

## Frozen Semantics

1. Local entry mechanics use the baseline regions, templates, policies, thresholds and short-circuit order:
   - `combat-flag`: `(974,630 51x20)`, `flag_battle.png`, `ANY`, `0.85`;
   - `combat-selection`: `(927,302 100x225)`, `zhaohuan.png` / `chehui.png`, `ANY`, `0.8`;
   - `combat-top`: `(456,62 123x39)`, `nu.png` / `yuan.png`, `ALL`, `0.8`.
2. Local output is the existing business-free `ObservationFactType.COMBAT_SIGNAL`; no new protocol,
   store, state machine, event bus, runner, queue, lease or command action.
3. Cloud remains the sole owner of `BattleRuntimeState`, enter/exit transition decisions, consecutive
   miss counting, pending edges and task wakeups.
4. A local negative sample is not `COMBAT_EXITED`. It only means that the three positive combat
   templates were not visible in that sample.
5. The existing local fast-exit `20x20`, 15-second delay, one-second sample, `0.35` diff and exact
   wait/generation gate are frozen.
6. `coordinate-strip` recognition, OCR fallback, typed pathing semantics and conservative exit meaning
   remain Cloud-owned. Only its sampling schedule changes from always-on to dynamic demand:
   exact active pathing or exact combat-exit fallback.
7. No runtime/UI/live capture/input during implementation or source review.

## Required Data Flow

```text
local runner
  -> in-memory exact-HWND ROI capture
  -> baseline-equivalent template short circuit over ROI 1/2/3
  -> latest-wins COMBAT_SIGNAL fact
  -> Cloud consumes the exact-run fact in the existing BattleRuntimeState monitor
  -> coordinate-strip interest is present while an exact pathing intent is active
  -> independently, only after frozen misses while Cloud remembers IN_COMBAT:
       publish/request coordinate-strip interest
       consume a fresh exact-revision coordinate-strip
       apply existing Cloud minimap-readable exit fallback
```

No PNG/Base64 upload is permitted for `combat-flag`, `combat-selection` or `combat-top` after this
cutover. The coordinate strip must be absent unless exact pathing or combat-exit fallback needs it.

## Acceptance

- Shared observation DTOs remain byte-identical in Client and Cloud.
- Client production test proves the three baseline template branches, `ANY/ALL`, thresholds,
  short-circuit capture count and one latest-wins `COMBAT_SIGNAL` fact.
- Cloud production test proves local positive fact enters through the existing monitor and negative
  facts preserve the frozen miss/hysteresis behavior.
- Cloud production test proves `coordinate-strip` is not in the initial interest set, is requested
  only for exact active pathing or after the existing miss threshold while remembered `IN_COMBAT`,
  and a stale/old-revision strip cannot advance pathing or confirm exit.
- Existing FastExpectedCombatExit tests remain unchanged and pass.
- Existing BattleRadar and TURN-40G observation named families pass; both repositories compile.
- Source scan proves no continuous ROI publication for keys `combat-flag`, `combat-selection`,
  `combat-top` or `coordinate-strip`.

<!-- TRUE_EOF: COMBAT-OBS-P1 READY ZERO-OWNER USER-APPROVED-NARROW-BOUNDARY 2026-07-23 -->

## CANONICAL WHOLE-CARD CLAIM - 2026-07-23T09:47:43-04:00 - CODEX

- state: `SOURCE_ACTIVE / WHOLE-CARD OWNER=CODEX`
- scope: exact `COMBAT-OBS-P1` Client + Cloud source/test write set only.
- baseline: read-only `D:\mavenProject\DHXY` at
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; dirty/untracked state preserved.
- business contract checked: `docs/业务逻辑.md` “Expected 战斗快脱战与回程验证兜底”
  and “修罗与五倍普通怪共用：入战识别、云端 fallback 与失败上限”.
- behavior: `无已批准业务差异；按基线等价迁移`.
- safety: no Git mutation, runtime/UI/live capture/input; existing dirty work in both writable repositories
  will be preserved.

<!-- TRUE_EOF: COMBAT-OBS-P1 CANONICAL-WHOLE-CARD-CLAIM OWNER-CODEX SOURCE-ACTIVE 2026-07-23T09:47:43-04:00 -->

## PARENT PRE-REVIEW ACK - 2026-07-23T09:50:00-04:00 - CODEX

- ack: local `COMBAT_SIGNAL` will preserve `VISIBLE / ABSENT / UNAVAILABLE`; ROI capture or template-load
  failure cannot become `ABSENT`, and Cloud cannot count it as a combat miss.
- fact shape: one latest-wins fact carries the complete short-circuit outcome/final signal for that sample;
  Cloud interprets it only inside the existing `BattleRuntimeState` monitor.
- coordinate fence: on-demand `coordinate-strip` interest is revision-bound and accepts only a fresh frame
  produced for that revision; prior continuous/stale frames cannot confirm exit.

<!-- TRUE_EOF: COMBAT-OBS-P1 PARENT-PRE-REVIEW-ACK TRI-STATE-FACT REVISION-FRESH-COORDINATE-FENCE 2026-07-23T09:50:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR - 2026-07-23T10:02:00-04:00 - CODEX

- Worker correctly found that `coordinate-strip` is shared infrastructure: TURN-40G
  `probeTypedPathing()` consumes it for `ARRIVED / STOPPED_AWAY`; fallback-only publication would
  regress navigation and violate the closed TURN-40G contract.
- Parent resolution is the no-business-difference dynamic union:
  `coordinateStripRequired = exactActivePathing || combatExitFallbackRequired`.
- This does not move pathing or exit recognition to Client and does not reopen their semantics.
  It only stops the ROI outside both existing consumers.
- Card returns to `SOURCE_ACTIVE / WHOLE-CARD OWNER=CODEX`; all other boundaries remain frozen.

<!-- TRUE_EOF: COMBAT-OBS-P1 PLAN-CONTRACT-REPAIRED ACTIVE-PATHING-OR-COMBAT-EXIT-FALLBACK COORDINATE-INTEREST SOURCE-ACTIVE OWNER-CODEX 2026-07-23T10:02:00-04:00 -->

## PLAN-CONTRACT BLOCKED - 2026-07-23T09:53:27-04:00 - CODEX

- state: `PLAN-CONTRACT BLOCKED / OWNER RETAINED / ZERO SOURCE DELTA`.
- conflict: the card freezes `coordinate-strip` to combat-exit fallback demand only, but current approved
  TURN-40G Stage 6 source still uses the same ROI as the only position input for typed pathing classification:
  `CloudWholeTaskObserver.probeTypedPathing()` calls `positionProbe.read(...)`, whose production implementation
  is `readPositionFromObservation()` over `coordinate-strip`.
- transitive evidence: Client has no production caller of
  `WindowRuntimeContext.updatePathingSnapshot(...)`; its typed pathing fact mirrors the existing snapshot and
  cannot replace the Cloud coordinate recognition. Removing the initial/continuous coordinate interest therefore
  makes `ARRIVED / STOPPED_AWAY` unreachable and regresses the Review #27-approved TURN-40G pathing chain.
- acceptance collision: the card simultaneously requires `coordinate-strip` absent outside combat fallback and
  requires the existing TURN-40G observation family to pass; current production/test contract needs that ROI for
  active pathing.
- source safety: the unclosed Client/Cloud implementation draft was removed. Target Java files have zero source
  diff from the pre-claim worktree; only this card claim/ack/blocker documentation changed.
- unique parent decision required:
  1. amend this card so `coordinate-strip` is dynamically requested for either active typed pathing or combat-exit
     fallback (demand union, with revision/fresh fences); or
  2. first authorize a separate local position/pathing-fact migration that removes Cloud pathing's dependence on
     the coordinate ROI, then resume this card's fallback-only rule.
- no approved business difference was implemented; no tests/compile were run because source was restored before
  the contract choice.

<!-- TRUE_EOF: COMBAT-OBS-P1 PLAN-CONTRACT-BLOCKED OWNER-CODEX ZERO-SOURCE-DELTA COORDINATE-ROI-PATHING-COLLISION PARENT-DECISION-REQUIRED 2026-07-23T09:53:27-04:00 -->

## PLAN-CONTRACT REPAIR ACK / SOURCE RESUMED - 2026-07-23T10:03:00-04:00 - CODEX

- state: `SOURCE_ACTIVE / WHOLE-CARD OWNER=CODEX`.
- ack: `coordinateStripRequired = exactActivePathing || combatExitFallbackRequired`.
- frozen remainder: local tri-state combat mechanics only; Cloud retains state/miss/phase/wakeup/exit ownership;
  existing FastExpectedCombatExit behavior unchanged; exact revision/fresh fences mandatory.

<!-- TRUE_EOF: COMBAT-OBS-P1 PLAN-CONTRACT-REPAIR-ACK SOURCE-ACTIVE OWNER-CODEX DEMAND-UNION-FROZEN 2026-07-23T10:03:00-04:00 -->

## CANONICAL WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-23T10:19:13-04:00 - CODEX

- state: `WHOLE-CARD SOURCE+TEST DELIVERED / AWAITING PARENT SOURCE+TEST SOURCE REVIEW / OWNER RETAINED`.
- business result:
  - Client performs only the frozen baseline mechanical checks for combat flag, selection and top ROIs, with the
    original ROI/threshold/ANY-ALL/template order unchanged.
  - one latest-wins `COMBAT_SIGNAL` fact reports `VISIBLE`, `ABSENT` or `UNAVAILABLE`; capture/template/matcher
    failure is fail-closed as `UNAVAILABLE` and cannot increment a Cloud combat miss.
  - templates are cached per observation run and released by sampler `reset()` / runner stop.
  - Cloud remains the sole owner of `BattleRuntimeState`, consecutive misses, hysteresis, phase and wakeup.
  - `coordinate-strip` is dynamically present only for
    `exactActivePathing || combatExitFallbackRequired`; exact revision, sequence and capture-time fences reject
    stale frames. Reusing one `ABSENT` while waiting for a fresh coordinate does not increment misses twice.
  - existing `FastExpectedCombatExitProbe` behavior is unchanged.
- exact Client write set:
  - `src/main/java/com/bot/dhxy/window/observation/LocalCombatSignalMechanics.java`
    SHA-256 `44822b12577a1d434b6e15de04aae1f149ecd6bb77745bccfb8ebca613cfc5d6`;
  - `src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java`
    SHA-256 `3ae70c4a938c28caf51a0bb0039172b13f0bbcd0c6be03399f9e974a62324cf9`;
  - `src/test/java/com/bot/dhxy/window/observation/LocalCombatSignalMechanicsTest.java`
    SHA-256 `c112337b6662854908f7076e9b441be3bb538a6b7d39b22587c2d0c996d6399b`.
- exact Cloud write set:
  - `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
    SHA-256 `af52b92a9bca1b9be46b00d99543d07ed4cf876e620bcedc4fa9a852f3a703a3`;
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
    SHA-256 `f716bad5bce2007b3d54f4c06d106b3e42d9817a69183f94a21bd47a5a6593d0`;
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/BattleRadarTurnContractTest.java`
    SHA-256 `8a060799b0f5d3ebab89a020b17b6469ac8a59a9dcaf0736eee231f8402744bc`;
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverProductionHarnessTest.java`
    SHA-256 `0dd951bcbf91e256fe3fdc74199a4235e397a32350843ea1b481af2ffc1226d4`.
- validation:
  - Client:
    `mvn -q "-Dtest=LocalCombatSignalMechanicsTest,WindowObservationRunnerContractTest,WindowObservationKandaContractTest,FastExpectedCombatExitProbeTest" test`
    -> `29/29`, failures `0`, errors `0`;
  - Cloud:
    `mvn -q -DskipTests=false "-Dtest=BattleRadarTurnContractTest,CloudWholeTaskObserverProductionHarnessTest,CloudObservationContractTest,FastExpectedExitObservationContractTest" test`
    -> `49/49`, failures `0`, errors `0`;
  - Client `mvn -q -DskipTests compile` -> exit `0`;
  - Cloud `mvn -q -DskipTests=false compile` -> exit `0`;
  - shared observation protocol DTOs -> `16/16` filename/SHA byte-identical across Client and Cloud;
  - exact write-set `git diff --check` -> exit `0`;
  - source scan: no continuous publication of `combat-flag`, `combat-selection` or `combat-top`; the sole
    `coordinate-strip` publisher is the frozen dynamic demand union.
- safety: no runtime/UI/live capture/input, no Git mutation, no branch/commit, and
  `D:\mavenProject\DHXY` remained strictly read-only. All unrelated dirty/untracked work was preserved.

<!-- TRUE_EOF: COMBAT-OBS-P1 CANONICAL-WHOLE-CARD-SOURCE-TEST-DELIVERED OWNER-CODEX AWAITING-PARENT-SOURCE-TEST-REVIEW CLIENT-29OF29 CLOUD-49OF49 DUAL-COMPILE DTO-16OF16 DIFF-CHECK-ZERO 2026-07-23T10:19:13-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - REPAIR REQUIRED - 2026-07-23T10:27:00-04:00

- verdict: `P0=0 / P1=0 / P2=3`; source behavior and tests are otherwise accepted, owner retained.
- `P2-1` Client `WindowObservationSampler` class JavaDoc says the sampler never produces input, but the same
  class owns the previously approved local-kanda atomic input exception in `sampleXiuluoLocalKanda(...)`.
  Repair the comment to name that exact exception; do not change behavior.
- `P2-2` Cloud `BattleRadarService.OBSERVATION_ROIS` JavaDoc still says all four entries are published as
  observation interests. After this card only `coordinate-strip` may be dynamically published; the three combat
  entries are local mechanics/reference geometry. Repair the comment only.
- `P2-3` Cloud `CloudWholeTaskObserver.runNoTurnCombatProbe(...)` still describes observation-ROI combat
  detection. It now consumes a geometry-free `COMBAT_SIGNAL` fact and optional dynamic coordinate fallback.
  Repair the comment only.
- accepted evidence retained: exact delivery SHA matched all seven files; tri-state failure is `UNAVAILABLE`;
  Cloud owns state/miss/hysteresis/wakeup; dynamic coordinate interest is the exact
  `activePathing || combatExitFallback` union; fresh-coordinate reuse passes `newSignalSample=false`.
- repair gate: comment-only delta, then rerun the same named Client/Cloud families, dual compile, DTO byte
  identity and exact write-set `git diff --check`; re-deliver at physical EOF.

<!-- TRUE_EOF: COMBAT-OBS-P1 PARENT-REVIEW-1 REPAIR-REQUIRED P0-0 P1-0 P2-3 COMMENT-ACCURACY-ONLY OWNER-CODEX 2026-07-23T10:27:00-04:00 -->

## REVIEW #1 COMMENT-ONLY REPAIR COMPLETE / WHOLE-CARD RE-DELIVERED - 2026-07-23T10:24:40-04:00 - CODEX

- state: `WHOLE-CARD SOURCE+TEST RE-DELIVERED / AWAITING PARENT REVIEW #2 / OWNER RETAINED`.
- Review #1 repairs, JavaDoc only:
  - Client `WindowObservationSampler` class JavaDoc now names the pre-existing, separately gated
    `sampleXiuluoLocalKanda(...)` atomic click as its sole input-producing exception.
  - Cloud `BattleRadarService.OBSERVATION_ROIS` JavaDoc now distinguishes the three Client-local combat
    mechanics reference geometries from the sole dynamically published `coordinate-strip`.
  - Cloud `CloudWholeTaskObserver.runNoTurnCombatProbe(...)` JavaDoc now describes the geometry-free
    `COMBAT_SIGNAL` fact and optional dynamic coordinate fallback.
- repaired file SHA-256:
  - `src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java`
    `5c4e77405de4242885754fe432445fc71c943b5bfb170f4f0c4e341987fcbcf9`;
  - `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
    `d1e6a9e64aa012beb9ab4f132a7e76e2552ecce9c730e06c9af6f32da9012eb7`;
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
    `e6f4a1ef5b0c395e0767987d6f7517b2c5d617cb4deadb6949eefad3488dcc81`.
- repeated validation:
  - same Client named family -> `29/29`, failures `0`, errors `0`;
  - same Cloud named family -> `49/49`, failures `0`, errors `0`;
  - Client and Cloud compile -> both exit `0`;
  - shared observation DTO -> `16/16`, differences `0`;
  - repaired exact write set `git diff --check` -> exit `0`.
- scope/safety: comment-only delta; no business behavior, test logic, protocol, runtime/UI/capture/input or Git
  mutation; read-only baseline untouched.

<!-- TRUE_EOF: COMBAT-OBS-P1 REVIEW1-COMMENT-REPAIR-COMPLETE WHOLE-CARD-REDELIVERED AWAITING-PARENT-REVIEW2 OWNER-CODEX CLIENT-29OF29 CLOUD-49OF49 DUAL-COMPILE DTO-16OF16 DIFF-CHECK-ZERO 2026-07-23T10:24:40-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - PASSED - 2026-07-23T10:31:00-04:00

- verdict: `P0=0 / P1=0 / P2=0`; `SOURCE+TEST PASSED / OWNER RELEASED / FRESH RUNTIME PENDING`.
- source:
  - Client local mechanics preserve the frozen ROI/template/threshold/`ANY/ALL` short circuit and emit only
    `VISIBLE / ABSENT / UNAVAILABLE`; per-run template cache is released at runner reset.
  - Cloud alone mutates `BattleRuntimeState`, miss count, hysteresis, enter/exit state and ready events.
  - `coordinate-strip` is the exact `activePathing || combatExitFallback` demand union; revision, observer
    sequence and capture time must all be fresh. A coordinate-only completion reuses `ABSENT` with
    `newSignalSample=false`, so it cannot add a duplicate miss.
  - no continuous `combat-flag`, `combat-selection`, `combat-top` or coordinate publication remains; existing
    `FastExpectedCombatExitProbe` source/test mtimes predate this card and behavior was not changed.
- parent validation:
  - Client named card family `29/29`; extra `FastExpectedCombatExitNoCommandPlaneTest` `1/1`;
  - Cloud named family `49/49`;
  - Client and Cloud compile exit `0`;
  - shared observation DTO `16/16`, differences `0`;
  - both repositories `git diff --check` exit `0`.
- residual test note: one expanded Client run observed a single asynchronous ACK timing failure in the existing
  `WindowObservationRunnerContractTest.transportFailureRetainsKeyEventsAndIsNeverABusinessFact`; the exact method
  and the original `29/29` family immediately passed on rerun. It is recorded as a pre-existing timing risk, not
  hidden as a clean first-pass run and not attributed to this card without causal evidence.
- runtime gate: no runtime/UI/live capture/input was run. Fresh acceptance must prove true combat entry, ordinary
  exit, Fast Expected Exit, pathing terminal continuity and no cross-window interference.

<!-- TRUE_EOF: COMBAT-OBS-P1 PARENT-REVIEW-2 SOURCE-TEST-PASSED OWNER-RELEASED P0-0 P1-0 P2-0 CLIENT-29OF29-PLUS1 CLOUD-49OF49 DUAL-COMPILE DTO-16OF16 FRESH-RUNTIME-PENDING 2026-07-23T10:31:00-04:00 -->

## FRESH RUNTIME P1 FAILURE / REPLACEMENT-LINEAGE REPAIR PASSED - 2026-07-23T13:22:05-04:00

- fresh run:
  - observation run `remote-turn-1820f6a3-c15e-42cb-a861-bc22cd8feab5`;
  - leader `hwnd-18530F58 / 67555`;
  - Client `13:13:06.934` through `13:14:00.251` rejected `38` consecutive observation requests with
    HTTP `400 INVALID_OBSERVATION_REQUEST`:
    `REPLACED pathing fact must name the current intent`;
  - Cloud remained in `WAIT_TRACKER_SHORTCUT_PATHING` for `179985ms`, received neither
    `PATHING_TERMINAL` nor `COMBAT_STATE_CHANGED`, then hit the 180-second pre-combat watchdog.
    Fast Expected Exit therefore never reached its required confirmed-combat arm point.
- `P1` root cause:
  - Client's accepted contract intentionally repeats the exact `REPLACED(old -> new)` lineage while the new
    intent remains current;
  - Cloud accepted only the first replacement. Every later sequence with the same valid lineage was rejected,
    and observation ingestion is atomic, so the malformed-pathing verdict also discarded same-envelope combat
    facts/events.
- repair:
  - Cloud `CloudWindowObservationInbox.validatePathing(...)` now accepts either the first replacement of the
    current intent or an exact replay of the already accepted replacement lineage;
  - different old/new identities, replacement after clear and any lineage not matching the accepted current
    state remain fail-closed;
  - no Client sampler, task phase, ROI/template/threshold, input, navigation or Fast Exit timing was changed.
- validation:
  - Cloud `CloudObservationContractTest` + `CloudWholeTaskObserverProductionHarnessTest`: `35/35`;
  - Client `WindowObservationRunnerContractTest`: `8/8`;
  - both Maven runs compiled their respective main/test sources successfully.
- fresh gate:
  restart Cloud, then rerun 修罗. The same observation run must have no repeated-lineage HTTP `400`;
  `PATHING_TERMINAL` or `COMBAT_STATE_CHANGED` must wake the parked phase before the 180-second watchdog;
  confirmed combat must then arm Fast Expected Exit, and real exit must advance post-combat recovery.

<!-- TRUE_EOF: COMBAT-OBS-P1 FRESH-RUNTIME-P1-FAILED REPLACEMENT-LINEAGE-REPAIR-SOURCE-TEST-PASSED CLOUD-35OF35 CLIENT-8OF8 RESTART-AND-FRESH-RERUN-REQUIRED 2026-07-23T13:22:05-04:00 -->

## FRESH RUNTIME P1 FINDINGS / ARM + REVISION REPAIR - 2026-07-23T13:52:05-04:00

- fresh evidence:
  - run `remote-turn-8247ca72-55a1-4f3c-a98a-9229539186d4`, leader
    `hwnd-18530F58 / 67555`;
  - three combat exits had no `FAST_EXPECTED_COMBAT_EXIT`/avatar-diff event and completed through ordinary
    BattleRadar repeated misses;
  - after `navigation.toNpc` completed, the task waited about `165053ms` for `PATHING_TERMINAL` before timeout;
    the eventual coordinate sync found `(113,94)` and the actual NPC click took only about 3 seconds.
- `P1-A` root cause and repair:
  - `CloudFastExpectedCombatExitCoordinator.authorizedTask(...)` compared enum names
    `XIULUO_V2/WUBEI`, but production `TaskExecutionContext.taskCode` carries
    `xiuluo_v2/wubei`;
  - authorization now uses `TaskType.XIULUO_V2.getCode()` and `TaskType.WUBEI.getCode()`;
  - missing coordinator/binding no longer masquerades as `ARMED`: it becomes
    `FULL_RADAR_FALLBACK`, retains the expected-wait identity and runs full radar normally;
  - arm/fallback logs now make the production branch directly auditable.
- `P1-B` root cause and repair:
  - dynamic `coordinate-strip` and `xiuluo-dialog` interests shared one run revision but each treated a revision
    advanced by the other as a reason to re-upsert itself. They could therefore bump revision forever and make
    every just-captured Client frame stale;
  - `CloudWindowObservationInbox.upsertInterest(...)` is idempotent for an identical existing interest and exposes
    `currentInterest(...)`;
  - `CloudWholeTaskObserver` now compares desired/current interest content and only mutates the revision when the
    interest actually changes. Removal is likewise content-aware. Existing identity/sequence/capture-time fences
    remain unchanged.
- exact Cloud source SHA-256:
  - `AutoCombatService.java`
    `2d65880e10a4e449641a498ef1c3ec1969f95204432dbef80ef7bd7d9379fd5f`;
  - `CloudWindowObservationInbox.java`
    `3d99255597df0a3f821fe6e6e406cc66db8b2ce1f76c287d2a8a5ef01369413f`;
  - `CloudFastExpectedCombatExitCoordinator.java`
    `09f0449a42573f009e6a8e668e6fb49cca1782f638528093ac31521f80a8d48f`;
  - `CloudWholeTaskObserver.java`
    `7e9854a8ec1ed27799c1611f3328f8e81167911946d0de3e1b46157c8f12b670`.
- verification:
  - `CloudFastExpectedCombatExitArmLifecycleTest` `5/5`;
  - `FastExpectedExitObservationContractTest` `5/5`;
  - `CloudWholeTaskObserverProductionHarnessTest` `22/22`;
  - combined focused family `32/32`, failures/errors `0`;
  - Cloud `mvn -q compile` exit `0`;
  - the repository intentionally ignores `src/test` and the migrated Cloud source tree is untracked, so review
    used physical file SHA/mtime and test reports rather than a misleading empty `git diff`.
- parent review: `P0=0 / P1=0 / P2=0` for this repair write set. No Fast Exit timing/ROI/threshold, business phase,
  navigation decision, NPC click, protocol or Client source changed.
- fresh gate: restart Cloud and rerun 修罗. Logs must show
  `fast expected-exit observation armed: task=xiuluo_v2` without a fallback warning; a real local exit event must
  advance post-combat recovery; coordinate/dialog interest revisions must converge; arrival must wake NPC handling
  without the prior 165-second timeout.

<!-- TRUE_EOF: COMBAT-OBS-P1 FRESH-RUNTIME-P1 ARM-AUTHORIZATION-AND-DYNAMIC-INTEREST-REVISION-REPAIRED PARENT-SOURCE-TEST-PASSED P0-0-P1-0-P2-0 CLOUD-32OF32 COMPILE-EXIT0 RESTART-AND-FRESH-RERUN-REQUIRED 2026-07-23T13:52:05-04:00 -->

## FRESH RUNTIME P1 - exact combat identity drift after arm

- observed run: `remote-turn-0b7b68b7-00cd-4573-8993-483d6dc304cb:0:XIULUO_V2`;
  Client observation run `remote-turn-0b7b68b7-00cd-4573-8993-483d6dc304cb`, leader
  `hwnd-18530F58`, player `67555`.
- runtime evidence: local kanda click completed at `14:03:26`; Cloud emitted
  `fast expected-exit observation armed` at `14:03:28`. Full radar then confirmed exit twice at
  `14:04:12` and `14:04:20`, but both valid exits were immediately rejected as
  `discard stale expected combat-exit signal`; task remained `WAIT_COMBAT`.
- root cause: observer combat-enter established identity A and Fast Exit armed against A. The same task tick
  confirmed combat-enter again, cleared the expected wait/generation and established identity B.
  `AutoCombatService.maybeHandleCombatEnter` only reconciled `PENDING`; because the arm was already `ARMED`,
  observation remained on A while task consumption required B.
- repair:
  - `CloudFastExpectedCombatExitCoordinator.reconcileAfterCombatEnter` now reconciles `PENDING` and `ARMED`;
  - same combat identity is idempotent and publishes nothing;
  - a changed `combatStartedAtMs` atomically replaces exact wait/interest;
  - `AutoCombatService` invokes reconciliation for both states after confirmed combat-enter.
- source SHA-256:
  - `CloudFastExpectedCombatExitCoordinator.java`:
    `727E6E63A61CCE462EAF315E0C9CC737648E0610D8CF813D7C85AE72E4B1484D`;
  - `AutoCombatService.java`:
    `330E98870F15C87350D3D70CD480CBD8E2D72273952A1F93A9D961F844A65171`;
  - `CloudFastExpectedCombatExitArmLifecycleTest.java`:
    `FCE55505D6F04A85A1A1F644113CD1EB01D0953F3E506CABB2A3F2817A051F1F`.
- verification: focused Cloud family `48/48`:
  `CloudFastExpectedCombatExitArmLifecycleTest` 5/5, `FastExpectedExitGateTest` 2/2,
  `BattleRadarTurnContractTest` 14/14, `FastExpectedExitObservationContractTest` 5/5,
  `CloudWholeTaskObserverProductionHarnessTest` 22/22. `mvn -q compile` exit `0`.
  An attempted `-DskipTests compile` was rejected by the repository Enforcer policy; the allowed compile
  then passed and this was not a source failure.
- parent source review: `P0=0 / P1=0 / P2=0` for this narrow repair. No Client source, ROI, threshold,
  timing, template, navigation, NPC, input or task-phase behavior changed.
- fresh gate: restart Cloud and rerun. A confirmed combat-enter identity replacement must reconcile the active
  Fast Exit wait; a subsequent valid exit must advance post-combat recovery. This run did not show a local
  `20x20` fast-edge event, so the fresh run must separately verify that path; full-radar fallback must progress
  even when the local edge misses.

<!-- TRUE_EOF: COMBAT-OBS-P1 FRESH-RUNTIME-P1 EXACT-COMBAT-IDENTITY-RECONCILED PARENT-SOURCE-TEST-PASSED P0-0-P1-0-P2-0 CLOUD-48OF48 COMPILE-EXIT0 RESTART-AND-FRESH-RERUN-REQUIRED 2026-07-23T14:16:54-04:00 -->

## FRESH RUNTIME P1 - exact fast-exit pending overwritten by overlapping visible sample

- observed run: `remote-turn-92876022-061e-4330-95fb-70c8c294bdd3:0:XIULUO_V2`, leader
  `hwnd-18530F58 / 67555`; Cloud JVM started after the prior source repair.
- exact runtime chain:
  - generation 2 Fast Exit armed at revision 5;
  - local Fast Exit was accepted: Cloud published `COMBAT_STATE_CHANGED` sequence 3 with
    `source=fast-expected-combat-exit:*`;
  - task wake completed in `313ms`, proving the `20x20` local edge, transport, exact gate and ready-event path worked;
  - before task consumption, the no-turn observer completed an overlapping `COMBAT_SIGNAL=VISIBLE`, restored
    `IN_COMBAT` and registered battleCount 3;
  - task discarded the pending exit because it was now `IN_COMBAT`; later full-radar exit was stale against the
    manufactured generation.
- repair: while an exact fast-exit is pending, correlated to the current expected wait/generation, and the state is
  already `FREE`, `checkAndSyncMechanicalCombatSignal(...)` ignores an overlapping visible sample. This boundary
  ends immediately when the task consumes the one-shot exit; a later true visible sample still enters a new combat.
- source SHA-256:
  - `BattleRadarService.java`:
    `4D5FDD9AE29540F653CA0098A73853FA99371A3E0E6417B95665F2159E81B4B1`;
  - `BattleRadarTurnContractTest.java`:
    `C89720E6F70B7A170714184F1A2FD72BC68AB771867BAE909EC3A0CE3C91CEC7`.
- verification: `BattleRadarTurnContractTest` 15/15,
  `CloudFastExpectedCombatExitArmLifecycleTest` 5/5, `FastExpectedExitGateTest` 2/2,
  `FastExpectedExitObservationContractTest` 5/5,
  `CloudWholeTaskObserverProductionHarnessTest` 22/22; total `49/49`.
  Cloud `mvn -q compile` and exact write-set `git diff --check` exit `0`.
- parent source review: `P0=0 / P1=0 / P2=0`. No Client source, Fast Exit ROI/threshold/timing,
  full-radar miss/readability rule, template, navigation, input or business phase changed.
- fresh gate: restart Cloud and rerun. The accepted fast edge must advance post-combat directly, without same-combat
  battleCount resurrection, `discard stale combat-exit signal while still IN_COMBAT`, or later exact stale discard.

<!-- TRUE_EOF: COMBAT-OBS-P1 FRESH-RUNTIME-P1 FAST-EXIT-PENDING-VISIBLE-RACE-REPAIRED PARENT-SOURCE-TEST-PASSED P0-0-P1-0-P2-0 CLOUD-49OF49 COMPILE-EXIT0 DIFF-CHECK-ZERO RESTART-AND-FRESH-RERUN-REQUIRED 2026-07-23T14:27:41-04:00 -->

## FRESH-RUNTIME P1 - Exact Fast Exit Must Be Consumed Before Radar

- fresh run: `remote-turn-005a7151-e207-47af-bcfa-29bb4ef233f0:0:XIULUO_V2`.
- evidence: fast edge published and woke the task in about `413ms`; the observer logged
  `ignore visible combat signal while exact fast-exit awaits consumption`. The task's own
  `handleCombatTick(...)` then ran sparse full radar before consumption, restored `IN_COMBAT`,
  and discarded the exact one-shot.
- repair: `AutoCombatService.handleCombatTick(...)` consumes a verified exact exit while the
  window is `FREE` before any full-radar fallback. No exact exit means the original radar path.
- source SHA256:
  `AutoCombatService.java=F2DACC81C1059996F8F0EA918A190B082C22560F0A6BE6FA03D3A334F232112F`;
  `AutoCombatServiceTurnContractTest.java=AF0DDB8539FB255A9F9792CA12888DC111F4765F40F0A57C2D2C6B02C5A5486A`.
- verification: new exact regression `1/1`; existing fast/observer families `49/49`;
  Cloud compile and exact diff-check exit `0`. The full `AutoCombatServiceTurnContractTest`
  has pre-existing fixture/order debt (`39` tests, `4 failures / 6 errors`) outside this repair.
- parent source review: `P0=0 / P1=0 / P2=0`. Restart Cloud and fresh rerun.

<!-- TRUE_EOF: COMBAT-OBS-P1 FRESH-RUNTIME-P1 FAST-EXIT-CONSUME-BEFORE-RADAR-REPAIRED PARENT-SOURCE-TEST-PASSED P0-0-P1-0-P2-0 NEW-REGRESSION-1OF1 EXISTING-FAST-OBSERVER-49OF49 COMPILE-EXIT0 DIFF-CHECK-ZERO RESTART-CLOUD-FRESH-RERUN-REQUIRED 2026-07-23T14:43:42-04:00 -->
