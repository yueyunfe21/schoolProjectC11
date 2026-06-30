# DHXY Project Context

This document records the current state of the DHXY project so a new Codex/Desktop session can continue without reading the entire chat history.

Last updated from the web ChatGPT debugging session after multi-window input queue and NPC click debugging.

## 1. High-level project purpose

DHXY is a Java/Spring/JavaFX desktop automation tool for 大话西游2 classic client workflows.

The current main workflow is the 五环 task. The goal is to make 五环 run reliably in:

1. single-window mode;
2. multi-window mode with multiple game clients;
3. long-running automation without keyboard/mouse actions crossing between windows.

The final intended system should have:

- JavaFX UI for registering and controlling multiple game windows;
- per-window role/status/task display;
- per-window `GameContext.State`;
- per-window native hwnd binding;
- per-window screenshot/temp image isolation;
- serialized physical input actions;
- recoverable logs and debug images.

## 2. Current status summary

### What works now

- Single-window 五环 had already been restored and can run.
- Two-window mode has improved significantly.
- Both windows can start and run much further than before.
- The original issue where two windows typed into the same target has been reduced by serializing input with `InputActionQueue`.
- `logs/dhxy-console.log` now captures normal business logs.
- `logs/tracker-coordinate.log` captures tracker/window-coordinate diagnostics.
- `ClientIdentityService` now reads identity from the bound window title first.
- `NpcClickService` first-shot debug confirmed the coordinate formula is good.
- `NpcClickService` formal first-shot flow now keeps `moveMouse + clickLeft` in one input sequence.

### What was recently tested

The retired one-shot 墨意 NPC first-shot debug path was used to verify the coordinate formula.

Confirmed by user:

- identity sync works after using bound title;
- `FINAL_CLICK_POINT` is correct;
- mouse can click the NPC when direct debug input is used;
- therefore the coordinate formula itself is likely correct.

After that, formal flow was changed so first-shot move and click are a single queue sequence.

### What should probably be tested next

Run normal 五环 and inspect:

- whether first-shot NPC click now works more consistently;
- whether two-window input crossing remains fixed;
- whether battle panel detection and dragging works;
- whether stop-all stops promptly inside loops;
- whether P2/P1 task navigation remains stable.

## 3. Critical configuration

File:

```text
src/main/resources/application.properties
```

Important known settings:

```properties
bot.window.isolation-enabled=true
bot.window.input-focus-enabled=true
bot.window.tracker-state-isolation-enabled=true
bot.window.bound-window-tracker-enabled=true
bot.window.scoped-temp-path-enabled=true
```

The scoped temp path switch is currently true for normal multi-window runs. Services should use `WindowScopedTempPath.resolve(...)` for task screenshots and OCR/template intermediates so concurrent windows do not overwrite each other. Standalone tools may still write fixed `images/temp` files when they run outside a window task context.

The old `debug.npc-first-shot` one-shot switch has been retired. Use normal 五环 runtime logs and testcase replay assets for future NPC click checks.

## 4. Important recent commits

These are recent key commits from the web debugging session. The exact commit list may continue to evolve; use `git log --oneline` locally.

Older key commits before the recent NPC work:

```text
28a22e2 Split window isolation feature switches
61e57d6 Use dedicated switch for input focus isolation
9ad03f7 Add tracker coordinate diagnostics
a8fd01f Write tracker diagnostics to file
a08aeb3 Honor tracker isolation switches
92aa56b Explain bound window tracker misses
```

Recent important commits:

```text
2a77938 Allow queued input requests to be cancelled
5222153 Cancel queued input wait on thread interrupt
c070ffb Force cancel window task on stop request
ccb3a95 Revert moving detection logic change
a7c3a9b Continue queued input after focus warning
5c52002 Run map search input as exclusive sequence
83edd7e Include last navigation link click in exclusive map search sequence
e31b85b Add detailed bag scan diagnostics
4721ee7 Avoid nested input queue deadlock in NPC ctrl probe
9721894 Write console logs to rolling file
48d8ac0 Stop NPC ctrl probe loop promptly when interrupted
e82d626 Scope battle radar temp paths and queue battle input
bf337b5 Treat queued window focus as fast best effort
7e4b69a Log P1 blind link click coordinates
9c054f3 Treat dialog after P1 click as blind path success
bf64196 Add debug first shot NPC click helper
33c96e8 Add five ring NPC first shot debug mode
6afaa91 Read identity from bound window title first
ffc5ad8 Use direct input for NPC first shot debug click
1e3e127 Keep NPC move and click in one input sequence
df883c7 Add NPC first shot debug switch to properties
d6b1d0f Add Codex agent instructions for DHXY
```

## 5. Major debugging history and conclusions

### 5.1 Two windows were getting same hwnd/base

Original issue:

- two windows sometimes still walked through `locateWindow-title-search`;
- both windows could get the same `base/hwnd`;
- tracker was not consistently using each `WindowTaskRunner`'s bound hwnd.

Important direction:

- `GameClientTracker` should prefer current `WindowRuntimeContext.nativeBinding` when `bot.window.bound-window-tracker-enabled=true`.
- Window registration must preserve native hwnd/title/class/process id.

Useful log:

```text
logs/tracker-coordinate.log
```

Look for:

```text
action=bound-tracker-miss
reason=...
```

### 5.2 Console business logs were missing from file

`tracker-coordinate.log` was not enough because it only logs tracker diagnostics. The user needed normal console business logs too.

A Logback config was added so console/business logs also write to:

```text
logs/dhxy-console.log
```

Use this as the main diagnostic file.

### 5.3 Stop button problem

Symptom:

- UI's “停止全部窗口” had to be clicked many times;
- when inside NPC Ctrl-probe loop, task did not stop promptly.

Changes made:

- `WindowTaskRunner.stopCurrentTask()` requests stop, interrupts, and force-cancels Future.
- `InputActionQueue.await(...)` cancels queued request on interrupt.
- `NpcClickService` checks `Thread.currentThread().isInterrupted()` in loops and sleeps.

Remaining consideration:

If any new long loop is added, it must check stop/interrupt.

### 5.4 Movement detection false diagnosis was reverted

At one point `GameStateUtil.isMovingByPixelDiff()` was changed to force-pass after repeated moving detections. User correctly objected because that logic was already tested.

That change was reverted.

Instruction: do not modify user-validated movement detection logic unless there is clear evidence. If stuck near movement detection, inspect:

- screenshot region;
- window base;
- call path;
- latest local screenshot;
- which window's tracker state is active.

### 5.5 Input focus was blocking actual input

Logs showed:

```text
Input queue failed to focus window ...
Input action moved to dead letter ...
```

This meant real actions like `Alt+E` were never executed.

Changed behavior:

- focus is best-effort;
- if `SetForegroundWindow` is not confirmed, log warning/debug but continue input sequence.

Later optimized:

