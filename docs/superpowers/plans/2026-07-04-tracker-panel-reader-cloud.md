# Tracker Panel Reader Cloud Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move left tracker panel reading/click-point calculation for 五环、修罗、五倍 into external cloud brain while keeping the local app responsible only for capture, safety validation, physical input, and task-side effects.

**Architecture:** Add a unified `TRACKER_PANEL_READER` decision service. DHXY captures the tracker panel/detail crop and uploads the raw PNG plus window-relative origin; external cloud brain returns the minimal executable payload: action and click coordinate for 五环/修罗, plus `taskKey` and optional link list for 五倍. Existing local tracker reader remains available for replay/shadow, but production cloud-required mode must not silently fall back to local green-link calculation.

**Tech Stack:** Java 17, Spring Boot, JavaFX app, existing `CloudDecisionRequest/Response`, external `D:\mavenProject\dhxy-cloud-brain`, Maven tests, repo-local testcase images with marked replay output.

---

### Task 1: DHXY Contract And Wiring Guard

**Files:**
- Modify: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionServiceId.java`
- Create: `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudRequest.java`
- Create: `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecision.java`
- Create: `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecisionService.java`
- Create: `src/test/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecisionServiceTest.java`

- [ ] **Step 1: Add the service id**

Add enum value:

```java
TRACKER_PANEL_READER,
```

- [ ] **Step 2: Create request DTO**

`TrackerPanelReaderCloudRequest` must be immutable Lombok `@Value @Builder` and include:

```java
String taskCode;
String phase;
String source;
String imagePayloadBase64;
String payloadMimeType;
String imageSha256;
String imageMode;
int imageOriginWindowX;
int imageOriginWindowY;
int requestedLinkIndex;
String selectionPolicy;
```

Use `requestedLinkIndex=-1` when not applicable.

- [ ] **Step 3: Create decision DTO**

`TrackerPanelReaderCloudDecision` must expose:

```java
boolean found();
boolean noAction();
boolean clickAction();
String taskKey();
Point clickWindowRelative();
List<Link> links();
String reason();
CloudDecisionResult cloudResult();
```

Each `Link` stores `index`, `Point clickWindowRelative`, and a window-relative rect string or value object.

- [ ] **Step 4: Implement service validation**

`TrackerPanelReaderCloudDecisionService` must reject requests before network when:

```text
imagePayloadBase64 blank
payloadMimeType != image/png
imageSha256 blank
taskCode not in wuhuan|xiuluo|wubei
imageOriginWindowX/Y negative
```

It must put these context keys into `CloudDecisionRequest.context`:

```text
imagePayloadBase64
payloadMimeType
imageSha256
imageMode
imageOriginWindow
requestedLinkIndex
selectionPolicy
```

- [ ] **Step 5: Write focused tests**

Tests must cover:

```text
valid request includes CR image payload fields and origin fields
invalid/missing payload returns no-action/fail-closed
cloud CLICK_TRACKER_LINK with coordinateSpace=WINDOW_RELATIVE parses to Point
cloud response outside 1024x768 is rejected
wuhuan/xiuluo do not require taskKey
wubei response requires taskKey when action is CLICK_TRACKER_LINK or REROLL
```

- [ ] **Step 6: Run focused test**

Run:

```powershell
mvn -q -Dtest=TrackerPanelReaderCloudDecisionServiceTest test
```

Expected: PASS.

### Task 2: External Cloud Brain Tracker Reader

**Files:**
- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`
- Create or modify focused helper classes only if they keep one clear responsibility.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\TrackerPanelReaderReplayTest.java`
- Add testcase images under `D:\mavenProject\dhxy-cloud-brain\src\test\resources\tracker-panel\`

- [ ] **Step 1: Add service dispatch**

Dispatch `TRACKER_PANEL_READER` in `DecisionEngine`.

- [ ] **Step 2: Implement minimal response schema**

Return `decision` and diagnostics using this schema:

```text
decision=click=<x>,<y>
diagnostics.action=CLICK_TRACKER_LINK
diagnostics.coordinateSpace=WINDOW_RELATIVE
diagnostics.status=FOUND
diagnostics.taskKey=<only required for wubei>
diagnostics.links=<index:x,y;index:x,y when needed>
```

For not found:

```text
decision=NO_ACTION
diagnostics.action=NO_ACTION
diagnostics.status=NOT_FOUND
diagnostics.reason=<specific reason>
```

- [ ] **Step 3: Move the existing tracker algorithms by behavior**

Use the same semantics as current DHXY `TaskTrackerPanelService`:

```text
title/template matching for wubei taskKey and xiuluo/wuhuan task presence
yellow text wash/OCR only when needed by wubei existing target-name flow
green text band detection
green link segmentation
wuhuan pathing-name segment rule
xiuluo first usable green link rule
wubei multi-link/probe rule
```

Do not invent new strategy order or new broad thresholds without a testcase image and card note.

- [ ] **Step 4: Replay test images**

Tests must include at least:

```text
wuhuan tracker panel with one clickable green link
xiuluo tracker panel with one shortcut green link
wubei ordinary/huangpao/dark-thunder or equivalent title sample
wubei bailongma/mirror sample with two green links
```

Each replay writes a marked PNG showing crop, green boxes, and final red click point.

- [ ] **Step 5: Run external focused tests**

Run in `D:\mavenProject\dhxy-cloud-brain`:

```powershell
mvn -q -Dtest=TrackerPanelReaderReplayTest test
mvn -q -DskipTests compile
```

Expected: PASS.

### Task 3: DHXY Production Consumption In TaskTrackerPanelService

**Files:**
- Modify: `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
- Modify only if necessary: `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelReadResult.java`
- Create: `src/test/java/com/bot/dhxy/service/TaskTrackerPanelCloudReaderWiringTest.java`

