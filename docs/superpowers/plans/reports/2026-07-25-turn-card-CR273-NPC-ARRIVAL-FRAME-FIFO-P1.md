# CR273 NPC ARRIVAL FRAME FIFO P1

## Status

`READY / ZERO OWNER / WHOLE-CARD SOURCE+TEST REQUIRED`

## Workspace Contract

- Client authority: `D:\mavenProject\DHXY-cr271`, branch `thin-client-design`, observed HEAD
  `59b85e0bb494`.
- Cloud authority: `D:\mavenProject\dhxy-cloud-brain`, branch `navigation-migration`, observed HEAD
  `3b988caa0102`.
- Read-only business baseline: `D:\mavenProject\DHXY`, branch
  `codex/baseline-696a12b0`, HEAD `696a12b0ffb8`.
- The baseline worktree is protected. Do not switch its branch, edit it, copy generated files into it,
  or use it as the Worker workspace.
- All three worktrees are dirty. Preserve unrelated tracked and untracked changes. No Git reset,
  checkout, clean, commit, branch switch or stash.

## User Decision

For targeted current-map navigation followed by NPC click, one exact stable Runner frame is the only
arrival image:

1. Cloud registers the existing navigation intent and the existing `NpcClickRequest`.
2. Client executes the existing mini-map navigation while preserving the existing task-turn policy.
3. The local Runner captures one fresh exact-window frame when the pathing generation is stationary
   enough to request a coordinate verdict. The coordinate strip must be cropped from that same frame.
4. That frame is uploaded once under exact
   `(tenant, device, window, hwnd, taskRunId, intentId, frameId/generation)`.
5. Cloud may immediately and concurrently:
   - recognize map/x/y from the frame's coordinate strip; and
   - start the existing asynchronous NPC smart-click FIFO producer from the full frame.
6. The local Runner remains the sole pathing authority. It validates the returned coordinates against
   the same intent/frame generation, applies the existing map/tolerance and 600 ms stationary rule, and
   emits `ARRIVED(frameId)` or keeps/finishes the existing non-arrival path.
7. An exact matching `ARRIVED(frameId)` unlocks that already-producing FIFO. Before that event, no
   candidate may be consumed and no mouse/keyboard action may run.
8. Client consumes candidates in the existing FIFO order as they become available. Do not wait for the
   complete candidate list before the first eligible click.
9. If the first FIFO is exhausted without success, perform exactly one local `cleanupAll`, capture one
   new fresh frame, and start one replacement FIFO. No third FIFO is allowed.

The stable frame is not uploaded again after `ARRIVED`; Cloud must not request another screenshot for
the first attempt. The `ARRIVED` message is a small exact-frame fact, not an image transfer.

## Frozen Business Semantics

- Reuse the current `NpcClickSmartQueueStore` asynchronous producer and
  `SmartClickRecognizer.produceQueueMessages(...)`.
- Preserve the validated candidate order:
  `MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES -> END`.
- Preserve existing templates, ROI formulas, thresholds, tune offsets, Ctrl safety checks, dialog
  verification, move+click atomicity, and `NpcClickSmart` result meanings.
- Do not create a second recognizer, second queue/store, complete-plan shadow algorithm, polling
  observer, Cloud pathing authority, or Cloud arrival reclassification.
- Do not change generic `NpcClickSmart` internals merely to fit this route. Adapt the transport/session
  boundary around the existing implementation.
- Do not change the existing keep-turn policy globally. This card only ensures that the exact route's
  already-selected policy remains in force through `ARRIVED` and FIFO execution.
- No proactive UI cleanup. Cleanup occurs only after the first complete FIFO failure and exactly once
  before the replacement frame/session.

## Current Gap

- `WindowObservationSampler` currently captures only a `178x35` coordinate strip for terminal
  recognition; the observation protocol explicitly rejects ordinary whole-window ROI frames.
- Cloud `NpcClickService.runSingleFrameNpcClickPlan(...)` performs a fresh post-arrival capture and
  converts the full producer output into a complete `NpcPreparedClickPlan`, so recognition and click
  begin only after the extra round trip and full-list materialization.
- `NpcClickSmartQueueStore` already supports asynchronous staged production, but the current targeted
  arrival path does not reconnect that queue to an exact local consumer.
- The Client HEAD contains the previously validated FIFO DTO/session/consumer family
  (`NpcClickSmartCloudDecisionService` and related records), while the current dirty CR271 tree deletes
  it. Treat HEAD as design evidence only: restore/adapt the minimum required files, and do not restore
  unrelated deleted business services.

## Required Design

### A. One-Shot Terminal Candidate Frame

- Add a dedicated observation payload for at most one bounded terminal-candidate frame. Do not relax
  the ordinary `ObservationRoi` limit globally.
- The payload must carry exact identity, full-frame geometry, encoding, positive `frameId`, capture
  timestamp, pathing generation and intent id.
- Client and Cloud protocol records and validators must remain byte-identical where shared.
- A frame may be sent once per exact generation. Movement, intent replacement, run replacement,
  native binding replacement, pause/stop/reset or `STOPPED_AWAY` must invalidate the pending frame and
  any speculative queue.

### B. Parallel Cloud Preparation

- On receipt of a valid frame, Cloud must start coordinate recognition and the existing FIFO producer
  without waiting for one another.
- Store only one exact preparation session per
  `(windowId, taskRunId, intentId, frameId)`.
- Duplicate delivery must be idempotent. Stale/wrong-run/wrong-window/wrong-hwnd/wrong-intent/wrong-frame
  input is rejected and cannot replace the current session.
- Speculative preparation is computation only. It grants no command/input authority.

### C. Exact ARRIVED Gate

- The existing local Runner remains the sole authority for `ARRIVED`.
- `ARRIVED` must reference the exact frame used for both coordinate recognition and FIFO preparation.
- Cloud publishes/unlocks the prepared FIFO only when the event identity matches every session field.
- `ACTIVE`, `STOPPED_AWAY`, clear, replacement and terminal task state cancel the session.
- No Cloud observer, radar, coordinator or periodic poll may manufacture, confirm or override arrival.

### D. Local FIFO Consumption

- Restore/adapt the minimum validated Client FIFO transport/session consumer from `59b85e0b`.
- Consume one candidate at a time as Cloud produces it. Preserve exact-window identity and existing
  atomic move+click input submission.
- The first candidate must be eligible immediately after exact `ARRIVED`, even if later recognition
  stages are still running.
- Queue completion and cancellation must be exact-run and idempotent. Stop/pause/restart cannot leak
  candidates into a newer run.

### E. One Replacement Attempt

- Only after the first queue reaches `END`/final failure without verification:
  local `cleanupAll` once -> fresh exact-window frame -> new frame/session identity -> second FIFO.
- The second attempt uses the same ARRIVED location fact; it does not navigate again and does not upload
  the first frame again.
- A second failure returns the existing NPC-click failure result to the owning task. No third attempt,
  hidden fallback, proactive cleanup or re-navigation.

## Initial Write Set

The Worker must first audit imports/callers and may amend this list in the card before editing if a
transitive dependency is missing. Any expansion must explain why it is necessary and must not include
unrelated migration cleanup.

### Client

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationRequest.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationProtocolValidator.java`
- one new shared terminal-frame protocol record under the same package
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java` only if exact frame/session
  lifecycle cannot be held safely by the Runner
- minimum required `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloud*.java` FIFO family restored
  from `59b85e0b` and adapted to the current HTTPS turn client
- the current local-operation executor/session owner that performs the existing NPC click actions

### Cloud

- mirrored observation protocol record/request/validator
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationHttpHandler.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` only for exact session exposure, not
  recognition changes
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java` or the current
  exact ready-event owner
- `XiuluoTaskV2` and `WubeiTask` only where their targeted navigation result currently invokes the
  post-arrival full-plan path

## Required Tests

1. Shared protocol files are byte-identical and validators accept one legal terminal frame while
   rejecting oversized, duplicate, malformed and identity-mismatched frames.
2. One stationary generation uploads the full frame exactly once; the coordinate strip derives from
   that same frame; `ARRIVED` causes zero additional screenshot requests.
3. FIFO production starts before `ARRIVED`, but no candidate can be consumed or executed before exact
   matching `ARRIVED(frameId)`.
