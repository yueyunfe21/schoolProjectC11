# TURN-T04 Worker D Report

- Card: `TURN-T04`
- Worker: `CR271 Worker D`
- Status: `CLAIMED`
- Claim scope: tests and this report only
- Production changes: forbidden
- Reviewer authority: none; Worker D will not write `APPROVED` or `BLOCKED` as a review judgment

## Resume

- Status: `RESUMED`
- Parent/user confirmation: implementation resumed after the design-confirmation pause.
- Scope remains unchanged: only the five frozen contract tests and this report; no production repair.

## Frozen write set

1. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutorContractTest.java`
2. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/local/UiLocalOperationExecutorContractTest.java`
3. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`
4. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/local/QuestLocalOperationExecutorContractTest.java`
5. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java`
6. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T04.md`

No other file is owned by this Worker. Existing dirty/untracked files in both repositories are protected and must not be reverted, overwritten, cleaned, staged, or committed.

## Authority and acceptance contract read

- `D:/mavenProject/DHXY/AGENTS.md`
- `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`
- Section 19 of `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`
- Production boundaries read:
  - `BagLocalOperationExecutor`
  - `UiLocalOperationExecutor`
  - `GiveItemLocalOperationExecutor`
  - `QuestLocalOperationExecutor`
  - `LocalServiceStepDispatcher`
  - `LocalServiceExecution`
- Both repository worktree statuses were captured before editing. Both contain pre-existing dirty/untracked work; none is part of this card.

## Required evidence

The named tests will cover the `LS+LX` profile without real runtime, application startup, Task execution, UI, capture, OCR, keyboard, mouse, or physical input:

- all nine closed `TurnLocalOperation` values route only to the four permanent-local Services;
- typed arguments and typed JSON results for success and failure;
- null, mismatched, and malformed argument unions fail closed before any Service call;
- Bag and Give dispatcher paths acquire exactly one exclusive boundary;
- the three non-X2 UI operations retain `UICleanerService` queue ownership and the X2 operation acquires exactly one exclusive boundary without nesting;
- Give item remains one indivisible macro call;
- Quest detail returns the same-call PNG with absolute origin, while failure returns no frame and performs no second capture;
- unknown or unsupported operations do not invoke any Service.

## Required commands

Run from `D:/mavenProject/DHXY`:

1. `mvn -q -Dtest=BagLocalOperationExecutorContractTest test`
2. `mvn -q -Dtest=UiLocalOperationExecutorContractTest test`
3. `mvn -q -Dtest=GiveItemLocalOperationExecutorContractTest test`
4. `mvn -q -Dtest=QuestLocalOperationExecutorContractTest test`
5. `mvn -q -Dtest=LocalServiceStepDispatcherContractTest test`
6. `mvn -q -DskipTests compile`

Final Worker status will be exactly one of:

- `TEST DELIVERED`: test sources and complete command evidence are ready for parent review; or
- `BLOCKED evidence`: a production defect or non-test write-set prerequisite prevents the required contract, with exact source evidence only and no production repair.

## Delivery

- Final Worker status: `BLOCKED evidence`
- Blocker class: repository-wide pre-existing test-source compilation debt outside TURN-T04 write set.
- Production changes by Worker D: none.
- Runtime/application/Task/UI/capture/input execution: none.
- Git mutation: none.

### Test sources delivered

1. `BagLocalOperationExecutorContractTest` - 4 tests
   - all three return-item intents retain typed arguments, cached absolute coordinates, and typed result JSON;
   - incense maps `true/false` to `USED/NOT_FOUND`;
   - null, malformed union, malformed bag arguments, and foreign operation invoke BagService zero times.
2. `UiLocalOperationExecutorContractTest` - 4 tests
   - all four UI operations return typed JSON;
   - the three Service-owned queue operations add no adapter exclusive wrapper;
   - X2 owns exactly one exclusive callback, has no nested callback, and invokes the direct Service method once;
   - queue failure and malformed calls perform no Service call.
