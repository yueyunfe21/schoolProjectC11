# CR271 TURN-28S2 production call-site acceptance preflight

## 1. 角色、范围与只读快照

- 角色：CR271 Internal helper，仅做 TURN-28S2 production call-site acceptance preflight；不是实现者，也不承担 CR 状态裁决。
- 唯一写入：本报告。Cloud sibling `D:\mavenProject\dhxy-cloud-brain` 全程只读。
- 已完整读取：仓库 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、HTTPS turn 协议、`docs/业务逻辑.md` 的 `696a12b0` 对应规则、TURN-28S2 原卡直到第 181 行 true EOF，以及当前 Cloud `NpcClickService.java` 全部 3374 行。
- 写报告前复核的 Cloud 生产文件：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`，3374 行，SHA-256
  `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`；与
  `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java` 字节一致。
- TURN-28S2 原卡当前 true EOF 是 `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md:168-181`：fresh External B 仍需先认领，当前零 owner，生产 SHA 未变。
- 本次没有运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input；没有执行 Git mutation。

## 2. 权威约束定位

| 权威材料 | 精确证据 | 本切片需保持的事实 |
|---|---|---|
| `docs/DHXY_CONTEXT.md` | `:14-16`, `:35-48`, `:60-66` | HTTPS turn 是当前迁移权威；不恢复 session/ledger；严格保持 `696a12b0` 的阶段、顺序、retry/fallback 和业务判断。 |
| `docs/ACTIVE_WORK.md` | `:3-15`, `:63-64`, `:94-96`, `:209-219` | 当前 assignment 只核四处生产 Alt 调用点；目标 SHA 和 one-file production slice 未变化。 |
| `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` | 第 14-19 节，`:1095-1776`；尤其 `:1133-1155`, `:1264-1268`, `:1374-1376`, `:1490-1544`, `:1718`, `:1763-1776` | 每次 public call 一 UUID、共享 exchange、exact-window、无传输重试；TURN-28 后续唯一命名测试由父级处理；切片写集不可外扩。 |
| `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` | `:48-80`, `:108-126`, `:151-157`, `:197-222`, `:294-308`, `:335-369` | 一个 action 是一个封闭 ordered operation；INPUT 与 WAIT 分步且有序；不自动重复物理动作；Cloud 只在新的业务决定下创建新 action。 |
| `docs/业务逻辑.md` | `:1253-1299`, `:1301-1380`, `:1419-1426` | 修罗以 commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 为行为基线；保留 `ACCEPT_TASK_CLICK_NPC` / `CLICK_TARGET_NPC` 的点击、直接战斗 fallback、stop 与 retry 语义；本切片不引入新的业务真值。 |
| TURN-28S2 原卡 | `:10-17`, `:19-36`, `:38-44`, true EOF `:168-181` | 只替换四个 active 顶层键盘 mechanic；每个 reached site 一个真实 public `TurnGameClient.execute(...)`；只有 exact `COMPLETED/COMPLETED` 可继续，其他终态零后续动作；两个 legacy private site 不动。 |

## 3. 四个 active 顶层调用点

以下行号均基于上述 3374 行、SHA 为 `cce8f...3441` 的当前 Cloud 文件。

### 3.1 Site 1：generic retry `ALT_C -> WAIT 700`

- 类/方法：`com.bot.dhxy.service.NpcClickService#clickNpcSmart`，`NpcClickService.java:599-634`。
- 到达前置顺序：
  1. 首次 `runNpcClickPipeline(request, verifier, "dialog")` 在 `:601-603` 成功即返回。
  2. `shouldStop()` 在 `:604-606` 为真即返回。
  3. 非空 `COMBAT_TARGET` 在 `:607-617` 记录“skips Alt+C retry”并返回，绝不能进入本 site。
  4. 其余首次 pipeline miss 才在 `:618-623` 先记录“press Alt+C before retry”。这包括当前基线允许的 `request == null` 路径；本切片不得新增 null gate。
