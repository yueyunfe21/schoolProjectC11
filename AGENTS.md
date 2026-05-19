# AGENTS.md — DHXY Project Agent Instructions

This repository is the DHXY desktop automation project. When using Codex or any coding agent, read this file first, then read `docs/DHXY_CONTEXT.md` before making changes.

## Project goal

The project is a Java/Spring/JavaFX automation tool for 大话西游2 classic client workflows. The current major goal is to make the 五环 task run reliably across one or more game windows.

The intended final shape is:

- A JavaFX desktop controller can register multiple game windows.
- Each registered window has its own runtime state, role identity, native hwnd binding, screenshots, temp files, and task state.
- Multiple windows can run tasks concurrently without keyboard/mouse input crossing between windows.
- All real keyboard/mouse actions are serialized through a single input queue where needed, so only one physical input sequence runs at a time.
- Screenshot/OCR/template matching logic must use the correct window binding and correct temp files.
- The code should remain debuggable from local logs, especially `logs/dhxy-console.log` and `logs/tracker-coordinate.log`.

## Current branch

Use the `dev` branch unless the user explicitly says otherwise.

## Important user preferences

The user is actively debugging and expects direct, practical changes. Do not hide behind vague explanations. When making changes, explain exactly which file and logic changed.

Important behavior constraints:

1. Do not casually rewrite or bypass user-validated business logic.
   - Example: do not change `GameStateUtil.isMovingByPixelDiff()` just to avoid symptoms.
   - If a validated detector appears wrong, first inspect screenshots, call path, window binding, temp file path, and logs.

2. Do not delete useful comments unless necessary.
   - Previous work accidentally removed some of the user's comments. Avoid repeating this.

3. Prefer small, targeted commits.
   - The user tests frequently and wants to understand each change.

4. When the user says a point is already tested, treat it as known unless logs contradict it.

5. Use Chinese in conversation with the user unless they switch language.

## Safety and scope

This repository controls the user's local desktop/game client. Be cautious with anything that sends input.

- Do not make code that clicks automatically at startup without an explicit debug switch or UI action.
- Debug clicks should be behind configuration or invoked from an explicit task path.
- When adding debug modes, make them easy to turn off.

## Architecture principles that are now settled

### 1. Window binding is the source of truth

For multi-window mode, code must prefer the current `WindowTaskRunner` / `WindowRuntimeContext` binding instead of title-searching the first matching game window.

Important classes:

- `WindowRuntimeContext`
- `WindowTaskContextHolder`
- `WindowRegistrationRequest`
- `WindowNativeBinding`
- `WindowTaskRunner`
- `GameClientTracker`
- `ClientIdentityService`

Where possible, read the current window binding from `WindowTaskContextHolder.rawCurrent()` or `current()` rather than doing a global title search.

### 2. Identity sync should read the bound window title first

`PlayerStateService.syncMyIdentity()` calls `ClientIdentityService.scanAndSyncIdentity(...)`.

`ClientIdentityService` must prefer:

1. `WindowTaskContextHolder.rawCurrent().getNativeBinding().getTitle()`
2. `tracker.getFullWindowTitle()`
3. `tracker.locateWindow()` fallback

Reason: in debug mode or early startup, `GameClientTracker.fullWindowTitle` may not have been refreshed yet, but the current `WindowRuntimeContext.nativeBinding.title` already has the correct title.

### 3. Input queue is for physical input serialization

The system now uses `InputActionQueue` / `InputActionWorker` to serialize keyboard and mouse actions. This was introduced to fix multi-window input crossing.

Important classes:

- `InputAction`
- `InputActionRequest`
- `InputActionQueue`
- `InputActionWorker`
- `InputActionDeadLetter`
- `InputSequences`
- `WindowAwareInputCoordinator`
- `WindowFocusService`

All normal task mouse/keyboard actions should go through `InputSequences.submitAndWait(...)` or `submitExclusiveAndWait(...)`, unless explicitly doing a temporary local debug action.

### 4. Move + click must be one atomic sequence

This is a critical rule confirmed by testing.

Bad pattern:

