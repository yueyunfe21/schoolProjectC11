# CR271 TURN-38M DELETE candidates companion/cohort closure PRECHECK R1

## 0. 角色、范围与结论边界

- 日期：2026-07-16。
- 角色：CR271 Internal 非实现、非 reviewer、非父级的非绑定分类证据 helper。
- 本轮只审计三个当前 `DELETE` 候选的 companion/cohort closure：
  `CloudPausedReadOnlyObservationContext`、`CloudPlayerStateStateGovernor`、
  `CommonBoxStateGovernor`。
- 本文中的 `DELETE` 始终表示“待父级裁决的候选方向”，不是分类冻结，也不改变任何卡片状态。
- 本文只给 `PRECHECK`、当前源码/ref/SHA、最小 companion closure、未来 44A 条件 cohort、测试归属、
  跨卡冲突与 `STOP-WORK` 条件。父级固定分类表和 44M45M 最终 manifest 仍是唯一可执行依据。
- 唯一写入是本文。未修改 Java、test、权威计划、CR271、`ACTIVE_WORK.md`、dashboard 或其它报告；未占用
  任何 production/test 写集。
- 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input；未执行
  Git mutation。

## 1. 完整读取材料与只读快照

### 1.1 已读权威材料

本轮完整读取或复核：

1. `D:/mavenProject/DHXY/AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271。
2. 权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
3. `2026-07-16-turn-38M-authority-classification-preflight-helper.md`。
4. `2026-07-16-turn-38M-game-context-owner-route-risk-helper-r1.md`。
5. `2026-07-16-turn-38M-left-top-owner-route-risk-helper-r1.md`。
6. 最新 `2026-07-16-turn-38A-readiness-preflight-helper.md`。
7. `2026-07-16-turn-44A-post-45A-deletion-readiness-helper-r1.md`。
8. `2026-07-16-turn-44M45M-readiness-preflight-helper.md` 与
   `2026-07-16-turn-44M45M-scc-decomposition-helper-r1.md`。
9. 为核对 45A 零写入边界，完整读取
   `2026-07-16-turn-45A-route-disconnect-readiness-helper-r1.md`。
10. 完整读取三个目标文件及两个零引用 companion；逐项读取 direct-consumer 相关实现段、active owner 的状态实现段，
    并全量扫描 Cloud/DHXY production/test symbol refs、当前 17-file SCC、40B 未来路径及相关 named-test refs。

权威计划的本轮硬边界是：

- `:1325-1334`：38C 只实施父级冻结的 `KEEP_REWIRE` 行；`DELETE` 行字节保持不动到 44A。
- `:1355-1362`：40B 只创建五个 runtime 文件；current runtime 与 last accepted ID/ack 是易失内存状态，
  不持久化、不加 TTL、不自动 retry。
- `:1397-1403`：Cloud 删除严格 `45A -> 44A -> 45B`，不得按目录、前缀或 wildcard 删除。
- `:1497-1499,1650,1664,1671-1673`：38M 是 `ZERO`；38C 独立 `*TurnStateTest` 只属于保留重接行；
  44A 的唯一新测试是 exact `OldAuthorityRemovalGuardTest`。

### 1.2 双仓受保护快照

快照时间：`2026-07-16T07:27:30-04:00`。

| Repo | Branch | HEAD | tracked dirty | untracked | 说明 |
|---|---|---|---:|---:|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 44 | 605 | 本报告写入前快照；全部保护 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 9 | 541 | 全部保护；下列 production 文件均为 `??` |

三个候选及其 companion 都不在 Cloud HEAD 中；当前只能对共享 untracked 工作树取证，不能把 HEAD 当作可恢复
baseline，也不能把本轮 SHA 当作未来删除授权。父级固定分类文件
`2026-07-15-turn-38-authority-state-classification.md` 和最终
`2026-07-15-turn-44-45-cloud-old-wire-delete-manifest.md` 当前均不存在。

### 1.3 术语口径

- **direct consumer**：源码中直接出现目标 Java type token 的外部 production 文件。
- **minimal companion closure**：删除目标后，为避免留下只被目标消费或继续消费目标的孤立源码，当前最少需要
  同一父级批次裁决的文件；它不是最终 manifest。
- **44A card-level cohort**：45A 完成后，按 final source graph 必须在同一 44A patch/guard/compile 门内处理的
  全体文件。它可能大于某一候选自己的 minimal closure。
- **semantic residual**：目标删除后可能只剩 old wire/run 语义的文件，交 45B 重新逐项扫描；不能自动并入 44A。

## 2. 当前非绑定 closure 总表

| 候选 | 外部 direct consumers | 当前 minimal companion closure | 仍活跃业务 owner | 38C 当前候选动作 | 未来候选去向 |
|---|---:|---|---|---|---|
| `CloudPausedReadOnlyObservationContext` | 32 个 type-site / 6 文件 | target + 6 direct consumers；因 6 文件都在 17-file SCC，card-level 被提升为 target + 完整 SCC | 新 turn 不需要 paused observation owner；pause 由 exact metadata/checkpoint/current runtime 协作 | target 与 consumer 均零写入 | 若父级维持候选，随 44A authority SCC |
| `CloudPlayerStateStateGovernor` | 0 | governor + `CloudPlayerStateStateOwner`；nested capacity exception 随 governor 同文件 | `PlayerStateService.runtimeStates` | 两文件零写入 | 若父级维持候选，作为 44A 两文件 leaf |
| `CommonBoxStateGovernor` | 0 | governor + `CloudCommonBoxProperties` | `CommonBoxService.pendingByKey` + `BotProperties` role toggles | 两文件零写入 | 若父级维持候选，作为 44A 两文件 leaf |

上表不是父级分类结果。特别是“零外部 direct consumer”只证明当前 dormant，不等于允许 38C 或本 helper提前删除。

## 3. `CloudPausedReadOnlyObservationContext` closure 证据

### 3.1 目标实物与 direct consumers

目标当前为 133 行、5,321 bytes、SHA-256
`BE02F23DB41CEA7F4342FF6B2FFC6757D6FDB16BE8882131F8818F676791CAE3`。

- `:9-23` 定义 exact PAUSED revision 的 immutable read-only capability。
- `:25-30` 直接保存 full old scope、taskRun、window、stopEpoch、paused revision。
- `:56-66` 只可从 `RemoteTaskRunBinding` 的 `PAUSED` status mint。
- `:69-131` 继续投影 old client session、window ref 与 stop ref。

全部外部 production type-site 如下；Cloud tests 与 DHXY production/test 均为零命中：

| Direct consumer | type-site | 当前 SHA-256 | 实际职责 |
|---|---|---|---|
| `CloudTaskRunExecutionGate.java` | `:345,353,366,390,411,415` | `0C5BC991665A869D3515701EEADA15C500C66AA4AE5AB81FF7C029AD353E0459` | mint snapshot，构造 PAUSED WINDOW_FACT/CAPTURE request并逐次 revalidate |
| `CloudTaskRunAuthorityAssembly.java` | `:152,163` | `A22AF1D212B0A1734FED546D44B413FDE2226FFE4ED9E88865EA8666A185D0E1` | lifecycle adapter 的 mint/park facade |
| `CloudTaskRunRetainedLifecycleActivationAdapter.java` | `:142,179,209,224,327,341-342,413,419` | `F9820B6678A22E67E752EF09F89BC210AA20C9EC2D2E11A854DE746D53D7D9DD` | pause/resume generation、opaque capability、snapshot retention |
| `CloudTaskRunActionLedger.java` | `:437,439,1162,1173,1197,1411,1752` | `90B980BDC4C9147F69DE5F93EE6EE6AAB7356CD02516A767CACAE93470BD63B1` | observation identity、semantic frontier、stable-run registration |
| `CloudTaskExclusiveInteractionAuthority.java` | `:70,72,1162` | `91349697592CD33CF32870E5B6732A21470480C2CE6EF16BCA90A3444297ABCC` | 将 ACTIVE exclusive generation park 到 exact paused successor |
| `CloudTaskRetainedActionState.java` | `:601,605,639,647,661` | `DFE18415D3B5D539B499B7BA574BF940F4F69BE4478761CDF47AE302928FFCCA` | retained battle-radar paused observation slot |

这六个文件全部属于第 7 节当前 17-file SCC。目标本身是 SCC 的 outward leaf，不是可先删的独立文件：只删 target
会让六个 consumer 失去类型；先逐个改 consumer 又会提前拆 44A SCC。

### 3.2 companion 与 45B semantic residual 边界

PAUSED observation 还有下列语义残余，但它们不是本候选可自动带入 44A 的 companion delete 行：

| 文件 | 当前 SHA-256 | 当前 paused-specific evidence | 后续边界 |
|---|---|---|---|
| `RemoteObservationMode.java` | `A788377B052D5AC9DAC6C84B1AC7BD8A956E5EDE382157935841CE96B1414945` | 唯一 enum 值 `PAUSED_READ_ONLY` | 44A 后由 45B 重扫，不能因单值 enum 先删 |
| `RequestContext.java` | `E2167213EEB5367BFEBEA0B80AFE9D6D721ED8CE212974E4BB22056C816A7631` | `:33-43` observationMode/input/attempt 约束 | 仍是 shared old request DTO，非 44A 自动 companion |
| `RemoteFinalConsumedAck.java` | `E62926E4D3AA217DD1FE99A2AD1F3137E401F260924E55BA22272A67943DE1E3` | `:89-104` paused final-consumed 规则 | 44A 后按 final-consumed residual 重扫 |
| `remote/run/RemoteTaskRunCoordinator.java` | `E0F57807F01C807AC1BD0A36E7B8646CD3576877A345000531B950D611EDF0FD` | `:890-925` denial/dispatch permit | old run wire 归 45B；不得拖入 44A |

`RemoteGameCommandBroker.java:889-890,1451-1465` 也直接调用 coordinator 的 paused gate，但 broker 已在当前 17-file
SCC 中，属于 44A core 而不是 45B residual。44A 完成后，上表四个文件是否仍有其它 caller，必须重新逐 symbol
计算；本报告不作后续删除分类。

### 3.3 仍活跃 owner 与 38C 零写入原因

新 HTTPS turn 的 pause owner 不是一个 read-only observation capability：

- `TurnWindowMetadata.pauseRequested` 与 `TaskExecutionContext` checkpoint 保持同一 Task/context/state 调用栈。
- 未来 `CloudTurnTaskRuntime` 只持 current in-memory runtime；resume 不创建 session、revision、handle 或 replacement
  Task，也不在 pause 中另行下发 capture/window-fact 业务动作。
- 协议和权威计划没有 paused observation request、retained observation identity 或 final-consumed acknowledgement 的
  turn-native入口。

因此，若父级维持当前候选方向，38C 对 target 和六个 consumer 均应为零写入。把 target 改成 turn-native 会新建
“pause 期间继续观察”的业务能力，并把 old client session/runRevision/ledger/exclusive park 带回新 runtime；这不是
无差异 plumbing。

### 3.4 跨卡冲突

- **38B1-4**：四卡与 target/六个 consumer 物理写集为零；任何 B 卡把它当 generic pause/state authority 都越出
  Bag/ReturnItem/startup/artifact 边界。
- **39**：39 会修改当前 17-file SCC 内的 `CloudGameClient`、`CloudTaskServicePort`、
  `CloudTaskServiceExecutionContext`。虽然它不直接写 target/六个 paused consumer，仍可能改变最终 SCC 形状；39 后
  必须重算，不能沿用当前 17-file hash/cohort。
- **40B**：五个未来 production path 当前均不存在。40B 对 target、六 consumer 和 semantic residual 必须零引用；
  若 runtime/factory 引入任一 old paused type，候选前提立即失效。
- **45A**：只改 Server、删 routes/endpoint、建 route guard；对 target 和 17 core 均为零写入。45A 结束时应是
  “old route 已断、target + old SCC 仍完整可编译”，不能顺手删 target。

### 3.5 44A/test ownership

- 若父级最终把本候选归 44A，当前图下 target 必须与完整 17 core 同一 card-level patch；不得先删 target再留六个
  direct consumers。
- 38M 本身是 `ZERO`，Cloud tests 对 target 名称当前零命中；DELETE 候选不创建 38C `*TurnStateTest`。
- 未来唯一删除测试 owner 是
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/OldAuthorityRemovalGuardTest.java`，该文件当前不存在。它必须按 final
  manifest exact path/symbol 证明 target 与六个 consumer 零生产引用，不能扫描整个 `remote/` 或 `Remote*`。

