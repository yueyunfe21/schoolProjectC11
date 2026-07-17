# CR271 TURN-34B post-34A latest readiness/collision delta

## PRECHECK 角色、边界与快照

- 角色：CR271 非实现 readiness/collision helper；不是 implementation Worker、reviewer 或父级 final reviewer。
- snapshotAt：`2026-07-16T06:36:17.2862227-04:00`。
- 唯一写入：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-34B-post-34A-latest-readiness-helper-r1.md`。
- 本轮完整读取并交叉核对：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、HTTPS turn 协议、`docs/业务逻辑.md`、TURN-22 原卡最新 Parent Review #4、TURN-33 原卡 Parent Review #5 与 Repair #3 独立 R1/R2 报告、TURN-34A 固定卡当前物理末尾、既有三份 TURN-34B readiness 材料，以及当前 `TaskMaintenanceService`、当前正在变化的 `AutoCombatService` 和真实 production callers。
- 只读 Git 证据：DHXY branch=`thin-client-design`、HEAD=`0114604e1ff5f15491d2910959c45252e893d04f`、status entries=`85`；Cloud branch=`navigation-migration`、HEAD=`3b988caa010254973e03342272e6d1d6a9685b01`、status entries=`28`。两仓全部 dirty/untracked 原样保护；未回滚、覆盖、清理、删除、暂存、提交或执行其它 Git mutation。
- 未运行 Maven、JUnit、compile/package、runtime/application/server、Task/UI、capture/input，也未发送真实 command。
- 本报告只冻结 PRECHECK 与证据，不改变卡片状态，不创建 34B 实施卡，不产生 34B owner、领取或派工事实。

## 证据 1 - 当前源码与卡片锚点

| Artifact | 当前事实 |
|---|---|
| Cloud `TaskMaintenanceService.java` | `1130` 行，SHA-256=`39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`，mtime=`2026-07-15 00:28:31`；本轮未变化 |
| Cloud `AutoCombatService.java` | TURN-34A 领取基线是 `836` 行、SHA-256=`80380B8D...`；当前活动 writer 已推进到 `852` 行、SHA-256=`532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9`、mtime=`2026-07-16 06:29:17` |
| `AutoCombatServiceTurnContractTest.java` | 当前不存在；属于活动 TURN-34A 的测试写集，不属于 34B |
| `TaskMaintenanceTurnContractTest.java` | 当前不存在 |
| `2026-07-16-turn-card-TURN-34B.md` | 当前不存在；本 helper 不提前创建 |

- 权威计划 `:1142-1144` 当前记录：TURN-33 Parent Review #5 与独立 R1/R2 均为 `P0/P1/P2=0/0/0`；TURN-34A 已由 External C 实际领取并处于实施中；TURN-34B 仍只有计划行。
- TURN-34A 固定卡 `:165-192` 是当前真实领取记录；其唯一 production/test 写集为 `AutoCombatService.java` 与 `AutoCombatServiceTurnContractTest.java`，并明确把 `TaskMaintenanceService` 留为只读依赖。
- TURN-22 原卡 `:446-501` 的最新 Parent Review #4 为 `P0/P1/P2=0/2/1`，要求 Repair #3 等 TURN-28P 最终 frozen queue API；其旧 source pass 已被该轮新证据覆盖。
- TURN-28P 原卡当前物理末尾仍是 External B Repair #2 实施中，尚无父级新的 source/test-source 通过记录。因此 TURN-22 Repair #3 还不能消费一个被父级最终固定的 queue API。
- TURN-33 原卡 `:585-613` 的 Parent Review #5 已给出 source/test-source `0/0/0`；`:629-645` 记录独立 R1/R2 均为 `0/0/0` 且父级分别采纳。它的 named test/build 仍是本卡自己的后续门，不重新关闭 34B 的 source 启动依赖。

## 证据 2 - 精确 startDependsOn

权威计划 `:1144` 冻结：

```text
startDependsOn = TURN-21 + TURN-22 + TURN-23 + TURN-26 + TURN-33
```

这里判断的是各前置卡的 parent source/test-source gate，不把 named-test/build cohort 冒充 source 启动门。

| Dependency | 当前 parent source/test-source 事实 | 对 34B source start 的含义 |
|---|---|---|
| TURN-21 | `P0/P1/P2=0/0/0`，后续 build 待稳定 writer cohort | 已满足该项 |
| TURN-22 | Parent Review #4=`0/2/1`；Repair #3 尚未实施，且传递等待 TURN-28P Repair #2 最终 API | **唯一尚未满足的显式项** |
| TURN-23 | `P0/P1/P2=0/0/0`，后续 named test/build 待执行 | 已满足该项 |
| TURN-26 | `P0/P1/P2=0/0/0`，后续 named test/build 待执行 | 已满足该项 |
| TURN-33 | Parent Review #5=`0/0/0`，独立 review=`2/2`；后续 named test/build 待执行 | 已满足该项 |

当前精确推进序列只能是：

```text
TURN-28P Repair #2 delivery + parent source/test-source gate
  -> TURN-22 Repair #3 delivery + parent source/test-source gate
  -> parent 重新核对 34A/34B mutex 并创建、冻结 TURN-34B 实施卡
  -> 被父级指定的 34B implementation lane 才能在该卡物理末尾真实领取
