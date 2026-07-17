# CR271 TURN-28S3 readiness preflight helper

- 生成时间：`2026-07-16T10:27:16.4202920-04:00`
- 角色：CR271 Internal readiness helper，只做下一 production 小片的只读合同收敛。
- 性质：本报告不是 reviewer 意见，不包含 `Approved`、`Blocked`、P0/P1/P2 或 CR 状态裁定，也不授权 Java 开工。
- 实际写入：仅本报告；未修改 Java、测试、CR 卡、`ACTIVE_WORK`、dashboard 或其它文件。
- 执行边界：未运行 Maven、JUnit、compile/package、runtime/application/server、Task、UI、capture、OCR 或 input，未执行 Git mutation。

## 1. 非绑定选择结论

S2 冻结的四个 active Alt shortcut 之后，建议下一张最小且真实推进 TURN-28 的 production 子片为：

> **TURN-28S3：direct-combat candidate pipeline 非 STOP 失败后，退出直接战斗模式的右键动作 HTTPS-turn cutover。**

该片只迁移当前 `NpcClickService.exitDirectCombatClickModeAfterFailure(...)` 内唯一 active
`inputSequences.submitAndWait("npcClick:directCombat:exitRightClick", ...)`。它不重复 S1 的 pending-proof
source 收敛，也不重复 S2 的 generic `ALT_C/700`、flying `ALT_C/700`、direct-combat `ALT_A/350`、ordinary
name-layer `ALT_4/400` 四处 shortcut。

这是剩余 mechanics 中最高价值的单调用点小片：它直接移除 direct-combat fatal cleanup 路径上的本地物理右键，
同时保持一个文件、一个 active call site、一个 closed action 形状。普通左键 helper、Ctrl menu、capture/OCR/template
都跨多个策略或多个 mechanics 边界，不属于同等大小的下一片。

本结论仅是 readiness candidate，不是 parent freeze、claim、source review 或批准。

## 2. 已完整读取的权威证据

本轮完整读取并交叉核对：

1. `D:/mavenProject/DHXY/AGENTS.md`。
2. `docs/DHXY_CONTEXT.md`。
3. `docs/ACTIVE_WORK.md` 顶部 CR271 当前块。
4. `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
5. `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` 全文。
6. `docs/业务逻辑.md` 全文，包括 strict 修罗基线与 NPC click FIFO/direct-combat 条目。
7. TURN-28 父卡及 TURN-28S1、TURN-28S2 fixed card 的物理 true EOF。
8. Cloud 当前 `NpcClickService.java` 全部 3374 行。
9. strict commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 中同一方法与当前源码的对应片段。
10. 当前 public `TurnGameClient`、`TurnInvocationContext`、`TurnInvocationResult`、
    `CloudTurnCommandResult`、`TaskExecutionContext`、`TaskExecutionContextHolder` 全文。
11. 当前 `TurnAction`、`TurnStep`、`TurnInputAction`、`TurnInputSpec`、`TurnOutcome`、
    `TurnStepResult` 协议模型。
12. TURN-28S2 parent-freeze 与 implementation preflight，以及现有 TURN-28S3 只读预检证据。

唯一业务权威是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。本片只能移动右键 mechanics ownership，
不得改变 business decision、attempt budget、probe、wait、return 或 caller throw。

## 3. 当前快照与 S3 initial SHA

### 3.1 当前 pre-S2 源码快照

| 项目 | 当前观察值 |
|---|---|
| Cloud production 文件 | `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java` |
| 行数 | `3374` |
| 字节数 | `175367` |
| SHA-256 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` |
| S2 true-EOF 状态 | `PARENT FROZEN ... EXTERNAL-B-REPLACEMENT-READY CLAIM-REQUIRED`；尚无 `CLAIMED` 或 `SOURCE DELIVERED` |

当前 SHA 与 S1 source-pass 后、S2 落盘前的 strict-696 bytes 相同。它是本 readiness 的
`preS2SnapshotSha256`，不是未来串行 S3 可以直接使用的 claim-time initial SHA。

