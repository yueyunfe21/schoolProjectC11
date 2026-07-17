# CR271 TURN-34C post-34A/34B latest readiness/collision delta PRECHECK

## PRECHECK 范围与只读边界

- helper role：CR271 Internal 非绑定 readiness helper；不是 implementation Worker、reviewer 或父级。
- snapshot：`2026-07-16T06:57:24.9712541-04:00`。
- 本轮完整读取并交叉核对：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、HTTPS turn 协议、`docs/业务逻辑.md`、TURN-34A 固定卡物理最新尾部及活动源码、TURN-34B 最新 readiness、既有 TURN-34C readiness、`696a12b0` 的 `AutoBattleTask`、当前 Cloud source/test refs 与 DHXY 真实 caller。
- 本报告只提供 PRECHECK、依赖、caller、写集、互斥、基线、named-test 和停工证据；不作卡片批准或阻断裁决，不领取 owner。
- 两仓只执行只读文件、hash、引用、`git status` 与 `git show` 核验；未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，未执行任何 Git mutation。
- DHXY 当前 branch=`thin-client-design`，`git status --porcelain` 共 `85` 项；Cloud 当前 branch=`navigation-migration`，共 `28` 项。全部 dirty/untracked 原样保护。

只读权威快照：

| 材料 | 行数 | SHA-256 |
|---|---:|---|
| `AGENTS.md` | 392 | `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `docs/DHXY_CONTEXT.md` | 1349 | `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| `docs/ACTIVE_WORK.md` | 79840 | `2D62D4B04C6518D0BA24A0928D3572784E4E8A8968C10AEE084C4CBB05347678` |
| 权威计划 | 1690 | `C66D47E335873C06965DFC709EC29B5B9439410BCDDAEC70EFE5E90605A9440F` |
| HTTPS turn 协议 | 383 | `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` |
| `docs/业务逻辑.md` | 1426 | `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` |

## 证据 1 - post-34A/34B 最新 delta

1. 既有 TURN-34C readiness 报告仍为 309 行、SHA-256=`45E50F5A3FF0BC8A6A208A523DD0B4FF3C27A390ED9B8688FB68A226F0C0EF96`。其中 TURN-33、34A、34B 的旧快照已经被后续物理尾部覆盖，不能直接作为当前 dispatch 输入。
2. TURN-34B 最新 readiness 报告保持 198 行、SHA-256=`CA634C75DE5FFB45106ED51904DB61DFD473D5627433EABFF8153EB628E2644C`、mtime=`2026-07-16T06:38:58.4259559-04:00`；本轮未改动该报告。
3. TURN-34A 当前 production `AutoCombatService.java` 为 852 行、SHA-256=`532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9`。固定卡当前为 255 行、SHA-256=`68173F0D1243CE60300B9519E1DDCFC87863A362876E11D71DB5DA008F601362`。
4. TURN-34A 最新父级尾部 `:231-255` 已认定共享 Cloud compile 债不妨碍编写 test source，要求原 External C owner 只补唯一 `AutoCombatServiceTurnContractTest.java`；截至本快照该测试仍不存在，production 尚未形成 source+test 交付与后续父级源码门证据。
5. TURN-34B 尚无固定实施卡和 `TaskMaintenanceTurnContractTest.java`；当前 `TaskMaintenanceService.java` 为 1130 行、SHA-256=`39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`。最新 readiness 证明其五项显式启动依赖中只剩 TURN-22 未满足。
6. 当前 Cloud `AutoBattleTask.java` 为 294 行、SHA-256=`E13BFFF740570B9C7B833F7EDCE336BFFE39FB89E410B630FF2156B69410264A`，仍是共享 untracked source；`AutoBattleTaskTurnContractTest.java` 与 TURN-34C 固定实施卡均不存在。

## 证据 2 - 精确 startDependsOn

权威计划 `:1145` 固定：

```text
startDependsOn = TURN-19 + TURN-21 + TURN-22 + TURN-23 + TURN-34A + TURN-34B
```

