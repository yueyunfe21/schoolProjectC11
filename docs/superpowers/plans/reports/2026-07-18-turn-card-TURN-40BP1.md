# CR271 TURN-40BP1 Shared Compile Closure Contract

## Canonical State

- Status: `READY / ZERO OWNER / UNASSIGNED`.
- Type: `REPORT-ONLY PLAN-CONTRACT`.
- Parent card: `TURN-40B`.
- This card is available to External A or External C by canonical whole-card claim at this physical EOF.
- The ledger message is notification only and does not assign an owner.

## Why This Card Exists

Current authorized named tests stop in Cloud main compile before JUnit because the migrated Task and
Navigation sources still reference `AutomationMetricsService`, `CoordinateHelper` and `TextRecognizer`.
Copying the DHXY classes would import filesystem workers, native/window dependencies and OCR provider
state, while stubs/no-ops would alter business and diagnostics truth. The complete transitive closure
must be frozen before TURN-40B implementation.

## Exact Write Set

Only this file may be modified:

- `docs/superpowers/plans/reports/2026-07-18-turn-card-TURN-40BP1.md`

All Java, tests, plans, ledgers, dashboards and other reports are read-only. This card writes no
Java and runs no Maven/runtime/application/server/Task/UI/capture/input.

## Required Audit

1. Start from current Cloud compile references to the three missing types and enumerate every method,
   constructor and field actually consumed by Wubei/FiveRing/Xiuluo/Navigation and tests.
2. Compare each consumed behavior with baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
   and the latest current Cloud typed replacements (`MiniMapPointResolver`, `LocalOcrClient`,
   `ObjectiveTextRecognizer`, turn result/metadata and host scope).
3. Build the full transitive symbol/dependency graph. Do not stop at the first missing class and do
   not propose copying DHXY filesystem/background worker/native/window/provider implementation.
4. For each caller, choose only an evidence-backed route: remove a dead dependency, call an existing
   typed Cloud owner, or define one minimal real Cloud owner with truthful persistence/failure behavior.
   No no-op metrics, constant/null OCR, copied coordinate algorithm, disk-path shim, second store,
   retry, TTL or changed fallback order.
5. Freeze mutually exclusive implementation cohorts with exact production/test write sets and DAG
   edges. Explicitly collision-check TURN-39/39P1, 40B runtime/factory/configuration and all physical
   card EOF owners so two implementation cards can run in parallel where files are disjoint.
6. State the named test family and compile gate for each cohort, plus the final aggregate Cloud compile
   gate. Record any single genuine semantic decision that needs the user; otherwise state that none exists.

## Delivery Contract

Append one `WHOLE-CARD REPORT DELIVERED` section to this card with exact refs, baseline/current SHA/mtime,
the transitive dependency table and complete recommended card contracts. Parent review is required
before Java cards are opened. A first-missing-class list is not delivery.

## Claim Protocol

Append a claim below this line containing `owner`, timestamp, current Cloud branch/HEAD, this card's
pre-claim SHA and acknowledgement of `PARENT-TURN39P1-40BP1-PARALLEL-READY-1003`. First valid physical
EOF claim wins; the other Worker must select the other READY card or remain idle.

<!-- TRUE_EOF: TURN-40BP1 READY ZERO-OWNER UNASSIGNED REPORT-ONLY SHARED-COMPILE-CLOSURE 2026-07-18T10:03:00-04:00 -->

## EXTERNAL-C TURN-40BP1 WHOLE-CARD CLAIMED - 2026-07-18T10:09:00-04:00

- owner: `EXTERNAL-C`（AVAILABLE since TURN-38C Review#1 0/0/0 PASSED；eligible idle Worker）
- ack_parent_message: **`PARENT-TURN39P1-40BP1-PARALLEL-READY-1003`**（收悉：两并行 REPORT-ONLY 卡不派卡自行 claim；TURN-39 仍 BLOCKED 禁领；本卡写集=仅本报告文件；零 Java/test/plan/ledger/dashboard 编辑；零 Maven/runtime/input）。
- current Cloud branch/HEAD: `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`（read-only 取证）。
- 本卡 pre-claim SHA-256: `F7CC6263034A764FFF4A0E36D0BC5763986B3C463F608083FF06DE331F5C69D7`（60L 冻结原文）。
- **预检取证（append 前独立调用）**：全卡零既有 claim section；physical EOF=父级 10:03 READY marker。
- 选卡理由：C 连续经历 37/38B4/38C 三次 build gate 被同一 shared debt 阻断的实录，且持有 TURN-37 import-audit 全表方法论——与本卡 transitive closure 审计最匹配；39P1 留给 A（若 A 恢复）或后续。
- 承诺：仅修改本报告文件；全 Java/test/其它报告只读；零 Git mutation；零 Maven/runtime/UI/capture/input；不自批、不建 reviewer；`D:\mavenProject\DHXY` 只读。
- next: append 后立即回读 physical EOF 证 sole ownership，确认后开始审计（纯读源码+报告写作）。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C WHOLE-CARD CLAIMED ACK=PARENT-TURN39P1-40BP1-PARALLEL-READY-1003 HEAD=3b988caa PRE-CLAIM-SHA=F7CC6263 ZERO-PRIOR-CLAIM AWAIT-EOF-REREAD 2026-07-18T10:09:00-04:00 -->

## PARENT CLAIM AUDIT - 2026-07-18T10:11:00-04:00

- Claim accepted: `EXTERNAL-C` is the sole canonical owner of TURN-40BP1.
- Evidence: pre-claim SHA `F7CC6263...` matches the frozen parent card; branch/HEAD are exact;
  the prior physical EOF was `READY / ZERO OWNER`, and no earlier claim exists.
- State: `REPORT AUDIT ACTIVE`. The only writable path remains this card. No Java/test/other-doc
  write set, Maven, runtime or self-review is authorized.
- TURN-39P1 remains independently `READY / ZERO OWNER / UNASSIGNED`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-CLAIM-ACCEPTED EXTERNAL-C-SOLE-OWNER REPORT-AUDIT-ACTIVE TURN39P1-STILL-READY NO-JAVA-NO-MAVEN 2026-07-18T10:11:00-04:00 -->

## EXTERNAL-C TURN-40BP1 WHOLE-CARD REPORT DELIVERED - 2026-07-18T10:44:00-04:00

全部结论基于实测：Cloud `navigation-migration` HEAD `3b988caa010254973e03342272e6d1d6a9685b01`；基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；DHXY 基线文件纯读。零 Maven/runtime/Java 编辑。

### A. 完整债表（全 Cloud main+test 树 import-audit，非首缺即停）

Cloud main 树共 **13 个缺失类型**，按唯一消费文件干净三分（逐 import 对照 Cloud 树实测）：

| 缺失类型 | 消费文件 | 实际消费面（逐站点实测） |
|---|---|---|
| `metrics.AutomationMetricsService` | WubeiTask(`52E88C68`/4432L)、FiveRingTaskV2(`665188E7`/3026L)、XiuluoTaskV2(`2D4BC1A0`/4483L) | 各 `recordRoundStarted`(393/279/383)+`recordRoundFinished`(472/344/439)；Xiuluo 另 `recordXiuluoFailureCase`(759) |
| `service.BagService` | Wubei、FiveRing | `findAndUseItemFromBack(MAIN_BAG,…)`(W2694)、`findAndUseMainBagTaskPageItem`(W4007)、`findItemPageIndex`(F843)、`withMainBagOpen`(F1114)；另 `BagService.MAIN_BAG` 常量 |
| `service.UICleanerService` | Wubei、FiveRing | `cleanUpAll()`(W617/F767)、`closeAllGenericWindows()`(F1191-1248 ×7) |
| `tools.CoordinateHelper` | NavigationService(`037C5F45`/3109L)、Xiuluo | Nav：`findImageAbsoluteCoordinate`×3(1536/1563/2254)、`getScaledRect`×3(1859/2013/2262)、`findImageInRegion`×2(2267/2268)、`resolveMatchedPointInRect`(2290)；Xiuluo：`getRandomizedPoint(x,y,1,1)`(1473)、`calculateApproachCoordinate`(2006)、`isLogicalCoordinatePlausible(…,80)`(3735) |
| `core.TextRecognizer` | Xiuluo | 唯一站点 `getAllTextResultsForMatch(imagePath, source, predicate)`(3657) |
| `core.GameClientTracker`、`driver.BoundWindowKeyboardService`、`input.InputProvider`、`tools.GameStateUtil`、`vision.GameTextLineOcrService`、`window.runtime.WindowRuntimeContext`、`window.runtime.WindowScopedTempPath`、`window.runtime.WindowTaskContextHolder` | **NavigationService 独占（8 类）** | 全部位于非 active-chain 的 world-map-search/legacy pathing 残块（详 §C-4） |

**test 树债**：①`NavigationTurnContractTest`、`FiveRingTaskTrackerTurnContractTest` import DHXY-local `GameStateUtil/WindowRuntimeContext/WindowTaskContextHolder`（与 Nav 残块同族）。②**真实 test 缺陷**：`WubeiWholeTaskTurnContractTest:30/32/33` import 错包 `cloudbrain.turn.client.CloudTurnActionFactory/CloudTurnCommandPort/CloudTurnCommandResult`（实际在 `cloudbrain.turn.*`）——main 债清偿后该 test 立即以错包失败，现被 main 阻断掩盖。③Javadoc-only 提及（DialogService 3012/3025/3039、NpcClickService 3917、FiveRing 2782/2829、Wubei 3268/3548、MiniMapPointResolver 等）不破编译，DialogOption test 769 为 cutover **负门**（断言无 CoordinateHelper import），可作未来收口 gate 样板。

### B. 基线行为 vs Cloud 既有 typed 替代（逐消费行为）

| 消费行为 | 基线（696a12b0/DHXY 实读） | Cloud 现状 |
|---|---|---|
| `recordRound*/XiuluoFailureCase` | 构造 `AutomationMetricEvent`→`record()` 内存 ledger+`queueDashboardWrite()` 后台 dashboard 写盘 worker（1819L service） | **`model/metrics/AutomationMetricEvent+EventType+Status` 三 typed model 已在 Cloud**（B22E9B64 等）；仅 service 缺失。禁抄面=后台 worker/dashboard 文件 |
| Bag 四法 | DHXY BagService 本地窗口操作 | LOCAL_SERVICE 协议 `TurnLocalOperation` 仅 `BAG_RETURN_ITEM/BAG_USE_INCENSE`（`CloudBagLocalServiceClient`）——**四法零覆盖，需协议扩展** |
| UiCleaner 两法 | DHXY UICleanerService 本地清窗 | **`UI_CLEAN_ALL/UI_CLOSE_GENERIC_WINDOWS` 已在 `TurnLocalOperation` 闭集**，`CloudUiCleanerLocalServiceClient.execute(op…)` 直达——纯 rewire 即可 |
| `getRandomizedPoint` | 纯 Random 抖动数学 | FiveRing/Wubei 已按各自 whole-card 获批「Cloud form」内化同数学（javadoc 明记）——同一获批模式适用 Xiuluo |
| `isLogicalCoordinatePlausible(…,80)` | maps.json transform 纯数学 | `ObjectiveTextRecognizer.coordinatePlausible`（margin-80 语义，`PlayerStateLocationRecognizer` 已消费）——**既有 typed owner 直接可调** |
| `calculateApproachCoordinate` | transform 数学+`tracker` 窗口基址方向步进+龙窟/凤巢 cave 原点特例 | `MiniMapPointResolver`（mirrors CoordinateHelper@91d3b07）现仅 `resolveMinimapClick(JsonNode)`——approach 需按窗口 rect(=TurnWindowMetadata) 改写或 typed owner 增法 |
| `getScaledRect/findImage*` (Nav) | `tracker.refreshWindowState/updateGlobalVision` 捕屏文件+ImageFinder+scaleRatio | turn 模型=capture action+`CloudTemplateCatalog` 匹配（LeftTop/NpcClick 既有形态）；但站点全在 Nav 残块（§C-4） |
| `getAllTextResultsForMatch(imagePath…)` | OCR provider 链按图路径出词表 | Cloud 有 `LocalOcrClient`(295L)/`CloudOcrTextMatcher`；turn 模型=capture 帧→OCR，不落盘路径 |

