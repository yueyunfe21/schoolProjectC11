# TaskTrackerPanelService Boundary Closure - Internal Worker U

## Parent Task Brief #1 - `W-TTPS-D1` - 2026-07-13T06:45:00-04:00

### 角色与写集

- 你是 Internal Worker U，只做设计/实现，不是 reviewer；父级独立审查。先在本日志追加 `CLAIMED`（task、claimedAt、唯一写集），再开始。
- 唯一写集仅本 append-only 日志。本轮只交 Design #1；两仓 Java/Maven/schema/resources/tests、P2/A/B 写集、host/caller 全冻结。
- 先读 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部、迁移矩阵，再以 committed HEAD `0114604e` 的 clean 2545 行 `TaskTrackerPanelService.java`、全部 caller、tracker DTO 与现有 Cloud `TrackerPanelReaderCloudDecisionService`/image processor 为权威。保护全部 dirty/untracked，不回滚、不覆盖、不提交。

### 目标与固定边界

- 固化该 Service 的最终归属，不把只读面板观察器误搬成 Cloud poller：DHXY 永久保留 exact-window capture、anchor/template 定位、ROI 裁剪、指纹缓存、artifact bytes、prepared dialog action 的本地安全构造与 observer wake；Cloud 只拥有 OCR/文本链/任务分类等业务解释。
- 不允许 Cloud 读本地模板路径、持 HWND、启动截图线程；不允许本地根据识别结果推进 Task phase。`UNKNOWN/STOPPED` 与缺图/空结果必须闭合，不得伪装普通“无任务”。
- 保持 HEAD public API、anchor/crop/fallback 顺序、缓存命中语义、prepared action correlation、日志与 caller behavior；无新 TTL/retry/extra read/fallback。

### Design #1 必交付

- 全部 public API/caller/常量/cache/mutable state inventory，逐项标 local mechanical/diagnostic 或 Cloud business state。
- exact scope/taskRun/window/stopEpoch/revision、capture occurrence/artifact identity、prepared action stable identity 与 terminal cleanup。
- typed capture/artifact/OCR/result seam，说明 raw image bytes 的 ownership、hard cap、release 与 uncertain；禁止 raw request/poll/outcome bypass。
- 无线程 continuation + local observer wake、tenant/global/per-run capacity、restart 无 restore、完整 failure matrix。
- 可编译 DAG 与精确文件表；指出是否存在一个可独立实施的真实 pure policy/type leaf，无则明确“无”，不造 wrapper。
- 自审 P0/P1/P2 仅 QA，不算批准；不运行 Maven，不启动任何运行面。

**无已批准业务差异；按基线等价迁移。**

## CLAIMED - `W-TTPS-D1`

- task: `W-TTPS-D1` / Parent Task Brief #1 / Design #1
- claimedAt: `2026-07-13T06:50:17.0351613-04:00`
- 唯一写集: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-tracker-panel-worker-u.md`（append-only）
- 冻结确认: DHXY/Cloud Java、Maven、schema、resources、tests、P2/A/B 文件、host/caller 全部只读；不回滚、不覆盖、不清理、不 `git add/commit`。

## DELIVERED - `W-TTPS-D1` / Design #1 - 2026-07-13T07:13:52.2020692-04:00

### 0. 基线、证据与本轮边界

- DHXY 业务基线固定为 committed HEAD `0114604e1ff5f15491d2910959c45252e893d04f`，当前分支 `thin-client-design`。`git status --short` 非空；所有 dirty/untracked 均只读保护，本轮未回滚、覆盖、清理、暂存或提交。
- Cloud 只用于核对现有接口与 Full R0 前置，读到的工作树 HEAD 为 `3b988caa010254973e03342272e6d1d6a9685b01`，同样存在 dirty/untracked；不把其中未提交实现当作 tracker 业务权威。
- 已完整读取固定日志、`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部、`docs/superpowers/specs/2026-07-12-service-migration-matrix.md`，并逐项核对 committed HEAD 的 `TaskTrackerPanelService.java`、全部生产 caller、tracker DTO、`TrackerPanelReaderCloudDecisionService`、`CloudImageProcessor` 与 Cloud `DecisionEngine.trackerPanelReader`。
- 已核对 `docs/业务逻辑.md`：基线门禁 215-224；修罗/五倍普通怪 tracker 到入战合同 283-335；五倍 tracker 目标地图 343-363、414-479、699-740、904-913；热启动顺序 1101-1166；修罗快捷路线 1168-1214；修罗 pre-cloud 基线/失败表 1253-1284。尤其保留修罗“接任务同一 snapshot”“维护后重读当前 tracker”“首次失败回完整非快捷入口”“中途失败不得拼旧路线”和五倍“目标地图只来自当前绿链文本”的事实。
- 迁移矩阵 `TaskTrackerPanelService` 行 496 与 DTO 行 641-648 把 title template/fingerprint 阈值写成 Cloud 最终权威；Parent Task Brief #1 已明确修正为：template、ROI、fingerprint、artifact bytes 与 prepared action 永久本地。后续实施应更新矩阵，但本轮唯一写集禁止修改它。
- HEAD 类注释称“从不发输入”并不完整：`allowPanelReposition=true` 时会经 `InputSequences.submitAndWait` 原子拖动 tracker panel 并睡眠 500ms。这是显式允许的本地机械安全动作，最终设计保留，Cloud 不持有坐标/HWND、不发该输入。
- 本轮零 Java/Maven/schema/resources/tests；没有运行 Maven、测试或任何应用/服务。

**固定结论：无已批准业务差异；按基线等价迁移。**

### 1. 最终归属闭合

| 能力 | 永久权威 | 闭合规则 |
| --- | --- | --- |
| exact window binding、`HWND`/PID/player epoch、窗口基点与坐标换算 | DHXY | 只从当前 `WindowRuntimeContext` / `WindowTaskContextHolder` 绑定读取；Cloud 只收到不可伪造的 exact echo，不持有或搜索 HWND。 |
| live capture、接任务 snapshot 派生、anchor 搜索、safe-area 判断、可选拖动、panel/detail/validation ROI | DHXY | 保持 HEAD 顺序、阈值、模板顺序、坐标空间与现有物理读取次数；Cloud 不启动截图线程，也不能给本地路径。 |
| anchor/title template 资源、模板路径、template match、title score | DHXY | Cloud 只收到稳定 `templateId + taskKey + score` 的机械事实，永远看不到 `images/template/...` 路径，也不在 Cloud 重跑 title template。 |
| panel/detail/validation PNG bytes、临时图路径、指纹和几何缓存 | DHXY | 本地 artifact registry 是唯一 bytes owner；Cloud 只拿一次防御性 wire copy 并瞬时解码。临时路径只用于现有日志/兼容 API，不是身份。 |
| 绿字洗图/分段、OCR 文本链、目标地图规整、任务分类、选择第几条 link、`REROLL`/no-action 业务解释 | Cloud | 由一个 per-runtime `TaskTrackerPanelInterpretationService` 持有；不能经 raw context map、Base64 request 或 `DecisionEngine` 字符串结果旁路。 |
| Cloud 选中点的窗口内边界校验、同帧 validation crop、fingerprint、`PreparedDialogAction` 构造和发布 | DHXY | Cloud 返回 typed window-relative link；本地只验证并物化，不解释任务 phase。物化必须引用原 observation artifact，禁止为构造 action 再截图。 |
| Task phase、重试/fallback/yield/park/watchdog/完成判断 | 对应 Cloud Task 编排 owner | tracker Service 两端都不得推进 phase。本地只发布 existing level wake；`UNKNOWN/STOPPED` 不可变成 phase 事实。 |
| Wuhuan cache-hit、Wubei chained fast verify | DHXY mechanical | 保留 `distance<=1` 的面板缓存和 `distance<=8` 的黄袍小区验证；后者仍恰好一张 fresh small screenshot，miss 终止快路且不全量重读。 |

最终形态不是新增 Cloud poller，也不是复制一份完整 `TaskTrackerPanelService`。DHXY 保留现有类作为本地机械核心和 HEAD public facade；Cloud 新建真正拥有 OCR/文本解释的 Service。两者只通过 Full R0 已授权的 typed `CAPTURE` retained action 交互。

### 2. committed HEAD inventory

#### 2.1 public API 与 caller

| HEAD API | committed caller | 最终职责拆分；兼容语义 |
| --- | --- | --- |
| `Point findWuhuanNextGreenClickPoint()` L149 | 无生产 caller | facade：本地 crop/template；Cloud 选 link；本地转 screen point。保留 null-on-miss。 |
| `TaskTrackerPanelReadResult readWuhuanTrackerTitle(String, boolean)` L180 | `FiveRingTaskV2` L2906，间接由 L1635/L2802 调用 | 本地 yellow title template 是机械事实；Cloud 只把 typed fact 映射成 presence 业务结果；保留 found/empty、是否允许拖 panel。 |
| `TaskTrackerPanelPrepareResult prepareWuhuanPathingLink(String, boolean)` L217 | `FiveRingTaskV2` L2380；`WindowTaskRunner` L1537 | 首帧 panel cache fingerprint、本地 cache hit、Cloud link 解释、本地同帧 action 物化。HEAD 当前不产出 negative，所有非正向仍映射 `empty()`。 |
| `TaskTrackerPanelReadResult readWubeiTrackerPanel(String)` L546 | `WubeiTask` L2559/L2878/L4771 | 本地按固定五模板顺序 match/crop；Cloud OCR 黄字/绿链/`targetMapName`/分类；保留同一 detail block 和 caller 分支。 |
| `readWubeiTrackerPanelFromSnapshot(Path,int,int,String)` L573 | 无生产 caller | 本地 existing snapshot 派生，不上传路径；无第二次截图。保留 debug/兼容入口，不据此新增测试。 |
| `readXiuluoTrackerPanel(String)` L628 | `XiuluoTaskV2` L2554/L3411 | 保持“Cloud active path first、disabled 才 legacy local fallback”的外部顺序；active path 内 title template 改由本地 typed capture 执行，Cloud 不读模板。 |
| `findXiuluoTrackerGreenClickPoint(String)` L653 | 无生产 caller | 只组合 read + selected point；不点击、不注册 intent、不改 phase。 |
| `readXiuluoTrackerPanelFromSnapshot(Path,int,int,String)` L668 | `XiuluoTaskV2` L1581/L5941 | 必须派生自接任务后的 exact single snapshot artifact；不能把本地 `Path` 传云、不能补拍。 |
| `resolveXiuluoTrackerGreenClickPoint(TaskTrackerPanelReadResult)` L681 | `XiuluoTaskV2` L3418 | 保留纯值提取：selected link 优先，否则第一 link；Cloud 已拥有 selection，helper 不再做业务判断。 |
| `readXiuluoTrackerPanelForReplay(Path,...,Path)` L704 | 无生产 caller | 本地 debug/replay API；no-local-test 模式下不迁、不运行、不新增 testcase。若以后显式要求 replay，再单开测试范围。 |
| `getCroppedTaskDetailInTrackerPanel(String,String)` L742 | 无生产 caller | 纯本地模板/ROI/debug path facade；Cloud 永远不见 template/path。 |
| `prepareWubeiChainedTrackerFastAction(TaskTrackerPanelReadResult,String)` L758 | `WubeiTask` L4837 | 用同一 read artifact 与 Cloud 已选 link 在本地构造 action；禁止重新截图。 |
| `verifyWubeiChainedTrackerFastAction(PreparedDialogAction,String,boolean)` L808 | `WubeiTask` L4713 | 本地恰好一次 validation ROI fresh capture + wash/fingerprint；保留 terminal miss/no full reread。 |
| package static `expandedVisionAnchorToScreenAnchor(Point,int,int)` L1794 | 类内/旧测试 | 本地坐标换算；不是 Cloud policy。 |
| 五个 public `WUBEI_TASK_KEY_*` 常量 L73-77 | Wubei caller/分类协议 | public String 保留兼容；wire 使用 closed enum，适配层只做一一映射。 |

`MultiWindowTaskManager` 只有 bean 注入，不调用 public 方法；不把注入关系误算业务 caller。

#### 2.2 dependency、DTO 与 mutable state

| HEAD 项 | 当前作用 | 最终归属 |
| --- | --- | --- |
| `GameClientTracker`、`CoordinateHelper`、`WindowScopedTempPath`、`InputSequences`、`WindowTaskContextHolder` | capture、template、坐标、临时图、可选拖动、exact runtime | 全部本地。 |
| `MapNameCanonicalizer` | 绿链目标地图规整 | Cloud interpretation；legacy local fallback 只按 HEAD 保留到 final cutover。 |
| `ImageProcessorService / CloudImageProcessor` | 当前 title wash、green wash、fingerprint 也经旧 Cloud preprocess | 最终拆开：tracker safety 所需 yellow/green wash 与 fingerprint 算法按现有像素算法等价落到 DHXY；Cloud 只在 OCR 内部瞬时 preprocess。不得用网络 preprocess 决定本地 action 安全。 |
| `TaskClassifierCloudShadowService` | Wubei/Xiuluo 分类影子 | 吸收到 Cloud interpretation；final cutover 后移除 tracker 调用。 |
| `TrackerPanelReaderCloudDecisionService` | raw Base64 + map/string parser | 被 typed Full R0 seam 取代；只在 rollout 期做单一兼容入口，不能与新 Service 双算。 |
| `TaskTrackerTitleTemplate` | taskKey/displayName/local path/threshold | 永久本地 template catalog；wire 只发 templateId/taskKey/score。 |
| `TaskTrackerGreenLink` | screen-absolute bbox + targetMap + source | Cloud 产生 window-relative typed link；本地 adapter 校验并转成现有 screen-absolute DTO。 |
| `TaskTrackerPanelReadResult` | found/title/path/text/links/selected/probe/source | 保留 HEAD facade；新增 opaque artifact correlation，不能让 path 成为 correlation。 |
| `TaskTrackerPanelPrepareResult` | action/negative/two local ROIs | 本地 facade；Cloud 不直接构造 Java local action/negative。 |
| `TaskTrackerPanelNegativeResult` | 三种 negative + local timestamp/sequence | 当前 Wuhuan prepare 不产出，迁移不得激活。未来若要启用必须另开行为 CR；`UNKNOWN` 永不映射该 DTO。 |
| `TaskTrackerPanelCacheEntry` | per-window fingerprint/click/geometry/ROI | 本地；追加 stable run/source-artifact correlation，保留 geometry + distance=1 判定。 |
| `TaskTrackerFastMatchResult` | local small fingerprint verify | 本地；矩阵“迁 Cloud”结论被 Parent Brief 修正。 |
| `PreparedDialogAction` | screen click + validation ROI/fingerprint | 本地；追加 dedicated tracker identity，不复用 `intentId`。 |

`TaskTrackerPanelService` 自身没有 mutable business field，只有九个 final collaborator。实际 mutable state 为：

1. `WindowRuntimeContext` 的 atomic prepared action、tracker cache、tracker negative 与 sequence；binding drift/reset 会清理 transient state。
2. `FiveRingPhaseContext` 的 tracker ROI/phase/intent；`WubeiTask` 的异步 tracker future、snapshot、chained cache；`XiuluoTaskV2` 的 accept snapshot/objective/tracker futures。它们继续由各自 Task owner 管理，Service 不复制。
3. window-scoped temp PNG 只做诊断/现有 facade 输出，不是 authoritative state。
4. 新增的 local artifact registry 是唯一 raw-byte retained owner；Cloud workflow state只保留 identity、typed result 与 retained handles，不保留图片。

#### 2.3 常量归属与不可变顺序

| 组 | exact HEAD 值/顺序 | 最终 owner |
| --- | --- | --- |
| game frame | `1024x768` | DHXY geometry validation。 |
| anchor 搜索 | window-relative `(6,196)-(207,551)`，threshold `0.82`；narrow miss 后 `updateGlobalVision` + full-window `ImageFinder`，再 local-to-screen | DHXY，顺序不变。 |
| panel geometry | anchor offsets `(-96,+12)-(+86,+350)`；safe max `(164,353)`；drag target `(104,221)` | DHXY。 |
| detail geometry | left padding `5`、width `175`；Wuhuan/Wubei block `65`、Xiuluo `40`；Wuhuan title fallback left shift `24` | DHXY。 |
| title catalog | Wubei 固定顺序：殿前献艺、三藏封魔、宝象谜情、智斗黄袍、魁星归位；另有 Wuhuan yellow/prepare 两模板与 Xiuluo 模板；title threshold `0.82` | DHXY；Cloud 无模板文件。 |
| text-chain algorithm | min pixels `20`、split gap `8`、delimiter max width `5`/pixels `18`、coordinate glyph max width `5`/min run `5` | Cloud；legacy local Xiuluo/Wuhuan disabled-mode copy只在 rollout 期保留，不成为第二生产 owner。 |
| cache fingerprint | Wuhuan panel `16x16`、max distance `1`；Wubei chained max distance `8` | DHXY mechanical safety。 |
| prepared validation crop | click-local `x-6..x+18`、`y-6..y+10`，green wash + binary fingerprint | DHXY；必须来自同一 detail frame。 |

### 3. typed seam：只扩展 Full R0 `CAPTURE`

#### 3.1 禁止的新旁路

- 不新增 `TRACKER_PANEL_READER` raw remote operation，不开放 raw `RemoteGameClientPort`，不让业务代码构造 requestId/actionId/captureId。
- 不再传 `imagePayloadBase64`、local path、template path、HWND、自由字符串 `selectionPolicy` 或分号 diagnostics。
- 不从 HTTP controller、old `CloudDecisionCoordinator.shadow`、raw poll/outcome map 直接调用 OCR。
- 不新增 tracker executor、watcher thread、scheduled poller、retry loop 或 timeout fallback。

#### 3.2 closed request union

在 P2 与 A 的 `CaptureRequest` 最终 canonical shape 稳定后，给现有 `CAPTURE` 增加至多一个 nullable `trackerPanelIntent`；它与 A 的 quest `artifactIntent` 互斥。`TrackerPanelCaptureIntent` 是有严格构造校验的 tagged union，不是字符串 map：

1. `OBSERVE`：`taskFamily(WUHUAN|WUBEI|XIULUO)`、`readProfile`、`sourceFrame`、`allowPanelReposition`。
2. `MATERIALIZE_SELECTED_LINK`：exact `artifactRef`、`interpretationDigest`、typed selected link、`DialogOperation.TASK_TRACKER_PATHING` 对应的 wire operation、target keyword。

`sourceFrame` 只允许：

- `LIVE_BOUND_WINDOW`：本地按该 profile 执行 HEAD anchor/template/crop 顺序。
- `EXISTING_CAPTURE_ARTIFACT`：引用 exact prior `captureId + imageSha256 + artifactId`，用于修罗接任务 single snapshot；本地从同一帧派生，禁止再截图。

`readProfile` 是本地机械 profile，不承载 Cloud 选择策略：`WUHUAN_TITLE_GATE`、`WUHUAN_PATHING`、`WUBEI_DETAIL`、`XIULUO_DETAIL`。Cloud 的 first-link/probe/task-aware 规则留在 interpretation Service 内。

#### 3.3 closed outcome fact

`CaptureOutcome` 保留 Full R0 common identity/image bytes/hash/dimensions/provider/observedWindow，并增加 nullable `trackerPanelFact`。仅 common `OBSERVED` 可带 fact：

- `ObservationFact`：artifactRef、task family/profile、ordered frame ordinals+SHA、panel/detail window-relative ROI、local templateId/taskKey/score、confirmed title hit/miss、panel fingerprint、capture occurrence。
- `MaterializationFact`：artifactRef、preparedActionId、window-relative click/validation ROI、fingerprint digest、local publish disposition、capture occurrence。

普通 CAPTURE 的 intent/fact 都为 null，canonical bytes 与行为不变。`UNKNOWN/STOPPED/NOT_EXECUTED` 不携带 domain fact/image；不能被伪造成 title miss 或 no-link。

成功 title miss 仍是一次 `OBSERVED` mechanical fact，并带本次 panel evidence bytes；Cloud 可据 baseline 映射成 confirmed no-task。截图失败、图片不可读、template engine 异常、capacity reject 都不是 title miss。

#### 3.4 Cloud business result

Cloud `TaskTrackerPanelInterpretation` 是 typed immutable result：

- `FOUND_LINK`：exact artifact echo、taskKey、yellowText、typed links、selectedLink、`targetMapName`、probe shape、interpretationDigest。
- `REROLL`：只用于 HEAD 已有 Wubei 规则，带 exact taskKey/reason。
- `CONFIRMED_NO_TASK` / `CONFIRMED_NO_LINK`：只有完整成功 observation + 对应 baseline reader 成功解释才可产生。
- `UNSUPPORTED_TASK`：成功读到但不属于已知 Wubei task key；compat adapter 映射 HEAD empty。
- `UNKNOWN` / `STOPPED`：机制失败或 lifecycle 终止；永远不是普通 negative。

