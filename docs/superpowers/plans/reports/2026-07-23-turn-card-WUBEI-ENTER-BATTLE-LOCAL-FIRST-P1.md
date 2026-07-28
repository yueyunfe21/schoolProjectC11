# CR271 TURN CARD - WUBEI-ENTER-BATTLE-LOCAL-FIRST-P1

- created: `2026-07-23T11:12:00-04:00`
- owner: `CODEX-WORKER / PARENT FINAL REVIEW`
- state: `WHOLE-CARD SOURCE+TEST DELIVERED / AWAITING PARENT FINAL REVIEW`
- repositories:
  - Client: `D:\mavenProject\DHXY-cr271` (`thin-client-design`)
  - Cloud: `D:\mavenProject\dhxy-cloud-brain` (`navigation-migration`)
- protected baseline: `D:\mavenProject\DHXY` (strictly read-only)

## Problem

The current thin-client flow uploads the full `529x208` dialog ROI while
`WUBEI_ENTER_BATTLE` is active and Cloud matches all three enter-battle templates. The protected local
baseline instead prepares these three templates locally at a `100ms` interval. This leaves 五倍 outside the
local-first performance repair already applied to 修罗.

`WUBEI_ACCEPT_TASK` and `WUBEI_PROBE_STORY` are not part of this card. They retain Cloud-owned memory,
dialog semantics and absence decisions.

## Git Baseline Evidence

- `f6d750f7` (`Migrate Wubei dialogs to runner preparation`) introduced the Client-local preparation path.
- `917ba165` (`五倍 latency 基本达标`) retained the local runner design.
- authoritative migration baseline `696a12b0` still defines:
  - `WINDOW_DIALOG_PREPARE_WUBEI_ENTER_BATTLE_INTERVAL_MS = 100L`;
  - `WubeiDialogPreparationProvider` calling
    `DialogService.prepareGreenTemplateOption(..., WubeiDialogCatalog.enterBattleSpecs(), true, null,
    suppliedDetection)`;
  - local `PreparedDialogAction` production followed by task-owned consumption.

Implementation must compare with `git show 696a12b0:<path>`, not infer business behavior from the current
dirty protected worktree.

## Frozen Business Contract

1. Only `WUBEI_ENTER_BATTLE` moves to a Client-local mechanical matcher. It uses the unchanged three-entry
   `WubeiDialogCatalog.enterBattleSpecs()` list, template assets, dialog ROI, offsets and validation rules.
2. The Client matcher runs only for an exact active 五倍 enter-battle interest belonging to the current
   `{taskRunId, windowId, hwnd, interestId}`. An ordinary miss emits nothing and changes no task state.
3. A local hit does not click directly and does not advance 五倍. It publishes a bounded typed observation fact
   containing the matched action key, template identity, window-relative match box/click point, capture time,
   observer sequence and exact interest identity.
4. Cloud validates the fact against the exact current run/window/hwnd/interest and the unchanged catalog, then
   creates the same `PreparedDialogAction` shape the existing Cloud image classifier creates. The 五倍 task
   remains the sole consumer/executor of that prepared action.
5. Client-local matching is first. While valid local facts continue to arrive, no full dialog ROI is requested.
6. Cloud image fallback is bounded and non-blocking. It becomes eligible only after the exact enter-battle
   interest has remained unresolved for `3000ms`; a fallback frame must be strictly newer than its request
   sequence/time fence.
7. Local prepared success, prepared-action consumption, interest clear/replacement, real `IN_COMBAT`, stop or
   task replacement removes fallback demand. A stale hit/fallback cannot revive a closed interest.
8. `WUBEI_ACCEPT_TASK`, `WUBEI_PROBE_STORY`, task phases, retry budgets, click execution, remembered choices,
   templates, offsets, dialog classification and Cloud semantic ownership remain unchanged.
9. The matcher must capture and match in memory. It must not write debug PNG files, acquire the physical input
   queue, focus a window, sleep while holding a turn, or perform OCR.

## Required Tests

- Client:
  - all three catalog templates produce the expected action key and click point;
  - normal misses emit no fact and no ROI request;
  - wrong run/window/hwnd/interest facts are impossible to publish;
  - clear/replacement stops matching;
  - matching performs no input/focus/disk write.
- Cloud:
  - a fresh exact local hit produces the existing 五倍 `PreparedDialogAction`;
  - action key/template/geometry or identity drift is rejected fail-closed;
  - no full dialog ROI is requested before `3000ms`;
  - unresolved exact interest requests one bounded fallback after `3000ms`;
  - stale frames and stale interests are rejected;
  - local prepared success, `IN_COMBAT`, clear/replacement and terminal stop remove demand;
  - accept-task and probe-story continue through the current Cloud image path unchanged.
- Named card families and applicable Client/Cloud compile must pass. No runtime/UI/live capture/input.

## Review Gate

Parent must review every changed source/test file against the protected baseline and current 五倍 Cloud task.
Findings are reported as `P0/P1/P2`; only `0/0/0` releases the owner.

<!-- TRUE_EOF: WUBEI-ENTER-BATTLE-LOCAL-FIRST-P1 ACTIVE WHOLE-CARD-SOURCE-TEST-IMPLEMENTATION OWNER-CODEX-WORKER PARENT-FINAL-REVIEW 2026-07-23T11:12:00-04:00 -->

## CANONICAL WHOLE-CARD SOURCE+TEST DELIVERY - 2026-07-23T11:46:15-04:00

- state: `WHOLE-CARD SOURCE+TEST DELIVERED / AWAITING PARENT FINAL REVIEW / OWNER RETAINED`;
  this delivery does not self-approve `P0/P1/P2`.