### 3.2 S3 必须采用的 initial SHA 规则

未来 S3 卡可写入的唯一合法值为：

`S3.initialSha256 = TURN-28S2 true-EOF EXTERNAL-B SOURCE DELIVERED 中记录的 finalSha256`

该值目前尚未产生，因此本 helper 不能编造一个固定 S3 initial SHA。S3 claim 前必须同时满足：

1. S2 四个 shortcut 已 source delivered。
2. parent 已完成 S2 source review 并释放 `NpcClickService.java` owner。
3. 重新读取 S2 卡物理 true EOF，取得其 final SHA。
4. 当场计算当前 production 文件 SHA，必须与 S2 final SHA 完全相等。
5. 把这个相等值写成 S3 initial SHA 后，External 才能从该字节增量编辑。

若任一步不成立，S3 必须重新 preflight/freeze；不得用本报告的 `cce8...3441` 恢复或覆盖 S2 bytes。

### 3.3 Public turn 前置锚点快照

| 只读锚点 | SHA-256 / 必要形状 |
|---|---|
| `TurnGameClient.java` | `a8f64d8dbb5f9ed2852975d518836e25af92073f9c818d5f7e9da7cf18056cb9`；public `execute(List<TurnStep>, boolean, Duration)` 内部生成 UUID |
| `TurnInvocationContext.java` | `9166bec9803c9b89b1171005a437721c255e9f54e3d1d1827013eaedc747d8ae` |
| `TurnInvocationResult.java` | `052d9c80a2bfe575514886d1d4eef30af6b474f70a713e132fb6d9ef910024a7` |
| `TaskExecutionContext.java` | `6d4e4a20a6fb4b6dba6a59cb45e95dd39c78a0415b9b2a650d75f9704151d003` |
| `TaskExecutionContextHolder.java` | `3fa2729917449fbb75bf72614e46a223526ea2acb53dc96351886559192c6f3b` |
| input protocol | `MOVE_MOUSE`、`CLICK_RIGHT` 存在；`TurnInputSpec` 含 nullable `clickDelayMs`、`queueHoldMs` |

以上任一 public signature、click timing、exact-window metadata 或 terminal result 形状在 claim 前漂移，均需刷新
S3 contract；不得在 `NpcClickService` 内临时补 adapter、second client、factory 或 local fallback。

## 4. 唯一写集

### 4.1 建议 S3 production 写集

