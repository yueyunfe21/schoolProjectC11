# TURN-29 - TaskTrackerPanelService core HTTPS turn cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15 21:16 EDT

- 状态：`READY`；类型：`INTEGRATION`；`countDelta=0`；startDependsOn：`TURN-02R`、`TURN-13C`
  source/test-source review passed。三个真实 Task caller 严格留在 TURN-30/31/32，本卡不得顺手修改 Task 或登记
  caller 覆盖。
- Worker 是 implementation Worker，不是 reviewer；父级是唯一 manager/final reviewer。
- Exact production write set（Cloud，共十个文件）：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java`
  - `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerTitleTemplate.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelSourceType.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelReadResult.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelPrepareResult.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelNegativeResult.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelCacheEntry.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerGreenLink.java`
  - `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerFastMatchResult.java`
- Exact test/report write set：
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskTrackerPanelTurnContractTest.java`
  - 本固定报告。
- 其余两仓文件全部只读。不得创建第十一个 production Java 文件，也不得改协议、Server/routes、DHXY、POM、
  caller/Task 或其它卡报告。
- 保留全部现有 public API、模型字段/坐标空间、task-specific title/green-link 规则、同帧 panel/detail 语义、
  fingerprint/cache、prepare/negative/final-consumed 状态与排序/selected-link 结果。所有业务算法留在 Cloud；
  exact-window capture 和物理 input 只能经现有 `TurnGameClient` 的 closed HTTPS turn action。不得再引用
  DHXY-only `GameClientTracker`、`InputSequences`、本地 OCR/temp path，或旧 `READ/MATERIALIZE` fact/macro 分支。
- 每次显式 client 调用只生成一个 UUID/一个 command；只消费 exact action/device/window/step 和同一 raw PNG。
  COMPLETED 之外、错 identity/step/frame、缺图/坏 PNG、STOPPED/uncertain 必须 typed fail-closed；不得折叠为
  success/miss，不得自动 retry、第二 exchange、旧 fact/local mechanics fallback、owner/session/ledger/TTL。
- 交付后 production 对 `GameClientTracker`、`InputSequences`、`TextRecognizer`、`OcrWindowScanService`、
  `WindowFact`/`readWindowFact`、`executeInputBundle`、旧 READ/MATERIALIZE/final-consumption transport 必须零引用；
  replay/reference API 只有在不接触本地 runtime/mechanics 且保持旧 public surface 时才可保留。
- Named test `TaskTrackerPanelTurnContractTest` 至少覆盖：
  1. exact raw PNG/absolute window origin 与 anchor/panel rectangle；
  2. 五环/五倍/修罗 title、黄字/绿字分段、排序和 selected link；
  3. capture/read/prepare/materialize/final-consumed 与 negative result；
  4. fingerprint cache hit/miss、窗口/round/source mismatch 与既有快速链失效规则；
  5. COMPLETED/FAILED/STOPPED/uncertain、错 action/window/step/frame/坏 PNG，且每次 client 调用单 UUID/command、
     任一异常均无自动 retry。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行 Git mutation；父级在
  全部 Java writers 稳定后运行 named test 与适用 Cloud compile cohort。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-29 parent frozen brief -->

## CLAIMED - 2026-07-15T21:20:07-04:00

- Worker：Galileo `019f6880-b69e-77a1-9fbe-ce084910ae99`；角色仅为 TURN-29 implementation Worker，
  不是 reviewer；父级是唯一 manager/final reviewer。
- Exact production write set（仅 Cloud 十文件）：
  1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java`
  2. `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
  3. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerTitleTemplate.java`
  4. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelSourceType.java`
  5. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelReadResult.java`
  6. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelPrepareResult.java`
  7. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelNegativeResult.java`
  8. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelCacheEntry.java`
  9. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerGreenLink.java`
  10. `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerFastMatchResult.java`
- Exact test/report write set：
  1. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskTrackerPanelTurnContractTest.java`
  2. `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-29.md`
- 其余两仓文件全部只读；不回滚、覆盖、清理或格式化任何其它 dirty/untracked，不创建第十一个 production
  Java 文件，不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不做 Git mutation。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。

<!-- TRUE_EOF: TURN-29 CLAIMED -->

## SOURCE+TEST SOURCE DELIVERED - 2026-07-15T22:20:39-04:00

- Worker：Galileo `019f6880-b69e-77a1-9fbe-ce084910ae99`；仅交付 implementation/test source，
  不是 reviewer；等待父级唯一 manager/final reviewer 独立审查。
- Exact production write set 保持为冻结的 Cloud 十文件；未创建第十一个 production Java 文件，
  未修改 caller/Task、DHXY、协议、Server/routes、POM 或其它卡报告。
