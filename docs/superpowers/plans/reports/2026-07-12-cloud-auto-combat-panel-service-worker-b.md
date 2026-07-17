# External Worker B：`AutoCombatPanelService` Cloud lift-and-shift

## Parent Task Brief #1 - 2026-07-12

### 目标

整类设计 DHXY HEAD `0114604e` 的 `AutoCombatPanelService` 迁入 Cloud，作为 AutoCombat W1 独立 collaborator。
保持 Alt+8 打开、面板识别/拖拽/安全区、回合数读取、刷新与全部顺序/坐标/阈值/等待/日志语义；截图和物理输入只经
retained typed Service port，图像处理只经 Cloud-native canonical owner，模板只经已批准 `CloudTemplateAssets`。首轮只
向本文件追加 `External Worker B - Design #1`；父级 `DESIGN APPROVED` 前零 Java/Maven/resources/tests。

### 必读与保护

- 完整读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、`docs/ACTIVE_WORK.md`
  顶部 CR271、迁移矩阵、A AutoCombat 固定日志和刚完成的 artifact/template 固定日志。
- 只读核对 DHXY HEAD `AutoCombatPanelService.java` 全类、全部 main caller、直接/间接 capture/template/image processor/
  input/config/timer/GameContext 依赖；当前 dirty 不是业务基线。
- 读取 Cloud 当前 `ImageProcessorService`/`CloudNativeImageProcessor`、`CloudTemplateAssets`、artifact API、
  `CloudTaskServicePort`、retained action state/ledger、H State owner、I properties、J/K/L 最新批准边界。
- 保护 A broker repair、K/M remote 设计、J turn、L BattleRadar、其它 dirty/untracked；不修改其它 Worker 日志。

### 设计不变量

1. 全部 public/private 方法、caller、返回/异常、GameContext 副作用、Alt+8/open/scan/drag/refresh 顺序、sleep、坐标、ROI、
   threshold、OCR/round parsing、fallback 和日志逐项等价；不新增 capture/verification/retry/TTL/park/yield/cleanup。
2. Cloud 不搬 tracker/WindowRuntime/HWND/Robot/JNA/InputSequences/queue/TaskPauseToken/local Path/temp/线程/poller；input
   必须为 retained typed atomic bundle，move+drag/click 不可拆帧或跨 bundle。
3. 每个 capture/input 使用 exact current TaskExecutionContext + stable semantic address/occurrence；UNKNOWN 同字节重投，
   final 未被业务消费不换 ID。不得开放 raw request/poll/outcome。
4. 图像洗色/计数/红字轮数只用 canonical `ImageProcessorService`；template 只用 approved `CloudTemplateAssets`，transient
   image/artifact ownership 与 flush 明确，无第二 loader/Path/OCR fallback。
5. `AutoCombatPanelService` 为 per-taskRun、跨 revision 保留的 dormant collaborator；不在本切片接 host/caller，不与 A
   `AutoCombatService` 主类并发写。生产只整批原子切换。

### Design #1 必交付

- HEAD 全方法/caller/状态/config/timer/截图/输入/模板/图像处理矩阵，标出 AutoCombat 实际 API。
- public API compatibility 与 explicit-context 适配；每个 capture/input 的 typed semantic address、原子 bundle、timeout、
  UNKNOWN/final、同字节重投与 retirement 依赖。
- panel locate/drag/round OCR 的同帧/同图/坐标空间/threshold/fallback 精确映射；artifact/template/image owner 释放边界。
- exact Cloud dependency DAG、constructor/factory/bean/caller 与文件写集；与 A/K/M/J/L 零交叉或严格顺序证明。
- pause/resume/stale/stop、错窗/乱序/断线、tenant、容量、内存、restart、运维、dormant 风险矩阵。
- 最小实施波次、P0/P1/P2、自审，以及 Cloud `mvn -q clean package` 门。自审不算批准。

### Worker 规则

- 只 append Design #1；父级批准前不改 Java/Maven/resources/tests，不运行 build，不启动任何运行面。
- 不创建 agent/reviewer，不回滚/覆盖/提交；完成后停止等待父级审查，BLOCKED 由同一 B 返修。

**无已批准业务差异；按基线等价迁移。**

## Parent Resume Directive #1 - Worker B CURRENT TASK - 2026-07-13

**Worker B 当前不是空闲状态。立即执行本文件 `Parent Task Brief #1` 的 `AutoCombatPanelService Design #1`。**

1. 只追加 `External Worker B - Design #1`，完整交付 brief 要求的 API/caller/state/authority/mechanical port/DAG/risk 矩阵；
   不得修改历史内容。
2. 当前阶段是 design-only；父级写入明确 `DESIGN APPROVED` 前，禁止修改 Java/Maven/resources/tests，禁止启动任何运行面。
3. 每 5 分钟重新完整读取本文件。若父级追加 `BLOCKED`，由同一 B 继续 Design Repair；若无新父级材料，继续完成当前
   Design，不得因聊天窗口无消息、用户离开或其它 Worker 尚未完成而停止。
4. Design 完成后把完整证据追加到本文件并等待父级审查；不得自称 Approved，不得转去其它任务。
5. 保护两仓全部 dirty/untracked，不回滚、不覆盖、不提交。**无已批准业务差异；按基线等价迁移。**

## External Worker B - Design #1 - 2026-07-13

纯设计，未改任何 Java/Maven/resources/tests、未 build、未启动运行面。基线 = DHXY HEAD `0114604e`
`AutoCombatPanelService.java`（全类 363 行已亲读）+ 其唯一 caller `AutoCombatService.java` + 已批准的 Cloud
`CloudTaskServicePort`/`CloudTaskServiceExecutionContext`（Worker A）/`CloudTemplateAssets`/`CloudArtifactStore`（本 B，
CR271 已 APPROVED）/`ImageProcessorService`。`无已批准业务差异；按基线等价迁移。` 自审仅 QA。

### 1. HEAD 全方法 / caller / 状态 / config / 截图 / 输入 / 模板 / 图像处理矩阵

**public/静态 API（逐项等价保留）**
| 方法 | 签名 | 副作用 | caller（`AutoCombatService`） |
|---|---|---|---|
| `verifyAndAlignPanel()` | ()→void | 委托 `VERIFY_AND_REFRESH` | （便捷重载） |
| `verifyAndAlignPanel(PanelVerifyMode)` | (mode)→boolean | 定位+对齐+(可选)刷新 | L1024 `ENTRY_MAINTENANCE`、L1084 `VERIFY_AND_REFRESH` |
| `ensurePanelVisible(String,int)` | (source,waitMs)→Point | 定位（Alt+8 兜底）返回面板中心 | L613 `combat-enter,500` |
| `recordCombatExit()` | ()→void | 回合估算 -3 | L631 |
| `resolveRoundsRefreshReason(int,long,long,long)` | static→RoundsRefreshReason | **纯函数、零副作用** | L463/L1001 |
| 嵌套 `PanelVerifyMode`/`RoundsRefreshReason`/`RefreshDueBurstDecision`/`TeamRefreshDueBurstGuard` | — | `TeamRefreshDueBurstGuard` 为纯内存 team-key 去抖 | 枚举/guard 引用 |

**依赖（构造）与迁移归属**
| 本地依赖 | 用途 | Cloud 归属 |
|---|---|---|
| `GameClientTracker` | `updateGlobalVision`/`getLatestVisionPath`/`getLastCaptureAudit`、`getWindowBaseX/Y` | **不搬**；截图经 `CloudTaskServicePort.capture(...)`、窗口基点经 `readWindowFact(...)` |
| `CoordinateHelper.findImageAbsoluteCoordinateByImagePath(tpl,path,0.80)` | 面板模板匹配 | canonical 图像 owner `ImageFinder`/`ImageProcessorService` + 模板经 `CloudTemplateAssets` |
| `InputSequences.submitAndWait` | Alt+8 / dragAndDrop / sleep | **不搬**；`CloudTaskServicePort.executeInputBundle(...)` 原子 bundle |
| `WindowTaskContextHolder` | windowId、`markRuntimeWarning` | 由 exact `CloudTaskServiceExecutionContext` 提供身份；warning 走 H/观测通道 |
| `AutomationMetricsService.recordWindowWarning` | 面板缺失告警指标 | 迁移矩阵告警 owner（缺失-attention 逻辑逐项保留） |
| `GameContext` 回合估算 4 方法 | `get/setAutoCombatEstimatedRounds`、`get/setLastAutoCombatRefreshAt` | **H State owner**（per-taskRun 跨 revision 状态） |
| `BotProperties.getAutoBattleRefreshIntervalMs` | 刷新间隔 | **I properties** owner |

**关键基线事实（防误迁）**：回合数**不是 OCR**——`auto_remaining.png` 仅用于**面板定位**（匹配点 +偏移(43,28)=面板中心）；
回合是 `GameContext` 里的**估算计数器**（退战 `recordCombatExit` -3、刷新 `recordAutoCombatRefresh` 重置 25）。刷新动作
=按 Alt+8（无读数）。设计**不新增任何 OCR/round parsing**。常量逐字保留：面板落点偏移(489,726)、安全区阈值 dist>20、
对齐后 sleep500、Alt+8 后 wait(入参/1000)、刷新 wait1000、缺失-attention 10min/repeat60s、team 去抖 30s、模板阈值 0.80、
默认 25、低回合阈值 10、每战 -3。

### 2. public API 兼容 + explicit-context 适配（每 capture/input 的 typed 语义地址 / 原子 bundle / timeout / UNKNOWN-final / 同字节重投 / retirement）

- **Cloud 类** `com.yueyunfe.dhxy.cloudbrain.service.AutoCombatPanelService`（新），**per-taskRun collaborator**。纯函数
  `resolveRoundsRefreshReason` + 嵌套枚举/record/`TeamRefreshDueBurstGuard` **逐字保留**（无 I/O）。每个有 I/O 的 public
  方法**新增首参 `CloudTaskServiceExecutionContext ctx`**（explicit-context，与 CR271 adapter 同款；I/O 前 `ctx.revalidate().allowed()`
  ACTIVE + 身份门，失败即 fail-closed 返回基线"未命中/未刷新"等价值）。本地 per-windowId `runtimeStates` map → 云端
  per-taskRun 单实例状态（缺失-streak、panelAligned）。
- **截图**：`findAutoCombatBox` 的 `updateGlobalVision+getLatestVisionPath` → `ctx.servicePort().capture(captureAction,
  CaptureRegion=当前客户端全域, PNG, CapturePurpose=panel-locate, timeoutMs)`。语义地址（occurrence-stable）：
  `auto-combat-panel:locate`。UNKNOWN → 同 `RetainedActionIdentity` 同字节重投；final 未被业务消费不换 id。
- **窗口基点**：安全区判定与落点(489/726)需 `getWindowBaseX/Y` → `ctx.servicePort().readWindowFact(windowFactAction,
  WindowFactKind=WINDOW_BASE_POINT, timeoutMs)`（语义地址 `auto-combat-panel:window-base`）。
- **输入 bundle（原子、move+drag 不拆帧不跨 bundle）**：
  - 开面板：`executeInputBundle(inputBundleAction,"battle:openAutoPanel:"+source, coordSpace, [pressAlt8, sleep(waitMs)], t)`；
    语义地址 `auto-combat-panel:open:<source>`。
  - 对齐拖拽：`[dragAndDrop(from,to), sleep(500)]` **单 bundle**（from=面板中心、to=安全区落点）；地址 `auto-combat-panel:drag`。
  - 刷新：`[pressAlt8, sleep(1000)]`；地址 `auto-combat-panel:refresh:<source>:<reason>`。
  每 bundle timeout 由既有配置/默认承载，无新增等待；facade 不重排 action 列表（`CloudTaskServicePort` 契约）。
- **重投/退休**：capture/input 用 exact `ctx` + 稳定语义地址 + occurrence；UNKNOWN 同字节重投；final 未被业务消费不换 ID；
  不新增 retry/TTL/park/yield/cleanup（invariant 1）。action handle 由 retained Task state 产出，本类不 mint。

### 3. panel locate / drag / refresh 的同帧-同图-坐标空间-threshold-fallback 精确映射 + owner 释放边界

- **同图定位**：一次 `capture` 得一帧 → 该帧上 `ImageFinder` 匹配 `auto_remaining.png`（阈值 **0.80** 逐字）；命中点
  +(43,28)=`panelCenter`（同基线）。未命中 → 返回 null（同基线 warn 日志逐字）。**同帧**：定位只用这一帧，不新增二次截图。
- **对齐 fallback**：拖拽后 `findAutoCombatBox` 复定位；复定位失败 → `AutoCombatPanelMatch(new Point(dropX,dropY),null,0,
  "drag-target-fallback")`（基线同款兜底，坐标空间=屏幕绝对/客户端基点+偏移，与 `readWindowFact` 基点一致）。
- **坐标空间**：所有落点/拖拽/匹配点统一 `CoordinateSpace`（客户端基点相对），bundle 内 from/to 同空间，杜绝跨空间漂移。
- **owner 释放**：`capture` 返回的 transient 图像 bytes/`BufferedImage` 由本类**即用即 flush**（匹配后释放，无 use-after-flush、
  无静默共享）；**不写 artifact**（基线无中间文件持久化）——仅当未来需 debug 才 `CloudArtifactStore.writePng(ctx,...)`，本切片
  **不启用**。模板经 `CloudTemplateAssets.loadTemplate(new TemplateId("images/template/battle/auto_remaining.png"))`（已批准
  allowlist，含该路径）；模板图 immutable、由 assets owner 持有，本类只读不 flush。图像洗色/计数**不涉及**（本类无洗色，纯模板匹配）。

### 4. Cloud dependency DAG + 写集 + 与 A/K/M/J/L 零交叉证明

**DAG（本类 → 依赖）**：`AutoCombatPanelService(cloud)` →
`CloudTaskServiceExecutionContext`（A：`servicePort()` 供 capture/input/windowFact、`revalidate()`/`scope()`/`taskRunId()`/
`runRevision()`）、`ImageFinder`/`ImageProcessorService`（canonical 图像 owner）、`CloudTemplateAssets`（B 已批准）、
H `GameContext`-state owner（回合估算读写）、I `BotProperties` owner（refreshIntervalMs）、告警 owner（缺失-attention）。
`CloudArtifactStore`（B）仅备用 debug，本切片不接。

**写集（父级批准前不落）**：预计 **Cloud 1 new**：`service/AutoCombatPanelService.java`（+ 若 DTO 映射必要，同包
private helper，不新增 public 类）。**DHXY 0**。**dormant**：不接 host/caller，不改 A `AutoCombatService`，`CloudServiceConfiguration`
是否加 dormant bean 待父级定（可延后到 caller 激活切片）。

**零交叉/严格顺序**：本类为**新增 collaborator**，不修改 `remote/**`、`api/RemoteTaskRunEndpoint.java`、`RemoteTaskRunErrorCode`、
`CloudTaskRunExecutionGate`、DHXY `cloud/remote/**`（A/K/M）、J turn、L BattleRadar 任何文件——仅**只读消费**其已批准 public
类型（`CloudTaskServicePort`/`CloudTaskServiceExecutionContext`）。对 H/I owner 为**只读依赖其已批准读写 API**，不改其文件。
严格顺序：本类实现依赖 H State owner + I properties + A retained port 的已批准 API 冻结；若任一尚未 APPROVED，实施
排在其后（顺序依赖，非文件交叉）。与 A 主类 `AutoCombatService` **不并发写**（本类只读状态 + 经 retained port 串行 input）。

### 5. 风险矩阵

| 维度 | 处置 |
|---|---|
| pause/resume/stale | 每 capture/input 前 `ctx.revalidate().allowed()` ACTIVE + `(taskRunId,runRevision)` 身份门；stale/非 ACTIVE → fail-closed 返回基线等价"未命中/未刷新"，不发输入 |
| 错窗/乱序/断线 | action handle 绑 exact ctx 身份；乱序/错窗由 retained port 拒绝；断线 → outcome 非成功 → 基线等价失败路径（warn+返回，不重试超基线） |
| UNKNOWN/final | UNKNOWN 同字节重投（同 RetainedActionIdentity）；final 未被业务消费不换 ID（invariant 3） |
| tenant | ctx.scope() 决定租户；本类无跨租户读写 |
| 容量/内存 | transient 图像即用即 flush；不写 artifact；per-taskRun 状态 O(1)；无线程/timer/poller |
| restart | 回合估算/lastRefreshAt 归 H State owner，其重启语义由 H 契约承载；本类无自持久化 |
| 运维/dormant | 本切片 dormant，不接 caller/host，不启动运行面；缺失-attention 告警经 owner 通道，逐项保留 10min/repeat60s 阈值与日志 |
| 并发 | 与 A 主类不并发写；input 经 retained port 串行原子 bundle |

### 6. 最小实施波次 / P 项 / 自审 / 构建门

- **波次**（仅 `DESIGN APPROVED` 后）：W1 落 `service/AutoCombatPanelService.java`（纯函数+枚举逐字 + I/O 方法改 explicit-ctx
  + capture/windowFact/input bundle 映射 + 回合状态经 H owner + 模板经 CloudTemplateAssets）；dormant，不接 caller。→ Cloud
  `mvn -q clean package`（不 skip）→ 追加 `Implementation #1`（diff/SHA-256/JAR SHA/javap/Surefire/零启动/零 Git mutation）。
- **未决 P 项**：`P0=0`；`P1=2`——(P1-a) H State owner 的回合估算读写 exact API 与 per-taskRun/revision 语义需在 H
  APPROVED 后对齐（决定 `recordCombatExit`/`recordAutoCombatRefresh`/健康判定的状态读写落点）；(P1-b) `readWindowFact` 是否已
  暴露 `WINDOW_BASE_POINT` 类 fact 与 `CoordinateSpace`/`CaptureRegion` 客户端全域表达，需按 A 最新批准 DTO 对齐（决定安全区
  判定与落点坐标空间）。`P2=1`——缺失-attention 告警在云端的落点通道（metrics/observation owner）需矩阵确认，逐项保留阈值与
  日志语义。以上均为**依赖 API 对齐**，非本类算法变更。
- **自审**：纯函数/枚举/record/guard 与基线字节等价；所有常量/顺序/阈值/sleep/坐标逐项保留；无新增 capture/verify/retry/TTL/
  park/yield/cleanup；截图/输入只经 retained typed port，图像只经 canonical owner，模板只经已批准 assets，无第二 loader/Path/OCR。
  **自审仅 QA，不算批准。**

`无已批准业务差异；按基线等价迁移。` Design #1 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`。

## Parent Design Review #1 - BLOCKED / W-ACP-0 IMPLEMENTATION APPROVED - 2026-07-13

父级已把 Design #1 与 DHXY HEAD `0114604e`、当前 `CloudTaskServiceExecutionContext`/`CloudTaskServicePort`、
H/K runtime、v1 geometry/capture/input DTO、`ImageFinder` 和 packaged template owner 逐项对照。业务方法与常量盘点可保留，
但主体设计当前 **BLOCKED，P0=0，P1=5，P2=1**。为避免 B 继续纯设计等待，本轮另批准不依赖下述边界的代码切片
`W-ACP-0`。

### P1-1：方法签名没有 retained action handle，单个 Service 新文件无法实现所述稳定身份

- 证据：L104 只给 I/O 方法新增 `CloudTaskServiceExecutionContext`；L109-L118 又声称由 retained Task state 提供 handle，
  但 `CloudTaskServicePort` 每次调用强制接收不可构造的 `WindowFactAction/CaptureAction/InputBundleAction`，
  `CloudTaskRetainedActionState` 及 `ActionAddress` 均 package-private。L141 写集只有
  `service/AutoCombatPanelService.java`，而 `source/reason` 还是 caller raw string。
- 影响：按当前写集无法编译；若 Service 用 `source` 动态铸造地址，会在 UNKNOWN/重入时换 ID，并形成无界 semantic slot。
- 返修条件：逐 workflow attempt 列出 caller-owned persisted phase/slot/occurrence；I/O API 必须接收已由 trusted runtime
  保存的 opaque handle bundle，或明确增加 remote-package retained adapter 写集。raw `source/reason` 只能用于日志，不能参与
  identity；begin/reenter/final-consume/retire 线性化点必须明确。

### P1-2：UNKNOWN/stale 被压成未命中、输入失败或 drag fallback

- 证据：L104 与风险矩阵 L153-L157 把 non-ACTIVE/断线 outcome 映射为基线“未命中/未刷新”；但 HEAD 的 null/false 是
  本地同步 capture/input 已得到确定失败后的业务分支。Cloud `CaptureOutcome` 非 OBSERVED 不携带图像，input UNKNOWN 还可能
  已开始部分步骤。
- 影响：UNKNOWN 会错误启动 Alt+8、missing-streak/10 分钟告警、drag-target-fallback，或把回合状态当作未刷新；再次进入还会
  生成新动作身份。
- 返修条件：给 locate/open/drag/refresh 分别定义 typed `OBSERVED_MATCH/OBSERVED_MISS/EXECUTED/NOT_EXECUTED/
  UNRESOLVED/STOPPED` 兼容表。UNRESOLVED 不返回 null/false、不更新 missing streak/round state、不启用 fallback、不 mint
  新 identity；只保留同 bytes outcome resolution 或 typed unwind。

### P1-3：设计引用不存在的 `WINDOW_BASE_POINT`，且 v1 输入只接受屏幕绝对坐标

- 证据：L111 的 `WindowFactKind.WINDOW_BASE_POINT` 不存在；当前枚举只有 `BINDING/GEOMETRY/FOCUS_STATE/STOP_STATE`。
  `WindowFact.GeometryFact` 已给 `(x,y,width,height,SCREEN_ABSOLUTE_PX)`，而 `InputBundleRequest` L17-L18 强制
  `SCREEN_ABSOLUTE_PX`。Design L127 却写“客户端基点相对”。
- 影响：实现不能编译，或会把 image-local/client-relative 点送进 screen-absolute input，产生错窗点击/拖拽。
- 返修条件：固定 `GEOMETRY -> GeometryFact`，验证 `observedWindow` exact；用 `(x,y,1024,768)` 复现 HEAD
  `updateGlobalVision` 固定视野并声明边界检查。所有 input 为 `SCREEN_ABSOLUTE_PX`；不得新增 WINDOW_BASE DTO。

### P1-4：遗漏 HEAD 的 DPI scale 变换，直接加 origin 会改变坐标

- 证据：HEAD `CoordinateHelper.findImageAbsoluteCoordinateByImagePath` L153-L160 计算
  `round(matchX/systemScaleRatio)+windowBaseX`；Design L123-L127 只写 match 点加 offset/origin，且 Cloud 尚无该 scale fact/config
  authority。
- 影响：Windows 非 100% 缩放时，面板中心、20px 安全区判断和 drag 起点都会偏移，属于输入语义变化。
- 返修条件：明确 scale 的唯一可信来源及 typed 传输/配置 owner，逐式保留 HEAD 的除法和 rounding；在该前置不存在前主体
  Java 冻结。不能偷偷假设 1.0。

### P1-5：H State 与 warning owner 不是当前 Service 可调用依赖

- 证据：`CloudGameContextStateOwner` 是 remote package-private，State 只能由 assembly 持 activation handle 并在
  `callWithState` 同步栈投影；Design L137-L148 把 H 当普通 Service 依赖。Cloud 当前也没有 L90/L172 所称的
  runtime-warning/metrics owner。
- 影响：单个新 Service 文件无法安全读写 per-run rounds state，也无法等价保留 10min/repeat60s 告警；直接注入
  `GameContext` 会退回 default ThreadLocal state。
- 返修条件：命名 exact assembly-owned synchronous projection/caller 路径，使 Service 只在已绑定 State 的栈内调用
  `GameContext`；告警定义 typed tenant/taskRun/window sink 及失效/容量语义。未交付前 collaborator/host/caller 保持 dormant。

### P2-1：template image 生命周期声明超过现有接口合同

- 证据：L130 称模板 image 由 assets owner 持有且 caller 不 flush；实际 `CloudTemplateAssets.loadTemplate` 只承诺返回
  `Optional<BufferedImage>`，`PackagedTemplateAssets` 每次通过 loader 返回图像，没有共享 immutable/cache 所有权合同。
- 返修条件：明确每次加载结果由 caller 使用后 `flush`，或另行批准并实现 cache owner；本切片不得口头扩展接口语义。

