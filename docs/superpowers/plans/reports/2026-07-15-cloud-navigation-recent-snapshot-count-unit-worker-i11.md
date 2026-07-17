# Internal I11 - NavigationService::confirmCurrentMapFromRecentPathingSnapshot

## Implementation #1 - 2026-07-15T02:35:03-04:00

### Assignment

- task: `W-COUNT-NAV-RECENT-PATHING-SNAPSHOT-1`
- countUnit: `NavigationService::confirmCurrentMapFromRecentPathingSnapshot`
- countDelta: `+1` requested; `0` delivered because the active chain is blocked by write-set-outside prerequisites.
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- allowed Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NavigationService.java`
- report write set: this file only.
- role: implementation worker, not reviewer.

### Baseline Gate

- Read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the applicable navigation/Xiuluo sections of
  `docs/业务逻辑.md`, the whole-Service plan, the Service migration matrix, and the active Cloud/DHXY
  caller, Service, runtime, watcher, transport, and model sources.
- `docs/业务逻辑.md` preserves the `696a12b0` Xiuluo navigation failure/fallback contract and forbids
  unapproved TTL, extra reads, retries, cleanup, park/yield, and fallback changes.
- The migration matrix records this exact cache rule as
  `RECENT_PATHING_SNAPSHOT_MAX_AGE_MS=1500ms` and keeps watcher/pathing observation local.
- No approved business difference exists. This unit must be a behavior-equivalent migration.

### Active Method Audit

The active Cloud method at `NavigationService.java:476-502` already preserves the complete baseline
decision table:

1. no current window context -> `NO_USABLE_SNAPSHOT`;
2. snapshot null, state `NONE`/`UNKNOWN`, or map null/blank -> `NO_USABLE_SNAPSHOT`;
3. age greater than the existing `1500ms` constant -> `NO_USABLE_SNAPSHOT`;
4. fresh map-name match -> `ARRIVED`;
5. fresh non-matching map with state `ACTIVE` -> `PATHING_ACTIVE`;
6. every other fresh state -> `NO_USABLE_SNAPSHOT`;
7. only a usable snapshot reaches the existing structured log.

An in-memory method extraction showed the active Cloud body is whitespace-equivalent to both active
DHXY and the `696a12b0` baseline. The Cloud and baseline extracted method bytes were identical after
line-ending normalization (`SHA-256 A3D0F4660423E4723A884AB51B04F70462BA147D41D98125022ED6858089AA37`).
No Java change is justified inside the allowed file.

### Intended Caller / Terminal / Fallback Chain

The copied Service body contains the intended chain:

`navigateToNPC` (`:206`) -> `navigateToMap` (`:262`) -> recent-snapshot check (`:313`, `:476`)
-> `ARRIVED` / `PATHING_ACTIVE` / `NO_USABLE_SNAPSHOT`.

- `ARRIVED` continues to the existing closed `NavigationResult.arrived` path (`:370-379`).
- `PATHING_ACTIVE` retains the existing route-dialog checks and returns a closed pathing/dialog result,
  including `NavigationResult.pathingStarted` at `:358`.
- `NO_USABLE_SNAPSHOT` retains the existing fresh-map confirmation at `:361`; a miss continues in the
  existing order through the route-dialog gate (`:404`) and world-map submission (`:421`).
- `navigateToNPC` then preserves the map-result short-circuit, current-map navigation, final stop
  checkpoint, and closed `NavigationResult` mapping. No cleanup, retry, TTL, or order change was made.

### Active Producer Evidence

DHXY has the real exact-window typed producer, but Cloud does not consume it:

- Active DHXY task callers invoke the local Service, for example
  `XiuluoTaskV2.java:2768` and `WubeiTask.java:2344`.
- `WindowTaskRunner.refreshPathingSignal` (`:2770/:2783/:2791`) reads the current bound window through
  its runner-owned `windowContext`, invokes `miniMapCoordinateReader.readCurrentTemplateLocation`
  (`:2811`), builds the typed snapshot in `updatePathingFromLocation` (`:2863/:2908`), and writes it via
  `windowContext.updatePathingSnapshot` (`:2932`).
- `WindowRuntimeContext` owns the per-window `AtomicReference<WindowPathingSnapshot>` (`:74`), exposes
  `getPathingSnapshot` (`:235`), registers intent (`:1762`), and accepts watcher updates (`:1837`).

The active Cloud source has no equivalent live producer/transport:

- Full active-Cloud search finds no task caller of `NavigationService.navigateToNPC`; the only executable
  occurrence is the method declaration itself.
- Active Cloud contains `WindowPathingSnapshot` and `WindowPathingState` value types, but the only snapshot
  construction is `WindowPathingSnapshot.idle()`.
- Active Cloud has no `WindowRuntimeContext.java` or `WindowTaskContextHolder.java`; its runtime directory
  contains only title/handle parser classes, even though the copied Service imports both missing runtime types.
- Neither Cloud nor DHXY remote `WindowFactKind` contains a pathing-snapshot fact, and no active remote
  command/result/codec/handler transports `WindowPathingSnapshot`, `pathingState`, or the observed current map.
- The existing `NAVIGATE_IN_CURRENT_MAP` macro is a separate current-map mechanics closure. It does not
  produce the watcher snapshot consumed by this cross-map stale-cache guard and therefore cannot prove this
  countUnit reachable.

### Blocker

**BLOCKED, write-set-outside prerequisite.** Closing the required real chain needs both:

1. an active Cloud task/Service assembly caller that invokes this Cloud `NavigationService` under an exact
   current task/window context; and
2. a closed typed DHXY watcher/pathing observation transport that supplies the current exact-window
   `WindowPathingSnapshot` (or an equivalent closed typed fact preserving state, map, timestamp, and stop
   terminal) to that Cloud call.

Both prerequisites require files outside the sole allowed Java write set. Implementing either only inside
`NavigationService.java` would require an unreachable stub, global/default window fallback, or fabricated
snapshot and would violate the count gate and exact-window business contract. No such code was added.

### Delivery

- status: `BLOCKED / NO JAVA CHANGE`
- countDelta applied: `0`
- Java changed: `0`
- report changed: this file only
- Maven/test/runtime/application/server/host/Task/poller/UI/capture/input: not run
- Git mutation: not performed
- intentional business differences: `无已批准业务差异；按 696a12b0 等价迁移`
- next action: parent must allocate the caller/context plus typed watcher-snapshot transport prerequisite in
  an allowed cross-file count unit; this I11 worker stops here and waits for parent review.
