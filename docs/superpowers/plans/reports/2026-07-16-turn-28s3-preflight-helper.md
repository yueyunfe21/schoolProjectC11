# CR271 TURN-28S3 Internal Helper Preflight

- 日期：2026-07-16
- 角色：CR271 Internal helper
- 目标：在 TURN-28S1/S2 之后识别下一块最小、互斥、可独立交给 External 的 production slice
- 性质：只读证据预检；不构成 parent freeze、source review、批准或阻断结论
- 唯一写入：本报告

## 0. 非绑定结论

S2 的四个 active background shortcut 之后，下一块最小真实实现片建议为：

> **TURN-28S3：direct-combat 非 STOP 点击失败后的“退出直接战斗模式”右键动作 HTTPS-turn cutover。**

只替换 Cloud `NpcClickService.exitDirectCombatClickModeAfterFailure(...)` 内当前唯一的
`inputSequences.submitAndWait("npcClick:directCombat:exitRightClick", ...)` 调用；保留锚点计算、fallback、最多三次
尝试、每次 mode probe、300ms 等待、返回值与 caller throw 语义。production 写集只有一份文件：

`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`

本 slice 不修改 `ObjectiveTextRecognizer`、`SmartClickRecognizer`、protocol/client/DHXY mechanics 或任何测试。
它有真实 caller、真实输入 ownership cutover 和可观察 terminal fence，不是 dormant facade、test-only slice 或接口占位。

## 1. 已完整读取的证据

本轮完整读取并交叉核对：

1. `AGENTS.md`。
2. `docs/DHXY_CONTEXT.md`。
3. `docs/ACTIVE_WORK.md` 顶部当前 CR271/owner/writer 状态。
4. `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14 至 19 节。
5. `docs/业务逻辑.md` 的业务基线门、修罗适用基线和 NPC click FIFO/紫名规则。
6. TURN-28 原卡完整物理 EOF。
7. TURN-28S1 原卡完整物理 EOF。
8. `2026-07-16-turn-28-next-slice-decomposition-helper.md`。
9. `2026-07-16-turn-28s2-parent-freeze-preflight-helper.md`。
10. TURN-28P、TURN-28Q、TURN-22/TURN-22D1 当前卡与相关 independent review 报告。
11. Cloud 当前 `NpcClickService.java`、`ObjectiveTextRecognizer.java`、`SmartClickRecognizer.java` 全文。
12. Cloud 当前 `TurnGameClient`、`TurnInvocationResult`、`CloudTurnCommandResult`、`TaskExecutionContext` 相关边界与
    `TurnAction`/`TurnStep`/`TurnInputSpec`/`TurnOutcome`/`TurnStepResult` 协议模型。
13. DHXY 当前 turn input mapper/executor、TURN-28P/Q exact-window action-list mechanics 的相关调用路径。
14. Git commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 中完整 `NpcClickService.java`，尤其
    `tryDirectCombatTargetClick(...)`、`exitDirectCombatClickModeAfterFailure(...)` 与锚点计算路径。

未运行 Maven、JUnit、compile、package、runtime、application/server、Task/UI、capture、OCR 或任何真实 input。
未执行 stage/commit/branch/checkout/reset/restore/merge/rebase/cherry-pick/clean 等 Git mutation。

## 2. 预检快照与基线权威

### 2.1 当前源码快照

| Cloud production 文件 | SHA-256 |
|---|---|
| `NpcClickService.java` | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` |
| `ObjectiveTextRecognizer.java` | `d3dc3cc247058ae85a6258e6173f8d9b56d7be119443c90a24c4bf6f180f3fe1` |
| `SmartClickRecognizer.java` | `ffbd984a4ed5841ccba6b87bf3378a1e0cb1e7d2bea68be3eed656be7324f102` |

