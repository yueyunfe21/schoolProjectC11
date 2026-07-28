# TURN-40F：Cloud 默认生产入口切换与客户端厚 Task 退役

## Canonical Card State

- Status: `SOURCE ACTIVE / REPAIR #2 SERVICE FULL MIGRATION / EPICURUS OWNER / TURN-41 BLOCKED`
- Created: `2026-07-20`
- Parent reviewer: 当前父级 Codex（唯一 final reviewer）
- Implementation owner: `Epicurus (agent 019f8025-e406-7d02-ba99-46cc2229a41b)`
- Depends on: TURN-40E 已迁移的当前本地业务差异；Cloud task/runtime/HTTPS turn source gates
- Blocks: TURN-41 user fresh runtime gate

## 1. 发现原因

用户在 CR271 IntelliJ workspace 发现客户端仍保留大体量修罗/五环业务源码。父级重新读取真实生产调用链后确认：

- client `task/xiuluo/XiuluoTaskV2.java`：6,853 行 / 438,481 bytes，`@Component implements GameTask`，
  保留真实 `execute(...)`、phase switch 与业务 shell。
- client `task/wuhuan/FiveRingTaskV2.java`：3,361 行 / 218,271 bytes，`@Component implements GameTask`，
  保留真实 `execute(...)`、`runPhases(...)` 与 phase switch。
- client `DefaultTaskFactory` 仍为 XIULUO/WUHuan/WUBEI 创建上述本地 Task；`WindowTaskRunner` 仍调用
  `taskFactory.createTask(...)` 后本地执行。
- UI `MainWindowController` 的生产启动入口调用 `windowTaskControlService.start(...)`；该方法进入
  `startSameQueue/startSelectedTasks` 和 `TurnModeGuard.startLocal(...)`。
- `startRemoteSameTask(...)` / `startRemoteSelectedTask(...)` 在 production Java 中只有各自声明，没有 caller。
  Cloud 虽已有修罗/五环/五倍厚 Task 与 HTTPS turn runtime，但不是当前 UI 默认生产入口。
- 既有 TURN-42/43 delete set 未包含三个客户端厚 Task、`DefaultTaskFactory` 或其本地 Runner 业务执行链。

因此当前形态是“客户端厚 Task 主执行 + 部分 Cloud decision/sidecar”，不是用户要求的“Cloud 厚 Task 主执行 +
客户端仅 HWND capture/input/local mechanics”。TURN-40E 的“全部逻辑等价迁移完成”和 TURN-41 READY 结论过宽，
必须更正。

## 2. 本卡必须先完成的审计合同

在任何 Java 修改前，逐 symbol 冻结完整传递依赖和写集：

1. UI 所有 start/pause/resume/stop/unregister 路径如何切到现有 remote turn control，禁止保留可误走 local Task 的
   默认入口或增加第二协议。
2. `DefaultTaskFactory`、`WindowTaskRunner`、`MultiWindowTaskManager`、registration/start request 的所有生产引用；
   区分必须保留的窗口 runtime/watcher/input/capture/local mechanics 与应断开的本地 task phase/decision。
3. XIULUO_V2、WUHuan_V2、WUBEI、AUTO_BATTLE 的 Cloud factory/runtime 完整可构造性、start/pause/stop、次数、
   role/team、queue failure policy 和 terminal result 闭包。
4. 客户端三个厚 Task 及仅由其引用的 phase/context/model/service 的 `KEEP_LOCAL_MECHANICS / REWIRE / DELETE`
   manifest；禁止只删顶层类后留下第二业务算法或死源码。
5. Cloud/local 双端 compile、用户授权 named contract tests，以及 TURN-41 fresh runtime 的唯一入口与日志证据。

## 3. 预期最终边界

- Cloud：唯一 task phase、业务条件、候选排序、retry/fallback、计数和跨 turn 状态 owner。
- Client：窗口注册/HWND binding、截图、OCR 前处理（仅机械部分）、输入队列、鼠标键盘执行、窗口短期 cache、
  明确永久本地 Service；不保留可执行的修罗/五环/五倍 phase machine。
- UI：默认且唯一生产启动链进入 remote HTTPS turn；本地 thick-task start 不可达。
- 删除：只在零引用、双端 compile 和父级 source review 通过后删除厚 Task 与专属残余；禁止 stub、恒 null、
  双协议/store/算法或整目录盲删。

本卡计划审计阶段已完成；后续实施仍禁止 runtime/application/server/Task/UI/capture/input。

## 4. 2026-07-20 客户端残余全面审计

完整报告：`docs/superpowers/plans/reports/2026-07-20-turn-40f-client-residual-audit.md`。

父级已盘点 CR271 production Java 共 `586` 文件/`124,233` 行。除 `task` 的 `18,335` 行外，客户端仍有
`service` `30,843` 行、`vision` `3,612` 行、旧 `cloud/remote` `18,627` 行、旧 `cloud/task` `10,918`
行。审计确认这些目录都是混合态，不能整目录盲删：业务门面/OCR/旧 sidecar 必须迁云退役，HWND capture、
固定 preprocess、物理输入、窗口 runtime、HTTPS turn protocol/executor 和永久本地 Service 必须保护。

实施合同冻结为七波：Cloud-default UI/control -> 断开 factory/runner -> 厚 Task 与专属模型退役 -> Service/OCR
收薄 -> 旧 remote/decision SCC 退役 -> 双端静态/compile/source review -> 用户 fresh runtime。当前仅完成计划审计，
未修改 Java，未运行测试或 runtime，尚未允许 TURN-41。

## 5. 2026-07-20 Canonical Whole-Card Claim

- 用户已明确批准后台 Worker 实施、父级前台独立审核。
- 唯一实现 owner：`Lagrange (agent 019f7fb5-95fa-7762-acbf-c731dfdca085)`。
- 状态：`SOURCE ACTIVE / WHOLE-CARD IMPLEMENTATION`。
- Worker 可写范围仅为 `D:\mavenProject\DHXY-cr271` 与 `D:\mavenProject\dhxy-cloud-brain`；
  `D:\mavenProject\DHXY` 继续严格只读。
- 父级不写 Java，不创建额外 reviewer/sub-agent；整卡 delivery 后由父级本人逐文件 final review。
- 禁止 runtime/application/server/Task/UI/capture/input；TURN-41 继续 BLOCKED。

<!-- TRUE_EOF: TURN-40F SOURCE-ACTIVE WHOLE-CARD-IMPLEMENTATION LAGRANGE-OWNER BLOCKS-TURN-41 2026-07-20 -->

## 6. Worker Wave A/B Exact Write-Set Freeze - 2026-07-20

- 只读调用链复核：`MainWindowController` 的 same/selected/queue 生产入口全部调用
  `WindowTaskControlService.start(...)`；`GameWindowRegistrationService` 另有一个
  `startSelectedTasks(...)` caller。控制服务的 `startSameQueue/startSelectedTasks` 当前仍进入
  `TurnModeGuard.startLocal(...)`，而 remote start/pause/resume/stop 方法仅被 contract test 调用。
- 现有 HTTPS turn remote lifecycle 已完整提供 exact-window `startRemote/pauseRemote/resumeRemote/stopRemote`；
  但生产配置缺少 remote API 必需的 `deviceId`，且 UI snapshot 仍只读取 local `RunningTaskHandle`。
- Wave A exact production write-set：
  `TurnClientProperties.java`、`application.properties`、`WindowTaskControlService.java`、
  `TurnModeGuard.java`、`TurnLoopRegistry.java`、`WindowTurnLoop.java`，以及 remote 状态投影所需的
  `WindowTaskRunner.java`。目标仅为 transport identity、remote-default dispatch、remote lifecycle 与 UI 状态；
  不改变 task code/queue order/max-runs/failure policy/role 或 Cloud phase。
- Wave B exact production write-set：`WindowTaskRunner.java`、`MultiWindowTaskManager.java`、
  `TaskFactory.java`、`DefaultTaskFactory.java`，及其零引用后专属 local execution types。先切断所有
  `taskFactory.createTask(...)` / `GameTask.execute(...)` production path，再做逐文件零引用删除。
- 当前业务合同：`docs/业务逻辑.md` 的五倍/修罗 `696a12b0...` 基线及 TURN-40E 当前 workspace
  增量均由既有 Cloud task/runtime 承担；本波无业务条件、phase、probe/read/verify、retry/fallback、
  park/yield、input order 改动。无已批准业务差异；按基线等价迁移。

<!-- TRUE_EOF: TURN-40F WAVE-A-B-WRITESET-FROZEN SOURCE-ACTIVE LAGRANGE-OWNER 2026-07-20 -->

## 7. Worker Wave D Plan-Contract Blocker - Five-Ring One-Bag Incense Decision

- 真实 source 链已复核：Cloud `FiveRingTaskV2.checkFiveRingSuppliesInOneBagSession(...)` 发送
  `BAG_FIVERING_SUPPLY_CHECK`；客户端 `BagLocalOperationExecutor.executeQueueOwning(...)` 在一个
  `BagService.withMainBagOpenGuarded(...)` callback 内固定执行
  `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag(...) -> captured stop checkpoint ->
  MainBagSession.countItemUpTo(...)`。该单 session、精确顺序和三字段结果已经 TURN-40B-C2 父级
  `SOURCE+TEST SOURCE REVIEW PASSED`，不是可静默改变的实现细节。
- 当前客户端 `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag(...)` 仍通过旧
  `cloud/task/SheyaoxiangStatusCloudDecisionService` 做 TICK/capture/upload/decision；因此它既是客户端
  business Service/decision owner 残余，也是 TURN-40F Wave D/E 要退役的第二旧协议 consumer。
- Cloud 已有等价 `PlayerStateService.ensureSheYaoXiangActive(...)` 和 HTTPS-turn capture/use ports，但该调用
  会在一个 Cloud turn 内先结束 Cloud 决策，再发独立本地 bag macro；随后数鞋必须再开一次 bag。直接改成
  Cloud ensure + local count 会把已冻结的单开包合同变成两次开包。
- 现有 HTTPS turn local-service action 是 Cloud response 中的单次闭集动作；客户端执行期间不能在保持
  `MainBagSession` 打开的 callback 中同步回到同一个 Cloud task 做中间香状态决策。若新增跨 turn bag-session
  handle/store，会违反本卡禁止第二 store/跨 turn 本地业务状态；若保留客户端香判断，则违反 Cloud 唯一
  PlayerState/OCR/decision owner；若拆成两次开包，则违反 TURN-40B-C2 已冻结的等价合同。
- **PLAN-CONTRACT BLOCKED（唯一待父级/用户裁决点）**：必须明确批准以下之一后才能删除客户端
  `PlayerStateService` 及旧 incense decision 链：
  1. 批准五环启动补给由“一次开包”调整为 Cloud 判香/独立用香后再开包数鞋；业务判断与香/鞋顺序不变，
     但 UI/input session 次数发生变化；或
  2. 批准保留 `BAG_FIVERING_SUPPLY_CHECK` 内的客户端香决策作为唯一、具名的永久本地复合 mechanics 例外，
     并保留其旧 decision transport（这与 TURN-40F 的 Cloud-only/HTTPS-only 边界冲突）。
- Worker 不选择任何一项，不增加 stub、恒 null、fallback、第二 store/协议，也不删除该活链。Wave A/B/C 已落盘
  且 client main compile exit 0；其余不依赖此选择的静态审计可继续，但 Wave D/E 整体 delivery 被此点阻断。

<!-- TRUE_EOF: TURN-40F PLAN-CONTRACT-BLOCKED FIVE-RING-ONE-BAG-INCENSE-CLOUD-DECISION LAGRANGE-OWNER SOURCE-ACTIVE 2026-07-20 -->

## 8. Parent Status Reconciliation - 2026-07-20

- 父级现场确认 Worker 已真实实施 Wave A/B/C：生产 start caller 已改到 remote，客户端三大厚 Task 及专属
  local task/factory 类型已删除，`XiuluoTaskV2.java`、`FiveRingTaskV2.java`、`WubeiTask.java` 均物理不存在。
- Worker 报告 client main compile exit 0；未运行测试/runtime/input。Wave D/E 尚未完成，父级尚未做逐文件
  source review，因此本卡不是 delivery/approved。
- Worker 已在五环 one-bag incense 合同阻断处 return；当前 `ZERO OWNER / PLAN-CONTRACT BLOCKED`。唯一待用户
  裁决：批准两次开包的 Cloud-only 实现，或批准保留本地复合香判断例外。父级推荐前者以满足完整上云目标。

<!-- TRUE_EOF: TURN-40F PARTIAL-A-B-C PLAN-CONTRACT-BLOCKED OWNER-RETURNED ZERO-OWNER TURN-41-BLOCKED 2026-07-20 -->

## 9. User Decision / Plan-Contract Repair #1 - One Bag Is Mandatory

- 用户明确否决“两次开包”；五环补给必须保持一次打开主背包，并在同一次开包内严格执行：
  `判香/必要时用香 -> stop checkpoint -> 数鞋 -> 关闭背包`。
- 唯一批准实现路线：扩展现有 HTTPS turn v1 为**同 action、同 local-service callback 内的有界
  continuation**。`BAG_FIVERING_SUPPLY_CHECK` 仍只进入一次 queue-owning
  `withMainBagOpenGuarded(...)`；客户端在背包保持打开、输入队列独占期间上报香的原始 observation，Cloud
  返回是否用香的强类型 decision，客户端执行该机械动作后 checkpoint、数鞋并返回整卡既有三字段结果。
- continuation 必须使用现有 HTTPS turn transport/envelope/correlation/validator，不得复活
  `SheyaoxiangStatusCloudDecisionService`、旧 `cloud/task` transport 或任何第二 endpoint/protocol/store。
  客户端不得缓存业务 phase/香状态，不得自行执行 `if inactive then use` 等业务判断；Cloud 是唯一判断 owner。
- continuation 生命周期只存在于当前 Java 调用栈和当前 action/input-exclusive callback；不得建立跨 turn 本地
  bag-session registry。transport/stop/error/identity mismatch 必须 fail 当前 action 并由既有 guarded callback
  关闭背包，不能继续数鞋或 fallback 到本地判断。
- 保持既有验证次数、截图/OCR/input 顺序和业务结果；无其它已批准业务差异。Worker 恢复 Repair #1，完成
  Wave D/E、双端 compile、授权 named tests 与整卡 delivery 后由父级终审。

<!-- TRUE_EOF: TURN-40F SOURCE-ACTIVE REPAIR-1 ONE-BAG-HTTPS-CONTINUATION LAGRANGE-OWNER TURN-41-BLOCKED 2026-07-20 -->

## 10. Parent Contract Correction / Repair #2 - Exactly Four Local Services

- 用户指出并由父级重新核对权威计划 §1196-1197、§1621-1629、§2296、§2888：DHXY 永久本地 Service
  **只有四个**：`BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService`；明确禁止第五个
  Service。此前父级把 `SystemPowerService` 并列为第五个属于错误口径，现撤销。
- `SystemPowerService` 若保留 Windows 睡眠能力，必须迁出 `service` 业务层并降为受控 host/local-operation
  executor；不得作为第五个 Service。除四个 keep set 外，所有 Service 业务 owner 必须迁 Cloud，纯机械部分
  必须进入 mechanics/executor/capture/input/host 包，不能保留原 Service 外壳。
- 新唯一 Worker：`Epicurus (019f8025-e406-7d02-ba99-46cc2229a41b)`，负责 Repair #2 Wave D/E 全量 Service、
  Vision/OCR 与旧 remote/decision 栈迁移；一次开包 HTTPS continuation 硬约束继续有效。

<!-- TRUE_EOF: TURN-40F SOURCE-ACTIVE REPAIR-2 EXACT-FOUR-LOCAL-SERVICES EPICURUS-OWNER TURN-41-BLOCKED 2026-07-20 -->

## 11. User Decision - Preserve Windows Sleep as Local Host Capability

- 用户确认 Windows sleep 明显是本地宿主能力，与游戏业务任务迁云无关，功能必须保留。
- 它不计入四个永久本地业务 Service。原 `SystemPowerService` 必须迁为非 Service 的受控
  host/local-operation executor；Cloud/任务队列只表达用户显式选择的 host action，client 执行 Windows 命令。
- 不恢复本地厚 `SleepComputerTask` 或第二 task phase owner；必须保持原用户显式选择、stop checkpoint、可中断
  等待、日志与 `rundll32 powrprof.dll,SetSuspendState 0,1,0` 行为，不得自动触发或静默删除功能。

<!-- TRUE_EOF: TURN-40F SOURCE-ACTIVE REPAIR-2 FOUR-LOCAL-SERVICES PLUS-LOCAL-HOST-SLEEP-EXECUTOR EPICURUS-OWNER TURN-41-BLOCKED 2026-07-20 -->

## 11. Repair #2 Worker Canonical Claim / Baseline / Exact Manifest - 2026-07-20

- 唯一实现 Worker：`Epicurus (019f8025-e406-7d02-ba99-46cc2229a41b)`；本段仅追加，不覆盖父级第 10 节。
- Client 基线：`thin-client-design`，当前 HEAD `59b85e0bb494f43ad7e7434f3d2170deb373c6ef`；
  当前 dirty diff 已确认 Wave A/B/C 的 remote-default start、local factory/runner 断链和厚 Task 删除均存在，
  不得回退。Cloud 基线：`navigation-migration`，当前 HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`。严格只读业务基线：
  `D:/mavenProject/DHXY@696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 加当前用户增量；禁止写、切支、
  构建或运行。
- 权威合同证据：§1196-1197、§1621-1629、§2296、§2888 均限定 **exact four**
  `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService`，并禁止第五 Service。
  `SystemPowerService` 不在 keep set；如保留睡眠 side effect，只能迁至非 Service 的受控 host/local-operation
  executor。
- 业务基线：已读 `docs/业务逻辑.md`；无已批准业务差异，按 `696a12b0...` 与 TURN-40E 用户增量等价迁移。
  不新增 TTL/read/probe/verify/retry/fallback/park/yield/cleanup/fail-closed 业务规则。
- 一次开包：仅沿现有 HTTPS turn v1 的同 action、同
  `BAG_FIVERING_SUPPLY_CHECK` queue-owning/input-exclusive callback 做有界 continuation；客户端只上报原始香
  observation 并机械执行 Cloud decision。失败由 guarded callback 关包，不数鞋、不 fallback；禁止 queue-in-queue、
  第二 endpoint/protocol/store、跨 turn bag registry 和客户端 `if inactive` 判断。
- 初始真实残留：Service `63` 文件/30,843 行，Vision `7` 文件/3,612 行，
  old `cloud/task` `55` 文件/10,918 行、`cloud/decision` `14` 文件/1,079 行、
  `cloud/xiuluo` `8` 文件/1,002 行、`cloud/remote` `129` 文件/18,626 行。

### 11.1 Service exact manifest (63)

- `src/main/java/com/bot/dhxy/service/autocombat/AutoCombatPanelRoundsLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/autocombat/AutoCombatPanelVisibilityLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroIntent.java`
- `src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroResult.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/battleradar/BattleRadarLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/ClientIdentityService.java`
- `src/main/java/com/bot/dhxy/service/commonbox/CommonBoxLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogDetectionLocalMechanics.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogFallbackPolicy.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogOperation.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionClickResult.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrImageLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionPolicy.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogPreparedActionValidationLocalMechanics.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogStoryAdvanceLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogStoryObjectiveCaptureLocalMechanics.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogStoryPolicy.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/DialogChoiceMemoryService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/GiveItemService.java`
- `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`
- `src/main/java/com/bot/dhxy/service/MapNameCanonicalizer.java`
- `src/main/java/com/bot/dhxy/service/MemoryService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/npc/CtrlMenuProbeTerminal.java`
- `src/main/java/com/bot/dhxy/service/npc/CtrlScanRectKind.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickMechanicsTiming.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickPreparedPointLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickTaskTooltipLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcCtrlMenuTagSet.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcLocalVerifyResult.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcVerifyMode.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
- `src/main/java/com/bot/dhxy/service/SmartClickEvidenceConfirmationService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillTailBoundaryScanner.java`
- `src/main/java/com/bot/dhxy/service/SystemPowerService.java`
- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`
- `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelRectLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
- `src/main/java/com/bot/dhxy/service/teamreturn/TeamReturnButtonLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/teamreturn/TeamReturnLeaderSignalLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/TeamReturnService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/service/wuhuan/FiveRingAcceptDialogLocalOperation.java`

### 11.2 Vision exact manifest (7)

- `src/main/java/com/bot/dhxy/vision/LocationVisionService.java`
- `src/main/java/com/bot/dhxy/vision/MapSurveyService.java`
- `src/main/java/com/bot/dhxy/vision/MiniMapCoordinateReader.java`
- `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`
- `src/main/java/com/bot/dhxy/vision/OcrTextMatcher.java`
- `src/main/java/com/bot/dhxy/vision/OcrWindowScanService.java`
- `src/main/java/com/bot/dhxy/vision/SheyaoxiangDigitTemplateReader.java`

### 11.3 Old stack exact manifest (206)

`cloud/task` (55):

- `src/main/java/com/bot/dhxy/cloud/task/CapabilityGateCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/CapabilityGateCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/CloudImageProcessor.java`
- `src/main/java/com/bot/dhxy/cloud/task/DialogPolicyCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/DialogPolicyCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/DialogPolicyPreClickCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/DialogPolicyPreClickCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/task/ImagePreprocessCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/ImagePreprocessCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/task/ImagePreprocessCloudService.java`
- `src/main/java/com/bot/dhxy/cloud/task/ImagePreprocessOperation.java`
- `src/main/java/com/bot/dhxy/cloud/task/ImagePreprocessWashedImageClient.java`
- `src/main/java/com/bot/dhxy/cloud/task/ImageProcessorService.java`
- `src/main/java/com/bot/dhxy/cloud/task/MaintenanceThresholdCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/MaintenanceThresholdCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/MiniMapLocationCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/MiniMapLocationCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/MiniMapLocationCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/task/NavigationPointCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/NavigationRoutePlanCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudSession.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartDirectCombatAuthorization.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartQueueMessage.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartQueueOutcome.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickStrategyCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/NpcClickStrategyCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/ObjectiveTextReaderCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/RouteCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/RouteCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/RouteMemoryOutcomeIngestResult.java`
- `src/main/java/com/bot/dhxy/cloud/task/RouteMemoryOutcomeReport.java`
- `src/main/java/com/bot/dhxy/cloud/task/SheyaoxiangStatusCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/SheyaoxiangStatusCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/SheyaoxiangStatusCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/task/SummonSkillCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/SummonSkillCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/SummonSkillCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowService.java`
- `src/main/java/com/bot/dhxy/cloud/task/TaskPolicyCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/TaskPolicyCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/TaskRecoveryCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/TaskRecoveryCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/TeamReturnPolicyCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/TeamReturnPolicyCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/TeamRoleTooltipCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/TeamRoleTooltipCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/TeamRoleTooltipCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/TrackerLinkRankerCloudShadowService.java`
- `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudRequest.java`

`cloud/decision` (14):

- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionClient.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionClientException.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionExecutionGate.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionMetricsService.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionMode.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionProperties.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionRequest.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionResponse.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionResult.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionServiceId.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudFallbackMode.java`
- `src/main/java/com/bot/dhxy/cloud/decision/HttpCloudDecisionClient.java`
- `src/main/java/com/bot/dhxy/cloud/decision/MockCloudDecisionClient.java`

`cloud/xiuluo` (8):

- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainActionOutcomeDecision.java`
- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainActionOutcomeRequest.java`
- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainActionType.java`
- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainDecision.java`
- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainResponse.java`
- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainStartRequest.java`
- `src/main/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainStepRequest.java`

`cloud/remote` (129):

