CLAIMED | uuid=019f6acc-877b-7ea1-9de7-b5d7e52043f5 | nickname=Leibniz | role=CR271 Internal helper (not implementation owner, not reviewer) | claimedAtSnapshot=2026-07-16T08:15:00.4411382-04:00

# CR271 TURN-34A Delivery Preflight Helper R2

## 1. 身份、边界与结论口径

- 本报告是 TURN-34A Internal delivery/preflight 辅助材料，不是 implementation delivery、review、批准、阻断或父级裁决。
- **非父级批准。** 下文的 P0/P1/P2 只表示建议父级优先核验的候选，不是 reviewer finding，也不改变卡片状态。
- 唯一写集是本报告：
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-34A-delivery-preflight-helper-r2.md`。
- 未修改 Java、POM、测试、原卡、权威计划、`ACTIVE_WORK.md`、业务逻辑、矩阵或 dashboard；未执行 Git mutation、Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input。
- 两仓全部 dirty/untracked/ignored 原样保护。本报告只做静态源码与卡片 true EOF 核验。

## 2. 已读权威材料

- 完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`。
- 读取 `docs/ACTIVE_WORK.md` 顶部最新 CR271 调度；当前身份由该段记录为 Leibniz
  `019f6acc-877b-7ea1-9de7-b5d7e52043f5`，职责是 TURN-34A delivery preflight。
- 完整读取权威计划
  `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节及最新变更。
- 完整读取协议规格
  `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
- 完整读取 TURN-34A 原卡、当前两文件、`696a12b0` 的 `AutoCombatService.java`，并读取四个真实 Task caller、
  `TaskExecutionContext`/holder、六个冻结 `TaskMaintenanceService` API 与本测试所调用构造 API 的相关源码。

## 3. 本轮发生的 delivery 状态转换

本 helper 首次读取原卡时，物理 true EOF 仍为
`PARENT RESUME OBSERVED / EXTERNAL-C UNIQUE-OWNER CONTINUES`；当时只有 mtime/SHA 变化，不能算交付。

本轮继续核验期间，External C 于原卡 `:282-324` 正式追加：

`SOURCE+TEST DELIVERED | deliveredAt=2026-07-16T08:06:55.921-04:00`

当前原卡真实尾标是：

`<!-- TRUE_EOF: TURN-34A EXTERNAL-C SOURCE+TEST DELIVERED PARTIAL-NAMED-TEST-COVERAGE-DECLARED 2026-07-16T08:06:55.921-04:00 -->`

因此本报告以该正式 delivery 对应字节做 preflight；此前任何中途 mtime/SHA 变化仍不被倒算成交付。原卡同时在
`:306-310` 明确声明 named-test coverage 不完整，故 delivery 只触发父级逐文件审查，不等于 source/test pass。

## 4. 最终快照：两仓与交付文件

快照窗口：`2026-07-16T08:13:55.8005570-04:00` 至 `2026-07-16T08:15:00.4411382-04:00`。

| 对象 | branch / HEAD / Git 状态 | SHA-256 / mtime / size |
|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`；85 项 status（44 tracked/deleted，41 untracked） | 全部保护，未改 |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`；28 项 status（9 tracked，19 untracked） | 全部保护，未改 |
| Cloud `src/main/java/com/bot/dhxy/service/AutoCombatService.java` | `??` untracked（`src/main/java/com/bot/` cohort） | `532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9`；`2026-07-16T06:29:17.7816908-04:00`；46,414 bytes；852 行 |
| Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java` | `!! src/test/`，被 Cloud `.gitignore:15` 忽略 | `5E2CA53F90CB25B29016ECDF7A9AA8702B0AB100A56D64B7D0D9A3D072F29361`；`2026-07-16T08:01:17.2202247-04:00`；30,885 bytes；611 行 |
| TURN-34A 原卡 | true EOF 为上述正式 delivery | `1BA02F58AAE911D3452AB7E65266B6EC0037560C0A3D9BA86EEC0422C8B424DF`；`2026-07-16T08:06:56.0629183-04:00`；29,779 bytes；324 行 |

`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的基线 source blob 是
`b1c2d48e89ed6b2ca90b1639df841dd7a97d691a`。只读 `git diff --no-index` 显示当前 production 相对该 blob 的
差异集中在：既有 Cloud UI-cleaner port、删除 coordinator wrapper、切换 exact-context state key/fingerprint，及相关
import/JavaDoc/private nested types；未见其它 tick/recovery/maintenance 主体分支变化。

## 5. Production 静态核验（仅 preflight，不是 source pass）

### 5.1 当前可确认的正向证据

