# Internal Worker I26 - ReturnItemPrescan chooseStrategy count unit

## CLAIMED - 2026-07-15T04:05:11-04:00

- task: `W-COUNT-RIPS-CHOOSE-STRATEGY-1`
- claimedAt: `2026-07-15T04:05:11-04:00`
- countUnit: `ReturnItemPrescanService::chooseStrategy`
- requested countDelta: `+1`
- unique Java write set: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
- unique report: `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-return-item-choose-strategy-count-unit-worker-i26.md`
- role: Internal implementation-only Worker; not reviewer.
- constraints: protect all dirty/untracked work; no Git mutation, Maven, tests, runtime/server/host/task/poller/UI/capture/input changes; no owner/permit/session/ledger/compaction/durable workflow/business TTL/automatic retry.

## Implementation result - BLOCKED - 2026-07-15T04:05:11-04:00

- result: `BLOCKED`
- severity: `P1`
- delivered countDelta: `0`
- Java result: `NO_CODE_CHANGE`
- blocker owner: parent must provide/authorize a real active Cloud caller outside this Worker's unique Java write set.

### Baseline and scope evidence

- Fully read the required project instructions/context, CR271 top, whole-Service plan, and the method-level migration matrix. The matrix file read was recorded as 3,400 lines, SHA-256 `8A7CB0EDB811AAD5B881E4FA3E8B3E0C21EE3F8DC1AB8A4DC3D5A0A02E6A9B2A`.
- Read `docs/业务逻辑.md`'s applicable five-times prescan boundary: the return-item prescan remains after the tracker green click; this count unit does not move it earlier or change later return-home verification.
- Compared active Cloud source with the filesystem evidence mirror of DHXY baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` at `migration-baseline/696a12b0/.../ReturnItemPrescanService.java`.
- Both repository statuses were read before implementation. DHXY is on `thin-client-design` with extensive protected dirty/untracked work; Cloud is on `navigation-migration` with extensive protected dirty/untracked work. No existing change was reverted, overwritten, cleaned, staged, or committed.

### `chooseStrategy` source equivalence

The active Cloud method is already baseline-equivalent and needs no Java edit:

1. `stateFor(...)` uses `states.computeIfAbsent(...)` at active Cloud lines 217-234.
2. Only the new-state mapping function calls `chooseStrategy(...)` when `forcedStrategy == null`; an existing key returns its retained `PrescanState` and does not redraw.
3. Candidate construction at lines 237-246 is exactly ordered:
   `AFTER_TRACKER_GREEN` when available, then `BACKGROUND_PATHING` when available, then always `IN_COMBAT_RANDOM`.
4. `ThreadLocalRandom.current().nextInt(candidates.size())` is called exactly once for that newly created ordinary state. There is no `SKIP`, TTL, retry, second draw, or re-selection on an existing state.
5. `afterTrackerGreenRequired(...)` intentionally supplies forced `AFTER_TRACKER_GREEN`; it does not call or count `chooseStrategy`, matching the baseline forced-slot behavior.

### Downstream typed terminal is present but unreachable

If a real caller invoked the Service, the downstream chain is already closed:

- `afterTrackerGreen(...)` -> `stateFor(...)` -> strategy gate -> `runPrescan(...)`.
- `whilePathing(...)` -> `stateFor(...)` -> strategy gate -> `runPrescan(...)`.
- `whileInCombat(...)` -> retained strategy/fallback/due gates -> `runPrescan(...)`.
- `runPrescan(...)` chooses one of the two approved prescan operations and calls the existing `executeBagReturnItemMacro(...)`.
- That method submits `LocalMacroKind.BAG_RETURN_ITEM` through `TaskExecutionContext.getGameClient().executeLocalMacro(...)`, validates the transport execution state and operation echo, and returns the typed `BagReturnItemMacroResult`.
- DHXY's existing `LocalRemoteGameCommandHandler` maps the closed command to `BagReturnItemMacroIntent`, executes it once inside `submitRemoteExclusiveAndWaitDetailed(...)`, calls `BagService.runReturnItemMacroDirectForExclusive(...)`, and emits the closed `FOUND/NOT_FOUND/USED/NOT_USED` result or `NOT_EXECUTED/STOPPED/UNKNOWN` transport terminal.

No approved `ReturnItem` macro/wire behavior was recomputed, copied, or changed.

### Exact reachability blocker

The required real `caller -> Cloud Service` edge does not exist in active Cloud source:

- Repository-wide Java search finds `ReturnItemPrescanService` only in its own class plus documentation-only references from dormant `ReturnItemPrescanDecision` / `service.returnitem` artifacts.
- There is no field/constructor injection, `new ReturnItemPrescanService(...)`, method call, `initialize(...)`, route, endpoint, or assembly registration for the active Cloud Service.
- `CloudBrainServer` manually constructs its runtime graph; it does not start a Spring application/component scan, so `@Service` does not instantiate this class by itself.
- The real DHXY callers remain in local `XiuluoTaskV2` and `WubeiTask` and call the DHXY-local `ReturnItemPrescanService`, not this active Cloud class. Cloud currently has neither active `WubeiTask` nor active `XiuluoTaskV2` caller source.
- The separate `CloudReturnItemPrescanStateOwner` explicitly describes itself as dormant/unwired and has a private constructor. Using or modifying it would violate this task's prohibition on owner/session/permit plumbing and is outside the unique Java write set.

Therefore `computeIfAbsent -> chooseStrategy -> retained state` is correct but unreachable. Creating a self-call, startup hook, wrapper, stub, or synthetic caller inside `ReturnItemPrescanService.java` would not establish the required real business caller and would violate the task constraints. The missing caller must be closed in an explicitly reserved caller/assembly write set; until then this count unit cannot honestly advance by `+1`.

### Verification gates

- Maven: not run, per parent instruction.
- Tests/runtime/server/host/input: not run or started.
- Git mutation: none.
- Java files changed: none.
- Report changed: only this fixed report.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-15T04:13:00-04:00

父级独立检索 active Cloud 装配与调用图，确认报告所述 blocker：`ReturnItemPrescanService` 只有类内
方法与 dormant artifact 引用，真实 DHXY caller 仍在未迁入 active Cloud 的 Wubei/Xiuluo Task；当前没有
active caller/bean assembly 能到达 `chooseStrategy`。结论 **P0=0/P1=1/P2=0，BLOCKED / countDelta=0**。
不得用 self-call、wrapper、startup hook 或 dormant owner 伪造可达性；后续必须与真实 Task caller promotion
同一完整计数单闭合。Java 保持未改，本 Worker 关闭。
