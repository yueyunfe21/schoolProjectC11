# STOP-RESTART-P1 - Cloud Stop/Restart Lifecycle Repair

## Status

`SOURCE+TEST PASSED / DIRECT USER-APPROVED REPAIR / FRESH RUNTIME REQUIRED`

## Runtime evidence

- Client `20:08:33.774` requested stop. The final stop-bearing turn returned HTTP 503 at `20:08:38.796` with
  `TURN_RUNTIME_STOP_TIMEOUT`.
- The same window started a fresh loop and received a fresh acknowledgement at `20:08:54.522-20:08:54.526` for
  `remote-turn-d5226c2d-295a-4be2-b7b8-93355749bd39`.
- Cloud retained the retired run's observer long enough to keep probing after stop. The fresh role preflight then
  failed with `team-role exact-window capture failed: WINDOW_BUSY` and the fresh queue terminated as `SKIPPED`.
- The Client UI reported `1/1` start success because start ACK precedes asynchronous role preflight/queue terminal.

## Frozen business contract

- Checked `docs/业务逻辑.md` and the approved 修罗 baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- No task phase, OCR/template rule, navigation/click order, retry/fallback order or combat truth changes are authorized.
- This card changes only exact-run lifecycle, transient command-slot backoff, and terminal diagnostics.
- 无已批准业务差异；按基线等价迁移。

## Repair contract

1. Bind the Cloud observer handle to the exact `(deviceId, windowId, startRequestId)` run slot. Stop closes that
   observer before stopping the task; a stop racing observer installation closes the late handle immediately.
2. The observer checks exact-run stop between probes and must not acquire another command turn after stop becomes
   visible.
3. Team-role retry delay is scheduler backoff, not a Client command. A transient busy first attempt must leave the
   slot free and retry; it must not become final `UNKNOWN` merely because the retry delay itself also hit the slot.
4. Cloud publishes a terminal result tied to the exact accepted `startRequestId`; Client accepts only the matching
   run terminal and stops displaying it as running. Stale terminals from a retired run are ignored.

## Verification

- Client `WindowTurnLoopContractTest,TurnProtocolValidatorContractTest`: PASS. The contract proves a retired run's
  terminal is ignored and only the exact current `startRequestId` can stop/project the fresh run.
- Cloud `CloudTeamRolePreflightServiceContractTest,CloudTurnTaskRegistryContractTest,CloudTurnTaskRuntimeContractTest`:
  PASS. The contracts cover slot-free busy retry, five-window isolation, registered observer stop, late observer
  rejection, terminal and restart.
- Cloud `CloudTurnHttpHandlerContractTest,TurnProtocolValidatorContractTest`: PASS across the HTTP/validation edge.
- Client and Cloud Java compile: PASS. Shared `TurnResponse`, `TurnTaskTerminalResult` and
  `TurnProtocolValidator` are SHA-256 byte-identical in both repositories. `git diff --check`: PASS.
- A broader pre-existing golden invocation is not green: `TurnTaskLifecycleProtocolGoldenJsonTest` reports the old
  `request-start.json` fixture omits existing `mapSurveyCommand`/`mapSurveyResultAckId` null fields. This request-side
  fixture drift is outside STOP-RESTART-P1 and was not rewritten to hide the failure.
- No runtime/UI/capture/input was run by Codex.
- Fresh user gate: running -> stop -> immediate restart must produce a fresh role result and task action; pause ->
  resume must continue the same run.

## Source review

`P0/P1/P2=0/0/0` for this repair scope. The 696 task phases, recognition rules, navigation order, click order and
fallback order are unchanged. Both Cloud and Client processes must be restarted before fresh verification because
an already-running JVM cannot load these newly compiled classes.

<!-- TRUE_EOF: STOP-RESTART-P1 SOURCE+TEST PASSED FRESH-RUNTIME-REQUIRED 2026-07-21 -->

## Fresh Runtime Reopen + Direct Repair #2 - 2026-07-21T22:09:48-04:00

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REPAIR PASSED / FRESH RUNTIME REQUIRED`.
- runtime proof: run `remote-turn-5b425595-0ee5-4b77-acb1-0b08857f01f8` navigated to 灵兽村 and then parked on
  `WAIT_TARGET_PATHING_TERMINAL`; every Cloud Observer cycle failed with `No TaskExecutionContext is bound`, so no
  terminal pathing fact could advance the phase to `NPC_CLICK_SMART`.
- lifecycle proof: stop took 5016ms and returned `TURN_RUNTIME_STOP_TIMEOUT`; subsequent starts were actually sent,
  but Cloud's active-run conflict produced an ACK-less HTTP 200 and Client correctly rejected it as
  `response requires taskStartAck for taskStartRequest`.
- repair: Observer turn handles bind the exact immutable run context; `TaskExecutionContext` exact metadata reads no
  longer depend on a worker ThreadLocal; exact-run stop interrupts the worker as well as closing Observer/stopping the
  task; conflicts return `409 TASK_START_CONFLICT` instead of an invalid response.
- tests: new Observer-background, blocked-stop/immediate-restart, and HTTP-conflict contracts pass 3/3. Full
  `CloudTurnTaskRuntimeContractTest`, `CloudTurnActivationContractTest`, and
  `CloudWholeTaskObserverProductionHarnessTest` pass; Cloud tests-enabled compile passes. No business phase, OCR,
  navigation, click, fallback, runtime/UI/capture/input behavior was executed or changed.
- fresh gate: restart the Cloud JVM, then verify navigation publishes `PATHING_TERMINAL`, reaches `NPC_CLICK_SMART`,
  stop completes without the 5-second timeout, and an immediate restart returns its matching ACK.

<!-- TRUE_EOF: STOP-RESTART-P1 FRESH-REPAIR2 SOURCE-TEST-PASSED OBSERVER-EXACT-CONTEXT STOP-INTERRUPTS-WORKER HTTP409-CONFLICT FRESH-RUNTIME-REQUIRED 2026-07-21T22:09:48-04:00 -->

## Fresh Runtime Repair #3 - Terminal predecessor command-slot retirement - 2026-07-22

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REPAIR PASSED / FRESH RUNTIME REQUIRED`.
- runtime evidence: windows `316365558` and `451753529` had already terminal `AUTO_BATTLE` predecessor runs, but each
  predecessor left its final unresolved Cloud command in the exact-window `CloudTurnExchange` slot. The replacement
  Xiuluo run was accepted, then its first `PlayerState` location capture failed immediately as `BUSY` before entering
  task business logic.
- repair: before a replacement start is acknowledged, the HTTP boundary may retire an unresolved exact-window action
  only when the registry proves that the predecessor is terminal and its `startRequestId` differs from the incoming
  start. The old waiter receives typed `STOPPED`; active predecessors still return the existing start conflict and a
  same-id duplicate start keeps its idempotent replay behavior.
- tests: `CloudTurnExchangeContractTest,CloudTurnActivationContractTest` pass `20/20`; Cloud main compile and
  test-compile pass through the named run. The activation contract uses the real residual predecessor command and
  proves that the replacement receives its matching ACK without old-action redelivery. No runtime/UI/capture/input.
- fresh gate: restart Cloud, then start a new task over terminal windows. Their first location capture must not return
  `BUSY`, and no action from the predecessor run may be delivered to the replacement run.

<!-- TRUE_EOF: STOP-RESTART-P1 FRESH-REPAIR3 SOURCE-TEST-PASSED TERMINAL-PREDECESSOR-SLOT-RETIRED CLOUD-20OF20 P0P1P2-0-0-0 FRESH-RUNTIME-REQUIRED 2026-07-22 -->