恰好一个 production 文件：

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`

S3 独立 test write set 为空。TURN-28 父级唯一 named test
`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java` 仍由父级后续统一闭合，
本片不创建或修改测试、source guard、replay image 或 testcase output。

### 4.2 本 helper 实际写集

恰好本文件：

`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-28s3-readiness-preflight-helper.md`

本 helper 不创建 S3 卡、不追加 S1/S2/parent 卡、不改 `ACTIVE_WORK` 或 dashboard。

## 5. 精确 production 调用点

当前语义锚点如下；S2 落盘后行号会移动，未来必须按 method/description 锚定，不能按旧行号盲改：

1. `tryDirectCombatTargetClick(...)` 当前约 `695-705`：direct-combat pipeline 返回 `false` 且非 STOP 后，
   调用 `exitDirectCombatClickModeAfterFailure(request)`；返回 `false` 时 caller 抛出既有
   `IllegalStateException`，阻止 follow-up cleanup/retry。
2. `exitDirectCombatClickModeAfterFailure(...)` 当前约 `711-754`：最多三次业务 attempt。
3. 每次 attempt 当前约 `720-735`：先求 `findPlayerAnchorForDirectCombatExit(request)`；无 anchor 时取窗口
   左上角加 `(512,424)`；随后唯一 local input call 为：

```java
inputSequences.submitAndWait("npcClick:directCombat:exitRightClick", List.of(
        InputAction.moveMouse(exitPoint.x, exitPoint.y),
        InputAction.sleep(120),
        InputAction.clickRight(exitPoint.x, exitPoint.y, 120),
        InputAction.sleep(600)
));
```

S3 只把这一 active local submission 替换为一次 public HTTPS turn invocation。无 anchor 时的
`(windowLeft + 512, windowTop + 424)` 应从该 attempt 的 exact latest turn metadata window rect 取得；发送的
`x/y` 始终是 screen-absolute 像素。purple/player anchor 的公式、region、catch 与 fallback 判定保持不变。

不得触碰 S2 四个 shortcut 调用点，不能把 S3 变成对 S2 helper 的二次改写；若 S2 final helper 不能直接承载
本 action，S3 应在该方法内保持一个闭合、可审查的 public invocation，不新增
`prepare -> execute -> handle -> resolve` wrapper nesting。

## 6. 每个 attempt 的 ordered HTTPS action

每个 reached attempt 恰好调用一次
`TurnGameClient.execute(orderedSteps, false, Duration.ofMillis(120_000L))`。ordered steps 必须精确为：

| index | type | inputAction | 精确字段 |
|---:|---|---|---|
| `0` | `INPUT` | `MOVE_MOUSE` | `x=exitPoint.x`, `y=exitPoint.y`；其余 input 字段为空 |
| `1` | `WAIT` | 无 | `waitMs=120` |
| `2` | `INPUT` | `CLICK_RIGHT` | `x=exitPoint.x`, `y=exitPoint.y`, `clickDelayMs=120`, `queueHoldMs=600`；其余 input 字段为空 |

固定 action 属性：

- `contractVersion=1`。
- `deviceId/windowId` 来自当前 exact `TurnInvocationContext`。
- `actionId` 由 public `TurnGameClient.execute(...)` 内部为本 attempt 生成一个 fresh UUID。
- `fullWindowFailureEvidence=false`。
- transport fence 为 `120_000ms`，只限制本次同步等待，不是业务 TTL、retry budget 或 resend 许可。
- 零 `CAPTURE`、`MATCH_TEMPLATE`、`LOCAL_SERVICE`、frame request、foreground focus 或 local input fallback。

`queueHoldMs=600` 将原 `InputAction.sleep(600)` 折叠进同一 exact-window queue submission；因此 HTTPS action
不得再增加 trailing `WAIT 600` step。最终本地原子队列语义仍是：

`MOVE -> SLEEP 120 -> CLICK_RIGHT(clickDelay=120) -> SLEEP 600`

MOVE 与 CLICK_RIGHT 不得拆成两条 command，也不得在 click 完成前释放输入队列 ownership。

## 7. UUID 前 exact-context 门

每个 attempt 独立执行以下顺序，不能跨 attempt 缓存 metadata：

1. 从现有 `TaskExecutionContextHolder` 取得当前非空 `TaskExecutionContext`。
2. 直接调用 `TaskCheckpoint.throwIfStopRequested(context, ...)`；不新增 checkpoint wrapper。
3. 要求 holder 仍指向同一个 context object。
4. 取得 `TurnInvocationContext binding = context.getTurnInvocationContext()`。
5. 取得现有 bound client：`context.getTurnGameClient().bind(binding)`；不注入第二个 client/factory/provider。
6. 在现有 anchor probe 完成后读取本 attempt 的 `latestWindowMetadata()`；要求 exact
   device/window/title/native HWND/process id/正数 window rect 与 context 一致，并通过既有 pause/stop checkpoint。
7. anchor 为 null 时，用这份 exact metadata rect 计算 screen-absolute fallback 点；anchor 非 null 时保留其原始
   screen-absolute 坐标。
8. 在 UUID-producing `execute(...)` 紧前再次确认 holder 是同一 context，metadata 未被另一 window 替代。
9. 仅此时调用一次 public `execute(...)`；`NpcClickService` 不生成、缓存、传入或复用 UUID。

任何 preflight 缺失、STOP、context/metadata 漂移都发生在 UUID/command 前，且为零右键、零 mode probe、零
later attempt。

## 8. 唯一 mechanics completion 形状

只有以下条件全部成立，才允许从本次 right-click mechanics 继续到现有 mode probe：

- `TurnInvocationResult.commandStatus == COMPLETED`。
- real `outcome != null` 且 `outcome.status == COMPLETED`。
- invocation/action/outcome 的 `actionId` 精确相关。
- outcome 的完整 `TurnWindowMetadata` 与本 attempt preflight snapshot 相等。
- `failedStepIndex == null`。
- 恰有三个 step result，按位置为 `(0, INPUT, COMPLETED)`、`(1, WAIT, COMPLETED)`、
  `(2, INPUT, COMPLETED)`。
- invocation raw frame 与 outcome frame metadata 都不存在。
- 每个 step result 的 match/local-service result 都不存在。
- 无额外 step、capture、match、local-service 或 failure evidence。

`commandStatus=COMPLETED` 本身不够；`TurnInvocationResult.from(...)` 已做的 action/device/window 与
step index/type correlation 也不能替代 Service 对 full metadata、step status、failed index 和 no-frame 的检查。

该 completion 只证明右键 mechanics 完成，不证明 direct-combat mode 已退出，不得直接返回业务成功。

## 9. Terminal、uncertain、UUID 与 no-retry

以下任一结果对当前业务推进均为 terminal：

- command `BUSY`、`DUPLICATE_ACTION_ID`、`TIMED_OUT_UNCERTAIN`、`INTERRUPTED_UNCERTAIN`。
- outcome `FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN`。
- missing/malformed outcome、step count/index/type/status mismatch、failed index 或结果污染。
- action/window/full metadata correlation rejection、holder/context drift、client/runtime failure。

确认 task stop 时走既有 stop exception；其它 terminal/uncertain 走既有 task-fatal path。不得映射成
`submitted=false`、普通 click miss、mode still active、`positionRefreshRequired` 或任何 fabricated business truth。

terminal/uncertain 后必须同时满足：

- 零 mode probe。
- 零 `TaskSleep.sleep(300)`。
- 零 later attempt、later UUID、later command。
- 零 local/foreground fallback、cleanup、compensation、candidate、click、verifier、memory 或 navigation。
- 零 transport retry/replay/resend，不论使用相同还是新 UUID。

baseline 最多三次退出 attempt 是 Cloud 在前一条 action **严格完成**、随后 mode probe 仍为 true 后做出的三个
显式业务决定。每个实际到达的 attempt 都产生不同 fresh UUID；这不是 transport retry，也不能由 client 自动执行。

## 10. Strict-696 业务继续条件

S3 必须保持以下精确控制流：

1. 只有 direct-combat candidate pipeline 返回 `false` 且未 STOP 才进入退出流程。
2. attempt `1..3` 每次重新求 purple/player anchor；null 才使用 exact window `(512,424)` fallback。
3. 只有本 attempt HTTPS action 满足第 8 节完整 completion，才执行一次既有 STOP 检查与一次
   `gameStateUtil.isDirectCombatClickModeLikely("npc-direct-combat-exit-attempt-" + attempt)`。
4. probe 为 `false`：`exitDirectCombatClickModeAfterFailure` 立即返回 `true`；caller 保持现有
   `positionRefreshRequired("direct-combat-failed-after-alt-a")`。
5. probe 为 `true`：保持现有 `TaskSleep.sleep(300)` 及其返回处理，然后进入下一次 baseline attempt；不得把
   该 300ms 放进右键 HTTPS action，也不得预先发下一条 command。
6. 三次均未确认退出：保持现有 error log 和 `false`；caller 保持既有 `IllegalStateException`，不得第四次点击、
   cleanup、capture、导航、restart 或 hybrid fallback。

### 10.1 300ms 父卡文字冲突

strict commit `696a12b0` 与当前 byte-identical source 都在每次 `modeLikely=true` 后无条件调用
`TaskSleep.sleep(300)`；第三次仍为 true 时也会先等待 300ms，再退出 loop 并返回 false。TURN-28 父卡中的
“`WAIT300 only before another attempt`”自然语言则可能被读成第三次后不等。

当前没有批准删除第三次 300ms 的业务差异。按 mandatory baseline gate，未来 S3 freeze 必须显式锚定真实
`696a12b0` source：保留第三次后的 300ms 及原位置。若 parent 想改成只有前两次等待，必须先把它作为独立
业务变化列出并取得用户明确选择；External 不得在 migration 中自行决定。本 helper 不修改父卡，也不作批准。

**无已批准业务差异；S3 按 strict `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。**

