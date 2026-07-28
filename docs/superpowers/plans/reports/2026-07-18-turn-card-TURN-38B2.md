# TURN-38B2 - ReturnItem turn-native single state owner

## Parent contract freeze / READY - 2026-07-18T04:36:00-04:00

状态：`READY / ZERO OWNER`。本卡不是派卡；有容量且受 lane 规则允许的 Worker 只能在本文件 physical EOF
先 canonical WHOLE-CARD claim，回读确认唯一 owner 后才可写 Java。

### 1. 前置与基线

- start gates：TURN-14、TURN-22、TURN-38A-F 均已通过 parent source review；TURN-35/37 的真实
  `ReturnItemPrescanService` callers 也已 source review passed。B1 已 Review #2 passed，文件零碰撞。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `ReturnItemPrescanService`，叠加 TURN-14
  已通过的 HTTPS Bag local-operation transport 映射。
- `docs/业务逻辑.md` 已核：回程道具成功后只做现有一次起点地图验证；不得新增回程前验证、TTL、二次地图
  验证、retry、park/yield、phase/fallback/时序变化。无已批准业务差异。

### 2. Parent plan-contract repair

旧 precheck 的两 production 文件是 disconnected dormant state-core：owner private constructor、零 factory，
`finishPrescan/completeRound` private；当前 15 个真实 Task call sites 全部只经过
`ReturnItemPrescanService` 自有 `states` map。若只改旧两文件，会保留第二状态权威并且 named test 无法穿过
真实 public path。因此本卡把真实 `ReturnItemPrescanService.java` 纳入 production 写集，要求删除该类自有
map/nested state，并接到唯一 scope-bound owner；Task callers 保持只读，public API 不改。

旧 dormant owner 的 `RemoteTaskRun*`、`revalidate()`、permit/attempt receipt/settlement model、client-coordinate
`PrescanCachePoint` 与 fixed capacity 是未接入 live baseline 的旧设计，不得变成新业务事实。特别是
`GLOBAL_LIMIT/PER_RUN_LIMIT` 若接入会给原本无此上限的 live service 新增 fail-closed 行为，故必须随旧模型
删除；不得替换为 eviction、TTL、LRU、session、ledger、scheduler 或 durable restore。

### 3. 固定写集（Cloud only，4 files）

Production modify：

1. `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
   - protected SHA-256 `FE31D4C9F13C4347639707346088445429737CE106D3C2B04EE7D3890AC5BEE6`
2. `src/main/java/com/bot/dhxy/service/returnitem/CloudReturnItemPrescanStateOwner.java`
   - protected SHA-256 `3E606C3BDCFB2A9F3F56A355B1B34F30BA7CA30298E39AEF58C8442BB1D124E4`
3. `src/main/java/com/bot/dhxy/service/returnitem/ReturnItemPrescanWorkflowState.java`
   - protected SHA-256 `FB6901BB9454776C225A9951F06EAB5E6F5AB280B9F2E08ECB5407F4045BC55D`

Test create：

4. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/returnitem/ReturnItemWorkflowStateTurnTest.java`
   - protected state `ABSENT`

除本原卡 append 外其余全部只读，包括 Wubei/Xiuluo/FiveRing Task、TURN-14 client/model/test、context/holder、
B1/B3/B4、host/config、protocol/POM/resource。若需要第五个 Java 文件，必须 OWNER RETURNED 并由父级修合同。

### 4. 唯一 owner 与 exact key

- `CloudReturnItemPrescanStateOwner` 是 Spring-host 每个 `CloudServiceScope(tenant,user)` 的唯一 owner；公开
  constructor seam 接 exact bound scope，允许 Spring host 与 named test 构造，不创建静态/global registry。
- 唯一 workflow map key 至少为 exact
  `(tenant,user,device,window,taskRunId,taskCode,round,canonicalTemplate)`；key 全从
  `TaskExecutionContext.getTurnServiceScope/getTurnInvocationContext/getTaskRunId` 与现有 public method 参数派生。
