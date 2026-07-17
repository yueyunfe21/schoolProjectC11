# HTTPS Turn 全量迁移第一轮计划审计 A：Foundation（TURN-00..TURN-17）

> 审计性质：`PRECHECK ONLY`，只给依赖、写集、capability 与验收建议，不改变任何卡片状态，不构成父级裁决。
>
> 审计范围：`TURN-00..TURN-17`，含 `TURN-01A..01D`、`TURN-03A..03B`、`TURN-08A..08B`、
> `TURN-10P/10A..10E` 以及待补 `TURN-13H`。
>
> 本轮只写本报告；未修改 Java、权威计划、协议、CR、迁移矩阵或 dashboard，未运行 Maven/tests/runtime，
> 未派发任何卡片。

## 1. 审计依据与当前事实

已完整交叉读取：

- `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271。
- 权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`。
- 协议规格 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
- `TURN-00..TURN-16` 当前卡报告、`TURN-13H` architecture helper、双仓当前源码和两仓 dirty/untracked 状态。
- `TURN-17` 当前没有独立卡报告；权威计划也尚未落入正式 `TURN-13H` 卡段。

静态事实：

1. 双仓 `com.bot.dhxy.cloud.turn.protocol` 各有 25 个 Java 文件，文件名和 SHA-256 全部一致。
2. DHXY Foundation 的 client/template/capture/match/input/local-service/action/loop/mode-guard 源码均已存在；
   Cloud 的 exchange/catalog/handler/routes 源码也已存在。
3. 当前 DHXY compile 曾有 exit 0 证据；Cloud `clean compile` 仍被迁入 Cloud 的 legacy whole Service/Task
   对 DHXY-only 类型的引用挡住，Foundation 尚无全仓绿色构建证据。
4. Cloud `pom.xml` 强制 `skipTests=false` 且 enforcer 拒绝所有 test-skip 参数；仓库同时处于 no-local-test
   默认模式。故“每卡/每 cohort 必须 `clean package`”与当前测试政策存在命令级冲突，不能继续把失败的
   package 当作普通源码问题，也不能伪造 package 成功。

## 2. 跨卡高优先级 PRECHECK 发现

### F-01：Cloud command 结果丢失 PNG 字节

- `CloudTurnHttpHandler` 构造真实 `CloudTurnFrame` 后交给 `CloudTurnExchange.exchange(...)`。
- `CloudTurnExchange.acceptPreviousOutcome(...)` 会校验 frame metadata、SHA、PNG 尺寸，但
  `currentOutcome` 的类型是 `CompletableFuture<TurnOutcome>`，最终只执行
  `completedFuture.complete(validatedOutcome)`。
- `CloudTurnCommandResult` 也只有 `TurnOutcome outcome`，没有 `CloudTurnFrame`；而
  `TurnOutcome.frame` 只有元数据。
- 影响：TURN-14/15/16 的无图 local-service 结果仍可设计；TURN-17 的 Quest 详情图以及后续任何
  `CAPTURE -> Cloud 算法` caller 都拿不到原始 PNG。
- PRECHECK 建议：在 TURN-17 开始前补一个 TURN-02 result-channel repair，至少冻结
  `CloudTurnExchange.java`、`CloudTurnCommandResult.java`（复用已有防御性 `CloudTurnFrame`）的精确写集，
  让一次 completed command 原子返回 outcome 与它声明的同一 frame；无 frame 时两者保持 null 对齐，不能另存
  durable artifact、不能再抓图、不能 retry。

### F-02：TURN-13H 是 14/15/16 的共同源代码前置，但不是运行 activation

- Server 当前只把同一个 `CloudTurnExchange` 封装进 `CloudTurnRoutes.Bundle` 并注册 HTTP handler。
- `CloudServiceHost` 的独立 Spring context 没有 `CloudTurnCommandPort` Bean，现有配置也不扫描未来 turn client。
- 影响：14/15/16 的现有写集不能取得 HTTP ingress 正在使用的同一个 slot；另建 exchange 会永久断链。
- PRECHECK 建议：正式补 `TURN-13H`，只建立 inert construction/DI capability；14/15/16/17 的
  `startDependsOn` 全部改为包含 13H 的父级源码复审收口，不以 TURN-13 单独放行。

### F-03：TURN-14 的完成条件与当前 Task caller/写集冲突

