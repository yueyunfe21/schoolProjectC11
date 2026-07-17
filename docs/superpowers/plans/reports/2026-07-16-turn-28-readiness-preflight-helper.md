# CR271 TURN-28 NpcClick HTTPS turn cutover readiness preflight helper

> 日期：2026-07-16  
> 角色：非绑定 readiness helper；仅向父级 TURN-28 冻结 brief 提供源码证据。  
> 边界：未修改 production/test/主计划/CR271/ACTIVE_WORK/矩阵/dashboard；未运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input；未执行 Git mutation。

## 1. 审计输入与仓库快照

已完整读取：

- `D:/mavenProject/DHXY/AGENTS.md`
- `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`
- `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271 进度
- `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节、TURN-28、exact write set 与 named-test 表
- `D:/mavenProject/DHXY/docs/业务逻辑.md` 五倍/修罗/NPC Click 基线
- `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- DHXY 与 `D:/mavenProject/dhxy-cloud-brain` 当前 NpcClick 整链
- DHXY git `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `NpcClickService.java`

只读快照：

- DHXY：branch `thin-client-design`；工作树原有大量 dirty/untracked，全部保护。
- Cloud：branch `navigation-migration`；工作树原有大量 dirty/untracked，全部保护。
- `696a12b0` NpcClick git blob：`74d9b26b76b84052718d5679529f7ffeb46e3273`。
- Cloud 当前 `NpcClickService.java` blob：`4d5339cc7b4c2836cc5461e911056d75938318b6`；相对基线为 `34 insertions / 2 deletions`，仅增加 pending evidence 的 normalized `sourceTask` 绑定与同源确认检查。
- DHXY 当前 active `NpcClickService.java` blob：`c853ced7c3ac1a74f12b668380afa72952a7f619`，不是 `696a12b0` 等价实现。
- Cloud 权威基线副本：`migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java`，3374 行，blob 与 git 基线一致。
- Cloud 历史 preserved helper shard：`migration-preserved/pre-696a12b0-whole-service-cutover-20260714T1129/src/main/java/com/bot/dhxy/service/NpcClickService.java`，478 行，blob `7574e3c35cb6ba789d0c9f4dd99abdaac54597b6`。

依赖时点按本次父级指令处理：TURN-23 与 TURN-24A 已完成源码层复核；TURN-26 仍视为有写入，父级必须在派发前重新确认其文件稳定且 owner 已退出。本 helper 不据此判断 TURN-28 可进入实施。

## 2. 当前真实激活事实

### 2.1 当前进程可达的 Cloud caller 数量

当前 Cloud server 到 Cloud `NpcClickService` public API 的进程可达 caller 数量是 **0**：

- `CloudBrainServer.java:68-111` 直接构造 `DecisionEngine`、legacy routes 与 `CloudTurnRoutes`；没有构造或注册 `CloudServiceHost`。
- `CloudServiceHost.create(...)` 只在 `host/CloudServiceHost.java:39-65` 声明，main source 无调用点。
- `CloudServiceConfiguration.java:27-34` 只扫描 `com.bot.dhxy.service` 与 `com.yueyunfe.dhxy.cloudbrain.turn.client`，不扫描 `com.bot.dhxy.task`；三大 Task 虽有 prototype component 注解，但当前 host 不发现它们。
- 因此下节是“当前源码中未来 Task/Service 激活后会运行的完整 caller 图”，不是当前 server 已经触发的运行链。真实 Task factory/queue/start 激活属于后续 TURN-40 系列，不在 TURN-28 写集。

### 2.2 Cloud 源码 caller 到四个 public API 的完整清单与时序

Cloud 当前 `NpcClickService` public API：

1. `clickNpcSmart(NpcClickRequest)`：当前行 599。
2. `tryDirectCombatTargetClick(NpcClickRequest)`：当前行 653。
3. `confirmPendingSmartClick(...)`：当前行 2271。
4. `confirmExpectedOptionProof(...)`：当前行 2295，经 `SmartClickEvidenceConfirmationService` 间接调用。

`clickNpcSmart` caller：

