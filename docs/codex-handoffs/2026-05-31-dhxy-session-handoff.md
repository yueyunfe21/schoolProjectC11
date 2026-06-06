# DHXY Codex Handoff - 2026-05-31

Reactivation prompt:

```text
We are continuing DHXY from this handoff. Read AGENTS.md, docs/DHXY_CONTEXT.md, docs/ACTIVE_WORK.md, and this handoff first. Inspect the current repo state before editing. Continue from the "Next Steps" section without assuming the old Codex chat history is available. Use Chinese with the user, preserve the dirty worktree, and do not revert unrelated changes from other agents.
```

## Repo / Branch

- Repo: `D:\mavenProject\DHXY`
- Branch: `dev`
- Project: Java / Spring / JavaFX desktop automation tool for 大话西游2 classic client workflows.
- Main local logs:
  - `logs/dhxy-console.log`
  - `logs/tracker-coordinate.log`
- Coordination docs:
  - `AGENTS.md`
  - `docs/DHXY_CONTEXT.md`
  - `docs/ACTIVE_WORK.md`

## Current Goal

The current working goal is to stabilize the multi-window DHXY controller and task execution flow while making the JavaFX UI usable enough for frequent manual testing.

Recent focus areas:

- Main control UI layout and task selection.
- Start / scan / window binding flow.
- Stop and pause responsiveness.
- Local OCR sidecar startup.
- Xiuluo V2 task flow and maintenance hooks.
- Reducing log noise enough for multi-window debugging.
- Preserving enough handoff context so old Codex history can be archived safely.

## Current Worktree State

The worktree is intentionally dirty. Multiple agents have been editing the repo at the same time.

Important rule for the next agent:

- Do not run `git reset --hard`.
- Do not use `git checkout --` to revert files unless the user explicitly asks.
- Do not delete or overwrite files just because they look unrelated.
- Read diffs before touching files that are already modified.
- If a file has unrelated changes, preserve them and make a small targeted edit around the requested area.

Observed dirty areas include:

- `docs/ACTIVE_WORK.md`
- many Java services/tasks/UI files under `src/main/java/com/bot/dhxy/...`
- config files such as `config/maps.json`, `config/transfer_choice_memory.json`, `config/vision_memory.json`
- task/template images under `images/template/...`
- new packages such as metrics, maintenance models, runner exception, wubei task code
- local `logs/` folder

## What We Already Completed

### UI main control and settings

- Main UI was moved toward a left-tab layout with a cleaner main control panel.
- Task selection was changed to small task cards with lightweight count badges.
- Task selection should not default to selecting 五环 on startup.
- Task queue selection should support multiple tasks in order; selection badges show order.
- Main page task count shortcut was introduced so users can see/edit task counts without first knowing to open Settings.
- Settings page was simplified toward game-internal settings only.
- Window registration controls in Settings were considered obsolete/redundant with Main Control and should stay removed/hidden unless a clear new use appears.
- Settings now includes task counts and summon skill options:
  - 修罗次数
  - 五环次数
  - 五倍次数
  - 天庭次数
  - 抓鬼次数
  - summon skill / 三技能 enable and interval
  - supply settings
- 五环次数 should be constrained to one or two runs, not arbitrary unlimited input.
- Settings are locked while tasks are running/paused/stopping. User decision: users should stop scripts first, then edit settings, then restart tasks. Avoid ambiguous hot config behavior.

### Main task entries cleanup

- UI should show only the newer Xiuluo task path:
  - `TaskType.XIULUO_V2` display name is now `修罗`.
  - Old `XIULUO` entry should not appear in task selection.
- Removed/hidden debug task entries from the main task selector:
  - `DEBUG_TEAM_ROLE`
  - `DEBUG_XIULUO_STORY_OBJECTIVE`
  - `DEBUG_XIULUO_TASK_PANEL_OBJECTIVE`
  - `DEBUG_XIULUO_MOCK_OBJECTIVE`