```

TURN-33 的双 reviewer delta 已关闭旧 readiness 中关于 Summon final source contract 的等待项；它不抵消 TURN-22 这一项。

## 证据 3 - 精确 approvalDependsOn

`approvalDependsOn` 与 `startDependsOn` 分离，当前可冻结为：

```text
approvalDependsOn =
  TURN-34B parent production/test-source review P0/P1/P2=0/0/0
  + two independent non-implementation reviews P0/P1/P2=0/0/0
  + C(TaskMaintenanceTurnContractTest) fresh exit 0
  + HTTPS_TURN_CONTRACT_TEST_FAMILY 中实际调用链相关的 T01/T02/T03/T04 门
  + applicable Cloud compile/build gate
```

证据与边界：

1. 权威计划 `:1463-1476` 要求每张未实施 Java 卡同时交付 production/test，并在 required test 与适用 compile 后才进入父级最终卡片裁决。
2. 计划 `:1602-1605` 要求 TURN-13G..40D 在最终卡片裁决前满足 T01/T02/T03/T04 中实际调用链相关部分。34B 的真实链为 `TaskMaintenanceService -> DialogService/SummonSkillService -> TurnGameClient/CloudUiCleanerPort -> exchange/DHXY executor/local UI adapter`，因此四个 family 均有实际链路关联；不得用 34B 单测替代这些基础门。
3. 计划 `:1640` 唯一点名 `service/TaskMaintenanceTurnContractTest`，profiles=`BC4+BASE+TASK+STATE`；未来稳定 writer gate 的精确命令是 `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`。本 helper 未运行。
4. 计划 `:1437-1444` 的 Cloud source compile 是 `mvn -q clean compile`；最终 `clean package` 会运行现有测试，只有父级取得用户对该次 package/test run 的明确授权后才能执行。本 helper 不把未运行门写成通过事实。
5. TURN-21/22/23/26/33 各自未完成的 named-test/build 是各卡自己的最终门；它们不改变上表 source start 判定，也不能替代 34B 自己的 named test、双 reviewer 与 compile 证据。

## 证据 4 - 唯一 production/test write set

未来父级固定卡只能授权以下两个 production/test 文件：

1. Modify only production:
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
2. Create only named test:
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`

独立 process artifact 只能是父级未来创建并由实施者 append 的：
`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34B.md`。

除上述范围外全部只读，尤其包括 `AutoCombatService`、`AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2`、`SummonSkillService`、`TeamReturnService`、`DialogService`、maintenance models、`TaskExecutionContext`、turn protocol/client/result、POM/config/resources、DHXY 全仓以及 TURN-21/22/23/26/28P/33/34A 的 production/test/card。

## 证据 5 - 与 TURN-34A 六个冻结 API 的并行互斥证明

TURN-34A 固定卡 `:103-110` 明文冻结以下六个现有 API；活动领取段 `:175` 再次承诺只调用、不改签名。当前真实源码仍保持下表契约：

| Frozen API in `TaskMaintenanceService` | 当前声明 | 当前 `AutoCombatService` 调用点 |
|---|---|---|
| `isPendingLocalSupportLeaderDetection(context)` | `TaskMaintenanceService:416-423` | `AutoCombatService:485,535,654` |
| `isLocalSupportMemberSession(context)` | `TaskMaintenanceService:321-327` | `AutoCombatService:492,523,645` |
| `isLocalTeamSupportCapabilityOpen(context, capability)` | `TaskMaintenanceService:468-475` | `AutoCombatService:493-494,646-647` |
| `awaitLocalTeamSupportCapabilityOpen(context, capability, timeoutMs)` | `TaskMaintenanceService:285-315` | `AutoCombatService:524-525` |
| `isLocalSupportMemberCandidate(context)` | `TaskMaintenanceService:404-409` | `AutoCombatService:541` |
| `awaitTeamFirstAidMaintenanceWindowOpen(context, teamMaintenanceKey, timeoutMs)` | `TaskMaintenanceService:247-274` | `AutoCombatService:544-545` |