| Caller | 源码位置 | 调用前后业务时序 |
|---|---:|---|
| `NavigationService` 张闻转灵兽村 | `NavigationService.java:690` | 先检查已有 route dialog；调用张闻点击；false 只告警，随后仍 checkpoint 并检查/恢复 route dialog。|
| 修罗接任务 NPC 近点路径 | `XiuluoTaskV2.java:1140` | 近点直点；成功进入接任务 dialog phase，失败转小地图导航。|
| 修罗接任务 NPC 导航后 | `XiuluoTaskV2.java:1185` | 导航/距离检查后点击；false 进入既有 accept-click recovery。|
| 修罗维护 NPC | `XiuluoTaskV2.java:1531` | 导航与 UI cleanup 后点击；false 返回 maintenance retry，成功继续维护 Service。|
| 修罗普通目标 | `XiuluoTaskV2.java:2021` | 构造 combat-target request；成功进入确认入战 option，false 进入目标点击恢复。|
| 五倍维护 NPC | `WubeiTask.java:1427` | 维护 hook 的有限 attempt 内，导航成功后点击；false 继续下一 attempt。|
| 五倍接任务 NPC | `WubeiTask.java:1960` | 仅在没有 prepared 结果且本轮未点过时调用；false 不立刻终止，仍等待 runner prepared accept dialog。|
| 五倍普通 combat target | `WubeiTask.java:3399` | 正常 smart click 成功后再点已知 enter-battle dialog；false 返回该正常路径失败。|
| 五环鞋店老板 | `FiveRingTaskV2.java:1205` | 在鞋店购买 flow attempt 内，UI cleanup 后点击；false 继续 attempt；成功再处理购买 option。|
| 五环接任务 NPC | `FiveRingTaskV2.java:2383` | `TaskTransaction` 内调用；boolean 直接映射为继续或可恢复错误，stop 由 transaction 外层单独处理。|

`tryDirectCombatTargetClick` caller：

| Caller | 源码位置 | 调用前后业务时序 |
|---|---:|---|
| 修罗直接战斗兜底 | `XiuluoTaskV2.java:2748` | 仅在看打模板、野怪取消模板与 OCR option recovery 均未解决后调用；combat entered 进入战斗，position refresh 分支回导航，其余走既有 cleanup/retry。|
| 五倍直接战斗兜底 | `WubeiTask.java:3440` | 正常目标点击/已知 option 链失败后调用；combat entered 初始化战斗，position refresh 标记重新寻路。|

`confirmPendingSmartClick` caller：

- 五倍：`WubeiTask.java:733`（priority prepared accept 已消费）、`:2017`（accept dialog 成功）。
- 修罗：`XiuluoTaskV2.java:2045`（正常 enter-battle option）、`:2694`（模板恢复）、`:2724`（OCR 恢复）、`:2786`（direct-combat/radar 确认）、`:2982`（accept option）、`:2995`（known enter-battle）、`:3005`（under-five confirm）、`:3016`（under-five wait）、`:3047`（remembered accept）。
- 五环与 `NavigationService` 当前没有显式 caller。

`confirmExpectedOptionProof` 唯一生产 caller：

- `DialogService.java:1512-1561` 的 `finishRequest` 自动证明链。
- 仅 `GREEN_TEMPLATE_VISIBLE`、`GREEN_TEMPLATE_CLICKED`、`BUSINESS_OPTION_CLICKED`、`OPTION_KEYWORD_CLICKED` 进入证明；从当前 window context 取 proof token，再带 `sourceTask/actionKey/matchedText/token/strength/reason` 调接口。
- Cloud 当前 NpcClick 相对 `696a12b0` 增加了同一 normalized `sourceTask` 门；source 不同或空白时保留 pending 给真正 owner，不得跨任务提交。

## 3. `696a12b0` exact 业务合同

### 3.1 总入口与主顺序

`clickNpcSmart`（基线 `NpcClickService.java:599-633`）：

1. 以 expected-dialog verifier 跑一次完整 pipeline。
2. 成功立即返回。
3. stop/interruption 立即返回，不转成下一业务 fallback。
4. `COMBAT_TARGET` 不执行普通 `Alt+C` retry。
5. 其余目标执行一次 `Alt+C -> 700ms`，再跑第二次完整 pipeline。

单次 pipeline（基线 `:778-933`）并非对所有请求机械执行同一六格序列，exact 分支如下：

1. 非 direct-combat、非五倍、非 combat target：先 pre-click dialog gate，再提前尝试 learned memory。
2. 普通模式只做一次 `Alt+4 -> 400ms` name-layer preparation（`:944-954`）；direct-combat 模式不重复 Alt+4。
3. 五倍先尝试 tooltip，再做主 dialog gate。
4. 主 dialog gate：STORY 先交 Dialog 清理再重新检测；仍有任何 dialog 则本轮失败；OPTION 不做通用 cleanup，直接失败。
5. 未提前尝试 memory 的请求再尝试 learned memory。
6. 非五倍再尝试 tooltip。
7. 非 direct-combat 在 tooltip 后再检测一次 dialog。
8. `TENTATIVE` 到此结束，不执行 yellow/formula/Ctrl。
9. 其余依次 yellow target、purple player-anchor formula、Ctrl menu。
10. 任一 strategy verifier 成功立即终止；全失败才返回 false。