- `ReturnItemPrescanWorkflowState` constructor package-private，只由 owner map 铸造；同 exact key 同实例，
  pause/resume 保留，foreign/null/scope/device/window/run mismatch 零变更且不得触发 Bag action。
- workflow state 只承接当前 live service 已有字段：mode、maxBackPage、一次选定 strategy、inProgress、done、
  combatFallback、combatDueAtMs 与 exact `ReturnItemCachePoint`。不得复制 service 业务算法或造第二 map。

### 5. 业务等价边界

- `ReturnItemPrescanService` 的七个现有 public Task-facing 方法签名、返回值与调用顺序不变：
  `afterTrackerGreen`、`afterTrackerGreenRequired`、`whilePathing`、`whileInCombat`、`useCached`、
  `invalidate`、`completeRound`。
- strategy candidate/order/draw count 不变；combat due 仍只在首次到达现有分支时计算一次
  `4000 + random[8000,18000]ms`，pause/resume 不重抽、不重算。不得增加 clock/expiry gate。
- TURN-14 transport 保持：每次明确 prescan/use action恰一 UUID、一 command、无 retry；`NOT_EXECUTED`
  保持现有 null/false+fallback/invalidate 语义，STOPPED/UNCERTAIN 继续按现有 checkpoint/fatal unwind，不能
  伪装成功或自动重发。不得增加现有 action 之外的 checkpoint/read/verification。
- cache payload 必须原样保存 TURN-14 `ReturnItemCachePoint(clickX,clickY,templatePath,learnedAtMs,source)`，
  坐标为 screen-absolute；`learnedAtMs=1` 仍有效。不得改为 client coordinates、geometry generation 或 age gate。
- `completeRound` 仍只在现有 Task 已完成一次回程起点地图验证后删除 exact round/template state；不增加验证，
  missing exact state 幂等 no-op，不能清其它 run/round/template。
- 新增唯一 lifecycle seam `releaseWorkflowOnTerminal(exactContext, SUCCESS|FAILED|STOPPED)`，三终态只释放
  exact taskRun 的 workflow state，foreign/stale/null finally-safe 零变更；真实 terminal assembly 属 TURN-40B。
  owner `close()` 仅用于 host close 清本 scope，幂等；不做后台 cleanup。

### 6. Named test gate

唯一 test 必须走真实 public `ReturnItemPrescanService` + owner boundary，使用 scripted
`CloudBagLocalServiceClient`/turn context；禁止 reflection、source guard、copied reducer、private helper direct test、
sleep race、runtime/capture/input。至少覆盖：

1. required prescan -> pause/resume continuity：同 state、exact cache/strategy/due/fallback，不额外 action。
2. tenant-only、user-only、device-only、window-only、taskRun-only 隔离，foreign/null 零状态/零 UUID/command。
3. prescan COMPLETED FOUND/NOT_FOUND、NOT_EXECUTED、STOPPED、UNCERTAIN 各自现有映射与一次 action。
4. cached-use USED/NOT_USED/NOT_EXECUTED/STOPPED/UNCERTAIN 映射；失败只走现有 invalidate，零重发。
5. `learnedAtMs=1` 的 exact five-field cache 跨任意 pause/时间仍使用，不新增 TTL/geometry expiry。
6. `completeRound` exact removal/missing idempotence；三 terminal exact run release、foreign release 零变更、restart fresh。
7. public service 不再持第二 map；owner close 只清本 scope且幂等。该项以行为/API断言为主，源码零残留由
   parent source review 完成，不得写 source-string scan。

授权命令：`mvn -q -Dtest=ReturnItemWorkflowStateTurnTest test`

交付后由父级先 source+test review；Java writer 稳定后才运行 named test 与适用 Cloud compile。禁止
runtime/application/server/Task/UI/capture/input，禁止 Git mutation。

### 7. Canonical claim / delivery

- Claim：在本文件 EOF append `EXTERNAL-<lane> TURN-38B2 WHOLE-CARD CLAIMED`，写明四文件 SHA/ABSENT、容量、
  collision scan；回读确认无竞争后才写 Java。
