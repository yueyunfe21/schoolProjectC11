# CR271 TURN-35 latest readiness helper R2

## 0. 角色、范围与快照

- 角色：CR271 Internal readiness helper，只做 TURN-35 latest readiness refresh；不是 implementation worker、reviewer、批准者或父级。
- 本报告不改变任何卡片状态，不创建 TURN-35 固定卡，不产生 owner/claim/READY/APPROVED 事实，不派工。
- 唯一写入是本报告。未修改 Java、测试、计划、CR 卡、dashboard 或其它文件；未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；未执行 Git mutation。
- 证据快照时间：`2026-07-16T10:24:36.268-04:00`。活动卡在并行追加；本报告只以该时刻各文件物理末行及当前磁盘源码为准，不能覆盖之后的新 true EOF。
- 已读证据：仓库 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、HTTPS turn 协议、`docs/业务逻辑.md` 五倍基线、TURN-35 两份既有 helper、全部直接 `startDependsOn` 最新固定卡/true EOF、必要子卡、两仓 status、当前 `WubeiTask.java`。

## 1. 权威口径

1. 权威计划第 14.1 节规定：第 16 节状态/依赖、第 17 节写集、第 18 节波次覆盖旧章节；`PLANNED/BLOCKED/NOT READY` 不可领取，新文件只能使用第 17/19 节固定名称。
2. TURN-35 当前注册表状态仍是 `PRECHECK DELIVERED / PARENT AUDIT PENDING / NOT READY`，直接 `S=` 为：
   `13C+14+15+21+22+23+26+27+28+31+34A+34B`。
3. TURN-35 目标只允许迁移完整 Wubei Task caller：14 个业务 state 加 `FAILED/STOPPED` terminal；所有本地动作只经 minimum HTTPS turn 或四个 closed local Service；不得恢复 old remote port、第二 facade 或本地业务编排。
4. `docs/业务逻辑.md` 的 authority 是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 加文档内用户明确批准的后续替换项。该 commit 在 DHXY 仓存在，基线 Wubei 文件为 4319 行；当前 Cloud Wubei 仍保留基线中的 direct input/Bag/capture seams、普通终态重按和黄袍固定上限。
5. HTTPS V1 只有 `CAPTURE/MATCH_TEMPLATE/INPUT/WAIT/LOCAL_SERVICE`；每次显式 invocation 一 UUID、一 command、至多一张 raw multipart PNG；失败后剩余 step 为 `NOT_RUN`；`STOPPED`、`DUPLICATE_OR_UNCERTAIN` 不得转成成功或自动重试。
6. `LOCAL_SERVICE` 只允许 `BagService/UICleanerService/GiveItemService/QuestManagerService` 的 closed operation。普通 OCR/图像决策在 Cloud；mouse move+click/queue hold 必须保持一次队列原子序列。

## 2. 两仓只读状态

### DHXY

- 仓：`D:/mavenProject/DHXY`
- branch：`thin-client-design`
- HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- upstream：无
- `git status --short --untracked-files=all` 快照：711 entries = 44 tracked entries + 667 untracked files；其中 1 个 deletion entry。本报告本身已计入 untracked files。
- 仓库指令虽写默认 `dev`，但本任务禁止 Git mutation，因此未切 branch。

### dhxy-cloud-brain

- 仓：`D:/mavenProject/dhxy-cloud-brain`
- branch：`navigation-migration`
- HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- upstream：无
- `git status --short --untracked-files=all`：550 entries = 9 tracked entries + 541 untracked files；0 deletion entry。
- 当前 Wubei：
  - 路径：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - status：`??`，受保护 untracked 文件，禁止 checkout/reset/copy baseline 覆盖。
  - 4329 行，mtime `2026-07-15T22:54:45.753-04:00`
  - SHA-256：`DFDE0AD08900F2553088A7D304556A2B5A754C4980305199DB7B9C9035B720D7`
  - `WubeiWholeTaskTurnContractTest.java` 不存在。

## 3. 全部直接 startDependsOn latest true EOF