`docs/业务逻辑.md:1332-1334` 将外部 FIFO 概括为 `MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES -> END`。父级必须明确把上面的五倍 tooltip-first、early-memory、dialog gates、`TENTATIVE` 截断和 formula 后立即 Ctrl probe 映射进 turn-native 状态机，不能只复制六个标签而改变执行条件。

### 3.2 每类候选、OCR/template 与 verifier 次数

公共 move+click helper（基线 `:176-215`）：

- 首次为一个输入事务：`MOVE -> WAIT 150ms -> LEFT_CLICK hold 150ms -> WAIT firstWaitMs`，随后 verifier 一次。
- 每个业务 retry 为新的同点输入事务：`MOVE -> WAIT 150ms -> LEFT_CLICK hold 150ms -> WAIT 1000ms`，随后 verifier 一次。
- `maxRetries=N` 的上限是 `N+1` 次 click 与 `N+1` 次 verifier；queue/input 失败或 stop 不继续。

候选 exact 计数：

| Strategy | 候选顺序与算法 | 每候选 click / verifier |
|---|---|---|
| learned memory | conservative window-relative remembered point；miss 记录为 SMALL_RING Ctrl origin | 1 / 1；first wait 1200ms；0 retry（`:1870-1913`） |
| tooltip template | 推荐 region 顺序；阈值 `0.82`；36px 去重；每个 region 内按 matcher 返回顺序逐个 hit，命中后仍以 expected dialog 证明 | 每个 dedup hit 1 / 1；first wait 1200ms；0 retry（`:1147-1230`） |
| yellow target | region 顺序；只有 target-not-found 允许扩 ROI；具体 target 点击不成立后不扩大；强匹配含“降魔侍卫”最长公共子串至少 3；点击 OCR 词中心下方 50px | 具体 hit 最多 2 / 2；first wait 800ms；retry=1（`:1938-2030`） |
| purple formula | purple 玩家名 OCR，失败再 purple blob；`UX=20, UY=0, VX=0, VY=-20`；`anchor + logical delta + tune`，Y 再减 50 | 1 / 1；first wait 1500ms；0 retry；miss 后额外 sleep 1500ms（`:2880-3047`） |
| Ctrl menu item | OCR provider-order words；接受 short-name match 或 regex `(?i).*(NPC|IPC|PC|NP).*`；点击 word 坐标 | 每个被接受 menu word 最多 2 / 2；first wait 800ms；retry=1（`:489-577`） |

注意：formula direct click miss 后，基线 `:1035-1049` 会立即对 formula point 跑一次 SMALL_RING Ctrl probe；若仍失败，pipeline 随后还会进入最终 Ctrl strategy（`:1084-1127`），同一 formula origin 可能再次被包含。父级必须冻结是否保留这次重复探测，不能把“单一 CTRL_CANDIDATES 标签”解释成任意去重。

### 3.3 Ctrl probe exact 行为

offset/profile（基线 `:137-157`）：

- DIRECT，1 点：`(0,0)`。
- SMALL_RING，9 点：`(0,0),(8,-8),(8,0),(0,-8),(-8,0),(0,8),(-8,-8),(-8,8),(8,8)`。
- FULL_RING，17 点：`(0,0),(16,-16),(16,0),(0,-16),(8,-8),(8,0),(0,-8),(16,16),(0,16),(-16,0),(-16,-16),(-8,-8),(-8,0),(-16,16),(-8,8),(8,8),(0,8)`。

生产 smart-click 调用不加 window-center fallback；普通非 combat target 必须先有 formula reference，并把 origins 过滤到 reference 15px 半径内。combat target 不受该 15px 过滤。

每个 probe（基线 `:323-443`）必须保持：

1. probe 前 `TaskCheckpoint` 与 interruption 检查。
2. 一个全局 input exclusive callback。
3. 对 probe 点周围构造 `x +/- 150, y +/- 120` 的 screen-absolute ROI，并 clamp 到当前 1024x768 bound window。
4. capture before。
5. hold Ctrl，wait 80ms，move 到 probe，wait 280ms。
6. capture after；用 `!ImageFinder.isMatch(before, after, 0.05)` 作变化门。
7. active path 跳过旧 `images/template/npc/npc_tag.png` shortcut，使用 yellow wash + OCR/fuzzy name；旧 tag template 只保留 private reference。
8. 命中 menu word 后 move、wait 100ms，再执行 direct click/verifier。
9. `finally` 无条件 release Ctrl，再 wait 100ms。
10. probe 后再次 `TaskCheckpoint` 与 interruption 检查。

### 3.4 verifier、direct-combat 与退出

Dialog verifier（基线 `:274-300`）：每次 verify 恰好一次 `DialogService.handleDialog(verifyExpectedOptionDialog)`；仅 `OPTION_VISIBLE` 或 `GREEN_TEMPLATE_VISIBLE` 为成功。

