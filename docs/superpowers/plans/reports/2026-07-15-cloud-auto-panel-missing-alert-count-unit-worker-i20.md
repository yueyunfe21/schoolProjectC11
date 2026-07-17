# W-COUNT-AUTO-PANEL-MISSING-ALERT-1 Worker I20 Report

- Status: `CLAIMED`
- Role: Internal I20 implementation-only Worker (not reviewer)
- Count unit: `AutoCombatPanelService::recordAutoPanelMissing`
- Count delta: `+1` (parent source review + fresh Maven same-round application only)
- Java write scope: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatPanelService.java`
- Report scope: this file only
- Restrictions: no build, test, runtime, input, or Git mutation; no owner/session/TTL/retry/wrapper additions

Investigation and implementation result pending full baseline, plan, matrix, repository-status, and call-path review.

## Scheduling Supersession

- Previous task: `W-COUNT-AUTO-PANEL-MISSING-ALERT-1`
- Previous status: `SUPERSEDED_BY_SCHEDULING`
- Previous count delta: `0`
- Previous Java result: no Java changes were made to `AutoCombatPanelService`

## Replacement Claim

- Status: `CLAIMED`
- Task: `W-COUNT-COMMON-BOX-CLEAR-PENDING-ROLE-1`
- Count unit: `CommonBoxService::clearPendingForRole`
- Count delta: `+1` (parent source review + fresh Maven same-round application only)
- Java write scope: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CommonBoxService.java`
- Report scope: this file only
- Required chain: role switch-off callers at the baseline-equivalent `:115` / `:260` paths -> clear only the corresponding role pending state -> closed detect/consume caller return
- Count boundary: this unit must be proven as an independent matrix state transition; member detect/consume units are frozen and must not be counted again
- Restrictions remain: no build, test, runtime, input, or Git mutation; no owner/session/TTL/retry/wrapper additions

## Parent Source Review #1 - 2026-07-15T03:37:07-04:00

父级独立对照 active Cloud 与 `696a12b0` 的 `CommonBoxService.clearPendingForRole`、role-disabled
consume caller 与 detect caller。该方法只清给定 role 的 pending records；两个真实 switch-off caller 分别
保持 `false`/`void return`，不重复计算 member detect/consume 的 observation、click 或 exact-key terminal。
结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**；无 Java 变化，无已批准业务差异，
fresh Cloud package 前 ledger 仍 `189/407`。

## Replacement Result - 2026-07-15T03:34:59-04:00

`NO_CODE_CHANGE DELIVERED / COUNT PENDING PARENT SOURCE REVIEW AND FRESH MAVEN`.

The active Cloud source already contains the complete `696a12b0` role-off pending transition. I20 made no Java
change because adding another cleanup method, call, state owner, or wrapper would duplicate the baseline behavior.

### Required Reads And Workspace Protection

- Read the complete repository `AGENTS.md` and `docs/DHXY_CONTEXT.md`, the top CR271 entry in
  `docs/ACTIVE_WORK.md`, the applicable `docs/业务逻辑.md` common-box rules, the complete
  `2026-07-14-696a12b0-whole-service-first-migration.md` plan, and the CommonBox/count-gate sections of the Service
  migration matrix.
- DHXY read-only status: branch `thin-client-design`, with extensive existing modified/deleted/untracked work.
- Cloud read-only status: branch `navigation-migration`, with extensive existing modified/untracked work.
- All existing dirty/untracked work was protected. No Git mutation, build, test, runtime, input, or application
  command was run.

### Baseline And Active Source Evidence

- Baseline authority:
  `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\CommonBoxService.java`.
