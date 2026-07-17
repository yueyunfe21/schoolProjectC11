# Cloud SummonSkillService Migration - Worker N

## Parent Task Brief #1 - 2026-07-13

### 目标

以 DHXY HEAD `0114604e` 的 `com.bot.dhxy.service.SummonSkillService` 为业务权威，形成整类 lift-and-shift 的可编译
依赖闭包，并划出一个无需本地窗口、输入队列、`WindowRuntimeContext`、`TaskPauseToken` 或 host 的首个可直接编码叶子。
现有 `SummonSkillTailBoundaryScanner` 已在 Cloud；不得重复复制或改写其算法。

### Worker N 当前唯一任务

只向本日志末尾追加 `Internal Worker N - Design #1`，父级 `DESIGN APPROVED` 前零 Java。设计必须给出：

1. HEAD 全部 public/package-visible API、全部 caller、构造依赖、static/instance mutable state、模板/config/时间语义矩阵；
2. 每条 screenshot/OCR/template/window fact/input bundle 的现有顺序、ROI/坐标空间、阈值、retry/fallback、sleep/stop 边界；
3. Cloud retained business-action identity 的 canonical phase/actionSlot/occurrence/attempt owner，UNKNOWN/STOPPED/NOT_EXECUTED
   的 typed unwind；不得让 Service 自铸随机 ID或自动 retry；
4. exact `TaskExecutionContext`/scope/taskRun/window/stopEpoch/runRevision 绑定，以及 pause/resume 同 taskRun retained state；
5. 只经现有 non-mintable `CloudTaskServicePort` / retained typed handle 请求机械能力，不开放 raw request/poll/outcome；
6. tenant 容量、restart fail-closed、diagnostics、dormant Spring reachability；
7. 精确依赖 DAG 与分波次写集。优先定义 `W-SS-0` 纯 DTO/config/CPU/模板叶子，目标必须当前不存在且与 A/B/M
   写集零交叉；主体 `SummonSkillService`、assembly/host/caller 在父级批准前冻结。

### 冻结与验收

- 不修改 A 的 TaskMaintenance、B 的 AutoCombatPanel、M 的 final-consumed DTO/Full R0、J/H/K authority stack。
- 不新增/恢复 tests；不运行 Maven；不启动 application/server/host/Task/poller/UI/capture/input。
- 不改业务 phase、OCR/template/click 顺序、fallback、retry、timing 或 stop 语义。
- 只追加本日志，不改 CR 状态，不提交/回滚/覆盖任何 dirty/untracked。

Worker 自审仅 QA，不构成批准。**无已批准业务差异；按基线等价迁移。**

## Internal Worker N - Design #1 - 2026-07-13

### 0. 结论、基线与本轮边界

- 业务权威：DHXY HEAD `0114604e` 的
  `src/main/java/com/bot/dhxy/service/SummonSkillService.java`（完整读取 1-1759 行）。
- caller 扫描：main 源码中唯一外部生产 caller 是
  `TaskMaintenanceService.java:2383 -> cleanSummonSkillsOnce(cleanupRequest)`；其余命中均为本类内部调用。
- Cloud 已有并复用：`SummonSkillCleanupRequest`、`SummonSkillCleanupResult`、`SummonSkillSlotStatus`、
  `SummonSkillTailBoundaryScanner`、`ImageFinder`、`ImageProcessorService`/`CloudNativeImageProcessor`、
  `SummonSkillRecognizer`、`CloudTaskServiceExecutionContext`、`CloudTaskServicePort` 与 retained action ledger。
