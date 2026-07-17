# CR271 TURN-34B test-only continuation preflight helper

## 角色与结论边界

- 角色：TURN-34B test-only continuation preflight helper；不是 implementation owner、reviewer、approver 或父级裁决者。
- 快照时间：`2026-07-16T09:10:48.1594942-04:00`。
- 本报告只收敛可实施测试合同，不领取 `TURN-34BT1`，不写 `APPROVED/CLOSED`，不作 P0/P1/P2 裁定。
- 唯一写入是本报告。未修改 Java、Maven/POM、production、原卡/子卡、runtime/input 或 Git 状态。
- 业务依据已核对：`docs/业务逻辑.md` 的本地队伍 capability 边界、通用盒子优先级、召唤兽三技能规则
  `170-211`、基线门禁 `215-224` 与 STOP 非业务失败规则；基线为
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。

**无已批准业务差异；按基线等价迁移。**

## 权威快照

| 项目 | 当前只读事实 |
|---|---|
| DHXY | branch `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f` |
| Cloud | branch `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01` |
| TURN-34B 原卡 true EOF | 160 行，SHA-256 `179F30D4F04A50F6F535D7C555939B54D2E8CA0714C988285D91609E5BF4D3A2`；末段是 `PARENT DECOMPOSITION #1 - TURN-34BT1 READY` |
| TURN-34BT1 子卡 true EOF | 46 行，SHA-256 `289B4DF682B7D449E506160C01CF5B95C2C9493171E9A7F14A4317B0DD219E60`；`READY / CLAIM REQUIRED / PRODUCTION PRESERVED` |
| D 归还 production | `TaskMaintenanceService.java` 1224 行，SHA-256 `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC` |
| 唯一 named test | `TaskMaintenanceTurnContractTest.java` 当前不存在 |

原卡 true EOF 明确保留 D 的 production WIP、释放原 owner，并把第一张真实接续切片拆为 `TURN-34BT1`。
子卡的唯一合法写集是：

1. 新建 Cloud
   `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`；
2. append-only 更新既有
   `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34BT1.md`。

`TaskMaintenanceService.java`、TURN-34B 原卡、其它 production/test/card、DHXY、POM/config/resources 全部只读。
实施者必须先在子卡物理 EOF 领取；本报告不产生 owner。

## 最小可编译 Harness

测试类固定为 package-private
`com.yueyunfe.dhxy.cloudbrain.service.TaskMaintenanceTurnContractTest`，只用现有 JUnit Jupiter，不引入 Mockito。
每个用例从 fresh harness 开始，真实构造 production Service：

```java
new TaskMaintenanceService(
        botProperties,
        gameContext,
        scriptedDialogService,
        scriptedSummonSkillService,
        taskExecutionContextHolder);
```

构造器与 scripted collaborators 固定如下：

1. `BotProperties`：直接 `new BotProperties()`，仅用现有 setter 配置 summon enabled、interval、unknown
   interval、run-immediately 和 ultimate cooldown；不启动 Spring binding。
2. `GameContext`：直接 `new GameContext()`；每例显式设置初始 `ActionState`，并在退出后断言恢复。
3. `TaskExecutionContextHolder`：直接 `new TaskExecutionContextHolder()`；仅用 `callWith` 构造 holder 冲突，
   不新增 thread-local/factory。
4. `ScriptedDialogService extends DialogService`：沿用 `SummonSkillTurnContractTest` 的既有 14 参数
   `super(null, ...)` 方式；覆盖唯一 public `handleDialog`，记录请求、调用次数和脚本结果，未脚本调用立即
   `AssertionError`。不得复制 Dialog reducer。
5. `ScriptedSummonSkillService extends SummonSkillService`：调用现有 public 四参构造，传入 holder、
   `CloudTemplateAssets` 空实现、上述 Dialog fake 与一个 test-private no-op `CloudUiCleanerPort`；只覆盖
   `cleanSummonSkillsOnce(SummonSkillCleanupRequest)`，用 deque 记录请求并返回 typed result 或原样抛出脚本异常。
   不进入 TURN-33 PNG/OCR/五次删除/cleanup/action 细节。
6. `ScriptedCommandPort implements CloudTurnCommandPort`：按 device/window 提供 latest metadata 队列；记录
   `metadataReads`、`executeCalls`、actions。TURN-34B coordinator 若调用 `execute`，本测试立即失败。
