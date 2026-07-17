# TURN-33 readiness preflight helper（CR271）

## 角色与审计边界

- 日期：2026-07-16。
- 角色：CR271 非绑定 readiness helper；不是 implementation Worker，不承担 reviewer 或父级裁决职责。
- 本轮只做 TURN-33 前置静态审计。未修改 Java、测试、计划、固定卡片或两仓其它文件；未运行 Maven、JUnit、compile/package、runtime、application、server、Task、UI、capture 或 input；未执行 Git mutation。
- 已核对：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部、权威计划第 14-19 节及其对旧章节的覆盖关系、TURN-15/18/26 固定报告、`docs/业务逻辑.md` 的维护优先级与召唤兽三技能规则、Cloud 当前四个指定类、现有 turn-native API，以及提交 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `SummonSkillService` / `TaskMaintenanceService`。

## 非绑定预检结论

TURN-33 的真实实现边界已经可以收敛，但当前还不能生成实施 claim：权威计划第 17 节给出的 `startDependsOn=S=15+18+26` 中，TURN-26 仍处于活动实现状态；TURN-15/18 虽然固定报告已记录源码与测试源码复核通过，但 named test 与适用 build 仍待 stable-writer cohort。父级可在 TURN-26 源码/测试源码门通过、owner 释放后立即冻结 TURN-33，最终卡片门仍须保留 TURN-15/18/26 的待运行证据。

父级冻结前还须把以下三项写成明文，不能交给 Worker 自选：

1. `696a12b0` 与 2026-07-02 用户确认的静态格子规则是组合基线：静态规则只覆盖旧 6/8 hover 判定、旧第 4/7 格起扫和 locked/empty/occupied 前置识别；其余整轮顺序、删除、终极角、超时、维护状态与重试语义继续按 `696a12b0`。
2. “whole pass”在新架构中是一次同步 Cloud 业务调用，内部按观察结果发多个 closed turn；它不是一个跨多次 HTTPS 往返的本地 input session。若仍要求像 `696a12b0` 一样在整个自适应整轮中持有不可插队的本地 exclusive callback，当前协议和三文件生产写集无法表达，父级必须另行扩卡，Worker 不得重建 acquire/release、session、owner、ledger 或 TTL。
3. “旧 whole-pass / exclusive authority 零引用”必须冻结为 Summon 活动调用链与 Summon 专属可执行桥的零引用门，而不能写成全 Cloud 源码的字面零命中。当前 `CloudTaskServicePort`、command executor、broker、retained state、ledger、DTO/enum 等多个计划外文件仍编译依赖旧类型；三文件写集不可能同时清除这些全仓引用。

## 真实 runnable caller

当前 Cloud 的真实生产调用链不是旧 capability facade，而是三个 Task 入口最终汇合到同一个维护服务 public path：

```text
AutoBattleTask::maybeRunIdleMaintenance
WubeiTask::maybeRunLeaderPathingSummonMaintenance
XiuluoTaskV2::runLeaderPathingSummonSkillMaintenance
    -> TaskMaintenanceService::runOpportunisticMaintenance
    -> TaskMaintenanceService::maybeCleanSummonSkill
    -> SummonSkillService::cleanSummonSkillsOnce(SummonSkillCleanupRequest)
```

当前源码位置与调用条件：

- `AutoBattleTask.java:182-228`：idle maintenance，`cleanSummonSkill=true`；是否要求 team-round/local-support gate 由当前窗口角色和队伍会话决定。
- `WubeiTask.java:1099-1145`：队长 pathing maintenance；外层已有 `taskTurnCoordinator.tryRun(...)`，并固定 `oneSummonSkillPerTeamRound=true`、`maxSummonSkillCleanersPerTeamRound=1`、team window open。
- `XiuluoTaskV2.java:3810-3846`：队长 route-owned movement 窗口，`oneSummonSkillPerTeamRound=true`、team window open；本方法内没有额外 acquire/release。
- `TaskMaintenanceService.java:624-796`：真实 due/config/free-state/team-round claim、窗口状态、缓存、结果落账和 retry-later owner；实际调用在 `:755`。

因此 TURN-33 不应以测试伪 caller、旧 `CloudTaskServicePort#summonSkillWholePass()` 或 debug/public helper 作为 runnable caller。`TaskMaintenanceService` 是本卡只读的真实 caller，后续由 TURN-34B 一文件独占收口；本卡必须保持其 public 请求/结果合同可直接运行。

