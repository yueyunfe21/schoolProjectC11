# TURN-40G：本地 Window Observation Runner 与修罗开打快速通道

## 1. 卡片身份

- 状态：`READY / ZERO OWNER / PLAN CONTRACT FROZEN / IMPLEMENTATION NOT STARTED`。
- 类型：CR271 runtime architecture repair，位于 TURN-40F/41 之后、P2 删除清理之前。
- 原因：Cloud `CloudWholeTaskObserver` 通过普通 turn command slot 主动拉取观察数据，会与前台 Cloud Task
  争用每窗口唯一 unresolved action；同时，修罗已验证的本地 `local-kanda` 小 ROI 快速通道在全云迁移中被删除。
- 用户批准决策：恢复**本地常驻 Observation Runner**；恢复 Git 最新已验证版本 CR232/253/256 的修罗
  `local-kanda` 快速通道；Cloud 继续持有任务 phase、业务解释、持久记忆和异常兜底。
- 业务基线：通用任务语义以 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 与
  `docs/业务逻辑.md` 为准；修罗开打 Observer/仲裁细节以 Git `59b85e0bb494f43ad7e7434f3d2170deb373c6ef`
  中 CR232/253/256 已验证实现为补充权威。
- 业务差异：仅改变观察职责部署位置和通信平面；**无已批准任务 phase、导航顺序、重试顺序、模板阈值、
  点击坐标或战斗确认语义差异**。

## 2. 要解决的问题

1. `CloudWholeTaskObserver` 与 Cloud Task 都能生产普通 turn action；每窗口单槽只能容纳一个 unresolved action，
   Observer 截图/读 fact 时可能把前台任务挡成 `BUSY` 或长期等待。
2. 全部观察经 Cloud round-trip 会把本应常驻、低成本的小 ROI 探测变成串行网络动作，破坏 Observer 实时性。
3. 修罗旧版已经证明本地 `41x21` 原图模板匹配是正常主路径；历史样本最近 `17/17` 由
   `local-kanda` 命中。删除该路径后，开打只能依赖更慢且会占 turn 的 Cloud 观察。
4. 不能用“再加一把锁”或延长 command lease 掩盖问题；必须从结构上把 observation producer 与 command
   producer 分离，同时保留唯一物理输入队列。

## 3. 完成后的职责边界

### 3.0 与现有多窗口启动 P1 的关系

- 当前“5个窗口仅首窗口收到start ACK”的直接根因是 Cloud `CloudTurnTaskRegistry` 进程级单槽；它不是
  TURN-40G 的 observation/command 争槽根因，必须由既有多窗口隔离repair单独关闭。
- TURN-40G 不得顺手修改 registry task ownership；其五窗口验收只证明：在5个窗口都能合法启动的前提下，
  5个本地Runner不会新增 command `BUSY`、跨窗口样本或输入串线。
- 正式五窗口runtime同时依赖“registry P1已关闭”和“TURN-40G通过”，任一未通过都不得称迁移完成。

### 3.1 本地 Runner 拥有

- 每个已获 Cloud start ACK 的窗口一个 `WindowObservationRunner`；绑定 exact `windowId + hwnd + taskRunId`。
- HWND 后台小区域截图、动态 interest、采样节流、`observerSeq`、最多一个 in-flight analysis 和 latest-wins 合并。
- 固定模板、像素差、计时器边沿等无业务解释的轻量机械信号。
- 关键边沿在 Cloud ACK 前保留并重发；普通快照允许被同窗口更新样本覆盖。
- 修罗 `local-kanda` 是唯一首期主动动作例外：命中后在本地完成 consume-time 复验和一次性仲裁，再进入
  现有 `InputActionQueue`。

### 3.2 Cloud 继续拥有

- 修罗/五倍/五环 phase、任务 interest、pathing terminal 分类、ready-event、路线学习和持久 memory。
- OCR、任务追踪语义、通用 dialog catalog、prepared candidate 的 Cloud owner/CAS。
- 修罗 stopped-static Cloud fallback：识别“看打”坐标或明确返回 `CLOUD_NO_ACTION`。
- 对 `ENTER_BATTLE_CLICKED`、`IN_COMBAT`、fallback 和 retry 结果推进任务状态。

### 3.3 永远留在唯一 command/input 通道

- 除 `local-kanda` 明确例外外的所有鼠标、键盘、窗口聚焦、开关地图、面板调整和 prepared 点击。
- `local-kanda` 也不得直接调用 Robot/InputProvider 绕过队列；它只是本地生成动作，物理执行仍通过同一个
  `InputActionQueue`，并把 move + click + required delay 组成一个原子 request。
- Observer 不得执行自动战斗 bootstrap、NPC 点击、任务追踪点击、OCR dialog 点击或任务 phase 转换。

## 4. 双平面通信合同

### 4.1 Command plane（保持现有）

- `/api/v1/client/turn` 继续承担 Cloud -> Client 主动命令和 outcome。
- 每窗口保持唯一 unresolved action、actionId 幂等、generation/binding refresh 和 stop fencing。
- TURN-40G 不允许增加第二 command store、第二输入队列或绕过 `CloudTurnExchange` 的普通业务命令。

### 4.2 Observation plane（新增独立端点）

- 建立独立 observation HTTP endpoint/client；不得复用 `/api/v1/client/turn` 的 currentAction slot。
- 请求最少字段：`tenantId/deviceId/windowId/hwnd/taskCode/taskRunId/observerSeq/capturedAtMs/interestRevision`。
- 关联字段按类型携带：`intentId/attemptId/round/source/activeCommandActionId`。
- payload 可包含轻量事实、关键边沿和按 interest 选择的小 ROI；不得默认上传整窗口帧。
- 响应最少字段：`acceptedObserverSeq/interestRevision/acknowledgedEventIds/analysisResults`。
- 普通响应只提供分析结果或更新 interest，不直接授予本地执行普通业务动作的权限。
- 请求必须沿用现有 tenant/device 鉴权、大小限制、严格 JSON schema 和 stable-window identity 校验。

### 4.3 顺序、背压与陈旧拒绝

- 每窗口最多一个 analysis request in flight；新普通快照覆盖未发送旧普通快照，不无限排队。
- 战斗进入/退出、pathing terminal、pre-battle timeout、local-kanda click result 等关键事件有独立 eventId，
  Cloud ACK 前不得丢失。
- Client 丢弃 taskRunId、HWND、interestRevision、intentId 或 attemptId 不匹配的响应。
- Cloud 对重复 `observerSeq/eventId` 幂等；低序列响应不得覆盖高序列状态。
- 网络不可用时本地只保留有界 latest snapshot + 未 ACK 关键边沿；不得把“网络失败”解释成业务 miss、
  `NONE`、`FREE`、`ARRIVED` 或可重试命令。

## 5. Runner 生命周期

1. Client 收到匹配 `taskStartAck` 后，`WindowTurnLoop` 才启动该窗口 Runner；ACK 前不得截图或输入。
2. Runner 使用与 turn loop 相同的 exact `WindowRuntimeContext`/native binding，不按标题重新搜索窗口。
3. Cloud 下发 task/interest revision 后，Runner 调整采样项；无 interest 时 park，不做固定 1 秒全量轮询。
4. stop requested、turn loop terminal、taskRunId replaced、window unregistered 或 HWND generation changed 时：
   先 fence 新采样和新 local-kanda 输入，再取消 in-flight、等待 Runner 有界终止、清除 ephemeral slots。
5. 旧 Runner 的迟到响应、迟到模板命中和迟到 input completion 都不得发布到新 taskRun。
6. 多窗口各有 Runner/sequence/state，但所有真实物理输入仍由既有全局输入 worker 串行。

## 6. 修罗 `local-kanda` 精确状态机

### 6.1 注册与采样

- 仅当 Cloud 修罗 phase 注册 `XIULUO_ENTER_BATTLE` interest，且存在当前 green-chain
  `attemptId` 时启用。
- 保留 Git `59b85e0b` 的时间锚：从首次任务追踪绿字点击的既有 anchor 计算 probe start；不得因 fallback
  重建而重新延后整个窗口。
- 使用窗口相对 ROI `(264,376)-(305,397)`，尺寸 `41x21`；模板
  `images/template/dialog/xiuluo/xiuluo_enter_battle_kanda2.png`；原图直接匹配，阈值 `0.82`。
- 不洗图、不 OCR、不走通用 dialog detection、不因单次 miss 请求 Cloud。

### 6.2 普通 miss

- 单次或连续模板 miss 只表示“本样本未命中”，Runner 继续观察。
- miss 不发布 ready event，不清 interest，不改变 phase，不重按 tracker，不消费 fallback 次数。

### 6.3 本地命中与一次性获胜

1. 命中后生成带 `windowId/hwnd/taskRunId/round/attemptId/frameFingerprint` 的本地候选。
2. consume 前在 fresh frame 上重跑同一模板校验，并确认 binding generation、taskRun、attempt 和 interest 仍一致。
3. 对该 attempt 执行 CAS/lease；只有获胜者可提交输入。Cloud fallback job 或另一轮本地命中不能再次获胜。
4. 点击坐标沿用模板命中框既有计算；move + click + delay 必须作为一个 `InputActionRequest`。
5. 输入成功后发布 `ENTER_BATTLE_CLICKED`，包含 action correlation、attempt、click point、执行结果和时间。
6. **不得把点击成功当作进入战斗**；interest、combat probe 和 watchdog 保持到真实 `IN_COMBAT`。

### 6.4 stopped-static Cloud fallback

- 仅当前 green-chain pathing 对该 attempt 首次进入真实 `ARRIVED` 或 `STOPPED_AWAY` 时，提交一次后台
  stopped-static analysis；不因每次 local miss 提交。
- stopped-static 必须复用 Git `59b85e0b` 的原始 Cloud dialog 语义路径：本地只上传当前完整 dialog snapshot/
  `DialogDetection` 所需 ROI，Cloud 调用既有 `DialogService.prepareGreenTemplateOption(...)` +
  `XiuluoDialogCatalog.enterBattleSpecs()` 判断并返回坐标或明确 `CLOUD_NO_ACTION`。
- Cloud fallback **不得**复制或调用本地 `xiuluo_enter_battle_kanda2.png` matcher；`kanda2`只属于本地快速通道。
  Cloud仓无需新增 `kanda.png`/`kanda2.png`资产，也不得新建第二套修罗 dialog 算法。
- fallback 与 local probe 并行；前台 Cloud Task继续 park，不占 command slot等待截图。
- Cloud 返回坐标时生成同 attempt 的 typed fallback candidate；消费前仍通过同一 attempt CAS。
- Cloud 明确 `CLOUD_NO_ACTION` 时才生成 `TRACKER_GREEN_RETRY`；unavailable、timeout、capture failure、unknown、
  stale response 均不等于 no-action，也不得重按 tracker。
- 实际执行成功的 tracker re-press 最多 3 次；执行失败不消耗次数。成功 re-press 创建新 attemptId，
  但复用原 probe timing anchor。

### 6.5 消费优先级与战斗确认

- 同一唤醒周期优先级固定为：当前 local-kanda > 当前 Cloud coordinate > 当前 Cloud explicit retry >
  其他既有 cleanup job。
- local-kanda 与 Cloud 同时完成时，本地候选先尝试 CAS；CAS 失败的一方只记录 superseded，不点击。
- 战斗探测确认 `IN_COMBAT` 后，本地原子停止该 attempt 的 probe、清 local ephemeral candidate/pathing interest，
  并发送 `IN_COMBAT` 关键事件；Cloud清自己的 prepared/fallback镜像并推进 phase。
- 点击后未进入战斗时沿用 CR232 的既有 watchdog/重注册语义，不新增 TTL、额外点击或新失败真值。

## 7. 其他 Observer 职责的首卡边界

- 战斗固定模板、position fast-path、timer edge 可以进入本地 Runner，但本卡实现前必须逐项对照
  `CR271_LOCAL_OBSERVER_PLACEMENT_AUDIT.md` 表格，不能一次性复制旧 `WindowTaskRunner` 整个状态机。
- OCR fallback、任务追踪分析、通用 dialog/剧情、路线学习仍由 Cloud；Runner只通过 observation plane 上传
  interest 指定的小 ROI。
- 五环和五倍不得因为修罗 local-kanda 例外获得新的本地业务点击路径。需要类似例外时另开用户批准卡。
- Cloud `CloudWholeTaskObserver` 在 observation producer 迁完后改为消费样本/推进 Cloud 状态；它不得继续经
  command plane重复拉取同一观察，也不得与本地 Runner形成两个探测 owner。

## 8. 冻结写集

### 8.1 Client 可写

- `src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnLoopFactory.java`
- `src/main/java/com/bot/dhxy/cloud/turn/HttpsTurnClient.java`（仅复用配置/鉴权基础，不把 observation 放回 turn slot）
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`（仅 exact-window ROI capture seam；不改识别算法）
- `src/main/java/com/bot/dhxy/service/DialogService.java`（仅恢复 CR232/256 local-kanda 原图 matcher/consume validator）
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`（仅复用/补齐 correlation，不建第二队列）
- `src/main/java/com/bot/dhxy/model/job/PreparedActionJob.java`
- `src/main/java/com/bot/dhxy/model/job/PreparedActionJobType.java`
- `src/main/java/com/bot/dhxy/model/job/XiuluoGreenChainSchedule.java`
- 新目录 `src/main/java/com/bot/dhxy/window/observation/`：Runner、per-window state、observation client。
- 新的 shared wire DTO 仅放在 `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/`。
- local-kanda 模板只允许复用现有文件，不得替换像素或阈值。

