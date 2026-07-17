# GiveItemService Cloud Lift - Internal Worker T

## Parent Task Brief #1 - `W-GIVE-D1` - 2026-07-13T05:52:00-04:00

### 角色与唯一写集

- 你是 Internal Worker T，只做设计/实现，不是 reviewer；父级独立审查。
- 先读 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、DHXY HEAD `0114604e` 的 `GiveItemService.java`、全部 caller、
  `BagService` 当前 HEAD 与 Cloud retained port/runtime 源码。
- 先在本日志追加 `CLAIMED`，本轮只追加 Design #1；两仓 Java/Maven/schema/resources/tests/host/caller 与其它报告冻结。
- 你不是独自在仓库工作：P2 正写 remote Full R0，A/B 与父级也有在途修改；不得回滚、覆盖、清理或提交任何他人 dirty/untracked。

### 目标与冻结语义

设计 109 行 `GiveItemService` 的等价迁云：Cloud 唯一编排“等待 800ms -> Bag 选物 -> 给与按钮识别 -> 点击 -> 等待 1000ms”与
true/false 结果；DHXY 只保留 screenshot/template fact、坐标随机化、原子 input bundle、exclusive section 执行与安全拒绝。

1. inventory 两个 public API 与全部 caller，解释 normal queue 路径和 already-exclusive direct 路径为何不能队中队；Cloud 不得复制
   `Thread.currentThread().getName()` 或本地 queue ownership，必须由 typed invocation mode/capability 显式区分。
2. 保持 HEAD：前置 800ms、Bag `GIVE_BAG` + template/index、按钮模板 `images/template/300huan/btn_give.png`、阈值 0.85、
   randomized `(20,8)`、click hold 100ms、post-click 1000ms，以及每个 false/interrupt/异常传播点。不得增 retry/fallback/verify/TTL。
3. Bag selection 只能调用未来批准的 Cloud Bag business API/retained mechanical adapter；不得在 GiveItem 重新实现翻页/cache/template owner。
4. Give 按钮 capture/match/click 需要 fixed semantic action addresses 和 retained identity；`UNKNOWN/STOPPED` 不变 false 后自动重做，
   不铸新 ID。说明 normal 与 exclusive 两模式的 atomicity、stop/pause、runRevision 和 outcome matrix。
5. click coordinate 最终由 DHXY current exact binding 解析并在输入 worker 前后过安全 fence；Cloud 不用陈旧 screen absolute geometry。
6. host/caller 继续 dormant；不得新增线程、poller、raw request/poll/outcome、ThreadLocal、TTL/takeover/restart restore。

### Design #1 输出

- API/caller/时序/failure/stop-pause/UNKNOWN 矩阵；
- Cloud business owner 与 DHXY retained mechanical boundary；
- fixed action slots、可编译依赖 DAG、精确 New/Modify 文件表；
- 标出 P2、Bag、client-px input、whole-pass exclusive 等前置；
- 可独立实施的真实 CPU/type leaf（没有则明确写“无”，不得造 trivial wrapper）；
- 自审 P0/P1/P2（仅 QA）。

**无已批准业务差异；按基线等价迁移。**

## CLAIMED - `W-GIVE-D1` - 2026-07-13T05:52:29-04:00

- 领取人：Internal Worker T（仅设计/实现，不是 reviewer）。
- 领取时间：`2026-07-13T05:52:29-04:00`。
- 唯一写集：仅 `docs/superpowers/plans/reports/2026-07-13-cloud-give-item-service-worker-t.md`，且仅允许追加。
- 冻结确认：DHXY / Cloud 的 Java、Maven、schema、resources、tests、host、caller 及其它报告全部冻结；不回滚、覆盖、清理或提交他人 dirty/untracked。

## Internal Worker T - Design #1 - 2026-07-13T06:14:32-04:00

### 0. 基线、取证与并发边界

- DHXY 权威基线：`thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`。已完整读取
  `GiveItemService.java` 109 行、`DialogService` 直接调用段、`FiveRingTaskV2` 间接业务入口、
  `InputSequences`、`TaskSleep`、`CoordinateHelper.findImageAbsoluteCoordinate(...)`、
  `ImageFinder.find(...)` 与 HEAD `BagService` 全类。`GiveItemService.java`、`DialogService.java`
  相对 HEAD 零 diff；工作树中的 `BagService.java` dirty 属他人在途内容，只作冲突识别，未作为业务权威。
- Cloud 只读基线：`navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01` 加当前未提交 retained
  port/runtime 源码。已读取 `CloudTaskServicePort`、`CloudTaskServiceExecutionContext`、
  `CloudTaskRetainedActionState`、`CloudTaskRunCommandExecutor`、`RemoteGameClientPort`、
  `RemoteFinalConsumptionCoordinator`、`CloudTaskRunAuthorityAssembly`、
  `CloudTaskRunRetainedLifecycleActivationAdapter`、`CloudTaskExclusiveInteractionState` 及三种 outcome。