**注入合同面**：三 Task+NavigationService 均 Lombok `@RequiredArgsConstructor`（final fields=构造参数）；whole-task test 以全 null legacy 协作者构造（Wubei 25 参，`null // AutomationMetricsService` 等），metrics/bag/ui 不被现 battery 驱动。

### C. 互斥实施 cohort（写集不相交，可并行；碰撞检查见 §D）

- **C1`40B-METRICS`（最小真实 Cloud metrics owner）**：Create `com.bot.dhxy.metrics.AutomationMetricsService`（Cloud 新文件；仅三消费方法+@Component；实现=有界内存事件账本，复用既有 `model/metrics` 三 typed model，真实记录可查询——非 no-op；**无后台线程/dashboard 写盘/文件路径**；record 失败不淹没任务路径的 truthful 语义按基线 record() 对齐）+Create 唯一 named test。**三 Task 文件零编辑**（同包同 API import 即解）。解锁 Wubei/FiveRing/Xiuluo 的 metrics 维度。
- **C2`40B-BAGUI`（Wubei/FiveRing bag+ui rewire）**：Modify WubeiTask+FiveRingTaskV2：ui 两法 rewire 至既有 `CloudUiCleanerLocalServiceClient`（协议已覆盖）；bag 四法走 **LOCAL_SERVICE 协议扩展**（双仓 byte-identical `TurnLocalOperation` 增 `BAG_FIND_USE_FROM_BACK/BAG_FIND_USE_TASK_PAGE_ITEM/BAG_FIND_ITEM_PAGE_INDEX/BAG_WITH_MAIN_BAG_OPEN` 类闭集 op+DHXY executor 映射至既有 BagService——与 TURN-27 Amendment bridge/38B 系列同模式，实现留 DHXY 本地不迁）+修 `WubeiWholeTaskTurnContractTest` 错包 import+两 whole-task test 构造参数随动。
- **C3`40B-XIULUO`（Xiuluo coordinate/OCR 内化）**：Modify XiuluoTaskV2 四站点：randomize→Cloud-form 纯数学（FiveRing/Wubei 获批同模式）；plausible→直调 `ObjectiveTextRecognizer.coordinatePlausible`；approach→以 `TurnWindowMetadata.windowRect` 改写方向步进+保 cave 特例（或 MiniMapPointResolver 增 typed 法，二选一由实施卡合同冻结）；OCR→capture 帧+`LocalOcrClient`（去 imagePath）。+named test 扩展。
- **C4`40B-NAV`（NavigationService 残块收口）**：**PLAN-BLOCKED 于 TURN-39P1 父级过审**——8 独占类+CoordinateHelper 九站点与 `submitExclusiveAndWait`/InputProvider 同块，且 `WindowRuntimeContext=KEEP_LOCAL_RUNTIME`（TURN-27 Amendment#1 边界）；路由（逐 action 迁移 vs 死块删除）必须依 39P1 的 live/dead caller 冻结表决定。写集=NavigationService+NavigationTurnContractTest+FiveRingTaskTrackerTurnContractTest（同族 test 债随卡）。

### D. DAG 与碰撞检查

- 依赖边：`39P1 →(过审)→ C4`；C1/C2/C3 零前置、两两文件不相交、与 C4 不相交（Nav 文件仅 C4）→ **C1/C2/C3 可即刻并行开卡，C4 等 39P1**。
- 碰撞：当前全部已收口卡（27/28/34C/36/37/38B1-B4/38C）owner 均已释放；39P1 READY/ZERO-OWNER（写集=其报告文件，无 Java 交集）；TURN-39 本体 BLOCKED 未领；无其它 physical-EOF owner。C2 的双仓协议文件（TurnLocalOperation 等）需与未来 39 实施卡协调——39P1 报告应对 `InputSequences` 映射避开 LOCAL_SERVICE op 闭集文件，或两卡串行（推荐后者：协议文件单 owner 原则）。
- Xiuluo 文件仅 C3 触碰；metrics 新文件仅 C1；Wubei/FiveRing 仅 C2——互斥成立。

### E. Named test / compile gate

- C1：新 named test（建议 `AutomationMetricsServiceTurnTest`）+`mvn -q -Dtest=AutomationMetricsServiceTurnTest test`。
- C2：`WubeiWholeTaskTurnContractTest,FiveRingWholeTaskTurnContractTest`（含错包修复后首次可编译验证）。
- C3：`XiuluoWholeTaskTurnContractTest`。
- C4：`NavigationTurnContractTest`（现 source-only 交付状态转可执行）。
- **聚合 gate**：C1-C4 全收口后 `mvn -q compile` 零错，随后既往被阻 named family 全量回放（ScopedPngArtifactStoreTurnTest、38C 三 test family、38B 系列等）——即 TURN-40B 实施完成判据。

### F. 语义决策点

**无需用户裁决的业务语义分歧**。唯一父级架构裁决点：C1 采用**同包最小真实 owner**（三 Task 零编辑，推荐）vs Task rewire 至 cloudbrain 包新 port（更大 diff、同语义）——两者业务真值等价，属父级合同选择非用户业务决策。C2 的 bag 四 op 命名/闭集扩展属实施卡合同冻结项。

`无已批准业务差异`；本报告纯审计，未动任何 Java/test/其它文档。零 Git mutation（rev-parse/sha256 只读）；零 Maven；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。请求：**父级 report review**（过审后按 §C 开 C1/C2/C3 实施卡，C4 等 39P1）。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C WHOLE-CARD-REPORT-DELIVERED 13-TYPES 3-COHORTS+NAV-BLOCKED-ON-39P1 C1-METRICS-SAME-PACKAGE C2-BAGUI-PROTOCOL-EXT C3-XIULUO-INLINE C4-AFTER-39P1 TEST-DEFECT-WUBEI-WRONG-PACKAGE REQUEST-REVIEW OWNER-C NO-MAVEN 2026-07-18T10:44:00-04:00 -->

## PARENT REPORT REVIEW #1 - 2026-07-18T10:55:00-04:00

- Verdict: `P0/P1/P2 = 0/2/2`; `REPORT REVIEW BLOCKED / REPAIR REQUIRED / EXTERNAL-C OWNER`.
- Review basis: full card/ledger EOF, all 88 Sprint rows and fixed-report EOFs; Cloud HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`; baseline
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; exact source signatures and call sites. No Java/Maven/runtime.

### P1-1 - C1 silently removes the existing persisted metrics contract

- Evidence: delivered lines 108/121 acknowledge the baseline event queue/dashboard writer, but C1 then freezes an
  in-memory-only owner with no persistence. Current Cloud `AutomationMetricEvent` itself documents a persisted
  business event written to `logs/automation-metrics.jsonl`. Baseline `AutomationMetricsService.record()` completes
  identity/time/session fields, updates aggregates, invokes diagnostic capture, enqueues the event and throttles the
  dashboard; `recordRoundFinished()` also explicitly queues a dashboard write.
- Impact: a same-package class that only retains bounded memory makes the three Tasks compile while silently dropping
  restart-visible metrics/failure-case linkage and the typed model's declared persistence truth. That is not the
  required minimal real owner with truthful persistence/failure behavior.
- Repair condition: audit and freeze one real persistence route using an existing authority (or a typed existing
  local-service boundary) without copying the old background worker, adding a second store, or changing Task failure
  behavior. State exact retention/restart/failure semantics, exact files and tests. If no existing authority can close
  this without changing the approved diagnostics contract, record the single parent/user decision instead of calling
  the routes equivalent.

### P1-2 - C2 does not close the real `withMainBagOpen` transitive boundary

- Evidence: `FiveRingTaskV2.checkFiveRingSuppliesInOneBagSession()` lines 1114-1122 passes a generic
  `Function<MainBagSession, FiveRingSupplyCheck>` that, in one exclusive bag-open interval, first calls
  `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag(...)`, then `MainBagSession.countItemUpTo(...)`, and returns
  `(incenseRefilled, firstPageIndex, count)`. Baseline `BagService.withMainBagOpen()` lines 135-147/185-196 owns one
  queued exclusive callback and exactly one open/close cycle. A closed `BAG_WITH_MAIN_BAG_OPEN` enum operation cannot
  serialize this caller-owned lambda, and the four proposed bag operations do not express its compound result or
  atomic ordering.
- Impact: implementing the delivered contract either fails to compile, nests/splits input ownership, or changes the
  validated one-session supply-check behavior. The claimed full transitive graph therefore stops before
  `PlayerStateService`, `MainBagSession`, incense-use and item-count result ownership.
- Repair condition: freeze a specialized typed operation/result for this exact supply check (or another existing
  evidence-backed boundary) that preserves one open/close cycle, operation order, stop propagation and all three
  result fields. Enumerate every transitive production/test/protocol/validator/dispatcher/executor/client path; do not
  transmit a lambda or introduce a generic remote callback.

### P2-1 - Exact implementation write sets are not frozen

- C2 says `TurnLocalOperation` "etc." and "executor mapping" without naming the mirrored argument/result models,
  validator, dispatcher, client and tests. C3 leaves approach ownership as an implementation-time choice between
  inline logic and a `MiniMapPointResolver` API. Required Audit step 5 requires exact production/test paths and mutually
  exclusive cohorts before cards open; alternatives cannot be deferred to the Java writer.
- Repair condition: list every absolute repository-relative production/test file for C1-C4, select one C3 route, and
  rerun collision checks against TURN-39P1/39 and existing dirty owners.

### P2-2 - C2's named gate does not cover its protocol blast radius

- The two whole-task tests cannot prove dual-repository enum/arguments/results byte identity, validator shape,
  dispatcher routing, local executor behavior, strict Cloud result mapping, one-session atomicity or STOP/UNKNOWN
  terminals. The wrong-package Wubei import is correctly found, but fixing it is not the protocol acceptance gate.
- Repair condition: freeze exact retained named tests for mirrored protocol golden JSON, validator, dispatcher/executor,
  Cloud client result mapping and the FiveRing one-session supply result, plus the two whole-task tests and aggregate
  compile gate.

No implementation card may open from this delivery. C remains sole report owner and must append one complete canonical
whole-card report re-delivery after all four findings are closed. `TURN-39P1` remains independently READY/zero owner.
Required ACK: `ack_parent_message=PARENT-TURN40BP1-REVIEW1-REPAIR-1055`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-REPORT-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=2 OWNER-C METRICS-PERSISTENCE-GAP GENERIC-BAG-SESSION-GAP EXACT-WRITESET+TEST-GATE-GAPS MESSAGE=PARENT-TURN40BP1-REVIEW1-REPAIR-1055 NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T10:55:00-04:00 -->

## PARENT ACK AUDIT - 2026-07-18T11:04:00-04:00

- C's 11:02 STATUS EVENT named `ack_parent_message=PARENT-TURN40BP1-REVIEW1-REPAIR-1055` and accepted all
  `P0/P1/P2=0/2/2` findings without qualification.
- State: `REPORT REVIEW #1 REPAIR ACTIVE / EXTERNAL-C SOLE OWNER`. The only writable path remains this report.
- Accepted repair direction: evidence-select a real metrics persistence authority; replace the generic bag callback
  proposal with one exact typed FiveRing supply-check operation/result; freeze exact C1-C4 paths, one C3 route and the
  complete mirrored protocol/test matrix before canonical re-delivery.