4. Wrong/stale run, window, hwnd, intent, generation or frame cannot unlock a queue.
5. Movement/replacement/`STOPPED_AWAY`/stop/restart cancels speculative work.
6. The first candidate is consumable before a deliberately delayed later producer stage completes.
7. Candidate order and existing SmartClick recognizer behavior remain unchanged.
8. First full failure performs one `cleanupAll`, one fresh frame and one replacement FIFO; second
   failure terminates with no third attempt and no re-navigation.
9. Xiuluo and Wubei targeted NPC paths use this contract while preserving their existing keep-turn
   choice and task result semantics.
10. Focused Client and Cloud named tests plus both main compiles pass without runtime/UI/capture/input.

## Prohibited Verification

- Do not start Client, Cloud server, JavaFX, game runtime, live capture or physical input.
- Do not claim fresh-runtime success from unit tests.

## Delivery Contract

The Worker appends a canonical `WHOLE-CARD SOURCE+TEST DELIVERED` block at physical EOF containing:

- exact files changed and why;
- before/after SHA and mtime;
- protocol byte-identity evidence;
- named test totals and compile exits;
- explicit proof of one-upload/no-second-capture, pre-ARRIVED execution prohibition, FIFO streaming,
  cancellation identities and exactly-one replacement attempt;
- remaining fresh-runtime gate.

Parent performs the only final review. `P0/P1/P2=0/0/0` is required before
`SOURCE+TEST REVIEW PASSED`.

## Canonical Transitive Write-Set Amendment — 2026-07-25

The claimed Worker completed the imports/callers/transitive-owner audit before touching Java source.
The initial list is expanded only where the existing protocol, exact ready-event owner, HTTPS
transport and local-operation dispatcher form mandatory compile/runtime boundaries:

### Client additions

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationTerminalFrame.java`
  (new dedicated bounded whole-frame carrier; ordinary ROI limits remain unchanged).
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`,
  `TurnWholeTaskRuntimeArguments.java`, `TurnWholeTaskRuntimeResult.java` and
  `TurnProtocolValidator.java` (byte-identical closed local-operation contract for exact FIFO consume).
- `src/main/java/com/bot/dhxy/cloud/turn/HttpsTurnClient.java` plus the minimum restored/adapted
  `cloud/task/NpcClickSmartCloudSession.java`, `NpcClickSmartQueueMessage.java` and
  `NpcClickSmartQueueOutcome.java` (same authenticated origin; poll/outcome only, no second store or
  recognizer).
- `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java` and
  `src/main/java/com/bot/dhxy/cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java`
  (the existing exact-window local-operation owner must dispatch the consumer).
- One focused local FIFO executor under `src/main/java/com/bot/dhxy/cloud/turn/local/` and only the
  existing 59b85e0b candidate mechanics needed to preserve atomic point/Ctrl handling. This is the
  minimum consumer shell, not restoration of the deleted local business service.
- Focused tests under `src/test/java/com/bot/dhxy/cloud/turn/protocol/`,
  `src/test/java/com/bot/dhxy/window/observation/` and
  `src/test/java/com/bot/dhxy/cloud/turn/local/`.

### Cloud additions

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationTerminalFrame.java`
  (byte-identical mirror).
- Mirrored `TurnLocalOperation.java`, `TurnWholeTaskRuntimeArguments.java`,
  `TurnWholeTaskRuntimeResult.java` and `TurnProtocolValidator.java`.
- One narrow authenticated FIFO endpoint/handler registered by
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`; it exposes only the existing
  `NpcClickSmartQueueStore` session and outcome operations.
- The existing exact task/intent demand owner used by `NpcClickService` and
  `CloudWholeTaskReadyEventState`; no observer, radar or second coordinator is added.
