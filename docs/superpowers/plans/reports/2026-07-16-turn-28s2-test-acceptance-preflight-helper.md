# CR271 TURN-28S2 external restart test/acceptance preflight

## 1. 角色与本轮边界

- 角色：CR271 Internal helper，只做 TURN-28S2 external restart 的 test/acceptance preflight。
- 不是 implementation owner、reviewer、父级 manager/final reviewer，也不领取 TURN-28S2/TURN-28。
- 本报告不批准、不阻断、不改变 CR271、TURN-28、TURN-28S2 或任何 reviewer/build 状态。
- 本轮唯一写入是本报告。未修改 Java、测试、卡片、计划、`ACTIVE_WORK`、dashboard、POM、resource、runtime、
  input 或 Git 状态。
- 未运行 Maven、JUnit、compile/package、application/server、Task/UI、runtime、capture/OCR/input；未执行
  stage/commit/checkout/reset/restore 或任何其他 Git mutation。

## 2. 已读取的权威证据

本轮完整读取并交叉核对：

1. `D:/mavenProject/DHXY/AGENTS.md`；
2. `docs/DHXY_CONTEXT.md`；
3. `docs/ACTIVE_WORK.md` 顶部 CR271 当前块；
4. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节；
5. HTTPS turn 协议规格与 protocol foundation；
6. `docs/业务逻辑.md`，包括 strict 修罗基线、NPC Click FIFO、direct-combat 与零自动 retry 约束；
7. TURN-28S2 子卡全部正文及物理 true EOF；
8. TURN-28 父卡的 exact write set 与唯一 named test contract；
9. Cloud 当前 `NpcClickService.java` 全部 3374 行及 strict-696 mirror；
10. 相关现有 contract harness：完整读取 `AutoCombatPanelTurnContractTest`、
    `DialogDetectionTurnContractTest`、`TurnGameClientContractTest`、
    `TaskExecutionContextTurnContractTest`，并索引其它 service turn tests 的 terminal/UUID 模式；
11. 当前 `TurnGameClient`、`TurnInvocationResult`、`CloudTurnCommandResult` 与输入 step 模型；
12. 两仓完整 `git status --porcelain=v1 --untracked-files=all`。

权威结论：业务行为只认
`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。TURN-28S2 只迁移四处物理 Alt shortcut + baseline WAIT 的
执行所有权，没有已批准业务差异。

## 3. 当前只读快照

快照时间：`2026-07-16T11:25:35-04:00`。

| Repo | Branch / HEAD | porcelain | 与本预检直接相关 |
|---|---|---:|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 732 项：43 `M`、1 `D`、688 `??` | S2 子卡为受保护 `??`；本报告写入前不存在 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 项：9 `M`、541 `??` | Cloud 当前 NpcClick 与 turn 新链为受保护 `??`；named test 不存在 |

Cloud 当前与 strict mirror 均为：

- 3374 行，175367 bytes；
- SHA-256 `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`；
- 两文件 byte-identical；当前 mtime `2026-07-16T08:50:50.2225209-04:00`。

S2 子卡当前为 181 行，物理 EOF 是：

```text
<!-- TRUE_EOF: TURN-28S2 PARENT-RESTART FRESH-EXTERNAL-B-NEXT ZERO-OWNER INITIAL-SHA-UNCHANGED CLAIM-REQUIRED STRICT-696 2026-07-16T11:03:03.155-04:00 -->
```

因此当前事实是 fresh External B 尚未 claim、零 owner、零 S2 WIP。该事实只是 preflight 快照，不是本 helper
对 ownership 的授予。

## 4. 准确写集必须分层

### 4.1 本 helper 的唯一写集

只有：

`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-28s2-test-acceptance-preflight-helper.md`

### 4.2 Fresh External B 的 TURN-28S2 写集

只有：

1. Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`；
2. append-only `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md` claim/delivery evidence。

**S2 test write set 精确为空。** Fresh External B 不得创建或修改任何 test；本报告也不把以下测试清单授予
External B。协议/client/context/result、recognizer、DHXY、caller、POM、resource、其它卡和全部现有测试均只读。

### 4.3 未来 TURN-28 父卡 test-source 写集

