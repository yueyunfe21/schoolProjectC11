# Internal I9 - CommonBox detect count unit

## CLAIMED

- task: Internal I9 implementation-only
- claimedAt: `2026-07-15 01:56:04 -04:00`
- countUnit: `CommonBoxService::detectBox`
- countDelta: `+1`
- writeSet: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CommonBoxService.java`
- report: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-common-box-detect-count-unit-worker-i9.md`
- baseline: DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- role: implementation-only; not reviewer

## Implementation

### Result

`NO_CODE_CHANGE / ACTIVE DETECT CORE CHAIN COMPLETE`.

The authorized Cloud Java file already contains the complete `detectBox` count unit. I9 made no Java change:
adding another observation wrapper, terminal adapter, retry, or pending owner would duplicate an already active
boundary and would violate the fixed `696a12b0` behavior. This report claims only the private
`CommonBoxService::detectBox` core unit; the already separated public
`detectMemberBoxAfterCombatExit` count unit is used only as the real reachability anchor and is not reimplemented.

### Baseline And Repository Evidence

- DHXY branch/HEAD: `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`, no upstream configured.
- Cloud branch/HEAD: `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`, no upstream configured.
- Both repositories were already heavily dirty/untracked. I9 did not reset, clean, checkout, stage, commit, or
  overwrite any existing work.
