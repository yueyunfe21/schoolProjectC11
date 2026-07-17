# TURN-22 Repair After TURN-28P Preflight Helper

## CLAIMED

- claimedAt: `2026-07-16T03:53:32.9424516-04:00`
- role: non-binding readiness helper; not an implementation Worker, reviewer, or final approver
- agent: `Planck`
- platformAgentId: `019f69e5-015b-7e71-aa0a-a30ea1f78b46`
- onlyWriteSet: this fixed report
- scope: freeze the minimum TURN-22 Repair #1 implementation brief after TURN-28P provides generic queue-owned click timing
- safety: protect both repositories' dirty/untracked files; no Java, test, configuration, plan, CR, matrix, dashboard, Maven, runtime, input, or Git mutation

<!-- TRUE_EOF: TURN-22-repair-after-28P-preflight-helper CLAIMED Planck 019f69e5-015b-7e71-aa0a-a30ea1f78b46 2026-07-16T03:53:32.9424516-04:00 -->

## REPLACEMENT CLAIMED

- claimedAt: `2026-07-16T04:02:24.0832697-04:00`
- role: replacement non-binding readiness helper; not an implementation Worker, reviewer, or final approver
- agent: `Bohr`
- platformAgentId: `019f69f1-5df9-76a3-aca1-356dbf44e7eb`
- predecessor: preserve Planck's historical claim and continue only the unfinished preflight work
- onlyWriteSet: this fixed report
- safety: no Java, test, configuration, plan, CR, matrix, dashboard, Maven, runtime, input, or Git mutation

<!-- TRUE_EOF: TURN-22-repair-after-28P-preflight-helper REPLACEMENT CLAIMED Bohr 019f69f1-5df9-76a3-aca1-356dbf44e7eb 2026-07-16T04:02:24.0832697-04:00 -->

## PRECHECK_COMPLETE - 2026-07-16T04:06:22.4080073-04:00

### Role And Evidence Boundary

