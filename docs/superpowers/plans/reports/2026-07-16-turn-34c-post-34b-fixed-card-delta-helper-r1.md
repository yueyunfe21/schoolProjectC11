# CR271 / TURN-34C post-TURN-34B fixed-card 与 source-start delta preflight R1

- 角色：`CR271 Internal` 只读 helper。
- 证据截点：`2026-07-16 12:09:41 -04:00`；BP2 正在写源码，因此其源码快照只用于证明“尚未冻结”。
- 本轮唯一写入：本报告。
- 本轮没有创建、claim 或追加任何 TURN 卡；没有修改 Java、测试、POM、协议、计划、`ACTIVE_WORK.md` 或其他文档。
- 本轮没有运行 Git、Maven、JUnit、compile、package、runtime、application、server、Task、UI、截图或输入。
- 输出性质：只冻结未来父级应写入 TURN-34C fixed card 的 delta 和 source-start 条件，不作卡状态裁决。

## 1. 权威依据与 true EOF 复读范围

本轮按以下顺序完成只读复核：

1. `D:/mavenProject/DHXY/AGENTS.md`。
2. `docs/DHXY_CONTEXT.md`。
3. `docs/ACTIVE_WORK.md` 顶部 CR271 当前段。
4. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
5. `2026-07-15-https-turn-thin-client-protocol-design.md`。
6. `docs/业务逻辑.md`，包括 `696a12b0` pre-cloud 行为基线、CommonBox 30 秒规则、召唤槽位及“不新增 TTL/额外读/park/retry/cleanup/fail-closed/phase”边界。
7. TURN-34A、TURN-34B、TURN-34BP1、TURN-34BP2 的固定卡及其当前物理 true EOF；TURN-34AT1 当前子卡尾也已复读。
8. TURN-34BP3 与 TURN-34C 的全部 readiness/freeze/preflight 报告物理 true EOF；两张 fixed card 当前均不存在。
9. TURN-34BP1 两份独立 review 报告和 build preflight 报告物理 true EOF。
10. TURN-34BP2 readiness delta、TURN-34BP3 post-BP2 preflight、TURN-34B post-BP3 whole-card closure preflight、TURN-34C 初版与 post-34AB readiness 报告的物理 true EOF。
11. TURN-19、TURN-21、TURN-22、TURN-23 父卡当前 true EOF，以及两仓相关实际源码、测试文件存在性、分支 ref 和只读文件哈希。

第 14 节明确覆盖旧的依赖表达；其记号为：

- `S = startDependsOn`，决定业务源码何时可以开始。
- 审查、named test、compile/package/build 属于后置交付门，不得反向改写 `S`。

## 2. 两仓与当前事实截面

### 2.1 仓库 ref 与 status 证据

- DHXY：`.git/HEAD` 指向 `thin-client-design`，ref=`0114604e1ff5f15491d2910959c45252e893d04f`。
- Cloud：`.git/HEAD` 指向 `navigation-migration`，ref=`3b988caa010254973e03342272e6d1d6a9685b01`。
- 因本任务明令不得运行 Git，本轮没有重跑 live `git status`。最新可引用的 status 是 TURN-34BP1 build preflight 在 `2026-07-16 11:51 -04:00` 记录的只读证据：
  - DHXY `--untracked-files=all` 共 740 条：43 modified、1 deleted、696 untracked。
  - Cloud `--untracked-files=all` 共 550 条：9 modified、541 untracked。
  - 普通 short status 的较小计数不能替代上述展开计数；两仓均为大 dirty worktree，任何未来 owner 都只能修改 fixed card 明示写集。
- 本报告不把 branch ref 当成业务文件 SHA；未来卡必须逐文件复算 start/final SHA。

### 2.2 当前卡与源码状态