当前 Cloud `NpcClickService.java` 的 Git blob 为
`74d9b26b76b84052718d5679529f7ffeb46e3273`，与 DHXY commit `696a12b0...` 中同路径 blob 完全相同。
因此本轮观察到的是 S2 落盘前的 strict-696 原字节；它只能作为 S3 的 pre-S2 证据，不能作为未来 S3 claim
时的 initial SHA。S3 必须在 S2 parent source pass、owner release 后重新读取并钉住 S2 final SHA。

### 2.2 唯一业务权威

本 slice 的业务权威是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。S3 只移动右键物理输入的
执行 ownership，不改变：

- 进入 direct-combat 的业务条件；
- `FLYING/UNKNOWN/grounded` 分支；
- `Alt+C/700` 与 `Alt+A/350`；
- candidate pipeline、BattleRadar verifier 或 combat 成功真值；
- purple/player anchor 算法与窗口 fallback；
- 尝试次数、probe 次数、等待位置、失败后的 caller 语义；
- CR255/CR267 的 authorization/event/restart 语义。

`docs/业务逻辑.md` 中旧 session/base-screenshot 章节只提供 FIFO/图像规则历史背景；TURN-28 新合同已明确
不保 runtime session/poller/共享 before frame。S3 不涉及 recognizer/capture，不能借本 slice 引入或删除这些结构。

## 3. 为什么 S3 是下一块最小真实实现片

### 3.1 单一 production 调用点

当前 `NpcClickService.java:711-754` 的 `exitDirectCombatClickModeAfterFailure(...)` 只有一个本地输入调用：

```java
inputSequences.submitAndWait("npcClick:directCombat:exitRightClick", List.of(
        InputAction.moveMouse(exitPoint.x, exitPoint.y),
        InputAction.sleep(120),
        InputAction.clickRight(exitPoint.x, exitPoint.y, 120),
        InputAction.sleep(600)
));
```

它位于一个现成 caller 和现成 retry budget 内。把这一处改为一个 HTTPS turn action，立即减少一条 active
本地 physical-input 路径；无需先新增 recognizer facade、model、service、adapter 或测试专用 hook。

### 3.2 与 S2 的互斥关系

S2 和 S3 都只写 `NpcClickService.java`，所以必须串行，不能并发 claim：

1. S2 先完成四个 shortcut 的 source delivery。
2. parent 对 S2 做 source review并释放 owner。
3. S3 parent freeze 重新记录 S2 final SHA、物理 EOF、当前 diff 与唯一 owner。
4. External 从该 SHA 增量编辑 S3；不得恢复到本报告的 pre-S2 strict-696 文件。

这使 S3 与 S2 在时间上互斥，也避免 External 覆盖 S2 新增的 imports、exact-context/terminal boundary 或日志。

### 3.3 比其它候选更小

| 候选 | 本轮不选原因 |
|---|---|
| ordinary `executeMoveClickAndVerify(...)` 左键 cutover | 一个 helper 服务 memory/tooltip/yellow/formula 多策略，连带不同 hold、retry/verifier 预算，blast radius 明显更大。 |
| `SmartClickRecognizer` typed facade only | 没有同片真实 NpcClick caller，会再次形成 dormant production。 |
| `ObjectiveTextRecognizer` 修改 | TURN-28 只是 reservation；右键退出不依赖它，零 diff 才是最小正确范围。 |
| Ctrl probe/menu click | 同时耦合 ROI、pixel probe、raw PNG、OCR、Ctrl-UP finally、menu provider 顺序与新 click action，不是下一最小片。 |
| 通用 mouse wrapper但不迁移 caller | 只有接线没有 active behavior，不是真实 production slice。 |

## 4. 建议的精确写集

### 4.1 Production 写集

唯一 production 文件：

`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`

只允许修改：

- `exitDirectCombatClickModeAfterFailure(...)` 中每次 reached attempt 的右键提交边界；
- 为直接使用现有 turn protocol/client 所必需的 imports；
- 同一方法内必要的 exact-context、action、result correlation 和 terminal projection。