## 11. Source-start 前置锚点

以下均是未来 parent/owner 在 S3 claim 前应确认的锚点，不是本 helper 的准入裁定：

1. TURN-28S1 source pass 仍有效，pending-proof source gate 没有回流。
2. TURN-28S2 已 true-EOF source delivered，parent source review 完成，owner 已释放。
3. S3 initial SHA 已按第 3.2 节钉为 S2 final SHA，当前 production bytes 精确相等且无第二 writer。
4. S2 四个 shortcut 仍是唯一已迁移 Alt sites；S3 不改它们的 branch、wait、UUID 或 terminal contract。
5. public `TurnGameClient.execute`、exact context/result、`MOVE_MOUSE`、`CLICK_RIGHT`、
   `clickDelayMs/queueHoldMs` 以及 DHXY exact-window action-list mapping 保持当前形状。
6. TURN-28P/Q/22 若在 S3 claim 前改变 action-list、click timing、queue hold 或 result contract，先重做 preflight；
   它们的独立 reviewer/test/build 结论仍是 parent final integration gate，不能由本报告替代。
7. 第 10.1 节 300ms 冲突已在未来 S3 freeze 中显式记录为 strict-source-preserving anchor。
8. External 在 claim 前完整重读 S2 final `NpcClickService.java` 与相关卡物理 EOF，不从 mirror、旧 report SHA 或
   `696a12b0` 文件覆盖当前 source。

