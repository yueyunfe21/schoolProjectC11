# Cloud SummonSkill Count Unit - Worker I5

`CLAIMED | worker=Internal Count Worker I5 | role=implementation-only | task=W-COUNT-SUMMON-SKILL-WHOLE-1 | countUnit=SummonSkillService::cleanSummonSkillsOnce | requestedCountDelta=+1 | claimedAt=2026-07-15T00:42:40-04:00`

## 交付结论

- 状态：`SOURCE CLOSED / DELIVERED`。当前共享源码已经闭合真实
  `TaskMaintenanceService caller -> Cloud SummonSkillService -> typed DHXY whole-pass -> closed terminal -> Cloud result/state`
  链；本 Worker 不制造重复 Java diff。
- 请求计数：`countDelta=+1`；当前计数入账：`0`。父级源码审查和统一 fresh build 通过前不得计数。
- 业务权威：DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `SummonSkillService.java`；Cloud migration-baseline Git blob 为 `d8afb9e2`。
- 已核对 `docs/业务逻辑.md` 的“召唤兽三技能维护 / 技能格静态边界识别”规则，以及迁移矩阵中
  `SummonSkillService`、`SummonSkillTailBoundaryScanner`、`TaskMaintenanceService` 的 caller/state/timeout/
  queue-in-queue 条目。本单不改变任何召唤兽技能业务条件。
- 无已批准业务差异；按 `696a12b0` 等价迁移。
- 未运行 Maven、测试、runtime/application/server/host/Task/poller/UI/capture/input；未执行 Git mutation。

## 基线与工作树

| repository | branch / HEAD | scoped baseline evidence | protection |
|---|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 业务基线固定为 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；typed handler 与 SummonSkill 源码只读 | 保留全部 dirty/untracked；未改 Java |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | active `SummonSkillService.java` SHA-256 `2ee437f1b82470da43fd94c02e934e4eb757e3cf04a43535aee3e0e36ccbd1f5` | 整个 `src/main/java/com/bot/**` 仍是共享 untracked 写集；未覆盖他人内容 |

两仓 `git status --short --branch` 均显示大量并行 dirty/untracked。I5 只读这些现状；唯一写入是本报告。

## 696 方法图对照

scoped 方法清单结果：baseline `43`、active Cloud `46`；baseline missing `0`。多出的 3 个方法仅为
`runSummonSkillWholePass`、`toCleanupResult`、`toSlotStatus`，共同把原 public 入口接到已经存在的 typed
whole-pass terminal，没有第二套 owner/session/permit/ledger/TTL/retry。

| baseline graph | 保留证据 | active path / responsibility |
|---|---|---|
| `cleanSummonSkillsOnce()`、`cleanSummonSkillsOnce(request)` | public 签名不变；默认 request 仍走 builder defaults | `TaskMaintenanceService` 的真实 request caller 进入 typed whole pass；input-worker 分支仍进入 direct mechanics |
| `cleanSummonSkillsOnceDirect`、`openSummonSkillPanel(Direct)` | 方法体在 scoped diff 中无 hunk | 40 秒 absolute deadline；Alt+O、anchor、必要拖板、技能页点击顺序不变 |
| `cleanTailNormalSkills(Direct)` 两层 | 方法体无 hunk | 6/8 槽判断、cached count/start、逐槽扫描、普通技能删除、`MAX_DELETE=5` 不变 |
| `scanLockedBoundary`、`SummonSkillTailBoundaryScanner` | Cloud helper SHA-256 `18027108a3b27d78c68c92a95fb3ff2e18af71a38e777bc4d2c9c5997bcde01d` | LOCKED 尾边界向前回扫；NORMAL 删除、KEEP 停、EMPTY 绝技检查、UNKNOWN 失败 |
| `maybeClickUltimateCorner` | 方法体无 hunk | 角标 hover/template/click、生成后重读、普通技能删除与 result 字段不变 |
| `inspectSkillSlot(Direct)`、`inspectCurrentHoverTip` | 方法体无 hunk | `NORMAL/KEEP/EMPTY/LOCKED/UNKNOWN` 五态；无法确认一律 UNKNOWN fail-closed |
| `deleteSkillAtSlot(Direct)`、`findForgetConfirmButton` | 方法体无 hunk | slot -> delete -> confirm template -> confirm click 的 delay/fallback 顺序不变 |
| capture/template/geometry helpers | 方法体无 hunk | tooltip ROI、黄字洗图、模板优先级、坐标换算、随机点与 temp path 语义不变 |
| debug methods、`isInputWorkerThread`、deadline helper、result types | 全部保留 | debug 不在真实 caller；worker 检测继续防 queue-in-queue；终局 result shape 不变 |

