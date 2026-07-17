# Internal I15 - AutoCombatPanelService::resolveRoundsRefreshReason

## Status

`CLAIMED`

- task: `W-COUNT-AUTO-COMBAT-PANEL-RESOLVE-REASON-1`
- role: Internal implementation-only Worker; not a reviewer
- countUnit: `AutoCombatPanelService::resolveRoundsRefreshReason`
- countDelta: `+1` only after parent source review and fresh Maven in the same round
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatPanelService.java`
- report write set: this file only
- current boundary: read-only authority and active-chain inspection; no build, test, runtime, Task, UI, capture, input, Git mutation, owner/session/TTL/retry/wrapper changes

## Handoff Status

`NO_CODE_CHANGE / ACTIVE CHAIN CLOSED / PARENT REVIEW + FRESH BUILD PENDING`

- The active pure decision already equals the `696a12b0` baseline and is reachable from a real Cloud task caller.
- No Java edit was needed. The active file was left at SHA-256
  `17F70569C2AF7C16B91BD333BED1C126E56697C999A8AF46ADD7271E6F85B6AE`, preserving I7/I11/I12 and all other
  concurrent work.
- This Worker does not approve or account the unit. `countDelta=+1` remains pending the parent's independent source
  review and fresh Maven gate in the same round.

## Authority And Workspace Gate

- Read the complete repository `AGENTS.md`, complete `docs/DHXY_CONTEXT.md`, top CR271 material in
  `docs/ACTIVE_WORK.md`, the applicable business-baseline gate in `docs/业务逻辑.md`, the complete whole-Service
  plan, and the complete Service migration matrix before source disposition.
- Applicable business rule checked: migration may move mechanics ownership but may not add a TTL, extra read or
  verification, park/yield, retry, cleanup, fail-closed business truth, or change decision priority, timing, phase,
  fallback, or input order without explicit approval.
- DHXY read-only status: branch `thin-client-design`, HEAD
  `0114604e1ff5f15491d2910959c45252e893d04f`, with extensive pre-existing dirty/untracked work.
- Cloud read-only status: branch `navigation-migration`, HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`, with extensive pre-existing dirty/untracked work.
- No dirty/untracked file was reverted, overwritten, cleaned, staged, committed, checked out, reset, or deleted.
- No build, test, runtime, Task, UI, capture, OCR, screenshot, input, or application/server command was run.

## Independent Count Boundary

The migration matrix has two adjacent but independent rows:

- `AutoCombatPanelService::resolveRoundsRefreshReason`: pure business decision returning
  `UNKNOWN/LOW_ROUNDS/REFRESH_DUE/null`.
- `AutoCombatPanelService::refreshAutoCombatRoundsIfNeeded`: consumes that reason, performs the existing one visible
  rounds read, and for a non-null reason executes the typed `Alt+8` refresh and success-only state reset.

I12 owns only the second row. Its scoped implementation replaced the final Cloud-local input submission inside
`refreshAutoCombatRoundsIfNeeded` with the existing typed `CloudGameClient.executeInputBundle` terminal. I12 did not
modify the pure method at current `AutoCombatPanelService.java:295-309`. Therefore this unit is not a private helper
recount of I12: it is the separately inventoried decision method used both before panel work and again after the
single visible-round read.

The old dormant `AutoCombatPanelDecision::resolveRoundsRefreshReason` leaf is not used as count evidence. Repository
search finds no caller of `AutoCombatPanelDecision`; the reachable chain below calls the same-name method on active
`AutoCombatPanelService` directly.

## Active Chain Evidence

The real Cloud task path is:

`AutoBattleTask::execute`
-> `handleAutoCombatTick(context)`
-> `AutoCombatService::handleCombatTick(context, "auto-battle", false)`
-> while `IN_COMBAT`, `maybeRunCombatMaintenance(context, source)`
-> `AutoCombatPanelService::resolveRoundsRefreshReason(...)`
-> reason-specific guard/verify route
-> `AutoCombatPanelService::verifyAndAlignPanel(VERIFY_AND_REFRESH)`
-> typed `AUTO_COMBAT_PANEL` observation/alignment
-> `refreshAutoCombatRoundsIfNeeded(panelMatch, source)`
-> one visible-round read
-> the same pure reason decision
-> I12's typed `PRESS_ALT_8 -> SLEEP(1000ms)` input bundle for non-null reason
-> success-only estimate/timestamp reset and boolean return.

