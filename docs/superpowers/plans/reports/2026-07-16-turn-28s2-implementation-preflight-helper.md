# CR271 TURN-28S2 精确 implementation preflight

- 生成时间：`2026-07-16T10:04:48.3992935-04:00`
- 角色边界：CR271 Internal helper，仅做只读 implementation preflight；不是源码写入者、审查者或状态裁定者。
- 写入边界：本轮只生成本报告；不修改 Java、测试、CR 卡、`ACTIVE_WORK`、dashboard 或其他文件。
- 执行边界：本轮不运行 Maven、JUnit、runtime、应用、服务、Task、UI、截图、OCR 或任何输入，也不执行 Git mutation。

## 1. 已完整核对的权威材料

本次预检已完整读取并交叉核对：

1. `D:/mavenProject/DHXY/AGENTS.md`；
2. `docs/DHXY_CONTEXT.md`；
3. `docs/ACTIVE_WORK.md` 顶部 CR271 当前块；
4. `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节；
5. `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`；
6. `docs/业务逻辑.md`，包括严格修罗基线与 NPC 点击 FIFO/直接战斗约束；
7. `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md`；
8. DHXY 与 `dhxy-cloud-brain` 两仓完整 `git status --short --untracked-files=all`；
9. Cloud 当前 `src/main/java/com/bot/dhxy/service/NpcClickService.java` 全部 3374 行；
10. 现有 public turn client、action factory、context、result、协议模型，以及 DHXY 端后台快捷键执行路径。

适用权威结论如下：

- 业务行为权威仍是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；无已批准业务差异，S2 只能迁移四处物理快捷键机制。
- 每个到达的快捷键调用点必须形成一条闭合 HTTPS action；Cloud 不得自动 retry、replay、resend，也不得创建 session、ledger、TTL 或 durable workflow。
- 本子卡 test write set 为空；TURN-28 父卡稍后独占 `NpcClickTurnContractTest`。
- Java writers 活跃期间禁止 Maven/JUnit/compile/package/runtime/input；后续测试与 build 由父级在 writers 稳定后统一执行。

## 2. 两仓与冻结源码快照

### 2.1 Git 状态

| 仓库 | 分支 | HEAD | porcelain 摘要 | 与本切片直接相关的状态 |
|---|---|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 704 项：43 modified、1 deleted、660 untracked | `ACTIVE_WORK.md`、`DHXY_CONTEXT.md`、DHXY `NpcClickService.java` 已有改动；计划、协议、S2 卡与父级预检报告均未跟踪 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 550 项：9 modified、541 untracked | `pom.xml` 已改；Cloud `NpcClickService.java`、turn client/action factory/context/result 及 migration baseline 均未跟踪 |

因此，Cloud 普通 `git diff` 不能为目标文件提供可靠的逐文件保护。实际实施前必须再次读取文件 SHA 和行证据；若冻结值变化，必须停止并交父级重新冻结，不能覆盖并发工作。

### 2.2 `NpcClickService.java` 冻结证据

- 当前路径：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`
- 当前长度：`175367` bytes
- 当前行数：`3374`
- 当前 mtime：`2026-07-16T08:50:50.2225209-04:00`
- 当前 SHA-256：`CCE8F0203AC90A0D39F7CFF99DDA8D9A616656A55467ED4AE3AA053AD0923441`
- Cloud `migration-baseline/696a12b0/.../NpcClickService.java` 的长度、行数和 SHA-256 完全相同，逐行差异为 `0`。
- 对 DHXY commit `696a12b0...` 的同路径内容执行 3374 行逐行比较，差异为 `0`；Git baseline blob 为 `74d9b26b76b84052718d5679529f7ffeb46e3273`。工作文件与 Git blob 的原始 blob 值不同仅来自工作树换行编码，文本记录没有差异。

上述 SHA 命中 S2 子卡初始冻结值，说明四处业务分支当前仍是严格 696 镜像。

### 2.3 可复用 public API 冻结值

