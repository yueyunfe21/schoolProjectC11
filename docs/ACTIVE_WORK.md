# DHXY Active Work

### Tang De - 2026-05-26 NPC target model seed

Status: implemented / compile passed

Goal:

- Add a canonical NPC/monster model so task code can describe "what target is this" instead of spreading name, map, coordinate, purpose, and fixed/roaming flags across call sites.

Changed files:

- `src/main/java/com/bot/dhxy/model/npc/NpcTarget.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcRole.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcMovementType.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `NpcTarget` with map name, logical X/Y coordinate, primary name, aliases, role, movement type, formula tune offsets, expected dialog template, key, and source.
- Added `NpcRole` for task giver, combat target, interaction target, and debug target.
- Added `NpcMovementType` for fixed, roaming, floating, and unknown targets.
- Added `NpcTarget.toClickRequest(PlayerCharacter)` so the model can feed the current `NpcClickRequest` pipeline without forcing a big refactor now.
- Boundary decision: do not pass the full `NpcTarget` into `NavigationService`. Navigation should keep using its narrow request/coordinate inputs because it only needs map and logical coordinates, not NPC role, aliases, OCR template, or click tuning.
- Cleanup after boundary review:
  - Removed `NpcNavigationRequest.fromTarget(NpcTarget)`.
  - Removed large static `NpcTarget` builder constants from task constant sections.
  - Navigation call sites now build `NpcNavigationRequest` from narrow map/coordinate/name fields.
- Migrated first examples:
  - 五环 accept NPC now has `NpcTarget ACCEPT_NPC` and uses it for debug click, navigation coordinates, logs, and smart-click request creation.
  - 修罗 accept NPC now has `NpcTarget ACCEPT_NPC` and uses it for navigation and smart-click request creation.
  - 修罗 combat objective now builds a per-objective `NpcTarget` with role `COMBAT_TARGET` and movement type `ROAMING` before entering the smart-click pipeline.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-26 approach coordinate boundary

Status: implemented / compile passed

Decision:

- `NavigationService` only navigates to the logical coordinate it is given. It should not know whether that coordinate came from an NPC, 修罗怪, or another task target.
- Task flows that need to stand near a target should first call `CoordinateHelper.calculateApproachCoordinate(mapName, targetX, targetY)`.
- The returned coordinate is still a logical in-game map coordinate and is then passed to `NavigationService.navigateInCurrentMap(...)`.
- 修罗 now derives its approach coordinate through `CoordinateHelper` before current-map navigation; the benchmark probe uses the same helper.

Changed files:

- `src/main/java/com/bot/dhxy/tools/CoordinateHelper.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/debug/XiuluoAcceptBenchmarkMain.java`

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-26 Java/Spring/Lombok/logging SOP

Status: decided

Rule:

- Use Spring Boot beans and constructor injection for real services/collaborators. Do not manually `new` service dependencies in task/business code.
- Put shared request/result/value objects in a proper model package, not under service implementation packages.
- For immutable request/result/value objects, use the existing Lombok pattern: `@Value` + `@Builder`, with `@Builder.Default` for defaults. Static factories should call `builder()` and then `build()`.
- Use enums for operation/status/policy values that cross service/task boundaries.
- Use SLF4J logging for normal app code. Avoid `System.out.println` outside temporary local debug tools.
- Logs for automation-sensitive paths should include source task, window context when available, target map/NPC/coordinate, result status, and timing where useful.

### He Li - 2026-05-26 Java file layout SOP

Status: decided

Rule:

- Keep public classes, public APIs, and the main workflow near the top of a Java file.
- Put private nested helper types (`private class`, `private record`, `private enum`, private interfaces) at the bottom of the enclosing class/file, after the main public and private workflow methods.
- Do not insert private helper types in the middle of a business flow unless Java syntax requires it; this keeps task code and service entry points easier to review.

### He Li - 2026-05-26 latency log seed

Status: implemented / compile passed

Goal:

- Add lightweight timing logs to high-frequency automation boundaries so later UI/dashboard work can graph latency without parsing ad-hoc business messages.

Decision:

- Use one stable log marker: `[latency] event=<name> elapsedMs=<ms> detail=<key-values>`.
- Instrument boundary methods, not every helper loop, to avoid log spam.
- Keep OCR-specific timings already present in OCR/vision services; add timing around orchestration layers that combine input, navigation, dialog matching, and task-turn ownership.

Changed files:

- `src/main/java/com/bot/dhxy/tools/LatencyMetrics.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/task/transaction/TaskTransactionRunner.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`

Events now emitted:

- `input.request`
- `task.transaction`
- `npc.click.smart`
- `navigation.mapCoordinate`
- `navigation.toMap`
- `navigation.currentMap`
- `dialog.detect`
- `dialog.greenTemplateClick`
- `dialog.greenTemplateFirst`
- `bag.itemAction`
- `player.sheyaoxiang.ensure`
- `player.position.sync`
- `location.scanCurrent`

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Xiuluo map confirm wrapper cleanup

Status: completed

Goal:

- Remove thin Xiuluo task wrappers around current-map confirmation so map checks call `GameStateUtil` directly.

Changed files:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Removed `XiuluoTask.isAlreadyInTargetMap(...)`.
- Inlined its only call site in `runObjectiveReadyFlow(...)` with a direct `gameStateUtil.confirmCurrentMap(...)` call.
- Kept Xiuluo-specific logs at the call site so the formal pathing precheck remains readable without another wrapper method.
- Re-scanned map confirmation usages: remaining normal flow calls go directly through `GameStateUtil.confirmCurrentMap(...)` or `confirmCurrentMapFresh(...)`.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC region resolution moved to memory service

Status: completed

Goal:

- Move current-window coordinate conversion out of `NpcClickService` so consumers receive already resolved NPC click regions.

Changed files:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/debug/XiuluoCtrlClickDebugMain.java`
- `src/main/java/com/bot/dhxy/debug/NpcTextCandidateGameWindowDebugMain.java`
- `docs/ACTIVE_WORK.md`

Done:

- `OcrRoiMemoryService.recommendNpcClickRegions(...)` now returns `ResolvedNpcClickRegion`, which includes:
  - persisted window-relative region;
  - current window base;
  - screen-absolute rectangle.
- The conversion uses the current bound `WindowRuntimeContext` native binding when present, and falls back to `GameClientTracker` only for standalone/debug paths.
- Added `recommendNpcClickWindowRegions(...)` for debug tools that still need raw window-relative regions.
- Removed the temporary `NpcClickService.NpcScanRegion`; `NpcClickService` now consumes the resolved region from the recommendation service directly.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC scan region coordinate boundary

Status: completed

Goal:

- Stop each NPC click strategy from manually converting recommended window-relative regions to screen-absolute rectangles.

Changed files:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `NpcScanRegion`, a resolved scan-region record that keeps both:
  - `windowRegion`: 1024x768 game-window-relative region used for OCR memory and evidence.
  - `screenRect`: screen-absolute rectangle used for screenshot/template capture.
- `resolveNpcScanRegions(...)` now converts recommended regions once using the current bound window base.
- Tooltip template, yellow-name OCR, and purple player-anchor formula now receive resolved regions instead of recalculating `base + x/y` independently.
- `captureCleanNameRegionToMemory(...)` now captures with the resolved screen-absolute rectangle directly.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC visual work region semantics

Status: completed

Goal:

- Treat learned NPC click regions as visual work areas that can support both yellow target-name OCR and purple player-anchor formula OCR.

Changed files:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Updated the recommendation JavaDoc to describe visual work regions instead of tight OCR-only boxes.
- Replaced shrinking success-count-based ROI sizing with a fixed work-region sizing policy:
  - padding: `240 x 190`
  - minimum size: `520 x 360`
- Both policy-derived regions and click-sample-derived regions now use the same `npcVisionWorkRegion(...)` helper.
- This keeps the learned region broad enough to include the target yellow name and the current player's purple name after navigation moves the character near the target.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC OCR region recommendation cap

Status: completed

Goal:

- Keep NPC OCR region recommendations small enough to avoid repeated screenshot/OCR scans.

Changed files:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Fixed targets now return at most one learned/recommended OCR region plus the default masked full-window fallback.
- Roaming targets now return at most two learned/recommended OCR regions plus the default masked full-window fallback.
- The recommendation collector can still consider policy, sample, and legacy sources, but the returned list is capped before default is appended.
- Logs now include `learnedCandidates` and `maxLearned` so it is visible when many historical candidates were trimmed.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC OCR mask path cleanup

Status: completed

Goal:

- Make yellow NPC-name OCR and purple player-anchor OCR use the same default full-window mask rule.

Changed files:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Replaced the yellow-only `prepareYellowTargetScanImage(...)` helper with shared `prepareNpcOcrScanImage(...)`.
- Yellow target OCR and purple player-anchor OCR now both capture to `BufferedImage` first and use the same default-region mask decision.
- Purple player-anchor still writes the prepared image to a temp file before washing because `ImagePreprocessor.washPurpleTextToBlackAndWhite(...)` currently accepts file paths.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Position scan gateway cleanup

Status: completed

Goal:

- Reduce normal business use of `LocationVisionService.scanCurrentLocation()` so current-position reads go through the player state sync gateway.

Changed files:

- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `docs/ACTIVE_WORK.md`

Done:

- Changed `PlayerStateService.syncMyPosition()` into the central business entry for no-input current-position scans; it now returns the latest recognized location so no extra wrapper method is needed.
- Updated map navigation arrival checks, NPC first-shot debug, NPC player-anchor formula, and `GameStateUtil.confirmCurrentMap(...)` to use `syncMyPosition()`.
- Normal service/task code no longer directly calls `LocationVisionService.scanCurrentLocation()` outside `PlayerStateService`.
- Remaining direct calls are limited to debug/calibration helpers:
  - `debug/XiuluoAcceptBenchmarkMain`
  - `tools/AutoGridCalibrator`
  - `vision/PlayerNameOcrDebugService`

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Pause/stop movement detection checkpoint

Status: completed

Goal:

- Diagnose why pause/stop can feel slow after sleep consolidation.
- Fix the concrete slow checkpoint found in the latest log.

Changed files:

- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `docs/ACTIVE_WORK.md`

Findings:

- Latest log showed the UI command was pause, not stop.
- Four windows reached `TaskPauseToken` checkpoint quickly, but one Xiuluo window continued inside `GameStateUtil` movement detection for about six seconds before pausing.
- The slow path was the movement detector's coordinate sampling plus pixel fallback. It only checked thread interruption, not the current task pause/stop token.

Done:

- `GameStateUtil` now reads the current task context through `TaskExecutionContextHolder`.
- Coordinate movement detection and pixel fallback loops now call a shared movement checkpoint before/after waits and captures.
- Pause requests can now be observed inside movement detection instead of waiting for the whole detector to finish.
- Thread interruption inside movement detection now becomes a `TaskStopRequestedException`, so stop exits through the normal STOPPED task path.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Task sleep utility consolidation

Status: completed

Goal:

- Stop duplicating small `Thread.sleep` / interrupt handling helpers in every task/service file.
- Give task waits one shared interrupt policy so stop/pause responsiveness is easier to audit.

Changed files:

- `src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java`
- `src/main/java/com/bot/dhxy/task/template/BaseTaskTemplate.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/GiveItemService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/service/TeamReturnService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/vision/MapSurveyService.java`

Done:

- Added `TaskSleep` as the shared task/service sleep helper.
- `TaskSleep.sleep(...)` returns false on interruption and always restores the interrupted flag.
- `TaskSleep.sleepOrStop(...)` checks the task context before and after sleeping, and throws `TaskStopRequestedException` when interrupted.
- Replaced duplicate local sleep helpers in the main task/navigation/dialog/NPC/item/team-return/vision flows.
- Follow-up scan tightened the rule: non-debug Java code now uses `TaskSleep` instead of local `Thread.sleep` helpers, including driver, tracker, input worker, focus, team role detection, movement detection, and standalone tool classes that compile with the main source set.
- Explicit `Debug*` classes and `debug` package experiments are left alone per user direction because they are temporary and may be deleted later.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-25 NPC smart-click learning record boundary

Status: implemented in `NpcClickService`; cleanup rule remains active

Decision:

- `NpcClickService.clickNpcSmart(...)` is the single production entry for clicking NPCs and task targets.
- Vision-memory learning for NPC/monster click behavior must be centralized behind this entry.
- Task code should only provide target facts through `NpcClickRequest`: map name, logical target coordinate, target name/keyword, roaming/fixed flag, and expected verification template.
- Internal strategies may differ, but they are implementation details of `clickNpcSmart(...)`:
  - task-tooltip template direct click;
  - learned direct click point;
  - yellow target-name OCR;
  - player-anchor coordinate formula;
  - Ctrl nearby-menu fallback.
- A strategy should return structured evidence to the `clickNpcSmart(...)` coordinator instead of writing learning data independently.
- The coordinator should be the only place that decides how to record:
  - successful click point samples;
  - failed or weak samples;
  - scan region used;
  - matched text/template rectangle;
  - actual clicked point;
  - verification strength and outcome.

Rationale:

- The project is converging on one click path for both NPC interaction and fixed/roaming monster interaction.
- If learning writes are scattered across yellow OCR, tooltip matching, formula clicks, and Ctrl probing, the JSON history becomes hard to trust and hard to debug.
- A single input point plus a single learning output point makes later ROI shrinking and direct-click learning inspectable.
- Template-based hits are valid learning evidence even when no OCR text was used, but only after the click verifies the expected dialog or battle state.

Implementation intent:

- Introduce or evolve an internal result shape similar to `NpcClickStrategyResult`.
- Each strategy should report, at minimum:
  - strategy/source name;
  - status;
  - window-relative scan region, if any;
  - matched rectangle, if any;
  - screen-absolute and window-relative reusable direct-click point, if any;
  - verification result;
  - diagnostic message.
- `clickNpcSmart(...)` should call a central recorder, for example `recordSmartClickEvidence(request, result)`.
- Verified results should write strong evidence:
  - `recordNpcClickAttempt(...)` for learned direct click points;
  - ROI policy/sample evidence when the strategy had a meaningful scan region and matched rectangle or click point.
- Unverified candidates may be recorded as weak/failure diagnostics, but must not become recommended click points.
- Existing older direct calls to `recordNpcClickAttempt(...)` or `recordNpcTargetOcrObservation(...)` from inside individual smart-click strategies should be treated as migration targets. They should either be removed or routed through the central `clickNpcSmart(...)` recording method.

Reusable click-point semantics:

- The stored click point means "a point that should be safe to left-click directly on the NPC/monster in a future run".
- It does not always equal the exact UI point physically clicked by the current strategy.
- Task-tooltip template path:
  - physically clicks the matched tooltip center to open the dialog;
  - records the reusable direct-click point as `tooltipCenter.x, tooltipCenter.y + 90`.
- Yellow-name OCR path:
  - records the same final direct left-click point used by the strategy.
- Player-anchor formula path:
  - records the same final formula direct left-click point used by the strategy.
- Ctrl-menu fallback:
  - physically clicks the yellow Ctrl-menu text candidate after the menu opens;
  - records the original Ctrl hover/probe point that caused the nearby menu to appear;
  - must not record the yellow menu text click point as the reusable NPC/monster point.

Boundary:

- This rule applies to NPC/monster click learning only.
- Other OCR diagnostics, map-label recognition, task-panel parsing, mini-map coordinate reading, and debug-only probes may keep their own records if they are not trying to learn NPC/monster click points.
- Debug mains may log or record temporary diagnostics, but they must not become a second production learning path.

Next owner guidance:

- Do not redesign `config/vision_memory.json` for this cleanup.
- Preserve the current JSON streams and append fields compatibly if needed.
- First clean up `NpcClickService` so all smart-click strategy evidence flows through one coordinator method.
- After that, update individual strategies one by one without changing their click order.

Current code status:

- `NpcClickService` now contains the first concrete internal structure:
  - `NpcClickStrategySource`;
  - `NpcClickStrategyStatus`;
  - `NpcClickStrategyResult`;
  - `recordSmartClickEvidence(request, result)`.
- The production `clickNpcSmart(...)` strategy pipeline now records through this single boundary:
  - task-tooltip template direct click;
  - learned direct click point;
  - yellow target-name OCR;
  - player-anchor coordinate formula;
  - Ctrl nearby-menu fallback.
- The old yellow/formula/learned/Ctrl strategy-local writes have been removed from the production smart-click path.
- Debug-only first-shot tooling may still write its own diagnostics; that is outside this production learning boundary.

Xieshuai/Solart/Humble review - smart-click recorder gates:

- Review status: implemented by He Li / compile passed.
- Risk confirmed:
  - `recordSmartClickEvidence(...)` must not write every non-skipped strategy result into `npcClickSamples`.
  - `OcrRoiMemoryService.recommendedNpcClickPoint(...)` rejects a learned direct-click point when the latest sample for the same target is not `clicked && success`.
  - Therefore a `NOT_FOUND` or pure `FAILED` sample can suppress an older good learned click point. In the current strategy order this can even happen inside one `clickNpcSmart(...)` call if an early tooltip miss is recorded before the learned-memory strategy runs.
- `npcClickSamples` must mean "real direct-click attempt result", not "any strategy result":
  - record `VERIFIED` when `clicked=true` and a reusable direct-click point exists;
  - record `CLICK_NOT_VERIFIED` only when `clicked=true` and a reusable direct-click point exists, because this is a real negative click sample;
  - do not record `NOT_FOUND`, pure `FAILED`, interrupted, screenshot-failed, OCR-miss, or Ctrl-scan-exhausted results into `npcClickSamples`.
- Suggested click-sample gate:

```java
boolean shouldRecordClickSample =
        result.clicked()
                && result.clickPointAbs() != null
                && result.clickPointRel() != null;
```

- ROI evidence must not be polluted by Ctrl-menu text:
  - `CTRL_MENU` can write a verified click sample using the original Ctrl probe/hover point as the reusable point;
  - `CTRL_MENU` must not call `recordNpcTargetOcrObservation(...)`, because its matched rectangle belongs to the Ctrl popup menu, not the in-scene NPC/monster yellow name.
- Suggested ROI-evidence gate:

```java
boolean shouldRecordRoiEvidence =
        result.source() != NpcClickStrategySource.CTRL_MENU
                && result.scanRegion() != null
                && (result.matchedRect() != null
                    || result.clickPointRel() != null
                    || result.source() == NpcClickStrategySource.YELLOW_TARGET_OCR);
```

- `YELLOW_TARGET_OCR` misses may still be recorded as ROI/target observations with `matched=false, verified=false` so repeated misses can mark the policy stale; they must not become direct-click samples.
- `TASK_TOOLTIP_TEMPLATE` verified results may provide visual cue evidence, but tooltip-not-found should normally remain a log-only miss and must not suppress learned direct-click points.
- Strong verification compatibility:
  - The migrated central recorder writes `verificationStrength="DIALOG_TEMPLATE"` for verified smart-click results.
  - `OcrRoiMemoryService.hasStrongNpcVerification(...)` historically recognized `DIALOG_OPTION` or `actualClickMeasured=true`.
  - Next owner should either make `hasStrongNpcVerification(...)` recognize `DIALOG_TEMPLATE`, or have the recorder write the legacy `DIALOG_OPTION`. Prefer recognizing `DIALOG_TEMPLATE` because it preserves the new semantics.

He Li implementation note:

- `recordSmartClickEvidence(...)` now writes `npcClickSamples` only when the strategy actually clicked and provides both screen-absolute and window-relative reusable click points.
- `NOT_FOUND`, pure `FAILED`, interrupted, screenshot/OCR/template misses, and exhausted Ctrl scans no longer write direct-click samples, so they cannot suppress an older good learned point through the latest-sample gate.
- ROI evidence now skips `CTRL_MENU`; Ctrl popup text rectangles are not fed into scene-level NPC/monster ROI learning.
- Yellow OCR misses may still write ROI/target observations, allowing repeated misses to stale the ROI policy without becoming learned direct-click samples.
- `OcrRoiMemoryService.hasStrongNpcVerification(...)` now treats `DIALOG_TEMPLATE` as strong verification alongside the legacy `DIALOG_OPTION`.

### Xieshuai - 2026-05-25 vision memory JSON schema decision

Status: decided

Decision:

- Keep the current `config/vision_memory.json` structure for now. Do not split or migrate it just to separate OCR ROI policy from raw observations.
- Reason: the file is not only for shrinking OCR regions. It is also the shared historical vision memory for:
  - OCR attempts and matched text rectangles;
  - player-name anchor samples;
  - NPC/monster target coordinates;
  - actual/predicted mouse click points;
  - camera/scale-related context;
  - verification outcomes that later decide whether a point can be trusted.
- Future learning should derive policy from the existing sample streams instead of discarding or reshaping them prematurely.
- If a future derived model becomes large or difficult to inspect, add a separate derived-policy file or section while preserving the existing raw sample schema and data.

Rule for agents:

- Do not propose a JSON schema migration for `vision_memory.json` unless the user explicitly reopens this decision.
- Add fields compatibly when needed, but preserve existing top-level streams and historical samples.

### He Li - 2026-05-25 Yellow target candidate contract

Status: implemented by Xie Shuai

Context:

- Xiuluo/NPC smart click should keep useful visual evidence even when exact yellow-name OCR does not match the target name.
- The next Ctrl-menu fallback should probe around high-confidence physical candidate points instead of only probing the window center.
- Another agent may implement the yellow-text candidate extraction; this section defines the expected return shape.
- The first production implementation now lives in `GameTextLineOcrService` and is consumed by `NpcClickService.clickNpcSmart(...)`.

Current implementation:

- Yellow washing/candidate extraction:
  - Service: `GameTextLineOcrService`.
  - API: `findYellowTextCandidateResult(BufferedImage raw, Path washedPath, Path overlayPath)`.
  - Convenience API: `findYellowTextCandidates(BufferedImage raw, Path washedPath, Path overlayPath)`.
  - Return: `TextCandidateScanResult`, whose `candidates()` list is immutable and sorted by score descending.
  - Candidate coordinates are image-local to the supplied screenshot or cropped scan image.
- Formal NPC click integration:
  - Service: `NpcClickService.clickNpcSmart(...)`.
  - Exact target path still tries `GameTextLineOcrService.findYellowTarget(...)` first.
  - If exact target OCR does not match, `NpcClickService` calls `findYellowTextCandidateResult(...)`, converts ranked candidates to screen-absolute points, and appends them to the Ctrl-menu probe origins.
  - These fallback candidates are not left-clicked directly; they are only used as Ctrl probe origins.
- Current yellow mask behavior:
  - Keeps sampled NPC yellow strokes including dark edge pixels such as `94,94,18`, `109,109,16`, `126,126,14` and bright pixels such as `213,213,5`, `253,253,50`, `251,253,77`, `248,250,158`.
  - Rejects the stall/vendor gold family around `203,181,88..106` with the characteristic red-green separation.
  - Penalizes high/skinny fragments, tiny fragments, and weak sparse blobs so non-text crumbs do not rank above real NPC-name text.

Recommended API shape:

- Do not return `Queue` or `Stack`.
- Return a result object that owns an already sorted immutable `List`.
- The list represents scored visual candidates, not a mutable work queue.

Suggested records:

```java
public record YellowTextCandidate(
        Point textCenterAbs,
        Point clickPointAbs,
        OcrWindowRegion textRectAbs,
        double score,
        String sourceText,
        String reason
) {}

public record YellowTargetScanResult(
        YellowTargetMatchStatus status,
        Point matchedClickPointAbs,
        List<YellowTextCandidate> fallbackCandidates
) {
    public List<Point> fallbackClickPoints() {
        return fallbackCandidates.stream()
                .map(YellowTextCandidate::clickPointAbs)
                .toList();
    }
}

public enum YellowTargetMatchStatus {
    TARGET_MATCHED,
    TARGET_NOT_FOUND_WITH_CANDIDATES,
    TARGET_NOT_FOUND,
    SCAN_FAILED
}
```

Contract:

- `fallbackCandidates` must be sorted by `score` descending before returning.
- Limit fallback candidates to the best 2 by default; best 3 is acceptable if diagnostics show it helps.
- `clickPointAbs` must be the actual point the yellow-target strategy would click after applying its vertical/target offset, not merely the yellow text center.
- The extractor should reject blobs that do not look like text. Use shape/quality filters such as minimum pixel count, width/height bounds, aspect ratio, connected-component sanity, and line-like text structure.
- Yellow background, skin, effects, or large decorative blobs must not become candidates.
- `TARGET_MATCHED`: exact/fuzzy target name matched; caller should try `matchedClickPointAbs` first and may also add it to Ctrl probe origins if the click does not verify.
- `TARGET_NOT_FOUND_WITH_CANDIDATES`: no target-name match, but text-like yellow candidates exist; caller should not left-click them blindly, only add their `clickPointAbs` values to Ctrl probe origins.
- `TARGET_NOT_FOUND`: scan succeeded but no usable target or fallback candidate exists.
- `SCAN_FAILED`: screenshot/OCR/washing failed or the result is untrustworthy; caller should not add fallback candidates from this scan.

Integration intent:

- `NpcClickService` should collect Ctrl probe origins from prior evidence:
  - yellow exact matched click point when it fails verification;
  - yellow fallback candidate click points;
  - player-name formula point;
  - purple-blob formula fallback point;
  - learned/previous attempted points if available;
  - window center only as the final fallback.
- Ctrl probing should iterate this de-duplicated ordered point list, then apply `DENSE_BLIND_OFFSETS` around each origin.

### Tangde - 2026-05-25 settings page game-config cleanup

Status: implemented

Changed:

- Removed the duplicate Window Registration block from the JavaFX Settings tab; window scan/register remains owned by the Main tab.
- Settings now focuses on in-game configuration:
  - editable task run-count fields for 修罗、五倍、天庭、抓鬼, with 五环 constrained to a 1/2 dropdown;
  - summon third-skill maintenance enable switch and minute interval dropdown;
  - existing supply thresholds.
- Added shared `BotProperties` fields and default `application.properties` entries for the new game task count settings, so future task implementations can consume one central config object.
- Changed 主控 role detail from an overlay to a real right-side layout panel, so opening details no longer covers table columns/text.
- Restored the top bar as a global root-level header so all tabs keep the same structure; removed the shell's forced 640px min-height so the main content does not overflow upward over the header in small/short windows.
- Retuned dark mode toward a Codex/GitHub-like black-gray palette and added explicit dark overrides for buttons, text fields, combo boxes, tables, lists, and task tiles to avoid black-on-black or overly bright blue areas.

### Xieshuai - 2026-05-25 Xiuluo Alt+1 Maven/IntelliJ benchmark rerun

Status: diagnostic / verified

Changed:

- Added IntelliJ Application run configs for the packaged benchmark main:
  - `XiuluoAcceptBenchmarkMain - WindowMessageAlt1`
  - `XiuluoAcceptBenchmarkMain - MiniMapProbe`
- Both configs run `com.bot.dhxy.debug.XiuluoAcceptBenchmarkMain` with project Make enabled, `$PROJECT_DIR$`
  as the working directory, and UTF-8 JVM output flags.

Rerun findings:

- Maven compile/classpath preparation passed with:
  `mvn -q -DskipTests compile dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"`.
- Running the same main class as IntelliJ would launch, with
  `-Dxiuluo.benchmark.onlyWindowMessageAlt1=true`, selected the correct bound window.
- `-Mode windowMessageAlt1` selected the correct bound window `hwnd-1E0DEC` / handle `1969644`.
- The JavaFX experiment service path still reports `posted=false` for `Alt+1` when launched from the current non-elevated process.
- This confirms the current blocker is process integrity/permission, not a dead runner or broken packaged main.
- If IntelliJ is launched as administrator, use the new IntelliJ configs directly to compare elevated vs non-elevated behavior.

### He Li - 2026-05-25 no-UI background input benchmark alignment

Status: diagnostic / current finding

Goal:

- Make the no-UI Xiuluo accept benchmark follow the same window/input shape documented by the previous agents.
- Verify whether current `Alt+1` background keyboard failure is a benchmark mistake or a real Win32 message failure.

Changed:

- `tools/XiuluoAcceptBenchmarkRunner.java` now splits the minimap probe into:
  - pure `Alt+1 + sleep` queue requests, so `InputActionWorker` can use the formal HWND-background keyboard path;
  - one focused real mouse `moveMouse + clickLeft` sequence for the minimap coordinate click.