## 4. `CloudPlayerStateStateGovernor` closure 证据

### 4.1 目标与 companion 实物

| 文件 | 行数 / bytes | SHA-256 | 外部 production/test ref |
|---|---:|---|---:|
| `remote/CloudPlayerStateStateGovernor.java` | 1,732 / 77,998 | `B5E17B474C11EC6D2FBBD0B01814E78D807CA4E47982A2D51B1597FD1702F713` | 0 |
| `remote/CloudPlayerStateStateOwner.java` | 460 / 17,060 | `2011362BBB6723DE4AA9A1CFE5828025A48D3DDC3D8DCB85225CFE5CA95D2D75` | 只被 governor 17 个 type-site 消费 |

Governor 的五个自身 type-site只是 declaration/constructors/self helper；没有 assembly、Spring、Task、Service 或 test
构造 caller。其 nested `CloudPlayerStateCapacityException` 也只有 governor `:828-843` 两个抛出点，随 governor 同文件
消失，不形成第三个 source companion。

这不是一个窄 per-window state map：

- `:38-47` 持有 coordinator、physical entries、exact sessions、owner quota 与 global accounting。
- `:85-232` 定义 provisional preparation、commit/rollback 与 opaque projection handle。
- `:243-347` 按 run revision resume 并在 owner lock 下投影。
- `:355-435` terminal release 进入 `RELEASE_PENDING`，允许相同 handle/binding 重试。
- `:437-465` 只有 authenticated client session release 才清 persistent entries。
- `:1452-1519` key 直接包含 old scope/clientSession/taskRun/window/stopEpoch/runRevision。