- Delivery：四文件一次 whole-card canonical delivery，列 blob/SHA/mtime/line/test count、旧 authority 与第二
  map residue、696/TURN-14 对照、无业务差异声明；不自批、不冒充 build。

<!-- TRUE_EOF: TURN-38B2 PARENT-CONTRACT-FREEZE READY ZERO-OWNER FOUR-FILE-WRITESET LIVE-SERVICE-INCLUDED SINGLE-SCOPE-OWNER NO-SECOND-MAP NO-CAPACITY-TTL-RETRY BUSINESS-EQUIVALENT 2026-07-18T04:36:00-04:00 -->

## EXTERNAL-C TURN-38B2 WHOLE-CARD CLAIMED - 2026-07-18T04:47:00-04:00

- Implementation Worker：**CR271 External Worker C**（本 lane TURN-27/36/37/38B1 四整卡全 PASSED 收官；38B1 于 04:28 Review#2 0/0/0 PASSED+OWNER RELEASED 后 IDLE）。非 reviewer，不自批；父级为唯一 manager/final reviewer。
- 领取时间：`2026-07-18T04:47:00-04:00`（=append 时刻）。容量：`AVAILABLE`（不持其它卡）。
- **防竞态预检证据**（预检与本 append 两次独立调用）：卡 106L physical EOF=04:36 父级 freeze 块，全卡唯一 "CLAIMED" 字样=第 7 节 claim 模板文字，**无实际 claim**；ledger 10787 行 EOF=A 04:42 IDLE（其扫池早于本卡落盘，且 A lane 规则明示 38 族严禁领取）；append 后立即回读 EOF 复核唯一性，发现更早 claim 立即 canonical 自撤。
- **领取点重取证**（实测逐字一致）：
  1. `ReturnItemPrescanService.java` SHA-256 `FE31D4C9F13C4347639707346088445429737CE106D3C2B04EE7D3890AC5BEE6` / 394L
  2. `CloudReturnItemPrescanStateOwner.java` SHA-256 `3E606C3BDCFB2A9F3F56A355B1B34F30BA7CA30298E39AEF58C8442BB1D124E4` / 1141L
  3. `ReturnItemPrescanWorkflowState.java` SHA-256 `FB6901BB9454776C225A9951F06EAB5E6F5AB280B9F2E08ECB5407F4045BC55D` / 206L
  4. test `ReturnItemWorkflowStateTurnTest.java` = ABSENT（与卡一致）
- **collision scan**：写集与 B1（已收官）/B3/B4/38C/Task 文件/TURN-14 client/model/context/holder/host/config/protocol 零交集；本卡外全部只读；需第五 Java 文件即 OWNER RETURNED 请父级修合同，不自扩。
- **合同收悉**（全 7 节）：①live `ReturnItemPrescanService` 入写集=删自有 `states` map/nested state 接唯一 scope-bound owner，7 个 public Task-facing 方法签名/返回/顺序不变（afterTrackerGreen/afterTrackerGreenRequired/whilePathing/whileInCombat/useCached/invalidate/completeRound），Task callers 只读；②旧 dormant 模型（RemoteTaskRun*/revalidate/permit/attempt receipt/settlement/client-coordinate PrescanCachePoint/GLOBAL_LIMIT+PER_RUN_LIMIT capacity）随删，**不得**变成新业务事实或换成 eviction/TTL/LRU/session/ledger/scheduler/durable restore；③owner=每 CloudServiceScope 唯一、public ctor seam；workflow key≥exact (tenant,user,device,window,taskRunId,taskCode,round,canonicalTemplate) 全从 turn-native context+现有 public 参数派生；state ctor package-private 仅 owner 铸造、同 key 同实例、pause/resume 保留、foreign 零变更且不触发 Bag action；state 只承接现有字段（mode/maxBackPage/一次 strategy/inProgress/done/combatFallback/combatDueAtMs/exact ReturnItemCachePoint）；④业务等价：strategy candidate/order/draw 不变、combat due 首达现有分支一次 `4000+random[8000,18000]ms` 不重抽、TURN-14 transport 每 action 恰一 UUID 一 command 零 retry、NOT_EXECUTED 保持 null/false+fallback/invalidate、STOPPED/UNCERTAIN 走现有 checkpoint/fatal unwind、cache 原样五字段 screen-absolute `learnedAtMs=1` 有效、completeRound 仅现有一次起点地图验证后 exact 删除+missing 幂等；⑤新增唯一 `releaseWorkflowOnTerminal(exactContext,SUCCESS|FAILED|STOPPED)` finally-safe+owner `close()` 仅 host close 本 scope 幂等；⑥test 走真实 public service+owner boundary+scripted `CloudBagLocalServiceClient`/turn context，7 项 gate（含五维隔离/五终态映射/learnedAtMs=1 无 TTL/exact removal/无第二 map 行为断言），授权命令 `mvn -q -Dtest=ReturnItemWorkflowStateTurnTest test`（交付后 build gate）。
- **基线**：`696a12b0` ReturnItemPrescanService + TURN-14 已过审 transport 映射；`docs/业务逻辑.md` 父级已核（回程道具成功后仅现有一次起点地图验证）；无已批准业务差异。
- **纪律**：零 Git mutation；`D:\mavenProject\DHXY` 只读；不运行 runtime/UI/capture/input；其它 Java writer 活动时不运行 Maven（javac 单文件 parse 除外）；不自批、不建 reviewer；heartbeat `778801ea` 切本卡监控。

