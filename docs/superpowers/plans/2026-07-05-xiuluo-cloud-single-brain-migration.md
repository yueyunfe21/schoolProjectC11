# Xiuluo Cloud Single Brain Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move 修罗 V2 to a single cloud-owned business brain where cloud decides every phase transition, retry, recovery, and round completion, while the local client only executes commands, collects facts, and enforces safety.

**Architecture:** Introduce an authoritative `XIULUO_BRAIN` cloud session protocol with `sessionId`, `stateSeq`, `phaseToken`, and `actionId`. The enabled production path must not compute a local business `nextPhase`; local phase code is refactored into action executors and fact reporters. Existing local-only safety remains local: stop/pause, HWND/window binding, input queue, screenshot capture, coordinate bounds, pathing watcher facts, and local-only team-return matching.

**Tech Stack:** Java/Spring Boot, existing cloud decision sidecar framework, JavaFX task runner, existing `TaskTransactionRunner`, focused Java source guards, Maven compile/test-compile, DHXY fresh runtime logs.

---

## Non-Negotiable P0 Rules

- 修罗 enabled cloud-brain path has exactly one business brain: `XIULUO_BRAIN`.
- `TASK_POLICY` after-local-outcome must not be used as the authoritative 修罗 implementation.
- Local code must not fallback to old 修罗 phase/retry/recovery decisions when cloud is unavailable, invalid, or rejected. It must fail closed or stop safely.
- Local stop/pause/window/input safety can reject an action, but cannot choose the next business phase.
- Every cloud action must carry `sessionId`, `stateSeq`, `phaseToken`, `actionId`, `ttlMs`, and `reason`.
- Every phase transition in fresh runtime must have a matching `XIULUO_BRAIN` execute log.
- Each implementation CR must pass local tests before user fresh runtime. If fresh runtime breaks, stop the migration and open a repair card for the last completed CR.

## Sprint / CR Map

| CR | Purpose | Local Test Gate | Fresh Runtime Gate |
| --- | --- | --- | --- |
| CR192 | Parent card and project contract | Docs/dashboard only | None |
| CR193 | `XIULUO_BRAIN` protocol/service skeleton | Protocol parse/fail-closed tests + compile | None |
| CR194 | Dev cloud brain session engine | Cloud session/stateSeq/action tests | None |
| CR195 | Client brain loop scaffold behind flag | Fake cloud loop tests; old path unaffected | None |
| CR196 | Phase report/action model and source guards | Guards prove enabled path cannot use local `nextPhase` | None |
| CR197 | Hot-start and startup initial phase cloud-owned | Hot-start fact tests; no local phase jump | Fresh Node A: startup/accept smoke |
| CR198 | Accept/task-objective/tracker shortcut phases cloud-owned | Accept/dialog/tracker fake-cloud tests | Fresh Node B: reach tracker green/pathing |
| CR199 | Route/target/click/enter-battle phases cloud-owned | Route/target/action-report tests + required replays for click-target changes | Fresh Node C: enter combat |
| CR200 | Combat/return/team-return/round-complete/recovery cloud-owned | Combat/return/recovery tests; fail-closed tests | Fresh Node D: one full round |
| CR201 | Remove old 修罗 business-brain entry points from enabled path | Source guards: no `TASK_POLICY` authority/no local recover fallback | Fresh Node E: 5-10 round run |
| CR202 | Long-run readiness and docs cleanup | Full focused suite + compile/test-compile/dashboard | Fresh Node F: long run, then close parent |

## Fresh Runtime Break Rule

After each fresh node:

- If logs match the node expectation, continue to the next CR.
- If anything breaks, do not continue implementing later CRs.
- Record the failure in CR192 and the specific child CR card.
- Open a repair sub-card or reopen the child CR.
- Run local focused tests first after repair, then repeat the same fresh node.

This keeps the migration one-way. Later cards must not silently rewrite earlier accepted behavior. If a later card needs an earlier contract change, it must reopen the earlier CR and repeat its fresh node.

---

### Task 1: CR192 Parent Card And Project Contract

**Files:**
- Modify: `docs/PACKAGE_ARCHITECTURE.md`
- Modify: `docs/ACTIVE_WORK.md`
- Modify: `docs/cr-dashboard-data.js` via generator
- Reference: `docs/HYBRID_CLOUD_WORKFLOW.md`