- `src/main/java/com/bot/dhxy/cloud/remote/BoundLeaderPrecheckCaptureCapability.java`
- `src/main/java/com/bot/dhxy/cloud/remote/HttpRemoteCommandTransport.java`
- `src/main/java/com/bot/dhxy/cloud/remote/HttpRemoteTaskRunApiClient.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LeaderPrecheckDisposition.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LeaderPrecheckFrameRegistry.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LeaderPrecheckMechanics.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LeaderPrecheckSource.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LeaderSignalPrecheckResult.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LeaderSignalPrecheckResultStatus.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LeaderSignalPrecheckStatus.java`
- `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
- `src/main/java/com/bot/dhxy/cloud/remote/PendingExecutorReadiness.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteAutoCombatPanelFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagReturnItemMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagReturnItemMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagUseIncenseMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagUseIncenseMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBattleRadarAvatarFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBattleRadarMinimapFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBattleRadarSignalFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteBindingFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureImageFormat.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureProvider.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCapturePurpose.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureRegion.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteClientSessionRef.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandHandler.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandOutcomeAck.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandOutcomeAckStatus.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollingLoop.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollRequest.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollResponse.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollStatus.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandTransport.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandTransportException.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommonBoxFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteCoordinateSpace.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogDetectionMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogDetectionMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogOptionOcrImageMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogOptionOcrImageMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogOptionOcrWordsMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogOptionOcrWordsMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogPreparedActionValidationMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogPreparedActionValidationMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogWhiteStoryTemplateMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteExclusiveInteractionControlCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteExclusiveInteractionControlOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteExclusiveSessionStepRef.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteExecutionState.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteFinalConsumedAck.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteFinalConsumedReceipt.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteFinalConsumedReceiptAck.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteFocusFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteFocusState.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameCommand.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOperation.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOutcomeEnvelope.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteGeometryFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteInputActionDto.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteInputActionMapper.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteInputActionType.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteInputBundleCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteInputBundleOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteLeftTopStatusFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNavigateInCurrentMapMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNavigateInCurrentMapMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcPlayerAnchorMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcPlayerAnchorMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcPreparedPointMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcPreparedPointMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcTaskTooltipMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcTaskTooltipMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcYellowTargetMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcYellowTargetMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteObservationMode.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteObservedWindowBinding.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteOutcomeCode.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemotePayloadException.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemotePlayerStateFirstAidMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemotePlayerStateFirstAidMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteQuestArtifactTaskCode.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteQuestDetailArtifactIntent.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteSemanticAddress.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteStopFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteStopRef.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteSummonSkillWholePassCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteSummonSkillWholePassOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunAction.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunActionRequest.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunActionResponse.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunApiClient.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunBinding.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunClientException.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunError.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunErrorCode.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunLifecycleException.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunLifecycleService.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunReceipt.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunRegistration.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunRegistry.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunScope.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunStatus.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunWindow.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunWireStatus.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerFinalConsumedAttachment.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerMaterializeCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerMaterializeOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerPanelRectFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerReadCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerReadOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnButtonFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnLeaderSignalFact.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteUiCleanMacroCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteUiCleanMacroResultPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowBindingRef.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/ResumeExecutorReadinessFact.java`

### 11.4 Repair #2 Exact Write Set

- Client MODIFY/DELETE：上述 63 个 `service`、7 个 `vision`、206 个 old-stack 文件及其零引用 consumer；
  机械实现必须迁到 `cloud/turn/local`、`mechanics`、`capture`、`input`、`host` 等明确非业务 owner 包。
- Client HTTPS turn v1 MODIFY：现有 `cloud/turn` protocol/validator/client/dispatcher/executor 与
  `BagLocalOperationExecutor`，仅用于同 action 有界 continuation 和迁移后的强类型 local operations；保护
  endpoint/envelope/correlation 与单协议。
- Cloud MODIFY/CREATE：现有业务 owner 同路径、`turn` protocol/runtime/client/handler、OCR/识别 owner；
  只吸收客户端当前基线逻辑与 continuation decision，不复制 HWND/capture/input/四 Service。
- Consumer closure MODIFY/DELETE：Spring wiring、UI 独立工具隔离、配置/POM/import、模型与测试 fixture，仅限
  由上述迁移产生的编译和零引用闭包。
- Documentation MODIFY：TURN-40F、权威计划、`ACTIVE_WORK.md`、`PACKAGE_ARCHITECTURE.md`、
  Service/验收矩阵与生成的 `docs/cr-dashboard-data.js`；不覆盖父级并发更新，写前重读物理 EOF。
- Test scope：仅运行用户授权的 `HTTPS_TURN_CONTRACT_TEST_FAMILY` named tests；不运行其它测试，不启动
  runtime/application/server/Task/UI/live capture/input。Java 完成后双端 compile。
- 退役顺序：consumer -> sidecar/handler -> lifecycle/transport -> DTO/codec/fact SCC；每批先零引用，再删除。
  最终 `service` 可达顶层业务类只能是 exact four，所有剩余 local 文件逐方法写
  `KEEP_LOCAL_MECHANICS` 与理由。

<!-- TRUE_EOF: TURN-40F REPAIR-2 WORKER-CLAIM EXACT-MANIFEST EXACT-FOUR-SERVICES EPICURUS-OWNER 2026-07-20 -->

## 12. User Contract Correction - Preserve Windows Sleep As Host Capability

- 用户明确裁决：Windows sleep 是必须保留的本地宿主能力，与游戏任务业务迁云无关；不得丢失“睡眠计算机”
  UI 显式选择语义、安全门、stop checkpoint、短暂可中断等待、日志或实际 Windows sleep side effect。
- `SystemPowerService` 不恢复为第五个永久本地业务 Service；其实现迁至明确的非 Service
  `host/local-operation executor`，由现有 HTTPS turn v1 的显式 host action 下发并在 client 执行。
- 不恢复已删除的客户端厚 `SleepComputerTask` 状态机。Cloud 持有选择后的 task/action 顺序与终止判断；
  client host executor 只执行一次经强类型协议验证的本机 sleep 动作，不在启动时自动执行。
- Repair #2 后续写集新增：双仓 HTTPS turn v1 host operation DTO/validator/dispatcher、Cloud task factory/runtime
  的显式 sleep task owner、client 非 Service host executor，以及 UI/TaskType 到 remote start 的原显式选择闭包。
  exact-four Service 合同保持不变。

<!-- TRUE_EOF: TURN-40F REPAIR-2 WINDOWS-SLEEP-HOST-CAPABILITY-PRESERVED EXACT-FOUR-SERVICES EPICURUS-OWNER 2026-07-20 -->

## 13. Repair #2 Wave D-E Source And Authorized-Test Delivery - 2026-07-20

### 13.1 Final Client Ownership / Deletion Evidence

- `src/main/java/com/bot/dhxy/service` 从基线 `63 files / 30,843 lines` 收缩为 **exactly 4 files**：
  `BagService.java`、`UICleanerService.java`、`GiveItemService.java`、`QuestManagerService.java`。
- `src/main/java/com/bot/dhxy/vision`=`0`；旧 `cloud/task`、`cloud/decision`、`cloud/xiuluo`、
  `cloud/remote` 均=`0`。生产引用扫描对 13 个点名厚 Service、`SystemPowerService`、旧
  `SheyaoxiangStatusCloudDecisionService` 和四个旧 cloud 包均为零；仅 JavaDoc 保留两个基线类名说明。
- 已删除点名业务门面：`TaskMaintenanceService`、`NavigationService`、`TaskTrackerPanelService`、
  `DialogService`、`SummonSkillService`、`PlayerStateService`、`NpcClickService`、`AutoCombatService`、
  `BattleRadarService`、`TeamReturnService`、`CommonBoxService`、`AutoCombatPanelService`、
  `ReturnItemPrescanService`，以及其余非 exact-four 顶层 Service 和失去 consumer 的 local business mechanics。
- 已删除 vision 业务 owner：`ObjectiveTextRecognitionService`、`MiniMapCoordinateReader`、
  `LocationVisionService`、`SheyaoxiangDigitTemplateReader`、`OcrTextMatcher`；固定 OCR mask/copy 改名迁至
  `capture.FixedOcrMaskPreprocessor`。`MapSurveyService` 及其 UI runtime 入口被隔离删除，避免继续携带旧
  pseudo-cloud recognizer/第二 decision owner；这是独立诊断工具可达性的显式变化，交父级复审。

### 13.2 KEEP_LOCAL_MECHANICS - Exact Four Services

- `BagService`：`findItemPageIndex`、`withMainBagOpen`、`withMainBagOpenGuarded`、`findAndSelectItem*`、
  `runReturnItemMacroDirectForExclusive`、`runUseIncenseMacroDirectForExclusive`、`findAndUseItem*`、
  `prescanMainBagTaskPageItem`、`captureMainBagTaskPagePrescanSnapshots`、
  `matchMainBagTaskPagePrescanSnapshots`、`prescanMainBagItemFromBack`、`useCachedMainBagReturnItem`、
  `findAndUseMainBagTaskPageItem`、`isMainBagOpen`，以及 open-session 的 `findItemPageIndex`、
  `countItemUpTo`、`useItem`：只拥有固定背包几何、capture/template fact、单输入队列和已打开 session 的
  物理动作；不拥有香状态解释、是否用香、任务 phase/count terminal。
- `UICleanerService`：`cleanUpAll`、`closeAllGenericWindows`、`cleanLightweightInterruptions`、
  `closeMapSearchInputByX2Direct`：只做通用窗口/X2 固定关闭宏；业务对话识别、候选与 fallback 已由 Cloud
  `CloudUiCleanerPort + DialogService` 持有。
- `GiveItemService`：`executeGive`、`executeGiveDirectForExclusive`、
  `executeGiveFromOpenDialogDirectForExclusive`：只做已批准 give-entry -> item-select -> give-button 的单队列
  物理宏与固定模板事实，不持有 task phase。
- `QuestManagerService`：`activateTaskIfPresent` 两入口、`captureCurrentQuestDetailForTask`：只做任务栏固定点击和
  原始 detail capture/absolute origin；OCR/任务解释由 Cloud 持有。
- client `vision` 没有 KEEP 文件。其余保留文件均位于 `capture/input/window/cloud.turn.local/host` 等非业务包，
  分别只承载 HWND binding、固定 ROI、raw PNG、pixel/window facts、强类型 dispatcher 或物理 side effect。

### 13.3 Same-Action Continuation And Sleep Host Result

- 五环补给：现有 HTTPS turn v1 增加 action-bound bounded continuation；同一
  `BAG_FIVERING_SUPPLY_CHECK` queue-owning exclusive callback 内保持背包打开，按
  `TICK -> STATUS_IMAGE? -> Cloud USE/KEEP -> direct MainBagSession.useItem -> OUTCOME -> Cloud COMPLETE ->
  stop checkpoint -> count shoes -> guarded close` 执行。没有第二 endpoint/store、跨 turn bag registry、
  queue-in-queue、本地 inactive 判断或两次开包；transport/identity/stop/error 抛出 action failure，不数鞋。
- 五环接任务对话：client 只上传两个固定 raw ROI 并直接执行 Cloud `CLICK_ACCEPT/CLOSE_STORY`；模板洗图、匹配、
  click point、daily-limit/accepted/terminal 均在 Cloud `FiveRingTaskV2` continuation handler。story close 未返回
  `ADVANCED` 或独占 callback checkpoint 失败时不再上报 closed outcome。
- Windows sleep：`SystemPowerService` 与 client `SleepComputerTask` 已删除；非 Service
  `cloud.turn.local.host.HostLocalOperationExecutor` 保留显式选择后的 checkpoint、1500ms 可中断等待、日志和
  `rundll32.exe powrprof.dll,SetSuspendState 0,1,0`。Cloud `HostSleepTask` 唯一拥有 queue task/terminal，协议仅有
  `SLEEP_COMPUTER -> HOST_SLEEP_COMPUTER`，UI 原显式选择和 `STOP_ON_FAILURE` 安全门保留。

### 13.4 Verification Evidence / Remaining Gate

- 双仓 protocol 与 9 个 T01 fixture：CRLF/LF 归一后 mismatch=`0`；未引入第二协议。
- Client T01：44 tests pass；Cloud T01：44 tests pass。
- Cloud T02 六类 named tests：30 tests pass，含新增 unresolved same-action continuation 测试。
- Client T03+T04：41 tests pass；仅运行计划授权 `HTTPS_TURN_CONTRACT_TEST_FAMILY` named tests。
- `D:\mavenProject\DHXY-cr271`：`mvn -q -DskipTests compile` exit 0。
- `D:\mavenProject\dhxy-cloud-brain`：`mvn -q compile` exit 0。
- 未启动 application/server/Task/UI/game/live capture/input；严格只读基线未写、未构建、未运行。
- **VISUAL REPLAY BLOCKER：** 两个可写仓没有五环 accept/story raw testcase；只读基线只有 task-tracker raw case。
  现存五环对话素材是 48x17 至 529x138 的 template/source crop，不能冒充完整 raw screenshot 产出可信 click
  标记图。本轮又明确禁止 live capture，所以视觉 replay/marked output 尚未交付。父级提供或批准一张 repo-local
  raw accept/daily-limit screenshot 后，才能执行该 gate；在此之前不写 `WHOLE-CARD SOURCE+TEST DELIVERED`。
- 当前状态：`SOURCE + AUTHORIZED TESTS DELIVERED / VISUAL REPLAY BLOCKED / PARENT REVIEW PENDING`；Worker 不自行
  `Approved`。

<!-- TRUE_EOF: TURN-40F REPAIR-2 SOURCE-AUTHORIZED-TESTS-DELIVERED VISUAL-REPLAY-BLOCKED EXACT-FOUR-SERVICES PARENT-REVIEW-PENDING 2026-07-20 -->

## 14. Repair #2 Final Baseline Clarification And Fresh Gate - 2026-07-20

- 严格只读 `696a12b0` 基线的 `FiveRingTaskV2.acceptInitialDialogAndTriggerPathing` 虽声明
  `for (attempt = 1; attempt <= 2; attempt++)`，但首轮的未点击、日限命中、普通点击成功三个分支分别直接
  `return RETRYABLE_ERROR`、`return READY_TO_CONTINUE`、`return READY_TO_CONTINUE`，所以第二轮在有效控制流中
  不可达。Repair #2 的同 action continuation 保持该有效单次 observation/click 行为；只修正 JavaDoc，不新增
  retry、fallback、stage 或协议字段。
- JavaDoc 修正后 fresh 授权聚合测试：client T01+T03+T04=`85/85`，Cloud T01+T02=`74/74`，均
  failures/errors/skipped=`0/0/0`；41 个双仓 protocol Java 文件按 CRLF/LF 归一后 mismatch=`0`。
- fresh compile：client `mvn -q -DskipTests compile` exit 0；Cloud `mvn -q compile` exit 0。
- fresh structure：client `service=4`（exact four）、`vision=0`、旧 `cloud/task|decision|xiuluo|remote=0`；
  `SystemPowerService`/client `SleepComputerTask` 生产引用零命中，显式 sleep UI -> task code -> Cloud
  `HostSleepTask` -> `HOST_SLEEP_COMPUTER` -> non-Service `HostLocalOperationExecutor` -> Windows command 可达。
- 状态不升级：仍为 `SOURCE + AUTHORIZED TESTS DELIVERED / VISUAL REPLAY BLOCKED / PARENT REVIEW PENDING`；
  不写 `WHOLE-CARD SOURCE+TEST DELIVERED`，不自行 `Approved`。

<!-- TRUE_EOF: TURN-40F REPAIR-2 FINAL-BASELINE-CLARIFIED FRESH-TEST-COMPILE-PASS VISUAL-REPLAY-BLOCKED PARENT-REVIEW-PENDING 2026-07-20 -->

## 15. Parent Residual Review - Dead Client Code Removed / Ready Event Closure Reopened

- 用户要求客户端不保留零生产调用类和空目录。父级按生产引用图删除 client `TeleportConfig`、
  `TextRecognizer`、`OcrWordResult`、41 个从非 model 生产入口不可达的旧业务 model，以及递归剥离出的
  `TeamTaskProperties`、`TaskTransactionRunner`、旧 `XiuluoPhase`、`WindowReadyEventBus`、
  `WindowReadyEvent`/`WindowReadyEventType` 等断线遗留；`src` 下 39 个空目录已删除，剩余空目录为 0。
- `TeleportConfig` 不是 OCR/dialog owner；它是零 caller 地图别名表，Cloud `DialogService` 与 Cloud 配置已有
  对应 owner。client `TextRecognizer` 是零 caller 的本地 OCR HTTP sidecar，已删除。保留的
  `FixedOcrMaskPreprocessor`、`OcrWindowRegion`、`ImagePreprocessor` 只做 raw frame 的固定遮罩、裁剪、洗图，
  不识别文字、不解释 dialog/任务语义。
- 清理后 client `mvn -q -DskipTests compile` exit 0；只读 `D:\mavenProject\DHXY` 未写、未构建、未切分支。
- 父级随后核对 `696a12b0` 与当前双仓，发现 **P1 Ready Event producer/observer closure 缺失**：Cloud
  `CloudWholeTaskReadyEventState` 已被三大 Cloud Task 消费，但全仓没有 production `publish(...)` caller；client
  `RemoteTurnMetadataSupplier` 固定发送 `pathingSnapshot=null`，client `WindowRuntimeContext.updatePathingSnapshot`
  零 caller，当前 thin `WindowTaskRunner` 也没有基线 movement/combat/dialog observer。
- 旧 local `WindowReadyEventBus` 不恢复为第二业务 Bus。唯一合法修复方向：client 只保留 HWND-bound
  movement/combat/dialog/pathing 机械 observer 与强类型事实 producer，经既有 HTTPS turn v1 单协议上报；Cloud
  `CloudWholeTaskReadyEventState` 仍是唯一 sequence/store/wait owner，并按 exact tenant/user/device/window/task
  scope 发布与唤醒。不得把 local miss/timeout 当业务真值，不得新增 TTL/retry/phase/fallback。
- 修复前必须逐事件核对 `696a12b0` publisher：`PATHING_TERMINAL`、`PREPARED_ACTION_READY`、
  `COMBAT_STATE_CHANGED`、`TASK_ATTENTION_REQUIRED` 及三大 Task 当前实际消费的其它 event；必须证明
  same-window terminal 可以在 bounded wait 内真正唤醒，而不是只能等 Cloud timeout 后下一 turn 才看到 metadata。
- 当前状态回退为 `REPAIR REQUIRED / P0=0 P1=1 P2=0 / NOT READY FOR USER TEST`。视觉 replay blocker 保留，
  但它不是本次 Ready Event source closure 的替代证据。无已批准业务差异；按 `696a12b0` 基线等价迁移。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW READY-EVENT-PRODUCER-OBSERVER-P1 REPAIR-REQUIRED NOT-READY-FOR-TEST 2026-07-20 -->

## 16. Repair #3 Card - Cloud Observer / Ready Event Closure

### 16.1 Authority And Baseline

- 业务等价权威为只读 `D:\mavenProject\DHXY` 当前工作区；其中
  `WindowTaskRunner.java`=`3027` lines / SHA-256
  `256C4AB16D33C2E30355AAC790A8D84F9E875C16F2C3742BB42D34AAF670FDFF`。不得写、构建、切分支或整理该工作区。
- `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 用于确认原始事件语义；当前本地 Runner 的 post-696
  stale-prepared、owner fence、combat/pathing guard 等增量也必须纳入迁移，不得退回旧字节。
- 当前五个真实 production publisher 是：`COMBAT_STATE_CHANGED`、`PRE_BATTLE_TIMEOUT`、
  `TASK_ATTENTION_REQUIRED`、`PREPARED_ACTION_READY`、`PATHING_TERMINAL`。扩展 enum 中没有当前本地
  production publisher 的类型，不得为了凑 enum 人工发布。

### 16.2 Frozen Architecture

- Cloud 新增 exact-window、exact-task-run observer lifecycle；它随已接受 Cloud task run 启停，复用现有
  `TaskExecutionContext`、`TurnGameClient`、Cloud OCR/template/navigation/dialog/combat owner 和 turn authority。
- observer 只通过现有 `/api/v1/client/turn` action/local-operation 通道请求 HWND-bound raw capture、固定 ROI、
  pixel fact 或必要的本地物理宏；不得新增 endpoint、旧 remote broker、第二 transport、客户端 OCR/模板解释或
  客户端 task phase。
- `CloudWholeTaskReadyEventState` 继续是唯一 event sequence/store/wait owner。observer 在 Cloud 完成事实重读、
  状态分类和 transition fence 后调用其 `publish(...)`；事件仍只作 soft wake，consumer 必须重读 Cloud 权威事实。
- 不允许用 `TurnWindowMetadata.pathingSnapshot` 的下一次 60 秒 long-wait 刷新代替唤醒；same-window terminal 必须
  由独立 Cloud observer action/probe 在 task await 期间到达并 signal。不得中断一个结果未知的 POST 来抢发 metadata。
- client `WindowTaskRunner` 保持薄 lifecycle/UI projection，不恢复 observer、OCR、Task factory 或本地业务状态机。

### 16.3 Five Transition Closures

- `COMBAT_STATE_CHANGED`：复用 Cloud `AutoCombatService`/battle-radar read-only verdict；仅在 tick 变化时发布，保留
  当前本地 combat-entry cleanup、五倍 tracker-green pathing clear、修罗 tracker-shortcut clear 的精确顺序。
- `PRE_BATTLE_TIMEOUT`：保留五倍 ordinary timer 的 start/pause/clear、in-combat 不计时、300000ms、单次 publish
  fence；timeout 不得由 turn timeout 推导。
- `TASK_ATTENTION_REQUIRED`：复用 Cloud dialog visible detection；保留 2500ms recent suppression、exact window/
  HWND/task interest fence，negative/miss 不得发布。
- `PREPARED_ACTION_READY`：Cloud 唯一准备 OCR/template/click candidate；保留 operation、target、intent、freshness、
  stale reprepare cooldown 和 current local owner fence，客户端只执行最终批准的 typed click action。
- `PATHING_TERMINAL`：Cloud 读取 raw mini-map capture并复用唯一 map/OCR owner，保持 intent identity、probe freshness、
  stationary timing、dialog blocking、ARRIVED/STOPPED_AWAY transition-only publish，以及 route-memory settlement顺序。

### 16.4 Write Set And Retirement

- Cloud MODIFY/CREATE：`turn/runtime` task-run lifecycle、一个明确 observer owner、现有 navigation/dialog/combat
  collaborators、`CloudWholeTaskReadyEventState` 接线及对应 tests。不得把 watcher/timer/store 塞进 event-state 类。
- Client MODIFY/CREATE：仅现有 turn protocol/local-operation/capture/mechanics 和 wiring；只在 Cloud observer
  缺少强类型 raw fact 时增加最小操作。禁止新增 `*Service`，永久本地 Service 仍 exact four。
- Shared protocol 仅在确有缺失 fact 时双仓同字节扩展现有 HTTPS turn v1 DTO/validator/golden；不得新增 observer
  专用 endpoint、并行 action consumer 或会吞掉正常 action 的 observation-only poll。
- closure 后删除/收缩只为旧 observer 服务的 client runtime slots/models/operations，必须逐符号零生产引用；不得
  先删 task 仍会重读的事实，也不得保留双份 pathing/dialog/prepared business store。

### 16.5 Acceptance

- 新增双仓 protocol golden（若协议有变化）和 Cloud named tests，逐类证明五个 transition、wrong window/task/intent
  拒绝、duplicate coalesce、stop/pause、same-window await 真唤醒、other-window priority，以及 miss/timeout 不成真。
- 回归现有 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 授权 named tests；Java writer 稳定后 client
  `mvn -q -DskipTests compile` 与 Cloud `mvn -q compile` 必须 exit 0。
- 结构门：client Service exact four、vision=0、旧 cloud stack=0、`WindowTaskRunner` 无业务 observer、client
  production OCR=0、empty `src` dirs=0。
- 禁止 application/server/Task/UI/live capture/input。visual replay blocker仍保留，不能用 unit test 替代。

当前状态：`REPAIR #3 READY / ZERO OWNER / P0=0 P1=1 P2=0 / TURN-41 BLOCKED`。

<!-- TRUE_EOF: TURN-40F REPAIR-3 CLOUD-OBSERVER-READY-EVENT-CLOSURE READY ZERO-OWNER TURN-41-BLOCKED 2026-07-20 -->

## 17. Repair #3 Parent Source Review #1 - Partial Pathing Slice Rejected

- Review scope：client protocol/local executor 4 files and Cloud protocol/client/pathing-state/observer/runtime/wiring
  8 files currently written by `Huygens (019f80b4-8421-7a42-907b-0640db588e3b)`；未发现整卡 delivery，未运行 Maven。
- `P0=0 / P1=2 / P2=1 / REPAIR REQUIRED`。现有 `PATHING_TERMINAL` 是 WIP vertical slice，不能作为
  TURN-40F Repair #3 delivery，也不能解锁 TURN-41 或用户测试。
- **P1-1 missing production closure：** 当前只新增 `PATHING_TERMINAL` publisher；冻结的另外四个真实 producer
  `COMBAT_STATE_CHANGED`、`PRE_BATTLE_TIMEOUT`、`TASK_ATTENTION_REQUIRED`、`PREPARED_ACTION_READY` 仍无 Cloud
  production `publish(...)` path。返修必须逐类迁入当前本地 Runner 的 transition/fence/cleanup 顺序，禁止只补 enum、
  test publisher 或 timeout fallback。
- **P1-2 same-window action race：** `CloudWholeTaskObserver` 每秒独立调用 `TurnGameClient.localService/capture`，与 task
  thread 共用 `CloudTurnExchange` 单槽；任一方抢占时另一方得到 `BUSY`。当前没有 task-turn priority/park-only admission
  或证明业务 caller 对 BUSY 无失败解释的合同，因此 observer 可能反向干扰正常任务 turn。返修必须复用现有 turn
  authority 建立 same-window task-priority、observer 仅在可观察窗口准入的单槽仲裁，并以并发测试证明 task action 不被
  observer 饿死/误失败、parked task 又能被 observer 真唤醒；不得新增第二 action consumer/endpoint。
- **P2-1 lifecycle/test gap：** observer 对所有 `GameTask` 无差别启动，尚无 exact task-type relevance gate、stop/pause
  teardown、wrong task/window/intent、duplicate transition、miss/timeout-no-truth、other-window priority 与 bounded
  same-window wake tests。返修应只为五类事件实际需要的 task lifecycle 启动对应 probes，并补齐第 16.5 节 named tests。
- 已核实 `PATHING_TERMINAL` 的 `1000ms` probe cadence、`2200ms` stationary threshold、UNTARGETED 不判 ARRIVED、
  coordinate tolerance 与当前只读本地 Runner 主判定一致；本次不要求为这些已对齐点重写算法。仍需保留 intent identity、
  transition-only publish、route-memory settlement/cleanup 与 Cloud 单一事实 owner 的完整验收证据。

当前状态：`REPAIR #3 SOURCE ACTIVE / PARENT REVIEW #1 0-2-1 / REPAIR REQUIRED / NOT READY FOR USER TEST`。

<!-- TRUE_EOF: TURN-40F REPAIR-3 PARENT-REVIEW-1 P0-0-P1-2-P2-1 REPAIR-REQUIRED HUYGENS-REPAIR NOT-READY-FOR-TEST 2026-07-20 -->

## 18. Repair #3 Canonical SOURCE+TEST Delivery

- Delivery owner：`Huygens (019f80b4-8421-7a42-907b-0640db588e3b)`。
- Delivery status：`WHOLE-CARD SOURCE+TEST DELIVERED / WAITING PARENT SOURCE REVIEW`；未自行标记 Approved。
- 只读业务基线复核：`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\window\execution\WindowTaskRunner.java`
  SHA-256 仍为 `256C4AB16D33C2E30355AAC790A8D84F9E875C16F2C3742BB42D34AAF670FDFF`，本轮未写入、未构建、未切分支。

### 18.1 Production closure