3. `GiveItemLocalOperationExecutorContractTest` - 3 tests
   - the complete Give macro is invoked exactly once with the exact template/index;
   - both `given=true` and `given=false` remain typed completed results;
   - malformed/foreign calls invoke GiveItemService zero times.
4. `QuestLocalOperationExecutorContractTest` - 4 tests
   - activate arguments/results are typed;
   - detail capture uses one Service call and returns PNG bytes from that same image with exact absolute origin and source step index;
   - failed capture returns no frame and does not capture again;
   - invalid index/union/arguments and foreign operations invoke QuestManagerService zero times.
5. `LocalServiceStepDispatcherContractTest` - 4 tests
   - asserts the closed enum contains exactly nine operations and routes every one to exactly one of the four permanent-local Services;
   - Bag/Give and UI X2 each use exactly one exclusive callback; other UI and Quest operations add none;
   - queue failure does not enter adapters or Services;
   - null/malformed calls fail closed, while an unknown wire enum cannot deserialize and therefore reaches neither dispatcher, input queue, nor any Service.

Total isolated TURN-T04 methods: 19; failures=0, errors=0, skipped=0.

### Required command evidence

The five exact lifecycle commands were each executed. All exited `1` during Maven `testCompile`, before the selected TURN-T04 test could run:

| Command | Exit | Result |
|---|---:|---|
| `mvn -q -Dtest=BagLocalOperationExecutorContractTest test` | 1 | blocked by unrelated existing test-source compilation errors |
| `mvn -q -Dtest=UiLocalOperationExecutorContractTest test` | 1 | same external test-source compilation errors |
| `mvn -q -Dtest=GiveItemLocalOperationExecutorContractTest test` | 1 | same external test-source compilation errors |
| `mvn -q -Dtest=QuestLocalOperationExecutorContractTest test` | 1 | same external test-source compilation errors |
| `mvn -q -Dtest=LocalServiceStepDispatcherContractTest test` | 1 | same external test-source compilation errors |
| `mvn -q -DskipTests compile` | 0 | DHXY main compile passed |

Representative exact external evidence:

- `src/test/java/com/bot/dhxy/service/SummonSkillStartIndexPolicyTest.java:19,25,36` calls missing `SummonSkillService.resolveStartIndex(...)`.
- `src/test/java/com/bot/dhxy/service/TaskMaintenanceSummonSkillUnknownBackoffTest.java:41,84` uses a stale `TaskMaintenanceService` constructor.
- `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoPreparedEnterBattleRetryWiringTest.java:26` uses a stale `withShortcutTrackerClick(...)` arity.
- `src/test/java/com/bot/dhxy/service/NpcClickTooltipGeometryTest.java:12,14,20` calls missing geometry helpers.
- Additional unrelated failures include stale TaskMaintenance/SummonSkill constructors, InputAction result typing, Dialog maintenance APIs, TeamRole constructor wiring, and Xiuluo round-context signatures.

No compiler error named any TURN-T04 test source. Maven Compiler 3.11.0 exposes `testIncludes` only as plugin configuration and has no command-line-bound property in its `testCompile` descriptor, so Worker D cannot isolate the lifecycle test compilation without changing `pom.xml`, which is outside this card's frozen write set.

### Isolated test-source and assertion evidence

To distinguish TURN-T04 correctness from the external test debt without modifying POM or production:

1. The five frozen test files were compiled together with Java 21 against current `target/classes` and the Maven test dependency classpath using `javac -proc:none`: exit `0`.
2. The already compiled named classes were then executed directly through the configured Surefire plugin:

| Isolated command | Exit | Tests |
|---|---:|---:|
| `mvn -q -Dtest=BagLocalOperationExecutorContractTest surefire:test` | 0 | 4 |
| `mvn -q -Dtest=UiLocalOperationExecutorContractTest surefire:test` | 0 | 4 |
| `mvn -q -Dtest=GiveItemLocalOperationExecutorContractTest surefire:test` | 0 | 3 |
| `mvn -q -Dtest=QuestLocalOperationExecutorContractTest surefire:test` | 0 | 4 |
| `mvn -q -Dtest=LocalServiceStepDispatcherContractTest surefire:test` | 0 | 4 |