- Bag 前置以其父级在同日写明的最终结论为准：`W-BAG-D3 DESIGN APPROVED，P0/P1/P2=0`；当前仅
  `ImageFinder.findAll(...,maxMatches)` CPU 叶 SOURCE APPROVED，Bag wire/state/body/activation 尚未实施。本设计只消费其
  批准 API/opaque child capability，不吸收当前 dirty `BagService` 的任何新增行为。
- P2/A/B/父级在两仓均有在途 dirty/untracked。本节和后续 Design #1 只追加本日志；没有修改、格式化、构建、运行或 Git 操作。

### 1. HEAD 两个 public API 与全部 caller

| HEAD API / 边 | `0114604e` 事实 | 冻结语义 |
|---|---|---|
| `executeGive(String,Integer)`，L30 | 外部 caller **无**；若当前线程名含 `dhxy-input-action-worker`，立即转 `executeGiveDirectForExclusive` | 普通路径不得假装已持有 input owner；但真在 owner 内时不能 nested queue |
| `executeGiveDirectForExclusive(String,Integer)`，L50 | 唯一直接 caller 为 `DialogService.executeDialogCloudPreClick(...)` L528；若不在 input worker，立即转回 `executeGive` | 只有已持有 owner 时才 direct；普通线程调用该名字仍走普通 queue |
| 两方法自调用边 | `normal -> direct` 与 `direct -> normal` 都只用于把实际 queue ownership 归一化 | 迁云后不得复制线程名、ThreadLocal、queue lookup 或递归 wrapper |
| 间接业务 caller | `FiveRingTaskV2.tryGiveItemAndTriggerPathingIfPossible(...)` L3224 构造 `DialogHandleRequest.giveItemIfAvailable("wuhuan-v2:give-item", "wuhuan/shoe.png", shoeBagIndex)`，再由 `DialogService` 进入上行 direct API | 五环只消费 `GIVE_ITEM_DONE/GIVE_ITEM_FAILED`；本卡不改五环 phase、失败计数或 tracker 语义 |

`FiveRingTaskV2` 的当前 `HANDLE_DIALOG` 路径在 coarse task turn 外运行，不足以成为永久的 ownership 判据；同一个
`DialogService` API 也可以从已持有 exclusive owner 的调用栈进入。因此 Cloud 不能按 caller 名或线程猜 lane。

#### 1.1 Cloud 单一 typed API（不造两个 trivial wrapper）

Cloud 主体只保留一个真实业务入口：

```java
boolean executeGive(
        GiveItemServicePortAdapter.Invocation invocation,
        String targetItemTemplate,
        Integer knownBagIndex)
```

`Invocation` 是 assembly/retained adapter 铸造、外部不可构造的 opaque capability，内含 closed
`InvocationMode { NORMAL, ALREADY_EXCLUSIVE }`。模式是 authority 对“是否携带同 exact run/window/stopEpoch 的有效
whole-pass capability”的结果，不是 caller 自报：

| authority 输入 | typed mode | 对应 HEAD |
|---|---|---|
| 无 outer capability | `NORMAL` | `executeGiveDirectForExclusive` 在非 worker 上回落 `executeGive` |
| 有且通过 owner/generation/runRevision 校验的 outer capability | `ALREADY_EXCLUSIVE` | 任一入口在 input worker 上执行 direct body |
| stale/foreign/伪造 capability | typed reject/unwind | 不能把“有一个坏 capability”静默当成“没有 capability” |

因此 Cloud 不重建两个互相转调的 public wrapper，也不复制 `isInputWorkerThread()`。未来 Cloud `DialogService` 只把自己真实持有的
outer capability（可能为空）交给 fixed adapter；adapter 决定 mode 后再调用上面的单一 API。

### 2. 两种 lane 的逐步时序与 atomicity

| 顺序 | `NORMAL` | `ALREADY_EXCLUSIVE` |
|---:|---|---|
| 0 | 无 whole-pass acquire；其它窗口可正常排队 | outer caller 已在进入 GiveItem 前 ACQUIRE，GiveItem 不 acquire、不 release |
| 1 | `TaskSleep.sleep(800)`；这 800ms 不占 input queue | 同样等待 800ms，但 outer local owner 始终保留，普通物理输入不能插入 |
| 2 | `bagService.findAndSelectItem(BagService.GIVE_BAG,targetItemTemplate,knownBagIndex)`；Bag 自己 acquire/release | `bagService.findAndSelectItemDirectForExclusive(...)`；消费同一个 outer child capability，禁止 nested acquire |
| 3 | fixed retained `GIVE_ITEM_BUTTON_MATCH` fact；事实读取不提交物理输入 | 同一个 fact；outer physical owner 在 Cloud round-trip 期间仍保持，fact 本身不 nested queue |
| 4 | 普通 FIFO 原子 bundle：`CLICK_LEFT(clientPoint,100)` + `SLEEP(1000)` | 同 outer session control lane 的一个原子 bundle：同样 `CLICK_LEFT` + `SLEEP(1000)` |
| 5 | bundle 全部完成才返回 true；Bag release 与 give click 之间仍允许 HEAD 同样的窗口插入 | 返回给 outer caller但不释放 owner；outer dialog/workflow 决定后续 step 与 RELEASE/ABORT |