## 建议 exact write set

### Production

严格采用权威计划第 17.2 节的后置覆盖写集：

- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java`
- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`

明确只读：

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- 全部 Task/caller、DHXY、双端 protocol、`TurnGameClient`、action factory、command port、routes/config/POM、资源文件及其它 remote legacy 文件。
- `SummonSkillStaticSlotPolicy.java`、`SummonSkillTailBoundaryScanner.java`、`CloudTemplateAssets.java`、`PackagedTemplateAssets.java` 可作为现有只读 collaborator，不得为了 TURN-33 再复制 policy、matcher 或 classpath loader。

`SummonSkillService` 可通过现有 Spring bean `CloudTemplateAssets` 读取 allowlisted `images/template/...` 资源；仓内未发现 production 对 `new SummonSkillService(...)` 的手工装配点，因此无需扩写 `CloudBrainServer`/configuration。Worker 仍须在交付时用当前源码重新确认这一点。

### Test

权威计划第 19 节只允许 create/modify：

- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java`

测试画像固定为 `IMG+LX`，并叠加计划默认 `BC4+BASE`；不得另建 mapper test、source guard、fixture helper 或复制 production policy。固定报告由父级另行指定，不能把本 helper 报告变成 Worker 的实施卡。

## 与活动 TURN-23 / TURN-26 的写集互斥

### TURN-23 活动写集

- `PlayerStateService.java`
- `ClientIdentityService.java`（仅 integration 必要时）
- `playerstate/CloudPlayerStateFirstAidPort.java`
- `playerstate/CloudPlayerStateIncenseStatusPort.java`
- `service/PlayerStateTurnContractTest.java`
- TURN-23 固定报告 true EOF

与建议 TURN-33 三个 production 文件及一个 named test 文件的文件级交集为零。TURN-23 当前 Repair #1 已回到源码/测试源码复核通过、build pending；TURN-33 不得触碰其 open-main-bag caller-session 兼容逻辑或借机复写 player-state ports。

### TURN-26 活动写集

- `DialogService.java`
- `remote/CloudDialogOptionOcrImagePort.java`
- `remote/CloudDialogOptionOcrWordsPort.java`
- `remote/CloudDialogWhiteStoryTemplatePort.java`
- `LocalOcrClient.java`（仅冻结的 typed API visibility + JavaDoc）
- `service/DialogOptionTurnContractTest.java`
- TURN-26 固定报告 true EOF

文件级交集同样为零，但存在真实功能依赖：`SummonSkillService` 在整轮内最多三次调用 `DialogService` 处理维护广播，TURN-33 必须消费 TURN-26 最终 exact-window/terminal API，不能复制 OCR、模板判定或另发第二 capture。故应等 TURN-26 源码/测试源码门通过且 owner 释放后再 claim TURN-33。

### 并发保护结论

- TURN-33 不写 `TaskMaintenanceService`，与未来 TURN-34B 保持互斥。
- TURN-33 不写 TURN-23/26 任一 production/test/report 文件。
- 只要 TURN-23/26 或其它 Java writer 仍活动，TURN-33 Worker 不运行 Maven/JUnit/compile；父级在 stable-writer cohort 统一执行 named test 与适用 Cloud compile/build。

## 组合业务基线：whole pass / start index / retry

### Whole-pass 保留项

按 `696a12b0` 保留：

- `cleanSummonSkillsOnce(request)` 是一次同步完整维护调用，整轮 deadline=`40_000ms`。
- 顺序保持：`Alt+O -> wait 900ms -> attribute anchor 0.85 -> 必要时拖窗并重新定位 anchor -> 点击技能页 -> wait 800ms -> 扫尾格 -> 需要时 hover 分类 -> 普通技能删除/确认 -> 终极角检查 -> lightweight cleanup`。
- anchor 仅在距窗口右边不足 `337px` 时拖动；拖动后只做基线已有的重新定位，不增加新验证轮次。
- 每轮最多删除 `5` 个普通技能；整轮内维护广播最多处理 `3` 次。
- 普通/高级/终极 tooltip 分类、普通技能删除、确认按钮匹配/点击、KEEP 不删、post-delete 分支、locked tail boundary、终极角“点击可”分支及其结果字段保持基线顺序和含义。
- 只有到达基线安全停止点才返回 success；timeout、interruption、窗口/图片/模板/匹配机制异常、未知格状态或删除确认失败都不能刷新完整成功冷却。
- 结束后的 lightweight cleanup 继续复用 TURN-15 的 `CloudUiCleanerPort`，整轮中的 dialog 继续复用 TURN-26 的 `DialogService`；不得在 Summon 内复制两者。

新 turn 可把“确定性 input + wait + 紧随其后的观察 capture”放进同一个 closed action，但不得跨越 Cloud 必须先看图片才能选择下一步的分支。每个观察驱动的后续动作都是新的显式 business invocation，而不是 transport retry。

### Start-index 的后置用户确认规则

`docs/业务逻辑.md:170-211` 是 `696a12b0` 之后用户明确确认的窄范围替代规则，应在父级 brief 中写成已批准组合基线：

1. 技能页打开后，每次先对当前 exact window 相对 ROI `(505,508)-(532,555)` 匹配 `images/template/zhaohuanshou/if8.png`；健康且明确命中为 8 格，健康且明确 miss 为 6 格。模板缺失、图片不可读、window/ROI 非法或 matcher 异常是 UNKNOWN，不能把机制失败当 6 格。
2. 使用校准固定格坐标和 `status_sealed1.png`、`status_unobtained1.png`、`status_inactive1.png` 静态分类。健康条件下三张均 miss 是 OCCUPIED，不是 UNKNOWN。
3. 从末格向前：LOCKED 跳过；先遇到尾部连续 EMPTY 时返回该连续段最前一格；否则返回从尾部遇到的首个 OCCUPIED。示例 `O,O,O,E,E,E,L,L` 返回零基第 `3`（用户显示第 4 格）。全部 LOCKED 或无可行动格返回无动作成功；任一 UNKNOWN 失败闭合。
4. 只有 OCCUPIED 且确需判断可删性时才 hover。静态 EMPTY 不做 tooltip hover；LOCKED 不做 hover。
5. 因此旧 `getTailCheckStartIndex(6)=3`、`getTailCheckStartIndex(8)=6` 和 `resolveStartIndex(request,...)` 不再决定本轮初始扫描点。`TaskMaintenanceService` 传入的 `expectedSkillCount/startSlotIndex` 不得绕过本轮 live `if8` 与静态倒扫；现有 request/result 字段仍保留，用于布局变化判断、`skipUltimateCornerCheck` 的既有语义以及把本轮 `nextStartIndex` 反馈给只读维护状态。

现有只读 `SummonSkillStaticSlotPolicy` 已表达上述 ROI、静态分类和倒扫算法，但当前 `SummonSkillService` 对它为零引用。父级应明确允许直接复用该 policy；不得在 `SummonSkillService` 再复制第二套静态算法。特别要锁住 caller 语义：只有 caller 已证明模板和 matcher 健康时，`if8` 的 null match 才能解释为 6 格。

### Retry / cooldown 边界

必须区分三类行为：

- **Transport retry：零。** timeout、interruption、HTTP/command uncertain、duplicate、correlation mismatch 均不得自动重发同一 action，不得复用 UUID。
- **整轮内已有业务分支：保留。** 例如拖窗后的 anchor 重新定位、删除后的既有状态检查、最多三次 dialog handling，都是 `696a12b0` 已有顺序；它们不是 transport retry，且每个新 turn 使用新 UUID。
- **跨整轮的维护再调度：继续由只读 `TaskMaintenanceService` 拥有。** success 才更新完整清理时间/窗口缓存；ultimate 已成功但后续失败时只保留既有 ultimate cooldown；UNKNOWN 按现有 retry-after 与 layout-cache invalidation；无状态变化时释放本窗口 team-round claim；最终返回 `SUMMON_SKILL_FAILED_RETRY_LATER`。TURN-33 不得添加、删除或移动这些 TTL/backoff/claim/cooldown，也不得把 UNKNOWN 变成成功事实。

## Exact window / UUID / terminal 合同建议

父级 brief 应把以下内容逐条冻结：

1. **绑定来源。** 每个低层 turn invocation 只使用当前 `TaskExecutionContextHolder` 的 turn-native `TaskExecutionContext#getTurnGameClient()`，再绑定当前 `deviceId/windowId`；禁止 `getRemoteGameClient()`、标题搜索或 Cloud 进程内 DHXY tracker/window holder。
2. **UUID 前置门。** 每次真正发 action 前读取一次 latest `TurnWindowMetadata`，在创建 UUID/command 之前校验 exact `deviceId/windowId`、初始 `nativeHandle`、初始 `processId`，并执行 stop checkpoint。metadata 缺失、窗口错配、HWND/process 漂移或已 stop 时 action count=`0`、UUID count=`0`。
3. **坐标空间。** 所有 panel ROI、slot、hover、drag、delete/confirm/ultimate 点均以该次 latest `windowRect.left/top` 加未缩放相对像素得到 `SCREEN_ABSOLUTE_PX`；禁止 `(0,0)` 假设、scale/clamp、跨窗坐标复用。Cloud 负责在基线随机半径内选出最终 exact 点，payload 携带最终点；DHXY 不再二次随机或重算业务坐标。
4. **一 action 一 UUID。** `latestWindowMetadata()` 本身零 action/UUID；每次 `TurnGameClient.capture/execute/localService` 由 production client 内部生成恰好一枚新 UUID。一个 action 可含有序 INPUT/WAIT/CAPTURE steps，但同一 UUID 不得用于第二次 HTTP submission。观察结果触发的下一项业务动作使用新 action、新 UUID。
5. **结果关联。** 只接受 exact actionId、device/window、step 顺序/类型一致的 terminal；CAPTURE 还必须校验 frame metadata、requested absolute ROI、raw `image/png`、SHA、width/height 与可解码像素。任何错窗、错 action、错 step、错 ROI、hash/dimension/decode 异常均 fail closed。
6. **Terminal 映射。** confirmed `COMPLETED` 才进入 Cloud 图像/业务判定；confirmed mechanical `FAILED` 返回 cleanup failure 且不刷新完整冷却；confirmed `STOPPED` 走任务停止语义；`DUPLICATE_OR_UNCERTAIN`、timeout/interruption/transport unknown 或 correlation mismatch 返回不确定失败，零自动 retry。不得把负 runner/ready 信号、known miss 或 uncertain 解释为 EMPTY、6 格、可删、删除成功或整轮成功。
7. **Whole-pass 返回。** `SummonSkillCleanupResult` 保持原 public shape、零基 `nextStartIndex`、observed statuses、ultimate clicked/succeeded、inspected/deleted counts。已执行动作后的不确定结果必须保留可确认的状态变化证据，但不能由服务自行刷新 maintenance cooldown；落账仍由真实 caller 决定。