- Focused tests under `src/test/java/com/yueyunfe/dhxy/cloudbrain/observation/`,
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/` and the existing Xiuluo/Wubei contract families.

No approved business difference is introduced. The amendment preserves the baseline-equivalent
candidate algorithm and only replaces the post-arrival transport/materialization boundary.

## Canonical WHOLE-CARD CLAIM — 2026-07-25

- owner: `Worker Parfit / 019f978c-097f-7f32-a3d0-6e75a8db8d9b`
- state: `SOURCE ACTIVE`
- scope: `CR273 WHOLE-CARD SOURCE+TEST`
- authority: This physical-EOF claim supersedes the earlier `READY / ZERO OWNER` status. The Worker
  must deliver the complete card and may not self-approve.

<!-- TRUE_EOF: CR273 SOURCE-ACTIVE OWNER-WORKER-PARFIT-019f978c-097f-7f32-a3d0-6e75a8db8d9b 2026-07-25 -->

## Canonical OWNER RETURNED — 2026-07-25

- former_owner: `Worker Parfit / 019f978c-097f-7f32-a3d0-6e75a8db8d9b`
- state: `READY / ZERO OWNER`
- reason: More than twenty minutes after claim, both Client and Cloud source mtimes remained unchanged
  and two directed status requests received no response. The agent was closed while still `running`.
- source_effect: `ZERO JAVA CHANGE`; existing dirty worktrees remain untouched.

<!-- TRUE_EOF: CR273 OWNER-RETURNED READY ZERO-OWNER ZERO-JAVA-CHANGE 2026-07-25 -->

## Canonical WHOLE-CARD CLAIM — 2026-07-25

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- state: `SOURCE ACTIVE`
- scope: `CR273 WHOLE-CARD SOURCE+TEST`
- authority: This physical-EOF claim supersedes the preceding `OWNER RETURNED / READY / ZERO OWNER`
  state. The Worker must deliver the complete card and may not self-approve.

<!-- TRUE_EOF: CR273 SOURCE-ACTIVE OWNER-WORKER-NOETHER-019f9799-96b7-7423-a589-8d569191046b 2026-07-25 -->

## STATUS EVENT — 2026-07-25 / Increment 1 protocol gate

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- delivered scope: dedicated one-shot `ObservationTerminalFrame`, `ObservationRequest` carrier,
  strict validator and focused Client contract test. Ordinary `ObservationRoi` remains capped at
  `640x640`; terminal payload is exactly one `1024x768` PNG with positive frame/generation and exact
  tenant/device/window/hwnd/run/intent identity.
- files/SHA-256:
  - both repos `ObservationTerminalFrame.java`:
    `CDEC0918D2D7C65585860123F79334229140243C71A9997525D0BCF28F5FBF28`
  - both repos `ObservationRequest.java`:
    `B9FFF08DC6410C53C9C9C832A10CA220672800A67F02EA694370F59F9C2C33AA`
  - both repos `ObservationProtocolValidator.java`:
    `509F72DC599DC0DA4F67BDF3C64021C9CB835CD6D80DF373AB64B90CBCC6E6CB`
  - Client `ObservationTerminalFrameContractTest.java`:
    `A1DAB99D365F1B887F289664775F52F2623183517635AE7044CA32C295B9FB88`
- protocol identity: all three shared production files are byte-identical across Client and Cloud.
- verification:
  - Client `mvn -q -Dtest=ObservationTerminalFrameContractTest test`: `5/5`, exit `0`.
  - Client `mvn -q -DskipTests compile`: exit `0`.
  - Cloud `mvn -q -DskipTests=false compile`: exit `0`.
  - The initial Cloud `-DskipTests compile` was rejected only by the repository's
    `require-tests-enabled` enforcer; the required command above passed.
- no runtime, UI, live capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: CR273 STATUS-INCREMENT-1 PROTOCOL-TEST-COMPILE-GREEN 2026-07-25 -->

## STATUS EVENT — 2026-07-25 / Increment 2 same-frame Runner upload

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- delivered scope: the bound Runner captures one exact `1024x768` frame for a stationary pathing
  generation, derives `coordinate-strip` from those same in-memory pixels, retains one
  `(frameId, generation, bytes)` across transport uncertainty, and removes the full upload only after
  a successful response. Movement, intent replacement, suspend and stop/reset invalidate terminal
  evidence; resume must therefore produce a fresh generation.
- files/SHA-256:
  - Client `WindowObservationSampler.java`:
    `3BE5C33C630E31902B0827AACB3F8E6CD23AE9B2A144261A8CA78088430DD004`
  - Client `WindowObservationRunner.java`:
    `FFC7E9B4327FAAACC7A9B986542F1FEABDB890013F282A01319B3DB8E8A117B8`
  - Client `HttpsObservationClient.java`:
    `12963E36B9982DF4155EF23F534405E2390F6FC4472697BCE0E0D4472643C35E`
  - Client `WindowObservationTerminalFrameContractTest.java`:
    `26456C5FFA585F491114B73FBE73194D089F02049214F27A67877DFE9F32E544`
  - Cloud `CloudObservationHttpHandler.java`:
    `50BE68CCE3ECA58C95FB74F26CB178B6C682CF10D6996981CCB62012BE34BE8E`
- verification:
  - Client
    `mvn -q -Dtest=WindowObservationTerminalFrameContractTest,ObservationTerminalFrameContractTest test`:
    `9/9`, exit `0`.
  - Client `mvn -q -DskipTests compile`: exit `0`.
  - Cloud `mvn -q -DskipTests=false compile`: exit `0`.
  - An expanded pre-existing observation family ran `24` tests; the new CR273 tests and current
    pathing/HTTPS tests passed. Its sole failure is the stale CR271 fixture
    `samplerUploadsExactCurrentPathingSnapshotAndClearReplacementLineage`, which still expects every
    replacement sample to be `REPLACED` although Repair #26 makes only the first edge `REPLACED` and
    later samples `CURRENT`. No parent-owned CR271 behavior was changed.
- no runtime, UI, live capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: CR273 STATUS-INCREMENT-2 SAME-FRAME-RUNNER-TEST-COMPILE-GREEN 2026-07-25 -->

## STATUS EVENT — 2026-07-25 / Repair P1 dual run identity in progress

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- finding acknowledged: production observation transport uses
  `observationRunId=startRequest.startRequestId()` (for example `remote-turn-x`), while the Cloud
  task/local-operation identity is `businessTaskRunId=context.getTaskRunId()` (for example
  `remote-turn-x:0:XIULUO_V2`). The current CR273 demand registration incorrectly keys the FIFO with
  the latter while terminal frame and accepted ARRIVED facts carry the former, so production prepare
  returns `NO_DEMAND` and the exact gate returns `STALE_ARRIVAL`.
- repair now in progress:
  - reuse `CloudWholeTaskReadyEventState`'s exact observation-run-to-`TaskExecutionContext` binding;
  - register/store/gate by the unmodified `observationRunId`, retain and independently validate the
    unmodified `businessTaskRunId` for session and local-operation ownership;
  - carry both identities through the CR273 queue endpoint and local consumer only where both are
    required; never strip a suffix, hash, guess, or compare the two identities as equal;
  - add a production-format positive test using `remote-turn-x` and
    `remote-turn-x:0:XIULUO_V2`, plus cross-observation-run and cross-business-task negatives.
- expected files:
  - Cloud `CloudWholeTaskReadyEventState.java`, `NpcClickService.java`, `DecisionEngine.java`,
    `NpcClickSmartQueueStore.java`, `CloudWholeTaskRuntimeLocalServiceClient.java`;
  - shared Client/Cloud `TurnWholeTaskRuntimeArguments.java` and validator as needed;
  - Client `NpcArrivalFrameFifoLocalExecutor.java`, `TurnClient.java`/HTTPS transport as needed;
  - focused Cloud/Client CR273 contract tests.
- contract blocker: none. Verification blocker remains limited to the already recorded parent-owned
  Cloud dirty tests whose stale constructors/API references prevent global `testCompile`; CR273 named
  tests will be compiled/run in isolation if that unrelated blocker remains.
- no runtime, UI, live capture, physical input or Git mutation is authorized or being performed.

<!-- TRUE_EOF: CR273 STATUS-REPAIR-P1-DUAL-RUN-IDENTITY-IN-PROGRESS 2026-07-25 -->

## STATUS EVENT — 2026-07-25 / Parent incremental review P1 blockers acknowledged

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- delivery remains prohibited until all four findings below are repaired and focused-tested:
  1. Client `NpcArrivalFrameFifoLocalExecutor` must precisely restore the `59b85e0b`
     `NpcClickService` FIFO consumer safety shell: candidate limit `12`, exact session/window/task
     stale reporting, typed `CANCELLED`/`STALE_IGNORED`/`INPUT_SUBMIT_FAILED`/
     `DIALOG_OPEN_UNVERIFIED`/`STORY_DIALOG_VISIBLE` outcomes, baseline ordinary and Ctrl
     candidate checkpoints/allowed-region/atomic-input behavior, and
     `queueOutcomeForVerification` semantics. The transport may use at most two sessions and no
     caller may add a third retry.
  2. Every applicable `PATHING_STARTED` branch must register the arrival-frame demand as its first
     business action, before maintenance, deferred recovery, input, wait, or any other work that
     could allow the Runner's one-shot terminal frame to arrive and be ACKed as `NO_DEMAND`.
     Xiuluo target near the current `2354-2360` ordering is explicitly affected; all Wubei sites
     require the same audit.
  3. The complete targeted current-map call surface must include Xiuluo maintenance broadcast
     (`heal-pet` and `repair`), Xiuluo accept-NPC when pathing starts, Xiuluo combat target, and every
     applicable Wubei combat/accept/maintenance route, while preserving existing keep-turn and
     non-ARRIVED fallback decisions.
  4. `CloudObservationHttpHandler` ordering needs focused proof that a byte-identical duplicate is
     idempotent, a conflicting same-sequence request is rejected by the inbox, and a lower sequence
     neither prepares a terminal frame nor applies a pathing gate.
- immediate implementation order: baseline FIFO safety-shell extraction and Client repair; demand
  ordering/callsite completion; observation duplicate/stale tests; dual-identity and whole-card
  compile/focused verification.
- contract blocker: none in the contract itself. The already recorded parent-owned Cloud dirty
  test-compilation failures remain an external verification constraint only.
- no runtime, UI, live capture, physical input or Git mutation is authorized or being performed.

<!-- TRUE_EOF: CR273 STATUS-PARENT-P1-FOUR-BLOCKERS-ACKNOWLEDGED 2026-07-25 -->

## STATUS EVENT — 2026-07-25 02:03 / FIFO safety-shell baseline audit complete

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- exact `59b85e0b` conclusions now being implemented:
  - `consumeNpcClickSmartCloudSession` has a `12` candidate-message budget; `WAIT` and stale
    session/window/task messages do not consume it. Stale messages report `STALE_IGNORED`.
  - `MEMORY` is handled separately from ordinary
    `TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA`; a missing click point is `SKIPPED`.
    `CTRL_CANDIDATES` remains last before `END`.
  - every candidate reports one typed queue outcome. Only `SKIPPED` and
    `VERIFICATION_FAILED` continue; `VERIFIED` succeeds and
    `DIALOG_OPEN_UNVERIFIED`, `CANCELLED`, `INPUT_SUBMIT_FAILED`, `SAFETY_REJECTED`, and
    other terminal outcomes stop that session.
  - ordinary candidates enforce the request allowed region, preserve atomic
    `move -> 150ms -> click(150ms) -> 1500ms`, distinguish stop cancellation from input
    submission failure, then map verifier output through `queueOutcomeForVerification`.
  - Ctrl candidates enforce stop and allowed-region checks per probe, use one exclusive input
    callback, preserve the five offsets, bounded scan rectangle, `0.80` template threshold,
    direct click timing, and unconditional Ctrl release. An open option dialog without the expected
    target is `DIALOG_OPEN_UNVERIFIED`.
  - CR255 `STORY_DIALOG_VISIBLE` is sampled only at the natural FIFO boundary; it reports
    `CANCELLED`, performs the known small-story fast click, and restarts within the CR273 hard cap of
    two total FIFO sessions.
- currently editing:
  - Client `NpcArrivalFrameFifoLocalExecutor.java`, `TurnClient.java`, `HttpsTurnClient.java`,
    dedicated whole-task arrival arguments/validator, and focused executor tests;
  - Cloud `NpcClickSmartQueueStore.java` and `DecisionEngine.java` for exact session open/outcome
    report on the existing store/recognizer;
  - next immediately: Xiuluo/Wubei PATHING_STARTED registration ordering and missing maintenance/
    accept callsites, then observation duplicate/stale tests.
- contract blocker: none. The known parent-owned Cloud global `testCompile` failures remain an
  external verification constraint; main compile is green and CR273 tests will run in isolation.
- no runtime, UI, live capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: CR273 STATUS-FIFO-BASELINE-AUDIT-COMPLETE-EDITING-IN-PROGRESS 2026-07-25 -->

## STATUS EVENT — 2026-07-25 02:18 / FIFO safety shell and accepted-side-effect seam compile increment

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- completed in this increment:
  - Client FIFO consumer now locks the `59b85e0b` safety-shell invariants in a focused named
    contract: candidate limit `12`; exact session/window/business-task stale reporting; typed
    `CANCELLED`, `STALE_IGNORED`, `INPUT_SUBMIT_FAILED`, `DIALOG_OPEN_UNVERIFIED`,
    `VERIFICATION_FAILED`, `SAFETY_REJECTED` and `FINAL_FAILED`; ordinary atomic move/click timing;
    Ctrl five-offset/ROI/`0.80`/release checkpoints; STORY boundary cancellation; and exactly two
    sessions with one cleanup/replacement.
  - Cloud handler now selects terminal prepare work and gate work only through an explicit result
    derived after `inbox.accept`: current/byte-identical duplicate may call idempotent prepare,
    while gate sees only `acceptedPathingFacts`.
  - removed the tautological lower-sequence store test. Its replacement executes the accepted
    side-effect seam with counters: current prepare=`1`, producer starts=`1`, gate=`1`; duplicate
    prepare call=`2` but producer starts remains `1` and gate remains `1`; same-sequence conflicting
    bytes throw; lower sequence changes none of the counters.
- files/SHA-256:
  - Client `NpcArrivalFrameFifoLocalExecutor.java`:
    `71F6F9EF112124BB7B84A9C4328A2DDA081FDE8AD68EC27EFE48DC2BACEB42FD`
  - Client `NpcArrivalFrameFifoLocalExecutorContractTest.java`:
    `2EE1FFF921F43E1BE930DF13C4703EFE748C16D9E242BC5A82EB42268C2849BA`
  - Cloud `CloudObservationHttpHandler.java`:
    `31DA555E13E445B2AB5B37EF53FC0C30C7BD3ADAACC83A101117F0E359E7D20D`
  - Cloud `CloudObservationContractTest.java`:
    `A4E3FA9962CE0D658697F22A536698AEB6DCFD46AA31F52602721C078B653C7F`
  - Cloud `NpcArrivalFrameQueueStoreContractTest.java`:
    `A8C621846B62D4DA03BD5037F863DF248DDAD47CA6DFB2E903FBAA5CAC06D135`
- verification:
  - Client `mvn -q -Dtest=NpcArrivalFrameFifoLocalExecutorContractTest test`: `4/4`, exit `0`.
  - Cloud `mvn -q -DskipTests=false compile`: exit `0`.
- current editing next: Xiuluo/Wubei source/behavior callsite contract, dedicated dual-run validator
  tests, then isolated Cloud focused class compilation/execution.
- contract blocker: none. Parent-owned Cloud global `testCompile` failures remain an external
  verification constraint and are not being modified.
- no runtime, UI, live capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: CR273 STATUS-FIFO-SHELL-ACCEPTED-EFFECT-SEAM-COMPILE-INCREMENT 2026-07-25 -->

## STATUS EVENT — 2026-07-25 03:14 / production repair written, final verification in progress

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- completed write set:
  - Client dedicated arrival FIFO protocol/spec/validator/HTTPS transport/local dispatcher and
    streaming `NpcArrivalFrameFifoLocalExecutor`; exact 59b85e0b safety shell (`12` candidates,
    typed outcomes, exact stale identity, ordinary/Ctrl atomic safety, STORY boundary), two-session
    hard cap, one cleanup/fresh replacement only.
  - Client Runner same-frame terminal capture/coordinate strip/exactly-once generation upload and
    runtime STORY sequence support.
  - Cloud exact observation-run/business-task-run binding, existing `NpcClickSmartQueueStore` demand,
    prepare, exact ARRIVED gate, OPEN/POLL/REPORT/REPLACE endpoint adaptation, and accepted-only
    observation side-effect seam.
  - Xiuluo accept NPC, heal-pet, repair-equipment and combat-target callsites; Wubei accept,
    maintenance and tracker-combat callsites. PATHING_STARTED registration is before subsequent
    maintenance/input/wait work. Exact FIFO second failure no longer falls into outer cleanup/click
    retry.
  - focused tests for Client safety-shell invariants; Cloud current/duplicate/lower/conflicting
    observation behavior with prepare/producer/gate counters; production-format dual identities;
    exact gate/streaming/replacement; Xiuluo/Wubei callsite coverage.
- verification completed so far:
  - Client `NpcArrivalFrameFifoLocalExecutorContractTest`: `4/4`, exit `0`.
  - Client `TurnProtocolValidatorContractTest`: `21/21`, exit `0`, including dedicated distinct
    `remote-turn-x` / `remote-turn-x:0:XIULUO_V2` payload positives and negatives.
  - Cloud isolated focused execution:
    `NpcArrivalFrameTaskCallsiteContractTest` +
    `NpcArrivalFrameQueueStoreContractTest` +
    `CloudObservationContractTest`: `21/21`, failures `0`; exact classes compiled with `javac`
    exit `0` against `target/classes` because parent-owned dirty tests block global testCompile.
  - Cloud main `mvn -q -DskipTests=false compile`: exit `0`.
- not yet complete:
  - mirror/run the dedicated validator test on Cloud;
  - run the final combined Client named suite, final Client compile and final Cloud compile;
  - compute shared protocol byte-identity SHA evidence and final exact file/SHA manifest;
  - append canonical `WHOLE-CARD SOURCE+TEST DELIVERED` with fresh-runtime gate.
- currently running/next verification: Cloud validator mirror alignment, then final named-suite,
  byte-identity and dual-compile pass.
- blocker: none in CR273. The known parent-owned Cloud global `testCompile` failures remain an
  external test-runner constraint only; isolated CR273 test compilation/execution is green.
- no runtime, UI, live capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: CR273 STATUS-PRODUCTION-REPAIR-WRITTEN-FINAL-VERIFY-IN-PROGRESS 2026-07-25 -->

## PARENT INCREMENTAL SOURCE REVIEW — 2026-07-25 / P1 exact PNG geometry

- reviewer: `Parent / sole final reviewer`
- result: `P0/P1/P2 = 0/1/0`; canonical delivery remains blocked.
- finding:
  - Client/Cloud `ObservationProtocolValidator.requireTerminalFrame(...)` validates the declared
    `1024x768` fields and the eight-byte PNG signature, but never validates the dimensions encoded
    by the PNG payload itself.
  - Cloud `DecisionEngine.npcArrivalFrameQueueResponse(...)` performs the same signature-only check
    for `REPLACE`, then constructs an `ObservationTerminalFrame` with fabricated
    `width=1024,height=768` regardless of the payload's real dimensions.
  - Therefore an `812x663` or otherwise wrong-size capture can pass the exact-frame contract and
    feed coordinate/click logic using false geometry. This violates Required Design A and repeats
    the already-observed wrong-HWND/partial-capture failure class.
- exact evidence:
  - Client and Cloud
    `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationProtocolValidator.java`,
    method `requireTerminalFrame(...)`.
  - Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java`, method
    `npcArrivalFrameQueueResponse(...)`, `REPLACE` branch.