- `W-SS-0` 目标
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillStaticSlotPolicy.java`
  当前不存在；与 A/B/M/J/H/K 当前写集零交叉。
- 本轮只形成设计并追加本日志；未修改 Java/Maven/resources/tests，未运行 Maven，未触发任何运行面。
- 总体判断：`W-SS-0` 可在父级批准后立即编码；主体 `SummonSkillService` 仍有两个不能靠 Service
  表面适配掩盖的 P1 前置：**整 pass 独占输入所有权**以及 **capture-time typed `systemScaleRatio`**。

### 1. HEAD API / caller / 构造与状态矩阵

| 类别 | HEAD 合同 | caller / Cloud 处置 |
|---|---|---|
| public ctor A | 11 依赖 `@Autowired` 构造：tracker、coordinate、input sequences/provider、UI cleaner、dialog、TMS provider、temp path、image processor、cloud decision、window context holder | 不复制；Cloud 构造依赖改成明确业务 collaborator + explicit context，禁止本地窗口/输入类进入 Cloud |
| public ctor B | 10 依赖兼容构造，内部铸一个返回 null 的 `ObjectProvider<TaskMaintenanceService>` | 仅兼容本地旧调用；Cloud 不保留该伪 provider 构造 |
| `cleanSummonSkillsOnce()` | defaults 请求，返回 structured result 的 `isSuccess()` | 无外部 main caller；Cloud 不重开无 context 入口 |
| `cleanSummonSkillsOnce(request)` | 完整 pass；null -> defaults；外层独占 input，结束后轻量 UI cleanup | 唯一生产 API；Cloud 变为 context + non-mintable execution handle + request + typed collaborators |
| `openSummonSkillPanel()` | 已在 input worker 则 direct，否则独占 queue | 无外部 main caller；仅作为 Cloud 主流程私有阶段，不公开裸机械入口 |
| `cleanTailNormalSkills()` | 已在 worker 则 direct，否则独占 queue；成功才 `cleanUpAll()` | 无外部 main caller；不公开 |
| `detectSummonSkillSlotCount()` | IF8 ROI 一次 capture/match，命中 8，否则 6 | 无外部 main caller；Cloud 主流程私有，纯判定部分进 `W-SS-0` |
| 4 个 debug public API | anchor-only、need-drag-only、count-only、clean-tail-only | 无 main caller；本地保留到原子切换，不迁成 Cloud host/caller |
| package-visible | `detectSkillCountFromIf8Match(double[])` | 纯函数，进入 `W-SS-0` |
| package-visible | `resolveStaticTailStartIndex(StaticSkillSlotState[])` | 参数 enum 为 private，实际仅类内可用；算法进入 `W-SS-0`，不改既有 tail scanner |
| package-visible static | 可变 `IF8_LAYOUT_ROI` int[]、`SKILL_SLOT_BOX_SIZE` | Cloud 不暴露可变数组；返回 defensive copy / immutable nested value |

生产调用的 TMS 语义不归 SummonSkillService 所有：caller 在调用前置 `INTERACTING`，调用后按 success、ultimate
成功、UNKNOWN 失败分别更新布局缓存/长 cooldown/短 backoff，最后恢复 action state；成功 dequeue，失败移尾。
这些状态继续由 A 的 TaskMaintenance 切片持有，N 不搬、不重置、不另建第二权威。

| 状态种类 | HEAD 事实 | Cloud 不变量 |
|---|---|---|
| instance mutable | 没有 Service 自有可变业务字段；11 个依赖均 final | Spring singleton 保持 stateless；每 run/pass 状态不得落 singleton |
| static mutable | 2 组 `Point[]` 与 `IF8_LAYOUT_ROI` 数组对象理论可变 | `W-SS-0` 内 private 常量，向外只给 immutable/defensive view |
| per-call | deadline、deleted/inspected/dialog count、index、observed map、图片和临时点 | 放 exact taskRun 的 retained workflow state；不放 ThreadLocal/static/default state |
| cross-pass | expected count、next start、skip ultimate、cooldown/backoff | 仍由 TMS caller 提供/接收，不由 Service 猜测或持久化 |
| random point | HEAD 每个 click/hover/drag 调 CoordinateHelper 随机化 | 每个 canonical action 首次生成一次后 retained；重投/同 taskRun resume 原样复用，不重新抽点 |

### 2. 构造依赖迁移矩阵

| HEAD 依赖 | 用途 | Cloud owner / 前置 |
|---|---|---|
| `GameClientTracker` | base/geometry、全图与 ROI capture | explicit context + retained typed fact/capture；不得复制 tracker/HWND/title search |
| `CoordinateHelper` | 模板命中坐标 `/systemScaleRatio`、window origin、随机点 | CPU 算式 + capture 同帧 typed scale + retained random point owner；禁止假设 1.0 |
| `InputSequences` / `InputProvider` | 整 pass queue 所有权及 direct physical input | 仅 `CloudTaskServicePort` 的 non-mintable retained typed operation；Service 不见 queue/provider |
| `UICleanerService` | pass 释放独占区后轻量 interruption cleanup | 独立 typed post-pass collaborator；必须保持在 exclusive release 之后 |
| `DialogService` | 删除成功后已知小剧情框 fast click | cloud-safe typed story-dialog collaborator；不得内联/复制本地 input |
| `ObjectProvider<TaskMaintenanceService>` | 最多 3 次 maintenance broadcast 处理，避免构造环 | TMS 注入 typed callback；不得让 Service 反查 TMS bean |
| `WindowScopedTempPath` | raw/washed debug artifact | 已批准 tenant-scoped `CloudArtifactStore`；artifact 仅诊断，不成为判定权威 |
| `ImageProcessorService` | yellow count/wash | 复用 Cloud native in-memory 实现 |
| `SummonSkillCloudDecisionService` | 当前 active 路径的 hover/post-delete 识别 | 复用单一 in-process `SummonSkillRecognizer`；不保留 Cloud 自 HTTP/双 recognizer |
| `WindowTaskContextHolder` | task/window/debug metadata | 删除；只读 explicit `TaskExecutionContext` exact binding |

### 3. 模板、配置与时间语义矩阵

Cloud `images/template/zhaohuanshou` 已有下列 14 个资源，逐文件与 DHXY 根目录源 bytes/SHA256 相同，本切片不复制资源：

| 资源 | HEAD 用途 / 阈值 |
|---|---|
| `ZHS_shuxing.png` | 全局 vision 定位属性 anchor，`0.85` |
| `if8.png` | window-client ROI `[505,508,532,555]`，`0.80`；miss/缺失/capture fail 均回 6 |
| `status_sealed1.png` / `status_unobtained1.png` | static 52x52 slot，`0.78` -> LOCKED |
| `status_inactive1.png` | static slot，`0.78` 或平均 RGB 欧氏距离 `<=12.0` -> EMPTY |
| `status_sealed.png` / `status_unobtained.png` | hover yellow-tip -> LOCKED |
| `status_inactive.png` | hover yellow-tip -> EMPTY |
| `status_normal.png` | hover yellow-tip -> NORMAL |
| `status_high.png` / `status_ultimate.png` | hover yellow-tip -> KEEP |
| `click_ultimate_template.png` | yellow corner tip `0.78`，命中才点击 |
| `forget_confirm_button.png` | ROI `(552,494,102,15)`，`0.82`，命中点才确认 |
| `click_ultimate.png` | HEAD 本类未引用；保留资源但不纳入新判断 |

当前 DHXY 配置中 summon recognizer 为 `shadow=true / execute=true / execute-percent=100 / fallback=STOP`；
整类迁云后直接调用当前 Cloud `SummonSkillRecognizer`，切换门必须确认仍是相同 recognizer/STOP 语义，不能因
删除 HTTP wrapper 而重新启用本地顺序匹配 fallback。TMS 拥有且保持：cleanup enabled、20 分钟 interval、
UNKNOWN 5 分钟 backoff、start-immediately=false、ultimate 3 小时 cooldown。

| 时间/上限 | HEAD 精确值与语义 |
|---|---|
| 完整 pass deadline | wall clock `now + 40_000ms`；open 后、loop、delete/ultimate/backscan 前继续检查；不改为 pause-progress clock |
| open | Alt+O 后 900ms；anchor miss 仅再等 800ms 重试 1 次；drag 后 600ms；tab click 后 1000ms；仍见 anchor 再等 800ms 复查 1 次，不二次点击 |
| hover | move 后 700ms 才 capture |
| delete | 选 slot 后 300ms；delete 后 600ms；confirm 后 900ms；story click 内 move wait 80ms/click 120ms/post wait 350ms |
| ultimate | corner hover 700ms；click 后 2500ms 再 re-hover/inspect |
| per-pass caps | delete 最多 5；maintenance dialog handler 最多 3；slot 固定 6/8 |
| stop | 每个物理输入前、每个 sleep 后、长 loop/scan/deadline 分支均 checkpoint；Cloud typed unwind，不吞成普通 false |

### 4. HEAD 机械/截图/判定顺序（不可重排）

OCR：本类没有 OCR。所有视觉均为 screenshot + template/yellow pixel/image decision。

#### A. 完整 pass 与开面板

1. 外层取得一次 `submitExclusiveAndWait("summonSkill:cleanOnce")`，**整段 open + scan + hover + delete +
   dialog 都在同一个 input worker 临界区**；记录 wall-clock 40s deadline。
2. checkpoint -> Alt+O -> sleep 900 -> checkpoint。
3. fresh global vision -> `ZHS_shuxing` 0.85；miss 才 sleep 800/checkpoint 后 fresh global vision 重试一次；再 miss fail。
4. anchor 到窗口右边距离 `<337` 时，anchor 附近 jitter `+/-3,+/-3` 拖到 window-client `(518,428)`
   jitter `+/-45,+/-35` -> sleep 600/checkpoint -> fresh global vision 重找 anchor 一次；miss fail。
5. anchor + `(287,213)` jitter `+/-4,+/-3` 点击 skill tab，click delay 150 -> sleep 1000/checkpoint。
6. fresh global vision 查 anchor；若仍可见，sleep 800/checkpoint 后再 fresh capture 一次；仍可见 fail，且不 reclick。

#### B. 6/8 布局与 static scan

1. request expected count 是 6/8 时直接用；否则 capture window-client `[505,508,532,555]`，`if8` 0.80；
   hit=8，missing/capture fail/read fail/miss=6，无 retry。若 observed count 改变，强制本 pass
   `skipUltimateCornerCheck=false`。
2. 6 槽中心顺序：`(416,384),(334,430),(335,511),(420,561),(500,511),(500,432)`；
   8 槽：`(405,364),(339,407),(311,475),(338,541),(406,584),(475,540),(503,474),(474,406)`。
3. 每槽 52x52；合并 ROI 向外 padding 8，并裁到 1024x768；只做一次 fresh capture。
4. 每槽按 sealed1 -> unobtained1 -> inactive1 -> inactive color-distance 的顺序判定；前两项 LOCKED，
   inactive 命中/距离 `<=12` 为 EMPTY，其余 OCCUPIED；rect/异常为 UNKNOWN，整个 static scan fail-closed。
5. 从右向左：跳过 LOCKED；遇 EMPTY 回连续 EMPTY run 的首位；遇 OCCUPIED 回该位；全 LOCKED 回 `-1`
   并安全成功。本规则独立于既有 `SummonSkillTailBoundaryScanner`，后者不得改写。

#### C. 主 loop 与 hover

1. 每轮先 stop/deadline；在 `before-slot-N` 调 TMS broadcast handler（总 cap=3），handled 则同 index continue。
2. static EMPTY 不 hover；否则 slot center jitter `+/-2,+/-2` move -> sleep 700/checkpoint。
3. 同一 hover 状态下 capture screen-absolute `[hover.x+25, hover.y, +237, +123]`；当前 active Cloud recognizer
   yellow count `<120` 或 capture/read/decision fail 均 UNKNOWN；recognized status 按现有 recognizer返回。
4. UNKNOWN 且 handler cap 未满时以 `unknown-slot-N` 调 handler，handled 则同 index 重试；否则整 pass fail。
5. KEEP -> `nextStart=index+1` 安全成功；EMPTY -> ultimate corner；LOCKED -> backward scanner；NORMAL -> delete。

#### D. 删除与 post-delete

1. slot center jitter `+/-3,+/-3` click delay 120 -> sleep 300/checkpoint。
2. window-client delete `(484,602)` jitter `+/-4,+/-3` click 120 -> sleep 600/checkpoint。
3. fresh capture window-client ROI `(552,494,102,15)` -> `forget_confirm_button` 0.82；miss fail。
4. match point jitter `+/-6,+/-4` click 120 -> sleep 900/checkpoint。
5. 已知小剧情框：同基线矩形下沿附近 jitter，move -> 80ms -> click 120 -> 350ms/checkpoint；false fail。
6. deleted++；达到 5 立即成功 break，`nextStart` 保持当前 index；否则 fresh 52x52 post-delete capture：
   EMPTY -> ultimate；KEEP -> `index+1` 成功；LOCKED -> backscan；NORMAL/UNKNOWN -> fail。

#### E. LOCKED backward scanner

从 lockedIndex-1 向左，逐槽 stop/deadline + hover/inspect：NORMAL -> delete 并在同 index 做 ultimate；
KEEP -> `i+1` 成功；EMPTY -> 同 index ultimate；LOCKED -> 继续；UNKNOWN -> fail；扫尽 -> `0` 安全成功。
状态转移继续调用已迁 `SummonSkillTailBoundaryScanner`，N 不复制算法。

#### F. ultimate corner

1. request skip 为 true：不 capture/不 click，直接 completed。
2. deadline -> slot + `(26,-26)` jitter `+/-2` hover -> 700ms/checkpoint -> same-hover yellow-tip capture/wash。
3. yellow `<120` 或 `click_ultimate_template` 0.78 miss：completed/no click（不是 UNKNOWN）。
4. 命中才点击同 retained point，delay 120 -> 2500ms/checkpoint -> re-hover 原 slot -> 700ms -> fresh inspect。
5. NORMAL -> delete，再 fresh post-delete：EMPTY 成功 same index；KEEP 成功 next index；其它 fail。
   KEEP -> next index 成功；EMPTY/LOCKED -> “clicked but no skill” fail；UNKNOWN -> fail。

#### G. 结果与 post-pass

只有安全停止才 `success=true`；result 精确保留 nextStart、observed statuses copy、deleted/inspected、ultimate
clicked/succeeded/message。UNKNOWN 不得刷新长 cooldown。独占段释放后才调用 lightweight UI cleanup；该 cleanup 失败
不得倒写已完成机械 outcome。standalone `cleanTailNormalSkills()` 的成功后 `cleanUpAll()` 无生产 caller，不迁成新入口。

### 5. exact binding、retained identity 与 typed unwind

#### 5.1 context 绑定

Cloud 主 API 每次只接收同一个不可伪造 `TaskExecutionContext` 桥接的 exact：
`tenantId,userId,deviceId,clientSessionId,taskRunId,taskType,windowId,hwnd,titleFingerprint,registrationEpoch,
stopEpoch,runRevision`。每个机械调用前 `context.throwIfStopRequested()` + port revalidate；request 的 scope/window/
stopEpoch/runRevision 仍由 broker enqueue、final-dispatch、本地副作用前三道门复核。

pause/resume 只产生新 revision context；K current-slot 必须把**同一 taskRun 的 retained run state**重绑到新 context，
不能新建 workflow、occurrence、random point 或 action ID。旧 revision request 始终 stale；只有 verified NOT_EXECUTED
可由 retained adapter在新 context 下 renewal，Service 不自行重建。

#### 5.2 canonical business action address

- `phaseCode` 固定为 `summon-skill.clean-pass`。
- `occurrence`：同 taskRun 下 cleanup pass 的单调序号，由 package-private retained
  `CloudSummonSkillRunState` 首次开启 pass 时分配并保存；不是 wall clock、runRevision、loop index 或随机数。
- `actionSlot` 使用稳定语义键，不使用 UUID：
  `open.alt-o`、`open.anchor.0/1`、`open.drag`、`open.anchor.after-drag`、`open.skill-tab`、
  `open.verify.0/1`、`layout.if8`、`layout.static`、`maintenance.before.<slot>.<n>`、
  `maintenance.unknown.<slot>.<n>`、`slot.<i>.hover-tip`、`slot.<i>.delete.select`、
  `slot.<i>.delete.button`、`slot.<i>.delete.confirm.capture`、`slot.<i>.delete.confirm.click`、
  `slot.<i>.delete.story`、`slot.<i>.post-delete`、`slot.<i>.ultimate.hover-tip`、
  `slot.<i>.ultimate.click`、`slot.<i>.ultimate.post-click`、`post-pass.ui-cleanup`。
- 同一 slot 因 backscan/重新生成再次执行同类动作时，在 `actionSlot` 加稳定 workflow step ordinal，ordinal 由
  run state 在前一 outcome terminal 后推进；禁止覆盖旧 terminal action。
- `attempt` 完全由既有 `CloudTaskRunActionLedger` 管理；Service 不读取/递增/重置 attempt。
- requestId/actionId/captureId、digest 与 wire bytes 只由 retained ledger/typed handle 生成并保存；重投交相同 handle。
- 所有 jitter point 在对应 action address 首次 bind 前生成并存入 run state；同一 address redelivery/resume 复用
  exact point 和 exact bundle bytes。UNKNOWN/STOPPED/EXECUTED 后绝不换 ID；新 ID 仅新业务 step 或 verified
  NOT_EXECUTED renewal。

#### 5.3 typed outcome/unwind

| port outcome | capture/fact | input bundle | Service 行为 |
|---|---|---|---|
| `OBSERVED` | 唯一成功；还要 exact observed window/binding | 非法 | 才允许业务识别 |
| `EXECUTED` | 非法 | 唯一成功 | 才推进机械 step |
| `STOPPED` | stop | stop | 抛/传播 `TaskStopRequestedException`，不组装普通 failed result |
| `UNKNOWN` | 未知是否观察 | 未知是否执行 | 抛 service-specific typed unresolved；不 retry、不换 ID、不刷新 cooldown |
| `NOT_EXECUTED` | 无副作用 | 无副作用 | 先 typed transition unwind；仅 retained adapter 验证 ledger 后可 renewal，Service 不自动 retry |

如果 `NOT_EXECUTED` 时 context 仍 current，adapter仍返回显式 retry-required/unresolved 给上层调度；不得在 Service
内部 tight-loop。capture/fact 带错误 execution state、空 payload、digest/binding 不一致也统一 fail-closed typed unwind。
ordinary **已观察到的业务 miss**（模板 miss、yellow<120 等）才按第 4 节回 false/6/no-click，不能把 transport
UNKNOWN 降级成业务 miss。

### 6. 当前端口缺口与安全边界

1. **P1：整 pass exclusive ownership 未闭合。** HEAD 在一个 `submitExclusiveAndWait` 中跨 input、sleep、capture、
   template decision；当前 `CloudTaskServicePort` 只有分离的 `capture(...)` 与 `executeInputBundle(...)`，无法证明
   hover->wait->capture、delete->confirm capture 或整个 pass 期间没有其它窗口插入物理输入。主体实现前必须由
   authority owner 在**现有 port/retained handle**内增加 bounded typed exclusive interaction 能力，或给出经父级批准的
   等价协议；禁止 Service 暴露 raw begin/end token、poll/outcome、直接 queue/provider，也禁止靠分离调用声称等价。
2. **P1：DPI scale fact 缺失。** HEAD anchor/template 点为
   `round(matchLocal/systemScaleRatio)+windowOrigin`；现有 geometry fact 没有 scale。依父级对 B 切片既定结论，
   必须由 DHXY capture owner 在同帧生成 typed `systemScaleRatio` 并进入 capture wire/digest；禁止假设 1.0。
3. confirm/tooltip/static/post-delete capture 的 ROI 本身是 window-client 或由 retained screen point推导；所有最终
   v1 input 必须转成 `SCREEN_ABSOLUTE_PX`。转换只用同一 exact binding 的 geometry + same-frame scale。
4. Service 只见 `CloudTaskServicePort` 与不可铸 handle；不得新增 public raw request、completion、poll 或 outcome API。

### 7. tenant 容量、restart、诊断与 dormant reachability

- 所有 run/pass/action state 以 exact scope + taskRun + window tuple + stopEpoch 归属；禁止裸 windowId、静态 map、
  ThreadLocal/default state 或跨租户共享 artifact。
- 单 pass 固定 6/8 slots、delete<=5、dialog<=3；图片在判定后释放，artifact 走 B 的 tenant-scoped governor。
- 既有 action ledger hard cap 10,000 且当前不驱逐。20 分钟周期会持续增长，因此主体激活依赖 M 的 Full R0
  final-consumed retirement/monotonic frontier；cap 到达时必须在副作用前拒绝，不做“先执行后记账”或偷偷 eviction。
- 进程 restart 后 retained context/action/workflow 不 durable：旧 run/action fail-closed，必须重新注册/新 taskRun；
  不扫描并复活 orphan request、不从 artifact 名重建 ID、不冒充 durable resume。
- 诊断字段：safe scope identifiers、taskRun/window tuple、stopEpoch/runRevision、phase/actionSlot/occurrence/attempt、
  operation/state/code/elapsed；不打印图片 bytes、digest preimage、凭据或本机绝对路径。
- Spring Service 可构造但 host/caller 默认无 producer，保持 dormant；不加 scheduler/thread/poller/startup action。

### 8. 精确依赖 DAG、波次与写集

```text
W-SS-0 纯 CPU static-slot policy（独立，可先做）
   |
   +------------------------------+
                                  v
