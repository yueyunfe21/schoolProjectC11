# TURN-38B1 - Bag turn-native state owner

## Canonical status

- Status: `READY / ZERO OWNER / WHOLE-CARD SOURCE+TEST`
- Predecessors: TURN-14 source gate and TURN-38A-F foundation gate are satisfied.
- Claim: an available Worker may claim this whole card only by appending a canonical claim at this
  file's physical EOF after rechecking hashes, status and collisions. The ledger does not assign it.
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` and the accepted TURN-14 HTTPS Bag contract.
- Business difference: `无已批准业务差异；按基线等价迁移`.

## Exact write set

Cloud production, and no other production files:

1. `src/main/java/com/bot/dhxy/service/bag/CloudBagStateOwner.java`
2. `src/main/java/com/bot/dhxy/service/bag/BagWorkflowState.java`

Cloud test, exactly one file:

1. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/bag/BagWorkflowStateTurnTest.java`

Protected starting bytes:

- `CloudBagStateOwner.java`: SHA-256 `EAFA0D7E4B98C6545A954867629603D402F3EBB10B4CC497F0130A24C4396AC1`,
  mtime `2026-07-13T18:54:19.2912107-04:00`, untracked.
- `BagWorkflowState.java`: SHA-256 `34EAD25E28BD640BDAEDCB51840940CA4D3009B896343A078FA981AD2BE5FFD8`,
  mtime `2026-07-13T18:54:47.9870475-04:00`, untracked.

Any required edit outside this set is a canonical plan-contract return, not permission to expand.

## Frozen production contract

1. `CloudBagStateOwner` becomes one explicitly constructible, scope-bound owner for one
   `CloudServiceScope`. Its public construction seam is used directly by the named test and later
   by TURN-40B assembly. This card does not edit Spring configuration or create runtime wiring.
2. The owner derives the exact `CloudServiceScope`, `TurnInvocationContext` and `taskRunId` from
   `TaskExecutionContext`. A context from another tenant/user/device/window is rejected with zero
   mutation. Remove all `RemoteTaskRun*`, `CloudTaskServicePort`, `revalidate()` and old retained
   authority/permit/ledger dependencies from both files.
3. The owner holds the only workflow map. The workflow key is exact
   `(tenant,user,device,window,taskRunId)`. Repeated lookup in the same run returns the same
   `BagWorkflowState`; pause/resume keeps it. A restart with a new taskRunId receives fresh state.
4. Expose one explicit exact-run terminal release surface for TURN-40B. SUCCESS, FAILED and STOPPED
   all release only that workflow state; replay is idempotent. A stale/foreign context cannot clear
   another run. No task-terminal event clears page/item/anchor/geometry caches.
5. Visible-page and item-page hints are keyed by owner scope + logical device/window + layout and
   remain first-scan hints only. They survive task terminal/restart and have no TTL/LRU/age rule.
6. Bound-base geometry and MAIN_BAG anchor additionally bind the exact native title/handle/process
   generation. Native identity or geometry drift invalidates only geometry-derived anchor state;
   it does not erase visible/item hints or become a new business fact.
7. Owner close/scope teardown clears only this scope's workflow and Bag caches. It is the host-close
   boundary later called by TURN-40B; no caller-addressable cross-scope removal API is allowed.
8. `BagWorkflowState` retains only workflow cursors and owner-issued operation identities. Preserve
   transaction/open/page/session/frozen-point ordering and idempotence. Do not add a second registry,
   session, ledger, durable restore, scheduler, TTL, retry, extra read/verification or cleanup rule.

The production construction and lifecycle API must be direct and reviewable. Do not add wrapper
chains or a second facade merely to rename owner methods.

## Named-test acceptance

The sole named test must traverse the real public construction/lookup/release surfaces and cover:

