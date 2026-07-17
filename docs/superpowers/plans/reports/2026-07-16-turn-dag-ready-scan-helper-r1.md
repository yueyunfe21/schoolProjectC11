# CR271 HTTPS Turn DAG Ready Scan Helper R1

- 扫描时间：`2026-07-16T02:30:14.157-04:00`；末轮 true EOF / worktree 对账：
  `2026-07-16T02:41:09.586-04:00`。
- 角色：非绑定 DAG / 并发排班 helper；不是 reviewer，不替代父级 manager/final reviewer。
- 结论类型：`PRECHECK`。本文不写 `APPROVED`、`BLOCKED`、`CARD CLOSED`，也不改变任何卡的权威状态。
- 唯一写集：本报告。未修改 Java、主计划、`ACTIVE_WORK`、CR271、迁移矩阵或 dashboard；未运行 Maven、
  JUnit、compile、runtime、application、server、Task、UI、capture 或 input；未执行 Git mutation。

## 1. 已读取材料与快照

已完整读取或核对：

1. `D:/mavenProject/DHXY/AGENTS.md`。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271 当前段。
4. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节，尤其：
   - `:955-963` 后置覆盖规则；
   - `:992-1096` 权威注册表；
   - `:1165-1209` business exact write set；
   - `:1316-1357` 动态并发与构建门；
   - `:1502-1505` start/approval gate 区分；
   - `:1512-1549` 各 business 卡唯一 named-test 写集。
5. 当前固定报告 true EOF：
   - TURN-22：扫描期间从 `:109 CLAIMED` 推进到 `:162 SOURCE+TEST DELIVERED`；当前待父级独立审查，尚无
     父级 source gate 结论；
   - TURN-33：`2026-07-16-turn-card-TURN-33.md:117`，最新为 `CLAIMED`；
   - TURN-23：`:345`，父级 source/test-source review `P0/P1/P2=0/0/0`；
   - TURN-24A：`:170`，父级 source/test-source review `P0/P1/P2=0/0/0`；
   - TURN-26：`:162`，父级 source/test-source review `P0/P1/P2=0/0/0`。
   - 并发生成的 `2026-07-16-turn-28-readiness-preflight-helper.md` 已完整读取，但当前没有结构化
     `<!-- TRUE_EOF: ... -->`，且其 TURN-26 时点和 test package 与最新权威材料不一致；本文不采用其结论，
     只把其中指出的 mechanics 风险重新对照源码独立核实。
   - 并发生成的 `2026-07-16-turn-34A-readiness-preflight-helper.md` 已完整读取；它同样只以普通文本
     `true EOF` 收尾，没有结构化 `<!-- TRUE_EOF: ... -->`。其稳定排班事实是 TURN-33 尚未形成 source gate，
     因而 TURN-34A 当前不能领取；其中额外列出的 identity/import/public-surface 风险只作为父级后续冻结提示，
     不由本 helper 擅自提升为新依赖或卡片裁决。
   - 对 `reports/*.md` 的全部结构化 true EOF 做末轮扫描后，只有 TURN-33 的最新 EOF 仍含 `CLAIMED`；
     TURN-22 的最新结构化 EOF 已是 `SOURCE+TEST DELIVERED`。没有发现第三个仍持有结构化 claim 的活动 owner。
6. 两仓 `git status`：
   - DHXY 分支 `thin-client-design`：40 个 modified/deleted、40 个 untracked；
   - Cloud 分支 `navigation-migration`：9 个 modified、19 个 untracked；
   - 两仓均可解析业务基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。

全部既有 dirty/untracked 均视为受保护材料；本扫描没有将“文件已 dirty”误判成当前 owner 冲突，也没有改写它们。

## 2. 并发容量结论

权威计划 `:1318-1320` 与 `:1354-1357` 已明确：

- 设计上限是最多 **7 条 Internal implementation**，不是 4 条；
- helper/preflight 不占 implementation 语义槽；
- “四槽”只是此前工具容量观察，不是计划天花板；
- 有依赖满足且 exact write set 互斥的卡时必须继续扫描并滚入，不能因为已有 Worker 写入就停止排班。

因此父级不应把 4 当作仓库流程硬上限。实际能否同时创建第 5-7 个会话，应以父任务当轮 spawn 工具返回为准；
若工具层暂时只容纳 4 个会话，已完成 true EOF 的 helper 应立即关闭并把会话位换成真实 implementation，不能让 helper
长期占用工具会话。

## 3. 当前活动 / 待审 implementation 写集

### TURN-22

最新 true EOF 已变为 `SOURCE+TEST DELIVERED`；Worker 已交父级审查，但在父级结论前仍保留原卡写集，不能被
其它 Worker并发修改。唯一写集：

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TeamReturnService.java`
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`
- Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
- 原 TURN-22 固定报告 append only

