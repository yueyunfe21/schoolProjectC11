# CR271 TURN-39 latest source-start readiness helper r2

## 0. 角色、边界与结论

- 角色：CR271 Internal 非绑定 source-start readiness helper；不是 parent、implementation owner 或 reviewer。
- 主审计快照：`2026-07-16T10:44:53.672-04:00`；落笔后竞态复核：`2026-07-16T10:48:46.476-04:00`。共享工作树有其它 lane 持续写入，本文只对列出的 SHA、mtime、物理 EOF 和引用快照负责。
- 唯一写入：本报告。
- 未批准、未领取、未派工；未修改 Java/test/计划/CR271/ACTIVE_WORK/card/dashboard。
- 未运行 Maven、JUnit、compile/package、runtime/application/server/Task/UI/capture/input。
- 未执行 branch switch、add/commit/checkout/reset/clean/stash/rebase/merge 或其它 Git mutation。

**source-start 结论：`NOT READY / NO CLAIM`。**

**External 拆片结论：当前没有一个属于 TURN-39、写集互斥且能 self-unblock 的安全 Java/test 小片。**
六个 TURN-39 production 路径暂时没有同路径活动 writer，只能证明 physical intersection 为零；直接依赖、
写集外 active refs、Cloud `InputSequences` 当前 owner/construction 缺口、metadata production authority 和既有
test ownership 均未闭合。本文不把候选方案写成 READY，也不替父级修订 DAG/write set。

## 1. 权威输入与覆盖口径

已完整读取并按当前磁盘复核：

1. `D:/mavenProject/DHXY/AGENTS.md`。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271；落笔前最新标题仍为 `10:38 A QP1 父级通过并接管无人领取 S2`，但 TURN-28S2 卡在 `10:43:15` 已有更新，见第 5.3 节。
4. 权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节：
   - 覆盖规则始于 `:1068`；
   - 当前 registry `:1212-1219`；
   - 38A/B/C/39 exact write set `:1360-1401`；
   - R5 顺序 `:1488-1491`；
   - named tests `:1704-1710`。