1. same-run pause/resume returns the same workflow with all cursors preserved;
2. SUCCESS/FAILED/STOPPED terminal release is exact and idempotent, then restart is fresh;
3. tenant/user/device/window/taskRun isolation and stale/foreign zero mutation;
4. visible/item hints survive task terminal and restart without TTL;
5. native identity/geometry drift invalidates only geometry-derived anchors;
6. owner close clears only its bound scope;
7. old `RemoteTaskRun*`, permit/session/ledger authority is absent from the production path.

No reflection, source-string guard, copied reducer, runtime/application/server, capture or input.
Named test and applicable Cloud compile remain the post-delivery build gate and must not run while a
Java writer is active.

## Collision and handoff

- Current physical intersection with TURN-38B2/B3/B4, TURN-38C and active A/C work is empty.
- TURN-40B consumes this card's public construction and terminal/close surfaces; 38B1 must not
  implement the runtime factory, queue, host lifecycle or Task wiring itself.
- TURN-38A-C remains deferred cleanup and does not block this card. This card removes its own old
  authority references now; 38A-C later deletes only caller-zero compatibility residue.

<!-- TRUE_EOF: TURN-38B1 READY ZERO-OWNER WHOLE-CARD SOURCE-TEST CONTRACT-FROZEN 2026-07-18T00:41:00-04:00 -->

## EXTERNAL-C TURN-38B1 WHOLE-CARD CLAIMED - 2026-07-18T00:47:00-04:00

- Implementation Worker：**CR271 External Worker C**（本 lane 已完成 TURN-27/36/37 whole-card 全 PASSED 收官；TURN-37 于 23:46 Review#2 0/0/0 PASSED+OWNER RELEASED 后 IDLE）。非 reviewer，不自批；父级为唯一 manager/final reviewer。
- 领取时间：`2026-07-18T00:47:00-04:00`（=append 时刻）。响应 `PARENT-TURN38B1-READY-0041`（TO-A,C，非派卡，自行竞领）。
- **防竞态预检证据**（预检与本 append 两次独立调用）：卡 physical EOF=`READY / ZERO OWNER`（00:41 冻结块，5 sections 无任何 claim）；ledger 10310 行 EOF=父级 READY 消息块，无 A claim 事件；append 后立即回读 EOF 复核唯一性，若发现更早 claim 立即 canonical 自撤。
- **领取点重取证**（实测）：`CloudBagStateOwner.java` SHA-256 `EAFA0D7E4B98C6545A954867629603D402F3EBB10B4CC497F0130A24C4396AC1`/717L、`BagWorkflowState.java` SHA-256 `34EAD25E28BD640BDAEDCB51840940CA4D3009B896343A078FA981AD2BE5FFD8`/856L——与卡 protected starting bytes 逐字一致，untracked 未漂移；唯一 test `BagWorkflowStateTurnTest.java` 当前不存在（与卡一致）。
- **前置阅读**（本会话实测完成）：原卡全文（85L 五节）；`AGENTS.md`（392L 全文，含 2A 业务基线门/编译门/测试例外/checkpoint 规则）；`docs/DHXY_CONTEXT.md` CR271 计划+placement 决策节（BagService=DHXY-local 永久 Service、Cloud 只经 closed typed contract、runtime 不得加 ledger/session/owner/durable workflow）；`docs/业务逻辑.md` 全表扫描——**实测 0 条背包/bag 基线行**，本卡行为权威=卡内 frozen production contract + TURN-14 已接受 HTTPS Bag 合同 + `696a12b0`，无已批准业务差异；按基线等价迁移。
- **唯一完整写集（不增不减）**：Cloud `src/main/java/com/bot/dhxy/service/bag/CloudBagStateOwner.java` + `src/main/java/com/bot/dhxy/service/bag/BagWorkflowState.java`（Modify）；Create Cloud test `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/bag/BagWorkflowStateTurnTest.java`；本卡 append-only。写集外任何必需改动=canonical plan-contract return，不自扩。
- **合同要点确认**（8 条 frozen contract + 7 条 named-test 验收全收悉）：单 scope-bound 显式可构造 owner（不动 Spring 配置/不建 runtime wiring）；exact `(tenant,user,device,window,taskRunId)` workflow key；foreign/stale context 零变更拒绝；删全部 `RemoteTaskRun*`/`CloudTaskServicePort`/`revalidate()`/旧 authority/permit/ledger 依赖；SUCCESS/FAILED/STOPPED 仅释放 workflow、replay 幂等、不清 page/item/anchor/geometry 缓存；visible/item hints 无 TTL/LRU 跨 terminal/restart 存活；native identity/geometry drift 仅失效 geometry-derived anchor；owner close 仅清本 scope 且为 40B host-close 边界、禁跨 scope removal API；不加第二 registry/session/ledger/durable restore/scheduler/TTL/retry/额外读验/cleanup；禁 wrapper 链/第二 facade；test 禁反射/source-string guard/copied reducer/runtime/capture/input。
- **冲突检查**：与 38B2/B3/B4/38C、A/C 活跃写集物理零交集（卡 77-83 行父级已证）；40B 消费本卡 public construction/terminal/close 面，本卡不实现 runtime factory/queue/host lifecycle/Task wiring；38A-C 为后置 cleanup 不阻本卡。
- **纪律**：零 Git mutation；`D:\mavenProject\DHXY` 只读；不运行 runtime/UI/capture/input；其它 Java writer 活动时不运行 Maven（javac 单文件 parse 除外）；named test+Cloud compile=交付后 build gate；不自批、不建 reviewer；heartbeat `778801ea` 切本卡监控。

