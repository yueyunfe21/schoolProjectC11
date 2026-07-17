# CR271 TURN-35 latest dependency readiness/collision delta R1

## PRECHECK 角色、边界与快照

- 角色：CR271 非实现 readiness/collision helper；不是 implementation Worker、reviewer 或父级 final reviewer。
- snapshotAt：`2026-07-16T06:50:16.868-04:00`。
- 唯一写入：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-35-latest-dependency-readiness-helper-r1.md`。
- 本报告只记录 PRECHECK 与源码/卡片证据，不改变任何卡片状态，不创建 TURN-35 固定卡，不产生 owner、claim、派工或计数进度事实。
- 本轮完整读取并交叉核对：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md` 五倍适用基线、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、既有 TURN-35 readiness 报告、当前 `WubeiTask` 及其 phase/context/wait 支撑类型、TURN-22/27/28/31/34A/34B 当前卡片与最新 readiness 材料，以及相关 turn/local-Service production API。
- 只读仓库锚点：DHXY branch=`thin-client-design`、HEAD=`0114604e1ff5f15491d2910959c45252e893d04f`；Cloud branch=`navigation-migration`、HEAD=`3b988caa010254973e03342272e6d1d6a9685b01`。两仓全部 dirty/untracked 原样保护。
- 未回滚、覆盖、清理、删除、暂存、提交或执行其它 Git mutation；未运行 Maven、JUnit、compile/package、runtime/application/server、Task/UI、capture/input，也未发送真实 turn command。

## PRECHECK 证据 1 - 本轮相对既有 TURN-35 readiness 的 delta

1. 权威计划与 `ACTIVE_WORK` 在取证中更新到 `2026-07-16 06:42:21`；本报告重新读取更新后的第 14-19 节，不沿用 06:29 旧快照。
2. TURN-33 Repair #3 的 Parent Review #5 与两份独立 delivery review 均已形成 `P0/P1/P2=0/0/0` 源码/测试源码证据；该变化解除 TURN-34A/34B 的 TURN-33 source 前置项，但不替代各卡自己的 named-test/build 门。
3. TURN-34A 已由 External C 在固定卡物理末尾真实领取。`AutoCombatService.java` 已从领取 SHA `80380B8D...` 变化为当前 SHA-256 `532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9`、mtime `06:29:17`；`AutoCombatServiceTurnContractTest.java` 仍不存在，所以 34A 仍是活动实施写集，不是稳定 predecessor surface。
4. TURN-34B 新增 06:38 的 post-34A readiness。该报告再次证明它的五项显式 source 前置中只有 TURN-22 尚未满足；`TaskMaintenanceService.java` 仍为 SHA-256 `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`，其 named test 与固定卡均不存在。
5. TURN-28P Repair #2 仍由 External B 实施。其活动测试 `InputActionFrozenExclusiveContractTest.java` 在本轮又于 `06:46:19` 变化为 SHA-256 `265FB5F25FA9ED0960DE4BC04D05B8EABB3F0C719CA697DF190852BF271FA2DB`；固定卡尚无 Repair #2 delivery/父级新 source gate 记录。这是 final frozen queue API 仍在变化的直接证据。
6. TURN-22 固定卡物理末尾仍是 Parent Review #4 的 `0/2/1` 返修要求；Repair #3 必须消费 TURN-28P Repair #2 的最终 API。权威计划第 17.2 节仍写“等待 Repair #1 API”，与当前注册表、`ACTIVE_WORK` 和 TURN-28P 固定卡的 Repair #2 活动事实存在时序漂移，不能据该旧字样猜最终合同。
7. TURN-27 与 TURN-28 仍只有 readiness/preflight 报告，无固定实施卡、无真实领取、无各自 named test。TURN-28 的 05:11 launch preflight 早于当前 TURN-28P Repair #2，不能把其中候选 API 当成最终冻结 API。
8. TURN-31 的 owner 已释放。当前 `WubeiTask.java` SHA-256 仍为 `DFDE0AD08900F2553088A7D304556A2B5A754C4980305199DB7B9C9035B720D7`、mtime `2026-07-15 22:54:45`，与 TURN-31 交付值一致；相对 `696a12b0` 的业务文件差异仍只是在 post-accept tracker async caller 中绑定 exact `TaskExecutionContext`，没有第二次 read/retry。
9. TURN-35 的 future named test 与固定实施卡当前均不存在；目标 production 文件也没有 TURN-35 owner/claim。