Combat verifier（基线 `:257-271`）：每次候选 verify 最多调用 `BattleRadarService.checkAndSyncCombatState()` 4 次；每次 miss 后都 sleep 350ms，包括第 4 次 miss，所以上限为 4 次 radar call + 4 次 sleep。

`tryDirectCombatTargetClick`（基线 `:653-708`）：

1. null/stop gate。
2. 检测 flying state：FLYING 执行 `Alt+C -> 700ms`；UNKNOWN 直接跳过；其余继续。
3. 执行一次 `Alt+A -> 350ms`。
4. 用 combat verifier 跑同一 pipeline；不重复 Alt+4，不做 pre-click dialog gate。
5. 成功返回 combat entered；stop 后不右键退出。
6. 非 stop 失败进入右键退出并要求 caller 刷新位置。

退出（基线 `:711-754`）最多 3 次：优先重新找 purple/player anchor，否则窗口中心 `(baseX+512, baseY+424)`；每次一个 `MOVE -> WAIT120 -> RIGHT_CLICK hold120 -> WAIT600`，再检测 direct-combat mode；仍在 mode 时 sleep 300ms 后下一次。3 次仍无法证明退出则抛出异常，不能继续 cleanup/retry。

### 3.5 stop/pause、reference/shadow 与 evidence

- 基线 `shouldStop()`（`:3253-3255`）只检查线程 interruption；普通 strategy 在输入前后、sleep/capture/OCR 关键边界检查。
- Ctrl probe 额外直接使用 `TaskCheckpoint`，由 task execution context 承担 pause/stop；pause/stop 不得变成 business false 后继续下一候选。
- 已提交的闭合 input sequence 先完成；外层在 sequence 后 checkpoint。Ctrl exclusive callback 用 `finally` 保证释放 Ctrl。
- expected-dialog 成功可立即记录；deferred proof 使用 current-window pending key + UUID proof token。显式确认匹配 map/name/mapX/mapY；自动证明匹配 sourceTask、proof token 与 expected option。当前 Cloud 新增的同源 sourceTask 防串提交应保留，除非父级另行冻结差异。
- 权威只读 reference：git `696a12b0` blob及 Cloud `migration-baseline/696a12b0/.../NpcClickService.java`。
- 历史 helper/reference：Cloud `migration-preserved/pre-696a12b0-whole-service-cutover-20260714T1129/.../NpcClickService.java`。
- TURN-28 期间只读：DHXY 当前 `NpcClickService.java`、DHXY `service/npc/**`、Cloud `ImageAlgorithms.java`；不得用 DHXY current legacy queue 或 local mechanics 当作 `696a12b0` 等价证据。

## 4. 当前 active legacy path 与旧 fact/macro/port

### 4.1 DHXY 当前 active NpcClick

- `NpcClickService.java:217-234`：`clickNpcSmart -> clickNpcSmartWithOutcome -> tryClickNpcSmartViaCloud`。
- `:547-615`：捕获固定 1024x768 full-window，将 PNG 放入 `imagePayloadBase64`，附 session/debug UUID、ROI/scan regions、旧 facts；不是 turn multipart raw frame。
- `:632-648`：普通模式 `Alt+4 -> 180ms`，与基线 400ms 不同。
- `:342-490`：旧 `NPC_CLICK_START/NPC_CLICK_POLL` session FIFO；candidate cap=12，WAIT timeout=30s，poll sleep=100ms。
- `:667-688`：普通候选本地事务 `MOVE -> WAIT150 -> CLICK hold150 -> WAIT1500` 后本地 verifier；没有保持各 strategy 不同的 1200/800/1500 与 retry 次数。
- `:695-859`：Ctrl 使用 5 个 `18px` offsets 和固定 `images/calibrate/npc_menu_clean_sample.png`、threshold `0.80`；不是基线 1/9/17 offsets 与 OCR/fuzzy active path。
- `:280-329`：最多 3 次 story event fast-click + restart，是 `696a12b0` 之后的行为。
- `:1101-1175`：direct-combat 先消费 `directCombatNormalFifoUnverified` 与独立 Cloud authorization fact，再 Alt+A，再新 Base64 session；不是 `696a12b0` 的 flying-state gate。

### 4.2 Cloud 当前 active legacy NpcClick