- 当前 mechanic：`:624-627` 的一个 `inputSequences.submitAndWait(...)`，严格为 `pressAltC()` 后 `sleep(700)`。
- 原 fallback/return/log 顺序：`:628-631` 的 submit/stop warning 与 `false`，之后只有 `:633` 的第二次 `runNpcClickPipeline(..., "dialog-after-alt-c")`。
- 父级验收焦点：
  - pre-action info log、COMBAT_TARGET 排除、旧 stop gate 和第二 pipeline 的相对位置不变。
  - 只有本 site 的 exact two-step completion 才能到 `:633`；turn terminal 不能伪装成 `dismountSubmitted=false` 再走旧 warning/return。
  - terminal 诊断应在共享 turn 边界带 operation/action/status/code 传播；不得为了复用旧 boolean warning 而吞掉 terminal。

### 3.2 Site 2：confirmed-flying direct-combat dismount `ALT_C -> WAIT 700`

- 类/方法：`NpcClickService#tryDirectCombatTargetClick`，`:653-709`。
- 到达分支：null 与 pre-stop 在 `:654-660` 返回；`:662-665` 检测并记录 `FlyingState`；只有 `FLYING` 的 `:666` 分支到达本 site。
- 当前 mechanic：`:667-670` 的 `pressAltC()` 后 `sleep(700)`。
- 原 fallback/return/log 顺序：mechanic 返回后先在 `:671-672` 记录 confirmed-flying submit 结果，再由 `:673-675` 的 submit/stop 条件返回 skipped；成功后顺序落入 Site 3。
- 明确零动作分支：`UNKNOWN` 在 `:676-680` warning 后返回，既不执行 Site 2，也不执行 Site 3。
- 父级验收焦点：
  - 只有 `FLYING` 到达 Site 2；`NOT_FLYING` 不可被额外 dismount。
  - exact completion 后仍按原顺序继续 Site 3；任何 terminal 在 Site 2 结束该路径，Site 3 与其后的 NPC pipeline 均为零动作。
  - 原 info/return 顺序应在成功路径保持；turn terminal 不能合成为 `submitted=false`。共享边界需留下 terminal 诊断后直接传播。

### 3.3 Site 3：direct-combat mode entry `ALT_A -> WAIT 350`

- 类/方法：`NpcClickService#tryDirectCombatTargetClick`，`:682-709`。
- 到达分支：
  - `FLYING`：仅在 Site 2 exact completion 且原 `shouldStop()` 未触发后到达。
  - `NOT_FLYING`：跳过 Site 2，直接到达。
  - `UNKNOWN`：`:676-680` 已返回，零 Site 3 动作。
- 当前 mechanic：`:682-685` 的 `pressAltA()` 后 `sleep(350)`。
- 原 fallback/return/log 顺序：`:686-689` submit/stop warning 与 skipped；`:691-693` 成功后记录 mode-likely；`:695` 才运行 `runNpcClickPipeline(..., "direct-combat")`；`:696-708` 保持 combat success/stop、右键退出确认、position-refresh-required 的既有次序。
- 父级验收焦点：只有 Site 3 exact completion 可到 mode-likely log 和 direct-combat pipeline；terminal 时不得运行 pipeline、右键退出、位置刷新或任何后续恢复动作。

### 3.4 Site 4：ordinary pipeline name layer `ALT_4 -> WAIT 400`

- 类/方法：
  - 调用点 `NpcClickService#runNpcClickPipeline`，`:778-934`，实际分支 `:810-846`。
  - mechanic owner `NpcClickService#prepareNpcPipelineNameLayerOnce`，`:944-955`。
  - 400ms 常量 `NPC_PIPELINE_HIDE_PLAYER_NAMES_SETTLE_MS` 在 `:135`。
- 到达分支：
  1. `:784-788` stop/invalid request 可先返回。
  2. `:810-839` 的 existing dialog / early learned-memory 分支可先返回或成功，未走到 name layer。
  3. `verificationMode == "direct-combat"` 在 `:841-843` 明确跳过 Alt+4。
  4. 只有普通 pipeline 落到 `:844`，才调用本 site；helper 的 pre-stop 在 `:945-947` 仍可返回。