P-scale capture-time scale wire -> W-SS-1 retained summon workflow/action adapter
P-exclusive typed exclusive interaction -> W-SS-1
B artifact/template adapter (已批准) ----> W-SS-2 Cloud SummonSkillService
Cloud recognizer/image processor (已有) -> W-SS-2
A TMS typed maintenance callback --------> W-SS-2
cloud-safe story dialog + post-pass UI cleanup -> W-SS-2
M Full R0 ledger retirement -------------> W-SS-3 assembly/caller activation
K same-taskRun current context (已有) ----> W-SS-1/W-SS-3
W-SS-0 + W-SS-1 + prerequisites ---------> W-SS-2
W-SS-2 -----------------------------------> W-SS-3 atomic caller/assembly wiring
W-SS-3 + shadow/runtime evidence ---------> W-SS-4 whole-service cutover
```

| 波次 | 精确写集（提案） | 本轮状态 / owner 边界 |
|---|---|---|
| `W-SS-0` | **New only** `cloud/.../com/bot/dhxy/service/SummonSkillStaticSlotPolicy.java` | N 可在 DESIGN APPROVED 后编码；不改资源/现有类 |
| `P-scale` | capture outcome/wire/digest + DHXY capture owner | 独立跨仓切片；不得与 M wire 并发；非 N 当前写集 |
| `P-exclusive` | existing service port/retained handle + broker/local handler 的 typed exclusive operation | authority owner 独立设计；非 N 当前写集 |
| `W-SS-1` | proposed package-private retained workflow/adapter 与 typed unresolved | 待两 P1 和 M address/retirement合同；不得改 H/K/J |
| `W-SS-2` | **New** Cloud `SummonSkillService.java`；必要的 narrow typed business collaborator | 主体冻结；不得先复制本地 Service |
| `W-SS-3` | `CloudTaskRunAuthorityAssembly`、`CloudServiceConfiguration`、A 的 TMS caller/adapter | assembly/host/caller冻结，父级另派；一次原子 wiring |
| `W-SS-4` | 配置/切换记录与 runtime shadow/replay 证据 | 不在设计/编码波次执行生产切换 |

### 9. exact `W-SS-0` 可直接编码叶子

唯一文件：
`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillStaticSlotPolicy.java`。

设计为 package-private `final` CPU 类，无 Spring、I/O、capture、input、clock、random、retained ID、artifact 或 host：

1. private immutable constants：6/8 slot offsets、1024x768、52 box、8 padding、0.78 threshold、12.0 color distance；
   所有数组/Point 向外 defensive copy。
2. `detectSkillCountFromIf8Match(double[] match)`：逐字保留 `match != null ? 8 : 6`。
3. `relativeScanRoi(int skillCount)`：逐字保留 min/max、half-box、padding、`+1` 与 1024x768 clamp。
4. `inspect(BufferedImage rawImage, int skillCount, Templates templates)`：输入是该 ROI 的一次 capture 与三张已加载模板；
   模板任一 null -> exact failed message；按 slot rect 顺序调用现有 public `ImageFinder.find(...,0.78)`，再做
   inactive 平均 RGB 欧氏距离 `<=12.0`；invalid/RuntimeException -> UNKNOWN 并整 scan fail。
5. `resolveStaticTailStartIndex(State[])`：逐字迁移 HEAD static-tail 算法；不触碰
   `SummonSkillTailBoundaryScanner`。
6. nested package-private/private `Templates`、`State`、`StaticScanResult` 只表达该真实 CPU 边界；result 对 states/map
   defensive copy，observed map 仅写 LOCKED/EMPTY，message 与 HEAD 保持。
7. 复用已存在 `ImageFinder` 和 `SummonSkillSlotStatus`；不新增 wrapper 套 wrapper，不加载模板，不新增 tests。

`W-SS-0` 验收矩阵：6/8 ROI 与 slot rect 逐项等于 HEAD；sealed/unobtained/inactive precedence 不变；
template missing、raw null、rect 越界和 runtime exception 均 fail-closed；全 LOCKED=-1、连续 EMPTY 返回首位、
OCCUPIED 返回最右可动作位。父级批准后才写 Java，并由父级统一执行 Cloud 强制 package。

### 10. Worker N 自审结论（仅 QA）

- `W-SS-0` 写集不存在且独立，可直接进入父级 design review。
- 主体整类当前 **BLOCKED by prerequisites**：P1=2（exclusive pass、capture-time scale）；另有 M Full R0、typed
  TMS/dialog/UI collaborators 的顺序依赖。未把这些缺口伪装成 Service 内 fallback。
- 没有提出任何新增 retry/TTL/extra verify/park/业务判定；没有改变 phase、阈值、顺序、sleep、deadline、stop、
  delete cap 或 ultimate 语义。
- **无已批准业务差异；按基线等价迁移。**

## Parent Design Review #1 - W-SS-0 IMPLEMENTATION APPROVED / MAIN BLOCKED - 2026-07-13T01:50:24-04:00

父级已对照 DHXY HEAD `0114604e` 的 `SummonSkillService` 源码复核 6/8 槽位坐标、`if8` 判定、52px box、8px padding、ROI `+1`/1024x768 clamp、sealed/unobtained/inactive precedence、12.0 平均 RGB 距离与从右向左 tail 算法。Design #1 对该纯 CPU 边界的提取方向成立。

### W-SS-0：APPROVED，立即实施

唯一写集为 **Cloud 1 New / 0 Modify**：

- `src/main/java/com/bot/dhxy/service/SummonSkillStaticSlotPolicy.java`

必须严格按 Design #1 第 9 节实现：package-private `final`、纯 CPU；复用现有 `ImageFinder` 与 `SummonSkillSlotStatus`；不加载资源、不访问 Spring/I/O/capture/input/clock/random/retained ID/artifact/host；不修改 `SummonSkillTailBoundaryScanner`；不新增 tests；不运行 Maven，由父级统一构建。实现前先在本日志追加 `## Internal Worker N - CLAIMED - <timestamp>`，领取仅代表开始，不代表批准。

### 主体：BLOCKED，P0=0 / P1=2 / P2=0

1. 整 pass exclusive interaction ownership 尚未落入 retained typed authority；分离 capture/input 调用不能证明 HEAD 的整段独占语义。
2. capture-time typed `systemScaleRatio` 尚未进入同帧 wire/digest；不得假设 1.0。

因此 `SummonSkillService` 主体、workflow adapter、assembly/host/caller 与跨仓 wire 继续冻结。**无已批准业务差异；按基线等价迁移。**

## Internal Worker N - CLAIMED - 2026-07-13T01:51:19-04:00

## Internal Worker N - Implementation #1 - 2026-07-13T01:54:43-04:00

### 写集