- focus waits only about 50ms;
- no heavy repeated foreground validation;
- avoid WARN spam.

### 5.6 Navigation sequence had to include clicking the final link

User pointed out that map navigation should not release the input queue after only typing map name and scrolling. It must continue through clicking the last coordinate link because that starts auto-pathing.

`NavigationService` was changed so map open/search/input/scroll/last-link click is one exclusive sequence.

Current intended sequence:

```text
open map
click 寻路
input map name
Enter
click scroll area
scroll down
scan map result
find last coordinate link
click last coordinate link
close map
```

### 5.7 Nested queue deadlock in NPC Ctrl probe

Symptom:

- Ctrl found NPC/menu but then stopped forever.

Cause:

`NpcClickService` used `submitExclusiveAndWait("npcClick:ctrlProbe", callback)` and inside the callback called `inputSequences.submitAndWait(...)` again. Since the only worker was busy executing the callback, the nested request could never run.

Fix:

- inside the exclusive callback, use direct `InputProvider` calls.
- do not enqueue again from inside an input-worker callback.

Rule:

Never nest `inputSequences.submitAndWait(...)` inside a `submitExclusiveAndWait(...)` callback.

### 5.8 Battle panel detection path issue

Symptom:

- battle was detected, but auto-battle panel was not found/dragged.

Cause:

`BattleRadarService.findAutoCombatBox()` still used old global latest vision path:

```java
GameClientTracker.LATEST_VISION_PATH
```

but multi-window now has per-window/latest paths.

Fix direction already applied:

- use `tracker.getLatestVisionPath()`;
- use `WindowScopedTempPath.resolve(...)` for battle temp scans/masks;
- battle input such as `Alt+8` and drag should be queued.

Next test should confirm logs show either:

```text
🎯 [主炮命中] 绿字指纹识别成功
```

or:

```text
🎯 [副炮命中] 白字指纹兜底成功
```

and if panel is off-position:

```text
📦 发现面板不在安全区！执行强制拖拽归位
```

### 5.9 P1 blind NPC link fallback

There was discussion about P1 “盲狙” being less useful than before. Current understanding:

- P1 is fallback after P2 image matching fails.
- It clicks a fixed offset relative to task panel anchor.
- Previously success was judged only by whether the character moved.
- This can be wrong if clicking the link directly opens dialog without movement.

Changes made:

- log actual P1 click coordinates;
- if dialog appears after P1 click, treat it as success;
- only mark failure if there is no movement and no dialog.

Future investigation:

- compare P1 anchor/offset with older working version;
- inspect whether panel scroll/right-side state differs;
- investigate why P2 fails often enough to need P1.

### 5.10 NPC name changed: 墨意 vs 莫易

User noticed the actual game NPC name may be 莫易, while code still uses 墨意.

This can affect OCR/menu matching in `NpcClickService`:

```java
text.contains(targetName)
```

Current debug first-shot formula does not depend on NPC name after coordinate calculation, but Ctrl menu OCR matching does.

Possible future improvement:

- support aliases like `墨意` and `莫易`;
- make target NPC name configurable;
- use coordinate first-shot primarily, menu OCR fallback secondarily.

Do not forget this if NPC clicking fails only in menu/OCR fallback.

## 6. Current important service logic

### 6.1 FiveRingTask

File:

```text
src/main/java/com/bot/dhxy/task/FiveRingTask.java
```

Important constants:

```java
private static final String TARGET_MAP_NAME = "长安";
private static final String TARGET_NPC_NAME = "墨意";
private static final int NPC_COOR_X = 87;
private static final int NPC_COOR_Y = 174;
private static final int TUNE_X = -10;
private static final int TUNE_Y = 0;
private static final String KEY_ITEM_NAME = "wuhuan/shoe.png";
```

### 6.2 NpcClickService

File:

```text
src/main/java/com/bot/dhxy/service/NpcClickService.java
```

Important methods:

- `clickNpcSmart(...)`
- `executeMoveClickAndVerify(...)`
- `scanMenuAndVerifyDirect(...)`

Current `clickNpcSmart(...)` order:

1. Try yellow target-name OCR first in regions recommended by `OcrRoiMemoryService.recommendNpcClickRegions(...)`.
   - There is no default center `350x200` compatibility overload in the formal NPC click request anymore.
   - Formal task callers should pass target facts only; learned vision memory owns the NPC/monster OCR regions.
   - Yellow OCR uses `GameTextLineOcrService.findYellowTarget(...)`, so fuzzy target matching can handle one-character local OCR errors.
2. If yellow target OCR does not open an option dialog, fall back to the old player-purple-name anchor plus current coordinate formula.
3. If the formula first shot still fails, use the Ctrl-menu dense scan as the final fallback.

The three strategies are also callable independently:

- `clickNpcByYellowTargetName(...)`
- `clickNpcByPlayerAnchorFormula(...)`
- `clickNpcByCtrlMenuScan(...)`

Current first-shot formula:

```java
int deltaLogicX = mapX - locInfo.x;
int deltaLogicY = mapY - locInfo.y;
int deltaPhysX = (int) Math.round(deltaLogicX * UX + deltaLogicY * VX);
int deltaPhysY = (int) Math.round(deltaLogicX * UY + deltaLogicY * VY);
int targetX = playerAnchor.x + deltaPhysX + tuneX;
int targetY = playerAnchor.y + deltaPhysY - 50 + tuneY;
```

Constants:

```java
private static final double UX = 20.0;
private static final double UY = 0.0;
private static final double VX = 0.0;
private static final double VY = -20.0;
```

Confirmed by debug:

- formula can produce correct point on NPC;
- direct click works.

Current formal first-shot should use:

```java
executeMoveClickAndVerify("npcClick:firstShotMoveClick", targetX, targetY, 1500, 0)
```

Important pattern:

```java
inputSequences.submitAndWait(description, List.of(
        InputAction.moveMouse(x, y),
        InputAction.sleep(150),
        InputAction.clickLeft(x, y, 100),
        InputAction.sleep((int) firstWaitMs)
));
```

This is the correct sequence pattern for move+click.

### 6.3 PlayerStateService and ClientIdentityService

`PlayerStateService.syncAll()` calls:

1. `syncMyIdentity()`
2. `syncMyPosition()`

Identity comes from window title. Position comes from screen OCR.

If identity is null but position works, inspect `ClientIdentityService` and window title binding.

`ClientIdentityService` should now prioritize the bound native title:

1. current `WindowRuntimeContext.nativeBinding.title`;
2. tracker title;
3. tracker locate fallback.

### 6.4 InputActionQueue / Worker

The queue owns serialized physical input. Every request is an `InputActionRequest` with a list of `InputAction`s or an exclusive callback.

A list of actions in a single `submitAndWait(...)` is one sequence. Another window cannot interleave actions inside that list.

Direct `InputProvider` calls are allowed only in narrow cases:

