# Cloud DialogChoiceMemoryService - External Worker A

> Append-only coordination log. External Worker A 只设计/实现，父级线程是唯一 reviewer/approval owner。

## Parent Task Brief #1 - `W-DCM-D1` - 2026-07-13T14:24:25-04:00

External Worker A 须在 `2026-07-13T14:44:25-04:00` 前先于本日志真实 EOF 追加 `CLAIMED`，字段必须包含：

- `task=W-DCM-D1`；
- `claimedAt=<带时区时间>`；
- `uniqueWriteSet=仅本 append-only 日志`。

领取后只做 committed HEAD `0114604e` 的 `DialogChoiceMemoryService` 整类 Cloud lift Design #1；父级
`DESIGN APPROVED` 前两仓 Java/Maven/schema/resources/tests、其它报告、CR 卡、host/caller 全冻结。任务可工作超过 20 分钟；
20 分钟只检查领取，不检查完成。任务只交 External A，绝不内部接管。

### 开工必读与事实锚

1. `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、迁移矩阵；
2. DHXY HEAD-clean `service/DialogChoiceMemoryService.java`（302 行）、`service/MemoryService.java`；
3. 全部真实 caller，至少 `WindowTaskRunner` route read/success/failure 路径以及 task-option caller；
4. Cloud `CloudServiceScope`、`CloudServiceStorage`、当前 authority assembly/Task-Service context；
5. 两仓 git status。保护全部 dirty/untracked，不回滚、不覆盖、不提交。

### 必须冻结的 HEAD 业务合同

- key 精确为 trim 后 `scope|action|contextKey`；任一 null/blank 就不读写；route context 精确为
  `fromMap->targetMap`，固定 `navigation/routeTransfer`；
- generic usable：`!disabled && successCount>0 && failCount<3`；stable task choice：
  `!disabled && consecutiveSuccessCount>=3 && consecutiveFailureCount<3`；
- success/failure 对所有计数、disabled、relative point、option/source/from/target 与时间字段的逐字段 mutation 顺序和结果；
- 单 owner 线性化（当前 synchronized）、LinkedHashMap 文档序、首次读 cache、missing/corrupt/load/save fail-soft、`.tmp` +
  atomic-move fallback；legacy `transfer_choice_memory.json` 只在新文件不存在时导入并按原规则改 key；
- 不新增 TTL、eviction、retry、额外验证、计数上限、自动清理或新业务 fallback。

### Design #1 必须交付

1. 完整 public API/caller/数据字段/分支矩阵，逐项说明 Cloud ownership 与 DHXY retained responsibility；
2. 单一真值方案：Cloud 在 exact `CloudServiceScope(tenantId,userId)` 下拥有 entry/counter/usability/disable 决策及 durable
   state；DHXY 可保留一次性 legacy 文件读取与 typed bootstrap transport，但 cutover 后不得继续成为第二业务权威；
3. legacy import 的稳定 identity/digest、重复投递/断线/UNKNOWN/restart 语义，以及“新 cloud state 已存在时绝不被旧本地文件覆盖”；
4. Cloud durable 写的原子性、并发线性化、进程重启恢复、tenant 隔离、损坏/磁盘失败的 baseline-equivalent fail-soft 映射；
   不得把 PNG artifact store 当 JSON 业务状态库，也不得信任 caller 提供 stateRoot/path；
5. caller 迁移 DAG、typed API/DTO（禁止 raw JSON/path/public storage bypass）、文件 New/Modify 表、波次拆分与每波构建门；
6. 明确本地永久保留的 capture/template/OCR/watcher/dialog inspect/pathing/movement/UICleaner/input safety 与本任务无关；
   不启动任何运行面，不写 Java，不跑 Maven。

设计须能直接进入实现，不用逐方法反编译，不写泛化架构论文。最后追加 self-QA 的 P0/P1/P2（仅 QA，不构成批准）并停止等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T14:30:06-04:00

- task=W-DCM-D1
- claimedAt=2026-07-13T14:30:06-04:00（领取截止 2026-07-13T14:44:25-04:00 内）
- uniqueWriteSet=仅本 append-only 日志。两仓 Java/Maven/schema/resources/tests、其它报告、CR 卡、host/caller 全冻结；不写 Java、不跑 Maven、不 git；保护全部 dirty/untracked。自审仅 QA，不构成父级批准。

## External Worker A - Design #1（W-DCM-D1，implementation-ready）- 2026-07-13

以 committed HEAD `0114604e` 的 `service/DialogChoiceMemoryService.java`（本 Delta 行号以 `git show` 导出为准，328 行）与 `service/MemoryService.java`（薄 facade）为唯一业务基线的整类 Cloud lift 设计。仅设计；两仓 Java/Maven/schema/resources/tests、其它报告、CR 卡、host/caller 冻结；两仓 dirty/untracked（DHXY 41 项 / Cloud 21 项）全保护不动。已读 AGENTS/DHXY_CONTEXT/ACTIVE_WORK CR271 顶部/迁移矩阵。

### 1. 完整 public API / caller / 数据字段 / 分支矩阵（Cloud ownership vs DHXY retained）

**HEAD public API（服务本体，全 `synchronized`）**：`findUsable(scope,action,contextKey)`(:62)、`findStableTaskChoice(...)`(:78)、`recordSuccess(scope,action,contextKey,fromMap,fromX,fromY,targetMap,relativeX,relativeY,optionText,source)`(:105)、`recordFailure(scope,action,contextKey,source)`(:152)、`findUsableRoute(fromMap,targetMap)`(:175)、`recordRouteSuccess(...)`(:179)、`recordRouteFailure(...)`(:191)。facade `MemoryService` 七个转发方法（:32/:37/:42/:57/:61/:65/:77）。

**上游 caller（HEAD 全量，均经 facade，共 7 点）**：

| # | caller:行 | facade 方法 | 路径 |
|---|---|---|---|
| 1 | WindowTaskRunner:2620 | `findUsableRouteDialogChoice(fromMap, targetKeyword)` | route read |
| 2 | WindowTaskRunner:3228 | `recordRouteDialogChoiceSuccess(...)` | route success |
| 3 | WindowTaskRunner:3246 | `recordRouteDialogChoiceFailure(...)` | route failure |
| 4 | WubeiDialogPreparationProvider:54 | `findStableTaskDialogChoice(...)` | task-option stable read |
| 5 | WubeiTask:1024 | `recordDialogChoiceSuccess(...)` | task-option success |
| 6 | WubeiTask:2489 | `recordDialogChoiceSuccess(...)` | task-option success |
| 7 | XiuluoTaskV2:5646 / :5846 | `recordDialogChoiceFailure` / `recordDialogChoiceSuccess` | task-option |

（generic `findUsableDialogChoice`(:32) 在 HEAD 无 facade 外上游 caller，仅由 route 变体经 `findUsableRoute` 复用同一读路径——迁移后 API 原样保留，不判定为死代码。）

**DialogChoiceEntry 17 字段**（:296-314）：`scope/action/contextKey/fromMap/fromX/fromY/targetMap/relativeX/relativeY/optionText/source`（业务载荷）+ `successCount/failCount/consecutiveSuccessCount/consecutiveFailureCount/disabled/lastSuccessAt/lastFailureAt`（计数/时间）。**分支矩阵（逐字冻结）**：key=trim 后 `scope|action|contextKey`，任一 null/blank → 读返回 empty、写直接 return（:251-259）；route contextKey=`fromMap->targetMap`、固定 `navigation/routeTransfer`（:261-265/:175-193）；usable=`!disabled && successCount>0 && failCount<3`（:316-320）；stable=`!disabled && consecutiveSuccessCount>=3 && consecutiveFailureCount<3`（:322-326）；success mutation 顺序=载荷 11 字段整体覆写→`successCount++`→`consecutiveSuccessCount++`→`consecutiveFailureCount=0`→`failCount=0`→`disabled=false`→`lastSuccessAt=now`→save（:121-139）；failure=entry 缺失即 return（不 create）→`failCount++`→`consecutiveFailureCount++`→`consecutiveSuccessCount=0`→`lastFailureAt=now`→`source` 覆写→`failCount>=3 ⇒ disabled=true`→save（:157-170）。

**ownership 判定**：entry/counter/usability/disable 决策、durable state、key 规则、mutation 顺序 → **Cloud**（单一真值）。dialog 的 capture/template/OCR/inspect、相对点的实际点击执行、UICleaner、pathing/movement/watcher、input safety → **DHXY 永久本地**（本任务零触碰）。DHXY 另保留一次性 legacy 文件读取 + typed bootstrap 上送（§3），cutover 后本地 JSON 不再是业务权威。

### 2. 单一真值方案（Cloud durable owner）

- **New Cloud `com.bot.dhxy.service.DialogChoiceMemoryService`**（整类 lift，per-`CloudServiceScope(tenantId,userId)` 实例，由 host 侧 per-scope 装配持有——同既有 per-scope Service 装配形状）：HEAD 302 行逻辑逐字迁移，仅替换持久化定位——`Path memoryPath` 改为构造注入的 `CloudServiceStorage.resolvePrivateFile("dialog_choice_memory.json")`（:52 trusted-root containment；**JSON 业务状态走 scoped 私有文件通道，不用 CR271 PNG artifact store**（:103-105 注释明示其为 PNG 专用）；stateRoot 由 host 装配，**不信任 caller 提供任何 path**——业务构造签名只收 `CloudServiceStorage`，不收 Path）。
- **线性化/恢复/fail-soft 逐字保**：单实例 `synchronized`（单 owner 线性化）；`LinkedHashMap` 文档序（:293）；首次读 cache（:206-231）；missing→空库、corrupt/load 失败→log.warn+空库（:226-230）、save 失败→log.warn 不抛（:246-248）；`.tmp` 写+`ATOMIC_MOVE`+非原子 fallback（:239-245）。进程重启恢复=重启后首读从 scoped 私有文件加载（与 HEAD 同语义）。tenant 隔离=scopeRoot 天然隔离（既有 containment 校验）。
- **不新增** TTL/eviction/retry/额外验证/计数上限/自动清理/新业务 fallback（HEAD 合同原文）。

### 3. legacy import（一次性 bootstrap，绝不覆盖已存在 cloud state）

- **稳定 identity/digest**：DHXY 侧一次性读取本地 `config/dialog_choice_memory.json`（若无则 `config/transfer_choice_memory.json`，导入时按 HEAD `migrateLegacyRouteKeys`(:267-282) 原规则改 key——该迁移逻辑随类整体上云，DHXY 只送原始 entries+来源标记），构造 typed import 请求：`scope 身份 + sourceFileName + sourceDigest(SHA-256 of exact bytes) + entries(typed DTO 列表)`。
- **幂等/重复投递/断线/UNKNOWN/restart**：Cloud 侧 import 仅当该 scope 的 durable state **从未初始化**（无 `dialog_choice_memory.json` 私有文件）时应用，应用后原子落盘（同 §2 save 路径）并从此视为已初始化；重复投递/断线重投/UNKNOWN 后重投 → 状态已初始化 ⇒ 返回 typed `ALREADY_INITIALIZED`（no-op，非错误）；同批次重投由 sourceDigest 相等佐证幂等，digest 不等亦不覆盖（仍 ALREADY_INITIALIZED）。**"新 cloud state 已存在时绝不被旧本地文件覆盖"由"仅未初始化可导入"结构性保证**——运行期产生的任何 cloud 写入都先于 import 落盘即视为已初始化。restart 无特殊路径（durable 文件即真值）。

### 4. durable 写原子性/并发/恢复/隔离/fail-soft 映射

已并入 §2：原子性=`.tmp`+ATOMIC_MOVE（fallback REPLACE_EXISTING，逐字 HEAD :239-245）；并发=per-scope 单锁 synchronized；恢复=首读加载；隔离=per-scope 私有文件（containment 校验拒绝越界文件名）；损坏/磁盘失败=baseline-equivalent fail-soft（load 坏→空库继续、save 坏→warn 继续，业务不中断、不重试）。

### 5. caller 迁移 DAG / typed API / 文件表 / 波次与构建门

**typed seam（禁 raw JSON/path/public storage bypass）**：DHXY↔Cloud 传输为 typed DTO 对（镜像模式=已 FINAL 的 W-QM artifact types）：`RemoteDialogChoiceEntry`（17 字段 typed 镜像，`@Value @Builder @Jacksonized`）、`RemoteDialogChoiceQuery/Record/RouteRecord`（按 facade 七方法参数成对）、`RemoteDialogChoiceImportRequest/Result`（§3）。Cloud 侧对应 record DTO 用 `RemoteProtocolValidation.required` 收紧。**不暴露 Path/stateRoot/raw JSON 字符串**；transport 复用既有 remote 通道形状（具体 envelope/gate 归共享 wire cohort，本设计不改其 schema——若该 cohort 尚未提供业务扩展点，则 W3 前置于其解冻，见波次）。

```
W1（可独立实施，零共享写集）：Cloud DialogChoiceMemoryService（整类+DTO+import 语义，per-scope 装配点除外可先 dormant）
  → W2：两仓 typed DTO 镜像对（同 W-QM-ARTIFACT-TYPES 模式，纯值类型，零 wire schema 修改）
  → W3（依赖门：共享 remote transport 业务扩展点 + host per-scope 装配解冻）：Cloud endpoint 接线 + DHXY MemoryService facade 改调 typed client + 一次性 legacy bootstrap
  → W4（caller cohort）：7 个上游调用点零改动（facade 签名不变，行为经 cloud）——预期零 caller diff，仅复验
