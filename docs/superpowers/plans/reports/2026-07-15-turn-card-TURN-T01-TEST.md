# TURN-T01-TEST Worker E Report

## CLAIMED

- 时间：2026-07-15 EDT
- 角色：CR271 Worker E，唯一负责 `TURN-T01-TEST`；父级 Codex 是唯一 reviewer，本 Worker 不自批。
- 范围：双仓五个指定 protocol golden/validator 测试、双仓 `src/test/resources/cloud-turn/v1/` 七个 plan-frozen JSON fixtures，以及本报告。
- 禁区：不改 production、POM 或其它文档；不做 Git mutation；不启动 runtime/application/server/Task/UI/capture/input。
- 基线已读：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、权威计划第 19 节、`2026-07-15-https-turn-thin-client-protocol-design.md`，并已核对 DHXY 与 `dhxy-cloud-brain` 当前 `git status --short --branch`。
- 并行依赖：`TURN-40A` production 正在写入；先交付既有协议的 `PG+EX` 测试，最终重读该 Worker 报告及两仓最终 production 后再适配 `LIFE` 并运行双仓五项 named tests。
- 行为差异：无已批准业务差异；本卡只验证冻结协议。

## TEST DELIVERED

- 交付时间：2026-07-15 EDT
- Worker 结论：`TEST DELIVERED`。等待父级 Codex 唯一 reviewer 审查；本 Worker 未自批、未写 `Approved`。
- 所有权说明：父级已确认 Worker E 的 `CLAIMED` 早于 Nash 的后续重复领取。Nash 并发期间写入的有效 validator/lifecycle 测试内容已保留并纳入最终逐文件 SHA；未回滚任何并发改动。
- 写集遵守：仅写双仓五个指定测试、双仓七个指定 fixture 和本报告。未修改 production、POM、其它测试或其它文档；未做 Git mutation；未启动 runtime/application/server/Task/UI/capture/input。
- 业务基线：无已批准业务差异；按冻结协议做测试交付。

### 覆盖范围

- `PG`：使用与 production 约束等价的严格 `ObjectMapper` 做 fixture/DTO 双向 round-trip；覆盖未知字段、未知/数字 enum、尾随 JSON、primitive null 及 closed-enum 集合，并启用严格重复字段检测。
- `EX`：覆盖 `INPUT -> WAIT -> LOCAL_SERVICE -> CAPTURE` 有序 steps、typed union、`actionId` 必填、device/window 关联、单帧 shape/hash/dimensions，以及非法 mixed union 和多 requested frame。
- `LIFE`：覆盖冻结任务队列顺序、`startRequestId` start/ack 对应、稳定重投、空队列/空 id/未知 task/未知 policy、`pauseRequested` 与 `stopRequested` 独立、unsolicited/missing/mismatched ack，并明确拒绝 `SLEEP_COMPUTER`。
- outcome：fixture 与 validator 覆盖 `COMPLETED`、`FAILED`（含 `FAILED + NOT_RUN` 尾部及 failure frame）、`STOPPED`、`DUPLICATE_OR_UNCERTAIN`；同时覆盖 `ACTION`/`IDLE` response union。
- 断言纪律：标准 Maven 阻断后未弱化任何断言；隔离诊断仍执行相同五个测试源码。

### TURN-40A 最终基线复核

已完整重读 `2026-07-15-turn-card-TURN-40A-T01.md` 的最终 `SOURCE DELIVERED`、review 记录及双仓最终 production。下列八个 production 文件在双仓 byte-identical；本 Worker 只读取并据此适配测试，未修改它们。