TRUE_EOF

<!-- TRUE_EOF: TURN-38B1 EXTERNAL-C WHOLE-CARD CLAIMED OWNER-C PROTECTED-BYTES-VERIFIED EAFA0D7E+34EAD25E TEST-ABSENT DOCS-READ ANTI-RACE-PRECHECKED ACK=PARENT-TURN38B1-READY-0041 2026-07-18T00:47:00-04:00 -->

## PARENT AMENDMENT #1 - 2026-07-18T01:01:00-04:00

- C 01:00 recon 中“owner 侧同删 permit/handle/sequence-fence”的解释不获批准。合同 #2 删除的是
  `RemoteTaskRun*`、`CloudTaskServicePort`、`revalidate()` 与依赖旧 authority 的 non-mintable permit/ledger，
  不是授权删除业务所需的 owner-issued observation identity、custody 与 `< / == / >` sequence fence。
- Bound-base observation 必须保留 turn-native owner-issued typed handle 与单调 sequence：旧序列零变更拒绝、同序
  同值幂等、同序异值拒绝、新序只在完整校验后原子推进。它不得成为 session/ledger，也不得铸造业务许可；
  只负责关联一次观测结果并防止旧 geometry 覆盖新 geometry。
- `BagWorkflowState` 继续保留 pending observation custody，并以 exact handle/reference 进行幂等 clear；不得改成
  裸坐标“最后写入获胜”。named test 增加 out-of-order、same-sequence replay/conflict 与 foreign-handle 零变更。
- Native generation 只用卡已冻结且 turn-native context 已提供的 title/handle/process identity；不得新增或猜测
  `identityEpoch`。geometry change 仍仅推进 geometry generation 并失效旧 anchor，不清 visible/item hints。
- 其余 scope-bound owner、workflow terminal/cache lifetime、write set 与 40B handoff 不变。C owner 保持，尚未写
  Java；收到定向 ACK 后再开始 production bytes。

<!-- TRUE_EOF: TURN-38B1 PARENT-AMENDMENT-1 OWNER-C HOLD-FOR-ACK PRESERVE-OBSERVATION-HANDLE-SEQUENCE-FENCE NO-IDENTITY-EPOCH 2026-07-18T01:01:00-04:00 -->

## EXTERNAL-C TURN-38B1 WHOLE-CARD CANONICAL DELIVERY - 2026-07-18T01:18:00-04:00