```

| 仓库 | 精确 FQCN | New/Modify | 波次 |
|---|---|---|---|
| Cloud | `com.bot.dhxy.service.DialogChoiceMemoryService`（整类 lift，构造收 `CloudServiceStorage`）| New | W1 |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.DialogChoiceEntryDto` 等 typed DTO（含 ImportRequest/Result）| New | W2 |
| DHXY | `com.bot.dhxy.cloud.remote.RemoteDialogChoiceEntry` 等镜像 DTO | New | W2 |
| DHXY | `service.MemoryService`（facade 改调 typed client；七方法签名不变）| Modify | W3 |
| DHXY | `service.DialogChoiceMemoryService`（cutover 后按父级裁定转一次性 bootstrap reader 或删除——本设计不预删）| Modify/后续 | W3 |
| DHXY | WindowTaskRunner/WubeiTask/WubeiDialogPreparationProvider/XiuluoTaskV2（7 点）| 零改动 | W4 复验 |

**每波构建门**：Cloud `mvn -q clean package`（不 skip）+ DHXY `mvn -q -DskipTests compile`；W1/W2 不接 host/caller 保持 dormant。

### 6. 本地永久保留边界（与本任务无关，零触碰）

capture/template/OCR、dialog inspect/watcher、pathing/movement、UICleaner、input safety 全部 DHXY 本地 Runner 保留；本任务不启动任何运行面、不写 Java、不跑 Maven。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。Design #1 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Design Review #1 - BLOCKED P1=3/P2=1 - 2026-07-13T14:38:00-04:00（真实 EOF 权威块）

父级已对照 committed HEAD `0114604e`、当前两仓全树、CR271 P1-6 host 通过记录、现有
`CloudServiceConfiguration`/`CloudServiceStorage` 与全部 caller。业务阈值、key、mutation 顺序、本地永久保留边界均正确，
但以下实现前置尚未闭合，当前不得改 Java：

1. **P1 - 现状/文件表错误，会覆盖已批准在途基线。** Cloud 侧
   `com.bot.dhxy.service.DialogChoiceMemoryService` 与 `MemoryService` 已是 DHXY 当前/HEAD-clean 源的逐字副本，
   `CloudServiceConfiguration.dialogChoiceMemoryService(...)` 也已通过
   `storage.resolvePrivateFile("dialog_choice_memory.json")` 建立 tenant+user scoped 唯一 bean；CR271 已明确记录 P1-6
   host Review #2 APPROVED。W1 不能再列 `DialogChoiceMemoryService` 为 New，也不能把既有 trusted configuration->Path
   注入改成 business Service 持 `CloudServiceStorage`。**返修：**把这三份现有文件列为 frozen/reused baseline；只在确有
   bootstrap API 所需时给出最小 delta，不能复制第二类、第二 bean、第二 state path 或重开已通过的 scope hash/containment。
2. **P1 - record 写入缺 exactly-once/UNKNOWN 合同。**七个 facade 方法中 success/failure 写会改变累计/连续计数；普通 HTTP
   重投若第一次已在 Cloud 应用但响应丢失，第二次会再次 `++`。Design #1 只写“typed DTO/复用通道”，没有稳定 mutation
   identity、duplicate result、UNKNOWN/late outcome 与 retained ledger owner，不能保证基线一次调用只变更一次。
   **返修：**为每次业务 mutation 指定 retained stable operation id 与 bounded Cloud dedupe owner；同 id+同 digest 返回同一结果，
   同 id+异 digest 冲突，UNKNOWN 不铸新 id、不自动 retry/renew。read query 可有独立 typed fail-soft 结果，但不得把未知写入
   当成明确失败后再加一次计数。列清 cap、restart 代价、final consume/retirement 与 caller 如何保留 id；不得开放 raw request。
3. **P1 - bootstrap 与首个业务调用没有线性化。**“文件不存在才 import”不足以防同进程覆盖：现有 Service 是 lazy cache，
   首个 business mutation 可先改内存，而 baseline `save` 又允许失败不抛；此时文件仍不存在，稍后的 import 会把旧本地快照
   覆盖当前 Cloud 内存。**返修：**明确 per-scope host 激活顺序：load/existing-or-bootstrap 决议必须在该 owner 暴露任何 read/write
   前 exactly-once 完成，且与首次 mutation 持同一 owner 锁/状态机线性化；`ALREADY_INITIALIZED` 不能只凭文件存在，须覆盖
   loaded-empty、corrupt fail-soft、save-failed-but-live-cache 与 restart 情形。不得新增第二业务 store。
4. **P2 - caller/文件计数不精确。**表格编号写 7 点，但 Xiuluo 两个独立行号使实际 direct call site 为 8；W3/W4 还未列出
   exact transport/endpoint/retained-ledger 文件，因此当前不能称 implementation-ready。返修需给出真实现有/新增/修改表和 DAG，
   并明确哪些共享文件仍冻结等待。

保留已通过部分不重写：HEAD key/threshold/counter/time/fail-soft 语义、local capture/template/OCR/dialog watcher/pathing/
UICleaner/input safety、本地 legacy 只读候选、tenant+user scoped state path。Worker A 只追加 `Design Repair #1 Delta`，逐项关闭
上述 P1/P2；Java/Maven/schema/resources/tests/host/caller 全冻结。结论：P0=0/P1=3/P2=1，`BLOCKED`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T14:40:01-04:00

- task=W-DCM-D1 / Design Repair #1 Delta（关闭 Parent Design Review #1 的 P1×3/P2×1）
- claimedAt=2026-07-13T14:40:01-04:00
- uniqueWriteSet=仅本 append-only 日志。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不写 Java、不跑 Maven、不 git。已通过部分（HEAD 语义/本地保留边界/legacy 只读候选/tenant+user scoped path）不重写。自审仅 QA，不构成父级批准。

## External Worker A - Design Repair #1 Delta（W-DCM-D1）- 2026-07-13

关闭 `Parent Design Review #1` 的 P1×3/P2×1。仅设计，两仓全冻结；已通过部分（HEAD key/threshold/counter/time/fail-soft 语义、本地永久保留边界、legacy 只读候选、tenant+user scoped path）不重写。新增现状证据（逐行核对 Cloud 当前树）：`CloudServiceConfiguration.dialogChoiceMemoryService(CloudServiceStorage storage)`（:35，`new DialogChoiceMemoryService(storage.resolvePrivateFile("dialog_choice_memory.json"))`，javadoc :31 明示"construction does not read or create the file"）；Cloud `com.bot.dhxy.service.DialogChoiceMemoryService` 与 `MemoryService` 均已存在（父级确认为 HEAD 逐字副本，ctor/字段行号与 HEAD 一致核对无误）。