优先在现有方法内就地完成，不新增顶层类型、service、facade 或 wrapper chain。若 S2 final bytes 已提供可安全
复用的同层 exact-context/result validator，S3 只能直接复用一个真实 ownership boundary；不得形成
`prepare -> execute -> handle -> resolve` 的一行 helper 嵌套，也不得把 key-only helper 勉强扩成行为不透明的通用层。

### 4.2 明确只读

- `ObjectiveTextRecognizer.java`；
- `SmartClickRecognizer.java`；
- `TurnGameClient`、turn protocol/model/factory/client；
- DHXY `InputSequences`、queue/worker、turn executors、keyboard/focus/capture；
- TURN-28 parent named test；
- Task/caller、POM、config、resources、templates。

### 4.3 测试写集

S3 独立测试写集为空。不要创建第二个 test、source guard、reflection test、replay image 或 testcase output。
本片的 JSON/UUID/terminal 证据保留到 parent 唯一
`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java` 中闭合。

## 5. Strict-696 业务与时序合同

### 5.1 进入与坐标

S3 不改变方法入口。只有 direct-combat candidate pipeline 非 STOP miss 后才进入退出流程。

每次 attempt：

1. 保留现有 STOP checkpoint/语义；STOP 不发送右键。
2. 重新调用现有 `findPlayerAnchorForDirectCombatExit(request)`。
3. 有 purple/player anchor 时使用该 screen-absolute 点。
4. 无 anchor 时使用当前 exact window 左上角加 `(512,424)`，即
   `(base.x + WINDOW_WIDTH/2, base.y + WINDOW_HEIGHT/2 + 40)`。
5. 不改锚点 OCR/公式、region、clamp、catch/fallback 或日志真值。

不得把 fallback 写成固定屏幕 `(512,424)`；该值是窗口相对偏移，发送到 turn action 的 `x/y` 仍是
screen-absolute 像素。

### 5.2 一次 attempt 的原子 mechanics

一个 reached attempt 必须只调用一次 public `TurnGameClient.execute(...)`，该一次调用生成一个 fresh UUID，发送
一条 command。ordered steps 精确为：

| index | type | 内容 |
|---:|---|---|
| 0 | `INPUT` | `MOVE_MOUSE(x=exitPoint.x, y=exitPoint.y)` |
| 1 | `WAIT` | `120ms` |
| 2 | `INPUT` | `CLICK_RIGHT(x=exitPoint.x, y=exitPoint.y, clickDelayMs=120, queueHoldMs=600)` |

使用 `fullWindowFailureEvidence=false` 和现有正 transport fence
`Duration.ofMillis(120_000L)`。`120_000ms` 只是一条 command 的等待上限，不是 TTL、业务 sleep、retry budget
或重发许可。

`queueHoldMs=600` 必须折叠在 click input 内，使 DHXY exact-window action-list 在同一 queue transaction 中执行
`MOVE -> SLEEP120 -> CLICK_RIGHT(delay120) -> SLEEP600`。不要再额外发送 trailing `WAIT600` step，不要拆成
多个 public command，也不要本地 fallback 到 `InputSequences`/`InputProvider`/foreground input。

### 5.3 最小 JSON 形状

以下为一个 attempt 的 canonical 语义形状；示例假定本 attempt 已算出的 screen-absolute point 是
`(1400,760)`，真实 action 必须替换为当次 `exitPoint.x/y`。Jackson 对 record 中普通 nullable 字段可保留
`null`，`clickDelayMs/queueHoldMs` 仅在 click step 出现。