- `CloudBrainServer.java:38-45,79-108` 暴露 `/api/cloud/decision`、`/api/cloud/npc-click-smart/outcome`、旧 remote routes 与 `/turn`，但没有激活 Cloud Service host/Task。
- `DecisionEngine.java:326-327` 路由 `NPC_CLICK_STRATEGY`/`NPC_CLICK_SMART`；`:2632-2672` 由 `NpcClickSmartQueueStore` 处理 START/POLL/直接战斗授权。
- `SmartClickRecognizer.java:141-197` 解码 `imagePayloadBase64`，一次性生产 `MEMORY/TOOLTIP/YELLOW_NAME/PURPLE_FORMULA/CTRL_CANDIDATES/END`。
- `NpcClickSmartOutcomeEndpoint` 与 `DecisionEngine.java:271-288` 处理旧 outcome/memory/session completion。
- 上述是当前实际 legacy path；TURN-28 新 Cloud `NpcClickService` 不得继续调用这些 session/store/outcome 入口。物理删除留给后续零引用删除卡。

### 4.3 旧 fact/macro/port 精确引用

- 旧 decision IDs：DHXY `cloud/decision/CloudDecisionServiceId.java:22-23` 的 `NPC_CLICK_SMART`、`NPC_CLICK_STRATEGY`。
- 旧 Base64/session facts：`NpcClickSmartCloudRequest` 及 DHXY builder `NpcClickService.java:578-615`；direct-combat facts 包括 `directCombatMode`、`directCombatProbeTargetReady`、`directCombatNormalFifoUnverified`、`directCombatArrivalTolerance`、`directCombatScenario`（`NpcClickSmartCloudDecisionService.java:809-816`）。
- Cloud stranded ports：`CloudNpcPreparedPointPort`、`CloudNpcTaskTooltipPort`、`CloudNpcYellowTargetPort`、`CloudNpcPlayerAnchorPort`。main source 搜索只有接口自身，无实现/consumer；也不存在 `CloudNpcCtrlProbePort`。
- 与之配套的 `NpcPreparedPoint*`、`NpcTaskTooltip*`、`NpcYellowTarget*`、`NpcPlayerAnchor*` command/result 目前只是旧接口 cohort。
- DHXY 对应 `RemoteNpc*Macro*Payload` 只有定义；main source 无 handler/codec 消费。
- Cloud `remote/LocalMacroKind.java:4-14` 与 `LocalMacroCommand.java:4-8` 的闭集没有 NPC kind/command。
- DHXY `cloud/remote/RemoteLocalMacroKind.java:7-17` 与 `RemoteLocalMacroCommandPayload.java:4-14` 的闭集也没有 NPC kind/command。
- DHXY `NpcClickPreparedPointLocalMacroMechanics`、`NpcClickTaskTooltipLocalMacroMechanics`、`NpcClickYellowTargetLocalObservationMechanics`、`NpcClickPlayerAnchorLocalObservationMechanics`、`NpcClickCtrlProbeLocalMacroMechanics` 无生产 handler caller；不得在 TURN-28 重新接成 active path。

## 5. 可复用 turn typed 能力与硬缺口

### 5.1 可直接复用且不需要改动的能力

- `TurnGameClient.java:20-25,73-168`：每个 public invocation 解析 exact device/window，创建一个新 UUID，提交一个 command；无 transport retry/cache/business interpretation；`bind(expected)` 在 port 前拒绝错 context。
- `CloudTurnActionFactory`：构造 typed capture/action/local-service payload。
- `TurnCaptureSpec`：ROI 是 unscaled screen-absolute；null 表示 full bound window。
- DHXY `TurnCaptureStepExecutor.java:128-169`：从当前 exact `windowRect` 捕获 full window 或指定 absolute ROI，原像素 raw PNG。
- `TurnFrameMetadata`：purpose/contentType/SHA/width/height/region/sourceStepIndex 与 raw PNG 必须同在且一致。
- `TurnInvocationResult.java:41-53,65-106`：校验 actionId、device/window、frame metadata/raw pair，以及 completed/failed outcome 的 step count/index/type exact correlation。
- `LocalTurnActionExecutor.java:65-160` + `TurnInputStepExecutor.java:100-146`：`MOVE -> WAIT -> CLICK` 的 maximal mouse fragment 进入同一个 `InputActionQueue` request；click 后 trailing wait 留在 queue 外但仍属于同一 action UUID。
- TURN-24A `BattleRadarService.checkAndSyncCombatState()` 已使用 turn capture，可作为 combat verifier 业务 collaborator；保留基线每 verifier 最多 4 次调用。
- TURN-26 完成且文件稳定后，可只读复用 `LocalOcrClient.readWords/OcrResult/OcrWord` 及 Cloud dialog image/word/template typed ports；TURN-28 不得修改这些文件，也不得复制第二 OCR client。
- 紫名必须只调用 `ImageAlgorithms.wash(source, "WASH_PURPLE")`：HSV H120..160、S>=70、V>=50，component area 3..900，最后白底黑字；`ImageAlgorithms.java` 只读。

### 5.2 raw PNG、ROI、坐标与 action 约束