### F1（P1-1）：**撤回** Design #1 的 W1 "New DialogChoiceMemoryService（构造收 CloudServiceStorage）"

- **frozen/reused baseline（三份现有文件，本设计零重建）**：① Cloud `com.bot.dhxy.service.DialogChoiceMemoryService`（HEAD 逐字副本）；② Cloud `com.bot.dhxy.service.MemoryService`；③ `CloudServiceConfiguration.dialogChoiceMemoryService(...)`（既有 trusted configuration→`resolvePrivateFile`→**Path 注入**，业务不持 `CloudServiceStorage`——Design #1 的"构造收 Storage"一并撤回）。不复制第二类、不建第二 bean、不开第二 state path、不重开已通过（CR271 P1-6 host Review #2 APPROVED）的 scope hash/containment。
- 后续全部能力（F2 dedupe、F3 bootstrap）均为**既有类内的最小 delta**（同一单锁、同一 durable 文件），非新 store。

### F2（P1-2）：mutation exactly-once——stable operation id + bounded dedupe owner

- **id 铸造与保留**：DHXY 侧在**业务事件发生点**（8 个 caller 行为不变，id 在 facade `MemoryService` 的 5 个写方法内铸造）为每次 success/failure mutation 铸一次 `mutationId`（UUID）+ `payloadDigest`（canonical 序列化 payload 的 SHA-256）；transport 层任何重投恒复用同 id+同 payload；**caller 保留 id 的方式=facade 方法内一次业务调用一个 id**（caller 无感知、无 raw request 面）。
- **Cloud dedupe owner（最小 delta，挂既有单锁内）**：Cloud `DialogChoiceMemoryService` 内新增有界结构 `LinkedHashMap<mutationId, MutationReceipt(digest, typedResult)>`（同一 `synchronized` 线性化，非第二业务 store——dedupe 表是传输去重账，不是业务状态）。语义：**同 id+同 digest → 返回 retained typedResult（绝不二次 `++`）；同 id+异 digest → typed `MUTATION_CONFLICT`（fail-closed 不应用）；新 id → 应用基线 mutation（逐字 :121-139/:157-170）+ 记录 receipt**。
- **cap 与退位**：per-scope 固定上限（常量，建议 256），FIFO 退位最老 receipt（`removeEldestEntry` 形状）；溢出不拒绝业务写。退位后超窗旧重投不再去重——暴露面受 caller 合同钳制（见下），如实列为残余窗口。**无 TTL/LRU 语义进入业务状态**（退位只作用于传输账）。
- **UNKNOWN/late**：响应丢失→DHXY 记 typed UNKNOWN，**不铸新 id、不自动 retry/renew**（同 Full R0 纪律）；下一个真实业务事件=新 id。late 响应到达已 UNKNOWN 的调用→丢弃（业务已按 UNKNOWN 封存）。**read query 独立 typed fail-soft**：查询传输失败→empty（等价基线 miss），绝不把未知写入当明确失败再补一次 failure 计数。
- **restart 代价（如实）**：dedupe 表为内存态，restart 清空（restart-no-restore 一致）；restart 前已应用、响应丢失、restart 后重投的组合可能二次应用——缓解=caller 合同不自动 retry（UNKNOWN 即封存），该组合需要人工/上层显式重发同 id，正常运行面不存在；durable 业务文件始终只含已应用结果。**retirement**：scope retire/terminal 时随实例消亡；无独立清理任务。

### F3（P1-3）：bootstrap 与首个业务调用同锁线性化（exactly-once 初始化决议）

- **撤回** Design #1 的"文件不存在才 import"判据。改为**内存状态机字段 `initialized`（owner 单锁内）**，与首次 mutation 同一把既有 `synchronized` 锁：
- **per-scope host 激活顺序**：既有 per-scope 装配点在把 bean 暴露给任何业务 read/write **之前**，锁内调用一次最小新增方法 `synchronized BootstrapDecision bootstrapOnce(Optional<ImportSnapshot> candidate)`，决议逻辑：
  1. durable 文件存在 → 基线 `load()`（含 loaded-empty：entries 为空也算已初始化）→ `initialized=true`，candidate 拒绝为 `ALREADY_INITIALIZED`；
  2. 文件存在但 corrupt → 基线 fail-soft 空库（:226-230）**且 `initialized=true`**（运行史已存在，旧本地快照不得覆盖）→ candidate 拒绝；
  3. 文件不存在且 candidate 存在 → 应用 import（含 HEAD legacy key 迁移规则）→ save → `initialized=true`；save 失败仍 fail-soft（live cache 已含导入态，`initialized=true`——**save-failed-but-live-cache 情形显式覆盖**，后续 import 同样拒绝）；
  4. 文件不存在且无 candidate → 空库 `initialized=true`（live-empty）。
- 所有 public read/write 方法入口由激活顺序结构性保证仅在 `initialized=true` 后可达（host 不在 bootstrapOnce 返回前发布 bean 引用）；因此"首个 business mutation 先改内存、save 失败、稍后 import 覆盖"在时序上不可能——import 只存在于 bootstrapOnce 内，且与全部 mutation 同锁。
- **restart**：重启后重新走 bootstrapOnce（文件存在→路径 1；不存在→此 scope 从未有 durable 数据，路径 3 导入安全）。**不新增第二业务 store**。

### F4（P2）：caller 计数修正 + exact 现有/新增/修改表 + DAG

- **direct call site 修正为 8**：WindowTaskRunner :2620/:3228/:3246（3）、WubeiDialogPreparationProvider :54（1）、WubeiTask :1024/:2489（2）、XiuluoTaskV2 :5646/:5846（**2，分列**）。
- 修订表：

| 类别 | 文件 | 状态 |
|---|---|---|
| frozen/reused baseline | Cloud `service.DialogChoiceMemoryService` / `service.MemoryService` / `host.CloudServiceConfiguration`（bean :35）| 现有，仅按 F2/F3 做最小 delta（dedupe 结构+receipt 类型+`bootstrapOnce`，均在 DialogChoiceMemoryService 单锁内；Configuration 零改动，激活点归 host cohort）|
| New（W2）| Cloud `remote.DialogChoiceMutationRequest/Result`、`remote.DialogChoiceQueryRequest/Result`、`remote.DialogChoiceImportRequest/Result`（含 mutationId/payloadDigest 字段，`RemoteProtocolValidation` 收紧）；DHXY `cloud.remote.RemoteDialogChoice*` 镜像（`@Value @Builder @Jacksonized`）| 纯值类型，零 wire schema 修改（W-QM artifact types 同模式）|
| Modify（W3，依赖门后）| Cloud endpoint 接线文件、DHXY `service.MemoryService`（facade 五写两读改调 typed client，签名不变）、host 激活点（bootstrapOnce 调用）| **门：共享 transport/envelope/gate/handler cohort（Internal Z 在途）+ host 装配解冻**——exact endpoint/transport FQCN 待该 cohort 稳定后列出，当前如实冻结等待 |
| 零改动（W4 复验）| 8 个 caller 行 | facade 签名不变 |

- **DAG**：`W2 DTO 对（可先行，零共享写集）→ [门：transport cohort + host 装配] → W3 最小 delta + facade 接线 + bootstrapOnce 激活 → W4 caller 零 diff 复验`；每波双构建门（Cloud `mvn -q clean package` / DHXY `mvn -q -DskipTests compile`）。当前**不声称 implementation-ready 的部分**=W3 全部（依赖门明示）。

`无已批准业务差异；按 0114604e 基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。Design Repair #1 Delta 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Design Review #2 - BLOCKED / Design Repair #2 Published - 2026-07-13T14:52:00-04:00（真实 EOF 权威块）

父级对照当前 Cloud `DialogChoiceMemoryService`、`MemoryService`、`CloudServiceConfiguration:35-36`、
`CloudServiceHost.create:40-46` 与全量原子切换目标复审 Repair #1。撤回第二 Service/bean/path、修正 8 个 caller、把 bootstrap
放到 host 发布前，这些方向通过；但整体仍 **BLOCKED，P0=0/P1=2/P2=1**：

1. **P1：F2 为本来同进程的业务调用新增了第二 mutation ledger 和不必要的远程权威。** 全量切换后 Task caller、
   `MemoryService` 与 `DialogChoiceMemoryService` 都在同一 Cloud service graph，现有 facade 直接方法调用和 Service 的
   `synchronized` 才是基线；DHXY 本地不会继续代理每次 success/failure。F2 新建 `LinkedHashMap<mutationId,receipt>` 既与现有
   retained action/Full R0 权威重叠，又在 256 条 FIFO 退位或进程重启后明确允许旧请求再次 `++`，并不满足所声称的
   exactly-once。**返修：删除业务 read/write transport、mutation UUID/digest/dedupe 表与对应 DTO；8 个 caller 随其 Task cohort
   迁云后仍直调原 facade，生产只整体原子切换，不运行双写。**
2. **P1：F3 的 restart 结论与实际状态不一致。** 当前 `CloudServiceHost.create` 只有 refresh 后直接发布 host，没有 candidate
   参数/导入调用；Repair 又只用内存 `initialized`。若首次 import/live mutation 的 save fail-soft 后重启，marker 和 cache 一起
   消失，旧本地 candidate 会再次导入，不能称为“从未有 durable 数据所以安全”。返修须在以下单一简单路线中固定一条：
   推荐把旧 JSON 作为**生产切换前置迁移工件**，由可信迁移编排按 tenant/user scoped 目标预置并校验 digest，失败则不放行 host/
   Task 激活；业务 Service 保持当前逐字实现、无 runtime bootstrap API。若坚持 runtime bootstrap，则必须给出真实 host 构造/发布点、
   同一 durable 文件内的可恢复初始化证据与 save-failed/restart 行为，不得另建第二 store/marker 权威。
3. **P2：文件表仍列了一批不存在且不需要的 endpoint/transport DTO，并把 exact FQCN 留到未来。** 收缩后应给出真实结论：当前
   Cloud 两个 Service + scoped bean 已完成代码搬迁；本切片若采用推荐路线，Java 实施波为 0，剩余只是切换前 state artifact
   搬运/校验门与未来 Task caller cohort 的零业务差异复验。不要为证明“做了代码”制造 wrapper/协议壳。

### 当前任务 `W-DCM-D2`

External Worker A 在本日志真实 EOF 追加 `CLAIMED` 后只写 Design Repair #2 Delta；Java/Maven/schema/resources/tests/host/caller
继续冻结。保留已通过的 HEAD key/threshold/counter/time/fail-soft、现有 scoped path 与本地永久能力边界；只关闭以上两项 P1 和
一项 P2，给出最小真实 DAG/文件表/切换验收门。任务只交 A，绝不内部接管；Worker self-QA 不构成父级批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Claim Gate #2 - `W-DCM-D2`

External Worker A 须在 `2026-07-13T15:12:00-04:00` 前于本日志真实 EOF 追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=仅本日志）。20 分钟只检查领取，不检查完成；按时领取后可持续设计超过 20 分钟，Java仍冻结，任务仍只归 A，
绝不内部接管。

## External Worker A - CLAIMED - 2026-07-13T14:54:12-04:00

- task=W-DCM-D2 / Design Repair #2 Delta（关闭 Parent Design Review #2 的 P1×2/P2×1）
- claimedAt=2026-07-13T14:54:12-04:00
- uniqueWriteSet=仅本 append-only 日志。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不写 Java、不跑 Maven、不 git。保留已通过部分（HEAD key/threshold/counter/time/fail-soft、现有 scoped path、本地永久能力边界）。自审仅 QA，不构成父级批准。

## External Worker A - Design Repair #2 Delta（W-DCM-D2）- 2026-07-13

关闭 `Parent Design Review #2` 的 P1×2/P2×1。仅设计，两仓全冻结。