## 12. 明确排除项

S3 不得包含：

- S1 pending-proof source 规则或 S2 四个 Alt shortcut 的新增/重做。
- ordinary `executeMoveClickAndVerify(...)` 的 memory/tooltip/yellow/formula 左键迁移。
- Ctrl dense probe、Ctrl-UP finally、menu OCR、direct menu click 或 menu retry。
- capture、pixel-change probe、raw PNG、template matching、OCR、image preprocess、ROI 或 screenshot provider 变更。
- `ObjectiveTextRecognizer.java`、`SmartClickRecognizer.java`、ImageAlgorithms、reference/shadow recognizer 变更。
- dialog/BattleRadar verifier count、candidate FIFO、memory priority、formula miss、TENTATIVE cutoff 或 fallback order 变更。
- `InputSequences`、`InputProvider`、DHXY executor/queue、turn protocol/client/factory/context/result 修改。
- 新 service/facade/model/session/owner/permit/ledger/TTL/durable workflow。
- extra verification/read、fourth attempt、cleanup、park/yield、retry、replay、resend 或 local/foreground compensation。
- 第二 production 文件、任何测试文件、POM/config/resource、caller、CR 卡、`ACTIVE_WORK` 或 dashboard 修改。

TURN-28 父级后续仍需剩余 production slices、唯一 named test、适用 compile/build、两名 independent reviewer 与
fresh runtime 的独立运行证据。本 readiness 报告不满足也不替代任何一项。

## 13. Helper 边界声明

本报告完成的是下一最小 production slice 的合同收敛：单调用点、三步 HTTPS action、exact context、fresh UUID、
terminal no-retry、strict-696 继续条件、唯一写集、动态 initial SHA gate、前置锚点与排除项。它不认领文件、
不评审实现、不批准 CR，也不改变任何任务状态。

TRUE_EOF PRECHECK_COMPLETE