5. HTTPS turn 协议 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`：一个 closed action/一个 outcome、mouse 继续由全局 `InputActionQueue` 串行、无自动 transport/business retry、无 session/ledger；Cloud 决策，DHXY 只执行闭合 mechanics。
6. `docs/业务逻辑.md`，特别是迁移不得新增 TTL、额外 verification/read、retry、park/yield、cleanup、fail-closed truth 或改变 phase/次数/顺序；修罗 authority 为 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，local-team 事实不得由请求/标题推断。
7. TURN-39、全部直接 `startDependsOn` 的最新 fixed report 物理 EOF，以及 38A/38M 的直接支撑报告。
8. `D:/mavenProject/dhxy-cloud-brain` 当前六个 TURN-39 文件、context/holder、Cloud `InputSequences`、protocol metadata/start DTO、old assembly/SCC 引用、B1-B4/38C targets 和现有 tests。

计划 `:1219` 已直接写明 TURN-39 为 `PLANNED / PRECHECK REAL BLOCKERS`，并逐项点名 DAG、active refs、
`InputSequences` owner、metadata authority、test ownership 未闭合。本文只做该行之后的精确 delta，不覆盖它。

## 2. 最新 fixed report 与物理 true EOF

| Scope | 文件 / SHA-256 | 物理末行 | 当前解释 |
|---|---|---|---|
| TURN-39 | `2026-07-16-turn-39-readiness-preflight-helper.md` / `B61FE4460F0D180422D85EED5724614AD76694CC47316BBAF4924AD1BA36843B` | `<!-- TRUE_EOF: CR271 TURN-39 READINESS PREFLIGHT REAL_BLOCKER_CONFIRMED -->` | 396 行；当前 production hashes 仍逐项相同 |
| TURN-38B1 | `2026-07-16-turn-38B1-readiness-preflight-helper-r1.md` / `7D32D1F8E226FA0B94328BC55D96B356F7697D79EE8656E11C9A0A96CC37E9D8` | `<!-- TRUE_EOF: CR271 TURN-38B1 READINESS PREFLIGHT PRECHECK_COMPLETE -->` | 仍以 38A、owner/lifetime/API/test 为真实门 |
| TURN-38B2 | `2026-07-16-turn-38B2-readiness-preflight-helper-r1.md` / `E3080A47F8097A6B6E86F0B2C8D9EAF0758051E3D3981842EAE0B4794BD5BAEE` | `PRECHECK_COMPLETE TRUE_EOF` | 仍以 TURN-22 parent aggregate、38A、live caller/cache terminal mapping 为门 |
| TURN-38B3 | `2026-07-16-turn-38B3-readiness-preflight-helper-r1.md` / `98E598DA57F009B3EEB5C42BF2782AB323E88AC5787B09E4625E814307A19B74` | `PRECHECK_COMPLETE TRUE_EOF` | 仍有 38A、计划路径漂移、metadata/construction/test 门 |
| TURN-38B4 | `2026-07-16-turn-38B4-scoped-png-artifact-store-readiness-helper-r1.md` / `98712CEEFE50E6E03CFD02A66FF98D51869CE70C46D4520E07158FDC094E9472` | `PRECHECK_COMPLETE TRUE_EOF` | 仍以 38A、identity/path/cleanup/caller 为门 |
| TURN-38A 支撑 | `2026-07-16-turn-38A-readiness-preflight-helper.md` / `04C8C2722A3D6E2C62ED876DB0CB6073DC309595F35A96A76EC63D692CFE456F` | `<!-- TRUE_EOF: CR271 TURN-38A READINESS PREFLIGHT REAL_BLOCKER -->` | 七文件 context cutover 自身仍不是 source-start clear |

TURN-38C 没有可消费的 latest fixed delivery：

- 计划要求的 parent authority 文件
  `docs/superpowers/plans/reports/2026-07-15-turn-38-authority-state-classification.md` **不存在**。
- reports 目录中没有文件名匹配 TURN-38C 的 fixed report，也没有 38C `SOURCE DELIVERED/APPROVED/PARENT FREEZE` marker。
- 38M preflight、LeftTop route 与 DELETE cohort helper 都只是非绑定证据；GameContext route helper 的物理末行仅为
  `PRECHECK_COMPLETE`，且无规范 `TRUE_EOF`。无论该 marker 缺失与否，这些 helper 都不是计划 `:1383-1392`
  要求的 parent classification freeze。

因此直接 `S=...+38C` 不是“报告待归档”，而是 classification/consumer write set/test owner 尚未产生。

## 3. 直接依赖的最新 delta

权威 registry `:1213-1219` 冻结：

| Card | `startDependsOn` | 当前满足面 | 当前未满足面 |
|---|---|---|---|
| 38B1 | `14+38A` | TURN-14 source contract 可消费 | 38A 为 `REAL BLOCKERS`；38A/B1 删除与后继 caller 顺序未冻结 |
| 38B2 | `14+22+38A` | TURN-14 source contract 可消费；TURN-22D1 child 已有 source review | TURN-22 parent card 物理 EOF 仍是 `PARENT-REPAIR3-PENDING`，未聚合；38A 未满足 |
| 38B3 | `23+38A` | TURN-23 source surface 可读 | 38A 未满足；计划仍写不存在的 `com/bot/dhxy/service/TaskStartupCheckService.java`，实际文件在 `task/startup` |
| 38B4 | `17+38A+13H` | 17、13H source surface 可读 | 38A 未满足；identity/path/terminal cleanup 的生产 caller 未冻结 |
| 38C | `38M parent freeze` | 五个候选源码存在，可审计 | parent classification 文件、每个 `KEEP_REWIRE` consumer write set 与独立 named test 均不存在 |
| 39 | `38B1/B2/B3/B4+38C` | 无 | 五个直接 predecessor 均没有 parent source-stable completion |

38A 的直接 `S=13C+34C+35+36+37` 也没有闭合：计划 `:1212` 仍为 parent audit pending/real blockers；
其报告还证明 38A 要删除的 old getters/delegate 目前被 B1/B2/B3、38C、old SCC 与写集外 callers 继续编译级使用。
因此等待某个单一 B 文件自然稳定不会解开该环。

## 4. TURN-39 与 predecessor 源码 delta

### 4.1 六个 TURN-39 production 文件

以下均位于 `D:/mavenProject/dhxy-cloud-brain/src/main/java`，Git 状态均为受保护 `??`，且 SHA 与 06:37
TURN-39 preflight 完全一致：

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java` | 169 | `6C6E3610AD37163C22D8EDC0A34CA4F45C458264B3A61F9CF27DF673E904E9CE` |
| `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java` | 328 | `CC8E8256853BC1310D5D92F830267542FE0ECB2E733D3BB9BAA6C75B86BED3C9` |
| `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java` | 227 | `A66E156FDE85BCF58FAB4330CCAFB2774A9F78214F17C63B5BD698D1D90F2599` |
| `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceMetadata.java` | 59 | `A3FE6615BD0D4F571C3618EE45C679B6E28CC08891FA560B7907EDE357C91C93` |
| `com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java` | 193 | `A8F64D8DBB5F9ED2852975D518836E25AF92073F9C818D5F7E9DA7CF18056CB9` |
| `com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java` | 31 | `96827E3179054DF7878D45F9D56B7955F64DD91C25526E1B2AFEB60167008A8B` |