- 当前 mechanic：`:948-951` 的 `pressAlt4()` 后 `sleep(400)`；`:952-954` 先记录 preparation result，再返回 `ok && !shouldStop()`。
- 原 fallback/return/log 顺序：`:844-846` 的 false 立即结束 pipeline；exact success 后从 `:848` 继续原 tooltip/yellow/formula/Ctrl 策略顺序。`:919-927` 的既有 `RuntimeException` catch 记录并重抛，`:928-933` 的 latency finally 始终执行，因此 turn terminal 不得在此被吞成普通 miss。
- 父级验收焦点：每次普通 pipeline 的动态到达各自形成一个新 action；direct-combat pipeline 永远不因本切片新增 Alt+4；terminal 时 `:848` 之后所有策略动作均为零。

## 4. 动态调用拓扑与 one-action 计数

这四个是四个 lexical mechanics，不等于一次顶层调用最多一个 action。父级应按每个动态 reached site 计数：

| 业务路径 | 合法到达序列 | action 计数语义 |
|---|---|---|
| 普通 `clickNpcSmart` 首次 pipeline 在 name-layer 后成功 | Site 4 | 一个 fresh UUID、一个 command。 |
| 普通首次 pipeline miss，generic retry 后第二 pipeline 成功/失败 | Site 4 -> Site 1 -> Site 4 | 三次独立业务到达，必须是三个 fresh UUID、三个 command；第二个 Site 4 不是 transport retry。任一 terminal 后余下计数为零。 |
| 首次 pipeline 在 Site 4 之前的 early branch 成功/返回 | 无，或随后按原条件到 Site 1 | 未到达 Site 4 就不能预发 action。 |
| `COMBAT_TARGET` 走 `clickNpcSmart` miss | 可能有首次普通 Site 4；随后 Site 1 明确为零 | `:607-617` 的既有 generic Alt+C 排除不变。 |
| direct combat，`FLYING` | Site 2 -> Site 3 | 两个独立 action；不可合成一个 action，也不可复用 UUID。Site 2 terminal 后 Site 3 为零。 |
| direct combat，`NOT_FLYING` | Site 3 | Site 2 为零。 |
| direct combat，`UNKNOWN` | 无 | Site 2、Site 3、direct pipeline 均为零。 |
| direct-combat pipeline | 无 Site 4 | `:841-843` 继续排除 repeated Alt+4。 |

对每个实际到达点统一验收：一次 `TurnGameClient.execute(...)`、一次由该 public invocation 生成的 UUID、一次 `CloudTurnCommandPort.execute(...)`，无本地 loop/catch resend、无相同 actionId replay、无 terminal 后新 action。`NpcClickService.java:2123` 的现有 `UUID.randomUUID()` 是 `PendingSmartClickEvidence.proofToken`，不是 turn actionId，不得改造或复用为这四处 action identity。

## 5. 可复用的最小 HTTPS ordered-action 边界

### 5.1 已存在的 public API

- `com.yueyunfe.dhxy.cloudbrain.turn.client.TurnGameClient`：
  - 类契约 `TurnGameClient.java:20-25` 已明确每次 valid call 一 UUID、一 command、无 retry/cache/lifecycle/business interpretation。
  - public `execute(...)` 在 `:107-126`，保留 ordered steps，不拆分、不重排、不 retry。
  - private `invoke(...)` 在 `:161-168`：`:164` 生成一个 UUID，`:165` 构造一个 action，`:166-167` 调用一次 `commandPort.execute(...)`。
  - `currentExactContext()` 在 `:171-180` 拒绝 bound context 漂移；`latestWindowMetadata()` 在 `:149-159` 只读 metadata，不创建 UUID/command/cache。
- `TaskExecutionContextHolder#current` 在
  `com.bot.dhxy.runner.context.TaskExecutionContextHolder.java:33-35`；当前 `NpcClickService` 已注入 holder（`NpcClickService.java:113`）。
- `TaskExecutionContext#getTurnInvocationContext`、`getTurnGameClient` 在
  `com.bot.dhxy.runner.context.TaskExecutionContext.java:250-264`；title/HWND/process getters 在 `:145-174`；turn metadata checkpoint 与 generation retirement 在 `:397-466`。
