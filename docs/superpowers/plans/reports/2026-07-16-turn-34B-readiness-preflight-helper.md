# TURN-34B readiness preflight helper

- 角色：CR271 非绑定 readiness helper；不是 reviewer，不作父级裁决，不改主计划、`ACTIVE_WORK`、CR271、迁移矩阵或 dashboard。
- 快照时间：`2026-07-16T02:52:22-04:00`。
- PRECHECK：`WAITING_DEPENDENCY_GATES_AND_PARENT_BRIEF_FREEZE`。
- 本轮只读双仓源码、基线、协议和固定报告；没有修改 Java，没有运行 Maven/JUnit/compile/package，也没有启动 runtime/application/server/Task/UI/capture/input，未执行 Git mutation。

## 1. 独立读取范围

- `D:/mavenProject/DHXY/AGENTS.md`
- `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`
- `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271
- `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节
- `D:/mavenProject/DHXY/docs/业务逻辑.md`
- `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- TURN-21、TURN-22、TURN-23、TURN-26、TURN-33 固定报告 true EOF
- Cloud 当前 production/test source、`migration-baseline/696a12b0` 对照源码及真实 public callers

## 2. 依赖门快照

权威计划 `:1069` 冻结 TURN-34B 的 start dependency 为 `S=21+22+23+26+33`。

| Dependency | 当前固定报告证据 | 对 TURN-34B 的含义 |
|---|---|---|
| TURN-21 | `2026-07-15-turn-card-TURN-21.md:245-268` 父级 Repair #1 复审 `P0/P1/P2=0/0/0`，named test/build cohort 待运行 | source start gate 已满足；最终测试/构建仍是后续 cohort 门 |
| TURN-22 | `2026-07-16-turn-card-TURN-22.md:164-199` 父级审查 `P0/P1/P2=0/1/0`；queue-owned post-click mechanics 前置尚未落盘 | start dependency 尚未满足；34B 不得绕过或在本卡修 TeamReturn mechanics |
| TURN-23 | `2026-07-15-turn-card-TURN-23.md:321-345` 父级 Repair #1 复审 `P0/P1/P2=0/0/0`，named test/build cohort 待运行 | source start gate 已满足 |
| TURN-26 | `2026-07-15-turn-card-TURN-26.md:136-162` 父级 source/test-source 审查 `P0/P1/P2=0/0/0`，named test/build cohort 待运行 | source start gate 已满足 |
| TURN-33 | `2026-07-16-turn-card-TURN-33.md:94-117` 只有 CLAIMED true EOF，没有 delivery true EOF 或父级 source gate | start dependency 尚未满足；必须等固定报告交付后重新读完整 Summon source/result contract |

TURN-33 的当前源码只可视为活动 writer 的中途快照，不能作为 34B 实施基线：

- `SummonSkillService.java:172-206` 仍从 `cleanSummonSkillsOnce(request)` 进入旧 `runSummonSkillWholePass(...)`，并调用 `summonSkillWholePass().execute(intent)`。
- 同一快照下 `CloudSummonSkillWholePassCapability.java:32-35` 已把 `execute(...)` 变成只返回 `Unknown` 的 compatibility tombstone。
- 快照 SHA-256：`SummonSkillService.java=2EE437F1...CCBD1F5`、`CloudSummonSkillWholePassCapability.java=3EE97295...E20A6D`、`CloudTaskExclusiveInteractionAuthority.java=91349697...29D7ABCC`。

这只是“等待 TURN-33 固定交付再复读”的证据，不是对活动 writer 的代码结论。

## 3. 精确写集与并发关系

### 3.1 TURN-34B 唯一 production 写集

- **仅** `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- 唯一 named test：`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
- 固定报告：后续只追加 TURN-34B 自己的卡报告；本 helper 不创建实施卡。

下列文件在 TURN-34B 全部只读：

- `SummonSkillService.java`、`DialogService.java`、`TeamReturnService.java`、`PlayerStateService.java`、`CommonBoxService.java`
- `TaskMaintenanceRequest/Result/Status`、`TeamSupportCapability`、`TeamMaintenanceWindowState`
- `TaskExecutionContext`、holder、turn protocol/client/action/command port
- `AutoCombatService`、`AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2` 及其它 Task/caller
- DHXY 全仓、POM/config/resources/templates、TURN-21/22/23/26/33 文件和测试

### 3.2 与 TURN-33 的关系

权威计划 `:1211-1214` 明确：

- TURN-33 只写 `SummonSkillService.java`、`CloudSummonSkillWholePassCapability.java`、`CloudTaskExclusiveInteractionAuthority.java`。
- TURN-34B 只写 `TaskMaintenanceService.java`。

因此 production 文件写集互斥，没有文件级并发冲突；但存在严格逻辑依赖。34B 消费 TURN-33 最终的
`SummonSkillService::cleanSummonSkillsOnce(SummonSkillCleanupRequest)` 返回/异常语义，不能根据中途源码猜测。

## 4. 当前 TaskMaintenanceService 对 696a12b0 的源码事实

当前目标文件：

- `TaskMaintenanceService.java`：`1130` 行，SHA-256=`39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`。
- `migration-baseline/696a12b0/.../TaskMaintenanceService.java`：`1123` 行，SHA-256=`4BEAFFD08314F694B41A841DFF236C4CE00DC335CBE75DE74A9F667A53803EDA`。
- 两边 public 方法均为 `19` 个，签名集合无差异。
- 完整 no-index diff 只显示 context ownership 适配：`WindowTaskContextHolder/WindowRuntimeContext` 换成 `TaskExecutionContextHolder/TaskExecutionContext`、`summonSkillState(windowKey)` 增加显式 context、log fallback 更新；维护业务主体顺序未改。
- 目标文件对旧 whole-pass/exclusive/fact/macro/input authority 的静态引用均为零：
  `CloudSummonSkillWholePassCapability`、`CloudTaskExclusiveInteractionAuthority`、`summonSkillWholePass`、
  `executeLocalMacro`、`LocalMacroKind`、`WindowFact/readWindowFact`、`executeInputBundle`、
  `InputProvider/InputSequences/GameClientTracker/WindowTaskContextHolder` 全部计数 `0`。

`TaskMaintenanceRequest.enqueueSummonSkillOnly` 当前只有 model 字段/JavaDoc，没有 production caller；它不是 TURN-34B
新增 queue、retry 或 durable state 的授权。模型不在写集内，应保持只读，不得为了使用该字段制造新流程。

## 5. Public caller 与业务顺序

### 5.1 真实 production caller 集合

当前 Cloud 对 19 个 public API 的外部引用只来自四个 production 类：

1. `AutoBattleTask.java`
   - `:136` 初始化维护；`:182-228` idle 顺序为 local team return release -> pending leader defer -> standalone TeamReturn -> optional LEFT_TOP -> `runOpportunisticMaintenance`。
   - `:245-254` 在 TEAM_RETURN release 中保持 pending CommonBox 先于 return-team click。
   - 本文件属于 TURN-34C，34B 只读。
2. `AutoCombatService.java`
   - `:485-554,669-678` 只读 local-team pending/session/capability 和 first-aid wait API。
   - 本文件属于 TURN-34A，34B 只读。
3. `WubeiTask.java`
   - `:366` initialize、`:386` begin round、`:1128-1136` leader pathing Summon-only pass、`:1433-1438` broadcast-only pass；另有 pathing/first-aid/return open-close callers。
   - 后续完整 caller 收口属于 TURN-35，34B 只读。
4. `XiuluoTaskV2.java`
   - `:330` initialize、`:371` begin round、`:1289/:1381/:1538` broadcast-only、`:3821-3829` leader pathing Summon-only；另有 pathing/return open-close callers。
   - 后续完整 caller 收口属于 TURN-37，34B 只读。

下列 public API 当前 Cloud 外部 production 引用为 `0`：
`registerLocalTeamSessionCandidate`、`markLocalTeamWindowRoleDetected`、`markLocalTeamLeaderDetected`、
`completeLocalTeamSessionWindow`。34B 不得伪造 caller，也不得删除或改签名；只保持基线状态机合同，真实注册/结束激活留给后续 activation 卡。

### 5.2 TaskMaintenance 内部顺序必须逐值保留

- `runOpportunisticMaintenance:578-596`：checkpoint -> broadcast（若允许）-> broadcast handled/failed/interrupted 立即返回 -> 只有 no-action 才进入 Summon -> 无能力返回 no-action。
- `handleMaintenanceBroadcast:599-621`：只调用 `DialogService.handleDialog(handleMaintenanceBroadcastOption(...))`；
  `BUSINESS_OPTION_CLICKED/INTERRUPTED/FAILED/no-action` 的映射顺序不变，full fallback flag 原样透传。
- `maybeCleanSummonSkill:624-796`：enabled -> interval -> FREE gate -> per-window due -> 既有 unknown-failure interval -> 既有 tail/count cache -> team round/capability/pathing -> claim max/duplicate -> checkpoint -> `INTERACTING` -> **恰好一次** `cleanSummonSkillsOnce(cleanupRequest)` -> result/cooldown/cache -> restore previous action state -> failed/no-state-change 才释放 round claim。
- 既有 2h tail/count cache、ultimate cooldown、unknown-failure interval、claim/释放规则是 696 基线行为；34B 不新增、删除或改写这些时序，也不把它们改成 transport retry。
- `openTeamPathingMaintenanceWindow:108-121` 精确打开 `FIRST_AID/PATHING_WINDOW/COMMON_BOX/SUMMON_SKILL/LEFT_TOP_STATUS`。
- `openTeamFirstAidMaintenanceWindow:141-150` 只打开 `FIRST_AID`，不能放行 Summon。
- `closeTeamMaintenanceWindow:166-179` 关闭上述五项。
- `open/closeLocalTeamReturnSupportWindow:193-210` 精确打开/关闭 `TEAM_RETURN+COMMON_BOX`。

`docs/业务逻辑.md:7-20,24-67` 的同批 UI 本地队伍边界、`:120-156` 的 CommonBox pending/最高优先级、
`:170-211` 的 live `if8`/静态格倒扫及删除/冷却/队列语义、`:215-223` 的基线等价门均必须保持。

## 6. PRECHECK 风险与父级冻结建议

### R1. turn-native identity epoch 当前不可调用

精确链路：

- `TaskMaintenanceService.java:649` -> `summonSkillState(windowKey, context)`。
- `:996-1026` -> `currentPlayerIdentityEpoch(context)` -> `context.getPlayerIdentityEpoch()`。
- `TaskExecutionContext.java:214-216` 的该方法只调用 legacy delegate。
- `TaskExecutionContext.java:442-451` 对 turn-native context 抛出 `IllegalStateException`。
- `TurnWindowMetadata.java:3-11` 不含 player identity epoch。

因此真实 `TaskExecutionContext.turnNative(...)` 在首次 due Summon、调用 TURN-33 public service 前就会失败。建议父级冻结为：

- legacy context 继续使用原 `playerIdentityEpoch`，不改变 696 行为；
- turn-native 只在 `TaskMaintenanceService.java` 内复用现有 exact bound client，读取当前
  `windowTitle+processId+nativeHandle` 形成私有 identity key；missing/mismatch/invalid 在任何 Summon delegate 前 fail closed；
- 该做法已有 `CommonBoxService.java:446-499` 的同类 turn-native identity fence 先例；不得修改
  `TaskExecutionContext`、协议或 metadata DTO，不新增 owner/session/ledger/TTL，也不产生 action/UUID。

### R2. 当前 singleton state key 缺 Cloud scope/device namespace

精确事实：

- `currentWindowKey:986-993` 只返回 `windowId`。
- `normalizeTeamKey:1069-1079` 只返回显式任务码/请求任务码/任务码。
- `teamRoundKey:1106-1107` 只拼 `teamKey#round`。
- local-team maps 只用原 `localTeamSessionKey`。
- 这些 map 均是 singleton `@Service` 内的 `ConcurrentHashMap`，而 turn-native context 已有
  `getTurnServiceScope()` 和 `getTurnInvocationContext()` 可提供 tenant/user/device/window identity。

