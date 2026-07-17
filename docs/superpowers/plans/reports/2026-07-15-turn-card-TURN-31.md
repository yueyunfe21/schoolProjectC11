# TURN-31 - Wubei TaskTracker real caller cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15T22:36:43-04:00

- 类型：`COUNT`；唯一 `countUnit=WubeiTask::taskTrackerCaller`；`countDelta=+1`；startDependsOn=`TURN-29`
  source/test-source review passed。父级是唯一 manager/final reviewer，Worker 不得自批。
- Exact production write set：仅
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`。
- Exact test/report write set：
  - Create/modify only
    `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiTaskTrackerTurnContractTest.java`
  - 本固定报告 true EOF append。
- 其余两仓文件全部只读；不得修改 TaskTracker core、协议、Service、context、POM、其它 Task/测试/报告。
- 先完整核对 `docs/业务逻辑.md` 五倍普通怪、白龙马 probe、黄袍首读/fast cache/chain end、维护中读 tracker 与
  `696a12b0` Task 源码。真实 runnable caller 只消费 TURN-29 typed Cloud result；保留最新 panel、黄字判断、
  targetMap、first green、fast miss 不 full reread、phase/park/fallback/retry/terminal 完全不变。
- Named test 必须实际调用 production Task caller，覆盖暗雷重抽、probe、普通怪、黄袍首读与后续 fast hit/miss、
  targetMap、click/park/terminal，且每次显式 tracker 调用一个 UUID/command、失败无自动 retry。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不做 Git mutation；交付
  `CLAIMED` 后只写 `SOURCE+TEST SOURCES DELIVERED`，等待父级审查与 stable-writer cohort。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-31 parent frozen brief -->

## CLAIMED - 2026-07-15T22:41:46-04:00

- TURN-31 implementation Worker 已领取；仅写冻结的 `WubeiTask.java`、
  `WubeiTaskTrackerTurnContractTest.java` 与本报告 true EOF。
- 其余两仓文件全部只读；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，
  不做 Git mutation，不自批。

<!-- TRUE_EOF: TURN-31 claimed -->

## BASELINE GATE RECORDED - 2026-07-15T22:54:00-04:00

- DHXY branch/status baseline（只读）：branch=`thin-client-design`；共享工作区已有大量 dirty/untracked，
  本 Worker 不回滚、不覆盖、不清理、不格式化。Cloud branch/status baseline（只读）：
  branch=`navigation-migration`；同样保留全部既有 dirty/untracked。
- 冻结业务源码证据：Cloud `WubeiTask.java` 当前 Git blob=
  `7c85ca645494623f102ca0ccd873bb4ef74e41c3`，与
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  的 blob 完全一致；当前 Cloud 文件 SHA-256=
  `3AD35D8FC6BEEE49F301952C1A0E7F14BC780F2DB828B54C7E2B258089B0684F`。
- 已核对权威计划第 14-19 节、`docs/业务逻辑.md` 第 215-1030 行五倍 tracker 全部合同，以及
  暗雷重抽、白龙马 probe、普通怪、`targetMapName`、黄袍首读/fast cache/chain end、绿字 click、
  park/fallback/retry/terminal 对应的 `696a12b0` production call path。
- 实施边界：只把接任务后异步 tracker read 从旧 window-thread binding 改为绑定传入的 exact
  `TaskExecutionContext` 后调用 TURN-29 typed Cloud Service；不改变 tracker 结果解释、phase、顺序、
  click、park、fallback、retry 次数或 terminal。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-31 baseline gate -->

## REPLACEMENT CLAIMED - 2026-07-15T23:03:31-04:00

- TURN-31 replacement implementation Worker 已接替上一位不可达 Worker；保留共享工作区与原卡全部既有内容。
- 已确认唯一 production 写集内存在部分改动：`WubeiTask.java` 已加入 exact
  `TaskExecutionContextHolder` 绑定的 post-accept tracker read；唯一 named test 尚不存在。本 Worker 将在该部分
  改动基础上继续，不回滚、不覆盖、不清理、不提交，也不修改任何其他 Worker 文件。
- 继续严格限制为冻结的 `WubeiTask.java`、`WubeiTaskTrackerTurnContractTest.java` 与本报告 true EOF；
  不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不做 Git mutation，不自批。

<!-- TRUE_EOF: TURN-31 replacement claimed -->

## SOURCE+TEST SOURCES DELIVERED - 2026-07-15T23:32:05-04:00

- Production source：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`；
  SHA-256=`DFDE0AD08900F2553088A7D304556A2B5A754C4980305199DB7B9C9035B720D7`。