- required repair:
  1. Validate the PNG payload's actual IHDR width and height as exactly `1024x768`; metadata alone
     is insufficient. Reject truncated/malformed/non-IHDR payloads fail-closed without decoding a
     large raster solely for validation.
  2. Apply the same check to both the observation terminal frame and replacement frame ingress.
  3. Keep mirrored shared protocol files byte-identical.
  4. Add focused positives for a real `1024x768` PNG and negatives for a valid-signature
     wrong-dimension PNG plus truncated/malformed IHDR on both ingress paths.
- verification gate: rerun the focused shared validator, replacement endpoint/store, combined Client
  CR273 suite and both compiles before canonical delivery.

<!-- TRUE_EOF: CR273 PARENT-REVIEW-P1-EXACT-PNG-GEOMETRY REPAIR-REQUIRED 2026-07-25 -->

## STATUS EVENT — 2026-07-25 / exact PNG geometry repair acknowledged

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- `ack_parent_review=P1-exact-png-geometry`
- canonical delivery remains stopped.
- only active repair scope:
  - shared Client/Cloud terminal-frame PNG structural validation of real IHDR `1024x768`,
    including truncated/malformed IHDR rejection while avoiding raster decode for validation;
  - Cloud REPLACE ingress reuse of the same strict validator;
  - focused ordinary-ingress and REPLACE-ingress positive/negative tests;
  - shared byte identity, focused suite and dual compile.