| 直接依赖 | 当前真实 source 证据 | 对 34C 的含义 |
|---|---|---|
| TURN-19 | 父级 production/test-source `P0/P1/P2=0/0/0`；build cohort 待稳定 writer | LeftTop public contract 已可只读消费 |
| TURN-21 | Repair #1 父级 `0/0/0`；build cohort 待稳定 writer | CommonBox exact identity、30s owner 及一次 typed click contract 已可只读消费 |
| TURN-22 | Parent Review #4=`0/2/1`；Repair #3 等 TURN-28P Repair #2 final frozen queue API | TeamReturn contract 仍不能由 34C 猜测或在 Task 层补延时 |
| TURN-23 | Repair #1 父级 `0/0/0`；build cohort 待稳定 writer | startup first-aid/incense public contract 已可只读消费 |
| TURN-34A | production 已写；唯一 named test 尚未创建，External C 按 `06:50:15` 父级裁定继续原 owner | 34C 只能读取当前四个 consumer API，不能把活动 source 当作最终交付 |
| TURN-34B | 无固定实施卡、无 named test、无 source delivery；它自身等待 TURN-22 | 34C 不得提前猜 `TaskMaintenanceService` 最终 state/terminal/API 形状 |

当前两条汇合链为：

```text
TURN-28P Repair #2
  -> TURN-22 Repair #3 source/test-source gate
  -> TURN-34B fixed card + source/test-source gate
  -> TURN-34C

TURN-34A named test source delivery + parent source/test-source gate
  -> TURN-34C
```

因此现时可冻结 dependency graph 和验收合同，但六项直接 source 启动条件尚未同时成立。

## 证据 3 - 精确 approvalDependsOn

`approvalDependsOn` 与 source 启动条件分离，当前可冻结为：

```text
approvalDependsOn =
  TURN-34C parent production/test-source review P0/P1/P2=0/0/0
  + two independent non-implementation reviews P0/P1/P2=0/0/0
  + C(AutoBattleTaskTurnContractTest) fresh exit 0
  + HTTPS_TURN_CONTRACT_TEST_FAMILY 中实际调用链相关的 T01/T02/T03/T04 门
  + direct dependency 各自未完成的 named-test/build 证据
  + applicable Cloud compile/build gate
```

证据映射：

1. 权威计划 `:1463-1476` 要求本卡 production/test 同 owner 交付，required test fresh exit 0、父级断言审查与适用 compile 均不能由聊天或 source scan 替代。
2. 计划 `:1602-1605` 要求 TURN-13G..40D 满足实际调用链相关的 T01-T04。34C 的链穿过 action/outcome protocol、Cloud exchange/raw PNG/template、DHXY HTTP/capture/input/local executor，以及 PlayerState/maintenance 下游的 Bag/UI/Give local Service，所以四个 foundation family 均有真实关联；Quest adapter 只有在未来最终 34B source 出现真实 Quest caller 时才增加该行关联，当前源码没有该证据。
3. 本卡唯一 named test 是计划 `:1641` 的 `task/AutoBattleTaskTurnContractTest`，profiles=`BC4+BASE+TASK`。未来精确命令为 `mvn -q -Dtest=AutoBattleTaskTurnContractTest test`，本 helper 未运行。
4. Cloud 非测试 source gate 是计划 `:1439-1443` 的 `mvn -q clean compile`；最终 `clean package` 会运行测试，须另有该次用户授权。本 helper 不把未运行门写成通过事实。

## 证据 4 - TURN-34C 唯一 production/test write set

未来本卡唯一 production/test 写集：

1. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/AutoBattleTask.java`。
2. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/AutoBattleTaskTurnContractTest.java`。

父级未来另行创建并冻结的流程报告候选路径是：

`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34C.md`。

除上述 production/test 与未来本卡固定报告外，两仓全部只读。特别不得修改 `AutoCombatService`、`TaskMaintenanceService`、PlayerState/TeamReturn/CommonBox/LeftTop/startup 类、`BaseTaskTemplate`、context/holder/checkpoint、protocol/client/action factory、host/factory/runtime、POM/config/resources、DHXY production 或依赖测试。

当前 `AutoBattleTask.java` 是共享 untracked 文件；未来只能在父级冻结 SHA 后增量编辑，禁止用 `git show`、baseline copy 或整文件替换覆盖现有 exact-context 适配。

## 证据 5 - 真实 caller 与激活边界

