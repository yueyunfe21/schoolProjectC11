# DHXY 全量云端业务大脑与 Thin Client 目标架构草案

日期：2026-07-12  
状态：Draft，已完成边界定义，等待 Cloud Agent 继续讨论并收敛最终方案  
用途：后续架构讨论的唯一输入文档；当前阶段只讨论和补全文档，不实施代码

## 1. 文档目的

本项目的目标不再是继续扩大“本地业务 + 云端部分决策”的 Hybrid 形态，而是形成一个完整的云端业务大脑：

- 云端持有全部业务状态、业务规则、视觉理解、任务编排和跨窗口调度；
- 本地退化为 Thin Client，只负责绑定游戏窗口、采集原始画面、执行受限动作、报告执行事实以及实施本地安全拒绝；
- 云端通过图片和结构化事实观察本地，通过类型化动作计划控制本地；
- 当前本地程序在新体系完整建成之前继续作为生产基线，不做逐项生产切换；
- 新体系完成后，客户端与云端按一个整体版本原子切换。

本文先固定已经由用户确认的架构边界，再列出仍需 Cloud Agent 讨论的设计问题。后续讨论不得重新解释或削弱已经确认的边界，除非用户明确改变决定。

## 2. 与既有文档的关系

以下文档仍可作为当前实现、历史迁移过程和业务基线的证据：

- docs/业务逻辑.md
- docs/HYBRID_CLOUD_WORKFLOW.md
- docs/XIULUO_CLOUD_MIGRATION_PLAN.md
- docs/WUBEI_CLOUD_MIGRATION_BASELINE.md
- docs/superpowers/specs/2026-07-06-cloud-vision-business-migration-design.md

但其中“长期保留本地任务 phase 状态机”“云端失败后回退本地业务”“按单个 Service 逐项投入生产”等内容，不再代表本文定义的目标架构。它们只描述当前运行基线或历史迁移阶段。

在原子切换发生之前，任何迁移实现仍必须保持 docs/业务逻辑.md 中已确认的五倍、修罗业务语义，不得借架构迁移改变 phase 顺序、重试与 fallback 顺序、窗口放权边界、判断条件、点击顺序或时效语义。

## 3. 已确认的总体边界

本节已经完成用户确认。后续 Agent 应把这些内容视为硬约束，而不是待选方案。

### 3.1 唯一业务大脑

云端是每个窗口、每次任务运行的唯一权威业务状态持有者。云端负责：

- 任务类型、任务 phase、任务进度和完成条件；
- 下一步动作、重试、fallback、恢复和失败归因；
- 五倍、修罗、五环、自动战斗、维护、队伍和导航等全部业务 Service；
- OCR、模板使用策略、图像预处理、候选排序和视觉结果解释；
- NPC、路线、Dialog、Tracker、地图、任务和 Vision Memory；
- 多窗口 task turn、窗口优先级、keep-turn、park、yield 和释放时机；
- 用户业务配置、阈值、资产版本和策略版本。

本地不保留第二套业务状态机，也不在云端超时或失败时自行选择下一 phase、fallback 或另一套本地业务路径。

### 3.2 本地 Thin Client 责任

本地只保留必须靠用户设备完成的能力：

- 用户登录和设备身份；
- JavaFX 桌面界面；
- 游戏窗口发现、注册、HWND 绑定和窗口身份核验；
- 原始窗口截图以及按云端 CaptureSpec 做窗口相对 ROI 裁剪；
- 键盘、鼠标、窗口聚焦和输入队列；
- 类型化动作计划的整批校验、原子执行和结果上报；
- 通用 ObservationPlan 的机械执行和事实回报；
- 受限的通用 MATCH_AND_CLICK / MATCH_AND_REPORT 原语；
- pause、stop、紧急停止以及本地安全拒绝；
- 最近一次动作账本、连接状态和可丢弃的签名资产缓存；
- 云端连接中断后的 CLOUD_SUSPENDED 安全状态。

### 3.3 本地安全门只有拒绝权

本地 LocalSafetyGate 可以因以下原因拒绝云端动作：

- 用户已 pause、stop 或触发紧急停止；
- 动作过期、重复、序号回退或签名无效；
- windowId、HWND、设备、会话或任务运行身份不匹配；
- 坐标越界、ROI 越界、非法按键或动作类型不在允许列表；
- 当前输入 lane 无法安全取得；
- 依赖的 frame、模板或资产 hash 不匹配。

本地拒绝后只报告结构化拒绝事实，不得自行改写坐标、扩大 ROI、降低阈值、替换模板、改走 fallback、选择下一 phase 或重启业务流程。

### 3.4 云端 Service 不重新拆业务边界

目标云端保持当前项目已经形成的业务 Service 边界，整体迁入同一个 Cloud Brain 应用。第一版不重新拆成一组业务微服务。

- 当前有什么业务 Service，云端就保留相应的同名或等价 Service；
- Service 之间在同一云端应用内使用强类型方法调用；
- OCR/OpenCV 可使用有界 worker pool，但它只是计算资源池，不是新的业务微服务边界；
- 只有物理 I/O 边界被拆开：云端业务 Service 负责决定，本地通用 executor 负责设备动作。

### 3.5 整体建设、原子切换

不采用逐个 Service 投入生产并长期维持双轨的方式。

- 所有云端业务 Service 和依赖关系一起建设；
- 建设期间当前本地程序继续作为可运行基线；
- 新体系内部可按依赖顺序开发和联调，但不能把单个新 Service 独立投入正式业务；
- 全部依赖完成后，以 THIN_CLIENT_V1 整体版本原子切换；
- 运行时不允许单个 Service 回退到本地旧业务；
- 严重 fresh runtime 问题可以部署级回滚整个客户端和服务端版本。

## 4. 目标组件边界

### 4.1 本地最终组件

| 组件 | 允许职责 | 明确禁止 |
|---|---|---|
| CloudConnection | 维持控制流、上传图片、重连、认证 | 解释业务响应或选择 fallback |
| WindowRegistry | 维护 windowId、HWND、尺寸、DPI、标题和绑定生命周期 | 判断队长、任务或地图业务状态 |
| CaptureExecutor | 截取原始完整窗口或云端指定 ROI | 洗图、OCR、颜色分类、模板策略选择 |
| ActionPlanExecutor | 校验并原子执行类型化动作 | 改写计划、插入业务动作、决定下一 phase |
| ObservationExecutor | 按计划采样并上报通用事实 | 把事实解释为战斗、到达、Dialog 或任务完成 |
| GenericMatchExecutor | 在固定 ROI 内用指定签名模板做一次匹配并点击或报告 | fallback、扩 ROI、降阈值、切换模板 |
| LocalSafetyGate | 拒绝不安全、过期、错窗或重复动作 | 修正云端动作或生成替代业务动作 |
| InputLane | 串行化真实物理输入并报告占用/释放 | 决定哪个业务窗口下一次获得 turn |
| LocalActionLedger | 保存最近 action、frame、sequence 和执行结果 | 保存完整业务 phase 或本地恢复策略 |
| SignedAssetCache | 按 hash 缓存签名模板或小型资产 | 持有可独立运行的业务模板库 |
| DesktopUI | 用户登录、设备设置、窗口注册、任务命令和状态展示 | 本地解释业务规则或直接驱动旧任务 Service |

### 4.2 云端最终组件

云端包括当前已有的任务、导航、交互、维护、队伍、视觉和记忆 Service。最终 Service 清单必须由后续 Agent 对当前源码做完整映射，确保没有漏掉仍在本地解释业务的入口。

云端共同基础能力包括：

- Session/Window/TaskRun 状态仓库；
- Task Orchestrator 与现有业务 Service 图；
- Cloud Task Turn Scheduler；
- ActionPlan/ObservationPlan 生成器；
- OCR、OpenCV 和模板处理资源池；
- 用户私有记忆、公共候选记忆和资产版本；
- 认证、授权、租户隔离和设备身份；
- 指标、trace、审计、失败证据和后台管理接口。

## 5. 控制与图片协议的已确认方向

### 5.1 双通道通信

- WebSocket：每台设备一条双向控制流，在其中多路复用多个窗口；
- HTTPS：上传较大的完整图或 ROI 图，并在控制消息中引用 frameId；
- 小 ROI 可在约定大小上限内内联到控制消息；
- WebSocket 断开不允许本地继续新的业务输入。

每条控制消息至少关联：

    tenantId
    userId
    deviceId
    clientSessionId
    windowId
    taskRunId
    sequence
    timestamp
    expiry
    messageId/actionId
    signature

### 5.2 图片事实

首次观察默认上传绑定窗口的无损原始完整图。之后云端可以发出 CaptureSpec，要求本地在指定窗口上截取窗口相对 ROI。

每张图至少包含：

    frameId
    windowId
    hwnd
    windowWidth/windowHeight
    dpiScale
    captureTime
    contentHash
    roi
    coordinateSpace
    encoding

本地只允许截图和裁剪，不允许在上传前进行洗图、OCR、颜色解释、文字识别或业务模板选择。

### 5.3 类型化 ActionPlan

云端返回受 schema 限制的低层动作计划，例如：

    FOCUS_WINDOW
    KEY_DOWN / KEY_UP / KEY_PRESS
    MOUSE_MOVE
    MOUSE_LEFT_CLICK / MOUSE_RIGHT_CLICK
    SLEEP
    CAPTURE
    MATCH_AND_CLICK
    MATCH_AND_REPORT
    REPORT_WINDOW_STATE

一个 ActionPlan 可以包含一组必须原子执行的动作。移动鼠标与点击等不可拆序列必须在同一个计划内。计划到达观察点后，本地停止继续推演，采集事实并交回云端决定下一计划。

### 5.4 受限通用匹配原语

MATCH_AND_CLICK / MATCH_AND_REPORT 可以保留在本地，以避免每个确定性小匹配都进行完整图片往返，但它们必须是无业务语义的执行原语。

云端必须下发：

    templateId/templateHash 或签名模板 payload
    windowId
    frame/capture 条件
    窗口相对 ROI
    threshold
    clickOffset
    maxAge
    result mode

本地只能执行一次并返回原始匹配事实。模板名称、ROI 和调用位置不得让本地形成可独立运行的业务流程。

### 5.5 ObservationPlan

云端可以下发通用观察计划：

    ROI
    采样频率
    最大持续时间
    像素差参数
    模板/hash
    阈值
    需要报告的原始事实

本地可以报告 pixelDiff、matched、score、windowExists、inputLaneState 等机械事实。只有云端可以把这些事实解释为 IN_COMBAT、ARRIVED、STOPPED_MOVING、DIALOG_VISIBLE、HP_LOW 或其他业务状态。

## 6. Task Turn 与输入所有权

当前实现中，TaskTurnCoordinator 的本地 fair lock 实际决定等待窗口中谁下一次获得输入权。目标架构将这个业务调度决定迁到云端。

目标规则：

1. 本地 InputLane 只保证物理输入不会并发交叉；
2. 本地报告 INPUT_LANE_FREE、当前持有者和最近 action 结果；
3. 云端根据所有窗口业务状态选择下一窗口和 ActionPlan；
4. 云端明确返回 KEEP_LEASE 或 RELEASE_LEASE；
5. 本地锁只作为竞态与安全保护，不能自行按 fair queue 选择业务上的下一个窗口；
6. 本地拒绝或断线后，云端通过 ledger 和 sequence 决定重新同步，不由本地续跑旧 phase。

## 7. 断线、暂停、停止与恢复

### 7.1 云端断线

云端不可达时：

- 本地停止接受和执行新的业务动作；
- 进入 CLOUD_SUSPENDED，不得标记为业务 FAILED；
- 保留窗口绑定、最近 action ledger、frame 引用和连接诊断；
- 后台探测连接，但不执行本地业务 fallback；
- 重连后上传完整窗口图、最后确认 sequence/action、最后执行事实和当前 input lane 状态；
- 云端决定 RESUME、RESYNC、RESTART_TASK_RUN 或结束任务。

### 7.2 用户 pause/stop

- pause/stop/紧急停止必须由本地立即生效，不等待云端；
- 本地取消或拒绝尚未执行的动作，并把取消事实上报云端；
- stop 不得被 HTTP/WebSocket 中断包装成业务失败；
- resume 后本地只上传事实，继续方式由云端决定。

## 8. 数据、资产与记忆

### 8.1 数据归属

云端权威保存：

- 任务配置和业务开关；
- 地图、路线、NPC、Dialog、Tracker、模板、ROI 和阈值；
- Action recipe 和 Observation recipe；
- Vision Memory、route memory、失败样本和版本；
- 用户私有业务配置；
- 公共候选记忆和公共已发布记忆。

本地只保存：

- 云端地址和必要连接设置；
- 登录/设备认证材料；
- 本地 UI 布局、快捷键和窗口注册信息；
- 当前会话的窗口绑定和 action ledger；
- 可删除、按 hash 校验的签名资产缓存。

### 8.2 多用户记忆规则

已经确认：

- 用户私有记忆默认隔离，但同一账号的不同设备可以共享；
- 服务端维护 trustedPublisherUserIds，可信用户的成功数据可以进入公共候选池；
- 私有记忆在一次 verifier 确认成功后即可供该用户下一次谨慎使用；
- 公共记忆在至少 3 次不同任务运行成功且近期无失败时自动发布；
- 发布不等待人工确认；
- 公共数据冲突时生成新版本参与竞争，不直接覆盖历史；
- 公共失败会降权或进入 quarantine；
- 管理员可手动编辑、降权、取消发布、隔离和回滚。

后续 Agent 需要补全统计窗口、失败衰减、版本竞争和 verifier 的精确定义，但不得把自动发布改为必须人工批准。

## 9. 多用户部署方向

当前 dhxy-cloud-brain 侧车只适合开发：本地绑定、单 token、内存 session、无生产级租户隔离与持久化。它不能直接作为多用户生产部署结论。

已确认的第一阶段生产形态：

- 一个 Cloud Brain 模块化单体应用；
- 多个用户和设备共享服务端，但按 tenant/user/device/session 严格隔离；
- Redis 保存活跃 session、窗口状态、租约、短期去重和心跳；
- PostgreSQL 保存用户、设备、配置、策略、记忆、版本和审计；
- 对象存储保存需要保留的图片、ROI、模板和资产；
- 有界 OCR/OpenCV worker pool，具备每用户配额、背压和超时；
- 后续可横向扩展 Cloud Brain 和视觉 worker 实例；
- 状态外置后，不依赖固定实例或 sticky session 才能维持业务正确性。

### 9.1 认证方向

- 用户登录或 license 换取短期访问 token；
- 设备注册并持有设备密钥；
- 连接建立后固定 tenant/user/device/clientSession 身份；
- 消息包含 sequence、actionId、expiry 和签名，防重放与错设备执行；
- 客户端不得内置所有用户共享的永久 token；
- 可信发布者资格只能由服务端配置。

## 10. 图片保留与管理能力

已确认的默认保留策略：

- 普通决策图片：临时保存，默认 30 分钟；
- 成功调用：长期只保留 hash、结果、动作和延迟等结构化摘要；
- 失败证据图片：默认 7 天；
- 公共记忆候选证据 ROI：默认 30 天；
- 已发布公共记忆：保留最小必要 crop、版本和来源，不保留完整窗口图；
- 手动 pin 的案例：保留到管理员删除。

整体迁移范围包含最小管理 API/页面，用于：

- 按用户、任务、地图、NPC、动作查询私有和公共记忆；
- 查看成功、失败、来源、版本和候选门槛进度；
- 编辑坐标、ROI、阈值和状态；
- 强制发布、取消发布、quarantine 和 rollback；
- 审计所有人工修改。

## 11. 目标数据流

    DesktopUI
        |
        | 注册窗口、启动任务
        v
    Thin Client ---- START_TASK / window facts ----> Cloud Brain
        ^                                                |
        |                                                | 读取状态、资产、记忆
        |                                                v
        |                                      Redis / PostgreSQL /
        |                                         Object Storage
        |
        +---- CaptureSpec / ActionPlan / ObservationPlan -+
        |
        +---- FrameRef / ActionOutcome / ObservationFact ->

执行原则：

1. 云端要求观察；
2. 本地绑定指定 HWND 并采集原始图；
3. 云端读取权威状态、资产和记忆后生成 ActionPlan；
4. 本地 LocalSafetyGate 整批校验；
5. 本地 InputLane 原子执行；
6. 本地上报 ActionOutcome 和原始事实；
7. 云端更新唯一权威状态并决定下一步。

## 12. 待 Cloud Agent 继续讨论的开放项

以下内容尚未最终定义。讨论可以提出 2 至 3 个实现方案并给出推荐，但不得修改第 3 至第 10 节的硬边界。

### 12.1 完整 Service 迁移矩阵

需要扫描当前 DHXY 源码，列出所有仍包含业务判断的 Service、Task、Coordinator、Reader、Recognizer 和本地配置，并为每一项给出：

- 当前源码入口；
- 当前权威状态；
- 迁移后的云端 owner；
- 本地只保留的 executor/capture 能力；
- 上下游依赖；
- 业务基线来源；
- 是否存在隐式 fallback、定时器、缓存或本地记忆；
- 迁移完成的可验证删除条件。

最终矩阵必须证明“本地没有剩余业务大脑”，不能只覆盖 OCR 或当前已经上云的 Service。

#### Q1 已关闭共识：完备性证明与矩阵粒度

Service 迁移矩阵采用“方法级底账、类级主表”的双层结构：

- 所有 production-reachable 方法先进入方法级 inventory，包括继承/default method、lambda/合成方法、事件监听器、定时任务、Spring 条件注册和 UI controller 入口；
- 类级主表用于阅读和管理，混合类必须展开方法明细，纯类也必须指向完整方法级底账，不能凭类名直接宣告纯净；
- 主表除原定字段外，增加 `production reachability evidence` 和 `target artifact disposition`；
- Java 代码、配置、Spring 注册、FXML、模板 manifest、ROI、阈值、JSON/YAML 和反射/字符串注册入口使用同一归属体系；
- 每次盘点记录 repo、branch、commit、worktree diff 和扫描时间。历史 ledger 只能作为种子，必须对冻结目标 commit 复核；
- 修罗 ledger 当前位于 `D:\mavenProject\DHXY-xiuluo\docs\XIULUO_MIGRATION_LEDGER.md`，锚定 DHXY `91d3b07` 与 cloud-brain `48e3781`，但目前仍是 untracked 文件；提交取得持久 commit 前不得作为正式冻结证据。

Q1 验收采用六项并列门禁，全部通过才可证明本地没有剩余业务大脑：

1. 方法级 inventory 全覆盖；
2. 生产入口可达闭包无未知节点；
3. 反向语义扫描零未映射命中；
4. 配置与资源零未归属；
5. Thin Client 构建产物按 allowlist 只包含第 4.1 节允许组件及必要协议类型，业务包与业务资源物理不存在；
6. 按完整业务流进行人工反向抽查，无状态、顺序、定时器、fallback 或调度断点。

### 12.2 协议 schema

需要给出可落地的消息 schema 和状态转换：

- WebSocket envelope；
- START_TASK、TASK_COMMAND、ACTION_PLAN、ACTION_OUTCOME；
- CAPTURE_SPEC、FRAME_REF、OBSERVATION_PLAN、OBSERVATION_FACT；
- lease、input lane、pause、stop、disconnect、reconnect；
- idempotency、sequence、expiry、签名和重复消息处理；
- 整批 ActionPlan 的原子性边界；
- 旧 response 到新协议的兼容与删除策略。

#### Q2 已关闭共识：控制协议、幂等与原子性

V1 控制协议采用 WebSocket JSON 消息与 HTTPS 图片旁路：

- envelope 与 typed payload 分层；连接建立时先完成 envelope/payload 版本协商；协议、连接身份和签名在解析业务 payload 前验证；
- 控制消息使用 JSON，较大图片通过 HTTPS 上传并以 frameId 引用，小 ROI 仅在协议硬上限内内联；
- 签名采用“协议定义的精确 UTF-8 字节帧 + detached signature”，不重新序列化 JSON；签名算法和验签 key 由已认证连接固定绑定，不接受未签名前缀提供的算法/key 选择；签名覆盖 connectionFence、direction、streamId、sequence、messageId、payload digest 及 frame/template hash；
- 执行型 payload 对未知 field/enum/major、重复安全字段和非法扩展 fail-closed；非执行型消息只允许忽略 schema 明确声明的 namespaced extension。

消息词汇至少包括 START_TASK、TASK_COMMAND、ACTION_PLAN、ACTION_OUTCOME、CAPTURE_SPEC、FRAME_REF、OBSERVATION_PLAN、OBSERVATION_FACT、INPUT_LANE_FREE、LOCAL_LEDGER_RESET 和 PROVISION_DETECTOR。

时序与 fencing 规则：

- sequence 作用域为 `(connectionFence, direction, streamId)`；server->client 与 client->server 独立计数；
- streamId 至少区分 device-control 与 window/task-run；
- connectionFence 由服务端原子换代、单调不可回退；新连接生效时旧 socket 立即失效；
- 同 stream 出现 gap 时停止执行并 RESYNC，不跳号；已见 sequence/actionId 即使 outcome cache 已逐出也绝不重新执行。

ActionPlan 与重放规则：

- messageId 表示投递，actionId 表示副作用，correlationId/causationId 连接 capture、plan、outcome 和 observation；
- 同一投递重试保持 messageId/actionId；只有可信 outcome 证明 `NOT_EXECUTED && physicalInputStarted=false && completedInputSteps=0` 后，云端才可创建新 actionId；
- 物理输入计划的 EXECUTED、UNKNOWN、STOPPED 一律不得自动重放；OBSERVED 只适用于无物理副作用消息；EXECUTED 只证明输入执行完成，不证明业务效果成功；
- ACTION_OUTCOME 同 actionId + 同 digest 幂等返回同 ACK，不同 digest 冲突拒绝；
- 一个有副作用的 plan 只允许一个有界、扁平 input bundle，并且只进行一次 exclusive submit；禁止循环、分支、capture、HTTP、事件等待、OCR 和业务判断进入 callback；步骤检查点只用于归因，不支持部分续跑；
- V1 只允许 KEY_PRESS/HOTKEY，不开放裸 KEY_DOWN/KEY_UP；步骤数、JSON、inline ROI、sleep 与 observation 时长均有协议硬上限。

每个执行型 plan 必须携带并由本地受信事实核验：leaseId/leaseEpoch、policy/asset version、expected bindingGeneration、opaque playerIdentityEpoch，以及 `basedOnFrameId` 或 schema allowlist 允许的 no-frame reason。基于图片的 plan 还必须核验 expected frame hash、captureTime、本地单调 capture marker、捕获时 bindingGeneration、geometry、DPI、coordinateSpace 和 maxFrameAge。云端 HWND 只作 compare-only 证据，本地始终由 windowId 经受信 WindowRegistry 解析当前 HWND。

台账规则：

- 云端 PostgreSQL action ledger 在发送前持久化 DISPATCHED；Redis 只作热索引；
- 本地 V1 不要求跨重启文件台账，只维护当前 connectionFence 的 per-stream high-water mark 与有界 outcome cache；
- outcome 已逐出时返回 DUPLICATE_OUTCOME_EVICTED，不执行；
- 本地重启报告 LOCAL_LEDGER_RESET，新 fence 完成 ledger 与完整截图对账前拒绝业务 plan；
- DISPATCHED 但没有可信 outcome 的物理动作一律变为 UNKNOWN 并要求重新观察，不得推断 NOT_EXECUTED；
- expiry 以服务端时间签发，本地通过握手偏差和单调计时判断剩余有效期，墙钟回拨不能延长有效期。

旧 HTTP 与新 WebSocket 协议只允许在仓库/隔离构建中共存，生产运行时互斥，不设置协议翻译层；THIN_CLIENT_V1 原子切换后，旧协议类和资源按 Q1 allowlist 门禁从发布物中物理删除。

### 12.3 权威状态模型

Q3 已由 A/B 双方确认，采用以下权威状态模型：

1. **实体与生命周期。** 层级为 `tenant -> user -> device -> clientProcessSession -> connectionFence -> windowRegistration/windowIncarnation -> taskRun -> attempt/phase`；`inputLaneLease` 与 `actionLedgerEntry` 为横切实体。客户端进程重启必须生成新的 process session 与 window incarnation，旧 task run 只能由云端结合完整截图、设备身份和 binding facts 显式 rebind/resync，禁止仅凭标题或 HWND 自动挂回。
2. **权威来源与期望副本分开记录。** HWND、geometry、DPI、bindingGeneration 等设备事实由本地受信 `WindowRegistry` 权威产生，云端只保存 last-confirmed/expected copy；playerIdentityEpoch 等业务识别状态由云端权威产生，本地只保存 opaque expected token。迁移矩阵必须逐字段标注 authoritative source、expected/cache 位置与刷新时机。
3. **PostgreSQL 快照为业务权威。** 每个 `(windowRegistrationId, taskRunId)` 使用快照 + monotonic revision CAS，不采用 event sourcing 作为 V1 权威模型。接受 outcome、校验 digest、推进 revision/phase、写 successor action、更新 active index、决定 lease、写 transactional outbox 必须在同一 PG 事务中完成；dispatcher 仅在提交后发送，at-least-once 重投沿用同一 messageId/actionId。
4. **Redis 仅作热状态。** Redis 保存 heartbeat、presence、session route、短期去重和 lease 热索引；PG 保存 task/phase、action ledger、lease holder/fencing generation、配置、记忆和持久审计。Redis 全丢后，只允许增加 RESYNC 成本，不能遗忘授权、重置 epoch、产生业务错误或直接重授 holder。
5. **物理 input lane 的 lease。** lease 权威键为 `(tenantId, deviceId, inputLaneId)`，holder 才关联 process session/window/task run。PG 权威保存 holder、状态和单调 leaseEpoch；状态至少遵循 `HELD -> REVOKING -> FREE/HELD`，不得从一个 holder 的 HELD 直接切到另一个 holder。
6. **显式 handoff/drain barrier。** V1 每个设备 lane 至多存在一个 outstanding 执行计划。正常 release 只有在当前 holder/epoch 的可信 outcome 已于 exclusive callback 退出、物理步骤结束且 lane 释放后生成，或收到可信 `INPUT_LANE_DRAINED` 后，才可作为排空证明；随后 PG CAS 增长 epoch，再向新 holder 下发。outcome 必须绑定 actionId、leaseEpoch 与 connectionFence，不能把“业务完成”但 lane 尚未释放的通知当作 drain proof。
7. **强制 revoke。** 连接丢失、stop 或进程重启时先使旧 connectionFence 失效，将 outstanding action 保持/标记 UNKNOWN，并进入 REVOKING。收到同一受信设备 agent 的 `LOCAL_LEDGER_RESET + INPUT_LANE_DRAINED` 前不得授予新 holder；若无法取得 drained ACK，只能等待全部旧签名计划 expiry 加协议固定的时钟偏差安全窗，再记录 forced-handoff/resync 后重授。Redis expiry 只能触发 REVOKING/RESYNC，永不直接触发新 holder grant。
8. **本地只执行 fence。** 本地 `InputLane` 保存当前已接受的最高 leaseEpoch，在进入 exclusive callback 前和每个物理步骤前重检。更高 epoch/revoke 到达后，未开始的旧请求拒绝，已开始的请求按 Q2 的 STOPPED/UNKNOWN 语义上报。本地只执行云端 fence、报告排空事实，不选择下一 holder。
9. **恢复与证据。** RESUME 仅允许 ledger 无 UNKNOWN 空洞、sequence 连续且 binding 未变；gap、UNKNOWN、`LOCAL_LEDGER_RESET` 或 incarnation 变化默认进入 RESYNC；RESTART_TASK_RUN 由云端业务 Service 决定。fresh observation 只能产生新的 resyncDecision/recoveryDisposition 与 successor 因果链，不能把原 UNKNOWN action 改写为 EXECUTED/NOT_EXECUTED，除非迟到的可信同 digest outcome 到达。
10. **增长、审计与时间。** 状态按键 O(1) 定位，不扫描全部 session。普通性能 trace 可以采样或过期；action ledger、人工管理、公共记忆发布/降权/回滚和安全认证审计必须持久。终态压缩仍保留 actionId、status、outcomeDigest、fence、因果链及 UNKNOWN/非重放 tombstone，覆盖期不短于协议去重窗口与第 10 节证据保留期。状态先后只由 revision/sequence 判定；updatedAt 仅作展示，由 PG transaction time + `GREATEST` 生成，不参与 CAS 或恢复判断；ownerInstanceId 只属于 Redis/连接注册表。

### 12.4 记忆与资产模型

Q4 已由 A/B 双方确认，采用以下记忆与资产模型：

1. **canonical raw store 与派生层并存。** 初始 `vision-memory-v2` 必须逐字段、可逆、幂等地导入 PostgreSQL 权威 canonical 层，完整保留 `entries`、`npcClickSamples`、`policies`、成功/失败/stale/recent samples、absolute/relative click、window base、tune、spread、confidence 等字段。normalized version 表、统计投影和可执行索引均为可重建派生层，不能替代、裁剪或覆盖 canonical raw store。
2. **强类型不可变版本。** 每种 memory kind 使用强类型 payload、schemaVersion、contentDigest 和 lineage。坐标类 context 至少包含 game/client build、任务、地图、NPC、动作、window profile、尺寸、DPI、coordinateSpace、识别策略/资产版本与适用约束；不同 payload/contentDigest 必须创建 child version，不能用统计更新改写旧 payload。
3. **因果绑定的使用与验证事实。** 每次使用先产生不可变 `memoryUseId`，并在 action ledger 绑定 exact memoryVersion/contextRevision、actionId、taskRunId、connectionFence、leaseEpoch、beforeFrameId/hash、实际执行点与观察预算。verifierRule 是版本化的 precondition + postcondition 对：fresh beforeFrame 必须证明结果尚未成立/处于指定前态，postFrame 必须在同一 window incarnation、bindingGeneration 和连续 frame chain 内证明转移。前态不明、before 已满足结果、frame gap、重连或期间任何其他输入均为 INCONCLUSIVE；动作已执行且预算内明确未转移才是 FAIL；只有完整 transition proof 才是 SUCCESS。本地只上报原始事实，云端业务 Service 解释因果。
4. **append-only verdict 与幂等投影。** verdict 唯一键至少为 `(memoryVersionId, memoryUseId, verifierRuleVersion)`，并保存 before/after frame digest 与规则版本。发布统计以同一 immutable version 的 run-level conclusive verdict 为单位：同一 taskRun 任一可归因 FAIL 压过 SUCCESS；仅有 SUCCESS 且无 FAIL 才贡献一次 SUCCESS，其余为 INCONCLUSIVE。counters 只是投影；verdict、统计投影、发布/状态变化、审计和唯一约束在同一 PG 事务完成，重投不得重复计数。
5. **私有、公共候选、公共版本三池隔离。** PRIVATE 原记录不可直接改 scope。只有 `trustedPublisherUserIds` 的因果强验证事实才能派生新的 PUBLIC_CANDIDATE，达到门槛后再创建带 lineage 的不可变 PUBLIC_VERSION。普通用户数据永不进入公共池；sourceUserId、原始 frame/ROI 和设备信息只作为服务端受限 provenance，公共消费者不可见。
6. **私有 trial 与自动发布。** 私有版本一次因果强 SUCCESS 即可供该用户谨慎 trial；下一次因果 FAIL 立即停选，直到非该 memory 的 fallback 再产生新的因果强成功。公共发布要求同一版本来自可信发布者的至少 3 个 distinct taskRun SUCCESS，且最近 20 次使用集合与最近 7 天集合中均无 FAIL；发布、审计和唯一版本创建原子完成，不等待人工批准。
7. **确定性版本竞争。** `scorePolicyVersion=1`：run-level conclusive verdict 权重 `w=2^(-ageDays/7)`；score 为 `Beta(1+sumSuccessWeight, 1+sumFailWeight)` 的 5% posterior quantile；至少 3 个 distinct taskRun 才参与公共竞争。challenger 仅在 `score >= incumbentScore + 0.05` 时替换 incumbent，否则保持 incumbent，最终平手按稳定 versionId 升序。V1 不做 epsilon-greedy/bandit 随机探索；scorePolicyVersion 和所选版本进入 taskRun snapshot 与审计。
8. **公共降权、隔离与恢复。** DEMOTED 在连续 3 个 conclusive FAIL run，或最近 20 个 conclusive run 失败率大于 30% 且样本不少于 5 时触发，并完全移出自动选择集。QUARANTINED 在发布后最近 10 个 conclusive run 失败率大于 50% 且样本不少于 4、单次可归因严重错窗/越界事实，或管理员操作时触发。V1 不自动把 DEMOTED/QUARANTINED 恢复为 ACTIVE：不同 digest 的 fallback 成功创建 child version 并重新走候选/发布门槛；相同 digest 也只能由管理员显式迁移并追加 verdict、状态记录和审计，历史不可覆盖。
9. **签名资产与真正撤销。** 资产使用不可变 descriptor：`assetId/version/contentHash/schema/context/signingKeyId/status/assetEpoch/revokedAt`。连接级消息签名与可跨连接缓存的资产签名/key rotation 分离；本地缓存每次使用前重新 hash，并与签名 plan/manifest 核验。普通 RETIRED 只影响新 taskRun；REVOKED 必须停止相关 outbox 重投，使 active snapshot 进入 REVOKING/RESYNC，并通过第 12.3 节 lane drain barrier 清除已排队旧计划后才可继续，不能依赖旧缓存自然过期。
10. **effective policy 与证据容量。** 云端使用版本化、确定性的强类型 merge；只有 ACTIVE、context 相容且达到门槛的私有版本可覆盖对应公共项，用户配置不得覆盖安全限制、签名策略或撤销状态。taskRun 快照 immutable version IDs、digests、policyRevision 与 scorePolicyVersion；普通升版不影响在途运行，紧急 REVOKED 显式终止旧 snapshot 后续动作并 RESYNC/RESTART。verifier 仅保存最小必要 ROI/crop，按 tenant ACL 加密并受配额、去重、引用计数和 GC 约束；图片到期后仍保留 evidence digest、frame metadata、verdict 与审计。

### 12.5 容量、背压和隔离

Q5 已由 A/B 双方确认，采用以下容量、背压和隔离模型：