- Cloud **1 New / 0 Modify**：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillStaticSlotPolicy.java`
- 文件 `11403` bytes，SHA256
  `a26f2b0edce13ac0ae3091b63d3b5434adc1261ec3be091024d942e0d34cf435`。
- 除本固定日志追加外未修改任何其它 Java、资源、文档、assembly/host/caller 或 A/B/M/J/H/K 写集。

### 实现方法

| 方法/类型 | 实现合同 |
|---|---|
| `if8LayoutRoi()` | 每次返回 defensive `int[] {505,508,532,555}` |
| `detectSkillCountFromIf8Match(double[])` | HEAD 精确语义：non-null=8，null=6 |
| `slotOffsets(int)` | 8 仅在 `skillCount==8`；其它走 6；数组和每个 `Point` 均新建 |
| `relativeScanRoi(int)` | HEAD 52px box/8px padding/右下 `+1`/1024x768 clamp 原式 |
| `inspect(raw,count,templates)` | templates/read fail-closed；一次 supplied ROI image；slot 顺序分类并生成 partial/complete observed map |
| `resolveStaticTailStartIndex(State[])` | null/empty/UNKNOWN=-1；从右跳 LOCKED；连续 EMPTY 回首位；否则回最右 OCCUPIED |
| `classify(...)` | sealed -> unobtained -> inactive template -> inactive RGB distance -> OCCUPIED；异常/越界 UNKNOWN |
| color-distance | 每个对齐位置计算平均 RGB 欧氏距离，`<=12.0` 命中 |
| nested values | immutable `SlotCenter`/`Templates`；`StaticScanResult` 构造与 accessor 均 clone states，map 用 `Map.copyOf` |

### 不变量与静态自审（仅 QA，不构成批准）

- package-private `final`，无 Spring annotation；仅依赖现有 `ImageFinder`、`SummonSkillSlotStatus` 与 JDK
  `Point`/`BufferedImage`/collections。
- forbidden surface 扫描零命中：无 I/O/resource loader、capture/input/window/HWND、clock/random、ThreadLocal、
  retained ID、remote/raw request、artifact、host、`SummonSkillTailBoundaryScanner`。
- 6 槽 static ROI 静态复算为 `[300,350,535,596]`；8 槽为 `[277,330,538,619]`；slot center、26 half-box、
  8 padding、右下 `+1` 与 HEAD `0114604e` 逐项一致。
- 模板缺失先于 raw image 检查，与 HEAD “先 load templates、后 capture/read” 顺序一致；UNKNOWN 时保留已观察的
  LOCKED/EMPTY partial map并返回 `actionIndex=-1`。
- 未加载/复制模板，未改变阈值、precedence、fallback 或 tail 算法；未修改既有
  `SummonSkillTailBoundaryScanner`。
- 按父级约束未新增 tests、未运行 Maven/编译、未启动任何运行面、未执行 git mutation。Cloud 强制 package
  由父级统一执行。
- 主体 `SummonSkillService`、workflow adapter、assembly/host/caller 与跨仓 wire 仍按 Parent Review #1 的
  P1=2 冻结；本交付不声称解除 main blockers。
- **无已批准业务差异；按基线等价迁移。**

DONE

## Parent Implementation Review #1 - W-SS-0 SOURCE APPROVED / PACKAGE PENDING - 2026-07-13T01:56:14-04:00

父级已逐方法对照 DHXY HEAD `0114604e` 的 static-slot 源码，结论为 **SOURCE APPROVED，P0=0 / P1=0 / P2=0**：

- 6/8 slot center、非 8 回 6、IF8 ROI、52px box、8px padding、右下 `+1` 与 1024x768 clamp 均一致；
- slot rect 由 relative ROI 原点正确抵消，sealed -> unobtained -> inactive template -> RGB distance `<=12.0` -> OCCUPIED 的优先级不变；
- template/raw/rect/runtime failure 均收敛为 fail-closed，UNKNOWN 不会产出可动作 index；partial observed map 只包含已确定 LOCKED/EMPTY；
- tail 算法保持从右跳 LOCKED、连续 EMPTY 回首位、否则回最右 OCCUPIED，全 LOCKED/UNKNOWN 为 `-1`；
- class/package/依赖面严格为纯 CPU 1 New，未加载资源、未触碰现有 scanner/assembly/host/caller/tests。

最终 Implementation APPROVED 等当前并行 Cloud 写入稳定后执行父级 fresh `mvn -q clean package`。SummonSkill 主体两项 P1
保持开放，本叶子不得被解读为主体放行。**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #2 - W-SS-0 FINAL APPROVED - 2026-07-13T02:20:21-04:00

父级 fresh Cloud `mvn -q clean package` exit 0；Surefire `4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`，shaded
JAR 实际含 `SummonSkillStaticSlotPolicy` 及其 immutable nested values。结合上一轮逐方法 source review，W-SS-0 最终结论为
**IMPLEMENTATION APPROVED，P0/P1/P2=0**。SummonSkill 主体的整 pass exclusive interaction 与 capture-time typed
`systemScaleRatio` 两项 P1 继续开放，host/caller 仍 dormant。**无已批准业务差异；按基线等价迁移。**

## Parent Task Brief - Internal R / Typed Whole-Pass Exclusive Interaction Design #1 - 2026-07-13T02:41:00-04:00

W-SS-0 已父级 FINAL APPROVED，capture-time typed `systemScaleRatio` 已交外部 A 实施。现把 SummonSkill 主体另一项独立 P1
交给 Internal Worker R：只设计 **retained typed whole-pass exclusive interaction authority**，本轮 Java/schema/resources/tests
零修改。先在本日志追加 `Internal Worker R - CLAIMED`（任务标题、领取时间、唯一写集），再交付 Design #1。

设计必须完整读取 DHXY HEAD `0114604e` 的 `SummonSkillService` 整 pass
`submitExclusiveAndWait` 边界、`InputSequences`/`InputActionWorker`，以及 Cloud 最新
`CloudTaskServicePort`、`CloudTaskRunActionLedger`、`RemoteGameCommandBroker`、本地 handler/queue。必须闭合：

1. **等价独占范围：** 从 pass 首个物理输入开始，跨 sleep/capture/template decision/delete-confirm/ultimate click，到
   post-pass cleanup 结束，不能被其它窗口物理输入插入；不得把它降级成多个普通 bundle，也不得在 Cloud 持本地 HWND/queue。
2. **retained authority：** exact tenant/user/device/clientSession + taskRun + window 四元组 + stopEpoch + runRevision；稳定
   exclusiveSessionId/action identity 只能由 retained ledger/assembly mint，Service 只持不可伪造 capability。禁止 public raw
   begin/end token、request/poll/outcome、callback 或 InputProvider。
3. **协议与状态机：** 给出 acquire/step/release 或等价的 typed operation 状态机、双仓精确 New/Modify FQCN、digest/wire 字段、
   broker enqueue/final-dispatch fence、本地副作用前 fence、同一 session 内 capture/input 的顺序与 correlation；UNKNOWN 不得
   当作已释放或业务 miss，NOT_EXECUTED 不得自动 retry/换 ID。
4. **容量和故障：** exact per-run/tenant/global hard cap；等待必须 bounded 且不启动新 thread/poller；pause/stop/revision change、
   disconnect、Cloud/DHXY restart、handler exception/interruption 时如何 fail-closed 和释放本地独占 owner，不能用 TTL/takeover/
   lease 复活旧 session，不能让 stale release 解锁新 owner。
5. **顺序依赖与并发写集：** A scale-wire 与 P Full R0 正在占用 remote/digest/schema 设计/实现面，本轮只给最终方法级合并顺序，
   不改任何文件；列出未来最小实施波次，确保第一波可独立编译但不接 host/caller/Service 主体。

不得改变 SummonSkill HEAD 的 phase/threshold/sleep/delete cap/tooltip/cleanup/retry/fallback 语义；不新增测试，不运行 Maven，
不启动任何运行面，不做 Git mutation。自审只算 QA，不构成父级批准。**无已批准业务差异；按基线等价迁移。**

## Internal Worker R - CLAIMED - 2026-07-13T02:51:17-04:00

- 已领取 `Parent Task Brief - Internal R / Typed Whole-Pass Exclusive Interaction Design #1`。
- 本轮唯一写集：仅 append-only 追加本固定日志 `docs/superpowers/plans/reports/2026-07-13-cloud-summon-skill-service-worker-n.md`；Java/schema/resources/tests 零修改，A scale-wire 与 P Full R0 并行写集全部冻结不碰。
- `CLAIMED` 仅表示领取，不表示 Design #1 完成或获父级批准。

## Internal Worker R - Design #1 - 2026-07-13T03:05:52-04:00

只读基线：DHXY `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`，Cloud
`navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`。`SummonSkillService.java`与
`InputSequences.java` 相对 DHXY HEAD 零 diff；整 pass 入口仍是
`submitExclusiveAndWait("summonSkill:cleanOnce", ...)`。双仓存在大量共享 dirty/untracked；本节未修改它们。

### 1. 等价的 whole-pass 独占范围

#### 1.1 证据与唯一合法边界

HEAD 的 `cleanSummonSkillsOnce(...)` 把 `cleanSummonSkillsOnceDirect(...)` 整体放在唯一
`InputActionWorker` 的 exclusive callback 中。因此 Alt+O、拖面板、点 skill tab、全部 sleep、同 hover
capture/模板判断、delete-select/delete-button/confirm capture/confirm click/剧情框、ultimate hover/capture/
click/post-click 检查之间，其它窗口的任何物理输入都无法插入。该性质不能用若干普通
`EXECUTE_INPUT_BUNDLE` 近似。

批准后的等价边界固定为：

```text
trusted retained authority ACQUIRE 成功
  -> 首个物理输入（Alt+O）
  -> 全部 HEAD sleep/capture/template decision/branch
  -> delete confirm/known story/ultimate click 及其 post-check
  -> 冻结 HEAD SummonSkillCleanupResult
  -> post-pass lightweight UI cleanup 的所有 capture/input step
  -> trusted retained authority RELEASE
```

- `ACQUIRE` 可以在首个物理输入前稍早占住 owner，但不得晚于 Alt+O 的副作用前门。
- 所有 sleep 仍是 session 内的 `SLEEP` input action：能与前一 click 同 bundle 就同 bundle，HEAD 中独立
  retry wait 则是只含一个 `SLEEP` 的 session step。不新增 WAIT 枚举或新时间语义。
- capture 由 DHXY exact-HWND owner 执行，Cloud 只做后续判定。Cloud capability 不保存 HWND 对象、
  `InputActionQueue` 引用、本地 lock 或 provider。
- template decision 在 Cloud CPU 中运行时，DHXY input worker 仍保留该 session owner，只接收同
  session 的有界 control-lane step；普通输入留在原 FIFO，不执行。
- post-pass cleanup 开始前先冻结 HEAD 机械结果；cleanup 失败不得倒写已完成结果，但 owner 仍在
  cleanup 结束后才 RELEASE。这只扩大输入隔离区间，不改 cleanup/result 业务语义。
- `cleanTailNormalSkills()` debug/standalone 入口不因本设计新增生产 caller。

### 2. retained authority 与不可伪造 capability

#### 2.1 exact owner key

一个 live session 的完整 key 是：

