# I41 - NpcClick fresh story blocker count unit

## Task

- task: `W-COUNT-NPC-FRESH-STORY-BLOCKER-1`
- countUnit: `NpcClickService::pollFreshStoryBlockerEvent`
- requested countDelta: `+1`
- actual countDelta: `0`
- result: `BLOCKED_MISSING_TYPED_EVENT_PORT / NO_CODE_CHANGE`
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`
- report: this file

## Baseline and workspace gates checked

- Read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current `CR271` section at the top of
  `docs/ACTIVE_WORK.md`, the NPC/Xiuluo-related rows in `docs/业务逻辑.md`, the whole-Service plan,
  the migration matrix, and both repositories' `git status` before touching the write set.
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- Both repositories contain extensive shared dirty/untracked work. No existing file was reverted,
  overwritten, cleaned, staged, committed, or otherwise mutated by I41.

## Exact blocker evidence

### P1-1 - The requested method is not part of the stated `696a12b0` authority

`git show 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/NpcClickService.java`
contains zero matches for all of:

- `pollFreshStoryBlockerEvent`
- `STORY_DIALOG_VISIBLE`
- `WindowReadyEvent`
- `WindowReadyEventBus`

`git log --all -S pollFreshStoryBlockerEvent -- src/main/java/com/bot/dhxy/service/NpcClickService.java`
points to `9aa987d1` (`修罗云端能跑`), so this behavior is a post-baseline CR255/CR267 addition rather
than a method that can be mechanically migrated from `696a12b0`.

Impact: the task brief's stated same-method baseline cannot be used to prove branch/order/freshness
equivalence. Copying the current DHXY implementation would introduce post-baseline behavior without a
declared behavioral authority.

### P1-2 - Active Cloud has no typed ready-event producer/port

The current DHXY implementation is at
`src/main/java/com/bot/dhxy/service/NpcClickService.java:509-528`. It reads the Runner-local
`WindowReadyEventBus` using the current `WindowRuntimeContext`, then accepts only an event whose:

1. request opted in via `consumeStoryDialogVisibleEvents`;
2. type is `STORY_DIALOG_VISIBLE`;
3. sequence is greater than both the session anchor and last consumed sequence;
4. `taskType` equals `request.sourceTask()`.

The active Cloud tree contains the copied DTO/enum
`WindowReadyEvent` / `WindowReadyEventType`, but contains no `WindowReadyEventBus` implementation and
no `RemoteGameClientPort`, ready-event port, or other typed contract that can supply the current
window/task-scoped latest event. A repository-wide Java search finds no
`RemoteGameClientPort` reference and no `WindowReadyEventBus` declaration in Cloud.

The active Cloud `NpcClickService` also still runs the baseline local smart-click pipeline and has no
cloud smart-click FIFO caller boundary at which this event can be consumed. Merely copying the DHXY
method would therefore either fail to compile or require copying Runner/runtime ownership into Cloud,
which the task explicitly forbids.

Impact: I41 cannot close the required active Cloud smart-click caller -> opt-in/same-task/fresh-sequence
fact -> stop-current-probe/null-continue -> closed outcome chain inside the one-file write set. Adding a
stub, reading only the copied DTO, returning unconditional null, or inventing a Cloud event owner would
create a false `+1`.

## Exact unlock condition

Keep this count unit at `countDelta=0` until all of the following exist outside I41's one-file write set:

1. A parent-approved typed DHXY producer/Cloud consumer contract for the latest
   `STORY_DIALOG_VISIBLE` observation. It must carry stable window/task identity, event sequence,
   source, and creation time, and must not copy `WindowReadyEventBus` ownership into Cloud.
2. An active Cloud smart-click FIFO/session boundary corresponding to the current DHXY CR255/CR267
   caller, including the single per-session anchor and last-consumed sequence inputs.
3. A declared behavioral authority for this post-`696a12b0` behavior (for example the relevant
   approved CR255/CR267 source), because the method does not exist in the fixed business baseline.
4. A later implementation scope that may wire that existing typed port at the exact FIFO boundary and
   preserve opt-in, same-task equality, strict sequence freshness, stop/cancel behavior, and null
   continuation without adding TTL, retry, wrapper, or a second event owner.

## Files changed and validation

- Java changes: none.
- Report changes: this file only.
- Build/test/runtime/input: not run, as required while shared Java writers are active and because this
  delivery is `NO_CODE_CHANGE / BLOCKED`.
- P0: `0`
- P1: `2`
- P2: `0`

无已批准业务差异；未实施行为变更。按 `696a12b0` 基线核对后因 typed event 边界缺失而精确阻断。

## Parent Source Review #1 - 2026-07-15T05:18:00-04:00

父级独立复核后确认阻塞成立，并校正一处表述：active Cloud **存在** package-private
`RemoteGameClientPort`，但该 port/executor/broker 没有 ready-event observation operation；Cloud 仅有
`WindowReadyEvent` DTO/enum 与 whole-Task 中无法解析的 `WindowReadyEventBus` import，没有 producer/bean。
`pollFreshStoryBlockerEvent` 又首次出现于 `9aa987d1`，不属于 `696a12b0`。

结论：**P0=0/P1=2/P2=0，BLOCKED_POST_BASELINE_MISSING_TYPED_EVENT_PORT，countDelta=0**。
无 Java 改动；不得把 DTO 当 producer、复制 Runner event bus 或返回固定 null。解锁条件为先有用户批准的后基线
行为权威、typed ready-event producer/consumer 与 active smart-click session caller。本内部实现槽立即释放并续派。
