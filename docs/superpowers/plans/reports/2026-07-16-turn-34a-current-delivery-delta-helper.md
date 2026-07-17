# TURN-34A External C 当前交付差额审计

> 角色：CR271 Internal helper，仅审计 TURN-34A External C 当前交付差额。  
> 观察时点：`2026-07-16T09:32:10.2730434-04:00`。  
> 本报告不作批准或阻断结论；未修改 Java、原卡、权威计划、`ACTIVE_WORK.md` 或其它既有文档，未运行 Maven/JUnit/compile/runtime/input，未执行 Git mutation。

## 1. 已读取依据

- 完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`。
- 读取 `docs/ACTIVE_WORK.md:3-37` 顶部 CR271 当前段。
- 完整读取权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:1016-1697` 第 14-19 节。
- 完整读取 TURN-34A 固定卡 `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34A.md` 至当前物理 true EOF。
- 完整读取 Cloud 当前 `AutoCombatService.java` 852 行与 `AutoCombatServiceTurnContractTest.java` 763 行。
- 按 `AGENTS.md` 的五倍/修罗基线门补读 `docs/业务逻辑.md:71-168` 的 CommonBox 优先级与 `:213-281` 的 expected 战斗快脱战规则；本审计不改业务裁决。

## 2. External C 当前唯一写集

TURN-34A 全周期固定写集只有：

1. Modify Cloud `src/main/java/com/bot/dhxy/service/AutoCombatService.java`。
2. Create/modify Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`。
3. Append-only TURN-34A 固定卡 true EOF。

父级 Review #1 后，Repair #1 的实际可写集收窄为第 2 项 named test 加第 3 项原卡 append；production 在原卡 `:332-370` 被写为 source review passed，并要求保持 SHA `532e6f84...` 只读。External C 已在当前 true EOF 交还 owner；截至观察时点，原卡尚未出现 replacement claim。其余两仓文件仍全部只读。

## 3. 当前字节快照

| 路径 | 行/字节 | mtime（EDT） | SHA-256 | 与卡片关系 |
|---|---:|---|---|---|
| Cloud `AutoCombatService.java` | 852 / 46,414 | `2026-07-16T06:29:17.7816908-04:00` | `532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9` | 与父级 Review #1 记录的 production 只读 SHA 一致；Review #1 后无 mtime/SHA 变化。 |
| Cloud `AutoCombatServiceTurnContractTest.java` | 763 / 37,108 | `2026-07-16T08:45:50.7610704-04:00` | `60E49ED9C641801AF81D02DF968C66ACDB7BE4B18BD6F225BFE70DDD14A8BBC6` | 晚于旧 partial delivery 的 611 行 / `5e2ca53f...`，也晚于原卡 `08:40:57` 记录的 744 行 / `1df2c63a...`；属于尚未交付的 Repair #1 中途字节。 |
| TURN-34A 固定卡 | 439 / 42,341 | `2026-07-16T09:31:43.3700550-04:00` | `B83378C8424274AEB16F09C608D36623005BE8CBE193B9C18FDF152E7CF55396` | 当前 true EOF 是 External C returned、production passed-readonly、test incomplete，不是 Repair #1 delivery。 |

## 4. 原卡最后 owner / delivery 事实

- 最后 owner 事实：原卡 `:411-439` 的物理 true EOF 是 `EXTERNAL-C RETURN`。External C 明确交还 owner；截至观察时点，卡内没有 replacement claim。
- 最后一次 Worker delivery 是原卡 `:282-324` 的 `SOURCE+TEST DELIVERED (PARTIAL NAMED-TEST COVERAGE DECLARED)`，对应旧 test SHA `5e2ca53f...`。
- 该旧 delivery 随后在 `:326-377` 被父级记录为 production source passed、test source Repair #1 required；它不是当前 Repair #1 的交付记录。
- 当前 true EOF 明确写 `RETURNED PRODUCTION-PASSED-READONLY TEST-INCOMPLETE`，没有 `REPAIR #1 SOURCE+TEST DELIVERED`。磁盘上的 763 行 test 作为 incomplete WIP 被交还，不是当前 delivery handoff。

## 5. 当前 test 的编译级证据差额

本 helper 未运行编译；以下是当前源码声明之间可直接核对的静态事实：

