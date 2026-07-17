# TURN-10E Permanent-Local Service Dispatcher Integration

## CLAIMED

- 领取时间：`2026-07-15T16:02:23-04:00`；状态：`CLAIMED`；`countDelta=0`。
- 角色：CR271 Internal implementation Worker；父级是唯一 manager/final reviewer。
- 前置：父级已批准 `TURN-10D`，并释放 owner 后续派本卡。
- 唯一 Java 写集：`src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`。
- 唯一报告：`docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-10E.md`。
- 只读依赖：四个 approved local adapter、`InputSequences`、冻结 `TurnLocalServiceCall`/
  `TurnLocalOperation` 与 `LocalServiceExecution`。禁止修改 adapter、永久本地 Service、协议 DTO、计划、CR 或其它文件。
- 已核对队列边界：Bag/Give adapter 只允许既有 exclusive callback 内调用；UI/Quest adapter 必须从 input worker 外
  调用。保护全部领取前 dirty/untracked，不回滚、覆盖、清理或提交。
- 当前有并行 Java writers；不运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，不执行 Git
  mutation。

`CLAIMED`

## SOURCE DELIVERED

- 交付时间：`2026-07-15T16:03:14-04:00`；状态：`SOURCE DELIVERED / PARENT REVIEW PENDING`；
  `countDelta=0`。Worker 到此停止，不自批、不领取下一卡。
- 唯一 Java 交付：`src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`；SHA-256
  `0056DDB966AEF036E345B272638158B3DDCBB34A12E06F5DFF134C54CF5DFB06`。
- `:14-33` 为 Spring `@Component`，constructor 恰好注入四个 approved adapter 与 `InputSequences`；没有直接
  permanent-local Service 依赖或第五 Service。
- `:46-49` 的 public `execute(call, sourceStepIndex)` 明确从 input worker 外调用；null call/operation 在任何 queue
  前返回 FAILED `INVALID_LOCAL_SERVICE_CALL`。
- `:51-63` 对 `BAG_RETURN_ITEM`/`BAG_USE_INCENSE` 只创建一次
  `submitExclusiveAndWait("turn:local-service:" + operation, ...)`；callback 只 store 一次
  `bagAdapter.execute(call)` 并返回 true。queue false 或未 store result 时只返回 FAILED
  `LOCAL_SERVICE_INPUT_FAILED`，没有 rerun/fallback。
- `:64-75` 对 `GIVE_ITEM_FROM_OPEN_DIALOG` 使用同一条一次性 exclusive 规则，callback 只调用一次
  `giveItemAdapter.execute(call)`；失败路径同样不重跑。
- `:76-78` 四个 UI operation 直接从 queue 外调用 `uiAdapter.execute(call)`，没有包裹；两个 Quest operation
  直接调用 `questAdapter.execute(call, sourceStepIndex)`，保留其 public Service 自有 queue 与 Quest frame 来源步骤。
- 两个 exclusive description 仅含冻结 operation enum，不记录任意 payload。dispatcher 不 catch/转换 adapter 异常，
  不含 reflection、arbitrary map、`InputProvider`、capture/OCR/match/click、business fallback、retry 或 wrapper helper。
- 静态核对：`submitExclusiveAndWait` 恰好 `2` 处；Bag/Give/UI/Quest adapter 调用各 `1` 处；
  `git diff --check` exit `0`。
- 当前仍有并行 Java writers，未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git
  mutation；未修改任一 adapter、Service、protocol DTO、计划、CR 或其它 dirty/untracked。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE APPROVED / BUILD COHORT PENDING

- 父级审查时间：`2026-07-15T16:10:00-04:00`；结论：`P0=0 / P1=0 / P2=0`，
  `SOURCE APPROVED / BUILD COHORT PENDING`，owner 释放。
- 父级独立读取当前 `LocalServiceStepDispatcher.java`，SHA-256
  `0056DDB966AEF036E345B272638158B3DDCBB34A12E06F5DFF134C54CF5DFB06` 与报告一致。
- `:51-75` 的 Bag/Give 两组各只获取一次 exclusive、各只调用一次对应 adapter；queue false/无结果只返回
  `LOCAL_SERVICE_INPUT_FAILED`，没有重跑或 fallback。`:76-78` 的 UI/Quest 从 queue 外直接调用，避免
  queue-in-queue，Quest 保留真实 `sourceStepIndex`。
- constructor 只注入四 adapter 与 `InputSequences`；无 permanent-local Service 直连、第五 Service、反射、任意
  map、直接输入、capture/OCR/match/click、业务 fallback 或 retry。
- `TURN-11` 已依赖解锁并立即续派。当前仍有 Java writer，不运行 Maven、tests、runtime/application/server/
  Task/poller/UI/capture/input；hard ledger `189/407`，本卡 `countDelta=0`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**