| S card | 物理末行/最新直接事实 | 当前 source-start 解释 | final 解释 |
|---|---|---|---|
| TURN-13C | `parent source and test-source review` | production/context source surface 可读消费 | named test/Cloud compile 仍阻断 |
| TURN-14 | `SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD COMPILE PENDING` | source 满足 | final 未完成 |
| TURN-15 | `parent re-review 1 passed` | source 满足 | build 未完成 |
| TURN-21 | `parent-repair-1-passed` | source 满足 | build 未完成 |
| TURN-22 | 父卡 true EOF 仍为 `C1-SOURCE-PASSED D1-EXTERNAL-A-READY PARENT-REPAIR3-PENDING`；子卡 TURN-22D1 已到 `2/2-APPROVED 0/0/0 BUILD-PENDING` | 子片源码证据已稳定，但父卡尚未聚合为 TURN-22 source pass，helper 无权代写满足 | parent aggregate、适用 named test/compile 仍待 |
| TURN-23 | `SOURCE+TEST SOURCE REVIEW PASSED P0=0 P1=0 P2=0` | source 满足 | build 未完成 |
| TURN-26 | `parent-source-test-source-review-passed` | source 满足 | named test/Cloud build 待 |
| TURN-27 | 无固定 TURN-27 卡；只有 readiness helper `PRECHECK_COMPLETE`，明确等待 TURN-28 final API 与父级 scope freeze | **硬阻断** | 未实施、未测试、未编译 |
| TURN-28 | 父卡仅 `S1-SOURCE-PASSED WHOLE-CARD-DECOMPOSED NEXT-SLICE-PREFLIGHT ACTIVE`；TURN-28S2 仍 `REPLACEMENT-READY CLAIM-REQUIRED` | **硬阻断**，完整 NpcClick public behavior 未 source-pass | whole named test/integration/build 未完成 |
| TURN-31 | `PARENT SOURCE+TEST SOURCE REVIEW PASSED` | source 满足，但它曾写同一个 `WubeiTask.java`，未来 claim 前必须复核当前 SHA | build 未完成 |
| TURN-34A | 父卡历史已明确 production SHA `532e6f84...` source-passed并冻结；最新父 true EOF 为 `CHILD-AT1 REPAIR-1-REQUIRED P0/P1/P2=0/2/0 PRODUCTION-FROZEN` | AutoCombat production API 可条件性只读消费；不是 TURN-34A 整卡完成 | AT1 test-only repair、AT2+、whole named test/build 均待 |
| TURN-34B | `RETAINED-PRODUCTION-REVIEW-1 P0/P1/P2=0/2/1`；TURN-34BP1 仍 `ZERO-OWNER ... CLAIM-REQUIRED` | **硬阻断**，TaskMaintenance/context semantics 未 source-pass | test tranches/reviews/build 均待 |

### 3.1 传递门的最新增量

- TURN-28Q 父卡末行仍为 `TURN-28QT1 REPAIR-1-REQUIRED P0/P1/P2=0/3/1 PRODUCTION-FROZEN`。
- TURN-28QT1 子卡在快照前刚追加 `EXTERNAL-A REPAIR-1 TEST-SOURCE DELIVERED ... 19-TESTS ... NOT-COMPILED NOT-RUN`。这是新交付，不是 parent re-review/source pass；不得把子卡 delivery 提升为 TURN-28Q 或 TURN-28 完成。
- TURN-34AT1 在 `10:17:06` 交付后，父级于 `10:23:00` 复审为 `P0/P1/P2=0/2/0 / REPAIR #1 REQUIRED`：最小 CAPTURE null shape 未锁全，七个 terminal case 的 UUID 规范/跨 invocation freshness 证据无效。C 保持 test-only owner；TURN-34A 仍需 AT2+。
- `docs/ACTIVE_WORK.md` 顶部已同步到 `10:23` 的 AT1 退修，并记录 QT1 Repair #1 已有真实增量。最新 child/card 事实仍不改变 TURN-35 parent gate 未过的判定。

## 4. 当前 source-start gates

### 4.1 已具备的面