并行互斥证据：

1. 生产文件物理互斥：34A 只写 `AutoCombatService.java`，34B 只写 `TaskMaintenanceService.java`；权威计划 `:1297` 与 `:1428-1429` 明确允许两卡在各自 source 门满足后并行。
2. 测试文件物理互斥：34A 为 `AutoCombatServiceTurnContractTest.java`，34B 为 `TaskMaintenanceTurnContractTest.java`；路径不同，且当前两文件均不存在。
3. 固定卡物理互斥：34A 与未来 34B 各有独立 append-only 卡；本 helper 的报告又与两张实施卡分离。
4. 共享边界是六个现有 Java API，而不是共享写文件。34A 只能保留当前调用签名、参数与 boolean 语义；34B 只能在 `TaskMaintenanceService` 内迁移其实现状态，不得要求 34A 改 caller。
5. 当前 34A 活动源码已从领取 SHA 演进到 `532E6F...`，但六组调用仍与冻结表一致；`TaskMaintenanceService` SHA 仍是 `39AEF8...`。这只证明本快照下的条件互斥，不替代未来 34B 领取前的再次 hash/API 核对。
6. 34B named test 必须直接测 production `TaskMaintenanceService`，不能依赖或修改活动中的 34A test；34A test 也不得通过复制 TaskMaintenance 实现来固定私有语义。

因此，并行条件不是“两个文件名不同”这一项，而是同时满足：生产/test/card 路径互斥、六 API shape 与 caller-visible 语义不漂移、34B 不写 AutoCombat、34A 不写 TaskMaintenance。

## 证据 6 - TURN-22 尚未恢复时只能冻结的内容

当前只允许父级/helper 以只读方式冻结：

1. 本报告中的 dependency snapshot、两文件 production/test write set、六 API 表、19 个 public API 清单、`696a12b0` 验收规则、named-test matrix 与 stop-work 条件。
2. TURN-28P、TURN-22、TURN-34A 物理末尾及目标文件 SHA/API 的后续只读刷新。
3. 父级未来实施卡的候选正文；在 TURN-22 parent source/test-source gate 恢复前，该正文不能转化为实施领取事实。

当前不能开始：

1. 不能创建 TURN-34B 固定实施卡、写 34B 领取段、修改 `TaskMaintenanceService` 或创建 named test。
2. 不能猜 TURN-28P 最终 frozen queue 方法名、参数、返回值或异常投影，也不能在 34B 内复制第二套 wrapper。
3. 不能修 TURN-22 的 Cloud test、DHXY `TurnInputStepExecutor`、`150/500` queue mechanics 或 sentinel fixture。
4. 不能把 `TEAM_RETURN+COMMON_BOX` capability coordination 扩成 TeamReturn click/action/UUID/frame/queue 逻辑。
5. 不能因 TURN-33 source contract 已稳定而提前消费一个仍未通过 TURN-22 的 team coordination contract。
6. 不能激活当前零 external production caller 的四个 local-session lifecycle API，也不能新增 host/factory/runtime/session authority。

## 证据 7 - `696a12b0` 等价验收

当前 `TaskMaintenanceService` 与 `migration-baseline/696a12b0` 的只读完整 diff 只出现 context ownership 适配：旧 `WindowTaskContextHolder/WindowRuntimeContext` 换为 `TaskExecutionContextHolder`、`summonSkillState` 接收显式 context、window/log fallback 改读 task holder、player identity epoch 改从显式/holder context 取得。`runOpportunisticMaintenance`、Summon gate/body、team windows、claim/cache/result 与 19 个 public API 的业务主体没有其它 diff。

34B 的等价验收必须同时锁住：