Cloud result只给出 window-relative rect/click。DHXY 必须验证 click 在 exact retained detail ROI、`1024x768`、当前 exact binding 内，再物化 screen-absolute point。

#### 3.5 same-frame prepared action 与 cache hit

1. `OBSERVE` local outcome将 ordered frame 存入 local registry。Wuhuan prepare 保留 HEAD 的实际顺序：先捕获 panel 做 `16x16` cache fingerprint；cache miss 后仍按现有 crop/title 路径取得 detail frame。一个 artifact 可含 `PRIMARY_PANEL` 与 `DETAIL_BLOCK` 两个有序 frame，不能擅自合并成一次读，也不能新增第三次读。
2. Cloud 解释 detail wire copy并选择 link，然后用已预留的第二个 retained CAPTURE handle 发 `MATERIALIZE_SELECTED_LINK`。
3. DHXY 从 exact artifact 的 `DETAIL_BLOCK` 裁 `x-6..x+18/y-6..y+10`，执行本地 green wash/fingerprint，构造 `PreparedDialogAction`。这是派生 crop，不是 fresh screen capture。
4. 同 `preparedActionId` 重投只幂等重发布同一个 action；不同 result bytes/decision digest 引用同 semantic address 一律拒绝。
5. Wuhuan cache hit仍不调用 Cloud OCR：cache 中的 click是上一份 Cloud interpretation 的已批准选择；本地只在当前 revision 的新 panel capture 上验证 geometry 与 distance<=1，再生成带当前 capture occurrence/revision 的新 prepared identity。
6. Wubei chained action从同一 read artifact物化；后续 `verifyWubei...` 恰好一张 fresh small screenshot、distance<=8，miss 不触发 full read。

### 4. exact identity、stable IDs 与 publish fence

每一个 observation/materialization 同时绑定：

`RemoteTaskRunScope(tenantId,userId,deviceId,clientSessionId) + taskRunId + taskType + Window(windowId,nativeHandle,processId,playerIdentityEpoch) + stopEpoch + runRevision + RemoteSemanticAddress(phaseCode,actionSlot,occurrence,attempt) + captureId + requestDigest`。

- `occurrence` 是业务阶段声明的第 N 次视觉事实；`attempt` 只有 verified `NOT_EXECUTED` 才可按 Full R0 规则更新。`UNKNOWN` 不续 mint、不换 occurrence、不自动重拍。
- `artifactId = tpa1:sha256(canonical(exact identity, captureId, requestDigest, readProfile, ordered frame SHA/ROI))`。不含 wall clock、随机数、本地 path；Cloud 可重算并核验。
- `preparedActionId = tpp1:sha256(artifactId, interpretationDigest, selected link index/rect/click, operation, targetKeyword, validationFingerprint)`。
- `PreparedDialogAction.intentId` 继续只表达现有 route/pathing intent；不得塞 artifact/prepared identity。新增 dedicated `TaskTrackerPreparedActionIdentity`。
- local cache可跨同一 stable run 的 revision 被“重新验证后重物化”，但旧 prepared action/artifact不可跨 revision 发布。新 action identity必须携带当前 revision与当前 capture occurrence。

本地发布前后都执行同一个 fence：

1. exact scope/taskRun/taskType/window tuple/stopEpoch/runRevision 与当前 runtime 相等；
2. `WindowTaskRunner` 现有 owner guard仍相等：windowId、taskType、taskCode、local taskRunId、exact `RunningTaskHandle` reference、taskIndex、stopToken reference；
3. 当前 interest/operation/targetKeyword仍匹配，且没有 combat/pathing/priority owner 冲突；
4. artifact、interpretation digest、selected link、validation fingerprint全匹配，click在 retained ROI；
5. fence fail只丢弃当前 publish并返回 typed stale/unknown，不清理别的 run、不发布 wake、不推进 phase。

### 5. bytes ownership、容量与 release

#### 5.1 local artifact registry

`TaskTrackerPanelArtifactRegistry` 是 assembly/Spring 单例内的唯一 byte owner，process-local、同步 admission、无线程、无 TTL、无 LRU/FIFO eviction：

| 维度 | hard cap |
| --- | --- |
| single encoded PNG | `512 KiB`；tracker 派生图最大 profile geometry必须同时通过。 |
| one artifact aggregate | `1 MiB`，包含 ordered panel/detail/validation bytes与元数据；prior full-window snapshot只做 exact reference，不在 tracker registry复制。 |
| one task run | `8 artifacts / 8 MiB` |
| one tenantId | `512 artifacts / 32 MiB` |
| process global | `8192 artifacts / 256 MiB` |
| retained tenant buckets | `1000` |

数值依据：HEAD live panel固定 `182x338`，detail最大 `175x65`，validation最大 `24x16`；`512 KiB` 大于单 panel 的未压缩 RGB/ARGB 量级，`1 MiB` 足够保留 HEAD ordered panel+detail派生；global count/bytes与现有 Cloud artifact governor的 `8192/256MiB` 对齐，但 tracker 不复用其会 eviction 的 store。

同 artifactId + same bytes redelivery返回 exact retained entry且不重复计费；同 identity不同 bytes拒绝。新 entry在一把 registry admission lock 下先同时检查 run/tenant/global count+bytes，再一次插入；任何 cap失败零写入，返回 `UNKNOWN/CAPACITY_REJECTED`，不 eviction、不重试、不产生 negative。

#### 5.2 Cloud state

Cloud只保留 typed workflow identity/result/opaque handles，不保留 image bytes：

| 维度 | hard cap |
| --- | --- |
| one run open tracker workflows | `8` |
| one tenantId open workflow records | `512` |
| global open workflow records | `10000`（对齐 Full R0 retained semantic-slot default） |
| tenant bucket count | `1000` |

PNG在 caller thread内校验 SHA、尺寸并瞬时解码；方法返回前 flush/drop decoded image。不建 tracker queue/thread。cap拒绝为 `UNKNOWN`，不排队、不睡眠、不改 Task phase。

#### 5.3 release/uncertain

- Cloud confirmed no-action/reroll/found result完成 final consumption，且 local收到 exact final-consumed receipt后，释放不再需要物化的 observation bytes。
- action路径必须等 local同 `preparedActionId` 已幂等 publish（或 exact fence 已确定拒绝）并收到 materialization final-consumed receipt后，才释放 artifact bytes。`PreparedDialogAction` 自身保留 click/fingerprint/identity，不依赖图片存活。
- Wuhuan cache只保留 fingerprint、window-relative click、geometry、ROI和 source correlation，不保留 raw bytes；按现有 runtime reset/binding/task lifecycle清理，不新增 TTL。
- broker/transport/ack `UNKNOWN` 时保留 bytes和workflow record；不 mint新 attempt、不重拍。容量满可以阻止新工作，但绝不能删 uncertain entry腾位置。
- exact task terminal、stopEpoch改变、binding/player epoch漂移、taskRun close时，只清理匹配 exact stable run的 artifacts/cache/prepared action；不得前缀扫、不得清其它窗口。
- 不使用 `CloudArtifactStore/ScopedPngArtifactStore` 承担 tracker authority，因为它在 Cloud、会持久化/evict，且违反“artifact bytes本地 owner”。

### 6. 无线程 continuation 与 local observer wake

1. Cloud Task/Service调用只运行在现有 Task host continuation/caller thread；每次只消费当前 retained handle/outcome并返回，不创建 `CompletableFuture` executor、scheduler或 sleep loop。
2. 远端等待沿用 Full R0 broker与 DHXY现有 `RemoteCommandPollingLoop`；不再建 tracker-specific poll endpoint。
3. `WindowTaskRunner` 现有观察 cadence只发布机械“当前 tracker observation应检查/已有 exact prepared action” level signal；它不解释 OCR、不判任务完成、不把 template miss当 phase事实。
4. local `MATERIALIZE_SELECTED_LINK` 幂等发布 action后，经现有 `WindowReadyEventBus` 发 `PREPARED_ACTION_READY / TASK_TRACKER_PATHING / target`。重复发布同 `preparedActionId` 是 level wake，不产生第二业务 occurrence。
5. Task醒来后仍按自身 HEAD operation/target/owner guard消费 action；generic `TASK_ATTENTION_REQUIRED`、plain dialog、runner negative都不能代替 typed tracker wake。
6. Cloud continuation若在 outcome后被中断，只留下 retained uncertain；下一次由现有 task lifecycle/ready wake重入同 handle。没有后台补偿线程。

### 7. failure matrix

| 阶段/故障 | typed 结果 | state/release | caller/wake 语义 |
| --- | --- | --- | --- |
| 缺 exact context、scope/taskRun/window/stopEpoch/revision不一致 | request reject / `STOPPED` 或 stale `UNKNOWN` | 不建 artifact | 不 wake、不改 phase。 |
| semantic address/handle非当前、foreign ledger、same address不同 request bytes | protocol reject | 保留原 retained record | 不重建、不旁路 raw request。 |
| source snapshot ref缺失、SHA/captureId/artifactId不一致 | `UNKNOWN` | 不补拍；保留可验证的原 entry | 修罗 snapshot path按解析失败/既有 fallback，不能伪造无 tracker。 |
| live binding refresh失败、capture provider失败、图片空/不可读 | `UNKNOWN` | 无 artifact或保留 uncertain partial前的零写入 | HEAD facade empty；不是 confirmed negative。 |
| narrow anchor miss，expanded search成功 | `OBSERVED` | 按 HEAD继续 | 不是 retry，是同一次既有 fallback。 |
| narrow miss且 expanded `updateGlobalVision`/match失败 | confirmed mechanical miss仅当完整图成功；机制失败为 `UNKNOWN` | 成功证据按 final receipt释放 | 不新增第三次搜索/截图。 |
| anchor outside safe、`allowPanelReposition=false` | mechanical no-observation/empty | 不发 input | 保持 read-only fail-closed。 |
| allow=true拖动提交失败或 outcome uncertain | `UNKNOWN` | 不假设 panel已移动，不再拖第二次 | 不 capture后续、不 negative。 |
| local title template完整成功但无 match | `OBSERVED + TITLE_MISSED` | 保留到 Cloud final | Cloud才可按该 task baseline产 `CONFIRMED_NO_TASK`。 |
| template资源缺失、洗图/模板引擎异常 | `UNKNOWN` | 不产 title miss | 不让 Task当“任务不存在”。 |
| local artifact run/tenant/global cap满 | `UNKNOWN/CAPACITY_REJECTED` | atomic zero-write，无 eviction | 不排队、不 retry、不 wake。 |
| encoded bytes/hash/dimension/profile不合法 | `UNKNOWN/MALFORMED_ARTIFACT` | 丢未 admission bytes | Cloud不解码、不分类。 |
| exact command redelivery | 返回同 artifactId/bytes/fact | 不重复计费 | 不重复 capture/input；同 action只 level wake。 |
| outcome transport timeout/late outcome | `UNKNOWN`，late exact non-UNKNOWN按 Full R0记录 | artifact保留 | 不 mint新 attempt；等现有 lifecycle重入。 |
| Cloud SHA/artifact identity/ROI echo校验失败 | `UNKNOWN/MALFORMED` | local artifact保留至终态 | 不 materialize。 |
| OCR engine异常、decode失败、文本链异常 | `UNKNOWN` | workflow/artifact保留 | 不映射 no-task/no-link。 |
| 成功 OCR 确认无任务/无 link | typed confirmed result | final receipt后释放 | public facade按 HEAD empty；Wuhuan不发布 `TaskTrackerPanelNegativeResult`。 |
| Wubei taskKey不受支持 | `UNSUPPORTED_TASK` | 正常 final释放 | 保持 HEAD empty/required-failure外观，不猜分支。 |
| Cloud选点越出 detail ROI/window、link index不存在 | `UNKNOWN/SAFETY_REJECTED` | 不 materialize，保留证据 | 不点击、不 negative。 |
| materialize引用artifact已释放/foreign run | `UNKNOWN` | 不 recapture | 不用 temp path恢复。 |
| validation crop/green wash/fingerprint失败 | `OBSERVED + MATERIALIZATION_MISS` 或机制异常 `UNKNOWN` | final后按 exact disposition释放 | public prepare empty；不重读 panel。 |
| materialize前/后 revision、binding、owner guard漂移 | `STOPPED/STALE` | exact run cleanup或保留到 terminal receipt | 不 publish/wake。 |
| 同 preparedActionId重复物化 | 返回同 materialization fact并幂等 publish | 不重复 action/state | level wake可重复，Task只消费同 identity一次。 |
| 同 semantic address得到不同 interpretation digest/selected link | protocol reject | 保留首个 decision | 禁止 Cloud重选后覆盖本地 action。 |
| Wuhuan cache geometry/fingerprint miss | 正常 cache miss | 保留 cache或由成功新读替换 | 继续 HEAD full path；无额外 retry/read。 |
| Wuhuan cache hit | local `CACHE_HIT_PREPARED` | 新 current-revision prepared identity | 无 Cloud OCR；发布 typed ready wake。 |
| Wubei chained small verify mismatch/capture fail | `TaskTrackerFastMatchResult.matched=false` | caller既有 cache处理 | terminal fast miss；恰好一读，不 full reread。 |
| Cloud confirmed action且local publish成功 | `FOUND_LINK + PREPARED` | final receipt后释放 bytes | Task按 operation/target消费并自行推进 phase。 |
| stop/pause interruption | `STOPPED`，不是 FAILED | exact lifecycle清理或保留到 coordinator终态 | 修罗/五倍按 `TaskCheckpoint`/既有暂停恢复，不包装 required failure。 |
| normal task terminal | exact terminal cleanup | 只删该 stable run | 无晚到 wake/action。 |

### 8. 三条业务路径的等价要求

#### Wuhuan

- title gate只由本地 `panel_title_yellow` template机械确认；prepare crop继续使用现有 prepare template。
- cache先于 full link read，geometry与distance=1不变；cache miss后的 capture/template/Cloud link/同帧 fingerprint顺序不变。
- `prepareWuhuanPathingLink` 任何 uncertain/empty都仍返回 `TaskTrackerPanelPrepareResult.empty()`。Runner已有 negative branch不得因迁移被激活；未来要产生 `TASK_NOT_FOUND/TASK_FOUND_NO_GREEN/TASK_FOUND_NO_LINK` 必须另开批准的行为 CR。
- local wake只发布 `PREPARED_ACTION_READY / TASK_TRACKER_PATHING / wuhuan`；不把 generic attention当业务事实。

#### Wubei

- local title顺序固定为殿前、三藏、宝象、黄袍、魁星；live与snapshot共用同一 local title judge与detail ROI，Cloud不得重跑 title template。
- Cloud拥有 yellow text、绿链、目标地图与任务分类；普通怪/黄袍 `targetMapName` 只来自该绿链文本，不用 route hint/目的地浮窗/移动终态伪造。
- 白龙马/显形镜双 prompt只保留第一绿链地图作诊断；暗雷重抽不伪造地图；殿前/宝象等现有 per-task parse/selection顺序照 HEAD。
- 黄袍 chained action同帧构造；fast verify一张 fresh小图、distance=8、miss不全读。

#### Xiuluo

- live API外部顺序保留：active Cloud path优先，disabled/offline才进入 HEAD legacy local green scan。active path的 template事实在 DHXY capture handler产生，Cloud只分绿链/解释。
- 接任务后 tracker与objective必须来自同一 exact full-window snapshot artifact；Cloud只引用 artifact，不拿 `Path`，不补拍。
- 维护未到期先预走并消费原 snapshot结果；维护到期后按 HEAD重新读取当前 live tracker，不复用旧坐标。
- 首次无绿链/点击失败回完整非快捷入口并消费既有objective future；中途失败按修罗基线处理，不把旧路线中间 phase拼入。
- tracker点击仍只表示交给游戏寻路；Service/observer不判断到达、进战或 direct-combat。

### 9. restart contract

- 两端 registry/retained action/workflow state全部 process-local，`restart=no restore`。不扫描 temp PNG、不从 debug path/hash重建 authority、不把旧 prepared action接到新进程。
- Cloud重启丢失 ledger时，旧 taskRun不能由新 assembly接管；DHXY收到 stale/not-found后按 Full R0协调关闭旧 run，再由正常启动注册新 `clientSessionId/taskRunId`。
- DHXY重启丢失 artifact/runtime时，Cloud旧 artifact/materialize handle不能要求“再拍一张当同 occurrence”；旧 run协调终止，新 run重新开始。
- 单边重启、网络分区、late outcome都不能生成 restore/takeover。只有双方新 session/run完成正常注册后才继续。

### 10. 可编译实施 DAG

| Node | 内容 | depends on | compile gate |
| --- | --- | --- | --- |
| `T0` | P2 Full R0 repair与 A capture `artifactIntent` wire/canonical digest最终通过；冻结文件解锁后重新读最新两仓代码 | 外部 P2/A | 本设计不执行。 |
| `T1` | 两仓原子加入 typed tracker intent/fact及 `CaptureRequest/Outcome` canonical wire；普通 CAPTURE bytes保持不变 | T0 | DHXY `mvn -q -DskipTests compile`；Cloud按启动路径 compile/package。两边都成功前不启动。 |
| `T2` | DHXY local image ops、artifact registry、`TaskTrackerPanelService` typed capture执行、handler/codec、exact identity与cleanup；legacy public API仍走HEAD facade | T1 | DHXY compile。 |
| `T3` | Cloud interpretation Service、per-run workflow state/handle activation、OCR/文本链/分类从 `DecisionEngine` 单一抽取；无 caller启用 | T1 | Cloud compile/package。 |
| `T4` | Wuhuan一个 cohort接 typed seam：Runner local wake、same-frame materialize、cache；legacy bridge仍可切回但不能双算 | T2+T3 | 两仓 compile；fresh runtime证据看 exact IDs、读次数、wake。 |
| `T5` | Wubei cohort，再 Xiuluo live+accept-snapshot cohort；每个 cohort独立切换，保持 fallback/phase顺序 | T4 | 每个 cohort两仓 compile；按日志/截图 runtime证据验收。 |
| `T6` | 全 caller切完后删除 tracker raw Base64/map parser与 Cloud template active path，更新迁移矩阵/schema文档 | T5 + fresh runtime acceptance | 两仓 compile/package；无显式测试请求时不新增/运行测试。 |

T1不能在 P2/A 的 canonical files上并行落代码；T4/T5不能和当前 B/host/caller writer重叠。任何节点发现需要新增读取、retry、TTL、negative、phase/fallback差异，必须停下另开行为 CR，不能借“实现本设计”带入。

### 11. 精确未来文件表（本轮全部未改）

#### DHXY

| action | exact file | 责任 |
| --- | --- | --- |
| NEW | `src/main/java/com/bot/dhxy/cloud/remote/RemoteTrackerPanelCaptureIntent.java` | closed OBSERVE/MATERIALIZE wire union。 |
| NEW | `src/main/java/com/bot/dhxy/cloud/remote/RemoteTrackerPanelCaptureFact.java` | closed observation/materialization fact。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureCommandPayload.java` | nullable tracker intent，和 A intent互斥。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteCaptureOutcomePayload.java` | nullable tracker fact。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java` | exact typed encode/decode/validation。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOutcomeEnvelope.java` | tracker fact进入 typed CAPTURE outcome。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java` | canonical intent/fact字段与普通 capture兼容。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` | exact context校验并直接调用现有 Service的真实 capture boundary；不建转发 wrapper。 |
| NEW | `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelArtifactRegistry.java` | bytes/capacity/idempotency/release唯一 owner。 |
| NEW | `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelLocalImageOps.java` | 直接拥有 tracker safety所需现有像素算法；不是 `CloudImageProcessor` wrapper。 |
| NEW | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelArtifactRef.java` | opaque exact artifact/capture occurrence correlation。 |
| NEW | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPreparedActionIdentity.java` | dedicated stable prepared identity；不挪用 intentId。 |
| MODIFY | `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java` | 保留public facade，加入真实typed capture执行；原方法原位复用，避免 prepare/handle/resolve wrapper链。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelReadResult.java` | 追加artifact correlation/interpretation digest，现有字段语义不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerGreenLink.java` | 追加stable link index；adapter仍给现有screen-absolute bbox。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelCacheEntry.java` | exact stable-run/source-decision correlation，保留原geometry/fingerprint/click。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/dialog/PreparedDialogAction.java` | nullable dedicated tracker identity；非tracker caller不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java` | exact publish/consume/cleanup与same prepared identity。 |
| MODIFY | `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java` | existing owner fence + level ready wake；不解释OCR/phase。 |
| MODIFY, T4/T5 only | `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java` | typed result activation，保持HEAD phase/fallback。 |
| MODIFY, T5 only | `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java` | typed live/chained activation，保持async/snapshot/fast verify。 |
| MODIFY, T5 only | `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` | exact accept snapshot ref + live typed activation。 |
| RETIRE, T6 | `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudRequest.java` | 删除raw Base64 request。 |
| RETIRE, T6 | `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecision.java` | 删除string/map compatibility result。 |
| RETIRE, T6 | `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecisionService.java` | 删除old shadow bridge。 |
| MODIFY/RETIRE tracker calls, T6 | `src/main/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowService.java` | classifier由Cloud interpretation单一owner。 |
| MODIFY after wire | `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 记录closed tracker CAPTURE extension与canonical digest。 |
| MODIFY after activation | `docs/superpowers/specs/2026-07-12-service-migration-matrix.md` | 修正template/fingerprint最终归属。 |