关键不变量：

- normal 路径**不能**被扩大成 select+give whole-pass；HEAD 的 Bag 普通 exclusive 返回后，另一个窗口可以在 give button click 前插入。
- already-exclusive 路径**不能**调用普通 Bag/input API；否则唯一 input worker 正持有 callback/session，却等待自己队列中的新请求，形成队中队死锁。
- 两种路径的最后点击都把 click hold `100ms` 与 post-click `1000ms` 放在同一个原子 bundle。direct 只是使用 outer control lane，
  不是让 Cloud 调 `InputProvider` 或 callback。
- GiveItem 不拥有 outer session 的 RELEASE/ABORT。它只冻结自己的 boolean 结果并返回到 caller，严格对应 HEAD direct body 返回 callback。

### 3. Cloud business owner 与 DHXY retained mechanical boundary

#### 3.1 Cloud 唯一业务 owner

Cloud `GiveItemService` 唯一拥有：800ms -> Bag 选物 -> 按钮事实判定 -> 点击/1000ms -> true/false 的阶段顺序、
每一阶段是否继续、mode、workflow occurrence `W` 与最终 boolean。它不读取本地线程名、HWND、路径、队列、provider、
`WindowTaskContextHolder`、raw request/poll/outcome，也不实现 Bag 的 page/cache/template/layout 逻辑。

`targetItemTemplate` 与 `knownBagIndex` 原值透传批准后的 Cloud `BagService`；GiveItem 不 trim、不 canonicalize、不 clamp、不验证 null，
也不复制 `GIVE_BAG` 的 layout 数值。Bag 返回 false 时 GiveItem 立即 false，异常保持传播；GiveItem 不加 retry/fallback/page scan。

#### 3.2 DHXY fixed screenshot/template fact

新增 closed `WindowFactKind.GIVE_ITEM_BUTTON_MATCH`，无自由 template/path/threshold 参数。DHXY provider 的唯一固定映射为：

```text
template = images/template/300huan/btn_give.png
threshold = 0.85
randomRadiusX = 20   // inclusive [-20,+20]
randomRadiusY = 8    // inclusive [-8,+8]
```

本地和 Cloud 打包资源当前 SHA-256 均为
`75083ed2a1248cd920cf7f66d0917c8b00c2f35e776c605397f3770ec238dcf9`，本卡 resources 写集为 0。

DHXY `LocalGiveItemButtonFactProvider` 必须复用现有方法，不重写 matcher：

1. 在 handler 已绑定的 exact `WindowRuntimeContext` 内，只调用一次
   `CoordinateHelper.findImageAbsoluteCoordinate("images/template/300huan/btn_give.png",0.85)`；
2. HEAD 该 helper 会调用一次 `tracker.updateGlobalVision()`，但**不检查其 boolean**，随后读取 window-scoped
   `latest_vision.png`。这是实际 pushed 行为；本设计不擅自新增 capture-failed gate、第二次 capture 或 fresh-frame verification；
3. helper 返回 null -> `OBSERVED/NOT_FOUND`；返回的是模板中心 screen-absolute logical point；
4. 只在命中时调用一次 `coordinateHelper.getRandomizedPoint(point,20,8)`；随机点作为该 retained fact outcome 的固定内容，
   redelivery/UNKNOWN 不再次抽样；
5. handler 在 fact 前后复验 exact registration/runRevision/binding。合法时把随机点减去**同一次 post-check current bound base**，
   回 `WINDOW_CLIENT_PX`；binding/revision/geometry 变化走 `NOT_EXECUTED` safety rejection，绝不伪装成 `NOT_FOUND`。

typed fact shape 固定为：

```text
matchState = MATCHED | NOT_FOUND
coordinateSpace = WINDOW_CLIENT_PX
clickX/clickY = MATCHED 时 required，NOT_FOUND 时 null
observedWindow = exact windowId/nativeHandle/processId/playerIdentityEpoch
```