TURN-38B1（Bag turn-native state owner）**交付源审 + build**。sole owner C；Amendment#1（`PARENT-TURN38B1-AMENDMENT1-0101`）已 01:04 具名 ACK 且父级 01:04:30 确认 gate 开后实施。零 Git mutation。

### A. 写集（不增不减，3/3）
| 文件 | 起始（protected） | 交付 blob / SHA-256 / 行数 |
|------|------------------|---------------------------|
| `CloudBagStateOwner.java` | `EAFA0D7E...`/717L | `3e780efb` / `C87F909E83EA513B24348EB2263AAC9E39326FDBA90B17B104C44FD12AF68380` / 756L |
| `BagWorkflowState.java` | `34EAD25E...`/856L | `705b9391` / `A3066E5AD66AAE0D6A743BDA437C9161FC38E9DE59C7C65D5008EE1AF3BE9DC8` / 674L |
| `BagWorkflowStateTurnTest.java`（Create） | ABSENT | `638e6a86` / `CED1089D2FB4A64B583BA8FD424B37B48F4B203F3114911AC13DBD37D0A4CCA4` / 471L / **13 @Test** |

旧 authority 残留实测：两 production 文件 `RemoteTaskRun*/CloudTaskServicePort/revalidate/Permit/RunRevision/StopEpoch` = **0**；test 唯一命中为 javadoc 验收项 7 叙述（非代码依赖）。

### B. 8 条 frozen contract 落地
1. public `CloudBagStateOwner(CloudServiceScope boundScope)` 构造 seam + `boundScope()`；不动 Spring/runtime wiring。
2. 全部业务面从 `TaskExecutionContext` turn-native 派生（`getTurnServiceScope/getTurnInvocationContext/getTaskRunId`）；foreign tenant/user 零变更拒（progress throw / finally-path false）；旧 authority 依赖两文件全删。
3. 唯一 workflow map，key=exact `(tenant,user,device,window,taskRunId)` record；`workflowOf` computeIfAbsent 同 run 同实例、pause/resume 保留、新 taskRunId 全新。
4. 单一 `releaseWorkflowOnTerminal(context, SUCCESS/FAILED/STOPPED)`：三终态同一释放、replay 幂等 false、null/foreign finally-safe 零变更、key 自 context 派生故 stale/foreign 不能清他 run、不触 page/item/anchor/geometry 缓存。
5. hints key=(device,window,layout[+canonical templateId 校验])、0..4 栅栏（页 5 拒）、无 TTL/LRU/age、跨 terminal/restart 存活。
6. geometry stream 绑 `NativeWindowIdentity(title,nativeHandle,processId)` 三元（**无 identityEpoch**，Amendment#1）；native drift 分支=仅推进 generation+清旧代 anchor+清 acceptedBase 后 rebind，hints 不动；anchor read/write 双绑 current generation+current identity。
7. 无参 `close()`=host-close 边界，仅清本 owner（workflow+全 Bag 缓存），幂等；无任何跨 scope removal API。
8. state 仅 workflow cursors+state-issued handle（`WorkflowTransactionHandle/OpenFlowHandle/PagePassHandle` 替代双 permit）：transaction（begin 幂等返 live/新事务清 frozenPoints+ordinal 重置/finish 前子流程须闭合）、open 4-stage 一步推进+已达 stage 幂等、page/sessionOp advance 携 exact 已消费 index/ordinal（-1 幂等/current 推进/其它 fail-closed）、frozen-point 12 slot 一次冻结；custody（Amendment#1）：CAS record+owner `describesExactRun` 装前校验+exact-handle 幂等 clear；无第二 registry/session/ledger/durable restore/scheduler/TTL/retry/额外读验/cleanup；无 wrapper 链。

### C. Observation fence（Amendment#1 保留面）
`beginBoundBaseObservation(context)`→owner-issued typed handle（ownerInstanceId+五元组+native 三元+单调 sequence，incrementExact）；`acceptBoundBaseObservation(handle,context,base)` 完整 `</==/>`：stale 序零变更拒/同序同值幂等/同序异值拒/新序**先算后写**原子推进（overflow 不半更）；foreign handle（异 owner）直接 throw、异 run/window/漂移 identity→`DISCARDED_FOREIGN_OR_STALE_CONTEXT` 零变更。非 session/ledger、不铸业务许可。