`TaskTrackerPanelNegativeResult.java`、resources/templates、Maven与tests不在默认写表：negative不激活，template bytes不改，no-local-test不新增测试。若实现需要触碰它们，先停下重新定范围。

#### dhxy-cloud-brain

| action | exact file | 责任 |
| --- | --- | --- |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TrackerPanelCaptureIntent.java` | Cloud mirror closed union。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TrackerPanelCaptureFact.java` | Cloud mirror typed fact。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CaptureRequest.java` | nullable tracker intent及互斥校验。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CaptureOutcome.java` | nullable tracker fact及OBSERVED/non-OBSERVED不变量。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java` | retained `CAPTURE` 接受closed tracker intent；不增加任意raw port。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java` | package-private typed delegation。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java` | exact canonical request build/redelivery。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java` | intent纳入same-bytes比较。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandEnvelope.java` | typed request wire。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java` | typed fact wire。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java` | 双端同 canonical field order。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/tasktracker/TaskTrackerPanelInterpretation.java` | typed business result。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/tasktracker/TaskTrackerPanelInterpretationService.java` | OCR/text-chain/task classification唯一owner。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/tasktracker/TaskTrackerPanelWorkflowState.java` | per-run handles/occurrence/result/capacity owner；无线程。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java` | 单例state/cap owner装配。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java` | 把exact context交给tracker Service，不暴露mint能力。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudNativeImageProcessor.java` | 只暴露/复用现有OCR像素算法给新Service；不承接local safety fingerprint。 |
| MODIFY then retire case, T3/T6 | `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` | 先让legacy case调用同一interpretation owner，final删除 `TRACKER_PANEL_READER` raw case与Cloud title-template path。 |

Cloud template resources不新增；现有tracker title template只可在legacy adapter存在期间保持未改且最终不被active path读取，删除资源需另做引用核对，不能顺手删。

### 12. pure leaf 判断

**无。**

- wire intent/fact看似纯 type，但它们的 canonical字段顺序、A的 `artifactIntent` 互斥和 P2 exact identity尚由前置文件决定；单独落一边会制造不可编译/不可部署协议分叉。
- local image ops单独落地只会复制当前 Cloud算法且没有生产owner，构成 dormant duplicate；必须和 `TaskTrackerPanelService` safety调用原子切换。
- artifact/identity DTO脱离registry、runtime cleanup和prepared publish fence没有可验证语义；单独实现只是包装层。
- Cloud interpretation result脱离retained handles与typed CAPTURE只能再次走raw request，违反本设计。

因此不为了并行制造 enum/wrapper/helper“叶子”。第一个真实可编译节点是 P2/A完成后的双仓 `T1` typed wire cohort。

### 13. self-QA（不是 reviewer approval）

- P0: 0。没有 Cloud持 HWND/template path、local推进phase、raw request旁路、UNKNOWN伪negative、同帧action补拍或restore/takeover设计。
- P1: 0。exact identity、occurrence/artifact/prepared ID、capacity、uncertain retention、terminal cleanup、snapshot复用、no-thread wake与caller映射均已闭合。
- P2: 0。实施阶段仍有两个外部 gate但不构成本设计缺项：必须等 P2/A canonical代码稳定；必须逐行对比 tracker所用 yellow/green wash/fingerprint算法，保证移到本地时像素等价。
- 未作 `Approved` 判断；本结论只表示 Internal Worker U 已完成 Design #1 交付，等待父级独立审查。

## Parent Design Review #1 - BLOCKED - 2026-07-13T07:21:00-04:00

父级以 HEAD `0114604e`、Full R0 `FINAL APPROVED` 实现和 tracker 全调用面复审。exact-window capture/template/ROI/
fingerprint/prepared-action safety 留 DHXY、OCR/text-chain/classification 归 Cloud、snapshot 不补拍、无线程/无 restore 的总边界
通过；整体仍 **BLOCKED，P0=0/P1=5/P2=2**，Java 继续冻结。

1. **P1：`CAPTURE` 被扩成有副作用的 materialization command。** Design #1 的
   `MATERIALIZE_SELECTED_LINK` 会构造并发布 `PreparedDialogAction`、写 runtime state并发 wake，这不再是 capture/read-only
   合同。不得把状态发布藏进 `CaptureRequest/Outcome`。D2 必须选用 Full R0 retained business-action 的独立 closed typed
   operation/seam，或证明现有真实 operation 可承载；其 action identity、request digest、本地副作用前/后 fence 与
   `NOT_EXECUTED/UNKNOWN` 必须独立闭合，不能开放 raw request/poll/outcome。
2. **P1：prepared identity 与 revision 再次混为一体。** `preparedActionId` 由含 `runRevision` 的 artifact 派生，且 cache
   跨 revision 明写“生成新 prepared identity”。Full R0 已固定 semantic address/action identity 跨 pause/resume 保留，
   `runRevision` 只做当前 request fence。只有 Cloud retained ledger 声明的新业务 occurrence 或可信 NOT_EXECUTED attempt 才能
   提供新 stable action ID；本地 cache/revision 不能自行 remint。D2 须给 observation identity、business action identity、
   mechanical attempt/capture ID、revision fence 四层 exact 映射。
3. **P1：final-consumed 控制方向写反，artifact release 时点不可实施。** 当前链路是 Cloud business consume 后向 DHXY 发布
   final-consumed control，DHXY 本地 ledger apply 后再把 `RemoteFinalConsumedReceipt` 发回 Cloud；本地不会“收到 receipt”。
   D2 必须把 byte release 绑定到 exact local control application（同时保留足够 receipt metadata/bytes 供同 receipt 重投），
   并覆盖 control redelivery、receipt delivery uncertain、terminal cleanup，禁止过早删证据或等一个永远不会收到的对象。
4. **P1：Cloud 并非不保留图片副本。** CAPTURE outcome 在 broker/final-consumption 完成前会保留完整 wire payload；即使
   interpretation 方法返回即 flush decoded image，Cloud broker 仍有 immutable encoded bytes。D2 必须把该真实副本计入
   Full R0 broker/outcome capacity、释放与租户隔离，明确“DHXY 是 raw artifact authority”不等于“Cloud 物理零 bytes”。
5. **P1：所谓现有 observer cadence 可能引入 HEAD 不存在的 tracker 轮询。** D1 允许 Runner 发布“tracker observation 应检查”
   signal，但未给触发源，容易变成额外 capture/read。D2 必须限定 wake 只能来自已存在 caller interest或已发布 exact
   prepared action；不得新增周期 tracker capture、额外 read、negative business truth或 generic attention 替代 typed wake。
6. **P2：容量数值需落真实常量证据。** `8192/256MiB/10000/1000` 等若复用既有 governor/Full R0 默认值，须列 exact
   file/constant/constructor 来源；若是 tracker 新默认值，须说明为什么不会在全局 cap 前制造 1000 tenant bucket 常驻，并给
   same-key-before-quota、原子退账和 terminal bucket removal。
7. **P2：文件表须随 D2 operation 修正。** 移除把 materialization 塞入 CAPTURE 的字段/codec 修改，列真正的 typed action
   DTO/handler/ledger/assembly 修改面；普通 CAPTURE canonical bytes/digest 必须保持不变。

### 下一任务 `W-TTPS-D2`

Internal Worker U 继续使用本日志，只追加 Design Repair #1 Delta 关闭上述 `P1 x5/P2 x2`。唯一写集仍为本 append-only
日志；两仓 Java/Maven/schema/resources/tests、A/B 写集、host/caller 全冻结。先追加新的 `CLAIMED` 再工作；Worker QA 不算
批准，不运行 Maven、不启动任何运行面。

**无已批准业务差异；按基线等价迁移。**

## CLAIMED - `W-TTPS-D2`

- task: `W-TTPS-D2` / Parent Design Review #1 / Design Repair #1 Delta
- claimedAt: `2026-07-13T07:24:21.0850016-04:00`
- 唯一写集: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-tracker-panel-worker-u.md`（append-only）
- 冻结确认: DHXY/Cloud Java、Maven、schema、resources、tests、host/caller、A/B/P2 文件全部只读；不创建 reviewer，不运行 Maven/测试，不启动运行面，不改其它文件，不回滚、覆盖、清理或 `git add/commit`。

## Replacement Internal Worker U2 - CLAIMED - 2026-07-13T10:52:52.2076115-04:00