- `CloudWholeTaskObserver` 只为 `WUBEI/WUHuan_V2/XIULUO_V2` exact task run 启动；pause/stop 时不探测，run terminal
  由 `CloudTurnTaskRuntime` finally 关闭。每个 tick 先用 `CloudTaskTurnAssembly` 创建的真实 authority handle 执行
  `tryEnter`：lane 有 holder 或任何 queued task waiter 时立即不准入，成功后 finally `forceRelease`。因此 observer 与 task
  复用同一个 fair-turn authority 和同一个 HTTPS turn exchange；没有新 endpoint/transport/action consumer/local bus。
- 五类真实 publisher 已闭合：
  - `COMBAT_STATE_CHANGED`：Cloud read-only combat transition；先 publish，后按本地顺序清 Wubei dialog/prepared/
    `wubei:tracker-green-click` 或 Xiuluo `xiuluo-v2:tracker-shortcut` pathing。
  - `PRE_BATTLE_TIMEOUT`：读取 exact local pre-battle timer，排除 IN_COMBAT，`300000ms` 后按同一 startedAt 只发布一次。
  - `TASK_ATTENTION_REQUIRED`：读取 exact task dialog interest fence，Cloud 检测可见 dialog，过期/错 task/空 operations/miss
    均不发布，`2500ms` 抑制重复。
  - `PREPARED_ACTION_READY`：由 Cloud 唯一 `CloudDialogPreparedActionState.publish` 真实生产点同步发布，保留 operation/
    target/source 和 exact window/HWND；不是 enum/test publisher。
  - `PATHING_TERMINAL`：保留 `1000ms` cadence、`2200ms` stationary、tolerance、UNTARGETED 不判 ARRIVED、intent-id
    二次 fence和 transition-only publish；local authoritative idle read 会清 Cloud mirror，避免 task 清槽后遗留旧 intent。
- client 永久 `@Component/@Service` 再核为 exact four：`BagService`、`UICleanerService`、`GiveItemService`、
  `QuestManagerService`；删除已无 production class 对应物的 `WindowReadyEventBusControlWakeTest`，未恢复 local bus。

### 18.2 Complete write set

- CR271 client：
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWholeTaskRuntimeResult.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnDialogRuntimeFact.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`
  - `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - deleted stale `src/test/java/com/bot/dhxy/window/runtime/WindowReadyEventBusControlWakeTest.java`
- Cloud：
  - matching four protocol files above
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java`
  - `src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java`
  - `src/main/java/com/bot/dhxy/service/dialog/CloudDialogPreparedActionState.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnRuntimeConfiguration.java`
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskFoundationContractTest.java`
  - new `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverPolicyContractTest.java`

### 18.3 Verification

- PASS：CR271 `mvn -q -DskipTests compile`。
- PASS：Cloud `mvn -q compile`。
- PASS：CR271 `mvn -q -Dtest=TurnProtocolValidatorContractTest test`（19 tests）。
- PASS：Cloud focused named methods
  `CloudWholeTaskObserverPolicyContractTest` plus
  `CloudWholeTaskFoundationContractTest#preparedSlotProductionPublishAlsoPublishesReadyEvent+
  sameWindowObserverAdmissionYieldsToTaskAndWakesOnlyAfterTaskParks`；真实 authority 日志证明 queued same-window task 先 handoff，
  active task 时 observer try-admission=false，task release 后 observer 才准入，真实 ready-state condition wait 在 `54ms` 被 publish 唤醒。
- 未运行 runtime/application/server/Task/UI/live capture/input；未运行未授权测试族。
- 备注：曾尝试整个 `CloudWholeTaskFoundationContractTest`，其既有无关 fixture 使用非数字 `hwnd-*`，并含一个 interrupt
  预期与当前 typed checkpoint 不一致，因此全类不作为本卡通过证据；本卡新增的两个精确 production 方法随后单独通过。

等待父级按第16-18节逐文件 final source review。

<!-- TRUE_EOF: TURN-40F REPAIR-3 WHOLE-CARD SOURCE+TEST DELIVERED HUYGENS WAITING-PARENT-SOURCE-REVIEW NOT-APPROVED 2026-07-20 -->

## 19. Repair #3 Parent Source Review #2 - Delivery Rejected

- Review verdict：`P0=0 / P1=2 / P2=1 / REPAIR REQUIRED`。双端 compile/focused-test 证据保留，但现有测试未覆盖
  下列 production 断链，TURN-40F、TURN-41 和用户测试继续 blocked。
- **P1-1 combat-entry cleanup clears the wrong authority：** Cloud
  `CloudWholeTaskObserver.probeCombat` 在 Wubei/Xiuluo 进入战斗后仅调用
  `CloudNavigationPathingState.clearIfSourcePrefix(...)` 清 Cloud mirror；当前本地 Runner 基线清的是客户端
  `WindowRuntimeContext.clearPathingSignalIfSourcePrefix(...)` 权威 intent/pending-memory slot。下一 tick 的
  `WHOLE_TASK_PATHING_READ` 会再次返回旧 intent 并由 `acceptLocalFact(...)` 回灌 Cloud。Wubei 同时遗漏基线
  `clearOrdinaryEnterBattleTargetMapGate(...)`，`clearDialogInterest(...)` 也不等价于清完整 request/prepared/gate。
  返修必须通过现有强类型 local operation 原子清权威 prefix slot，并同步收敛 Cloud mirror；补生产链测试证明进入战斗
  后 local read 为 idle、旧 intent 不复活、pending memory/gate按基线顺序清理。
- **P1-2 no parked prepared-action producer：** `probeAttention` 只 detect visible dialog并发布
  `TASK_ATTENTION_REQUIRED`；它没有迁入当前本地 Runner 的
  `task-dialog-interest priority -> route-dialog -> task-dialog-interest fallback` preparation chain，也没有 target-map
  matched gate opening。`CloudDialogPreparedActionState.publish(...)` 新增的 `PREPARED_ACTION_READY` 只是被动 hook；
  当前 observer 没有在 task parked时产生新的 `PreparedDialogAction`，consumer仍可能永久等不到 prepared wake。
  返修必须复用 Cloud `DialogService`/prepared state按当前本地 post-696 顺序执行真实 prepare，保留 intent/operation/
  target/freshness/stale cooldown fence；补从 exact local interest+visible raw fact到 stored action+ready event+bounded await
  的生产集成测试，禁止直接调用 `publish(...)` 冒充 producer。
- **P2-1 pre-battle event loses baseline payload/fence：** observer 的 `PRE_BATTLE_TIMEOUT` 不携带本地 timer slot 的
  `targetKeyword`，且只用 observer-thread字段 `preBattlePublishedStartedAt` 去重，没有复用本地
  `markOrdinaryPreBattleTimeoutPublished` 单次 fence。返修需扩展最小 typed timer fact/mark operation或等价原子合同，保持
  task/source/target/startedAt/publishedAt与 timer clear/reset语义；补 duplicate/restart/miss测试，不能从 turn timeout推导。
- 另外，observer持有 task-turn authority 时顺序执行 combat/timer/attention/pathing多个 HTTPS action；任一事件先 publish
  后，已唤醒 task只能排队到整 tick释放。返修测试须证明该 hold 有界且 task waiter获得 FIFO优先，不可用第二 consumer解决。

当前状态：`REPAIR #3 SOURCE ACTIVE / PARENT REVIEW #2 0-2-1 / REPAIR REQUIRED / NOT READY FOR USER TEST`。

<!-- TRUE_EOF: TURN-40F REPAIR-3 PARENT-REVIEW-2 P0-0-P1-2-P2-1 REPAIR-REQUIRED HUYGENS-REPAIR NOT-READY-FOR-TEST 2026-07-20 -->

## 20. Repair #3 Canonical SOURCE+TEST Delivery After Review #2

- Delivery owner：`Huygens (019f80b4-8421-7a42-907b-0640db588e3b)`。
- Delivery status：`WHOLE-CARD SOURCE+TEST DELIVERED / WAITING PARENT SOURCE REVIEW`；未自行标记 Approved。
- `D:\mavenProject\DHXY` 全程只读；未写入、未构建、未切分支。CR271 client 永久 Service 复核仍为 exact four：
  `BagService`、`GiveItemService`、`QuestManagerService`、`UICleanerService`。

### 20.1 Review #2 closure

- 新增现有 HTTPS turn v1 下的强类型 local operations：
  `WHOLE_TASK_COMBAT_ENTRY_CLEANUP`、`WHOLE_TASK_PRE_BATTLE_FACT_READ`、
  `WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK`、`WHOLE_TASK_TARGET_MAP_GATE_OPEN`；无新 endpoint、transport、action consumer、bus 或 store。
- combat-entry：Cloud transition event publish 后调用 client authoritative cleanup。Wubei 按基线顺序清 dialog interest、
  dialog request（连带 prepared）、target-map gate，再以 source-prefix CAS 清 pathing slot及 pending transfer memory；Xiuluo
  清其 tracker-shortcut prefix。local operation成功后才清 Cloud prepared/pathing mirror，下一次 local read为 idle，旧 intent
  不再回灌。
- pre-battle：typed `TurnPreBattleFact` 保留 task/source/target/startedAt/publishedAt及完整 gate字段；timeout mark复用
  `WindowRuntimeContext.markOrdinaryPreBattleTimeoutPublished` CAS，只有 `newlyPublished=true` 才生产事件。timer clear/restart
  继续由原 runtime reset published fence。
- parked dialog：`probeAttention` 读取 exact interest并复用同一帧 `DialogDetection`，执行
  `task-interest priority -> route -> task-interest fallback`；Wubei accept-memory/template、enter-battle、story/absent及Xiuluo
  enter-battle均调用现有 `DialogService`真实 producer。route prepared action新增原 intentId绑定；existing slot保留
  operation/target/window/HWND/freshness fence，stale clear有1000ms cooldown。成功 prepare经唯一
  `CloudDialogPreparedActionState.publish` 自动生产 `PREPARED_ACTION_READY`。
- Wubei pathing probe在 canonical current-map命中 typed gate target后，以 local CAS open gate，再登记 exact
  `WUBEI_ENTER_BATTLE` interest；没有由Cloud镜像自行制造 gate truth。
- authority：一个 observer tick拆成 combat/prebattle/attention/pathing四次独立 `tryEnter/finally forceRelease`短租约；任一
  publisher返回即释放本次租约。既有 fair authority保证已排队 task waiter FIFO优先，后续 observer probe不能插队。

### 20.2 Complete Review #2 write set

- CR271：`TurnLocalOperation`、`TurnProtocolValidator`、`TurnWholeTaskRuntimeResult`、`TurnDialogRuntimeFact`、新
  `TurnPreBattleFact`/`TurnCombatCleanupFact`、`LocalServiceStepDispatcher`、
  `WholeTaskRuntimeLocalOperationExecutor`、`WindowRuntimeContext`、新
  `WindowRuntimeObserverClosureContractTest`。
- Cloud：matching protocol files、`CloudWholeTaskRuntimeLocalServiceClient`、`CloudWholeTaskObserver`、
  `DialogService` route-intent overload、`WindowReadyEvent` timer payload、
  `CloudWholeTaskFoundationContractTest` numeric-HWND fixture correction。

### 20.3 Verification

- PASS：CR271 `mvn -q compile`；Cloud `mvn -q compile`。
- PASS：CR271
  `mvn -q '-Dtest=WindowRuntimeObserverClosureContractTest,TurnProtocolValidatorContractTest' test`
  （2 + 19 tests；包含 local idle/pending clear/旧intent不复活及prebattle duplicate/restart/miss）。
- PASS：Cloud focused named tests：`CloudWholeTaskObserverPolicyContractTest`、
  `CloudWholeTaskFoundationContractTest#sameWindowObserverAdmissionYieldsToTaskAndWakesOnlyAfterTaskParks`、
  `DialogOptionTurnContractTest#rememberedRouteRealProducerPublishesExactWindowBoundActionAndStateConsumesIt`。
  authority日志证明 queued same-window task在observer释放后 `61ms` handoff，ready waiter在publish后 `28ms` bounded wake；
  DialogService真实producer生成并存储exact-window action，随后canonical state CAS consume成功。
- 全量 `CloudWholeTaskFoundationContractTest` 曾运行：31/32非错误路径执行，但一个既有 interrupt测试仍按旧“返回empty”
  期望，当前 typed checkpoint抛 `TaskStopRequestedException`，故不作为本卡通过证据；本卡要求的精确authority方法单独PASS。
- 未运行 runtime/application/server/Task/UI/live capture/input；未写用户基线；未做额外Git mutation。

等待父级按第16-20节逐文件 final source review。

<!-- TRUE_EOF: TURN-40F REPAIR-3 WHOLE-CARD SOURCE+TEST DELIVERED AFTER-REVIEW-2 HUYGENS WAITING-PARENT-SOURCE-REVIEW NOT-APPROVED 2026-07-20 -->

## 21. Repair #3 Parent Source Review #3 - Delivery Rejected

- Review verdict：`P0=0 / P1=3 / P2=1 / REPAIR REQUIRED`。Review #2 的 authoritative cleanup、pre-battle CAS
  与短 authority lease 已闭合，但当前 observer 仍未与只读本地 post-696 Runner 保持完整业务等价；TURN-41 与用户测试继续 blocked。
- **P1-1 parked preparation priority/stale semantics changed：** Cloud
  `CloudWholeTaskObserver.prepareParkedDialog`（约 251-276）无条件先执行 `prepareTaskInterest(...)`，再尝试 route；本地
  `WindowTaskRunner.shouldPrioritizeTaskDialogInterest`（约 1923-1928）只允许 `XIULUO_V2 + OPTION +
  XIULUO_ENTER_BATTLE interest` 抢先，其余均为 route-first、task-interest fallback。当前实现会让 Wubei task interest
  抢占同时存在的 route transfer。它还把任意 action 超过 3 秒统一清除，未保留基线 route operation/target/intent
  “already current” fence。返修必须迁入同一优先级与 operation/target/intent current/stale 规则；不得用统一 TTL 替代。
- **P1-2 target-map gate transaction can strand open truth：** `probePathing`（约 367-375）先用一次 HTTPS local operation
  CAS 打开 gate，再用第二次 action登记 `WUBEI_ENTER_BATTLE` interest；第二步 BUSY/FAILED/UNKNOWN 时 gate 已永久 opened，
  后续 probe因 `gateOpenedAtMs>0` 不再重试，任务可能永远没有 dialog interest。返修必须以现有 HTTPS turn v1 下一个
  原子 typed local operation同时完成 gate-open + exact interest registration，或提供等价的可重试单一提交语义；不得新增 endpoint/store。
- **P1-3 route-memory settlement missing：** 本地 `WindowTaskRunner.updatePathingFromLocation` 在 terminal publish 后依次调用
  `settlePendingTransferChoiceMemory` 与 `settlePendingWorldMapRouteResultMemory`（约 2451-2453），ARRIVED/STOPPED_AWAY
  分别写 route/world-map success/failure。Cloud observer当前只 `publishPathingTerminal(...)`（约 421/458），全类没有
  `recordRouteDialogChoiceSuccess/Failure`、`recordWorldMapRouteResultSuccess/Failure` 或等价 settlement caller；学习槽会遗留且
  下一次路线决策不等价。返修必须保持 transition publish -> transfer settlement -> world-map settlement顺序及 intent/source fence。
- **P2-1 acceptance still proves helpers/direct producer, not observer closure：**
  `CloudWholeTaskObserverPolicyContractTest` 只覆盖 relevance/interest/suppression/classify；
  `rememberedRouteRealProducerPublishes...` 直接调用 `DialogService`，未从 observer 的 local fact + visible frame 跑到 stored
  action/ready wake。返修须增加 observer production tests，覆盖 Wubei route-vs-task priority、Xiuluo priority、stale/current
  intent、atomic gate+interest failure/retry、两类 terminal settlement、wrong scope、stop/pause与 miss-no-truth。

当前状态：`REPAIR #3 SOURCE ACTIVE / PARENT REVIEW #3 0-3-1 / REPAIR REQUIRED / NOT READY FOR USER TEST`。

<!-- TRUE_EOF: TURN-40F REPAIR-3 PARENT-REVIEW-3 P0-0-P1-3-P2-1 REPAIR-REQUIRED HUYGENS-REPAIR NOT-READY-FOR-TEST 2026-07-20 -->

WORKER STATUS: 2026-07-20 Parent Review #3 第21节已收到；Repair #3 正按 parked preparation、原子 gate+interest、PATHING terminal 双 settlement 与 production observer tests 继续返修。

<!-- TRUE_EOF: TURN-40F REPAIR-3 REVIEW-3 ACKNOWLEDGED SOURCE-ACTIVE 2026-07-20 -->

## 22. Repair #3 Canonical SOURCE+TEST Delivery #4

- Delivery status: `WHOLE-CARD SOURCE+TEST DELIVERED / WAITING PARENT REVIEW / NOT APPROVED`.
- Parked preparation parity:
  - `CloudWholeTaskObserver.prepareParkedDialog` now gives task-interest priority only to
    `XIULUO_V2 + OPTION + XIULUO_ENTER_BATTLE`; every other path is route-first then task fallback.
  - route prepared action remains current only on exact `ROUTE_TRANSFER + target + intentId`; it is not expired by a
    blanket 3-second TTL. Task actions retain the 3-second verified freshness plus visible/stationary and 1-second
    stale-reprepare cooldown fence.
- Atomic Wubei gate transaction:
  - added `WHOLE_TASK_TARGET_MAP_GATE_OPEN_AND_DIALOG_INTEREST`; one existing HTTPS turn v1 local operation invokes
    synchronized `WindowRuntimeContext.openOrdinaryEnterBattleTargetMapGateAndUpdateDialogInterest`.
  - uncertain retries are idempotent: the first call owns gate-open CAS; every same-operation retry re-installs the exact
    `WUBEI_ENTER_BATTLE` interest. No endpoint, transport, store, local bus or second submission was added.
- PATHING terminal settlement:
  - production order is now `publish PATHING_TERMINAL -> consume/settle transfer choice -> consume/settle world-map route`.
  - added typed `WHOLE_TASK_PENDING_TRANSFER_CHOICE_CONSUME` and
    `WHOLE_TASK_PENDING_ROUTE_OUTCOME_CONSUME`; both consume only while local authoritative active pathing still matches
    exact `intentId + source`, then return the consumed typed carrier once.
  - transfer ARRIVED/STOPPED_AWAY writes route-dialog success/failure; world-map writes success/failure and preserves
    `intent-replaced` / `target-replaced` abandonment semantics.
- CR271 write set:
  - `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWholeTaskRuntimeResult.java`
  - `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
  - `src/test/java/com/bot/dhxy/window/runtime/WindowRuntimeObserverClosureContractTest.java`
  - `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
- Cloud write set:
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWholeTaskRuntimeResult.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
  - `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverPolicyContractTest.java`
- Verification (all exit 0):
  - CR: `mvn -q -Dtest=WindowRuntimeObserverClosureContractTest,TurnProtocolValidatorContractTest,TurnCoreProtocolGoldenJsonTest test`
  - CR: `mvn -q -DskipTests compile`
  - Cloud: `mvn -q -Dtest=CloudWholeTaskObserverPolicyContractTest,TurnCoreProtocolGoldenJsonTest,CloudWholeTaskFoundationContractTest#awaitNewerHonorsAfterSequenceAndTimeoutWithoutExpiringFacts+otherWindowEventsAreExcludedFromLatestAndAwait+awaitNewerPathingTerminalOrPreparedRouteThrowsOnAStopRequest+sameWindowObserverAdmissionYieldsToTaskAndWakesOnlyAfterTaskParks test`
  - Cloud: `mvn -q compile`
  - tests cover Xiuluo-only priority, Wubei route-first, current/stale intent, atomic gate failure/retry/idempotence,
    wrong intent/source non-consume, single consume, ARRIVED/STOPPED_AWAY transfer/world-map meanings, wrong window,
    stop, timeout-no-truth, same-window FIFO task priority and bounded wake. Observer loop's production pause gate remains
    the same `context.isPauseRequested()` skip alongside stop; no runtime/UI/input was executed.
- Protocol equality:
  - `TurnLocalOperation.java` SHA256 `8335C48F352AB2BEAD39545F3271CB22BA1128D6A1EF60F950080BD5AD0C4839`
  - `TurnProtocolValidator.java` SHA256 `0FB029325EF6DD8803752E189498027FD9EB9EFD0DFCEED395A1298E25E65735`
  - `TurnWholeTaskRuntimeResult.java` SHA256 `916D5BD98C40554D4E301C8DCCDA5179FD4D96FACAE6E668B13AD92AAA8E1B61`
  - all above are byte-identical in CR271 and Cloud; shared golden test SHA256
    `440BF80477312F6B5D6BAF21171379606B7EB059F106182696F7A7D9B7EFE7B6` is also identical.
- Thin-client/baseline evidence:
  - CR client permanent Service count = 4: `BagService`, `GiveItemService`, `QuestManagerService`, `UICleanerService`.
  - read-only `D:\mavenProject\DHXY` branch `codex/baseline-696a12b0`, HEAD
    `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; `WindowTaskRunner.java` = 3156 lines, SHA256
    `256C4AB16D33C2E30355AAC790A8D84F9E875C16F2C3742BB42D34AAF670FDFF`.
  - baseline remained dirty exactly as found; no write/build/checkout was performed there. Existing dirty/untracked content
    in both writable repositories was preserved; no reset/clean/checkout/commit was run.

<!-- TRUE_EOF: TURN-40F REPAIR-3 CANONICAL-SOURCE-TEST-DELIVERY-4 WAITING-PARENT-REVIEW NOT-APPROVED 2026-07-20 -->

## 23. Parent Source Review #4 And Full Baseline Gap Audit - Delivery Rejected

- Verdict：`P0=0 / P1=3 / P2=1 / REPAIR REQUIRED`。第22节的 prepare priority、原子 gate+interest、协议
  byte equality 与单次 settlement语义接受；整卡仍未达到“当前本地 workspace全部逻辑等价迁移”。
- **P1-1 terminal settlement is transition-only and cannot recover：** Cloud
  `CloudWholeTaskObserver.probePathing` 当前只在 `nextState != current.state` 且首次进入 ARRIVED/STOPPED_AWAY时依次
  publish/consume transfer/consume world-map。任一 settlement HTTPS turn 为 BUSY/FAILED/UNKNOWN后，后续 terminal probe
  因 state不再变化永远不重试，pending learning slot遗留。只读基线 `WindowTaskRunner.updatePathingFromLocation` 仅 event
  publish是transition-only，两个settlement每次terminal observation都执行。返修必须保持event去重但让未消费slot在后续
  terminal probe可重试；仍须保持publish -> transfer -> world-map顺序与exact intent/source CAS。
- **P1-2 enabled startup-window preparation was deleted without replacement：** 本地基线
  `TaskStartupWindowPreparationService + DefaultWindowTaskStartupInitializer` 在任务开始前执行map tracking、Alt+5购物栏、
  Alt+6可见层和flying/startup guards；`bot.dhxy.task-startup-preparation-enabled=true`。CR客户端删除了这两类，属性仍保留
  但零consumer；Cloud production对Alt+5/Alt+6/map tracking也零caller（仅旧remote DTO残留 `PRESS_ALT_6` enum）。这会让
  fresh runtime跳过已验证启动准备。返修需由Cloud持有顺序/判定，通过现有HTTPS turn v1下最小raw capture/typed input
  mechanics执行，client不得恢复本地业务Service/initializer或第二协议。
- **P1-3 manual map-survey capability was knowingly removed, not migrated：** 只读基线
  `MainWindowController` 有完整地图样本、四边界、中心锚点、投影测试、修正点、撤销等UI；`MapSurveyService`约1650行，
  持久化 `config/map_camera_bounds.json` 并含interpolation/local-fit。CR删除全部UI入口和类；Cloud只有
  `ObjectiveTextRecognizer` 注释“new-map calibration must ingest here”，实际没有ingest/persist/undo/project production path。
  权威迁移矩阵早已裁决标定math/persistence应上Cloud、client只留raw capture/mouse/manual interaction。返修须恢复完整用户
  功能并按该边界迁移，不得把OCR/标定算法塞回client，也不得增加第五个local Service。
- **P2-1 observer test remains helper-level：** 新 `CloudWholeTaskObserverPolicyContractTest` 仍直接调用static priority/
  settlement helpers，未实例化observer从local fact/visible capture经过production probe到ready state。至少补一个真实observer
  harness覆盖失败后terminal settlement retry，以及route/task prepare到stored action+ready wake；现有authority测试可复用。
- 扩大物理盘点：本地基线268个production Java中有30个basename在CR/Cloud均无同名peer，共13362行。大部分已由
  Cloud renamed owner或remote lifecycle替代；当前明确未承接的用户能力是startup-window preparation与map-survey。窗口diagnostic
  dead classes、local OCR sidecar与old local runner不要求恢复；角色判定已有Cloud metadata/startup authority承接。

当前状态：`TURN-40F REPAIR #4 REQUIRED / PARENT REVIEW #4 0-3-1 / NOT READY FOR USER TEST / TURN-41 BLOCKED`。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 FULL-BASELINE-GAP-AUDIT P0-0-P1-3-P2-1 REPAIR-4-REQUIRED NOT-READY-FOR-TEST 2026-07-20 -->

## 24. Repair #4 Canonical Worker Acknowledgement

- `SOURCE ACTIVE / REPAIR #4 / OWNER RETAINED`。
- 已完整重读第23节、post-delivery equivalence gap audit、`AGENTS.md`、`DHXY_CONTEXT.md`、权威计划及两份迁移矩阵；接受四项返修合同与边界。
- `D:\mavenProject\DHXY` 当前 dirty workspace继续作为严格只读业务等价权威；仅写CR client与Cloud仓。
- 无已批准业务差异；按当前只读基线等价迁移。不得启动runtime/application/server/Task/UI/live capture/input，不得自标Approved。

<!-- TRUE_EOF: TURN-40F REPAIR-4 SOURCE-ACTIVE OWNER-RETAINED BASELINE-READONLY 2026-07-20 -->

## 25. Repair #4 PLAN-CONTRACT BLOCKER - MapSurvey Command/Pointer Facts Missing From HTTPS Turn v1