### 8.2 Cloud 可写

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnHttpHandler.java`（仅共享鉴权/metadata校验 seam）
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java`
- `src/main/java/com/bot/dhxy/service/dialog/CloudDialogPreparedActionState.java`
- `src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java`
- Cloud 中与 Client 对称的 `com/bot/dhxy/cloud/turn/protocol/observation/` DTO。
- 新目录 `src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/`：独立 handler、per-window inbox/ACK、analysis dispatcher。
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`：仅允许接入既有 Cloud dialog verdict、同 attempt
  typed candidate、点击结果与最多3次实际成功tracker re-press预算；不得改变其他修罗phase/顺序。

### 8.3 只读参考

- Cloud `XiuluoTaskV2`、`DialogService`、`TaskTrackerPanelService`、`BattleRadarService`可按调用编译需要最小接线；
  若需改变业务分支、方法顺序或结果解释，必须先回卡扩写集并由父级裁决。
- `D:\mavenProject\DHXY` 全树严格只读；只允许 `git show 696a12b0...` / `git show 59b85e0b...` 取证。
- TURN-42M 以后删除清理卡在 TURN-40G source review通过前不得删除上述 observer/协议依赖。

## 9. 实施顺序

1. **合同与无动作骨架：** 对称 wire DTO、严格 validator、Cloud 独立 endpoint、Client observation client；证明
   observation 请求不读取/写入 `CloudTurnExchange.currentAction`。
2. **生命周期：** `WindowTurnLoop` 在 start ACK 后启动、stop/replace 前有界关闭 per-window Runner；先实现无截图
   heartbeat/interest ACK，验证五窗口互不冲突。
3. **被动事实：** 迁移无需业务解释的 timer/combat/position samples；Cloud observer改为消费样本，不再经 command
   plane拉同一事实。每迁一项同时删除原 producer，禁止双 owner。
4. **修罗快速通道：** 恢复 exact ROI matcher、attempt schedule、consume复验、CAS、原子 input、CLICKED/IN_COMBAT事件。
5. **Cloud fallback：** stopped-static analysis、coordinate/no-action typed result、三次实际 retry budget和stale fencing。
6. **收口：** residual scan证明 Cloud observer不再生产 observation command；双仓compile、点名合同、父级逐文件终审。

不得跳过前3步直接把旧 `WindowTaskRunner` 粘回客户端；不得先删 Cloud observer 再补 observation consumer。

## 10. 强制测试合同

### 10.1 Client named contracts

- start ACK 前 Runner 零启动；ACK 后每窗口恰一 Runner；stop/restart/taskRun replace 无旧 Runner 泄漏。
- 五窗口并发 observation 不占 command action slot、不制造 observation 导致的 command `BUSY`；既有
  registry `ACTIVE_CONFLICT`由独立repair验收，不得用本测试冒充关闭。
- latest-wins、单 in-flight、关键事件 ACK 重发、stale response 拒绝、bounded backlog。
- local-kanda ROI/模板/阈值与 `59b85e0b` 一致；普通 miss 零 Cloud fallback、零 ready、零 input。
- local 与 Cloud race 只有一个 CAS winner；旧 attempt、旧 HWND generation、旧 taskRun 永不点击。
- move+click 为单队列 request；不得 nested queue；stop fence 后零输入。
- `ENTER_BATTLE_CLICKED` 不清 combat interest；只有 `IN_COMBAT` 才完成 cleanup。

### 10.2 Cloud named contracts

- observation endpoint 不调用 `CloudTurnExchange.exchange/publishAction`，不占 unresolved command slot。
- tenant/device/window/hwnd/taskRun/seq/revision 严格校验；重复事件幂等、乱序不回退。
- stopped-static 只在 current attempt terminal触发一次；普通 local miss不触发。
- 只有明确 `CLOUD_NO_ACTION` 产生 retry；timeout/unavailable/unknown 零 retry。
- 最多3次**成功执行**重按；失败执行不计数；新 attempt 保留原 timing anchor。
- local click/fallback click/IN_COMBAT 事件推动现有 Cloud phase，不复制第二修罗状态机。

### 10.3 构建与 source gates

- 运行用户授权 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 中与本卡相关的 named tests，并列出精确 case/count。
- Client 与 Cloud 分别执行 tests-enabled compile；不得以 stale jar/IDE class替代。
- 对称 wire DTO 做物理字节或生成源一致性检查。
- source scan：observation endpoint到 `CloudTurnExchange.currentAction` 零调用；Observer直接 Robot/InputProvider 零调用；
  除 local-kanda 外本地任务业务识别/点击新增为零。

## 11. Fresh runtime 验收

1. 先由独立registry repair证明5个窗口均收到匹配ACK；在此前提下，每窗口恰一独立Runner，observation不得
   制造command `BUSY`、跨窗口样本或跨窗口输入。TURN-40G不以此冒充关闭registry `ACTIVE_CONFLICT`。
2. 主任务持续导航/输入时 Observer 仍产生有序样本，日志中 observation 与 command actionId 分离。
3. 修罗有任务时，正常样本优先由 local-kanda 命中并点击；记录 ROI match、attempt、CAS winner、队列 action和
   `IN_COMBAT`，不得出现点击即伪报战斗。
4. 人为构造 local miss/导航停止时，只出现一次 stopped-static Cloud fallback；Cloud unavailable时任务继续 park +
   local probe，不自动重按 tracker。
5. stop 后无迟到截图、事件或输入；立即 restart 使用新 taskRun/attempt，旧返回全部被拒绝。

## 12. Parent review 验收表

- [ ] 职责部署改变但 `docs/业务逻辑.md` phase/顺序/重试无差异。
- [ ] observation plane 与 command plane 在代码、endpoint、queue/store上物理分离。
- [ ] 每窗口生命周期、单 in-flight、ACK、背压和 stale fencing闭合。
- [ ] CR232/253/256 local-kanda ROI/阈值/优先级/三次fallback预算逐项等价。
- [ ] local-kanda 是唯一批准的 Observer 主动输入例外，且仍走唯一输入队列。
- [ ] CLICKED 与 IN_COMBAT 两阶段事实没有合并。
- [ ] 五窗口、stop/restart、race、网络失败 contracts 全部通过。
- [ ] 双仓 compile、wire一致性、source scans通过。
- [ ] Worker在本卡physical EOF追加 whole-card delivery；父级本人给出 P0/P1/P2 和最终结论。

## 13. 禁止项

- 禁止写入、切分支、构建或运行 `D:\mavenProject\DHXY`。
- 禁止复制完整旧 `WindowTaskRunner`、修罗状态机、OCR、dialog catalog或memory到Client。
- 禁止 Observer 继续经普通 turn slot请求截图/fact，同时又启用新 observation plane。
- 禁止每秒上传整窗口、无界队列、每窗口多 in-flight或网络失败升级为业务事实。
- 禁止 local-kanda 绕过输入队列、点击后直接宣告 IN_COMBAT、或由两条 race 路径重复点击。
- 禁止 runtime/UI/live capture/input，除非到达本卡 fresh runtime gate 且用户明确执行。

<!-- TRUE_EOF: TURN-40G READY ZERO-OWNER PLAN-CONTRACT-FROZEN LOCAL-WINDOW-OBSERVATION-RUNNER XIULUO-LOCAL-KANDA CR232-CR253-CR256 OBSERVATION-PLANE COMMAND-SLOT-SEPARATION IMPLEMENTATION-NOT-STARTED -->

## CANONICAL CLAIM - 2026-07-21T11:03:52-04:00 - EXTERNAL-A

- claimant: `EXTERNAL-A` (heartbeat `dea947fe`), acting on the user's direct instruction ("你去完成一下", TURN-40G frozen plan).
- pre_check: full 268-line card read; physical EOF at claim time = `TRUE_EOF: TURN-40G READY ZERO-OWNER PLAN-CONTRACT-FROZEN ... IMPLEMENTATION-NOT-STARTED`; card mtime `2026-07-21 11:01:10`; zero prior claim block present.
- claim: EXTERNAL-A claims sole ownership of the whole card `TURN-40G` (implementation not yet started) under the frozen plan contract. If any physically earlier claim exists above this block, this claim self-withdraws canonically.
- scope acknowledged: frozen write set §8 only (Client + Cloud + new observation packages + symmetric DTO); §9 implementation order 1->6 respected (contract/no-action skeleton first; no wholesale WindowTaskRunner paste-back); registry single-slot P1 is OUT of scope (separate repair; will not touch CloudTurnTaskRegistry task ownership); TURN-42M+ deletion stays blocked until this card's source review.
- discipline: zero Git mutation; no runtime/UI/live capture/input (fresh runtime gate is user-executed); `D:\mavenProject\DHXY` read-only (only `git show 696a12b0/59b85e0b` evidence); Maven only when sole writer, named tests + compile per §10.3.
- next: recon evidence pass (`git show 59b85e0b` CR232/253/256 local-kanda + current WindowTurnLoop/CloudWholeTaskObserver reality), then §9 step 1 (symmetric wire DTO + validators + Cloud endpoint + client, no-action skeleton).

<!-- TRUE_EOF: TURN-40G CANONICAL-CLAIM EXTERNAL-A SOLE-OWNER-PENDING-READBACK PLAN-CONTRACT-FROZEN IMPLEMENTATION-STARTING HEARTBEAT-dea947fe 2026-07-21T11:03:52-04:00 -->

## EXTERNAL-A CANONICAL SOURCE+TEST DELIVERY (STEPS 1-4 + CLOSURE SCANS) + STEP-5 CONTRACT QUESTION - 2026-07-21T12:19:00-04:00

- author: `EXTERNAL-A` (sole owner, claim line 270). state: `STEPS 1-4 DELIVERED / STEP 5 CONTRACT-BLOCKED / AWAITING_PARENT_REVIEW`.
- baseline attestation: business `696a12b0`; kanda evidence `git show 59b85e0b` only; `D:\mavenProject\DHXY` untouched read-only; zero Git mutation; no runtime/UI/live capture/input executed by the worker (fresh runtime gate remains user-executed).

### Step 1 - contract & no-action skeleton (DONE)
- Symmetric wire DTOs x10 in `com/bot/dhxy/cloud/turn/protocol/observation/` BYTE-IDENTICAL both repos (SHA verified 10/10):
  Request `7CAB0F13`/53L, Response `936A5451`/25L, Fact `F9B38FB6`+FactType `CFBEB56F`(COMBAT_SIGNAL/POSITION_SAMPLE/TIMER_EDGE), KeyEvent `0C6421CD`+Type `6C4542EB`(IN_COMBAT/COMBAT_EXITED/PATHING_TERMINAL/PRE_BATTLE_TIMEOUT/ENTER_BATTLE_CLICKED), Roi `92F2C2EC`, Interest `18DD690F`(+optional ROI geometry), AnalysisResult `4A02E53D`, Validator `588951C9`/211L (bounds: ROI<=512px/256KB, 64/32/8 caps, unique ids, ack-subset-of-carried, interest-ROI all-or-none).
- Cloud endpoint `/api/v1/client/observation`: `cloudbrain/observation/` CloudObservationHttpHandler `97DEB38A`/238L (POST-only exact-path single-Bearer strict-bounded-JSON, parse->400, mirrors turn ingress idioms), CloudWindowObservationInbox `6C67F8F7`/288L (per-window PER-TASKRUN: monotonic seq no-regress, idempotent event ACK, exactly-once drainKeyEvents, latest-wins facts+ROIs by seq, bounded runs4/ids256/events128), CloudObservationWiring `73938959` (DI bridge); registered in CloudBrainServer `04734EE2`/212L.
- Client transport `window/observation/`: ObservationClient `88243A11`, HttpsObservationClient `D5ADB376`/291L (single-attempt HTTP/2 strict bounded), ObservationTransportException `7D9ECFD3`; HttpsTurnClient `971C8C19`/537L +`newObservationClient()` config/auth reuse seam (sanctioned).

### Step 2 - lifecycle (DONE)
- WindowObservationRunner `39A91865`/354L: daemon per acknowledged window, monotonic observerSeq, single in-flight by construction, key-event retention/resend-until-ACK bounded 64, parked-heartbeat/interest-period pacing, transport failure never a business fact. Factory/Wiring/Spring wiring `0D656DB7`/`1C94F7CD`/`414A55D1` (tenant from CloudTurnSidecarProperties; registers only for HTTPS transport).
- WindowTurnLoop `C77D360A`/623L: runner starts ONLY at/after accepted matching taskStartAck (maybeStartObservationRunner at ack point; restart-safe; identity = windowId+live hwnd+startRequestId-as-runId; runner failure never kills the loop); bounded interrupt-safe close FIRST in runLoop finally. TurnLoopFactory `A4C64AED`/61L threads the factory (2-arg ctor lazily resolves wiring bridge).

### Step 3 - passive facts migrated: timer/combat/position (DONE; per-item old producer deleted)
- TIMER: local sampler duty `prebattle-timer` evaluates the IDENTICAL predicate (startedAt>0 && now-startedAt>=300_000) over the IDENTICAL one-shot CAS `WindowRuntimeContext.markOrdinaryPreBattleTimeoutPublished` -> PRE_BATTLE_TIMEOUT key event; observer consumes drained edges -> same WindowReadyEvent publish. Observer's once-per-second `markPreBattleTimeout` command pull DELETED.
- COMBAT: runner uploads the radar's EXACT ROI set (single source of truth `BattleRadarService.OBSERVATION_ROIS`: combat-flag/combat-selection/combat-top/coordinate-strip); observer drives the UNCHANGED radar (all 4 stages/thresholds/policies/2-miss exit hysteresis byte-identical) over inbox frames via the sanctioned minimal frame-source seam (`RoiFrameSource` per-call param; absent/stale frame = UNAVAILABLE fail-closed keeps IN_COMBAT); `probeWindowCombatStateReadOnly` command pull DELETED. BattleRadarService `2ED8A114`/1640L (seam is wiring-only per card §8.3 "最小接线").
- POSITION: runner uploads the coordinate strip; observer runs the UNCHANGED Cloud recognizer `PlayerStateLocationRecognizer.recognize` (template-first/OCR-fallback/canonicalize/plausibility) + the same `getMe()` position-memory update; `syncMyPosition` command pull DELETED (staleness bound 5s fails closed like an unavailable capture). PositionProbe seam now (context, observationRunId).
- Identity: CloudTurnTaskRuntime `43A4CEC6`/646L threads startRequestId (the client observation runId) into observer.start; interests published per run at observer start. CloudWholeTaskObserver `C9CC9332`/1027L.

### Step 4 - xiuluo local-kanda fast path restored per 59b85e0b (DONE)
- DialogService (client, CREATE `3DD06001`/100L): ONLY the restored raw matcher + consume revalidation — ROI (264,376)-(305,397) 41x21, existing template `xiuluo_enter_battle_kanda2.png` (unreplaced, asset present), threshold 0.82, raw capture + raw template + ImageFinder.find (TM_CCOEFF_NORMED center) -> click point = ROI origin + match center; revalidation = same matcher on fresh frame; miss => abort (never click stale).
- Sampler kanda duty (WindowObservationSampler `A7BD60D5`/282L): gate = window's own registered xiuluo dialog interest (XIULUO_V2 + XIULUO_ENTER_BATTLE + localTemplateProbeOnly + CR253 probeStartAtMs anchor reached) AND open green-chain attempt AND live hwnd==schedule.hwnd; ordinary miss = NOTHING (no Cloud request/no event/no interest change/no input); hit -> fresh-frame revalidate + binding-epoch/attempt/interest consistency recheck -> attempt-scoped ONE-SHOT CAS (`WindowRuntimeContext.tryClaimXiuluoEnterBattleClick`, new field, re-armed on attempt replace, released on unexecuted click, cleared with schedule) -> ONE atomic move+sleep+click request via the single existing InputActionQueue (`InputSequences.moveAndClickLeft(desc,x,y,80,150)`) -> ENTER_BATTLE_CLICKED key event (attemptId/round/click point/executed). Click NEVER clears interest/schedule/probe — only real IN_COMBAT does (combat edge -> existing cleanupCombatEntry path). WindowRuntimeContext `3E83998C`/2602L.
- Cloud: observer consumes ENTER_BATTLE_CLICKED -> clears its own prepared mirror for the window (a Cloud candidate can never re-click the attempt) per §6.5; phase progression stays with the real IN_COMBAT combat edge.

### Named tests (all PASS)
- CLIENT (isolate-run, junit-platform-console-standalone vs target/classes; Maven aggregate testCompile remains BLOCKED by pre-existing out-of-card dirty tests — documented technique): `WindowObservationRunnerContractTest` 5T `89FD2454`, `WindowTurnLoopObservationContractTest` 5T `39669991` (incl. FIVE-window isolation: one runner per acked window, run identities never cross, stop no-leak, restart fresh-runner), `WindowObservationKandaContractTest` 6T `1D548343` (gates/miss-zero/CAS-single-winner/revalidate-abort/unexecuted-release/attempt-rearm/click-clears-nothing), `DialogServiceKandaConstantsContractTest` 2T `36DC36AE` (constants byte-equivalent + asset reuse) = **[18/18]**. Client main compile exit0.
- CLOUD (full Maven, tests-enabled): `CloudObservationContractTest` 4T `FC991B83` (idempotent/latest-wins/no-regress/exactly-once drain/per-run isolation/endpoint fencing 404-405-401-400-200/ACK+interest round-trip), `BattleRadarTurnContractTest` 8T `D590F041` (4-stage semantics PRESERVED through the seam), `CloudWholeTaskObserverProductionHarnessTest` 5T `C53A1A22` = **[17/17]**. Cloud tests-enabled compile + test-compile exit0. (Two cloud test files received mechanical seam adaptations only: method-ref -> lambda; probe seam arity.)

### Closure scans (step 6, run now)
- Observation code (client+cloud+DTOs) -> CloudTurnExchange/currentAction/CloudTurnCommandPort/publishAction/turn path: **0 matches**.
- Observer -> direct Robot/InputProvider: **0 matches**.
- Deleted producers (markPreBattleTimeout/syncMyPosition/probeWindowCombatStateReadOnly) in observer: **0 executable references** (1 javadoc mention).
- Client new local business click paths: **exactly one** (the approved kanda `moveAndClickLeft`, sampler L190).
- DTO byte-identity: **10/10** SHA-identical across repos.
- HONEST RESIDUAL (beyond the three §9.3-named kinds; still command-plane in the observer): dialog snapshot/type detection (probeAttention, dialogTypeProbe), route/task dialog prepare* (5 sites), tracker preparation, pathing fence read (`readPathing`), WUBEI gate read+open (`readPreBattleFact`/`openTargetMapGateAndUpdateDialogInterest`), transfer/route settle consumes, combat-entry cleanup op. Per audit rows 12-20 these carry business interpretation/actions whose migration was NOT in step 3's enumerated scope; §9.6's literal "observer no longer produces observation command" therefore cannot be fully closed inside this card without additional migration far beyond the three named kinds — REQUEST the parent adjudicate: (a) accept the three-kind migration as this card's step-3/6 scope with the residual list recorded for a successor card, or (b) direct additional migrations (each will need its own evidence pass).

### STEP-5 CONTRACT QUESTION (stopped-static Cloud fallback — blocked by the frozen write set)
- §6.4 requires: attempt-terminal-triggered one-shot stopped-static analysis, CLOUD_EXECUTED-coordinate -> same-attempt typed fallback candidate, explicit CLOUD_NO_ACTION -> TRACKER_GREEN_RETRY, at most 3 EXECUTED re-presses (failures free) reusing the original timing anchor.
- BLOCKERS: (1) the fallback's consumption/retry-budget/re-press mechanics live in Cloud `XiuluoTaskV2` (shortcutTrackerRetryCount/withShortcutTrackerClick/fallbackFromShortcut...), which §8.3 freezes as READ-ONLY reference — implementing §6.4 requires changing its business branches, which §8.3 says needs a write-set expansion ruling; (2) the cloud repo has NO xiuluo kanda template assets (`images/template/dialog/xiuluo/` absent), so a Cloud-side static verdict has no template to match, and adding template binaries is outside the frozen write set. REQUEST one of: (a) widen the cloud write set to `XiuluoTaskV2` + authorize copying the two kanda template assets from the frozen baseline into cloud resources; (b) authorize a client-side stopped-static equivalent (the client owns the template + matcher) with a Cloud-verdict round-trip over the observation plane; or (c) defer §6.4 to a successor card with the local-kanda path (already restored and watchdog-guarded by existing CR232 semantics) as the sole fast path for now.

- discipline: zero Git mutation; write set honored (BattleRadarService touched under the §8.3 minimal-wiring allowance; two cloud test files mechanically adapted; no other out-of-set file modified; GameClientTracker/InputSequences/InputActionQueue/PreparedActionJob(+Type)/XiuluoGreenChainSchedule/CloudTurnHttpHandler/ready-event/prepared/pathing states needed NO byte changes); TURN-42M+ deletions untouched; registry single-slot P1 untouched. Owner RETAINED; holding for parent review + step-5 ruling.

<!-- TRUE_EOF: TURN-40G EXTERNAL-A STEPS-1-4-DELIVERED CLOSURE-SCANS-CLEAN STEP5-CONTRACT-BLOCKED(XIULUOTASKV2-READONLY+NO-CLOUD-KANDA-ASSETS) RESIDUAL-OBSERVER-COMMAND-LIST-RECORDED CLIENT-18OF18-ISOLATE CLOUD-17OF17-MAVEN DTO-10OF10-IDENTICAL COMPILES-EXIT0-BOTH ZERO-COMMAND-SLOT-CONTACT OWNER-A-RETAINED AWAITING-PARENT-REVIEW 2026-07-21T12:19:00-04:00 -->

## PARENT MESSAGE - 2026-07-21T13:00:56-04:00 - TO EXTERNAL-A - STEP 5 CONTRACT CORRECTION

- `ack_parent_message=2026-07-21T13:00:56-04:00` required on the next STATUS EVENT.
- User ruling: local fast path alone uses `xiuluo_enter_battle_kanda2.png`. The stopped-static Cloud fallback must use
  the original full-dialog semantic path from Git `59b85e0b`: capture/upload the dialog snapshot through the observation
  plane, then reuse current Cloud `DialogService.prepareGreenTemplateOption(...)` with
  `XiuluoDialogCatalog.enterBattleSpecs()` to produce coordinate or explicit `CLOUD_NO_ACTION`.
- The proposed Cloud kanda asset copy is rejected. Add neither `kanda.png` nor `kanda2.png` to Cloud and do not copy the
  local raw matcher into Cloud. The earlier asset blocker is therefore invalid and closed by this contract correction.
- Write-set expansion is approved only for Cloud `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`, plus the
  already-authorized observation/DialogService minimal wiring and focused tests, to consume the existing Cloud verdict,
  enforce current-attempt typed candidate/CAS, and preserve the CR232 maximum three actually-executed tracker re-presses
  with failed executions free and the original timing anchor retained. No other Xiuluo phase/order change is authorized.
- Resume Step 5, rerun affected named tests and both compiles, then re-deliver the whole card at physical EOF. Keep owner.
  The residual observer command-plane list remains a separate parent review item; do not treat it as permission to widen
  business behavior while completing this corrected Step 5.

<!-- TRUE_EOF: TURN-40G PARENT-MESSAGE TO-EXTERNAL-A STEP5-CORRECTED ORIGINAL-CLOUD-DIALOG-SEMANTIC-PATH NO-CLOUD-KANDA-ASSETS XIULUOTASKV2-LIMITED-WRITESET-EXPANSION ACK-REQUIRED OWNER-RETAINED RESUME-STEP5 2026-07-21T13:00:56-04:00 -->

## EXTERNAL-A STEP-5 (CORRECTED) COMPLETE - WHOLE-CARD RE-DELIVERY - 2026-07-21T13:16:00-04:00

- author: EXTERNAL-A (sole owner). ack_parent_message: `2026-07-21T13:00:56-04:00` (correction implemented exactly as granted). state: `WHOLE CARD (STEPS 1-5 + CLOSURE) DELIVERED / AWAITING_PARENT_REVIEW`.
- STEP 5 per corrected contract:
  1. Observer publishes `xiuluo-dialog` interest for XIULUO_V2 runs (window-relative 250,312 529x208 @2s; validator MAX_ROI_DIMENSION raised 512->640 both repos byte-identical: 211L/2676466C).
  2. On a tracker-shortcut attempt's FIRST real pathing terminal (edge-triggered at the existing updateObserved transition; once-per-attempt guard via ObserverState.lastStopStaticIntentSource) the observer runs ONE stopped-static analysis over the uploaded frame through the EXISTING semantic chain `DialogService.prepareGreenTemplateOption(source, XIULUO_ENTER_BATTLE, XiuluoDialogCatalog.enterBattleSpecs(), true, null, suppliedDetection)` — supplied-frame seam, zero self-capture, NO kanda assets cloud-side, NO matcher duplicate. Missing/stale frame or missing window rect => NOTHING recorded (never a verdict).
  3. COORDINATE verdict -> prepared candidate bound windowId/hwnd (intentId null per consume fence) published through the existing `CloudDialogPreparedActionState` CAS; consumed by the UNCHANGED `consumePreparedXiuluoEnterBattle` (fingerprint consume-revalidation intact). Explicit no-match on a valid frame -> `CloudStopStaticVerdictState.markNoAction` (NEW one-shot per-window slot, 59L/5E51FD6B).
  4. XiuluoTaskV2 (granted expansion, 4647L/B31B65D4): the generic ARRIVED/STOPPED_AWAY branch no longer re-reads the tracker; it consumes the one-shot no-action verdict for the exact current intent source: verdict pending -> keep waiting (prepared publication wakes it); explicit CLOUD_NO_ACTION -> CR232 budget `MAX_CLOUD_ENTER_BATTLE_FALLBACKS=3` ACTUALLY-EXECUTED saved-green re-presses (saved shortcutTrackerClickX/Y via the same executeInputTurn move/wait120/click150 bundle; unexecuted press consumes nothing and keeps waiting; executed press re-registers movement intent/pathing/dialog interest and `incrementShortcutTrackerRetry` PRESERVES firstTrackerGreenClickAtMs — original timing anchor retained); budget exhausted -> `fallbackFromShortcut("cloud-fallback-limit-reached")`. No other xiuluo phase/order changed.
- VERIFICATION: CLOUD tests-enabled compile+test-compile exit0; named CLOUD [17/17] (observation4/radar8/harness5); CLIENT main compile exit0; named CLIENT [18/18] isolate-run (runner5/loop5/kanda6/constants2; aggregate testCompile still blocked by pre-existing out-of-card dirty — documented). Observer final: 1116L/698D0693.
- KNOWN GAP (honest, needs parent direction): the current cr271 cloud choreography NEVER opens the client-side green-chain schedule and registers the xiuluo dialog interest WITHOUT localTemplateProbeOnly/probeStartAtMs (`registerXiuluoDialogInterest` passes null,null) — so the step-4 local-kanda gate (schedule+probe-only+anchor per frozen §6.1) cannot light up under today's cloud flow. Closing it needs the dialog-interest local-op args extended (probe-only+anchor) plus a schedule-opening seam — BOTH outside even the corrected write set (op executor/protocol files). The restored local-kanda machinery is complete and contract-tested; the enablement plumbing awaits a ruling.
- discipline: zero Git mutation; no runtime/UI/capture/input; DHXY read-only; residual observer command-plane list unchanged (separate review item, no widening).

<!-- TRUE_EOF: TURN-40G EXTERNAL-A WHOLE-CARD-STEPS1-5-DELIVERED STEP5-CORRECTED-IMPLEMENTED ACK-13:00:56 CLOUD-17OF17 CLIENT-18OF18 COMPILES-EXIT0 KANDA-ENABLEMENT-PLUMBING-GAP-REPORTED OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T13:16:00-04:00 -->

## PARENT SOURCE+TEST REVIEW - 2026-07-21T13:40:28-04:00 - REPAIR REQUIRED

- verdict: `P0/P1/P2=0/1/0 / REPAIR REQUIRED / OWNER RETAINED / NOT FORMAL TEST READY`.
- P1 evidence: current Cloud `XiuluoTaskV2.registerXiuluoDialogInterest(...)` calls
  `updateDialogInterest(..., null, null, ...)`, so the real shortcut flow publishes neither `probeOnly=true` nor the
  baseline `probeStartAtMs`. The symmetric `TurnWholeTaskRuntimeArguments`/Client `toDialogInterest(...)` path carries
  `probeOnly` but no `probeStartAtMs`. Production also never opens
  `WindowRuntimeContext.updateXiuluoGreenChainSchedule(...)` for the same taskRun/round/pathing-attempt. The matcher/CAS
  tests pass only by manually installing both prerequisites; the local-kanda sampler is unreachable in a real run.
- Required repair is baseline restoration, not a business choice. Through the existing local-operation protocol, each
  initial/retry shortcut attempt must publish probe-only interest with
  `probeStartAtMs = firstTrackerGreenClickAtMs + ENTER_BATTLE_LOCAL_PROBE_DELAY_MS` and atomically open/replace the Client
  schedule with exact `windowId/hwnd/taskRunId/round/attemptId/openedAtMs`. Attempt replacement must stale-fence the old
  schedule; IN_COMBAT/stop/replacement cleanup remains unchanged.
- Add production-entry contracts that drive real Cloud `XiuluoTaskV2` initial and saved-green retry paths through the
  wire/local executor, proving exact identity/anchor propagation, anchor-before zero matcher/input, anchor-after one
  current-attempt CAS winner, and stale prior-attempt rejection. Directly seeded fixtures remain unit coverage only.
- Freeze the delivered Cloud full-dialog semantics, `CLOUD_NO_ACTION`, CR232 three actually-executed re-press budget,
  local matcher constants/assets, and every unrelated phase/order. No Cloud kanda assets, duplicate matcher, second
  protocol, or second store is authorized.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW P0-0-P1-1-P2-0 REPAIR-REQUIRED OWNER-RETAINED LOCAL-KANDA-PRODUCTION-REACHABILITY EXACT-ATTEMPT-SCHEDULE PROBE-ONLY-25S-ANCHOR PRODUCTION-ENTRY-CONTRACTS CLOUD-DIALOG-FROZEN NOT-FORMAL-TEST-READY 2026-07-21T13:40:28-04:00 -->

## EXTERNAL-A P1 REPAIR COMPLETE - WHOLE-CARD RE-DELIVERY - 2026-07-21T14:10:00-04:00

- author: EXTERNAL-A (sole owner). ack_parent_message: `2026-07-21T13:40:28-04:00` (P1 baseline restoration — implemented exactly as directed). state: `WHOLE CARD DELIVERED / AWAITING_PARENT_REVIEW`.
- P1 REPAIR (kanda production reachability, through the EXISTING local-operation protocol):
  1. `TurnWholeTaskRuntimeArguments` (186L/1F55F491) +5 trailing fields (probeStartAtMs, scheduleAttemptId/Round/TaskRunId/OpenedAtMs) with a backward-compatible 26-field ctor; `TurnProtocolValidator` (1044L/EDDFD2C1) WtField/presentFields/per-op allowlist extended for WHOLE_TASK_DIALOG_INTEREST_UPDATE — both BYTE-IDENTICAL across repos (verified 0 mismatches, observation DTOs also 0).
  2. Client `WholeTaskRuntimeLocalOperationExecutor` (440L/6442F198): toDialogInterest now carries probeStartAtMs; the interest operation ATOMICALLY opens/replaces `XiuluoGreenChainSchedule` bound to the live window identity (windowId + live hwnd resolved client-side; taskRunId/round/attemptId/openedAtMs from the wire); replacement stale-fences the previous attempt (existing updateXiuluoGreenChainSchedule drops non-matching jobs + stale prepared kanda + re-arms the one-shot click claim).
  3. Cloud `CloudWholeTaskRuntimeLocalServiceClient.updateDialogInterestWithSchedule(...)` (628L/B3191779).
  4. `XiuluoTaskV2` (4682L/3C071F92): `registerXiuluoShortcutEnterBattleInterest` (probeOnly=TRUE, probeStartAtMs = firstTrackerGreenClickAtMs + ENTER_BATTLE_LOCAL_PROBE_DELAY_MS=25_000, attemptId = the pathing intent id, run projection, openedAt=now) wired at BOTH production sites — initial green click (L1760) and saved-green retry (L1999; retry keeps the ORIGINAL anchor and REPLACES the schedule with the new attempt identity). Cloud full-dialog semantics, CLOUD_NO_ACTION, and the CR232 three-executed-re-press budget are FROZEN unchanged.
- PRODUCTION-ENTRY CONTRACTS (new, `XiuluoKandaProductionChainContractTest` 4T 276L/30070A1C): the EXACT wire payload the task now sends, executed through the REAL local-operation executor against a REAL WindowRuntimeContext + REAL sampler/CAS — (a) exact identity/anchor propagation into the installed probe-only interest and atomically opened schedule (windowId/hwnd/taskRunId/round/attemptId/openedAtMs all asserted); (b) anchor-before => ZERO matcher runs + ZERO input; (c) anchor-after => exactly ONE current-attempt CAS winner (second hit never clicks); (d) attempt replacement stale-fences the old attempt (typed job dropped, claim re-armed, original anchor retained, new attempt clicks exactly once and also never twice). Directly-seeded fixtures remain unit coverage (WindowObservationKandaContractTest).
- VERIFICATION: CLIENT main compile exit0 + named family [22/22] isolate-run (runner5/loop5/kanda6/constants2/production-chain4; aggregate testCompile blocked by pre-existing out-of-card dirty — documented). CLOUD tests-enabled compile+test-compile exit0 (after the concurrent registry-lane writer stabilized) + named [17/17] (observation4/radar8/harness5). Shared protocol byte-identity: turn 2/2 + observation 10/10.
- discipline: zero Git mutation; no runtime/UI/capture/input; DHXY read-only; concurrent registry-P1 lane's files untouched by me; residual observer command-plane list unchanged (separate review item). Owner RETAINED.

<!-- TRUE_EOF: TURN-40G EXTERNAL-A P1-REPAIR-COMPLETE WHOLE-CARD-DELIVERED ACK-13:40:28 PROBE-ONLY-25S-ANCHOR ATOMIC-SCHEDULE-OPEN-REPLACE STALE-FENCE PRODUCTION-ENTRY-CONTRACTS-4T CLIENT-22OF22 CLOUD-17OF17 PROTOCOL-BYTE-IDENTICAL COMPILES-EXIT0 OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T14:10:00-04:00 -->

## PARENT SOURCE+TEST REVIEW - 2026-07-21T14:22:00-04:00 - REPAIR REQUIRED

- verdict: `P0/P1/P2=0/2/0 / REPAIR REQUIRED / OWNER RETAINED / NOT FORMAL TEST READY`.
- P1-1 exact identity is lost in the production entry. Cloud
  `XiuluoTaskV2.registerXiuluoShortcutEnterBattleInterest(...)` converts the authoritative String
  `TaskExecutionContext.taskRunId` with `(long) context.getTaskRunId().hashCode()`, while the wire,
  `XiuluoGreenChainSchedule`, and `PreparedActionJob` retain only that `long`. Java hash is not injective: `Aa` and
  `BB` have the same hash, and appending the same queue suffix preserves the collision. Two different Cloud runs can
  therefore satisfy the same local stale fence. The card requires exact taskRun identity, not a hash projection.
- P1-2 the new optional schedule tuple is not validated atomically. `TurnProtocolValidator` merely allowlists
  `scheduleAttemptId/scheduleRound/scheduleTaskRunId/scheduleOpenedAtMs`; it does not require all four together,
  nonblank attempt, positive round, exact nonblank run id, and nonnegative opened time. Client
  `WholeTaskRuntimeLocalOperationExecutor` then silently substitutes missing values with `0/0/currentTimeMillis`,
  installing an identity that was never sent. Malformed/partial schedule payloads must fail closed before mutation.
- Required repair: carry the authoritative String taskRunId byte-identically end-to-end through both protocol copies,
  Cloud client, Client executor, schedule and prepared-job identity checks. Do not add a second projected id/store.
  Validator must enforce schedule tuple all-or-none and validate every member; executor must never default a supplied
  schedule. Add collision (`Aa`/`BB` or equivalent), partial-tuple rejection, zero-mutation, and existing production-chain
  tests. Preserve local matcher, Cloud full-dialog fallback, retry budget, timing anchor, and unrelated phase/order.
- Parent verification already passed the unaffected combined Cloud family: compile plus registry/runtime/observation/
  radar/harness named tests = `48/48`, failures/errors/skips=`0/0/0`. This does not waive the two uncovered P1s.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW P0-0-P1-2-P2-0 REPAIR-REQUIRED OWNER-RETAINED EXACT-STRING-TASKRUN-ID NO-HASH-PROJECTION SCHEDULE-TUPLE-ATOMIC-VALIDATION NO-DEFAULT-MUTATION CLOUD-COMBINED-48OF48 NOT-FORMAL-TEST-READY 2026-07-21T14:22:00-04:00 -->

## EXTERNAL-A REVIEW-ROUND-2 P1x2 REPAIR COMPLETE - WHOLE-CARD RE-DELIVERY - 2026-07-21T14:42:00-04:00

- author: EXTERNAL-A (sole owner). state: `WHOLE CARD RE-DELIVERED / AWAITING_PARENT_REVIEW`. ack: review round-2 `P0/P1/P2=0/2/0` findings both repaired.
- P1-A (identity collision, XiuluoTaskV2 hashCode): the wire now carries the EXACT String task run identity end to end — `TurnWholeTaskRuntimeArguments.scheduleTaskRunId` Long->String (186L/E8C9BCCD); `XiuluoGreenChainSchedule.taskRunId` and `PreparedActionJob.taskRunId` long->String with equals-based `sameIdentity` (46L/D778A68B, 48L/79C7C151); `WindowRuntimeContext.consumePreparedActionJobValidated` expectedTaskRunId long->String equals (2602L/E8E75527); cloud client overload param String; `XiuluoTaskV2` passes `context.getTaskRunId()` verbatim — the hashCode projection is GONE (4682L/5C57F80E). No collision is possible: different runs can never share an identity.
- P1-B (partial schedule identity): `TurnProtocolValidator` (1058L/CA4FD4A8) now enforces the four schedule identity fields ALL-OR-NONE on WHOLE_TASK_DIALOG_INTEREST_UPDATE — any one present requires nonblank scheduleAttemptId, present nonnegative scheduleRound, nonblank scheduleTaskRunId, present positive scheduleOpenedAtMs (probeStartAtMs positive-when-present added too). A partial identity is rejected on the wire.
- P1-C (silent defaults): `WholeTaskRuntimeLocalOperationExecutor` (439L/29CBE77D) fabricates NOTHING — the 0L/currentTimeMillis defaults are removed; the schedule is built only from `Objects.requireNonNull` exact wire values (the validator guarantees presence; the executor fails fast rather than install a wrong identity).
- Both repos byte-identical on all 4 shared changed files (0 mismatches; observation DTOs still 10/10).
- VERIFICATION: CLIENT main compile exit0 + named family **[22/22]** isolate-run (production-chain test now asserts the String run identity propagates verbatim into the schedule); CLOUD tests-enabled compile+test-compile exit0 + named **[17/17]**. Frozen surfaces untouched: Cloud full-dialog semantics, CLOUD_NO_ACTION, CR232 three-executed-re-press budget, matcher constants/assets, all unrelated phases.
- discipline: zero Git mutation; no runtime/UI/capture/input; DHXY read-only. Owner RETAINED.

<!-- TRUE_EOF: TURN-40G EXTERNAL-A ROUND2-P1X2-REPAIRED STRING-TASKRUN-IDENTITY-NO-COLLISION ALL-OR-NONE-SCHEDULE-VALIDATION NO-FABRICATED-DEFAULTS SHARED-FILES-IDENTICAL CLIENT-22OF22 CLOUD-17OF17 COMPILES-EXIT0 OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T14:42:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #3 - 2026-07-21T15:08:00-04:00 - REPAIR REQUIRED

- verdict: `P0/P1/P2=0/2/0 / REPAIR REQUIRED / OWNER RETAINED / NOT FORMAL TEST READY`.
- Closed from Review #2: String `taskRunId` now travels verbatim through production, wire, schedule, prepared job,
  and consume fence; the `hashCode()` projection and executor `0/now` defaults are gone.
- P1-1 atomicity is still false. `WholeTaskRuntimeLocalOperationExecutor` first calls
  `runtime.updateDialogInterest(...)` and only afterwards calls `runtime.updateXiuluoGreenChainSchedule(...)`.
  These are two independent AtomicReference writes. Concurrent `WindowObservationSampler` reads interest first and
  schedule second, so attempt replacement can expose new interest with the old schedule and permit an old-attempt local
  click before stale fencing runs. Implement one WindowRuntimeContext operation that installs/replaces the paired
  interest+schedule under one synchronization/immutable-state boundary and performs stale-job/claim cleanup in that same
  transition; ordinary interest-only updates remain unchanged.
- P1-2 the delivered validator/test contract is incomplete. `scheduleRound >= 0` accepts round zero although the model
  and parent contract define a one-based positive round. No collision-separation, partial-tuple rejection, or
  zero-mutation test was added: `XiuluoKandaProductionChainContractTest` remains four positive tests, and repository test
  search contains no `Aa/BB` (or equivalent) collision and no schedule-partial negative case. Require
  `scheduleRound > 0`; add deterministic legacy-hash collision separation, partial tuple rejection, zero state mutation,
  and an atomic replacement race contract. The focused Client test count must increase.
- Preserve the now-correct exact String identity, local matcher, Cloud full-dialog fallback, retry budget, anchor, and
  unrelated phase/order. Re-run the expanded Client family, Cloud 17 family, both compiles, and shared byte identity.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW3 P0-0-P1-2-P2-0 REPAIR-REQUIRED OWNER-RETAINED PAIRED-INTEREST-SCHEDULE-ATOMIC-TRANSITION ROUND-POSITIVE NEGATIVE-CONTRACTS-MISSING COLLISION-PARTIAL-ZERO-MUTATION TEST-COUNT-MUST-INCREASE NOT-FORMAL-TEST-READY 2026-07-21T15:08:00-04:00 -->

## EXTERNAL-A REVIEW#3 P1x2 REPAIR COMPLETE - WHOLE-CARD RE-DELIVERY - 2026-07-21T15:40:00-04:00

- author: EXTERNAL-A (sole owner). ack_parent_message: Review #3 `2026-07-21T15:08:00-04:00` (`P0/P1/P2=0/2/0`). state: `WHOLE CARD RE-DELIVERED / AWAITING_PARENT_REVIEW`.
- P1-1 (atomic paired transition) CLOSED: `WindowRuntimeContext` (2672L/135CB199) now owns ONE kanda monitor (`xiuluoKandaTransitionLock`). NEW `updateDialogInterestWithXiuluoGreenChainSchedule(interest, schedule, reason)` installs/replaces the pair in ONE synchronized transition — the stale-job discard, stale prepared-kanda discard and one-shot click-claim re-arm run INSIDE that same transition. Schedule open/replace (`updateXiuluoGreenChainSchedule`), close (`clearXiuluoGreenChainSchedule`) and the claim CAS (`tryClaim`/`release` — the open-attempt check and the CAS are now also one transition, so a replacement can never slip between them) all serialize on the same monitor. NEW atomic reader `getXiuluoKandaProbeView()` (immutable record snapshot under the same monitor); `WindowObservationSampler` (286L/6A5D2687) reads the pair through it at BOTH the gate and the post-revalidation live re-check — a reader can never observe the new interest with the previous attempt's schedule. Executor (446L/983C0C85): ONE call to the paired context operation when the wire carries a schedule; the full tuple is materialized fail-fast BEFORE any runtime mutation (a partial tuple mutates nothing); interest-only updates unchanged.
- P1-2 (validator/tests) CLOSED: `TurnProtocolValidator` (1058L/6BC12EA2, byte-identical both repos) now requires `scheduleRound > 0` (one-based; production sends completedRuns+1, verified >=1 at both sites). Client family EXPANDED 22 -> **26**: NEW `collidingHashRunIdsNeverSatisfyEachOthersFence` (deterministic `Aa`/`BB` legacy-hash collision with shared suffix, asserted colliding, exact String fence stale-fences the colliding run's job); NEW `partialScheduleTupleIsRejectedOnTheWireAndMutatesNothing` (four partial tuples rejected by the shared validator through a real TurnAction wrap + validator-bypassed executor drive fails fast with ZERO mutation: no interest, no schedule, no job); NEW `roundZeroIsRejectedOnTheWire` (round 0 and -1 rejected, one-based tuple accepted); NEW `pairedInstallIsAtomicToTheObservationReader` (1000 marker-paired replacements against a concurrent reader: zero torn observations — the race contract). Production-chain test now 7T (391L/67D7B1AA), kanda contract test 7T (312L/D53264C4).
- Preserved frozen surfaces: exact String taskRunId end-to-end, local matcher constants/assets, Cloud full-dialog fallback + CLOUD_NO_ACTION, CR232 three-executed-re-press budget, timing anchor, all unrelated phases/order.
- VERIFICATION: CLIENT main compile exit0 + expanded named family isolate-run **[26/26]** (runner5/kanda7/loop5/constants2/production-chain7); CLOUD tests-enabled compile+test-compile exit0 + named **[17/17]** (observation4/radar8/harness5); shared byte identity turn 4/4 + observation 10/10 (0 mismatches).
- discipline: zero Git mutation; no runtime/UI/capture/input; DHXY read-only. Owner RETAINED.

<!-- TRUE_EOF: TURN-40G EXTERNAL-A REVIEW3-P1X2-REPAIRED ATOMIC-PAIRED-TRANSITION SINGLE-KANDA-MONITOR ATOMIC-PROBE-VIEW ROUND-POSITIVE COLLISION-PARTIAL-ZERO-MUTATION-RACE-CONTRACTS CLIENT-26OF26 CLOUD-17OF17 SHARED-IDENTICAL COMPILES-EXIT0 OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T15:40:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #4 - 2026-07-21T17:10:43-04:00

- verdict: `P0/P1/P2=0/1/0 / REPAIR REQUIRED / OWNER RETAINED / NOT FORMAL TEST READY`.
- accepted: Review #3 的 paired interest+schedule 原子安装、`scheduleRound > 0`、partial tuple 零 mutation、
  exact String taskRun fence 与四项新增合同均正确。父级独立复跑 Client compile/test-compile + focused
  `26/26`，Cloud compile + focused `17/17`，全部通过。
- P1: local-kanda sampler 没有绑定创建它的 observation `taskRunId`。`WindowObservationSampler` 只持有
  `WindowRuntimeContext`；`sampleXiuluoLocalKanda(...)` 仅比较 schedule 的 `attemptId`，随后
  `tryClaimXiuluoEnterBattleClick(String attemptId, ...)` 也只校验当前 attempt。stop/restart 或 taskRun
  replacement 的重叠窗口里，旧 Runner 的迟到 matcher 可以读取新 run 的 paired state、赢得新 attempt 的
  CAS、产生物理点击并把事件发回旧 observation run。这违反 §5.4-5 与 §6.3 的 exact taskRun fence。
- required repair: 把 authoritative observation `taskRunId` 绑定到 sampler；matcher 前、fresh-frame 后及 claim
  的同一 kanda monitor 内校验完整 expected schedule identity。claim/release 不得只用裸 attemptId。增加
  restart-overlap 合同：旧 run sampler 面对新 run schedule 必须零 matcher/零 input/零 event；当前 run 仍恰一
  CAS winner；旧 run 的迟到 unexecuted release 不得释放新 run claim。
- registry/run joint review: Cloud 五窗口 exact-key registry/runtime repair 独立结论为 `0/0/0 PASSED`；父级复跑
  `CloudTurnTaskRegistryContractTest` + `CloudTurnTaskRuntimeContractTest` = `31/31`，不随本 P1 返修重开。

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW4 P0-0-P1-1-P2-0 REPAIR-REQUIRED OWNER-EXTERNAL-A-RETAINED OLD-RUNNER-TASKRUN-FENCE REQUIRED-RESTART-OVERLAP-CONTRACT CLIENT-26OF26-CLOUD-17OF17-PARENT-RERUN-PASS REGISTRY-RUN-31OF31-PASSED NOT-FORMAL-TEST-READY 2026-07-21T17:10:43-04:00 -->

## EXTERNAL-A TURN-40G REVIEW#4 P1 REPAIR COMPLETE - WHOLE-CARD RE-DELIVERY - 2026-07-21T19:25:00-04:00

- author: EXTERNAL-A (sole owner). ack_parent_message: `2026-07-21T17:10:43-04:00` (Review #4 P0/P1/P2=0/1/0). state: `WHOLE CARD RE-DELIVERED / AWAITING_PARENT_REVIEW`. Registry/run 31/31 not reopened.
- P1 (exact observation taskRun fence) CLOSED — the local-kanda sampler now acts ONLY on a green-chain schedule belonging to its own runner's authoritative observation run, so an old runner surviving a stop/restart overlap can never consume, click for, publish on, or release the new run's paired schedule:
  1. `WindowObservationSampler` (326L/A64C8D55) carries its runner's authoritative `taskRunId` (nonblank identity; threaded from the runner via `SpringObservationRunnerFactory` 90L/9E08847D). New `scheduleBelongsToThisRun(schedule)` = exact `taskRunId` + `windowId` match.
  2. BEFORE the matcher: the gate returns immediately (no matcher, no capture, no input, no event) when the schedule is not this run's.
  3. AFTER the fresh-frame revalidation: the live schedule is re-fenced to this run's full identity (taskRunId + windowId + same attempt) before any click.
  4. ATOMICALLY inside claim/release: `WindowRuntimeContext.tryClaimXiuluoEnterBattleClick(attemptId, expectedTaskRunId, reason)` and `releaseXiuluoEnterBattleClick(attemptId, expectedTaskRunId, reason)` (context 2684L/AE064BCA) now verify the live schedule's `taskRunId` equals the caller's run under the same kanda monitor as the paired install/replace — an old runner cannot win the CAS nor release (re-arm) the current run's held claim.
- RESTART-OVERLAP CONTRACT (new, `WindowObservationKandaContractTest` 365L/08E61892 +1 test = 8T): with the new run's paired interest+schedule installed for `run-2`/`attempt-2`, an old runner bound to the retired `run-1` produces ZERO matcher runs / ZERO input / ZERO event and CANNOT release the new run's held claim; the current run (`run-2`) is then the exactly-one CAS winner and can never click the same attempt twice. Existing seeded fixtures updated for the fenced claim/release + sampler run identity (production-chain 391L/26CB0F4E).
- FROZEN preserved: matcher constants/assets, Cloud full-dialog fallback + CLOUD_NO_ACTION, CR232 three-executed re-press budget, timing anchor, exact String taskRun identity (review#3), the atomic paired-transition monitor (review#3), all unrelated phases.
- VERIFICATION: CLIENT main compile exit0 + named kanda family isolate-run **[27/27]** (runner5/kanda8/loop5/constants2/production-chain7). CLOUD unaffected by this client-only fence (observation/radar/harness [17/17] unchanged from review#3). All five changed files are client-only (window.observation / window.runtime) — no shared-protocol byte-identity impact.
- discipline: zero Git mutation; no runtime/UI/capture/input; DHXY read-only. Owner RETAINED.

<!-- TRUE_EOF: TURN-40G EXTERNAL-A REVIEW4-P1-REPAIRED EXACT-OBSERVATION-TASKRUN-FENCE BEFORE-MATCHER AFTER-FRESH-FRAME ATOMIC-CLAIM-RELEASE RESTART-OVERLAP-CONTRACT CLIENT-27OF27 CLOUD-17OF17-UNCHANGED CLIENT-ONLY-NO-SHARED-IMPACT OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T19:25:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #5 - 2026-07-21T22:52:00Z

- verdict: `P0/P1/P2=0/1/0 / REPAIR REQUIRED / OWNER RETAINED / NOT FORMAL TEST READY`.
- accepted: authoritative sampler `taskRunId` is threaded from `SpringObservationRunnerFactory`; a different run is
  fenced before the matcher; claim/release now reject a different taskRun; the restart-overlap test correctly proves
  old-run zero matcher/input/event and cross-run release rejection. Preserve these changes.
- P1: the required **full schedule identity** fence is still incomplete. `XiuluoGreenChainSchedule` identity is
  `windowId + hwnd + taskRunId + round + attemptId`. In `WindowObservationSampler`, the post-fresh-frame comparison
  checks only `taskRunId + windowId + attemptId`; it can accept a same-run replacement whose `hwnd` or `round` changed,
  then click using the stale matcher result and stale schedule. In `WindowRuntimeContext`, claim checks only
  `taskRunId + attemptId`, and release checks only `taskRunId` before its CAS. These are partial identities, contrary to
  Review #4's explicit full-identity requirement.
- required repair: define one exact schedule-identity comparison for the five identity fields (do not use
  `openedAtMs` as identity); use it after fresh-frame revalidation and atomically under `xiuluoKandaTransitionLock`
  for both claim and release. Pass the expected schedule identity, not two loose strings. Add a deterministic same-run
  replacement contract that changes `round` and/or `hwnd` while retaining colliding/reused loose identifiers and proves
  stale matcher/claim/release produce zero click/event and cannot re-arm the replacement claim.
- no Maven rerun is needed for this failed source review. Re-deliver with exact SHA and the expanded focused Client
  count; Cloud and registry/run accepted gates remain closed and need not be reopened.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW5 P0-0-P1-1-P2-0 REPAIR-REQUIRED PARTIAL-SCHEDULE-IDENTITY-FENCE FULL-FIVE-FIELD-IDENTITY-REQUIRED SAME-RUN-REPLACEMENT-CONTRACT-REQUIRED OWNER-RETAINED NOT-FORMAL-TEST-READY 2026-07-21T22:52:00Z -->

## EXTERNAL-A TURN-40G REVIEW#5 P1 REPAIR COMPLETE - WHOLE-CARD RE-DELIVERY - 2026-07-21T20:45:00-04:00

- author: EXTERNAL-A (sole owner). ack_parent_message: `2026-07-21T22:52:00Z` (Review #5 P0/P1/P2=0/1/0). state: `WHOLE CARD RE-DELIVERED / AWAITING_PARENT_REVIEW`. Accepted/frozen (unchanged): sampler taskRun plumbing, pre-matcher cross-run fence, taskRun-aware claim/release shape, run-1/run-2 restart-overlap contract. Accepted Cloud/registry/run gates NOT reopened.
- P1 (full five-field schedule identity fence) CLOSED — the fence now compares the complete identity `{windowId, hwnd, taskRunId, round, attemptId}` (`openedAtMs` is a diagnostic timestamp, NOT identity) atomically under the existing kanda monitor at all three points:
  1. NEW `XiuluoGreenChainSchedule.sameFullIdentity(other)` (61L/A4230C2F, synced BYTE-IDENTICAL to the cloud copy) compares exactly those five fields.
  2. Post-fresh-frame (`WindowObservationSampler` 329L/8C61AA10): the live schedule must `sameFullIdentity` the exact schedule the probe started on — previously it checked only taskRunId+windowId+attemptId, omitting hwnd/round.
  3. Claim (`WindowRuntimeContext.tryClaimXiuluoEnterBattleClick(XiuluoGreenChainSchedule expected, reason)` 2684L/CA8203CD): under the monitor, the live schedule must `sameFullIdentity(expected)` before the CAS — previously it omitted windowId/hwnd/round.
  4. Release (`releaseXiuluoEnterBattleClick(XiuluoGreenChainSchedule expected, reason)`): the live schedule must `sameFullIdentity(expected)` before releasing — previously it checked only taskRunId before the CAS.
  The sampler passes the exact captured schedule as the expected identity to claim/release, so a stale/replaced schedule can never win the CAS nor re-arm the replacement's claim.
- SAME-RUN REPLACEMENT CONTRACT (new, `WindowObservationKandaContractTest` 424L/6B05F63C, +1 test = 9T; family total **28/28**): `sameRunScheduleReplacementChangingRoundOrHwndFencesStaleMatcherClaimAndRelease` — within ONE run, replacing the schedule with a different round AND hwnd while the taskRunId AND attemptId collide/reuse: the stale matcher (schedule replaced mid-probe via a deterministic revalidation hook) produces ZERO click and ZERO event; the stale full identity can neither claim the replacement's click nor release/re-arm its held claim; only the replacement's exact identity claims exactly once. Existing seeded claim/release tests + restart-overlap contract updated to the expected-schedule signature.
- FINAL SHAs (all client-only except the byte-identical schedule sync): `XiuluoGreenChainSchedule.java` A4230C2F/61L (client == cloud, 0 mismatch), `WindowRuntimeContext.java` CA8203CD/2684L, `WindowObservationSampler.java` 8C61AA10/329L, `WindowObservationKandaContractTest.java` 6B05F63C/424L.
- VERIFICATION: CLIENT main compile exit0 + named kanda family isolate-run **[28/28]** (runner5/kanda9/loop5/constants2/production-chain7). CLOUD main compile exit0 + accepted named family **[17/17]** unchanged (the schedule sync is purely additive — no Cloud/registry/run gate reopened). Shared `XiuluoGreenChainSchedule` byte-identical across repos (0 mismatch).
- discipline: zero Git mutation; no runtime/UI/capture/input; DHXY read-only. Owner RETAINED.

<!-- TRUE_EOF: TURN-40G EXTERNAL-A REVIEW5-P1-REPAIRED FULL-FIVE-FIELD-IDENTITY-FENCE POST-FRAME-CLAIM-RELEASE-ATOMIC SAME-RUN-REPLACEMENT-CONTRACT CLIENT-28OF28 CLOUD-17OF17 SCHEDULE-BYTE-IDENTICAL COMPILES-EXIT0 OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T20:45:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #6 - 2026-07-21T23:33:00Z

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REVIEW PASSED / OWNER RELEASED`.
- source: `XiuluoGreenChainSchedule.sameFullIdentity` compares exactly
  `windowId + hwnd + taskRunId + round + attemptId` and excludes diagnostic `openedAtMs`. The Client and Cloud copies
  are byte-identical at A4230C2F. `WindowObservationSampler` uses this identity after fresh-frame revalidation and
  passes the exact captured schedule into claim/release; `WindowRuntimeContext` performs the same comparison and CAS
  atomically under `xiuluoKandaTransitionLock` at CA8203CD/8C61AA10.