- Map calibration / map survey controls below task selection were hidden for now. Code may remain because user may revisit it later.

### Start / scan / stale hwnd binding

- User expected pressing Start to auto-scan game windows; users should not need to manually scan first.
- The intended behavior:
  - Start should call window registration / scan first.
  - If no windows are selected, start all latest accepting windows.
  - If windows are selected, after scan only keep selected IDs that still exist and are accepting task queue.
  - Old/stale hwnd must not be used after scan.
- `GameWindowRegistrationService.registerDetectedGameWindows(...)` should prune stale idle native-bound runners not present in the current scan result.
- This stale hwnd issue was previously motivated by logs like:
  - `bound-geometry-live-delta | binding=(971,304 1036x783) | live=(0,0 0x0) | hwnd=351166`
  - capture provider `UNKNOWN`
  - `FOCUS_NOT_CONFIRMED`

### Stop / pause behavior

- Added global pause hotkey:
  - `Ctrl+Shift+F11` = pause all
  - `Ctrl+Shift+F12` = emergency stop all
- Stop button wording was clarified:
  - main toolbar red button became `停止所选`
  - a visible `停止全部` button was added next to it
- Backend stop result is now more honest:
  - `WindowTaskRunner.stopCurrentTask()` returns boolean.
  - `MultiWindowTaskManager.stop(...)` and `stopAll()` return accepted stop counts.
  - `WindowTaskControlService.stopWindows(...)` distinguishes `已请求停止`, `当前没有运行任务`, and `窗口不存在`.
  - `WindowTaskControlService.stopAll()` reports actual accepted count rather than treating every registered window as success.
- A log investigation showed a perceived "stop did not work" case was actually only stopping selected `hwnd-264100A` while other windows continued.
- A later full selected stop showed all five windows got stop requests; auto-battle windows stopped immediately, Xiuluo leader stopped after current navigation/dialog stage exited.
- Pause responsiveness was improved in `NpcClickService.clickNpcByCtrlMenuScan(...)` by checking `TaskCheckpoint.throwIfStopRequested(...)` before start, before each probe, and after each probe.

### No-focus screenshot / dialog detection

- Goal was to keep pure screenshot/detection paths no-focus:
  - `DialogService.captureCurrentStoryImage(...)` should not go through focused input queue just to detect dialog type.
  - `readCurrentStoryGreenText` and capture-only methods should use no-focus detection when no click/keyboard is needed.
  - Clicking paths like `handleDialog`, `clickGreenTemplateOption`, and `fastClickStoryDialog` still need input queue / focus.
- `GameClientTracker.captureToMemory/captureToFile` should prefer `HWND_PRINTWINDOW` and only focus for Robot fallback when HWND capture fails and fallback is allowed.
- Logs should distinguish no-focus detection vs queued/focused detection and capture provider `HWND_PRINTWINDOW` vs `ROBOT`.

### Fast click story dialog restriction

- User wanted `fastClickStoryDialog` constrained:
  - It should mainly be called by UI cleaner paths.
  - If current window is a team member, only click fast story dialog while in combat.
  - If leader, allow existing configured story/dialog fast-click cases.
  - Avoid team members clicking story dialogs outside combat.

### Local OCR sidecar

- Added `LocalOcrSidecarService`.
- `MainWindowController` start paths call it to check/start local OCR in the background.
- Default intent:
  - health-check local OCR endpoint first.
  - if not responding, start `scripts/local_ocr_server.py` in a background thread/process.
  - do not block task start on OCR startup.
  - write sidecar logs to `logs/local-ocr-sidecar.log`.
- User explicitly wanted Start to ensure local OCR is running automatically.

### OCR / location / map learning direction

The user decided to move away from endless manual coordinate calibration and toward OCR / learned data.

Short-term goal:

- OCR over a broad game-window area first.
- Save enough successful observations so screenshot search areas can shrink over time:
  - left / right / top / bottom / center style candidate regions.

Long-term goal:

- If the system knows target NPC/monster/map coordinate and has enough learned observations, click directly without OCR.

Data that should be retained for future learning:

- map name
- player map coordinate
- target NPC/monster name
- target map coordinate
- screen/window-relative detection region
- OCR result / confidence / provider
- click point if a click succeeded
- window binding/base coordinate at capture time
- screenshot region/mask version
- task/source/reason
- whether the target is fixed, floating, or roaming

Important design preference:

- Business click services should not perform their own coordinate-space conversion if a coordinate service can return already converted usable coordinates.
- Store relative/model data internally if useful, but service callers should receive ready-to-use coordinates in the expected coordinate space.

### Minimap / sync position

- User asked whether `sync my position` uses local OCR.
- Desired fallback order should prefer faster local methods:
  - minimap/template reader if it is reliable
  - local OCR
  - Baidu OCR last
- Avoid introducing unnecessary mini-services if existing matching methods already cover the need.

### NPC / navigation modeling direction

- User wanted an NPC model/object rather than scattering NPC fields everywhere.
- Proposed NPC attributes:
  - name
  - map
  - x/y map coordinate
  - role/purpose such as task-accept NPC or combat target
  - target mobility type such as fixed/floating/roaming
  - extra aliases/templates/policies if needed later
- But the user later noted that `NavigationService` has moved toward request objects, and not every NPC attribute belongs in every request. Keep request objects lean and do not pass the whole NPC model where only target coordinate/name is needed.
- User disliked direct static constants inside `XiuluoTaskV2` around line 84 if structure is not standard. Prefer a central model/config place when it becomes real data.

### Navigation cleanup

- User questioned `navigateToMap` vs `navigateToMapInternal` because both had the same parameters and both were private.
- Decision: remove meaningless wrapper/internal layering where possible; keep one natural method name if the wrapper adds no policy.
- User also questioned `syncMyPosition` wrappers and broad exposure of `scanCurrentLocation`.
- Desired rule:
  - avoid stacked wrapper/helper layers with identical semantics.
  - expose current position sync through a clear entry point, not many direct calls to low-level scan methods.
- `GameStateUtil` should be the unified place for map comparison helpers. Remove duplicated map-confirm logic in task files such as Xiuluo V2 when found.

### Checkpoint / sleep utility cleanup

- `TaskCheckpoint` should be used directly for task stop/interruption checks.
- Do not add local wrappers like `checkpoint(...)`, `checkpointTask(...)`, `throwIfStopRequested(...)`.
- The project had many ad-hoc sleep/interrupt helpers. Direction was to consolidate into one shared utility for interruptible sleeps, except debug throwaway code can be ignored if user says so.

### Log noise reduction

- Multi-window logs were too noisy.
- Successful HWND screenshot/capture diagnostic logs were demoted from INFO to DEBUG where safe.
- Keep warnings/failures visible:
  - HWND blank/fallback
  - Robot fallback
  - capture failures
  - business-relevant failures
- `logs/dhxy-console.log` remains the main business debug log.

### Authentication / license

- User asked about authentication UI and license worker reuse.
- Direction:
  - two projects should use separate authorization codes.
  - same license worker/service may be reused if it can distinguish products/clients safely.
  - record that license server/service work remains needed; user did not want to run service yet.
- Need remind user before generating auth codes that service/worker must be started/configured.

### UI visual direction

- Mock style chosen earlier was "E" from a set of UI options.
- User liked a light Fluent-like clean style and also requested a dark mode similar to Codex dark colors, not bright blue-heavy dark UI.
- Main UI should avoid giant crowded button bars.
- Left tabs are expected.
- Right role detail panel should not permanently squeeze/obscure the table; details can be collapsible or contextual.
- Selection should be clear:
  - selected table rows need more visible color.
  - optional checkmark in the first column was discussed.
- Task count badge on task card should be small and sit on the bottom border line of the square card, similar to the mock in `docs/DHXY_FLUENT_MOCK.html`.
- Badge clicks should not shift card layout, and clicking the badge should not clear the selected task count/order.
- Holding plus/minus on count badge should repeat but at a controlled speed; too-fast repeated increments felt unusable.