- Named test source：
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiTaskTrackerTurnContractTest.java`；
  SHA-256=`5514FA3CDED30F4C1DADDDE5BB06AE5EE3454D79F4256A4CBF61AE02FC5A097C`。
- Production 证据：接任务后的真实异步 caller 现在显式接收原 `TaskExecutionContext`，在后台线程通过
  `TaskExecutionContextHolder.callWith(context, ...)` 调用 TURN-29 typed
  `TaskTrackerPanelService.readWubeiTrackerPanel(source)`；该方法内仍只有一次 tracker Service 调用且没有新增自动
  retry。去掉本卡这组 exact-context 接线差异后，当前 production 源码与
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线 4319 行逐行一致。
- Named test 证据：测试实际反射调用 production `WubeiTask` caller；真实 TURN-29 Service + fake command port
  覆盖 COMPLETED/FAILED/STOPPED/UNCERTAIN，并逐类断言 exact device/window、一个 UUID、一个 command、单次
  full-window CAPTURE、失败不自动重发。typed scripted panel 驱动同一 production phase 覆盖暗雷重抽边界、
  白龙马 probe、普通怪、黄袍首读建 fast cache、后续 fast hit/miss 与 chain end；同时固定
  `targetMapName`、first-green move/120ms/click 顺序、click retry、park wait spec 和 FAILED terminal。
- 旧路径静态证据：production 对 `readWubeiTrackerPanelFromSnapshot`、
  `getCroppedTaskDetailInTrackerPanel`、`scanWubeiTrackerGreenLinksForReplay`、`TaskTrackerReadRequest`、
  `TaskTrackerMaterializeRequest`、旧 `windowTaskContextHolder.callWith(runtime, ...)` 均为零引用；黄袍 fast
  hit/miss 分支仍先于 full read，且 fast miss 分支内无 full tracker reread。
- 已核对 `docs/业务逻辑.md` 五倍普通怪/白龙马/黄袍 tracker 规则和权威计划第 14-19 节；普通怪首绿字、
  黄字/title 判定、`targetMapName`、phase/click/fallback/park/retry/terminal 均保持原顺序与原次数。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

- 按父级冻结禁令，本 Worker 未运行 Maven、JUnit、compile、runtime、application/server、Task、UI、capture 或
  input；未执行任何 Git mutation。全部构建/测试/运行门仍待父级与 stable-writer cohort 执行，本记录仅交付
  production/test source，不作审批或关闭结论。

<!-- TRUE_EOF: TURN-31 source and test sources delivered -->

## PARENT SOURCE+TEST SOURCE REVIEW PASSED - 2026-07-15T23:41:00-04:00

- Parent independent review: `P0/P1/P2=0/0/0`; Worker delivery prose was not accepted as review evidence.
- Baseline/current production comparison confirms the only caller cutover is
  `afterAcceptTaskSucceeded(...) -> schedulePostAcceptTrackerPanelRead(context,state)` and, inside the async thread,
  `TaskExecutionContextHolder.callWith(context, ...)` around exactly one TURN-29 typed read
  (`WubeiTask.java:2011-2071`). The frozen one-second delay, interruption/exception handling and downstream explicit
  fallback/recovery behavior remain unchanged from `696a12b0`.
- `WubeiTask.java:2359-2416` retains the existing anchor-recovery/read-phase order. `:4001-4055` retains 黄袍 fast
  hit/miss before the later explicit full read, so fast miss does not silently open another tracker command.
- `WubeiTaskTrackerTurnContractTest.java:101-134` proves exact async context binding and one UUID/one command for
  COMPLETED/FAILED/STOPPED/UNCERTAIN without an outer task-thread binding. `:136-257` exercises production Task
  branches for 暗雷、probe、普通怪、黄袍 first read/fast hit/fast miss/chain end; `:259-329` freezes park,
  terminal, first-green order, target-map gate and forbidden legacy transport references.
- Independent SHA-256 verification matches the delivered production/test values. Production searches found zero
  snapshot/materialize/replay caller and zero old async window-holder binding.
- Result: source and named-test source review passed. Named Maven/JUnit and applicable Cloud compile/build remain
  pending in the stable-writer cohort; this is not yet `CARD APPROVED/CLOSED`.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-31 PARENT SOURCE+TEST SOURCE REVIEW PASSED -->