1. test `:35` import `com.yueyunfe.dhxy.cloudbrain.host.CloudTemplateCatalog`，当前 production 类型实际位于 `com.yueyunfe.dhxy.cloudbrain.turn.CloudTemplateCatalog`；前一 FQCN 在 Cloud main/test source 中不存在。
2. test `:37`、`:40` import `remote.CloudBagLocalServiceClient` 与 `remote.CloudUiCleanerLocalServiceClient`，当前 production 类型实际位于 `turn.client`；两个被 import 的 FQCN 均不存在。
3. 即使改为现有 `turn.client` 类型，test `:678`、`:684` 使用的两个 no-arg constructor 也不存在；两类当前 public constructor 都要求一个 `TurnGameClient`。
4. test `:690-691` 调用 `new CommonBoxService(..., System::currentTimeMillis)` 与三参数 `new CloudCommonBoxPortAssembly(...)`；当前两个三参数 constructor 都是 `private`，公开入口均为二参数 constructor。
5. 原卡和权威计划要求保留实际 named-test 命令、exit code、tests run、failures/errors，并在适用时保留 Cloud compile exit 0。当前原卡明确记录未运行 Maven/JUnit/compile，当前也没有任何 fresh 编译或点名测试结果。

所以当前材料只有 Java 源字节和静态 fixture 草稿，没有“该 763 行 named test 已成功 test-compile”的证据。

## 6. 当前 test 的契约证据差额

- 当前仍是 17 个 `@Test`，数量与旧 partial delivery 声明的 17 个相同。没有新增测试方法执行 Repair #1 要求的 action-capable 路径。
- 新增的 `battleFlagTemplate`、`blankRoiPng`、`battleFlagRoiPng`、`completedCapture`、`enqueueCaptures` 都只有定义或内部互调，没有任何 `@Test` 调用；`ScriptedCommandPort.enqueue*` 也没有测试调用。
- 现有测试对 `executeCalls` 的 12 处断言全部是 `0`。没有一次成功 command、FAILED、STOPPED outcome、`DUPLICATE_OR_UNCERTAIN` 或 timeout/interrupted uncertainty 脚本，也没有 fresh UUID/command 1:1、顺序、零 compensation/retry 的 action 证据。
- `EXIT_RECOVERED` 只出现在 enum 值列表断言，没有 tick 返回 `EXIT_RECOVERED` 的测试。没有真实 `NONE -> IN_COMBAT -> EXIT_RECOVERED` 序列。
- test 没有引用或实例化 `AutoBattleTask`、`FiveRingTaskV2`、`WubeiTask`、`XiuluoTaskV2`，因此没有四个真实 caller phase/tick 消费证据。
- 没有执行 FAST `15s/1s/4s`、enter `+4s`、三个 recovery policy 动作序、CommonBox-before-first-aid、one re-probe、deferred leader clear point、maintenance `4s/40s/30s/10s`、panel `(489,726)/>20px` 的测试路径。
- `initializeClearsPendingWorkButKeepsDeferredLeaderRecovery` 没有先制造 deferred leader pending；它只断言 initialize 后仍为 `false`，不能证明已有 pending 被保留。
- 当前 production `nextCombatWakeDelayMs()` 对 fresh state 会先由 `nextCombatMaintenanceDelayMs()` 得到 `0`，而 test `:375-384` 直接对 fresh state断言结果在 `500..10000`；caller clamp 没有在该测试中执行。
- 当前 `RefreshDuePanelVerifyGate.reserveIfAllowed` 只按 team key 保存 `lastAt`，没有 same-window bypass；test `:423-431` 的同一 team/window `+10ms` 第二次调用却断言 `deferred=false`。这是当前 production 路径与当前断言的静态不一致。
- 固定卡 `:139-140` 还要求 active AutoCombat 对 direct input/capture、七个旧 `BATTLE_RADAR` fact 与 Summon authority 的 source gate；当前反射检查只覆盖六个 post-baseline 方法名和三个 legacy field type，没有覆盖这些余项。

## 7. 是否已有可交付闭环

**截至本报告观察时点：没有当前可交付闭环。** 这是生命周期与证据状态记录，不是 reviewer 的批准或阻断裁决：

- 当前 Repair #1 字节尚无原卡 true EOF `REPAIR #1 SOURCE+TEST DELIVERED`；
- 当前 named test 仍有可静态定位的类型/constructor 解析差额；
- Repair #1 点名的 action、四 caller、timing/recovery/maintenance/terminal/UUID 契约证据尚未进入实际 `@Test`；
- required named test 与适用 Cloud compile 的 fresh exit-0 证据尚不存在。

TRUE_EOF PRECHECK_COMPLETE
