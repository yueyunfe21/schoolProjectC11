# Internal I22 - CommonBox role-enabled count unit

## CLAIMED

- task: `W-COUNT-COMMON-BOX-ROLE-ENABLED-1`
- role: Internal I22 implementation-only Worker; not a reviewer
- claimedAt: `2026-07-15T03:44:38-04:00`
- countUnit: `CommonBoxService::isRoleEnabled`
- countDelta: `+1`
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CommonBoxService.java`
- report write set: this file only
- authority: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- gate: parent source review plus the parent's fresh applicable Maven gate; I22 does not apply the ledger delta

## Authority And Workspace Baseline

- Read the complete repository `AGENTS.md` and `docs/DHXY_CONTEXT.md`, the top CR271 entry in
  `docs/ACTIVE_WORK.md`, the applicable common-box baseline in `docs/业务逻辑.md`, the complete
  `2026-07-14-696a12b0-whole-service-first-migration.md` plan, and the CommonBox/count-gate portions of the
  Service migration matrix.
- DHXY status was already heavily dirty/untracked on branch `thin-client-design`; Cloud status was already heavily
  dirty/untracked on branch `navigation-migration`. I22 did not reset, clean, checkout, stage, commit, delete, or
  overwrite any existing work.
- Baseline mirror Service SHA-256:
  `F49A6EC634A918AA9B4BA72735C055DF099CBEC76E0218C7A32C211FD26F4892`.
- Active Cloud Service SHA-256 before and after this pass:
  `5F3FFB1E8DED18035220B7A216DC845AF36E893FB62DC851775EC76D339D1F5B`.
- Applicable business rows: `docs/业务逻辑.md` lines 73-90 require independent leader/member toggles, defaults
  leader=true/member=false, and role-only pending cleanup when a role is disabled. The selected source baseline
  additionally fixes `hasPendingBoxForCurrentWindow` as read-only: toggle-off returns false without adding cleanup.

## Result

`NO_CODE_CHANGE / ROLE-ENABLED COUNT UNIT SOURCE-COMPLETE`.

The authorized Java file already contains the complete `isRoleEnabled` decision and every baseline caller-side
terminal. Changing it would either alter `696a12b0` behavior or repeat the already delivered detect, consume,
has-pending, and clear-pending units. No Java edit was made.

## Per-Hop Evidence

### 1. Real member detect caller to role gate

1. `AutoBattleTask` reaches the injected `AutoCombatService` from its active task loop.
2. `AutoCombatService.consumeExitAndRecover` consumes a trusted combat-exit signal and calls
   `detectMemberBoxAfterCombatExit(context, requestedTaskCode, source)` at active Cloud lines 345-367.
3. `detectMemberBoxAfterCombatExit` delegates exactly once to `detectBox(..., CommonBoxRole.MEMBER, ...)` at
   `CommonBoxService.java:75-77`.
4. `detectBox` calls `isRoleEnabled(MEMBER)` at lines 259-263 before role validation or observation.
5. `isRoleEnabled` reads only `botProperties.isMemberCommonBoxEnabled()` at lines 367-370.
6. Disabled terminal: `detectBox` calls the existing `clearPendingForRole(MEMBER, ...)`, logs the role-toggle skip,
   and returns without observation. Enabled terminal: it continues through the existing role check and the single
   typed observation. This pass adds no observation, pending write, owner, retry, or cleanup.

### 2. Real has-pending caller to role gate

1. `AutoCombatService.runPendingMemberCommonBoxIfAllowed` calls
   `hasPendingBoxForCurrentWindow(context, requestedTaskCode)` at active Cloud lines 476-483 before taking the
   task turn.
2. The Service resolves the current context role and calls `isRoleEnabled(role)` at lines 200-206.
3. MEMBER reads only the member toggle; LEADER reads only the leader toggle. A disabled role returns `false`
   immediately.
4. The caller then returns `false` without acquiring the task turn or attempting input. The Service intentionally
   does not clear pending in this read-only method, exactly matching baseline lines 170-202 and the prior parent
   CommonBox review. Adding clear here would be an unapproved behavioral change and would duplicate
   `clearPendingForRole` accounting.

### 3. Real consume callers to role gate

1. `AutoCombatService.runPendingMemberCommonBoxIfAllowed` calls `consumePendingBoxIfAllowed` after acquiring the
   existing task turn at lines 500-517.
2. `AutoBattleTask.tryRunLocalTeamReturnRelease` calls the same consume API before return-team input when the
   existing `COMMON_BOX` capability is open at lines 235-255.
3. The consume method resolves `roleFor(context)`, calls `isRoleEnabled(role)` at lines 107-118, and therefore
   selects only the configured member or leader toggle for that context role.
4. Disabled terminal: clear pending records for that role only, log, return `false`, and perform no click.
5. Enabled terminal: continue into the already approved TTL/stale gates and typed click terminal. Only executed
   click clears the exact pending key; non-execution retains it until the existing TTL. I22 does not recount or
   modify that consume unit.

### 4. Configured toggle source and role independence

- `BotProperties` is the exact mutable configuration dependency imported into the production
  `CloudServiceConfiguration` together with the one existing CommonBox port assembly.
- `BotProperties.leaderCommonBoxEnabled` remains `true`; `memberCommonBoxEnabled` remains `false`.
- `isRoleEnabled` is byte-for-byte behavior-equivalent to the baseline decision: MEMBER selects the member getter;
  every other value reaching this private method selects the leader getter. Its callers pass only the closed
  `CommonBoxRole.LEADER/MEMBER` enum after their existing role resolution.
- `clearPendingForRole` removes only records whose stored role equals the disabled role, so one toggle cannot clear
  the other role's pending state.

## Count-Boundary Isolation

- This report claims only the matrix row `CommonBoxService::isRoleEnabled` and its caller-visible role-toggle
  terminals.
- It does not claim or reimplement `clearPendingForRole`, `detectBox`,
  `detectMemberBoxAfterCombatExit`, `consumePendingBoxIfAllowed`, or
  `hasPendingBoxForCurrentWindow`.
- The active Cloud tree still has no external caller of `detectLeaderBoxAfterReturnHome`; that existing leader
  producer disclosure is preserved. I22 did not fabricate a Task caller or modify a Task outside its write set.
  The role decision itself and its leader/member configuration branches are present; parent review owns whether
  the missing active leader task caller affects this isolated count unit.
- No second `CommonBoxStateGovernor`, pending owner, session, TTL, retry, wrapper, or configuration authority was
  added. The dormant governor files were not connected to this baseline Service path.

## Changed Files

| File | Change |
|---|---|
| `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-common-box-role-enabled-count-unit-worker-i22.md` | Added this implementation-only evidence report. |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CommonBoxService.java` | No change. |

## Status And Handoff

- status: `NO_CODE_CHANGE DELIVERED / PENDING PARENT SOURCE REVIEW AND FRESH MAVEN`
- count applied by I22: `0`
- requested count delta after all parent gates: `+1`
- build/test/runtime: not run, as explicitly prohibited for this Worker
- Task/UI/capture/input/application/server: not run or modified
- Git mutation: none

**无已批准业务差异；按 `696a12b0` 基线等价迁移。父级审查与 fresh Maven 通过前不得记账。**

## Parent Source Review #1 - 2026-07-15T03:49:00-04:00

父级独立复核 `CommonBoxService:114/205/259 -> isRoleEnabled:367-372` 与 `696a12b0`。三个 active
detect/hasPending/consume 路径均只按 closed `CommonBoxRole` 读取对应 member/leader 配置；disabled 分支保持各自
clear/false/void terminal，enabled 分支只继续既有 observation/consume，不重复其计数。结论：
**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**；无已批准业务差异，fresh Cloud package
前 ledger 仍 `189/407`。