- task: `W-TTPS-D2` / Parent Design Review #1 / Design Repair #1 Delta
- claimedAt: `2026-07-13T10:52:52.2076115-04:00`
- 唯一写集: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-tracker-panel-worker-u.md`（append-only）
- 接管说明: 原 Internal Worker U 会话在桌面重启后已 `not_found`，本 Replacement Internal Worker U2 从原 `CLAIMED W-TTPS-D2` 接管，仅追加 Design Repair #1 Delta。
- 冻结确认: DHXY/Cloud Java、Maven、schema、resources、tests、host/caller、A/B/P2 文件全部只读；不创建 reviewer/agent，不运行 Maven/测试，不启动运行面，不改其它文件，不回滚、覆盖、清理、暂存或提交。

## DELIVERED - `W-TTPS-D2` / Design Repair #1 Delta - 2026-07-13T11:01:23.1291304-04:00

### 0. Delta 权威与覆盖关系

- 本 Delta 只覆盖 Design #1 的第 3、4、5、6、10、11 节中与 operation、identity、final-consumed、capacity、wake 和文件表有关的内容；其余 committed HEAD inventory、Wuhuan/Wubei/Xiuluo 业务等价要求和 `restart=no restore` 继续有效。
- 已只读复核 DHXY HEAD `0114604e1ff5f15491d2910959c45252e893d04f` 的 `TaskTrackerPanelService`、全部生产 caller、`WindowTaskRunner` tracker prepare/wake 路径。已只读复核 Cloud 当前 Full R0 `FINAL APPROVED` 实现：`RemoteOperation`、`CloudTaskServicePort`、`CloudTaskRetainedActionState`、`CloudTaskRunActionLedger`、`RemoteGameCommandBroker`、`RemoteFinalConsumptionCoordinator`，以及 DHXY `RemoteOperationLedger`、`RemoteCommandPollingLoop`、`RemoteTaskRunRegistry`。
- 真实 Full R0 API 当前只有 `CAPTURE | WINDOW_FACT | EXECUTE_INPUT_BUNDLE`；没有现成 tracker materialization operation。本 Delta 不臆造“现有 API 已能承载”的事实，而是明确把两个新 operation 列为必须双仓原子实施的协议扩展。
- 本轮仅追加本日志；没有改 Java/Maven/schema/resources/tests/host/caller，没有运行 Maven/测试，没有启动运行面，没有创建 reviewer/agent，没有暂存或提交。

**无已批准业务差异；按基线等价迁移。**

### 1. P1-1 修复：普通 `CAPTURE` 严格只读，materialization 为独立 retained action

#### 1.1 四条 operation 边界

1. 现有 `CAPTURE`、`CaptureRequest`、`CaptureOutcome` 及其普通 wire 完全冻结：仍只捕获 caller 声明的一个矩形，绝不拖动 panel、不写 `WindowRuntimeContext`、不构造/发布 `PreparedDialogAction`、不发 wake。
2. 新增 closed typed `TASK_TRACKER_READ` operation：它是 tracker 专用的 retained operation，request 只能选 fixed `readProfile/sourceFrame`、exact existing-snapshot ref 或 live binding、以及 HEAD 已有的 `allowPanelReposition`。该 operation 直白声明“可按 HEAD 顺序执行最多一次 panel drag”，因此不把这个已有输入副作用伪装成普通 `CAPTURE`。它在同一 handler invocation 中保持 HEAD `anchor narrow -> expanded fallback -> optional drag -> panel/detail crop` 顺序、物理读取次数和 500ms drag sleep，不新增重读。
3. 新增 closed typed `TASK_TRACKER_MATERIALIZE_ACTION` operation：只接受 exact retained artifact ref、Cloud interpretation digest、closed selected-link DTO、`DialogOperation.TASK_TRACKER_PATHING`、target keyword。它是唯一允许从 retained detail bytes 构造并幂等发布 `PreparedDialogAction`、写 runtime prepared state、发 `PREPARED_ACTION_READY` 的路径。
4. `WINDOW_FACT/EXECUTE_INPUT_BUNDLE` 语义不变。不用 input bundle 绕过 tracker materialization，不开 raw request/poll/outcome port。

#### 1.2 closed typed action 与副作用 fence

- `TrackerPanelMaterializeRequest` 不是字符串 map，不含 local path/template path/HWND/screen-absolute Cloud click。`selectedLink` 只能是 exact observation 中的 stable index + window-relative rect/click；本地再校验 detail ROI、`1024x768`、binding 和 selected-link digest。
- 本地 `TaskTrackerPanelMaterializationHandler` 在任何 runtime 写之前、validation crop 之前、真正 publish 前各做 exact scope/taskRun/taskType/window tuple/stopEpoch/runRevision 与 `WindowTaskRunner` owner guard。前置拒绝是 `NOT_EXECUTED/TASK_RUN_MISMATCH|WINDOW_BINDING_CHANGED`；已经开始但无法证明 publish 与否时是 `UNKNOWN`，绝不重投新 identity。
- 本地独立 `TaskTrackerPanelMaterializationLedger` 按 `(stable run, actionId)` 存 first requestDigest、artifact/decision/link digest、publish disposition 和 exact outcome。same actionId + same bytes 返回同 outcome，并最多重发同 identity 的 level wake；same actionId + different bytes 在副作用前 `ACTION_ID_REUSE/IDEMPOTENCY_CONFLICT`。
- `TASK_TRACKER_MATERIALIZE_ACTION` 不发物理鼠标输入；Task 仍按 HEAD 消费 prepared action 后通过现有串行输入队列点击。Service/handler 不推进 Task phase。

### 2. P1-2 修复：四层 identity 完全分离，revision 不 remint

| 层 | exact owner / 内容 | revision 规则 |
| --- | --- | --- |
| observation identity | Cloud `TaskTrackerPanelWorkflowState` 通过 `CloudTaskRetainedActionState.ActionRecord` 打开 fixed tracker-read semantic slot；稳定身份为 stable run + `phaseCode/actionSlot/occurrence` + retained read `actionId`。 | 不含 `runRevision`。pause/resume 不新建 occurrence。只有既有 caller interest 要求的新业务观察，且上一 occurrence 已 final-consumed，ledger 才打开 `completed+1`。 |
| stable business action identity | 独立 tracker-materialize semantic slot + occurrence + Full R0 retained `actionId`；`PreparedDialogAction` 保存这个 actionId/semantic address 的 dedicated typed identity。 | 不从 artifactId、interpretationDigest、cache fingerprint 或 revision 派生；跨 revision 保留同 actionId。local cache 无铸号权。 |
| mechanical request/capture identity | Full R0 ledger 对每 attempt 保留 `requestId`；`TASK_TRACKER_READ` 另有 ledger-minted `captureId`；materialize 只有 requestId，不冒充 capture。artifactId 只绑 exact observed bytes/SHA/ROI。 | 只在已 final-consumed 的可信 `NOT_EXECUTED` 续 attempt 时更换 requestId/captureId，actionId/occurrence 不变；`UNKNOWN` 不更换任何 ID。 |
| runRevision fence | `CloudTaskRunExecutionContext`/request context 和 DHXY registration 的 exact current revision。 | 只参与 requestDigest 和 Cloud enqueue/final-dispatch/DHXY pre-side-effect/post-derive fence。它不进 semantic slot/action ID/artifact stable key，不能因 resume 或 cache hit 产生新业务 action。 |

跨 revision 映射为：未 bind request 的原 handle 可用 Full R0 `planActiveInvocation` 更新 exact context；已 bind 的旧 revision request 必须稳定拒绝，等其可信 `NOT_EXECUTED` final-consumed/compacted 后使用现有 renewal，仍保留 actionId/occurrence。未解 `UNKNOWN`、local cache 命中、新 artifact SHA 都不允许 remint。

### 3. P1-3 修复：Full R0 final-consumed 方向与 bytes release

真实方向固定为：

`Cloud business consume -> RemoteFinalConsumptionCoordinator -> broker FINAL_CONSUMED control -> DHXY RemoteCommandPollingLoop -> RemoteOperationLedger.applyFinalConsumedAck -> DHXY release bytes + retain receipt outbox evidence -> RemoteFinalConsumedReceipt -> Cloud compact -> RemoteFinalConsumedReceiptAck`。

1. Cloud 业务完成 typed tracker interpretation 或 materialize outcome 消费后，继续调真实 `consume...Final`；`RemoteFinalConsumptionCoordinator` 保留并发布 exact `RemoteFinalConsumedAck`。DHXY 不等一个“receipt from Cloud”。
2. DHXY `applyFinalConsumedAck` 先验证 exact outcome/control，再预留现有 receipt outbox slot（真实 `MAX_RECEIPT_OUTBOX=64`）。对产生 tracker bytes 的 read ack，它以 requestId/captureId/outcomeDigest 调用 artifact ledger 的 closed apply participant：删除 raw encoded bytes，但保留不含图片的 `ReleaseWitness(artifactId,captureId,imageSha256,chargedBytes,ackDigest)` 和已 canonicalized 的 exact `RemoteFinalConsumedReceipt` 对象。
3. local request/detail/frontier 更新、bytes 退账、release witness 和 receipt outbox 入队是一个固定 lock order 的 prepare/commit：`RemoteOperationLedger.monitor -> TaskTrackerPanelRemoteArtifactLedger.monitor`。任一 prevalidate/cap 失败都是零写入；commit 后的同 ack 命中 frontier `lastReceipt`/release witness，不二次退账。
4. receipt delivery uncertain 时，`RemoteCommandPollingLoop.flushFinalConsumedReceiptOnce` 按真实实现把同一 retained receipt handle 放回 READY；raw bytes 不恢复，receipt 完整 bytes/digest、release witness 和 run outbox count 继续保留，供同 receipt 重投。
5. Cloud 收到 receipt 并完成 broker/ledger compaction 后，才退 Cloud retained encoded-byte charge，并把当前 full outcome witness 换为 digest-only compacted witness。Cloud receipt ack 回到 DHXY 后，DHXY 才删 receipt outbox/release witness。
6. terminal cleanup 只在 exact run terminal/absent，current detail、uncertain、control、receipt outbox 和 release witness 全空时 O(1) 删 run bucket。terminal 不强删 uncertain bytes/回执，不扫前缀，不加 TTL/LRU/takeover。

### 4. P1-4 修复：Cloud 真实 retained encoded image bytes 计费

- 说法修正为：DHXY tracker artifact ledger 是 raw artifact **business authority**，但 Cloud 在 broker/final-consumption 结束前确实物理保留 encoded outcome bytes。`CaptureOutcome`/tracker-read outcome constructor 的 defensive copy、`PendingCommand.terminalOutcome/lateResolution` 与 current compacting witness 都必须计入真实 owner；不再写“Cloud 物理零 bytes”。
- `TaskTrackerPanelWorkflowState` 只保留 identity、artifact ref、SHA/ROI、typed interpretation/digest，不保留 `byte[]`。OCR 在 caller thread 取防御性 copy，方法返回前 flush/drop decoded image；这不抵消 broker retained copy 的配额。
- `RemoteGameCommandBroker` 对所有会在 broker 中保留 encoded image bytes 的 outcome（普通 `CAPTURE` 和 `TASK_TRACKER_READ`）在同一 `stateLock` 下计 count+bytes。transient HTTP `JsonNode`/decode buffer 在方法返回前丢弃，不算 retained；若日后新增另一个 retained defensive copy，必须按实际 copy 再计一份，不得藏在 workflow DTO。
- exact outcome redelivery 先命中 `(scope,operation,requestId)` 及 digest 再做 quota，同对象/同 bytes 不重复计费。新 terminal/late-resolution bytes 只在新 retained copy 入主索引前预留；容量不足 fail closed 为 non-business `UNKNOWN/BROKER_CAPACITY_EXCEEDED`，不伪造 OCR/template miss。

### 5. P1-5 修复：零新周期 tracker capture/read

- 不设计 `TRACKER_OBSERVATION_DUE`、generic attention、scheduler、watcher thread、sleep/retry loop 或 Cloud poller。`TASK_TRACKER_READ` 只能在 HEAD 已有 caller 当前调用栈的 interest 中同步激活：
  - `FiveRingTaskV2` 现有 title/prepare 直接调用（HEAD 含 L2380、L2906 路径）；
  - `WindowTaskRunner` 现有 `refreshTaskTrackerPreparationSignal` 调用点 L1033，仅保留 L1494-L1537 的 `WUHuan_V2`、无 dialog request、无 combat/pathing、无 current prepared action、owner-current 全部 guard；迁移不再加一个 cadence 或在同 tick 多读一次；
  - `WubeiTask`/`XiuluoTaskV2` 现有 live/snapshot/fast-action caller，仍由对应 phase 进入，不由 runner 新建通用 tracker 巡检。
- 远端等待只复用 Full R0 现有 broker + DHXY `RemoteCommandPollingLoop`；没有 tracker-specific poll endpoint。
- 唯一可新发的 wake 是 local `TASK_TRACKER_MATERIALIZE_ACTION` 已幂等 publish exact action 后的现有 `PREPARED_ACTION_READY / TASK_TRACKER_PATHING / target`。重投同 actionId 只是 level wake，Task 以 dedicated identity 只消费一次。
- 不激活 HEAD 未产出的 `TaskTrackerPanelNegativeResult`，不发新 `TASK_TRACKER_NEGATIVE_READY`，不把 `UNKNOWN/STOPPED`、runner negative 或 `TASK_ATTENTION_REQUIRED` 变成业务事实。

### 6. P2-1 修复：容量常量、owner 与账务原子性

#### 6.1 真实现有常量证据

| 现有 owner | exact file/constant | 真实含义 |
| --- | --- | --- |
| Cloud retained semantic action | `dhxy-cloud-brain/.../remote/CloudTaskRunActionLedger.java` `DEFAULT_RETAINED_ACTION_LIMIT=10_000` | assembly 单例 ledger 的 retained semantic-slot hard cap；`acquire` 先查同 key/current record，后查新 slot quota。 |
| Cloud lifecycle activation | `.../CloudTaskRunRetainedLifecycleActivationAdapter.java` `DEFAULT_GLOBAL_RETAINED_LIMIT=10_000`, `DEFAULT_OWNER_RETAINED_LIMIT=1_000` | owner 是 tenant/user/device；`activateInitial` 先查 exact stable key，后 quota，K/H 失败原位删 entry、退 owner/global，owner 回 0 删 bucket。 |
| Cloud broker | `.../RemoteGameCommandBroker.java` `DEFAULT_GLOBAL_RETAINED_REQUEST_LIMIT=10_000`, `DEFAULT_OWNER_RETAINED_REQUEST_LIMIT=1_000`, pending `64`, retained action `10_000/1_000`, route `1_000/64` | Full R0 request/action/route 真实账本；这些是 record caps，不是 image-byte caps。 |
| 已有 Cloud artifact governor | `.../host/CloudArtifactCapacityGovernor.java` `ROOT_MAX_BYTES=256L*1024*1024`, `ROOT_MAX_COUNT=8192`, `SCOPE_MAX_COUNT=512` | 数值只作为本 Delta 新 broker encoded-byte cap 的已存在证据；tracker 不复用其 FIFO/eviction/store authority。 |
| DHXY semantic/final receipt | `DHXY/.../RemoteOperationLedger.java` `MAX_SEMANTIC_SLOTS=1_000`, `MAX_CURRENT_DETAILS=64`, `MAX_RECEIPT_OUTBOX=64` | 单 client-session local ledger 上限；final control apply 必须先预留 receipt slot。 |
| DHXY run registry | `DHXY/.../RemoteTaskRunRegistry.java` `DEFAULT_GLOBAL_CAPACITY=10_000`, `DEFAULT_OWNER_CAPACITY=1_000` | `register` 先查 same taskRunId，仅新 entry 查 quota；`releaseTerminal/unregister` 退 owner，为 0 删 bucket。 |

#### 6.2 新 tracker/broker hard cap（明确标注为新，不冒充现有常量）

| 新常量 | 值 | 依据/作用 |
| --- | --- | --- |
| local `TRACKER_SINGLE_ENCODED_MAX_BYTES` | `512 KiB` | HEAD panel 最大 `182x338`，detail `175x65`，validation `24x16`；512KiB 已大于最大 panel 的未压缩 RGB 量级。 |
| local `TRACKER_ARTIFACT_MAX_BYTES` | `1 MiB` | 一 observation 最多保留 HEAD 顺序的 panel/detail derived frames；accept snapshot 只引用原 artifact，不复制 full-window bytes。 |
| local per-run | `8 artifacts / 8 MiB` | 跟 D1 保持一致的 tracker-specific 新限制；大于单个 HEAD caller chain 的 current live/snapshot/chained 并存需求，又不允许一 run 吃满 owner/global。 |
| local per owner | `512 artifacts / 32 MiB` | count 对齐已有 `SCOPE_MAX_COUNT=512`；32MiB 是新的租户 byte isolation，为 global 256MiB 的 1/8，一 owner 不能耗尽全局。 |
| local global | `8192 artifacts / 256 MiB` | 数值对齐已有 `CloudArtifactCapacityGovernor` 的真实 root cap；新 local ledger 无 eviction。 |
| Cloud broker encoded outcome | global `8192 / 256 MiB`, per owner `512 / 32 MiB` | count/global bytes 取真实 governor 值；owner bytes 32MiB 是新租户隔离限制。它叠加在 broker 现有 record caps 之上，不取代 `10_000/1_000/64`。 |

不再设立 D1 的“`retained tenant buckets=1000`”独立 bucket cap。local artifact owner bucket 只在首个 artifact 成功 admission 时创建，且每个 bucket 至少对应一个被 global `8192` 计费的 artifact，因此不可能在 global count 之前制造空的常驻 tenant bucket。Cloud broker 不新建第二张 tenant map，直接扩展现有 `usageByOwner(InputFenceScope=tenant/user/device)`；usage 全零时删 map entry。

#### 6.3 same-key-before-quota、原子退账、terminal removal

1. local artifact admission：先在 registry lock 中查 `(stableRun, observation actionId, requestId, captureId)`；same key + same SHA/bytes 返原 entry 且不计费，same key + different bytes 拒绝；只有真新 key 才同时检 run/owner/global count+bytes，全部通过后一次写 entry/counters。
2. Cloud broker outcome admission：沿用真实 `handleOutcome` 的 current/compacted request 命中顺序；在 `stateLock` 中先处理 same outcome digest duplicate/已有 UNKNOWN late-resolution，只对新 retained encoded copy 预留 owner/global count+bytes。
3. 任一后续索引写入失败，在同 lock 把本次预留的 count/bytes 全部原子退回；负数/不一致立即 `IllegalStateException`，不带部分 entry 继续。
4. final receipt compaction 一次性从 full retained outcome 退 encoded count/bytes，并替换为不含 image bytes 的 digest-only witness；control/receipt uncertain 前不退 Cloud bytes。
5. exact run terminal 且 all detail/control/receipt/release-witness 全空时，Cloud `runStates`/local run map O(1) remove，同时退 run/owner/global；owner 为全零即 remove，不保留空 bucket。无 TTL、LRU、FIFO eviction 或 uncertain 强删。

### 7. P2-2 修复：独立 typed action 精确未来文件表

#### 7.1 DHXY（本轮全部未改）

| action | exact file | D2 责任 |
| --- | --- | --- |
| NEW | `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerReadCommandPayload.java` | closed tracker read profile/source/snapshot/reposition contract；不是普通 CAPTURE payload。 |
| NEW | `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerReadOutcomePayload.java` | ordered frame bytes/SHA/ROI/template/fingerprint/mechanical fact 及 ledger-minted captureId。 |
| NEW | `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerMaterializeCommandPayload.java` | closed artifact/decision/link/operation/target action DTO；无 image bytes/path/HWND。 |
| NEW | `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerMaterializeOutcomePayload.java` | publish/reposition-independent typed disposition、validation digest、exact action identity echo。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOperation.java` | 原子加 `TASK_TRACKER_READ`/`TASK_TRACKER_MATERIALIZE_ACTION`。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java` | 两个新 operation 各自 closed codec；普通 CAPTURE branch 逐字不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java` | 新 operation canonicalizer；tracker-read outcome digest 排除自己的 image bytes；普通 CAPTURE tree/key order/digest 不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOutcomeEnvelope.java` | 允许两个新 closed payload；common identity 不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteFinalConsumedAck.java` | `captureId` 对 `TASK_TRACKER_READ` 也 required；普通 CAPTURE ack canonical 不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` | generic exact registration/claim/fence 后路由新 typed handler；不内联 materialize 业务。 |
| NEW | `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelRemoteArtifactLedger.java` | raw bytes authority、capacity/idempotency、final-control apply/release witness、terminal bucket removal。 |
| NEW | `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelMaterializationLedger.java` | stable actionId/requestDigest/publish disposition 幂等权威。 |
| NEW | `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelMaterializationHandler.java` | pre/post fence、same-frame validation、runtime publish 和 exact level wake。 |
| MODIFY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java` | 新 operation semantic detail、artifact release apply participant、receipt outbox/witness 清理；不复制 materialization ledger。 |
| MODIFY | `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java` | public facade 保持 HEAD；原位接 typed read/materialize，不建 wrapper chain，不改 anchor/crop/cache/fast-verify 顺序。 |
| NEW | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelArtifactRef.java` | exact observation/artifact/capture/SHA/ROI 不可变引用。 |
| NEW | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPreparedActionIdentity.java` | stable materialize actionId/semantic address；不用 intentId、artifactId 或 revision。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelReadResult.java` | opaque artifact/observation correlation；现有 public 字段语义不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerGreenLink.java` | stable link index + window-relative typed fact；兼容 facade 仍输出 screen-absolute。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelCacheEntry.java` | 只存 observation/action correlation 和 fingerprint/geometry；无 remint 能力。 |
| MODIFY | `src/main/java/com/bot/dhxy/model/dialog/PreparedDialogAction.java` | nullable dedicated tracker action identity；普通 dialog action 不变。 |
| MODIFY | `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java` | same actionId 幂等 publish/consume/cleanup，exact run terminal 清理。 |
| MODIFY, activation only | `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java` | 复用 HEAD 唯一 caller cadence/guards；只对 exact materialized action 发 level wake，不加 read tick。 |
| MODIFY, cohort only | `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java` | existing interest 进 typed facade，phase/fallback/negative 语义不变。 |
| MODIFY, cohort only | `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java` | live/snapshot/chained 切换，快验仍一读/distance=8/miss terminal。 |
| MODIFY, cohort only | `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` | exact accept snapshot/live 切换，不补拍、不改快捷/完整入口 fallback。 |

**明确 NOT MODIFY for ordinary CAPTURE canonical contract**：`RemoteCaptureCommandPayload.java`、`RemoteCaptureOutcomePayload.java` 不增 tracker field；普通 CAPTURE payload 仍是 `captureId/region/imageFormat/capturePurpose`，outcome 仍是现有八个 key。

#### 7.2 dhxy-cloud-brain（本轮全部未改）

| action | exact file | D2 责任 |
| --- | --- | --- |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerReadRequest.java` | closed typed tracker read request，独立于 `CaptureRequest`。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerReadOutcome.java` | typed mechanical observation + encoded frames/captureId。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerMaterializeRequest.java` | closed retained business-action request。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerMaterializeOutcome.java` | typed publish/stale/unknown result。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteOperation.java` | 原子加两个 operation。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteRequest.java` | sealed permits 两个新 request。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteOutcome.java` | sealed permits 两个新 outcome。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandEnvelope.java` | 新 operation payload branch；普通 CAPTURE branch 逐字不变。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java` | 新 operation closed keys/decode；普通 CAPTURE exact-eight-key 合同不变。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java` | 新 operation canonical/digest；普通 `withComputedRequestDigest(CaptureRequest)` 和 CAPTURE digest 不变。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteFinalConsumedAck.java` | `TASK_TRACKER_READ` 的 captureId 约束；旧 operation canonical 不变。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java` | package-private typed read/materialize delegation；不开 raw port。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java` | 独立 `TaskTrackerReadAction`/`TaskTrackerMaterializeAction` opaque handle、invoke/consumeFinal。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java` | fixed typed retain/renew/newHandle；occurrence owner 继续是 `ActionRecord`。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunActionLedger.java` | 为 tracker read mint captureId；materialize actionId 跨 revision 保留；只可信 NOT_EXECUTED renewal。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java` | 两个新 request 的 retained build/redelivery/outcome correlation。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java` | 新 operation admission，CAPTURE+tracker-read encoded count/bytes、tenant isolation、atomic rollback/release、digest-only compacted witness。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/tasktracker/TaskTrackerPanelInterpretation.java` | typed Cloud business result；无 bytes owner。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/tasktracker/TaskTrackerPanelInterpretationService.java` | OCR/text-chain/task classification 唯一 owner；无线程。 |
| NEW | `src/main/java/com/yueyunfe/dhxy/cloudbrain/tasktracker/TaskTrackerPanelWorkflowState.java` | fixed read/materialize semantic slots、observation/action 四层映射、typed result；不存 byte[]。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java` | 在真实 authority assembly 中装配 workflow state/typed handles，不给 caller mint 权。 |
| MODIFY | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java` | 仅交付不可铸号的 typed tracker workflow capability。 |
| MODIFY then retire tracker case | `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` | rollout 期调同一 interpretation owner，final 删 raw `TRACKER_PANEL_READER`。 |

**明确 NOT MODIFY for ordinary CAPTURE canonical contract**：Cloud `CaptureRequest.java`/`CaptureOutcome.java` 不加 tracker intent/fact，不改普通 CAPTURE request/outcome bytes。`RemoteFinalConsumedReceipt.java`/`ReceiptAck.java`、`RemoteCommandPollingLoop.java`、`RemoteFinalConsumptionCoordinator.java` 的方向/运输算法可直接复用，不为 tracker 建新方向或 endpoint。

### 8. 修正后实施 DAG 与失败闭合

| node | 原子交付 | depends on / gate |
| --- | --- | --- |
| `D2-T0` | 冻结后重读 P2/A/B 最新 Full R0；确认新 enum 值、final-ack captureId 规则和 canonical field order | 前置 writer 解锁；本轮不执行。 |
| `D2-T1` | 双仓两个 new operation DTO/enum/sealed union/envelope/digest/final-ack、retained handle/ledger/executor 同波 | 普通 CAPTURE canonical regression 必须逐字比对；两仓 compile。 |
| `D2-T2` | DHXY artifact/materialization ledgers + typed handler + `RemoteOperationLedger` final apply participant + local assembly wiring | D2-T1；DHXY compile。 |
| `D2-T3` | Cloud interpretation/workflow + broker encoded-byte accounting + authority assembly | D2-T1；Cloud compile/package。 |
| `D2-T4` | Wuhuan 单 cohort 用现有 caller interest 切换，统计物理 anchor/panel/detail 读次数与 exact wake | T2+T3；双仓 compile；fresh runtime 只看日志/截图证据。 |
| `D2-T5` | Wubei live/snapshot/chained，再 Xiuluo live/accept snapshot；每 cohort 独立切换 | T4 过后；不变 fallback/读次数。 |
| `D2-T6` | 删 raw Base64 tracker bridge/DecisionEngine case，更新 protocol/schema/matrix | 全 cohort fresh acceptance 后。 |

额外 failure closure：

- tracker read 在 optional drag 副作用前拒绝 -> `NOT_EXECUTED`；drag 已提交但 outcome 不明 -> `UNKNOWN`，同 IDs 保留，不再拖/不再读。
- tracker read 成功但 title miss -> typed mechanical observed miss；只有 Cloud baseline interpreter 可产 confirmed no-task。capture/template/codec/cap 失败 -> `UNKNOWN`。
- materialize pre-fence stale -> `NOT_EXECUTED`；validation/derive 开始后不明 -> `UNKNOWN`；明确安全拒绝只返 typed no-publish，不重读 artifact。
- same actionId redelivery -> exact retained outcome + same level wake；same actionId/different bytes -> 副作用前 conflict。
- Cloud/DHXY encoded-byte cap -> zero new admission、no eviction、no queue/retry、no business negative。
- final control redelivery -> same local receipt/release witness；receipt uncertain -> same receipt bytes 重投；terminal 不越过 outbox/witness 删证据。

### 9. Parent Review #1 关闭矩阵

| review 项 | D2 关闭点 |
| --- | --- |
| P1-1 CAPTURE 藏 materialization 副作用 | 普通 CAPTURE 完全冻结且只读；tracker read 显式承载 HEAD optional reposition；materialize 为独立 operation/DTO/handler/ledger/handle。 |
| P1-2 identity/revision 混同 | observation/action/request-capture/revision 四层分离；actionId/occurrence 跨 revision 保留；cache/UNKNOWN/revision 无 remint 权。 |
| P1-3 final-consumed 方向错 | 改为 Cloud control -> DHXY apply/release + retained receipt evidence -> Cloud receipt/compact，覆盖 redelivery/uncertain/terminal。 |
| P1-4 Cloud 图片副本未计费 | broker/outcome retained encoded copies计 global+owner count/bytes；workflow 不留 byte[]；receipt compact 后才退。 |
| P1-5 潜在新 tracker 轮询 | 只从 HEAD 已有 caller stack/Runner 现有 guarded call进 read；无 observation wake/cadence；只 exact prepared action level wake。 |
| P2-1 容量无真实常量/owner | 列出 7 组 exact source constant/owner；新值明标新默认与依据；same-key-before-quota、atomic rollback、zero-owner/terminal removal。 |
| P2-2 文件表仍混 CAPTURE/action | 双仓列独立 read/action DTO/handler/ledger/assembly改动；`CaptureRequest/Outcome` 及 DHXY capture payload 明确 NOT MODIFY，普通 CAPTURE canonical bytes/digest 零变化。 |

### 10. Replacement Worker U2 self-QA（不是 reviewer approval）

- P0: 0。未留 raw request/poll/outcome bypass，未让 Cloud 持 HWND/template path，未让 local 解释 phase，未把 uncertain 伪造 negative，未新增 restore/takeover。
- P1: 0。Parent Review #1 的 CAPTURE 副作用、identity/revision、final-consumed 方向、Cloud bytes 副本、轮询触发源 5 项已在本 Delta 逐项闭合。
- P2: 0。已给真实常量/owner/原子账务，已修正精确文件表并明确普通 CAPTURE canonical 零变化。
- 本 self-QA 不构成 `Approved`，不替代父级独立 review。本轮未执行 compile/test/runtime，因为用户明确要求 Java/Maven/schema/resources/tests/host/caller 全冻结且不运行 Maven/测试/运行面。

## Parent Design Review #2 - BLOCKED - 2026-07-13T11:13:00-04:00

父级以当前 Full R0 final-consumed 实现、DHXY `RemoteOperationLedger` 与 D2 两阶段 read/materialize 时序复审。
普通 `CAPTURE` 与 tracker materialization 已拆开、identity/revision、wake source 和 final-consumed 方向已纠正；整体仍
**BLOCKED，P0=0/P1=3/P2=1**，Java/Maven/schema/resources/tests/host/caller 继续冻结。

1. **P1：read final-consumed 会先删掉 materialize 必需的同帧证据。** D2 `:510-515` 规定 Cloud consume
   `TASK_TRACKER_READ` 后，DHXY apply control 即删除 raw encoded bytes；但 `:483/488-490` 的后续
   `TASK_TRACKER_MATERIALIZE_ACTION` 又只携 artifact ref，并要求本地用 retained detail ROI 做 same-frame validation。
   D2 没有定义 read-final mutation 如何原子建立 materialize dependency lease，也没有 refcount/owner transition。正常成功路径因此
   可能“read final -> 删图 -> materialize 找不到 artifact”，只能补拍或失败，二者都破坏 HEAD 同帧/不补拍语义。D3 必须给
   exact owner transaction：Cloud 在 consume read final 的 checked mutation 中提交 stable materialize dependency，local apply read
   control 只退 Cloud/wire copy而保留受 lease 保护的 local validation artifact；仅 materialize final-consumed、显式可信取消或 exact
   terminal 清空依赖后才删最后本地 bytes。覆盖 control/receipt redelivery、UNKNOWN、pause/resume 和 terminal，且不得靠 TTL。