```text
tenantId/userId/deviceId/clientSessionId
+ taskRunId/taskType
+ windowId/nativeHandle/processId/playerIdentityEpoch
+ nonTerminalStopEpoch/runRevision
+ exclusiveSessionId
```

broker 的物理输入冲突 key 仍故意为 `(tenantId,userId,deviceId)`，不含 clientSessionId；这与现有
`InputFenceScope` 一致，避免换 session 后在同一台物理设备并行输入。但每个协议校验仍必须匹配
clientSessionId 和上述全 tuple，不得从 device key 推导 authority。

#### 2.2 铸造与 API 边界

- `exclusiveSessionId`、ACQUIRE/RELEASE/ABORT 的 requestId/actionId 在首次 admission 前一次性由
  `CloudTaskRunActionLedger + CloudTaskRunAuthorityAssembly` 绑定的 package-private authority 铸造。RELEASE 与
  ABORT identity 必须预铸，不能在 pause/exception 后用 stale context 新铸。
- 每个 CAPTURE/INPUT_BUNDLE step 的 identity 只由 Full R0 最终的
  `CloudTaskRetainedActionState` occurrence owner 铸造；Service 只得到 operation-specific opaque action handle。
- 对外 Service 只持有 `CloudTaskServicePort.ExclusiveInteractionCapability`（nested opaque type，无 ID/scope/
  revision/queue accessor）和对应 opaque step handle。它只能调用
  `captureInExclusiveInteraction(...)` / `executeInputInExclusiveInteraction(...)`。
- `ACQUIRE` / `RELEASE` / `ABORT` 只是 `CloudTaskExclusiveInteractionAuthority` 的 package-private 方法。
  `SummonSkillService`、host 与普通 caller 均看不到 begin/end token、sessionId、raw request、poll、wire outcome、
  callback、`InputProvider` 或 queue。
- Service 看到的 step result 是 `CloudTaskServicePort` nested non-forgeable typed projection：只暴露
  execution state/code 与 capture/input 业务必需字段，内部保留 exact wire outcome reference 供 P Full R0
  final-consume，不暴露 request/session identity。
- capability 绑定一个 exact runRevision；pause/resume 不替换 capability，而是终止本 pass。新 revision
  只能开新 retained session，绝不复活旧 owner。

### 3. typed 协议、状态机、摘要、fence 与精确写集

#### 3.1 单一 typed operation 与 closed wire

双仓 operation 枚举同位增加 `EXCLUSIVE_INTERACTION`。它是一个 operation，payload 内的
`commandKind` 只允许：

```text
ACQUIRE | CAPTURE | INPUT_BUNDLE | RELEASE | ABORT
```

common request 字段直接复用 P Full R0 最终 wire，顺序为
`contractVersion,operation,requestId,actionId,taskRunId,runRevision,observationMode,semanticAddress,window,stop,
timeoutMs,requestDigest`。`EXCLUSIVE_INTERACTION` 强制 `observationMode` key absent。

operation payload 顶层只有 `exclusiveSessionId,command`。`command` 是按 kind 闭合的 exact object：

| kind | exact command fields（声明/wire 顺序） |
|---|---|
| `ACQUIRE` | `commandKind` |
| `CAPTURE` | `commandKind,stepSequence,previousOutcomeDigest,captureId,region,imageFormat,capturePurpose` |
| `INPUT_BUNDLE` | `commandKind,stepSequence,previousOutcomeDigest,description,coordinateSpace,actions` |
| `RELEASE` | `commandKind,stepSequence,previousOutcomeDigest` |
| `ABORT` | `commandKind,lastKnownStepSequence,lastKnownOutcomeDigest` |

`stepSequence` 从 1 严格递增；ACQUIRE 隐含 sequence 0。首个 predecessor 为 64 个字符的 zero SHA-256，
后续 CAPTURE/INPUT/RELEASE 必须引用前一个 exact non-UNKNOWN outcomeDigest。ABORT 是不推进业务
sequence 的受限释放动作，只声明 Cloud 已知的最后 sequence/digest；本地可以回报实际更高的
last committed sequence，但不得因此执行或补做业务 step。

typed outcome payload 为固定 17-key closed object，顺序为：

```text
exclusiveSessionId, commandKind, sessionStatus, appliedStepSequence,
predecessorOutcomeDigest,
captureId, imageBytes, imageSha256, width, height, captureProvider,
systemScaleRatio, observedWindow,
actionCount, startedStepIndex, lastCompletedStepIndex, inputQueueRequestId
```

非适用字段必须是 explicit null。A 的 `systemScaleRatio` 顺序与 finite-positive 合同原样复用。成功
ACQUIRE/CAPTURE/INPUT 的 `sessionStatus=ACTIVE`，成功 RELEASE 为 `RELEASED`，成功 ABORT 为
`ABORTED`。任何 `UNKNOWN` 的 `sessionStatus` 必须是 null，不得宣称已释放。CAPTURE 成功使用
`OBSERVED`；其它成功使用 `EXECUTED`。非 CAPTURE 不得带图片字段，非 INPUT_BUNDLE 不得带
input progress 字段。

requestDigest 覆盖 P 的 required semanticAddress、exact run/window/stop/revision、sessionId 和整个 closed command。
outcomeDigest 覆盖 common + 上述全部非图片字段，并与现行 CAPTURE 一样仅排除 root
`imageBytes`；`imageSha256` 仍绑定 bytes。A 的 finite-binary64 canonicalizer 不改一字。

P Full R0 完成后 poll response 还必须增加 required canonical `brokerInstanceId`，对
`IDLE/COMMAND/FINAL_CONSUMED` 三种状态都存在。它只是 Cloud 进程代际，不是 lease。DHXY 在处理
payload 前先比对；发现 Cloud restart 时只 abort 当前 exact local session，不接管或恢复它。

#### 3.2 三层状态机

Cloud retained authority：

```text
DECLARED
  -> ACQUIRE_WAITING -> ACQUIRE_DISPATCHED
  -> ACTIVE(nextStep=1)
       -> STEP_IN_FLIGHT -> ACTIVE(nextStep+1)
       -> RELEASE_IN_FLIGHT -> RELEASED
       -> ABORT_IN_FLIGHT -> ABORTED

any dispatched uncertain result -> UNRESOLVED
any verified NOT_EXECUTED       -> FAILED_NOT_EXECUTED
```

`UNRESOLVED` 不进业务 branch、不换 ID、不发 normal RELEASE。只能读取 broker 的 exact late resolution（不重投）
或使用预铸 ABORT 释放 exact owner。`FAILED_NOT_EXECUTED` 同样不自动 renewal/retry；它只返回 typed
unwind 给上层调度。

Cloud broker/device fence：

```text
FREE
  -> ACQUIRE_RESERVED(cutoffSequence)
  -> ACQUIRE_DISPATCHED
  -> ACTIVE(sessionId)
  -> STEP_IN_FLIGHT | RELEASE_IN_FLIGHT | ABORT_IN_FLIGHT
  -> FREE only after exact non-UNKNOWN RELEASE/ABORT

UNKNOWN -> UNRESOLVED_FENCE_HELD
```

DHXY local owner：

```text
NONE
  -> ACQUIRE_SENTINEL_IN_NORMAL_FIFO
  -> ACTIVE(sessionId,nextStep=1)
  -> STEP_RESERVED -> ACTIVE(nextStep+1)
  -> RELEASING -> RELEASED tombstone
  -> ABORTING  -> ABORTED tombstone
```

同一 sessionId 一生只 ACQUIRE 一次；不存在 renew/reacquire。旧 RELEASE/ABORT 命中自己 tombstone 时只返
duplicate terminal；当前 owner 是其它 sessionId 时返 `NOT_EXECUTED/EXCLUSIVE_SESSION_MISMATCH`，严禁
clear 当前 owner。

#### 3.3 enqueue / final-dispatch / local side-effect fence

1. broker enqueue fence：先走 P Full R0 server-owned semantic frontier/detail admission，再校验 exact ACTIVE
   scope/taskRun/window/stopEpoch/runRevision、session capability、command kind/sequence/predecessor、live cap。ACQUIRE 获得
   device reservation 与 `cutoffSequence`；其后的 ordinary input 不得越过它。
2. broker final-dispatch fence：在现有 coordinator atomic authorization + dispatch mark 中再验全 tuple。ACQUIRE 只在
   cutoff 前的普通输入全部 terminal、设备无 input flight、无 ACTIVE session 时派发。ACTIVE 期只优先派发
   exact session control lane 的 step/release/abort；其它输入继续保留。
3. ABORT 是唯一 release-only 特例：它必须命中 broker 已保留的 exact session 与预铸 identity，可在
   PAUSED/STOPPING/revision-advanced 时通过 correlation-only release gate；它不得携带 capture/input，不得创建
   session，不得改业务 frontier。错 window/stopEpoch/taskRun/session 仍拒绝。
4. local handler fence：依次验 digest -> P Full R0 local frontier claim -> exact registry -> exact HWND/window ->
   session/sequence。CAPTURE 在 A 最终 scale-before -> 唯一 frame -> scale-after -> post-binding fence 中执行；INPUT 进
   session control lane。
5. input worker 最后门：ACQUIRE sentinel 在原 normal FIFO 中排队，因此它前面已接收的普通输入先完成。
   sentinel commit 后 worker 只取 active session lane；每个 action 副作前再验 exact registration revision/status/
   binding/session/sequence。禁止 nested queue/callback/InputProvider 外泄。
6. local sequence commit 与 operation-ledger terminal publication 不同时持有 coordinator/queue lock。在 acquire publication 间隙，
   尚未 terminal 的 acquire detail 保证 quiescence 计数非零；release 则先确认 worker 无副作在跑，再 clear owner。
   `RemoteOperationLedger.QuiescenceSnapshot.inFlightInputCount` 聚合 active exclusive owner，无需改 readiness wire。

#### 3.4 未来双仓精确 New / Modify

Cloud Brain New（6）：

1. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskExclusiveInteractionState`
2. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskExclusiveInteractionAuthority`
3. `com.yueyunfe.dhxy.cloudbrain.remote.ExclusiveInteractionCommandKind`
4. `com.yueyunfe.dhxy.cloudbrain.remote.ExclusiveInteractionSessionStatus`
5. `com.yueyunfe.dhxy.cloudbrain.remote.ExclusiveInteractionRequest`
6. `com.yueyunfe.dhxy.cloudbrain.remote.ExclusiveInteractionOutcome`

Cloud Brain Modify（17 Java + 1 schema）：

1. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteOperation`
2. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteRequest`
3. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteOutcome`
4. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteCommandEnvelope`
5. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteCommandOutcomeEnvelope`
6. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteCommandPollResponse`
7. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteProtocolDigests`
8. `com.yueyunfe.dhxy.cloudbrain.remote.OutcomeCode`
9. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunExecutionGate`
10. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunActionLedger`
11. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRetainedActionState`
12. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskServicePort`
13. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteGameClientPort`
14. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunCommandExecutor`
15. `com.yueyunfe.dhxy.cloudbrain.remote.RemoteGameCommandBroker`
16. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskServiceExecutionContext`
17. `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunAuthorityAssembly`
18. `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`

