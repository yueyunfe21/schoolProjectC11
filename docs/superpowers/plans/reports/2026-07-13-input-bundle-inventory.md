# DHXY Direct Input-Bundle Inventory

## Rules

- `ONE_BUNDLE`: existing ordered physical steps become one `InputBundle` without changing order or delay.
- `LOCAL_MACRO`: input is interleaved with capture/template/OCR or another local fact; keep the entire existing method local and expose one typed call/result.
- `LOCAL_RESIDENT`: direct `InputProvider` call already running inside a local macro/exclusive owner; never send separately.
- `NO_PHYSICAL_INPUT`: matching text/API names that are not mouse/keyboard actions.

Committed business baseline: `0114604e`.

## Shared Action Coverage

Committed Service/Task queued lists use 12 `InputAction` factories: `clickLeft`, `clickRight`, `dragAndDrop`,
`moveMouse`, `pressAlt1`, `pressAlt4`, `pressAlt8`, `pressAltA`, `pressAltC`, `pressAltQ`, `scrollDown`, `sleep`.
All are already represented by Cloud `InputActionDto.Type`, DHXY `RemoteInputActionType`, and
`RemoteInputActionMapper`. Direct calls such as `pressCtrlA` that occur only inside a `LOCAL_MACRO` stay local and
do not require a new wire action.

## Cohort A - Parent Approved

Source report: `docs/superpowers/plans/reports/2026-07-13-cloud-npc-click-service-worker-a.md`,
`Parent Source Inventory Review #1`.

- `ONE_BUNDLE=11`: NpcClick 3, Navigation 1, GiveItem 1, QuestManager 5, TeamReturn 1.
- `LOCAL_MACRO`: Npc Ctrl-menu probe; Navigation route/world-map/panel/mini-map macros; Quest detail capture/activation macro.
- `LOCAL_RESIDENT`: all direct `InputProvider` calls inside those macros, including Navigation `pressCtrlA` and `pressEnter`.
- Migration consequence: bundle rows need no protocol work; macro rows require only a thin local mechanics method and typed result, not a state machine.

## Cohort B - Parent Approved

Source report: `docs/superpowers/plans/reports/2026-07-13-cloud-team-return-service-worker-b.md`,
`Parent Source Inventory Review #1`.

- `ONE_BUNDLE`: Dialog caller-supplied initial click.
- Fixed bundle: UICleaner Alt+1 close; UICleaner itself remains local by explicit user decision.
- Local-fact then bundle: Dialog option click, LeftTop status close, tracker-panel drag, maintenance confirm.
- `NO_PHYSICAL_INPUT`: TaskTracker prepare/cache paths and TaskMaintenance queue/event paths.
- Atomicity correction: `moveAndClickLeft` is atomic within the global input queue; a preceding template/OCR read remains
  the existing observation window and does not justify a new Service state machine.

## Cohort D - Parent Approved

Source report: `docs/superpowers/plans/reports/2026-07-13-cloud-return-item-prescan-state-worker-d.md`,
`Parent Source Inventory Review #1`.

- `ONE_BUNDLE=21`: FiveRing 9, Wubei 4, Xiuluo 8.
- FiveRing shoe reveal/buy/fallback remains Cloud business orchestration containing separate bundles; it is not a new
  local macro.
- `TaskTransactionRunner` does not transport an entire business callback to the local input worker. Cloud code invokes
  the shared bundle facade at each actual physical boundary.
- All listed points are computed before queue admission; no queued action list requires capture/template/OCR mid-bundle.