1. 当前 Cloud production 对 `AutoBattleTask` 的引用只有类自身；没有 factory、registry、runtime 或 host caller。现有两个测试引用只是 `CommonBoxTurnContractTest:553` 与 `SummonSkillTurnContractTest:933` 的 source boundary scan，不是运行 caller。
2. 当前真实可运行 caller 仍在 DHXY：`DefaultTaskFactory.java:19,25,35-45` 由 `AUTO_BATTLE` 取得 `AutoBattleTask`，`WindowTaskRunner.java:608` 创建 task，`:756-771` 绑定执行上下文并在 `:766` 调用 production `task.execute(executionContext)`。
3. Cloud `@Component`/prototype annotation 只声明 bean shape，不等于实际构造或启动。
4. 权威计划 `:1355-1362` 把 Cloud real Task factory/registry/runtime 固定给 TURN-40B；40C 才接 host/HTTP activation。因此 34C named test 必须直接走 production public `execute(...)`，但不能把单测、bean annotation 或 source existence 写成 Cloud runtime reachability。

## 证据 6 - 696a12b0 等价验收

基线对象为 DHXY commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `AutoBattleTask.java`，Git blob=`18bcd33322c5b1037087f40ba136b1bc9146dda9`。当前 Cloud source 的业务体与该 blob 逐段核对结果：

| 基线/当前位置 | 必须保持的业务合同 |
|---|---|
| baseline `:124-139` / current `:122-137` | startup gate -> RUNNING -> startup first aid -> maintenance init(`auto-battle`) -> AutoCombat init，次数各一 |
| baseline `:141-151` / current `:139-149` | 每轮 checkpoint -> 一次 combat tick；非 `NONE` 不进 idle；仅 FREE+`NONE` 进 idle；每轮只 sleep 一次 |
| baseline `:164-165` / current `:162-163` | `handleCombatTick(context,"auto-battle",false)` 参数、返回原样 |
| baseline `:184-235` / current `:182-233` | local return release -> pending leader defer -> standalone return -> follower/left-top -> opportunistic maintenance 的优先级不变 |
| baseline `:237-257` / current `:235-255` | local support + zero-wait `TEAM_RETURN`；`COMMON_BOX` capability/consume 在前；无论 box true/false 都随后调用 TeamReturn；最后 `consumedBox || clickedReturn` |
| baseline `:265-273` / current `:263-271` | follower 仅 `MEMBER` 且 requested task 非空、不同于 `auto_battle` |
| baseline `:282-289` / current `:280-287` | polling 优先级固定为 pending first aid=`500ms`、FREE=`3000ms`、否则 AutoCombat dynamic |
| baseline `:292-294` / current `:290-292` | task retry policy 恒为 `none` |

当前差异只允许是迁移机械层：显式 context rejection、`TaskExecutionContextHolder.callWith` 全生命周期绑定与 imported `TaskStep` 简化。不得新增或恢复 CR244 return self-check、第二 probe、marker memory、owner/session/ledger、TTL、cleanup、park/yield、补偿 command 或自动 retry。

`docs/业务逻辑.md:1-67` 固定同批本地 team session/capability 隔离；`:69-168` 固定 CommonBox 最高维护优先级及其 owner 自管 30s pending；`:170-217` 固定 Summon static-tail 业务并禁止迁移自行加 TTL/二次验证/retry。34C 只消费最终 34A/34B public contract，不复制这些 owner。

无已批准业务差异；按 `696a12b0` 等价迁移。

## 证据 7 - 34A/34B 当前 consumer API 快照

34C 当前对 34A production 的四个真实调用：

| AutoBattle caller | 当前 34A API |
|---|---|
| `AutoBattleTask:137` | `AutoCombatService.initializeForCurrentWindow()` at `:82` |
| `AutoBattleTask:163` | `handleCombatTick(context,"auto-battle",false)` at `:107/:126` |
| `AutoBattleTask:281` | `hasPendingFollowerFirstAidForCurrentWindow()` at `:320` |
| `AutoBattleTask:287` | `getDynamicPollingIntervalMs()` at `:236` |

34C 当前对未来 34B production 的六个真实调用：

| AutoBattle caller | 当前 TaskMaintenance API |
|---|---|
| `:136` | `initializeForTaskStart(context,"auto-battle")` at `:67` |
| `:187` | `isPendingLocalSupportLeaderDetection(context)` at `:416` |
| `:193/:198/:236` | `isLocalSupportMemberSession(context)` at `:321` |
| `:203/:245` | `isLocalTeamSupportCapabilityOpen(context, capability)` at `:468` |
| `:239` | `awaitLocalTeamSupportCapabilityOpen(context, TEAM_RETURN, 0L)` at `:285` |
| `:208-228` | `runOpportunisticMaintenance(context, exact request)` at `:578` |

