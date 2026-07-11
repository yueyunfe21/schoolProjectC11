# TaskClassifierCloud Shadow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Track every checkbox as work completes.

**Goal:** Add the first real Hybrid Cloud hook: 五倍/修罗 tracker-title task classification in Shadow mode. The local tracker classifier remains the only executed business decision; cloud output is logged for comparison only.

**Architecture:** Keep the existing title-template matching in `TaskTrackerPanelService` as the source of truth. Add a small shadow reporter around the final local result, using `CloudDecisionCoordinator` and `CloudDecisionServiceId.TASK_CLASSIFIER`. The hook must produce per-read samples when enabled, without changing task phase, task key, clicks, navigation, or retry behavior.

**Tech Stack:** Java 17, Spring services, existing cloud-decision skeleton, existing Maven test style.

---

## Scope Rules

- Do not change the 五倍 title-template order, thresholds, image paths, OCR, green-link scan, or task branching.
- 修罗 uses the same shadow-only rule: report `xiuluo.tracker` / `NOT_FOUND`, but never use cloud output for shortcut click/pathing decisions.
- Do not use cloud `effectiveDecision` to choose a branch.
- Do not add real HTTP calls.
- Do not add click/navigation/testcase replay work; this card does not change visual matching targets or click points.
- Default runtime behavior must remain quiet/inert unless cloud and the service shadow flag are explicitly enabled.

## File Map

- Modify: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java`
  - Add a no-log readiness helper so high-frequency business hooks can avoid disabled-mode log spam.
- Create: `src/main/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowService.java`
  - Builds a `TASK_CLASSIFIER` request from the local 五倍 tracker result and calls shadow mode only when active.
- Modify: `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
  - Inject the shadow service and call it after each local 五倍 panel read result is known.
- Modify: `src/main/resources/application.properties`
  - Add disabled-by-default service-level `task-classifier` flags.
- Create: `src/test/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowServiceTest.java`
  - Tests request fields and no-call disabled behavior.
- Create: `src/test/java/com/bot/dhxy/cloud/task/WubeiTaskClassifierCloudShadowWiringTest.java`
  - Source/wiring guard that the 五倍 tracker reader calls the shadow hook but does not consume cloud decisions.
- Create: `src/test/java/com/bot/dhxy/cloud/task/XiuluoTaskClassifierCloudShadowWiringTest.java`
  - Source/wiring guard that the 修罗 tracker reader calls the shadow hook but does not consume cloud decisions.
- Modify: `docs/HYBRID_CLOUD_WORKFLOW.md`
  - Record CR-HC-002 and the enable/rollback switches.
- Modify: `docs/ACTIVE_WORK.md`
  - Record baseline, implementation, and verification.

## Task 1: Coordinator Active Guard

**Files:**

- Modify: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java`
- Modify: `src/test/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinatorTest.java`

- [ ] Add a public helper:

```java
public boolean isActive(CloudDecisionServiceId serviceId)
```

Expected behavior:

- `false` when global `cloud.enabled=false`.
- `false` when `serviceId == null`.
- `false` when both service `shadowEnabled=false` and `executeEnabled=false`.
- `true` when global cloud is enabled and either shadow or execute is enabled for that service.

- [ ] Do not log from `isActive(...)`.
- [ ] Keep existing `shadow(...)` behavior and tests passing.
- [ ] Add focused test coverage for the active helper.

## Task 2: TaskClassifier Shadow Service

**Files:**

- Create: `src/main/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowService.java`
- Create: `src/test/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowServiceTest.java`

- [ ] Implement a Spring service with constructor-injected `CloudDecisionCoordinator`.
- [ ] Add method:

```java
public void shadowWubeiTrackerResult(String source, TaskTrackerPanelReadResult result)
```

- [ ] If `coordinator.isActive(TASK_CLASSIFIER)` is false, return without calling `shadow(...)`.
- [ ] Build `localDecision` as:
  - `result.titleTemplate.taskKey` when `result.found=true` and the title template exists;
  - `NOT_FOUND` otherwise.
- [ ] Build `CloudDecisionRequest` with:
  - `serviceId=TASK_CLASSIFIER`
  - `taskCode=wubei`
  - `phase=tracker-title-classification`
  - `localDecision=<same localDecision>`
  - stable trace id prefix `wubei-task-classifier:`
  - context fields:
    - `source`
    - `found`
    - `taskKey`
    - `title`
    - `yellowText`
    - `greenLinkCount`
    - `probeObjective`
    - `detailRawPath`
    - `detailYellowPath`
- [ ] Call `coordinator.shadow(request, localDecision)` and ignore the returned effective decision.
- [ ] Tests must prove:
  - disabled cloud does not call a throwing client;
  - enabled shadow sends `TASK_CLASSIFIER`, `wubei`, phase, task key, and context;
  - empty/not-found result sends `NOT_FOUND` when enabled.

## Task 3: 五倍 Tracker Hook

**Files:**

- Modify: `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
- Create: `src/test/java/com/bot/dhxy/cloud/task/WubeiTaskClassifierCloudShadowWiringTest.java`