2. **P1：两侧 byte quota 都在副作用之后才 admission。** local `:562` 以已得到的 SHA/bytes 做新 artifact admission，Cloud
   `:522/563` 在新 terminal/late outcome 入索引前才预留 encoded bytes；但 `TASK_TRACKER_READ` 在此之前可能已做 optional drag
   和 capture。若此时容量拒绝，副作用已经发生、结果又无法 retained，stable action 会落 UNKNOWN 且本地证据无法 final-consume，
   形成可避免的长期占用。D3 必须按 closed `readProfile` 给 hard worst-case count/bytes，在 local capture/drag 前和 Cloud final
   dispatch 前分别 reserve；actual outcome commit 后 settle 实际值并退差额。same-key 命中先于新 quota；任何 reserve 失败必须零
   capture/零 drag/零新 artifact。普通 CAPTURE 是否纳入同一预留也要写明，不得只对 post-outcome copy 记账。
3. **P1：local 持久副本计费漏掉 `RemoteOperationLedger.terminalOutcome.payload`。** 当前
   `RemoteOperationLedger.complete:193-225` 将完整 `RemoteGameOutcomeEnvelope` 留在 `SemanticDetail.terminalOutcome`，其
   `payload` 是 `JsonNode`；`LocalRemoteGameCommandHandler.terminal:756-791` 已把 `byte[]` 编成独立 JSON/Base64 节点。
   D2 又新增 raw-byte artifact ledger，却只给 artifact ledger count/bytes，无法把这两个物理保留对象当成一份。D3 必须选择：
   消除持久重复（保持 wire 序列化为 transient），或把 artifact raw bytes + ledger JsonNode/Base64 + 任何 defensive copy 按真实
   footprint 全部计入同 owner/global reservation，并给各 copy 的唯一释放点；Cloud constructor defensive copy 同理逐项列明。
4. **P2：closed capability 仍缺可机械编码的 API 与 profile 上界。** 文件表写“context 交付 typed tracker workflow
   capability”，但未给 capability 的 public type/constructor ownership、`read/materialize/consumeFinal` exact signature，也未定义
   `readProfile` enum 对应的最大帧数/尺寸（而 P1-2 的 pre-reserve 正依赖它）。D3 列精确 FQCN、方法签名、opaque handle 由 remote
   package assembly 铸造并跨 revision 复用的链；business package 仍不得获得 raw ledger/mint/renew/request/poll/outcome。

### 下一任务 `W-TTPS-D3`

同一 Internal Worker U2 先追加 `CLAIMED`，只追加 Design Repair #2 Delta 关闭 `P1 x3/P2 x1`；唯一写集仍为本日志。
两仓 Java/Maven/schema/resources/tests、A/B/V 写集、host/caller 全冻结，不运行 Maven/测试/运行面。Worker QA 不算批准。

**无已批准业务差异；按基线等价迁移。**
## Replacement Internal Worker U2 - CLAIMED - 2026-07-13T11:16:37-04:00

- task: `W-TTPS-D3`
- claimedAt: `2026-07-13T11:16:37-04:00`
- 唯一写集：`D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-tracker-panel-worker-u.md`
- 接管说明：原 U 会话桌面重启后 `not_found`；Replacement Internal Worker U2 继续同一固定日志的 Parent Design Review #2 返修，不创建 reviewer/agent，不触碰任何冻结源码或运行面。

## Design Repair #2 Delta

本 Delta 只修 Parent Design Review #2 的 `P1 x3/P2 x1`，未改写 D2 已关闭的 CAPTURE 只读、四层 identity、Full R0 final-consumed 方向、wake source 与普通 CAPTURE canonical 契约。实现仍未获准；以下所有 Java/FQCN 都是下一实现波的精确契约，不是本轮源码变更。

### 0. 只读底账与冻结声明

- DHXY 只读基线为 `0114604e1ff5f15491d2910959c45252e893d04f`。HEAD `TaskTrackerPanelService` 的真实上界是 game `1024x768`、panel `182x338`、detail width `175`、五环/五倍 detail height `65`、修罗 detail height `40`；这些常量来自 `GAME_CLIENT_WIDTH/HEIGHT`、`TRACKER_PANEL_FROM_ANCHOR_*`、`TASK_DETAIL_WIDTH`、`WUHUAN_TRACKER_BLOCK_HEIGHT`、`XIULUO_TRACKER_BLOCK_HEIGHT`，不是新估值。
- Cloud 只读工作树 HEAD 为 `3b988caa010254973e03342272e6d1d6a9685b01`；其 dirty/untracked 全部保持原样。现有 Full R0 public facade 真实签名仅为 `CloudTaskServicePort.readWindowFact(...)`、`capture(...)`、`executeInputBundle(...)`；三个 `consume*Final(...)` 是 package-private，最终都进入 `RemoteFinalConsumptionCoordinator.consumeFinal(...)`。本 Delta 不臆造这些 API 已存在，而是给 tracker 专用 public capability 的新增精确契约。
- 当前 DHXY `RemoteOperationLedger.complete(...)` 确实把完整 `RemoteGameOutcomeEnvelope` 留在 `SemanticDetail.terminalOutcome`，`LocalRemoteGameCommandHandler.terminal(...)` 确实先经 `RemoteOperationPayloadCodec.toPayloadTree(...)` 形成 `JsonNode`；Cloud `RemoteCommandOutcomeEnvelope` 会 `payload.deepCopy()`，`CaptureOutcome` 构造和 accessor 都会复制 `byte[]`，`RemoteGameCommandBroker.CompactedWitness` 当前仍持完整 `RemoteOutcome`。D3 不能把这些物理对象假装成同一份。
- 本轮唯一写入仍是本日志；Java/Maven/schema/resources/tests/host/caller、A/B/V 写集均冻结，未运行 Maven、测试或任何运行面。

### 1. read final-consumed 与 materialize dependency lease

#### 1.1 两侧唯一 owner 与状态

Cloud 的依赖 owner 是 package-private `TaskTrackerPanelRetainedActionState.MaterializationDependency`；它只保存 `taskRunId + observationId + artifactId + artifactDigest + stableMaterializeActionId + semanticAddress + leaseDigest`，不保存图片。DHXY 的字节 owner 是 `TaskTrackerPanelRemoteArtifactLedger.ArtifactEntry`；每个 artifact 恰有一个状态：

```text
READ_OUTCOME_OWNED
  -> MATERIALIZE_DEPENDENCY_OWNED(stableMaterializeActionId, leaseDigest)
  -> RELEASED(digest-only release witness)
```

不是 refcount 猜测，也没有两个 owner 同时给同一 `byte[]` 计费。read final 只能把 owner 从 `READ_OUTCOME_OWNED` 转给 materialize dependency，或在确认没有 materialize 时直接转 `RELEASED`；它不能无条件删 bytes。

#### 1.2 Cloud checked-final 原子事务

`TaskTrackerPanelCapability.consumeFinal(TaskTrackerReadResult, ...)` 在现有 `RemoteFinalConsumptionCoordinator` 的 checked-final 临界区内完成以下单事务：

1. 校验 exact read outcome、observation/artifact digest、当前 retained read handle 与 run fence；同 final key 重投先返回既有 control，不再 mutation。
2. `RELEASE_NO_MATERIALIZE`：stage typed `RELEASE_AFTER_READ` directive，不建立依赖。
3. `RETAIN_FOR_MATERIALIZE`：要求 assembly 已铸造且 retained 的 materialize handle；以该 handle 的 stable action identity 建立 `MaterializationDependency`，stage typed `RETAIN_FOR_MATERIALIZE` directive。business 不能传入或生成 actionId。
4. 执行 business checked mutation；只有 mutation 成功，才同时 commit Cloud dependency、action final disposition 与 final-consumed control。mutation 抛出时三者都不提交。
5. control 的 tracker attachment 精确为 `artifactId/artifactDigest/stableMaterializeActionId/leaseDigest/directive`。普通 `WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE` 没有该 attachment，原 canonical ack bytes/digest 路径逐字不变。

Cloud 在 read final control 得到 local receipt 后，可以 compact/release Cloud broker/wire 的 read image copy；这个 receipt 不结束 DHXY 的 materialize lease。

#### 1.3 DHXY apply/control 原子事务

`RemoteOperationLedger.applyFinalConsumedAck(...)` 对 tracker attachment 使用一个 package-private typed participant，并在同一 owner lock 下：先校验 same ack/frontier，预建并预留 receipt outbox，随后原子执行 artifact owner transition、移除 read terminal metadata、插入 retained receipt；任何一步失败都不移除 terminal record、不改变 artifact owner、不退 quota。

- `RELEASE_AFTER_READ`：receipt 已入 outbox 后删 raw bytes，留下 digest-only release witness，再原子退实际 byte/frame/artifact reservation。
- `RETAIN_FOR_MATERIALIZE`：把同一 raw bytes 的 owner 改为 stable materialize action lease；不复制、不删、不退本地 retained bytes。read terminal record 可删，receipt 只带 lease digest 证据。
- control redelivery：frontier 命中后返回逐字相同 receipt；owner transition 与 quota refund 都不重复。
- receipt submit/ack uncertain：outbox 仍以相同 receipt bytes 重投；不会重新建 lease，也不会提前删 artifact。

#### 1.4 最后释放点与异常矩阵

| 事件 | Cloud dependency | DHXY 最后一份 artifact bytes | 释放规则 |
| --- | --- | --- | --- |
| read final，明确无 materialize | 不建立 | read receipt 入 outbox 后删除 | `RELEASE_AFTER_READ` 唯一一次退账。 |
| read final，后续要 materialize | 建立 stable dependency | owner 转为 materialize lease 并保留 | Cloud read copy 可在 receipt 后释放；local 不释放。 |
| materialize `UNKNOWN` / request outcome 不明 | 保留 | 保留 | 不 consume final、不 renew、不补拍、不靠 TTL。 |
| pause/resume / `runRevision` 变化 | 同 action identity 保留 | 同 lease 保留 | 新 context 只 rebind，不 remint/release。 |
| materialize `NOT_EXECUTED` 且允许 attempt renewal | 同 action identity 保留 | 保留 | consume disposition 为 `ATTEMPT_RETIRED_KEEP_LEASE`；仅机械 request/capture ID 更新。 |
| materialize final-consumed 成功或明确安全拒绝 | 清除 | materialize receipt 入 outbox 后删除 | `RELEASE_OCCURRENCE_COMPLETE`，随后原子退账。 |
| 显式可信取消 | 清除 | cancel final control apply 后删除 | 只允许 broker/retained ledger 证明 materialize 从未 dispatch 且不存在 late outcome；已 dispatch/UNKNOWN 不可取消释放。 |
| exact terminal | 清除 | terminal fence commit 后删除 | 必须先让该 taskRun 永久拒绝 late materialize；仅删 bytes，receipt/release digest witness 继续保留到各自 terminal bucket 清除。 |

“exact terminal”是 coordinator/local registry 已提交的不可逆 taskRun terminal fence，不是超时、无 caller、断线或进程时间；全链没有 TTL、lease timeout、LRU eviction 或 restore/takeover。

### 2. closed profile 的副作用前 reserve 与 actual settle

#### 2.1 public profile hard bounds

新增 public enum 精确 FQCN：`com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerReadProfile`。DHXY wire mirror 为 `com.bot.dhxy.cloud.remote.RemoteTaskTrackerReadProfile`，canonical enum name 必须逐字相同。四个 v1 值及 hard bounds：

| enum | retained frame bounds | max artifact/frame count | max retained encoded bytes | max local working images / ARGB bytes | optional drag |
| --- | --- | --- | --- | --- | --- |
| `WUHUAN_TITLE_GATE` | panel `182x338` + detail `175x65` | `1 / 2` | `1,048,576`（每帧 `524,288`） | `4 / 3,683,356` | allowed by command flag |
| `WUHUAN_PATHING` | panel `182x338` + detail `175x65` | `1 / 2` | `1,048,576` | `4 / 3,683,356` | allowed by command flag |
| `WUBEI_DETAIL` | panel `182x338` + detail `175x65` | `1 / 2` | `1,048,576` | `4 / 3,683,356` | allowed by command flag |
| `XIULUO_DETAIL` | panel `182x338` + detail `175x40` | `1 / 2` | `1,048,576` | `4 / 3,665,856` | allowed by command flag |

`maxLocalWorkingArgbBytes` 是按 HEAD 最坏同时存活的 `full 1024x768 + two panel 182x338 + one detail`、每 pixel 4 bytes 做 checked arithmetic：五环/五倍为 `4*(1024*768 + 2*182*338 + 175*65)`，修罗为 `4*(1024*768 + 2*182*338 + 175*40)`。`EXISTING_CAPTURE_ARTIFACT` source 仍使用相同 derived-frame bounds，但禁止新 full-window capture/drag；已有 source artifact 由其原 lease 计费，不在 tracker ledger 再计一份。

enum 必须公开以下只读方法，且返回 immutable bounds：

```java
public int maxArtifactCount();
public int maxRetainedFrameCount();
public List<TaskTrackerFrameBound> retainedFrameBounds();
public long maxRetainedEncodedBytes();
public int maxLocalWorkingImageCount();
public long maxLocalWorkingArgbBytes();
public int maxCloudTransientCopyCount();
public long maxCloudTransientBytes();
public boolean allowsPanelReposition();
```

Cloud envelope/tree 路径在未重写前的 transient hard reserve 是 `7,820,640` bytes、4 copy slots：对 `B=1,048,576`，按两棵最坏 UTF-16 Base64 tree `2*(2*4*ceil(B/3))`、decode input 与 constructor defensive copy `2*B`、再加 `128 KiB` closed structural/scratch allowance。解析完成后只留一份 typed retained bytes，立即退 transient 差额；若采用 streaming decoder 降低 actual，只能退更多，不能降低 dispatch 前 reserve。

#### 2.2 DHXY reserve 顺序

新 command 到达时固定顺序为：

1. `RemoteOperationLedger.claim(...)` 先查同 request/action/semantic key。duplicate terminal/active 直接复用既有 reservation 与 artifact/send handle，不申请新 quota、不 capture、不 drag。
2. 对真正 new tracker read，`TaskTrackerPanelRemoteArtifactLedger.reserve(profile, scope, taskRunId, stableActionId)` 一次预留 profile 的 artifact count、frame count、retained encoded bytes 与 working image/ARGB bytes；继续受 D2 的 per-run `8 artifacts/8 MiB`、per-owner `512/32 MiB`、global `8192/256 MiB` 约束，working+retained 都占对应 byte ceiling。
3. 只有 reserve 成功才可进入 `TaskTrackerPanelService` 的 anchor/capture/optional drag。reserve 失败返回 capacity `NOT_EXECUTED`，必须是零 capture、零 drag、零 artifact、零 retry/wake。
4. 编码完成后以 ownership transfer 把 encode buffer 变成 artifact 唯一 raw storage；原子 commit artifact + terminal send metadata，再按实际 frame count/encoded bytes/working high-water settle，退全部差额。失败在 artifact commit 前则整笔 rollback；drag/capture 已发生后的非容量不明仍按 `UNKNOWN`，但原 reservation 继续覆盖证据，不能重做。
5. read final 转 materialize lease 时 reservation 只换 owner，不退款；到 1.4 的最后释放点才退款。zero-owner bucket 在退款事务末尾移除，terminal 扫描只删已经由 exact terminal fence 覆盖的 bucket。

#### 2.3 Cloud final dispatch 前 reserve

`RemoteGameCommandBroker` 在 retained same request/action key 命中检查之后、`registerAuthorizedCommandLocked`/route offer 之前，调用 broker-owned `RemoteEncodedOutcomeCapacityGovernor.reserveBeforeDispatch(...)`。tracker read 预留 `1 outcome + 2 frames + 1,048,576 retained bytes + 4 transient slots + 7,820,640 transient bytes`，并同时计 D2 Cloud broker per-owner `512/32 MiB`、global `8192/256 MiB`。

- reserve 失败时 broker 在 dispatch 前返回 `NOT_EXECUTED/BROKER_CAPACITY_EXCEEDED`；DHXY 不可能看到 command，因此零 capture/零 drag。
- actual typed outcome accepted 后，broker 与 action ledger只能共享同一 outcome object/byte arrays；settle 实际 encoded bytes，解析 transient 全释放并退差额。
- dispatch 后 timeout/`UNKNOWN` 仍可能有 late outcome，因此最坏 retained+transient reservation 必须保留，直到 late exact outcome settle 或 broker 证明该 request 永不再产 outcome；不能在 timeout 时偷退，再让 late image 无额度进入。
- `NOT_EXECUTED` 且 broker 证明未 dispatch 时整笔退；attempt renewal 使用 same action identity，但新 mechanical request reservation 只有在旧 attempt final-consumed/retired 后建立，绝不跨未知 attempt 复用额度。
- same key always before quota；settle/refund 与 pending/compacted index mutation 在同 broker lock 中完成。异常 rollback count、frame、retained bytes、transient bytes 四本账，owner 归零即删 owner bucket；exact terminal 先移 index/阻断 late ingress，再退终态 bucket。

#### 2.4 普通 CAPTURE 明确纳入

普通 `CAPTURE` 仍是 read-only，request/outcome 的字段、field order、canonical bytes/digest 一字不改；但它不能成为 quota 旁路。它同样使用“Cloud dispatch 前 + DHXY capture 前”reservation，只是 profile 由既有 `CaptureRegion(width,height)` 机械派生，不增加 wire 字段：`maxFrameCount=1`，encoded hard bound 用 checked `1 MiB + 8*width*height`，超过 owner/global ceiling 即在副作用前拒绝。actual settle 后只保留一份 encoded outcome blob，final-consumed apply 后释放。由此普通 CAPTURE 也不会在截图后才发现没有 byte/count 容量。

### 3. 图片所有持久副本归一与释放

D3 选择“消除持久重复”，不是把 raw、JsonNode、Base64 当一份账：

| 位置/对象 | D3 物理形态 | persistent? / 计费 | 唯一释放点 |
| --- | --- | --- | --- |
| DHXY tracker artifact ledger | frame raw encoded bytes，唯一 artifact storage | 是；actual frame/count/bytes 全计 scope/run/owner/global | 1.4 的 materialize final、可信取消或 exact terminal；无 materialize 则 read final。 |
| DHXY ordinary CAPTURE outcome blob | 一份 raw encoded bytes | 是；同 quota family，count 1 + actual bytes | ordinary CAPTURE final-consumed apply receipt 后。 |
| DHXY `RemoteOperationLedger.terminalOutcome` | 改为 sealed terminal metadata：inline non-image 或 `EncodedOutcomeRef`；tracker/CAPTURE 不留 image `JsonNode`/Base64/byte[] | metadata 是；image footprint 为 0 | 对应 final control apply 时移除。 |
| DHXY outcome wire serialization | transport 按 `EncodedOutcomeRef` 从 owner ledger streaming Base64；不调用 image operation 的 `toPayloadTree(byte[])`，不缓存完整 Base64 String/JsonNode | 否；bounded scratch 计 working reservation | 单次 HTTP submit 返回/抛出即释放；uncertain 重投仍从同 blob stream。 |
| DHXY materialization ledger | artifact ref、digest、lease/action identity、publish witness | 是；image footprint 为 0 | materialize final/terminal metadata cleanup。 |
| Cloud endpoint `RemoteCommandOutcomeEnvelope.payload.deepCopy()` 与 tree decode input | 最多两棵 Base64 tree + decode input，保持现有 defensive validation | 否；按 `maxCloudTransient*` 在 dispatch 前预留 | typed outcome 构造并完成 digest validation 后立即释放。 |
| Cloud typed `TaskTrackerReadOutcome`/`CaptureOutcome` constructor copy | constructor input transient；constructor defensive copy成为唯一 retained arrays | 仅 defensive copy persistent；actual bytes/count 计 broker governor | local final receipt 被 Cloud 接受并 compact 后。 |
| Cloud broker pending + action ledger exact outcome | 必须引用同一个 typed outcome object，不得 `withCommon`/accessor-copy 后分别保存 | 是，但物理只计一次 | action final 变 digest witness；broker 在 receipt compact 后释放最后引用并退账。 |
| Cloud public `TaskTrackerReadResult` | observation、opaque artifact ref、digests；内部只引用上行 exact outcome，不复制 arrays，public 无 bytes accessor | 引用不新增物理 copy | capability consume final 后清引用；broker owner 决定 bytes 生命周期。 |
| Cloud workflow/materialization dependency | observation/action/lease digest 与 artifact ref | 是；image footprint 为 0 | materialize final/cancel/terminal。 |
| Cloud `RemoteGameCommandBroker.CompactedWitness` | 改为 digest-only witness：request/outcome/ack/receipt digest、code/state/timestamps，无 `RemoteOutcome` | 是；image footprint 为 0 | 既有 terminal bucket removal。late duplicate 以 digest 校验并返回 duplicate witness，不重建图片。 |