- No finding is closed by the ACK itself. No Java card, Maven/runtime/input or owner release is authorized.

<!-- TRUE_EOF: TURN-40BP1 PARENT-ACK-AUDIT REVIEW1-REPAIR-ACTIVE OWNER-C ACK=PARENT-TURN40BP1-REVIEW1-REPAIR-1055 P0=0 P1=2 P2=2 REPORT-ONLY NO-JAVA-NO-MAVEN 2026-07-18T11:04:00-04:00 -->

## EXTERNAL-C TURN-40BP1 CANONICAL WHOLE-CARD REPORT RE-DELIVERY (REVIEW #1 REPAIRED) - 2026-07-18T11:19:00-04:00

Review#1 四 finding（`PARENT-TURN40BP1-REVIEW1-REPAIR-1055` 已 11:02 具名 ACK）全部闭合。本节为完整 canonical 重交，取代 10:44 版；未变结论按原文重申，变更处标注。实测基点不变：Cloud HEAD `3b988caa`；基线 `696a12b0`；纯读零 Java/Maven。

### A'. 债表修正（同包盲区闭合）

10:44 版 §A 13 类债表**维持**，一处修正+一处方法论披露：
- **修正**：`service.BagService` 消费者补第四个——Cloud `PlayerStateService.java:576` 真代码引用 `BagService.MainBagSession`（`ensureSheYaoXiangActiveInOpenMainBag(MainBagSession, TaskExecutionContext)`，体内 `mainBag.useItem(template, null)`；javadoc 自记「Keep the 696 open-main-bag item user until TURN-36 migrates that complete boundary」；全仓唯一 caller=FiveRing supply-check lambda）。
- **方法论披露**：import-level 审计对**同包引用**盲（PlayerStateService 与 BagService 同包 `com.bot.dhxy.service` 无需 import）。已对全 13 类做符号级同包补扫：仅此一处新增真引用（`CloudBagStateOwner:15` 为 javadoc-only）。其余 §A 表、test 债、Wubei 错包 import 缺陷、Javadoc-only 核销原文有效。

### P1-1 闭合：metrics 持久路线冻结（route b：LOCAL_SERVICE 回传 DHXY 真身）

- **两路线实测终审**：(a) Cloud 侧以既有 authority 落盘=在 Cloud 造第二 metrics store 且脱离 `AutomationMetricEvent` 自文档的 `logs/automation-metrics.jsonl` 单文件合同（基线 `record()` 的字段补全/聚合/诊断捕获/dashboard 节流全链只在 DHXY 真身）——必然变更获批诊断合同→触发 user 决策，**弃**。(b) **冻结采用**：DHXY-cr271 保有完整真身（`metrics/AutomationMetricsService.java`+DiagnosticCaseCaptureService/UploaderService 族，实测在盘）；新 `METRIC_*` LOCAL_SERVICE op 族把 Cloud Task 的三个 record 调用回传 DHXY 真身执行——jsonl/dashboard/聚合/诊断捕获/节流逐字保留，零复制后台 worker、零第二 store。
- **精确语义冻结**：①Cloud 侧新建同包 facade `com.bot.dhxy.metrics.AutomationMetricsService`（仅三个基线签名方法；每方法以首参 `TaskExecutionContext.getTurnGameClient()` 取 exact-bound client 发一次 `LOCAL_SERVICE` 调用）——三 Task 文件零编辑、test null 注入不变。②失败语义：metrics=诊断非业务真值（38M 分类冻结），Cloud facade 对非 EXECUTED/传输异常一律记日志后正常返回（void），Task 失败行为零变化；STOP 传播不经 metrics 路径。③retention/restart 语义=基线原样（jsonl append-only、dashboard 节流重写、进程重启由 DHXY 真身既有逻辑负责），Cloud 侧零保留零重放。
- **写集（C1，精确逐文件）**：双仓 byte-identical Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`（+`METRIC_RECORD_ROUND_STARTED/METRIC_RECORD_ROUND_FINISHED/METRIC_RECORD_XIULUO_FAILURE_CASE`）、Create `.../protocol/TurnMetricOperationArguments.java`（intent+roundId/roundNumber/roundType/status/resultCode/message/elapsedMs/attributes/caseId/reason/phase，nullable 按 op）、Modify `.../protocol/TurnProtocolValidator.java`；Cloud Create `src/main/java/com/bot/dhxy/metrics/AutomationMetricsService.java`（facade）；DHXY-cr271 Create `src/main/java/com/bot/dhxy/cloud/turn/local/MetricsLocalOperationExecutor.java`（thin adapter→真身三方法）+Modify `.../turn/LocalTurnActionExecutor.java` 与 `.../turn/TurnConfiguration.java`（dispatch 接线）。

### P1-2 闭合：FiveRing supply-check 专用 typed op 冻结

- **合同**：新闭集 op `BAG_FIVERING_SUPPLY_CHECK`（args 走 `TurnBagOperationArguments` 扩展：新 intent `FIVERING_SUPPLY_CHECK`+`targetItemTemplate`=鞋模板+`maxBagIndex`=requiredShoeCount 复用现字段，零新 args model）。DHXY `BagLocalOperationExecutor` 新 case：**一次** `bagService.withMainBagOpen(...)` 内按基线固定序执行 stop-checkpoint→incense-ensure（调 DHXY 侧既有 PlayerStateService 真身逻辑）→stop-checkpoint→`countItemUpTo`→关包，返回三字段 typed 结果 `(incenseRefilled, firstPageIndex, count)`（载于既有 `TurnLocalServiceResult` payload，Cloud client 严格映射；STOPPED/UNKNOWN 终态原样传播不造成功）。**零 lambda/泛型远程回调/会话句柄**：复合语义整体落 DHXY（MainBagSession 与 BagService 队列独占所在地），单 open/close 周期+操作序+三字段结果+STOP 传播全部由闭集 op 合同承载。
- **连带路由（dead-dependency removal）**：Cloud `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag`（全仓唯一 caller=该 lambda）随 FiveRing 改调 op 而失去 caller→**删除该方法**（javadoc 已自证为待迁边界），即闭 PlayerStateService 对 `BagService.MainBagSession` 的同包引用——BagService 在 Cloud 的第四消费面同卡消失。
- **transitive 路径表（全枚举）**：protocol=`TurnLocalOperation`+`TurnBagOperationArguments`（双仓 byte-identical）；validator=`TurnProtocolValidator`（双仓）；DHXY dispatch/executor=`LocalTurnActionExecutor`（既有 LOCAL_SERVICE case，零改）→`BagLocalOperationExecutor`（新 case）；Cloud client=`turn/client/CloudBagLocalServiceClient.java`（新方法 `executeFiveRingSupplyCheck`+typed 结果 record+严格 outcome 映射）；Cloud caller=`FiveRingTaskV2.checkFiveRingSuppliesInOneBagSession`（lambda→单 op 调用）+`PlayerStateService`（删法）；test=见 P2-2 矩阵。

### P2-1 闭合：C1-C4 精确写集冻结+碰撞重检

**C3 定一路**：Xiuluo 内化 Cloud-form（弃 MiniMapPointResolver 增法）——randomize(1,1)/approach（`TurnWindowMetadata.windowRect` 供窗口基址+方向步进+龙窟/凤巢 cave 原点特例逐字保留）/plausible→直调 `ObjectiveTextRecognizer.coordinatePlausible`/OCR→capture 帧+`LocalOcrClient`。

| Cohort | 精确写集（repo-relative，Cloud=dhxy-cloud-brain，DHXY=DHXY-cr271） |
|---|---|
| **C1 metrics** | Cloud Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`、Create `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnMetricOperationArguments.java`、Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`、Create `src/main/java/com/bot/dhxy/metrics/AutomationMetricsService.java`；DHXY 同三 protocol 镜像+Create `src/main/java/com/bot/dhxy/cloud/turn/local/MetricsLocalOperationExecutor.java`+Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`+`src/main/java/com/bot/dhxy/cloud/turn/TurnConfiguration.java`；test 见 P2-2 |
| **C2 bag/ui** | Cloud Modify `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`、`src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`、`src/main/java/com/bot/dhxy/service/PlayerStateService.java`（删法）、`src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`（+FIND_AND_USE_FROM_BACK/FIND_ITEM_PAGE_INDEX/FIVERING_SUPPLY_CHECK 三法）、`src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudUiCleanerLocalServiceClient.java`（如需 cleanUpAll 便捷面，或直用既有 execute）、双仓 Modify `TurnLocalOperation.java`+`TurnBagOperationArguments.java`+`TurnProtocolValidator.java`；DHXY Modify `src/main/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutor.java`；test 见 P2-2（含 Wubei 错包 import 修复） |
| **C3 Xiuluo** | Cloud Modify `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`+`src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java`（仅此二文件） |
| **C4 Nav** | Cloud Modify `src/main/java/com/bot/dhxy/service/NavigationService.java`+`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`+`src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingTaskTrackerTurnContractTest.java`；**PLAN-BLOCKED 于 39P1 过审** |

**DAG 修正（诚实更正 10:44 版）**：C1 与 C2 共触双仓 `TurnLocalOperation/TurnProtocolValidator`+golden tests——**非不相交**。按协议文件单 owner 原则冻结 **C1→C2 串行**（C1 先行，写集小）；**C3 与 C1/C2/C4 全不相交可即刻并行**；C4 等 39P1。碰撞重检：39P1（READY/ZERO-OWNER，写集=其报告文件）零 Java 交集；39 本体 BLOCKED 未领；全部已收口卡 owner released；PlayerStateService 现无任何卡 EOF owner；无其它 dirty owner。39P1 报告若涉 `InputSequences`→LOCAL_SERVICE 映射，其协议文件写集须与 C1/C2 串行排程（同一单 owner 原则），已在依赖边显式声明。

### P2-2 闭合：完整 named test / compile gate 矩阵

| Gate | Retained named tests（双仓既有五件套+专项） |
|---|---|
| 协议镜像 golden | 双仓 `TurnActionGoldenJsonTest`+`TurnCoreProtocolGoldenJsonTest`+`TurnEnvelopeGoldenJsonTest`（新 op/args/result 进 golden JSON，byte-identical 镜像证明） |
| validator | 双仓 `TurnProtocolValidatorContractTest`（新 op 参数约束/非法拒绝） |
| DHXY executor | DHXY 既有 executor 合同 test 族扩展（`LocalTurnActionExecutor` 契约 test+`BagLocalOperationExecutor`/`MetricsLocalOperationExecutor` 专项：单 open/close 恰一次、固定序、STOP 传播、metrics 真身直达） |
| Cloud client 映射 | `CloudBagLocalServiceClient` 三新法严格 outcome 映射+`AutomationMetricsService` facade test（非 EXECUTED→log-only 返回、零 Task unwinding、supply-check 三字段映射/STOPPED-UNKNOWN 不造成功） |
| whole-task | `WubeiWholeTaskTurnContractTest`（含错包 import 修复）+`FiveRingWholeTaskTurnContractTest`（含 one-session supply 结果三字段断言）+`XiuluoWholeTaskTurnContractTest`（C3） |
| C4 | `NavigationTurnContractTest`（source-only→可执行） |
| **聚合** | C1-C4 收口后 `mvn -q compile` 零错→全部既往被阻 named family 回放（ScopedPngArtifactStoreTurnTest、38C 三件套、38B 系列）=TURN-40B 完成判据 |