```json
{
  "contractVersion": 1,
  "actionId": "<fresh UUID for this reached attempt>",
  "deviceId": "<exact current deviceId>",
  "windowId": "<exact current windowId>",
  "steps": [
    {
      "index": 0,
      "type": "INPUT",
      "inputAction": "MOVE_MOUSE",
      "input": {
        "x": 1400,
        "y": 760,
        "endX": null,
        "endY": null,
        "scrollDelta": null,
        "key": null,
        "text": null
      },
      "waitMs": null,
      "capture": null,
      "match": null,
      "localService": null
    },
    {
      "index": 1,
      "type": "WAIT",
      "inputAction": null,
      "input": null,
      "waitMs": 120,
      "capture": null,
      "match": null,
      "localService": null
    },
    {
      "index": 2,
      "type": "INPUT",
      "inputAction": "CLICK_RIGHT",
      "input": {
        "x": 1400,
        "y": 760,
        "endX": null,
        "endY": null,
        "scrollDelta": null,
        "key": null,
        "text": null,
        "clickDelayMs": 120,
        "queueHoldMs": 600
      },
      "waitMs": null,
      "capture": null,
      "match": null,
      "localService": null
    }
  ],
  "fullWindowFailureEvidence": false
}
```

`NpcClickService` 不生成、缓存或复用 UUID。若 mode probe 仍为 true 并到达下一次 baseline attempt，下一次
`execute(...)` 必须生成另一个 UUID；transport terminal/uncertain 不得以相同或新 UUID 自动重发。

### 5.4 完成后业务顺序

只有 command 与 outcome 严格完成后，才允许继续原方法：

1. 执行现有 STOP checkpoint/检查。
2. 调用一次现有 `gameStateUtil.isDirectCombatClickModeLikely("npc-direct-combat-exit-attempt-N")`。
3. probe 为 false：立即返回 `true`，caller 返回现有 `positionRefreshRequired`。
4. probe 为 true：执行现有 `TaskSleep.sleep(300)`，再按现有 loop 继续或结束。
5. 三次均未确认退出：方法记录现有 error 并返回 `false`，caller 保持现有 throw；不得 cleanup、第四次点击、
   capture、导航、restart 或 CR255/CR267 hybrid。

机械 action `COMPLETED` 只表示右键 mechanics 完成，绝不等于模式已退出。只有现有 mode probe 的 false
结果能结束退出 loop。

### 5.5 已发现的 300ms 文本冲突

这里存在一处必须由 parent 在未来 S3 freeze 中显式记录的基线文本冲突：

- `696a12b0` 与当前 byte-identical source 在每次 `modeLikely=true` 后无条件执行
  `TaskSleep.sleep(300)`；因此第三次仍为 true 时，也会先等待 300ms，再退出 loop、记录失败并返回 false。
- TURN-28 原卡第 80 行写成“`WAIT300 only before another attempt`”，其自然含义是第三次后不等待。
- 同一原卡又声明 strict `696a12b0`、`无已批准业务差异`，没有把删除第三次后的 300ms 列为批准差异。

S3 是 ownership-only slice，最小无差异实现应保持现有 `TaskSleep.sleep(300)` 行及其位置不动，即三次 probe
都为 true 时仍有三次 300ms 等待。本报告不修改原卡，也不替 parent/user 裁决该矛盾；如果 parent 要求改成
只有两次等待，必须先按 AGENTS 业务基线门记录为明确行为变更并取得用户选择，不能让 External 在迁移中自行决定。

## 6. Exact context、completion 与 terminal 合同

### 6.1 UUID 之前的 exact-context preflight

每个 attempt 独立执行，不能跨 attempt 缓存 metadata：

1. 从现有 `TaskExecutionContextHolder` 要求当前 `TaskExecutionContext` 存在。
2. 直接调用 `TaskCheckpoint.throwIfStopRequested(context, ...)`；不新增 checkpoint wrapper。
3. 要求 holder 仍是同一个 context object。
4. 取得 `context.getTurnInvocationContext()` 与 `context.getTurnGameClient().bind(binding)`。
5. 在 UUID-producing `execute(...)` 前读取 `latestWindowMetadata()`，要求 exact `deviceId/windowId`、native HWND、
   process id 和正数 window rect 全部与 context 一致。