### 立即实施切片 W-ACP-0

B 现在可以直接编码以下独立叶子，不等待完整 Design Repair：

1. Cloud exact-copy DHXY HEAD `AutoCombatPanelService` 中以下纯类型/纯决策到
   `src/main/java/com/bot/dhxy/service/AutoCombatPanelDecision.java`：`resolveRoundsRefreshReason`、
   `RoundsRefreshReason`、`RefreshDueBurstDecision`、`TeamRefreshDueBurstGuard` 及它们使用的常量；类为纯 Java，禁止 Spring、
   I/O、clock 读取、context、port、H state、warning。
2. public 静态 API 只接收原方法已有参数；保证 `null/default`、team fallback key、30s guard 和 signed wall-clock 差值逐字等价。
3. 不创建 `AutoCombatPanelService`，不改 remote/assembly/host/caller/DHXY/resources/tests，不启动运行面。
4. 完成后执行 Cloud `mvn -q clean package`，追加 `External Worker B - W-ACP-0 Implementation #1`，附 scoped diff、
   方法对照和构建证据。

同时下一份设计材料只追加 `Design Repair #1 Delta` 关闭上述 P1/P2，不重写已通过的基线方法盘点。
**无已批准业务差异；按基线等价迁移。**

## External Worker B - W-ACP-0 Implementation #1 - 2026-07-13

按父级批准的 `W-ACP-0` 独立叶子实施。**Cloud 1 new，DHXY 0**；纯 Java，无 Spring/I/O/clock/context/port/H-state/warning；
未创建 `AutoCombatPanelService`，未改 remote/assembly/host/caller/DHXY/resources/tests，未启动任何运行面。

### 落盘

- 新文件 `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelDecision.java`，SHA-256(16)
  `9C4F7A1C1491D72F`，73 行。`git status` 仅 `?? .../AutoCombatPanelDecision.java`（无其它变动）。

### 逐方法字节等价对照（HEAD `0114604e` `AutoCombatPanelService` → `AutoCombatPanelDecision`）

| 成员 | 来源行 | 等价点 |
|---|---|---|
| `resolveRoundsRefreshReason(int,long,long,long)` | HEAD L137-151 | 分支顺序、`<0→UNKNOWN`、`<=10→LOW_ROUNDS`、`interval>0 && (last<=0 || now-last>=interval)→REFRESH_DUE`、否则 `null` 逐字 |
| `enum RoundsRefreshReason{UNKNOWN,LOW_ROUNDS,REFRESH_DUE}` + `logValue()` | HEAD L299-313 | 三值与 `"unknown"/"low-rounds"/"refresh-due"`、`logValue()` 私有 逐字 |
| `record RefreshDueBurstDecision(deferred,retryAfterMs,lastTeamRefreshAgeMs)` + `allowed()/deferred()` | HEAD L315-323 | `allowed=(false,0,-1)`、`deferred=(true,retry,age)` 逐字 |
| `class TeamRefreshDueBurstGuard.reserveIfAllowed(teamKey,windowId,reason,now)` | HEAD L325-344 | 非 REFRESH_DUE→allowed；team fallback key（`teamKey` blank→windowId，再 blank→`"default"`）；`age=now-lastAt`、`age>=0 && age<30_000→deferred(30_000-age,age)`；否则 `put(key,now)+allowed` 逐字 |
| 常量 `LOW_ROUNDS_REFRESH_THRESHOLD=10`、`REFRESH_DUE_TEAM_BURST_GUARD_MS=30_000L` | HEAD L31/L36 | 逐字 |

- public 静态 API 参数不变（`resolveRoundsRefreshReason` 四参）；`null`/default/team fallback key/30s guard/signed
  `now-lastAt` 差值全部逐字保留（父级 W-ACP-0 第 2 条）。`AutoCombatPanelDecision` 私有构造，纯静态+嵌套类型持有者；
  `TeamRefreshDueBurstGuard` 独立可实例化（保留其 per-team `ConcurrentHashMap` 状态语义）。

### 构建证据

- Cloud `mvn -q clean package`（**无 skip**）：**exit 0**；Surefire **suites=4, tests=21, failures=0, errors=0,
  skipped=0**。`javap`：`public final class AutoCombatPanelDecision`，`public static RoundsRefreshReason
  resolveRoundsRefreshReason(int,long,long,long)`，嵌套 `RoundsRefreshReason`/`RefreshDueBurstDecision`/
  `TeamRefreshDueBurstGuard` 为 public。未启动运行面、未 git add/commit、两仓在途 dirty 未覆盖。

`W-ACP-0 自审仅 QA，不构成批准。` 等父级对本叶子的复核。

## External Worker B - Design Repair #1 Delta - 2026-07-13

只关闭 `Parent Design Review #1` 的 P1-1..P1-5 / P2-1，不重写已通过的基线方法盘点。纯设计，未改代码（W-ACP-0 除外，已
单独落盘）。主体 mechanical Java 在下列前置未交付前**保持冻结**。

- **P1-1（缺 retained action handle / 稳定身份）**：撤回"由 Service 用 `source` 铸造地址"。改为：Service 的每个 I/O
  方法接收一个**由 trusted 运行时/assembly 预建的 opaque handle bundle**（本 Service 不 mint、不构造
  `WindowFactAction/CaptureAction/InputBundleAction`）。定义**固定枚举的 caller-owned 持久 phase/slot**（一次
  `verifyAndAlignPanel`/`ensurePanelVisible` attempt 内）：`LOCATE`(CAPTURE)、`WINDOW_GEOMETRY`(WINDOW_FACT)、
  `OPEN`(INPUT)、`DRAG`(INPUT)、`REFRESH`(INPUT)；每 slot 一个 occurrence。线性化点：`begin(attempt)` 分配 →
  `reenter`（同 occurrence 同 bytes 重投）→ `final-consume`（业务读取 outcome）→ `retire`（occurrence 关闭）。raw
  `source/reason` **只进日志、不参与 identity**。写集选择（待父级拍板）：①Service 接收 activation 层预建 bundle（本
  collaborator 写集不碰 remote/）；或②在 remote 包新增 retained adapter（需父级把该文件纳入写集并指派归属）。**推荐①**。
- **P1-2（UNKNOWN/stale 被压成 miss/fail/fallback）**：为 locate/open/drag/refresh 定义 typed 兼容表——
  `OBSERVED_MATCH`/`OBSERVED_MISS`（locate）、`EXECUTED`/`NOT_EXECUTED`（open/drag/refresh）、`UNRESOLVED`、`STOPPED`。
  映射：基线 `null`(定位失败)=`OBSERVED_MISS`、`false`(输入失败)=`NOT_EXECUTED`——**只有这两者**驱动 missing-streak/
  round-state/drag-fallback（等价基线）。`UNRESOLVED`（capture 非 OBSERVED 无图 / input 可能已部分执行）→**不**返回
  null/false、**不**更新 missing-streak/round state、**不**启 fallback、**不** mint 新 identity；仅同 bytes outcome
  resolution 或 typed unwind 上抛，由 runtime 决定重投/退避。`STOPPED`→ typed 停止 unwind。
- **P1-3（`WINDOW_BASE_POINT` 不存在 / v1 input 只 SCREEN_ABSOLUTE_PX）**：改用 `WindowFactKind.GEOMETRY →
  GeometryFact(x,y,width,height,SCREEN_ABSOLUTE_PX)`，先验 `observedWindow` exact 匹配当前 ctx 窗口。capture region =
  GeometryFact 原点 +**固定 (x,y,1024,768)** 复现 HEAD `updateGlobalVision` 全视野 + 边界检查；template 匹配点为该帧
  local 坐标 → 屏幕绝对 = 原点 + local（+ P1-4 的 scale 反算）。**所有 input 点 `SCREEN_ABSOLUTE_PX`**（面板中心、drag
  from/to、落点 489/726 相对 GeometryFact 原点换算为屏幕绝对）。**不新增 WINDOW_BASE DTO**。
- **P1-4（漏 DPI scale 变换）**：HEAD 为 `round(matchX/systemScaleRatio)+windowBaseX`。云端当前无 scale fact/config
  authority——**主体 mechanical Java 冻结于此前置**。需父级指定 scale 的**唯一可信来源**：①`CaptureOutcome` 携带 capture-time
  `systemScaleRatio` typed fact（推荐，随帧一致）；或②per-window scale config owner。交付后 Service 逐式保留 HEAD 的
  `/systemScaleRatio` 除法与 `Math.round` rounding，**绝不假设 1.0**。
- **P1-5（H State + warning owner 非可调用依赖）**：`CloudGameContextStateOwner` 为 remote package-private、只能由 assembly
  持 activation handle 在 `callWithState` 同步栈投影内访问。改为：本 collaborator 由 **activation 层在已绑定 State 的同步栈内
  调用**（`callWithState(taskRun) { panelService.verifyAndAlignPanel(ctx,...) }`），Service 内对 `GameContext` 的回合估算
  读写即投影到该 per-run State（不直接注入 `GameContext`，不回退 default ThreadLocal）。缺失-attention 告警定义 typed
  **`PanelWarningSink(tenant,taskRun,window)`** 接口（由 activation/矩阵 owner 提供），逐项保留 10min/repeat60s 阈值与日志；
  失效/容量语义随 sink owner。**该前置未交付前 collaborator/host/caller 全程 dormant。**
- **P2-1（模板生命周期超接口合同）**：撤回"assets owner 持有、caller 不 flush"。改为：`CloudTemplateAssets.loadTemplate`
  每次返回**独立** `Optional<BufferedImage>`，本 Service 在每次匹配用毕即 `flush()`（caller-owned transient）；不口头扩展
  接口为 cache/immutable。若未来热路径需 cache，另开切片并批准 cache owner，本切片不夹带。

### 冻结与依赖闭合

主体 `service/AutoCombatPanelService.java`（mechanical port 版）待以下前置**全部** APPROVED 后方可实施，且顺序依赖非文件
交叉：①handle bundle 提供方（P1-1，推荐 activation 预建）；②typed outcome 兼容表被 port DTO 支持（P1-2）；③GEOMETRY 全
视野 + SCREEN_ABSOLUTE_PX（P1-3，已存在，仅需确认）；④scale 可信来源（P1-4，**当前缺，硬前置**）；⑤assembly `callWithState`
投影 + `PanelWarningSink`（P1-5）。在④⑤交付前，本 collaborator 仅 W-ACP-0 纯决策叶子落地（已完成），其余保持 dormant。
`P0=0`；本 Delta 后**待父级复核**是否接受①②③⑤的契约与④的来源指定。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Design Repair #1 Delta 到此停止，等待父级
`DESIGN APPROVED` 或再次 `BLOCKED`。

## Internal Worker O - CLAIMED - 2026-07-13

- 领取任务：接管 External Worker B 的 `Design Repair #3`，仅完成 `AutoCombatPanelService` 主体迁云设计返修并逐项闭合 `Parent Review #3` 的 P1/P2；`CLAIMED` 仅表示已领取，不表示完成或批准。
- 领取时间：`2026-07-13T01:37:27-04:00`（本地时间）。
- 当前唯一写集：`D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-12-cloud-auto-combat-panel-service-worker-b.md`（append-only）；在父级明确给出 `DESIGN APPROVED` 前，不修改 Java、Maven、resources、tests、其它 Worker 日志或 CR 文档。

## Parent Review #3 - MAIN DESIGN BLOCKED - 2026-07-13

父级已对照当前 `CloudTaskServicePort`、`CloudTaskRetainedActionState`、`CloudGameContextStateOwner`、H/K runtime 和
DHXY HEAD `AutoCombatPanelService` 复审 Repair #2。scale 决定已关闭且 W-ACP-0 批准不重开；主体仍为
**BLOCKED，P0=0 / P1=4 / P2=1**：

1. **P1：bundle 的 owner/store 仍不存在，而且“taskRun 激活时铸五个 handle”粒度错误。** 当前
   `CloudTaskRetainedActionState` 以 `(phaseCode, actionSlot, occurrence)` 保存一个具体业务 action attempt，handle identity
   绑定请求 bytes；同一 taskRun 内多次 locate/open/drag/refresh 的 region、actions 或 payload 可以不同，不能永久复用每种
   operation 的一个 identity。报告所称“retained-action store”“每 phase 有界 occurrence”都没有 exact FQCN、方法、frontier、
   容量值或 final-consumed/reclaim 合同。返修必须指定真实 owner，按每次业务 attempt 分配 canonical occurrence，给出
   begin/reenter/final-consume/retire/NOT_EXECUTED renewal 的 exact 方法和与 M frontier 的关系；不能按五个 operation 在 run
   激活时一次性铸造并复用到 terminal。
2. **P1：UNRESOLVED 的“端口内部同字节消解”没有实现边界。** 当前 `CloudTaskServicePort` Javadoc 与实现明确“no retry or
   outcome interpretation”，`CloudTaskRetainedActionState.invoke` 只在一次 current gate 下调用 delegate 一次。Repair 没有给出
   谁保存 unresolved request、谁在何时读取其最终 outcome、调用是否阻塞、断线/timeout/terminal 如何结束，也没有文件写集。
   不能凭一句“永不上抛业务”消除 UNKNOWN。返修必须给出一个真实 retained resolver 的 FQCN/API/容量/终止矩阵，且不得新增
   业务 retry/park/yield；若仍可能未决，必须有明确 typed unwind，不得映射 null/false。
3. **P1：STOPPED 映射自相矛盾。** Repair 一方面把 STOPPED 写成 `NOT_EXECUTED=false/兜底`，另一方面又声称不触
   missing-streak/fallback；HEAD 的 open `false` 会直接 `recordAutoPanelMissing`，refresh `false` 也走失败分支。STOPPED 必须
   沿现有 typed stop/checkpoint unwind 退出，不能伪装成机械 NOT_EXECUTED，也不能继续 drag fallback 或状态 mutation。
4. **P1：H State 与 warning owner 仍未闭合。** 现有 `CloudGameContextStateOwner.callWithState` 必须同时拿
   non-mintable `StateActivationHandle` 和 exact `TaskExecutionContext`；提议的 `callWithBoundState(ctx, body)` 丢失 handle，无法
   选择/证明 exact retained State，并且“revision 变化使 State 失效”与已批准的 same-State resume 合同冲突。应由 trusted
   activation adapter 保存 handle，并调用现有 API，而不是新增 ctx-only lookup。`PanelWarningSink` 仍只有接口名、没有 concrete
   owner/mint/terminal cleanup/assembly injection，主体仍无法形成可编译依赖图。
5. **P2：Repair #2 缺逐文件写集和依赖顺序。** 下一轮必须列出每个 New/Modify 的精确仓库路径/FQCN、owner、前置批准、
   与 A/M/J/H/K 的冲突顺序；不得再用“assembly/A 归属”“observation 归属”代替任务所有权。

Worker B 当前任务：主体 Java 继续冻结，只追加 `External Worker B - Design Repair #3 Delta`，逐项关闭以上五点；不要重写
W-ACP-0、HEAD 方法矩阵或已接受的 scale 章节。父级建议优先复用 M 的 final-consumed address/frontier 与现有 H
`callWithState(handle, context, action)`，不要另造平行 authority。**无已批准业务差异；按基线等价迁移。**

## Parent Review #2 - W-ACP-0 APPROVED / MAIN DESIGN BLOCKED - 2026-07-13

### W-ACP-0 implementation

**APPROVED，P0/P1/P2=0。** 父级逐段对照 DHXY HEAD `0114604e` 的
`AutoCombatPanelService.resolveRoundsRefreshReason`、`RoundsRefreshReason`、
`RefreshDueBurstDecision`、`TeamRefreshDueBurstGuard` 与两个常量；分支顺序、signed
`now-lastAt`、fallback key、30 秒 burst gate 均保持。写集只有 Cloud 新文件
`com.bot.dhxy.service.AutoCombatPanelDecision`，未触碰 Spring/I/O/context/remote/host/caller/tests。

父级 fresh Cloud `mvn -q clean package`：exit 0；Surefire suites=4、tests=21、
failures=0、errors=0、skipped=0。W-ACP-0 切片收口。

### 主体 Design Repair #1

主体 `AutoCombatPanelService` 仍 **BLOCKED，P1=3，P2=1**；不得开始主体 Java：

1. **P1：retained handle bundle 仍只有两个选项，没有真实 owner/API。** Delta 的“推荐 activation
   预建”没有指出现有 FQCN、bundle 字段、同 taskRun 跨 revision 保存者、终态释放和容量。当前
   `CloudTaskRunAuthorityAssembly.CurrentContextSlotActivation` 只是一次返回 record，不是 retained
   activation adapter。返修只需补 exact owner/方法/生命周期/写集，不重写已关闭矩阵。
2. **P1：`UNRESOLVED` 的 caller 语义仍写成由 runtime 决定重投/退避。** 这会把 retry/park/yield
   决策移出 HEAD 业务合同。返修必须给每个现有 public caller 的确定映射；transport 同字节 resolution
   可以在端口内部完成，但不得新增业务 retry、fallback、missing-streak 或 phase 转移。
3. **P1：H State 投影和 `PanelWarningSink` 仍没有可调用实现 owner。** `CloudGameContextStateOwner`
   明确是 package-private same-process owner；仅写“activation 层 callWithState”不能形成可编译调用链。
   必须给 exact FQCN、方法签名、authority identity、容量/失效、assembly 注入点及文件写集。
4. **P2：scale 来源的父级决定为 capture-time typed `systemScaleRatio`。** 它必须由 DHXY 本地 capture
   owner在同帧产生、随 capture outcome/wire/digest 绑定，Cloud 不自行探测、不默认 1.0；该跨仓协议
   改动另立前置切片，避免与 M 的 wire 写集并发覆盖。B 下一轮只补 `Design Repair #2 Delta` 的
   1-3 项和 scale 前置引用，不再重写全设计。

Worker B 当前任务：主体保持冻结；只追加上述 Delta。若父级另行下发 scale 前置实现，再开始 Java。

## External Worker B - Design Repair #2 Delta - 2026-07-13

只补 `Parent Review #2` 主体的 P1(1-3) + P2(scale 前置引用)，不重写已关闭矩阵/W-ACP-0。纯设计，主体 Java 继续冻结。

### P1-1：retained handle bundle 的 exact owner / API / 生命周期 / 容量 / 写集

- **精确契约**（B 消费，assembly 提供）：新增 remote 包 retained adapter
  `com.yueyunfe.dhxy.cloudbrain.remote.run.RetainedPanelActionBundle`（**非** 一次性 record，是 per-taskRun 跨 revision 持有者）。
  - **字段/API**（opaque，本类不 mint）：`CaptureAction locate()`、`WindowFactAction windowGeometry()`、
    `InputBundleAction open()`、`InputBundleAction drag()`、`InputBundleAction refresh()`——每个返回该 `(taskRunId, phase)`
    的**同一 retained handle**，跨 re-entry/revision occurrence 稳定；构造时经 `CloudTaskRetainedActionState` 一次性铸造。
  - **owner/saver**：`CloudTaskRunAuthorityAssembly` 在 taskRun activation 时构造并存入 **retained-action store（按 taskRunId
    跨 revision 保留）**，不用 `CurrentContextSlotActivation` 那个一次性 record。
  - **生命周期线性化**：`begin`=activation 铸 5 handle；`reenter`=同 phase 同 occurrence 同 bytes 重投；`final-consume`=业务读
    outcome；`retire`=taskRun terminal 释放 5 handle/occurrence。**容量**：固定 5 phase × 每 phase 有界 occurrence 计数
    （raw `source/reason` **不入 identity**，无界 slot 消除）。
  - **写集归属**：`RetainedPanelActionBundle` 落 **remote.run（assembly owner，即 A/remote 归属）**——请父级指派该文件归属或
    批准 B 在明确边界内新增；**B collaborator 写集不含它**，`service/AutoCombatPanelService.java` 仅以
    `RetainedPanelActionBundle bundle` 作 I/O 方法入参消费。**推荐 assembly owns**。

### P1-2：`UNRESOLVED` 逐 caller 确定映射（不新增业务 retry/fallback/streak/phase）

- **核心澄清**：`UNRESOLVED` 是**端口内部 transport 态**，由 retained identity **同字节 resolution 在端口内消解**，**永不上抛业务**。
  Service 业务分支只见 HEAD 同款的 4 个确定 outcome + `STOPPED`，逐 caller 映射：
  | caller | locate outcome | input outcome | 映射（=HEAD 分支逐字） |
  |---|---|---|---|
  | `ensurePanelVisible(source,waitMs)` | MATCH→panelCenter；MISS→null | open EXECUTED→复定位；NOT_EXECUTED→`recordAutoPanelMissing(input-failed)`+null | 与 HEAD L74-110 逐字 |
  | `verifyAndAlignPanel(mode)` | MISS→return false（HEAD L56-58） | drag EXECUTED/NOT_EXECUTED **均**复定位/`drag-target-fallback`（HEAD L123-128，不看 drag 结果）；refresh EXECUTED→`recordAutoCombatRefresh`+true，NOT_EXECUTED→warn+false（HEAD L175-182） | 与 HEAD 逐字 |
  | `recordCombatExit()` | 无 capture/input（纯状态 -3） | — | 与 HEAD L232-240 逐字 |
- `STOPPED`（runtime 停止信号）→ 走 HEAD 的 interrupt 等价路径：`submitAndWait` 被打断的返回即 `NOT_EXECUTED`（=false/兜底），
  **不**新增 stop 分支、**不**触 missing-streak/fallback/phase 转移。`UNRESOLVED` **绝不**返回 null/false、**绝不**更新 streak/
  round-state、**绝不** mint 新 identity——只端口内同 bytes 消解或 typed unwind。**零新增业务 retry/退避**（决策仍在 HEAD 合同内）。

### P1-3：H State 投影 + `PanelWarningSink` 的 exact FQCN / 签名 / authority / 容量 / 注入点 / 写集

- **H State 同步投影**（H owner 提供）：`com.yueyunfe.dhxy.cloudbrain.remote.CloudGameContextStateOwner` 增暴露
  collaborator 可调的**同步投影入口**
  `<T> T callWithBoundState(CloudTaskServiceExecutionContext ctx, java.util.function.Supplier<T> body)`：在 body 执行期间把
  **该 ctx.taskRun 的 per-run GameContext State** 绑定到同步栈（ThreadLocal 投影），body 内 `gameContext.getAutoCombatEstimatedRounds()`
  等即读写该 per-run state，**不回退 default**。**authority identity**=ctx `revalidate().allowed()` 的 `(tenant,taskRunId,runRevision)`；
  **容量/失效**：每 `(tenant,taskRun)` 一份 State，revision 变更/terminal 失效。**注入点**：activation 层以
  `stateOwner.callWithBoundState(ctx, () -> panel.verifyAndAlignPanel(bundle, ctx, ...))` **包裹调用** collaborator（Service 内
  不注入/不构造 GameContext，只在被绑定栈内用）。**写集归属**：该入口落 **H owner 文件（Worker H/remote 归属）**——B 不改，
  作前置依赖。
- **`PanelWarningSink`**（B 定义接口、observation owner 实现）：`com.yueyunfe.dhxy.cloudbrain.service.PanelWarningSink`
  `void warn(CloudTaskServiceExecutionContext ctx, String key, String message, java.util.Map<String,String> details)`；语义=HEAD
  `markRuntimeWarning`+`recordWindowWarning` 等价，**逐项保留** 10min 首告/repeat60s 去抖阈值与日志串
  `"自动战斗面板连续未识别超过10分钟..."`。**authority**=ctx 的 `(tenant,taskRun,window)`；**容量/失效**：per-window 去抖态随 sink
  owner，terminal 清。**写集**：接口可入 B 的 `service/` 写集；**实现 owner=迁移矩阵 observation/metrics 归属（前置）**，B 不实现。

### P2（scale）：接受父级决定，引用外部前置切片

- 父级已定：scale = **capture-time typed `systemScaleRatio`**，由 **DHXY 本地 capture owner 同帧产生**，随 capture
  outcome/wire/digest 绑定；**Cloud 不自探测、不默认 1.0**。该跨仓 wire 协议改动**另立前置切片**（避免与 M 的 wire 写集并发覆盖），
  **B 不改跨仓协议**，仅在其交付后消费：Service 逐式保留 HEAD `CoordinateHelper.findImageAbsoluteCoordinateByImagePath`
  L153-160 的 `Math.round(matchLocalX / captureOutcome.systemScaleRatio()) + geometryOrigin.x`（y 同），除法与 rounding 逐字，
  **绝不假设 1.0**。

