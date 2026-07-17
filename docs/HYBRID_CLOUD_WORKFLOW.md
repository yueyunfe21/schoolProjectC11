# DHXY Hybrid 云端化工作流程

本文档定义 DHXY 商业版云端化的实际推进流程。目标不是一次性把本地 Java Service 整个搬走，而是把核心策略逻辑拆成小的云端决策服务，逐个接入、影子验证、灰度执行、可随时回滚。

当前工作分支：

```text
codex/hybrid-cloud-protection
```

当前基线：

```text
696a12b chore: remove obsolete debug tooling
```

## 1. 总原则

1. 核心逻辑能放云端就放云端，但真实执行安全边界必须留本地。
2. 云端负责策略、候选排序、阈值、资产、失败归因、feature flag。
3. 本地负责窗口绑定、截图、输入队列、pause/stop、二次校验、真实点击、最终安全判断。
4. 每个云端能力必须可以单独开启、单独关闭、单独看日志。
5. 所有能力先 Shadow，再 Hybrid 执行，不允许一次性大迁移。
6. 任何改变点击点、模板匹配、导航点位的执行逻辑，都必须按 AGENTS.md 做 testcase replay。
7. 云端失败不能让本地进入未知状态，必须有明确 fallback：停、继续本地、或只记录。
8. 洗字/图像预处理能力属于核心资产，最终发布形态不允许保留在本地 runtime。黄字、绿字、白字、
   紫字等 `ImagePreprocessor` 级别的清洗、二值化、指纹/模板前处理都必须迁到云端；本地只允许截图、
   裁剪、上传原始图或必要 ROI，并执行云端返回的受限结果。
9. 本地可以保留窗口绑定、截图、输入队列、pause/stop、坐标边界校验和真实点击，但不能保留可独立复用的
   文字清洗/识别策略包作为生产路径 fallback。

### 1.1 Cloud Brain API Gateway / 模块化单体约定

当前阶段的云端结构按“API Gateway + 逻辑 microservice + 单 JVM 部署”推进：

1. DHXY 本地客户端只和 cloud-brain API Gateway 通信，不直接知道内部 service 类或策略实现。
2. API Gateway 只负责 HTTP 边界：鉴权、method/path 路由、JSON 解析、统一错误响应、trace/log 入口。
3. Gateway 后面的每个能力先作为同一个 `dhxy-cloud-brain` 进程里的逻辑 service/endpoint，不在第一版拆成多个服务器。
4. 内部 service 之间第一版用强类型 Java 方法调用，由 orchestrator/brain 负责串联；不要在同 JVM 内部绕一圈 HTTP。
5. 外部路径要逐步从泛化 `/api/cloud/decision + serviceId` 收口到清晰的能力接口，例如 `xiuluo/brain/start`、
   `npc-click/start`、`tracker-panel/read`、`image/preprocess`。旧路径必须通过兼容 adapter 平滑保留，不能一次性打断现有 DHXY 客户端。
6. 每次迁移一个入口时都必须证明：旧 endpoint 兼容、云端新 endpoint 有独立测试、本地调用点是否切换有明确 CR/fresh gate。

## 2. 两层拆分

### 2.1 大 Service，给人拍板

这些是产品/架构层面的决策单位：

| ID | 大 Service | 推荐方式 | 说明 |
|---|---|---|---|
| SVC-01 | Task Strategy | Hybrid | 五倍/修罗任务策略、phase、失败恢复 |
| SVC-02 | Navigation | Hybrid | 黄字路线、地图标定、route memory |
| SVC-03 | Interaction | Hybrid | 绿色链接、NPC click smart、Dialog 策略 |
| SVC-04 | Team/Maintenance | Hybrid | 队伍放权、归队、盒子、补给策略 |
| SVC-05 | Asset/Memory | Cloud | 地图/模板/策略包/学习资产 |
| SVC-06 | Diagnostics/Policy | Shadow first | 失败归因、指标、灰度、热修 |

### 2.2 小云端 Service，给开发落地

真正开发时按小 Service 做，不按巨型 Service 做。

| 小 Service | 属于 | 第一阶段目标 |
|---|---|---|
| TaskClassifierCloud | Task Strategy | 判断任务类型、标题、状态 |
| TaskPolicyCloud | Task Strategy | 返回下一步 phase/branch |
| TaskRecoveryCloud | Task Strategy | 返回 retry/reaccept/stop 策略 |
| RouteCandidateCloud | Navigation | 给黄字候选评分 |
| MapTransformAssetCloud | Navigation / Asset | 下发地图坐标转换资产 |
| RouteMemoryCloud | Navigation / Asset | 管 clean/dirty/降权经验 |
| TrackerLinkRankerCloud | Interaction | 排绿色链接候选 |
| NpcClickStrategyCloud | Interaction / Vision | 云端识别 NPC click smart 目标、策略和 retry 状态机 |
| DialogPolicyCloud | Interaction | 给业务选项和 fallback 顺序 |
| CapabilityGateCloud | Team/Maintenance | 下发允许哪些队员能力 |
| MaintenanceThresholdCloud | Team/Maintenance | 下发补给/摄妖香/三技能阈值 |
| TeamReturnPolicyCloud | Team/Maintenance | 下发归队窗口、超时、优先级 |
| ImagePreprocessCloud | Asset/Memory / Vision | 云端洗黄字/绿字/白字/紫字等 OCR/template 前处理 |
| SummonSkillCloud | Team/Maintenance / Vision | 云端识别三技能 tooltip/状态并返回删除/保留动作 |
| SignedAssetBundleCloud | Asset/Memory | 下发签名资产包 |
| LearnedMemoryCloud | Asset/Memory | 管 NPC/route 学习点 |
| PolicyVersionCloud | Asset/Memory | 按 license/session 下发策略版本 |
| FailureClassifierCloud | Diagnostics | 分类失败原因 |
| FeatureFlagCloud | Diagnostics | 灰度和热修开关 |
| MetricsIngestCloud | Diagnostics | 收集延迟、错判、fallback 指标 |

## 3. 每个小 Service 的标准推进流程

每个小云端 Service 都必须按下面 7 步走。

### Step 0：选最小能力边界

进入开发前必须回答：

- 这个 Service 只做什么？
- 它不做什么？
- 本地原逻辑在哪里？
- 云端返回的是 enum、candidate id、policy version，还是坐标？
- 云不可用时 fallback 是什么？

输出物：

```text
serviceId
request schema
response schema
feature flags
fallback mode
日志字段
```

禁止事项：

- 不允许第一版返回未验证的裸点击动作。
- 不允许云端直接控制输入队列。
- 不允许把本地 phase 语义悄悄改掉。

### Step 1：Cloud Decision Framework 骨架

先做统一框架，不先接业务。

本地需要有：

- `CloudDecisionClient`
- `CloudDecisionRequest`
- `CloudDecisionResponse`
- `CloudDecisionServiceId`
- timeout 配置
- feature flag 配置
- fallback 配置
- trace id / task run id / window id 日志
- 本地 mock 云端或 fake client

云端响应必须至少包含：

```text
serviceId
traceId
policyVersion
decision
confidence
ttlMs
fallbackReason
diagnostics
```

本地必须记录：

```text
cloud.decision serviceId=... mode=shadow|execute traceId=...
localDecision=... cloudDecision=... effectiveDecision=... agree=true|false executed=true|false elapsedMs=...
fallback=... window=... task=... phase=...
```

验收标准：

- 不接任何业务 hook 时，项目能编译。
- mock client 能返回固定建议。
- 云端超时、异常、空响应都有日志。
- 不影响五倍/修罗正常本地执行。

### Step 2：Shadow 接入

Shadow 模式只记录，不执行云端建议。

流程：

```text
本地照旧算 decision
同时构造 cloud request
调用云端或 mock
记录 local vs cloud
本地仍执行原 decision
```

Shadow 日志必须能回答：

- 云端建议是什么？
- 本地实际做了什么？
- 是否一致？
- 云端耗时多少？
- 云端失败时 fallback 是什么？
- 这条建议对应哪一个窗口、哪一轮任务、哪一个 phase？

验收标准：

- 连跑日志里能看到 shadow 对比。
- 云端慢或失败时，本地任务不被拖死。
- 不改变任何点击、导航、任务 phase 的实际行为。

### Step 3：Shadow 数据复盘

一个小 Service 进入 Hybrid 执行前，必须先看 Shadow 数据。

最低复盘项：

- 一致率
- 云端 P50/P95/P99 延迟
- 云端失败率
- fallback 次数
- 本地与云端不一致的样本
- 是否有误判会导致任务卡住/误点/浪费道具

建议门槛：

```text
核心点击/导航类：一致率 >= 99%，P95 延迟在可接受窗口内
策略/阈值类：一致率 >= 95%，不一致样本可解释
诊断类：不要求一致率，但不能影响本地执行
```

如果达不到门槛：

- 保持 Shadow。
- 调整云端规则。
- 补日志/补样本。
- 不进入执行模式。

### Step 4：Hybrid 执行灰度

只有通过 Shadow 复盘的小 Service 才能进执行模式。

执行模式也必须有灰度：

```text
execute=false        只 shadow
execute=true, pct=0  已接入但不生效
execute=true, pct=5  小流量执行
execute=true, pct=50 扩大验证
execute=true, pct=100 全量执行
```

执行时本地仍必须做：

- ttl 检查；
- policyVersion 检查；
- window/session 检查；
- fingerprint/模板/状态二次校验；
- pause/stop 检查；
- fallback 检查。

执行日志必须包含：

```text
cloud.execute serviceId=... accepted=true|false
rejectReason=ttl_expired|low_confidence|state_mismatch|window_mismatch|paused|timeout|...
```

验收标准：

- 单个小 Service 可独立开关。
- 一键关闭后立刻回本地旧逻辑。
- 失败后不会卡死窗口，也不会重复执行危险动作。

### Step 5：实战验证

每个小 Service 的执行模式必须有实战验证点。

最低要求：

- 五倍至少覆盖普通怪、暗雷、白龙马、黄袍其中相关场景。
- 修罗至少覆盖正常进战、战后回程、失败恢复其中相关场景。
- 导航必须覆盖成功路线和失败降权。
- 绿色链接/NPC/Dialog 必须保留 testcase replay 标记图。
- 队伍维护必须覆盖队长放权、队员归队、盒子优先级。

验证记录写到：

```text
docs/ACTIVE_WORK.md
docs/run-reports/...
```

### Step 6：可回滚发布

每个小 Service 发布前必须有回滚方式：

```text
cloud.services.<service-id>.shadow-enabled=false
cloud.services.<service-id>.execute-enabled=false
cloud.services.<service-id>.fallback=LOCAL
```

回滚后要求：

- 本地旧逻辑仍能跑。
- 不需要重新打包客户端即可通过配置关闭。
- 日志明确记录关闭原因。

## 4. 推荐开发顺序

### Phase A：框架，不接业务

目标：

- 建 Cloud Decision Framework。
- 加 mock client。
- 加 feature flag。
- 加统一日志。

不做：

- 不改五倍/修罗行为。
- 不改点击点。
- 不改导航策略。

### Phase B：低风险 Shadow

优先接：

1. FailureClassifierCloud
2. MetricsIngestCloud
3. FeatureFlagCloud

原因：

- 不影响执行。
- 能先把日志、延迟、云端健康度跑起来。

### Phase C：高价值 Shadow

再接：

1. TrackerLinkRankerCloud
2. NpcClickStrategyCloud
3. DialogPolicyCloud
4. RouteCandidateCloud
5. TaskClassifierCloud

原因：

- 这些最值钱。
- 但必须先 shadow 看一致率。

### Phase D：第一个 Hybrid 执行点

优先选一个最小、可回滚、影响面清楚的点。

推荐候选：

```text
TaskClassifierCloud
```

原因：

- 它只判断类型，不直接点鼠标。
- 错了会走错分支，但本地可以保留二次验证和 fallback。
- 日志容易看懂。

备选：

```text
TrackerLinkRankerCloud
```

原因：

- 价值高。
- 但接近点击行为，必须更谨慎。

### Phase E：资产云端化

再做：

1. SignedAssetBundleCloud
2. MapTransformAssetCloud
3. RouteMemoryCloud
4. LearnedMemoryCloud
5. PolicyVersionCloud

这阶段要重点处理：

- license；
- device binding；
- asset TTL；
- version；
- offline grace period；
- revoke；
- cache encryption/signature。

## 5. 配置命名建议

当前 skeleton 使用 `CloudDecisionProperties.services` map，所以服务级配置必须用 `cloud.services.<service-id>.*`。

```properties
cloud.enabled=false
cloud.base-url=http://127.0.0.1:18080
cloud.timeout-ms=300
cloud.default-fallback=LOCAL

cloud.services.task-classifier.shadow-enabled=false
cloud.services.task-classifier.execute-enabled=false
cloud.services.task-classifier.execute-percent=0
cloud.services.task-classifier.fallback=LOCAL

cloud.services.tracker-link-ranker.shadow-enabled=false
cloud.services.tracker-link-ranker.execute-enabled=false
cloud.services.tracker-link-ranker.execute-percent=0
cloud.services.tracker-link-ranker.fallback=LOCAL
```

fallback 可选值：

```text
LOCAL      云失败时继续本地旧逻辑
STOP       云失败时停止当前任务
SHADOW_ONLY 永远不执行云建议
```

## 6. 请求/响应安全规则

请求必须带：

- license/session；
- device fingerprint；
- app version；
- policy version；
- service id；
- task run id；
- window id；
- nonce；
- timestamp；
- request hash。

响应必须带：

- trace id；
- decision；
- confidence；
- ttl；
- policy version；
- signature 或 HMAC；
- fallback reason。

客户端必须检查：

- ttl 是否过期；
- response 是否匹配 request；
- service id 是否一致；
- session 是否一致；
- policy version 是否允许；
- signature 是否有效；
- confidence 是否达标。

## 7. 每次开发任务的固定 checklist

每接一个小 Service，任务卡必须写：

```text
Service id:
Hook point:
Local baseline:
Request fields:
Response fields:
Shadow log:
Fallback:
Feature flags:
Test command:
Runtime verification:
Rollback switch:
```

开发完成前必须确认：

- 编译通过；
- Shadow 不改变行为；
- 日志能定位窗口/任务/phase；
- 云失败能 fallback；
- execute 默认关闭；
- 文档更新；
- 如果更新 CR，dashboard 已同步。

## 8. 第一张实现卡建议

建议第一张卡只做框架：

```text
CR-HC-001 Cloud Decision Framework skeleton
```

范围：

- 新增 cloud decision model；
- 新增 client interface；
- 新增 mock client；
- 新增配置；
- 新增统一日志；
- 不接五倍/修罗主流程。

验收：

- `mvn -q -DskipTests compile` 通过；
- 单元测试覆盖 mock success/timeout/failure；
- 没有改变任何任务行为；
- 文档记录下一步可接入的第一个 Shadow hook。

Implementation note:
- CR-HC-001 skeleton implemented in package `com.bot.dhxy.cloud.decision`.
- No business hook is connected yet.
- Execute mode remains disabled by default. If a service-level `execute-enabled=true` is configured
  during the skeleton stage, the coordinator may report `mode=EXECUTE` for diagnostics, but it still
  returns `effectiveDecision=localDecision` and `executed=false`; no cloud decision is executed.
- Next candidate hook: TaskClassifierCloud shadow mode.

## 9. CR-HC-002 TaskClassifierCloud 五倍/修罗 Shadow Hook

Service id:

```text
TASK_CLASSIFIER
```

Hook point:

- `TaskTrackerPanelService.readWubeiTrackerPanel(...)`
- `TaskTrackerPanelService.readWubeiTrackerPanelFromSnapshot(...)`
- `TaskTrackerPanelService.readWubeiTrackerDetail(...)`
- `TaskTrackerPanelService.readXiuluoTrackerPanel(...)`
- `TaskTrackerPanelService.readXiuluoTrackerPanelFromSnapshot(...)`
- `TaskTrackerPanelService.readXiuluoTrackerPanelForReplay(...)`
- `TaskTrackerPanelService.readXiuluoTrackerDetail(...)`

Local baseline:

- 五倍 tracker title-template matching remains the only business decision.
- The existing title template order stays unchanged:
  `殿前献艺` -> `三藏封魔` -> `宝象谜情` -> `智斗黄袍` -> `魁星归位`.
- 修罗 tracker shortcut matching remains the only 修罗 shortcut decision:
  local `xiuluo.tracker` / `NOT_FOUND` is reported to cloud, but cloud output is not used for
  shortcut click, pathing, route, battle, or recovery decisions.
- Every hook reports the already-built local `TaskTrackerPanelReadResult` and then returns that same
  local result. Cloud `effectiveDecision` is not read, not branched on, and not used for 五倍 phase,
  修罗 shortcut phase, navigation, click, retry, or task-key selection.

Request fields:

- `serviceId=TASK_CLASSIFIER`
- `taskCode=wubei` or `taskCode=xiuluo`
- `phase=tracker-title-classification`
- `localDecision=<五倍 taskKey / xiuluo.tracker / NOT_FOUND>`
- trace id prefix: `wubei-task-classifier:` or `xiuluo-task-classifier:`
- context: `source`, `found`, `taskKey`, `title`, `yellowText`, `greenLinkCount`,
  `probeObjective`, `detailRawPath`, `detailYellowPath`

Shadow log:

```text
cloud.decision serviceId=TASK_CLASSIFIER mode=SHADOW taskCode=wubei|xiuluo phase=tracker-title-classification ...
localDecision=... cloudDecision=... effectiveDecision=... agree=... executed=false
```

Fallback and rollback switch:

```properties
cloud.services.task-classifier.shadow-enabled=false
cloud.services.task-classifier.execute-enabled=false
cloud.services.task-classifier.execute-percent=0
cloud.services.task-classifier.fallback=LOCAL
```

Runtime verification:

```powershell
Select-String -Path logs/dhxy-console.log -Pattern "cloud.decision serviceId=TASK_CLASSIFIER"
```

Default acceptance:

- With `cloud.enabled=false` and task-classifier flags disabled, 五倍/修罗 tracker reads must stay quiet
  and local-only.
- With `cloud.enabled=true` plus
  `cloud.services.task-classifier.shadow-enabled=true`, logs may show shadow comparison samples, but
  `executed=false` and the returned local tracker result remains the only effective decision.

## 10. CR-HC-003 TrackerLinkRanker 五倍/修罗 Shadow Hook

Service id:

```text
TRACKER_LINK_RANKER
```

Hook points:

- `WubeiTask.triggerCombatTrackerPathing(...)`: after local selection
  `panel.getGreenLinks().get(0)`, before the existing click retry loop; phase
  `wubei-combat-tracker-pathing`.
- `WubeiTask.startProbeTrackerPathing(...)`: after local selection
  `currentProbeSegments.get(nextIndex)`, before `clickTaskTrackerGreen(...)`; phase
  `wubei-probe-tracker-pathing`.
- `WubeiTask` enter-battle retry: after local selection
  `currentTrackerPanel.getGreenLinks().get(0)`, before `clickTaskTrackerGreen(...)`; phase
  `wubei-enter-battle-retry`.
- `XiuluoTaskV2.tryTrackerShortcutWithPanel(...)`: after resolving the local click point and before
  `inputSequences.moveAndClickLeft(...)`; phase `xiuluo-tracker-shortcut`.

Local baseline:

- The local green-link choice is still the only executed decision.
- 五倍 keeps the existing selected index and candidate list for combat, probe, and enter-battle retry
  clicks.
- 修罗 keeps `resolveXiuluoTrackerGreenClickPoint(...)` and the existing tracker shortcut click point;
  the shadow report uses `panel.getGreenLinks()` with `selectedIndex=0`.
- Cloud output is not read, not branched on, and not used for click, pathing intent, retry, dialog,
  battle, bag, NPC, navigation, or task phase decisions.

Request fields:

- `serviceId=TRACKER_LINK_RANKER`
- `taskCode=wubei` or `taskCode=xiuluo_v2`
- `phase=<hook phase>`
- `traceId=tracker-link-ranker:<taskCode>:<source>`
- `localDecision=index=<selectedIndex>;click=<x>,<y>;rect=<minX>,<minY>,<maxX>,<maxY>` or `NO_LINK`
- context: `source`, `candidateCount`, `selectedIndex`, `selectedClick`, `selectedRect`,
  `selectedTargetMap`, `selectedTargetMapScore`, `candidates`

Shadow log:

```text
cloud.decision serviceId=TRACKER_LINK_RANKER mode=SHADOW taskCode=wubei|xiuluo_v2 phase=...
localDecision=... cloudDecision=... effectiveDecision=... agree=... executed=false
```

Fallback and rollback switch:

```properties
cloud.services.tracker-link-ranker.shadow-enabled=false
cloud.services.tracker-link-ranker.execute-enabled=false
cloud.services.tracker-link-ranker.execute-percent=0
cloud.services.tracker-link-ranker.fallback=LOCAL
```

Runtime verification:

```powershell
Select-String -Path logs/dhxy-console.log -Pattern "cloud.decision serviceId=TRACKER_LINK_RANKER"
```

Default acceptance:

- This branch intentionally enables `cloud.services.tracker-link-ranker.shadow-enabled=true` for live
  shadow verification.
- `execute-enabled=false`, `execute-percent=0`, and `fallback=LOCAL` must remain set, so
  `executed=false` and the local link remains the effective action.

## 11. CR-HC-004 Real Cloud Transport + 双服务真实 Shadow

Status:

```text
Review passed locally / fresh runtime with real external endpoint pending
```

Owner split:

- 业务实现：子智能体负责 `CloudDecisionClient` 真实 HTTP 传输层、配置、测试和 shadow 接入。
- Review / 验收：主 agent 负责检查是否保持 shadow-only、是否没有改业务决策、是否满足本卡验收。
- 谢帅当前只担任业务主管/reviewer：不亲自写 Java 业务实现代码；实现由 worker 子智能体完成，
  并额外安排 review/helper 子智能体辅助审查。谢帅可以直接维护本卡、验收清单、运行验证和记录审查结论。

Scope:

- 在现有 CR-HC-001 skeleton、CR-HC-002 `TASK_CLASSIFIER` shadow、CR-HC-003
  `TRACKER_LINK_RANKER` shadow 基础上，补真实远程调用传输层。
- 第一版只验证“真实发 request、真实收 response、真实记录延迟/失败/fallback”。
- 云端结果仍然不能控制任务行为。五倍/修罗的本地分类、绿链选择、点击点、pathing intent、
  retry、dialog、回城、NPC、导航、自动战斗都必须继续使用本地结果。

Services:

```text
TASK_CLASSIFIER
TRACKER_LINK_RANKER
```

Required behavior:

1. 默认安全：
   - 无 endpoint / token / enable 配置时，不允许启动失败；
   - 保持本地执行；
   - 可以继续 mock/local shadow，或明确记录 real transport disabled。
2. 真实 shadow：
   - 配置开启后，两个 service 都要能通过真实 HTTP client 发出云端 request；
   - 云端 response 只进入 shadow 对照日志；
   - `executed=false` 仍必须成立；
   - `effectiveDecision` 仍必须等于 local decision。
3. 失败兜底：
   - timeout、HTTP error、JSON parse error、空 response、schema mismatch 都必须 fallback 到
     `LOCAL`；
   - 失败不能改变任务 phase，也不能阻塞五倍/修罗主流程。
