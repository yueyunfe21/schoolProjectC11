# World Map Route Result Memory Design

## Goal

Reduce repeated OCR work in world-map navigation by remembering the final route-result click point
for a stable `fromMap -> targetMap` pair.

The memory is only a fast path after the normal live route flow has proved the click point several
times. It must not change first-run navigation behavior, route result scrolling, or watcher-based
arrival confirmation.

## Existing Flow

The current world-map route path lives primarily in
`src/main/java/com/bot/dhxy/service/NavigationService.java`.

The relevant sequence is:

1. Open the world map / route panel.
2. Input the target map name.
3. Search.
4. Scroll the route-result panel to the bottom using the existing scroll method.
5. Capture the route result area.
6. Verify the destination text through `GameTextLineOcrService.verifyWorldMapRouteDestination(...)`.
7. Find the final coordinate link through `GameTextLineOcrService.findLastWorldMapRouteCoordinate(...)`.
8. Click the resolved link point.
9. Register a `WindowPathingIntent`.
10. Let the watcher decide whether pathing reaches the target map.

This design keeps steps 1-4 unchanged and inserts memory lookup only after step 4.

## Scope

In scope:

- Persist a route-result memory keyed by `fromMap -> targetMap`.
- Store click point and counters.
- Use only clean memory as a fast path.
- Record live success, live failure, and abandoned pending attempts.
- Integrate with the current watcher settlement model.
- Add clear logs so live runs can prove whether the fast path is being used.

Out of scope:

- Do not store screenshots in the memory file.
- Do not learn from offline testcase images.
- Do not change map-name OCR, route-result OCR, scrolling, or route-link selection logic.
- Do not replace `DialogChoiceMemoryService`; that service is for route dialog options, not
  world-map route result rows.
- Do not use memory before the route-result panel has been opened, searched, and scrolled to the
  same bottom position as the current OCR flow.

## Memory Key

Use only:

```text
fromMap -> targetMap
```

Examples:

```text
灵兽村->长安
长安城东->龙宫
```

The first version intentionally ignores current coordinate. This matches the requested behavior and
keeps the memory easy to inspect.

## Persisted Data

Recommended file:

```text
config/world_map_route_result_memory.json
```

One entry should contain:

- `fromMap`
- `targetMap`
- `relativeX`
- `relativeY`
- `matchedText`
- `successCount`
- `failureCount`
- `consecutiveSuccessCount`
- `consecutiveFailureCount`
- `clean`
- `disabled`
- `lastSuccessAt`
- `lastFailureAt`
- `lastAbandonedAt`
- `source`

Coordinate space:

- `relativeX` and `relativeY` are game-window-relative pixels, based on the current 1024x768 client
  coordinate system.
- Runtime click uses current `tracker.getWindowBaseX/Y()` plus the saved relative point.
- Do not save screen-absolute coordinates because moved windows would make them stale.

Screenshots:

- Do not save route screenshots as memory data.
- Existing failure/debug screenshot mechanisms may continue to run independently.

## Clean / Dirty Policy

Memory starts dirty.

An entry becomes clean only when:

```text
consecutiveSuccessCount >= 5
```

Only clean entries can be used for the fast path.

On live success:

- increment `successCount`
- increment `consecutiveSuccessCount`
- reset `consecutiveFailureCount` to `0`
- set `clean=true` when `consecutiveSuccessCount >= 5`

On live failure:

- increment `failureCount`
- increment `consecutiveFailureCount`
- reset `consecutiveSuccessCount` to `0`
- set `clean=false`

Failure does not need to permanently disable the entry by default. A later implementation may disable
after a high consecutive failure threshold, but the first version can simply mark it dirty so OCR
becomes the fallback again.

## Live Data Rules

The memory must be written only from live navigation results.

Allowed learning source:

- The route-result click point produced by the current live OCR path.
- The watcher later confirms that the same navigation reached the target map.

Not allowed:

- Offline testcase replay.
- Debug images.
- Manually inserted sample data.
- A click that was followed by another navigation before watcher settlement.

This keeps the memory tied to real game behavior instead of static screenshots.

## Runtime Flow

### Normal / Dirty Path

When `submitWorldMapSearchAndClickDestination(...)` runs:

1. Capture current `fromMap` before the route search.
2. Run the existing route panel preparation:
   - open route UI;
   - type target map;
   - search;
   - scroll result panel to bottom.
3. After scrolling, check memory by `fromMap -> targetMap`.
4. If no clean entry exists, run the existing OCR path.
5. When OCR finds the destination/coordinate link and clicks it:
   - register the normal pathing intent;
   - create a pending route-result memory record in the current `WindowRuntimeContext`.

### Clean Fast Path

After route search and bottom-scroll:

1. Look up `fromMap -> targetMap`.
2. If the entry is clean, click the saved relative point.
3. Register pathing intent exactly as the OCR path does.
4. Create a pending route-result memory record that says this attempt used memory.

The fast path still depends on watcher settlement. A memory click is never considered successful at
click time.

## Pending Settlement

Add a pending memory object to the current window runtime, separate from dialog option memory. It
should include:

- `fromMap`
- `targetMap`
- `relativeX`
- `relativeY`
- `matchedText`
- `source`
- `usedMemory`
- `createdAtMs`
- a navigation intent id or equivalent identity if available

Settlement rules:

- If watcher confirms arrival at `targetMap`, record success.
- If watcher reports `STOPPED_AWAY` for the same intent and no second navigation replaced it, record
  failure.
- If a second navigation starts before settlement, abandon the pending record. Do not record success
  or failure.
- If task stop/pause/interrupt clears the intent, abandon the pending record.

This prevents a click from being credited or blamed when a later navigation changed the route.

## Logging

Add concise logs:

- memory lookup skipped: missing / dirty / disabled
- memory fast path used
- OCR path produced pending memory
- pending success
- pending failure
- pending abandoned due to second navigation / stop / stale intent

Suggested log prefix:

```text
[world-map-route-memory]
```

Important fields:

- `fromMap`
- `targetMap`
- `relativeX`
- `relativeY`
- `clean`
- `successCount`
- `failureCount`
- `consecutiveSuccessCount`
- `consecutiveFailureCount`
- `usedMemory`
- `intentId`
- `source`

## Error Handling

- If memory JSON cannot be read, log a warning and use an empty memory.
- Save by writing a sibling temp file and moving it into place, matching existing safe-write style.
- If the memory click cannot be submitted to input, do not record failure immediately; let the caller
  fall back to OCR or return a normal route-click failure according to current navigation behavior.
- If map names are null or blank, skip memory lookup and writing.

## Testing And Verification

No offline replay is required for learning or validation of this feature.

Required live verification:

1. First four successful `fromMap -> targetMap` runs still use OCR after bottom-scroll.
2. Each success increments `consecutiveSuccessCount`.
3. The fifth consecutive success marks the entry clean.
4. The next run for the same `fromMap -> targetMap` scrolls to bottom and then uses memory.
5. A watcher-confirmed arrival after memory click records success.
6. A watcher `STOPPED_AWAY` after memory click records failure and marks the entry dirty.
7. A second navigation before settlement records abandoned and does not change success/failure counts.

Compile verification remains required after implementation:

```powershell
mvn -q -DskipTests compile
```

## Open Implementation Notes

- The existing `DialogChoiceMemoryService` should not be reused directly because its coordinate
  space and semantics are dialog-relative option clicks.
- The new service can mirror its safe-write and counter style.
- The pending settlement should reuse the watcher area where pending transfer choice memory is
  currently settled, but as a separate pending type to avoid mixing dialog and world-map result
  semantics.