| production 文件 | Bytes | SHA-256 |
|---|---:|---|
| `TurnTaskCode.java` | 129 | `a116361ee173f37639967459111ab1cc595469ee1c8f8e3ba91e108bdb2895f7` |
| `TurnTaskQueueFailurePolicy.java` | 131 | `2b9dbff0f75612dcc818b89825f2ec003a8bd08adf05176d7592f4c570b5c97c` |
| `TurnTaskStartRequest.java` | 338 | `d4af7b55dd1b4a6b01df5eed4e9f2468b745a31241314762242c340d0ff03117` |
| `TurnTaskStartAck.java` | 101 | `b5c196c7084211ae917db543411c08247611b0a962cd60219e478660bc1d299b` |
| `TurnWindowMetadata.java` | 681 | `e1430169aae3e35ac9f6295e41ea401e66eff910e0b4b4dff954e72c9416af1b` |
| `TurnRequest.java` | 505 | `bf7ea75a8ff44cbd5b7fb8f73ecf172bb7466b00eecc1407087688e5d74b571c` |
| `TurnResponse.java` | 328 | `646f9738fc296949be6d1787481b8f946f2cf4cfa7d2971d5b4b987ba2f1e75f` |
| `TurnProtocolValidator.java` | 21484 | `1590fed78e690e36905f9ee3f697c5a03e16f5cc7f89ad077a3cd3fc2e0bb9c2` |

### 双仓交付 SHA

以下为最终源文件字节数和 SHA-256。逐对读取 DHXY 与 `dhxy-cloud-brain` 后，12/12 均为 byte-identical。

| 相对路径 | Bytes | SHA-256 | 双仓一致 |
|---|---:|---|---|
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java` | 12779 | `18606e49af67968d55f4b18c904c81de8150790b29da2068aa5d60454a39061e` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java` | 3166 | `d3afe584104f55de0f9bdedd415e5eccbbc6be0610474554cb11e8236b31ff98` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnEnvelopeGoldenJsonTest.java` | 4181 | `011b2126f11654b823418ef4680c585267cccff89dd88e1626e9056967d95550` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` | 9399 | `49d98dd8190d3626348ffb49317e1d1728556bac0fddde2ea98c09ede90eab27` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java` | 7479 | `fd66b0a686978f17b79d4972f38946b919cd68b4aed12168a2d810a9e298f41b` | 是 |
| `src/test/resources/cloud-turn/v1/action-input-capture.json` | 1476 | `17d27ac802db8b4047f235d71d5c2806e98460ca0cfb2878c8de8f3179cad429` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-completed.json` | 1563 | `06576660cfef6ccce0e945fdcb09460f07ce4fae2338a0686252f84b323bfb28` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-failed-with-frame.json` | 1355 | `9fa5ca5b11a32d0e01d1a9f18466288bc147dc7c1c0472c5ddc933797b4e20f1` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-stopped.json` | 900 | `79b60b085b8e06eee8539eedc1616cef9357868726fb7e13ddf6aeaa6c503370` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-duplicate-or-uncertain.json` | 618 | `72a022e1a56356bc588f9239e8c50402e1141e3eca4c53de9fd9691d762011d0` | 是 |
| `src/test/resources/cloud-turn/v1/request-start.json` | 643 | `6354ad76a3fbe144c1d4fc2cc7aca4e1a6ddd8a04c37c4e62e54dcd90d1da027` | 是 |
| `src/test/resources/cloud-turn/v1/response-start-ack-idle.json` | 108 | `722ddba237d6ee09dba21cd3f2228e459766a47e04f0b40f2be0322068624376` | 是 |

### 十项标准 Maven 命令证据

按要求在两仓分别、逐类运行以下标准命令。十条命令均在进入本卡 Surefire 测试前被仓库当前范围外的编译错误阻断，因此真实结果均为 `exit 1 / 本 named test tests run 0`。这些结果未被隔离诊断替代，也未通过跳过测试伪装为成功。

| 仓库 | 标准命令 | Exit | 本类执行 | 首个阻断证据 |
|---|---|---:|---:|---|
| DHXY | `mvn -q -Dtest=TurnCoreProtocolGoldenJsonTest test` | 1 | 0 | `SummonSkillStartIndexPolicyTest.java:[19,39]` 找不到 `resolveStartIndex(...)` |
| DHXY | `mvn -q -Dtest=TurnActionGoldenJsonTest test` | 1 | 0 | 同一既有非本卡 testCompile 错误 |
| DHXY | `mvn -q -Dtest=TurnEnvelopeGoldenJsonTest test` | 1 | 0 | 同一既有非本卡 testCompile 错误 |
| DHXY | `mvn -q -Dtest=TurnProtocolValidatorContractTest test` | 1 | 0 | 同一既有非本卡 testCompile 错误 |
| DHXY | `mvn -q -Dtest=TurnTaskLifecycleProtocolGoldenJsonTest test` | 1 | 0 | 同一既有非本卡 testCompile 错误 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnCoreProtocolGoldenJsonTest test` | 1 | 0 | `TaskTrackerPanelService.java:[3,25]` 找不到 `com.bot.dhxy.core.GameClientTracker` |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnActionGoldenJsonTest test` | 1 | 0 | 同一非本卡 mainCompile 迁移依赖错误 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnEnvelopeGoldenJsonTest test` | 1 | 0 | 同一非本卡 mainCompile 迁移依赖错误 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnProtocolValidatorContractTest test` | 1 | 0 | 同一非本卡 mainCompile 迁移依赖错误 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnTaskLifecycleProtocolGoldenJsonTest test` | 1 | 0 | 同一非本卡 mainCompile 迁移依赖错误 |

