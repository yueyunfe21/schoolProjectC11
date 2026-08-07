# G008：暂停/停止旧运行清场与热启动

## 范围

本卡处理 HTTPS turn 的运行生命周期及最小 Cloud 启动屏幕观察合同；不修改任务业务 phase、OCR、模板、绿链、
坐标或任何 Client 本地输入逻辑。

## 已移植内容

1. 暂停和停止都会结束旧 remote turn，以便清空旧观察、输入与 phase；但两者的窗口业务状态不同：
   暂停完成后必须保持 `PAUSED`，停止才投影为 `STOPPED`。暂停后的“继续”会按当前屏幕创建 fresh run，
   不原地 resume 旧 turn。
2. 旧 `RemoteTaskHandle` 在清态前失效；晚到 terminal 与旧 `TurnExecutionWindow` 通过 handle 身份围栏失效。
3. 清理旧 stop token 尚未开始的输入；已开始的单个原子输入事务可完成，身份 epoch 漂移仍会中断。
4. 每次 abort 均调用 `WindowRuntimeContext.clearTaskExecutionState(...)`；cr271 不含 `WindowReadyEventBus`，其等价的本地 ready/执行态均由该上下文清理。
5. UI 所有“继续”入口改为“重新启动”，主启动路径不再调用 `resumeWindows(...)`。独立审查发现
   窗口行的暂停按钮仍残留 `resumeWindows(...)`；已于 2026-08-03 修为
   `startWindows(List.of(windowId), "重新启动")`，tooltip 同步更新。

## 真实运行验收

重启后，`logs/dhxy-console.log` 不得再出现 `Start selected task flow: resume paused` 或
`Remote resume remains paused because the original window is not present`。应看到 fresh-run 启动日志，
并且暂停前旧 run 的 terminal 不得重新投影到新 run。

## 2026-08-04 fresh runtime 纠正：清场不等于停止

`02:29:18` 的天庭实机运行证明，第 1 阶段把 pause 和 stop 共用同一个
`abortRemoteRuns(..., STOPPED)`，导致用户点击暂停后旧 Cloud turn 返回 `STOPPED`，窗口和队列也被投影成
“已停止”。这违反用户确认的暂停语义：暂停应清空旧运行态，但窗口仍处于暂停中；恢复时才按当前画面创建新 run。

修复合同：

1. pause 在请求 Cloud 结束旧 turn 前，先作废旧 `RemoteTaskHandle`、取消旧 token 尚未开始的输入并清空
   `WindowRuntimeContext` 任务执行态，从而让旧 terminal 通过 handle identity fence 失效；
2. pause 清场完成后投影 `WindowRuntimeStatus.PAUSED`，保留窗口绑定、选择任务和待启动队列；不得写
   `STOPPED`、不得把这次用户操作记为任务自然结束或队列完成；
3. stop 继续投影 `STOPPED`，两条路径不得再共用一个写死 `STOPPED` 的 helper；
4. PAUSED 窗口点击继续时创建全新的 remote run，但这是“暂停热恢复”，不是冷启动：按当前屏幕观察并由
   task-specific Cloud consumer 恢复业务，不继承暂停前 phase，也不重跑窗口发现、`Alt+T` 队伍预检、
   包裹页校准或公共启动 UI 前置；
5. UI 日志必须在分支后明确打印 `action=PAUSE`、`action=PAUSE_RESUME` 或 `action=COLD_START`，不能继续用分支前统一的
   `UI start button clicked` 冒充实际动作。

当前状态：`P1 SOURCE REPAIR DELIVERED / FRESH RUNTIME REQUIRED`。Client 已在请求 Cloud 停止旧 turn 前先
失效旧 handle/token、清空 task-owned context，并按调用方显式状态投影：pause 为 `PAUSED`、stop 为
`STOPPED`。旧 terminal 回调因 handle identity fence 不再有资格覆盖暂停状态；主按钮日志也已改为分支后的
`action=PAUSE/FRESH_START`。`mvn -q -DskipTests compile` 与精确写集 `git diff --check` 均通过。
尚需 fresh runtime 验证暂停后五个窗口保持 `PAUSED`，随后启动创建新 `startRequestId` 并按当前屏幕恢复；
在该证据出现前不得关闭 G008。