## 旧 whole-pass / exclusive authority 零引用门

### 当前事实

当前活动路径仍是：

```text
SummonSkillService
  -> TaskExecutionContext#getRemoteGameClient()
  -> CloudTaskServicePort#summonSkillWholePass()
  -> CloudSummonSkillWholePassCapability#execute(...)
  -> CloudTaskExclusiveInteractionAuthority#executeSummonSkillWholePass(...)
  -> retained action / generated session UUID / owner / ledger / quota / late outcome
  -> executeSummonSkillWholePass remote command
```

这正是 TURN-33 必须从真实 Summon 路径移除的旧 bridge。

### 建议可执行 gate

1. `SummonSkillService.java` 对以下标识必须为零命中：

```text
getRemoteGameClient
summonSkillWholePass
CloudSummonSkillWholePassCapability
CloudTaskExclusiveInteractionAuthority
executeSummonSkillWholePass
SUMMON_SKILL_WHOLE_PASS
EXCLUSIVE_INTERACTION_CONTROL
ACQUIRE / RELEASE / ABORT（exclusive control 语义）
GameClientTracker / CoordinateHelper / InputSequences / InputProvider
WindowScopedTempPath / WindowTaskContextHolder
```

允许保留 Cloud-side `ImageFinder`、`CloudTemplateAssets` 和 pure CPU policy；它们不属于本地 capture/input authority。