标准 gate 阻断归属：DHXY 为既有其它测试源码与当前 production API 不一致；Cloud 为并行迁移中的 production 依赖尚未齐备。两者均不在 Worker E 唯一写集内，故仅记录证据，不修改或弱化本卡断言。

### 隔离诊断证据

为区分本卡测试源码/fixture 问题与仓库外部编译问题，在不修改 production/POM/其它测试的前提下，DHXY 仅临时编译本卡五个测试；Cloud 另将当前协议 production 源码临时编译到 `target/classes`，随后临时编译本卡五个测试并直接执行相同五类 Surefire 测试。此项只作诊断，不冒充上述标准 Maven gate，临时产物也不属于交付写集。

| 仓库 | 结果 | 分类计数 |
|---|---|---|
| DHXY | `exit 0`，20 tests，0 failures，0 errors，0 skipped | Core 5；Action 2；Envelope 4；Validator 4；Lifecycle 5 |
| `dhxy-cloud-brain` | `exit 0`，20 tests，0 failures，0 errors，0 skipped | Core 5；Action 2；Envelope 4；Validator 4；Lifecycle 5 |

诊断执行命令的 Surefire 阶段为：

```text
mvn -q "-Dtest=TurnCoreProtocolGoldenJsonTest,TurnActionGoldenJsonTest,TurnEnvelopeGoldenJsonTest,TurnProtocolValidatorContractTest,TurnTaskLifecycleProtocolGoldenJsonTest" surefire:test
```

第一次 DHXY 隔离诊断期间，并行 Maven 曾短暂把共享 `target/test-classes` 中的 `request-start.json` 刷成另一版本；源文件未变。待共享输出稳定并重新复制当前七个 fixture 后复跑为上述 `20/0/0/0`，最终双仓源文件 SHA 仍完全一致。

### 交接注意

- `dhxy-cloud-brain/.gitignore:15` 当前以 `src/test/` 忽略整个 Cloud 测试树，因此 Cloud 的五个测试和七个 fixture 不出现在普通 `git status --short` 中。文件已实际存在且与 DHXY byte-identical；本 Worker 遵守禁令，没有修改 `.gitignore`、没有 `git add -f`，父级后续 Git 交付时需显式处理。
- 双仓 `cloud-turn/v1/frame-2x2.png` 属于其它并行任务，不在七 fixture 写集内，未触碰。
- 标准 Maven gate 仍需等待上述仓库外部编译阻断解除后，由父级 reviewer 复跑十条命令。当前状态是测试材料已交付、标准命令证据已如实记录，不代表 reviewer approval。

## PARENT TEST SOURCE REVIEW #1 - REPAIR REQUIRED

- reviewedAt: `2026-07-15T19:09:10-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/1/2 / REPAIR #1`
- parity retained: parent independently recomputed all 12 test/fixture pairs as byte-identical and all 29 Java
  files under the two production protocol trees as `29/29` byte-identical. The ten required Maven commands remain
  `exit 1 / selected tests 0`; isolated `20+20` passes are diagnostic only.

### P1-1 - Test-owned mapper is presented as the production parser

- Evidence: `TurnProtocolGoldenSupport.PRODUCTION_MAPPER` at
  `TurnCoreProtocolGoldenJsonTest.java:161-168` is a test-local `new ObjectMapper()`. The malformed JSON cases at
  `TurnProtocolValidatorContractTest.java:18-47` therefore prove only that this test mapper rejects them. They do not
  execute either repository's HTTP JSON parser. DHXY production `HttpsTurnClient.java:76-80` currently has a
  different feature set, so the name and delivery claim overstate the evidence.