Source anchors inspected:

- `AutoBattleTask.java:139-149,162-164`: live patrol loop calls `handleCombatTick` every tick.
- `AutoCombatService.java:126-175`: live tick reaches combat maintenance only while `IN_COMBAT`.
- `AutoCombatService.java:621-733`: reads estimate/timestamp/config, calls the pure reason method, preserves the
  `REFRESH_DUE` team guard and `UNKNOWN/LOW_ROUNDS` urgent per-window guard, then calls typed panel verification.
- `AutoCombatPanelService.java:88-103,295-376`: `VERIFY_AND_REFRESH` reaches the private refresh method; after exactly
  one visible-round read it re-runs the pure decision, returns `false` on null, and otherwise reaches I12's typed
  refresh terminal.

`AutoCombatService::nextCombatMaintenanceDelayMs` is an additional exact-baseline caller of the same pure method,
but its current `nextCombatWakeDelayMs` wrapper has no task caller in the Cloud source tree. It is not relied on as
the active reachability proof above.

## Baseline Equivalence

The authoritative Git object and the filesystem mirror at
`migration-baseline/696a12b0/.../AutoCombatPanelService.java` were compared with the active method.

- Baseline/current method length: `15/15` lines.
- Baseline/current method SHA-256 after newline normalization:
  `12D2ED84A654F653A8B9D763B591D499CDD64A4C5CC12E10D358F23AF721B76D`.
- Exact comparison: `true`.
- Constants remain baseline-exact: low threshold `10`, default reset `25`, refresh wait `1000ms`.
- Decision priority remains baseline-exact:
  1. `estimatedRounds < 0` -> `UNKNOWN`.
  2. `estimatedRounds <= 10` -> `LOW_ROUNDS`.
  3. `refreshIntervalMs > 0` and (`lastRefreshAt <= 0` or `now - lastRefreshAt >= refreshIntervalMs`) ->
     `REFRESH_DUE`.
  4. Otherwise -> `null`.
- Plain `long` subtraction/sign behavior is unchanged. No clamp, expiry, retry, extra read, state mutation, or new
  terminal interpretation was added.
- The reason enum and log values remain `UNKNOWN("unknown")`, `LOW_ROUNDS("low-rounds")`, and
  `REFRESH_DUE("refresh-due")`.

## Disposition

The requested count unit is already fully represented in active source and its real task-to-typed-terminal chain is
closed. A Java edit would not add missing behavior and would risk duplicating or changing the baseline decision, so
the correct implementation disposition is `NO_CODE_CHANGE`.

No approved business difference; migrate behavior-equivalently from `696a12b0`.

Parent handoff: independently review the active method and caller anchors above, then include this unit only in the
same round as a fresh applicable Maven success. Until both gates pass, the ledger remains unchanged.

## Parent Source Review #1 - 2026-07-15T03:25:00-04:00

父级独立对照 active/baseline `resolveRoundsRefreshReason` 四分支，方法体逐字符一致；真实 Cloud
`AutoCombatService:633` 与 `AutoCombatPanelService:321` caller 分别消费该决策并进入既有 typed panel/Alt+8
terminal。该方法在迁移矩阵中是独立 count unit，不与 I12 的物理 refresh 执行重复。结论：
**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**；fresh Cloud package 前不记账。

### Parent Count-Boundary Resolution

helper 的重复风险已复核：矩阵分别把 pure reason (`:1334`) 与 physical refresh (`:1335`) 定义为两个唯一
countUnit；I15 只计四分支 policy，I12 只计 typed Alt+8/state reset，互不重复。原 SOURCE APPROVED 保持。
