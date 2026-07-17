# Cloud TeamReturnService Public Closure - Internal Worker BU

CLAIMED | task=W-TEAMRETURN-DIRECT-PUBLIC-CLOSURE-IMP1; claimedAt=2026-07-14T06:29:31.3549175-04:00; writeSet=[Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TeamReturnService.java`; append-only report `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-team-return-public-closure-worker-bu.md`]

## Implementation Result

`BLOCKED` - 当前 shared fact/input surface 不足以在唯一 Java 写集内闭合 committed
`clickReturnTeamIfPresent(...)`，因此本 Worker 未修改 Cloud Java，也未用 callback、reflection、假实现或新 wire
绕过缺口。`beginLeaderSignalPrecheck(...)` / `consumeLeaderSignalPrecheck(...)` 虽可用现有
`TEAM_RETURN_LEADER_SIGNAL` fact 做机械投影，但本任务要求剩余 public cohort 直接闭合；在点击链硬阻断时不留下
新的 partial public cohort。

## Baseline And Current Source

- business baseline: DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f`
- baseline TeamReturn Git blob: `5e39866ca48be2f51486edb69935bca6a076b123`
- Cloud branch / HEAD: `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`
- current Cloud `TeamReturnService.java` SHA-256 before and after this task:
  `1EE6F16D4065EF880B36A8CC329E2779A96EB94B802F3D58A2E7703838079395`
- current Cloud `PlayerStateService.java` SHA-256 inspected:
  `359461143E06DA9ABB9955F1BC31373612CE89BF9155538E36627040E69294DB`

## Public API Matrix

| committed public API | current Cloud state | baseline equivalence audit | disposition |
|---|---|---|---|
| `clickReturnTeamIfPresent(context, source)` | missing | Baseline order is first button observation -> `ensureSheYaoXiangActive(context)` -> second button observation -> random `+/-3 px` point -> one ordered `clickLeft(..., 150)` + `sleep(500)` bundle. The required incense API is absent. | `BLOCKED`, not implemented |
| `probeMemberReturnMarker(source)` | present | One `TEAM_RETURN_BUTTON` fact per call. `PRESENT/ABSENT` map directly; non-`OBSERVED`, wrong type, capture/template/mechanics failure, and interruption stay `UNKNOWN`. No input. | mechanically equivalent |
| `waitForMembersReturnIfNeeded(context, source)` | present | Initial leader probe, `120000 ms` default timeout, `3000 ms` default poll, positive-config fallback, checkpoint-before-sleep, post-sleep probe, disappearance/timeout returns all preserve committed order. No added retry/TTL/park. | mechanically equivalent |
| `isReturnTeamSignalPresent()` | present | One `TEAM_RETURN_LEADER_SIGNAL` fact; only confirmed `PRESENT` is true. All unavailable/failure states remain false as committed `find... == null`. No input. | mechanically equivalent |
| `beginLeaderSignalPrecheck(context, source)` | missing | Existing typed leader fact can replace exact-window capture/template analysis. Approved simplified mechanical shape is one fact read at begin and an immutable scope/result handle; Cloud must not copy capture/template code or create a new local/future producer. | not implemented because cohort is blocked |
| `consumeLeaderSignalPrecheck(context, precheck, source)` | missing | Consumption can scope-check the immutable begin result and map no-signal/signal/failure to the existing conclusive/inconclusive status without a second fact read. | not implemented because cohort is blocked |

## Mechanical Replacement Map

1. First return-button observation: existing `WindowFactKind.TEAM_RETURN_BUTTON`; only typed `PRESENT` supplies the
   screen-absolute center point. Any unavailable/unknown/failure is a no-match path, not a fabricated `ABSENT` fact.
2. Incense step: must remain the committed `PlayerStateService.ensureSheYaoXiangActive(context)` call in the same
   position. It cannot be deleted, reordered, replaced by a constant, or reproduced inside TeamReturn.
3. Refreshed return-button observation: a new occurrence of the same typed fact after the incense call; disappearance
   returns false before input.
4. Click: preserve the committed independent `[-3, +3]` X/Y randomization, then submit exactly one ordered
   `InputBundle`: `clickLeft(screenX, screenY, 150)` followed by `sleep(500)`. No split bundle or resend.
5. Leader precheck: use only the existing `TEAM_RETURN_LEADER_SIGNAL` typed fact at begin; the returned scoped value is
   consumed later without capture/template work or another fact read. Failure/unavailable remains inconclusive so the
   caller retains the committed live-detection fallback.

## Blocking Evidence And Minimum Gap

- committed `TeamReturnService.java:65-86` contains the mandatory sequence; specifically line 75 calls
  `playerStateService.ensureSheYaoXiangActive(context)` between the two button observations at lines 66 and 76.
- current Cloud `PlayerStateService.java` exposes only its constructor at line 67 plus the public enum at line 468;
  `rg` finds no `ensureSheYaoXiangActive` method.
- current `WindowFactKind` has `TEAM_RETURN_BUTTON` and `TEAM_RETURN_LEADER_SIGNAL`, but no incense fact.
- current `LocalMacroKind` contains only `BAG_RETURN_ITEM`.
- current `CloudGameClient` exposes the existing generic fact/capture/input-bundle calls and the Bag-only local macro;
  none can execute the committed incense check/use chain without inventing a new contract.

Minimum prerequisite: the PlayerState owner must first close an existing, callable Cloud
`PlayerStateService.ensureSheYaoXiangActive(TaskExecutionContext)` equivalent (including its already-approved typed
local mechanics) without changing TeamReturn's call order or boolean meaning. Once that API exists, this same-file
TeamReturn closure can inject the collaborator, perform the two existing facts around it, and send the one existing
ordered input bundle. This Worker does not expand wire/schema/caller/host to manufacture that prerequisite.

## Verification

- Command: Cloud `mvn -q compile`
- Result: exit `0` in approximately `3.4 s`
- Constraints observed: no `clean`; no tests created or run; no application/server/host/Task/UI/input started.

## Changed Files

- Added only this report:
  `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-team-return-public-closure-worker-bu.md`
- Cloud `TeamReturnService.java` was pre-existing untracked shared work and remains byte-for-byte unchanged at the
  SHA-256 recorded above.
- No other dirty/untracked file was modified, reverted, cleaned, staged, or committed.

Worker self-audit only, not reviewer approval: scope respected; no Java implementation claimed; waiting for parent
judgment. **无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Blocker Review #1 - 2026-07-14T06:37:22-04:00

**BLOCKED，P0=0 / P1=1 / P2=0；Worker 的零 Java 停止判断正确。** committed
`clickReturnTeamIfPresent(...)` 在第一次按钮观察和第二次 fresh 观察之间必须调用
`PlayerStateService.ensureSheYaoXiangActive(context)`；当前 Cloud `PlayerStateService` 没有该 API，现有
fact/macro 也不能执行摄妖香检查/使用链。删除、常量替代或重排都会改变 `0114604e` 业务顺序。

精确返修条件：先独立闭合 Cloud `PlayerStateService.ensureSheYaoXiangActive(TaskExecutionContext)` 的等价 public
API 及其已存在/最小 closed local mechanics；不得把摄妖香逻辑复制进 TeamReturn，不新增 owner/session/ledger/TTL/retry。
前置通过后恢复 TeamReturn 单文件任务，保持 observation -> incense -> fresh observation -> randomized click bundle 顺序。
当前 TeamReturn Java SHA 未变化，Cloud compile exit 0；不计成果。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