- The benchmark also has `-Dxiuluo.benchmark.onlyWindowMessageAlt1=true`, which directly calls the same `WindowMessageInputExperimentService.postAlt1(...)` used by the JavaFX `后台按键 Alt+1` button.
- `BoundWindowKeyboardService` now logs per-message `PostMessage` results and `Native.getLastError()` for failed HWND shortcuts.

Current finding:

- The selected game window was registered and bound correctly: `windowId=hwnd-1E0DEC`, `hwnd=1969644`.
- HWND screenshots still work through `HWND_BITBLT`.
- The JavaFX-experiment service path itself currently reports `posted=false` for `Alt+1`.
- The formal `BoundWindowKeyboardService` also fails all four `WM_SYSKEY*` messages with `lastError=5` (`ERROR_ACCESS_DENIED`).
- Therefore the current failure is not caused by the benchmark skipping the existing experiment path. It is a real Win32 message permission/integrity issue for the current process/window state.
- The input worker fallback still focuses the bound window and sends the real `Alt+1`, so the task can continue, but it will not be background-only in this state.
- Process token check confirmed the mismatch:
  - game process `xy2_tab_x64 pid=10500`: elevated/high integrity (`S-1-16-12288`);
  - normal PowerShell/Java process: medium integrity (`S-1-16-8192`).
- Running the same no-UI probe elevated confirms the old conclusion still holds when integrity levels match:
  - `WindowMessageInputExperimentService.postAlt1(...)` reports `posted=true`;
  - formal `BoundWindowKeyboardService` reports `Alt+1 result=true`, all four `PostMessage` calls have `lastError=0`;
  - no fallback focus was needed for pure Alt+1 requests.

Helper:

- `tools/RunXiuluoWindowMessageAlt1Probe.ps1`
  - `-Mode windowMessageAlt1`: run the JavaFX-experiment service path without the JavaFX UI.
  - `-Mode miniMapProbe`: run the formal input queue path where pure Alt+1 uses background HWND keyboard and minimap click uses focused real mouse input.
  - Intended to be launched with `Start-Process -Verb RunAs` when comparing against an elevated game client.

Next diagnostic direction:

- If the game client is elevated and we want background keyboard, run the Java/IDE/Codex process elevated too.
- If the Java process remains medium while the game is high, expect HWND keyboard to fail with error 5 and fall back to focused real input.

### Tangde - 2026-05-24 matcher internal comments

Status: implemented

Changed:

- Added internal section comments to matcher/recognition logic so reviewers can follow each stage without reverse-engineering the loops.
- Covered mini-map coordinate and map-label matching in `MiniMapCoordinateReader`.
- Covered objective map-name and coordinate matching in `ObjectiveTextRecognitionService`.
- Covered shared image/template matching internals in `ImageFinder`.
- Covered map survey map-label matching handoff in `MapSurveyService`.

Scope:

- Comments only. No business logic, thresholds, provider order, OCR behavior, or input behavior changed.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 agent code documentation rule

Status: documented

Changed:

- Added a mandatory code documentation rule to `AGENTS.md`.
- Future code changes must include production-grade comments/JavaDoc for new or modified public APIs, complex private helpers, business decisions, fallback chains, threading/input behavior, native-window handling, OCR/template matching, debug paths, configuration switches, and persisted data formats.
- Method JavaDoc must document what the method does, every parameter, return/failure semantics, side effects, and safety assumptions. Long methods must also use internal block comments to explain each meaningful stage.
- Comments should explain intent, assumptions, edge cases, invariants, and safety constraints. Low-value comments that merely restate code are not acceptable.
- Agents touching undocumented code should add comments for the touched logic instead of leaving it undocumented.

### He Li - 2026-05-24 unified NPC Ctrl-menu click contract

Status: implemented

Changed:

- Replaced the separate Xiuluo Ctrl-click paths with one `NpcClickService.clickNpcByCtrlMenuScan(targetKeyword, npcTagTemplatePath, expectedDialogTemplatePath)` entry.
- The unified Ctrl path tries `(NPC)` template candidates first, then falls back to OCR keyword matching.
- `NpcClickService` only clicks the NPC/menu candidate and verifies that the expected option-dialog template is visible. It no longer clicks the task option itself.
- Xiuluo now passes `修罗`, `images/template/npc/npc_tag.png`, and `images/template/dialog/xiuluo_enter_battle_kanda.png`.
- Wuhuan `clickNpcSmart(...)` now receives `images/template/dialog/wuhuan_accept_first_option.png` as the expected accept-dialog template.
- Removed the public old split methods `clickNpcByCtrlMenuKeyword(...)` and `clickNpcByCtrlMenuNpcTagCandidates(...)`.
- Added `DialogService.isGreenTemplateOptionVisibleDirectForExclusive(...)` so NPC-click code can verify a business dialog without clicking its option.

Behavior:

- Dialog option clicking remains owned by the task/DialogService flow.
- Wuhuan still clicks the accept option in its accept transaction.
- Xiuluo still clicks `看打!` through `tryConfirmEnterBattleDialog(...)`.
- The Ctrl-menu service is now a click-and-verify helper, not a task-progress helper.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 sync position provider cascade

Status: implemented

Goal:

- Make `syncMyPosition` prefer the fastest local source before falling back to OCR/cloud.
- Keep Baidu OCR as the last fallback only, not the default first choice.

Changed:

- `LocationVisionService.scanCurrentLocation()` now resolves location in this order:
  1. `MINIMAP_TEMPLATE`: read mini-map coordinate digits with local templates, then recognize the cleaned map-label image against `images/template/map_label`.
  2. `LOCAL_OCR`: if template location fails, capture the coordinate strip and parse it with the local OCR sidecar only.
  3. `BAIDU_OCR`: if local OCR cannot produce a valid map/coordinate, call Baidu OCR as the final fallback.
- `MiniMapCoordinateReader` now exposes `readCurrentTemplateLocation()`, returning map name, coordinate, template score, and the saved clean label debug image path.
- No new matching service was added. The existing mini-map map-label template logic is now exposed as `recognizeMapLabelImage(...)`, and `MapSurveyService` reuses it instead of keeping a duplicate private matcher.
- `TextRecognizer` now exposes `parseLocationLocalOnly(...)` and `parseLocationBaiduOnly(...)` so location fallback order is explicit and not hidden inside provider config.

Runtime logs to check:

- `[location] selected provider=MINIMAP_TEMPLATE ... templateElapsedMs=...`
- `[location] selected provider=LOCAL_OCR ... localElapsedMs=...`
- `[location] selected provider=BAIDU_OCR ... baiduElapsedMs=...`
- `[ocr-location] provider=local-only ...`
- `[ocr-location] provider=baidu-only ...`

Expected behavior:

- Template/minimap should usually be fastest because it is local image/template matching and does not call OCR/network.
- If map-label templates are missing or score is too low, the chain falls through automatically; it should not block `syncMyPosition`.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed.

### Tangde - 2026-05-24 vision memory auto data capture

Status: implemented

Goal:

- Store the data needed for the two-step vision plan without requiring manual bookkeeping:
  1. shrink OCR scan regions from full masked window toward learned ROI/edge/center regions;
  2. later learn enough NPC click samples to click known NPC coordinates without OCR.

Changed:

- `OcrRoiMemoryService` now stores three sample streams in `config/vision_memory.json`:
  - `ocrAttempts`: every keyed masked-window/ROI OCR attempt, including region type, scan region, target text, matched text rectangle, word count, success/failure, and message.
  - `playerAnchorSamples`: existing player-name anchor samples with map coordinate, anchor, center delta, and camera state.
  - `npcClickSamples`: NPC first-shot prediction/click samples, including current map coordinate, target NPC/name/coordinate, player anchor, predicted/actual click point relative to the game window, tune values, formula version, click outcome, and verification signal.
- `OcrWindowScanService` now records both learned-ROI attempts and full-masked-window fallback attempts.
- `NpcClickService.clickNpcSmart(...)` now records the first-shot prediction result after the move+click verification, and also records skipped first shots when current location or player anchor is missing.
- `NpcClickService.clickNpcSmart(...)` no longer accepts caller-provided OCR regions in the formal request. NPC OCR regions are resolved through `OcrRoiMemoryService.recommendNpcClickRegions(...)` so old hardcoded task/window rectangles cannot silently re-enter the production click path.
- `clickNpcSmart(...)` now tries yellow target-name OCR first with `GameTextLineOcrService.findYellowTarget(...)`, then falls back to the old purple self-name anchor + coordinate formula, and uses Ctrl-menu dense scan last.
- The three NPC click strategies are split into independent methods: `clickNpcByYellowTargetName(...)`, `clickNpcByPlayerAnchorFormula(...)`, and `clickNpcByCtrlMenuScan(...)`. `clickNpcSmart(...)` is now just the default ordered composition.
- `NpcClickService.debugClickNpcSmartFirstShot(...)` records the debug first-shot point as an unverified debug sample.

Behavior:

- This is record-only. It does not change the NPC click formula, the Ctrl-probe fallback, dialog handling, or task business logic.
- The data is now sufficient to start implementing ROI shrinking policy and later NPC direct-click learning on top of `vision_memory.json`.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-24 local OCR / vision memory review

Status: review notes for Tangde / Xie Shuai

Current decision:

- Stop using the current map-survey projection/interpolation algorithm as the main solution for player/NPC screen-point estimation.
- Move the next experiment direction to local OCR plus saved vision observations.
- Treat `config/vision_memory.json` as the main raw-data memory for OCR and future learning.

Review after reading `docs/ACTIVE_WORK.md`, `docs/LOCAL_OCR_EXPERIMENT.md`, `OcrRoiMemoryService`, and `OcrWindowScanService`:

1. The general direction looks right.
   - `vision_memory.json` is record-only for now, which is important.
   - It should not immediately change business behavior until we have enough real samples and can verify accuracy.
   - Storing OCR attempts, player-name anchor samples, and NPC click samples in one place is reasonable for the current experiment phase.

2. Please keep raw observations separate from learned policy.
   - Current `MemoryEntry` stores both samples and `recommendedRoi`.
   - That is okay short term, but long term we should mentally separate:
     - raw evidence: OCR attempts / anchor samples / click samples;
     - derived policy: recommended ROI / future click model.
   - If a learned ROI becomes bad, we need to be able to clear or recompute policy without losing raw samples.

3. `saveMemory(...)` should eventually use safe writes.
   - Current `OcrRoiMemoryService.saveMemory(...)` writes directly to `config/vision_memory.json`.
   - This data will become expensive to recreate after many OCR/click samples.
   - Recommendation: mirror the safer config-write style used elsewhere: write a sibling temp file, then atomic move/replace.

4. Provider/preprocess identity should be explicit in saved data.
   - The local OCR experiment will mix local-only, compare, Baidu, masked-window, learned-ROI, and segmented-center paths.
   - Current samples store `source`, `purpose`, and `regionType`, but we should make sure every OCR attempt can answer:
     - OCR provider: local / baidu / compare-returned-baidu / hybrid-local / hybrid-fallback;
     - preprocessing variant: full masked window / learned ROI / segmented purple line / segmented yellow line / task panel crop;
     - debug image paths or stable image ids for replay.
   - Without this, good and bad samples from different pipelines may get blended under the same key.

5. Memory keys may need stronger namespacing.
   - `player-name|<name>` is useful, but can become too broad if the same role name, server, task, or window layout differs.
   - NPC click keys should also stay task/map/NPC/target specific.
   - Suggested key dimensions where available:
     - purpose/task;
     - server/player name/player id;
     - map name and map coordinate;
     - target text or target NPC;
     - window size;
     - OCR/preprocess path.

6. NPC click samples should not treat normal-run `actualClick` as ground truth unless it was measured.
   - In normal first-shot flow, `actualClickAbs` may be the same point we predicted/clicked, not an independently verified true NPC point.
   - The useful supervision signal is `success + verification`.
   - If we later train/derive a click correction model, we should only use samples with a strong verification signal or explicit manual measurement.

7. Player-anchor samples need enough context to debug OCR mistakes.
   - The current fields `mapName`, `mapX/Y`, `anchor`, `anchorDelta`, `cameraState`, matched text/fragment/mode/score are good.
   - I would also keep/record the image path or image hash for the sample if possible, because OCR mistakes are hard to reason about from coordinates alone.
   - If the minimap coordinate read is unstable, store that confidence/source too, so wrong map coordinates do not pollute later analysis.

8. Retention policy is okay for logs, but training data may need a protected subset.
   - `MAX_OCR_ATTEMPTS = 1000`, `MAX_GLOBAL_SAMPLES = 600`, `MAX_NPC_CLICK_SAMPLES = 600` is fine for rolling diagnostics.
   - If we manually validate high-value samples later, they should not be trimmed away with ordinary rolling attempts.
   - Consider a future `acceptedSamples` / `pinnedSamples` section or a separate curated file.

9. Current local config check:
   - I only see `config/ocr_roi_memory.json` locally right now, not `config/vision_memory.json`.
   - That may simply mean the new path has not been run yet.
   - First validation should confirm the new file is created and contains the three expected top-level streams.

Recommended next steps:

1. Run local OCR debug enough times to generate real `vision_memory.json` samples.
2. Verify the file has reproducible sample context: provider, preprocessing path, crop/region, map/coord, matched text, score, image path/id.
3. Add safe-write for `vision_memory.json` before collecting lots of manual data.
4. Keep this record-only until we have enough sample volume and can inspect false positives/false negatives.

### Tangde - 2026-05-24 response to He Li vision-memory review

Status: implemented

Accepted review points:

- Kept `vision_memory.json` record-only. No OCR ROI policy or NPC click model is used by business logic yet.
- Added safe-write for `config/vision_memory.json`: write sibling temp file first, then atomic move when supported, falling back to replace-existing move.
- Added explicit OCR sample context:
  - `provider`
  - `preprocessVariant`
  - `rawPath`
  - `maskedPath`
  - `overlayPath`
  - `roiPath`
- Added explicit player-anchor context:
  - `provider`
  - `preprocessVariant`
  - `imagePath`
  - `secondaryImagePath`
  - `locationSource`
- Added NPC-click supervision clarity:
  - `actualClickMeasured`
  - `actualClickSource`
  - `verificationStrength`
  - Normal first-shot samples now mark `actualClickMeasured=false`; they should be used as "prediction + verification result", not as an independent true NPC point.
- Exposed `TextRecognizer.currentProviderName()` so OCR sample records can distinguish configured provider paths. In compare mode, masked-window samples label provider as `compare-returned-baidu` because the returned business result is Baidu while local is only logged for comparison.

Data sufficiency conclusion:

- For step 1, shrinking OCR regions, the stored data is now enough to implement policy later: attempts include key, target, provider, preprocessing variant, scan region, matched text rectangle, success/failure, image paths, and rolling recommended ROI.
- For step 2, future NPC direct-click learning, the stored data is now enough to start learning safely: samples include current map coordinate, target NPC/map coordinate, player anchor, predicted click point, formula version, tune values, and verification outcome. Strong model training should still filter for `verificationStrength` and avoid treating unmeasured click points as ground truth.

Remaining future-only items:

- If we later manually validate high-value samples, add a curated/pinned section so those samples are never trimmed by rolling retention.
- Per the 2026-05-25 schema decision above, do not split or migrate `vision_memory.json` now. If raw/policy separation becomes cumbersome later, add a compatible derived-policy layer while preserving existing raw sample streams and historical samples.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 masked full-window OCR and ROI memory

Status: implemented / debug entry connected

Goal:

- Start the OCR-region plan with a safe baseline: capture the full 1024x768 game window, mask UI areas that should not be sent to OCR, then run OCR on the masked image.
- Add a lightweight ROI memory layer so repeated successful detections can prefer a smaller learned region before falling back to the full masked window.

Default masked-out relative regions:

- `0,0 -> 258,200`
- `0,0 -> 1024,54`
- `768,58 -> 1020,160`
- `4,735 -> 706,768`
- `710,700 -> 1024,768`

Changed:

- Added `OcrWindowRegion`.
- Added `OcrWindowScanService`.
  - Captures the current bound game window using current tracker base.
  - Writes raw and masked debug images through `WindowScopedTempPath`.
  - Also writes `*_mask_overlay.png`; red areas are masked out, blue area is the learned ROI if one exists.
  - If ROI memory exists for the key, scans that ROI first.
  - If the ROI scan misses the target text, falls back to the full masked window.
- Added `OcrRoiMemoryService`.
  - Upgraded the memory file to `config/vision_memory.json`; if only the old `config/ocr_roi_memory.json` exists, it is read as a legacy source and future writes go to `vision_memory.json`.
  - Recomputes a recommended ROI from recent successful samples.
  - Keeps the first version intentionally conservative: learned ROI only narrows the first attempt; full-window masked OCR remains the fallback.
  - Successful player-anchor samples now store map name/coordinate, anchor point, text rectangle, OCR source, matched text/fragment/mode, score, window size, center point, `anchorDelta`, and a coarse `cameraState`.
- Connected the vision-memory write to `PlayerNameOcrDebugService`.
  - The current debug button path uses the segmented/enhanced center-crop OCR result as the player-name anchor source.
  - `OcrWindowScanService` and masked full-window ROI memory are available as the next fallback/integration point, but the current button does not rely on it as the primary anchor path.
  - Successful player-name anchors are recorded under key `player-name|<name>`.
  - The debug result now reports structured anchor output:
    - `segmentedMatch`: center segmented-enhanced OCR result.
    - `selected`: the match actually used for the anchor.
    - `anchorSource`: currently `SEGMENTED_CENTER` or `NONE`.
    - Each match includes anchor point, matched text, text rectangle, fragment/mode, and score.
  - On successful name-anchor recognition, `PlayerNameOcrDebugService` now also reads the current mini-map coordinate under the selected window context and writes a full vision-memory sample.

Current scope:

- This does not replace formal NPC/menu/dialog/task OCR yet.
- Next integration candidates are NPC first-shot player-anchor OCR and target/NPC text OCR after the debug path proves stable.
- The current `cameraState` is a coarse screen-center delta classification (`CENTERED`, `LEFT`, `RIGHT`, `UP`, `DOWN`, or combined). It is meant as raw training data, not the final camera model.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 live window geometry refresh

Status: completed

Goal:

- Keep backend window base coordinates synchronized after the user drags a registered game window.
- Avoid continuing to use stale `WindowNativeBinding` geometry after hwnd position changes.

Changed:

- Added `WindowNativeBindingRefreshService`, a no-focus/no-input helper that reads the current hwnd rect through `IsWindow + GetWindowRect`.
- Added `WindowNativeBinding.withGeometry(...)` and `hasSameGeometry(...)`.
- `MultiWindowTaskManager` now refreshes live native geometry before task submission and before producing UI/system snapshots.
- `GameClientTracker` now refreshes the current bound window geometry before updating `windowBaseX/windowBaseY`, instead of trusting the old stored binding.
- `TaskWindowRuntimeService` now returns a refreshed binding when it resolves task-window runtime geometry.

Expected behavior:

- If the game window is moved while still registered, the UI Base column should update on the next UI refresh.
- Subsequent screenshots/click coordinate calculations should use the moved window position.
- If the hwnd is gone or has no live rect, task submission fails as stale binding instead of running against the old coordinates.

Known limitation / follow-up:

- Moving a game window while a task is actively clicking or reading coordinates is not treated as a synchronized operation yet.
- Current behavior is poll/use-time refresh: after the drag settles, the next UI refresh or backend coordinate refresh should pick up the new geometry.
- Future optimization can pause or debounce per-window task execution while geometry is changing, then resume after the window rect is stable for a short period.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-24 local OCR direction and name-anchor debug

Status: active experiment / user testing

Goal:

- Validate local OCR accuracy before replacing Baidu OCR in normal task flows.
- Start with the player-name / name-anchor use case because Wuhuan already uses OCR name fragments to estimate the character screen anchor.
- Keep the test easy to run from the main page.

Current decision:

- Use the local RapidOCR sidecar as the preferred experiment path for new OCR validation.
- Do not globally replace Baidu OCR yet.
- Current OCR validation uses `bot.dhxy.ocr.provider=hybrid`: business flows try local OCR first, and target-matching OCR paths should retry Baidu when local text does not match the expected target.
- Debug-only local OCR calls may bypass provider routing and use the local sidecar directly.

Changed:

- Added `scripts/local_ocr_server.py` and `scripts/requirements-local-ocr.txt`.
- Added `docs/LOCAL_OCR_EXPERIMENT.md` with install/start/provider-mode notes.
- Added `TextRecognizer.getAllTextResultsLocalOnly(...)` for debug-only local OCR.
- Added `PlayerNameOcrDebugService`.
- Added main-page button `本地OCR测名字`.

How the name debug works:

- Select exactly one registered window on the main page.
- Click `本地OCR测名字`.
- The debug focuses the selected game window, waits briefly, captures the bound HWND, crops a larger center region, saves raw and washed images, runs local OCR on the washed image, and logs detected words plus relative/absolute anchor coordinates.
- Images are written to `images/temp/player_name_ocr/<windowId>/latest_raw.png` and `latest_washed.png`.

Next:

- User will run the main-page debug button and inspect whether local OCR finds enough of the player name despite special symbols.
- If local OCR is accurate enough, consider switching specific OCR-heavy debug paths to `local` or normal flows to `hybrid`.

### He Li - 2026-05-23 map survey UI for map labels and camera bounds

Status: implemented / needs user calibration samples

Goal:

- Add a UI-assisted long-term map survey path for replacing fragile OCR/player-anchor guesses.
- Reuse one UI map name for both minimap-label template sampling and camera-bound recording.
- Keep this as an explicit debug/calibration action, not normal task startup behavior.

Changed:

- Added `MapSurveyService`.
- Added main task-selector buttons:
  - `保存地图名样本`
  - `测试地图名`
  - `记左边界`
  - `记右边界`
  - `记上边界`
  - `记下边界`
  - `测角色点`
- The buttons all use the existing `地图校准名` input.
- Minimap map-label samples are saved under `images/template/map_label/<地图名>.png`.
- Camera-bound samples are saved into `config/map_camera_bounds.json`.
- `MiniMapCoordinateReader` now exposes public helpers to extract a clean map-label image and to read a location snapshot from an already captured minimap strip.

How to use:

- Select exactly one registered window.
- Enter the map name in `地图校准名`.
- For map-label recognition:
  - click `保存地图名样本`;
  - click `测试地图名` to verify current minimap label matches the saved template.
- For camera bounds:
  - walk to the map's left/right/top/bottom camera edge;
  - place the mouse on the character body/feet;
  - click the corresponding boundary button.
- After all four boundaries are recorded, `测角色点` reads the current minimap coordinate and estimates the character's screen-relative point.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 task pause checkpoints in navigation

Status: completed

Goal:

- Make pause/stop requests reach long-running navigation detection loops promptly.
- Use a task execution context holder so deep services can checkpoint without widening business method signatures.
- Keep real input sequences atomic and avoid pausing inside input worker callbacks.

Owns:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContextHolder.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`

Avoids:

- Changing FiveRing/Xiuluo business flow.
- Changing movement/dialog detection thresholds.
- Inserting pause waits inside atomic input queue callbacks.

Changed:

- Added `TaskExecutionContextHolder` as a task-thread `ThreadLocal` holder with `checkpointIfPresent()`.
- `WindowTaskRunner` now binds each task execution context around startup initialization and task execution.
- `MultiWindowTaskManager` injects the holder into each runner.
- `NavigationService` now checkpoints pause/stop in long-running navigation and mini-map pathing confirmation loops:
  - before/after combat-state polling;
  - before/after movement/dialog/location detection;
  - after long sleeps;
  - between retry attempts.
- Checkpoints are intentionally not added to the generic sleep helper or inside input worker callbacks.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 UI map transform calibrator

Status: implemented / needs user calibration test

Goal:

- Add an explicit UI-triggered debug task for writing missing mini-map transforms such as `瑶池` into `config/maps.json`.
- Keep calibration manual and safe: no automatic clicks, no normal task behavior changes.

Changed:

- Added task type `debug_map_calibrator` / `地图校准`.
- Added `DebugMapCalibratorTask`, which reads the map name from UI runtime config, waits for two stable mouse points, OCRs coordinate candidates from a full-window debug capture, picks the candidate nearest the mouse, calculates `CoordinateHelper.MapTransform`, and writes `config/maps.json`.
- Added a `地图校准名` input on the task selector UI. The value is synced into `BotProperties.debugMapCalibratorMapName` before task start.
- Skipped normal startup initialization for `debug_map_calibrator`, so map-tracking setup / Alt+6 prep does not disturb a manually prepared calibration screen.

How to use:

- Select one bound window.
- Enter the target map name, e.g. `瑶池`, in `地图校准名`.
- Select `地图校准` and start.
- Open/prepare the map in-game, place the mouse on point A until the task beeps/logs success, then move to point B and hold again.
- Pick two points whose logical X and Y both differ.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 main refresh scans game windows

Status: completed

Goal:

- Make the main page refresh button perform real game-window scan/register instead of only repainting the table.
- Give visible UI feedback while scanning and after scan results return.
- Keep start-button scan/start behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing task start assignment behavior.
- Changing window discovery service behavior.

Changed:

- Main page refresh button now calls `scanAndRefreshGameWindowsFromMain()` instead of only repainting the table.
- The refresh action runs `GameWindowRegistrationService.registerDetectedGameWindows(...)`, so it scans real game windows and updates registrations/bindings.
- The UI now logs and shows an action hint immediately while scan is running.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 Xiuluo hot-start and task takeover consensus

Status: active design / next implementation target

Goal:

- Make Xiuluo leader task startup/takeover robust from real in-game states, not only from a clean empty screen.
- Let other agents share the same Xiuluo assumptions before touching UI, framework, or dialog code.
- Keep Wuhuan's existing hot-start path separate unless the user explicitly asks to merge it.

Owner:

- He Li owns Xiuluo task takeover, Xiuluo dialog/template flow, and Xiuluo task-state transitions.

Other-agent handoff:

- Startup visibility prep should keep map-tracking setup and Alt+6 visibility confirmation before task navigation.
- After Alt+6 visibility is confirmed, wait about 1s before dialog detection so the floating "hide players" toast can disappear.
- `NavigationService.navigateToNPC(...)` should not run generic UI cleanup if a dialog is already open after arrival. Business dialogs must be left for the current task to classify.

Do not:

- Put Xiuluo-specific templates into `NavigationService`.
- Randomly click unknown option dialogs.
- Widen map-coordinate arrival checks as a workaround for NPC-name clicks.
- Change Wuhuan's validated hot-start behavior while implementing Xiuluo.

Shared rules now agreed:

- Startup order is: generic startup prep -> Alt+6 visibility/fade wait -> task-level dialog hot-start detection -> navigation/click NPC only if no recognized dialog state exists.
- STORY dialogs are usually ignored during normal task progress. Xiuluo only reads the accept-task STORY immediately after accepting a task, because that story contains target map and coordinate.
- OPTION dialogs are high-priority after startup prep. The active task must classify whether the option belongs to its own stage.
- Unknown OPTION dialogs should be cleaned or skipped by policy, not blindly clicked.
- If current-map navigation to an NPC opens a dialog, that counts as arrival success even if player coordinates do not equal the clicked mini-map target. This matters for Xiuluo because clicking the yellow NPC name can make the game auto-walk to the NPC body and open the dialog.

Xiuluo hot-start states to support:

- Accept-task option already open: match the Xiuluo accept template such as `xiuluo_accept_xianlaiwu.png`, click the accept option, then read the accept STORY.
- Under-five prompt already open: match the under-five confirm/wait templates and follow the configured user policy.
- Accept STORY already open: read target map/coordinate from the story and continue navigation.
- Existing task but no useful story: open Quest Manager and read the Xiuluo objective from the task panel as fallback.
- Enter-battle option already open: match the Xiuluo battle option such as "看打!", click it, then enter auto-battle/wait-combat flow.
- In combat at startup: wait for combat to finish, then continue into Xiuluo post-combat handling.
- Post-combat return state is still a known gap: after combat, Xiuluo should use the return item, return to the task NPC, accept the next round, then only yield when the leader has started meaningful movement or a safe wait state.

Recent evidence:

- A run around `2026-05-23 12:33` showed the Xiuluo accept dialog was visible, but early dialog detection missed it while the Alt+6 toast was likely still fading.
- Later the same run detected an OPTION dialog during `navigateInCurrentMap:dialog-arrived`, which confirms dialog-arrival should be treated as current-map navigation success.
- Generic arrival cleanup then saw an OPTION dialog through `ui-cleaner:force-close`, so cleanup must not close task-owned business dialogs before Xiuluo classifies them.
- Current dialog debug images use fixed filenames and can be overwritten; when diagnosing timing-sensitive dialog misses, add timestamped or reason-scoped evidence before drawing conclusions.

Files likely involved:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`