- [ ] **Step 1: Add CR192 parent card**

Add a CR table row and detailed card describing the whole 修罗 single-brain migration. Required wording:

```text
修罗启用云端脑以后，phase/hot-start/retry/recovery/round done/fail 只能由 XIULUO_BRAIN 决定。
本地只执行 action、采集 facts、执行 stop/pause/window/input safety。
```

- [ ] **Step 2: Add child CR rows CR193-CR202**

Each row must include owner/status/scope/summary and its fresh node, if any.

- [ ] **Step 3: Record current planning baseline in ACTIVE_WORK**

Record branch, current dirty status note, and that this pass is docs/planning only.

- [ ] **Step 4: Sync dashboard**

Run:

```powershell
node scripts/generate-cr-dashboard-data.js
```

Expected: exits `0`, updates `docs/cr-dashboard-data.js`.

---

### Task 2: CR193 `XIULUO_BRAIN` Protocol And Service Skeleton

**Files:**
- Create: `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainStartRequest.java`
- Create: `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainStepRequest.java`
- Create: `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainResponse.java`
- Create: `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainActionType.java`
- Create: `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainCloudDecisionService.java`
- Modify: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionServiceId.java`
- Test: `src/test/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainCloudDecisionServiceTest.java`

- [ ] **Step 1: Write failing protocol tests**

Test cases:

```text
valid response with sessionId/stateSeq/phase/actionId/reason is accepted
missing sessionId is rejected
missing reason is rejected
invalid XiuluoPhase is rejected
expired ttl is rejected
wrong windowId/taskRunId is rejected
coordinator inactive or cloud timeout returns fail-closed, not local passthrough
```

- [ ] **Step 2: Implement immutable request/response models**

Use Lombok `@Value` + `@Builder`. Stable values crossing the boundary must be enums, not loose strings.

- [ ] **Step 3: Implement required-execute service**

The service must not expose a `localDecision` fallback. It returns only:

```text
ACCEPTED_CLOUD_COMMAND
LOCAL_SAFETY_DENIED
CLOUD_REQUIRED_FAILURE
```

- [ ] **Step 4: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainCloudDecisionServiceTest" test
mvn -q -DskipTests compile
```

Expected: both exit `0`.

**Fresh runtime:** none. This CR must not affect production flow.

---

### Task 3: CR194 Dev Cloud Brain Session Engine

**Files:**
- Modify/Create under: `src/main/java/com/bot/dhxy/cloud/xiuluo/`
- Modify: `src/main/java/com/bot/dhxy/ui/CloudDecisionDevSidecarService.java` or current dev sidecar handler
- Test: `src/test/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainDevServerTest.java`

- [ ] **Step 1: Write fake session tests**

Test cases:

```text
start creates sessionId and stateSeq=1
step with matching sessionId/stateSeq advances stateSeq
old stateSeq is rejected
wrong taskRunId/windowId is rejected
phaseToken mismatch is rejected
action outcome is accepted once only
```

- [ ] **Step 2: Implement dev session store**

The dev brain can initially mirror the known 修罗 happy-path policy, but it must be cloud-side state. Do not ask local code for `localNextPhase`.

- [ ] **Step 3: Add structured logs**

Required log vocabulary:

```text
xiuluo.brain.start
xiuluo.brain.step.request
xiuluo.brain.step.response
xiuluo.brain.action.outcome
xiuluo.brain.failClosed
```

