# DHXY Codex Handoff - Wubei/Xiuluo Stabilization

Date: 2026-05-31

## Repo And Branch

- Repo path: `D:\mavenProject\DHXY`
- Current branch: `dev`
- Recent HEAD: `681d3e8 Sync dialog and xiuluo maintenance work`
- Current worktree status: dirty, with many modified/untracked files from multiple agents and user-provided templates. Do not assume every diff belongs to the current agent.

## Current Goal

The active goal is to keep stabilizing DHXY multi-window task automation, with current focus moving from 修罗 to 五倍.

Immediate intent:

- Keep 修罗 usable and do not lose its hard-earned fixes.
- Bring 五倍 to a playable/testable first version.
- Reuse shared infrastructure instead of creating a separate 五倍-only input/dialog/navigation stack.
- Preserve multi-window safety: task turns, background screenshots, serialized physical input, and no mouse/keyboard crossing.

## What Is Already Completed

### Project-Wide / Framework

- Multi-window binding is the architectural source of truth.
- Input is serialized through `InputActionQueue` / `InputActionWorker` / `InputSequences`.
- Move + click must be one atomic queued sequence.
- Focus is best-effort; foreground verification failure should not hard-fail an action.
- Stop/pause checkpoints should use `TaskCheckpoint` directly.
- Window-scoped screenshot/temp paths are used in many hot paths.
- Local OCR sidecar work exists and should be preferred before Baidu OCR where already wired.
- `mvn -q -DskipTests compile` passes as of this handoff.

### 修罗

修罗 V2 has a state/phase style mainline and is largely playable, but not considered fully finished.

Completed/mostly completed:

- 接任务 via `NpcClickService.clickNpcSmart(...)`.
- Story/task-panel objective reading.
- Navigation through `NavigationService`.
- Target click through shared NPC click logic.
- Enter battle dialog handling.
- Post-combat return item handling with verification.
- Team return/dead-member waiting hooks.
- 医宝宝 / 修装备 maintenance hooks.
- Opportunistic 三技能 hook while leader is pathing.
- Several fallback discussions and partial implementations around return item, objective reread, and broad recovery.

Important 修罗 pause note:

- 修罗 was paused to start 五倍, not abandoned.
- Known remaining 修罗 polish includes fallback strictness, destination/coordinate edge cases, and occasional battle/dialog timing issues.

### 五倍

五倍 now has a first playable task class:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiPhase.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiRoundContext.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiStepOutcome.java`

五倍 currently implements:

- Start/accept NPC: `宝象国` `降魔侍卫` at `(86,87)`.
- Accept dialog template: `wubei_accept_chumoweiguo.png`.
- Task tracker anchor/panel capture.
- Yellow OCR and green-link scan from the same tracker panel screenshot.
- 暗雷怪 detection and reroll by returning to accept task.
- Single green-link route.
- Double green-link / 显形镜 route.
- Destination hint capture from the short yellow floating prompt after clicking green link.
- Known enter-battle dialogs:
  - `消灭它`
  - `证明实力`
  - `魁星归位`
- 黄袍 continuous-combat branch:
  - yellow tracker keyword `黄袍`;
  - in-combat chained marker scan;
  - after battle, tracker determines whether to continue or return.
- Return item:
  - `bag/wubei_return_item.png`;
  - verifies map becomes `宝象国`.
- Probe item:
  - `bag/wubei_probe_item.png`;
  - supports story check `开口说起话来`.
- Team return wait.
- 医宝宝 and 修装备 hooks:
  - 医宝宝 NPC: `沙拉买提`, `宝象国`, `(95,126)`;
  - 修装备 NPC: `李道宗`, `洛阳城`, `(324,109)`.
- Leader pathing maintenance hook.
- 三技能 is currently limited to one cleaner per 五倍 round.
- Broad recovery exists: clean UI and return to accept-task chain after repeated failures.

## Files Touched Or Investigated

This list includes files inspected during this handoff/session and important files already dirty in the repo.

### Main Task Files

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiPhase.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiRoundContext.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiStepOutcome.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoPhase.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

### Shared Services

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`

### Input / Window / Runtime

- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/input/action/InputAction.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java`
- `src/main/java/com/bot/dhxy/driver/WinApiMouseController.java`

### Models / Config / UI

- `src/main/java/com/bot/dhxy/config/BotProperties.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcTarget.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcClickRequest.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcTooltipType.java`
- `src/main/java/com/bot/dhxy/model/maintenance/*`
- `src/main/java/com/bot/dhxy/model/metrics/*`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/GameUiSettingsStore.java`
- `src/main/java/com/bot/dhxy/ui/LocalOcrSidecarService.java`

### Vision / OCR / Helpers

- `src/main/java/com/bot/dhxy/vision/GameTextLineOcrService.java`
- `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`
- `src/main/java/com/bot/dhxy/tools/CoordinateHelper.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`

### Templates / Docs

- `images/template/wubei/README.md`
- `images/template/wubei/*`
- `images/template/dialog/wubei/*`
- `images/template/dialog/xiuluo/*`
- `images/template/dialog/wuhuan/*`
- `images/template/dialog/maintenance/*`
- `images/template/task/wubei_*`
- `images/template/bag/wubei_probe_item.png`
- `images/template/bag/wubei_return_item.png`
- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`
- `AGENTS.md`

## Commands / Tests Already Run

Commands run in this handoff/session:

```powershell
git branch --show-current
git status --short
git log --oneline -8
Get-Content -Path docs\DHXY_CONTEXT.md -TotalCount 260
Get-ChildItem -Path docs -Force
Get-ChildItem -Path src\main\java\com\bot\dhxy\task\wubei -Force
Get-ChildItem -Path images\template\wubei -Force
rg -n "TODO|FIXME|not implemented|未|后续|fallback|WubeiPhase|fivefold|wubei|五倍|三技能|summon|repair|return item|probe|暗雷|黄袍|显行|魁星|证明实力|消灭" ...
mvn -q -DskipTests compile
```

Validation:

- `mvn -q -DskipTests compile` passed on 2026-05-31.

No full automated test suite was run. Most validation remains manual in-game testing with `logs/dhxy-console.log`.

## Known Errors / Warnings / Failing Checks

### Current Build

- No compile failure currently known.
- Compile passes with tests skipped.

### Worktree / Repo State

- Worktree is very dirty.
- Many files are modified/untracked/deleted/renamed.
- Some old dialog template files under `images/template/dialog/` are deleted because templates were reorganized into task-specific subfolders.
- `logs/` is untracked and should not be committed unless explicitly requested.
- Debug/probe Java files exist and may be untracked. Do not casually delete them; some are useful for local diagnostics.

### Runtime / Behavior Risks Still Known

- 五倍 tracker green-link click needs more real samples.
- 五倍 double-green / 显形镜 branch needs more real samples.
- 五倍 destination hint capture now works in some cases, but should not be the only correctness proof.
- 五倍 enter-battle dialog has multiple variants; only known templates are currently wired.
- 五倍 task-turn/yield behavior has improved but still needs real multi-window testing.
- Maintenance broadcast timing can still be sensitive:
  - 医宝宝/修装备 broadcast should be high priority.
  - 队员 must click within the actual broadcast window; late clicks do not count.
- 修罗 and 五倍 both have broad recovery, but not every phase has a perfect business-specific fallback.
- Coordinate/minimap behavior in cave maps such as 龙窟/凤巢/蜂巢 was historically fragile. Current policy tends to avoid risky coordinate offsets for those maps.

## Open Decisions

1. Whether 五倍 should receive the same full per-phase fallback polish as 修罗 immediately, or only after more live runs expose concrete failures.
2. Whether to update `images/template/wubei/README.md` now to match current code, or wait until the next 五倍 live run stabilizes the remaining branches.
3. Whether task maintenance config names should be generalized. Some shared maintenance intervals still use 修罗-flavored property names even when 五倍 reuses them.
4. Whether to keep all debug/probe tools in repo or move them under a clearer debug/tools package with gitignore rules for outputs.
5. How strict 五倍 run-count accounting should be:
   - simple round counter after verified return;
   - or read task panel periodically and reconcile with predicted count.
6. How much of 修罗's state-machine structure should be backported into 五环 later. User agreed to finish 修罗/五倍 patterns first.

## Constraints, User Preferences, And Do-Not-Touch Areas

### User Preferences

- Use Chinese for normal collaboration unless the user switches language.
- Be direct and practical.
- Do not invent vague explanations; use logs/screenshots/evidence.
- If diagnosing, do not start changing code without saying what will be changed.
- If the user explicitly says to implement, proceed.
- The user tests frequently and wants targeted, understandable changes.
- The user dislikes duplicate wrappers, excessive helper layers, and service sprawl.
- The user prefers clear state/phase flow over tangled nested methods.
- The user wants public/main/high-risk methods documented enough to review why branches exist.

### Do Not Touch Casually

- Do not rewrite user-validated business logic just to mask symptoms.
- Do not change `GameStateUtil.isMovingByPixelDiff()` unless logs/screenshots prove it is the real issue.
- Do not bypass the input queue for normal task actions.
- Do not split mouse move and click when they are one logical action.
- Do not add new services/helpers unless there is a real boundary.
- Do not add hard-coded OCR regions inside business services when an existing OCR/window recommendation path should provide the region.
- Do not use full-screen OCR/matching as a first resort.
- Do not let `NavigationService` know 修罗/五倍 business policy; task layers decide phase/retry/fallback.
- Do not make startup code auto-click the game without an explicit task/debug action.
- Do not delete useful comments or user-provided templates unless replacing with an agreed path.
- Do not revert unrelated dirty files.

### Architecture Principles To Preserve

- Window binding is source of truth.
- Identity sync should prefer the bound window title.
- Physical input goes through serialized input queue.
- Move + click is atomic.
- Focus is best-effort.
- Temp/debug images should be window scoped when used by task windows.
- DialogService should trend toward a single `handleDialog(...)` public entry for expected dialog handling.
- NPC clicking should trend toward one `clickNpcSmart(...)` request/result path, with learned click evidence recorded there.

## Next 3-7 Concrete Steps

1. Run one controlled 五倍 test with 1 leader + support windows and watch:
   - green-link click;
   - destination hint parse;
   - enter-battle dialog;
   - turn yield while pathing;
   - member auto battle and supply behavior.

2. If 五倍 green-link or probe path fails, use the saved task tracker debug images:
   - verify yellow OCR;
   - verify green segmentation;
   - verify single-vs-double link decision;
   - add failed screenshots to a failure corpus before changing the algorithm.

3. Validate 五倍 maintenance:
   - 医宝宝 NPC `沙拉买提` at `宝象国 (95,126)`;
   - 修装备 NPC `李道宗` at `洛阳城 (324,109)`;
   - one 三技能 cleaner per 五倍 round;
   - broadcast should preempt normal low-priority work.

4. Tighten 五倍 fallback only where logs show a concrete failure:
   - task tracker anchor miss;
   - return item not found/failed;
   - enter-battle dialog not recognized;
   - probe item branch stuck.

5. Update `images/template/wubei/README.md` after the next live run so it reflects the actual stable branch behavior rather than planned behavior.

6. Revisit 修罗 after 五倍 is playable:
   - confirm post-combat return fallback;
   - confirm team return wait;
   - confirm maintenance/broadcast timing;
   - clean stale debug paths only after behavior is stable.

7. Before committing/pushing, inspect the dirty worktree carefully and separate:
   - source changes;
   - template moves/additions;
   - config memory changes;
   - logs/debug outputs that should not be committed.

## Reactivation Prompt For Fresh Codex Chat

Paste this into a fresh Codex chat:

```text
请先阅读 AGENTS.md、docs/DHXY_CONTEXT.md、docs/ACTIVE_WORK.md，以及 docs/codex-handoffs/2026-05-31-dhxy-wubei-xiuluo-handoff.md。

我们继续 D:\mavenProject\DHXY，分支 dev。当前重点是五倍任务的实跑稳定性，不要依赖旧聊天上下文。

请先不要大改。先检查当前工作区状态、WubeiTask.java、wubei 模板 README、最近 logs/dhxy-console.log。当前五倍主链路已经接上：接任务、任务追踪、暗雷重接、单/双绿字、显形镜、进入战斗模板、黄袍连续战斗、回程、归队、医宝宝/修装备 hook、移动后放权、三技能一轮一个人。下一步主要根据日志验证和修补：

1. 五倍移动中是否正确放权；
2. 队员是否能补蓝/自动战斗/归队；
3. 双绿字/显形镜分支是否稳定；
4. 进入战斗弹窗是否还有新模板；
5. 任务追踪绿字点击是否仍会点偏；
6. 如果失败，先保存失败截图/日志证据，再做小改。

约束：
- 不要随便重写已验证逻辑；
- 正常鼠标键盘动作必须走 InputSequences/InputActionQueue；
- move+click 必须原子；
- 不要新增没必要的 service/helper/wrapper；
- 不要把业务策略塞进 NavigationService；
- 不要改 GameStateUtil.isMovingByPixelDiff()，除非日志和截图证明它就是根因；
- 用中文和我沟通。

先给我一个当前状态摘要和你准备检查的文件/日志，然后等我说继续再改代码。
```