- 状态：`PLAN-CONTRACT BLOCKED / SOURCE PARTIAL / OWNER RETAINED / NOT DELIVERED / NOT APPROVED`。
- 已完成且保留的独立源码修复：Cloud `CloudWholeTaskObserver.probePathing` 现在只在首次terminal transition发布
  `PATHING_TERMINAL`，但每次仍为terminal的probe均按 `pending transfer -> pending world-map` 顺序重试exact
  intent/source CAS settlement；BUSY/FAILED/UNKNOWN不再因observer state已terminal而永久吞掉后续重试机会。
- 唯一协议阻断：完整MapSurvey需要client UI把`operation + mapName + selected exact window`提交给Cloud，并在Cloud
  发起3秒manual preparation后从该exact window读取实时pointer sample。当前单一HTTPS turn v1没有这两个typed carrier：
  1. `TurnTaskStartRequest.java:5-9` 只有`startRequestId/taskCodes/taskMaxRuns/failurePolicy`；
  2. `TurnTaskCode.java:3-8` 只有五个固定运行任务，不能把operation/mapName编码进taskCode而破坏closed enum；
  3. `TurnInputAction.java:3-14` 只有写入型mouse/key动作，没有pointer read fact；
  4. `TurnLocalOperation.java:3-48` 没有exact-window pointer/manual sample operation/result；
  5. 只读基线 `MapSurveyService.java:148-293` 的boundary/center/correction均在3秒等待后调用
     `MouseInfo.getPointerInfo()`，correction还必须在等待后重读当前坐标，不能用start metadata或恒值替代。
- 禁止的不等价绕路：不得把`operation/mapName`拼入task code字符串、不得让client保留OCR/投影/标定持久化、不得
  用固定/旧pointer坐标、不得新增第二endpoint/bus/store、不得把MapSurvey恢复为第五个本地Service。
- **唯一待用户/父级裁决：** 是否允许在现有HTTPS turn v1内做加法式closed typed扩展：
  `TurnTaskStartRequest`携可选`TurnMapSurveyCommand(operation,mapName)`，并新增一个
  `MAP_SURVEY_POINTER_SAMPLE` typed local operation/result（exact HWND + screen-absolute pointer + sampledAt）；仍使用同一
  endpoint、同一action/exchange、同一local dispatcher，Cloud持有OCR/math/persistence/undo/project，client仅UI/raw
  capture/pointer sample/typed mouse move。若不批准此v1扩展，本卡第23节P1-3无法无业务差异闭合。
- 因该精确blocker，已按合同停止继续Java扩展；startup preparation与production harness尚未实施，本轮未运行Maven。

<!-- TRUE_EOF: TURN-40F REPAIR-4 PLAN-CONTRACT-BLOCKED MAPSURVEY-V1-TYPED-CARRIER-DECISION SOURCE-PARTIAL NOT-DELIVERED NOT-APPROVED 2026-07-20 -->

## 26. Parent Plan-Contract Repair #4A - MapSurvey Typed Turn Session Approved

- 父级审核第25节后批准**同一 `/api/v1/client/turn`、同一 v1 envelope、同一 action/outcome exchange 内的
  加法式强类型扩展**；解除 `MAPSURVEY-V1-TYPED-CARRIER-DECISION` blocker。不得新增 endpoint、transport、bus、
  第二业务store或第五个本地Service。
- 第25节提出的两个carrier不足以闭合完整调用链，且不得把manual survey伪装成普通task start。固定表示为：
  1. `TurnRequest` 新增可选 `TurnMapSurveyCommand(commandId, operation, mapName)` 与可选
     `mapSurveyResultAckId`；它们与 `taskStartRequest`/`continuation` 的合法组合由validator明确约束。
  2. `TurnResponse` 新增可选 `TurnMapSurveyResult(commandId, status, message, mapName, projectedPoint...)`；
     `ACCEPTED/COMPLETED/FAILED` 必须exact command correlation。Cloud在收到ack前可重发terminal result，client
     只向UI交付一次并在下一turn回执，transport不确定时不得重复执行命令。
  3. 新增 `MAP_SURVEY_POINTER_SAMPLE` typed local operation/result，返回exact selected HWND对应的
     screen-absolute pointer与`sampledAt`；不得读取旧pointer或恒值。
  4. UI manual command使用现有WindowTurnLoop的专用one-shot command attachment，不进入
     `CloudTurnTaskRuntime`普通task queue，不新增 `TurnTaskCode`，且活动task/window冲突时fail closed。
- Cloud新增唯一MapSurvey session/service owner：命令去重、3秒manual preparation、等待后坐标重读、map label OCR、
  boundary/center/correction、>500px拒绝、interpolation/local-fit、project/test/undo及tenant-scoped
  `map_camera_bounds.json`兼容持久化均在Cloud；client仅UI、exact-HWND raw capture、pointer sample与typed mouse move。
- 双仓所有共享protocol文件必须byte-identical，补strict JSON/golden/duplicate/ack/retry/wrong-window/active-task
  negative tests；不得只测record/static helper。现有六份learned-memory与当前`map_camera_bounds.json`实际数据导入仍是
  TURN-41 cutover gate，本轮只证明schema兼容，不得写或复制只读基线数据。
- Repair #4继续完成第23节startup preparation与production observer harness；完成前仍
  `SOURCE ACTIVE / NOT READY FOR USER TEST / NOT APPROVED`。

<!-- TRUE_EOF: TURN-40F PARENT-PLAN-CONTRACT-REPAIR-4A MAPSURVEY-TYPED-TURN-SESSION-APPROVED SOURCE-ACTIVE NOT-READY-FOR-TEST 2026-07-20 -->

## 27. Parent Review #4 Asset-Byte Addendum - Six Production Tracker Templates Stale

- Review计数修正为 `P0/P1/P2=0/4/1`。生产引用资源逐SHA对照发现：只读本地当前workspace修改了
  `images/template/task/wubei_tracker_anchor.png`及五张
  `images/template/wubei/wubei_title_*_yellow.png`；Cloud真实`TaskTrackerPanelService`/`WubeiTask`引用同路径，
  但Cloud resources仍全部是旧字节。
- 当前本地SHA前缀分别为：anchor `16AF2EE4AB14`、宝象`53A18485EDC4`、殿前`D396375B3B3C`、魁星
  `5E4D1E44CF25`、三藏`864A6F3CC26D`、智斗`2A6B2940E815`；Cloud当前分别为`EC421B6BDF55`、
  `FC8141BF10CD`、`03D15FF17E75`、`0C0CEFA58944`、`E3A4873AFB7D`、`10770C299FDF`。
- Repair #4须将**当前只读基线字节**迁入Cloud真实consumer路径并给出完整SHA；不得修改基线、不得激活
  `tracker-panel/templates/wubei`无caller副本或复制第二算法。补资源加载/consumer contract test；其它98个本地生产
  image引用未发现target缺图，唯一无target的`images/temp/latest_vision.png`是runtime输出而非模板。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 ASSET-BYTE-ADDENDUM P0-0-P1-4-P2-1 SIX-WUBEI-TEMPLATES-STALE REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 27. Repair #4A Canonical Worker Acknowledgement

- `ACK PARENT PLAN-CONTRACT REPAIR #4A / SOURCE ACTIVE / OWNER RETAINED`。
- 接受第26节固定合同：MapSurvey使用同一`TurnRequest/TurnResponse`专用command/result/ack attachment，不进入普通
  `TurnTaskStartRequest`或`CloudTurnTaskRuntime` queue；新增exact pointer sample typed local operation；terminal result
  ack前可重发、client UI只交付一次、transport不确定不得重复执行业务命令。
- 不新增`TurnTaskCode`、endpoint、transport、store或第五个local Service；Cloud持有全部MapSurvey业务/OCR/math/
  persistence/undo/project，client仅UI/raw capture/pointer sample/typed mouse move。
- `map_camera_bounds.json`本轮只做schema兼容测试，不导入或复制只读基线实际数据；无已批准业务差异。

<!-- TRUE_EOF: TURN-40F REPAIR-4A ACK SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## 28. Parent Review #4 Obsolete-Asset Addendum - Four Deleted Baseline Templates Still Packaged

- Review计数修正为 `P0/P1/P2=0/4/2`。当前只读本地workspace已经删除下列旧生产资产，但Cloud仍将其打包：
  `images/template/task/xiuluo_tracker_title.png`、
  `images/template/wubei/source_tracker_dark_thunder.png`、
  `images/template/wubei/source_tracker_probe_two_links.png`、
  `images/template/xiuluo/Snipaste_2026-06-23_12-57-46.png`。
- Cloud production源码对四个旧路径均为零引用；当前修罗真实consumer只引用
  `images/template/xiuluo/xiuluo_tracker_title.png`。Cloud旧`images/template/wubei/README.md`仍声称保留
  `source_tracker_dark_thunder.png`，与当前基线删除裁决相冲突。
- Repair #4须在再次证明双仓production零引用后，从Cloud runtime resources删除这四个旧资产并同步或删除失真README；
  不得误删新的修罗标题资产，不得以目录中存在旧图片作为fallback。补资源manifest/零引用合同证据。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 OBSOLETE-ASSET-ADDENDUM P0-0-P1-4-P2-2 FOUR-DELETED-BASELINE-TEMPLATES-STILL-PACKAGED REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 29. Parent Review #4 Method-Level Addendum - Wrapped Route Click Center Not Migrated

- Review计数修正为 `P0/P1/P2=0/5/2`。当前只读本地workspace的
  `GameTextLineOcrService.correctWrappedRouteContinuationCenter(...)`在既有route destination匹配完成后，使用上一行
  右侧cluster下边界切掉黄色圆点/上行污染，并在保留的下一行黄字像素中重算点击中心；该逻辑专门修复换行尾字
  OCR框被圆点拉高导致的错误点击。
- Cloud `DecisionEngine.findExpectedRouteDestination(...)`虽会拼接上一行prefix与左侧continuation文本，但仍直接用
  continuation OCR box的`averageCenterX/averageCenterY`；direct与packed两个生产入口均未调用等价的yellow-pixel center
  correction。Cloud源码注释仍只声明`696a12b0`等价，不能覆盖用户当前workspace的post-696修复。
- Repair #4须在Cloud唯一route OCR owner中吸收当前基线的**匹配后几何校正**，保持文本匹配、wrap选择、fallback顺序
  和absolute/window-relative换算不变；不得在client恢复OCR算法。必须用repo-local wrapped-route testcase验证并产出
  标记图，显示原OCR框、校正后黄字框和最终点击点；同时补production入口合同测试。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 WRAPPED-ROUTE-CENTER-ADDENDUM P0-0-P1-5-P2-2 CLOUD-CLICK-GEOMETRY-MISSING REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 30. Parent Review #4 P1-2 Contract Expansion - Full Startup Initializer Chain

- Review计数保持 `P0/P1/P2=0/5/2`，但第23节P1-2不得缩成三个快捷键。当前只读基线
  `TaskStartupWindowPreparationService`完整机械/判定合同还包括：
  1. 同一Alt+1 panel/session内依次确认`map tracking checked`、`auto close map checked`、`open fly checked`；
     未勾选才点，checked/unchecked模板阈值0.95，关闭后best-effort复核。
  2. Alt+U的`expand`必须保持unchecked；五环用background HWND probe，只有明确看到错误状态才进入窄foreground
     correction，UNKNOWN不得猜测点击。
  3. Alt+5后验证`blacklist_shopping`、Alt+6后验证`blacklist_crowd`，各最多3次、500ms复查、确认后1000ms overlay
     fadeout；顺序固定Alt+5再Alt+6。
  4. flying/unflying status guard、exact HWND/window-scoped capture、interrupt/stop检查及前台原子修正边界必须保留。
- `DefaultWindowTaskStartupInitializer`业务顺序也必须由Cloud唯一owner承接：startup先同步identity/position（navigation
  stress只identity）；clean queue/five-ring queue preparation只执行一次；left-top status leader/member probe在UI准备前；
  debug/auto-battle/member跳过；五环走background-first并成功后mark done；五倍在full prep关闭时仍做Alt+5/Alt+6窄guard；
  普通leader失败只warn、不硬停，interrupt仍终止。
- client只实现exact-HWND shortcut/capture、foreground checkbox click等typed mechanics，Cloud持有上述task/role/order/
  ready/UNKNOWN/fallback语义。必须补leader/member/debug/five-ring/background UNKNOWN/queue-idempotence合同测试；不得恢复
  client Service/initializer或把UNKNOWN当unchecked。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 STARTUP-FULL-CHAIN-CONTRACT-EXPANDED P0-0-P1-5-P2-2 REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 31. Parent Review #4 P1 Addendum - Enabled Live Team-Role Preflight Has No Turn Owner

- Review计数修正为 `P0/P1/P2=0/6/2`。当前只读本地`application.yml`明确
  `bot.team.role-detection-enabled=true`；`WindowTaskRunner.resolveTaskTypeBeforeStart(...)`在五环及leader-only任务
  dispatch前调用`TeamRoleDetectionService.detectCurrentRole(...)`，再用`TaskTeamAssignmentPolicy`改派：member的五环/
  修罗/五倍 -> `AUTO_BATTLE`；solo/unknown的修罗/五倍 -> `UNKNOWN`不派发；五环允许solo/unknown。
- CR client仍保留同一enabled配置和team hover/tooltip/panel坐标，但`TeamRoleDetectionService`已删除且这些配置零consumer。
  `NativeWindowRegistrationMapper`/batch builder仅按窗口顺序写LEADER/MEMBER；`TurnWindowMetadata.role`把该注册metadata当成
  authority，Cloud `CloudStartupGateAuthority`只parse该字符串。Cloud旧`DecisionEngine TEAM_ROLE_TOOLTIP`没有现有HTTPS turn
  production caller。因此窗口顺序与真实游戏队伍角色不一致时会执行错误任务。
- Repair #4须在现有task-start turn lifecycle中、Cloud factory实际dispatch前恢复live role preflight：client只做exact-HWND
  hover/mouse/capture和必要的typed快捷键；Cloud做tooltip白/紫text-like分布、队长数字ID OCR与bound title/current player ID
  比对、status-strip deviation和任务改派。普通startup在tooltip OCR miss/status-deviation时保持UNKNOWN且**不打开Alt+T**；
  Alt+T marker fallback只属于显式debug/force路径，panel关闭时序保持当前基线。
- 检测结果须成为本次Cloud run/task assignment authority，并通过既有single HTTPS turn同步client短期WindowRole或以等价
  typed result保持下一turn一致；不得新增第二endpoint/store或恢复client业务Service。补leader/member/solo/UNKNOWN、错误注册
  顺序、OCR miss不Alt+T、member改派及leader-only拒绝的production合同测试。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 LIVE-TEAM-ROLE-PREFLIGHT-ADDENDUM P0-0-P1-6-P2-2 ENABLED-CONFIG-ZERO-CONSUMER REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 31. Repair #4 Startup Full-chain Acknowledgement

- `ACK PARENT REVIEW #4 SECTION 30 / SOURCE ACTIVE / OWNER RETAINED`。
- 接受完整 `TaskStartupWindowPreparationService + DefaultWindowTaskStartupInitializer` 等价合同：同一 Alt+1
  session 三项 checked、Alt+U expand unchecked、五环 background-first 且 UNKNOWN fail-closed、Alt+5 后 Alt+6
  各三次复核/fadeout、flying guard、exact HWND/interrupt/atomic correction，以及 identity/position、queue 幂等、
  leader/member/debug/auto-battle/task-kind 的完整初始化顺序。
- Cloud继续作为唯一业务 policy/order owner；client只增加既有 HTTPS turn v1 下的 typed exact-window mechanics，
  不恢复本地 startup Service/initializer，不新增 endpoint/store/第五 Service，不修改只读 baseline。

<!-- TRUE_EOF: TURN-40F REPAIR-4 SECTION-30-ACK SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## 32. Repair #4 Live Role Preflight Acknowledgement

- `ACK LIVE-TEAM-ROLE-PREFLIGHT ADDENDUM / SOURCE ACTIVE / OWNER RETAINED`。
- task-start在Cloud factory materialize前执行exact-window live role preflight；Cloud持有tooltip白紫分布、leader数字ID
  OCR、bound title/current player ID比对、status deviation与`TaskTeamAssignmentPolicy`改派，注册顺序role仅作为待校验输入。
- 普通startup的OCR miss或status deviation保持`UNKNOWN`且不触发Alt+T；Alt+T仅保留显式debug/force。检测结果成为
  本run assignment authority并通过同一HTTPS turn typed事实保持后续一致，不新增endpoint/store/本地Service。
- 接受member五环/修罗/五倍改派`AUTO_BATTLE`、solo/unknown修罗/五倍拒绝、五环solo/unknown允许的基线规则；
  client仍只执行exact-HWND hover/input/capture mechanics，baseline继续严格只读。

<!-- TRUE_EOF: TURN-40F REPAIR-4 LIVE-ROLE-PREFLIGHT-ACK SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## 33. Parent Review #4 Addendum - Prepared Action Freshness And Run Ownership

- `P0/P1/P2=0/7/2 / REPAIR #4 SOURCE ACTIVE / NOT READY FOR USER TEST`。
- **P1-7 current-baseline gap:** 当前只读 baseline 的 `WindowTaskRunner.refreshTaskTrackerPreparationSignal(...)`
  已增加五环 tracker 后台准备的双阶段 fence：准备前不得处于战斗、active/UNKNOWN pathing、probe 或已有
  active intent；准备结束后还必须确认同一 `windowId/taskType/taskCode/taskRunId/RunningTaskHandle/taskIndex/stopToken`
  owner 仍有效，且没有更高优先级 prepared action。CR client/Cloud 当前没有等价的 production fence，旧 run 的
  延迟观察可污染新 run 或覆盖更高优先级动作。
- 同一 baseline 的 `refreshTaskDialogInterestPreparationSignal(...)` 已增加 stale prepared-action 重发布规则：
  仅当同一 HWND 的可见 dialog 类型仍匹配、prepared verification 已过期、pathing 为 `ARRIVED/STOPPED_AWAY`
  且非 probe，才允许清除并重新准备；`WUBEI_ENTER_BATTLE` 另有严格的 no-active-intent、pathing `NONE`、
  OPTION 同类型窄例外；全链有 1000ms republish cooldown。当前 Cloud observer/prepared state 未承接这些条件。
- Repair #4 必须由 Cloud 持有 freshness/priority/run-owner policy，client 仅通过现有 HTTPS turn v1 上报 exact-HWND
  combat/pathing/dialog/stop typed facts并执行必要 mechanics；不得恢复本地 Runner 业务判断、第五 Service、第二
  endpoint/store/bus。补 stale-old-run discard、combat/pathing/probe suppress、higher-priority preserve、stationary
  reprepare、moving/visible-mismatch/cooldown retain、Wubei no-pathing narrow exception 的 production-entry 合同测试。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 PREPARED-ACTION-FRESHNESS-RUN-OWNERSHIP P0-0-P1-7-P2-2 REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 34. Repair #4 Prepared-action Ownership Acknowledgement

- `ACK SECTION 33 / SOURCE ACTIVE / OWNER RETAINED`。
- 五环tracker preparation在Cloud production入口执行准备前与准备后同run owner双fence；exact
  `windowId/taskType/taskCode/taskRunId/handle identity/taskIndex/stop token`任一变化均丢弃旧run延迟结果，且不覆盖
  higher-priority prepared action。combat、active/UNKNOWN pathing、probe、active intent均抑制准备。
- task-dialog stale republish只接受same HWND visible dialog type仍匹配、verification过期、非probe且stationary terminal；
  `WUBEI_ENTER_BATTLE`仅保留no active intent + pathing NONE + OPTION同类型窄例外，全链保持1000ms cooldown。
- Cloud持有freshness/priority/run-owner policy；client继续只提供现有HTTPS turn v1 exact-HWND typed facts/mechanics，
  不恢复local Runner业务判断，不新增Service/endpoint/store/bus，不修改或运行只读baseline。

<!-- TRUE_EOF: TURN-40F REPAIR-4 SECTION-33-ACK SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## 35. Parent Review #4 Addendum - Five-Ring Post-Combat Tracker Intent Cleanup

- `P0/P1/P2=0/8/2 / REPAIR #4 SOURCE ACTIVE / NOT READY FOR USER TEST`。
- **P1-8 current-baseline gap:** 当前只读baseline的`FiveRingTaskV2.waitPathing(...)`在tracker pathing期间观察到
  combat、且`AutoCombatService.handleCombatTick(...)`完成恢复后，会先调用
  `clearTrackerPathingIntentAfterCombatRecovery()`，按
  `wuhuan-v2:prepared-tracker-panel-click:`优先、`wuhuan-v2:tracker-green-click:`回退，只清理本次五环tracker
  产生的旧pathing signal，再进入`SYNC_TASK_PANEL`。
- Cloud当前`FiveRingTaskV2.waitPathing(...)`在同一recovery分支直接返回`SYNC_TASK_PANEL`；Cloud observer只存在
  Wubei/Xiuluo的source-prefix cleanup，没有五环等价生产调用。旧tracker intent因此可跨combat recovery继续存活，
  并被后续observer/task path误认为当前寻路authority。
- Repair #4须在Cloud唯一五环task policy内，通过既有`CloudWholeTaskRuntimeLocalServiceClient`/typed pathing clear
  边界实现相同的prefix顺序与只清匹配语义；client不得恢复task判断、不得无条件clear exact-window全部pathing、不得
  新增endpoint/store/bus。补production-path测试：prepared-prefix命中、legacy-prefix回退、foreign/route intent保留、
  未经历combat路径不clear、stop/pause期间不发新cleanup。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 FIVE-RING-POST-COMBAT-TRACKER-INTENT-CLEANUP P0-0-P1-8-P2-2 REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 36. Parent Review #4 Addendum - Maintenance Broadcast Raw-ROI Convergence

- `P0/P1/P2=0/9/2 / REPAIR #4 SOURCE ACTIVE / NOT READY FOR USER TEST`。
- **P1-9 current-baseline gap:** 当前只读baseline的`TaskMaintenanceService.handleMaintenanceBroadcast(...)`只在
  window-relative `260,373..378,413`固定raw ROI内，按threshold `0.85`依次匹配
  `maintenance_heal_all_repair_raw.png`与`maintenance_repair_confirm_raw.png`并原子点击；两张均miss即no-action。
  同一baseline的`DialogService`已让`CLICK_BUSINESS_OPTION`直接返回not-found，并删除整块dialog绿/黄洗图、repair-giveup
  及fixed-miss full fallback。
- Cloud当前`TaskMaintenanceService.handleMaintenanceBroadcast(...)`仍调用
  `DialogHandleRequest.handleMaintenanceBroadcastOption(...allowFullMaintenanceBroadcastFallback)`；Cloud
  `DialogService.handleBusinessOption(...)`及`CloudUiCleanerPort.cleanLightweightInterruptions(...)`仍使旧full-dialog
  business-option识别/点击生产可达。这会偏离当前用户验证后的raw strip行为，并可能把普通OPTION误识别为维护选项。
- Repair #4须由Cloud保留当前raw ROI/template顺序/threshold/miss语义，client只做exact-HWND capture与typed原子点击；
  删除或封闭`TaskMaintenanceService`和轻量清窗真实链上的旧green/yellow full-dialog fallback，不得保留“保险fallback”。
  补production-entry测试：两个raw模板各自hit、双miss no-action、普通OPTION不点击、wrong HWND/stop/pause fail-closed；
  旧fallback helper零production caller/资源零引用须有source guard。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-4 MAINTENANCE-BROADCAST-RAW-ROI-CONVERGENCE P0-0-P1-9-P2-2 REPAIR-4-SOURCE-ACTIVE 2026-07-20 -->

## 37. Repair #4 Worker ACK - Maintenance Broadcast Raw-ROI Convergence

- 已完整读取第36节；`TURN-40F Repair #4` owner retained。
- 本轮将按当前只读 baseline 收敛 Cloud production：仅保留 exact-HWND 固定 raw ROI
  `260,373..378,413`、threshold `0.85`、heal-all 后 repair-confirm 的顺序匹配与原子点击；双 miss
  为 no-action。
- 将封闭或删除 `TaskMaintenanceService`、`DialogService` 与轻量清窗真实链上的 full-dialog green/yellow
  fallback，并补 raw hit/miss、普通 OPTION、wrong HWND、stop/pause 与 source guard 测试；不保留保险 fallback。

<!-- TRUE_EOF: TURN-40F REPAIR-4-WORKER-ACK SECTION-36 MAINTENANCE-RAW-ROI OWNER-RETAINED SOURCE-ACTIVE 2026-07-20 -->

## 38. Repair #4 Canonical WHOLE-CARD SOURCE+TEST Delivery

- Delivery status：`WHOLE-CARD SOURCE+TEST DELIVERED / WAITING PARENT REVIEW / NOT APPROVED`。
- `D:\mavenProject\DHXY` 全程严格只读，branch=`codex/baseline-696a12b0`，HEAD=
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；未写、未构建、未切分支。CR client永久
  Service仍exact four：`BagService`、`GiveItemService`、`QuestManagerService`、`UICleanerService`。
- 未新增endpoint、transport、action consumer、local bus、业务store、`TurnTaskCode`或第五个Service；全部新增carrier均走
  既有`/api/v1/client/turn` v1 request/action/result/ack链。

### 38.1 Review #4 closure

- Observer：terminal transition首次保持`publish -> transfer settlement -> world-map settlement`；以后只要local fact仍为
  exact terminal且pending slot未消费，每次probe继续按`transfer -> world-map`重试，BUSY/FAILED/UNKNOWN不吞槽。
  production harness实际运行`probePathing`并证明首次settlement失败、后续同terminal成功；实际运行`probeAttention`并从
  supplied OPTION frame经真实`DialogService`生成stored prepared action、`PREPARED_ACTION_READY`及bounded wake。
- Startup：Cloud唯一policy owner承接Alt+1同session三项checked、Alt+U expand unchecked、五环background-first且
  UNKNOWN不猜点、Alt+5后Alt+6各三次/500ms复核/1000ms fadeout、flying guard、interrupt/exact-window与initializer
  identity/position、queue幂等、role/task分支顺序；client仅typed exact-HWND mechanics。