7. `CountingUuidSupplier`：沿用 `PlayerStateTurnContractTest`、`TeamReturnTurnContractTest` 的既有
   package-private 四参 `TurnGameClient(..., Supplier<UUID>)` 反射构造 seam。它不是对
   `TaskMaintenanceService` 私有字段/方法的反射；TaskMaintenance private reflection 仍禁止。
8. `FixedInvocationContextProvider`：每个 context 的 client 返回该 context 自己的 binding，不从 holder 猜
   binding。这样才能真实证明“显式 supplied context 胜过错误 holder”，而不是先被 holder provider 的绑定冲突打断。

每个 turn-native fixture 必须真实调用：

```java
TaskExecutionContext.turnNative(
        serviceScope,
        invocationContext,
        initialWindowMetadata,
        cloudTaskServiceMetadata,
        taskRunId,
        turnGameClient);
```

`TurnWindowMetadata` 使用非空 title/HWND、正 process id、正宽高 `TurnWindowRect`；测试 drift 时只改变被测
latest metadata 字段，不能靠无效 initial fixture 提前在构造器失败。所有 coordinator 用例最终断言
`commandPort.executeCalls == 0`、`uuidSupplier.calls == 0`；这只表示 TaskMaintenance 自身不造 action/UUID，
不冒充 TURN-33 的 action/UUID 测试。

## 19 Public API 与 34A 六 API

`TaskMaintenanceService` 当前 19 个 public instance method 的 exact shape 必须作为一个排序后的 signature set
一次锁住；不得只断言数量：

```text
void initializeForTaskStart(TaskExecutionContext,String)
void beginTeamMaintenanceRound(TaskExecutionContext,String,int,String)
void openTeamPathingMaintenanceWindow(TaskExecutionContext,String,int,String)
void openTeamFirstAidMaintenanceWindow(TaskExecutionContext,String,int,String)
void closeTeamMaintenanceWindow(TaskExecutionContext,String,int,String)
void openLocalTeamReturnSupportWindow(TaskExecutionContext,String)
void closeLocalTeamReturnSupportWindow(TaskExecutionContext,String)
boolean isTeamPathingMaintenanceWindowOpen(TaskExecutionContext,String)
boolean awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext,String,long) [34A]
boolean awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability,long) [34A]
boolean isLocalSupportMemberSession(TaskExecutionContext) [34A]
void registerLocalTeamSessionCandidate(String,Collection,String)
void markLocalTeamWindowRoleDetected(TaskExecutionContext,String,String,String)
boolean isLocalSupportMemberCandidate(TaskExecutionContext) [34A]
boolean isPendingLocalSupportLeaderDetection(TaskExecutionContext) [34A]
void markLocalTeamLeaderDetected(TaskExecutionContext,String,String)
boolean isLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability) [34A]
void completeLocalTeamSessionWindow(String,String,String)
TaskMaintenanceResult runOpportunisticMaintenance(TaskExecutionContext,TaskMaintenanceRequest)
```

本 tranche 对四个零 production caller lifecycle API
`registerLocalTeamSessionCandidate`、`markLocalTeamWindowRoleDetected`、`markLocalTeamLeaderDetected`、
`completeLocalTeamSessionWindow` 只锁 public shape，不调用它们，不制造 runtime 可达性。

六个 TURN-34A API 需有最小真实行为兼容用例：fresh closed state 下 candidate/session/pending/capability/zero-timeout
均保持现值；leader context 用 production `openTeamPathingMaintenanceWindow` 打开后，member context 对 candidate、
pending、capability immediate/await 和 formal first-aid await 的返回保持现值。不得编辑或复制 `AutoCombatService`。

## TURN-34BT1 立即测试矩阵

子卡当前只授权 exact-context/scoping/drift/A->B->A/19+6 API tranche。建议最小方法集如下，名称可微调，
断言不可缩减：

1. `publicSurfaceKeepsExactlyNineteenSignaturesAndSixAutoCombatApis()`
   锁上表 exact set、五参 production constructor 与六 API 返回类型；四个零 caller API 不激活。
2. `suppliedContextWinsConflictingHolderAndTurnNativeNeverReadsLegacyEpoch()`
   holder 绑定 wrong scope/window，显式参数绑定 exact context；用 public initialize/due 行为证明状态归 explicit
   context。first-due 能到 scripted Summon 一次即同时证明未调用 turn-native 禁用的
   `getPlayerIdentityEpoch()`；coordinator action/UUID 仍为零。