- This is a non-binding readiness precheck only. It does not make an implementation or final-review decision.
- Read and compared `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the CR271 top of `docs/ACTIVE_WORK.md`, plan sections
  14-19, the HTTPS turn protocol, `docs/业务逻辑.md`, the complete TURN-22 and TURN-28P fixed reports, both
  repositories' dirty status, current TURN-22 production/test source, current TURN-28P protocol/mapper/executor/test
  source, and the complete `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` `TeamReturnService`.
- Baseline evidence is exact: the baseline member path is observe -> incense once -> observe -> random `+-3` ->
  one queue submission containing `InputAction.clickLeft(...,150)` then `InputAction.sleep(500)`. Leader initial
  observation, `120000ms` timeout, `3000ms` poll, precheck single immutable observation, and consume-without-new-
  capture remain unchanged. `docs/业务逻辑.md` also keeps `WAIT_TEAM_RETURN` while the signal is present and resumes
  by source only after it clears.
- Current TURN-22 source hashes remain the first-delivery hashes: `TeamReturnService.java`
  `CD1CD365BFF90B16817C15831A2685F2FEAE84E2D49893B9B975362D4EC4EDAF`, assembly
  `7450B3B8D76D8F7D467078C437480E498E0002E9F61E7F47CA3363EC8734C3F1`, and named test
  `CEDD8FA6878A39CB2231A9DE7905F4C944ADE80370CE06FC31DCCAB140CC1E21`.
- TURN-28P's current working source already contains nullable `clickDelayMs/queueHoldMs`, maps click delay into the
  physical click, appends the positive queue hold to the same mapped list, and submits that list once. This observed
  working-tree state is not itself a dependency decision: the TURN-28P fixed report currently ends at Maxwell's
  replacement claim and has no later source/test delivery entry.

### Minimum TURN-22 Repair #1 Exact Write Set

1. Modify only
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`.
2. Modify only
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`.
3. Append claim/delivery evidence only to
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-22.md`.

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TeamReturnService.java` is read-only in this
repair. TURN-28P protocol/validator/mapper/executor/tests, DHXY, callers, Tasks, ports, action factory, resources,
configuration, POMs, other reports, and all other files are read-only. No new file, facade, wrapper, helper chain,
retry, session, ledger, TTL, durable workflow, or second action is needed.

### Frozen Production Delta

1. Preserve the existing `CLICK_DELAY_MS=150` and `POST_CLICK_WAIT_MS=500` constants and every exact
   device/window/HWND/process/latest-metadata/STOP/point preflight.
2. Build one `TurnInputSpec` with screen-absolute `screenX/screenY`, all unrelated fields null,
   `clickDelayMs=CLICK_DELAY_MS`, and `queueHoldMs=POST_CLICK_WAIT_MS`.
3. Replace the current three-step `CLICK_LEFT -> WAIT150 -> WAIT500` list with exactly one step:
   index `0`, type `INPUT`, action `CLICK_LEFT`, carrying that typed input. There are no outer `WAIT` steps.
4. Call the already-bound `TurnGameClient.execute(...)` exactly once with that one-step list. One service call still
   creates exactly one UUID and one command. Command uncertainty, duplicate/uncertain outcome, failure, or stop never
   resends the action.
5. Keep the existing outcome/frame/exact-correlation checks, but correlate the single input result only. Completed
   maps to `EXECUTED`, failed to `NOT_EXECUTED`, stopped to `STOPPED`, and transport/outcome uncertainty to `UNKNOWN`.
   The click action still returns no frame.
6. Do not alter observation, template matching, ROI, incense order, randomization, leader wait, precheck, or caller
   behavior. Member/leader screenshots remain one exact `272x69` raw PNG per observation and all matching/business
   decisions remain in Cloud.

### Named-Test Increment

- Change the captured production click assertion from three steps to exactly one `CLICK_LEFT` input step and assert
  `input.clickDelayMs()==150`, `input.queueHoldMs()==500`, no `WAIT`, capture, match, local-service, or frame data.
- Rename the three-step-specific test wording without weakening its terminal matrix. For the single-step failed case,
  change the scripted failed step index from `1` to `0`.
- Preserve member happy order `capture -> incense -> capture -> click`, first-miss zero incense/click, refresh-miss zero
  click, matched-center `+-3`, one UUID/command, exact binding pre-port zeros, and all leader/precheck/raw-PNG tests.
- Preserve and re-run the completed/failed/stopped/outcome-uncertain/transport-uncertain cases. Each case must capture
  one action and one UUID; uncertainty creates no retry. Wrong input type, unexpected frame, wrong outcome metadata,
  and malformed stopped correlation must continue to fail closed.
- Add a source/action assertion that the production click bundle contains no outer `WAIT` step. Do not duplicate
  TURN-28P's generic mechanics tests inside the Cloud business test.
- Cross-card acceptance pairs this production action assertion with TURN-28P's named
  `TurnInputStepExecutorContractTest::clickDelayAndQueueHoldStayInsideOneSubmissionForEachSingleClick`, which proves
  production mapper output is `[CLICK_LEFT(delay=150), SLEEP(500)]` in exactly one input-queue submission. TURN-28P
  golden/validator coverage remains responsible for byte parity, legacy-null behavior, bounds, right-click parity,
  and rejecting timing on other input actions.

### Dispatch And Dependency Gate

- Dispatch only after the parent independently records the TURN-28P source/test-source gate and confirms the final
  byte-identical `TurnInputSpec` API plus the mapper/executor named-test evidence. Working-tree presence alone is not
  the gate.
- The TURN-22 repair Worker must append a real `REPAIR #1 CLAIMED` at the true EOF of the original TURN-22 card before
  editing, preserve all prior delivery/review history, use only the three-item write set above, and deliver only
  source/test evidence for parent review.
- The Worker does not run Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input. Once Java writers are
  stable, the parent runs the authorized TURN-22 named test together with the applicable TURN-28P named mechanics
  tests and Cloud/DHXY compile gates.

### Copyable Dispatch Brief

```text
Implement TURN-22 Repair #1 only after the parent marks its TURN-28P dependency gate satisfied. Preserve both dirty
repositories and append REPAIR #1 CLAIMED to the original TURN-22 report before editing. Exact write set: only
CloudTeamReturnPortAssembly.java, TeamReturnTurnContractTest.java, and append-only TURN-22 report. TeamReturnService
and every other file are read-only. Replace the three-step CLICK_LEFT/WAIT150/WAIT500 action with one CLICK_LEFT input
whose TurnInputSpec has clickDelayMs=150 and queueHoldMs=500. Keep one UUID, one command, one queue submission through
the TURN-28P mapper/executor, no frame, and zero transport retry. Update the named test to assert one input step, exact
timings, failedStepIndex=0, all terminal mappings, raw-PNG/member/incense/leader/precheck behavior, and no outer WAIT.
Do not run Maven/runtime/input and do not write any approval or closure conclusion; append source/test delivery evidence
to the original card for parent review.
```

无已批准业务差异；按 `696a12b0` 与最小 HTTPS JSON turn 等价迁移。

<!-- TRUE_EOF: TURN-22-repair-after-28P-preflight-helper PRECHECK_COMPLETE Bohr 019f69f1-5df9-76a3-aca1-356dbf44e7eb 2026-07-16T04:06:22.4080073-04:00 -->