- MapSurvey：同一turn新增command/result/ack及exact pointer sample，Cloud session按commandId去重、terminal保留到exact
  ACK；Cloud持有label OCR、四边界、center、project、correction record/test/undo、3秒manual preparation、等待后坐标重读、
  >500px拒绝、interpolation/local-fit/corrections与tenant-scoped schema兼容。真实client loop测试证明不确定transport保留
  immutable command，显式重启原样重发，ACCEPTED不清，terminal只完成UI future一次，ACK成功后才开放下一命令。
- Live role：Cloud factory materialize前执行live preflight；MEMBER改派AUTO_BATTLE，UNKNOWN对修罗/五倍拒绝、对五环
  允许，检测结果进入同run context authority；普通OCR miss/status deviation不触发Alt+T。
- Prepared ownership：Cloud observer保留五环prepare前/后同run owner fence、combat/pathing/probe/active-intent suppress、
  higher-priority preserve；task-dialog stale reprepare保留same HWND/type、stationary terminal、1000ms cooldown及Wubei
  no-intent/pathing NONE/OPTION窄例外。五环combat recovery按prepared tracker prefix优先、legacy prefix回退，只清匹配intent。
- Maintenance：Cloud生产入口固定window-relative raw ROI `260,373..378,413`、threshold `0.85`，heal-all后
  repair-confirm顺序匹配并原子点击；双miss no-action。普通`CLICK_BUSINESS_OPTION`直接not-found，maintenance与轻量清窗
  旧full-dialog green/yellow fallback已封闭且source guard为零production caller。
- Route视觉：Cloud唯一OCR owner迁入wrapped continuation retained-yellow-pixel center correction；文本匹配、wrap选择、
  fallback及坐标换算未改。可检查标记图
  `images/test-cases/world-map-route/wrapped-continuation/wrapped-route-center-marked.png`（原OCR框、校正黄字框、最终点）。
- Assets：六张五倍生产模板已按只读baseline字节迁入Cloud；删除四张双仓production零引用旧资源，并在
  `process-resources`精确清理incremental `target/classes`旧副本；未启用未引用tracker-panel副本。

### 38.2 Complete Repair #4 write set

- CR271 protocol/turn/local/UI：`TurnRequest`、`TurnResponse`、`TurnMapSurveyCommand`、`TurnMapSurveyResult`、
  `TurnMapSurveyPointerSample`、`TurnLocalOperation`、`TurnWholeTaskRuntimeArguments`、`TurnProtocolValidator`、
  `LocalServiceStepDispatcher`、`WholeTaskRuntimeLocalOperationExecutor`、`MapSurveyPointerLocalOperationExecutor`、
  `WindowTurnLoop`、`TurnModeGuard`、`WindowTaskControlService`、`MainWindowController`。
- CR271 tests：`TurnCoreProtocolGoldenJsonTest`、`TurnProtocolValidatorContractTest`、
  `LocalServiceStepDispatcherContractTest`、`WindowTurnLoopContractTest`、`MainWindowControllerSourceGuardTest`及既有
  `WindowRuntimeObserverClosureContractTest`扩展。
- Cloud protocol/assembly/runtime：matching protocol files、`CloudTurnExchange`/turn handler assembly、
  `CloudWholeTaskRuntimeLocalServiceClient`、`CloudWholeTaskObserver`、`CloudTaskStartupPreparationService`、
  `CloudMapSurveySessionService`、`CloudTurnTaskRuntime`、`CloudTurnTaskFactory`、`CloudMapSurveyService`、
  `CloudTeamRolePreflightService`、`CloudDialogPreparedActionState`。
- Cloud business owners：`DecisionEngine`（wrapped route center）、`DialogService`、`TaskMaintenanceService`、
  `CloudUiCleanerPort`、Cloud `FiveRingTaskV2`及相应startup/role/prepared integration wiring。
- Cloud build/resources：`pom.xml`；六张`images/template/task|wubei`五倍生产模板；新
  `images/template/xiuluo/xiuluo_tracker_title.png`；删除
  `images/template/task/xiuluo_tracker_title.png`、两张`wubei/source_tracker_*`、
  `images/template/xiuluo/Snipaste_2026-06-23_12-57-46.png`；同步`wubei/README.md`。
- Cloud tests：`CloudWholeTaskObserverProductionHarnessTest`、`CloudWholeTaskObserverPolicyContractTest`、
  `CloudTaskStartupPreparationServiceContractTest`、`CloudTurnTaskRuntimeContractTest`、
  `CloudMapSurveyCalibrationContractTest`、`CloudMapSurveySessionServiceContractTest`、
  `FiveRingCombatRecoveryCleanupContractTest`、`DecisionEngineRouteOcrContractTest`、
  `WubeiProductionTemplateAssetContractTest`、`DialogOptionTurnContractTest`、matching protocol golden tests。

### 38.3 Verification and immutable evidence

- PASS：CR271 `mvn -q -DskipTests compile`；Cloud `mvn -q compile`。
- PASS：CR271 named tests：`TurnCoreProtocolGoldenJsonTest,TurnProtocolValidatorContractTest,
  LocalServiceStepDispatcherContractTest,MainWindowControllerSourceGuardTest,WindowTurnLoopContractTest`。
- PASS：Cloud named tests：`CloudWholeTaskObserverProductionHarnessTest,CloudWholeTaskObserverPolicyContractTest,
  CloudTaskStartupPreparationServiceContractTest,CloudTurnTaskRuntimeContractTest,
  FiveRingCombatRecoveryCleanupContractTest,CloudMapSurveyCalibrationContractTest,
  CloudMapSurveySessionServiceContractTest,WubeiProductionTemplateAssetContractTest,TurnCoreProtocolGoldenJsonTest`，
  `DecisionEngineRouteOcrContractTest#wrappedDestinationUsesRetainedYellowPixelsForTheProductionClickCenter`，以及
  Section 36 五个`DialogOptionTurnContractTest` raw hit/miss/ordinary-option/wrong-scope/source-guard方法。
- 已知非本卡断言：整类旧`TaskMaintenanceTurnContractTest`仍含turn-native fixture未绑定
  `TaskExecutionContextHolder`，在进入其旧summon-skill分支时抛`LegacyTaskExecutionTurnContextProvider no context`；本卡
  Section 36 production方法均使用exact bound context单独PASS，未把该整类旧fixture误报为通过。
- Repair #4变更的共享协议文件双仓byte-identical：`TurnLocalOperation.java`
  `539E79B655B22C474A4421FAEC849829AC0EAD54A26CC2D68FB4A6408E645228`；
  `TurnWholeTaskRuntimeArguments.java`
  `93922E15F5168647C1DA5E6E1CB7D3B8BAE3508EF6322BF66F4741AF396F994F`；
  `TurnProtocolValidator.java`
  `9BFD75B5FF2612643F605668CB875F933B748044BBFD47E18CA850C2C21EF9CA`。
- 六张baseline/Cloud生产资源SHA-256依次为：anchor
  `16AF2EE4AB14677AED174BD962553A4238CAF18C7F907F1D3702B6696C269865`、宝象
  `53A18485EDC4FC8EE265D8EF928B9C71A30CE5BA5B2D9E2C141F2D12FE90F626`、殿前
  `D396375B3B3C70E46D23C38DB5EAB33801D8E48B59622650F40CCB22E165514D`、魁星
  `5E4D1E44CF254A5031866FC903510F41D30B1FE2A057030E4411C1BFC236EF85`、三藏
  `864A6F3CC26D24D975B63AAB12B9EBFE65E6CD409FDBE3496565790F75A42DEE`、智斗
  `2A6B2940E8155390D5341673718EBE0826A7A5A140323D2D99479F4AB15B35A2`；新修罗标题SHA
  `F3230B87FF5477511249610AE0E14F8356151932A6AB8CCB4B345D19C20A1F82`。
- 未运行runtime/application/server/Task/UI/live capture/input；未reset/clean/checkout/commit，三仓原有dirty/untracked均保留。

等待父级按第23-38节逐文件final source review；本worker不自行标Approved。

<!-- TRUE_EOF: TURN-40F REPAIR-4 WHOLE-CARD-SOURCE-TEST-DELIVERED WAITING-PARENT-REVIEW NOT-APPROVED 2026-07-20 -->

## 39. Parent Review #5 - Repair #4 Delivery Blocked

- Review：`P0/P1/P2=0/2/0 / BLOCKED / REPAIR #5 REQUIRED / NOT READY FOR USER TEST`；Huygens whole-card owner 保留。
- **P1-1 visual replay contract 未满足：**`DecisionEngineRouteOcrContractTest::wrappedDestinationUsesRetainedYellowPixelsForTheProductionClickCenter`
  在测试内生成 `220x64` 黑底/白矩形 synthetic mask，并通过 reflection 直调 private
  `findExpectedRouteDestination(...)`。交付目录只有 367-byte marked PNG 与 232-byte synthetic mask，没有原始游戏
  world-map screenshot；测试也没有经过 public `verifyWorldMapRouteDestination(...)` 或 turn production entry。
  这不满足 AGENTS visual replay 规则与本卡第29节冻结的 raw testcase、production入口、原OCR框/校正框/最终点击点证据。
  Repair #5须从只读baseline现有 `images/test-cases/world-map-route/raw` 选择能复现换行尾字污染的真实截图，复制到
  Cloud repo-local testcase，走真实production入口并产出可审阅marked output；不得以程序生成色块替代。
- **P1-2 五环战后cleanup测试未执行生产行为：**`FiveRingCombatRecoveryCleanupContractTest`仅用
  `Files.readString(...)`/`indexOf(...)`检查源码字符串；未执行`FiveRingTaskV2::waitPathing`，未驱动
  `CloudWholeTaskRuntimeLocalServiceClient`，因此没有证明prepared-prefix命中、legacy-prefix回退、foreign/route保留、
  未经历combat不clear、stop/pause不发cleanup。Repair #5须补 production-path harness，断言真实调用顺序、次数、prefix、
  terminal/boolean分支与零误清；source guard可保留但不能作为唯一验收。
- 其余 Repair #4 production review继续冻结；本结论已足以拒绝当前delivery。修复后须同卡 canonical whole-card
  re-delivery，父级再完成剩余逐文件终审。不得运行runtime/application/server/Task/UI/live capture/input。

<!-- TRUE_EOF: TURN-40F PARENT-REVIEW-5 BLOCKED P0-0-P1-2-P2-0 REPAIR-5-REQUIRED VISUAL-RAW-PRODUCTION-REPLAY-MISSING FIVE-RING-CLEANUP-SOURCE-GUARD-ONLY HUYGENS-OWNER TURN41-BLOCKED 2026-07-20 -->

## 40. Repair #5 WIP Observed

- 状态：`SOURCE ACTIVE / OWNER RETAINED / NOT DELIVERED`。Cloud
  `DecisionEngineRouteOcrContractTest.java`已产生新SHA `AE74C0C6...`，并新增真实raw testcase
  `world-map-route-wrapped-changan-raw.png`（SHA-256 `FF70BC72...`，46,349 bytes）。
- 当前测试仍为探索态：扫描只读baseline raw目录、调用public`verifyWorldMapRouteDestination(...)`后故意
  `throw new AssertionError(results)`；尚未绑定repo-local raw、形成稳定断言或更新marked output。
- `FiveRingCombatRecoveryCleanupContractTest.java`仍为旧SHA `CABFC32D...`，P1-2 production harness尚无字节变化。
  Java writer活动中，父级不运行Maven；Review #5 `0/2/0`与TURN-41 blocked不变。

<!-- TRUE_EOF: TURN-40F REPAIR5-WIP SOURCE-ACTIVE OWNER-RETAINED RAW-TESTCASE-COPIED ROUTE-TEST-EXPLORATORY FIVE-RING-HARNESS-NOT-STARTED NO-MAVEN TURN41-BLOCKED 2026-07-20T18:17:00-04:00 -->

## 41. Repair #5 Canonical WHOLE-CARD SOURCE+TEST Re-delivery

- Delivery status：`WHOLE-CARD SOURCE+TEST DELIVERED / WAITING PARENT REVIEW / NOT APPROVED`；Review #5
  两项 P1 均已按 production-path 合同返修，Repair #4 其余 production/write set 保持第38节冻结内容不变。
- Repair #5 未修改任何 production Java/protocol/resource；只新增/修改 Cloud focused tests 与 repo-local replay
  assets。`D:\mavenProject\DHXY` 始终只读，HEAD=
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，未构建、未切分支、未写入；原有 dirty/untracked 全部保留。

### 41.1 P1-1 real visual production replay closure

- 从只读 baseline 真实 raw
  `images/test-cases/world-map-route/raw/images__temp__world_map_route_online_dry_run__20260529_144456__case_04_raw.png`
  复制为 Cloud repo-local
  `images/test-cases/world-map-route/wrapped-continuation/world-map-route-wrapped-fengchao-six-raw.png`；两者
  SHA-256 均为 `FF70BC724B9EC34CF6F22C100A1913AE5A382FA4FAEDD019AFE9CF8B456957D4`，46,349 bytes。
- `DecisionEngineRouteOcrContractTest::wrappedDestinationUsesRetainedYellowPixelsForTheProductionClickCenter`
  不再生成 synthetic mask、不再 reflection 调 private helper、不再扫描 baseline 绝对目录。测试以真实 raw bytes 调用
  public production `DecisionEngine.verifyWorldMapRouteDestination(...)`；仅用既有 loopback test fixture代替当前未运行的
  external OCR sidecar，packed OCR明确返回空，业务断言只接受 production direct-row结果。
- 测试从同一真实 `WASH_YELLOW` pixels复核 retained bounds，断言最终中心等于黄字 retained-pixel中心且不等于受污染
  continuation OCR box average，并产出
  `world-map-route-wrapped-fengchao-six-marked.png`：蓝框=原OCR continuation box、绿框=校正黄字框、红色十字=
  production最终点击点。marked SHA-256=
  `A4485856A9691E9729E31530E25DCCA45ED7BAC4A4B5C8E5A2AA4CE227CCEEF1`。

### 41.2 P1-2 Five-ring production cleanup harness closure

- `FiveRingCombatRecoveryCleanupContractTest`已从`Files.readString/indexOf` source-only guard替换为真实 production
  harness：绑定 exact `TaskExecutionContext`/`TurnGameClient`，reflection仅用于进入 private production phase方法本身，
  实际执行`FiveRingTaskV2.waitPathing(...)`、`CloudWholeTaskRuntimeLocalServiceClient`与
  `WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX` typed operation/result链。
- 7个测试覆盖：prepared prefix命中只发一次；prepared boolean false后按序legacy fallback；foreign/world-map route
  两次窄prefix miss后保留；未观察combat为零cleanup；stop为零调用；pause bounded阻塞且stop后零调用；首操作
  `UNKNOWN`或`STOPPED` terminal均不猜测fallback；active combat与recovery仍`IN_COMBAT`均不提前cleanup。成功恢复才进入
  `SYNC_TASK_PANEL`，boolean true/false与non-EXECUTED terminal分支均被实际执行。

### 41.3 Exact Repair #5 write set and verification

- `src/test/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngineRouteOcrContractTest.java` SHA-256
  `9F5A2C2E9C536E0F77CF21C977836173B99AC2A286E303F99C997B632CD6836D`。
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingCombatRecoveryCleanupContractTest.java`
  SHA-256 `23DC12ADB240242E7654F8F7EB1A1A642A60297D3802FCAB0B6DEABE3615A81D`。
- 两张 testcase PNG及SHA见41.1；调查期`contact-inspect.png`与旧synthetic mask/marked均已删除，不作为交付资产。
- PASS：Cloud
  `mvn -q "-Dtest=DecisionEngineRouteOcrContractTest#wrappedDestinationUsesRetainedYellowPixelsForTheProductionClickCenter,FiveRingCombatRecoveryCleanupContractTest" test`
  （1个真实视觉replay + 7个production cleanup tests）。PASS：Cloud `mvn -q compile`。
- `mvn -q -DskipTests compile`被本仓`maven-enforcer-plugin require-tests-enabled`按合同拒绝，未绕过；随后使用允许的
  `mvn -q compile`成功。未运行runtime/application/server/Task/UI/live capture/input；未reset/clean/checkout/commit。

等待父级继续整卡final source review；本worker不自行标Approved。

<!-- TRUE_EOF: TURN-40F REPAIR-5 WHOLE-CARD-SOURCE-TEST-DELIVERED WAITING-PARENT-REVIEW NOT-APPROVED P1-1-REAL-RAW-PUBLIC-PRODUCTION-REPLAY-CLOSED P1-2-WAITPATHING-PRODUCTION-HARNESS-CLOSED 2026-07-20T18:31:36-04:00 -->

## 42. Parent Verification #6 - Repair #5 Focused Gates Passed

- Parent本人逐文件复核Repair #5两项返修，focused结论：`P0/P1/P2=0/0/0`。原Review #5的P1-1、P1-2均已关闭。
- 世界地图测试使用repo-local真实raw，经public production `DecisionEngine.verifyWorldMapRouteDestination(...)`执行；marked图同时标出原continuation OCR框、retained-yellow bounds及production最终点击点，未再使用synthetic mask或private production helper。
- 五环cleanup测试实际进入production `FiveRingTaskV2.waitPathing(...)`并驱动typed `WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX`链；prepared/legacy/foreign、terminal、无combat、pause/stop及active-combat分支均有真实调用次数、顺序和零误清断言。
- Parent复跑PASS：`mvn -q "-Dtest=DecisionEngineRouteOcrContractTest#wrappedDestinationUsesRetainedYellowPixelsForTheProductionClickCenter,FiveRingCombatRecoveryCleanupContractTest" test`；Parent复跑PASS：Cloud `mvn -q compile`。
- 本节仅关闭Repair #5 focused gates，不等同于TURN-40F whole-card final approval。Repair #4其余完整写集仍由父级逐文件终审，Huygens owner保留；TURN-41 learned-memory/`map_camera_bounds` cutover gate继续BLOCKED，当前仍`NOT READY FOR USER TEST`。
- 未运行runtime/application/Task/UI/live capture/input；只读baseline未写、未构建、未切分支。

<!-- TRUE_EOF: TURN-40F PARENT-VERIFICATION-6 REPAIR5-FOCUSED-GATES-PASSED P0-0-P1-0-P2-0 WHOLE-CARD-FINAL-REVIEW-CONTINUES OWNER-RETAINED TURN41-BLOCKED NOT-READY-FOR-USER-TEST 2026-07-20T18:32:11-04:00 -->

## 43. Parent Whole-Card Review #7 - Role Preflight Baseline Drift

- Review：`P0/P1/P2=0/2/0 / BLOCKED / REPAIR #6 REQUIRED / NOT READY FOR USER TEST`；Repair #5 focused `0/0/0`保持有效，Huygens whole-card owner保留。
- **P1-1 live UNKNOWN丢失既有窗口角色回退。**只读dirty baseline
  `WindowTaskRunner.resolveTaskTypeBeforeStart(...)` lines 2884-2900 在live role为UNKNOWN时读取
  `windowContext.getRole()`，只要既有角色是LEADER/MEMBER便作为`assignmentRole`再交给
  `TaskTeamAssignmentPolicy.resolveTaskForRole(...)`。Cloud
  `CloudTurnTaskRuntime.detectLiveRole(...)` lines 363-377把检测异常直接折成UNKNOWN，随后
  `materializeAssignedEntry(...)` lines 380-391把该UNKNOWN直接传给`rolePreflight.assign(...)`，完全未使用已经通过
  `taskStartAuthorityError(...)`校验的`TurnWindowMetadata.windowRole()`。结果是metadata=LEADER、一次tooltip/OCR miss时，
  baseline继续修罗/五倍，Cloud却返回null跳过任务；这改变了用户当前基线的任务派发语义。
- **P1-2 tooltip retry mechanics与当前基线不等价。**只读dirty baseline
  `TeamRoleDetectionService.hoverAndCaptureTeamTooltipWithRetries(...)` lines 342-355 在两次probe之间保留
  `TaskSleep.sleep(1000)`；`hoverAndCaptureTeamTooltipOnce(...)` lines 371-391通过配置半径`x=8/y=6`随机化
  `(644,91)` hover点。Cloud `CloudTeamRolePreflightService.detect()` lines 56-79两次失败后立即重试，
  `hoverAndCapture(...)` lines 99-109固定点击精确`(644,91)`。这移除了当前基线专门用于坏边缘像素和startup focus settling的
  可靠性策略，可能把可恢复miss误判为SOLO/UNKNOWN并进一步改变派发。
- Repair #6条件：恢复UNKNOWN时使用已校验window metadata role的baseline fallback；恢复tooltip probe配置等价值
  `500ms hover wait + 最多2次 + 两次之间1000ms + x/y随机半径8/6`，仍不得打开Alt+T。新增production-path测试至少证明
  `(live UNKNOWN, metadata LEADER, requested WUBEI/XIULUO_V2)`保留原任务、metadata MEMBER改派AUTO_BATTLE、metadata UNKNOWN仍拒绝；
  并证明两次probe的坐标均在冻结半径内且失败重试前存在1000ms wait。不得改共享协议或新增endpoint/store。
- 本轮仅源码审计，未运行额外Maven；未运行runtime/application/server/Task/UI/live capture/input；baseline严格只读。

<!-- TRUE_EOF: TURN-40F PARENT-WHOLE-CARD-REVIEW-7 BLOCKED P0-0-P1-2-P2-0 REPAIR6-REQUIRED ROLE-UNKNOWN-METADATA-FALLBACK-MISSING TOOLTIP-RETRY-RANDOMIZATION-AND-DELAY-MISSING OWNER-RETAINED TURN41-BLOCKED NOT-READY-FOR-USER-TEST 2026-07-20T18:37:11-04:00 -->

## 44. Repair #6 Worker Acknowledgement

- `ACK PARENT-HUYGENS-TURN40F-REPAIR6-ROLE-PREFLIGHT-20260720-183711 / SOURCE ACTIVE / OWNER RETAINED`。
- live role仅在`UNKNOWN`时回退到已通过task-start authority校验的metadata `LEADER/MEMBER`；真实`SOLO`、
  `LEADER`、`MEMBER`检测结果保持优先，metadata `UNKNOWN`不猜测。
- tooltip保持每次`MOVE -> WAIT 500 -> CAPTURE`、最多2次、首次miss后独立`WAIT 1000`，并在
  `(644,91)`的`x±8/y±6`范围落点；正常startup不打开Alt+T。仅修改Cloud production与focused tests。

<!-- TRUE_EOF: TURN-40F REPAIR-6 ACK-PARENT-HUYGENS-TURN40F-REPAIR6-ROLE-PREFLIGHT-20260720-183711 SOURCE-ACTIVE OWNER-RETAINED NOT-APPROVED 2026-07-20T18:41:38-04:00 -->

## 45. Repair #6 Canonical WHOLE-CARD SOURCE+TEST Re-delivery

- Delivery status：`WHOLE-CARD SOURCE+TEST DELIVERED / WAITING PARENT REVIEW / NOT APPROVED`；Parent Review #7
  两项 P1 已返修，Repair #5 focused `0/0/0`与第38节其余whole-card写集继续冻结有效。
- `ack_parent_message=PARENT-HUYGENS-TURN40F-REPAIR6-ROLE-PREFLIGHT-20260720-183711`；owner retained。

### 45.1 Role assignment fallback

- `CloudTurnTaskRuntime.materializeAssignedEntry(...)`现在只在live role为`UNKNOWN`时读取已经通过
  `taskStartAuthorityError(...)`校验的`TurnWindowMetadata.windowRole()`；仅非UNKNOWN的`LEADER/MEMBER`成为
  assignment role。live `SOLO/LEADER/MEMBER`始终优先，metadata `UNKNOWN`仍保持`UNKNOWN`，不新增猜测或第二authority。
- assignment role同时进入`CloudTeamRolePreflightService.assign(...)`和本run绑定的
  `CloudTaskServiceMetadata.windowRole`：metadata LEADER保留`WUBEI/XIULUO_V2`，metadata MEMBER改派
  `AUTO_BATTLE`，metadata UNKNOWN拒绝leader-only任务但仍允许五环，与当前只读baseline
  `WindowTaskRunner.resolveTaskTypeBeforeStart(...)`一致。

### 45.2 Tooltip mechanics parity

- `CloudTeamRolePreflightService.detect()`最多执行2次tooltip probe；每次production TurnAction保持
  `MOVE_MOUSE -> WAIT 500ms -> exact-window CAPTURE`。首次missing/failed probe后通过同一现有turn transport执行独立
  `WAIT 1000ms`再进入第二次；成功不额外等待。
- 每次hover点以window-relative中心`(644,91)`独立随机，范围严格为`x±8/y±6`；tooltip/status ROI、OCR/白紫分布、
  status deviation与normal-startup no-Alt+T语义未改。未新增endpoint、协议、store、Service或action consumer。

### 45.3 Exact Repair #6 write set and verification