DHXY New（5）：

1. `com.bot.dhxy.cloud.remote.RemoteExclusiveInteractionCommandKind`
2. `com.bot.dhxy.cloud.remote.RemoteExclusiveInteractionSessionStatus`
3. `com.bot.dhxy.cloud.remote.RemoteExclusiveInteractionCommandPayload`
4. `com.bot.dhxy.cloud.remote.RemoteExclusiveInteractionOutcomePayload`
5. `com.bot.dhxy.input.action.InputExclusiveSessionCoordinator`

DHXY Modify（11 Java；schema 已在 Cloud 表列为同一共享文件）：

1. `com.bot.dhxy.cloud.remote.RemoteGameOperation`
2. `com.bot.dhxy.cloud.remote.RemoteOperationPayloadCodec`
3. `com.bot.dhxy.cloud.remote.RemoteProtocolDigests`
4. `com.bot.dhxy.cloud.remote.RemoteOutcomeCode`
5. `com.bot.dhxy.cloud.remote.LocalRemoteGameCommandHandler`
6. `com.bot.dhxy.cloud.remote.RemoteOperationLedger`
7. `com.bot.dhxy.cloud.remote.RemoteCommandPollResponse`
8. `com.bot.dhxy.cloud.remote.RemoteCommandPollingLoop`
9. `com.bot.dhxy.input.action.InputActionQueue`
10. `com.bot.dhxy.input.action.InputActionRequest`
11. `com.bot.dhxy.input.action.InputActionWorker`

`RequestContext` / DHXY `RemoteGameCommand` / outcome semantic echo 只复用 P Full R0 最终实现，R 不二次修改其地址
合同。`CloudTaskRunRetainedLifecycleActivationAdapter`、`RemoteFinalConsumptionCoordinator`、A 的 capture DTO、
`SummonSkillStaticSlotPolicy`、host/caller/具体 Service/resources/tests 全部不在 R 基础设施写集。

### 4. 容量、有界等待与故障收敛

#### 4.1 exact hard caps

| 状态库/队列 | per taskRun | per tenantId | global |
|---|---:|---:|---:|
| Cloud live session（WAITING/ACTIVE/terminal-in-flight） | 1 | 64 | 1000 |
| broker acquire reservation | 1 | 64 | 1000 |
| broker exact-session in-flight command | 1 | 64 | 1000 |
| broker route exclusive control slots | 1 per session | 64 per route | 已计入 1000 live cap |
| DHXY active physical owner | 1 | 1 | 1 |
| DHXY pending exclusive control lane | 1 per run | 64 | 64 |

所有 cap 在铸造新 ID、broker enqueue 或本地副作之前检查，满时返
`NOT_EXECUTED/BROKER_CAPACITY_EXCEEDED`，不 eviction、不抢占、不先执行后记账。P Full R0 的 retained
semantic slot/detail/control 上限仍是 owner `1000` / global `10000`、pending `64`；session terminal detail 只经其
no-gap final-consumed 退休，不建第二套 GC。

#### 4.2 bounded wait，无新 thread/poller

- ACQUIRE 在 Cloud broker 等待，不在 DHXY handler 等另一 session 释放。否则唯一本地 poller
  会被卡住，无法再取到前一 session 的 RELEASE。
- ACQUIRE 等待上限固定为 `120000ms` unpaused time，与现有 `InputActionQueue` legacy exclusive
  wait 一致。在派发前超时是 `NOT_EXECUTED/TIMEOUT`，释放 reservation，但不自动重投或换 ID；
  派发后超时是 `UNKNOWN/TIMEOUT`，device fence 保留。
- HEAD 40s pass deadline 从 ACQUIRE 成功、`cleanSummonSkillsOnceDirect` 等价起点开始，不包括排队等待；
  pause 期间不消耗 acquire wait，但 pause/revision 门会使尚未派发的 acquire fail closed。
- broker 只使用 P 最终的现有 pending future/long-poll availability signal，加固定 64-slot session lane。
  DHXY 只使用已有 remote poller 与唯一 input worker。worker 等待 session step 时以有界 condition wait
  重验 safety，不创建 scheduler/timer/thread/poller。
- ACTIVE session 本身没有 TTL。deadline 只决定某次同步调用的 typed outcome，绝不自动解锁
  local owner。

#### 4.3 failure matrix

| 场景 | local owner | Cloud/session 结论 |
|---|---|---|
| pause / stop / runRevision 变更 | input worker 在空闲 condition 或下一 action 门检测 exact status/revision，完成当前已开始的最小副作后 ABORT；未开始的动作不执行 | capability stale；预铸 ABORT 可走 release-only gate，不复活旧 revision |
| Cloud Service/caller exception | trusted authority `finally` 发 exact ABORT | pass typed unwind，不改 cooldown/业务 miss |
| handler exception/interruption | handler/coordinator `finally` 请求 ABORT；只在 worker 确认无 input side effect 在跑后 clear | 副作已可能开始则 `UNKNOWN`；不重投 |
| input worker exception/interruption | exact active owner -> ABORTED tombstone；若进程退出则由进程消亡释放 | Cloud 保留 UNKNOWN/late-final 门 |
| transport disconnect / explicit poller stop | `RemoteCommandPollingLoop.finally` 向 coordinator 发 exact abort；等待有界，未确认前不运行普通输入 | broker UNKNOWN 不当 RELEASED |
| Cloud process restart | 下一 poll response 的 `brokerInstanceId` 变化时 abort exact current owner，然后才处理新 response | 旧 broker/ledger 丢失，旧 session 不 rehydrate；必须新 clientSession/taskRun |
| DHXY process restart | JVM 本地 owner/queue 消失 | Cloud 运输 UNKNOWN；旧 registry 不存在，旧 command 副作前拒绝，不恢复 session |
| exact RELEASE response loss | local tombstone 保留 exact requestDigest/outcome；同 bytes 返 duplicate | Cloud 保留 fence，直到 exact late/duplicate non-UNKNOWN |
| stale RELEASE/ABORT 晚到 | 只命中同 session tombstone；当前 owner 不同则拒绝且不 clear | 无 takeover，无 lease resurrection |
| step `UNKNOWN` | worker 若已安全停止可由 exact ABORT 释放；但不把 UNKNOWN 本身当释放证明 | 不作模板 miss/不进 branch/不换 ID |
| step `NOT_EXECUTED` | 未产生该 step 副作；authority 使用预铸 ABORT 收尾 | 不 auto-retry/renew/新 actionId |

释放 local owner 与宣称业务 outcome 确定是两件事：只有 worker 确认已无物理副作在执行时才可
clear owner；Cloud 仍可以对最后一步保留 `UNKNOWN`，不得因 local 已 abort 而消费业务结果。

### 5. A/P 顺序依赖、方法级合并顺序与最小实施波次

#### 5.1 当前门与零并发写

- A 已追加 `Q-SCALE-WIRE Implementation #1`和双构建证据，但截至 R 落笔时还没有父级
  `Implementation APPROVED`。R 实施必须等该批准。
- P 已追加 `Full R0 Reconciliation #1`，仍等父级 design review；其 Java 尚未放行。R 不在 P 前修改
  semantic address/frontier/final-consumed control lane。
- 本 R 回合只写本日志。上述 New/Modify 均是未来父级批准后的闭包，不表示现在可写。

#### 5.2 最终方法级合并顺序

1. 先等 A 父级 APPROVED，记录其 8 文件最终 hash。R 在 DHXY
   `LocalRemoteGameCommandHandler#executeCapture/#emptyCapturePayload/#readSystemScaleRatioNow`的最终实现上提取一个真实
   mechanical capture boundary，普通 CAPTURE 与 exclusive CAPTURE 共用它；scale bracket/唯一 frame/
   failure mapping 顺序不动。
2. A 的双仓 `RemoteProtocolDigests#appendCanonical/#appendCanonicalDouble` 原样保留。R 只在 request/outcome
   typed dispatch 与 strict payload tree 上增 exclusive 分支，不重写数字 canonicalizer。Cloud
   `RemoteCommandOutcomeEnvelope` 保留 A capture exact-key/null reconstruction，另加 exclusive closed-key 分支。
3. 再等 P Full R0 design + implementation 父级 APPROVED。R 直接使用 P 最终
   `RequestContext.semanticAddress`、`CloudTaskRunActionLedger` occurrence/detail/frontier、
   `CloudTaskRetainedActionState` opaque handle、broker `ControlSlot[64]`/frontier、local-ledger claim/outbox。不复制 map、
   occurrence owner 或 final-consume lane。
4. 在 P 最终 `RemoteGameCommandBroker#dispatchAndAwait/#registerAuthorizedCommandLocked/#poll/
   #completeOutcome/#finishTerminalLocked/#validateAgainstPending/#terminalOutcome` 上按顺序增：
   device acquire reservation/cutoff -> active-session urgent lane -> session outcome transition。P 的 command/control 1:1 fairness 保留；
   只有 ACTIVE session 的 exact STEP/RELEASE/ABORT 在最多 HEAD 40s pass 内先于普通输入，防止 release 死锁。
5. DHXY 先在 P 最终 `RemoteOperationLedger#claim/#complete/#quiescenceSnapshot` 上增 session detail/active-owner
   aggregate，不改 ledger -> registry 顺序；释放 ledger monitor 后才调 input coordinator。然后在
   `LocalRemoteGameCommandHandler#handle/#executeOwnedCommand/#emptyOutcomePayload` 增 typed 分支，任何副作前保留
   P frontier claim。
6. 最后改 `InputActionQueue#take/await`、`InputActionRequest` typed kind/progress 与
   `InputActionWorker#runLoop/#handle/#execute`，将 acquire sentinel 与 active session lane 接入。不删旧 exclusive callback，
   不影响尚未迁移的本地 Service；无 active session 时旧 FIFO 字节级不变。
7. poll response 的 `brokerInstanceId` 必须在 P closed union 之后同波加到双仓；
   `RemoteCommandPollingLoop#runLoop` 先比对代际/必要时 abort，再分流 FINAL_CONSUMED/COMMAND。不得覆盖 P 的
   receipt-outbox 发送顺序。

#### 5.3 未来最小波次