`CloudPlayerStateStateOwner` 是 governor-private business-state object；它复制 first-aid/incense/check counters、pending
plan/lease、startup precheck 与 execution lock。由于它没有 governor 外 caller，当前 minimal companion closure 是
**governor + owner 两文件**。只删 owner 会破坏 governor 编译；只删 governor 会留下一个外部零引用 owner leaf。

### 4.2 仍活跃业务 owner 与 consumers

真正 active owner 是 `PlayerStateService.java`：

- 文件当前 SHA-256 `865A66B761EB9752B9697CDDF8058F06D71A9B87BD0B7D0895025298C0C35548`。
- `:111` 的 `runtimeStates` 是 current `ConcurrentHashMap`。
- `:1216-1242` 以 turn `deviceId/windowId` 为 key；没有 context 时回落 `default`，identity drift 时替换 state。
- `:1368-1377` 保存 incense 时间/offset、首药检查计数、战斗退出时间和 pending first-aid plan。

Active invocation consumers 为：

- `AutoBattleTask.java:135`；
- `AutoCombatService.java:362,382,397,399,409,462,464,472,552,554,556`；
- `XiuluoTaskV2.java:354,594,1110,1561,1941,2310,2853,2875,2902`；
- `WubeiTask.java:357-358,1770,3888,3914,4143`；
- `FiveRingTaskV2.java:768,1112,1833`；
- `TeamReturnService.java:64`、`NavigationService.java:821`。