- old verification flow is stopped; no Maven/Java validation process is running at this event.
- blocker: none.
- no Git mutation, runtime, UI, live capture or physical input was performed.

<!-- TRUE_EOF: CR273 ACK-PARENT-P1-EXACT-PNG-GEOMETRY-REPAIR-IN-PROGRESS 2026-07-25 -->

## Canonical WHOLE-CARD SOURCE+TEST DELIVERED — 2026-07-25

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- state: `WHOLE-CARD SOURCE+TEST DELIVERED / PARENT REVIEW REQUIRED`
- this is source/test delivery only. It is not `SOURCE+TEST REVIEW PASSED` and does not self-assign
  `P0/P1/P2`.

### Exact delivered files

- Client protocol/Runner:
  `ObservationTerminalFrame.java`, `ObservationRequest.java`,
  `ObservationProtocolValidator.java`, `WindowObservationSampler.java`,
  `WindowObservationRunner.java`, `HttpsObservationClient.java`, and
  `WindowRuntimeContext.java`.
- Client FIFO transport/safety shell:
  `NpcClickSmartCloudSession.java`, `NpcClickSmartQueueMessage.java`,
  `NpcClickSmartQueueOutcome.java`, `TurnLocalOperation.java`,
  `TurnNpcArrivalFrameFifoSpec.java`, `TurnWholeTaskRuntimeArguments.java`,
  `TurnWholeTaskRuntimeResult.java`, `TurnProtocolValidator.java`, `TurnClient.java`,
  `HttpsTurnClient.java`, `LocalServiceStepDispatcher.java`,
  `WholeTaskRuntimeLocalOperationExecutor.java`, `NpcArrivalFrameFifoLocalExecutor.java`, and
  the touched verifier seam in `DialogService.java`.
- Cloud shared protocol and ingress/store:
  mirrored shared protocol files above; `CloudObservationHttpHandler.java`,
  `NpcClickSmartQueueStore.java`, `DecisionEngine.java`,
  `api/NpcArrivalFrameQueueEndpoint.java`, and `CloudBrainServer.java`.
- Cloud ownership/callsites:
  `NpcClickService.java`, `CloudWholeTaskReadyEventState.java`,
  `turn/client/CloudWholeTaskRuntimeLocalServiceClient.java`, `XiuluoTaskV2.java`, and
  `WubeiTask.java`.
- focused tests:
  both `ObservationTerminalFrameContractTest.java` copies;
  Client `WindowObservationTerminalFrameContractTest.java`,
  `NpcArrivalFrameFifoLocalExecutorContractTest.java`, and
  `TurnProtocolValidatorContractTest.java`;
  Cloud `CloudObservationContractTest.java`,
  `NpcArrivalFrameQueueStoreContractTest.java`,
  `NpcArrivalFrameTaskCallsiteContractTest.java`, and the mirrored validator contract.

### P1 exact PNG geometry repair

- shared validator now performs bounded structural PNG parsing without raster decode: exact
  signature; leading 13-byte `IHDR`; positive actual width/height exactly `1024x768`; chunk bounds
  and CRC; at least one `IDAT`; and terminal empty `IEND`.
- ordinary terminal-frame ingress and Cloud `REPLACE` both call the same shared validator.
  Wrong-size (`812x663`), truncated IHDR, non-IHDR/malformed first chunk and bad structural payloads
  fail closed before a frame/session can be prepared.
- repair SHA/mtime:
  - shared validator before P1 repair:
    `EFFE8BEDC357C0C207D887B5156BA652853644DDCB371A1F26FE1CD9823C4316`;
    after:
    `F2D4E67261A46AE8CE2AD93F93DAA423605892999333FEE2388E9209E0BB047E`;
    Client mtime `2026-07-25 03:28:17.747`, Cloud mtime
    `2026-07-25 03:29:26.911`.
  - mirrored geometry test before:
    `A1DAB99D365F1B887F289664775F52F2623183517635AE7044CA32C295B9FB88`;
    after:
    `FB91AC94F3FEB7A3F50F351E662349A326A9A1042F96797CFF5435FFE232FD4E`;
    Client/Cloud mtimes `2026-07-25 03:30:09.975` /
    `2026-07-25 03:30:57.020`.
  - Cloud `DecisionEngine.java` after:
    `046AAB7DE90D982FA7A4E4AB1C959D01A552A58FDB2825200AC30E53C24251B4`,
    mtime `2026-07-25 03:29:43.504`.
  - Cloud replacement test after:
    `BA2F866A46DD61AC5E8CD38AAB29F9C4A454FAE83D303C209C12784D6A38B8F9`,
    mtime `2026-07-25 03:32:56.750`.

### Shared protocol byte identity

- `ObservationProtocolValidator.java`:
  `F2D4E67261A46AE8CE2AD93F93DAA423605892999333FEE2388E9209E0BB047E`
- `ObservationRequest.java`:
  `B9FFF08DC6410C53C9C9C832A10CA220672800A67F02EA694370F59F9C2C33AA`
- `ObservationTerminalFrame.java`:
  `CDEC0918D2D7C65585860123F79334229140243C71A9997525D0BCF28F5FBF28`
- `TurnLocalOperation.java`:
  `19417BA28CE8B010EC6DD4364140CAA1A0876DC29941D88BD1B2821D3972C8F9`
- `TurnNpcArrivalFrameFifoSpec.java`:
  `6A34E7DD7EA20DAAC630B41F5BC9ABEB4A02D427F7AC1EDF2551FE6E1DFAE2D2`
- `TurnProtocolValidator.java`:
  `10D97469F49A7DD1C4568D8F823DFABB6CBCC227FB3BA4C9F0392376C05DE59F`
- `TurnWholeTaskRuntimeArguments.java`:
  `7196C6949B346E98E4B3CD527EAA303DC64369AC3A003E74F253543CE8206843`
- all seven comparisons: `Client SHA == Cloud SHA`, byte-identical.

### Verification

- Client combined named suite:
  `ObservationTerminalFrameContractTest` `8/8` +
  `WindowObservationTerminalFrameContractTest` `5/5` +
  `NpcArrivalFrameFifoLocalExecutorContractTest` `4/4` +
  `TurnProtocolValidatorContractTest` `21/21` =
  `38/38`, failures/errors/skipped `0/0/0`, Maven exit `0`.
- Cloud isolated exact-class compilation: `javac` exit `0`.
- Cloud isolated four focused classes:
  `ObservationTerminalFrameContractTest` +
  `NpcArrivalFrameQueueStoreContractTest` +
  `CloudObservationContractTest` +
  `NpcArrivalFrameTaskCallsiteContractTest` =
  `30/30`, failures `0`.
- dedicated Cloud production-format dual-run validator method:
  `1/1`, failures `0`.
- Client `mvn -q -DskipTests compile`: exit `0`.
- Cloud `mvn -q -DskipTests=false compile`: exit `0`.
- Cloud Maven named tests remain blocked only by the already-recorded unrelated parent-owned stale
  global `testCompile` sources; no CR273 source/test failure is hidden by that blocker.

### Contract proof and remaining gate

- same-frame Runner tests prove one full capture/upload per generation, coordinate-strip pixel
  derivation from that frame, ACK no-reupload, transport resend identity, and zero ARRIVED-triggered
  second capture.
- store/handler tests prove production starts before exact gate, pre-ARRIVED poll yields no candidate,
  byte-identical duplicate starts one producer, lower/duplicate facts do not gate, conflicting
  same-sequence rejects, and wrong run/task/frame/generation cannot unlock.
- Client safety-shell tests lock `12` candidates, exact stale outcomes, typed terminal outcomes,
  ordinary/Ctrl atomic safety, STORY boundary, one cleanup/fresh replacement and no third FIFO.
- callsite tests cover Xiuluo accept/heal/repair/combat and Wubei accept/maintenance/tracker combat;
  exact FIFO failure does not add an outer cleanup/click retry.