| 项目 | 当前事实 | 对 TURN-34C 的含义 |
|---|---|---|
| TURN-34BP1 | 两个独立 reviewer 均记录 `P0/P1/P2=0/0/0`；生产源码 SHA=`a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e`，named test SHA=`3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785`；final build 待办 | 是 TURN-34B 的已固定 lineage；其 build 待办不是 BP2、BP3 或 34C 的 source-start 前置 |
| TURN-34BP2 | fixed card 已有 worker claim，当前为 source-active；卡文件最后修改仍停在 claim；没有 canonical delivery/parent source receipt true EOF | 不得冻结当前 WIP 的 private type、public signature 或 final SHA |
| TURN-34BP3 | fixed card 不存在，owner 未开 | 必须等 BP2 canonical delivery 与父级 source receipt 后，才能按实际 BP2 结果建卡 |
| TURN-34B | 父卡文件存在，但 post-BP3 whole-production 聚合阶段尚未开；BP2/BP3 尚未形成父级整体验收快照 | 34B 的 `S` 份额尚未形成；但其后置 named test、双审和 final build 不是 34C source-start 条件 |
| TURN-34A | 生产源码已由父级固定在 `AutoCombatService.java` SHA=`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`；子测试 tranche 仍在推进 | 生产 API 快照可作为 `S` 证据，但 34C 建卡前必须复算并确认没有后续 production repair/API drift；测试与 build 债不前移到 `S` |
| TURN-22 | 父卡仍等待其拆分子项聚合后的 parent Repair #3/source receipt | `S` 尚缺；不能以当前半成品或单个子卡替代 TURN-22 父级生产/source contract receipt |
| TURN-19/21/23 | 父级 source/test-source 复读均为 `P0/P1/P2=0/0/0`，owner 已释放；build cohort 仍可后置 | 三项的生产/source 份额可供 34C 建卡前复核 |
| TURN-34C | fixed card 不存在；named test 不存在；无 worker claim | 当前只能 preflight，不能 source-start |

BP2 活动源码的只读快照为：

- `TaskMaintenanceService.java`：`1290` 行，mtime=`2026-07-16 12:05:45 -04:00`，SHA=`3a86f36dcd049c1aea1d58176f0010817dc0e9eef9455fc7283154c110dc5a38`。
- 该 SHA 已较 `12:04` 快照再次变化；它明确是 WIP 证据，不是 BP2 final、BP3 start 或 TURN-34B whole-production SHA。
- 当前 WIP 同时含新 typed-key 结构和未完成的旧调用形态。本报告故意不抄录、不命名、不推断这些 private type，因为 BP2 尚未 canonical delivery。

## 3. TURN-34C 的真实 startDependsOn

权威计划冻结值不变：

```text
TURN-34C startDependsOn = TURN-19 + TURN-21 + TURN-22 + TURN-23 + TURN-34A + TURN-34B
```

这里每一项要求的是“34C 实际消费的生产/source contract 已由对应父级接收、SHA 与 API 已固定、相关 source owner 已释放”，不是对应卡已经完成 named test、final compile/package/build 或整卡关闭。

### 3.1 当前满足度

| `S` 项 | source-start 所需证据 | 当前结论 |
|---|---|---|
| TURN-19 | 父级 source receipt、实际供应文件 SHA 未漂移、owner 释放 | 已有；34C 建卡时重算 |
| TURN-21 | 父级 source receipt、实际供应文件 SHA 未漂移、owner 释放 | 已有；34C 建卡时重算 |
| TURN-22 | 父级完成拆分聚合，写入 production/source contract receipt、final SHA、owner release | 待形成 |
| TURN-23 | 父级 source receipt、实际供应文件 SHA 未漂移、owner 释放 | 已有；34C 建卡时重算 |
| TURN-34A | 父级确认 `AutoCombatService.java` production SHA=`532e6f...` 仍是最终 source 输入；四个 consumer API 未漂移 | production 份额已有，claim 前须重算；AT 系列测试/final build 不属于 `S` |
| TURN-34B | BP2 canonical delivery -> 父级 source receipt/release -> BP3 fixed card/delivery -> 父级 whole-production source receipt/release；最终 public API 表与 SHA 固定 | 尚未形成 |