DHXY 对 image-bearing terminal 的具体类型收口为 package-private `RemoteOperationLedger.EncodedTerminalOutcome`，字段仅为 common envelope metadata、payload canonical metadata、`EncodedOutcomeRef` 与 outcome digest；`Claim.awaitTerminalOutcome()`/polling loop 改拿 `RemoteGameOutcomeSubmission`，由 `HttpRemoteCommandTransport` streaming 写 wire。普通非图片 operation 仍可用现有 inline `JsonNode`，不会被错误计入 image bytes。这样 tracker 路径不存在“artifact raw + terminal JsonNode/Base64”两份持久对象；Cloud 也不存在 pending outcome + action outcome + compacted outcome 三份 arrays。

### 4. public closed tracker capability 的精确 API

#### 4.1 FQCN、constructor owner 与 signatures

唯一 public capability 为 `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerPanelCapability`：

```java
public final class TaskTrackerPanelCapability {
    TaskTrackerPanelCapability(
            CloudTaskRunExecutionContext context,
            CloudTaskRetainedActionState actionState,
            TaskTrackerPanelRetainedActionSet actionSet,
            RemoteGameClientPort commandPort,
            RemoteFinalConsumptionCoordinator finalConsumptionCoordinator,
            TaskTrackerPanelInterpreter interpreter);

    public TaskTrackerReadResult read(
            TaskTrackerReadCommand command,
            long timeoutMs);

    public TaskTrackerMaterializeResult materialize(
            TaskTrackerMaterializeCommand command,
            long timeoutMs);

    public void consumeFinal(
            TaskTrackerReadResult exactResult,
            TaskTrackerReadFinalDisposition disposition,
            CheckedReadFinalMutation mutation)
            throws InterruptedException;

    public void consumeFinal(
            TaskTrackerMaterializeResult exactResult,
            TaskTrackerMaterializeFinalDisposition disposition,
            CheckedMaterializeFinalMutation mutation)
            throws InterruptedException;

    @FunctionalInterface
    public interface CheckedReadFinalMutation {
        void apply(TaskTrackerObservation exactObservation) throws InterruptedException;
    }

    @FunctionalInterface
    public interface CheckedMaterializeFinalMutation {
        void apply(TaskTrackerPreparedAction exactPreparedAction) throws InterruptedException;
    }
}
```

相关 public closed types 的精确 FQCN：

- `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerReadCommand`：`readProfile/source/allowPanelReposition/sourceArtifactRef`，不含 requestId/actionId/captureId/revision；`sourceArtifactRef` 只允许 `EXISTING_CAPTURE_ARTIFACT` 且其 constructor package-private。
- `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerReadResult`：public accessor 仅 `readProfile()`、`observation()`、`artifactRef()`、`observationDigest()`；raw wire outcome/encoded frames/opaque read handle 只允许 package-private capability 访问。
- `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerMaterializeCommand`：`artifactRef/observationDigest/selectedLink/preparedOperation/targetKeyword/validationPolicy`；没有 raw bytes、capture 指令或 mint 字段。
- `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerMaterializeResult`：public accessor 仅 `status()`、`preparedAction()`、`resultDigest()`；raw outcome 与 opaque materialize handle package-private。
- `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerReadFinalDisposition`：仅 `RELEASE_NO_MATERIALIZE`、`RETAIN_FOR_MATERIALIZE`。
- `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerMaterializeFinalDisposition`：仅 `RELEASE_OCCURRENCE_COMPLETE`、`ATTEMPT_RETIRED_KEEP_LEASE`、`RELEASE_TRUSTED_CANCEL_BEFORE_DISPATCH`；最后一值必须由 broker never-dispatched proof 校验，business 选择本身不构成 proof。
- `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerReadProfile` 与 `TaskTrackerFrameBound`：bounds 如 2.1；enum constructor private，不能扩展/free-form。

`CloudTaskServiceExecutionContext` 新增的唯一 getter 精确为：

```java
public TaskTrackerPanelCapability taskTrackerPanel();
```

现有 `CloudTaskServicePort` 不新增 public `taskTrackerRead/rawMaterialize/rawConsume` 方法；tracker business constructor 只接收 `TaskTrackerPanelCapability`，不接收 `CloudTaskServicePort`、broker、ledger、command port 或 codec。

#### 4.2 opaque handle 铸造与跨 revision 复用链

1. package-private `CloudTaskRunAuthorityAssembly` 是唯一 constructor root；其 retained lifecycle activation adapter 以 persisted phase/action slot 调用 package-private `TaskTrackerPanelRetainedActionState.openOrReuseActionSet(...)`。
2. `TaskTrackerPanelRetainedActionSet` 内含 opaque read/materialize handles；第一次 occurrence 由 retained `CloudTaskRunActionLedger` 铸 stable business action identity，constructor/package-private mint 均不向 business 暴露。
3. action set 把每个 slot 固定到一个 closed `TaskTrackerReadProfile`。business command 的 profile 必须等于当前 retained slot；不匹配在 Cloud dispatch/reserve 前拒绝，不能借较小 profile 绕过 quota。
4. pause/resume 或新 `runRevision` 复用现有 `CloudTaskRetainedActionState` 与同一 action set；assembly 只用新 exact context wrapper rebind capability。observation identity 与 stable action identity 不变，只有获准 renewal 才换 mechanical request/capture ID；revision 从不参与 mint key。
5. `read()` 内部顺序固定为 profile/handle fence -> Cloud reserve -> typed tracker read wire -> package-private interpreter -> 无 raw 的 `TaskTrackerReadResult`。`materialize()` 只接受同 capability 产出的 exact read result/artifact ref，并校验 Cloud dependency lease 后发送独立 `TASK_TRACKER_MATERIALIZE_ACTION`。
6. business 可见对象没有 raw ledger、mint/renew、request/poll/outcome envelope、requestId/actionId/captureId、encoded image accessor；callback 只拿 typed observation/prepared action。remote assembly/capability 独占所有机械 identity 与 final-consumed authority。

### 5. Parent Review #2 关闭矩阵

| review 项 | D3 关闭点 |
| --- | --- |
| P1-1 read final 先删 materialize 证据 | Cloud checked-final 原子建立 stable dependency并下发 typed directive；DHXY read apply 只转 owner，最后 local bytes 仅在 materialize final、可信未派发取消或 exact terminal 释放；覆盖 redelivery/UNKNOWN/pause-resume/terminal，无 TTL。 |
| P1-2 quota 在副作用后 | 四个 closed profile 给 hard frame/dimension/count/retained/transient bounds；local capture/drag 前与 Cloud dispatch 前 reserve，same-key-before-quota，actual settle 原子退差额，reserve fail 保证零 side effect；普通 CAPTURE 同样纳入。 |
| P1-3 terminalOutcome/Base64 与 raw 重复 | tracker/CAPTURE image terminal 改 metadata + encoded ref，wire Base64 streaming transient；local 唯一 raw owner、Cloud 唯一 typed defensive copy，action/broker共享对象，compacted witness digest-only；每份 transient/persistent 均列释放点。 |
| P2-1 capability/API/profile 不机械 | 给出 public FQCN、package-private constructor owner、四个 exact signatures、closed DTO/disposition/profile bounds；assembly 铸并 retained opaque handles，跨 revision rebind 不 remint，business 无 raw/mint/request/poll/outcome。 |

### 6. Replacement Worker U2 self-QA（不是 reviewer approval）

- P0: 0。未引入 raw request/poll/outcome bypass，未把 Cloud interpretation 下放本地，未使 business 获得 mint/renew/final-ingress，也未用 terminal/TTL 猜测释放。
- P1: 0。Parent Review #2 三项 P1 已分别由 dependency owner transaction、双侧 pre-reserve/settle、持久图片副本归一与逐项释放闭合。
- P2: 0。public capability 的 FQCN/signatures、constructor/assembly owner、profile frame/dimension/count/byte bounds 与跨 revision handle 链均已可机械编码。
- 普通 `CAPTURE` 仍只读，request/outcome canonical bytes/digest 零变化；新增 tracker materialize 仍是独立 typed operation/handler/ledger/assembly，不藏入 CAPTURE。
- **无已批准业务差异；按基线等价迁移。** 本 self-QA 不是 `Approved`，不替代父级复审。

## Replacement Internal Worker U2 - DELIVERED / Design Repair #2 Delta - 2026-07-13T11:24:12-04:00

- task: `W-TTPS-D3`
- delivery: Parent Design Review #2 的 `P1 x3/P2 x1` 已在本日志追加 Delta 与 self-QA，等待父级复审；没有自行批准。
- verification: 只读核查 DHXY `0114604e` tracker 路径与 Cloud Full R0 API/copy/ledger 路径；按用户冻结要求未运行 Maven、测试或运行面。

## Parent Design Review #3 - BLOCKED - 2026-07-13T11:34:00-04:00

D3 已把 same-frame dependency、双侧 pre-reserve、图片物理副本清单与 public capability/profile 上界推进到可实施
方向；这些通过项不重开。父级再按当前 `RemoteFinalConsumptionCoordinator`、双仓 `RemoteFinalConsumedAck` 与
ledger 锁边界机械复审，整体仍 **BLOCKED，P0=0/P1=3/P2=1**，Java/Maven/schema/resources/tests/host/caller
继续冻结。

1. **P1：materialize dispatch 缺少“本地 lease 已安装”的因果门。** D3 `:744-752` 在 Cloud mutation 后只
   stage/commit final control；当前 `RemoteFinalConsumptionCoordinator.consumeFinal(...)` 在 control 入 broker 后即返回，local
   receipt 是之后异步到达。D3 `:925` 的 `materialize()` 只校验 Cloud dependency，可能在 DHXY 尚处
   `READ_OUTCOME_OWNED`、尚未应用 `RETAIN_FOR_MATERIALIZE` control 时先派 materialize。网络/control lane 重排下，本地只能
   错拒、UNKNOWN 或补等待，破坏同帧一次性语义。D4 必须规定 capability 在 exact retain receipt 被 Cloud accepted/compacted
   前不可 dispatch materialize，并给 typed `DEPENDENCY_NOT_READY` continuation/level wake；不得 busy poll、sleep、自动 retry或
   新线程。
2. **P1：所谓 Cloud/local“单事务”没有真实共同 transaction owner。** 当前 coordinator `:34-55` 仅在
   `retirementLock` 下 reserve/complete action+broker，而 checked business mutation 在锁外执行；mutation 成功后 complete 失败
   只能进入 uncertain，无法回滚。D3 又要求 Cloud dependency、business mutation、action disposition、control 同时提交；local
   则要求 `RemoteOperationLedger` outbox 与独立 artifact ledger 在“同一 owner lock”原子切换，但未指定共享锁/prepare-commit
   owner。D4 必须选择并列 exact owner/file/method：把 dependency/artifact state 纳入现有 ledger 单锁，或新增同 assembly 的
   package-private transaction coordinator，给 prepare/commit/uncertain/redelivery 顺序；不能继续用跨对象口头原子性。
3. **P1：tracker final-control attachment 没有闭合双仓 wire/digest/schema 写集。** D3 `:750` 新增
   `artifactId/artifactDigest/stableMaterializeActionId/leaseDigest/directive`，但当前双仓 `RemoteFinalConsumedAck` 均无该字段，
   D2 文件表 `:582/:617` 只写 captureId 规则，`:631` 还声明 coordinator/receipt运输直接复用。D4 必须列 Cloud+DHXY typed
   attachment DTO、两个 ack model、两个 digest canonicalizer、strict schema/protocol 文档、local apply participant 与 operation-
   specific backward compatibility；旧三类 operation 的 canonical ack bytes 必须逐字不变。
4. **P2：working-memory 与 retained-artifact reservation 的释放时点仍混在一起。** D3 `:816` 说 settle working
   high-water，`:817` 又说 owner 转 lease 时“reservation 只换 owner、不退款”，可能让 `3.6MiB` working budget 随 artifact lease
   一直占用。D4 分账并逐点写清：working image/encode scratch 在 handler/transport 调用结束时退，retained encoded bytes/count
   才随 read/materialize lease 保留；UNKNOWN 只保留仍可能被 late outcome 使用的那一类额度。

### 下一任务 `W-TTPS-D4`

同一实现 Worker 或 replacement 先在本日志末尾追加 `CLAIMED`，只追加 Design Repair #3 Delta 关闭
`P1 x3/P2 x1`；已通过的 profile 数值、图片副本归一和 ordinary CAPTURE 纳入 quota 不重开。唯一写集仍为本日志；
两仓 Java/Maven/schema/resources/tests/host/caller 全冻结。Worker QA 不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## Replacement Internal Worker U2 - CLAIMED - 2026-07-13T11:43:32.6414077-04:00

- task: `W-TTPS-D4`
- claimedAt: `2026-07-13T11:43:32.6414077-04:00` (`America/New_York`)
- uniqueWriteSet: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-tracker-panel-worker-u.md`（append-only，仅此日志）
- 冻结确认：DHXY/Cloud Java、Maven、schema、resources、tests、host/caller 全部只读；保护全部 dirty/untracked 与在途改动，不回滚、不覆盖、不清理、不暂存、不提交。

## Design Repair #3 Delta

本 Delta 只覆盖 Parent Design Review #3 的 `P1 x3/P2 x1`。D3 已通过的 profile 数值、图片持久副本归一、普通 `CAPTURE` 纳入副作用前 quota、CAPTURE 只读、四层 identity 与既有 caller/wake 边界均不重开。以下均为冻结解除后的精确实施合同；本轮没有修改任何源码、schema 或 host/caller。

### 1. exact RETAIN receipt 是 materialize dispatch 的唯一因果门

#### 1.1 因果状态机

Cloud 的 dependency 权威不再放在松散 workflow map，而纳入现有 `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunActionLedger` 的 read/materialize retained records，同一 ledger monitor 下只有以下状态：

```text
NONE
  -> RETAIN_CONTROL_PREPARED
  -> RETAIN_CONTROL_PENDING_RECEIPT
  -> LOCAL_RETAIN_RECEIPT_COMPACTED_READY
  -> MATERIALIZE_DISPATCHED
  -> RELEASE_CONTROL_PENDING_RECEIPT
  -> RELEASED

任何 mutation 已开始后的异常：
  -> BUSINESS_CONSUMPTION_UNKNOWN_OWNER
```

`RETAIN_CONTROL_PENDING_RECEIPT` 只说明 Cloud 已发布 final control，不证明 DHXY 已安装 lease。唯一可 dispatch 的状态是 `LOCAL_RETAIN_RECEIPT_COMPACTED_READY`；它只能由 Cloud 接受 exact `RemoteFinalConsumedReceipt`、broker 与 action ledger 完成同一 receipt compaction 后设置。收到/发送 control、Cloud mutation 返回、DHXY poll 到 control、DHXY 生成 receipt、receipt 正在网络中，均不能提前置 READY。

exact readiness key 为：

`stableRun + read semanticAddress/actionId + read outcomeDigest + artifactId/artifactDigest + materialize semanticAddress/actionId + leaseDigest + retainAckDigest + retainReceiptDigest`。

receipt 现有 wire 不新增 tracker 字段；它的 `ackDigest` 已绑定完整 tracker attachment，Cloud `requireCompactionPlan(...)` 又要求 receipt 与 retained ack exact match，因此 readiness 不靠仅 actionId 或仅 artifactId 推断。

#### 1.2 `materialize()` typed not-ready contract

D3 public API 的 materialize 返回类型修正为 closed union：

```java
public TaskTrackerMaterializeAttempt materialize(
        TaskTrackerMaterializeCommand command,
        long timeoutMs);

public sealed interface TaskTrackerMaterializeAttempt
        permits TaskTrackerMaterializeResult, TaskTrackerDependencyNotReady { }

public record TaskTrackerDependencyNotReady(
        TaskTrackerDependencyContinuation continuation,
        DependencyStatus status)
        implements TaskTrackerMaterializeAttempt { }

public enum DependencyStatus {
    DEPENDENCY_NOT_READY,
    DEPENDENCY_CONSUMPTION_UNKNOWN,
    DEPENDENCY_TERMINAL
}
```

`TaskTrackerDependencyContinuation` constructor package-private，只含 assembly-minted stable materialize handle、leaseDigest 与 generation，不含 requestId/raw ledger。`TaskTrackerPanelCapability.materialize(...)` 固定顺序：

1. 先以 retained handle 查 `CloudTaskRunActionLedger.requireTrackerDependencyReadiness(...)`。
2. `LOCAL_RETAIN_RECEIPT_COMPACTED_READY` 才进入 Cloud pre-dispatch quota/fence/broker；这是 materialize request 第一个可能 dispatch 的点。
3. `RETAIN_CONTROL_PREPARED/PENDING_RECEIPT` 返回 `DEPENDENCY_NOT_READY` 与同一 opaque continuation；零 broker reservation、零 request mint、零 DHXY command、零 side effect。
4. `BUSINESS_CONSUMPTION_UNKNOWN_OWNER` 返回 `DEPENDENCY_CONSUMPTION_UNKNOWN`；不等待、不续 attempt、不 dispatch。
5. exact terminal 返回 `DEPENDENCY_TERMINAL`；不 dispatch。

#### 1.3 no-lost-wake、无 polling/retry

新增 package-private `com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerDependencyContinuationGate`，由 `CloudTaskRunAuthorityAssembly` 单例装配；它无线程、无 timer，只有一把 `ReentrantLock`、每 stable materialize action 一个 sticky level state 和至多一个 opaque continuation waiter：

```java
TaskTrackerDependencyContinuation armOrObserveReady(
        CloudTaskRunActionLedger.TrackerDependencyHandle dependency);

void markReceiptCompactedReady(
        CloudTaskRunActionLedger.TrackerDependencyReadySignal signal);

void markTerminal(
        CloudTaskRunActionLedger.TrackerDependencyHandle dependency);
```

- dependency 在 prepare 时先登记 gate entry；`armOrObserveReady` 与 `markReceiptCompactedReady` 都在 gate lock 下检查/写 sticky `NOT_READY|READY|TERMINAL`，所以“先 ready 后 arm”和“先 arm 后 ready”都不丢 wake。
- `RemoteFinalConsumptionCoordinator.acceptReceipt(...)` 只有在现有 `actionLedger.commitCompaction(...)` 与 `broker.compactFinalConsumedControl(...)` 成功返回后，才把 ledger 产生的 exact `TrackerDependencyReadySignal` 交给 gate；duplicate compacted receipt 可重复发同一 level signal。
- continuation 由现有 task continuation/caller thread 消费；它只在 level wake 后重入同 retained materialize handle。没有 busy poll、sleep、scheduled retry、自动 retry、新 executor 或 tracker thread。重复 signal 合并为同一 READY level，不生成新 occurrence/request/action。
- gate signal 若发生在 action-ledger READY 之后但 caller 尚未 park，sticky READY 使 caller 直接继续；若 signal 调用异常，action ledger 仍是权威 READY，下一次同 continuation arm 会直接观察 READY，不把 dependency 回滚或 remint。

DHXY `TaskTrackerPanelMaterializationHandler` 仍做防御校验：若收到 materialize command 时 `RemoteOperationLedger` 尚无同 `leaseDigest` 的 `MATERIALIZE_DEPENDENCY_OWNED`，在任何 validation/publish 前返回 typed `NOT_EXECUTED/DEPENDENCY_NOT_READY`；这只表示协议/因果门违例，不触发本地等待、重读或 retry。

### 2. 可实现的 prepare/commit/compensate；不再声称跨仓单事务

#### 2.1 总原则

Cloud 与 DHXY 没有共同 transaction owner。D4 的唯一说法是：

1. Cloud 进程内由 `RemoteFinalConsumptionCoordinator.retirementLock` 串行 orchestration，dependency 的 durable process-local owner 是 `CloudTaskRunActionLedger` 自身 synchronized monitor；broker 继续由 `RemoteGameCommandBroker.stateLock` 管理。
2. DHXY 进程内由 `RemoteOperationLedger.monitor` 同时拥有 final frontier/outbox 与 tracker artifact bytes/lease/counters；D2/D3 计划中的独立 `TaskTrackerPanelRemoteArtifactLedger.monitor` 撤销，避免不存在的跨 monitor 原子性。
3. 跨仓只靠 `ack -> local apply/receipt -> Cloud compact` 异步 handshake。任一仓提交后另一仓失败都进入显式 retained/fail-closed owner，不做分布式回滚。

#### 2.2 Cloud exact owner/file/method

在 `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunActionLedger.java` 新增/扩展 package-private synchronized 方法：

```java
TrackerReadFinalPlan prepareTrackerReadFinal(
        RetainedActionIdentity readIdentity,
        RetainedActionIdentity materializeIdentity,
        TaskTrackerReadOutcome exactOutcome,
        TaskTrackerReadFinalDisposition disposition);

