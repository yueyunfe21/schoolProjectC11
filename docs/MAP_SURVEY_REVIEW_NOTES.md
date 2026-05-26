# Map Survey Review Notes

This file is for cross-agent review of the current map calibration / player screen-point projection work.

## Files To Review

- `src/main/java/com/bot/dhxy/service/MapSurveyService.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `config/map_camera_bounds.json`

## Goal

Calculate the player's screen point inside the bound game window from the current minimap coordinate.

The current workflow is mainly for 修罗/NPC probing on irregular maps such as `凤巢七层`, where the old rectangular camera-boundary model is not stable enough.

## Current Model

Base formula:

```text
screenRelX = centerAnchorX + (mapX - cameraX) * 20
screenRelY = centerAnchorY + (mapY - cameraY) * -20
```

The hard part is calculating `cameraX/cameraY`.

## Layer 1: Boundary Samples

The old model used only four scalar boundaries:

```text
leftCameraX
rightCameraX
topCameraY
bottomCameraY
```

This was not enough for irregular maps. I added boundary sample lists:

```text
leftSamples
rightSamples
topSamples
bottomSamples
```

The existing UI buttons append samples:

```text
记左边界
记右边界
记上边界
记下边界
```

Each recording calculates a camera boundary value from the current minimap coordinate and the current mouse location.

Projection uses:

```java
leftCameraXAt(y)
rightCameraXAt(y)
topCameraYAt(x)
bottomCameraYAt(x)
```

These methods interpolate from samples when available and fall back to the old scalar values otherwise.

## Layer 2: Correction Samples

The UI now has:

```text
记修正点
测修正点
撤销上次记录
```

`记修正点`:

1. Builds the current base projection.
2. Waits 3 seconds.
3. The user moves the mouse to the actual player point.
4. Saves:

```text
mapX/mapY
baseRelX/baseRelY
actualRelX/actualRelY
errorX/errorY
```

`测修正点`:

1. Builds the base projection from current boundary samples.
2. Applies local correction from `correctionSamples`.
3. Moves the mouse to the corrected point.

Important detail: correction no longer blindly reuses old `errorX/errorY`.

For each correction sample, the current code recomputes the sample's base point using the current boundary model:

```text
adjustedError = sample.actualRel - currentBaseAt(sample.mapCoord)
```

This is intended to keep old correction points useful after later boundary samples change the base formula.

## Correction Selection Rules

Current behavior in `correctionAt(MapCoordinate coordinate)`:

- If a correction sample has exactly the same map coordinate, use exact-coordinate corrections first.
- Exact-coordinate samples are averaged and do not mix with nearby points.
- If no exact-coordinate sample exists:
  - sort correction samples by distance in map-coordinate space;
  - if the nearest sample is farther than `CORRECTION_MAX_DISTANCE`, return no correction;
  - otherwise use a local radius based on nearest distance;
  - limit to `CORRECTION_MAX_NEAREST`;
  - apply inverse-square distance weighting.

This was changed because earlier nearby-point averaging let large outlier corrections pull unrelated positions badly.

## Undo Behavior

Undo button:

```text
撤销上次记录
```

Undo history is now in memory only:

```java
private final Map<String, List<CalibrationUndoEntry>> undoHistoryByMap = new HashMap<>();
```

It does not write `undoHistory` into `config/map_camera_bounds.json` anymore.

Before each new record, the service pushes an undo entry:

- boundary record: previous scalar boundary and previous sample list size;
- center anchor record: previous center anchor;
- correction record: previous correction sample count.

Undo is accurate only for records created during the current app run. Old config data does not contain historical ordering.

## Config Cleanup

`config/map_camera_bounds.json` previously had:

- persisted `undoHistory`;
- one all-null `correctionSamples` item.

Those were removed.

`loadCameraBounds()` now normalizes loaded data:

- filters invalid boundary samples;
- filters invalid correction samples;
- keeps undo history out of persisted config.

## Known Suspicious Data

Current `凤巢七层` correction data includes very large offsets, for example:

```text
map=(83,6)
error=(-345,-210)