| 文件 | SHA-256 | 本切片用途 |
|---|---|---|
| `turn/client/TurnGameClient.java` | `A8F64D8DBB5F9ED2852975D518836E25AF92073F9C818D5F7E9DA7CF18056CB9` | `bind`、`latestWindowMetadata`、一次 `execute`、内部 UUID 生成 |
| `turn/CloudTurnActionFactory.java` | `81331BCBFC1D4046956AF72C6E8AEA7CAE6A4ED0AC4E296D1D94C6424779E5CB` | 由 client 内部创建并校验 action；业务 Service 不应直接注入 |
| `runner/context/TaskExecutionContext.java` | `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003` | 暴露 exact invocation context、bound client、初始 title/HWND/process 与 checkpoint |
| `runner/context/TaskExecutionContextHolder.java` | `3FA2729917449FBB75BF72614E46A223526EA2ACB53DC96351886559192C6F3B` | 当前线程 exact context 与对象身份栅栏 |
| `turn/client/TurnInvocationResult.java` | `052D9C80A2BFE575514886D1D4EEF30AF6B474F70A713E132FB6D9EF910024A7` | actionId/device/window/step index/type 的基础 correlation |

实际实施开始前应复核这些值。任何 API 漂移都应先重新做 preflight，不能在 `NpcClickService` 内补 adapter 或兼容 wrapper。

## 3. 四个 active Alt shortcut 调用点

当前文件中恰有四个 active top-level `InputAction.pressAlt*()` 机制；其位置与顺序如下。行号是本报告快照行号，实施后会顺移。

| # | 当前位置 | 到达条件 | 严格 696 机制 | 仅成功后允许执行的下一行 |
|---|---|---|---|---|
| 1 | `clickNpcSmart` 624-627 | 第一次 pipeline 已失败、未 stop、且 target 不是 `COMBAT_TARGET` | `ALT_C`，随后 `WAIT 700ms` | 恰好一次 `runNpcClickPipeline(..., "dialog-after-alt-c")` |
| 2 | `tryDirectCombatTargetClick` 667-670 | `detectFlyingState(...) == FLYING` | `ALT_C`，随后 `WAIT 700ms` | 继续到公共 `ALT_A` 入口 |
| 3 | `tryDirectCombatTargetClick` 682-685 | 状态为 `FLYING` 且 dismount 成功，或状态为 grounded/continuing | `ALT_A`，随后 `WAIT 350ms` | direct-combat pipeline 与既有 BattleRadar verifier |
| 4 | `prepareNpcPipelineNameLayerOnce` 948-951 | ordinary pipeline 已到达 name-layer preparation；direct-combat mode 明确跳过 | `ALT_4`，随后 `WAIT 400ms` | 现有 tooltip/yellow/formula/Ctrl 候选顺序 |

必须保留的精确分支事实：

1. `COMBAT_TARGET` 在 generic 第一次 pipeline 失败后仍直接返回 `false`，不会发 generic `ALT_C`，也不会多跑一条 full pipeline。
2. direct-combat 的 `FLYING` 路径是两个独立业务调用点：先一条 `ALT_C/700` action，再一条 `ALT_A/350` action。不能合并成一条 action；两条 action 各自拥有不同 UUID。
3. `UNKNOWN` flying state 当前立即返回，既不发 `ALT_C`，也不发公共 `ALT_A`。grounded/continuing 路径不发 dismount，只发公共 `ALT_A/350`。
4. direct-combat pipeline 明确跳过 `ALT_4`。ordinary pipeline 也可能在 name-layer 之前因已有 dialog 分支或 early learned-memory 成功而返回；这种严格基线早退仍是零 `ALT_4`。准确断言应是“每次到达 name-layer preparation 恰好一条 `ALT_4/400`”，不能写成“每次进入 ordinary pipeline 无条件发 `ALT_4`”。
5. generic retry 若两个 ordinary pipeline 都到达 name-layer，合法顺序可能是第一条 `ALT_4/400`、第一次 pipeline miss、`ALT_C/700`、第二条 `ALT_4/400`；不得去重或把第二次 `ALT_4` 当成 transport retry。
6. 快捷键后的 dialog、OCR、template、yellow、formula、Ctrl、BattleRadar、right-click exit、memory 与 fallback 顺序全部保持原样。

另外两处 private legacy/debug helper 不是本切片目标：