4. 日志要求：
   - 每次 shadow 必须能看出 `serviceId`、`taskCode`、`phase`、`traceId`、`mode`、
     `elapsedMs`、`localDecision`、`cloudDecision`、`effectiveDecision`、`agree`、
     `executed=false`、`fallback`、`error/timeout reason`。
   - 真实 HTTP shadow 不能再全部是 `elapsedMs=0`；如果 transport disabled，则日志必须明确
     disabled/fallback 原因。
5. 配置要求：
   - 继续使用 `cloud.services.<service-id>.*` 服务级开关；
   - `execute-enabled=false` 和 `execute-percent=0` 保持默认；
   - 第一版允许新增 endpoint、token/header、timeout 等配置，但 token 不得写死在源码或提交到
     repo 明文文件。

Non-goals:

- 不实现云端执行灰度；
- 不让 cloud response 决定真实点击；
- 不搬模板、坐标转换资产或 navigation policy；
- 不改 OCR/template/click/navigation 阈值；
- 不改五倍/修罗任务 phase 语义；
- 不改 input queue / Runner / ready event 业务行为。

Tests:

必须至少覆盖：

- HTTP client success：能解析云端 response，记录非零耗时；
- timeout/failure：fallback local，不抛出导致任务中断；
- malformed/empty response：fallback local；
- `TASK_CLASSIFIER` real-shadow 接入后，返回的业务 tracker result 仍是本地 result；
- `TRACKER_LINK_RANKER` real-shadow 接入后，真实点击仍使用本地 link/click point；
- 默认配置下云端传输不启用，现有 skeleton/shadow 行为不破坏。

Runtime verification:

```powershell
Select-String -Path logs/dhxy-console.log -Pattern "cloud.decision serviceId=TASK_CLASSIFIER"
Select-String -Path logs/dhxy-console.log -Pattern "cloud.decision serviceId=TRACKER_LINK_RANKER"
```

Acceptance:

- 本地编译和 focused tests 通过；
- 开启真实 shadow endpoint 后，五倍和修罗都有真实 HTTP shadow 日志；
- `agree=false` 样本需要被记录，但不得影响任务；
- `executed=true` 必须仍为 0；
- timeout/error 时任务继续跑，本地结果继续执行；
- 主 agent review 确认没有把 cloud result 接入任何真实业务决策。

Implementation note 2026-06-30:

- Worker implementation added `HttpCloudDecisionClient` as the primary `CloudDecisionClient`.
- Real HTTP is gated by:

```properties
cloud.real-transport-enabled=false
cloud.base-url=http://127.0.0.1:18080
cloud.endpoint-path=/api/cloud/decision
cloud.token=
```

- With real transport disabled, missing endpoint, or missing token, the client does not send HTTP and
  `CloudDecisionCoordinator` logs `success=false`, `fallback=LOCAL`, and a transport-disabled reason.
- With real transport enabled and token configured, the client POSTs the cloud request, parses the
  cloud response, and the coordinator still returns `effectiveDecision=localDecision` and
  `executed=false`.
- Coordinator validation treats wrong `serviceId`, wrong `traceId`, or missing `decision` as
  `schema mismatch` and falls back to `LOCAL`.
- Timeout, HTTP non-2xx, JSON parse failure, empty response, missing token, and disabled transport all
  keep the local decision and never change task phase/click/navigation behavior.
- `cloud.decision` logs now include `success={}` in addition to `serviceId`, `taskCode`, `phase`,
  `traceId`, `elapsedMs`, `localDecision`, `cloudDecision`, `effectiveDecision`, `agree`,
  `executed`, `fallback`, and `reason`.
- Focused verification used by worker:

```powershell
mvn -q -DskipTests test-compile
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.decision.CloudHttpDecisionClientTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.task.CloudRealShadowServicesIntegrationTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

Review repairs 2026-06-30:

- P1: `CloudHttpDecisionClientTest` main guard had an unstable empty-response simulation; fixed the
  test helper so empty response logs `reason=empty response`, while the separate timeout case still
  logs `timeout after 50ms`.
- P1: 五倍 `TRACKER_LINK_RANKER` hook was initially missing. `WubeiTask` now reports shadow-only
  link-ranker samples after local link selection and before real click in:
  - `wubei-probe-tracker-pathing`;
  - `wubei-combat-tracker-pathing`;
  - `wubei-enter-battle-retry`.
- P1: `CloudRealShadowServicesIntegrationTest` main guard could time out at `500ms` and then read a
  null response. The integration guard now uses a test-only `2000ms` timeout, checks cloud availability
  and response presence before reading the response, and keeps the dedicated timeout/fallback coverage
  in `CloudHttpDecisionClientTest`.
- P2 scope note: `bot.dhxy.summon-skill-ultimate-generate-cooldown-ms=10800000` in
  `application.properties` is unrelated pre-existing dirty work and is not owned by CR-HC-004. This
  card owns only the `cloud.*` config additions in that file.

Final local review 2026-06-30:

- Independent review helper found no remaining P0/P1/P2 after the repairs.
- Main-agent review verified `CloudDecisionResult`, `getEffectiveDecision`, `effectiveDecision`, and
  `cloudDecision` are not read by `service/task/window/input` business paths.
- 五倍 and 修罗 link-ranker hooks are fire-and-forget `shadowTrackerLinkSelection(...)` calls; local
  selected link/click point, retry loop, pathing intent, and phase continue unchanged.
- Fresh verification run by main agent:

```powershell
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.decision.CloudHttpDecisionClientTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.task.CloudRealShadowServicesIntegrationTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

- The integration main produced 五倍 `TASK_CLASSIFIER`, 五倍 `TRACKER_LINK_RANKER`, and 修罗
  `TRACKER_LINK_RANKER` real-shadow samples with `success=true`, non-zero `elapsedMs`,
  `effectiveDecision=<local decision>`, and `executed=false`.

Fresh-runtime acceptance remains pending:

- Configure a real test endpoint/token and run 五倍 + 修罗 shadow samples.
- Confirm logs contain both `TASK_CLASSIFIER` and `TRACKER_LINK_RANKER` with non-zero HTTP
  `elapsedMs`, `success=true` for reachable endpoint samples, and `executed=false`.
- Confirm timeout/error endpoint samples log `success=false`, `fallback=LOCAL`, and the local tracker
  classification / local green-link click still executes.

## 12. CR-HC-005 Local Dev Cloud Decision Endpoint

Status:

```text
Source implemented / local verification passed / runtime use available
```

Goal:

- Provide a real local HTTP endpoint for fast live testing of `HttpCloudDecisionClient`.
- This endpoint is a dev/test bridge, not the final commercial cloud deployment.
- It lets the app exercise real request/response latency without waiting for external infrastructure.

Owner split:

- 业务实现：endpoint worker owns the standalone dev server, tests, script, and docs.
- Review / 验收：谢帅/main agent owns review only; no Java business implementation by main agent.

Scope:

- Add a test-scope standalone server for:

```text
POST /api/cloud/decision
Authorization: Bearer <token>
```

- Response contract stays aligned with `CloudDecisionResponse`:

```json
{
  "serviceId": "TASK_CLASSIFIER",
  "traceId": "trace-1",
  "policyVersion": "dev-local-v1",
  "decision": "local-A",
  "confidence": 1.0,
  "ttlMs": 1000,
  "diagnostics": {
    "server": "dev-local"
  }
}
```

Default behavior:

- Echo request `localDecision` as `decision`.
- Support an optional forced decision for execute-framework testing.
- Reject bad token with HTTP `401`.
- Reject malformed JSON with HTTP `400`.
- Never require a production secret in repo files.

Non-goals:

- No Cloudflare/Worker deployment yet.
- No template/image upload.
- No auth/licensing hardening yet.
- No business task code changes.

Verification:

```powershell
mvn -q -DskipTests test-compile
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServerTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

Runtime command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-cloud-decision-dev-server.ps1 -Port 18080
```

Client test config:

```properties
cloud.real-transport-enabled=true
cloud.base-url=http://127.0.0.1:18080
cloud.endpoint-path=/api/cloud/decision
cloud.token=local-dev-token
cloud.services.task-classifier.shadow-enabled=true
cloud.services.task-classifier.execute-enabled=false
cloud.services.tracker-link-ranker.shadow-enabled=true
cloud.services.tracker-link-ranker.execute-enabled=false
```

Implementation note 2026-06-30:

- CR-HC-005 worker added a test-scope standalone local dev endpoint:
  `src/test/java/com/bot/dhxy/cloud/dev/CloudDecisionDevServer.java`.
- The endpoint uses Java `com.sun.net.httpserver.HttpServer` and serves exact path
  `POST /api/cloud/decision`.
- Auth requires `Authorization: Bearer <token>`; default local token is `local-dev-token`.
- Default response echoes request `localDecision` into response `decision`.
- `forcedDecision` is supported through `start(...)`, `startForTest(...)`, and main args
  `--forced-decision`, so execute-framework tests can force a cloud task key.
- Bad token returns `401`, malformed JSON returns `400`, wrong path returns `404`, and non-POST
  returns `405`.
- Response JSON includes `serviceId`, `traceId`, `policyVersion=dev-local-v1`, `decision`,
  `confidence=1.0`, `ttlMs=1000`, and `diagnostics.server=dev-local`.
- Added main-guard test:
  `src/test/java/com/bot/dhxy/cloud/dev/CloudDecisionDevServerTest.java`.
- Added local startup script:
  `scripts/run-cloud-decision-dev-server.ps1`, defaulting to port `18080`, path
  `/api/cloud/decision`, and token `local-dev-token`.

Verification note 2026-06-30:

- Worker RED before implementation: `mvn -q -DskipTests test-compile` failed because
  `CloudDecisionDevServer` did not exist.
- Worker observed both required commands pass once:
  - `mvn -q -DskipTests test-compile`
  - `mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServerTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java`
- Main-agent final verification reran the dev-server main guard and passed:
  `CloudDecisionDevServerTest passed`.

## 13. CR-HC-006 TaskClassifier Execute Framework

Status:

```text
Source implemented / independent review passed / fresh runtime execute gate pending
```

Goal:

- Make `execute-enabled` and `execute-percent` real for `TASK_CLASSIFIER`.
- Keep `TRACKER_LINK_RANKER` shadow-only in this CR.
- Move faster than pure shadow while still limiting the first executable cloud decision to a narrow enum-like task key.

Owner split:

- 业务实现：execute-framework worker owns coordinator/task-classifier/task-tracker-service wiring and tests.
- Review / 验收：谢帅/main agent reviews only and must verify cloud result cannot affect clicks/navigation.

Allowed execution:

- Only `TASK_CLASSIFIER`.
- Only when:
  - service `execute-enabled=true`;
  - deterministic `execute-percent` gate hits;
  - cloud response is available and schema-valid;
  - cloud `decision` maps to a known local task key/title template.

Safe application rules:

- 五倍: cloud decision may replace only the local `TaskTrackerTitleTemplate` when the local result is
  already `found=true`.
- 修罗: cloud decision may only keep/confirm the supported `xiuluo.tracker` task key.
- The code must not invent green links, click points, OCR crops, coordinates, or found results.
- Unknown cloud task keys must log and return the original local result.
- Cloud failure, timeout, malformed response, empty response, schema mismatch, or percent miss must
  return the original local result.

Explicitly forbidden:

- `TRACKER_LINK_RANKER` execution.
- Cloud-controlled click coordinate.
- Cloud-controlled pathing intent.
- Cloud-controlled retry/phase/dialog/bag/NPC/navigation/battle.
- Any OCR/template threshold or image matching change.

Rollout config:

```properties
cloud.real-transport-enabled=true
cloud.services.task-classifier.shadow-enabled=true
cloud.services.task-classifier.execute-enabled=true
cloud.services.task-classifier.execute-percent=5
cloud.services.task-classifier.fallback=LOCAL
cloud.services.tracker-link-ranker.shadow-enabled=true
cloud.services.tracker-link-ranker.execute-enabled=false
cloud.services.tracker-link-ranker.execute-percent=0
cloud.services.tracker-link-ranker.fallback=LOCAL
```

Verification:

```powershell
mvn -q -Dtest="CloudDecisionCoordinatorTest,TaskClassifierCloudShadowServiceTest,TaskClassifierCloudExecuteWiringTest,TrackerLinkRankerCloudShadowWiringTest" test
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
```

Runtime acceptance:

- With local dev endpoint returning the same task key, live logs show `executed=true` only for the
  percent-hit `TASK_CLASSIFIER` samples.
- `TRACKER_LINK_RANKER` remains `executed=false`.
- 五倍/修罗 task behavior continues normally.
- If the endpoint is stopped, logs show `success=false fallback=LOCAL`, and tasks continue locally.

Implementation note 2026-06-30:

- `CloudDecisionCoordinator` now honors execute mode only when a schema-valid cloud response passes
  the deterministic `traceId|serviceId|taskCode|phase` execute-percent gate.
- All failure, timeout, empty/malformed response, schema mismatch, and percent-miss paths keep the
  local decision with `executed=false`.
- `TaskClassifierCloudShadowService` now returns `CloudDecisionResult` while preserving old
  ignored-return call compatibility.
- `TaskTrackerPanelService` consumes only `TASK_CLASSIFIER` executed results, and only replaces
  `titleTemplate` on an already-found local tracker result. It preserves found state, detail raw/yellow
  paths, OCR text, green links, band width, probe flag, and coordinates.
- 五倍 cloud key mapping is limited to existing local 五倍 title templates/task keys. 修罗 mapping is
  limited to `xiuluo.tracker`. Unknown cloud keys log reject and return the local result.
- No OCR/template threshold, click coordinate, navigation point, pathing intent, retry, phase, dialog,
  bag, NPC, battle, Runner, or input-queue behavior was changed.

Review repair 2026-06-30:

- Independent review found a P2: coordinator-level execute initially applied to any service, so a
  misconfigured `TRACKER_LINK_RANKER execute-enabled=true` could log `executed=true` even though tasks
  did not consume the result.
- Repair added an execute allowlist: only `TASK_CLASSIFIER` may produce `executed=true` /
  cloud `effectiveDecision`.
- `TRACKER_LINK_RANKER` remains coordinator-level shadow-only even if misconfigured with
  `execute-enabled=true` and `execute-percent=100`; the result keeps the local decision and logs
  `execute not allowed for service; service not executable; keeping local decision`.
- `CloudDecisionCoordinatorTest` now guards both sides:
  - `TASK_CLASSIFIER execute-percent=100` can execute.
  - `TRACKER_LINK_RANKER execute-percent=100` still keeps local and `executed=false`.

Final local review 2026-06-30:

- Independent review passed after the allowlist repair; no remaining P0/P1/P2 were reported.
- Main-agent grep review found `CloudDecisionResult` / `effectiveDecision` consumption only in
  `TaskTrackerPanelService` for `TASK_CLASSIFIER`; task/window/input paths do not consume cloud
  effective decisions.
- `TRACKER_LINK_RANKER` remains fire-and-forget shadow-only for 五倍/修罗 click candidates.

Final verification 2026-06-30:

```powershell
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServerTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.decision.CloudHttpDecisionClientTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.task.CloudRealShadowServicesIntegrationTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

Key verification evidence:

- Cloud package tests showed `TASK_CLASSIFIER` can produce `executed=true` when the execute gate hits.
- The same run showed misconfigured `TRACKER_LINK_RANKER` keeps
  `effectiveDecision=<local decision>` and `executed=false`.
- Real-shadow integration still produced 五倍 `TASK_CLASSIFIER`, 五倍 `TRACKER_LINK_RANKER`, and 修罗
  `TRACKER_LINK_RANKER` samples with `success=true`, non-zero `elapsedMs`, and local effective decisions.

Runtime config prepared 2026-06-30:

- `application.properties` was set to local dev endpoint execute-test mode:
  - `cloud.real-transport-enabled=true`
  - `cloud.token=local-dev-token`
  - `cloud.services.task-classifier.execute-enabled=true`
  - `cloud.services.task-classifier.execute-percent=100`
  - `cloud.services.tracker-link-ranker.execute-enabled=false`
- A local dev endpoint was started on `127.0.0.1:18080`; a manual POST probe returned the echoed
  `TASK_CLASSIFIER` decision.

Fresh runtime gate:

- Start the local dev endpoint or a real endpoint, enable controlled `TASK_CLASSIFIER` execute
  percentage, and run 五倍/修罗.
- Verify live logs show `executed=true` only for percent-hit `TASK_CLASSIFIER` samples.
- Verify `TRACKER_LINK_RANKER` remains `executed=false`, and task behavior continues normally.

Fresh runtime evidence 2026-06-30:

- Local dev endpoint was running on `127.0.0.1:18080`.
- A live 修罗 run starting at `2026-06-30 21:01:32.637` reached at least round 11.
- Runtime logs showed `TASK_CLASSIFIER` samples with `mode=EXECUTE`, `executed=true`,
  `success=true`, `effectiveDecision=xiuluo.tracker`, local/cloud agreement, and HTTP elapsed times
  around `4-15ms`.
- Runtime logs showed `TRACKER_LINK_RANKER` samples with `mode=SHADOW`, `executed=false`,
  `success=true`, local/cloud agreement, and HTTP elapsed times around `3-7ms`.
- No `Exception` / `ERROR` was observed in that latest 修罗 segment.

## 14. CR-HC-007 TrackerLinkRanker Enhanced Shadow Runtime Evaluation

Status:

```text
Source implemented / local verification passed / helper/main review passed / fresh runtime pending
```

Goal:

- Keep 五倍/修罗 using the local tracker/link decision exactly as before.
- Send the same local tracker/link candidate data to the cloud at runtime.
- Use the cloud response only for validation: returned decision, latency, success/failure, and
  local/cloud agreement.
- Do not execute `TRACKER_LINK_RANKER` cloud decisions in this card.

Owner split:

- 业务实现：worker owns any logging/metrics-only code changes and focused tests.
- Review / 验收：谢帅/main agent reviews only, plus a helper reviewer if needed.

Shadow-only rule:

- `TRACKER_LINK_RANKER` must remain `executed=false`.
- `effectiveDecision` must remain the local decision.
- 五倍/修罗 must not read cloud link-ranker result to choose a link.
- If runtime config accidentally sets `execute-enabled=true`, the coordinator must still keep
  `TRACKER_LINK_RANKER` shadow-only until a later explicit execute card reopens it.

Shadow request shape:

- The local decision should continue to be candidate-index oriented, for example:

```text
index=0
index=0;click=310,258;rect=293,252,328,264
```

Runtime evidence expected:

- For each sample, logs should identify:
  - `serviceId=TRACKER_LINK_RANKER`;
  - `taskCode`;
  - `phase`;
  - `traceId`;
  - `candidateCount`;
  - local selected index/click/rect;
  - cloud decision;
  - agreement;
  - `elapsedMs`;
  - `success`;
  - `executed=false`.
- 五倍 must keep reporting the existing three hook phases:
  - `wubei-probe-tracker-pathing`;
  - `wubei-combat-tracker-pathing`;
  - `wubei-enter-battle-retry`.
- 修罗 must keep reporting:
  - `xiuluo-tracker-shortcut`.

Explicitly forbidden:

- Cloud-controlled link selection.
- Cloud-controlled raw click coordinate.
- Cloud-controlled newly invented candidate.
- Cloud-controlled pathing intent or navigation target.
- Cloud-controlled retry count, task phase, dialog, bag, NPC, battle, or input queue behavior.
- OCR/template threshold changes.
- Minimap/world-map click changes.

Rollout config:

```properties
cloud.real-transport-enabled=true
cloud.services.task-classifier.shadow-enabled=true
cloud.services.task-classifier.execute-enabled=true
cloud.services.task-classifier.execute-percent=100
cloud.services.task-classifier.fallback=LOCAL
cloud.services.tracker-link-ranker.shadow-enabled=true
cloud.services.tracker-link-ranker.execute-enabled=false
cloud.services.tracker-link-ranker.execute-percent=0
cloud.services.tracker-link-ranker.fallback=LOCAL
```

Verification:

```powershell
mvn -q -Dtest="CloudDecisionCoordinatorTest,TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudRealShadowServicesIntegrationTest" test
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
```

Runtime acceptance:

- With local dev endpoint echoing the same local decision, live logs show
  `TRACKER_LINK_RANKER mode=SHADOW executed=false success=true` and local/cloud agreement.
- Endpoint latency should be visible in `elapsedMs`.
- If the endpoint is stopped or returns invalid data, logs show `success=false` / fallback diagnostics
  while local green-link clicks continue unchanged.
- This card is complete when logs are good enough to answer: what did cloud return, how slow was it,
  did it agree with local, and did local behavior stay untouched.

Implementation / verification note 2026-07-01:

- Worker kept `TRACKER_LINK_RANKER` shadow-only and added only a unified-log context enhancement.
- `CloudDecisionCoordinator` logs now include `context={...}`. For link ranker samples this includes
  `candidateCount`, `selectedIndex`, `selectedClick`, `selectedRect`, target-map hints, and the
  serialized candidate list.
- Main review verified no `CloudDecisionResult`, `getEffectiveDecision`, `isExecuted`, or cloud
  decision consumption appears in 五倍/修罗 task, runner, or input paths.
- Verification passed:

```powershell
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
mvn -q -Dtest="CloudDecisionCoordinatorTest,TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudRealShadowServicesIntegrationTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
```

- Fresh runtime still needs a restarted app build to show live `context={...}` in
  `logs/dhxy-console.log`; the already-running process will still use the older log format.

## 15. CR-HC-008 CloudDecision Runtime Metrics Summary

Status:

```text
Fresh-runtime wiring repair implemented / local verification passed / fresh runtime passed
```

Goal:

- Turn raw `cloud.decision` samples into local runtime metrics so we can see success rate, agreement
  rate, executed count, fallback count, and latency percentiles without manual grep.
- This is diagnostics-only and must not change any task behavior.

Owner split:

- 业务实现：worker owns the metrics implementation and focused tests.
- Review / 验收：谢帅/main agent reviews and verifies no task behavior changed.

Scope:

- Add an in-memory cloud decision metrics collector for `CloudDecisionResult`.
- Group at minimum by `serviceId`, `mode`, `taskCode`, and `phase`.
- Track:
  - total samples;
  - cloud success/failure;
  - agreement/disagreement;
  - executed count;
  - fallback/local count;
  - elapsed `p50/p95/p99` or a bounded equivalent;
  - last failure reason.
- Emit concise periodic summary logs with a stable prefix such as:

```text
cloud.metrics serviceId=... taskCode=... phase=... total=... successRate=... agreeRate=... p95Ms=...
```

Non-goals:

- No cloud execution changes.
- No changes to 五倍/修罗 click, navigation, retry, phase, dialog, bag, NPC, battle, Runner, or input
  behavior.
- No external metrics backend yet.
- No UI dashboard unless later requested.

Verification:

```powershell
mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest" test
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
```

Runtime acceptance:

- During live 五倍/修罗, logs include both raw `cloud.decision` and periodic `cloud.metrics` summaries.
- Metrics summaries must be non-blocking and must not throw into task execution.
- `TRACKER_LINK_RANKER` remains `executed=false`; `TASK_CLASSIFIER` execute behavior remains unchanged.

Precondition note 2026-07-01:

- CR-HC-008 starts after CR-HC-007's enhanced shadow/runtime evaluation work.
- `TRACKER_LINK_RANKER` remains outside the coordinator execute allowlist. Even if runtime config
  accidentally sets `execute-enabled=true` and `execute-percent=100`, the coordinator keeps
  `effectiveDecision=<local decision>` and `executed=false`.
- `CloudDecisionCoordinator` now writes request `context={...}` in the unified `cloud.decision` log.
  For tracker-link-ranker samples this includes `candidateCount`, `selectedIndex`, `selectedClick`,
  `selectedRect`, target-map hints, and the serialized local candidate list.
- 五倍/修罗 task code still uses fire-and-forget `shadowTrackerLinkSelection(...)` calls only; no
  task imports or reads `CloudDecisionResult`, `effectiveDecision`, or cloud link-ranker output.
- No click point, pathing intent, phase, retry, navigation, dialog, bag, NPC, battle, OCR/template, or
  input-queue behavior was changed.
- At this precondition point CR-HC-008 had not yet been implemented; the worker evidence below records
  the implementation pass.

Worker implementation / verification note 2026-07-01:

- Added `CloudDecisionMetricsService`, a local in-memory metrics collector for `CloudDecisionResult`
  samples.
  - Group key: `serviceId + mode + taskCode + phase`.
  - Counts: total, cloud success/failure, agreement/disagreement, executed, fallback/local.
  - Latency: bounded recent elapsed window per key with `p50Ms`, `p95Ms`, and `p99Ms`.
  - Failure diagnostics: last cloud-unavailable reason per key.
  - Log cadence: first few samples per key, then every configured interval, with stable prefix
    `cloud.metrics`.
- Updated `CloudDecisionCoordinator` so each `logDecision(result)` path records metrics immediately
  after the raw `cloud.decision` log. The metrics call is wrapped in `try/catch`; metrics failures
  only emit warn/debug logs and do not change the returned `CloudDecisionResult`.
- Added `CloudDecisionMetricsServiceTest` and extended `CloudDecisionCoordinatorTest` for:
  - grouping/count/rate/percentile behavior;
  - bounded elapsed sample window;
  - stable `cloud.metrics` log-line format and throttling;
  - coordinator metrics recording after decision result creation;
  - metrics exception swallowing.
- RED/GREEN:
  - RED: `mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest" test`
    failed before production implementation because `CloudDecisionMetricsService` was missing.
  - GREEN: the same focused command passed after adding the collector and coordinator metrics call.