- `TaskCheckpoint.throwIfStopRequested(...)` 在
  `com.bot.dhxy.runner.stop.TaskCheckpoint.java:25-34`、`:43-52`；interruption 转正常 stop exception 在 `:82-85`。

### 5.2 最小 payload 形状

每个 site 只需同一个有真实边界的 private ordered-key operation；父级验收其行为，不要求增加 wrapper 链：

1. 从当前 holder 取得唯一 task context，并经现有 `TaskCheckpoint` 做 stop/pause/generation checkpoint；无 context、context transition 或 exact binding 缺失不能降级到旧 `InputSequences`。
2. 取得一个 expected `TurnWindowMetadata` 快照，逐字段核对当前 context 的 `deviceId/windowId/windowTitle/nativeHandle/processId`，并要求 `windowRect` 非空、宽高为正。返回 outcome 必须与该完整 metadata record 相等，因而 rect/pause/stop 也参与 correlation。
3. 构造且仅构造两个有序 step：
   - index `0`：`TurnStepType.INPUT`、`TurnInputAction.KEY_TAP`，`TurnInputSpec.key` 分别为 `ALT_C`、`ALT_A` 或 `ALT_4`；其坐标、text、delay、capture、match、localService 字段均为空。
   - index `1`：`TurnStepType.WAIT`，`waitMs` 分别为 `700L`、`350L` 或 `400L`；其余字段为空。
4. 对 exact-context-bound `context.getTurnGameClient()` 调用一次
   `execute(orderedSteps, false, positiveTimeout)`；`false` 表示此纯 input/wait action 不请求 frame。timeout 只控制这一次 wait，不能触发第二次 execute。
5. exact completion 返回给原 boolean 位置一个 success 值；其余状态通过 stop/fatal 边界离开，不能返回普通 `false` 让 caller 继续旧 fallback。

协议 DTO 证据：

- `TurnStep.java:3-11`；`TurnInputSpec.java:5-27`；`TurnInputAction.java:3-14` 的 `KEY_TAP`；`TurnStepType.java:3-8` 的 `INPUT/WAIT`。
- `TurnProtocolValidator.java:67-92`, `:125-168`, `:200-216` 校验 action、step index/shape 与 key-only input。
- DHXY read-only 映射和执行链：
  `TurnKeyMapper.java:19-34` 映射 `ALT_*`；
  `BoundWindowKeyboardService.java:293-306` 已含 `ALT_4/ALT_A/ALT_C`；
  `TurnInputStepExecutor.java:42-99`, `:153-165` 分别执行 HWND keyboard 与独立 WAIT；
  `LocalTurnActionExecutor.java:53-133`, `:175-187`, `:242-247` 按序执行并在 terminal 后停止后续 step，不做 retry。

### 5.3 现有 API 已覆盖与 caller 仍须补齐的 correlation

- `TurnInvocationResult.java:49-71`, `:78-121` 已验证 invocation/outcome actionId、device/window、step count/index/type，以及 frame metadata/raw frame 配对。
- 它没有替 Site 4 caller 验证完整 title/HWND/process/rect snapshot、两个 step 的 `COMPLETED` status、success 时 `failedStepIndex == null`、`match/localResultJson == null` 和“本 action 必须零 frame”。这些仍是 production call-site 边界的验收项。
- `AutoCombatPanelService#executeOrdered/#requireTerminalOutcome` 的可复用先例在
  `AutoCombatPanelService.java:581-677`；ordered key/wait builder 在 `:874-890`。S2 不可照搬其 `allowKnownFailure=true`：原卡要求 `FAILED` terminal。

## 6. terminal / uncertainty 传播矩阵

`CloudTurnCommandResult.Status` 定义于
`com.yueyunfe.dhxy.cloudbrain.turn.CloudTurnCommandResult.java:104-109`；single-slot、wait-once、timeout/interruption 保持 fenced 且不 retry 的实现位于 `CloudTurnExchange.java:31-36`, `:48-88`。`TurnOutcome.Status` 在协议 `TurnOutcome.java:16-20`。