- fresh-runtime gate remains entirely with Parent: run real Xiuluo/Wubei targeted arrivals and inspect
  one-upload, exact ARRIVED, FIFO ordering, dialog verification and task keep-turn behavior. No
  runtime/UI/capture/input verification was performed by this Worker.
- no Git mutation was performed.

<!-- TRUE_EOF: CR273 CANONICAL-WHOLE-CARD-SOURCE-TEST-DELIVERED OWNER-WORKER-NOETHER-019f9799-96b7-7423-a589-8d569191046b 2026-07-25 -->

## PARENT INCREMENTAL SOURCE REVIEW — 2026-07-25 / P1 stop-restart cancellation

- reviewer: `Parent / sole final reviewer`
- result: `P0/P1/P2 = 0/1/0`; canonical delivery remains blocked.
- finding:
  - `NpcClickSmartQueueStore` cancels an arrival demand for accepted `REPLACED`,
    `STOPPED_AWAY`, and `CLEARED` pathing facts, but a direct task stop/observer shutdown only
    calls `CloudWholeTaskReadyEventState.unregisterObservationRun(...)`.
  - `unregisterObservationRun(...)` removes the observation bindings but does not cancel the
    matching observation run's arrival demands or prepared/unlocked FIFO sessions.
  - The new run cannot consume the old session because the exact observation/business-run
    identities differ, but the old demand/session remains pollable by an exact stale request and
    retains producer/session state after the task has stopped. This violates the card's explicit
    stop/restart cancellation boundary and is unsafe for a physical-input candidate channel.
- exact evidence:
  - Cloud
    `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java`,
    method `unregisterObservationRun(...)`.
  - Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java`:
    cancellation is currently reachable only through exact-intent `cancelArrivalDemand(...)`;
    there is no observation-run cancellation seam.
  - Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`:
    `finally` unregisters the observation run without cancelling its FIFO state.
- required repair:
  1. Add one bounded store operation that cancels all arrival demands and prepared sessions owned
     by the exact `tenant/device/window/hwnd/observationRunId` being unregistered.
  2. Invoke it from the existing observation-run unregister lifecycle; do not create a second
     lifecycle/coordinator and do not change task/NPC business logic.
  3. Preserve exact identity isolation: stopping one run/window must not cancel another run/window.
  4. Add focused tests proving stop/unregister removes both locked and unlocked sessions, stale
     polling fails closed, and a replacement run can register independently.
- verification gate: rerun the focused Cloud store/lifecycle tests, CR273 isolated Cloud family,
  Client focused family, and both compiles before canonical redelivery.

<!-- TRUE_EOF: CR273 PARENT-REVIEW-P1-STOP-RESTART-CANCELLATION REPAIR-REQUIRED 2026-07-25 -->

## STATUS EVENT — 2026-07-25 Parent Review P1 stop-restart cancellation repair started

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- ack_parent_review: `P1-stop-restart-cancellation`
- baseline audit: `NpcClickSmartQueueStore` arrival keys already retain exact `tenant/device/window/hwnd/observationRunId/intentId`; `cancelArrivalDemand` is intent-scoped only, while `CloudWholeTaskReadyEventState.unregisterObservationRun` removes only observation bindings. Therefore locked and unlocked sessions can survive observer shutdown and remain pollable.
- repair in progress: add one bounded exact-run Store cancellation over existing `arrivalDemands` plus its prepared/unlocked `sessions`; expose it through the existing `DecisionEngine` Store owner; invoke it from the existing `unregisterObservationRun` lifecycle seam before binding removal.
- expected production files: `NpcClickSmartQueueStore.java`, `DecisionEngine.java`, `CloudWholeTaskReadyEventState.java`; focused tests remain in the existing CR273 Cloud test family.
- contract blocker: none. No coordinator/second lifecycle, NPC algorithm, task business, runtime/UI/capture/input, or Git operation is involved.

## CANONICAL WHOLE-CARD SOURCE+TEST REDELIVERED — 2026-07-25 / Parent Review P1 stop-restart cancellation

- owner: `Worker Noether / 019f9799-96b7-7423-a589-8d569191046b`
- repair delivered:
  - `NpcClickSmartQueueStore.cancelArrivalRun(...)` performs one bounded scan and removes every
    demand matching exact `tenant/device/window/hwnd/observationRunId`, including its locked or
    unlocked prepared session.
  - cancellation marks the removed demand before clearing its session; a concurrent prepare checks
    that the demand remains current and not cancelled, so observer shutdown cannot leave an orphan
    pollable session.
  - the existing `DecisionEngine` Store owner exposes this one operation, and the existing
    `CloudWholeTaskReadyEventState.unregisterObservationRun(...)` lifecycle seam invokes it before
    removing the exact observation binding. No coordinator or second lifecycle was added.
- exact repair write set and SHA-256:
  - Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java`
    `909879907cf442c139bb78ac0f571583de3524be4443c9fecaac3b09f55fe56a`
  - Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java`
    `ef47a6db189c659d9ff2e042f2794ce8b8f8eb90249f8e58ba8784d4cc1e7ca3`
  - Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java`
    `8c55c76d69773bc9dfce0cf7fdff5d2a1b618e7c030481f7697c170ba94050d9`
  - Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/NpcArrivalFrameQueueStoreContractTest.java`
    `c4aa7fa2cee3aadfea58a2482ce22a6b4fd153df9b6baa280e615f633511a147`
- focused behavior evidence:
  - exact-run cancellation tests prove locked and unlocked sessions are removed, stale current poll
    returns fail-closed `INVALID`/`REQUIRED_FAILURE`, another window/run remains consumable, and a
    new observation run registers independently.
  - the lifecycle behavior test uses a production-format
    `remote-turn-x` / `remote-turn-x:0:XIULUO_V2` context and proves
    `unregisterObservationRun(...)` makes the old Store session unavailable.
  - Cloud isolated exact-class `javac`: exit `0`.
  - Cloud isolated CR273 four-class family:
    `ObservationTerminalFrameContractTest` +
    `NpcArrivalFrameQueueStoreContractTest` +
    `CloudObservationContractTest` +
    `NpcArrivalFrameTaskCallsiteContractTest` =
    `32/32`, failures `0`.
  - Client focused family remains the independently confirmed CR273 result:
    `ObservationTerminalFrameContractTest` +
    `WindowObservationTerminalFrameContractTest` +
    `NpcArrivalFrameFifoLocalExecutorContractTest` +
    `TurnProtocolValidatorContractTest` =
    `38/38`, failures/errors/skipped `0/0/0`, exit `0`; this repair changed no Client source.
- compile evidence:
  - Cloud `mvn -q -DskipTests=false compile`: exit `0` after this repair.
  - Client `mvn -q -DskipTests compile`: previously and independently confirmed exit `0`; this
    repair changed no Client source.
  - Cloud global Maven named-test path remains blocked only by the already recorded unrelated stale
    parent-owned global `testCompile`; the isolated focused family above compiled and ran directly.
- shared protocol identity:
  - Client SHA-256:
    `f2d4e67261a46ae8ce2ad93f93daa423605892999333fee2388e9209e0bb047e`
  - Cloud SHA-256:
    `f2d4e67261a46ae8ce2ad93f93daa423605892999333fee2388e9209e0bb047e`
  - byte-identical: `true`.
- fresh-runtime acceptance remains a Parent gate: stop/unregister an observation run with locked and
  unlocked arrival state, verify its stale poll fails closed, then start a fresh run on the same
  window and verify its independent demand/frame/ARRIVED/FIFO path. No runtime/UI/capture/input was
  executed by this Worker.
- no Git mutation was performed. This is source+test redelivery only and is not a self-approved
  `PASSED`.

<!-- TRUE_EOF: CR273 CANONICAL-WHOLE-CARD-SOURCE-TEST-REDELIVERED-STOP-RESTART-CANCELLATION 2026-07-25 -->

## PARENT SOURCE+TEST SOURCE REVIEW PASSED — 2026-07-25