- tests: the new same-run replacement contract deterministically changes both round and hwnd while retaining the same
  taskRunId and attemptId. It proves stale matcher zero click/event, stale claim rejection, and stale release cannot
  re-arm the live claim. The prior cross-run restart-overlap and all accepted kanda contracts remain present.
- parent verification: Client `mvn -q -DskipTests compile` exit0; named kanda family `28/28` (runner5, kanda9,
  loop5, constants2, production-chain7; 0F/0E/0S). Cloud tests-enabled compile exit0; accepted Cloud 17/17 and
  registry/run 31/31 gates were byte-unaffected and remain closed. No runtime/UI/capture/input.
- TURN-40G is closed. Together with NAV-REGRESSION-P1 Review #3, the current source+test blockers for fresh-runtime
  user testing are cleared; runtime behavior itself remains to be verified by a new user-started run.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW6 P0-0-P1-0-P2-0 SOURCE-TEST-REVIEW-PASSED FULL-FIVE-FIELD-IDENTITY CLIENT-28OF28 CLOUD-COMPILE-EXIT0 SCHEDULE-BYTE-IDENTICAL OWNER-RELEASED FRESH-RUNTIME-USER-TEST-READY 2026-07-21T23:33:00Z -->

## FRESH RUNTIME REOPEN + PARENT DIRECT REPAIR REVIEW #7 - 2026-07-21T23:46:00-04:00

- runtime evidence: run `remote-turn-22e63ba3-44bf-44ac-9c43-7e569b8af4fc` completed navigation, then remained
  `WAIT_TARGET_PATHING_TERMINAL`. Cloud continuously logged `observation frame unavailable`; therefore no terminal
  fact existed to advance the whole task to `NPC_CLICK_SMART`.
- root cause: `CloudBrainServer` used `core=4/max=32/queue=256`. With Java `ThreadPoolExecutor`, tasks are queued
  after all core threads are busy and the pool grows beyond core only when the queue fills. Four blocking turn
  requests could therefore starve the independent observation endpoint until the Client timed out.
- repair: Cloud HTTP executor is now bounded `32/32`, retains the 256 queue and rejection policy, permits 60-second
  idle core timeout, and makes no protocol/task/business change. Client `WindowObservationRunner` adds bounded first/
  periodic transport WARN, recovery INFO and stop summary so this failure cannot remain invisible.
- contracts: a real loopback HTTP Client test sends all five production-sized Xiuluo ROIs; Cloud real-handler HTTP
  coverage accepts the same payload; an executor contract blocks four long polls and proves the fifth observation
  request runs immediately.
- parent review: `P0/P1/P2=0/0/0`. Client focused family `6/6`; Cloud observation/runtime family `19/19`; both main
  source sets compiled through Maven. No runtime/UI/capture/input and no Git mutation.
- verdict: `SOURCE+TEST REPAIR PASSED / ZERO OWNER / FRESH RUNTIME REQUIRED`. Restart both JVMs. Acceptance requires
  Client observation success/recovery evidence, Cloud no persistent `observation frame unavailable`, then
  `PATHING_TERMINAL` and `NPC_CLICK_SMART` for the exact taskRun.

<!-- TRUE_EOF: TURN-40G FRESH-RUNTIME-REPAIR7 HTTP-OBSERVATION-STARVATION P0-0-P1-0-P2-0 SOURCE-TEST-PASSED CLIENT-6OF6 CLOUD-19OF19 ZERO-OWNER RESTART-BOTH FRESH-RUNTIME-REQUIRED 2026-07-21T23:46:00-04:00 -->

## FRESH RUNTIME REVIEW #8 - PAYLOAD ENVELOPE P1 - 2026-07-21T23:58:00-04:00

- exact run: `remote-turn-4d263f85-ffc2-463d-b073-b49cb46fdc14`. The executor repair is active, but Client lines
  9140-9190 show every request carrying five ROIs fails locally with `REQUEST_CONTRACT observation request JSON
  exceeds 262144 bytes`. Alternating recovery lines are empty heartbeats; Cloud therefore has no coordinate/dialog/
  combat ROI and remains at `WAIT_TARGET_PATHING_TERMINAL` until stop.
- P1 contract defect: `MAX_ROI_PNG_BYTES=256KiB` and `MAX_ROIS_PER_REQUEST=8`, but both Client transport and Cloud
  handler cap the complete Base64 JSON at the same 256KiB. The transport envelope cannot represent the protocol's
  own bounded valid payload domain.
- proposed repair: symmetrically raise only the complete HTTP JSON envelope to bounded 4MiB, retain per-ROI 256KiB
  and count 8, and add real HTTP contracts proving a valid five-ROI payload above the old limit passes while a body
  above the new limit is rejected. No business phase, recognition, ROI geometry or input order changes.
- verdict: `P0/P1/P2=0/1/0 / REPAIR PROPOSED / USER APPROVAL REQUIRED / ZERO OWNER`. No Java edit or runtime action
  occurred in this audit.