Cloud 只把 `MATCHED/NOT_FOUND` 解释为继续/false，不从 stale `SCREEN_ABSOLUTE_PX` 做几何推导。最终 input step 的
`WINDOW_CLIENT_PX` 由 DHXY input worker 在副作用前用 current exact binding 转屏幕点；handler 入队前、worker 每个副作用前、
await 后 outcome 发布前均执行 registration/runRevision/binding/session/geometry fence。普通 screen-absolute 旧路径不变。

本地 provider/matcher/runtime 异常不得压成 `NOT_FOUND`；按 Full R0 形成 `UNKNOWN/INTERNAL_ERROR`。这与 HEAD 未捕获异常“不能作为
正常 false 分支继续”一致，同时避免不确定点击被自动重做。

### 4. Retained workflow、fixed slots 与 runRevision

#### 4.1 stable key 与 occurrence

一个 invocation 的 stable identity 为：

```text
RemoteTaskRunScope(tenantId,userId,deviceId,clientSessionId)
+ taskRunId/taskType
+ RemoteTaskRunWindow(windowId,nativeHandle,processId,playerIdentityEpoch)
+ admissionStopEpoch
+ fixed callsite address（当前唯一 DIALOG_GIVE_ITEM）
+ workflow occurrence W
+ invocationMode
+ outer exclusive stable key（仅 ALREADY_EXCLUSIVE）
```

`runRevision` 不进入 stable key；它由 current-context slot/whole-pass authority 在 pause-resume 时前进。`W` 只由 Full R0 retained
frontier 分配：前一 invocation 得到确定 terminal/final-consumed 后，下一次 caller 调用才分配 `W+1`。UNKNOWN、pause、transport
redelivery、stale handle 都保留原 W；Service/adapter/Bag/handler 均不得 `UUID.randomUUID()` 或自行递增 occurrence。

#### 4.2 fixed semantic action slots

notation：`RemoteSemanticAddress(phaseCode="give-item", actionSlot=<fixed enum>, occurrence=W, attempt=<Full R0 owner>)`。

| fixed slot | operation / owner | 声明时点 |
|---|---|---|
| `GIVE_BUTTON_MATCH_FACT` | `WINDOW_FACT`；retained adapter | 800ms 完成且 Bag 明确 true 后才声明 |
| `GIVE_BUTTON_CLICK_SETTLE_INPUT` | `EXECUTE_INPUT_BUNDLE`；normal 用普通 action，direct 用 outer session child step | fact 明确 `MATCHED` 后才声明 |
| `BAG_SELECT_NORMAL` | 交批准后的 Bag root `entry-select` | mode=NORMAL、800ms 完成后 |
| `BAG_SELECT_OUTER_CHILD` | 交批准后的 Bag root `child-bag-select-direct` | mode=ALREADY_EXCLUSIVE、outer capability 校验后 |

800ms 是 Cloud retained workflow cursor，不伪造 remote SLEEP action；normal 不占 queue，direct 则由已存在 outer owner 保持物理隔离。
GiveItem 不声明 ACQUIRE/RELEASE/ABORT slot，不建第二个 exclusive registry。

`GiveItemWorkflowState` 只保留 `W/mode/stage/preWaitCompleted/Bag definitive result/fact handle+fact result/click handle+outcome/terminal result`
与 outer opaque capability reference；不持 HWND、screen base、Path、图片、queue、TTL 或 restart restore 数据。assembly initial 创建一次，resume
构造新 per-revision adapter/service 时复用同一个 state；旧 service/handle 永久 stale。

### 5. true/false、failure、UNKNOWN 与异常矩阵

| 阶段 / exact outcome | Cloud 业务结果 | occurrence / 副作用纪律 |
|---|---|---|
| 800ms 完整结束 | 继续 | `preWaitCompleted=true`，pause/resume 不重复这 800ms |
| `TaskSleep.sleep(800)==false` | 保持 HEAD `false`；若生命周期已 STOPPED，caller 同时按 stop unwind | 尚无 remote action；不得自动开始新 W |
| Bag 明确 `true` | 继续 | 只消费 Bag 的确定 terminal result |
| Bag 明确 `false` | 立即 `false` | 不声明 button fact/click；无额外 page/retry/fallback |
| Bag `UNKNOWN` | 不返回 boolean，park unresolved | 同 Bag child identity、同 W；不重新选物 |
| Bag `STOPPED/stale` | typed stop/stale unwind，不作为“物品不存在” | outer owner按 exact terminal合同收口；不自动新 W |
| button fact `OBSERVED/MATCHED` | 继续 | 固定使用 fact 中一次抽出的 client point |
| button fact `OBSERVED/NOT_FOUND` | 立即 `false` | 对应 HEAD `btnGivePoint == null`；不 click、不补 capture |
| fact confirmed `NOT_EXECUTED` | typed mechanical failure/unwind，不伪装 `NOT_FOUND` | 无自动 renewal/retry；由 outer caller下一次业务调用决定是否产生新 W |
| fact `UNKNOWN` / provider exception | 不返回 boolean，保留 unresolved | 不换 requestId/actionId，不再次截图/随机化 |
| click bundle `EXECUTED` 且两 action 全完成 | `true`，记录 finished log | click hold=100ms，post sleep=1000ms 均已完成 |
| click bundle confirmed `NOT_EXECUTED` 且 `startedStepIndex=-1` | 对应 normal `submitAndWait=false`，返回 `false` | exact attempt final-consumed；Give 内不自动重投 |
| click bundle `STOPPED` 且未开始 | HEAD false 与 stop unwind同时成立 | 不让 task stop后的 scheduler把 false当普通 retry |
| click 已开始但 sleep 未完成时 `STOPPED/UNKNOWN` | 不形成可重做的普通 false；typed stop/unresolved unwind | click 可能已经发生，严禁新 ID 重点 |
| Bag/provider/port/Cloud CPU 非 stop 异常 | 原样传播到 retained task host；remote side-effect uncertainty 为 UNKNOWN | 不 catch 成 false，不新建 fallback |