2. `CloudTaskExclusiveInteractionAuthority.java` 的 Summon 专属 `executeSummonSkillWholePass`、late outcome、cleanup projection 和 Summon retained invocation 分支应从可执行 authority 中移除；generic authority 若仍被其它旧卡调用，不得在 TURN-33 冒充已全局删除。

3. `CloudSummonSkillWholePassCapability.java` 不得再委托 authority 或发旧 command。若计划外编译依赖要求暂留 nested DTO/构造签名，应成为 fail-closed compatibility tombstone：调用时零 command/UUID、不可进入 exclusive authority；父级需在 brief 中写明这一兼容范围。Worker 不能自行删掉导致 `CloudTaskServicePort` 等计划外文件失编，也不能静默保留可运行旧桥。

4. named test/source gate 应证明 production `SummonSkillService` 的 reachable path 只进入 bound `TurnGameClient`，且不会构造 acquire/release/session/owner/ledger/TTL。不要用全仓 `rg` 零命中作为 TURN-33 完成条件。

### 为什么不能冻结“全仓字面零引用”

当前只读扫描确认以下计划外文件仍引用旧类型/operation：`CloudTaskServicePort`、`CloudTaskRunCommandExecutor`、`RemoteGameClientPort`、`RemoteGameCommandBroker`、`CloudTaskRetainedActionState`、`CloudTaskRunActionLedger`、`RemoteCommandOutcomeEnvelope`、`RemoteOperation`、request/outcome DTO 等。若父级要求全仓删除 `SUMMON_SKILL_WHOLE_PASS` 或整个 `CloudTaskExclusiveInteractionAuthority`，必须先扩大生产/测试写集并重新做并发排程；这不再是权威计划第 17.2 节的 TURN-33。