3. `missingDeviceAndWindowMetadataStopBeforeEitherDelegate()`
   分别脚本 latest metadata absent、device mismatch、window mismatch；请求同时允许 broadcast+Summon，断言
   `TaskCheckpointTransitionException` 的对应 outcome、Dialog=0、Summon=0、execute=0、UUID=0。
4. `titleHandleAndProcessDriftStopBeforeEitherDelegate()`
   保持 device/window 相同，分别只漂移 latest title、nativeHandle、processId；必须在 Dialog/Summon 前以非成功
   exception 退出，四类计数均为零。不得把“清 cache 后继续 delegate”写成通过。
5. `sameWindowTaskRoundIsIsolatedByTenantUserAndDevice()`
   对 tenant、user、device 三个维度分别使用相同 windowId/task/round，借 public cooldown、formal round open 与
   one-per-round claim 行为证明互不串态；不读取 private map。
6. `nativeFingerprintAbaDoesNotReviveCooldownCacheOrRoundClaim()`
   同 logical scope/window 依次使用 exact A、exact B、exact A initial+latest metadata；B 和第二个 A 都是新的
  合法 rebind，不是 mid-call drift。通过 public due/cache/claim 结果证明旧 A state 不复活。
7. `nullAndHolderFallbackKeepExistingBehaviorWithoutMintingLegacyAuthority()`
   覆盖 null+empty holder 的 `default` 行为，以及 null supplied+bound holder 的 holder fallback。旧
   `TaskExecutionContext(CloudTaskServiceExecutionContext)` constructor 和 legacy epoch public shape 可反射锁住，
   但不得为此创建旧 owner/session/ledger。
8. `sixAutoCombatApisRemainCallableWithoutLifecycleActivation()`
   用上一节 closed/open 两组 public 行为覆盖六 API；timeout 使用 `0` 或已打开 fast path，不 sleep、不起线程。

## 当前字节的预期红灯索引

以下是 test-only 实施前必须保留的静态不兼容索引，不是 reviewer finding 或批准结论：

1. **latest title/HWND/process fence 尚无可见执行路径。**
   `TaskExecutionContext.latestExactTurnMetadata()` 当前只比较 device/window；
   `TaskMaintenanceService.currentIdentityToken()` 读取的是 context 的 initial title/HWND/process，未调用
   `context.getTurnGameClient().latestWindowMetadata()`。因此同 device/window 的 latest title/HWND/process drift
   会越过首 checkpoint；broadcast 还会在任何 summon identity 逻辑之前调用 Dialog。矩阵第 4 项按当前字节预期
   暴露失败，测试不得改成只验证 cache invalidation。
2. **formal team round/window/claim key 尚未带 Cloud scope。**
   per-window `currentWindowKey()` 已带 tenant/user/device/window，但 `normalizeTeamKey()` 与 `teamRoundKey()` 仍只形成
   `task#round`，`activeTeamRoundByKey`、`teamMaintenanceWindowStateByRound` 与
   `summonSkillClaimsByTeamRound` 因此仍有跨 scope 共享路径。矩阵第 5 项按当前字节预期暴露失败。
3. **A->B->A claim 与 cache 不是同一隔离边界。**
   identity drift 会替换 summon cache，却不会清同 formal round 中相同 scoped windowKey 的 retained claim；矩阵第 6
   项必须分别观察 cache 与 claim，不能只证明 cache replacement 后宣称整卡通过。
4. **legacy runtime 夹具与本子卡禁令存在真实边界。**
   轻量 turn-native fixture 不能构造 legacy `CloudTaskServiceExecutionContext`；真实旧 authority 会引入本卡禁止的
   owner/session/ledger。故本 tranche 只做 null/holder runtime fallback + legacy public-shape compatibility。若父级要求
   legacy runtime 行为测试，必须先另行冻结合法 fixture；实施者不得用 Unsafe、伪 delegate 或私有 state 反射凑证据。

由于 `TURN-34BT1` 禁止 production 写入，实施者遇到以上红灯只能保留真实断言并在子卡 delivery 中报告当前源码
不满足；不得改 production、删断言、改 fixture 迎合现状或写通过结论。production repair 是否开卡由父级决定。

## 后续同一 Named Test 的冻结债务