- `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag(...)` 的 public 参数仍是
  `BagService.MainBagSession`。
- `FiveRingTaskV2` 当前直接注入 `BagService`，并调用上述 open-main-bag 路径；TURN-14 又明确禁止修改 Task，
  同时要求“无 Cloud BagService 实例”。
- 影响：仅改 TURN-14 四个文件无法同时保持该 caller、移除 Cloud `BagService`、完成真实切流。
- PRECHECK 建议：领取前二选一并冻结：
  1. 将 TURN-14 的完成口径缩为“仅切 `ReturnItemPrescanService` 与非 open-session incense caller”，把
     FiveRing/open-main-bag 收口明确留给后续 whole Task 卡；或
  2. 把精确 FiveRing caller 文件加入串行写集并更新依赖，不能继续声称 Task 不改且本卡清零 `BagService`。

### F-04：卡片写集与 Bean discovery 尚未闭合

- 13H helper 推荐只扫描新 `com.yueyunfe.dhxy.cloudbrain.turn.client`，这是避免把 exchange/handler/routes
  扫入每个 host 的正确方向。
- 但 `CloudUiCleanerPort` 位于 `remote` 包，当前被多个 `com.bot.dhxy.service` Bean 注入，而 `remote` 不在
  `CloudServiceConfiguration` scan/import 中；只扫描 `turn.client` 仍不能构造这些 Service。
- PRECHECK 建议：13H 只增加窄 `turn.client` scan 和 action factory Bean；同时在 13H 的 exact config
  write set 内显式 import 既有 `CloudUiCleanerPort`（以及 14 最终仍需的具体 remote port），或给 14/15 各自
  增加互斥 assembly 文件。禁止扫描整个 `remote` 或整个 `turn` 包。

### F-05：结果 DTO 与 stable actionId 合同没有完全冻结

- TURN-14 主计划写“typed result DTO”，冻结报告却只列四个 Java；TURN-17 只写“Quest typed result DTO”，
  没有文件名、包名、字段、frame ownership 或 public client 方法。
- `CloudTurnActionFactory` 要求 caller 显式提供 stable `actionId/deviceId/windowId`。现有授权
  `TaskExecutionContext` 能提供 `getScope().deviceId()`、`getWindowId()`、`getTaskRunId()`；14/15 的 public
  API 还提供 `phaseCode/actionSlot`，但具体 canonical actionId 拼接/转义/长度规则尚未落卡。
- PRECHECK 建议：14..17 开工前冻结同一 actionId 规则及 timeout 映射；DTO 要么作为各 client 文件内 private
  record，要么给出唯一新文件，不能交付时临时扩写集。

### F-06：Foundation 构建门需要先统一

- DHXY 门可继续用 `mvn -q -DskipTests compile`。
- Cloud `mvn -q clean package` 会按 pom 执行测试；test-skip 又被 enforcer 拒绝，与 no-local-test 默认政策冲突。
- PRECHECK 建议：Foundation source cards 先统一以 Cloud `mvn -q clean compile` 作为 Java gate；最终 package
  要由父级明确选择“本次允许现有测试运行”或另开 pom/policy 决策卡。未解决前，每卡都应写
  `SOURCE GATE` 与 `PACKAGE COHORT GATE` 两列，不能把 source 收口等同可运行交付。

### F-07：协议规格存在同文档自相矛盾示例

- Locked Minimum Contract 只允许 `CAPTURE/MATCH_TEMPLATE/INPUT/WAIT/LOCAL_SERVICE`。
- 同一规格 `Payload Shape` 仍出现 `KEY_PRESS` 与 `CLICK` step type，后文也继续解释这两个旧类型。
- PRECHECK 建议：TURN-00 补一次文档勘误，把示例改为 `INPUT + TurnInputAction`；否则后续普通 caller 卡可能
  按旧示例制造第六/第七种 step。

## 3. 逐卡审计

### TURN-00 — 协议冻结

- **PRECHECK：`SPEC-DRIFT`。** 文档卡可独立交付，零 start 依赖，写集与其它 Java 卡互斥，运行 capability 不适用。
- **依赖/验收：** 应以 locked contract、operation allowlist、frame/null 规则和 failure/stop shape 一致为验收物；无构建门。
- **风险/建议：** F-07 的旧 payload 示例尚未消除；在继续扩展 caller 前先做纯文档勘误，并明确不改变现有 25 个 DTO。