## Named-test 建议验收面

唯一 `SummonSkillTurnContractTest` 应直接实例化 production `SummonSkillService`、现有 pure policy/assets 和 production `TurnGameClient` path，不能只测复制 mapper。至少覆盖：

- 三个真实 caller 所使用的 `cleanSummonSkillsOnce(request)` public shape，且 `TaskMaintenanceService` 源码/API 不改。
- 非零/负 monitor origin、exact window、HWND/process drift、metadata missing/mismatch/stop 的 UUID-before-preflight 门。
- `if8` hit=8、healthy miss=6、模板/图片/matcher/ROI failure=UNKNOWN；固定 6/8 坐标和 sealed/unobtained/inactive/occupied 分类。
- 倒扫 start index：尾 LOCKED、连续 EMPTY、尾 OCCUPIED、全 LOCKED、任一 UNKNOWN；锁住 `O,O,O,E,E,E,L,L -> 3`（零基）。断言旧 cached 第 4/7 起点不能绕过 live static scan。
- 普通/高级/终极 hover 分类、KEEP 不删、普通删除/确认、post-delete、locked boundary、ultimate corner、最多 5 次删除、最多 3 次 dialog、40 秒边界和 lightweight cleanup 顺序。
- 每个 fixture 的 exact action/UUID 数；只有业务观察分支可产生下一枚新 UUID，同一 action 零重发，transport uncertainty 零 retry。
- `COMPLETED/FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN`、错 action/window/step/frame/ROI/SHA/dimension/decode 的精确映射和 cooldown fail-closed。
- Summon 活动路径对旧 whole-pass/exclusive bridge 的 scoped 零引用；旧 compatibility surface 不得产生 command/UUID。

测试与 build 只由父级在 writers 稳定后按计划执行；本 helper 未创建或运行它们。

## 尚未满足点

1. TURN-26 当前仍在实现，`DialogService` 的最终 source/test-source 合同尚未成为 TURN-33 可消费的稳定依赖。
2. TURN-15/18 固定报告均仍保留 named test / build pending；它们不妨碍父级准备 brief，但影响 TURN-33 最终卡片门。
3. 父级尚未把“组合基线”写入真实 TURN-33 固定卡；若只写“完全按 `696a12b0`”，Worker 可能错误恢复旧 hover 6/8 和第 4/7 起点。
4. 父级尚未界定 scoped 零引用与全仓 legacy cleanup 的区别；按当前三文件写集，全仓字面零命中不可实现。
5. 父级尚未明示跨多个 HTTPS action 是否接受“每 action exact-window 原子 + 每步重验”替代旧 pass-wide local exclusive callback。若不接受，当前 turn protocol 缺少合法表达，需先另设 foundation/caller card。
6. `SummonSkillStaticSlotPolicy` 已存在但未接入；父级需明确它是本卡只读复用件，并锁住 healthy miss 才能映射 6 格，避免 null/error 合并。

## 建议 parent-frozen brief

父级在 TURN-26 通过后可按以下最小文字冻结真实卡：