| 返回层 | 状态/异常 | 本 site 后续 |
|---|---|---|
| command | `COMPLETED` | 继续验证真实 outcome；这本身不是业务成功。 |
| command | `INTERRUPTED_UNCERTAIN` | 先走现有 `TaskCheckpoint`；若 checkpoint 确认 stop/interruption，传播 `TaskStopRequestedException`；否则走现有 `TaskFatalException`。零后续 action。 |
| command | `BUSY` / `DUPLICATE_ACTION_ID` / `TIMED_OUT_UNCERTAIN` | `TaskFatalException`，带 operation、actionId、status/code；零后续 action。 |
| outcome | `COMPLETED` | 仅完整 metadata 相等、恰好两个 ordered step、两者 status 均 `COMPLETED`、无 failed index/match/local result/frame 时，才返回 success 并执行下一条 strict-696 业务语句。 |
| outcome | `STOPPED` | 走现有 checkpoint；若未确认 stop，则转 fatal；零后续 action。 |
| outcome | `FAILED` | fatal；不得作为旧 submit `false`、普通 NPC miss 或 known mechanical failure 继续 fallback。 |
| outcome | `DUPLICATE_OR_UNCERTAIN` | fatal；零后续 action。 |
| validation | malformed、action correlation、context/binding/metadata drift、step count/index/type/status/shape drift、unexpected frame | fatal，并保留 cause/operation/correlation 诊断；零后续 action。 |

传播路径核对：Site 1 和 Sites 2/3 外围没有会吞掉这些 terminal 的 catch；Site 4 位于 `runNpcClickPipeline` 的 `RuntimeException` catch 内，但 `:919-927` 明确重抛，finally 仅记录 latency。未来改动不得新增 catch-and-false、catch-and-retry 或 terminal 后旧 `InputSequences` fallback。

## 7. 明确排除的两个 legacy private Alt+4 site

这两个 site 不属于四个 active 顶层 mechanics，未来 production patch 必须保持原字节和原调用关系：

1. `NpcClickService#captureCleanNameToFileDirect(...)`：`:3257-3275`；legacy raw Alt+4 在 `:3271`，`TaskSleep.sleep(400)` 在 `:3272`。当前唯一显式调用 `:524` 传 `prepareAlt4=false`；默认 `true` overload 没有本文件 caller。不得迁移、删除、重定向或顺带清理。
2. `NpcClickService#captureCleanNameRegionToMemory(...)`：`:3277-3315`；exclusive callback 内 legacy raw Alt+4 在 `:3302`，400ms wait 在 `:3303`。普通 pipeline 的 yellow path `:1947` 显式传 `false`；formula path `:2913` 透传 `prepareAlt4`，而默认 formula overload `:2865-2876` 可传 `true`，direct-combat exit anchor path `:757-775` 是现存调用之一。无论动态可达性如何，本切片都不得迁移此 private site。

两个 legacy site 也不得被新的 shared turn helper 间接接管；否则会改变 capture/formula/direct-exit 的业务范围，并破坏原卡的 explicit exclusion。

## 8. 未来父级逐文件验收清单

### Cloud production 与基线

- [ ] `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`
  - diff 只含四个 active RHS/调用点所需替换、一个最小 shared ordered-key boundary、必要 imports/单次 timeout constant/诊断；public API 与其它业务策略不变。
  - Site 1 保持 `:601-633` 的 first pipeline、stop、COMBAT_TARGET exclusion、pre-log、second pipeline 顺序。
  - Sites 2/3 保持 `:654-708` 的 null/stop、flying detect、FLYING/UNKNOWN/NOT_FLYING 分支、mode log、pipeline、exit/refresh 顺序。
  - Site 4 保持 `:810-954` 的 early branches、direct-combat exclusion、strategy fallback、info/catch/finally 顺序。
  - 四个 reached site 均为 exact two-step public execute；terminal 零后续；无 legacy `InputSequences` fallback。
  - `:3271-3272` 与 `:3302-3303` 两个 legacy private site 及其 callers 原样保留；`:2123` proof UUID 原样保留。
- [ ] `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\NpcClickService.java`
  - 保持只读。用它复核四个 mechanic 周边业务分支与 fallback 等价，不把 migration plumbing 差异误当业务变更。

