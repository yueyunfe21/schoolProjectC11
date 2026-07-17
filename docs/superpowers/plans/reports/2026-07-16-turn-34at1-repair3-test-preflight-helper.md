# CR271 TURN-34AT1 Repair #3 Deterministic Test-Only Preflight Helper

## Role And Scope

- Role: `CR271 Internal helper`; not the implementation owner, reviewer, parent, or approver.
- Scope: freeze the deterministic test-only repair recipe for the three items already accepted by TURN-34AT1
  Parent Review #4: legal `FAILED` fixture, strict `696a12b0` 30-second same-team defer, and null
  `clearPointerIfOverRegion` / `pixelChangeProbe` on the minimal Stage-1 CAPTURE.
- This report does not claim, implement, review, approve, block, or change the status of TURN-34AT1/TURN-34A.
- Helper write set: only this report. No Java, card, plan, CR dashboard, POM, resource, fixture, or other report was
  modified.
- No Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input, or Git mutation was run.

## Authorities Read And Current Snapshot

The preflight fully read the current repository instructions and requested authorities: `AGENTS.md`,
`docs/DHXY_CONTEXT.md`, the latest CR271 material at the top of `docs/ACTIVE_WORK.md`, authority-plan Sections
14-19, the approved HTTPS turn protocol design, `docs/业务逻辑.md`, both repository statuses, the complete
TURN-34AT1 child card through physical true EOF, and all 1,026 lines of the frozen named test. The two independent
AT1 review reports and their finding-preflight reports were also cross-checked; they are evidence, not inherited
approval.

Snapshot before this helper report was written:

| Object | Frozen identity |
| --- | --- |
| DHXY | branch `thin-client-design`; HEAD `0114604e1ff5f15491d2910959c45252e893d04f`; porcelain `-uall`: 43 modified, 1 deleted, 685 untracked |
| Cloud | branch `navigation-migration`; HEAD `3b988caa010254973e03342272e6d1d6a9685b01`; porcelain `-uall`: 9 modified, 541 untracked |
| Cloud production | `AutoCombatService.java`; 852 lines; SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` |
| Cloud named test | `AutoCombatServiceTurnContractTest.java`; 1,026 lines / 22 `@Test`; SHA-256 `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292` |
| Child-card true EOF | `PARENT-REVIEW-4 REPAIR-3-REQUIRED ... EXTERNAL-D-FRESH-RESTART TEST-ONLY CLAIM-REQUIRED` at `2026-07-16T11:03:03.155-04:00` |
| Current CR271 top | `2026-07-16 11:15`; fresh D is the designated future TURN-34AT1 Repair #3 lane, with no old task/heartbeat treated as owner |
| Business baseline | `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no approved business difference |

Line anchors below are against the frozen test SHA above. Earlier in-file insertions will move later numeric lines;
the method and statement anchors are authoritative during implementation.

## Repair Map

Only the existing named test needs Java changes. Keep the test count at 22 and do not create a new test method,
test class, fixture, resource, production hook, or helper file.

### 1. Legal `FAILED` Outcome Fixture

Current anchors:

- `stage1OutcomeFailuresKeepInCombatWithExactlyOneCommand()` at test lines `496-523`; the public probe returns at
  `512-513` and the raw scripted command result is retained in `harness.port.results`.
- `everyStage1InvocationEmitsAFreshCanonicalUuidAcrossTerminalAndPositiveCases()` at `533-563`; its fifth reply
  uses nominal `FAILED` at `544` and must remain part of the same eight-call service sequence.
- `nonCompletedOutcome(...)` at `848-869`; it currently gives every status `failedStepIndex=null` and every step
  `NOT_RUN`.
- Protocol authority: `TurnProtocolValidator.java:355-370` requires legal `FAILED` to identify a failed step and
  mark that result `FAILED`. `TurnInvocationResult.java:49-66` validates the outcome before returning it.
  Therefore the current fixture reaches `BattleRadarService.java:627-630` generic exception fallback instead of
  the legal terminal path at `570-577`.