- Cloud production `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
  SHA-256 `E79BD0C1361E82E717101CAD84C3F89F06A4A6AA71DCE28C9DD90FB3CDEA77E2`。
- Cloud production `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudTeamRolePreflightService.java`
  SHA-256 `F96C66DB6E60B5D67DF82B0875DAD30C40F3E90C50A1D0C49CAF762A76D66EFE`。
- Cloud test `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java`
  SHA-256 `E20E068F1FD0B55B992CC69491B3EFDF9B0DD6A7AE89BF81252FE6F72D34BEA4`。
- Cloud test `src/test/java/com/yueyunfe/dhxy/cloudbrain/CloudTeamRolePreflightServiceContractTest.java`
  SHA-256 `83F6F85B8B528C0967451293845730A0FBB2F4145A1E4517DF8B0E958195B42B`。
- PASS：Cloud
  `mvn -q "-Dtest=CloudTurnTaskRuntimeContractTest,CloudTeamRolePreflightServiceContractTest" test`；
  `CloudTurnTaskRuntimeContractTest` 29/29，`CloudTeamRolePreflightServiceContractTest` 1/1。
- PASS：Cloud `mvn -q compile`。`D:\mavenProject\DHXY`全程只读，HEAD仍为
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；未构建、未切分支、未写入。未运行
  runtime/application/server/Task/UI/live capture/input；未reset/clean/checkout/commit。

等待父级whole-card final source review；本worker不自行标Approved。

<!-- TRUE_EOF: TURN-40F REPAIR-6 WHOLE-CARD-SOURCE-TEST-DELIVERED WAITING-PARENT-REVIEW NOT-APPROVED ROLE-UNKNOWN-METADATA-FALLBACK-CLOSED TOOLTIP-RETRY-RANDOMIZATION-CLOSED 2026-07-20T18:45:38-04:00 -->

## 46. Parent Verification #8 - Repair #6 Focused Gates Passed

- Parent逐行审查Repair #6 production与tests，focused=`P0/P1/P2=0/0/0`。Review #7两项P1均关闭。
- `CloudTurnTaskRuntime.assignmentRole(...)`只在live UNKNOWN时读取已校验metadata role；LEADER/MEMBER回退与baseline一致，真实live role始终优先，metadata UNKNOWN不猜测。production-path tests覆盖LEADER保留WUBEI/XIULUO_V2、MEMBER改派AUTO_BATTLE、UNKNOWN拒绝leader-only并允许五环。
- `CloudTeamRolePreflightService.detect()/hoverAndCapture()/waitBeforeTooltipRetry()`恢复两次probe、每次500ms hover wait、首次miss后1000ms独立WAIT、中心`(644,91)`的x8/y6随机范围且normal startup无Alt+T；测试驱动真实TurnAction并逐步断言。
- Parent复跑PASS：`mvn -q "-Dtest=CloudTurnTaskRuntimeContractTest,CloudTeamRolePreflightServiceContractTest" test`（29+1）；Parent复跑PASS：Cloud `mvn -q compile`。
- 本节仅关闭Repair #6 focused gates；TURN-40F其余Repair #4 whole-card写集终审继续，Huygens owner保留。TURN-41仍BLOCKED，当前`NOT READY FOR USER TEST`。
- 未运行runtime/application/server/Task/UI/live capture/input；baseline严格只读。

<!-- TRUE_EOF: TURN-40F PARENT-VERIFICATION-8 REPAIR6-FOCUSED-GATES-PASSED P0-0-P1-0-P2-0 TESTS-30-PASS COMPILE-PASS WHOLE-CARD-FINAL-REVIEW-CONTINUES OWNER-RETAINED TURN41-BLOCKED NOT-READY-FOR-USER-TEST 2026-07-20T18:47:12-04:00 -->

## 47. Parent Whole-Card Audit - TURN-41 Data Cutover Contract Clarified

- 只读baseline六份dirty runtime JSON已按真实属主分类：三个`config` canonical stores与三个旧`data`
  sidecar/compatibility stores；后者不得盲拷贝为Cloud第二权威store。
- canonical事实：dialog=22 entries；vision=460 entries、600 NPC samples、1000 target samples；world-map route=80
  entries。`transfer_choice_memory.json`的14个key已全部并入dialog canonical store，`ocr_roi_memory.json`只作legacy fallback。
- `map_camera_bounds.json`=11327 bytes，SHA-256
  `4428F7F998C11AC787A27C1DEE98D186DEB97D9A24307F2E1BD4224FB8E8A74B`；Cloud仓和当前exact scoped target均无已验收副本。
- Cloud storage必须使用实际启动参数`tenantId/userId/stateRoot`的哈希私有scope。未确定exact scope、未备份Cloud现存目标、
  未完成schema-compatible merge/import及计数/SHA验收前，TURN-41保持BLOCKED。TURN-40F whole-card终审与Huygens owner不变。
- 本节仅修计划合同与数据门证据；未改Java，未运行Maven/runtime/application/server/Task/UI/capture/input，baseline零写入。

<!-- TRUE_EOF: TURN-40F PARENT-WHOLE-CARD-AUDIT TURN41-DATA-CUTOVER-CONTRACT-CLARIFIED THREE-CANONICAL-THREE-LEGACY EXACT-SCOPE-REQUIRED TURN41-BLOCKED OWNER-RETAINED NO-JAVA-NO-RUNTIME 2026-07-20T18:52:12-04:00 -->

## 48. Parent Whole-Card Review #9 - MapSurvey Correction Algorithm Drift

Verdict：`P0/P1/P2=0/1/0 / BLOCKED / REPAIR #7 REQUIRED`。Repair #6 focused结论仍为`0/0/0`；本节是
Repair #4全写集终审新发现。

### P1-1 Cloud correction is not baseline-equivalent

- 当前只读baseline `MapSurveyService.CameraBounds.correctionAt(...)`（SHA-256
  `0555A3A6F53FFE25E56543C1692EF28947323AF49542BC62ABEA50B367AD4418`，lines 930-1128）对exact pin使用
  `actualRel - current basePointAt(...)`，所以边界或center重标定后仍以当前投影为基准；非exact点先按map distance取样，再按
  220px screen cluster筛选，做加权仿射拟合，拒绝少于3点、singular matrix及weighted residual >95。
- Cloud `CloudMapSurveyService.CameraBounds.correctionAt(...)`（SHA-256
  `5815CAF4B8F0606CABC289D1D86217BE68C5B739A21BF1A940EDA63FC34FE99D`，lines 524-549）exact直接返回历史
  `errorX/errorY`，非exact仅对误差做inverse-distance average；没有current-base重算、screen cluster、affine gradient、
  singular-fit或residual gate。边界/center变化、空间梯度或脏样本会产生与baseline不同的点击坐标。
- `CloudMapSurveyCalibrationContractTest.localFitRequiresThreeNearbySamplesAndRejectsOver500PixelLegacyNoise()`
  （SHA-256 `C9E0596A1F2F5D069DCBA7932445A8F97471833E2D557E9DEF45F7E82335587C`，line 41）只给对称样本，
  两种算法都偶然返回`(10,10)`；且未覆盖exact pin在boundary/center变化后的重算、非恒定仿射场、screen outlier、
  singular fit和residual rejection。

### Repair #7 frozen scope and acceptance

- Huygens保留原owner。只允许返修Cloud `CloudMapSurveyService`唯一校正实现及
  `CloudMapSurveyCalibrationContractTest`（必要时同目录既有map-survey production contract test）；不得新增第二算法、store、
  endpoint/protocol或client业务逻辑。
- 必须逐语义迁入baseline current-base exact delta、map-nearby+screen-cluster、weighted affine solve与singular/residual fail-closed。
  测试必须分别制造边界/center变化、非恒定affine gradient、screen-space离群簇、singular和高residual样本，并通过Cloud
  production owner断言最终delta/拒绝结果。
- canonical whole-card re-delivery后父级复审；当前不运行Maven。TURN-41保持BLOCKED / NOT READY FOR USER TEST。

<!-- TRUE_EOF: TURN-40F PARENT-WHOLE-CARD-REVIEW-9 P0-0-P1-1-P2-0 REPAIR7-REQUIRED MAP-SURVEY-CORRECTION-BASELINE-DRIFT OWNER-RETAINED TURN41-BLOCKED NO-MAVEN-NO-RUNTIME 2026-07-20T18:59:42-04:00 -->

## 49. Parent Whole-Card Review #9 Addendum - Map Label Asset Closure

Updated verdict：`P0/P1/P2=0/2/1 / BLOCKED / REPAIR #7 REQUIRED`。

### P1-2 Current baseline map label is absent from the real Cloud consumer

- 当前只读baseline `images/template/map_label`共有62张。Cloud production `MiniMapRecognizer.readMapLabelTemplates()`
  line 806-808唯一读取`templates/map_label`，该目录只有61张；缺少baseline 2026-07-19新增的`铁匠屋.png`，SHA-256
  `8BF1850437D74B6783CA32B10092EE45C0534D331BA56D1FE5673BB2254D2CFC`。其它61张逐文件SHA与baseline一致。
- 缺失会使`CloudMapSurveyService.recognizeCurrent()`在铁匠屋无法通过现有saved-label/native template路径稳定识别，进而阻断
  boundary/center/correction/undo/project。Repair #7须把该字节放入唯一真实consumer，并以62/62 manifest+SHA测试验收。

### P2-1 Duplicate zero-reference map-label resource root

- Cloud `src/main/resources/images/template/map_label`另打包61张重复资源；全仓production Java对`map_label`仅两处：
  Cloud scoped `map_label_samples.json`与`MiniMapRecognizer`的`templates/map_label`。重复目录没有production caller，增加包内
  双副本并继续混淆资产属主。
- Repair #7须在零引用/manifest test保护下删除整个重复目录，不得向两处都补`铁匠屋.png`。

### Repair #7 addendum

- 原数学返修写集不变；追加真实consumer `src/main/resources/templates/map_label/铁匠屋.png`、删除
  `src/main/resources/images/template/map_label/*.png`重复树及相应asset contract test。不得修改baseline/client Java。
- Huygens owner保留，等待对两个parent message均STATUS EVENT ACK后继续；当前不运行Maven，TURN-41继续BLOCKED。

<!-- TRUE_EOF: TURN-40F PARENT-WHOLE-CARD-REVIEW-9-ADDENDUM P0-0-P1-2-P2-1 REPAIR7-REQUIRED MAP-LABEL-TIEJIANGWU-MISSING DUPLICATE-RESOURCE-ROOT OWNER-RETAINED TURN41-BLOCKED NO-MAVEN-NO-RUNTIME 2026-07-20T19:04:42-04:00 -->

## 50. Parent Audit - Repair #7 Communication And Activity Stale

- Huygens对`PARENT-HUYGENS-TURN40F-REPAIR7-MAPSURVEY-CORRECTION-20260720-185942`与
  `PARENT-HUYGENS-TURN40F-REPAIR7-MAPLABEL-ASSET-20260720-190442`连续两轮无STATUS EVENT ACK。
- 截至19:14 EDT，`CloudMapSurveyService.java`、`CloudMapSurveyCalibrationContractTest.java` SHA/mtime未变，
  `templates/map_label/铁匠屋.png`仍不存在，重复目录仍61张；超过10分钟无事件和源码变化。
- 状态=`COMMUNICATION_STALE + ACTIVE_STALE / OWNER RETAINED`。不另派Worker、不催促、不运行Maven；收到有效ACK后再标恢复。
- Review #9=`0/2/1`与Repair #7写集不变；TURN-41继续BLOCKED / NOT READY FOR USER TEST。

<!-- TRUE_EOF: TURN-40F PARENT-AUDIT REPAIR7 COMMUNICATION-STALE ACTIVE-STALE OWNER-RETAINED P0-0-P1-2-P2-1 TURN41-BLOCKED NO-MAVEN-NO-RUNTIME 2026-07-20T19:14:43-04:00 -->

## 51. Parent Audit - Protected Baseline Foreign Worktree Change

- 只读baseline dirty count由93变94；新增`D:\mavenProject\DHXY\.codex-audit-CQWebGame\`，创建时间
  `2026-07-20T23:36:50Z`，内部有独立`.git`、CQWebGame源码/资产，共205项。
- 该目录与DHXY/CR271业务写集无关，视为用户/外部并行工作；不删除、不移动、不纳入baseline逻辑等价审计。
- DHXY branch/HEAD仍为`codex/baseline-696a12b0@696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；
  Repair #7 `COMMUNICATION_STALE + ACTIVE_STALE`、Review #9=`0/2/1`及TURN-41 BLOCKED均不变。

<!-- TRUE_EOF: TURN-40F PARENT-AUDIT BASELINE-WORKTREE-CHANGE DIRTY-94 FOREIGN-CQWEBGAME-PROTECTED REPAIR7-STALE TURN41-BLOCKED 2026-07-20T19:39:43-04:00 -->

## 52. Parent Audit - Second Protected Baseline Foreign Worktree

- baseline dirty count 94 -> 95；新增`D:\mavenProject\DHXY\.codex-audit-legend-web\`，创建于
  `2026-07-20T23:43:18Z`，内部含独立`.git`、client/server/dist/node_modules等12,368项。
- 与DHXY/CR271业务无关，标记FOREIGN/PROTECTED，不删除、不移动、不纳入迁移；先前CQWebGame目录同样保持保护。
- baseline branch/HEAD不变；Repair #7 stale、Review #9=`0/2/1`与TURN-41 BLOCKED不变。

<!-- TRUE_EOF: TURN-40F PARENT-AUDIT BASELINE-WORKTREE-CHANGE DIRTY-95 FOREIGN-LEGEND-WEB-PROTECTED REPAIR7-STALE TURN41-BLOCKED 2026-07-20T19:44:43-04:00 -->

## 53. Parent Audit - Third Protected Baseline Foreign Worktree

- baseline dirty count 95 -> 96；新增`D:\mavenProject\DHXY\.codex-audit-h5-mir\`，创建于
  `2026-07-20T23:49:14Z`，内部含独立`.git`及h5-mir源码/资产，共445项。
- 与DHXY/CR271业务无关，标记FOREIGN/PROTECTED，不删除、不移动、不纳入迁移；先前两个外来目录同样保护。
- baseline branch/HEAD不变；Repair #7 stale、Review #9=`0/2/1`与TURN-41 BLOCKED不变。

<!-- TRUE_EOF: TURN-40F PARENT-AUDIT BASELINE-WORKTREE-CHANGE DIRTY-96 FOREIGN-H5-MIR-PROTECTED REPAIR7-STALE TURN41-BLOCKED 2026-07-20T19:49:43-04:00 -->

## 54. Parent Audit - Repair #7 Worker Resumed

- 用户明确要求立即恢复实施；父级重新唤醒Huygens并提交同一张TURN-40F Repair #7固定返修。
- 状态由`COMMUNICATION_STALE + ACTIVE_STALE`恢复为`SOURCE ACTIVE / OWNER RETAINED`；submission id=
  `019f81f9-8025-7040-8034-54c3a42da747`。
- 写集仅Cloud map-survey校正算法/production tests、`templates/map_label/铁匠屋.png`及零引用重复资源树删除；
  baseline/client Java禁止修改。TURN-41与用户测试继续BLOCKED，等待canonical re-delivery与父级终审。

<!-- TRUE_EOF: TURN-40F PARENT-AUDIT REPAIR7 WORKER-RESUMED SOURCE-ACTIVE OWNER-RETAINED TURN41-BLOCKED NO-RUNTIME 2026-07-20T19:59:43-04:00 -->

## 55. Repair #7 Canonical Source Active Acknowledgement

- Status: `SOURCE ACTIVE / OWNER RETAINED / NOT APPROVED`.
- `ack_parent_message=PARENT-HUYGENS-TURN40F-REPAIR7-MAPSURVEY-CORRECTION-20260720-185942`.
- `ack_parent_message=PARENT-HUYGENS-TURN40F-REPAIR7-MAPLABEL-ASSET-20260720-190442`.
- `ack_parent_message=PARENT-HUYGENS-TURN40F-REPAIR7-RESUME-20260720-200000`.
- Frozen write set: Cloud unique map-survey correction implementation and focused contracts; exact baseline
  `铁匠屋.png` bytes in the sole production consumer; zero-reference duplicate map-label resource tree removal.
  Baseline and CR client Java remain read-only.

<!-- TRUE_EOF: TURN-40F REPAIR7 SOURCE-ACTIVE OWNER-RETAINED ACK-THREE-PARENT-MESSAGES NOT-APPROVED 2026-07-20T20:03:57-04:00 -->

## 56. Parent Audit - Communication Recovered / Fourth Protected Baseline Worktree

- Huygens已在20:03:57 EDT同时ACK全部三个Repair #7 parent message；`COMMUNICATION_STALE + ACTIVE_STALE`
  正式解除，状态确认`SOURCE ACTIVE / OWNER RETAINED`。
- 只读baseline dirty 96 -> 97；新增`D:\mavenProject\DHXY\.codex-audit-legendary-game\`，创建于
  `2026-07-21T00:03:40Z`，含独立`.git`及111项文件，标记FOREIGN/PROTECTED并排除迁移/清理。
- Repair #7写集与Review #9=`0/2/1`保持；TURN-41继续BLOCKED，尚不可用户测试。

<!-- TRUE_EOF: TURN-40F PARENT-AUDIT REPAIR7-COMMUNICATION-RECOVERED SOURCE-ACTIVE BASELINE-DIRTY-97 FOREIGN-LEGENDARY-GAME-PROTECTED TURN41-BLOCKED 2026-07-20T20:04:44-04:00 -->

## 57. Repair #7 Canonical WHOLE-CARD SOURCE+TEST Re-delivery

- Delivery status: `WHOLE-CARD SOURCE+TEST RE-DELIVERED / WAITING PARENT REVIEW / NOT APPROVED`.
- ACKs: `PARENT-HUYGENS-TURN40F-REPAIR7-MAPSURVEY-CORRECTION-20260720-185942`,
  `PARENT-HUYGENS-TURN40F-REPAIR7-MAPLABEL-ASSET-20260720-190442`, and
  `PARENT-HUYGENS-TURN40F-REPAIR7-RESUME-20260720-200000`.

### 57.1 Correction parity

- Cloud unique `CloudMapSurveyService.CameraBounds.correctionAt(...)` now matches the read-only baseline:
  exact pins use `actualRel - current basePointAt`; non-exact points select up to 8 samples within 18 map units,
  retain the nearest sample's 220px actual-screen cluster, solve weighted affine X/Y fields, and fail closed for
  fewer than 3 samples, singular 3x3 systems, or weighted residual above 95px. Historical `errorX/errorY`
  inverse-distance averaging is no longer an authority.
- Focused production-owner tests cover current boundary/center exact-pin recomputation, a non-constant affine
  field, screen-space outlier exclusion, singular rejection, and high-residual rejection.

### 57.2 Map-label production asset closure

- Copied read-only baseline bytes into the sole production consumer
  `src/main/resources/templates/map_label/铁匠屋.png`; SHA-256
  `8BF1850437D74B6783CA32B10092EE45C0534D331BA56D1FE5673BB2254D2CFC`.
- Baseline and Cloud production manifests are now `62/62`; all 62 filenames and SHA-256 values match.
  `MiniMapRecognizer` production initialization is exercised by the manifest contract.
- Production references to `images/template/map_label` are zero. Deleted all 61 tracked PNGs under that
  duplicate resource root; duplicate file count is zero. The one existing test resource lookup was moved to
  the sole `templates/map_label` production path.

### 57.3 Exact write set and verification

- Cloud production `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMapSurveyService.java`, SHA-256
  `13D3C521B116AEB134104AFE838626266B94D61A280B7538425C5FA735295783`.
- Cloud test `src/test/java/com/yueyunfe/dhxy/cloudbrain/CloudMapSurveyCalibrationContractTest.java`, SHA-256
  `EDA1D884A6263FCC027A05621DAAAFB5B435E7DB92ADCF01D65485CDE42404DC`.
- Cloud test `src/test/java/com/yueyunfe/dhxy/cloudbrain/MapLabelProductionAssetContractTest.java`, SHA-256
  `9432B5C6165C0CC3F9957041D76E772274D73DBF5E92DE6A27E437F0C2A6E4D8`.
- Cloud test path-only update
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/PlayerStateTurnContractTest.java`, SHA-256
  `3EB3D72C8027252A94F238B24606B92008CE245D9D719B7286871A4607989BA7`.
- Cloud asset `src/main/resources/templates/map_label/铁匠屋.png`, SHA-256 as above; deleted exact duplicate
  tree `src/main/resources/images/template/map_label/*.png` (61 files, filenames frozen by the 62-item manifest
  test minus `铁匠屋.png`).
- PASS: `mvn -q "-Dtest=CloudMapSurveyCalibrationContractTest,MapLabelProductionAssetContractTest" test`
  (`6 + 2`, zero failures/errors/skips).
- PASS: `mvn -q compile`.
- Additional directly related path-regression run:
  `mvn -q "-Dtest=PlayerStateTurnContractTest" test` ran 17 tests; 13 passed and 4 pre-existing unrelated
  gates failed (three bars-capture fixtures returned no capture; one source guard found existing
  `LocationVisionService`). No map-label resource failure occurred, so Repair #7 was not expanded into those
  unrelated dirty paths.
- Read-only baseline remains branch `codex/baseline-696a12b0`, HEAD
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; baseline `MapSurveyService.java` SHA-256 remains
  `0555A3A6F53FFE25E56543C1692EF28947323AF49542BC62ABEA50B367AD4418`. No CR client Java changed.
  No runtime/application/server/Task/UI/live capture/input, reset, clean, checkout, commit, or extra Git
  mutation was performed.

Waiting for parent whole-card source review; this worker does not self-approve.

<!-- TRUE_EOF: TURN-40F REPAIR7 WHOLE-CARD-SOURCE-TEST-RE-DELIVERED WAITING-PARENT-REVIEW NOT-APPROVED MAPSURVEY-CORRECTION-CLOSED MAPLABEL-62-OF-62 DUPLICATE-ZERO FOCUSED-TESTS-8-PASS CLOUD-COMPILE-PASS 2026-07-20T20:10:27-04:00 -->

## 58. Parent Source+Test Final Review #10 - Repair #7 Passed

- 结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。Huygens owner释放。
- 父级逐行对照baseline `MapSurveyService.CameraBounds`：current-base exact delta、18-map/8-sample筛选、
  220px screen cluster、weighted affine fit、singular与weighted residual>95 fail-closed，以及base-point边界插值/
  center/tile常量均等价；未保留历史error反距离算法或第二store/protocol。
- 父级核验baseline/Cloud map-label均62张且逐文件SHA差异为0；`铁匠屋.png` SHA正确，重复目录不存在，
  production旧路径引用为0。`PlayerStateTurnContractTest`仅把测试fixture改到唯一production path。
- 父级复跑`mvn -q "-Dtest=CloudMapSurveyCalibrationContractTest,MapLabelProductionAssetContractTest" test`
  共`6+2`项PASS；`mvn -q compile` PASS。未运行runtime/application/server/Task/UI/live capture/input。
- TURN-40F source review闭合；TURN-41继续`BLOCKED / DATA CUTOVER REQUIRED / NOT READY FOR USER TEST`。

<!-- TRUE_EOF: TURN-40F PARENT-SOURCE-TEST-REVIEW-10 PASSED P0-0-P1-0-P2-0 OWNER-RELEASED FOCUSED-TESTS-8-PASS CLOUD-COMPILE-PASS TURN41-DATA-CUTOVER-BLOCKED 2026-07-20T20:14:44-04:00 -->

## 59. Fresh Runtime Repair - Stop-Bearing Turn Must Not Long-Poll

- 真实日志证据：客户端`2026-07-21T00:07:37.626-04:00`收到停止命令，直到
  `00:08:36.632`才完成，`elapsedMs=59006`。根因是`WindowTurnLoop.requestStop()`中断普通turn后，最后一次
  stop-bearing turn仍使用`waitTimeoutMs=60000`；Cloud记录metadata后继续`awaitAction`。
- 修复写集：Cloud `CloudTurnExchange.java`在已接收metadata/outcome后，对`stopRequested=true`跳过
  `awaitAction`并返回`IDLE`；若有尚未执行action，则清除slot并用新增typed command state
  `STOPPED/WINDOW_STOPPED`完成Cloud waiter，不伪造业务`TurnOutcome`。`CloudTurnHttpHandler.java`在该exchange
  后调用active runtime cooperative stop。`CloudTurnCommandResult.java`增加上述非业务terminal状态；
  `CloudTurnExchangeContractTest.java`新增60秒wait参数下小于1秒返回及action释放证明。
- 验证：`mvn "-Dtest=CloudTurnExchangeContractTest,CloudTurnHttpHandlerContractTest" test`，16项
  PASS、0 failure/error/skip；主源码与测试源码compile PASS。组合运行仍复现既存且无关的
  `CloudTurnActivationContractTest.validTaskStartTurnReachesRuntimeAndReturnsMatchingAck` 400/200失败，未把它
  误记为本修复回归。
- 部署：旧Cloud PID `23632`已停止；fresh classpath Cloud PID `37340`于
  `2026-07-21T00:17:52-04:00`监听18080，OCR PID `15908`监听18762。本地baseline严格只读；未执行游戏
  capture/input。对真实18080发送`waitTimeoutMs=60000`、`stopRequested=true`的无游戏输入smoke turn，
  `444ms`返回HTTP 200 + `IDLE`。用户fresh runtime下一验收点为“开始后立即停止不再等待约59秒”。

<!-- TRUE_EOF: TURN-40F FRESH-RUNTIME-STOP-LONG-POLL-REPAIR FOCUSED-TESTS-16-PASS CLOUD-FRESH-PID-37340 USER-RETEST-READY 2026-07-21T00:18:00-04:00 -->

## 60. Fresh Runtime Repair - Start Success Requires Matching Cloud ACK

- 日志证据：`2026-07-21T00:23:59.767-04:00` UI在`14ms`内报告远程启动完成；到
  `00:23:59.781` turn loop才因`response requires taskStartAck for taskStartRequest`退出。旧实现把本地线程创建
  当成成功，用户所见“点启动没有实际操作”属实。
- Client repair：`WindowTurnLoop.awaitStartAcknowledged(Duration)`在lifecycle monitor上等待匹配ACK；
  `WindowTaskControlService.startOneRemote(...)`只在ACK后调用`markRemoteStarted`。10秒超时、loop停止、transport
  异常或线程中断均不再报告成功，并清理刚创建的loop。没有改变修罗/五环业务阶段、OCR、导航或input决策。
- Cloud restart-race repair：`CloudTurnHttpHandler`先传播runtime stop，由exchange取消未执行action，再等待runtime
  terminal（上限5秒）后才返回stop response；超时返回typed `TURN_RUNTIME_STOP_TIMEOUT`，不让新start撞上旧ACTIVE slot。
- Parent verification：client `WindowTurnLoopContractTest,TurnModeGuardContractTest`共4项PASS、compile PASS；Cloud
  `CloudTurnExchangeContractTest,CloudTurnHttpHandlerContractTest`共16项PASS、compile PASS。