## PRECHECK 证据 2 - TURN-35 精确 source gates

权威计划 `:1151` 冻结：

`
startDependsOn =
  TURN-13C + TURN-14 + TURN-15 + TURN-21 + TURN-22 + TURN-23
  + TURN-26 + TURN-27 + TURN-28 + TURN-31 + TURN-34A + TURN-34B
`

| Dependency | 当前 parent source/test-source 事实 | TURN-35 source start 含义 |
|---|---|---|
| TURN-13C | exact-context/holder/checkpoint/provider bridge 源码与测试源码审查通过；named test/Cloud compile 尚待稳定 writer cohort | source 项已满足 |
| TURN-14 | Bag typed prescan/use-cached source 与测试源码审查通过；build 尚待 | source 项已满足，但见本报告“Bag uncached route”接口缺口 |
| TURN-15 | UI cleaner typed local-Service source与测试源码审查通过；build 尚待 | source 项已满足 |
| TURN-21 | CommonBox exact-context source与测试源码审查通过；build 尚待 | source 项已满足 |
| TURN-22 | Parent Review #4=`0/2/1`；Repair #3 未实施，且传递等待 TURN-28P Repair #2 final API | source 项未满足 |
| TURN-23 | PlayerState/first-aid/incense source与测试源码审查通过；build 尚待 | source 项已满足 |
| TURN-26 | Dialog option/OCR/white-story source与测试源码审查通过；named test/build 尚待 | source 项已满足 |
| TURN-27 | 仅完成 preflight；等待 TURN-28 final API 与父级 scope freeze，无固定卡/test/claim | source 项未满足 |
| TURN-28 | 等待 TURN-28P Repair #2；无固定卡/test/claim | source 项未满足 |
| TURN-31 | Wubei TaskTracker caller source与测试源码审查通过；当前交付 SHA 稳定，build 尚待 | source 项已满足 |
| TURN-34A | External C 正在写 production；named test 尚未创建，也无 delivery/source review | source 项未满足 |
| TURN-34B | 只有最新 PRECHECK；等待 TURN-22 Repair #3，无固定卡/test/claim | source 项未满足 |

传递 source 顺序必须按当前真实材料读取：

`
TURN-28P Repair #2 delivery + parent source/test-source gate
  -> TURN-22 Repair #3 delivery + parent source/test-source gate
  -> TURN-28 final fixed API/card/delivery
  -> TURN-27 final fixed API/card/delivery

TURN-34A delivery + parent source/test-source gate
TURN-22 Repair #3 source gate -> TURN-34B fixed card/delivery/source gate

上述全部完成
  -> 父级重新核对 Wubei SHA、API、写集互斥和 Bag route
  -> 父级才可创建 TURN-35 固定卡并指定唯一 implementation lane
`

第 19 节的完成门与 source start 分离：TURN-35 自身 source 可以只按 `startDependsOn` 判断，但卡片最终验收前必须满足 `TURN-T01/T02/T03/T04` 四张 Foundation 补债门，不能用 Whole Task 单测替代。

## PRECHECK 证据 3 - 当前真实 caller 与迁移边界

### 3.1 唯一 Whole Task public entry

- 当前 production public entry 是 `WubeiTask.execute(TaskExecutionContext)`，源码 `WubeiTask.java:346-431`。
- 外层按配置轮数执行；每轮 `runRoundPhases(context, roundContext)`，`STOPPED` 直接返回、非 success 映射失败，见 `:373-428`。
- phase transaction loop 位于 `:504-592`；每个 phase 前执行 `TaskCheckpoint`，transaction/outcome 的 stop 均映射 `TaskRunResult.STOPPED`。
- `TaskExecutionContext.getTurnGameClient()` 在 turn-native 路径真实返回 exact-context client，见 `TaskExecutionContext.java:246-251`；TURN-35 不需要为此发明第二 client/facade。