HEAD 的五个 false 点逐项冻结：前置 sleep 中断、Bag select false、give button null、normal queue false、direct post-click sleep false。
其中 direct “click 已执行但 1000ms sleep 被中断”的 false 只能与 stop/partial-progress证据一起收口，绝不能被解释成“没点过”后自动重做。

无 retry、fallback、额外 verify、TTL、expiry、cleanup、fail-closed business rule 或后台补偿；caller 原有下一轮重入属于 caller 业务，
不是 GiveItem 内部自动 retry。

### 6. pause / stop / runRevision / atomic owner matrix

| 场景 | NORMAL | ALREADY_EXCLUSIVE |
|---|---|---|
| pause 落在 800ms | 当前一次 wait 可完成；下一安全门 park，`preWaitCompleted` 不回退 | outer session 以 R-X approved `PARKED_PAUSED` 保留同 session/nextStep；不 RELEASE |
| pause 落在 Bag/fact 前后 | 只继续尚未完成的 retained stage；已确定 Bag/fact 不重做 | Bag child + fact W不变，outer capability经 current-generation handoff绑定新 revision |
| pause 落在已提交 click bundle | 当前 input worker 已开始的原子 bundle按现行规则完成后再 pause | 同一 outer control-lane bundle完成后 park；其它窗口仍不能插入 |
| resume | assembly发布新 per-revision service/adapter，复用同一 workflow state/W | 同一 exclusiveSessionId、W、nextStep；只换 binding generation/current runRevision |
| stop before side effect | false/typed stop，未声明后续 slot | exact outer ABORT；Give 不自行 mint abort |
| stop/transport loss after possible click | progress/UNKNOWN 保守保留，不当普通 false | local owner只在 worker确认无副作用运行后释放；Cloud业务仍可保持 UNKNOWN |
| Cloud/DHXY restart | 不 restore、不 takeover；旧 run/session不能复活 | 同；不靠 TTL/lease/new poller恢复 owner |

### 7. 可编译依赖 DAG 与实施门

```text
P2 Full R0 FINAL APPROVED + 两仓 shared remote/digest/schema hash 稳定
  ├─ R-X1/R-X2/R-X3 whole-pass authority 完整实现（当前只有 R-X0 policy leaf）
  ├─ B 已批准 WINDOW_CLIENT_PX input validator/codec/current-binding conversion 实现
  └─ Bag W-BAG-F0/C0/S0/A0 完整实现（含 direct outer child capability）
        -> W-GIVE-M0 fixed GIVE_ITEM_BUTTON_MATCH fact（双仓 schema/mechanical，dormant）
        -> W-GIVE-C0 GiveItem retained state + fixed adapter + Cloud主体（dormant）
        -> W-GIVE-A0 assembly publication + Cloud Dialog caller 接线（另卡；host仍 dormant）
        -> THIN_CLIENT_V1 全体原子 cutover 后 fresh runtime 验收
```

硬门：

- **P2**：没有 final-consumed/occurrence/UNKNOWN identity，就不能声明 W 或 action handle。
- **Bag**：GiveItem 只调用批准后的普通/direct API；Bag state/body未到位时禁止在 GiveItem 复制页序/cache/template。
- **client-px input**：当前 Cloud `InputBundleRequest` 与 DHXY codec 仍拒绝 `WINDOW_CLIENT_PX`；B 的批准实现完成前 click 不可运行。
- **whole-pass**：R-X0 只是 state policy，尚无 local owner/control lane/port capability。R-X1..X3 未完成前 direct 模式不可运行，
  也不得退化成多个普通 bundle。
- **caller/host**：Cloud `DialogService`/Task 尚未迁入且 host 未激活；W-GIVE-C0 只能 dormant，不添加 bean、route、thread、poller或启动路径。