- reviewer: `Parent / sole final reviewer`
- final finding count: `P0/P1/P2 = 0/0/0`.
- reviewed result:
  - one Runner stable full frame is the sole source for the coordinate strip and Cloud NPC
    SmartClick FIFO preparation; exact Client `ARRIVED(frameId,generation)` is the only unlock.
  - Client consumes the existing SmartClick candidate order and atomic input semantics. The first
    whole FIFO failure permits exactly one `cleanupAll`, one replacement frame/session, and no
    third attempt.
  - exact observation-run unregister now cancels locked and unlocked arrival demands/sessions;
    stale polling fails closed while other windows/runs and a fresh replacement run remain isolated.
  - the actual PNG payload is structurally validated as exact `1024x768` on both ordinary and
    replacement ingress; shared protocol files remain byte-identical.
- Parent independent verification:
  - Client focused family:
    `ObservationTerminalFrameContractTest`,
    `WindowObservationTerminalFrameContractTest`,
    `NpcArrivalFrameFifoLocalExecutorContractTest`,
    `TurnProtocolValidatorContractTest`, and
    `HttpsTurnClientContractTest` = `41/41`, exit `0`.
  - Cloud isolated focused family:
    `ObservationTerminalFrameContractTest`,
    `NpcArrivalFrameQueueStoreContractTest`,
    `CloudObservationContractTest`, and
    `NpcArrivalFrameTaskCallsiteContractTest` = `32/32`, failures `0`.
  - Client compile exit `0`; Cloud compile exit `0`.
  - Cloud full Maven test path remains blocked at global `testCompile` by pre-existing stale
    unrelated fixtures; CR273 focused sources compile and execute independently.
- status: `SOURCE+TEST REVIEW PASSED / OWNER RELEASED / FRESH RUNTIME REQUIRED`.
- fresh-runtime acceptance is still required and was not performed here: restart both Client and
  Cloud, verify one arrival upload, exact ARRIVED unlock, FIFO ordering, one replacement maximum,
  task-turn retention, and stop/restart stale-poll rejection.
- no runtime, UI, live capture, physical input, or Git mutation was performed.

<!-- TRUE_EOF: CR273 SOURCE-TEST-SOURCE-REVIEW-PASSED P0-P1-P2-0-0-0 OWNER-RELEASED FRESH-RUNTIME-REQUIRED 2026-07-25 -->

## PARENT FRESH-RUNTIME REPAIR + INCREMENTAL SOURCE REVIEW — 2026-07-25 / direct SmartClick FIFO

- reviewer/implementer: `Parent / sole final reviewer`.
- fresh evidence:
  - `14:06:15.008` ordinary/near-NPC `clickNpcSmart` captured its frame.
  - first physical memory click did not occur until `14:06:54.633`, about `39.6s` later.
  - this run emitted no CR273 demand/frame/ARRIVED/open/poll chain because
    `runSingleFrameNpcClickPlan(...)` still called
    `prepareNpcClickPlanFromObservation(...)` and waited for complete
    `MEMORY -> TOOLTIP -> YELLOW -> CTRL -> END` materialization.
- user-approved repair boundary:
  - do not modify `SmartClickRecognizer`, candidate order, templates, thresholds, click coordinates,
    verifier semantics, or `NpcClickSmart` fallback policy.
  - ordinary/direct SmartClick must use the same existing producer/store/Client FIFO consumer as
    arrival-frame SmartClick; direct fresh frames unlock immediately because no pathing verdict is
    pending.
- delivered implementation:
  - `DecisionEngine.prepareImmediateNpcFrame(...)` uses the existing exact demand registration,
    existing `prepareNpcArrivalFrame(...)` producer, and existing exact FIFO unlock.
  - `NpcClickService.runSingleFrameNpcClickPlan(...)` preserves the existing `Alt+4`, one fresh
    `1024x768` capture and expected-dialog short circuit, then delegates candidate execution to
    `clickNpcArrivalFrameFifo(...)`.
  - the old complete-plan APIs remain only for separately owned 五环 prepared flow; the ordinary
    SmartClick production path no longer calls them.
- Parent incremental review: `P0/P1/P2 = 0/0/0`.
- verification:
  - Cloud `mvn -q -DskipTests=false compile`: exit `0`.
  - Client `mvn -q -DskipTests compile`: exit `0`; no Client source changed.
  - isolated `NpcArrivalFrameQueueStoreContractTest` +
    `NpcArrivalFrameTaskCallsiteContractTest`: `10/10`, failures `0`.
  - Cloud global Maven named-test path remains blocked at testCompile by pre-existing stale
    observer/pathing fixtures; production compile and the exact isolated family above are green.
  - touched-file `git diff --check`: exit `0`.
- status: `SOURCE+TEST INCREMENTAL REVIEW PASSED / P0-P1-P2=0/0/0 /
  FRESH RUNTIME REQUIRED`.
- fresh gate: restart both processes and run one ordinary/near-NPC click. Require direct FIFO
  start/open/poll evidence and first available memory/tooltip candidate execution without waiting
  for yellow OCR completion.
- no runtime, UI, capture, physical input, or Git mutation was performed.

<!-- TRUE_EOF: CR273 DIRECT-SMARTCLICK-FIFO-INCREMENTAL-REVIEW-PASSED P0-P1-P2-0-0-0 FRESH-RUNTIME-REQUIRED 2026-07-25 -->

## USER PARAMETER DECISION — 2026-07-25 / FIFO explicit-WAIT retry

- Client `NpcArrivalFrameFifoLocalExecutor.WAIT_SLEEP_MS` changed from `100ms` to `500ms`.
- This delay applies only after Cloud explicitly returns `WAIT`; it does not batch candidates and
  does not delay a currently blocked Cloud queue poll, which is awakened immediately by
  `queue.offer(...)`.
- Cloud still publishes each `MEMORY / TOOLTIP / YELLOW / PURPLE / CTRL` result immediately after
  that stage completes, and Client still executes each received candidate before polling the next.
- verification: `NpcArrivalFrameFifoLocalExecutorContractTest` Maven test exit `0`.
- fresh-runtime gate remains required; no runtime/UI/capture/input or Git mutation was performed.

<!-- TRUE_EOF: CR273 USER-PARAMETER-WAIT-RETRY-500MS CLIENT-FOCUSED-TEST-0 FRESH-RUNTIME-REQUIRED 2026-07-25 -->

## PARENT FRESH-RUNTIME DTO REPAIR — 2026-07-25 / strict FIFO JSON

- fresh evidence:
  - Cloud correctly produced tooltip candidate `windowRelativeClickPoint=(452,277)` from the
    exact arrival frame, but no Client physical click followed.
  - Client OPEN/POLL failed before execution with Jackson `InvalidDefinitionException` because
    `NpcClickSmartCloudSession` / `NpcClickSmartQueueMessage` had Lombok builders but no JSON
    creator.
  - the queue message also exposed two strict-wire hazards: `java.awt.Point` serialized numeric
    coordinates as floating values while Client disables float-to-int coercion, and derived
    `ordinaryClickCandidate` / `hasClickPoint` properties conflicted with unknown-property failure.
- parent repair:
  - both shared DTOs now use explicit `@JsonCreator` / `@JsonProperty` constructors while
    preserving their existing builder API and default status/type/list behavior.
  - queue points use one shared strict serializer/deserializer contract: exactly integer `x` and
    `y`, including `ctrlProbePoints`; malformed, additional, missing, floating, or out-of-int-range
    point fields fail closed.
  - derived local helper methods are `@JsonIgnore`; no candidate generation, candidate ordering,
    coordinates, FIFO policy, SmartClick recognition, cleanup, or physical-input behavior changed.
- shared-source identity:
  - `NpcClickSmartCloudSession.java`: Client/Cloud byte-identical.
  - `NpcClickSmartQueueMessage.java`: Client/Cloud byte-identical.
- verification:
  - Client `mvn -q -DskipTests compile`: exit `0`.
  - Cloud `mvn -q -DskipTests=false compile`: exit `0`.
  - current no-local-test policy was honored; no test, runtime, UI, capture, or input was run.
- status: `SOURCE COMPILE REVIEW PASSED / FRESH RUNTIME REQUIRED`.
- fresh gate: restart both processes and repeat one NPC SmartClick. OPEN/POLL must deserialize
  without creator/coercion/unknown-property errors, and the first Cloud candidate must reach the
  Client FIFO executor. Runtime acceptance remains open until that evidence exists.

<!-- TRUE_EOF: CR273 STRICT-FIFO-JSON-DTO-REPAIR COMPILE-PASSED FRESH-RUNTIME-REQUIRED 2026-07-25 -->