6. metadata STOP 通过既有 checkpoint/stop exception 投影，不能变成普通 click miss。
7. 在 `execute(...)` 前再次确认同一 holder context。

任何 preflight 缺失/漂移都发生在 UUID 和 command 之前，且不得调用 mode probe 或发送本地右键。

### 6.2 唯一可继续的完成形状

继续到 mode probe 必须同时满足：

- `TurnInvocationResult.commandStatus == COMPLETED`；
- real outcome 存在且 `outcome.status == COMPLETED`；
- invocation/action/outcome action id 精确相关；
- outcome window 与本 attempt preflight metadata 完全相等；
- `failedStepIndex == null`；
- 恰有三个 step result，按位置分别是 `(0,INPUT)`, `(1,WAIT)`, `(2,INPUT)`，status 全为 `COMPLETED`；
- outcome frame metadata 与 raw frame 均不存在；
- 每个 step result 的 match/local-service result 均不存在；
- 没有额外 step、capture、match、local-service 或 failure evidence。

不得用“command completed”代替 outcome/step correlation，也不得用任一 step code 推导 direct-combat mode 真值。

### 6.3 Terminal/uncertain 零后续动作

下列结果对本次业务推进均为 terminal：

- command `BUSY`、`DUPLICATE_ACTION_ID`、`TIMED_OUT_UNCERTAIN`、`INTERRUPTED_UNCERTAIN`；
- outcome `FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN`；
- missing/malformed outcome、step count/index/type/status mismatch、action/window correlation rejection；
- preflight/outcome metadata drift、holder/context drift、client/runtime failure；
- 任何 frame、match、local-service 等不属于本 action 的结果污染。

确认 task stop 时走现有 stop exception；其它 terminal/uncertain 走现有 task-fatal path。两类都必须满足：

- 本 attempt 后零 mode probe；
- 零 `TaskSleep.sleep(300)`；
- 零 later attempt、later UUID、later command；
- 零 candidate/click/verifier/memory/navigation/cleanup/compensation；
- 零 local/foreground fallback；
- 零 transport retry/replay/resend。

## 7. Parent 唯一 named test 的 S3 保留项

S3 不单建测试。parent `NpcClickTurnContractTest` 最终至少保留下列 production-penetrating case，使用 scripted
`TurnGameClient`/port、内存结果和 fake business collaborators，不做 source scan/private reflection：

1. purple/player anchor 命中时，唯一 action 使用该 screen-absolute 点和精确三步 JSON。
2. anchor 缺失时，唯一 action 使用 exact latest window `(left+512, top+424)`，不是固定屏幕点。
3. 第一次 command/outcome/steps 完成且 mode probe=false：一个 UUID、一个 command、一次 probe、零后续 attempt。
4. 前两次 probe=true、第三次 probe=false：三次 action 都有 fresh distinct UUID；每次精确三步；无自动重发。
5. 三次 probe=true：最多三个 distinct UUID/command，三次 probe，随后原 caller throw；没有第四次 action、cleanup
   或 position-refresh success。300ms 断言按第 5.5 节的 parent/user 明确裁决冻结，不能由测试作者猜测。
6. 每个 command terminal/uncertain 枚举分别断言：一个 reached UUID/command，零 mode probe，零 later UUID/action。
7. outcome `FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN`、窗口漂移、step count/index/type/status 错误分别 fail closed。
8. STOP/context/metadata 异常发生在 execute 前时：零 UUID、零 command、零 mode probe。
9. command completed但 outcome/step 不完整，或 action completed但 mode 仍 true，均不能伪造 exit success。
10. click 的 `clickDelayMs=120`、`queueHoldMs=600` 与一个 action/一个 exact-window queue transaction 最终由
    TURN-28Q/TURN-22 DHXY integration contract 交叉闭合；Cloud 测试不手工制造“queue 已原子”的假证据。

## 8. Source-start 前置与 final-only 依赖