1. Inside a callback passed to `InputSequences.submitExclusiveAndWait(...)`.
   - The input worker is already executing the serialized exclusive section.
   - Do not call `inputSequences.submitAndWait(...)` from inside that callback, because it would enqueue work behind the callback that is currently holding the only worker.
   - Use direct `InputProvider` calls inside the callback instead.
2. Temporary debug-only paths that are explicitly named as direct/debug, such as NPC first-shot debug helpers.
   - These should not quietly become normal production task paths.
3. Low-level input worker / driver code.

All normal task mouse/keyboard actions outside an exclusive callback should go through `InputSequences` / `InputActionQueue`.

If a service injects both `InputSequences` and `InputProvider`, check each direct `InputProvider` call before changing it:

- direct calls inside `submitExclusiveAndWait(...)` are usually intentional;
- direct calls outside exclusive callbacks are suspicious unless the method is a clearly marked debug path;
- move + click still needs to be one atomic sequence, either as one `submitAndWait(...)` action list or inside one exclusive callback.

## 7. Logs and how to read them

### 7.1 Main log

```text
logs/dhxy-console.log
```

Search terms:

```text
NPC首点调试
FINAL_CLICK_POINT
npcClick:firstShotMoveClick
NPC move+click sequence
身份识别
状态中枢
P1盲狙
P2识图
导航串行
包裹
战斗雷达
自动挂机
```

### 7.2 Tracker diagnostic log

```text
logs/tracker-coordinate.log
```

Search terms:

```text
bound-tracker-miss
locateWindow-title-search
captureToFile
captureToMemory
bringWindowToFront
windowBase
hwnd
```

### 7.3 Useful debug images

Potentially in window-scoped temp dirs or configured temp paths:

```text
debug_npc_firstshot_center_raw.png
debug_npc_firstshot_player_washed.png
center_scan_layer1.png
center_scan_player.png
npc_menu_scan.png
npc_menu_clean.png
menu_before.png
menu_after.png
select_scan.png
top_scan.png
debug_hsv_mask_green.png
debug_thin_white_text.png
```

## 8. Things not to regress

Do not undo these decisions without a good reason:

1. Bound window title should be used for identity.
2. Focus failure should not abort input.
3. Move+click should be one sequence.
4. No nested queue calls inside exclusive callbacks.
5. Stop checks in NPC loops are required.
6. Do not modify `GameStateUtil.isMovingByPixelDiff()` without evidence.
7. Do not remove `logs/dhxy-console.log` logging.
8. Do not return to title-searching the first game window in multi-window mode.

## 9. Possible next improvements

### 9.1 Remove unnecessary full screenshots in clickNpcSmart

`clickNpcSmart()` currently calls:

```java
tracker.updateGlobalVision();
```

Then later it takes a smaller center-region screenshot for player anchor OCR.

This full screenshot may be unnecessary and slow. It may be a legacy cache update. Before removing, confirm it is not needed for base refresh. A safer replacement might be:

- ensure/bind window base;
- then only capture center region.

### 9.2 Avoid writing raw screenshot when only washed image is needed

Current flow:

```text
capture center raw image to file
wash purple text to another file
OCR reads washed file
```

Potential optimization:

```text
capture center image to memory
wash image in memory
write only final washed image for OCR
```

Longer-term ideal:

```text
capture memory -> wash memory -> OCR memory
```

But OCR currently likely expects file paths, so first step is only to avoid writing the raw intermediate if possible.

### 9.3 Support NPC aliases

Support both:

```text
墨意
莫易
```

or move NPC name into config if game text changes.

### 9.4 P2/P1 stabilization

Investigate why P2 image matching fails often enough to fall back to P1.

Potential work:

- inspect `p2_raw.png` / `p2_washed.png`;
- confirm template paths under `images/template/wuhuan/`;
- log matched template name and coordinates;
- compare panel anchor/rect with old working version;
- add OCR fallback for P2 if image matching is fragile.

### 9.5 Bag scan performance

Bag scan is detailed but may be slow.

Potential work:

- cache known shoe page per window;
- avoid scanning all pages every startup if previous page is known;
- only full scan when cache fails.

### 9.6 Better debug UI

Current debug uses config switches. Later add UI debug buttons for:

- NPC first-shot test;
- current window identity sync test;
- screenshot current center player anchor;
- run one `clickNpcSmart` without full 五环.

## 10. Desktop session update: 2026-05-20

This section records the newer Codex Desktop work after the earlier web-session context above. Use this section first when splitting work across multiple agents/threads.

### 10.1 Current main status

- Normal 五环 has run much further and at least one recent two-window run completed successfully.
- The current active focus is still 五环 multi-window stability, especially after-combat flow, task completion detection, P2/P1 fallback behavior, and movement detection.
- The codebase has moved further toward the multi-window architecture. Old single-window startup pieces have been reduced or removed in favor of window task runners and per-window startup initialization.
- The user wants fewer test runs because 五环 has daily limits. Prefer log-driven diagnosis and small targeted changes.

### 10.2 Overall working plan

Use this as the current high-level plan before picking a concrete coding task.

#### 1. Stabilize 五环 first

五环 is the real validation task for the whole architecture. Keep this as the active hotfix line until normal one-window and two-window runs are boringly reliable.

Priorities:

- Post-combat verification: after battle exit, heal first, re-enable task sync, then check whether 五环 is finished before any P2/P1.
- Dialog entry/recovery: if the correct accept-task dialog is already open, enter from that dialog; if the wrong dialog is open, let global UI cleanup close it.
- Give-item handoff: after 五环 successfully gives the shoe, do not stay in place clearing follow-up story dialogs. Re-enable task sync and continue into task-panel P2/P1 in the same loop.
- Startup visibility cleanup: `WindowTaskRunner` sends `ALT+6` twice after generic window startup initialization and before the concrete task executes, so every task benefits from hiding other player models before later screenshots.
- Movement judgment: use mini-map coordinate changes as primary evidence, recent pathing intent as protection, and pixel diff as fallback.
- Logging: key decisions must be explainable from `logs/dhxy-console.log`.

#### 2. Finish multi-window input unification

The target is: one physical input sequence at a time, always bound to the intended window.

Keep scanning for:

- direct mouse/keyboard calls that bypass `InputSequences`;
- split move/click sequences that should be atomic;
- repeated or stale focus/topmost calls;
- screenshot/OCR/temp paths that do not use the current window context.

#### 3. Remove or quarantine old single-window leftovers

The multi-window runner is now the main path, even when only one game window is registered.

Continue checking:

- old startup/init paths such as single-window `GameWindowService` style flows;
- global title-search logic that should be fallback only;
- `GameClientTracker` paths that still do not clearly bind to `WindowRuntimeContext`;
- old task queue/runner concepts that should either become per-window queues or be removed.

#### 4. Extract common services carefully

