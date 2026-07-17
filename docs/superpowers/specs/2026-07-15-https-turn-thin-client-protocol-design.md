# HTTPS Turn Thin-Client Protocol Design

## Status

- Decision date: 2026-07-15
- Decision: user approved
- Scope: DHXY local client and Cloud Brain transport and action/result envelope
- Authority: this document supersedes the WebSocket transport choice in
  `2026-07-12-full-cloud-thin-client-architecture-draft.md` and the current HTTP poll/outcome shape as the
  target architecture. Existing HTTP code is implementation history until a separate implementation plan
  replaces it.

## Contract Test Acceptance

The user explicitly authorized the named `HTTPS_TURN_CONTRACT_TEST_FAMILY` on 2026-07-15. It is a narrow
exception to the repository's default no-local-test mode and does not authorize unrelated tests or real desktop
automation.

- Protocol golden tests must prove canonical Cloud action JSON is accepted by DHXY and canonical DHXY outcome
  JSON is accepted by Cloud. The two repositories keep byte-identical fixtures with parent-reviewed SHA-256.
- Outcome cases include `COMPLETED`, failed step plus following `NOT_RUN`, `STOPPED`, and
  `DUPLICATE_OR_UNCERTAIN`. None may be silently converted into business success or an automatic retry.
- Multipart tests compare the real raw PNG bytes and their SHA/dimensions/region metadata; comparing metadata
  alone is insufficient.
- Lifecycle tests prove stable `startRequestId` acknowledgement, duplicate start suppression, pause/resume,
  stop/unregister release, and rejection of `SLEEP_COMPUTER`.
- Business tests capture production `TurnAction` through a fake `TurnGameClient`, script all outcome classes, and
  assert the 696a12b0 decision order, counts, delays, fallback, park and terminal semantics remain unchanged.
- Tests use only fake capture/input/local Services, scripted outcomes, and loopback HTTP. They never start the
  application/server/Task runtime or send real desktop input. Real Win32 behavior remains the separate TURN-41
  user runtime gate.

The authoritative per-card test files, cases, commands, debt cards, and approval state machine are in Section 19
of `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`.

## Decision

V1 uses HTTPS request/response transport with one client-initiated long-wait `turn` endpoint.

- Do not use WebSocket.
- Do not use raw TCP sockets.
- Do not use frequent short polling.
- The local client is always the connection initiator; Cloud never opens a connection to the game machine.
- HTTP/2 connection reuse is preferred. The protocol remains valid over HTTP/1.1 keep-alive.
- Plain HTTP is allowed only for an explicitly local development endpoint. Any machine-to-cloud traffic uses
  HTTPS.

The protocol follows the actual business cadence: Cloud sends one closed action payload, DHXY executes it, and
DHXY returns one closed outcome before Cloud decides the next payload.

## Locked Minimum Contract

This section is the implementation contract for the complete migration plan. It closes the protocol without
creating another protocol layer.

### One JSON action and five step kinds

An action contains only `CAPTURE`, `MATCH_TEMPLATE`, `INPUT`, `WAIT`, and `LOCAL_SERVICE`. `INPUT` uses the closed
action enum `CLICK_LEFT`, `CLICK_RIGHT`, `DOUBLE_CLICK_LEFT`, `DOUBLE_CLICK_RIGHT`, `DRAG_LEFT`, `SCROLL`,
`KEY_TAP`, `KEY_DOWN`, `KEY_UP`, and `TEXT_INPUT`.

- Mouse input stays on the existing global `InputActionQueue`; move+click and drag remain atomic sequences.
- Keyboard uses HWND background delivery whenever supported; an unsupported key returns a typed failure and never
  silently falls back to foreground input.
- DHXY executes `MATCH_TEMPLATE` only when the payload explicitly asks for it. Candidate ordering and business
  fallback remain Cloud decisions.

### One outcome frame per turn

The validator rejects an action with more than one step that can upload an image. A normal capture, Quest-detail
PNG, and failure evidence share one frame slot identified by `framePurpose`. If a later failure requires full-window
evidence, that full-window PNG replaces an earlier unreturned success frame while prior step metadata remains in the
outcome. PNG is multipart raw bytes only, never Base64 JSON.

### Current bound-window metadata

Every request and outcome carries one `TurnWindowMetadata` with the current `deviceId`, `windowId`, `windowTitle`,
string-form `nativeHandle`, `processId`, `windowRect(left, top, width, height)`, `pauseRequested`, and
`stopRequested`.
`TurnFramePurpose` is closed to `CAPTURE`, `MATCH_EVIDENCE`, `QUEST_DETAIL`, and `FAILURE_EVIDENCE`.
`FOCUS_STATE` is diagnostic only; `STOP_STATE` cannot be interpreted by Cloud as a new business failure.