TURN-34BP1 不是 TURN-34C registry 中的独立 `S` 项。它是 TURN-34B 的内部 lineage；其双审 2/2 允许 BP2 继续，但它的 final build 待办不得被提升为 34C source-start 条件。

### 3.2 TURN-34B 对 34C 的最小 source prerequisite

TURN-34B 对 34C 的 source prerequisite 精确止于以下节点：

1. BP2 worker 在 BP2 卡 true EOF 写入 canonical source delivery，列出实际完整 write set、逐文件 final SHA、最终 public declaration 表和无额外写入声明。
2. TURN-34B 父级逐文件复读 BP2，写入 parent source receipt，并释放 BP2 对 `TaskMaintenanceService.java` 的 owner。
3. 父级只根据 BP2 实际 final 源码建立 TURN-34BP3 fixed card；卡内填入 BP2 final SHA 作为 BP3 start SHA，不得使用当前 WIP SHA，也不得猜 BP2 private type。
4. BP3 完成其 fixed write set，worker canonical delivery 后由父级复读；父级写入 BP3 source receipt、最终 `TaskMaintenanceService.java` SHA、whole-production public API 表和 source owner release。
5. TURN-34B 父卡写入 whole-production source contract receipt，明确 34C 所消费的六个调用点如何绑定到最终真实 public declarations。

到第 5 步，TURN-34B 对 TURN-34C 的 `S` 份额即可形成。以下事项继续留在 TURN-34B 的后置交付门，不得追加为 TURN-34C 的 source prerequisite：

- `TaskMaintenanceTurnContractTest` 的串行创建与 source review。
- TURN-34B 的两份独立整卡 review。
- named test 实际执行。
- Cloud compile/package/final build。
- fresh runtime。

唯一例外不是“等待后置门”，而是快照失效：若后续 review/test 发现必须修改 TURN-34B production source 或 public API，父级必须写回新 source SHA；尚未 claim 的 34C 重建 start snapshot，已 claim 的 34C 停止在原地等待 rebase/review。该规则不能被预先改写成“必须等 final build 才能开始”。

## 4. fixed-card 创建与 source-start 的精确时点

### 4.1 父级何时可以创建 TURN-34C fixed card

必须同时具备：

1. TURN-19、TURN-21、TURN-23 的当前 source receipt 与实际供应文件 SHA 复算一致。
2. TURN-22 父卡已有最终 production/source contract receipt、final SHA 和 owner release。
3. TURN-34A 父级再次确认 `AutoCombatService.java` SHA 与四个 consumer API 未发生 production drift。
4. TURN-34B 已完成上一节五步 whole-production source receipt，BP2/BP3 对 `TaskMaintenanceService.java` 的 source owner 均已释放。
5. 父级从 BP3 后的实际 `TaskMaintenanceService.java` 逐字抄入最终 19 个 public declarations，并单列 AutoBattle 实际使用的六个调用点；不能从 readiness 报告、BP2 WIP 或聊天中补全。
6. `AutoBattleTask.java` 当前文件无人持有写 owner，且 start SHA 已重新计算。
7. `AutoBattleTaskTurnContractTest.java` 的存在性与 start SHA 已重新计算；若仍不存在，卡内写 `ABSENT`，不能写空 SHA 冒充文件。
8. fixed card 已解决第 8 节所述 test harness ownership，不把 TURN-38A/B3 或 TURN-40B 的未来职责塞进 34C。

### 4.2 worker 何时可以 source-start

父级完成上节 fixed card 并写到物理 true EOF 后，未来 worker 还必须：

1. 从卡首读到 true EOF。
2. 复算卡中所有 start SHA/ABSENT 断言。
3. 确认下节三文件没有其他 writer。
4. 仅在 TURN-34C 卡 true EOF 追加唯一 claim，列出三文件 exact write set 与 start snapshot。
5. claim 落盘后才可修改生产源码或 named test。