- `captureCleanNameToFileDirect` 3271-3272 的 direct `inputProvider.pressAlt4()` + `TaskSleep.sleep(400)`；
- `captureCleanNameRegionToMemory` 3302-3304 exclusive callback 内的 direct `inputProvider.pressAlt4()` + 400ms wait。

这两处以及所有 mouse/Ctrl/capture/OCR/template/recognizer/input helper 均必须保持不变。

## 4. 可复用 public turn client/action/context 模式

`NpcClickService` 当前已注入 `TaskExecutionContextHolder`，因此不需要增加 `TurnGameClient`、`CloudTurnActionFactory` 或第二个 context provider 字段，也不需要改变 Lombok 构造器。

每个到达的 shortcut 应走同一条现有 public 路径：

1. 从 `taskExecutionContextHolder.current()` 获取非空 `TaskExecutionContext`；缺失即走现有 task-fatal 路径，不回退到 `InputSequences`。
2. 对该 context 直接调用 `TaskCheckpoint.throwIfStopRequested(context, ...)`；不要新增 checkpoint wrapper。
3. checkpoint 返回后，要求 holder 中仍是同一个 context 对象，使用对象身份比较而不是仅比较字段。
4. 取得 `TurnInvocationContext binding = context.getTurnInvocationContext()`。
5. 取得 `TurnGameClient client = context.getTurnGameClient().bind(binding)`。context 已保存 bound view，重复 `bind` 对同一 binding 是幂等的，不创建第二个 bean、transport、exchange、thread 或 lifecycle owner。
6. 读取一次 `client.latestWindowMetadata()`。该读取不生成 UUID、不执行 command，也不缓存新状态。
7. preflight 要求：device/window 与 binding 精确一致；title 非空且与 context 初始 title 一致；HWND 非空且与 context 一致；process id 为正且与 context 一致；rect 非空且 width/height 为正。
8. 在 UUID-producing call 紧前再次要求 holder 仍是同一个 context 对象。
9. 只调用一次 `client.execute(steps, false, Duration.ofMillis(120_000L))`。client 在该调用内部取一个 fresh UUID，再调用其既有 action factory；`NpcClickService` 不生成、不缓存、不传入 UUID。

`120_000ms` 只是一次 command 的正 transport wait fence，不是业务 wait、TTL、重试预算或再次发送授权。

每个调用点的 action 形状只能是：

```text
fullWindowFailureEvidence = false
step[0] = INPUT / KEY_TAP / key=ALT_C|ALT_A|ALT_4
step[1] = WAIT / waitMs=700|350|400
```

`TurnInputSpec` 只填 `key`；坐标、text、scroll、click delay 与 queue hold 均为空。没有 CAPTURE、MATCH_TEMPLATE、LOCAL_SERVICE、frame request、mouse action 或 foreground fallback。DHXY 当前 `TurnKeyMapper` 能解析 `ALT_4`、`ALT_A`、`ALT_C`，三者在 `BoundWindowKeyboardService.AltShortcut` 中都标记为 background HWND supported；`TurnInputStepExecutor` 只通过 exact bound HWND 的 `KEY_TAP` 路径执行，随后才执行独立可中断 WAIT。

## 5. completion、terminal 与 uncertain 的零继续门

### 5.1 唯一可继续条件

只有同时满足以下全部条件，helper 才能返回成功并允许 caller 执行下一条严格 696 业务行：

- `commandStatus == COMPLETED`；
- `outcome != null` 且 `outcome.status == COMPLETED`；
- `TurnInvocationResult` 已完成 actionId、device/window、step count/index/type 的基础 correlation；
- outcome 的完整 `TurnWindowMetadata` 与本次 preflight snapshot 一致，从而覆盖 device/window/title/HWND/process/rect 及控制 metadata drift；
- `failedStepIndex == null`；
- step result 恰好两个且顺序固定：`0/INPUT/COMPLETED`、`1/WAIT/COMPLETED`；
- 两个 step 的 `match`、`localResultJson` 均为空；
- `invocation.frame == null` 且 `outcome.frame == null`。