### D. 唯一 test（13 @Test，全走真实 public 面，无反射/source guard/copied reducer/runtime/capture/input）
7 验收全覆盖：①pause/resume 同实例+全 cursor（tx/stage/pass+cursor/ordinal/frozen-point 不重抽）②三终态逐一 exact+幂等+restart 全新+stale/foreign/null 不能清他 run ③五元组隔离（window/device/taskRun 各异实例）+foreign scope 全 owner 面零变更拒+state 级 progress throw/finally false ④hints 跨 SUCCESS terminal+新 taskRunId restart 存活+页 5 拒 ⑤geometry drift 仅清 anchor 不清 hints+native identity drift 仅清 geometry-derived+stale identity anchor write fail-closed ⑥双 owner close 仅本 scope+幂等 ⑦全生命周期仅凭 turn-native context 完成=结构性证明无旧 authority（javadoc 明载）。Amendment#1 3 case：out-of-order stale 拒+零变更探针、same-seq replay 幂等+conflict 拒+零变更探针、foreign-handle/context fence+custody 全负例矩阵。

### E. 验证与纪律
三文件单文件 `javac -proc:none -implicit:none -sourcepath "" -cp ""` 零纯语法错。**未运行 Maven**（C sole active writer；named test+Cloud compile=交付后 build gate，且模块受 S1-S3 shared debt 阻断至 40B）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；零 Git mutation；不自批。请求：**whole-card source review + build gate 登记**。

<!-- TRUE_EOF: TURN-38B1 EXTERNAL-C WHOLE-CARD-CANONICAL-DELIVERY OWNER=3e780efb STATE=705b9391 TEST=638e6a86 13-TESTS 8-CONTRACT-CLAUSES-LANDED AMENDMENT1-FENCE-KEPT ZERO-AUTHORITY-RESIDUE REQUEST-SOURCE-REVIEW OWNER-C NO-MAVEN 2026-07-18T01:18:00-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST SOURCE REVIEW #1 - BLOCKED - 2026-07-18T01:26:00-04:00

结论：`P0/P1/P2=0/1/2`，`SOURCE+TEST SOURCE REVIEW BLOCKED / REPAIR REQUIRED`。三文件写集与交付
SHA 均核实无外溢；旧 `RemoteTaskRun*`、`CloudTaskServicePort`、`revalidate()`、permit/ledger 与
`identityEpoch` production 残留为零。以下三项必须在同一整卡返修并重新 canonical delivery：

1. **P1 - `BagWorkflowState.java:92-105` 的 public constructor 绕过唯一 workflow map。** 任意 caller
   都可 `new BagWorkflowState(owner, same five-tuple)`，为同一 run 建立不在 `CloudBagStateOwner.workflows`
   中的第二个 cursor/handle state；task terminal 与 owner map lookup 均无法证明或释放该副本。这与冻结合同
   #3“owner holds the only workflow map / same run single state”冲突。返修：构造器收窄为 owner 同包可见，
   只允许 `CloudBagStateOwner.workflowOf(...)` 铸造；不得增加 factory/facade/第二 registry。
2. **P2 - `CloudBagStateOwner.java:294-325` 的 native-drift accept 不是原子提交。** 代码先在
   `:297-301` 写 generation/base/identity 并删 anchor，随后又在 `:313-315` 对已推进 generation 做第二次
   `incrementExact`。这会让一次 identity drift + outcome 把 generation 推进两次；在边界溢出时，第二次
   increment 抛出而前半段已经落盘，直接违反 Amendment #1“完整校验后原子推进/overflow 不半更”。返修：
   在首个字段写前一次性计算最终 generation、sequence/base/identity 与 anchor 删除条件，然后一次提交；一次
   identity/geometry transition 只推进一个 generation。