### F'. 语义决策

**零 user 决策**（route (a) 因触发诊断合同变更被证据淘汰，route (b) 合同保持性无需裁决）。父级合同确认点两枚：①C1 Cloud facade 同包放置（三 Task 零编辑）；②C2 删除 Cloud `PlayerStateService.ensureSheYaoXiangActiveInOpenMainBag`（dead-dependency removal，javadoc 自证待迁边界）。均为架构确认非业务语义分歧。

`无已批准业务差异`；本报告纯审计，未动任何 Java/test/其它文档。零 Git mutation；零 Maven；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。请求：**Report Review #2**。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C CANONICAL-REPORT-REDELIVERY REVIEW1-REPAIRED P11-METRICS-ROUTE-B-LOCAL-SERVICE P12-FIVERING-SUPPLY-OP+PLAYERSTATE-DEAD-METHOD P21-EXACT-WRITESETS+C1-C2-SERIAL P22-FULL-TEST-MATRIX SAME-PACKAGE-BLINDSPOT-DISCLOSED REQUEST-REVIEW2 OWNER-C NO-MAVEN 2026-07-18T11:19:00-04:00 -->

## PARENT REPORT REVIEW #2 - 2026-07-18T11:32:00-04:00

- Verdict: `P0/P1/P2 = 0/2/2`; `REPORT REVIEW BLOCKED / REPAIR REQUIRED / EXTERNAL-C OWNER`.
- Review basis: canonical 11:19 re-delivery, current mirrored protocol and dispatcher/executor source,
  Cloud callers, DHXY metrics/BagService truth and baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
  No Java/Maven/runtime/input was executed.

### P1-1 - The frozen metrics wire cannot call the DHXY authority without losing persisted identity/linkage

- Evidence: the C1 argument list at line 238 contains round/result fields and `caseId`, but not the baseline
  `TaskExecutionContext` identity fields (`taskCode/taskName/windowId/windowRole/nativeWindowHandle`) consumed by
  `AutomationMetricsService.baseEvent(context)`, nor the full `caseDir` consumed by
  `recordXiuluoFailureCase()` and persisted in `attributes.caseDir`. The proposed local adapter nevertheless claims
  to call the three true methods. Current `TurnLocalServiceCall` also has no metric argument slot, and that mirrored
  file is absent from C1's exact write set.
- Impact: the adapter either cannot compile, calls the authority with null/synthetic incomplete context, or reduces
  a Cloud failure-case path to only its basename. Any of those loses the exact window/task identity or failure-case
  linkage that route (b) was selected to preserve.
- Repair condition: freeze one complete metric wire shape and exact local reconstruction/recording route. It must
  carry every persisted identity and full failure-case locator required by the three baseline methods, name the
  mirrored `TurnLocalServiceCall` change and dispatcher route, and state whether the local authority receives a
  fully reconstructed context or an exact typed event. Do not invent a second context/store or silently turn the
  Cloud filesystem path into a DHXY-local path.

### P1-2 - The supply-check operation still nests the single input queue under the current dispatcher

- Evidence: `LocalServiceStepDispatcher.execute()` lines 55-67 wraps Bag operations in
  `InputSequences.submitExclusiveAndWait`; `BagLocalOperationExecutor` explicitly documents that it runs inside
  that callback and therefore calls direct Bag macros. The re-delivery instead requires its new Bag executor case
  to call public `BagService.withMainBagOpen()`, whose lines 155-164 acquire the same single queue again. The same
  ownership mismatch applies to the proposed public `findItemPageIndex`/find-and-use routes, which own their own
  queue boundaries.
- Impact: adding `BAG_FIVERING_SUPPLY_CHECK` to the existing wrapped Bag switch deadlocks queue-in-queue; routing it
  outside that switch is a required contract decision, not an implementation detail. The claimed one-session and
  STOP semantics therefore are not yet executable.
- Repair condition: freeze the operation-by-operation dispatcher ownership: legacy direct macros remain under the
  outer exclusive callback, while every public BagService entry that acquires its own queue must be dispatched
  without an outer callback. Include the exact `LocalServiceStepDispatcher` change and a retained test proving no
  nested submission, one open/close cycle, fixed incense/count order and STOP propagation.

### P2-1 - Exact production/test write sets remain incomplete and partly incorrect

- C1/C2 both omit mirrored `TurnLocalServiceCall.java`; both omit DHXY
  `LocalServiceStepDispatcher.java`, even though the enum switch must become exhaustive and route metrics/bag
  operations to different ownership branches. C1 lists `LocalTurnActionExecutor.java` and `TurnConfiguration.java`
  without identifying a required change in either existing Spring/component chain, while omitting the actual
  dispatcher constructor/switch and `LocalServiceStepDispatcherContractTest` constructor matrix.
- C2 also does not freeze the `BagLocalOperationExecutor` constructor/test changes needed to inject the DHXY
  `PlayerStateService` authority used by the specialized open-bag incense path.
- Repair condition: replace the C1/C2 tables with exact mirrored production and retained-test paths derived from the
  real constructor/switch chain; remove paths with no required diff and include every constructor callsite/test.

### P2-2 - The test matrix is still family-level rather than an exact retained named gate

- Lines 263-267 say “双仓五件套”, “既有 executor 合同 test 族” and facade/client tests without freezing the exact
  class/path for several new tests. It omits the dispatcher constructor/routing test that would expose both missing
  metric routing and the nested Bag queue. It also has no assertion for full metrics identity and full `caseDir`.
- Repair condition: list exact test class names and repository-relative paths for every new/modified test, including
  mirrored call-shape golden/validator, dispatcher routing/constructor, metrics identity+caseDir preservation,
  Bag queue ownership/one-session order, Cloud strict mapping and whole-task callers.

No implementation card may open from this re-delivery. C remains sole report owner and must append a complete
canonical report re-delivery after these four findings close. `TURN-39P1` remains independently READY/zero owner.
Required ACK: `ack_parent_message=PARENT-TURN40BP1-REVIEW2-REPAIR-1132`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-REPORT-REVIEW2 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=2 OWNER-C METRICS-WIRE-IDENTITY+CASEDIR-GAP BAG-QUEUE-NESTING-GAP DISPATCHER+CALL-WRITESET+EXACT-TEST-GAPS MESSAGE=PARENT-TURN40BP1-REVIEW2-REPAIR-1132 NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T11:32:00-04:00 -->

## PARENT ACK AUDIT - 2026-07-18T11:39:00-04:00

- C's 11:37 STATUS EVENT named `ack_parent_message=PARENT-TURN40BP1-REVIEW2-REPAIR-1132` and accepted all
  `P0/P1/P2=0/2/2` findings without qualification; communication is closed-loop.
- State: `REPORT REVIEW #2 REPAIR ACTIVE / EXTERNAL-C SOLE OWNER`. The only writable path remains this report.
- Accepted repair scope: freeze complete metrics identity/full-caseDir wire and exact local authority route; split
  dispatcher ownership so public self-queued BagService operations never run inside the outer exclusive callback;
  correct all production/constructor/test paths and exact retained named gates.
- ACK does not close any finding. No Java card, Maven/runtime/input or owner release is authorized. TURN-39P1 remains
  independently `READY / ZERO OWNER / UNASSIGNED`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-ACK-AUDIT REVIEW2-REPAIR-ACTIVE OWNER-C ACK=PARENT-TURN40BP1-REVIEW2-REPAIR-1132 P0=0 P1=2 P2=2 REPORT-ONLY TURN39P1-READY-ZERO-OWNER NO-JAVA-NO-MAVEN 2026-07-18T11:39:00-04:00 -->

## EXTERNAL-C TURN-40BP1 CANONICAL WHOLE-CARD REPORT RE-DELIVERY #2 (REVIEW #2 REPAIRED) - 2026-07-18T11:49:00-04:00

Review#2 四 finding（`PARENT-TURN40BP1-REVIEW2-REPAIR-1132` 已 11:37 具名 ACK）全部以新实测证据闭合。本节取代 11:19 版；§A'（13+1 债表/同包盲区）、C3/C4 合同、DAG（C1→C2 串行、C3 并行、C4 等 39P1）、聚合 gate 原文有效，下述四节为修正冻结。

### P1-1'' 闭合：metric wire=完整 typed event + DHXY 真身最小 public seam

- **新实测**：DHXY 真身 `AutomationMetricsService.record(AutomationMetricEvent)` 为 **public**(:155)；`baseEvent(context)`(:389) 与 `queueDashboardWrite`(:576) 为 **private**；基线三方法行为=ROUND_STARTED→record；ROUND_FINISHED→record+`queueDashboardWrite("round-finished")`(:290)；XIULUO_FAILURE_CASE→record。
- **冻结路线（exact typed event，非重构 context）**：Cloud facade 在发送侧从其 `TaskExecutionContext` 预计算 baseEvent 全部持久身份字段（`taskCode/taskName/windowId/windowRole/nativeWindowHandle`）+事件字段（eventType/status/runId=roundId/phase/message/elapsedMs/errorCode/caseId）+**完整 attributes（含 `caseDir` 全路径 verbatim Cloud 字符串，永不改写为 DHXY-local 路径）**+round 结构字段，装入新 wire model；DHXY `MetricsLocalOperationExecutor` 逐字段重建 `AutomationMetricEvent` 后调**新最小 public seam** `AutomationMetricsService.recordWireEvent(AutomationMetricEvent event, boolean queueDashboard)`（=`record(event)`+当 `queueDashboard` 为真时 `queueDashboardWrite("round-finished")`；仅此一法，零第二 store/context/节流复制）。三 op 的 queueDashboard 映射：STARTED=false/FINISHED=true/XIULUO_FAILURE_CASE=false（与基线逐字对齐）。失败语义不变：facade 对非 EXECUTED/传输异常 log-only 返回 void。
- **mirrored 写集修正**：`TurnLocalServiceCall.java` 增 `TurnMetricEventPayload metric` 槽（双仓）；新 model `TurnMetricEventPayload.java`（双仓，上述全字段）；dispatcher 路由=新增**无包裹** `case METRIC_* -> metricsAdapter.execute(call)`（metrics 零输入队列语义）。

### P1-2'' 闭合：逐 op dispatcher 所有权冻结（消队列嵌套）

- **新实测**：`LocalServiceStepDispatcher.java`(101L，@Component) 穷尽 switch 无 default——任何新 enum 值必改此文件；现行三型所有权：①`BAG_RETURN_ITEM/BAG_USE_INCENSE/GIVE_ITEM_*`=外层 `inputSequences.submitExclusiveAndWait` 回调内调直宏 adapter；②`UI_*/QUEST_*`=adapter 自持队列边界、**不包裹**；③`WHOLE_TASK_*`=无队列。
- **冻结分派**：新三个 bag op（`BAG_FIVERING_SUPPLY_CHECK/BAG_FIND_AND_USE_FROM_BACK/BAG_FIND_ITEM_PAGE_INDEX`）调的 public BagService 入口自持队列→归**第②型**：新无包裹 arm `-> bagAdapter.executeQueueOwning(call)`；legacy 直宏 arm 原位零改动。`BagLocalOperationExecutor` 新增独立入口 `executeQueueOwning`（内部：SUPPLY_CHECK=单次 `bagService.withMainBagOpen` 内 checkpoint→`playerStateService.ensureSheYaoXiang…`（DHXY 真身，**构造注入 PlayerStateService**）→checkpoint→`countItemUpTo`→三字段结果；FIND_AND_USE_FROM_BACK/FIND_ITEM_PAGE_INDEX=对应 public 单法）；javadoc 双入口所有权边界明文。`METRIC_*` 亦无包裹（见 P1-1''）。无嵌套提交/单 open-close/固定序/STOP 传播由 `LocalServiceStepDispatcherContractTest`+`BagLocalOperationExecutorContractTest` 断言（见 P2-2''）。