唯一 named test
`src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java` 仍不存在；Cloud
`.gitignore:15` 忽略整个 `src/test/`，所以未来不能用普通 `git status` 证明它存在或被保留。

### 4.2 38B/38C targets

- B1 两 production SHA 仍为 `34EAD25E...` / `EAFA0D7E...`，named test 不存在。
- B2 两 production SHA 仍为 `3E606C3B...` / `FB6901BB...`，named test 不存在。
- B3 `CloudStartupGateAuthority.java` 为 `5648EEA3...`，真实
  `task/startup/TaskStartupCheckService.java` 为 `289E3930...`；计划中的 `service/TaskStartupCheckService.java`
  不存在，named test 也不存在。
- B4 三 production SHA 仍为 `D6907211...` / `CF7E857C...` / `B047D9F9...`，named test 不存在。
- 38C 五个候选源码仍为 `8D5BBEFA...`、`FC3C859C...`、`BE02F23D...`、`B5E17B47...`、
  `DD4C8CCA...`；没有 classification/consumer/test delta。

结论：preflight 后没有任何 predecessor production delivery 可以改变 TURN-39 gate。

## 5. Active refs 与 `InputSequences` owner

### 5.1 old facade/context 的当前编译级 refs

| Symbol | TURN-39 写集外 active evidence | 所属/阻断 |
|---|---|---|
| `CloudGameClient` | Cloud `input/InputSequences.java:5,39,46`；`TaskExecutionContext.java:10,319-320` | `InputSequences` 无当前 owner；context 属 38A |
| `CloudTaskServicePort` | `TaskExecutionContext.java:13,331-332`；`CloudTaskRetainedActionState.java:62-86,247-268,472-478`；`CloudTaskExclusiveInteractionAuthority.java:633`；`RemoteFinalConsumptionCoordinator.java:49,144-187` | 后三者是 old authority/final-consumption SCC，计划留到 44A；不在 TURN-39 六文件写集 |
| `CloudTaskServiceExecutionContext` | `TaskExecutionContext.java:11,35,51,442`；`CloudArtifactStore.java:30,37,44`；`ScopedPngArtifactStore.java:62,153,174,197,209`；`CloudTaskRunAuthorityAssembly.java:204-220,288-300` | 依次属于 38A、B4、未来 44A SCC；当前均未清零 |
| old getter call | `NavigationService.java:564` 仍调用 `taskContext.getGameClient().executeLocalMacro(...)` | TURN-27 文件，不在 39 write set；当前没有 fixed TURN-27 source completion 可消费 |
| production context construction | `CloudTaskRunAuthorityAssembly.java:220,300` 仍只构造 legacy `new TaskExecutionContext(serviceContext)` | `TaskExecutionContext.turnNative(...)` 的 production caller 为 0；40B factory 尚不存在 |

`TurnGameClient` 本身不是可删除旧面：当前 main tree 有 19 个含该 symbol 的文件，真实 Service/port 已通过
`context.getTurnGameClient()` 或 injected singleton 使用它。TURN-39 若修改该文件，必须保留 13G/28P 已冻结的一次
UUID/一次 command/exact context/uncertain 不重试合同，不能借 old facade 清理重写已验证行为。

### 5.2 两个同名 `InputSequences` 必须分仓判断