TRUE_EOF

<!-- TRUE_EOF: TURN-38B2 EXTERNAL-C WHOLE-CARD CLAIMED OWNER-C PROTECTED-BYTES-VERIFIED FE31D4C9+3E606C3B+FB6901BB TEST-ABSENT ANTI-RACE-PRECHECKED CAPACITY-AVAILABLE 2026-07-18T04:47:00-04:00 -->

## EXTERNAL-C TURN-38B2 WHOLE-CARD CANONICAL DELIVERY - 2026-07-18T05:19:00-04:00

TURN-38B2（ReturnItem turn-native single state owner）**交付源审**。sole owner C；零 Git mutation。

### A. 写集 4/4（不增不减）
| 文件 | 起始（protected） | 交付 blob / SHA-256 / 行数 |
|------|------------------|---------------------------|
| `ReturnItemPrescanService.java`（Modify） | `FE31D4C9...`/394L | `82576ac0` / `25979FF92124B8C06C5842DF89EDBB0D2B6B04DBA780A8CFEED6D11E0E393B7C` / 353L |
| `CloudReturnItemPrescanStateOwner.java`（Modify） | `3E606C3B...`/1141L | `9828e088` / `9D6F6AFA0EE63A45D2837D934779EF7EA56A70C45D22687FF46977BAB5E39F46` / 275L |
| `ReturnItemPrescanWorkflowState.java`（Modify） | `FB6901BB...`/206L | `a8ccf11a` / `FD6A6B4B94426C44247ED1225B95DA3D0C96D95896D1C4A18174149FD7AA6533` / 134L |
| `ReturnItemWorkflowStateTurnTest.java`（Create） | ABSENT | `521a1f4e` / `6E8E58502AE21BCFC11CC13DD4841931F0E6B364D329013BB3E1A33EF537EADC` / 579L / **11 @Test** |

四文件单文件 `javac -proc:none -implicit:none -sourcepath "" -cp ""` 零纯语法错。

### B. 残留矩阵（实测 grep）
旧模型 `RemoteTaskRun*/revalidate/Permit/Receipt/Settlement/GLOBAL_LIMIT/PER_RUN_LIMIT/PrescanCachePoint` 三 production 文件 = **0**；service 内 `ConcurrentHashMap/PrescanKey/PrescanState/states.` = **0**（3 grep 命中均为 `CloudReturnItemPrescanStateOwner` 类名子串）。无 eviction/TTL/LRU/session/ledger/scheduler/durable restore/后台 cleanup。