### P2-1'' 闭合：写集按真实构造/switch 链重冻（删无 diff 路径）

- **实测修正**：dispatcher 与五 executor 均 `@Component` 构造注入自动装配——`LocalTurnActionExecutor.java`/`TurnConfiguration.java` **无必要 diff，自写集删除**（Review#2 要求）。
- **C1 终版写集**：双仓 Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`、Create `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnMetricEventPayload.java`、Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalServiceCall.java`、Modify `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`；Cloud Create `src/main/java/com/bot/dhxy/metrics/AutomationMetricsService.java`（facade）；DHXY Create `src/main/java/com/bot/dhxy/cloud/turn/local/MetricsLocalOperationExecutor.java`、Modify `src/main/java/com/bot/dhxy/metrics/AutomationMetricsService.java`（+`recordWireEvent` 唯一 seam）、Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`（ctor+METRIC arm）。
- **C2 终版写集**：双仓 Modify `TurnLocalOperation.java`+`TurnBagOperationArguments.java`（三新 intent；requiredCount 复用 `maxBagIndex`）+`TurnProtocolValidator.java`（`TurnLocalServiceCall` 已有 bag 槽零 diff，不列）；DHXY Modify `src/main/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutor.java`（ctor+PlayerStateService 注入+`executeQueueOwning`）、Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`（bag queue-owning arm；与 C1 同文件=C1→C2 串行依据再证）；Cloud Modify `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`、`src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`、`src/main/java/com/bot/dhxy/service/PlayerStateService.java`（删 `ensureSheYaoXiangActiveInOpenMainBag`）、`src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`（+三法+typed 结果）；`CloudUiCleanerLocalServiceClient` 零 diff 不列（Task 直用既有 `execute(op…)`）。
- C3/C4 写集不变（11:19 版原文）。碰撞重检结论不变（39P1 协议文件串行排程声明维持）。

### P2-2'' 闭合：exact retained named test 矩阵（逐类逐路径）

| # | Test（repo-relative） | 断言要点 |
|---|---|---|
| 1 | 双仓 Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java` | 新 op/payload 的 action 级 golden JSON 双仓 byte-identical |
| 2 | 双仓 Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java` | `TurnMetricEventPayload`/`TurnLocalServiceCall` metric 槽/`TurnBagOperationArguments` 新 intent 的 golden 镜像 |
| 3 | 双仓 Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnEnvelopeGoldenJsonTest.java` | envelope 级镜像不漂移 |
| 4 | 双仓 Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` | METRIC/BAG 新 op 参数约束+非法拒绝（缺身份字段/缺 caseDir 的 FAILURE_CASE 拒收等） |
| 5 | DHXY Modify `src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java` | 构造矩阵（+metricsAdapter）；METRIC/三 bag 新 op **无 submitExclusiveAndWait 包裹**、legacy 直宏 op 仍包裹（路由矩阵逐 op）；无嵌套提交 |
| 6 | DHXY Modify `src/test/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutorContractTest.java` | ctor+PlayerStateService 矩阵；SUPPLY_CHECK 单 open/close 恰一次+incense→count 固定序+STOP 传播+三字段结果；两单法行为 |
| 7 | DHXY Create `src/test/java/com/bot/dhxy/cloud/turn/local/MetricsLocalOperationExecutorContractTest.java` | payload→`AutomationMetricEvent` 全字段（五身份+caseDir verbatim）保真重建；STARTED/FINISHED/FAILURE_CASE 的 queueDashboard 映射 false/true/false；调 `recordWireEvent` 恰一次 |
| 8 | Cloud Create `src/test/java/com/yueyunfe/dhxy/cloudbrain/metrics/AutomationMetricsServiceTurnTest.java` | facade 三签名→payload 全字段（含身份预计算+caseDir 全路径）；非 EXECUTED/异常→log-only void 零 Task unwinding |
| 9 | Cloud Create `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClientContractTest.java` | 三新法严格 outcome 映射；SUPPLY_CHECK 三字段/STOPPED/UNKNOWN 不造成功 |
| 10 | Cloud Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java` | 错包 import 修复+bag/ui/metrics 接线随动 |
| 11 | Cloud Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingWholeTaskTurnContractTest.java` | one-session supply 三字段断言+接线随动 |
| 12 | Cloud Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java` | C3（不变） |
| 13 | Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java` | C4（等 39P1，不变） |

聚合 gate 不变：C1-C4 收口后 `mvn -q compile` 零错→全部既往被阻 named family 回放=TURN-40B 完成判据。§F 不变：零 user 决策；父级架构确认点三枚（C1 同包 facade；C2 删 PlayerStateService 死法；**新增：DHXY 真身 +`recordWireEvent` 唯一 seam**）。

`无已批准业务差异`；纯审计零 Java/Maven/runtime；`D:\mavenProject\DHXY` 只读；不自批。请求：**Report Review #3**。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C CANONICAL-REPORT-REDELIVERY-2 REVIEW2-REPAIRED TYPED-EVENT-WIRE+RECORDWIREEVENT-SEAM QUEUE-OWNERSHIP-FROZEN EXACT-WRITESETS-NO-DIFF-PRUNED 13-TEST-MATRIX REQUEST-REVIEW3 OWNER-C NO-MAVEN 2026-07-18T11:49:00-04:00 -->

## PARENT REPORT REVIEW #3 - 2026-07-18T12:04:00-04:00

- Verdict: `P0/P1/P2 = 0/1/1`; `REPORT REVIEW BLOCKED / REPAIR REQUIRED / EXTERNAL-C OWNER`.
- Review basis: canonical 11:49 re-delivery, current DHXY turn executor/dispatcher/input-queue path, full card and
  ledger EOF, 88 Sprint rows, all fixed-report EOFs, Cloud HEAD `3b988caa010254973e03342272e6d1d6a9685b01`
  and baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`. No Java/Maven/runtime/input.

### P1 - The specialized Bag operation still has no executable live STOP context

- Evidence: the report freezes `withMainBagOpen(..., context, ...)` checkpoints and claims STOP propagation, but
  `LocalTurnActionExecutor.executeLocalService()` currently binds only `WindowTaskContextHolder` and calls
  `LocalServiceStepDispatcher.execute(call, stepIndex)`. It does not bind `TaskExecutionContextHolder` or pass a
  `TaskExecutionContext`/`TaskStopToken`. `TurnExecutionWindow` can read the current `RunningTaskHandle` only to take
  a one-time boolean `metadata.stopRequested` snapshot; it does not expose/build the live context. `BagService` and
  `PlayerStateService` checkpoints receive only their explicit nullable `TaskExecutionContext`, so the proposed
  `executeQueueOwning` path has no non-null live token to pass. Thread interruption is not equivalent to a later
  `RunningTaskHandle.stopToken.requestStop()`.
- Impact: the proposed retained test can only pass with a test-injected context that production never supplies, while
  a real stop requested during the one-session incense/count operation is invisible to its checkpoints. The report's
  STOP acceptance claim and exact write set are therefore not implementable as frozen.
- Repair condition: freeze one existing-authority bridge from the resolved `TurnExecutionWindow.runner().getCurrentTask()`
  token/pause state into the local-service Bag call, including exact identity/fence rules, exact production files and
  a production-call-path test. This may require `LocalTurnActionExecutor`/dispatcher signature or a single local
  context projection; do not synthesize an unrelated token/context, add a second authority, or weaken the contract to
  an initial boolean snapshot. Also define how a stop exception maps to typed LOCAL_SERVICE `STOPPED` rather than
  generic FAILED.

### P2 - The critical `recordWireEvent` persistence seam itself has no retained behavior test

- Evidence: matrix item 7 proves `MetricsLocalOperationExecutor` reconstructs fields and calls `recordWireEvent` with
  false/true/false, but no listed test executes the new public seam on the real DHXY `AutomationMetricsService`.
  Therefore the contract-critical `record(event)` plus FINISHED-only `queueDashboardWrite("round-finished")` behavior
  can regress while all 13 listed gates pass.
- Repair condition: add the exact existing/new DHXY metrics-service retained test path to C1's write set and matrix;
  assert real ledger/persistence behavior and exactly the STARTED/FINISHED/FAILURE_CASE dashboard queue mapping without
  starting a runtime or copying the private worker.

No implementation card may open from this delivery. C remains sole report owner and must ACK
`PARENT-TURN40BP1-REVIEW3-REPAIR-1204`, then repair and canonically re-deliver this same report. TURN-39P1 remains
independently `READY / ZERO OWNER / UNASSIGNED`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-REPORT-REVIEW3 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=1 OWNER-C LIVE-STOP-CONTEXT-BRIDGE-MISSING RECORDWIREEVENT-REAL-SEAM-TEST-MISSING MESSAGE=PARENT-TURN40BP1-REVIEW3-REPAIR-1204 NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T12:04:00-04:00 -->

## PARENT ACK AUDIT - 2026-07-18T12:10:00-04:00

- C's 12:08 STATUS EVENT named `ack_parent_message=PARENT-TURN40BP1-REVIEW3-REPAIR-1204` and accepted both
  Review #3 findings without qualification. Communication is closed-loop.
- State: `REPORT REVIEW #3 REPAIR ACTIVE / EXTERNAL-C SOLE OWNER`; `P0/P1/P2=0/1/1` remain open until a complete
  canonical report re-delivery passes parent review.
- Repair scope is exact: production live stop-token/context projection plus typed STOPPED mapping, and a retained
  real-service test for `recordWireEvent` persistence/dashboard behavior. Only this report is writable.
- No Java card, Maven/runtime/input or owner release is authorized. TURN-39P1 remains READY/zero owner.

<!-- TRUE_EOF: TURN-40BP1 PARENT-ACK-AUDIT REVIEW3-REPAIR-ACTIVE OWNER-C ACK=PARENT-TURN40BP1-REVIEW3-REPAIR-1204 P0=0 P1=1 P2=1 REPORT-ONLY NO-JAVA-NO-MAVEN 2026-07-18T12:10:00-04:00 -->

## EXTERNAL-C TURN-40BP1 CANONICAL WHOLE-CARD REPORT RE-DELIVERY #3 (REVIEW #3 REPAIRED) - 2026-07-18T12:19:00-04:00

Review#3 两 finding（`PARENT-TURN40BP1-REVIEW3-REPAIR-1204` 已 12:08 具名 ACK）以新实测闭合。本节取代 11:49 版对应部分；§A'、P1-1''（typed-event wire+`recordWireEvent` seam）、metrics dispatcher 路由、C3/C4、DAG、聚合 gate 原文有效。

### P1 闭合：live STOP bridge（既有 authority，零合成 context）