| 波次 | 写集 | 可编译与激活门 |
|---|---|---|
| `R-X0` | **Cloud 1 New only** `CloudTaskExclusiveInteractionState.java` | 纯 JVM/package-private state+cap policy leaf，不引用新 wire enum，可独立编译；不改 assembly/host/caller/port/Service |
| `R-X1` | DHXY New `InputExclusiveSessionCoordinator` + Modify `InputActionQueue/InputActionRequest/InputActionWorker` | 只加 dormant owner/lane，无 producer 时旧 FIFO 不变；必须等当前 J dirty 写入稳定和父级放行 |
| `R-X2` | 双仓 remaining New DTO/enum + operation/sealed/envelope/digest/schema + broker/local handler/ledger/poller atomic cohort | 必须基于 A/P 父级 APPROVED 最终内容；双仓同波，不支持 mixed version；仍无 host/caller |
| `R-X3` | Cloud `CloudTaskExclusiveInteractionAuthority` + ledger/retained-state/gate/executor/port/execution-context/assembly dormant seam | Service port 只获 opaque capability；不铸 SummonSkill workflow，不激活 caller |
| `W-SS-1+` | N 日志已列 retained SummonSkill workflow、主 Service、cleanup collaborator、caller/assembly activation | 只在 R-X3 + A scale + P Full R0 + N 其它前置全部父级 APPROVED 后另行实施 |

`R-X0` 是本设计要求的首个可独立编译波次；它不使整 pass 可运行，不能被用来宣称 P1 已关闭。

#### 5.4 R 自审（仅 QA）

- 五项 brief 均有 exact boundary/key/state/wire/fence/cap/failure/file/wave；没有把 session 降级成普通 bundle，
  没有让 Cloud 持有 HWND/queue/provider/callback。
- 没有新增 TTL/takeover/lease/retry/thread/poller；UNKNOWN 不是 RELEASED/业务 miss，NOT_EXECUTED 不自动
  renewal/换 ID，stale release 不能解锁新 owner。
- HEAD phase/threshold/sleep/delete cap/tooltip/cleanup/retry/fallback/result 语义均保留。本 Worker 自审不构成
  `APPROVED`；Design #1 停止等待父级审查。
- 本轮 Java/schema/resources/tests 零修改，未运行 Maven/tests，未启动 application/server/host/Task/
  poller/UI/capture/input，未执行 git mutation。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #2 - BLOCKED / Repair #1 Published - 2026-07-13T04:18:00-04:00

父级对照 DHXY HEAD `0114604e` 的 `SummonSkillService.java:262-282`、当前 `InputActionScope.checkpoint()` 与
`InputActionWorker.waitIfPaused(...)` 复审。结论：**BLOCKED，P0=0 / P1=3 / P2=1**。

1. **P1：独占边界扩大，改变 HEAD 行为。** HEAD 仅在 `submitExclusiveAndWait` callback 内执行
   `cleanSummonSkillsOnceDirect`（271-274），`uiCleanerService.cleanLightweightInterruptions` 在独占返回后执行（281）。R
   却把 post-pass cleanup 纳入同一 exclusive session，并称不改变语义。影响是额外延长全局物理输入隔离，改变其它窗口可插入
   时点。返修必须让 RELEASE 发生在 HEAD callback 返回的等价边界，UI cleanup 继续在 release 后单独执行；其失败不得倒写
   已冻结结果。
2. **P1：pause/resume 被错误改成终止 pass + 新 session。** 当前 `InputActionScope.checkpoint()` 会等待 pause token，
   `InputActionWorker` 日志与代码明确“resumed; continuing same sequence”；R 设计却让 revision 变化终止当前 pass并在新 revision
   开新 session。影响是 pause 被变成业务失败/重开，可能重复已完成 click/delete。返修必须由本地 exclusive owner 在 PAUSED
   期间保留同一 retained pass/session并 park；resume 后只接受经 current-context/transition authority 重新绑定的新 revision
   capability，继续同一 workflow step。旧 revision capability/request 仍不得执行或解锁新 owner。
3. **P1：预铸 RELEASE/ABORT identity 与动态 payload 自相矛盾。** R 要求 admission 前预铸 requestId/actionId，但 RELEASE
   payload 依赖最终 `stepSequence/previousOutcomeDigest`，ABORT 依赖动态 `lastKnown*`，这些 bytes 在 pass 前未知。返修须区分
   stable session/action identity 与每个 exact request identity：要么把 RELEASE/ABORT 改为只含预先可知 session generation 的
   fixed payload，要么由 retained owner 预留业务 action identity、到合法 transition 才一次绑定 exact request bytes；任何重投
   必须保持完全相同 bytes，异常后不得换 ID。
4. **P2：`R-X0` 纯 state shell 不能独立关闭任何当前门。** 在上述三项合同未闭合前，新建 state 类只会固化错误状态机。
   Repair 批准前 `R-X0` Java 继续冻结；修订后必须列出其真实调用者、不可变 key、状态转移和为何可独立编译且不激活 producer。

### 下一任务：`R-EXCLUSIVE-DESIGN-R1`

重启后原 Internal R 会话已不可达，父级将由一个 replacement internal Worker 接管。替代 Worker 必须先在本日志追加
`Internal Worker R2 - CLAIMED`（任务标题、领取时间、唯一写集=仅本日志），再只追加短
`Typed Whole-Pass Exclusive Interaction Design Repair #1 Delta` 关闭上述四点。Java/schema/resources/tests/Maven/runtime 全冻结；
不得触碰 A/B/P 写集、host/caller 或 HEAD 业务阈值/顺序。Worker 自审只算 QA。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker R2 - CLAIMED - 2026-07-13T04:26:34-04:00

- 任务：`R-EXCLUSIVE-DESIGN-R1`
- 领取时间：`2026-07-13T04:26:34-04:00`
- 唯一写集：仅 `docs/superpowers/plans/reports/2026-07-13-cloud-summon-skill-service-worker-n.md`
- 边界：仅追加设计返修；Java/schema/resources/tests/Maven/runtime 全冻结，不做 review 或 Approved。

## Internal Worker R2 - Typed Whole-Pass Exclusive Interaction Design Repair #1 Delta - 2026-07-13T04:29:38-04:00

本 Delta 只替换 Design #1 中被 Parent Design Review #2 指出的四处冲突。依据为 DHXY HEAD
`0114604e` 的 `SummonSkillService.java:262-282`、当前 `InputActionScope.checkpoint()` / 
`InputActionWorker.waitIfPaused(...)`，以及现有 `CloudTaskRunCurrentContextSlot` / 
`CloudTaskRunAuthorityAssembly` 的 same-taskRun current-generation 切换边界。其余已写合同不重开。

### 1. P1：独占边界恢复为 HEAD callback 等价边界

- 独占 session 只覆盖 HEAD `submitExclusiveAndWait` callback 中的
  `cleanSummonSkillsOnceDirect(safeRequest)` 等价执行体。最后一个 direct step 结束后先冻结
  `SummonSkillCleanupResult`、`stepSequence` 与 `previousOutcomeDigest`，再绑定并完成 exact `RELEASE`；
  exact local owner 已确认释放，才构成 callback 等价返回。
- `uiCleanerService.cleanLightweightInterruptions("summon-skill:finish")` 明确不属于 session，不占 stable
  step slot，也不进入 RELEASE payload。它只在上述 callback 等价返回之后执行；其它窗口因此仍可在
  callback 返回与该 cleanup 之间插入输入，顺序与 HEAD `271-274 -> 281` 一致。cleanup 的成功/失败不得
  回写已冻结的 pass result、next slot、cooldown 或业务结论。
- 正常路径必须先得到 non-UNKNOWN `RELEASED` 再运行 post-pass cleanup；若 release 仍 `UNKNOWN`，不得假装
  callback 已返回或让 cleanup 越过仍可能存在的物理 owner。

### 2. P1：pause park 并继续同一 retained pass/session

- `exclusiveSessionId` 与稳定 pass key 跨 revision 不变。pause 只在下一物理副作用前的安全 checkpoint 把
  `ACTIVE(bindingGeneration=g, runRevision=r, nextStep=n)` 变为
  `PARKED_PAUSED(g,r,n)`；已开始的最小副作用先按当前基线收口。不得 RELEASE/ABORT、不得返回业务失败、
  不重置 pass deadline/jitter、不得新建 session/workflow occurrence，也不得重复 `1..n-1` 已完成步骤。
- resume 不复用旧 request，也不 reacquire。未来 typed control union 增加无 capture/input payload 的
  `REBIND_CURRENT_GENERATION`：Cloud 只能从同一 `CloudTaskRunCurrentContextSlot` 的 assembly-owned resume
  transition permit 取得 exact current context；本地只在 exact registration 已处于新 confirmed ACTIVE
  revision、window/stopEpoch/session 全匹配且 worker 仍持有该 parked owner 时，原子执行
  `PARKED_PAUSED(g,r,n) -> ACTIVE(g+1,r2,n)`。`sessionId` 与 `nextStep` 不变，只有 binding generation 与
  current revision 前进。
- `InputActionRequest` 的 immutable 旧 revision 不被改写；worker 后续 checkpoint 读取 session coordinator
  发布的 current-generation handle。旧 revision capability/request 永久 stale，在 Cloud enqueue、final dispatch、
  DHXY handler 副作用前门和 input worker 最后门均拒绝，且绝不能 RELEASE/ABORT/解锁当前 owner。
- stop/terminal 不是 resume：它只能由 retained authority 在 exact terminal correlation 下进入 ABORT 分支；
  pause 本身保持当前 `waitIfPaused(...); continuing same sequence` 语义。

### 3. P1：稳定 business action identity 与 exact request identity 分层

- session admission 只预留稳定的 `exclusiveSessionId`、`releaseActionId`、`abortActionId`；此时 RELEASE 的
  final sequence/digest 与 ABORT 的 last-known fields 尚未知，因此不铸 `requestId`、不构造 payload、
  不计算 digest/bytes。该预留由 retained authority 持有，Service/factory 无 mint 接口。
- 合法 RELEASE 或 ABORT transition 到来时，owner 才在同一锁内一次性冻结动态字段、铸该 action 唯一
  `requestId`，并绑定 `(actionId, requestId, exact context/revision/generation, closed payload,
  requestDigest, canonical wire bytes)`。绑定后对象和 bytes 不可替换；所有 transport 重投直接重交保留的
 同一 bytes，不重新调用 factory，不换 requestId/actionId，不重算动态字段。
- PAUSED 期间不提前绑定 RELEASE/ABORT，所以 current-generation handoff 后仍可在真正合法 transition 上
  首次绑定 current revision。若一个 request 已绑定后得到 `UNKNOWN`，状态进入 `UNRESOLVED` 并保留 fence，
  只接受 exact late resolution 或同 bytes 重投；即使 verified `NOT_EXECUTED`，本切片也不自动 renewal、
  不换 ID，只允许同一 retained bytes 的合同内重交。RELEASE 一旦绑定，ABORT 不得越过其未决结果另铸请求。

### 4. P2：`R-X0` 的真实 owner、key 与状态机