### 8. 精确 New / Modify 文件表（未来波次；本轮零写）

#### 8.1 `W-GIVE-M0` fixed fact 原子波

Cloud New：**无**（fact record 作为既有 sealed `WindowFact` 的 nested variant）。

Cloud Modify：

| 精确文件 | delta |
|---|---|
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/WindowFactKind.java` | 增 closed `GIVE_ITEM_BUTTON_MATCH` |
| `.../remote/WindowFact.java` | permits/nested `GiveItemButtonMatchFact` + closed `MATCHED/NOT_FOUND`，严格 null/coordinate/binding shape |
| `.../remote/WindowFactOutcome.java` | kind/variant strict matching |
| `.../remote/RemoteCommandOutcomeEnvelope.java` | strict JSON reconstruction 新 fact variant |

DHXY New：

| 精确文件 | delta |
|---|---|
| `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteGiveItemButtonMatchFact.java` | immutable local wire DTO；match state/client point/observed binding |
| `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/remote/LocalGiveItemButtonFactProvider.java` | 固定 helper/template/0.85/(20,8) mechanical provider；无 business retry |

DHXY Modify：

| 精确文件 | delta |
|---|---|
| `.../cloud/remote/RemoteWindowFactKind.java` | 同构增 `GIVE_ITEM_BUTTON_MATCH` |
| `.../cloud/remote/LocalRemoteGameCommandHandler.java` | constructor注入 provider；exact context bind；fact switch；前后 registration/runRevision/binding/geometry fence；generic payload发布 |
| `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 双仓共享 closed fact kind/result shape 与原子版本门 |

明确零修改：两仓 operation union、request shape、`RemoteProtocolDigests` canonical算法（既有 generic typed tree自然覆盖新 fact）、
`RemoteCommandEnvelope`、DHXY `RemoteOperationPayloadCodec` request fields、坐标 enum、Maven、resources、tests。

#### 8.2 `W-GIVE-C0` Cloud retained/body dormant 波

| 精确文件 | New/Modify | delta |
|---|---|---|
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/GiveItemWorkflowState.java` | New | package-private retained W/mode/stage/handles/result；跨 revision，无 TTL/restart restore |
| `.../remote/GiveItemServicePortAdapter.java` | New | public type + package-private constructor；assembly唯一构造；fixed slots、opaque Invocation、normal vs outer capability投影、final-consume owner；不暴露 raw handle/ID |
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/GiveItemService.java` | New | 单一 typed API与本设计顺序；per-runtime，无 `@Component`、无 thread-name判断 |
| `.../remote/CloudTaskRunAuthorityAssembly.java`（含 nested `TaskServiceRuntime`） | Modify | initial创建 workflow state/adapter/service；resume复用同 state、构造新 revision adapter/service并随 runtime原子发布 |

`CloudTaskRunCurrentContextSlot` 0 Modify：它已原子发布整个 `TaskServiceRuntime`，只有最终 R-X/Bag publication shape 出现真实编译缺口时
才由对应基础设施 owner修改；Give卡不加 ambient getter。`CloudTaskServicePort`/ledger/broker/input session 的 whole-pass方法归 R-X3，
Give卡只消费，不重复认领。

#### 8.3 `W-GIVE-A0` caller/cutover（另卡，当前冻结）

| 仓库/精确文件 | future delta |
|---|---|
| Cloud `src/main/java/com/bot/dhxy/service/DialogService.java`（其迁移卡创建后） | 唯一 direct caller改为取得 fixed `DIALOG_GIVE_ITEM` Invocation；有 outer capability则传同一 cap，无则 NORMAL |
| Cloud `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`（其迁移卡） | 仍只经 Dialog API；Give卡不改五环 phase/result mapping |
| DHXY `src/main/java/com/bot/dhxy/service/DialogService.java` | 全局原子 cutover时移除本地 GiveItem business call/field；此前原样保留 dormant旧路径 |
| DHXY `src/main/java/com/bot/dhxy/service/GiveItemService.java` | 仅全局原子 cutover后删除本地 business owner；机械 fact 已在 `cloud.remote` 独立保留 |
| 两仓 host/config | 本卡 0 Modify；不得单独激活 GiveItem bean/route/caller |

### 9. 可独立实施的真实 CPU/type leaf

**无。** GiveItem 没有独立纯算法：按钮 matcher/randomizer必须留在 DHXY fixed fact并依赖 exact binding；normal/direct mode依赖 R-X
capability；Bag 分支依赖已批准但未实施的 Bag owner；workflow state又依赖 Full R0 occurrence/final-consume。单独新建 enum、request wrapper、
boolean policy 或只转调另一个 helper都属于 trivial wrapper/shell，不构成可交付叶子，故本设计不造。

### 10. Worker T 自审（仅 QA，不构成 Approved）