- [ ] Inject `TaskClassifierCloudShadowService`.
- [ ] In `readWubeiTrackerPanel(String source)`, when crop is missing, call the shadow hook with `TaskTrackerPanelReadResult.empty()` before returning.
- [ ] In `readWubeiTrackerPanelFromSnapshot(...)`, when snapshot is invalid or title is missed, call the shadow hook with `TaskTrackerPanelReadResult.empty()` before returning.
- [ ] In `readWubeiTrackerDetail(...)`, build the existing local result exactly as before, call the shadow hook, then return the same local result.
- [ ] On image read failure / IO exception, call the shadow hook with `TaskTrackerPanelReadResult.empty()` before returning.
- [ ] Do not read or branch on `CloudDecisionResult`.
- [ ] Wiring/source test must guard that the hook exists and no cloud result/effective decision is consumed by `TaskTrackerPanelService`.

## Task 4: Config And Docs

**Files:**

- Modify: `src/main/resources/application.properties`
- Modify: `docs/HYBRID_CLOUD_WORKFLOW.md`
- Modify: `docs/ACTIVE_WORK.md`

- [ ] Add disabled-by-default config:

```properties
cloud.services.task-classifier.shadow-enabled=false
cloud.services.task-classifier.execute-enabled=false
cloud.services.task-classifier.execute-percent=0
cloud.services.task-classifier.fallback=LOCAL
```

- [ ] Add CR-HC-002 note to `docs/HYBRID_CLOUD_WORKFLOW.md`:
  - service id;
  - hook point;
  - local baseline;
  - request fields;
  - shadow log;
  - fallback/rollback switch;
  - runtime verification command/log keyword.
- [ ] Record baseline and verification in `docs/ACTIVE_WORK.md`.

## Verification

Run:

```powershell
mvn -q -DskipTests compile
mvn -q "-Dtest=CloudDecisionPropertiesTest,CloudDecisionCoordinatorTest,CloudDecisionSkeletonWiringTest,TaskClassifierCloudShadowServiceTest,WubeiTaskClassifierCloudShadowWiringTest" test
```

Expected:

- Compile passes.
- Focused tests pass.
- No visual/click/navigation replay required because no click target, template threshold, OCR wash, or navigation coordinate changed.

## Runtime Acceptance

With default config:

```properties
cloud.enabled=false
cloud.services.task-classifier.shadow-enabled=false
```

Expected:

- 五倍/修罗 behavior unchanged.
- No high-frequency disabled cloud log spam from 五倍 tracker reads.

With shadow enabled:

```properties
cloud.enabled=true
cloud.services.task-classifier.shadow-enabled=true
```

Expected 五倍 logs:

```text
cloud.decision serviceId=TASK_CLASSIFIER mode=SHADOW ... taskCode=wubei phase=tracker-title-classification ...
localDecision=wubei.* cloudDecision=wubei.* effectiveDecision=wubei.* executed=false
```

Rollback:

```properties
cloud.services.task-classifier.shadow-enabled=false
cloud.services.task-classifier.execute-enabled=false
cloud.services.task-classifier.fallback=LOCAL
```

## Manager Review Checklist

- [ ] Confirm `TaskTrackerPanelService` still returns the local `TaskTrackerPanelReadResult`.
- [ ] Confirm Wubei dark-thunder/probe/chained-combat helpers still read `panel.titleTemplate.taskKey`.
- [ ] Confirm cloud result is never consumed.
- [ ] Confirm disabled defaults.
- [ ] Confirm no real HTTP dependency.
- [ ] Confirm tests and compile pass.