<!-- TRUE_EOF: TURN-40G FRESH-RUNTIME-REVIEW8 PAYLOAD-ENVELOPE P0-0-P1-1-P2-0 REPAIR-PROPOSED USER-APPROVAL-REQUIRED ZERO-OWNER NO-JAVA-EDIT 2026-07-21T23:58:00-04:00 -->

## PARENT DIRECT REPAIR REVIEW #9 - PAYLOAD ENVELOPE - 2026-07-22T00:00:00-04:00

- approved implementation: Client `HttpsObservationClient.MAX_JSON_BYTES` and Cloud
  `CloudObservationHttpHandler.MAX_JSON_BYTES` are symmetrically bounded at 4MiB. The protocol's existing 256KiB
  raw PNG limit per ROI and maximum eight ROIs per request are unchanged.
- positive contract: both real HTTP tests serialize a five-ROI payload deliberately larger than the retired 256KiB
  complete envelope and accept it end-to-end; Cloud confirms all five latest ROI slots are populated.
- negative contract: Cloud rejects a declared body larger than 4MiB with HTTP 413 before parsing. Strict JSON,
  bearer, exact path and protocol validation remain unchanged.
- parent review: `P0/P1/P2=0/0/0`. Client focused tests `6/6`; Cloud executor/observation/observer tests `19/19`;
  main and test sources compiled through Maven. Diff checks pass. Source SHAs: Client transport `3C9FE8A1`, Client
  HTTP test `F9FCABAE`, Cloud handler `791FA341`, Cloud contract `F77D4429`.
- verdict: `SOURCE+TEST REPAIR PASSED / ZERO OWNER / FRESH RUNTIME REQUIRED`. Restart both JVMs. Acceptance requires
  no `observation request JSON exceeds 262144 bytes`, successful five-ROI sends, Cloud coordinate frames, then the
  exact run's `PATHING_TERMINAL -> NPC_CLICK_SMART` progression.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW9 PAYLOAD-ENVELOPE-4MIB P0-0-P1-0-P2-0 SOURCE-TEST-PASSED CLIENT-6OF6 CLOUD-19OF19 ZERO-OWNER RESTART-BOTH FRESH-RUNTIME-REQUIRED 2026-07-22T00:00:00-04:00 -->

## PARENT FINAL SOURCE REVIEW #10 - EXACT CHILD RUN + REAL DIALOG ROI - 2026-07-22T10:27:00-04:00

- fresh evidence: the observation runner owns the acknowledged queue run (`remote-turn-UUID`), while the Xiuluo
  schedule owns its exact element child (`remote-turn-UUID:0:XIULUO_V2`). Exact String equality therefore rejected
  the legitimate schedule before local-kanda matching. A real `529x208` dialog ROI also exceeded the 256KiB raw-PNG
  limit before upload, although the complete 4MiB envelope was already valid.
- repair: accept only a colon-delimited child of the exact queue run plus the same exact window; a mere textual prefix
  such as `run-1` versus `run-10` remains fenced before capture/matcher/input. Raise the symmetric per-ROI PNG bound
  to `640KiB`; keep the 4MiB complete envelope, eight-ROI count, dimensions, Cloud recognition and click ordering.
- related dependency: CR212's four member windows were correctly assigned `MEMBER -> AUTO_BATTLE`, but Spring could
  not instantiate `AutoBattleTask`. Its existing production constructor is now explicitly selected, and the
  Cloud-authoritative effective queue is projected to the Client UI; this does not change TURN-40G leader semantics.
- parent review: `P0/P1/P2=0/0/0`. Client identity/observation/effective-task focused tests and compile pass. Cloud
  main compile and isolated AutoBattle/runtime contracts `64/64` pass; ordinary Cloud test-compile remains blocked
  by unrelated stale test constructors. No runtime/UI/capture/input.
- verdict: `SOURCE+TEST PASSED / ZERO OWNER / FRESH RUNTIME REQUIRED`. Restart both JVMs and verify exact-run local
  kanda or Cloud dialog click, with member windows visibly running `AUTO_BATTLE`.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW10 EXACT-COLON-CHILD-RUN ROI-640KIB CR212-AUTOBATTLE-DEPENDENCY P0-0-P1-0-P2-0 SOURCE-TEST-PASSED ZERO-OWNER RESTART-BOTH FRESH-RUNTIME-REQUIRED 2026-07-22T10:27:00-04:00 -->

## PARENT FRESH-RUNTIME REOPEN + SOURCE REVIEW #11 - PREPARED-ACTION WAKE - 2026-07-22T11:12:12-04:00

- correction: Review #10 did not close the actual enter-battle chain. Run
  `remote-turn-2117af92-7043-4c5e-9985-32bbe0c5a7dd:0:XIULUO_V2` repeatedly logged
  `operation=XIULUO_ENTER_BATTLE ... prepared=true`, but only `TASK_ATTENTION_REQUIRED` was published. The task
  awaited `PATHING_TERMINAL / PREPARED_ACTION_READY / COMBAT_STATE_CHANGED`, so it never woke or clicked.
- root cause: `CloudWholeTaskObserver.probeAttention` discarded the non-null result returned by
  `prepareParkedDialog` and relied on optional `DialogService` thread-holder publication.
- repair: Observer now checks the exact-window slot and publishes the exact-bound result when an identical action is
  not already stored. This emits `PREPARED_ACTION_READY` with `XIULUO_ENTER_BATTLE / xiuluo.enterBattle` and retains
  consume-time validation plus one-shot CAS.
- verification: `CloudWholeTaskObserverProductionHarnessTest` `7/7`, including the no-implicit-publish Xiuluo
  reproduction and exact once-consume contract; `XiuluoWholeTaskTurnContractTest` `19/19`; Cloud main compile exit 0.
  Two stale constructor fixtures were mechanically synchronized to the current `ObjectMapper` assembly contract.
  The broader old tracker class still has four unrelated fixture failures (deleted legacy title asset/local-service
  fixture), recorded but not hidden.
- parent verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REPAIR PASSED / ZERO OWNER / FRESH RUNTIME REQUIRED`. Restart Cloud;
  runtime acceptance requires typed prepared publication, one Client click and subsequent `IN_COMBAT`.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW11 PREPARED-ACTION-WAKE P0-0-P1-0-P2-0 SOURCE-TEST-PASSED OBSERVER-7OF7 XIULUO-19OF19 CLOUD-COMPILE-EXIT0 ZERO-OWNER RESTART-CLOUD FRESH-RUNTIME-REQUIRED 2026-07-22T11:12:12-04:00 -->

## PARENT FRESH-RUNTIME REPAIR REVIEW #12 - CLOUD POST-TERMINAL FRAME FENCE - 2026-07-22T11:55:00-04:00

- runtime root cause: run `remote-turn-d1acbff3-8f4b-41af-afea-0dcbf020ffdc:0:XIULUO_V2` produced its first
  pathing terminal at `11:23:25.266`. Cloud immediately analyzed the newest buffered `xiuluo-dialog` ROI, scored
  the unchanged Cloud template `0.1773`, recorded terminal `CLOUD_NO_ACTION`, and consumed the attempt. The dialog
  became actionable about 1.8 seconds later; the then-enabled Client kanda clicked at `11:23:27.200`. This proves
  the failed Cloud input was a pre-dialog frame, not a Cloud template defect.
- repair: Cloud now anchors the first terminal by exact intent source, observation sequence and Client capture time.
  A dialog ROI is eligible only when both its sequence and capture time are strictly newer; same-batch/buffered
  frames defer without recording a verdict or consuming the one-shot analysis. Client and Cloud log the exact PNG
  dimensions, byte count, sequence/capture time and SHA-256 at send/analyze boundaries. The Cloud template, wash
  chain, threshold and catalog are unchanged.
- local path policy: production `bot.xiuluo.local-kanda-enabled=false`; disabled sampling performs zero matcher,
  claim, input and event. Its dormant state gap is closed with an exact `{device,window,attemptId,round}` pending-click
  fact; only a later real `IN_COMBAT` attributes the battle to Xiuluo, so CLICKED remains distinct from IN_COMBAT.
- verification: Cloud tests-enabled compile and `CloudWholeTaskObserverPolicyContractTest` pass; Client compile and
  `WindowObservationKandaContractTest` pass, including the disabled-path zero-side-effect contract. No runtime/UI/
  capture/input was executed by the parent.
- parent verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REPAIR PASSED / ZERO OWNER / FRESH RUNTIME REQUIRED`. Restart both
  JVMs. Acceptance requires matching Client `Observation dialog ROI sending` and Cloud `stopped-static analyzing
  uploaded dialog frame` SHA-256 values, no local-kanda log/click, then Cloud coordinate click and real IN_COMBAT.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW12 CLOUD-POST-TERMINAL-FRAME-FENCE LOCAL-KANDA-DISABLED P0-0-P1-0-P2-0 SOURCE-TEST-PASSED ZERO-OWNER RESTART-BOTH FRESH-RUNTIME-REQUIRED 2026-07-22T11:55:00-04:00 -->

## PARENT P1 REPAIR REVIEW #13 - ZERO-FOCUS KEYBOARD/CAPTURE - 2026-07-22T13:42:00-04:00

- runtime evidence: at `13:05:47` (`hwnd-87158C`) and `13:07:12` (`hwnd-1EDA0BD6`) the client logged a foreground
  focus immediately before successful HWND `Alt+8`. The focus was not required by the shortcut; frozen exact-window
  action lists focused unconditionally before dispatch.
- keyboard repair: every queued Alt shortcut, Ctrl transition/chord, Enter and Unicode text now uses the request's
  immutable HWND only. Pure keyboard bundles do not focus, background delivery failure is fail-closed, and the old
  foreground-keyboard fallback is removed. `BagService`/`QuestManagerService` direct `Alt+E/Q` callback calls use the
  same exact-HWND service. `PASTE_TEXT` has zero production callers and is explicitly rejected rather than focused.
- capture repair: `GameClientTracker` no longer focuses then falls back to Robot capture; HWND failure is fail-closed
  and runtime fallback configuration is disabled. Task-tracker capture runs without initial focus and focuses only
  if its own result proves a real panel drag is required.
- residual focus boundary: physical mouse move/click/drag/scroll still acquires foreground ownership. It is not a
  screenshot or keyboard fallback and remains required by the current Windows input provider.
- verification: Client main compile exit 0; `InputActionWorkerAltFallbackContractTest` +
  `TurnInputStepExecutorContractTest` plus isolation guards pass `12/12`; production source scan finds zero direct physical keyboard call
  sites and zero screenshot-focus helper references. One unrelated architecture test still expects exactly four
  service files while the current tree also contains `DialogService.java`.
- parent verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REPAIR PASSED / FRESH RUNTIME REQUIRED`. Restart Client; acceptance
  requires follower `Alt+8`, background observations and ordinary HWND captures to produce no `event=focus`.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW13 ZERO-FOCUS-KEYBOARD-CAPTURE P0-0-P1-0-P2-0 SOURCE-TEST-PASSED CLIENT-COMPILE-EXIT0 NAMED-12OF12 FRESH-RUNTIME-REQUIRED 2026-07-22T13:44:42-04:00 -->

## PARENT SOURCE+TEST REVIEW #14 - LOCAL MAINTENANCE PATROL + BASELINE ITEMS 1/2/6 - 2026-07-22T19:20:00-04:00

- decision: passive member maintenance is local mechanical observation, not Cloud business truth. Only effective
  `AUTO_BATTLE` creates `LocalMaintenanceBroadcastRunner`; it binds the exact `WindowRuntimeContext`, runs only in
  `FREE`, samples every 3000ms, and calls no observation transport. One capture/matcher failure is logged per tick
  and the resident patrol continues. Stop/replacement shuts down both the observation and local patrol threads.
- matcher/input: `OpenCvLocalMaintenanceBroadcastHandler` restores the baseline raw-template path over ROI
  `(260,373,118,40)`, threshold `0.85`, absolute ROI-origin + match-center click, unified atomic input, and 1500ms
  successful-click dedup. Cloud `AutoBattleTask` sets `handleMaintenanceBroadcast(false)`; formal leader paths and
  leader cooldown ownership are unchanged.
- approved baseline repairs: (1) Xiuluo tracker click X is
  `minX + min(18, max(0,width/3))`; (2) `修罗古城` and `铁匠屋` exist as packaged canonical map keys and assets;
  (6) FiveRing handover sends `RUNNER_PREPARED_NOT_READY + title visible` to `SYNC_TASK_PANEL` before accept.
- review evidence: Client source SHAs `LocalMaintenanceBroadcastRunner=E144407F`,
  `OpenCvLocalMaintenanceBroadcastHandler=DEBBBD9D`, `WindowObservationRunner=C267CA4D`, factory `B1CB3B46`.
  Cloud target SHAs tracker `94E5862E`, FiveRing `B40C9BD7`, AutoBattle `C858B92B`, maps `C75C1A5C`.
- verification: Client named tests `19/19` (local runner 2, OpenCV replay 1, observation runner 5, kanda 11) and
  main compile exit0. Marked replay: `target/test-artifacts/maintenance-broadcast-replay-marked.png`. Cloud focused
  tests `36/36` (AutoBattle 33 plus tracker/map/handover 3). No runtime/UI/live capture/input; all three dirty trees
  preserved.
- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REVIEW PASSED / ZERO OWNER / FRESH RUNTIME REQUIRED`.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW14 LOCAL-MAINTENANCE-PATROL BASELINE-ITEMS-1-2-6 P0-0-P1-0-P2-0 CLIENT-19OF19-COMPILE CLOUD-36OF36 ZERO-OWNER FRESH-RUNTIME-REQUIRED 2026-07-22T19:20:00-04:00 -->

## PARENT PLAN-CONTRACT #15 - LOCAL FAST EXPECTED COMBAT EXIT - 2026-07-22T19:45:55-04:00

- state: `IMPLEMENTING / WORKER FERMAT / AWAITING WHOLE-CARD SOURCE+TEST DELIVERY`. Parent remains sole final
  reviewer. No runtime/UI/live capture/input is authorized.
- lifecycle boundary: reuse the existing per-window `WindowObservationRunner`; do not create another runner or
  thread. A small mechanical probe collaborator may run from `WindowObservationSampler` only under a Cloud-issued,
  geometry-free fast-exit interest.
- frozen baseline semantics: only an expected Xiuluo/Wubei combat with `FAST_EXPECTED_EXIT` armed may create an
  interest. The first frame after confirmed `IN_COMBAT` plus armed wait establishes the baseline and can never
  detect exit. ROI remains `20x20` around configured team hover; detection begins at `combatStartedAt+15000ms`,
  runs at 1000ms cadence and preserves `ImageFinder.isMatch` / diff threshold `0.35`. Replacement, stop and a new
  combat generation discard the old baseline. A capture/matcher miss emits no fact.
- transport/correlation boundary: zero per-second avatar PNG upload and zero command-plane avatar capture. A hit
  emits one retained typed `FAST_EXPECTED_COMBAT_EXIT` edge, correlated by exact taskRun/window plus a unique
  expected-wait identity and combat generation; timestamps alone and generic uncorrelated `COMBAT_EXITED` are
  forbidden. Shared DTOs must remain byte-identical and malformed/partial identities fail closed.
- Cloud boundary: Cloud validates and idempotently consumes the exact edge, records the existing expected-wait exit
  signal and immediately wakes the parked task. Existing `consumeCombatExitSignalForExpectedWait`, deferred leader
  recovery, return verification and Xiuluo/Wubei phases remain authoritative. The ordinary full battle radar remains
  the conservative fallback.
- false-positive boundary: trusted `IN_COMBAT` remains a Cloud business decision. It returns Xiuluo to
  `WAIT_COMBAT` or Wubei to `WAIT_BATTLE_FINISH`, replaces the expected-wait identity/interest and causes Client to
  capture a fresh current in-combat baseline. Client never changes task phase or executes recovery/input.
- acceptance contracts: first-frame no-detect, 15s/1s timing, unchanged/changed frames, one-shot delivery,
  transient miss, replacement/stop reset, stale/duplicate/wrong-run rejection, immediate Cloud wake, false-positive
  re-arm/new baseline, and proof the fast path no longer calls command-plane avatar capture.

<!-- TRUE_EOF: TURN-40G PLAN-CONTRACT15 LOCAL-FAST-EXPECTED-EXIT IMPLEMENTING OWNER-FERMAT EXISTING-RUNNER NO-NEW-THREAD EXACT-WAIT-GENERATION ZERO-PERIODIC-PNG ZERO-COMMAND-CAPTURE AWAITING-DELIVERY 2026-07-22T19:45:55-04:00 -->

## PARENT PLAN-CONTRACT #16 - OBSERVATION / TASK-TURN DECOUPLING - 2026-07-23

- state: `IMPLEMENTING IN ORDERED STAGES / PARENT FINAL REVIEW REQUIRED`. The user approved implementation after
  a full baseline and collateral-impact audit. `D:\mavenProject\DHXY` remains strictly read-only. No runtime, UI,
  live capture or input is authorized during implementation.
- root cause: the Client observation plane is already a dedicated per-window runner and the Cloud ready/pathing
  stores are thread-safe, but `CloudWholeTaskObserver` still creates `CloudTaskTurnCoordination` and wraps pathing,
  combat, pre-battle, attention and tracker probes in the same business/input turn used by the main task. Several
  probes also call `TurnGameClient` or runtime local-service reads. A parked task can therefore prevent the observer
  from publishing the fact that would wake that task.
- frozen architecture: local observation sampling and Cloud fact interpretation run independently of task turn.
  Observation may update exact-run fact stores, publish soft ready events and prepare an immutable candidate from
  already-uploaded ROIs. Observation must never grant action authority and must never issue input, command-plane
  capture, runtime local-service command or cleanup. The task re-reads the authoritative state, reacquires task turn,
  revalidates exact identity and only then consumes a candidate or executes a side effect.
- protocol stage: add a bounded typed pathing fact carrying exact `taskRunId/windowId/hwnd/intentId`, source,
  target map/coordinate/tolerance, pathing type and relevant timestamps/state. Do not use an ad-hoc detail string.
  Client and Cloud protocol bytes remain identical. Old run, old intent, non-increasing observer sequence,
  pre-intent buffered frame and replacement overlap fail closed.
- Cloud fact stage: `PATHING_TERMINAL`, `COMBAT_STATE_CHANGED`, `PRE_BATTLE_TIMEOUT`,
  `TASK_ATTENTION_REQUIRED`, `PREPARED_ACTION_READY` and tracker-positive/negative facts may be published without
  task turn only from exact observation payloads. Existing `CloudNavigationPathingState`,
  `CloudWholeTaskReadyEventState` and prepared-action CAS remain the sole stores; no second queue, lock, lease,
  event bus, observer thread or business protocol is allowed.
- deferred consequence stage: combat-entry runtime cleanup, Wubei target-map gate/dialog-interest update, pending
  route-learning settlement, supplemental command-plane capture and every click/key operation are removed from the
  observer. Where still required, they become exact-run one-shot pending consequences consumed by the owning task
  after it holds task turn. `uncertain`, stale or stopped outcomes are never fabricated as success.
- protected fast-exit contract: PLAN-CONTRACT #15 remains unchanged. The existing `WindowObservationRunner`,
  exact wait/generation fence, first-frame baseline, `15s/1s/0.35`, reliable edge and ordinary radar fallback must
  survive this refactor. No additional runner, periodic PNG upload or command-plane avatar capture may return.
- protected FiveRing contracts:
  1. `WAIT_PATHING` remains outside task turn.
  2. `BUY_SHOES` remains outside task turn while its approved compound-action inherited-turn exception remains.
  3. `ACCEPT_TASK` remains outside task turn while prepare/handover inherited-turn exceptions remain.
  4. `HANDLE_DIALOG` remains outside task turn while waiting for a prepared verdict.
  5. `SYNC_TASK_PANEL` remains outside task turn while waiting for tracker facts.
  6. ready-event priority, exact identity checks, post-wake authoritative reread and one-shot prepared consumption
     remain intact, including `RUNNER_PREPARED_NOT_READY + title visible -> SYNC_TASK_PANEL`.
  7. inherited-turn release, combat-recovery cleanup and prepared ownership remain once-only and fail closed.
- protected Wubei/Xiuluo contracts: waiting on pathing/combat/pre-battle facts does not require turn; actual input
  and cleanup still do. Wubei target-map gating is consumed after wake. Xiuluo stopped-static analysis may use the
  uploaded ROI without turn, but production local-kanda remains disabled and Cloud fallback ordering is unchanged.
- ordered implementation:
  1. Client/shared protocol and exact pathing-fact upload, with no Cloud observer edit.
  2. Cloud inbox/state acceptance and no-turn pathing/pre-battle publication.
  3. no-turn combat fact publication plus task-owned deferred cleanup.
  4. dialog/tracker ROI preparation split from supplemental capture and action.
  5. FiveRing/Wubei/Xiuluo consequence consumption adjustments.
  6. delete observer `observerTurn`, `runPathingProbe` and `runParkedProbe` only after all probes are command-free.
- acceptance gates:
  - while the main task deliberately holds task turn and waits, a newer exact observation still publishes terminal
    pathing/combat/timer facts and wakes it;
  - no mouse, keyboard, focus, command capture, local-service command or cleanup occurs before task reacquisition;
  - five-window isolation rejects cross-window/run/intent facts and follower observation produces zero input/focus;
  - all protected FiveRing, Wubei, Xiuluo and fast-exit contracts remain green;
  - source scan finds zero `CloudTaskTurnCoordination`, `TurnGameClient` and runtime local-service references from
    the final `CloudWholeTaskObserver`;
  - both repositories compile from current sources. Fresh runtime remains a separate user-run gate.
- business difference: `无已批准业务差异；按本地基线等价迁移`. Any discovered need to change phase order,
  detector semantics, timeout, retry/fallback order or input ordering is a contract blocker and must return to the
  parent before implementation continues.

<!-- TRUE_EOF: TURN-40G PLAN-CONTRACT16 OBSERVATION-TASK-TURN-DECOUPLING ORDERED-STAGES PARENT-FINAL-REVIEW BASELINE-EQUIVALENT FAST-EXIT-FROZEN FIVERING-SEVEN-CONTRACTS-FROZEN NO-RUNTIME-NO-INPUT 2026-07-23 -->

## STAGE 1 CANONICAL SOURCE+TEST DELIVERY - TYPED PATHING FACT - 2026-07-23T03:34:23-04:00

- state: `WHOLE-STAGE SOURCE+TEST DELIVERED / AWAITING PARENT REVIEW / NOT SELF-APPROVED`.
- scope delivered: Client/shared observation protocol plus exact current pathing-snapshot upload only. No
  `CloudWholeTaskObserver`, `CloudNavigationPathingState`, FiveRing/Wubei/Xiuluo task, Cloud consumer, observer turn,
  business decision, timeout/retry/fallback/input ordering, runner/thread/store/queue/lock/lease was changed.
- shared protocol, byte-identical in Client and Cloud:
  - `ObservationRequest.java` SHA `B6271BB8CA02`, mtime `03:23:35`;
  - `ObservationProtocolValidator.java` SHA `0C66F1DF9027`, mtime `03:32:38`;
  - `ObservationPathingFact.java` SHA `7B0493F7EA0C`, mtime `03:23:18`;
  - `ObservationPathingType.java` SHA `11AFAD800D77`, `ObservationPathingState.java` SHA `AEF2C5C7FC54`,
    `ObservationPathingTransition.java` SHA `9772A3C7EE9B`, mtime `03:23:18`.
- typed fact contract: bounded zero-or-one fact per request; full exact `taskRunId/windowId/hwnd/intentId`, typed
  source/target/current coordinates, tolerance, type, start/update/location/movement timestamps, state and explicit
  `CURRENT/CLEARED/REPLACED` lineage. Validator cross-checks fact identity against the request and fails closed on
  blank/partial identity, wrong run/window/hwnd/intent, malformed coordinate pairs, out-of-bound coordinate or
  tolerance, invalid time ordering, and illegal state/transition combinations. No detail string is used.
- Client:
  - `WindowObservationSampler.java` SHA `5E936DEE1253`, mtime `03:26:48`: reads only the existing exact-window
    `WindowRuntimeContext.getPathingSnapshot()` plus native binding; maps current/clear/replacement snapshots without
    capture, focus, command plane, local service or input.
  - `WindowObservationRunner.java` SHA `DD892E5ACD68`, mtime `03:27:03`: carries the sampler's exact pathing fact on
    the existing runner/request lifecycle; no second runner or thread.
  - `WindowObservationRunnerContractTest.java` SHA `9AF0A705A4BC`, mtime `03:31:20`: covers exact snapshot,
    clear/replacement lineage, no-interest upload, request identity and strict validation.
- Cloud test-only evidence:
  - `CloudObservationContractTest.java` SHA `5F3119107C1A`, mtime `03:28:50`: accepts the complete typed fact and
    rejects wrong-run, partial identity, invalid coordinate, tolerance and timestamp payloads. No Cloud consumer was
    added or changed.
- verification:
  - Client approved named observation + frozen fast-exit family:
    `WindowObservationRunnerContractTest` 7, `HttpsObservationClientRoundTripContractTest` 1,
    `WindowObservationKandaContractTest` 11, `FastExpectedCombatExitProbeTest` 4,
    `FastExpectedCombatExitNoCommandPlaneTest` 1 = `24/24`; `mvn -q -DskipTests compile` exit 0.
  - Cloud approved observation + frozen fast-exit family:
    `CloudObservationContractTest` 6, `FastExpectedExitObservationContractTest` 4,
    `FastExpectedExitGateTest` 2 = `12/12`; exact isolate-run was used because two unrelated dirty Xiuluo constructor
    fixtures block global test compilation; no fixture was modified. `mvn -q -DskipTests=false compile` exit 0.
  - Shared protocol SHA equality `6/6`; forbidden Cloud observer/state/task diff empty.