- Baseline method authority: DHXY
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/CommonBoxService.java`.
- Active Cloud `CommonBoxService.java` SHA-256:
  `5F3FFB1E8DED18035220B7A216DC845AF36E893FB62DC851775EC76D339D1F5B`.
- Active Cloud `AutoCombatService.java` SHA-256:
  `80380B8D65EAA4230886AD233DFBD49D8BED91F44F54BCFEA7AFE2B45BB5632D`.

### Real Active Caller

1. Cloud `AutoBattleTask.runTask` executes the normal polling loop and calls
   `handleAutoCombatTick(context)`; that method delegates to the injected `AutoCombatService`.
2. `AutoCombatService.handleCombatTick` begins with `context.throwIfStopRequested()` and reaches
   `consumeExitAndRecover` only through its existing combat-radar flow.
3. `consumeExitAndRecover` calls `detectMemberBoxAfterCombatExit` only after one trusted combat-exit signal has
   been consumed and the existing `recordCombatExit` / `resetCheckCounter` bookkeeping has run
   (`AutoCombatService.java:345-367`).
4. The public member entry delegates exactly once to
   `detectBox(context, requestedTaskCode, CommonBoxRole.MEMBER, source)`
   (`CommonBoxService.java:75-77`). No second member observation caller is introduced by this unit.

### `detectBox` Gate Sequence

Active `CommonBoxService.java:244-279` preserves the applicable `696a12b0` order:

| Gate | Active evidence | Closed effect |
|---|---|---|
| expiry cleanup | `pruneExpiredPending()` | removes only already expired pending records |
| task | `normalizeSupportedTask` | only `xiuluo_v2` / `wubei` continue |
| window | non-null context plus `context.hasWindow()` | missing logical window returns before observation |
| run | non-blank `taskRunKey(context)` | invalid run logs and returns |
| toggle | `isRoleEnabled(MEMBER)` | member switch-off clears MEMBER pending only, then returns |
| role | `roleFor(context)` and `actualRole == requestedRole` | unknown/mismatched role removes only the requested key and returns |
| stop | `detectAndRecord` calls `context.isStopRequested()` before the port | confirmed stop returns with no observation or mutation |

The pending key remains `windowId | nativeHandle | role | task | taskRun`; the exact native handle, process,
identity epoch, stop epoch, run revision, and task-run registration are revalidated by the existing remote command
authority and DHXY handler before mechanics run. No default window lookup is used.

### Single Typed Observation And Exact-Window Mechanics

1. `detectAndRecord` contains exactly one call to `commonBoxPort.observe(...)`
   (`CommonBoxService.java:282-300`), with phase `common-box`, role slot `member-detect`, and the existing timeout.
2. The sole production implementation is `CloudCommonBoxPortAssembly`, imported with `BotProperties` by
   `CloudServiceConfiguration`. Its `observe` method calls
   `context.getGameClient().readWindowFact(..., WindowFactKind.COMMON_BOX, ...)` exactly once.
3. Cloud retained-action and task-run gates validate the current active run before request construction. No
   automatic resend is performed; an unresolved `UNKNOWN` remains unresolved.
4. DHXY `LocalRemoteGameCommandHandler` runs registration and exact binding gates before and after the fact read
   (`requireRegistration` / `requireBoundWindow`). The `COMMON_BOX` branch calls
   `CommonBoxLocalObservationMechanics.observe(access.binding())` exactly once under that window context.
5. Local mechanics captures only client ROI `(623,590)-(682,618)`, reads
   `images/template/common/leader_box_marker.png`, and matches at `0.86`. A hit returns client coordinates, score,
   and the DHXY-local `matchedAtEpochMs`; the handler converts the point to `SCREEN_ABSOLUTE_PX` using the same
   exact binding origin. It performs no click or input.

Read-only SHA evidence for the retained terminal:

| File | SHA-256 |
|---|---|
| Cloud `CloudCommonBoxPortAssembly.java` | `B9AE9555E5CA562CFCFD29BFF7F8BA81E97E6AF0C85F005007202A3B61F059FC` |
| Cloud `CommonBoxObservationResult.java` | `3F30C8D55D7577FEB48DE128D010113DA0B10FFC0172C77B87214B91C1AE4E4E` |
| DHXY `CommonBoxLocalObservationMechanics.java` | `7E9F09084495DFA71D83C516EC321E11B77890E902780148626E90A6C540DAFD` |
| DHXY `LocalRemoteGameCommandHandler.java` | `B1CD28FA03F1F933E92BB037C09BA1E2922635149D32D4482637B1CD313BCDFC` |
| DHXY `RemoteCommonBoxFact.java` | `4090FC927BB968CD4EBDB1125E7EB41C6CC0808DF658C90A889288C64150525F` |

### Closed Terminal And Mutation Matrix

The requested five terminal classes are closed without adding a second adapter. The existing typed contract keeps
mechanical failure detail rather than collapsing it into a false miss:

| Effective terminal | Existing typed state(s) | `pendingByKey` effect |
|---|---|---|
| `MATCHED` | `MATCHED` | the only branch that writes/replaces one pending record |
| `NOT_MATCHED` | `NOT_MATCHED` | no mutation |
| `CAPTURE_FAILED` family | `CAPTURE_UNAVAILABLE`, `TEMPLATE_UNAVAILABLE`, `MECHANICS_FAILED`, transport `NOT_EXECUTED` | no mutation; never treated as a match |
| `STOPPED` | `STOPPED` | checkpoint unwind; unconfirmed STOPPED is fatal; no mutation |
| `UNKNOWN` | `UNKNOWN` or an impossible observation terminal | fatal/unresolved unwind; no mutation |

On `MATCHED`, active Cloud stores the exact task/window/native handle/run/role/identity/source and anchors the
existing expiry to `matchedAtEpochMs + 30_000`. No other branch calls `pendingByKey.put`. Negative terminals do not
clear an otherwise valid pending entry and do not trigger another observation.

### Scope And Handoff

- Java action: none; authorized Java write set remains byte-identical.
- Added no second observation, TTL, retry, owner, session, pending map, cleanup, park/yield, or input action.
- The consume/click path and its ordered input bundle are outside this detection count unit and were not changed.
- Per explicit instruction, I9 did not run Maven, javac, tests, runtime/application/server/host, Task/poller, UI,
  capture, or input.
- `countDelta=+1` remains claimed only. I9 does not update the ledger and does not issue an `Approved` judgment.
- Handoff status: `NO_CODE_CHANGE DELIVERED / PENDING PARENT SOURCE REVIEW AND UNIFIED BUILD GATE`.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-15T02:33:00-04:00

父级独立复核 active `AutoCombatService:366 -> detectMemberBoxAfterCombatExit:75 -> detectBox:244 ->
detectAndRecord:282 -> CloudCommonBoxPort.observe -> DHXY exact-window COMMON_BOX fact`，源码本身保持一次 observation、
仅 MATCHED 写 pending、negative terminal 不清已有 pending，业务语义无漂移。但本任务不能单独记账：父级此前已经把
`CommonBoxService::detectMemberBoxAfterCombatExit` 作为完整 member detection caller chain 标为
`SOURCE APPROVED / COUNT PENDING BUILD`；本次 `detectBox` 是该同一调用链内的 private core，复用同一 caller、同一
typed observation、同一 pending terminal，不能再次作为独立 `+1`。

结论：**P0=0/P1=1/P2=0，COUNT BOUNDARY BLOCKED / countDelta=0**。P1 是重复计算同一完整 caller chain，
不是 Java 缺陷；保留 I9 的源码证据，不改 Java，不进入 unified build ledger。无已批准业务差异。