1. 19 个 public 方法名称、参数、返回值和 caller-visible 语义不变；真实 direct production callers 仍只有 `AutoCombatService`、`AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2`，四个零 external caller API 不制造可达性。
2. `runOpportunisticMaintenance` 精确保持 `checkpoint -> maintenance broadcast -> handled/failed/interrupted short-circuit -> optional one Summon public call -> no-action`。broadcast short-circuit 时 Summon=`0`；只有 no-action 且 request 允许时 Summon=`1`。
3. Summon gate 顺序保持 feature、interval、FREE、due、既有 unknown-failure interval、既有 2h tail-safe/skill-count cache、team round/local capability/pathing、duplicate/max claim、action 前 checkpoint。
4. 一次 due maintenance 只调用一次 TURN-33 public `cleanSummonSkillsOnce(request)`；TURN-33 内部的 static-tail observation、五删 budget、终极角和 cleanup 不得复制到 34B。
5. success、known failure、delete/ultimate state change、terminal/uncertain/STOP 对 cooldown/cache/claim 与 previous action-state 的投影不变；不把异常或不确定性改成 false/success，不自动重调。
6. pathing window 精确打开 `FIRST_AID/PATHING_WINDOW/COMMON_BOX/SUMMON_SKILL/LEFT_TOP_STATUS`；first-aid weak window只打开 `FIRST_AID`；close 精确关闭上述五项；return support 精确打开/关闭 `TEAM_RETURN+COMMON_BOX`。
7. `docs/业务逻辑.md:1-67` 的初级 local-team session 边界与 `:145-156` 的 CommonBox 最高优先级不变；34B 只维护 capability，不抢占 34C/真实 caller 的任务级消费顺序。
8. `docs/业务逻辑.md:170-211` 的静态尾扫只由 TURN-33 owning service 实现；34B 不新增 hover、scan、delete、template、PNG、OCR、click 或 UUID。
9. turn-native first-due 必须不再调用 legacy-only `TaskExecutionContext.getPlayerIdentityEpoch()`；current metadata missing 或 device/window/HWND/process/title drift 必须在 Dialog/Summon delegate 前得到 closed failure，delegate/action/UUID=`0`。这项只能在唯一 production 文件内使用现有 exact-context/metadata API闭合；若需要改 context/protocol/model，立即停止扩大写集。
10. 现有 context-bearing singleton state 必须按既有 tenant/user/device/window scope 隔离；legacy/null fallback 保持原语义。只增加 namespace/fence，不新增业务时钟、owner、session、lease、ledger、TTL、compaction、durable workflow、background queue 或 transport retry；现有 2h cache 与已批准业务时间不属于“新增”。

固定业务口径：无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 与 `docs/业务逻辑.md` 已确认规则等价迁移。

## 证据 8 - 未来 named-test matrix

唯一测试类：`com.yueyunfe.dhxy.cloudbrain.service.TaskMaintenanceTurnContractTest`。测试必须直接实例化 production `TaskMaintenanceService`，通过其 public API 驱动；只用 test-private scripted Dialog/Summon/turn collaborators，不启动 Spring、HTTP、host、runtime、Task、UI 或真实 input/capture，不新增 production test hook/helper/DTO/facade。

| Matrix | 必须直接穿过的 production surface | 正向/保持证据 | 负向与旧代码失败能力 |
|---|---|---|---|
| API/caller boundary | 全部 19 个 public API；特别是六个 34A frozen API | shape、参数、返回逐项固定 | 任何 rename/overload/返回漂移使 test-compile 或反射 shape 断言失败；caller 文件 SHA 由父级静态核对，不用 source-only test 冒充运行证据 |
| Priority | `runOpportunisticMaintenance` | broadcast no-action 后 Summon 恰好 1 次；两项关闭返回原 no-action | handled/failed/interrupted 时 Summon=0；pre-checkpoint stop 时 Dialog/Summon=0 |
| Summon gates | `runOpportunisticMaintenance` due path | valid due/pathing/capability 只调用 final TURN-33 public API 1 次 | disabled、interval<=0、non-FREE、not-due、unknown interval、fresh tail cache、no round、closed capability/pathing、duplicate/max claim 全部 delegate=0 |
| Result projection | due path + scripted result/exception | success 更新既有 skill-count/start-slot/tail/ultimate/cooldown；known fail/no state change 释放 claim | delete/ultimate state change 保留 claim；terminal/uncertain/STOP 不伪成功、不刷新成功 cooldown、previous action state 恢复、调用数不超过 1 |
| Exact turn context | `TaskExecutionContext.turnNative(...)` + first-due public path | exact current metadata 与 initial identity 一致时可到 delegate | 当前旧路径会调用 legacy-only epoch，故 turn-native first-due 用例能在旧代码失败；missing 或 device/window/HWND/process/title drift 时 delegate/action/UUID=0 |
| Scope isolation | 两组真实 public setup/run APIs | 不同 tenant/user/device 使用相同 windowId/task/round 时各自维护 cache/cooldown/formal round/window/claim | 当前 plain key 会串 scope，因此交叉读写用例能在旧代码失败；explicit context 必须胜过 holder 中错误 context |
| Team windows | begin/open/close/await/is-open public APIs | pathing 五项、first-aid 一项、close 五项、return 两项、capability epoch/leader conflict/absent/all-candidate completion 保持 | first-aid 不得放行 Summon；close 后 await/is-open 为 false；四个零 caller lifecycle API 只锁单 scope 基线，不宣称 runtime 激活 |
| TURN-33 delegation boundary | `runOpportunisticMaintenance` -> scripted production collaborator | 一次 typed delegate 与现有 request/result projection | 不在本测试复制 static scan、五删、post-delete observation、action/UUID/PNG/OCR fixture；任何第二次 delegate 立即失败 |
| TURN-22 separation | return capability public APIs | 只断言 `TEAM_RETURN+COMMON_BOX` capability 状态 | 不构造 `150/500` JSON、queue、click 或 sentinel fixture；出现 TeamReturn mechanics 即越过本卡边界 |
| Legacy/null compatibility | legacy context 与 null fallback public paths | 原 default/window/team-key/log fallback 保持 | 不把 turn-native exact fence反向变成 legacy 行为变化，不新增 fail-closed observation 或计时 |
| No extra machinery | 全部 public paths + invocation counters | 显式业务 continuation 仍由 owning service 内部完成 | transport retry、第二 command、补偿调用、新 owner/session/ledger/TTL/queue 均应由零额外调用和静态 write-set review发现 |

