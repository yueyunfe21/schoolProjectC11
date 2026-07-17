# CR271 / TURN-35 Wubei Whole-Task HTTPS Turn Card

## PARENT FROZEN WHOLE-CARD SOURCE-START READY - 2026-07-17T01:10:00-04:00

- 状态：`WHOLE-CARD SOURCE-START READY / ZERO OWNER`。
- 类型：既有完整 `TURN-35` 父卡；禁止 tranche、fragment、子卡或多人共享写集。
- sourceDependsOn 已满足：`13C+14+15+21+22+23+28+31+34A+34B`。
- approvalDependsOn：`TURN-26+TURN-27+TURN-T01/T02/T03/T04`、本卡父级 source/test-source review、
  唯一 named test 与 Cloud compile。approval gate 不再阻止 source-start。
- 领取点 production：`WubeiTask.java` 4,329 行，SHA-256
  `dfde0ad08900f2553088a7d304556a2b5a754c4980305199db7b9c9035b720d7`；唯一 test 当前不存在。

## 唯一完整写集

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
2. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`
3. 本固定报告只允许 claim/delivery/return/repair 追加；其余 production/test 全部只读。

## 整卡验收合同

- 从当前 Cloud 字节增量迁移完整 14-state、`FAILED/STOPPED`、retry/fallback、park/yield、维护、回程、
  普通怪/白龙马/黄袍链；不得复制 `696a12b0` 覆盖 TURN-31 等已接受 caller。
- physical input/capture/OCR/local service 只能经现有 HTTPS turn 与四个 closed `LOCAL_SERVICE`；不得新增
  facade、shim、第二 store、TTL、自动 retry 或本地业务编排。
- `TURN-26/27` 必须保持 Task 已用 public caller signature；Worker 不修改 Dialog/Navigation/NpcClick/API 文件。
- 唯一 test 必须从 public Task path 覆盖 `BC4+BASE+TASK+IMG+LS`，包括 14 state、terminal/uncertain、
  exact context、一 invocation 一 UUID/command、raw PNG 与 closed service 正负矩阵；禁止 private reflection、
  source guard、恒真 fake。
- `TaskExecutionContext.builder()` 等当前缺失本地构造必须在本 Task 内迁到已绑定 turn-native entry，禁止加 shim。
- 无已批准业务差异；按 `docs/业务逻辑.md` 五倍规则与唯一基线 `696a12b0` 等价迁移。

## 自行领取协议

Worker 领取前必须重读三张 TURN-35/36/37 原卡 EOF 和写集 SHA；仅最早在本文件 physical EOF 追加
`EXTERNAL-X TURN-35 WHOLE-CARD CLAIMED` 且回读确认唯一者为 owner。领取后整卡负责 production/test/report/
返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical `OWNER RETURNED`。Java writer 活动时不运行
Maven；稳定后只运行授权 named test 与适用 compile。

<!-- TRUE_EOF: TURN-35 PARENT-FROZEN WHOLE-CARD-SOURCE-START-READY ZERO-OWNER PROD=dfde0ad/4329 TEST=ABSENT APPROVAL-WAITS-26-27-T01-T04 NO-FRAGMENT NO-DISPATCH 2026-07-17T01:10:00-04:00 -->

## PARENT PLAN-CONTRACT AUDIT #1 - SOURCE START SUSPENDED - 2026-07-17T01:32:26-04:00

- 状态改为 `PLAN-CONTRACT BLOCKED / ZERO OWNER`；01:10 的 READY 标记撤销。本卡尚无 claim、production/test
  字节未动，不存在 owner 归还问题。
- TURN-37 的完整传递审计证明本卡同样直接依赖冻结写集外的 `TaskTransactionRunner`、
  `WindowReadyEventBus`、`WindowTaskContextHolder/WindowRuntimeContext`、pathing/dialog-interest state。
  让 Worker 在 Task 内复制这些本地 owner/runtime 或自造轮询会改变 keep-turn/park 语义，禁止实施。
- 统一修正：`TURN-26+TURN-27` 恢复为本卡 `sourceDependsOn`。TURN-27 将拥有唯一 exact-context、无 TTL 的
  Cloud pathing state；本卡只读消费 TURN-26 prepared state 与 TURN-27 pathing state。phase 仍逐次执行，
  transaction result/yield/park/retry/fallback 次数和顺序保持；不得复制 local runtime/event bus。
- TURN-26/27 source pass 后，父级按两卡真实 public API 在本原卡追加 Amendment #2 并恢复 READY。

<!-- TRUE_EOF: TURN-35 PARENT-PLAN-CONTRACT-AUDIT-1 BLOCKED ZERO-OWNER SOURCE-WAITS-TURN26-27 NO-LOCAL-RUNTIME-COPY 2026-07-17T01:32:26-04:00 -->