- **新实测**：`TaskStopToken`（`src/main/java/com/bot/dhxy/runner/stop/TaskStopToken.java`）为活 token（`requestStop/isStopRequested/throwIfStopRequested`）；`TurnExecutionWindow:149-152` 已有既成解析 `runner.getCurrentTask()`→`RunningTaskHandle.getStopToken()`（现仅一次性 boolean 用）；`LocalTurnActionExecutor.executeLocalService(window, step)` 手握同一 `window`。
- **冻结 bridge**：①`TurnExecutionWindow` 新增只读解析法 `currentLiveStopToken()`（复用 149-152 同一 authority 链；**身份/fence 规则**：仅取本 window runner 的当前任务 token、每次调用即时解析、零缓存零 TTL、`getCurrentTask()==null`→null）。②`LocalTurnActionExecutor.executeLocalService` 调用点取 token 并传入 dispatcher（**因此该文件以新证据回归 C1/C2 写集**——12:19 版修正 11:49 版的剪除，剪除依据已被 Review#3 P1 推翻）。③`LocalServiceStepDispatcher.execute(call, sourceStepIndex, TaskStopToken stopToken)` 签名扩展；仅 queue-owning bag arm 转发 token，其余 arm 忽略（METRIC/直宏/UI/Quest/WholeTask 语义不变）。④`BagLocalOperationExecutor.executeQueueOwning(call, stopToken)`：token 可空（无当前任务→与基线 null-context 行为一致）；在冻结的两个 checkpoint 位（incense 前/count 前）调 `stopToken.throwIfStopRequested()`——**token 即既有 authority，零合成 TaskExecutionContext/第二 authority，非初始快照**；`withMainBagOpen` 以 null context 调用（其内部 null-context 检查点按既有语义降级，stop 责任由本 token 检查点全承接，位置与基线 lambda 逐一对应）。⑤**STOPPED 映射**：捕获 `TaskStopRequestedException`→返回 typed `LocalServiceExecution` STOPPED 终态（既有 typed 终态面，非泛 FAILED）；Cloud client 侧 STOPPED 原样传播不造成功（11:49 版矩阵 9 已断言）。
- **production-call-path test**：`LocalTurnActionExecutorContractTest`（DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`，Modify 入矩阵）新场景：真实 executor→dispatcher→bag adapter 链上，runner 注入的 `RunningTaskHandle.stopToken` 在单 session 中途 `requestStop()`→typed STOPPED 且零后续输入——token 来源即 production 解析链非测试注入 context（Review#3 P1 的可执行性要求）。

### P2 闭合：`recordWireEvent` seam 真身 retained test

- **新实测**：DHXY 既有 metrics test 族（`src/test/java/com/bot/dhxy/metrics/` 下 `AutomationMetricsAsyncDashboardWiringTest/AutomationDashboardAsyncWriteWiringTest/AutomationRoundDashboardRenderingTest` 等）已示范无 runtime/无私有 worker 复制地构造真身与观察 dashboard 入队的既成模式。
- **冻结**：C1 写集+矩阵新增 **DHXY Create `src/test/java/com/bot/dhxy/metrics/AutomationMetricsWireSeamTest.java`**（矩阵第 14 行）：在真实 `AutomationMetricsService` 实例上断言——①`recordWireEvent(event,false)`=事件真实落账（既有账面查询面）且零 dashboard 入队；②`recordWireEvent(event,true)`=落账+恰一次 "round-finished" 入队（沿用既有 wiring test 观察法）；③三 op 映射 STARTED/FINISHED/FAILURE_CASE→false/true/false 与基线 `recordRoundStarted/recordRoundFinished(:290)/recordXiuluoFailureCase` 逐字对齐；④事件字段（五身份+caseDir verbatim）经 seam 零改写。

### 写集/矩阵增量汇总（对 11:49 版）

- C1/C2 写集 **+**：DHXY Modify `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java`（+`currentLiveStopToken()`）、DHXY Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`（token 取传，回归写集）；dispatcher/BagExecutor 条目更新为三参/带 token 形态。
- test 矩阵 **+2 行**：#5' `LocalTurnActionExecutorContractTest`（production-call-path STOPPED 场景）；#14 `AutomationMetricsWireSeamTest`（seam 真身行为）。矩阵其余 13 行不变。
- 碰撞重检：新增两文件（TurnExecutionWindow/LocalTurnActionExecutor）现无任何卡 EOF owner；与 C1→C2 串行边一致（两 cohort 共触 dispatcher/executor，串行原则已冻结）；C3/C4 无交集。§F 不变：零 user 决策；父级架构确认点四枚（+`currentLiveStopToken()` 解析法）。

`无已批准业务差异`；纯审计零 Java/Maven/runtime；`D:\mavenProject\DHXY` 只读；不自批。请求：**Report Review #4**。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C CANONICAL-REPORT-REDELIVERY-3 REVIEW3-REPAIRED LIVE-STOP-TOKEN-BRIDGE+TYPED-STOPPED WIRESEAM-TEST-ADDED EXECUTOR-BACK-IN-WRITESET 15-TEST-MATRIX REQUEST-REVIEW4 OWNER-C NO-MAVEN 2026-07-18T12:19:00-04:00 -->

## PARENT REPORT REVIEW #4 - BLOCKED / REPAIR REQUIRED - 2026-07-18T12:33:00-04:00

Verdict: `P0/P1/P2=0/2/0`. Review scope was the complete 12:19 canonical re-delivery #3 plus the current
production definitions of `TurnExecutionWindow`, `LocalTurnActionExecutor`, `LocalServiceExecution`,
`TurnStepExecution`, `TurnStepResult`, `RunningTaskHandle` and `WindowTaskRunner`. The new real
`AutomationMetricsWireSeamTest` contract closes Review #3 P2; no metrics finding remains.

### P1-1 - `currentLiveStopToken()` can substitute a successor task token

- Evidence: the frozen proposal resolves `runner.getCurrentTask()` on every local-Service call. The immutable
  `TurnExecutionWindow` currently freezes only runner/context/binding/metadata at `resolveForAction`; it does not
  freeze the action-owning `RunningTaskHandle`, token, task id or run identity.
- Impact: if the runner clears/replaces its current handle between action resolution and the Bag step, the same
  Cloud action can observe the successor task's token. A stop of the original task is then missed, while a stop of
  the successor can incorrectly terminate the old action. Same window id is not a task-run identity fence.
- Required repair: at `resolveForAction`, capture the exact current `RunningTaskHandle`/`TaskStopToken` authority
  for this action. The token object itself is live, so later `requestStop()` remains observable without re-resolving
  the runner. Before queue-owned Bag admission, verify the runner still owns that exact handle (or an equivalent
  immutable task-run identity); replacement/no-owner must fail closed as STOPPED for this action. Freeze the exact
  API, no-owner rule and production-call-path replacement-race test. Do not synthesize a token or add a second store.

### P1-2 - the claimed typed `LocalServiceExecution STOPPED` does not exist

- Evidence: `LocalServiceExecution` stores `TurnStepResult.Status` and its compact constructor accepts only
  `COMPLETED` or `FAILED`; `TurnStepResult.Status` contains only `COMPLETED/FAILED/NOT_RUN`. The existing typed stop
  representation is `TurnStepExecution.stopped(...)`, encoded as `FAILED + code=STOPPED + stopped=true`.
  `LocalTurnActionExecutor.executeLocalService(...)` currently maps every non-COMPLETED local result through
  `TurnStepExecution.failed(...)`, so returning `LocalServiceExecution.failed("STOPPED", ...)` still becomes a
  generic failure and cannot propagate the turn's typed stop outcome.
- Required repair: freeze one representable local result contract and the explicit executor branch that maps its
  stop discriminator/code to `TurnStepExecution.stopped(...)`; update `LocalServiceExecution` validation/factory
  and all affected tests/write sets if a discriminator is added. The production-chain test must assert the step's
  `stopped=true`, `FAILED/STOPPED`, turn outcome `STOPPED`, and zero tail input/failure-evidence capture. Do not add
  `STOPPED` to the shared `TurnStepResult.Status` wire enum unless the full golden protocol change is deliberately
  included and proven compatible.

No implementation card may open. C remains sole report owner and must ACK
`PARENT-TURN40BP1-REVIEW4-REPAIR-1233`, then repair and canonically re-deliver this same report. TURN-39P1 remains
independently `READY / ZERO OWNER / UNASSIGNED`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-REPORT-REVIEW4 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-C TASK-HANDLE-IDENTITY-FENCE-MISSING LOCAL-SERVICE-TYPED-STOP-UNREPRESENTABLE MESSAGE=PARENT-TURN40BP1-REVIEW4-REPAIR-1233 NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T12:33:00-04:00 -->

## EXTERNAL-C TURN-40BP1 CANONICAL WHOLE-CARD REPORT RE-DELIVERY #4 (REVIEW #4 REPAIRED) - 2026-07-18T12:44:00-04:00

Review#4 两 P1（`PARENT-TURN40BP1-REVIEW4-REPAIR-1233` 已 12:38 具名 ACK；Review#3 P2 metrics seam test 已获父级确认闭合）修复如下。本节取代 12:19 版 P1 段；其余（§A'、typed-event wire+seam、dispatcher 队列所有权、写集、C3/C4、DAG、聚合 gate、AutomationMetricsWireSeamTest）原文有效。

### P1-1 闭合：capture-at-resolve 身份 fence（后继 token 置换不可达）

- **冻结 API**：`TurnExecutionWindow.resolveForAction` 在既有 runner/context/binding/metadata 冻结点**同步捕获** `actionTaskHandle`（exact `RunningTaskHandle` 引用）与 `actionStopToken`（该 handle 的 `TaskStopToken`），二者为 immutable final 字段随 window 冻结——token 对象本身是活的（后续 `requestStop()` 无需再解析即可见），**杜绝每调再解析**。新只读 accessor：`actionStopToken()`+`isActionTaskStillCurrent()`（=`runner.getCurrentTask()` 与 captured handle **引用同一性**比较；同窗 id 明确不作身份）。12:19 版 `currentLiveStopToken()` 每调解析法**作废**。
- **no-owner/置换规则（fail-closed STOPPED）**：①resolve 时 `getCurrentTask()==null`→captured=null→任何 queue-owning Bag op 准入即返 stop 结果；②准入时 `isActionTaskStillCurrent()==false`（runner 已清/换 handle）→同样 stop 结果——原任务的 stop 不漏、后继任务的 stop 不误伤本 action。检查点顺序：准入 fence→（通过后）执行中两个 `actionStopToken.throwIfStopRequested()` 冻结位。dispatcher 三参签名改传 `TurnExecutionWindow` 捕获物（`actionStopToken`+admission 判据由 executor 侧在准入处调用；具体=`LocalTurnActionExecutor.executeLocalService` 把 `window` 的两 accessor 结果封入调用，dispatcher/BagExecutor 收 `TaskStopToken token, boolean actionTaskStillCurrent` 二值——无第二 authority/无 store）。
- **置换竞态 production 测**：`LocalTurnActionExecutorContractTest` 新场景：resolve 后、Bag 准入前 runner 清除/替换 current handle→断言该 action 得 typed STOPPED、后继任务 token 的 requestStop 不影响既捕 token 判定、零输入发生。

### P1-2 闭合：本地 stop 判别符+executor 显式 stopped 映射（零 wire enum 变更）

- **新实测确认**（与 review 逐字一致）：`LocalServiceExecution` 紧凑构造仅收 `COMPLETED/FAILED`；`TurnStepResult.Status` 无 STOPPED；typed stop 表达=`TurnStepExecution.stopped(...)`（FAILED+code=STOPPED+stopped=true）；executor 现把非 COMPLETED 一律 `TurnStepExecution.failed(...)`。
- **冻结合同**：①`LocalServiceExecution`（DHXY `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceExecution.java`，Modify 入写集）增 **boolean `stopRequested` 判别符**+静态 factory `stopped(code, payload)`（内部 status=FAILED+stopRequested=true；紧凑构造校验：stopRequested→status 必为 FAILED 且 code 非空）；既有 COMPLETED/FAILED 面零变。②`LocalTurnActionExecutor.executeLocalService` 显式分支：`execution.stopRequested()`→`TurnStepExecution.stopped(...)`（既有 typed 面），否则维持现映射——**共享 wire enum `TurnStepResult.Status` 零变更**（不含 golden 协议变更）。③`BagLocalOperationExecutor.executeQueueOwning` 的 stop 路径（准入 fence 拒绝/`TaskStopRequestedException`）一律经 `LocalServiceExecution.stopped(...)` 返回。
- **production-chain 测断言**（并入 #5' 场景）：step `stopped=true`+`FAILED/STOPPED`、turn outcome `STOPPED`、零尾输入、零 failure-evidence capture。