本矩阵不重复 TURN-22 的 input-queue mechanics test，也不重复 TURN-33 的 static-tail/action/UUID test；它只验 34B coordinator、state、scope 和一次 typed delegate。

## 证据 9 - stop-work 条件

未来任何实施者遇到下列任一事实必须立即停止写源码/测试并回到父级刷新固定卡：

1. TURN-22 Parent Review #4 的 `0/2/1` 尚未由 Repair #3 新 source/test-source 证据覆盖，或 TURN-28P 最终 frozen API 仍未由父级固定。
2. 父级尚未创建 TURN-34B 固定实施卡，或卡片物理末尾没有真实 lane 领取记录。
3. `TaskMaintenanceService.java` 在领取前不再是本报告 SHA，或已有另一 active writer 占用同一 production/test/card 写集。
4. TURN-34A delivery 改动六个 frozen API 中任一签名、参数、返回、capability 条件、等待语义或调用顺序，或要求 34B 同时修改 `AutoCombatService`。
5. 34B 需要第二个 production 文件、第二个 named test、caller、maintenance model、`TaskExecutionContext`、turn protocol/client、POM/config/resource 或 DHXY 修改。
6. TURN-33 reviewed public boundary、result/exception/cleanup contract或 reviewed SHA 发生变化，导致“一次 public Summon delegate”无法按本报告投影。
7. 计划改变 `696a12b0` 的条件、优先级、delay、fallback、cache/cooldown、claim/release、capability set、失败/terminal 语义或动作次数。
8. 拟复制 TURN-22 TeamReturn mechanics、猜未完成的 TURN-28P API、复制 TURN-33 loop，或新增 action/UUID/frame/OCR/input authority。
9. exact-context/scope 修复无法在唯一 `TaskMaintenanceService.java` 内使用现有 API 闭合，必须发明 wrapper/facade、owner/session/ledger/TTL/durable workflow 或自动 retry。
10. named test 不能直接穿过 production public API、不能让 current old-code seam 失败，或只能靠 private helper/source string/无效 mock 证明。
11. 完成工作需要覆盖、删除、清理、回滚或重建任一既有 dirty/untracked 文件。

## PRECHECK 当前事实

- 唯一未满足的显式 `startDependsOn`：TURN-22。
- TURN-33 delta：Parent Review #5 和独立 R1/R2 source/test-source 证据已经齐全；named test/build 仍属后续门。
- TURN-34A collision delta：External C 正在写 `AutoCombatService`；六个 TaskMaintenance API 在当前源码中仍与固定卡一致，文件级并行成立但必须在 34B 真实领取前再次核对。
- 34B 当前允许动作：只读冻结和等待父级下一任务；不得开始 production/test 实施。
- 本轮写入：仅本报告。
- 本轮 Maven/runtime/input/Git mutation：无。

PRECHECK_COMPLETE true EOF