- 协议规格 `:68-73,249-268`：每个 action 最多一个可上传 frame；multipart `metadata: application/json` + `frame: image/png`；禁止 Base64；1 source pixel = 1 transmitted pixel。
- `windowRect.left/top` 是绑定窗口真实 screen coordinates；Cloud 从 frame/region 计算 screen-absolute click；DHXY 不缩放。
- 普通 pipeline 的“单 base screenshot”只约束基础视觉帧。父级必须分别冻结：每个 click 后 verifier frame、每个 Ctrl probe 的 before/after frames、普通 Alt+C 第二次 pipeline 的新 base frame、direct-combat Alt+A 后的新 base frame；不能把补充验证帧偷算成策略重抓，也不能删掉基线要求的 live evidence。
- 每个 `TurnGameClient.capture/execute/localService` public call 都是新 UUID。一次业务 miss 后的下一 verifier、下一 candidate、下一 Ctrl probe是新业务 command/UUID；不得复发旧 actionId，也不得由 transport 自动 retry。
- command terminal 为 completed/busy/duplicate-id/timeout-uncertain/interrupted-uncertain；turn outcome 为 completed/failed/stopped/duplicate-or-uncertain。任何非 completed command、stopped、failed、duplicate/uncertain、actionId/window/frame/step correlation mismatch 都不得继续点击、继续 fallback、写 success memory 或伪造成 verifier miss。

### 5.3 现有能力无法表达的基线动作

1. Cloud/DHXY protocol enum 当前声明 `MOVE_MOUSE`、`KEY_DOWN`、`KEY_UP`；但协议 prose `:58-60` 漏列 `MOVE_MOUSE`，父级需指定 source enum 为本轮 wire authority或另卡修正文档。
2. DHXY `TurnInputStepExecutor.java:70-81` 只接受 background-validated `KEY_TAP`；`KEY_DOWN/KEY_UP` 返回 `BACKGROUND_KEY_UNSUPPORTED`。
3. `BoundWindowKeyboardService.AltShortcut` 中 `ALT_4` 可后台发送，而 `ALT_A`、`ALT_C` 的 `backgroundHwndSupported=false`（`:225-245`）。因此基线普通 Alt+C retry、direct-combat Alt+C/Alt+A 当前不能由既定 turn executor表达。
4. Ctrl probe 要求一个 exclusive boundary 内完成 before capture、Ctrl down、move、after capture、OCR/menu click/verifier、finally Ctrl up。现有 generic turn：
   - 不把 keyboard + mouse + capture 合并成同一个 input-queue exclusive fragment；
   - 一个 action 不能上传 before 与 after 两张图；
   - 任一步失败后后续 step 变为 NOT_RUN，无法保证作为末尾 step 的 Ctrl up 被执行；
   - 两个 action 又无法保持基线的跨 before/after 物理互斥。
5. HTTPS 规格只允许四个 permanent local Service；新增/复活 NPC local macro 会违背 allowlist 及旧 macro 零 active path 目标。

父级可冻结的最小方向：先开一个独立于 TURN-28 的通用机械前置，明确 Alt+A/Alt+C 的合法 HWND/physical 交付方式，以及一个 fail-safe、finally-release 的 Ctrl-hover observation 能力；该前置通过后 TURN-28 仍保持三 production 文件。不得把协议/executor/DHXY mechanic 文件临时塞进 TURN-28 写集，也不得静默删除 Ctrl 或两条快捷键分支。

## 6. 推荐 exact、互斥写集

### 6.1 TURN-28 production reservation set

仅 Cloud：

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java`

其中 `ObjectiveTextRecognizer.java` 只有在 Npc typed API 确实要复用其现有 map/coordinate pure calculation 时才产生 diff；否则保留为 reservation-only、零 diff。`SmartClickRecognizer` 应把现有 Base64/JsonNode/session 入口背后的 pure image algorithms 提升为 typed `BufferedImage/raw frame metadata -> ordered candidate evidence` 边界；不得让新 Npc service 回调旧 DecisionEngine/session queue。

### 6.2 TURN-28 test write set

仅 Cloud 新文件：

- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/service/NpcClickTurnContractTest.java`

报告文件由 helper 单独写，不进入 Worker production/test ownership。

### 6.3 明确只读/排除

- 全部 DHXY production/test。
- `ImageAlgorithms.java`。
- `migration-baseline/**`、`migration-preserved/**`、DHXY current NpcClick 与 `service/npc/**` reference/shadow。
- TURN-26 当前写集：Cloud `DialogService.java`、`CloudDialogOptionOcrImagePort.java`、`CloudDialogOptionOcrWordsPort.java`、`CloudDialogWhiteStoryTemplatePort.java`、`LocalOcrClient.java` 及 `DialogOptionTurnContractTest.java`。
- protocol、turn client/factory/result/executor、Cloud server/host/config、Task、Navigation、BattleRadar、model、pom、主计划、CR/进度文档、矩阵/dashboard。