- safety/business: no Git mutation; no runtime/UI/live capture/input; `D:\mavenProject\DHXY` remained read-only;
  all pre-existing dirty/untracked work preserved. `无已批准业务差异；按本地基线等价迁移`.

<!-- TRUE_EOF: TURN-40G PLAN-CONTRACT16-STAGE1 TYPED-PATHING-FACT WHOLE-STAGE-SOURCE-TEST-DELIVERED AWAITING-PARENT-REVIEW PROTOCOL-6OF6-BYTE-IDENTICAL CLIENT-24OF24-COMPILE-EXIT0 CLOUD-12OF12-COMPILE-EXIT0 NO-BUSINESS-DIFFERENCE NO-RUNTIME-NO-INPUT 2026-07-23T03:34:23-04:00 -->

## PARENT STAGE 1 SOURCE REVIEW #17 - REPAIR REQUIRED - 2026-07-23

- verdict: `P0/P1/P2=0/1/0 / STAGE 1 REPAIR REQUIRED / OWNER FERMAT RETAINED`. Stage 2 remains closed.
- P1 evidence: `ObservationPathingFact` and `WindowObservationSampler.sampleCurrentPathingFact()` omit
  `dialogBlocking`, `dialogBlockingReason` and `dialogBlockingDetectedAtMs`, although the authoritative local
  `WindowPathingSnapshot` carries them and the existing Cloud `TurnPathingSnapshot` /
  `CloudNavigationPathingState.toWindowPathingSnapshot(...)` preserve them. Cloud `NavigationService` consumes
  those fields to distinguish a route terminal that must yield to dialog handling from one that may continue route
  recovery. Switching Stage 2 to the new typed fact as delivered would silently erase that baseline fact and can
  reopen/retry navigation while an expected dialog is already present.
- required repair: extend the shared typed fact byte-identically with the three bounded dialog-blocking fields;
  map them directly from the exact local snapshot; validate `reason` as optional bounded text and require
  `detectedAtMs == 0` when false, or a positive timestamp within the pathing interval when true. A `CLEARED` fact
  must carry false/null/zero. Add Client mapping and Cloud malformed-payload contracts. Do not add dialog
  recognition, command capture, input, cleanup, a second store, or any Stage 2 consumer in this repair.
- retained acceptance: all Stage 1 identity, clear/replacement, no-interest upload, frozen kanda/fast-exit tests,
  shared-byte equality and both compile gates must remain green. No runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW17 STAGE1 P0-0-P1-1-P2-0 REPAIR-REQUIRED DIALOG-BLOCKING-FACT OWNER-FERMAT STAGE2-CLOSED 2026-07-23 -->

## STAGE 1 REVIEW #17 CANONICAL RE-DELIVERY - DIALOG-BLOCKING FACT - 2026-07-23T03:44:45-04:00

- state: `REPAIR COMPLETE / WHOLE-STAGE SOURCE+TEST RE-DELIVERED / AWAITING PARENT REVIEW / NOT SELF-APPROVED`.
  This repair addresses only parent P1 #17; Stage 2 remains closed.
- shared byte-identical repair:
  - `ObservationPathingFact.java` adds `dialogBlocking`, optional bounded `dialogBlockingReason` and
    `dialogBlockingDetectedAtMs`; Client/Cloud SHA `71159186D866`, mtime `03:41:40`.
  - `ObservationProtocolValidator.java` enforces: false => `null/0`; true => positive detected time within
    `[pathingStartedAtMs,pathingUpdatedAtMs]`; reason remains optional but bounded; `CLEARED` => false/null/0.
    Client/Cloud SHA `33C1C5AE4154`, mtime `03:41:40`.
  - all six shared Stage 1 protocol files remain byte-identical: request `B6271BB8CA02`, validator
    `33C1C5AE4154`, fact `71159186D866`, type `11AFAD800D77`, state `AEF2C5C7FC54`, transition
    `9772A3C7EE9B`.
- Client mapping/test:
  - `WindowObservationSampler.java` SHA `09DD1B53278B`, mtime `03:41:49`: current facts map the three fields
    directly from the exact `WindowPathingSnapshot`; non-blocking normalization is false/null/0; clear facts are
    always false/null/0. No recognition, capture, service, focus or input was added.
  - `WindowObservationRunnerContractTest.java` SHA `58CE88BF31A9`, mtime `03:41:59`: verifies exact true mapping
    and cleared false/null/0.
- Cloud malformed contracts:
  - `CloudObservationContractTest.java` SHA `487D2919B56D`, mtime `03:43:59`: verifies valid optional-null reason
    and rejects true+zero time, true+out-of-interval time, over-bound reason, false+residual reason/time and
    `CLEARED`+blocking payloads. No Cloud production consumer was touched.
- exact retained Stage 1 verification:
  - Client command:
    `mvn -q "-Dtest=WindowObservationRunnerContractTest,HttpsObservationClientRoundTripContractTest,WindowObservationKandaContractTest,FastExpectedCombatExitProbeTest,FastExpectedCombatExitNoCommandPlaneTest" test`
    => runner 7 + HTTPS 1 + kanda 11 + fast probe 4 + no-command 1 = `24/24`.
  - Cloud exact isolate-run named family => `CloudObservationContractTest` 6 +
    `FastExpectedExitObservationContractTest` 4 + `FastExpectedExitGateTest` 2 = `12/12`. The unchanged unrelated
    dirty Xiuluo constructor fixtures still block global test compilation and were not modified.
  - Client `mvn -q -DskipTests compile` exit 0; Cloud `mvn -q -DskipTests=false compile` exit 0.
  - shared protocol SHA equality `6/6`; forbidden Cloud observer/state/FiveRing/Wubei/Xiuluo diff empty.
- safety/business: no Git mutation; no runtime/UI/live capture/input; `D:\mavenProject\DHXY` stayed read-only;
  existing dirty/untracked work preserved. `无已批准业务差异；按本地基线等价迁移`.

<!-- TRUE_EOF: TURN-40G STAGE1 REVIEW17-REPAIR DIALOG-BLOCKING-FACT WHOLE-STAGE-SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW NOT-SELF-APPROVED PROTOCOL-6OF6-BYTE-IDENTICAL CLIENT-24OF24-COMPILE-EXIT0 CLOUD-12OF12-COMPILE-EXIT0 NO-BUSINESS-DIFFERENCE 2026-07-23T03:44:45-04:00 -->

## PARENT STAGE 1 FINAL SOURCE REVIEW #18 - PASSED - 2026-07-23

- verdict: `P0/P1/P2=0/0/0 / STAGE 1 SOURCE+TEST REVIEW PASSED`. Review #17 is closed; Stage 1 owner is
  released and Stage 2 may begin under the contract below.
- source review: all six shared protocol files are byte-identical. The typed fact now preserves the existing
  `TurnPathingSnapshot` payload including dialog-blocking state; Client current/clear/replacement mapping is
  exact-window and command-free; false and clear normalize to `false/null/0`. Validator rejects partial identity,
  malformed coordinate/time/transition combinations and every residual dialog-blocking combination required by
  Review #17. No Cloud production consumer, Observer or task was changed.
- verification accepted: Client focused family `24/24` plus compile exit 0; Cloud focused family `12/12` plus
  compile exit 0; protocol SHA equality `6/6`. Parent independently rechecked the shared SHA set and repair write
  boundary. No runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW18 STAGE1 P0-0-P1-0-P2-0 SOURCE-TEST-PASSED OWNER-RELEASED STAGE2-OPEN 2026-07-23 -->

## PARENT STAGE 2 IMPLEMENTATION CONTRACT - CLOUD PATHING/PREBATTLE NO-TURN - 2026-07-23

- state: `STAGE 2 IMPLEMENTING / OWNER FERMAT / PARENT FINAL REVIEW REQUIRED`. Stages 3-6 remain closed.
- inbox boundary: extend the existing per-run `CloudWindowObservationInbox.RunState`; do not add a second store,
  queue, thread, listener, lock or lease. Retain at most one `SequencedPathingFact(observerSeq,capturedAtMs,fact)`.
  Lower/non-increasing sequence cannot replace a newer fact. Retrieval is exact
  `(tenantId,deviceId,windowId,observationRunId)`.
- identity/lineage boundary: the observation fact owns the acknowledged queue run while the Cloud task context owns
  its exact colon-delimited child. Reject a mere textual prefix, wrong window/hwnd, stale run, stale sequence,
  `CURRENT` changing an established intent without replacement lineage, a `REPLACED` fact whose
  `replacedIntentId` is not the current intent, and a `CLEARED` fact that does not name the current intent.
- state boundary: add an observation-fact acceptance/CAS seam to the existing
  `CloudNavigationPathingState`; no second pathing mirror. Observer classification must read that mirror without
  consulting `TurnGameClient` or a runtime local service. The first coordinate ROI at or before the new intent's
  fact sequence is only the fence; only a strictly newer exact-run ROI may classify movement/arrival/stopped-away.
  Existing map canonicalization, arrival rule, stopped-away thresholds and once-only terminal publication remain
  unchanged.
- no-turn boundary: typed pathing acceptance, coordinate classification, `PATHING_TERMINAL`, retained
  `PRE_BATTLE_TIMEOUT` edge consumption and its ready publication execute without
  `CloudTaskTurnCoordination`. They may update only existing Cloud fact/ready stores. They may not capture through
  the command plane, call a runtime local service, prepare/consume an action, execute cleanup/input, open the Wubei
  target-map gate, settle route-learning memory or run Xiuluo stopped-static consequences in this stage.
- transition boundary: while a typed pathing fact exists for the exact run, the old command-read pathing probe must
  not classify or publish the same intent. A temporarily retained legacy fallback may run only when no typed fact
  has ever been accepted for that exact run; it is deleted in Stage 6. Stages 3-5 will move the listed deferred
  consequences before fresh runtime is allowed.
- required tests: inbox latest-wins/run isolation; exact child versus prefix collision; stale/current/replaced/clear
  CAS; pre-intent/same-sequence ROI fence; task turn deliberately held while a newer pathing terminal and
  pre-battle timeout still publish/wake; no command capture/local service/input/cleanup on that path; unchanged
  FiveRing/Wubei/Xiuluo/fast-exit focused contracts and Cloud compile.

<!-- TRUE_EOF: TURN-40G STAGE2-CONTRACT CLOUD-PATHING-PREBATTLE-NO-TURN OWNER-FERMAT STAGES3-6-CLOSED EXACT-LINEAGE NO-SECOND-STORE NO-COMMAND-SIDE-EFFECT PARENT-REVIEW-REQUIRED 2026-07-23 -->

## STAGE 2 CANONICAL WHOLE-STAGE SOURCE+TEST DELIVERY - 2026-07-23T04:03:43.798-04:00

- state: `STAGE 2 WHOLE-STAGE DELIVERED / AWAITING PARENT FINAL REVIEW / OWNER FERMAT`.
  `P0/P1/P2 = NOT SELF-APPROVED`; Stages 3-6 remain closed.
- scope: Cloud source only plus this card EOF. `D:\mavenProject\DHXY` remained read-only; Client/shared DTOs and
  FiveRing/Wubei/Xiuluo task sources were not edited; no Git mutation and no runtime/UI/capture/input was run.
- implementation:
  - existing `CloudWindowObservationInbox.RunState` now retains at most one typed
    `SequencedPathingFact(observerSeq,capturedAtMs,fact)`, exact-run latest-wins and an ever-accepted fence.
    Dynamic lineage is validated transactionally before any run mutation: a rejected request cannot advance the
    accepted sequence, overwrite ordinary facts/ROIs/pathing, enqueue events, or invoke the key-event listener.
  - CURRENT/REPLACED/CLEARED lineage is strict. A cleared intent identity is retained, so the canonical
    `CURRENT(A) -> CLEARED(A) -> REPLACED(B,replacedIntentId=A)` sequence succeeds while any other predecessor is
    rejected.
  - existing `CloudNavigationPathingState` remains the only pathing mirror. Its CAS now accepts the exact queue-run
    observation fact only for a non-empty colon-delimited task child, rejects textual-prefix collisions,
    wrong window/hwnd, stale sequence and invalid transition lineage, and preserves the new-intent ROI sequence
    fence across later CURRENT refreshes.
  - `CloudWholeTaskObserver` processes retained `PRE_BATTLE_TIMEOUT` before fallible typed pathing work and isolates
    typed-pathing failures. Typed pathing reads only the inbox/mirror plus a strictly newer exact-run coordinate ROI,
    classifies with the existing arrival/stopped-away policy, and publishes `PATHING_TERMINAL` directly to the
    existing ready store without `CloudTaskTurnCoordination`. Once an exact run has accepted typed pathing, the
    legacy command-read classifier is permanently suppressed for that run.
  - the new no-turn path performs no command capture/runtime local-service call, action prepare/consume,
    cleanup/input, Wubei target-map gate, route-memory settlement, or Xiuluo stopped-static consequence.
- exact files:
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudWindowObservationInbox.java`
    SHA256 `C171BA5F76BC19C6904C6F6708021086F7A3D418E6A14C50C77E01C3BAF5FDF3`,
    mtime `2026-07-23T03:57:40.291-04:00`, 432 lines.
  - `src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java`
    SHA256 `847EC1939ACF3CCC504629620DA8518B4932E586617CD01044437526E14824AD`,
    mtime `2026-07-23T03:58:08.278-04:00`, 382 lines.
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
    SHA256 `75D0AAD245A91ECB1E144916478838FD2223B74166A9A0BFF9D53F03FAACD5BC`,
    mtime `2026-07-23T03:58:00.224-04:00`, 1440 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationContractTest.java`
    SHA256 `414D9EBBC2385945AD1248BA76B0A026EFD82759133DBB33B619B23FAF27EB92`,
    mtime `2026-07-23T03:58:50.265-04:00`, 706 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverProductionHarnessTest.java`
    SHA256 `9570D01FF773800074A7854E681FC395893C7AE74675A22F8AD267E1C014C578`,
    mtime `2026-07-23T03:59:33.178-04:00`, 810 lines.
- verification:
  - `mvn -q -DskipTests=false compile` => exit 0.
  - isolated `javac -encoding UTF-8 -proc:none` for the four approved observation/observer families => exit 0.
  - exact isolate-run:
    `CloudObservationContractTest` 8/8,
    `FastExpectedExitObservationContractTest` 4/4,
    `CloudWholeTaskObserverPolicyContractTest` 10/10,
    `CloudWholeTaskObserverProductionHarnessTest` 11/11 => 33/33.
  - `BattleRadarTurnContractTest` => 8/8. Stage-2 observation/observer/radar aggregate => 41/41.
  - source scan of the typed no-turn slice: zero references to `CloudTaskTurnCoordination`,
    `CloudWholeTaskRuntimeLocalServiceClient`, `readPathing`, `readPreBattleFact`, target-map gate,
    settlement, stopped-static, capture, input or combat cleanup; Stage-2 symbols have zero task-source hits.
- preserved external test blockers (not modified because outside the frozen write set):
  - aggregate Maven `testCompile` is still blocked by stale Xiuluo constructor fixtures in
    `CloudTurnTaskRuntimeContractTest.java:1018` and `CloudTurnTaskFactoryAllowlistTest.java:186`.
  - the broader dirty task-family run was executed and exposed pre-existing fixture/source drift:
    FiveRing whole 18T (2F/3E), tracker 7T (3F/4E), cleanup 7T (2F/1E);
    Wubei whole 15T (0F/2E), tracker 4T (1F/1E);
    Xiuluo whole 19T (1F/2E), tracker 5T (3F/1E).
    Representative evidence is old input-source marker expectations, unavailable retired local-service stubs,
    and strict result JSON fixtures missing newer creator fields. None points into the five Stage-2 changed files;
    no out-of-card fixture or task source was changed to manufacture a green result.

<!-- TRUE_EOF: TURN-40G STAGE2-WHOLE-STAGE-DELIVERED OWNER-FERMAT AWAITING-PARENT-FINAL-REVIEW P0-P1-P2-NOT-SELF-APPROVED STAGES3-6-CLOSED 2026-07-23T04:03:43.798-04:00 -->

## STAGE 2 PRE-FINAL TERMINAL-EDGE REPAIR RE-DELIVERY - 2026-07-23T04:07:11.843-04:00

- state remains `STAGE 2 WHOLE-STAGE DELIVERED / AWAITING PARENT FINAL REVIEW / OWNER FERMAT`;
  `P0/P1/P2 = NOT SELF-APPROVED`.
- repaired edge: a typed fact may already install `ARRIVED`/`STOPPED_AWAY` in the existing mirror. Terminal
  publication no longer depends on `nextState != current.state`; `ObserverState` now grants one terminal
  publication per exact typed intent only after a strictly newer ROI has been accepted and classified.
  The fact-sequence ROI remains a fence and publishes nothing; replay and later confirming ROIs cannot duplicate
  the wake. Classification, arrival and stopped-away semantics are unchanged.
- superseding files:
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
    SHA256 `E9BC36755D552DDDA432CE3F16E96FE90666253C2202BFA001282C577AA6C400`,
    mtime `2026-07-23T04:05:43.444-04:00`, 1449 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverProductionHarnessTest.java`
    SHA256 `72E94578CD5045A4BE7D7F610CDB57A19F12FFC82D6F0A13F43C2A9384F87C5F`,
    mtime `2026-07-23T04:06:10.202-04:00`, 872 lines.
- verification:
  - Cloud `mvn -q -DskipTests=false compile` => exit 0.
  - exact isolate-run:
    `CloudObservationContractTest` 8/8,
    `FastExpectedExitObservationContractTest` 4/4,
    `CloudWholeTaskObserverPolicyContractTest` 10/10,
    `CloudWholeTaskObserverProductionHarnessTest` 12/12,
    `BattleRadarTurnContractTest` 8/8 => 42/42.
  - new contract explicitly covers terminal typed fact + same-sequence ROI fence + newer confirming ROI +
    same-frame replay + later confirming ROI, with exactly one `PATHING_TERMINAL`.
- no other source or test changed in this repair; prior out-of-scope aggregate/task-family blockers remain exactly
  as recorded in the whole-stage delivery.

<!-- TRUE_EOF: TURN-40G STAGE2-TERMINAL-EDGE-REPAIRED REDELIVERED OWNER-FERMAT AWAITING-PARENT-FINAL-REVIEW EXACTLY-ONCE-TERMINAL 42-OF-42 STAGES3-6-CLOSED 2026-07-23T04:07:11.843-04:00 -->

## PARENT STAGE 2 SOURCE REVIEW #19 - REPAIR REQUIRED - 2026-07-23

- verdict: `P0/P1/P2=0/1/0 / STAGE 2 BLOCKED / OWNER FERMAT RETAINED`.
- `P1` reliable `PRE_BATTLE_TIMEOUT` edge can be permanently lost after the inbox has acknowledged and drained it:
  - `CloudWholeTaskObserver.observeLoop(...)` drains the complete exact-run edge batch, then invokes
    `consumeClientClickEdges(...)` before `runNoTurnObservationProbes(...)`. Any runtime failure in the former
    escapes to the outer cycle catch, but the timeout edge has already been removed from
    `CloudWindowObservationInbox.unconsumedEvents`; the client will not resend an acknowledged event.
  - `probePreBattleTimeout(...)` returns when `ObserverState.combat == IN_COMBAT`. Because its input batch is
    already drained, a delayed but valid timeout edge is also discarded rather than published exactly once.
- required repair:
  - consume/publish retained `PRE_BATTLE_TIMEOUT` first and isolate it from unrelated edge consumers and typed
    pathing failures;
  - do not silently discard an acknowledged timeout solely because the observer's current combat snapshot is
    `IN_COMBAT`; preserve the event's existing payload and let the owning task decide its consequence;
  - use the real client detail encoding (`|` separators) in the production test and assert
    `startedAtMs`, `publishedAtMs`, `targetKeyword`, source, exact window/hwnd and exactly-once publication;
  - add focused cases proving timeout publication survives (a) an unrelated client-click consumer failure and
    (b) an `IN_COMBAT` observer snapshot. No second queue/store and no business-state mutation are allowed.
- accepted portion: the terminal typed-fact repair is correct: same-sequence ROI remains fenced, the first
  strictly newer confirming ROI publishes once, and replay/later confirming ROI does not duplicate the wake.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW19 STAGE2 P0-0-P1-1-P2-0 REPAIR-REQUIRED RELIABLE-PREBATTLE-EDGE OWNER-FERMAT STAGES3-6-CLOSED 2026-07-23 -->

## STAGE 2 REVIEW #19 RELIABLE PRE-BATTLE EDGE RE-DELIVERY - 2026-07-23T04:15:38.336-04:00

- state: `RE-DELIVERED / AWAITING PARENT REVIEW / OWNER FERMAT`; `P0/P1/P2 = NOT SELF-APPROVED`.
- repair:
  - `observeLoop(...)` now routes each drained exact-run batch through `processObservationEdges(...)`, which
    publishes retained `PRE_BATTLE_TIMEOUT` before any fallible typed-pathing or unrelated click-edge work.
  - typed pathing retains its existing per-probe isolation; client-click consumption is independently caught
    only after reliable timeout publication, so either failure cannot erase an ACKed/drained timeout.
  - `probePreBattleTimeout(...)` no longer drops an acknowledged timeout because the observer snapshot is
    `IN_COMBAT`; it publishes the unchanged typed ready event and leaves all business consequence to the task
    consumer.
  - real Client detail encoding is parsed and tested with `|` separators.