- Active source:
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CommonBoxService.java`.
- Active whole-file SHA-256: `5f3ffb1e8ded18035220b7a216dc845af36e893fb62dc851775ec76d339d1f5b`.
- Baseline whole-file SHA-256: `f49a6ec634a918aa9b4ba72735c055df099cbec76e0218c7a32c211fd26f4892`.
  The whole files differ because desktop observation/input mechanics were replaced by the approved typed Cloud
  boundary; this count unit is not one of those substitutions.
- Extracting from `public void clearPendingForRole` up to `private void detectBox`, normalizing CRLF/LF only, gives
  661 characters on both sides and identical SHA-256
  `8c1fcde6d2059e9f48530c035387b525287114ab90f21c0f23c0abcf733bc108`.
- The baseline method at `:211-227` and active method at `:226-242` are therefore content-exact: null role is a
  no-op; the existing `pendingByKey` map is traversed; only entries whose stored role is the requested role are
  removed; and a log is emitted only when at least one entry was cleared.

### Real Caller Closure

1. **Consume role-off caller:** active `CommonBoxService:113-118` resolves the current context role. When
   `isRoleEnabled(role)` is false, `:115` invokes
   `clearPendingForRole(role, "switch-off:" + source)`, logs the role-off skip, and closes the public
   `consumePendingBoxIfAllowed(...)` result with `return false`. No pending lookup, stale gate, typed click, retry,
   or input follows this branch.
2. **Detect role-off caller:** both public role-specific detection entries delegate to `detectBox(...)`; active
   `CommonBoxService:259-263` checks the requested role switch before role matching or observation. When disabled,
   `:260` invokes the same role cleanup, logs the skip, and closes `detectBox(...)` with `return`; the public void
   detect caller consequently returns with no `COMMON_BOX` observation and no pending creation.
3. The two switch-off calls match baseline `CommonBoxService:119` and `:247`. Their order and closed returns are
   unchanged. The applicable business rule is also preserved: leader/member switches remain independent, and
   disabling one role clears that role's pending state without enabling, detecting, consuming, or clearing the
   other role.

### Independent Matrix State Transition / No Duplicate Count

The Service migration matrix lists `CommonBoxService::clearPendingForRole` as its own method-level state unit:
"UI 关关/跳过路径清角色 pending 防陈旧延迟点击". Its state transition is:

`pendingByKey -> pendingByKey without every entry whose PendingCommonBox.role == requested role`.

This is independent from the previously delivered CommonBox units:

- `detectMemberBoxAfterCombatExit` owns the real combat-exit caller, one `member-detect` typed observation, and
  `MATCHED`-only creation/replacement of one exact member pending entry. It does not own the role-wide removal
  transition and does not cover leader-role cleanup.
- `consumePendingBoxIfAllowed` owns exact current window/task/run pending validation, one typed click, success-only
  removal of that exact key, and false retention until the existing TTL. The role-off path exits before all of
  those consume operations; `clearPendingForRole` can remove multiple entries across window/task/run keys for the
  selected role and sends no input.
- This count unit therefore counts only the independently listed role-wide state mutation reached by both active
  switch-off branches. It does **not** recount member observation, pending creation, exact-key consume, click,
  success removal, TTL, or closed transport terminals.

### Scope And Gate

- Java result: no change to `CommonBoxService.java`; no write-set expansion was needed.
- No owner, session, TTL, retry, cleanup policy, wrapper, observation, input, or business fallback was added.
- `countDelta=+1` is not applied by I20. It remains pending the parent's independent source review and the same-round
  fresh Maven gate required by the count ledger.
- No reviewer conclusion is claimed by this implementation-only worker.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。父级源码审查与 fresh Maven 同轮通过前不真正计数。**

## Parent TRUE EOF Source Review #1 - 2026-07-15T03:42:00-04:00

父级独立对照 active Cloud 与 `696a12b0` 的 `CommonBoxService.clearPendingForRole`、role-disabled
consume caller 与 detect caller。方法只清给定 role 的 pending records；两个真实 switch-off caller 分别保持
`false`/`void return`，不重复计算 member detect/consume 的 observation、click、TTL 或 exact-key terminal。
结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**；无 Java 变化，无已批准业务差异，
fresh Cloud package 前 ledger 仍 `189/407`。