### C. 合同落地（§4/§5 逐条）
- **owner**：public `CloudReturnItemPrescanStateOwner(CloudServiceScope)` seam；8 元 `WorkflowKey(tenant,user,device,window,taskRunId,taskCode,round,template)` 全从 turn-native context+public 方法参数派生；无静态/global registry。
- **state**：ctor package-private 仅 `workflowOf` 铸造；同 exact key 同实例、pause/resume 保留；只承接现有字段（keyText/template/mode/maxBackPage/strategy final + inProgress/done/combatFallback/combatDueAtMs/cachePoint volatile）；零业务算法。
- **service**：删自有 states map 接 owner（`workflowOf`/`existingWorkflowOrNull`/`removeWorkflow`）；**7 个 public Task-facing 方法签名/返回/顺序逐字不变**（grep 实测在位）；strategy candidates 顺序+单抽（选择逻辑+日志入 workflowOf supplier=仅创建时执行）；combat due 首达一次 `now+4000+nextLong(8000,18001)` 不重抽不重算；TURN-14 transport 逐字保留（120s/phase-code/3 action-slot/每 action 恰一 UUID 一 command 零 retry/NOT_EXECUTED→null+invalidate/STOPPED+UNCERTAIN→checkpoint 后 TaskFatal）；cache 原样五字段 screen-absolute、`learnedAtMs=1` 有效、无 age gate；completeRound=exact 删除+missing 幂等（owner 返 removed state 保 hadCache 日志）。
- **新 lifecycle seam**：`releaseWorkflowOnTerminal(exactContext, SUCCESS|FAILED|STOPPED)` 释放 exact taskRun 全部 round/template 条目、foreign/stale/null finally-safe 零变更、replay 幂等；`close()` 仅 host-close 本 scope、幂等、无跨 scope API。
- foreign/null/blank 矩阵：progress 路径 fail-closed throw（Bag action 零触发）、query/finally 路径 null/false 零变更。

### D. 唯一 test（11 @Test，真实 public service+owner boundary+真实 CloudBagLocalServiceClient over scripted port；public 3-arg TurnGameClient ctor **无反射**、无 source guard/copied reducer/sleep race/runtime/capture/input）
7 gate 逐条：①required prescan pause/resume（同 state/同 cache 实例/零额外 action）②tenant-only+user-only 零 state 零 UUID/command+null 零变更+device/window/taskRun 各自隔离 ③prescan FOUND/NOT_FOUND/NOT_EXECUTED/STOPPED/UNCERTAIN 现有映射各恰一 action 零重发 ④cached-use USED/NOT_USED/NOT_EXECUTED/STOPPED/UNCERTAIN 映射+失败仅 invalidate 零重发 ⑤learnedAtMs=1 五字段 cache 跨 pause 照用无 TTL ⑥completeRound exact+幂等+三终态 exact-run release+foreign/null 零变更+replay 幂等+restart fresh ⑦无第二 map 行为断言（owner 读见 service 写/owner close 后 service 失 cache 零 action/close 幂等）+combat due 一次计算窗口断言+每 action fresh UUID+exact `BAG_RETURN_ITEM`。源码零残留由父级 source review 完成（未写 source-string scan）。

### E. ⚠️ 写集外 collateral 披露（请父级裁定路由）
既有只读 `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ReturnItemPrescanTurnContractTest.java:598` 以旧 1-arg `new ReturnItemPrescanService(localServiceClient)` 构造 service。本卡合同要求的 owner 注入（@RequiredArgsConstructor 第二 final 字段）使该行在 test-compile 时编译断裂。该文件属"其余全部只读（含 TURN-14 client/model/test）"，C 无权修改；系合同内在结果，非本卡可避免。建议路由：40B/cleanup 链单行补 `new ReturnItemPrescanService(client, owner)`，或父级另行裁定。除此之外无任何写集外影响（Task callers 走 7 个未变签名，均不受影响）。

### F. 基线与纪律
`696a12b0` ReturnItemPrescanService+TURN-14 transport 对照逐段等价；无已批准业务差异（`无已批准业务差异；按基线等价迁移`）。零 Git mutation；未运行 Maven（授权命令 `mvn -q -Dtest=ReturnItemWorkflowStateTurnTest test`=交付后 build gate）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。请求：**whole-card source+test review**。