### G1（P1-1）：**撤回** Repair #1 F2 的全部 mutation transport 面——同进程直调即基线权威

- **撤回**：业务 read/write transport、`mutationId`（UUID）/`payloadDigest`/有界 dedupe 表（`LinkedHashMap<mutationId,receipt>`）、`MUTATION_CONFLICT`/UNKNOWN 传输合同，及其全部 DTO（`DialogChoiceMutationRequest/Result`、`DialogChoiceQueryRequest/Result` 与 DHXY 镜像）。根因承认：F2 建立在"DHXY 本地继续代理每次 success/failure 的远程调用"的错误前提上；**全量原子切换后 8 个 caller 随各自 Task cohort 迁云，与 `MemoryService`、`DialogChoiceMemoryService` 同处一个 Cloud service graph**——facade 直接方法调用 + Service 既有 `synchronized` 就是基线线性化权威，一次业务调用恰应用一次（同进程无重投面），exactly-once 天然成立，无需也不得有第二 mutation ledger（与 retained action/Full R0 权威重叠、且 FIFO 退位/重启后仍会二次 `++` 的缺陷一并消除）。
- **切换纪律**：生产只做**整体原子切换**（caller cohort 迁云与 memory 归属同时生效），**不运行双写**、无 DHXY→Cloud 业务代理期。

### G2（P1-2）：**撤回** F3 runtime bootstrap/`initialized` 状态机——采纳推荐路线：legacy JSON 为切换前置迁移工件

- **撤回**：`bootstrapOnce(...)`、内存 `initialized` 标志、`ALREADY_INITIALIZED` 结果与 host 激活序内的 candidate 参数（实况核对确认 `CloudServiceHost.create:40-46` 仅 refresh 后直接发布，无此挂点；且内存 marker 在 save fail-soft + restart 组合下确实丢失——父级判定成立，restart 结论撤回）。
- **采纳推荐路线（固定为唯一路线）**：旧 JSON 是**生产切换的前置迁移工件**，由可信迁移编排（切换 runbook 的一步，非业务 Java）执行：
  1. 源=DHXY 本地 `config/dialog_choice_memory.json`；若仅存 legacy `config/transfer_choice_memory.json`，编排先按 HEAD `migrateLegacyRouteKeys`（:267-282）等价规则**离线转换为新格式**（Cloud bean 用单 Path 构造 :45-47，`legacyTransferMemoryPath=null`，云上无 legacy 读取分支——工件必须已是新格式）；
  2. 目标=该 tenant/user 的 `CloudServiceStorage.resolvePrivateFile("dialog_choice_memory.json")` 实际 scoped 路径（既有 containment 权威，编排不越过）；
  3. **校验门**=复制后源/目标 SHA-256 逐字节一致 + 目标 JSON 可被基线 `MemoryFile` 形状解析；任一失败→**不放行该 scope 的 host/Task 激活**（切换中止，人工处置）；
  4. 无源文件（全新租户）→跳过预置，空库=基线 load 缺失文件行为（:213-216），照常放行。
- 业务 Service **保持当前逐字实现、零改动、无任何 runtime bootstrap API、无第二 store/marker 权威**；restart 语义=基线（durable scoped 文件即唯一真值，首读加载）。迁移工件门发生在 host 发布之前，与首个业务调用的竞态在时序上不存在。

### G3（P2）：真实收缩结论——本切片 Java 实施波 = 0

- **撤回** Repair #1 文件表中全部 endpoint/transport/DTO 行（Cloud `remote.DialogChoice*` 六类与 DHXY `cloud.remote.RemoteDialogChoice*` 镜像）——不存在且不需要，不造 wrapper/协议壳。
- 真实结论表：

| 类别 | 内容 | 状态 |
|---|---|---|
| 完成态 frozen | Cloud `service.DialogChoiceMemoryService`（HEAD 逐字）+ `service.MemoryService` + `CloudServiceConfiguration.dialogChoiceMemoryService`（:35-36 scoped bean）| **代码搬迁已完成，零修改** |
| 切换门（非 Java）| G2 迁移工件预置+digest/形状校验+失败不放行 | 生产切换 runbook 步骤，owner=切换编排 |
| 未来复验（非本切片写集）| 8 个 caller（WindowTaskRunner:2620/:3228/:3246、WubeiDialogPreparationProvider:54、WubeiTask:1024/:2489、XiuluoTaskV2:5646/:5846）随各 Task cohort 迁云时零业务差异复验（facade 签名不变）| 归各 Task cohort |

- **DAG（一行）**：`[完成态：两 Service + scoped bean] → 切换前迁移工件门（runbook）→ Task caller cohort 迁云（各 cohort 写集）→ 8 点零 diff 复验 → 原子切换`。本切片无构建波（Java=0），无新验收门可造。