只有父级之后明确分配 test owner 时，以下保留写集才生效：

1. 唯一 test file：
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java`；
2. append-only TURN-28 父卡交付证据：
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28.md`。

该 test file 当前不存在。不得新增第二个 `NpcClick*Test`、共享 test helper 文件、fixture/resource、POM 依赖或
source guard。S2 assertions 必须并入父卡这一份完整 named test，不能用 S2-only partial test 冒充父卡 test gate。

## 5. 四个 active site 的 696 acceptance freeze

行号只对应当前 strict-696 快照；实现后应按 public method、branch condition 和 description 重新定位。

| Site | 当前 active 位置与到达条件 | 一次到达的 exact action | 仅严格成功后允许的后续 | 必须保留的零动作反例 |
|---|---|---|---|---|
| `GENERIC_ALT_C` | `clickNpcSmart` 624-627；第一条完整 pipeline miss、未 stop、且不是 `COMBAT_TARGET` | `ALT_C -> WAIT 700ms` | 恰好一条第二 pipeline `dialog-after-alt-c` | `COMBAT_TARGET` 零 generic Alt+C、零第二 pipeline |
| `FLYING_ALT_C` | `tryDirectCombatTargetClick` 667-670；flying state 为 `FLYING` | `ALT_C -> WAIT 700ms` | 继续到同方法公共 Alt+A 点 | `UNKNOWN` 零 Alt+C 且零 Alt+A；grounded 零 dismount |
| `DIRECT_ALT_A` | 同方法 682-685；grounded，或 FLYING dismount 已成功 | `ALT_A -> WAIT 350ms` | 既有 direct-combat candidate pipeline | Alt+A terminal 后零 pipeline、零 exit-right-click |
| `NAME_LAYER_ALT_4` | `prepareNpcPipelineNameLayerOnce` 948-951；ordinary pipeline 实际到达 name-layer | `ALT_4 -> WAIT 400ms` | 既有 tooltip/yellow/formula/Ctrl 顺序 | direct-combat、early memory/dialog return 均零 Alt+4 |

必须按业务到达次数计数，而不是错误地要求每个 public scenario 总共只有一条 action：

- ordinary 两轮都到达 name-layer 的合法 key 顺序是
  `ALT_4/400 -> first pipeline miss -> ALT_C/700 -> ALT_4/400`；第二个 Alt+4 是第二条业务 pipeline，
  不是 transport retry。
- direct `FLYING` 的合法顺序是两条独立 action：`ALT_C/700` 后 `ALT_A/350`，两个 fresh UUID，不能合并。
- direct grounded 只有 `ALT_A/350`；`UNKNOWN` 两条都没有。
- ordinary 在 name-layer 前已业务返回时是零 Alt+4；测试不得把 Alt+4 改写成无条件入口动作。
- 当前 3271 和 3302 附近两个 private legacy/direct Alt+4 site 不属于 S2，不纳入 active-site assertion，
  也不得因测试方便而迁移。

## 6. 最小 S2 测试边界

父卡唯一 `NpcClickTurnContractTest` 中，S2 子矩阵最小收敛为以下五组 public-path 测试。可以在一个测试方法内
使用 scenario table/loop，但不能降低断言内容，也不能反射调用 private shortcut helper 或扫描源码字符串。

### T1. `ordinaryShortcutTopologyPreservesStrict696`

通过真实 `clickNpcSmart` 驱动两个 ordinary pipeline：

- 两轮都到达 name-layer 时，过滤 key actions 后精确为 `ALT_4/400, ALT_C/700, ALT_4/400`；
- generic Alt+C 只出现一次，第二 pipeline 只进入一次，绝无第三 pipeline；
- 同样的 first-pipeline miss 若 request 是 `COMBAT_TARGET`，generic Alt+C 与第二 pipeline 均为零；
- early memory/dialog return 在 name-layer 前发生时，Alt+4 为零。

### T2. `directCombatShortcutTopologyPreservesStrict696`

通过真实 `tryDirectCombatTargetClick` 分别驱动：