当前尚缺 TURN-22 source receipt、TURN-34B whole-production source receipt、BP3 fixed card/delivery/final SHA 和 TURN-34C fixed card，因此当前不能进入上述 source-start 序列。

## 5. TURN-34C future exact write set

权威计划第 17 节规定 TURN-34C 的 production write set 只有一个文件。未来完整 worker write set 冻结为：

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/AutoBattleTaskTurnContractTest.java`
3. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34C.md`，由父级先创建 fixed card；worker 只在 true EOF 追加 claim/delivery/repair 证据。

明确排除：

- `TaskMaintenanceService.java`、`AutoCombatService.java`、`TaskExecutionContext.java` 及所有上游 service/port/model。
- DHXY 仓的 `AutoBattleTask.java` 或任何其他 Java 源码。
- `TaskMaintenanceTurnContractTest.java`、任何上游 named test、protocol/spec/plan、POM、配置、模板和其他卡。
- 为了让测试可构造而新增 production constructor、wrapper、reflection helper、fallback 或跨卡 adapter。

如最终 BP2/BP3 public API 与当前 caller 不一致，34C 只在自己的 `AutoBattleTask.java` 内适配最终已冻结 contract；若问题属于上游 contract 缺失或错误，由 TURN-34B 父级先修复并重发 source receipt，34C 不扩大写集代修。

## 6. 同文件冲突与构建互斥

| 资源 | 所有权/冲突规则 |
|---|---|
| Cloud `AutoBattleTask.java` | TURN-34C 单 owner；claim 前必须重查没有其他卡或人工 writer。DHXY 同名文件是另一仓只读参照，不构成可并写副本 |
| Cloud `AutoBattleTaskTurnContractTest.java` | TURN-34C 唯一测试 owner；当前物理不存在，不能由上游测试 tranche 预建 |
| TURN-34C card | 父级先建 fixed body；worker、父级和 reviewer 只能按阶段在 true EOF 串行追加，不能覆盖前文 |
| Cloud `TaskMaintenanceService.java` | BP2 与 BP3 同文件严格串行；34C 不写，但必须消费 BP3 后父级固定的 final API/SHA |
| Cloud `TaskMaintenanceTurnContractTest.java` | TURN-34B post-BP3 串行测试 owner；与 34C 测试文件不同，不是 34C source-start 门 |
| Cloud `AutoCombatService.java` | TURN-34A production owner 已释放；34C 只调用，不能改。若 SHA/API 漂移则重建 34C snapshot |
| Cloud `target/`、surefire、compiler/package 输出 | 全模块共享构建资源；任何 Cloud Java/test writer 活动时不得运行 clean/final cohort |

因此正确的互斥关系是：

- final build 与活动 source/test writer 互斥。
- final build 不是 source-start prerequisite。
- BP1/34A/34B 的 build 待办可以继续排队；当 34C fixed card 已具备 `S` 且三文件 owner 空闲时，应先允许 34C claim/source delivery，再在所有 Cloud writers 稳定后统一运行 build cohort。
- 若父级选择先打开一个稳定 writer 窗口跑 build，也只能把它当调度选择，不能把“build 尚未跑”写成 34C 的依赖缺口。
- 禁止形成循环等待：`34C 等 final build`，同时 `final build 等 34C writer 结束`。

## 7. API 与调用者所有权

### 7.1 TURN-34A 当前 consumer surface

当前 `AutoBattleTask.java` 消费以下四个 `AutoCombatService` public API；TURN-34C fixed card 必须以 claim 前实际 declarations 为准重新抄录：

1. `initializeForCurrentWindow()`
2. `handleCombatTick(TaskExecutionContext, String, boolean)` -> `TickResult`
3. `hasPendingFollowerFirstAidForCurrentWindow()` -> `boolean`
4. `getDynamicPollingIntervalMs()` -> `int`

