# CR212 tooltip hash grouping and local leader controlled member gating

## Objective

Reduce duplicate startup TEAM_ROLE_TOOLTIP cloud calls and reduce idle auto-battle maintenance broadcast scans for members whose same-team leader is locally controlled.

## Existing context

- DHXY is a Java/Spring/JavaFX desktop automation project.
- Follow `AGENTS.md` and `docs/DHXY_CONTEXT.md`.
- Current branch has many unrelated dirty files. Do not revert, reset, clean, or rewrite unrelated work.
- Current relevant implementation:
  - `TeamRoleDetectionService` captures a team tooltip raw image per window and sends it to cloud `TEAM_ROLE_TOOLTIP` when active. The request already has `imageSha256`.
  - `WindowTaskControlService`, `MultiWindowTaskManager`, and `WindowTaskRunner` already pass `localTeamSessionKey`, `localLeaderWindowId`, and `localLeaderPresent`.
  - `TaskMaintenanceService` owns local-team session/capability state.
  - `AutoBattleTask.maybeRunIdleMaintenance(...)` currently builds `TaskMaintenanceRequest.handleMaintenanceBroadcast(true)`.

## Requirements

1. Startup tooltip grouping
   - For a UI same-queue / multi-window startup that performs team role detection, group tooltip raw images by conservative local hash.
   - First version must be strict: only identical tooltip image payload/hash groups together.
   - Do not add fuzzy/perceptual similarity, OCR, color washing, or template logic.
   - Reuse existing hash/fingerprint/sha256 utility where reasonable. Do not add heavy image algorithms.
   - Same hash group sends one representative raw tooltip image to cloud `TEAM_ROLE_TOOLTIP`.
   - Cloud returns `leaderClientId`; local code derives role for every window in that group by comparing each window's player/account ID to `leaderClientId`.

2. Local same-team relationship
   - Track same-team relationship primarily by player/account ID, not only window ID.
   - Window ID / hwnd is only current binding.
   - Record at least: group hash, leader player ID, leader window ID if local, member player IDs/window IDs, whether leader is local-controlled.
   - If `leaderClientId` is not among the selected local window player IDs, this is an external leader group and must not suppress member idle scans.

3. Member idle maintenance broadcast gating
   - If a member belongs to a same-tooltip group whose leader is local-controlled, suppress the member's idle unexpected maintenance broadcast scan.
   - This means `AutoBattleTask` should not ask opportunistic maintenance to do idle `handleMaintenanceBroadcast` for those members.
   - Do not suppress leader-opened capability behavior:
     - `TEAM_RETURN`
     - `FIRST_AID`
     - `SUMMON_SKILL`
     - `COMMON_BOX`
     - `LEFT_TOP_STATUS`
   - Do not suppress maintenance broadcast image matching when the leader has explicitly opened the relevant maintenance capability/window. Members still must respond to leader-triggered maintenance.

4. Lifecycle rules
   - Leader pause/resume does not change this broadcast rule.
   - Stop, error, session completion, local leader absent, or external leader must restore old idle scan behavior.
   - Existing local-team session completion/tombstone behavior must continue to work.

5. Diagnostics
   - Logs must include session key, group hash, leader player ID, leader window ID, member player IDs/window IDs, and whether idle scan is suppressed/restored.
   - Fresh runtime should make it obvious that identical tooltip images caused one cloud call for a group.

## Out of scope

- Do not change tooltip hover coordinates, tooltip ROI, screenshot timing, or game click/input behavior.
- Do not change cloud `TEAM_ROLE_TOOLTIP` algorithm.
- Do not change OCR/template/click/navigation business logic.
- Do not add a new broad service unless there is a real existing package boundary need. Prefer extending the current team role/session/maintenance ownership.

## Suggested files

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- focused tests under `src/test/java/com/bot/dhxy/team`, `src/test/java/com/bot/dhxy/service`, or `src/test/java/com/bot/dhxy/window`.

## Required verification

- Add focused tests covering:
  - identical tooltip hash group only invokes cloud once;
  - local leader-controlled group suppresses member idle `handleMaintenanceBroadcast`;
  - external leader or absent local leader does not suppress idle scanning;
  - pause/resume does not restore idle scanning;
  - stop/session completion restores old scanning behavior.
- Run focused tests.
- Run `mvn -q -DskipTests compile`.
- Run `mvn -q -DskipTests test-compile` if test files changed.
- Update `docs/ACTIVE_WORK.md` and `docs/PACKAGE_ARCHITECTURE.md` CR212 card with implementation result and verification.
- Run `node scripts/generate-cr-dashboard-data.js`.

## Fresh runtime acceptance

- Five same-team windows show one `TEAM_ROLE_TOOLTIP` cloud execute for the identical tooltip hash group.
- Group roles are derived locally from `leaderClientId + playerId`.
- Local-controlled leader running: members do not repeatedly perform idle unexpected maintenance broadcast scans.
- Leader-triggered maintenance still makes members respond via maintenance broadcast image matching.
- External leader / leader absent / session completed restores old scan behavior.