## 2026-08-04 fresh runtime P1：暂停热恢复被冷启动队伍预检拒绝

`03:28:22` 的暂停边界已正确工作：五个窗口保持 `PAUSED`，旧 run 的五个 `STOPPED` terminal 全部由
`Ignore stale remote terminal after task replacement` 拦截。`03:28:31` 用户从战斗画面点击继续时，UI 却错误进入
`action=FRESH_START`，并在创建任何新 `startRequestId` 之前无条件调用了冷启动专用的
`LocalTeamRolePreflightService.prepareBatch(...)`。该服务依次发送 `Alt+T`，随后要求五秒内在队伍面板 ROI 命中
队长按钮；战斗画面不能提供该面板，因此五窗口全部返回“本地队伍菜单在 5 秒内未命中队长按钮”，最终
`远程启动未执行：0/5`。没有新 runner/observer，故脱战后仍显示暂停并非状态被改回，而是恢复从未成功。

修复合同：pause 只清 task-run-owned phase/事实/输入；`WindowRuntimeContext` 已明确保留 exact HWND 绑定、
LEADER/MEMBER 角色、已选任务和玩家身份。PAUSED 点击继续必须走独立的暂停热恢复入口：直接以这些已保留的
lifecycle identity 创建新的 `teamSessionKey/startRequestId`，禁止重新发现窗口、禁止 `Alt+T` 队伍预检、禁止
包裹页校准和公共启动 UI 前置。若恢复时仍在战斗，新 run 只观察战斗并进入“启动等待战斗结束”；真正脱战后直接
进入当前任务的 tracker/dialog/回程道具热恢复链，不能再补跑冷启动前置。若保留的绑定或角色本身缺失，恢复应
fail-closed 并明确报 lifecycle identity 缺失，不能退回可见 UI 预检，更不能猜 UNKNOWN。显式冷启动仍使用原有
完整前置，两条入口不得混用。

状态：`PARENT SOURCE REVIEW APPROVED / P0-P1-P2=0 / FRESH RUNTIME REQUIRED`。不得把源码通过视为
G008 fresh-runtime 通过。

实施 owner：worker Faraday（`019fcbba-16bf-7bb0-9fc0-2fdf3077de3f`）。父级 Codex 负责独立源码审核、
定向契约门和 Client compile；worker 不得修改 Cloud、任务 phase、协议、模板/ROI、导航或输入动作，也不得自批通过。

## 2026-08-04 父级源码复审

父级逐条核对 worker 的四文件精确写集，当前未发现 P0/P1/P2：

1. 主按钮与其他重新启动入口均把“全部 `PAUSED`”分流到独立 `resumePaused(...)`；混合
   `PAUSED`/非 `PAUSED` 在任何窗口启动前 fail-closed；全部非暂停窗口保持冷启动。
2. 暂停热恢复不调用 `registerDetectedGameWindows(...)`、`prepareLocalTeamRoles(...)` 或
   `calibrateMainBagTaskTabBeforeRemoteStart(...)`。它只校验保留 HWND 仍有效，并复用保留角色、已选任务和玩家身份。
3. 缺失 HWND、`UNKNOWN` 角色或组队任务不是唯一 `LEADER` 时整组拒绝，不使用
   `unknownPreflight(...)` 猜成员身份；身份校验完成后才创建任何新 remote loop。
4. 每次恢复重新生成 `teamSessionKey` 与 `startRequestId`，并在 ACK 前调用
   `prepareRemoteFreshStart(...)` 清旧 run 执行态；战斗中恢复继续依赖既有新 observer/Cloud 战斗等待，不新增本地
   combat 轮询、截图或输入。