### 冻结与前置闭合（更新）

主体 `service/AutoCombatPanelService.java` 待以下**全部**交付/APPROVED 方可实施，均为**依赖 owner 前置**（非 B 文件交叉）：
①`RetainedPanelActionBundle`（remote.run，assembly/A 归属）；②`CloudGameContextStateOwner.callWithBoundState`（H 归属）+
`PanelWarningSink` 实现（observation 归属）；③capture-time `systemScaleRatio` wire 前置切片（跨仓，独立）。③④之前 collaborator
仅 W-ACP-0（已 APPROVED 收口）落地，其余 dormant。B 本轮无新代码。`P0=0`；待父级复核契约并指派①②③的 owner/写集。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Design Repair #2 Delta 到此停止，等待父级
`DESIGN APPROVED` 或再次 `BLOCKED`。

## External Worker B - Design Repair #3 Delta - 2026-07-13

逐条关闭 `Parent Review #3`（MAIN DESIGN BLOCKED，P1=4/P2=1）。按父级建议**复用 M 的 final-consumed address/frontier 与
现有 H `callWithState(handle, context, action)`**，不另造平行 authority；不重写 W-ACP-0/HEAD 矩阵/已接受的 scale 章节。
纯设计，主体 Java 继续冻结。

### P1-1：撤回"激活时铸 5 handle 复用到 terminal"，改为**每次业务 attempt 分配 canonical occurrence**

- 事实纠正：`CloudTaskRetainedActionState` 以 `(phaseCode, actionSlot, occurrence)` 保存**一次具体业务 attempt**，identity 绑
  该次请求 bytes（region/actions/payload）。故同一 taskRun 内多次 locate/open/drag/refresh **各是独立 attempt**，不能按 operation
  永久复用一个 identity。HEAD 语义印证：`ensurePanelVisible` 内 locate→miss→Alt+8→**再 locate** 是**两个 locate attempt**（两个
  occurrence）。
- **真实 owner/分配**：canonical occurrence/address 由 **M 的 final-consumed frontier `RemoteFinalConsumedReceipt`**（+
  `CloudTaskRetainedActionState`）统一分配，**由 trusted activation adapter**（非本 Service）按每次业务 attempt 取 address；本
  Service 只在被调用时**消费该 attempt 的 opaque handle**。
- **每 attempt 生命周期**（复用 M frontier 合同，无平行 authority）：`begin`=activation 从 M frontier 取该 attempt 的 canonical
  address+occurrence；`reenter`=同 address 同 bytes 返回同 retained handle；`final-consume`=业务读该 attempt outcome，frontier 标
  final-consumed（`RemoteFinalConsumedReceiptAck`）；`retire`=receipt ack 释放；`NOT_EXECUTED renewal`=**新的真实 attempt**（如
  HEAD Alt+8 后的再 locate）→取**下一个 occurrence**，不复用旧 identity。容量随 M frontier 既有 bound（raw `source/reason` 仅
  日志、不入 address）。

### P1-2：UNRESOLVED 的真实 retained resolver = M frontier，端口保持单发

- `CloudTaskServicePort`/`CloudTaskRetainedActionState.invoke` 明确"no retry / delegate 一次"——**不改**。跨 attempt 的"同字节
  消解"由 **M final-consumed frontier** 承担：某 address 若已 `RemoteFinalConsumedReceipt` 记录 final outcome，trusted runtime
  对同 address 的 reenter **返回该已记录 outcome**（同 bytes，无二次 delegate）；**owner=M frontier**，API=既有 receipt/ack，容量随
  其 bound。
- **终止矩阵**：`OBSERVED_*`/`EXECUTED`/`NOT_EXECUTED` = 端口单发确定 outcome，直接进业务（=HEAD 分支）。断线/timeout/尚未
  final-consumed = **genuinely UNRESOLVED** → **typed unwind**（一个 typed `PanelActionUnresolved` 沿 runtime 上抛，由 activation/
  runtime 决定，非本 Service），**绝不**映射 null/false、**绝不**更新 streak/round-state、**绝不** mint 新 identity、**不**新增业务
  retry/park/yield。Service 只见"确定 outcome 或 typed unwind"二选一。

### P1-3：STOPPED 沿 typed stop/checkpoint unwind 退出，**不等于** NOT_EXECUTED

- 纠正 Repair #2 的自相矛盾：`STOPPED`（runtime 停止信号）**不**映射 `NOT_EXECUTED`。它沿**既有 typed stop/checkpoint unwind**
  （HEAD `TaskCheckpoint`/`TaskStopRequestedException` 的云端等价）直接退出方法，**跳过** missing-streak、drag-fallback、round-state
  mutation 与后续 phase。`NOT_EXECUTED` 专指**机械输入失败**（HEAD `submitAndWait` 返回 false）→ 保留 HEAD false 分支
  （open false→`recordAutoPanelMissing`、refresh false→失败日志+return false）。三者互斥：`EXECUTED` / `NOT_EXECUTED`(机械失败,走
  HEAD false) / `STOPPED`(typed unwind,零副作用)。

### P1-4：H State 用现有 `callWithState(handle, context, action)`；warning sink 落 B 自有 concrete owner

- **H 投影**：撤回 `callWithBoundState(ctx, body)`。改为**复用现有** `CloudGameContextStateOwner.callWithState(StateActivationHandle
  handle, TaskExecutionContext context, Supplier action)`（L173）。**trusted activation adapter** 持 non-mintable
  `StateActivationHandle`（来自 `activateInitial`/`activateResumed`）+ exact context，以
  `stateOwner.callWithState(handle, ctx, () -> panel.verifyAndAlignPanel(bundle, ctx, ...))` **包裹调用** collaborator；Service 内对
  `GameContext` 的回合读写即投影该 exact retained State。**不新增 ctx-only lookup、不按 revision 使 State 失效**（尊重已批准
  same-State resume：resume 走 `activateResumed` 复用同 State）。该 adapter=activation/assembly 归属（见写集）。
- **warning sink（B 自有,不外包）**：B 写集含接口 `com.yueyunfe.dhxy.cloudbrain.service.PanelWarningSink` **与 concrete 实现**
  `com.yueyunfe.dhxy.cloudbrain.service.LoggingPanelWarningSink`（B owns）：`warn(ctx,key,message,details)` 逐字保留 HEAD
  告警串 `"自动战斗面板连续未识别超过10分钟，请人工检查是否已断自动"` + 10min 首告/repeat60s 去抖态（per (taskRun,window)，
  terminal 清）。HEAD 的 `markRuntimeWarning`(UI) 云端无 UI 面→退化为该结构化 warning（core 语义=日志+去抖，逐字）；
  `recordWindowWarning` 的 metrics 侧**可选**经注入的既有 metrics owner，缺则 log-only（不影响业务分支）。**mint/terminal
  cleanup/injection**：sink 由 collaborator 构造期注入，terminal 随 taskRun 清 per-window 去抖态。

### P2/写集：逐文件精确写集 + 依赖顺序（P0=0）

| # | 仓库路径 / FQCN | New/Modify | owner | 前置/依赖顺序 | 与 A/M/J/H/K 冲突 |
|---|---|---|---|---|---|
| 1 | `dhxy-cloud-brain/.../service/AutoCombatPanelService.java` | New | **B** | 依赖 #2 bundle + #4 callWithState 包裹 + scale wire；dormant 不接 caller | 无（consume 其 public API） |
| 2 | `dhxy-cloud-brain/.../service/PanelWarningSink.java` + `LoggingPanelWarningSink.java` | New | **B** | 无（自含） | 无 |
| 3 | trusted activation adapter：按每 attempt 从 M frontier 取 occurrence + 建 handle bundle + 持 `StateActivationHandle` 调 `callWithState` 包裹 collaborator | New | **activation/assembly（A/remote 归属，需父级指派）** | 依赖 A `CloudTaskRetainedActionState`/M `RemoteFinalConsumedReceipt`/H `callWithState`（均已存在） | 顺序在 A/M/H 之后；与 K current-context slot 走同 activation，需 A/K 协调 |
| 4 | capture-time `systemScaleRatio` wire 前置切片（跨仓 DHXY capture owner + wire/digest） | New | **capture/wire owner（跨仓，独立切片，需父级指派）** | 与 M wire 写集**不并发**（独立切片） | 与 M wire 顺序隔离 |

- **B 本轮写集 = #1 + #2（均 B owns，dormant）**；#3/#4 为**必须由父级指派 owner 的前置**（B 不代管、不越权写 remote/跨仓）。
  主体 `AutoCombatPanelService.java` 待 #3(handle bundle + callWithState 包裹)与 #4(scale)交付/APPROVED 后方可实施；当前仅
  W-ACP-0（已收口）落地。请父级：①确认 #1/#2 由 B 实施的时机（可在 #3/#4 前先落 dormant 骨架 or 一并）；②指派 #3/#4 owner。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Design Repair #3 Delta 到此停止，等待父级
`DESIGN APPROVED` 或再次 `BLOCKED`。

## Parent Worker Claim Protocol - 2026-07-13T01:37:59-04:00

从下一条父级任务或返修单开始，Worker B 必须在任务发布后 20 分钟内先向本固定日志追加
`## External Worker B - CLAIMED - <timestamp>`，写明领取的父级任务标题和唯一写集。`CLAIMED` 只证明任务已经被领取，
不表示完成或批准；领取后允许实际设计/实现超过 20 分钟，并继续按既有 heartbeat 读取本日志。仅当 20 分钟内没有
`CLAIMED` 回执时，父级才判定外部任务可能未唤醒/卡死，并把该切片交给内部 Worker 接管。不得用“20 分钟没有交付成果”
作为接管条件。

## Parent Design Review #4 - MAIN BLOCKED / W-ACP-1 IMPLEMENTATION APPROVED - 2026-07-13T01:43:56-04:00

Repair #3 已修正固定五 handle、STOPPED 映射和 H `StateActivationHandle` 投影问题。主体仍为
**BLOCKED，P0=0 / P1=2 / P2=1**：

1. **P1：`RemoteFinalConsumedReceipt` 不是 occurrence 分配 API。** 它是 DHXY 已应用 frontier 后回 Cloud 的 receipt DTO；
   当前 `CloudTaskRetainedActionState.retain*` 仍接收 caller-supplied `ActionAddress`，Full R0 也尚未实现。主体返修必须引用
   M Full R0 最终获批的 exact package-private mint API/opaque handle owner，不能让 activation adapter 从 receipt 自行推导或
   传 raw occurrence/string。
2. **P1：receipt/frontier 当前不能返回 compact 后的 typed outcome。** 现有 action ledger 只留
   `recordedState + recordedOutcomeDigest`；receipt 也没有 `WindowFactOutcome/CaptureOutcome/InputBundleOutcome` payload。
   在 detail 未 compact 时，同 handle+同参数可经 broker 返回 retained result；compact 后若要 reenter，必须由 Full R0 明确
   typed outcome 的保留/消费边界，不能把 receipt 本身当 resolver。
3. **P2：warning 的 `log-only` fallback 丢失 HEAD 的 runtime warning + metrics 可见性。** 主体接线前须有 typed warning
   notification owner/port，或明确保持等价的云端结构化事件并由本地 UI/metrics 消费；缺 owner 不能静默降级为可选 metrics。

为避免继续等待前置，父级单独放行 **W-ACP-1**：Worker B 现在直接修改现有 Cloud
`src/main/java/com/bot/dhxy/service/AutoCombatPanelDecision.java`，**1 Modify / 0 New**，把 HEAD
`recordAutoPanelMissing` / `clearAutoPanelMissing` 的纯状态转移抽入该类：

- 常量逐值保持 `10*60*1000L` 与 `60*1000L`；clock 由参数传入，类内不读 wall clock；
- immutable missing state 至少保留 `missingSinceAt` 与 `lastAttentionAt`；
- 结果必须可区分首次 STARTED、阈值前 STILL_MISSING、60 秒内 SUPPRESSED、ATTENTION_REQUIRED，并返回 next state 与
  baseline `missingMs`；clear 精确归零；负/回拨时间按 HEAD 的普通 long 算术，不新增 clamp/TTL/fail-closed；
- 不加 Spring/I/O/log/metrics/context/port/线程，不改现有 rounds/burst 决策，不新增 tests。

`AutoCombatPanelService` 主体、sink、activation adapter、M/H/K/assembly、scale wire、host/caller 全部冻结。Worker B 必须在
本任务发布时间后 20 分钟内先追加 `## External Worker B - CLAIMED - <timestamp>`（任务名 `W-ACP-1`、写集 1 Modify），
随后实施并追加 `External Worker B - W-ACP-1 Implementation #1`。共享 `target/` 构建由父级在 A/B 写入稳定后统一执行 fresh
Cloud `mvn -q clean package`。**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T01:49:33-04:00

- **领取任务**：`W-ACP-1`（`Parent Design Review #4` 单独放行的实施切片）——把 HEAD `AutoCombatPanelService`
  `recordAutoPanelMissing`/`clearAutoPanelMissing` 的**纯状态转移**抽入现有 `AutoCombatPanelDecision`。
- **唯一写集**：**1 Modify / 0 New** = `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelDecision.java`。
- 约束：常量 `10*60*1000L`/`60*1000L` 逐值；clock 由参数传入、类内不读 wall clock；immutable missing state 含
  `missingSinceAt`+`lastAttentionAt`；结果区分 `STARTED`/`STILL_MISSING`/`SUPPRESSED`/`ATTENTION_REQUIRED` + next state +
  baseline `missingMs`；clear 精确归零；负/回拨时间按 HEAD 普通 long 算术，不 clamp/TTL/fail-closed；无 Spring/I/O/log/metrics/
  context/port/线程，不改 rounds/burst 决策，不加 tests。主体/sink/activation/M/H/K/scale/host/caller 全部冻结。
- 本 `CLAIMED` 仅表示已接手（20 分钟内回执），不代表完成/批准；随后追加 `W-ACP-1 Implementation #1`。主体 `Design Repair #4
  Delta` 另行追加关闭 P1×2/P2×1。**无已批准业务差异；按基线等价迁移。**

## External Worker B - W-ACP-1 Implementation #1 - 2026-07-13

按 `Parent Design Review #4` 放行的 `W-ACP-1` 实施。**1 Modify / 0 New**，仅改现有
`dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelDecision.java`；纯 Java，无
Spring/I/O/log/metrics/context/port/线程；未改 rounds/burst 决策，未加 tests；主体/sink/activation/M/H/K/scale/host/caller
全部保持冻结。

### 落盘（1 Modify）

- `AutoCombatPanelDecision.java`：SHA-256(16) `5564144F5AB9F322`，126 行。`git status` 仅该文件（`??`——W-ACP-0 起为
  working-tree 未提交文件，相对已批准 W-ACP-0 版本为 1 Modify，**零 git mutation**）。新增：常量
  `AUTO_PANEL_MISSING_ATTENTION_MS=10*60*1000L`、`AUTO_PANEL_MISSING_ATTENTION_REPEAT_MS=60*1000L`；`static recordMissing`、
  `static clearMissing`；`enum MissingOutcome{STARTED,STILL_MISSING,SUPPRESSED,ATTENTION_REQUIRED}`；`record MissingPanelState
  (missingSinceAt,lastAttentionAt)`；`record MissingPanelTransition(nextState,outcome,missingMs)`。

### 逐分支字节等价（HEAD `recordAutoPanelMissing`/`clearAutoPanelMissing` L193-230 → 纯转移）

| HEAD 分支 | 纯转移结果 |
|---|---|
| `missingSinceAt<=0` → set `missingSinceAt=now`（不动 lastAttentionAt） | `STARTED`，nextState=(now, lastAttentionAt)，missingMs=0 |
| `missingMs=now-missingSinceAt < 10min` | `STILL_MISSING`，nextState=current，missingMs |
| `lastAttentionAt>0 && now-lastAttentionAt < 60s` | `SUPPRESSED`，nextState=current，missingMs |
| else → set `lastAttentionAt=now` | `ATTENTION_REQUIRED`，nextState=(missingSinceAt, now)，missingMs |
| `clearAutoPanelMissing` | `clearMissing`→(0,0) 精确归零 |

- clock 仅由参数 `now` 传入，类内不读 wall clock；负/回拨时间走普通 `long`（`missingMs<10min` 对负值为真→`STILL_MISSING`），
  **无 clamp/TTL/fail-closed**，与 HEAD 一致。HEAD 的日志/告警/streak-cleared missingMs 由**未来主体 caller**承担（本纯类不 I/O）。

### 构建证据

- **非破坏性**独立 `javac -d <scratch> AutoCombatPanelDecision.java`：**exit 0**（该类仅依赖 `java.util`，可独立编译；
  **刻意不跑 `mvn clean package`** 以免 clean 掉与 Worker A 并发的共享 `target/`——按父级"共享 target build 由父级在 A/B 稳定后
  统一 fresh Cloud `mvn -q clean package`"）。`javap` 确认 `recordMissing`/`clearMissing` 为 `public static` + 三嵌套类型
  public。未启动运行面、未 git add/commit。

`W-ACP-1 自审仅 QA，不构成批准。` 等父级统一 fresh package 与复核。

## External Worker B - Design Repair #4 Delta - 2026-07-13

逐条关闭 `Parent Design Review #4` 主体的 P1×2 + P2×1。纯设计，主体 Java 继续冻结；不重写已通过的 W-ACP-0/1、HEAD 矩阵、
scale 与已接受章节。

- **P1-1（`RemoteFinalConsumedReceipt` 不是 occurrence 分配 API）**：纠正 Repair #3——receipt 只是 DHXY 应用 frontier 后回
  Cloud 的 DTO，`CloudTaskRetainedActionState.retain*` 现仍收 **caller-supplied `ActionAddress`** 且 **M Full R0 未实现**。故
  occurrence/handle 的**分配 owner = M Full R0 最终获批的 exact package-private mint API / opaque handle owner**（M 归属，前置）。
  trusted activation adapter **只从该 mint API 取 opaque handle**，**不**从 receipt 推导、**不**传 raw occurrence/string。每次业务
  attempt 经该 mint API 取一个 opaque handle（begin/reenter/final-consume/retire 由 Full R0 合同定义）。
- **P1-2（receipt/frontier 不能返回 compact 后 typed outcome）**：现 action ledger 仅 `recordedState+recordedOutcomeDigest`，
  receipt 无 `WindowFactOutcome/CaptureOutcome/InputBundleOutcome` payload。故 UNRESOLVED 的"同参数 reenter 拿 retained 结果"
  **仅在 detail 未 compact 时**由 broker 承担（同 handle+同参数）；**compact 后**的 reenter 保留/消费边界**由 M Full R0 明确定义**
  （前置），**不得把 receipt 本身当 resolver**。本 Service 只见"确定 typed outcome 或 typed unwind"，跨 compact 的可解性归 Full R0。
- **P2-1（warning log-only 降级丢可见性）**：撤回 log-only fallback。改为主体接线前必须有 **typed warning notification
  owner/port**：`PanelWarningSink.warn(...)` 发一个 typed `RuntimeWarningNotification(tenant,taskRun,window,key,message,details)`
  到该 owner/port（**named 前置**，由 UI/metrics 侧 owner 实现与消费），逐项保留 HEAD `markRuntimeWarning`+`recordWindowWarning`
  的可见性与告警串/10min/60s 语义；**缺该 owner 前不静默降级、主体冻结**。（W-ACP-1 已把 missing 的**纯判定**落地，判定与告警
  投递解耦——判定属纯类，投递待此 warning owner。）

### 冻结与前置（更新）

主体 `service/AutoCombatPanelService.java` 待以下**全部** APPROVED/交付方可实施（均为依赖 owner 前置，非 B 文件交叉）：
①**M Full R0** 的 package-private handle mint API + typed-outcome 保留/消费边界（M 归属，P1-1/P1-2）；②typed warning
notification owner/port（UI/metrics 归属，P2-1）；③capture-time `systemScaleRatio` wire（跨仓，独立）；④H 现有
`callWithState(handle,context,action)` + activation adapter（已在 Repair #3 定契约）。当前已落地叶子：W-ACP-0（决策/枚举/guard，
已 APPROVED 收口）+ W-ACP-1（missing 纯转移，本轮）。B 主体无新代码，`P0=0`；待父级复核并交付①②。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。W-ACP-1 Implementation #1 与 Design Repair #4 Delta
到此停止，等待父级统一 fresh package + `DESIGN APPROVED`/`BLOCKED`。

## Parent Implementation Review #1 - W-ACP-1 SOURCE APPROVED / PACKAGE PENDING - 2026-07-13T01:54:00-04:00

父级已对照 DHXY HEAD `AutoCombatPanelService.recordAutoPanelMissing/clearAutoPanelMissing` 逐分支复核最新源码，W-ACP-1
源码结论为 **SOURCE APPROVED，P0=0 / P1=0 / P2=0**：

- 10 分钟与 60 秒常量逐值一致；`now` 完全由调用方传入，未引入新 clock/TTL/clamp/fail-closed；
- STARTED、STILL_MISSING、SUPPRESSED、ATTENTION_REQUIRED 四分支、`missingMs` 普通 long 算术、next state 与 clear `(0,0)` 均与 HEAD 等价；
- 写集严格为现有 `AutoCombatPanelDecision.java` 1 Modify，无 Spring/I/O/log/metrics/context/port/线程/tests，未改 rounds/burst 决策或冻结主体。

最终 Implementation APPROVED 仍等待当前并行 Java 写入稳定后的父级 fresh Cloud `mvn -q clean package`。Design Repair #4
只正确登记 M Full R0、typed warning notification 与 capture-time scale 的外部前置；这些前置未实现前，主体仍为
**BLOCKED，P0=0 / P1=2 / P2=1**。**无已批准业务差异；按基线等价迁移。**

## Parent Task Brief - W-ACP-WARN-D1 - 2026-07-13T01:58:46-04:00

Worker B 的下一任务是为 Panel 主体 P2 所需的 **typed runtime-warning notification** 形成可实施设计。发布后 20 分钟内先
追加 `## External Worker B - CLAIMED - <timestamp>`（任务名 `W-ACP-WARN-D1`、写集仅本日志），领取截止
`2026-07-13T02:18:46-04:00`；领取后允许设计超过 20 分钟。

本轮只设计、Java 零修改。必须对照 DHXY HEAD 的
`WindowRuntimeContext.markRuntimeWarning`、`AutomationMetricsService.recordWindowWarning`、Panel 的 10min/60s gate，及现有
Cloud/DHXY remote envelope/digest/local-handler 模式，给出：

1. Cloud -> DHXY 单向 typed notification 的 exact scope/taskRun/window/stopEpoch/runRevision、canonical source/key/message/
   attributes 与稳定 notification identity；重投必须同 bytes，Service 不自铸随机 ID或自动 retry。
2. Cloud admission/outbox 与 DHXY 本地副作用前的 tenant/window/revision fence、去重终态、UNKNOWN/STOPPED/terminal 行为；
   不开放 raw request/poll/outcome，不把 warning 当输入命令，也不允许 stale warning 覆盖新 run UI。
3. 本地一次成功消费必须同时保持 HEAD 的 user-facing `lastMessage` 与 `SYSTEM_WARNING` metrics；部分失败的收敛、容量、restart
   fail-closed、诊断字段和运维可见性必须明确，不能 log-only 静默降级。
4. 精确跨仓文件表、依赖顺序与最小可编码 W-ACP-WARN-0；不得修改 M/P lifecycle adapter、N SummonSkill、A TMS 或 host/caller。

完成后只向本日志追加 `External Worker B - W-ACP-WARN-D1 Design #1`，等待父级 review；批准前不改 Java、不运行 Maven、
不启动运行面、不做 git mutation。**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T02:02:49-04:00

- **领取任务**：`W-ACP-WARN-D1`（`Parent Task Brief - W-ACP-WARN-D1`，发布 01:58:46，领取截止 02:18:46）——为 Panel 主体
  P2 所需的 **typed runtime-warning notification** 形成可实施设计。