不同 tenant/user/device 可出现相同 `windowId`、`wubei#round` 或 session 文本；当前键会共享 cooldown、claim、window
gate 或 capability。建议父级冻结为：

- 对所有**当前真实 context-bearing caller**，私有 key 至少包含既有 `tenantId/userId/deviceId`，per-window key 再含
  `windowId`；formal team key 再含现有 task key，local-team key 再含现有 session key。
- 这只是给既有内存状态加 namespace，不创建新 session/owner/ledger，也不改变业务优先级、round/epoch、claim 数或 TTL。
- 四个当前零外部 caller 的 candidate registration/completion API 保持 public shape，不在 34B 伪激活；父级应在后续
  activation 卡给出 context-bearing 调用边界。34B 测试只锁其单 scope 基线状态机，不能声称已完成跨 scope activation。

### R3. TURN-33 最终返回/异常合同尚未落定

34B 只允许：

- 构造既有 `SummonSkillCleanupRequest`；
- 同步调用一次 `cleanSummonSkillsOnce(request)`；
- 按 TURN-33 最终 `SummonSkillCleanupResult` 或 stop/uncertain 异常投影维护 cache/cooldown/claim，并在所有退出路径恢复
  `GameContext.ActionState`。

不得在 34B 重建 JSON action、UUID、capture/OCR/input loop，也不得捕获 terminal uncertainty 后自动重调 Summon。
TURN-33 delivery true EOF 和父级 source gate出现后，必须先重读最终 production/test source，再发 34B 实施卡。