5. worker 实际只修改 `MainWindowController.java`、`WindowTaskControlService.java`、
   `WindowRemoteTurnControlContractTest.java`、`MainWindowControllerSourceGuardTest.java`，未碰 Cloud、任务 phase、
   协议、模板、导航或输入。

父级验证：`mvn -q -DskipTests compile` exit `0`；隔离 JUnit 的四个 G008 生命周期合同 `4/4` 通过；
`MainWindowControllerSourceGuardTest` 通过；精确四文件 `git diff --check` 通过（只有既有 LF/CRLF 提示）。直接
`mvn -q -Dtest=WindowRemoteTurnControlContractTest test` 仍被仓内既有
`TurnContractFixtures.LocalPathingStartProofMechanics` 缺失阻断于 test-compile，未把它伪报为通过。

Fresh gate：用户重启当前 Client 后，在战斗中暂停约 10 秒再点击启动。日志必须出现
`action=PAUSE_RESUME`、全新的 `teamSessionKey/startRequestId` 和新 observer；不得出现窗口重新发现、`Alt+T`、
包裹页校准或 `action=COLD_START`。脱战后必须从当前任务的 tracker/dialog/回程恢复继续，五窗口不能继续停留在
`PAUSED`。满足前 G008 不关闭。

## 状态

第 1 阶段 fresh runtime 发现的 pause/stop 状态混同 P1 已完成源码返修，待 fresh runtime；第 2 阶段同样待
fresh runtime。第 2 阶段的 Cloud demand/消费链已交付：`WUHUAN_V2`、
`XIULUO_V2`、`WUBEI` 都只由 Cloud 解释启动屏幕，并且只会发布各任务已经存在的
`PreparedAction`。本卡实现已完整，但没有 fresh runtime 证据前不得关闭。

## 第 2 阶段：重新启动按当前屏幕热启动（Client + Cloud 协议已交付）

### 已实施的客户端边界

1. `WindowTaskControlService.startOneRemote(...)` 在新 Cloud start ACK 之前调用
   `WindowTaskRunner.prepareRemoteFreshStart(...)`：作废遗留 handle、取消该旧 token 尚未开始的输入、
   清空旧 queue/运行态/`WindowRuntimeContext` 执行态。新 run 因此不会继承暂停前 phase 或事实。
2. `WindowTaskRunner.markRemoteStarted(...)` 不再在 ACK 后第二次清空上下文。`WindowTurnLoop` 会在
   ACK 处理中启动新的 `WindowObservationRunner`；旧实现会在该第一帧已经开始观察后擦掉新状态。
3. 新 observation runner 的首个成功请求带现有可选字段
   `source=startup-screen-observation`。五环新 run 会在同一张共享 HWND 帧上，只读产出既有
   `WUHUAN_TITLE_PRESENCE` 与 `WUHUAN_DIALOG_PRESENCE`；修罗、五倍不自行匹配或点击启动
   dialog，而是只在 Cloud 的 exact prepared-frame demand 到达后回传该 demand 指定的帧。
   三者都不捏造本地 phase，任何输入仍只走 Cloud `PreparedAction` 的既有消费通路。
4. 战斗热启动仍由现有本地 combat 观察保持自动战斗；真脱战后才由既有 tracker/title 事实继续。
   本地看打、天庭本地模板已存在的“命中并执行”事件保持原协议事件上报，未新增第二条本地业务动作链。

### Cloud 启动帧合同（2026-08-03 已实施）

Cloud 复用既有 typed `ObservationPreparedFrameDemand` / `ObservationPreparedFrame`，没有扩展
`TurnTaskStartRequest`、没有新增本地 phase，也没有建立 Client 自主上传整屏的通道：