该集合与 TURN-26 文件级互斥；如第 5.3 节机械前置尚未解决，应先完成独立前置，不扩张本集合。

## 7. `NpcClickTurnContractTest` 最小验收矩阵

计划 profile：默认 `BC4+BASE`，本卡附加 `IMG+LX`；只使用 fake `TurnGameClient`/scripted outcomes/内存 PNG，不启动 OCR runtime、server、Task、UI、capture 或真实 input。

| 建议 test method | 最小断言 |
|---|---|
| `ordinaryPipelineKeepsExactBaselineOrderingAndOneBaseFrame` | early-memory/五倍-tooltip-first/dialog-gate/普通 sequence/`TENTATIVE` 截断；普通一次 Alt+4+400 后一张 base；成功立即停。|
| `learnedAndTooltipKeepPerCandidateClickAndVerifyCounts` | learned 1 click/1 verify；tooltip threshold 0.82、36px dedup、region/hit order，每 hit 1/1、0 retry。|
| `yellowAndFormulaKeepFallbackAndRetryCounts` | yellow 仅 target-not-found 扩 region；concrete hit 最多 2/2；formula 1/1、miss extra 1500、immediate SMALL_RING 后仍按基线进入最终 Ctrl。|
| `ctrlProbeKeepsOffsetsFramesOcrAndReleaseBoundary` | DIRECT/SMALL/FULL exact offsets；15px noncombat reference filter；每 probe before/after ROI、0.05 change gate、yellow wash + OCR/fuzzy；旧 npc tag 不参与 active decision；任何 terminal 都释放 Ctrl。|
| `moveWaitClickIsOneAtomicLocalQueueFragment` | action step exact `MOVE/WAIT150/CLICK`；同一 UUID/command；trailing strategy wait 不拆第二 click command；无 transport retry。|
| `dialogAndCombatVerifierKeepExactObservationCounts` | dialog 每 verify 一次且仅两种 visible 状态成功；combat 每 candidate 最多 4 radar calls，并保留第 4 miss 后 350ms。|
| `directCombatKeepsFlyingAltAExitAndStopBoundaries` | FLYING/UNKNOWN/ground 分支；Alt+C 700、Alt+A 350；direct pipeline 不 Alt+4/不 pre-dialog gate；失败最多 3 次右键退出；stop 不退出。|
| `rawPngRoiAndAbsoluteCoordinatesStayExactlyCorrelated` | multipart raw PNG bytes/SHA/dimensions/region；non-zero window origin；screen-absolute click；无 Base64/resize；out-of-window 零 input。|
| `eachBusinessCommandGetsFreshUuidAndNeverTransportRetries` | base capture、每 candidate click、每 verifier、每 probe observation都各一新 UUID；busy/duplicate/uncertain 不重发物理 action、不继续。|
| `terminalAndCorrelationNegativesDoNotAdvanceOrLearn` | command non-completion、outcome failed/stopped/uncertain、actionId/device/window/frame/step count/index/type mismatch均零后续 command/零 success memory。|
| `pendingEvidenceRequiresExactWindowSourceTokenAndExpectedOption` | window key、normalized sourceTask、proof token、map/name/coords、expected option；mismatch 不跨提交，只有匹配证明写 confirmed evidence。|
| `referencePipelineRemainsReadOnlyAndLegacySessionPathIsNotCalled` | 通过 fake collaborator/call counters 证明新入口不调用 DecisionEngine、queue store、outcome endpoint、旧 macro ports或 DHXY mechanics；不以源码字符串 guard 代替行为断言。|

父级运行门应在所有 Java writers 稳定后执行这一个 named class及 Cloud compile；helper 本轮未运行任何测试或编译。

## 8. 父级必须冻结的行为点与未决问题