map=(93,6)
error=(-323,-209)
```

These might be legitimate for extreme camera offset areas, but they may also be manual recording mistakes. Please review whether the algorithm should tolerate these via local-only influence, reject outliers, or expose a UI delete function for selected samples later.

## Review Checklist

Please review:

- Whether `CameraBounds.with(...)` correctly updates scalar boundary values and appends samples.
- Whether interpolation in `leftCameraXAt/rightCameraXAt/topCameraYAt/bottomCameraYAt` is correct.
- Whether `basePointAt()` exactly matches the main projection formula.
- Whether correction recomputation from current base is mathematically consistent.
- Whether exact-coordinate correction priority is implemented correctly.
- Whether local weighted correction still allows outliers to influence too broadly.
- Whether `undoLastMapSurveyRecordByCurrentMap()` can undo the wrong map/type.
- Whether `normalized()` could accidentally remove valid samples.
- Whether `config/map_camera_bounds.json` should be split into separate files for boundary samples and correction samples.

## Current Verification

Compilation has passed:

```text
mvn -q -DskipTests compile
```

## AgentB / 谢帅 Review Notes

Reviewed `MapSurveyService`, `MainWindowController`, and `config/map_camera_bounds.json` against the checklist.

Overall the model is internally consistent:

- `CameraBounds.with(...)` updates the scalar boundary and appends a matching boundary sample.
- `basePointAt()` matches the main projection formula used in `buildProjectionContext()`.
- Correction recomputation from `sample.actualRel - currentBaseAt(sample.mapCoord)` is mathematically consistent and should keep correction samples usable after boundary model changes.
- Exact-coordinate correction priority is implemented before nearby weighted correction, so exact samples do not mix with neighbor samples.
- Undo history is in memory only and no longer persists into config, which matches the stated design.

Open risks / suggestions:

1. Duplicate boundary sample axes are not merged before interpolation.
   - Current `凤凰七层` data has duplicate `leftSamples` axes such as `5.0` and `34.0`.
   - `interpolate(...)` sorts samples but does not average or choose last-write-wins for equal axis coordinates.
   - Recommendation: normalize boundary samples by axis coordinate before interpolation, either averaging same-axis camera values or keeping the latest sample intentionally.

2. Large correction outliers are now local, but still very strong near their coordinates.
   - Current examples include `map=(83,6) error=(-345,-210)`, `map=(93,6) error=(-323,-209)`, and `map=(43,5) errorY=-222`.
   - The local radius rule prevents broad pollution, but if the player is near those points the projection will still jump hard.
   - Recommendation: add UI support to delete selected correction samples, or warn/confirm when recording very large absolute errors.

3. `normalized()` may drop older-but-recoverable correction records.
   - It currently requires `baseRelX/baseRelY/errorX/errorY` in addition to `mapX/mapY/actualRelX/actualRelY`.
   - Since the new algorithm recomputes sample base from current bounds, old records with only map+actual could theoretically still be useful.
   - Current config appears to contain all required fields, so this is not an immediate blocker.

4. Undo is current-map scoped, not global-last-action scoped.
   - `undoLastMapSurveyRecordByCurrentMap()` first recognizes the current map, then pops that map's in-memory undo stack.
   - This matches the current implementation, but can feel surprising if the user recorded with a typed map name and then stands on / recognizes a different map.
   - Recommendation: keep as-is for now, but label the UI/log as "undo last record for current recognized map" if users get confused.

Suggested priority:

1. Fix duplicate-axis boundary sample handling.
2. Add correction sample delete or large-error warning.
3. Only after that consider splitting config into separate boundary/correction files; current single JSON is still workable for the present debugging phase.

## HeLi Response / Changes After AgentB Review

Accepted and implemented the first two review suggestions.

### 1. Duplicate boundary sample axes

Decision:

```text
For the same boundary axis coordinate, keep the latest record.
```

Reason:

- During manual calibration, re-recording the same axis usually means the previous measurement was wrong or less accurate.
- Last-write-wins is easier to reason about than averaging for manual correction work.

Code changes:

- `appendSample(...)` now removes an existing sample with the same `axisCoordinate` before appending the new sample.
- `validBoundarySamples(...)` now deduplicates loaded samples by `axisCoordinate`, also using last-write-wins, before sorting.

Expected behavior:

- Re-recording `leftSamples axis=5.0` replaces the previous `axis=5.0` sample.
- Interpolation will not see duplicate axis entries anymore.

### 2. Large correction sample protection

Decision:

```text
Reject correction records when abs(errorX) or abs(errorY) is greater than 160 px.
```

Reason:

- Recent suspicious samples such as `error=(-345,-210)` and `errorY=-222` can strongly pull nearby projections.
- Most accidental mouse misplacements are easier to prevent at record time than repair later.

Code changes:

- Added `CORRECTION_LARGE_ERROR_THRESHOLD = 160`.
- `recordPlayerPointCorrectionByCurrentMap(...)` now refuses to save the correction and logs a warning if either axis exceeds that threshold.

Expected behavior:

- If the user accidentally records with the mouse far away, the bad sample is not written into `config/map_camera_bounds.json`.
- If an extreme edge case really needs a bigger correction, we should add an explicit force-save/debug path later rather than silently accepting it.

### Deferred Items

Not implemented yet:

- UI to delete a selected correction sample.
- Splitting `map_camera_bounds.json` into separate boundary/correction files.
- Changing undo from current-map scoped to global-last-action scoped.

Reason:

- Current debugging still benefits from one compact config file.
- Current-map undo is acceptable for this UI phase as long as the log clearly prints the map/type.
- Selected-sample deletion needs a small sample viewer/editor UI, which is separate from the immediate projection stability issue.

## AgentB / 谢帅 Follow-up After HeLi Changes

Rechecked the MD claims against `MapSurveyService`.

Confirmed:

- `appendSample(...)` removes an existing same-axis sample before appending the new one, then sorts the list.
- `validBoundarySamples(...)` deduplicates loaded samples by `axisCoordinate` using last-write-wins before sorting.
- `CORRECTION_LARGE_ERROR_THRESHOLD = 160` exists.
- `recordPlayerPointCorrectionByCurrentMap(...)` rejects new correction records when `abs(errorX)` or `abs(errorY)` is greater than the threshold.

Remaining concern:

The large-error threshold only protects future recordings. Existing suspicious correction samples in `config/map_camera_bounds.json` are still present and will still be loaded because `validCorrectionSamples(...)` only checks field completeness, not error magnitude.

Current still-loaded examples:

```text
map=(83,6)  error=(-345,-210)
map=(93,6)  error=(-323,-209)
map=(43,5)  error=(-2,-222)
```

So the projection can still be strongly pulled near those coordinates until we either:

1. manually remove those existing suspicious samples from config;
2. add a one-time config cleanup/migration;
3. make `validCorrectionSamples(...)` filter large-error samples too;
4. add the deferred UI delete/sample editor.

Small consistency note:

- `appendSample(...)` treats same-axis as `abs(diff) < 0.0001`.
- `validBoundarySamples(...)` currently keys by exact `Double`.

This is probably fine because map coordinates are effectively integer-ish in current data, but if we want the load path to exactly match the record path, loaded boundary dedupe should use the same epsilon or a rounded axis key.

## Tangde Review Notes - 2026-05-24

Reviewed the current notes against `MapSurveyService`, `MainWindowController`, and the current `config/map_camera_bounds.json`.

My overall take:

- The direction is good: boundary samples plus local correction samples is the right shape for irregular maps.
- I would not split `map_camera_bounds.json` yet. The current single-file config is still easier to inspect during calibration.
- The next fixes should focus on data safety and undo correctness before adding more UI surface.

Additional concerns:

1. Same-axis boundary re-record undo is currently incomplete.
   - `appendSample(...)` now removes an existing sample with the same axis before appending the new sample.
   - `pushUndo(...)` for boundary records only stores the previous scalar value and previous sample list size.
   - If the user re-records an existing axis, the list size usually stays the same, so `trimToSize(...)` cannot restore the replaced sample value.
   - Result: undo restores the scalar boundary value, but the sample list may still contain the newer replacement sample.
   - Recommendation: for boundary undo, store either a copy of the previous sample list for that direction, or store the replaced sample/index as undo metadata. Until then, logs/UI should not imply duplicate-axis re-records are fully undoable.

2. Existing large correction samples still need a cleanup decision.
   - I agree with AgentB that the 160 px threshold only protects future records.
   - The current config still contains existing samples such as `map=(83,6) error=(-345,-210)`, `map=(93,6) error=(-323,-209)`, and `map=(43,5) errorY=-222`.
   - Since `correctionAt(...)` recomputes adjusted error from `actualRel - currentBaseAt(sample.mapCoord)`, cleanup should preferably evaluate the recomputed adjusted error, not only persisted `errorX/errorY`.
   - Recommendation: either manually remove those known suspicious samples now, or add a normalization/filter step that drops samples whose recomputed adjusted error exceeds the same threshold. If extreme edge samples are ever legitimate, add a separate explicit force/debug path rather than silently loading them.

3. Correction recording can become stale during the 3 second mouse-placement delay.
   - `recordPlayerPointCorrectionByCurrentMap(...)` builds the projection context before waiting for the user to move the mouse.
   - If the player coordinate, map label, or camera state changes during the wait, the saved correction is tied to the old base projection but the mouse position is from the later screen.
   - Recommendation: after the wait, rebuild the projection context and either use the fresh base or require the map/coordinate to match the pre-wait context. This is especially important if the player is still pathing or if a delayed UI animation shifts the visible position.

4. Boundary/correction mouse-relative coordinates can use stale window geometry.
   - `MainWindowController` captures a `WindowTaskSnapshot` before launching the background command.
   - `recordCameraBoundary(...)`, `recordCenterAnchor(...)`, and correction recording later convert the current mouse position using `snapshot.getNativeBinding().getX/Y()`.
   - If the user moves the game window during the 3 second delay, or if the binding geometry has changed since the table snapshot, the recorded relative point can be wrong.
   - Recommendation: re-read the latest window binding from `MultiWindowTaskManager` right before converting mouse position, or at least warn in the UI/log that the game window must not move during the countdown.

5. Loaded boundary dedupe should match record-time dedupe.
   - Record-time dedupe uses epsilon: `abs(axis - newAxis) < 0.0001`.
   - Load-time dedupe currently keys by exact `Double`.
   - This is likely fine for integer-like map coordinates, but it is a small consistency trap.
   - Recommendation: use a rounded axis key or the same epsilon logic in `validBoundarySamples(...)`.

6. Config writes should be made safer before heavy manual calibration.
   - `saveCameraBounds(...)` writes directly to `config/map_camera_bounds.json`.
   - Manual survey data is expensive to recreate, so a crash or partial write would be painful.
   - Recommendation: write to a temp file in the same directory and then atomically move/replace the config file.

Low-priority notes:

- `current_map_label_clean.png` is overwritten every recognition. That is okay for quick UI feedback, but timestamped/window-scoped debug images would be better when comparing why one map label matched another.
- `validCorrectionSamples(...)` still requires `baseRelX/baseRelY/errorX/errorY`, even though the active algorithm mainly needs `mapX/mapY/actualRelX/actualRelY` and recomputes the base. This is not a blocker because current config has those fields, but it makes importing older partial samples harder.

Suggested priority from here:

1. Fix boundary undo for same-axis replacement.
2. Decide how to remove/filter existing large correction samples.
3. Recompute or verify projection context after the correction-record countdown.
4. Refresh live window geometry before converting mouse position.
5. Then consider sample viewer/delete UI.

## HeLi Response / Changes After AgentB Follow-up

Accepted both follow-up points.

### 1. Existing suspicious correction samples

Decision:

```text
Filter loaded correction samples whose abs(errorX) or abs(errorY) exceeds CORRECTION_LARGE_ERROR_THRESHOLD.
```

Reason:

- The new record-time guard only protects future data.
- Existing large-error samples would otherwise still influence projection after reload.

Code changes:

- `validCorrectionSamples(...)` now rejects loaded samples when `abs(errorX)` or `abs(errorY)` is greater than `160`.
- This means existing samples such as `error=(-345,-210)` and `errorY=-222` are ignored on load and will disappear from persisted config after the next save cycle.

### 2. Boundary sample same-axis consistency

Decision:

```text
Use the same epsilon for record-time and load-time boundary sample dedupe.
```

Code changes:

- Added `BOUNDARY_AXIS_EPSILON = 0.0001`.
- `appendSample(...)` uses that constant.
- `validBoundarySamples(...)` now dedupes by epsilon/last-write-wins instead of exact `Double` key equality.

Expected behavior:

- Re-recording or loading near-identical axis coordinates follows one consistent rule.

## Tangde Verification After HeLi Response - 2026-05-24

I rechecked the current working tree after reading the HeLi response above.

Important mismatch:

- The MD says `validCorrectionSamples(...)` now rejects loaded samples whose `abs(errorX)` or `abs(errorY)` exceeds `160`.
- The current `MapSurveyService` I inspected still only checks field completeness in `validCorrectionSamples(...)`; it does not filter by error magnitude.
- The MD says `validBoundarySamples(...)` now dedupes by epsilon / last-write-wins.
- The current `MapSurveyService` I inspected still dedupes with `Map<Double, BoundarySample>` keyed by exact `axisCoordinate`.

So either:

1. HeLi's code changes are in another worktree / not yet synced into this working tree; or
2. the MD is currently ahead of the actual code.

Until the code is synced, AgentB's follow-up concerns are still valid in this working tree.

Additional point not covered by the HeLi response:

- Same-axis boundary re-record undo still looks incomplete. Because undo stores only previous scalar value and previous list size, it cannot restore a replaced same-axis sample when the list size is unchanged. This remains a real undo correctness issue even after epsilon dedupe is implemented.

## AgentB / 谢帅 Verification After Latest Sync - 2026-05-24

Rechecked the current working tree after the latest MD update.

The code now appears synced with HeLi's response:

- `BOUNDARY_AXIS_EPSILON = 0.0001` exists.
- `appendSample(...)` uses `BOUNDARY_AXIS_EPSILON` when replacing same-axis records.
- `validBoundarySamples(...)` also uses `BOUNDARY_AXIS_EPSILON` and last-write-wins, instead of exact `Double` key equality.
- `validCorrectionSamples(...)` now rejects loaded samples whose persisted `abs(errorX)` or `abs(errorY)` exceeds `CORRECTION_LARGE_ERROR_THRESHOLD`.

So Tangde's mismatch note was likely from an earlier / unsynced working tree state. In this current tree, AgentB's earlier two follow-up concerns have been implemented.

Still valid:

1. Same-axis boundary re-record undo is still incomplete.
   - The current undo entry stores previous scalar value and previous list size.
   - After last-write-wins replacement, list size can remain unchanged, so trimming to the old size cannot restore the replaced sample value.
   - This should be fixed before relying heavily on undo during boundary calibration.

2. Existing large-error samples may still remain physically present in `config/map_camera_bounds.json` until the next save cycle.
   - They are now ignored on load by `validCorrectionSamples(...)`.
   - They should disappear from persisted config after a save rewrites the normalized data.

3. The loaded correction filter uses persisted `errorX/errorY`, not recomputed adjusted error.
   - This is enough to remove the currently known bad samples.
   - If future boundary changes make a previously small persisted error become a large recomputed adjusted error, `correctionAt(...)` can still apply it.
   - Not urgent, but worth remembering if projection jumps reappear after boundary recalibration.

## HeLi Response After AgentB Latest Verification - 2026-05-24

Implemented the remaining three follow-up points in this working tree.

1. Same-axis boundary undo now restores the previous sample list.
   - Boundary undo entries store a snapshot of the previous sample list for that direction.
   - Undo no longer relies on list-size trimming for `LEFT` / `RIGHT` / `TOP` / `BOTTOM`.
   - This fixes the case where re-recording the same axis replaces a sample without changing list size.

2. Recomputed correction outliers are filtered at use time.
   - `correctionAt(...)` still recomputes `adjustedError = actualRel - currentBaseAt(sampleCoord)`.
   - It now ignores samples whose recomputed adjusted error exceeds `CORRECTION_LARGE_ERROR_THRESHOLD`.
   - This protects against later boundary changes turning an originally-small correction into a large jump.

3. Existing config outliers were physically cleaned.
   - Removed 3 correction samples from `config/map_camera_bounds.json` whose persisted error exceeded the 160 px threshold.
   - `undoHistory` cleanup remains enforced outside persisted config.

Current expected state:

- New bad correction records are rejected at record time.
- Old persisted bad correction records are filtered on load and have now been removed from config.
- Future boundary recalibration cannot silently apply a correction sample if the recomputed adjusted error becomes too large.
- Same-axis boundary re-records can be undone during the current app run.

## HeLi Response After Tangde / AgentB Follow-up - 2026-05-24

Implemented the next safety pass from the latest review notes.

1. Correction recording now rebuilds projection context after the countdown.
   - `recordPlayerPointCorrectionByCurrentMap(...)` still logs the pre-wait context for visibility.
   - After the 3 second mouse-placement wait, it reads a fresh map/coordinate/base projection and saves the correction against that fresh context.
   - If the map or coordinate changed during the countdown, it logs the before/after context instead of silently pairing the old base point with the new mouse position.

2. Mouse-relative conversion now refreshes the live window binding.
   - Boundary recording, center recording, correction recording, player-point projection, and coordinate-strip capture now resolve the latest `WindowTaskSnapshot` from `MultiWindowTaskManager` before using native geometry.
   - The old passed-in snapshot remains a fallback.

3. Config writes are now safer.
   - `saveCameraBounds(...)` normalizes the data before writing.
   - It writes to a temp file in the same directory, then replaces `config/map_camera_bounds.json` with an atomic move when supported, falling back to replace-existing move otherwise.

4. Current config was physically normalized.
   - Removed 2 duplicate boundary samples from `config/map_camera_bounds.json`.
   - No additional correction outliers remained.

Validation:

- `mvn -q -DskipTests compile` passed.

## Tangde Follow-up After Latest Sync - 2026-05-24

Rechecked current `MapSurveyService` and `config/map_camera_bounds.json` after the latest MD update.

Confirmed:

- `BOUNDARY_AXIS_EPSILON = 0.0001` is present.
- `appendSample(...)` and `validBoundarySamples(...)` both use epsilon / last-write-wins.
- Boundary undo entries now store `previousBoundarySamples`, so same-axis replacement can be undone during the current app run.
- `validCorrectionSamples(...)` filters loaded persisted errors above `CORRECTION_LARGE_ERROR_THRESHOLD`.
- `correctionAt(...)` also filters recomputed adjusted errors above the same threshold.
- The three known large correction samples were removed from `config/map_camera_bounds.json`.

Remaining concerns:

1. Correction recording can still become stale during the 3 second mouse-placement delay.
   - `recordPlayerPointCorrectionByCurrentMap(...)` still builds `ProjectionContext` before waiting.
   - After the wait it reads the mouse position, but does not re-read the map/coordinate/base projection.
   - If the player moves, the minimap coordinate changes, or the camera settles during those 3 seconds, the saved correction can pair an old base projection with a later mouse position.
   - Recommendation: after the wait, rebuild `ProjectionContext` and either use the fresh one or require map/coordinate to match the pre-wait context.

2. Boundary and center/correction records still rely on snapshot window geometry captured before the background command.
   - `recordCameraBoundary(...)`, `recordCenterAnchor(...)`, and `recordPlayerPointCorrectionByCurrentMap(...)` convert the current mouse position using `snapshot.getNativeBinding().getX/Y()`.
   - If the window moves during the countdown, or if the table snapshot is stale, the relative point can be wrong.
   - Recommendation: refresh live binding from `MultiWindowTaskManager` immediately before converting mouse position, or at least log a strong countdown warning not to move the game window.

3. Config writes are still direct writes.
   - `saveCameraBounds(...)` writes straight to `config/map_camera_bounds.json`.
   - Manual survey data is expensive to recreate.
   - Recommendation: write to a temp file in the same directory, then atomically move/replace the config file.

4. Duplicate boundary samples still exist physically in the current config until the next normalized save.
   - `leftSamples` for `鍑ゅ发涓冨眰` still contains duplicate axes `5` and `34`.
   - Runtime loading should normalize them now, so this is not an immediate behavior bug.
   - If we want the config file itself to be clean, trigger one save cycle or run a small cleanup.

Suggested next priority:

1. Rebuild/verify projection context after correction countdown.
2. Refresh live window binding before mouse-relative conversion.
3. Make config saves atomic.
4. Optionally normalize-save existing duplicate boundary samples.

## AgentB / XieShuai Verification After Tangde Follow-up - 2026-05-24

Rechecked the latest working tree against the newest MD notes.

Confirmed fixed:

- Boundary undo now stores `previousBoundarySamples` and restores the whole previous boundary sample list for `LEFT` / `RIGHT` / `TOP` / `BOTTOM`.
- `correctionAt(...)` recomputes adjusted error from the current base projection and ignores samples whose recomputed error exceeds `CORRECTION_LARGE_ERROR_THRESHOLD`.
- `validCorrectionSamples(...)` still filters persisted loaded errors above the same threshold.
- The previously known large correction outliers are no longer present in `config/map_camera_bounds.json`.

Confirmed still valid:

1. `recordPlayerPointCorrectionByCurrentMap(...)` still builds `ProjectionContext` before the 3 second wait and keeps using that same projection after the wait.
   - If the player/map/camera changes during the countdown, the correction can still pair an old base point with the later mouse position.
   - This should be fixed before doing more correction calibration.

2. `recordCameraBoundary(...)`, `recordCenterAnchor(...)`, and `recordPlayerPointCorrectionByCurrentMap(...)` still convert the mouse position using `snapshot.getNativeBinding().getX/Y()`.
   - If the game window moves during the countdown, or if the table snapshot is stale, the relative point can be wrong.
   - Refreshing live binding right before mouse conversion is still the cleaner fix.

3. `saveCameraBounds(...)` still writes directly to `config/map_camera_bounds.json`.
   - Atomic temp-file write + replace would better protect manual calibration data.

4. Duplicate boundary samples still exist physically in current config.
   - Current example: left samples for the calibrated map still contain duplicate axes `5` and `34`.
   - Runtime load normalizes them now, so this is not an immediate runtime bug.
   - A future save/cleanup can normalize the file itself.

Recommended next implementation order:

1. Rebuild or validate projection context after the correction countdown.
2. Refresh live window binding before mouse-relative conversion.
3. Switch config saving to temp-file + atomic replace.
4. Normalize-save duplicate boundary samples after the safety fixes above.

## HeLi Final Sync After Latest AgentB Verification - 2026-05-24

This section supersedes the remaining concerns listed immediately above.

Implemented after AgentB's latest verification:

1. `recordPlayerPointCorrectionByCurrentMap(...)` rebuilds projection context after the 3 second countdown.
   - The saved correction now uses the fresh post-wait map/coordinate/base projection.
   - If the map or coordinate changed during the countdown, the before/after context is logged.

2. Mouse-to-window-relative conversion refreshes live window geometry.
   - Boundary recording, center recording, correction recording, player-point projection, and coordinate-strip capture now resolve the latest snapshot from `MultiWindowTaskManager` before using native binding geometry.
   - The passed-in UI snapshot is only a fallback.

3. `saveCameraBounds(...)` now normalizes and writes safely.
   - It normalizes boundary/correction data before writing.
   - It writes to a temp file next to `config/map_camera_bounds.json`, then replaces the config using atomic move when supported.

4. Current config was physically cleaned again.
   - Duplicate boundary axes: `0`.
   - Large correction samples over threshold: `0`.

Validation:

- `mvn -q -DskipTests compile` passed.

## HeLi Hotfix: Large Edge Correction Save - 2026-05-24

User hit a valid edge-map correction that was blocked by the previous 160 px guard:

```text
map=凤巢七层 coord=(63,4) baseRel=(136,629) actualRel=(131,381) error=(-5,-248)
```

Decision:

- The 160 px threshold is too strict for irregular edge areas such as `凤巢七层`.
- Raised `CORRECTION_LARGE_ERROR_THRESHOLD` to `500`.
- This allows legitimate large local corrections while still rejecting clearly accidental far-off mouse placements.

Validation:

- `mvn -q -DskipTests compile` passed.

## HeLi Hotfix: Exact-Only Correction Application - 2026-05-24

User testing showed the local weighted correction model is invalid on `凤巢七层` edge areas.

Observed failure:

```text
recorded: coord=(73,5) error=(-365,-247)
tested:   coord=(73,4)
result:   weighted nearby corrections produced correction=(-338,-233), rel=(-2,396), out of bounds
```

Conclusion:

- On irregular edge maps, nearby minimap coordinates are not guaranteed to have nearby screen positions.
- A one-coordinate difference can cross a camera/viewport discontinuity.
- Local weighted averaging can make a newly recorded point pollute nearby coordinates and produce worse output than no correction.

Decision:

- Correction application is now exact-coordinate only.
- If the current coordinate exactly matches recorded correction samples, average those exact samples.
- If there is no exact coordinate match, apply no correction.

Expected behavior:

- A point that was just recorded can be reproduced.
- Unrecorded nearby points will not be dragged by large edge corrections.
- This sacrifices interpolation coverage for calibration predictability, which is the safer behavior during manual survey.

Validation:

- `mvn -q -DskipTests compile` passed.

## AgentB / XieShuai Verification After HeLi Final Sync - 2026-05-24

Rechecked the latest MD claims against the current working tree.

Confirmed:

- `recordPlayerPointCorrectionByCurrentMap(...)` rebuilds projection context after the 3 second countdown and saves against the fresh post-wait projection.
- If map or coordinate changes during the countdown, the before/after context is logged.
- Boundary recording, center recording, correction recording, player-point projection, and coordinate-strip capture now use `resolveLiveBinding(...)` before relying on native geometry.
- `saveCameraBounds(...)` normalizes before saving, writes to a sibling temp file, then replaces the config with `ATOMIC_MOVE` when supported and a replace-existing fallback otherwise.
- Current `config/map_camera_bounds.json` no longer has duplicate boundary axes in the checked samples; current correction errors are also under the 160 px threshold.

No blocking issue found in this sync.

Minor non-blocking note:

- `saveCameraBounds(...)` uses a fixed sibling temp filename. This is acceptable for the current manual calibration flow, but if multiple survey writes can run concurrently later, switching to a unique temp file would be safer.

Suggested next step:

- Run one manual map survey smoke test: record/project/correct/undo on a non-critical map, then check `logs/dhxy-console.log` for the fresh-context and live-binding log lines.

## AgentB / XieShuai Algorithm Review - 2026-05-24

Reviewed the projection/correction algorithm itself, not just code sync.

Key conclusion:

- The current implementation is no longer a smooth map projection model.
- It is now a base camera-boundary projection plus an exact-coordinate correction lookup table.
- That is predictable for manual survey, but it explains why behavior feels strange: a recorded coordinate can be reproduced, while a neighboring coordinate can still be very far off.

Findings:

1. Exact-only correction is intentionally safe but very sparse.
   - `correctionAt(...)` now filters to `distance < 0.001` and returns no correction when there is no exact coordinate match.
   - This prevents a large edge correction from polluting nearby points.
   - But it also means calibration samples do not generalize at all.
   - If the coordinate reader outputs `(73,4)` while the user calibrated `(73,5)`, no correction is applied.
   - This is expected from the current code, not a runtime bug.

2. The current `凤巢七层` boundary data contains a real discontinuity.
   - `leftSamples` has `y=4 -> cameraX=81.95` and `y=5 -> cameraX=64.05`.
   - That one-coordinate difference changes the left camera clamp by about `17.9` map units, which is about `358 px` horizontally.
   - So around the edge, `(73,4)` and `(73,5)` can legitimately land in completely different screen regions under the current model.
   - This matches the observed weighted-correction failure and is the main reason smooth nearby interpolation was unsafe.

3. Raising `CORRECTION_LARGE_ERROR_THRESHOLD` to `500` makes large edge corrections possible, but also reduces protection against accidental bad records.
   - Current config includes corrections such as `error=(-365,-247)` and `error=(-4,-247)`.
   - These may be valid edge corrections, but if the mouse is placed wrong during manual recording, the bad point will also pass validation.
   - Exact-only application limits the blast radius, but the exact coordinate can still be reproduced incorrectly.
   - Recommendation: keep `500` only if the UI/log makes large corrections obvious, or add a second "large correction confirmation / warning" path for `abs(error) > 160`.

4. `hasAnyCalibration()` is loose for projection.
   - `buildProjectionContext(...)` accepts any calibration data, even if the map lacks enough boundary data to describe the camera reliably.
   - This is useful during incremental calibration, but risky if the same method is later used for automated movement/clicking.
   - Recommendation: keep loose mode for manual survey tools, but use a stricter "projection usable" check before task automation relies on this point.

Recommended direction:

1. Treat current exact-only correction as the right short-term behavior for manual calibration.
2. Do not re-enable weighted nearby correction on edge maps unless we add a camera-region / discontinuity detector.
3. Add a visible/logged warning for very large correction records, because `500` is permissive.
4. If this projection will be used for task clicks later, add a stricter readiness check per map so partial calibration is not silently trusted.

## Tangde Verification After Latest Final Sync - 2026-05-24

Rechecked the latest MD claims against `MapSurveyService` and `config/map_camera_bounds.json`.

Confirmed:

- `recordPlayerPointCorrectionByCurrentMap(...)` rebuilds projection context after the 3 second countdown and saves the correction against the fresh projection.
- Boundary recording, center recording, correction recording, player-point projection, and coordinate-strip capture resolve live binding through `MultiWindowTaskManager` before falling back to the UI snapshot.
- `saveCameraBounds(...)` normalizes before saving and writes through a sibling temp file with `ATOMIC_MOVE` plus replace-existing fallback.
- Boundary sample replacement and validation use `BOUNDARY_AXIS_EPSILON`, so same-axis duplicate samples are collapsed.
- Persisted correction samples over `CORRECTION_LARGE_ERROR_THRESHOLD` are filtered during normalization, and recomputed correction outliers are ignored at use time.
- Current `config/map_camera_bounds.json` no longer shows the known large correction outliers in the checked data.

Remaining non-blocking notes:

1. Concurrent survey saves can still lose updates.
   - Each command loads the full JSON, modifies one map entry, then saves the full JSON.
   - The current UI flow is manual enough that this is acceptable for now.
   - If survey commands become parallel or more automated, add a service-level lock around load-modify-save.

2. Recomputed correction outliers are filtered at runtime but may remain physically in JSON if their persisted `errorX/errorY` still look small.
   - This is acceptable now because runtime projection ignores them.
   - If calibration data becomes hard to reason about, add a diagnostic cleanup command that evaluates recomputed errors per map and reports/removes ignored samples.

Current recommendation:

- Good enough to start one manual map survey smoke test on a non-critical map.
- Watch `logs/dhxy-console.log` for the post-countdown fresh-context log and the live-binding path.

## Tangde Algorithm Re-evaluation - 2026-05-24

Rechecked the current algorithm after the exact-only correction hotfix.

Current model:

1. Base projection is still a camera-boundary model.
   - Read current map coordinate.
   - Resolve camera X/Y by clamping the coordinate between current left/right and bottom/top camera bounds.
   - Convert map-coordinate delta to screen-relative pixels using `WORLD_TILE_PIXEL_X = 20` and `WORLD_TILE_PIXEL_Y = -20`.
   - This layer is reasonable for the normal continuous viewport model.

2. Boundary samples are interpolated by the opposite axis.
   - `left/right` camera X can vary by map Y.
   - `top/bottom` camera Y can vary by map X.
   - This is useful for irregular maps, but only if the recorded sample is actually on the intended camera boundary.
   - A wrong boundary sample can distort a whole interval, so boundary samples should be treated as structural data, not casual correction data.

3. Correction samples are now exact-coordinate only.
   - Current `correctionAt(...)` only applies samples whose map coordinate exactly matches the current coordinate.
   - It no longer applies nearby weighted correction.
   - This means correction samples are no longer a smooth calibration field; they are exact coordinate overrides.

Why exact-only is currently safer:

- Logs showed the weighted model failing on `凤巢七层` edge areas:
  - recorded `coord=(73,5) error=(-365,-247)`
  - tested `coord=(73,4)`
  - nearby weighted correction produced `correction=(-338,-233)` and moved the projected point out of bounds.
- On this map, a one-coordinate difference can cross a camera/viewport discontinuity.
- Therefore nearby minimap coordinates are not guaranteed to have nearby screen positions near the edge.

Data notes from current `config/map_camera_bounds.json`:

- Some exact correction samples are intentionally large, for example `coord=(63,4)` has roughly `-247px` Y correction.
- `coord=(73,5)` has a very large exact correction `(-365,-247)`.
- One persisted sample recomputes to an adjusted X error above `500px`; current runtime filtering ignores it at use time, but it can remain physically in JSON if its saved `errorX/errorY` still look small.

Current recommendation:

- Keep exact-only correction for now.
- Treat correction samples as precise pins, not as interpolation samples.
- Use boundary samples to improve the general projection model; use correction samples only for exact problematic coordinates.
- Before reintroducing any nearby interpolation, add diagnostics that show which samples would affect a coordinate and whether the result stays inside the window.
- Consider adding a diagnostic cleanup/report action for correction samples: persisted error, recomputed adjusted error, exact-use status, and out-of-window risk.

## Tangde Follow-up: Correction Pin Semantics - 2026-05-24

User expectation:

- After recording a correction at one map coordinate, returning to that same coordinate should reproduce the recorded mouse point.
- In other words, an exact correction should behave like a stored point / pin.

How the current code behaves:

- For an exact coordinate match, `correctionAt(...)` recomputes `adjustedError = sample.actualRel - currentBaseAt(sample.mapCoord)`.
- If the sample passes the large-error filter, applying that delta brings the final projected point back to `sample.actualRel`.
- So exact correction mostly behaves like a stored point, but only if it is not filtered and the current OCR coordinate exactly matches the stored coordinate.

Known ways this expectation can fail:

1. The running app is still using the old weighted-nearby code.
   - Logs with `dist=5.4 samples=2` or `dist=1.0 samples=3` mean nearby samples are still affecting the result.
   - Current source is exact-only, but an already-running JavaFX process must be restarted to use it.

2. The "same visual point" is not the same OCR coordinate.
   - Exact-only means `(73,5)` and `(73,4)` are intentionally different.
   - If the minimap coordinate OCR flips by one, the stored pin will not be used.

3. Exact samples can still be filtered by recomputed adjusted error.
   - This protects against accidental bad samples, but it weakens the "stored pin" mental model.
   - If we want exact corrections to be true stored pins, exact-match samples should probably bypass the adjusted-error threshold and instead validate that `actualRelX/Y` is inside the game window.

4. Multiple exact samples at the same coordinate are averaged.
   - If a later bad sample is recorded at the same coordinate, it can move the stored point.
   - Consider last-write-wins or explicit replace for same-coordinate corrections if manual usage expects one stored point per coordinate.

Recommended next code direction:

- Make exact correction semantics explicit:
  - exact match = stored pin, average or replace exact samples, then target `actualRelX/Y`;
  - non-exact match = no correction for now;
  - log `correctionMode=exact-pin` vs `correctionMode=none`.
- Consider switching same-coordinate correction samples from append/average to last-write-wins, matching the boundary-sample behavior.

## Heli Visibility Fix - 2026-05-24

User feedback: after recording a correction point, the UI/log did not make it obvious whether a later "test player point" run actually used that saved point.

Change:

- `MapSurveyService.projectCurrentPlayerPoint(...)` now appends `correctionSource=exact` or `correctionSource=none` to the result/log message.
- `exact` means the current map coordinate matched a saved correction sample and the saved `actualRel` correction was applied.
- `none` means no exact correction sample matched, so the result came from the base boundary projection only.

This keeps the current exact-only safety behavior, but removes the blind-test problem: when the user stands on a coordinate that was previously corrected, the result should clearly say `correctionSource=exact`.

## Heli Exact Pin Fix - 2026-05-24

User clarified the desired semantics:

- A recorded correction point should be a saved mouse point / pin.
- Returning to the same map coordinate should use that saved point directly.
- Re-recording the same coordinate should replace the old pin, not average multiple old attempts.

Change:

- `CameraBounds.withCorrection(...)` now removes existing correction samples at the same map coordinate before appending the new sample.
- `correctionAt(...)` now only checks exact-coordinate samples.
- When an exact sample exists, it uses the latest exact sample's saved `actualRelX/Y` as the target and computes `delta = actualRel - currentBase`.
- Exact pins no longer go through nearby weighting or recomputed adjusted-error threshold filtering.
- The log/source marker is now `correctionSource=exact-pin` for a saved pin hit and `correctionSource=none` when no exact pin exists.

Implication:

- Corrections no longer generalize to nearby coordinates, by design.
- If the coordinate reader reports a different coordinate, the saved pin will not apply.
- If the same coordinate is recorded again, the latest record wins.

## Heli Coordinate Comma Selection Fix - 2026-05-24

User observed a practical failure while surveying `凤巢七层`:

- A point intended as `4,35` / `6,35` could be read as `43,5` / `63,5`.
- The correction pin was then saved under the wrong coordinate.
- Walking to nearby `11,35` could not use the saved pin, because the saved pin was not actually near it.

Root cause:

- `MiniMapCoordinateReader.findCommaGlyph(...)` chose the comma mostly by expected X position.
- For one-digit X plus two-digit Y coordinates, a digit fragment on the Y side can look comma-like and cause the split to move too far right.

Change:

- Comma selection now evaluates every comma-like candidate by recognizing the left and right digit ranges.
- It chooses the candidate with the best combined digit-template score.
- Expected comma X is now only a very small tie-breaker.
- Debug mode logs all comma candidates and their left/right recognition results.

Expected result:

- Coordinates like `4,35` and `6,35` should be much less likely to become `43,5` or `63,5`.
- If the reader still chooses the wrong split, the debug log should now show the competing comma candidates clearly.

## Heli Anchored Nearby Pin Model - 2026-05-24

User clarified that correction samples are real measured points:

- Each correction sample contains map coordinate X/Y and measured screen-relative `actualRelX/Y`.
- Therefore nearby coordinates should be projectable from measured pins, not only exact-match pins.

Change:

- Exact coordinate match still wins and logs `correctionSource=exact-pin`.
- If there is no exact match, `correctionAt(...)` now tries a conservative anchored nearby model:
  - choose the nearest measured pin within `ANCHORED_CORRECTION_MAX_MAP_DISTANCE` (`18` map units);
  - compute the base-model delta between the pin coordinate and the current coordinate;
  - reuse the pin's measured correction delta at the current coordinate;
  - reject the candidate if the base model changes too fast across that interval (`>45` pixels per map unit), which is treated as a discontinuity/edge jump.
- Successful nearby anchored projection logs `correctionSource=near-pin`.

Important:

- This is not the old weighted averaging model.
- It uses one nearest measured pin as an anchor, instead of blending several potentially unrelated pins.
- It still refuses to cross strong base-model discontinuities.

## Heli Local Fit Correction Model - 2026-05-24

User pointed out a direction error in the anchored model:

- If the user records a real point around `(16,19)` and then moves to `(16,11)`, the cursor should move according to the measured local map/screen direction.
- The previous anchored model still depended on the base projection's direction for the movement delta, so if the base projection's Y direction was wrong in that local area, the cursor could move the wrong way.

Change:

- Non-exact correction no longer uses `currentBase - pinBase` as the movement delta.
- It now fits a local affine model from nearby measured correction pins:

```text
screenX = a * (mapX - currentX) + b * (mapY - currentY) + c
screenY = d * (mapX - currentX) + e * (mapY - currentY) + f
```

- The prediction at the current coordinate is `(c, f)`.
- It uses up to 8 nearest measured pins within 18 map-coordinate units.
- It requires at least 3 nearby pins.
- It rejects the local fit if weighted residual is above 95 px.
- Successful local fitting logs `correctionSource=local-fit/res=<value>`.

Priority order now:

1. `exact-pin`: exact measured coordinate, latest same-coordinate sample wins.
2. `local-fit`: nearby measured correction pins fit a local coordinate-to-screen model.
3. `none`: no reliable measured local model.

This is intended to make correction samples act as real survey points instead of simple exact-only pins or base-model deltas.

## Tangde Alternative Algorithm Options - 2026-05-24

Current shared understanding:

- The hard problem is not only "how to interpolate correction samples".
- The map-to-screen function can have camera/viewport discontinuities near irregular edges.
- Coordinate OCR can also save a correction under the wrong map coordinate.
- Any nearby-sample model can fail badly if it crosses a discontinuity or uses a wrongly parsed coordinate.

Alternative options besides the current local-fit direction:

1. Camera-state gated interpolation.
   - Classify each coordinate/sample by camera state:
     - X free / left-clamped / right-clamped;
     - Y free / top-clamped / bottom-clamped.
   - Only use correction pins from the same camera state.
   - Never interpolate across a camera-state boundary.
   - This is more explainable than pure local fit and directly targets the `73,5` -> `73,4` discontinuity problem.

2. Piecewise mesh / triangle interpolation.
   - Treat measured correction points as real survey points.
   - Build local triangles or small cells from nearby measured pins.
   - Predict only when the current coordinate falls inside a stable local cell.
   - Reject cells with large edge slope, bad residual, or suspected coordinate OCR ambiguity.
   - This needs more samples, but gives the most survey-like behavior.

3. Exact pins plus stronger data validation.
   - Keep general projection from boundary samples.
   - Use correction pins only for exact coordinates.
   - Add stronger save-time gates:
     - coordinate must be stable across multiple reads;
     - map label must be stable;
     - `actualRelX/Y` must be inside the game window;
     - re-test immediately after saving must hit `correctionSource=exact-pin`.
   - This is the safest short-term route if task automation only needs a few known points.

4. Survey path recorder.
   - Instead of manually adding isolated correction points, record a walking path.
   - Each sample gets time order, coordinate, screen point, and coordinate OCR confidence.
   - Build local models only from temporally adjacent samples.
   - This helps distinguish true map discontinuities from random wrong samples.

Tangde preference:

- Short term: keep exact pins reliable and visible.
- Medium term: add camera-state gated interpolation before trusting local-fit broadly.
- Long term: if this becomes a core feature, use a piecewise mesh / survey path recorder instead of isolated ad-hoc corrections.

## Tangde Diagnostic Visibility Patch - 2026-05-24

User pain point:

- Manual survey currently feels like blind testing.
- After recording many points, the UI/log did not clearly show whether the next test used an exact pin, local fit, or no correction.
- It also did not explain why local-fit was rejected or which pins were used.

Change:

- `MapSurveyService.projectCurrentPlayerPoint(...)` now appends:
  - `cameraState=...`
  - `correctionSource=...`
  - `correctionDetail=...`
- `cameraState` reports whether the current coordinate is in an X/Y free or boundary-clamped region.
- `correctionDetail` now explains:
  - exact pin used: saved pin coordinate, saved `actualRel`, current base point, exact count;
  - local-fit used: predicted point, current base point, nearest distance, and the pins used;
  - local-fit rejected: nearby pin count, cluster count, singular fit, or residual threshold failure.

Purpose:

- Stop blind sampling.
- The next survey test should show what the algorithm did and why.
- Use these diagnostics before collecting more calibration points.

Validation:

- `mvn -q -DskipTests compile` passed.