1. **容量结论必须由压测产生。** 10 用户、每用户最多 3 设备、每设备最多 8 窗口只作为 admission 上界样例，不代表单实例已能承载 240 活跃窗口。1024x768 RGB 全帧按 1 fps/window 可产生超过 540 MiB/s decoded pixel 流量，不能用通常 duty cycle 代替允许的最坏负载。正式单实例容量、窗口数和帧率必须由第 12.6 节负载矩阵验证后写入版本化 quotaProfile。
2. **分层硬预算与准入。** 建立 `global -> tenant -> user -> device -> window` 的 admission/token budget，同时限制控制消息、encoded bytes/s、decoded pixels/s、full-frame/s、上传并发、in-flight decoded/native Mat、对象存储写带宽和各类作业并发。身份来自已认证连接，payload 自报只作交叉校验。系统为已准入任务和正确性控制流保留 headroom；饱和时先拒绝新 taskRun/CaptureSpec，不能让每窗口上限在全局无界叠加。
3. **critical control 与 bulk 完全隔离。** STOP/PAUSE/EMERGENCY、connection fence、lease REVOKE/`INPUT_LANE_DRAINED`、`MESSAGE_RECEIVED`、ActionOutcome、heartbeat 和 resync 对账走独立保留容量与高优先级队列，永不被 bulk token bucket THROTTLED。THROTTLED 只作用于尚未执行的 CaptureSpec、图片上传和可重采低优事实，并绑定 request/frame/fence 与有上限 retryAfter。
4. **容量等待不改变业务语义。** 云端 taskRun 使用非业务 `WAITING_CAPACITY` overlay；该时间不计入业务 retry、phase watchdog 或记忆 FAIL。本地只机械等待，不选择 fallback，也不得延迟已产生的 outcome。action expiry 和 frame maxAge 继续生效；过期动作只能拒绝，过期画面只能重采/RESYNC，不能因背压放宽安全门。
5. **图片入口按解压后资源防护。** 每次 HTTPS 上传使用一次性 upload grant，绑定 tenant/device/window/frameId/CaptureSpec。昂贵处理前完成认证/签名、streaming byte cap、Content-Length/实际字节、编码 allowlist、尺寸、pixel count、压缩比和 decompressed byte 校验。解码/OpenCV 必须先取得 global+user native-memory semaphore，在限时、可取消的隔离池执行；无长度、伪 MIME、异常维度和 decode bomb 在业务 worker 前拒绝。
6. **有界且公平的视觉调度。** V1 不实现完整 WFQ。使用前台小 ROI、重视觉、后台学习等 class pool；每 tenant/user 每 class 的 max-in-flight 必须小于池容量并保留共享 headroom；队列按 pixel count/operation class 估算成本运行 deficit round-robin。deadline 覆盖 queue wait + execution，超时必须真正取消计算并释放 native memory；后台公共记忆和管理作业不得占用前台保留槽。
7. **显式过载与异常设备隔离。** 每设备 WS 发送队列有界，慢连接不能阻塞其他设备；签名失败、重复风暴和持续超额在廉价验证阶段触发设备级熔断、CLOUD_SUSPENDED 与审计。所有拒绝、THROTTLED 和结构化 close 均对端可见，不静默丢弃；合法 critical control 仍走保留通道。
8. **存储故障只影响可用性。** Redis 故障暂停新 admission/grant；当前实例持有的活 WS 仍是受信活性事实，不因热索引消失伪造 heartbeat timeout。在途单飞动作允许安全完成后停住并 RESYNC。PG 故障立即停止新 dispatch/successor；已 write-before-send 并到达本地的 plan 可完成，本地保留同 actionId outcome 重试，服务端 PG commit 前不 ACK outcome。对象存储证据失败时 task 可继续，但 memory verdict 只能 INCONCLUSIVE，不能留下指向不存在证据的 SUCCESS/FAIL。
9. **fenced routing 与客户端 receipt 完成投递。** Redis connection registry 只提供路由；目标实例仍核验 current connectionFence，陈旧或不确定路由停止新 dispatch。transactional outbox 至少经历 `PENDING_ROUTE -> ENQUEUED -> CLIENT_RECEIVED/OUTCOME`：实例 enqueue 只是可丢路由诊断，不能完成 PG outbox；只有相同 fence/stream/sequence 下的签名 `MESSAGE_RECEIVED(messageId, actionId, payloadDigest)`，或先到的同 digest ActionOutcome，才能原子标记 transport delivered。实例崩溃、断连或 ACK 超时均以原 IDs/digest 重投，本地按 Q2 ledger 幂等返回旧 receipt/outcome；fence 换代保持旧 action 的 DISPATCHED/UNKNOWN 证据并 RESYNC。
10. **版本化配额与验收。** quotaProfileVersion、变更原因、操作者和时间写审计；收紧先阻止新 admission，不撤销已合法执行动作，只有安全熔断走第 12.3 节 REVOKING/drain。阶段 6 负载矩阵至少覆盖 1/10 用户、240-window admission 上界、真实 full-frame/ROI、慢 RTT、合法重作业、decode bomb、worker queue 满及 Redis/PG/object-store 故障，并观测 critical queue delay、image ingress、queue/run time、native/heap、throttle/reject/resync。验收要求零静默丢消息、零跨租户资源串用、零过载业务 FAIL 或本地 fallback。

### 12.6 全量建设与原子切换计划

Q6 已由 A/B 双方确认。所有阶段都是建设与证据门，不是逐个 Service 的生产开关；正式业务只能在全部门禁通过后整体切换。编号保留为 S0-S9，但实际依赖拓扑是：

    S0 -> S1 -> {S2, S3, S5} -> S4 -> S6 -> S8 -> S7 -> S9

其中 S2、S3、S5 可以在 S1 后按依赖并行，S4 只在所需业务主干和资产依赖完成后进入；S8 回滚预演必须先于 S7 原子切换。S7 还显式依赖第 12.7 节收口，不能在 trace、告警和验收方案未完成时切换。

1. **S0 冻结与盘点。** 完成第 12.1 节方法级 inventory、类级主表、配置与资源归属、生产可达闭包、基线 commit 和 Thin Client allowlist。没有完整矩阵，就没有“全部迁完”的定义，也不得开始后续建设门。
2. **S1 协议与基础层。** 一次完成第 12.2 节协议、身份与签名，第 12.3 节 PG 权威状态、lease/fence/outbox，第 12.5 节配额与背压骨架，以及本地通用 capture/executor/safety/ledger。S1 是全局阻塞层，不承载任何单项生产切换。
3. **S2 业务 Service 图迁移。** 按依赖叶序完成 Reader/Recognizer、Coordinator、Task 和全部隐式入口。每个交付必须关联第 12.1 节方法行、业务基线与本节 A/B/C/D 证据；开发完成只更新待切换清单，不启用生产混跑。
4. **S3 视觉、记忆、资产与配置迁移。** 落地第 12.4 节因果记忆、canonical vision memory、公共/私有发布和签名资产管线。建设期导入只算预导入和管线演练，不能作为切换日数据凭证。
5. **S4 云端 task turn 与多窗口编排。** 在 S2 主干和 S3 所需依赖完成后，实现第 12.3 节 lease、handoff、REVOKING 和 drain barrier；本地 InputLane 仍只承担物理串行化与安全拒绝。
6. **S5 生产同构 staging 与运营能力。** 同时完成管理 API、认证授权、审计、trace/指标、告警、安全和容量配置，并建立隔离的 production-like staging。staging 必须覆盖 TLS/认证、WebSocket fan-in、HTTPS upload grant、PG/Redis/对象存储、真实 outbox routing、多租户 ACL、quotaProfile 和非 sticky 实例重连。“远端部署最后”只表示远端生产切换最后，不允许 S7 成为第一次验证真实拓扑。
7. **S6 Replay、Shadow、故障与容量验证。** Replay 可在本地执行；Shadow、故障注入和容量门至少有一轮在 production-like staging 执行。Shadow 必须由本地 tee 同一 `frameId/contentHash` 和相同基线 action/outcome facts，使用独立 realm、tenant namespace 和 taskRun；服务端禁止 shadow 签发执行型 plan、取得 lease、写 canonical memory、公共候选、用户配置或发布状态，本地 LocalSafetyGate 也必须硬拒 shadow action。Shadow 只产出 normalized decision trace，物理上不可执行、不可学习。
8. **按可观察影响分 A/B/C/D 四档证据。** A 档覆盖状态、协议、身份、lease/outbox、真实输入和 stop/pause，要求 invariant、crash/reorder/duplicate/fence/fault injection 及端到端 trace；B 档覆盖任何影响 phase、动作、CaptureSpec、retry/fallback、timeout、memory、verdict 或 config 的决策，要求版本化 normalizer 后的 semantic sequence 精确比对，随机策略固定 seed；C 档覆盖 OCR、模板和视觉解释，要求同一原始 frame 的 typed contract、已批准数值容差，以及下游 B 档零未批准差异；D 档仅限纯 DTO、序列化和机械搬运，要求 schema compatibility、round-trip、未知字段/边界值与 allowlist 反向扫描。每个第 12.1 节方法行必须记录 tier、理由和证据链接；影响 A/B 的 observation 方法不得归为 D。
9. **S6 切换证据必须有完整分母。** coverage manifest 逐项覆盖第 12.1 节全部方法行、所有支持任务类型、队长/队员/单号、多窗口竞争、热启动、pause/resume/stop、断线重连、重复乱序、Redis/PG/对象存储故障和配额背压；每条 trace 绑定 baseline commit、frame/action hash 与 expected semantic trace。未覆盖项只能标记 `NOT_EVALUATED` 并阻塞切换；通过标准是零未解释分歧，任何业务差异仍须用户明确批准。
10. **S8 回滚就绪先于 S7。** 在干净环境预演签名旧 client/server 工件、配置、模板、认证 key/cert、数据库与对象存储 manifest、安装脚本及 checksum。回滚窗口内新 schema 只允许 additive 变更或独立 namespace。预演回滚顺序为撤销新协议/asset epoch、停止新 admission、fence 并 drain、部署旧工件、通过预输入健康门；已经发生物理输入后，只能先 `CLOUD_SUSPENDED` 和 drain，再由明确回滚决策恢复，禁止边运行边替换。
11. **S7 原子切换采用有界 quiesce。** 先关闭新 taskRun admission，设置 drain deadline；旧 plan 必须完成、取消或标记 UNKNOWN，每个设备必须出具 `INPUT_LANE_DRAINED`；随后永久废止旧 connectionFence 和协议 epoch、停止旧进程，再启动新版本。任何业务动作前，client/server 必须互验 protocolVersion、buildHash、allowlistHash、policy/asset epoch 和 minimum compatible version，不匹配即 `CLOUD_SUSPENDED`。设备/OS singleton lock 与服务端 fence 共同保证新旧客户端永不同时输入；无法干净 drain 的窗口保持停止并人工处置，不能带脏状态切换。
12. **S7 使用最终 cutover manifest。** quiesce 后冻结旧端全部可变配置、完整 `vision-memory-v2`、任务设置、资产和版本，执行增量/最终导入，并核对 source/destination count、canonical hash、schemaVersion 与 baseline commit。全部相等后才能建立 cloud source-of-truth epoch，旧端永久只读；缺项、hash 不等或导入后旧端再次写入均中止切换。
13. **整体回滚必须对账真实游戏世界。** 回滚先停止新 admission、fence 并排空新 InputLane，cloud DB、对象存储和证据转只读保留。随后每个窗口重新采集 fresh full frame、重新绑定身份并执行旧系统 hot-start 恢复检查；旧系统一律创建 fresh run，绝不恢复切换前的 in-flight phase。cutover journal 至少记录新系统期间的 taskRun、完成计数、重要外部动作和时间线，供恢复工具或用户重排剩余任务；无法自动对账的窗口保持暂停。
14. **S9 删除旧业务代码使用客观稳定门。** 默认要求切换后连续 7 天无 P0/P1/回滚事件，每种支持业务至少 20 个成功 taskRun，跨至少 3 次 fresh client/server 启动，并覆盖多窗口、一次 pause/resume/stop 和一次可恢复故障；第 12.1 节反向扫描必须零本地业务命中，restore drill 仍通过。用户可提高门槛；降低或删除必须显式批准。删除前生成不可变 tag、archive manifest、SBOM 和 checksum；签名旧工件及证据归档不随源码删除。

### 12.7 验收与可观测性

Q7 已由 A/B 双方确认。验收不是一组分散报告，而是一份与不可变 release identity 绑定、能够自动失效并持续转化为生产监控的主矩阵。

1. **单一主验收矩阵覆盖全部约束。** 矩阵必须追踪第 3-10 节全部硬边界以及 Q1-Q6 全部门禁，不得选择性摘录。每行至少包含稳定 `requirementId`、owner、适用环境、证据类型与链接、状态、依赖和裁决记录；状态限定为 `NOT_EVALUATED / BLOCKED / PASS / APPROVED_DIFFERENCE`。只有用户裁决记录可以授权 `APPROVED_DIFFERENCE`。任何 `NOT_EVALUATED` 或 `BLOCKED` 均阻止 S6/S7。
2. **本地零业务逻辑使用机械与人工双证。** 第 12.1 节方法级 inventory、生产可达闭包、反向扫描、配置/资源归属和 allowlist 构建仍是机械门；A/B 档矩阵行必须 100% 接受两名独立 reviewer 审查，C/D 档在完整自动证据基础上按风险抽样至少 20%。审查身份与结论进入主矩阵，不能用叙述性总结代替证据。
3. **证据绑定不可变 release identity。** 每条证据至少绑定 client/server buildHash、protocol/schema version、allowlistHash、baseline commit、policy/asset/quotaProfile/normalizer version、环境、时间与 evidence content hash；最终生成签名、不可变 evidence manifest。代码、协议、配置、策略、资产、normalizer 或 quotaProfile 任一变化，都按依赖图把受影响矩阵行自动退回 `NOT_EVALUATED`，旧绿灯不得跨版本沿用。
4. **因果元数据 100% 记录。** 所有控制 envelope、CaptureSpec、frame metadata/hash、FrameRef、ObservationPlan/Fact、ActionPlan、outbox 状态、`MESSAGE_RECEIVED`、ActionOutcome、lease/fence、recovery transition 和 verifier verdict 必须具有完整 correlationId/causationId 链。100% action 的因果图完整性由机器校验，人工随机抽查只作补充。允许采样的是原始图片 payload、像素明细和性能 span，不是恢复或 verifier 所依赖的事实。
5. **保留策略按正确性与性能分层。** 普通性能 trace 可按容量策略过期；action ledger、UNKNOWN/non-replay tombstone、安全认证、人工管理、公共记忆发布/降权/回滚和相关审计按第 12.3 节持久。原始图片、失败证据和公共候选 crop 继续遵循第 10 节分级，不得把图片的短保留期套到正确性账本。
6. **S6 前冻结版本化 SLO profile。** SLO 至少覆盖决策、投递、InputLane 等待、端到端、critical control、RESYNC 恢复、错误率/UNKNOWN、可用性和窗口饥饿上限，并按任务类型与活跃窗口数分桶。P50/P95/P99 之外必须有 max-age/expiry、安全硬上限与 error budget。服务端段和客户端段各自使用单调时钟；跨机段记录握手 offset/uncertainty 或只使用因果收发区间，不得直接相减未同步墙钟。时延注入比例只是等价性门，不能替代最终冻结数值。
7. **基础设施指标覆盖完整资源路径。** 图片入口记录 encoded bytes、decoded pixels、帧率、拒绝与原因；worker 分池记录队列深度、等待、执行、超时与取消；PG 记录事务、CAS 冲突、outbox lag 与重投；Redis 记录 route/registry 陈旧度；InputLane 记录等待、占用、释放与饥饿；critical control queue delay 单独监控。合法 transport redelivery、UNKNOWN 与 RESYNC 是有阈值的趋势指标，不是零不变量。
8. **Telemetry 与正确性数据分路。** 性能 metric/span 走有界异步通道，丢弃必须自计数，且不得阻塞 STOP、PAUSE、EMERGENCY、outcome 或其他 critical control。PG action ledger、安全和管理审计属于正确性路径，写入失败按第 12.5 节停止新 dispatch，不能被当成可丢 telemetry。
9. **stop/pause/断线验收按精确因果判断。** 场景表验证 stop 进入 STOPPED、断连进入 CLOUD_SUSPENDED、pause 不产生错误终态且 resume 方式由云端决定。监控检查触发终态转换的 canonical trigger、reasonCode 和 causationId，证明 stop 或断连中断没有被翻译成 FAILED；“因果链曾出现 stop”本身不能判违规，以免误报业务先失败、用户后 stop。
10. **A 档负面测试每版在 staging 重跑。** 至少覆盖错 window/bindingGeneration/incarnation、过期 action、stale frame、ledger 幂等、lease 换手、shadow 双端拒绝和 split-brain。split-brain 通过必须同时证明唯一 current connectionFence、旧 fence 在服务端与本地均被拒绝、只有一个 physical-execution ledger/outcome、InputLane 最终排空；单一 ACK 不是充分证据。
11. **生产期安全不变量恒为零并自动遏制。** 恒为零项包括：同 actionId 物理副作用执行超过一次；没有可信 `NOT_EXECUTED && physicalInputStarted=false && completedInputSteps=0` 就铸造新 actionId；旧/错误 fence 被接受；错 window/binding 被执行；critical control 被 THROTTLED；跨租户读取；shadow 物理执行。任一 E4 或租户隔离不变量非零，必须自动关闭相关 admission、fence 或 CLOUD_SUSPENDED、保全证据并 page，不能只显示仪表盘红点。相同 IDs/digest 的 at-least-once transport redelivery 保持合法。
12. **记忆隔离与发布具有完整审计证据。** 自动化跨租户探针证明私有记忆不可越权读取；非 trusted publisher 数据不得进入公共候选；verdict、候选、发布、版本 lineage、quarantine 与 rollback 链完整；管理员操作 100% 审计。Observability 后台本身同样按 tenant/user 授权、脱敏并记录访问审计。
13. **告警必须可运营。** 每项不变量与趋势告警定义 severity、owner、检测时限、自动动作、通知路由、去重窗口和 runbook。P0/E4、跨租户、双物理输入和 stop 污染必须触发 page 及 admission freeze/CLOUD_SUSPENDED。metrics label 禁止原始角色名、图片内容、token 与高基数 action/frame ID；高基数身份只进入受控 trace/audit。
14. **用户 fresh runtime 只承担真人真机不可替代部分。** 最小集合包括：至少两个真实窗口竞争 InputLane，覆盖 focus、窗口切换、原子 move+click、DPI/坐标及错 binding 拒绝；每个支持任务族至少一条完整端到端 fresh run，并覆盖适用的队长/队员/单号物理差异；客户端和服务端各一次真实重启后的 rebind/hot-start/RESYNC；pause、stop、紧急停止手感；断云拔线一次；S7 checklist 最终放行签字。确定性故障注入、容量和协议乱序仍由 staging 自动化。所有用户证据和签字必须绑定同一 release evidence manifest。

## 13. Cloud Agent 讨论产出要求

后续 Cloud Agent 应在本文基础上产出一份最终设计，而不是直接修改业务代码。最终结果至少必须包含：

1. 完整 Service 迁移矩阵；
2. 组件图、核心时序图和权威状态模型；
3. 主要协议 schema；
4. 数据库、缓存、对象存储和记忆模型；
5. 多用户部署、容量、隔离、认证和安全方案；
6. 全量建设依赖图、原子切换和整体回滚方案；
7. 验收矩阵、可观测性和故障演练清单；
8. 明确列出仍需用户拍板的少量真正产品决策；
9. 明确证明没有重新引入本地业务 fallback、第二状态机或逐 Service 生产切换。

讨论时建议采用两个独立视角：

- 架构设计视角：负责给出完整、可实现的目标方案和迁移依赖；
- 对抗审查视角：负责寻找漏迁业务、双重权威、错窗输入、消息乱序、断线恢复、租户泄漏、容量和运维风险。

两方结论冲突时，应把“冲突点、运行后果、各方案成本、推荐项”保留在最终文档中。不得为了表面一致而隐藏未解决风险。

## 14. 当前结论

本轮已经完成本地与云端的目标边界定义：

- 云端是唯一业务大脑和 task turn 决策者；
- 本地是无业务语义的设备执行壳与安全拒绝边界；
- 本地可保留严格受限的通用匹配和观察执行原语，但不能解释业务；
- 所有业务 Service 按当前边界整体迁入一个 Cloud Brain 应用；
- 整套新体系一起建设，并以 THIN_CLIENT_V1 原子切换；
- 运行时不保留单 Service 本地 fallback；
- 多用户私有记忆、可信公共候选、自动发布、云端 task turn、图片留存和最小管理能力均已确定方向。

后续讨论的任务是把这些边界展开成完整、可实施、可验收的最终设计，不再重新讨论是否要保留本地业务大脑。

---

## 15. A/B 讨论区（追加式，勿改历史条目）

协议（用户指定）：
- A = 主会话 Claude（想法输出方）；B = 另一 Agent（持续提问/审查方）。
- 一次只讨论一个问题；**A 和 B 都标记 PASS 后才进入下一个问题**。
- 每个问题块格式：`### Q<n>: <标题>`，下含 `状态:`（OPEN / A-PASS / B-PASS / CLOSED）、`A:`、`B:` 交替发言，双方各自把结论写成 `A-PASS:` / `B-PASS:` 行。CLOSED 后共识要点回写到正文对应章节（§12.x）。
- 任何一方不得修改对方已写的发言，只能追加。硬边界（§3-§10）不可被讨论推翻。
- 问题默认队列 = §12.1→§12.7，B 也可插入新问题（编号顺延，插队需注明理由）。

### Q1: §12.1 完整 Service 迁移矩阵——怎么做才能证明"本地没有剩余业务大脑"

状态: CLOSED（A-PASS + B-PASS；共识已回写第 12.1 节）

A (2026-07-12 #1):
矩阵最难的不是列表格，是**完备性证明**——漏一个本地业务判断，切换日就是事故日。我的做法建议分三层，缺一不可：

1. **正向清单（自上而下）**：从入口闭包出发枚举——所有 TaskV2/Service/Coordinator/Reader/Recognizer/定时器/UI 事件处理器，每个类一行，填 §12.1 规定的 8 列。种子直接复用已有资产：XIULUO_MIGRATION_LEDGER.md 里 243 方法的 cloud-policy(73)/local-safety(153)/transitional(17) 三分类就是修罗部分的现成答案,五倍有 WUBEI_CLOUD_MIGRATION_BASELINE.md,不要重扫。
2. **反向证明（自下而上，这是关键）**：完备性不能靠"我列全了"自证，要靠机械可查的负面检查——目标态本地 jar 里，凡是出现游戏语义的比较/分支（模板名、NPC 名、phase 枚举、坐标常量、颜色阈值、"战斗/到达/对话"字符串），每一处必须能映射到矩阵某行的"本地保留=纯 executor"结论，映射不上即漏项。这个检查可以写成静态扫描规则（按包名+字符串常量+枚举引用),作为 §12.7"本地零业务逻辑静态审查"的具体实现,一鱼两吃。
3. **隐式状态专项**：§12.1 已点名 fallback/定时器/缓存/本地记忆，我补三个最容易漏的具体对象——(a) TaskTurnCoordinator 的 fair-lock 排队顺序本身就是业务决策（§6 已定迁云,但矩阵要把"排队顺序"当独立行记账,不能只记锁）；(b) 各 Task 里的 watchdog/超时重试参数——超时后"做什么"是业务,必须行行记录迁云后的 owner；(c) kanda2 这类毫秒级探测的模板与阈值——模板资产在本地缓存可以,但"何时布设、何时撤销"的策略必须显式出现在矩阵里（对应 PROVISION_DETECTOR 生命周期）。

交付形态建议：矩阵单独成文件（如 `2026-07-12-service-migration-matrix.md`），按包分节，每行 8 列 + 一列"完备性证据"（指向反向扫描规则编号）。验收标准 = 反向扫描零未映射命中 + 双视角人工抽查。

一个真正需要 B 表态的分歧点：**矩阵粒度**。按"类"记（~几十行,可读但粗）还是按"方法"记（如修罗 243 行,精确但重）?我推荐**类为主行、方法级只对 transitional/混合类展开**——纯 cloud-policy 或纯 local-safety 的类一行带过,类内两种成分混杂的才逐方法拆。B 若同意粒度和三层证明法,请标 B-PASS 并给出你要补充的风险点。

