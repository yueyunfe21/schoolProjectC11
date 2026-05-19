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

`debug.npc-first-shot=true` was used to run a one-shot 墨意 NPC first-shot test through the 五环 task.

Confirmed by user:

- identity sync works after using bound title;
- `FINAL_CLICK_POINT` is correct;
- mouse can click the NPC when direct debug input is used;
- therefore the coordinate formula itself is likely correct.

After that, formal flow was changed so first-shot move and click are a single queue sequence.

### What should probably be tested next

Set:

```properties
debug.npc-first-shot=false
```

Then run normal 五环 again and inspect:

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
bot.window.scoped-temp-path-enabled=false
```

The scoped temp path switch is currently false because some older recognition code may still read fixed `images/temp` paths. However, newer service code often uses `WindowScopedTempPath.resolve(...)` directly where needed.

Temporary debug switch:

```properties
debug.npc-first-shot=false
```

Use:

```properties
debug.npc-first-shot=true
```

only to run the one-shot 墨意 first-click debug through the 五环 task.

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

Debug mode:

```java
@Value("${debug.npc-first-shot:false}")
private boolean debugNpcFirstShot;
```

When true, `execute(...)` calls `executeNpcFirstShotDebug(...)` and does not run full 五环.

### 6.2 NpcClickService

File:

```text
src/main/java/com/bot/dhxy/service/NpcClickService.java
```

Important methods:

- `clickNpcSmart(...)`
- `debugClickNpcSmartFirstShot(...)`
- `executeMoveClickAndVerify(...)`
- `scanMenuAndVerifyDirect(...)`

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

## 10. How to resume work in Codex Desktop

After pulling the latest `dev`, tell Codex:

> 先读 AGENTS.md 和 docs/DHXY_CONTEXT.md。我们继续 DHXY 项目。当前测试已经确认 NPC 首点公式是对的，debug 可以点到 NPC。下一步主要跑正常五环，验证正式 `npcClick:firstShotMoveClick` 是否解决正式点击问题，同时继续看多窗口稳定性。

Recommended first local checks:

```bash
git checkout dev
git pull
```

Open:

```text
src/main/resources/application.properties
```

Make sure for normal 五环:

```properties
debug.npc-first-shot=false
```

Then build/run locally and inspect:

```text
logs/dhxy-console.log
logs/tracker-coordinate.log
```

If testing first-shot only:

```properties
debug.npc-first-shot=true
```

Then select/start 五环 in UI.