These isolated runs are diagnostic evidence only and do not replace the plan-required lifecycle commands. Parent resolution condition: repair or validly partition the unrelated repository test-source compilation debt, then rerun all five exact `mvn -q -Dtest=... test` commands. Worker D makes no approval judgment.

## PARENT TEST SOURCE REVIEW #1 - 2026-07-15 18:36 EDT

- Review authority: parent Codex, independent of the Worker report and isolated execution claim.
- Verdict: `P0/P1/P2=0/0/0` for the five delivered test sources and assertions.
- Status: `TEST SOURCE REVIEW PASSED / REQUIRED MAVEN TEST COMMANDS BLOCKED`; not `CARD APPROVED`.
- Independent assertion evidence:
  - `BagLocalOperationExecutorContractTest.java:35-135` covers all three return-item intents, preserves cached
    absolute coordinates, maps incense true/false to typed JSON, and proves malformed/foreign calls make zero
    BagService calls.
  - `UiLocalOperationExecutorContractTest.java:35-109,177-206` proves the three Service-owned queue operations
    add no adapter exclusive wrapper, while X2 uses one exclusive callback, zero nested callbacks and one direct
    public Service call; queue failure invokes no Service.
  - `GiveItemLocalOperationExecutorContractTest.java:30-74` invokes the complete public Give macro exactly once,
    preserves exact arguments and keeps both true/false as typed completed business results.
  - `QuestLocalOperationExecutorContractTest.java:38-127` proves one capture call produces the same PNG pixels,
    exact absolute origin and source-step index; failed capture has no frame and performs no second capture.
  - `LocalServiceStepDispatcherContractTest.java:38-147,178-339` freezes exactly nine operations, routes each to
    exactly one of the four permanent local Services, verifies exclusive ownership and rejects null/malformed/
    unknown wire operations before queue or Service entry.
  - Parent SHA capture and scoped `git diff --check` found exactly the five frozen test files and no production
    mutation by this Worker.
- Gate evidence retained as a real blocker: all five required `mvn -q -Dtest=... test` commands exit 1 in Maven
  `testCompile` on pre-existing out-of-scope stale tests before the selected class runs. The isolated javac +
  `surefire:test` 19/19 pass is useful diagnostic evidence only and does not satisfy the authoritative commands.
- Resolution condition: after the repository test-source compilation cohort is repaired or parent-authorized
  partitioning exists, rerun all five exact commands and perform a fresh parent result review. T04 test-source
  ownership is released; Kuhn is immediately continued on TURN-13G.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT INTEGRATION RECHECK - REPAIR #1 READY - 2026-07-16T00:54:32-04:00

- 父级在 TURN-10CR 四态 source/test-source 通过后重新检查 TURN-T04 真实 dispatcher test，结论
  `P0/P1/P2=0/1/0 / TEST REPAIR #1 REQUIRED`。这不是 production 返修，也不改变前五份测试的既有审查结论。
- **P1 精确证据：**`LocalServiceStepDispatcherContractTest.java:301-313` 的 `FakeGiveItemService` 仍只 override
  旧 boolean `executeGiveDirectForExclusive(...)`；production `GiveItemLocalOperationExecutor` 已按 TURN-10CR 改为
  调用 `executeGiveFromOpenDialogDirectForExclusive(...)` 并返回
  `GIVEN/GIVE_OPTION_NOT_FOUND/GIVE_ITEM_FAILED/INTERRUPTED` 四态 JSON。现有 dispatcher test 因而既没有拦截
  真实 whole API，也没有在 dispatcher exclusive boundary 上验证四态结果，旧测试结论已被 production 合同演进
  超越。
- Repair #1 唯一 test write set：
  `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java`；
  本报告可 append true EOF。两仓所有 production、其它 test、POM 与报告只读。
- 必须把 fake 改为 override 四态 whole API，默认 `GIVEN`；保留旧 boolean direct override 仅作零调用探针。新增
  production dispatcher-path 测试逐态断言 exact `{"state":"<ENUM>"}`、`COMPLETED/OK`、一次 exclusive callback、
  whole API 一次、legacy direct 零次、其它三个 Service 零次。原九 operation 路由、queue failure、malformed/
  unknown wire 和 ownership 断言全部保留。