TrackerNoticeCommitPlan prepareTrackerNoticeCommit(
        TrackerReadFinalPlan plan,
        RemoteFinalConsumedAck exactAck,
        RemoteGameCommandBroker.ControlPublication publication);

void commitTrackerNoticePending(TrackerNoticeCommitPlan plan);

void markTrackerConsumptionUnknown(
        TrackerReadFinalPlan plan,
        TrackerConsumptionFailureStage stage);

TrackerDependencyReadySignal commitTrackerReceiptCompaction(
        CompactionPlan plan);

TrackerDependencyReadiness requireTrackerDependencyReadiness(
        TrackerDependencyHandle handle);
```

`RemoteFinalConsumptionCoordinator.consumeFinal(...)` 的 tracker overload 采用以下真实阶段：

1. **prepare-before-mutation**：在 `retirementLock` 下调用 `prepareTrackerReadFinal`，只把 exact read/materialize identities 与 attachment data stage 为 `RETAIN_CONTROL_PREPARED`；再 reserve broker control。任一失败且 business mutation 尚未被调用时，取消 action/control reservation并回到原 `OUTCOME_FINAL_UNCONSUMED/NONE`，零外部可见状态。
2. **mutation**：释放 locks 后调用 checked business mutation，保持当前 Full R0 真实边界。调用一旦开始，就不再宣称 mutation 可回滚。
3. **prepare notice commit**：mutation 正常返回后构造含 typed attachment 的 exact ack；在 `retirementLock` 下先让 action ledger 与 broker 分别完成所有 validation/arithmetic，生成 immutable `TrackerNoticeCommitPlan` 与 `ControlPublication`，但不 dispatch。
4. **commit**：先由 action ledger 把 retained read record/dependency 置 `RETAIN_CONTROL_PENDING_RECEIPT` 并 retain exact ack，再由 broker 把已 prepared control 从 `RESERVED` 置 `QUEUED`。broker queue 前 local 不可见；queue 后即使 Cloud caller 返回异常，materialize 仍被 receipt gate 阻断。
5. **receipt commit**：现有 `acceptReceipt(...)` 先 `requireCompactionPlan`、broker accept，再在 broker/action 的既有 compaction callback 中提交 frontier；tracker dependency 同 action-ledger commit 置 `LOCAL_RETAIN_RECEIPT_COMPACTED_READY`。方法成功返回后才 signal continuation gate。

#### 2.3 mutation 后不可回滚的 fail-closed owner

- business mutation 抛出/interrupt、mutation 正常返回后 ack 构造失败、prepare/commit notice 失败、或无法证明 control 是否 queue：调用 `markTrackerConsumptionUnknown(...)`，状态为 `BUSINESS_CONSUMPTION_UNKNOWN_OWNER`。它永久保留 exact outcome、artifact/dependency identities、quota 和任何已 reserve/可能已 publish control evidence；禁止 materialize、renew、新 occurrence、重做 mutation与自动 compensation。
- 只有“business mutation 从未被调用”可真正 rollback prepared reservation。mutation 已进入后所谓 compensate 仅能清理**可证明未发布**的空 control slot/working permit；业务状态、dependency evidence 与 unknown owner 不回滚。
- 若 control 实际已到 DHXY，local 可安装 lease并回 receipt；Cloud unknown owner 不接受其成为 READY，直到 exact retained ack/receipt 能按同 transaction 恢复完成 compaction。若无法恢复，只能 exact terminal 清理，不能猜测成功。
- redelivery 命中 `RETAIN_CONTROL_PENDING_RECEIPT` 返回同 ack/control；命中 `LOCAL_RETAIN_RECEIPT_COMPACTED_READY` 返回同 ready handle；命中 `BUSINESS_CONSUMPTION_UNKNOWN_OWNER` 返回 typed unknown，不再次调用 mutation。

#### 2.4 DHXY 单锁 owner 与 local participant

D4 修正未来文件表：tracker artifact raw bytes、retained byte/count reservation、lease state 与 release witness 全部纳入 `DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java`，受现有 `monitor` 管理；不再创建带独立 monitor 的 `service/tasktracker/TaskTrackerPanelRemoteArtifactLedger.java`。`TaskTrackerPanelMaterializationLedger` 仍只拥有 publish idempotency，不拥有 artifact lease/bytes。

`RemoteOperationLedger` 新增 private nested `TaskTrackerFinalControlParticipant` 与 immutable `PreparedTrackerFinalApply`：

```java
private PreparedTrackerFinalApply prepare(
        SemanticDetail exactDetail,
        RemoteFinalConsumedAck exactAck);

private void commit(PreparedTrackerFinalApply prepared);
```

`applyFinalConsumedAck(...)` 在同一个现有 `monitor` 内按顺序：

1. 校验 ack digest/scope/frontier/current detail/outcome 与 typed attachment；same `ackDigest + lastReceipt` 先返回，不再 participant。
2. 校验 outbox capacity、artifact bytes/digest、当前 owner、lease/action/address；checked arithmetic 预计算 frontier、count/bytes、release witness 与 exact receipt，形成 plan，零写入。
3. 单次 deterministic commit 同时写 artifact owner transition或释放、frontier/current detail、receipt outbox/release witness 与 counters；commit 内不调用网络、Spring bean、window/runtime、handler 或另一个 monitor。
4. commit 前 RuntimeException 为零写入；commit 后只有 VM/process failure 才可能中断，按既定 `restart=no restore` 协调终止，绝不从 temp file 猜测恢复。

local participant 对 directive 的动作固定为：`RETAIN_FOR_MATERIALIZE` 将 `READ_OUTCOME_OWNED -> MATERIALIZE_DEPENDENCY_OWNED`；`KEEP_FOR_MATERIALIZE_RENEWAL` owner 不变；三个 RELEASE directive 在 receipt 已预建后清 raw bytes/retained counters并留 digest witness。receipt delivery uncertain 只重投同 receipt，不逆转 owner transition。

### 3. tracker final-control attachment 的完整双仓合同

#### 3.1 两个 typed attachment DTO

- Cloud：`com.yueyunfe.dhxy.cloudbrain.remote.TaskTrackerFinalConsumedAttachment`，public immutable record，文件 `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerFinalConsumedAttachment.java`。
- DHXY：`com.bot.dhxy.cloud.remote.RemoteTaskTrackerFinalConsumedAttachment`，`@Value + @Builder + @Jacksonized`，文件 `DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerFinalConsumedAttachment.java`。

两端 exact JSON property order/fields：

```text
attachmentVersion = 1
directive
artifactId
artifactDigest
sourceReadActionId
sourceReadSemanticAddress
materializeActionId?          // RELEASE_AFTER_READ 时必须 absent
materializeSemanticAddress?   // RELEASE_AFTER_READ 时必须 absent
leaseDigest?                  // RELEASE_AFTER_READ 时必须 absent
```

closed `directive` 为：

- `RELEASE_AFTER_READ`：仅 `TASK_TRACKER_READ + OBSERVED + OCCURRENCE_COMPLETE`；source read 必须等于 ack outer action/address，materialize 三字段 absent。
- `RETAIN_FOR_MATERIALIZE`：仅 `TASK_TRACKER_READ + OBSERVED + OCCURRENCE_COMPLETE`；materialize identity 与 leaseDigest required。
- `KEEP_FOR_MATERIALIZE_RENEWAL`：仅 `TASK_TRACKER_MATERIALIZE_ACTION + NOT_EXECUTED + ATTEMPT_RETIRED_FOR_RENEWAL`；完整 lease字段 required，local bytes不释放。
- `RELEASE_AFTER_MATERIALIZE`：仅 materialize exact final `OCCURRENCE_COMPLETE`；完整 lease字段 required。
- `RELEASE_TRUSTED_CANCEL`：仅 materialize `NOT_EXECUTED + OCCURRENCE_COMPLETE`，且 broker never-dispatched proof 已由 Cloud ledger验证；完整 lease字段 required。

`leaseDigest = SHA-256(JCS({artifactId,artifactDigest,sourceReadActionId,sourceReadSemanticAddress,materializeActionId,materializeSemanticAddress}))`，不含 directive，使 retain/keep/release 始终引用同一 lease。artifact/read/materialize identities、digest或 directive 任一不符均在 local side effect 前 strict reject。

#### 3.2 两个 ack model

1. Cloud `com.yueyunfe.dhxy.cloudbrain.remote.RemoteFinalConsumedAck` record 在 `disposition` 与 `ackDigest` 之间新增：

   `@JsonSetter(nulls = Nulls.FAIL) TaskTrackerFinalConsumedAttachment trackerArtifactControl`。

2. DHXY `com.bot.dhxy.cloud.remote.RemoteFinalConsumedAck` 同位置新增：

   `@JsonSetter(nulls = Nulls.FAIL) RemoteTaskTrackerFinalConsumedAttachment trackerArtifactControl`，并在 Lombok builder setter 同样标 `Nulls.FAIL`。

两端 `@JsonPropertyOrder` 都变为现有 21 fields + `trackerArtifactControl`（紧邻 `ackDigest`）。operation-specific validation：

- `WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE` 必须 attachment absent；其 captureId/observationMode 规则不变。
- `TASK_TRACKER_READ`：`captureId` required；OBSERVED final 必须 attachment，`NOT_EXECUTED` renewal不得伪造 artifact attachment。
- `TASK_TRACKER_MATERIALIZE_ACTION`：`captureId` absent；final/renewal attachment 按 directive matrix required。
- explicit JSON null 与 unknown attachment fields strict reject；新 tracker operation 不允许 mixed-version/忽略字段部署。

#### 3.3 两个 digest canonicalizer、receipt 与 backward compatibility

- Cloud `RemoteProtocolDigests.computeFinalConsumedAckDigest(...)` 与 DHXY `RemoteProtocolDigests.computeFinalConsumedAckDigest(...)` 继续对整个 typed ack `valueToTree`，只 remove `ackDigest`；因此非 null attachment 自动进入 JCS/SHA-256。两端另增相同 `computeTaskTrackerLeaseDigest(...)`，字段集合/顺序如 3.1。
- `withComputedFinalConsumedAckDigest(...)` 必须把 attachment 原对象传入新 ack constructor；不得在重建时漏字段。DHXY `finalConsumedAckDigestMatches(...)` 无旁路。
- `RemoteFinalConsumedReceipt`、`RemoteFinalConsumedReceiptAck`、receipt digest 与 endpoint 不增 tracker字段；receipt 的 ackDigest 已完整绑定 attachment。local participant commit 后才生成 APPLIED receipt，Cloud compaction再按 retained ack校验。
- 旧三类 operation 的 attachment 为 Java null，`@JsonInclude(NON_NULL)` 使 wire 完全不出现该 key；其旧 property bytes 与 JCS key set逐字相同，ackDigest 逐字相同。不是“旧端忽略新字段”，而是旧 operation 没有新字段；两个新 tracker operation 必须双仓同波上线。

#### 3.4 strict schema/protocol 与精确未来写集修正

冻结解除后同一 wire cohort 必须同时修改：

| side | exact file | required change |
| --- | --- | --- |
| Cloud | `remote/TaskTrackerFinalConsumedAttachment.java` | NEW closed DTO/directive/lease digest validation。 |
| Cloud | `remote/RemoteFinalConsumedAck.java` | typed nullable field、property order、operation matrix。 |
| Cloud | `remote/RemoteProtocolDigests.java` | ack reconstruction + lease digest canonicalizer。 |
| Cloud | `remote/RemoteFinalConsumptionCoordinator.java` | build exact attachment；receipt compact 后发 ready signal。 |
| Cloud | `remote/CloudTaskRunActionLedger.java` | dependency prepare/unknown/receipt-ready唯一 owner。 |
| Cloud | `remote/RemoteGameCommandBroker.java` | ack attachment-vs-pending strict validation；运输方向/endpoint不变。 |
| DHXY | `cloud/remote/RemoteTaskTrackerFinalConsumedAttachment.java` | NEW mirror DTO/directive。 |
| DHXY | `cloud/remote/RemoteFinalConsumedAck.java` | mirror field/property order/operation matrix/builder null rule。 |
| DHXY | `cloud/remote/RemoteProtocolDigests.java` | attachment-bound ack digest + lease digest。 |
| DHXY | `cloud/remote/RemoteOperationLedger.java` | single-lock artifact owner + typed final participant。 |
| Docs | `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | exact nested schema、required/forbidden matrix、22-field order、ack/lease digest；明确 no mixed version。 |
| Docs | `docs/superpowers/specs/2026-07-12-thin-client-components-sequences.md` | retain control -> local apply -> receipt compact -> READY -> materialize dispatch sequence。 |
| Docs | `docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md` | dependency/unknown owner、local lease与release states。 |
| Docs | `docs/superpowers/specs/2026-07-12-service-migration-matrix.md` | tracker final-control participant/capability owner与硬前置。 |

D2 `:631` 的“`RemoteFinalConsumptionCoordinator` 直接复用且不用改”被本 Delta 撤回；receipt transport algorithm/endpoint仍复用，但 coordinator、两个 ack、两个 digests、broker validation、local ledger participant 与三份 strict docs 必须同波。

### 4. working-memory permit 与 retained artifact lease 分账

D3 profile bounds数值保持不变，但 reservation 拆成互不转换、互不延长的 owner：

| quota class | exact owner | covers | release |
| --- | --- | --- | --- |
| DHXY local working | `com.bot.dhxy.service.tasktracker.TaskTrackerWorkingMemoryGovernor.WorkingPermit` | live/snapshot decode、full/panel/detail `BufferedImage`、encode scratch；profile `3,683,356/3,665,856` bounds | `TaskTrackerPanelService` typed read handler `finally`，在 image flush且encode buffer ownership transfer后；success/NOT_EXECUTED/UNKNOWN/throw/interrupt都退。 |
| DHXY transport working | 同 governor 的独立 `TransportPermit` | streaming Base64/JSON scratch，不含 retained raw blob | `HttpRemoteCommandTransport.submitOutcome(...)` 每次调用 `finally`；网络 uncertain也退，重投时重新短租。 |
| DHXY retained artifact | `RemoteOperationLedger.TrackerArtifactEntry` + `RetainedArtifactCharge` | artifact/frame count与actual encoded raw bytes（profile最多 `1/2/1MiB`） | read release，或 owner转 materialize lease后在 materialize final/可信取消/exact terminal释放；绝不携带 working bytes。 |
| Cloud deferred inbound | `RemoteEncodedOutcomeCapacityGovernor.DeferredInboundPermit` | dispatch前预留未来一次 late outcome parse所需copy slots/`7,820,640` headroom；尚无实际 buffer | exact outcome开始解析时转同 permit的 ACTIVE；若 timeout/UNKNOWN 且仍可 late outcome则只保留此 deferred permit，broker证明不会再有outcome时退。 |
| Cloud active working | 同 governor `ActiveInboundPermit` | envelope tree/decode input/constructor scratch | endpoint typed decode + digest validation调用 `finally`；不随 broker outcome或artifact lease保留。 |
| Cloud retained outcome | `RemoteEncodedOutcomeCapacityGovernor.RetainedOutcomePermit` | broker唯一 typed encoded arrays的actual count/bytes | exact receipt compact/digest-only witness后退；late仍可能带image时保留worst-case permit，确定non-image/never-late后settle为0并退。 |

副作用前 local 顺序是 `reserve WorkingPermit -> reserve RetainedArtifactCharge -> capture/optional drag`；第二步失败先退 working再返回 `NOT_EXECUTED/CAPACITY`，零 capture/drag。成功 encode 后 raw buffer ownership transfer给 artifact entry，retained charge settle actual；随后 handler `finally` 无条件退 working。read final 的 owner transition只移动 `RetainedArtifactCharge`，与 working governor没有任何调用。

Cloud dispatch前分别 reserve `DeferredInboundPermit` 与 `RetainedOutcomePermit`；任何一个失败均在 route offer前原子释放另一个并返回 capacity `NOT_EXECUTED`。outcome到达后 active working在本次 endpoint调用结束即退，retained outcome按actual settle。`UNKNOWN` 分类必须精确：

- DHXY handler尚未commit artifact：所有 working与未commit retained reservation都退；partial image/scratch不能跨调用保留。
- DHXY exact OBSERVED outcome已commit但 submit uncertain：working/transport permits退，唯一 retained artifact/raw blob继续保留供同 outcome重投。
- Cloud dispatch timeout但 late outcome仍可能到达：没有 active buffers；只保留 deferred inbound permit与future retained permit，两者确实可能被late outcome使用。
- Cloud exact non-image terminal且broker禁止late image：deferred与retained image permits都退，只留普通semantic record quota。
- Cloud image outcome已accepted：active working立即退，actual retained permit留到receipt compact；不把 `7.8MiB` working或 DHXY `3.6MiB` working挂到materialize lease。

same-key-before-quota、actual settle、owner/global caps、zero-owner bucket removal沿用D3；本节只修owner与release，不改任何已通过数值。

### 5. Parent Review #3 关闭矩阵

| review item | D4 closure |
| --- | --- |
| P1-1 materialize早于local lease | `LOCAL_RETAIN_RECEIPT_COMPACTED_READY` 是唯一dispatch态；`DEPENDENCY_NOT_READY`返回opaque continuation，receipt compact后sticky level wake；零poll/sleep/retry/thread。 |
| P1-2虚构跨仓单事务 | Cloud dependency纳入`CloudTaskRunActionLedger`单锁，DHXY artifact/lease纳入`RemoteOperationLedger.monitor`；给prepare/mutation/notice commit/receipt commit与pre-mutation rollback、post-mutation unknown owner，跨仓仅ack/receipt handshake。 |
| P1-3 attachment写集不闭合 | 列Cloud+DHXY attachment DTO、两个ack model、两个digest canonicalizer、coordinator/broker/local participant、三份strict protocol/state docs与operation-specific旧ack逐字兼容。 |
| P2-1 working与retained混账 | local working/transport、local retained artifact、Cloud deferred/active working、Cloud retained outcome各自permit/owner/release；UNKNOWN只保留late outcome仍可能使用的permit。 |

### 6. Replacement Worker U2 self-QA（不是父级批准）

- P0: 0。没有跨仓伪原子、receipt前materialize dispatch、raw API/mint旁路、TTL/restore/takeover或UNKNOWN重放。
- P1: 0。三个P1分别由exact receipt causal gate、两侧真实单锁+post-mutation fail-closed、完整双仓attachment wire/digest/schema/local participant闭合。
- P2: 0。working与retained permit已按物理生命周期分账，D3已通过profile数值与ordinary CAPTURE quota未重开。
- 普通 `WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE` final ack在attachment absent时wire bytes/JCS/ackDigest逐字不变；tracker两个operation要求双仓同波，不支持mixed-version忽略。
- **无已批准业务差异；按基线等价迁移。** 本QA不构成`Approved`，只等待父级复审。

## Replacement Internal Worker U2 - DELIVERED / Design Repair #3 Delta - 2026-07-13T11:50:34.1567396-04:00

- task: `W-TTPS-D4`
- delivery: Parent Design Review #3 的 `P1 x3/P2 x1` 已按本 Delta 精确返修，等待父级复审；未自行批准。
- verification: 完整读取固定日志并只读核对当前 `RemoteFinalConsumptionCoordinator`、双仓 `RemoteFinalConsumedAck`/digest、Cloud action/broker compaction与DHXY local apply；按冻结要求未运行 Maven、测试或运行面。

## Parent Design Review #4 - DESIGN APPROVED / PROTOCOL WAVE AUTHORIZED - 2026-07-13T11:55:00-04:00

父级按当前 action-ledger/broker/final-consumption/local-ledger 锁边界复审 D4。exact retain receipt 作为 materialize 唯一
dispatch 门、sticky level continuation、两仓各自 prepare/commit/unknown owner、typed attachment 绑定 ackDigest，以及 working/
retained 五类物理额度分账均已闭合；不再声称跨仓事务或 receipt 前 readiness。

