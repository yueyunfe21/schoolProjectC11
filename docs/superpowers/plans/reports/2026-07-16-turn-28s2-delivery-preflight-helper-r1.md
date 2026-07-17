# CR271 TURN-28S2 delivery preflight helper R1

## 1. 角色、范围与证据

- 角色：TURN-28S2 delivery preflight helper；不是 implementation owner、reviewer、父级 final reviewer 或状态裁决者。
- 唯一用途：为父级在未来看到正式 true-EOF `SOURCE DELIVERED` 后逐行复核源码提供精确清单。
- 本报告不批准、不阻断、不改变 TURN-28S2/TURN-28/CR271 状态，也不把 claim、mtime、SHA 变化或 WIP 当成交付。
- 唯一写入：本报告。`AGENTS.md`、上下文、计划、协议、业务逻辑、S2 子卡、External A lane、两仓源码和状态均只读。
- 未运行 Maven、JUnit、compile/package、runtime/application/server/Task/UI、capture/OCR/input；未执行任何 Git mutation。
- 已完整读取：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、
  HTTPS turn 协议规格、`docs/业务逻辑.md`；并读取 S2 子卡、External A lane、当前/strict-696
  `NpcClickService.java`、public turn client/result/context/protocol 路径与两仓完整 porcelain status。

## 2. 当前不是 delivery

最后一次紧邻报告写入前的只读快照为 `2026-07-16T10:59:08.331-04:00`：

- S2 子卡物理 EOF 是
  `EXTERNAL-A OWNER-RETURNED ZERO-BYTES-WRITTEN ... 2026-07-16T10:43:15.654-04:00`，不是
  `SOURCE DELIVERED`。
- External A lane 物理尾部同步记录 `S2 RETURNED ZERO-BYTES`，A 当前不持有该卡。
- Cloud 目标仍是领取前 3374 行初始字节；没有可供本 helper 审查的 S2 production delivery。
- 因此以下内容全部是未来 delivery checklist。父级必须先看到子卡物理 EOF 的正式
  `SOURCE DELIVERED`，再以届时字节重新取证；本快照不能替代该门。

## 3. 两仓保护快照与唯一写集

### 3.1 Git status 快照

| Repo | Branch / HEAD | `--untracked-files=all` | 与本切片直接相关 |
|---|---|---:|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 719 项：43 `M`、1 `D`、675 `??` | DHXY `NpcClickService.java` 已有受保护修改；本报告创建前尚不存在 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 项：9 `M`、541 `??` | 当前目标、strict-696 mirror、全部 turn 新链均为受保护 `??` |

Cloud 目标是 untracked，普通 `git diff -- NpcClickService.java` 不会给出可信 delivery diff。父级必须按
文件 SHA/mtime、子卡交付 SHA 和 strict-696 mirror 做独立文本/字节比较，不能用 Git clean/reset/checkout/
restore/stage/commit 制造“干净基线”。

### 3.2 TURN-28S2 唯一写集

未来 implementation delivery 只允许：

1. Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`；
2. append-only `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md` claim/delivery evidence。

Test write set 为空。Recognizer、协议、client/context/result、DHXY executor/key mapper、caller、POM、resource、
其它卡/lane/计划/`ACTIVE_WORK`/dashboard 均不属于 S2 production write set。

### 3.3 当前 SHA/mtime

| Path | Lines / bytes | SHA-256 | mtime (EDT) |
|---|---:|---|---|
| Cloud current `NpcClickService.java` | 3374 / 175367 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` | `2026-07-16T08:50:50.222-04:00` |
| Cloud `migration-baseline/696a12b0/.../NpcClickService.java` | 3374 / 175367 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` | `2026-06-30T01:43:39.000-04:00` |
| TURN-28S2 child card | 166 / 12524 | `5fe6421f8a1e92316927fe9c1f295a4d651b96a6808b98ba393fbf86e309de05` | `2026-07-16T10:43:15.828-04:00` |
| External A lane | 289 / 23611 | `110294025701bab11f61b6d208572b2ef34c5627112e6c35ce04f43a25185b9b` | `2026-07-16T10:43:17.032-04:00` |

当前 Cloud 文件与 mirror byte-identical；与 DHXY Git
`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/NpcClickService.java`
按顺序比较 3374 行差异为 0，Git baseline blob 为 `74d9b26b76b84052718d5679529f7ffeb46e3273`。

## 4. 四个且仅四个 active shortcut 位置

以下行号是当前 strict-696 快照锚点；delivery 后父级应以方法名、description 和相邻业务分支重新定位，不能假设
行号不变。

| # | 当前锚点 | 到达条件 | 必须替换的机械动作 | 唯一成功后续 |
|---|---|---|---|---|
| 1 | `clickNpcSmart` 624-627，`npcClick:retry:altC-dismount` | 第一条完整 pipeline miss、未 stop、且不是 `COMBAT_TARGET` | `ALT_C` -> `WAIT 700ms` | 一次 `runNpcClickPipeline(..., "dialog-after-alt-c")` |
| 2 | `tryDirectCombatTargetClick` 667-670，`npcClick:directCombat:altC-dismount` | `detectFlyingState(...) == FLYING` | `ALT_C` -> `WAIT 700ms` | 继续到同方法公共 `ALT_A` 入口 |
| 3 | `tryDirectCombatTargetClick` 682-685，`npcClick:directCombat:enterAltA` | grounded/continuing，或 FLYING 已完成 dismount | `ALT_A` -> `WAIT 350ms` | 既有 direct-combat pipeline + BattleRadar verifier |
| 4 | `prepareNpcPipelineNameLayerOnce` 948-951，`npcClick:pipeline-hide-player-names:*` | ordinary pipeline 实际到达 name-layer preparation | `ALT_4` -> `WAIT 400ms` | 既有 tooltip/yellow/formula/Ctrl 顺序 |

逐行保全点：

1. `COMBAT_TARGET` 第一次 ordinary pipeline miss 后仍直接返回 `false`，零 generic `ALT_C`、零第二 pipeline。
2. FLYING 的 `ALT_C/700` 与随后 `ALT_A/350` 是两个独立业务调用点、两个 action、两个不同 UUID；不能合并。
3. flying `UNKNOWN` 仍零 action 并走原 skip；grounded 不发 dismount，只到公共 `ALT_A/350`。
4. direct-combat pipeline 仍跳过 `ALT_4`。ordinary pipeline 的 early memory/dialog return 仍可能零 `ALT_4`；
   只有实际到达 preparation 才发一次。
5. generic retry 两轮 ordinary pipeline 都到达 name-layer 时，合法业务顺序可为
   `ALT_4/400 -> miss -> ALT_C/700 -> ALT_4/400`；第二次 `ALT_4` 不是 transport retry，不能去重。
6. 原日志、public signature/return、候选 FIFO、OCR/template/click/verifier、right-click exit、BattleRadar、
   memory、dialog、navigation、pause/stop 业务顺序均保持。

明确排除：3271 的 `captureCleanNameToFileDirect` direct `inputProvider.pressAlt4()+400ms` 与 3302 的
exclusive callback direct `inputProvider.pressAlt4()+400ms` 必须原样保留。185/202 的 move-click、370 的 Ctrl
exclusive、730 的 exit right-click 也不属于 S2。

## 5. 正式 delivery 后的 public TurnGameClient 逐行清单

### 5.1 先确认 delivery 字节稳定

1. 先读 S2 子卡物理 EOF；只有明确 `SOURCE DELIVERED`、final SHA、行证据和 owner 停止写入才开始检查。
2. 立即记录目标的 SHA、mtime、行数、bytes，并核对等于子卡声明的 final SHA；claim/WIP/mtime 变化均不替代此门。
3. 从 strict-696 mirror 逐行比较 final 文件。允许的逻辑 delta 只能是必要 imports/常量、一个真实边界 helper，
   以及上述四处 RHS/调用点迁移；其余业务块应保持等价。
4. review 结束前再次计算目标 SHA/mtime并重读子卡 EOF。若两次快照不同，仅说明证据已过时；丢弃本轮行号并
   从新字节重取证，不把中途变化判成交付质量结论。
5. 同时复核只读 public API 未漂移。当前参考 SHA：
   - `TurnGameClient.java` `a8f64d8dbb5f9ed2852975d518836e25af92073f9c818d5f7e9da7cf18056cb9`；
   - `TurnInvocationResult.java` `052d9c80a2bfe575514886d1d4eef30af6b474f70a713e132fb6d9ef910024a7`；
   - `CloudTurnCommandResult.java` `f54a5b9fe29d264e5e4e16768b092a134f583cf79e9c372b53cf2c4edaa9b87f`；
   - `TaskExecutionContext.java` `6d4e4a20a6fb4b6dba6a59cb45e95dd39c78a0415b9b2a650d75f9704151d003`；
   - `TaskExecutionContextHolder.java` `3fa2729917449fbb75bf72614e46a223526ea2acb53dc96351886559192c6f3b`。

### 5.2 每个 reached site 的 action shape

每处只能调用一次真实 public
`TaskExecutionContext.getTurnGameClient().bind(binding).execute(steps, false, positiveTimeout)`。S2 卡没有冻结
具体 timeout 数字；父级只需确认它是一次正 command wait fence，不是 TTL、业务 delay、循环或 resend 授权。

```text
TurnAction
  actionId = public TurnGameClient 内部本次调用新建的 canonical UUID
  deviceId/windowId = 当前 exact TurnInvocationContext
  fullWindowFailureEvidence = false
  step[0] = index 0 / INPUT / KEY_TAP /
            TurnInputSpec(key=ALT_C|ALT_A|ALT_4, 其它字段全 null)
  step[1] = index 1 / WAIT / waitMs=700|350|400