- 不改 protocol/dispatcher/adapter/Service/Task/Cloud，不新增 command/retry/session/ledger/TTL；Worker 不运行
  Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不做 Git mutation。父级待所有 Java writers
  稳定后运行 TURN-T04 原六个 named tests 与 DHXY compile。

**无已批准业务差异；本修复只让测试跟上已父级批准的 TURN-10CR 四态 production 合同。**

<!-- TRUE_EOF: TURN-T04 parent-integration-recheck-repair-1-ready -->

## CLAIMED - 2026-07-16T00:57:48-04:00

- Agent: `019f6947-f00a-7780-bdf4-fd8e5643c534`.
- Role: `CR271 TURN-T04 Repair #1 implementation Worker`；不是 reviewer，父级 Codex 是唯一
  manager/final reviewer，本 Worker 不写 `APPROVED`、`CLOSED` 或 review judgment。
- Exact write set（仅以下两项）：
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java`
  - `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T04.md`（仅 true EOF append）
- 两仓所有 production、其它 test、POM/配置/计划/CR 文档全部只读；保护全部并行 dirty/untracked，不回滚、
  覆盖、清理、暂存、提交或改写他人工作，并与当前 TURN-21/25/23 写集互斥。
- 本修复只更新 dispatcher contract test：fake 改接四态 whole API，旧 boolean direct API 仅保留为零调用探针，
  并补齐四态 production dispatcher-path 精确断言；不改 protocol/dispatcher/adapter/Service/Task/Cloud，不新增
  command/retry/session/ledger/TTL/durable workflow。
- 禁止运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input；禁止任何 Git
  mutation。交付只写 `TEST REPAIR DELIVERED`，不作批准或关闭判断。

<!-- TRUE_EOF: TURN-T04 repair-1-claimed-019f6947-f00a-7780-bdf4-fd8e5643c534 -->

## TEST REPAIR DELIVERED - 2026-07-16T01:02:14-04:00

- Agent: `019f6947-f00a-7780-bdf4-fd8e5643c534`，角色仍为
  `CR271 TURN-T04 Repair #1 implementation Worker`；本段仅交付 test repair，最终判断留给父级 Codex。
- 实际 Java 写入仅为
  `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java`；
  本报告仅在 current true EOF append。

### Test methods

1. `routesAllNineClosedOperationsToOnlyFourPermanentLocalServices`（原覆盖保留）
2. `giveItemDispatcherPreservesAllFourWholeApiStatesInsideOneExclusiveRequest`（本次新增）
3. `bagAndGiveQueueFailuresDoNotInvokeAdaptersOrServices`（原覆盖保留）
4. `nullUnknownAndMalformedCallsFailClosedWithZeroServiceCalls`（原覆盖保留）
5. `unknownWireOperationCannotReachDispatcherOrAnyLocalService`（原覆盖保留）

### Exact repair assertions

- 新测试显式冻结 `OpenDialogGiveState` 恰好四态，并逐态断言 exact JSON：
  - `GIVEN -> {"state":"GIVEN"}`
  - `GIVE_OPTION_NOT_FOUND -> {"state":"GIVE_OPTION_NOT_FOUND"}`
  - `GIVE_ITEM_FAILED -> {"state":"GIVE_ITEM_FAILED"}`
  - `INTERRUPTED -> {"state":"INTERRUPTED"}`
- 每个状态均通过 production `LocalServiceStepDispatcher -> GiveItemLocalOperationExecutor` 路径，断言
  `LocalServiceExecution.status=COMPLETED`、`code=OK`、`frame=null`、exclusive request=`1`、callback=`1`、
  nested exclusive=`0`、whole API=`1`、legacy boolean direct=`0`、Bag/UI/Quest Service=`0`。
- `FakeGiveItemService` 现 override
  `executeGiveFromOpenDialogDirectForExclusive(...)`，默认结果为 `GIVEN`；旧
  `executeGiveDirectForExclusive(...)` 只递增 `legacyDirectCalls`，仅作为零调用探针。