### TURN-33

最新 true EOF 仍为领取，尚无固定报告交付 EOF。唯一写集：

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java`
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`
- Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java`
- 原 TURN-33 固定报告 append only

两张卡彼此无文件交集；当前 Java writer 是 TURN-33，TURN-22 为 delivered/pending parent review。

## 4. TURN-28 的业务依赖已齐，但当前 exact write set 仍不能等价实现基线

非绑定结论：`PRECHECK_NOT_START_READY`。TURN-28 的三项业务 source dependency 已有材料，写集也与 TURN-22/33
互斥；但独立源码核对发现，当前 HTTPS turn mechanics 无法在冻结三文件写集内表达 `696a12b0` 的两个必需动作边界。
因此不能只凭依赖表把 TURN-28 派给 Worker；父级需先修订计划并冻结一个真实 mechanics 前置。

### 4.1 start dependency 证据

权威注册表 `:1055` 固定 TURN-28 的 `S=TURN-23+TURN-24+TURN-26`：

| 前置 | 当前证据 | 预检判断 |
|---|---|---|
| TURN-23 | 原报告 `:345` 为 Repair #1 父级 source/test-source review `0/0/0`，owner 已释放 | source start gate 已有材料 |
| TURN-24 | 父卡不可直接领取；唯一已冻结子卡 TURN-24A 原报告 `:170` 为父级 review `0/0/0` | source start gate 已有材料 |
| TURN-26 | 原报告 `:162` 为父级 source/test-source review `0/0/0`，owner 已释放 | source start gate 已有材料 |

TURN-24 不需要再人造 TURN-24B：计划 `:637-639` 和 TURN-24A 固定报告明确，TURN-24A 的唯一 caller 是
`AutoCombatService -> BattleRadarService::checkAndSyncCombatState`，同文件其余 public methods 已作为完整 Service
integration surface 一并迁移，且不得重复计数。再拆一个 BattleRadar implementation 会重复写
`BattleRadarService.java`，不是合法新卡。

### 4.2 TURN-28 exact write set

依据计划 `:1200-1202` 与 `:1531`，父级可冻结的完整写集只能是：

1. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`。
2. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java`。
3. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java`。
4. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java`。
5. 父级新冻结的 TURN-28 固定报告 append only。

`ImageAlgorithms.java`、reference/shadow pipeline、Task、Navigation、BattleRadar、Dialog、协议、DHXY、POM、配置与
其它报告全部只读。不得顺手扩成 model/DTO/helper 卡。

### 4.3 与活动 owner 的交集

| 对比 | Production 交集 | Test 交集 | Report 交集 |
|---|---:|---:|---:|
| TURN-28 vs TURN-22 | 0 | 0 | 0 |
| TURN-28 vs TURN-33 | 0 | 0 | 0 |

当前磁盘上 `NpcClickService.java` 为既有 untracked，`ObjectiveTextRecognizer.java` 与
`SmartClickRecognizer.java` 为既有 modified，named test 尚不存在。这些是受保护的既有工作树材料，不是 TURN-22/33
owner 冲突；TURN-28 Worker 必须基于当前内容工作，不得回滚或覆盖。

### 4.4 独立核实出的 mechanics 缺口

#### A. `Alt+C` / `Alt+A` 当前不能按已批准后台键盘边界执行

- `696a12b0:NpcClickService.java:624-627` 要求普通首次失败后执行 `Alt+C -> 700ms` 再跑完整 pipeline。
- 同一基线 `:667-685` 要求 direct-combat 在确认 flying 时执行 `Alt+C -> 700ms`，随后执行
  `Alt+A -> 350ms`。
- 当前 DHXY `TurnInputStepExecutor.java:70-81` 对除 `KEY_TAP` 外的 `KEY_DOWN/KEY_UP/TEXT_INPUT` 全部返回
  `BACKGROUND_KEY_UNSUPPORTED`。
- `TurnKeyMapper` 只接受 `backgroundHwndSupported=true` 的快捷键；当前
  `BoundWindowKeyboardService.java:236-237` 明确把 `ALT_A`、`ALT_C` 标为 `false`。
- TURN-28 冻结写集不含上述 DHXY executor/driver，因此 Worker 既不能合法实现两条基线快捷键，也不能偷偷前台
  fallback、删分支或修改写集。

#### B. Ctrl probe 的 before/after frame + finally-release 无现成闭合 action

- `696a12b0:NpcClickService.java:370-428` 在同一全局 exclusive callback 内依次：capture before、hold Ctrl、
  wait 80、move、wait 280、capture after、Cloud-equivalent OCR/verify，并在 `finally` 无条件 release Ctrl + wait 100。
- 协议规格 `https-turn-thin-client-protocol-design.md:68-73` 与双端 validator
  `TurnProtocolValidator.java:75-82` 限定一个 action 最多一个上传 frame；同一 action不能返回 before 与 after 两图。
- 把 before/after 拆成两个 action 会失去跨 action 的 input exclusive，且会制造被明确禁止的 session/owner。
- 即便把 `KEY_DOWN -> MOVE -> CAPTURE -> KEY_UP` 塞入一个 action，当前 `TurnInputStepExecutor` 不执行
  `KEY_DOWN/KEY_UP`；并且 `LocalTurnActionExecutor.java:94-115` 在中途 failed/stopped 后把后续步骤标为
  `NOT_RUN`，不能保证 Ctrl 的 `finally` 释放。
- 四个永久本地 Service allowlist 没有 NPC local macro；不能用第五个 local Service 绕开协议。

#### C. 为什么不能把缺口塞回 TURN-28

TURN-28 的权威 production 写集只有三个 Cloud 文件；以上缺口至少涉及 DHXY turn executor、键盘 driver、双端协议或
一个新的通用 fail-safe mechanical boundary。直接让 TURN-28 Worker改这些文件会违反 `:960-962` 的后置覆盖规则，
也会与 Foundation test/validator ownership 混在一起。父级必须先修计划、冻结 exact mechanics 产物与 named test，
再判断它与 TURN-22/33 是否互斥；helper 不自行命名或领取该前置卡。

### 4.5 当前并行判断

TURN-28 的 Cloud 三文件与 TURN-22/33 的确零交集，但“文件互斥”只满足并发条件之一；其实施合同尚缺 mechanics
前置，不能形成第三条合法 implementation。当前真实状态仍是：

```text
TURN-22 (SOURCE+TEST DELIVERED / parent review pending) || TURN-33 (active writer)
```

## 5. 当前没有第三张可立即写代码的权威卡

以下是完整 DAG 的最近实现前沿；“等待”仅描述依赖事实，不是 helper 的卡片裁决：

| 卡 | Exact production + test write set | 当前未满足的 start dependency | 为什么不能现在派 |
|---|---|---|---|
| TURN-28 | `NpcClickService.java`、`ObjectiveTextRecognizer.java`、`SmartClickRecognizer.java`；`NpcClickTurnContractTest.java` | 权威注册表尚缺 Ctrl/Alt mechanics 前置 | 业务依赖齐且文件互斥，但三文件内无法闭合基线 Alt+A/Alt+C 与 Ctrl before/after/finally-release；需父级先修计划 |
| TURN-27 | `NavigationService.java`、`CloudMiniMapCoordinateReadability.java`、`MiniMapPointResolver.java`、`NavigationRoutePlanResolver.java`；`NavigationTurnContractTest.java` | TURN-28 | 权威注册表 `:1056` 明确最后消费 NpcClick；虽与 TURN-28 文件互斥，也不能越过语义依赖并行写 |
| TURN-34A | `AutoCombatService.java`；`AutoCombatServiceTurnContractTest.java` | TURN-33 | `:1062` 明确依赖 TURN-33；其余 19/20/21/23/24 已有 source-gate 材料 |
| TURN-34B | `TaskMaintenanceService.java`；`TaskMaintenanceTurnContractTest.java` | TURN-22 父级 source gate、TURN-33 | `:1063` 同时消费 TeamReturn 与 Summon；TURN-22 仅交付待审，TURN-33 仍在写 |
| TURN-34C | `task/AutoBattleTask.java`；`AutoBattleTaskTurnContractTest.java` | TURN-22、TURN-34A、TURN-34B | `:1064` 要等两个 caller 收口卡，不能提前改 Task |
| TURN-35 | `task/wubei/WubeiTask.java`；`WubeiWholeTaskTurnContractTest.java` | TURN-22、27、28、34A、34B | `:1070` 的 whole-task 前置未汇合 |
| TURN-36 | `task/wuhuan/FiveRingTaskV2.java`；`FiveRingWholeTaskTurnContractTest.java` | TURN-27、28、34A | `:1071` 的 Navigation/NpcClick/AutoCombat 前置未汇合 |
| TURN-37 | `task/xiuluo/XiuluoTaskV2.java`；`XiuluoWholeTaskTurnContractTest.java` | TURN-22、27、28、34A、34B | `:1072` 的 whole-task 前置未汇合 |
| TURN-38A | 计划 `:1213-1223` 七个 context/checkpoint/task framework 文件；`TaskExecutionContextOldAuthorityRemovalTest.java` | TURN-34C、35、36、37 | 三大 Whole Task 与 AutoBattle caller 尚未闭合 |
| TURN-38B1/B2/B3/B4 | 计划 `:1225-1234` 四组互斥 state/artifact 文件及各自 named test | TURN-38A（B2 还等 22） | 只能在 38A 后并行，不能先改 state owner |
| TURN-38C | 仅父级冻结的 38M `KEEP_REWIRE` 行及对应独立 tests | TURN-38M parent freeze | 当前连 exact consumer write set 都未冻结，不能实施 |
| TURN-39 | 计划 `:1247-1254` old facade 六文件；`OldFacadeRemovalContractTest.java` | TURN-38B1/B2/B3/B4、38C | state/context 汇合未完成 |
| TURN-40B | 计划 `:1266-1273` 五个 Cloud runtime 新文件及两项 named tests | TURN-39 | real Task factory 不能早于 old facade 收口 |
| TURN-40C | 计划 `:1275-1283` Cloud activation 六文件及 named test | TURN-40B | activation 必须消费真实 runtime |
| TURN-40D | 计划 `:1285-1294` DHXY loop/guard/control 七路径及 named test | TURN-40C | DHXY 用户入口必须等 Cloud activation |
| TURN-43A/42A/43B | 由 43M/42M 精确 manifest 决定的 DHXY 删除 SCC 与 guards | TURN-41 + manifest freeze 链 | manifest/user runtime 前不可删除 |
| TURN-45A/44A/45B | 由 44M45M 决定的 Cloud route/authority/wire 删除 SCC 与 guards | TURN-40C、41、39 + manifest freeze 链 | 当前不具备删除前置 |
| TURN-46 | 两仓 POM/property/config/doc + `HttpsTurnDependencyCleanupGuardTest` | DHXY/Cloud 删除链全部结束 | 不能提前清依赖 |

Foundation named tests / compile cohort 也不是可新增 Java implementation：其 test source 已交，且计划 `:1349` 明确任何
Java writer 活动时不并发 clean；当前 TURN-33 正在写，因此不能拿 Maven 等待项伪造第三张代码卡。

## 6. 下一轮滚动并发建议

按依赖释放顺序，父级可以提前准备但不能越级实施：

1. **现在：**父级立即独立审查 TURN-22 delivery；保持 TURN-33；同时修正 TURN-28 DAG，冻结一个能够闭合
   Alt+A/Alt+C 与 Ctrl fail-safe observation 的真实 mechanics 前置，不派三文件 TURN-28 近似实现。
2. **TURN-33 source gate 通过后：**TURN-34A 可成为下一张候选；若届时 TURN-22 父级 source gate 也通过，
   TURN-34B 可与 34A 同时滚入。
3. **若 TURN-33 先通过、TURN-22 仍在父级审查：**只滚入 TURN-34A；34B 继续等 TURN-22 父级结论。
4. **TURN-28 mechanics 前置通过后：**派 TURN-28；其后三文件可与 34A/34B 并行。
5. **TURN-28 通过后：**立即滚入 TURN-27；它可与 34A/34B 并行。
6. **34A/34B 与 22 汇合后：**滚入 TURN-34C；完成 27/28/34 caller 前沿后再并行 35/36/37。

计划容量仍是最多 7 条；当前快照是 1 个活动 Java writer（TURN-33）加 1 个待父级审查 delivery（TURN-22），但
仍没有可直接领取的第三张权威卡。扩容的最快真实路径不是提前派 TURN-28，而是父级立即审 TURN-22、补齐
TURN-28 mechanics 前置，并在 TURN-33 source gate释放后滚入 34A/34B。

## 7. PRECHECK 结论

- `PRECHECK_NO_IMMEDIATE_THIRD_IMPLEMENTATION`: TURN-22 已交付待父级审查，TURN-33 仍在写；当前仍没有可直接
  领取的第三张权威实现卡。
- `PRECHECK_TURN28_PLAN_GAP`: TURN-28 三项业务 dependency 已有父级 source/test-source review `0/0/0` 材料，
  exact Cloud write set 与活动卡也零交集，但现有 turn mechanics 无法闭合基线 Alt+A/Alt+C 与 Ctrl
  before/after/finally-release，因此仍不是可直接领取的实施合同。
- `PRECHECK_NEXT_RELEASES`: 父级应先审 TURN-22；TURN-33 通过后最先解锁 TURN-34A；TURN-22+33 均通过后
  再解锁 TURN-34B；
  TURN-28 需先有父级新冻结的 mechanics predecessor。
- 父级动作建议：独立复核源码证据，先修正主计划/DAG并冻结真实前置；helper 不执行状态变更。

<!-- TRUE_EOF: CR271 TURN DAG READY SCAN HELPER R1 PRECHECK -->
