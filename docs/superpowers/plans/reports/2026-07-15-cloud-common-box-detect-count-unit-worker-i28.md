# Internal I28 - CommonBox detect core count unit

## CLAIMED

- task: `W-COUNT-COMMON-BOX-DETECT-CORE-1`
- role: Internal implementation-only Worker I28; not a reviewer
- requested countUnit: `CommonBoxService::detectBox`
- requested countDelta: `+1`
- business baseline: DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CommonBoxService.java`
- report write set: this file only

## Disposition

`BLOCKED / DUPLICATE COUNT BOUNDARY / countDelta=0 / NO_CODE_CHANGE`.

The authorized Java file already contains the complete active detect core, but it cannot be delivered as a new
unique `+1` unit. The current CR271 plan and migration matrix explicitly record the parent decision from I9:
private `CommonBoxService::detectBox` is the core of the already source-approved
`CommonBoxService::detectMemberBoxAfterCombatExit` caller chain. Both units use the same real caller, the same one
typed observation, and the same pending terminal. Counting I28 would count that one chain twice.

This is a count-boundary blocker, not a Java defect and not a missing implementation prerequisite. I28 therefore
made no Java change and did not create a wrapper, stub, second observation, pending owner, TTL, cleanup, retry, or
terminal adapter.

## Authority And Workspace Evidence

- DHXY: `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`; the repository was already heavily
  dirty/untracked.
- Cloud: `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`; the repository was already heavily
  dirty/untracked and the target Service belongs to the existing untracked Cloud migration tree.
- Active Cloud `CommonBoxService.java` SHA-256:
  `5F3FFB1E8DED18035220B7A216DC845AF36E893FB62DC851775EC76D339D1F5B`.
- Baseline mirror `CommonBoxService.java` SHA-256:
  `F49A6EC634A918AA9B4BA72735C055DF099CBEC76E0218C7A32C211FD26F4892`.
- `docs/ACTIVE_WORK.md` top CR271 entry and its `2026-07-15 02:33` entry preserve the duplicate-unit ruling.
- The whole-Service plan states that I9 private `detectBox` reused the already approved member-detect chain and was
  removed as `P1/countDelta=0`.
- The migration matrix states that an already approved public caller chain's private helper/policy cannot receive a
  second `+1`, naming `CommonBoxService::detectBox` as `COUNT BOUNDARY BLOCKED P1=1/countDelta=0`.
- The prior I9 report contains the inherited parent conclusion:
  `P0=0/P1=1/P2=0, COUNT BOUNDARY BLOCKED / countDelta=0` because the public member-detect chain already owns the
  same core.

I28 did not reset, clean, checkout, stage, commit, delete, or overwrite any existing work.

## Existing Closed Active Chain

The requested runtime path is already present without an I28 Java edit:

1. Active Cloud `AutoCombatService.consumeExitAndRecover` consumes the trusted combat-exit signal, records combat
   exit, resets the player-state check counter, then calls
   `commonBoxService.detectMemberBoxAfterCombatExit(context, safeRequestedTaskCode(context), source + ":combat-exit")`.
2. `detectMemberBoxAfterCombatExit` delegates exactly once to
   `detectBox(context, sourceTask, CommonBoxRole.MEMBER, source)`.
3. `detectBox` preserves the existing order: prune expired pending, supported-task gate, non-null exact window gate,
   non-blank task-run gate, member toggle gate, actual-role gate, then the stop gate in `detectAndRecord`.
4. `detectAndRecord` invokes the existing `CloudCommonBoxPort.observe` exactly once with phase `common-box`, slot
   `member-detect`, and the existing timeout.
5. The production `CloudCommonBoxPortAssembly` performs one typed `WindowFactKind.COMMON_BOX` read. DHXY
   `LocalRemoteGameCommandHandler` resolves the current registration and exact native-window binding, then invokes
   `CommonBoxLocalObservationMechanics.observe(access.binding())` for the fixed client ROI.
6. The typed result returns to the same `detectAndRecord` switch and closes as pending write, no mutation, cooperative
   stop, or fatal unresolved terminal. There is no click in the detection path.

## Closed Result And Pending Mutation

| Result | Existing pending effect |
|---|---|
| `MATCHED` | The only branch that writes/replaces one pending record, using the exact window, native handle, task, task run, member role, identity epoch, source, and DHXY-local match timestamp. |
| `NOT_MATCHED` | No pending write and no false match. |
| `CAPTURE_UNAVAILABLE` | No pending write and no false match. |
| `TEMPLATE_UNAVAILABLE` | No pending write and no false match. |
| `MECHANICS_FAILED` | No pending write and no false match. |
| `NOT_EXECUTED` | No pending write and no false match. |
| `STOPPED` | Rechecks the existing task checkpoint; confirmed stop unwinds, unconfirmed STOPPED is fatal; no pending write. |
| `UNKNOWN` | Fatal unresolved terminal through the existing default branch; no pending write. |

The active file contains one `pendingByKey.put(...)`, only inside `case MATCHED`. Negative, STOPPED, and UNKNOWN
cannot manufacture pending state. Existing pending is not cleared by a negative observation.

## Changed Files

| File | I28 action |
|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CommonBoxService.java` | No change; existing chain is source-complete. |
| `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-common-box-detect-count-unit-worker-i28.md` | Added this exact blocked/count-boundary report. |

## Handoff

- implementation status: `BLOCKED / DUPLICATE COUNT BOUNDARY`
- requested count delta: `+1`
- deliverable count delta: `0`
- Java change: none
- build/test/runtime: not run, as explicitly prohibited for I28
- application/server/host/Task/poller/UI/capture/input: not run or modified
- Git mutation: none
- parent action: do not place I28 in the unified-build ledger as a new unit; issue a genuinely independent matrix
  count unit if another `+1` implementation slot is required

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**
