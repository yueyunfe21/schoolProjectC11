# Summon Skill Maintenance Queue Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move summon-skill cleanup from immediate due-time execution to a deduplicated maintenance queue consumed only by old-window snapshots.

**Architecture:** `TaskMaintenanceService` owns the queue, dedupe, snapshot eligibility, success dequeue, and failure tail-move policy. Wubei/Xiuluo only pass task-specific consumption budgets and must not duplicate queue logic. `SummonSkillService` visual/click behavior remains unchanged.

**Tech Stack:** Java 17, Spring services, existing task maintenance DTOs, source-level focused tests.

---

### Task 1: Add Queue Semantics Inside TaskMaintenanceService

**Files:**
- Modify: `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- Modify if needed: `src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceRequest.java`
- Modify if needed: `src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceResult.java`
- Modify if needed: `src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceStatus.java`
- Test: `src/test/java/com/bot/dhxy/service/TaskMaintenanceSummonSkillQueueWiringTest.java`

- [ ] **Step 1: Add a focused guard before implementation**

Create or extend a source-level/manual test that proves:

```text
1. due detection enqueues a SUMMON_SKILL item and does not call SummonSkillService in the same pass.
2. duplicate due checks for the same window/account/type do not create another item.
3. queue consumption only considers items with enqueuedAt < windowOpenAt.
4. success removes the item and updates cooldown.
5. failure moves the item to queue tail and does not update cooldown.
```

Expected before implementation: FAIL because current `maybeCleanSummonSkill(...)` directly calls cleanup when due.

- [ ] **Step 2: Implement the queue data model**

Keep it private to `TaskMaintenanceService` unless a test absolutely needs public access.

Required fields:

```text
maintenanceType = SUMMON_SKILL
windowKey
account/role identity if already available in current context; otherwise include windowKey and player identity epoch/title where existing helpers expose them
enqueuedAt
attemptCount
lastFailureReason
```

Use a dedupe key that prevents more than one unprocessed summon-skill item for the same window/account/type.

- [ ] **Step 3: Split due detection from queue consumption**

When summon skill is due:

```text
if item not already queued:
    enqueue item with enqueuedAt=now
return queued/deferred result
```

Do not update `lastSummonSkillCleanAtByWindow` when enqueueing.

- [ ] **Step 4: Add queue snapshot consumption**

At maintenance-window consumption time:

```text
windowOpenAt = current maintenance window opened time
eligible = queue items where enqueuedAt < windowOpenAt
consume up to request.maxSummonSkillCleanersPerTeamRound()
```

If no eligible item exists, return no-action/queued-not-ready without opening or keeping a summon-skill action alive.

- [ ] **Step 5: Preserve success and failure semantics**

On success:

```text
remove item
update summon-skill window state
lastSummonSkillCleanAtByWindow.put(windowKey, successCompletionTime)
```

On retryable failure/interruption/unknown:

```text
attemptCount += 1
lastFailureReason = result/message
move item to tail
do not update lastSummonSkillCleanAtByWindow
```

Do not add irreversible failure removal in this CR.

### Task 2: Wire Wubei/Xiuluo Budgets Without Changing Business Clicks

**Files:**
- Modify: `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- Modify: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Test: focused guard from Task 1 may check these sources.

- [ ] **Step 1: Keep Wubei budget at one**

`WubeiTask.maybeRunLeaderPathingSummonMaintenance(...)` must keep max summon-skill queue consumption at `1`.

- [ ] **Step 2: Set Xiuluo budget to two**

`XiuluoTaskV2.runLeaderPathingSummonSkillMaintenance(...)` must request max summon-skill queue consumption of `2`.

- [ ] **Step 3: Do not edit visual/click/navigation paths**

Confirm no changes to:

```text
SummonSkillService slot/click algorithm
NPC/navigation/tracker green-link click
prepared enter-battle
expected combat
return-home/bag
```

### Task 3: Documentation, Dashboard, and Verification

**Files:**
- Modify: `docs/PACKAGE_ARCHITECTURE.md`
- Modify: `docs/ACTIVE_WORK.md`
- Modify: `docs/cr-dashboard-data.js` after generator.

- [ ] **Step 1: Update CR145 card**

Record implementation summary, test commands, and fresh-runtime gates in CR145.

- [ ] **Step 2: Run dashboard generator**

Run:

```powershell
node scripts/generate-cr-dashboard-data.js
```

Expected: `docs/cr-dashboard-data.js` updates or stays logically equivalent.

- [ ] **Step 3: Run verification**

Run:

```powershell
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
```

Run the focused CR145 guard and record the exact command/output in `docs/ACTIVE_WORK.md`.

### Self-Review Checklist

- CR145 does not change `SummonSkillService` matching/click decisions.
- Same-round due only enqueues; it cannot clean in the same maintenance window.
- Window-open snapshot prevents items enqueued after `windowOpenAt` from being consumed immediately.
- Wubei consumes at most 1 old item; Xiuluo consumes at most 2 old items.
- Failure moves to tail and does not refresh cooldown.
- Duplicate due checks do not grow the queue.
