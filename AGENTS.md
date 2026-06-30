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

2. Before claiming or implementing any sprint card, every agent must compare against the latest pushed code for the touched business path.
   - Treat the latest pushed code as the business-logic baseline. Current local business differences are not trusted during migration and must be restored to the pushed behavior by default.
   - Record the relevant baseline in `docs/ACTIVE_WORK.md` before editing: current branch, latest pushed commit if known, `git status`, and the relevant `git diff` / `git show` evidence for files being touched.
   - Framework migration work may move scheduling, parking, wakeup, ownership, or diagnostics plumbing, but it must not change task business decisions by accident.
   - If a patch would change task phase semantics, prompt interpretation, OCR/template/click/navigation order, fallback order, or when a probe/NPC/dialog is considered resolved, stop and write the conflict in Markdown; do not keep the local behavior as part of migration unless the user explicitly opens a new behavior-change story.
   - Do not convert runner/ready-event negative signals into new business truth unless the latest pushed business logic already did that.

3. Do not delete useful comments unless necessary.
   - Previous work accidentally removed some of the user's comments. Avoid repeating this.

4. Prefer small, targeted commits.
   - The user tests frequently and wants to understand each change.

5. When the user says a point is already tested, treat it as known unless logs contradict it.

6. Use Chinese in conversation with the user unless they switch language.

7. Visual matching or click-target changes must be verified through testcase replay.
   - This applies to minimap matching/clicking, world-map search/result clicking, task tracker green-text clicking, NPC/template matching, dialog option matching, and any code that changes where the mouse will click based on screenshot/OCR/template output.
   - Do not rely only on verbal reasoning, one live observation, or log text. Use or create a repo-local testcase image under an appropriate `images/test-cases/...` folder, run the matching/click algorithm against that testcase, and produce a marked output image showing the detected target and final click point.
   - The marked output must make the important points visible: for example destination OCR anchor, matched text/template box, and actual click coordinate. The user should be able to inspect the image and confirm the red point/box is correct.
   - If no testcase exists for the scenario being fixed, save the raw screenshot first, then add/reuse a small replay/debug tool rather than testing only against the live game window.
   - After the change, record the testcase input, output image path, and command/tool used in `docs/ACTIVE_WORK.md`.

8. Investigation-first rule for user questions.
   - When the user asks why something happened, asks whether a behavior is correct, asks where/how to change something, or asks for a discussion/plan, do not immediately edit code.
   - First inspect the relevant logs, screenshots, call path, state transitions, and existing implementation. Then explain the likely root cause and a concrete modification plan.
   - Only start code changes after the user clearly approves the proposed plan, for example by saying "可以", "按这个改", "继续做", or an equivalent explicit approval.
   - This rule does not block tiny documentation-only updates requested directly by the user, but it does apply to behavior, navigation, OCR/template matching, runner/watcher, task flow, and input changes.

9. CR log-audit rule.
   - Before auditing runtime logs or writing a run report, read the current sprint/CR board and identify every CR whose status is not clearly Done/Deprecated/Closed.
   - The audit must check each open/review CR against the relevant log evidence, not just the newest visible symptom.
   - If an open/review CR fails in the logs, record the evidence in the CR/report and immediately dispatch a sub-agent to fix that failing CR unless the user explicitly says not to modify code.
   - Do not leave a failed CR as only a chat summary. The repair owner, expected files, and fresh-runtime verification point must be recorded in Markdown.

10. CR dashboard sync rule.
   - When an agent claims, creates, updates, reopens, closes, or changes the status/owner/summary of any CR in `docs/PACKAGE_ARCHITECTURE.md`, it must refresh the static dashboard data before handing off.
   - Before running the dashboard generator, make the CR table row user-facing fields readable in Chinese. The dashboard reads the CR table directly, so `Status`, touched-area text, and especially the summary/description must be a concise Chinese translation/summary of the card, not raw English planning text. Keep technical identifiers, enum names, file names, and log keywords in backticks when useful.
   - Run `node scripts/generate-cr-dashboard-data.js` from the repository root after the Markdown CR table/card update.
   - Include the resulting `docs/cr-dashboard-data.js` change together with the Markdown change, so `docs/CR_DASHBOARD.html` reflects the latest CR status after a browser refresh.
   - If the script cannot run, record that as a blocker in `docs/ACTIVE_WORK.md` and tell the user that the dashboard snapshot is stale.

11. CR card persistence rule.
   - Any sprint/CR task discussion that produces a decision, review opinion, blocker, repair direction, verification result, fresh-runtime acceptance point, or "do not run yet" warning must be written back into the corresponding CR card in `docs/PACKAGE_ARCHITECTURE.md` before handoff.
   - Do not leave CR review conclusions only in chat, a sub-agent reply, terminal output, or `docs/ACTIVE_WORK.md`. `docs/ACTIVE_WORK.md` may summarize the active pass, but the CR card is the durable source of truth for that task.
   - If a review finds a P0/P1/P2 issue, record the severity, evidence, affected files/methods, required repair direction, and verification/fresh-runtime gate inside the CR card.
   - If the CR table row status/owner/summary changes as a result, follow the CR dashboard sync rule and regenerate `docs/cr-dashboard-data.js`.