- **唯一写集**：**仅本固定日志**（design-only，零 Java/Maven/resources/tests，不启动运行面，无 git mutation）。
- 交付 4 段：①Cloud→DHXY 单向 typed notification 的 exact scope/taskRun/window/stopEpoch/runRevision + canonical
  source/key/message/attributes + 稳定 notification identity（同 bytes 重投，不自铸随机 ID/不自动 retry）；②admission/outbox +
  DHXY 本地副作用前 tenant/window/revision fence、去重终态、UNKNOWN/STOPPED/terminal 行为（不开 raw request/poll/outcome、
  warning 非输入命令、stale 不覆盖新 run UI）；③本地一次成功消费同时保 HEAD `lastMessage` + `SYSTEM_WARNING` metrics，
  部分失败收敛/容量/restart fail-closed/诊断/运维可见性（不 log-only 降级）；④精确跨仓文件表 + 依赖顺序 + 最小可编码
  `W-ACP-WARN-0`（不改 M/P lifecycle adapter、N SummonSkill、A TMS、host/caller）。
- 本 `CLAIMED` 仅表示 20 分钟内已接手，不代表完成/批准。随后追加 `W-ACP-WARN-D1 Design #1`。**无已批准业务差异；按基线
  等价迁移。**

## External Worker B - W-ACP-WARN-D1 Design #1 - 2026-07-13

纯设计，零 Java/Maven/resources/tests。基线落点：HEAD `WindowRuntimeContext.markRuntimeWarning(String)`（L2048-2049，
`lastMessage=normalize(message)`，user-facing）+ `AutomationMetricsService.recordWindowWarning(ctx,source,message,attrs)`
（L367，`SYSTEM_WARNING` metric）；Panel ATTENTION_REQUIRED 分支（W-ACP-1 已落纯判定）同时触发这两者。`无已批准业务差异；
按基线等价迁移。` 自审仅 QA。

### 1. Cloud→DHXY 单向 typed notification identity

- **DTO `RuntimeWarningNotification`（typed，不可变）**字段：`scope`（`RemoteTaskRunScope` tenant/user/device/clientSession）、
  `taskRunId`、`window`（windowId + normalized nativeHandle）、`stopEpoch`、`runRevision`、`source`（canonical，如
  `"auto-combat-panel"`）、`key`（稳定，如 `"auto-combat-panel-missing"`）、`message`（**逐字** HEAD 串
  `"自动战斗面板连续未识别超过10分钟，请人工检查是否已断自动"`）、`attributes`（有序 `Map`：`reason`、`missingMs`）。
- **稳定 notification identity** = 对 `(scope,taskRunId,runRevision,window,source,key,attentionOccurrence)` 的**确定性 digest**
  （同现有 remote envelope/digest 模式）。`attentionOccurrence` **取自 W-ACP-1 missing 判定**的 `ATTENTION_REQUIRED` 那次
  `lastAttentionAt`（**非随机**）——同一次 attention 的重投 = **同 bytes 同 identity**；60s 后的下一次 attention = 新 occurrence =
  新 identity。Panel/Service **不自铸随机 ID、不自动 retry**（重投由 outbox 按同 identity 同 bytes 完成）。

### 2. Cloud admission/outbox + DHXY fence/去重/UNKNOWN-STOPPED-terminal

- **Cloud 侧**：`RuntimeWarningOutbox`（有界，per taskRun）持 pending notification（键=identity）。admission 前
  `ctx.revalidate().allowed()` ACTIVE + tenant/user + `(taskRunId,runRevision)` 门；**单向**经现有 Cloud→DHXY envelope/broker
  投递，**非** request/poll、**不**回收 outcome、warning **不是输入命令**（不驱动任何 Task 决策）。
- **DHXY 本地副作用前 fence**：consume 前校验 notification 的 `(tenant,window,runRevision,stopEpoch)` == 当前 run；
  **stale**（`runRevision` 旧 / window 不符 / stopEpoch 过期）→**丢弃**，**绝不覆盖新 run 的 UI `lastMessage`**。
- **去重终态**：DHXY 维护 per-(taskRun,window) 的 **consumed-identity set**（有界）；重复 identity → **幂等 no-op**。
- **UNKNOWN**（传输未决）：notification 留 Cloud outbox，按**同 identity 同 bytes** 重投；DHXY 去重保证**至多一次副作用**。
  **STOPPED/terminal**：run 终态时 outbox 该 run 条目退休（有界），consumed-set 清；stale warning 不进新 run。

### 3. 本地一次成功消费同时保 `lastMessage` + `SYSTEM_WARNING`（不 log-only 降级）

- **DHXY local handler**（新）on consume：**同时**执行 `windowContext.markRuntimeWarning(message)`（user-facing lastMessage）
  **与** `automationMetricsService.recordWindowWarning(windowContext, source, message, attributes)`（`SYSTEM_WARNING`
  metric）——逐项保 HEAD 双可见性。
- **部分失败收敛**：仅当**两者都成功**才把 identity 记入 consumed-set 并向 Cloud 确认；任一失败 → **不**标 consumed → outbox
  同 identity 同 bytes 重投；`markRuntimeWarning` 幂等（set lastMessage），`recordWindowWarning` 由 **identity 去重**避免重复计
  `SYSTEM_WARNING`。**绝不 log-only 静默降级**（缺 sink/失败即 fail-closed 重投，不降级）。
- **容量**：outbox + consumed-set 均有界（per run，terminal 清）。**restart fail-closed**：Cloud outbox 为真相源，DHXY 重启后
  consumed-set 丢失 → Cloud 按 identity 重投 → DHXY 幂等重放（lastMessage 幂等 + metrics identity 去重于有界窗口）；无法确认
  consume 即保持 pending（fail-closed，不丢告警）。
- **诊断/运维可见性**：结构化字段 `identity(前缀)/taskRun/window/source/key/missingMs/consumeResult/attempt`；不记敏感全路径。

### 4. 精确跨仓文件表 + 依赖顺序 + 最小可编码 W-ACP-WARN-0

| # | 仓库路径 / FQCN | New/Modify | owner | 依赖顺序 |
|---|---|---|---|---|
| a | `dhxy-cloud-brain/.../service/RuntimeWarningNotification.java`（typed DTO + identity digest） | New | **B** | 叶子，无依赖 |
| b | `dhxy-cloud-brain/.../service/PanelWarningSink.java`（接口）+ `RuntimeWarningOutbox`（有界 outbox + admission） | New | **B** | 依赖 a + Cloud→DHXY envelope/broker（A/remote 现有） |
| c | `DHXY/.../<warning>/RuntimeWarningNotificationHandler.java`（本地消费：fence+去重+markRuntimeWarning+recordWindowWarning） | New | **B（跨仓 DHXY）** | 依赖 a 的 DTO/identity + 现有 `WindowRuntimeContext`/`AutomationMetricsService` + envelope 接收端 |
| d | Cloud→DHXY warning envelope 类型登记（复用现有 envelope/digest 通道，若需新 message-type 常量） | Modify（envelope owner） | **envelope/broker owner（A/remote，需父级指派）** | 前置 |

- **最小可编码 `W-ACP-WARN-0`** = **#a**（`RuntimeWarningNotification` typed DTO + 确定性 identity digest，纯类型，无 Spring/I/O/
  transport），可**立即**独立落地编译（同 W-ACP-0/1 叶子模式）；#b/#c/#d 待 envelope message-type（#d，A/remote 归属）与父级
  批准后逐步接线。**不改** M/P lifecycle adapter、N SummonSkill、A TMS、host/caller。