### TURN-01A — envelope/window DTO

- **PRECHECK：`SOURCE-INDEPENDENT / COHORT-BUILD-PENDING`。** 目标可按 TURN-00 独立实现；写集精确且双仓互斥。
- **依赖：** start 依赖 TURN-00 足够；最终收口依赖 01D validator 是合理的 source/approval 分层，不形成实现循环。
- **capability/验收：** 25 文件 parity 已实证；本卡自身只提供 envelope/window 类型，不产生运行 capability。
- **风险/建议：** 验收应保留双仓 SHA parity、canonical JSON round-trip 和最终双仓 compile；不能只凭文件存在。

### TURN-01B — action/step DTO

- **PRECHECK：`SOURCE-INDEPENDENT / COHORT-BUILD-PENDING`。** 写集精确，与 01A/01C/01D 互斥。
- **依赖：** TURN-00 start 足够；最终依赖 01D 合理。
- **capability/验收：** closed step type、input action、capture/match/local-service 参数都已存在；运行能力仍需 executor 卡。
- **风险/建议：** 任何后续卡不得依据规格中的旧 `CLICK/KEY_PRESS` 示例扩大 enum。

### TURN-01C — outcome/frame DTO

- **PRECHECK：`SOURCE-INDEPENDENT / COHORT-BUILD-PENDING`。** 写集精确，与其它 protocol slice 互斥。
- **依赖：** TURN-00 start 足够；最终依赖 01D 合理。
- **capability/验收：** frame metadata 可表达 absolute region/purpose/sourceStepIndex，但不携带 PNG bytes；PNG 属 multipart。
- **风险/建议：** F-01 是 Cloud command result 的实现缺口，不应错误扩展 protocol DTO 来塞 Base64。

### TURN-01D — 双侧 validator

- **PRECHECK：`CLEAR-SOURCE / COHORT-BUILD-PENDING`。** start 依赖 01A/B/C source 充分，写集仅双侧 validator。
- **capability/验收：** 应检查双侧字节一致、closed union、frame/result nullability、最多一个 success upload producer。
- **风险/建议：** validator 只证明 payload shape；不能替代 command frame bytes 的传递验收。

### TURN-02 — Cloud single-slot exchange

- **PRECHECK：`RESULT-CHANNEL-GAP`。** in-memory single-slot、uncertain timeout、重复 actionId fence 可独立交付；
  但“完整 command result”尚不成立。
- **依赖：** start 只依赖 TURN-00 在 frozen-API 并行模式下可接受，最终必须等 01D；写集五文件精确。
- **capability：** action/outcome slot 已具备；原始 PNG 在 ingress 验证后未进入 command caller，见 F-01。
- **验收/构建：** 除状态机静态审查外，必须增加“outcome.frame 与 command frame 同时有/同时无、PNG 防御性复制、
  repeated outcome 不重复完成 future”的 source acceptance；Cloud cohort compile。
- **风险/建议：** TURN-17 和所有 capture-driven Cloud 算法依赖 result-channel repair；不得用 artifact store/第二截图绕开。

### TURN-03A — template catalog

- **PRECHECK：`CLEAR-SOURCE`。** 只读 allowlist/catalog 可独立交付，写集单文件，与 handler 互斥。
- **依赖：** TURN-00 足够；运行依赖现有 `PackagedTemplateAssets` 已具备。
- **验收/构建：** path normalization、folder expansion、hash/ETag metadata、越界拒绝；Cloud compile cohort。
- **风险/建议：** 不得把 catalog 变成 mutable cache、上传入口或文件系统任意路径读取器。

### TURN-03B — template HTTP handler

- **PRECHECK：`START-DEPENDENCY-TOO-WEAK`。** handler 可独立交付，但源码直接消费 03A catalog API。
- **依赖：** 建议 startDependsOn 从仅 TURN-00 收紧为 `TURN-03A SOURCE`；仅把 03A 放在最终 approval 依赖会造成
  早期开工时 API 猜测或无法编译。
- **写集/capability：** handler 与 03A 互斥；auth/path/ETag/304 capability 已存在。
- **验收/构建：** auth before existence、allowlist path、content type、hash、304；Cloud cohort compile。