- Deployment：fresh classpath Cloud PID `33608`于`2026-07-21T00:31:52-04:00`监听18080，OCR PID `24200`
  监听18762。当前无AutoBot进程；必须由用户重新启动客户端加载新class。fresh runtime acceptance：启动按钮只有
  收到Cloud ACK才成功并继续动作；停止后立即重启不得再出现ACTIVE_CONFLICT/缺失ACK。

<!-- TRUE_EOF: TURN-40F FRESH-RUNTIME-START-ACK-GATE CLIENT-TESTS-4-PASS CLOUD-TESTS-16-PASS CLOUD-FRESH-PID-33608 USER-RETEST-READY 2026-07-21T00:33:00-04:00 -->

## 61. Fresh Runtime Repair - Task Start Must Bypass Ordinary Action Long-Poll

- 真实证据：`00:34:20.425`首个新客户端loop启动，直到`00:34:30.579`才以`0/1`返回；第二次
  `00:35:00.476`启动、`00:35:00.493`收到ACK并标running，但到用户`00:36:01.509`停止前没有
  ACTION/capture/input。窗口识别与绑定均正确。
- 根因：`CloudTurnHttpHandler.handleRequest(...)`先执行
  `turnExchange.exchange(... Duration.ofMillis(waitTimeoutMs))`，之后才`withTaskStartAck(...)`启动runtime。
  因此task-start生命周期握手错误地被当作普通action long-poll，ACK和runtime启动均被推迟，且重试时序不稳定。
- 修复：携带`taskStartRequest`的turn以`Duration.ZERO`完成同一个exchange内的metadata/outcome登记，随后按原路径
  activation runtime并返回matching ACK；worker产生的第一条action仍由唯一exchange slot保留，在客户端下一turn
  交付。非启动turn继续使用请求的long-wait；无第二协议/store，无修罗业务、OCR、导航或input顺序变化。
- 回归：修复既存stale activation fixture的`taskMaxRuns`，并让其携带`waitTimeoutMs=60000`断言ACK小于2秒。
  `CloudTurnActivationContractTest#validTaskStartTurnReachesRuntimeAndReturnsMatchingAck`、
  `CloudTurnExchangeContractTest`、`CloudTurnHttpHandlerContractTest`共17项PASS，Cloud compile PASS。
- 部署：fresh classpath Cloud PID `31924`于`00:39:34`监听18080，OCR PID `28476`监听18762。
  用户复测门：一次启动应立即收到ACK，随后首个组队身份预检action应移动鼠标并截图；仍无动作则以新日志继续定位，
  不得仅凭running状态宣称任务已运行。

<!-- TRUE_EOF: TURN-40F TASK-START-LONG-POLL-ORDERING-REPAIR TESTS-17-PASS CLOUD-FRESH-PID-31924 USER-RETEST-READY 2026-07-21T00:40:00-04:00 -->

## 62. Fresh Runtime Repair - Legal Tracker ABSENT Must Continue

- 真实客户端日志完整链路：`00:42:41.306`收到匹配start ACK；`00:42:41.475`执行队长区域鼠标移动；
  背包第一页命中并右键使用`sheyaoxiang_item.png`，`00:42:53.816 success=true`；随后
  `00:42:55.189 [task-tracker-capture] anchor absent in masked full window`，此后直到用户停止没有任何
  action/capture/input。摄妖香操作本身成功，不是停滞原因。
- 根因：客户端`TurnTaskTrackerOperationResult`对`ABSENT`等非`CAPTURED`状态依法返回nullable image字段；Cloud
  `CloudTaskTrackerLocalServiceClient`却启用`FAIL_ON_NULL_CREATOR_PROPERTIES`，在record构造前拒绝合法JSON。
  `TaskTrackerPanelService.observe()`把解析失败升级为runtime异常，而`CloudTurnTaskRuntime.runOneTask()`此前只返回
  `FAILED`不记录异常，形成“UI运行中但游戏不动”的静默失败。
- 修复写集：Cloud `CloudTaskTrackerLocalServiceClient.java`移除null-creator全局拒绝，保留unknown、missing、
  trailing严格门及record自身状态不变量；`CloudTurnTaskRuntime.java`为单任务失败、queue terminal异常补齐
  `taskCode/taskRunId/windowId/startRequest`和stack trace；新增
  `CloudTaskTrackerLocalServiceClientContractTest.java`覆盖所有非截图状态nullable字段，并证明missing/unknown仍
  fail closed。
- 父级验证：`CloudTaskTrackerLocalServiceClientContractTest` 2、`CloudTurnTaskRuntimeContractTest` 29、
  `CloudTurnActivationContractTest#validTaskStartTurnReachesRuntimeAndReturnsMatchingAck` 1、
  `CloudTurnExchangeContractTest` 9、`CloudTurnHttpHandlerContractTest` 7，共`48/48` PASS，0 failure/error/skip。
- 部署：旧Cloud PID `31924`已核实并停止；fresh classpath Cloud PID `45812`于
  `2026-07-21T00:48:09-04:00`监听18080，fresh OCR PID `40836`监听18762，启动stderr为空。用户实机复测门：
  tracker不存在时必须继续startup return-item检查并进入`ACCEPT_TASK_NAVIGATE_TO_NPC`，不得停在摄妖香之后；
  在该fresh run通过前仍不宣称修罗迁移完成。

<!-- TRUE_EOF: TURN-40F TRACKER-ABSENT-PARSE-REPAIR TESTS-48-PASS CLOUD-FRESH-PID-45812 OCR-PID-40836 USER-RETEST-READY NOT-LIVE-ACCEPTED 2026-07-21T00:49:00-04:00 -->

## 63. Headless Supervised Auto-Start - Source WIP

- 用户明确要求监督运行不再通过JavaFX UI。现有生产入口
  `GameWindowRegistrationService.scanRegisterAndStartIndependentWindows(TaskType)`已完整拥有窗口扫描、独立注册、
  exact-window绑定和Cloud ACK启动；`AutoBot`却无条件忽略`bot.run.auto-start=true`，与
  `TaskRunProperties`中`showUi=false`可直接跑任务的既有配置合同冲突。
- 修复仅限client启动编排：`show-ui=false + auto-start=true + init-game-window=true + exactly-one tasks`复用上述
  唯一生产链；非法/多任务配置、空扫描或未全量收到Cloud ACK均fail fast并输出明确日志。不新增任务协议/store，
  不复制任务实现，不改变修罗阶段、OCR、导航、输入或失败恢复。
- 基线门：只读`D:\mavenProject\DHXY@696a12b0...`未写；当前client HEAD `59b85e0`，Cloud HEAD
  `3b988ca`，两仓原dirty状态保护。业务合同核对`docs/业务逻辑.md`修罗启动/热恢复章节；无已批准业务差异，
  本改动只替换启动触发方式。client compile与真实headless启动待验证。

### 63.1 First headless runtime finding

- client compile PASS。首次真实参数启动PID `32716`完成窗口扫描、注册、turn loop启动和Cloud ACK，日志为
  `Headless auto-start finished ... success=1 failed=0`；随后因`showUi=false`没有JavaFX非daemon线程，runner返回后
  JVM立即退出。这是host生命周期缺口，不是修罗阶段失败。
- headless成功路径现由main runner阻塞驻留直到进程被中断；中断时恢复interrupt并记录退出日志。不增加轮询、
  业务等待、协议或第二控制store。重新compile和fresh runtime待验证。

<!-- TRUE_EOF: TURN-40F HEADLESS-SUPERVISED-AUTOSTART HOST-LIFETIME-REPAIR SOURCE-WIP FIRST-COMPILE-PASS RUNTIME-RETRY-PENDING 2026-07-21T01:04:00-04:00 -->

## 64. Fresh Runtime Repair - Xiuluo Start-Exit Must Close Mini Map

- 真实客户端日志：`2026-07-21 03:45:27.556` 打开小地图，`03:45:28.311` 点击修罗起步出口，随后
  反复执行移动证明与重点击，但没有第二次 `Alt+1`；用户看到小地图一直未关闭。
- 根因：Cloud 已计算 `startExitFireAndHandoff`，但仍调用普通 proof-gated
  `clickMiniMapLogicalPointForHandoff(...)`。当前 dirty baseline 对该特殊分支是 fire-and-handoff：点击完成后
  立即 best-effort 关闭小地图，移动证明交给后续 watcher，关闭失败不否定已经发出的路径点击。
- 修复：Cloud `NavigationService.navigateInCurrentMap(...)` 对特殊分支调用新增
  `clickMiniMapLogicalPointForFireAndHandoff(...)`；保持 exact-window 点击、点击完成后登记 pathing intent、无条件
  尝试 `Alt+1`，普通小地图路径不变。新增
  `NavigationXiuluoStartExitPrepathFireAndHandoffWiringTest` 锁定分支和动作顺序。
- 验证：定向 source contract PASS；Cloud tests-enabled compile PASS（560 source files）。fresh Cloud PID `13860`
  已监听 `18080`。未写只读 baseline，未启动客户端或发送游戏输入。

## 65. Parent Four-Way Baseline Equivalence Audit - Repair Required

- 父级本人加三路只读 Worker 对当前 dirty baseline、CR client 与 Cloud 做方法级交叉审计；去重结论：
  `P0/P1/P2=0/10/3 / REPAIR REQUIRED / NOT READY FOR FORMAL USER TEST`。
- 修罗主流程 P1：accept objective/tracker 不再同帧；维护重试新增 fresh dialog inspect 且 lightweight cleanup 未等价
  `forceCloseDialog()`；tracker 后台读取异常被升级为 fatal；accept 临时缺失 window metadata 被升级为 fatal。
- turn/input 公共 P1：多个 consumer 用包含 pause/stop/pathing 动态字段的整份 metadata `equals()` 校验稳定窗口，
  可在导航完成后误拒 NPC/dialog/tracker 动作；部分鼠标步骤已执行时被统一回报 `NOT_RUN`，存在重复点击；旧基线
  HWND 普通失败后的 focused keyboard fallback 丢失。
- 其它业务 P1：召唤兽删技能漏掉确认后的 story 点击；技能面板打开丢失一次锚点重试与页面切换复验；自动战斗
  次数重新把 OCR 结果设为 authority，偏离当前 dirty baseline 的缓存计数规则。
- P2：缺少跨 consumer 的稳定-window/dynamic-metadata 参数化回归；Cloud 有零引用 `SystemPowerService`；另有恒
  `null`/零生产引用的旧 decision 接口残余。六份 learned-memory 与 `map_camera_bounds` 的数据 cutover证据仍是独立门。
- 返修顺序：先修会直接卡住修罗的 stable-window metadata、accept 同帧/异常恢复、维护关闭；再修 partial outcome 与
  keyboard fallback；最后修召唤兽/自动战斗及 P2 清理。每项必须以当前 dirty baseline 的可复现合同测试验收，
  不在 client 复制业务算法，不新增第二协议/store。

<!-- TRUE_EOF: TURN-40F MINIMAP-CLOSE-REPAIRED FOUR-WAY-EQUIVALENCE-AUDIT P0-0-P1-10-P2-3 REPAIR-REQUIRED NOT-FORMAL-TEST-READY ZERO-OWNER 2026-07-21T04:09:53-04:00 -->

## 66. Repair #8 Canonical Claim - NOETHER

- Canonical status: `SOURCE ACTIVE / REPAIR #8 / NOETHER OWNER / OWNER RETAINED / NOT DELIVERED / NOT APPROVED`.
- Scope is the complete deduplicated `P0/P1/P2=0/10/3` set in section 65. All ten P1 findings must close in
  this same whole-card repair; partial completion is not a delivery condition.
- Repair order is frozen as A) Xiuluo formal-test blockers, B) shared turn/input semantics, C) summon-skill and
  auto-combat parity. P2 work is subordinate and may proceed only with complete zero-reference proof.
- `D:\mavenProject\DHXY` remains the current dirty business authority and is strictly read-only. Writable roots are
  only `D:\mavenProject\DHXY-cr271` and `D:\mavenProject\dhxy-cloud-brain`; all existing dirty/untracked state is
  protected. No runtime/application/server/Task/UI/live capture/input is authorized.
- Before the first Java edit, the worker will append the method-level baseline evidence and exact write set. Any
  genuinely undecidable semantic conflict will be recorded as `PLAN-CONTRACT BLOCKED` for that point while all
  independent P1 repairs continue.

<!-- TRUE_EOF: TURN-40F REPAIR8 CANONICAL-CLAIM NOETHER-OWNER SOURCE-ACTIVE OWNER-RETAINED ALL-P1-10-REQUIRED NOT-DELIVERED NOT-APPROVED 2026-07-21T04:27:19-04:00 -->

## 67. Repair #8 Canonical Owner Transfer - LORENTZ

- Previous delayed claim owner `NOETHER (agent 019f83be-9731-7ec0-a386-ce5ccd60cd21)` is
  `RETURNED / SHUTDOWN`; Noether made no Java changes for Repair #8.
- Canonical owner is transferred to `LORENTZ (agent 019f83ca-f262-7382-a8d4-68c07719e0cf)`.
- Status: `SOURCE ACTIVE / REPAIR #8 / LORENTZ OWNER / OWNER RETAINED / NOT DELIVERED / NOT APPROVED`.
- Scope is unchanged: all ten deduplicated P1 findings in section 65 must close as one whole-card repair.
  No partial delivery, second protocol/store, client business-algorithm copy, stub, or constant-null substitute is allowed.
- `D:\mavenProject\DHXY` remains strictly read-only. Existing dirty/untracked state in all three workspaces is protected;
  no runtime/application/server/Task/UI/live capture/input and no reset/checkout/clean/revert/commit are authorized.

<!-- TRUE_EOF: TURN-40F REPAIR8 OWNER-TRANSFER NOETHER-RETURNED-SHUTDOWN LORENTZ-019F83CA-F262-7382-A8D4-68C07719E0CF SOURCE-ACTIVE OWNER-RETAINED ALL-P1-10-WHOLE-CARD SCOPE-UNCHANGED NOT-DELIVERED NOT-APPROVED 2026-07-21T04:31:41-04:00 -->

## 68. Repair #8 Method Evidence And Exact Write Set - LORENTZ

### 68.1 Read-only dirty baseline evidence

- Baseline remains `D:\mavenProject\DHXY@696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`, branch
  `codex/baseline-696a12b0`, dirty/untracked protected and never built or written.
- Stable window: the migrated consumers currently compare full `TurnWindowMetadata` records. The stable contract is only
  `deviceId/windowId/nativeHandle/processId/windowTitle/windowRect`; pause, stop, pathing and startup/role authority facts
  are mutable turn facts and are not identity.
- Accept frame: baseline `XiuluoTaskV2.scheduleAcceptObjectiveBackgroundParse(...)` lines 3086-3169 captures once and
  sends the same `AcceptWindowSnapshot` to objective crop and `readXiuluoTrackerPanelFromSnapshot(...)`. Cloud lines
  3251-3338 captures the objective frame, then calls live `readXiuluoTrackerPanel(...)`, creating a second capture.
- Maintenance: baseline `cleanupAndLogMaintenanceRetry(...)` lines 1542-1567 reads the observer-owned visible-dialog
  snapshot with a 3-second freshness bound and executes `forceCloseDialog()` before generic-window cleanup. Cloud lines
  1550-1573 performs a fresh `DialogHandleRequest.inspect(...)` and substitutes lightweight/generic cleanup.
- Tracker/snapshot misses: baseline background tracker exceptions return `TaskTrackerPanelReadResult.empty()` and failed
  window refresh/capture returns a missing accept snapshot. Cloud rethrows `ExecutionException` runtime causes and
  treats absent latest metadata as fatal; exact device/window identity mismatch remains a fatal invariant.
- Partial mouse: client `LocalTurnActionExecutor.execute(...)` lines 91-123 maps any failed aggregated queue request to
  first step FAILED plus every later step NOT_RUN, although `InputActionExecutionResult` already carries
  `startedStepIndex/lastCompletedStepIndex`; an already executed click can therefore be retried.
- Keyboard: dirty baseline `InputActionWorker.pressAltShortcut(...)` lines 234-262 preserves HWND success, terminal
  rejection failure, and focused real-input fallback after not-attempted/ordinary failure inside the input transaction.
  Client `TurnInputStepExecutor` lines 76-227 bypasses that serialized fallback and fails direct HWND attempts.
- Summon skill: baseline `deleteSkillAtSlotDirect(...)` lines 745-778 requires confirm click, 900ms wait, then successful
  story click before returning true. Baseline `openSummonSkillPanelDirect(...)` lines 238-298 retries the initial anchor
  once after 800ms and, after the skill-tab click, waits 1000ms then rechecks once after 800ms. Cloud omits the story
  click and both verification contracts.
- Auto combat: baseline `refreshAutoCombatRoundsIfNeeded(...)` lines 153-183 uses only the cached estimated-round counter
  plus interval; OCR is not authority. Cloud lines 284-328 overwrites the cache from `readRemainingRounds(...)` before
  deciding. Panel location/alignment remains valid and is retained.
- `docs/业务逻辑.md` 修罗/维护/自动战斗相关基线已核对；无已批准业务差异，按当前 dirty baseline 等价迁移。

### 68.2 Frozen production/test write set

- Shared Cloud/client protocol: both copies of `TurnWindowMetadata.java`; both copies of `TurnLocalOperation.java` only
  if required to expose the existing client `UICleanerService.forceCloseDialog()` through HTTPS turn v1.
- Client production: `LocalTurnActionExecutor.java`, `TurnInputStepExecutor.java`, `TurnInputActionMapper.java`,
  `UiLocalOperationExecutor.java`; existing input queue/worker is reused and is changed only if needed to preserve the
  baseline keyboard fallback for the already closed keyboard vocabulary.
- Cloud production: stable-identity consumers in `AutoCombatPanelService`, `BattleRadarService`, `DialogService`,
  `CloudLeftTopStatusPortAssembly`, `NavigationService`, `NpcClickService`, `CloudPlayerStateFirstAidPort`,
  `CloudPlayerStateIncenseStatusPort`, `SummonSkillService`, `TaskTrackerPanelService`, `CloudCommonBoxPortAssembly`,
  `CloudDialogDetectionPort`, `CloudDialogPreparedActionValidationPort`, and `CloudTeamReturnPortAssembly`;
  plus `XiuluoTaskV2`, `CloudUiCleanerPort`, and `AutoCombatPanelService` for the remaining business repairs.
- Client tests: `TurnInputStepExecutorContractTest`, `LocalTurnActionExecutorContractTest`,
  `UiLocalOperationExecutorContractTest`, and affected protocol golden tests.
- Cloud tests: `XiuluoWholeTaskTurnContractTest`, `TaskTrackerPanelTurnContractTest`, `SummonSkillTurnContractTest`,
  `AutoCombatPanelTurnContractTest`, `UiCleanerTurnContractTest`, affected existing consumer contract tests, and one
  parameterized cross-consumer stable-window contract. Tests must drive production methods and include opposite-field
  counterexamples; source-only guards are not acceptance.
- No second protocol/store, client business algorithm, stub, constant-null result, retry, runtime, UI, live capture/input,
  or Git mutation is authorized. Any mechanically required test-fixture constructor update remains inside the named
  HTTPS turn contract family and will be listed at delivery.

<!-- TRUE_EOF: TURN-40F REPAIR8 METHOD-EVIDENCE-WRITESET-FROZEN LORENTZ-OWNER ALL-P1-10-WHOLE-CARD BASELINE-696A12B0 READ-ONLY NO-APPROVED-BUSINESS-DIFFERENCE SOURCE-ACTIVE NOT-DELIVERED NOT-APPROVED 2026-07-21T04:38:00-04:00 -->

## 69. Repair #8 Whole-Card Source + Test Delivery - LORENTZ

- Canonical status: `WHOLE-CARD SOURCE+TEST DELIVERED / ALL-10-P1 CLOSED IN SOURCE+FOCUSED CONTRACTS /
  LORENTZ OWNER RETAINED / PARENT REVIEW REQUIRED / NOT APPROVED / NOT PASSED / NOT FORMAL TEST READY`.
- Owner remains `LORENTZ (agent 019f83ca-f262-7382-a8d4-68c07719e0cf)`. The Noether shutdown transfer in section 67
  remains authoritative. Client base is `59b85e0bb494f43ad7e7434f3d2170deb373c6ef`; Cloud base is
  `3b988caa010254973e03342272e6d1d6a9685b01`; read-only dirty baseline remains
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` and was not written, built, or run.

### 69.1 Ten-P1 production closure map

1. Both protocol copies now expose one `hasSameStableWindowIdentity(...)` contract over exactly
   `deviceId/windowId/nativeHandle/processId/windowTitle/windowRect`; fourteen Cloud consumers use it and ignore all
   mutable pause/stop/pathing/startup/role facts. The 19-case parameterized contract varies every dynamic field and
   separately rejects every stable-field mismatch.
2. Xiuluo accept objective and tracker parsing consume the identical caller-owned `AcceptWindowSnapshot` image; the
   tracker path no longer performs a second capture.
3. Xiuluo maintenance reads the observer-owned dialog fact with a maximum age of 3000ms and calls
   `DialogService.closeObservedDialog(...)`; the close executes the baseline story/option action without fresh dialog
   classification, then generic-window cleanup runs.
4. Background tracker parser/future exceptions degrade to miss; absent or invalid transient window metadata degrades
   to snapshot missing; exact device/window mismatch remains fatal.
5. Client atomic mouse execution maps `startedStepIndex/lastCompletedStepIndex` to the exact completed turn-step prefix;
   completed clicks are `COMPLETED`, the current failed/stopped step preserves its terminal status, and only the untouched
   suffix is `NOT_RUN`.
6. Alt shortcuts use the existing serialized input queue/worker boundary. HWND not-attempted and ordinary failure retain
   focused fallback; terminal rejection remains failure; no callback submits a nested queue request.
7. Summon delete confirmation now waits 900ms and executes the story click; the delete result is true only when that story
   click succeeds.
8. Summon panel opening gets one 800ms Alt+O anchor retry. Skill-page click waits 1000ms and, if still on the attribute
   anchor, receives one further 800ms recheck.
9. Auto-combat refresh authority is the dirty-baseline cached estimate plus refresh interval; remaining-round OCR no longer
   overwrites or controls that decision, while panel location/alignment remains unchanged.
10. The merged maintenance P1 closes both observer-fact provenance and a real dialog-close action, and its complete gate
    includes the 19-case cross-consumer stable-window parameterized production contract from item 1.

No second protocol/store/operation was retained, no business algorithm moved to Client, and no stub or constant-null
substitute was added. A provisional `UI_FORCE_CLOSE_DIALOG` route was removed after method-level review showed the Cloud
owner could execute the baseline-equivalent action through existing OCR/image/input ports. The transiently inspected files
(`TurnLocalOperation`, both validators, Client dispatcher/UI executor, Cloud UI-cleaner client/port) were restored to their
pre-repair bytes and are not part of the final delta.

### 69.2 Exact final write set, SHA-256, size, and mtime

| Repo | File | Bytes | mtime (EDT) | SHA-256 |
|---|---|---:|---|---|
| client | `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java` | 18676 | 2026-07-21T05:10:29.603-04:00 | `0D68799EEAAE7C66A01E3E3E993CDE513CFB634F87F9D7C66AE61874401D0293` |
| client | `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java` | 23674 | 2026-07-21T05:11:46.204-04:00 | `7DF42DC07D1A1487EBADDD36A55B6DE57819243213F3C50A0D5B91DC8D8534D2` |
| client | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java` | 4584 | 2026-07-21T04:38:00.458-04:00 | `5A5AB71477CA28766E581A6EC85BE1178CEE891FEB8F46BA0442DFE9297113BE` |
| client | `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java` | 2577 | 2026-07-21T05:11:05.992-04:00 | `E2D4077AF3846053F476F05FF8574AAE891C4BB2807735B4F14500DDD8418C4C` |
| client | `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java` | 2953 | 2026-07-21T04:57:47.188-04:00 | `D714BDAF5D1AC90359E311A0255DBB286A325C3E84E078CD3732E033A2E7203F` |
| client | `src/test/java/com/bot/dhxy/input/action/InputActionWorkerAltFallbackContractTest.java` | 4599 | 2026-07-21T04:57:47.189-04:00 | `BEB45D5556D586A45B98ABAC70900455505C1A87DEB8C6F5F26331F91AA82BAE` |
| cloud | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java` | 4584 | 2026-07-21T04:38:00.459-04:00 | `5A5AB71477CA28766E581A6EC85BE1178CEE891FEB8F46BA0442DFE9297113BE` |
| cloud | `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java` | 43091 | 2026-07-21T04:48:52.753-04:00 | `B86E6382BB257894A2A6C3F787EBBBB2F6F43FE5DC0D55E3A6CCEB204EA372C1` |
| cloud | `src/main/java/com/bot/dhxy/service/BattleRadarService.java` | 66710 | 2026-07-21T04:38:00.462-04:00 | `401CD9B134942A19CFFB441230089821FEC5910A6EC1CE12B1E45A968EC39233` |
| cloud | `src/main/java/com/bot/dhxy/service/DialogService.java` | 159794 | 2026-07-21T05:15:42.459-04:00 | `5BBDDDF811F3F71BF08DCF910B3955C70BB4686353113C7BC9F11F00D681AF30` |
| cloud | `src/main/java/com/bot/dhxy/service/NavigationService.java` | 183794 | 2026-07-21T04:38:42.331-04:00 | `70D7152F9A8526C1878F3112DFFED9D863A6803B41D2DDDD798F4C5C78C4E40F` |
| cloud | `src/main/java/com/bot/dhxy/service/NpcClickService.java` | 218470 | 2026-07-21T04:38:00.465-04:00 | `A24BD347D64A29B1E2EF73639A5523317A3A9FCFFB1B44EFFF310D2EA58AFCA0` |
| cloud | `src/main/java/com/bot/dhxy/service/SummonSkillService.java` | 69131 | 2026-07-21T04:48:38.646-04:00 | `9F80DBEAC4BB721BAA2FA3E79E9096F40ED3FF3F5E5D9D770797D2B84C816FB3` |
| cloud | `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java` | 57735 | 2026-07-21T04:41:49.500-04:00 | `235E6E727317A5FA80CD01150736DC5142E25664E523DB86BC9220CBE6293A86` |
| cloud | `src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java` | 20005 | 2026-07-21T04:38:00.464-04:00 | `2D69CF9B5D893042764AD2C6A5190E9FEA63B556CCBB01C77544DDC7E111D80F` |
| cloud | `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateFirstAidPort.java` | 22272 | 2026-07-21T04:38:00.466-04:00 | `7E853E6604C8CC349C663640BAB2C54299C13750B90C9BF7D60D00C6C4409602` |
| cloud | `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateIncenseStatusPort.java` | 16027 | 2026-07-21T04:38:00.467-04:00 | `E8B7F36C066281A11CECDCD947ED4508125028BC5C17B7F693BF579A0D438279` |
| cloud | `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` | 267836 | 2026-07-21T05:13:23.763-04:00 | `55FA80F92E1E9931B010F5431B55B6EEDF97E2E15B791C6AC6AD2B1D5CB25205` |
| cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java` | 24276 | 2026-07-21T04:38:00.470-04:00 | `230840DDD64A54230807C8B6A388FDE1FCB6127624250D1EC23D3A5C2E450C76` |
| cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java` | 17029 | 2026-07-21T04:38:42.333-04:00 | `07FAEAC2DDF92792C462D9793419DDB94A2A77B7D152A711CE8FC177D99EDD54` |
| cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java` | 13451 | 2026-07-21T04:38:00.470-04:00 | `81BC62853BF18BAB6E2C13182EB7A9CF7278AEAF02DBB91826BE883C35B6CBB9` |
| cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java` | 25540 | 2026-07-21T04:38:00.471-04:00 | `8A79669E069A97C00F9C8EBBCD0818DC7DFD3A98D86CFCD007C194C22BBBD493` |
| cloud | `src/test/java/com/bot/dhxy/cloud/turn/protocol/StableWindowIdentityCrossConsumerContractTest.java` | 3773 | 2026-07-21T05:07:08.497-04:00 | `A968B8A805DAF75EC37E3153A3C78643A97A45938DE7C9A9E3D19D0C240D47D9` |
| cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatPanelTurnContractTest.java` | 54011 | 2026-07-21T05:08:42.062-04:00 | `1C2A1593039901D1423E7481BCCE457C27AC0923E6BF7EDFD437DF81166353F3` |
| cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/BattleRadarTurnContractTest.java` | 41402 | 2026-07-21T05:07:08.497-04:00 | `9CD2D4696E83F4C20E425B5DF5FF5715D2D1E85F67B1979BD8853D9A20100C9E` |
| cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java` | 110552 | 2026-07-21T05:16:10.301-04:00 | `32C055F35738DD33E9B1B0D33F4F41236A62D16335C5DF6562A28130287B5D13` |
| cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java` | 89096 | 2026-07-21T05:09:43.912-04:00 | `CAFD3DF9B5D9B202E233991521219C744FC16F91A356F5089ACCF501114D0ED8` |
| cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java` | 47201 | 2026-07-21T05:08:22.940-04:00 | `01E76D1BACA763B7582722565500C2C40249C5E40158E027B1E9D9A31A1FC888` |

