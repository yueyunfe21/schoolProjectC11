# CR271 TURN CARD - XIULUO-DIALOG-ROI-FALLBACK-P1

- created: `2026-07-23T10:44:31-04:00`
- owner: `CODEX-WORKER / PARENT FINAL REVIEW`
- state: `ACTIVE / WHOLE-CARD SOURCE+TEST IMPLEMENTATION`
- repositories:
  - Client: `D:\mavenProject\DHXY-cr271` (`thin-client-design`)
  - Cloud: `D:\mavenProject\dhxy-cloud-brain` (`navigation-migration`)
- protected baseline: `D:\mavenProject\DHXY` (strictly read-only)

## Problem

The four continuous BattleRadar image uploads were removed by `COMBAT-OBS-P1`, but Cloud still publishes the
`529x208` `xiuluo-dialog` ROI every two seconds for the whole run. Re-enabling the verified Client-local
`kanda2` matcher alone would not remove that upload and would let local and Cloud preparation race.

An ordinary local template miss is expected while the dialog is not yet visible. It must never be interpreted as
a Cloud-fallback request.

## Frozen Business Contract

1. Production re-enables the existing verified local `kanda2` matcher. Its ROI, template, threshold, fresh-frame
   revalidation, exact attempt fences, one-shot CAS and atomic input sequence remain unchanged.
2. While the exact dialog interest is `probeOnly=true`, an ordinary local miss has zero side effects:
   no Cloud ROI demand, no negative fact, no retry increment and no interest replacement.
3. The first exact pathing transition to `ARRIVED` or `STOPPED_AWAY` records a non-blocking fallback eligibility
   boundary:
   `fallbackEligibleAtMs = terminalObservedAtMs + 3000`.
4. During that three-second grace window the local matcher continues normally. No thread sleeps or holds a task
   turn/input slot for the grace period.
5. At/after `fallbackEligibleAtMs`, Cloud may request the full `xiuluo-dialog` ROI only if the same exact
   `{taskRunId, windowId, hwnd, attemptId, round, intentId}` remains current and no local click, real
   `IN_COMBAT`, clear or replacement has superseded it.
6. Stopped-static Cloud fallback may analyze only a frame whose Client capture time and observer sequence are
   strictly newer than the three-second eligibility boundary/request fence.
7. A task-owned explicit enter-battle confirmation retry (`probeOnly=false`) may request the Cloud ROI
   immediately. This retry exists only after an enter-battle click was attempted and real `IN_COMBAT` was not
   confirmed within the existing bounded confirmation window.
8. The full dialog ROI demand is dynamic and bounded to the existing demand union:
   route-dialog preparation, active non-probe-only dialog interest, or eligible stopped-static fallback.
9. Local click, real `IN_COMBAT`, interest clear, task/attempt replacement or fallback completion removes the
   corresponding Cloud ROI demand.
10. Do not change templates, thresholds, retry counts, task phases, click coordinates, dialog catalogs,
    `FastExpectedCombatExit`, or Cloud ownership of semantic dialog fallback.

## Required Tests

- Client:
  - production local-kanda enablement;
  - ordinary miss produces no Cloud demand/action/event;
  - local hit during the three-second grace wins and cancels fallback;
  - clear/replacement/run/window/hwnd/attempt/round mismatch cannot request or click;
  - no blocking sleep/turn/input hold.
- Cloud:
  - no static `xiuluo-dialog` publication for probe-only waiting;
  - terminal plus less than 3000 ms publishes no ROI demand;
  - exact terminal plus at least 3000 ms publishes one bounded demand;
  - pre-boundary, same-sequence and stale-attempt frames are rejected;
  - `probeOnly=false` explicit confirmation retry requests the ROI immediately;
  - local click/`IN_COMBAT`/clear/replacement removes demand;
  - same observer sequence is never analyzed twice.
- named card families and applicable Client/Cloud compile must pass. No runtime/UI/live capture/input.

## Review Gate

Parent must review every changed source/test file against `TURN-40G`, `COMBAT-OBS-P1`, the frozen local-kanda
baseline and the current dirty worktrees. Findings are reported as `P0/P1/P2`; only `0/0/0` releases the owner.

<!-- TRUE_EOF: XIULUO-DIALOG-ROI-FALLBACK-P1 ACTIVE WHOLE-CARD-SOURCE-TEST-IMPLEMENTATION OWNER-CODEX-WORKER PARENT-FINAL-REVIEW 2026-07-23T10:44:31-04:00 -->

## CANONICAL WHOLE-CARD SOURCE+TEST DELIVERY - 2026-07-23T11:10:13-04:00

- state: `WHOLE-CARD SOURCE+TEST DELIVERED / AWAITING PARENT FINAL REVIEW / OWNER RETAINED`;
  `P0/P1/P2 NOT SELF-APPROVED`.