1. TURN-13C/14/15/21/23/26/31 已有 production/source-test-source review 证据。
2. TURN-34A production source 已审查并冻结，当前活动只在唯一 named test tranche，不与 TURN-35 两个未来文件物理重叠。
3. TURN-22C1/D1 子片均有 source review；D1 还有 2/2 独立 approval。其 production/test files 与 TURN-35 物理互斥。
4. 当前没有其他 writer 修改 `WubeiTask.java` 或未来 WholeTask test；文件级 lane 暂时空闲。

### 4.2 尚未满足的硬门

1. 父级尚未创建 `2026-07-16-turn-card-TURN-35.md`，注册表仍明确 `NOT READY`，所以没有合法 claim/owner/write window。
2. TURN-27 无固定卡、无实现、无 source pass；它又等待 TURN-28 final API。
3. TURN-28 只完成 S1；S2 未 claim，whole source/test-source 未通过；TURN-28Q child delivery 也仍待 parent re-review。
4. TURN-34B production 当前有 P1/P2；它依赖的 TURN-34BP1 exact native metadata fence 未 claim。Wubei 不能在旧 context fence 上冻结 terminal/caller 语义。
5. TURN-22 父卡仍 `PARENT-REPAIR3-PENDING`。子片证据可以作为 parent audit 输入，但 helper 不能替父卡追加 aggregate pass。
6. Bag uncached route 没有冻结 API：`ReturnItemIntent` 只有 `PRESCAN_TASK_PAGE/PRESCAN_FROM_BACK/USE_CACHED_RETURN_ITEM`，没有 baseline-equivalent “scan and actually use” closed intent。TURN-35 一文件写集无权扩 protocol/client/local executor。
7. 当前 `WubeiTask` 与 current `TaskExecutionContext` 已发生入口 API 碰撞：`WubeiTask:4250-4255` 仍调用不存在的 `TaskExecutionContext.builder()`；current context 只在 turn-native path 通过 `getTurnGameClient()` 返回 exact-bound client。当前 Wubei 对 `TurnGameClient` 零引用。父级必须先冻结入口/legacy-no-arg 处理，不能由 worker加 builder shim。

### 4.3 source-start 结论

`TURN-35 = NOT READY / NO CLAIM / NO JAVA WRITE`。

文件级无重叠不等于 source-start 已满足。当前至少 `TURN-27 + TURN-28 + TURN-34B + TURN-22 parent aggregate + Bag route + TURN-35 fixed card` 未闭合；helper 不得把 TURN-34A production stable 或 child delivery 外推为整张 TURN-35 READY。

## 5. 当前 final gates

即使父级未来按更窄子卡开放部分 Task source，TURN-35 final 仍至少需要：

1. 所有直接 predecessor 的最终 production API/source-test-source 合同稳定，且无未解决 P0/P1/P2；尤其 TURN-22/27/28/34A/34B。
2. TURN-35 自身 `SOURCE DELIVERED + TEST DELIVERED`。
3. `PARENT SOURCE REVIEW` 与 `PARENT TEST REVIEW`；父级若沿用 CR271 full process，还需固定卡明确的独立 review gate，helper 本身不计 reviewer。
4. 唯一 named test `WubeiWholeTaskTurnContractTest` 覆盖权威计划的 `BC4+BASE+TASK+IMG+LS`：14 state、STOP/pause、retry/fallback、park/yield、terminal/uncertain、raw PNG、closed local Service、exact-context、一 invocation 一 UUID/command。
5. 显式 required command fresh exit 0：`mvn -q -Dtest=WubeiWholeTaskTurnContractTest test`。这是未来固定卡的 explicit-test exception；本 helper 未创建/运行它。
6. Cloud applicable compile fresh exit 0，通常为 `mvn -q -DskipTests compile`；Java 实施前不得拿旧 classes/jar 当证据。
7. Foundation `TURN-T01/T02/T03/T04` 四张补债卡全部通过。当前它们最多到 test-source review，Maven/compile 门仍未全绿；Whole Task test 不能替代该门。
8. 固定卡最终物理末行明确 `CARD APPROVED`，且没有待返修项。fresh runtime 是独立验收记录，不可伪装成源码/测试 gate。

