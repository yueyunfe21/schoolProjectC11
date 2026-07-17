# TURN-32 - FiveRing TaskTracker real caller cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15T22:36:43-04:00

- 类型：`COUNT`；唯一 `countUnit=FiveRingTaskV2::taskTrackerCaller`；`countDelta=+1`；startDependsOn=`TURN-29`
  source/test-source review passed。父级是唯一 manager/final reviewer，Worker 不得自批。
- Exact production write set：仅
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`。
- Exact test/report write set：
  - Create/modify only
    `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingTaskTrackerTurnContractTest.java`
  - 本固定报告 true EOF append。
- 其余两仓文件全部只读；不得修改 TaskTracker core、协议、Service、context、POM、其它 Task/测试/报告。
- 核对 `docs/业务逻辑.md` 五环 tracker prepared-action/wakeup 与 `696a12b0` Task 源码。真实 runnable caller 只
  消费 TURN-29 typed Cloud result；保持 title/link negative、prepared action、click、phase、park、fallback、
  retry 次数和 terminal 不变，不得恢复本地绿字扫描/OCR/temp path 或旧 READ/MATERIALIZE transport。
- Named test 必须实际调用 production Task caller，覆盖 prepared hit、title/link miss、click success/failure、
  wakeup/park/terminal/checkpoint 与 stale/mismatch 拒绝；每次显式 tracker 调用一个 UUID/command、失败无自动 retry。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不做 Git mutation；交付
  `CLAIMED` 后只写 `SOURCE+TEST SOURCES DELIVERED`，等待父级审查与 stable-writer cohort。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-32 parent frozen brief -->

## CLAIMED - 2026-07-15T22:42:35-04:00

- TURN-32 implementation Worker 已领取；仅写冻结的 `FiveRingTaskV2.java`、
  `FiveRingTaskTrackerTurnContractTest.java` 与本报告 true EOF。
- 其余两仓文件全部只读；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，
  不做 Git mutation，不自批。

<!-- TRUE_EOF: TURN-32 claimed -->

## REPLACEMENT CLAIMED - 2026-07-15T23:03:25-04:00

- 上一位 TURN-32 implementation Worker 因连接中断不可达；replacement Worker 接管原冻结 brief，继续只写
  `FiveRingTaskV2.java`、`FiveRingTaskTrackerTurnContractTest.java` 与本报告 true EOF。
- 已确认首任 Worker 未留下 Java 半成品：production 文件最后写入早于原 `CLAIMED`，唯一 named test 尚不存在；
  不回滚、不覆盖、不清理共享工作区中的任何其他改动。
- 其余两仓文件全部只读；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，
  不做 Git mutation，不承担 reviewer 或 final reviewer 角色。

<!-- TRUE_EOF: TURN-32 replacement claimed -->

## SOURCE+TEST SOURCES DELIVERED - 2026-07-15T23:31:03-04:00

- Replacement implementation Worker 已完成冻结写集内的 source/test-source 交付；未承担 reviewer 或 final
  reviewer 判断，等待父级审查与 stable-writer cohort。
- Production：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
  - SHA-256：`287FF0EBE4F3CECF9820A10D2FFCBF0F7AED2A26BEB7A5F510D92F540E8A4BDB`
  - 编辑前已核对其 Git blob 为 `f5c5022162b89953216e1787546f4a0c616e5fe0`，与
    `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `FiveRingTaskV2.java` 完全相同；首任 Worker
    未留下 Java 半成品。
  - 真实 `detectHandover` / `syncTaskPanel` 共用 caller 现只显式调用一次 TURN-29
    `prepareWuhuanPathingLink(...)`，直接消费 typed `TaskTrackerPanelPrepareResult`；没有 catch/reissue，
    terminal/uncertain 继续原样上抛。
  - typed negative 按 `TASK_NOT_FOUND`、`TASK_FOUND_NO_GREEN`、`TASK_FOUND_NO_LINK` 精确映射；先校验
    `windowId` / `TaskType.WUHuan_V2` / `wuhuan`，错配只按既有 `TRACKER_UNAVAILABLE` fallback 处理，
    不把 negative 升格为新的业务事实。
  - typed action 仅接受当前 `windowId`、当前 native handle、`TASK_TRACKER_PATHING/wuhuan`、
    `clickRequired=true`、`CLOUD_TRACKER_PANEL_READER` 且满足原有 2.5 秒 prepared 校验的结果；stale/
    mismatch 不点击、不二次读取。
  - 原 runtime prepared-action 仍先于 live Cloud read 消费；move + click 仍在同一个
    `InputSequences.submitAndWait(...)` 序列，成功后仍写 movement/pathing intent 并进入
    `WAIT_PATHING`，失败、phase、park、fallback、retry 次数与下游 terminal 分支未改。
  - 静态扫描确认真实 caller 已无 `findWuhuanNextGreenClickPoint()`，未加入本地绿字扫描/OCR/temp path，
    未加入旧 READ/MATERIALIZE transport，也未加入自动 retry。