`TurnInvocationResult.from(...)` 不会替业务 Service 检查 title/HWND/process/rect、两个 step 的 status、`failedStepIndex` 或 no-frame，因此这些检查不能省略。成功只代表快捷键与 baseline wait 已完成，不代表 NPC 找到、dialog 打开、点击成功、战斗进入、OCR 命中或 verifier 成功。

### 5.2 精确终态投影

| 结果 | 投影 | 该调用点之后允许的动作数 |
|---|---|---|
| command `BUSY` | task-fatal | 0 |
| command `DUPLICATE_ACTION_ID` | task-fatal | 0 |
| command `TIMED_OUT_UNCERTAIN` | task-fatal；绝不 resend | 0 |
| command `INTERRUPTED_UNCERTAIN` | 先走现有 checkpoint；若未确认 stop，则 task-fatal | 0 |
| outcome `FAILED` | task-fatal，不映射为原有 boolean miss/skip | 0 |
| outcome `STOPPED` | 走现有 checkpoint；若 checkpoint 未确认 stop，则 task-fatal | 0 |
| outcome `DUPLICATE_OR_UNCERTAIN` | task-fatal；绝不 replay | 0 |
| missing/malformed outcome、runtime client exception | task-fatal | 0 |
| holder/context drift、metadata drift、step/frame correlation defect | task-fatal | 0 |

checkpoint 抛出的现有 `TaskStopRequestedException` 应原样向上抛；不要被宽泛 `RuntimeException` catch 包装为普通失败。其他 runtime/correlation 异常可附 cause 转为现有 `TaskFatalException`。尤其不能照搬其他 Service 对已知 mechanical `FAILED` 返回结构化 miss 的做法，因为 S2 子卡明确规定这里的 `FAILED` 是 terminal。

按四处 caller 投影，“零继续”具体意味着：

- generic `ALT_C` 不确定或失败后，不得进入第二条 pipeline；
- flying `ALT_C` 不确定或失败后，不得再发 `ALT_A`；
- `ALT_A` 不确定或失败后，不得进入 direct-combat pipeline，也不得追加 right-click compensation；
- `ALT_4` 不确定或失败后，不得继续 tooltip/OCR/template/candidate/click/verifier/Ctrl；
- 任何 terminal 都不得回退到原 `InputSequences`、`InputProvider` 或 foreground input。

## 6. 最小 in-place 修改方案

建议的最小源码形状是“必要 imports + 四处 RHS 替换 + 一个真实边界 helper”，不移动现有业务块：

1. 增加现有协议/client/context/result、`TaskExecutionContext`、`TaskFatalException` 与 `Duration` 的必要 imports。
2. 四处保留原 boolean 局部变量、日志、`shouldStop()` 后置门、return/result 与下一业务调用，仅把 `inputSequences.submitAndWait(...)` RHS 换成同一个 private helper 调用，并传入原 description、精确 key 和精确 wait。
3. 新增一个 private helper；它独立拥有 context preflight、两步 action 创建、一次 public execute、完整 correlation 与 terminal 投影，成功时只返回 `true`，其他情况只抛 stop/fatal。
4. helper 要有简短 JavaDoc，说明 key、wait 毫秒单位、exact-context/no-retry/no-frame 与 terminal 语义。这是安全敏感物理输入边界，不应省略说明。
5. 不再新增 prepare/resolve/handle 等转发层，不增加 nested wrapper，不新增 service/facade/model/enum，不直接调用 action factory。
6. 不删除 `InputSequences`、`InputAction`、`InputProvider` 或 `TaskSleep` 字段/import；它们仍被文件中其他未迁移机制使用。

这一个 helper 是合理的真实边界：四处共享约 1:1 的 HTTPS invocation/correlation/terminal 规则，仅 key、wait 和诊断 source 不同。把同一大段 validation 内联四次会增加漂移风险；再拆成多个一行 wrapper 则违反 no-wrapper-nesting 规则。

实施者必须保留现有 description 语义用于日志定位：generic dismount、direct-combat dismount、direct-combat mode entry、pipeline name-layer preparation。不得借机改日志、候选排序、验证次数、fallback、retry、right-click exit 或 OCR/capture 时机。

## 7. 测试与 compile 风险

### 7.1 本子卡不写、不跑测试