### Codex local maintenance

- Installed `keep-codex-fast` skill from `https://github.com/vibeforge1111/keep-codex-fast`.
- First report-only run showed:
  - active threads: 51
  - sessions total: about 10.817 GB
  - logs: about 338.8 MB
  - thread title chars: 1,294,073
  - thread first_user_message chars: 1,296,335
  - max title/preview: 57,600 chars
  - title over limit: 28
  - preview over limit: 29
  - preview over 10k: 27
  - metadata repair candidates: 30
- The recommended safe plan was:
  - create this handoff before archiving history.
  - close Codex before applying maintenance.
  - run normal apply first.
  - optionally run `--repair-thread-metadata-bloat` after backup.
  - verify with report mode.

## Files Touched Or Investigated

Main UI:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/GameUiSettingsStore.java`
- `src/main/java/com/bot/dhxy/ui/LocalOcrSidecarService.java`
- `docs/DHXY_FLUENT_MOCK.html`

Window control / runtime:

- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/discovery/GameWindowRegistrationService.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowTaskContextHolder.java`
- `src/main/java/com/bot/dhxy/window/diagnostics/WindowInteractionMetricsService.java`

Input / focus / screenshots:

- `src/main/java/com/bot/dhxy/input/GlobalEmergencyStopHotkeyService.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java`
- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `src/main/java/com/bot/dhxy/window/interaction/WindowFocusService.java`

Tasks and services:

- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/model/TaskType.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`

OCR / vision / coordinates:

- `src/main/java/com/bot/dhxy/vision/GameTextLineOcrService.java`
- `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`
- `src/main/java/com/bot/dhxy/tools/CoordinateHelper.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`

Models / config / docs:

- `src/main/java/com/bot/dhxy/model/npc/NpcTarget.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcClickRequest.java`
- `src/main/java/com/bot/dhxy/config/BotProperties.java`
- `src/main/resources/application.properties`
- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`
- `AGENTS.md`

## Commands / Tests Already Run

Common successful validation command used repeatedly:

```powershell
mvn -q -DskipTests compile
```

Recent compile checks passed after:

- local OCR sidecar integration
- log noise reduction
- pause checkpoint in `NpcClickService`
- task entry cleanup
- map survey UI hiding
- global pause hotkey
- settings lock while busy
- stop button semantics and stop result correction

Other useful commands used:

```powershell
git branch --show-current
git status --short
Get-Content logs/dhxy-console.log -Tail 260
py -3 "C:/Users/Yunfeng Yue/.codex/skills/keep-codex-fast/scripts/keep_codex_fast.py"
```

## Known Errors / Warnings / Risks

- Worktree is very dirty and includes multiple agents' changes. Treat git status as collaborative state, not as trash.
- Some Java files may contain other agents' incomplete edits. Always compile before claiming done.
- The UI dark mode was reported as visually wrong: too blue in places, some text too bright, some text black/invisible against dark backgrounds. User wants Codex-like dark colors.
- Window dragging lag happened on the user's machine. A logout fixed it once. This may be Windows/session/GPU/input related, not necessarily DHXY.
- Stop/pause can still be delayed if an inner loop or focused input callback does not hit `TaskCheckpoint` promptly.
- Navigation/dialog OCR path can still run long. A log showed Xiuluo leader in `ACCEPT_TASK_NAVIGATE_TO_NPC` spent about 53 seconds before stop surfaced through transaction result.
- Local OCR sidecar may depend on Python/RapidOCR environment. If `python` is not in PATH, fallback to `py -3` is intended.
- Some command-line process details for Python/Java could not be read through WMI in previous checks.
- Codex local state has thread metadata bloat. This handoff exists so old session history can be safely archived/repaired.

## Open Decisions