结论：整体设计 **APPROVED，P0=0/P1=0/P2=0**。D2 已通过的 profile/图片副本/ordinary CAPTURE quota 与 D4
修订共同构成实施合同；旧三 operation 在 attachment absent 时 JSON key set、canonical JCS 与 digest 必须逐字不变，两个
tracker operation 只允许双仓同波 dormant 落码，不支持 mixed-version 激活。Worker QA 不替代本结论。

### 下一任务 `W-TTPS-T1A-IMP1`（双仓 dormant strict protocol cohort）

同一 U2 先追加 `CLAIMED`，然后实施 D2-T1 中**仅协议类型/strict codec/digest/final-ack attachment**的自包含第一波：

- Cloud New：`TaskTrackerReadRequest`、`TaskTrackerReadOutcome`、`TaskTrackerMaterializeRequest`、
  `TaskTrackerMaterializeOutcome`、`TaskTrackerFinalConsumedAttachment`。
- Cloud Modify：`RemoteOperation`、`RemoteRequest`、`RemoteOutcome`、`RemoteCommandEnvelope`、
  `RemoteCommandOutcomeEnvelope`、`RemoteProtocolDigests`、`RemoteFinalConsumedAck`。
- DHXY New：`RemoteTaskTrackerReadCommandPayload`、`RemoteTaskTrackerReadOutcomePayload`、
  `RemoteTaskTrackerMaterializeCommandPayload`、`RemoteTaskTrackerMaterializeOutcomePayload`、
  `RemoteTaskTrackerFinalConsumedAttachment`。
- DHXY Modify：`RemoteGameOperation`、`RemoteOperationPayloadCodec`、`RemoteProtocolDigests`、
  `RemoteGameOutcomeEnvelope`、`RemoteFinalConsumedAck`。
- Docs Modify：`docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`（只追加本 cohort 的 strict schema、
  required/forbidden matrix、property order 与 ack/lease digest）。

本波不接 handler/port/ledger/broker/coordinator/assembly/caller/host，不启动任何 producer；新 operation 即使被外部伪造也必须
在既有未支持 handler 路径 fail-closed，不能执行机械副作用。所有新 DTO 必须 closed validation、无 raw path/HWND/mint；
普通 `CAPTURE/WINDOW_FACT/EXECUTE_INPUT_BUNDLE` 的 bytes/digest/行为逐字不变。不得新增/恢复 tests，不 git mutation。

Java 写完后先确认 Internal X 已停止 Cloud 写入，再由 U2 运行 Cloud `mvn -q clean package`（不可 skip）与 DHXY
`mvn -q -DskipTests compile`；若 X 尚在写，只追加 `READY_FOR_BUILD` 并等待父级继续信号，不并发 clean。Implementation #1
须列精确文件、old-operation canonical parity 依据、new-operation strict validation 与双构建结果。

**无已批准业务差异；按基线等价迁移。**

## Replacement Internal Worker U2 - CLAIMED - 2026-07-13T11:58:38.7185791-04:00

- task: `W-TTPS-T1A-IMP1`
- claimedAt: `2026-07-13T11:58:38.7185791-04:00` (`America/New_York`)
- uniqueWriteSet: 仅 Parent Design Review #4 明列的 Cloud 5 New + 7 Modify、DHXY 5 New + 5 Modify、`docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`，以及本 append-only 日志；不含 handler/port/ledger/broker/coordinator/assembly/caller/host/tests/Maven/resources。
- 并发保护：不回滚、不覆盖、不清理任何 dirty/untracked 或 Internal X/其他 Worker 在途改动；不执行 git mutation，不启动运行面。代码完成后若 X 仍在写 Cloud 文件，只追加 `READY_FOR_BUILD`，不并发 clean/build。

## Replacement Internal Worker U2 - Implementation #1 - SOURCE COMPLETE / BUILD BLOCKED - 2026-07-13T12:18:00.7556888-04:00

- task: `W-TTPS-T1A-IMP1`
- implementedAt: `2026-07-13T12:18:00.7556888-04:00` (`America/New_York`)
- status: Review #4 的 dormant strict protocol 源码与 schema 写集已完成；双仓 build gate 被写集外旧枚举穷尽 switch 阻断，未标记 DELIVERED/APPROVED，交父级复审与下一 cohort 授权。

### 1. 精确实施文件

Cloud New：

- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerReadRequest.java`
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerReadOutcome.java`
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerMaterializeRequest.java`
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerMaterializeOutcome.java`
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTrackerFinalConsumedAttachment.java`

Cloud Modify：

- `remote/RemoteOperation.java`
- `remote/RemoteRequest.java`
- `remote/RemoteOutcome.java`
- `remote/RemoteCommandEnvelope.java`
- `remote/RemoteCommandOutcomeEnvelope.java`
- `remote/RemoteProtocolDigests.java`
- `remote/RemoteFinalConsumedAck.java`

DHXY New：

- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerReadCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerReadOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerMaterializeCommandPayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerMaterializeOutcomePayload.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerFinalConsumedAttachment.java`

DHXY Modify：

- `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOperation.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOutcomeEnvelope.java`
- `src/main/java/com/bot/dhxy/cloud/remote/RemoteFinalConsumedAck.java`

Docs Modify：

- `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`：只增加本 cohort 的 operation/payload/profile/outcome/final-control strict schema 与 deployment gate。
- 本固定日志：append-only claim/implementation evidence。

没有修改 handler/port/ledger/broker/coordinator/assembly/caller/host/Maven/resources/tests；没有创建 producer、thread、poll/retry/wake；没有 git mutation、提交或运行面启动。

### 2. old-operation canonical parity

1. `RemoteCommandEnvelope` 的既有 `CaptureRequest`、`WindowFactRequest`、`InputBundleRequest` 三个 payload branch 原语句、字段名和写入顺序保持不变；tracker branch 只追加在三者之后。
2. Cloud/DHXY request digest 的旧 context/tree 组装保持不变；tracker read/materialize 只新增独立 typed overload/branch，没有给 ordinary CAPTURE 增加字段，也没有改变 CAPTURE `imageBytes` 排除规则。
3. outcome digest 对 `TASK_TRACKER_READ` 才复制 `frames[]` metadata 并删除各自 `imageBytes`；普通 CAPTURE 仍只删除原 root `imageBytes`，WINDOW_FACT/EXECUTE_INPUT_BUNDLE 不进入新分支。
4. final ack 新字段 `trackerArtifactControl` 紧邻 `ackDigest`，两端继续 `NON_NULL`；旧三 operation constructor 强制 attachment absent，wire/JCS 中不出现该 key。Cloud/DHXY 均保留旧参数 constructor 代理到 attachment=null，既有构造调用与 canonical key set不变。
5. `computeFinalConsumedAckDigest` 仍对完整 typed ack `valueToTree` 后只删除 `ackDigest`；旧 operation 因 attachment absent 得到旧 JCS/ackDigest，新 tracker operation 才把 attachment 纳入 digest。

本轮没有写 parity testcase；按 Review #4 禁止新增 tests。Cloud package 在 compile 阶段即被写集外 switch 阻断，未进入测试阶段，因此没有伪称 runtime/test parity 已执行。

### 3. new-operation strict validation

- operation：双仓同波新增 `TASK_TRACKER_READ`、`TASK_TRACKER_MATERIALIZE_ACTION`；Cloud sealed request/outcome union同步闭合，未接任何 handler/producer。
- read command：closed `captureId/readProfile/source/allowPanelReposition`；四个 profile 固定 frame/encoded/working/transient bounds；source 只允许 live 或 exact existing `captureId/imageSha256/artifactId`，existing source禁止 reposition。
- read outcome：payload exact 7 keys；仅 OBSERVED 允许 typed artifact/ordered frames/mechanical fact/observedWindow，其它 state 要求六个 observation key explicit null；frame role/order、WINDOW_CLIENT_PX ROI、profile dimensions、单帧/aggregate bytes、SHA 与 capture occurrence 均校验。
- materialize command：closed artifact/observation/dependency lease/selected link/fixed prepared operation/target/fixed validation policy；click必须在 window-relative rect 内；不含 image bytes/path/HWND/screen-absolute click/mint。
- materialize outcome：payload exact 6 keys；EXECUTED 只允许 publish/already-published，NOT_EXECUTED 只允许 dependency-not-ready/stale/safety-rejected，UNKNOWN/STOPPED 全部 operation fields explicit null，OBSERVED禁止。
- lease：两端同构 `SHA-256(JCS(artifactId,artifactDigest,sourceReadActionId,sourceReadSemanticAddress,materializeActionId,materializeSemanticAddress))`，directive 不参与；materialize command与attachment均重算 exact compare。
- final control：attachmentVersion=1、9-field order、五个 closed directive、RELEASE_AFTER_READ 三个 materialize字段 absent、其余完整 lease；ack outer operation/state/disposition/action/address 与 directive matrix双端校验，explicit JSON null/unknown field fail closed。
- dormant gate：新 operation 没有 handler/port/ledger/broker/coordinator/assembly/caller 接线；本 cohort 自身不具备截图、drag、publish、wake 或 phase side effect 能力，不支持 mixed-version 降级。

### 4. 双构建证据与 blocker

Internal X 已由父级确认停止写入并关闭；U2 在自身目标文件时间戳稳定后才执行 clean/build。

1. Cloud：在 `D:/mavenProject/dhxy-cloud-brain` 执行父级指定 `mvn -q clean package`（未 skip），exit 1，compile 唯一错误：
   `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java:244` 的旧三 operation switch expression 不再穷尽新增枚举。
2. DHXY：在 `D:/mavenProject/DHXY` 执行父级指定 `mvn -q -DskipTests compile`，exit 1，compile 三个错误：
   `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java:132,301,799` 的旧三 operation switch expression 不再穷尽新增枚举。
3. 以上 `CloudTaskRetainedActionState` 与 `LocalRemoteGameCommandHandler` 均被 Parent Review #4 明确冻结且不在 uniqueWriteSet。U2 没有越界加入 default、伪 handler 或机械执行分支；当前 build blocker 只能由父级把这些 fail-closed switch closure 纳入下一批准 cohort 后解除。
4. javac 未报告本批准写集内其它语法/类型错误；但因 compile gate 未通过，不能把该结果表述为双构建成功。

### 5. U2 self-QA（不算父级批准）

- P0: 0。无 CAPTURE 副作用、raw authority、mixed-version bypass、receipt前materialize、线程/poll/retry或跨仓伪事务。
- P1: 1 build blocker。新增 enum 与冻结的旧穷尽 switch 无法在同一次全仓 compile 共存；证据及精确文件/行号如 4 节，U2 按写集边界未越权修复。
- P2: 0。closed DTO、profile bounds、nested image exclusion、lease/ack attachment与 old-operation NON_NULL parity 已双仓同构落码。
- review gate: 本 self-QA 不构成 Approved；当前状态是 `SOURCE COMPLETE / BUILD BLOCKED / Parent review required`。

**无已批准业务差异；按基线等价迁移。**

## Parent Source Review #1 - BLOCKED / NARROW DORMANT SWITCH REPAIR AUTHORIZED - 2026-07-13T12:24:00-04:00

父级已逐项复核 U2 的 Cloud/DHXY 新 DTO、closed payload codec、request/outcome digest、tracker lease digest、
final-consumed attachment 与 strict schema。协议 cohort 本身未发现新增 P0/P1/P2：普通三 operation 在
`trackerArtifactControl == null` 时不新增 wire key，tracker read 的 nested frame bytes 只从 outcomeDigest 排除且
仍由 `imageSha256` 绑定，`captureOccurrence` 在双仓 envelope/codec 均精确比对 outer semantic occurrence。

当前结论仍为 **BLOCKED，P0=0/P1=1/P2=0**，唯一 P1 是全仓编译门未通过：

- Cloud `CloudTaskRetainedActionState.newHandle(...)` 的旧三项 switch 未显式拒绝两个 dormant tracker operation；
- DHXY `LocalRemoteGameCommandHandler` 的 decode、execute、empty outcome 三个旧三项 switch 未闭合。

影响：当前源码不能生成 fresh Cloud jar/DHXY classes，不能作为已迁移或可交付代码；同时不能用 `default` 或伪 handler
吞掉新 operation，否则会失去 enum 扩展的审计门或误开机械副作用。

### 下一任务 `W-TTPS-T1A-IMP1-R1`

同一 U2 追加 `CLAIMED` 后只修改以下两个 Java 文件及本日志：

1. Cloud `remote/CloudTaskRetainedActionState.java`：在 `newHandle(...)` 对
   `TASK_TRACKER_READ` / `TASK_TRACKER_MATERIALIZE_ACTION` 使用显式 throw，声明本 cohort 尚无 retained handle；不得
   建 tracker handle、不得改 ledger/broker/assembly。
2. DHXY `cloud/remote/LocalRemoteGameCommandHandler.java`：
   - decode switch 对两个 tracker operation 先走本波 strict codec，再显式返回 dormant/unsupported 的 fail-closed
     `NOT_EXECUTED` 路径；
   - execute switch 对两项显式 throw unreachable/dormant，绝不进入 capture/input/window side effect；
   - empty outcome switch 生成 operation-specific strict payload：tracker read 保留 safe `captureId` 且其余六字段
     explicit null，materialize 六字段 explicit null，使拒绝 outcome 自身可被 strict decoder/digest 验证。

禁止新增 producer/handler 实现、caller、thread、poll/retry、测试或其它写集。完成后运行 Cloud
`mvn -q clean package`（不可 skip）及 DHXY `mvn -q -DskipTests compile`，交付精确 diff、双构建结果与 dormant
side-effect proof。Worker 自审不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## Replacement Internal Worker U2 - CLAIMED - 2026-07-13T12:28:05.3236450-04:00

- task: `W-TTPS-T1A-IMP1-R1`
- claimedAt: `2026-07-13T12:28:05.3236450-04:00` (`America/New_York`)
- uniqueWriteSet: 仅 Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`、DHXY `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` 与本 append-only 固定日志。
- scope guard: 只实施 Parent Source Review #1 批准的 dormant fail-closed switch repair；不新增真实 tracker handler/producer/caller，不修改其它文件，不新增测试，不执行 git mutation，不自行 Approved。

## Replacement Internal Worker U2 - Implementation #1 R1 - DELIVERED FOR PARENT REVIEW - 2026-07-13T12:32:45.7362873-04:00

- task: `W-TTPS-T1A-IMP1-R1`
- deliveredAt: `2026-07-13T12:32:45.7362873-04:00` (`America/New_York`)
- status: Parent Source Review #1 批准的 narrow dormant switch repair 已实施且双构建通过；仅交父级复审，Worker 未自行 Approved。

### 1. 精确 diff

1. Cloud `CloudTaskRetainedActionState.java`，仅改 `newHandle(...)` switch：
   - 新增 `case TASK_TRACKER_READ -> throw new IllegalStateException("TASK_TRACKER_READ is dormant and has no retained action handle")`。
   - 新增 `case TASK_TRACKER_MATERIALIZE_ACTION -> throw new IllegalStateException("TASK_TRACKER_MATERIALIZE_ACTION is dormant and has no retained action handle")`。
   - 原 `WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE` 三个 handle constructor case 逐字未改；没有新增 tracker handle、retain API、ledger/broker/assembly wiring。
2. DHXY `LocalRemoteGameCommandHandler.java`，decode switch：
   - `TASK_TRACKER_READ` 调用现有 strict `payloadCodec.readTaskTrackerRead(command.getPayload())`。
   - `TASK_TRACKER_MATERIALIZE_ACTION` 调用现有 strict `payloadCodec.readTaskTrackerMaterialize(command)`。
   - strict decode 成功后，两项在 `commandAdmissionSnapshot`/`operationLedger.claim` 前立即返回 `NOT_EXECUTED/INVALID_REQUEST`，message=`task tracker operation is dormant and unsupported by the local handler`。
3. DHXY `executeOwnedCommand(...)`：
   - 方法入口在 `requireRegistration`/`requireBoundWindow` 前对两个 tracker operation 显式 throw dormant unreachable。
   - operation switch 另分别加入两个显式 throw case，保持 enum 扩展审计门；没有 default，没有调用 capture/window/input executor。
4. DHXY `emptyOutcomePayload(...)`：
   - tracker read 返回 `RemoteTaskTrackerReadOutcomePayload`：safe non-blank `captureId`，`readProfile/source/artifact/frames/mechanicalFact/observedWindow` 六项 explicit null。
   - materialize 返回 `RemoteTaskTrackerMaterializeOutcomePayload`：`artifact/observationDigest/preparedActionId/publishDisposition/validationFingerprintDigest/observedWindow` 六项 explicit null。
   - 原 CAPTURE/WINDOW_FACT/EXECUTE_INPUT_BUNDLE 三个 empty payload case 逐字未改。

### 2. dormant side-effect proof

1. requestDigest 仍先按 typed request 校验；tracker command 随后必须通过 operation-specific strict codec。malformed payload 在 decode catch 中直接生成 strict `NOT_EXECUTED/INVALID_REQUEST` outcome。
2. valid tracker payload 在 decode 后立即 terminal return；源码顺序为 dormant return -> `commandAdmissionSnapshot` -> `operationLedger.claim`，所以 tracker 不登记 ledger owner、不进入 duplicate/renewal/frontier流程。
3. dormant return 更早于 `executeOwnedCommand`；因此不会调用其中的 `requireBoundWindow`、`bindingRefreshService.refreshAndCommit`、`captureService`、window fact reader、`inputActionQueue`、focus或任何 runtime publish/wake。
4. `executeOwnedCommand` 自身又在首个 registration/window调用前设 fail-closed guard；其 switch 的两个 tracker case 只 throw，构成误路由时的第二道无副作用门。
5. Cloud `newHandle` 两项只 throw，不能铸造 `ActionHandle`；当前文件没有 tracker retain/renew/invoke入口，且本任务未修改 ledger/broker/assembly。
6. tracker拒绝 outcome使用 operation-specific closed payload；`RemoteOperationPayloadCodec.toPayloadTree` 保留规定的 explicit-null key，现有 outcome digest可计算，Cloud strict outcome decoder可验证，不借普通 CAPTURE/window/input payload降级。
7. 未新增真实 tracker handler/producer/caller、线程、poll/retry、wake、测试或其它源码；未执行 git mutation。

### 3. 双构建结果

1. Cloud：`D:/mavenProject/dhxy-cloud-brain` 执行 `mvn -q clean package`（不可 skip），exit `0`，耗时约 `98.5s`；compile/package 与现有测试全部通过。
2. DHXY：`D:/mavenProject/DHXY` 执行 `mvn -q -DskipTests compile`，exit `0`，耗时约 `30.9s`；fresh classes 编译成功。
3. 上一轮唯一 P1 的四处 enum switch compile blocker 已在批准写集内闭合；本轮未创建/运行额外测试，Cloud 仅运行父级明确要求的既有 package test lifecycle。

### 4. Worker self-QA（不算父级批准）

- P0: 0。没有激活 tracker mechanical/business side effect。
- P1: 0。Cloud 1处与DHXY 3处穷尽 switch均显式闭合，双仓 build gate通过。
- P2: 0。拒绝 outcome保持新 operation 的 strict payload形状，旧三 operation case未改。
- review gate: `DELIVERED FOR PARENT REVIEW`；U2 不是 reviewer，本结论不是 Approved。

**无已批准业务差异；按基线等价迁移。**

## Parent Source Review #2 - FINAL APPROVED - 2026-07-13T12:40:00-04:00

父级已独立复核 Repair #1 的两处真实源码。Cloud `CloudTaskRetainedActionState.newHandle(...)` 对两个
tracker operation 均为显式 dormant throw；DHXY handler 先 strict decode，再于 admission/ledger/window/input 之前返回
`NOT_EXECUTED/INVALID_REQUEST`，误路由 execute 仍有显式 throw，拒绝 outcome 也保持 operation-specific explicit-null
closed payload。没有 producer、caller、机械副作用、renewal、thread/poll/retry 或 host 激活。

父级 fresh 构建证据：Cloud `mvn -q clean package` exit 0，Surefire `suites=4, tests=21, failures=0,
errors=0, skipped=0`；在 External B 写入稳定后，DHXY `mvn -q -DskipTests compile` 于
`2026-07-13T12:39:xx-04:00` 再次 exit 0。结论为 **FINAL APPROVED，P0=0/P1=0/P2=0**。
本批准只覆盖 dormant TaskTracker strict protocol cohort，不激活 capture/OCR/materialize/caller/host。

**无已批准业务差异；按基线等价迁移。**