- superseding files:
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
    SHA256 `625B0671EBF695EC0ADDE23C2906009A981066CE9E77F294C50FDC157C7AA57F`,
    mtime `2026-07-23T04:12:21.078-04:00`, 1471 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverProductionHarnessTest.java`
    SHA256 `82521EA8A4ED854026878AEBDDE1DD2EA6D62B439412E33348828D0C147B6A5F`,
    mtime `2026-07-23T04:13:29.310-04:00`, 941 lines.
- focused evidence:
  - new production-harness case accepts one exact-run batch containing an `IN_COMBAT`
    `PRE_BATTLE_TIMEOUT` and an unrelated `ENTER_BATTLE_CLICKED`; the intentionally null prepared-state makes
    the click consumer throw after the timeout publication.
  - asserted ready payload: `windowId=window`, `hwnd=0x40f`, `source=wubei:timer`,
    `targetKeyword=灵兽村`, exact `startedAtMs`, `publishedAtMs`, `createdAtMs`; a second inbox drain is empty
    and the ready sequence remains unchanged (exactly once).
  - existing synthetic typed-pathing-failure case still proves the same retained timeout survives pathing
    failure.
- verification:
  - Cloud `mvn -q -DskipTests=false compile` => exit 0.
  - isolated focused-test `javac -encoding UTF-8` for the five named families => exit 0.
  - exact named run:
    `CloudObservationContractTest` 8/8,
    `FastExpectedExitObservationContractTest` 4/4,
    `CloudWholeTaskObserverPolicyContractTest` 10/10,
    `CloudWholeTaskObserverProductionHarnessTest` 13/13,
    `BattleRadarTurnContractTest` 8/8 => `43/43`, failures 0, errors 0.
- boundary/source scan: no Client/shared DTO/task source changed; this repair adds no thread, store, queue,
  lock, lease, command capture, runtime local-service call, cleanup or input. Stages 3-6 remain closed.

<!-- TRUE_EOF: TURN-40G STAGE2-REVIEW19-PREBATTLE-EDGE-REPAIRED REDELIVERED OWNER-FERMAT AWAITING-PARENT-REVIEW 43-OF-43 STAGES3-6-CLOSED 2026-07-23T04:15:38.336-04:00 -->

## PARENT STAGE 2 FINAL SOURCE REVIEW #20 - PASSED - 2026-07-23

- verdict: `P0/P1/P2=0/0/0 / STAGE 2 SOURCE+TEST REVIEW PASSED`. Review #19 is closed; Stage 2 owner is
  released and Stage 3 may begin only under the contract below.
- parent source review:
  - inbox acceptance is exact-window/exact-run, latest-wins and transactionally validates typed pathing before
    mutating sequence, ordinary facts, ROI frames, events or listener state;
  - the existing `CloudNavigationPathingState` remains the sole mirror and enforces colon-delimited child-run,
    window/hwnd, sequence and CURRENT/REPLACED/CLEARED lineage;
  - typed classification fences the fact sequence, consumes only a strictly newer ROI, suppresses the legacy
    command probe after first typed acceptance and publishes each terminal intent once;
  - retained timeout publication precedes and is isolated from typed-pathing and local-click failures, preserves
    the real Client payload and no longer drops an acknowledged edge during combat.
- parent verification: all five delivered SHA256 values match the physical files; Cloud compile independently
  rerun with exit 0; delivered focused family is `43/43`; the no-turn pathing/timer slice has zero references to
  `CloudTaskTurnCoordination`, `TurnGameClient`, runtime local-service, input, cleanup, route settlement,
  target-map gate or stopped-static consequence. No runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW20 STAGE2 P0-0-P1-0-P2-0 SOURCE-TEST-PASSED OWNER-RELEASED STAGE3-OPEN 2026-07-23 -->

## PARENT STAGE 3 IMPLEMENTATION CONTRACT - NO-TURN COMBAT / TASK-OWNED CLEANUP - 2026-07-23

- state: `STAGE 3 IMPLEMENTING / OWNER FERMAT / PARENT FINAL REVIEW REQUIRED`. Stages 4-6 remain closed.
- detection boundary: `CloudWholeTaskObserver` must run the existing `BattleRadarService` observation-ROI
  detector and publish `COMBAT_STATE_CHANGED` without `CloudTaskTurnCoordination`. Preserve the exact four-stage
  radar, templates, thresholds, missing/stale-frame fail-closed behavior, entry/exit hysteresis and
  `GameContext` update semantics. A click edge is never combat truth.
- fast-exit boundary: retain the existing local expected-exit runner, exact wait/generation identity,
  first-frame baseline, `15s/1s/0.35`, reliable edge and ordinary radar fallback. Do not merge the fast edge with
  ordinary ROI classification or weaken trusted-`IN_COMBAT` correction/re-arm behavior.
- consequence boundary: the observer may publish only the combat fact/ready wake. It must not create
  `CloudWholeTaskRuntimeLocalServiceClient`, invoke `WHOLE_TASK_COMBAT_ENTRY_CLEANUP`, clear prepared/pathing
  state, capture, input or mutate task phase. Wubei/Xiuluo must perform their existing cleanup only after the
  owning task has resumed and holds its normal task turn.
- once-only task consumption: reuse the existing ready-event sequence plus the task's existing exact run/round
  state; do not add a second global cleanup store, queue, lock, lease or worker. Cleanup must correlate exact
  run/window/hwnd and only a real transition into `IN_COMBAT`; retries after BUSY/uncertain remain eligible, while
  a confirmed cleanup is not repeated. Preserve Wubei's dialog/prepared/target-map/pathing cleanup and Xiuluo's
  tracker-shortcut pathing cleanup exactly.
- required tests:
  - hold/refuse the task turn while ordinary ROI combat entry and exit still update state and publish one wake
    per transition, with zero command/local-service/cleanup/input;
  - missing/stale ROI remains fail-closed and cannot invent entry/exit; click-only edge cannot publish combat;
  - Wubei and Xiuluo task consumers perform exact cleanup after turn reacquisition, retry uncertain outcomes,
    reject wrong run/window/hwnd/stale sequence and do not repeat confirmed cleanup;
  - fast expected-exit focused contracts, `BattleRadarTurnContractTest`, Stage 2 `43/43`, affected task named
    families and Cloud compile remain green. No broad fixture rewrite outside the frozen write set.

<!-- TRUE_EOF: TURN-40G STAGE3-CONTRACT NO-TURN-COMBAT TASK-OWNED-CLEANUP OWNER-FERMAT STAGES4-6-CLOSED PARENT-REVIEW-REQUIRED 2026-07-23 -->

## STAGE 3 CANONICAL CLAIM - 2026-07-23T04:18:54.037-04:00

- owner: `FERMAT`; state: `STAGE 3 IMPLEMENTING`; parent final review required.
- claimed scope is exactly the Stage 3 EOF contract: existing observation-ROI combat detection and
  `COMBAT_STATE_CHANGED` publication without observer turn/local-service/cleanup/input, plus exact
  Wubei/Xiuluo task-owned once-only cleanup after normal turn reacquisition.
- `D:\mavenProject\DHXY` remains strict read-only; dirty/untracked work in both active repositories is preserved;
  no Git mutation and no runtime/UI/live capture/input.
- Stages 4-6 remain closed. `P0/P1/P2 = NOT SELF-APPROVED`.

<!-- TRUE_EOF: TURN-40G STAGE3-CLAIMED OWNER-FERMAT IMPLEMENTING STAGES4-6-CLOSED PARENT-REVIEW-REQUIRED 2026-07-23T04:18:54.037-04:00 -->

## STAGE 3 CANONICAL WHOLE-STAGE SOURCE+TEST DELIVERY - 2026-07-23T04:40:36.878-04:00

- state: `STAGE 3 WHOLE-STAGE DELIVERED / AWAITING PARENT FINAL REVIEW / OWNER FERMAT`.
  `P0/P1/P2 = NOT SELF-APPROVED`; Stages 4-6 remain closed.
- scope: Cloud source/tests plus this card EOF only. `D:\mavenProject\DHXY` remained strict read-only; both active
  dirty/untracked repositories were preserved; no Git mutation and no runtime/UI/live capture/input was run.
- implementation:
  - `CloudWholeTaskObserver` now invokes the existing four-stage `BattleRadarService` over observation ROI frames
    through `runNoTurnCombatProbe(...)`. Ordinary `IN_COMBAT`/`NOT_IN_COMBAT` transitions update the existing
    `GameContext` and publish typed `COMBAT_STATE_CHANGED` without consulting or acquiring
    `CloudTaskTurnCoordination`.
  - the observer combat slice no longer creates a runtime local-service client, invokes combat-entry cleanup,
    clears prepared/pathing state, captures through the command plane or performs input. Missing/stale ROI remains
    unavailable/fail-closed; a click edge remains only a click edge and cannot publish combat truth.
  - combat ready events carry and strictly require exact `taskRunId/windowId/hwnd` plus typed
    `WindowCombatState`. The existing ready map retains the latest entry and exit separately, so a later exit does
    not erase the entry sequence before the task consumes cleanup; wrong/missing identity and malformed state
    shape are rejected. No second store/queue/lock/lease was added.
  - Wubei and Xiuluo consume a strictly newer exact combat-entry ready sequence at the beginning of their existing
    normal phase transaction, after task-turn acquisition. They invoke the existing
    `WHOLE_TASK_COMBAT_ENTRY_CLEANUP` with exact run/round/ready sequence; BUSY/uncertain remains eligible for the
    next phase turn, while an EXECUTED typed cleanup advances the prototype-local sequence cursor and cannot
    repeat. Existing Wubei prepared/pathing cleanup and Xiuluo tracker-shortcut pathing cleanup remain in the task.
  - fast expected-exit keeps its existing wait/generation identity, first-frame baseline, `15s/1s/0.35`,
    correction/re-arm and reliable edge semantics; only its ready wake now carries the same exact typed exit
    identity required by the shared ready state.
- exact production files:
  - `src/main/java/com/bot/dhxy/window/model/WindowCombatState.java`
    SHA256 `A0F4AA7CE32A0A02818EFF8F8F28BA03FBE8F25D84A4AFC4D7268489511EE2D9`,
    mtime `2026-07-23T04:23:07.651-04:00`, 7 lines.
  - `src/main/java/com/bot/dhxy/window/model/WindowReadyEvent.java`
    SHA256 `C7942CDC9281DE890F89E90F2809F98625A1394A266D64E185CC6F8AC8FEEF84`,
    mtime `2026-07-23T04:23:07.652-04:00`, 42 lines.
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java`
    SHA256 `250A9FFCF292938FCB70383A1F2EC0C90F5676F5D7B37E1F3E0C2031EF7D768C`,
    mtime `2026-07-23T04:38:59.734-04:00`, 543 lines.
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
    SHA256 `15D4ED476A83AE6818FAA1C1E1E976D8CF57B1F028927D057AA8F7896874BD02`,
    mtime `2026-07-23T04:26:28.178-04:00`, 1479 lines.
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudFastExpectedCombatExitCoordinator.java`
    SHA256 `BE9AE7FDAEB217C5D42F7531605E1E406E58F956560443FEF90093AB7114F666`,
    mtime `2026-07-23T04:23:29.911-04:00`, 308 lines.
  - `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
    SHA256 `A099506397EB7FE6B0F2DCC639052443113CAFE3F91F9753108008EFA43F52E3`,
    mtime `2026-07-23T04:25:24.588-04:00`, 4525 lines.
  - `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
    SHA256 `96EFF37D0C076C41608E2633F4390B22BE43A3C7F89640594A31F535B931023D`,
    mtime `2026-07-23T04:24:52.501-04:00`, 4727 lines.
- exact focused-test files:
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverProductionHarnessTest.java`
    SHA256 `0928E019B55A04E269F91DBEA6BDC1CF89CC0FF0CD3FF4018BE54C46BC14C4B3`,
    mtime `2026-07-23T04:38:22.192-04:00`, 1119 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`
    SHA256 `870BECE076446FB09195DE0AF6A85ED156A3D2EDD91B0F6B7CE77E84B3E253A2`,
    mtime `2026-07-23T04:31:44.868-04:00`, 926 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java`
    SHA256 `8A2E7329903486988D2F35E341B1F97B13BC5BDC9857AF1B675E51A0C8B31E66`,
    mtime `2026-07-23T04:35:35.571-04:00`, 1006 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskFoundationContractTest.java`
    SHA256 `52D433EBED9503CBF5BF1198E67B7BEE58C6D0A226DA29DC6EB07E87B7FF9485`,
    mtime `2026-07-23T04:38:59.735-04:00`, 1178 lines.
  - constructor-only compile alignment in
    `CloudTurnTaskRuntimeContractTest.java`
    SHA256 `564C28CACFA5ABC7F5F045BF8144403E521C7E1910B8D4669AC90D5B17BA66EE`
    and `CloudTurnTaskFactoryAllowlistTest.java`
    SHA256 `CDBE1A0A6346B4BB64E92BC9FE5E3497E96CFC64DA7128DB2A765F48ADD05D61`;
    each removes one stale extra null argument and changes no assertion or business fixture.
- verification:
  - `mvn -q -DskipTests=false compile` => exit 0.
  - exact named run:
    `CloudObservationContractTest` 8/8,
    `FastExpectedExitObservationContractTest` 4/4,
    `BattleRadarTurnContractTest` 8/8,
    `CloudWholeTaskObserverPolicyContractTest` 10/10,
    `CloudWholeTaskObserverProductionHarnessTest` 15/15,
    Wubei Stage-3 consumer 1/1 and Xiuluo Stage-3 consumer 1/1 => `47/47`, failures 0, errors 0.
  - production harness covers refused ordinary task turn while ROI entry+exit still publish, exact entry retained
    after exit, wrong run/hwnd rejection, missing and stale ROI fail-closed, click-only no combat truth, zero command
    executions, BUSY/uncertain retry and confirmed exactly-once cleanup.
  - source scan: `CloudWholeTaskObserver.java` has zero `cleanupCombatEntry` /
    `WHOLE_TASK_COMBAT_ENTRY_CLEANUP` hits; combat call sites are only
    `runNoTurnCombatProbe(...) -> probeCombat(...)`. Fast-exit constants remain `15_000`, `1_000`, `0.35` and
    exact `expectedWaitId/combatGeneration`.
- preserved pre-existing broad-family drift, explicitly rerun and unchanged except each new Stage-3 test passes:
  - Wubei whole: 16 tests, 0 failures / 2 errors; both old tests still use a six-field strict-result JSON missing
    later protocol fields.
  - Xiuluo whole: 20 tests, 1 failure / 2 errors; old fixtures still omit `NavigationService` and call the removed
    legacy `isNearCoordinate(...)` signature.
  - the unrelated Foundation aggregate remains 35 tests, 1 failure / 1 error in pre-existing freshness/timing
    assertions. No broad fixture rewrite was performed to manufacture a green result.

<!-- TRUE_EOF: TURN-40G STAGE3-WHOLE-STAGE-DELIVERED OWNER-FERMAT AWAITING-PARENT-REVIEW P0-P1-P2-NOT-SELF-APPROVED 47-OF-47 STAGES4-6-CLOSED 2026-07-23T04:40:36.878-04:00 -->

## PARENT STAGE 3 SOURCE REVIEW #21 - REPAIR REQUIRED - 2026-07-23

- verdict: `P0/P1/P2=0/1/0 / STAGE 3 BLOCKED / OWNER FERMAT RETAINED`.
- `P1` no-turn combat probing introduces a real same-window data race that the delivered serial tests do not cover:
  - `CloudWholeTaskObserver.probeCombat(...)` now calls
    `BattleRadarService.checkAndSyncCombatState(RoiFrameSource)` outside the task turn;
  - the owning task still reaches the same singleton `BattleRadarService` through
    `AutoCombatService.handleCombatTick(...)` / `probeWindowCombatStateReadOnly(...)`, while
    `CloudFastExpectedCombatExitCoordinator` may concurrently call
    `recordFastExpectedCombatExit(...)`;
  - `BattleRuntimeState` keeps ordinary mutable `combatExitMisses`, enter/exit pending flags, battle count,
    expected-wait identity/generation and fast baseline fields. The old turn-serialized observer could not race
    these task paths; Stage 3 can now overwrite/reset a miss count, lose/duplicate a pending edge, or interleave
    `GameContext` transitions.
- required repair:
  - serialize radar state transitions per exact bound window using the existing per-window runtime-state ownership;
    do not add a global lock, second state store/queue/lease/worker and do not reacquire `CloudTaskTurnCoordination`;
  - keep image capture/template work outside any broad/global critical section where possible, but make the final
    state/hysteresis/fast-identity transition atomic for one window; different windows must remain independent;
  - add a deterministic concurrency contract that races ordinary observation entry/exit with task/fast-exit access
    and proves one transition, intact two-miss exit hysteresis, exact wait/generation fencing, and no lost or
    duplicated enter/exit pending signal;
  - rerun Stage 3 `47/47`, the new concurrency contract, `BattleRadarTurnContractTest`, fast-exit contracts and
    Cloud compile. Preserve all frozen radar constants, templates and business ordering.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW21 STAGE3 P0-0-P1-1-P2-0 REPAIR-REQUIRED PER-WINDOW-RADAR-ATOMICITY OWNER-FERMAT STAGES4-6-CLOSED 2026-07-23 -->

## STAGE 3 REVIEW #21 CANONICAL RE-DELIVERY - 2026-07-23T04:56:05.245-04:00

- ACK: `PARENT-REVIEW21-PER-WINDOW-RADAR-ATOMICITY`.
- state: `REPAIRED / WHOLE RE-DELIVERED / AWAITING PARENT REVIEW / OWNER FERMAT`;
  `P0/P1/P2 = NOT SELF-APPROVED`; Stages 4-6 remain closed.
- repair:
  - the existing exact-window `BattleRuntimeState` instance is now the sole monitor for ordinary radar,
    task-side `AutoCombatService` radar access and fast-exit coordinator radar access. No global lock, second
    state store/queue/lease/worker or task-turn reacquisition was introduced.
  - one ordinary `checkAndSyncCombatState(...)` holds that exact-window monitor for the complete ordered sample:
    auto flag, selection, top icons, two-miss hysteresis, optional minimap confirmation and final transition.
    A blocked first same-window probe therefore cannot be overtaken by a later sample; different window state
    instances remain parallel.
  - enter/exit transition, battle count, miss counter, avatar baseline/probe cadence, expected wait/generation,
    pending enter/exit edges and all consume/discard operations use the same per-window monitor.
  - `GameContext.State.currentActionState` is `volatile`, giving JMM visibility to task/other-service reads that
    intentionally do not acquire the radar monitor. This also makes
    `keepCombatForUnavailableProbe(...)` observe an observer-thread combat write without timing assumptions.
- exact files:
  - `src/main/java/com/bot/dhxy/core/GameContext.java`
    SHA256 `DD7A851B17E17A8E6A2E1737704D9D67862F67684D7AAD58CEB047A327A55947`,
    mtime `2026-07-23T04:50:06.281-04:00`, 206 lines.
  - `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
    SHA256 `50D4CC54BC9A234A741C08123260A5C8DC639E4EBCC534237EDAAC0038EB9CFD`,
    mtime `2026-07-23T04:54:23.507-04:00`, 1722 lines.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/BattleRadarTurnContractTest.java`
    SHA256 `BAB9A5A33BE92FE07FF28F924295DD62860C0F8FFD4E3CDAA65F4103680217CF`,
    mtime `2026-07-23T04:54:37.948-04:00`, 1168 lines.
- deterministic concurrency evidence:
  - observer combat write is task-thread visible and an unavailable probe preserves visible `IN_COMBAT`;
  - one miss cannot exit; a concurrent second ordinary miss and exact fast-exit produce exactly one transition,
    preserve exact wait/generation fencing, and retain enter/exit pending edges exactly once;
  - holding window A's radar monitor blocks window A access while window B completes;
  - a blocked ordinary sample prevents a later same-window probe from entering capture/commit until release.
- verification:
  - Stage 3 + Review #21 exact suite:
    `CloudObservationContractTest` 8/8,
    `FastExpectedExitObservationContractTest` 4/4,
    `BattleRadarTurnContractTest` 12/12,
    `CloudWholeTaskObserverPolicyContractTest` 10/10,
    `CloudWholeTaskObserverProductionHarnessTest` 15/15,
    Wubei consumer 1/1, Xiuluo consumer 1/1,
    `CloudFastExpectedCombatExitArmLifecycleTest` 4/4 and `FastExpectedExitGateTest` 2/2;
    total `57/57`, failures 0, errors 0.
  - `mvn -q -DskipTests=false compile` => exit 0.
  - source scan confirms no `CloudTaskTurnCoordination` reference or new store/queue/lock/lease/worker in the
    repair. Existing `runtimeStates` remains the one pre-existing per-window map.
  - extra non-gating broad `AutoCombatServiceTurnContractTest` audit remains at its pre-existing fixture drift:
    39 tests, 3 failures / 6 errors (old exact-window metadata, command-step and count expectations). No broad
    fixture rewrite was made; the approved exact Stage 3/Review #21 families are green.
- safety: `D:\mavenProject\DHXY` remained read-only; dirty/untracked work preserved; zero Git mutation and zero
  runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G STAGE3-REVIEW21-REDELIVERED ACK-PARENT-REVIEW21-PER-WINDOW-RADAR-ATOMICITY OWNER-FERMAT AWAITING-PARENT-REVIEW P0-P1-P2-NOT-SELF-APPROVED 57-OF-57 STAGES4-6-CLOSED 2026-07-23T04:56:05.245-04:00 -->

## PARENT STAGE 3 FINAL SOURCE REVIEW #22 - PASSED - 2026-07-23

- verdict: `P0/P1/P2=0/0/0 / STAGE 3 PASSED / OWNER RELEASED FOR STAGE / STAGE 4 OPEN`.
- source review:
  - `GameContext.State.currentActionState` is `volatile`, so reads intentionally outside the radar monitor observe
    observer-thread state transitions.
  - the existing exact-window `BattleRuntimeState` is the sole monitor for the complete ordered radar sample and
    every mutation/consume of miss hysteresis, battle count, pending enter/exit edges, expected wait/generation and
    fast-exit baseline. A blocked sample cannot be overtaken by a later sample for the same window.
  - monitor identity remains per window; different windows are not serialized. No global lock, second state
    store/queue/lease/worker or `CloudTaskTurnCoordination` reacquisition was added.
  - no lock inversion or callback into the radar monitor was found in the capture seam. Frozen four-stage radar,
    two-miss exit, `15_000/1_000/0.35`, exact wait/generation and task-owned cleanup contracts remain unchanged.
- exact reviewed SHA:
  - `GameContext.java` `DD7A851B17E17A8E6A2E1737704D9D67862F67684D7AAD58CEB047A327A55947`;
  - `BattleRadarService.java` `50D4CC54BC9A234A741C08123260A5C8DC639E4EBCC534237EDAAC0038EB9CFD`;
  - `BattleRadarTurnContractTest.java`
    `BAB9A5A33BE92FE07FF28F924295DD62860C0F8FFD4E3CDAA65F4103680217CF`.
- independent verification:
  - `mvn -q -DskipTests=false compile` => exit 0;
  - Stage 3 named families plus deterministic concurrency coverage => `57/57`, failures 0, errors 0.
- residual broad-fixture drift remains pre-existing and is not hidden: old Wubei/Xiuluo/Foundation/AutoCombat
  aggregate fixtures still contain removed constructor/protocol/freshness assumptions. Stage 3 exact contracts pass;
  fixture repair may only occur in a later stage when the fixture is inside that stage's frozen write set.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW22 STAGE3 PASSED P0-0-P1-0-P2-0 CLOUD-COMPILE 57-OF-57 OWNER-RELEASED STAGE4-OPEN 2026-07-23 -->

## PARENT STAGE 4 IMPLEMENTATION CONTRACT - DIALOG/TRACKER ROI PREPARATION - 2026-07-23

- owner: `External-A / Fermat`; ACK required:
  `PARENT-STAGE4-DIALOG-TRACKER-ROI-PREPARATION`.
- state: `STAGE 4 OPEN / STAGES 5-6 CLOSED / PARENT FINAL REVIEW REQUIRED`.
- allowed result: the existing `WindowObservationRunner` samples Cloud-issued exact ROI interests; Cloud
  `CloudWholeTaskObserver` consumes those uploaded frames without task turn and may only publish an immutable
  prepared candidate or soft ready/typed tracker fact into the existing exact-run stores. It grants no action
  authority.
- dialog contract:
  - remove `probeAttention(...)` dependence on `readDialogRuntimeFact(...)` and
    `detectDialogSnapshotNoFocus(...)`; Observer must not call command capture or a runtime local service.
  - carry the existing exact dialog interest identity/operations/expiry through the observation plane using the
    current protocol/inbox lifecycle; do not create a second interest store, event bus or business protocol.
  - analyze only an exact-run uploaded dialog ROI, reusing the existing supplied-frame `DialogService`
    preparation paths and preserving their current ordering: route keyword, Wubei accept/green/story behavior and
    Xiuluo Cloud enter-battle fallback. Do not add kanda assets, enable local-kanda or change fallback/retry order.
  - missing, undecodable, stale, pre-interest, wrong run/window/hwnd/revision or replaced-interest frames are
    `unavailable`; they publish no prepared action and no `CLOUD_NO_ACTION`. A genuine analyzed miss may retain only
    the already-approved explicit no-action semantics for the exact attempt.
- tracker contract:
  - replace Observer `prepareWuhuanPathingLink(...)` command/capture path with pure analysis of the uploaded
    exact-window tracker frame through the existing Cloud `TaskTrackerPanelService` supplied-frame API.
  - recognition may prepare the same immutable click candidate or publish the same typed positive/negative fact.
    Any panel drag, supplemental capture, click or retry is a Stage 5 owning-task consequence and must not execute
    in Observer. `RUNNER_PREPARED_NOT_READY + title visible -> SYNC_TASK_PANEL` remains unchanged.
- identity and atomicity:
  - fence every frame/candidate to `taskRunId/windowId/hwnd`, monotonic `observerSeq`, `capturedAtMs`, current
    interest revision and applicable attempt/round/intent identity. Same-frame replay is idempotent; latest exact
    frame wins; stale candidates cannot overwrite or clear a current prepared slot.
  - continue using `CloudDialogPreparedActionState`, `CloudWholeTaskReadyEventState` and the current observation
    inbox as sole ownership/state boundaries. No second queue, lock, lease, worker, runner or store.
- forbidden in Stage 4: task phase edits, click/key/focus/drag, command-plane capture, runtime local-service command,
  combat cleanup, Wubei target-map gate mutation, route-learning settlement, retry-budget consumption, or deletion
  of `observerTurn`/`runParkedProbe`; those remain for ordered Stages 5-6.
- required deterministic tests:
  1. while the owning task deliberately holds task turn, a newer exact dialog ROI still prepares and publishes the
     same candidate/ready event with zero command/local-service/capture/input calls;
  2. the equivalent tracker ROI prepares/publishes without turn; a drag-required/not-ready frame performs no action
     and preserves the frozen FiveRing handover contract;
  3. stale, missing, malformed, wrong run/window/hwnd/revision/attempt frames fail closed, and replacement overlap
     cannot publish or clear the new candidate;
  4. exact template/OCR/fallback ordering and click coordinates are replayed against the existing repository
     testcase fixtures; no detector constant or asset changes;
  5. Stage 3 `57/57`, observer production/policy, relevant FiveRing/Wubei/Xiuluo preparation contracts and both
     repository compiles remain green.
- delivery must list every production/test file SHA and physical EOF, plus source scans proving the Stage 4
  dialog/tracker slice has zero `TurnGameClient`, runtime local-service, input/focus/drag and cleanup execution.

<!-- TRUE_EOF: TURN-40G PARENT-STAGE4-CONTRACT OWNER-FERMAT ACK-PARENT-STAGE4-DIALOG-TRACKER-ROI-PREPARATION DIALOG-TRACKER-ROI-PREP-ONLY STAGES5-6-CLOSED PARENT-REVIEW-REQUIRED 2026-07-23 -->

## STAGE 4 CANONICAL CLAIM - 2026-07-23T05:05:57.831-04:00

- owner: `External-A / Fermat`.
- ACK: `PARENT-STAGE4-DIALOG-TRACKER-ROI-PREPARATION`.
- state: `SOURCE_ACTIVE / DIALOG-TRACKER ROI PREPARATION ONLY / STAGES 5-6 CLOSED`.
- frozen boundary accepted: reuse existing runner/inbox/interests and supplied-frame dialog/tracker analyzers; publish
  only immutable candidates and existing ready/fact state with exact identity and sample fencing. No task-turn,
  command capture, runtime local-service command, input/focus/drag/cleanup/phase consequence is authorized.
- safety: `D:\mavenProject\DHXY` remains strict read-only; preserve all dirty/untracked state; zero Git mutation and
  zero runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G STAGE4-CANONICAL-CLAIM OWNER-FERMAT ACK-PARENT-STAGE4-DIALOG-TRACKER-ROI-PREPARATION SOURCE-ACTIVE STAGES5-6-CLOSED 2026-07-23T05:05:57.831-04:00 -->

## STAGE 4 CANONICAL WHOLE-STAGE SOURCE+TEST DELIVERY - 2026-07-23T06:07:00-04:00

- owner: `External-A / Fermat`; state: `WHOLE-STAGE SOURCE+TEST DELIVERED / AWAITING PARENT REVIEW`.
- review declaration: `P0/P1/P2 NOT SELF APPROVED`; Stages 5-6 remain closed.
- delivered behavior:
  - Client reuses the sole `WindowObservationRunner` and uploads bounded dialog/tracker ROIs with typed,
    enum-allowlisted dialog interest identity. Dialog, tracker and pathing lineage are independently fenced.
  - Cloud inbox accepts same-frame replay only when payload-identical; same-sequence conflicting fact/ROI payloads
    cannot overwrite current state. Exact run/window/hwnd/observerSeq/capturedAt/revision/attempt/round/intent
    mismatches fail closed.
  - `CloudWholeTaskObserver` prepares dialog and tracker candidates from uploaded ROIs while the owning task turn is
    deliberately held. Missing/stale/replaced/unavailable frames publish neither negative truth nor stale clears.
  - tracker supplied-frame analysis now carries explicit frame origin. Full-window restored bounded ROI uses
    `(0,0)` while the legacy panel-cropped path retains panel origin, preserving detected box, absolute click,
    validation crop and fingerprint without duplicating the recognizer.
  - drag-required/not-ready tracker input performs zero action and retains the existing FiveRing handover.

### Exact write set

- Client `D:\mavenProject\DHXY-cr271`:
  - `ObservationDialogInterestFact.java` SHA256
    `81DCA88615C779A1B86BE1C09D190F3BA814141BD8A30F1CDF3CB23FD118F5E5`,
    mtime `2026-07-23T05:18:14.733-04:00`, 29 lines.
  - `ObservationDialogOperation.java` SHA256
    `970693D15F4558B58DCE05E2550574BFEEAC54D3FFDA3222C4E4A6748B159472`,
    mtime `2026-07-23T05:31:50.072-04:00`, 23 lines.
  - `ObservationProtocolValidator.java` SHA256
    `614A981E41CC065BC764F1592435F79CF0AC1B8E43E9182D37C179988880613B`,
    mtime `2026-07-23T05:32:21.462-04:00`, 442 lines.
  - `ObservationRequest.java` SHA256
    `F4D3C33829340F3401E1999D943F43A260E6A01BE0D26A9E70A0B385B8D8F86B`,
    mtime `2026-07-23T05:20:46.408-04:00`, 108 lines.
  - `ObservationRoi.java` SHA256
    `DE00AC9314D117046DEF26E0BE5F48CDD614B59F4FBDD4E301854FE9B263F27B`,
    mtime `2026-07-23T05:31:50.071-04:00`, 38 lines.
  - `WindowObservationRunner.java` SHA256
    `5CEACF6CD9E09F5F94B056C729C0CEFB0C16C3F820EA99C44CF1BE3DA27AD2DB`,
    mtime `2026-07-23T05:32:41.016-04:00`, 443 lines.
  - `WindowObservationSampler.java` SHA256
    `76059300D8407350F3E2D3479D76432EDF7695C11EF09EB2F605744E211D5540`,
    mtime `2026-07-23T05:32:35.044-04:00`, 550 lines.
  - `WindowObservationRunnerContractTest.java` SHA256
    `A9B5A4025DB5B21E43E796AACE176DC1FBCC015F94E5C3A7C1AFC2E1B3FDC187`,
    mtime `2026-07-23T05:34:20.102-04:00`, 426 lines.
- Cloud `D:\mavenProject\dhxy-cloud-brain`:
  - the five shared protocol files above are byte-identical with the same SHA256 values; Cloud mtimes are
    respectively `05:18:14.733`, `05:31:50.072`, `05:32:21.463`, `05:20:46.409`, `05:31:50.072` (`-04:00`).
  - `CloudWindowObservationInbox.java` SHA256
    `56D39353FDE6733E96F331B3F104A3FC108F39C4E43F92718D77F924C7A1E0F0`,
    mtime `2026-07-23T05:33:51.765-04:00`, 555 lines.
  - `CloudWholeTaskObserver.java` SHA256
    `33B348BF61CE424A02A923F0BA282FAFE96FC27A343B00F2EF7D22176025F9BF`,
    mtime `2026-07-23T05:38:59.141-04:00`, 1644 lines.
  - `DialogService.java` SHA256
    `65C30DAD36FE51A355BCF87D1207EFE5171CF787EE204CE9A71BC514A6064680`,
    mtime `2026-07-23T05:16:55.869-04:00`, 2997 lines.
  - `TaskTrackerPanelService.java` SHA256
    `0CA6309ADB95AC29DFA330EB94A3BC8A3E64E8F1159FC9387130B30F460CA080`,
    mtime `2026-07-23T05:42:48.845-04:00`, 1393 lines.
  - `TaskExecutionContext.java` SHA256
    `FFC9D94AC39F8A36005197FA1D32FB27CCC4646D3260BFA7344579853EC153E2`,
    mtime `2026-07-23T05:16:30.034-04:00`, 598 lines.
  - `CloudObservationContractTest.java` SHA256
    `401AE1301C88E581B854CAE3FEA4018EDA49320EE188507B61E21DCF20CAA391`,
    mtime `2026-07-23T05:36:28.428-04:00`, 893 lines.
  - `TaskTrackerPanelTurnContractTest.java` SHA256
    `3092A07B93D4BFBA8DE6B326A1D5DD1D1D6497F85D66EFDC423DD874DC3D91D3`,
    mtime `2026-07-23T05:52:13.578-04:00`, 929 lines.
  - `CloudWholeTaskObserverProductionHarnessTest.java` SHA256
    `73FF009898DE0FCA9B7DFCC8FA55964C28858AFF1571733B9A7650A07C33E148`,
    mtime `2026-07-23T05:57:30.904-04:00`, 1324 lines.

### Verification

- Client:
  - `mvn -q -Dtest=WindowObservationRunnerContractTest test` => `8/8`, failures 0, errors 0.
  - `mvn -q -DskipTests=false compile` => exit 0.
- Cloud Stage 3 + Stage 4 exact families:
  - `mvn -q "-Dtest=CloudObservationContractTest,FastExpectedExitObservationContractTest,
    BattleRadarTurnContractTest,CloudWholeTaskObserverPolicyContractTest,
    CloudWholeTaskObserverProductionHarnessTest,CloudFastExpectedCombatExitArmLifecycleTest,
    FastExpectedExitGateTest" test` => `60/60`, failures 0, errors 0
    (`12+4+12+10+16+4+2`).
  - production harness `16/16` includes held-task-turn dialog and tracker preparation, tracker drag-required/not-ready,
    wrong identity/revision/attempt and replacement-overlap fail-closed cases.
  - `mvn -q "-Dtest=WubeiWholeTaskTurnContractTest#combatEntryCleanupRetriesBusyThenExecutesOnceInsideTheTaskTurn,
    XiuluoWholeTaskTurnContractTest#combatEntryCleanupRetriesUncertainThenExecutesOnceForExactRun" test`
    => `2/2`, failures 0, errors 0. Exact core plus consumers = `62/62`.
  - `mvn -q
    -Dtest=TaskTrackerPanelTurnContractTest#suppliedBoundedTrackerFrameKeepsLegacyDetectionClickValidationAndFingerprint
    test` => `1/1`, failures 0, errors 0.
  - `mvn -q -DskipTests=false compile` => exit 0.
