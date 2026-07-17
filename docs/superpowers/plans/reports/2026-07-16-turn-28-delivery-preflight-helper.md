# CR271 TURN-28 delivery preflight helper

## Role and snapshot

- Role: CR271 Internal helper for External B / TURN-28 delivery preflight.
- This helper is **not** the implementation owner, **not** an independent reviewer, and **cannot** write
  `APPROVED`, `SOURCE REVIEW PASSED`, `CARD APPROVED`, or close any card.
- Snapshot time: `2026-07-16T08:52:49.685-04:00`.
- Read authority: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current CR271 block at the top of
  `docs/ACTIVE_WORK.md`, plan sections 14-19, the HTTPS turn protocol specification,
  `docs/业务逻辑.md`, the physical true EOF of TURN-28/TURN-28S1/TURN-28P/TURN-28Q/TURN-22,
  the `696a12b0` NpcClick mirror, and the current Cloud production/test paths.
- Scope of this report: a non-binding delivery checklist and false-proof risk list. It changes no Java, card,
  plan, ACTIVE_WORK, matrix, dashboard, or other report.

## Physical-EOF ownership truth

| Item | Physical true EOF at this snapshot | Preflight meaning |
|---|---|---|
| `TURN-28` | `PARENT DECOMPOSED TURN-28S1 ... WHOLE-CARD-NO-OWNER` at `08:42:21.828` | The original four-file card has no whole-card owner. Later migration must proceed through mutually exclusive slices. |
| `TURN-28S1` | `EXTERNAL-B SOURCE DELIVERED` at `08:51:38` | External B delivered only the one-file pending-proof cleanup. This is delivery material awaiting parent source review, not an approval. |
| `TURN-28P` | Parent Review #4: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING` | Its public exact-window probe mechanics may be treated as source-reviewed, but build/card approval is still pending. |
| `TURN-28Q` | Parent Review #1: `P0/P1/P2=0/3/1 / REPAIR #1 REQUIRED` | Frozen complete-action-list mechanics are not final. TURN-28 must not guess or locally replace this API. |
| `TURN-22` | `P0/P1/P2=0/1/0 / BLOCKED-BY-TURN-28Q / OWNER RELEASED` | The caller integration remains a final gate; it does not authorize a second queue path in TURN-28. |

The current `TURN-28S1` delivery changed Cloud `NpcClickService.java` from 3406 lines / `f4e3842c...`
to 3374 lines / `cce8f020...`. The delivered SHA is byte-identical to the read-only
`migration-baseline/696a12b0/.../NpcClickService.java` mirror. This observation is useful evidence only;
the parent must still review the actual diff and delivery card.

## Current Cloud source snapshot