## 允许差异对照

`git diff --no-index migration-baseline/696a12b0/.../SummonSkillService.java active/.../SummonSkillService.java`
只有以下既有 hunk：

1. 注入共享 `TaskExecutionContextHolder`，从 `AutoBattleTask.callWith(context, ...)` 读取 authority-minted current context；缺失时 fail closed，不合成 default/epoch 0。
2. non-input-worker 的 `cleanSummonSkillsOnce(request)` 由 Cloud 本地 `submitExclusiveAndWait` 改为一次
   `context.getRemoteGameClient().summonSkillWholePass().execute(intent)`；input-worker direct 分支保留。
3. 新增 4-field intent 与 9-field/5-enum result 映射；`LinkedHashMap` 保持 slot observation 顺序。
4. 原两个 UI cleanup 调用点替换为既有 `CloudUiCleanerPort` typed 调用；调用时机和 caller 丢弃 boolean 的语义不变。

除此之外，696 的 panel/slot scan、ordinary delete、ultimate corner、locked-tail scan、6/8 layout、deadline、
`MAX_DELETE_SKILL_COUNT_PER_RUN=5`、delay/fallback 与 result construction 均无 diff。I5 未新增或修改 Java。

## 完整真实调用链

1. Cloud `AutoBattleTask.execute(context)` 在共享 `TaskExecutionContextHolder.callWith(context, ...)` 内运行 patrol，
   `maybeRunIdleMaintenance` 把同一 context 传给 `TaskMaintenanceService.runOpportunisticMaintenance`。
2. Cloud `TaskMaintenanceService.java:578-797` 保留 broadcast-first、due/free-state、team round/capability/claim、
   tail-safe cache、cooldown、UNKNOWN backoff 和 `GameContext.ActionState.INTERACTING` finally 恢复；`:755` 调用
   `summonSkillService.cleanSummonSkillsOnce(cleanupRequest)`。
3. Cloud `SummonSkillService.java:172-227` 保留 input-worker direct 分支；正常 Cloud caller 从同一 holder 取得 exact
   context，把 `expectedSkillCount/trustExpectedSkillCount/startSlotIndex/skipUltimateCornerCheck` 一一写入
   `WholePassIntent`，并只调用一次 `summonSkillWholePass().execute(intent)`。
4. Cloud retained authority 用固定 `ActionAddress("summon-skill", "whole-pass")`、当前 generation/binding 和现有
   action ledger 进入 `CloudTaskRunCommandExecutor -> RemoteGameCommandBroker`；UNKNOWN 保留 unresolved fence，
   不自动重发，不新建 TTL/retry。
5. DHXY `LocalRemoteGameCommandHandler.java:2655-2778` 解码 4-field request，在 exact window/task-run admission 下
   打开现有 in-flight exclusive handle，并通过 `InputActionQueue.submitRemoteExclusiveAndWaitDetailed` 执行一次
   `SummonSkillService.cleanSummonSkillsOnce(request)`。
6. 回调已在唯一 input worker 中，因此 DHXY `SummonSkillService.isInputWorkerThread()` 直接进入
   `cleanSummonSkillsOnceDirect`，不会在 exclusive callback 内再次 submit queue；面板、扫描、点击、delay 与
   fallback 作为一个 retained whole pass 执行。