34A 拥有战斗 tick、pending first-aid 与动态轮询语义；34C 只拥有调用顺序、分支和结果传播，不得重做其内部判断。

### 7.2 TURN-34B 当前 caller 定位，不作为 BP2/BP3 final contract 猜测

当前 `AutoBattleTask.java` 有六个 TaskMaintenance 调用点：

1. `initializeForTaskStart(...)`
2. `isPendingLocalSupportLeaderDetection(...)`
3. `isLocalSupportMemberSession(...)`
4. `isLocalTeamSupportCapabilityOpen(...)`
5. `awaitLocalTeamSupportCapabilityOpen(...)`
6. `runOpportunisticMaintenance(...)`

这些名称只用于定位现有 caller 和测试所有权；本报告不冻结其 BP2/BP3 最终参数、返回类型、可见性、内部 key 或 private helper。TURN-34C fixed card 必须在 BP3 后从实际源码列出：

- TURN-34B 全部最终 19 个 public declarations。
- 上述六个调用点对应的逐字最终 declarations。
- 最终 `TaskMaintenanceService.java` SHA。

34C 不拥有 maintenance 的 expiry、claim、local-team session、round、capability、Summon、common-box 或 map-key 策略；这些全部由 TURN-34B 冻结。

### 7.3 其他直接 collaborator

34C 仅拥有 orchestration，其他语义分别属于：

- `TaskStartupCheckService`：startup gate。
- `PlayerStateService`：startup first-aid 行为。
- `TeamReturnService`：回队 mechanics，TURN-22 source contract。
- `CommonBoxService`：pending box、30 秒和 click contract，TURN-21。
- `LeftTopStatusSwitchService`：任务支持性与 follower safe-window，TURN-19。
- `TaskExecutionContext`/holder：exact context 与恢复语义，TURN-34BP1 及后续 context 卡。

### 7.4 生产调用者与 activation 边界

- 当前 Cloud 源码没有 `AutoBattleTask` 的生产 factory/runner caller；只有任务类自身和测试侧源码扫描证据。
- 当前 DHXY 运行链为 `DefaultTaskFactory` -> `WindowTaskRunner` -> `task.execute(executionContext)`；该链只用于对照基线，不在 TURN-34C 写集。
- TURN-40B 拥有未来 Cloud task factory/runtime activation。TURN-34C 不注册、不启动、不接 host route。
- TURN-38A/B3 拥有 old-authority/context/startup 的后续 turn-native rewire。TURN-34C 不提前修改 `BaseTaskTemplate`、startup gate 或 authority model。

## 8. TURN-34C named test 所有权

未来唯一 named test：

```text
D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/AutoBattleTaskTurnContractTest.java
```

当前为 `ABSENT`。其测试归属只覆盖 TASK orchestration：

1. startup 顺序：startup check -> state `RUNNING` -> PlayerState first aid -> TaskMaintenance init -> AutoCombat init。
2. 每轮一次 checkpoint、一次 AutoCombat tick；非 `NONE` 结果不进入 idle；仅 `FREE` 进入 idle；一轮只做一次 sleep。
3. idle 优先级：local-team return/release -> pending support leader -> standalone return -> follower/left-top -> opportunistic maintenance。
4. local-team return：先 await `TEAM_RETURN`，再按最终 34B contract 检查/消费 CommonBox capability，最后执行 TeamReturn；不新增读、retry、park、cleanup 或 fallback。
5. terminal/result/state 传播、stop 后 `IDLE/FREE`、异常不吞并。
6. 显式传入 `TaskExecutionContext` 时 holder 绑定与退出恢复；同线程既有 sentinel 不泄漏、不覆盖。
7. 任务层对 transport/action/input/capture/OCR/template/UUID 的直接生产引用为零；测试使用 in-process fake/recording collaborator，只断言 orchestration。

以下不属于该 test：