- Verification passed:

```powershell
mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest" test
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
```

- Focused cloud-suite evidence still shows `TASK_CLASSIFIER mode=EXECUTE ... executed=true` when the
  execute gate is allowed, and misconfigured `TRACKER_LINK_RANKER mode=EXECUTE ... executed=false`
  with local `effectiveDecision`.
- No 五倍/修罗 click, navigation, retry, phase, dialog, bag, NPC, battle, Runner, OCR/template, or
  input-queue behavior was changed. Fresh runtime still needs restarted-app logs to confirm live
  `cloud.metrics` summaries during 五倍/修罗.

Main review note 2026-07-01:

- Independent helper review found no P0/P1/P2 blocker.
- Main verification reran the focused metrics/coordinator tests, compile, test-compile, and full
  cloud test suite listed above; all passed.
- Fresh runtime is still pending because the already-running DHXY Java app loaded the previous
  classes. The next runtime proof requires restarting the app and then checking
  `logs/dhxy-console.log` for both `cloud.decision` and `cloud.metrics`.

Fresh-runtime wiring repair 2026-07-01:

- Runtime blocker evidence from the user/runtime logs: after the 2026-06-30 22:15 restart, live
  `logs/dhxy-console.log` at 22:31 still showed `cloud.decision` for `TASK_CLASSIFIER` and
  `TRACKER_LINK_RANKER`, but no `cloud.metrics`.
- Root cause shape: `CloudDecisionCoordinator` allowed a production instance with
  `metricsService == null` through optional `@Autowired(required = false)` setter injection, and
  `recordMetrics(...)` silently returned when metrics was null. That exactly matched the symptom:
  raw decision logs continued, metrics summaries never emitted.
- Repair:
  - `CloudDecisionCoordinator` now requires `CloudDecisionMetricsService` in its constructor and stores
    it as a `final` dependency.
  - The optional metrics setter and null-skip branch were removed, so Spring must wire metrics for the
    coordinator bean instead of allowing a silent no-metrics runtime path.
  - Metrics recording exceptions are still caught and logged as `cloud.metrics record failed`; the
    original `CloudDecisionResult` remains unchanged.
  - `CloudDecisionSkeletonWiringTest` now registers/gets `CloudDecisionMetricsService`, and
    `CloudDecisionCoordinatorTest` guards against reintroducing optional setter/null-skip wiring.
- RED/GREEN:
  - RED: `mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest" test` failed on
    `testCoordinatorRequiresMetricsConstructorDependency` before the production wiring repair.
  - GREEN: the same focused command passed after constructor injection.
- Verification passed:

```powershell
mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest" test
mvn -q -Dtest="CloudDecisionSkeletonWiringTest" test
mvn -q -DskipTests compile
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
```

- Cloud-suite verification still showed `TASK_CLASSIFIER mode=EXECUTE ... executed=true` for the
  allowed execute path and `TRACKER_LINK_RANKER ... executed=false` with local `effectiveDecision`.
- Fresh runtime passed after the user restarted the DHXY Java app:
  - `2026-06-30 23:32:34.641` 修罗 `TASK_CLASSIFIER` logged both `cloud.decision` and
    `cloud.metrics`, with `mode=EXECUTE`, `executed=true`, `success=true`, and agreement.
  - `2026-06-30 23:32:36.484/486` 修罗 `TRACKER_LINK_RANKER` logged both `cloud.decision` and
    `cloud.metrics`, with `mode=SHADOW`, `executed=false`, `success=true`, and agreement.
  - `2026-06-30 23:39:23.488/490` 五倍 `TASK_CLASSIFIER` logged both `cloud.decision` and
    `cloud.metrics`, with `mode=EXECUTE`, `executed=true`, `success=true`, and agreement.
  - `2026-06-30 23:39:27.185` 五倍 `TRACKER_LINK_RANKER` logged both `cloud.decision` and
    `cloud.metrics`, with `mode=SHADOW`, `executed=false`, `success=true`, and agreement.
  - One expected controlled runtime sample at `2026-06-30 23:42:44.025` showed
    `TASK_CLASSIFIER ... success=false reason=timeout after 300ms`, and the paired
    `cloud.metrics` summary recorded the failure while the effective decision remained local.
  - No live `cloud.metrics record failed` line was found in the checked segment.

## 16. CR-HC-009 TrackerLinkRanker Candidate-Index Execute Gate

```text
Source implemented / local verification passed / helper review passed / fresh runtime passed
```

Goal:

- Add the first safe execute skeleton for `TRACKER_LINK_RANKER` without letting cloud return raw click
  coordinates or directly control input.
- Cloud may only choose among the local candidate list by returning an existing candidate index.
- Local code must still own the actual click point, candidate bounds, pathing intent, retry, phase,
  dialog, Runner, and input queue.

Safety boundary:

- Default runtime config must keep `TRACKER_LINK_RANKER` non-executing: `execute-enabled=false` or
  `execute-percent=0`.
- Even when execute is enabled in tests, cloud output is accepted only if:
  - the response schema is valid and policy/version gates pass;
  - the returned candidate index exists in the local candidate list for the same request;
  - the request fingerprint/session/trace still matches the current local selection context;
  - pause/stop and existing local safety checks still pass.
- Cloud must never return a naked screen coordinate for this CR.
- If cloud returns an invalid index, timeout, schema mismatch, or disagreement that cannot be mapped to
  an existing candidate, fallback is local selection and `executed=false`.

Non-goals:

- Do not change actual 五倍/修罗 click behavior by default.
- Do not make cloud control minimap navigation, route transfer, NPC click, dialog choice, bag, battle,
  return item, team maintenance, or task phase.
- Do not remove existing shadow metrics.

Expected implementation ownership:

- 业务实现：worker sub-agent owns the code and focused tests.
- Review：helper sub-agent plus main agent review; main agent remains manager/reviewer and does not write
  Java business implementation.

Likely touch areas:

- `CloudDecisionCoordinator` / allowlist or service-level execute policy.
- `TrackerLinkRankerCloudShadowService` request/response mapping.
- Focused cloud/task wiring tests proving default no-behavior-change and invalid-index fallback.

Verification:

```powershell
mvn -q -Dtest="TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudDecisionCoordinatorTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Runtime acceptance:

- With default config, live 五倍/修罗 still show `TRACKER_LINK_RANKER mode=SHADOW executed=false`.
- Optional later runtime gate: with an explicit test-only execute config and dev endpoint returning a
  valid existing candidate index, logs may show `executed=true`, but the effective click must be the
  local candidate's already-computed click point.
- Invalid index/timeout must log fallback and keep local behavior.

Review blocker recorded 2026-07-01:

- Do not add `TRACKER_LINK_RANKER` to the generic coordinator execute allowlist and let the common
  coordinator mark `executed=true` before ranker-specific validation.
- A valid execute sample must prove both:
  - cloud returned only a candidate index, not executable coordinates;
  - the cloud response is bound to the current local candidate list through a fingerprint/session or
    equivalent request-context guard.
- Missing/mismatched fingerprint, invalid index, coordinate-bearing decision, timeout, or schema
  mismatch must keep local selection and `executed=false`.

Worker result 2026-07-01:

- Implemented the safe candidate-index execute skeleton.
  - `CloudDecisionCoordinator` now supports a service-specific `CloudDecisionExecutionGate`.
  - The default coordinator execute allowlist remains `TASK_CLASSIFIER`; `TRACKER_LINK_RANKER` can
    execute only through `TrackerLinkRankerCloudShadowService`'s candidate-index gate.
  - `TrackerLinkRankerCloudShadowService` returns `TrackerLinkRankerCloudDecision`, whose effective
    link is always a local `TaskTrackerGreenLink` candidate.
- Safety behavior:
  - Accepted cloud response format is strict `index=N`.
  - The index must exist in the same local candidate list.
  - `index=1;click=...`, out-of-range index, timeout, and schema/trace mismatch all fallback to the
    local candidate with `executed=false`.
  - Existing 五倍/修罗 callers still ignore the return value, so default click/pathing behavior remains
    unchanged.
- Verification:
  - RED: `mvn -q -Dtest="TrackerLinkRankerCloudShadowServiceTest" test` failed before implementation
    on missing `TrackerLinkRankerCloudDecision`.
  - `mvn -q -Dtest="TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudDecisionCoordinatorTest" test`
    passed.
  - `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test` passed after updating the integration test
    recording coordinator to capture the new gate-aware overload.
  - `mvn -q -DskipTests compile` passed.
  - No 五倍/修罗 business click, navigation, OCR/template, dialog, bag, battle, return-item, team
    maintenance, Runner, or input queue logic was edited.

Worker repair 2026-07-01 / fingerprint execute gate:

- Wegener review found P1: the first pass did not enforce the documented candidate
  fingerprint/session/policyVersion gate; it only checked `traceId`, strict candidate index, and the
  current local candidate list.
- Repair:
  - Request context now includes `candidateFingerprint`, a SHA-256 hex fingerprint of local
    `candidateCount`, candidate sequence, `selectedIndex`, and selected local decision.
  - `TRACKER_LINK_RANKER` execute now requires the cloud response to echo the same fingerprint in
    `diagnostics.candidateFingerprint`.
  - Missing or mismatched fingerprint rejects execute with local fallback and `executed=false`.
  - Valid test-only execute now needs both a strict `index=N` and a matching fingerprint.
- Tests:
  - RED focused ranker test failed before repair on missing request fingerprint and missing/mismatch
    execute acceptance.
  - Focused ranker GREEN showed valid matching fingerprint execute, missing fingerprint fallback,
    fingerprint mismatch fallback, invalid index fallback, coordinate-bearing response rejection,
    timeout fallback, and schema mismatch fallback.
- Scope held: no 五倍/修罗 task click/pathing/default behavior, OCR/template, dialog, bag, battle,
  return-item, team maintenance, Runner, or input queue logic changed.

Main verification / final review 2026-07-01:

- Main agent reran the required verification:

```powershell
mvn -q -Dtest="TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudDecisionCoordinatorTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- All three commands passed.
- Main grep verified 五倍/修罗 task code still treats `shadowTrackerLinkSelection(...)` as
  fire-and-forget diagnostics and still clicks the original local `segment` / `point`.
- Independent helper review found no P0/P1/P2 blocker. It confirmed:
  - the default coordinator execute allowlist still contains only `TASK_CLASSIFIER`;
  - `TRACKER_LINK_RANKER` execute is reachable only through the ranker service's candidate-index gate;
  - cloud coordinates are rejected rather than executable;
  - `diagnostics.candidateFingerprint` must match the request fingerprint;
  - default runtime config remains `execute-enabled=false` / `execute-percent=0`.

Fresh runtime acceptance:

- Restart the DHXY Java app before live acceptance.
- Default live 五倍/修罗 logs must continue to show
  `TRACKER_LINK_RANKER mode=SHADOW executed=false`.
- A later test-only execute run may show `executed=true` only when the dev endpoint returns strict
  `index=N` plus matching `diagnostics.candidateFingerprint`; click point must still be the mapped
  local candidate point.

Heartbeat runtime check 2026-07-01 00:20 local:

- No new live `cloud.decision` / `cloud.metrics` samples were found.
- The DHXY JavaFX app log stopped at `2026-07-01 00:11:44.928` after main-window close / stop-all.
- The local cloud dev endpoint remained active on `127.0.0.1:18080`; at that time the missing piece was
  a restarted DHXY app run that triggers 五倍/修罗 tracker-link-ranker hooks.

Fresh runtime acceptance 2026-07-01 11:31 local:

- User restarted/reran DHXY and produced fresh 修罗 + 五倍 runtime samples.
- `TRACKER_LINK_RANKER` live evidence matched the CR-HC-009 default-runtime gate:
  - total `14` samples after `10:44`;
  - all `success=true`;
  - all `agree=true`;
  - all `mode=SHADOW`;
  - all `executed=false`.
- Samples covered both 修罗 and 五倍:
  - 修罗: `xiuluo-tracker-shortcut` at `10:45:24.559` and `10:48:51.455`;
  - 五倍: `wubei-combat-tracker-pathing`, `wubei-probe-tracker-pathing`, and
    `wubei-combat-terminal-repath` through round 10.
- No `TRACKER_LINK_RANKER success=false`, `agree=false`, or timeout/failure lines were found in the
  checked segment.
- `TASK_CLASSIFIER` remained healthy in the same run with `15/15` success/agreement and
  `executed=true`, so CR-HC-009 did not regress the existing classifier execute path.
- Latency result for the `TRACKER_LINK_RANKER` cloud request/response path:
  - samples: `14`;
  - values: `4,5,6,12,17,12,3,6,2,8,6,3,3,7ms`;
  - average `6.71ms`, p50 `6ms`, p90 `12ms`, p95 `17ms`, p99 `17ms`, max `17ms`.
- Important gap: this runtime did not record a separate local link-ranker elapsed time. The current
  `cloud.decision elapsedMs` measures only the cloud client round trip / response handling inside
  `CloudDecisionCoordinator`; the local green-link candidate selection was already completed before
  the shadow request. Therefore this evidence proves cloud/local result agreement and cloud latency,
  but not a full local-vs-cloud latency comparison yet.

## 17. CR-HC-010 Local Cloud Brain Endpoint Sidecar Gate

```text
Source implemented / 2026-07-02 external brain default corrected / verification in progress
```

Goal:

- When the user clicks the UI task start button, automatically ensure the local external cloud brain
  endpoint is running.
- Remove the manual "remember to start `D:\mavenProject\dhxy-cloud-brain`" step during local
  五倍/修罗 cloud execute validation.

Trigger:

- Hook into the same UI task-start gates that already call `LocalOcrSidecarService`:
  - pending task queue start;
  - main start button task start;
  - selected-window context start.
- This is not a main-window-open startup hook. Opening the DHXY JavaFX app should not start the
  cloud dev endpoint until the user actually starts a task run.

Scope:

- The production/default auto sidecar launches the external real cloud brain project:

```powershell
scripts/run-cloud-brain-server.ps1 -Port 18080 -Path /api/cloud/decision -Token local-dev-token
```

- `scripts/run-cloud-decision-dev-server.ps1` is kept only as a compatibility wrapper that forwards
  to the external brain launcher.
- DHXY's test-scope `CloudDecisionDevServer` is isolated behind
  `scripts/run-dhxy-test-cloud-decision-stub.ps1 -AllowDhxyTestSidecar`; it is for unit tests or
  explicit debug only and must not be the production/default cloud endpoint.
- Health/readiness should be checked before task startup proceeds.
- If an endpoint is already listening and healthy, do not start another process.
- If this sidecar starts the process, track ownership and shut it down on app shutdown.
- If the endpoint was already running externally, do not kill it.
- Write sidecar logs to a stable file such as `logs/cloud-decision-dev-sidecar.log`.

Safety boundary:

- This CR is for local launch orchestration only. It must not turn DHXY's test-scope dev server into
  the production cloud service.
- It must not change 五倍/修罗 task phase, tracker classification, green-link ranking, click points,
  navigation, dialog, bag, battle, return item, team maintenance, Runner, OCR/template, or input queue
  behavior.
- It must not make cloud decisions more authoritative. Existing service configs still decide whether
  `TASK_CLASSIFIER` or `TRACKER_LINK_RANKER` run in shadow/execute mode.
- Startup failure must be explicit in logs and UI. With the current execute-required wave, task
  startup may continue into existing task control, but cloud-required services must fail closed /
  `STOP`; production paths must not reinterpret a missing cloud brain as `LOCAL` fallback.

Expected implementation ownership:

- 业务实现：worker sub-agent owns Java/source/test changes.
- Review：helper sub-agent plus main agent review; main agent remains manager/reviewer and does not
  write Java business implementation.

Likely touch areas:

- New UI/support service near `LocalOcrSidecarService`.
- `MainWindowController` task-start gate only.
- Cloud properties if a small explicit auto-start switch is needed.
- Focused tests for startup gating, already-running detection, and no duplicate process start.

Suggested config:

```properties
cloud.dev-sidecar.auto-start-enabled=true
cloud.dev-sidecar.script-path=scripts/run-cloud-brain-server.ps1
cloud.dev-sidecar.log-path=logs/cloud-decision-dev-sidecar.log
cloud.dev-sidecar.startup-timeout-ms=60000
```

Verification:

```powershell
javac -encoding UTF-8 -d target\test-classes src\test\java\com\bot\dhxy\ui\CloudBrainSidecarScriptGuardTest.java; java -cp target\test-classes com.bot.dhxy.ui.CloudBrainSidecarScriptGuardTest
mvn -q -Dtest="CloudDecisionDevSidecarServiceTest,MainWindowControllerCloudSidecarGateSourceGuardTest" test
mvn -q -DskipTests compile
```

Runtime acceptance:

- Ensure `127.0.0.1:18080` is stopped.
- Restart/open DHXY if needed, select 五倍/修罗 task queue, and click the UI start button.
- Logs should show the external `D:\mavenProject\dhxy-cloud-brain` sidecar starting before task
  control begins.
- After endpoint readiness, live logs should again show `cloud.decision` / `cloud.metrics` samples.
- Closing DHXY should stop only the endpoint process that this sidecar started.

Worker implementation result 2026-07-01:

- Added `CloudDecisionDevSidecarService` near the existing local OCR sidecar.
- Readiness is checked by a real authenticated POST to
  `cloud.base-url + cloud.endpoint-path`; this treats an externally/manual-started local dev
  endpoint as already available and does not take ownership of it.
- If readiness fails, the service starts the configured script, now defaulting to
  `scripts/run-cloud-brain-server.ps1`, with the configured port/path/token, redirects process
  output to `logs/cloud-decision-dev-sidecar.log`, and records sidecar lifecycle notes in the same
  log.
- Duplicate start protection is synchronized inside the service; concurrent/rapid UI starts wait on
  the same owned process instead of spawning multiple servers.
- Shutdown only destroys the process handle owned by this sidecar. A manually started endpoint is
  never killed by this service.
- UI startup gate behavior: OCR remains blocking as before; cloud endpoint failure logs WARN, writes
  an explicit UI log line, and leaves cloud-required services to fail closed / `STOP`.
- `MainWindowController` task-start gates now run cloud readiness before
  `windowTaskControlService.start(...)` for pending queue start, main start, selected-window context
  start, and the visible legacy "启动已选任务" button.
- Safety check: no 五倍/修罗 task phase, tracker classification, green-link ranking/click point,
  navigation, dialog, bag, battle, return item, team maintenance, Runner, OCR/template, or input
  queue behavior was changed by the sidecar launcher itself. Service execute/STOP authority is
  controlled by the current `cloud.services.*` config and service-specific gates.

Correction 2026-07-02:

- Production/default auto sidecar now uses `scripts/run-cloud-brain-server.ps1`, which starts the
  external `D:\mavenProject\dhxy-cloud-brain` shaded jar and accepts the same port/token arguments
  passed by `CloudDecisionDevSidecarService`.
- `scripts/run-cloud-decision-dev-server.ps1` is now only a compatibility wrapper to the external
  launcher. It no longer starts DHXY's `com.bot.dhxy.cloud.dev.CloudDecisionDevServer`.
- DHXY's internal test-sidecar is available only through
  `scripts/run-dhxy-test-cloud-decision-stub.ps1 -AllowDhxyTestSidecar`.
- UI failure wording now says cloud-required services remain fail-closed / `STOP`; it no longer
  describes missing cloud as local fallback.
- Verification:
  - RED guard first failed because `application.properties` still pointed at
    `scripts/run-cloud-decision-dev-server.ps1`.
  - `CloudBrainSidecarScriptGuardTest` passed after the script/config change.
  - `MainWindowControllerCloudSidecarGateSourceGuardTest` passed.
  - `CloudDecisionDevSidecarServiceTest` passed with the default script path resolved to
    `scripts/run-cloud-brain-server.ps1`.
  - DHXY `mvn -q -DskipTests compile` passed.
  - Full DHXY `mvn -q -DskipTests test-compile` is currently blocked by unrelated dirty tests that
    reference `TaskMaintenanceService` without resolving it; this sidecar pass did not touch those tests.
  - External `D:\mavenProject\dhxy-cloud-brain` `mvn -q test` passed on rerun, and
    `mvn -q -DskipTests package` passed.
  - Starting `scripts/run-cloud-brain-server.ps1` on port `18183` returned
    `TASK_CLASSIFIER / policyVersion=cloud-brain-local-v1` with diagnostics `server=dhxy-cloud-brain`.
  - `scripts/run-dhxy-test-cloud-decision-stub.ps1` without `-AllowDhxyTestSidecar` failed closed and
    did not start the DHXY test-sidecar.

Local verification 2026-07-01:

```powershell
mvn -q -Dtest="CloudDecisionDevSidecarServiceTest,MainWindowControllerCloudSidecarGateSourceGuardTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Result: all three commands exited 0. Focused tests covered already-running detection, duplicate
start prevention, owned-process-only shutdown, and UI start-gate source wiring.

Worker repair note 2026-07-01:

- Main review found a Windows shutdown gap: destroying only the owned `powershell` wrapper may leave
  Maven / Java dev-server children alive.
- `CloudDecisionDevSidecarService.shutdown()` now shuts down the owned process tree: collect
  `process.toHandle().descendants()`, destroy descendants first, destroy the root wrapper, then
  force any still-alive descendants/root after a short wait.
- The repair does not affect external/manual endpoints. If this sidecar did not start the endpoint,
  it still owns no process handle and kills nothing.
- Focused guard coverage now checks for `descendants()` / process-tree shutdown logic.

Main review 2026-07-01:

- Helper review risks were addressed:
  - readiness uses an authenticated POST to the configured decision endpoint, not a non-existent
    `/health`;
  - main code starts the script as an external process and does not import the test-scope dev server;
  - owned shutdown now targets the process tree rather than only the PowerShell wrapper;
  - UI source guard covers the task-start gates before `windowTaskControlService.start(...)`.
- Main verification passed:

```powershell
mvn -q -Dtest="CloudDecisionDevSidecarServiceTest,MainWindowControllerCloudSidecarGateSourceGuardTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- No 五倍/修罗 business flow, click, navigation, OCR/template, Runner, team maintenance, or cloud
  execute policy change was found in main review.