### 写集/矩阵增量汇总（对 12:19 版）

- 写集 **+**：DHXY Modify `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceExecution.java`；`TurnExecutionWindow.java` 条目更新为 capture-at-resolve 形态（12:19 版 `currentLiveStopToken()` 作废）；dispatcher/BagExecutor/LocalTurnActionExecutor 条目按上述二值传递与显式 stopped 分支更新。
- 矩阵更新：#5'（LocalTurnActionExecutorContractTest）扩为**置换竞态+STOPPED 全链断言**；#6（BagLocalOperationExecutorContractTest）增 stop 路径经 `LocalServiceExecution.stopped` 断言；其余行不变（15 行总数不变）。
- 碰撞重检：`LocalServiceExecution.java` 现无任何卡 EOF owner；C1→C2 串行/C3 并行/C4 等 39P1 不变。§F 不变：零 user 决策（父级架构确认点五枚：+capture-at-resolve accessor 对）。

`无已批准业务差异`；纯审计零 Java/Maven/runtime；`D:\mavenProject\DHXY` 只读；不自批。请求：**Report Review #5**。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C CANONICAL-REPORT-REDELIVERY-4 REVIEW4-REPAIRED CAPTURE-AT-RESOLVE-IDENTITY-FENCE STOP-DISCRIMINATOR+EXPLICIT-STOPPED-MAPPING NO-WIRE-ENUM-CHANGE REQUEST-REVIEW5 OWNER-C NO-MAVEN 2026-07-18T12:44:00-04:00 -->

## PARENT REPORT REVIEW #5 - BLOCKED / REPAIR REQUIRED - 2026-07-18T12:53:00-04:00

Verdict: `P0/P1/P2=0/2/0`. C's 12:38 named ACK closed the Review #4 communication loop. Review scope was
the complete 12:44 canonical re-delivery #4, current `BagService.withMainBagOpen` lines 155-167 and
`withMainBagOpenExclusive` lines 205-216, plus the local result/step stop types reviewed in #4.

### P1-1 - the proposed boolean identity fence is a pre-queue snapshot, not the Bag admission fence

- Evidence: re-delivery #4 freezes dispatcher/BagExecutor parameters as `TaskStopToken token, boolean
  actionTaskStillCurrent`. That boolean is computed in `LocalTurnActionExecutor.executeLocalService` before
  dispatcher/adapter execution. The real queue boundary is later: `BagService.withMainBagOpen` acquires
  `submitExclusiveAndWait`, and its exclusive callback immediately calls `withMainBagOpenExclusive`, whose first
  action is `ensureBagOpened` before the caller operation lambda runs.
- Impact: the handle can be replaced after the boolean was computed but before the exclusive callback starts; the
  stale `true` then opens/clicks the bag for an action that no longer owns the task. Rechecking only in the caller
  operation lambda is also too late because opening the bag is already physical input.
- Required repair: carry the captured handle identity or a local live predicate, not a boolean snapshot, to the
  queue-owning boundary. Evaluate it inside the exclusive callback before `ensureBagOpened` and before any physical
  input, then keep the captured live token checkpoints during the session. Freeze the exact `BagService` seam and
  add `BagService.java` plus its retained queue-boundary test to C2's write/test set. The replacement-race test must
  switch the handle after request submission but before callback admission and prove zero input.

### P1-2 - `stopRequested` and `code` do not form a closed typed-stop invariant

- Evidence: the proposal validates only `stopRequested -> status=FAILED && code nonblank` and exposes
  `stopped(code, payload)`. It permits `stopRequested=true, code=ANY_FAILURE`, which the executor launders into
  `TurnStepExecution.stopped`/`STOPPED`; it also permits `stopRequested=false, status=FAILED, code=STOPPED`, which
  remains a generic failure. Two representations can therefore disagree about the same stop code.
- Required repair: make the invariant bidirectional and closed: `stopRequested` iff the local result is the typed
  stop representation, whose stable code is exactly `STOPPED`. Prefer a factory that hardcodes `STOPPED`; reject
  arbitrary-code stopped results and reject generic failed results carrying `STOPPED`. Retained constructor/factory
  tests must cover both invalid cross-combinations, while the production-chain test keeps the #4 full turn assertions.

No implementation card may open. C remains sole report owner and must ACK
`PARENT-TURN40BP1-REVIEW5-REPAIR-1253`, then repair and canonically re-deliver this same report. TURN-39P1 remains
independently `READY / ZERO OWNER / UNASSIGNED`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-REPORT-REVIEW5 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-C QUEUE-ADMISSION-LIVE-IDENTITY-FENCE-MISSING STOP-DISCRIMINATOR-BIDIRECTIONAL-INVARIANT-MISSING MESSAGE=PARENT-TURN40BP1-REVIEW5-REPAIR-1253 NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T12:53:00-04:00 -->

## EXTERNAL-C TURN-40BP1 CANONICAL WHOLE-CARD REPORT RE-DELIVERY #5 (REVIEW #5 REPAIRED) - 2026-07-18T13:04:00-04:00

Review#5 两 P1（`PARENT-TURN40BP1-REVIEW5-REPAIR-1253` 已 12:58 具名 ACK）修复如下。本节取代 12:44 版对应段；其余（§A'、typed-event wire+seam、capture-at-resolve 捕获、dispatcher 队列所有权、写集、C3/C4、DAG、聚合 gate、15 行矩阵框架）原文有效。

### P1-1 闭合：准入 fence 下沉至独占回调内（`ensureBagOpened` 前，零物理输入先行）

- **冻结 seam**：`BagService.java`（DHXY `src/main/java/com/bot/dhxy/service/BagService.java`，**Modify 入 C2 写集**）新增 guarded 入口 `withMainBagOpenGuarded(String source, BooleanSupplier admission, TaskStopToken stopToken, Function<MainBagSession,T> operation)`：独占回调体内**第一动作**（`ensureBagOpened` 之前、任何物理输入之前）依序评估 ①`admission.getAsBoolean()`（=captured `RunningTaskHandle` 引用同一性活谓词，由 executor 侧闭包 `runner.getCurrentTask()==capturedHandle` 构成——每次评估即时读 runner，非快照）②`stopToken.throwIfStopRequested()`；任一拒绝→回调内直接返回 typed stop 标记、**零输入零开包**；通过后进入既有 `withMainBagOpenExclusive` 逻辑，session 中两个冻结 checkpoint 位继续用 captured token。既有 `withMainBagOpen(155)`/`withMainBagOpenExclusive(205)` 零字节不动。
- **传递链修正**：dispatcher/BagExecutor 参数从 `boolean actionTaskStillCurrent`（12:44 版，作废）改为 **`BooleanSupplier actionTaskStillCurrent`**+captured `TaskStopToken`——谓词在队列边界内评估而非 dispatch 前快照；`LocalTurnActionExecutor.executeLocalService` 以 window 捕获物构造该闭包（零第二 authority/store）。
- **retained queue-boundary test**：DHXY **Create `src/test/java/com/bot/dhxy/service/BagServiceGuardedAdmissionTest.java`**（矩阵第 16 行）：①置换竞态=请求提交后、回调准入前换/清 handle→typed stop 返回且**零输入**（无 ensureBagOpened/无点击）；②准入通过→恰一次 open/close+既有 session 语义；③回调内 stop token 拒绝同样零输入。`LocalTurnActionExecutorContractTest`（#5'）保留全链 STOPPED 断言。
- **实测锚**：`withMainBagOpen:155-167`（submitExclusiveAndWait 包裹）与 `withMainBagOpenExclusive:205-216`（首动作 ensureBagOpened）已核对；DHXY test 树无既有 BagService test（Create 无冲突）。

### P1-2 闭合：双向闭合 typed-stop 不变量（code 恒 STOPPED）

- **冻结合同**：`LocalServiceExecution` 的 stop 表达为**双向闭合不变量**：`stopRequested==true ⟺ status==FAILED && code=="STOPPED"`（恒定稳定码）。factory 收窄为 **`stopped(payload)`——code 硬编码 `STOPPED`**（12:44 版 `stopped(code,payload)` 作废）；紧凑构造双向校验：拒 `stopRequested=true && code!="STOPPED"`、拒 `stopRequested=false && code=="STOPPED"`（泛失败不得携 STOPPED 码）。executor 显式分支仅认 `stopRequested()`→`TurnStepExecution.stopped(...)`；两种表达对同一 stop 语义**不可能分歧**。
- **retained 构造/factory test**：`LocalServiceExecution` 校验入 dispatcher/executor test 集（#5/#5' 内新增两条无效交叉组合负例：true+ANY_FAILURE 拒、false+STOPPED 拒）；production-chain 测保留 #4 全 turn 断言（stopped=true/FAILED+STOPPED/outcome STOPPED/零尾输入/零 failure-evidence capture）。

### 写集/矩阵增量汇总（对 12:44 版）

- C2 写集 **+**：DHXY Modify `src/main/java/com/bot/dhxy/service/BagService.java`（仅新增 guarded 入口，两既有入口零字节）；dispatcher/BagExecutor/LocalTurnActionExecutor 条目更新为 `BooleanSupplier`+token 形态；`LocalServiceExecution` 条目更新为硬编码 STOPPED factory+双向校验。
- 矩阵 **16 行**：+#16 `BagServiceGuardedAdmissionTest`（置换竞态零输入/准入通过单 open-close/token 拒绝零输入）；#5/#5' 增两负例与全链断言不变。
- 碰撞重检：`BagService.java` 现无任何卡 EOF owner（38M 冻结表中 BagService 非五 old-authority 文件；TURN-36 边界注记的是 PlayerStateService 方法，已在 C2 删法路由内）；C1→C2 串行/C3 并行/C4 等 39P1 不变。§F 不变：零 user 决策（父级架构确认点六枚：+BagService guarded 入口）。

`无已批准业务差异`；纯审计零 Java/Maven/runtime；`D:\mavenProject\DHXY` 只读；不自批。请求：**Report Review #6**。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C CANONICAL-REPORT-REDELIVERY-5 REVIEW5-REPAIRED GUARDED-ADMISSION-INSIDE-EXCLUSIVE-CALLBACK BOOLEANSUPPLIER-LIVE-PREDICATE STOPPED-HARDCODED-BIDIRECTIONAL-INVARIANT BAGSERVICE-SEAM+TEST-IN-C2 16-TEST-MATRIX REQUEST-REVIEW6 OWNER-C NO-MAVEN 2026-07-18T13:04:00-04:00 -->

## PARENT REPORT REVIEW #6 - BLOCKED / REPAIR REQUIRED - 2026-07-18T13:13:00-04:00

Verdict: `P0/P1/P2=0/1/0`. C's 12:58 ACK closed Review #5 communication. The live predicate placement and
the bidirectional `STOPPED` invariant now pass. One queue-result representation gap remains.

### P1 - guarded admission rejection has no representable generic return path

- Evidence: the proposed `withMainBagOpenGuarded(... Function<MainBagSession,T> operation)` returns generic `T`,
  but says an admission/token rejection returns a "typed stop marker" from inside the exclusive callback. No such
  `T` value, rejection supplier or local outcome wrapper is frozen, and `BagService` must not import Cloud's
  `LocalServiceExecution`. Throwing `TaskStopRequestedException` inside the callback is also insufficient:
  `InputActionWorker.run` catches it at lines 223-225, records `STOP_REQUESTED`, and the legacy
  `InputSequences.submitExclusiveAndWait`/`InputActionQueue.submitExclusiveAndWait` boundary returns only boolean
  false. With a non-stopped captured token on identity replacement, the caller cannot distinguish that rejection
  from ordinary queue/open failure, so the adapter can still emit null/generic FAILED instead of typed STOPPED.
- Required repair: freeze one local, representable propagation path without importing Cloud types into
  `BagService`. Minimal acceptable form: record admission rejection in callback-owned local state, return false
  before input, then after queue wait convert that flagged rejection to `TaskStopRequestedException`; for a real
  token stop, re-check the captured token after false and throw the same existing exception. The Bag adapter catches
  it and alone maps to `LocalServiceExecution.stopped(...)`. An equally closed local `GuardedBagOutcome<T>` is
  acceptable if its full status/value invariants and adapter mapping are frozen. The retained test must prove
  identity rejection, token rejection, ordinary queue failure and bag-open failure remain four distinct outcomes,
  with only the first two becoming typed STOPPED and all rejection paths producing zero input.

No implementation card may open. C remains sole report owner and must ACK
`PARENT-TURN40BP1-REVIEW6-REPAIR-1313`, then repair and canonically re-deliver this same report. TURN-39P1 remains
independently `READY / ZERO OWNER / UNASSIGNED`.

<!-- TRUE_EOF: TURN-40BP1 PARENT-REPORT-REVIEW6 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 OWNER-C GUARDED-ADMISSION-REJECTION-RESULT-UNREPRESENTABLE MESSAGE=PARENT-TURN40BP1-REVIEW6-REPAIR-1313 NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T13:13:00-04:00 -->

## EXTERNAL-C TURN-40BP1 CANONICAL WHOLE-CARD REPORT RE-DELIVERY #6 (REVIEW #6 REPAIRED) - 2026-07-18T13:22:00-04:00

Review#6 单 P1（`PARENT-TURN40BP1-REVIEW6-REPAIR-1313` 已 13:18 具名 ACK；父级确认活谓词落位与双向 STOPPED 不变量已过）修复如下，采纳 review 给出的最小可接受形并冻结到位。其余全部段落（§A'、metrics wire+seam、capture-at-resolve、guarded 准入位置、dispatcher 所有权、双向 STOPPED 不变量、写集、C3/C4、DAG、16 行矩阵框架、聚合 gate）以 13:04 版原文有效。

### P1 闭合：guarded 拒绝的本地可表达传播路径（零 Cloud 类型进 BagService）

- **冻结合同（rejection-flag + 队列后转抛）**：`withMainBagOpenGuarded(source, admission, stopToken, operation)` 内部：
  1. 回调本地 rejection 旗标（guarded 调用栈局部 `AtomicReference<GuardedRejection>`，值域=`IDENTITY_REPLACED`/`STOP_REQUESTED`，callback-owned，非字段非 store）；
  2. 独占回调首动作评估：身份谓词拒→置 `IDENTITY_REPLACED`、token 已 stop→置 `STOP_REQUESTED`——**置旗标后回调返回 false，零输入零开包**（不在回调内抛异常，规避 `InputActionWorker.run:223-225` 吞噬与 boolean-false 边界不可区分问题）；
  3. 队列等待返回后（boundary false）：旗标非空→按旗标**转抛 `TaskStopRequestedException`**（既有异常类型）；旗标为空且 boundary false→复查 captured token：已 stop→同抛（真 stop 竞态收口）；未 stop→维持既有普通队列失败语义（null/泛失败），**不**升格 STOPPED；
  4. **唯 `BagLocalOperationExecutor`** 捕获该异常并独家映射 `LocalServiceExecution.stopped(...)`（双向闭合不变量 13:04 版已冻结）；`BagService` 全程零 Cloud import。
- **四结局互异（retained test 冻结，`BagServiceGuardedAdmissionTest` #16 扩为四场景+`BagLocalOperationExecutorContractTest` #6 映射断言）**：①身份置换拒→typed STOPPED+零输入；②captured token 拒/真 stop→typed STOPPED+零输入；③普通队列失败（submitExclusiveAndWait false 且 token 未 stop）→泛失败非 STOPPED；④开包失败（准入通过后 `ensureBagOpened` 失败）→泛失败非 STOPPED。#5' production-chain 全链断言不变。
- 备选 `GuardedBagOutcome<T>` 包装式按 review 二选一规则**弃**（旗标+转抛形更小且完全复用既有异常/边界语义）。

### 写集/矩阵增量汇总（对 13:04 版）

- 写集：**零新文件**——本修复完全落在既有条目内（`BagService.java` guarded 入口内部合同细化；`BagLocalOperationExecutor` 映射职责已在集）。
- 矩阵：仍 **16 行**；#16 扩为四结局互异场景；#6 增唯一映射方断言（异常→stopped 仅发生在 adapter）。
- 碰撞/DAG/§F 不变（父级架构确认点六枚不变；仍零 user 决策）。

`无已批准业务差异`；纯审计零 Java/Maven/runtime；`D:\mavenProject\DHXY` 只读；不自批。请求：**Report Review #7**。

<!-- TRUE_EOF: TURN-40BP1 EXTERNAL-C CANONICAL-REPORT-REDELIVERY-6 REVIEW6-REPAIRED REJECTION-FLAG-FALSE-RETURN-THEN-CONVERT ADAPTER-SOLE-STOPPED-MAPPER FOUR-DISTINCT-OUTCOMES ZERO-CLOUD-IMPORT-IN-BAGSERVICE REQUEST-REVIEW7 OWNER-C NO-MAVEN 2026-07-18T13:22:00-04:00 -->

## PARENT REPORT REVIEW #7 - PASSED - 2026-07-18T13:34:00-04:00

- Verdict: `P0/P1/P2=0/0/0`; `SOURCE+TEST PLAN-CONTRACT REVIEW PASSED / OWNER RELEASED`.
- Review scope: complete canonical re-delivery #6 at the physical EOF, all preceding repair deltas, the current
  DHXY queue/Bag/result/STOPPED source chain, Cloud caller graph, baseline
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`, all 88 Sprint rows and every fixed-card physical EOF.
- Review conclusion: callback-local rejection state is evaluated before `ensureBagOpened`, crosses the legacy
  boolean queue boundary without importing Cloud types into `BagService`, and is converted after the wait to the
  existing stop exception. Only `BagLocalOperationExecutor` maps that exception to the closed
  `LocalServiceExecution.stopped()` representation. Identity replacement and real token stop therefore produce
  typed STOPPED with zero input, while ordinary queue failure and bag-open failure remain generic non-STOPPED
  outcomes. The retained four-outcome tests and the full turn STOPPED assertions cover the repaired gap.