- Impact: the T01 suite can remain green while a real client/parser accepts a numeric enum, scalar coercion, float to
  integer coercion, or null primitive before validation. That is a false-positive PG acceptance signal.
- Repair condition in this card: rename the helper and test wording to `STRICT_CONTRACT_MAPPER` (or an equally
  explicit non-production name) in both byte-identical repositories and state that actual ingress/client parsing is
  covered by T02/T03A. The parent has separately reopened T03A Repair #2 for the real DHXY parser; T01 must not claim
  that its private mapper substitutes for that boundary.

### P2-1 - Ordered step indices have no rejecting regression case

- Evidence: legal fixture order is asserted, but no test rejects duplicate, skipped, or out-of-order
  `TurnStep.index`; production enforcement is `TurnProtocolValidator.java:76-82`.
- Repair condition: add direct fail-closed cases for invalid step index/list-position correlation in the existing
  validator contract test in both repositories.

### P2-2 - The one-frame negative case omits QUEST_CAPTURE_DETAIL

- Evidence: `TurnProtocolValidatorContractTest.java:63-73` only combines CAPTURE and MATCH_TEMPLATE. Production also
  counts `QUEST_CAPTURE_DETAIL` as an uploading operation at `TurnProtocolValidator.java:238-243`.
- Repair condition: add a `QUEST_CAPTURE_DETAIL + CAPTURE(UPLOAD_IMAGE)` rejection case, proving the permanent-local
  Quest result shares the same single frame slot.

### Frozen Repair #1 write set

Only the byte-identical copies of these existing tests and this report may change:

1. both `TurnCoreProtocolGoldenJsonTest.java`
2. both `TurnProtocolValidatorContractTest.java`
3. both `TurnTaskLifecycleProtocolGoldenJsonTest.java` only if required by the mapper-symbol rename
4. `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T01-TEST.md`

No fixture, production, POM, `.gitignore`, other test/document, or Git metadata change is authorized. Cloud ignored
test-tree retention remains a later delivery/commit gate; it must not be hidden, but this card may not mutate Git to
work around it. The original Worker must append Repair #1 delivery evidence and truthful named Maven results.

## REPAIR #1 TEST DELIVERED

- repairedAt: `2026-07-15 EDT`
- worker: `CR271 Worker E / original TURN-T01-TEST owner`
- status: `REPAIR #1 TEST DELIVERED / PARENT RE-REVIEW REQUIRED`
- approval boundary: 本节是原 test Worker 的返修交付，不是 review 或 self-approval；等待父级 Codex 复审。
- write-set integrity: 只修改双仓 `TurnCoreProtocolGoldenJsonTest.java`、
  `TurnProtocolValidatorContractTest.java`、mapper symbol rename 必需的
  `TurnTaskLifecycleProtocolGoldenJsonTest.java`，以及在本报告 true EOF 追加本节。未修改 fixtures、production、
  POM、`.gitignore`、其它 tests/docs 或 Git metadata；未启动 runtime/server/Task/UI/capture/input。

### P1-1 Repair - Test-local mapper evidence corrected

- 双仓 helper 已从 `PRODUCTION_MAPPER` 更名为 `STRICT_CONTRACT_MAPPER`；Core、Validator、Lifecycle 的对应
  test method 名称、符号引用和 round-trip 失败文案均改为 strict-contract 语义。
- 更正并取代本报告此前 PG 描述：`STRICT_CONTRACT_MAPPER` 是测试代码自行构造的严格 contract mapper，
  只证明 DTO/fixture 在这套测试约束下的序列化、反序列化和 malformed JSON 行为；本 T01 测试没有执行、
  替代或证明任一仓库真实 HTTP ingress/client parser。
- 真实 parser 边界由父级分派的 `T02/T03A` 覆盖，其中 DHXY parser 修复属于 `T03A Repair #2`；本 Worker
  未触碰该 production 路径。
- 双仓三份获准测试经 `rg` 复核，`PRODUCTION_MAPPER`、`ProductionMapper`、`production ObjectMapper`、
  `production parser` 和 `ThroughProduction` 命中数均为 0。