`无已批准业务差异；按 0114604e 基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。Design Repair #2 Delta 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Design Review #3 - FINAL APPROVED / Cutover Runbook Task Published - 2026-07-13T15:00:00-04:00（真实 EOF 权威块）

父级对照当前 Cloud `DialogChoiceMemoryService`、`MemoryService`、`CloudServiceConfiguration:35-36` 与
`CloudServiceHost.create:35-47` 复审 Repair #2。G1 已删除本来同进程业务调用不需要的 mutation transport、UUID/digest、
dedupe ledger 与全部 DTO；8 个 caller 随 Task cohort 迁云后继续直接调用原 facade，生产只整体原子切换且不双写。G2 已删除
不可恢复的 runtime `initialized/bootstrapOnce`，把旧状态固定为 host/Task 激活前的可信迁移工件门；现有两个 Service 与 scoped
bean 保持逐字实现。G3 的 Java 实施波为 0，真实文件表与 DAG 不再制造 wrapper。

结论：**FINAL APPROVED，P0=0/P1=0/P2=0**。本批准关闭 DialogChoiceMemoryService 代码迁移边界；它不执行生产复制，
也不代表 8 个 Task caller 已迁完。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

### 下一任务 `W-DCM-RUNBOOK-IMP1`

External Worker A 须在 `2026-07-13T15:20:00-04:00` 前于本日志真实 EOF 追加 `CLAIMED`，随后新建
`D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-13-dialog-choice-memory-cutover-runbook.md`。唯一写集为该新文档与
本日志；两仓 Java/Maven/schema/resources/tests/host/caller 全冻结。runbook 必须给出 canonical 源文件选择、tenant/user scoped
目标的可信解析方式、复制前后 bytes+SHA-256+JSON shape 校验、原子发布/失败不激活/回滚点、无源新租户语义与审计证据模板；
不得实际复制生产数据、不得写凭据。若只有 legacy `transfer_choice_memory.json`，不得用文本脚本臆造私有迁移逻辑：须明确
fail-closed，并登记独立可信转换工具为切换前 blocker。完成后只做 Markdown 自检，不跑 Maven。Worker self-QA 不构成父级批准，
任务仍只归 A，绝不内部接管。

## External Worker A - CLAIMED - 2026-07-13T15:03:32-04:00

- task=W-DCM-RUNBOOK-IMP1（领取截止 2026-07-13T15:20:00-04:00 内）
- claimedAt=2026-07-13T15:03:32-04:00
- uniqueWriteSet=① New `docs/superpowers/plans/2026-07-13-dialog-choice-memory-cutover-runbook.md`；② 本 append-only 日志。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不实际复制生产数据、不写凭据、不跑 Maven（仅 Markdown 自检）。self-QA 不构成父级批准。

## External Worker A - W-DCM-RUNBOOK-IMP1 交付证据 - 2026-07-13

- New `docs/superpowers/plans/2026-07-13-dialog-choice-memory-cutover-runbook.md` 已创建；写集恰为该文档+本日志，未触任何 Java/schema/resources/tests/host/caller，未复制生产数据，未含凭据，未跑 Maven。
- 覆盖任务全部要求：§1 canonical 源选择（含"两者皆存在/皆无"情形）；legacy-only 情形 fail-closed 并登记 `BLOCKER-DCM-LEGACY-CONVERT`（独立可信转换工具为切换前 blocker，不用文本脚本臆造迁移）；§2 scoped 目标可信解析（复用生产 `CloudServiceStorage.resolvePrivateFile`(:52)/`hashScope`(:83)/ensure-directory(:116) 代码路径，操作者零手工拼 hash）；§3 bytes+SHA-256+JSON shape（Jackson 同配置 MemoryFile/17 字段形状）三重校验 + `.tmp`→ATOMIC_MOVE 原子发布 + 发布后复核；§4 失败不激活/回滚点（激活前删除即完全回滚、激活后无 runbook 级回滚）；§5 逐 scope 审计证据模板；无源新租户=NO_SOURCE_SKIP 照常放行。
- Markdown 自检：标题层级/表格/代码块闭合完整，无生产实值。

self-QA：**P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。W-DCM-RUNBOOK-IMP1 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Runbook Review #1 - BLOCKED / Repair Published - 2026-07-13T15:10:00-04:00（真实 EOF 权威块）

父级完整复核新建 runbook，并对照当前 Cloud `CloudServiceStorage:23-170`、
`CloudServiceConfiguration:26-36` 与 `DialogChoiceMemoryService:233-248,292-315`。canonical 新格式优先、legacy-only
fail-closed、无源新租户语义、激活前校验和不实际复制生产数据的方向通过；但 runbook 暂 **BLOCKED，P0=0/P1=2/P2=2**：

1. **P1：发布步骤会在竞态下覆盖既有云状态。** §0/§3.1 声明“绝不覆盖”，但 §3.5 在
   `ATOMIC_MOVE` 失败后明确使用 `REPLACE_EXISTING`。目标预检查与 move 之间没有 scope 级排他权威；另一操作者或 host
   可在窗口内创建目标，fallback 会直接覆盖，§4 随后的“删除目标回滚”还可能删除并非本次迁移拥有的文件。业务
   `DialogChoiceMemoryService.save:239-245` 的 live replace 语义不能作为首次迁移发布的 no-clobber 依据。**返修条件：**固定
   一个真实 scope cutover lease/activation freeze；使用唯一 staging 文件；在 lease 内重验目标不存在；发布必须是
   create-if-absent/no-replace，任何 target-exists 或平台无法证明原子 no-replace 都 fail-closed，且 cleanup 只能删除由本次
   operation token 证明拥有的 staging/target，禁止 `REPLACE_EXISTING`。
2. **P1：tenant/user 目标解析与源到 scope 的绑定目前只是不可执行选项。** `CloudServiceStorage.hashScope` 是 private，
   `establishRealScopeRoot/resolveWithinRealScope` 是 host package-private；runbook 的“切换编排组件调用 ensure-directory”没有
   现存可调用入口，备选“只读运维入口”也尚不存在。操作者手填 `<tenantId,userId>` 仍可把同一全局本地文件放入错误 scope。
   **返修条件：**只保留一个真实可落地的 trusted resolver/manifest owner：scope 必须来自已认证 host/control-plane inventory，
   输出不可手改的 `operationId + scope + resolved target` manifest；同时明确旧文件是全局 bootstrap seed 并复制到 manifest
   中每个获准 scope，或给出能证明它只归属单一 scope 的现有证据。若该 resolver 尚未实现，登记明确的 cutover blocker，
   不得把不存在的 A/B 选项写成可执行步骤。
3. **P2：JSON shape 事实写错。** 当前 `DialogChoiceEntry:296-315` 有 **18** 个持久字段，不是 17。返修不得继续手工维护
   字段计数；应明确用当前构建产物中的 `DialogChoiceMemoryService.MemoryFile`/`DialogChoiceEntry` 与同配置 ObjectMapper
   做真实绑定，并验证 `entries != null`、每个 value 非 null、关键字符串/坐标/计数类型可绑定。未知字段容忍保持基线。
4. **P2：staging 的崩溃恢复/耐久证据不足。** 固定 `<目标>.tmp` 可与旧残留或并发运行冲突，且发布前没有写明 flush/force
   与重启后复验。返修须使用 operationId 唯一 staging，写完后 force、发布后重新打开复验 bytes/SHA/shape；进程崩溃后
   未完成 operation 只能由同一 manifest/token 识别和清理，激活门保持关闭。

### 当前任务 `W-DCM-RUNBOOK-IMP1-R1`

External Worker A 立即继续原任务，只允许修改
`docs/superpowers/plans/2026-07-13-dialog-choice-memory-cutover-runbook.md` 与本日志；两仓 Java/Maven/schema/resources/tests/
host/caller 全冻结，不实际复制生产数据、不写凭据、不跑 Maven。直接原地修订 runbook 并追加 Repair #1 证据；若 trusted
resolver/manifest owner 尚不存在，须把它登记为**生产切换 blocker**，本轮仍可把 runbook 文档修到 APPROVED，但不得伪称
当前即可执行。Worker self-QA 不构成父级批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Claim Gate #4 - `W-DCM-RUNBOOK-IMP1-R1`

External Worker A 须在 `2026-07-13T15:30:00-04:00` 前于本日志真实 EOF 追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=runbook + 本日志）。20 分钟只检查领取，不检查完成；按时领取后可持续返修超过 20 分钟。截止未领取只在
本日志记录并原样重发给 A，绝不内部接管。

## External Worker A - CLAIMED - 2026-07-13T15:13:13-04:00

- task=W-DCM-RUNBOOK-IMP1-R1（领取截止 2026-07-13T15:30:00-04:00 内）
- claimedAt=2026-07-13T15:13:13-04:00
- uniqueWriteSet=① `docs/superpowers/plans/2026-07-13-dialog-choice-memory-cutover-runbook.md`（原地修订）；② 本 append-only 日志。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不复制生产数据、不写凭据、不跑 Maven。trusted resolver 不存在则登记为生产切换 blocker，不伪称当前可执行。self-QA 不构成父级批准。

## External Worker A - W-DCM-RUNBOOK-IMP1 Repair #1 证据 - 2026-07-13

runbook 已原地修订（同一文档，R1 修订版）；写集恰为 runbook+本日志。逐项关闭 Parent Runbook Review #1：

- **P1-1（no-clobber 发布）**：§0 增加 no-clobber 不变量并**全流程禁止 `REPLACE_EXISTING`**（撤回原 §3.5 fallback——业务 save 的 live replace 语义明确排除出首次迁移发布）；§3.0 新增 scope cutover lease/activation freeze（无 lease 能力 fail-closed）；§3.4 operationId 唯一 staging；§3.5 lease 内重验目标不存在 + 原子 create-if-absent（`ATOMIC_MOVE` 不带 REPLACE，平台无法证明"原子且 no-replace"即 `ABORT_NO_ATOMIC_NOREPLACE`，零 fallback）；§4 清理归属规则=仅删本 operationId 可证明拥有的 staging/本次创建目标，既有目标一律不动。
- **P1-2（trusted resolver）**：撤回原 A/B "可执行选项"表述——如实核定 `hashScope` private、`establishRealScopeRoot/resolveWithinRealScope` host package-private、无任何现存调用/运维入口 → §2 固定唯一路线（host/control-plane trusted resolver 产出不可手改 manifest：operationId+scope+resolved target，操作者零手填），并登记 **`BLOCKER-DCM-TRUSTED-RESOLVER`** 为生产切换 blocker；文档头部加"当前不可执行"诚实声明。源归属语义固定：本地文件=全局 bootstrap seed，复制到 manifest 每个获准 scope，单一归属收窄权留给父级 manifest 层。
- **P2-1（shape 事实）**：更正字段计数错误（实为 **18** 个持久字段）；§3.3 改为**用当前构建产物的 `MemoryFile`/`DialogChoiceEntry` 类+同配置 ObjectMapper 真实绑定**（类定义为唯一权威，不再手工维护清单），并加 `entries != null`/每 value 非 null/类型可绑定校验，未知字段容忍保持基线。
- **P2-2（staging 耐久/崩溃恢复）**：§3.4 staging 唯一命名+写后 force+重开复验；§3.5 发布后重开目标复验 bytes/SHA/shape；§4 崩溃恢复=残留仅由同 manifest operationId 识别、重取 lease 清理重跑、恢复前激活门关闭。§5 审计模板同步（operationId/lease/force/reopenVerify/双 preCheck 字段）。
- Markdown 自检通过；未复制生产数据、未含凭据、未跑 Maven。

self-QA：**P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。W-DCM-RUNBOOK-IMP1-R1 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Runbook Review #2 - APPROVED / Resolver Design Published - 2026-07-13T15:18:00-04:00（真实 EOF 权威块）

父级逐行复核 R1 runbook。首次迁移已全程禁止 `REPLACE_EXISTING`，并以 scope cutover lease、lease 内双重 target-absent
检查、operationId 唯一 staging、atomic no-replace 能力缺失即 fail-closed 和 token-owned cleanup 关闭覆盖/误删竞态；source
明确是本地全局 bootstrap seed，目标 scope 只能来自 authenticated inventory manifest。shape 现以当前构建产物中的 public
`MemoryFile/DialogChoiceEntry` 真实绑定（18 字段事实不再手工维护），staging force/reopen、发布后 reopen 与 crash recovery
均有审计字段。文档诚实登记 `BLOCKER-DCM-TRUSTED-RESOLVER` 与 legacy converter，未声称当前可执行，也未执行生产复制。

结论：**APPROVED，P0=0/P1=0/P2=0**。这只批准 runbook 流程合同；两个 blocker 未交付前生产切换仍禁止。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

### 下一任务 `W-DCM-RESOLVER-D1`

External Worker A 只做 trusted resolver/manifest/lease owner 的 Design #1，唯一写集仍仅本日志，Java/Maven/schema/resources/
tests/host/caller 全冻结。必须只读核对现有 `CloudServiceHost` 创建/关闭、scope 来源、host registry（若有）、
`CloudServiceStorage` package-private 能力与 control-plane authenticated inventory，给出：

1. 唯一可信 scope 来源与不可手改 manifest 的 closed shape/owner；不得接受操作者 raw tenant/user/path；
2. 同一 scope 的 cutover lease 与 host/Task activation freeze 的唯一线性化点，包含 duplicate、restart、crash、release；
3. no-replace staging/publish/owned cleanup 的真实 Java/NIO 能力边界；平台不能保证则保持 blocker；
4. 最小精确 New/Modify 文件表、public/package-private 边界、容量上界、编译门与生产切换独立验收门；
5. 不把迁移工具注册成业务 Service，不启动 host/Task，不写凭据、不实际复制文件。

如现有 control-plane/host 生命周期没有可挂的可信 inventory/activation freeze，须明确写 `BLOCKED` 与最小缺失 seam，不得
虚构入口。Worker self-QA 不构成父级批准，父级批准 Design 前不得改 Java。

## Parent Claim Gate #5 - `W-DCM-RESOLVER-D1`

External Worker A 须在 `2026-07-13T15:38:00-04:00` 前于本日志真实 EOF 追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=仅本日志）。20 分钟只检查领取，不检查完成；截止未领取只在本日志记录并原样重发给 A，绝不内部接管。

## External Worker A - CLAIMED - 2026-07-13T15:17:37-04:00

- task=W-DCM-RESOLVER-D1（领取截止 2026-07-13T15:38:00-04:00 内）
- claimedAt=2026-07-13T15:17:37-04:00
- uniqueWriteSet=仅本 append-only 日志（trusted resolver/manifest/lease owner Design #1）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不写 Java、不跑 Maven、不 git、不写凭据、不实际复制文件；只读核对现有 host/storage/control-plane 事实；无可挂 seam 则如实写 BLOCKED+最小缺失 seam。self-QA 不构成父级批准。

## External Worker A - Design #1（W-DCM-RESOLVER-D1）- 2026-07-13 - **结论：BLOCKED（两个缺失 seam）**

仅设计，两仓全冻结。只读核对事实（逐行）：host 包全集=8 文件（`CloudServiceHost/CloudServiceConfiguration/CloudServiceScope/CloudServiceStorage/CloudArtifactStore/CloudArtifactCapacityGovernor/ScopedPngArtifactStore/CloudTemplateAssets`）——**无 host registry、无 control-plane inventory、无 lease/freeze 设施**；`CloudServiceHost.create(CloudServiceScope, Path stateRoot)`（:35，public static）**在整个 Cloud 源码中零调用者**（除定义文件），`close()`（:65）同——**host 激活生命周期在生产装配上尚不存在**；`CloudServiceStorage` package-private 能力实证：`stateRootKey()`、`establishRealScopeRoot() throws IOException`、`resolveWithinRealScope(Path realScopeRoot, String fileName)`（均同包可用）。

### 判定：BLOCKED——最小缺失 seam（不虚构入口）

1. **SEAM-1 `authenticated scope inventory`**：不存在任何 control-plane 侧"获准 tenant/user scope 清单"的可信来源（代码/配置/文件均无）。没有它，manifest 的 scope 输入只能来自操作者手填——恰是 Runbook Review #1 P1-2 禁止的。owner=control-plane 切片（非-A/父级另派）。
2. **SEAM-2 `host activation lifecycle owner`**：`CloudServiceHost.create` 零调用者——没有集中激活点，就没有可挂 activation freeze/cutover lease 的唯一线性化位置（对不存在的生命周期"冻结"是空话）。owner=host 装配切片（非-A/父级另派）。

两 seam 交付前，trusted resolver/manifest/lease owner **不可实施**；以下为 seam 交付后的**绑定条件设计**（把当前能定死的全部定死，实施时不再返工设计）。

### 1. 唯一可信 scope 来源与不可手改 manifest（closed shape/owner）

- scope 唯一来源=SEAM-1 inventory（authenticated 读取，操作者零输入）；resolver 拒绝任何 raw tenant/user/path 参数——其唯一输入是 inventory 句柄。
- **owner**：host 包内 New package-private `DialogChoiceCutoverResolver`——对 inventory 每个 scope：构造 `CloudServiceStorage(scope, <host 装配的 stateRoot>)` → `establishRealScopeRoot()` → `resolveWithinRealScope(realScopeRoot, "dialog_choice_memory.json")`（全部走既有 containment 权威，零手工 hash/拼接）。
- **manifest closed shape**：package-private record 行 `CutoverManifestEntry(String operationId /*UUID*/, CloudServiceScope scope, Path resolvedTarget)`；整体 `CutoverManifest(List<CutoverManifestEntry> entries, String contentSha256)`——落盘文件末行携带对规范序列化内容的 SHA-256，消费端（runbook 执行者/lease owner）重算校验，**任何手改即校验失败 fail-closed**。manifest 只由 resolver 产出、只被 lease owner 消费，无 public 构造。

### 2. cutover lease / activation freeze 的唯一线性化点

- **线性化点=SEAM-2 的 host activation owner 内部单锁**（未来所有 `CloudServiceHost.create` 调用必经的唯一入口）：per-scope 状态 `{FREE, LEASED(operationId), PLACED, ABORTED}`。
- **duplicate**：同 scope 二次 acquire→拒绝（返回既有 LEASED 的 operationId，幂等查询不重铸）；不同 operationId 抢占→拒绝（无 takeover）。
- **activation freeze**：owner 在 `LEASED` 状态拒绝该 scope 的 `create`（激活门），`PLACED`/`FREE`（无源跳过）放行；freeze 与 lease 同一把锁=同一线性化点。
- **restart/crash**：lease 状态为 owner 内存态（restart-no-restore 一致）；崩溃后残留 staging 仅由同 manifest 的 operationId 识别（runbook §4 已批），恢复=重新 acquire 同 scope lease→清理本 operationId 残留→重跑；恢复完成前 owner 不放行该 scope（重启后默认 FREE 但目标预检查/staging 识别使未完成迁移无法被误放行——PLACED 判定以目标文件存在+审计记录为准，由操作流程复核，owner 不自行推断）。
- **release**：PLACED/ABORT 后由持 operationId 的执行者显式 release；release 后状态不可逆回 LEASED（同 operationId）。

### 3. no-replace staging/publish/owned cleanup 的真实 Java/NIO 能力边界

- staging 与目标同目录（既有 runbook §3.4）⇒ **恒同卷**，规避 `ATOMIC_MOVE` 跨卷不支持问题。
- 发布原语=`Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE)`（**不带** `REPLACE_EXISTING`）：JDK 合同下目标已存在→抛 `FileAlreadyExistsException`（映射 `ABORT_TARGET_EXISTS`）；平台不支持原子移动→抛 `AtomicMoveNotSupportedException`（映射 `ABORT_NO_ATOMIC_NOREPLACE`，**保持 fail-closed blocker 条款，无 fallback**）。NTFS 同卷 rename 满足"原子且 no-replace"两性质——能力边界真实存在且可由异常类型机械判定，无需平台探测代码。
- owned cleanup：可删对象仅 ①`dialog_choice_memory.json.<operationId>.staging`（唯一命名=归属自证）②发布后复验失败的目标（本 lease+本 operationId 创建，审计链自证）。其余一律不动（含 `FileAlreadyExistsException` 场景的既有目标）。

### 4. 最小文件表 / 可见性 / 容量 / 门（seam 交付后）

| 仓库 | FQCN | New/Modify | 可见性/说明 |
|---|---|---|---|
| Cloud | `host.DialogChoiceCutoverResolver` | New | package-private final；输入=SEAM-1 inventory 句柄+装配 stateRoot；输出=自校验 manifest；复用 storage 三个 package-private 能力 |
| Cloud | `host.CutoverManifest` / nested `CutoverManifestEntry` | New | package-private record + contentSha256 自校验 |
| Cloud | SEAM-2 activation owner（含 per-scope lease 状态机）| 归 SEAM-2 切片 | 本设计仅绑定其锁为唯一线性化点 |
| — | public 面 | **0** | 不注册业务 Service、不进 `CloudServiceConfiguration`、不启动 host/Task |

- **容量上界**：manifest 行数=inventory scope 数（外部有界）；lease 表 ≤ scope 数；resolver 无长寿命状态（一次产出即完）。
- **编译门**：Cloud `mvn -q clean package`（不 skip）。**生产切换独立验收门**：runbook §3-5 全量审计记录 + 父级对 manifest contentSha256/逐 scope decision 的独立复核——与编译门分离。
- 不写凭据、不实际复制文件；resolver 运行只产 manifest 文本。

`无已批准业务差异；按 0114604e 基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准；BLOCKED 判定本身即交付物）。W-DCM-RESOLVER-D1 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Resolver Design Review #1 - BLOCKED / Design Repair #1 Published - 2026-07-13T15:31:00-04:00（真实 EOF 权威块）

