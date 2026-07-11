# TrackerLinkRankerCloud Shadow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `TRACKER_LINK_RANKER` shadow logging for 五倍/修罗 tracker green-link selection without changing any real click, pathing, task phase, or retry behavior.

**Architecture:** Keep the local tracker-link selection as the only executed decision. Add a focused cloud shadow service that receives the local selected link and all available candidate links, then logs local-vs-cloud through `CloudDecisionCoordinator`. The service is called immediately before existing tracker green clicks or cached tracker actions, and the returned cloud result is ignored.

**Tech Stack:** Java 21, Spring services, existing `com.bot.dhxy.cloud.decision` skeleton, existing POJO/source guard tests, Maven.

---

## Scope Rules

- Shadow only. Do not consume `CloudDecisionResult` or `effectiveDecision`.
- Do not alter green-link scanning, sorting, OCR, template thresholds, pathing intent registration, or click coordinates.
- Do not add real HTTP calls.
- Do not change 五倍 probe retry order, 黄袍 chained fast action, 修罗 tracker shortcut pathing, or return-item prescan order.
- Default config must be disabled unless explicitly enabled:
  - `cloud.services.tracker-link-ranker.shadow-enabled=false`
  - `cloud.services.tracker-link-ranker.execute-enabled=false`

## File Map

- Create: `src/main/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudShadowService.java`
  - Builds `TRACKER_LINK_RANKER` requests from local selected link index and candidate links.
- Modify: `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - Report shadow before tracker green clicks that already have a selected `TaskTrackerGreenLink`.
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  - Report shadow before the tracker shortcut click using the selected first link.
- Modify: `src/main/resources/application.properties`
  - Add disabled-by-default tracker-link-ranker flags.
- Create: `src/test/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudShadowServiceTest.java`
  - Covers disabled no-call and request fields.
- Create: `src/test/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudShadowWiringTest.java`
  - Source guard for hook presence and no cloud-result consumption in 五倍/修罗 task code.
- Modify: `docs/HYBRID_CLOUD_WORKFLOW.md`
  - Add CR-HC-003 note.
- Modify: `docs/ACTIVE_WORK.md`
  - Record baseline, implementation, and verification.

## Task 1: Shadow Service

**Files:**

- Create: `src/main/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudShadowService.java`
- Create: `src/test/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudShadowServiceTest.java`

- [ ] Create a Spring `@Service` with constructor-injected `CloudDecisionCoordinator`.
- [ ] Add method:

```java
public void shadowTrackerLinkSelection(
        String taskCode,
        String source,
        String phase,
        List<TaskTrackerGreenLink> candidates,
        int selectedIndex,
        TaskTrackerGreenLink selectedLink)
```

- [ ] If `coordinator.isActive(CloudDecisionServiceId.TRACKER_LINK_RANKER)` is false, return without logging and without calling the client.
- [ ] Normalize inputs:
  - blank `taskCode` -> `unknown`
  - blank `phase` -> `tracker-green-link-selection`
  - null candidates -> empty list
  - null selected link but valid `selectedIndex` -> use `candidates.get(selectedIndex)`
  - invalid selection -> `localDecision=NO_LINK`
- [ ] Build `localDecision` as:

```text
index=<selectedIndex>;click=<x>,<y>;rect=<minX>,<minY>,<maxX>,<maxY>
```

or:

```text
NO_LINK
```

- [ ] Build request:
  - `serviceId=TRACKER_LINK_RANKER`
  - `taskCode=<taskCode>`
  - `phase=<phase>`
  - `traceId=tracker-link-ranker:<taskCode>:<source>`
  - `localDecision=<localDecision>`
  - context:
    - `source`
    - `candidateCount`
    - `selectedIndex`
    - `selectedClick`
    - `selectedRect`
    - `selectedTargetMap`
    - `selectedTargetMapScore`
    - `candidates`
- [ ] Encode `candidates` as a compact semicolon-separated string:

```text
0:click=123,456;rect=100,440,146,452;map=平顶山;score=0.9307|1:click=...
```

- [ ] Call `coordinator.shadow(request, localDecision)` and ignore the returned result.
- [ ] Tests:
  - disabled global cloud does not call a throwing client;
  - enabled `TRACKER_LINK_RANKER` sends service id, task code, phase, selected index, selected click, and candidate count;
  - invalid/empty selection sends `NO_LINK`.

## Task 2: 五倍 Hook

**Files:**

- Modify: `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`

- [ ] Inject `TrackerLinkRankerCloudShadowService`.
- [ ] In `triggerCombatTrackerPathing(...)`, after selecting `TaskTrackerGreenLink segment = panel.getGreenLinks().get(0);` and before the click loop, call:

```java
trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection(
        TASK_CODE,
        label,
        "wubei-combat-tracker-pathing",
        panel.getGreenLinks(),
        0,
        segment);