- 原九 operation 精确路由、四个永久本地 Service ownership、Bag/Give 与 UI-X2 exclusive ownership、
  no-nested、queue failure callback/service 零调用、malformed/null/unknown wire fail-closed 以及 Quest frame
  absolute origin/source-step assertions 均保留；Give owner 计数现同时纳入 whole 与 legacy probe，防止旧 API 回流。

### File evidence

- `LocalServiceStepDispatcherContractTest.java`：390 行，5 个 `@Test`，花括号 `55/55`，尾随空白 0 行，
  SHA-256 `3AA4997CC8527F16A4738434934B1F8527701E5C4876B3E085A182CE8D5F5BF4`。
- 本报告在本交付段 append 前为 217 行，SHA-256
  `5B80DA94F5ADFDB63F606B0FD92D707436CD96A53D6371FA94147051A95476D1`；append 后为 `271` 行。
  报告最终 SHA 不写入自身，避免 self-reference；父级可在读取本 true EOF 后独立计算。

### Scope and gates

- 两仓 production、其它 test、POM/配置、权威计划、CR 文档及 TURN-21/25/23 写集均未修改；未新增
  protocol/dispatcher/adapter/Service/Task/Cloud 代码，也未新增 command/retry/session/ledger/TTL/durable workflow。
- 全部并行 dirty/untracked 保留；未回滚、覆盖、清理、暂存或提交任何他人改动，未执行 Git mutation。
- 按冻结禁令，本 Worker 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、
  capture 或 input；以上仅为完整源码回读、符号计数、结构检查与文件哈希证据，不冒充运行门禁结果。
- 父级待 Java writers 稳定后执行 TURN-T04 六个 named tests 与 DHXY compile；本 Worker 不作最终审查判断。

**无已批准业务差异；本次只让 dispatcher test 追平已冻结的 TURN-10CR 四态 production 合同。**

<!-- TRUE_EOF: TURN-T04 repair-1-test-repair-delivered-agent-019f6947-f00a-7780-bdf4-fd8e5643c534 -->

## PARENT TEST-SOURCE RE-REVIEW - REPAIR #1 PASSED - 2026-07-16 01:05 EDT

- 审查角色：CR271 父级唯一 manager/final reviewer。父级独立复读完整 test、production
  `LocalServiceStepDispatcher`、production `GiveItemLocalOperationExecutor` 与 TURN-10CR 四态 Service API，并重算
  test SHA；不以 Worker 自述替代结论。
- 结论：`P0/P1/P2=0/0/0 / TEST SOURCE REVIEW PASSED / NAMED TEST+DHXY COMPILE PENDING`。Kepler owner
  可释放；这不是测试运行通过或 CARD CLOSED。
- `LocalServiceStepDispatcherContractTest.java:75-107` 对
  `GIVEN/GIVE_OPTION_NOT_FOUND/GIVE_ITEM_FAILED/INTERRUPTED` 四态逐一调用 production dispatcher；每态精确断言
  `COMPLETED/OK/{"state":"<ENUM>"}`、一个 exclusive callback、whole API 一次、legacy direct 零次及其它三个
  Service 零调用。
- test fixture 在 `:215-233` 真实组装 production dispatcher 与四个 production adapter；fake 在 `:338-362`
  override 四态 whole API，旧 boolean API 只计数。因此本测试不会再绕过 production
  `GiveItemLocalOperationExecutor.java:47-56` 的四态 JSON 路径。
- 原九 operation ownership、no-nested、queue failure、malformed/null/unknown wire 与 Quest frame 断言均保留。
  test SHA `3AA4997CC8527F16A4738434934B1F8527701E5C4876B3E085A182CE8D5F5BF4` 与交付一致。
- 父级未运行 Maven/JUnit/compile，因为 TURN-23 Java writer 仍活动；TURN-T04 六个 named tests 与 DHXY compile
  进入 stable-writer cohort。

**无已批准业务差异；本测试只追平已批准的 TURN-10CR 四态 production 合同。**

<!-- TRUE_EOF: TURN-T04-parent-repair-1-passed -->