- `CloudTaskExclusiveInteractionState` 是 package-private、无 Spring、无 public constructor 的纯状态叶子。
  它的唯一真实调用者是后续 `R-X3` 的 `CloudTaskExclusiveInteractionAuthority`；该 authority 只能由
  `CloudTaskRunAuthorityAssembly` 构造，并从 current-context slot/retained state 获得 transition permit。
  未来 Service 只拿 typed opaque whole-pass handle，不能直接调用 state 或铸 identity。
- immutable `StableExclusivePassKey` 精确包含
  `(RemoteTaskRunScope 四元组, taskRunId, taskType, RemoteTaskRunWindow 四元组, stopEpoch,
  businessActionAddress/occurrence, exclusiveSessionId)`；`runRevision` 不在稳定 key 中，而在可前进的
  `(bindingGeneration,currentRunRevision)` 内。任何 scope/window/stopEpoch/session/address 变化均是 foreign key。
- 状态转移固定为：

  ```text
  DECLARED -> ACQUIRE_BOUND -> ACTIVE(g,r,nextStep=1)
  ACTIVE -> STEP_BOUND -> ACTIVE(g,r,nextStep+1)
  ACTIVE -> PARKED_PAUSED(g,r,nextStep)
  PARKED_PAUSED -> HANDOFF_BOUND -> ACTIVE(g+1,r2,same nextStep)
  ACTIVE -> RELEASE_BOUND -> RELEASED
  ACTIVE|PARKED_PAUSED -> ABORT_BOUND -> ABORTED
  any bound uncertain request -> UNRESOLVED_FENCE_HELD
  ```

  每个 `*_BOUND` 保存第 3 节的一次性 exact request binding；旧 generation transition、跨 session terminal
  request 与重复 step 均 fail closed。
- `R-X0` 只引用 JDK 与仓内现有 immutable run/scope/window value types，不引用尚未建立的 wire enum，也不改
  configuration/assembly/host/caller，因此可单文件编译。由于本波不注册 producer、无 assembly 修改且构造器
  package-private，单独落地也不会激活任何 runtime；它只是供 `R-X3` 使用的状态合同，不能单独宣称关闭运行门。
  Parent 对本 Delta 放行前，`R-X0` Java 继续冻结。

### 5. 修订后的实施约束

- 从既有 stable slot/address 表删除 `post-pass.ui-cleanup`；增加每次 pause occurrence 唯一的 typed
  `resume-generation-handoff` control address，但不增加业务 phase、retry、fallback 或 cleanup。
- `R-X0/R-X1/R-X2/R-X3` 仍遵循原 A/P 合并顺序；本 Delta 不授权任何 Java/schema/resources/tests/Maven/runtime
  动作，不触碰 A/B/P 写集、host/caller，也不启动 application/server/Task/poller/UI/capture/input。
- 无已批准业务差异；按 HEAD 基线等价迁移。本 Worker 自审仅为 QA，不构成 Approved。

### R2 Delta 补充：terminal `stopEpoch` correlation - 2026-07-13T04:31:28-04:00

上文 stable key 中的 `stopEpoch` 是 immutable `admissionStopEpoch`。“变化即 foreign key”仅指没有
assembly-owned exact terminal permit 的任意 mismatch。STOPPED 分支可按现有 coordinator/current-slot 合同验证
`exactTerminalBinding.stopEpoch == admissionStopEpoch + 1` 后绑定 retained ABORT；这只是 correlation-only release，
不改 stable key、不 reacquire、不创建新 session，也不使其它 revision/stopEpoch request 复活。

## Parent Design Review #3 - DESIGN APPROVED / R-X0 Published - 2026-07-13T04:34:00-04:00

父级对照 DHXY HEAD `SummonSkillService.java:262-282`、`InputActionScope.checkpoint()`、当前 current-context slot 与 authority
assembly 复审。Repair #1 已关闭：exclusive 只覆盖 callback、pause 保留同 session/nextStep、resume 只做 current-generation handoff、
stable action identity 与 late-bound exact request 分层、R-X0 owner/key/state 可独立编译。结论：
**DESIGN APPROVED，P0/P1/P2=0**。

绑定实现解释：HEAD 在 `submitExclusiveAndWait` 返回 false 时仍执行 post-pass UI cleanup。因此未来 outer workflow 在 ACQUIRE/STEP/
ABORT 得到**确定且已证明本地无 owner**的 non-UNKNOWN 失败结论后，也必须先冻结 failed result，再在独占外执行同一 cleanup；只有
UNKNOWN 或仍可能持有 local owner 时不得越过。该解释不新增业务动作，只补全 Repair 的 failure matrix。

### 下一任务：`R-X0-IMP1`

由同一 Internal Worker R2 继续实施，仅允许新建 Cloud
`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionState.java`。要求：package-private、无 Spring/I/O/
thread/clock/random/public constructor；实现 Repair #1 的 immutable stable key、generation/revision/nextStep 与合法状态转移/拒绝，
不修改 assembly/port/broker/host/caller/DHXY/schema/resources/tests。先在本日志追加
`## Internal Worker R2 - R-X0-IMP1 CLAIMED - <timestamp>`，再写 Java 与 implementation evidence；父级随后做源码审查并运行 fresh Cloud
`mvn -q clean package`。Worker 不做 Approved 判断。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker R2 - R-X0-IMP1 CLAIMED - 2026-07-13T04:36:30-04:00

- 任务：`R-X0-IMP1`
- 领取时间：`2026-07-13T04:36:30-04:00`
- 唯一 Java 写集：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionState.java`
- 证据写集：仅本固定日志；不修改其它 Java/DHXY/schema/resources/tests，不运行 Maven，不做 Git mutation 或 Approved 判断。

## Internal Worker R2 - R-X0-IMP1 Implementation #1 - 2026-07-13T04:42:14-04:00

### 精确交付

- 仅新建 Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionState.java`；
  写入前 `Test-Path=False`，写入后 scoped `git status --short` 为 `??`。未修改任何其它 Java、DHXY、
  assembly/port/broker/host/caller、schema/resources/tests。
- 文件为 `15245 bytes / 375 lines`，SHA-256
  `342BB8A15593373F30B939A4B2EDB453AAA7F6DBC5BC27217D49A46232BBB70D`。

### 合同落点

- `:17-61`：class package-private，初始与 transition snapshot 全字段 `final`；唯一 package-private constructor
  只接收 retained stable key 与 initial revision，无 Spring/I/O/thread/clock/random/public constructor。
- `:331-352`：immutable `StableExclusivePassKey` 精确绑定 scope 四元组、taskRunId/taskType、window 四元组、
  immutable `admissionStopEpoch`、现有 retained `ActionAddress`（含 occurrence）与 `exclusiveSessionId`；文本/数值
  使用现有 `RemoteProtocolValidation` canonical/fail-closed。
- `:92-263`：实现并仅实现批准状态机：
  `DECLARED -> ACQUIRE_BOUND -> ACTIVE`、`ACTIVE -> STEP_BOUND -> ACTIVE(nextStep+1)`、
  `ACTIVE -> PARKED_PAUSED`、`PARKED_PAUSED -> HANDOFF_BOUND -> ACTIVE(generation+1,newRevision,same nextStep)`、
  `ACTIVE -> RELEASE_BOUND -> RELEASED`、`ACTIVE|PARKED_PAUSED -> ABORT_BOUND -> ABORTED`，以及任一
  bound request 到 `UNRESOLVED_FENCE_HELD`。
- `:147-179`：handoff 只接受严格更大的 revision；完成时 session stable key 与 `nextStep` 不变，只前进
  binding generation/current revision，旧 generation/revision cursor 随即 stale。
- `:214-232`：terminal ABORT 只接受
  `terminalRunRevision == currentRunRevision + 1` 且
  `terminalStopEpoch == admissionStopEpoch + 1`；不改 stable key、不 reacquire/新建 session。
- `:281-329`：每个 transition 都先精确校验 stable key/generation/revision/nextStep 和 allowed status；foreign key、
  stale cursor、重复/越级 transition 均抛拒绝。handoff pending revision 与 unresolved bound-origin 具有构造期
  shape invariant，不能伪造为普通 ACTIVE/terminal state。
- `:354-374`：status 为该 package-private class 的 nested policy enum，不是 wire enum；本文件没有 producer、
  registration 或 runtime 接线。

### Worker QA 与门禁

- 静态禁用项扫描：Spring annotation、`java.io/java.nio`、Thread/Executor、Clock/Instant、系统时钟、
  Random/UUID/SecureRandom、`synchronized`、public class/record/enum/interface/public constructor 均零命中。
- 结构检查 14/14 为 true：stable scope/window/stopEpoch/address/session、generation/revision/nextStep、pause same-step、
  handoff generation advance、terminal successor、stale rejection、unresolved origin 均存在。
- 按父级任务，本 Worker 未运行 Maven/tests/runtime，未执行 git add/commit 或其它 Git mutation；fresh Cloud package
  由父级源码审查后执行。本 Worker 不作 Approved 判断，现停下等待父级 review。

**无已批准业务差异；按基线等价迁移。**

### R-X0-IMP1 evidence count correction - 2026-07-13T04:42:14-04:00

上节“结构检查 14/14”为记录计数笔误；fresh 静态脚本实际输出为 **15/15 true**（另有
`prohibited_count=0`）。此更正只修证据计数，不改变源码、SHA 或实现结论。

## Parent Implementation Review #4 - R-X0 FINAL APPROVED - 2026-07-13T04:46:00-04:00

父级逐方法审查唯一新增文件
`CloudTaskExclusiveInteractionState.java`（`15245 bytes / 375 lines`，SHA-256
`342BB8A15593373F30B939A4B2EDB453AAA7F6DBC5BC27217D49A46232BBB70D`）。结论：
**FINAL APPROVED，P0/P1/P2=0**。

- class/constructor/nested policy types 均为 package-private；所有 snapshot 字段 immutable，未引入 Spring、I/O、thread、clock、random、
  public raw mint 或 host/caller 接线。
- stable key 精确含 scope、taskRunId/taskType、window、admission stopEpoch、retained action address 与 exclusiveSessionId；每次 transition
  都校验 exact key/generation/revision/nextStep。pause 保持同 session/nextStep，resume 仅严格前进 revision+binding generation；terminal
  ABORT 只接受 current revision 与 admission stopEpoch 的 exact successor；bound UNKNOWN 进入不可继续的 retained fence。
- 父级 fresh Cloud `mvn -q clean package` exit 0：4 suites / 21 tests，failures=0、errors=0、skipped=0；shaded JAR
  SHA-256 `01D424454C0461EF9268B1FC149E28440B0EEB1E21435C843545CFADE325B0C0`。未触碰 DHXY Java，故本切片无需 DHXY compile。

本批准只覆盖 dormant R-X0 policy leaf；R1 owner/port/handler/whole-pass 接线仍须后续独立切片，不启动 host/Task/input。
**无已批准业务差异；按基线等价迁移。**