- 逐文件 SHA-256：
  1. `3138F1372D80CC7EEE064B7B1EF9F4A295B970EA0B7A4BD837CF73B27F19DC48`  
     `src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java`
  2. `C2C48ACCC94F8C5096DC7C0D2B18AFB049E72EA0C98DA339A6CCA13C6B7CC4E7`  
     `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
  3. `124480B731CEADE9A1BC63EC022DDFD1CFA6E4A600A7CCFCBE8DF57C9BD57150`  
     `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerTitleTemplate.java`
  4. `41C52C33351E63AFF66FB407C5698767FA6C133C5D5B3A532ED994676E175D1A`  
     `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelSourceType.java`
  5. `AB827E574D4952D781121A87AB76E7E637BF5CC14DE6DB5751AA154870B0D0D0`  
     `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelReadResult.java`
  6. `919D47BE380812BA27A08475B2A759A0ACA6D81B0A69354CD00AE593DBE8AED0`  
     `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelPrepareResult.java`
  7. `39584C9A76B2271D651CCC17571D95FCB473620FE796819612AFAA6FB3AAE50A`  
     `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelNegativeResult.java`
  8. `F8EDC7CCAA39553F5BE202AE55E28BAD4EDCA26FAC2C8F9E56BCE956B9F1A465`  
     `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerPanelCacheEntry.java`
  9. `C5A1147FB4614B95005FC941FC88E90216EDC08910DA6E2700D6011D451AA5FA`  
     `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerGreenLink.java`
  10. `B367A8196125041DA875604862CCAEE49449FFA1D0B74EE1C5CA325559C151FA`  
      `src/main/java/com/bot/dhxy/model/tasktracker/TaskTrackerFastMatchResult.java`
  11. `9E965C2F211309A5D5AE1B9F4930D5EB86FE94E01B486F807C8342973B971BF1`  
      `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskTrackerPanelTurnContractTest.java`
- 静态交付证据：
  - Cloud 算法接收同一 raw PNG，在内存中闭合 anchor、panel/detail rectangle、五环/五倍/修罗 title、
    黄/绿字处理、排序/selected link、五倍 map OCR、fingerprint/cache/prepare/negative/final result；
    保留 `696a12b0` 的 public API、绝对坐标和三任务规则。
  - live facade 的 mechanics 只有同一条件分支内二选一的 `TurnGameClient.capture(...)` 或
    `TurnGameClient.execute(...)`；每次显式调用只进入一个 existing client command。prepare 消费同帧结果，
    不发第二条 command；unsafe anchor 仅在下一次显式调用组成单个 `DRAG+WAIT+CAPTURE` closed action。
  - 对 command/outcome `COMPLETED`、exact outcome window、step index/type/status、raw PNG purpose、
    content-type、capture region、source-step、SHA-256、像素尺寸和 PNG 解码均作 typed fail-closed 校验；
    FAILED/STOPPED/uncertain/错 identity/step/frame/坏 PNG 不折叠为 success/miss。
  - 十个 production 文件对 `GameClientTracker`、`InputSequences`、`TextRecognizer`、
    `OcrWindowScanService`、`WindowFact`、`readWindowFact`、`executeInputBundle`、旧 `READ`/
    `MATERIALIZE` transport 的最终区分大小写扫描均为零命中。
  - 十个 production 文件对新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/
    auto retry/second exchange 的最终扫描为零命中。模型中旧 public `freshWithin(...)` 仅为 API 兼容保留；
    live service 对 `freshWithin`/`verifiedWithin`/expiry/max-age 零引用，不创建年龄策略。
  - Named test 固定六组：三任务纯 Cloud 几何/规则；capture-read-prepare-materialize-final 同帧；
    negative 与 device/window/round/source cache scope；fast hit/miss 与 round mismatch；下一显式调用的
    closed reposition；terminal/identity/step/frame/PNG 失败矩阵及每调用单 UUID/command、无 retry。
- 未运行门：严格按本卡冻结 brief，未运行 Maven、JUnit、compile、runtime、application、server、Task、
  UI、capture 或 input；这些门留给全部 Java writers 稳定后的父级 cohort。未执行任何 Git mutation。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。

<!-- TRUE_EOF: TURN-29 SOURCE+TEST SOURCE DELIVERED -->

## PARENT SOURCE+TEST SOURCE REVIEW - 2026-07-15T22:36:43-04:00

- 结论：`SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD BUILD PENDING`；父级独立审查
  `P0/P1/P2=0/0/0`。Worker 自述不作为批准依据，本结论也不冒充 `CARD APPROVED/CLOSED`。
- 父级逐文件核对十个 production 文件、唯一 named test、`TurnGameClient`/`TurnInvocationResult` 的 action、
  window、step、frame 关联，以及 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 tracker 常量、标题顺序和
  public caller surface。anchor 搜索与 panel/detail 坐标、五环/五倍/修罗 title、绿字分段、selected link、
  黄字/地图 OCR、fingerprint/cache、prepare/negative/final-consumed 均留在 Cloud 内存算法。
- 八张 live 模板均实际存在于 Cloud `src/main/resources`，且与 DHXY 当前对应模板 SHA-256 完全一致；生产文件
  对 DHXY-only tracker/input/OCR/temp mechanics 和旧 READ/MATERIALIZE transport 的扫描为零命中。
- 每次显式 live 调用只有一次 `TurnGameClient.capture(...)` 或一次 closed
  `DRAG_LEFT + WAIT(500) + CAPTURE`；exact action/device/window/step/frame/PNG/SHA/尺寸不符及
  FAILED/STOPPED/uncertain 均 typed fail-closed。没有自动 retry、第二 exchange、owner/session/ledger/TTL。
- named test source 直接覆盖三任务几何/规则、同帧 read/prepare/final、cache scope、fast miss、下一次显式
  reposition 与 terminal/identity/step/frame/PNG 失败矩阵。Cloud Java writers 尚未稳定，本轮未运行 Maven；
  后续必须 fresh 运行 `TaskTrackerPanelTurnContractTest` 与适用 Cloud compile/build cohort。
- `TURN-30/31/32` 依赖现已解锁；三张 caller 卡分别独占 Xiuluo/Wubei/FiveRing Task 文件与各自 named test，
  可以并行领取，严禁跨写集修改本 core 或其它 Task。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-29 PARENT SOURCE+TEST SOURCE REVIEW PASSED -->