```

父级逐字段确认：

- `INPUT` 与 baseline WAIT 在同一 `List<TurnStep>`、同一次 public `execute`，不能拆成两个 command。
- `TurnInputSpec` 的 x/y/end/scroll/text/clickDelayMs/queueHoldMs 均为 null；只有 `key` 非空。
- 没有 CAPTURE、MATCH_TEMPLATE、LOCAL_SERVICE、mouse、frame request、foreground fallback 或第二 wait。
- 四处不再走原 `InputSequences.submitAndWait`；terminal 时也不能回退 `InputSequences`/`InputProvider`。
- 不直接注入/调用 `CloudTurnActionFactory`、`CloudTurnCommandPort` 或第二 `TurnGameClient` bean。

### 5.3 exact context 与 correlation

每个 helper invocation 应形成一个完整、可见的单边界：

1. 从既有 `taskExecutionContextHolder.current()` 取得非空 `TaskExecutionContext`；缺失走 task-fatal，不走 local input。
2. 在动作前直接调用现有 `TaskCheckpoint.throwIfStopRequested(context, ...)`；不增加 checkpoint wrapper。
3. checkpoint 后、UUID-producing `execute` 紧前，holder 仍必须是同一个 context 对象（identity，不只是字段相等）。
4. `binding = context.getTurnInvocationContext()`；client 必须是 context 的 exact-bound public view。
5. `latestWindowMetadata()` 读取不生成 UUID/command。preflight 至少确认：
   - deviceId/windowId 与 binding exact；
   - title 非空且与 context frozen title exact；
   - nativeHandle 非空且与 context HWND exact；
   - processId 正数且与 context process exact；
   - rect 非空、width/height 正数。
6. 返回 `outcome.window()` 必须与这次 preflight 的完整 `TurnWindowMetadata` snapshot exact-equal；因此
   device/window/title/HWND/process/rect 以及 control metadata drift 均不能被忽略。
7. `TurnInvocationResult.from(...)` 已核 actionId、device/window、step index/type，但没有替 Service 核
   title/HWND/process/rect、step status、failedStepIndex 或 no-frame；delivery 必须显式闭合这些剩余检查。
8. 唯一继续形状是 command `COMPLETED` + outcome `COMPLETED` + `failedStepIndex == null` + 恰好
   `0/INPUT/COMPLETED`、`1/WAIT/COMPLETED`；两个 step 的 match/local result 均为 null。
9. `invocation.frame == null` 且 `outcome.frame == null`。shortcut completion 只证明 key+wait mechanics 完成，
   不能构造 NPC/dialog/click/combat/OCR/verifier 成功事实。
10. 在返回 success/进入下一业务行前保留最终 stop checkpoint；confirmed stop 只走现有
    `TaskStopRequestedException` 路径。

### 5.4 terminal 投影与零后续

| 收到的结果 | 必须投影 | 本调用点之后允许动作数 |
|---|---|---:|
| command `BUSY` | existing task-fatal | 0 |
| command `DUPLICATE_ACTION_ID` | existing task-fatal | 0 |
| command `TIMED_OUT_UNCERTAIN` | task-fatal；不 resend | 0 |
| command `INTERRUPTED_UNCERTAIN` | 先 checkpoint；未确认 stop 则 task-fatal | 0 |
| outcome `FAILED` | task-fatal；不能压成 boolean miss/skip | 0 |
| outcome `STOPPED` | 先 checkpoint；未确认 stop 则 task-fatal | 0 |
| outcome `DUPLICATE_OR_UNCERTAIN` | task-fatal；不 replay | 0 |
| null/malformed、client exception、context/metadata/step/frame drift | checkpoint（如适用）后 task-fatal | 0 |

零后续必须在具体 caller 上可见：

- generic `ALT_C` terminal 后：零第二 pipeline；
- flying `ALT_C` terminal 后：零 `ALT_A`；
- `ALT_A` terminal 后：零 direct-combat pipeline、零 right-click compensation/cleanup；
- `ALT_4` terminal 后：零 tooltip/OCR/template/yellow/formula/Ctrl/click/verifier；
- 所有 terminal：零 local fallback、零另一 action、零 frame/capture。

`TaskStopRequestedException` 应原样传播；不能被宽泛 catch 转成 `false`、`skipped` 或
`DirectCombatClickResult` 正常返回。其它 terminal/correlation 异常使用既有 `TaskFatalException`，不能继续 strict-696
下一行。

### 5.5 UUID 与零 retry

- 每个 reached site 只执行一次 public `TurnGameClient.execute`。该 public client 在一次 invocation 内调用
  `actionIdSupplier.get()` 一次并生成 canonical `UUID.toString()`；`NpcClickService` 不生成、传入、缓存或复用 actionId。
- 当前 `NpcClickService` 2123 的 `UUID.randomUUID()` 是既有 `PendingSmartClickEvidence.proofToken`，不是 turn
  actionId；S2 不得借用或改变它。
- 两个不同 `ALT_C` site 若都被业务到达，必须是两个不同 UUID；同一 site 的两次独立业务到达也各自 fresh。
- command/outcome uncertainty 后无自动 retry/replay/resend/same-UUID execution；业务显式再次到达另一调用点才是
  新 invocation、新 UUID。
- 不新增 session、owner、ledger、TTL、durable workflow、permit、local uncertainty resolver 或后台补偿动作。

## 6. 父级后续 gate 提醒

- S2 子卡 test write set 为空；production `SOURCE DELIVERED` 及其源码逐行复核不能冒充 TURN-28 parent test gate。
- TURN-28 仍独占完整 `NpcClickTurnContractTest`，其中应覆盖四个到达条件、fresh/distinct UUID、两步 exact
  correlation、所有 terminal/uncertain、metadata/context drift、零后续与零 retry。
- 本 helper 未运行测试或 compile，也没有形成 review verdict。正式 delivery 后的 source review、独立 reviewer、
  named test 与 stable-writer build 均仍由父级按权威计划处理。
- 无已批准业务差异；按 strict `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

TRUE_EOF PRECHECK_COMPLETE