父级对照当前 `CloudServiceHost` 全树零调用、`CloudServiceStorage:111-153` 的 package-private real-root 能力与
JDK `Files.move` 合同复审 Design #1。A 对两个缺失 seam 的事实判断成立：当前没有 authenticated scope inventory，也没有
统一 host activation lifecycle owner；因此 resolver 不能实施，生产切换继续 fail-closed。条件设计仍有
**P0=0/P1=3/P2=1**，不得据此落 Java：

1. **P1：普通 `contentSha256` 不是不可篡改授权。** 可编辑 manifest 的操作者能同时修改 entries 并重算未认证 SHA-256，
   所以 `operationId + scope + resolvedTarget + contentSha256` 只能做损坏检测，不能证明 scope 来自 authenticated inventory。
   Repair 必须把授权权威留在 inventory/activation owner：优先使用 owner 铸造、不可由文件反序列化伪造的进程内 capability；
   若选择签名/MAC，则只登记外部 key-lifecycle/credential blocker，本切片不得造密钥或把摘要冒充认证。落盘 manifest 只能是
   audit artifact，不能单独解冻 host。
2. **P1：restart 后默认 `FREE` 会绕过未完成迁移 freeze。** 当前设计的 lease/状态全在内存，进程崩溃后若 activation owner
   先恢复为 FREE，host 可在 staging/目标/审计恢复前创建；“操作流程稍后复核”不能反向封住已经激活的 host。Repair 必须让
   activation owner 启动于 durable `RECOVERY_BLOCKED`，在同一 owner/锁内完成 trusted journal/manifest reconciliation 后才
   进入 FREE 或 PLACED；不得新增 TTL、takeover 或自动 retry。