### TURN-04 — Cloud turn ingress

- **PRECHECK：`CLEAR-SOURCE`。** start 依赖 02 source、最终依赖 01D，写集 reader+handler 精确。
- **capability：** multipart JSON/PNG correlation、auth、尺寸/hash 校验已具备；依赖 02 的 slot。
- **验收/构建：** JSON-only 与 multipart 两条入口、unknown part/oversize/path/auth 拒绝；Cloud cohort compile。
- **风险/建议：** handler 当前只证明 frame 进入 exchange，不证明 command caller 能取得 bytes；F-01 仍需单独验收。

### TURN-05 — Cloud route/server 接线

- **PRECHECK：`HTTP-CAPABILITY-CLEAR / COMMAND-EXPORT-MISSING`。** 两个 HTTP context 可独立接线，写集精确。
- **依赖：** 03B+04 充分；与未来 TURN-40 对 `CloudBrainServer` 的修改必须串行。
- **capability：** `/turn` 与 `/templates` 已有真实 Server wiring；同一 bundle 的 command capability 未进入 host graph。
- **验收/构建：** exactly one exchange、exactly two handlers、无 host/loop activation；Cloud cohort compile。
- **风险/建议：** 该卡的输出应显式列出“HTTP ingress ready，business command DI not ready”，并以 13H 补后者。

### TURN-06 — DHXY HTTPS client

- **PRECHECK：`CLEAR-SOURCE / DORMANT`。** client/transport 文件写集精确，可按 frozen contract 并行实现。
- **依赖：** TURN-00 start、01D 最终收口足够。
- **capability：** HTTPS/loopback HTTP、bearer、multipart、long-wait、template GET 已具备；无自动 loop/start。
- **验收/构建：** timeout 层级、非 loopback 明文拒绝、PNG 原始 multipart、防御性 bytes；DHXY compile。

### TURN-07 — DHXY template cache

- **PRECHECK：`CLEAR-SOURCE / DORMANT`。** 单文件写集，依赖 03B+06 充分。
- **capability：** manifest/ETag/atomic replace/云端不可用时本地命中规则已具备；没有自动下载线程。
- **验收/构建：** root/path/hash/304/原子替换/删除后可按需下载；DHXY compile。
- **风险/建议：** 不得让 cache 本身决定业务 fallback 或自动 retry。

### TURN-08A — window binding/capture/PNG

- **PRECHECK：`CLEAR-SOURCE / DORMANT`。** 四文件写集精确，和 match/input 互斥。
- **依赖：** TURN-00 start、01D 最终收口足够；现有 window/capture API 可复用。
- **capability：** exact window resolve once、background capture、absolute region、PNG metadata 已具备。
- **验收/构建：** 不 title-search refresh、不伪 `(0,0)`、metadata 与 PNG 尺寸/SHA一致；DHXY compile。

### TURN-08B — local template match

- **PRECHECK：`CLEAR-SOURCE / DORMANT`。** 单文件写集，依赖 07+08A 充分。
- **capability：** optional local match、absolute center、miss 不 click 已具备；实际 click 组合由 TURN-11。
- **验收/构建：** template version/cache、ROI 坐标、threshold、resultMode/frame；DHXY compile。
- **风险/建议：** 不得把 Cloud OCR/业务判断塞入 matcher。

### TURN-09 — typed input executor

- **PRECHECK：`CLEAR-SOURCE / DORMANT`。** 三文件写集精确，与 capture/match/local adapter 互斥。
- **依赖：** TURN-00 start、01D 最终收口足够。
- **capability：** background-capable keyboard 与 foreground mouse 的 typed mechanical mapping 已存在。
- **验收/构建：** closed key/action allowlist、atomic ordered sequence、stop mapping、无 direct business fallback；DHXY compile。
- **风险/建议：** 卡报告/计划应列出实际支持的 key 名称，unsupported 必须稳定失败，不能运行时猜键。

### TURN-10P — LocalServiceExecution

- **PRECHECK：`CLEAR-SOURCE`。** 单文件 write set，可独立交付；依赖 01C+08A 充分。
- **capability：** typed status/code/JSON 与可选 Quest frame 的边界已存在。
- **验收/构建：** JSON byte cap、Quest-only frame、PNG/SHA/region 校验、防御性 bytes；DHXY compile。