## 7. 可冻结 implementation brief

依赖满足后，TURN-34B 可冻结为以下最小实施：

1. 只改 `TaskMaintenanceService.java`；保留全部 19 个 public 签名、四类真实 caller 和 caller 文件 byte-untouched。
2. 保持 `runOpportunisticMaintenance` 的 broadcast-before-Summon 顺序及所有 status/result 投影。
3. 保持 Summon gate、existing cooldown/cache、team-round claim、partial-state-change 和 action-state restore 逐值等价；
   每次 pass 最多调用一次 TURN-33 public service，零 transport retry。
4. 仅在本文件修复 R1 的 turn-native identity fence，并给当前 context-bearing state key 增加 R2 的既有 scope namespace。
5. local-team capability 打开/关闭集合、first-aid 弱窗口、return window 与 CommonBox 优先级不变。
6. 不消费 dormant `enqueueSummonSkillOnly`，不增加 background queue、worker、owner、permit、session、ledger、TTL、
   compaction、durable workflow 或自动 retry。
7. 不把 TaskMaintenance 变成 HTTPS/action owner；JSON/raw PNG/OCR/input mechanics 分别由 TURN-26/TURN-33 下游
   production 服务和它们自己的 named tests 负责。
8. 固定记录：`无已批准业务差异；按 696a12b0 与已确认业务逻辑等价迁移`。