3. **P1：`ATOMIC_MOVE` 不能机械证明 no-replace。** Java 合同规定指定 `ATOMIC_MOVE` 时其它 option 被忽略；目标已存在时
   行为依赖实现，不能据 `FileAlreadyExistsException` 断言所有支持平台均原子且不覆盖。Repair 必须给出经部署平台证明的
   create-if-absent 原语与能力探测/验收，或诚实保留 `BLOCKER-DCM-ATOMIC-NOREPLACE`；禁止 fallback 到
   `REPLACE_EXISTING`，也不得删除无法用 operation ownership 证明的新目标。
4. **P2：`inventory scope 数`不是容量上界。** Repair 须给 trusted configuration 的正数 manifest/lease cap、重复 scope
   拒绝、超限 fail-closed 与内存/耐久账本释放时点；不能把未知外部集合写成已 bounded。

### 当前任务 `W-DCM-RESOLVER-D2`

External Worker A 只追加 Design Repair #1 Delta，唯一写集仍仅本日志；两仓 Java/Maven/schema/resources/tests/host/caller
继续冻结。只关闭上述四项，不重写已通过的“两 seam 当前不存在、runbook 当前不可执行”结论。若 inventory/activation owner
尚无真实实现，最终结论仍可为 BLOCKED，但必须给最小可实现 seam，不能把普通摘要或进程内 FREE 状态称作安全门。

## Parent Claim Gate #6 - `W-DCM-RESOLVER-D2`

External Worker A 须在 `2026-07-13T15:51:00-04:00` 前于本日志真实 EOF 追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=仅本日志）。20 分钟只检查领取，不检查完成；截止未领取只在本日志记录并原样重发给 A，绝不内部接管。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T15:37:04-04:00

- task=W-DCM-RESOLVER-D2（领取截止 2026-07-13T15:51:00-04:00 内）
- claimedAt=2026-07-13T15:37:04-04:00
- uniqueWriteSet=仅本 append-only 日志（Design Repair #1 Delta，关闭 Resolver Review #1 的 P1×3/P2×1）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不重写已通过的"两 seam 不存在/runbook 当前不可执行"结论。self-QA 不构成父级批准。

## External Worker A - Design Repair #1 Delta（W-DCM-RESOLVER-D2）- 2026-07-13

关闭 `Parent Resolver Design Review #1` 的 P1×3/P2×1。仅设计，两仓全冻结；已通过结论（两 seam 当前不存在、resolver 不可实施、生产切换 fail-closed、runbook 当前不可执行）不重写，**最终结论保持 BLOCKED**——本 Delta 只把条件设计修到可信，并收敛出最小可实现 seam。

### H1（P1-1）：**撤回**"contentSha256=防手改"——授权权威改为 owner 铸造的进程内 capability

- 承认：未认证摘要可被同时改 entries+重算，只能做**损坏检测**。修正后的权威模型：
  - **授权载体=进程内 capability**：SEAM-2 activation owner 在读取 SEAM-1 authenticated inventory 后，于自身锁内为每个获准 scope 铸造不可伪造的 `CutoverAuthorization`（package-private final，私有构造，仅 owner 可 new——同 `AuthorityInstanceIdentity` 先例形状；**不可序列化、不可由任何文件反序列化重建**）。runbook 执行组件只能持该 capability 调用 lease/publish 门；无 capability 即无授权，文件内容永远不是授权来源。
  - **落盘 manifest 降级为 audit artifact**：仅供审计与崩溃恢复对账（记录 operationId/scope/resolvedTarget），其 contentSha256 仅作损坏检测标记；**manifest 文件不能单独解冻 host、不能兑换 capability**。
  - 签名/MAC 路线**不采用**：本切片不造密钥、不冒充认证；若父级未来要求跨进程可验证工件，则登记独立 `BLOCKER-DCM-MANIFEST-CREDENTIAL`（外部 key-lifecycle/credential 归 control-plane，非-A），当前不展开。

### H2（P1-2）：**撤回**"restart 默认 FREE"——owner 启动于 durable `RECOVERY_BLOCKED`

- activation owner 增加**最小 durable journal**（owner 私有 scoped 文件，仅记 per-scope 迁移状态转移：`LEASED(operationId)→PLACED|ABORTED|RELEASED`，append/原子更新，非业务 store）：
  - **启动序**：owner 构造即置全局 `RECOVERY_BLOCKED`（durable 标志=journal 存在未终结条目，或 journal 本身缺失但存在任何 `*.staging` 残留/未对账目标）；该状态下**拒绝一切 `CloudServiceHost.create`**。
  - **reconciliation（同一 owner/锁内）**：逐 journal 未终结条目 × 文件系统实况（staging 残留、目标存在性、审计记录）对账——可证明 PLACED→标记 PLACED；残留半成品→按 owned-cleanup（operationId 归属）清理并标 ABORTED；无法证明归属的任何文件→保持 BLOCKED 待人工。全部条目终结后才离开 `RECOVERY_BLOCKED`，逐 scope 进入 FREE/PLACED。
  - **无 TTL、无 takeover、无自动 retry**；reconciliation 只做状态证明与 owned 清理，不重跑迁移。
- 由此"崩溃后 host 先于恢复被创建"在时序上不可达：创建门与 reconciliation 同一把 owner 锁。

### H3（P1-3）：**撤回**"`ATOMIC_MOVE` 异常类型机械证明 no-replace"

- 承认 JDK 合同：指定 `ATOMIC_MOVE` 时其它 option 被忽略、目标已存在时行为实现相关——不可据 `FileAlreadyExistsException` 断言全平台原子 no-replace。
- **修正后的 create-if-absent 发布原语（JDK 合同背书，跨平台）**：
  1. `Files.createFile(target)`——JDK 明确保证"存在性检查+创建"为**单一原子操作**且 fail-if-exists（独占占位=同时证明 no-replace 与本 operation 归属）；已存在→`FileAlreadyExistsException`→`ABORT_TARGET_EXISTS`。
  2. 经 `FileChannel` 向该占位文件写入 staging 已复验的字节 → `force(true)` → 重开复验 bytes+SHA-256+shape。
  3. 中途崩溃=半写目标：由 H2 journal 的未终结 `LEASED(operationId)` 识别，`RECOVERY_BLOCKED` 挡住激活，owned-cleanup 删除（归属由 createFile 占位+journal 双证明）。
  - 该原语牺牲"发布瞬间原子内容可见性"换取可证明的 no-replace+归属；安全性由"占位→写入→复验→journal PLACED"链与激活门（复验通过前该 scope 恒不放行）保证——半写文件永远不会被业务读到。
- **可选强化保留为 blocker**：若部署平台（NTFS 同卷）经父级验收证明 rename-NOREPLACE 原子语义，可改用单步 move 发布；在此之前登记 `BLOCKER-DCM-ATOMIC-NOREPLACE`（验收证明归部署切片）。**禁止 `REPLACE_EXISTING`、禁止删除无 operation 归属证明的目标**（均维持既有条款）。

### H4（P2）：容量账本——trusted configuration 正数 cap

- **撤回**"inventory scope 数=上界"。改为：manifest 行数与并存 lease 数受 **trusted configuration 注入的正数 cap**（装配常量，先例=既有 authority cap 形状）约束：inventory 读取时**重复 scope 拒绝**（fail-closed，非去重容忍）；行数超 cap→整个 manifest 产出 fail-closed（不截断）；lease 表超 cap→acquire 拒绝。
- **释放时点**：内存 lease 条目于 release 后移除；durable journal 条目于 PLACED/ABORTED 终结且审计归档确认后标记 terminal（保留只读历史，不再参与 reconciliation 工作集）；resolver 本身无长寿命状态不变。

### 最小可实现 seam（收敛，供父级排期；本切片不实施）

`SEAM-2'（合并形态）`：一个 host 包内 `CloudHostActivationOwner`——单锁持有 ①`CloudServiceHost.create` 唯一入口门 ②per-scope lease 状态机 ③durable journal+RECOVERY_BLOCKED 启动序 ④`CutoverAuthorization` 铸造。SEAM-1 仍为其上游（authenticated inventory 来源，control-plane）。两 seam 交付前**最终结论保持 BLOCKED**。