<!-- TRUE_EOF: TURN-38B2 EXTERNAL-C WHOLE-CARD-CANONICAL-DELIVERY SERVICE=82576ac0 OWNER=9828e088 STATE=a8ccf11a TEST=521a1f4e 11-TESTS 7-SIGNATURES-INTACT ZERO-OLD-MODEL-RESIDUE COLLATERAL-DISCLOSED-OLD-TEST-CTOR-598 REQUEST-REVIEW OWNER-C NO-MAVEN 2026-07-18T05:19:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW - BLOCKED / REPAIR REQUIRED - 2026-07-18T05:24:00-04:00

Verdict: **P0=0 / P1=2 / P2=0**. External C remains the sole whole-card owner. The business mapping,
exact eight-part key, single owner map, strategy/due/cache semantics, terminal release selectivity and 11-test
matrix were reviewed with no separate semantic finding. The delivery cannot pass because construction and
lifecycle are not closed end to end.

### P1-1 - Owner is not a Spring-host bean and host shutdown does not own its close boundary

- Evidence: `ReturnItemPrescanService.java:38-52` is a scanned `@Service` with required dependency
  `CloudReturnItemPrescanStateOwner`. `CloudReturnItemPrescanStateOwner.java:41-52` is a plain final class with no
  Spring stereotype. `CloudServiceConfiguration.java:27-40` scans `com.bot.dhxy.service`, and
  `CloudServiceHost.java:52-60` registers the exact `CloudServiceScope` then refreshes that context, but there is
  no owner `@Bean` or component. Once shared main compile debt is cleared, host refresh has no candidate for the
  new required dependency. `CloudReturnItemPrescanStateOwner.java:170-177` also declares an ordinary `close()`
  without `AutoCloseable`, `DisposableBean`, `@PreDestroy`, or configured destroy method, so host close does not
  own the promised map release.
- Repair condition: within the existing owner file, make the owner discoverable as exactly one bean per isolated
  host context using the registered `CloudServiceScope`, and connect `close()` to Spring destruction with a real
  lifecycle contract (for example component + `AutoCloseable`). Do not create a global/static registry, lazy
  context-derived second owner, compatibility stub, or background cleanup. Extend the named test only as needed
  to prove the same bound owner is injected and close remains idempotent/scope-local.

### P1-2 - Fixed write set omitted a retained test deterministically broken by the constructor change

- Evidence: retained
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ReturnItemPrescanTurnContractTest.java:598` constructs
  `new ReturnItemPrescanService(localServiceClient)`, while Lombok now generates only the two-argument required
  constructor. A full no-ignore caller scan found exactly this retained one-argument construction plus the new
  named test's valid two-argument construction. This remains a test-compile failure after shared main compile debt
  is removed; routing that line to 40B would leave B2's own API migration incomplete.
- Repair condition: the parent fixes this original card's write set to include that retained TURN-14 test. Update
  its harness to construct one exact `CloudReturnItemPrescanStateOwner(SERVICE_SCOPE)` and pass it to the service;
  do not add a one-argument compatibility constructor because it cannot safely invent a bound scope and would
  create a second authority. Preserve every existing TURN-14 assertion and action/UUID count.

### Repaired fixed whole-card write set (Cloud only, 5 files)

1. `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
2. `src/main/java/com/bot/dhxy/service/returnitem/CloudReturnItemPrescanStateOwner.java`
3. `src/main/java/com/bot/dhxy/service/returnitem/ReturnItemPrescanWorkflowState.java`
4. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/returnitem/ReturnItemWorkflowStateTurnTest.java`
5. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ReturnItemPrescanTurnContractTest.java`

File 5 repair baseline at review: SHA-256
`1F4BADAB2A2A721895829AC66C2E68EF063F71E67F351457EB8286C5B2063F18`, 837 physical lines, mtime
`2026-07-17T20:29:22.8576677-04:00`. Re-delivery must cover all five files and repeat SHA/mtime/line/test evidence.