- 冻结 public surface 仍在 `AutoCombatService.java:52-73,82,107-128,199,223,236,252,301,320,328,423,442,802,812-815`；
  两 enum、11 个方法、public decision record/gate 未被删除。
- exact logical key 与 native fingerprint 在 `:740-756,768-776,779-800,831-850`；key 使用
  tenant/user/device/window，fingerprint 留在 state 内，并由一次 `ConcurrentHashMap.compute` 替换。
- `TaskTurnCoordinator` 两个 wrapper 已从 CommonBox/follower 路径移除；当前同步顺序见
  `:476-507` 和 `:510-565`。相对 `696a12b0:475-588` 的差异与冻结卡授权一致。
- 六个冻结 `TaskMaintenanceService` API 仍位于
  `TaskMaintenanceService.java:247,285,321,404,416,468`；AutoCombat 只在
  `AutoCombatService.java:484-545,645-659` 调用这些既有入口。
- production 对 `TaskTurnCoordinator`、`WindowTaskContextHolder`、`WindowRuntimeContext`、
  `getPlayerIdentityEpoch`、`TurnGameClient`、direct input/capture、旧 `BATTLE_RADAR_*` fact 与 Summon authority 的
  symbol scan 为零；仅 JavaDoc 出现普通 `capture/screenshot` 字样。
- 四个真实 caller 仍在：AutoBattle `AutoBattleTask.java:137,141-149,163,280-287`；FiveRing
  `FiveRingTaskV2.java:1847-1869`；Wubei `WubeiTask.java:918-925,3593-3627,3756-3788,4161-4170`；
  Xiuluo `XiuluoTaskV2.java:1819-1836,2060-2084,2247-2255,2433-2471`。本卡没有改这些 caller。

### 5.2 建议父级优先确认的 production 候选

**Potential P0 - exact holder 与显式 context 不一致时未在 orchestrator 入口 fail closed。**

- `handleCombatTick` 在 `AutoCombatService.java:129` 对传入 context checkpoint，却在 `:133` 通过 holder 选择 state；
  后续 radar/state 走 holder，而 recovery/team/player collaborator 又消费传入 context（`:150-172,345-413,476-565`）。
- `handleWindowCombatGuardTick` 与 `probeWindowCombatStateReadOnly` 在 `:199-230` 完全不核 holder/current context 一致性。
- 原卡 acceptance `:128-138` 要求 wrong-scope 零 collaborator action；当前 test 没有构造 holder=A、argument=B 的负例。
- 风险需父级裁决：若误接线发生，A 的 state/radar 与 B 的 recovery/input context 可能在同一 tick 混用。建议父级先要求
  同一 named test 添加 exact mismatch 零 action 证明，再决定 production 是否需入口一致性门。本 helper 不判定成立与否。

**Potential P1 - 无 context public API `getDynamicPollingIntervalMs()` 未执行 mandatory holder gate。**

- `AutoCombatService.java:236-238` 直接调用 `battleRadarService.getDynamicPollingIntervalMs()`；不像其它 no-context API，
  没有先走 `state()`/`requireBoundContext()`。
- 冻结卡 `:30-43` 要求所有无 context public API 缺 binding 时在 collaborator 前 fail closed；当前 missing-holder test
  `AutoCombatServiceTurnContractTest.java:93-106` 只检查 initialize 和两个 pending getter，没有覆盖该方法。

除以上候选外，本次静态 baseline diff 未发现新的 production 业务分支漂移；这不是 P0/P1/P2=0 结论。

## 6. Named test 的直接可执行性候选

以下均由当前 delivery SHA 的源码直接推出，未运行 Maven/JUnit。

### Potential P1 - 当前测试源码存在明确的类型/构造编译矛盾

1. 测试 `:28` import `host.CloudTemplateCatalog`，真实类是
   `turn/CloudTemplateCatalog.java:1,27,44`。
2. 测试 `:30` import `remote.CloudBagLocalServiceClient`，真实类是
   `turn/client/CloudBagLocalServiceClient.java:1,40,56`，且 public constructor 需要 `TurnGameClient`；测试 `:543`
   使用无参构造。
3. 测试 `:33` import `remote.CloudUiCleanerLocalServiceClient`，真实类是
   `turn/client/CloudUiCleanerLocalServiceClient.java:1,31,44`，且 public constructor 需要 `TurnGameClient`；测试
   `:549` 使用无参构造。