- broad drift retained and reported, not rewritten:
  - full `TaskTrackerPanelTurnContractTest`: 7 tests, 2 failures, 0 errors:
    old `CAPTURE -> LOCAL_SERVICE` fixture expectation and old x-coordinate `278 -> 279`.
  - full `FiveRingTaskTrackerTurnContractTest`: 7 tests, 3 failures / 4 errors from old command/local-service fixture,
    old `PT20S` timeout and old handover/count expectations; Stage 4 does not modify `FiveRingTaskV2`.
  - two prepared FiveRing consumer seams: no-proof case passes; proof case reaches old
    `TurnWholeTaskRuntimeResult` fixture missing `pendingTransferChoice`. No old business/assertion was changed.
- source scan:
  - `CloudWholeTaskObserver` tracker slice lines 333-429: 0 forbidden references.
  - `CloudWholeTaskObserver` dialog slice lines 560-732: 0 forbidden references.
  - `TaskTrackerPanelService` supplied-frame slice lines 288-337: 0 forbidden references.
  - forbidden set:
    `TurnGameClient|CloudTaskTurnCoordination|localService|capture(|execute(|input|focus|drag|cleanup|runParkedProbe`.
- safety: `D:\mavenProject\DHXY` remained read-only; all dirty/untracked work preserved; zero Git mutation and zero
  runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G STAGE4 WHOLE-STAGE SOURCE-TEST-DELIVERED OWNER-FERMAT AWAITING-PARENT-REVIEW P0-P1-P2-NOT-SELF-APPROVED CLIENT-8OF8 CLOUD-60OF60 CONSUMERS-2OF2 TRACKER-REPLAY-1OF1 DUAL-COMPILE SHARED-DTO-BYTE-IDENTICAL BROAD-DRIFT-DECLARED STAGES5-6-CLOSED 2026-07-23T06:07:00-04:00 -->

## PARENT STAGE 4 FINAL SOURCE REVIEW #23 - PASSED - 2026-07-23

- verdict: `P0/P1/P2=0/0/0 / STAGE 4 PASSED / OWNER RELEASED FOR STAGE / STAGE 5 OPEN`.
- source review:
  - Client reuses the one existing per-window observation runner. Dialog lineage is carried only by the matching
    `xiuluo-dialog` ROI; tracker ROI remains uncorrelated and cannot inherit dialog attempt/round/intent fields.
  - Client and Cloud shared protocol files are byte-identical at the delivered SHA. Dialog operations are a strict
    enum allowlist; malformed/partial correlation and invalid timing fail closed.
  - Cloud inbox rejects a same-sequence conflicting payload before mutation, accepts only current-revision exact
    geometry, and stores per-ROI lineage. Lower sequence requests cannot overwrite current fact/ROI state.
  - `probeAttention(...)` and `probeTrackerPreparation(...)` consume only uploaded exact-run frames. Both recheck
    current sample identity after analysis and neither slice references task turn, command capture, runtime local
    service, input/focus/drag, cleanup or task phase consequences.
  - tracker supplied-frame restoration preserves the legacy recognizer's window-relative coordinate system:
    restored full-window input uses frame origin `(0,0)`, while the legacy panel crop retains panel origin. The
    repository replay proves identical box, absolute click, validation crop and fingerprint.
- independent verification:
  - Client `WindowObservationRunnerContractTest` `8/8` and compile exit `0`;
  - Cloud Stage 3+4 exact families `60/60`, Wubei/Xiuluo cleanup consumers `2/2`, supplied tracker replay `1/1`,
    and compile exit `0`;
  - Stage 4 forbidden-source slices are clean. Declared broad-fixture drift remains visible and is not treated as
    Stage 4 acceptance evidence.
- business: `无已批准业务差异；按本地基线等价迁移`. Detector assets/constants, fallback order, FiveRing seven
  contracts and local-kanda disabled state are unchanged.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW23 STAGE4 PASSED P0-0-P1-0-P2-0 CLIENT-8OF8 CLOUD-60OF60 CONSUMERS-2OF2 TRACKER-REPLAY-1OF1 DUAL-COMPILE STAGE5-OPEN 2026-07-23 -->

## PARENT STAGE 5 IMPLEMENTATION CONTRACT - TASK-OWNED CONSEQUENCES - 2026-07-23

- owner: `External-A / Fermat`; ACK required:
  `PARENT-STAGE5-TASK-OWNED-CONSEQUENCES`.
- state: `STAGE 5 OPEN / STAGE 6 CLOSED / PARENT FINAL REVIEW REQUIRED`.
- objective: after an exact observation wakes the owning task, every side effect or destructive consume executes
  only from that task's existing phase flow while it holds its normal task turn. The Observer remains a fact/
  preparation producer and may not execute task consequences.
- required migration:
  - FiveRing retains all seven frozen outside-turn/inherited-turn contracts. Prepared dialog/tracker actions are
    re-read, exact-run/window/hwnd validated and consumed once only at the existing task-owned handover. Any required
    supplemental capture, tracker drag, retry or click remains in that handover and cannot run from Observer.
  - Wubei consumes the target-map gate/dialog-interest update only after the exact pathing/ready wake and authoritative
    reread. Preserve the current map condition, gate identity, dialog operation, source, phase order and retry result.
  - Wubei/Xiuluo consume pending transfer-choice and world-map route-learning slots only after the exact terminal
    pathing wake, in the frozen transfer-then-world order. A stale/replaced intent is abandoned exactly as today;
    `BUSY`, uncertain, stop or malformed outcomes remain pending/fail closed and are never recorded as success.
  - local-kanda `ENTER_BATTLE_CLICKED` may update its exact attempt fact without turn, but clearing/consuming a
    prepared action and phase consequences belong to the exact Xiuluo task after turn reacquisition. A click remains
    only pending combat confirmation; only real `IN_COMBAT` advances combat state.
  - Stage 3 combat-entry cleanup stays task-owned and once-only. No second consequence store, queue, lock, lease,
    worker, event bus or protocol is allowed; reuse existing ready/prepared/pathing/runtime pending state.
- forbidden: business phase/order changes, new timeout/TTL/retry, detector/template/coordinate changes, local-kanda
  enablement, observer command/local-service/input/focus/drag/cleanup, or deletion of observer turn wrappers. Stage 6
  alone removes wrappers after the final zero-command source audit.
- required deterministic verification:
  1. task holds turn before each gate/settlement/clear/input consequence; Observer can publish the exact wake while
     the turn is held but performs zero consequence;
  2. exact run/window/hwnd/sequence and applicable intent/attempt/round fences reject stale/replaced/cross-window
     state without consuming the current slot;
  3. uncertain/BUSY outcomes remain retryable and confirmed outcomes execute exactly once;
  4. FiveRing seven contracts, Wubei/Xiuluo pathing/dialog/combat-entry consumers, Stage 4 exact families, fast-exit
     contracts and both compiles remain green;
  5. delivery lists every production/test SHA and a source map proving Observer no longer owns the migrated
     consequence calls. Stage 6 remains closed pending parent `0/0/0`.

<!-- TRUE_EOF: TURN-40G PARENT-STAGE5-CONTRACT OWNER-FERMAT ACK-PARENT-STAGE5-TASK-OWNED-CONSEQUENCES TASK-CONSEQUENCES-ONLY STAGE6-CLOSED PARENT-REVIEW-REQUIRED 2026-07-23 -->

## STAGE 5 CANONICAL CLAIM - 2026-07-23T06:15:00-04:00

- owner: `External-A / Fermat`.
- ACK: `PARENT-STAGE5-TASK-OWNED-CONSEQUENCES`.
- state: `SOURCE_ACTIVE / TASK-OWNED CONSEQUENCES ONLY / STAGE 6 CLOSED`.
- frozen boundary accepted: Observer remains fact/preparation producer; destructive prepared/pathing/runtime consumes,
  gate/interest updates, transfer-before-world settlement, supplemental dialog/tracker action and local-kanda
  prepared clear/consume execute only in the existing FiveRing/Wubei/Xiuluo owning-task post-wake flow while its
  normal task turn is held. Stage 3 cleanup remains unchanged.
- business: `无已批准业务差异；按本地基线等价迁移`. No phase/order/timeout/TTL/retry/detector/template/
  coordinate/fallback changes; local-kanda stays disabled.
- safety: `D:\mavenProject\DHXY` remains strict read-only; preserve both dirty/untracked repositories; zero Git
  mutation and zero runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G STAGE5-CANONICAL-CLAIM OWNER-FERMAT ACK-PARENT-STAGE5-TASK-OWNED-CONSEQUENCES SOURCE-ACTIVE TASK-CONSEQUENCES-ONLY STAGE6-CLOSED 2026-07-23T06:15:00-04:00 -->

## STAGE 5 CANONICAL WHOLE-STAGE SOURCE+TEST DELIVERY - 2026-07-23T06:46:00-04:00

- owner: `External-A / Fermat`.
- state: `WHOLE-STAGE SOURCE+TEST DELIVERED / AWAITING PARENT REVIEW / P0-P1-P2 NOT SELF-APPROVED`.
- scope delivered:
  - `CloudWholeTaskObserver` now only publishes the exact wake/fact for the migrated Stage 5 paths. It no longer
    opens the Wubei target-map gate, consumes transfer/world pending slots, records route-learning success, or
    clears the Xiuluo prepared action.
  - Wubei and Xiuluo consume exact terminal wakes under their existing held task turn, recheck exact
    run/window/hwnd/sequence/intent, preserve transfer-before-world, and confirm a ready sequence only after both
    pending slots are confirmed. `BUSY`/uncertain/stale/malformed remains retryable or fail closed.
  - Wubei target-map gate/dialog-interest update executes after authoritative task-owned reread with the frozen
    operation/source/map condition.
  - Xiuluo local-kanda click remains only an exact fact; task-owned consumption exact-clears the matching prepared
    action and waits for real `IN_COMBAT`.
  - FiveRing `SYNC_TASK_PANEL` and ready-priority outside-turn paths reenter the existing task turn only around the
    exact prepared tracker reread/CAS/supplemental click. PREPARE/HANDOVER/POST_COMBAT held-turn callers use the
    direct held implementation; no nested queue-in-turn wrapper was added.

### Exact Write Set

- production:
  - `src/main/java/com/bot/dhxy/service/MemoryService.java`
    SHA256 `67262E771160FB1D01866D5C988F2236ED4CB2B1800196EBF16D93C516670093`,
    mtime `2026-07-23T06:18:44.552-04:00`.
  - `src/main/java/com/bot/dhxy/service/dialog/CloudDialogPreparedActionState.java`
    SHA256 `470B6C73F2942B9580F388D74C3EA75179363C4C4F20CB95402E790A948F1B99`,
    mtime `2026-07-23T06:18:44.554-04:00`.
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
    SHA256 `150411BFA2F5B639E5E6D6222440CFA274A58391A62CBF7C96B9355CA29B67DA`,
    mtime `2026-07-23T06:18:44.555-04:00`.
  - `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
    SHA256 `623F0051BE06BF016A4AC89BA38CC6C737E700FA92FF78E676F023994939E464`,
    mtime `2026-07-23T06:33:20.782-04:00`.
  - `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
    SHA256 `8A0408F90A0D74CDEA328A07D5EACB4CBA23E54FBF060A69A3FA71CBE15E5E0C`,
    mtime `2026-07-23T06:33:20.783-04:00`.
  - `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
    SHA256 `7AB2E0B6D9E5EFA43C9D226B259FAFFBBD697AA537200DD1E664E051FA1692F1`,
    mtime `2026-07-23T06:24:15.742-04:00`.
- tests:
  - `CloudWholeTaskObserverPolicyContractTest.java`
    `D64E1A38CD5149C3DABE899D3EDAF04FA2E481994BCFCD21B247013105B2C8D5`.
  - `CloudWholeTaskObserverProductionHarnessTest.java`
    `2C8797752F45AE26D678AFAD20DE152949F6A71D7193A3A16375FBEAA5D7A66B`.
  - `CloudTurnTaskRuntimeContractTest.java`
    `6C68E3DE7081AFA22FA3D870391AA55D38A48D3CEAC5DF2EDB0B76F9308C32A0`.
  - `CloudTurnTaskFactoryAllowlistTest.java`
    `3FD14CD9B85E26719FAD8F677F7F4714740F05971E8D8F50D3A90E8DCF245C13`.
  - `WubeiWholeTaskTurnContractTest.java`
    `581BF6FD45E162DE15B3419AEC8AF0051ECEDB0BB7EBF47FDAF9956B8428D726`.
  - `FiveRingWholeTaskTurnContractTest.java`
    `F141A0C83AC8DF134F28F304D40652F3448E0D5BE9089F4FFAF48356DFB752C2`.
  - `Stage5TaskOwnedConsequencesContractTest.java`
    `D41504B9A187011622690666D92EEB17528428CEAE30BB8D790C3DA049CBF45E`,
    mtime `2026-07-23T06:40:34.996-04:00`.

### Deterministic Evidence

- Stage 5 exact suite `8/8`: held-turn gate/settlement/clear/action, exact identity rejection,
  transfer-before-world, first-transfer BUSY retry, world uncertain retry without duplicate transfer memory,
  confirmed exactly-once, exact local-kanda CAS, minimal FiveRing outside-turn handover, and structural assertions
  for all seven frozen FiveRing turn contracts.
- Observer policy/production is included and remains `10/10 + 16/16`; held task turn can receive the wake while
  Observer performs zero migrated consequence.
- full exact command:
  `mvn -q -DskipTests=false "-Dtest=CloudObservationContractTest,FastExpectedExitObservationContractTest,
  BattleRadarTurnContractTest,CloudWholeTaskObserverPolicyContractTest,
  CloudWholeTaskObserverProductionHarnessTest,CloudFastExpectedCombatExitArmLifecycleTest,
  FastExpectedExitGateTest,Stage5TaskOwnedConsequencesContractTest" test`
  => `68/68`, failures/errors/skips `0/0/0`.
- exact consumers:
  `WubeiWholeTaskTurnContractTest#combatEntryCleanupRetriesBusyThenExecutesOnceInsideTheTaskTurn` and
  `XiuluoWholeTaskTurnContractTest#combatEntryCleanupRetriesUncertainThenExecutesOnceForExactRun`
  => `2/2`; supplied bounded tracker replay => `1/1`.
- Client `WindowObservationRunnerContractTest` => `8/8`; Client and Cloud
  `mvn -q -DskipTests=false compile` => exit `0`.
- Observer migrated-consequence scan:
  `openTargetMapGateAndUpdateDialogInterest|consumePendingTransferChoice|consumePendingRouteOutcome|
  settlePendingTransferChoice|settlePendingWorldMapRouteOutcome|preparedActionState.clear|clearIfMatches`
  in `CloudWholeTaskObserver.java` => `0` matches.
- task-owned source map:
  - Wubei gate/settlement: `WubeiTask.java:660-721`;
  - Xiuluo settlement: `XiuluoTaskV2.java:656-706`; exact prepared clear: `XiuluoTaskV2.java:2026`;
  - FiveRing minimal handover: `FiveRingTaskV2.java:3093-3130`;
  - settlement facade: `MemoryService.java:96-143`; exact CAS: `CloudDialogPreparedActionState.java:287-317`.

### Declared Broad Drift And Safety

- non-gating broad audit of `FiveRingWholeTaskTurnContractTest` plus
  `FiveRingCombatRecoveryCleanupContractTest`: `27` tests, `4` failures / `3` errors. All are declared old fixture
  drift: null collaborators, unsupported old continuation port, and old dual-prefix/command-count expectations.
  No production behavior or old assertion was changed to manufacture green.
- business: `无已批准业务差异；按本地基线等价迁移`. Stage 3 cleanup, detector/template/coordinate/fallback,
  timeout/TTL/retry and local-kanda disabled state remain unchanged.
- no second store/queue/lock/lease/worker/protocol; zero Git mutation and zero runtime/UI/live capture/input.
  `D:\mavenProject\DHXY` remained strict read-only. Stage 6 remains closed.

<!-- TRUE_EOF: TURN-40G STAGE5 WHOLE-STAGE SOURCE-TEST-DELIVERED OWNER-FERMAT AWAITING-PARENT-REVIEW P0-P1-P2-NOT-SELF-APPROVED EXACT-68OF68 CONSUMERS-2OF2 TRACKER-1OF1 CLIENT-8OF8 DUAL-COMPILE OBSERVER-MIGRATED-CONSEQUENCE-ZERO-CALL FIVE-RING-SEVEN-CONTRACTS STAGE6-CLOSED 2026-07-23T06:46:00-04:00 -->

## PARENT STAGE 5 FINAL SOURCE REVIEW #24 - PASSED - 2026-07-23

- verdict: `P0/P1/P2=0/0/0 / STAGE 5 PASSED / OWNER RELEASED FOR STAGE / STAGE 6 OPEN`.
- source review:
  - delivered SHA/mtime inventory matches disk exactly;
  - Wubei/Xiuluo exact terminal wake fences include task type, taskRun, window, HWND, sequence, terminal state and
    intent identity; `BUSY`/uncertain leaves the sequence unconfirmed, transfer remains before world, and a confirmed
    sequence cannot execute again;
  - Wubei target-map gate uses the frozen authoritative map condition, task code, operation and source under the
    existing held task turn;
  - Xiuluo local-kanda only records/consumes an exact click fact; replacement-safe CAS clear happens in the owning
    task and the click remains pending until real `IN_COMBAT`;
  - FiveRing outside-turn paths use one minimal task-turn handover, while already-held PREPARE/HANDOVER/POST_COMBAT
    paths call the held implementation directly and cannot nest;
  - `CloudWholeTaskObserver` has zero migrated gate/settlement/prepared-clear consequence references.
- independent verification: Cloud exact `68/68`, consumers `2/2`, supplied tracker replay `1/1`, Client observation
  `8/8`, and Client/Cloud compile exit `0`. The declared broad FiveRing old-fixture drift remains visible and was
  not used as acceptance evidence.

## PARENT STAGE 6 IMPLEMENTATION CONTRACT - REMOVE OBSERVER TURN/COMMAND WRAPPERS - 2026-07-23

- owner: `External-A / Fermat`; ACK required: `PARENT-STAGE6-ZERO-TURN-ZERO-COMMAND-OBSERVER`.
- state: `STAGE 6 OPEN / FINAL SOURCE REVIEW REQUIRED`.
- required:
  - remove `observerTurn`, `runPathingProbe`, `runParkedProbe` and construction/injection paths that exist only for
    Observer task-turn acquisition;
  - remove every `CloudWholeTaskObserver` reference to `CloudTaskTurnCoordination`, `TurnGameClient`,
    `CloudWholeTaskRuntimeLocalServiceClient` and direct command/local-service execution;
  - replace remaining pathing/runtime reads only with the existing uploaded observation/inbox immutable facts and
    existing fact stores; no second runner, store, queue, lease, event bus or protocol;
  - preserve exact lifecycle, run/window/HWND/revision/sequence/attempt-round-intent fences, pathing/combat/timer/
    dialog/tracker preparation, fast expected-exit behavior, Wubei/Xiuluo/FiveRing task-owned consumers and all
    detector/template/coordinate/timeout/retry/fallback semantics.