- **依赖顺序**：#a（B，now）→ #d envelope 登记（A/remote）→ #b Cloud outbox（B）→ #c DHXY handler（B 跨仓）。Panel 主体 P2
  接线 = 主体解冻后由 `PanelWarningSink`(#b) 发 `RuntimeWarningNotification`(#a)。

`P0=0`；待父级复核契约并指派 #d envelope owner、批准 #a `W-ACP-WARN-0` 立即实施时机。`无已批准业务差异；按基线等价迁移。`
Worker B 自审仅 QA，不构成批准。W-ACP-WARN-D1 Design #1 到此停止，等待父级 `DESIGN APPROVED` / `W-ACP-WARN-0 批准` /
`BLOCKED`。

## Parent Design Review #5 - W-ACP-WARN-D1 BLOCKED / Repair #1 Published - 2026-07-13T02:11:07-04:00

父级已对照 DHXY HEAD 的 warning/metrics 实现与当前 remote polling transport 复审。保持两种可见性的目标正确，但当前设计仍为
**BLOCKED，P0=0 / P1=3 / P2=1**；`W-ACP-WARN-0` 暂不编码：

1. **P1：`lastAttentionAt` 不是稳定 notification identity owner。** 它是 W-ACP-1 纯判定里的 wall-clock 状态；若判定已更新
   state、但 outbox admission 前失败，下一次自然调用会取得新的 `now/lastAttentionAt`，从而把同一未交付 warning 铸成新
   identity/bytes。返修必须由 retained warning ledger/outbox 在业务 occurrence 首次 admission 时原子保留稳定 identity 与完整
   canonical bytes；Service 只提交业务 key/payload 或 opaque retained handle，不自行从 wall clock 生成幂等身份。须说明失败在
   missing-state 更新前、reserve 后、publish 后各如何重入。
2. **P1：所谓“现有 Cloud->DHXY 单向 envelope/broker”当前不存在。** DHXY
   `RemoteCommandPollingLoop.java:145-163` 只有 poll command -> local handler -> submit outcome；Cloud 现有
   `RemoteCommandEnvelope` 也是机械 request envelope。设计没有给 warning 的 exact route/FQCN、poll response union、strict schema、
   两仓 DTO/digest 重建与 ack 状态机，却同时声明“不经过 request/poll/outcome”。影响是文件表 #a 无法证明字段/摘要与真实 transport
   同构，#b/#c 也无可编译调用链。返修需在两种方案中明确选择并完整落地一条：扩展现有 poll response 为 typed notification
   lane，或新增独立 typed notification endpoint；两者都必须给 route/transport/DTO/schema/digest/ack 的真实方法与写集，且不能
   把 warning 伪装成输入 command。
3. **P1：metrics 的 identity 去重 API 不存在，restart 叙述自相矛盾。** HEAD
   `AutomationMetricsService.recordWindowWarning(...)`（`:367-377`）不接 identity，每次调用都会写一条
   `SYSTEM_WARNING`。设计同时说 consumed set 仅进程内且 restart 丢失，又声称重投后 metrics 可按 identity 去重；当前没有
   durable/bounded identity witness。影响是“mark 已成功、metric/ack 未完成”或 DHXY restart 后同 bytes 重投会重复 metrics，
   无法满足一次成功消费同时保持两种副作用。返修须给出真实 atomic/retained local consumption ledger：至少区分
   `UNSEEN/APPLYING/APPLIED`，保存 mark/metric 两子步骤结果并允许 exact resume；若选择让 metrics owner 本身接 identity 去重，
   列出精确 API、持久性/容量/restart 合同与最小写集。
4. **P2：canonical attributes/number 表示未定义。** `Map(reason,missingMs)` 只说有序，没有固定 key order、类型、JSON
   canonicalization 或 digest 字段顺序。返修需给严格 schema，`missingMs` 使用数值字段而非依赖任意 Map 字符串序列化，并说明
   identity digest 覆盖的逐字段顺序。

**下一任务 `W-ACP-WARN-D2` 已发布。** Worker B 必须在 `2026-07-13T02:31:07-04:00` 前先追加
`## External Worker B - CLAIMED - <timestamp>`，写明领取 `W-ACP-WARN-D2`，唯一写集仍为本日志、Java 零修改；领取后允许设计
超过 20 分钟。随后追加 `External Worker B - W-ACP-WARN-D2 Design Repair #1 Delta`，只关闭上述四点，并输出新的最小可编码
波次。M/P lifecycle adapter、N SummonSkill、A TMS、Panel 主体、host/caller/tests 继续冻结。W-ACP-1 源码批准不重开，最终
package 仍由父级统一执行。**无已批准业务差异；按基线等价迁移。**

## Parent W-ACP-1 Final Build Review - APPROVED - 2026-07-13T02:23:51-04:00

父级 fresh Cloud `mvn -q clean package` exit 0；Surefire `4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`，shaded
JAR 实际含 `AutoCombatPanelDecision` 及 missing-state nested values。结合上一轮逐分支 source review，W-ACP-1 最终结论为
**IMPLEMENTATION APPROVED，P0/P1/P2=0**。Panel 主体与 warning transport 仍按最新 Review 冻结。
**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T02:22:14-04:00

- **领取任务**：`W-ACP-WARN-D2`（`Parent Design Review #5` 发布，领取截止 02:31:07）——为 `W-ACP-WARN-D1` 的
  `Design Repair #1 Delta`，逐条关闭 Review #5 的 P1×3 + P2×1（稳定 identity owner、真实 warning transport route、可信本地
  consumption ledger、canonical schema），并输出修正后的最小可编码波次。
- **唯一写集**：**仅本固定日志**（design-only，零 Java/Maven/resources/tests，不启动运行面，无 git mutation）。
- 冻结项：M/P lifecycle adapter、N SummonSkill、A TMS、Panel 主体、host/caller/tests；W-ACP-1 源码批准不重开，最终 package
  由父级统一执行。
- 本 `CLAIMED` 仅表示 20 分钟内已接手；随后追加 `W-ACP-WARN-D2 Design Repair #1 Delta`。**无已批准业务差异；按基线等价迁移。**

## External Worker B - W-ACP-WARN-D2 Design Repair #1 Delta - 2026-07-13

逐条关闭 `Parent Design Review #5` 的 P1×3 + P2×1，输出修正后的最小可编码波次。纯设计，零 Java。真实 transport 已核：DHXY
`RemoteCommandPollingLoop:145-163` = `transport.poll → RemoteCommandPollResponse(IDLE|COMMAND) → handler.handle(command) →
transport.submitOutcome`；Cloud `RemoteCommandEnvelope` 为机械 request record。warning 无 outcome，**不能走 command/outcome 道**。

- **P1-1（稳定 identity owner）**：撤回"从 `lastAttentionAt`/wall clock 生成身份"。改为 **Cloud 侧 retained warning outbox/ledger**
  在**业务 occurrence 首次 admission 时原子保留**稳定 identity + 完整 canonical bytes。identity =
  `digest(scope,taskRunId,runRevision,windowId,source,key,attentionSequence)`，其中 `attentionSequence` 是 ledger 内
  **per-(taskRun,window) 单调计数**（非 wall clock）。Panel/Service 只提交业务 `(source,key,message,reason,missingMs)` 或
  opaque retained handle，**不自铸幂等身份**。**失败重入**：①**missing-state 更新前**（reserve 未成）→ 状态未提交，下次
  `recordMissing` 仍判 ATTENTION_REQUIRED→重试 reserve（按 occurrenceKey 幂等，不重号）；②**reserve 后、state commit 前** →
  outbox 已 durable 保留同 identity+bytes，下次同 occurrenceKey reserve 幂等命中后再 commit state，投递一次；③**publish 后
  ack 未回** → outbox `SENT` 按同 identity 重投，DHXY 去重（见 P1-3）→ 至多一次副作用。**关键**：main service 必须把
  "reserve 入 outbox" 与 "commit missing-state(lastAttentionAt 更新)" 做成**先 reserve 再 commit** 的顺序，杜绝"state 已推进
  但 warning 丢失"。
- **P1-2（真实 warning transport route）**：**选定方案 A——扩展现有 poll response 为 typed notification lane**（复用单条 long-poll，
  不新建 endpoint、不把 warning 伪装成 command）：
  - `RemoteCommandPollResponse` 增 `status` 值 `NOTIFICATION` + 可空 `RuntimeWarningNotification notification`（与 `command`
    并列，互斥）。Cloud poll 在无 command 且 outbox 有 pending 时返回 `NOTIFICATION`。
  - `RemoteCommandPollingLoop` 增分支：`status==NOTIFICATION` → `warningHandler.consume(notification)` → **`transport.ackNotification(
    RuntimeWarningAck(identity, AppliedResult))`**（**ack 道，非 submitOutcome**）→ `continue`。
  - **ack 状态机**：Cloud outbox `RESERVED → SENT → ACKED(APPLIED)`（NACK/超时→按同 identity 重 SENT，有界重投）。
  - **DTO/schema/digest**：两仓共享 strict schema（见 P2），identity digest 逐字段定长前缀（同 `CloudServiceStorage` length-prefixed
    hashing），DHXY 收端按同 schema 重建 + 校验 digest 一致才消费。**warning 不进 command/outcome 关联校验**（`validateOutcomeCorrelation`
    仅 command 道）。
  - **写集归属**：`RemoteCommandPollResponse`/polling loop/transport 的 `NOTIFICATION` lane + `ackNotification` 由 **transport/envelope
    owner（A/remote，需父级指派）** 落地；B 提供 DTO(#a)、Cloud outbox(#b)、DHXY 消费 handler(#d)。
- **P1-3（可信本地 consumption ledger；不靠 metrics identity API）**：`recordWindowWarning` 不接 identity，故去重**不放 metrics
  owner**，改为 **DHXY 侧 durable+bounded consumption ledger**（per-(taskRun,window)，terminal 清 + 容量上限）。每 identity 三态
  **`UNSEEN → APPLYING → APPLIED`**，并**分别持久化两子步骤结果** `markDone`/`metricDone`：consume 时 UNSEEN→APPLYING(persist)
  →fence(tenant/window/revision/stopEpoch，stale 丢弃不覆盖新 run UI)→`markRuntimeWarning`→persist markDone→`recordWindowWarning`
  →persist metricDone→APPLIED→ack。**exact resume**：restart 落在 APPLYING → 按 ledger 只补未完成子步骤（`markRuntimeWarning`
  幂等 set；`recordWindowWarning` 仅当 `!metricDone` 才执行）→ 杜绝"mark 成功但 metric/ack 未完成"或重启后重复 metrics。
  **一次成功消费同时保两副作用**由该 ledger 保证，非 log-only。
- **P2（canonical schema）**：`RuntimeWarningNotification` 定长有序 strict schema，字段顺序固定：
  `schemaVersion, tenantId, userId, deviceId, clientSessionId, taskRunId, runRevision, stopEpoch, windowId, nativeHandle,
  source, key, message, reason, missingMs(long), attentionSequence(long)`。`missingMs`/`attentionSequence` 为**数值字段**（非 Map
  字符串序列化）；identity digest = SHA-256 over 上述字段**按此顺序**的定长前缀编码。`attributes` 全部提升为 typed 字段
  （`reason:String`、`missingMs:long`），**取消任意 Map**。

### 修正后最小可编码波次

| 波 | 仓库路径 / FQCN | New/Modify | owner | 依赖 |
|---|---|---|---|---|
| **W-ACP-WARN-0（now）** | `dhxy-cloud-brain/.../service/RuntimeWarningNotification.java`（strict typed record）+ `RuntimeWarningIdentity`（定长前缀确定性 digest，纯类型，无 Spring/I/O/transport） | New | **B** | 叶子，可**立即**独立编译 |
| WARN-1 | `RemoteCommandPollResponse` 增 `NOTIFICATION` + `ackNotification` transport/schema/ack 状态机 | Modify | **A/remote transport owner（前置）** | 依赖 WARN-0 DTO/schema |
| WARN-2 | `dhxy-cloud-brain/.../service/RuntimeWarningOutbox.java`（retained ledger：occurrenceKey→identity+bytes+RESERVED/SENT/ACKED、per-(taskRun,window) 单调 attentionSequence、ACTIVE admission） | New | **B** | 依赖 WARN-0 + WARN-1 |
| WARN-3 | `DHXY/.../<warning>/RuntimeWarningNotificationHandler.java`（durable+bounded consumption ledger UNSEEN/APPLYING/APPLIED + fence + mark/metric 子步 + ack） | New | **B（跨仓 DHXY）** | 依赖 WARN-0 + WARN-1 收端 + 现有 `WindowRuntimeContext`/`AutomationMetricsService` |
| WARN-4 | Panel 主体 `PanelWarningSink` 在 ATTENTION_REQUIRED occurrence **先 reserve 再 commit** | New | **B（主体，冻结）** | 主体解冻后 |

- **不改** M/P lifecycle adapter、N SummonSkill、A TMS、Panel 主体、host/caller/tests；W-ACP-1 源码批准不重开，最终 package 由
  父级统一执行。**依赖顺序**：WARN-0(B,now) → WARN-1(A/remote transport) → WARN-2(B outbox) → WARN-3(B DHXY handler) →
  WARN-4(主体解冻)。
- `P0=0`；待父级复核契约、指派 WARN-1 transport owner、批准 WARN-0 立即实施时机。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。W-ACP-WARN-D2 Design Repair #1 Delta 到此停止，
等待父级 `DESIGN APPROVED` / `WARN-0 批准` / 再次 `BLOCKED`。

## Parent Design Review #6 - W-ACP-WARN-D2 BLOCKED / Repair #2 Published - 2026-07-13T02:34:00-04:00

B 已正确撤回 wall-clock identity、确认当前只有 command/outcome poll 链，并补出 typed notification 方向；但当前方案仍
**BLOCKED，P0=0 / P1=4 / P2=1**，`WARN-0` 继续禁止编码：

1. **P1：本地“两副作用 exactly-once”仍是无法闭合的双写。** `markRuntimeWarning` 或
   `recordWindowWarning` 成功后、`markDone/metricDone` 持久化前进程崩溃，restart 无法判断副作用是否已发生，仍会重复 metric；
   先写 done 又会在副作用前崩溃并永久漏记。普通三态 ledger 不能给另一个非事务资源制造 exactly-once。返修必须让
   `AutomationMetricsService` 自身提供按 notification identity 原子去重的真实 API/持久化边界，或诚实选择并记录
   at-least-once 指标语义；不得继续声称当前双写可严格去重。
2. **P1：两端“durable ledger”没有任何真实 storage owner。** 文件表只列内存 `RuntimeWarningOutbox` 与 handler，没有
   `CloudServiceStorage`/本地 state file 或数据库的 FQCN、原子 replace/fsync、启动 scan、损坏/容量/租户目录合同。当前材料
   无法支持 restart resume 的声明。返修要么列出真实持久化 owner 与最小写集，要么收缩为 process-retained 并明确 restart
   fail-closed/可能重投边界。
3. **P1：`occurrenceKey` 未定义。** D2 用它解释 reserve 幂等，却未把它放入 missing-state、schema 或 identity preimage；
   `attentionSequence` 又只在 reserve 时才分配。reserve 前失败/重复调用仍无法证明会命中同一 occurrence。返修须让
   per-taskRun missing-state 持有明确 monotonic warning occurrence/sequence，规定 reserve 与 state commit 的线性化顺序，且
   caller 不从 `now/lastAttentionAt` 自铸 identity。
4. **P1：选择“无新 endpoint”却又调用 `transport.ackNotification(...)`，路由仍断。** 当前 poll request 无 ack 字段，现有
   outcome endpoint 又只接受 command correlation。返修必须二选一并列全：新增 authenticated typed ack endpoint 的 route/
   Cloud handler/DHXY transport/error mapping，或把 exact ack piggyback 到下一 poll request；同时给 wrong tenant/session/
   taskRun/window/revision/identity 的拒绝位置。不能只写一个不存在的方法名。
5. **P2：warning fence 少了完整窗口四元组与有界公平性。** schema 仅有 windowId/nativeHandle，缺 processId/
   playerIdentityEpoch；“只有无 command 才发 notification”在连续 command 流下可永久饥饿。Repair 需补 exact window tuple、
   owner/global 容量、满载行为和不会饿死 command/notification 任一侧的有界调度规则。

**下一任务 `W-ACP-WARN-D3` 已发布。** Worker B 必须在 `2026-07-13T02:54:00-04:00` 前追加
`## External Worker B - CLAIMED - <timestamp>`；唯一写集仍为本日志、Java 零修改。领取后追加一个短
`W-ACP-WARN-D3 Design Repair #2 Delta`，只关闭以上五项，优先采用最小、诚实的 delivery 合同，不再扩写已批准 Panel/W-ACP-1。
M/P、A scale wire、N SummonSkill、Panel 主体、host/caller/tests 继续冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T02:36:38-04:00

- **领取任务**：`W-ACP-WARN-D3`（`Parent Design Review #6` 发布，截止 02:54:00）——`W-ACP-WARN-D2` 的
  `Design Repair #2 Delta`，最小诚实合同关闭 Review #6 的 P1×4 + P2×1（双写去重诚实语义、真实/收缩持久化、occurrenceKey
  入 state、ack 路由、window 四元组 + 有界公平）。
- **唯一写集**：**仅本固定日志**（design-only，零 Java/Maven/resources/tests，不启动运行面，无 git mutation）。冻结：M/P、A
  scale wire、N SummonSkill、Panel 主体、host/caller/tests；不重开 W-ACP-0/1。
- 本 `CLAIMED` 仅表示 20 分钟内已接手；随后追加 `W-ACP-WARN-D3 Design Repair #2 Delta`。**无已批准业务差异；按基线等价迁移。**

## External Worker B - W-ACP-WARN-D3 Design Repair #2 Delta - 2026-07-13

最小、诚实合同关闭 Review #6 的 P1×4 + P2×1。纯设计，零 Java。不扩写已批准 Panel/W-ACP-0/1。

- **P1-1（双写不可能 exactly-once → 诚实分级语义）**：不再声称"两副作用严格 exactly-once"。诚实定：**user-facing
  `lastMessage` = exactly-once 语义**（`markRuntimeWarning` 是幂等 set，重投覆盖为同 message，无副作用累积）；**`SYSTEM_WARNING`
  metric = at-least-once**（`recordWindowWarning` 无 identity API、每次调用写一条；transport 重投/restart 下**罕见重复**，属可接受
  的诊断冗余）。**不**用普通三态 ledger 假装给非事务 metrics 制造 exactly-once。（可选升级：若 metrics owner 未来提供按
  identity 原子去重 API，则升为 exactly-once——列为 owner 前置，非本切片强求。）
- **P1-2（持久化无真实 owner → 收缩为 process-retained + fail-closed）**：撤回"durable ledger"。两端 outbox/consumed-set
  **均为 process-retained（内存，有界）**，**不虚构** `CloudServiceStorage`/state file/DB owner。**restart 边界明确**：Cloud 进程
  重启 → 未 ack 的 outbox 条目丢失 → 该次 warning **可能漏投（fail-closed，warning 属诊断非关键，可接受漏一次）**；DHXY 进程
  重启 → consumed-set 丢失 → Cloud 未 ack 条目按同 identity 重投 → **at-least-once**（message 幂等、metric 罕见重复，同 P1-1）。
  **不**声称跨 restart resume。
- **P1-3（occurrenceKey 入 state + 线性化）**：`warningOccurrence` 由 **Panel 主体 per-taskRun state 持有一个单调
  `warningOccurrenceSeq`**（`long`，随每次 `recordMissing` 返回 `ATTENTION_REQUIRED` 递增；**注意不改已冻结 W-ACP-1 纯类**，seq
  在主体 state，非纯判定）。identity preimage 与 schema **均含** `warningOccurrenceSeq`。**线性化（per-taskRun 锁内）**：
  `seq++ → outbox.reserve(occurrenceKey=(taskRun,window,seq), identity, canonical bytes) → commit missing-state`；reserve 前崩溃
  → seq 未推进、state 未提交 → 下次同判定重来命中同 seq（幂等）；caller **绝不**从 `now/lastAttentionAt` 自铸 identity。
- **P1-4（ack 路由：piggyback 到 poll request，无新 endpoint）**：撤回不存在的 `ackNotification`。**选定：把 ack 搭载到下一个
  poll request**——`RemoteCommandPollRequest` 增 `List<RuntimeWarningAck> acks`（每项 `identity + AppliedResult`）；Cloud poll
  handler 在返回下一 response 前先消费 acks：校验 `(tenant,user,device,clientSession,taskRunId,runRevision,windowTuple,identity)`
  匹配 outbox 条目才 `RESERVED/SENT→ACKED`，**任一不匹配即在 handler 处拒绝并丢弃该 ack**（不影响 command 道，不新增 endpoint、
  不碰 outcome correlation）。route/写集：`RemoteCommandPollRequest`(+acks)/poll handler/DHXY poll transport 归 **A/remote
  transport owner（前置）**。
- **P2（window 四元组 + 有界公平）**：schema 的 window 用**完整四元组** `(windowId, nativeHandle, processId,
  playerIdentityEpoch)`（与 `CloudTaskServiceExecutionContext.window()` 一致），fence 逐项比对当前 run。**有界公平调度**：poll
  response 每次**最多 1 command 或 1 notification**，但保证 notification 每 `K` 次 poll 至少有一个投递槽（即使 command 连续流也
  不饿死 notification；反之亦然）；**outbox 有界**（per-taskRun 上限 `N`，满载 drop-oldest 并计数日志），无全局无界增长。

### 修正后最小可编码波次（更新）

| 波 | 路径/FQCN | owner | 语义 |
|---|---|---|---|
| **WARN-0（now）** | `service/RuntimeWarningNotification.java`（strict schema 含 `warningOccurrenceSeq` + 四元组 window）+ `RuntimeWarningIdentity`（定长前缀 digest） | **B** | 纯类型，可立即独立编译 |
| WARN-1 | `RemoteCommandPollRequest(+acks)` + poll handler ack 消费 + DHXY poll transport | **A/remote（前置）** | ack piggyback，无新 endpoint |
| WARN-2 | `service/RuntimeWarningOutbox.java`（**process-retained** 有界 + 单调 seq + ACTIVE admission + 公平槽） | **B** | 无持久化，restart fail-closed |
| WARN-3 | `DHXY/.../RuntimeWarningNotificationHandler.java`（process-retained consumed-set + fence 四元组 + mark(exactly-once)/metric(at-least-once) + piggyback ack） | **B（跨仓）** | 诚实分级语义 |
| WARN-4 | Panel 主体：per-taskRun `warningOccurrenceSeq` + 先 reserve 再 commit | **B（主体，冻结）** | 解冻后 |

`P0=0`；诚实合同：`lastMessage` exactly-once、`SYSTEM_WARNING` at-least-once、restart fail-closed（可能漏/可能重、message 幂等）、
process-retained 无虚构 storage、ack piggyback 无新 endpoint。待父级复核 + 指派 WARN-1 owner + 批准 WARN-0 编码时机。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。W-ACP-WARN-D3 Design Repair #2 Delta 到此停止，
等待父级 `DESIGN APPROVED` / `WARN-0 批准` / 再次 `BLOCKED`。

## Parent Design Review #7 - W-ACP-WARN-D3 BLOCKED / Repair #3 Published - 2026-07-13T02:40:00-04:00

结论：**BLOCKED，P0=0 / P1=4 / P2=1**。本轮认可两项收缩方向：不再虚构 durable storage；明确 metric 只能是
at-least-once。完整 window 四元组也正确。但当前合同仍不能落到现有 poll wire，`WARN-0` 继续冻结。

1. **P1：通知没有可达的 response wire。** 真实 DHXY
   `RemoteCommandPollResponse.java:10-13` 只有 `status/retryAfterMs/command`，`RemoteCommandPollStatus.java:3-5` 只有
   `IDLE/COMMAND`；Cloud `RemoteCommandPollResponse.java:3-25` 同样强制 `IDLE/COMMAND` 二选一。D3 只列
   `RemoteCommandPollRequest(+acks)`，却声称 response 可返回 notification，没有列 response DTO/status、Cloud route/broker、
   DHXY transport strict validation 和 poll-loop dispatch 的精确写集。**影响：** notification 无法序列化或被本地消费。
   **返修：** 给出一种唯一 typed response shape（推荐显式 `NOTIFICATION` variant，且 command/notification 互斥），列全双仓
   FQCN、strict validation、digest/identity correlation 和 handler 分支；不得把 notification 塞进 raw map/string。
2. **P1：ACK 的 retain/重投/settle 状态机缺失。** 真实
   `RemoteCommandPollingLoop.java:18,146` 保存并重复发送同一个 immutable `pollRequest`，没有 pending-ack owner。D3 又规定
   Cloud 对不匹配 ack “丢弃但继续 poll”，response 不返回逐项 accepted/rejected，因此 DHXY 收到 HTTP 200 后不知道哪些 ack
   可以删除。**影响：** ack 可能提前丢失、永久重投或污染下一 session。**返修：** 明确 package-private bounded pending-ack
   owner、stable ack bytes/identity、每轮 poll request snapshot、HTTP/JSON failure 后保留、Cloud 幂等 settle、DHXY 仅凭明确
   ack receipt 才删除；或把整批 ack 处理改为原子 all-or-nothing 并在 response 返回 exact accepted batch digest。列出当前固定
   request 如何改为 per-poll immutable snapshot，不能引入 public raw queue。
3. **P1：occurrence sequence 的线性化自相矛盾。** D3 写 `seq++ -> reserve -> commit`，又声称 reserve 前崩溃时 seq 未推进；
   但 seq 被定义为 Panel 主体 state 字段，`seq++` 已是状态变化，且 reserve 失败/容量拒绝时没有 exact rollback。**影响：**
   同一业务 occurrence 可跳号、换 identity，或 state 与 outbox 不一致。**返修：** 定义未提交 candidate（如
   `candidateSeq=committedSeq+1`）与一个原子 commit point：reserve 成功后在同一 owner/permit 内同时提交 committed seq 和
   warning state；reserve 失败不得推进；禁止 caller 自行 rollback。说明 restart 后 process-retained state/outbox 同时消失。
4. **P1：有界合同仍是占位符且只有 per-run 预算。** `K/N` 未给定，缺 tenant/global hard cap；`drop-oldest` 会删除未 ack
   occurrence，和“exactly-once”表述冲突，也未定义被删除 entry 的 ack 行为。**影响：** 多 taskRun 可造成进程级无界内存，
   或静默漏掉已承诺交付的 warning。**返修：** 给出 exact per-run/tenant/global constants、原子 admission/accounting、满载
   policy、fairness cursor 与退休规则；若选择丢弃，必须诚实定义为 best-effort/idempotent-effect，而不是 exactly-once。
5. **P2：`markRuntimeWarning` 是幂等 set，不等于 exactly-once delivery。** 真实
   `WindowRuntimeContext.java:2048` 只是覆盖 `lastMessage`；重复调用可得到相同最终值，但调用次数并非 exactly once。
   **返修：** 统一术语为“idempotent final-state effect”；metric 为 at-least-once；process restart/drop 满载下 notification 为
   best-effort，并把允许的漏投/重复边界写进合同。

新任务为 **`W-ACP-WARN-D4`（Design Repair #3 Delta）**。Worker B 必须先在本日志追加
`External Worker B - CLAIMED`（任务标题、领取时间、唯一写集）；领取截止
**2026-07-13T03:00:00-04:00**，20 分钟只检查领取，已领取后可继续工作超过 20 分钟。仍只改本日志，Java/resources/tests/
schema 冻结；不重开 W-ACP-0/1，不碰 A scale-wire、P Full R0、N SummonSkill 或 host/caller。交付后等待父级复审。
**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T02:50:48-04:00

- **领取任务**：`W-ACP-WARN-D4`（`Parent Design Review #7` 发布，`Design Repair #3 Delta`，领取截止 03:00:00）——关闭
  Review #7 的 P1×4 + P2×1（NOTIFICATION response variant、ack 状态机、occurrence candidate/commit、有界 exact 常量、术语诚实）。
- **唯一写集**：**仅本固定日志**（design-only，零 Java/resources/tests/schema，不启动运行面，无 git mutation）。冻结：W-ACP-0/1、
  A scale-wire、P Full R0、N SummonSkill、host/caller。
- 本 `CLAIMED` 仅表示 20 分钟内已接手；随后追加 `W-ACP-WARN-D4 Design Repair #3 Delta`。**无已批准业务差异；按基线等价迁移。**

## External Worker B - W-ACP-WARN-D4 Design Repair #3 Delta - 2026-07-13

关闭 Review #7 的 P1×4 + P2×1。纯设计，零 Java。**全局定性：warning notification = best-effort、idempotent-final-state
effect，非 exactly-once**（下述合同统一此口径）。

- **P1-1（NOTIFICATION response variant，双仓 FQCN + 校验 + dispatch）**：给唯一 typed response shape——两仓
  `RemoteCommandPollResponse` 各增 `status` 枚举值 `NOTIFICATION`（`RemoteCommandPollStatus` 增 `NOTIFICATION`，与 `IDLE/COMMAND`
  互斥）+ 可空字段 `RuntimeWarningNotification notification`（与 `command` **互斥**，`status==NOTIFICATION` 时非空、其余为空）。
  - Cloud：`.../remote/RemoteCommandPollResponse`（+`notification`/`NOTIFICATION`）+ poll handler/broker 在无 command 且 outbox
    有 pending 且公平槽命中时构造 `NOTIFICATION` response。
  - DHXY：`cloud/remote/RemoteCommandPollResponse`+`RemoteCommandPollStatus`（+`NOTIFICATION`/`notification`）；
    `RemoteCommandPollingLoop` 在 `status==NOTIFICATION` 分支：**strict validation**（schema 版本、四元组 fence、必填字段、
    `RuntimeWarningIdentity` digest 与 payload 一致性 correlation）→ `warningHandler.consume` → 记入本轮待发 ack。校验失败 → 丢弃
    该 notification（不消费、不 ack），不塞 raw map/string。owner：response DTO/status/handler dispatch 归 **A/remote transport
    owner（前置）**；B 提供 `RuntimeWarningNotification`/`RuntimeWarningIdentity`（WARN-0）。
- **P1-2（ack retain/resend/settle 状态机）**：DHXY 侧新增 **package-private bounded pending-ack owner** `RuntimeWarningAckBook`
  （非 public queue）：持 `identity→RuntimeWarningAck(identity, AppliedResult, stable bytes)`。**每轮 poll**：`pollRequest` 不再是固定
  immutable 单例，而是**每 poll 重建的 immutable snapshot** = 基础 `pollRequest` 字段 + `List<RuntimeWarningAck> acks`（当前 ack-book
  快照，有界）。Cloud poll handler **幂等 settle**：对匹配 outbox 的 ack `→ACKED`（重复 settle 无副作用），并在 **response 返回
  `acceptedAckDigest`（本批已受理 identity 的定长有序 digest）**。DHXY **仅当某 ack.identity 出现在 `acceptedAckDigest`** 才从
  ack-book 删除；HTTP/JSON 失败或未被受理 → **保留重投**（下轮 snapshot 再带）。不匹配 ack（错 tenant/session/taskRun/window/
  revision/identity）→ Cloud 在 handler 处**拒绝、不进 acceptedAckDigest**、DHXY 据此不删（最终 ack-book 有界满 → 见 P1-4）。
- **P1-3（occurrence candidate/atomic commit）**：Panel 主体 per-taskRun state 持 `committedWarningSeq`（`long`）。每次
  `ATTENTION_REQUIRED`：取 **candidate `candidateSeq = committedWarningSeq + 1`（未提交、非状态变更）** → 在 outbox owner/permit
  内 `reserve(occurrenceKey=(taskRun,window,candidateSeq), identity, bytes)`。**唯一原子 commit point**：reserve 成功 → 在**同一
  owner/permit 内同时** `committedWarningSeq = candidateSeq` **且**提交 warning missing-state（W-ACP-1 nextState）。**reserve 失败/
  容量拒绝 → 不推进 committedWarningSeq、不提交 state**（下次同判定重取同 candidate，幂等，不跳号）；**禁止 caller 自行 rollback**。
  **restart：process-retained state 与 outbox 一并消失**（一致，无残留半提交）。
- **P1-4（有界 exact 常量 + admission，不 drop 已承诺 exactly-once）**：定 exact 常量：**per-run pending ≤ 64、per-tenant ≤ 512、
  process-global ≤ 4096**；ack-book per-run ≤ 64。**原子 admission/accounting**（outbox owner 单锁）：reserve/ack 入账前查三级上限，
  **满载 → 拒绝 admission（reserve 返回 REJECTED），该次 warning best-effort 放弃并计数日志**（**不 drop 已入账未 ack 的旧
  occurrence**，避免"承诺后静默漏"）。fairness：per-tenant round-robin cursor 选下一个有 pending 的 taskRun 出一个 notification 槽，
  command 每轮优先但 notification 每 `K=4` 轮保底一槽（command/notification 皆不饿死）。retirement：taskRun terminal 清该 run 的
  outbox+ack-book。**丢弃语义诚实**：admission 拒绝 = best-effort 漏投，非 exactly-once。
- **P2（术语诚实）**：统一——`lastMessage` 为 **idempotent final-state effect**（`markRuntimeWarning` 覆盖，调用次数非 once）；
  `SYSTEM_WARNING` metric 为 **at-least-once**；notification 整体 **best-effort**。**允许边界写入合同**：Cloud 重启未 ack 条目、
  admission 满载拒绝 → **允许漏投**；transport 重投 / DHXY 重启 consumed-set 丢失 → **允许 metric 重复**（message 幂等）。无
  exactly-once 声称。

### 最小可编码波次（不变，语义收敛）

WARN-0（B，now，纯 `RuntimeWarningNotification` strict schema + `RuntimeWarningIdentity` digest）→ WARN-1（A/remote：
`RemoteCommandPollStatus.NOTIFICATION` + response `notification` + `RemoteCommandPollRequest.acks` + `acceptedAckDigest` + poll
handler/loop dispatch）→ WARN-2（B `RuntimeWarningOutbox` process-retained + 三级 admission + fairness cursor + candidate/commit）→
WARN-3（B 跨仓 DHXY handler + `RuntimeWarningAckBook` + strict validation + idempotent-final mark / at-least-once metric）→
WARN-4（主体 `committedWarningSeq` + 原子 commit，解冻后）。`P0=0`。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。W-ACP-WARN-D4 Design Repair #3 Delta 到此停止，
等待父级 `DESIGN APPROVED` / `WARN-0 批准` / 再次 `BLOCKED`。

## Parent Design Review #8 - WARN-0 DESIGN APPROVED / Transport BLOCKED / D5 Published - 2026-07-13T03:02:43-04:00

D4 已正确补齐 typed `NOTIFICATION` response 方向、process-retained 诚实边界、完整窗口四元组与三级容量，并统一为
best-effort / idempotent-final-state / metric at-least-once。父级把可独立编译的纯类型叶子与后续 transport/state 分开判断：

### `W-ACP-WARN-0`：DESIGN APPROVED，P0/P1/P2=0

允许 B 新建 Cloud 两个纯类型文件：

1. `src/main/java/com/bot/dhxy/service/RuntimeWarningNotification.java`
2. `src/main/java/com/bot/dhxy/service/RuntimeWarningIdentity.java`

字段必须严格固定为 `schemaVersion, tenantId, userId, deviceId, clientSessionId, taskRunId, runRevision, stopEpoch,
windowId, nativeHandle, processId, playerIdentityEpoch, source, key, message, reason, missingMs, warningOccurrenceSeq,
identity`；字符串 non-null/non-blank，revision/epoch/millis/sequence non-negative，identity 由除 identity 自身外的字段按固定
length-prefixed UTF-8 + fixed-width long 顺序 SHA-256 计算并在构造时复核。类型必须 immutable、无 Spring/I/O/clock/random/
transport/outbox/host/caller，不引用 raw request/poll/outcome，不新增 tests。该叶子不授权 WARN-1 以后任何 wiring。

### WARN-1..4：BLOCKED，P0=0 / P1=4 / P2=2

1. **P1：`acceptedAckDigest` 是不可枚举的 opaque digest。** D4 同时要求“仅当某 `ack.identity` 出现在
   `acceptedAckDigest` 才删除”，但 digest 中不存在可查询成员。影响是 DHXY 无法知道本批哪些 ACK 已 settle，可能过早删除
   或永久重投。返修二选一：返回 bounded exact accepted identity receipt list；或 Cloud 对 snapshot 严格 all-or-nothing，
   response 返回整个 submitted snapshot 的 digest，DHXY 只在 digest 全等时整批删除。禁止部分接受却只回一个总 digest。
2. **P1：warning lane 容量不能改变 W-ACP-1 business state。** HEAD
   `AutoCombatPanelService.java:210` 先推进 `lastAutoPanelMissingAttentionAt` 再做本地 warning；已批准
   `AutoCombatPanelDecision.java:65-67` 也无条件产出该 nextState。D4 的“reserve 失败不提交 state”会让满载 warning outbox
   改变 60 秒 suppression/重复节奏。返修必须把两者分开：W-ACP-1 `nextState` 按 HEAD 无条件提交；warning occurrence/seq
   仅在 reserve 成功时提交，admission 拒绝就是诚实 best-effort 漏投，不能回滚或阻塞业务 missing-state。
3. **P1：poll route 与公平 cursor 的 scope 不一致。** 现有 `RemoteCommandPollRequest#scopeHint()` 和 broker queue 都按
   exact `(tenantId,userId,deviceId,clientSessionId)` 路由；D4 只给 per-tenant round-robin，可能为当前 session 选择另一
   device/session 的 taskRun。notification eligibility、cursor 与 wake signal 必须绑定 exact `RemoteClientScope`；tenant/global
   只做额外 accounting，response 再逐项校验 taskRun/window/revision。
4. **P1：terminal cleanup 跨进程越权。** Cloud taskRun terminal 只能清 Cloud outbox，不能直接清 DHXY
   `RuntimeWarningAckBook`。若 outbox 已不存在或 ACK 永久 rejected，本地 ACK 会重投至 64 cap。返修必须给本地 exact
   registration-terminal cleanup，或由 Cloud 返回明确 bounded rejected/retired receipts；旧 run receipt 不得清新 generation。
5. **P2：notification 没有唤醒现有 long-poll 的 owner。** 当前 Cloud broker 在
   `RemoteGameCommandBroker.java:196-198` 阻塞于 command `BlockingQueue.poll(timeout)`；D4 没说明 outbox admission 如何唤醒
   该 exact route，K=4 也因此不是可执行保证。返修要么把 notification 作为同 route 的 typed queue item，要么给 route-local
   bounded signal；不得新建 thread/poller/timer。
6. **P2：`K=4` 的计数/重置语义不明确。** 必须定义按 exact route 的 successful response opportunity 计数，command 与
   notification ready 时如何选择、何时重置，以及 IDLE/校验拒绝是否计数；保证任一 ready lane 的最大等待轮数有明确上界。

### 下一任务：`W-ACP-WARN-D5 + WARN-0-IMP1`

External Worker B 必须在 **2026-07-13T03:22:43-04:00** 前追加
`## External Worker B - CLAIMED - <timestamp>`，注明唯一写集为上述 **Cloud 2 New + 本固定日志 append-only**；20 分钟只检查
领取，领取后可工作超过 20 分钟。先实施 `WARN-0` 两个纯类型叶子，再在同一日志追加一个短 `D5 Design Repair #4 Delta`
只关闭上述 6 项。WARN-1..4、两仓 remote transport、Panel 主体、A scale-wire、P Full R0、R SummonSkill、host/caller/schema/
resources/tests 全部冻结。Worker 自审不构成批准；父级将对两文件源码与 D5 分别复审，并在所有并发 Java 稳定后统一跑 Cloud
`mvn -q clean package`。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T03:14:55-04:00

- **领取任务**：`W-ACP-WARN-D5 + WARN-0-IMP1`（`Parent Design Review #8` 发布，截止 03:22:43）——先实施获批的 `WARN-0`
  两个纯类型叶子，再追加 `D5 Design Repair #4 Delta` 关闭 WARN-1..4 的 6 点（P1×4+P2×2）。
- **唯一写集**：**Cloud 2 New** = `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/RuntimeWarningNotification.java` +
  `.../service/RuntimeWarningIdentity.java`；**+ 本固定日志 append-only**。Java 其余/resources/tests/schema、WARN-1..4、两仓
  remote transport、Panel 主体、A scale-wire、P Full R0、R/N SummonSkill、host/caller 全部冻结。
- 本 `CLAIMED` 仅表示 20 分钟内已接手；随后追加 `WARN-0-IMP1` 证据 + `D5 Design Repair #4 Delta`。**无已批准业务差异；按
  基线等价迁移。**

## External Worker B - WARN-0-IMP1 Implementation #1 - 2026-07-13

按 `Parent Design Review #8` 的 `W-ACP-WARN-0` DESIGN APPROVED 实施。**Cloud 2 New / 0 Modify，DHXY 0**；纯类型，无
Spring/I/O/clock/random/transport/outbox/host/caller，不引用 raw request/poll/outcome，不新增 tests；不触碰 WARN-1+ wiring。

### 落盘（2 New）

| 文件 | SHA-256(16) | 行 |
|---|---|---|
| `dhxy-cloud-brain/.../service/RuntimeWarningIdentity.java`（确定性 digest：定长前缀 UTF-8 + 定宽 long/int，固定字段序） | `BAFEF951BD286ED7` | 87 |
| `dhxy-cloud-brain/.../service/RuntimeWarningNotification.java`（strict schema record + 构造期校验 + identity 复核） | `5B516A63068CC054` | 94 |

- 字段严格固定序：`schemaVersion, tenantId, userId, deviceId, clientSessionId, taskRunId, runRevision, stopEpoch, windowId,
  nativeHandle, processId, playerIdentityEpoch, source, key, message, reason, missingMs, warningOccurrenceSeq, identity`。
- 校验：string non-null/non-blank；`runRevision/stopEpoch/processId/playerIdentityEpoch/missingMs/warningOccurrenceSeq`
  non-negative；`schemaVersion` 正；`identity` = 除自身外全字段的 SHA-256（定长前缀）**构造期复核相等**，不符即拒。
- digest preimage 逐字段定序、string 4-byte 长前缀 + UTF-8、int 4-byte BE、long 8-byte BE——字节确定、跨 run 同字段同 identity。

### 构建证据

- **非破坏性**独立 `javac -d <scratch> RuntimeWarningIdentity.java RuntimeWarningNotification.java`：**exit 0**（仅依赖
  `java.security`/`java.nio`，可独立编译；**刻意不跑 `mvn clean package`**——按父级"所有并发 Java 稳定后父级统一跑"）。
  `git status` 仅这 2 个 `??` 新文件，无其它变动。未启动运行面、未 git add/commit。

`WARN-0-IMP1 自审仅 QA，不构成批准。` 等父级源码复审 + 统一 fresh package。

## External Worker B - W-ACP-WARN-D5 Design Repair #4 Delta - 2026-07-13

关闭 Review #8 WARN-1..4 的 P1×4 + P2×2。纯设计，零 Java（WARN-0 已单独落盘）。合同口径不变：best-effort /
idempotent-final-state / metric at-least-once。

- **P1-1（`acceptedAckDigest` 不可枚举 → all-or-nothing 快照 digest）**：选 **Cloud 对 submitted ack snapshot 严格
  all-or-nothing settle**；response 返回**整个 submitted snapshot 的 digest**（定长有序 over 该批 identity）。DHXY **仅当返回
  digest 与本地提交 snapshot 的 digest 全等时，整批删除该 snapshot 的 ack**；不等/HTTP/JSON 失败 → 整批保留重投。**不做部分
  接受**（杜绝"部分接受只回一个总 digest"歧义）。
- **P1-2（warning 容量不得改 W-ACP-1 business state）**：**彻底解耦**。W-ACP-1 `recordMissing` 的 `nextState`（含
  `lastAttentionAt` 推进）**按 HEAD 无条件提交**，60s suppression/重复节奏零改变；warning 投递是**旁路**：ATTENTION_REQUIRED
  时**另**取 `warningOccurrenceSeq` 尝试 outbox reserve——**reserve 成功才提交 seq**，**admission 拒绝 = 诚实 best-effort 漏投**，
  **绝不**回滚/阻塞/改变 missing-state。（即：business state 永远前进，warning 可丢。）
- **P1-3（route/cursor scope 一致）**：notification eligibility、fairness cursor、wake **全部绑定 exact
  `RemoteClientScope(tenantId,userId,deviceId,clientSessionId)`**（= 现有 `RemoteCommandPollRequest#scopeHint()`/broker queue 路由键）；
  tenant/global 上限**仅做额外 accounting**，**不**跨 session/device 选 taskRun。response 再**逐项**校验 `taskRunId/window 四元组/
  runRevision`，不符即拒不投。
- **P1-4（terminal cleanup 不越权）**：**本地 exact registration-terminal cleanup**——DHXY 在**自己** run terminal 时清自己的
  `RuntimeWarningAckBook`（带 generation 校验，**旧 run receipt 绝不清新 generation**）；跨进程侧由 **Cloud 在 response 返回
  bounded retired/rejected receipts**，DHXY 据此丢弃对应（已退休/永久 rejected）ack，避免重投至 64 cap。Cloud **只**清 Cloud
  outbox，不直接碰 DHXY ack-book。
- **P2-1（唤醒现有 long-poll，不新建 thread/poller）**：notification 作为**同 route 的 typed queue item**——`RemoteGameCommandBroker`
  的 route 队列元素改为 union `RemoteRouteItem = COMMAND(RemoteGameCommand) | NOTIFICATION(RuntimeWarningNotification)`；outbox
  admission = **向该 exact route 队列 enqueue 一个 NOTIFICATION item**，自然唤醒既有 `BlockingQueue.poll(timeout)`（`:196-198`）；
  poll-loop 按 item type dispatch。**不新建 thread/poller/timer**（transport union/enqueue 归 A/remote owner，前置）。
- **P2-2（`K` 计数/重置语义）**：per-exact-route 计数 = **自某 lane 上次 delivery 起、该 route 的成功 response opportunity 数**
  （成功 = 非 IDLE 且非 validation-reject；IDLE/校验拒绝**不计**）。command/notification 均 ready 时交替出队，但**任一 ready lane
  最多等 `K=4` 个成功 opportunity 必得一次 delivery**（有明确上界，不饿死）；该 lane delivery 后其计数 reset。

### 波次（语义收敛，写集归属明确）

WARN-0（B，**已落盘**）→ WARN-1（A/remote：`RemoteCommandPollStatus.NOTIFICATION` + response `notification` + request `acks` +
all-or-nothing `submittedAckDigest`/回执 + route union `RemoteRouteItem` + enqueue 唤醒 + retired/rejected receipts）→ WARN-2
（B `RuntimeWarningOutbox` process-retained + exact-scope eligibility + 三级 accounting + K 公平 cursor + reserve-成功才提交 seq）→
WARN-3（B 跨仓 DHXY handler + `RuntimeWarningAckBook`(generation) + strict validation + idempotent-final mark / at-least-once
metric + all-or-nothing ack 删除 + 本地 terminal cleanup）→ WARN-4（主体：`warningOccurrenceSeq` 旁路，business state 无条件前进）。
`P0=0`。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。WARN-0-IMP1 与 W-ACP-WARN-D5 Design Repair #4 Delta
到此停止，等待父级源码复审 / `DESIGN APPROVED` / 再次 `BLOCKED`。

## Parent Review #9 - WARN-0 FINAL APPROVED / D5 BLOCKED / D6 Published - 2026-07-13T04:18:00-04:00

### `W-ACP-WARN-0` 源码结论：FINAL APPROVED

父级逐行审查 `RuntimeWarningIdentity` 与 `RuntimeWarningNotification`：字段顺序、non-blank/non-negative 校验、
固定 4-byte UTF-8 长度前缀、big-endian int/long、SHA-256 identity 构造期复核均符合 Review #8；类型无 Spring/I/O/
clock/random/transport/outbox/host/caller。父级 fresh Cloud `mvn -q clean package` exit 0，Surefire
`4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`，shaded JAR 已包含两类型。结论：
**IMPLEMENTATION APPROVED，P0/P1/P2=0**。WARN-0 到此收口，不因后续 transport 设计返修而回退。

### `W-ACP-WARN-D5` 结论：BLOCKED，P0=0 / P1=2 / P2=1

1. **P1：all-or-nothing ACK snapshot 可被一个永久不可接受 ACK 毒化。** D5 同时要求整批 digest 全等才删除，又允许
   retired/rejected receipt；但没有定义当同一 snapshot 含一个已退休 ACK 与多个仍有效 ACK 时，Cloud 的原子判定、response
   closed union 与 DHXY 删除顺序。影响是整批可能永久保留并占满本地 64 cap，或在混合 receipt/digest 下误删尚未 settle 的
   ACK。返修必须给唯一结果：`ACCEPTED_WHOLE(submittedSnapshotDigest)` 或
   `REJECTED_WHOLE(exact bounded retired/rejected identity receipts)`；DHXY 只在 generation/exact snapshot 校验后整批删除，
   或先只删除明确永久 receipt 后以新 snapshot 重投余项。禁止一份 response 同时暗含部分 settle 与整批 accepted digest。
2. **P1：outbox entry 与 route queue item 是两个 owner，未闭合重复/丢失。** D5 让 reserve 同时写 outbox 并 enqueue
   `NOTIFICATION` item，但未定义 timeout、validation reject、poll dequeue 后未 ACK、重复 wake/retry 时 queue item 的稳定
   identity、重入与再发布。影响是 outbox 仍 pending 但唯一 item 已丢，或同 identity 堆积多个 queue item并重复占容量。
   返修必须让 outbox 成为唯一 notification 真值；route 只保存 bounded/coalesced wake signal，或保存与 outbox exact generation
   一一对应且可 CAS 的稳定 slot，不能复制第二份投递账本。
3. **P2：单一 FIFO union queue 无法实现声明的 `K=4` 双 lane fairness。** 当前源码
   `RemoteGameCommandBroker.java:183-198` 是 `BlockingQueue<PendingCommand>` FIFO + `queue.poll(...)`；把元素改成 union
   后，排在大量 command 后面的 notification 仍不可被 scheduler 选择，反之亦然。返修应定义 exact-route
   `command lane + notification owner + coalesced wake signal`（或等价可选择两 lane 的固定状态），poll 被唤醒后在持锁线性化点
   根据 per-route successful-opportunity counter 选择 lane；IDLE/reject 不计数，不新增 thread/poller/timer。

### 下一任务：`W-ACP-WARN-D6`（Design Repair #5 Delta）

External Worker B 必须在 `2026-07-13T04:38:00-04:00` 前追加
`## External Worker B - CLAIMED - <timestamp>`，写明任务标题、领取时间、唯一写集为**仅本固定日志**。20 分钟只检查领取；
领取后可工作超过 20 分钟。只关闭上述 P1×2/P2×1，Java/resources/tests/schema 全冻结；不得重开 WARN-0、W-ACP-0/1，
不得修改 A scale-wire、P Full R0、R SummonSkill 或 host/caller。交付后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T04:26:21-04:00

- **任务**：`W-ACP-WARN-D6`（Design Repair #5 Delta）——闭合 Review #9 的 P1×2 / P2×1。
- **领取时间**：2026-07-13T04:26:21-04:00（父级截止 04:38:00，在窗内）。
- **唯一写集**：仅本固定日志 `docs/superpowers/plans/reports/2026-07-12-cloud-auto-combat-panel-service-worker-b.md`。设计-only。
- **冻结遵守**：不重开 WARN-0（已 FINAL APPROVED）、W-ACP-0/1；不动 Java/resources/tests/schema、A scale-wire、P Full R0、R SummonSkill、host/caller。不跑构建、不改 git。

## External Worker B - W-ACP-WARN-D6 Design Repair #5 Delta - 2026-07-13

本 Delta 只改设计文字，闭合 Review #9 三点。基线：Review #8 已批的 best-effort/at-least-once 契约、`lastMessage` 幂等终态、
`SYSTEM_WARNING` 至少一次、process-retained bounded state、SHA-256 identity（固定序 length-prefixed，非时钟派生）——均不变。
以下为对被 BLOCK 的 D5 三点的**唯一收敛裁定**。

### P1-1 闭合：ACK response 为 closed union，禁止部分-settle 与整批-accepted 并存

**根因**：D5 允许「整批 digest 全等才删除」与「容忍 retired/rejected receipt」同时成立，混合 snapshot（1 个永久不可接受 ACK
+ N 个仍有效 ACK）下 Cloud 原子判定 / response / DHXY 删除顺序未定义 → 整批永久滞留占满本地 64 cap，或误删未 settle 项。

**裁定（唯一结果，二选一，互斥）**。Cloud 对一次提交的 ack-snapshot（DHXY 侧 generation `G` + `submittedSnapshotDigest`
= 对该 snapshot 全部 ack-identity 按固定序做的 SHA-256）在**单个持锁线性化点**产出且仅产出下述之一：

1. `ACCEPTED_WHOLE(submittedSnapshotDigest)` — Cloud 已把该 snapshot 的**每一条** ack 都成功落到 outbox 的终态推进
   （对应 notification 转 `ACKED`）。回带原样 `submittedSnapshotDigest`。
2. `REJECTED_WHOLE(retiredReceipts)` — `retiredReceipts` 为**精确、bounded** 的永久不可接受 ack-identity 列表
   （retired/rejected，即对应 outbox entry 已不存在或已被 Cloud 终结、DHXY 再投也永不会被接受者）。**不含**任何仍可 settle 的 identity。

**禁止**：一份 response 同时给「部分 settle」与「整批 accepted digest」，或给一个混合列表暗示"这些删、那些留但我也认了整批"。

**DHXY 侧删除顺序（严格）**：
- 收到 `ACCEPTED_WHOLE`：先校验回带 digest == 本地对当前 generation `G` 该 snapshot 重算的 digest，且 `G` 未被本地新 reserve 推进
  （exact snapshot 校验）。通过才**整批删除**该 snapshot 覆盖的 ack-book 条目；不通过（generation 漂移/digest 不符）则整批保留，下轮重投。
- 收到 `REJECTED_WHOLE`：**只删除** `retiredReceipts` 精确命名的那几条永久条目；其余条目**原样保留**，并作为**新 snapshot**（新 generation `G+1`、
  新 `submittedSnapshotDigest`）在下次 poll 重投。由此一个永久毒 ACK 被 `REJECTED_WHOLE` 点名摘除后，余项以干净 snapshot 前进，
  不再毒化整批、不占死本地 cap。

这样两条路径都不出现「混合 settle」：`ACCEPTED_WHOLE` 是整批终结，`REJECTED_WHOLE` 是「只摘永久项 + 余项换新 snapshot 重投」。

### P1-2 闭合：outbox 为 notification 唯一真值，route 只持 coalesced wake，不复制第二份投递账本

**根因**：D5 让 reserve 同时写 outbox **并** enqueue 一个 `NOTIFICATION` queue item，形成两个 owner；timeout/reject/poll-dequeue-未-ACK/
重复 wake 下未定义 queue item 稳定 identity 与重入 → outbox 仍 pending 但唯一 item 丢失，或同 identity 多个 item 堆积占容量。

**裁定**：**outbox 是 notification 的唯一投递账本（single source of truth）**。route 侧**不再保存任何 notification 副本**，只保存一个
**per-route coalesced wake signal**——一个幂等布尔/单 token（"本 route 的 outbox 尚有 pending notification"），多次 reserve/重投/重复 wake
只把它置位一次（coalesce），永不堆叠、无独立 identity、不承载 payload。

- **outbox entry 生命周期（唯一账本）**：`RESERVED`（占 occurrenceSeq、写入 identity）→ `SENT`（已随 poll-response 发出）→ `ACKED`（收到
  `ACCEPTED_WHOLE` 覆盖或 `REJECTED_WHOLE` 摘除）。per-run bounded（沿用既有 cap 与 K=4 admission）。
- **timeout / validation reject**：entry **不删**，停留在 `RESERVED`/`SENT`；wake signal 依 outbox「是否仍有 pending」**重新置位**（re-arm），
  下次 poll 唤醒后重发。reject 不推进 occurrenceSeq、不新增 identity。
- **poll dequeue 后未 ACK**：notification 停留 `SENT`（**未删**，删除只发生在 ACK 路径）；wake 依 outbox 仍 pending 而 re-arm。
- **重复 wake / retry**：coalesced signal 至多一个，重复 wake 不产生第二份投递记录。
- **重入 / 再发布**：对 outbox 的状态推进用**基于 entry generation 的 CAS**（期望旧 generation 才能推进），并发重入只有一个成功，另一个观察到新
  generation 后自然幂等返回。route signal 若采用「稳定 slot」变体，则该 slot 与 outbox entry generation **exact 1:1** 且同样 CAS，不构成第二账本。

即：route 永不是「另一份投递真值」，只是「叫醒既有 poll 去查 outbox」的去重信号。

### P2 闭合：不做单一 FIFO union queue；exact-route 双 lane + coalesced wake + 线性化点按 K 计数选 lane

**根因**：真实 `RemoteGameCommandBroker.java:183-198` 是 `BlockingQueue<PendingCommand>` FIFO + `queue.poll(...)`；把元素改成 union 后，
排在大量 command 之后的 notification 永远轮不到 scheduler 选择（反之亦然），声称的 `K=4` 双 lane fairness 无法实现。

**裁定**：不改成 union。exact-route 由**三个显式部件**构成，均属既有 broker、无新线程/poller/timer：
1. **command lane** = 既有 `BlockingQueue<PendingCommand>`（行为、identity、poll 完全不变）。
2. **notification owner** = 上节 outbox 的 per-route pending 视图（唯一账本，非队列副本）。
3. **coalesced wake signal**（上节）——command enqueue **或** notification reserve 任一有活时，唤醒**既有的** `poll(timeout)`（command
   lane 的入队本就唤醒 poll；notification 侧通过对同一等待点 signal 复用之，不新增等待原语）。

**选 lane（持锁线性化点，poll 被唤醒后）**：per-route 维护 `successfulOpportunityCounter`。在锁内、单点做决策：
- 若 notification owner 有 pending 且其已积压 `≥ K(=4)` 次**成功投递机会**（即 command lane 已被连续成功选择 K 次而 notification 未获选），
  则本次选择 **notification** lane（从 outbox 取下一条 eligible，按 entry generation 有序），并**重置**该计数。
- 否则：command lane 有 `PendingCommand` 则选 **command**（每次成功选择使计数 +1，向 notification 靠近其保底机会）；command lane 空但
  notification 有 pending 则选 **notification**（并重置计数）。
- **IDLE**（两 lane 皆无 ready）与 **validation-reject / 未成功投递**：**不计数**（既不 +1 也不重置），避免空转或失败被误算为「机会」。

由此 notification 在最多 K 次 command 之后必获一次固定投递机会，command 在其余机会优先，实现声明的 K=4 双 lane fairness；且不触碰 host/caller、
不新增 thread/poller/timer、`RemoteCommandPollStatus` 仍只是「选出的那一 lane 的既有 response 变体」。

### 不变量重申

- WARN-0（`RuntimeWarningIdentity` / `RuntimeWarningNotification`）FINAL APPROVED，本 Delta 不触碰；W-ACP-0/1 不触碰。
- 全程 Java/resources/tests/schema 冻结；未跑构建、未改 git；写集仅本日志。
- notification 仍 best-effort（允许 Cloud 重启/admission-reject 漏、允许 retry/DHXY 重启重），`lastMessage` 幂等终态、`SYSTEM_WARNING` 至少一次不变。
- 交付完毕，等待父级复审。

## Parent Review #10 - D6 PARTIAL PASS / D7 Published - 2026-07-13T04:34:00-04:00

父级对照当前 `RemoteGameCommandBroker` 的 `BlockingQueue<PendingCommand>` long-poll 与 DHXY
`RemoteCommandPollingLoop` 复审。D6 已把 ACK closed union 收敛为 `ACCEPTED_WHOLE` / `REJECTED_WHOLE`，并明确
`REJECTED_WHOLE` 只摘永久 receipt、余项形成新 snapshot；该项**通过且后续不得重开**。outbox 单一真值方向也正确，但 poll wake/
lane selector 仍不可按当前源码实现。当前总设计结论仍为 **BLOCKED，P0=0 / P1=1 / P2=1**。

1. **P1：coalesced boolean/token 不能唤醒当前 `BlockingQueue.poll(timeout)`。** D6 写“复用同一等待点 signal”，但当前等待点只会被
   `commandQueue.offer(PendingCommand)` 唤醒；一个 route boolean 不会唤醒阻塞 poll，另塞 notification token 又会回到第二 queue item
   owner。Repair 必须选定一个真实可编译原语并给方法级算法，例如 route-owned `Semaphore`/`Condition`：command 从 empty->non-empty、
   notification pending 从 false->true 时如何只发一个 permit/signal，poll 如何 bounded wait、醒来后如何持锁选 lane、消费后如何在仍有 ready
   work 时 re-arm，以及 timeout/spurious wake/validation reject 时如何不丢唤醒、不累计虚假 permits。不得新增 thread/poller/timer。
2. **P2：selector 的线性化锁序与 counter reset 仍缺一拍。** Repair 需固定 outbox lock、route selector lock、broker `stateLock` 的唯一
   顺序，禁止持 outbox/route lock 调 coordinator、网络或 I/O；`successfulOpportunityCounter` 只在 notification 已 pending 且 command
   成功选中时递增，notification 从空变 pending 时从 0 开始，notification 选中/清空时归零。还要明确先选后 validation 失败时 lane item/
   wake 如何恢复，避免“未成功投递不计数”却把 notification 永久取走。

### 下一任务：`W-ACP-WARN-D7`（Design Repair #6 Delta）

External Worker B 必须在 `2026-07-13T04:55:00-04:00` 前追加
`## External Worker B - CLAIMED - <timestamp>`，写明任务标题、领取时间与唯一写集=仅本固定日志。只关闭上述 P1×1/P2×1；ACK
closed union、WARN-0、W-ACP-0/1 不得重开。Java/resources/tests/schema/Maven/runtime 继续冻结，完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T04:37:34-04:00

- **任务**：`W-ACP-WARN-D7`（Design Repair #6 Delta）——闭合 Review #10 剩余 P1×1（唤醒原语）/ P2×1（锁序 + counter reset + 选中-失败恢复）。
- **领取时间**：2026-07-13T04:37:34-04:00（父级截止 04:55:00，在窗内）。
- **唯一写集**：仅本固定日志 `docs/superpowers/plans/reports/2026-07-12-cloud-auto-combat-panel-service-worker-b.md`。设计-only。
- **冻结遵守**：不重开 ACK closed union（Review #10 已 PASS）、WARN-0（FINAL APPROVED）、W-ACP-0/1；不动 Java/resources/tests/schema/Maven/runtime、A/remote/api、J/K/M/H owner、host/caller。不跑构建、不改 git。

## External Worker B - W-ACP-WARN-D7 Design Repair #6 Delta - 2026-07-13

只闭合 Review #10 的 P1×1 / P2×1。**已 PASS 且不重开**：ACK closed union（`ACCEPTED_WHOLE` / `REJECTED_WHOLE` 只摘永久 receipt + 余项换新
snapshot）、outbox 为 notification 唯一真值（D6 已定，本 Delta 不改其账本语义，只补「唤醒原语 + 选择器」这一层）。

### P1 闭合：选定真实可编译原语 `ReentrantLock + Condition`，替换裸 `BlockingQueue.poll(timeout)` 的内部阻塞点，给方法级算法

**根因**：D6 说「复用同一等待点 signal」不成立——当前 `RemoteGameCommandBroker.java:183-198` 的等待点只被 `commandQueue.offer(PendingCommand)`
唤醒；一个 route boolean 唤不醒阻塞 poll，另塞 notification token 又退回「第二 queue item owner」。

**裁定（选定原语）**：**每个 exact route 拥有一个 `ReentrantLock routeLock` + 一个 `Condition ready = routeLock.newCondition()`**，用它取代
broker 对该 route 的 `BlockingQueue.poll(timeout)` 内部阻塞（command 元素本身仍存在既有 `commandLane`，但「等待/唤醒」改由 `routeLock/ready`
统一承载，不再依赖 `BlockingQueue` 自带的阻塞 take）。routeLock 下的 route 状态：
- `ArrayDeque<PendingCommand> commandLane`（既有 command 元素，语义不变）；
- `boolean notificationPending`——**从 outbox 派生的缓存唤醒旗标**（true ⇔ 本 route outbox 有 ≥1 条 RESERVED/SENT 可投 notification）。它是
  coalesced 信号本体：无 payload、无 identity、非第二账本；outbox 仍是唯一真值，本旗标只是「outbox 有货」的一位缓存，poll 醒来后一律回查 outbox 取实体。
- `int successfulOpportunityCounter`（见 P2）。

**生产者（command 入队）** `enqueueCommand`：
```
routeLock.lock();
try { commandLane.addLast(cmd); ready.signal(); }   // 每次入队 signal 一次；谓词由消费者复检，signal 幂等
finally { routeLock.unlock(); }
```

**生产者（notification 转 pending）** `markNotificationPending`：**在 outbox 条目已在 outboxLock 内提交并释放 outboxLock 之后调用**（锁序见 P2）：
```
routeLock.lock();
try { notificationPending = true; ready.signal(); }  // 幂等：置一个已 true 的 boolean 不新增状态；重复 wake 被 coalesce
finally { routeLock.unlock(); }
```

**消费者（poll，替换裸阻塞）** `pollRoute(timeoutMs, now)`：
```
routeLock.lock();
try {
  long deadlineNanos = ready-wait 基于 routeLock 的 awaitNanos(剩余)   // now 由既有 broker 时钟传入，不新增 timer/thread
  while (commandLane.isEmpty() && !notificationPending) {
    long remaining = deadlineNanos - elapsed;
    if (remaining <= 0) return IDLE;                 // bounded wait 到点 → IDLE；不改 counter、不改 notificationPending
    remaining = ready.awaitNanos(remaining);         // spurious wake 由 while 谓词复检兜住，不丢唤醒
  }
  LaneChoice lane = selectLane();                    // 线性化点：仍持 routeLock，纯决策（见 P2）
  return prepareUnderLocks(lane);                    // 仅在锁内快照实体，网络发送在锁外（见 P2 恢复路径）
} finally { routeLock.unlock(); }
```
- **spurious wake**：`while` 谓词复检 → 不误投、不丢唤醒。
- **不累计虚假 permit**：用 `Condition`（非计数 `Semaphore`），signal 不攒余量；即便连发多次 signal，醒来只按谓词判定一次，无 permit 堆积。
- **timeout**：到点返回 IDLE，`notificationPending` 原样保留（下次 poll 仍能凭 outbox 再置），counter 不动。
- **无新增 thread/poller/timer**：awaitNanos 复用调用方线程与既有超时预算。

### P2 闭合：唯一锁序 + counter reset 精确规则 + 「选中后校验失败」恢复

**唯一锁获取顺序（全局固定，禁止逆序）**：`brokerStateLock ⊐ routeLock ⊐ outboxLock`（需要时先取 brokerStateLock，再 routeLock，最后
outboxLock；释放逆序）。
- **禁止持 outboxLock 或 routeLock 调 coordinator / 网络 / I/O**。故 `markNotificationPending` 必须在 **outboxLock 释放后** 才取 routeLock
  （绝不 outboxLock→routeLock，避免与消费者的 routeLock→outboxLock 撞成逆序死锁）。
- 消费者在选中 notification 时可在**持 routeLock 的前提下**再取 outboxLock 快照条目并标 `SENT`（顺序 routeLock→outboxLock，合法内层）；随后
  **释放两锁再做网络发送**——发送/序列化不在任何锁内。

**`successfulOpportunityCounter` 规则（精确）**：
- notification 从「空」变「pending」（false→true）时：counter **置 0**（从 0 起算其保底进度）。
- command 被**成功选中并投递**、且当时 `notificationPending==true`：counter **+1**（唯一递增点；notification 不 pending 时选 command 不计）。
- notification 被**成功选中并投递**（成功 SENT 且成功交给发送）时：counter **归 0**。
- notification 被清空（outbox 该 route 无可投项，notificationPending→false）：counter **归 0**。
- IDLE / 选中后校验失败 / 未成功投递：counter **不变**（既不 +1 也不归 0）。

**`selectLane()`（持 routeLock，纯决策）**：
- `notificationPending && counter >= K(=4)` → **NOTIFICATION**；
- 否则 `!commandLane.isEmpty()` → **COMMAND**；
- 否则（command 空、notification pending）→ **NOTIFICATION**。

**「先选后校验失败」恢复（闭合 off-by-one，避免把 notification 永久取走）**：**选中 ≠ 消费落定**。
- **COMMAND**：在 routeLock 内从 `commandLane` 取出该 command 即视为该 lane 的既有 at-least-once 投递（command 域 Review #10 未列问题，语义不变）；若其后校验失败，走 command 既有 outcome 路径，不涉及 notification counter。
- **NOTIFICATION**：选中时**只**在 outboxLock 内把该条目标 `SENT`（**不删**，删除仅在 ACK，D6 已定），并快照其 identity/payload；**此刻不归零 counter**。
  释放锁后做网络发送：
  - 发送**成功** → 再取 routeLock：counter 归 0；若 outbox 该 route 仍有余项则 `notificationPending=true; ready.signal()` 重新武装，否则置 false。
  - 发送/校验**失败** → 再取 routeLock+outboxLock：把该条目由 `SENT` 复位为可再投（回到 RESERVED/仍 SENT 视既有语义，总之**不删**）、
    `notificationPending=true; ready.signal()` 重新武装，**counter 保持不变**（仍 ≥K，下一 opportunity 必再选 notification）。由此「未成功投递不计数」
    不会把 notification 永久取走——它被重新武装且保留高优先级，下一拍再选。

即：counter 的归零只发生在 notification **成功投递** 或 **真正清空**，选中但发送失败一律「保状态 + 重武装 + 不归零」，off-by-one 消除。

### 不变量重申

- ACK closed union（Review #10 PASS）、WARN-0（FINAL APPROVED）、W-ACP-0/1 均不触碰、不重开。
- outbox 仍是 notification 唯一账本；`routeLock/ready/notificationPending` 只是唤醒+选择层，非第二投递真值。
- 全程 Java/resources/tests/schema/Maven/runtime 冻结；未跑构建、未改 git；写集仅本日志。未碰 A/remote/api、J/K/M/H owner、host/caller。
- notification 仍 best-effort / at-least-once，`lastMessage` 幂等终态、`SYSTEM_WARNING` 至少一次不变。
- 交付完毕，等待父级复审。

## Parent Review #11 - D7 PARTIAL PASS / D8 Published - 2026-07-13T04:42:00-04:00

父级对照当前 `RemoteGameCommandBroker.java:39,49,175-251,451-485,704-721` 复审。D7 选择
`ReentrantLock + Condition`，给出了 bounded `awaitNanos`、spurious wake 谓词循环、统一锁序、counter reset 与失败 re-arm；
这些方向通过且后续不得重开。但按现有代码直接实现仍会改变容量合同并在并发 long-poll 下重复选择同一 notification。
当前结论仍为 **BLOCKED，P0=0 / P1=2 / P2=0**。

1. **P1：`ArrayDeque` 未保持现有 command lane 的有界 admission 与 route 生命周期。** 当前每 route 是
   `ArrayBlockingQueue(limits.routeQueueCapacity())`，在写 request/action/flight ledger 前以 `remainingCapacity/offer` fail-closed；route
   创建还受 global/owner route limits 和 `usage.routes` 计数。D7 只写 `commandLane.addLast(cmd)`，会把 64 cap 变为无界并绕过
   `INPUT_QUEUE_REJECTED` 路径。Repair 必须给 exact `RouteState` 构造/注册/容量检查/terminal remove 算法，保持原
   `routeQueueCapacity`、global/owner route limit、ledger 注册顺序与失败无残留；不得借 warning 改普通 command 的 admission 语义。
2. **P1：notification selection 缺少 route 内唯一 in-flight claim，且成功线性化点前后谓词读取不原子。** D7 把
   `RESERVED/SENT` 都称为可投，并在锁外发送前仍保留 `notificationPending=true`；同 route 两个并发 poll 可选择同一 outbox entry。
   发送后又只写“再取 routeLock，若 outbox 仍有余项”，却没有在 `routeLock -> outboxLock` 内原子重算，可能把并发 reserve 的 wake
   清掉。Repair 必须定义每 route 至多一个 selected/in-flight notification claim（带 exact entry generation），选择时在
   `routeLock -> outboxLock` 内排除已 claim 项；只有 response 已成功构造并作为该 poll 返回值提交后才算 successful opportunity/归零
   counter。校验/序列化/返回前失败必须按 generation 原子释放 claim、重算 pending 并 signal；ACK 与新 reserve 并发时也不得覆盖新
   generation。普通 command 只有通过现有 final dispatch gate 并实际返回 `COMMAND` 才可给 fairness counter +1，不能把 dequeue 或
   authorization reject 算成功投递。

### 下一任务：`W-ACP-WARN-D8`（Design Repair #7 / implementation-ready Delta）

External Worker B 必须在 `2026-07-13T05:02:00-04:00` 前追加
`## External Worker B - CLAIMED - <timestamp>`，写明任务标题、领取时间与唯一写集=仅本固定日志。只关闭上述 P1×2；
ACK closed union、outbox 单一真值、`Condition` wake、WARN-0、W-ACP-0/1 全部冻结且不得重开。Java/resources/tests/schema/Maven/
runtime 继续冻结。Delta 必须给可直接落代码的 `RouteState` 字段、锁内状态转移和既有 broker 方法映射；完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T04:43:08-04:00

- **任务**：`W-ACP-WARN-D8`（Design Repair #7 / implementation-ready Delta）——闭合 Review #11 剩余 P1×2（① command lane 有界 admission/route 生命周期；② notification 唯一 in-flight claim + 成功线性化点原子谓词）。
- **领取时间**：2026-07-13T04:43:08-04:00（父级截止 05:02:00，在窗内）。
- **唯一写集**：仅本固定日志 `docs/superpowers/plans/reports/2026-07-12-cloud-auto-combat-panel-service-worker-b.md`。设计-only（implementation-ready 蓝图，不落 Java）。
- **冻结遵守**：不重开 ACK closed union / outbox 单一真值 / `Condition` wake（均已 PASS）、WARN-0、W-ACP-0/1；不动 Java/resources/tests/schema/Maven/runtime、A/remote/api、`RemoteTaskRunEndpoint`/`RemoteTaskRunErrorCode`/`CloudTaskRunExecutionGate`、J/K/M/H owner、host/caller。不跑构建、不改 git。

## External Worker B - W-ACP-WARN-D8 Design Repair #7 Delta - 2026-07-13

已核对真实源码 `RemoteGameCommandBroker.java`：L46 `stateLock`、L49 `commandQueues: Map<RemoteClientScope,BlockingQueue<PendingCommand>>`、
L175-251 `poll`、L451-485 enqueue admission、L462 `new ArrayBlockingQueue<>(limits.routeQueueCapacity())`、L463 `remainingCapacity()==0`、
L467 `offer`、L476-484 createRoute + `usage.routes` 递增、L704-722 `finishTerminalLocked`（`queue.remove(pending)`）。下述蓝图**保留全部既有
command 语义**，只在其旁**增设**唤醒+notification 选择层。**已 PASS 不重开**：ACK closed union、outbox 唯一真值、`Condition` wake 方向。

### P1-1 闭合：command lane 保留既有有界 `ArrayBlockingQueue` admission 与 route 生命周期（撤回 D7 的 `ArrayDeque`）

**根因**：D7 写 `ArrayDeque.addLast` 会把 64 cap 变无界并绕过 `BROKER_CAPACITY_EXCEEDED` / route limit / ledger 注册顺序。

**裁定**：**不新建 command 容器**。每 route 的 command 仍是既有 `ArrayBlockingQueue<PendingCommand>(routeQueueCapacity)`，存于既有
`commandQueues`，admission 与生命周期**逐字不变**：

- **建 route**（映射 L451-484）：仍在 `stateLock` 内，顺序不变——`commandQueues.size()>=globalRouteLimit` → `capacityOutcome`；
  `usage.routes()>=ownerRouteLimit` → `capacityOutcome`；否则 `new ArrayBlockingQueue<>(routeQueueCapacity)`。已存在 route 时
  `remainingCapacity()==0` → `capacityOutcome`。
- **入队**（映射 L467-484）：`offer(candidate)` 失败 → `capacityOutcome`；成功后顺序仍为 registerAction → `inputFlights.put` →
  createRoute(`commandQueues.put` + `usage.routes` 递增)。offer 前失败无残留，逐字保留。
- **terminal remove**（映射 L704-722）：`pendingCounted` CAS → `usage.pendingRequests--` → `queue.remove(pending)` → releaseInputFlight，不变。

**新增（唯一改动）**：为每个 route 配一个并列的 `RouteWait`（见下）承载「等待/唤醒」。command **入队成功后**（在 `stateLock` 内、L467 offer 成功那一支）
追加一步 `routeWait.signalCommand()`（`stateLock ⊐ routeLock`，是允许方向）。**warning 绝不改动普通 command 的 admission**：notification 有独立
outbox 容量与 K=4 admission，不写 `commandQueues`、不占 `routeQueueCapacity`、不动 `usage.routes`。

### `RouteState` / `RouteWait` 可直接落码字段（新增类型，B 写集内的 service 层；不改 broker 既有字段语义）

```
// 每 exact route（RemoteClientScope）一个，与既有 commandQueues 条目一一对应，随 createRoute 建、随 route 生命周期存活
final class RouteWait {
    final ReentrantLock routeLock = new ReentrantLock();
    final Condition ready = routeLock.newCondition();          // 唯一等待点，取代对 ABQ 的阻塞 take
    boolean notificationPending;                                // 缓存旗标：outbox 有 ≥1 条“eligible 且未被 claim”的 notification
    NotificationClaim inFlight;                                 // 每 route 至多一个 in-flight claim；null=无
    long successfulOpportunityCounter;                          // 见 P2-fairness（沿用 D7 已 PASS 的语义）
}
record NotificationClaim(long entryGeneration, String entryIdentity) {}   // 精确锚定 outbox 条目代
```
- command 容器**不在** `RouteState` 内——它就是既有 `commandQueues.get(scope)` 的 `ArrayBlockingQueue`，避免任何“第二 command 账本/无界化”。
- `notificationPending`/`inFlight`/`counter` 只受 `routeLock` 保护；outbox 条目本体仍由 `outboxLock` 保护（outbox 唯一真值不变）。

### P1-2 闭合：每 route 唯一 in-flight notification claim + 成功线性化点原子谓词

**根因**：D7 把 RESERVED/SENT 都称可投、发送前保留 `notificationPending=true`，同 route 两并发 poll 会选同一 outbox entry；发送后未在
`routeLock→outboxLock` 内原子重算，可能清掉并发 reserve 的 wake。

**裁定（锁序恒为 `stateLock ⊐ routeLock ⊐ outboxLock`；禁止持 routeLock/outboxLock 调 coordinator/网络/I/O）**：

**poll 主循环**（新版，替换 L192-251 的阻塞 take；command dispatch gate 逻辑逐字复用）：
```
routeWait.routeLock.lock();
try {
  while (commandQueue.isEmpty() && !routeWait.notificationPending) {
     long remaining = deadlineNanos - clock.nanoTime();
     if (remaining <= 0) return IDLE;                     // 超时：不改 counter、不改 claim/pending
     ready.awaitNanos(remaining);                          // spurious wake 由 while 谓词兜住
  }
  lane = selectLaneLocked();                               // 持 routeLock 的线性化点
} finally { routeWait.routeLock.unlock(); }                // 选 COMMAND 前必须先放 routeLock（否则违反 stateLock⊐routeLock）

if (lane == COMMAND) {
   // 释放 routeLock 后进既有 stateLock 路径：queue.poll()（非阻塞）+ 既有 authorizeAndMarkDispatch 门
   // 完全复用 L206-247；只有 L225/L242 真正 `return ...command(...)` 才是“成功投递”
   // 并发另一 poll 若也醒来，queue.poll() 原子，只有一个拿到，另一个得 null→回到 routeLock 等待（与今日语义一致）
   ... 既有 command dispatch ...
   // 成功 return COMMAND 时：若返回前 notificationPending 为真，则 counter+1（见 fairness）；dequeue/authz reject 不计
}
```

**selectLaneLocked()（持 routeLock）**：
- `notificationPending && counter >= K(=4)` → NOTIFICATION；
- 否则 `!commandQueue.isEmpty()` → COMMAND；
- 否则（command 空、notificationPending）→ NOTIFICATION。

**NOTIFICATION 选择—claim—发送—落定（原子谓词）**：
1. **claim（`routeLock→outboxLock` 内，原子）**：要求 `inFlight == null`（每 route 至多一个 in-flight；非 null 则本 route 视 notification 不可选，
   `selectLaneLocked` 据此不选 NOTIFICATION）。从 outbox 取**下一条 eligible 且未被 claim** 的条目（eligible = RESERVED 或 SENT-且-resend-due），
   置 `inFlight = new NotificationClaim(entry.generation, entry.identity)`，把该 outbox 条目标 `SENT`（**不删**，删除仅在 ACK），快照 payload；
   **原子重算** `notificationPending = outbox 是否还有另一条 eligible-未claim`；释放 outboxLock、routeLock。
2. **发送在锁外**（无锁做序列化/返回构造/网络）。
3. **落定（再取 `routeLock→outboxLock`，按 `inFlight.entryGeneration` 原子）**：
   - **成功**（response 已成功构造并作为本 poll 返回值提交）：按 generation 校验 `inFlight` 仍指向同代条目 → 清 `inFlight=null`；`counter=0`（仅此“成功投递”归零）；
     **原子重算** `notificationPending`（读当前 outbox，不盲写 false，故并发 reserve 的 wake 不丢），若真则 `ready.signal()`。
   - **失败（校验/序列化/返回前任何失败）**：按 generation 释放 claim（`inFlight=null`），把该 outbox 条目由 `SENT` 复位为 eligible（**不删**）；
     **counter 不变**（未成功投递，保持 ≥K 优先级，避免被永久取走）；原子重算 `notificationPending=true` 并 `ready.signal()` 重新武装，下一拍再选。
   - **ACK 与新 reserve 并发**：ACK 按 generation 移除条目、新 reserve 以**新 generation** 追加；落定的重算与 claim 均以 `entryGeneration` 为锚，
     只影响自己那一代，**绝不覆盖新 generation**（generation 不等则该步对该条目 no-op）。

**fairness counter（沿用 D7 已 PASS 语义，精确映射既有 dispatch gate）**：
- notification 从空变 pending（false→true，在 reserve 的 markPending 内）→ `counter=0`。
- command **成功投递**（poll 在 L225/L242 真正 `return ...command(...)`）且当时 `notificationPending==true` → `counter+1`；
  `queue.poll()` 仅出队、或 `completeTerminalLocked`（authorization/observation reject）→ **不计**。
- notification **成功落定** → `counter=0`；notification 被清空（outbox 该 route 无 eligible）→ `counter=0`。
- IDLE / 选中后失败 / 未成功投递 → counter 不变。

**producer 锁序复核**：command 入队在 `stateLock` 内 `offer` 成功后调 `routeWait.signalCommand()`（stateLock→routeLock，合法）；notification
`markPending` 由 outbox reserve 在 **释放 outboxLock 之后** 取 routeLock 置 `notificationPending=true`+`signal`（绝不 outboxLock→routeLock）。
消费者 poll 选 NOTIFICATION 时 routeLock→outboxLock（合法内层）。全程无环、无锁内 I/O。

### 不变量重申

- ACK closed union（Review #10 PASS）、outbox 唯一真值（D6）、`Condition` wake（Review #11 方向 PASS）、WARN-0（FINAL APPROVED）、W-ACP-0/1：均冻结不重开。
- command lane 的有界 `ArrayBlockingQueue` admission / route limit / ledger 注册顺序 / terminal remove **逐字保留**；warning 不改普通 command admission。
- 全程 Java/resources/tests/schema/Maven/runtime 冻结；未跑构建、未改 git；写集仅本日志。未碰 A/remote/api、J/K/M/H owner、host/caller。
- 交付完毕，等待父级复审。

## Parent Review #12 - D8 PARTIAL PASS / D9 Published - 2026-07-13T04:53:00-04:00

父级复审 D8 的 `RouteWait`、bounded command admission 与 generation claim。D8 已关闭 command lane 无界化和同一 warning 被并发
poll 重复 claim；既有 ABQ admission/ledger 顺序、`stateLock -> routeLock -> outboxLock`、仅成功 response 更新 fairness counter 均通过且
不得重开。当前仍有两个 notification-only 可达性缺口，结论为 **BLOCKED，P0=0 / P1=2 / P2=0**。

1. **P1：notification-only route 无法创建或被 poll。** D8 规定 `RouteWait` 与既有 `commandQueues` 一一对应，warning 又“不写
   commandQueues、不占 usage.routes”。但当前 `poll()` 在 `commandQueues.get(route)==null` 时立即 IDLE；一个 run 在首次 mechanical
   command 前，或 command route 已合法退役后产生 warning，outbox 虽有 entry 却没有 RouteWait/等待点，永远不可达。Repair 必须把
   exact route owner 明确成同时承载 command/outbox readiness 的单一 `RouteState` map：notification reserve 能在同一 global/owner route
   cap 下创建 notification-only state，command 后到时复用同 state 与其有界 ABQ；route usage 只计一次。创建/失败/terminal cleanup 必须
   原子且无空壳残留，poll 只查该 RouteState，不再以 command queue 是否存在判断 route 存在。
2. **P1：`SENT-resend-due` 没有无线程的 level predicate/wake 算法。** D8 claim 只把 `SENT` 在“resend due”时算 eligible，
   但 `notificationPending=false` 后 Condition 没有 timer 会在 due 时 signal；若 response 丢失且无新 command/reserve，warning 永久停在
   SENT。Repair 必须选择并固定一种：推荐由每次 client long-poll 在锁内以 outbox 的 `nextEligibleAtNanos` 计算
   `min(clientDeadline, nextEligibleAt)`，`awaitNanos` 到点后主动重算 pending；不新增 thread/timer/poller，且重发 delay 只能是 transport
   pacing，不能改变业务 retry/fallback。或定义 SENT 立即仍 eligible，但必须证明不会形成无 ACK busy-loop/command starvation并保持 K=4。
   exact generation claim、ACK 并发与 Condition re-arm 仍按 D8，不得用 TTL 清账。

### 下一任务：`W-ACP-WARN-D9`（Design Repair #8 / final Delta）

External Worker B 必须在 `2026-07-13T05:13:00-04:00` 前追加 CLAIMED（任务、时间、唯一写集=仅本日志），只关闭上述 P1×2。
D8 已通过的 bounded ABQ、唯一 claim/锁序/fairness，以及 ACK/outbox/WARN-0/W-ACP-0/1 全冻结。Java/resources/tests/schema/Maven/
runtime 继续冻结；完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T04:56:14-04:00

- **任务**：`W-ACP-WARN-D9`（Design Repair #8 / final Delta）——闭合 Review #12 剩余 P1×2（① notification-only route 创建/可 poll；② `SENT-resend-due` 无线程 level wake）。
- **领取时间**：2026-07-13T04:56:14-04:00（父级截止 05:13:00，在窗内）。
- **唯一写集**：仅本固定日志 `docs/superpowers/plans/reports/2026-07-12-cloud-auto-combat-panel-service-worker-b.md`。设计-only（implementation-ready，不落 Java）。
- **冻结遵守**：不重开 bounded ABQ / 唯一 claim / 锁序 / fairness（D8 PASS）、ACK closed union、outbox 唯一真值、`Condition` wake、WARN-0、W-ACP-0/1；不动 Java/resources/tests/schema/Maven/runtime、A/remote/api、`RemoteTaskRunEndpoint`/`RemoteTaskRunErrorCode`/`CloudTaskRunExecutionGate`、J/K/M/H owner、host/caller。不跑构建、不改 git。

## External Worker B - W-ACP-WARN-D9 Design Repair #8 Delta（final）- 2026-07-13

已核对真实源码：broker **无 route 退役路径**——`commandQueues` 只增不删（L477 `put`），`usage.routes` 从不递减，L720 `queue.remove(pending)`
只移除已终结 command，route 本体 per scope 永存。故真实缺口是「run 在首个 mechanical command 之前产生 warning → `commandQueues.get(route)==null`
→ `poll` 于 L187-189 立即 IDLE → outbox 有 entry 却无等待点，永不可达」；「退役后」为潜在场景，一并以「统一 RouteState + 双 lane 排空才可退役」覆盖。
**已 PASS 不重开**：bounded ABQ admission、唯一 generation claim、`stateLock⊐routeLock⊐outboxLock`、仅成功 response 更 fairness counter、ACK
closed union、outbox 唯一真值、`Condition` wake。

### P1-1 闭合：单一 `RouteState` 同时承载 command + outbox readiness；notification reserve 可在同一 route cap 下创建 notification-only state

**根因**：D8 让 `RouteWait` 与 `commandQueues` 一一对应、warning 不写 `commandQueues`，而 `poll` 以 command queue 是否存在判断 route 存在 →
warning-先到 无 route 即不可达。

**裁定**：把 route owner 收敛为**单一注册表** `Map<RemoteClientScope, RouteState> routes`（取代「以 `commandQueues` 判存在」；`commandQueues`
的 ABQ 内聚进 `RouteState`，admission 语义逐字不变）。route 由 **command 入队或 notification reserve 谁先到谁创建**，同一 global/owner cap、
`usage.routes` **只计一次**。

```
final class RouteState {                       // 每 exact route 一个，routes.get(scope)
    final RemoteClientScope scope;
    ArrayBlockingQueue<PendingCommand> commandQueue;   // 懒创建：首个 command 时 = new ArrayBlockingQueue<>(routeQueueCapacity)
                                                       // notification-only 期间为 null（无 command 时不占 ABQ；不改 command admission）
    final ReentrantLock routeLock = new ReentrantLock();
    final Condition ready = routeLock.newCondition();
    boolean notificationPending;                       // 见 P1-2（level 重算）
    NotificationClaim inFlight;                         // 每 route 至多一个（D8）
    long successfulOpportunityCounter;                  // D8 fairness
}
```

**统一创建门** `ensureRouteLocked(scope, owner)`（在既有 `stateLock` 内，供两条路径共用）：
```
RouteState s = routes.get(scope);
if (s == null) {
    if (routes.size() >= limits.globalRouteLimit())  return capacityOutcome(...);   // 同既有 global 门
    if (usage.routes() >= limits.ownerRouteLimit())  return capacityOutcome(...);   // 同既有 owner 门
    s = new RouteState(scope);
    routes.put(scope, s);
    usage.routes()++  （Math.incrementExact，只一次）
}
return s;
```
- **command 入队**（映射 L451-484）：改为先 `ensureRouteLocked`，若其 `commandQueue==null` 则此刻 `new ArrayBlockingQueue<>(routeQueueCapacity)`；
  其余 `remainingCapacity()==0`→reject、`offer` fail-closed、registerAction→inputFlights→(route 已由 ensure 计数，不再重复 `usage.routes++`)
  **逐字保留**。command 复用同一 `RouteState`（含 notification 先建的那个）。
- **notification reserve**：在 outbox 写 entry **之前**先 `ensureRouteLocked`（同 cap、同门），创建 notification-only state（`commandQueue` 留 null）；
  随后按 D8 在 outboxLock 内写 entry、释放 outboxLock 再取 routeLock 置 `notificationPending`+`signal`。
- **poll**（映射 L183-189）：改为 `RouteState s = routes.get(route); if (s==null) return IDLE;`——**route 存在性只看 `routes`**，不再看 command queue；
  等待/选择在 `s.routeLock/ready` 上（D8 主循环）。
- **原子无空壳**：ensure 与 `usage.routes++` 在同一 `stateLock` 临界区，失败在 put 之前 return，无残留。**退役（当前不存在；若未来新增）唯一条件**：
  `commandQueue 空/为 null` 且 outbox 该 route 无 entry 且 `inFlight==null`，在 `stateLock` 内原子 `routes.remove + usage.routes--`；由此
  **outbox entry 绝不 outlive 其 RouteState**，「退役后 warning 不可达」不可能发生；退役后新 warning 由 reserve 重新 ensure 创建。

### P1-2 闭合：`SENT-resend-due` 用「每 poll 锁内 `min(clientDeadline, nextEligibleAt)` + `awaitNanos` 到点重算」的无线程 level wake

**根因**：D8 把 SENT 仅在 resend-due 时算 eligible，但 `notificationPending=false` 后 `Condition` 无 timer 在 due 时 signal，response 丢失且无新
command/reserve 时 warning 永停 SENT。

**裁定（采纳父级推荐项，level-triggered，无线程/timer/poller）**：outbox 每 route 维护派生量 `nextEligibleAtNanos` =
「当前最早变为可投的 nanoTime」：存在 RESERVED（从未发）或已 due 的 SENT → 取当前时刻（即刻可投）；否则 = 最早那条 SENT 的 `sentAtNanos + resendPacingNanos`；
无 pending → `Long.MAX_VALUE`。poll 主循环改为：
```
routeLock.lock();
try {
  while (commandQueueEmpty() && !immediatelyEligibleLocked(now())) {
     long now = clock.nanoTime();
     long clientWait = clientDeadlineNanos - now;
     if (clientWait <= 0) return IDLE;                       // 客户端 deadline 到 → IDLE（不改 counter/claim/pending）
     long dueWait    = max(outbox.nextEligibleAtNanos(route) - now, 0);
     ready.awaitNanos(min(clientWait, dueWait));             // 无 signal 也会在 due 或 deadline 醒来
     // 醒来（signal 或到点）后 while 重算：immediatelyEligibleLocked 依 now>=nextEligibleAt 现算，
     // 并据此重算 notificationPending（level，不依赖历史 signal）
  }
  lane = selectLaneLocked(); ...
} finally { routeLock.unlock(); }
```
- `immediatelyEligibleLocked(now)` = outbox 有「RESERVED 未 claim」或「SENT 且 `now >= entry.nextEligibleAt` 且未 claim」的 entry。
- **无线程**：due 唤醒折进既有每-poll `awaitNanos`；无新 thread/timer/poller。
- **level（不丢）**：eligibility 由 outbox 状态 + clock 现算，非 edge；即便本 poll 因 client deadline 先到而 IDLE，long-poll 模型下客户端立即再 poll，
  下一 poll 重算仍会在 due 时选中 → SENT 不会永久卡住（真无客户端 poll 期本就投不出，属 best-effort，允许）。
- **`resendPacingNanos` 仅 transport pacing**：是「同一 entry 两次 transport 重发的最小间隔」，**不改业务 retry/fallback**、不新增 OCR/retry/TTL、
  **不 TTL 清账**（entry 只在 ACK 删除，D6）。
- **exact generation claim / ACK 并发 / `Condition` re-arm 仍按 D8**：claim/落定按 `entryGeneration` 原子；ACK 按 generation 删、reserve 以新
  generation 加；成功落定/失败释放 claim 时**原子重算** `notificationPending` 与 `nextEligibleAt` 并 `signal`，并发 reserve 的 wake 不丢、不覆盖新代。
- **无 busy-loop / 无 command starvation**：未 due 的 SENT 不算 eligible（谓词为假则 `awaitNanos` 真阻塞到 due，不空转）；K=4 fairness 不变
  （notification 仅在 eligible 时参与 selectLane，counter 规则同 D8）。

### 不变量重申

- D8 已 PASS 项（bounded ABQ admission、唯一 generation claim、`stateLock⊐routeLock⊐outboxLock`、仅成功 response 更 fairness）、ACK closed union、
  outbox 唯一真值、`Condition` wake、WARN-0（FINAL APPROVED）、W-ACP-0/1：全部冻结不重开。
- 统一 `RouteState` 后 command admission（bounded ABQ / route limit / ledger 顺序 / L720 terminal remove）逐字不变；warning 不改普通 command admission。
- 全程 Java/resources/tests/schema/Maven/runtime 冻结；未跑构建、未改 git；写集仅本日志。未碰 A/remote/api、J/K/M/H owner、host/caller。
- 交付完毕，等待父级复审。

## Parent Review #13 - D9 PARTIAL PASS / D10 Published - 2026-07-13T05:00:30-04:00

父级对照当前 `RemoteGameCommandBroker.java:175-251,451-485,704-722` 复审。D9 已把 route 存在性从
`commandQueues` 收敛到单一 `RouteState`，notification-first 与 command-later 共用同一 route cap/usage；long-poll 也采用
`min(clientDeadline,nextEligibleAt)` 的无线程 level wait。这两条主方向通过且后续不得重开。但当前仍有 route 事务/回收和并发
claim 下的可实施性缺口，结论为 **BLOCKED，P0=0 / P1=3 / P2=1**。

1. **P1：notification-first route 创建没有与 outbox admission 形成同一原子事务。** D9:1658-1674 先
   `ensureRouteLocked` 并立刻 `routes.put + usage.routes++`，随后才写 outbox；“失败在 put 之前无残留”只覆盖 route cap 失败，
   没覆盖 outbox owner/global cap、duplicate/generation 冲突、计数溢出或 reserve 异常。任一后续失败都会留下无 command、无 outbox 的
   空壳 route 并永久占 route quota；若 ensure 后释放 `stateLock` 再写 outbox，还存在 route 被退役后写出 orphan entry 的窗口。
   Repair 必须给单一线性化协议：要么持 `stateLock -> outboxLock` 完成 route reservation 与 outbox admission，并在任何失败上对
   `createdNow` 做 exact rollback；要么先建立带 generation 的 reservation/pin，commit 后才计 route，abort 必须按 exact generation
   回滚。rollback 只能在 commandQueue 仍 null/empty、outbox 无 entry、无 claim/in-flight 且没有并发 owner 时执行，route 与
   `usage.routes` 必须同一临界区一增一减，禁止删掉并发 command 创建者复用的 state。
2. **P1：route retirement 不能留作“若未来新增”。** D9:1677-1679 把退役写成未来条件，但 notification-only state 在 ACK 后若不
   实际回收，会随着 clientSession/route 变化持续占满 global/owner route cap；当前 broker 本就只减 pending、不减 routes，D9 引入
   warning-first 创建后会扩大该泄漏面。Repair 必须把本波次实际 terminal cleanup 写清：ACK/abort/command terminal 后，在统一
   state 下仅当 command lane 空、outbox 空、`inFlight==null`、无在途 poll/producer reservation 时按 exact state 条件移除并
   `usage.routes--`；有旧 poll 的处理须复用 P Full R0 已批准的 `inFlightPolls/retired/conditional remove` 纪律，不能另造第二套
   route owner。若 P 实施后的真实类型/方法名不同，B 必须等 P 稳定后按实际源码方法级合并。
3. **P1：`nextEligibleAtNanos` 与 immediate eligibility 对 claim 的谓词不一致，会在并发 poll 下零等待自旋。**
   D9:1686-1688 把任一 due SENT 算作 `nextEligibleAt=now`，但 D9:1704 的 `immediatelyEligibleLocked` 又排除已 claim entry。
   当 poll-1 正持有唯一 due entry 的 `inFlight`，poll-2 会看到 immediate=false、dueWait=0，反复 `awaitNanos(0)` 直到 poll-1
   落定，形成 hot spin。Repair 必须让 `immediatelyEligible`、`nextEligibleAt` 与 lane selection 使用完全相同的
   “eligible 且未被 exact generation claim”谓词；若只剩 claimed entry，nextDue 必须视为无可选项并等待 claim release 的
   Condition signal 或 client deadline，不能零等待循环。
4. **P2：重发 pacing 仍无精确配置属主和值，且 deadline 算术未定义溢出策略。** `resendPacingNanos` 在 D9 首次出现，未给来源、
   默认值、上下界或构造注入；实施者仍可随意选择新 timing。Repair 必须固定为 Cloud transport 配置/limits 中的唯一字段（不是
   business retry/TTL），给 exact 默认值与校验范围，并规定 `sentAt + pacing`、`deadline-now` 使用饱和或既有 monotonic helper，
   `Long.MAX_VALUE` 表示无 due，禁止整数溢出变成即时重发。

### 下一任务：`W-ACP-WARN-D10`（Design Repair #9 / final implementation-ready Delta）

External Worker B 必须在 `2026-07-13T05:20:30-04:00` 前追加 CLAIMED（任务、时间、唯一写集=仅本日志），只关闭上述
P1x3/P2x1。D9 已通过的单一 RouteState 方向、同 cap/usage、无线程 long-poll due wait，以及 D8 之前全部 PASS 项均冻结。
Java/resources/tests/schema/Maven/runtime 继续冻结；P Full R0 的 route/selector 写集仍由 P 独占，B 不得提前落码。完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T05:06:09-04:00

- **任务**：`W-ACP-WARN-D10`（Design Repair #9 / final implementation-ready Delta）——闭合 Review #13 剩余 P1×3/P2×1（① route 创建与 outbox admission 单一原子事务；② 本波 route retirement 复用 P Full R0 纪律；③ 统一 eligible-未claim 谓词消除零等待自旋；④ resendPacing 配置属主+溢出策略）。
- **领取时间**：2026-07-13T05:06:09-04:00（父级截止 05:20:30，在窗内）。
- **唯一写集**：仅本固定日志 `docs/superpowers/plans/reports/2026-07-12-cloud-auto-combat-panel-service-worker-b.md`。设计-only。
- **冻结遵守**：不重开单一 RouteState 方向 / 同 cap-usage / 无线程 long-poll due wait（D9 PASS）及 D8 前全部 PASS 项、ACK closed union、outbox 唯一真值、WARN-0、W-ACP-0/1；**P Full R0 的 route/selector 写集由 P 独占，B 不提前落码**；不动 Java/resources/tests/schema/Maven/runtime、A/remote/api、`RemoteTaskRunEndpoint`/`RemoteTaskRunErrorCode`/`CloudTaskRunExecutionGate`、J/K/M/H owner、host/caller。不跑构建、不改 git。

## External Worker B - W-ACP-WARN-D10 Design Repair #9 Delta（final）- 2026-07-13

已核对真实源码 `RemoteGameCommandBroker.java`：L190 `deadlineAfter(waitTimeoutMs)`、L193 `remaining(deadlineNanos)` 既有 monotonic
deadline helper；L84-86 `Limits` 由构造注入、`RemoteProtocolValidation.positive` 校验；L41-43 `DELIVERY_TIMEOUT_MS`/`OUTCOME_GRACE_MS`/
`DEADLINE_RECHECK_MS` 为既有 transport 常量。**已 PASS 不重开**：单一 RouteState、同 cap/usage、无线程 due wait、bounded ABQ、唯一 generation
claim、锁序、fairness、ACK closed union、outbox 唯一真值、`Condition` wake、WARN-0、W-ACP-0/1。

### P1-1 闭合：route reservation 与 outbox admission 在**同一 `stateLock→outboxLock` 临界区**线性化，失败零残留（deferred-commit）

**根因**：D9 先 `ensureRouteLocked` 即刻 `routes.put + usage.routes++` 再写 outbox，只覆盖 route-cap 失败；outbox owner/global cap、
duplicate/generation 冲突、计数溢出、reserve 异常任一失败 → 空壳 route 永占 quota；ensure 后释放 `stateLock` 再写 outbox → route 退役后写出 orphan。

**裁定（deferred-commit 单一线性化协议，`reserveNotificationLocked`）**：整个 notification reserve 在**持有 `stateLock` 全程**内完成，outbox
admission 以 `outboxLock` 内层嵌套；**任何 `routes`/`usage.routes`/outbox 的可见变更只在全部校验通过后、于同一临界区末尾一次性提交**——失败路径因
「尚未提交」而天然零残留，无需回滚：
```
synchronized (stateLock) {                                   // 全程持有，command 创建者被序列化，绝不误删其复用的 state
  RouteState s = routes.get(scope);
  boolean createdNow = (s == null);
  if (createdNow) {                                          // 仅“检查”，先不 put、不 ++
     if (routes.size() >= globalRouteLimit) return capacityOutcome(...);
     if (usage.routes()  >= ownerRouteLimit)  return capacityOutcome(...);
  }
  synchronized (outboxLock) {                                // stateLock⊐…⊐outboxLock，合法内层
     OutboxAdmission a = outbox.admit(scope, entryDraft);    // owner/global outbox cap + duplicate/generation 冲突 + 溢出，全在此校验
     if (!a.ok()) return a.outcome();                        // 失败：未 put、未 ++、未写 entry → 零残留
     // —— 全通过，单点提交 ——
     if (createdNow) { routes.put(scope, s = new RouteState(scope)); usage.routes()++; }  // 一增在同一临界区
     outbox.commit(a);                                       // 写 entry（generation 由 admit 分配）
  }                                                          // 释放 outboxLock
  s.routeLock.lock();                                        // stateLock⊐routeLock，合法；绝不 outboxLock→routeLock
  try { s.notificationPending = true; s.ready.signal(); } finally { s.routeLock.unlock(); }
}
```
- **不释放 stateLock 于 route-commit 与 outbox-write 之间** → orphan 窗口消除。
- **一增一减同一临界区**：`usage.routes++` 与 `routes.put` 原子；cleanup 的 `--`/`remove` 亦在 `stateLock` 内（P1-2）。
- **若实施者偏好 early-put 变体**：必须对 `createdNow` 做 exact rollback，且 rollback 仅在 `commandQueue==null/empty ∧ outbox 无 entry ∧
  inFlight==null ∧ 无并发 owner` 时执行；本 Delta 采用 deferred-commit 以从根上免除该风险，二选一由父级定夺。

### P1-2 闭合：本波 route retirement 写清，复用 P Full R0 的 `inFlightPolls/retired/conditional remove`，B 不另造第二 owner、不提前落码

**根因**：D9 把退役写成「未来条件」，notification-only state ACK 后不回收将随 clientSession/route 变化占满 route cap；warning-first 创建扩大泄漏面。

**裁定**：本波必须实际回收。terminal cleanup 触发点 = **ACK 落定 / abort / command terminal**（映射既有 `finishTerminalLocked` L704-722 之后
增一步 `retireRouteIfDrainedLocked(scope)`）。在 `stateLock` 内按 **exact state 条件**移除：
```
retireRouteIfDrainedLocked(scope):
  RouteState s = routes.get(scope); if (s == null) return;
  if (s.commandQueue == null || s.commandQueue.isEmpty())        // command lane 空
     && outbox.isEmpty(scope)                                    // outbox 无 entry
     && s.inFlight == null                                       // 无 in-flight claim
     && noInFlightPollOrProducerReservation(scope):              // 无在途 poll/producer reservation —— 见下
        routes.remove(scope); usage.routes()--;                  // 一减，同一临界区
```
- **在途 poll/producer 的判定必须复用 P Full R0 已批准的 `inFlightPolls / retired / conditional remove` 纪律**，不新建第二套 route owner。
  因 **P Full R0 的 route/selector 写集由 P 独占且尚未稳定**，本条为**设计级契约**：B 在 D10 只固定「回收条件与触发点」，`noInFlightPollOrProducerReservation`
  的**具体类型/方法名以 P 稳定后的真实源码为准**，B 届时按方法级合并进 `retireRouteIfDrainedLocked`，**现在不提前落这段码**。
- 退役后新 warning 由 `reserveNotificationLocked`（P1-1）在 cap 内重新创建，`outbox entry 绝不 outlive RouteState` 不变。

### P1-3 闭合：`immediatelyEligible` / `nextEligibleAt` / lane selection 用**完全相同**的「eligible ∧ 未被 exact-generation claim」谓词，消除零等待自旋

**根因**：D9 让任一 due SENT 记 `nextEligibleAt=now`，但 `immediatelyEligibleLocked` 又排除已 claim entry；poll-1 持唯一 due entry 的 `inFlight`
时，poll-2 见 immediate=false、dueWait=0，反复 `awaitNanos(0)` 直到 poll-1 落定 → hot spin。

**裁定**：定义唯一谓词并三处共用：
```
eligibleUnclaimed(entry, now) =
   ( entry.state == RESERVED                                  // 从未发
     || (entry.state == SENT && now >= entry.nextEligibleAt) )// SENT 且已到 resend-due
   && !isClaimed(entry.generation)                            // 未被本 route inFlight 以 exact generation 占用
```
- `immediatelyEligibleLocked(now)` = ∃ entry: `eligibleUnclaimed(entry, now)`。
- `nextEligibleAtNanos(now)` = min over **未 claim** entry 的 eligible-at；**claimed entry 一律排除**。若剩余 pending 全被 claim → `Long.MAX_VALUE`（无 due）。
- `selectLaneLocked` 选 notification 也只认 `eligibleUnclaimed`。
- 于是 poll-2（只剩 claimed entry）：immediate=false ∧ nextEligibleAt=MAX → `awaitNanos(min(clientDeadline−now, MAX))` = 等到 **client deadline
  或 claim-release 的 `ready.signal()`**（poll-1 成功落定/失败释放 claim 时按 D8 `signal`），**无零等待循环**。K=4 fairness 与 claim/ACK 并发按 D8 不变。

### P2 闭合：`resendPacing` 固定为 Cloud transport `Limits` 唯一字段 + 饱和算术 + `Long.MAX_VALUE`=无 due

**裁定**：
- **属主与值**：在既有 `Limits`（构造注入、`RemoteProtocolValidation.positive` 校验）新增唯一字段
  `notificationResendPacingNanos`（来源：Cloud transport 配置，非 business retry/TTL）。默认 `DEFAULT_NOTIFICATION_RESEND_PACING_MS = 1_000L`
  （与既有 `OUTCOME_GRACE_MS`=1_000 同量级的 transport pacing），校验范围 `[DEADLINE_RECHECK_MS(=100ms), DELIVERY_TIMEOUT_MS(=5_000ms)]`，
  构造期 `positive` + 上下界校验；实施者不得另选 timing。**它只是同一 entry 两次 transport 重发的最小间隔**，不改业务 retry/fallback、不新增 OCR/retry、不 TTL 清账。
- **算术（饱和，复用既有 monotonic helper）**：
  - `entry.nextEligibleAt = saturatingAdd(entry.sentAtNanos, pacingNanos)`；`saturatingAdd` 溢出 → `Long.MAX_VALUE`（视为无 due，绝不回绕成即时重发）。
  - poll 的 `dueWait`、`clientWait` 复用既有 `deadlineAfter(L190)`/`remaining(L193)` monotonic 语义；`remaining<=0` 走 IDLE；`min(clientWait,dueWait)`
    前对 `dueWait` 取 `max(0, …)`，对 `MAX_VALUE` 短路为「仅受 clientDeadline 约束」。
  - `Long.MAX_VALUE` 恒表示「无 due」，是 `nextEligibleAt` 唯一哨兵。

### 不变量重申

- D9/D8 及更早全部 PASS 项、ACK closed union、outbox 唯一真值、`Condition` wake、WARN-0、W-ACP-0/1：冻结不重开。
- **P Full R0 route/selector 写集由 P 独占**：D10 只固定回收条件/触发点/谓词/配置，`inFlightPolls/retired` 具体类型待 P 稳定后按真实源码方法级合并，B 不提前落码。
- 统一 RouteState 后 command admission（bounded ABQ / route limit / ledger 顺序 / L720 terminal remove）逐字不变；warning 不改普通 command admission。
- 全程 Java/resources/tests/schema/Maven/runtime 冻结；未跑构建、未改 git；写集仅本日志。未碰 A/remote/api、J/K/M/H owner、host/caller。
- 交付完毕，等待父级复审。

## Parent Review #14 - D10 DESIGN APPROVED / Implementation Sequenced - 2026-07-13T05:09:08-04:00

父级复审 D10。deferred commit 已把 route cap、outbox admission、generation/duplicate 校验与可见提交收进同一个
`stateLock -> outboxLock` 事务；三处 eligibility 已统一排除 exact claimed generation；1 秒 resend pacing 已固定为 transport
`Limits` 字段并给出 100ms..5000ms 校验和饱和 monotonic 算术。Review #13 的 route/outbox 空壳、零等待自旋与 timing 任意性均已关闭。

父级增加一条**绑定实施修正**：D10:1813-1823 的 `retireRouteIfDrainedLocked` 不能只持 `stateLock` 裸读由其它锁保护的
`inFlight/outbox`。最终实现必须在 P Full R0 稳定后的同一个真实 `RouteState` 上按
`stateLock -> routeLock -> outboxLock` 取得一致快照，确认 exact state 未 retired、command lane 空、outbox 空、claim 空、
producer reservation/inFlightPolls 均为 0 后先标 retired，再 `routes.remove(scope, exactState)` 并同临界区 `usage.routes--`；
释放顺序反向，锁内不得网络/I/O/callback。该修正替代伪代码的裸读，不新增第二 route owner。

在上述父级修正下，**warning transport 最终设计结论：DESIGN APPROVED，P0/P1/P2=0**。实现仍必须等 P2
`FULL-R0-IMP1` 源码与双构建稳定后，由 B 对真实类型做方法级合并；在此之前不得写 broker/routes/transport Java。

### Next-task handoff

B 不空等 P2。下一项独立任务已写入
`docs/superpowers/plans/reports/2026-07-13-cloud-left-top-status-switch-worker-b.md`：`W-LTSS-D1`，只做
`LeftTopStatusSwitchService` HEAD 基线整类迁云 Design #1，与 A/P/S 写集不重叠。B 必须到新日志领取；本日志 warning 设计到此冻结。

**无已批准业务差异；按基线等价迁移。**