### P2-1 Repair - Invalid step index/list-position

`TurnProtocolValidatorContractTest.duplicateSkippedAndOutOfOrderStepIndicesFailClosed()` 新增三个彼此独立的
fail-closed 动作，所有 step 除 index 外均合法，并均直接调用 production
`TurnProtocolValidator.requireValid(...)`：

| 场景 | step index 序列 | 预期/结果 |
|---|---|---|
| 重复 | `[0, 0]` | `IllegalArgumentException` |
| 跳号 | `[0, 2]` | `IllegalArgumentException` |
| 乱序 | `[1, 0]` | `IllegalArgumentException` |

### P2-2 Repair - QUEST_CAPTURE_DETAIL shares the frame slot

- 在既有 one-frame 负例中新增合法 `QUEST_CAPTURE_DETAIL` local step，后接合法
  `CAPTURE(UPLOAD_IMAGE)` step。
- 两个 step 的 index 为 `[0, 1]`，各自 typed union 均合法；组合动作直接由
  `TurnProtocolValidator.requireValid(...)` 拒绝为 `IllegalArgumentException`，覆盖 Quest detail 与 capture
  共用“最多一个 returned frame”槽位的冻结规则。

### Repair #1 Final SHA / Parity

返修后重新读取双仓全部五个 T01 tests 和七个 fixtures。以下 12 对文件的字节数与 SHA-256 均相同，
结论为 `12/12 byte-identical`。