## Code documentation rule

- Temporary lightweight policy: keep comments useful, but do not turn every small code change into a documentation pass.
- New or modified public APIs only need JavaDoc when the behavior is non-obvious, externally reused, or safety-sensitive. Simple UI handlers, trivial getters/setters, and self-explanatory private helpers do not need forced JavaDoc.
- Main/high-frequency methods must have top-level JavaDoc that explains the method inputs and output. For every parameter, state what it represents and include coordinate space/unit/nullability when relevant. This is mandatory for navigation, OCR, input, window binding, task execution, and UI command entry methods.
- Must document logic that is risky or hard to infer:
  - physical input, focus, HWND/window binding, screenshot provider choice;
  - OCR/template matching fallback order;
  - task stop/pause handling, task transaction/yield decisions, retry policy;
  - persisted data formats, config switches, and debug-only behavior;
  - coordinate-space conversions, especially screen-absolute vs window-relative values.
- For complex multi-stage methods, add short block comments at the important decision points only. The goal is to make the flow reviewable, not to write a full SOP for every branch.
- Comments should explain why the logic exists, what invariant or safety rule matters, and what should not be changed casually. Avoid comments that merely repeat the code.
- When touching undocumented code, add concise comments only for the touched risky section. Do not broaden the edit just to document unrelated old code.
- Do not remove existing useful comments unless the code they describe is removed or the comment is being replaced with a more accurate one.
- Java file layout rule: keep public classes, public APIs, and the main workflow near the top. Private nested helper types (`private class`, `private record`, `private enum`, private interfaces) should be placed at the bottom of the enclosing class/file, after the main public and private workflow methods, unless Java syntax makes that impossible.
- Before adding a new method, class, helper, wrapper, or nested layer, first inspect the existing workflow and decide whether the change can be made by modifying the current method in place. Prefer adjusting the existing flow, return value, or branch structure when that preserves readability and ownership. Add a new function only when it creates a real reusable boundary, removes meaningful duplication, or isolates a distinct policy. Avoid stacked `internal`/wrapper/helper methods and nested call layers whose only purpose is to route to almost the same logic.
- Do not add trivial wrapper layers just to expose a second name for the same operation. If an existing method can naturally return useful data while preserving its side effect, prefer changing that method's return type and letting callers ignore the return value. Add a wrapper only when it enforces a real boundary, policy, or compatibility requirement.
- No "wrapper nesting" anywhere in the codebase. Do not create a method whose main job is to call another same-scope helper which then calls another helper for the same decision. A caller should either show the actual decision inline, or call one clearly named collaborator that owns a real boundary. Do not hide behavior behind chains of one-line helpers, adapter helpers, or "prepare/handle/resolve" nesting when the caller still has to understand all layers to review the flow. This applies to all Java code, not only 五倍.
- Task stop/pause checkpoint rule: use `TaskCheckpoint` directly for task stop/interruption checkpoints. Do not add local wrappers such as `checkpoint(...)`, `checkpointTask(...)`, `throwIfStopRequested(...)`, or ad-hoc `Thread.currentThread().isInterrupted()` exception checks in task/service code. A local helper is allowed only when it adds real domain behavior beyond the standard checkpoint. Boolean loop guards in worker/debug/control loops may still check interruption directly.

## Java / Spring / Lombok / logging conventions

- Use the existing Spring Boot style instead of manual object wiring. Services should normally be Spring beans (`@Service`, `@Component`, `@Configuration`, etc.) and dependencies should be injected through constructors, preferably with Lombok `@RequiredArgsConstructor` when it matches nearby code.
- Do not create ad-hoc singleton/service holders or manually `new` service dependencies inside business code. If a dependency is a real collaborator, inject it.
- Model/request/result classes should live in an appropriate model package, not inside service implementation packages unless they are truly private implementation details.
- Request/result/value objects should follow the existing Lombok style: use `@Value` + `@Builder` for immutable data objects, and use `@Builder.Default` for defaults. Static convenience factories are allowed, but they should build through `builder()` rather than bypassing the builder path.
- Use enums for stable operation/status/policy values instead of hard-coded strings when the value crosses service/task boundaries.
- Use SLF4J logging (`private static final Logger log = LoggerFactory.getLogger(...)`, or the project's existing Lombok logging style if a class already uses it). Do not use `System.out.println` in normal application code; reserve it only for throwaway local debug tools when there is no logger context.
- Logs should include enough structured context to debug multi-window behavior: window id/title when available, source task, target map/NPC/coordinate, result status, elapsed time for high-latency paths, and whether input/screenshot used HWND or focused real input when relevant.

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

## Current known stable findings

The recent debug test confirmed:

- The old one-shot NPC first-click debug proved the formula can calculate a correct `FINAL_CLICK_POINT` for 墨意.
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