### Build evidence

Parent ran the authorized `mvn -q -Dtest=ReturnItemWorkflowStateTurnTest test`. It failed in main compile before
test-compile on the already-recorded TURN-40B shared debt (`TextCandidateScanStatus`, metrics, Bag/UI/input,
Navigation/window/OCR collaborators). This does not add a B2 source finding and does not hide P1-2. No runtime,
server, Task, UI, capture or input was started.

<!-- TRUE_EOF: TURN-38B2 PARENT-SOURCE-TEST-REVIEW BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-C FIVE-FILE-WRITESET SPRING-BEAN-LIFECYCLE-REQUIRED RETAINED-TURN14-TEST-CTOR-REPAIR SHARED-MAIN-COMPILE-DEBT-40B NO-RUNTIME 2026-07-18T05:24:00-04:00 -->

## PARENT COMMUNICATION AUDIT - 2026-07-18T05:34:00-04:00

- External C emitted 05:25 and 05:30 STATUS EVENT keepalives after the parent review message, but both used the
  superseded four-file delivery snapshot and neither ACKed `PARENT-TURN38B2-REVIEW1-REPAIR-0524`.
- Communication state is `COMMUNICATION_STALE`. External C remains sole owner; the five-file repair contract,
  `P0/P1/P2=0/2/0`, source hashes and build-block evidence are unchanged.

<!-- TRUE_EOF: TURN-38B2 PARENT-COMMUNICATION-AUDIT EXTERNAL-C COMMUNICATION_STALE TWO-ROUNDS-NO-ACK MESSAGE=PARENT-TURN38B2-REVIEW1-REPAIR-0524 OWNER-C BLOCKED REPAIR-REQUIRED FIVE-FILE-WRITESET 2026-07-18T05:34:00-04:00 -->

## EXTERNAL-C REVIEW #1 REPAIR / CANONICAL FIVE-FILE RE-DELIVERY - 2026-07-18T05:42:00-04:00

具名 ACK `PARENT-TURN38B2-REVIEW1-REPAIR-0524`+`PARENT-TURN38B2-COMMUNICATION-STALE-0534`（ledger 05:37 已回执，通信恢复；STALE 根因=增量读旧行号锚点跳读，已改为每轮 tail 固定行数）。两 P1 全部返修完成。

### P1-1 修复 — owner 成为 Spring-host bean + close 接 Spring 销毁
`CloudReturnItemPrescanStateOwner` 加 `@Component`（`CloudServiceConfiguration` 扫 `com.bot.dhxy.service` 含子包→每个隔离 host context 恰发现一个 owner bean，其构造参数 `CloudServiceScope` 由该 context 注册的 exact scope bean 注入）+ `implements AutoCloseable`+`close()` @Override（Spring bean destruction 于 host context shutdown 驱动同一 close）；javadoc 明载 lifecycle 契约。无 global/static registry/lazy 第二 owner/兼容 stub/后台 cleanup。**named test 扩展**：新 `@Test theServiceUsesTheExactBoundOwnerAndSpringDestructionDrivesTheSameClose`——assertSame bound scope（同一注入 owner）+ 经 `AutoCloseable` seam 调 close 断言同一 scope-local 清理+重复 close 幂等。
### P1-2 修复 — 保留 TURN-14 test 纳入写集并改造 harness
`ReturnItemPrescanTurnContractTest`（baseline `1F4BADAB...`/837L 起）:598 区 harness 改为构造 exact `new CloudReturnItemPrescanStateOwner(SERVICE_SCOPE)` 并以 2-arg 传入 service（+1 import；注释明载禁 1-arg 兼容路径）；**全部既有 14 @Test 断言与 action/UUID 计数逐字未动**（仅 harness 构造两行+import）。未加 1-arg 兼容构造器。