3. **P2 - named test 未锁住上述 generation 合同，且 scope 五元组隔离矩阵不完整。**
   `nativeIdentityDriftDropsOnlyGeometryDerivedStateAndNeverTheHints` 只断言返回枚举/null/hint，没有通过新 anchor
   的 `geometryGeneration` 证明 drift 仅 +1；`workflowKeyIsolatesWindowDeviceAndTaskRun` 与 foreign-scope case
   仅把 tenant/user 同时改掉，没有分别证明 tenant-only、user-only 隔离。返修 test：增加 drift 后写/读新 anchor
   并断言 generation 恰好 `before+1`，再增加 tenant-only 与 user-only 的拒绝/零变更矩阵；继续只走 public 面，
   不用 reflection/source guard/copied reducer。

返修边界：仅上述三个固定文件；其它已审通过的 terminal/cache/custody/`< / == / >` fence、13 个既有测试与
无业务差异声明全部保留。C owner 保持；修复前不运行 Maven/runtime/UI/capture/input，返修重交后父级复审，
再决定 named test 与适用 Cloud compile gate。

<!-- TRUE_EOF: TURN-38B1 PARENT-WHOLE-CARD-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=2 OWNER-C CONSTRUCTOR-MUST-BE-OWNER-ONLY NATIVE-DRIFT-ATOMICITY-BROKEN TEST-MATRIX-GAP NO-MAVEN 2026-07-18T01:26:00-04:00 -->

## EXTERNAL-C REVIEW #1 REPAIR / CANONICAL WHOLE-CARD RE-DELIVERY - 2026-07-18T01:34:00-04:00

三项返修全部完成，写集仍 3 文件不增不减；已审通过面（terminal/cache/custody/`</==/>` fence/既有 13 test/无业务差异声明）未动。

### P1 修复 — 构造器收窄 owner-only
`BagWorkflowState` 构造器 `public`→**package-private**（同包唯一铸造方=`CloudBagStateOwner.workflowOf` computeIfAbsent），javadoc 明载「owner 的 workflow map 是唯一 registry」；无 factory/facade/第二 registry 新增。test 本就只经 `workflowOf` 获取 state，无需改动即兼容。

### P2-1 修复 — drift accept 原子化
`acceptBoundBaseObservation` 重排：①stale/replay 判定**先于** drift 处理（旧序/同序在任何 drift 写入前零变更返回）；②acceptance 提交前**一次性**计算 `identityDrift`+`geometryChanged=identityDrift||baseChanged`+`nextGeneration`（单次 `incrementExact`），然后单段提交（fence/base/identity→generation→旧代 anchor 清理）——**一次 identity/geometry transition 恰推进一个 generation**；`incrementExact` 溢出发生在任何字段写之前，stream 零半更。javadoc 同步更新。

### P2-2 修复 — test 补矩阵
①`nativeIdentityDrift...` 增：drift 前经 anchor 读取取 `generationBeforeDrift`，drift 接受后写/读新 anchor 断言 `geometryGeneration()==before+1`（一次 transition 恰 +1）；②新 `@Test tenantOnlyAndUserOnlyScopeMismatchesAreEachRejectedWithZeroMutation`：tenant-only 与 user-only 两独立 mismatch 各自断言 lookup throw+hint write throw+finally release false，末尾以 bound scope 的 workflow assertSame+hint 值双探针证零变更。仍全走 public 面，无反射/source guard/copied reducer。

### 交付取证（全部单文件 parse 零纯语法错）
| 文件 | blob | SHA-256 | 行数 |
|------|------|---------|------|
| `CloudBagStateOwner.java` | `9a85fde1` | `11E7250B135E0446AE14F3070454CFB4A863163F428DB6945C33966A085167BC` | 751L |
| `BagWorkflowState.java` | `9f545028` | `FD7710E0DECF01F14B853290EB2DB4CB4F1A0F01E8C1A997FCC367499E9D2BF9` | 678L |
| `BagWorkflowStateTurnTest.java` | `f4e5cc7f` | `FCD59C5E80236F23935382A22A3AA0A53054C728CA7CCBA0E1A5D2CBC7F2A09A` | 511L / **14 @Test** |