- final gate: full-file forbidden scan zero, exact Stage 1-5 families and task consumers green, shared protocol
  byte-identical, both compiles exit `0`, and parent `P0/P1/P2=0/0/0`. No runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW24 STAGE5 PASSED P0-0-P1-0-P2-0 STAGE6-OPEN ACK-PARENT-STAGE6-ZERO-TURN-ZERO-COMMAND-OBSERVER 2026-07-23 -->

## STAGE 6 CANONICAL CLAIM - 2026-07-23T06:55:00-04:00

- owner: `External-A / Fermat`.
- ACK: `PARENT-STAGE6-ZERO-TURN-ZERO-COMMAND-OBSERVER`.
- state: `SOURCE_ACTIVE / FINAL ZERO-TURN ZERO-COMMAND OBSERVER`.
- scope accepted: remove only Observer task-turn wrappers and command/runtime-local-service dependencies; retain
  existing uploaded observation/inbox immutable facts and existing stores for every remaining read.
- frozen: no business/detector/template/coordinate/timeout/retry/fallback change; no second
  runner/store/queue/lease/event bus/protocol; no runtime/UI/live capture/input; `D:\mavenProject\DHXY` read-only.

<!-- TRUE_EOF: TURN-40G STAGE6-CANONICAL-CLAIM OWNER-FERMAT ACK-PARENT-STAGE6-ZERO-TURN-ZERO-COMMAND-OBSERVER SOURCE-ACTIVE FINAL-ZERO-TURN-ZERO-COMMAND-OBSERVER 2026-07-23T06:55:00-04:00 -->

## PARENT STAGE 6 SOURCE REVIEW FINDING #25 - P1 REPAIR REQUIRED - 2026-07-23

- severity: `P0/P1/P2=0/1/0`; owner remains `External-A / Fermat`; state:
  `SOURCE_ACTIVE / REPAIR REQUIRED / NOT DELIVERED`.
- evidence:
  - Client `WindowObservationSampler.sampleCurrentPathingFact()` returns the current typed pathing fact on every
    observation collection, not only on an intent edge.
  - `WindowObservationRunner.sendOnce()` places that fact and the due coordinate ROI into the same
    `ObservationRequest`; both therefore carry the same request `observerSeq`.
  - current `CloudWholeTaskObserver.probeTypedPathing()` rejects
    `position.observerSeq() <= view.factObserverSeq()`. Equality is the normal production pairing, so every
    coordinate sample can be discarded and `ARRIVED` / `STOPPED_AWAY` can never be classified or published.
- required repair:
  - accept a coordinate ROI paired with the current fact from the same observer sequence;
  - continue rejecting a genuinely older ROI and every ROI belonging to a replaced/old intent;
  - add deterministic production-harness coverage for same-sequence fact+ROI progression, older ROI rejection,
    and replacement followed by old-ROI rejection.
- frozen: no second store/queue/protocol, no business threshold/timing/detector/fallback change, and no
  runtime/UI/live capture/input.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW25 STAGE6 P1-REPAIR-REQUIRED SAME-SEQUENCE-PATHING-FACT-ROI-MUST-PROGRESS OLDER-AND-REPLACED-ROI-MUST-FAIL-CLOSED OWNER-FERMAT 2026-07-23T06:56:05-04:00 -->

## STAGE 6 CANONICAL WHOLE-STAGE SOURCE+TEST RE-DELIVERY - 2026-07-23T07:04:00-04:00

- owner: `External-A / Fermat`.
- state: `WHOLE-STAGE SOURCE+TEST DELIVERED / AWAITING PARENT FINAL REVIEW`.
- review state: `P0/P1/P2 NOT SELF-APPROVED`; parent Review #25 P1 repair included.

### Production And Test Write Set

- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
  - SHA-256 `b4f166571eec3e1a10a72932470d5d3fd81090f7add998c0dd0150df9f9e2e26`
  - mtime `2026-07-23T07:00:21.3873039-04:00`; bytes `72784`.
  - removed Observer-only turn/command/local-service wrappers and the obsolete dialog command probe;
    remaining pathing/position/dialog/tracker/combat reads use uploaded observation facts/ROIs and existing stores.
  - Review #25: a coordinate ROI is rejected only when its sequence is genuinely older than the current
    intent fence; the first same-sequence sample is accepted once, while replay and replacement-old samples
    remain rejected.
- Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverProductionHarnessTest.java`
  - SHA-256 `4a53fb1d86b5b7ba42e0bbce3083197f247eb815836a0fc7c9ec20611f03ee05`
  - mtime `2026-07-23T07:01:15.0852124-04:00`; bytes `74217`.
  - deterministic coverage: same-sequence fact+ROI progresses, older ROI is ignored, replacement rejects
    the old ROI and accepts the replacement's same-sequence ROI exactly once; all command-port counters remain zero.

### Exact Validation

- Cloud:
  - `mvn -q "-Dtest=CloudObservationContractTest,FastExpectedExitObservationContractTest,BattleRadarTurnContractTest,CloudWholeTaskObserverPolicyContractTest,CloudWholeTaskObserverProductionHarnessTest,CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,Stage5TaskOwnedConsequencesContractTest" test`
    => exit `0`, `69/69` (`12+4+12+10+17+4+2+8`).
  - `mvn -q "-Dtest=WubeiWholeTaskTurnContractTest#combatEntryCleanupRetriesBusyThenExecutesOnceInsideTheTaskTurn,XiuluoWholeTaskTurnContractTest#combatEntryCleanupRetriesUncertainThenExecutesOnceForExactRun,TaskTrackerPanelTurnContractTest#suppliedBoundedTrackerFrameKeepsLegacyDetectionClickValidationAndFingerprint" test`
    => exit `0`, `3/3`.
  - `mvn -q -DskipTests=false compile` => exit `0`.
- Client:
  - `mvn -q "-Dtest=WindowObservationRunnerContractTest" test` => exit `0`, `8/8`.
  - `mvn -q -DskipTests=false compile` => exit `0`.
- symmetric protocol: all `16` Java files under
  `com/bot/dhxy/cloud/turn/protocol/observation` are SHA-identical between Client and Cloud (`16/16`,
  mismatches `0`).
- full-file forbidden scan on `CloudWholeTaskObserver.java` for
  `observerTurn|runPathingProbe|runParkedProbe|CloudTaskTurnAssembly|CloudTaskTurnCoordination|TurnGameClient|
  CloudWholeTaskRuntimeLocalServiceClient|getTurnGameClient|detectDialogTypeNoFocus|localService|execute(`
  => `0` matches.

### Contract Result

- no second runner/store/queue/lease/event bus/protocol; no task-turn reacquisition and no direct
  command/runtime-local-service execution in Observer.
- exact run/window/HWND/revision/sequence/attempt-round-intent fences, Stage 1-5 behavior, fast expected-exit,
  detector/template/coordinate thresholds, timeout/retry/fallback order and task-owned consequences remain unchanged.
- zero Git mutation and zero runtime/UI/live capture/input. `D:\mavenProject\DHXY` remained strict read-only.
- owner retained only for parent final source review.

<!-- TRUE_EOF: TURN-40G STAGE6 WHOLE-STAGE SOURCE-TEST-REDELIVERED OWNER-FERMAT REVIEW25-P1-REPAIRED AWAITING-PARENT-FINAL-REVIEW P0-P1-P2-NOT-SELF-APPROVED CLOUD-69OF69 CONSUMERS-3OF3 CLIENT-8OF8 DUAL-COMPILE DTO-16OF16 FORBIDDEN-ZERO 2026-07-23T07:04:00-04:00 -->

## STAGE 6 REVIEW #26 CANONICAL WHOLE-STAGE SOURCE+TEST RE-DELIVERY - 2026-07-23T07:14:00-04:00

- owner: `External-A / Fermat`.
- state: `WHOLE-STAGE SOURCE+TEST RE-DELIVERED / AWAITING PARENT FINAL REVIEW`.
- review state: `P0/P1/P2 NOT SELF-APPROVED`; ACK parent Review #26 repeated-CURRENT mirror finding.

### Review #26 Repair

- first added and ran
  `CloudWholeTaskObserverProductionHarnessTest#repeatedSameIntentCurrentFactCannotResetCloudObservedStationaryClock`
  against the uncorrected source: `1` test, `1` failure; expected `STOPPED_AWAY`, actual `ACTIVE`.
- `CloudNavigationPathingState` now marks an existing mirror value only after
  `updateObserved()` has committed a Cloud coordinate classification. A later same-run/same-intent `CURRENT`
  fact merges wire-authoritative intent metadata and dialog-blocking fields into that mirror without replacing
  Cloud-observed map/X/Y, movement/location clocks, terminal state, probe state or non-wire UI-cleanup state.
- a new intent, `REPLACED`, `CLEARED`, or a same-intent fact before any Cloud observation still takes the complete
  typed fact. Existing CAS, sequence and retained-cleared-intent lineage remain unchanged.
- deterministic sequence now proves: stale Client initial coordinates at seq `N+1/N+2/N+3` cannot reset the
  stationary clock; unchanged ROI crosses `2.2s` and emits one `STOPPED_AWAY`; replay emits no duplicate.
  The same test proves target-map upgrade and dialog blocker update survive, and UI cleanup is retained.

### Exact Write Set

- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
  - SHA-256 `b4f166571eec3e1a10a72932470d5d3fd81090f7add998c0dd0150df9f9e2e26`;
    mtime `2026-07-23T07:00:21.3873039-04:00`; bytes `72784`.
- Cloud `src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java`
  - SHA-256 `b4e62b9035ba194b908e358243be09cb7f064b858f87ec66ed74a0652c380b37`;
    mtime `2026-07-23T07:11:10.8167890-04:00`; bytes `18817`.
- Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserverProductionHarnessTest.java`
  - SHA-256 `49c4b5c78c5ada1a1605441d7840b22eefb6ec819e701f0c0aa0c7da0d2daded`;
    mtime `2026-07-23T07:11:32.0580593-04:00`; bytes `79600`.

### Exact Validation

- Cloud:
  - `mvn -q "-Dtest=CloudObservationContractTest,FastExpectedExitObservationContractTest,BattleRadarTurnContractTest,CloudWholeTaskObserverPolicyContractTest,CloudWholeTaskObserverProductionHarnessTest,CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,Stage5TaskOwnedConsequencesContractTest" test`
    => exit `0`, `70/70` (`12+4+12+10+18+4+2+8`).
  - `mvn -q "-Dtest=WubeiWholeTaskTurnContractTest#combatEntryCleanupRetriesBusyThenExecutesOnceInsideTheTaskTurn,XiuluoWholeTaskTurnContractTest#combatEntryCleanupRetriesUncertainThenExecutesOnceForExactRun,TaskTrackerPanelTurnContractTest#suppliedBoundedTrackerFrameKeepsLegacyDetectionClickValidationAndFingerprint" test`
    => exit `0`, `3/3`.
  - `mvn -q -DskipTests=false compile` => exit `0`.
- Client:
  - `mvn -q "-Dtest=WindowObservationRunnerContractTest" test` => exit `0`, `8/8`.
  - `mvn -q -DskipTests=false compile` => exit `0`.
- shared observation protocol remains byte-identical: `16/16`, mismatches `0`.
- Stage 6 Observer forbidden-reference scan remains `0`.

### Safety And Result

- no threshold, classification, detector, retry, fallback, command, protocol or task business change.
- no second store/queue/lock/lease/runner; the existing `CloudNavigationPathingState` mirror CAS owns the flag
  and merge. Replacement/clear and Review #25 same-sequence/old-ROI fences remain green.
- zero Git mutation and zero runtime/UI/live capture/input. `D:\mavenProject\DHXY` remained strict read-only.
- owner retained only for parent final source review.

<!-- TRUE_EOF: TURN-40G STAGE6 REVIEW26 WHOLE-STAGE SOURCE-TEST-REDELIVERED OWNER-FERMAT REPEATED-CURRENT-MIRROR-REPAIRED AWAITING-PARENT-FINAL-REVIEW P0-P1-P2-NOT-SELF-APPROVED CLOUD-70OF70 CONSUMERS-3OF3 CLIENT-8OF8 DUAL-COMPILE DTO-16OF16 FORBIDDEN-ZERO 2026-07-23T07:14:00-04:00 -->

## PARENT STAGE 6 FINAL SOURCE REVIEW FINDING #26 - P1 REPAIR REQUIRED - 2026-07-23

- severity: `P0/P1/P2=0/1/0`; owner remains `External-A / Fermat`; the `07:04` re-delivery is not accepted.
- production evidence:
  - Client `WindowObservationSampler.sampleCurrentPathingFact()` emits the current
    `WindowRuntimeContext.pathingSnapshot` on every request.
  - Client `markPathingStarted()` seeds the snapshot once, while Cloud
    `CloudNavigationPathingState.updateObserved()` does not write the observed coordinates back to Client;
    Client `updatePathingSnapshot()` has no production caller.
  - on the next sequence, `CloudNavigationPathingState.acceptObservationFact()` currently replaces the Cloud
    observed snapshot with the repeated Client snapshot. `probeTypedPathing()` can therefore compare every
    unchanged real coordinate against the same initial coordinate, report `changed=true` on every cycle, refresh
    movement/stationary timing forever and make `STOPPED_AWAY` unreachable.
- required repair:
  - add a deterministic production sequence: seq N fact + ROI establishes an observed coordinate; seq N+1 repeats
    the same-intent stale Client snapshot while the ROI coordinate is unchanged; after the frozen 2.2-second
    interval the exact intent must publish `STOPPED_AWAY` once;
  - preserve Cloud-observed coordinate/timing across repeated same-intent `CURRENT` facts while still accepting
    legitimate target-map intent upgrade, dialog-blocker/UI-cleanup metadata, `REPLACED`, and `CLEARED`;
  - retain Review #25 same-sequence acceptance and old/replaced ROI rejection.
- frozen: no threshold change, second store/protocol, business fallback change, runtime/UI/live capture/input, or
  write to `D:\mavenProject\DHXY`.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW26 STAGE6 P1-REPAIR-REQUIRED REPEATED-CURRENT-FACT-MUST-NOT-RESET-CLOUD-OBSERVED-STATIONARY-TIMING STOPPED-AWAY-MUST-REMAIN-REACHABLE OWNER-FERMAT 2026-07-23T07:07:37-04:00 -->

## STAGE 6 REVIEW #26 CANONICAL WHOLE-STAGE RE-DELIVERY AT PHYSICAL EOF - 2026-07-23T07:15:00-04:00

- owner/state: `External-A / Fermat`; `SOURCE+TEST RE-DELIVERED / AWAITING PARENT FINAL REVIEW`;
  `P0/P1/P2 NOT SELF-APPROVED`.
- repair: the existing `CloudNavigationPathingState` mirror now distinguishes a committed Cloud observation.
  Newer same-run/same-intent `CURRENT` facts retain Cloud position/state/timing and non-wire UI state while
  applying the fact's intent/target-map and dialog-blocker fields. New intent, `REPLACED`, `CLEARED`, and
  pre-observation CURRENT retain complete typed-fact semantics.
- red/green: the new repeated-CURRENT production sequence first failed
  `expected STOPPED_AWAY / actual ACTIVE`; after repair it crosses the unchanged `2.2s` threshold, publishes
  exactly one `STOPPED_AWAY`, preserves target-map/dialog/UI metadata, and does not duplicate on replay.
- exact files:
  - `CloudWholeTaskObserver.java`: SHA-256
    `b4f166571eec3e1a10a72932470d5d3fd81090f7add998c0dd0150df9f9e2e26`;
    mtime `2026-07-23T07:00:21.3873039-04:00`;
  - `CloudNavigationPathingState.java`: SHA-256
    `b4e62b9035ba194b908e358243be09cb7f064b858f87ec66ed74a0652c380b37`;
    mtime `2026-07-23T07:11:10.8167890-04:00`;
  - `CloudWholeTaskObserverProductionHarnessTest.java`: SHA-256
    `49c4b5c78c5ada1a1605441d7840b22eefb6ec819e701f0c0aa0c7da0d2daded`;
    mtime `2026-07-23T07:11:32.0580593-04:00`.
- exact commands/results:
  - Cloud Stage 1-6 named families command recorded above => `70/70`, exit `0`;
  - exact Wubei/Xiuluo cleanup plus supplied tracker replay command recorded above => `3/3`, exit `0`;
  - Cloud and Client `mvn -q -DskipTests=false compile` => both exit `0`;
  - Client `mvn -q "-Dtest=WindowObservationRunnerContractTest" test` => `8/8`, exit `0`;
  - shared observation DTO SHA comparison => `16/16`, mismatch `0`;
  - Observer forbidden-reference scan => `0`.
- safety: no second store/protocol/queue/lock/lease/runner; no threshold/business/fallback change; zero
  Git/runtime/UI/live capture/input; `D:\mavenProject\DHXY` remained strict read-only.

<!-- TRUE_EOF: TURN-40G STAGE6 REVIEW26 CANONICAL-WHOLE-STAGE-REDELIVERY PHYSICAL-EOF REPEATED-CURRENT-MIRROR-REPAIRED AWAITING-PARENT-FINAL-REVIEW P0-P1-P2-NOT-SELF-APPROVED CLOUD-70OF70 CONSUMERS-3OF3 CLIENT-8OF8 DUAL-COMPILE DTO-16OF16 FORBIDDEN-ZERO 2026-07-23T07:15:00-04:00 -->

## PARENT STAGE 6 FINAL SOURCE+TEST REVIEW #27 - PASSED - 2026-07-23T07:18:43-04:00

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`.
- parent-reviewed source:
  - `CloudWholeTaskObserver.java`
    `b4f166571eec3e1a10a72932470d5d3fd81090f7add998c0dd0150df9f9e2e26`;
  - `CloudNavigationPathingState.java`
    `b4e62b9035ba194b908e358243be09cb7f064b858f87ec66ed74a0652c380b37`;
  - `CloudWholeTaskObserverProductionHarnessTest.java`
    `49c4b5c78c5ada1a1605441d7840b22eefb6ec819e701f0c0aa0c7da0d2daded`.
- Review #25 closure: one request's typed fact and coordinate ROI may share the same `observerSeq`; that pair
  progresses once, while older sequence and replaced-intent ROI remain fail-closed.
- Review #26 closure: after a Cloud observation commits coordinates/timing, newer same-run/same-intent
  `CURRENT` facts cannot reset those observations to the Client's pathing-start snapshot. Target-map and dialog
  metadata still update; replacement and clear retain full typed-fact semantics. The deterministic production
  sequence reaches one `STOPPED_AWAY` after an unchanged `2.2s` interval and cannot duplicate on replay.
- parent independent verification:
  - Cloud Stage 1-6 named families: `70/70`, exit `0`;
  - exact Wubei/Xiuluo consumers plus tracker replay: `3/3`, exit `0`;
  - Client observation runner: `8/8`, exit `0`;
  - Client and Cloud compile: exit `0`;
  - shared observation DTO: `16/16`, mismatches `0`;
  - Observer forbidden-reference scan: `0`.
- safety: no second store/protocol/queue/lock/lease/runner; no threshold, phase, detector, retry, fallback,
  command or local-kanda enablement change. No Git mutation and no runtime/UI/live capture/input.
  `D:\mavenProject\DHXY` remained strict read-only.
- acceptance boundary: TURN-40G Stage 1-6 source/test contract is closed. Fresh deployed runtime verification
  remains a user-run gate and is not claimed by this review.

<!-- TRUE_EOF: TURN-40G PARENT-REVIEW27 STAGE6 FINAL-SOURCE-TEST-PASSED P0-0-P1-0-P2-0 OWNER-RELEASED CLOUD-70OF70 CONSUMERS-3OF3 CLIENT-8OF8 DUAL-COMPILE DTO-16OF16 FORBIDDEN-ZERO FRESH-RUNTIME-REQUIRED 2026-07-23T07:18:43-04:00 -->

## USER-APPROVED DIALOG-DEMAND AMENDMENT - WHOLE-CARD SOURCE+TEST DELIVERY - 2026-07-23T11:01:31-04:00

- state: `SOURCE+TEST DELIVERED / AWAITING PARENT SOURCE+TEST SOURCE REVIEW / FRESH RUNTIME PENDING`;
  this post-Review #27 amendment is not self-approved.
- Client:
  - `application.properties`: production `bot.xiuluo.local-kanda-enabled=true`;
  - `WindowObservationSampler.collectBound(...)`: local kanda first refusal before dialog fact/ROI;
    ordinary probe-only miss has no event, demand or business side effect; exact click claim suppresses the
    same request's stale dialog ROI;
  - `WindowRuntimeContext.getXiuluoKandaProbeView()`: exact attempt claim joins the existing atomic kanda view;
  - `ObservationDialogInterestFact.enterBattleClaimed` and validator are symmetric in both repositories.
- Cloud:
  - `CloudWholeTaskObserver.publishObservationInterests(...)` no longer statically publishes
    `xiuluo-dialog 529x208 @2s`;
  - `recordXiuluoStopStaticTerminal(...)` anchors first tracker `ARRIVED/STOPPED_AWAY` to the Client
    capture/terminal timestamp; `syncDialogRoiInterest(...)` applies the fixed `3000ms` exact-identity demand
    union and revokes on `IN_COMBAT`, local claim/click, clear or replacement;
  - `probeAttention(...)` ignores probe-only frames; explicit non-probe confirmation remains immediately
    eligible; stopped-static requires both observer sequence and captured timestamp to be strictly newer than
    the fallback demand.
- frozen: kanda2 asset, ROI `(264,376) 41x21`, threshold `0.82`, retry counts, phase ordering and same-frame
  dedupe. No sleep, second protocol/store/runner/queue, runtime/UI/live capture/input or write to
  `D:\mavenProject\DHXY`.
- exact verification:
  - Client named family
    `WindowObservationKandaContractTest,WindowObservationRunnerContractTest,HttpsObservationClientRoundTripContractTest,WindowTurnLoopObservationContractTest`
    => `27/27`, exit `0`; Client compile exit `0`;
  - Cloud named family
    `CloudWholeTaskObserverPolicyContractTest,CloudWholeTaskObserverProductionHarnessTest,CloudObservationContractTest`
    => `45/45`, exit `0`; Cloud compile exit `0`;
  - shared fact/validator SHA-256 are byte-identical:
    `80D477917ED2F82E01E62BD9FD48BCD5019DE46C6CAFA7F3C82FA4A097B86A20` and
    `857210FDCE6C10B41031CF799E34870CA22C889E49DA3E612A2606644BDD0F82`;
  - Client and Cloud `git diff --check` => exit `0`.
- residual gate: parent must review this exact amendment and user must run fresh deployed runtime before it can
  be called accepted.

<!-- TRUE_EOF: TURN-40G USER-APPROVED-DIALOG-DEMAND-AMENDMENT WHOLE-CARD-SOURCE-TEST-DELIVERED AWAITING-PARENT-SOURCE-REVIEW LOCAL-KANDA-FIRST CLOUD-ROI-ON-DEMAND CLIENT-27OF27 CLOUD-45OF45 DUAL-COMPILE DTO-SYMMETRIC DIFF-CHECK-ZERO FRESH-RUNTIME-PENDING 2026-07-23T11:01:31-04:00 -->

## PARENT P1 REPAIR - FAST EXIT SAME-MILLISECOND FENCE - 2026-07-23T12:26:47-04:00

- finding: historical CR136 runtime proved that a true combat exit and the replacement expected-wait arm can share
  one millisecond after a false fast-exit is corrected back to trusted `IN_COMBAT`. The migrated Cloud source had
  regressed to `combatExitPendingAtMs <= now`, which discards that valid equal-timestamp exit.
- repair: `BattleRadarService` now treats only an invalid timestamp or
  `combatExitPendingAtMs < armedAtMs` as stale. Equality is explicitly retained because millisecond timestamps
  cannot order the two events. A deterministic boundary test covers equal, newer, older and invalid timestamps.
- frozen: local `20x20` avatar ROI, `15s/1s/0.35`, exact wait/generation, false-positive trusted correction,
  ordinary radar fallback, task phase, return-home/recovery and input behavior are unchanged.
- verification: Cloud named family
  `BattleRadarTurnContractTest,FastExpectedExitObservationContractTest,FastExpectedExitGateTest,CloudFastExpectedCombatExitArmLifecycleTest`
  passed; Cloud compile and exact-write-set `git diff --check` exited `0`.
- reviewed SHA-256: `BattleRadarService.java`
  `85144D0201C6DED0859DBD4154A296DBC310A3CDF8694D77644DF211E493F620`;
  `BattleRadarTurnContractTest.java`
  `BCE13A379E0C1D9EE6B40D053CA05AE7FA622F31DFE941DCD297B8E7C2DD8E67`.
- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST PASSED / FRESH RUNTIME REQUIRED`. Restart Cloud and verify a
  correction episode can still consume the later true exit and advance to `RETURN_HOME`.

<!-- TRUE_EOF: TURN-40G PARENT-P1-FAST-EXIT-SAME-MILLISECOND-FENCE SOURCE-TEST-PASSED P0-0-P1-0-P2-0 CLOUD-NAMED-PASS COMPILE-EXIT0 DIFF-CHECK-ZERO FRESH-RUNTIME-REQUIRED 2026-07-23T12:26:47-04:00 -->