`PlayerStateService` 及其 test 不是 dormant governor 的 deletion companion，必须作为 active business owner保护。
`PlayerStateTurnContractTest.java` 当前 SHA-256
`FAD5523999506453BE16FBCF3DE68DBC16FCE55047B925FAE694A944ACDE81D1`，对 governor/owner 名称均零引用。

### 4.3 38C 零写入原因

Governor 本身没有 active caller，38C 不需要通过改写它来维持任何 turn-native API。反而一旦接入，就会同时激活
session、projection handle、run revision、quota、persistent entry 与 retryable terminal release，明显超出 38C 的
state rewire 边界并冲突最小 turn 合同。

若父级维持当前候选方向，38C 应对 governor/owner 两文件零写入，active `PlayerStateService` 行为也保持不变。
`PlayerStateService.runtimeStates` 是否跨 concrete Task/new start 保留、何时按 identity/window/host lifecycle 释放，仍需
父级依据 696 基线单独冻结；不能用 dormant governor 填补这一待定项，也不能顺手增加 terminal cleanup、TTL 或 retry。

### 4.4 38A/38B/39/40B/45A 冲突

- **38A compile conflict**：governor 当前直接消费 `TaskExecutionContext` 的 old
  `getScope/getRunRevision/getPlayerIdentityEpoch/getStopEpoch`，关键区间为 `:85-94,243-250,306-314,940-947,
  1462-1483,1509-1519`。38A 若在 governor 仍留到 44A 时真删这些 API，会破坏中间 compile；父级必须先冻结
  source-compatible shell、重排原子删除边界或其它明确方案。38C 不能通过改 governor偷偷修复。
- **38B1-4**：四卡 exact production write set 与 governor/owner/active PlayerStateService 均无交集；B1 的 Bag state、
  B2 ReturnItem state、B3 startup gate、B4 artifact store 都不得吸收这个 dormant persistent-session design。
- **39**：六文件写集与 governor/owner物理零交集。39 只收 active old facade；它不能为维持 dormant governor改
  `TaskExecutionContext` 或 old run types，也不能把这两个 leaf并入自己的 `OldFacadeRemovalContractTest`。
- **40B**：runtime/factory 必须对 governor、owner、projection/session types 零引用。40B 若需要 PlayerState lifecycle
  integration，应由父级先冻结 active `PlayerStateService` 的既有语义和合法 API/write-set；不得在五个新 runtime文件中
  反射、static lookup 或另建 governor map。
- **45A**：对两文件零写入、零删除；route sever不构造也不消费它们。把它们顺手放进 45A 会超过固定 3+1 写集。

### 4.5 44A/test ownership