- delivered behavior:
  - production `bot.xiuluo.local-kanda-enabled=true`;
  - Client `WindowObservationSampler.collectBound(...)` gives exact local kanda first refusal before dialog
    fact/ROI sampling; ordinary probe-only miss emits no event/fact/demand, while an exact successful claim
    suppresses the same request's stale Cloud dialog ROI;
  - the symmetric `ObservationDialogInterestFact.enterBattleClaimed` and validator carry the exact claim;
  - Cloud stable interests no longer include `xiuluo-dialog 529x208 @2s`;
  - Cloud dynamic demand is the union of route/full-dialog, explicit `probeOnly=false`, and exact tracker
    `ARRIVED/STOPPED_AWAY` fallback after Client capture/terminal time `+3000ms`;
  - stopped-static accepts only a frame with sequence later than the demand fence and
    `capturedAtMs > fallbackEligibleAtMs`; local claim/click, real `IN_COMBAT`, clear/replacement and completed
    fallback revoke demand.
- exact Client files, SHA-256, mtime:
  - `src/main/resources/application.properties`:
    `20254965E4341AFC5D39CF7E9220D060579B408760C27EDC3753747C6A44B885`,
    `2026-07-23T10:50:33.7111507-04:00`;
  - `ObservationDialogInterestFact.java`:
    `80D477917ED2F82E01E62BD9FD48BCD5019DE46C6CAFA7F3C82FA4A097B86A20`,
    `2026-07-23T10:50:33.7029359-04:00`;
  - `ObservationProtocolValidator.java`:
    `857210FDCE6C10B41031CF799E34870CA22C889E49DA3E612A2606644BDD0F82`,
    `2026-07-23T10:50:33.7039345-04:00`;
  - `WindowRuntimeContext.java`:
    `27E31393FB8E2F8D5E240529BE26969C937418469513E17562B75DB20CD1C8CC`,
    `2026-07-23T10:50:33.7098299-04:00`;
  - `WindowObservationSampler.java`:
    `9974CF8F111296E5B6449A04359F91E8237D4249FA36CBC3656876023B27F763`,
    `2026-07-23T10:50:53.4145438-04:00`;
  - `WindowObservationKandaContractTest.java`:
    `96E8ED302E7539EA3C929CD36D082EEFC6C77D85E2E4FD315394617AF09B0EEA`,
    `2026-07-23T10:56:07.0738168-04:00`.
- exact Cloud files, SHA-256, mtime:
  - symmetric fact/validator have the same two hashes above;
  - `CloudXiuluoLocalClickState.java`:
    `02BAF6FB9C8E46C54EA5B6B9052D08F66582E7BB050338750BC2493A1F2D7B54`,
    `2026-07-23T10:51:34.0799860-04:00`;
  - `CloudWholeTaskObserver.java`:
    `C6931302107083EC847E72FC1F0E48CE8ABFE67E7FF8DF2FCE5FC8E6691CC245`,
    `2026-07-23T10:55:36.4795649-04:00`;
  - `CloudWholeTaskObserverPolicyContractTest.java`:
    `E1778F4FD9E30B895500AAE732437F08F93577C4ADA68D0F0E5EDA5B9901EB11`,
    `2026-07-23T10:55:59.9334855-04:00`;
  - `CloudWholeTaskObserverProductionHarnessTest.java`:
    `01D97EA22CCF4C26BADBC5A85DFDF8BE1D24CA5ED3F0374F3C2225BB596ED396`,
    `2026-07-23T10:57:44.6595251-04:00`.
- exact verification:
  - Client
    `WindowObservationKandaContractTest,WindowObservationRunnerContractTest,HttpsObservationClientRoundTripContractTest,WindowTurnLoopObservationContractTest`
    => `27/27`, exit `0`; Client `mvn -q compile` exit `0`;
  - Cloud
    `CloudWholeTaskObserverPolicyContractTest,CloudWholeTaskObserverProductionHarnessTest,CloudObservationContractTest`
    => `45/45`, exit `0`; Cloud `mvn -q compile` exit `0`;
  - shared fact/validator byte-identical; both repositories `git diff --check` exit `0`.
- frozen/safety: kanda2 asset, ROI `(264,376) 41x21`, threshold `0.82`, retry/phase/click ordering unchanged;
  no sleep, second protocol/store/runner/queue, Git mutation, runtime/UI/live capture/input; protected baseline
  `D:\mavenProject\DHXY` remained read-only.
- review boundary: parent must independently review this exact delivery; fresh deployed runtime remains open.

<!-- TRUE_EOF: XIULUO-DIALOG-ROI-FALLBACK-P1 CANONICAL-WHOLE-CARD-SOURCE-TEST-DELIVERED AWAITING-PARENT-FINAL-REVIEW OWNER-RETAINED P0-P1-P2-NOT-SELF-APPROVED CLIENT-27OF27 CLOUD-45OF45 DUAL-COMPILE DTO-SYMMETRIC DIFF-CHECK-ZERO FRESH-RUNTIME-PENDING 2026-07-23T11:10:13-04:00 -->