### Closed local-Service arguments

`LOCAL_SERVICE` uses `operation` plus operation-specific typed arguments. It rejects `className`, `methodName`,
reflection, and arbitrary maps. The closed operations are `BAG_RETURN_ITEM`, `BAG_USE_INCENSE`, `UI_CLEAN_ALL`,
`UI_CLOSE_GENERIC_WINDOWS`, `UI_CLEAN_LIGHTWEIGHT`, `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2`,
`GIVE_ITEM_FROM_OPEN_DIALOG`, `QUEST_ACTIVATE`, and `QUEST_CAPTURE_DETAIL`. Bag intent/cached point and Quest
detail PNG use dedicated typed DTOs rather than generic scalar fields.

The exact argument records are:

```java
TurnBagOperationArguments(ReturnItemIntent intent, String targetItemTemplate,
        Integer maxBagIndex, TurnReturnItemCachePoint cachedPoint, String source)
TurnReturnItemCachePoint(String templatePath, int clickX, int clickY, long learnedAtMs, String source)
TurnUiOperationArguments(String source)
TurnGiveItemOperationArguments(String targetItemTemplate, Integer knownBagIndex)
TurnQuestOperationArguments(String task, Boolean keepOpen)
```

`ReturnItemIntent` is `PRESCAN_TASK_PAGE`, `PRESCAN_FROM_BACK`, or `USE_CACHED_RETURN_ITEM`. For no-argument
operations (`BAG_USE_INCENSE`, `UI_CLEAN_ALL`, `UI_CLOSE_GENERIC_WINDOWS`) the corresponding argument group is
null. `UI_CLEAN_LIGHTWEIGHT` and `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2` require nonblank `source`.
`QUEST_ACTIVATE` requires nonblank `task` and nonnull `keepOpen`; `QUEST_CAPTURE_DETAIL` requires nonblank `task`
and null `keepOpen`. Its PNG is returned through the single `TurnFrameMetadata` slot with purpose `QUEST_DETAIL`.

### Acknowledgement and retry boundary

Cloud waits for an action through `CloudTurnCommandResult`, never a fabricated business `TurnOutcome`. Any valid
`200 TurnResponse`, including `IDLE`, accepts the carried previous outcome, so DHXY clears its previous outcome and
frame. On network uncertainty, DHXY resubmits that same outcome but never re-executes its `actionId`. There is no
local business retry: ROI widening or a full-window retry is an explicit new Cloud action with a new `actionId`.

### Task start and control boundary

The first remote request may carry one typed `TurnTaskStartRequest` containing a stable `startRequestId`, an ordered
list of allowlisted task codes, and `CONTINUE_ON_FAILURE` or `STOP_ON_FAILURE`. The exact request is carried again
until the matching `TurnTaskStartAck` is observed; Cloud keeps only the current runtime and last accepted
`startRequestId/ack` in memory so transport redelivery cannot start a second Task. This is acknowledgement
correlation, not a business retry, durable workflow, session, or ledger.

`pauseRequested` pauses Cloud Task progression at the existing checkpoint boundary while the DHXY long-wait loop
stays alive. `stopRequested` requests Task stop; unregister performs stop semantics before removing the local loop
and bound-window registration. `SLEEP_COMPUTER` is not a remote Task code. V1 uses one server-process configuration
for `tenantId/userId/stateRoot`; request-body identity text never selects Cloud private state.

### Template authority

`CloudTemplateCatalog` supplies the same PNG bytes for an action `contentHash` and its template GET response.
DHXY verifies `images/template/...png` by SHA-256, downloads stale/missing content, and atomically replaces it
without restart.

## Turn Flow

The single logical endpoint is:

```text
POST /api/v1/client/turn
```

Each request reports the previous turn outcome. Each response supplies the next action payload. On the first
request, `previousOutcome` is absent. If no action is ready, Cloud holds the request until an action is ready or
the transport wait expires.

```text
DHXY -- previous result and optional image --> Cloud
DHXY <-- next action JSON ------------------- Cloud
```

An expired or disconnected long-wait request is a transport event only. DHXY reconnects to continue transport
but must not automatically repeat a physical or business action. Cloud decides whether to issue a new payload,
retry a failed step, request a wider capture, or restart the business sequence.

Only one action payload may be in flight for one bound game window. Every payload carries an `actionId`, and every
outcome echoes it. A duplicate or uncertain `actionId` must not silently execute physical input again; DHXY
returns a typed duplicate/uncertain outcome and lets Cloud decide.

## Payload Shape