1. **状态/依赖：** TURN-33 implementation card；startDependsOn=`TURN-15+TURN-18+TURN-26` 的 source gates，最终门另含三卡待完成 build 证据、本卡 source/test-source review、`SummonSkillTurnContractTest` 与适用 Cloud compile/build。
2. **唯一真实入口：** `TaskMaintenanceService::maybeCleanSummonSkill -> SummonSkillService::cleanSummonSkillsOnce(request)`；三个真实 Task callers 如本报告所列；`TaskMaintenanceService` 与 callers 全部只读。
3. **Exact write set：** 仅三个 production 文件和一个 named test 文件；现有 static policy/tail scanner/template assets 只读复用；不写 DHXY/protocol/config/POM/resources/其它 remote legacy。
4. **组合基线：** `696a12b0` 保留 whole-pass/delete/ultimate/dialog/cleanup/timeout/result/maintenance retry；`docs/业务逻辑.md:170-211` 明确替代旧 layout hover 与旧 start index。固定写入：`无已批准业务差异；按 696a12b0 与用户确认的静态格子规则等价迁移`。
5. **Whole-pass 定义：** 一次同步 Cloud 业务调用、最多 40 秒；观察驱动的多个 closed turn，每个 exact action 新 UUID、零 transport retry。禁止旧 whole-pass remote command、exclusive acquire/release、session/owner/ledger/TTL。
6. **Exact turn 合同：** 每 action UUID 前校验 device/window/HWND/process/stop；所有坐标为 latest rect + 未缩放相对像素；严格 terminal/correlation/raw-PNG；uncertain fail closed，confirmed stop 传播，known mechanical failure 不刷新成功冷却。
7. **Scoped legacy gate：** Summon production reachable path 对旧 capability/authority/operation 为零；authority 移除 Summon 专属可执行分支；compatibility tombstone 若因计划外编译依赖暂留必须零 command/UUID。全仓 legacy enum/DTO 清理不在本卡。
8. **Exclusive 口径：** 父级明确选择“Cloud whole-pass + 每 action exact-window 原子”作为本卡授权的架构迁移边界；若要求 pass-wide 不可插队，先暂停 claim 并扩卡，不允许 Worker自行恢复旧 session。
9. **并发禁令：** 不触碰 TURN-23/26 写集及 dirty/untracked，不运行 Maven/JUnit/compile/runtime/UI/input，不做 Git mutation；稳定后由父级执行本卡 named test 和 Cloud build。

PRECHECK ONLY / NOT APPROVAL

## Parent independent precheck review - 2026-07-16 02:00 EDT

- 父级已独立复读当前 `SummonSkillService.java:160-225,263-430,968-993`、真实
  `TaskMaintenanceService.java:624-775`、`CloudSummonSkillWholePassCapability`、
  `CloudTaskExclusiveInteractionAuthority.java:792-913`，并与 DHXY 基线
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `SummonSkillService` 及
  `docs/业务逻辑.md:170-211` 逐项对照。helper 对真实 caller、旧 retained whole-pass、当前 4/7 起点和
  三文件可执行清理边界的证据成立；本结论是父级预检裁决，不是 source review 或 implementation approval。
- 父级冻结方向：组合基线必须使用 `696a12b0` 的删除/确认/终极角/dialog/cleanup/40s/result 顺序，加用户已确认
  的 live `if8` + 静态格子倒扫规则；直接复用只读 `SummonSkillStaticSlotPolicy`，只有模板、图片、ROI 和 matcher
  均健康时 `if8` miss 才是 6 格，机制失败一律 UNKNOWN。
- 用户已明确选择最小 HTTPS JSON turn，并明确禁止 session/owner/ledger/durable workflow，因此 TURN-33 的
  “whole pass”固定为一次同步 Cloud 业务调用在 Cloud 内连续决策多个 closed action；每个 action 独立 exact-window
  绑定并在本地 input queue 内原子执行，每次使用新 UUID，观察驱动的下一步是新业务 action，零 transport retry。
  不恢复跨 action 的 local exclusive session/acquire/release，也不把调度迁移变成新的业务成功/失败条件。
- 精确 production 写集维持 `SummonSkillService.java`、`CloudSummonSkillWholePassCapability.java`、
  `CloudTaskExclusiveInteractionAuthority.java`；`TaskMaintenanceService` 与三个 Task caller 只读。旧 capability 若因
  计划外编译引用必须保留 public shape，只能成为零 command/UUID 的 fail-closed compatibility tombstone；active
  Summon path 必须零旧 whole-pass/exclusive authority。
- 当前状态：`PARENT PRECHECK REVIEWED / WAITING TURN-26 SOURCE GATE / NOT READY`。TURN-15/18 的 source gates
  已通过，named tests/build 留最终 cohort；TURN-26 尚在写，父级不会提前派发 TURN-33。

<!-- TRUE_EOF: TURN-33 parent-precheck-reviewed-not-ready -->