其余 direct collaborator refs 当前为 `PlayerStateService.performStartupFirstAidCheck:225`、`TeamReturnService.clickReturnTeamIfPresent:52`、`CommonBoxService.consumePendingBoxIfAllowed:107`、`LeftTopStatusSwitchService.consumeFollowerSafeWindow:88` 与 `isSupportedTaskCode:127`。

34A 活动 production 已把旧 `readWindowFact(BATTLE_RADAR_*)` path 清零，但 `AutoBattleTask:36` 的 JavaDoc 仍描述该旧链；这是 34C 自有文件内的最新文档漂移证据，不授权恢复旧 fact caller，也不授权修改 34A。

## 证据 8 - 与 34A/34B/35/36/37 写集互斥

| Card | 唯一 production | 唯一 named test | 当前物理状态 | 与 34C 交集 |
|---|---|---|---|---|
| TURN-34A | `service/AutoCombatService.java` | `service/AutoCombatServiceTurnContractTest.java` | production active SHA=`532E...`；test 尚无 | `0` 文件；34C 只消费四 public API |
| TURN-34B | `service/TaskMaintenanceService.java` | `service/TaskMaintenanceTurnContractTest.java` | 未建固定卡/test | `0` 文件；34C 等最终六 consumer API |
| TURN-34C | `task/AutoBattleTask.java` | `task/AutoBattleTaskTurnContractTest.java` | source untracked；test/card 尚无 | 本卡独占 |
| TURN-35 | `task/wubei/WubeiTask.java` | `task/wubei/WubeiWholeTaskTurnContractTest.java` | source SHA=`DFDE0AD0...`；test 尚无 | `0` 文件 |
| TURN-36 | `task/wuhuan/FiveRingTaskV2.java` | `task/wuhuan/FiveRingWholeTaskTurnContractTest.java` | source SHA=`287FF0EB...`；test 尚无 | `0` 文件 |
| TURN-37 | `task/xiuluo/XiuluoTaskV2.java` | `task/xiuluo/XiuluoWholeTaskTurnContractTest.java` | source SHA=`46F96659...`；test 尚无 | `0` 文件 |

文件级证明：六张卡 production path 两两不同，六个 named-test path 两两不同；没有共享可写 DTO、fixture、POM 或 report。逻辑上 34C/35/37 均消费最终 34A+34B，36 消费最终 34A；共享 Java API 是依赖，不是共享文件 owner。

计划 `:1428-1431` 仍把 34C 放在 Caller Wave R4、等待 34A+34B；35/36/37 属 Whole Task Wave R5，三文件可彼此并行。即使物理写集互斥，也必须分别满足注册表 DAG 与父级波次调度，不得把“无碰撞”解释成当前可领取。TURN-38A 最终同时等待 `34C+35+36+37` 汇合。

## 证据 9 - exact-context 当前真实边界

1. `AutoBattleTask:111-113` 先拒绝 null，再用 production `TaskExecutionContextHolder.callWith` 绑定整个 lifecycle；holder `:19-30` 会在 normal/throw 两条路径恢复先前 sentinel。这是 34C 可冻结的 exact-reference contract。
2. 当前 task 自身直接读取的 `getLogPrefix/getRequestedTaskCode/getWindowRole/getLocalTeamSessionKey/throwIfStopRequested` 均有 turn-native 路径；但 `AutoBattleTask:120` 调用 inherited `BaseTaskTemplate.logWindowContext`。
3. `BaseTaskTemplate:185-196` 在日志中读取 `getRunRevision/getStopEpoch`；`TaskExecutionContext:205-226,442-451` 明确把 `getScope/getPlayerIdentityEpoch/getStopEpoch/getRunRevision` 设为 old-authority-only，turn-native 调用会 fail-closed。
4. startup gate 还有第二层同类边界：`CloudStartupGateAuthority:125-127,162-164,239-262` 读取 `getScope/getPlayerIdentityEpoch/getStopEpoch/getRunRevision`；`TaskStartupCheckService:51-85` 会在 gate evaluation 中真实经过它。
5. 权威计划 `:1302-1312` 把 `TaskExecutionContext/BaseTaskTemplate` 的 old-authority removal 交给后续 TURN-38A；`:1319-1320` 把 startup authority/service rewire 交给 TURN-38B3。注册表又固定 `38A` 等 `34C+35+36+37`，`38B3` 等 `38A`。因此 34C 不能把 38A/38B3 加成自己的直接依赖，也不能在本卡越权修改这些文件形成 DAG 环。
6. 计划 `:1320` 写的是 `com/bot/dhxy/service/TaskStartupCheckService.java`，真实源码位于 `com/bot/dhxy/task/startup/TaskStartupCheckService.java`。该路径差异须由父级在未来 38B3 固定卡中校正；34C helper/worker不得猜测搬包。