Cloud sends one JSON payload containing an ordered list of local mechanical steps and the evidence required after
those steps.

```json
{
  "contractVersion": 1,
  "actionId": "white-dragon-001",
  "deviceId": "device-1",
  "windowId": "window-2",
  "steps": [
    {
      "index": 0,
      "type": "INPUT",
      "inputAction": "KEY_TAP",
      "input": {"key": "ALT_E"}
    },
    {
      "index": 1,
      "type": "INPUT",
      "inputAction": "CLICK_LEFT",
      "input": {"x": 1420, "y": 736}
    },
    {"index": 2, "type": "WAIT", "waitMs": 800},
    {
      "index": 3,
      "type": "CAPTURE",
      "capture": {
        "region": {"x": 1080, "y": 420, "width": 700, "height": 500},
        "resultMode": "UPLOAD_IMAGE"
      }
    }
  ],
  "fullWindowFailureEvidence": true
}
```

The first V1 step vocabulary is deliberately small:

- `CAPTURE`: capture the bound game window or an explicit ROI in the background.
- `MATCH_TEMPLATE`: match a Cloud-named template locally and return a typed match result.
- `INPUT`: execute one closed `TurnInputAction`; mouse actions use the exact supplied coordinate and keyboard
  actions use a background-capable native provider when supported.
- `WAIT`: wait for the supplied mechanical delay inside the closed payload.
- `LOCAL_SERVICE`: invoke one closed, typed operation owned by exactly one of the four permanent local Services:
  `BagService`, `UICleanerService`, `GiveItemService`, or `QuestManagerService`. The service and operation are
  closed enums with operation-specific typed arguments; arbitrary class names, method names, or generic JSON
  reflection are forbidden.

Composite business choices do not belong in DHXY. Cloud expresses the complete ordered mechanics it wants in the
payload and receives the requested evidence at the end.

`LOCAL_SERVICE` exists only because those four Services own approved closed local mechanics. Its result is a typed
step result in the same turn outcome. It must not call another Cloud-owned Service, choose a task phase, or perform
an unrequested business retry/fallback.

### Queue-Owned Click Timing

`TurnInputSpec` may carry nullable `clickDelayMs` and `queueHoldMs` only for `CLICK_LEFT` and `CLICK_RIGHT`.
Each value is an explicit bounded mechanical delay in `[0,5000]`; null preserves the legacy zero-delay behavior.
DHXY maps the click delay into the physical click action and, when `queueHoldMs>0`, appends one sleep to the same
mapped action list. The complete list is submitted once to the global input queue. These fields do not authorize a
second command, transport retry, no-op mouse action, or business decision. All other input actions reject them.

## Capture And Result Modes

Every observation step explicitly chooses one result mode:

- `UPLOAD_IMAGE`: return the captured pixels to Cloud for Cloud OCR, template matching, geometry, and business
  decisions.
- `RETURN_MATCH_RESULT`: run the explicitly requested local template match and return found/not-found, score,
  rectangle, and point without uploading the successful image unless the payload also asks for it.

`MATCH_TEMPLATE` also declares what happens after a match:

- `onMatch=NONE`: return the anchor or match result without clicking.
- `onMatch=CLICK`: click the exact matched point as part of the same closed local payload, then return the physical
  action result and any requested post-action evidence.

This supports both latency policies without changing the interface. Cloud can keep OCR and matching in Cloud by
requesting `UPLOAD_IMAGE`, or selectively use local matching by requesting `RETURN_MATCH_RESULT`.

### Exact-Window Pixel-Change Probe

An `UPLOAD_IMAGE` CAPTURE with a non-null ROI may optionally carry `pixelChangeProbe`. This is a single-action
mechanics primitive for the existing Ctrl-hover transition: exact-HWND before capture, Ctrl down, explicit settle,
one exact screen-absolute mouse move, explicit settle, same-HWND after capture, pixel-difference comparison, and
finally Ctrl up plus explicit settle. The action contains only that one CAPTURE step. Before pixels stay in memory;
the after frame is the action's only requested raw PNG.

The payload supplies the target, three bounded settle delays, and a finite `[0.0,1.0]` difference-ratio threshold.
The existing fixed RGB channel tolerance remains local mechanics. A completed result is typed
`PIXELS_CHANGED` or `PIXELS_UNCHANGED`; Ctrl release failure, stop, interruption, or another mechanics failure must
not fabricate either code. `pixelChangeProbe` is mutually exclusive with pointer-clear. Cloud alone performs OCR,
template/FIFO decisions, decides whether to click, and decides whether a later action is needed.

## Post-Action Evidence

A payload may require evidence after keyboard or mouse input. This requirement is part of the same payload; Cloud
does not need to send a second command merely to request the expected follow-up evidence.