- Fresh runtime passed:
  - `2026-07-01 10:44:05.390` UI task-start gate found
    `http://127.0.0.1:18080/api/cloud/decision` unavailable and started
    `scripts/run-cloud-decision-dev-server.ps1`.
  - `2026-07-01 10:44:12.030` endpoint became ready with `startedBySidecar=true`.
  - `2026-07-01 10:44:12.031` `MainWindowController` logged
    `UI task start cloud sidecar gate passed`.
  - Window task queues started only after the sidecar gate passed at `10:44:12.085/086`.
  - Later starts at `11:01:51.613` and `11:25:55.070` detected the endpoint as already available
    with `startedProcess=false`, so no duplicate endpoint was spawned.
  - Live cloud samples followed in the same run for both `TASK_CLASSIFIER` and
    `TRACKER_LINK_RANKER`.

## 18. CR-HC-011 Runtime Decision Shadow Wave

```text
Source implemented / local verification passed / main review passed / fresh runtime pending
```

Goal:

- Put all already-decided runtime decision services into Shadow in one batch so the next live 五倍/修罗
  test can see the whole cloud surface at once.
- Stop validating one tiny service at a time now that `TASK_CLASSIFIER`, `TRACKER_LINK_RANKER`, real
  transport, metrics, and the local dev endpoint sidecar have passed fresh runtime.

Shadow wave services:

```text
TASK_POLICY
TASK_RECOVERY
ROUTE_CANDIDATE
ROUTE_MEMORY
NPC_CLICK_STRATEGY
DIALOG_POLICY
CAPABILITY_GATE
MAINTENANCE_THRESHOLD
TEAM_RETURN_POLICY
FAILURE_CLASSIFIER
FEATURE_FLAG
POLICY_VERSION
```

Already active / not duplicated:

```text
TASK_CLASSIFIER
TRACKER_LINK_RANKER
METRICS_INGEST via local metrics logging
```

Deferred from this wave:

- `MAP_TRANSFORM_ASSET`, `SIGNED_ASSET_BUNDLE`, and `LEARNED_MEMORY` full asset delivery.
- Reason: these are asset/cache/version distribution problems, not simple runtime decision shadow
  hooks. This wave may add lightweight `POLICY_VERSION` / `FEATURE_FLAG` echo diagnostics, but it must
  not start downloading or replacing map/template/learned-memory assets.

Safety boundary:

- Every newly connected service in this CR must remain Shadow only:
  - `shadow-enabled=true`;
  - `execute-enabled=false`;
  - `execute-percent=0`;
  - `fallback=LOCAL`.
- No newly connected service may change actual 五倍/修罗 behavior.
- No cloud result may control click coordinates, navigation coordinates, pathing intent, NPC/dialog
  choices, task phase, team permission, maintenance action, bag/return item, Runner state, or input
  queue execution in this CR.
- Local business decisions remain the only executed decisions. Cloud results are used only for
  `cloud.decision` / `cloud.metrics` comparison.
- Cloud request building should happen after the local decision/candidate/policy is known, and should
  be fire-and-forget or bounded by existing cloud timeout so it does not stall task flow.

Shared foundation implementation 2026-07-01:

- Scope adjusted by user: this worker owns only the shared foundation, not the individual business
  hook wiring.
- Added `RuntimeDecisionShadowService` in `src/main/java/com/bot/dhxy/cloud/runtime/`.
  - Public API is fire-and-forget `void shadow(...)`.
  - It builds `CloudDecisionRequest`, enriches context from the current
    `WindowTaskContextHolder.rawCurrent()` binding, and delegates to
    `CloudDecisionCoordinator.shadow(request, localDecision)`.
  - It does not return `CloudDecisionResult`, does not read `effectiveDecision`, and does not expose
    cloud output to business callers.
  - Context task identity rule after the 2026-07-01 pollution repair:
    `selectedTaskType` / `activeTaskType` / `activeTaskCode` describe the current shadow request
    task from `taskCode` / caller request context. The stale window UI/default selection is kept only
    as `windowSelectedTaskType` for diagnostics. A `taskCode=wubei` request must not report
    `selectedTaskType=XIULUO_V2` even if the bound window's selected/default task is 修罗.
- Added shadow-only defaults in `application.properties` for:
  `TASK_POLICY`, `TASK_RECOVERY`, `ROUTE_CANDIDATE`, `ROUTE_MEMORY`,
  `NPC_CLICK_STRATEGY`, `DIALOG_POLICY`, `CAPABILITY_GATE`,
  `MAINTENANCE_THRESHOLD`, `TEAM_RETURN_POLICY`, `FAILURE_CLASSIFIER`,
  `FEATURE_FLAG`, and `POLICY_VERSION`.
  - Each new service is `shadow-enabled=true`, `execute-enabled=false`, `execute-percent=0`,
    `fallback=LOCAL`.
- Confirmed local dev endpoint support:
  - `CloudDecisionDevServer` already echoes arbitrary request `serviceId` and `localDecision`
    unless a forced decision is explicitly configured.
  - No server/sidecar code change was needed for CR-HC-011 service ids.
- No business hook files were edited by this worker after scope adjustment. Follow-up workers should
  inject `RuntimeDecisionShadowService` only at post-local-decision points and must keep caller code
  fire-and-forget.

Shared foundation verification:

```powershell
mvn -q -Dtest="RuntimeDecisionShadowServiceTest,RuntimeDecisionShadowWaveWiringTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Shared foundation focused verification passed earlier in this pass before follow-up business-hook
workers changed the dirty worktree. The focused tests cover helper request fields, inactive-service
no-call behavior, all new CR-HC-011 service config defaults, and the coordinator allowlist guard that
keeps `TRACKER_LINK_RANKER` out of generic execute mode.

Worker A implementation 2026-07-01 / task strategy shadow:

- Added `TASK_POLICY` / `TASK_RECOVERY` shadow hooks in:
  - `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`;
  - `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`.
- Hook points:
  - 五倍 `TASK_POLICY`: after `WubeiStepOutcome` is produced and logged in the phase loop.
  - 五倍 `TASK_RECOVERY`: inside `recoverRoundAfterFailure(...)` after the local recovery-limit or
    reaccept route target has been selected.
  - 修罗 `TASK_POLICY`: after `XiuluoStepOutcome` is produced/logged and added to the round trace.
  - 修罗 `TASK_RECOVERY`: local retry/recover/fail outcomes in `retryCurrentOrRecover(...)` /
    `recoverOrFail(...)`, plus same-round restart decisions in phase-failure and loop-guard recovery.
- Safety result:
  - Task files call the fire-and-forget `RuntimeDecisionShadowService.shadow(...)` API only.
  - They do not import/read `CloudDecisionResult`, do not call `getEffectiveDecision`, and do not
    branch on cloud output.
  - Shadow exceptions are caught and logged, so a cloud/reporting failure does not change local
    phase/retry/reaccept/return/stop behavior.
- Added focused source guard:
  `src/test/java/com/bot/dhxy/cloud/task/TaskStrategyCloudShadowWiringTest.java`.

Worker A verification:

- RED: `mvn -q -Dtest="TaskStrategyCloudShadowWiringTest" test` failed before implementation because
  `WubeiTask` did not import/use `RuntimeDecisionShadowService`.
- Source guard GREEN was verified independently with:

```powershell
New-Item -ItemType Directory -Force target\source-guard | Out-Null
javac -d target\source-guard src\test\java\com\bot\dhxy\cloud\task\TaskStrategyCloudShadowWiringTest.java
java -cp target\source-guard com.bot.dhxy.cloud.task.TaskStrategyCloudShadowWiringTest
```

- Result: `TaskStrategyCloudShadowWiringTest passed`.
- Focused Maven guard passed after implementation:
  `mvn -q -Dtest="TaskStrategyCloudShadowWiringTest" test`.
- Required Worker A/full-worktree verification passed:

```powershell
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- Worker A did not touch `NavigationService`, `TeamReturnService`, `NpcClickService`,
  `DialogService`, `TaskMaintenanceService`, Runner, input queue, route/click/dialog/team logic, or
  any OCR/template algorithm. The task-strategy hook code remains shadow-only.

Worker B implementation 2026-07-01 / navigation shadow:

- Added `ROUTE_CANDIDATE`, `ROUTE_MEMORY`, and lightweight `POLICY_VERSION` shadow reporting in
  `src/main/java/com/bot/dhxy/service/NavigationService.java`.
- Hook points:
  - `ROUTE_CANDIDATE` reports after local world-map route action result is known in
    `performWorldMapSearchAndClickDestination(...)`, including yellow-memory, yellow-OCR,
    legacy-memory, and legacy-OCR paths.
  - `ROUTE_MEMORY` reports local route-result memory lookup/use/failure/pending-created decisions in
    the legacy and yellow destination route memory paths.
  - `POLICY_VERSION` is only a diagnostic echo (`navigation-shadow-v1`) emitted from the route
    candidate reporting path.
- Safety result:
  - `NavigationService` only calls the shared fire-and-forget `RuntimeDecisionShadowService.shadow(...)`.
  - It does not import/read `CloudDecisionResult`, does not consume `effectiveDecision`, and does not
    branch on cloud execute state.
  - No cloud response changes route choice, map point, coordinate conversion, pathing intent,
    route-memory clean/dirty state, fallback order, OCR/template matching, Runner state, or input
    queue behavior.
  - `MAP_TRANSFORM_ASSET`, signed assets, templates, map assets, and learned memory are still
    deferred/not connected.
- Added focused source guard:
  `src/test/java/com/bot/dhxy/cloud/runtime/NavigationRuntimeDecisionShadowWiringTest.java`.
- Focused RED/GREEN:
  - RED failed before implementation with `NavigationService must import CloudDecisionServiceId`.
  - GREEN passed with:

```powershell
mvn -q clean test-compile
java -cp target\test-classes com.bot.dhxy.cloud.runtime.NavigationRuntimeDecisionShadowWiringTest
```

- Required Worker B verification passed:

```powershell
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- Latest result after clearing the parallel-Maven `target` race: both commands exited `0`.

Expected hook groups:

1. Task strategy:
   - `TASK_POLICY`: phase/branch recommendation shadow after local phase decision is known.
   - `TASK_RECOVERY`: retry/reaccept/return/stop recommendation shadow at existing recovery/failure
     branches.
2. Navigation:
   - `ROUTE_CANDIDATE`: route/yellow-map/current-map candidate scoring shadow after local candidate is
     selected.
   - `ROUTE_MEMORY`: clean/dirty/降权 recommendation shadow when local route memory is read or updated.
3. Interaction:
   - `NPC_CLICK_STRATEGY`: NPC click strategy order/result shadow around `NpcClickService`'s local
     strategy pipeline.
   - `DIALOG_POLICY`: dialog option/fallback order shadow around `DialogService` after local option is
     selected.
4. Team / maintenance:
   - `CAPABILITY_GATE`: read-only diagnostic shadow of the local capability/session gate result. It
     must not consume or grant capability.
   - `MAINTENANCE_THRESHOLD`: threshold/budget shadow for common box, first-aid, incense, summon-skill
     queue, and related maintenance decisions.
   - `TEAM_RETURN_POLICY`: return-team wait window, precheck outcome, and priority shadow around
     `TeamReturnService` / local support windows.
5. Diagnostics / policy:
   - `FAILURE_CLASSIFIER`: classify timeout/failure/blocker samples from existing failure branches.
   - `FEATURE_FLAG`: dev endpoint echo of relevant flag/context; diagnostics only.
   - `POLICY_VERSION`: dev endpoint echo of policy version/session; diagnostics only.

Expected implementation ownership:

- 谢帅/main agent remains manager/reviewer only and does not write Java business implementation.
- Worker agents own implementation in disjoint domains and must not revert unrelated dirty work.
- A helper review agent must independently check shadow-only safety, code growth, default config, and
  no behavior consumption before main acceptance.

Suggested worker split:

1. Worker A - task strategy shadow:
   - Owns `TASK_POLICY` / `TASK_RECOVERY`.
   - Likely touches 五倍/修罗 task phase/recovery call sites only to add diagnostics calls after local
     decisions are already made.
2. Worker B - navigation shadow:
   - Owns `ROUTE_CANDIDATE` / `ROUTE_MEMORY` plus lightweight `POLICY_VERSION` if shared config is
     needed.
   - Must not alter route selection or map transform math.
3. Worker C - interaction shadow:
   - Owns `NPC_CLICK_STRATEGY` / `DIALOG_POLICY`.
   - Must not alter click points, strategy order, dialog option selection, or testcase-replay-sensitive
     logic.
   - Implementation note 2026-07-01:
     - `NpcClickService` reports `NPC_CLICK_STRATEGY` from `recordSmartClickEvidence(...)` after a
       local `NpcClickStrategyResult` exists, including skipped strategy results before the
       vision-memory learning gate ignores them. The cloud sample records strategy/status/clicked/
       verified context only; it does not change target click coordinates, tooltip/yellow OCR/
       player-anchor/Ctrl fallback order, learned memory, or retry budget.
     - `DialogService` reports `DIALOG_POLICY` from `finishRequest(...)` after the local
       `DialogResult` has been built and before returning that same result. The cloud sample records
       operation/policy/status/action context only; it does not change option choice, remembered
       option consumption, fallback order, item-give behavior, or visual matching.
     - Source guard:
       `src/test/java/com/bot/dhxy/cloud/runtime/InteractionShadowWiringTest.java`.
     - Focused guard passed with:
       `java src/test/java/com/bot/dhxy/cloud/runtime/InteractionShadowWiringTest.java`.
     - Required verification passed after refreshing stale `target/classes`:
       `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test` and
       `mvn -q -DskipTests compile`.
4. Worker D - team/maintenance/diagnostics shadow:
   - Owns `CAPABILITY_GATE`, `MAINTENANCE_THRESHOLD`, `TEAM_RETURN_POLICY`, `FAILURE_CLASSIFIER`,
     `FEATURE_FLAG`.
   - Must not alter CR138 local-session/capability semantics or maintenance action order.
   - Implementation note 2026-07-02:
     - `TaskMaintenanceService` now consumes dedicated `CAPABILITY_GATE` and
       `MAINTENANCE_THRESHOLD` cloud decision wrappers instead of treating those services as
       fire-and-forget behavior shadows.
     - `CAPABILITY_GATE` can only narrow the local gate:
       `effective allow = localAllowed && cloudAllow`. Cloud `ALLOW` cannot revive a stale,
       closed, completed, disabled, or otherwise locally denied CR138/CR148 session/capability
       window. Cloud unavailable/invalid/STOP required failure denies.
     - `MAINTENANCE_THRESHOLD` is checked before downstream maintenance actions. Cloud `ALLOW`
       permits the already-local enabled maintenance request, `SKIP` / `NO_ACTION` return
       no-action, and required failure returns explicit
       `TaskMaintenanceStatus.CLOUD_REQUIRED_FAILURE` without calling dialog/summon maintenance
       actions.
     - `TeamReturnService` now consumes dedicated `TEAM_RETURN_POLICY` decisions. Member return
       click failure/deny happens before `ensureSheYaoXiangActive(...)` and before
       `inputSequences.submitAndWait(...)`; leader wait failure returns false; leader precheck
       failure returns a conclusive `cloud-required-failure` status instead of inconclusive local
       continuation.
     - `FEATURE_FLAG` and `FAILURE_CLASSIFIER` remain diagnostic-only at these business call sites.
       They may be configured as execute/STOP by the wave, but `TaskMaintenanceService` /
       `TeamReturnService` do not consume behavior-controlling feature/failure decision services.
     - Added/updated Worker D tests:
       `CapabilityGateCloudDecisionServiceTest`,
       `TaskMaintenanceCloudRequiredFailureTest`,
       `TeamReturnCloudRequiredFailureTest`, and
       `RuntimeDecisionWorkerDShadowWiringTest`.
     - Worker D verification passed:
       `mvn -q -Dtest="CapabilityGateCloudDecisionServiceTest,TaskMaintenanceCloudRequiredFailureTest,TeamReturnCloudRequiredFailureTest" test`,
       `java -cp 'target/test-classes;target/classes' com.bot.dhxy.cloud.runtime.RuntimeDecisionWorkerDShadowWiringTest`,
       `java -cp 'target/test-classes;target/classes' com.bot.dhxy.cloud.runtime.RuntimeDecisionShadowWaveWiringTest`,
       `mvn -q -Dtest="TaskMaintenanceCR138LocalSupportCapabilityTest,AutoCombatCR138FirstAidOnlyCommonBoxGuardTest,CR148LocalTeamSessionInvalidationWiringTest" test`,
       `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test`, and
       `mvn -q -DskipTests compile` all exited `0`.
     - No WubeiTask, XiuluoTaskV2, NavigationService, NpcClickService, DialogService,
       input queue internals, OCR/template/click geometry, or physical click-coordinate logic was
       changed by this worker.

Logging requirements:

Every new shadow hook must log through the existing cloud decision coordinator / metrics path and be
queryable with:

```powershell
Select-String -Path logs/dhxy-console.log -Pattern "cloud.decision serviceId="
Select-String -Path logs/dhxy-console.log -Pattern "cloud.metrics"
```

Each log sample must expose:

```text
serviceId, taskCode, phase, traceId, context, localDecision, cloudDecision,
effectiveDecision, agree, executed=false, elapsedMs, success, fallback, reason
```

Verification:

```powershell
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Main review 2026-07-01:

- 谢帅/main agent did not write Java business implementation for this CR; Java business hooks were
  implemented by the assigned worker agents and then reviewed here.
- Focused worker/source guards passed:

```powershell
mvn -q -Dtest="RuntimeDecisionShadowServiceTest,RuntimeDecisionShadowWaveWiringTest,TaskStrategyCloudShadowWiringTest,InteractionShadowWiringTest,NavigationRuntimeDecisionShadowWiringTest,RuntimeDecisionWorkerDShadowWiringTest" test
```

- Full cloud suite and compile passed:

```powershell
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- Static safety grep result:
  - New CR-HC-011 services are configured as `shadow-enabled=true`, `execute-enabled=false`,
    `execute-percent=0`, and `fallback=LOCAL`.
  - `CloudDecisionCoordinator.EXECUTABLE_SERVICES` still contains only `TASK_CLASSIFIER`.
  - Business `CloudDecisionResult` / `effectiveDecision` / `isExecuted()` consumption remains limited
    to the existing `TaskTrackerPanelService` / `TASK_CLASSIFIER` execute path. None of the new
    CR-HC-011 hooks consumes cloud output.
  - `TRACKER_LINK_RANKER` remains execute-disabled in runtime config; execute-mode appearances in
    logs during tests are test-only gate cases.
  - `MAP_TRANSFORM_ASSET`, `SIGNED_ASSET_BUNDLE`, and cloud `LEARNED_MEMORY` asset delivery remain
    deferred; no cloud asset download/replacement path was added.
- `git diff --check` on the touched CR-HC-011 files reported only existing LF/CRLF working-copy
  warnings and no whitespace errors.

Fresh runtime acceptance:

- User must restart the DHXY Java app after this CR lands so Spring loads the new shadow services.
- Start 五倍/修罗 from the UI. The sidecar should start or reuse `127.0.0.1:18080` automatically.
- Runtime logs should show the new CR-HC-011 service ids with `mode=SHADOW`, `executed=false`, and
  local task behavior unchanged.
- If any new service fails or times out, the task must keep local fallback and continue.

Rollback:

```properties
cloud.services.task-policy.shadow-enabled=false
cloud.services.task-recovery.shadow-enabled=false
cloud.services.route-candidate.shadow-enabled=false
cloud.services.route-memory.shadow-enabled=false
cloud.services.npc-click-strategy.shadow-enabled=false
cloud.services.dialog-policy.shadow-enabled=false
cloud.services.capability-gate.shadow-enabled=false
cloud.services.maintenance-threshold.shadow-enabled=false
cloud.services.team-return-policy.shadow-enabled=false
cloud.services.failure-classifier.shadow-enabled=false
cloud.services.feature-flag.shadow-enabled=false
cloud.services.policy-version.shadow-enabled=false
```

## 19. CR-HC-012 TrackerLinkRanker Window-Relative Execute

```text
Source implemented / local verification passed / fresh runtime pending
```

User decision:

- CR-HC-009 的 candidate-index execute gate 太保守，只适合作为早期安全壳。
- `TRACKER_LINK_RANKER` 的 shadow 已经有 fresh runtime 证据：修罗/五倍共 `14/14`
  success/agreement，p95 `17ms`，没有 `success=false`、`agree=false`、timeout/failure。
- 第一版真实接管绿链时，不再做“云端结果 vs 本地业务结果”的业务比对。
- 云端应直接返回窗口相对点击点；本地只做通用执行安全壳，然后通过 input queue 点击。

Goal:

- 把五倍/修罗 tracker 绿链点击的执行点切到云端：
  `left tracker screenshot/context -> cloud -> window-relative click point -> local input queue click`。
- 云端返回的是窗口相对坐标，不是屏幕绝对坐标。
- 本地不再把本地候选 index / 本地候选框作为执行前提。长期目标是本地不保留绿链核心识别逻辑。

Response contract:

```text
decision=click=<windowX>,<windowY>
diagnostics.action=CLICK_TRACKER_LINK
diagnostics.coordinateSpace=WINDOW_RELATIVE
diagnostics.confidence=<0..1>
```

Allowed local safety checks:

- `serviceId` / `traceId` / schema must match the current request.
- Cloud response must arrive inside the configured timeout/ttl window.
- `decision` must parse as one window-relative point.
- `coordinateSpace` must be `WINDOW_RELATIVE`.
- The point must be inside the current window bounds and the known left-tracker/action ROI.
- Current task/window must not be paused/stopped.

Explicitly removed from this CR:

- Do not require cloud to return a local candidate index.
- Do not require `diagnostics.candidateFingerprint`.
- Do not require the cloud point to match a locally generated green-link candidate.
- Do not silently fallback to the local green-link choice when execute mode is enabled for this test.

Failure policy for this CR:

- In execute testing, cloud failure/invalid response should be visible and loud.
- If cloud cannot provide an accepted window-relative click point, the tracker-link execute result must
  be rejected and logged with `executed=false` plus a clear reason.
- The task path must not quietly click the old local link after a cloud failure when this CR's execute
  config is being tested. It may stop/fail the current green-link action according to `fallback=STOP`
  or an equivalent explicit no-click result, so runtime testing can prove whether cloud truly works.
- A config rollback must still exist: turning `cloud.services.tracker-link-ranker.execute-enabled=false`
  returns to the pre-CR local path.

Implementation ownership:

- 业务实现必须由 worker 子智能体完成。
- 谢帅/main agent 只负责写卡、分派、审查、跑验证、写回文档，不直接写 Java 业务实现。
- 另派 helper/reviewer 子智能体独立审查，重点看是否还残留本地业务比对、是否仍偷回本地点击、
  是否扩大到 NPC/dialog/navigation/return-item/team maintenance。

Likely touch areas:

- `TrackerLinkRankerCloudShadowService` / `TrackerLinkRankerCloudDecision` response parsing and result
  envelope.
- 五倍/修罗当前调用 `shadowTrackerLinkSelection(...)` 后的绿链点击消费点。
- `application.properties` test config for `TRACKER_LINK_RANKER` execute mode.
- Focused tests proving:
  - cloud `click=x,y` can become the effective window-relative click point;
  - invalid coordinate/schema/timeout/paused state does not click local fallback in execute testing;
  - execute disabled returns to existing local behavior;
  - unrelated cloud services and task phase/navigation/dialog/team logic are untouched.

Verification:

```powershell
mvn -q -Dtest="TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudDecisionCoordinatorTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Implementation result 2026-07-01:

- `TrackerLinkRankerCloudShadowService` execute gate accepts only
  `decision=click=<windowX>,<windowY>` with
  `diagnostics.action=CLICK_TRACKER_LINK` and
  `diagnostics.coordinateSpace=WINDOW_RELATIVE`.
- Execute mode no longer requires `candidateFingerprint`, `index=N`, or a cloud point matching a
  local green-link candidate. Local candidate/index data remains only as request context and local
  rollback context.
- `TrackerLinkRankerCloudDecision` represents three task-facing outcomes:
  local passthrough when execute is disabled/missed, accepted cloud window-relative click when
  executed, and rejected no-click when execute mode reached cloud/gate but failed.
- 五倍 now passes the returned decision into the three requested tracker green-link click sites:
  probe tracker pathing, combat tracker pathing, and enter-battle retry. 修罗 V2 consumes the decision
  at `trackerShortcut`.
- When cloud execute succeeds, 五倍/修罗 convert the cloud window-relative point with the current
  `GameClientTracker` logical `windowBaseX/windowBaseY` and then click through the existing atomic
  input queue path. The dev sidecar reads `context.selectedWindowClick`, not the screen-absolute
  `selectedClick`, so DPI scaling and window-base units stay aligned with the existing input layer.
- When cloud execute fails or returns an invalid point, the task logs the no-click/rejection and does
  not click the old local tracker green point for this execute attempt.
- If the current tracker window base cannot be refreshed, 五倍/修罗 pass an invalid base and treat the
  execute path as explicit no-click instead of using stale coordinates.
- Current CR-HC-012 live-test config is execute-on:
  `cloud.services.tracker-link-ranker.execute-enabled=true`,
  `cloud.services.tracker-link-ranker.execute-percent=100`, and
  `cloud.services.tracker-link-ranker.fallback=LOCAL`. Rollback remains a config-only switch by
  turning execute back off.

Focused test coverage added/updated:

- Valid cloud click is accepted and exposes `cloudWindowRelativeClickPoint`.
- Wrong `diagnostics.action`, wrong `diagnostics.coordinateSpace`, invalid `click=` format, old
  `index=N`, out-of-ROI coordinate, schema mismatch, and timeout become rejected no-click results.
- Execute-disabled config keeps local passthrough; current local runtime config is intentionally
  execute-enabled for fresh CR-HC-012 testing.
- Source guard verifies 五倍/修罗 consume `TrackerLinkRankerCloudDecision`, convert cloud clicks from
  tracker logical window base, keep physical clicks on the input queue, and do not retain the old
  "tasks do not consume cloud decision" shape.

Local verification 2026-07-01:

```powershell
mvn -q -Dtest="TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudDecisionCoordinatorTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- All three commands exited `0`.
- Manager re-verification also passed:

```powershell
mvn -q -Dtest="CloudDecisionDevServerTest,TrackerLinkRankerCloudShadowServiceTest,TrackerLinkRankerCloudShadowWiringTest,CloudDecisionCoordinatorTest,RuntimeDecisionShadowWaveWiringTest" test
mvn -q -DskipTests compile
```

- The focused test log included `TRACKER_LINK_RANKER mode=EXECUTE ... cloudDecision=click=42,555
  effectiveDecision=click=42,555 executed=true`.

Fresh runtime acceptance:

- Restart the DHXY Java app after implementation.
- Run 修罗 and 五倍 with the local cloud dev endpoint sidecar active.
- Logs should show `TRACKER_LINK_RANKER mode=EXECUTE executed=true` for green-link click samples.
- The effective decision should be `click=<windowX>,<windowY>` with
  `coordinateSpace=WINDOW_RELATIVE`.
- User-visible behavior: tracker green-link clicks still enter pathing/combat normally.
- Any cloud failure should be obvious in logs and should not be hidden by an automatic local green-link
  click during this execute test.

## 20. CR-HC-013 RouteMemory / RouteCandidate Execute

```text
Implemented / review repair verification passed
```

User decision:

- `TRACKER_LINK_RANKER` execute has been live-tested enough for the current stage.
- The next cloud step should not stay in slow shadow-only mode. For route decisions, the goal is to
  let the cloud response become the real decision immediately, then use live runtime failures to
  identify and repair cloud-side or contract bugs.
- First route execute scope is limited to `ROUTE_MEMORY` and `ROUTE_CANDIDATE`.
- This CR intentionally accepts the risk that a bad cloud route decision can make navigation wrong
  during test runs, because the user wants direct signal instead of prolonged shadow comparison.
- The comparison direction is reversed from the earlier shadow wave: cloud is the authoritative
  execute decision, and the old local route logic becomes the shadow/oracle. If a route goes wrong,
  logs must show what cloud executed and what local would have done, so the mismatch is immediately
  attributable.

Goal:

- Convert route memory / route candidate from diagnostic-only shadow into true request/response
  execute:
  `navigation context -> cloud -> route/memory decision -> local executor`.
- 本地仍负责窗口绑定、截图/input queue、安全停止/暂停、以及最终点击执行。
- 云端负责决定 route memory 是否可用、route candidate 应该采用哪一种结果、以及必要的
  window-relative / route-result click payload。
- 本地旧 route 决策仍要计算出来，但只作为 `localShadowDecision` / `localWouldClick` 写入日志，
  不能再作为默认有效结果抢回主流程。

Execute services:

```text
ROUTE_MEMORY
ROUTE_CANDIDATE
```

Expected response contracts:

```text
ROUTE_MEMORY:
decision=lookup=<HIT|MISS|DISABLED>;routeMode=<mode>;click=<windowX>,<windowY>|;reason=<text>
diagnostics.coordinateSpace=WINDOW_RELATIVE when click is present

ROUTE_CANDIDATE:
decision=routeMode=<CANONICAL_ROUTE_MODE>;candidateSource=<source>;status=<CLICKED|NOT_FOUND|SKIP|FAILED>;click=<windowX>,<windowY>|;reason=<text>
diagnostics.coordinateSpace=WINDOW_RELATIVE when click is present
routeMode is canonical upper snake case (YELLOW_DESTINATION_MINI_MAP). Legacy responses used
`mode=yellow-destination-mini-map`; the client execute gate transitionally accepts that key/value
form (routeMode read first, then mode, value normalized), unknown or missing modes stay rejected.
```

Allowed local safety checks:

- `serviceId` / `traceId` / schema must match the current request.
- Cloud response must arrive inside the configured timeout.
- If a response contains `click=`, the point must parse as one window-relative point and stay inside
  current window bounds / route result ROI.
- Current task/window must not be paused/stopped.
- If cloud returns `MISS`, `NOT_FOUND`, `SKIP`, or an invalid no-click response, do not fabricate a
  local successful route click in execute mode.

Failure policy for this CR:

- During execute testing, cloud route failures should be loud and attributable.
- A failed/invalid cloud route decision may fall back only through the explicit configured
  `fallback=LOCAL` path, with logs showing `executed=false` or the rejection reason.
- Do not silently hide an execute failure by reporting the local route decision as if the cloud
  succeeded.
- If cloud execute succeeds but navigation later behaves incorrectly, the report/log must contain the
  paired local shadow result from the same route context. This lets the user compare:
  `cloud executed = X` vs `local would have done = Y`.

Implementation ownership:

- 业务实现必须由 worker 子智能体完成。
- 谢帅/main agent 只负责写卡、分派、审查、跑验证、写回文档，不直接写 Java 业务实现。
- 另派 helper/reviewer 子智能体独立审查，重点看是否真正由 cloud response 接管 route
  decision、是否还偷回本地 route 结果、是否扩大到 NPC/dialog/battle/return/team maintenance。

Likely touch areas:

- `NavigationService` current `shadowRouteCandidate(...)` / `shadowRouteMemory(...)` call sites and
  nearby route-result consumption.
- New or revised cloud task/runtime route decision result models for parsed execute results.
- `RuntimeDecisionShadowService` may need a route-specific execute-capable collaborator instead of
  the current fire-and-forget diagnostic helper.
- Local dev sidecar response handling for `ROUTE_MEMORY` and `ROUTE_CANDIDATE`.
- Structured logs/metrics must preserve both sides: cloud effective execute decision and local shadow
  decision.
- `application.properties` live-test config:

```properties
cloud.services.route-memory.shadow-enabled=true
cloud.services.route-memory.execute-enabled=true
cloud.services.route-memory.execute-percent=100
cloud.services.route-memory.fallback=LOCAL
cloud.services.route-candidate.shadow-enabled=true
cloud.services.route-candidate.execute-enabled=true
cloud.services.route-candidate.execute-percent=100
cloud.services.route-candidate.fallback=LOCAL
```

Do not change in this CR:

- NPC click strategy execution.
- Dialog policy execution.
- Tracker link ranker behavior beyond compatibility with existing execute.
- Battle/return-item/team maintenance/三技能/input queue behavior.
- OCR/template matching thresholds unless the route execute contract requires a focused parser test.

Verification:

```powershell
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Worker implementation result (2026-07-01):

- Added route-specific execute model/service:
  `src/main/java/com/bot/dhxy/cloud/task/RouteCloudDecision.java`
  and `src/main/java/com/bot/dhxy/cloud/task/RouteCloudDecisionService.java`.
- `RouteCloudDecisionService` uses the existing coordinator plus a dedicated route execute gate.
  Only `ROUTE_MEMORY` / `ROUTE_CANDIDATE` are allowed through this gate.
- Cloud click responses must be window-relative, parseable, and inside the 1024x768 game window.
  They must also stay inside the route-result/yellow-result ROI. Invalid schema/coordinate/ROI
  responses become explicit no-click/rejection with `executed=false`.
- `NavigationService` now computes the old route result as local shadow/oracle and asks the cloud
  route service before submitting the real click for:
  legacy route memory, yellow route memory, yellow OCR route candidate, and legacy coordinate OCR
  route candidate.
- Review repair: if the local route-candidate scan fails but the same cloud execute response returns
  `status=CLICKED;click=x,y`, the failed path now consumes that `RouteCloudDecision` before retrying
  or closing the route panel. Legacy-green clicks use the existing route result click/cleanup path;
  yellow destination clicks use the existing yellow-row -> destination mini-map -> pathing
  confirmation path. The local failed result remains recorded as `localShadowDecision`.
- If local route memory misses but cloud returns `lookup=HIT` with a valid click, the cloud click is
  carried through the existing local click/confirmation path without writing a fake clean entry into
  local memory.
- Valid cloud execute replaces the local route-result row/link click. Existing local window binding,
  input queue submission, mini-map final-coordinate click, cleanup, and pathing confirmation remain
  local.
- Successful route candidate clicks are not reported a second time after click execution; failed
  local scans still report no-click status for diagnostics.
- Runtime config changed only the two route services to execute:
  `route-memory.execute-enabled=true`, `route-memory.execute-percent=100`,
  `route-candidate.execute-enabled=true`, and `route-candidate.execute-percent=100`.
- Local dev sidecar now includes `diagnostics.coordinateSpace=WINDOW_RELATIVE` for route responses
  containing `click=` and supports `--route-click x,y` for route-specific cloud click override, so
  tests can prove route execute is using a cloud click different from the local shadow click.

Worker verification:

```powershell
mvn -q clean -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- Focused route tests covered valid cloud execute, invalid schema/coordinate rejection,
  execute-disabled local behavior, and retained local shadow/oracle result.
- First RED run failed because `RouteCloudDecisionService` did not exist; final focused/cloud/compile
  verification passed.

Review repair verification (2026-07-01):

```powershell
mvn -q -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest,CloudDecisionDevServerTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- Added focused guards for local-failure path consuming cloud `CLICKED`, route-result ROI rejection,
  and dev sidecar route click override different from local.
- First full cloud-suite run hit a transient `TrackerLinkRankerCloudShadowServiceTest$CapturingClient`
  class-load failure; the class was present in `target/test-classes`, the specific test passed on
  rerun, and the full cloud suite passed on the following run.

Manager / helper final review (2026-07-01):

- Final product decision is cloud-primary route execution: `ROUTE_MEMORY` and `ROUTE_CANDIDATE`
  cloud responses are the authoritative route-result row/link click decisions.
- The existing local route calculation remains in the same runtime context only as shadow/oracle
  evidence, so a live route failure can be debugged by comparing "cloud clicked" against "local
  would have clicked".
- Helper review result: PASS. No remaining P1/P2 issues were found after the local-failure cloud
  click consumption repair, route-result ROI gate, and dev sidecar route-click override.
- Manager verification passed:

```powershell
mvn -q -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest,CloudDecisionDevServerTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Route memory ownership decision (2026-07-01):

- Route memory is treated as a route asset, not an ordinary local cache. Once it covers enough
  `fromMap -> targetMap -> click` pairs, leaking the full local memory file would let a copied
  client bypass much of the yellow-route recognition/candidate-ranking work.
- Target product direction: cloud is the authoritative route-memory owner. Local runtime should not
  persist the full long-lived route-memory table after this surface is migrated.
- The local Runner stays local. It is the only component that can reliably observe window-bound
  arrival facts such as current map/coordinate, active pathing intent, timeout, `ARRIVED`,
  `STOPPED_AWAY`, or intent replacement.
- Cloud route-memory flow should be:

```text
client asks cloud: fromMap + targetMap + routeMode
-> cloud returns HIT + window-relative click + routeDecisionId, or MISS/DISABLED/ERROR
-> local executor clicks when HIT, otherwise continues the existing non-memory route search/candidate path
-> local Runner observes the result
-> Runner reports outcome to cloud with routeDecisionId / intentId / observed map-coordinate facts
-> cloud records success/failure/clean/dirty and owns the durable route memory
```

- Cloud `MISS`, `DISABLED`, timeout, or invalid response is not fatal. It means "no usable cloud
  memory"; the task should continue through the existing non-memory route search/candidate path.
- The server must not blindly trust outcome reports. Outcome ingest must validate that the report is
  tied to a recent cloud-issued `routeDecisionId`, matches the expected from/target/mode/click, is
  inside an allowed time window, is idempotent for repeated reports, and includes enough observed
  local facts for later audit.
- The current `world-map-route-memory-pending` log is a lifecycle/accounting event, not a click
  decision. It should not be counted as a failed `ROUTE_MEMORY` execute decision just because it does
  not contain `lookup=HIT/MISS/DISABLED`. It should either be removed from execute metrics or moved to
  a dedicated outcome/audit contract.
- First implementation stage should not preserve local full-memory fallback as a product behavior.
  If cloud memory is unavailable, fall through to normal route candidate/OCR search rather than using
  a local long-lived memory table as the authority.

Fresh runtime acceptance:

- Restart DHXY after implementation.
- Run 五倍 and 修罗 with local dev sidecar active.
- Logs must show `ROUTE_MEMORY mode=EXECUTE` and `ROUTE_CANDIDATE mode=EXECUTE` samples when route
  paths are reached.
- Successful execute samples must show the cloud decision as the effective decision.
- Each execute sample must also include the local shadow/oracle result for the same route context.
- If a route goes wrong, logs must make it clear whether the bad result came from `ROUTE_MEMORY` or
  `ROUTE_CANDIDATE`, including phase/source/fromMap/targetMap/click/status.

## 21. CR-HC-014 TaskPolicy Execute

```text
Planned / worker implementing / fresh runtime pending
```

Goal:

- Move the next small cloud surface from shadow to execute without touching visual click logic.
- `TASK_POLICY` cloud response may become the authoritative phase outcome for 五倍 and 修罗.
- The existing local phase outcome must still be computed first and recorded as the local
  shadow/oracle result, so live issues can compare "cloud executed" with "local would have done".

Why this card is small:

- It uses the existing `TASK_POLICY` shadow hook points already present after local
  `WubeiStepOutcome` / `XiuluoStepOutcome` calculation.
- It controls only enum-like phase outcome fields: `result`, `yield`, and `next`.
- It does not introduce any new OCR/template matching, coordinate conversion, mouse click point,
  NPC click strategy, dialog option choice, route candidate, route memory, team maintenance, bag,
  battle, return item, or input queue behavior.

Cloud execute contract:

```text
serviceId=TASK_POLICY
taskCode=wubei | xiuluo_v2
phase=phase-outcome
cloudDecision=result=<TaskTransactionResult>;yield=<TaskYieldPolicy>;next=<phase enum>;reason=<text>
diagnostics.coordinateSpace is not used
```

Acceptance / rejection rules:

- Local task code must always produce the old local outcome first.
- Cloud `result` must parse to `TaskTransactionResult`.
- Cloud `yield` must parse to `TaskYieldPolicy`.
- Cloud `next` must parse to the current task's own phase enum:
  - 五倍: `WubeiPhase`
  - 修罗: `XiuluoPhase`
- Cloud must not change a local `STOPPED` outcome. If local result is stopped, keep local.
- Cloud must not force `STOPPED`; stop remains a local task-control concern.
- If cloud schema is missing, malformed, not executable, timeout, wrong phase enum, or tries to
  cross task phase domains, fallback to local and log the rejection.
- If cloud is accepted, build a new task-specific step outcome using the same current state and
  message/original context where possible, replacing only result/yield/next with the accepted cloud
  policy.

Logging requirements:

- Every `TASK_POLICY` execute sample must log:
  - `mode=EXECUTE`;
  - local decision string;
  - cloud decision string;
  - effective decision string;
  - `executed=true|false`;
  - rejection/fallback reason when not executed.
- Local decision string must remain human-readable:
  `phase=<current>;result=<localResult>;yield=<localYield>;next=<localNext>`.
- Context must include `round`, current phase, local next phase, effective next phase, source,
  message, and task code.

Runtime config for this card:

```properties
cloud.services.task-policy.shadow-enabled=true
cloud.services.task-policy.execute-enabled=true
cloud.services.task-policy.execute-percent=100
cloud.services.task-policy.fallback=LOCAL
```

Do not change in this CR:

- `TASK_RECOVERY` execute. Recovery remains shadow-only.
- `ROUTE_MEMORY` / `ROUTE_CANDIDATE` behavior from CR-HC-013.
- `TRACKER_LINK_RANKER` behavior from CR-HC-012.
- NPC click, dialog, maintenance, team return, return item, battle, runner, input queue, OCR,
  templates, minimap/world-map coordinate conversion, or learned memory.

Implementation ownership:

- 谢帅/main agent is manager/reviewer only: writes the card, dispatches worker/reviewer, verifies, and
  does not write Java business implementation.
- Worker owns the minimal `TASK_POLICY` execute service/envelope, 五倍/修罗 consumption wiring, config,
  and focused tests.
- Helper reviewer independently checks spec compliance, code size, fallback safety, no cross-task
  phase parsing, and no visual/click behavior change.

Suggested touched areas:

- `src/main/java/com/bot/dhxy/cloud/task/` for a task-policy execute envelope/service.
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java` only around existing
  `shadowTaskPolicyDecision(...)` / phase outcome consumption.
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` only around existing
  `shadowTaskPolicyDecision(...)` / phase outcome consumption.
- `src/main/resources/application.properties` for `task-policy` execute enablement only.
- Focused tests under `src/test/java/com/bot/dhxy/cloud/`.

Verification:

```powershell
mvn -q -Dtest="TaskPolicyCloudDecisionServiceTest,TaskPolicyExecuteWiringTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Worker implementation result (2026-07-01):

- Added `TaskPolicyCloudDecision` and `TaskPolicyCloudDecisionService` under
  `src/main/java/com/bot/dhxy/cloud/task/`.
- `TASK_POLICY` execute uses a dedicated service gate and does not widen the generic
  `CloudDecisionCoordinator` executable allowlist.
- 五倍 and 修罗 still compute the local `WubeiStepOutcome` / `XiuluoStepOutcome` first. They log or
  trace that local outcome as the oracle, then call the task-policy execute service before the
  phase-machine consumes STOPPED/FAILED/PATHING/SHARED_STATE/yield/next.
- Accepted cloud policy replaces only `result`, `yield`, and `next`. Existing task state, source,
  message, waitSpec, objective/runtime context, and all click/navigation/visual behavior remain
  local.
- Rejection/fallback guards cover malformed or missing schema, wrong task phase enum domain, cloud
  `STOPPED`, local/runner `STOPPED`, and execute-disabled passthrough.
- Runtime config now enables only task-policy execute:
  `cloud.services.task-policy.execute-enabled=true` and
  `cloud.services.task-policy.execute-percent=100`; `TASK_RECOVERY` execute remains disabled.

Worker verification:

```powershell
mvn -q -Dtest="TaskPolicyCloudDecisionServiceTest,TaskPolicyExecuteWiringTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- All three commands exited `0`.
- First RED pass failed on missing `TaskPolicyCloudDecisionService`, then the focused/cloud/compile
  verification passed after implementation.

Worker review repair (2026-07-01):

- P1 repaired: 五倍/修罗 apply `TASK_POLICY` execute inside the
  `taskTransactionRunner.run(...)` callback. The callback now records the local oracle first, applies
  cloud policy, stores the effective outcome into `phaseOutcome`, and returns the effective
  `outcome.transactionResult()` to `TaskTransactionRunner`.
- This fixes the earlier blocker where `TaskTransactionRunner` / `TaskTurnCoordinator.leave(...)`
  saw only the local pre-cloud result while the outer loop consumed the cloud-decorated result later.
- The outer loops now consume the effective `AtomicReference` outcome and no longer re-apply cloud
  after the transaction returns.
- STOPPED guard remains intact: local result is passed as the pre-runner `runnerResult`, so local
  STOPPED cannot be overridden, and cloud `result=STOPPED` remains rejected.
- P2 repaired: accepted `TASK_POLICY` execute now requires non-blank `reason`; malformed/missing
  reason keeps local.
- `CloudDecisionDevServer` now handles `TASK_POLICY` explicitly and returns
  `result=...;yield=...;next=...;reason=dev-local-task-policy` instead of echoing
  `phase=...;result=...;yield=...;next=...`. Tracker-link and route sidecar behavior are unchanged.

Repair verification:

```powershell
mvn -q -Dtest="TaskPolicyCloudDecisionServiceTest,TaskPolicyExecuteWiringTest" test
mvn -q -Dtest="CloudDecisionDevServerTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- All four commands exited `0`.

Second manager review repair (2026-07-01):

- P1.2 repaired: the first repair moved task-policy application into the transaction callback and
  returned the effective `TaskTransactionResult`, but the runner still constructed
  `TaskTransactionOutcome` with the fixed caller yield `CONTINUE_CHAIN`. Effective cloud yield was
  therefore not visible to `TaskTurnCoordinator.shouldYield(...)`.
- Added `TaskTransactionRunner.runDynamic(...)` and
  `TaskTransactionRunner.TaskTransactionDecision(result, yieldPolicy)`. This is the transaction
  boundary for policies that can change both result and yield before task-turn ownership is decided.