由此，34C named test 可冻结“supplied exact context reference、holder sentinel、missing-context fail-closed、legacy-compatible production orchestration”；现阶段不能宣称 `TaskExecutionContext.turnNative(...)` 已能正向穿过 startup gate。若未来 34C 固定卡要求该正向用例在 38A/38B3 之前通过，父级必须先明确调整 test/dependency 所有权；worker 不得用 fallback context、反射绕 gate、null evaluation 或扩大写集伪造通过。

## 证据 10 - future named-test matrix

唯一类：`AutoBattleTaskTurnContractTest`。必须直接实例化 production `AutoBattleTask`，从 public `execute(...)`/`stop()` 进入；可在同一测试文件内使用 scripted ports、合法 test subclass 和 deterministic protected sleep seam，但不得直接调用 private helper、只反射常量、source-only 冒充 production 行为，或启动 Spring/host/runtime。

| future test method | production 路径 | 精确断言 |
|---|---|---|
| `rejectsMissingContextBeforeAnyCollaborator` | `execute()`、`execute(null)` | 在 startup collaborator/action/UUID 前失败；不造 default/epoch-zero context |
| `bindsSuppliedContextForWholeLifecycleAndRestoresPriorSentinel` | public `execute(context)` -> holder `callWith` | 每个 collaborator 看到同一对象引用；normal return 后恢复原 sentinel |
| `restoresPriorSentinelWhenTerminalEscapes` | 任一 collaborator 抛 terminal | 异常原样上抛；holder 恢复；后续 collaborator/action/UUID=`0` |
| `returnsStartupSkipWithoutStartupSideEffects` | startup gate skip branch | 返回 gate 给出的 exact task result；RUNNING/first-aid/maintenance/AutoCombat/tick 全为 `0` |
| `runsStartupInBaselineOrderBeforeFirstTick` | startup allow branch | gate -> RUNNING -> first-aid -> maintenance init -> AutoCombat init -> checkpoint/tick，各一次且顺序 exact |
| `nonNoneCombatTickSkipsIdleMaintenance` | `IN_COMBAT`、`EXIT_RECOVERED` | 每个 case 一 tick/一 poll；TeamReturn/CommonBox/left-top/maintenance=`0` |
| `freeNoneTickRunsOneIdleChain` | `NONE` + FREE | 只跑一次基线 idle chain；非 FREE 时整条 idle chain=`0` |
| `localReturnChecksBoxThenAlwaysAttemptsReturn` | local session + TEAM_RETURN open | COMMON_BOX gate/consume 在前；即使 consume=true 也仍调用 TeamReturn；最终 OR 只短路后续 idle，不短路 return call |
| `closedTeamReturnCapabilityDoesNotProbeOrWait` | local session + TEAM_RETURN closed | timeout 参数严格 `0L`；CommonBox/TeamReturn release=`0`；不新增 probe/wait/retry |
| `pendingLeaderDefersRemainingIdleWork` | failed release -> pending leader | standalone return、left-top、opportunistic maintenance=`0` |
| `separatesStandaloneAndFollowerSupportPaths` | standalone/local follower/legacy follower | local member 不走 standalone；follower truth table仅 MEMBER+different requested task |
| `buildsExactMaintenanceRequestForThreeModes` | `runOpportunisticMaintenance` | source/broadcast/fullFallback/cleanSummon/onePerRound/teamKey/openWindow/requiredCapability 全字段按 `:209-228` exact |
| `pollingPriorityIsPendingThenFreeThenDynamic` | protected deterministic sleep seam | `500` 优先于 `3000`，其后才 dynamic；不求和、不 backoff |
| `stopResetsStateAndCheckpointTerminalDoesNotRetry` | public `stop()` 与 loop checkpoint | stop 写 IDLE+FREE；confirmed stop/failed/uncertain 不转 false/success，不发 compensation，不重试 |
| `ownsNoActionUuidOrActivationAuthority` | production task source + real public flow | task 层 `UUID/TurnGameClient/action factory/command port/input/capture/OCR` direct refs=`0`；无 factory/host/runtime claim |