```java
inputSequences.submitAndWait("move", List.of(
        InputAction.moveMouse(x, y),
        InputAction.sleep(150)
));

inputSequences.submitAndWait("click", List.of(
        InputAction.clickLeft(x, y, 100)
));
```

Why bad: another window can insert focus/move/click between the move and click.

Good pattern:

```java
inputSequences.submitAndWait("moveClick", List.of(
        InputAction.moveMouse(x, y),
        InputAction.sleep(150),
        InputAction.clickLeft(x, y, 100),
        InputAction.sleep(800)
));
```

Use this for:

- NPC first shot clicking
- OCR text click after moving
- Menu item click
- Any workflow where mouse position preparation and click are logically inseparable

### 5. Do not nest input queue calls inside the input worker callback

Do not call `inputSequences.submitAndWait(...)` from inside a callback passed to `submitExclusiveAndWait(...)`, because the only `InputActionWorker` is already executing the callback and cannot consume the newly queued request. This causes a queue-in-queue deadlock.

Inside an exclusive callback, use direct `InputProvider` calls only if you are already inside the serialized input section.

This was specifically fixed in `NpcClickService` for `npcClick:ctrlProbe`.

### 6. Focus is best-effort

Windows `SetForegroundWindow` may return false even when physical input can still work. Do not treat foreground verification failure as a hard failure.

`WindowFocusService` should attempt:

- `ShowWindow`
- `BringWindowToTop`
- `SetForegroundWindow`

Then continue quickly. Avoid repeated heavy `GetForegroundWindow` validation and warning spam.

### 7. Stop must be honored inside loops

Task stop is not enough if inner loops ignore interruption. Long loops must check `Thread.currentThread().isInterrupted()` or the execution context stop token where available.

Known sensitive loops:

- `NpcClickService` dense Ctrl probe loop
- OCR/menu scan loops
- retry loops
- long sleeps

Use interruptible sleeps when possible.

### 8. Temp files must be scoped where possible

Multi-window cannot share the same temp screenshot path unless that path is known safe. Prefer `WindowScopedTempPath.resolve(...)` for debug and OCR intermediate images.

Important temp/log files:

- `logs/dhxy-console.log` — full normal console/business logs
- `logs/tracker-coordinate.log` — tracker coordinate/window diagnostics
- window-scoped temp images such as `debug_npc_firstshot_center_raw.png`, `debug_npc_firstshot_player_washed.png`, `npc_menu_scan.png`, `npc_menu_clean.png`

## Logging expectations

The user often shares screenshots/log snippets. The best diagnostic file is now:

- `logs/dhxy-console.log`

Tracker-only details are in:

- `logs/tracker-coordinate.log`

Do not rely only on tracker logs. Business flow logs are essential.

## Current debug switch

There is a temporary debug switch:

```properties
debug.npc-first-shot=false
```

When set to `true`, starting the 五环 task does not run full 五环. It only runs the 墨意 NPC first-shot debug using the 五环 constants.

Use it to test `clickNpcSmart` first-shot coordinate calculation and click behavior.

After testing, set it back to `false`.

## Current known stable findings

The recent debug test confirmed:

- `debugClickNpcSmartFirstShot(...)` can calculate a correct `FINAL_CLICK_POINT` for 墨意.
- The point lands on the NPC.
- Direct input click can click the NPC.
- Therefore the coordinate formula is probably okay.
- The formal flow problem was the input sequence being split into move and click; this has been changed in `NpcClickService` using a combined move+click sequence.

## Files to inspect first for common issues

For multi-window binding:

- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowTaskContextHolder.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRegistrationRequest.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`

For physical input:

- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/window/interaction/WindowFocusService.java`

For 五环 flow:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/ClientIdentityService.java`

## How to start a new Codex session

Recommended first message to Codex:

> 请先阅读 AGENTS.md 和 docs/DHXY_CONTEXT.md。我们继续 DHXY 项目，现在主要看五环多窗口稳定性、输入队列原子动作、NPC 点击逻辑。不要大改已验证逻辑，先根据日志定位。