4. 测试 `:555-556` 调用 `CloudCommonBoxPortAssembly` 和 `CommonBoxService` 的三参构造；两者三参构造分别在
   `CloudCommonBoxPortAssembly.java:76-83`、`CommonBoxService.java:69-75` 为 private，公开构造都是二参
   （分别 `:69-74` 与 `:64-67`）。

这与原卡 delivery 自审 `:312-314` 所称“包路径/构造缺陷已修正”不一致。建议父级把这组静态矛盾列为第一项 test-source
复核，避免等 shared main compile 债解除后才看到 testCompile 失败。

### Potential P1 - 多个测试在进入 AutoCombat public API 前就会被 context validator 拒绝

- `TaskExecutionContext.requireInitialWindowMetadata` 在
  `TaskExecutionContext.java:458-480` 强制 metadata device/window 与 invocation exact 相同，且 rect 宽高必须正数。
- 测试 helper `AutoCombatServiceTurnContractTest.java:488-516` 固定使用 `BINDING_A` 的 device/window。
- `differentLogicalWindowsKeepIsolatedState` 在 `:113-115` 改了 invocation window，却仍传固定 metadata；
  `tenantUserAndDeviceAreAllPartOfTheLogicalKey` 在 `:139-140` 改了 device，也仍传固定 metadata。
- 两个 uncertainty 用例在 `:304-305,321-322` 构造 `width=0,height=0`；会在 context 构造期被
  `TaskExecutionContext.java:476-480` 拒绝，无法证明 `probeWindowCombatStateReadOnly` 的任何行为。

建议用与 invocation 对齐、尺寸合法的 metadata，再通过同一 `ScriptedCommandPort` 脚本化 FAILED/STOPPED/UNCERTAIN
outcome；不要用非法 context 绕开真实 capture path。

### Potential P1 - 两个断言与当前/696 基线直接冲突

- `refreshDueGateDoesNotLockOutTheSameWindow` 在 test `:403-412` 断言同一 team/window 10ms 内第二次 reserve 仍 allowed；
  当前 production `AutoCombatService.java:815-827` 与 `696a12b0:820-832` 都只按 team key 的 last timestamp，第二次应
  deferred。该断言若要成立会改变冻结 30s gate 语义，不能由测试自行批准。
- `wakeDelayStaysInsideBaselineClamp` 在 test `:355-365` 对 fresh service 的 raw `nextCombatWakeDelayMs()` 断言
  `500..10000`；fresh state 的 `lastCombatUiCleanAt=0` 会让 `nextCombatMaintenanceDelayMs()` 返回 0。真实 clamp 属于
  Wubei caller `WubeiTask.java:918-922` 和 Xiuluo caller `XiuluoTaskV2.java:2248-2252`，不是 service raw return。
  应执行真实 caller phase，而不是把 caller clamp 改写成 service 断言。

## 7. 缺失 acceptance evidence

权威计划 `:1474-1483,1616-1617,1646,1687-1697` 与原卡 `:124-149` 要求完整
`BC4+BASE+TASK+STATE`。当前 delivery 自己在原卡 `:306-310` 明确承认未覆盖，静态测试源码也印证：

- 测试没有 import/实例化/执行 `AutoBattleTask`、`FiveRingTaskV2`、`WubeiTask`、`XiuluoTaskV2`；文件内四 caller
  名称只有注释/metadata 文本，没有真实 caller phase。
- `ScriptedCommandPort.execute` 在 test `:581-591` 对任意 command 直接抛 AssertionError；当前 17 个测试全部是
  zero-command 路径，没有 scripted COMPLETED/FAILED/STOPPED/UNCERTAIN 四态、fresh UUID、1:1 command 或
  `FAILED -> NOT_RUN` 证据。
- 缺 full radar/FAST `15s/1s/4s`、enter `+4s`、三 recovery policy、exit consume、CommonBox-before-first-aid、
  follower one re-probe、deferred leader clear-before-work、`4s/40s/30s/10s` maintenance、panel `(489,726)/>20px`。
- `initializeClearsPendingWorkButKeepsDeferredLeaderRecovery`（test `:334-353`）从未先创建 deferred pending，只证明
  initialize 后是 false，不能证明“故意保留”。
- source gate test `:419-449` 只反射 method/field type；它没有证明 method body 对旧 facade/fact/macro/direct
  input/capture/七个旧 BATTLE_RADAR fact/Summon authority 零引用。production 当前静态 scan 虽为零，测试断言仍缺。
- `mvn -q -Dtest=AutoCombatServiceTurnContractTest test` 没有 fresh exit 0 记录；Cloud compile/build 也未运行。
- 没有 parent production-source review、parent test-assertion review、两名独立 reviewer 结论或适用 compile/build
  成功证据；按计划状态机不能进入 `CARD APPROVED`。