### 69.3 Named test and compile evidence

- Client command:
  `mvn -q "-Dtest=TurnInputStepExecutorContractTest,InputActionWorkerAltFallbackContractTest,LocalTurnActionExecutorContractTest" test`.
  Surefire reports: `5 + 3 + 2 = 10`, failures/errors/skips=`0/0/0`, command exit 0.
- Cloud command:
  `mvn -q "-Dtest=StableWindowIdentityCrossConsumerContractTest,BattleRadarTurnContractTest#outcomeDynamicTurnFactsDoNotInvalidateStableBattleWindow,DialogDetectionTurnContractTest#captureAcceptsAPathingFactThatAdvancedAfterTheExactActionWasExposed,DialogOptionTurnContractTest#observedOptionCloseUsesKeywordActionWithoutASecondDialogClassification,AutoCombatPanelTurnContractTest#visibleRoundsOcrDoesNotOverrideHealthyCachedCounter,SummonSkillTurnContractTest#panelAnchorAndSkillPageUseSingleEightHundredMillisecondRechecks+ordinaryDeletePreservesSelectDialogConfirmPostDeleteOrderAndLimits+confirmedDeleteIsNotSuccessfulWhenPostDeleteStoryClickFails,XiuluoWholeTaskTurnContractTest#acceptTrackerUsesCallerFrameAndParserExceptionDegradesToMiss+exceptionalAcceptTrackerFutureAndMissingMetadataRemainRecoverable+maintenanceCloseUsesThreeSecondObserverPolicyAndRealObservedAction" test`.
  Surefire reports: `19 + 1 + 1 + 1 + 1 + 3 + 3 = 29`, failures/errors/skips=`0/0/0`, command exit 0.
- Total focused production-path contracts: `39`, failures/errors/skips=`0/0/0`.
- Client and Cloud each ran `mvn -q -DskipTests=false compile` against the final source; both commands exited 0.
  `git diff --check` exited 0 in both writable repos (only pre-existing line-ending conversion warnings in dirty files).
- No application/server/task/runtime was started; no UI, live capture, or physical input was invoked. No Git state mutation
  command or commit was performed. This is worker source/test delivery evidence only; parent review remains the sole gate.

<!-- TRUE_EOF: TURN-40F REPAIR8 WHOLE-CARD-SOURCE-TEST-DELIVERED LORENTZ-019F83CA-F262-7382-A8D4-68C07719E0CF OWNER-RETAINED ALL-P1-10-CLOSED-SOURCE-FOCUSED-CONTRACTS TESTS-39-ZERO-FAILURE-ERROR-SKIP CLIENT-COMPILE-EXIT0 CLOUD-COMPILE-EXIT0 BASELINE-696A12B0-READONLY PARENT-REVIEW-REQUIRED NOT-APPROVED NOT-PASSED NOT-FORMAL-TEST-READY 2026-07-21T05:21:20-04:00 -->

## 70. Parent Source + Test Review #11 - Repair #8 Repair Required

- Parent逐文件终审当前结论：`P0/P1/P2=0/2/0 / REPAIR REQUIRED / OWNER RETAINED / NOT FORMAL TEST READY`。
- **P1-1 maintenance STORY safety gate未等价：**只读dirty baseline
  `UICleanerService.forceCloseDialog()` lines 307-315在`STORY_IGNORED`后先执行
  `canFastClickStoryDialog()`；成员窗口只有`GameContext.ActionState.IN_COMBAT`才允许快点剧情。当前Cloud
  `DialogService.closeObservedDialog(...)` lines 262-268对任何observer-owned `STORY`直接调用
  `handleStoryDialog()`，而`XiuluoTaskV2.closeMaintenanceObserverDialog(...)` lines 1580-1585也未验证
  window role/action state。返修必须保留3秒observer fact、不得二次分类，同时恢复相同的成员/动作状态安全门；
  反例测试必须证明member + non-combat不产生任何story input，leader或member + combat才允许。
- **P1-2 cross-consumer测试未驱动consumer生产入口：**
  `StableWindowIdentityCrossConsumerContractTest.everyConsumerUsesStableIdentityWhenDynamicFactsAdvance(...)`
  只是参数化13个字符串标签并重复调用`TurnWindowMetadata.hasSameStableWindowIdentity(...)`；没有实例化或调用第68.2节
  14个consumer的任何生产校验方法，因此不满足原卡“Tests must drive production methods; source-only guards are not
  acceptance”。返修必须对14个consumer逐一驱动其实际outcome/window校验入口（可复用生产可见 seam），每个consumer
  至少覆盖动态字段变化接受与一个稳定字段变化拒绝；不能以字符串标签、源码contains或只测helper替代。
- 其它已审生产路径继续保留；这两项关闭前不运行父级Maven、不释放owner、不称全部P1完成。

<!-- TRUE_EOF: TURN-40F PARENT-SOURCE-TEST-REVIEW-11 REPAIR8 P0-0-P1-2-P2-0 REPAIR-REQUIRED LORENTZ-OWNER-RETAINED STORY-SAFETY-GATE-MISSING CROSS-CONSUMER-PRODUCTION-HARNESS-MISSING NOT-FORMAL-TEST-READY 2026-07-21T05:29:39-04:00 -->

## 71. Repair #8 Review #11 Whole-Card Source + Test Re-Delivery - LORENTZ

- `ack_parent_message=2026-07-21T05:29:39-04:00`.
- Canonical status: `WHOLE-CARD SOURCE+TEST RE-DELIVERED / REVIEW11 TWO-P1 CLOSED IN SOURCE+FOCUSED CONTRACTS /
  LORENTZ OWNER RETAINED / PARENT REVIEW REQUIRED / NOT APPROVED / NOT PASSED / NOT FORMAL TEST READY`.
- Read-only dirty baseline remains `D:\mavenProject\DHXY@696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` and was
  neither written, built, nor run. Client production/test bytes and the other eight Repair #8 findings were not edited.

### 71.1 Review #11 P1 closure

1. **Maintenance STORY safety.** `XiuluoTaskV2` still reads the observer-owned dialog fact with the unchanged 3000ms
   maximum age and performs no second classification. Its production close path now passes the bound
   `TaskExecutionContext` into the close policy and rejects STORY when `windowRole=MEMBER` and
   `GameContext.ActionState != IN_COMBAT`. `DialogService.closeObservedDialog(...)` requires the resulting
   `storyClickAllowed` value and emits no STORY input when false; OPTION behavior is unchanged. The production-path test
   enters the private Xiuluo policy, then the real DialogService and scripted turn port: member+FREE emits zero actions;
   leader+FREE and member+IN_COMBAT each emit exactly one INPUT and zero CAPTURE/classification actions.
2. **Fourteen real consumer gates.** Each section 68.2 consumer now owns a public production seam named
   `acceptsStableOutcomeWindow(...)`, and every pre-existing outcome/window branch in that consumer calls its own seam.
   The rewritten parameterized contract supplies fourteen direct Java method references, not names or labels. Each method
   reference executes once with all mutable turn facts advanced and once with HWND changed: `14 x 2 = 28` production-seam
   cases. The test contains no source read/`contains`, consumer string label, or direct call to
   `TurnWindowMetadata.hasSameStableWindowIdentity(...)`; the shared helper remains implementation behind each real gate.

The fourteen driven production entries are in `AutoCombatPanelService`, `BattleRadarService`, `DialogService`,
`CloudLeftTopStatusPortAssembly`, `NavigationService`, `NpcClickService`, `CloudPlayerStateFirstAidPort`,
`CloudPlayerStateIncenseStatusPort`, `SummonSkillService`, `TaskTrackerPanelService`, `CloudCommonBoxPortAssembly`,
`CloudDialogDetectionPort`, `CloudDialogPreparedActionValidationPort`, and `CloudTeamReturnPortAssembly`.

### 71.2 Exact Review #11 write set

| Cloud file | Bytes | mtime (EDT) | SHA-256 |
|---|---:|---|---|
| `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java` | 43396 | 2026-07-21T05:35:23.068-04:00 | `53D509D7A199C4FA494E35BAD8426E76DC31C06956FB295248D955CB4C1F556E` |
| `src/main/java/com/bot/dhxy/service/BattleRadarService.java` | 67015 | 2026-07-21T05:35:23.069-04:00 | `5789FAEA3938D610B05F6DE271A33E025EB067010FCEE869AB2DF01F81723310` |
| `src/main/java/com/bot/dhxy/service/DialogService.java` | 160258 | 2026-07-21T05:35:23.071-04:00 | `C170D3AED0309AB5FD23E61BF953BFAFC0380E387D3267F60A81A75800EC3D5B` |
| `src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java` | 20310 | 2026-07-21T05:35:23.071-04:00 | `3025BD37040C6D9839475ECBDF9930E2C1E564A01BE8BF8B250CF3B872D83562` |
| `src/main/java/com/bot/dhxy/service/NavigationService.java` | 183764 | 2026-07-21T05:35:23.073-04:00 | `A178A521D3EC2F3E65CACEAD555DD9AF3757622BF254E8CC390034261446E6A6` |
| `src/main/java/com/bot/dhxy/service/NpcClickService.java` | 218775 | 2026-07-21T05:35:23.074-04:00 | `D11E004ED45D32A45076644F16DB02909F35152A6398D8DACF563CE9026A7349` |
| `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateFirstAidPort.java` | 22577 | 2026-07-21T05:35:23.075-04:00 | `A6EDA0CA6506D7FF5C755271A2F2CB33985EEAB676BC12E55103238C2D7CD996` |
| `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateIncenseStatusPort.java` | 16332 | 2026-07-21T05:35:23.076-04:00 | `7D282C2913462E59A79047BCE048B8F8E556A975EDE9927744277CBAE6BCE0A4` |
| `src/main/java/com/bot/dhxy/service/SummonSkillService.java` | 69436 | 2026-07-21T05:35:23.076-04:00 | `FA4187D19D811442EFD4C7F6D9BC1F09088F12C063ADBE91460A04D41658FCB4` |
| `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java` | 58040 | 2026-07-21T05:35:23.077-04:00 | `B25C7333307F138487D9E18B4F148B5695F8D29C7DF7EB526DAFBB701B63CC77` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java` | 24581 | 2026-07-21T05:35:23.078-04:00 | `0ED49B39AB2E0579E53677970C22E32EC13AE94AFFC939AED5659585E4F15F7A` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java` | 17026 | 2026-07-21T05:35:23.078-04:00 | `63232AC783C9E68CEE528E13855E1853197DCD46F42BC620BF82E0C492211316` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java` | 13756 | 2026-07-21T05:35:23.079-04:00 | `F8B711E8337BC64D604770D7E42C4D9B3CD0816E8195DC14157DCE63008C99FB` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java` | 25845 | 2026-07-21T05:35:23.080-04:00 | `9B434629756AD9CCA05C8E69A8E0E05F41D371A5235D72FF90C6DABC0115FF02` |
| `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` | 268435 | 2026-07-21T05:35:23.081-04:00 | `03387BA11822744FE3F143201748AC98D82D62255BAFCB2F6985BD854C556E79` |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/StableWindowIdentityCrossConsumerContractTest.java` | 4299 | 2026-07-21T05:35:59.176-04:00 | `544DFFB551C4E2615F7A6252934C0E15B0D4FE41E9498F947624D7146CAE58C5` |
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java` | 110558 | 2026-07-21T05:36:44.572-04:00 | `C5D17ED584342D130643251B9846C938FE8B7C5F145B4B87E09C7211993F2417` |
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java` | 50542 | 2026-07-21T05:36:44.573-04:00 | `6A0A893C2947569F21C727633D86264E773A98CE7CFD38EC9C35AE5628C9945B` |

### 71.3 Named tests and compile evidence

- Cloud command:
  `mvn -q "-Dtest=StableWindowIdentityCrossConsumerContractTest,BattleRadarTurnContractTest#outcomeDynamicTurnFactsDoNotInvalidateStableBattleWindow,DialogDetectionTurnContractTest#captureAcceptsAPathingFactThatAdvancedAfterTheExactActionWasExposed,DialogOptionTurnContractTest#observedOptionCloseUsesKeywordActionWithoutASecondDialogClassification,AutoCombatPanelTurnContractTest#visibleRoundsOcrDoesNotOverrideHealthyCachedCounter,SummonSkillTurnContractTest#panelAnchorAndSkillPageUseSingleEightHundredMillisecondRechecks+ordinaryDeletePreservesSelectDialogConfirmPostDeleteOrderAndLimits+confirmedDeleteIsNotSuccessfulWhenPostDeleteStoryClickFails,XiuluoWholeTaskTurnContractTest#acceptTrackerUsesCallerFrameAndParserExceptionDegradesToMiss+exceptionalAcceptTrackerFutureAndMissingMetadataRemainRecoverable+maintenanceCloseUsesThreeSecondObserverPolicyAndRealObservedAction+maintenanceStorySafetyGateSuppressesMemberNonCombatInputAndAllowsLeaderOrCombatMember" test`.
  Surefire reports: `28 + 1 + 1 + 1 + 1 + 3 + 4 = 39`, failures/errors/skips=`0/0/0`, command exit 0.
- Frozen Client command:
  `mvn -q "-Dtest=TurnInputStepExecutorContractTest,InputActionWorkerAltFallbackContractTest,LocalTurnActionExecutorContractTest" test`.
  Surefire reports: `5 + 3 + 2 = 10`, failures/errors/skips=`0/0/0`, command exit 0.
- Combined focused production-path contracts: `49`, failures/errors/skips=`0/0/0`. Final Client and Cloud
  `mvn -q -DskipTests=false compile` both exited 0. Both writable repos' `git diff --check` exited 0 with only pre-existing
  line-ending conversion warnings.
- No runtime/application/server/task/UI/live capture/input or Git mutation occurred. This remains worker evidence pending
  the parent-only final review.

<!-- TRUE_EOF: TURN-40F REPAIR8 REVIEW11 WHOLE-CARD-SOURCE-TEST-RE-DELIVERED ACK-PARENT-MESSAGE-2026-07-21T05-29-39-04-00 LORENTZ-019F83CA-F262-7382-A8D4-68C07719E0CF OWNER-RETAINED TWO-P1-CLOSED-SOURCE-FOCUSED-CONTRACTS TESTS-49-ZERO-FAILURE-ERROR-SKIP CLIENT-COMPILE-EXIT0 CLOUD-COMPILE-EXIT0 BASELINE-696A12B0-READONLY PARENT-REVIEW-REQUIRED NOT-APPROVED NOT-PASSED NOT-FORMAL-TEST-READY 2026-07-21T05:39:24-04:00 -->

## 72. Repair #8 Review #11 Superseding Whole-Card Delivery - Existing Member Helper Reused

- `ack_parent_message=2026-07-21T05:29:39-04:00`.
- This section supersedes section 71's fingerprint and STORY-gate wording after the parent's in-progress note. The only
  source adjustment is that `XiuluoTaskV2.closeMaintenanceObserverDialog(...)` now calls the existing null-safe,
  case-insensitive `isMemberWindow(context)` production helper instead of comparing role text locally. The negative test
  uses lowercase `member` and still proves zero turn actions for non-combat; leader+FREE and member+IN_COMBAT still each
  produce exactly one INPUT and no CAPTURE/classification action.
- The 3-second observer fact, no-second-classification rule, fourteen direct consumer production method references, other
  Repair #8 bytes, and all workspace restrictions remain unchanged.
- Canonical status: `WHOLE-CARD SOURCE+TEST RE-DELIVERED / REVIEW11 TWO-P1 CLOSED IN SOURCE+FOCUSED CONTRACTS /
  LORENTZ OWNER RETAINED / PARENT REVIEW REQUIRED / NOT APPROVED / NOT PASSED / NOT FORMAL TEST READY`.

### 72.1 Exact final Review #11 write set

| Cloud file | Bytes | mtime (EDT) | SHA-256 |
|---|---:|---|---|
| `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java` | 43396 | 2026-07-21T05:35:23.068-04:00 | `53D509D7A199C4FA494E35BAD8426E76DC31C06956FB295248D955CB4C1F556E` |
| `src/main/java/com/bot/dhxy/service/BattleRadarService.java` | 67015 | 2026-07-21T05:35:23.069-04:00 | `5789FAEA3938D610B05F6DE271A33E025EB067010FCEE869AB2DF01F81723310` |
| `src/main/java/com/bot/dhxy/service/DialogService.java` | 160258 | 2026-07-21T05:35:23.071-04:00 | `C170D3AED0309AB5FD23E61BF953BFAFC0380E387D3267F60A81A75800EC3D5B` |
| `src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java` | 20310 | 2026-07-21T05:35:23.071-04:00 | `3025BD37040C6D9839475ECBDF9930E2C1E564A01BE8BF8B250CF3B872D83562` |
| `src/main/java/com/bot/dhxy/service/NavigationService.java` | 183764 | 2026-07-21T05:35:23.073-04:00 | `A178A521D3EC2F3E65CACEAD555DD9AF3757622BF254E8CC390034261446E6A6` |
| `src/main/java/com/bot/dhxy/service/NpcClickService.java` | 218775 | 2026-07-21T05:35:23.074-04:00 | `D11E004ED45D32A45076644F16DB02909F35152A6398D8DACF563CE9026A7349` |
| `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateFirstAidPort.java` | 22577 | 2026-07-21T05:35:23.075-04:00 | `A6EDA0CA6506D7FF5C755271A2F2CB33985EEAB676BC12E55103238C2D7CD996` |
| `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateIncenseStatusPort.java` | 16332 | 2026-07-21T05:35:23.076-04:00 | `7D282C2913462E59A79047BCE048B8F8E556A975EDE9927744277CBAE6BCE0A4` |
| `src/main/java/com/bot/dhxy/service/SummonSkillService.java` | 69436 | 2026-07-21T05:35:23.076-04:00 | `FA4187D19D811442EFD4C7F6D9BC1F09088F12C063ADBE91460A04D41658FCB4` |
| `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java` | 58040 | 2026-07-21T05:35:23.077-04:00 | `B25C7333307F138487D9E18B4F148B5695F8D29C7DF7EB526DAFBB701B63CC77` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java` | 24581 | 2026-07-21T05:35:23.078-04:00 | `0ED49B39AB2E0579E53677970C22E32EC13AE94AFFC939AED5659585E4F15F7A` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java` | 17026 | 2026-07-21T05:35:23.078-04:00 | `63232AC783C9E68CEE528E13855E1853197DCD46F42BC620BF82E0C492211316` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java` | 13756 | 2026-07-21T05:35:23.079-04:00 | `F8B711E8337BC64D604770D7E42C4D9B3CD0816E8195DC14157DCE63008C99FB` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java` | 25845 | 2026-07-21T05:35:23.080-04:00 | `9B434629756AD9CCA05C8E69A8E0E05F41D371A5235D72FF90C6DABC0115FF02` |
| `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` | 268418 | 2026-07-21T05:40:48.573-04:00 | `2F9D2AFAD84ABB657C0FFF76BCB1A72DA1DD0EE3B27B7DE5F2BE75CB3C7DE9B0` |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/StableWindowIdentityCrossConsumerContractTest.java` | 4299 | 2026-07-21T05:35:59.176-04:00 | `544DFFB551C4E2615F7A6252934C0E15B0D4FE41E9498F947624D7146CAE58C5` |
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java` | 110558 | 2026-07-21T05:36:44.572-04:00 | `C5D17ED584342D130643251B9846C938FE8B7C5F145B4B87E09C7211993F2417` |
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java` | 50552 | 2026-07-21T05:40:48.574-04:00 | `CBBB91DDD113A8341647BA371581A93C7EFC5F63F9E02253BF5DA2AC863C199F` |

### 72.2 Final rerun evidence

- The exact Cloud named command in section 71.3 was rerun after the helper reuse: 39 tests,
  failures/errors/skips=`0/0/0`, exit 0. The exact frozen Client named command was rerun: 10 tests,
  failures/errors/skips=`0/0/0`, exit 0.
- Final combined focused count remains 49. Client and Cloud `mvn -q -DskipTests=false compile` were rerun after the final
  edit and both exited 0. No runtime/UI/live capture/input, baseline build/write/run, or Git mutation occurred.

<!-- TRUE_EOF: TURN-40F REPAIR8 REVIEW11 SUPERSEDING-WHOLE-CARD-DELIVERY ACK-PARENT-MESSAGE-2026-07-21T05-29-39-04-00 EXISTING-ISMEMBERWINDOW-REUSED LOWERCASE-MEMBER-ZERO-INPUT FOURTEEN-CONSUMER-PRODUCTION-SEAMS TESTS-49-ZERO-FAILURE-ERROR-SKIP CLIENT-COMPILE-EXIT0 CLOUD-COMPILE-EXIT0 LORENTZ-OWNER-RETAINED PARENT-REVIEW-REQUIRED NOT-APPROVED NOT-PASSED NOT-FORMAL-TEST-READY 2026-07-21T05:42:01-04:00 -->

## 73. Parent Source + Test Final Review #12 - Repair #8 Passed

- Parent final verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / ZERO OWNER / FORMAL USER TEST READY`.
- Parent reviewed the superseding section 72 write set against the read-only dirty baseline
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`. The 3000ms observer-owned maintenance fact remains the only dialog
  classification; `isMemberWindow(context)` preserves null-safe/case-insensitive member detection, member + non-combat
  emits zero actions, and leader or member + combat emits exactly one INPUT with no CAPTURE.
- All fourteen consumer outcome branches call their own `acceptsStableOutcomeWindow(...)` production entry. The
  parameterized contract holds fourteen direct method references and executes 28 production-entry cases: mutable turn
  facts accepted and changed HWND rejected. No consumer labels, source-string assertions, or direct helper-only
  acceptance remains.
- The parent independently reran the exact named families: Client `10/0/0/0`, Cloud `39/0/0/0`, combined
  `49/0/0/0` for tests/failures/errors/skips. Client and Cloud `mvn -q -DskipTests=false compile` both exited 0;
  both `git diff --check` commands exited 0 with only pre-existing line-ending warnings.
- No runtime/application/server/task/UI/live capture/input was invoked. The protected baseline was not written, built,
  run, or switched. All P1 source/test gates are closed; remaining P2 cleanup does not block formal user runtime testing
  but still prevents claiming the entire migration complete.

<!-- TRUE_EOF: TURN-40F PARENT-SOURCE-TEST-FINAL-REVIEW-12 REPAIR8 P0-0-P1-0-P2-0 SOURCE-TEST-SOURCE-REVIEW-PASSED ZERO-OWNER TESTS-49-ZERO-FAILURE-ERROR-SKIP CLIENT-COMPILE-PASS CLOUD-COMPILE-PASS FORMAL-USER-TEST-READY P2-REMAINS NOT-MIGRATION-COMPLETE 2026-07-21T05:46:00-04:00 -->