- 五倍/修罗 phase loops now use `runDynamic(...)`. The callback keeps the local oracle evidence,
  applies `TASK_POLICY` execute inside the callback, stores the effective outcome, and returns
  `TaskTransactionDecision.of(outcome.transactionResult(), outcome.yieldPolicy())`.
- `READY_TO_CONTINUE + MUST_YIELD` is now represented in the runner's
  `TaskTransactionOutcome.yieldPolicy()` before `TaskTurnCoordinator.leave(outcome)` runs.
- P2 remains intact: accepted task-policy execute still requires non-blank `reason`, and the local
  dev server still returns
  `result=...;yield=...;next=...;reason=dev-local-task-policy` for `TASK_POLICY`.

Second repair verification:

```powershell
mvn -q -Dtest="TaskPolicyCloudDecisionServiceTest,TaskPolicyExecuteWiringTest" test
mvn -q -Dtest="CloudDecisionDevServerTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- All four commands exited `0`.

Final manager/helper review (2026-07-01):

- Manager thread re-ran all four verification commands above; all exited `0`.
- Helper reviewer re-reviewed the repaired code and returned `PASS`.
- Previous P1 is closed: effective cloud `result` and `yield` both reach
  `TaskTransactionOutcome` before `TaskTurnCoordinator.leave(...)`.
- Previous P2 is closed: accepted `TASK_POLICY` execute requires non-blank `reason`, and the local
  dev sidecar produces a strict task-policy decision instead of raw echoing `localDecision`.
- No new P1/P2 blockers were found in the final review.

Fresh runtime acceptance:

- Restart DHXY after implementation.
- Run 五倍 and 修罗 with local dev sidecar active.
- Logs must show `TASK_POLICY mode=EXECUTE` samples for 五倍 and 修罗 phase outcomes.
- Accepted samples must show cloud effective phase outcome and the old local shadow/oracle phase
  outcome in the same line.
- Rejected samples must keep local outcome and include a clear reason.

## 22. CR-HC-015 TaskPolicy Metrics Semantic Agreement

```text
Worker implemented / Maven verification passed / fresh runtime pending
```

Problem:

- Fresh runtime proved `TASK_POLICY` execute is functionally working, but `cloud.metrics agreeRate`
  is misleading for `TASK_POLICY`.
- The current metrics comparison effectively compares the full local/cloud decision strings. That
  marks correct samples as `agree=false` because the local oracle includes `phase=...` while the
  strict cloud decision includes `reason=dev-local-task-policy`.
- This is a metrics/reporting bug, not a task behavior bug. It should not change task phase
  execution, fallback, yield handling, stop handling, or sidecar response semantics.

Goal:

- Make `TASK_POLICY` agreement semantic: compare only the policy fields that represent behavior:
  `result`, `yield`, and `next`.
- Ignore non-behavioral diagnostic differences such as local `phase` and cloud `reason` for agree
  calculation.
- Preserve the raw `cloud.decision` log strings exactly enough for manual debugging.

Acceptance:

- Focused tests prove that:
  - local `phase=A;result=X;yield=Y;next=Z` and cloud `result=X;yield=Y;next=Z;reason=...`
    count as agree;
  - different `result`, `yield`, or `next` counts as disagree;
  - non-`TASK_POLICY` services keep their existing agreement behavior unless they already have a
    dedicated semantic comparator.
- Fresh runtime after restart should show `TASK_POLICY mode=EXECUTE success=true` with a meaningful
  nonzero/positive `agreeRate` when cloud mirrors local behavior.

Implementation boundaries:

- Worker owns metrics/coordinator/task-policy agreement parsing and focused tests only.
- Do not change 五倍/修罗 phase outcome consumption, `TaskTransactionRunner`, cloud task-policy
  execute gate, sidecar response contract, or route/link/NPC/dialog behavior.
- Main agent acts only as manager/reviewer.

Suggested touch areas:

- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionMetricsService.java`
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java` only if the comparator
  belongs at coordinator/result level.
- Existing/focused cloud metrics tests under `src/test/java/com/bot/dhxy/cloud/`.

Verification:

```powershell
mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest,TaskPolicyCloudDecisionServiceTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Worker implementation result (2026-07-01):

- Added a `TASK_POLICY`-specific agreement comparator in
  `CloudDecisionCoordinator`. The comparator parses semicolon `key=value` decision strings and
  compares only `result`, `yield`, and `next`.
- Local diagnostic `phase=...` and cloud diagnostic `reason=...` no longer make semantically identical
  task-policy decisions disagree.
- Non-`TASK_POLICY` services still use the existing raw string equality agreement behavior.
- Raw `localDecision`, `cloudDecision`, and `effectiveDecision` strings are still logged unchanged; only
  the computed `agree` boolean and downstream metrics agreement count become semantic for
  `TASK_POLICY`.
- No changes were made to 五倍/修罗 phase execution, `TaskTransactionRunner`, task-policy execute gate,
  sidecar contract, route/link/NPC/dialog behavior, OCR/template/click/navigation logic, or runtime task
  flow.

Worker verification:

- RED first:
  - `mvn -q -Dtest="CloudDecisionCoordinatorTest" test` failed as expected on
    `TASK_POLICY semantic agreement expected=true actual=false`.
- GREEN focused:
  - `mvn -q -Dtest="CloudDecisionCoordinatorTest" test` passed after the comparator change.
  - Isolated `javac/java` run passed for `CloudDecisionCoordinatorTest`.
  - Isolated `javac/java` run passed for `CloudDecisionMetricsServiceTest`.
  - Isolated `javac/java` run passed for `TaskPolicyCloudDecisionServiceTest`.
- Full Maven verification:
  - `mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest,TaskPolicyCloudDecisionServiceTest" test`
    passed after the transient parallel CR-HC-016 constructor churn settled.
  - `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test` passed.
  - `mvn -q -DskipTests compile` passed.

Fresh runtime gate:

- Restart DHXY and sidecar.
- Run a few 五倍/修罗 turns.
- `TASK_POLICY` metrics should no longer report artificial `agreeRate=0` when effective
  `result/yield/next` matches local.

## 23. CR-HC-016 RouteMemory Cloud Authority / Outcome Ingest

```text
Worker implemented / main verification passed / fresh runtime pending
```

Problem:

- `ROUTE_MEMORY` execute currently moves the route-row click decision cloud-primary, but durable
  route-memory success/failure ownership is still local.
- Route memory is a route asset. If the local long-lived memory table eventually covers enough
  `fromMap -> targetMap -> click` pairs, leaking that table lets a copied client bypass much of the
  yellow-route recognition/candidate-routing work.
- Fresh runtime also showed `world-map-route-memory-pending` being counted as a failed
  `ROUTE_MEMORY` execute decision because it is a lifecycle/accounting event (`pending=CREATED`) but
  the current route execute gate only recognizes lookup decisions (`lookup=HIT/MISS/DISABLED`).

Target product decision:

- Cloud is the authoritative owner for durable route memory.
- Local Runner stays local and observes arrival/timeout/pathing facts.
- Local runtime should not use a local long-lived full route-memory table as product authority once
  this surface is migrated.
- Cloud memory miss/error is not fatal; it is treated as "no usable memory" and falls through to the
  existing non-memory route candidate/OCR/search path.

Target flow:

```text
client asks cloud: fromMap + targetMap + routeMode
-> cloud returns HIT + window-relative click + routeDecisionId, or MISS/DISABLED/ERROR
-> local executor clicks when HIT; otherwise continue non-memory route search/candidate
-> local Runner observes result for the pathing intent
-> Runner reports outcome to cloud with routeDecisionId / intentId / observed map-coordinate facts
-> cloud records success/failure/clean/dirty as the durable route memory authority
```

Required behavior:

- `ROUTE_MEMORY` lookup execute:
  - `HIT` with valid window-relative click executes the existing local click/mini-map/pathing
    confirmation path.
  - `MISS`, `DISABLED`, timeout, invalid schema, or invalid click does not block navigation; it
    falls through to the existing non-memory route candidate/OCR/search path.
- Outcome ingest:
  - Runner reports success/failure only for a recent cloud-issued `routeDecisionId`.
  - Include `intentId`, `fromMap`, `targetMap`, `routeMode`, cloud click, observed current
    map/coordinate, result, elapsed time, and reason.
  - Make the client-side report idempotent for duplicate runner observations.
- Learning from non-memory route search:
  - When local non-memory route candidate/OCR/search succeeds, it may report the resulting click and
    outcome to cloud as a learn candidate. Cloud decides whether/when to promote it.
- Local persistence:
  - Do not keep local full route-memory table as the product authority.
  - It is acceptable to keep short-lived in-memory pending tokens and debug logs.
  - If a transition/debug compatibility file remains temporarily, it must not be used as an
    authority ahead of cloud and must be documented as non-product fallback.
- Metrics:
  - `world-map-route-memory-pending` should not be recorded as a failed `ROUTE_MEMORY` execute
    lookup just because it lacks `lookup=...`.
  - Pending/outcome should use a dedicated lifecycle/audit/outcome service id or route-memory
    outcome contract, not the route lookup execute parser.

Server-side safety requirements:

- Do not blindly trust client outcome reports.
- Validate that the outcome references a cloud-issued `routeDecisionId`, matches expected
  from/target/mode/click, arrives within an allowed time window, and is idempotent.
- Keep enough structured audit to later investigate bad route promotions or malicious reports.

Implementation ownership:

- 业务实现必须由 worker 子智能体完成。
- 谢帅/main agent only owns this card, delegation, review, verification, and documentation.
- A separate helper reviewer should inspect this CR before fresh runtime use because it changes route
  asset ownership.

Suggested touch areas:

- `src/main/java/com/bot/dhxy/cloud/task/RouteCloudDecisionService.java`
- `src/main/java/com/bot/dhxy/cloud/task/RouteCloudDecision.java` and any new route-memory outcome
  request/result models.
- `src/main/java/com/bot/dhxy/service/NavigationService.java` only around route-memory lookup,
  pending creation, and cloud learn/outcome hook points.
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java` only around settling pending
  world-map route result memory.
- Local dev sidecar / test endpoint for route-memory lookup and outcome ingest.
- Focused tests under `src/test/java/com/bot/dhxy/cloud/`, `src/test/java/com/bot/dhxy/service/`,
  and `src/test/java/com/bot/dhxy/window/execution/` as needed.

Do not change in this CR:

- Tracker-link ranker, task classifier, task policy, NPC click, dialog, battle, return item,
  team maintenance, summon-skill queue, input queue, pause/resume reconciliation, OCR/template
  thresholds, or minimap/world-map coordinate conversion algorithms.
- Do not add testcase-dependent visual/click algorithm changes unless separately approved and
  replay-verified under the visual matching rule.

Verification:

```powershell
mvn -q -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest,CloudDecisionDevServerTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

Fresh runtime gate:

- Restart DHXY and the local dev sidecar after implementation.
- Run 五倍 and 修罗 route paths.
- Logs should show cloud route-memory lookup execute, outcome/report samples from Runner, and no
  false failed `ROUTE_MEMORY` execute metric for `world-map-route-memory-pending`.
- If cloud memory is unavailable or returns miss/error, navigation must continue through the
  existing non-memory route candidate/OCR/search path.

Worker B implementation result (2026-07-01):

- Added `routeDecisionId` to `RouteCloudDecision` and the short-lived
  `WorldMapRouteResultPendingMemory` token. Cloud route-memory HITs now carry the cloud-issued
  decision id into Runner settlement; local/non-memory route clicks clear that id.
- `ROUTE_MEMORY` execute now treats `lookup=ERROR` like `MISS` / `DISABLED`: accepted no-click
  fallthrough instead of fatal navigation failure. Invalid HIT schema/click remains rejected no-click.
- Removed the old `shadowRouteMemory(... phase=world-map-route-memory-pending ...)` call from pending
  creation. Pending is now lifecycle state only and no longer enters `ROUTE_MEMORY` lookup execute
  metrics.
- Added `RouteMemoryOutcomeReport` / `RouteMemoryOutcomeIngestResult` and
  `RouteCloudDecisionService.reportRouteMemoryOutcome(...)`. Outcome ingest posts to
  `/api/cloud/route-memory/outcome`, is idempotent by route/id/intent/result key, and never throws
  back into task flow.
- `WindowTaskRunner` reports watcher-settled world-map route memory outcomes after local
  `MemoryService` success/failure recording. Success/failure reports require a cloud
  `routeDecisionId`; local non-memory successful route clicks are reported only as `LEARN_CANDIDATE`.
- Local dev sidecar now returns a `routeDecisionId` for `ROUTE_MEMORY` HIT and accepts idempotent
  route-memory outcome ingest in memory.
- Local full route-memory persistence remains present for debug/compat migration, but cloud
  miss/error/no-click no longer falls back to local long-lived memory as authority in the execute
  path; it falls through to existing non-memory route candidate/OCR/search.

Worker B verification:

```powershell
mvn -q -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest,CloudDecisionDevServerTest,WindowTaskRunnerRouteMemoryOutcomeWiringTest" test
mvn -q -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest,CloudDecisionDevServerTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- Initial RED failed on missing `routeDecisionId`, missing outcome report/result model, missing dev
  sidecar outcome count/endpoint, and the old service constructor.
- After implementation, all four commands exited `0`.

Main/helper review result and repair (2026-07-01):

```text
Repair completed; fresh runtime still pending
```

- P1 blocker: `ROUTE_MEMORY lookup=HIT` currently requires a valid cloud click but does not require
  a cloud-issued `routeDecisionId`. That means the client can execute a cloud memory click and later
  Runner has no id to report success/failure outcome back to cloud. `HIT` must require both a valid
  window-relative click and `routeDecisionId`; missing id should become invalid/no-click fallthrough.
- P1 blocker: early cloud memory HIT failures before Runner settlement, such as destination mini-map
  not opening or mini-map handoff not being confirmed, are recorded locally but do not send a cloud
  failure outcome. Those failures must carry the cloud `routeDecisionId` and report `FAILURE` with
  the concrete reason, without removing the existing local failure record.
- P2 boundary to keep explicit: local persistent route memory may remain temporarily for debug /
  migration compatibility, but in `ROUTE_MEMORY execute=100` with cloud available it cannot act as
  the product authority. Cloud `MISS` / `DISABLED` / `ERROR` / invalid no-click must fall through to
  the non-memory route candidate/OCR/search path, not silently click local clean memory.
- P3 cleanup: client-side outcome idempotency should be atomic or recorded as a follow-up. The dev
  sidecar already deduplicates, but the client currently separates duplicate check and successful
  submission marking.

Repair notes:

- `RouteCloudDecisionService.parseMemory(...)` now rejects `ROUTE_MEMORY lookup=HIT` unless the cloud
  response includes both a valid window-relative `click` and a nonblank `routeDecisionId`. Missing id
  becomes invalid/no-click fallthrough with reason
  `routeDecisionId is required when ROUTE_MEMORY lookup=HIT`; Navigation must not execute that cloud
  click.
- Yellow route-memory early failures after a cloud HIT now keep the existing local
  `MemoryService.recordWorldMapRouteResultFailure(...)` behavior and additionally report a cloud
  `FAILURE` outcome with `routeDecisionId`, click, observed map/coordinate, elapsed time, and the
  concrete reason such as `mini-map-panel-not-visible` or `handoff-not-confirmed:*`.
- Local persistent route memory remains only compatibility/debug migration state for this phase. When
  route-memory execute is active and cloud returns `MISS` / `DISABLED` / `ERROR` / invalid no-click,
  Navigation falls through to non-memory route candidate/OCR/search instead of letting local clean
  memory silently become product authority.
- Client-side outcome dedupe is now atomic add-before-send with rollback on disabled transport,
  serialization failure, HTTP failure, timeout, interruption, or IO failure.

Legacy clean memory migration repair (2026-07-01):

- Fresh runtime at `21:24` showed a local legacy clean `灵兽村 -> 洛阳城` yellow route-memory HIT
  (`clean=true`, `successCount=9`) whose local JSON row had no cloud `routeDecisionId`. The execute
  gate correctly rejected the echoed `ROUTE_MEMORY lookup=HIT` with
  `routeDecisionId is required when ROUTE_MEMORY lookup=HIT`, then navigation fell back to
  `ROUTE_CANDIDATE`.
- The safety rule is preserved: `RouteCloudDecisionService.parseMemory(...)` still requires
  `routeDecisionId` for every executable `ROUTE_MEMORY lookup=HIT`.
- Added `RouteMemoryMigrationRequest` / `RouteMemoryMigrationResult` and
  `RouteCloudDecisionService.migrateLegacyRouteMemory(...)`. The client submits legacy clean facts
  (`fromMap`, `targetMap`, `routeMode`, window-relative click, `matchedText`, local success/failure,
  `clean`, and local `source`) to `/api/cloud/route-memory/migrate`.
- `NavigationService` now tries that migration only when a local clean route-memory HIT is rejected
  specifically because the cloud execute response lacks `routeDecisionId`. Migration success creates
  a cloud-executed decision with the returned `routeDecisionId` and click, so the same navigation can
  continue through the existing yellow memory fast path and later outcome reporting. Migration
  failure, cloud unavailable, invalid click, or no id keeps the safe fallback to non-memory
  route-candidate/OCR/search and does not execute the old local memory as authority.
- The local dev sidecar now supports `/api/cloud/route-memory/migrate` and returns a deterministic
  dev `routeDecisionId` plus executable `lookup=HIT` decision for local testing.
- Added explicit logs:
  `legacy route memory migration submitted`,
  `legacy route memory migration succeeded`, and
  `legacy route memory migration failed`, including from/target/mode/click and `routeDecisionId`
  when available.

Repair verification:

```powershell
mvn -q -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest,CloudDecisionDevServerTest,WindowTaskRunnerRouteMemoryOutcomeWiringTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
```

- Worker verification: all three commands exited `0`.
- Main thread verification:
  - `mvn -q -Dtest="RouteCloudDecisionServiceTest,NavigationRuntimeDecisionShadowWiringTest,CloudDecisionDevServerTest,WindowTaskRunnerRouteMemoryOutcomeWiringTest" test` exited `0`.
  - `mvn -q -Dtest="CloudDecisionMetricsServiceTest,CloudDecisionCoordinatorTest,TaskPolicyCloudDecisionServiceTest" test` exited `0`.
  - `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test` exited `0`.
  - `mvn -q -DskipTests compile` exited `0`.
  - `git diff --check` exited `0`; output contained only existing LF/CRLF conversion warnings.
- Legacy migration RED/GREEN:
  - RED first:
    `mvn -q -Dtest="RouteCloudDecisionServiceTest,CloudDecisionDevServerTest,NavigationRuntimeDecisionShadowWiringTest" test`
    failed at test-compile on missing `RouteMemoryMigrationRequest`,
    `RouteMemoryMigrationResult`, and `migrateLegacyRouteMemory(...)`.
  - GREEN focused:
    the same command exited `0` after the migration API, dev endpoint, and Navigation wiring were
    implemented.
  - Follow-up verification:
    `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test` exited `0`;
    `mvn -q -DskipTests compile` exited `0`;
    `git diff --check` exited `0` with only existing LF/CRLF conversion warnings.

Additional repair worker result (2026-07-02):

- Review follow-up P2 fixed: legacy green and yellow memory fast paths now require the final
  route-memory decision to be `CLOUD_EXECUTED` before executing the remembered click. If the route
  coordinator is inactive, cloud is disabled, `ROUTE_MEMORY` is shadow-only, or the decision is local
  passthrough/localOnly, Navigation falls through to the existing non-memory route candidate/OCR/search
  path instead of using local clean memory as product authority.
- Review follow-up P2 fixed: yellow memory early failure recording now receives the actual
  `effectiveRelativeClick` used for the click and stores that window-relative click in the local
  pending token and cloud `FAILURE` outcome. This prevents a cloud HIT whose click differs from the
  legacy row from reporting failure for the wrong click.
- The missing-`routeDecisionId` migration path remains narrow: migration is attempted only after a
  cloud no-click rejection whose reason includes `routeDecisionId`. `RouteCloudDecisionService.parseMemory(...)`
  still requires `routeDecisionId` for executable `ROUTE_MEMORY lookup=HIT`.

Additional repair verification:

```powershell
mvn -q -Dtest="NavigationRuntimeDecisionShadowWiringTest" test
mvn -q -Dtest="RouteCloudDecisionServiceTest,CloudDecisionDevServerTest,NavigationRuntimeDecisionShadowWiringTest" test
mvn -q -DskipTests compile
git diff --check
```

- RED first: `NavigationRuntimeDecisionShadowWiringTest` failed on the new cloud-executed authority
  source guard.
- GREEN: all commands above exited `0`; `git diff --check` produced only existing LF/CRLF warnings.

Dev sidecar learn-loop repair (2026-07-02):

- Fresh runtime evidence showed `ROUTE_CANDIDATE mode=EXECUTE` succeeded and Runner submitted
  `cloud.route-memory.outcome ... result=LEARN_CANDIDATE`, but subsequent same-key
  `ROUTE_MEMORY` lookups stayed `MISS`.
- Root cause: local dev sidecar `/api/cloud/route-memory/outcome` stored reports only in the
  idempotency map `routeMemoryOutcomes`; it did not promote valid `LEARN_CANDIDATE` reports into a
  lookup table that can return future `ROUTE_MEMORY HIT`.
- `CloudDecisionDevServer` now keeps an in-memory learned route table keyed by
  `fromMap + targetMap + routeMode`. The first valid learn candidate stores a deterministic
  `routeDecisionId`, window-relative click, and `reason=dev-route-memory-learned`.
- Later same-key `ROUTE_MEMORY` lookup returns
  `lookup=HIT;routeMode=...;routeDecisionId=...;click=x,y;reason=dev-route-memory-learned`.
- Existing safety boundary is preserved: `SUCCESS` / `FAILURE` outcome ingest still requires
  `routeDecisionId`; invalid learn candidates are accepted as ignored and do not create a HIT.
- RED/GREEN:
  - RED first: `mvn -q -Dtest="CloudDecisionDevServerTest" test` failed because the lookup after
    `LEARN_CANDIDATE` still returned `MISS`.
  - GREEN: `mvn -q -Dtest="CloudDecisionDevServerTest" test`,
    `mvn -q -Dtest="CloudDecisionDevServerTest,RouteCloudDecisionServiceTest" test`,
    `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test`, `mvn -q -DskipTests compile`, and
    `git diff --check` all exited `0`; diff-check only emitted existing LF/CRLF warnings.

Fresh runtime acceptance remains pending:

- Restart DHXY and the dev sidecar.
- Run 五倍/修罗 route paths and confirm logs show route-memory lookup execute plus
  `cloud route-memory outcome report`.
- For the dev sidecar learning loop, after a non-memory route candidate succeeds and logs
  `cloud.route-memory.outcome ... result=LEARN_CANDIDATE`, run the same
  `fromMap + targetMap + routeMode` again in the same sidecar process. The second route should show
  `ROUTE_MEMORY lookup=HIT routeDecisionId=... click=... reason=dev-route-memory-learned`.
- For the legacy clean row case, confirm logs show:
  `routeDecisionId is required when ROUTE_MEMORY lookup=HIT`,
  then `legacy route memory migration submitted`,
  then either `legacy route memory migration succeeded ... routeDecisionId=...` followed by the
  yellow memory fast path continuing, or `legacy route memory migration failed` followed by
  non-memory route-candidate/OCR fallback.
- In cloud disabled / `ROUTE_MEMORY` shadow-only / service inactive cases, confirm legacy green/yellow
  memory logs show `fast path skipped because cloud route memory was not executed`, followed by
  non-memory route candidate/OCR/search rather than a local clean memory click.
- For a cloud HIT whose click differs from the legacy entry click, force an early yellow memory failure
  and confirm `cloud.route-memory.outcome submitted` carries the cloud/effective click coordinates.
- Confirm no failed `ROUTE_MEMORY` execute metric is produced for pending lifecycle creation.

## 24. CR-HC-017 DialogPolicy Cloud Execute Candidate Gate

```text
Implemented / focused tests passed / fresh runtime pending
```

Decision:

- 下一步迁移 `DIALOG_POLICY`，从 shadow 升级到可执行 execute。
- 选择它优先于 `NPC_CLICK_STRATEGY`，因为 dialog 的第一版边界可以做得更小：云端只选择本地已构造/已验证的业务策略或候选，不直接控制截图识别、裸坐标、输入队列或点击算法。
- 本卡不等待 CR-HC-016 fresh runtime 单独验证；后续 live run 一起验证 route-memory 和 dialog execute 证据。

Service id:

```text
DIALOG_POLICY
```

Hook point:

- `DialogService.handleDialog(...)` / `finishRequest(...)` 现有 shadow 上报点。
- 第一版 execute gate 应尽量复用当前 `finishRequest(...)` 附近的 local result/context，不重排整个 `handleDialog(...)` 大流程。

Required behavior:

- 云端只允许返回“本地候选中的一个安全 action / policy / status decision”，不能返回未验证裸坐标。
- 本地仍负责：
  - dialog 截图和类型检测；
  - green/template/keyword/remembered/business/fallback option 的具体识别；
  - 输入队列和真实点击；
  - pause/stop checkpoint；
  - remembered option / item-give / route-transfer 的本地安全校验。
- 云端超时、失败、空响应、schema mismatch、低置信、候选 id 不存在、action 与当前 request 不匹配时，必须 fallback 到本地原 `DialogResult`。
- 默认配置必须保持可回滚；execute 可以接线，但服务开关必须独立：

```properties
cloud.services.dialog-policy.shadow-enabled=true
cloud.services.dialog-policy.execute-enabled=false
cloud.services.dialog-policy.execute-percent=0
cloud.services.dialog-policy.fallback=LOCAL
```

Execute acceptance for this card:

- 当用户后续把 `dialog-policy.execute-enabled=true` 且 `execute-percent=100` 打开时，云端有效响应可以成为 `effectiveDecision`。
- 即使 execute 打开，云端也只能选择本地候选集合中的 action；不能让本地产生新的 OCR/template/click 坐标算法。
- 如果云端选择无效，日志必须明确 `cloud.execute serviceId=DIALOG_POLICY accepted=false rejectReason=...`，并继续本地结果。
- 如果云端选择有效，日志必须明确：

```text
cloud.decision serviceId=DIALOG_POLICY mode=EXECUTE ... executed=true ...
dialog cloud policy accepted ...
```

Request / response contract:

- Request context 至少包含：
  - `sourceTask`
  - `operation`
  - `storyPolicy`
  - `optionPolicy`
  - `fallbackPolicy`
  - `verifyDialogType`
  - `targetKeyword`
  - local result `status/type/actionKey/clicked`
  - candidate/action list if the hook can safely expose it
- Response decision 第一版建议使用 key/value string，例如：

```text
action=USE_LOCAL_RESULT;reason=...
action=SELECT_CANDIDATE;candidateId=...;reason=...
action=REJECT;reason=...
```

Current `DIALOG_POLICY` pre-click request context addendum (2026-07-02):

- Covered dialog click policies must use `phase=dialog-pre-click-option` before local option click code.
- Covered production click/interaction policies are: keyword / route transfer, remembered point, business option,
  give-item, green template, fallback first/last, and story click-through when the local read-only detector observed
  `detectedDialogType=STORY`. Cloud miss/timeout/invalid/low-confidence/no-action/abort/request-new-screenshot is
  fail closed; after-local `USE_LOCAL_RESULT` is not a covered click success path.
- Prepared dialog click actions are covered by the same boundary. `prepareRouteKeywordOption`,
  `prepareRememberedRouteOption`, `prepareRememberedChoiceOption`, and `prepareGreenTemplateOption` may only save a
  `PreparedDialogAction` from a validated `DIALOG_POLICY` `WINDOW_RELATIVE click`; local OCR, remembered coordinates,
  template matches, fallback order, or story-click geometry must not decide prepared `absoluteX/Y`.
- Local verify-only template checks may remain only when they do not click, do not build `PreparedDialogAction`, and
  do not feed a covered click/prepared-click success path. Current allowed examples are `VERIFY_GREEN_TEMPLATE` and
  `VERIFY_WHITE_TEMPLATE` visibility checks.
- `CLICK_GREEN_TEMPLATE` must not be represented only by legacy `greenTemplateSpecs=name@path`; DHXY sends the full
  local `GreenTemplateClickSpec` click contract as flat context fields for compatibility/diagnostics:
  - `greenTemplateSpecCount=<n>`
  - `greenTemplateSpecNames=<name0>|<name1>|...`
  - `greenTemplateSpec.N.name=<spec.name>`
  - `greenTemplateSpec.N.templatePath=<spec.templatePath>`
  - `greenTemplateSpec.N.minOffsetX=<screen-pixel offset range min>`
  - `greenTemplateSpec.N.maxOffsetX=<screen-pixel offset range max>`
  - `greenTemplateSpec.N.randomRadiusY=<screen-pixel y random radius>`
  - deterministic range only: `greenTemplateSpec.N.clickOffsetX=<x>` and
    `greenTemplateSpec.N.clickOffsetY=0`
- Machine-readable summaries `greenTemplateSpecs` and `targetKeywordTemplateSpecs` are JSON object arrays containing
  `name`, `templatePath`, offset/random fields, and deterministic `clickOffsetX/clickOffsetY` where applicable.
  The external parser also accepts old `name@path[minOffsetX=...]` entries and strips bracket suffix before path parsing.
- DHXY also sends `detectedDialogType`, `rememberedRelativeX`, and `rememberedRelativeY`. External `DIALOG_POLICY`
  must return explicit action metadata:
  - `STORY_CLICK_THROUGH` for observed story click-through;
  - `targetKeyword` / remembered action key for keyword or remembered-point clicks;
  - `heal-pet`, `repair-equipment`, or `repair-equipment-giveup` for business;
  - `itemToGive` for give-item;
  - `GreenTemplateClickSpec.name` for green/template actions;
  - `FALLBACK_FIRST_OPTION` / `FALLBACK_LAST_OPTION` for fallback green-band actions.
- For `CLICK_KEYWORD` / `ROUTE_TRANSFER` without `targetKeywordTemplateSpecs`, DHXY sends
  `targetKeywordAliases`. The external brain must perform cloud-side option text recognition/matching from the raw
  ROI and return `actionId=<targetKeyword>` plus a window-relative click. DHXY must not execute
  `handleKeywordOption(...)` / route retry as a production fallback when this cloud recognizer misses.
- The external brain owns template recognition, spec selection, offset/randomization policy, and final click selection.
  DHXY only validates the returned plain-left `WINDOW_RELATIVE click` and `candidateBox` are inside the window/ROI,
  then executes the click through the input queue.

Worker implementation constraints:

- 业务实现必须由 worker 子智能体完成；谢帅/main agent 只维护本卡、派工、review、跑验证。
- Worker 必须先写 RED 测试，再写生产代码。
- 不允许改 NPC click、tracker link、route memory、navigation、五倍/修罗 phase、战斗、队伍维护、三技能队列、模板阈值、OCR 洗图。
- 不允许把 `RuntimeDecisionShadowService` 直接改成会影响所有 shadow hook 的 execute 消费层，除非有明确测试证明其他 shadow service 仍不消费云端结果。
- 如果需要新增 `DialogPolicyCloudDecisionService`，它必须只服务 `DIALOG_POLICY`，避免扩大到 `NPC_CLICK_STRATEGY`。

Suggested touch areas:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/cloud/task/DialogPolicyCloudDecisionService.java` 或等价窄服务
- `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionServiceId.java` / execute allowlist 相关测试
- `src/test/java/com/bot/dhxy/cloud/runtime/InteractionShadowWiringTest.java`
- 新增 focused tests under `src/test/java/com/bot/dhxy/cloud/...`
- Local dev sidecar only if needed to echo valid/invalid dialog decisions for tests