### TURN-10A — Bag adapter

- **PRECHECK：`DEPENDENCY-METADATA-NEEDS-TIGHTENING`。** 单 adapter 写集精确，closed Bag macro 可独立交付。
- **依赖：** 仅写 10P 不足以表达它还消费 01B local-call/args 与既有 BagService direct API；建议显式加
  `TURN-01B SOURCE` 和 frozen Bag direct API readiness。
- **capability/验收：** caller owns exactly one outer exclusive；intent/cache point/JSON terminal 已具备；DHXY compile。

### TURN-10B — UICleaner adapter

- **PRECHECK：`DEPENDENCY-METADATA-NEEDS-TIGHTENING`。** 单 adapter 写集精确。
- **依赖：** 建议显式加 01B source 与 UICleaner 四个 public/direct API readiness。
- **capability/验收：** broad/generic/lightweight 保留 Service 自有 queue；X2 只拥有一次 exclusive；无 queue nesting。
- **风险/建议：** Navigation 三个 X2 caller 的外围 mouse-away/direct-input 合并仍是后续 caller integration，不应
  由本 adapter 偷改。

### TURN-10C — GiveItem adapter

- **PRECHECK：`DEPENDENCY-METADATA-NEEDS-TIGHTENING`。** 单 adapter 写集精确。
- **依赖：** 建议显式加 01B source 和 `GiveItemService.executeGiveDirectForExclusive` readiness。
- **capability/验收：** dispatcher 外层一次 exclusive，adapter 内无 queue；target template/known index 与 boolean JSON。

### TURN-10D — Quest adapter

- **PRECHECK：`PREREQUISITE-NOT-REFLECTED-IN-MAIN-CARD`。** adapter 本身单文件可独立交付，但成功 frame 需要真实
  absolute capture origin。
- **依赖：** 实际已先补 `QuestDetailCapture.screenX/screenY` 与 `QuestManagerService` 单次 capture 返回 origin；
  主计划应把这项 prerequisite 固化，不能让未来复做时重新猜 `(0,0)`。
- **capability/验收：** activate JSON、detail 一次 capture/一次 encode/一次 flush、QUEST_DETAIL frame 已具备；DHXY compile。
- **风险/建议：** Cloud 端仍拿不到该 frame bytes，见 F-01；这不是 adapter 再截图可以解决的。

### TURN-10E — Local Service dispatcher

- **PRECHECK：`CLEAR-SOURCE`。** 单文件写集，依赖 10A/B/C/D 充分，并隐含 10P。
- **capability/验收：** Bag/Give 各一次 outer exclusive；UI/Quest direct 避免二次 queue；closed switch/fail-closed。
- **风险/建议：** 以后新增 operation 必须先改协议/validator/adapter，dispatcher 不得 default reflection。

### TURN-11 — local action executor/outcome assembler

- **PRECHECK：`CLEAR-SOURCE / DORMANT`。** 四文件写集精确，依赖 08B+09+10E 充分。
- **capability：** ordered steps、first failure、STOPPED、one frame slot、failure evidence replacement 已具备；
  LOCAL_SERVICE 在 exact `WindowTaskContext` 下执行。
- **验收/构建：** step/result type closure、later NOT_RUN、stop 无 evidence、normal failure 清空旧 candidate 后再尝试
  full-window evidence、ExecutedTurn frame/PNG 双 null 或双 present；DHXY compile。

### TURN-12 — per-window loop/registry/factory

- **PRECHECK：`CLEAR-SOURCE / DORMANT`。** 三文件写集精确，依赖 06+11 充分。
- **capability：** 显式 start/stop/await、一次 exchange、previous ACK、transport uncertainty、actionId memory cache 已具备。
- **验收/构建：** lifecycle start/stop 原子、registry retire/remove 原子、真实 metadata supplier、无 scheduler/retry；DHXY compile。

### TURN-13 — Foundation wiring/mode exclusion

- **PRECHECK：`SOURCE-CLEAR / BUILD-POLICY-UNRESOLVED / NO-ACTIVATION`。** 依赖 05+12 从技术链上充分；写集在冻结报告
  中已收敛为三类 turn config/guard、一个真实 control file、properties。