### 8.1 S3 source-start 前 parent 应重新确认

以下是未来 claim 的前置核对，不是本 helper 的准入结论：

1. S2 parent source review 已完成，`NpcClickService.java` owner 已释放。
2. 重新读取 S2 card/报告物理 EOF，记录 S2 final SHA 与 current diff；本报告的 pre-S2 SHA 不可直接沿用。
3. Cloud `NpcClickService.java` 没有第二个 active writer；External 的 production 写集保持单文件。
4. `TurnGameClient.execute(List<TurnStep>, boolean, Duration)`、protocol click timing 与 current context API 形状未漂移。
5. 第 5.5 节 300ms 源码/卡片差异在 S3 freeze 中被显式记录；External 不自行选择业务变化。
6. External 完整重读 S2 final bytes、strict-696 method、TURN-28P/Q/22 最新物理 EOF 后再动源码。

### 8.2 可以留到 parent final integration 的门

S3 Cloud source 可以只依赖现有 public API 形状进行静态实现；下列证据不能被 S3 delivery 冒充，留作 parent
最终集成门：

- TURN-28P 两名 independent reviewer 的最新轮、授权 named tests 与适用 DHXY compile/build。
- TURN-28Q exact-window action-list 的 reviewer 返修、最新两名 reviewer 结论、named test 与 DHXY build。
  本轮读取快照中，TURN-28Q public facade/API 已存在且 parent source review 曾通过；之后 R1/R2 报告又记录了
  exact-binding Alt、自暂停取消与 pause proof 的未闭合意见。Alt 项直接影响 S2，取消/completion barrier 也影响
  S3 mouse action-list。S3 不修改或重审这些 DHXY bytes，parent final 不能把“API 可调用”写成“mechanics 已闭合”。
- TURN-22/TURN-22D1 当前 repair 的 parent source review、independent review、named tests 与 build；它负责让 turn
  ordered input 真正穿透 frozen exact-window action-list consumer，而不是只在 Cloud JSON 层看起来原子。
- TURN-28 parent 唯一 `NpcClickTurnContractTest` 对 S1/S2/S3 与后续 left-click/recognizer/Ctrl slices 的合并证据。
- TURN-28 剩余 production slices、两名 independent reviewer 最新批准轮、Cloud compile/build。
- fresh runtime 仍是独立运行验收，不是本 preflight 或 source review 的替代品；本 helper 未运行它。

任何 final-only 依赖如果在 S3 claim 前改变 public signature、click timing、terminal projection 或业务基线，parent
需要刷新 S3 freeze；External 不能猜测新接口。

## 9. External 交付边界建议

未来 External 若由 parent 正式派发 S3，应只交付：

1. S2 final SHA 之上的 `NpcClickService.java` 单文件增量。
2. 当前右键本地调用归零的精确行证据。
3. 每 attempt 一个 action/UUID、三步 JSON、exact metadata、terminal 零后续动作的源码证据。
4. strict-696 锚点/fallback/三次预算/mode probe/300ms/caller throw 原样保留的对照证据。
5. initial/final SHA 和零其它 production/test diff 说明。

External 不自批、不运行 Maven/runtime/input、不修改 parent card 之外的写集，也不顺手迁移普通左键、Ctrl、OCR、
recognizer 或 direct-combat authorization。parent 后续再做独立 source review 和最终门编排。

## 10. Helper 边界声明

本报告识别的是一个非绑定的下一最小 slice，并记录其基线、JSON、UUID、terminal tests 与 final-only dependencies。
它不写 `APPROVED`、`BLOCKED`、P0/P1/P2 verdict，不改变 TURN-28/CR271 状态、owner、矩阵或 dashboard。

无已批准业务差异；S3 建议按 `696a12b0` 做右键 ownership 等价迁移，300ms 文本冲突留给 parent/user 显式裁决。

TRUE_EOF PRECHECK_COMPLETE