- `FLYING`：精确两条 key actions，`ALT_C/700` 后 `ALT_A/350`，UUID 不同；
- grounded/continuing：零 dismount，只有 `ALT_A/350`；
- `UNKNOWN`：零 key action，并保持原 `DirectCombatClickResult.skipped(...)` 业务分支；
- direct candidate pipeline：零 Alt+4；只有既有 BattleRadar verifier 能形成 combat success。

### T3. `eachReachedShortcutUsesOneActionOneUuidOneRawTypedResult`

以四个 site 为 scenario table。对每次实际到达记录 `beforeExecuteCount/beforeUuidCount`，断言增量而非总量：

- `executeCalls` 增量恰为 1；UUID supplier 调用增量恰为 1；
- actionId 是 canonical UUID，且 submitted action、`TurnInvocationResult`、真实 `TurnOutcome` 使用同一 actionId；
- 不同 reached site 或后续独立业务到达使用不同 UUID；
- action 的 exact current device/window 与当前 binding 一致；
- command port 返回真实 `CloudTurnCommandResult`，经真实 public `TurnGameClient` 形成
  `TurnInvocationResult`；不得把 command/outcome 预先折成本地 boolean fake。

这里的“raw result”指真实 typed command/outcome 结果不被测试替身篡改。S2 key action **不请求 raw PNG**，因此
`TurnInvocationResult.frame()`、`TurnOutcome.frame()` 与 `CloudTurnCommandResult.frame()` 必须全部为 null。
父卡 `IMG+LX` 的 NPC capture/raw-PNG 场景属于 TURN-28 其它切片，不得塞进 S2 shortcut action。

### T4. `onlyExactCompletedTwoStepResultContinues`

四种 action shape 分别断言：

```text
TurnAction.fullWindowFailureEvidence = false
step[0] = index 0 / INPUT / KEY_TAP / key=ALT_C|ALT_A|ALT_4
step[1] = index 1 / WAIT / waitMs=700|350|400
```

逐字段要求：

- INPUT 的 `x/y/endX/endY/scrollDelta/text/clickDelayMs/queueHoldMs` 全部为 null；`waitMs/capture/match/localService`
  也为 null；
- WAIT 的 `inputAction/input/capture/match/localService` 全部为 null；
- 没有 CAPTURE、MATCH_TEMPLATE、LOCAL_SERVICE、mouse、frame request、第二个 WAIT 或 foreground fallback；
- 唯一继续结果是 command `COMPLETED`、outcome `COMPLETED`、`failedStepIndex == null`、exact full
  window metadata、恰好两个 step results，分别 `0/INPUT/COMPLETED` 与 `1/WAIT/COMPLETED`；
- 两个 step result 的 match/local result 均为空，三个 frame 位均为空；
- shortcut completion 只允许执行下一条 696 业务行，不生成 NPC-found/dialog-open/click/combat/OCR 真值。

### T5. `terminalUncertainDriftAndMalformedStopAtTheReachedSite`

使用一个 shared scenario matrix 覆盖四个 site，但每个 site 都必须有 caller-specific downstream sentinel。每例用
fresh harness，脚本 port 在 terminal 后收到任何 command 就立即测试失败。

完整 terminal set：

1. command `BUSY`；
2. command `DUPLICATE_ACTION_ID`；
3. command `TIMED_OUT_UNCERTAIN`；
4. command `INTERRUPTED_UNCERTAIN`；
5. outcome `FAILED`；
6. outcome `STOPPED`；
7. outcome `DUPLICATE_OR_UNCERTAIN`；
8. null/malformed、wrong actionId、wrong step count/index/type/status、wrong `failedStepIndex`、unexpected frame；
9. device/window/title/HWND/process/rect/control metadata drift；
10. holder/context identity drift或 public client correlation exception。

每例必须断言：

- 在该 site 到达前已有 action 可保留；site 到达后的 action/UUID 增量最多为 1，之后增量为 0；
- command uncertainty 仍保留原 typed status，不能伪造 outcome/frame 或业务 false/miss；
- confirmed stop 只抛现有 `TaskStopRequestedException`/checkpoint 路径；未确认 STOP/interruption 及其它 terminal
  走现有 `TaskFatalException` 路径；