### Cloud HTTPS/context/protocol 依赖（均应保持只读）

- [ ] `...\turn\client\TurnGameClient.java`：确认生产调用 public `execute`，没有新 client/exchange/UUID supplier/retry wrapper。
- [ ] `...\turn\client\TurnInvocationResult.java`：确认调用方没有假定该 record 已代做完整 metadata/step-status/no-frame 验证。
- [ ] `...\turn\CloudTurnCommandPort.java`、`CloudTurnCommandResult.java`、`CloudTurnExchange.java`、`CloudTurnActionFactory.java`：无切片改动；一次 publish/wait 与 uncertainty fence 保持原状。
- [ ] `...\cloud\turn\protocol\TurnStep.java`、`TurnInputSpec.java`、`TurnInputAction.java`、`TurnStepType.java`、`TurnWindowMetadata.java`、`TurnOutcome.java`、`TurnStepResult.java`、`TurnProtocolValidator.java`：无 schema/validator 扩写。
- [ ] `...\runner\context\TaskExecutionContext.java`、`TaskExecutionContextHolder.java`、`...\runner\stop\TaskCheckpoint.java`、`TaskStopRequestedException.java`、`...\runner\exception\TaskFatalException.java`：只复用现有 context/checkpoint/fatal 路径，不新建 lifecycle/owner/session 层。
- [ ] `...\service\AutoCombatPanelService.java` 与 `...\remote\CloudDialogDetectionPort.java`：仅作为现有调用范式参考，不能因 S2 抽公共框架或改变其行为。

### DHXY 执行端（全部只读）

- [ ] `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnKeyMapper.java`
- [ ] `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\driver\BoundWindowKeyboardService.java`
- [ ] `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnInputStepExecutor.java`
- [ ] `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\LocalTurnActionExecutor.java`

父级对以上四个文件只核现有 `ALT_C/ALT_A/ALT_4` 映射、HWND keyboard、WAIT 顺序与 terminal stop；S2 不产生 DHXY diff。

### 卡片、计划与后续测试

- [ ] `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md`：未来 owner 只按 append-only claim/delivery 规则写最终 SHA 与精确行证据；本 helper 报告不替代 owner delivery。
- [ ] `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\service\NpcClickTurnContractTest.java`：属于 TURN-28 父级后续唯一命名测试门禁，不在 S2 写集；父级稳定 writer 阶段再核 four-site reach、ordered steps、fresh UUID、one command、terminal zero-later 和两个 legacy exclusion。
- [ ] 权威计划、协议、`docs/业务逻辑.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md`、CR card/table/dashboard：S2 production patch 不得产生旁支改动；流程材料仅由对应 owner/父级按既有规则更新。

## 9. 禁止扩写边界

- 不增加或恢复 session、ledger、owner、authorization、TTL、durable workflow、cache、transport retry、replay、resend、dedupe 或 background worker。
- 不把两个/三个动态业务到达合并为一个 action；不跨 reached site 复用 UUID；不把第二 pipeline 的 Site 4 当作重试而去重。
- 不改 mouse、Ctrl、capture、template、OCR、recognizer、dialog、BattleRadar、memory、navigation、caller、task phase、fallback priority、verification count 或 expiry 语义。
- 不改 `GameStateUtil.detectFlyingState`、`shouldStop()` 或现有业务 detector 来规避 turn terminal；不把 UNKNOWN、runner negative signal 或 local screenshot/OCR 结果升级为新业务真值。
- 不迁移、删除、重命名或包装 `:3271-3272`、`:3302-3303` 的 legacy private mechanics。
- 不新增同层 wrapper nesting；一个 shared ordered-key operation 已是足够的真实边界，caller 仍应清楚显示原分支和后续业务语句。
- 不为 S2 修改测试、POM、resource、protocol/context/client、DHXY 执行端、其它 service/recognizer 或任何 dashboard 数据。
- 不在 writer 活跃期启动 build/runtime；后续 compile、命名测试、双 repo 稳定构建与 runtime evidence 由父级门禁处理，本报告不替代这些步骤。

TRUE_EOF PRECHECK_COMPLETE