Do not keep solving everything inside 五环. Generic behavior should move into common services after the 五环 use case proves the shape.

Common areas:

- `DialogHandleRequest` / dialog result enum / dialog policy;
- `UICleanerService` for closing maps, dialogs, and blocking UI;
- coordinate template recognition for mini-map coordinates;
- screenshot diagnostics with per-window path/result logging;
- HP/MP and summon HP/MP checks with configurable thresholds.

#### 5. Prepare later task expansion

After 五环 stabilizes, the same structure should support:

- 三百环, with more complex item requests and inventory strategy;
- 修罗/五倍/other task accept-dialog flows;
- summon skill deletion via the ported `SummonSkillService`;
- UI-configurable healing thresholds and task policies.

#### 6. Split parallel work safely

If using multiple Codex threads, keep this conversation as the 五环 hotfix owner. Other threads should avoid touching the same core flow files unless explicitly coordinated.

Good parallel work:

- read-only scanner for input queue bypass, old window binding, and task completion bypass;
- future-feature preparation for summon skill service, HP/MP config, and coordinate template diagnostics;
- documentation/design cleanup for dialog request/result contracts.

### 10.3 Recent settled design decisions

#### Dialog handling

- The unified public dialog entry should be `DialogService.handleDialog(DialogHandleRequest)`.
- Older direct `processDialog` style interfaces should not be used as the main task-facing API.
- Tasks should not implement dialog clicking themselves. Tasks describe intent/policy through request data; `DialogService` and helper services execute the dialog behavior.
- 五环 can request `GIVE_ITEM_IF_AVAILABLE`, but the actual decision still depends on the opened dialog type/options.
- Generic cleanup/fallback belongs in `UICleanerService` / dialog cleanup logic, not in 五环-specific code.

#### Task completion and sync

- 五环 must keep the ability to detect that the task is finished by checking the task panel.
- The current 五环 task state must keep a task-sync gate that allows `syncTaskState(...)` to verify whether `wuhuan` still exists in the task list.
- A recent regression happened when post-combat flow resumed P2/P1 without re-enabling task sync. Keep the current task-sync gate enabled after post-combat verification so completion is checked before further P2/P1.
- Never let post-combat optimizations bypass task completion detection.

#### Post-combat flow

Current intended 五环 post-combat behavior:

1. `BattleRadarService` detects combat exit and sets `GameContext.ActionState.TASK_VERIFYING`.
2. 五环 sees `TASK_VERIFYING`.
3. Because battle exit means the character is definitely stopped, 五环 skips movement detection for that loop.
4. 五环 performs first-aid/heal check immediately.
5. 五环 sets `needTaskSync=true`.
6. 五环 restores `FREE`.
7. Same loop continues into `syncTaskState(...)` before any P2/P1.
8. If the task panel no longer contains 五环, finish. If it still exists, continue P2/P1.

Do not reorder this without checking logs. The important invariant is:

```text
post combat -> heal -> task sync/completion check -> P2/P1 only if task still exists
```

#### Battle detection

- `BattleRadarService` now uses a short exit debounce. One missed combat signal while in `IN_COMBAT` does not exit combat immediately.
- Consecutive combat-signal misses are required before combat exit. This was added because a single missed battle icon scan caused `Alt+Q` task-panel logic to run while the game was still in battle.
- The dynamic polling interval:
  - `IN_COMBAT`: around 3000 ms
  - `TASK_VERIFYING`: around 1000 ms
  - `FREE`: long idle interval
- `BattleRadarService` should not perform first-aid check while `TASK_VERIFYING`; 五环 owns its post-combat sequencing.

#### Movement detection

- Movement detection now uses mini-map coordinate recognition as primary evidence when possible.
- Pixel-diff movement detection is still retained as fallback.
- Movement intent protection is important after P1/P2 clicks; do not immediately declare stopped while a recently triggered pathing click may still be taking effect.
- STOPPED_STABLE should be the final conclusion after stronger states/evidence are considered.
- Battle exit is considered a strong stopped signal, so 五环 should skip ordinary movement detection in `TASK_VERIFYING`.

#### Input and multi-window

- All normal keyboard/mouse actions should go through `InputSequences` / `InputActionQueue`.
- Move + click must remain one atomic queued sequence.
- Do not enqueue a new input request from inside an exclusive input worker callback.
- Focus remains best-effort. `SetForegroundWindow` failure should not abort the action by itself.
- Window binding from `WindowTaskContextHolder` / `WindowRuntimeContext` is source of truth.

#### Task transaction and yield model

Use Wuhuan as the first reference implementation, but treat this as a global task design rule.

Do not split a workflow at every small completed action. A task should keep the current window's input turn through a meaningful transaction until it reaches a handoff state. The important question is not "did one click/check finish?", but "has this window reached a state where handing control to another window is useful and safe?"

Core terms:

- `TransactionResult`: what the action chain achieved.
- `YieldPolicy`: what the scheduler should do after that result.

Useful `TransactionResult` values:

- `READY_TO_CONTINUE`: local maintenance/preparation succeeded, and the same task can continue.
- `PATHING_STARTED`: movement, auto-pathing, or a route transition has been triggered.
- `SHARED_STATE_TRIGGERED`: this window triggered something other windows should respond to, such as a team-wide dialog.
- `TASK_FINISHED`: the task is confirmed complete.
- `RETRYABLE_ERROR`: an unknown/abnormal UI was cleaned or rejected; this window can retry later.
- `FAILED`, `STOPPED`: the task chain should stop.

Useful `YieldPolicy` values:

- `MUST_YIELD`: the window is moving/pathing, in combat, or has triggered a shared state other windows must handle.
- `MAY_YIELD`: an independent maintenance action finished; yielding is allowed but not required.
- `CONTINUE_CHAIN`: this was only preparation or a middle option; keep going until a real handoff state.
- `RETRY_LATER`: release the turn after cleaning/closing an unknown UI so other windows can proceed before this window retries.
- `STOP_CHAIN`: task finished, failed, stopped, or paused.

Preparation and maintenance are not the same as handoff:

- Chain preparation such as map tracking setup, identity/position sync, incense check, shoe page lookup, and startup UI cleanup returns `READY_TO_CONTINUE + CONTINUE_CHAIN`. It should not be a yield point by itself.
- Independent maintenance such as idle HP/MP recovery can return `READY_TO_CONTINUE + MAY_YIELD`.
- Post-combat recovery uses the same HP/MP service, but the caller owns the larger chain: post combat -> heal -> task sync -> P2/P1 or finish. In Wuhuan that means `READY_TO_CONTINUE + CONTINUE_CHAIN`, not a forced yield.

Dialog policy:

- Story dialogs are not task-advancement transactions by default. Normal task advancement should usually ignore story dialogs and continue toward task-panel/pathing actions. Story cleanup belongs in idle cleanup, battle/auto-battle maintenance, or explicit UI cleanup.
- Option dialogs must be classified by intent.
- Known advancement options, such as accepting a task or giving an item, must continue until the next real handoff state, usually P2/P1 pathing or task finish.
- Middle options, such as choosing a destination map, are not yield points if the next stage can immediately continue to map-to-NPC or mini-map pathing.
- Unknown options should be closed/cancelled by dialog/UI cleanup and returned as `RETRYABLE_ERROR + RETRY_LATER`.
- Team broadcast options can return `SHARED_STATE_TRIGGERED + MUST_YIELD`, allowing member windows to handle their dialogs before the leader continues.

Concrete Wuhuan examples:

- Initial accept dialog: accept option -> activate task panel -> P2/P1 -> `PATHING_STARTED + MUST_YIELD`.
- Give shoe: give option -> select shoe -> give button -> activate task panel -> P2/P1 -> `PATHING_STARTED + MUST_YIELD`.
- Existing task handover or task sync: activate Wuhuan -> P2/P1 -> `PATHING_STARTED + MUST_YIELD`, or `TASK_FINISHED + STOP_CHAIN`.
- World-map search: type target -> click the green coordinate link -> continue to the next pathing stage if available; do not yield at a middle destination option if the task can immediately trigger mini-map/NPC movement.
- Startup checks such as incense/shoe/map tracking: `READY_TO_CONTINUE + CONTINUE_CHAIN`.

### 10.4 Recent important local changes to know

These are important changes from the Desktop session. They may already be in the dirty working tree and should not be casually reverted:

- `FiveRingTask`
  - Uses post-combat `TASK_VERIFYING` branch.
  - Skips movement/dialog in that branch.
  - Performs immediate first-aid check.
  - Re-enables `needTaskSync` after post-combat verification.
  - Then allows `syncTaskState(...)` to finish the task if the task panel is empty.
  - After `GIVE_ITEM_DONE`, re-enables `needTaskSync` and continues toward P2/P1 instead of stopping the loop to clear follow-up story dialogs.
- `BattleRadarService`
  - Added combat-exit miss debounce.
  - Avoids first-aid checks while `TASK_VERIFYING`.
  - `TASK_VERIFYING` polling interval shortened.
- `PlayerStateService`
  - Added immediate first-aid path so 五环 can run post-combat heal without waiting the normal delay.
  - 摄妖香 logic was adjusted so failed bag searches do not fake-refresh incense state.
  - HP/MP post-combat checks now move the mouse away before bar screenshots, use a small sample area instead of one pixel, use a higher-percentage point as an anti-false-positive check, and require a second screenshot before right-click healing when a bar looks low.
- `GameStateUtil`
  - Coordinate-based movement detection became primary.
  - Stable coordinate can confirm stopped quickly.
  - Pixel fallback remains for coordinate-unknown cases.
- `BagService`
  - Input interruption while opening/clicking bag should be treated as stop, not false item-not-found.
- `WindowTaskControlService`
  - Logs UI stop source for selected/all window stops.
- `SummonSkillService`
  - Cloud/old auto-battle summon skill deletion logic was ported locally.

### 10.5 Current known risks / next validation

High-priority validation:

- Run 五环 once after the latest post-combat `needTaskSync=true` fix.
- Confirm when the fifth ring finishes:
  - logs show `[状态查岗]`;
  - if task panel is empty, logs show 五环 finished;
  - no further P2/P1 clicks happen after completion.
- Confirm post-combat no longer spends extra time on ordinary movement detection.
- Confirm battle exit debounce does not keep the bot in combat too long after battle truly ends.

Likely next fixes if logs still look wrong:

- If task completion still fails, inspect `QuestManagerService.activateTaskIfPresent("wuhuan", true)` and task-panel anchor/empty-list handling.
- If P2 fails too often and P1 keeps firing, inspect `p2_raw.png`, template names, and match thresholds.
- If post-combat dialog blocks P2/P1, decide whether to clear story dialog during pathing idle time rather than before task sync.

### 10.6 Current parallel ownership update

This section supersedes the older thread split below when multiple Codex agents are active.

Short-term multi-agent coordination lives in:

```text
docs/ACTIVE_WORK.md
```

Rule of thumb:

- Update `docs/DHXY_CONTEXT.md` only for long-lived architecture decisions, tested conclusions, or session-resume guidance.
- Update `docs/ACTIVE_WORK.md` whenever an agent starts work, claims files, finishes a phase, becomes blocked, or needs another agent to provide an interface/field.
- Every agent should read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and `docs/ACTIVE_WORK.md` before editing files.

Current active split:

Named agents:

- 何黎: framework / multi-window foundation
- 谢帅: summon skill / auto battle
- 唐德: UI

#### 何黎: framework / multi-window foundation

Owner: framework / multi-window agent.

Scope:

- Keep the multi-window task framework clean and extensible.
- Maintain the shape: `MultiWindowTaskManager -> WindowTaskRunner -> WindowTaskQueue -> TaskType`.
- Keep one-window execution on the same multi-window path.
- Keep real keyboard/mouse input serialized through `InputSequences` / `InputActionQueue`.
- Remove or quarantine old single-window runner/control leftovers.
- Document architecture decisions in this file.

Recent framework status:

- Old single-window `GameWindowService` and old `runner/execution/TaskRunner` / `TaskQueue` classes are already deleted in the current dirty tree.
- `WindowTaskQueue` is now a first-class wrapper with `empty()`, `of(...)`, `single(...)`, `toLogText()`, `toDisplayText()`, and an explicit `WindowTaskFailurePolicy`.
- The default queue failure policy is `CONTINUE_ON_FAILURE`, preserving the current behavior: a `FAILED` task marks the queue result as failed but does not stop later queued tasks. `STOPPED` still stops the queue. `STOP_ON_FAILURE` exists for future queues that should fail fast, but current UI defaults do not select it.
- `WindowTaskStartRequest` can carry a `WindowTaskQueue`, not only a single `TaskType`.
- `WindowTaskControlService` routes `SAME_TASK` through `startSameQueue(...)`.
- `MultiWindowTaskManager.submitWithResult(...)` wraps single tasks into `WindowTaskQueue.single(...)`.
- `WindowTaskRunner` executes each registered window's queue sequentially on that window's own runner thread.
- `WindowTaskRunner` aggregates each queue into a final queue result and writes last queue display/result/message/policy into `WindowRuntimeContext`.
- Queue completion does not overwrite per-task `lastResult` / `lastResultMessage`; those fields describe the last concrete task event, while `lastQueueResult` / `lastQueueMessage` describe the queue as a whole.
- Stop/cancel handling must not rely only on `Future.isDone()`. A cancelled future can be marked done before the task runner thread has fully exited; the window should not accept another queue until the runner thread is no longer alive.
- `WindowTaskSnapshot` exposes running queue display/progress/size/policy, last queue display/result/message/policy, and `isAcceptingTaskQueue()`.
- `WindowTaskSnapshot` also exposes structured player identity from the bound per-window `GameContext.State.me`: `playerName`, `playerId`, and `serverName`. UI should prefer these fields over parsing the native window title, using title parsing only as a fallback.
- `WindowRuntimeContext.selectedTaskType` is the persistent configured/default task for the window. Runtime events such as queued/started tasks must update `lastTaskType` / running-task snapshot state, not mutate `selectedTaskType`.
- `WindowTaskSubmitResult` has structured `WindowTaskSubmitStatus`; `WindowTaskCommandDetail` preserves submit status, queue display text, and queue failure policy for UI/log diagnostics.
- Per-window startup initialization is handled by `WindowTaskStartupInitializer`. The default initializer calls `NavigationService.ensureMapTrackingOption()` before normal tasks and skips this step for `debug_coordinate`.
- `WindowTaskRunner` sends `ALT+6` twice after startup initialization and before the concrete task executes, so all tasks get the current visibility preparation without each task implementing it.
- Per-window pause/resume is cooperative and safe-point based. Pause lives on the active `RunningTaskHandle` through `TaskPauseToken`; existing `TaskExecutionContext.throwIfStopRequested()` checkpoints also wait while paused. Pause does not stop the global `InputActionWorker`, and an already-submitted physical input sequence finishes before the task can pause at the next checkpoint. Stop still wakes and interrupts paused tasks.
- 五环 has additional pause checkpoints around preparation, initial NPC/task acceptance, post-combat supply, dialog handling, task sync, and P2/P1 pathing triggers so the first pause test should not have to wait for a whole loop to finish.
- Maven compile passed after these framework changes with `mvn -q -DskipTests compile`.

Important mental model:

```text
Windows are long-lived workers/resources.
Tasks go into each window's WindowTaskQueue.
Physical input goes into the one global InputActionQueue.
```

Do not put windows themselves into a global task queue. That would reduce useful multi-window concurrency. A window may OCR/wait/track state independently; only physical input must be globally serialized.

Avoid touching `SummonSkillService` and `AutoBattleTask` while another agent owns them.

#### 谢帅: summon skill / auto battle feature thread

Owner: this conversation / summon skill and auto battle agent.

Scope:

- `SummonSkillService`
- `AutoBattleTask`
- summon skill deletion integration
- auto-battle behavior and related templates/config

This thread may inspect framework APIs, but should avoid broad edits in `WindowTaskRunner`, `WindowTaskControlService`, `MultiWindowTaskManager`, and 五环 core flow unless coordinated.

#### 唐德: UI thread

Owner: UI agent.

Scope:

- JavaFX UI cleanup and extension.
- Make window/task controls clearer.
- Show per-window task queue information when the model is ready.
- Add UI entry points for future selected task queues.
- Improve display text/status panels without changing task behavior.

Good UI targets:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- UI view/model classes that still exist after old UI cleanup
- `WindowTaskSnapshot`, `WindowSystemSnapshot`, `WindowTaskCommandResult` display
- task selection and future queue-building controls

Suggested UI searches:

```bash
rg -n "Button|TableView|ComboBox|WindowTaskStartRequest|WindowTaskSnapshot|WindowSystemSnapshot|WindowTaskCommandResult" src/main/java/com/bot/dhxy/ui src/main/java/com/bot/dhxy/window
rg -n "startSameTask|startSameQueue|selectedTask|detectedRole|WindowTaskQueue" src/main/java
```

UI thread constraints:

- Do not change 五环 behavior while doing UI work.
- Do not edit `SummonSkillService` / `AutoBattleTask`; another agent owns them.
- Prefer UI-only changes and small model additions.
- If UI needs new backend fields, coordinate with framework thread first.

UI thread update 2026-05-20:

- `MainWindowController` now has a small task-queue builder for selected windows.
- New UI controls let the user add task types into a pending queue, remove one queue item, clear the queue, and start the queue on selected windows.
- Queue start uses the existing backend path: `WindowTaskStartRequest.sameQueue(...) -> WindowTaskControlService.start(...) -> startSameQueue(...)`.
- The window table now shows running queue progress and running queue display text from `WindowTaskSnapshot`.
- Selected-window commands in `MainWindowController` now capture selected window ids on the JavaFX thread before dispatching backend work to the background worker.
- This change intentionally does not alter 五环, `SummonSkillService`, `AutoBattleTask`, input queue behavior, or task execution semantics.
- Maven compile passed after this UI queue entry change with `mvn -q -DskipTests compile`.

Recommended prompt for the UI thread:

> 请先阅读 AGENTS.md 和 docs/DHXY_CONTEXT.md，尤其是 `10.6 Current parallel ownership update`。你负责 UI 线程，只做 JavaFX UI/显示/控制入口整理，不改五环核心逻辑，不改 `SummonSkillService` 和 `AutoBattleTask`。当前框架方向是 `MultiWindowTaskManager -> WindowTaskRunner -> WindowTaskQueue -> TaskType`，窗口是长期 worker，任务进入每个窗口自己的队列，全局物理输入进入 `InputActionQueue`。请先扫描 `MainWindowController`、窗口 snapshot/result model、任务启动按钮和表格展示，给出 UI 清理方案；如果改代码，保持小步、不要碰业务任务行为。

Recommended prompt for the summon skill / auto battle thread:

> 请先阅读 AGENTS.md 和 docs/DHXY_CONTEXT.md，尤其是 `10.6 Current parallel ownership update`。你负责 `SummonSkillService` 和 `AutoBattleTask` 相关功能，不要改 `FiveRingTask`、`BattleRadarService`、`QuestManagerService`，也尽量不要改 `WindowTaskRunner` / `MultiWindowTaskManager` / `WindowTaskControlService`。如果发现需要框架支持，先输出需要的接口或字段，不要直接大改框架。

### 10.7 Older suggested parallel thread split

Use this split if running multiple Codex agents/threads.

#### Thread A: active hotfix thread

Owner: current conversation.

Scope:

- Read latest `logs/dhxy-console.log`.
- Fix 五环 behavior regressions.
- Touch core flow files only when logs justify it.

Allowed files:

- `FiveRingTask.java`
- `BattleRadarService.java`
- `QuestManagerService.java`
- `DialogService.java`
- `GameStateUtil.java`
- closely related services as needed

Avoid broad refactors.

#### Thread B: read-only architecture scanner

Scope:

- Do not edit files unless explicitly approved later.
- Scan for direct input calls that bypass `InputSequences`.
- Scan for old single-window/title-search call paths.
- Scan for task completion paths and places that can bypass `syncTaskState`.
- Produce a report with file/line findings and proposed small patches.

Suggested searches:

```bash
rg -n "inputProvider\\.|click|press|Alt\\+|submitAndWait|submitExclusiveAndWait" src/main/java
rg -n "locateWindow|findWindow|title-search|fullWindowTitle|getWindowBase" src/main/java
rg -n "needTaskSync|syncTaskState|FINISHED|activateTaskIfPresent|triggerWuHuanNativePathing" src/main/java
```

#### Thread C: future feature preparation

Scope:

- Work on lower-risk feature modules not touching 五环 core flow.
- Possible targets:
  - `SummonSkillService` integration plan.
  - HP/MP percentage UI/config review.
  - Coordinate digit template diagnostics.
  - Documentation of dialog request/result enums.

Do not edit `FiveRingTask`, `BattleRadarService`, or `QuestManagerService` while Thread A is debugging them.

### 10.7 Recommended prompt for a new parallel Codex thread

Use this for a scanner/helper thread:

> 请先阅读 AGENTS.md 和 docs/DHXY_CONTEXT.md，尤其是 `Desktop session update: 2026-05-20`。你是并行只读扫描线程，不要改代码。请扫描 DHXY 项目里所有可能绕过输入队列、绕过任务完成判定、或仍使用旧单窗口/window title 搜索的路径。输出文件/行号、风险说明、建议小补丁，不要动 `FiveRingTask` / `BattleRadarService`。

Use this for a future-feature thread:

> 请先阅读 AGENTS.md 和 docs/DHXY_CONTEXT.md，尤其是 `Desktop session update: 2026-05-20`。你负责非五环核心流程的准备工作，不要改 `FiveRingTask`、`BattleRadarService`、`QuestManagerService`。请整理 `SummonSkillService`、血法百分比配置/UI、坐标模板诊断中最适合先做的一项，并给出小步实施计划。

## 11. How to resume work in Codex Desktop

After pulling the latest `dev`, tell Codex:

> 先读 AGENTS.md 和 docs/DHXY_CONTEXT.md。我们继续 DHXY 项目。当前测试已经确认 NPC 首点公式是对的，debug 可以点到 NPC。下一步主要跑正常五环，验证正式 `npcClick:firstShotMoveClick` 是否解决正式点击问题，同时继续看多窗口稳定性。

Recommended first local checks:

```bash
git checkout dev
git pull
```

Build/run locally and inspect:

```text
logs/dhxy-console.log
logs/tracker-coordinate.log
```

## 12. HWND Screenshot Direction

This is a long-lived architecture finding from 2026-05-22.

A JavaFX debug button was added for a per-HWND screenshot experiment. It captures selected registered windows using:

- `PrintWindow(hwnd, memoryDc, PW_RENDERFULLCONTENT)`
- `GetWindowDC(hwnd) + BitBlt(...)`

User-tested cases:

- two game clients overlapped;
- browser covering the game client;
- IntelliJ IDEA covering the game client.

Observed result:

- both selected game HWNDs produced non-blank images;
- each image contained its own bound game window content;
- the covering browser/IDE/other game window did not appear in the captured image.

Conclusion:

- For the user's current machine and 大话西游2 classic client, per-HWND capture appears usable even when the game window is covered.
- This can potentially replace many `Robot.createScreenCapture(...)` visible-screen screenshots and reduce the five-window foreground/focus thrashing problem.
- Minimized-window behavior is still unknown and must be tested separately; covered and minimized are different cases.

Implementation direction:

- Extract the experiment into a reusable bound-window capture service.
- Let `GameClientTracker.captureToMemory(...)` and `captureToFile(...)` prefer HWND capture when a current `WindowRuntimeContext.nativeBinding` exists.
- Convert existing absolute screen rects into window-relative crops using the tracked window base.
- Keep Robot capture as fallback while logging the capture provider, e.g. `HWND_PRINTWINDOW`, `HWND_BITBLT`, or `ROBOT`.
- Do not remove the existing Robot path until Wuhuan, battle radar, minimap coordinate, dialog, task panel, and bag scans are validated with the HWND provider.

Initial implementation:

- `BoundWindowCaptureService` is the reusable per-HWND capture provider.
- `bot.window.hwnd-capture-enabled=true` enables the new provider under the existing multi-window isolation settings.
- `bot.window.hwnd-capture-fallback-to-robot-enabled=true` keeps the old Robot path as fallback.
- `GameClientTracker.captureToMemory(...)` and `captureToFile(...)` try HWND capture first when a current bound `WindowRuntimeContext` exists; successful HWND capture does not focus/foreground the game window.

## 13. Background Input Direction

This is a long-lived architecture finding from 2026-05-22.

A JavaFX debug experiment posted Win32 `WM_*` messages directly to selected bound HWNDs and compared before/after HWND screenshots. The experiment does not use Robot/SendInput, does not move the physical cursor, and does not require foreground focus.

Tested result:

- Background keyboard message for `Alt+Q` posted successfully, and before/after HWND screenshots showed the task panel opening.
- Background keyboard message for `Alt+1` posted successfully, and the user confirmed it opened the mini-map. This covers the number-key Alt shortcut path.
- Top-level `WM_LBUTTON*` / `WM_RBUTTON*` mouse messages posted successfully but produced no visible game response.
- Posting right-click messages to the largest visible child `Win32Window` also produced no visible game response.

2026-05-25 privilege/integrity follow-up:

- The background keyboard result depends on Windows process integrity level.
- When the game process is elevated/high-integrity and the Java/Codex/PowerShell process is normal medium-integrity, direct `PostMessage` to the game HWND fails with `lastError=5` (`ERROR_ACCESS_DENIED`).
- This was reproduced through the retired no-UI direct window-message probe and the formal `InputActionWorker -> BoundWindowKeyboardService` path.
- Running the same no-UI probes elevated, so Java and the game are at matching high integrity, makes `Alt+1` post successfully again. The formal input queue path logs `hwndKeyboard shortcut=Alt+1 success=true`, and all four `WM_SYSKEY*` messages have `lastError=0`.
- Therefore a high-integrity game client needs the Java app/IDE/probe to run elevated too if we expect background keyboard shortcuts. Otherwise the worker will log the HWND-keyboard failure and fall back to focused real input.

Conclusion:

- Background keyboard input is usable/promising for tested shortcuts and can be expanded shortcut-by-shortcut.
- Background mouse input via normal `WM_MOUSE*` messages should be treated as unavailable for now.
- Mouse clicks, drags, and right-click supply actions should still use serialized real input with focus.
- Automation design should separate:
  - background screenshot patrol through HWND capture;
  - background keyboard shortcuts where individually validated;
  - focused real mouse input only when an actual mouse action is required.

Initial implementation:

- `BoundWindowKeyboardService` sends verified Alt shortcuts directly to the current bound HWND.
- `bot.window.hwnd-keyboard-enabled=true` enables this under the existing multi-window/bound-window switches.
- `InputActionWorker` uses HWND keyboard only for queued requests containing supported `PRESS_ALT_*` actions and `SLEEP`.
- Supported background shortcuts are `Alt+1`, `Alt+2`, `Alt+4`, `Alt+6`, `Alt+8`, `Alt+T`, `Alt+O`, `Alt+E`, and `Alt+Q`.
- Mixed sequences, mouse actions, drags, right-clicks, text input, Enter, Ctrl, and unverified shortcuts still focus the bound window and use the existing real-input provider.
- If HWND posting is disabled, unavailable, or fails, the worker focuses the bound window inside the active input transaction and falls back to the original real-input shortcut method.

Diagnostic logging:

- `WindowInteractionMetricsService` emits cumulative `Interaction metrics` log lines per `windowId`.
- It counts focus attempts, HWND capture successes, Robot capture successes, capture failures, and HWND keyboard successes/failures.
- After a five-window run, grep `Interaction metrics` in `logs/dhxy-console.log` to estimate how much foreground focus remains and whether screenshots/shortcuts are using the background paths.
- The same service also writes `logs/interaction-metrics-dashboard.html`, a local auto-refreshing dashboard. The JavaFX diagnostics panel has a `统计 Dashboard` button that writes the latest metrics and opens the HTML file.

## 14. License Worker Deployment Reminder

This is a long-lived deployment reminder from 2026-05-22.

Local projects involved:

- `D:/mavenProject/dhxy-license-worker`
- `D:/mavenProject/dhxy-auto-battle`
- `D:/mavenProject/DHXY`

Current intended design:

- One shared Cloudflare Worker handles license verification.
- License codes are separated by `licenses.app_id`.
- DHXY main project must use `appId=dhxy`.
- auto-battle must use `appId=dhxy-auto-battle`.
- The worker-side `/api/license/renew` endpoint extends a matching license by 30 days.

Important user preference:

- The user explicitly does not want to run/deploy the worker or remote D1 migration yet.
- Before generating real license codes later, remind the user to first deploy/run the worker-side changes and apply remote D1 migration `migrations/0002_add_app_id.sql`.
- Without that migration/deploy, remote licenses will not have reliable `app_id=dhxy` / `app_id=dhxy-auto-battle` separation, and the renewal endpoint/action responses may not exist remotely.

## 15. Local OCR Direction

This is a long-lived OCR direction from 2026-05-24.

Current decision:

- Use local RapidOCR as the preferred experiment path for new OCR validation.
- Do not globally replace Baidu OCR until accuracy is validated on real game screenshots.
- Use `bot.dhxy.ocr.provider=hybrid` for current validation: normal business flows try local OCR first, and selected target-matching paths retry Baidu when local OCR text does not match the expected business target.
- Debug-only probes may call the local sidecar directly when the point is to measure local OCR accuracy.
- Keep the current `config/vision_memory.json` structure. It is the shared historical vision memory for OCR ROI attempts, player anchors, NPC/monster coordinates, predicted/actual click points, camera/scale context, and verification outcomes. Do not migrate/split this JSON only for OCR ROI policy separation unless the user explicitly reopens the schema decision.

Local OCR sidecar:

- Script: `scripts/local_ocr_server.py`
- Requirements: `scripts/requirements-local-ocr.txt`
- Default endpoint: `http://127.0.0.1:18761`
- Health check: `GET /health`
- OCR endpoints: `POST /ocr/text`, `POST /ocr/words`

Java integration:

- `TextRecognizer` supports OCR providers: `baidu`, `local`, `compare`, and `hybrid`.
- `TextRecognizer.getAllTextResultsLocalOnly(...)` is available for debug-only local OCR.
- `TextRecognizer.getAllTextResultsForMatch(...)` should be used when the caller knows what text/pattern it needs. In `hybrid` mode it logs whether local OCR matched and whether Baidu fallback rescued the match.
- `docs/LOCAL_OCR_EXPERIMENT.md` contains install/start/config notes.
- The old no-UI yellow OCR probe and JavaFX player-name OCR debug entry were retired during the CR140 release cleanup. Use the shared text-color OCR service or saved testcase images for future checks.

Current text-color OCR method:

- Shared service: `GameTextLineOcrService`.
- Purple player-name text uses the stable purple hue mask, connected-component filtering, line packing, local OCR, and the existing `LocationVisionService` name-fragment/anchor logic. Do not replace this with target fuzzy matching unless purple starts failing in real tests.
- Yellow NPC/name text uses a looser yellow mask plus an optional nearby-shadow expansion. The reusable target path scans yellow candidate lines one by one, runs local OCR per line, and compares against the known target name using edit-distance plus longest-common-substring matching. This is intended for known NPC/monster names such as `无名小妖` or `灵兽村使者`, where OCR may miss or misread one character.
- Generic yellow debug output still writes a compact all-line image for visual inspection; target-aware callers should use `findYellowTarget(...)` so partial strings like only `小妖` do not count as a reliable hit.
- Yellow fallback candidate extraction is now a formal API:
  - `GameTextLineOcrService.findYellowTextCandidateResult(raw, washedPath, overlayPath)` returns a `TextCandidateScanResult`.
  - `TextCandidateScanResult.candidates()` is immutable and sorted by descending score.
  - Candidate rectangles/click points are image-local to the supplied screenshot. A caller that cropped a window region must add the crop origin and window base before using the points as physical input.
  - `NpcClickService.clickNpcSmart(...)` uses this API when `findYellowTarget(...)` does not match the exact target. It converts ranked candidates to screen-absolute points and adds them to Ctrl-menu probe origins. It does not left-click fallback candidates directly.
  - Current yellow washing keeps real NPC yellow samples including dark edge pixels and bright strokes, rejects the stall/vendor gold family around `203,181,88..106`, and penalizes high/skinny or weak sparse fragments.

Next validation:

- User should run `本地OCR测名字` on real windows and compare the logged local OCR words/anchor coordinates.
- If local OCR is reliable enough, switch selected OCR-heavy paths to `local` or use `hybrid` for normal flows.

## 16. Xiuluo Pause / Wubei Next Direction

This is a short long-term pointer from 2026-05-29.

Current decision:

- 修罗 V2 work is paused and stored for later continuation.
- The next urgent task line is 五倍.
- The detailed 修罗 handoff is in `docs/ACTIVE_WORK.md`, entry:
  `He Li - 2026-05-29 修罗暂存与五倍切换交接`.

When starting 五倍:

- Reuse the common task infrastructure already shaped by 修罗:
  - `NavigationService`
  - `NpcClickService.clickNpcSmart(...)`
  - `DialogService.handleDialog(...)`
  - `TaskTransactionRunner` / `TaskTurnCoordinator`
- Do not create a separate private navigation/dialog/NPC-click stack for 五倍.
- 五倍 should define its own phases, templates, objective reading, retry/fallback policy, and task-specific dialog mapping.
- Generic maintenance such as 医宝宝、修装备、三技能 should stay as hooks until the shared maintenance service boundary is finalized.