- **capability：** inert Bean、exact-window local/remote start exclusion、显式 guard API 已具备；没有用户入口、host、loop 自动启动。
- **验收/构建：** DHXY compile 有成功证据；Cloud 全仓 compile 仍受 legacy copies 影响，package gate又受 F-06 约束。
- **风险/建议：** TURN-13 source 收口只能表示“DHXY dormant foundation 可装配”，不能作为 14..17 获得 Cloud
  command capability 的依据；下游必须显式经过 13H。

### TURN-13H — same-exchange dormant host injection（待正式落卡）

- **PRECHECK：`REQUIRED / DORMANT-ONLY`。** 目标可独立交付，`countDelta=0`；建议
  `startDependsOn=TURN-02 + TURN-05 + TURN-13 source`，完成后只放行 source implementation，不宣称 runtime ready。
- **推荐唯一 Cloud Java 写集：**
  - `com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnRoutes.java`
  - `com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`
  - `com/yueyunfe/dhxy/cloudbrain/host/CloudServiceHost.java`
  - `com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java`
- **精确 dormant 设计：**
  1. `Bundle.commandPort()` 只公开已保存的同一个 typed capability，不 new exchange、不包第二层 state。
  2. `CloudBrainServer` 只把该 capability 保存为 final 字段，供 TURN-40 后续显式使用；本卡不调用
     `CloudServiceHost.create`，不建 host 集合/endpoint，不 start/close host/loop。
  3. `CloudServiceHost.create(scope,stateRoot,commandPort)` 强制非 null，并在 refresh 前注册 supplied exact Bean；
     删除无 command-port 的两参数 fallback。当前无 production caller，故无兼容循环。
  4. `CloudServiceConfiguration` 只扫描新 `com.yueyunfe.dhxy.cloudbrain.turn.client`，提供无状态
     `CloudTurnActionFactory` Bean；不得扫描整个 `turn`/`remote` 包。现有 remote port 用显式 import 或独立窄
     assembly 闭合，见 F-04。
- **与 TURN-40 隔离验收：** 13H 源码中不得出现 host create caller、用户命令、window/task 选择、scope 认证、
  server endpoint、loop start、`@PostConstruct/@Scheduled`、thread/executor/timer/retry、static holder/service locator。
  TURN-40 仍独占 authenticated scope/state-root 选择、host 创建/登记/关闭和用户可见 REMOTE_TURN activation。
- **风险/建议：** exchange key 目前只有 deviceId/windowId，而 host 有 tenant/user scope；TURN-40 激活前必须证明
  deviceId 在一个 Server 内全局唯一，或提供不新增 protocol owner/session 的显式部署隔离约束。13H 不应提前解决
  这项 runtime ownership。

### TURN-14 — Bag typed facade/caller cutover

- **PRECHECK：`NOT-INDEPENDENT-AS-WRITTEN`。** 必须新增 13H start dependency；无图 result 不受 F-01 影响。
- **写集：** 当前冻结报告四文件彼此互斥，但主计划多写了未命名 typed DTO；需先统一。新 client 应落到
  `turn/client/CloudBagLocalServiceClient.java`，以便 13H 窄扫描。
- **capability：** context 已有 device/window/taskRun identity；shared port/Bean 尚待 13H。Bag adapter JSON 是
  `{intent,state,cachePoint}` 或 `{state:USED|NOT_FOUND}`，可严格 parse。
- **遗漏前置：** F-03 的 `BagService.MainBagSession` Task caller 使“本卡后无 Cloud BagService”无法在当前写集内成立。
- **验收/构建：** stable actionId、timeout/uncertain terminal、cache point round-trip、prescan/fallback/order 不变；
  Cloud source compile 与最终 package cohort 分列。

### TURN-15 — UICleaner typed facade

- **PRECHECK：`NEEDS-13H-AND-BEAN-DISCOVERY`。** 两文件业务写集互斥，但 source start 依赖必须包含 13H。
- **capability：** UI adapter JSON 已固定为 `{operation,handled}`；context identity 充分；shared port 待 13H。
- **遗漏前置：** `CloudUiCleanerPort` 本身不在 host scan，而多个 Service 构造器依赖它；13H 或独立 assembly
  必须显式注册，不能只扫描新 client。
- **验收/构建：** 四 operation 与原 boolean/void terminal 一一映射，before/after TaskCheckpoint 保留，X2 不重开
  queue，uncertain 不伪 false/成功；Cloud source compile 与 package cohort 分列。