Implementation checkpoint:

- Added a generic `task.hotstart` screen classifier:
  - `TaskHotStartService`
  - `TaskHotStartSnapshot`
  - `TaskHotStartScreenState`
- The generic classifier only reports coarse current-screen state: `IN_COMBAT`, `OPTION_DIALOG`, `STORY_DIALOG`, or `NONE`.
- Xiuluo owns the task-specific interpretation of those states.
- `XiuluoTask` now probes current screen at round start before doing accept-NPC navigation.
- Xiuluo can now take over from:
  - already in combat;
  - enter-battle option already open;
  - accept-task option already open;
  - under-five prompt already open;
  - accept-task STORY already open;
  - existing Xiuluo task in Quest Manager when no useful dialog is visible.
- `clickTargetAndEnterBattle(...)` also checks whether the battle-confirm option is already open before trying another target click.
- Xiuluo no longer consumes `IN_COMBAT` in the outer execute loop; combat is now handled inside the round flow so post-combat return can run.
- Xiuluo hot-start decisions now emit high-signal `[XIULUO_HOT_START]` logs with `source`, `screen`, `action`, and objective target when available.
- Xiuluo only uses Quest Manager "existing task" hot-start on the first loop/true startup. After a completed combat-return round, the next loop skips that fallback so it does not misread stale task-panel objectives before accepting the next round.
- Auto-battle follower support mode still skips general idle maintenance, but now keeps the return-team button check so members can归队 while the leader waits after Xiuluo return.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 capture resource release hygiene

Status: completed

Goal:

- Reduce screenshot/detection memory pressure from high-frequency multi-window capture.
- Explicitly release copied/intermediate `BufferedImage` and graphics resources where safe.
- Keep detection behavior and task logic unchanged.

Owns:

- `src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java`
- `src/main/java/com/bot/dhxy/tools/ImagePreprocessor.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing capture provider selection or fallback behavior.
- Changing battle/movement/dialog detection thresholds.

Changed:

- `BoundWindowCaptureService.captureRegion(...)` now releases the full-window image after copying the requested crop.
- `BoundWindowCaptureService.captureRegionToFile(...)` now releases the cropped image after writing it to disk.
- Blank PrintWindow images are released when BitBlt succeeds and becomes the returned provider.
- `ImagePreprocessor` now disposes temporary `Graphics2D` objects and flushes temporary BGR conversion images after copying data into OpenCV `Mat`.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 Alt6 visibility template confirmation

Status: completed

Goal:

- Replace blind startup `Alt+6` double press with a template-confirmed visibility preparation loop.
- Confirm the game is in the desired "other players hidden/name-only" state by matching `images/template/2.png` in the user-provided window-relative region.

Owns:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing task business logic.
- Changing HWND screenshot or input worker internals.

Changed:

- Convert user-provided absolute region `(1661,690)-(1978,978)` with base `(1302,419)` into window-relative `(359,271)-(676,559)`.
- `NavigationService.prepareTaskStartupWindow()` now calls `ensureAlt6VisibilityDirect()` after map-tracking setup.
- The visibility helper checks `images/template/2.png` before pressing.
- If not confirmed, it presses `Alt+6`, waits 500ms, re-checks, and stops once matched; max attempts is 3.
- Startup visibility failure now makes `prepareTaskStartupWindow()` return false, so a task will not continue when the hidden-name state cannot be confirmed.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 guard member story fast click

Status: completed

Goal:

- Allow UI cleaner story fast-click for leader windows under the existing cleanup conditions.
- Restrict member windows so story fast-click only runs while the current window is in combat.
- Keep pure dialog detection no-focus and avoid changing business dialog/option click behavior.

Owns:

- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing FiveRing/Xiuluo task business logic.
- Changing dialog detection/template matching logic.

Changed:

- `UICleanerService.forceCloseDialog()` now checks role/state before fast-clicking a STORY dialog.
- Leader or unknown-role windows keep the existing UI cleaner behavior.
- Member windows only fast-click STORY dialogs while `GameContext.ActionState` is `IN_COMBAT`; outside combat they log and skip.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 make dialog detect default no-focus

Status: completed

Goal:

- Make `DialogService.detectDialogType()` a pure no-focus detection path by default.
- Remove the old input-queue wrapping that existed for Robot screenshot/focus requirements.
- Keep real click/keyboard dialog operations focused through their existing input queue paths.

Owns:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing dialog business policies or task flow decisions.
- Changing Npc/FiveRing/Xiuluo click logic beyond detection focus behavior.

Changed:

- `DialogService.detectDialogType()` now delegates to `detectDialogTypeNoFocus("detect-dialog-type")` directly.
- `DialogService.handleDialog(...)` uses no-focus detection after an optional initial click as well; the initial click itself remains on the existing focused input path.
- Real dialog input paths such as story click, green option click, give-item, and template-option click still use the input queue/focus where they send mouse input.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 Xiuluo follower auto-battle quiet mode

Status: completed

Goal:

- Prevent Xiuluo member windows that were auto-reassigned to AutoBattle from stealing input while the leader is still accepting/pathing.
- Keep explicit standalone AutoBattle behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Changed:

- `TaskExecutionContext` now carries `requestedTaskCode` / `requestedTaskName` in addition to the resolved running task.
- `WindowTaskRunner` preserves the originally requested task when a member window is reassigned from a leader task such as Xiuluo to `AUTO_BATTLE`.
- `AutoBattleTask` detects follower-support mode when `windowRole=MEMBER`, current task is AutoBattle, and requested task differs from AutoBattle.
- In follower-support mode, AutoBattle still polls combat state, but skips FREE-state idle maintenance such as return-team clicking, maintenance-broadcast dialog handling, and summon-skill cleanup until combat is actually detected.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 Main UI Base Coordinate Probe

Status: completed

Goal:

- Let the user observe which top-left base coordinate the UI/runtime is currently using after moving or detaching the game chat window.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added a `Base` column immediately after the role-name column in the main window table.
- The value is calculated the same way `GameClientTracker.updateBaseFromBinding(...)` calculates task base coordinates:
  - `baseX = nativeBinding.x / CoordinateHelper.getScaleRatio()`
  - `baseY = nativeBinding.y / CoordinateHelper.getScaleRatio()`
- The cell tooltip shows the logical tracker base, native rect, scale ratio, and hwnd.
- This is a UI diagnostic only; no task/capture/click logic changed.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-23 dialog empty-detect no-focus

Status: completed

Goal:

- Stop pure empty dialog checks from bringing game windows to foreground during AutoBattle/UI cleanup patrol.
- Keep real dialog clicks/give-item/story-click paths on focused input.

Owns:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `docs/ACTIVE_WORK.md`

Changed:

- `DialogService.handleDialog(...)` now uses `detectDialogTypeNoFocus(...)` for requests without an initial click. If no dialog exists, it returns `NO_DIALOG` without entering `dialog:detectType` focused input queue.
- Requests with an initial click still use the focused path after the click, and detected dialogs still use existing focused click/ocr/give-item paths where real input is needed.
- `UICleanerService.forceCloseDialog()` now also starts with no-focus detection, so empty cleanup checks do not focus a game window.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

Expected log change:

- Empty `handleDialog(CLICK_BUSINESS_OPTION)` patrols should show `dialog detect no-focus: reason=handle-dialog:CLICK_BUSINESS_OPTION result=NONE`.
- They should no longer be followed by `Interaction metrics ... event=focus action=queued:dialog:detectType`.

### Tangde - 2026-05-23 restore centered task count badge

Status: completed

Goal:

- Restore the main task selector count badge toward the earlier HTML mock: task name, meta, and a small centered count pill inside the task tile.
- Keep this as a UI-only correction after the user rejected the bottom-right badge / dialog-like visual direction.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` task tile layout only
- `src/main/resources/styles/dhxy-fluent.css` task tile styles only
- `docs/ACTIVE_WORK.md`

Avoids:

- Task execution semantics.
- Window/task backend behavior.
- Other agents' task business logic.

Changed:

- `MainWindowController.buildTaskTile(...)` now lays out task name, meta text, and the small count badge in one centered vertical stack, matching the earlier HTML mock direction.
- The order badge remains a small top-right dot.
- Task tile size is back to the mock-like square proportion.
- `dhxy-fluent.css` restored the count badge to a centered small pill style instead of the bottom-right corner badge.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

Open:

- Follow-up after user feedback: the first restore was still too large and did not make count editing visible enough.
- Task tiles were reduced to 70x70.
- Clicking the centered count pill now opens an inline count editor under the task grid with `- / input / + / apply / cancel`.
- `mvn -q -DskipTests compile` passed again after rerunning with dependency/network access.
- Follow-up: user clarified the count marker should not be a rounded pill. It is now a plain clickable text marker, bottom-centered on the task tile's own square base, with no separate border/background.
- `mvn -q -DskipTests compile` passed after the bottom-marker adjustment.
- Follow-up: user clarified the count marker should sit on the task tile bottom border line itself. The marker is now bottom-centered and translated downward so the tile's bottom line visually crosses through the count text.
- `mvn -q -DskipTests compile` passed after the border-line placement adjustment.
- Follow-up: user found the original reference in `docs/DHXY_FLUENT_MOCK.html` and asked to follow that mock instead of the border-line interpretation.
- Reverted the JavaFX task selector toward the mock:
  - title row has `任务选择` plus right-side selected-task summary and light `次数` button;
  - task tiles are back to 82x82 mock-like cards;
  - count summaries are small rounded count badges inside the task card content;
  - the inline count editor remains below the grid, matching the mock's lightweight count-popover direction.
- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.
- Follow-up: user screenshot showed the count badge was still escaping below the JavaFX task tile. The task tile now uses `ContentDisplay.GRAPHIC_ONLY` plus fixed internal graphic/text-stack sizes so `name / meta / count` stay inside the 82x82 card.
- `mvn -q -DskipTests compile` passed after the fixed-card-layout correction.
- Follow-up: user pointed out the mock is not vertically centered; its content is top-flowed with `strong` taking a 32px block and the count badge near the bottom. JavaFX task tiles now remove button padding and explicitly lay out name/meta/count from a 12px top inset, with the count badge below meta and near the card bottom.
- `mvn -q -DskipTests compile` passed after the mock-padding layout correction.
- Follow-up: fixed task-count interactions after user feedback:
  - count editor keeps a fixed reserved height while hidden so opening it does not move task cards;
  - clicking the same task count badge again preserves the in-progress value instead of resetting from the badge text;
  - count +/- buttons add/subtract 1 on normal click and repeat by 10 while held down.
- `mvn -q -DskipTests compile` passed after the interaction fixes.
- Follow-up: slowed the held +/- repeat rate for task count editing. Hold still changes by 10, but repeat interval is now 350ms after a 550ms hold delay for better control.
- Validation is currently blocked by unrelated in-progress `XiuluoTask.java` / `BotProperties` compile errors from another lane, not by the UI files.

This file is the short-term multi-agent coordination board for the DHXY project.

Every agent must read these files before editing:

1. `AGENTS.md`
2. `docs/DHXY_CONTEXT.md`
3. `docs/ACTIVE_WORK.md`

## Document Roles

### `docs/DHXY_CONTEXT.md`

Long-term project memory. Use it for:

- architecture direction;
- settled design decisions;
- tested conclusions;
- important historical bugs and root causes;
- long-term multi-agent ownership principles;
- session-resume guidance.

Do not add every small implementation detail there.

### `docs/ACTIVE_WORK.md`

Short-term collaboration board. Use it for:

- who is currently working on what;
- which files each agent owns right now;
- which files each agent should avoid;
- unfinished risks;
- handoff notes;
- interface/field requests between agents.

This file can be updated frequently.

## Required Update Rules

Each agent must update `docs/ACTIVE_WORK.md` in these cases:

1. Before starting a new task.
   - State the goal, owned files, avoided files, and planned files.
2. Before editing a high-conflict file.
   - High-conflict examples: `FiveRingTask.java`, `BattleRadarService.java`, `QuestManagerService.java`, `WindowTaskRunner.java`, `MultiWindowTaskManager.java`, `WindowTaskControlService.java`, `MainWindowController.java`, `SummonSkillService.java`, `AutoBattleTask.java`.
3. After finishing a meaningful phase.
   - State what changed, which files changed, validation status, and open issues.
   - Tell the user the next planned step in the chat response, not as a required `docs/ACTIVE_WORK.md` entry.
4. When another agent is needed.
   - Do not broad-edit another agent's files. Record the needed interface/field and the reason.
5. When pausing, changing direction, or abandoning a plan.
   - Leave a clear status so the next agent does not continue stale work.

Update `docs/DHXY_CONTEXT.md` only when:

1. architecture direction changes;
2. a design decision is settled;
3. a test result is confirmed;
4. a bug pattern is likely to recur;
5. session-resume instructions need to change.

## File Ownership Rules

- The agent that declares `Owns` has priority for those files.
- If another agent needs to edit an owned file, it must first record the need here and ask the user or owning agent to coordinate.
- Prefer asking for a small interface/field instead of directly changing another agent's implementation.
- Always run `git status` and inspect this file before editing.
- Do not revert unrelated dirty work.

## Current Named Agent Lanes

Name mapping:

- 何黎: framework / multi-window foundation
- 谢帅: summon skill / auto battle
- 唐德: UI

### He Li - 2026-05-23 Xiuluo task-panel fallback reuse

Status: completed

Goal:

- Make formal `XiuluoTask` use the newly verified one-shot task-detail capture for task-panel fallback.
- Avoid reopening the task panel separately for template fallback and OCR fallback.

Owns:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java` only if the existing capture API needs a tiny adjustment.

Avoids:

- Changing Wuhuan flow.
- Broad navigation/dialog/business rewrites.

Changed:

- `XiuluoTask` task-panel fallback now calls `QuestManagerService.captureCurrentQuestDetailForTask(...)` once.
- Template fallback and OCR fallback reuse the same saved right-detail screenshot.
- The already-tested story objective path was already formal; this change only aligns the rare task-panel fallback path.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 Xiuluo first workflow skeleton

Status: phase 1 completed

Goal:

- Replace the Xiuluo placeholder task with the first real leader workflow skeleton based on the user-defined flow.
- Keep member windows out of Xiuluo business logic; members should use AutoBattle/maintenance paths.
- Build the main loop around: accept task, read objective story, pre-move to Ling Shou Village exit, establish world-map pathing, maintain during formal pathing, click 修罗, enter combat, use return item, repeat.

Owns:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/task/model/TaskType.java`
- `src/main/java/com/bot/dhxy/task/DefaultTaskFactory.java`
- small Xiuluo-specific additions in `DialogService`, `BagService`, and `NavigationService` if needed.

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- broad framework/window runner changes.
- changing validated Wuhuan behavior.

Plan:

- Wire Xiuluo into task type/factory.
- Add reusable targeted dialog helpers for Xiuluo accept/confirm templates without making DialogService globally auto-handle Xiuluo.
- Add a BagService path to use a target item by scanning bag pages from the back, for the Xiuluo return item.
- Implement the first Xiuluo loop with clear TODO logs for missing image templates.

Open:

- The screenshot-derived template PNG files still need to be created in the expected paths before runtime testing can pass.

Changed:

- `XiuluoTask` now runs the first leader workflow skeleton instead of returning `SKIPPED`.
- `TaskType` and `DefaultTaskFactory` now expose/create `XIULUO`.
- `TaskTeamAssignmentPolicy` treats Xiuluo like Wuhuan for member reassignment to AutoBattle.
- `DialogService` has generic helpers for clicking a green option by template and reading green story text.
- `NavigationService` has no-yield mini-map/world-map pathing triggers for task-owned chained transactions.
- `BagService` can scan item pages from back to front for Xiuluo return item usage.
- `NpcClickService` has a Ctrl-menu keyword click path that requires the yellow menu text to contain the keyword, avoiding the older generic NPC-tag match.
- `BotProperties` / `application.properties` include first Xiuluo config defaults:
  - `bot.dhxy.xiuluo-max-runs=1`
  - `bot.dhxy.xiuluo-allow-under-five-members=false`

Validation:

- `mvn -q -DskipTests compile` passed.

Open after phase 1:

- Need create/verify template PNGs:
  - `images/template/dialog/xiuluo_accept_xianlaiwu.png`
  - `images/template/dialog/xiuluo_underfive_confirm.png`
  - `images/template/dialog/xiuluo_underfive_wait.png`
  - `images/template/dialog/xiuluo_enter_battle_kanda.png`
  - `images/template/item/xiuluo_return_item.png`
- Task-panel fallback target parsing is logged as not implemented yet.
- Cancel-task recovery branch is not implemented yet.

Template tooling update:

- Added `scripts/BuildXiuluoTemplates.java` to generate washed Xiuluo templates with Java `ImageIO`.
- Added `images/template_sources/xiuluo/README.md`.
- Source screenshots are now named:
  - `accept_dialog.png`
  - `under_five_dialog.png`
  - `enter_battle_dialog.png`
  - `return_item.png`
  - `objective_story_example.png`
- Generated and visually checked the current Xiuluo templates:
  - `images/template/dialog/xiuluo_accept_xianlaiwu.png` = "闲来无"
  - `images/template/dialog/xiuluo_cancel_task.png` = "我想取消任务"
  - `images/template/dialog/xiuluo_underfive_confirm.png` = "确定"
  - `images/template/dialog/xiuluo_underfive_wait.png` = "我再想想"
  - `images/template/dialog/xiuluo_enter_battle_kanda.png` = "看打!"
  - `images/template/item/xiuluo_return_item.png`
  - `images/template/npc/npc_tag.png` = "(NPC)"
- `ImageFinder` now has `findAll(...)` for multi-candidate template matching.
- Xiuluo target click now first uses Ctrl-menu `(NPC)` template matching, tries all matched candidates top-to-bottom, and confirms the correct target by matching/clicking the Xiuluo "看打!" dialog option.
- OCR keyword matching through `NpcClickService.clickNpcByCtrlMenuKeyword("修罗")` remains only as fallback after the `(NPC)` template candidate path fails.
- Added Xiuluo task-panel fallback:
  - `images/template/task/xiuluo_title.png` = collapsed/expandable "常规" group.
  - `images/template/task/xiuluo.png` = concrete "修罗" task label.
  - `images/template/task/xiuluo_active.png` = active/highlighted "修罗" task label.
  - `QuestManagerService.readCurrentQuestDetailTextForTask("xiuluo")` activates the task, captures the right detail panel, and returns OCR text.
  - `XiuluoTask` now falls back to task-panel OCR when the accept story objective cannot be parsed.
  - The task-panel scanner does not treat "常规" as the task. It first looks for any Xiuluo task-label variant, clicks "常规" only once if the group is collapsed, then searches again.
  - The Xiuluo task-detail OCR crop is narrowed to anchor-relative `(-269, 12, 264x50)` based on the user-measured task panel coordinates.

### Tangde - 2026-05-22 license worker integration

Status: completed

Goal:

- Connect DHXY main project to the shared `dhxy-license-worker` with a separate `appId=dhxy`.
- Keep DHXY and auto-battle license codes isolated by worker-side `app_id`.
- Add a real renewal endpoint to the worker so expired licenses can be extended by 30 days through the same service.

Owns:

- `src/main/java/com/bot/dhxy/auth/*`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` authentication-tab wiring only
- `src/main/resources/application.properties` license settings
- external local worker files under `D:/mavenProject/dhxy-license-worker/*`

Avoids:

- Wuhuan flow and task business logic.
- Input queue/window runtime/framework files owned by 何黎.
- `SummonSkillService.java` and `AutoBattleTask.java` owned by 谢帅.

Plan:

- Add worker `/api/license/renew` with `appId + licenseCode + deviceFingerprint + days`.
- Add DHXY license client service that posts `appId=dhxy`.
- Replace the placeholder authentication tab with verify/status/renew controls.

Changed:

- Added DHXY auth client package:
  - `src/main/java/com/bot/dhxy/auth/DeviceFingerprintService.java`
  - `src/main/java/com/bot/dhxy/auth/LicenseActionType.java`
  - `src/main/java/com/bot/dhxy/auth/LicenseAuthResult.java`
  - `src/main/java/com/bot/dhxy/auth/LicenseAuthService.java`
- Added DHXY license worker config in `src/main/resources/application.properties`.
- Replaced the `验证` tab placeholder with DHXY license verify / refresh / 30-day renewal controls in `MainWindowController.java`.
- Updated the external local `dhxy-license-worker` project:
  - added `migrations/0002_add_app_id.sql`;
  - added `appId` validation to verify/status/unbind;
  - added `/api/license/renew`;
  - updated license creation scripts to write `app_id`.
- Updated the external local `dhxy-auto-battle` project so its auth requests send `appId=dhxy-auto-battle`.

Validation:

- `D:/mavenProject/dhxy-license-worker`: `npx tsc --noEmit` passed.
- `D:/mavenProject/dhxy-license-worker`: `node --check scripts/create-license.js` passed.
- `D:/mavenProject/dhxy-license-worker`: `node --check scripts/create-license-menu.js` passed.
- `D:/mavenProject/DHXY`: `mvn -q -DskipTests compile` passed after rerunning with network/dependency access.
- `D:/mavenProject/dhxy-auto-battle`: `./mvnw.cmd test` passed.

Open:

- The worker directory is not a git repository, so its files must be deployed/copied through the existing worker deployment process.
- Remote D1 still needs migration `0002_add_app_id.sql` before deploying the new worker.
- Existing remote license rows will default to `app_id='dhxy-auto-battle'`; create or migrate separate DHXY license rows with `app_id='dhxy'`.
- User does not want to run/deploy the worker or remote D1 migration yet. Before generating real license codes later, remind the user to first run the worker deployment/migration steps, especially `0002_add_app_id.sql`, otherwise `app_id=dhxy` / `app_id=dhxy-auto-battle` isolation and renew responses will not exist remotely.

### Tangde - 2026-05-23 stale hwnd startup binding fix

Status: completed

Goal:

- Fix UI `启动` using stale selected window ids after the automatic scan/register step.
- Remove idle old native-bound runners whose hwnds are not present in the latest scan, so tasks do not start against dead bindings.

Files changed:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/window/discovery/GameWindowRegistrationService.java`

Changed:

- `GameWindowRegistrationService.registerDetectedGameWindows(...)` now prunes idle stale registrations before registering current scan results.
- Stale means either an idle manual/unbound runner or an idle native-bound runner whose `windowId` is not in the latest scan result.
- `scanRegisterAndStartIndependentWindows(...)` uses the same stale-prune step instead of removing every runner, so busy windows are not killed during scan/start.
- `MainWindowController.startMainSelectedTasks()` now scans/registers first, then recomputes target window ids from latest snapshots.
- If no windows were selected, it starts all latest `isAcceptingTaskQueue()` windows.
- If windows were selected, it only starts selected ids that still exist after scan and are still accepting task queues.
- If the selected ids went stale or became unavailable, startup returns a clear message and does not submit the old binding.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-23 no-focus dialog capture fix

Status: completed

Goal:

- Prevent pure dialog/story screenshot or detection paths from bringing game windows to foreground when HWND capture is available.
- Keep real mouse/keyboard dialog actions on the existing focused input queue path.

Owns:

- `src/main/java/com/bot/dhxy/service/DialogService.java` no-focus detection/capture path only
- `src/main/java/com/bot/dhxy/core/GameClientTracker.java` capture provider/fallback focus check only if needed

Avoids:

- Dialog click/keyboard business behavior (`handleDialog`, green option click, story fast click) unless required for compile.
- FiveRing/Xiuluo task flow logic.

Changed:

- `DialogService.detectDialogType()` now logs the focused/queued path explicitly.
- Added `DialogService.detectDialogTypeNoFocus(...)` for capture-only detection; it calls `detectDialogTypeDirect()` without entering `InputSequences.submitExclusiveAndWait(...)`.
- `DialogService.readCurrentStoryGreenText(...)` and `captureCurrentStoryImage(...)` now use the no-focus detection path.
- `GameClientTracker` focus-failure capture logs now mark `provider=ROBOT`, so HWND success vs Robot fallback is visible in logs.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 task tile count badge restore

Status: completed

Goal:

- Restore the lighter task count/parameter badge in the main task selector.
- Make the small badge clickable without toggling task queue selection.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` task tile UI only
- `src/main/resources/styles/dhxy-fluent.css` task tile styles only
- `docs/ACTIVE_WORK.md`

Avoids:

- Task execution semantics and backend task parameter contracts.

Changed:

- Task tile count/parameter badge moved back to a small bottom-right badge instead of taking a full row in the tile.
- Selection order badge stays as a small top-right dot.
- Clicking the count/parameter badge opens a small edit dialog and does not toggle task queue selection.
- Task tile size was reduced from the larger square feel to a lighter compact tile.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 pure-vision no-focus audit

Status: completed

Goal:

- Audit formal Xiuluo / task objective reading paths for pure screenshot/OCR work that still enters focused exclusive input.
- Convert only pure detection/capture paths to no-focus; keep paths that open panels, click options, move mouse, or press keys on focused input queue.

Owns:

- `src/main/java/com/bot/dhxy/service/LocationVisionService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java` read-only first; edit only if a pure-capture subpath can be separated safely
- `docs/ACTIVE_WORK.md`

Avoids:

- Xiuluo business decisions and target flow.
- Dialog/NPC click behavior.
- Broad framework/input queue changes.

Findings:

- `QuestManagerService` task-detail fallback opens/selects/clicks the task panel before reading detail text, so its focused exclusive transaction is intentional and was left unchanged.
- `LocationVisionService.scanCurrentLocation()` was a true pure-vision path but still entered `submitExclusiveAndWait("location:scanCurrent", ...)` when a window context existed.

Changed:

- `LocationVisionService.scanCurrentLocation()` now uses no-focus capture when a bound window context exists.
- Legacy no-context fallback still calls `tracker.bringWindowToFront()` because title-search/Robot-style single-window operation needs a visible foreground target.
- Location capture element is now logged as `location-current`.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

Open:

- Next runtime check should confirm position sync logs show `[location] scan current no-focus` and capture provider `HWND_PRINTWINDOW` without `location:scanCurrent` focus events.

### Tangde - 2026-05-23 main UI task queue selection fix

Status: completed

Goal:

- Remove the default Wuhuan task selection from the main task selector.
- Make the main task tiles build a real multi-task queue instead of being overwritten by the current combo-box task at start time.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` task selection / pending queue UI only
- `docs/ACTIVE_WORK.md`

Avoids:

- Window runtime/control backend behavior.
- Non-task-selector layout refactors.

Changed:

- Removed the startup default `pendingTaskQueue.add(TaskType.WUHuan)`, so the main task selector starts empty.
- Removed the combo-box listener that turned current task changes into a single selected queue item.
- Task tiles now toggle membership in `pendingTaskQueue`: click to append, click again to remove.
- Main `启动` now submits the existing `pendingTaskQueue` as-is instead of clearing it and replacing it with the combo-box value.
- Main `启动` is disabled when the queue is empty.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### 何黎: Framework / Multi-Window Foundation

Status: completed

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- input queue/framework classes when needed
- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- large UI refactors in `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- 五环 core behavior unless logs justify it

Current focus:

- Keep `MultiWindowTaskManager -> WindowTaskRunner -> WindowTaskQueue -> TaskType` clean.
- Keep single-window runs on the same multi-window path.
- Expose framework state to UI through snapshots instead of UI touching runners directly.
- Keep physical input serialized through `InputActionQueue`.

Recent status:

- `WindowTaskQueue` has first-class factories and display/log helpers.
- `WindowTaskStartRequest` can carry a `WindowTaskQueue`.
- `WindowTaskControlService` routes `SAME_TASK` through `startSameQueue(...)`.
- `RunningTaskHandle` records queue progress.
- `WindowTaskSnapshot` exposes running queue display/progress/size.
- Compile passed after these changes with `mvn -q -DskipTests compile`.

### He Li - 2026-05-21 startup input serialization fix

Goal:

- Diagnose why a 5-window Wuhuan start visually appeared to stop after opening mini maps.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`

Result:

- Root cause from logs: each window queued `ensureMapTrackingOption` first, then queued the separate startup `Alt+6` sequence. With five windows, the global input queue ran all map checks before the first window's `Alt+6`, delaying real Wuhuan entry and making the UI look stuck at mini-map startup.
- Added `NavigationService.prepareTaskStartupWindow()` so map tracking check and the two `Alt+6` presses run in one exclusive input callback for the same window.
- `DefaultWindowTaskStartupInitializer` now calls this combined startup preparation.
- Removed the separate `WindowTaskRunner` startup `Alt+6` step to avoid duplicated visibility preparation.
- Validation: `mvn -q -DskipTests compile` passed.

Open issue:

- The latest user run was stopped before prepare reached bag checks; next run should confirm first Wuhuan window reaches `五环战前准备-4` sooner and no longer looks stuck after mini-map startup.

### 何黎 - 2026-05-21 broad framework scan

Goal:

- Broad-scan framework/multi-window cleanup instead of only one small point.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `src/main/java/com/bot/dhxy/input/*`
- selected navigation framework/input cleanup in `NavigationService.java`
- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- large UI refactors in `MainWindowController.java`
- RO/role-assignment cleanup while 谢帅 owns backend role recognition

Findings:

- `WindowRuntimeContext.markQueued(...)` and `markStarted(...)` still mutate `selectedTaskType`; this can make UI/default selected task drift when a queue runs multiple task types.
- `markQueueFinished(...)` no longer overwrites `lastResult` / `lastResultMessage`; queue-level result/message lives in the dedicated queue fields.
- Input paths are mostly serialized through `InputSequences`; remaining direct `InputProvider` calls are mainly inside `submitExclusiveAndWait(...)`, debug paths, or 谢帅-owned summon/auto-battle code.
- `NavigationService` has two distinct map-close paths: `Alt+1` mini-map popups close with `Alt+1`, while `Alt+2` world-map search results still close with double-right-click because it closes the search input and the world map together.
- `WindowScopedTempPath` did not respect `bot.window.scoped-temp-path-enabled`; it now honors the switch, and the default config is set to true for multi-window runs.
- Old `bot.run.initGameWindow` config remains in `TaskRunProperties` logging but is no longer an active startup path because `AutoBot` ignores auto-start in multi-window mode.
- Old detected-role/RO start path is still present but deprecated/frozen while 谢帅 works on backend role recognition.

Changed:

- `NavigationService`: corrected map-close semantics after review; `ensureMapTrackingOption()` / mini-map actions use `Alt+1`, while world-map search-result close keeps double-right-click.
- `WindowScopedTempPath`: resolves per-window paths only when `isScopedTempPathActive()` is true.
- `application.properties` / `DHXY_CONTEXT.md`: scoped temp path default documented as enabled.

Validation:

- `mvn -q -DskipTests compile` passed after the code/config changes.

Open:

- Decide whether queue finish should stop writing queue result into per-task `lastResult`.

### He Li - 2026-05-21 capture focus binding investigation

Goal:

- Fix 5-window cross-window screenshot/OCR/template matching caused by screenshots reading a covered or wrong window region.
- Keep Wuhuan/navigation business logic unchanged because the 1-2 window logic has already been validated.

Finding:

- A first attempt to focus the bound window before every `GameClientTracker.captureToFile(...)` / `captureToMemory(...)` was too broad.
- In 5-window mode, ordinary status checks/OCR/template scans become high-frequency foreground switching, making the game windows visibly jump and causing more interference.
- The broad capture-focus change was removed. Future fixes should focus only inside deliberate input/exclusive action segments or replace screen-coordinate capture with real hwnd/window capture.

Validation:

- `mvn -q -DskipTests compile` passed after removing the broad capture-focus behavior.

Open:

- Need a narrower design: focus only for screen-to-click atomic workflows, or implement/test hwnd-based capture that does not require foreground switching.

### He Li - 2026-05-21 dialog atomic screenshot-click fix

Goal:

- Reduce five-window cross-window dialog handling caused by taking a dialog screenshot on one visible window and clicking later after another window has stolen focus.
- Keep Wuhuan/navigation business logic unchanged.

Changed:

- `DialogService` now uses the input queue's exclusive transaction for dialog workflows where screenshot/template detection and the final click must belong to the same bound window.
- Five-ring accept-dialog template click, green option fallback click, and give-option detection/click now perform their capture/detection plus direct click inside one exclusive input callback.
- Dialog OCR/business-option raw captures now run through a short exclusive capture callback, while slower OCR/template processing remains outside the input lock.
- Direct `InputProvider` usage is only inside exclusive callbacks to avoid queue-in-queue deadlock.

Changed files:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 interaction metrics HTML dashboard

Status: completed

Goal:

- Make the focus/capture/keyboard counters visible in a local HTML dashboard instead of requiring manual log reading.

Changed:

- `WindowInteractionMetricsService` now writes `logs/interaction-metrics-dashboard.html`.
- The dashboard auto-refreshes every 3 seconds and shows per-window bars for focus, HWND capture, Robot capture, failures, and HWND keyboard.
- `MainWindowController` exposes a `统计 Dashboard` button that writes the latest dashboard and opens it with the OS browser.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 interaction metrics logging

Status: completed

Goal:

- Add log counters so the next five-window run can quantify how much focus switching remains after HWND capture and background Alt shortcuts.

Changed:

- Added `WindowInteractionMetricsService`.
- Focus attempts, capture provider results, and HWND keyboard shortcuts now emit cumulative `Interaction metrics` log lines per `windowId`.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 Alt+1 background input experiment

Status: completed

Goal:

- Add one more explicit debug experiment for `Alt+1` so the user can verify that number-key shortcuts also work through HWND background keyboard messages.

Changed:

- `WindowMessageInputExperimentService` now supports `postAlt1(...)`.
- `MainWindowController` now exposes a `后台按键 Alt+1` debug button beside `后台按键 Alt+Q`.

Validation:

- `mvn -q -DskipTests compile` passed.
- User tested `后台按键 Alt+1`; it opened the mini-map successfully, confirming number-key Alt shortcuts work through HWND messages on the tested client.

### He Li - 2026-05-22 bounded HWND keyboard integration

Status: completed

Goal:

- Promote the verified background Alt-key experiments into the normal input queue for the narrow safe case.
- Keep mouse and unverified keyboard shortcuts on the existing focus + real input path.

Owns:

- `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`
- `src/main/java/com/bot/dhxy/config/WindowIsolationProperties.java`
- `src/main/resources/application.properties`

Plan:

- Add a no-focus input transaction mode for pure background-keyboard sequences.
- Use HWND Alt shortcuts only when a queued request contains only supported `PRESS_ALT_*` actions and `SLEEP`.
- If HWND posting fails or is disabled, focus the bound window and fall back to the existing `InputProvider.pressAltQ()`.

Changed:

- Added `BoundWindowKeyboardService` for the verified HWND Alt shortcut path.
- Added `bot.window.hwnd-keyboard-enabled=true`.
- `InputActionWorker` now skips focus only for queued requests made solely of supported Alt shortcuts and `SLEEP`.
- Supported background shortcuts are `Alt+1`, `Alt+2`, `Alt+4`, `Alt+6`, `Alt+8`, `Alt+T`, `Alt+O`, `Alt+E`, and `Alt+Q`.
- If HWND posting is not attempted or fails, the worker focuses the bound window inside the active input transaction and uses the original real-input shortcut method.
- Mouse actions and mixed keyboard/mouse sequences still use the focused real-input path.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 HWND capture experiment follow-up

Status: active

Goal:

- Promote the successful per-HWND screenshot experiment into a reusable capture path.
- Reduce or remove the need to foreground/focus game windows before screenshots.
- Keep existing Robot screenshot behavior as a fallback until enough task paths are validated.

Owns:

- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `src/main/java/com/bot/dhxy/window/diagnostics/WindowCaptureExperimentService.java`
- planned capture provider/facade classes under `src/main/java/com/bot/dhxy/window/diagnostics` or `src/main/java/com/bot/dhxy/driver`
- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`

Avoids:

- Wuhuan business rules unless logs require it.
- `SummonSkillService.java` / `AutoBattleTask.java` while 谢帅 owns them.
- broad UI refactors in `MainWindowController.java`; only keep the existing debug button if needed.

Finding:

- User tested the new `后台截图实验` with two game windows overlapped, browser covering the game, and IntelliJ IDEA covering the game.
- Both `PrintWindow(PW_RENDERFULLCONTENT)` and `GetWindowDC + BitBlt` produced non-blank images for the selected bound HWNDs.
- The captured images preserved each window's own content and did not capture the covering browser/IDE/game window.
- This is a major architecture finding: for this client/machine, per-HWND capture can likely replace many `Robot` visible-screen screenshots and reduce five-window focus thrashing.

Planned direction:

- Extract the successful experiment code into a reusable bound-window capture service.
- Let `GameClientTracker.captureToMemory(...)` / `captureToFile(...)` prefer HWND capture when a current `WindowRuntimeContext.nativeBinding` exists.
- Convert absolute screen rects to window-relative rects by subtracting the tracked window base before cropping the HWND image.
- Keep Robot capture as fallback and log provider=`HWND_PRINTWINDOW` / provider=`HWND_BITBLT` / provider=`ROBOT`.
- Add a config switch before fully relying on the new provider.

Done:

- Added `BoundWindowCaptureService` as the reusable per-HWND provider.
- Added config switches:
  - `bot.window.hwnd-capture-enabled=true`
  - `bot.window.hwnd-capture-fallback-to-robot-enabled=true`
- `GameClientTracker.captureToMemory(...)` and `captureToFile(...)` now try HWND capture first when a bound window context is present.
- If HWND capture succeeds, the screenshot path no longer focuses/foregrounds the game window.
- If HWND capture fails, the old Robot screenshot path remains available as fallback.
- Capture logs now include `provider=HWND_PRINTWINDOW`, `provider=HWND_BITBLT`, `provider=ROBOT`, or `provider=HWND` for failed no-fallback cases.

Validation:

- `mvn -q -DskipTests compile` passed after the provider/tracker changes.

Open:

- Need decide whether the first production provider should use `PrintWindow`, `BitBlt`, or try `PrintWindow` then fallback to `BitBlt`.
- Need test minimized windows separately; covered windows worked, but minimized windows are a different case.

### He Li - 2026-05-22 background input message experiment

Status: completed

Goal:

- Test whether keyboard/mouse input can also be sent to a bound game HWND without foreground focus.
- Keep this as an explicit UI-triggered diagnostic, not automatic task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/window/diagnostics/WindowMessageInputExperimentService.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added a debug service that posts Win32 `WM_*` messages directly to selected HWNDs.
- Added UI debug buttons:
  - `后台按键 Alt+Q`
  - `后台鼠标中心左键`
  - `后台鼠标中心右键`
  - `子窗口中心右键`
- Each experiment saves before/after HWND screenshots under `images/temp/window_input_experiment`.
- The experiment does not use Robot/SendInput, does not move the physical cursor, and does not require foreground focus.
- `子窗口中心右键` first enumerates child HWNDs under the selected game window, logs class/title/rect, chooses the largest visible child, then posts right-click messages to that child.

Validation:

- `mvn -q -DskipTests compile` passed.
- `Alt+Q` experiment produced `posted=true`; before/after HWND screenshots showed the task panel opening, so background keyboard message worked on the tested client/window.
- Top-level `WM_LBUTTON` / `WM_RBUTTON` experiments produced `posted=true` but no visible game response, even when the game window was foreground and unobstructed.
- Child-window scan found a large visible `Win32Window` child matching the game render area. Posting right-click to that child also produced `posted=true` but no click response; before/after screenshots only showed normal animation/chat changes.

Open:

- Background keyboard messages are promising and can be tested per shortcut.
- Background mouse via normal `WM_MOUSE*` messages should be treated as unavailable for now; mouse clicks should continue using focus + serialized real input.

Open:

- Next five-window test should verify dialog accept/fallback clicks no longer land on another window, without the severe foreground flicker caused by broad capture focus.

### 何黎 - 2026-05-21 selected task semantics update

Goal:

- Keep `selectedTaskType` as the persistent configured/default task for a window.
- Prevent runtime queue/task execution from changing the selected/default task.

Changed:

- `WindowRuntimeContext.markQueued(...)` now updates only `lastTaskType` / status/message.
- `WindowRuntimeContext.markStarted(...)` now updates only `lastTaskType` / status/message/timestamps.
- Added a small runtime-event resolver so unknown runtime events still fall back to the configured selected task for display without mutating it.

Result:

- `WindowTaskSnapshot.getSelectedTaskType()` remains stable across queued task execution.
- Running/current task display continues to come from `RunningTaskHandle` / `runningTaskType`.
- Last task display continues to use `lastTaskType`.

### 何黎 - 2026-05-21 queue result separation update

Goal:

- Keep per-task result fields and per-queue result fields semantically separate.

Changed:

- `WindowRuntimeContext.markQueueFinished(...)` still updates window status, finish time, general `lastMessage`, and dedicated queue fields.
- It no longer writes queue-level result/message into `lastResult` / `lastResultMessage`.

Result:

- `WindowTaskSnapshot.getLastResult()` / `getLastResultMessage()` now describe the last concrete task event.
- `WindowTaskSnapshot.getLastQueueResult()` / `getLastQueueMessage()` describe the submitted queue as a whole.
- UI can safely show both without one overwriting the other.

### 何黎 - 2026-05-21 queue boundary scan

Goal:

- Scan queue start/stop/failure/snapshot boundaries after separating selected task, task result, and queue result semantics.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `src/main/java/com/bot/dhxy/window/control/*`
- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`

Avoids:

- 五环 business flow files unless logs require it.
- `SummonSkillService.java` / `AutoBattleTask.java`.
- large UI edits in `MainWindowController.java`.

Planned scan:

- Stop/cancel path: `WindowTaskRunner.stopCurrentTask()` and `RunningTaskHandle`.
- Failure path: task creation failure, task exception, `STOP_ON_FAILURE` / `CONTINUE_ON_FAILURE`.
- Snapshot path: running queue vs last queue vs last task display after task/queue completion.

Findings:

- `Future.cancel(true)` can mark the future done before the runner thread has fully exited. If `RunningTaskHandle.isRunning()` only checks `future.isDone()`, UI/scheduler can think the window accepts a new queue too early.

Changed:

- `RunningTaskHandle.isRunning()` now treats the handle as running while the runner thread is still alive, even if the future was cancelled.
- `WindowTaskRunner` now clears stale inactive handles through `getActiveTaskHandle()`.
- `runQueue(...)` only clears `currentTask` if it is still clearing the same handle, so a later handle cannot be accidentally erased.

Expected result:

- Stop/cancel no longer opens the window for another queue until the previous runner thread has really left its serialized task section.

### 谢帅: Summon Skill / Auto Battle

Status: active in this thread

Owns:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- summon skill deletion templates/config/service code
- auto-battle behavior and related config

Avoids:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`

If framework support is needed:

- Record the requested field/interface here.
- Let 何黎 or the user approve framework changes.

### 唐德: UI

Status: active or planned in another thread

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- UI table/button/status display
- displaying `WindowTaskSnapshot` fields
- future task queue UI controls

Avoids:

- changing task execution behavior;
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`;
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`;
- 五环 core logic.

If backend support is needed:

- Record the requested snapshot field/control API here.
- Let 何黎 add or approve framework-facing fields.

Recent UI status:

- `docs/DHXY_CONTEXT.md` notes that UI queue controls and table queue display may already have been added by the UI thread.
- Before editing UI again, inspect current `MainWindowController.java` and `git status`.

## Agent Start Template

Use this before starting a task:

```md
## Agent X - yyyy-MM-dd HH:mm

Status: active

Goal:
- ...

Owns:
- ...

Avoids:
- ...

Planned files:
- ...

Needs from others:
- none

### He Li - 2026-05-21 wuhuan transaction model cleanup

Status: active

Goal:

- Apply the settled transaction/yield model to Wuhuan.
- Stop treating story dialogs as Wuhuan advancement work.
- Keep Wuhuan option/give/task-panel chains atomic until they reach pathing, retry, finish, or failure.

Owns:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- summon/auto-battle owned files
- broad framework changes
- changing validated OCR/navigation business targets

Planned files:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-22 summon-skill clean cooldown fix

Status: completed

Done:

- Fixed AUTO_BATTLE summon-skill maintenance cooldown handling.
- `lastSummonSkillCleanAt` now updates only when `SummonSkillService.cleanSummonSkillsOnce()` returns true.
- Failed or incomplete summon-skill cleanup no longer consumes the long maintenance cooldown, so the next eligible idle/maintenance window can retry.

Changed files:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Later design should let broadcast maintenance preempt starting summon-skill cleanup, because team broadcast is higher priority than personal long-cycle maintenance.

Needs from others:

- none

### 谢帅 - 2026-05-22 auto-battle free patrol interval update

Status: completed

Done:

- Changed AUTO_BATTLE free-state patrol sleep to 3000ms inside `AutoBattleTask`.
- Kept `BattleRadarService.getDynamicPollingIntervalMs()` unchanged so Wuhuan/navigation paths are not affected.

Changed files:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Combat-state polling can be optimized further with an early-combat grace window, but that should be designed separately from the free-state broadcast patrol interval.

Needs from others:

- none

Done:

- `DialogHandleRequest.giveItemIfAvailable(...)` now ignores story dialogs instead of clicking through them.
- Wuhuan main loop no longer treats story dialogs as handled advancement work; story returns `STORY_IGNORED` and the loop continues toward task-panel P2/P1 advancement.
- Wuhuan unknown option dialogs without a give entry are treated as retryable abnormal UI: clean UI, set `needTaskSync=true`, and yield this loop.
- Removed the temporary Wuhuan "give item or story" direct path so the Wuhuan dialog transaction only owns option/give behavior.
- Added a thin task transaction layer: `TaskTransactionRunner`, `TaskTransactionResult`, `TaskYieldPolicy`, and `TaskTransactionOutcome`.
- Wuhuan initial accept, give-item, handover, task sync, and combined P2/P1 advancement now declare transaction names, expected results, and yield policies before running their exclusive input work.
- `TaskTransactionRunner` now also supports non-exclusive transactions for semantic chains that must not hold the input worker, such as preparation and post-combat maintenance.
- Wuhuan startup preparation is declared as `READY_TO_CONTINUE + CONTINUE_CHAIN`.
- Wuhuan post-combat recovery is declared as `READY_TO_CONTINUE + CONTINUE_CHAIN`, so heal/incense recovery remains part of the current chain and does not become a yield point.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-21 scan registration cleanup

Status: completed

Done:

- Found that the `roleA`/manual window row comes from manual/test registration defaults, not from team-role detection.
- Updated scan registration to prune idle manual registrations without native bindings before registering real scanned game windows.
- Running windows and native-bound game windows are left untouched.

Changed files:

- `src/main/java/com/bot/dhxy/window/discovery/GameWindowRegistrationService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- If a stale row has a native handle from a closed game window, this cleanup will not remove it yet.

Needs from others:

- none

### 谢帅 - 2026-05-21 auto-battle quiet flow audit update

Status: completed

Done:

- Audited the auto-battle input/action chain for excessive member-window operations.
- AutoBattleTask now calls `BattleRadarService.checkAndSyncCombatState(false)`, so free-state radar polling does not trigger extra first-aid checks.
- Post-combat first-aid remains handled by `AutoBattleTask.maybeHandleCombatExit(...)` only after an actual combat-exit signal.
- This keeps navigation/five-ring radar behavior unchanged while making member auto-battle quieter.

Changed files:

- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs repeat five-window observation to confirm member windows no longer do free-state health checks before any real combat exit.

Needs from others:

- none

### 谢帅 - 2026-05-21 auto-battle quiet member mode update

Status: completed

Done:

- Investigated the five-window five-ring test where member windows were reassigned to auto-battle but still pressed Alt+6 and switched windows too often.
- Root cause: reassigned AUTO_BATTLE still ran the generic task startup initializer, which performs map tracking setup and Alt+6 visibility preparation.
- Root cause: AUTO_BATTLE startup repeated team role detection even though the runner had just detected MEMBER for reassignment.
- Root cause: idle auto-battle lightweight cleanup ran every polling loop, causing frequent dialog scans and focus/input transactions.
- AUTO_BATTLE now skips the generic startup initializer.
- Runner now syncs detected LEADER/MEMBER into `WindowRuntimeContext`, so reassigned auto-battle can pass startup checks without another Alt+T team probe.
- Auto-battle idle lightweight cleanup is throttled by `auto-battle-ui-clean-interval-ms`.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs repeat five-window test: expected member windows should only do initial role probe, then enter quiet auto-battle loop without Alt+6 startup preparation and without rapid dialog-clean focus churn.

Needs from others:

- none

### 谢帅 - 2026-05-21 task startup team assignment update

Status: completed

Done:

- Added a task startup team assignment policy before `WindowTaskRunner` creates the actual task.
- When a window is asked to run five-ring and is clearly detected as MEMBER, the runner reassigns that window to AUTO_BATTLE.
- LEADER / SOLO / UNKNOWN currently keep the requested five-ring task, because five-ring can be run solo.
- Added a policy hook for future leader-only tasks such as 抓鬼 / 修罗: those can later reject SOLO and reassign MEMBER to auto-battle from the same place.

Changed files:

- `src/main/java/com/bot/dhxy/task/startup/TaskTeamAssignmentPolicy.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game queue validation: start five-ring on five windows; expected result is leader runs five-ring and four members are reassigned to auto-battle.

Needs from others:

- none

### 谢帅 - 2026-05-21 team-role auto-battle gate update

Status: completed

Done:

- Connected the validated team role detector to auto-battle startup checks.
- Auto battle now requires MEMBER by config; LEADER / SOLO / UNKNOWN skip when the gate is enabled.
- Five-ring startup no longer performs team role detection unless `five-ring-requires-leader` is enabled.
- Navigation lightweight cleanup is limited to the current window's AUTO_BATTLE task path, so five-ring navigation will not open the team panel only for cleanup gating.
- Reused the already-detected role inside startup checks instead of detecting twice.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/resources/application.yml`
- `docs/ACTIVE_WORK.md`

Validation:

- Five-window team-role debug test passed in logs: one LEADER and four MEMBER windows detected.
- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs one in-game auto-battle queue test: leader should skip auto battle, member windows should enter auto battle.

Needs from others:

- none

### Xie Shuai - 2026-05-21 team role hover short-circuit check

Status: completed

Done:

- Checked latest debug logs and confirmed one window stopped after hover because tooltip probe returned empty, so it never entered the Alt+T panel probe.
- Changed hover probing so a negative tooltip match no longer records an input dead-letter failure.
- Added explicit best-effort focus inside team-role hover and panel probes so the debug task can still work when startup Alt+6 visibility prep is skipped.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs another in-game debug run to verify the previously empty hover probe now sees the tooltip.

Needs from others:

- none
```

## Agent Update Template

Use this after finishing, pausing, or getting blocked:

```md
## Agent X - yyyy-MM-dd HH:mm update

Status: completed / paused / blocked

Done:
- ...

Changed files:
- ...

Validation:
- `mvn -q -DskipTests compile` passed / not run

Open issues:
- ...

Needs from others:
- none
```

## Active Log

### Tangde - 2026-05-22 15:04 update

Status: completed

Done:

- Adjusted the JavaFX main table checkbox column to better match `docs/DHXY_FLUENT_MOCK.html`.
- Reduced the selection column width from 34px to 28px.
- Reduced the checkbox visual size and styled it as a lightweight 12px control instead of the default large JavaFX checkbox.
- Kept real multi-select checkbox behavior from the previous update.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check the checkbox against the mock. If JavaFX checkbox styling still feels heavy, replace it with a custom tiny glyph button while preserving checkbox behavior.

### Tangde - 2026-05-22 14:56 update

Status: completed

Done:

- Replaced the main table's left `✓` selection indicator with a real checkbox column.
- Users can now select/unselect multiple windows by clicking the checkbox directly without needing Ctrl-click table selection.
- Checkbox changes keep existing selected windows selected unless that specific row is unchecked.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should verify checkbox multi-select behavior in the main control table.

### Tangde - 2026-05-22 14:49 update

Status: completed

Done:

- Fixed selected table row text becoming nearly invisible after clicking a row in the JavaFX main-control window table.
- Root cause: JavaFX selected table cells were still using the default selected text color while the custom selected background is light blue.
- Added CSS so selected row cells keep normal text color in both light and dark themes.

Changed files:

- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check row selection; selected rows should remain readable while still visibly selected.

### Tangde - 2026-05-22 14:42 update

Status: completed

Done:

- Improved selected-window visibility in the JavaFX main table.
- Added a narrow left selection indicator column that shows `✓` for selected rows.
- Strengthened selected row background and border color so `全选` is visually obvious.
- Added a selected-items listener to refresh the table selection indicator whenever multi-selection changes.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check whether selected rows are obvious enough; if still weak, switch the checkmark column to actual checkbox visuals.

### Tangde - 2026-05-21 14:31 update

Status: completed

Done:

- Fixed the blank filler area on the right side of the JavaFX main window table.
- Set the window table resize policy to constrained mode so columns fill the available table width instead of leaving a large empty white area after `操作`.
- This is display-only and does not change window/task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Re-check the table at normal width; if operation buttons become too spread out, cap the action column and add a lightweight status/message column instead.

### Tangde - 2026-05-21 14:25 update

Status: completed

Done:

- Compared the main-control page against `docs/DHXY_FLUENT_MOCK.html` after user pointed out missing row stop actions and hidden task selector.
- Added row-level stop action for problem/stopped rows, so the `操作` column keeps a visible stop affordance alongside retry/detail where appropriate.
- Reduced the default window table height from 340px to 260px and lowered its min height to 150px.
- Reduced task tile size from 82px to 76px and tightened task selector spacing so `任务选择` is visible in a normal-height console.
- Reduced shell minimum height from 760px to 640px and slightly tightened card/metric padding.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check the default console height. If task selector is still low, move it above the window table as a compact strip.

### Tangde - 2026-05-21 14:14 update

Status: completed

Done:

- Fixed the JavaFX pause/resume UI state bug reported by the user.
- Root cause: `WindowTaskSnapshot.isRunning()` can remain true after pause because the task thread is still alive, so paused windows were still rendered as pause-able instead of resume-able.
- Row actions now check `WindowRuntimeStatus.PAUSED` before `snapshot.isRunning()`, so paused rows show `▶` continue.
- The top bulk pause/resume button now uses status semantics: show `继续` when selected windows include paused windows and no selected window is `RUNNING` / `QUEUED` / `STOPPING`; mixed running+paused still shows `暂停`.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should retest pause then resume from the main UI; if status refresh is still delayed, add a short optimistic UI state after pause/resume command submission.

### Tangde - 2026-05-21 14:04 update

Status: completed

Done:

- Reworked `角色详情` again based on user feedback that opening details below the table felt disconnected from the row `详情` button.
- Detail now appears as a floating panel near the right side of the window table instead of a bottom drawer.
- The floating panel does not participate in the workbench left/right layout, so it does not squeeze table columns.
- Kept explicit `详情` open and `收起` close behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should check whether the floating panel feels natural enough; if it covers too much table content, next option is a compact row-expanded detail directly below the selected row.

### Tangde - 2026-05-21 13:55 update

Status: completed

Done:

- Changed `角色详情` from a narrow right-side panel into a bottom detail drawer under the window table.
- Opening details no longer reduces the window table width, so role/server/id/task columns stay readable.
- Detail content now uses the full main-panel width, giving long hwnd/message/title fields room to wrap naturally.
- Kept explicit `详情` open and `收起` close behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Re-check the runtime screenshot; if the bottom drawer takes too much vertical space, make it collapsible to a compact one-row summary plus an expanded detail body.

### Tangde - 2026-05-21 13:44 update

Status: completed

Done:

- Fixed the main-control layout issue shown in the user screenshot where the right detail panel squeezed toolbar buttons and table cells into `...`.
- The `角色详情` panel no longer opens automatically from normal table selection or bulk select.
- Row `详情` now explicitly opens the detail panel for that window, and the panel has a `收起` button to return space to the table.
- Added minimum widths for the main toolbar controls so `刷新` / `全选` / `启动` / `停止` are not compressed into ellipses.
- Reduced the main table column footprint and narrowed the detail panel from 340px to 300px to leave more room for the window list.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Re-check the actual UI screenshot/runtime layout; if text is still cramped, move the detail panel into a bottom drawer or floating inspector instead of keeping it in the right column.

### Tangde - 2026-05-21 13:32 update

Status: completed

Done:

- Tightened the JavaFX main shell and main-control layout toward `docs/DHXY_FLUENT_MOCK.html`.
- Matched the left sidebar width to the mock-style 184px layout and gave the shell a 760px minimum height with a softer container shadow.
- Removed obsolete top-tab CSS rules from `dhxy-fluent.css`.
- Added a title-row counter in the `窗口与任务` panel: `已选窗口：N`, matching the mock's panel-title information pattern.
- Renamed the JavaFX shell builder from `buildMainTabs()` to `buildMainShell()` to reflect that this is now a left-sidebar shell, not a top tab layout.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Continue comparing the running JavaFX main page against the HTML mock, then tune the toolbar density and task selector/detail panel proportions if the app still feels off.

### Tangde - 2026-05-21 13:24 update

Status: completed

Done:

- Changed the JavaFX shell navigation from top tabs to a left-side sidebar like `docs/DHXY_FLUENT_MOCK.html`.
- Sidebar entries now switch pages for main control, settings, authentication, debug, logs, and notes.
- Removed the old unused JavaFX `Tab` / `TabPane` references and dead `buildTab(...)` helper so the intended layout is unambiguous.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- pending after this update.

Next:

- Continue tightening main-control spacing against the HTML mock, especially sidebar proportions, workbench width, and detail-panel behavior.

### 唐德 - 2026-05-21 13:16

Status: active

Goal:

- Move JavaFX page navigation from top tabs to a left-side sidebar like the accepted HTML mock.
- Keep existing page content, but change shell/navigation layout.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 13:06

Status: active

Goal:

- Continue JavaFX main-control layout refinement.
- Tune table density, row coloring, and row action styling to better match the mock and reduce visual noise.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 13:09 update

Status: completed

Done:

- Tuned JavaFX main table visual density.
- Added fixed table row height and tighter cell padding.
- Softened status row background colors to reduce visual noise.
- Made row action buttons lighter and narrower, with hover color states.

Changed files:

- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed.

Open issues:

- Needs visual review in the running APP to see whether JavaFX table styling renders exactly as intended.

Needs from others:

- none

### 唐德 - 2026-05-21 12:54

Status: active

Goal:

- Continue JavaFX main-control layout refinement.
- Replace the right-side selected-window detail ListView with structured key/value detail rows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 13:00 update

Status: completed

Done:

- Reworked the JavaFX right-side `角色详情` panel from a ListView-style text log into structured key/value rows.
- Detail rows now show window, role, status, binding, current task, previous execution, recent task, end time, message, and native title.
- Added CSS for `detail-row`, `detail-key`, and `detail-value`.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.
- User should review whether all detail rows are needed or whether some should move behind an expanded view.

Needs from others:

- none

### 唐德 - 2026-05-21 12:42

Status: active

Goal:

- Bring the JavaFX main-control layout closer to the accepted HTML mock.
- Put the window toolbar and table into one left workbench panel, with the detail panel on the right and task selector below.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:46 update

Status: completed

Done:

- Adjusted the JavaFX main layout closer to the HTML mock.
- Moved the window toolbar, hint, and table into one left `窗口与任务` workbench panel.
- Kept the right-side selected-window detail panel beside that workbench.
- Left summary metrics above the workbench and task selector below it.
- Narrowed the right-side detail panel slightly to better match the mock proportions.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.
- Continue tuning right-detail content density and toolbar/table spacing after user tries this layout.

Needs from others:

- none

### 唐德 - 2026-05-21 12:31

Status: active

Goal:

- Make JavaFX main start auto-select the windows it is about to start when the user has no manual selection.
- Keep the UI selection aligned with the auto-discovered/auto-targeted windows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:35 update

Status: completed

Done:

- Updated JavaFX main start flow so auto-targeted windows become selected in the table.
- When the user clicks `启动` with no manual selection, the auto-discovered accepting windows are remembered.
- After the command returns and the table refreshes, those windows are selected in the UI.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.

Needs from others:

- none

### 唐德 - 2026-05-21 12:22

Status: active

Goal:

- Fix the JavaFX main `启动` button flow so it can be used without manually scanning windows first.
- The start button should be enabled when tasks are selected, auto-discover/register game windows, then start available windows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:25 update

Status: completed

Done:

- Fixed the JavaFX main `启动` button so it no longer requires selected windows.
- The start button is enabled whenever the task selector has at least one selected task.
- Clicking `启动` now logs that it is auto-refreshing/discovering game windows before starting.
- Existing `startMainSelectedTasks()` path already auto-registers detected game windows and starts selected windows, or all accepting windows when no windows are selected.
- Updated the no-selection hint to explain that start auto-refreshes windows.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.

Needs from others:

- none

### 唐德 - 2026-05-21 12:08

Status: active

Goal:

- Continue JavaFX main-control layout refinement.
- Replace verbose overview text with compact summary metrics and reduce main page visual clutter.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:14 update

Status: completed

Done:

- Continued JavaFX main-control layout refinement.
- Replaced the verbose `运行概览` text card with three compact summary metric cards:
  - `窗口`
  - `运行中`
  - `异常`
- Moved detailed registered/selected/visible/accepting/binding information into the lighter operation hint text.
- Added CSS for summary metric cards.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- Verified summary metric UI code and CSS are present.

Open issues:

- Full Maven compile still needs to be rerun after unrelated non-UI compile errors in active backend files are resolved.
- Next main-layout pass should look at right-side detail panel density and table/action spacing.

Needs from others:

- none

### 唐德 - 2026-05-21 11:55

Status: active

Goal:

- Refine mixed selected-window pause/resume behavior in the JavaFX main toolbar.
- Mixed running+paused selection should show pause, not resume.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:59 update

Status: completed

Done:

- Refined selected-window pause/resume behavior for mixed selections.
- If selected windows include any running window, the top bulk action shows `暂停` and calls pause.
- The button shows `继续` only when the selected set has paused windows and no running windows.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` is currently blocked by unrelated non-UI compile errors in `PlayerStateService.java` and `NavigationService.java`.

Open issues:

- Re-run full Maven compile after the other active backend/thread changes restore compile.

Needs from others:

- none

### 唐德 - 2026-05-21 11:45

Status: active

Goal:

- Fix the JavaFX main toolbar selected-window pause button so it can resume paused windows.
- Make the bulk pause/resume control reflect selected window state.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:49 update

Status: completed

Done:

- Fixed the JavaFX main toolbar selected-window pause control.
- The top bulk pause button now changes to `继续` when any selected window is paused.
- Clicking that same button now calls `resumeWindows(...)` for selected paused windows; otherwise it calls `pauseWindows(...)`.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed after Maven network/plugin resolution was allowed.
- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- If mixed selected windows include both running and paused windows, the current top action prioritizes `继续` when any selected window is paused. We can refine that if the mixed-state behavior feels wrong in use.

Needs from others:

- none

### 唐德 - 2026-05-21 11:22

Status: active

Goal:

- Translate the accepted main-control mock into the JavaFX app.
- Focus on the usable main page first: compact toolbar, window table row actions, right detail panel, and quiet square task selector.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files unless a compile issue exposes a narrow UI API mismatch
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:36 update

Status: completed

Done:

- Translated the accepted main-control mock into the JavaFX `MainWindowController`.
- Main page now uses a compact toolbar with refresh/filter/search, selected-window pause/stop, selection reset, select-all, and primary start.
- Added search filtering by role, server, player id, native title, or window id.
- Simplified the window table to role/server/id/status/running task/progress/actions.
- Added per-row action controls for start/resume, pause, stop, retry, and detail selection.
- Replaced the old visible queue builder in the main page with quiet square task tiles backed by the existing `pendingTaskQueue`.
- Main `启动` now performs automatic game-window discovery first, then starts the selected task queue on selected windows, or on accepting windows when none are selected.
- Added CSS for task tiles, row actions, toolbar, and stronger start/stop actions.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed after Maven network/plugin resolution was allowed.
- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- Verified the new JavaFX task selector, row actions, search filter, and auto-discovery start path are present.

Open issues:

- Needs hands-on UI review in the running APP to tune spacing, right-side detail width, and task tile density.
- Task count badges are visible as summaries in JavaFX, but the clickable count popover from the HTML mock is not implemented yet.

Needs from others:

- none

### 唐德 - 2026-05-21 11:12

Status: active

Goal:

- Remove native number spinner controls from the task count popover input in the main mock.
- Keep +/- buttons as the only stepper controls.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:14 update

Status: completed

Done:

- Removed native browser spinner controls from the task count popover number input.
- Kept the external `-` / `+` buttons as the only visible stepper controls.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified spinner-hiding CSS for WebKit and Firefox-style number inputs is present.

Open issues:

- none

Needs from others:

- none

### 唐德 - 2026-05-21 11:00

Status: active

Goal:

- Try interactive task-count badges in the main mock.
- Clicking a task count badge should show a lightweight count editing popover.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:05 update

Status: completed

Done:

- Made main-page task count badges visually clickable in the mock.
- Clicking a count badge now opens a lightweight count popover.
- The popover shows task name, stepper controls, unit text, apply, and cancel.
- Count badge clicks stop propagation so they do not toggle task selection.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `task-count`, `count-popover`, and popover JS handlers are present.

Open issues:

- User should review whether the popover belongs inline below the task grid or should appear closer to the clicked badge in the final JavaFX implementation.

Needs from others:

- none

### 唐德 - 2026-05-21 10:48

Status: active

Goal:

- Add a lightweight task-count shortcut/summary to the main page mock.
- Keep full task count configuration in Settings, but make counts visible near task selection.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 10:52 update

Status: completed

Done:

- Added a lightweight task-count summary to the main task selector.
- Each task tile now shows a small count badge such as `1轮`, `60分`, `3轮`, or `按需`.
- Added a subtle `次数` shortcut button near the task selection heading.
- Kept the full editable task-count form in the Settings tab.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `task-count` badges and the `次数` shortcut are present.

Open issues:

- User should review whether counts belong inside task tiles, in a separate compact row, or only behind the `次数` shortcut.

Needs from others:

- none

### 唐德 - 2026-05-21 10:34

Status: active

Goal:

- Replace the placeholder settings tab in the main mock with direct, visible configuration groups.
- Include supply settings, summon skill settings, task count settings, and run safety settings without nested dialogs.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 10:40 update

Status: completed

Done:

- Replaced the placeholder settings tab in the main mock with direct visible configuration groups.
- Added `补给设置` with character/summon HP/MP thresholds and post-combat supply switches.
- Added `召唤兽技能` with third-skill maintenance, interval, retry, strategy, and timing controls.
- Added `任务次数` with 五环 / 抓鬼 / 修罗 / 自动战斗 counts.
- Added `运行安全` with startup auto-refresh, skip-running, default idle selection, exception handling, and emergency hotkey.
- Removed abstract placeholder blocks such as `窗口注册`, `任务默认值`, and `窗口行为`.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified the new settings group headings are present and old abstract settings headings are gone.

Open issues:

- User is still deciding whether task counts belong only in settings or also need a lightweight main-page shortcut/summary.

Needs from others:

- none

### 唐德 - 2026-05-21 10:22

Status: active

Goal:

- Clean up the main mock table and task selector after user review.
- Remove the redundant result column from the window table.
- Remove the duplicate start button from the task selector panel and simplify task selection reset wording.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team role debug startup skip

Status: active

Goal:

- Make `队伍识别测试` faster and cleaner for repeated testing by skipping general window startup preparation.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Reason for touching high-conflict file:

- `WindowTaskRunner` owns the universal Alt+6 visibility preparation, so skipping it for one debug task requires a small targeted guard there.

Planned:

- Skip map-tracking startup initializer for `debug_team_role`.
- Skip Alt+6 visibility preparation for `debug_team_role`.
- Do not change normal 五环 / 自动战斗 startup behavior.

### 谢帅 - 2026-05-21 team role debug startup skip update

Status: completed

Done:

- `DefaultWindowTaskStartupInitializer` now skips map-tracking startup init for `debug_team_role`.
- `WindowTaskRunner` now skips the universal Alt+6 visibility preparation for `debug_team_role`.
- Normal 五环 / 自动战斗 startup behavior is unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs repeated two-window `队伍识别测试` runs to confirm the test is faster and remains attempt-1 stable.

### 唐德 - 2026-05-21 10:25 update

Status: completed

Done:

- Removed the `结果` column from the main mock window table.
- Kept current window state in the table and left historical result information for the right-side detail panel.
- Removed the duplicate `启动` button from the task selector panel.
- Changed the task selector reset action to `清空任务选择`.
- Kept the only task start entry in the top bulk action area.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `结果`, `SUCCESS`, `FAILED`, and `取消任务选择` are gone from the main table/task selector area.

Open issues:

- User should review whether the task selector panel needs any action button at all besides `清空任务选择`.

Needs from others:

- none

### 唐德 - 2026-05-21 10:12

Status: active

Goal:

- Reduce top toolbar density in the main Fluent mock.
- Shorten filter/search widths and make the bulk stop action match the start action's visual weight.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team panel exclusive probe

Status: active

Goal:

- Test whether the leader first-attempt miss is caused by another window taking focus/foreground between Alt+T open and panel screenshot.

Owns:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Hypothesis:

- Current panel probing serializes each input action, but the whole open-wait-capture-close sequence is not one exclusive transaction.
- Because screenshots use Robot screen pixels, another window can cover or change foreground before the panel crop.

Planned:

- Wrap `Alt+T -> wait -> transfer/member screenshots -> Alt+T close` in one `submitExclusiveAndWait(...)` callback.
- Use direct `InputProvider` calls inside the exclusive callback to avoid queue-in-queue deadlock.

### 谢帅 - 2026-05-21 team panel exclusive probe update

Status: completed

Done:

- `TeamRoleDetectionService` now wraps `Alt+T -> wait -> transfer/member screenshots -> Alt+T close` in a single exclusive input queue callback.
- Inside the exclusive callback it uses direct `InputProvider.pressAltT()` rather than nested `InputSequences` calls.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs another two-window `队伍识别测试` run. If leader still needs attempt 2, the remaining cause is likely Alt+T toggle state / panel-open detection rather than cross-window foreground interference.

### 唐德 - 2026-05-21 10:15 update

Status: completed

Done:

- Reduced the top toolbar density in the main mock.
- Shortened the filter dropdown column and search input width.
- Made the bulk stop action match the primary start action size/weight.
- Shortened `停止选中` to `停止` while keeping it red and grouped with selected-window actions.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified compact toolbar widths and `bulk-danger-action` styling are present.

Open issues:

- User should review whether `停止` is clear enough in context or should return to `停止选中`.

Needs from others:

- none

### 唐德 - 2026-05-21 10:02

Status: active

Goal:

- Clarify the bulk selection reset wording in the main mock.
- Make the primary start action more visually prominent and easier to click.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 10:05 update

Status: completed

Done:

- Renamed bulk `清空` to `取消选择` in the main mock.
- Renamed task selector `清空任务选择` to `取消任务选择`.
- Made the primary `启动` button wider, taller, and visually stronger with a subtle shadow.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified selection reset wording and stronger start button styling are present.

Open issues:

- The logs tab still has a separate `清空` button for clearing logs; that is unrelated to window selection.

Needs from others:

- none

### 唐德 - 2026-05-21 09:52

Status: active

Goal:

- Refine the main mock bulk action order and wording.
- Put `全选` next to the primary start action, shorten start wording, and separate stop from the start cluster.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 09:56 update

Status: completed

Done:

- Reordered the main mock bulk action cluster.
- Put `全选` directly next to the primary `启动` button.
- Shortened start buttons from `启动所选任务` to `启动`.
- Renamed stop to `停止选中` to make clear it is not global stop-all.
- Separated stop/clear from the start cluster with a subtle divider.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified old `启动所选任务` / `停止所选` labels are gone from the mock.

Open issues:

- User should review whether `停止选中` belongs in this toolbar or should move to another location later.

Needs from others:

- none

### 唐德 - 2026-05-21 09:42

Status: active

Goal:

- Correct the main mock toolbar/action icon direction after user feedback.
- Restore top batch window controls to text buttons and fix row pause/stop icon proportions.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team panel delay tune

Status: active

Goal:

- Tune team panel open delay after logs showed the first leader probe captured before the panel was rendered.

Owns:

- `src/main/resources/application.yml`
- `docs/ACTIVE_WORK.md`

Avoids:

- Team role algorithm changes beyond this delay.

Planned:

- Increase `bot.team.team-panel-open-delay-ms` from 500 to 800.

### 谢帅 - 2026-05-21 team panel delay tune update

Status: completed

Done:

- Increased `bot.team.team-panel-open-delay-ms` from 500 to 800.

Changed files:

- `src/main/resources/application.yml`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs one more in-game team-role debug run to confirm leader probe no longer needs the second retry.

### 唐德 - 2026-05-21 09:45 update

Status: completed

Done:

- Restored top batch controls to text buttons: `暂停所选` and `停止所选`.
- Removed unused square/circular toolbar icon-button styling from the mock.
- Replaced row pause/stop symbols with better-proportioned `⏸` and `⏹`.
- Kept `详情` as text.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `icon-button`, `toolbar-divider`, old `Ⅱ`, and old `■` are gone.

Open issues:

- User should review whether row icons should remain as symbols or be replaced later by real icon assets/library icons in JavaFX.

Needs from others:

- none

### 唐德 - 2026-05-21 09:30

Status: active

Goal:

- Fix ugly row action icon styling in the main mock.
- Remove square icon-button framing from row actions and restore details as text.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 09:34 update

Status: completed

Done:

- Removed the ugly square framing from row action icons in the main mock.
- Restored `详情` as a text action instead of an icon.
- Row start/pause/retry actions are now lightweight frameless icon actions.
- Row stop action is now a subtle circular danger action.
- Batch pause/stop buttons now use circular framing instead of square boxes.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `ⓘ` is gone and row actions use `detail-action` / `stop-action`.

Open issues:

- User should review whether row actions should be even quieter or whether some actions should return to text.

Needs from others:

- none

### 唐德 - 2026-05-21 09:18

Status: active

Goal:

- Reduce clutter in the main mock window toolbar.
- Change per-row operations from text buttons to compact icon buttons for start/pause/resume/stop/detail.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 09:22 update

Status: completed

Done:

- Reduced top toolbar clutter in the main Fluent mock.
- Split toolbar layout into left-side refresh/filter/search and right-side compact batch actions.
- Shortened batch selection buttons to `全选` / `清空`.
- Changed batch pause/stop to icon buttons with titles.
- Changed per-row operations to compact icon buttons:
  - `▶` for start/resume;
  - `Ⅱ` for pause;
  - `■` for stop;
  - `↻` for retry;
  - `ⓘ` for details.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified icon-button and row-action markup is present and old top-bar labels are absent.

Open issues:

- User should review whether the top toolbar still feels too dense and whether the icon choices are clear enough.

Needs from others:

- none

### 唐德 - 2026-05-21 09:05

Status: active

Goal:

- Update the main Fluent mock top window toolbar and table actions based on the latest UI decision.
- Keep filtering/search separate from selection, expose select-current-list, and move single-window actions into each row.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team role UI shortcut

Status: active

Goal:

- Add a direct selected-window team-role test button because the current task / queue UI is too unclear for quick in-game validation.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Backend task execution changes.
- Broad UI redesign.

Planned:

- Add a single button that starts `TaskType.DEBUG_TEAM_ROLE` on the selected windows.
- Keep the existing queue and selected-task behavior unchanged.

### 谢帅 - 2026-05-21 team role UI shortcut update

Status: completed

Done:

- Added a direct `队伍识别测试` button in the main task-control row.
- The button starts `TaskType.DEBUG_TEAM_ROLE` on the selected windows through the existing `WindowTaskStartRequest.sameTask(...)` path.
- Existing current-task, selected-task, and queue behavior is unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The broader task/queue UI is still confusing and should be simplified by 唐德 later, but the team-role test now has a direct path.

### 唐德 - 2026-05-21 09:10 update

Status: completed

Done:

- Updated the main Fluent mock window toolbar:
  - kept refresh;
  - added status filter dropdown;
  - added search by role / ID / server;
  - exposed `全选当前列表` and `清空选择`;
  - kept batch `暂停所选` / `停止所选` / `启动所选任务`.
- Removed the vague `窗口操作`, `启动当前任务`, and `启动已选任务` top-bar actions from the mock.
- Added per-row window operations in the table:
  - running row: pause / stop / detail;
  - idle row: start / detail;
  - paused row: resume / stop / detail;
  - problem row: retry / detail.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified old top-bar action labels are gone and row action controls are present.

Open issues:

- User should review whether the toolbar density and row action column feel right before translating to JavaFX.

Needs from others:

- none

### 唐德 - 2026-05-21 08:52

Status: active

Goal:

- Finalize task selector direction as option A in the main mock.
- Remove the temporary C/A comparison from the main mock and keep only the quiet square task-tile selector.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files until the mock direction is confirmed enough to translate
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:57 update

Status: completed

Done:

- Finalized the main mock task selector direction as option A.
- Removed the temporary C/A comparison from the main Fluent mock.
- Main mock now keeps only quiet square task tiles with click-order badges.
- Noted runtime guidance: JavaFX should keep tile clicks local/lightweight and only call backend when the user presses start.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified C comparison text/styles are gone from the main mock and `task-card-grid` remains.

Open issues:

- Next UI implementation should translate A into JavaFX using lightweight local selection state first, then submit selected tasks only from the start action.

Needs from others:

- none

### 唐德 - 2026-05-21 08:40

Status: active

Goal:

- Put task selector options A and C into the main Fluent HTML mock for in-context comparison.
- Keep this design-only and do not modify JavaFX or backend code.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:45 update

Status: completed

Done:

- Integrated task selector options C and A into the main Fluent mock for in-context comparison.
- Left side shows C: compact checkbox-list style.
- Right side shows A: quiet square task-tile style.
- Kept the comparison design-only and did not touch JavaFX or backend files.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `C. 紧凑勾选列表`, `A. 安静小方块`, and comparison layout classes are present.
- Verified A no longer has inner checkbox pseudo-element styling in the main mock.

Open issues:

- User should pick A or C after viewing them inside the main mock context.

Needs from others:

- none

### 唐德 - 2026-05-21 08:30

Status: active

Goal:

- Refine option A in the standalone task selector mock.
- Remove the inner checkbox square from A so each task uses only one square container.

Owns:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- `docs/DHXY_FLUENT_MOCK.html` unless the user picks a final direction
- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:32 update

Status: completed

Done:

- Refined option A in the standalone task selector mock.
- Removed the inner checkbox square from A task tiles.
- Option A now uses one square container only, with selected state shown by border and order badge.

Changed files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_TASK_SELECTOR_OPTIONS.html docs\ACTIVE_WORK.md` passed.
- Verified A no longer has `task-square::before` checkbox styling.

Open issues:

- User should review whether A now feels cleaner or still needs a different selected-state cue.

Needs from others:

- none

### 唐德 - 2026-05-21 08:18

Status: active

Goal:

- Create a standalone task-selector design comparison mock with options A/B/C.
- Keep the main HTML mock unchanged while comparing task selection UI directions.

Owns:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:23 update

Status: completed

Done:

- Added a standalone task selector comparison mock with options A/B/C.
- Option A shows quiet square task tiles.
- Option B shows icon-style task tiles.
- Option C shows compact checkbox-list selection.
- Kept this separate from the main fluent mock so layout discussion stays focused.

Changed files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_TASK_SELECTOR_OPTIONS.html docs\ACTIVE_WORK.md` passed.
- Verified all three option headings are present in the HTML.

Open issues:

- User should choose A/B/C direction before integrating the selected pattern into the main mock.

Needs from others:

- none

### 唐德 - 2026-05-21 08:05

Status: active

Goal:

- Quiet down the compact task selector in the HTML mock.
- Change task tiles from narrow rectangles to fixed square tiles with subtler selected state.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:08 update

Status: completed

Done:

- Changed compact task items from narrow rectangles into fixed 82px square tiles.
- Reduced selected-state noise by removing the large blue fill.
- Kept a subtle selected border, small checkbox marker, and numbered order badge.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified square task tile CSS is present.

Open issues:

- User should review whether the square tile size and selected-state contrast feel right.

Needs from others:

- none

### 唐德 - 2026-05-21 07:55

Status: active

Goal:

- Adjust the HTML mock task selector from large task cards to compact checkbox-like task tiles.
- Keep selected execution order badges, but make each task item small enough for several per row.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team role debug follow-up

Status: active

Goal:

- Fix the team-role one-click debug path after logs showed concurrent hover screenshots could be affected by another window's queued mouse move.

Owns:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `images/template/team/member_marker.png`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 core flow.
- UI/controller changes unless the user asks.

Planned:

- Keep hover, delay, and tooltip screenshot in one exclusive input queue callback.
- Replace the too-small member marker template with the actual captured member marker area.

### 谢帅 - 2026-05-21 team role debug follow-up update

Status: completed

Done:

- `TeamRoleDetectionService` now performs hover, hover delay, and tooltip capture inside one exclusive input queue callback, so another window cannot move the mouse away before the tooltip screenshot.
- Tooltip logs now include the randomized hover point.
- Replaced `images/template/team/member_marker.png` with the actual captured member marker region (`暂时`) from the failing member window.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `images/template/team/member_marker.png`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java docs/ACTIVE_WORK.md` passed with only CRLF warning.

Open issues:

- Needs one more in-game debug run on both windows to confirm the member path now reports `MEMBER` instead of `UNKNOWN`.

### 唐德 - 2026-05-21 07:58 update

Status: completed

Done:

- Changed the HTML mock task selector from large cards to compact checkbox-like task tiles.
- Each task tile is now small enough for several items in one panel row.
- Kept selected order badges and automatic order rerendering.
- Added two extra example task tiles so the density is visible in the mock.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified compact tile CSS and six visible task options are present.

Open issues:

- User should review whether this compact density is right before translating the pattern back to JavaFX.

Needs from others:

- none

### 唐德 - 2026-05-21 07:35

Status: active

Goal:

- Update the HTML mock so the main task area uses task selection cards instead of queue terminology.
- Show selected task execution order with small number badges on cards.
- Keep this design-only; do not touch JavaFX implementation.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 07:45 update

Status: completed

Done:

- Replaced the main mock's queue strip with task selection cards.
- Reduced the top summary to three clear metrics: window count, running count, and exception count.
- Added click-order badges on task cards so selected tasks show execution order.
- Added clear selection behavior and updated visible actions to `启动所选任务` / `清空任务选择`.
- Removed the visible `待提交队列` / `加入任务` / `队列操作` wording from the HTML mock.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- Verified the HTML mock contains `task-card`, `toggleTask`, and `启动所选任务`.
- Verified the HTML mock no longer contains `待提交队列`, `加入任务`, `队列操作`, or `启动队列`.

Open issues:

- After user review, decide the exact task-card names and whether execution order adjustment needs drag/drop or small arrow controls later.

Needs from others:

- none

### 唐德 - 2026-05-21 07:15

Status: active

Goal:

- Switch UI workflow from direct JavaFX tweaking to a standalone mock-view design phase.
- Create an HTML/CSS mock for the selected E / Windows 11 Fluent Light direction.
- Use the mock to discuss overall layout, panels, and control hierarchy before further JavaFX implementation.

Owns:

- `docs/ui-mockups/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase unless a tiny documentation link is needed
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 07:15 update

Status: completed

Done:

- Switched UI work to a standalone mock-view phase instead of continuing direct JavaFX layout tweaks.
- Added `docs/DHXY_FLUENT_MOCK.html` as a single-file HTML/CSS mock for the selected E / Windows 11 Fluent Light direction.
- The mock includes:
  - left navigation shell;
  - light/dark theme toggle;
  - `主控`, `设置`, `验证`, `调试`, `日志`, `说明` sections;
  - main workbench with summary metrics, window table, right-side role detail panel, and queue strip;
  - settings/debug/log placeholder panels for discussion.
- This file is design-only and does not affect JavaFX runtime or backend behavior.
- Attempted to create `docs/ui-mockups/`, but Windows returned access denied for creating that subdirectory; used `docs/DHXY_FLUENT_MOCK.html` instead.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs/DHXY_FLUENT_MOCK.html docs/ACTIVE_WORK.md` passed.
- Verified the mock contains all six planned sections and the theme toggle script.

Open issues:

- Need user review of the mock at the layout/panel level before translating any of it back into JavaFX.
- If desired later, move mock files into a dedicated folder after the directory creation permission issue is resolved.

Needs from others:

- none

### 唐德 - 2026-05-21 06:55

Status: active

Goal:

- Reduce button clutter in the JavaFX main control tab.
- Collapse low-frequency window selection/management/runtime actions into menus.
- Keep backend behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 06:55 update

Status: completed

Done:

- Reduced main-tab button clutter by collapsing low-frequency actions into menus.
- Replaced many always-visible window buttons with three menus:
  - `选择窗口`: all/running/idle/problem/bound/unbound/clear selection;
  - `窗口管理`: unregister selected/all;
  - `运行控制`: pause/resume selected/all and stop selected/all.
- Kept high-frequency actions visible:
  - refresh;
  - filter;
  - start current task;
  - start selected task.
- Menu items now follow selection-based disabled states for selected-window operations.
- Backend behavior remains unchanged; menu actions reuse the existing control-service calls.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Queue controls are still expanded. If they feel crowded in actual use, the next UI pass can collapse queue reorder/clear/presets into a `队列操作` menu.

Needs from others:

- none

### 唐德 - 2026-05-21 06:35

Status: active

Goal:

- Move the main tab closer to the selected E layout with a center table and right-side detail panel.
- Hide the right-side detail panel when no window is selected.
- Add light/dark theme switching without changing backend behavior.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 06:35 update

Status: completed

Done:

- Moved the main tab closer to the selected E / Windows 11 Fluent layout:
  - top controls stay above the work area;
  - window table is the center workbench;
  - selected-window detail is a right-side panel.
- The right-side selected-window detail panel now hides when no window is selected and no longer shows an empty placeholder.
- Added a `深色模式` toggle in the top bar.
- Added dark-theme CSS variables and overrides in `dhxy-fluent.css`.
- Kept existing pause/resume UI buttons from the framework pause work intact.
- Backend task behavior remains unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java src/main/resources/styles/dhxy-fluent.css docs/ACTIVE_WORK.md` passed with only CRLF warnings on Java files.
- `mvn -q -DskipTests compile` is currently blocked by unrelated in-progress 五环 pause work:
  - `FiveRingTask.java` calls `checkpoint(TaskExecutionContext)`;
  - no matching method is currently available in the scanned task/runner/window files.

Open issues:

- Dark-mode rendering needs a real JavaFX window check; CSS is present but visual contrast should be verified manually.
- Compile should be rerun after the framework/FiveRing pause checkpoint work is completed.

Needs from others:

- 何黎 / framework pause owner: finish or expose the missing `checkpoint(TaskExecutionContext)` support used by `FiveRingTask.java`.

### 何黎 - 2026-05-21 pause safe-point control

Status: active

Goal:

- Add first-version per-window task pause/resume.
- Pause should happen at task safe checkpoints, not by stopping the global input worker.
- Already-submitted physical input sequences are allowed to finish naturally.

Owns:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `src/main/java/com/bot/dhxy/runner/stop/TaskPauseToken.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/model/WindowRuntimeStatus.java`
- small pause/resume button wiring in `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 business logic
- `SummonSkillService.java`
- `AutoBattleTask.java`
- broad UI layout or styling changes

Planned files:

- same as Owns.

Needs from others:

- none

### 何黎 - 2026-05-21 pause safe-point control update

Status: completed

Done:

- Added cooperative per-window task pause/resume.
- Pause is stored on the active `RunningTaskHandle` through `TaskPauseToken`.
- Existing task stop checkpoints now also wait while paused via `TaskExecutionContext.throwIfStopRequested()`.
- Stop still wakes and interrupts a paused task.
- Added backend control APIs for selected/all pause and resume.
- Added small UI buttons for selected/all pause and resume without changing broad UI layout.

Changed files:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `src/main/java/com/bot/dhxy/runner/stop/TaskPauseToken.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/model/WindowRuntimeStatus.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- This is a safe-point pause. If the current task is inside a long input sequence or a loop without `executionContext.throwIfStopRequested()`, it will pause after that section reaches the next checkpoint.
- After a follow-up scan, 五环 received extra checkpoints around prepare, initial navigation/NPC click, post-combat supply, dialog handling, task sync, and P2/P1 pathing triggers.
- User test showed pause requests arrived, but the 摄妖香补给 path kept scanning bag pages because `PlayerStateService.ensureSheYaoXiangActiveForLeaderTask(...)` called `BagService` without `TaskExecutionContext`.
- Fixed by adding context-aware `PlayerStateService` overloads and wiring 五环/AutoBattle post-combat calls through them.
- `TaskPauseToken` now logs when a pause checkpoint is reached and resumed.

Needs from others:

- none

### 唐德 - 2026-05-21 06:15

Status: active

Goal:

- Start applying the selected E / Windows 11 Fluent Light visual direction.
- Add a JavaFX stylesheet and migrate controller inline styling toward reusable style classes.
- Keep backend behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowService.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowService.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 06:15 update

Status: completed

Done:

- Started applying the selected E / Windows 11 Fluent Light visual direction.
- Added a reusable JavaFX stylesheet:
  - `src/main/resources/styles/dhxy-fluent.css`
- `MainWindowService` now loads `/styles/dhxy-fluent.css` into the JavaFX `Scene`.
- Increased initial window size from `980x640` to `1120x720` to better fit the tabbed console layout.
- Migrated UI styling away from inline JavaFX style strings toward reusable style classes:
  - root/top bar/tab content;
  - tab pane;
  - section cards and titles;
  - primary/secondary/danger buttons;
  - status/hint/queue summary text;
  - table/list/log styling;
  - row status styles for running/accepting/stopped/error windows.
- Backend task behavior remains unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowService.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java src/main/java/com/bot/dhxy/ui/MainWindowService.java src/main/resources/styles/dhxy-fluent.css docs/ACTIVE_WORK.md` passed with only CRLF warnings on Java files.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Visual result still needs real JavaFX window inspection; CSS compiles as a resource but JavaFX runtime rendering should be checked manually.
- Later polish can move more layout spacing into CSS and split `MainWindowController` into tab-specific components.

Needs from others:

- none

### 唐德 - 2026-05-21 05:50

Status: active

Goal:

- Fill missing functional UI tabs before visual polish.
- Add separate log and diagnostics/debug tabs.
- Keep implementation UI-only and reuse existing task/queue paths.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 02:05 update

Status: completed

Done:

- Added `DebugTeamRoleTask`, a one-shot task for testing backend team-role detection on the selected/bound window.
- Added `TaskType.DEBUG_TEAM_ROLE`, so the task appears in existing task selectors/queues.
- Registered the task in `DefaultTaskFactory`.
- Added `TeamRoleDetectionService.detectCurrentRoleForDebug(...)`, which intentionally bypasses `roleDetectionEnabled` for manual debug runs only.

Changed files:

- `src/main/java/com/bot/dhxy/task/DebugTeamRoleTask.java`
- `src/main/java/com/bot/dhxy/task/model/TaskType.java`
- `src/main/java/com/bot/dhxy/task/DefaultTaskFactory.java`
- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The debug task is available through the existing task selector/start flow. A dedicated one-click UI button was not added yet because `MainWindowController.java` has high churn/encoding-fragile text in this area.

Needs from others:

- none

### 唐德 - 2026-05-21 05:50 update

Status: completed

Done:

- Added missing functional UI tabs before visual polish:
  - `调试`: task diagnostics/debug entry points;
  - `日志`: window command/UI operation logs.
- Moved command log display from the bottom area into the `日志` tab so it no longer crowds the main window table.
- Added initial diagnostics controls:
  - set current task selectors to `坐标调试`;
  - add `坐标调试` to the pending queue.
- Added diagnostics notes for important log files:
  - `logs/dhxy-console.log`;
  - `logs/tracker-coordinate.log`.
- Updated the `说明` tab to describe `调试` and `日志`.
- Backend behavior remains unchanged; debug controls reuse existing task selection/queue paths.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- `调试` tab currently exposes only coordinate-debug helpers and placeholders for NPC first-shot, screenshot/OCR, and template matching tools.
- `验证` tab remains a placeholder until captcha/authentication behavior exists.

Needs from others:

- none

### 唐德 - 2026-05-21 05:30

Status: active

Goal:

- Correct the UI layout direction after user feedback.
- Remove whole-page scrolling and split the JavaFX UI into tabs.
- Keep scrolling only inside detail/list controls such as selected-window details.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 05:30 update

Status: completed

Done:

- Corrected the UI layout direction based on user feedback.
- Removed the whole-page `ScrollPane` from the central window-control area.
- Added a `TabPane` with four tabs:
  - `主控`: day-to-day window selection, task control, window table, selected-window detail, and task queue;
  - `设置`: window registration/scanning and supply configuration;
  - `验证`: placeholder for future captcha/authentication workflows;
  - `说明`: short explanation of the tab layout and current UI responsibilities.
- Kept scrolling local to list/detail controls such as the selected-window detail list and logs.
- Removed leftover registration/discovery rows from the main tab so JavaFX controls are not attached to duplicate parents.
- Backend task behavior remains unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- The `验证` tab is currently a placeholder until the captcha/authentication flow exists.
- Main-tab spacing should be checked in the actual JavaFX window, especially on smaller resolutions.

Needs from others:

- none

### 唐德 - 2026-05-21 05:05

Status: active

Goal:

- Do a broader UI-only cleanup pass after scanning current JavaFX UI and snapshot fields.
- Improve status hierarchy, table readability, selected-window diagnostics, queue summary, and command logs together.
- Keep backend behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 05:05 update

Status: completed

Done:

- Scanned the current JavaFX UI controller and available `WindowTaskSnapshot` fields.
- Added a broader UI-only cleanup pass:
  - wrapped the top status text into a dedicated `运行概览` section;
  - added light section borders/backgrounds for clearer grouping;
  - added pending queue summary text above the queue list;
  - increased selected-window detail height and added more diagnostics;
  - selected-window details now show last task/result timestamps, last queue message, last result message, and queue failure policy;
  - main table now includes recent task and recent result columns;
  - table rows are lightly colored by state: running, stopped, error, and task-accepting windows;
  - window overview now includes visible accepting-window count and bound-window count;
  - command detail logs now use `[成功]` / `[失败]` prefixes for faster scanning.
- This is UI display/layout/logging only and does not change backend task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Current styling is inline JavaFX CSS inside the controller. Later, if the UI keeps growing, moving styles into a stylesheet would be cleaner.
- No visual runtime screenshot was captured in this headless/tooling pass; user should verify the actual JavaFX window sizing/scroll behavior in the app.

Needs from others:

- none

### 唐德 - 2026-05-21 04:45

Status: active

Goal:

- Continue JavaFX UI layout cleanup after the scroll/table visibility fix.
- Reduce crowded horizontal control rows by allowing action rows to wrap.
- Keep changes UI-only.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 04:45 update

Status: completed

Done:

- Replaced crowded one-line JavaFX action rows with wrapping `FlowPane` control rows.
- Registration, discovery, window selection, task control, supply config, and queue controls now wrap when the window is narrow.
- Split task queue controls into separate rows for adding tasks, managing queue order, and applying presets.
- Kept the previous scroll/table visibility fix intact.
- This is UI layout only and does not change backend task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Visual styling is still utilitarian. Later polish can add spacing/section styling, but the immediate usability issue should be improved.

Needs from others:

- none

### 何黎 - 2026-05-21 03:44 update

Status: completed

Done:

- Scanned potentially overlapping window snapshot/runtime fields.
- No code changes were made.
- Current interpretation:
  - `roleName` is legacy/display fallback identity and still useful when structured identity is empty.
  - `playerName` / `playerId` / `serverName` are the preferred structured player identity fields.
  - `selectedTaskType` is the configured/default task for the window.
  - `runningTaskType` is the current task inside the active queue.
  - `lastTaskType` / `lastResult` describe the most recent single task result.
  - `lastQueueDisplayText` / `lastQueueResult` / `lastQueueMessage` describe the last submitted queue/batch result.
  - `lastMessage` is the latest high-level window status message.
  - `lastResultMessage` is the latest single-task/finish message.
  - `lastQueueMessage` is the latest queue-level finish message.
- 唐德's UI already prefers `WindowTaskSnapshot.getPlayerName()/getServerName()/getPlayerId()` before native-title parsing, so the new snapshot identity interface is consumed.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Scan-only / docs-only update; compile not run.

Open issues:

- Do not remove the overlapping fields yet; they represent different time scopes and UI/debug consumers still use them.

Needs from others:

- none

### 何黎 - 2026-05-21 03:35 update

Status: completed

Done:

- Scanned framework/input/window naming for misleading legacy/debug/test names after `GlobalInputLock` cleanup.
- No Java code changes were made.
- Findings:
  - No remaining misleading bean name comparable to `legacyGlobalInputLock` was found in active framework/input code.
  - `WindowInteractionDiagnostics`, `WindowInteractionReport`, `TaskWindowRuntimeService`, and `TaskWindowBindingResolver` names match their current support/diagnostic responsibilities.
  - `DefaultWindowTaskStartupInitializer` / `WindowTaskStartupInitializer` names match current startup behavior.
  - The main remaining "test/role assignment" naming is the old RO/assignment path (`DETECTED_ROLE`, `startByDetectedRoleForTest`, `WindowTaskAssignmentPolicy`, etc.), but this is intentionally frozen while 谢帅 works on backend role recognition.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Scan-only / docs-only update; compile not run.

Open issues:

- Revisit RO/assignment naming only after 谢帅's backend role recognition work settles.

Needs from others:

- none

### 唐德 - 2026-05-21 04:30

Status: active

Goal:

- Fix JavaFX UI layout usability after user testing.
- Make the window table visible without manual resizing.
- Add scrolling to the main window-control area and move the table higher in the layout.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 04:30 update

Status: completed

Done:

- Fixed the JavaFX window-control layout so the registered-window table is visible without manually dragging the app taller.
- Wrapped the main window-control area in a `ScrollPane` with vertical and horizontal scrollbars as needed.
- Moved the window table higher in the page, before the detail/queue/registration sections.
- Gave the window table and selected-window detail list stable minimum/preferred heights.
- Reduced bottom command-log panel height so it does not crowd out the main table.
- Slimmed the main table by removing low-frequency `绑定标题` and redundant `运行中` columns; full title remains visible in selected-window details.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- The control rows are still long horizontal rows. A later UI pass can split registration, queue, and selection actions into tighter rows if the app window is narrow.

Needs from others:

- none

### 何黎 - 2026-05-21 03:26 update

Status: completed

Done:

- Renamed the `GlobalInputLock` Spring bean from `legacyGlobalInputLock` to `globalInputLock`.
- Checked there are no `@Qualifier("legacyGlobalInputLock")` usages before renaming.
- The class remains the active global physical-input lock used by `WindowAwareInputCoordinator`, `WindowFocusService`, and `GameClientTracker`.

Changed files:

- `src/main/java/com/bot/dhxy/input/GlobalInputLock.java`
- `docs/ACTIVE_WORK.md`

Validation:

- Pending compile after this small bean-name cleanup.

Open issues:

- none

Needs from others:

- none

### 唐德 - 2026-05-21 04:10

Status: active

Goal:

- Continue JavaFX UI-only improvements after reading the latest coordination board.
- Make selected-window diagnostics easier to read without changing backend execution behavior.
- Surface structured queue submit diagnostics already provided by framework command details.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 04:10 update

Status: completed

Done:

- Reworked selected-window diagnostics in `MainWindowController` from one long label into a structured detail list.
- Detail rows now separate:
  - window/status/accepting-task state;
  - role/server/player id identity;
  - native binding hwnd/class/pid;
  - selected/running task and queue progress;
  - current/last queue and queue result;
  - full native title and last message.
- Command detail logs now surface structured queue submit diagnostics when present:
  - submitted queue display text;
  - submit status;
  - queue failure policy.
- This is UI display/logging only and does not change task execution behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- The table is still very wide; the next UI-only pass can move low-frequency columns such as full title/message out of the table and rely on the structured detail list instead.

Needs from others:

- none

### 何黎 - 2026-05-21 03:18 update

Status: completed

Done:

- Scanned for old single-window / old runner leftovers after the current documentation sync.
- Confirmed deleted-old-path statement is accurate:
  - no `GameWindowService` source remains;
  - no old `runner/execution/TaskRunner` or `runner/execution/TaskQueue` source remains;
  - no old task registry/history/log/viewmodel classes remain in source.
- Remaining `runner/*` files are current support types:
  - `runner/context/TaskExecutionContext`;
  - `runner/policy/TaskRetryPolicy`;
  - `runner/stop/*`.
- Remaining `window/interaction/*` files are current support/diagnostic types, not the old mouse/screenshot service stack:
  - `WindowFocusService`;
  - `TaskWindowRuntimeService`;
  - `TaskWindowBindingResolver`;
  - `WindowInteractionDiagnostics`;
  - `WindowInteractionReport`.
- `GlobalInputLock` bean has since been renamed to `globalInputLock`; it is actively used by `WindowAwareInputCoordinator`, `WindowFocusService`, and `GameClientTracker`; do not delete it as old code.
- No Java code was changed.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Scan-only / docs-only update; compile not run.

Open issues:

- The confusing old `legacyGlobalInputLock` bean name has been cleaned up. `GlobalInputLock` is not dead code.

Needs from others:

- none

### 何黎 - 2026-05-21 03:08 update

Status: completed

Done:

- Scanned long-term docs against the current framework code for recently changed APIs.
- Updated `docs/DHXY_CONTEXT.md` framework status to include:
  - `WindowTaskQueue` failure policy;
  - default `CONTINUE_ON_FAILURE` behavior and future `STOP_ON_FAILURE`;
  - queue result aggregation and runtime/snapshot queue fields;
  - structured snapshot player identity fields;
  - structured submit status/details;
  - startup initializer behavior and `debug_coordinate` skip;
  - runner-level `ALT+6` visibility preparation.
- No Java code was changed.

Changed files:

- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Validation:

- Documentation-only change; compile not run.

Open issues:

- `AGENTS.md` still has older high-level summaries; this is acceptable because `docs/DHXY_CONTEXT.md` carries the detailed current state.

Needs from others:

- none

### 何黎 - 2026-05-21 02:58 update

Status: completed

Done:

- Documented the settled direct-input rule in `docs/DHXY_CONTEXT.md`.
- Rule summary:
  - normal task input goes through `InputSequences` / `InputActionQueue`;
  - direct `InputProvider` calls are allowed inside `submitExclusiveAndWait(...)` callbacks because the worker is already in a serialized exclusive section;
  - do not enqueue `submitAndWait(...)` from inside an exclusive callback;
  - debug-only direct input paths must stay clearly marked as debug/direct;
  - move + click remains atomic as one queued action list or one exclusive callback section.
- No Java code was changed.

Changed files:

- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Validation:

- Documentation-only change; compile not run.

Open issues:

- Later code comments may be useful around special direct-input methods, but avoid touching `SummonSkillService` while 谢帅 owns it.

Needs from others:

- none

### 唐德 - 2026-05-21 01:07

Status: active

Goal:

- Consume framework-provided structured player identity fields in the JavaFX UI.
- Prefer `WindowTaskSnapshot.playerName/playerId/serverName` over native title parsing.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 01:07 update

Status: completed

Done:

- Updated `MainWindowController` identity display to prefer structured snapshot fields:
  - `getPlayerName()`
  - `getServerName()`
  - `getPlayerId()`
- Native-title regex parsing remains only as fallback.
- Final fallback still uses the old roleName/title-derived value when structured identity and title parsing are unavailable.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed.

Open issues:

- UI will show `-` for structured identity until a task path has run `PlayerStateService.syncMyIdentity()` for that window.

Needs from others:

- none

### 何黎 - 2026-05-21 02:44

Status: completed

Goal:

- Add structured player identity fields to `WindowTaskSnapshot` for 唐德's UI thread.
- Source identity from each window's bound `GameContext.State.me`.
- Keep UI untouched and preserve existing snapshot constructors.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- RO/leader-member recognition logic owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- Added structured player identity fields to `WindowTaskSnapshot`:
  - `playerName`
  - `playerId`
  - `serverName`
- `WindowTaskRunner.snapshot()` now reads the current window's dedicated `GameContext.State.me` and passes identity into the snapshot.
- Empty identity values are normalized to `null`.
- Existing snapshot constructors are preserved for compatibility.
- UI was not edited; 唐德 can now prefer snapshot identity fields and keep native-title parsing as fallback.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI thread still needs to consume the new getters.

### 何黎 - 2026-05-21 02:32 update

Status: completed

Done:

- Scanned framework/business boundary points while intentionally avoiding RO/leader-member cleanup because 谢帅 is working on that backend feature.
- No code changes were made.
- Window layer still has RO-shaped fields/helpers (`WindowRole`, `WindowRuntimeContext.role`, `WindowRegistrationRequest.role`) but these are frozen for now per user direction.
- Direct `InputProvider` usage classification:
  - Looks acceptable / intentional:
    - `NavigationService.openMapInputTargetAndClickLastNavPointExclusive(...)`: direct input is inside `submitExclusiveAndWait(...)`.
    - `NavigationService.ensureMapTrackingOption(...)`: direct input is inside `submitExclusiveAndWait(...)`.
    - `SummonSkillService` direct input calls are mostly inside `submitExclusiveAndWait(...)`; owned by 谢帅, do not modify here.
    - `NpcClickService` Ctrl probe direct input is inside `submitExclusiveAndWait(...)`, matching the no-nested-queue rule.
  - Needs future review, not changed now:
    - `NpcClickService.executeClickAndVerifyDirect(...)` / direct first-shot debug helpers still use `InputProvider` directly. They appear tied to direct/debug or exclusive paths, but should stay documented as special-case only.
    - Some services still inject both `InputProvider` and `InputSequences`; this is allowed only when direct calls are inside an exclusive input transaction or a debug-only path.
- No obvious active path was found that directly creates/runs `WindowTaskRunner` outside `MultiWindowTaskManager`.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Code not changed in this scan; compile not rerun.

Open issues:

- Later framework cleanup could add comments around direct-input special cases, but avoid touching `SummonSkillService` while 谢帅 owns it.

Needs from others:

- none

### 何黎 - 2026-05-21 02:20 update

Status: completed

Done:

- Scanned remaining framework compatibility/legacy APIs without deleting code.
- Current backend execution shape is still clean: `MultiWindowTaskManager -> WindowTaskRunner -> WindowTaskQueue -> TaskType`.
- Cleanup candidates found:
  - old/test role-assignment flow:
    - `WindowTaskStartMode.DETECTED_ROLE`
    - `WindowTaskStartRequest.detectedRole(...)`
    - `WindowTaskControlService.startByDetectedRole(...)`
    - `WindowTaskControlService.startByDetectedRoleForTest(...)`
    - `GameWindowRegistrationService.registerDetectedGameWindowsByRoleForTest(...)`
    - deprecated role-mapping helpers in `WindowRegistrationBatchBuilder` / `NativeWindowRegistrationMapper`
  - boolean/single-task compatibility wrappers in `MultiWindowTaskManager`, such as `submit(...)`, `submitSelectedTask(...)`, `submitSelectedTasks(...)`, and `submit(Collection, TaskType)`.
- Keep for now:
  - `WindowTaskStartMode.SELECTED_TASK`, because current UI still uses selected-task startup.
  - `WindowTaskStartRequest.sameTask(...)`, because current UI single-task start uses it.
  - `WindowTaskControlService.startIndependentWindows(...)`, until UI/discovery naming is cleaned up.
- Do not delete the old/test role-assignment flow until the user confirms no thread still needs the test path.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Code not changed in this scan; compile not rerun.

Open issues:

- Next cleanup can either mark more legacy factories as `@Deprecated`, or leave them untouched until UI and discovery flows settle.

Needs from others:

- none

### 唐德 - 2026-05-21 00:52

Status: active

Goal:

- Investigate and improve JavaFX window identity display.
- Keep role/name/server/id display separate from the full native window title.
- Prefer UI-side parsing/display first; avoid backend binding changes unless needed.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:52 update

Status: completed

Done:

- Fixed UI identity display so `角色名` no longer shows the full native window title when the title is parseable.
- Reused the same title shape as `ClientIdentityService`: `- 服务器 - 角色名 (ID:123)`.
- Added separate table columns for `服务器` and `ID`.
- Updated selected-window detail to show parsed role/server/id separately from the full native title.
- This is UI display only. Backend registration, title binding, and task identity sync were not changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed.

Open issues:

- If the game title format changes, UI parsing and `ClientIdentityService` should eventually share a common parser instead of duplicating the regex.

Needs from others:

- none

### 何黎 - 2026-05-21 02:08

Status: completed

Goal:

- Preserve structured submit diagnostics in `WindowTaskCommandDetail`.
- Avoid forcing UI/log code to parse free-form message strings later.
- Keep existing messages and UI behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- summon skill / auto battle files owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/control/WindowTaskCommandDetail.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- `WindowTaskSubmitResult` now exposes queue failure policy and submit status display text.
- `WindowTaskCommandDetail` can now preserve structured submit diagnostics:
  - submit status;
  - task queue display text;
  - task queue failure policy.
- `WindowTaskControlService` now builds task-start command details from `WindowTaskSubmitResult`, while keeping the existing message text.
- Registration/stop/remove details remain simple message-only details.
- UI was not edited.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskCommandDetail.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI still logs only detail messages. It can use `getSubmitStatusDisplayName()` and queue/policy getters later if needed.

### 何黎 - 2026-05-21 01:55

Status: completed

Goal:

- Add structured queue failure policy fields to runtime/snapshot data.
- Keep UI behavior unchanged; only expose backend state for later display/debug.
- Avoid editing `MainWindowController.java` while 唐德 owns UI.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- summon skill / auto battle files owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- `WindowRuntimeContext` now stores the last queue failure policy together with last queue result/message.
- `RunningTaskHandle` exposes the active queue failure policy.
- `WindowTaskSnapshot` now has structured running/last queue failure policy getters and display-name helpers.
- `WindowTaskRunner` passes the active policy into both snapshot construction and queue-finished runtime state.
- UI was not edited.

Changed files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI can optionally display `getRunningQueueFailurePolicyDisplayName()` / `getLastQueueFailurePolicyDisplayName()` later, but this is not required for current behavior.

### 唐德 - 2026-05-21 00:46

Status: active

Goal:

- Continue UI work autonomously within the UI lane.
- Add task queue preset helpers so common queues can be built quickly.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:46 update

Status: completed

Done:

- Added task queue preset buttons to the JavaFX queue panel:
  - `预设:当前任务`
  - `预设:五环`
  - `预设:自动战斗`
  - `预设:五环+自动战斗`
- Presets only update the UI pending queue and do not change backend queue execution semantics.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- Full `mvn -q -DskipTests compile` is currently blocked by an unrelated framework signature mismatch in `WindowTaskRunner.markQueueFinished(...)` / `WindowRuntimeContext.markQueueFinished(...)`, owned by 何黎's active framework lane.

Open issues:

- Re-run full compile after the framework lane finishes reconciling `markQueueFinished(...)`.

Needs from others:

- 何黎: finish or reconcile the framework `markQueueFinished(...)` signature before full-project compile can pass.

### 何黎 - 2026-05-21 01:42

Status: completed

Goal:

- Expose queue failure policy through the backend start/submit APIs with thin overloads.
- Keep existing UI/default behavior unchanged.
- Avoid UI edits; leave actual policy selection for a later user/UI decision.

Owns:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- summon skill / auto battle files owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/control/WindowTaskStartRequest.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- Added thin policy-aware overloads to the backend start/submit chain:
  - `WindowTaskStartRequest.sameTask(..., failurePolicy)`
  - `WindowTaskControlService.startSameTask(..., failurePolicy)`
  - `MultiWindowTaskManager.submitWithResult(..., failurePolicy)`
- Existing default UI/control calls still use `CONTINUE_ON_FAILURE` through `WindowTaskQueue.single(...)`.
- No UI behavior or task behavior changed.

Changed files:

- `src/main/java/com/bot/dhxy/window/control/WindowTaskStartRequest.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI still does not expose failure policy selection. That is intentional for now.

### 何黎 - 2026-05-21 01:30

Status: completed

Goal:

- Add explicit queue failure policy scaffolding for `WindowTaskQueue`.
- Keep the current default behavior unchanged: task `FAILED` still lets later queued tasks continue, while `STOPPED` stops the queue.
- Make the policy visible in runner logs so future UI/task configuration can decide `CONTINUE_ON_FAILURE` vs `STOP_ON_FAILURE` without burying that rule in `WindowTaskRunner`.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskQueue.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskFailurePolicy.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- Added `WindowTaskFailurePolicy` with `CONTINUE_ON_FAILURE` and `STOP_ON_FAILURE`.
- `WindowTaskQueue` now carries a failure policy; all existing constructors/factories default to `CONTINUE_ON_FAILURE`, so current queue behavior is unchanged.
- `WindowTaskRunner` now reads the queue policy when deciding whether `FAILED` should stop later queued tasks.
- Queue finish messages now include the active failure policy for diagnostics.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskFailurePolicy.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskQueue.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- No UI/control path selects `STOP_ON_FAILURE` yet. That should be wired only after we decide which queued tasks need fail-fast behavior.

### 唐德 - 2026-05-21 00:40

Status: active

Goal:

- Continue UI work autonomously within the UI lane.
- Improve the window control panel structure and display queue-level results now exposed by the framework.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:40 update

Status: completed

Done:

- Reorganized the JavaFX window panel into functional sections: window registration, supply config, task queue, window selection, and task control.
- Updated the table to display framework-provided queue-level result data:
  - `上次队列`
  - `队列结果`
- Updated selected-window detail to show last queue/result instead of only last single task/result.
- Added a `当前任务入队` button to quickly add the main task ComboBox value into the pending queue.
- Kept all changes UI-only; backend execution and queue semantics were not changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI is better grouped but still uses plain JavaFX controls; later visual polish can improve spacing/style.
- Queue presets are still in-memory only; persistent presets could be a later feature if useful.

Needs from others:

- none

### 何黎 - 2026-05-21 01:18 update

Status: completed

Done:

- Added queue-level result memory to `WindowRuntimeContext`.
- `WindowTaskRunner` now aggregates each submitted `WindowTaskQueue` into a final queue result after the batch finishes.
- The aggregation does not change current execution flow:
  - failed tasks still do not stop later tasks;
  - stopped tasks still stop the queue;
  - any failure makes the final queue result `FAILED`;
  - any stop makes the final queue result `STOPPED`;
  - otherwise successful work makes it `SUCCESS`.
- `WindowTaskSnapshot` now exposes last queue display/result/message for UI and diagnostics.

Changed files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI does not yet display last queue result/message. It can use the new snapshot getters later.
- Queue failure policy is still "continue after FAILED"; changing that should be a separate explicit decision.

Needs from others:

- none

### 唐德 - 2026-05-21 00:35

Status: active

Goal:

- Improve UI command log readability.
- Remove visible role/leader/member controls from the JavaFX UI where they are not useful to the user.
- Keep backend role fields and assignment logic untouched.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:35 update

Status: completed

Done:

- Removed visible role/leader/member UI controls from `MainWindowController`.
- Removed the `显示身份` table column.
- Removed the `测试按身份启动` button from the action row.
- Manual window registration now passes `WindowRole.UNKNOWN`; backend role fields and assignment logic remain untouched.
- Improved UI command log readability with a command summary line and clearer `成功` / `失败` detail prefixes.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Backend still contains deprecated/test role-assignment paths. They are no longer exposed in this UI, but framework cleanup should be owned by 何黎 if needed.
- UI log still does not consume structured `WindowTaskSubmitStatus` directly because `WindowTaskCommandDetail` currently exposes message text only.

Needs from others:

- none

### 何黎 - 2026-05-21 01:10

Status: active

Goal:

- Inspect queue-level execution result semantics.
- Decide whether framework needs an explicit queue-level result/status instead of exposing only the latest task result.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- TBD after scan; likely `WindowTaskRunner`, `WindowTaskSnapshot`, possibly `WindowRuntimeContext`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:35

Status: active

Goal:

- Move lightweight cleanup role gating away from `WindowRole`.
- Use backend team-role detection (`TeamRoleDetectionService`) as the single role decision entry point.

Owns:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- UI/window role assignment behavior
- `FiveRingTask.java`
- framework execution/control files owned by 何黎

Planned files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `src/main/java/com/bot/dhxy/config/TeamTaskProperties.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputAction.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionType.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/WinApiMouseController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:35 update

Status: completed

Done:

- Confirmed the existing backend entry point is `TeamRoleDetectionService`; window/UI role should not be the source of truth for real leader/member decisions.
- Added `TeamRoleDetectionService.shouldRunLightweightCleanup(...)` as the central rule for lightweight cleanup eligibility.
- Added team config switches:
  - `lightweightCleanupRequiresMember`, default `true`
  - `allowLightweightCleanupWhenRoleUnknown`, default `false`
- `NavigationService` now asks `TeamRoleDetectionService` instead of checking `WindowRole` directly.
- Added the first real team-role detection flow behind `roleDetectionEnabled=false`:
  - hover configured team area and inspect configured tooltip rect for white + purple pixels;
  - return `SOLO` when no team tooltip is detected;
  - press Alt+T and match configured transfer-leader template for `LEADER`;
  - match configured member marker template for `MEMBER`;
  - return `UNKNOWN` when neither leader nor member marker matches;
  - retry Alt+T panel detection according to `teamPanelRoleDetectionMaxAttempts`;
  - close the team panel with a single Alt+T after each panel probe.
- Added queued Alt+T input support.
- Added `bot.team` config keys in `application.yml`, including hover delay, hover random radius, panel open/close delay, retry count, and leader/member template rects.
- Renamed provided team templates to `transfer_leader_button.png` and `member_marker.png`.
- Filled tooltip detection rect from user coordinates: `(1672,510)-(1783,579)` relative to base `(992,386)` => `(680,124,w=111,h=69)`.
- Strengthened team tooltip detection from only white+purple pixels to white+purple plus text-like distribution checks:
  - colored pixels must span enough rows and columns;
  - rows must contain enough foreground/background transitions;
  - a single row cannot be mostly filled by one continuous color block.

Changed files:

- `src/main/java/com/bot/dhxy/config/TeamTaskProperties.java`
- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputAction.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionType.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/WinApiMouseController.java`
- `src/main/resources/application.yml`
- `images/template/team/transfer_leader_button.png`
- `images/template/team/member_marker.png`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Leader/member panel rects and templates are filled from the user's provided coordinates/templates.
- `roleDetectionEnabled` defaults to `false`, so behavior remains safe until coordinates/templates are filled.
- Tooltip text-distribution thresholds may need tuning from real debug logs.

Needs from others:

- none

### 唐德 - 2026-05-21 00:30

Status: active

Goal:

- Improve JavaFX UI control states for window/task actions.
- Disable or hint actions when selection/queue state makes them unusable.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:30 update

Status: completed

Done:

- Added an operation hint label to the JavaFX window panel.
- Start/stop/remove buttons now reflect the current table selection state.
- Queue controls now reflect the current pending queue size.
- `启动队列` is disabled when no window is selected or the pending queue is empty.
- During background window commands, controls remain disabled and the hint shows that a command is running.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- This is still functional gating only. Later visual polish should group controls and make disabled reasons more discoverable.

Needs from others:

- none

### 唐德 - 2026-05-21 00:27

Status: active

Goal:

- Use framework-provided queue acceptance state in the JavaFX UI.
- Show whether windows can accept task queues and warn before starting tasks on unavailable windows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:05

Status: active

Goal:

- Treat `UICleanerService.cleanLightweightInterruptions(...)` as a role-agnostic cleanup.
- Wire it into low-risk navigation/movement waits before touching 五环 task logic.

Owns:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- framework execution/control files owned by 何黎
- UI files owned by 唐德

Planned files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:05 update

Status: completed

Done:

- Treated lightweight interruption cleanup as role-agnostic/common behavior.
- Wired `UICleanerService.cleanLightweightInterruptions(...)` into `NavigationService` movement/wait loops.
- Added per-window navigation throttling so lightweight cleanup runs at most once every 2500ms during navigation waits.
- Did not edit `FiveRingTask`; 五环 will benefit indirectly when it uses `NavigationService`.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game validation that business dialog handling during navigation does not interfere with target-map dialog selection.
- If navigation feels slower, tune `LIGHTWEIGHT_CLEAN_INTERVAL_MS`.

Needs from others:

- none

### 谢帅 - 2026-05-21 01:20 update

Status: completed

Done:

- Rechecked role logic: leader windows own main task routing, OCR dialogs, NPC/task progress; member windows own auto-battle, status maintenance, and simple popups.
- Adjusted `NavigationService` lightweight cleanup so navigation only runs it for explicit MEMBER windows.
- LEADER, UNKNOWN, and no-window-context navigation skip lightweight cleanup to avoid stealing task dialogs from leader OCR/business flow.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- If we later want UNKNOWN windows to behave like member windows, make that a deliberate config decision instead of the default.

Needs from others:

- none

### 唐德 - 2026-05-21 00:27 update

Status: completed

Done:

- Used `WindowTaskSnapshot.isAcceptingTaskQueue()` in the JavaFX UI.
- Added a `可接任务` table column.
- Added `可接任务` to the selected-window detail line.
- `启动当前任务`, `启动已选任务`, and `启动队列` now write a UI log warning when selected windows are not accepting task queues.
- The warning does not block submission; backend rules still decide the final command result.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI could later disable start buttons or split accepted/rejected windows, but this phase intentionally only warns.

Needs from others:

- none

### 唐德 - 2026-05-21 00:21

Status: active

Goal:

- Continue UI functionality without touching framework files currently owned by 何黎.
- Add a table filter for registered windows so multi-window debugging is easier.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:21 update

Status: completed

Done:

- Added a JavaFX table filter for registered windows.
- The window table can now show all/running/idle/bound/unbound windows.
- The window summary line now includes the visible row count after filtering.
- This is UI-only and does not change task execution, registration, or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Selection count currently reflects selected visible rows. Hidden rows are not acted on by UI commands, which is safer for filtered views.

Needs from others:

- none

### 唐德 - 2026-05-21 00:22

Status: active

Goal:

- Add problem-window selection/filter helpers to the UI.
- Keep this limited to snapshot display/selection state.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- framework files currently owned by 何黎
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:22 update

Status: completed

Done:

- Added an "异常/停止" window selection helper.
- Added an "异常/停止" table filter option.
- Both helpers are snapshot/UI-only and do not change task execution or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI controls are now functionally richer but visually crowded; visual grouping remains a later cleanup task.

Needs from others:

- none

### 何黎 - 2026-05-21 00:40

Status: active

Goal:

- Continue framework cleanup around structured queue submit results.
- Add machine-readable submit status so UI/logging does not need to parse message text.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitStatus.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 何黎 - 2026-05-21 00:45 update

Status: completed

Done:

- Added `WindowTaskSubmitStatus` for machine-readable queue submit outcomes.
- `WindowTaskSubmitResult` now carries a structured status while keeping old success/message/task getters.
- `MultiWindowTaskManager.submitQueueWithResult(...)` maps submit failures to explicit statuses:
  - `INVALID_WINDOW_ID`
  - `INVALID_QUEUE`
  - `WINDOW_NOT_REGISTERED`
  - `RUNNER_CLOSED`
  - `WINDOW_BUSY`
  - `SUBMIT_REJECTED`

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitStatus.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI does not use `WindowTaskSubmitStatus` yet; it can adopt it later for clearer warnings/disable states.

Needs from others:

- none

### 何黎 - 2026-05-21 00:30

Status: active

Goal:

- Continue framework cleanup around window task command result and queue-facing diagnostics.
- Check whether control/result models still expose only single-task semantics where queue semantics should be visible.

Owns:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- TBD after scan; likely only control/execution result models if needed
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 00:35 update

Status: planning next phase

Decision:

- The old auto-battle project avoided handling many dialogs because five roles shared one game window and tab switching left little time for maintenance.
- DHXY now targets independent windows per role, so each window has enough time during movement, waiting, and idle polling to handle lightweight dialogs or close interfering windows.
- Known business dialogs such as 医宝宝 / 修装备 / 装备无需修理 should remain shared `DialogService` capabilities, not auto-battle-only logic.

### 谢帅 - 2026-05-21 00:45 update

Status: completed

Done:

- Kept lightweight maintenance inside the existing `UICleanerService` instead of adding another service entry point.
- Added `UICleanerService.cleanLightweightInterruptions(...)` for conservative movement/wait/idle cleanup.
- The lightweight cleanup path currently handles known business dialog options first, then closes safe generic windows.
- AutoBattleTask now calls this existing cleaner during idle maintenance.

Changed files:

- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game validation for the three business dialog templates and safe generic-window close behavior.
- FiveRingTask is not wired to this yet because 五环 core behavior is high conflict and should be coordinated before editing.

Needs from others:

- none

Next suggested work:

- Add a shared "lightweight dialog/window maintenance" entry point that tasks can call while moving or waiting.
- The first scope should be conservative: handle known business dialog options, close safe generic popups, and avoid broad fallback clicking.
- AutoBattleTask should keep using this maintenance path while FREE or moving/waiting.
- FiveRingTask and other tasks can later call the same maintenance path during navigation waits, after confirming it does not interfere with NPC/task dialogs.
- Keep all real clicks through `InputSequences`; any move+click remains one atomic sequence.

Recommended next implementation owner:

- 谢帅 can implement the shared maintenance service/API and wire it into AutoBattleTask first.
- If FiveRingTask integration is needed, coordinate before editing because 五环 core behavior is high conflict.

Needs from others:

- 何黎: no framework change needed right now, unless maintenance needs a standardized moving/waiting hook in the runner later.
- 唐德: no UI change needed right now.

### 唐德 - 2026-05-21 00:14

Status: active

Goal:

- Continue JavaFX UI functionality work.
- Add practical window selection helpers for multi-window debugging and control.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- framework execution/control files currently owned by 何黎

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:14 update

Status: completed

Done:

- Added UI selection helper buttons for registered windows.
- The window panel can now select running, idle, bound, or unbound windows directly from the table.
- These controls only update JavaFX table selection state; they do not start tasks, stop tasks, or send input.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Button row is getting crowded; visual grouping/polish should happen later after functional coverage is acceptable.

Needs from others:

- none

### 唐德 - 2026-05-21 00:15

Status: active

Goal:

- Add a direct "start current task" UI action for selected windows.
- Use existing backend `sameTask` request path without changing execution semantics.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- framework execution/control files currently owned by 何黎
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:15 update

Status: completed

Done:

- Added a "启动当前任务" button for selected windows.
- The action starts the task currently selected in the UI task ComboBox on selected windows.
- It uses the existing request path: `WindowTaskStartRequest.sameTask(...) -> WindowTaskControlService.start(...)`.
- No backend execution or task behavior changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The action row now has several start modes; later UI polish should group them by "current task", "saved selected task", "role test", and "queue".

Needs from others:

- none

### 何黎 - 2026-05-21 00:34 update

Status: completed

Done:

- Added explicit queue-acceptance state to the framework snapshot.
- `WindowTaskRunner.canAcceptTaskQueue()` now centralizes the current rule: a window accepts a new queue only when the runner is not shut down and no task is running.
- `WindowTaskSnapshot.isAcceptingTaskQueue()` exposes that rule to UI/readers without requiring them to inspect runner internals.
- `MultiWindowTaskManager.submitQueueWithResult(...)` now uses `canAcceptTaskQueue()` for the idle check.
- Removed an accidental UTF-8 BOM from `MultiWindowTaskManager.java` after compile caught it.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- Current queue behavior is batch-submit only. Appending to a running queue is not implemented yet.

Needs from others:

- none

### 何黎 - 2026-05-21 00:20

Status: active

Goal:

- Continue framework cleanup around window task queue submission results.
- Make submit diagnostics queue-aware instead of only first-task-aware.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 00:25 update

Status: completed

Done:

- Renamed the shared request factory to `DialogHandleRequest.clickBusinessOption(...)`.
- Clarified the intent: 医宝宝 / 修装备 / 装备无需修理 are shared `DialogService` capabilities available to any task.
- Kept auto battle as a narrow caller that only uses this shared business-option capability during idle maintenance.

Changed files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Other tasks can now call `dialogService.handleDialog(DialogHandleRequest.clickBusinessOption("task-name"))` when they want this same known-business-dialog behavior.

Needs from others:

- none

### 谢帅 - 2026-05-21 00:25

Status: active

Goal:

- Clarify that known business dialog options are a shared `DialogService` capability, not an auto-battle-only capability.
- Keep auto battle as a narrow caller that uses only this shared capability during idle maintenance.

Owns:

- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- framework execution/control files owned by 何黎
- UI files owned by 唐德
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 何黎 - 2026-05-21 00:25 update

Status: completed

Done:

- Made `WindowTaskSubmitResult` queue-aware while keeping first-task getters for compatibility.
- `MultiWindowTaskManager.submitQueueWithResult(...)` now returns the full submitted `WindowTaskQueue` in success/failure results.
- `WindowTaskControlService.startSameQueue(...)` now reports queue display text in command details.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- `submitSelectedTaskWithResult(...)` still naturally wraps one selected task into a one-item queue.
- Framework does not yet support appending to a running window queue; current queue is submitted as one batch before execution starts.

Needs from others:

- none

### 唐德 - 2026-05-21 00:07

Status: active

Goal:

- Continue JavaFX UI functionality work for the window control panel.
- Add more useful runtime/result display without changing task execution behavior.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- framework execution/control files unless a small interface request is recorded first

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:07 update

Status: completed

Done:

- Added more runtime/result columns to the JavaFX window table.
- The table now shows last task, last result, last finished time, and bound native window title.
- The window summary label now includes selected window count.
- This is display-only and does not change task execution, registration, or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The table is becoming wide; later visual polish should decide whether to split detailed binding/runtime info into a side panel or detail area.

Needs from others:

- none

### 唐德 - 2026-05-21 00:08

Status: active

Goal:

- Add a selected-window detail display to the JavaFX UI.
- Make native binding and last-run diagnostics visible without requiring more table columns.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- task execution behavior
- framework execution/control files
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:08 update

Status: completed

Done:

- Added a selected-window detail label to `MainWindowController`.
- Selecting a window now shows native title, hwnd, class name, process id, running queue, last task/result, and last message.
- Multiple selection shows the selected count and details for the first selected row.
- This is display-only and does not change backend registration, execution, or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Detail display is still a compact text line; later UI polish may split it into structured fields.

Needs from others:

- none

### 谢帅 - 2026-05-21 00:10

Status: active

Goal:

- Add auto-battle business dialog handling through the existing `DialogService.handleDialog(...)` request/policy model.
- Keep the behavior auto-battle-scoped: only match the known business templates for 医宝宝 / 修装备 / 装备无需修理离开, and do not add broad fallback clicking to other task flows.

Owns:

- `src/main/java/com/bot/dhxy/service/dialog/*`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- related dialog templates under `images/template/dialog/`
- `docs/ACTIVE_WORK.md` for this work log

Avoids:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- framework execution/control files owned by 何黎
- UI files owned by 唐德

Planned files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionPolicy.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleResult.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `images/template/dialog/heal_pet_option.png`
- `images/template/dialog/repair_equipment_option.png`
- `images/template/dialog/repair_equipment_option_giveup.png`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:01

Status: active

Goal:

- Continue JavaFX UI functionality work after reading the updated coordination rules.
- Keep changes focused on UI controls/status display and short-term coordination notes.
- Improve the existing task queue UI so it is safer and more useful before visual polish.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- UI table/button/status display
- `docs/ACTIVE_WORK.md` for this UI work log

Avoids:

- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- framework execution/control files unless a small interface request is recorded first

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:01 update

Status: completed

Done:

- Added a UI button to batch-set the selected task for selected registered windows.
- The new action preserves each selected window's role, role name, and native binding by rebuilding `WindowRegistrationRequest` from `WindowTaskSnapshot`.
- The action uses the existing `WindowTaskControlService.registerWindows(...)` path; it does not bypass runner/framework rules.
- Running windows still follow the existing backend rule: `WindowTaskRunner.refreshRegistration(...)` only changes selected task when the runner is not running.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI task queue ordering is still minimal: add/remove/clear/start exists, but there are no move up/down controls yet.
- UI layout is still functional-first and not visually polished.

Needs from others:

- none

### 唐德 - 2026-05-21 00:02

Status: active

Goal:

- Improve the UI task queue builder with basic ordering controls.
- Keep this as UI-only behavior over the existing `pendingTaskQueue`.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- task execution semantics
- framework execution/control files
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:02 update

Status: completed

Done:

- Added up/down ordering controls to the UI task queue builder.
- Queue items can now be added, removed, moved up, moved down, cleared, and started on selected windows.
- Remove/move operations keep a useful queue selection when possible.
- This remains UI-only over `pendingTaskQueue`; no backend task execution behavior changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI layout is still functional-first and may need visual grouping/polish later.
- There is still no persisted task queue preset; queue is in-memory for the current UI session.

Needs from others:

- none

### 何黎 - 2026-05-20 update

Status: active

Done:

- Created this active coordination board.
- Added a long-term pointer from `docs/DHXY_CONTEXT.md` to this file.

Changed files:

- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`

Validation:

- Documentation-only change; compile not required.

Open issues:

- Other active agents should add their own latest status entries here before further edits.

### 谢帅 - 2026-05-21 00:10 update

Status: completed

Done:

- Added auto-battle-scoped business dialog handling through `DialogService.handleDialog(...)`.
- Added a `CLICK_BUSINESS_OPTION` request/policy/result path for known auto-battle maintenance dialogs.
- `DialogService` now scans window-scoped `business_dialog_raw.png` / `business_dialog_washed.png` and matches heal-pet, repair-equipment, and repair-giveup templates in order.
- `AutoBattleTask` now calls `DialogHandleRequest.businessOption("auto-battle")` during idle maintenance, with no broad fallback option click.

Changed files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionPolicy.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogOperation.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleResult.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game validation against the actual 医宝宝 / 修装备 / 装备无需修理 dialogs.
- Business option template threshold is currently `0.70`; tune only if logs/screenshots show misses or false positives.

Needs from others:

- none
### He Li - 2026-05-21 bag exclusive input fix

Goal:

- Diagnose five-window Wuhuan bag chaos where bags opened but later clicks landed on the map/wrong window.

Files changed:

- `src/main/java/com/bot/dhxy/service/BagService.java`

Result:

- Logs showed different windows reusing the same bag scan rect/tab click coordinates even when their window bases were different. Example: windows with bases around `(1405,441)` and `(1223,478)` still scanned/clicked around `(1206,755)-(1518,963)` / `(1534,806)`, which means bag anchor detection had been contaminated by another visible window.
- Root cause: bag operations were split into separate queue actions (`bag:openAltE`, `bag:switchTab`, `bag:itemAction`, `bag:closeAltE`) while screenshots/template matching happened between them. With five windows, another window could focus/cover the target between bag open, anchor detection, tab switch, scan, and item click.
- `BagService.findItemPageIndex(...)` and item actions now run as one `submitExclusiveAndWait(...)` transaction.
- Inside that exclusive transaction, bag open/close/tab click/item click use direct `InputProvider` calls, avoiding queue-in-queue deadlocks while keeping the whole bag workflow serialized for one window.
- Validation: `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify bag scan rects differ correctly by each window base and no longer reuse another window's bag anchor coordinates.

### He Li - 2026-05-21 navigation current-map resync

Goal:

- Diagnose why five-window Wuhuan still opened the world map for `长安` even when the characters were already in Chang'an.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`

Result:

- Latest logs showed the skip logic itself still works: the one window with `navigate to map: 长安 current=长安` skipped world-map navigation and went straight to `navigate in map`.
- Other windows logged `navigate to map: 长安 current=null`, so they had no current-map memory and fell through to `openMapInputTargetAndClickLastNavPoint`.
- `navigateToMap(...)` now performs one `playerStateService.syncMyPosition()` when `currentMapName` is null/blank, then re-checks the target map before opening the world map.
- Validation: `mvn -q -DskipTests compile` passed.

Open:

- If OCR/sync still returns null for specific windows, inspect their `tmp_pos.png` / coordinate strip images instead of changing navigation semantics.

### He Li - 2026-05-21 focused location capture fix

Goal:

- Fix the root cause behind five-window startup `syncMyPosition()` returning null: coordinate screenshots can be taken while another game window is covering the target window.

Files changed:

- `src/main/java/com/bot/dhxy/service/LocationVisionService.java`

Result:

- `LocationVisionService.scanCurrentLocation()` now captures the mini-map coordinate strip through `InputSequences.submitExclusiveAndWait(...)` whenever it is running inside a bound window task context.
- The input worker's existing window-aware transaction brings the bound window to front before the screenshot and prevents another physical input sequence from interleaving during capture.
- OCR parsing stays outside the focused input transaction so slow OCR does not hold the global input queue.
- Calls already running on `dhxy-input-action-worker` still use direct capture to avoid queue-in-queue deadlock.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should inspect each window-scoped `tmp_pos.png`; it should contain the actual mini-map coordinate strip for that hwnd instead of roof/map/other-window content.

### He Li - 2026-05-21 UI cleaner close-click atomic fix

Goal:

- Fix the case where a character reaches the NPC coordinate, then immediately gets moved away before NPC accept-click.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`

Result:

- Latest logs for `hwnd-B21276` / `『忍者』影` showed `arrived: (87, 174)` at 15:01:48, then `UICleanerService.cleanUpAll()` ran from `NavigationService.navigateToNPC()`.
- The better root cause is not "cleanup exists", but that generic close-button scan/click was split: one window could be captured/scanned while another window was focused for the later click.
- `navigateToNPC()` keeps the post-navigation cleanup behavior.
- `UICleanerService.clickCloseButtonOnce()` now runs screenshot, template match, and close click inside one `submitExclusiveAndWait(...)` transaction.
- Inside that transaction the click uses direct `InputProvider`, avoiding queue-in-queue and preventing another window from interleaving between finding the X and clicking it.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- If generic window cleanup still false-clicks elsewhere, `UICleanerService.closeAllGenericWindows()` needs a stricter window/header guard before accepting `x1/x2/x3` matches.

### He Li - 2026-05-21 location OCR concurrency fix

Goal:

- Address the real reason initial `syncMyPosition()` often left `currentMapName=null` during five-window startup.

Files changed:

- `src/main/java/com/bot/dhxy/core/TextRecognizer.java`

Result:

- Logs confirmed `syncMyPosition()` was called for each window and coordinate-region captures succeeded, but several windows still logged "radar could not read current position" and did not update `GameContext.State.me`.
- The failure pattern appears when several window task threads call the singleton Baidu `AipOcr` client concurrently during startup.
- `TextRecognizer` now serializes all direct Baidu OCR client calls (`basicGeneral` / `general`) through one lock so concurrent windows do not hit the shared OCR client at the same time.
- Validation: `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should show each startup `syncMyPosition()` either updating `currentMapName=长安` or, if still failing, leaving a screenshot/recognition issue to inspect per window rather than a concurrent OCR-client race.

### He Li - 2026-05-21 navigation and quest transaction tightening

Goal:

- Reduce five-window window-hopping by grouping more input-sensitive navigation/task-panel work into larger transactions.
- Add route-click diagnostics without changing the validated green-coordinate click target.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/window/interaction/WindowFocusService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `navigateToMap(...)` now checks movement/pathing intent before trying to handle the arrival dialog, so a just-clicked route link is allowed to start moving before dialog detection competes for foreground.
- Route coordinate clicking still uses the OCR-returned green coordinate link. It now logs `windowId`, bound hwnd, foreground hwnd, base, map rect, image path, relative point, and absolute point at click time.
- Wuhuan P1/P2 native pathing now runs as one exclusive task-panel transaction. Inside the transaction, panel opening, task activation, scrolling, P1/P2 click, and close use direct `InputProvider` calls to avoid splitting these steps across multiple queued requests.
- Added `WindowFocusService.getForegroundNativeHandleText()` for diagnostics.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should compare `boundHwnd` and `foregroundHwnd` in `navigation route coordinate click` logs when a route click appears visually wrong.

### He Li - 2026-05-21 dialog detect and route target click fix

Goal:

- Fix latest five-window Wuhuan issues where navigation dialogs were reported as `NO_DIALOG`, and one window clicked the wrong route result after typing Chang'an in the world map search.

Files changed:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `DialogService.detectDialogType()` now runs its dialog mask/option/story screenshots inside one `InputSequences.submitExclusiveAndWait(...)` transaction when called from task threads.
- Calls that are already inside the input worker fall through to a direct detector to avoid queue-in-queue deadlock.
- A temporary route-target-text click idea was removed after user review: yellow destination names are not clickable. Route clicking must remain on the OCR-returned green coordinate link.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify `navigation` dialog handling no longer returns `type=NONE result=NO_DIALOG` when the Chang'an option dialog is visibly open.

### He Li - 2026-05-21 route coordinate substring click fix

Goal:

- Fix route-result OCR clicking where the regex matched a green coordinate substring, but the actual click point used the center of the whole OCR text block.
- Clean up NavigationService route logs so the file no longer carries garbled navigation diagnostics.

Files changed:

- `src/main/java/com/bot/dhxy/core/TextRecognizer.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `TextRecognizer.findLastCoordinateLink(...)` now computes the click point from the regex-matched coordinate substring range within the OCR block, instead of clicking the center of the whole OCR block.
- The route log now prints `OCR coordinate match` with the matched text, block range, estimated coordinate substring range, and final relative point.
- `NavigationService` map-search logs were normalized to ASCII labels, and route-click diagnostics include `windowId`, bound hwnd, foreground hwnd, window base, map result rect, image path, relative point, and absolute point.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next route-click test should compare `OCR coordinate match ... point=(x,y)` with `navigation route coordinate click ... relative=(x,y)` and verify the clicked pixel lands on the green `(x,y)` coordinate text rather than the middle of the whole OCR sentence.

### He Li - 2026-05-21 navigation moving yield

Goal:

- Reduce five-window focus thrashing while a character is already auto-pathing.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `navigateToMap(...)` now checks `GameStateUtil.detectMovementState()` before dialog handling.
- If the current window is `MOVING`, `PATHING_ACTIVE`, or `MAYBE_MOVING`, navigation resets the stuck counter, logs a yield message, sleeps for `1500ms`, and lets other windows use the input queue.
- Dialog handling, location OCR, and route retries now happen only after movement is no longer active.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next multi-window test should watch whether focus switching drops during long auto-pathing sections. If it is still too chatty, the next knob is `MOVING_NAVIGATION_YIELD_MS`.

### He Li - 2026-05-21 wuhuan task sync pathing transaction

Goal:

- Prevent another window from interleaving between "check/activate Wuhuan task" and "click P2/P1 pathing".
- After initial task accept or task sync, push the current window into a pathing/movement-intent state before yielding control.

Files changed:

- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Result:

- Added `QuestManagerService.activateAndTriggerWuHuanPathing()`.
- The new method runs one exclusive transaction: activate Wuhuan task, try P2 pathing, then try P1 pathing if P2 is unavailable.
- `FiveRingTask` now uses this combined transaction after initial task accept and whenever `needTaskSync=true`.
- Normal loop pathing also uses the combined transaction, so P2 failure and P1 fallback are no longer split across two input queue turns.
- Successful combined pathing records movement intent (`wuhuan:syncPathing`, `wuhuan:syncPathingAfterCleanup`, or `wuhuan:combinedPathing`) before yielding.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify that after a character accepts Wuhuan or checks the task panel, it reaches a movement/pathing intent before other windows visually take over.

### He Li - 2026-05-21 wuhuan handover task detection transaction

Goal:

- Fix five-window startup where characters that already had Wuhuan still reported "task not found" and went back to initial task setup.
- Keep startup handover detection consistent with the newer exclusive task-panel transaction style.
- Do not yield after merely opening/checking the task panel; if the task exists, trigger P2/P1 pathing before handing control to another window.

Files changed:

- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Result:

- Added `QuestManagerService.activateTaskIfPresentExclusive(task, keepOpen)`, which wraps the existing direct task-panel scanner in one `submitExclusiveAndWait(...)`.
- `FiveRingTask.detectHandover(...)` now uses `activateAndTriggerWuHuanPathing()` as the main handover path, so an existing Wuhuan task is activated and immediately pushed into P2/P1 pathing before the task yields.
- Successful handover pathing records movement intent as `wuhuan:handoverPathing`.
- The two "confirm task after accepting initial dialog" checks now also call `activateAndTriggerWuHuanPathing()` instead of merely confirming the task exists.
- Successful initial accept pathing records movement intent as `wuhuan:initialAcceptPathing` or `wuhuan:currentScreenAcceptPathing`.
- Initial accept is now a single exclusive transaction: verify/click Wuhuan accept dialog, wait for server response, activate the task panel, trigger P2/P1, and briefly wait for pathing to start before yielding.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify that windows with an existing Wuhuan task enter handover/takeover mode instead of returning to initial NPC setup.

### He Li - 2026-05-21 wuhuan dialog/navigation transaction tightening

Goal:

- Scan for more five-window interleaving risks beyond the shoe give-item path.
- Keep validated Wuhuan business decisions unchanged; only tighten input transaction boundaries.

Files changed:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/GiveItemService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Result:

- Wuhuan give-item now runs as one exclusive input transaction: detect give option, click give option, select shoe, click give button, then immediately trigger P2/P1 pathing before yielding.
- Generic give-item handling also no longer splits "click give option" and "select/click item" across separate queued input turns.
- Wuhuan story dialog handling during the same dialog checkpoint can now be clicked inside the same dialog transaction instead of detecting once and then re-detecting in a later input turn.
- Navigation cached route reclick now opens the world map/search UI, clicks the cached green coordinate point, closes the world-map search UI, and records movement intent inside one exclusive input transaction.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window Wuhuan test should watch whether give-item/story-dialog/task-panel phases now run to a movement/pathing state before another window takes over.

### He Li - 2026-05-21 Wuhuan turn-yield audit

Status: completed

Goal:

- Audit Wuhuan task-turn boundaries after the five-window run where one window kept the turn while already pathing.
- Fix both directions: pathing must release, but cleanup/retry confirmation must not continue doing input after a premature release.

Changed files:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Wuhuan movement intent is now recorded inside the pathing transaction before the transaction releases the task turn.
- Handover, initial accept, give-item, task-sync, and combined P2/P1 pathing follow the same order: trigger pathing, record movement intent, return `PATHING_STARTED`, then release turn.
- Empty task-panel verification no longer releases as `TASK_FINISHED` before the confirm-cleanup pass. First empty result keeps the turn, runs cleanup as `READY_TO_CONTINUE + CONTINUE_CHAIN`, then retries task-panel pathing; only the second confirmed empty result can finish and release.
- UI cleanup after repeated task-panel/pathing errors is wrapped as `RETRYABLE_ERROR + RETRY_LATER` so it reacquires safely and then yields.
- Navigation failure exits force-release any held task turn so a failed map/current-map navigation cannot hold the global task turn forever.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Legacy unused Wuhuan P1/P2 helper methods still exist near the bottom of `FiveRingTask`; current flow uses `activateAndTriggerWuHuanPathingDirectForExclusive()` instead. They should be removed later when the file is cleaned, but they are no longer part of the active path.

### Xie Shuai - 2026-05-22 Auto8 quiet patrol rules

Status: completed

Goal:

- Apply the agreed Auto8 behavior rules so member auto-battle windows stay quiet outside explicit maintenance actions.

Changed files:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `docs/ACTIVE_WORK.md`

Done:

- Auto8 no longer focuses the game window at task startup.
- FREE-state Auto8 patrol uses a fixed 3 second interval.
- FREE-state patrol handles only maintenance broadcast business options through `UICleanerService.handleMaintenanceBroadcast(...)`.
- Maintenance broadcast matching includes 医保宝 / 修装备 style options and deliberately excludes 放弃修理.
- Generic close-window cleanup is no longer part of FREE-state Auto8 patrol.
- Summon skill cleanup remains lower priority than broadcast handling and still updates cooldown only after success.
- Combat-state generic window cleanup is throttled to 40 seconds inside Auto8 maintenance.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Post-combat HP/MP supply is still owned by `PlayerStateService`; batching one window's full person/pet supply before releasing input is the next supply-line cleanup if logs show interleaving.

### Xie Shuai - 2026-05-22 Battle radar timing split

Status: completed

Goal:

- Move battle-entry timing decisions out of `BattleRadarService` so the radar stays closer to detection/state signaling and Auto8 owns its own combat maintenance schedule.

Changed files:

- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- `BattleRadarService.checkAndSyncCombatState()` no longer accepts/runs a free-state first-aid option.
- `BattleRadarService` no longer sleeps/cleans generic windows/auto-aligns the combat panel immediately inside `onEnterCombat()`.
- Battle enter is now exposed as `consumeCombatEnterSignal()`.
- Auto8 consumes the battle-enter signal, waits 4 seconds, then performs one entry maintenance pass: generic window close plus auto-combat panel verify/align.
- Auto8 retains the 40 second combat generic-window cleanup throttle after that.
- FiveRing now uses the simplified `checkAndSyncCombatState()` signature; its post-combat supply path remains task-owned.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Superseded by "Auto-combat panel split" below: panel helpers have been moved out of `BattleRadarService`.

### Xie Shuai - 2026-05-22 Unified auto-combat state service

Status: completed

Goal:

- Make "auto combat" a shared state capability instead of behavior owned by the standalone AutoBattle task.

Changed files:

- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `AutoCombatService` as the shared combat-state automation layer.
- `AutoCombatService` owns per-window combat maintenance timing:
  - battle-enter signal consumption;
  - 4 second delayed entry maintenance;
  - generic window cleanup during combat;
  - auto-combat panel verify/align;
  - refresh interval from `BotProperties`;
  - battle-exit recovery with first-aid;
  - optional leader-task sheyaoxiang check after combat.
- `AutoBattleTask` is now a thin hanging/free-patrol task:
  - it delegates combat state/maintenance/recovery to `AutoCombatService`;
  - it keeps only FREE-state member patrol work such as return-team, maintenance broadcast, and summon skill maintenance.
- `FiveRingTask` now delegates its combat phase to `AutoCombatService`.
  - On unified combat exit recovery, Wuhuan sets `needTaskSync=true` and resumes its own task-panel/P2/P1 logic.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- `BattleRadarService` still exposes panel verify/align helpers used by `AutoCombatService`. A later cleanup can move those helpers fully into `AutoCombatService` or a dedicated panel service.

### Xie Shuai - 2026-05-22 Auto-combat panel split

Status: completed

Goal:

- Finish the responsibility split so `BattleRadarService` does not own auto-combat panel behavior.

Changed files:

- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `AutoCombatPanelService`.
- Moved auto-combat panel template detection, Alt+8 opening, drag alignment, and panel-round estimate state out of `BattleRadarService`.
- `AutoCombatService` now calls `AutoCombatPanelService.verifyAndAlignPanel()` for entry maintenance and refresh maintenance.
- `AutoCombatService` now calls `AutoCombatPanelService.recordCombatExit()` when unified combat exit recovery runs.
- `BattleRadarService` now keeps only combat detection/state signaling and polling interval logic.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- `BattleRadarService.getDynamicPollingIntervalMs()` still lives in radar. We can later move polling policy into `AutoCombatService` if we want all timing policy outside radar as well.

### Xie Shuai - 2026-05-22 Post-combat supply batching

Status: completed

Goal:

- Make one window finish its whole post-combat HP/MP supply pass before another window can interleave physical input.

Changed files:

- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `docs/ACTIVE_WORK.md`

Done:

- `PlayerStateService.healAll()` now runs the full person/pet HP/MP supply pass inside one `InputSequences.submitExclusiveAndWait("playerState:healAll", ...)` transaction.
- Inside that exclusive transaction, supply uses direct `InputProvider` mouse movement/right-clicks instead of nested `submitAndWait(...)`, avoiding input-queue deadlock.
- The full batch now covers:
  - moving the mouse away before bar screenshots;
  - initial bar snapshot;
  - secondary confirmation snapshots;
  - all needed person HP/MP and pet HP/MP right-click supply actions.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Runtime logs should confirm fewer interleaved `playerState:heal:*` actions because the visible queue item is now the single `playerState:healAll` transaction.

### He Li - 2026-05-22 Wuhuan combat Alt+Q audit

Status: completed

Goal:

- Diagnose the latest five-window Wuhuan run where several windows appeared to press `Alt+Q` after entering combat.
- Check why `忆叶知秋` appeared to wait at startup.

Findings:

- `忆叶知秋` did not deadlock. It waited for the global task turn from `00:08:32` to `00:09:20` because two earlier windows held the turn through prepare/handover/navigation until pathing. This matches the current "keep turn until pathing" rule, but it makes five-window startup visibly serial.
- The original "P1/P2 immediately triggered combat" explanation was too broad. Wuhuan usually has a visible pathing interval between clicking the task link and entering combat.
- The stronger root cause is `BattleRadarService`: after two missing combat detections it can mark `IN_COMBAT -> FREE`, then Wuhuan consumes the combat-exit signal and opens the task panel with `Alt+Q` while the user still visually sees combat.
- Wuhuan also allowed `BattleRadarService.checkAndSyncCombatState()` to run free-state first aid before Wuhuan's own post-combat recovery consumed the combat-exit signal, causing duplicate post-combat supply checks.

Changed files:

- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Removed the success-path `Alt+Q` close from Wuhuan P1/P2 pathing clicks. Failure/cleanup paths still close the task panel explicitly.
- Wuhuan now calls `battleRadarService.checkAndSyncCombatState(false)` and lets `wuhuan:postCombatRecovery` own post-combat first-aid/supply work.
- Battle exit now requires both repeated missing battle signals and a readable minimap coordinate, so a temporary loss of combat templates alone will not let Wuhuan open the task panel during combat.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Five-window startup is still intentionally serial until each window reaches pathing. If this feels too slow, the next design task is to split safe startup checks from input-sensitive preparation without reintroducing bag/window crossing.

### Xie Shuai - 2026-05-22 Summon skill exclusive cleanup

Status: completed

Goal:

- Ensure summon skill cleanup keeps the physical input permission for the whole open/inspect/delete/confirm pass, so other windows cannot interleave while it is maintaining summon skills.

Changed files:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `docs/ACTIVE_WORK.md`

Done:

- `cleanSummonSkillsOnce()` now wraps the whole cleanup pass in one `InputSequences.submitExclusiveAndWait("summonSkill:cleanOnce", ...)` transaction.
- Substeps now detect when they are already running on the input worker and call direct `InputProvider` operations instead of nesting another queue request.
- This direct-when-owned path covers panel open, extra-slot hover, skill-slot inspect, delete button click, and forget-confirm click.
- `uiCleanerService.cleanUpAll()` runs only after the summon cleanup transaction releases the input queue.
- Failure still returns `false`, so `AutoBattleTask` will not update the summon-clean timestamp and the next idle maintenance round can retry.
- Added a 40-second total deadline for one summon cleanup pass. If the business flow exceeds that deadline at a check point, it aborts with `false`, releases the exclusive input transaction, and leaves the cooldown timestamp untouched for retry.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-22 Team return service split

Status: completed

Goal:

- Split return-team handling out of `AutoBattleTask` and make the leader/member behavior explicit.

Changed files:

- `src/main/java/com/bot/dhxy/service/TeamReturnService.java`
- `src/main/java/com/bot/dhxy/config/BotProperties.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/resources/application.properties`
- `docs/ACTIVE_WORK.md`

Done:

- Added `TeamReturnService` as the shared team-maintenance capability for return-team handling; member clicks use `images/template/status/gui.png`, while leader-side wait detection uses `images/template/status/zhao.png`.
- Member/auto-battle behavior: if the return signal is present, click it through the input queue.
- Leader behavior: the return signal is only checked at task-defined safe points. The leader does not click the return button and does not release the task turn while waiting.
- Leader wait timing is configurable with `bot.dhxy.return-team-leader-wait-timeout-ms` and `bot.dhxy.return-team-leader-wait-poll-ms`.
- `AutoBattleTask` now delegates return-team handling to `TeamReturnService` instead of owning template/coordinate logic directly.
- Removed the generic `FiveRingTask` leader wait check because it could wait in the wrong location after battle.
- `XiuluoTask` now checks and waits for member return after the return item succeeds, which is the first safe point after returning to town.

Validation:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-05-26 停止后异常状态未清理

Status: completed

Goal:

- 处理 UI 中某个窗口任务已经失败结束后，用户点停止仍一直显示“异常”的状态语义问题。

Log finding:

- 最新 `logs/dhxy-console.log` 显示 `hwnd-3300F7A / 刑部ㄨ忍者` 在 00:08:35 已经结束：`window [hwnd-3300F7A] task finished: 修罗 -> FAILED`。
- 失败原因是 `navigateInCurrentMap:retry` 五个候选点击都没有移动，随后 `generic navigation to objective failed`。
- 00:09:18 用户点停止时，另外 4 个仍在自动战斗的队员立即 `STOPPED`；队长已经没有 active task，所以没有新的 task stopped 日志。
- 因此不是“停不下来”，而是失败后的 terminal `ERROR` 状态没有被停止命令清成 `STOPPED`。

Changed files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Done:

- 新增 `WindowRuntimeContext.markStoppedAfterTerminalStop(...)`。
- `WindowTaskRunner.stopCurrentTask()` 在没有 active task、但窗口处于 `ERROR` 或 `STOPPING` 时，会把窗口级状态改成 `STOPPED`。
- 保留原来的 `lastResult=FAILED` 和失败信息，方便详情面板继续看见上一次为什么异常。

Validation:

- `git diff --check` passed for touched Java files.
- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-05-25 修罗停止卡在目标文字识别

Status: completed

Goal:

- 处理 UI 点“停止”后修罗仍要等很久才真正停止的问题。
- 保持目标文字识别算法不变，只让长时间本地模板扫描能响应任务 stop token。

Log finding:

- `logs/dhxy-console.log` 显示 21:10:34 UI 已经请求停止 `hwnd-3300F7A`，`XiuluoTask` 也收到 stop requested。
- 任务直到 21:12:18 才退出，期间卡在 `ObjectiveTextRecognitionService` 的 objective map/template scan。
- 中间用户多次点启动，UI 正确刷新并过滤 busy/not accepting 窗口，所以表现成“启动没反应/次数没刷新”。

Changed files:

- `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`
- `docs/ACTIVE_WORK.md`

Done:

- `ObjectiveTextRecognitionService` 注入 `TaskExecutionContextHolder`。
- 在 objective 识别入口、地图模板扫描、坐标模板扫描、前景裁剪、glyph trim、foreground similarity 等长循环里加入 cooperative stop checkpoint。
- `TaskStopRequestedException` 不再被目标识别的 `catch (Exception)` 当普通识别失败吞掉，而是记录 stopped 日志后重新抛给 runner。
- 未修改修罗业务流程、目标识别阈值、模板匹配算法或窗口启动逻辑。

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java` passed.
- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NavigationService task-turn comments

Status: completed

Goal:

- Explain confusing high-frequency task-turn helpers in `NavigationService`.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added JavaDoc for `ensureTaskTurn(String source)` explaining that it acquires the task-level turn, not the physical input queue.
- Added JavaDoc for `releaseTaskTurnAfterPathing(String source)` explaining why navigation releases ownership once game auto-pathing starts.
- Clarified that later focused/state-mutating navigation actions must call `ensureTaskTurn(...)` to re-enter the business turn.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/service/NavigationService.java` passed.

### Tang De - 2026-05-26 NavigationService remove map internal wrapper

Status: completed

Goal:

- Remove the redundant `navigateToMapInternal(...)` wrapper split because both methods were private and had identical parameters.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Merged `navigateToMapInternal(String, boolean)` back into `navigateToMap(String, boolean)`.
- Kept latency tracking in `navigateToMap(...)` using `try/finally`, so early returns still emit `navigation.toMap` metrics.
- Preserved the existing navigation stages and comments; no route/search/retry behavior was changed.

Validation:

- `rg navigateToMapInternal` shows no remaining method/call.
- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/service/NavigationService.java` passed.

### Tang De - 2026-05-26 main-method input JavaDoc rule

Status: completed

Goal:

- Clarify the lightweight comment policy: high-frequency/main methods still need proper top-level input/output documentation.

Changed files:

- `AGENTS.md`
- `docs/ACTIVE_WORK.md`

Done:

- Added a mandatory rule that main/high-frequency methods must have JavaDoc explaining inputs and output.
- For each parameter, agents must state what it represents and include coordinate space, unit, and nullability when relevant.
- This is explicitly mandatory for navigation, OCR, input, window binding, task execution, and UI command entry methods.
- The broader policy remains lightweight: trivial helpers and obvious UI plumbing do not need forced heavy comments.

### Tang De - 2026-05-26 NavigationService high-frequency comments

Status: completed

Goal:

- Add concise internal comments to high-frequency navigation paths without changing behavior.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Documented the map-and-coordinate navigation stage split: cross-map route, current-map coordinate click, and arrival cleanup.
- Documented the `navigateToMapInternal(...)` loop stages: cached-map fast path, one-time unknown-map sync, first world-map route submission, movement wait/yield, dialog handling, OCR arrival check, and stuck retry policy.
- Kept the wrapper/internal separation intact; no navigation logic was changed.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/service/NavigationService.java` passed.

### Tang De - 2026-05-26 FiveRingTask mojibake cleanup

Status: completed

Goal:

- Remove the remaining mojibake from the whole FiveRingTask file after the LocationVisionService cleanup.
- Keep the change limited to readable strings/log messages and avoid task-flow changes.

Changed files:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Replaced garbled task/map/NPC display strings with readable text: `五环`, `长安`, `云游大师`.
- Rewrote garbled log lines and step display names into readable Chinese/English diagnostics.
- Removed broken emoji/mojibake fragments such as `馃`, `鈿`, `鈻`, `宺esult`, and `歳eason`.
- Re-scanned the full file for common mojibake patterns and found no remaining matches.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 LocationVisionService cleanup

Status: completed

Goal:

- Clean up mojibake comments/log-adjacent text in `LocationVisionService`.
- Move public API members above private helpers so the file reads public-first, private-detail-second.

Changed files:

- `src/main/java/com/bot/dhxy/vision/LocationVisionService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Fixed garbled Chinese comments in the class header, floor-template verification notes, and player-anchor width comment.
- Corrected the dungeon floor map matcher from garbled text to `.*[一二三四五六七八九十]+层$`.
- Moved `extractPlayerPhysicalAnchor(...)`, `extractPlayerAnchorMatch(...)`, and public record `PlayerAnchorMatch` above the private helper section.
- Removed redundant same-package imports.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/vision/LocationVisionService.java` passed.

### Tang De - 2026-05-26 lightweight code comment policy

Status: completed

Goal:

- Temporarily reduce comment/JavaDoc weight so agents can spend less context and token budget on obvious code while still documenting risky automation behavior.

Changed files:

- `AGENTS.md`
- `docs/ACTIVE_WORK.md`

Done:

- Replaced the previous heavy documentation rule with a lightweight policy.
- Public JavaDoc is now required only when behavior is non-obvious, externally reused, or safety-sensitive.
- Mandatory comments remain for input/focus/HWND binding, OCR/template fallback order, stop/pause/transaction behavior, config/debug switches, persisted formats, and coordinate-space conversions.
- Complex methods should have concise decision-point comments, not full SOP-style narration for every branch.
- Agents should document only the touched risky section and avoid broad unrelated documentation passes.

### Tang De - 2026-05-26 Xiuluo stop during location OCR

Status: completed

Goal:

- Diagnose why one selected Xiuluo window still took several seconds to stop after the UI stop command.
- Prevent a normal user stop from being reported as a task failure when it happens inside the Xiuluo objective-prepare transaction.

Log finding:

- Latest log showed the stop request at `00:56:31.371`.
- The slow window was the Xiuluo leader `hwnd-3300F7A / 刑部ㄨ忍者`.
- It exited at `00:56:37.689` after `LocationVisionService.scanCurrentLocation()` finished mini-map template, local OCR, and Baidu OCR fallback.
- Root cause: location scanning had no cooperative stop checkpoints before the slow OCR fallback stages, and the stopped transaction was later mapped to a generic Xiuluo hot-start failure.

Changed files:

- `src/main/java/com/bot/dhxy/vision/LocationVisionService.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added task stop checkpoints to `LocationVisionService.scanCurrentLocation()` before/after mini-map template scanning, coordinate-strip capture, local OCR, and Baidu OCR.
- Added an explicit checkpoint before Baidu OCR so a user stop does not enter the slow network/token fallback after stop has already been requested.
- Re-throw `TaskStopRequestedException` from the mini-map template helper instead of swallowing it as a generic template miss.
- Added a `STOPPED` Xiuluo hot-start state so `xiuluo:prepareObjectiveForPathing` can preserve `TaskTransactionResult.STOPPED` and return `TaskRunResult.STOPPED` instead of `FAILED`.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-23 Xiuluo return map-label verification

Status: completed

Goal:

- Confirm the Xiuluo leader has actually returned to town before checking the return-team signal.
- Reuse the mini-map coordinate strip capture, but compare the washed map-name label image instead of OCR or coordinate movement.

Changed files:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `src/main/java/com/bot/dhxy/service/MiniMapCoordinateReader.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/config/BotProperties.java`
- `src/main/resources/application.properties`
- `docs/ACTIVE_WORK.md`

Done:

- `GameStateUtil.detectMovementState()` already uses `MiniMapCoordinateReader.readCurrentCoordinate()` first; the old pixel diff is only a fallback when coordinate samples are insufficient.
- `MiniMapCoordinateReader.readCurrentMapLabelImage()` now returns a washed binary image of the mini-map coordinate strip's map-name label, cropped before the coordinate bracket.
- `GameStateUtil` now owns the reusable map-label verification helpers:
  - `captureCurrentMapLabelSnapshot(...)`
  - `isCurrentMapLabelChangedFrom(...)`
- `XiuluoTask.useReturnItem(...)` now reads a map-name label baseline through `GameStateUtil` before opening the bag and using the return item.
- Before reading the baseline, Xiuluo verifies it is no longer in combat and the main bag is not open.
- After the return item is used, Xiuluo polls the washed map-name label image until it differs from the baseline, then treats return-to-town as complete.
- This avoids OCR cost and avoids false success from a small accidental coordinate movement.
- During the post-return polling, if the main bag is still open, that sample is skipped so bag UI cannot pollute the location strip.
- If the map label does not change within the configured timeout, the return item step returns a retryable error instead of continuing to member-return waiting.
- Return verification now only needs timing config:
  - `bot.dhxy.xiuluo-return-verify-timeout-ms`
  - `bot.dhxy.xiuluo-return-verify-poll-ms`

Validation:

- `mvn -q -DskipTests compile` passed.

Follow-up:

- User observed auto-battle member windows stealing foreground during idle patrol.
- Log root cause: `UICleanerService.handleMaintenanceBroadcast(...)` called `DialogService.handleDialog(...)` every patrol, and `handleDialog` used queued/focused `dialog:detectType` even when no dialog existed.
- Added a no-focus precheck with `dialogService.detectDialogTypeNoFocus(...)`; auto-battle now only enters focused/click-capable dialog handling when a dialog is actually visible.
- `mvn -q -DskipTests compile` passed after this change.
- User clarified the real issue was the Xiuluo leader releasing the task turn before the agreed formal movement point.
- Log root cause: Xiuluo used generic `NavigationService.navigateToNPC(...)` while going to the accept NPC; generic navigation releases the task turn whenever pathing starts.
- Added `NavigationService.navigateToNPCWithoutTurnRelease(...)` and wired Xiuluo accept-NPC navigation to it, preserving old release behavior for other tasks.
- Xiuluo current-screen accept precheck now uses no-focus dialog detection before opening a transaction, so the normal "no current dialog" case no longer creates a failed transaction that can release the task turn.
- `mvn -q -DskipTests compile` passed after this change.

### He Li - 2026-05-22 Screenshot focus binding

Status: completed

Goal:

- Fix the root cause where multi-window `Robot` screenshots could capture another visible window even when the logical `windowId/base/hwnd` belonged to the current task window.

Changed files:

- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `docs/ACTIVE_WORK.md`

Done:

- `GameClientTracker.captureToFile(...)` and `captureToMemory(...)` now focus the current bound window inside the same `GlobalInputLock` immediately before calling the `Robot` screenshot provider.
- If the foreground hwnd is still not the current bound window after the focus attempt, the screenshot fails with `FOCUS_NOT_CONFIRMED` instead of capturing polluted visible pixels.
- Capture logs now include the current foreground hwnd, making it visible when a screenshot was taken while another window was still foreground.
- Tracker diagnostics now write `action=capture-focus` with expected hwnd, foreground before/after, and focus confirmation.
- BattleRadar now treats battle-region screenshot failure while already in `IN_COMBAT` as "evidence unavailable, keep combat state" so stale or polluted images cannot trigger a false combat exit.

Why:

- Input actions were already serialized and focused, but screenshots were only serialized by the global lock. Since the screenshot provider captures visible screen pixels, a different game window could cover the target window and pollute BattleRadar/minimap/task-panel scans.

Validation:

- `mvn -q -DskipTests compile` passed.