## 6. exact Task-only future write set

### 6.1 唯一 production modify

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`

### 6.2 唯一 named test create/modify

`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`

### 6.3 DTO 规则

- 必要 DTO 只能放在 `WubeiTask.java` 底部，作为真正必要的 `private nested type`。
- 不得新增 public/package DTO、第二 production Java、第二 test、adapter/helper/facade/wrapper 文件。

### 6.4 全部只读

- `WubeiPhase.java`、`WubeiRoundContext.java`、`WubeiStepOutcome.java`、`WubeiWaitSpec.java`、`WubeiWaitReason.java`、`WubeiDialogCatalog.java`。
- TURN-31 的 `WubeiTaskTrackerTurnContractTest.java`。
- Navigation/NpcClick/Dialog/AutoCombat/Bag/ReturnItem/PlayerState/TaskMaintenance/CommonBox/TeamReturn/UI/TaskTracker 等全部 Service 与 model。
- `TaskExecutionContext`、TurnGameClient、protocol、executor/queue、POM/config/resources，以及 DHXY 全仓。
- 未来 process card `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-35.md` 只能由父级创建并冻结；它不属于 Java implementation write set，当前不存在。

## 7. caller/API/语义碰撞

| 碰撞 | 当前源码证据 | Readiness 影响 |
|---|---|---|
| TURN-31 同文件 | TURN-31 写过 `WubeiTask.java`，当前 SHA 为 `DFDE0AD0...`；post-accept exact-context async tracker read 在 `WubeiTask:2043-2071` | 未来唯一 writer 必须以当前 untracked bytes 增量编辑，保留 TURN-31 caller；claim 前复核 SHA，不能复制 696 文件覆盖 |
| TaskExecutionContext 入口 | `execute(null)` 经 `resolveExecutionContext` 调不存在的 `builder()`；current context 的 exact client 是 `getTurnGameClient()` | 必须由父级冻结 strict turn-native entry/no-arg legacy 行为；禁止 builder shim、manual `new TurnGameClient` 或第二 field/facade |
| TURN-27 Navigation | Wubei 调 `navigateToNPC` 于 `:1407,1849`，`navigateInCurrentMap` 于 `:1588,2115` | TURN-27 无 final API；Wubei 不得复制 route/OCR/candidate/retry，不能猜返回投影 |
| TURN-28 NpcClick | Wubei 的 pending confirm、`clickNpcSmart`、direct-combat caller 分布于 `:733,1427,1960,2017,3399,3440` | S2/whole card 未 source-pass；不能在 Task 复制 Ctrl/Alt/tooltip/verify mechanics 或按 WIP API 接线 |
| TURN-28Q input | current Task 仍直接使用 `InputSequences`；28Q production frozen但 QT1 仅刚交付返修 | 未来 direct input 只能改成 exact turn；在 parent re-review 前不能把 child test delivery写成 input contract final |
| TURN-22 TeamReturn | `useReturnItem` 在实际物品动作前调用 `teamReturnService.beginLeaderSignalPrecheck`，后续等待/返回语义由 parent aggregate 决定 | D1 2/2 证据不替代 TURN-22 parent pass；Task 只读消费最终 API，不复制 150/500 或 queue contract |
| TURN-34A AutoCombat | production `532e6f84...` source-passed/frozen；Wubei init/wake/tick/recovery/trusted-state caller 多处 | production surface 可读，但 AT1 当前 `0/2/0` 返修、AT2+ 和 final tests未闭合；不得借 TURN-35 改 AutoCombat 或重新解释 terminal |
| TURN-34B Maintenance | retained `TaskMaintenanceService` WIP SHA `963b028c...`，19 public API shape 保留但 parent 判 `0/2/1` | shape 稳定不等于 semantics 稳定；BP1 对 exact title/HWND/process 和 A-B-A scope 的修复会影响 Wubei checkpoint/caller assumptions |
| Bag closed API | `WubeiTask:2636` 显形镜调用 `findAndUseItemFromBack`；`:3902` uncached 回程调用 `findAndUseMainBagTaskPageItem` | 现有三种 ReturnItemIntent 不能自行证明 baseline-equivalent scan+use；须 predecessor/API decision，不能删 fallback 或拆成隐式 retry |
| direct input | Alt+C `:2093-2096`；prepared click `:2206-2211`；tracker green `:2736-2740`；黄袍 cached click `:4233-4237` | 都必须迁为 closed turn，保留每处 exact delay/atomicity/terminal；不能把 move/click拆成两 command |
| direct capture/OCR | destination hint 在 `:3044-3212` 直接 tracker capture、yellow wash、本地 OCR | 普通 Cloud-owned image/OCR 必须变成 raw PNG turn evidence和 Cloud decision；不能保留本地业务 OCR或一 turn 多 frame |

## 8. 五倍业务基线对当前源码的已批准差异

以下不是 helper 新行为意见，而是 `docs/业务逻辑.md` 已明确覆盖当前/旧源码的验收规则：

1. 普通怪：当前 `WubeiTask:1675-1695` 在 `PATHING_TERMINAL` 立即重按同一绿链；新验收只允许当前 `attemptId` 的 Cloud static miss 或 Cloud click failure 下发 fallback，且只有实际重按才计数，最多三次。
2. 黄袍：当前 `MAX_CHAINED_COMBAT_ATTEMPTS=5` 与 `:3983-3989` 固定失败上限不再成立；获批规则是无固定场次上限，第一次战后 full tracker，之后只 fast ROI，fast miss 直接结束链且不 full reread。
3. 入战前预算：固定 180 秒，从接任务 option 成功起算，热启动以第一条绿链成功兜底；它必须抢占当前 `300000ms` probe wait、无限 park 和所有 inner wait，pause 阻塞需补偿。
4. 白龙马：每个 probe 显形镜最多两次；镜后继续持 turn，不新增 sleep/park/yield；四种 provider 结果与 Alt+A 授权顺序 exact。
5. 已验证回城：起始地图+坐标是无 TTL 任务事实，只在下一轮接任务 option 实际成功后清除，不新增第二位置验证。
6. CommonBox：回城验证后检测，下一轮接任务 NPC 前优先消费；30 秒 pending 是既有盒子规则，不得拿它给其它 Wubei state 新造 TTL。

除此之外写 `无已批准业务差异；按基线等价迁移`。不得把当前 dirty/untracked 行为自动当 authority，也不得借 migration 自增 TTL、verification、park/yield、retry、cleanup、fail-closed 或 fallback。

## 9. 可拆给 External 的最小先行片

### 9.1 当前可立即派出的 Java 片

**无。**

理由不是文件冲突，而是父级未建卡且至少 TURN-27/28/34B、TURN-22 aggregate、Bag closed route 与 Task entry/context contract 未冻结。给 External 直接改 Wubei 会迫使其猜 caller API 或业务 terminal，违反 startDependsOn 和 baseline gate。

### 9.2 父级未来可考虑的最小真实 implementation tranche

仅作为 parent freeze 候选，不是 READY/claim/派工：

`TURN-35E0 candidate = turn-native public entry + post-accept Alt+C closed action`

固定内容应同时包含：

1. 仍只写 `WubeiTask.java` 与同一个 `WubeiWholeTaskTurnContractTest.java`；不建第三文件。
2. 从 public `execute(TaskExecutionContext)` 使用 context 已绑定的 `getTurnGameClient()`；明确处理 no-arg/nullable legacy entry，不加 `builder()` 兼容壳。
3. 只迁 `startPostAcceptPrepath` 中既有非 startup-flying 分支：一个 closed action，ordered mechanics 为 `Alt+C` 后 `WAIT 120ms`；保留 startup-flying skip 条件；后续 `NavigationService.navigateInCurrentMap` 仍只读、不改。
4. test 从 public execute path 证明 exact context、一次 UUID/command、step 顺序、零 direct `InputSequences`、零 auto retry；`STOPPED/uncertain/FAILED` 的 caller-visible投影必须先由父级逐项冻结。
5. 该片通过只证明一个真实 action boundary，不代表 TURN-35 source pass、14-state coverage 或 final completion；后续仍由同一唯一 writer/同一 named test 增量完成 whole Task。

此候选只有在父级先完成以下动作后才可发：新建 append-only child/fixed card；把其缩窄 `startDependsOn`、initial SHA、exact methods、terminal mapping、test cases 和 owner 写清；确认 TURN-34BP1 后的 context API；确认没有新的 TURN-31/Wubei writer。若父级不愿正式拆卡，就等待全部 S gate 后一次实施 TURN-35 whole card。

### 9.3 不能作为先行片的假闭环

- 只删除 `TaskExecutionContext.builder()` 或加一个 builder shim。
- 只注入/缓存 `TurnGameClient`，但不迁任何 production action。
- 先创建空 `WubeiWholeTaskTurnContractTest`、source guard、reflection/private-helper test 或恒真 fake。
- 先批量删 imports/fields/direct calls，让文件处于半迁移不可编译状态。
- 把一个 direct input 换成 helper，再由 helper 调 old `InputSequences`；这只是 wrapper nesting，不是 turn closure。

## 10. 禁止提前项

1. 不得在 TURN-35 fixed card/child card、parent READY 和真实 claim 之前修改任何 Java/test。
2. 不得提前改 `NavigationService/NpcClickService/AutoCombatService/TaskMaintenanceService/TaskExecutionContext/Bag protocol/client/executor/queue`；有缺口先回 predecessor 或由父级修计划写集。
3. 不得把 TURN-22D1 的 `2/2 APPROVED`、TURN-28QT1 的 child delivery或 TURN-34AT1 的 test-only repair结果提升为 parent card pass。
4. 不得保留普通怪 immediate terminal re-click 或黄袍五场上限作为“最新源码事实”；也不得在独立小片里脱离完整 attempt/budget/state 语义抢先删除它们。
5. 不得删 uncached return fallback、把 prescan 当 use、增加 scan/use 自动重试，或扩大 `ReturnItemIntent` 而不先改 predecessor/card。
6. 不得在 Cloud Task 继续 direct input/capture/local OCR/temp-file业务路径；不得用 `MATCH_TEMPLATE` 承担未请求的普通 OCR。
7. 不得一 turn 请求多张上传图，不得 Base64，不得 timeout/interrupt/uncertain 后重发同业务动作。
8. 不得把 mouse move 与 click 拆成两个 action/queue request；不得在 local Service/exclusive callback 内嵌第二 queue submit。
9. 不得新增 session/owner/lease/ledger/durable workflow/compaction/transport retry/未批准 TTL、第二 observation 或第二 verification。
10. 不得创建 wrapper/helper 层替代 in-place Task flow；必要 private nested type 必须置于文件底部并有真实边界。
11. 不得改/复用 TURN-31 tracker named test 充当 Whole Task test；不得让两个 writer并发改同一 WholeTask test。
12. 当前阶段不运行 Maven/JUnit/compile/runtime/input；未来 Java 实施交付时再按 fixed card 执行显式 named test 与 mandatory compile，不能用 stale artifact 或 `-DskipTests` 代替 named test。
13. 不执行 branch switch、stage、commit、checkout/reset/clean 或其它 Git mutation；保护两仓全部 dirty/untracked。

## 11. Helper 结论

- source-start：`NOT READY`。物理写集当前无重叠，但 formal/semantic gates 未闭合。
- final：`BLOCKED` 于 predecessor completion、TURN-35 source+test、parent reviews、required named test、Cloud compile、Foundation T01-T04 和最终 card marker。
- exact future write set：恰一 production `WubeiTask.java` + 恰一 named test `WubeiWholeTaskTurnContractTest.java`；必要 DTO 仅本文件 private nested。
- External：当前没有可立即领取的 TURN-35 Java 片；最小候选是 parent 正式拆卡后的 turn-native entry + post-accept `Alt+C/WAIT120` closed action，不得把候选当 READY。
- 本报告不构成 review、approval、claim、owner、派工或 Java 变更授权。

TRUE_EOF PRECHECK_COMPLETE