### TURN-16 — GiveItem typed facade

- **PRECHECK：`NEEDS-13H`。** 两文件写集互斥，shared command capability 是唯一共同前置。
- **capability：** Give adapter JSON 是 `{given:boolean}`；context identity 充分；local action 在 DHXY 一次 exclusive 内
  完成 item select + Give button。
- **依赖/写集风险：** 卡片必须明确本卡替换的是“give entry 已打开后的 GiveItemService 边界”；Dialog 之前寻找/点击
  give entry 的 capture/识别/点击迁移不应被误算为 TURN-16 已完成，也不能在两文件外顺手改。
- **验收/构建：** target template/index、Dialog status、stop/uncertain mapping、无 auto retry；Cloud source compile 与
  package cohort 分列。

### TURN-17 — Quest typed facade 与 PNG result

- **PRECHECK：`CARD-UNDERSPECIFIED / FRAME-CAPABILITY-MISSING`。** 当前没有独立报告；主计划只列 client 与未命名 DTO，
  目标不能按现状独立领取。
- **依赖：** 至少需要 13H、10D/11 的 source capability，以及 F-01 的 command frame result repair；仅依赖 TURN-13 不充分。
- **写集：** 必须冻结 `turn/client/CloudQuestLocalServiceClient.java` 和唯一 DTO 文件（或 client 内 private records），
  同时写清 public activate/capture API、actionId inputs、timeout、PNG bytes ownership。
- **capability：** DHXY Quest adapter 已能产生 typed activate JSON 和一张 QUEST_DETAIL frame；Cloud command caller
  当前只能看到 frame metadata，不能得到 PNG bytes。
- **验收/构建：** activate 严格 parse `{activated}`；capture 严格 parse `{captured}` 并取得同一 command 的原始 PNG，
  metadata/SHA/region 对齐、防御性复制、不暴露 DHXY temp path、不二次下载/截图/retry；Cloud source compile 与
  package cohort 分列。

## 4. 建议后的 Foundation 依赖主干

```text
TURN-00
  -> TURN-01A/B/C -> TURN-01D
  -> TURN-02 -> TURN-02 result-channel repair
  -> TURN-03A -> TURN-03B
  -> TURN-04
TURN-03B + TURN-04 -> TURN-05
TURN-06 + TURN-11 -> TURN-12
TURN-07 -> TURN-08B
TURN-10P + TURN-01B -> TURN-10A/B/C/D -> TURN-10E
TURN-08B + TURN-09 + TURN-10E -> TURN-11
TURN-05 + TURN-12 -> TURN-13
TURN-02 + TURN-05 + TURN-13 -> TURN-13H
TURN-13H -> TURN-14/15/16
TURN-13H + TURN-02 result-channel repair -> TURN-17
```

注意：上述箭头只表示 source/start 充分条件；TURN-40 之前仍没有用户可见 runtime activation。

## 5. 第一轮建议清单

1. 在权威计划正式补 `TURN-13H`，采用本报告的 dormant-only 边界，并把 14..17 的 start dependency 改为 13H。
2. 在 TURN-17 前补 TURN-02 command frame result repair；不改 JSON protocol，不把 PNG Base64 化。
3. 修正 TURN-00 协议规格中的旧 `KEY_PRESS/CLICK` payload 示例。
4. 冻结 14..17 的 client package、actionId 规则、timeout/uncertain mapping 与精确 DTO 文件。
5. 处理 TURN-14 的 FiveRing open-main-bag caller/write-set 矛盾，不能用“后续再说”同时宣称本卡清零 Cloud BagService。
6. 让 13H 或窄 assembly 显式注册 `CloudUiCleanerPort` 等实际 host 依赖；禁止扩大 component scan。
7. 父级先统一 Cloud build 命令与 no-local-test/enforcer 的冲突，再给 Foundation 定最终 package gate。
8. 在 13H、TURN-02 frame repair 和 14/15/16/17 卡片元数据修订完成前，不应把这些卡视为可并行直接实现的 READY 集。

## 6. 审计结论边界

本报告只做第一轮 Foundation 计划 PRECHECK。它不改变任何现有源码复审记录，不给任何卡写最终裁决，
不创建 owner/permit/session/ledger/TTL/durable workflow/retry，也不触发实现、构建或运行。