当前 source 静态核验为：`UUID=0`、`TurnGameClient=0`、`CloudTurnActionFactory=0`、`CloudTurnCommandPort=0`、`TurnAction=0`、`InputProvider=0`、`InputSequences=0`、direct capture/OCR=`0`。每个 downstream public call 在适用分支至多一次；真实 JSON/PNG bytes 与每次 action 的 fresh UUID 由 TURN-19/21/22/23/34A/34B 各自 production named test 负责，34C test 只验证 task 不重复调用、不吞 terminal、不自行造 action。

HTTPS turn 协议 `:108-121,151-157,294-308,335-368` 固定：network uncertainty 只重送同 outcome，不重执行 actionId；失败后后续 step 停止；Cloud 明确的新业务 retry 才产生新 payload/actionId；无本地 business retry/session/ledger。34C 不得把 lower typed terminal 映射成 false 或再调用一次 service。

## 证据 11 - 当前可冻结项与必须等待项

当前可冻结：

1. 六项 `startDependsOn`、两条汇合链、approval/test/build 公式。
2. 34C 两文件唯一 production/test write set，以及与 34A/34B/35/36/37 的 `0` 文件交集。
3. `696a12b0` startup/tick/idle/team/CommonBox/left-top/maintenance/poll/stop/retry 顺序和次数。
4. 当前 34A 四个、34B 六个 consumer API shape，作为 dispatch 前必须重读的 snapshot，不作为未来签名猜测许可。
5. production public API named-test matrix、terminal/UUID/exact-reference/sentinel 断言，以及 Cloud runtime 归 TURN-40B 的零激活声明。

必须等待：

1. TURN-34A 原 owner 创建唯一 named test、追加 source+test delivery，并取得父级 production/test-source gate；本轮生产 SHA 仍属活动 snapshot。
2. TURN-28P Repair #2 -> TURN-22 Repair #3 -> TURN-34B fixed card/delivery/source gate 的完整链。
3. 34B 最终 public API、state identity、terminal/Summon/team coordination contract 落盘后，父级再次逐项核对 AutoBattle 六个 consumer call。
4. 父级明确 34C 与后续 38A/38B3 的 exact-context 测试分层；在此之前只冻结 exact supplied-reference/legacy-compatible orchestration，不宣称 turn-native startup 正向可运行。
5. writer 稳定后的唯一 named test、相关 T01-T04、direct dependency tests 与 Cloud compile/build；本 helper 不执行这些门。
6. TURN-40B/40C 的真实 Cloud caller/activation，以及 TURN-41 用户 fresh runtime；34C 不提前覆盖。

## 证据 12 - stop-work 条件

未来 34C dispatch/worker 遇到任一项必须停止写源码并回父级重新冻结：

1. 六项直接 source gate 尚未全部有最新父级证据，或 TURN-34C 固定卡/真实 lane claim 仍不存在。
2. `AutoBattleTask.java`、34A/34B source/API、TURN-22/28P/34A/34B 物理尾部在领取前发生漂移而未重读 SHA/调用链。
3. 需要第二个 production 文件、第二个 test、共享 fixture、POM/config/resource，或要求修改 34A/34B/35/36/37 文件。
4. 需要修改 `BaseTaskTemplate`、Task context/holder/checkpoint、startup authority/service、protocol/client/action factory、host/factory/runtime 来让 34C test 看似可运行。
5. 要求在 38A/38B3 之前声称 turn-native startup 正向通过，却没有父级重新冻结 DAG/test ownership。
6. 拟恢复 CR244 self-check、增加 owner/session/ledger/TTL、第二 observation/probe、cleanup、park/yield、task/transport auto retry 或 compensation action。
7. named test 不从 production public API 进入，mock 掉本卡目标边界，调用 private helper，或复制 lower JSON/PNG mapper 来代替真实 collaborator contract。
8. terminal/uncertain 被压成 false/`NONE`/success，失败后仍调用后续 collaborator，或同一业务调用生成/发送第二 actionId。
9. 需要启动 Spring/host/runtime/Task、真实 sleep/input/capture/OCR/UI，或清理、覆盖、回滚、暂存、提交任何 dirty/untracked 文件。

## PRECHECK_COMPLETE

- deliverable：TURN-34C post-34A/34B latest dependency、collision、baseline、caller、exact-context 与 named-test delta 已写入本报告。
- cardDecision：`none；仅供父级后续冻结与调度`。
- filesWritten：仅本报告。
- testsRuntimeGitMutation：`none`。

PRECHECK_COMPLETE true EOF