- authoritative behavior baseline:
  - migration baseline is Git commit `696a12b0`, inspected with `git show 696a12b0:<path>`;
  - `917ba165` is retained only as latency evidence;
  - the restored chain is the baseline chain: Client
    `WubeiDialogPreparationProvider` semantics at `100ms` produce a `PreparedDialogAction`, Cloud reconstructs
    that existing action shape from a minimal typed fact, and `WubeiTask` remains the sole consumer/executor.
- delivered behavior:
  - Client performs only the three unchanged `WUBEI_ENTER_BATTLE` mechanical green-template matches in memory;
    it emits no fact on an ordinary miss and never clicks, focuses, sleeps or acquires the input queue;
  - the typed fact carries exact task-run/window/HWND/interest identity, capture/observer sequence, action key,
    template identity, match geometry and click point;
  - Cloud accepts only the exact current fact and reconstructs the existing `PreparedDialogAction`; malformed,
    stale or identity-drifted facts fail closed;
  - the full Cloud dialog ROI remains disabled for the first `3000ms` of unresolved enter-battle interest and
    becomes a bounded dynamic fallback afterward; `WUBEI_ACCEPT_TASK` and `WUBEI_PROBE_STORY` retain their
    existing Cloud path.
- exact Client source/test files, SHA-256:
  - `ObservationPreparedDialogFact.java`
    `347BB4EE10CBEDC8CBB0CE762A44C2FE34B063711632B79287016B765DA0C8DC`;
  - `ObservationRequest.java`
    `B8748A8A1C76178440608C11A710A3DAC75688317AB3F96964348E397E6D6CF0`;
  - `ObservationProtocolValidator.java`
    `168428332376EA596F5B34D19760312C7391B491B999B8482A7D11EBF18C47E4`;
  - `GreenTemplateClickSpec.java`
    `98FE53DBC0635D1EA25600A03BF97FCC6443CFF8774049A1342B49F88FB1D3A0`;
  - `WubeiDialogCatalog.java`
    `8FDED4F6B5A762C3903E68FEC173845FCF3235E7EB5B1AFF6C88D075F96282A4`;
  - `ImagePreprocessor.java`
    `C6052E6EA07A32EAB62C5EC55EEF53E48AE65C281F9C0D60C61B5FB204B21BE5`;
  - `DialogService.java`
    `881213BAF30B48057D035AFCC029D70AE4633075C64A83303E944780FE84FC84`;
  - `WindowObservationSampler.java`
    `9D0A2D92FCE5074BCD184B8617DD86C05534BA34203DB724BCCA7BF6C777240E`;
  - `WindowObservationRunner.java`
    `F5BB63CA54BF429941C31306A6D2ECEE3EAC9A78C5C32D7A0B55B45CF6E544F1`;
  - `WindowObservationKandaContractTest.java`
    `0D58C47D276AC4DEEC0431FEC64D0DD8702C309A2EFDFB61132B0118E7719427`;
  - `WubeiLocalDialogPreparationContractTest.java`
    `28B10352CDEDFFE7128948FD474FAF38A0B8F3600B95CE6FB3232681A2A263E1`.
- exact Cloud source/test files, SHA-256:
  - the three shared observation files are byte-identical to the Client hashes above;
  - `CloudWindowObservationInbox.java`
    `29371615354530131DA907D2BF21DCB90AF3C7740B5E6D59D20BE46BD33A3BE0`;
  - `CloudWholeTaskObserver.java`
    `62FFEA3B2BA073873EE80236458567B4DDEEF2135B24773C843420790F377960`;
  - `CloudObservationContractTest.java`
    `F9557D7F435B2D7B63CCC5E3E712A83372118252ACEFB64EFC82FEE51AB6E051`;
  - `CloudWholeTaskObserverPolicyContractTest.java`
    `F30E1AD8F8B883D6A99B009CDA35B30B4165ED2C990482D4B62626690E196B09`;
  - `CloudWholeTaskObserverProductionHarnessTest.java`
    `FE29170449DBF3954BC723F0218130E6721FF7C8F3CCEC849FBDC2F16CF21903`.
- exact verification:
  - Client
    `WubeiLocalDialogPreparationContractTest,WindowObservationKandaContractTest,WindowObservationRunnerContractTest,HttpsObservationClientRoundTripContractTest`
    => `22/22`, failures/errors `0`; this includes actual OpenCV replay of all three production template assets;
  - Cloud
    `CloudObservationContractTest,CloudWholeTaskObserverPolicyContractTest,CloudWholeTaskObserverProductionHarnessTest,WubeiWholeTaskTurnContractTest`
    => `64/64`, failures/errors `0`, compile/testCompile success;
  - shared fact/request/validator byte-identical; both exact write sets pass `git diff --check`.
- worker self-check: no known `P0/P1/P2` finding, but only the parent may record the final `0/0/0`.
- residual gate: no runtime/UI/live capture/input was run. Fresh deployed runtime acceptance remains pending.
  `D:\mavenProject\DHXY` remained strictly read-only; no Git mutation was performed.

<!-- TRUE_EOF: WUBEI-ENTER-BATTLE-LOCAL-FIRST-P1 CANONICAL-WHOLE-CARD-SOURCE-TEST-DELIVERED AWAITING-PARENT-FINAL-REVIEW OWNER-RETAINED CLIENT-22OF22 CLOUD-64OF64 DUAL-COMPILE DTO-3OF3-BYTE-IDENTICAL DIFF-CHECK-ZERO FRESH-RUNTIME-PENDING 2026-07-23T11:46:15-04:00 -->