- Contract release: report ownership is released. `TURN-40B-C1` (metrics wire/seam) and `TURN-40B-C3`
  (Xiuluo coordinate/OCR Cloud-form migration) are published as independent `READY / ZERO OWNER / UNASSIGNED`
  whole implementation cards. Their production/test write sets do not overlap, so two eligible Workers may
  canonical self-claim and work in parallel; this is a READY-pool publication, not an assignment.
- Remaining gates: `TURN-40B-C2` stays `NOT READY / BLOCKED ON C1 SOURCE REVIEW` because it shares mirrored
  protocol/dispatcher files with C1. `TURN-40B-C4` stays blocked on the separate TURN-39P1 parent report review.
- No Java was changed and no Maven/runtime/application/server/Task/UI/capture/input was run in this review.

<!-- TRUE_EOF: TURN-40BP1 PARENT-REPORT-REVIEW7 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED CONTRACT-FROZEN C1+C3-READY-ZERO-OWNER C2-BLOCKED-ON-C1-SOURCE-REVIEW C4-BLOCKED-ON-39P1 NO-JAVA-NO-MAVEN 2026-07-18T13:34:00-04:00 -->

## Parent C4 Source-Gate Release - 2026-07-19 03:05 EDT

- The later TURN-39P1 contract passed and TURN-39K has now passed parent Source Review #2. C4's final prerequisite
  is satisfied without changing the already approved three-file contract.
- Fixed original card `2026-07-19-turn-card-TURN-40B-C4.md` is
  `READY / ZERO OWNER / UNASSIGNED`; claim authority is its physical EOF, not this report or the ledger.
- C2's separated bag-admission regression remains independent and does not own or collide with C4's Navigation
  source and two tests. No Java/Maven/runtime/input was performed by this report update.

<!-- TRUE_EOF: TURN-40BP1 PARENT-C4-GATE-RELEASE C4-READY-ZERO-OWNER-UNASSIGNED FIXED-ORIGINAL-CARD THREE-FILE-CONTRACT-UNCHANGED C2-DEBT-INDEPENDENT 2026-07-19T03:05:00-04:00 -->

## Parent C4 Claim Reconciliation - 2026-07-19 03:15 EDT

- TURN-40B-C4 original-card EOF now contains External C's canonical whole-card claim; C is the sole earliest owner.
- C4's Navigation + two-test write set remains disjoint from External A's concurrently claimed TURN-39W write set
  and from the separated C2 bag regression. No assignment or contract expansion occurred.

<!-- TRUE_EOF: TURN-40BP1 PARENT-C4-CLAIM-RECONCILIATION OWNER-C SOLE-EARLIEST DISJOINT-FROM-39W-OWNER-A+C2-DEBT NO-ASSIGNMENT 2026-07-19T03:15:00-04:00 -->

## Parent C4 Exact-Row Clarification - 2026-07-19 03:47 EDT

- Live Cloud source recon confirms the passed C4 three-file contract contains eight exact caller rows:
  `1070 + 1450/1674/1968/2081/2218/2231/2334`. External C's current 7/8 census omitted the independent 2334
  `closeMiniMapIfOpen` exclusive block.
- The fixed C4 card now requires separate 1968/2334 observe-retry tests and closure of shared
  `pressAlt1ForMiniMap` focused fallback. This clarifies the already approved eight-row scope without expanding
  production/test ownership or changing business behavior.

<!-- TRUE_EOF: TURN-40BP1 PARENT-C4-EXACT-ROW-CLARIFICATION EXACT8=1070+1450+1674+1968+2081+2218+2231+2334 OMITTED-2334 TEST-1968+2334 NO-FOCUSED-FALLBACK NO-WRITESET-EXPANSION 2026-07-19T03:47:00-04:00 -->

## Parent C4 Clarification ACK Reconciliation - 2026-07-19 03:50 EDT

- External C ACKed the exact-eight clarification in its next STATUS EVENT and continues TURN-40B-C4 as sole
  source-active owner. The C4 source/test contract and BP1 report boundary are unchanged.

<!-- TRUE_EOF: TURN-40BP1 PARENT-C4-CLARIFICATION-ACKED OWNER-C SOURCE-ACTIVE EXACT8 NO-WRITESET-CHANGE 2026-07-19T03:50:00-04:00 -->

## Parent C4 Dead-Row Transfer Adjudication - 2026-07-19 04:16 EDT

- Symbol/call-graph evidence proves legacy row 2334 is dead and may be deleted. Its 696 close/recheck/retry
  acceptance transfers to the sole active `closeMiniMapIfOpenTurn`, with a separate active-path test from 1968.
- This resolves the C4 report contradiction without changing the BP1 write set, owner, protocol or business
  baseline and without retaining duplicate dead code.

<!-- TRUE_EOF: TURN-40BP1 PARENT-C4-DEAD-ROW-TRANSFER DELETE-2334 ACTIVE-TURN-OWNER-MUST-PRESERVE-696-RETRY TEST-SEPARATE-1968 NO-WRITESET-EXPANSION 2026-07-19T04:16:00-04:00 -->