- 若父级最终维持候选方向，当前 candidate-specific minimal closure 是
  `CloudPlayerStateStateGovernor.java` + `CloudPlayerStateStateOwner.java`；两行都必须进入 final 44M45M manifest，
  不能只列 38M 点名的 governor。
- 两文件应在同一 44A patch/guard/compile 门处理；`CloudPlayerStateCapacityException` 作为 governor 同文件 symbol
  由同一 exact guard 覆盖。
- `OldAuthorityRemovalGuardTest` 是唯一新测试 owner；它验证 exact 两路径不存在、两个主 symbol与 capacity symbol
  无生产引用，并同时证明 active `PlayerStateService` 路径仍存在。它不接管或改写
  `PlayerStateTurnContractTest`。

## 5. `CommonBoxStateGovernor` closure 证据

### 5.1 目标与 companion 实物

| 文件 | 行数 / bytes | SHA-256 | 外部 production/test ref |
|---|---:|---|---:|
| `remote/CommonBoxStateGovernor.java` | 484 / 20,044 | `DD4C8CCA5D020CF729820414CEF10B70C6082B9DE66A448C257FCC0FA6B11465` | 0 |
| `com/bot/dhxy/config/CloudCommonBoxProperties.java` | 27 / 1,124 | `2369AE89E2A18E3D29635CBD4999714153E01381E148C836BCFCD5A41E5BB82A` | 只被 governor import/return/implement |

Governor 自身只有 declaration/constructor 两个 type-site，没有 assembly、Spring、Service、Task 或 test caller。
`CloudCommonBoxProperties` 除自身 declaration 外只在 governor `:3,59,472` 出现，因此当前 minimal companion closure 是
**governor + properties 两文件**。

Dormant governor 并不是 active Service 的等价 wrapper：

- `:16-30,38-42` 管理 tenant state、incarnation、config revision、capacity 和 pending map。
- `:96-218` 引入 detect ticket、claim/seal、reservation 与 settle lifecycle。
- `:34,137-154,287-292` 内置 30 秒 TTL/prune。
- `:256-278` 添加 run cleanup 与 scope retirement。
- `:406-448` fence/key 仍含 full old `RemoteTaskRunScope`、window tuple、taskRun、stopEpoch、runRevision。

### 5.2 仍活跃业务 owner 与 consumers

真正 active owner 是：

- `CommonBoxService.java`，SHA-256
  `93E93321AE4CBDD29C3D94AF4172D72AE8FEFE137CF417E7F2C570B93856CE68`；
  `:62` 的 `pendingByKey`、`:141-193` consume、`:320-341` observation commit 与既有 30 秒 pending TTL。
- `BotProperties.java:84-85` 的 leader/member switches，文件 SHA-256
  `D56521FDF71CDCA8B2CAA660E4398330CF64A631D135E1863F8727498FD9BB87`。

Active invocation consumers 为：

- `XiuluoTaskV2.java:2412,2475`；
- `WubeiTask.java:2790,3935`；
- `AutoBattleTask.java:247`；
- `AutoCombatService.java:366,481,500`。

`CommonBoxService`、`BotProperties` 和现有 30 秒业务 TTL 都不是 dormant governor 的 deletion companion，必须保护。
`CommonBoxTurnContractTest.java` 当前 SHA-256
`6C3FFA9E9CA303A8A755668603533ABCA19D70EC6D58737403A5EE4E0EAE6D50`，对 governor/properties 名称均零引用。

### 5.3 38C 零写入原因

Target 无 active caller，38C 不需要改它来保住业务。把它接入会新增 tenant incarnation、config revision、claim/seal、
capacity、scope retirement 和 old run fence；即使两边都恰好写了 `30_000ms`，其消费/失败/并发语义也不等价。

若父级维持当前候选方向，38C 对 governor/properties 两文件零写入；active Service 的 30 秒 TTL 原样保留，既不删除
也不复制。`no TTL` 约束在这里表示“不新增或改变 TTL”，不是把用户基线中已经存在的 pending TTL 顺手移除。

### 5.4 38A/38B/39/40B/45A 冲突

- **38A active-owner gap**：`CommonBoxService.java:448,460` 仍调用 old
  `TaskExecutionContext.getPlayerIdentityEpoch()`，最新 38A PRECHECK 已确认当前没有明确 pre-38A owner。删除 dormant
  governor不会修复这两处；38A/父级必须单独冻结 compatibility/rewire owner，不能把 CommonBoxService塞进 44A。
- **38B1-4**：四卡与 governor/properties/CommonBoxService 物理写集均为零；不得以“state owner”相似为由把
  CommonBox companion吸收到 Bag/ReturnItem/startup/artifact 卡。