7. DHXY handler 关闭 owner 后返回 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` 和 owner proof；Cloud authority
   final-consume terminal，冻结 result/state；Cloud `SummonSkillService` 映回 `SummonSkillCleanupResult`，再由
   `TaskMaintenanceService` 的原 finally 更新或保留 cooldown/cache/backoff/state。

## Terminal 与状态

| terminal | Cloud SummonSkillService | Cloud retained/state | TaskMaintenance consequence |
|---|---|---|---|
| `Executed(CleanupValue)` | 9 字段、5 slot enum 一一映射 | final consume，occurrence complete | 仅 `success=true` 更新 clean timestamp/cache；原 ultimate-success-before-failure 仍保留 |
| `NotExecuted(message)` | 映为 `SummonSkillCleanupResult.failed` | closed no-owner proof | 不写成功时间；进入原 failed/no-state-change 处理 |
| `Stopped(message)` | `TaskFatalException` 直接 unwind | closed STOP terminal | 不转普通失败，不另启 pass |
| `Unknown(message)` | `TaskFatalException` 直接 unwind | unresolved fence held | fail closed，零自动重发，零新 cooldown truth |
| `InterruptedException` | 恢复 interrupt flag 后 `TaskFatalException` | 当前 invocation 不伪装完成 | 不继续 cleanup result/state 路径 |

## 文件表

| file | action | evidence |
|---|---|---|
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java` | Read only / reuse | active SHA-256 `2ee437f1...`; 43/43 baseline methods present + 3 typed adapter methods |
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java` | Frozen / read only | active SHA-256 `39aef808...`; real caller at line 755 |
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/**` | Frozen / read only | existing capability, authority, executor, broker and retained state reused |
| `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` | Frozen handler branch / read only | active SHA-256 `b1cd28fa...`; whole-pass branch at 2655-2778 |
| `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/service/SummonSkillService.java` | Read only | active SHA-256 `0b89ca46...`; existing input-worker direct mechanics reused |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-summon-skill-count-unit-worker-i5.md` | Add | I5 claim, baseline, call graph, terminal and scoped evidence |

未修改/新增 SummonSkill contract/port/assembly：现有 typed whole-pass 已经闭合，新增第二套专属 wire 会重复
authority。冻结的 generic `LOCAL_MACRO` shared 12、TaskMaintenance、A/B/C/D、I2/I4、CommonBox、Navigation、
PlayerState、BattleRadar、Dialog、Npc 均未触碰。

## Scoped 静态检查

- baseline/active 方法清单：`43 / 46`；baseline missing `0`；added `3`，均为 typed whole-pass adapter。
- scoped no-index diff：仅 context/whole-pass/result mapper 与既有 UI-clean port substitution；完整 baseline private
  business graph无其它 hunk。
- `git diff --no-index --check`：无 whitespace error；命令以 `1` 退出仅表示预期存在上述 scoped diff。
- constants/source grep：`CLEAN_ONCE_TIMEOUT_MS=40_000L`、`MAX_DELETE_SKILL_COUNT_PER_RUN=5`、6/8 slot arrays、
  `UNKNOWN` fail-closed、worker direct path、ultimate/locked-tail methods均存在。
- DHXY handler grep：whole-pass 只经过一次 `submitRemoteExclusiveAndWaitDetailed`；回调内调用现有
  `cleanSummonSkillsOnce`，由 input-worker direct 分支防止 queue-in-queue。
- terminal/state grep：DHXY 四态 producer 与 Cloud `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` final/unknown fence
  分支完整；9-field cleanup 和 5-enum映射完整。
- 未运行 Maven/tests/runtime/application/server/host/Task/poller/UI/capture/input；统一构建门由父级执行。

`DELIVERED | task=W-COUNT-SUMMON-SKILL-WHOLE-1 | countUnit=SummonSkillService::cleanSummonSkillsOnce | requestedCountDelta=+1 | countApplied=0 | businessDifference=NONE | sourceReview=PARENT_PENDING | buildGate=PARENT_PENDING`

## Parent Source Review #1 - SOURCE APPROVED / COUNT PENDING BUILD - 2026-07-15T00:54:19-04:00

父级独立核对 `TaskMaintenanceService:755` 的真实 caller、Cloud `SummonSkillService` 的 43/43 基线方法图、
现有 `SUMMON_SKILL_WHOLE_PASS` typed authority、DHXY handler 单次 remote exclusive 与 input-worker direct mechanics。
原 40s deadline、6/8 槽、`MAX_DELETE_SKILL_COUNT_PER_RUN=5`、普通/保留/空/锁定/未知五态、角标与删除确认顺序、
terminal/state 映射均保留；没有第二套协议或新业务门。

结论：**P0=0 / P1=0 / P2=0，SOURCE APPROVED。** `countUnit=SummonSkillService::cleanSummonSkillsOnce`
进入统一 fresh build 队列；构建前 `countApplied=0`，Cloud package 与适用 DHXY compile 通过同轮才原子 `+1`。