1. **FIFO 语义**：外部六标签与 `696a12b0` 的 early-memory、五倍 tooltip-first、两次 dialog gate、`TENTATIVE` 截断、formula immediate Ctrl、final Ctrl 的 exact 映射。
2. **formula Ctrl 重复**：是否保留 formula point 立即 9-probe 后又可能进入最终 Ctrl 的基线次数；任何去重都是行为变化。
3. **Ctrl ownership**：旧业务文档要求 Ctrl hover/menu 本地且 after 图不上传；最终 HTTPS 规格要求 OCR/template 默认在 Cloud、DHXY 只 capture/input。需明确等价迁移形式及 before/after 证据 transport。
4. **Ctrl fail-safe capability**：当前 turn 无跨 capture/input 的 exclusive/finally-release 表达；必须先冻结独立机械前置，不能由 Npc Worker自行选择近似实现。
5. **Alt+A/Alt+C capability**：当前 background executor 明确不支持；必须冻结合法交付机制，不能删 baseline 分支或偷偷 foreground fallback。
6. **one-frame rule**：每 action 一个上传 frame与 Ctrl before/after 两图冲突；需明确新 capability或批准的多-command等价边界。
7. **single base screenshot 计数**：普通 pipeline base、Alt+C 第二 pipeline、Alt+A direct pipeline、post-click verifier、Ctrl before/after各自的允许帧数和 purpose。
8. **story blocker**：`696a12b0` 只有同步 pre-click STORY cleanup；DHXY current 的 ready-event、3 次 restart 是后续行为。父级须指定采用哪一权威，不得混合。
9. **direct-combat authority**：`696a12b0` 使用 flying detection + Alt+C/Alt+A；DHXY current 使用 `directCombatNormalFifoUnverified` + Cloud authorization facts。若保留后者，需指明用户批准的业务差异来源和 exact facts；否则回基线。
10. **verifier ownership**：TURN-26 dialog typed ports稳定后如何做到每 verify 恰好一次 observation；不得让 `DialogService.handleDialog` 的旧 input/capture路径多抓或多点。
11. **current sourceTask safety**：当前 Cloud 比基线多出的 pending sourceTask gate是否作为安全修复保留；建议保留并由 named test锁定，但需父级明示。
12. **reference/shadow 名单**：父级应把第 3.5 节列出的具体路径写入 brief，避免 Worker 重写当前 Cloud Npc 后误删唯一完整基线副本。
13. **SmartClick typed API**：需冻结输入为 raw `BufferedImage` + exact frame/window metadata + typed request，输出为 ordered candidate evidence；禁止继续构造旧 JsonNode/Base64/session facts。
14. **ObjectiveTextRecognizer 触碰条件**：只允许暴露/复用现有 pure map/coordinate API；不得顺手改 Navigation/OCR 行为。
15. **host bean reachability**：当前 `CloudServiceConfiguration` 不扫描 Task，也没有显式 import `com.yueyunfe...remote` 的 dialog components；TURN-28 不改 host/config。父级需确认这是后续 activation 卡责任，不能把“源码有 caller”写成当前 server 已可运行。
16. **TURN-26 稳定门**：本次用户指令视其仍有写入；ACTIVE_WORK 顶部另有“源码门完成、owner 释放”的较新记录。父级派发前必须按实际 writer 与文件 hash重新核验，不由本 helper裁决。
17. **窗口身份 fence**：现有 action只携 deviceId/windowId；父级需冻结 capture后到click前是否还要求 initial/latest HWND/process/windowRect一致检查，以及 mismatch 的零 command/零 memory结果。
18. **旧路径归零范围**：TURN-28 只把新 Cloud Npc active path从 DecisionEngine/session/fact/macro/port中切走；旧文件物理删除必须等后续全局零引用卡。

## 9. PRECHECK EVIDENCE / RISKS

### PRECHECK EVIDENCE

- 当前 Cloud `NpcClickService` 是 `696a12b0` 完整业务副本加一项 sourceTask pending-evidence 安全门，可作为逐段迁移来源。
- source-level caller 与 public API 已完整枚举；当前 server 激活 caller 为 0，后续 activation责任边界清楚。
- turn 已提供 exact-window raw PNG、screen-absolute ROI、one UUID/command、原子 mouse fragment与严格 terminal/correlation校验。
- TURN-28 三 production/一 named-test 的最小 reservation set与 TURN-26 文件级互斥；`ImageAlgorithms` 和完整 reference/shadow均可保持只读。
- baseline 的 candidate order、每候选 click/verifier次数、Ctrl offsets、direct-combat与 stop/pause边界已有可直接写入父级 brief的源码锚点。

### RISKS

- 当前 turn 无法等价表达 Ctrl exclusive before/after/finally-release，也无法合法表达 Alt+A/Alt+C；这是 production cutover 前必须由父级冻结的机械前置，不属于三文件内可自行解决的问题。
- final HTTPS ownership与 `docs/业务逻辑.md` 旧 session/local-Ctrl ownership存在冲突；`696a12b0` exact special ordering也比固定六标签更复杂。
- 当前 Cloud host/Task未激活，dialog remote components的 bean reachability尚非 TURN-28 闭环；源码 caller不能当运行证据。
- 当前 legacy DHXY/DecisionEngine/SmartClick路径含 Base64、session/poll/outcome、后续 story/direct-combat facts及不同 Ctrl/template/timing；不得作为 `696a12b0` 等价实现直接搬入新 path。
- TURN-26实际稳定时点、sourceTask安全门、story blocker、direct-combat authority、Ctrl多帧方案与窗口身份 fence仍需父级逐项冻结。

本报告只提供上述源码证据与风险清单，不给最终审查判定。

TRUE EOF