Required tests:

```powershell
mvn -q -Dtest="DialogPolicyCloudDecisionServiceTest,InteractionShadowWiringTest" test
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests compile
git diff --check
```

Fresh runtime gate:

- Restart DHXY and the dev sidecar after implementation.
- Run 五倍 + 修罗 dialog-heavy paths.
- Confirm logs show `DIALOG_POLICY` execute/shadow samples with `elapsedMs`, `executed`, `accepted/rejectReason`, and that invalid/timeout cloud decisions do not change local dialog behavior.
- For visual/click-sensitive changes, if worker changes where dialog clicks happen rather than only selecting local candidates, testcase replay is mandatory before fresh runtime.

Implementation result:

- Added `DialogPolicyCloudDecisionService` as a DIALOG_POLICY-only execute gate on top of
  `CloudDecisionCoordinator.shadow(..., CloudDecisionExecutionGate)`.
- Added `DialogPolicyCloudDecision` as the narrow result envelope. The effective dialog result can only be the
  original local `DialogResult` / local safe candidate; cloud responses cannot produce raw click coordinates or input
  actions.
- `DialogService.finishRequest(...)` now keeps the existing local dialog result first, then calls the dedicated
  `dialogPolicyCloudDecisionService.decide(request, result)` before final logging/return. No `handleDialog(...)`
  branch order, OCR/template matching method, fallback click method, or click coordinate algorithm was changed by this
  worker.
- DIALOG_POLICY execute validation accepts only:
  - `action=USE_LOCAL_RESULT`
  - `action=SELECT_CANDIDATE;candidateId=local-result`
  - `action=REJECT` as an explicit cloud rejection that keeps the local result
- The gate rejects and falls back to local on invalid candidate, unknown action, raw coordinate fields
  (`click`, `relativeClick`, `absoluteClick`, `coordinateSpace`, etc.), operation/option-policy/action-key/status/type
  mismatch, low confidence, timeout/client failure, empty response, and schema mismatch.
- `RuntimeDecisionShadowService` remains a fire-and-forget `void shadow(...)` helper and was not turned into a global
  execute consumption layer.
- `CloudDecisionCoordinator.EXECUTABLE_SERVICES` still contains only `TASK_CLASSIFIER`; DIALOG_POLICY execute is only
  reachable through the dedicated service-specific `CloudDecisionExecutionGate`.
- `application.properties` remains rollback-safe:
  `cloud.services.dialog-policy.execute-enabled=false`, `execute-percent=0`, and
  `cloud.services.npc-click-strategy.execute-enabled=false`.
- Dev sidecar default `DIALOG_POLICY` response now returns
  `action=USE_LOCAL_RESULT;operation=<context/local operation>;reason=dev-local-dialog-policy`, so a local fresh run
  can produce valid execute samples after the user explicitly enables dialog-policy execute.

RED/GREEN:

- RED first:
  - `mvn -q -Dtest="DialogPolicyCloudDecisionServiceTest,InteractionShadowWiringTest" test` failed at test compile
    because `DialogPolicyCloudDecisionService` did not exist.
  - `mvn -q -Dtest="CloudDecisionDevServerTest#junitRunsMainSuite" test` failed because the dev sidecar still echoed
    local DIALOG_POLICY decision text instead of returning `action=USE_LOCAL_RESULT`.
- GREEN:
  - `mvn -q -Dtest="DialogPolicyCloudDecisionServiceTest,InteractionShadowWiringTest" test` exited `0`.
  - `mvn -q -Dtest="CloudDecisionDevServerTest#junitRunsMainSuite" test` exited `0`.
  - `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test` exited `0`.
  - `mvn -q -DskipTests compile` exited `0`.
  - `git diff --check` exited `0`; output contained only existing LF/CRLF warnings and no whitespace errors.

Fresh runtime acceptance remains pending:

- Restart DHXY and the dev sidecar.
- Keep default config closed unless intentionally testing:
  `cloud.services.dialog-policy.execute-enabled=false` and `execute-percent=0`.
- For the execute test, temporarily set `dialog-policy.execute-enabled=true` and `execute-percent=100`, then run 五倍
  and 修罗 dialog-heavy paths.
- Confirm logs contain both accepted and rejected DIALOG_POLICY evidence as applicable:
  `cloud.decision serviceId=DIALOG_POLICY mode=EXECUTE ... executed=true ...`,
  `dialog cloud policy accepted ...`, and
  `cloud.execute serviceId=DIALOG_POLICY accepted=false rejectReason=...` for invalid/timeout/schema mismatch cases.
- Confirm invalid/timeout cloud decisions do not change local dialog behavior.

Current CR167 Dialog boundary update 2026-07-02:

- The earlier after-local `DIALOG_POLICY` note above is historical only for diagnostics. Covered Dialog click/prepare
  production paths now require pre-click/prepared `DIALOG_POLICY` authority before any action can succeed.
- Local production code may capture raw dialog ROI, build request context, validate cloud response, persist
  `PreparedDialogAction`, and execute allowlisted input. It must not choose option click point, prepared click point,
  fallback order, remembered-vs-green order, route keyword candidate, story click-through target, or white-story
  matched/miss/absent semantic.
- `WubeiDialogPreparationProvider` sends one composite accept prepared request containing remembered hint and green
  specs; external brain decides remembered vs green or no-action.
- `DialogService.prepareCloudWhiteStoryTemplateOrAbsent(...)` sends white specs plus miss/absent metadata to cloud; external
  brain returns a no-click semantic actionId and candidateBox, or fail-closed. Local `verifyWhiteStoryTemplate(...)`
  remains read-only only and is not a production prepared semantic path.
- Source/verification gate for this pass:
  - old local prepared/click handlers `prepareOptionKeywordWithOcr`, `readDialogOptionWords(`,
    `buildPreparedDialogAction(`, `buildRememberedPreparedDialogAction`, `handleStoryDialog(`,
    `fastClickStoryDialogDirect(` are absent from `DialogService.java`;
  - `mvn -q -DskipTests compile` passed;
  - `mvn -q -Dtest="TaskMaintenanceCloudRequiredFailureTest,DialogCloudPreClickWiringGuardTest,DialogPolicyCloudPreClickDecisionServiceTest" test`
    passed;
  - external `dhxy-cloud-brain`
    `mvn -q -Dtest="AlgorithmFailClosedTest,CloudBrainSmokeTest" test` passed.

## 25. CR-HC-018 Cloud-Required Execute Wave

```text
Source implemented / local verification passed / fresh runtime pending
```

Decision:

- 用户明确调整推进方式：不再一项一项 shadow/灰度后让用户反复重启测试。
- 当前分支还不是生产发布形态，可以接受云端 execute 暴露问题；目标是在一次 live run 中看见所有已决定云端化能力的真实执行证据。
- 本卡把策略改为 `cloud-required execute wave`：
  - 云端是权威决策源；
  - 本地原逻辑只做 shadow comparator / diagnostic oracle；
  - 不允许 cloud execute 失败后静默 fallback 到本地并继续执行；
  - 失败要以明确 no-action / failed outcome / task failure log 暴露出来，方便从日志定位具体 service。

Wave service groups:

```text
TASK_CLASSIFIER
TASK_POLICY
TASK_RECOVERY
TRACKER_LINK_RANKER
ROUTE_CANDIDATE
ROUTE_MEMORY
NPC_CLICK_STRATEGY
DIALOG_POLICY
CAPABILITY_GATE
MAINTENANCE_THRESHOLD
TEAM_RETURN_POLICY
FAILURE_CLASSIFIER
FEATURE_FLAG
POLICY_VERSION
```

Deferred from this wave:

- `MAP_TRANSFORM_ASSET`, `SIGNED_ASSET_BUNDLE`, full `LEARNED_MEMORY` asset delivery, and template/image bundle delivery.
- Reason: those are asset distribution/versioning systems, not the same runtime decision execute contract. `ROUTE_MEMORY`
  learn/outcome may continue because it already has an HTTP outcome/migration path.

Cloud-required contract:

- In execute mode with required fallback, a service must not return a local effective action when:
  - cloud endpoint is unavailable;
  - cloud response is empty;
  - schema/trace/service mismatch happens;
  - service-specific execute gate rejects the response;
  - cloud returns an invalid candidate, invalid coordinate space, out-of-ROI click, or low confidence.
- The local decision must still be sent/logged as `localDecision` / context so logs can compare local vs cloud.
- Local comparator may be used for `agree`, metrics, and debugging only; it must not become the action source in this wave.
- Physical execution remains local:
  - screenshots / OCR / template reads stay local;
  - input queue stays local;
  - Runner/watcher state and stop/pause remain local;
  - cloud may choose policy/candidate/click coordinates only through service-specific safe gates.

Configuration target for this branch:

```properties
cloud.enabled=true
cloud.real-transport-enabled=true
cloud.default-fallback=STOP

cloud.services.<wave-service>.shadow-enabled=true
cloud.services.<wave-service>.execute-enabled=true
cloud.services.<wave-service>.execute-percent=100
cloud.services.<wave-service>.fallback=STOP
```

The only exceptions are diagnostic-only services where the implementation explicitly has no behavior to change
(`FEATURE_FLAG`, `POLICY_VERSION`, and some `FAILURE_CLASSIFIER` samples). Even for those, the service must log
cloud-required status clearly instead of pretending it controlled behavior.

Worker split:

1. Foundation worker
   - Owns common cloud-required semantics, config profile, dev sidecar response coverage, and source guards.
   - Must make it easy for dedicated service gates to detect "cloud required but not executed".
   - Must not widen the generic coordinator execute allowlist in a way that bypasses service-specific gates.

   Implementation note:
   - `CloudDecisionResult.isRequiredExecuteFailure()` is the common foundation signal for `mode=EXECUTE`,
     `fallback=STOP`, and `executed=false`.
   - On required execute failure, `CloudDecisionCoordinator` keeps `localDecision` as comparator/log evidence but
     sets `effectiveDecision=null`, so callers cannot accidentally treat STOP failure as local fallback.
   - `CloudDecisionCoordinator.EXECUTABLE_SERVICES` remains narrow (`TASK_CLASSIFIER` only); other executable
     services must use service-specific `CloudDecisionExecutionGate` checks.
   - Branch config now sets every wave service to `shadow=true`, `execute=true`, `execute-percent=100`, and
     `fallback=STOP`; physical input/screenshot/OCR/template behavior remains local and guarded in Java.
   - Dev sidecar/source guards cover schema-valid default responses for every wave service and prevent generic
     execute allowlist widening.

2. Task-flow worker
   - Owns `TASK_CLASSIFIER`, `TASK_POLICY`, and `TASK_RECOVERY`.
   - Must ensure task classifier and task phase/recovery decisions do not silently use local results when required cloud execute fails.
   - Must preserve local STOP/pause safety and task checkpoint behavior.

   Implementation note:
   - `TASK_CLASSIFIER` required execute failure and unsupported executed task keys now return not-found/empty,
     not the local found task. Accepted supported task keys may replace the local classifier while preserving
     local OCR/template evidence.
   - `TASK_POLICY` required execute failure or STOP-mode gate rejection now returns an explicit
     `CLOUD_REQUIRED_FAILURE` outcome with terminal phase `FAILED`, `RETRYABLE_ERROR`, and `MUST_YIELD`.
     Local task STOP/pause safety remains local passthrough.
   - `TASK_RECOVERY` now has a dedicated execute gate. Cloud may only authorize the local recovery candidate
     action and next phase; unavailable, invalid, mismatched, unsupported, or required-failure decisions produce
     no recovery.
   - 五倍 and 修罗 recovery wrappers now require `TASK_RECOVERY` cloud acceptance before local retry/restart/recover
     code runs. Without acceptance they return terminal failed task state/outcome instead of local recovery fallback.
   - Dev sidecar default coverage includes strict `TASK_RECOVERY`
     `action=<local action>;next=<local next>;reason=dev-local-task-recovery` responses.
   - No visual matching, OCR/template matching, route/NPC/dialog click target, input, or click-geometry logic changed.

3. Navigation worker
   - Owns `ROUTE_CANDIDATE`, `ROUTE_MEMORY`, route-memory outcome/learn/migration behavior, and navigation `POLICY_VERSION` diagnostics.
   - Must ensure route candidate/memory click authority is cloud execute only; local route memory/candidate output is comparator only.
   - Must not change map OCR/template/click geometry except through existing cloud route gates.

   Implementation note:
   - `RouteCloudDecision` now distinguishes accepted cloud no-click (`CLOUD_NO_CLICK`) from required execute
     failure/gate rejection (`CLOUD_REJECTED_NO_CLICK`).
   - `ROUTE_MEMORY` `MISS`/`DISABLED`/`ERROR` and `ROUTE_CANDIDATE` `NOT_FOUND`/`SKIP`/`FAILED` are accepted
     cloud no-click decisions. They do not expose a local effective click.
   - `ROUTE_MEMORY` HIT still requires a cloud `routeDecisionId`, a `WINDOW_RELATIVE` click, and route-result ROI
     validation before navigation may click.
   - `NavigationService` route candidate paths now require `CLOUD_EXECUTED` before executing any local OCR/黄字 click.
     `LOCAL_PASSTHROUGH`, invalid, unavailable, rejected, and accepted no-click decisions become no-click/failure.
   - Legacy clean route memory may still be sent as migration facts, but migration no longer turns local clean memory
     into current-click authority in the same navigation attempt.
   - Navigation `POLICY_VERSION` remains diagnostic-only and logs/sends that it does not control navigation behavior.

4. Interaction worker
   - Owns `TRACKER_LINK_RANKER`, `NPC_CLICK_STRATEGY`, and `DIALOG_POLICY`.
   - Must make tracker green-link and NPC/dialog strategy cloud-required.
   - Must keep real input local and guarded; if cloud gives no valid executable candidate/click, expose no-click/failure instead of local fallback.
   - Any new click-coordinate behavior must be covered by existing source guards and, if actual click math changes, testcase replay.

   Implementation note:
   - `TRACKER_LINK_RANKER` now treats `CloudDecisionResult.isRequiredExecuteFailure()` as an explicit
     no-click result, so execute-required/STOP failures cannot fall through to a local tracker-link click.
   - `NPC_CLICK_STRATEGY` has a dedicated strategy gate. Cloud may only authorize the currently selected
     local strategy/candidate (`local-strategy`); it cannot return naked coordinates, ROI changes, input
     instructions, or strategy reordering. Required failure/invalid/timeout stops the smart-click pipeline
     before the next local strategy/input.
   - `DIALOG_POLICY` required failure now returns an effective failed/no-click `DialogResult`. This hook
     remains after the local dialog result is built, so it is a task-visible required failure and does not
     retroactively prevent a click that already happened before `finishRequest(...)`.
   - Dev sidecar default coverage includes `NPC_CLICK_STRATEGY` with
     `action=ALLOW_LOCAL_STRATEGY;candidateId=local-strategy`.
   - No testcase replay was required in this implementation pass because actual click coordinates,
     ROI/template/OCR matching, and geometry math were not changed.