- 五环：首个 `startup-screen-observation` 同时报告
  `WUHUAN_DIALOG_PRESENCE=present` 时，Cloud 才创建一次、精确绑定
  `windowId/hwnd/taskRunId/generation` 的 `wuhuan-startup-screen-dialog` demand。Cloud 分类后只发布
  既有 `DialogOperation.INSPECT`、`clickRequired=false` 的 PreparedAction 和
  `PREPARED_ACTION_READY`；五环既有 Cloud task 决定下一步 task-specific interest/动作。
- 修罗：首个 `startup-screen-observation` 由 Cloud 创建一次
  `startup-screen-inspection:XIULUO_V2` demand。Cloud 只在整个启动帧被识别为 `OPTION_DIALOG` 后，
  用既有 `xiuluo_accept_xianlaiwu.png` 或 `xiuluo_enter_battle_kanda.png` 模板生成
  `ACCEPT_TASK` 或 `XIULUO_ENTER_BATTLE` PreparedAction；`XiuluoTaskV2` 仅在已有
  `ACCEPT_TASK_DIALOG` / `WAIT_TRACKER_SHORTCUT_PATHING` 消费它。故事框、未知 option 和模板 miss
  都不发布动作，不猜 phase。
- 五倍：首个 `startup-screen-observation` 由 Cloud 创建一次
  `startup-screen-inspection:WUBEI` demand。Cloud 只用既有五倍 accept / enter-battle template catalog
  生成 `WUBEI_ACCEPT_TASK` 或 `WUBEI_ENTER_BATTLE` PreparedAction；`WubeiTask` 把它映射回已有
  `ACCEPT_TASK` / `ENTER_BATTLE` phase，再由原有 priority consumer 校验并执行。没有匹配到既有模板时
  不创建 action。
- 战斗中：启动 observation 已报告 `COMBAT_SIGNAL=VISIBLE:*` 时 Cloud 不创建 demand；修罗进入既有
  `WAIT_COMBAT`、五倍进入既有 `WAIT_BATTLE_FINISH` 并维持自动战斗，只有 verified exit 才继续。
- 无当前任务 tracker：不从启动屏幕猜测旧 phase。各任务保持其原有恢复顺序：
  `tracker -> 回程道具 -> 已验证 saved context -> 接任务入口`；本地模板已执行时沿用既有
  key-event/result 事实，不另发启动点击。

未覆盖任务：`AUTO_BATTLE`、`SLEEP_COMPUTER` 没有 Cloud whole-task 的任务对话框 phase，legacy
`TaskType.XIULUO` 也没有可启动的 `TurnTaskCode` / Cloud task 实现。它们不请求 startup demand；
这不是遗漏，而是没有可复用的既有 task-specific PreparedAction 语义。所有未知 task code 同样
fail-closed，不能按视觉存在直接点击或猜 phase。

### 验证与 fresh runtime 门

- Cloud `mvn -o compile -DskipTests=false`：2026-08-03 最终重跑通过。
- Client `mvn -q -DskipTests compile`：2026-08-03 最终重跑通过。
- `ObservationFactType.java` 已以 SHA-256 `294A7079211D06C6AFD940A3ACC8730E885B3591CE07AFC249BA96E23237487E`
  在两仓逐字节一致。
- 定向 `mvn -q -Dtest=WindowObservationRunnerContractTest test`：未运行到目标类；现有
  `LocalTurnActionExecutorContractTest` 仍引用已删除的 `LocalPathingStartProofMechanics`（第 724、1260 行），
  Maven 在 test-compile 先失败。未伪造绿灯，也未修改该写集外历史测试。
- fresh runtime（由用户执行）：在走路、已有 accept / 看打 dialog、战斗三种画面分别暂停再重新启动。
  日志须显示新的 `startRequestId`、`startup-screen-observation`、exact demand、Cloud prepared operation、
  原 phase consumer 以及旧 run fence；不得出现本地凭 dialog 自行点击或自行决定 phase。