- **39**：`CommonBoxService.java:466` active 使用 `TurnGameClient`，但该 Service不在 39 写集。39 可以收 final client
  facade，不能顺手改 common-box owner，也不能把 dormant governor当 metadata authority。
- **40B**：runtime/factory 对 governor/properties 必须零引用，只通过 final Task/Service injection 调用 active
  `CommonBoxService`。不得新增第二 pending map、session lookup、claim ledger、TTL cleaner或自动 click retry。
- **45A**：对 governor/properties/active Service 均零写入；45A 只断 old route registration，不处理业务 state leaf。

### 5.5 44A/test ownership

- 若父级最终维持候选方向，candidate-specific minimal closure 是
  `CommonBoxStateGovernor.java` + `CloudCommonBoxProperties.java`。Properties 不在 38M 五目标列表内，必须由最终
  44M45M manifest 独立列 path/symbol/ref/SHA；没有该行就不能在 44A 顺手删。
- 两文件应在同一 44A patch/guard/compile 门处理；`CommonBoxService`、`BotProperties` 与 active
  `CloudCommonBoxPort` path明确排除。
- `OldAuthorityRemovalGuardTest` 负责 exact 两路径/symbol零引用；不接管或改写
  `CommonBoxTurnContractTest`，也不能把 active `PENDING_TTL_MS` 当作 old authority token删除。

## 6. 当前 44A shared card-level cohort 证据

### 6.1 当前 17-file SCC 与 SHA

以下只是 39/40/45A 前当前图的 source evidence；final 44M45M 必须重取。路径前缀均为
`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/`。

| # | 17-file core | 当前 SHA-256 |
|---:|---|---|
| 1 | `CloudGameClient.java` | `6C6E3610AD37163C22D8EDC0A34CA4F45C458264B3A61F9CF27DF673E904E9CE` |
| 2 | `CloudSummonSkillWholePassCapability.java` | `3EE97295B2D50B052E56347E420EB04C35BEA5472B327AEC48E02FB015E20A6D` |
| 3 | `CloudTaskExclusiveInteractionAuthority.java` | `91349697592CD33CF32870E5B6732A21470480C2CE6EF16BCA90A3444297ABCC` |
| 4 | `CloudTaskExclusiveInteractionState.java` | `2F85CD0D30F0C6F350DBDD353F2DC1359B50BEADC61FC4331C3A86394C3AC93C` |
| 5 | `CloudTaskRetainedActionState.java` | `DFE18415D3B5D539B499B7BA574BF940F4F69BE4478761CDF47AE302928FFCCA` |
| 6 | `CloudTaskRunActionLedger.java` | `90B980BDC4C9147F69DE5F93EE6EE6AAB7356CD02516A767CACAE93470BD63B1` |
| 7 | `CloudTaskRunAuthorityAssembly.java` | `A22AF1D212B0A1734FED546D44B413FDE2226FFE4ED9E88865EA8666A185D0E1` |
| 8 | `CloudTaskRunCommandExecutor.java` | `14E52F56A84C4B2A2D3C16E25C95A025211DF1228A0B7DE1C8FD47E7F4F6303D` |
| 9 | `CloudTaskRunCurrentContextSlot.java` | `D25A77F29A94223BC0EB3E8A8727BE9CED0FEDE5159B8CB1CE526CA1DA68E9EA` |
| 10 | `CloudTaskRunExecutionGate.java` | `0C5BC991665A869D3515701EEADA15C500C66AA4AE5AB81FF7C029AD353E0459` |
| 11 | `CloudTaskRunRetainedLifecycleActivationAdapter.java` | `F9820B6678A22E67E752EF09F89BC210AA20C9EC2D2E11A854DE746D53D7D9DD` |
| 12 | `CloudTaskServiceExecutionContext.java` | `A66E156FDE85BCF58FAB4330CCAFB2774A9F78214F17C63B5BD698D1D90F2599` |
| 13 | `CloudTaskServicePort.java` | `CC8E8256853BC1310D5D92F830267542FE0ECB2E733D3BB9BAA6C75B86BED3C9` |
| 14 | `RemoteFinalConsumptionCoordinator.java` | `BDA3DCE275CAB871CC073AB1D3409EE10B42866AD377963EAF34FC6EEDF3C6CA` |
| 15 | `RemoteGameClientPort.java` | `34EF38528F7E0F3F1690688296515D0775B15D7081F394CEC7DB549CEA314875` |
| 16 | `RemoteGameCommandBroker.java` | `5B8D6B1C8B7F36DB7F9B707F44E839689487556C2189CE4184527D442670F90A` |
| 17 | `TaskTransactionAction.java` | `6B6B26EA1FE09092733B0F0C9BDED928F953C87217F82DB29B09DE79377CF5D2` |