5. Team / maintenance / diagnostics worker
   - Owns `CAPABILITY_GATE`, `MAINTENANCE_THRESHOLD`, `TEAM_RETURN_POLICY`, `FAILURE_CLASSIFIER`, `FEATURE_FLAG`.
   - Must turn local support/session, maintenance thresholds, team-return policy, and failure classification into cloud-required policy decisions where they affect behavior.
   - Must preserve CR138 local-team safety invariants: cloud cannot grant physical input outside the local task-turn/capability window.

   Implementation note:
   - `CAPABILITY_GATE` now uses a dedicated gate where cloud can only narrow local authority:
     `effective allow = localAllowed && cloudAllow`. Local stale/closed/completed/session-disabled
     CR138/CR148 denials remain denials even if cloud returns `ALLOW`; cloud unavailable/invalid/STOP
     required failure denies.
   - `MAINTENANCE_THRESHOLD` now runs before downstream maintenance actions. Cloud `ALLOW` can permit
     the already-local enabled request, `SKIP` / `NO_ACTION` return no-action, and required failure
     returns `TaskMaintenanceStatus.CLOUD_REQUIRED_FAILURE` without invoking dialog/summon maintenance
     actions.
   - `TEAM_RETURN_POLICY` now denies member return click before sheyaoxiang/input submission when cloud
     is unavailable/invalid/required-failed. Leader wait required failure returns false, and leader
     precheck required failure is conclusive (`cloud-required-failure`) instead of an inconclusive
     local continuation.
   - `FEATURE_FLAG` and `FAILURE_CLASSIFIER` remain diagnostic-only at the team/maintenance business
     call sites. The wave may configure them as execute/STOP, but these services do not control
     behavior in `TaskMaintenanceService` or `TeamReturnService`.
   - Verification passed:
     `mvn -q -Dtest="CapabilityGateCloudDecisionServiceTest,TaskMaintenanceCloudRequiredFailureTest,TeamReturnCloudRequiredFailureTest" test`,
     `java -cp 'target/test-classes;target/classes' com.bot.dhxy.cloud.runtime.RuntimeDecisionWorkerDShadowWiringTest`,
     `java -cp 'target/test-classes;target/classes' com.bot.dhxy.cloud.runtime.RuntimeDecisionShadowWaveWiringTest`,
     `mvn -q -Dtest="TaskMaintenanceCR138LocalSupportCapabilityTest,AutoCombatCR138FirstAidOnlyCommonBoxGuardTest,CR148LocalTeamSessionInvalidationWiringTest" test`,
     `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test`, and
     `mvn -q -DskipTests compile`.
   - No WubeiTask, XiuluoTaskV2, NavigationService, NpcClickService, DialogService, input queue
     internals, OCR/template/click geometry, or physical click-coordinate logic was changed.

Review plan:

- Main agent remains manager/reviewer only and does not directly write Java business implementation.
- Dispatch one helper reviewer to audit the whole wave for:
  - no hidden local fallback in execute-required paths;
  - no generic unsafe execute bypass;
  - no input/OCR/template/click geometry changes outside the named gates;
  - config is actually set to execute 100 / STOP for wave services.
- Each worker must write RED tests before production code and report changed files.
- Main thread runs focused tests, cloud package tests, `compile`, dashboard/docs sync if CR cards change, and `git diff --check`.

Review result 2026-07-01:

- Whole-wave helper review first found one P1: 五倍黄袍续战 tracker green paths could still click locally
  without `TRACKER_LINK_RANKER` authority.
- A focused repair worker fixed that P1 by removing the null `cloudDecision` bypass, routing
  `continueChainedCombatFromTracker(...)` and `clickCachedChainedTrackerGreen(...)` through
  `shadowTrackerLinkSelection(...)`, and requiring a cloud-executed window-relative point before input.
- Main verification passed:
  - `mvn -q -DskipTests test-compile`
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.cloud.task.TrackerLinkRankerCloudShadowWiringTest`
  - `mvn -q -Dtest="TrackerLinkRankerCloudShadowServiceTest,InteractionShadowWiringTest" test`
  - `mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test`
  - `mvn -q -DskipTests compile`
  - `mvn -q -Dtest="MainWindowControllerCloudSidecarGateSourceGuardTest,CloudDecisionDevSidecarServiceTest,TaskMaintenanceCR138LocalSupportCapabilityTest,AutoCombatCR138FirstAidOnlyCommonBoxGuardTest,CR148LocalTeamSessionInvalidationWiringTest,TeamReturnPrecheckWiringTest,WubeiCR136FastExitLifecycleWiringTest,XiuluoCR147ReturnItemRetryStateMachineWiringTest,WindowTaskRunnerPostCombatIdleWatchdogWiringTest" test`
  - Scoped `git diff --check` reported only LF/CRLF conversion warnings.
- No testcase replay was required for the P1 repair because it did not change OCR/template/ROI matching or
  local click geometry; it only changed the authority gate for an existing tracker-green click.

Fresh runtime gate:

- User can run one combined 五倍/修罗 test after the wave is complete.
- Expected logs:
  - all wave services appear in `cloud.decision serviceId=... mode=EXECUTE`;
  - authority services show `executed=true` on valid cloud response;
  - invalid/unavailable cloud response shows explicit required failure/no-click/no-action, not local fallback;
  - `localDecision` remains visible for comparison.

## 26. Cloud Asset / Vision Boundary Decisions

```text
Decision recorded / implementation CRs pending
```

User decision 2026-07-02:

- `ImagePreprocessor` 级别的洗字/图像预处理不能留在本地发布 runtime。
- 范围不限于三技能：黄字、绿字、白字、紫字，以及依赖这些 washed image 的模板匹配、
  指纹生成、文字候选抽取，都属于核心资产。
- 本地可以截图、裁剪、上传 raw image / ROI，也可以做窗口相对坐标边界校验和真实输入；
  但本地不能保留可独立复用的洗字服务或生产 fallback。

Corrective operating rule:

- 后续任何云端化 CR 立项前，必须先做“核心资产盘点”，不能只按现有 Java service 边界机械迁移。
- 默认判断应改为：业务策略、视觉识别、文字清洗、模板/指纹、候选排序、坐标转换、fallback 状态机、
  retry 预算、学习记忆和成功/失败归因都属于云端候选；只有窗口绑定、截图、输入队列、pause/stop、
  本地结果验证和坐标安全壳默认留本地。
- 如果某项核心算法暂时仍留在本地，必须在对应 CR 写明这是临时过渡、为什么暂不迁、何时迁走；
  不能把本地旧逻辑当成长期 fallback。
- 谢帅/main agent 在派 worker 前必须主动扫 `ImagePreprocessor`、`ImageFinder`、OCR、template、
  click smart、navigation、dialog、bag、battle、team/maintenance 等调用链，并列出
  `cloud-required / local-safety-only / transitional-local` 三类，不再等用户逐项提醒。

Implementation checkpoint 2026-07-02:

- DHXY 生产路径：
  - `ImagePreprocessor` 已瘦成 crop/save/path helper；
  - wash/count/fingerprint/green-band/text-candidate/route-mask 等核心图片处理能力通过
    `ImageProcessorService` / `CloudImageProcessor` 调 `IMAGE_PREPROCESS`；
  - 小地图坐标/地图名识别通过 `MINIMAP_LOCATION`；
  - 云端不可用、低置信、缺必需输出时按 required failure / no-result 处理，不回旧本地 image preprocess fallback。
- 外部 `D:\mavenProject\dhxy-cloud-brain`：
  - `IMAGE_PREPROCESS` 的黄字、白字、HSV count、route packed、text candidate、tooltip metrics 已按
    DHXY dev-sidecar / 旧本地算法做等价修复；
  - 验证命令：`mvn -q test` passed (`29` tests, `0` failures/errors)，
    `mvn -q -DskipTests package` passed。
- 当前仍按本地保留/后续另卡处理的范围：
  - 通用 `ImageFinder` 模板匹配；
  - 左上角状态模板；
  - 部分简单状态读数/发光检查，例如摄妖香数字与任务激活 glow。
  这些不属于 CR173/CR177G 的 `ImageProcessor` 洗图核心闭环；如后续决定也要保护，应另开卡迁移。

Summon skill / 三技能 target boundary:

- 本地职责：
  - 打开三技能面板；
  - hover 技能格；
  - 截取 raw tooltip / 相关 ROI；
  - 调用云端；
  - 对云端返回的窗口相对坐标做 UI 区域校验；
  - 通过本地 input queue 执行删除/确认等真实输入。
- 云端职责：
  - 洗 tooltip 图；
  - 做状态模板/文字/指纹识别；
  - 判断 `NORMAL_SKILL` / `KEEP_SKILL` / `LOCKED_SLOT` / `EMPTY_SLOT` / `UNKNOWN`；
  - 决定删除、保留、重试或终止；
  - 返回动作、置信度、原因、窗口相对点击坐标。
- 验收红线：
  - 生产路径中 `SummonSkillService` 不再直接调用 `ImagePreprocessor.washYellowText(...)`
    或本地三技能状态模板匹配作为 fallback；
  - 云端不可用/低置信/无效坐标时，不允许本地私自继续按旧洗字逻辑删除技能；
  - testcase/replay/debug 工具可保留用于开发，但不能成为发布 runtime 的可用 fallback。

Navigation target boundary:

- 本地可以继续负责：
  - 打开世界地图/小地图；
  - 输入地图名或目标文本；
  - 下拉/滚动搜索结果；
  - 执行真实点击、关闭地图、等待 Runner/pathing；
  - 窗口绑定、坐标边界校验、pause/stop。
- 云端必须负责的导航决策：
  - 世界地图搜索结果里应该点哪一个黄色候选；
  - 候选评分、route candidate / route memory 选择；
  - 小地图弹出后应该点击哪个窗口相对坐标；
  - 与地图标定/坐标转换相关的策略和资产。
- 本地不应保留生产可用的黄字洗图、候选识别、候选排序、地图转换资产 fallback。
- 第一版可以继续让本地做输入和物理 UI 操作，但“点哪个黄色结果”和“点小地图哪里”
  必须来自云端 request/response。

NPC click smart target boundary:

- 当前 CR-HC-018 的 `NPC_CLICK_STRATEGY` 只做“云端授权本地策略”，这只是过渡态，不是最终发布形态。
- 最终形态中，NPC click smart 背后的 ROI 裁剪、黄字/紫字/菜单文字洗图、目标识别、候选排序、
  首点公式、Ctrl 菜单 fallback、retry 顺序和预算，都必须迁到云端。
- 本地职责：
  - 截取当前游戏窗口 raw image，或按云端/配置要求截取安全 ROI；
  - 传入 target facts，例如目标 NPC 名、逻辑坐标、任务上下文、当前地图/窗口尺寸、上一次尝试结果；
  - 执行云端返回的受限动作：普通点击、`Ctrl`+点击、允许名单内的快捷键动作、等待/重新截图；
  - 在动作后验证本地可观测结果，例如 dialog 是否出现、是否移动、菜单是否出现、是否失败；
  - 把验证结果和新截图作为下一次 request 的输入。
- `NPC_CLICK_SMART` 不使用任何“按目标 NPC 名字制作的黄字/glyph/template”字段或素材。request 传 raw image、
  ROI、`npcName` / `mapName` / `target` 等任务事实；黄色目标只能由外部 brain 在 raw image 中做语义识别。
  通用 tooltip 模板和预期 dialog 模板仍是独立语义，不能被当作目标黄字模板复用。
- `imagePayloadBase64`、`payloadMimeType`、`imageSha256`、`roi`、`windowSize` 是普通和 direct-combat 路径的
  必填视觉素材。
- 上述字段是 declarative request context；本地不得借这些字段生成 `candidateBox`、score/confidence 或 click。
  可执行点击必须来自云端 response 的 `WINDOW_RELATIVE` 动作，再由本地做边界校验和 input queue 执行。
- 云端职责：
  - 在 raw screenshot / ROI 上自行裁剪和洗图；
  - 识别 NPC 名、玩家锚点、黄色目标、Ctrl 菜单候选和任务相关菜单项；
  - 选择下一步动作类型，例如 `CLICK`、`CTRL_CLICK`、`PRESS_HOTKEY_THEN_CLICK`、`RETRY_WITH_MENU`、
    `REQUEST_NEW_SCREENSHOT`、`ABORT`；
  - 返回窗口相对坐标、是否需要按住 `Ctrl`、是否需要快捷键、等待/验证规则、attempt token、原因和置信度。
- CR169 收紧后的 direct-combat 边界：
  - 本地不得在 cloud action 前自行按 `Alt+A` / `Alt+C` 或先跑 flying/direct-combat 本地目标策略；
  - direct-combat request 通过 `verificationMode=direct-combat` 交给 `NPC_CLICK_SMART`；
  - cloud 若要进入点怪模式，必须返回 `PRESS_HOTKEY_THEN_CLICK;hotkey=ALT_A` 或 allowlist 内动作 bundle；
  - 本地只按 allowlist 执行 `ALT_4` / `ALT_C` / `ALT_A`、`CTRL_CLICK`、普通点击，并在动作后验证是否进战斗。
  - cloud action 失败后如检测到仍在 direct-combat 点怪模式，本地只能用固定窗口安全点右键退出；不得再用
    player-anchor / 黄字 / 公式等本地视觉算法寻找退出点。
- NPC click smart 应按状态机式 request/response 推进：
  - 第一次 request 给云端当前窗口图和目标事实；
  - 本地执行云端动作并验证；
  - 若未成功，本地把失败类型、新截图和 attempt token 发回云端；
  - 云端进入下一策略并返回下一次动作；
  - 超出云端 retry/预算后返回 `ABORT`，本地不允许回退到旧本地 smart-click 逻辑。
- CR169 implementation note 2026-07-02:
  - `NpcClickService` production source must not contain reusable local NPC strategy DTO/helper blocks such as
    `NpcClickStrategyResult` / `NpcClickStrategySource` / `recordSmartClickEvidence` /
    `calculatePlayerAnchorFormulaPoint` / `findPlayerAnchorForDirectCombatExit`。
  - Historical task calls to `confirmPendingSmartClick(...)` may be kept only as no-op compatibility until callers
    are cleaned up; they must not write pending evidence or vision memory, and must not affect NPC click success。
- 验收红线：
  - 发布 runtime 中 `NpcClickService` 不再保留可独立复用的 yellow/purple/menu 洗图识别、首点公式、
    Ctrl 菜单候选排序或 fallback 状态机作为生产 fallback；
  - 本地只保留输入安全壳和结果验证，不保留“云端失败后自己算怎么点”的能力；
  - 云端返回的快捷键/修饰键必须走本地 allowlist 和 input queue，云端不能直接控制 OS 输入。

## 27. CR162-CR166 First Cloud Core Migration Wave

```text
Cards created / implementation pending
```

User decision 2026-07-02:

- 先立一张总卡，再从总卡拆第一批小卡。
- 第一批只做已经明确、价值高、边界清楚的四块：
  - CR163 `ImagePreprocessCloud`：洗字/图像预处理。
  - CR164 `NavigationCloud`：导航候选、坐标转换、route memory/outcome。
  - CR165 `NpcClickSmartCloud`：NPC click smart 全链路策略。
  - CR166 `SummonSkillCloud`：三技能 tooltip/槽位识别和删除策略。
- CR162 作为总卡，统一记录“本地安全壳 / 云端核心资产”的红线。
- `DialogCloud`、`TrackerCloud`、`BagItemCloud`、五倍/修罗 phase brain / face / task-plan 暂不进入第一批；
  等 CR163-CR166 实测通过后再讨论第二批。

Manager rule for this wave:

- 谢帅/main agent 继续只做业务主管和 reviewer，不直接写 Java 业务实现。
- 每张子卡必须先写清：
  - request/response contract；
  - 云端职责；
  - 本地安全壳职责；
  - 云端失败时的 fail-closed 语义；
  - 本地旧算法是否只是 shadow comparator；
  - focused guard 和 fresh runtime 验收点。
- 不允许 worker 只加一个 cloud gate 就声称迁移完成；验收必须证明生产路径不再依赖旧本地核心算法作为 fallback。

## 28. Fresh-run 前本地测试矩阵

```text
Recorded 2026-07-06 / local-test gate for CR192-CR203 cloud work
```

用户明确要求：fresh runtime 不能用来发现每个云端小步到底返回什么。每个云端能力进入 fresh 前，
必须先有本地 integration / contract test，用 testcase image、fake window facts 或 dev-server
request 断言 response、queue message、坐标范围、phase/result/yield。fresh runtime 只验真实窗口、
焦点、输入时序和跨窗口并发。

| 能力 | request fixture | expected response | 坐标 / phase / yield 必断言 | 已有本地测试 | fresh-run 前缺口 |
|---|---|---|---|---|---|
| `XIULUO_BRAIN` | fake `windowId/taskRunId/sessionId/stateSeq/phaseToken/actionId`，dev server start/step/action-outcome；tracker handoff fixture 为 `TRY_TRACKER_SHORTCUT` + `PATHING_STARTED` + `MUST_YIELD` + `WAIT_TRACKER_SHORTCUT_PATHING` | start 返回合法 command；action-outcome 返回 `ACCEPTED`；step 只按已接受 outcome 推进，不接受 missing/wrong token | tracker handoff step 必须是 `WAIT_TRACKER_SHORTCUT_PATHING`，不得变 `ROUND_DONE` 或跳 `WAIT_COMBAT`；失败/停止/retry/non-pathing yield 必须 fail-closed | `XiuluoBrainDevServerTest`、`XiuluoBrainCloudDecisionServiceTest`、`XiuluoBrain*WiringTest`；2026-07-06 已补 tracker handoff contract 和 CR199 route/target/enter-battle contract | CR199 本地 gate 已过，Fresh Node C 只验真实窗口进入战斗时序；CR200 combat/return/team-return/recovery 仍需先补本地 contract test 再 fresh |
| `NPC_CLICK_SMART` / FIFO queue | NPC raw screenshot/testcase image、target facts、window size、attempt token；FIFO fixture 覆盖多窗口请求顺序 | 返回受限动作：`CLICK`、`CTRL_CLICK`、`PRESS_HOTKEY_THEN_CLICK`、`REQUEST_NEW_SCREENSHOT`、`ABORT` 等；invalid/unavailable required-stop 不得回本地 smart click | 云端 click 必须是 `WINDOW_RELATIVE` 且在窗口/ROI 内；FIFO queue message 顺序和 window/task identity 不能串线 | `NpcClickSmartCloudDecisionServiceTest`、`NpcClickSmartFifoBehaviorTest`、`NpcClickSmartCloudWiringGuardTest`，`images/test-cases/npc/**` 有部分 replay/marked output | 仍缺一个“真实 testcase image -> dev server/request -> response click in ROI”的最小集成门；补齐前不要让 fresh 发现 NPC 坐标 contract |
| `DIALOG_POLICY` | fake dialog request、pre-click image-backed request、after-local no-action request；包含 operation/dialogType/local status | pre-click required cloud 有效才可执行；after-local `NO_DIALOG` / `STORY_IGNORED` safe no-action 可窄口 passthrough；schema mismatch 仍 fail-closed | 点击型响应需 action/status/click 或 no-action status 一致；after-local 不产生新点击；失败不能转成本地成功 | `DialogPolicyCloudDecisionServiceTest`、`DialogPolicyCloudPreClickDecisionServiceTest`、`DialogCloudPreClickWiringGuardTest`、`DialogImageProcessorCR177CReplayTest` | CR203 fresh 前本地已有 schema/no-action guard；真实 runtime 只验 maintenance 不再反复 `broadcast scan failed` |
| `IMAGE_PREPROCESS` | raw image payload、ROI、operation、image sha；覆盖黄字/白字/绿字/紫字/tooltip/route packed/text candidate | 返回 washed image metadata、candidate boxes/counts/metrics 或明确 no-result/required failure | 不返回裸点击；必须断言输出尺寸、sha/路径、candidate box 在 ROI 内、required failure 不回本地洗图 | `ImagePreprocessCloudServiceTest`、`ImagePreprocessWashedImageClientTest`、`ImageProcessorServiceCR173*Test`、`CloudDecisionDevServerTest` 相关 image-preprocess coverage | 仍需按具体新能力补 testcase image 和 marked/debug output；不能让 fresh 才发现洗图输出字段缺失 |
| `TRACKER_PANEL_READER` / tracker link | tracker panel crop/raw path、origin/window facts、task code、local tracker facts；link ranker fixture 含 candidate rect/click | reader 返回 task/link facts；ranker 返回 selected link click 或 no-link；outside click rejected | tracker click 必须 `WINDOW_RELATIVE`，在 panel/window ROI 内；五倍可断 taskKey，修罗需断 shortcut link/pathing handoff | `TrackerPanelReaderCloudDecisionServiceTest`、`TrackerPanelReaderCloudSourceGuardTest`、`TrackerLinkRankerCloudShadowServiceTest`、`TrackerLinkRankerCloudShadowWiringTest` | 仍缺当前 tracker crop 的 dev-server image replay矩阵项；补齐前 fresh 只能验窗口/焦点/时序，不应承担 response contract 发现 |
| `ROUTE_MEMORY` / route candidate | fake route memory facts、route outcome/migration endpoint；route candidate 需世界地图 result testcase image、window size、route-result ROI | memory `HIT/MISS/ERROR/DISABLED` 有明确 no-click/click；candidate `CLICKED` 必带 `routeDecisionId` 和 window-relative click | candidate click 必须在 1024x768 window 和 route-result ROI 内；outside ROI/window fail-closed；不得用本地黄字 fallback 继续点 | `RouteCloudDecisionServiceTest`、`NavigationRuntimeDecisionShadowWiringTest`、`CloudDecisionDevServerTest` route memory/outcome coverage、`GameTextLineOcrCR177FRouteReplayTest`；2026-07-06 CR199 已补 image-payload route candidate ROI replay guard | CR199 Fresh Node C 不再承担 route candidate response contract 发现；后续若新增地图点击返回字段，仍需先补对应 ROI test |
| `MINI_MAP_LOCATION` | `images/test-cases/minimap/failure-location/20260616_huoyundong_4_3/tmp_pos.png` image payload、map/window facts | 返回 `status=HIT;x=4;y=3` 或 no-result/fail-closed | 断言逻辑坐标、map label/status；若未来返回点击点，再断言窗口/小地图 ROI | `MiniMapLocationCloudReplayTest`、`MiniMapLocationCloudDecisionServiceTest`、`MiniMapCoordinateReaderCR179CloudWiringGuardTest` | 当前只证明逻辑坐标 contract；若接入“点小地图哪里”，必须新增 click ROI test |
| `SUMMON_SKILL` | tooltip/raw slot image、slot index、window facts、skill policy facts | 返回 `NORMAL_SKILL -> DELETE`、`KEEP_SKILL`、`LOCKED_SLOT`、`EMPTY_SLOT`、`UNKNOWN` 等明确动作/状态 | 删除/确认坐标若由云端返回，必须在技能 UI ROI 内；低置信/unknown 不得本地旧洗图删除 | `SummonSkillCloudTooltipReplayTest`、`SummonSkillCloudDecisionServiceTest`、`SummonSkillCloudWiringGuardTest`、`TaskMaintenanceSummonSkillQueueWiringTest` | 现有 tooltip replay 偏状态 contract；若后续云端返回真实删除坐标，fresh 前必须补 click ROI/queue integration test |

当前最小补齐结果：

- 2026-07-06 本轮只补一个关键 integration gap：
  `XiuluoBrainDevServerTest.testStepAdvancesTrackerShortcutPathingOutcomeToWaitPhase`。
- 该测试已经本地通过：
  `mvn -q -Dtest="XiuluoBrainDevServerTest" test`。
- 本轮不补 route/NPC/image 第二测试；这些在上表保持 fresh-run 前缺口，等待下一张 worker 卡单独处理。