### 3.2 对直接 predecessor 的真实 production caller

| Predecessor surface | 当前 Wubei caller |
|---|---|
| TURN-27 `NavigationService` | `navigateToNPC` at `WubeiTask:1407,1849`；`navigateInCurrentMap` at `:1588,2115` |
| TURN-28 `NpcClickService` | pending confirm `:733,2017`；`clickNpcSmart` `:1427,1960,3399`；direct combat `:3440` |
| TURN-31 tracker caller | schedule at `:2025`；exact context async read `:2043-2071` |
| TURN-34A `AutoCombatService` | init/wake/tick/recovery/trusted-state calls at `:351,788,918,2777,3447,3595,3624,3756,4164-4168` |
| TURN-34B `TaskMaintenanceService` | task/round setup `:366,386`；window open/close and opportunistic work `:600,771,1119,1128,1204,1433,1710,1763,1793-1818,2765,3958` |

TURN-35 只能消费这些 predecessor 最终 public APIs；不得在 Wubei 内复制 Navigation/NPC/AutoCombat/Maintenance 的 OCR、queue、state、retry 或 result projection。

### 3.3 当前尚未 turn 化的真实路径

- 旧/local authority imports/fields 仍存在：`InputSequences` `:7,278`、`BagService` `:45,266`、`TaskTurnCoordinator` `:65,280`、`WindowTaskContextHolder` `:83,284`；当前文件没有 `TurnGameClient` 引用。
- 直接 input 仍存在：Alt+C `:2093-2096`、prepared dialog click `:2206-2211`、tracker green click `:2736-2740`、黄袍 cached click `:4233-4237`。
- 直接 Bag mechanics 仍存在：显形镜 `bagService.findAndUseItemFromBack(...)` at `:2636`；回程 uncached fallback `bagService.findAndUseMainBagTaskPageItem(...)` at `:3902`。
- 当前 ordinary `PATHING_TERMINAL` 仍会立即重按同一 tracker 绿链，见 `:1675-1695`；该行为已被 `docs/业务逻辑.md:283-341` 的 current-attempt Cloud fallback 合同取代。
- 当前黄袍仍有 `MAX_CHAINED_COMBAT_ATTEMPTS=5`，见 `:169,3983-3989`；该固定场次数已被 `docs/业务逻辑.md:976-978` 明确取代。
- 当前 `PROBE_ENTER_BATTLE_TIMEOUT_MS=300000` 与若干 inner wait 仍在，见 `:173`；获批的 180 秒全局 pre-battle budget 必须在所有 inner wait 之上抢占，见 `docs/业务逻辑.md:845-880`。

这些是 TURN-35 future old-code-failure seams，不是可冻结为最终业务合同的现状。

## PRECHECK 证据 4 - 唯一 coverage identity 与精确写集

### 4.1 唯一 coverage identity

- 历史 Whole Task 报告已固定唯一 `countUnit=WubeiTask::execute(TaskExecutionContext)`。
- 权威计划 `:1035-1043` 已废止用 `countDelta` 表示发卡/进度；未来注册应把同一字符串只记录为 `legacyCoverageKey`，用于防重复与查漏。
- TURN-31 已独立覆盖 `WubeiTask::taskTrackerCaller`。TURN-35 不得把该 tracker caller 再计一次，也不得拆出第二个 Whole Task count unit。
- 本 PRECHECK 不分配该 identity，不写 `countDelta`，不产生 owner/claim。

### 4.2 唯一 production/test write set

未来 TURN-35 固定卡只能包含：