| Path | Current state | Delivery-preflight consequence |
|---|---|---|
| `src/main/java/com/bot/dhxy/service/NpcClickService.java` | 3374 lines; SHA `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` | S1 pending-proof cleanup is present. The class still uses local `InputSequences`, `tracker` capture and local business collaborators; the HTTPS turn cutover has not yet been delivered. |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java` | 914 lines; SHA `d3dc3cc247058ae85a6258e6173f8d9b56d7be119443c90a24c4bf6f180f3fe1` | Unchanged reservation file. Zero diff remains legal under the frozen conditions below. |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java` | 3026 lines; SHA `ffbd984a4ed5841ccba6b87bf3378a1e0cb1e7d2bea68be3eed656be7324f102` | Still exposes legacy `JsonNode`/Base64 and session queue-message paths. The new NpcClick path must call those paths zero times and use only a minimal typed image facade. |
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java` | Missing | The required single named contract test has not yet been delivered. |

## A. Strict-696 conditional FIFO and business order

The later TURN-28 slices and final named test must prove every item below through real `NpcClickService`
production behavior. The labels are semantic FIFO labels; they must not become a runtime queue/session/poller.

| ID | Required invariant | Evidence the final delivery must expose |
|---|---|---|
| `F-01` | One `clickNpcSmart` attempt starts with exactly one full expected-dialog pipeline. | One public invocation trace with no duplicated initial pipeline. |
| `F-02` | Verified candidate short-circuits immediately. Mechanical click completion, OCR/template hit, pixel change, or bare Alt+A is not success. | No later strategy/action after the verifier returns one of the permitted business-success statuses. |
| `F-03` | STOP/interruption/fatal/correlation/uncertain aborts, with zero later candidate, click, verifier, or memory commit. | Scripted terminal cases with exact post-abort command count `0`. |
| `F-04` | `COMBAT_TARGET` has no generic retry. Every other target has exactly one new `Alt+C + WAIT 700` action followed by one second full pipeline; never a third. | Separate combat/non-combat call-count and order assertions. |
| `F-05` | One ordinary pipeline keeps the exact conditional order: first dialog gate and applicable early memory; one `Alt+4 + WAIT 400`; Wubei tooltip-first; main dialog gate with one STORY handling/re-detect and OPTION block; late memory; non-Wubei tooltip; post-tooltip dialog gate; `TENTATIVE` cutoff; yellow; purple formula; Ctrl. | Stage trace from the real service, including skipped conditional stages without reordering. |
| `F-06` | External semantic labels remain `MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES -> END`. | Ordered diagnostic/result evidence only; no session id, queue store, polling, outcome reporter, or durable state. |
| `F-07` | Formula miss immediately performs SMALL_RING Ctrl around that formula point; final Ctrl may probe that point again. | Two distinct stage/action traces; no cross-stage dedup. |
| `F-08` | Direct combat remains strict 696: null/STOP gate; FLYING one Alt+C/700; UNKNOWN skip; grounded continue; one Alt+A/350; same candidate pipeline without a second Alt+4/dialog pre-gate; only BattleRadar closes combat. | Branch-specific action and verifier counts. |
| `F-09` | Direct-combat non-stop miss exits at most three times using purple/player anchor or window-relative `(512,424)`. Each attempt is `MOVE -> WAIT120 -> CLICK_RIGHT(delay=120, hold=600)`, then mode probe; WAIT300 only when another attempt will occur. Three unconfirmed exits throw. | Exact three-attempt ceiling and no trailing WAIT300 after the final attempt. |

## B. OCR, template, click budget, and verifier contract

| ID | Strict contract | Forbidden weakening |
|---|---|---|
| `V-01` | Learned memory: one click, first hold `1200`, no retry. | A second memory click or treating memory lookup as success. |
| `V-02` | Tooltip: threshold `.82`, dedup distance `36px`, provider hit order, one click with hold `1200`, no retry. | Sorting candidates differently, accepting a hit without dialog verification, or calling the old full recognizer route. |
| `V-03` | Yellow target: provider word center, final Y offset `-50`, first hold `800`, exactly one baseline retry with hold `1000` and a fresh verifier. | Local/DHXY OCR, extra region expansion after an exact target click fails verification, or more than one retry. |
| `V-04` | Purple formula: `UX=20, UY=0, VX=0, VY=-20`, final Y `-50`, hold `1500`, no click retry, plus the baseline extra `1500` miss wait. Purple wash remains the existing `ImageAlgorithms.wash(..., "WASH_PURPLE")` behavior. | New purple thresholds/geometry, local OCR, an omitted miss wait, or a second formula click. |
| `V-05` | Ctrl menu OCR uses provider order and the first short-name match; the historical npc-tag shortcut remains inactive as a decision path. The frozen regex is `(?i).*(NPC|IPC|PC|NP).*`; it must not become a local/template business success. First click hold `800`, exactly one retry hold `1000`. | Reordering OCR words, reviving `npc_tag` as an active shortcut, or treating changed pixels as a menu match. |
| `V-06` | Dialog verifier performs one TURN-26 read and accepts only `OPTION_VISIBLE` or `GREEN_TEMPLATE_VISIBLE`. | Accepting capture success, generic dialog presence, STORY, or any terminal transport status. |
| `V-07` | Combat verifier performs at most four BattleRadar reads and, for four known false results, exactly four `350ms` waits including the wait after the fourth false. | Three waits, a fifth read, or success from Alt+A/click completion. |
| `V-08` | Cloud owns OCR/template/formula/menu decisions over the raw PNG returned by the action. One action returns at most one raw PNG. | Base64, temp-file OCR, DHXY/local business classification, stale shared frames, or an extra capture/read. |

## C. Ctrl profiles and probe mechanics

The profile arrays are ordered contracts, not sets:

| Profile | Exact ordered offsets |
|---|---|
| `DIRECT` | `(0,0)` |
| `SMALL_RING` | `(0,0)`, `(8,-8)`, `(8,0)`, `(0,-8)`, `(-8,0)`, `(0,8)`, `(-8,-8)`, `(-8,8)`, `(8,8)` |
| `FULL_RING` | `(0,0)`, `(16,-16)`, `(16,0)`, `(0,-16)`, `(8,-8)`, `(8,0)`, `(0,-8)`, `(16,16)`, `(0,16)`, `(-16,0)`, `(-16,-16)`, `(-8,-8)`, `(-8,0)`, `(-16,16)`, `(-8,8)`, `(8,8)`, `(0,8)` |

Additional probe invariants:

- Keep 3px same-origin dedup, the 15px non-combat formula-reference filter, exact-window clamp, and no window-center fallback.
- Each probe is one `CAPTURE` action using latest exact-window ROI `x +/-150, y +/-120`, `UPLOAD_IMAGE`,
  `clearPointerIfOverRegion=null`, and `pixelChangeProbe=(x,y,80,280,100,0.05)`.
- The local `before` frame never leaves the client. Only the sole `after` raw PNG may reach Cloud.
- `changed` permits Cloud menu OCR; `unchanged` advances to the next ordered probe. Ctrl release failure,
  mechanics failure, STOP, uncertainty, or correlation mismatch must abort and must never be normalized to
  `unchanged` or a business miss.
- The menu click is a new action after Ctrl has been released. It is not nested in the probe action and does not
  reuse the probe UUID.

## D. One action, one UUID, and one command

| ID | Required boundary |
|---|---|
| `A-01` | Before every command, call `TaskCheckpoint` directly, resolve exactly one `TurnInvocationContext`, bind one client, read latest metadata, reject STOP before UUID creation, and validate exact device/window/HWND/process/latest rect. |
| `A-02` | One public client call creates one fresh UUID and sends one command. No transport retry, replay, duplicate submission, or UUID reuse. |
| `A-03` | Every left click is one HTTPS action and one mouse-queue submission with mechanics `MOVE -> WAIT150 -> CLICK_LEFT(delay=150, queueHold=firstWaitMs)`. |
| `A-04` | A 696 business retry is a new action with a new UUID, hold `1000`, and one new verifier. It is not a transport retry of the prior command. |
| `A-05` | Alt+A, Alt+C, and Alt+4 use exact-HWND background key support. No foreground keyboard fallback. |
| `A-06` | One action has at most one outcome image slot/raw PNG; no action may borrow a prior action's frame. |

## E. Terminal and correlation fences

- Only a strictly correlated `COMPLETED` result may be interpreted further, and even then only as the intended
  business result or business miss. `COMPLETED` alone is never business success.
- `FAILED`, `STOPPED`, `DUPLICATE_OR_UNCERTAIN`, timeout/interruption, Ctrl release failure, missing frame,
  wrong action/step, nonterminal step, or any device/window/HWND/process/rect/UUID/correlation mismatch aborts.
- After an abort, command/candidate/click/verifier/memory counts must remain zero for all later stages.
- Correlation must be checked against the exact request and latest invocation metadata, not merely against a
  non-null response UUID or a matching device id.
- A business `NOT_FOUND`/miss may advance FIFO only when it comes from a strictly correlated, terminally
  `COMPLETED` action with the expected frame/action/step shape.

## F. `sourceTask` delta prohibitions

The `TURN-28S1` delivery is intended to remove one unapproved post-696 gate. Final review must distinguish that
gate from legitimate request-level business branches.

| Required preservation/removal | Preflight check |
|---|---|
| Remove `PendingSmartClickEvidence.sourceTask`, its constructor/from wiring, `matchesSourceTask(...)`, `normalizeSourceTask(...)`, and the early source-task rejection in `confirmExpectedOptionProof(...)`. | Current delivered source has zero occurrences of those three pending-evidence symbols and is byte-identical to the 696 mirror. Parent must still inspect the actual diff. |
| Preserve `confirmExpectedOptionProof(String sourceTask, ...)` signature and callers. | `sourceTask` may remain diagnostic data after proof-token/option checks; it must not regain decision authority. |
| Preserve every `request.sourceTask()` business branch, especially Wubei tooltip-first and non-Wubei conditional stages. | Do not globally delete or normalize the same-named request field. |
| Preserve exact pending key, window/token/map/name/coordinates, proof token, expected action/text match, remove-on-option-mismatch, and confirmed-memory commit fences. | Removing the extra sourceTask equality must not loosen any baseline fence. |
| Add no substitute gate. | No new sourceTask-derived session key, owner, permit, TTL, ledger, cleanup, retry, caller phase, or durable workflow. |

## G. `ObjectiveTextRecognizer` zero-diff legality

Zero production diff in `ObjectiveTextRecognizer.java` is legal only under all of these conditions:

- TURN-28 does not need to change its existing map/coordinate ownership or semantics. Existing package-level
  `coordinatePlausible(...)` and `mapTransform(...)` remain the single map-transform snapshot used by their
  existing callers.
- The new NpcClick path does not call legacy `recognize(JsonNode)`, because that entry decodes
  `imagePayloadBase64`; the TURN-28 path is raw-PNG/typed-image only.
- No map transform, plausibility rule, template loader, or coordinate repair is copied into `NpcClickService` or
  `SmartClickRecognizer` merely to avoid using the existing owner.
- No visibility widening, wrapper, or unrelated formatting is introduced just to make the reserved file appear
  touched. If no genuine pure API reuse is needed, leaving the file at SHA `d3dc3cc2...` is the correct result.
- Compatibility evidence must be behavioral through an existing public/owned route; a SHA assertion, source
  string scan, or reflection-only test is not proof of compatibility.

## H. Named-test false-proof risks

The one allowed test path is
`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java`. It is currently missing.
The eventual test must drive real `NpcClickService` production with a scripted `TurnGameClient` and in-memory
PNGs. The following patterns would create false confidence:

| False-proof pattern | Required counter-evidence |
|---|---|
| Calling `SmartClickRecognizer` or a new helper directly and never entering public `NpcClickService`. | Invoke the real public service path and observe its client commands, stage order, verifier calls, and memory effects. |
| Stubbing a recognizer result or manually constructing a click point. | Feed in-memory PNGs through the real typed image facade and actual OCR/template/formula/menu logic. |
| A scripted client that returns `COMPLETED` for every request without validating UUID, metadata, action index, step status, and frame shape. | Make the script reject mismatches and record every request/result correlation field. |
| Reusing a UUID or counting only method calls. | Assert a distinct UUID per public client call and one command per UUID, including business retries. |
| Returning `changed`/`unchanged` directly without exercising the one CAPTURE probe result and sole after PNG. | Record exact probe payload, one capture action, one after frame, Ctrl release outcome, and the subsequent separate menu-click action. |
| Sharing one PNG/result between multiple FIFO stages. | Supply action-specific frames and prove no stale base-frame reuse. |
| Testing only success paths. | Cover STOP, failure, duplicate/uncertain, timeout, interrupted, release failure, correlation mismatch, missing/wrong frame/action/step, and assert zero later commands. |
| Asserting only final boolean success/failure. | Assert exact conditional FIFO, per-strategy budgets, verifier counts, action parameters, and no third generic pipeline. |
| Treating an OCR/template hit, pixel change, click completion, or Alt+A completion as success. | Require the exact dialog statuses or BattleRadar result frozen by the card. |
| Proving the old queue path instead of its absence. | Instrument zero calls to legacy `recognizeQueueMessages`, `produceQueueMessages`, queue store, session/poller, macro, and full-fallback routes. |
| Using source-string guards, reflection over private helpers, or SHA-only assertions. | Use production behavior and public/owned boundaries; SHA is supporting evidence only. |
| Testing S1 by symbol count alone. | Exercise proof-token/option acceptance with differing diagnostic `sourceTask`, and separately prove wrong window/token/map/name/coordinates/option still cannot commit memory. |
| Claiming Objective compatibility because the file was untouched. | Exercise the existing owner route that depends on its map/coordinate contract, without invoking Base64 from the new NpcClick path. |

## Non-binding delivery stop list

The parent reviewer should investigate before accepting any later TURN-28 slice if it finds any of the following:

- A call from the new NpcClick path to legacy JsonNode/Base64/session/queue-store/macro/full-fallback APIs.
- Local/DHXY OCR or business classification, temp-file OCR, extra capture/read/retry/cleanup, or a new caller phase.
- Any FIFO reorder, altered 696 constant/budget, active npc-tag shortcut, center fallback, or cross-stage Ctrl dedup.
- Any terminal/uncertain/correlation failure normalized to a business miss or success.
- Any command replay, UUID reuse, later action after abort, or click split across multiple mouse-queue submissions.
- Any reintroduction of pending-evidence `sourceTask` equality or removal of request-level Wubei behavior.
- A named test that bypasses the real service/typed client/image path or cannot distinguish a correlated miss from
  transport uncertainty.

This helper did not run Maven, JUnit, compile, package, runtime, application, server, Task, UI, capture, or input.
It performed no Git mutation and made no approval decision.

PRECHECK_COMPLETE

<!-- TRUE_EOF: TURN-28 DELIVERY-PREFLIGHT-HELPER PRECHECK_COMPLETE NON-REVIEW NON-OWNER 2026-07-16T08:52:49.685-04:00 -->