- named test 当前被 `.gitignore:15` 的 `src/test/` 忽略；显式测试例外要求保留测试。父级需把 Cloud test retention
  作为交付/最终提交门处理，不能只凭本地 ignored 文件存在认定已持久化。

## 8. 依赖与父级快速审查顺序建议

1. 先冻结本报告第 4 节的两文件 SHA；若 delivery 后 SHA 再变，要求 External C 在原卡追加新的 repair delivery 与 SHA，
   旧审查不跨字节复用。
2. 先做 test-source compileability review：修正包名、public constructor、metadata identity/rect 与两条冲突断言。
   这些缺口独立于 shared Cloud main compile blocker。
3. 再审 production exact-context mismatch 候选和 no-context holder gate；以 wrong-scope/missing-holder 零 action 为验收点。
4. 再补齐原卡明确承认缺失的 action-capable/四 caller/enter-exit-recovery/maintenance 矩阵；只在同一个 named test 内完成，
   不扩大 production/test write set，不新增 test hook 或复制 reducer。
5. External C 新 true EOF repair delivery 后，父级分别做 production source review 与 assertion review，再交两名非实现者
   独立 review。
6. 所有 Java writer 稳定后，才由父级运行点名 test 和 Cloud compile/build；本 helper 未运行也不主张任何结果。
7. 当前权威计划 registry `:1149` 仍写 `EXTERNAL-C RESUMED / UNIQUE OWNER ACTIVE / 当前不是 source pass`，而原卡
   已正式 delivery。父级审查后再同步 registry；本 helper 不改计划或卡状态。

当前建议优先级汇总：`Potential P0=1`（exact context 混用，待父级确认）、`Potential P1=5 组`
（holder gate、testCompile、非法 context fixture、基线冲突断言、required matrix/retention 缺失）、`Potential P2=1`
（registry 与原卡 delivery 状态暂时不同步）。这只是 preflight index，**非父级批准、非 reviewer finding、非阻断裁决**。

## 9. 报告落盘后的原卡 true EOF 漂移（只读复核）

在本报告初稿落盘后，TURN-34A 原卡
`docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34A.md:326-377` 又由父级追加了正式 Review #1；
这不是两份交付源码的中途变化。`2026-07-16T08:18:49.8551365-04:00` 复核结果如下：

- production 仍为 SHA-256
  `532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9`，mtime
  `2026-07-16T06:29:17.7816908-04:00`，46,414 bytes / 852 行；未漂移。
- named test 仍为 SHA-256
  `5E2CA53F90CB25B29016ECDF7A9AA8702B0AB100A56D64B7D0D9A3D072F29361`，mtime
  `2026-07-16T08:01:17.2202247-04:00`，30,885 bytes / 611 行；未漂移。
- 原卡现为 SHA-256 `3B7553626C1800BFA9BFFE115F361A90CAC4589B8431468922B055AD600AFAE5`，mtime
  `2026-07-16T08:17:58.4445300-04:00`，34,288 bytes / 377 行。
- 原卡 `:332-370` 的父级正式结论是
  `P0/P1/P2=0/1/0 / PRODUCTION SOURCE REVIEW PASSED / TEST SOURCE REPAIR #1 REQUIRED`；production SHA 保持只读，
  External C 仅对同一个 named test 返修并继续持有 owner。该结论由父级写卡，本 helper 只转录最新状态，**非父级批准**。
- 原卡当前物理尾行 `:377` 是：
  `<!-- TRUE_EOF: TURN-34A PARENT REVIEW-1 REPAIR-1-REQUIRED P0P1P2=0/1/0 PRODUCTION-SOURCE-PASSED TEST-SOURCE-BLOCKED EXTERNAL-C-RETAINS-OWNER 2026-07-16T08:17:00-04:00 -->`

因此，第 5-8 节保留的是父级 Review #1 到达前的 preflight 候选与缺证据索引；当前正式卡状态以上述 `:326-377`
为准。源码/test SHA 未变不代表 Repair #1 已交付；只有 External C 依卡 `:370` 再追加新的
`REPAIR #1 SOURCE+TEST DELIVERED` true EOF 与最终 SHA，才构成下一次可审交付。

PRECHECK_COMPLETE

<!-- TRUE_EOF: CR271 TURN-34A DELIVERY PREFLIGHT HELPER R2 PRECHECK_COMPLETE Leibniz 019f6acc-877b-7ea1-9de7-b5d7e52043f5 2026-07-16T08:18:49.8551365-04:00 NON_PARENT_APPROVAL -->