- [ ] **Step 1: Keep local capture/origin**

Use existing tracker anchor and crop behavior. The cloud request must use the crop path and crop origin already produced by tracker panel capture/detail crop.

- [ ] **Step 2: Add cloud read path**

For `readWubeiTrackerPanel`, `readXiuluoTrackerPanel`, and 五环 tracker read helpers, build a `TrackerPanelReaderCloudRequest` from the crop image and task context.

- [ ] **Step 3: Adapt response**

Convert cloud response to existing result:

```text
found=true when status=FOUND and action is usable
titleTemplate/taskKey only for wubei and existing xiuluo/wuhuan compatibility
greenLinks from cloud link/click rectangles
yellowText only when cloud returned targetName/yellowText for wubei existing target-name flow
```

五环/修罗 do not require title/yellow text in the production payload.

- [ ] **Step 4: Fail closed in cloud-required mode**

If cloud is active/required and response is invalid, return `TaskTrackerPanelReadResult.empty()` or no-click decision. Do not call the local green-link scanner to drive production click.

- [ ] **Step 5: Preserve local replay/shadow**

Keep existing local reader methods usable for testcase replay and shadow logs. They must not override production cloud clicks in cloud-required mode.

- [ ] **Step 6: Run focused test**

Run:

```powershell
mvn -q -Dtest=TaskTrackerPanelCloudReaderWiringTest test
```

Expected: PASS.

### Task 4: Task Integration And Safety

**Files:**
- Modify narrowly as needed: `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- Modify narrowly as needed: `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- Modify narrowly as needed: `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- Create or extend focused source guards under `src/test/java/com/bot/dhxy/task/...`

- [ ] **Step 1: 五环**

FiveRing must consume only cloud-produced green-link click for tracker pathing. It must not need title/yellow text.

- [ ] **Step 2: 修罗**

Xiuluo shortcut must consume only cloud-produced first green-link click. Existing objective, return item, pathing intent, maintenance, and dialog behavior stays unchanged.

- [ ] **Step 3: 五倍**

Wubei must consume cloud `taskKey` for:

```text
wubei.dianqian_xianyi -> dark thunder reroll
wubei.baoxiang_miqing -> probe/mirror multi-link path
wubei.zhidou_huangpao -> chained combat
ordinary task keys -> normal green-link pathing
```

Wubei must consume cloud links for probe/mirror and cloud current click for normal/chained pathing.

- [ ] **Step 4: Guard against local fallback**

Add source guards that fail if cloud-required tracker pathing still calls local green-link scan as production fallback.

- [ ] **Step 5: Run focused tests and compile**

Run:

```powershell
mvn -q -Dtest=TaskTrackerPanelCloudReaderWiringTest test
mvn -q -DskipTests compile
```

Expected: PASS.

### Task 5: Docs, Dashboard, Review Gate

**Files:**
- Modify: `docs/PACKAGE_ARCHITECTURE.md`
- Modify: `docs/ACTIVE_WORK.md`
- Modify generated: `docs/cr-dashboard-data.js`

- [ ] **Step 1: Update CR182 implementation notes**

Record touched files, exact tests, replay input images, marked output images, and fresh runtime gate.

- [ ] **Step 2: Regenerate dashboard**

Run:

```powershell
node scripts/generate-cr-dashboard-data.js
```

Expected: generated rows include CR182.

- [ ] **Step 3: Two independent reviews**

Request two separate reviewer sub-agents. CR182 is not complete until both reviewers approve with no P0/P1/P2 blocker.

- [ ] **Step 4: Fresh runtime gate**

Tell the user to restart DHXY and external cloud brain before live verification. Expected runtime logs:

```text
TRACKER_PANEL_READER request ...
TRACKER_PANEL_READER response status=FOUND action=CLICK_TRACKER_LINK click=...
wuhuan/xiuluo/wubei tracker green click uses cloud response
```

If 五倍白龙马 appears, logs must show multi-link response and selected index/click.