- [ ] **Step 4: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainDevServerTest,XiuluoBrainCloudDecisionServiceTest" test
mvn -q -DskipTests compile
```

**Fresh runtime:** none. This CR must not affect production flow.

---

### Task 4: CR195 Client Brain Loop Scaffold Behind Flag

**Files:**
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Modify: `src/main/resources/application.properties`
- Test: `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoBrainLoopWiringTest.java`

- [ ] **Step 1: Add disabled config flag**

Add:

```properties
cloud.services.xiuluo-brain.execute-enabled=false
cloud.services.xiuluo-brain.fallback=FAIL_CLOSED
```

- [ ] **Step 2: Write fake-cloud loop test**

Test:

```text
when enabled and cloud returns PREPARE_ROUND then ACCEPT_TASK_NAVIGATE_TO_NPC,
XiuluoTaskV2 consumes cloud next phase and does not use local outcome next phase.
```

- [ ] **Step 3: Implement a separate enabled-path loop**

Add a new method shaped like:

```java
private TaskRunResult runRoundWithXiuluoBrain(TaskExecutionContext context, int round)
```

Do not delete the old path yet. When the flag is off, existing 修罗 behavior remains. When the flag is on, no local fallback is allowed.

- [ ] **Step 4: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainLoopWiringTest" test
mvn -q -DskipTests test-compile
mvn -q -DskipTests compile
```

**Fresh runtime:** none. The flag remains off.

---

### Task 5: CR196 Phase Report / Action Model And Source Guards

**Files:**
- Create: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoPhaseReport.java`
- Create: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoActionExecutionResult.java`
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Test: `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoSingleBrainSourceGuardTest.java`

- [ ] **Step 1: Write source guard**

The enabled cloud-brain path must not call:

```text
applyTaskPolicyCloudDecision(...)
TaskPolicyCloudDecisionService.decide(...)
TaskRecoveryCloudDecisionService.decide(...)
retryCurrentOrRecover(...)
restartRoundAfterPhaseFailure(...)
restartRoundAfterLoopGuard(...)
resolveTaskHotStart(...)
```

It must also reject `localDecision=...next=...` logs in the enabled brain path.

- [ ] **Step 2: Add report/action models**

Reports contain facts only:

```text
phase
status
facts
actionResult
waitSpec
errorReason
safetyDeniedReason
evidencePaths
```

No `nextPhase` field is allowed in `XiuluoPhaseReport`.

- [ ] **Step 3: Verify**

Run:

```powershell
javac -encoding UTF-8 -d target\cr196-guard src\test\java\com\bot\dhxy\task\xiuluo\XiuluoSingleBrainSourceGuardTest.java
java -cp target\cr196-guard com.bot.dhxy.task.xiuluo.XiuluoSingleBrainSourceGuardTest
mvn -q -DskipTests compile
```

**Fresh runtime:** none. The flag remains off.

---

### Task 6: CR197 Hot-Start And Startup Initial Phase Cloud-Owned

**Files:**
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Create/modify: `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoBrainHotStartWiringTest.java`

- [ ] **Step 1: Write hot-start tests**

Test cases:

```text
combat fact is reported but local does not jump to WAIT_COMBAT
tracker-green fact is reported but local does not jump to AFTER_ACCEPT_MAINTENANCE_CHECK
return item available fact is reported but local does not use item before cloud command
cloud initial phase is applied exactly
invalid cloud initial phase fail-closes
```

- [ ] **Step 2: Implement `collectHotStartFacts`**

This method may inspect combat, dialog, tracker, current map, and return item availability. It must not click return item or choose phase.

- [ ] **Step 3: Wire start request**

When cloud-brain flag is enabled:

```text
start round -> collect facts -> XIULUO_BRAIN start -> execute returned phase/action
```

- [ ] **Step 4: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainHotStartWiringTest,XiuluoBrainLoopWiringTest" test
mvn -q -DskipTests compile
```

**Fresh Node A: startup/accept smoke**

User runs one 修罗 start with cloud-brain enabled and dev sidecar active.

Expected logs:

```text
xiuluo.brain.start
facts: combat/tracker/dialog/currentMap
cloud initial phase/action accepted
no resolveTaskHotStart local phase jump
no startup return item click unless cloud action explicitly says USE_RETURN_ITEM
```

If startup cannot reach the first cloud command or old hot-start logs appear, stop and repair CR197.

---

### Task 7: CR198 Accept / Objective / Tracker Shortcut Cloud-Owned

**Files:**
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Modify dev brain policy from CR194
- Test: `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoBrainAcceptTrackerWiringTest.java`

- [ ] **Step 1: Write fake-cloud tests**

Test a cloud-driven sequence:

```text
PREPARE_ROUND
ACCEPT_TASK_NAVIGATE_TO_NPC
ACCEPT_TASK_CLICK_NPC
ACCEPT_TASK_DIALOG
READ_OBJECTIVE
AFTER_ACCEPT_MAINTENANCE_CHECK
TRY_TRACKER_SHORTCUT
```

Each local executor must return facts/report only. The next phase must come from cloud response.

- [ ] **Step 2: Refactor accept-related executors**

Keep existing action behavior, but report:

```text
npcClick submitted/verified
dialog action key/status
accept snapshot path
objective parse status
tracker panel found/green count/click point
maintenance due facts
```

- [ ] **Step 3: Preserve CR191 order**

The no-maintenance sequence remains:

```text
accept snapshot -> start-exit-prepath -> tracker green clicked -> common box/deferred recovery
```

Cloud decides the order; local executor must not inject box/deferred before green click.

- [ ] **Step 4: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainAcceptTrackerWiringTest,XiuluoSingleBrainSourceGuardTest" test
mvn -q -DskipTests test-compile
mvn -q -DskipTests compile
```