- Named test source：
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingTaskTrackerTurnContractTest.java`
  - SHA-256：`EB361B203574BF6D06B879ADD6F708714DF2B11692817384597BA84736DAD32B`
  - 测试通过反射实际进入 production `detectHandover(...)` 与 `syncTaskPanel(...)`，使用真实 TURN-29
    `TaskTrackerPanelService`、纯算法和 turn action/result 协议；仅 fake turn port 与最终物理点击提交边界。
  - 覆盖 prepared hit/click success、`TASK_NOT_FOUND`/no-green/no-link、sync click failure 的
    `SHARED_STATE_TRIGGERED + MUST_YIELD`、watcher prepared wakeup 优先及 `WAIT_PATHING` park、stale/
    window/HWND/negative mismatch 拒绝、FAILED/STOPPED/UNCERTAIN terminal 与 stop checkpoint。
  - 每个显式 tracker 调用均断言恰好一个 UUID、一个 command、一个 action；click/terminal 失败不自动
    retry，stop checkpoint 在 command/UUID/click 之前终止。
  - Cloud 仓当前 `.gitignore` 的 `src/test/` 规则会隐藏该新文件；文件已真实写入指定绝对路径，Worker
    按禁令未做任何 Git mutation，父级 stable-writer cohort 纳入时需显式处理该 ignore 状态。
- 未跑门：按父级冻结 brief，未运行 Maven、JUnit、compile、runtime、application、server、Task、UI、
  capture 或 input；未启动任何应用/服务，未做任何 Git mutation。以上为 source/test-source 静态证据，
  后续门禁由父级 stable-writer cohort 统一执行。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-32 source+test sources delivered -->

## PARENT SOURCE+TEST SOURCE REVIEW PASSED - 2026-07-15T23:44:00-04:00

- Parent independent review: `P0/P1/P2=0/0/0`; Worker delivery statements were independently checked against
  production, named-test source and `696a12b0`.
- `FiveRingTaskV2.java:2471-2555` keeps the baseline prepared-action-first branch, then performs exactly one TURN-29
  typed prepare call. Malformed/negative/mismatched results map to the existing tracker statuses without click or
  re-read; only an exact current-window/HWND/cloud-source/click-required/fresh action reaches physical input.
- `FiveRingTaskV2.java:2565-2570` preserves the single serialized screen-absolute
  `MOVE_MOUSE -> SLEEP(120) -> CLICK_LEFT` sequence. `detectHandover(...)` and `syncTaskPanel(...)` retain their
  existing phase, retry/fallback and park consumers.
- `FiveRingTaskTrackerTurnContractTest.java:107-263` directly invokes production `detectHandover(...)` and
  `syncTaskPanel(...)`, covering prepared priority, title/link negatives, click success/failure, stale/window/HWND
  mismatch, park/wakeup, terminal outcomes and checkpoint-before-command. Every explicit live read asserts one UUID
  and one CAPTURE command; watcher-prepared/checkpoint paths assert zero command.
- Independent SHA-256 verification matches both delivered values. Production search finds one typed
  `prepareWuhuanPathingLink(...)` caller and zero old point/local scan, OCR/temp or READ/MATERIALIZE transport caller.
- Result: source and named-test source review passed. Named Maven/JUnit and applicable Cloud compile/build remain
  pending in the stable-writer cohort; this is not yet `CARD APPROVED/CLOSED`.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-32 PARENT SOURCE+TEST SOURCE REVIEW PASSED -->