- S2 的 test write set 明确为空。
- 当前 Cloud 仓不存在 `src/test/java/com/bot/dhxy/service/NpcClickTurnContractTest.java`，也不存在其他 `NpcClick*Test`；它是 TURN-28 父卡后续唯一测试文件，不属于本切片补建范围。
- 父级授权测试族是 `HTTPS_TURN_CONTRACT_TEST_FAMILY`；TURN-28 默认断言 `BC4+BASE`，额外 profiles 为 `IMG+LX`。
- writers 稳定且父级测试源码完成后，计划中的 Cloud 命令别名对应 `mvn -q -Dtest=NpcClickTurnContractTest test`。本报告没有执行该命令。

父级测试至少要避免以下误判：

1. generic 非 combat 首次 miss 后恰好一条 `ALT_C/700` 和最多一条第二 pipeline；`COMBAT_TARGET` 为零 generic `ALT_C`。
2. direct `FLYING` 是 `ALT_C/700` 后 `ALT_A/350` 两条独立 action；`UNKNOWN` 为零 action；grounded/continuing 只有 `ALT_A/350`。
3. ordinary pipeline 只有在到达 name-layer preparation 时才发一条 `ALT_4/400`；early memory/dialog return 为零，direct-combat 为零。
4. 每个 reached site 一个 fresh UUID、一个 command、两个 ordered completed step、零 frame；两处 `ALT_C` 不共享 UUID。
5. 所有 command/outcome uncertain/failure、metadata/context drift 与 malformed result 都断言零 later command、零 fallback、零 fabricated success。
6. 测试必须经过真实 public `TurnGameClient` 与 fake command port，不能把 `NpcClickService` 接到同步 local input fake。

### 7.2 compile 风险要分层归属

本切片新增引用所需类型当前均存在，且 `TaskExecutionContextHolder` 已注入，所以不需要 Spring wiring 或构造器修改。主要局部 compile 风险是 imports/record constructor 参数顺序、`Long waitMs` 装箱、异常 catch 顺序，以及遗漏 `Duration`/result/status 类型。

全仓另有显著的既有迁移风险：在本快照中，Cloud `NpcClickService.java` 的 12 个既有 internal imports 尚无对应 Cloud source 文件，包括 `GameClientTracker`、`TextRecognizer`、`InputProvider`、`GameStateUtil`、`CoordinateHelper`、四个 vision service，以及三个 window runtime 类。这些缺口早于 S2，且均不在 S2 write set；不得为了让本切片独立 compile 而顺手复制或修改它们。

Cloud `pom.xml` 当前要求 Java 21，并通过 enforcer 禁止 test-skip flags。权威计划规定 writers 稳定后的非测试 source gate 为 `mvn -q clean compile`，最终 package 另需显式授权并会运行现有 tests。若后续 compile 首错落在上述未迁移依赖，应归属对应迁移 owner，不能把它误报为 S2 shortcut 回归；若首错落在 S2 新 helper/import/action 构造，则由 S2 源码写入者修复。

## 8. 实施前停止条件与预检结论

以下任一情况出现，都应停止 S2 Java 修改并交父级重新冻结：

- `NpcClickService.java` 不再是 3374 行或 SHA-256 不再是 `CCE8F...3441`；
- 本报告第 2.3 节任一 public API hash 漂移；
- Cloud 目标文件出现另一个并发写入者或来源不明变化；
- 实现需要触碰四处以外的 Alt/mouse/Ctrl/capture/OCR/template/recognizer/dialog/BattleRadar/navigation/caller；
- 实现方案需要新增业务 retry、fallback、frame、第二 command、foreground input、session/ledger/TTL 或 local truth；
- terminal 只能通过返回 `false`/`skipped` 才能接入，无法保持零继续 fatal/stop 投影。

在上述冻结值保持、父级确认单一源码写入窗口的前提下，TURN-28S2 的实现路径已经精确到四个调用点、一个 public turn 边界 helper、三组 key/wait 参数及完整终态矩阵，可以做最小 in-place 源码实现。本结论仅表示 implementation preflight 已完成，不改变 CR 状态，也不替代父级测试、compile、独立审查或最终运行验收。

TRUE_EOF PRECHECK_COMPLETE