## 8. Named-test acceptance 建议

唯一测试文件：`service/TaskMaintenanceTurnContractTest`。测试应直接实例化 production
`TaskMaintenanceService`，使用 test-only scripted Dialog/Summon collaborators；不得复制 production decision mapper，
不得启动 Spring/runtime 或真实网络/input。

### A. priority 与 result projection

- broadcast handled/failed/interrupted 各自 short-circuit，Summon 调用数 `0`。
- broadcast no-action 且 `cleanSummonSkill=true` 时 Summon 恰好 `1` 次；`cleanSummonSkill=false` 时为 `0`。
- `allowFullMaintenanceBroadcastFallback`、sourceTask、cleanup request hints 原样透传。
- stop checkpoint 在 collaborator 前终止；Dialog/Summon 调用数均为 `0`。

### B. Summon gate、cache 与 failure

- disabled、interval disabled、non-FREE、not-due、existing unknown interval、fresh tail cache、无 round、capability
  closed、pathing closed、same-window duplicate、claim limit：Summon 调用数均为 `0`。
- due + 合法 pathing/capability：只调用一次 public Summon service；TaskMaintenance 自身 action/UUID 计数为 `0`。
- success 精确更新 skill-count/start-slot/tail/ultimate/cooldown，并返回既有 success status。
- failed 且无状态改变释放 round claim；已有 delete/ultimate state change 时保留 claim。
- UNKNOWN/uncertain/throw 不伪 success、不刷新成功 cooldown；所有 success/failure/throw/stop 路径恢复此前
  `GameContext.ActionState`。
- 既有 2h caches、unknown interval、ultimate cooldown 与 max claim 值不漂移，不引入新 timer/retry。

### C. turn-native exact identity 与 scope

- 用真实 `TaskExecutionContext.turnNative(...)` 覆盖 first-due path，证明不调用 legacy-only epoch API。
- current bound metadata title/process/HWND 变化或 missing/mismatch 时，在 Dialog/Summon 前 fail closed，delegate/action/UUID=`0`。
- 两个 tenant/user/device 使用相同 `windowId`、相同 task code/round 时，cooldown/window/claim/capability 不串态。
- 显式 context 必须胜过 holder 中的 wrong context；null legacy fallback 保持原行为。

### D. team coordination exact

- pathing window 精确打开五项；first-aid window只打开 `FIRST_AID`；close 精确关闭五项。
- return window 精确打开/关闭 `TEAM_RETURN+COMMON_BOX`。
- one-per-team-round 的 max/duplicate/failed release、local capability epoch、leader conflict/leader absent/all-candidate
  completion 按现有状态机；不支持运行中动态加入或猜 leader。
- 四个当前零外部 caller 的 public shape 保持，但测试不伪造生产 activation。

### E. API/source boundary 与依赖复用

- reflection 锁住 19 个 public 方法签名；`TaskMaintenanceRequest/Result/Status` 与四个 caller source 不改。
- production reachable TaskMaintenance 对 old whole-pass/exclusive/fact/macro/input authority 零引用。
- 不在本测试重复 TURN-26 的 OCR/raw PNG 或 TURN-33 的 per-action JSON/UUID 细节；34B 只断言一次 typed delegate 和
  coordinator projection。最终 cohort 同时依赖 TURN-22、TURN-26、TURN-33 各自 named test 及适用 Cloud compile/build。
- 四个永久本地 Service 清单仍仅 `BagService/UICleanerService/GiveItemService/QuestManagerService`。

## 9. Helper 结语

精确单 production 文件写集和 named-test 边界可以按上述 brief 冻结；当前尚不能派发真实 TURN-34B implementation，
原因是 TURN-22 source dependency 仍有一项父级返修前置，TURN-33 也尚无固定 delivery/source gate。依赖满足后，父级还应
在发卡前复核 R1/R2 是否逐字进入实施 brief，并重新读取 TURN-33 最终 public result/terminal contract。

<!-- TRUE_EOF: TURN-34B-readiness-preflight-helper PRECHECK_WAITING_DEPENDENCY_GATES_AND_PARENT_BRIEF_FREEZE -->