当前还有两个条件 compile-closure leaf：

- `CloudTaskTurnAuthority.java`，SHA-256
  `AED690199C8FE3F5C9EE9094EBCCBF5BEC5C6CF762E22692B617D0EA58BDEF1F`，仍引用 current slot。
- `LeaderPrecheckAction.java`，SHA-256
  `097AF2C0ADAE49E9065B4100C919023A09FED0F9ECD5D1F299B215668ED9C42A`，仍引用 retained state。

若 final graph 仍保持这些边，44A card-level cohort 是：完整 17 core + 仍有边的两个 conditional leaf + 父级最终归
44A 的 38M state rows + final manifest明确列出的 companion rows。三个本轮候选的当前附加 leaf 是：

1. paused target 1 文件；其 consumer 已在 17 core。
2. Player governor + Player owner 2 文件。
3. CommonBox governor + CommonBox properties 2 文件。

这是当前最大 `17 + 2 conditional + 5 candidate/companion` 的条件集合，不是固定的 `24-file DELETE` 结论；
GameContext/LeftTop 路线、38A/39 rewires、40B/C consumer 与 45A 后重算都可能改变 final 行。

### 6.2 真实 DAG 顺序

```text
34C/35/36/37 source stable
  -> 38A final context/compatibility boundary
  -> 38B1/B2/B3/B4 + 38M parent classification freeze
  -> 38C 只写最终 KEEP_REWIRE rows；本轮三个 DELETE candidates 零写入
  -> 39 final active facade/context rewire，重算 old SCC
  -> 40B -> 40C -> 40D -> 41
  -> 44M45M 基于 final source 冻结逐文件 ref/SHA/cohort
  -> 45A 断 routes，候选和 17 core 零写入
  -> post-45A 重算
  -> 44A exact authority/state/companion cohort + guard + compile
  -> 45B 只接 post-44A residual
```

### 6.3 测试 ownership

| 阶段 | test owner | 本轮三个候选的边界 |
|---|---|---|
| 38M | `ZERO` | source/ref/SHA 分类证据；不运行/新增 JUnit |
| 38C | 每个父级冻结的 `KEEP_REWIRE` 独立 `*TurnStateTest` | 三个 DELETE candidates 不占该 test write set |
| active Player/CommonBox | `PlayerStateTurnContractTest` / `CommonBoxTurnContractTest` | 保持业务 owner合同；两者当前对候选/companion零 ref，44A 只读 |
| 40B | `CloudTurnTaskRuntimeContractTest` / `CloudTurnTaskFactoryAllowlistTest` | 必须证明 runtime/factory 不构造 old governor/paused authority；不得改成 candidate deletion test |
| 45A | `OldRemoteRouteRemovalGuardTest` | 只证 route sever，并要求 17 core保持；不得删除候选 |
| 44A | `OldAuthorityRemovalGuardTest` | 唯一 candidate deletion guard；逐 exact path/symbol/ref，不用 wildcard |

当前 `OldAuthorityRemovalGuardTest`、40B 五个 production 文件及两个 40B tests均不存在。44A card-level test-compile
还受既有 `TaskExecutionContextTurnContractTest`、`SummonSkillTurnContractTest` 与 LeftTop reflection fixture 的前置
ownership约束；只新增 guard不能掩盖这些既有测试源码依赖。

## 7. 跨卡冲突矩阵

| Card | 与三个 candidate/companion 的物理交集 | 必须保持的边界 | 冲突信号 |
|---|---:|---|---|
| 38B1/B2/B3/B4 | 0 | 只处理 Bag、ReturnItem、startup、artifact；不得并入 authority-bound remote state | 任一 B 卡 import/construct candidate 或 companion |
| 39 | candidate leaf 为 0；当前 17 core 为 3 文件 | 只收 active facade，old SCC保持可编译到 44A；之后重算 | 39 为通过零引用而删/改候选，或留下半拆 SCC |
| 40B | 0；只创建 5+2 文件 | current runtime + last ID/ack；active Service owner；零 old authority ref | factory/runtime引用 candidate、owner/properties、session/revision/ledger |
| 45A | 0；对 17 core 也是 0/17 | 只改 Server、删 routes/endpoint、建 route guard；保留 target/SCC字节 | 45A 删除候选、companion、17 core 或 active Service |
| 44A | 取决于 parent final manifest | 处理完整 final cohort；一个 guard/compile 门 | companion漏行、hash漂移、split SCC或 active owner被吸入 |