- P0 自审：0。没有 raw input/request/poll/outcome、没有 queue-in-queue、没有 stale screen absolute click、没有 host/Task启动。
- P1 自审：0。800ms、`GIVE_BAG`、target/index透传、按钮模板/0.85、模板中心、inclusive `(20,8)`、100ms、1000ms、
  normal interleave/direct whole-pass、五个 false点及异常传播均逐项冻结。
- P2 自审：0。两个 HEAD API与全部 direct/indirect/self caller已盘点；typed mode不靠线程名；fixed slots/W/runRevision/UNKNOWN/STOPPED、
  DAG、真实文件表、无叶结论均闭合。
- 外部实施门仍开放：P2 Full R0、R-X1..X3、Bag F0/C0/S0/A0、client-px input、Cloud Dialog/caller/全局 cutover均未完成；
  因此当前**不得实施/启动/交给 fresh runtime**。
- 本轮只有本日志两次 append（CLAIMED + Design #1）；Java/Maven/schema/resources/tests/host/caller/其它报告/Git均未修改，
  未运行 Maven/tests/application/server/Task/poller/UI/capture/input。

**无已批准业务差异；按基线等价迁移。**

Worker T 到此停止，等待父级独立审查；本自审不算 `Approved`。

## Design #1 最新边界收口 - 2026-07-13T06:19:21-04:00

本节是 Design #1 的规范性追加，落实用户最新边界；若前文对 capability 的“铸造/持有”措辞可被理解为 Cloud 拥有
already-exclusive 或 input queue 安全能力，以本节为准。它不改变 `0114604e` 业务顺序、结果或数值。

### 11. 本设计不等待 P2

- `W-GIVE-D1 / Design #1` 现在即为完整设计交付，不以 P2 Full R0 的 Java 落地为前提，也不等待 P2 才能形成设计结论。
- P2、R-X1..X3、Bag 与 client-px input 只列为**未来 Java 实施/编译 DAG**；它们不能把本轮报告状态改写成“设计未完成”。
- 本轮仍只写报告：不实现上述依赖，不触碰两仓 Java/Maven/schema/resources/tests/host/caller，不启动任何运行路径。

### 12. 最终 authority 边界

| authority | 唯一所有权 | 明确禁止 |
|---|---|---|
| Cloud `GiveItemService` | GiveItem 的业务阶段游标与顺序：800ms -> Bag `GIVE_BAG` -> 按钮事实解释 -> 给与点击 -> 1000ms settle -> 最终 boolean；`W`、fixed action slots、UNKNOWN/stop/pause 后是否继续同一业务阶段 | 不截图、不读模板、不算坐标、不持 input worker/queue/session，不验证本机 exclusive owner，不直接点击，不按本地线程名猜 mode |
| DHXY screenshot/template capability | exact bound window 截图、固定 `images/template/300huan/btn_give.png`、阈值 `0.85`、模板中心、命中后唯一一次 inclusive `(20,8)` 随机化、坐标转换与 binding/geometry fence | 不解释“未命中后做什么”，不决定 true/false，不推进 Bag/click/terminal phase，不 retry/fallback/额外 verify |
| DHXY input/exclusive capability | 普通 input queue；already-exclusive session 的 mint/validate/join/control lane；防队中队；focus/binding；client-px 到副作用时转换；`CLICK_LEFT(...,100)` + `SLEEP(1000)` 原子执行；执行证据/UNKNOWN | Cloud 不能 mint/伪造 owner，DHXY 不能因持有 session 而接管 GiveItem 业务阶段或 occurrence |

因此，Cloud 可保留一个 typed `Invocation` **业务信封**，但信封中的 outer capability 只能是 DHXY exclusive authority 已签发并通过
协议保留的 opaque reference。Cloud assembly/adapter 不创建本机 exclusive 能力，不读取其内部状态，也不替 DHXY 判定 queue ownership。
前文“adapter 铸造 Invocation”仅表示构造不可由业务 caller 任意填充的 typed 调用信封，不表示铸造 exclusive session/capability。

### 13. normal / already-exclusive 的精确跨边界合同

1. Cloud caller 没有 DHXY 签发的 outer opaque capability 时，业务信封为 `NORMAL`；DHXY 只通过普通 FIFO input API执行各机械动作。
2. Cloud caller 转交 outer opaque capability 时，业务信封表达 `ALREADY_EXCLUSIVE` 期望；DHXY 必须在每个机械命令执行前验证
   exact run/window/stopEpoch/current runRevision/generation/session/nextStep，验证成功后才 join 同一 control lane。
3. stale、foreign、伪造或已终结 capability 必须由 DHXY typed reject/UNKNOWN fence；绝不由 Cloud 降级为 NORMAL，也绝不另开普通队列重做。
4. Cloud 拥有并推进 800ms、Bag、match、click、terminal 业务阶段；DHXY 的 outer session 可在这些 Cloud round-trip 间保持
   物理输入所有权，但它只保存机械 session/nextStep/执行证据，不保存 GiveItem phase。