- Whether to run keep-codex-fast normal apply and metadata repair after Codex is closed.
- Whether to create more handoffs for other active repos/sessions before archiving Codex history.
- Whether stop-all should become visually more prominent or remain next to stop-selected.
- Whether the right-side detail panel should stay inline, become collapsible drawer, or become a row-expanded detail.
- Final dark-mode palette and CSS pass.
- How much of map survey / map calibration should return to UI later.
- Final data model for learned OCR/camera/NPC observations.
- Whether Xiuluo maintenance needs stricter team-ready gating before 三技能 instead of current opportunity-based round gate.
- Whether `scanCurrentLocation` exposure should be reduced now or after navigation/OCR refactor settles.

## Constraints / User Preferences / Do Not Touch

- Use Chinese with the user unless they switch language.
- The assistant name in this session is 唐德.
- User expects direct, practical changes and frequent status updates, but does not want endless tiny UI confirmations.
- Read `AGENTS.md` and `docs/DHXY_CONTEXT.md` before significant work.
- Update `docs/ACTIVE_WORK.md` after meaningful changes because multiple agents coordinate through it.
- Prefer small targeted edits.
- Do not bypass user-validated business logic just to silence symptoms.
- Do not casually change `GameStateUtil.isMovingByPixelDiff()`.
- Do not remove useful comments.
- Do not add excessive comments everywhere. Add comments for risky/touched logic only.
- Public/high-frequency navigation/OCR/input/window/task APIs need useful JavaDoc with parameter meaning and coordinate space when relevant.
- Avoid wrapper/helper layers that only route to the same logic.
- Use `TaskCheckpoint` directly for stop/pause checkpoints.
- Move + click must be one atomic input sequence.
- Never nest input queue submissions inside an input worker callback.
- Pure screenshot/detection should avoid focus; real mouse/keyboard actions still need serialized input/focus.
- Window binding is source of truth. Prefer `WindowTaskContextHolder` / `WindowRuntimeContext` binding over global title search.
- Temp files should be window-scoped where possible.
- Do not automatically click at startup without explicit UI/debug switch.
- Do not delete or revert other agents' work.

## Next Steps

1. Compile current repo before new edits:

   ```powershell
   mvn -q -DskipTests compile
   ```

2. If the user wants Codex maintenance now, close Codex and run keep-codex-fast:

   ```powershell
   py -3 "C:/Users/Yunfeng Yue/.codex/skills/keep-codex-fast/scripts/keep_codex_fast.py" --apply --archive-older-than-days 10 --worktree-older-than-days 7
   py -3 "C:/Users/Yunfeng Yue/.codex/skills/keep-codex-fast/scripts/keep_codex_fast.py" --apply --repair-thread-metadata-bloat
   py -3 "C:/Users/Yunfeng Yue/.codex/skills/keep-codex-fast/scripts/keep_codex_fast.py"
   ```

3. Continue UI cleanup:
   - fix dark mode colors using Codex-like dark palette.
   - verify no text disappears in dark mode.
   - check main control table/detail panel layout.
   - keep `停止所选` / `停止全部` distinction visible.

4. Re-test stop/pause in real multi-window Xiuluo:
   - select one window and stop selected.
   - select all and stop selected.
   - use `停止全部`.
   - inspect `logs/dhxy-console.log` for delayed stop checkpoints.

5. Continue reducing long-running stop delays:
   - inspect navigation/dialog/OCR loops that can run tens of seconds.
   - add `TaskCheckpoint` only at meaningful boundaries.
   - do not split atomic physical input sequences.

6. Stabilize local OCR / position sync:
   - confirm local OCR sidecar starts from UI Start.
   - verify minimap/template vs local OCR vs Baidu fallback order.
   - log provider and elapsed time clearly.

7. Decide learned data schema for OCR/camera/NPC observations:
   - store enough information to shrink OCR regions later.
   - return caller-ready coordinates from coordinate services.
   - keep business services from redoing coordinate conversions.

8. Keep `docs/ACTIVE_WORK.md` updated after each meaningful change and compile pass.