`无已批准业务差异；按 0114604e 基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。W-DCM-RESOLVER-D2 Delta 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Resolver Design Review #2 - BLOCKED / Design Repair #2 Published - 2026-07-13T16:00:00-04:00（真实物理 EOF 权威块）

父级复核 D2。未认证摘要已降为 audit-only、restart 默认 FREE 已撤回、`ATOMIC_MOVE` no-replace 误证已撤回，
正数 trusted cap/duplicate/超限规则也成立；两条缺失 seam 与生产切换 blocker 继续诚实保留。当前条件设计仍
**BLOCKED，P0=0/P1=2/P2=1**，不得据此落 Java：

1. **P1：lease/freeze 未覆盖已经 ACTIVE 的 host。** D2 只写“LEASED 后拒绝新的 `CloudServiceHost.create`”，但没有
   规定 acquire lease 时如何处理同 scope 已经创建且仍持有 `DialogChoiceMemoryService` 的 host。若旧 host 仍 active，
   `Files.createFile(target)` 后的半写 live 文件可被既有 Service 读取，activation freeze 对它无效。Repair 必须把
   host create、active-host registry、host close 与 lease acquire/release 放在同一 owner/锁：有 active host 时 lease
   acquire 必须 fail-closed（本迁移波不自动 stop/drain）；只有证明该 scope 无 active host 后才可进入 LEASED。
2. **P1：durable journal 仍只有名称，没有 crash-consistent 写序。** “append/原子更新”未定义 LEASED 是否在创建 target
   前持久化、何时 force 文件/目录、torn tail/损坏记录如何处理。若 target 已 create 而 LEASED 未 durable，restart 后没有
   operation ownership 证明，既不能安全删也不能安全放行。Repair 必须给闭合顺序：durable LEASED write-ahead + force
   成功后才 create target；target write/force/reopen verify 后再 durable PLACED；无法解析、序号跳变、torn tail、target/
   staging 与 journal 不一致一律保持 `RECOVERY_BLOCKED`，不得自动清理或猜测成功。
3. **P2：`package-private final CutoverAuthorization` + `private` constructor + 另一个 owner 类负责 new 在 Java 中不可编码。**
   Repair 须给真实形状，例如 owner 的 package-private/static nested final handle，private constructor 由 enclosing owner
   铸造，并携 owner identity + exact scope + operationId；每次使用都校验对象归属与 current LEASED state。不能只靠类名/
   字段 equality，也不能从 manifest 反序列化。

### 当前任务 `W-DCM-RESOLVER-D3`

External Worker A 只追加 Design Repair #2 Delta，唯一写集仍仅本日志；两仓 Java/Maven/schema/resources/tests/host/caller
全冻结。只关闭上述三项，不重写 D2 已通过的 audit-only manifest、正数 cap、no-replace createFile 方向和“两 seam 当前缺失”
结论。若两 seam 尚不存在，最终状态继续 BLOCKED；不得以设计自洽冒充生产入口已存在。

## Parent Claim Gate #7 - `W-DCM-RESOLVER-D3`

External Worker A 须在 `2026-07-13T16:20:00-04:00` 前于本日志真实 EOF 追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=仅本日志）。20 分钟只检查领取，不检查完成；截止未领取只在本日志记录并原样重发给 A，绝不内部接管。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T16:05:19-04:00

- task=W-DCM-RESOLVER-D3（领取截止 2026-07-13T16:20:00-04:00 内）
- claimedAt=2026-07-13T16:05:19-04:00
- uniqueWriteSet=仅本 append-only 日志（Design Repair #2 Delta，关闭 Resolver Review #2 的 P1×2/P2×1）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不重写已通过项（audit-only manifest、正数 cap、no-replace createFile 方向、两 seam 缺失结论）；最终状态继续 BLOCKED。self-QA 不构成父级批准。

## External Worker A - Design Repair #2 Delta（W-DCM-RESOLVER-D3）- 2026-07-13

关闭 `Parent Resolver Design Review #2` 的 P1×2/P2×1。仅设计，两仓全冻结；**最终结论保持 BLOCKED（两 seam 未交付）**。

### K1（P1-1）：lease/freeze 覆盖 ACTIVE host——create/registry/close/lease 同一 owner 锁

- **撤回** D2 中"LEASED 仅拒绝新 create"的不完整覆盖。修正：SEAM-2'（`CloudHostActivationOwner`）的单锁同时持有四件事，任何一件不经该锁不可发生：
  1. **host create 唯一入口**：create 前检查该 scope 非 `LEASED/RECOVERY_BLOCKED`，成功后将 host 实例登记入 **active-host registry**（owner 内 `Map<scope, CloudServiceHost>`，锁内写）。
  2. **host close 登记出**：owner 提供唯一 close 入口（包装既有 `CloudServiceHost.close()` :65），锁内从 registry 移除；不经 owner 的 close 不存在（create 只经 owner，引用不外泄裸 host——owner 返回受管句柄）。
  3. **lease acquire**：锁内先查 registry——**该 scope 存在 active host → acquire fail-closed（`ABORT_ACTIVE_HOST`）**；本迁移波**不自动 stop/drain**（停机属运维前置步骤，由 runbook 操作者先经 owner close 后重试 acquire）。registry 无该 scope 且状态 FREE → 进入 LEASED。
  4. **lease release**：锁内状态转移（K2 的 durable 序完成后）。
- 由此"旧 active host 读到半写 live 文件"在时序上不可达：目标文件的 `createFile` 只发生在 LEASED 内，而 LEASED 的前置是锁内证明无 active host；host 再创建又被 LEASED 状态挡住。

### K2（P1-2）：durable journal 的 crash-consistent 闭合写序

- **记录格式**：单文件 append-only；每记录=固定头（单调递增 seq + scope + operationId + state∈{LEASED,PLACED,ABORTED,RELEASED}）+ 记录级 CRC32；编码定长/长度前缀，无跨记录依赖。
- **闭合顺序（每 scope 一次迁移）**：
  1. **write-ahead LEASED**：append `LEASED(seq,scope,opId)` → `FileChannel.force(true)` **成功后**才允许 `Files.createFile(target)`（占位）。force 失败→不创建任何文件，锁内回退 FREE（无外部痕迹）。
  2. target 写入 → force → **重开复验**（bytes+SHA-256+shape）全部通过后 → append `PLACED(seq,scope,opId)` + force。PLACED durable 前该 scope 激活门恒关。
  3. ABORT 路径同法：owned cleanup 完成后 append `ABORTED` + force；release 后 append `RELEASED` + force。
- **恢复判定（RECOVERY_BLOCKED 内，逐条重放）**：CRC 失败/序号非单调/**torn tail**（尾部长度不完整）→ **一律保持 `RECOVERY_BLOCKED`**，不自动清理、不猜测成功；journal 与实况不一致（最新 LEASED 但 target 不存在且无 staging；或存在 journal 未记载的 staging/target 痕迹）→ 同样保持 BLOCKED 待人工。**只有**"最新记录=LEASED 且 target/staging 实况与该 opId 完全对账"才允许 owned cleanup→ABORTED；"最新=PLACED 且 target 复验通过"才标 PLACED 放行。
- Windows/NTFS 持久性注记（如实）：`force(true)` 覆盖文件数据+元数据；目录项持久性依赖 NTFS 元数据日志，无用户态目录 fsync——该残余窗口（journal 已 force 但目录项丢失=整个 journal 文件消失）等价于"journal 缺失但存在实况痕迹"，已被恢复判定的 BLOCKED 分支覆盖，不额外发明原语。

### K3（P2）：capability 的可编码真实形状

- **撤回** D2 "package-private final 类 + private ctor 由另一个 owner 类 new"（Java 不可编码）。真实形状：
```
final class CloudHostActivationOwner {
    static final class CutoverAuthorization {          // owner 的 static nested final
        private final CloudHostActivationOwner owner;  // 铸造者引用
        private final CloudServiceScope scope;
        private final String operationId;
        private CutoverAuthorization(CloudHostActivationOwner owner, CloudServiceScope scope, String operationId) {...}
    }                                                  // private ctor：enclosing owner 可直接 new（Java 嵌套类合法）
    CutoverAuthorization acquireLease(CloudServiceScope scope) { synchronized(lock){ ...; return new CutoverAuthorization(this, scope, opId); } }
    void requireAuthorized(CutoverAuthorization auth, CloudServiceScope scope) {
        // 引用相等校验 auth.owner == this；scope 相等；且该 scope 当前状态==LEASED 且 leaseTable[scope].operationId.equals(auth.operationId)
    }
}
```
- 每次 publish/cleanup 门调用 `requireAuthorized`：**对象归属（owner 引用相等，非类名/字段 equality）+ current LEASED state + operationId 匹配**三重校验；不可序列化、无 manifest 反序列化路径（manifest 维持 audit-only）。先例形状=`CloudTaskRunAuthorityAssembly.AuthorityInstanceIdentity`（private ctor nested）+ `requireAuthority` 引用相等校验。

`无已批准业务差异；按 0114604e 基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。W-DCM-RESOLVER-D3 Delta 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Resolver Design Review #3 - PARKED / BLOCKED - 2026-07-13T16:10:00-04:00（真实物理 EOF 权威块）

D3 已补 active-host 检查、journal write-ahead 主序和可编码 nested authorization，方向成立；但现有仓库仍没有
authenticated inventory 与 activation lifecycle owner，继续修纸面设计不会产生可运行代码。父级将本 arc 明确
**PARKED/BLOCKED，P0=0/P1=3/P2=1**，不再发布 D4；下列条件转为两个 seam 真正开工时的强制验收项：

1. **P1：现有 `CloudServiceHost.create` 仍是 public static bypass。** 唯一 activation owner 若不同时收口该入口的
   可见性/调用面，任何代码仍可绕过 registry/lease 直接创建 host；真实实施必须把 create 变成 owner-only 可达，并由
   managed handle 暴露 service/close，禁止泄漏裸 host。
2. **P1：在 owner 锁内直接 `CloudServiceHost.close()` 会执行 Spring context 回调。** 回调可能阻塞或重入 owner，不能持
   registry 锁做外部生命周期 I/O。真实实施必须在锁内 `ACTIVE -> CLOSING` 并封住 create/lease，锁外 close，随后锁内
   success -> FREE；close 失败保持 CLOSING/RECOVERY_BLOCKED，不能假装 registry 已空。
3. **P1：`FileChannel.force(true)` 失败后回退 FREE 不安全。** force 抛错不证明 append 字节未写或未持久化；该路径必须
   进入 `RECOVERY_BLOCKED`，不得在同进程立即复用 scope/operationId，也不得创建 target。
4. **P2：append-only journal 历史仍无正数容量上界。** manifest/active lease cap 不会限制 terminal history；实施前须给
   owner 停止且全部 entry terminal 时的 crash-safe snapshot/compaction，或 trusted positive history cap 超限 fail-closed，
   不得使用 TTL/LRU 删除恢复证据。

DialogChoice 代码迁移与 cutover runbook 的既有 APPROVED 结论不回退；**生产切换仍禁止**，直到两个 seam 实现并关闭
上述门。External A 本 arc 到此停止，不再改该日志或相关 Java。

### Parent Next Task Handoff

External Worker A 的新固定日志：
`docs/superpowers/plans/reports/2026-07-13-cloud-npc-click-service-worker-a.md`。当前任务 `W-NPC-D1`，先做
NpcClickService 整类迁云 Design #1，父级批准前零 Java。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