Deterministic in-place repair:

1. Add the same-file import for `com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator`.
2. In `nonCompletedOutcome(...)`, preserve the existing status/code/message/frame behavior, but derive the shape
   by status:

```java
boolean failed = status == TurnOutcome.Status.FAILED;
List<TurnStepResult> results = action.steps().stream()
        .map(step -> new TurnStepResult(
                step.index(),
                step.type(),
                failed && step.index() == 0
                        ? TurnStepResult.Status.FAILED
                        : TurnStepResult.Status.NOT_RUN,
                status.name(),
                null,
                null))
        .toList();

TurnOutcome outcome = new TurnOutcome(
        1,
        action.actionId(),
        window,
        status,
        failed ? 0 : null,
        status.name(),
        "turn outcome " + status,
        results,
        null);
```

For this one-step CAPTURE action, legal `FAILED` is exactly `failedStepIndex=0` plus index-0
`TurnStepResult.Status.FAILED`; there is no later step to mark `NOT_RUN`. `STOPPED` and
`DUPLICATE_OR_UNCERTAIN` retain `failedStepIndex=null` and index-0 `NOT_RUN`.

3. In `stage1OutcomeFailuresKeepInCombatWithExactlyOneCommand()`, immediately after the public probe returns,
   retrieve `harness.port.results.get(0).outcome()` and validate it in the test body, outside the scripted reply
   lambda:

```java
TurnOutcome rawOutcome = harness.port.results.get(0).outcome();
assertNotNull(rawOutcome);
TurnProtocolValidator.requireValid(rawOutcome);
assertEquals(status, rawOutcome.status());
assertEquals(1, rawOutcome.stepResults().size());
assertEquals(TurnStepType.CAPTURE, rawOutcome.stepResults().get(0).type());
assertNull(rawOutcome.frame());
if (status == TurnOutcome.Status.FAILED) {
    assertEquals(Integer.valueOf(0), rawOutcome.failedStepIndex());
    assertEquals(TurnStepResult.Status.FAILED, rawOutcome.stepResults().get(0).status());
} else {
    assertNull(rawOutcome.failedStepIndex());
    assertEquals(TurnStepResult.Status.NOT_RUN, rawOutcome.stepResults().get(0).status());
}
```

The validator call must remain after the public probe. Putting it only inside the reply lambda would let
`BattleRadarService` catch its `RuntimeException` and recreate the current false-positive route.

Do not otherwise edit the shared eight-call method. It must still enqueue four command-terminal replies, three
protocol-legal outcome-terminal replies, then one real completed battle-flag capture; invoke eight times; observe
eight commands; exhaust replies; and prove eight canonical pairwise-distinct UUIDs with zero Stage-2/3, retry,
resend, compensation, fallback, or second action.

### 2. Strict `696a12b0` 30-Second Same-Team Defer

Current anchors:

- `refreshDuePanelVerifyGateKeepsThirtySecondTeamSharing()` at `632-651` already correctly proves a different
  window on the same team is deferred at `+29,999ms` and allowed at `+30,000ms`; leave this method unchanged.
- `refreshDueGateDoesNotLockOutTheSameWindow()` at `655-662` incorrectly expects the same team and same window at
  `+10ms` to be allowed.
- Current Cloud production `AutoCombatService.java:33,812-827` and the authoritative Git object
  `696a12b0:AutoCombatService.java:33,817-833` key by nonblank `teamKey`; `windowId` is only the fallback when the
  team key is absent. No same-window exception exists.

Replace the second method in place; rename its JavaDoc and method so neither continues to claim a same-window
exemption:

```java
/** The 30s team gate also defers the same window when it re-reserves for the same team. */
@Test
void refreshDueGateDefersTheSameTeamAndSameWindowInsideThirtySeconds() {
    AutoCombatService.RefreshDuePanelVerifyGate gate =
            new AutoCombatService.RefreshDuePanelVerifyGate();
    long now = 2_000_000L;

    AutoCombatService.RefreshDuePanelVerifyDecision first =
            gate.reserveIfAllowed("team-1", "window-34a", now);
    AutoCombatService.RefreshDuePanelVerifyDecision sameWindowTooSoon =
            gate.reserveIfAllowed("team-1", "window-34a", now + 10L);

    assertFalse(first.deferred());
    assertTrue(sameWindowTooSoon.deferred());
    assertEquals(29_990L, sameWindowTooSoon.retryAfterMs());
    assertEquals(10L, sameWindowTooSoon.lastTeamRefreshAgeMs());
}
```

This is a test correction to the existing strict baseline, not authorization for a production same-window
exception or any timing/business change.

### 3. Minimal CAPTURE Inner Mechanics Must Be Null

Current anchor: `stage1BattleFlagEntersCombatWithExactlyOneCanonicalCapture()` at `379-434`, specifically the
capture-shape block at `404-416`. It already locks all five non-CAPTURE outer union fields to null, then asserts
`step.capture()` non-null, exact region, and `UPLOAD_IMAGE`, but does not lock the two optional inner mechanics.

Immediately after current `assertNotNull(step.capture())` at line `414`, add:

```java
assertNull(step.capture().clearPointerIfOverRegion(),
        "a minimal Stage-1 capture performs no pointer-clear mechanics");
assertNull(step.capture().pixelChangeProbe(),
        "a minimal Stage-1 capture performs no Ctrl-hover pixel-change probe");
```

Keep the existing exact `(1074,680,51,20)` ROI, `UPLOAD_IMAGE`, 120-second timeout, exact metadata/action
correlation, raw PNG bytes/SHA, single command, and Stage-2/3 zero-command assertions unchanged.

## Zero-Extra-File Boundary

Repair #3's exact allowed modify set remains:

1. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`.
2. Append-only delivery/owner record in
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md` by the authorized
   implementation/parent owner.

There is no third file. In particular, do not modify or add:

- `AutoCombatService.java`, `BattleRadarService.java`, `TurnGameClient.java`, `TurnInvocationResult.java`, any
  protocol/validator/action-factory source, caller, POM, resource, fixture, or second test;
- TURN-34A parent card, `ACTIVE_WORK.md`, authority plan, dashboard, or unrelated report as part of the Java repair;
- a production/test hook, copied reducer, reflection/source scan, wall-clock polling, session/owner/ledger/TTL,
  retry/replay/resend, cleanup, park/yield, or alternate business gate.

Inside the named test, the only intended Java deltas are one protocol-validator import, the existing
`nonCompletedOutcome(...)` helper, the existing outcome-terminal test body, the existing same-window gate test,
and two assertions in the existing positive CAPTURE test. Keep 22 `@Test` methods and all other test behavior.

## Deterministic Handoff Checks

After an authorized implementation owner delivers new bytes, static re-review should confirm all of the following
on one new frozen test SHA:

1. Production remains exactly SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
2. The raw `FAILED` outcome is protocol-valid after the public probe and has failed index 0 / CAPTURE `FAILED`;
   `STOPPED` and `DUPLICATE_OR_UNCERTAIN` retain their legal null-index / `NOT_RUN` shapes.
3. The shared sequence still proves exactly eight commands, eight canonical distinct UUIDs, exhausted replies,
   and no Stage-2/3/retry/fallback.
4. Same team + same window at `+10ms` deterministically yields `deferred=true`, `retryAfterMs=29_990`, and
   `lastTeamRefreshAgeMs=10`; no production exception was introduced.
5. The positive Stage-1 CAPTURE explicitly has both optional inner mechanics null while all prior outer shape,
   ROI, timeout, metadata, and raw-PNG assertions remain.
6. The test count remains 22 and the Java diff has no file beyond the one named test.

The authorized named-test/build gates remain future owner/parent work under authority-plan Section 19. This helper
did not run them and makes no prediction or approval claim about their result.

无已批准业务差异；按 `696a12b0`、最小 HTTPS JSON turn 与 exact-window Stage-1 合同等价修复测试。

TRUE_EOF PRECHECK_COMPLETE