- TURN-19/21/22/23、34A、34B 各自的 JSON/PNG/action sequence、UUID、窗口绑定、mechanics 和内部业务判断。
- TURN-40B 的实际 activation。
- TURN-38A/B3 的 turn-native startup positive path。
- Maven/JUnit/build 执行结果本身。

当前 `BaseTaskTemplate.logWindowContext(...)` 与 startup 路径仍读取后续 context 卡才会清理的 old-authority 字段。为避免循环依赖，34C fixed card 必须把测试 seam 冻结为“当前可构造的 direct task orchestration + supplied context/holder restoration”，并写明实际构造方式；不能要求 TURN-38A/B3 positive activation，也不能用 production wrapper、反射或 fallback 伪造。若父级无法从当前实际 API 写出该构造方式，应先解决卡间所有权，再创建 fixed card，而不是让 worker 猜。

未来 named-test 命令归 TURN-34C 交付后父级稳定窗口执行：

```text
mvn -q -Dtest=AutoBattleTaskTurnContractTest test
```

本轮未运行该命令；测试文件当前也不存在。

## 9. 行为基线冻结

- TURN-34C 只把 `696a12b0` 已确认的 AutoBattle orchestration 等价迁入 Cloud task，不批准任何业务差异。
- 不从当前 DHXY `AutoBattleTask.java` 搬入其后续 CR244 self-check、maintenance broadcast queue 或其他晚于基线的行为。
- 不新增 TTL、额外 verification/read、park/yield、retry、cleanup、fail-closed、第二 command、phase、优先级或 fallback。
- 不改变 startup first-aid/maintenance、combat tick、team return、left-top、common-box 的既有顺序和调用次数。
- 当前 Cloud `AutoBattleTask.java` 的 JavaDoc 仍提到旧 `readWindowFact(BATTLE_RADAR_*)`。该文件属于 34C 写集，未来 worker 可在不改变行为的前提下把注释校正为实际 collaborator contract；不能借注释清理扩大逻辑范围。

## 10. fixed-card 必填 SHA 台账