## 8. `STOP-WORK` 条件

出现任一项即停止对应 implementation/delete 动作，回到父级刷新分类、DAG、write set 或 manifest；本 helper不替父级
消解：

1. 38M 固定分类文件仍不存在，或父级尚未逐项写明三个 target 的候选去向和 companion owner。
2. 44M45M final manifest仍不存在，或 target/companion/direct consumer 的 path、ref、byte size、SHA 与 frozen 行不一致。
3. 三个 target 中任一获得新的 active turn/runtime/Service/test direct consumer；必须重开分类，而不是继续按旧候选删。
4. 38C 试图修改任一 candidate/companion，或以“顺手兼容”把它接回 turn-native path。
5. 38A 删除 `TaskExecutionContext` old API 后，Player governor、CommonBox active Service或 17 core不能继续 source-compile，
   但父级尚未冻结兼容/原子 cohort方案。
6. 任一 38B 卡试图吸收 paused、Player governor或 CommonBox governor，或引入第二 state map/registry/permit。
7. 39 需要改 candidate/companion 才能通过，或修改 17 core 三文件后没有重算完整 SCC和外部入边。
8. 40B factory/runtime 引用 candidate、`CloudPlayerStateStateOwner`、`CloudCommonBoxProperties`、old run scope、paused
   capability或 old ledger/final-consumption；或者新增 session、owner、durable workflow/history、TTL、自动 Task/action/
   cleanup retry。
9. 40B/父级在没有明确业务裁决时改变 active `PlayerStateService` 的跨 Task/new-start保留语义，或复制/改变
   `CommonBoxService` 既有 30 秒 TTL、consume/failure语义。
10. 45A 尚未真实完成，或 45A patch触及 target、companion、17 core、active Service、新 turn/template ingress。
11. post-45A 图仍是 17-file SCC，却计划先删 paused target、拆 ledger/broker、把 final coordinator留 45B，或并发
    两个 Cloud deletion writer。
12. Player owner或 CommonBox properties 未进入 final manifest，却有人因“零引用 companion”在 44A 顺手删除。
13. `OldAuthorityRemovalGuardTest` 使用 `remote/**`、`Remote*`、目录不存在或宽泛字符串代替 exact path/symbol；
    或 existing test-compile/reflection owner尚未闭合。
14. 44A 需要修改 active `PlayerStateService`、`CommonBoxService`、`BotProperties`、业务 Task/Service 或 696
    phase/input/retry/fallback 才能编译。
15. 任一受保护 dirty/untracked 内容出现无法归属的并发变化，或有人要求 clean/reset/stash/checkout/add/commit等
    Git mutation来整理删除工作树。

## 9. 给父级的非绑定冻结建议

若父级后续维持三个候选方向，建议在正式分类/manifest 中逐项写清：

1. paused 行：38C 零写入；44A target 与 final 17-core cohort同门；paused semantic wire留 45B重扫。
2. Player 行：38C 零写入；44A 必须同时列 governor、owner和 governor同文件 capacity symbol；active
   `PlayerStateService` 明确保留。
3. CommonBox 行：38C 零写入；44A 必须同时列 governor、properties；active `CommonBoxService`、
   `BotProperties` 与现有 30 秒 TTL明确保留。
4. 38A 先冻结中间 compile boundary，尤其 Player governor old context calls与 CommonBoxService identity-epoch calls；
   不把问题延期成 44A 才发现的缺符号。
5. 39 后、40B/C 后、45A 后各重扫一次 source/test refs与 SCC；任何一次漂移都刷新 SHA/cohort。
6. 40B 明文列 candidate/companion old symbols零引用，并保持无 durable workflow/session/ledger、无新 TTL、无自动
   retry。
7. 44A guard逐 path/symbol验证删除行，同时逐 path保护 active business owners和新 HTTPS turn ingress。

这些建议是父级冻结清单，不是本 helper 对 `KEEP/DELETE` 的决定。

## 10. 本 helper 操作确认

- 只新增本报告。
- 未修改 Java、test、计划、CR271、`ACTIVE_WORK.md`、dashboard 或其它报告。
- 未还原、移动、清理、暂存、提交、切换或覆盖两仓既有 dirty/untracked 内容。
- 未运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input。
- 本报告结束后本 Internal helper 保持在线，不持有任何 Java production/test 写集。

PRECHECK_COMPLETE

<!-- TRUE_EOF: CR271 TURN-38M DELETE CANDIDATES COHORT CLOSURE HELPER R1 PRECHECK_COMPLETE -->