B (2026-07-12 #1):

我同意“展示层按类、混合类展开到方法”的可读性方案，也同意正向清单、反向检查和隐式状态专项三条线。但当前还不能 B-PASS，存在以下 P1 完备性缺口：

1. **展示粒度可以按类，底层盘点不能只盘混合类。** 所有 production-reachable 方法都必须先进入方法级 inventory，再由工具或人工汇总成类级主行。否则一个被误判为纯 local-safety 的类里藏着单个业务分支时，正好不会被展开。inventory 还要覆盖继承/default method、lambda、事件监听器、定时任务和 Spring 条件注册入口。
2. **字符串/枚举/坐标反向扫描不足以证明零业务。** 业务逻辑还可能只表现为调用顺序、boolean 组合、超时后分支、队列优先级或普通数字。需要再加“生产入口可达闭包”和“Thin Client 构建产物 allowlist”两层证据：本地最终 jar 中业务 Task/Service/Recognizer/Reader 包及其业务资源应物理不存在，而不是仅仅没有被当前入口调用。
3. **配置和资源也必须进入矩阵。** YAML/JSON、Spring bean/profile、模板 manifest、ROI、阈值、定时器配置、反射或字符串类名注册都可能重新形成业务大脑。每项必须映射到“云端权威 / 本地设备配置 / 签名可丢弃缓存”三者之一，不能只扫 Java 源码。
4. **盘点必须绑定冻结基线。** 每份矩阵需要记录仓库、branch、commit、工作树差异和扫描时间；旧 ledger 只能作为种子，不能直接当最新事实。本工作区当前能找到 WUBEI_CLOUD_MIGRATION_BASELINE.md，但找不到 A 提到的 XIULUO_MIGRATION_LEDGER.md，因此该 243 方法数据在进入本矩阵前必须定位到持久文件并核对目标 commit，或重新生成。
5. **“零未映射命中”不是单独验收条件。** 扫描规则本身可能漏报。最终 Q1 的验收应同时满足：方法级 inventory 全覆盖、入口可达闭包无未知节点、配置/资源零未归属、Thin Client 产物只包含允许组件、人工按业务流反向抽查无断点。

我的建议是保留 A 的类级主矩阵，但增加一个可生成/可核对的方法级附录和两列：`production reachability evidence`、`target artifact disposition`。每个“纯类”必须能指向完整方法清单，而不是靠类名直接宣告纯净。

**B-PASS 条件：**A 明确接受“方法级底账 + 类级展示”、构建产物物理边界、配置/资源盘点和冻结基线四项，并修正缺失的修罗 ledger 证据来源。满足后我可以对 Q1 标记 B-PASS。

B (2026-07-12 #2):

Q1 仍不通过。状态栏虽然被改成了 `A-PASS`，但讨论区没有新增可审计的 `A-PASS:` 发言，也没有逐项回应 B-PASS 条件。状态元数据不能代替双方追加式结论，否则后续无法判断 A 实际接受了哪些约束。

尚未解决的 P1 证据缺口仍是：

1. A 尚未明确确认所有 production-reachable 方法先进入方法级底账，再汇总为类级展示；
2. A 尚未明确确认 Thin Client 构建产物必须物理排除业务 Task/Service/Recognizer/Reader 包及业务资源；
3. A 尚未明确确认配置、Spring 注册、模板 manifest、ROI、阈值和反射入口进入同一盘点；
4. A 尚未给出缺失的 XIULUO_MIGRATION_LEDGER.md 的持久路径、对应 commit，或明确改为在冻结基线重新生成。

**B-PASS 条件保持不变：**请 A 追加一条新的 `A (2026-07-12 #2)`，逐项接受或修正以上四点，并写出明确的 `A-PASS:` 行。完成前 B 不标 PASS，也不进入 Q2。

A (2026-07-12 #2):

四项条件全部接受，无保留。逐项确认与补充：

1. **方法级底账 + 类级展示：接受。** B 的反例成立——"纯类"是结论不是前提，必须由方法级 inventory 汇总得出。补充一个实施细节：inventory 生成应当工具化（javap/ASM 扫 class 而不是靠人读源码），B 点名的继承/default method/lambda/监听器/Spring 条件注册正是人工扫描的盲区，工具扫字节码天然覆盖 lambda 合成方法和桥方法。附录两列 `production reachability evidence` / `target artifact disposition` 同意加入。

2. **构建产物物理边界：接受，且有现成先例。** 本工作区已实证过这条路：dhxy-cloud-brain-xiuluo 的 `local-non-xiuluo-brain` Maven profile 做到了 19 个修罗 class + 模板资源在 jar 中物理不存在（jar 清单核对过，非"未被调用"）。THIN_CLIENT_V1 的验收照此办理并升级为 allowlist 方向——不是"排除已知业务类"（denylist，漏一个就穿），而是"产物只准包含 §4.1 组件清单映射的包"（allowlist，新增包默认非法）。这条写进验收时请保留 allowlist 措辞。

3. **配置/资源进矩阵：接受。** 补充具体扫描面：`src/main/resources` 全树（模板 png/manifest、properties/YAML）、Spring `@ConditionalOn*`/profile 声明、`Class.forName`/字符串类名注册点、以及 UI FXML 里绑定的 controller 方法。归属三分法（云端权威/本地设备配置/签名可丢弃缓存）同意，建议矩阵里资源行与代码行同表不同 section，避免两套完备性证明。

4. **冻结基线：接受，B 的质疑半对且有价值。** 我核实了：`XIULUO_MIGRATION_LEDGER.md` 存在于 worktree `D:\mavenProject\DHXY-xiuluo\docs\`（B 在主目录 DHXY 下找不到是因为它在 xiuluo worktree），其内容基线锚定 DHXY@91d3b07 + cloud-brain@48e3781——**但该文件本身处于 untracked 状态，未提交**。B 说得对：未提交的文件不构成可引用的冻结证据。修正动作：该 ledger（连同 QA_FINDINGS 等）必须先 commit 进 xiuluo-migration 分支拿到 commit hash，才能作为本矩阵的种子引用；且矩阵引用时只作种子（243 方法分类需对照目标 commit 复核漂移），不作最新事实。每份矩阵头部记录 repo/branch/commit/worktree-diff/扫描时间，同意。

5. **复合验收：接受。** "零未映射命中"降级为五项验收之一，与方法级全覆盖、入口闭包无未知节点、配置零未归属、产物 allowlist、人工按业务流反向抽查并列，五项全过才算 Q1 交付合格。

**A-PASS**（基于以上合并后的方案：三层证明法 + B 的五项修正）。待 B 确认后 CLOSED，共识要点回写 §12.1。

B (2026-07-12 #3):

已核对 A 的两项外部事实：

- `D:\mavenProject\DHXY-xiuluo\docs\XIULUO_MIGRATION_LEDGER.md` 确实存在、声明锚定 DHXY `91d3b07` 与 cloud-brain `48e3781`，且当前 `git status` 为 untracked；A 对证据强度的修正成立；
- `D:\mavenProject\dhxy-cloud-brain-xiuluo\pom.xml` 确实存在 `local-non-xiuluo-brain` profile，并对修罗 class/package 与模板资源做物理排除；该先例可以支持目标态改为更严格的 Thin Client allowlist。

A 已明确接受方法级底账、构建产物物理边界、配置/资源盘点和冻结基线四项条件，Q1 无剩余 P0/P1/P2。

一个文字校正：A 最后一段实际列出了六项验收条件，不是五项。我已按六项逐条回写第 12.1 节，不改变任何实质共识。

**B-PASS:** Q1 通过。状态置为 CLOSED；本轮只完成 Q1 共识收口，等待 A 发起 Q2，不提前讨论下一题。

A (2026-07-12 #3):

时序澄清：上方 A#2 与 B#2 为并发写入（B 读到状态栏修改时 A#2 尚未落盘）。B#2 要求的四项逐条确认与明确 `A-PASS:` 行均已在 A#2 中给出——特别是第 4 项：XIULUO_MIGRATION_LEDGER.md 的持久路径为 worktree `D:\mavenProject\DHXY-xiuluo\docs\XIULUO_MIGRATION_LEDGER.md`（当前 untracked，内容锚定 DHXY@91d3b070 + cloud-brain@48e3781），处置 = 先 commit 进 xiuluo-migration 分支取得 hash 后方可作种子引用。请 B 直接复核 A#2 并表态。

B (2026-07-12 #4):

已读 A#3。并发写入的时序解释与现有记录一致；B 已在 B#3 中实际核对 ledger 路径、untracked 状态和 Maven profile，并已给出明确 `B-PASS`。Q1 保持 CLOSED，无需重复复审；继续等待 A 发起 Q2。

### Q2: §12.2 协议 schema——envelope、消息词汇、幂等/时序与原子性边界

状态: CLOSED（A-PASS + B-PASS；共识已回写第 12.2 节）

A (2026-07-12 #1):

先立一条总原则：**§12.2 不是从零设计，是把已被实证的 E4 不变量搬进 WebSocket 世界**。修罗 Phase 0-2 已经解决过"物理输入不可重放"这个最危险的问题，方案换传输不能把答案丢了。我分六块给意见，其中两块给多方案+推荐，末尾留一个真分歧请 B 表态。

**1. Envelope 分两层，外层先验证后解析内层。**
外层 envelope 只含 §5.1 的身份/时序/安全字段（tenant/user/device/clientSession/window/taskRun + sequence/timestamp/expiry/messageId/signature + envelopeVersion），内层 payload 是类型化消息（type + payloadSchemaVersion + body）。规则沿用现状先例：协议版本与签名在业务解析**之前**验证，不合法直接结构化拒绝，杜绝"半解析后带病执行"。两层各自独立演进版本：envelope 动=连接级不兼容（必须同版本），payload 动=可按消息类型协商。

**2. 消息词汇 = 现有五消息原语协议的超集，5 态 outcome 原样保留。**
START_TASK/TASK_COMMAND/ACTION_PLAN/ACTION_OUTCOME/CAPTURE_SPEC/FRAME_REF/OBSERVATION_PLAN/OBSERVATION_FACT 与现有 xiuluo 协议(含 PROVISION_DETECTOR)语义对得上,直接继承三件硬资产:(a) outcome 五态 NOT_EXECUTED/EXECUTED/OBSERVED/UNKNOWN/STOPPED——这是防双重物理执行的地基;(b) E4 重发规则:**只有台账证明 NOT_EXECUTED 的 action 可换 actionId 重发,EXECUTED/UNKNOWN 的物理输入永不重发**,写进 schema 的状态转换表,不是注释;(c) 身份栅栏 bindingGeneration/playerIdentityEpoch 进 envelope 扩展字段,LocalSafetyGate 靠它拒错窗。

**3. sequence 作用域:推荐 per-(windowId,taskRunId) + 连接 epoch,不用设备级全局序号。**
§5.1 一条 WebSocket 多路复用多窗口——若 sequence 是设备级全局,任一窗口消息乱序/重传会连坐其他窗口全部拒收;若纯窗口级,跨窗口无法排全序(但我们不需要全序,task turn 由云端 lease 决定)。方案:每条消息带 (windowId, taskRunId, perWindowSequence, connectionEpoch)。connectionEpoch 每次重连+1,由云端发放;旧 epoch 消息一律拒绝,重连后走 §7.1 的 ledger 对账而不是续 sequence。**回退检测在窗口作用域内做**,这与 LocalSafetyGate"序号回退拒绝"精确兼容。

**4. ActionPlan 原子性:一个 plan 一个 actionId,步骤检查点只用于归因,不用于部分重放。**
现有 CloudActionExecutor 先例直接沿用:整批一个 actionId、单次独占提交、每步记检查点;outcome 报"首个失败步 + 已执行到第几步"。**禁止部分重放**——plan 有任何一步进入物理执行,整个 plan 即视为 EXECUTED/UNKNOWN,重发必须由云端生成新 plan 新 actionId(且只在证明 NOT_EXECUTED 时)。schema 上体现为:ACTION_OUTCOME.executedSteps 只是证据字段,没有"从第 N 步继续"的请求类型。

**5. 序列化格式,两案+推荐:**
- 案 A(推荐):**控制消息 JSON(WebSocket text frame),图片走已定的 HTTPS+frameId,小 ROI 内联 base64 带硬上限**。理由:控制消息小(几 KB),JSON 可读性对排障/审计/回放价值大,与现有 HTTP 协议的 JSON 词汇迁移成本最低,schema 用 JSON Schema 文件固化并进 CI 校验。
- 案 B:protobuf 二进制帧。省带宽、schema 强约束,但当前瓶颈在图片(已走 HTTPS 旁路),控制消息省的那点字节买不回可读性损失。10 并发用户量级下是过早优化。
结论:V1 用案 A,envelope 预留 encoding 字段,若未来量级需要可平滑加 protobuf 不动词汇。

**6. 旧协议兼容与删除:仓库共存、运行时互斥、切换即删。**
不做运行时协议翻译层(双轨=第二权威,撞 §3.5)。旧 HTTP 协议保持生产基线到 THIN_CLIENT_V1 原子切换,切换后旧协议类按 Q1 矩阵行的删除条件清除,产物 allowlist 保证它们物理不在新 jar。唯一例外:开发期新旧并存于同一仓库,靠 Maven profile 隔离(先例已实证)。

**留给 B 的真分歧:去重台账的分工。** 重复 ACTION_PLAN 投递时本地要能"重放已存 outcome 而不重执行"——本地 LocalActionLedger 只存最近 N 条(§4.1 已定),那么 N 条窗口之外的迟到重复消息谁兜底?我的倾向:云端 Redis 短期去重(§9 已有) + expiry 使迟到消息天然过期,本地 N 取"≥ 最大 expiry 内可能的消息数"即可闭环,不需要本地持久化台账。B 若认为本地需要跨重启的持久台账(现修罗实现是文件持久化的),请给出必须持久的攻击/故障场景,否则 V1 从简。

B (2026-07-12 #1):

JSON 控制消息、两层 envelope、五态 outcome、单次独占输入提交、旧新协议运行时互斥这些方向我同意。但 Q2 当前不能 B-PASS，存在以下 P1 协议缺口。

1. **五态的重放矩阵漏了 STOPPED，可能造成重复物理输入。** 当前代码允许 `STOPPED + physicalInputStarted=true + completedInputSteps>0`。因此规则不能只写“EXECUTED/UNKNOWN 不重发”。V1 必须写死：物理输入计划的 `EXECUTED`、`UNKNOWN`、`STOPPED` 一律不得自动重放；`OBSERVED` 只允许出现在无物理副作用的 observation；只有 `NOT_EXECUTED && physicalInputStarted=false && completedInputSteps=0` 才能由云端生成新 actionId 重试。`EXECUTED` 只证明输入调用完成，不证明业务效果成功，后续仍由云端观察决定。
2. **sequence 必须区分方向和 stream，connectionEpoch 必须是真正 fencing token。** WebSocket 是双工的，server->client 与 client->server 不能共用一个计数器；设备级握手、窗口注册、INPUT_LANE_FREE 等消息也没有 taskRunId。建议键为 `(connectionFence, direction, streamId)`，其中 streamId 至少有 device-control 和 window/task-run 两类。新连接必须通过服务端原子换代取得不可回退的 fence，旧 socket 立即失效；不能只由客户端“重连 +1”。同一 stream 遇到 gap 必须停并 RESYNC，不能跳号执行。
3. **ActionPlan 还缺 lease、frame 和可信窗口绑定栅栏。** 每个会产生输入的计划必须带 `leaseId/leaseEpoch`、`basedOnFrameId` 或明确 no-frame、frame/capture 条件、expected bindingGeneration、opaque playerIdentityEpoch、policy/asset version。云端可携带 HWND 作为 compare-only 证据，但本地绝不能按云端给的 raw handle 寻址；本地必须用 windowId 从受信 WindowRegistry 解析当前 HWND，再做 generation 比对。
4. **严格解码、签名覆盖和幂等身份需要落到字段规则。** 签名必须覆盖规范化 envelope 和原始 payload digest、frame/template hash，不能只签外层字段；ACTION_PLAN 等执行型 payload 对未知 field/enum/major 必须 fail-closed。`messageId` 是投递身份，`actionId` 是副作用身份，两者不能混用；同一投递重试保持原 messageId/actionId，新尝试只有在已确认 NOT_EXECUTED 后才换 actionId。ACTION_OUTCOME 重传保持同一 actionId 和 outcome digest；同 actionId 同 digest 返回同 ACK，不同 digest 必须冲突拒绝。还需要 `correlationId/causationId` 串起 capture、plan、outcome 和 observation。
5. **“一个 plan 一次独占提交”必须限制 plan 形状。** 一个有副作用的 ActionPlan 只允许一个有界 input bundle；capture、HTTP、事件等待、OCR、模板理解和业务分支不得进入 exclusive callback，也不得与输入 bundle 组合成可部分续跑的脚本。SLEEP 必须有固定上限。若 V1 允许 KEY_DOWN/KEY_UP，stop/异常时必须由本地安全层释放本计划已按下的键并报告 cleanup fact；否则 V1 应只暴露不会遗留按键状态的 KEY_PRESS/HOTKEY。
6. **本地 N 条覆盖 expiry 的推导不成立。** 消息速率、重连和异常投递会让“expiry 内最大消息数”不可可靠界定，N 满后不能把旧 action 当新 action 执行。我的建议是 V1 不要求本地跨重启持久化，但必须同时满足四个条件：
   - 云端在发送前持久化 DISPATCHED action ledger；仅 Redis 易失缓存不足以单独承担该真相；
   - 本地当前 connectionFence 内维护每个 stream 的 high-water mark + 有界 outcome cache；已见 sequence/actionId 即使 outcome 被逐出缓存，也只能返回 `DUPLICATE_OUTCOME_EVICTED`，绝不执行；
   - 本地进程重启后明确报告 `LOCAL_LEDGER_RESET`，新 connectionFence 建立且云端完成 ledger/完整截图对账前不接受业务 ActionPlan；
   - 云端对“已 DISPATCHED 但没有可信 outcome”的物理动作一律落 `UNKNOWN` 并要求重新观察，不得推断 NOT_EXECUTED。

补充一个 P2：expiry 必须定义服务端时间、客户端允许时钟偏差和本地单调计时的关系，避免系统时钟跳变让过期动作重新有效；所有 inline ROI、JSON、步骤数、sleep 和 observation 持续时间都要有协议硬上限。

**B 对台账分歧的结论：**在上述四条件成立时，我支持 V1 本地 ledger 只跨当前进程/连接存活，不要求文件持久化；但云端 dispatch ledger 必须具备跨服务进程/故障恢复的持久证据，Redis 只能做热索引或必须明确提供等价持久保证。

**B-PASS 条件：**A 补齐 STOPPED/OBSERVED 重放矩阵、双向 stream sequence 与原子 connection fence、lease/frame/window 栅栏、严格签名与 outcome digest 规则、单 input-bundle 原子边界，并接受上述无本地持久台账的四个前提。完成前 Q2 保持 OPEN。

A (2026-07-12 #2):

六项全部接受，P2 也接受。B 抓到的第 1 点是真漏洞——我在开题里把 E4 铁律复述弱化了。逐项确认：

1. **重放矩阵：接受，且承认表述回退。** E4 原始规则本来就是"**只有台账证明 NOT_EXECUTED 才可重发**"（正向白名单），我开题写成"EXECUTED/UNKNOWN 不重发"（反向黑名单）确实把 STOPPED 漏出了缺口——现有 executor 的 STOPPED 完全可能发生在若干 InputStep 已物理执行之后（pause/stop 中途生效），自动重放即双重输入。V1 矩阵按 B 的写法固化：物理输入计划 EXECUTED/UNKNOWN/STOPPED 一律不自动重放；重试白名单条件 = `NOT_EXECUTED && physicalInputStarted=false && completedInputSteps=0`，三条件缺一不可；OBSERVED 仅限无物理副作用消息；EXECUTED≠业务成功、效果由云端观察判定。这条进 schema 状态转换表并作为 §12.7 验收项。

2. **sequence 键 (connectionFence, direction, streamId)：接受。** 双工共用计数器确实是我的疏漏；device-control 与 window/task-run 两类 stream 的划分同意（握手、窗口注册、INPUT_LANE_FREE 归 device-control）。fence 语义修正为：**服务端原子换代、单调不可回退、旧 socket 立即失效**，客户端只是携带者不是发放者。gap → 停下 + RESYNC，不跳号执行——这与 §7.1"云端决定 RESUME/RESYNC"闭环。

3. **ActionPlan 栅栏字段：接受。** leaseId/leaseEpoch、basedOnFrameId(或显式 no-frame)、expected bindingGeneration、opaque playerIdentityEpoch、policy/asset version 全部进入执行型 payload 必填字段。**HWND compare-only 特别赞成**——这正是 Q1 前 QA 里"blank HWND fallback 伪装干净"P1 的教训：本地必须由 windowId 经受信 WindowRegistry 解析当前 HWND 再比对 generation，云端下发的 handle 只作证据永不作寻址。

4. **messageId/actionId 分离 + 签名覆盖 + 严格解码：接受。** 投递身份与副作用身份分离写进字段规则；签名覆盖规范化 envelope + payload digest + frame/template hash；执行型 payload 未知 field/enum fail-closed（非执行型可容忍前向兼容字段，需在 schema 里按消息类型标注）；ACTION_OUTCOME 同 actionId 同 digest 幂等返 ACK、不同 digest 冲突拒绝；correlationId/causationId 链路字段同意，它同时服务 §12.7 的端到端 trace。

5. **plan 形状限制：接受，并采纳更严选项。** 现有 executor 先例本就符合：扁平 InputStep 列表、无循环分支、单次 submitExclusiveAndWait、maxInputSteps=12——exclusive callback 里不进 capture/HTTP/OCR/业务分支这条直接成文。SLEEP 硬上限同意。KEY_DOWN/KEY_UP 问题采纳 B 的保守案：**V1 只暴露 KEY_PRESS/HOTKEY**（现有 9 热键 ALT allowlist 场景全覆盖），不暴露裸 DOWN/UP；若 Q1 矩阵盘点发现真实存在"长按"业务需求，再按"本地安全层保证 stop 时释放已按下键并上报 cleanup fact"的设计单独开题，不默认放行。

6. **无本地持久台账的四前提：全部接受。** (a) 云端发送前持久化 DISPATCHED——现有先例支持（XiuluoBrainSessionStore 的 actionLedger 是 temp+fsync+ATOMIC_MOVE 落盘后才下发），生产态由 PostgreSQL 承担，Redis 只做热索引，同意"易失缓存不单独承担真相"；(b) high-water mark + 有界 outcome cache + `DUPLICATE_OUTCOME_EVICTED`（宁可报逐出、绝不当新 action 执行）——接受，我收回"N 覆盖 expiry"的推导，B 说得对，速率与重连使上界不可靠；(c) 重启报 `LOCAL_LEDGER_RESET`、对账完成前不接业务 plan——接受，与 §7.1 重连流程合并为同一状态机；(d) DISPATCHED-无可信-outcome 一律判 UNKNOWN、禁止推断 NOT_EXECUTED——这就是 E4 本义，接受。

7. **P2 时钟语义：接受，且有实证支撑。** expiry 以服务端时间为准 + 声明最大允许偏差 + 本地用单调时钟计量剩余有效期（不用墙钟比较）。前期 QA 恰好抓到过同类缺陷（云端 session 推进路径缺 Math.max 时钟回拨保护，回拨即自 DoS），说明这类规则不写死必然出事。协议硬上限清单（inline ROI 字节数、JSON 大小、步骤数、sleep、observation 时长）同意进 schema 常量表。

**A-PASS**（基于开题六块 + B 六项修正 + P2 时钟条款的合并方案）。待 B 复核后 CLOSED 并回写 §12.2。

B (2026-07-12 #2):

A 已完整解决 STOPPED/OBSERVED 重放矩阵、双向 stream sequence、原子 connection fence、lease/window 栅栏、幂等身份、单 input-bundle 以及无本地持久台账四前提；这些部分无剩余 P0/P1/P2。Q2 暂时仍不 B-PASS，只剩一个 P1 和一个 P2 需要写死：

1. **P1 stale-frame 栅栏仍不完整。** `basedOnFrameId` 本身只是引用，不证明该 frame 仍适用于当前窗口。攻击/故障场景是：云端用很早以前的截图生成一个刚签发、expiry 尚新的 ActionPlan，本地若只检查 action expiry 就会在已经变化的 UI 上点击。执行型 plan 必须携带并由本地 frame ledger 核验：`basedOnFrameId`、expected frame hash、captureTime、捕获时的 bindingGeneration、窗口 geometry/DPI/coordinateSpace，以及协议固定上限内的 `maxFrameAge`。`no-frame` 不能只是任意布尔值，必须是 schema allowlist 中明确允许无视觉依据的 primitive/reason；其他输入计划缺 frame 一律拒绝。
2. **P2 签名字节规范必须唯一。** “规范化 envelope”需要指定唯一算法，例如 RFC 8785/JCS 后排除 signature 字段再签，或签协议定义的精确 UTF-8 字节帧；不能留给客户端和服务端各自序列化。签名覆盖 connectionFence、direction、streamId、sequence、messageId、payload digest、frame/template hash。非执行型消息若允许前向扩展，只能忽略 schema 明确声明的 namespaced extension，身份/时序/安全字段和 enum 仍不得容忍未知或重复字段。

其余结论我确认：V1 可以不做本地跨重启文件台账；云端 PostgreSQL write-before-send action ledger 是权威，Redis 只作热索引；本地用 connectionFence 内 high-water + 有界 outcome cache，逐出重复只能拒绝；重启后 `LOCAL_LEDGER_RESET` 并在完整对账前禁执行。

**B-PASS 条件：**A 接受上述 frame freshness/hash/geometry 栅栏和唯一签名字节规范。补齐后 Q2 可直接 B-PASS，无需重新打开前六项。

A (2026-07-12 #3):

两项都接受，各补一个实施细节：

1. **stale-frame 栅栏：接受全字段。** B 的攻击场景成立——action expiry 只证明"计划签发得新"，不证明"计划依据的画面还新"，两者是独立时效。执行型 plan 必载：`basedOnFrameId` + expected frame contentHash + captureTime + 捕获时 bindingGeneration + 窗口 geometry/DPI/coordinateSpace + 协议上限内的 `maxFrameAge`，由本地 frame ledger 逐项核验，任一不符拒绝。补一个与上轮 P2 时钟条款的衔接：**frame 年龄用本地单调时钟计量**——captureTime 本来就是本地打的（frame 是本地采的），所以 `age = 本地单调now − 本地单调capture标记`，完全不经墙钟、不受云端时钟偏差影响，这条栅栏因此天然免疫时钟跳变。`no-frame` 同意做成 schema allowlist（初始成员预期只有 FOCUS_WINDOW / REPORT_WINDOW_STATE 这类无视觉依据原语），allowlist 之外缺 frame 一律拒绝，新增成员=schema 变更需走评审。

2. **签名字节规范：接受，且在 B 给的两案里明确选"精确字节帧"。** 理由：JCS/RFC 8785 在 Java 侧要引入或自实现规范化器，规范化代码本身成为新的攻击面与双端不一致来源；而"签发送的原始 UTF-8 字节帧 + detached signature（签名放帧外的固定前缀/头部，不参与被签内容）"没有第二次序列化，双端不可能对"签了什么"产生分歧，实现只有 hash+verify 两步。字段覆盖按 B 清单：connectionFence、direction、streamId、sequence、messageId、payload digest、frame/template hash 全部在被签字节内。非执行型消息的前向扩展收窄为"仅 schema 声明的 namespaced extension 字段可忽略，身份/时序/安全字段与 enum 未知或重复即拒"——同意，写进解码规则。

**A-PASS**（合并方案 = 开题六块 + B 六项修正 + P2 时钟条款 + frame 栅栏 + 精确字节帧签名）。待 B 确认后 CLOSED 并回写 §12.2。

B (2026-07-12 #3):

A 已明确接受并补齐 stale-frame 的 frameId/hash/captureTime/binding/geometry/DPI/coordinateSpace/maxFrameAge 全字段核验，也明确选定精确 UTF-8 字节帧签名和 namespaced extension 规则。结合 A#2，Q2 的重放、fencing、错窗、幂等、原子输入、断线台账和时钟语义均无剩余 P0/P1/P2。

签名实施时再固定一个由现有认证边界直接推出的要求：detached signature 的算法与验签 key 由已认证连接/协议版本绑定，未签名前缀不得让发送方选择算法或 key，避免 algorithm/key confusion。该要求已回写第 12.2 节，不改变 A 选择的精确字节帧方案。

**B-PASS:** Q2 通过。状态置为 CLOSED；本轮只完成 Q2 共识收口，等待 A 发起 Q3，不提前讨论下一题。

### Q3: §12.3 权威状态模型——实体关系、持久化边界与恢复决策

状态: CLOSED（A-PASS + B-PASS；权威状态、PG lease fence 与 handoff/drain barrier 已收口）

A (2026-07-12 #1):

Q3 的核心风险不是"状态存哪"，是**两条铁律在状态层的落地**：(1) 唯一权威（§3.1）——任何状态都必须有且只有一个可指认的权威副本；(2) E4——恢复路径上任何环节都不得把"不知道"推断成"没执行"。我分七块，两处给多方案+推荐，末尾留真分歧。

**1. 实体层级与身份。**
`tenant → user → device → clientSession(含 connectionFence) → window → taskRun → attempt(phase 序列)`，外加两个横切实体：`lease`（输入权租约）与 `actionLedgerEntry`。身份规则沿用已实证先例：**需要幂等的 ID 用确定性推导**（现有 sessionId=SHA256(authScope|windowId|taskRunId|startRequestId)，同一 START_TASK 重放天然撞同一 session，不产生分裂脑），其余服务端生成。身份栅栏字段的归属：bindingGeneration/playerIdentityEpoch 挂 window 实体，leaseEpoch 挂 lease，connectionFence 挂 clientSession——每个栅栏有唯一宿主，禁止在多个实体上各存一份。

**2. 权威状态的持久化形态，两案+推荐：**
- 案 A（推荐）：**每 (window,taskRun) 一份快照文档 + revision CAS**，写 PostgreSQL；这是现有 XiuluoBrainSessionStore 的模型（revision CAS + 终态归档命名空间 + PENDING/ARCHIVING 有界对账），已实证过并发安全与恢复语义，迁到 PG 只是换存储引擎不换语义。
- 案 B：event sourcing（append-only 事件流，状态由重放导出）。审计与回放强，但引入投影一致性、schema 演进、重放正确性三类新问题，V1 复杂度不划算。
- 结论：案 A 为权威 + **另设 append-only 审计/trace 流（非权威，可丢）**服务 §12.7 的端到端 trace——审计要 append-only 的好处，权威要快照的简单，两者分开各取所需，别合成一个 event sourcing。

**3. Redis/PG 写入边界，用一条可判定规则代替逐项枚举：**
**"Redis 全丢后，系统必须仅凭 PG + 本地对账恢复到安全状态；丢失的最大代价是一次 RESYNC，绝不是业务错误。"** 由此自动导出归属：PG=dispatched action ledger（Q2 已定 write-before-send）、taskRun/phase 状态、记忆/配置/审计；Redis=心跳、presence、去重热窗、lease 热索引、session 路由缓存。写序：PG 先于下发，Redis 事后更新、允许滞后。每个新增状态项入库前必须先回答"Redis 丢了它会怎样"，答案不是 RESYNC 的就进 PG。

**4. 防无界增长（前 QA F5 的教训直接进设计）：**
现侧车实现每请求全量读+全量重写 session JSON、start 扫全目录，长任务线性劣化。目标态规则：状态文档按 (windowId,taskRunId) 定位（O(1) 键访问，不扫描）；actionLedger 只在活跃 attempt 内保留全量，attempt 终态即压缩为摘要（终态归档先例）；PENDING/ARCHIVING 对账保持有界（Task 4 Repair 先例）。

**5. 重连决策表（§7.1 的 RESUME/RESYNC/RESTART 判定输入显式化）：**
输入 = {新 connectionFence, 客户端上报的最后确认 sequence/action/执行事实, LOCAL_LEDGER_RESET 标志, bindingGeneration 比对, 完整截图}。判定：**RESUME** 仅当 ledger 无 UNKNOWN 空洞、sequence 连续、binding 未变；**RESYNC**（默认路径）= 有 gap、有 UNKNOWN、或 LOCAL_LEDGER_RESET——云端重新观察完整帧、对账 ledger、以观察结果闭合 UNKNOWN；**RESTART_TASK_RUN** 是业务决策（taskRun 已终态/不可恢复），由业务 Service 判，不由传输层判。铁律：**不确定时向 RESYNC 收敛，永不为了省一次观察把 UNKNOWN 记成 NOT_EXECUTED**。

**6. 云端 crash 恢复：**
Cloud Brain 重启后 PG 即真相；所有 DISPATCHED-无-outcome 一律判 UNKNOWN 并触发对应窗口 RESYNC（与 Q2 第 6.d 同源）；Redis 惰性重建。V1 单实例（§9 允许后扩），但 schema 预留 `ownerInstanceId`（可空）字段，横向扩展时用它做窗口归属，不用改表。

**7. 时间戳规则：**
所有状态推进的 updatedAt 必须 `Math.max(now, previous.updatedAt)` 单调防回拨——前 QA 实证过：现实现 archive 路径有此保护、advance 路径没有，时钟回拨会让"updatedAt≥createdAt"校验自锁（NON_REPLAYABLE 自 DoS）。这条作为状态层通则写死，不留"某条路径忘加"的机会。

**留给 B 的真分歧：lease 的权威放哪。** 我的倾向：**lease 是导出性/短时状态，权威放 Redis**（热路径每 action 都要验，PG 扛不住也不该扛），其安全性不靠存储可靠性保证，而靠三重防线：本地 InputLane 物理串行（错发 lease 最多两窗口计划排队，不会输入交叉）+ ActionPlan 携带 leaseEpoch 由本地栅栏拒旧 + Redis 丢失即全窗口 lease 作废走 RESYNC 重授。代价是 Redis 故障时多一次全体重授。若 B 认为 lease 必须 PG 持久（例如担心 Redis 丢失瞬间的双授租约），请给出三重防线挡不住的具体交叉场景，否则 V1 从简。

B (2026-07-12 #1):

快照 + revision CAS、Redis 全丢只能导致 RESYNC、O(1) 定位和默认 RESYNC 方向都正确。但 Q3 不能 B-PASS，当前有以下 P1 状态一致性缺口。

1. **Redis-only lease 的三重防线挡不住“顺序执行两条错误计划”。** 具体交叉场景：W1 持有只存在 Redis 的 lease，W1 ActionPlan 已到本地队列但尚未执行；Redis 丢失后云端认为无 lease，给 W2 重授；W2 plan 到达。本地 InputLane 只保证 W1/W2 不同时输入，却可能先后执行两条计划。旧 W1 plan 的 leaseEpoch 在本地仍是当时合法值，而 Redis 作废事件无法保证先于已排队 plan 到达。结果不是输入交叉，而是 stale business action 被顺序执行。因此 lease 必须拆成两层：
   - PostgreSQL 权威保存每个物理 input lane 的 holder 与单调 fencing generation，grant/revoke 与 task snapshot/action ledger 同事务提交；
   - Redis 只保存 heartbeat、短时 expiry、presence 和热索引；Redis 丢失不得重置或复用 fencing generation，重授仍须 PG CAS 增长后才能下发。
2. **lease 的资源作用域必须是设备物理输入 lane，不是 window。** 一个设备上的多个窗口共享鼠标键盘，所以权威键应类似 `(tenantId, deviceId, inputLaneId)`，holder 才是 `(clientSession/window/taskRun/action)`。如果按 window 各存一份 lease，多个窗口会同时“合法持有”。本地 InputLane 是最后的物理互斥，不是云端调度权威。
3. **状态推进、action write-before-send 与 lease 决定必须有一个原子事务/outbox。** 仅说 snapshot CAS 不够。接受 outcome、校验 outcome digest、推进 revision/phase、记录 successor action、更新 active task-run index、决定 KEEP/RELEASE lease、写 outbox 必须在同一 PG 事务里；提交后 dispatcher 才发送固定 messageId/actionId。否则 crash 会产生“phase 已推进但 action 未记账”或“action 已发但 snapshot 仍允许生成第二 successor”。同一 outbox 重投保持原 IDs，由 Q2 本地幂等兜底。
4. **UNKNOWN 不能被后来截图改写或“闭合”为 EXECUTED/NOT_EXECUTED。** fresh observation 可以让云端把 task state 重同步到一个可继续的业务状态，但原 action ledger 的历史执行事实必须永久保持 UNKNOWN，除非迟到的可信同 digest outcome 到达。恢复应另记 `recoveryDisposition/resyncDecision` 和 successor 因果链，不能篡改 UNKNOWN 证据。
5. **实体层级缺少进程重启后的 window incarnation/重新绑定模型。** `clientSession` 应表示客户端进程生命周期，`connectionFence` 是其下每次 socket 换代；window 需要独立 `windowRegistrationId/windowIncarnation`。本地进程重启产生新 clientSession 和新 window incarnation，旧 taskRun 不能凭同一个标题/HWND 自动挂回，必须由云端在完整截图、设备身份和 binding facts 对账后显式 rebind/resync。否则“taskRun 在重启后继续”与“window 是 clientSession 子实体”互相矛盾。
6. **“每个栅栏只有一个宿主”要区分设备事实与云端期望。** 当前 HWND、geometry、DPI、bindingGeneration 的事实权威是本地受信 WindowRegistry；云端 window/task snapshot 保存的是 last-confirmed/expected 值，用于签发和比对，不是第二份当前真相。playerIdentityEpoch 若由云端业务识别产生，则云端是权威、本地只保存 opaque expected token。矩阵必须给每个字段写 `authoritative source` 与 `cached/expected copy`，不能用“禁止复制”掩盖事实快照。
7. **审计与压缩不能统一标成可丢。** 普通性能 trace 可以按采样和保留期丢弃；但 action ledger、人工管理修改、公共记忆发布/降权/回滚和安全认证审计必须持久且不可静默丢失。attempt 终态压缩前必须保留 actionId、status、outcomeDigest、fence、因果链和 UNKNOWN/非重放 tombstone，至少覆盖协议去重窗口及第 10 节证据保留期；不能压缩后让旧 actionId 重新变成“未见过”。

补充两个 P2：

- revision/sequence 才能决定状态先后；updatedAt 只作审计展示。多实例下应用进程的 `Math.max(now, previous)` 仍受跨节点时钟影响，应由 PostgreSQL transaction time + `GREATEST` 生成单调展示值，不能成为 CAS 或恢复条件。
- `ownerInstanceId` 是 WebSocket 路由/presence 的短时事实，应该在 Redis 或连接注册表，不应进入 PG 权威 task snapshot；目标已明确不依赖 sticky instance，实例更换不应构成业务状态推进。

**B 对 lease 分歧的结论：**V1 采用“PG 权威 fencing generation + Redis 活性/热索引”。这不会要求每次 action 都重新争抢 PG lease：KEEP_LEASE 期间沿用已持久 generation；只有 grant/revoke/holder 变化才 PG CAS。每次 action 本来就需要 PG write-before-send ledger，可在同一事务验证当前 holder/epoch，不增加第二次业务往返。

**B-PASS 条件：**A 接受 device input-lane 作用域的 PG 权威 lease fence、原子事务/outbox、UNKNOWN 历史不可改写、window incarnation/rebind、物理事实与 expected copy 分工、持久审计/非重放 tombstone，以及两个 P2 修正。完成前 Q3 保持 OPEN。

A (2026-07-12 #2):

七项 P1 + 两项 P2 全部接受。第 1 点我正面认输并复盘错在哪：

1. **lease 分歧：B 胜，PG 权威 fencing generation 成立。** 我的三重防线有一个未声明的假设——"Redis 丢失时云端知道自己丢了、且作废信号能赶在已排队 plan 执行之前到达本地"。B 的场景两个都不成立：危险不是输入交叉，是**云端遗忘已授租约后重授，两条业务上互斥的计划被本地顺序合法执行**。物理串行挡不住"顺序执行两条错计划"，leaseEpoch 栅栏挡不住"旧 epoch 在本地视角仍是当时合法值"。真正的修复只能在源头：PG 权威保存 holder + 单调 fencing generation，Redis 丢失时云端**不会遗忘授权**，错误重授根本不发生。B 关于成本的论证也成立——action 下发本来就有 write-before-send 的 PG 事务，holder/generation 校验搭同一事务，零新增往返。接受"PG 权威 + Redis 只做活性/热索引、丢失不得重置或复用 generation"。

2. **lease 作用域 = (tenantId, deviceId, inputLaneId)：接受。** 这与物理现实和现实现都对齐——一台设备一套鼠标键盘，现 TaskTurnCoordinator 的互斥本来就是设备级的；holder 才是 window/taskRun。按 window 各存一份会造成多窗口同时"合法持有"，此错误形态在 Q1 矩阵里也要给 TaskTurnCoordinator 行标注（迁移后资源键变化）。

3. **原子事务 + transactional outbox：接受。** outcome 接收→digest 校验→revision/phase 推进→successor 记账→lease 决定→outbox 写入，同一 PG 事务；提交后 dispatcher 才发送，重投保持固定 messageId/actionId、由 Q2 本地幂等兜底。这同时把 Q2 的"write-before-send"从单条规则升级成结构保证——不存在"发了没记"或"记了两个 successor"的 crash 窗口。补一条实施要求：outbox 消费者必须容忍 at-least-once（重发同 IDs 是常态不是异常），监控上重投率单列，掩盖不了的投递故障要报警而不是靠幂等静默吸收。

4. **UNKNOWN 历史不可改写：接受。** 台账是证据不是工作区——fresh observation 只能产生新的 `resyncDecision` 与 successor 因果链，把 task state 重同步到可继续状态；原 entry 的 UNKNOWN 永久保留，唯一例外是迟到的可信同 digest outcome。这是 E4 在状态层的完整表达，比我开题写的"以观察结果闭合 UNKNOWN"更准确——"闭合"的是业务不确定性，不是证据记录，我的措辞会诱导实现者改写台账，收回。

5. **window incarnation / rebind：接受。** 层级修正为：clientSession=客户端进程生命周期，connectionFence=其下 socket 换代序列，window 带独立 windowRegistrationId/incarnation。进程重启 = 新 clientSession + 新 incarnation，旧 taskRun 只能由云端在完整截图 + 设备身份 + binding facts 对账后**显式 rebind**，永不凭标题/HWND 自动挂回——HWND 是可复用句柄，凭它自动挂回就是错窗输入的入口，这与 Q2 的 HWND compare-only 同根。

6. **authoritative source / expected copy 分栏：接受。** 我开题的"每个栅栏唯一宿主"确实混淆了两件事：**权威唯一**（成立）与**禁止副本**（不成立也做不到）。修正为每个字段两列注记：authoritative source（HWND/geometry/DPI/bindingGeneration 权威在本地受信 WindowRegistry；playerIdentityEpoch 权威在云端业务识别，本地只持 opaque expected token）+ cached/expected copy 的位置与刷新时机。此分栏并入 Q1 矩阵字段级章节。

7. **审计分层 + 非重放 tombstone：接受。** "可丢"仅限采样性能 trace；action ledger、人工管理操作、公共记忆发布/降权/回滚、安全认证审计为持久层级。attempt 终态压缩保留 {actionId, status, outcomeDigest, fence, 因果链, UNKNOWN/非重放 tombstone}，覆盖期 ≥ max(协议去重窗口, §10 对应证据保留期)；压缩永不使旧 actionId 变回"未见过"——否则 Q2 的重复拒绝在长任务尾部失效。

8. **P2 两项：接受。** (a) 状态先后由 revision/sequence 决定，updatedAt 降级为审计展示值，由 PG transaction time + GREATEST 生成单调展示，不参与 CAS 与恢复判定——这比我开题的"应用层 Math.max 通则"更对，应用层单调在多实例下本来就不成立，我那条只在单实例侧车语境里有效；(b) ownerInstanceId 收回出 PG snapshot，放连接注册表/Redis，实例更换不构成业务状态推进。

**A-PASS**（合并方案 = 开题七块经 B 七项 P1 + 两项 P2 修正后的版本；lease 采用 B 案：PG 权威 fencing generation + Redis 活性）。待 B 确认后 CLOSED 并回写 §12.3。

B (2026-07-12 #2):

A 已解决 B#1 的七项 P1 与两项 P2，尤其是 PG 权威 fencing generation、device input-lane 作用域、事务 outbox、UNKNOWN 不可改写和 window incarnation 均成立。但复核后仍发现一个 P1：**持久 epoch 防止云端遗忘旧 lease，却不能单独证明本地旧计划已经退出队列。**

正常或强制换手都存在同一竞态：W1 的 epoch=10 plan 已排入本地 InputLane；云端 PG CAS 把 holder 改为 W2/epoch=11；在 epoch=11 到达本地并更新本地 fence 之前，W1 仍可能按本地已知 epoch=10 通过检查并执行。PG 单调性防止 token 重用，但没有建立“旧 holder 已排空”的 happens-before。

需要增加显式 lease handoff barrier：

1. lease 状态至少包含 `HELD -> REVOKING -> FREE/HELD`，holder 变化不能从 HELD 直接跳到另一个 holder；
2. 正常 RELEASE 必须由当前 holder/epoch 的可信 outcome 或 `INPUT_LANE_DRAINED` 触发，证明该 device input lane 中旧 epoch 的计划已执行完、取消或拒绝；随后 PG CAS 增长 epoch，再向新 holder 下发；
3. 强制 revoke（连接丢失、stop、进程重启）先使旧 connectionFence 失效并把所有 outstanding action 保持/标记 UNKNOWN；在收到同一受信设备 agent 的 `LOCAL_LEDGER_RESET + INPUT_LANE_DRAINED` 对账前，不得给该物理 lane 新 holder；
4. 若无法取得 drained ACK，只能等待所有旧 plan 的签名 expiry 加时钟偏差安全窗后再重授，并把该过程记录为 forced-handoff/resync；不能仅因 Redis heartbeat/lease expiry 到时就立刻重授；
5. 本地 InputLane 必须保存“当前已接受的最高 leaseEpoch”，每次真正进入 exclusive callback 和每个物理步骤前重检；收到更高 epoch/revoke 后，旧 epoch 尚未开始的请求全部拒绝，已开始的请求按 Q2 STOPPED/UNKNOWN 规则上报。

这不是新的业务调度逻辑：本地仍不选择 holder，只执行云端 fence 并报告 lane 是否排空；谁获得下一 lease 仍完全由云端决定。

**B-PASS 条件：**A 接受上述 lease handoff/drain barrier，并明确 Redis expiry 只能触发 REVOKING/RESYNC，不能直接触发新 holder grant。其余 Q3 条件无需重开。

A (2026-07-12 #3):

接受全部五条 handoff/drain barrier，并明确确认：**Redis expiry 只能触发 REVOKING/RESYNC，永不直接触发新 holder grant**。B 的竞态成立——PG 单调性回答"token 不会被复用"，回答不了"旧 token 的计划是否还在管道里"，这是两个正交问题，我在 #2 里只修了前者。

逐条确认与两点补强：

1. `HELD → REVOKING → FREE/HELD` 状态机接受，holder 永不 HELD→HELD 直跳。
2. 正常 RELEASE 由当前 holder/epoch 的可信 outcome 或 `INPUT_LANE_DRAINED` 触发后才 CAS 增长 epoch——接受。**补强提案 A-1：V1 叠加"每设备 lane 至多一个 outstanding 执行计划"的单飞纪律。** 单飞下常规路径的排空证明就是最后一条 outcome 本身（收到 outcome=lane 已空），`INPUT_LANE_DRAINED` 专用于取消/拒绝/超时等无 outcome 兜底路径。这不是替代 barrier，是让 barrier 在 99% 路径上可以用已有消息直接证明，减少一类新消息的高频往返。单飞纪律同时简化 Q2 的本地队列语义（排队深度≤1，"已排队未执行"窗口最小化）。若后续多窗口吞吐需要放宽，再按 barrier 全量语义处理多 outstanding。
3. 强制 revoke 三步曲（fence 失效 → outstanding 全标 UNKNOWN → 等 `LOCAL_LEDGER_RESET + INPUT_LANE_DRAINED` 对账）接受；对账前该物理 lane 不授新 holder。
4. 无 drained ACK 时等"签名 expiry + 时钟偏差安全窗"再重授并记 forced-handoff——接受。注意这里 expiry 用的是 Q2 已定义的服务端时间语义，安全窗常数进协议硬上限表，不留实现自定。
5. 本地 InputLane 保存已接受最高 leaseEpoch、exclusive callback 进入前 + 每个物理步骤前重检——接受。**补强 A-2：这与现有 executor 的每步检查点结构天然合拍**（CloudActionExecutor 本就逐 InputStep 打检查点），epoch 重检挂在同一检查点上实现，已开始的计划收到更高 epoch 时按 Q2 规则报 STOPPED（未开始步骤不再执行）或整计划 UNKNOWN，不产生新的 outcome 词汇。

边界确认：barrier 全程本地零调度权——本地只执行 fence、报告排空事实，holder 选择仍 100% 在云端，与 §3.3/§6 无冲突。

**A-PASS**（Q3 最终合并方案 = A#1 七块 + B#1 九项修正 + B#2 handoff barrier + A-1 单飞纪律 + A-2 检查点挂载）。待 B 确认后 CLOSED 并回写 §12.3。

B (2026-07-12 #3):

A#3 已明确接受 `HELD -> REVOKING -> FREE/HELD`、正常与强制换手的 drain barrier、本地逐物理步骤 epoch 重检，以及“Redis expiry 只能触发 REVOKING/RESYNC，不能直接重授 holder”。因此 B#2 的最后一个 P1 已解除。

A-1 的单飞纪律可以作为 V1 简化，但必须固定一个实施不变量：可信 outcome 只有在 exclusive callback 已退出、全部物理步骤结束且 lane 已释放后才能充当 drain proof，并且必须绑定 actionId、leaseEpoch 与 connectionFence；否则仍需显式 `INPUT_LANE_DRAINED`。这由 Q2 已确认的“回调内不得进行网络/OCR/业务处理”与本题 barrier 直接导出，已回写 §12.3，不引入本地调度权。

复核后 Q3 在唯一权威、双重副本边界、错窗重绑、消息乱序、断线恢复、lease 换手、审计压缩和多实例时间语义上均无剩余 P0/P1/P2。

**B-PASS:** Q3 通过。状态置为 CLOSED；本轮只完成 Q3 共识收口，等待 A 发起 Q4，不提前讨论下一题。

### Q4: §12.4 记忆与资产模型——数据模型、verifier 定义、发布判定与版本竞争

状态: CLOSED（A-PASS + B-PASS；因果记忆、公共发布、版本竞争与资产撤销已收口）

A (2026-07-12 #1):

§8.2 的硬边界已定（自动发布不得改人工、3 次跨 run 成功、冲突生成新版本竞争），Q4 要补的是把"成功""近期""verifier"这些词变成可判定的定义。六块意见 + 一个真分歧。

**1. 统一 MemoryRecord 数据模型（覆盖现 Vision Memory 等价物）：**
`{memoryId, scope: PRIVATE(tenantId,userId) | PUBLIC_CANDIDATE | PUBLIC_PUBLISHED, kind(npc 坐标/dialog recipe/路线/模板 ROI/阈值…), contextKey(任务×地图×NPC×动作的规范化键), payload, version + lineage(parentVersionId), provenance{sourceUserId, 贡献 taskRunIds, 证据 frame 引用}, stats{successCount, failCount, distinctTaskRunIds, lastSuccessAt, lastFailAt}, state: ACTIVE|DEMOTED|QUARANTINED|RETIRED, createdBy}`。写 PostgreSQL（Q3 规则：丢了会错业务的进 PG）；证据 ROI 图进对象存储按 §10 保留期。现有本地 Vision Memory 文件在建设期做一次性导入，`provenance=IMPORTED_LOCAL_BASELINE` + 冻结基线 commit 标注，默认进该用户 PRIVATE，不直接进公共池。

**2. verifier 的可判定定义（§8.2 留白的核心）：**
verifier 不是新组件，是**业务 Service 定义的后置条件检查**：某次真实 taskRun 中使用该 memory 后，云端用后续观察事实验证"记忆预言的效果发生了"（例：按记忆坐标点 NPC → 后续帧出现该 NPC 的 Dialog）。记录形态 = `{memoryId, taskRunId, verdict: SUCCESS|FAIL|INCONCLUSIVE, evidence frameId, verifierRule 版本}`。三条硬规则：(a) verdict 必须绑定证据帧，无证据不计数；(b) INCONCLUSIVE 不计成功也不计失败（比照 UNKNOWN 哲学：不确定不推断）；(c) verifier 规则本身有版本，规则变更后历史 verdict 不重算、只标注规则版本，避免"改规则=篡改统计"。

**3. "3 次不同任务运行成功 + 近期无失败"的精确化：**
成功计数键 = distinct taskRunId（同 run 内重复使用只计 1）；发布门 = `distinctTaskRunIds ≥ 3 AND 近期窗口内 failCount = 0`；**近期窗口建议双条件取严：最近 20 次使用 AND 最近 7 天，任一窗口内有 FAIL 即不发布**。发布判定在 verifier 写 verdict 的同一 PG 事务里原子评估（Q3 事务先例），并发第 3 次成功也只发布一个版本，无竞态。发布动作 = state 置 PUBLIC_PUBLISHED + 审计记录（持久审计层级，Q3 #7），自动、不等人工，符合硬边界。

**4. 失败衰减与 quarantine：**
FAIL 即时效果 = 该版本选择权重降级（DEMOTED 阈值：连续 3 FAIL 或滑窗失败率 >30% 且样本 ≥5）；QUARANTINED = 管理员手动或"发布后窗口内失败率 >50%"自动触发，隔离即从选择集移除但保留记录与统计（可回滚）。衰减只作用于**选择权重**，不删除数据——与"冲突生成新版本、不覆盖历史"同一精神。

**5. 签名资产与客户端缓存（这里 Thin Client 架构送了个安全红利）：**
资产按 contentHash 寻址存对象存储，签名绑定已认证连接的 key 语义（Q2 已定）；本地 SignedAssetCache 只按 hash 拉取、验签+验 hash 后落缓存。**撤销不需要向客户端推送失效**：本地永远不自主选模板——每次使用都由云端 plan 显式引用 templateHash，撤销=云端停止引用+发布替代版本，旧缓存条目自然死亡（无引用即无用），惰性逐出即可。客户端缓存中毒不可能成立：改了内容 hash 就对不上，plan 引用的 hash 本地没有就按 Q2 资产条件拒绝并回报，云端改发内联 payload 或新 hash。
**6. 用户配置与公共资产版本的关系：**
生效策略 = `merge(公共已发布版本集, 用户私有 override)`，云端计算；**taskRun 启动时快照一份 effective policy/asset 版本集记入 taskRun**，运行中不漂移（延续既有"会话一致性"原则：策略校准不得中途改变业务行为），重现/审计都指向该快照。公共资产升版只影响新 taskRun。

**留给 B 的真分歧：版本竞争的选择策略。** 同一 contextKey 多版本并存时怎么选？我推荐 **V1 确定性择优**：score=近期加权成功率(样本量下限保护)，最高分者被选，平分取最新发布；不引入 epsilon-greedy/bandit 探索。理由：探索=故意用次优版本换信息，在别人的真实任务里做实验，V1 用户量(10 并发)撑不起探索的统计意义,却实打实付失败成本;新版本的"竞争入场"机会由其贡献者自己的私有成功记录自然提供(私有一次成功即可用→积累→进候选→发布),不需要在公共选择层注入随机性。B 若认为无探索会让次优版本永久锁死（马太效应），请给出比"贡献者私有路径入场"更必要的探索场景，否则 V1 从简。

B (2026-07-12 #1):

PostgreSQL 权威、verifier 版本化、同一 taskRun 固定 effective policy，以及 V1 不做随机探索的方向可以接受；但当前不能 B-PASS。现有仓库已经给出两个不能忽略的反例：`PACKAGE_ARCHITECTURE.md` 的 CR169 明确记录“后续出现 dialog 不能证明先前 memory click 导致了它”，并因此停用了无因果绑定的 learned memory；CR185 又明确要求云端完整保留旧 `vision-memory-v2` 的 `entries/npcClickSamples/policies` 全字段，且私有一次强成功可 trial、trial 失败立即降权停用。A#1 当前会重新引入这两类已知问题。

1. **P1：verifier 缺少可证明的因果绑定。** “使用 memory 后的后续帧出现 Dialog”仍可能由同一窗口后续 fallback、另一个候选、人工输入或迟到画面造成。每次真实使用必须先产生不可变 `memoryUseId`，并把 exact `memoryVersionId/contextRevision`、actionId、taskRunId、connectionFence、leaseEpoch、beforeFrameId/hash、实际执行点和观察预算绑定进 action ledger。SUCCESS 只能来自该 action 后、任何其他业务输入/fallback 之前的第一条满足版本化后置条件的可信观察；中间出现其他输入、frame gap、重连或证据链断裂一律 INCONCLUSIVE。动作已执行且在固定观察预算内明确未出现后置条件才是 FAIL。本地只回报原始执行/观察事实，因果解释仍在云端业务 Service。
2. **P1：不能用新的通用 `MemoryRecord` 丢失既有 canonical schema 与坐标适用域。** 初始导入必须逐字段、可逆、幂等地保留完整 `vision-memory-v2`，包括失败、stale、recent samples、absolute/relative click、window base、tune、spread、confidence 等；normalized version 表和可执行索引只能是派生层，不能替代或裁剪 canonical raw store。每种 `kind` 需要强类型 payload + schemaVersion/contentDigest；坐标类 context 还必须包含 game/client build、任务/地图/NPC/动作、window profile、尺寸、DPI、coordinateSpace、识别策略/资产版本与适用约束，不能只用“任务×地图×NPC×动作”合并不同视觉环境。
3. **P1：私有、候选和公共发布不能是同一行直接改 scope。** 只有服务端配置的 `trustedPublisherUserIds` 的因果强验证数据才能派生公共候选；普通用户数据永不进入公共池。PRIVATE 证据、PUBLIC_CANDIDATE 和 PUBLIC_VERSION 应是有 lineage 的独立不可变版本，晋级创建新公共版本，不修改或泄露私有原记录。sourceUserId、原始 frame/ROI 和设备信息保持服务端受限 provenance，公共消费者不可见。用户已确认的私有策略必须单列：一次因果强成功即可成为该用户的 trial；下一次因果 FAIL 立即停止选择，直到后续非该 memory 的 fallback 再产生新的因果强成功。A#1 的“连续 3 FAIL 或失败率 >30%”不能套到私有 trial。
4. **P1：统计必须由 append-only use/verdict 事实幂等派生，不能把可变 counters 当证据。** verdict 唯一键至少包含 `(memoryVersionId, memoryUseId, verifierRuleVersion)`；同一 immutable payload/version 的每个 taskRun 最多贡献一个发布 verdict。同 run 多次使用时，任一可归因 FAIL 应压过 SUCCESS，只有存在 SUCCESS 且没有 FAIL 才贡献一次 SUCCESS，其他为 INCONCLUSIVE，防止重试刷票。公共门槛必须明确为同一版本、可信发布者的 3 个 distinct taskRun 成功；最近 20 次使用或最近 7 天任一集合出现 FAIL 都阻止发布。发布、统计投影、审计和版本唯一约束在同一 PG 事务完成，重复 verdict/outbox 重投不得重复计数。
5. **P1：content hash 只保证完整性，不能替代资产信任和撤销。** “云端停止引用，旧缓存自然死亡”挡不住已入 outbox、已下发或已排队的旧 plan，也挡不住合法签发后发现有害的资产。资产需要不可变 descriptor/manifest：`assetId/version/contentHash/schema/context/signingKeyId/status/assetEpoch/revokedAt`。缓存每次使用前都重新 hash，并验证与当前签名 plan/manifest 一致；计划的连接级签名与可跨连接缓存的资产签名/key rotation 必须分开。普通 RETIRED 可以只影响新 taskRun；安全/错误资产的 REVOKED 必须停止旧 outbox 重投、使相关 active snapshot 进入 REVOKING/RESYNC，并借 Q3 lane drain barrier 清掉已排队旧计划后才能继续，不能等自然过期。
6. **P1：effective policy merge 与运行中撤销边界仍不完整。** `merge(公共集, 私有 override)` 必须是版本化、确定性的强类型规则，只允许 ACTIVE、context 相容且已通过相应门槛的私有版本覆盖；用户配置不得覆盖安全限制、签名策略或撤销状态。taskRun 只快照 immutable version IDs + digests + policyRevision，普通升版不影响在途运行；但紧急 REVOKED 是例外，必须显式终止旧 snapshot 的后续动作并 RESYNC/RESTART，不能让任务继续使用已知有害资产。

补充两个 P2：

- **确定性择优可以接受，但公式不能留成描述性文本。** V1 建议用固定的置信下界（例如带先验的 Beta/Wilson lower bound）+ 明确时间衰减、最小样本和 incumbent hysteresis；3/3 新版本不应仅因表面 100% 立刻压过 95/100 稳定版本。平分优先当前 incumbent，再按稳定 versionId，不能“取最新”制造无证据抖动。scorePolicyVersion 必须进入 taskRun snapshot 和审计。
- **证据与容量边界要补齐。** verifier 只保留最小必要 ROI/crop，按 tenant ACL 加密存对象存储并受用户/设备配额、去重、引用计数和 GC 约束；图片到期后仍保留 evidence digest、frame metadata、verdict 和审计，不得因对象删除让已发布统计失去可验证锚点。

**B-PASS 条件：**A 补齐 causally-bound memoryUse/verdict、旧 canonical schema 无损保留、可信发布者与私有 trial 独立生命周期、幂等 run-level 统计、真正的资产撤销/签名模型、effective policy 的普通冻结与紧急撤销边界，并把两个 P2 固定成可实现规则。完成前 Q4 保持 Review required。

A (2026-07-12 #2):

六项 P1 + 两项 P2 全部接受。我已独立核实 B 的两处引用属实（PACKAGE_ARCHITECTURE.md:5226-5229：用户明确裁决"后续 verifier 看见 dialog 不能证明该 memory click 与 dialog 有因果绑定"并生产禁用 LEARNED_MEMORY；CR185 全字段云端保留要求）。要点是：**这两条不是 B 的新意见，是用户已裁决过的先例**——我开题的 verifier 定义恰好复刻了 CR169 被用户否决的那个错误，B 引先例纠正完全成立，此项无讨价余地。逐项确认：

1. **因果绑定 verifier：接受全链。** `memoryUseId` 不可变、绑定 {memoryVersionId, contextRevision, actionId, taskRunId, connectionFence, leaseEpoch, beforeFrameId/hash, 执行点, 观察预算} 入 action ledger；SUCCESS 仅限"该 action 之后、任何其他输入/fallback 之前的第一条满足版本化后置条件的可信观察"；任何中间输入、frame gap、重连、证据链断裂 → INCONCLUSIVE；观察预算内明确未出现后置条件才是 FAIL。本地只报原始事实，因果解释在云端——与 §3 边界一致。

2. **canonical raw store 无损 + 派生投影：接受。** vision-memory-v2 全字段（entries/npcClickSamples/policies、absolute/relative click、window base、tune、spread、confidence、失败与 stale samples）逐字段、可逆、幂等导入 canonical 层；normalized 版本表与可执行索引只是派生层，可重建、不可替代。每 kind 强类型 payload + schemaVersion + contentDigest。坐标类 contextKey 扩展为含 game/client build、window profile、尺寸、DPI、coordinateSpace、识别策略/资产版本——"任务×地图×NPC×动作"确实合并了不同视觉环境，收回。

3. **三池独立生命周期：接受。** 普通用户数据永不进公共池；trustedPublisherUserIds 的因果强验证数据**派生**公共候选（新不可变版本 + lineage，不改私有原记录）；sourceUserId/原始 frame/设备信息为服务端受限 provenance，公共消费者不可见。私有 trial 语义按 CR185 原样：一次因果强成功 → trial；下一次因果 FAIL → 立即停选，直到后续非该 memory 路径再产生新的因果强成功。我的"连续 3 FAIL/失败率 30%"阈值仅适用于公共已发布版本的 DEMOTED 判定，与私有 trial 分列，两套规则不互串。

4. **append-only 事实 + 幂等派生统计：接受。** verdict 唯一键 `(memoryVersionId, memoryUseId, verifierRuleVersion)`；run-level 聚合规则接受——同 run 内任一可归因 FAIL 压过 SUCCESS，仅"有 SUCCESS 且无 FAIL"贡献一次 SUCCESS，其余 INCONCLUSIVE，防重试刷票；发布门 = 同一版本、可信发布者、3 个 distinct taskRun 成功，且 20 次/7 天双窗口零 FAIL；发布+投影+审计+版本唯一约束同一 PG 事务，outbox 重投不重复计数（Q3 结构直接复用）。counters 一律降级为投影，事实表才是证据。

5. **资产撤销模型：接受，收回"自然死亡"论。** 我只考虑了"未来引用"，B 指出的三个窗口（已入 outbox、已下发排队、签发后发现有害）确实挡不住。接受不可变 asset descriptor `{assetId, version, contentHash, schema, context, signingKeyId, status, assetEpoch, revokedAt}`；缓存每次使用前重新 hash 并对照当前签名 plan/manifest；**资产签名（跨连接缓存、独立 key rotation）与连接级消息签名分离**；RETIRED 只影响新 taskRun，REVOKED 走强路径：停止相关 outbox 重投 → active snapshot 进 REVOKING/RESYNC → 借 Q3 lane drain barrier 清排队旧计划 → 才恢复。撤销语义与 Q3 状态机就此闭环。

6. **effective policy merge 规则化：接受。** merge 为版本化确定性强类型规则；仅 ACTIVE + context 相容 + 已过门槛的私有版本可覆盖对应公共项；用户配置永不覆盖安全限制/签名策略/撤销状态；taskRun 快照 = immutable version IDs + digests + policyRevision + scorePolicyVersion；普通升版不动在途，紧急 REVOKED 为唯一例外，显式终止旧 snapshot 后续动作并 RESYNC/RESTART。

7. **P2 评分公式：接受，并收回"平分取最新"。** B 说得对，取最新会制造无证据抖动。V1 固化：带先验的 Wilson/Beta 置信下界 + 时间衰减 + 最小样本量 + incumbent hysteresis（新版本必须显著优于在位者才换）；平分优先 incumbent，再按稳定 versionId 排序；scorePolicyVersion 进 taskRun snapshot 与审计。3/3 不压 95/100 由置信下界天然保证。

8. **P2 证据容量边界：接受。** 最小必要 ROI/crop、tenant ACL 加密、配额/去重/引用计数/GC；对象删除后保留 evidence digest + frame metadata + verdict + 审计，已发布统计的可验证锚点永不悬空——与 Q3 tombstone 同一精神：删得掉图片，删不掉"它存在过且长这样"的证明。

**A-PASS**（Q4 合并方案 = A#1 六块经 B 六项 P1 + 两项 P2 全量修正；CR169/CR185 用户先例作为约束来源显式入档）。待 B 确认后 CLOSED 并回写 §12.4。

B (2026-07-12 #2):

A#2 已完整接受 B#1 的 canonical raw store、三池隔离、trusted publisher、私有 trial、幂等事实表、资产强撤销和 effective policy 边界，原 6 项 P1 已解除。但因果链与评分/状态机还有以下未定项，当前仍不能 B-PASS。

1. **P1：SUCCESS 必须证明状态转移，而不只是绑定 beforeFrame 后看见 post-state。** 如果 expected dialog 在点击前已经存在，memory action 即使没有产生效果，点击后的第一张图仍会满足“Dialog 可见”。因此 verifierRule 必须声明可判定的 precondition + postcondition（或其他 action-specific transition token）：fresh beforeFrame 必须证明预期结果尚未成立/处于指定前态，postFrame 必须在同一 window incarnation、bindingGeneration 与连续 frame chain 中证明转移发生；before 已满足、前态读不清、frame chain 不连续或期间有任何非该 action 输入都只能是 INCONCLUSIVE，不能 SUCCESS。该 transition proof 与 memoryUseId/actionId 一起落 append-only verdict。
2. **P2：评分仍写成“Wilson/Beta”，没有选定唯一公式和常数。** 两者不是同一算法，时间衰减、最小样本与“显著优于”也仍无法编码。建议 V1 直接固定为：run-level conclusive verdict 按 `w=2^(-ageDays/7)` 衰减；使用 `Beta(1+sumSuccessWeight, 1+sumFailWeight)` 的 5% posterior quantile 作为 score；至少 3 个 distinct taskRun 才参与公共竞争；challenger 只有 `score >= incumbentScore + 0.05` 才换位；否则保留 incumbent，最后才按稳定 versionId 排序。A 可以给出另一套数值，但必须唯一、完整、版本化，不能继续留二选一或未定义参数。
3. **P2：公共 DEMOTED/QUARANTINED 的窗口和恢复仍含糊。** 必须明确统计单位仍是 run-level conclusive verdict，并固定：连续 3 FAIL 或最近 20 个 conclusive run 中失败率 >30% 且样本 >=5 时如何影响选择；“>50% 自动 quarantine”使用哪个窗口/最小样本；是否存在单次严重错窗/越界的立即 quarantine；DEMOTED 是否还能自动首选；QUARANTINED 如何恢复。不同 payload/contentDigest 的 fallback 成功必须创建新的 child version，不能用成功计数改写失败版本的 payload；相同 digest 的恢复也要追加 verdict 与审计后按明确状态迁移，不能覆盖历史。

**B-PASS 条件：**A 补齐 pre-state -> post-state 的因果转移证明，选定唯一评分公式/参数，并固定公共降权、隔离与恢复状态机。B#1 已解决部分不再重开。

A (2026-07-12 #3):

三项全接受，逐项定死：

1. **P1 转移证明：接受。** B 的反例成立——"点击前 Dialog 已在"时，无效点击也会被我 #2 的定义记 SUCCESS，这是残留的因果漏洞。verifierRule 定义升级为**版本化的 precondition + postcondition 对**：fresh beforeFrame 必须证明后置条件尚未成立/处于声明的前态；postFrame 必须在同一 window incarnation + bindingGeneration + 连续 frame chain 内证明转移发生。判定表：before 已满足后置 → INCONCLUSIVE；前态读不清 → INCONCLUSIVE；frame chain 断裂/重连 → INCONCLUSIVE；期间任何非该 action 输入 → INCONCLUSIVE；观察预算内明确未转移 → FAIL；转移证明成立 → SUCCESS。transition proof（before/after frameId+hash+规则版本）随 memoryUseId/actionId 落 append-only verdict。

2. **P2 评分公式：直接采纳 B 的数值，不另起炉灶。** `scorePolicyVersion=1` 固化为：统计单位 = run-level conclusive verdict；权重 `w=2^(-ageDays/7)`；score = `Beta(1+ΣsuccessWeight, 1+ΣfailWeight)` 的 5% posterior quantile；≥3 distinct taskRun 才参与公共竞争；challenger 换位条件 `score ≥ incumbentScore + 0.05`；否则保留 incumbent；终极平手按稳定 versionId 升序。全部常数进协议常量表，改动=新 scorePolicyVersion，随 taskRun snapshot 审计。

3. **P2 降权/隔离/恢复状态机：定死如下（统计单位同上）：**
   - **DEMOTED 触发**：连续 3 个 conclusive FAIL run，或最近 20 个 conclusive run 失败率 >30% 且样本 ≥5。**效果**：完全移出自动选择集（不是降权重续选——被选不到就不会积累新 verdict，这是有意为之的保守设计）。
   - **QUARANTINED 触发**：发布后最近 10 个 conclusive run 失败率 >50% 且样本 ≥4 自动触发；或单次可归因严重事件（该 memory payload 直接导致错窗输入/坐标越界被本地拒绝）立即触发；或管理员手动。
   - **恢复路径**：V1 无自动恢复。DEMOTED/QUARANTINED → ACTIVE 仅两条路：(a) 管理员审计后手动迁移（追加审计记录）；(b) 被新 child version 取代（不同 payload/contentDigest 的 fallback 成功创建带 lineage 的新版本，走完整候选→发布门槛，失败版本原样留档）。同 digest 恢复 = 追加 verdict + 显式状态迁移 + 审计，永不覆盖历史统计。
   - 私有 trial 不适用本状态机（Q4 A#2 第 3 条已分列）。

**A-PASS**（Q4 最终合并 = A#1 经 B#1 六项 P1 + B#2 转移证明修正，评分与状态机按本条定死）。待 B 确认后 CLOSED 并回写 §12.4。

B (2026-07-12 #3):

A#3 已补齐 pre-state -> post-state transition proof，选定唯一 `scorePolicyVersion=1` 及全部常数，并明确公共 DEMOTED/QUARANTINED 的统计单位、阈值、选择效果和恢复路径。B#2 的 1 项 P1 与 2 项 P2 均已解除。

“V1 无自动恢复”作为总规则时，同 digest 的显式状态迁移只能由管理员审计操作触发；不同 digest 的 fallback 成功创建 child version 并重新走候选/发布门槛。该解释与 A#3 的两条恢复路径一致，已回写 §12.4，不新增第三条自动恢复路径。

复核后 Q4 在因果归因、旧 canonical 数据保真、租户/发布者隔离、幂等统计、版本竞争、缓存信任、紧急撤销和证据容量上均无剩余 P0/P1/P2。

**B-PASS:** Q4 通过。状态置为 CLOSED；本轮只完成 Q4 共识收口，等待 A 发起 Q5，不提前讨论下一题。

### Q5: §12.5 容量、背压和隔离——10 并发用户样例下的配额、公平性与故障模式

状态: CLOSED（A-PASS + B-PASS；容量、背压、公平调度与 fenced delivery 已收口）

A (2026-07-12 #1):

Q5 的原则先立住：**容量问题只允许表现为"变慢/暂停/RESYNC"，永不允许表现为"业务错误或本地 fallback"**（§3.3/§7 硬边界的容量侧推论）。数字给默认值+测量修正机制，不拍脑袋当真理。六块 + 一个真分歧。

**1. 负载模型与默认配额（10 用户样例的推导起点）：**
单活跃窗口稳态负载上界（由 Q2/Q3 协议直接导出）：控制消息 ≤2 msg/s（单飞纪律下 plan/outcome 交替 + observation 事实）、inline ROI ≤64KB/条（协议硬上限表）、完整帧仅观察点触发。默认配额：每设备窗口数 ≤8；每窗口 outstanding plan = 1（Q3 单飞已定）；每设备并发图片上传 ≤2、持续完整帧率 ≤1 帧/s/窗口；每用户设备数 ≤3。10 用户满配 ≈ 240 活跃窗口上界、<500 msg/s 控制流、~10-30MB/s 图片峰值——单实例 + 有界池可承受，但**这些系数必须由 §12.6 阶段 6 的 shadow/replay 环境实测修正**（沿用时延注入 pilot 的方法论先例），配额常数全部进配置并入审计，不硬编码。

**2. 背压必须是显式协议状态，不是静默丢弃：**
入口三层：连接级（每设备 token bucket：控制消息速率 + 上传字节数）→ 会话级（超额 → 下发 `THROTTLED{retryAfterMs, reason}`，本地机械延迟下一次采集/上报，不改业务决策）→ 硬顶（持续超额 → 断连走 CLOUD_SUSPENDED，恢复走 §7.1）。规则：任何丢弃都必须变成对端可见的结构化事实；本地对 THROTTLED 的唯一合法反应是等待，禁止借背压走本地路径（§3 推论）。超大消息/图片在网关**解码前**按字节数拒绝（防 decode bomb），拒绝事实带原因回执。

**3. OCR/OpenCV worker pool：**
按作业类别分池（毫秒级小 ROI 匹配池 / 重 OCR/全帧分析池），各自有界队列；配额单位 = 用户（不是设备/窗口，防单用户多开挤兑）；每作业类别有超时预算，超时 = 结构化 TIMEOUT 结果交回业务 Service，由云端业务规则决定重试/失败归因——**对业务层这只是"决策变慢/一次观察失败"，落在既有 watchdog 语义内，不产生新业务行为**（R0 等价性的容量侧表述）。队列溢出同理：拒绝带原因，不静默排队到天荒地老。

**4. 慢用户/异常设备隔离：**
每设备独立 WS 发送队列（有界，溢出即断连而非阻塞他人）——单个 300ms RTT 或狂传大图的设备只占满自己的队列；worker 层按用户配额 + 轮转调度，禁止 FIFO 全局队列（单用户重作业连发会队头阻塞所有人）；图片解码在受限内存的隔离池执行。异常模式（签名失败率异常、重复消息风暴、超限高频）触发设备级熔断 → 该设备 CLOUD_SUSPENDED + 审计事件，不影响同租户其他设备。

**5. 三存储的故障模式（统一原则：可用性事件，永不是正确性事件）：**
- **Redis 故障**：Q3 已定——lease/fence 权威在 PG，Redis 丢失最多 RESYNC。V1 简化：Redis 不可用 → 暂停新 grant 与新 taskRun 准入（PG 直读扛不住热路径就不硬扛），在途任务按心跳超时进 REVOKING，恢复后全量 RESYNC。
- **PG 故障**：write-before-send 不可能 → 全部 dispatch 停止，等效云端侧 CLOUD_SUSPENDED；恢复走 outbox 重投 + 对账。**PG 是唯一"停摆开关"，这是设计而非缺陷**——任何"PG 挂了先发着后补账"的缓冲方案都直接违反 E4，不讨论。
- **对象存储故障**：新完整帧上传失败 → 需要新帧的决策停等（业务 watchdog 语义处理），inline 小 ROI 路径在上限内可继续；证据写入失败 → verdict 记 INCONCLUSIVE（Q4 规则：无证据不计数），不阻塞任务本身。
- 全部故障恢复路径复用 Q2/Q3 的 RESYNC/对账机器，**不发明第三套恢复模式**。

**6. 单实例与横向扩展的一致性：**
§9 已定"不依赖 sticky"。V1 单实例；扩展时正确性锚点不变：窗口状态推进靠 PG revision CAS（跨实例竞态 = CAS 失败重读，不是锁），lease/fence 全局唯一在 PG，Redis 热索引允许短暂不一致（读到旧值最多导致一次 CAS 失败）。视觉 worker 无状态，独立横扩。唯一需要新增的横扩约束：同一设备 WS 连接落在哪个实例都必须能服务（连接注册表在 Redis），实例宕机 = 该批设备重连，走既有 fence 换代——已有机器全覆盖，无新机制。

**留给 B 的真分歧：公平性的主执行点。** 我推荐**边缘从严 + worker 从简**：入口配额卡死（token bucket 是可预测、可解释、可审计的），worker 层只做按用户轮转（round-robin），不做全功能 WFQ/优先级调度。理由：10 用户量级下 WFQ 的收益体现不出来，但它的复杂度（权重配置、优先级反转、饥饿检测）立刻要付；入口配额从严的代价只是"用户在自己超额时变慢"，这正是我们要的语义。B 若认为轮转不足（例如毫秒级探测池会被重 OCR 用户的合法流量饿死），请给出轮转挡不住的具体场景，否则 V1 从简。

B (2026-07-12 #1):

“过载只能变慢/暂停/RESYNC”、PG 故障停止 dispatch、按作业类别分池和 V1 不上完整 WFQ 的方向正确，但当前不能 B-PASS。主要问题不是配额数字是否以后可调，而是当前估算和背压边界会让过载污染正确性路径。

1. **P1：240 窗口的图片容量算术不成立，缺少全局/分层硬预算。** `10*3*8=240` 窗口若都达到允许的 1 full-frame/s，以 1024x768 RGB 解码后每帧约 2.25 MiB 计算，仅 decoded pixel 流量就超过 540 MiB/s；500 条/s 的 64 KiB inline ROI 单独也可达约 31 MiB/s，尚未计对象存储、复制、OpenCV Mat/native memory 和压缩峰值。因此“图片峰值 10-30 MB/s、单实例可承受”没有证据。必须建立 `global -> tenant -> user -> device -> window` 分层 admission/token budget，至少同时限制 encoded bytes/s、decoded pixels/s、full-frame/s、in-flight decoded bytes/native Mat、对象存储写带宽和并发作业；保留既有任务的资源 headroom，饱和时先拒绝新 taskRun/CaptureSpec，不能靠所有窗口各自 1 fps 汇总失控。正式容量只能由代表性 frame 大小与 duty cycle 的压测证明后填写，当前不得宣称单实例承载结论。
2. **P1：正确性控制消息不能与 bulk observation 共用背压语义。** STOP/PAUSE/EMERGENCY、connection fence、lease REVOKE/`INPUT_LANE_DRAINED`、ActionOutcome、heartbeat/resync 对账必须拥有独立保留容量和高优先级队列，不能被图片/普通 observation token bucket 延迟或挤掉；否则 outcome 被 THROTTLED 会制造 UNKNOWN，drain 被堵会冻结整个设备。`THROTTLED` 只允许作用于尚未执行的 CaptureSpec、图片上传和可重采低优先事实，并必须关联 request/frame、由云端 taskRun 进入非业务的 WAITING_CAPACITY overlay；本地不得自行延迟一个已经产生但尚未上报的 outcome。容量等待不计入业务 retry/phase watchdog/记忆 FAIL，但 action expiry 与 frame maxAge 仍继续生效，过期后只能拒绝并重采/RESYNC。
3. **P1：大图防护必须覆盖解压后资源，而不只是解码前字节数。** 每次 HTTPS 上传需要一次性、绑定 tenant/device/window/frameId/CaptureSpec 的 upload grant；在昂贵处理前完成认证/签名与 streaming byte cap，并校验 Content-Length/实际流量、编码 allowlist、width/height/pixel count、压缩比、解码时限和 decompressed byte 上限。解码/OpenCV 必须先取得全局+用户 native-memory semaphore，在隔离池内执行并可取消；无长度、维度异常、伪 MIME、zip/decode bomb 均在进入业务 worker 前拒绝。仅有 64 KiB 消息上限挡不住小压缩包解出巨大 Mat。
4. **P1：纯 round-robin 挡不住“合法长作业先占满全部 worker”。** 具体场景：重 OCR 池有 4 线程，用户 A 在用户 B 到达前合法提交 4 个各 5 秒作业，RR 已把 4 个都派发；B 随后的一条 100 ms 重类作业仍被阻塞 5 秒，入口 token bucket 和队列 RR 都无法抢回已占线程。V1 不需要完整 WFQ，但至少需要：按前台小 ROI/重视觉/后台学习分 class pool；每 user/tenant 每 class 的 max-in-flight 小于池容量并保留共享 headroom；队列使用按估算成本（pixel count/operation class）的 deficit round-robin；deadline 覆盖 queue wait + run，超时必须真正取消计算并释放 native memory。后台公共记忆/管理作业不得占用前台 task control 的保留槽。
5. **P1：三存储故障时对“已下发动作”的处理还缺闭环。** PG 故障后不能假设所有动作都尚未执行：已经 write-before-send 并到达本地的单飞 plan 可以完成，本地必须保留同 actionId outcome 并重试；服务端未完成 PG commit 前不得 ACK outcome，也不得生成 successor，设备 lane 进入 CLOUD_SUSPENDED/REVOKING。恢复后先提交迟到 outcome 或将缺口保持 UNKNOWN，再按 Q2/Q3 对账。对象存储证据失败时，task 可继续但 memory verdict 事务只能写 INCONCLUSIVE，不能留下 SUCCESS/FAIL 指向不存在对象。Redis 故障不能把“Redis 中没有 heartbeat”当设备失联事实：V1 当前实例仍持有的活 WS 是受信活性事实；应暂停新 admission/grant，允许当前单飞动作完成后安全停住，再 RESYNC，而不是因热索引消失批量伪造 heartbeat timeout。
6. **P1：横向路由和多租户隔离不能只靠可陈旧 Redis registry。** 配额必须包含 global/tenant 层，身份从已认证连接获取，不能信 payload 自报 tenant/user；否则同租户多用户可绕过“每用户公平”占满全局资源。outbox dispatcher 通过 Redis 找到 owner instance 后，仍须用当前 connectionFence 做投递栅栏；目标实例仅对自己持有且 fence 相同的 socket ACK enqueue，陈旧路由不得把 outbox 标成 delivered。Redis 不可用或 registry 不确定时停止新 dispatch，等待连接重建/RESYNC；不能为了“任意实例可服务”把同一设备同时交给两个实例。

补充两个 P2：

- **容量常数需要可验收的 SLO/负载矩阵。** 阶段 6 至少覆盖 1/10 用户、正常与 240-window admission 上界、真实 full-frame/ROI 尺寸、慢 RTT、单用户合法重作业、decode bomb、worker queue 满、Redis/PG/object-store 故障；记录 critical-control queue delay、image ingress、queue wait/run time、native/heap memory、throttle/reject/resync 率。验收必须证明零静默丢消息、零跨租户资源串用、零过载业务 FAIL/本地 fallback，并据此产生版本化 quotaProfile，而不只是“以后实测修正”。
- **动态配额变化也要有版本和生效边界。** quotaProfileVersion、原因、操作者和时间写审计；收紧配额先阻止新 admission，不得撤销已经合法执行中的动作；只有安全熔断可以走 Q3 REVOKING/drain。客户端收到的 retryAfter 使用有上限的服务端提示并带连接 fence，请求重放仍沿用原 message/frame identity，不能生成重复事实。

**B 对公平性分歧的结论：**不采用完整 WFQ；采用“分层入口预算 + class pool + per-user/tenant in-flight cap + cost-aware deficit round-robin”。这是 RR 的最小补强，解决已运行长作业占满池的问题，同时避免完整优先级调度器。

**B-PASS 条件：**A 修正图片容量模型并加入分层全局预算，隔离 critical control 与 bulk 背压，补齐解压后大图防护、worker in-flight/DRR、公用存储故障闭环和 fenced outbox routing，并接受两个 P2 验收/配置版本规则。完成前 Q5 保持 Review required。

A (2026-07-12 #2):

六项 P1 + 两项 P2 全部接受。两处先认错：

1. **容量算术：B 对，我错，且错得典型。** 我给的"10-30MB/s 峰值"与我自己的配额上界（240 窗口 × 1 fps）自相矛盾——按 1024×768 RGB 解码后单帧 ~2.25MiB，允许的上界流量是我声称峰值的 20-50 倍。错误根源是拿"预期通常负载"冒充"配额允许的最坏负载"，而配额设计必须按后者算。修正：**收回"单实例可承受"结论**，正式容量只能由压测证明；建立 `global → tenant → user → device → window` 分层 admission/token budget，同时限制 encoded bytes/s、decoded pixels/s、full-frame/s、in-flight decoded/native 内存、对象存储写带宽、并发作业数；饱和时先拒新 taskRun/CaptureSpec、保既有任务 headroom。

2. **正确性控制流与 bulk 背压隔离：接受，这是我开题的真漏洞。** 我把"背压是显式协议状态"写成了全局语义，没有豁免正确性消息——outcome 被 THROTTLED 直接制造 UNKNOWN、drain 被堵冻结设备，等于让容量问题污染 E4。修正：STOP/PAUSE/EMERGENCY、fence、REVOKE/INPUT_LANE_DRAINED、ActionOutcome、心跳/对账走**独立保留容量 + 高优先级队列**，永不 THROTTLED；THROTTLED 只作用于未执行的 CaptureSpec、图片上传、可重采低优事实；taskRun 容量等待进非业务 `WAITING_CAPACITY` overlay，不计业务 retry/watchdog/记忆 FAIL，但 action expiry 与 frame maxAge 照常生效（过期→拒绝+重采/RESYNC，不放水）；本地永不自行延迟已产生的 outcome 上报。

3. **解压后防护：接受全链。** 一次性 upload grant（绑定 tenant/device/window/frameId/CaptureSpec）→ 认证/签名先于昂贵处理 → streaming byte cap + Content-Length 校验 → 编码 allowlist + 尺寸/像素数/压缩比预检 → 解码限时限内存、先取 global+user native semaphore、隔离池内可取消。64KiB 消息上限只管 inline 路径，HTTPS 大图路径按本条全链防护，decode bomb 在业务 worker 之前死掉。

4. **公平性：接受 B 的折中案作为定案。** B 的 4×5s 场景确实是入口配额和队列 RR 都够不着的——问题在"已派发占着线程"，只有 in-flight cap 管得住。定案 = 分 class pool（前台小 ROI / 重视觉 / 后台学习）+ 每 user/tenant 每 class max-in-flight < 池容量并留共享 headroom + 按估算成本（像素数/操作类）的 deficit round-robin + deadline 覆盖排队与执行、超时真取消并释放 native 内存 + 后台作业不占前台保留槽。不上完整 WFQ 的共识保留。

5. **存储故障闭环：接受，并收回我开题第 5 块的一个错误。** 我写"Redis 不可用→在途任务按心跳超时进 REVOKING"——B 指出这等于**因热索引消失批量伪造设备失联**，违反我自己立的"可用性事件永不变正确性事件"。修正：当前实例仍持有的活 WS 是受信活性事实，Redis 故障只停新 admission/grant，在途单飞动作允许安全完成后停住再 RESYNC。PG 故障侧同样接受：已 write-before-send 到达本地的 plan 可完成，本地保留同 actionId outcome 重试上报；服务端 PG commit 前不 ACK、不生成 successor；恢复后先补迟到 outcome 或留 UNKNOWN 再对账。对象存储故障：证据写入失败 → verdict 只能 INCONCLUSIVE（Q4 规则的存储侧执行），永不留 SUCCESS/FAIL 指向不存在的对象。

6. **fenced routing + 认证身份：接受。** tenant/user 一律取自已认证连接，payload 自报仅作交叉校验；配额含 global/tenant 层防同租户多用户合谋绕过；outbox dispatcher 经 registry 找 owner 后仍以 connectionFence 做投递栅栏，仅 fence 匹配的持有实例可 ACK enqueue，陈旧路由不得标 delivered；registry 不确定 → 停新 dispatch 等重建，绝不让同一设备同时挂两个实例。

7. **P2 两项：接受。** (a) 阶段 6 负载矩阵按 B 清单全收（1/10 用户、admission 上界、真实帧尺寸、慢 RTT、合法重作业、decode bomb、队列满、三存储故障），验收三零标准（零静默丢消息、零跨租户串用、零过载业务 FAIL/本地 fallback），产出**版本化 quotaProfile** 而非"以后再说"；(b) quotaProfileVersion + 原因 + 操作者入审计；收紧只挡新 admission 不撤已执行动作，安全熔断走 Q3 REVOKING/drain；retryAfter 带上限与 fence，重放沿用原 identity。

**A-PASS**（Q5 合并方案 = A#1 骨架经 B 六项 P1 全量修正 + B 公平性折中案 + 两项 P2；"单实例可承受"结论撤回，待压测）。待 B 确认后 CLOSED 并回写 §12.5。

B (2026-07-12 #2):

A#2 已解决 B#1 的图片容量、critical/bulk 隔离、decode bomb、worker 公平、三存储故障和认证路由问题，原 6 项 P1 + 2 项 P2 均已接受。但复核 fenced routing 时发现一个剩余 P1，且需要修正 B#1 自己的“ACK enqueue”措辞：

**P1：owner instance 接收入队不能让 transactional outbox 进入 delivered/完成态。** 场景：dispatcher 找到 fence 正确的实例 I1，I1 把 ActionPlan 放进进程内 WS send queue 后回 `ACK_ENQUEUE`；如果 PG outbox 此时标 delivered，而 I1 在 socket write/客户端接收前崩溃，这条 write-before-send action 会永久丢失。正确分层至少是 `PENDING_ROUTE -> ENQUEUED(ownerInstanceId, connectionFence) -> CLIENT_RECEIVED/OUTCOME`：

1. instance enqueue ACK 只更新可丢的路由诊断/尝试次数，不能完成或删除 PG outbox；
2. 只有客户端在相同 connectionFence/stream/sequence 下返回签名 `MESSAGE_RECEIVED(messageId, actionId, payloadDigest)`，或先收到同 digest ActionOutcome，PG 才能原子标记 transport delivered；
3. owner instance 崩溃、socket 关闭或客户端未 ACK 超时后，dispatcher 以原 messageId/actionId/digest 重投；本地按 Q2 ledger 幂等返回旧 RECEIVED/outcome，绝不生成新 action identity；
4. connectionFence 已换代时不得把旧连接的 enqueue/ACK 归到新连接；进入既有 RESYNC，对旧 action 保持 DISPATCHED/UNKNOWN 证据语义；
5. `ENQUEUED` 不是权威业务状态，ownerInstanceId 仍只在 Redis/route attempt 诊断中，PG 权威只保留 outbox attempt、客户端 ACK/outcome 与 action ledger。

**B-PASS 条件：**A 接受“实例 enqueue 不完成 outbox，客户端签名 receipt/outcome 才完成”的 fenced delivery 语义。B#1 其余部分不再重开。

A (2026-07-12 #3):

接受，无保留。B 的场景成立：I1 进程内 send queue 是易失的，enqueue ≠ 送达——把 outbox 在 enqueue 时标 delivered，等于把 write-before-send 的"send"定义在一个会蒸发的缓冲区上，I1 崩溃即永久丢 action，而云端还以为发出去了。五条分层全收：

- `PENDING_ROUTE → ENQUEUED(ownerInstanceId, connectionFence) → CLIENT_RECEIVED/OUTCOME`，其中 ENQUEUED 只是可丢路由诊断（Redis/attempt 计数），PG 权威只认 outbox attempt + 客户端签名 receipt/outcome + action ledger；
- transport delivered 的唯一判据 = 同 connectionFence/stream/sequence 下的签名 `MESSAGE_RECEIVED(messageId, actionId, payloadDigest)`，或先到的同 digest ActionOutcome（outcome 蕴含已收到，允许跳过 receipt 直达）；
- 超时/崩溃/断连重投沿用原 messageId/actionId/digest，本地按 Q2 ledger 幂等回旧 receipt/outcome，永不铸新身份；
- fence 换代后旧连接的 enqueue/ACK 不得归入新连接，走 RESYNC，旧 action 保持 DISPATCHED/UNKNOWN 证据语义。

两点实施注记：(a) `MESSAGE_RECEIVED` 是新增的轻量 envelope 消息类型，进 Q2 词汇表——它属于正确性控制流，走 A#2 第 2 条的保留容量通道，永不被 THROTTLED；(b) Q3 单飞纪律让这套分层的运行态极简：每设备 lane 至多一条在途 plan，意味着重投窗口内最多一条未确认消息，receipt 跟踪不需要复杂的窗口管理。

**A-PASS**（Q5 最终合并 = A#1 骨架 + B#1 六项 P1 修正 + B#2 fenced delivery 分层 + 两项 P2）。待 B 确认后 CLOSED 并回写 §12.5。

B (2026-07-12 #3):

A#3 已明确接受“instance enqueue 不完成 outbox，客户端签名 receipt 或同 digest outcome 才完成 transport delivery”，并固定 fence 换代、原 identity 重投和正确性保留通道语义。B#2 的最后一个 P1 已解除。

`MESSAGE_RECEIVED` 是 Q2 envelope 的新增正确性消息词汇，但不改变 Q2 已确认的动作执行/重放矩阵：receipt 只证明客户端收到了固定 payload，不证明已执行；执行事实仍只能来自 ActionOutcome，receipt 后本地崩溃仍保持 DISPATCHED/UNKNOWN 并走既有 RESYNC。该一致性要求已回写 §12.5。

复核后 Q5 在最坏容量、分层配额、关键控制背压、decode bomb、worker 饥饿、三存储故障、租户隔离和多实例投递上均无剩余 P0/P1/P2。

**B-PASS:** Q5 通过。状态置为 CLOSED；本轮只完成 Q5 共识收口，等待 A 发起 Q6，不提前讨论下一题。

### Q6: §12.6 全量建设与原子切换计划——依赖图、验证策略、切换与回滚、删除门槛

状态: CLOSED（A-PASS + B-PASS；建设拓扑、验证矩阵、原子切换、回滚与删除门槛已收口）

A (2026-07-12 #1):

Q6 的本质：把 Q1-Q5 的共识排成**建设顺序**（依赖图），并给切换/回滚/删除三个不可逆动作各配一个可判定门。先立两条排序原则，再给阶段图，末尾一个真分歧。

**排序原则：**
(a) **每阶段交付物都在本地侧车上可测**（既有 U1-U5 裁决：全部开发对本地 cloud-brain，远端部署最后）；(b) **dev-complete ≠ switched**——任何 Service 开发完成只改变"待切换清单"状态，绝不单独进生产（§3.5），阶段门是建设质量门，不是生产开关。

**阶段依赖图（S0-S9）：**
- **S0 冻结与盘点**：Q1 迁移矩阵全量完成（方法级底账+配置/资源+基线锚定）。阻塞一切——没有矩阵就没有"迁完了"的定义。
- **S1 协议与基础层**：Q2 envelope/词汇/签名 + Q3 状态仓库/lease/outbox + Q5 配额/背压骨架 + 通用 executor。**现有资产直接作种子**：休眠原语运行时（13 类，已过 QA/修复循环）、XiuluoBrainSessionStore（E4 语义可迁 PG）、拆分构建 profile（升级为 allowlist）。S1 是唯一全局阻塞层。
- **S2 业务 Service 图迁移**（依赖 S1）：按依赖叶序开发——Reader/Recognizer 先、Coordinator 次、Task 最后；每个 Service 的迁移 PR 必须挂 Q1 矩阵行 + R0 等价性证据（决策序列机械比对，V7 先例）；与 S3 可并行。
- **S3 视觉/记忆/资产/配置迁移**（依赖 S1）：Q4 模型落地 + canonical 导入 + 资产签名管线。
- **S4 云端 task turn 与多窗口编排**（依赖 S2 主干 + S3 部分）：Q3 lease/handoff barrier 实装。
- **S5 管理 API/可观测性/安全/容量**（依赖 S1，与 S2-S4 并行）：§10 管理面 + Q5 配额压测（quotaProfile v1 在此产出）+ Q7 的 trace 埋点。
- **S6 全链路 shadow/replay 验证**（依赖 S2-S5 全部）：见下。
- **S7 原子切换**（依赖 S6 门 + S8 预演）。
- **S8 回滚就绪**：**是 S7 的前置条件，不是事后补救**——回滚预演（旧 client jar + 旧侧车恢复演练）必须在切换前完成并留证。
- **S9 旧代码删除**（依赖 S7 后稳定期）：见下。

**S6 验证策略（切换门的证据来源），双模式缺一不可：**
- **Replay（逻辑等价）**：录制真实基线会话（帧+决策+动作序列），把帧流回放进云端大脑，机械比对两侧决策流。抓纯逻辑分歧，可重复、可回归。
- **Shadow（环境等价）**：本地基线正常跑生产，Thin Client 以观察模式并行——云端产生决策**只记录不执行**，与本地实际决策逐步 diff。抓时序/环境/容量分歧（replay 抓不到的那类）。
- 分歧预算：**零未解释分歧**。每条分歧必须二选一收口：bug（修）或已批准差异（比照 V6/V7 先例走用户裁决入账）。统计上"分歧率很低"不构成通过。

**S7 切换机制：**
切换 = 整体版本翻转，不迁移在途任务——所有 taskRun 干净终态后停机窗口内完成 client+server 同步部署，fresh 启动。前置清单：S0-S6 全门 + S8 预演证据 + 用户 fresh runtime 验证（治理规则：默认门禁的用户实测环节）+ Q5 quotaProfile 就位。V1 单用户使"按用户灰度"暂无意义,但记录解释：§3.5 禁止的是新旧 Service 混跑,不禁止未来多租户按租户批次切换,该解释供多用户期复用。
**S8 回滚语义：**
部署级整体回滚（旧 client + 旧侧车/无云依赖模式）；数据安全性依据：记忆导入是 copy-not-move（Q4），本地基线自持全部旧配置/模板，切换后新系统的状态增量不需要回迁——回滚 = 放弃新系统运行期数据（记录在案），旧系统从自己的最后状态续跑。回滚触发条件写死：切换后稳定窗口内出现 {无法 RESYNC 恢复的业务中断、E4 类正确性事件、用户裁定不可接受的行为差异} 任一。

**S9 删除门槛：**
删除 = Q1 allowlist 构建物理生效 + 五门规则（矩阵行逐行核销）。时间条件：切换后稳定期 ≥ 用户认可的验收窗口（建议：全部业务类型各 N 次成功 taskRun 且零回滚事件，N 由用户定）；治理条件：删除清单经用户批准（不可逆动作，按惯例请示）；证据条件：删除后 allowlist jar 重新过 Q1 反向扫描零命中。**旧二进制+配置归档保留至 S9 完成后仍不删**（回滚窗口关闭 ≠ 历史证据销毁）。

**留给 B 的真分歧：S2 的等价性证据强度分级。** 全部 Service 一刀切"决策序列逐字比对"成本极高（部分 Service 输出含时间戳/随机采样等天然噪声）。我提议三档：**核心业务决策类**（phase 推进、点击选择、fallback 顺序）= 逐字机械比对；**观察解释类**（OCR 结果解释、模板匹配解释）= 同输入同输出比对（允许封装差异）；**纯基础设施类**（无业务语义的搬运）= 双侧编译+源码审查即可。B 若认为三档会漏（例如"观察解释类"里藏着未声明的业务决策），请给出判档的客观标准修正案，否则按三档走。

B (2026-07-12 #1):

整体建设、原子切换、零未解释分歧、回滚预演先于切换和删除需用户批准的方向正确，但当前不能 B-PASS。现方案仍有 7 项 P1，会在切换或回滚时重新引入双运行、状态过期或未验证基础设施。

1. **P1：本地侧车开发不能替代生产拓扑 staging。** “远端部署最后”只能解释为远端生产切换最后；S6 之前必须有与生产同构的隔离 staging，覆盖 TLS/认证、WebSocket fan-in、HTTPS upload grant、PG/Redis/Object Storage、真实 outbox routing、多租户 ACL、quotaProfile 和非 sticky 实例重连。Replay 可以在本地跑，Shadow/故障注入/容量门必须至少有一轮跑在 staging。否则 S7 才第一次遇到网络、密钥、路由和持久化组合，S6 无法证明目标架构。S7 还必须显式依赖已收口的 §12.7，不可在 Q7 trace/告警方案未完成时切换。
2. **P1：shadow 必须是物理上不可执行、不可学习、同帧输入的隔离域。** 基线与云端不能各自截图后再比较；本地必须 tee 同一 frameId/hash、同一基线 action/outcome facts 给 shadow，才能排除采样时差。SHADOW realm 使用独立 tenant/namespace/taskRun，服务端不得签发或投递执行型 ActionPlan、不得获取 lease、不得写 canonical memory/公共候选/用户配置，客户端也要硬拒任何 shadow action。只允许记录 normalized decision trace；否则一次配置错误就会双击，或把未执行决策当成成功学习。
3. **P1：三档证据按“类名”分级会把最高风险基础设施降成只编译审查。** outbox、receipt、lease fence、input executor、签名、pause/stop 都可被叫作基础设施，却直接决定重复输入与 E4。客观分档应按可观察影响：
   - A：状态/协议/身份/lease/outbox/真实输入/stop-pause 安全层，必须 invariant + crash/reorder/duplicate/fence/fault injection + 端到端 trace；
   - B：任何能影响 phase、动作、CaptureSpec、retry/fallback、timeout、memory/verdict/config 的业务决策，使用去除时间戳/随机 ID 后的 normalized semantic sequence 精确比对；
   - C：OCR/模板/视觉解释，使用同一原始 frame 的 typed output contract + 已批准数值容差，并要求下游 B 类决策零未批准分歧；
   - D：纯 DTO/序列化/机械搬运，至少 schema compatibility、round-trip、未知字段/边界值和 allowlist 反向扫描，不能只有 compile+review。
   每个 Q1 方法行必须标 tier、理由和证据链接；影响 A/B 的 observation 方法不得自称 D。
4. **P1：S7 缺少有界 quiesce、跨版本 fence 和兼容握手。** 原子切换不能只说“等所有 taskRun 终态”：先关闭新 taskRun admission，设置有界 drain deadline；所有旧 plan 完成/取消/标 UNKNOWN，所有 device InputLane 出具 `INPUT_LANE_DRAINED`；随后使旧 connectionFence/协议 epoch 永久失效、停止旧进程，再启动新版本。client/server 必须在任何业务动作前互验 protocolVersion、buildHash、allowlistHash、policy/asset epoch 与 minimum compatible version，不匹配即 CLOUD_SUSPENDED。OS/device 级 singleton lock 与服务端 fence 必须共同保证旧/新客户端永不同时输入；无法干净 drain 的窗口不进入切换，只能停住人工处置。
5. **P1：建设期仍运行旧基线，S3 的一次导入会在切换前过期。** S3 只能做预导入；S7 quiesce 后必须生成最终 cutover manifest，冻结旧端所有可变配置、完整 `vision-memory-v2`、任务设置、资产和版本，做增量/最终导入并核对 source/destination count、canonical hash、schemaVersion、baseline commit。导入成功后才建立 cloud source-of-truth epoch，旧端永久只读。任何缺项、hash 不等或导入后旧端又写入都必须中止切换。
6. **P1：回滚不能“放弃新数据后从旧系统最后状态续跑”。** 云端运行期间游戏世界已经发生真实任务完成、道具消耗、位置、队伍和窗口变化，旧状态必然过期；直接续跑会重复任务或在错误画面输入。回滚必须先停止新 admission，fence/排空全部新 InputLane，保留 cloud DB/对象证据只读而不删除，再为每个窗口采集 fresh full frame、重新绑定身份并执行旧系统已有的 hot-start/恢复检查；旧系统一律创建 fresh run，不恢复切换前的 in-flight phase。需要保留最小 cutover journal（新系统期间 taskRun、完成计数、重要外部动作和时间），由恢复工具/用户据此重设剩余任务计划；无法自动对账的窗口保持暂停，不能假装状态可回迁。
7. **P1：回滚工件与数据 schema 需要 expand-contract 门。** S8 预演必须从干净环境验证签名旧 client/server artifact、配置、模板、认证 key/cert、数据库/对象存储 manifest 和安装脚本的 checksum；新云端 schema 在回滚窗口内只允许 additive/独立 namespace，禁止破坏旧工具所需数据。回滚先 revoke 新协议/asset epoch 并 drain，再部署旧工件；预输入健康门失败可自动 abort，已经发生物理输入后的异常只能先 CLOUD_SUSPENDED/drain，再由明确回滚决策恢复，不能边运行边替换。

补充两个 P2：

- **“零未解释分歧”还需要覆盖分母。** S6 coverage manifest 必须逐项覆盖 Q1 每个方法行、全部支持任务类型、队长/队员/单号、多窗口竞争、热启动、pause/resume/stop、断线/重连、重复/乱序、三存储故障和配额背压；每个 replay/shadow trace 绑定 baseline commit、frame/action hashes 和 expected semantic trace。未覆盖不能当零分歧，只能标 NOT_EVALUATED 并阻塞切换。
- **S9 稳定期需要默认客观门槛。** 建议默认：切换后连续 7 天无 P0/P1/回滚事件；每个支持业务类型至少 20 个成功 taskRun，跨至少 3 次 fresh client/server 启动，并覆盖一次多窗口、一次 pause/resume/stop 和一次可恢复故障；Q1 反向扫描零本地业务命中、restore drill 仍通过。用户可提高门槛；降低或删除仍必须显式批准。删除前打不可变 tag、生成 archive manifest/SBOM/checksum，旧工件与证据归档不随源码删除。

**B 对等价性分级的结论：**不做全量逐字文本比较，也不接受“基础设施只编译审查”；采用上述按影响分 A/B/C/D 四档证据矩阵。时间戳、traceId 等只可通过版本化 normalizer 排除，随机策略必须固定 seed；任何业务语义差异仍需用户批准。

**B-PASS 条件：**A 补齐 production-like staging、shadow 硬隔离与同帧 tee、A/B/C/D 证据矩阵、有界 quiesce/版本握手、最终增量导入、外部世界回滚对账、expand-contract 恢复门，并接受两个 P2 覆盖/删除阈值。完成前 Q6 保持 Review required。

A (2026-07-12 #2):

七项 P1 + 两项 P2 全部接受。三处先认错，再逐项确认：

**认错一（对应 #3）**：我的"纯基础设施类=编译+审查"是整个三档提案里最危险的降级——outbox/lease fence/receipt/input executor/stop-pause 按名字都叫"基础设施"，按后果全是 E4 生死层。按类名分档=让最需要验证的代码自我豁免。**认错二（对应 #5）**：S3"一次性导入"忽略了建设期基线还在生产运行——切换日导入的数据早就过期了，这是把静态迁移思维套在活系统上。**认错三（对应 #6）**：回滚"从旧系统最后状态续跑"错在把"状态"当成只存在于我们的存储里——真实状态在游戏世界里（任务已完成、道具已消耗、位置已变），旧系统的最后快照对游戏世界必然过期，直接续跑=重复任务+错误画面输入。

1. **staging：接受。** "远端部署最后"修正解读为"远端**生产切换**最后"；S6 前必须有生产同构隔离 staging（TLS/认证/WS fan-in/upload grant/PG+Redis+对象存储/真实 outbox 路由/多租户 ACL/quotaProfile/非 sticky 重连），Replay 可本地，Shadow/故障注入/容量门至少一轮在 staging。staging 建设并入 S5 交付物。S7 显式依赖 Q7 收口——trace/告警没就绪不切换，接受。

2. **shadow 硬隔离 + 同帧 tee：接受。** 基线与云端各自截图再比较确实测不出真分歧（采样时差污染）；改为本地 tee 同一 frameId/hash + 基线 action/outcome facts 给 SHADOW realm（独立 tenant/namespace/taskRun）。服务端 shadow 域禁签执行型 plan、禁获取 lease、禁写 canonical memory/公共候选/用户配置；客户端硬拒任何 shadow 来源 action（LocalSafetyGate 加 realm 校验）；只产出 normalized decision trace。"一次配置错误就双击"的风险由双端双向拒绝封死。

3. **A/B/C/D 四档证据矩阵：接受为定案。** 按可观察影响分档：A 档（状态/协议/身份/lease/outbox/真实输入/stop-pause）= invariant + crash/reorder/duplicate/fence/fault injection + 端到端 trace；B 档（影响 phase/动作/CaptureSpec/retry/fallback/timeout/memory/config 的决策）= 版本化 normalizer 去噪后的 semantic sequence 精确比对，随机策略固定 seed；C 档（视觉解释）= 同帧 typed output contract + 已批准数值容差 + 下游 B 档零未批准分歧；D 档（纯 DTO/搬运）= schema 兼容 + round-trip + 未知字段/边界值 + allowlist 反向扫描。每个 Q1 方法行标 tier + 理由 + 证据链接；observation 方法影响 A/B 者不得自称 D。业务语义差异仍走用户批准，无例外。

4. **S7 quiesce/fence/握手：接受全序列。** 关新 admission → 有界 drain deadline → 旧 plan 完成/取消/标 UNKNOWN → 每设备 `INPUT_LANE_DRAINED` → 旧 connectionFence/协议 epoch 永久失效 → 停旧进程 → 起新版本。互验 protocolVersion/buildHash/allowlistHash/policy+asset epoch/min compatible version，不匹配即 CLOUD_SUSPENDED。设备级 singleton lock + 服务端 fence 双保险防新旧同时输入。**drain 不干净的窗口不切换、停住人工处置**——这与 Q3 forced-handoff 语义同构，quiesce 整个复用 Q3 REVOKING/drain barrier 机器，不新造。

5. **S3 预导入 + S7 最终 cutover manifest：接受。** 建设期只算预导入（练管线）；quiesce 后冻结旧端全部可变数据（vision-memory-v2 全量、任务设置、资产、版本），增量/最终导入 + 逐项核对 source/destination count + canonical hash + schemaVersion + baseline commit；全对才建立 cloud source-of-truth epoch，旧端转永久只读；缺项/hash 不等/导入后旧端再写入 → 中止切换。

6. **回滚对账：接受全流程。** 停新 admission → fence/排空全部新 InputLane → cloud DB/对象证据转只读保留（不删）→ 每窗口 fresh full frame + 重新绑定身份 + 旧系统 hot-start 恢复检查 → **一律 fresh run，永不恢复切换前 in-flight phase**。新增 cutover journal（新系统期间的 taskRun、完成计数、重要外部动作、时间线）作为用户/恢复工具重排任务计划的依据——它就是 Q3 审计流在切换场景的具体消费者。无法对账的窗口保持暂停，不装作可回迁。

7. **expand-contract + 回滚工件门：接受。** S8 预演从干净环境验证签名工件/配置/模板/key+cert/DB+对象存储 manifest/安装脚本 checksum；回滚窗口内新云端 schema 只许 additive/独立 namespace；回滚顺序 = revoke 新协议/asset epoch → drain → 部署旧工件 → 预输入健康门（失败自动 abort）；已有物理输入后的异常只走 CLOUD_SUSPENDED/drain + 显式回滚决策，永不边跑边换。

8. **P2 两项：接受。** (a) S6 coverage manifest 为"零未解释分歧"补分母：逐 Q1 方法行 × 任务类型 × 角色（队长/队员/单号）× 多窗口竞争 × 热启动 × pause/resume/stop × 断线重连 × 重复乱序 × 三存储故障 × 配额背压；trace 绑 baseline commit + frame/action hash + expected semantic trace；未覆盖=NOT_EVALUATED=阻塞切换，不许静默算零。(b) S9 默认门槛按 B 数值收：7 天无 P0/P1/回滚、每业务类型 ≥20 成功 taskRun、≥3 次 fresh 启动、覆盖多窗口/pause-stop/可恢复故障各一次、反向扫描零命中、restore drill 通过；用户可提高，降低/删除必须显式批准；删除前不可变 tag + archive manifest/SBOM/checksum，工件与证据归档永存。

**A-PASS**（Q6 合并方案 = A#1 十阶段骨架 + staging 并入 S5 + shadow 硬隔离 + A/B/C/D 证据矩阵 + quiesce/握手序列 + 二段式导入 + 外部世界回滚对账 + expand-contract 门 + 两项 P2 阈值）。待 B 确认后 CLOSED 并回写 §12.6。

B (2026-07-12 #2):

A#2 已逐项接受并具体化 B#1 的 7 项 P1 与 2 项 P2。复审确认：production-like staging 不再被本地侧车替代；Shadow 已具备同帧 tee、服务端禁签/禁租约/禁学习与客户端硬拒的双向隔离；验证强度按可观察影响分 A/B/C/D；S7 已补齐有界 quiesce、跨版本 fence、兼容握手、最终 cutover manifest 和 dirty-drain 停止条件；回滚不再续跑过期旧状态，而是保留云端证据、fresh frame/rebind/hot-start、fresh run 与 cutover journal 对账；expand-contract、覆盖分母和 S9 客观稳定门均已固定。

阶段编号虽保留 S0-S9，但已在 §12.6 明确实际拓扑为 `S6 -> S8 -> S7`，避免把回滚就绪误读成切换后的补救；S7 同时依赖第 12.7 节收口。复核后 Q6 无剩余 P0/P1/P2。

**B-PASS:** Q6 通过。状态置为 CLOSED；本轮只完成 Q6 共识收口，等待 A 发起 Q7，不提前讨论下一题。

### Q7: §12.7 验收与可观测性——主验收矩阵、trace、SLO、不变量监控与审计证据

状态: CLOSED（A-PASS + B-PASS；验收矩阵、因果 trace、SLO、安全监控与真人验收已收口）

A (2026-07-12 #1):

Q7 的定位：Q1-Q6 已经把大部分验收条件定义在各自题里了，Q7 不重新发明，做三件事——**汇总成单一主矩阵、定义度量方法、把验收时不变量变成生产期持续监控**。八块 + 一个真分歧。

**1. 主验收矩阵 = 各题门禁的并集，单文档管理。**
一张主表，每行：`需求条目 → 证据类型 → 证据链接 → 判定 → 裁决人`。来源分区：Q1（矩阵完备五项验收+allowlist）、Q4（记忆审计）、Q5（负载三零标准+quotaProfile）、Q6 S6（覆盖清单+A/B/C/D 证据+零未解释分歧）。规则：**没有证据链接的行不存在"通过"状态**，叙述性描述不构成证据。

**2. 本地零业务逻辑的审查方法（补 Q1 的人工侧）：**
机械侧 Q1 已定（方法级底账+反向扫描+allowlist 构建）；人工侧补充：按 A/B/C/D 档分配审查强度——A/B 档矩阵行 100% 双人独立审查（沿用 §13 双视角），C/D 档按风险抽样 ≥20%；审查签名进主矩阵裁决人列。

**3. 端到端 trace 规格（消费 Q2 correlationId/causationId + Q3 审计流 + Q5 delivery 状态）：**
每条业务 action 的完整链：`taskRun → 决策（含 policy/asset/scorePolicy 版本 + 依据 frameId）→ outbox 状态序列 → MESSAGE_RECEIVED → outcome → verifier verdict`。业务动作 100% trace（量低），observation 事实采样。trace 保留期对齐 §10 分级。验收项：随机抽 N 条已完成 action，trace 链无断点可重建全因果。

**4. 延迟 SLI 定义（先定测点，再定数值）：**
四段测量：**决策延迟**（帧到达云端 → plan 落 outbox commit）、**投递延迟**（commit → 客户端 receipt）、**输入延迟**（receipt → 物理执行开始）、**端到端**（观察触发 → 输入开始）。P50/P95/P99 按任务类型 × 活跃窗口数分桶。初始 SLO 种子沿用已裁决的时延注入阈值体系（150ms 注入 ≤+15% 任务时长、300ms ≤+30%、stale ≤2%），在 staging 压测中重校准后随 quotaProfile 版本化。

**5. 基础设施指标（Q5 的度量面）：**
图片链路（ingress 字节/帧率/拒绝数+原因分布）、worker（分池队列深度/等待/执行/超时/取消数）、PG（事务延迟/CAS 冲突率/outbox lag/重投率）、Redis（命中率/registry 陈旧度）、**关键控制通道排队延迟单列告警**——按 Q5 设计它就不该排队，排队即异常。

**6. stop/pause/断线不污染 FAILED（R18 精神的目标态版本）：**
场景目录 × 期望终态表（stop→STOPPED、断连→CLOUD_SUSPENDED、pause/resume→原任务续由云端决定），staging 自动化 + 用户 fresh runtime 双证。**生产期持续监控**：任何 FAILED 的因果链含 stop/pause/disconnect 即告警——这是不变量（应恒为零），不是统计指标。

**7. 阻断证据（负面测试目录，A 档故障注入套件）：**
错窗（bindingGeneration/incarnation 不匹配）、重复输入（ledger 幂等重放）、过期 action、stale frame、split-brain（双实例争同一设备 → fenced delivery 单 ACK 证明）、lease 换手竞态（Q3 barrier）、shadow 域 action 双端拒绝（Q6）。每项的证据物 = 结构化拒绝事实 + 审计条目。**每次发版在 staging 重跑**，不是一次性仪式。

**8. 记忆隔离与发布审计：**
跨租户探针（用户 A 私有记忆对 B 不可见，自动化验证）、trustedPublisher 门禁测试（非可信用户数据不进候选池）、发布事务审计链（verdict→发布→版本 lineage 完整）、quarantine/回滚演练各一次。管理员操作 100% 审计（§10 已定）。

**9. 生产期"零不变量"持续监控（把 E4/R18 装进仪表盘）：**
恒为零、非零即告警的清单：非 NOT_EXECUTED 重发数、同 actionId 物理重复执行数、关键通道 THROTTLED 数、stop 因果链 FAILED 数、shadow 域执行数、跨租户记忆访问数。加一个趋势型：UNKNOWN 率（不恒为零，但突增=系统性问题）。**验收测一次证明"能拦"，监控装一辈子证明"一直在拦"。**

**留给 B 的真分歧：用户 fresh runtime 的最小必做集。** 治理规则要求用户实测环节，但目标态下 staging 已能自动化绝大多数场景。我提议用户必做集收敛为四类：(1) 真实窗口绑定+首次输入原子性（物理环境唯一不可仿真项）；(2) pause/stop/紧急停止手感（人机交互质量判断）；(3) 断云拔线实操一次；(4) 切换日 S7 checklist 的最终放行签字。其余全部由 staging 自动化证据覆盖。B 若认为还有"只有真人真机才能暴露"的场景必须进用户必做集，请点名，否则按此四类收口。

B (2026-07-12 #1):

Q7 的“单一主矩阵 + 端到端 trace + 验收不变量转生产监控”方向正确，且多数故障场景已覆盖；但当前不能 B-PASS。现有方案有 7 项 P1，会允许陈旧证据通过新版本、切断恢复因果链，或把合法重投和真实安全事故混为一谈。

1. **P1：主矩阵不是 Q1/Q4/Q5/Q6 的选择性并集，而必须完整追踪第 3-10 节与 Q1-Q6。** 当前清单漏写 Q2 协议/重放门、Q3 权威状态/lease/drain/recovery 门，也把 Q1 已确认的六项门禁写成“五项”。每条硬约束必须有稳定 requirementId、owner、适用环境、证据类型、裁决状态和依赖；状态至少区分 `NOT_EVALUATED / BLOCKED / PASS / APPROVED_DIFFERENCE`。已批准差异必须指向用户裁决，不能只由普通“裁决人”放行。
2. **P1：证据必须绑定不可变 release identity，并具备失效规则。** 一个可变链接不能证明当前构建。每条证据至少绑定 client/server buildHash、protocol/schema、allowlistHash、baseline commit、policy/asset/quotaProfile/normalizer version、环境、时间和证据 content hash；最终生成签名/不可变 evidence manifest。任何代码、协议、配置、策略、资产、normalizer 或 quotaProfile 变化，都必须按依赖图自动使受影响行回到 `NOT_EVALUATED`，不得沿用旧绿灯。
3. **P1：Observation 不能在因果元数据层采样。** Q3 恢复和 Q4 verifier 需要完整的 CaptureSpec/FrameRef/ObservationPlan/ObservationFact 链。所有控制 envelope、frame metadata/hash、plan、receipt、outcome、lease/fence、recovery transition 和 verifier verdict 必须 100% 产生可关联元数据；可以采样的是原始图片 payload、像素明细和性能 span，不是决定因果的事实。因果图完整性应对 100% action 自动校验，随机抽 N 条只能作为人工补充。普通性能 trace 可过期，但 action ledger、UNKNOWN/non-replay tombstone、安全认证、人工管理和公共记忆审计必须按 Q3 持久，不能笼统“对齐图片保留期”。
4. **P1：目前没有可执行的数值 SLO 门，延迟注入比例也不能替代 SLO。** S6 前必须冻结版本化 SLO profile，至少覆盖决策、投递、InputLane 等待、端到端、critical control、RESYNC 恢复、错误率/UNKNOWN、可用性和窗口饥饿上限，并按任务类型与活跃窗口数分桶。跨 client/server 不能直接相减未同步墙钟；服务端段和客户端段分别使用单调时钟，跨机段记录握手 offset/uncertainty 或仅使用因果收发区间。P50/P95/P99 之外还要有 max-age/expiry、安全硬上限和 error budget；“staging 后再重校准”必须产出冻结数值后才能过 S6/S7。
5. **P1：零不变量中的“非 NOT_EXECUTED 重发数=0”违反 Q2 的 at-least-once 重投语义。** 相同 messageId/actionId/digest 的传输重投是合法且必要的，应该作为趋势指标。真正恒为零的是：同 actionId 的物理副作用执行超过一次、没有可信 `NOT_EXECUTED && physicalInputStarted=false && completedInputSteps=0` 就铸造新 actionId、旧/错误 fence 被接受、错 window/binding 被执行、critical control 被 THROTTLED、跨租户读取和 shadow 物理执行。任何 E4/租户隔离不变量非零，不能只“告警”：必须自动关闭相关 admission、fence/CLOUD_SUSPENDED、保全证据并 page；UNKNOWN 率、合法 transport redelivery 和 RESYNC 率是有阈值的趋势指标。
6. **P1：负面证据和 stop 归因还不够精确。** split-brain 场景“单 ACK”不能证明没有双输入，必须同时证明唯一 current connectionFence、旧 fence 在服务端和本地均被拒绝、只有一个 physical-execution ledger/outcome、lane 最终排空。`FAILED` 因果链“包含 stop/pause/disconnect”也会误报“业务先失败、用户后 stop”的正常序列；不变量应检查导致终态转换的 canonical trigger/reasonCode/causationId，证明 stop/断连中断没有被翻译成 FAILED，pause 本身不生成错误终态。
7. **P1：用户 fresh runtime 四项骨架过窄。** “首次输入”不能覆盖本项目最危险的错窗与真实流程时序。最小真人真机集必须增加：(a) 至少两个真实窗口竞争 InputLane，覆盖 focus、窗口切换、原子 move+click、DPI/坐标和错 binding 拒绝；(b) 每个支持任务族至少一条完整端到端 fresh run，并覆盖其适用的队长/队员/单号物理差异；(c) 客户端与服务端各一次真实重启后的 window rebind/hot-start/RESYNC；再保留 A 提议的 pause/stop/emergency、断云拔线和切换日签字。故障矩阵其余部分仍由 staging 自动化，不要求用户手工重复。

另有 3 项 P2 需要一并收口：

- **观测系统故障不能改变业务。** 性能 metrics/span 使用有界异步通道，丢弃必须自计数且不能阻塞 critical control；PG action ledger/安全与管理审计属于正确性数据，写入失败按 Q5 停止新 dispatch，不能与可丢 telemetry 混成一条路径。
- **告警必须可运营。** 每项不变量/趋势告警需要 severity、owner、检测时限、自动动作、通知路由、去重窗口和 runbook；P0/E4、跨租户、双物理输入与 stop 污染应有明确 page 和 admission freeze/CLOUD_SUSPENDED 行为，不能只有仪表盘红点。
- **Observability 自身也必须租户隔离和最小披露。** metrics label 不放原始角色名、图像内容、token 或高基数 action/frame ID；高基数身份只进受控 trace/audit，按 tenant/user 授权、脱敏和访问审计。用户 fresh runtime 证据也必须绑定同一 release manifest，避免签字与实际构建脱节。

**B 对 fresh runtime 分歧的结论：**接受 A 的四类作为骨架，但必须补充上述真实多窗口竞争、每任务族完整运行和真实重启恢复。只有 OS 输入、游戏 UI 时序与窗口绑定相关场景要求真人真机；可确定性故障注入、容量和协议乱序继续由 staging 自动化承担。

**B-PASS 条件：**A 补齐完整需求追踪与证据失效规则、100% 因果元数据、冻结数值 SLO、合法重投/安全不变量分离、精确 split-brain 与 stop 归因、扩展后的真人真机集，并接受三项 P2。完成前 Q7 保持 Review required。

A (2026-07-12 #2):

七项 P1 + 三项 P2 全部接受。先认两处：**其一**，"非 NOT_EXECUTED 重发数=0"是我把两个层次混成了一个——**transport redelivery**（同 messageId/actionId/digest 重投，Q5 fenced delivery 的 at-least-once 本义，合法且必要）与 **business reissue**（铸新 actionId，受 E4 三条件门控）。把前者记零等于把我们自己设计的重投机制当事故。**其二**，Q1 验收我又写成"五项"——B 在 Q1 收口时就纠正过一次"实际是六项"，我在 Q7 复述时二犯同一笔误，这正好证明 B 第 1 条"要 requirementId 稳定追踪、不要靠人复述"的必要性。逐项确认：

1. **完整需求追踪：接受。** 主矩阵覆盖 §3-§10 全部硬边界 + Q1-Q6 全部门禁（含我漏列的 Q2 协议/重放门、Q3 权威状态/lease/drain/recovery 门）；每行 `requirementId(稳定) + owner + 适用环境 + 证据类型 + 状态(NOT_EVALUATED/BLOCKED/PASS/APPROVED_DIFFERENCE) + 依赖`；APPROVED_DIFFERENCE 必须链接用户裁决记录，普通裁决人无权放行差异——这与治理规则（业务差异唯用户可批）严格一致。

2. **证据绑定不可变 release identity + 失效规则：接受。** 每条证据绑 {client/server buildHash, protocol/schema, allowlistHash, baseline commit, policy/asset/quotaProfile/normalizer version, 环境, 时间, 证据 content hash}，终产物为签名不可变 evidence manifest；任何依赖项变化按依赖图自动把受影响行打回 NOT_EVALUATED——旧绿灯不跨版本存活。

3. **因果元数据 100%，采样只许碰非因果层：接受。** 修正我开题的"observation 事实采样"：所有控制 envelope、frame metadata/hash、plan、receipt、outcome、lease/fence、recovery transition、verifier verdict 100% 落可关联元数据；可采样的仅原始图片 payload、像素明细、性能 span。因果图完整性对 100% action 自动校验（抽查只是人工补充）。保留期分层照 Q3：action ledger/tombstone/安全/管理/公共记忆审计持久，性能 trace 可过期，不笼统对齐图片期。

4. **冻结数值 SLO profile：接受。** S6 前必须产出版本化冻结数值（决策/投递/InputLane 等待/端到端/critical control/RESYNC 恢复/错误率与 UNKNOWN/可用性/窗口饥饿上限，按任务类型×窗口数分桶）；时延注入比例只是等价性门，不冒充 SLO。时钟语义收下：两侧各自单调时钟，跨机段记握手 offset/uncertainty 或只用因果收发区间，不做未同步墙钟减法（与 Q2 时钟条款同源）。P 分位之外补 max-age/安全硬上限/error budget。

5. **零不变量清单重构：接受 B 版本为定案。** 恒为零 + 自动遏制（关 admission + fence/CLOUD_SUSPENDED + 证据保全 + page）：同 actionId 物理副作用执行 >1 次；无可信 `NOT_EXECUTED && physicalInputStarted=false && completedInputSteps=0` 铸新 actionId；旧/错 fence 被接受；错 window/binding 被执行；critical control 被 THROTTLED；跨租户读取；shadow 物理执行。趋势指标（有阈值告警）：UNKNOWN 率、transport redelivery 率、RESYNC 率。**E4/租户类非零=事故响应，不是仪表盘红点**。

6. **精确归因：接受两处修正。** split-brain 证明升级为四要素并证：唯一 current connectionFence + 旧 fence 双端拒绝记录 + 单一 physical-execution ledger/outcome + lane 最终排空（单 ACK 只是必要非充分）。stop 归因改为检查终态转换的 canonical trigger/reasonCode/causationId——"因果链包含 stop"确实会把"业务先败、用户后停"的正常序列误报，判据是"stop/断连**导致**了终态翻译错误"而非"出现在链上"。

7. **真人真机集扩展：接受 B 的补充三项为定案。** 骨架四类保留，增补：(a) ≥2 真实窗口竞争 InputLane（focus/切换/原子 move+click/DPI 坐标/错 binding 拒绝）；(b) 每支持任务族 ≥1 条完整端到端 fresh run，覆盖队长/队员/单号物理差异；(c) 客户端、服务端各一次真实重启后的 rebind/hot-start/RESYNC。确定性故障注入/容量/协议乱序仍归 staging。这个划分标准本身入档：**只有 OS 输入、游戏 UI 时序、窗口绑定物理性相关的场景要求真人**。

8. **三项 P2：接受。** (a) telemetry 与正确性数据双路分离：metrics/span 走有界异步可丢通道（丢弃自计数），action ledger/安全/管理审计走正确性路径、写失败按 Q5 停新 dispatch；(b) 每条告警配 severity/owner/检测时限/自动动作/通知路由/去重窗口/runbook，P0 类（E4/跨租户/双物理输入/stop 污染）带 page + admission freeze/CLOUD_SUSPENDED 自动行为；(c) observability 自身租户隔离：label 无原始角色名/图像/token/高基数 ID，高基数身份只进受控 trace/audit 且访问审计；用户 fresh runtime 证据同样绑定 release manifest，签字与构建不脱节。

**A-PASS**（Q7 合并方案 = A#1 九块经 B 七项 P1 全量修正 + 三项 P2；零不变量清单以 B 版为准；真人真机集 = 四类骨架 + B 三项增补）。待 B 确认后 CLOSED 并回写 §12.7。

B (2026-07-12 #2):

A#2 已完整接受并具体化 B#1 的 7 项 P1 与 3 项 P2。复审确认：主矩阵覆盖第 3-10 节和 Q1-Q6 全部硬门；证据绑定不可变 release identity 并在依赖变化时自动失效；因果元数据与完整性校验均为 100%，只采样非因果 payload/span；SLO 在 S6 前冻结数值且使用可比较时钟；合法 transport redelivery 已从零不变量移出；split-brain 与 stop 归因改为可证明的精确判据；真人真机集补齐多窗口竞争、每任务族完整运行和真实重启恢复。Telemetry/正确性分路、可运营告警及 observability 租户隔离也已固定。

复核后 Q7 无剩余 P0/P1/P2。Q7 的验收规则已回写 §12.7，并与 Q2 at-least-once、Q3 持久审计、Q5 critical control 及 Q6 S6/S7 门保持一致。

**B-PASS:** Q7 通过。状态置为 CLOSED；本轮只完成 Q7 共识收口，不在同一轮执行整份设计终审。

### Final Review: 整份目标架构终审

状态: Review required（A-2/A-3 v4 待交付；A-1 方法级 inventory workflow 进行中；A-4/A-5/A-6/A-7 已登记待复审）

B (2026-07-12 Final #1):

Q1-Q7 分题均已双 PASS，但分题通过不自动等于整份最终设计可交付。横向复核第 3-10 节、§12.1-§12.7 与第 13 节产出要求后，当前不能给出 B Final PASS；存在以下 8 项 P1。

1. **P1：第 13 节要求的最终设计工件尚未真正产出。** 当前 §12.1 只有“如何制作矩阵”的规则，没有实际完整 Service/方法迁移矩阵；§12.2 是协议原则，没有可实现的完整 schema/字段表/状态转换；§12.3-§12.4 没有数据库表、主外键、唯一约束、索引和事务边界清单；也没有完整组件/信任边界图、正常动作/stop/重连/lease 换手/切换回滚核心时序，以及实际主验收矩阵和故障演练清单。Final Proposed 前必须在本文附录或冻结的链接工件中提供这些内容，并由 evidence manifest 绑定 commit/hash。这里只要求设计工件，不要求现在写 Java 实现。
2. **P1：后题新增的协议语义没有完整回灌 §12.2。** Q5 已把签名 `MESSAGE_RECEIVED` 定义为 outbox transport-delivered 的唯一条件之一，并明确“进入 Q2 词汇表”，但 §12.2 第 421 行最终词汇仍未列出它；REVOKE、`INPUT_LANE_DRAINED`、握手/版本协商、RESYNC 等控制消息也没有完整 schema。与此同时 §5.1 写“每条消息至少有 windowId/taskRunId/actionId”，但 device-control、握手、心跳和窗口注册天然没有这些字段。最终协议必须定义显式 message scope、每类 required/forbidden 字段、receipt/outcome 状态机，以及 START_TASK/TASK_COMMAND 的 requestId 幂等、ACK 和重复处理；不能继续依赖“至少包括”的开放列表。
3. **P1：`connectionFence` 的持久权威和原子换代位置未定义。** §12.2 只说“服务端原子换代”，§12.5 又明确 Redis registry 只是可陈旧路由，但没有说明当前 fence/generation 存在何处、如何在多实例和 Redis 全丢后保证单调不复用。需要像 inputLane lease 一样给出可判定权威：至少由 PG 或等价耐久 CAS 保存 device/process connection generation，建立新连接、撤销旧 fence、更新连接有效性和允许 outbox 路由之间有明确事务/栅栏顺序；Redis 只能缓存 owner route。否则 Redis 丢失或实例分区后可能重新接受旧签名计划。
4. **P1：无 drain ACK 的 forced handoff 只等“plan expiry + clock skew”不充分。** 旧计划可能在 expiry 前一刻合法进入 exclusive callback，并在 expiry 后继续执行有界 sleep/后续步骤；此时 expiry 到点不代表 InputLane 已空。必须二选一并写进协议：要么 expiry 是硬执行截止时间，本地在 callback 进入前及每个物理步骤前检查，截止后停止并报 STOPPED/UNKNOWN；要么重授等待到 `latestExpiry + maxSignedExecutionDuration + clockSkewSafety`。max execution duration 必须由有界步骤/sleep 推导、被签名并由本地强制执行。没有 drain proof 或该最坏完成边界，不得授新 holder。
5. **P1：`MATCH_AND_CLICK` 与 side-effect plan 的原子边界存在未解决冲突。** §5.4 允许本地一次截图/模板匹配后点击，§12.2 又禁止 capture/模板处理进入 exclusive callback。最终 schema/时序必须明确：MATCH_AND_REPORT 无物理副作用；MATCH_AND_CLICK 在持有云端 device-lane lease 时完成绑定窗口 capture/match，记录 frame/hash/score/box，随后只把有界 move+click 放进一次 exclusive callback，并在进入 callback 前重新核验 fence、binding、asset/hash、frame age 和坐标；匹配失败只上报事实，不得本地 fallback。否则实现者要么把 OpenCV 塞进 InputLane 长占权，要么在释放 lease 后点击陈旧结果。
6. **P1：切换后的“旧端永久只读”与整体回滚的写权限互相矛盾。** S7 建立 cloud source-of-truth epoch 后把旧端永久只读，但 S8 回滚部署旧系统后，旧任务状态、记忆和配置路径仍需要写入。回滚必须定义一次反向 authority transfer：停止云端 admission、fence/drain、封存云端权威快照和 journal、撤销 cloud writer epoch，然后从已校验快照启动一个新的 rollback authority epoch，并只对该旧系统恢复写权限；任何时刻只能有一个 writer epoch。不能简单把旧目录改回可写，也不能让只读旧系统带写失败继续运行。
7. **P1：生产认证、密钥和管理授权生命周期没有闭环。** §9.1 只有 token/device key 方向，协议又依赖连接签名、asset signing、upload grant、管理员和 trusted publisher，但缺少设备注册/解绑、短 token 刷新与撤销、设备私钥安全存储、消息/资产 signing key rotation 与 compromise revoke、服务端 secret/cert 管理、upload grant 单次消费，以及管理 API 的 RBAC/高风险操作再认证。必须定义 principal/role/permission 表和 key epoch/revocation 对运行中连接、outbox、缓存资产的影响；否则多租户隔离和资产撤销只停留在字段层。
8. **P1：部署级回滚不等于权威数据灾备。** PG 是 phase/action/lease 唯一权威，对象存储承载证据，但文档没有 backup、PITR、RPO/RTO、PG 与对象 manifest 一致性或灾难恢复后的 non-replay 规则。尤其从旧 PG 备份恢复会“遗忘”已在游戏世界执行的动作，若直接续跑会重复输入。需要定义备份与恢复策略、加密 key/cert 恢复、定期 restore drill，以及数据恢复后强制生成全局新 fence、所有设备 CLOUD_SUSPENDED、所有备份时间点后的 action 视为 UNKNOWN、fresh frame/rebind/RESYNC 后才可重新 admission；不能从陈旧快照直接恢复执行。

另有 2 项 P2，应随最终收口处理：

- **文档状态与章节措辞仍是旧阶段。** 文件头仍写 Draft/等待讨论，§12 仍称“开放项”，§14 仍说“后续讨论的任务”。这在 A/B Final PASS 前可以保留；最终通过时必须统一改为 `Final Proposed`，并把 Q1-Q7、最终工件索引和剩余用户决策写成可导航目录。
- **第 13 节要求的“仍需用户拍板事项”没有显式结论。** 若已经没有未决产品选择，应明确写“无未决产品决策；后续数值由 SLO/quota/evidence 门产生，降低门槛仍需用户批准”；若仍有，必须列 owner、影响和最迟裁决阶段，不能让实现者自行选择。

**B Final PASS 条件：**A 补齐或接受上述 8 项 P1 与 2 项 P2，并把缺失的实际设计工件纳入本文或冻结链接；随后在追加区给出明确 `A Final PASS`。B 将基于最新完整文档再次终审。在此之前，文档保持 Draft，heartbeat 继续。

A (2026-07-12 Final #1):

八项 P1 + 两项 P2 全部接受。**A Final PASS 本轮不给**——#1 的工件缺口是事实，工件不存在就没有可终审的对象，我不会用"规则已定义"冒充"设计已交付"。本回应做两件事：把 #2-#8 七个跨章节断点**现在定死为设计决定**（它们都是可在文档层收口的），并给 #1 的工件产出计划。

**#2 协议回灌 §12.2：接受，决定如下（同步回写 §12.2）。**
- `MESSAGE_RECEIVED` 补入最终词汇表（Q5 已定其语义：仅证送达不证执行）；REVOKE_LEASE、`INPUT_LANE_DRAINED`、`HANDSHAKE/VERSION_NEGOTIATE`、`RESYNC_*` 全部列为一等消息类型，各配 schema。
- 引入显式 **message scope 分类**取代"至少包括"开放列表：`DEVICE_SCOPE`（握手/心跳/窗口注册/INPUT_LANE 状态/LOCAL_LEDGER_RESET——必填设备级身份+fence+direction+streamId+sequence，**禁填** windowId/taskRunId/actionId）、`WINDOW_SCOPE`（窗口绑定/capture 类——加 windowId+incarnation）、`TASKRUN_SCOPE`（plan/outcome/verdict——全量身份）。每个消息类型声明 scope，验证器按 scope 强制 required/forbidden，§5.1 的表述据此修正为"按 scope 定义"。
- START_TASK/TASK_COMMAND 补 **requestId 幂等**：客户端生成 requestId，服务端按 (deviceId, requestId) 去重，重复请求重放原响应（sessionId 确定性推导已保证 START 幂等的下半段，此处补上半段）。
- receipt/outcome 状态机 = Q5 已定的 `PENDING_ROUTE→ENQUEUED→CLIENT_RECEIVED→OUTCOME_RECEIVED→CLOSED`，入 schema 工件。字段级完整表归 A-2 工件。

**#3 connectionFence 持久权威：接受，比照 lease 处理。**
PG 权威保存每 (tenantId, deviceId) 的 connection generation，CAS 单调递增永不复用；换代事务顺序写死：`认证通过 → PG CAS generation+1（同事务：旧 fence 标失效 + 禁止 outbox 向旧 fence 路由）→ commit → 才允许 Redis 发布新路由 → 才允许 dispatch`。outbox 投递校验 fence 时读 PG 已提交值（与 write-before-send 同一事务窗口）。Redis 全丢：路由重建于重连，generation 由 PG 保单调——旧签名计划在任何实例任何时刻都过不了 fence 校验。

**#4 forced handoff 最坏完成边界：接受，二选一中选"expiry=硬执行截止"。**
理由：本地 executor 本就有每步检查点（Q3 A-2 已挂 epoch 重检），把 expiry 检查挂同一位置零新增机制。规则：进入 exclusive callback 前 + 每个物理步骤前检查 expiry，过期即停余步、按检查点报 STOPPED/UNKNOWN；SLEEP 不豁免（sleep 醒来后下一步检查同样生效）。由此云端无 drain ACK 时的重授等待简化为 `latestExpiry + clockSkewSafety`——硬截止保证该时点后 lane 物理静默。maxExecutionDuration 由有界步骤+sleep 上限推导、随 plan 签名，本地作为二道校验（计划声明的执行预算超过 expiry 窗口即整计划拒绝）。

**#5 MATCH_AND_CLICK 原子边界：接受 B 的规范时序为定案（回写 §5.4/§12.2）。**
MATCH_AND_REPORT 无物理副作用，走普通观察路径。MATCH_AND_CLICK：必须持有云端 device-lane lease → 在 exclusive callback **外**完成绑定窗口 capture+match（lane 空闲态），记录 frame/hash/score/box → 匹配成功后把**仅含有界 move+click** 的步骤放入一次 exclusive callback → callback 入口重新核验 {fence, binding, asset hash, frame age ≤ maxMatchToClickAge（新协议常数）, 坐标窗口内} → 执行。匹配失败只报事实。OpenCV 永不进 callback，陈旧匹配结果被 maxMatchToClickAge 拦死——B 指出的两个错误实现形态都被结构排除。

**#6 回滚写权限：接受，补"反向 authority transfer"仪式（回写 §12.6 S8）。**
单一 writer epoch 不变量：任何时刻至多一个 ACTIVE writer epoch，记录于 authority 台账（PG + 签名 cutover manifest 文件双写，后者供旧系统在无云依赖下核验）。回滚序列：停云端 admission → fence/drain 全部 lane → 封存云端权威快照 + cutover journal（不可变、checksum）→ **撤销 cloud writer epoch** → 从已校验快照签发 rollback authority epoch → 旧系统启动时核验 epoch 文件，仅在持有该 epoch 时恢复写权限。"旧端永久只读"修正为"旧端在 cloud epoch 有效期内只读"；矛盾消除，且旧系统绝不带写失败运行。

**#7 认证/密钥生命周期：接受，设计级闭环如下（展开为 A-6 工件）。**
设备注册=用户授权的 enrollment，设备私钥生成于 OS 密钥库（Windows DPAPI/TPM），公钥注册服务端；解绑=撤销设备记录+其全部活跃 token/fence。访问 token 短时效+可服务端撤销+refresh 流。签名密钥三类分治：连接消息签名密钥在握手时派生绑定（Q2 已定 key/算法绑定连接）；资产签名密钥服务端持有，带 keyId+轮换（keyring 验证，旧 key 退役期可验不可签；compromise → revoke keyId → 该 keyId 缓存资产全体失效走 Q4 REVOKED 路径）；服务端 secret/cert 进专用管理（与数据备份分开托管，见 #8）。upload grant 单次消费：服务端原子标记 consumed，二次使用拒绝。管理 API：principal/role/permission 表（viewer/operator/admin 起步），高危操作（强制发布/quarantine/回滚/删除/trustedPublisher 变更）要求再认证 + 100% 审计。key epoch 变化对在途的影响规则：连接密钥撤销=强制断连重连；资产 keyId 撤销=按 Q4/Q5 REVOKED 强路径。

**#8 权威数据灾备：接受（展开为 A-7 工件）。**
PG：WAL 连续归档 + PITR + 每日基础备份，RPO ≤5 分钟、RTO 目标随 quotaProfile 版本化；对象存储开版本化 + 与 PG manifest 定期交叉核对；restore drill 周期执行（并入 S9 既有 drill 条款）；加密 key/cert 独立于数据备份托管（否则备份即泄露）。**恢复后强制序列**（B 的"备份遗忘已执行动作"场景的解药）：全局 fence epoch 强制换代 → 全设备 CLOUD_SUSPENDED → 备份时间点之后的所有 action 一律标 UNKNOWN（禁止 NOT_EXECUTED 推断——E4 在灾备场景的表述）→ 逐窗口 fresh frame + rebind + RESYNC → 才重开 admission。陈旧快照直接续跑被结构禁止。

**P2 两项：接受。** 文档状态措辞留到 Final PASS 时统一改 `Final Proposed` + 可导航目录。"仍需用户拍板事项"显式化（回写 §13）：(1) S9 删除门槛数值最终确认（默认值已定，降低须批准）；(2) SLO/quotaProfile 冻结数值批准（S6 前）；(3) 真人真机验收集的执行安排；(4) 切换日 S7 最终放行签字；(5) 迁移矩阵产出后的删除清单批准。除此五项无其他未决产品决策；实现层数值由证据门产生。

**#1 工件计划（A Final PASS 的前置）：**
| 工件 | 内容 | 依赖 |
|---|---|---|
| A-2 `protocol-schema.md` | envelope+全消息字段表、scope 矩阵、receipt/outcome 与 lease/fence 状态机、常数表 | 本回应 #2-#5 |
| A-3 `state-and-data-model.md` | PG 表/主外键/唯一约束/索引/事务边界、Redis 键空间、对象存储布局 | Q3/Q4 + #3/#6/#8 |
| A-1 `service-migration-matrix.md` | Q1 规则实例化的全库矩阵（方法级底账+配置资源+tier 标注） | 全库扫描，工作量最大 |
| A-4 `components-and-sequences.md` | 组件/信任边界图 + 六核心时序（正常动作/stop/重连/lease 换手/切换/回滚） | A-2/A-3 |
| A-5 `master-acceptance-matrix.md` | Q7 规则实例化 + requirementId 全量行 | A-1 定分母 |
| A-6 `security-key-lifecycle.md` | #7 展开 | 本回应 |
| A-7 `dr-and-backup.md` | #8 展开 | 本回应 |

产出序：A-2/A-3/A-6/A-7（纯设计，先行）→ A-1（全库扫描）→ A-4/A-5。每件完成即 commit 取 hash 入 evidence manifest 并在本区登记，全部就绪后我再给 `A Final PASS`，B 基于完整文档二次终审。

B (2026-07-12 Final #2):

A Final #1 对原 #2-#8 的方向性接受有效：message scope、requestId 幂等、PG connection generation、硬 expiry、MATCH_AND_CLICK 两段边界、密钥生命周期和灾备恢复先 suspend/resync 都比原稿完整；A 也正确拒绝在工件不存在时提前给 A Final PASS。但复核具体决定后仍有 4 项 P1 + 1 项 P2，不能让它们直接固化进 A-2/A-3/A-7。

1. **P1：A-1 至 A-7 仍只是计划，不是可审对象。** 这延续 B Final #1 的工件阻塞，不重复扩大范围。每份工件必须实际存在、互相引用一致、具有 commit/content hash，并在主文档建立索引；尤其 A-1 全库矩阵必须有冻结源码 commit 与工作区差异证据。工件齐备前 Final Review 继续保持 Review required。
2. **P1：PG 持久 connection generation 解决了“记不住旧 fence”，但尚未解决“旧 fence 计划已在本地队列”。** 新 fence CAS 可以阻止后续路由，却无法召回旧实例已经 `ENQUEUED`、客户端尚未执行的计划。connection 换代必须复用 Q3 barrier：PG CAS 先把旧 fence/相关 lane 置 REVOKING，停止旧路由并关闭旧 socket，旧 outstanding action 保持/标 UNKNOWN；客户端先持久到进程内最高 fence 并 ACK 握手，随后拒绝所有旧 fence 请求；服务端取得 `INPUT_LANE_DRAINED`，或等到本题 #4 的硬 expiry 安全边界后，才允许新 fence 的第一条有副作用 plan dispatch。新控制连接可以先建立，但新业务输入不能越过旧队列排空证明。
3. **P1：writer epoch 不能由“PG + 签名文件双写”共同权威。** 两个介质无法原子提交；若文件显示 rollback ACTIVE 而 PG 仍显示 cloud ACTIVE，就重新产生双 writer。应采用单向提交协议：PG authority ledger 在云端阶段是唯一权威，状态至少 `CLOUD_ACTIVE -> TRANSFER_PREPARED -> ROLLBACK_ACTIVE`；先 drain/freeze 并生成 PREPARED manifest，PG 事务撤销 cloud writer、提交目标 epoch 为 ROLLBACK_ACTIVE 后，才签发只含该已提交 transaction/LSN、snapshot hash、source/target epoch 和 transferId 的 ACTIVE manifest。旧系统只接受这个后置签名投影；任何一步崩溃都保持全系统停止，绝不回退猜测。manifest 是离线 capability/projection，不是与 PG 并列的第二权威。
4. **P1：正确性账本不能采用 RPO ≤5 分钟。** 如果 PG 恢复点丢了最近 5 分钟，已执行 action 的记录根本不存在，系统无法把“备份时间点之后的所有 action”逐条标 UNKNOWN；本地 V1 台账又不跨进程持久，fresh frame 也不能恢复已完成次数和道具消耗。必须把数据分级：action ledger、connection/lease fence、outbox、task revision 和 non-replay tombstone 的已确认提交要求灾备 RPO=0（同步耐久副本或独立不可变 action journal）；图片/性能证据等可按已批准 RPO。若 correctness ledger 无法达到 RPO=0，则恢复只能全局停机并要求对潜在丢失窗口做人工外部对账，不能自动重新 admission。A-7 必须明确选择并给出实现与演练证据，不能用“事后全部标 UNKNOWN”替代不存在的数据。

另有 1 项 P2：

- **receipt/outcome 状态机必须允许 outcome 先到。** Q5 已明确同 digest ActionOutcome 蕴含客户端收到 plan，可跳过 `MESSAGE_RECEIVED`。因此 A-2 不能只有线性 `ENQUEUED -> CLIENT_RECEIVED -> OUTCOME_RECEIVED`；必须至少允许 `ENQUEUED -> OUTCOME_RECEIVED` 直接边，并对迟到 receipt 幂等吸收。receipt 只完成 transport delivery，不改变 physical execution 状态。

对其余决定的复核结论：硬 expiry 方案可以成立，但 A-2 必须把 `now + signed maxExecutionDuration <= expiry` 作为 callback 启动门，并在每个物理步骤前复检；MATCH_AND_CLICK 方案可以成立，但 capture/match 期间必须持续持有云端 lease 且本地 lane 处于无旧请求状态；#7 密钥/RBAC 方向无新增 P0/P1/P2。

**B Final PASS 条件更新：**A 先修正上述 connection 换代 barrier、单一 writer-epoch 提交协议、correctness-ledger RPO 和 outcome-before-receipt 状态边，并将结果落实进实际 A-1 至 A-7 工件。工件齐备且 A 明确 `A Final PASS` 后，B 再做完整二次终审。

A (2026-07-12 Final #2):

四项 P1 + 一项 P2 全部接受。时序说明：B Final #2 与我产出 A-2 工件为并发——A-2 已存在，且 B 本轮全部修正**已直接落实进 A-2 当前版**（非口头接受）。逐项：

1. **工件存在性：A-2 已产出并登记（见下），其余按序推进。** 同意工件齐备前 Final Review 保持 Review required。
2. **connection 换代复用 drain barrier：接受，已写入 A-2 §8.3。** 我 Final #1 的换代序列只管住了"以后不路由旧 fence"，管不住"旧 fence 计划已在客户端队列"——B 对。修正序列：PG 事务内 CAS gen+1 + 旧 fence 失效 + 相关 lane 全置 REVOKING → 关旧 socket → outstanding 保持/标 UNKNOWN → 客户端持久最高 fence 后拒一切旧 fence 请求 → 新控制连接可先行（握手/心跳/对账），**但新 fence 首条有副作用 plan 必须等 INPUT_LANE_DRAINED 或 latestExpiry+CLOCK_SKEW_SAFETY_MS 硬边界**。新业务输入不越过旧队列排空证明。
3. **单一 writer epoch 提交协议：接受，收回"双写共同权威"。** 两介质无法原子提交，我的方案在文件/PG 分歧窗口里就是双 writer——正是 Q3 里我们消灭过的错误在回滚场景借尸还魂。定案：PG authority ledger 云端阶段唯一权威，`CLOUD_ACTIVE → TRANSFER_PREPARED → ROLLBACK_ACTIVE` 单向状态机；drain/freeze → PREPARED manifest → PG 事务撤 cloud writer + 提交 ROLLBACK_ACTIVE → **之后**才签发只含已提交 transaction/LSN + snapshot hash + source/target epoch + transferId 的 ACTIVE manifest；旧系统只认这个后置签名投影（capability，非权威）；任何一步崩溃全系统保持停止，不回退猜测。此定案将落 A-3（authority ledger 表）与 A-7（回滚章）。
4. **正确性账本 RPO=0：接受，收回 RPO≤5min 的笼统值。** B 的论证无可辩驳：丢失窗口内的 action 记录根本不存在，"事后全标 UNKNOWN"标不了不存在的行，本地 V1 台账又不跨进程持久——这不是恢复策略，是数据蒸发。数据分级定案：**correctness 级**（action ledger、connection/lease fence、outbox、task revision、non-replay tombstone）已确认提交要求 RPO=0（同步耐久副本或独立不可变 action journal，A-7 给实现选型与演练证据）；**证据级**（图片/性能）按已批准 RPO。correctness RPO=0 不可达时，恢复=全局停机 + 潜在丢失窗口人工外部对账，禁止自动 re-admission。
5. **P2 outcome 先到直接边：接受，已写入 A-2 §8.1**（`ENQUEUED → OUTCOME_RECEIVED` 直接边 + 迟到 receipt 幂等吸收 + receipt 不改执行状态）。
6. **B 复核的两条补条已同步落 A-2 §6**：callback 启动门 `now + declaredExecutionBudgetMs ≤ expiry`（装不下即拒）+ 每步复检；MATCH_AND_CLICK 全程持续持 lease 且 capture/match 期间 lane 无旧请求。

**工件登记：**
- **A-2** `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` — 已产出（含本轮全部修正）。内容 SHA-256=`101d238d8f65c0a34381845bd6f5c232bbffcdfae8e5dae9ea2b4a55702138bd`，commit hash 待批量提交补登。覆盖：envelope 字段表、scope 矩阵、全消息注册表、ActionPlan 栅栏、五态重放矩阵、outbox（含直接边）/lease/fence（含换代 barrier）三状态机、精确字节帧签名、时钟规则、常数表、拒绝码。B 可即时审。
- A-3/A-6/A-7 按产出序推进中（#3/#4 定案随 A-3/A-7 落地）。

B (2026-07-12 Final #3，A-2 工件审查):

已核验 A-2 文件 `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`，实际 SHA-256 与 A 登记的 `101d238d...02138bd` 一致。工件已正确落实：五态正向重放白名单、outcome-before-receipt 直接边、PG connection generation + drain barrier、callback 启动预算门、每步 expiry 复检，以及 MATCH_AND_CLICK 不把 OpenCV 放入 exclusive callback。但 A-2 当前仍有 7 项 P1 + 2 项 P2，不能通过工件审查。

1. **P1：wire framing 自相矛盾，签名字节无法唯一实现。** §2 把 `signature` 放在 JSON Envelope 内，§9 又说 signature detached、位于 text frame 外固定前缀；标准 WebSocket text frame 没有协议自定义的“帧外前缀”。`payloadDigest=SHA-256(payload 原始字节)` 也没有定义如何从包含 object 的 JSON 中无歧义截取原始 payload 字节。A-2 必须选择并完整定义唯一 wire layout、长度/编码/解析边界和被签字节，例如固定二进制 framing + UTF-8 body，或外层 wrapper 携带 base64 exact signed bytes；不得同时保留“JSON 内 signature”和“帧外 detached”两种说法。
2. **P1：首次 HANDSHAKE 无法满足正常 Envelope 的 fence 要求。** §2/§3 要求 DEVICE_SCOPE 必填 connectionFence/sequence，但客户端首次连接尚未获得 PG 新 generation。必须定义独立 bootstrap/auth framing：在 TLS/设备认证完成后，以无业务执行权的 HELLO 发起，服务端 PG CAS 生成 fence 并返回 challenge/negotiation，客户端确认并安装最高 fence 后 normal fenced envelope 才启用；bootstrap 消息不得携带业务 payload，也不得在握手完成前 dispatch。不能让客户端猜 fence 或用 0 穿过正常验证器。
3. **P1：`streamId` 只有 `DEVICE_CONTROL | WINDOW_TASK` 两个 enum，会让所有窗口共享一个 sequence。** 一台设备多窗口时，任一窗口 gap 会连坐其他窗口停机，退回 Q2 最初否决的设备级全局序号。需要拆成 `streamKind` 与稳定 `streamKey`：DEVICE_CONTROL 只有一个 key；WINDOW/TASK 流至少按 windowRegistration/incarnation/taskRun epoch 唯一标识。sequence 作用域应为 `(connectionFence,direction,streamKey)`，并定义 taskRun 创建、终态和 rebind 时 stream 生命周期。
4. **P1：scope 字段没有落入 Envelope，receipt 也无法引用原消息。** §3 宣称 WINDOW/TASKRUN scope 必填 windowId/incarnation/taskRunId/actionId，但 §2 Envelope 没有这些字段，也未定义统一 `scope` object。`MESSAGE_RECEIVED` 自己运行在 DEVICE_CONTROL stream，却只含 messageId/actionId?/digest，无法证明 §8.1 所称“同原 fence/stream/sequence”。必须增加签名覆盖的 scope object，并让 receipt 明确携带 `receivedConnectionFence/receivedStreamKey/receivedSequence/messageId/payloadDigest`；其自身 sequence 与被确认消息的 sequence 必须分开。否则 receipt 可以跨窗口/stream 错配。
5. **P1：START_TASK 的 scope 与身份公式是循环的，TASK_COMMAND 也缺 ACK。** 表中 `WINDOW→TASKRUN` 不是一个可验证 scope；请求发生时 taskRunId 尚不存在，却用 taskRunId 参与 `sessionId=SHA256(...taskRunId...requestId)`。应把 START_TASK 定义为 WINDOW_SCOPE request，以 `(authScope,windowRegistrationId/incarnation,requestId)` 幂等地产生 taskRunId；ACK 再进入 TASKRUN_SCOPE。TASK_COMMAND 必须有 requestId、`TASK_COMMAND_ACK` 和重复请求返回原结果的状态语义，且明确 pause/stop 本地先行事实与云端状态推进的区别。
6. **P1：ActionPlan 缺失 Q2 已批准的 stale-frame 字段和 GenericMatch 强类型 payload。** §6 没有 expected window geometry、DPI、coordinateSpace；`basedOnFrameId | noFrameReason` 的条件必填规则也未列成 schema。MATCH_AND_CLICK 只有 prose，没有 templateId/hash/payload、matchAlgorithmVersion、窗口相对 ROI、threshold、clickOffset、result mode、max age 和 outcome fact 的字段/状态定义。A-2 必须补齐这些签名字段，并定义“匹配成功但输入未开始”“匹配失败”“匹配后栅栏失效”分别如何映射 OBSERVED/NOT_EXECUTED/ActionOutcome，不能交给实现者猜。
7. **P1：协议硬安全上限不能随 quotaProfile 任意上调，执行预算也必须由本地重算。** MAX_INPUT_STEPS、MAX_STEP_SLEEP、control bytes、inline ROI、observation duration 等应有 client/protocol 编译期 absolute cap；quotaProfile/协商值只能取更小值。`declaredExecutionBudgetMs` 不能只信云端声明，本地必须按经过 allowlist 的 steps、每步最坏耗时和 sleep 重算，并要求声明值不小于推导值且 `now + derivedBudget <= expiry`，否则拒绝。否则被签但错误的低预算可绕过 forced-handoff 安全证明。

另有 2 项 P2：

- **Q3 drain proof 条件在 §8.2 被弱化。** “可信 outcome（单飞下即排空证明）”必须恢复完整限定：outcome 仅在 exclusive callback 已退出、全部物理步骤结束且 lane 已释放后才能作 drain proof，并绑定 actionId/leaseEpoch/connectionFence；否则必须等 `INPUT_LANE_DRAINED`。
- **消息注册表应一行一个 wire type 和唯一 scope。** `HANDSHAKE / HANDSHAKE_ACK`、`START_TASK / START_TASK_ACK`、`WINDOW/TASKRUN` 这类合并写法不能直接生成 schema validator；CAPTURE_SPEC/FRAME_REF 若同时允许 WINDOW_SCOPE 与 TASKRUN_SCOPE，也必须拆成明确 variant 或在注册表列出两种合法 scope 及各自 required fields。ENQUEUED 是路由诊断时，还要明确它不作为 PG outbox 权威完成态。

**A-2 通过条件：**修正以上 7 项 P1 + 2 项 P2，更新内容 hash 并在 Final Review 登记。A-2 通过只代表协议工件通过，不代表整份设计 Final PASS；A-1/A-3 至 A-7 仍需分别交付与审查。

A (2026-07-12 Final #3，A-2 v2 重写回应):

七项 P1 + 两项 P2 全部接受，A-2 已整体重写为 v2（内容 SHA-256=`105935ee5fb4dbe396000e07dd814b4464dd3449298e5347b7971092d91e0f63`）。逐项落点：

1. **wire framing（P1-1）**：定案为 **WS binary frame**：`MAGIC "TC" | FRAME_KIND(BOOTSTRAP|FENCED) | SIG_LEN | SIGNATURE | SIGNED_BYTES(Envelope UTF-8 JSON 精确字节)`。signature 彻底移出 JSON（detached 在帧头）；`payload` 字段改为 base64(payload 字节)，payloadDigest=SHA-256(解码后字节)——字节边界由 base64 天然唯一，无截取歧义。"JSON 内 signature"与"帧外前缀"的矛盾消除（v2 §2）。
2. **bootstrap（P1-2）**：新增 FRAME_KIND=BOOTSTRAP 独立状态机：`HELLO(无 fence 无业务) → 服务端 PG CAS 出新 generation → FENCE_GRANT(fence+challenge+算法协商+版本互验) → FENCE_ACK(安装最高 fence)` → FENCED 帧才启用；bootstrap 不携业务 payload、完成前零 dispatch、客户端永不猜 fence（v2 §4）。
3. **stream 模型（P1-3）**：拆为 streamKind + streamKey，sequence 作用域=(fence, direction, **streamKey**)；WINDOW 流 key=`w:{registrationId}#{incarnation}`、TASK_RUN 流 key=`t:{taskRunId}`，各自生命周期成文——单窗口 gap 只停自己，不再退化回设备级全局序号（v2 §5）。
4. **scope object（P1-4）**：ScopeObject 落进 Envelope 随帧被签，四档 required/forbidden 表成文；MESSAGE_RECEIVED payload 补全 `{receivedConnectionFence, receivedStreamKey, receivedSequence, receivedMessageId, receivedPayloadDigest}`，回执自身 sequence 与被确认消息分离——跨 stream 错配过不了核验（v2 §6/§7.5）。
5. **START_TASK 去循环（P1-5）**：改为 WINDOW_SCOPE 请求，taskRunId 由服务端幂等生成=H(authScope|windowRegistrationId#incarnation|requestId)，ACK 后才进 TASK_RUN stream；TASK_COMMAND 补 requestId + TASK_COMMAND_ACK + 重复重放，并明确"本地先行生效=事实上报，云端推进=ACK 体现"（v2 §7.3/§7.4）。
6. **ActionPlan 补全（P1-6）**：expectedWindowGeometry/DpiScale/coordinateSpace 补入；frameBasis 与 noFrameReason(枚举 allowlist) 二选一必填成 schema；MatchSpec 强类型化（templateHash/algorithmVersion/roi/threshold/clickOffset/resultMode/maxAge）；六行 outcome 映射表定死（匹配失败=NOT_EXECUTED+matchFact、栅栏失效=NOT_EXECUTED+rejectionCode、完成=EXECUTED、中断=STOPPED、REPORT_ONLY=OBSERVED），不留实现者猜的空间（v2 §8/§8.3）。
7. **硬顶双列 + 预算重算（P1-7）**：常数表拆 ABSOLUTE（编译期，任何配置不可上调）与协商默认（quotaProfile 只可更小）两列；本地按 allowlist 步骤最坏耗时重算 derivedBudget，三条件启动门（declared≥derived、now+derived≤expiry、步数≤ABSOLUTE）缺一即拒 `BUDGET_REJECTED`——低报预算绕不过 forced-handoff 证明（v2 §8.2/§11）。
8. **P2 两项**：drain proof 完整限定恢复原文（callback 已退出+全步结束+lane 已释放+三元绑定，v2 §9.2）；注册表改一行一 wire type 唯一 scope，CAPTURE_SPEC/FRAME_REF 拆 `_W` 变体，ENQUEUED 显式标注"绝非 PG 权威完成态"（v2 §7/§9.1）。

请 B 复审 A-2 v2。
- **A-3** `docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md` — 已产出。内容 SHA-256=`3cd96efe7433eeba3fcad49f495269cf699d5def8d659e7381849b999726e263`，commit hash 待批量提交补登。覆盖：存储角色总则、数据三级分级与灾备要求（correctness RPO=0 + 恢复强制序列）、PG 全表规格（身份/连接/窗口/任务/lease/action_ledger/outbox/causation_link/authority_ledger 单向状态机/记忆四表/资产/grant/配置/审计/evidence_manifest）、六大事务边界（T1-T6）、Redis 键空间（全可丢）、对象存储布局与保留期、增长控制（QA F5 教训成文）。B 可即时审。

B (2026-07-12 Final #4，A-3 工件审查):

已核验 A-3 文件 `docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md`，实际 SHA-256 与登记的 `3cd96efe...6e263` 一致。PG 权威/Redis 可丢、correctness RPO=0、connection generation、authority manifest 只作提交后投影和六类事务方向成立；但作为“列/键/约束/索引/事务边界”工件，当前仍有 8 项 P1 + 3 项 P2。

1. **P1：租户隔离没有数据库约束。** 多数表只各自存 tenant_id/user_id/device_id，却用单列 FK 或无 FK；数据库允许 tenant A 的 task/window/action 引用 tenant B 的 user/device。`login_identity UNIQUE` 还是全局而非 tenant-scoped。A-3 必须统一采用 `(tenant_id,id)` composite unique/FK 或等价强约束，并定义 PostgreSQL RLS/服务角色策略；所有查询、唯一键、upsert 和管理接口都必须以认证 tenant 为条件。不能只靠应用层记得加 tenant filter。
2. **P1：Q7 的 100% 因果元数据没有事实表。** 当前只有无 FK 的泛型 `causation_link`，却没有 frame/capture metadata、window binding fact、ObservationPlan/Fact、MESSAGE_RECEIVED、action checkpoint/physical execution fact 等不可变记录，无法重建 `capture -> plan -> receipt -> outcome -> observation -> verdict`，也无法证明 stale-frame、错 binding 或 split-brain。需要强类型事实表或一个有 schema/version/唯一约束的 append-only protocol fact ledger，并让 causation 边引用实际存在的 immutable IDs。
3. **P1：lease holder 与 ActionPlan 栅栏字段在表中丢失。** `input_lane_lease` 没有 lease_id、holder clientSession/connectionFence/outstanding action；`action_ledger` 没有 tenant/device/window/incarnation/bindingGeneration/inputLaneId/leaseId、expected geometry/DPI/coordinateSpace、policy/asset digest 和 successorActionId。仅靠 task_run 间接关联不足以审计换手与错窗拒绝。必须保存每次签发时的 exact snapshot/fence，并建立唯一/外键约束。
4. **P1：START_TASK 身份仍循环，request 表也混淆命令类型。** `session_id=SHA256(...taskRunId...startRequestId)` 在 taskRun 创建前依赖 taskRunId；`start_request` 却又让 task_run_id FK 指向待创建行，并声称同时服务 TASK_COMMAND。应按 A-2 返修要求，以 `(tenant/device,windowRegistration/incarnation,requestId)` 幂等生成或查回 taskRunId；命令请求至少加入 request_kind/target_task_run/command/response_digest。还需增加 T0 事务：锁定幂等 request → 创建/复用 task_run → 记录初始 revision/lease/action/outbox → 保存原响应，保证重复 START 不产生第二 run。
5. **P1：outbox 的权威状态与路由诊断仍混在一列，receipt/outcome 也未进入事务清单。** `state` 包含 ENQUEUED，但正文又说 ENQUEUED 仅 Redis 诊断；这会让实现者把易失 enqueue 写成权威推进。应把 route attempt/owner instance 放独立诊断表或 Redis，PG outbox 只持 durable pending、client receipt/outcome 与 closed facts。T1 必须在接受 outcome 时同时核验并推进对应 outbox；另需 receipt 事务，按原 fence/stream/sequence/message/digest 幂等写 CLIENT_RECEIVED。direct outcome edge也必须原子完成 transport-delivered 与 action outcome。
6. **P1：authority_ledger 的单行可变状态不能证明 epoch 历史和唯一 ACTIVE writer。** `scope_id PK` 反复改 state/epoch 会覆盖 source epoch，无法可靠审计 manifest lineage，也没有约束保证每 scope 只有一个 ACTIVE epoch。应拆分 append-only authority_epoch 与 authority_transfer（source/target/transferId/PREPARED/COMMITTED/LSN/manifest hash），用 partial unique/排他约束保证每 scope 至多一个 ACTIVE writer；T5 明确 manifest 只能在 COMMITTED 后签发，崩溃恢复只读 PG 已提交状态。
7. **P1：canonical memory 的“幂等导入、不可变版本、自动发布”没有唯一约束。** `memory_canonical` 只有随机 PK，同一 baseline/content 可重复导入；memory_version 缺 parent FK、tenant/context/content 的去重键与发布状态转换约束；发布/统计也没有 distinct taskRun 事实锚。必须增加 canonical import identity、payload/context digest、lineage 和 immutable-version unique constraints，并把 run-level conclusive verdict/publication creation/audit 的原子唯一键写入 T4，才能保证重投不重复发布。
8. **P1：durable-audit RPO≤5min 与“同事务、不可静默丢失”冲突。** memory 发布、authority transfer、设备/密钥撤销和高危管理操作的 audit 与 correctness 状态同事务产生，不能允许恢复后状态存在而审计丢 5 分钟。应把与 correctness 变化绑定的审计纳入 RPO=0 同步提交；只有独立低风险运营审计可使用≤5min并显式缺口。A-7 的灾备分级必须与此一致。

另有 3 项 P2：

- **约束与索引仍不完整。** status/enum transition 需要 CHECK；message_id、payload/action digest、session/request identity、active task per window、outstanding action per lane 等需要明确唯一/partial index；所有 FK 与常用 tenant/window/task 查询应列索引，不能只给 action/outbox 两组。
- **对象存储锚点与 GC 模型缺失。** 需要 frame/evidence/asset object metadata 表记录 tenant、contentHash、size、retentionClass、encryption key、refCount/legalHold/deletedAt；删除后保留 digest 但不能让 verdict 指向未声明丢失的对象。upload grant consume 事务也必须同时核验 tenant/device/frame/digest/expiry，而非只按 grantId+consumed。
- **“无全表扫描”与 projection 全量重建需区分在线路径和离线维护。** memory projection 重建、归档/GC 可以有界分片扫描，但必须有 cursor/checkpoint、限流和前台 worker 隔离；正文不应同时宣称“无全表扫描路径”又要求未约束的全量重建。

**A-3 通过条件：**修正上述 8 项 P1 + 3 项 P2，更新内容 hash 并登记。A-2 的 7 项 P1 + 2 项 P2 仍独立待修；任何一个工件未通过都不能给 A/B Final PASS。

A (2026-07-12 Final #4，A-3 v2 重写回应):

时序说明：A-2 已先按 B Final #3 重写为 v2（登记见 A Final #3，SHA-256=`105935ee...d91e0f63`），请 B 复审。本条回应 A-3 审查：八项 P1 + 三项 P2 全部接受，A-3 已重写为 v2（SHA-256=`60819990da92d52813cf44980c4a97cec61b3597817889773c0c827a5e579634`）。逐项落点：

1. **租户隔离进数据库（P1-1）**：全表复合键规约 `(tenant_id, id)` PK/FK、login_identity 改租户内唯一、RLS 三档角色策略成文——应用层 filter 降为第二道防线（v2 §0）。
2. **协议事实账本（P1-2）**：新增 append-only `protocol_fact` 表（11 种 fact_kind：FRAME_META/WINDOW_BINDING/MESSAGE_RECEIVED/ACTION_CHECKPOINT/LEASE_TRANSITION/FENCE_TRANSITION 等，幂等唯一键），causation_link 的 from/to 必须引用实际存在的 immutable 行并由事务断言——因果图 100% 校验有了事实基础（v2 §4）。
3. **栅栏字段入表（P1-3）**：input_lane_lease 补 lease_id/holder 全景（clientSession/fence/outstanding action）；action_ledger 补 device/window/incarnation/bindingGeneration/laneId/leaseId/expected geometry/DPI/coordinateSpace/policy+asset digest/successorActionId——签发时 exact snapshot 全量落行（v2 §4）。
4. **START_TASK 身份去循环（P1-4）**：taskRunId=H(tenant|device|windowRegistrationId#incarnation|requestId) 本身即幂等锚（独立 session_id 公式取消，与 A-2 v2 §7.3 对齐）；request_ledger 带 request_kind/target/command/response_digest；新增 **T0 事务**保证重复 START 不产生第二 run（v2 §3）。
5. **outbox 权威/诊断分离（P1-5）**：PG outbox 删除 ENQUEUED 态（PENDING→CLIENT_RECEIVED→OUTCOME_RECEIVED→CLOSED），route_attempt 出 PG 入诊断表/Redis；T1' 在接受 outcome 时同事务核验推进 outbox（直接边原子）；新增 **T-receipt 事务**按五字段全核验幂等推进（v2 §5）。
6. **authority append-only 化（P1-6）**：拆 authority_epoch（partial UNIQUE 保证每 scope 至多一个 ACTIVE writer）+ authority_transfer（PREPARED/COMMITTED/LSN/manifest hash 全历史）；T5' 明确 manifest 只能 COMMITTED 后签发（v2 §6）。
7. **记忆唯一约束（P1-7）**：canonical 导入身份 UNIQUE(source, baseline_commit, content_digest)；version 补 parent 自引用 FK + UNIQUE(scope, kind, context_digest, payload_digest)；T4' 内发布唯一键防并发双发布，distinct taskRun 锚=verdict 事实行本身（v2 §7）。
8. **审计分级修正（P1-8）**：与 correctness 迁转绑定的审计（发布/移交/密钥撤销/高危管理）同事务写入、继承 RPO=0；≤5min 仅限独立低风险运营审计——A-7 将按此分级编写（v2 §1）。
9. **P2 三项**：约束/索引清单成表（v2 §9）；object_metadata 表（refCount/retention/encryption/legalHold/deleted_at，删除保留 digest 锚点）+ grant 五条件原子消费（v2 §7）；"无全表扫描"修正为"在线路径无扫描 + 离线维护有界分片扫描（cursor/checkpoint/限速/隔离连接池）"（v2 §8）。

请 B 复审 A-3 v2 与 A-2 v2。A-6/A-7 继续按序产出。

**工件登记（A 追加）：**
- **A-6** `docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md` — 已产出 v1。内容 SHA-256=`65ee28d36396fa62c743e1988a4d6c791271257b39de3076feb885b50c17a7c7`。覆盖：Principal/Role/Permission 矩阵（含高危操作再认证清单）、设备 enroll/unbind 生命周期（私钥不出 OS 密钥库）、token 签发/滚动刷新/撤销传播、签名密钥三域分治（连接会话域/资产发布域 keyring 轮换与 compromise 强撤销/部署域 secret 独立托管）、upload grant 单次消费、key/epoch 变化对运行态六行影响矩阵、trustedPublisher 治理、审计分级对齐 A-3。B 可即时审。
- **A-4** `docs/superpowers/specs/2026-07-12-thin-client-components-sequences.md` — 已产出 v1。内容 SHA-256=`8feaf46d4696695686fadc11636a39133a7dfdbcdeeb8209092f658d0b22decb`。覆盖：组件/三级信任边界图（T1 网络/T2 租户 RLS/T3 admin 通道）、六核心时序（正常动作循环含 T1'/T-receipt 事务锚点、stop 永不 FAILED、断线重连对账、lease 正常+强制换手、S7 原子切换、S8 整体回滚含 manifest 后置签发）、时序↔零不变量映射表。消息名与 A-2 v2、表名与 A-3 v2 一一对应。B 可即时审。
- **A-5** `docs/superpowers/specs/2026-07-12-thin-client-master-acceptance-matrix.md` — 已产出 v1（骨架）。内容 SHA-256=`d41b6cf3684b4efd4fd2c748fff051ed1bfe60865438d55358da00401edcf09b`。覆盖：稳定 requirementId 全量追踪 §3-§10（18 行）+ Q1（7 行 + REQ-M-* 方法级分区显式待 A-1 注入并标注"空缺即阻塞切换"）+ Q2/Q3（13 行）+ Q4（8 行）+ Q5（8 行）+ Q6（10 行）+ Q7（10 行）+ 终审跨章节决定（9 行）；四态状态规则 + release identity 绑定 + 依赖失效规则；§9 用户裁决登记区（五项含 owner 与最迟裁决阶段，B Final#1 P2-2 收口）。方法级分区待 A-1 交付后注入并重登 hash。B 可即时审。
- **A-7** `docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md` — 已产出 v1。内容 SHA-256=`8cfb0373e4bb25f6e9327fe223992311601c02dfaef84d52c5ad359469949666`。覆盖：灾备三级分级与 A-3 严格一致（correctness RPO=0=同步 standby，V1 单节点开发替代方案 + 生产真同步副本为 S7 前置门；不可达时唯一合法姿态=全局停机+人工对账）、备份策略（PITR/对象 manifest 对账/key 独立托管+key 自身恢复演练）、四场景 RTO 目标、恢复后八步强制序列（authority 校验先行、恢复点后 action 全标 UNKNOWN、逐窗口 fresh frame+rebind 后才逐窗口重开）、整体回滚与灾备的边界（回滚走 authority transfer 非灾备恢复）、六类 restore drill 及通过标准、监控挂钩（恢复路径不豁免任何零不变量）。B 可即时审。

B (2026-07-12 Final #5，A-2 v2 复审):

已核验 A-2 v2 实际 SHA-256=`105935ee5fb4dbe396000e07dd814b4464dd3449298e5347b7971092d91e0f63`，与 A 登记一致。B Final #3 的 7 项 P1 + 2 项 P2 均已按原要求修正；但在把 v2 当成可直接生成 codec/validator/executor 的协议工件复核后，仍有 4 项 P1 + 2 项 P2，A-2 暂不通过。

1. **P1：bootstrap 仍没有唯一可实现、可防降级的认证 transcript。** §2 只定义正常 Envelope 的 `SIGNED_BYTES`，却没有 HELLO/FENCE_GRANT/FENCE_ACK 的精确 wire schema、双方 nonce、被认证字段和签名/MAC 输入；`SIG_LEN` 仍写“按认证方式定义”。FENCE_GRANT 还同时承担算法协商，但没有规定算法只能由服务端从设备预注册 allowlist 选择，也没有证明 FENCE_ACK 绑定本次 TLS/设备身份、client/server nonce、版本集合和新 fence。必须给 bootstrap 单独的精确字节 schema；签名域还要覆盖协议 domain tag、MAGIC 与 FRAME_KIND，不能只签 JSON body，防止跨 frame-kind/跨协议重解释。A-6 可以定义 key 生命周期，但不能替代 A-2 的 wire transcript。
2. **P1：Q5 已批准的 critical-control 保留通道没有落入 stream/sequence 设计。** TASK_COMMAND、ACTION_OUTCOME 仍跑普通 TASK_RUN stream；若该 stream 前面一条 CAPTURE/普通 fact 发生 gap，stop/pause 事实上报和 outcome 会一起被顺序门堵住。REVOKE_LEASE、INPUT_LANE_DRAINED、MESSAGE_RECEIVED、fence/resync 也没有注册表级 `CRITICAL/NORMAL/BULK` 分类、独立有界队列/保留容量和独立 sequence 路径。必须让关键正确性事实不受普通 task stream gap、bulk 背压或慢发送队列阻塞；本地 stop/pause/emergency 仍立即生效，关键通道只负责及时把该事实和目标 task/action 因果键送达云端，不新增本地业务决策。
3. **P1：MATCH_AND_CLICK 的 `steps[]` 仍不可生成且会造成坐标双重权威。** 云端签 plan 时尚不知道本地 match box，无法预先给出实际 move/click 坐标；若本地改写云端 `steps[]`，又违反 ActionPlan 不可改写。该 planKind 应禁止任意输入 steps，只允许签名的固定 recipe：GenericMatchExecutor 从 match box + clickOffset 机械导出唯一 move→bounded delay→left-click bundle，并在 outcome 记录 box、最终窗口相对/屏幕绝对点和派生 bundle digest。ActionPlan 还必须显式携带 `inputLaneId`，否则本地无法证明 leaseId/epoch 授权的是正在使用的物理 lane。
4. **P1：parser/时效安全常数仍可被错误配置削弱。** `CLOCK_SKEW_SAFETY_MS` 是 forced-handoff 的最小安全垫，不能按“quotaProfile 只可更小”协商；应区分 maximum cap、minimum safety floor 与协商值，并要求 safety ≥ 实测 uncertainty。`maxFrameAge` 也缺协议 absolute cap。二进制帧在分配/JSON 解析前还缺 `MAX_SIGNATURE_BYTES`、`MAX_SIGNED_BYTES`、base64 decoded payload 上限、JSON nesting/string/collection 上限；只给 `MAX_CONTROL_MSG_BYTES` 不能阻止大 SIG_LEN、解码膨胀或深层 JSON 资源攻击。执行预算的本地最坏耗时表也必须版本化并覆盖 focus/move/click provider 的固定最坏耗时，而不只是 sleep。

另有 2 项 P2：

- `ACTION_OUTCOME_ACK` 的“successor 可随 ACK 下发”必须明确为：同一 PG 事务生成、但通过独立 ACTION_PLAN envelope/messageId/actionId/outbox 投递；不得把下一动作嵌入 ACK 绕过 sequence、签名与 write-before-send。
- `LEASE_GRANT` 不应同时承载 `KEEP/RELEASE`。建议拆成唯一语义的 GRANT、KEEP/CONTINUE fact 与 RELEASE/REVOKE 命令，并分别定义 ACK、幂等键和合法状态边；同时统一 `DRAINING` 与权威状态机 `REVOKING` 的词汇。

**A-2 v2 复审结论：Review required / 不通过。** 修正以上 4 项 P1 + 2 项 P2、更新 hash 并登记后再复审。A-2 未通过前，依赖其消息名和时序的 A-4 只能登记，不能批准。

A (2026-07-12 Final #5，A-2 v3 回应):

四项 P1 + 两项 P2 全部接受，A-2 重写为 v3（SHA-256=`677014a8f4a2af0447947f944c3e1a75451b753819a02460520ffb4df3e10656`）。逐项：

1. **bootstrap transcript（P1-1）**：v3 §2 帧头加 DOMAIN_TAG + SIG_ALG，**被签内容扩为 MAGIC‖KIND‖DOMAIN_TAG‖SIG_ALG‖SIGNED_BYTES**（防跨 frame-kind/跨协议重解释）；v3 §4 给 HELLO/FENCE_GRANT/FENCE_ACK 三段精确 JSON schema，含 clientNonce+serverNonce+tlsChannelBinding（绑定本次 TLS）+ 双向 echo（抗重放/抗跨会话拼接）；算法只能由服务端从设备 enrollment 预注册 allowlist 选（客户端不得强制降级）。SIG_LEN 不再"按认证方式定义"，受 MAX_SIGNATURE_BYTES 约束。
2. **critical 通道落地（P1-2）**：v3 §5 引入 channelClass CRITICAL/NORMAL/BULK，进 envelope + 注册表每行标注；sequence 域扩为 `(fence,direction,channelClass,streamKey)`——一个 class 的 gap 不连坐另一个 class；TASK_COMMAND/ACTION_OUTCOME/REVOKE_LEASE/INPUT_LANE_DRAINED/MESSAGE_RECEIVED/RESYNC/fence 全部 CRITICAL，保留容量永不 THROTTLED，不受 NORMAL/BULK 背压阻塞。
3. **MATCH recipe（P1-3）**：v3 §8.2 定死 MATCH_* **禁带 steps[]**；GenericMatchExecutor 从 match.box+clickOffset 机械导出唯一固定 recipe（move→micro-sleep→click），坐标单一权威=本地导出（消除双权威），outcome 记 box+最终点+derivedBundleDigest；ActionPlan 新增 `inputLaneId` 显式字段。
4. **安全常数分级（P1-4）**：v3 §11 常数表拆 ABSOLUTE cap / SAFETY floor / 协商默认三列；CLOCK_SKEW_SAFETY_MS 改 **floor 语义**（协商只可更大，且 ≥ 实测 uncertainty）；MAX_FRAME_AGE 加 absolute cap；新增 MAX_SIGNATURE_BYTES/MAX_SIGNED_BYTES/MAX_DECODED_PAYLOAD_BYTES/JSON 深度+串+集合上限（分配/解析前拦 decode 膨胀与深层 JSON 攻击）；derivedBudget 由版本化最坏耗时表推导，覆盖 focus/move/click provider 各自最坏耗时，非仅 sleep。
5. **P2-1 successor**：v3 §7 明确 ACTION_OUTCOME_ACK 不携后继；successor 同一 PG 事务生成，经**独立 ACTION_PLAN envelope（独立 messageId/actionId/outbox）**正常签名+sequence+write-before-send 投递，不嵌 ACK 绕过。
6. **P2-2 lease 拆分**：v3 §7 拆 LEASE_GRANT/LEASE_KEEP/LEASE_RELEASE/REVOKE_LEASE 四个唯一语义 wire type，各有 ACK/幂等键；DRAINING 别名删除，统一 REVOKING。

请 B 复审 A-2 v3。A-3 v3 下一轮产出（8 P1 工作量大，单独一轮）。

B (2026-07-12 Final #6，A-3 v2 复审):

已核验 A-3 v2 实际 SHA-256=`60819990da92d52813cf44980c4a97cec61b3597817889773c0c827a5e579634`，与 A 登记一致。B Final #4 的租户复合键、RPO=0、outbox 诊断分离、authority 分表和对象元数据方向已经吸收；但当前工件仍有 8 项 P1 + 3 项 P2，A-3 暂不通过。

1. **P1：v2 不是自包含数据模型，多个权威表实际没有 schema。** `memory_use / memory_verdict`、`asset_descriptor / upload_grant`、Redis 键空间、对象存储布局和增长控制都只写“同 v1”；但该文件当前为 untracked v2，仓库没有可冻结引用的 v1 commit/hash。审计表、evidence manifest、配置/策略表也只在总则出现。A-3 必须把全部列、复合 FK、唯一键、状态与事务边界写入当前工件，或引用一个真实存在且带 hash 的冻结附录，不能依赖已经被覆盖的草稿版本。
2. **P1：全表租户约束的总则与实际表定义不一致。** `authority_epoch`、`authority_transfer` 和 `route_attempt` 没有 tenant_id；若 scope 是全局也必须声明 scope_kind/global singleton，若是租户级则必须使用复合键。RLS 还需写明认证层如何以不可由业务 payload/普通 SQL 任意改写的方式设置 transaction-local tenant context，以及 admin/maintenance 的跨租户授权边界。当前“所有租户表”声明不足以证明这些遗漏不会形成跨租户引用或诊断串线。
3. **P1：请求幂等只按 requestId 命中，没有请求内容冲突保护。** `request_ledger` 缺 request_payload_digest、payload schema/version 和按 request_kind 的 required CHECK；同一 `(tenant,device,requestId)` 被重用于不同 task 参数、不同 command 或不同 target 时会静默重放旧 response。必须先比较原请求 digest，完全相同才幂等重放，不同 digest 返回冲突；START_TASK 与 TASK_COMMAND 的 window/target/command 空值规则和复合 FK 需由数据库约束。
4. **P1：protocol_fact 既不能拒绝冲突事实，也没有覆盖 Q7 的完整控制因果链。** UNIQUE `(tenant,fact_kind,subject_id,content_digest)` 允许同一 subjectId 以两个不同 digest 插入两条相互冲突的 FRAME_META/receipt；自然幂等身份应唯一，digest 不同必须冲突拒绝。fact_kind 又缺 CONTROL_ENVELOPE、ACTION_PLAN、ACTION_OUTCOME 等不可变事实；仅靠可变 action_ledger 和 S2C outbox 不能证明全部 C2S/S2C envelope 的 fence/stream/sequence 与 `capture→plan→receipt→outcome→observation` 100% 因果闭包。
5. **P1：T-receipt 声称核验五字段，但 outbox 没保存原 stream/sequence。** outbox 只有 issued_connection_fence、messageId 和 payloadDigest，没有 issuedStreamKey、issuedSequence、direction/scope；服务端无法把 MESSAGE_RECEIVED 的五字段与原签发 envelope 全量比对。必须在 durable outbox/envelope fact 中保存原 fence+direction+streamKey+sequence+messageId+digest，并由 receipt/outcome 事务引用同一不可变记录。
6. **P1：单飞与 lease/action 栅栏没有被数据库约束。** `input_lane_lease` 上的 partial UNIQUE 是冗余的，因为该表本来每 lane 只有一行；它不能阻止 action_ledger 同一 lane 同时出现两条未关闭 DISPATCHED action。应在 action_ledger/独立 outstanding 表上建立 lane 级 partial UNIQUE，并给 leaseId/epoch 建立可历史引用的唯一实体/FK。action_ledger 的“exact snapshot”还缺 expectedFrameHash、frameCaptureMarker/maxFrameAge 或 noFrameReason，successorActionId 也无自引用约束，无法独立审计 stale-frame 与 successor 单飞。
7. **P1：私有记忆的所有权、幂等导入和不可变晋级仍不成立。** memory_version 没有 private owner key，UNIQUE `(tenant,scope,kind,contextDigest,payloadDigest)` 会让同租户两个用户的相同 PRIVATE 内容互相冲突；`source_user_id` 是 provenance，不能替代 owner/ACL。memory_canonical 的唯一键又缺 userId，且 PostgreSQL 普通 UNIQUE 会把 NULL baseline_commit 视为互不相等，RUNTIME 重投仍可重复插入。还要删除“PRIVATE→CANDIDATE→PUBLISHED 迁转”这种原行改 scope 的表述，晋级只能 INSERT 带 parent lineage 的新 immutable 行。
8. **P1：UNKNOWN 终态规则与已批准 Q3 恢复语义冲突。** A-3 写“UNKNOWN 永不改写”，而 §12.3 明确允许迟到的可信同 digest outcome 解除原 UNKNOWN。必须定一种唯一实现：允许受约束的 UNKNOWN→真实 outcome 迁转并保留旧 UNKNOWN fact，或保持 action row UNKNOWN 但追加 immutable late-resolution fact 并让 recovery disposition 引用它；不能直接拒绝可信迟到 outcome，也不能覆盖历史不留痕。

另有 3 项 P2：

- authority_epoch 标为 append-only，但 T5' 又 UPDATE 源 epoch state；需明确“epoch 身份行不可删除、状态迁转另有 append-only fact”或改名。`committed_lsn` 也不能在同一事务提交前假装知道精确 commit record LSN；manifest 应引用稳定 authority revision/transferId，并在 commit 后生成可验证的 LSN/snapshot 投影。
- object_metadata 只有可变 ref_count，没有 object_reference/owner 表与引用增减事务，GC 无法证明计数可重建或 verdict/asset/frame 仍在引用；应把引用关系做成租户内唯一事实，再由投影计算 refCount。
- 约束清单还需补 active window per `(tenant,device,windowId)`、leaseId 唯一/历史 FK、request/outbox 常用复合索引，以及所有 parent/canonical/source-user FK 的完整租户列；“FK 列默认建索引”不是可审查的实际索引清单。

**A-3 v2 复审结论：Review required / 不通过。** 修正以上 8 项 P1 + 3 项 P2、形成自包含工件、更新 hash 并登记后再复审。A-3 未通过前，依赖其表名/事务边界的 A-4/A-7 只能做一致性预审，不能批准。

A (2026-07-12 Final #6，A-3 v3 回应):

八项 P1 + 三项 P2 全部接受，A-3 重写为 **v3 自包含版**（SHA-256=`acc2f85c6b9a50be0ea0028ee4796f7558ce08e9e1632236b3dfa785ed218edc`，全表列/键/约束/事务均在文内，不再"同 v1"）。逐项：

1. **自包含（P1-1）**：memory_use/verdict、asset/grant、Redis 键空间、对象布局、增长控制全部写全列（v3 §8/§9/§14/§12）；审计/manifest/配置表在相应节展开，不依赖被覆盖草稿。
2. **authority/route 补租户 + RLS 机制（P1-2）**：authority_epoch 加 scope_kind(GLOBAL/TENANT)+scope_ref（GLOBAL 走 singleton 断言），route_attempt 补 tenant_id；RLS 明确 `SET LOCAL app.tenant_id`（受信入口设置、transaction-local、业务 SQL 不可改写），app_rw 无 BYPASSRLS，跨租户仅 admin_ops/maintenance 两受控通道（v3 §0）。
3. **请求内容冲突保护（P1-3）**：request_ledger 加 request_payload_digest + schema_version + request_kind CHECK；幂等命中先比 digest，相同才重放，不同返 `REQUEST_CONFLICT`（v3 §3）。
4. **protocol_fact 冲突拒绝 + 全链（P1-4）**：UNIQUE 改 `(tenant,fact_kind,natural_identity)`（不含 digest），同自然身份第二个不同 digest=`FACT_DIGEST_CONFLICT`；fact_kind 补 CONTROL_ENVELOPE_C2S/S2C、ACTION_PLAN、ACTION_OUTCOME 等,覆盖全 C2S/S2C envelope（v3 §5）。
5. **outbox 存原字段（P1-5）**：outbox_envelope 加 issued_direction/issued_stream_key/issued_sequence，T-receipt 五字段全量比对原签发记录（v3 §6/§10）。
6. **单飞 DB 强制 + 栅栏字段（P1-6）**：新增 outstanding_action 表（PK per lane 从数据库层禁双 DISPATCHED）；action_ledger 补 expected_frame_hash/frame_capture_marker/max_frame_age/no_frame_reason + frame_basis CHECK + successor 自引用 FK + lease_id FK 引用 lease_entity（v3 §4）。
7. **私有记忆 owner/ACL + 幂等键（P1-7）**：canonical UNIQUE 含 owner_user_id、baseline_commit NOT NULL 哨兵（解 NULL 互不相等）；version UNIQUE 含 owner_user_id（两用户相同 PRIVATE 不冲突）+ RLS owner 谓词；晋级=INSERT 新 immutable 行带 parent，删除"原行改 scope"表述（v3 §8）。
8. **UNKNOWN 迟到解除（P1-8）**：定唯一实现——state 保持 UNKNOWN 不覆盖，追加 immutable late-resolution protocol_fact，action_ledger.late_resolution_fact_id 指向 + recovery_disposition=LATE_RESOLVED；业务不确定性据 late fact 解除，历史与 tombstone 永存；不同 digest 迟到=冲突拒绝（v3 §11，与 §12.3 统一）。
9. **P2 三项**：authority_epoch 身份行不可删、状态迁转经 append-only authority_epoch_state fact（committed_authority_revision 替代"提交前假装知道的 LSN"）（v3 §7）；object_reference 引用事实表，refCount=投影可重建（v3 §9）；约束/索引清单补 active-window/leaseId FK/常用复合索引（v3 §13）。

请 B 复审 A-3 v3。至此 A-2 v3、A-3 v3 均已按复审重写；A-4/A-5/A-7 依赖二者，待其通过后我将核对一致性并按需微调重登。

A (2026-07-12 Final #7，A-1 骨架交付):

A-1 迁移矩阵**骨架** `docs/superpowers/specs/2026-07-12-service-migration-matrix.md` 已交付（SHA-256=`d5385771b85139a4e79061fdcea009435b1ca27da663128bb05b783a1fbf77d7`）。诚实定级：这是**顶层完备盘点 + tier 初判 + 已有种子引用 + 隐式风险点**，不是完成的方法级底账。

已完成（真实进展）：
- 全业务包枚举完备（主库 344 类，按 task/service/vision/model/cloud 分区盘点，类数核实）；
- 顶层组件矩阵（任务编排层/25 个 Service/视觉层/数据模型层）各带 tier 初判 + 迁后 owner + 本地保留；
- 已有资产标为种子（修罗 243 方法分类、五倍 baseline、cloud/ 69 类现有云决策框架）；
- 隐式状态专项五项落位（fair-lock 排队/watchdog/kanda2/本地记忆/Catalog）。

明确缺口（诚实标注，对齐 A-5 做法）：
- 方法级 inventory（含继承/lambda/监听器/条件注册）= NOT_EVALUATED，需 javap/ASM 全库字节码扫描；
- 反向静态扫描零命中、入口可达闭包、配置资源穷尽、allowlist 构建证据 = 全部 NOT_EVALUATED；
- Q1 六项验收当前 0/6 达成。

完成路径两条（A-1 §5）：(a) 多代理 workflow 按包 fan-out（需用户 opt-in，token 成本高）；(b) 留 Q6 S0 冻结盘点阶段做（S0 本就是 S1 前置）。**在方法级底账完成前 A-1 不通过，A Final PASS 不给**——这是我不会松动的门。

工件登记：**A-1 骨架** SHA-256=`d5385771b85139a4e79061fdcea009435b1ca27da663128bb05b783a1fbf77d7`。至此七件工件全部有交付物（A-1 骨架/A-2 v3/A-3 v3/A-4/A-5 骨架/A-6/A-7），但 A-1 与 A-5 的方法级分区仍 NOT_EVALUATED，构成已知的、显式的、阻塞切换的缺口，非隐藏。

B (2026-07-12 Final #7，A-2 v3 复审):

已核验 A-2 v3 实际 SHA-256=`677014a8f4a2af0447947f944c3e1a75451b753819a02460520ffb4df3e10656`，与 A 登记一致。B Final #5 指出的 MATCH 坐标双权威、successor 嵌 ACK、lease 词汇混用和基础 parser cap 已按方向修正；但 v3 仍有 4 项 P1 + 2 项 P2，A-2 不能批准。

1. **P1：v3 不再是自包含、可生成 codec/validator 的 schema。** §6、§7、§9、§10、§12、§13 大量写“同 v2”，但 A-2 当前是 untracked 单文件，v2 已被覆盖且没有冻结 commit/hash 可引用。完整 Scope required/forbidden 表、全消息注册表、MESSAGE_RECEIVED schema、outbox/lease/fence 状态机、拒绝码与旧协议删除规则因此都不在可交付工件内。必须把这些内容完整并入 v3，或引用一个真实存在且内容 hash 固定的附录；不能让实现者从聊天历史还原协议。
2. **P1：bootstrap/会话签名仍有跨工件冲突，精确算法域没有闭环。** A-2 v3 帧头新增 `SIG_ALG`，HELLO/FENCE_ACK 用设备私钥签、FENCE_GRANT 用服务端 key 签；A-6 §4.1 却仍写“帧头不携算法字段”，并称 bootstrap 派生会话签名密钥，但没有 KDF/key-confirmation。当前一个 `SIG_ALG` 也没有区分“本帧 signer 算法”和“协商后的 fenced-session MAC/签名算法”，设备 key 与服务端 key 算法不同就无法唯一解释。必须统一 A-2/A-6：固定 DOMAIN_TAG 与算法 id 表、signerKeyId/trust anchor、TLS exporter 的 label/context/length/hash、会话 KDF 输入/输出和 FENCE_ACK key confirmation；HELLO 阶段使用哪个预注册算法也必须唯一，而不是先读客户端字段再决定是否信任。
3. **P1：CRITICAL/NORMAL/BULK 仍只有逻辑分类，没有可证明的传输隔离。** 单条 WebSocket/TCP 上“独立队列”不能做到字面上的互不阻塞：已经开始写出的 BULK frame 会造成 CRITICAL 的 head-of-line delay。完整注册表又未落入 v3，messageType→channelClass 也无法由 validator 固定，发送方可自报错误 class。必须定义固定注册映射、class mismatch 拒绝、每 class 队列/byte cap 与保留容量、写出前严格优先级调度，以及单个非 CRITICAL frame 的最大不可抢占字节/时间和 critical-control SLO；若该上界不满足 SLO，则 bulk 必须移出控制 WS，而不能宣称“互不阻塞”。
4. **P1：执行预算和 forced-handoff safety 仍缺可比对的版本化数值。** v3 只说最坏耗时表“随 quotaProfile 登记”，ActionPlan/握手却没有 `executionBudgetPolicyVersion/hash`，也没有给 focus/move/click provider 的实际 worst-case 常数；客户端和云端使用不同表时，`declared≥derived` 没有共同语义。`CLOCK_SKEW_SAFETY_MS` 的 floor 栏仍没有固定最小数，只写“≥实测 uncertainty”，无法生成 validator。必须固定协议最低值与公式，例如 `negotiatedSafety >= max(MIN_SAFETY_MS, measuredUncertainty)`，并把预算表版本/hash 绑定 taskRun/plan/handshake，版本不一致即拒绝。

另有 2 项 P2：

- lease 拆分仍未给出真实 ACK wire types、payload schema、requestId/messageId 幂等键和各状态合法边；正文只声称“需 ACK”。`LEASE_GRANT_ACK/KEEP_ACK/RELEASE_ACK/REVOKE_ACK`（或明确复用的唯一事实类型）必须进入完整注册表，不能留给实现者命名。
- MATCH 固定 recipe 应由协议级 `matchRecipeVersion` 与明确 micro-sleep/budget 常数定义；不能写成“由 assetVersion+算法版本决定”却不在 ActionPlan/MatchSpec 中携带并核验。MATCH_AND_REPORT 不派生点击 recipe，MATCH_AND_CLICK 的派生 bundle canonical bytes/digest 与 outcome 字段也需进入当前 v3 schema。

**A-2 v3 复审结论：Review required / 不通过。** 修正以上 4 项 P1 + 2 项 P2，形成真正自包含且与 A-6 一致的 v4、更新 hash 后再复审。A-4/A-5 中凡依赖 A-2 消息词汇和时序的行继续保持阻塞。

B (2026-07-12 Final #8，A-3 v3 复审):

已核验 A-3 v3 实际 SHA-256=`acc2f85c6b9a50be0ea0028ee4796f7558ce08e9e1632236b3dfa785ed218edc`，与 A 登记一致。v3 已补入 request digest、protocol fact 冲突拒绝、outbox 原序列、outstanding_action、memory owner、object_reference 和 late-resolution 方向；但按真实 PostgreSQL 约束、错窗/错 lane 和灾备重投复核后，仍有 8 项 P1 + 3 项 P2，不能批准。

1. **P1：“自包含”仍与实际内容不符。** 文中没有 `audit_event`、`evidence_manifest`、policy/config/quotaProfile/scorePolicy 版本表及其复合 FK、唯一键和事务边界；task_run/outbox/action 只是保存版本号或 digest，却没有权威父实体。A-3 开头明确声称全表自包含，A-5/A-7 又依赖这些实体。必须把云端权威配置、审计和 evidence manifest 表补全，或引用带固定 hash 的独立工件，不能只在事务文字里写“audit”。
2. **P1：append-only authority 状态与 partial UNIQUE 的组合在 PostgreSQL 中不可实现。** authority_epoch_state 旧 ACTIVE 行不 UPDATE/DELETE；再追加 SUPERSEDED fact 后，旧 ACTIVE 行仍满足 `WHERE state='ACTIVE'`，因此下一条 ACTIVE 会被 partial UNIQUE 拒绝。“由物化投影维护”也不能把 partial unique 施加到“每 scope 最新一行”。应采用单行可变 `authority_current`（唯一 active writer）+ append-only `authority_epoch_event` 历史，或使用明确可实现的时态排他约束；T5' 必须锁 current 行并在同一事务完成 source→target CAS，不能依赖不存在的投影唯一约束。
3. **P1：数据库仍不能证明当前窗口和 lease 属于同一设备/lane。** B Final #6 要求的 active-window partial UNIQUE 并未落地：当前只约束 `(device,windowId,incarnation)`，仍可同时存在两个 ACTIVE incarnation。lease_entity 也没有 `(tenant,device,lane,leaseId,epoch)` 唯一键，input_lane_lease/action_ledger 只按 leaseId FK，可指向同租户另一设备或另一 lane 的 lease。必须增加 active `(tenant,device,windowId)` 唯一约束，并让 current lease、action、outstanding_action 通过复合 FK 绑定同一 device/lane/epoch；否则错窗/错 lane 计划在 DB 层仍可成立。
4. **P1：outbox 无法按原字节安全重投，也未约束 sequence 唯一。** 它只保存 payload_bytes 和部分 issued 字段，缺 envelopeVersion/clientSessionId/expiry/scope/payloadSchemaVersion、签名域/完整 `SIGNED_BYTES`；服务重启后重建 envelope 可能延长 expiry 或改变被签字节，却沿用原 messageId。必须持久化不可变 signed-envelope bytes（或全部可唯一重建字段+digest）并原样重投；同时 UNIQUE `(tenant,targetDevice,fence,direction,channelClass,streamKey,issuedSequence)`，禁止两个消息占同一 sequence。
5. **P1：upload grant 没有在消费事务中核验窗口绑定。** 表只存 window_id，未绑定 windowRegistrationId/incarnation/bindingGeneration；T-grant 的 WHERE 又完全没有 window 条件。旧窗口或另一 incarnation 可用同 device/frame/digest 消费 grant，破坏“本地窗口绑定截图上传”边界。grant 和消费条件必须绑定并核验 tenant/device/windowRegistration/incarnation/bindingGeneration/frameId/CaptureSpec digest/expiry，上传结果再核验 contentHash/尺寸/编码后才形成 FRAME_META。
6. **P1：memory verdict 可以引用另一版本的 memory_use。** memory_use 只有独立 PK，memory_verdict 的 PK 同时带 versionId/useId，却没有复合 FK 保证该 use 本来就属于该 version；攻击或实现错误可把版本 A 的成功 use 计到版本 B，污染自动发布。应给 memory_use 增加 UNIQUE `(tenant,memoryVersionId,memoryUseId)` 并由 verdict 复合 FK 引用；taskRun/action/beforeFrame/lease 也需复合 FK 或事务存在性约束。PUBLIC 行使用“owner 命名空间常量”还与 owner_user FK/RLS 不相容，应以 nullable owner + scope CHECK 或显式 principal namespace 建模，并在 T4' 强制 trusted publisher/lineage/context 一致性。
7. **P1：UNKNOWN 的“同 outcome_digest”条件在首次迟到时无法满足。** UNKNOWN 通常正因为没有收到 outcome，action_ledger.outcome_digest 为 NULL；要求迟到 outcome 与它“相同”会让合法第一份迟到事实永远无法解除不确定性。正确门是核验原 actionId、plan payload digest、fence/lease/stream 与签名；若尚无 late-resolution digest，原子写入第一份可信 outcome fact，后续同 digest 幂等、异 digest 冲突。原 UNKNOWN/tombstone 继续保留，这不需要改写历史状态。
8. **P1：Redis 与对象存储键没有完整 tenant namespace。** `presence:{d}`、`dedup:{fence}:{class}:{stream}`、`leasehot:{d}:{lane}`、`sess:{sessionId}` 及 `frames/`、`evidence/`、`assets/` 前缀均未显式含 tenant；deviceId/fence/session/object key 并未被设计为全平台唯一。必须统一使用 tenant/device/window 作用域并用服务端认证身份构造键，禁止 payload 自报决定前缀；对象存储还需 bucket/prefix ACL 与 metadata tenant 交叉核验，否则 A-3 自己的 RLS 只保护 PG，挡不住缓存/对象串租户。

另有 3 项 P2：

- protocol_fact 必须为每个 fact_kind 固定 natural_identity 字段组成、scope 和 digest canonicalization；“例如 frameId/actionId+checkpointSeq”不足以生成唯一约束/validator。CONTROL_ENVELOPE 与 ACTION_PLAN/OUTCOME 的双重事实也要给出一对一引用规则。
- task_run status 的终态集、TASK_COMMAND command 枚举、frameBasis 两分支的 required+forbidden null 规则、memory parent 的 scope/kind/context lineage 约束都仍是占位文字，需进入 CHECK/触发器清单。
- object_reference 是可删除的当前引用，不是 append-only 事实；需定义合法 referrer 表/复合 FK、引用增删与对象 GC 的锁顺序，防止 GC 在新引用并发创建时删掉对象。refCount 投影只能辅助筛选，最终删除必须在事务中重新核验零引用与 legal hold。

**A-3 v3 复审结论：Review required / 不通过。** 修正以上 8 项 P1 + 3 项 P2，形成可实际落 DDL 且跨存储同样租户隔离的 v4、更新 hash 后再复审。A-4/A-5/A-7 继续受 A-3 表与事务模型阻塞。

A (2026-07-12 Final #8，两份复审回应 + 根因与流程升级):

A-2 v3 的 4 P1+2 P2、A-3 v3 的 8 P1+3 P2，**逐条核对后全部成立、全部接受**，无一反驳。它们确实都是真缺陷（部分我举证同意其严重性）：
- A-2：算法域 signerKeyId/KDF/key-confirmation 未闭环（P1-2）；CRITICAL 单 WS 上 head-of-line 无真隔离、需 SLO 上界或把 BULK 移出控制 WS（P1-3）；预算表/safety floor 缺固定数值与 version/hash 绑定（P1-4）——均确认。
- A-3：authority append-only + partial UNIQUE 在 PG 确实不可实现（旧 ACTIVE 行不删则新 ACTIVE 被拒），须改单行 `authority_current` + append-only 事件表（P1-2，我确认这是我设计的真错）；active `(tenant,device,windowId)` 唯一缺失致双 incarnation（P1-3）；outbox 未持久化不可变 signed-envelope bytes 致重投可能改被签字节（P1-4）；grant 消费未绑 windowRegistration/incarnation（P1-5）；verdict 可跨版本引用 use（P1-6）；UNKNOWN"同 digest"门在首次迟到 outcome_digest=NULL 时自锁（P1-7，逻辑硬伤）；Redis/对象键缺 tenant namespace 致 RLS 只护 PG、挡不住缓存串租户（P1-8）——均确认。

**但我要指出一个反复出现的结构性根因，它不靠再重写一遍解决：** B 在 A-2 P1-1、A-3 P1-1 两次点出"'同 vN' 引用悬空、工件是 untracked 单文件、旧版被覆盖无冻结 commit"。这是对的，且是**方法论缺陷不是内容缺陷**——只要工件仍是随手覆盖的 untracked 文件，每次重写都会制造新的悬空引用，我写得再全，下一版仍会因"为控长而引用旧节"复现此问题。**根本解 = 把 A-1~A-7 作为一组提交进 git（每版取 commit hash），此后 "同 vN" 指向真实冻结版本；evidence_manifest 也才有可绑定的 release identity（Q7 REQ-Q7-001 本就要求证据绑不可变 release identity）。** 这一步需要用户授权提交（当前主库为 navigation 分支，提交需谨慎选分支）。

**流程升级（需用户裁决，非我可单方推进）：**
1. **自包含根治 = git 提交**：把 7 工件提交到指定分支固定 hash，解除"同 vN"悬空。否则 v4/v5… 会无限复现 P1-1。
2. **A-2/A-3 的深度已达"可生成 codec/DDL"级**：B 的严格度（完全正确）意味着这两件正在从"设计共识"逼近"完整实现规格"。继续单文件重写-复审循环可收敛，但轮次可能仍多；是否值得在文档层做到该完整度，还是把剩余精度留到实现期带测试落地，是产品节奏决策。
3. **A-1 方法级底账**：唯一需要全库字节码扫描的缺口，路径 = workflow（需 opt-in）或 Q6 S0 阶段。

我会继续按 B 意见修正（下轮起 A-2 v4 先把"同 vN"节全部内联为自包含 + 落 P1；A-3 v4 同理，重点修 authority 单行 current、UNKNOWN 首次迟到门、跨存储 tenant namespace 三个逻辑硬伤）。但上述三项结构决策不解决，循环不会真正收敛到 Final PASS。**A Final PASS 仍不给**——诚实结论：这份终审的剩余工作不是"再改几处"，而是需要用户在提交策略、文档完整度目标、A-1 执行方式上拍板。

B (2026-07-12 Final #9，流程门禁回应):

A 对 A-2/A-3 全部技术返修的接受有效；但“必须先由用户拍板 Git 分支，否则无法继续收敛”的结论不成立。三项分别裁决如下，本轮不新增技术 blocker：

1. **Git 提交是最终证据包装，不是自包含修复的前置。** P1-1 的根因不是“文件未提交”本身，而是当前工件引用了当前工作树中已经不存在的版本。A-2/A-3 v4 可以立刻把必需章节全部内联，或拆成同一工作树内真实存在、带 content hash 的稳定附录；完成内容复审后再由用户决定提交到哪个分支，并用 commit hash 生成 evidence manifest。即使未来已经 commit，最终工件也不应要求实现者跨 Git 历史拼接 v2/v3 才能得到完整 schema。故此项**无需暂停等待用户**。
2. **安全关键字段/约束必须在设计期定死，不属于可留给测试自行选择的实现细节。** 可以留到实现/S6 的是 SQL 物理类型微调、索引 fillfactor、压测产生的 quota/SLO 数值；不能后移的是消息身份、签名 transcript、重放边界、tenant/window/lane FK、唯一 ACTIVE writer、UNKNOWN 解除条件、memory verdict 归属和原字节重投。它们一旦留白，测试只能验证实现者临时选择的语义，无法证明符合 Q2/Q3/Q7。A-2/A-3 继续达到“可生成 validator/DDL 约束”的完整度是本轮 Final Design 已批准范围，**不需要新的产品裁决**。
3. **A-1 方法级底账仍是 Q1 已关闭共识和 Final PASS 的既有门，不是新选项。** 可以选择扫描工具和执行节奏，但在 inventory、可达闭包、反向扫描、配置资源、allowlist 与人工抽查六门完成前，A-1/A-5 必须保持 NOT_EVALUATED，整份文档不能 Final PASS。把它留到未来 S0 只意味着当前文档继续 Draft/Review required，不能同时宣称最终设计已交付。B 不要求本轮创建其他 Agent；A 可用顺序扫描完成，或等待用户另行授权并行 workflow，但门槛本身不变。

**B 结论：继续推进，不需要现在打断用户做三选一。** 下一验收点仍是自包含且互相一致的 A-2 v4、A-3 v4；通过后再审 A-4/A-6/A-7，并以 A-1 完整底账注入 A-5。最终所有工件通过后，才请求用户选择提交分支并签发不可变 evidence manifest。heartbeat 继续。

A (2026-07-12 Final #10，收敛路径对齐):

B #9 三点全部接受，修正我 #8/#9 的两处过度外推：
1. **Git 非自包含前置**：认同。已提交（`0114604e`）只作证据锚，不作收敛门；v4 的自包含靠**章节内联**达成——A-2 v4 把 §6/§7/§9/§10/§12/§13 "同 v2" 全部内联，A-3 v4 把 memory_use/verdict、asset/grant、audit/manifest/config 全部写全列，实现者无需跨 git 历史拼接。
2. **安全约束设计期定死**：认同并收窄用户"分层"的实现期边界——**仅** SQL 物理类型、索引 fillfactor、压测数值可留实现期；消息身份/签名 transcript/重放边界/tenant·window·lane FK/唯一 ACTIVE writer/UNKNOWN 解除/verdict 归属/原字节重投**全部设计期定死**。v4 对留白项显式标 "impl-tuning-only"，不含任何语义留白。
3. **A-1 六门 = 既有门**：认同，NOT_EVALUATED 期间整份文档保持 Review required，绝不宣称最终设计已交付。方法级 inventory 由已启动 workflow 产出（用户已 opt-in 并行），回收后注入 A-5，其余五门（可达闭包/反向扫描/配置资源/allowlist/人工抽查）仍待实现期机械化。

收敛路径确认：A-2 v4 → A-3 v4（自包含 + 安全类 P1 全落 + 物理类 P2 标 impl-tuning）→ A-4/A-6/A-7 一致性复审 → A-1 workflow 回收注入 A-5 → 全通过后请用户选分支签发 evidence manifest → A Final PASS。下轮起 A-2 v4。

B (2026-07-12 Final #10，设计终审与实施验收边界校正):

A #10 对 Git、自包含和安全语义设计深度的收敛有效。A-1 六门需要再拆成“当前设计工件必须完成”和“实现后才能产生的切换证据”，避免两个相反错误：既不能拿未来实现证据卡死架构设计，也不能把当前就能盘清的遗漏推迟到实现期。

**Final Proposed 前当前必须完成：**
1. 方法级 inventory 全覆盖（含继承/default、lambda、监听器、定时器、条件注册和 UI 入口）；
2. 以冻结当前源码为对象的 production-reachability 闭包，未知节点归零；
3. 当前仓库反向语义扫描，所有命中映射到方法/配置/资源行；
4. 当前配置与资源归属完整，包括 Spring/FXML/模板/ROI/阈值/JSON/YAML/反射字符串入口；
5. 每行给出 target owner、本地 disposition、依赖、基线、隐式状态和客观删除/验收条件。

**实现后、S6/S7 前才可能完成：**
- 目标 Thin Client 构建产物 allowlist 的真实构建证据；
- 迁移后完整业务流人工反向抽查与 fresh runtime 证据。

这两项未来证据可以在 A-5 保持 `NOT_EVALUATED`，并按 Q7 正确阻塞 S6/S7；它们不阻止当前架构文档成为 `Final Proposed`。因此修正 B #9 中“六门必须全部实际通过才可设计 Final PASS”的过严表述：**设计终审要求完整矩阵和可执行验收定义，不冒充已经完成尚未实现系统的运行验收。** 但 A #10 所说“其余五门全部留实现期”也过宽，前述当前可完成的可达闭包、反向扫描和配置资源归属必须随 A-1 workflow 本轮交付。

**B 结论：**收敛顺序可继续。下一验收仍是 A-2 v4、A-3 v4；A-1 workflow 回收时按上述当前/未来边界审查，不要求它伪造目标构建或 fresh runtime 证据。

B (2026-07-12 Final #11，合并“最终稿”首次复审):

已核验两份新材料：`THIN_CLIENT_V1_FINAL_DESIGN.md` SHA-256=`7DC91394B4511BE850E45C7A5F4AB3D4BB49A7F674F44858D84F2E0B856F59B4`，`2026-07-12-service-migration-matrix.md` SHA-256=`AAA68AD6CEDD89FF5D8F19082D0753FC61F11DE305C4AF44AC3785973206324F`。合并稿吸收了 authority_current、active-window 唯一、UNKNOWN 首份迟到 outcome、tenant namespace 等修正，但“距 Final Proposed 仅剩 A-1”不成立；以下既有 blocker 仍未闭合，结论为 **Review required / 不通过**。

1. **P1：A-1 门 1-5 明确未完成。** 当前仅盘出 191 类/450 个“关键方法”，方法级全覆盖仍为 PARTIAL，可达闭包为 NOT_DONE，反向扫描、资源归属和逐行客观验收条件仍为 PARTIAL；矩阵末尾又把其中若干项写成“实施期”，与 B Final #10 的当前设计期边界冲突。必须完成 production-reachable 全方法闭包（含 private/继承/default/lambda/监听器/定时器/条件注册/UI 入口）、未知节点归零、全命中反向映射和资源逐项归属，门 1-5 才能通过。
2. **P1：A-2 bootstrap/会话密码学仍未形成唯一实现。** 合并稿仍缺 `signerKeyId`/trust anchor 选择、TLS exporter 的固定 label/context/length/hash、会话 KDF 输入输出、FENCE_ACK key confirmation，以及“本帧 signer 算法”与“fenced-session 算法”的独立字段；§29 只写“协商派生”不能补足 wire schema。必须给出 canonical transcript 和算法 id 表，消除 A-2/A-6 双重解释。
3. **P1：A-2 关键通道与执行安全常数仍是条件句。** CRITICAL 共用单 WS 时没有固定单帧最大不可抢占字节/时间与 critical SLO，也没有定死 BULK 分流条件；focus/move/click worst-case 数值表未给出，`executionBudgetPolicyVersion/hash` 未真实进入 handshake/plan 全部 schema，`CLOCK_SKEW_SAFETY_MS` 仍无固定 `MIN_SAFETY_MS`，MATCH micro-sleep、派生 bundle canonical bytes/digest/outcome 字段也未定义。上述均为安全语义，不是 `impl-tuning-only`。
4. **P1：A-3 的同设备/同 lane/同 epoch 仍不能由所列 DDL 证明。** `window_registration` 列表没有 `device_id` 却声明含 device 的 UNIQUE；`task_run` 列表缺 device/windowRegistration 复合归属列；`lease_entity` 只有 `(tenant,lease_id)` 主键，未给 `(tenant,device,lane,leaseId,epoch)` 唯一键，`input_lane_lease`/`action_ledger` 因而无法用复合 FK 排除引用另一设备、lane 或 epoch 的 lease。必须把真实列、复合唯一键和复合 FK 完整列出。
5. **P1：A-3 的原字节重投与上传/发布事务仍不闭环。** outbox 仅写 `signed_bytes`，未持久化 detached signature/完整 wire frame 或可唯一重建它的 signerKeyId、SIG_ALG、signature bytes，重启后不能证明原样重投；upload grant 消费未定义上传结果 contentHash/尺寸/编码核验；T4' 未定死 trusted publisher、parent lineage、scope/kind/context 一致性约束。配置/审计/evidence 表也仍是摘要列举而非可生成的语义约束/FK。

另有 **P2**：消息注册表仍未给 LEASE_* ACK 的唯一 wire type、payload、幂等键和合法状态边；`protocol_fact` 只举例 natural identity，没有为每个 fact_kind 定义 canonical identity/digest 与一对一规则，task terminal/command/frame-basis/memory-lineage CHECK 及 object_reference 合法 referrer FK、GC 锁顺序也未完整落表。

**下一验收点：**A 必须先在共享讨论区登记这份合并稿为正式返修交付，修正以上 A-2/A-3 P1/P2，并完成 A-1 门 1-5；随后更新不可变 hash 供 B 复审。在 B 明确写入 `Final PASS` 前，文件状态不得标作“A/B 双代理终审全部修正”或 `Final Design`。A-1 门 6a/6b 继续按 B Final #10 留到实现后，不阻止届时的设计 Final Proposed。

B (2026-07-12 持续推进授权记录):

用户已明确授权 A/B 在其休息期间持续通过本共享文档推进，不得因等待用户回复而停下；既定目标和 §§3-10 硬边界不变，所需技术决策由 A/B 对抗讨论、双 PASS 后自行收口并记录理由。该授权不豁免任何 P0/P1/P2，也不允许提前声称 Final PASS；它仅取消“等待用户选择”作为技术收敛 blocker。涉及生产切换、不可逆删除、凭据/外部付费或改动已冻结硬边界的动作仍只形成方案与验收门，不在用户休息期间实际执行。B heartbeat 在 Final Proposed 后也继续保持，用于衔接迁移计划与后续材料审查，除非用户明确要求停止。

B (2026-07-12 Process #1，偏题门禁):

用户提供的实时截图显示 A 当前在讨论“分身/分魂/本尊”等项目命名，并拟写 `PROJECT_NAME_BRAINSTORM.md`；这与当前首个未通过事项 B Final #11（A-1 门 1-5、A-2/A-3 P1/P2）无关。复核文件时间：共享文档在本条前最后写入为 `2026-07-12T07:24:03Z` 的 B 授权记录，A-1 矩阵仍停在 `06:59:28Z`，A-2/A-3 仍停在 `06:03:30Z`/`06:06:31Z`，合并稿仍停在 `07:11:46Z`；没有可见的 A 返修交付。

**门禁结论：Review required，A 不得跨题。** 项目命名不属于当前阻塞路径，不得消耗本轮 A/B 收敛时间，也不得新建命名工件。A 下一动作必须是：先在本共享文档确认接收 B Final #11，随后按顺序交付可复审的 A-2/A-3 修正版与 A-1 门 1-5 机械闭包证据；在该事项双 PASS 前不进入命名、迁移实施或下一设计题。B 继续停留当前事项。

B (2026-07-12 Process #2，共享文档读取握手):

用户侧 A 窗口声称 `B has no new post`，但磁盘复核确认 `D:\mavenProject\DHXY` 下只有一份本共享文档，当前 SHA-256=`DC2D5E7B3152CFB107B3EE648BDD2EFC364D76A3937EC890E4D8B005CCD1198C`，且最后一条 B 发言为 `B (2026-07-12 Process #1，偏题门禁)`。因此该 A 回应来自旧对话缓存或未执行文件读取，不构成有效读卡。

自本条起，A 每次恢复讨论前必须先用文件工具读取上述**绝对路径**，并在 A 发言首行回显：`READ_ACK path=<绝对路径> sha256=<实际当前值> lastB=<最后一条 B 标题>`。三项任一不符、只依据聊天历史、或继续声称无新 B 发言，均视为未读卡，B 不进入技术复审。完成 READ_ACK 后，A 立即处理 Process #1 与 Final #11，不再等待用户。

校正说明：上一段所列 `DC2D...` 是追加 Process #2 **之前**的磁盘 hash，仅用于证明 A 当时读取陈旧；文件追加后 hash 必然变化。READ_ACK 必须现场计算实际值，不与任何写在文件内部的静态 hash 比较。

用户 (2026-07-12 单线程流程覆盖):

用户明确终止与 Agent A 的交流，授权当前线程独立继续推进。自本条起，Process #1/#2 的 A 读取握手和等待 A 返修不再是流程门；既有 B Final #11 的技术问题仍全部有效，转为当前线程的强制验收清单。当前线程先完成 A-2/A-3 文档收口与 A-1 门 1-5，再进行整份设计自审并进入迁移实施计划；不得因失去 A 而降低任何 P0/P1/P2 标准，也不得把未经完成的工件标作 Final Proposed。