1. Modify production only:
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
2. Create named test only:
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`

必要 DTO 只能是 `WubeiTask.java` 内的 `private nested type`。`WubeiPhase`、`WubeiRoundContext`、`WubeiStepOutcome`、`WubeiWaitSpec`、所有 Service、turn protocol/client/context、POM/config/resource 与 DHXY 全仓均只读。

未来 process artifact 只能由父级创建、实施者 append 的独立固定卡：
`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-35.md`。
该文件当前不存在，本 helper 不创建。

## PRECHECK 证据 5 - 与 TURN-34A/34B/External lanes 的互斥

| Lane/card | 当前/未来真实写集 | 与 TURN-35 物理重叠 | 必须保留的语义门 |
|---|---|---|---|
| External B / TURN-28P Repair #2 | DHXY 6 production + 3 tests；Cloud 2 tests；原卡 append | 无 | API 仍在活动变化；先完成 28P，再释放 22/28/27 |
| External A / TURN-22 Repair #3 | Cloud `TeamReturnTurnContractTest`；DHXY `TurnInputStepExecutor` 与其 contract test；原卡 append | 无 | 等 final 28P queue API；不得由 TURN-35 猜接口 |
| External C / TURN-34A | Cloud `AutoCombatService.java` + `AutoCombatServiceTurnContractTest.java`；34A 卡 append | 无 | Wubei 只读消费 final AutoCombat public surface；34A 当前未交付 |
| External D / TURN-34B | Future Cloud `TaskMaintenanceService.java` + `TaskMaintenanceTurnContractTest.java`；34B 卡 append | 无 | 等 TURN-22；Wubei 只读消费 final Maintenance API |
| TURN-31 | `WubeiTask.java` + tracker named test；31 卡 | production 与 TURN-35 同文件 | 31 owner 已释放且当前 SHA 稳定；TURN-35 领取前再次核 hash，任何新 writer 都停止领取 |
| TURN-36/37 | 各自 Task 主文件 + 各自 Whole Task named test | 无 | 三张 Whole Task 卡可在各自 source gates 满足后文件级并行 |

34A/34B 的互斥不是只看文件名：

1. 34A 固定卡只写 `AutoCombatService`，并冻结只调用 `TaskMaintenanceService` 六个现有 API；34B 只能在自己文件内保持这六个 API 的 shape 与 caller-visible 语义。
2. TURN-35 同时是两者的真实 caller，只能等两张卡各自形成 final source surface 后再接线，不能在活动 API 上编译猜测。
3. 未来 TURN-35 实施期间，`WubeiTask.java`、`WubeiWholeTaskTurnContractTest.java` 与 TURN-35 固定卡必须只有一个 writer；任何 helper/reviewer 保持只读。
4. External A/B/C/D 的 source/test/card 路径与 TURN-35 物理互斥成立，但 B/C 当前活动写入会改变 TURN-35 的传递/直接 API 事实，因此“无文件冲突”不等于 predecessor 已稳定。

## PRECHECK 证据 6 - Bag uncached route 仍需父级冻结

- 双仓 `TurnBagOperationArguments.ReturnItemIntent` 当前只有 `PRESCAN_TASK_PAGE`、`PRESCAN_FROM_BACK`、`USE_CACHED_RETURN_ITEM`。
- Cloud `CloudBagLocalServiceClient` public surface 只有 `executeReturnItem(...)` 与 `executeUseIncense(...)`；`ReturnItemPrescanService` 只提供 prescan lifecycle 和 `useCached(...)`。
- `WubeiTask:2636` 的显形镜和 `:3902` 的 uncached 回程 fallback 都是“扫描并实际使用”的当前 baseline mechanics。仅 prescan 后再 use-cached 是否能在所有 miss/cache-invalid 分支保持相同动作数、顺序、failure/STOP 语义，现有 predecessor 卡没有冻结。
- TURN-35 的一 production 文件写集不能修改 Bag protocol、DHXY local executor、`CloudBagLocalServiceClient` 或 `ReturnItemPrescanService`，也不能静默删除 uncached fallback。
- 父级在创建 TURN-35 卡前必须二选一并写成既有卡/API 事实：由 predecessor 提供 baseline-equivalent closed Bag operation；或用现有 API 证明所有相关路径都已保证 exact cached point 且不改变 baseline。没有该证据时不得猜合同。

## PRECHECK 证据 7 - 696a12b0 与已批准规则的精确验收

验收基线是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，再叠加 `docs/业务逻辑.md` 中用户明确批准的后续替换项。当前 Cloud 源码不能在与这些替换项冲突处反向充当冻结合同。

### 7.1 14-state graph 与 terminals

`WubeiPhase.java:10-23` 的 14 个业务 state 必须全部保留且测试逐一可达：

1. `HOT_START_DETECT`
2. `ROUTE_TO_MAIN_TASK`
3. `ACCEPT_TASK`
4. `READ_TRACKER`
5. `AFTER_ACCEPT_MAINTENANCE_CHECK`
6. `BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK`
7. `TRACKER_PATHING`
8. `RESOLVE_AFTER_PATHING`
9. `ENTER_BATTLE`
10. `WAIT_BATTLE_FINISH`
11. `POST_BATTLE_RECOVER`
12. `RETURN_HOME`
13. `WAIT_TEAM_RETURN`
14. `ROUND_DONE`

`FAILED` 与 `STOPPED` 是 terminal，不计入 14 个业务 state。`ROUND_DONE -> SUCCESS`；`STOPPED -> STOPPED`；其它 terminal -> failure。不得把 collaborator miss/false、uncertain 或 negative ready-event 提升为新业务 phase 事实。

### 7.2 STOP、pause 与 action identity

1. 外层 round checkpoint、每个 phase checkpoint、每个 action 前 checkpoint 都必须传播 confirmed stop；返回 `STOPPED`，不是 failure。
2. stop/missing/mismatched exact context 在 command 前发生时，后续 UUID=`0`、command=`0`、local mechanics=`0`；stop 后不得 compensation command。
3. pause 保留同一 state/attempt/cache，等待安全点并补偿 pre-battle/maintenance/probe timer；pause 不产生业务 progress、失败或新 UUID。
4. 一次显式业务 invocation 只生成一次 fresh UUID/action/command；timeout/interrupted/uncertain 不自动重发。只有 baseline 明确的下一次业务 retry 才是下一 invocation 和新 UUID。
5. terminal/uncertain/correlation/frame 错误不得压成 false/miss/success 后继续下一 phase。

### 7.3 park/yield

1. 普通怪与黄袍第一战：tracker 绿链实际点击后才可 `MUST_YIELD`，只 park 当前 window，等待 exact Runner/provider event；普通 event wait 可以是 `timeout=-1`，但必须同时接受并优先消费 `PRE_BATTLE_TIMEOUT`。
2. 本地 template miss、静态图尚未返回、plain STORY/OPTION 或迟到 result 不唤醒、不授权 fallback。
3. 白龙马显形镜使用后至成功进入后续链或失败重接任务，全程继续持有 task turn；无 sleep、park、yield、80ms polling 或自造 timeout result。
4. 黄袍第一战第一次进入 `WAIT_BATTLE_FINISH` 后，不再进入普通怪无限 park；唯一额外放权是战后固定 `5000ms` 队员 first-aid window，随后立即推进 tracker/cache 判断与续战/回程。
5. 战斗状态 wait 可按 final AutoCombat dynamic wake 保持 `500..10000ms` maintenance wake；这不是新增业务 terminal 或 retry。

### 7.4 retry/fallback 数量与顺序

1. 通用 phase recovery：`recoveryCount=0/1/2` 可按基线 cleanup、`800ms`、回 `ROUTE_TO_MAIN_TASK`；失败 state 的 count 已为 `3` 时进入 `FAILED`。不得改成无限 retry 或 transport retry。
2. phase loop guard 保持超过 `32` 步走既有 recovery，不直接伪 success。
3. maintenance hook：单 hook 最多 `5` 次；连续失败阈值 `3`，失败投影与主线继续/告警顺序保持。
4. tracker click 上限 `12`、anchor recovery `5`、每条 probe 显形镜使用上限 `2`、回程验证 `2` 次且间隔 `500ms`，不得自行增减。
5. 普通怪 fallback 只由当前 `attemptId` 的 Cloud static miss 或 Cloud click failure 授权；本地 Runner miss、停稳、`PATHING_TERMINAL`、旧 attempt result 均无权授权。只有实际执行的 fallback 绿链重按才计数；累计 `3` 次仍未进战，走既有 recovery。
6. 白龙马四结果保持 exact：`targetReady` 进入 smart target click 后才允许已批准 direct-combat fallback；`wrongPosition` 回同 probe 绿链；`storyAbsent` 同 probe 只重用一次显形镜，第二次 absent 失败重接；`noTarget` 只推进第二 probe。absent/noTarget 均无 tooltip fallback。
7. 黄袍第一次战后允许一次 full tracker read 并建立 fast cache；后续只读 fast ROI。fast miss/cache mismatch 直接结束链，不再 full reread；黄袍没有固定战斗场次数上限。
8. 回程顺序保持 cached return item -> 获批的 uncached fallback -> 起始地图验证；失败先做 trusted combat correction，仍在战斗则回 `WAIT_BATTLE_FINISH`，不得直接重接或宣告成功。
9. 热启动总顺序固定为 `IN_COMBAT > 当前任务 dialog > team return > current tracker green > return item > saved context > ROUTE_TO_MAIN_TASK`；五倍当前没有独立已批准 saved context，不得用旧 OCR/坐标/过期缓存补位。

### 7.5 expiry/freshness

1. pre-battle budget 固定 `180000ms`，正常轮从第一次接任务 option 成功起算；热启动未经过接任务时以第一条 tracker 绿链成功点击兜底起算；暗雷 reroll 不重置。
2. budget 覆盖 maintenance、tracker read、绿链点击、navigation、`ENTER_BATTLE`、probe 和所有 inner wait/retry；优先级高于 `300000ms` probe wait、`timeout=-1` event park 与 prepared-dialog wait。
3. pause 阻塞时长补回 budget；Runner 首次确认 `IN_COMBAT` 或 `EXIT_RECOVERED` 才结束 budget。战斗时长不计入该 budget；现有单次 `WAIT_BATTLE_FINISH=180000ms` 仍按基线单独验收。
4. `WUBEI_ENTER_BATTLE` / probe dialog interest 没有 `15s` 业务 TTL；黄袍续战在点击绿链前注册 interest，不延迟注册。
5. 已验证回城 `起始地图+坐标` 是任务事实，没有 TTL；队伍等待、排队、retry、pause 或日志耗时均不清除，只在下一轮接任务 option 实际点击成功后清除，且不得新增第二次位置验证。
6. 既有 prepared action/event freshness guard 保持其原用途和数值：route click `2500ms`、enter-battle block `5000ms`、prepared dialog `3000ms`、ready-event priority `3000ms`。它们不是 dialog-interest 或 verified-return 的业务 TTL，不得混用。
7. 不新增 session、owner、lease、ledger、durable workflow、compaction、background retry、第二 observation、第二 verification 或任何未批准 TTL。

## PRECHECK 证据 8 - 未来 named-test matrix

唯一测试类：
`com.yueyunfe.dhxy.cloudbrain.task.wubei.WubeiWholeTaskTurnContractTest`，
profiles=`HTTPS_TURN_CONTRACT_TEST_FAMILY / BC4+BASE+TASK+IMG+LS`。

每个行为用例必须直接构造 production `WubeiTask`，绑定 production `TaskExecutionContextHolder.callWith(...)` 的 turn-native exact context，并从 public `execute(TaskExecutionContext)` 驱动；可以使用 test-private scripted collaborators，但不得复制 phase reducer、只反射 private helper/常量、只扫源码或绕过目标 public boundary。静态零引用断言只能是补充证据。

| Future named test method | 必须穿过的 production public path | 必须锁住的证据/旧代码失败 seam |
|---|---|---|
| `executeTraversesExactFourteenStateGraphAndMapsTerminals` | `execute(turnNativeContext)` | 14 state 全部可达，ROUND_DONE/FAILED/STOPPED 映射 exact |
| `confirmedStopAtEveryActionBoundaryCreatesNoLaterUuidOrCommand` | public execute + scripted turn port | 每个 action 前 stop 都是 STOPPED；stop 后 UUID/command/mechanics=0 |
| `pauseRetainsStateAndCompensatesAllOwnedTimers` | public execute + real checkpoint/holder | pause 不推进/失败，不重置 attempt/cache/budget，恢复后 timer 补偿 |
| `hotStartUsesExactSevenFactPriorityWithoutSyntheticSavedContext` | public execute hot-start round | 固定优先级与无五倍 saved-context；旧 OCR/cache 不得插队 |
| `turn31PostAcceptTrackerReadBindsExactContextOnceWithoutRetry` | public execute accept -> tracker | TURN-31 一次 async typed read、exact context、一次 UUID/command、异常不自动重读 |
| `ordinaryFallbackRequiresCurrentAttemptCloudAuthority` | public execute ordinary path | current attempt Cloud miss/click failure 才授权；local miss/terminal/旧 result 均零 fallback |
| `ordinaryFallbackStopsAfterThreeActualExecutions` | public execute ordinary path | 只计实际绿链重按，恰好三次后走既有 recovery；第四次 UUID/command=0 |
| `preBattleBudgetStartsOnceAtAcceptAndPreemptsEveryInnerWait` | public execute accept/probe/prepared paths | 180s 单起点、暗雷不重置、抢占 300s/infinite/inner waits；当前仅 phase-boundary seam 必须失败 |
| `preBattleBudgetEndsOnlyOnTrustedCombatFact` | public execute enter/wait path | prepared click 不结束；首个 IN_COMBAT/EXIT_RECOVERED 才结束；battle duration 排除 |
| `whiteDragonKeepsTurnAndMapsFourProviderResultsExactly` | public execute probe path | mirror 后零 park/yield/sleep；四结果 branch、无自读图 |
| `whiteDragonAbsentRetriesSameProbeOnceAndNoTargetOnlyAdvancesSecondProbe` | public execute probe path | absent 仅一次；noTarget 只推进第二 probe；两者 tooltip action=0 |
| `huangpaoFirstBattleParksButContinuationOnlyYieldsFiveSecondAidWindow` | public execute chained path | 第一战普通 park；续战无无限 park；固定 5s 后继续 |
| `huangpaoHasNoFixedFightCapAndFastMissEndsWithoutFullReread` | public execute chained path | 第六场不因场次失败；首轮 full、后续 fast-only、miss 后 full read=0；当前固定 5 seam 必须失败 |
| `returnHomeUsesCachedThenFrozenUncachedRouteAndTrustedCombatCorrection` | public execute return path | exact Bag 顺序、两次验证；still-in-combat 回 WAIT_BATTLE_FINISH |
| `verifiedReturnSnapshotHasNoTtlAndClearsOnlyAfterAcceptOption` | public execute return/team/next accept | 任意时间、queue/retry/pause 后仍复用；仅 option success 清除；第二位置 read=0 |
| `maintenanceAndAutoCombatCollaboratorsKeepExactCallerOrder` | public execute all maintenance/combat phases | 只消费 final 34A/34B public APIs，CommonBox/team/first-aid/maintenance 顺序和返回投影不变 |
| `allActionCapableBranchesUseOneUuidOneCommandAndNoTransportRetry` | public execute + recording turn port | BC4 四终态、exact correlation/frame；每 invocation 1:1，uncertain 后零重发/补偿 |
| `wholeTaskUsesOnlyTurnAndFourClosedLocalServiceBoundaries` | public execute + recording local dispatcher | Bag/UI/Give/Quest closed operations exact；production active path 零 direct input/capture/OCR/old authority |

未来稳定 writer cohort 的唯一点名命令由权威计划固定为：

`
mvn -q -Dtest=WubeiWholeTaskTurnContractTest test
`

本 helper 未运行该命令。测试源码不存在，当前 production 仍有 direct local/old authority、ordinary terminal re-click、固定黄袍上限和 Bag route 缺口，因此矩阵必须能在当前旧路径上产生真实失败，不能写成只验证新 fake 的恒真断言。

## PRECHECK 证据 9 - stop-work 条件

未来父级或实施者遇到下列任一事实，必须停止 TURN-35 source/test 写入并刷新固定卡/依赖证据：

1. TURN-22、TURN-27、TURN-28、TURN-34A 或 TURN-34B 任一 source gate 尚未形成最新 parent source/test-source 证据。
2. TURN-28P Repair #2 final queue API 尚未固定，或 TURN-22/28/27 仍引用 Repair #1/候选接口。
3. 父级尚未创建 TURN-35 固定卡，卡片物理末尾没有被指定 lane 的真实领取记录，或有人根据本 PRECHECK 虚构 owner/claim。
4. `WubeiTask.java` 不再是本报告 SHA，TURN-31 后出现未归属 drift，或另一 writer 正占用 Wubei production/test/card 任一路径。
5. TURN-34A delivery 改变 Wubei 使用的 AutoCombat public signature/result/delay/terminal 语义，或 TURN-34B delivery 改变 Wubei caller 所需 Maintenance API/窗口/优先级。
6. Bag uncached scan-and-use 无 parent-frozen baseline-equivalent API/证明，或方案需要 TURN-35 修改 protocol、local executor、Bag client/Service。
7. TURN-35 需要第二个 production 文件、第二个 named test、`WubeiPhase`/support type、caller、Service、context、turn client/protocol、POM/config/resource 或 DHXY 修改。
8. 方案把当前 `PATHING_TERMINAL` immediate re-click 或黄袍固定 5 场当成最终合同，忽略 `docs/业务逻辑.md` 的已批准替换项。
9. 方案改变 14-state graph、terminal projection、STOP/pause、phase recovery count、tracker/probe/return counts、park/yield、fallback authority/order、180s budget、5s aid window、hot-start priority 或 return snapshot lifetime。
10. 方案新增 TTL、二次验证/read、cleanup、fail-closed business truth、transport retry、compensation command、session/owner/lease/ledger/durable workflow，或把 negative Runner signal 提升为业务事实。
11. named test 不能从 production public `execute(TaskExecutionContext)` 穿过真实 Task wiring，不能在 current old-code seam 失败，或只能靠 private reflection/source string/复制 reducer 证明。
12. Foundation `T01/T02/T03/T04` 未齐时不得作最终卡片验收；但也不得把这四项混写成当前 direct source-start predecessor。
13. 完成工作需要覆盖、删除、清理、回滚、重建或暂存任一既有 dirty/untracked 文件。

## PRECHECK 当前事实

- 已满足 direct source 项：TURN-13C、14、15、21、23、26、31。
- 尚未满足 direct source 项：TURN-22、27、28、34A、34B；传递活动项是 TURN-28P Repair #2。
- TURN-35 future 唯一 coverage identity：`WubeiTask::execute(TaskExecutionContext)`；当前计划字段应为 `legacyCoverageKey`，不以 count delta 表示进度。
- TURN-35 future production/test 写集严格为 `WubeiTask.java` + `WubeiWholeTaskTurnContractTest.java`；当前 test/card 均不存在。
- 与 External A/B/C/D、TURN-34A/34B 的文件级写集互斥成立，但 predecessor API/source gate 尚未全部稳定。
- Bag uncached route 与三项明确 supersession（ordinary Cloud fallback、180s global pre-battle budget、黄袍无固定场次）必须在父级固定卡中显式冻结，不能从当前旧源码猜合同。
- 本轮写入仅本报告；Maven/runtime/input/Git mutation 均为无。

PRECHECK_COMPLETE true EOF