## PARENT FRESH-RUNTIME COMMON-CONSUMER REPAIR — 2026-07-25

- fresh evidence after the strict DTO repair:
  - the exact frame independently produces a valid `TOOLTIP` candidate at window-relative
    `(390,200)` with score `0.9996389746665955`;
  - the complete producer emits `MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA ->
    CTRL_CANDIDATES -> END`;
  - Client emitted no `npcClick:fifoCandidate:*` physical-input request, while Cloud immediately
    received `verified=false`. Therefore the common Client consumer rejected before candidate
    submission; this is not a tooltip/template/yellow-recognition miss.
- parent repair:
  - `NpcArrivalFrameFifoLocalExecutor` now records every common boundary: exact runtime/spec
    binding, OPEN response, POLL transport, message identity/type/point, safety decision, physical
    submission, verifier outcome, and asynchronous REPORT failure.
  - all five candidate types continue through the same consumer. No per-tooltip bypass or local
    recognition fallback was added.
  - exact identities are compared after trimming; HWND identity additionally accepts equivalent
    decimal, `0x` hexadecimal, and `hwnd-` hexadecimal representations. This prevents one native
    window from being rejected before OPEN solely because its handle crossed the wire in a
    different canonical text form.
- unchanged behavior:
  - no changes to `SmartClickRecognizer`, template assets, thresholds, MEMORY/TOOLTIP/YELLOW/
    PURPLE/CTRL order, click coordinates, verifier policy, retry count, cleanup policy, or task
    phase semantics.
  - `docs/业务逻辑.md` NPC Click FIFO rules were checked; no approved business difference.
- verification:
  - Client `mvn -q -DskipTests compile`: exit `0`.
  - no runtime, UI, capture, input, or automated test was run.
- fresh gate:
  - restart Client and repeat one NPC SmartClick. The log must show either
    `NPC arrival FIFO submitting candidate` and the physical request, or one precise common
    rejection reason. Runtime acceptance remains open until a candidate reaches input.

<!-- TRUE_EOF: CR273 COMMON-CONSUMER-IDENTITY-REPAIR CLIENT-COMPILE-0 FRESH-RUNTIME-REQUIRED 2026-07-25 -->

## PARENT REAL-FRAME END-TO-END TEST — 2026-07-25

- user-authorized test scope:
  - no runtime, UI, live capture, or physical input;
  - replay the exact incident PNG through the formal Cloud producer;
  - feed the exact emitted wire JSON into the formal Client FIFO consumer with a fake input queue.
- Cloud result:
  - input:
    `images/captures/20260725/unbound_15999382/20260725_163420_010_163_HWND_PRINTWINDOW_167_45_1024x768.png`;
  - formal order:
    `MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES -> END`;
  - tooltip box `(374,192,32,16)`, relative click `(390,200)`, confidence
    `0.9996389746665955`;
  - test: `NpcArrivalFrameRealImagePipelineTest`, isolated compile/run exit `0`.
- exact wire handoff result:
  - the Client test consumed the raw Cloud-generated JSON, including the Cloud-generated random
    session id;
  - decimal HWND `15999382` matched the bound `hwnd-F42196`;
  - Client submitted exactly one atomic
    `MOVE_MOUSE -> SLEEP -> CLICK_LEFT -> SLEEP` sequence;
  - window origin `(167,45)` produced absolute click `(557,245)`;
  - report outcome was `VERIFIED`;
  - test: `NpcArrivalFrameFifoRealCandidateTest`, Maven exit `0`.
- compile:
  - Client `mvn -q -DskipTests compile`: exit `0`;
  - Cloud `mvn -q -DskipTests=false compile`: exit `0`.
- unrelated repository gate:
  - Cloud full `testCompile` is currently red before this named test can run under normal Surefire,
    due to existing stale tests calling the removed no-arg `CloudWholeTaskReadyEventState()`,
    removed `probeTypedPathing(...)`, and an outdated `NavigationService` constructor.
  - the real-frame Cloud test was therefore compiled and run in isolation against the current
    production classes and Maven test dependency classpath.
- conclusion:
  - offline `real PNG -> Cloud producer -> exact JSON -> Client consumer -> fake atomic input`
    is proven green;
  - runtime acceptance is still open until a restarted Client reaches the physical input queue.

<!-- TRUE_EOF: CR273 REAL-FRAME-END-TO-END GREEN CLIENT-TEST-0 CLOUD-ISOLATED-TEST-0 COMPILE-0 FRESH-RUNTIME-REQUIRED 2026-07-25 -->

## FRESH-RUNTIME FAILURE / TEST-GAP REPAIR — 2026-07-25 20:28

- actual leader run `hwnd-147181C` reached the NPC arrival FIFO three times, but each attempt did:
  `OPEN -> MEMORY parsed/SKIPPED -> next POLL RESPONSE_PARSE`.
- no candidate reached `npcClick:fifoCandidate:*`; therefore the accept-NPC click was never
  submitted and the task could not accept the quest.
- the previous real-frame test was insufficient because it passed only the `TOOLTIP` wire JSON
  into the Client, not every Cloud FIFO message.
- repaired test gate:
  - Cloud now exports every raw message in exact order;
  - Client strictly parses and asserts all six:
    `MEMORY / TOOLTIP / YELLOW_NAME / PURPLE_FORMULA / CTRL_CANDIDATES / END`;
  - it then feeds the exact Cloud `TOOLTIP` into the formal consumer and requires one atomic input
    bundle plus `VERIFIED`;
  - cross-repository named test exit `0`.
- diagnostics:
  - `HttpsTurnClient.pollNpcArrivalFrame(...)` now includes the bounded raw response and exact
    Jackson cause in `RESPONSE_PARSE`; no retry, DTO, candidate, or business semantics changed.
- process evidence:
  - the failed runtime ended at `20:28`;
  - the listening Cloud Java process was replaced at `20:29`, after that failed run.
- verification:
  - Client compile exit `0`;
  - Client named test exit `0`;
  - no runtime/UI/capture/physical input was initiated by the parent.
- status remains `FRESH-RUNTIME REQUIRED`; the 20:28 run is explicitly rejected as acceptance.

<!-- TRUE_EOF: CR273 FRESH-RUNTIME-FAILED SECOND-POLL-RESPONSE-PARSE ALL-SIX-WIRE-TEST-0 CLIENT-COMPILE-0 2026-07-25 -->

## PARENT ROOT-CAUSE REPAIR — 2026-07-25 20:39

- fresh reproduction exposed the exact hidden cause:
  `Trailing token (FIELD_NAME) found after value ... FAIL_ON_TRAILING_TOKENS`.
- why the previous test was falsely green:
  - production `HttpsTurnClient` enables `FAIL_ON_TRAILING_TOKENS`;
  - the test mapper did not;
  - shared `IntegerPointDeserializer` called nested `readTree()`, so production Jackson treated
    the next valid DTO field after Point as a trailing token.
- repair:
  - both repositories now use a byte-identical token-stream Point deserializer;
  - it accepts exactly one integral `x` and one integral `y` within int range;
  - unknown, duplicate, missing, floating, and overflowing values remain fail-closed;
  - no candidate generation, ordering, coordinate, input, retry, cleanup, or task phase changed.
- test repair:
  - Client test mapper now enables the production `FAIL_ON_TRAILING_TOKENS` setting;
  - all six raw Cloud messages are strictly parsed before the formal Client consumer runs;
  - exact Cloud JSON handoff and named Client test exit `0`.
- verification:
  - shared source SHA256:
    `6FC1E4E5869786A870AA69C3C506D61C28DB90F4FE716762C7EAAA47C61839C0`;
  - Client named test: exit `0`;
  - Client compile: exit `0`;
  - Cloud compile: exit `0`;
  - isolated Cloud producer plus exact Client wire handoff: exit `0`.
- fresh gate: restart both Client and Cloud. A valid Point-bearing second message must parse and
  reach `NPC arrival FIFO submitting candidate`. No runtime acceptance is claimed yet.

<!-- TRUE_EOF: CR273 POINT-DESERIALIZER-TRAILING-TOKEN-ROOT-REPAIRED STRICT-PRODUCTION-MAPPER-TEST-0 COMPILE-0 FRESH-RUNTIME-REQUIRED 2026-07-25 -->