Examples:

- click, then upload the resulting dialog ROI;
- use an item, then locally match a named dialog template and return true/false;
- click a matched option, then upload a post-click ROI;
- press a key, then capture the full bound window.

The physical action status and the observation result are separate fields. `OK` for a click means the physical
input was issued successfully; it does not claim that the business objective succeeded.

## Image Transfer

Large images are not Base64-encoded inside JSON. When an outcome contains an image, DHXY sends one HTTPS
`multipart/form-data` turn request:

```text
metadata: application/json
frame:    image/png
```

V1 image rules:

- Capture the smallest Cloud-requested ROI that preserves the required evidence.
- Preserve one source pixel as one transmitted pixel; do not resize coordinates or images.
- Use PNG for deterministic UI/OCR/template evidence.
- `windowRect.left` and `windowRect.top` are the actual screen coordinates of the bound window, not `0,0`.
- Cloud receives the actual window rectangle and returns exact screen-absolute click coordinates derived from that
  rectangle. DHXY must not scale those coordinates.
- A full-window image is used when Cloud explicitly requests it or when required failure evidence cannot be
  represented by the requested ROI.

Small JSON-only outcomes continue to use `application/json`.

## Failure Handling

If any step fails, DHXY stops the remaining steps and returns:

- `actionId`;
- failed step index and step type;
- typed physical/mechanical status;
- error detail suitable for diagnostics;
- the actual bound `windowRect`;
- a failure screenshot, normally full-window unless the payload explicitly requires a sufficient ROI;
- results from already completed steps.

DHXY does not choose a business retry. Cloud inspects the failure and screenshot, then sends a new payload. Cloud
may first retry with a focused ROI and later request a full-window image, but each retry is a new explicit Cloud
decision and a new action payload.

## Template Distribution

Cloud identifies templates by stable logical key and content hash, for example:

```json
{
  "templateKey": "dialog/white-dragon-option",
  "contentHash": "sha256:..."
}
```

DHXY may keep development templates and a runtime cache. Before local matching, it compares the requested hash
with its cached copy:

- matching hash: use the local copy;
- missing or stale hash: download the exact template from Cloud over HTTPS, replace the cache entry, then match;
- Cloud unavailable: return a typed template-unavailable result; the four permanent local Services do not operate
  as an independent application without Cloud.

Template updates do not require a DHXY restart. Unneeded local template files may be removed later because they
are fetched on demand when Cloud explicitly requests local matching.

An explicitly requested folder match may use a manifest containing ordered `templateKey + contentHash` entries;
DHXY must not enumerate an uncontrolled local folder as business truth.

## Local And Cloud Responsibilities

Cloud owns:

- Task and Service business logic;
- OCR and image analysis by default;
- action ordering and all business retry/fallback decisions;
- deciding whether evidence is uploaded or matched locally;
- exact template versions and the next payload after success or failure.

DHXY owns:

- exact bound-window capture and native window geometry;
- background-capable capture and keyboard mechanics;
- foreground mouse mechanics when required by Windows;
- ordered execution of the supplied closed payload;
- optional local template matching only when explicitly requested;
- typed physical results, failure step, and requested evidence.

The permanently local business/mechanical Services remain `BagService`, `UICleanerService`, `GiveItemService`,
and `QuestManagerService`. Their local implementation may use the same capture/match/input primitives, but they do
not become independent Cloud-free workflows.

## Explicit Non-Goals

V1 does not add:

- WebSocket heartbeat, reconnect, sequence, or backpressure machinery;
- raw socket framing;
- short-interval command polling;
- local OCR for ordinary Cloud-owned Services;
- local business orchestration or automatic business retry;
- image resizing or coordinate scaling;
- Base64 encoding for large screenshots;
- a second local task runtime or an independent local workflow engine.

## Acceptance Criteria

The transport redesign is ready for implementation planning when the plan preserves all of these invariants:

1. One client-initiated HTTPS `/turn` exchange replaces separate poll/outcome target semantics.
2. One payload can express physical actions plus required post-action capture or local matching.
3. Images use multipart binary transfer and exact unscaled window coordinates.
4. Failures return the failed step and screenshot; only Cloud decides the next attempt.
5. Templates use `templateKey + contentHash` and refresh without restarting DHXY.
6. Capture and keyboard use background-capable mechanics; only mouse input requires foreground operation.
7. Ordinary OCR and image computation remain in Cloud unless a payload explicitly selects local template matching.
8. The only locally invokable Service step is the closed `LOCAL_SERVICE` allowlist for `BagService`,
   `UICleanerService`, `GiveItemService`, and `QuestManagerService`.