| 输入/产物 | 当前只读证据 | TURN-34C fixed card 或交付必须填入 |
|---|---|---|
| TURN-19 `LeftTopStatusSwitchService.java` | `03e43188b52e6f07c50e7975b7eee3c53bddc4c12d9866ff130a869d5cfe1ef2` | claim 前复算 final source SHA |
| TURN-19 `CloudLeftTopStatusPortAssembly.java` | `9b767117e2903e32db448773d823d7a0f527802d6be0a29716b5fe4de81df7e1` | claim 前复算；仅依赖证据，不进 34C 写集 |
| TURN-21 `CommonBoxService.java` | `93e93321ae4cbdd29c3d94af4172d72ae8fefe137cf417e7f2c570b93856ce68` | claim 前复算 final source SHA |
| TURN-21 `CloudCommonBoxPortAssembly.java` | `bc60c0980b5edb3cfb220c68fab98c808fde3ff486e390d442588d7267d0ae7d` | claim 前复算；仅依赖证据 |
| TURN-23 `PlayerStateService.java` | `865a66b761eb9752b9697cddf8058f06d71a9b87bd0b7d0895025298c0c35548` | claim 前复算 final source SHA |
| TURN-23 两个 player-state ports | `f66624a9afe26f387fff9fa7f08bd8d144343fbd7e99e8a32533c8717049a895` / `8cd5a67b6aab39b0fce47a4c1689a62b15ae49232766feef4206ecd3e438b5c0` | claim 前复算；仅依赖证据 |
| TURN-22 `TeamReturnService.java` | 当前 `cd1cd365bff90b16817c15831a2685f2feae84e2d49893b9b975362d4ec4edaf`，但 parent Repair #3 尚未形成 | **待填 TURN-22 parent final production/source receipt SHA**；当前值不能单独满足 `S` |
| TURN-34BP1 `TaskExecutionContext.java` | `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e` | 作为 34B lineage 记录；build 结果另列，不作 34C start SHA |
| TURN-34BP1 named test | `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785` | 作为 lineage 记录；final build 仍待填但不阻塞 source-start |
| TURN-34BP2 `TaskMaintenanceService.java` final | 当前活动 WIP=`3a86f36dcd049c1aea1d58176f0010817dc0e9eef9455fc7283154c110dc5a38` | **待填 BP2 canonical delivery + parent source receipt final SHA**；禁止采用 WIP 值 |
| TURN-34BP3 start | fixed card 不存在 | **待填，必须等于父级接收的 BP2 final SHA** |
| TURN-34BP3 / TURN-34B whole-production final | 尚未产生 | **待填 BP3 delivery + parent whole-production source receipt 后的 `TaskMaintenanceService.java` final SHA** |
| TURN-34B final public API | BP2/BP3 尚未冻结 | **待填实际 19 个 declarations、六个 AutoBattle consumer declarations 及对应 source SHA** |
| `TaskMaintenanceTurnContractTest.java` | `ABSENT` | **待 post-BP3 串行测试创建后填 final SHA**；这是 34B 后置交付证据，不是 34C source gate |
| TURN-34A `AutoCombatService.java` | `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` | claim 前复算并确认父级 production receipt 未回退 |
| TURN-34A final named-test aggregate | 子 tranche 尚未结束 | **待填最终 test-source SHA/receipt**；后置证据，不作 34C source gate |
| TURN-34C `AutoBattleTask.java` start | 当前候选 `e13bfff740570b9c7b833f7edce336bffe39fb89e410b630ff2156b69410264a`，294 行 | **fixed card 创建时复算并填 start SHA**；worker claim 前再核对 |
| TURN-34C `AutoBattleTaskTurnContractTest.java` start | `ABSENT` | fixed card 写 `ABSENT`；若建卡前出现文件，停止并查 owner，不得覆盖 |
| TURN-34C production delivery | 尚未开始 | **待填 `AutoBattleTask.java` final SHA** |
| TURN-34C test delivery | 尚未开始 | **待填 `AutoBattleTaskTurnContractTest.java` final SHA** |
| TURN-34C card | 当前不存在 | 父级创建后填 card initial SHA/true EOF；worker 只追加 claim/delivery |

## 11. 父级可直接抄入 fixed card 的 source-start checklist

```text
[ ] TURN-19 source receipt re-read; supplier SHA unchanged; source owner free
[ ] TURN-21 source receipt re-read; supplier SHA unchanged; source owner free
[ ] TURN-22 parent production/source receipt and final SHA recorded; source owner free
[ ] TURN-23 source receipt re-read; supplier SHA unchanged; source owner free
[ ] TURN-34A production SHA/API re-read; no production drift; source owner free
[ ] BP2 canonical delivery and parent source receipt recorded with final SHA/API
[ ] BP3 fixed card built only from BP2 final source; BP3 delivery and parent source receipt recorded
[ ] TURN-34B whole-production source receipt records final SHA, 19 declarations, six consumer declarations, owner release
[ ] AutoBattleTask start SHA recomputed; AutoBattleTaskTurnContractTest presence recorded exactly
[ ] Three-file exact write set has no writer
[ ] Test harness uses current direct orchestration seam; no TURN-38A/B3 or TURN-40B ownership leak
[ ] Worker claim appended at TURN-34C card true EOF before first source write
```

不得追加到上述 checklist：BP1/34A/34B named test 已执行、双审结束、Cloud compile/package/final build 完成或 fresh runtime 完成。它们属于各卡后置交付与运行验收；把它们前移会违反第 14-19 节的 `S`/后置门拆分，并制造 build 与 writer 的循环等待。

无已批准业务差异；按 `docs/业务逻辑.md` 与 `696a12b0` 基线等价冻结 TURN-34C future card/source-start delta。

TRUE_EOF PRECHECK_COMPLETE