**Fresh Node B: reach tracker green/pathing**

Run 修罗 until at least one tracker green link is clicked.

Expected logs:

```text
each phase transition has XIULUO_BRAIN response
tracker facts are reported to cloud
cloud action executes tracker green click
no TASK_POLICY keep-local / no local nextPhase authority
CR191 order still holds
```

If accept or tracker breaks, stop and repair CR198.

---

### Task 8: CR199 Route / Target / Enter-Battle Cloud-Owned

**Files:**
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Modify dev brain policy from CR194
- Test: `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoBrainRouteEnterBattleWiringTest.java`

- [ ] **Step 1: Write fake-cloud tests**

Test cloud-driven phases:

```text
WAIT_TRACKER_SHORTCUT_PATHING
NAVIGATE_TO_TARGET
CLICK_TARGET_NPC
CONFIRM_ENTER_BATTLE
WAIT_COMBAT
```

Local reports navigation/pathing/click/dialog/combat facts. Cloud chooses fallback or next phase.

- [ ] **Step 2: Remove local route fallback authority from enabled path**

Enabled cloud-brain path must not let these local methods choose business next phase:

```text
navigationOutcome(...)
fallbackFromShortcut(...)
recoverTargetNavigationFailure(...)
recoverTargetClickFailure(...)
```

They can still execute action and report facts.

- [ ] **Step 3: Visual/click replay rule**

If this CR changes any click coordinate or visual matching algorithm, add testcase replay and marked output before fresh runtime. If it only changes phase authority, no new replay is required.

- [ ] **Step 4: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainRouteEnterBattleWiringTest,XiuluoSingleBrainSourceGuardTest" test
mvn -q -DskipTests compile
```

**Fresh Node C: enter combat**

Run 修罗 until battle entry.

Expected logs:

```text
cloud action commands route/target/enter battle
local reports pathing/combat facts
WAIT_COMBAT reached only by XIULUO_BRAIN command
no local route fallback authority
```

If navigation, target click, or enter battle breaks, stop and repair CR199.

---

### Task 9: CR200 Combat / Return / Team Return / Recovery Cloud-Owned

**Files:**
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Modify dev brain policy from CR194
- Test: `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoBrainCombatReturnRecoveryWiringTest.java`

- [ ] **Step 1: Write combat/return tests**

Test cases:

```text
combat exit fact does not locally choose RETURN_HOME
return item verified fact does not locally choose WAIT_TEAM_RETURN/ROUND_DONE
team return signal fact is local-only detector but cloud decides wait/complete
post-combat idle timeout fact does not locally reaccept
pre-combat watchdog fact does not locally reaccept
phase loop guard fact does not locally restart
cloud timeout/invalid fail-closes
```

- [ ] **Step 2: Refactor combat and return executors**

Keep action execution:

```text
wait combat event
use return item
verify start map
wait team return local template
navigate back if cloud commands it
```

But only report facts. Cloud chooses next phase.

- [ ] **Step 3: Replace recovery authority**

Enabled path must not call `TaskRecoveryCloudDecisionService` with a local candidate. Recovery is a normal `XIULUO_BRAIN` decision based on failure facts.

- [ ] **Step 4: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainCombatReturnRecoveryWiringTest,XiuluoSingleBrainSourceGuardTest" test
mvn -q -DskipTests test-compile
mvn -q -DskipTests compile
```