5. Bag direct 机械路径与最后 click bundle 都 join 同一 DHXY owner/control lane；不得在 owner 内调用普通 queue，不得拆成新的
   `submitAndWait`，也不得让 Cloud 回调 `InputProvider`。
6. NORMAL 的 Bag 普通 acquire/release 后，到 give click 入队前仍可被其它窗口插入；ALREADY_EXCLUSIVE 从 caller 的 outer acquire
   到 caller 的 release/abort 均不允许其它物理输入插入。GiveItem 自身不 acquire/release outer owner。

### 14. DHXY 不持 GiveItem 业务 phase

DHXY 侧不得新增 `GiveItemWorkflowState`、GiveItem stage enum、800ms-completed cursor、Bag业务结果缓存、MATCHED/NOT_FOUND 的业务解释、
Give click 后 boolean 决策、`W` 分配器或 GiveItem retry/fallback 状态。DHXY 可以且必须保留的 retained 内容仅是通用机械协议所需的
exact request/action identity、opaque exclusive session/nextStep、fixed fact outcome、input progress 与 UNKNOWN/STOPPED 执行证据；这些内容
只回答“机械事实/副作用发生到哪一步”，不能自行决定下一条 GiveItem 业务命令。

对应未来精确文件表的规范性解释：

- Cloud 的 `GiveItemWorkflowState` / `GiveItemServicePortAdapter` / `GiveItemService` 承担全部 GiveItem phase 与最终 boolean。
- DHXY 的 GiveItem-specific 增量仍仅是 fixed fact DTO/provider 与 generic fact handler/kind 接线；不新增本地 GiveItem service/body/state/phase。
- DHXY already-exclusive/input queue/session/client-px/fence 由 R-X/input 通用基础设施提供并永久留在 DHXY；它们是 GiveItem 的机械依赖，
  不是迁入 Cloud 的业务实现，也不是本轮可抢写的 GiveItem 文件。

### 15. 最新边界下的冻结结论

- 仍严格冻结 `800ms`、`BagService.GIVE_BAG`、target/index 原值透传、模板路径、`0.85`、模板中心、inclusive `(20,8)`、
  click hold `100ms`、post-delay `1000ms` 与 HEAD normal/direct interleave 差异。
- `NOT_FOUND` 才是确定 false；capture/provider/binding/session/transport 不确定性保持 typed UNKNOWN/stop/stale，不新增截图、随机点、
  action ID、retry、TTL、fallback、extra verification 或 fail-closed 业务规则。
- 本地 retained mechanical identity 不等于本地业务 phase；Cloud retained business phase 不等于 Cloud 获得本机 exclusive/input 权限。
- **无可独立实施的真实 CPU/type leaf**：本轮不为抢进度制造 enum、wrapper、state shell 或本地业务 phase。

**无已批准业务差异；按基线等价迁移。Design #1 已完成，不等待 P2；等待父级独立审查，自审不算 `Approved`。**

## Parent Design Review #1 - APPROVED - 2026-07-13T06:33:00-04:00

父级以 DHXY committed HEAD `0114604e` 的 `GiveItemService`、`DialogService` direct caller 与五环 caller 路径复核 Design #1 及最新边界收口，结论为 **DESIGN APPROVED，P0/P1/P2=0**：

- 800ms、`GIVE_BAG`、target/index 原值、按钮模板/`0.85`、模板中心、inclusive `(20,8)`、click hold 100ms、post-click 1000ms、normal 可插入与 direct whole-pass 不可插入均与 HEAD 对齐。
- Cloud 只拥有 GiveItem 业务阶段/boolean；DHXY 永久拥有 exact-window capture、模板匹配/随机点、坐标转换、input queue 与 already-exclusive owner。尤其 UI clean/Runner 所需截图与模板能力留本地的最新用户定案没有被反向迁云。
- direct 的 1000ms 中断在 HEAD 会先形成 false，但五环 caller 紧接 `TaskCheckpoint` 走 stop unwind；设计中的 partial-progress/STOPPED 证据只阻止不安全 renewal，不得在未来 caller 实施时新增普通 retry/fallback 或改变非 stop 的 `GIVE_ITEM_FAILED` 映射。
- 当前无可独立实施的真实 leaf，Java 实施必须等 Full R0、Bag retained adapter、whole-pass authority 与 client-px input 合同稳定；这只是依赖顺序，不影响本次设计通过。host/caller 继续 dormant。

Internal Worker T 本切片完成并可关闭；父级不会为了“看起来有进度”制造 wrapper/state shell。

**无已批准业务差异；按基线等价迁移。**
