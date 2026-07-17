# W-COUNT-SUMMON-BUSINESS-DIALOG-1 Worker I34 Report

CLAIMED

- task: `W-COUNT-SUMMON-BUSINESS-DIALOG-1`
- claimedAt: `2026-07-15T04:43:19-04:00`
- countUnit: `SummonSkillService::handleBusinessDialogDuringSkillClean`
- requestedCountDelta: `+1`
- appliedCountDelta: `0`
- 唯一 Java 写集：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`
- 唯一报告写集：本文件

## Implementation Result

- role: Internal implementation Worker I34；只实现/核对，不是 reviewer。
- disposition: `NO_CODE_CHANGE / BLOCKED_SHARED_LANE / countDelta=0`。
- Java change: none；active Cloud `SummonSkillService.java` 保持原状。
- blocker: count unit 没有形成真实 active Cloud caller -> typed DHXY mechanics -> closed terminal 链；唯一
  Java 写集不足以安全补齐，禁止在本类造 stub、旁路点击或新的 wrapper。

## Baseline Gate

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md`、whole-Service 计划、迁移矩阵和两仓 `git status`。
- 业务权威为 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；未使用 `0114604e` 改写业务语义。
- `696a12b0` 与 active Cloud 在本单的循环语义一致：
  - `cleanTailNormalSkillsDirect` 在检查每个 slot 前调用一次
    `handleBusinessDialogDuringSkillClean("before-slot-N")`；
  - `UNKNOWN` slot 返回失败前再调用一次
    `handleBusinessDialogDuringSkillClean("unknown-slot-N")`；
  - 仅 `BUSINESS_OPTION_CLICKED` 返回 true；成功后 `handledBusinessDialogs++` 并 `continue`，因此仍检查
    同一 slot；最多处理 3 次，之后恢复原 slot 判定/unknown fallback。
- stop、deadline、slot index、handled-count、fallback 和 continuation 顺序均未改动。

`无已批准业务差异；按基线等价迁移`。

## Blocking Evidence

### P1-1 - production Cloud public entry does not reach this count unit

- active Cloud `SummonSkillService.java:172-182` 的 public
  `cleanSummonSkillsOnce(SummonSkillCleanupRequest)` 只有在线程名包含
  `dhxy-input-action-worker` 时才进入 `cleanSummonSkillsOnceDirect`；普通 Cloud 调用在 `:178` 直接进入
  `runSummonSkillWholePass`。
- `isInputWorkerThread` 位于 `SummonSkillService.java:1049-1051`，只按本进程线程名判断；正常 Cloud task
  线程不是 DHXY input worker。
- 两个目标 caller 位于 direct 状态机 `SummonSkillService.java:409-411` 和 `:548-550`。因此当前真实
  public Cloud caller 默认经 whole-pass remote capability 绕过这两个 Cloud private caller。
- 影响：仅证明 private 方法和基线一致，不能证明本 count unit 的 active reachable chain；按 hard-count 门不得
  `+1`。

### P1-2 - maintenance dialog terminal is still Cloud-local capture/input, not typed DHXY mechanics

- `SummonSkillService.java:596-604` 调用
  `DialogService.handleDialog(handleMaintenanceBroadcastOption(...))` 并消费
  `DialogResultStatus.BUSINESS_OPTION_CLICKED`。
- `DialogService.java:151-155` 将该请求路由到
  `handleMaintenanceBroadcastOptionFastPath`；`:266-301` 按 heal-pet -> repair-equipment 顺序执行。
- 关键 mechanics 仍在 Cloud `DialogService.java:361-422`：
  `GameClientTracker.refreshWindowState/captureToFile`、Cloud-local `ImageFinder`、
  `InputProvider.clickLeft` 或 `InputSequences.clickLeft`、sleep，最后才本地构造 `DialogResult`。
- active `LocalMacroKind` 只有现有 `DIALOG_DETECTION`、prepared validation、OCR image/words、white-story 等；
  没有 maintenance-business-dialog capture+match+click 的 closed kind/result。现有 typed Dialog ports 也没有
  覆盖上述连续 mechanics。
- 影响：若强行让 Cloud direct caller可达，会在 Cloud 主机读取窗口并发送物理输入；若只复用
  `DIALOG_DETECTION`，又不能保持 baseline 的固定 ROI、绿->黄模板优先、命中点击、800-1099ms settle 与 closed
  `BUSINESS_OPTION_CLICKED/NOT_FOUND/INTERRUPTED/FAILED` terminal。两者都不满足 thin-client 边界。

## Exact Repair Condition

本任务保持 `BLOCKED_SHARED_LANE`，不得拆成 DTO/helper/filler 计数。父级在当前 External C shared 29-Java
Dialog/TaskTracker writer 稳定并释放共享 wire 后，必须把同一 `countUnit` 扩为一次完整互斥实现单：

1. 选择唯一 active owner：要么让 Cloud `cleanSummonSkillsOnce` 真正调用该 Cloud count unit，要么明确该业务
   单元随既有 `SUMMON_SKILL_WHOLE_PASS` 在 DHXY local mechanics 内执行；不得两边都跑。
2. 若保留 Cloud owner，新增/复用一个 closed typed DHXY local macro，一次性完成 exact binding 下的
   heal-pet ROI -> repair-equipment ROI、绿后黄模板、点击与 settle，并返回 closed typed terminal。
3. Cloud 只消费 terminal 并保持 `BUSINESS_OPTION_CLICKED -> handledCount++ -> same-slot continue`、最多 3 次、
   stop/deadline/fallback/order 不变。
4. 返修写集必须包含实际 Cloud contract/port/wire 与 DHXY handler/mechanics 所需全部文件；不得在
   `SummonSkillService.java` 中伪造 terminal，且不得新增 owner/session/TTL/retry/wrapper。
5. 父级独立源码审查和适用 fresh Maven 门均通过后，才允许该单 `countDelta=+1`。

## Static Verification And Scope

- 未修改 Java；未改任何共享 wire、Task、Runner 或 test。
- 未运行 build/test/runtime/application/server/host/Task/poller/UI/capture/input/Git mutation。
- 未回滚、覆盖或清理两仓任何 dirty/untracked。
- 仅按任务允许写入本固定报告。

`DELIVERED | task=W-COUNT-SUMMON-BUSINESS-DIALOG-1 | worker=I34 | countUnit=SummonSkillService::handleBusinessDialogDuringSkillClean | requestedCountDelta=+1 | appliedCountDelta=0 | Java=NO_CODE_CHANGE | disposition=BLOCKED_SHARED_LANE | P0=0 | P1=2 | P2=0 | parentReview=PENDING`

## Parent Source Review #1 - 2026-07-15T04:58:00-04:00

父级独立核验：public `cleanSummonSkillsOnce` 当前转入 `runSummonSkillWholePass`，未到达旧 private 两个
caller；即使恢复该 caller，`DialogService:361-422` 的 maintenance branch 仍在 Cloud 本机 capture/template/
click，未形成 typed DHXY terminal。结论 **P0=0/P1=2/P2=0，BLOCKED_SHARED_LANE /
countDelta=0**。返修须在 shared lane 稳定后确定唯一 owner，并以同一 count unit 接 closed DHXY local
macro，保持 heal-pet -> repair-equipment、绿/黄模板、click settle、最多三次及 terminal 顺序；禁止造第二
owner 或伪 terminal。