### 五文件终版取证（全部单文件 parse 零纯语法错）
| 文件 | blob | SHA-256 | 行数 |
|------|------|---------|------|
| `ReturnItemPrescanService.java`（本轮未动） | `82576ac0` | `25979FF92124B8C06C5842DF89EDBB0D2B6B04DBA780A8CFEED6D11E0E393B7C` | 353L |
| `CloudReturnItemPrescanStateOwner.java` | `80a382e5` | `0A8EEC1DAA4B5A75E062ECCD218A6F020B6B3E221C8BA2A4186409A2A290851E` | 284L |
| `ReturnItemPrescanWorkflowState.java`（本轮未动） | `a8ccf11a` | `FD6A6B4B94426C44247ED1225B95DA3D0C96D95896D1C4A18174149FD7AA6533` | 134L |
| `ReturnItemWorkflowStateTurnTest.java` | `6e0f4e24` | `7271EF80C5A5E0A41610AA2A97064BECC701AFD38CAC4EDE7D2E55AA7797FC67` | 598L / **12 @Test** |
| `ReturnItemPrescanTurnContractTest.java`（新入写集） | `ea1304bd` | `F878EB7B070E579A74FC11E9DB15A64915CD100AC20434746F4AD639E415A8FE` | 843L / **14 @Test** |

已审通过面（业务映射/8 元 key/单 owner map/strategy-due-cache 语义/terminal release 选择性/原 11 test 矩阵）未动。零 Git mutation；未运行 Maven（授权命令留 build gate，40B shared debt 已知）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。请求：**Review #2**。

<!-- TRUE_EOF: TURN-38B2 EXTERNAL-C REVIEW1-REPAIR FIVE-FILE-REDELIVERY OWNER=80a382e5 COMPONENT+AUTOCLOSEABLE NAMEDTEST=6e0f4e24-12T RETAINED-TEST=ea1304bd-14T-HARNESS-2ARG SERVICE+STATE-UNCHANGED REQUEST-REVIEW2 OWNER-C NO-MAVEN 2026-07-18T05:42:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - PASSED - 2026-07-18T05:47:00-04:00

Verdict: **P0=0 / P1=0 / P2=0**. Both Review #1 findings are closed.

- P1-1 closed: `CloudReturnItemPrescanStateOwner` is a scanned `@Component`, consumes the exact
  `CloudServiceScope` registered by each isolated `CloudServiceHost`, and implements `AutoCloseable`; its
  idempotent scope-local `close()` is now part of Spring bean destruction. No static/global registry, lazy second
  owner, compatibility seam or background cleanup was introduced.
- P1-2 closed: retained `ReturnItemPrescanTurnContractTest` now creates exactly one
  `CloudReturnItemPrescanStateOwner(SERVICE_SCOPE)` and injects it through the two-argument service constructor.
  Full no-ignore caller scan shows only valid two-argument constructions. Existing 14 test methods and their
  action/UUID assertions remain; named owner test now has 12 tests and covers exact bound owner plus idempotent
  close. Service/state delivery hashes are unchanged from Review #1.
- Business review: exact eight-part key, single map, strategy draw/order, combat due, cache five fields,
  completeRound, terminal release, TURN-14 one-action/no-retry mappings and 696 behavior remain equivalent.
  `无已批准业务差异；按基线等价迁移`.
- Build evidence: parent reran authorized `mvn -q -Dtest=ReturnItemWorkflowStateTurnTest test` against this exact
  five-file terminal source. It again failed in main compile on TURN-40B shared missing collaborators before
  test-compile. No B2 file appears in the compiler findings; build remains blocked without retracting source
  review. No runtime/server/Task/UI/capture/input.

External C communication is recovered and the TURN-38B2 whole-card owner is released.

<!-- TRUE_EOF: TURN-38B2 PARENT-SOURCE-TEST-REVIEW2 PASSED P0=0 P1=0 P2=0 FIVE-FILE-REPAIR-CLOSED OWNER-RELEASED EXTERNAL-C-COMMUNICATION-RECOVERED BUILD-BLOCKED-BY-40B-SHARED-MAIN-COMPILE NO-BUSINESS-DIFFERENCE NO-RUNTIME 2026-07-18T05:47:00-04:00 -->