- terminal/correlation exception 不被 `clickNpcSmart` 的 boolean 或 `DirectCombatClickResult.skipped(...)` 吞掉；
- 零 fallback 到 `InputSequences`、`InputProvider`、foreground input 或第二 public turn。

## 7. “零后续”必须落到四个 caller

| Terminal site | terminal 后必须为零的可观察后续 |
|---|---|
| `GENERIC_ALT_C` | 第二 pipeline、第二轮 Alt+4、候选/click/verifier/memory 与任何 command |
| `FLYING_ALT_C` | 公共 Alt+A、direct pipeline、right-click exit 与任何 command |
| `DIRECT_ALT_A` | direct pipeline、BattleRadar success path、right-click compensation 与任何 command |
| `NAME_LAYER_ALT_4` | tooltip、yellow、purple formula、Ctrl、capture/OCR/template/click/verifier 与任何 command |

不能只断言“同一种 key 没有再次出现”。必须让 downstream collaborator/sentinel 保持零调用，并让 scripted
command port 拒绝任何 terminal 后 command，才能证明真正的 zero later action。

## 8. 零 retry 的精确断言

- 每个 reached site 的一次 public `execute` 只消费一条 scripted raw result；port queue 不准备“重试成功”回复。
- uncertainty/terminal 后 `executeCalls`、UUID supplier calls、actions 均不再增长；同 UUID 绝不 resend，fresh UUID
  也绝不 replay。
- `uuids.calls == actions.size() == executeCalls` 只作为整个 harness 的全局一致性断言；site-level 仍使用增量，
  以容纳合法的前置/后续业务 actions。
- generic 成功后第二 pipeline 与 FLYING 成功后 Alt+A 是基线明确授权的后续业务调用，各自 fresh UUID；
  这两者不是 transport retry。
- 不加入 retry counter、TTL、session、ledger、owner、permit、durable workflow、后台 compensation 或 local
  uncertainty resolver。

## 9. 测试 harness 复用边界

未来 named test 应在同一个 test class 内复用现有模式：

- `AutoCombatPanelTurnContractTest`：ordered key+WAIT、scripted command port、terminal 后 command 计数、
  fresh UUID 集合；
- `DialogDetectionTurnContractTest`：输入字段 null 检查、confirmed/unconfirmed stop、metadata drift 与 UUID 前零
  command；
- `TurnGameClientContractTest`：one UUID/one command、typed raw statuses、step/action/device/window correlation；
- `TaskExecutionContextTurnContractTest`：exact bound context、native generation drift 与 checkpoint 零 action。

允许在唯一 test class 内放 nested `Harness`、`Scenario`、`ScriptedCommandPort`、`CountingUuidSupplier` 与内存
sentinel。测试必须经过真实 public `clickNpcSmart`/`tryDirectCombatTargetClick` 和真实 public
`TurnGameClient`；不能直接测 private helper、伪造 helper boolean、运行真实 HTTP/runtime/input，或修改生产代码只为
source/reflection guard。

S2 keyboard 子矩阵无需 PNG、临时文件或 fixture。父卡其它 `IMG+LX` 场景需要的图片也必须是内存 PNG，并留在同一
named test，不新增 resource 写集。

## 10. 后续验收命令与本轮未执行项

权威计划为父卡唯一 named test 保留的命令是：

```text
cd D:/mavenProject/dhxy-cloud-brain
mvn -q -Dtest=NpcClickTurnContractTest test
```

该命令只能在父级明确 test owner 已交付完整 test source、所有相关 Java writers 稳定后执行。本 helper 没有运行它，
也没有运行 compile/package。测试 exit 0、Cloud compile/build、source/test-source review、双 reviewer 与 fresh runtime
均不是本 preflight 的结论。

## 11. Preflight conclusion

- Fresh External B 的 S2 test write set 仍严格为空；本报告没有给它增加测试或验收所有权。
- 父卡未来唯一 `NpcClickTurnContractTest` 的 S2 最小边界已冻结为：四 site 的 696 到达拓扑、每 reached site
  one action/UUID/raw typed result、exact two-step/no-frame completion、terminal/uncertain caller-specific 零后续，
  以及零 retry/replay/resend。
- 无已批准业务差异；按 strict `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

TRUE_EOF PRECHECK_COMPLETE