```

- [ ] In probe pathing where the selected segment comes from `currentProbeSegments.get(nextIndex)`, call shadow before `clickTaskTrackerGreen(...)`:

```java
trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection(
        TASK_CODE,
        label,
        "wubei-probe-tracker-pathing",
        currentProbeSegments,
        nextIndex,
        segment);
```

- [ ] In enter-battle retry where code clicks `currentTrackerPanel.getGreenLinks().get(0)`, call shadow before `clickTaskTrackerGreen(...)`:

```java
trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection(
        TASK_CODE,
        "enter-battle-retry",
        "wubei-enter-battle-retry",
        currentTrackerPanel.getGreenLinks(),
        0,
        currentTrackerPanel.getGreenLinks().get(0));
```

- [ ] In chained continuation fast-cache preparation is not a click decision by itself; do not hook it in this first card.
- [ ] Do not change `clickTaskTrackerGreen(...)`.
- [ ] Do not change selected index or candidate list.

## Task 3: 修罗 Hook

**Files:**

- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`

- [ ] Inject `TrackerLinkRankerCloudShadowService`.
- [ ] In `tryTrackerShortcutWithPanel(...)`, after resolving `Point point = clickPoint.get();` and before `inputSequences.moveAndClickLeft(...)`, call:

```java
trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection(
        TASK_CODE,
        "xiuluo-v2:trackerShortcutGreen:" + state.round(),
        "xiuluo-tracker-shortcut",
        panel.getGreenLinks(),
        0,
        panel.getGreenLinks().isEmpty() ? null : panel.getGreenLinks().get(0));
```

- [ ] Do not change `resolveXiuluoTrackerGreenClickPoint(...)`.
- [ ] Do not change tracker shortcut pathing intent, dialog interest, maintenance windows, return-item prescan, or deferred recovery order.

## Task 4: Config, Docs, Guards

**Files:**

- Modify: `src/main/resources/application.properties`
- Create: `src/test/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudShadowWiringTest.java`
- Modify: `docs/HYBRID_CLOUD_WORKFLOW.md`
- Modify: `docs/ACTIVE_WORK.md`

- [ ] Add config:

```properties
cloud.services.tracker-link-ranker.shadow-enabled=true
cloud.services.tracker-link-ranker.execute-enabled=false
cloud.services.tracker-link-ranker.execute-percent=0
cloud.services.tracker-link-ranker.fallback=LOCAL
```

Note: this branch is for live shadow verification, so `shadow-enabled=true` is intentional. Keep execute disabled.

- [ ] Add source guard:
  - `WubeiTask.java` imports/injects `TrackerLinkRankerCloudShadowService`.
  - `XiuluoTaskV2.java` imports/injects `TrackerLinkRankerCloudShadowService`.
  - both files contain `shadowTrackerLinkSelection(`.
  - neither file contains `CloudDecisionResult`, `getEffectiveDecision`, or `effectiveDecision`.
- [ ] Add CR-HC-003 doc entry with:
  - service id `TRACKER_LINK_RANKER`;
  - hook points;
  - local baseline;
  - request fields;
  - shadow log keyword;
  - rollback switch.
- [ ] Record verification in `docs/ACTIVE_WORK.md`.

## Verification

Run:

```powershell
mvn -q -DskipTests compile
mvn -q "-Dtest=CloudDecisionPropertiesTest,CloudDecisionCoordinatorTest,CloudDecisionSkeletonWiringTest,TaskClassifierCloudShadowServiceTest,TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest" test
java -cp "target/classes;target/test-classes" com.bot.dhxy.cloud.task.TrackerLinkRankerCloudShadowWiringTest
```

Expected:

- Compile passes.
- Focused tests pass.
- Source guard main passes.
- No testcase replay required because the local selected link and click point are unchanged.

## Runtime Acceptance

When 五倍/修罗 clicks tracker green links, logs should include:

```text
cloud.decision serviceId=TRACKER_LINK_RANKER mode=SHADOW taskCode=wubei|xiuluo phase=...
localDecision=index=...;click=... cloudDecision=... effectiveDecision=... agree=... executed=false
```

Rollback:

```properties
cloud.services.tracker-link-ranker.shadow-enabled=false
cloud.services.tracker-link-ranker.execute-enabled=false
cloud.services.tracker-link-ranker.fallback=LOCAL
```

## Manager Review Checklist

- [ ] Confirm cloud result is never consumed.
- [ ] Confirm click coordinates are unchanged.
- [ ] Confirm pathing intent order is unchanged.
- [ ] Confirm execute remains disabled.
- [ ] Confirm no visual matching, OCR, template threshold, or navigation coordinate changed.