1. DHXY：`D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/InputSequences.java`，210 行，SHA
   `B293E0C6792303D45A4314050C6E4F1C8B39D0F4DEA426632586ED0F292DACB3`，Git `M`。它属于 DHXY
   physical queue/frozen binding 合同；TURN-28Q 最新 parent review 已释放 implementation owner，仍待独立 review/build。
   它不是 Cloud old facade ref，也不能给 Cloud 文件借 owner。
2. Cloud：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/input/InputSequences.java`，158 行，SHA
   `D728E318338E00287FEC33E229DFAB09F808FE7D6B2C5E6BFF19B6930586AD65`，Git `??`。历史
   `W-INPUT-D2-IMP1-R1` 只证明 2026-07-13 的 source approval/provenance；当前权威计划全文没有给该物理路径
   分配 card/write set，反而在 `:1219` 明确把其 owner 列为未闭合。

Cloud 文件当前事实：

- `:5,39,46` 直接依赖 old `CloudGameClient`。
- 不是 Spring bean；main/test tree 都没有 `new InputSequences(...)`、`@Bean` factory 或其它 production
  construction point。其 JavaDoc 又冻结为 per-run、非 singleton；不能临时改成 singleton 以制造 wiring。
- 除自身外有六个 main caller 文件：`DialogService`、`NavigationService`、`NpcClickService`、`WubeiTask`、
  `FiveRingTaskV2`、`XiuluoTaskV2`。这六个都持有 final field 并有真实调用。
- 删除或改 constructor/client type 会同时改变上述六个 source-compatible surface；只改一个文件没有现成 named test、
  construction owner、terminal projection 或 caller cutover 证据。

### 5.3 TURN-28S2 的最新变化不清除该 blocker

TURN-28S2 卡在 `10:41:06` 被 External A 领取，又在 `10:43:15` 规范
`OWNER-RETURNED ZERO-BYTES-WRITTEN`。当前 `NpcClickService.java` 仍为 3374 行、SHA
`CCE8F0203AC90A0D39F7CFF99DDA8D9A616656A55467ED4AE3AA053AD0923441`，无 owner/WIP。

该既有子卡即使未来由 fresh External 完成，也只迁四个 top-level Alt shortcut；`NpcClickService` 仍有其它
`inputSequences` mouse/Ctrl/exclusive calls和 field/import。它是 TURN-28 的独立 source slice，不是 TURN-39 的
`InputSequences` owner，也不能把六-caller old-client ref 变成零。因此不得把 S2 replacement-ready 写成 T39 self-unblock。

## 6. Metadata production authority

`CloudTaskServiceMetadata.java:29-50` 当前包含：effective/requested task code/name、window role、local-team
session/leader/presence/support、retry policy、startup mode、startedAt。它是 powerless value，不是这些事实的来源。

当前 source evidence：

1. main source 对 `new CloudTaskServiceMetadata(...)` 为 **0**；main 中只有 5 个 type-ref 文件。
2. `TaskExecutionContext.turnNative(...)` 在 `:96-109` 强制接收完整 metadata，但该 factory 的 production caller 为 **0**。
3. 当前唯一 context construction 仍由 old `CloudTaskRunAuthorityAssembly` 接受外部 metadata 参数后构造 legacy context；
   main tree 没有把真实 role/team/retry/startup facts 组装成该 record 的 caller。
4. 当前 `TurnWindowMetadata` 只有 device/window/title/HWND/process/rect/pause/stop；`TurnTaskStartRequest` 只有
   startRequestId、ordered taskCodes、failurePolicy。两者都不提供 local-team role/session/leader/support、
   retry policy、startup mode 或 startedAt。
5. 权威计划把真正 production Task factory/runtime 放在 TURN-40B，而 R5/R6 顺序是 `... -> 39 -> 40B`。
   在父级冻结 construction handoff 前，39 不能猜 40B 字段来源，也不能从 task code、window title 或 request 动态
   推断 local-team truth。

所以 metadata 没有可独立提前实现的“字段补齐”小片。任何方案都必须先由父级逐字段冻结 authority、factory owner、
构造时点和 38A/39/40B 的可编译顺序；若需要 protocol/runtime/context/tests 新路径，先修订 exact write set。

## 7. Test ownership 的最新 delta

### 7.1 TURN-39 唯一 test 不足以独自闭合现状

- `OldFacadeRemovalContractTest` 不存在；计划 `:1710` 只授权这一 test。
- `TaskExecutionContextTurnContractTest.java` SHA 仍为
  `D667D6958DBC38A6FCCF2BA5E562CECD4EF60629DF7A4CD55E347C9DBD9ED945`。其
  `:497-506` 反射要求 legacy constructor、`getScope/getGameClient/getRemoteGameClient` 存在，`:516-534`
  又要求 turn-native 调这些 API fail-closed。它是 13C/38A 的既有合同，不在 39 test write set。
- `LeftTopStatusTurnContractTest.java:96-233` 仍直接调用 old pending mark/read/consume/clear API；其迁移依赖
  38M/38C 分类和独立 test owner。

### 7.2 preflight 之后测试占用反而增加

06:37 TURN-39 preflight 记录 17 个 test class 构造 `CloudTaskServiceMetadata`。当前扫描为：

- **18 个 test files / 20 个 constructor sites**；
- 新增者是 `service/AutoCombatServiceTurnContractTest.java`，当前 SHA
  `B5438DA588B8C572BABC65FA3D6D3F1A93E7F1880DA67975C843D960516C5292`；
- 主快照时 TURN-34AT1 卡物理 EOF 为 External C Repair #2 test-source delivery；落笔后该卡于磁盘
  `10:45:56` 追加 parent Review #3：`P0/P1/P2=0/0/0 / OWNER-RELEASED / DUAL-REVIEW-BUILD-PENDING`，
  SHA仍为 `B5438DA5...`。owner release 只解除同路径 writer，不把该 34AT1 test 转给 TURN-39，也不减少
  metadata fixture 数。

18 个文件为：context、AutoCombatPanel、AutoCombatService、BattleRadar、ClientIdentity、CommonBox、
DialogDetection、DialogOption、LeftTop、PlayerState、ReturnItemPrescan、SummonSkill、TaskTrackerPanel、TeamReturn、
UiCleaner，以及 Wubei/FiveRing/Xiuluo 三个 tracker tests。它们同时都直接构造
`LegacyTaskExecutionTurnContextProvider` fixture。父级只能二选一并写入明确合同：

1. 39 保留 metadata/provider 的 source-compatible fixture API；或
2. 把受影响 tests 分配给 predecessor/专门 test card，并尊重每个 test 的现有卡归属与当时 owner。

不能让唯一新 `OldFacadeRemovalContractTest` 隐式拥有这 18 个受保护 ignored tests。

### 7.3 `TurnGameClientContractTest` 的真实 delta

`TurnGameClientContractTest.java` 在旧 preflight 后由 TURN-28P 增至 639 行、SHA
`89DA4FA3E61430DCFEE39C313FC9CDB05D2905B3BBFD4A34BFAC39F0A730EA67`。TURN-28P parent 已记录该 SHA，
当前测试继续证明 one UUID/one command、timeout/interruption uncertainty 零 retry、correlation 和 bound context；
它不引用 old facade/context/metadata/provider，且不属于 TURN-39 test write set。该 delta强化了需要保留的 early
`TurnGameClient` 合同，但没有清掉任何 TURN-39 blocker。

B1/B2/B3/B4 的四个 named tests与 38A 的 `TaskExecutionContextOldAuthorityRemovalTest` 均仍不存在；38C 的
per-`KEEP_REWIRE` tests 也尚未由 parent freeze 命名。

## 8. 可拆给 External 的 self-unblocking 小片审计

| 候选 | 物理写集 | 为什么当前不能安全派为 TURN-39 self-unblock |
|---|---|---|
| 先改/删六文件中的任意子集 | 六文件内 | `CloudGameClient/Port/ExecutionContext` 互相构造，并被 38A、B4、old SCC 与 `InputSequences` 写集外编译引用；部分删除会制造假编译点，计划又把完整 SCC 删除留给 44A |
| 单独把 Cloud `InputSequences` 改接 `TurnGameClient` | 新增第七 production + 新 test | 当前权威计划无 owner；无 construction seam；六 caller 的 terminal/atomic/exclusive API 不同；会迫使 worker猜 singleton/per-run lifecycle、UNKNOWN/STOP projection和测试归属 |
| 单独改 `NavigationService:564` | TURN-27 文件 | 不在 39 write set，TURN-27 无 fixed source completion；local macro 到 typed turn 的 result/terminal mapping尚未冻结 |
| 单独补 `CloudTaskServiceMetadata` factory | metadata + runtime/context/protocol/tests | production authority 与 40B owner尚不存在，且 18 tests受影响；不是单文件闭环 |
| 先建 `OldFacadeRemovalContractTest` | 仅 named test | 生产引用仍非零；会得到预期失败的空门，且不能修改 13C/LeftTop/18 fixture tests；不是 implementation self-unblock |
| 把 38M 分类交给 External | 仅 parent manifest 文档 | 这是父级 authority 决策，不是 worker Java slice；helper 报告不能冻结 `KEEP_REWIRE/DELETE` 或 consumer/test write set |
| 复派 TURN-28S2 | `NpcClickService.java` | 是独立 TURN-28 slice，当前 owner 已归还且可由 parent另行复派；但只迁四个 Alt sites，不消除 `NpcClickService` 或其余五 caller 对 Cloud `InputSequences` 的依赖 |

**精确结论：`NO SAFE EXTERNAL TURN-39 SELF-UNBLOCKING SLICE`。**

这里不是因为 External 不可用，也不是因为六文件有同路径 writer；而是任何能实际减少 TURN-39 blocker 的修改都需要
当前 exact write set 之外的 owner/consumer/test，或依赖尚未冻结的业务/terminal/metadata authority。父级若决定
拆卡，必须先新增 append-only fixed card，写清 predecessor、initial SHA、唯一 owner、production+test write set、
terminal/compatibility contract，并确认与当时 External A/C/D 活动字节互斥；本 helper不代选路线。

## 9. TURN-39 source-start 前必须出现的可复验增量

1. 38A 的 34C/35/36/37 source gates与 38A/B/C 可编译顺序被父级唯一化，且 38A 最新 source hashes/test owner冻结。
2. B1/B2/B3/B4 分别达到 parent source-stable delivery；B2 的 TURN-22 parent aggregate 明确更新；B3 计划路径纠正。
3. parent 创建计划要求的 38M authority classification，逐 symbol冻结 `KEEP_REWIRE/DELETE`、全部 consumer、exact
   production/test write set；38C 再按该 freeze source-stable。
4. Cloud `InputSequences` 获得明确 card owner，或者六个 active caller 由各自 owner 全部清零；必须同时冻结
   construction/lifetime、atomic sequence、exclusive callback 与 terminal projection，不得只删 import。
5. `CloudTaskServiceMetadata` 每字段 production authority、40B factory owner、构造时点和 38A/39/40B 顺序写入卡；
   不允许从请求、标题或当前窗口动态推断 local-team truth。
6. 父级冻结 existing test 兼容路线：13C context test、LeftTop test、18 metadata/provider fixtures各自是保留 API、
   predecessor迁移还是扩写专门 test owner。若需要第八文件，先修订计划 `:1394-1401` 与 `:1710`。
7. 领取前重扫两仓 status、六文件/全部 active caller/test SHA与 owner；共享 untracked bytes不能按 HEAD恢复或覆盖。

在以上证据出现前，TURN-39 保持 `NOT READY`；不得用 source-text zero guard、只跑唯一 named test、兼容 wrapper
nesting或 44A exclusion allowlist 掩盖真实 active refs。

## 10. Dirty/untracked 保护记录

写入前只读快照：

| Repo | Branch | HEAD | `git status --porcelain=v1 --untracked-files=all` |
|---|---|---|---:|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 717 entries |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 550 entries |

本 helper 只读使用 `Get-Content`、`rg`、`Get-Item`、`Get-FileHash`、`Test-Path`、`git status`、
`git branch --show-current`、`git rev-parse HEAD`、`git ls-files`、`git check-ignore` 与 `Get-Date`。第一次路径探测
把 Cloud 当成 DHXY 子目录时只得到 `MISSING`，随后定位到真实 sibling repo；没有创建、删除或覆盖任何文件。

所有既有 dirty/untracked 与其它 lane bytes 原样保护。无已批准业务差异；按 `696a12b0`、exact-window generation
与最小 HTTPS JSON turn 等价迁移。

TRUE_EOF PRECHECK_COMPLETE