**Fresh Node D: one full round**

Run one full 修罗 round with cloud-brain enabled.

Expected logs:

```text
WAIT_COMBAT/RETURN_HOME/WAIT_TEAM_RETURN/ROUND_DONE all chosen by XIULUO_BRAIN
return item verified path works
team return detector remains local-only fact, no TEAM_RETURN_POLICY cloud request
no local recovery/reaccept after timeout/watchdog
```

If combat/return/round completion breaks, stop and repair CR200.

---

### Task 10: CR201 Disable Old Local Brain In Enabled Path

**Files:**
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Modify: `src/main/java/com/bot/dhxy/cloud/task/TaskPolicyCloudDecisionService.java` only if needed to prevent 修罗 authority
- Test: `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoBrainNoDualBrainGuardTest.java`

- [ ] **Step 1: Write no-dual-brain guard**

The enabled path must fail if it references:

```text
applyTaskPolicyCloudDecision
TaskPolicyCloudDecisionService.decide
TaskRecoveryCloudDecisionService.decide
resolveTaskHotStart
retryCurrentOrRecover
restartRoundAfterPhaseFailure
restartRoundAfterLoopGuard
localDecision=...next=
LOCAL_PASSTHROUGH
keep local outcome
```

- [ ] **Step 2: Keep old path only behind disabled legacy flag**

The old path may remain for rollback while `cloud.services.xiuluo-brain.execute-enabled=false`. It cannot be reachable when the flag is true.

- [ ] **Step 3: Verify**

Run:

```powershell
mvn -q -Dtest="XiuluoBrainNoDualBrainGuardTest,XiuluoSingleBrainSourceGuardTest" test
mvn -q -DskipTests compile
```

**Fresh Node E: 5-10 round run**

Run 5-10 修罗 rounds.

Expected logs:

```text
all phase transitions use XIULUO_BRAIN
no TASK_POLICY/TASK_RECOVERY authority for 修罗
no local fallback after cloud errors
common box/deferred recovery order remains correct
no performance regression obvious enough to block
```

If a regression appears, stop and repair CR201 or the last responsible child CR.

---

### Task 11: CR202 Long-Run Readiness And Documentation Cleanup

**Files:**
- Modify: `docs/PACKAGE_ARCHITECTURE.md`
- Modify: `docs/HYBRID_CLOUD_WORKFLOW.md`
- Modify: `docs/业务逻辑.md`
- Modify: `docs/ACTIVE_WORK.md`
- Modify: `docs/cr-dashboard-data.js` via generator

- [ ] **Step 1: Update business logic docs**

Document the new 修罗 authority model:

```text
XIULUO_BRAIN owns phase/hot-start/retry/recovery/round completion.
Local owns execution/facts/safety.
Team return stays local-only detector/fact.
```

- [ ] **Step 2: Run local verification suite**

Run:

```powershell
mvn -q -Dtest="XiuluoBrain*Test,*Xiuluo*Brain*Test" test
mvn -q -DskipTests test-compile
mvn -q -DskipTests compile
node scripts/generate-cr-dashboard-data.js
```

- [ ] **Step 3: Fresh Node F long run**

Run a longer 修罗 fresh runtime, then optionally mixed 五倍/修罗 if user wants cross-task confidence.

Expected logs:

```text
XIULUO_BRAIN success across multiple rounds
no dual-brain logs
no local recovery authority
no stuck post-combat idle
no team-return cloud request
acceptable latency
```

- [ ] **Step 4: Close parent only after evidence**

CR192 stays Review until CR193-CR202 are either Done or intentionally split into follow-up cards with user approval.

---

## Execution Policy

- 谢帅/main agent manages scope, cards, review, and final judgment. 谢帅 does not write Java business implementation.
- Each implementation CR gets one worker and at least two independent reviewers.
- Reviewer #1 checks single-brain authority and hidden local fallback.
- Reviewer #2 checks local safety, window/input correctness, and regression risk.
- Any P0/P1/P2 finding blocks fresh runtime.
- Every CR update must be written into `docs/PACKAGE_ARCHITECTURE.md` and synced with `node scripts/generate-cr-dashboard-data.js`.