| 相对路径 | Bytes | SHA-256 | 双仓一致 |
|---|---:|---|---|
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java` | 12822 | `fc766e450f756db75583917d1b8a81510d81a96803ba2d178ee933f9acac8e42` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java` | 3166 | `d3afe584104f55de0f9bdedd415e5eccbbc6be0610474554cb11e8236b31ff98` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnEnvelopeGoldenJsonTest.java` | 4181 | `011b2126f11654b823418ef4680c585267cccff89dd88e1626e9056967d95550` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` | 11403 | `b59377c3e3e8b18565e313f1b315c2b0631236f6579a90873a30e732b9802f3b` | 是 |
| `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java` | 7518 | `57aabf91b654a688a175bbac7e3a7d1ac2d238852dc2d8a74a2fe24375bee288` | 是 |
| `src/test/resources/cloud-turn/v1/action-input-capture.json` | 1476 | `17d27ac802db8b4047f235d71d5c2806e98460ca0cfb2878c8de8f3179cad429` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-completed.json` | 1563 | `06576660cfef6ccce0e945fdcb09460f07ce4fae2338a0686252f84b323bfb28` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-failed-with-frame.json` | 1355 | `9fa5ca5b11a32d0e01d1a9f18466288bc147dc7c1c0472c5ddc933797b4e20f1` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-stopped.json` | 900 | `79b60b085b8e06eee8539eedc1616cef9357868726fb7e13ddf6aeaa6c503370` | 是 |
| `src/test/resources/cloud-turn/v1/outcome-duplicate-or-uncertain.json` | 618 | `72a022e1a56356bc588f9239e8c50402e1141e3eca4c53de9fd9691d762011d0` | 是 |
| `src/test/resources/cloud-turn/v1/request-start.json` | 643 | `6354ad76a3fbe144c1d4fc2cc7aca4e1a6ddd8a04c37c4e62e54dcd90d1da027` | 是 |
| `src/test/resources/cloud-turn/v1/response-start-ack-idle.json` | 108 | `722ddba237d6ee09dba21cd3f2228e459766a47e04f0b40f2be0322068624376` | 是 |

### Repair #1 Named Maven Evidence

返修完成后在两仓重新逐类运行原卡十条标准命令。十条均在 selected test 进入 Surefire 前被范围外编译错误
阻断；真实结果仍为 `exit 1 / selected tests 0`，未用 diagnostic 结果替代。

| 仓库 | 标准命令 | Exit | Selected tests | 阻断 phase / 首个证据 |
|---|---|---:|---:|---|
| DHXY | `mvn -q -Dtest=TurnCoreProtocolGoldenJsonTest test` | 1 | 0 | `testCompile`; `SummonSkillStartIndexPolicyTest.java:[19,39]` 找不到符号 |
| DHXY | `mvn -q -Dtest=TurnActionGoldenJsonTest test` | 1 | 0 | 同上 |
| DHXY | `mvn -q -Dtest=TurnEnvelopeGoldenJsonTest test` | 1 | 0 | 同上 |
| DHXY | `mvn -q -Dtest=TurnProtocolValidatorContractTest test` | 1 | 0 | 同上 |
| DHXY | `mvn -q -Dtest=TurnTaskLifecycleProtocolGoldenJsonTest test` | 1 | 0 | 同上 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnCoreProtocolGoldenJsonTest test` | 1 | 0 | `mainCompile`; `TaskTrackerPanelService.java:[3,25]` 找不到符号 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnActionGoldenJsonTest test` | 1 | 0 | 同上 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnEnvelopeGoldenJsonTest test` | 1 | 0 | 同上 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnProtocolValidatorContractTest test` | 1 | 0 | 同上 |
| `dhxy-cloud-brain` | `mvn -q -Dtest=TurnTaskLifecycleProtocolGoldenJsonTest test` | 1 | 0 | 同上 |

上述错误没有指向 T01 五个测试、七个 fixture 或 protocol production 文件；错误所属文件均超出 Repair #1
冻结写集，故仅如实记录，未修改本卡断言或越界修复。

### Repair #1 Isolated Diagnostic Only

为验证返修源码本身，在标准命令失败后执行与初次交付相同的隔离诊断流程：临时编译五个 T01 tests；
Cloud 另将当前 protocol production 源临时编译到 `target/classes`；随后仅调用以下 Surefire goal：

```text
mvn -q "-Dtest=TurnCoreProtocolGoldenJsonTest,TurnActionGoldenJsonTest,TurnEnvelopeGoldenJsonTest,TurnProtocolValidatorContractTest,TurnTaskLifecycleProtocolGoldenJsonTest" surefire:test
```

| 仓库 | Diagnostic exit | Tests | Failures | Errors | Skipped | 分类计数 |
|---|---:|---:|---:|---:|---:|---|
| DHXY | 0 | 21 | 0 | 0 | 0 | Core 5；Action 2；Envelope 4；Validator 5；Lifecycle 5 |
| `dhxy-cloud-brain` | 0 | 21 | 0 | 0 | 0 | Core 5；Action 2；Envelope 4；Validator 5；Lifecycle 5 |

隔离结果只证明当前五份测试源码在当前 protocol class 上通过，不是标准 Maven gate，也不证明真实 HTTP
parser 行为。Cloud `src/test/` 仍被现有 `.gitignore:15` 忽略；Repair #1 未隐藏或绕过该后续交付门禁。

## PARENT TEST SOURCE REVIEW #2 - REPAIR #1 PASSED

- reviewedAt: `2026-07-15T19:24:38-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/0/0 / TEST SOURCE REVIEW PASSED / REQUIRED MAVEN GATES BLOCKED`
- Parent independently re-read all Repair #1 test changes. `STRICT_CONTRACT_MAPPER` and its assertion wording now
  accurately describe a test-owned strict contract mapper; no test claims to execute a production HTTP parser.
  Actual DHXY response parsing is separately covered by the passed T03A Repair #2 boundary.
- Duplicate `[0,0]`, skipped `[0,2]` and out-of-order `[1,0]` step indices are direct validator rejection cases.
  `QUEST_CAPTURE_DETAIL` followed by `CAPTURE(UPLOAD_IMAGE)` is also directly rejected, proving both operations share
  the frozen single returned-frame slot.
- Parent independently recomputed all 12 bilateral test/fixture pairs as byte-identical and the complete bilateral
  production protocol tree as `29/29` byte-identical. No assertion bypass or fixture/production drift was found.
- All ten standard Maven commands remain `exit 1 / selected tests 0` because unrelated DHXY testCompile and Cloud
  mainCompile debt blocks Surefire. Isolated `21+21` passes are diagnostic only. Cloud's ignored `src/test/` tree
  remains a later delivery/Git gate; this card did not mutate Git.
- The implementation owner is released. No further T01 repair is requested; the original ten commands and Cloud test
  retention must be closed before card approval.

**No approved business differences; equivalent migration against baseline `696a12b0`.**