纪律：零 Git mutation；未运行 Maven/runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。请求：**whole-card Review #2**。

<!-- TRUE_EOF: TURN-38B1 EXTERNAL-C REVIEW1-REPAIR CANONICAL-REDELIVERY OWNER=9a85fde1 STATE=9f545028 TEST=f4e5cc7f 14-TESTS CTOR-PKG-PRIVATE DRIFT-ATOMIC-SINGLE-GENERATION TEST-MATRIX-FILLED REQUEST-REVIEW2 OWNER-C NO-MAVEN 2026-07-18T01:34:00-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST SOURCE REVIEW #2 - PASSED - 2026-07-18T04:28:00-04:00

结论：`P0/P1/P2=0/0/0`，`SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`。父级重新核对原三文件
physical bytes、调用面与返修矩阵，Review #1 三项均已闭合，且无写集外 Java 变化：

1. `BagWorkflowState` constructor 已收窄为 package-private；Cloud 全源码唯一 `new BagWorkflowState(...)`
   位于 `CloudBagStateOwner.workflowOf(...)`，同 run state 仍只由 owner 唯一 workflow map 铸造。
2. `CloudBagStateOwner.acceptBoundBaseObservation(...)` 先完成 foreign/stale/replay 判定，再在任何字段写入前
   一次计算 `identityDrift`、`geometryChanged` 与单次 `Math.incrementExact(...)`；随后单段提交 sequence/base/
   identity/generation 并失效旧 anchor。一次 native/geometry transition 恰好 `+1`，overflow 零半更。
3. `BagWorkflowStateTurnTest` 已增 tenant-only、user-only scope mismatch 各自零变更矩阵；native drift case
   通过 drift 前 generation 与新 anchor 断言 exact `before+1`。总计 14 个 `@Test`，继续只走真实 public 面。

复核取证保持：`CloudBagStateOwner.java`=`9a85fde1` / SHA-256
`11E7250B135E0446AE14F3070454CFB4A863163F428DB6945C33966A085167BC` / 751L；
`BagWorkflowState.java`=`9f545028` / SHA-256
`FD7710E0DECF01F14B853290EB2DB4CB4F1A0F01E8C1A997FCC367499E9D2BF9` / 678L；
`BagWorkflowStateTurnTest.java`=`f4e5cc7f` / SHA-256
`FCD59C5E80236F23935382A22A3AA0A53054C728CA7CCBA0E1A5D2CBC7F2A09A` / 511L / 14T。
旧 authority、第二 registry/session/ledger、`identityEpoch` 与业务差异残留仍为零；无已批准业务差异。

通信状态：C 01:35 已明确回执全部 Review #1 finding、完成返修并 canonical re-deliver，故此前
`COMMUNICATION_STALE` 解除。source owner 现释放。

Build gate：父级在 C 停止写入后运行授权命令 `mvn -q -Dtest=BagWorkflowStateTurnTest test`；命令在进入
该 named test 前即被写集外 Cloud main compile 债阻断。代表性缺口包括 `TextCandidateScanStatus`、
`AutomationMetricsService`、`CoordinateHelper`、`TextRecognizer`、`InputActionRequest` 及 Navigation/三 Task
共享 collaborators；输出未指向本卡两 production 或本卡 test。该 build blocker 归 TURN-40B shared debt，
不回退本卡 source review，不要求 C 继续返修。未启动 runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-38B1 PARENT-WHOLE-CARD-REVIEW2 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED COMMUNICATION-RECOVERED NAMED-TEST+CLOUD-COMPILE-BLOCKED-BY-40B-SHARED-DEBT NO-RUNTIME 2026-07-18T04:28:00-04:00 -->