子卡第 34-35 行明确把下列内容留给后续 parent-frozen tranche。它们必须继续追加到同一个
`TaskMaintenanceTurnContractTest.java`，不能建第二测试；本轮 34BT1 不得冒充已覆盖。

### Broadcast 与一次 Summon

- `BUSINESS_OPTION_CLICKED`、`FAILED`、`INTERRUPTED` 各自 short-circuit，Summon 调用数 0；no-action 才允许
  `cleanSummonSkill=true` 的一次 typed delegate。
- Dialog request 的 source 与 full-fallback 原值透传；无第三 fallback、第二 observation 或 background work。
- disabled、interval<=0、non-FREE、not-due、existing unknown interval、fresh tail-safe、无 round、capability closed、
  pathing closed、same-window duplicate、max claim 全部 zero delegate。
- eligible due path 的 scripted Summon `calls==1`、request 精确、TaskMaintenance execute/UUID 都为 0；不得复制
  TURN-33 五删除/PNG/OCR/action fixture。

### Team capability

- pathing open 精确五项：`FIRST_AID/PATHING_WINDOW/COMMON_BOX/SUMMON_SKILL/LEFT_TOP_STATUS`，TEAM_RETURN 关闭；
- weak first-aid 只一项 `FIRST_AID`；
- team close 精确关闭上述五项；
- return support open/close 精确两项 `TEAM_RETURN+COMMON_BOX`；
- duplicate/max claim、known-failure release、state-change retain、capability epoch、leader conflict/absent 与
  all-candidate completion 走 production public API；四个零 caller API 即使在后续测试中被夹具调用，也不能宣称
  production runtime 已激活。

### Terminal、UUID 与 no-retry

- initial metadata STOP：`TaskStopRequestedException`，Dialog=0、Summon=0、execute=0、UUID=0。
- scripted Summon 抛原 `TaskStopRequestedException`：同一 exception 传播，delegate=1，无第二脚本消费，previous
  `ActionState` 恢复，coordinator execute/UUID=0。
- scripted Summon 分别抛 `TaskFatalException("DUPLICATE_OR_UNCERTAIN")` 与 correlation fatal：同一 exception
  传播、delegate=1、零自动 retry、成功 cooldown 不刷新、previous state 恢复。
- known failed/no state change：单次 public invocation delegate=1 且返回既有 failed status；claim 释放可由下一次
  **显式** public invocation观察，不能把第二次显式调用称为 transport retry。
- delete/ultimate state change failure：claim 保留，下一次同 round 显式调用 zero delegate。
- unknown result：第一次 delegate=1 并记录既有 configured interval；紧接的第二次显式调用 deferred 且调用数仍 1。
- success：delegate=1、既有 success status/cooldown/cache 投影一次、previous state 恢复。TURN-33 自身 action/UUID
  1:1 仍由 `SummonSkillTurnContractTest` 负责，本测试不重复也不反向断言其为零。

既有 2h tail-safe/skill-count expiry 也属于最终 named-test acceptance，但当前子卡同时禁止 wall-clock sleep、
TaskMaintenance private reflection 与 production Clock seam。后续 tranche 必须由父级先冻结一种真实、可确定的测试
方式；在此之前不得用即时调用伪装“已覆盖 2h expiry”。

## 领取与交付口径

- External D 只有在 `TURN-34BT1` 物理 EOF 追加真实 `EXTERNAL-D CLAIMED` 后才可写两项写集。
- 实施期间不运行 Maven/JUnit/compile/package，不启动 Spring/HTTP/runtime/application/server/Task/UI/capture/input，
  不执行 Git mutation；后续授权命令仍是 `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`，由 stable-writer gate
  执行。
- delivery 必须记录测试文件 SHA/行数、实际覆盖方法、未运行门和任何当前-production assertion mismatch；不能写
  `APPROVED/CLOSED`，不能把未执行测试写成通过。
- Parent source/test-source review、后续 test tranches、TURN-22 final gate、两名独立 reviewer 与 Cloud build 均独立待办。

PRECHECK_COMPLETE

<!-- TRUE_EOF: CR271 TURN-34B TEST-SLICE PREFLIGHT HELPER PRECHECK_COMPLETE NON-IMPLEMENTER NON-REVIEWER NON-APPROVER TARGET=TURN-34BT1 EXACT-WRITESET=[TaskMaintenanceTurnContractTest.java,append-only-child-card] PRODUCTION-SHA256=963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC 2026-07-16T09:10:48.1594942-04:00 -->
