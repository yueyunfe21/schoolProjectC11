CLAIMED | helperInstanceUuid=`7c44041f-c58e-4911-a5ae-72b19d5c8e06` | nickname=`Cartan` | role=`CR271 Internal delivery-preflight helper` | claimedAt=`2026-07-16T11:04:23.0445351-04:00`

# CR271 TURN-34BP1 Delivery Preflight Helper R1

> 快照截止：`2026-07-16T11:08:57.4342090-04:00`  
> 角色：Internal delivery-preflight helper；不是 implementation owner、reviewer、approver 或 parent。  
> 结论边界：`PRECHECK_ONLY / NON_PARENT_APPROVAL / NON_BLOCKING_HELPER_OUTPUT`。下文的 P0/P1/P2 均为供父级复算的候选，不是本 helper 的 finding、批准或阻断裁决。

## 1. 执行边界与权威输入

本轮唯一写入是本报告。未修改 Java、POM、test、现有卡、权威计划、`ACTIVE_WORK.md`、
`PACKAGE_ARCHITECTURE.md` 或其它文件；未运行 Maven/JUnit/compile/package；未启动
runtime/application/server/Task/UI/capture/input；未执行 Git mutation。两仓全部既有 dirty/untracked
字节原样保护。

已完整读取并用于本报告：

1. `AGENTS.md` 全文。
2. `docs/DHXY_CONTEXT.md` 全文。
3. `docs/ACTIVE_WORK.md` 顶部最新 CR271 状态。
4. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节全文。
5. HTTPS turn 协议 `2026-07-15-https-turn-thin-client-protocol-design.md` 全文。
6. `docs/业务逻辑.md` 全文，尤其 `:215-226` 的业务基线门与 `:1253-1266` 的 STOP/暂停语义。
7. TURN-34BP1 child card 直至当前 physical true EOF，以及 TURN-34B parent card 全文。
8. Cloud 当前 `TaskExecutionContext.java` 与 `TaskExecutionContextTurnContractTest.java` 全文。
9. 为核实公共接点而只读了 `TaskCheckpoint`、`TaskCheckpointDecision/Outcome`、
   `TaskExecutionContextHolder`、`TurnWindowMetadata`、`TurnGameClient`、`CloudTurnCommandPort/Exchange` 和
   `TaskMaintenanceService` 的首 checkpoint/delegate 路径。
10. `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 commit metadata、基线
    `TaskExecutionContext.java` 全文，以及基线 `TaskMaintenanceService.runOpportunisticMaintenance`、
    `maybeCleanSummonSkill` 与 `checkpoint` 原始路径。

## 2. 两仓与交付快照

| Repo | Branch | HEAD | 说明 |
|---|---|---|---|
| DHXY | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 大量既有 dirty/untracked；HEAD 不是业务基线 |
| Cloud | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 大量既有 dirty/untracked；HEAD 不是业务基线 |

业务权威仍是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 与 `docs/业务逻辑.md`；两仓 HEAD 只用于
说明当前工作树来源。

### 2.1 当前物理字节

| Artifact | Lines | mtime (`-04:00`) | SHA-256 | Git 可见性 |
|---|---:|---|---|---|
| Cloud `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java` | 502 | `2026-07-16T11:01:13.3242454-04:00` | `05bbfda35e5471748f754c3f1e0be9b3eddc7065fd09a0cb58c72ab1322b7d99` | untracked |
| Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java` | 829 | `2026-07-16T11:06:22.4883450-04:00` | `2af2c0aefedf5eb3e837757632d9892d11b3be8772721c6d275baadd5bd63385` | `.gitignore:15` 忽略 `src/test/` |
| TURN-34BP1 child card | 139 | `2026-07-16T11:07:08.6624210-04:00` | `cb064e04db7a5ca7f3a9be73e66757dc4200a0d74d711f3033f2527bc9dc070a` | DHXY reports 目录既有 untracked |
| TURN-34B parent card | 182 | `2026-07-16T09:29:53.7121194-04:00` | `fb3a5044a629389e6aabdbb18b0c2e7b30f76adac69d89be258673bc52d85985` | DHXY reports 目录既有 untracked |

child card 冻结起点是 production `491` 行 / `6d4e4a20...`、test `753` 行 / `d667d695...`。本 helper
观察到 production 在 `11:01:13` 先单独变化、test 当时仍为冻结 SHA；该阶段有源码增量但不是交付。
只有 child card 后来在 physical true EOF 写入
`EXTERNAL-C SOURCE+TEST DELIVERED`（`:103-139`，时间 `11:07:08.560-04:00`）后，上表两份 SHA 才成为
可供父级复算的 delivery snapshot。当前真尾没有 `APPROVED`、`CLOSED` 或 compile/test exit 0 声明。

## 3. 696a12b0 基线边界

`696a12b0` 的 `TaskExecutionContext.throwIfStopRequested()` 已是 public `long` checkpoint，按 stop、pause、
window identity suspension、stop 的顺序执行。当前 Cloud dual-path context 保留同一 public shape；BP1 只允许
加强 turn-native metadata fence，不得改 legacy delegate、pause cadence、interrupt 或 STOP 投影。

基线 `TaskMaintenanceService.java` 的精确业务顺序是：

```text
runOpportunisticMaintenance
  -> checkpoint(context)
  -> optional maintenance broadcast and terminal short-circuit
  -> optional Summon maintenance
     -> checkpoint(context)
     -> cleanSummonSkillsOnce(...) exactly once
  -> no action
```

基线行证据为 `696a12b0:TaskMaintenanceService.java:579-600,744-756,981-985`；当前 Cloud 对应路径仍为
`:581-602,746-758,983-986`。BP1 不得修改该 Service 或把 metadata negative signal 解释成新的业务事实。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## 4. Latest title/HWND/process exact-generation 调用链

1. HTTPS 协议 `:75-79` 固定每个 request/outcome 的 current `deviceId/windowId/windowTitle/nativeHandle/processId`。
2. `CloudTurnExchange.java:98-104` 只读 exact device/window slot 的 newest accepted metadata；
   `:145,163,176` 在合法 request/outcome 路径把该 slot 原子替换为本次 `requestWindow`。它不保留历史 generation。
3. `TaskExecutionContext.turnNative(...)` 在 `TaskExecutionContext.java:75-81,96-110` 冻结 initial metadata 和
   exact invocation context，并保存 singleton `TurnGameClient.bind(invocationContext)` 的轻量 bound view。
4. public `TaskExecutionContext.throwIfStopRequested()`（`:259-271`）在 turn-native 路径进入
   `checkpointTurnMetadata()`（`:385-410`），其第一步调用 `latestExactTurnMetadata()`（`:412-440`）。
5. `TurnGameClient.latestWindowMetadata()`（`:156-158`）先用 `currentExactContext()` 拒绝错 holder/context，再调用
   `CloudTurnCommandPort.latestWindowMetadata(deviceId, windowId)`；该 observation 不造 UUID、action 或 command。
6. delivered production 在 `TaskExecutionContext.java:416-438` 依次检查 missing、device、logical window、
   `windowTitle/nativeHandle/processId`，native mismatch 复用
   `TaskCheckpointDecision.turnWindowMismatch()` / `TaskCheckpointOutcome.WINDOW_MISMATCH`。
7. `TaskMaintenanceService.runOpportunisticMaintenance()` 在 `:584` 先调用 private `checkpoint(context)`；该方法
   在 `:983-986` 调 public `context.throwIfStopRequested()`。首个可能的 Dialog delegate 在 `:587-605`，Summon
   delegate 在 `:758`，所以共享 context fence 在不改 Service 的前提下可达首个 delegate 之前。

禁止把 initial getters `getNativeWindowTitle/Handle/ProcessId` 当作 latest fence：它们只返回冻结的 initial 值。

## 5. Public checkpoint reachability 清单

父级应逐项确认以下现有路径未被新增 wrapper 或 private test seam 替代：

| Public entry | Existing route | BP1 验收点 |
|---|---|---|
| `TaskExecutionContext.throwIfStopRequested()` | `checkpointTurnMetadata -> latestExactTurnMetadata` | 必须是 title/HWND/process drift 主测试入口 |
| `TaskCheckpoint.throwIfStopRequested(context, message)` | 直接调用上述 context public API | public shape/返回值不变 |
| `TaskCheckpoint.throwIfStopRequested(holder, message)` | `holder.checkpointIfPresent()` 后进入上述 API | holder path 不改 |
| `TaskCheckpoint.throwIfStopRequested(context, holder, message)` | explicit 后 holder，保持基线顺序 | 不因 BP1 增删 checkpoint |
| `TaskExecutionContextHolder.checkpointIfPresent()` | 当前 thread-local context 的上述 API | 无 TurnGameClient 注入新环 |
| `TaskMaintenanceService.runOpportunisticMaintenance(...)` | 首个 private checkpoint 调上述 public API | Dialog/Summon delegate/action/UUID 前拒绝 |

named test 应只经 public `turnNative(...)` 与 `throwIfStopRequested()` 驱动 metadata slot；不得反射
`latestExactTurnMetadata()` 或未来的 invalidation state 来替代公共可达性证明。

## 6. A -> B -> A' 必要负证据

卡片冻结合同 `:28-38` 要求的是**同一个 initial-A context 的顺序历史**：

| Step | Same context initial authority | Scripted latest slot | Expected public checkpoint result |
|---:|---|---|---|
| 1 | A | 独立对象 A0，值等于 initial A | `0L` / pass |
| 2 | A | 同 device/window、native 三元组不同的 B | `WINDOW_MISMATCH` |
| 3 | A | 独立对象 A'，值再次等于 initial A | 仍为 `WINDOW_MISMATCH`，旧 context 不复活 |

必要断言：`A0.equals(A') == true`、`A0 != A'`；三次 public call 精确消费三个 slot，
`metadataReads==3`、script deque 空、`executeCalls==0`、`actions.isEmpty()`、`uuids.calls==0`。三次调用是测试对
三段历史的显式驱动，不是 runtime 自动 retry。

当前 delivered source `TaskExecutionContext.java:35-41,412-440` 只有 immutable initial fields 和逐次 direct
equality，没有 context-local monotonic invalidation 状态。按当前字节复算，同一 initial-A context 会表现为：

```text
A0 -> pass
B  -> WINDOW_MISMATCH exception
A' -> direct equality 再次成立，因此 pass
```

当前 delivered test `TaskExecutionContextTurnContractTest.java:443-457` 实际创建的是 **initial B context**，只给它
一个 A' slot 并断言一次 mismatch；随后又创建一个**新的** initial-B context 证明 B pass。它没有让同一个
initial-A context 依次观察 A0、B、A'，因此不能否定上述复活路径。该差异是本报告提供给父级的首要候选，
不是 helper 的阻断裁决。

## 7. 父级精确 delivery 检查清单

### 7.1 Ownership、真尾与字节

- [ ] 只以 child card physical true EOF 的 `EXTERNAL-C SOURCE+TEST DELIVERED` 触发审查；此前 mtime/SHA 变化均不算交付。
- [ ] 从磁盘重新计算 production/test/card 的 line、mtime、SHA，不照抄 delivery 表。
- [ ] 当前两 Java SHA 必须与真尾 `05bbfda3...` / `2af2c0ae...` 对应；若任一文件 mtime 晚于 delivery 且真尾未更新，退回重新交付取证。
- [ ] delivery 后没有第二 writer、D heartbeat 回放、后续未声明字节或冒充 `APPROVED/CLOSED`。

### 7.2 最小写集

- [ ] Java 只允许 `TaskExecutionContext.java` 与唯一 existing named test。
- [ ] 第三个允许写入仅为 TURN-34BP1 child card append-only delivery/rework 记录。
- [ ] 不改 `TaskMaintenanceService`、`TaskCheckpoint*`、`TaskExecutionContextHolder`、`TurnWindowMetadata`、
  `TurnGameClient`、exchange/protocol/result、POM、parent card、AutoCombat、DHXY Java 或第三个 test。
- [ ] 因 production untracked、test 被忽略，不能用普通 `git diff/status` 空白证明写集；必须按逐文件 SHA/mtime/line 和 lane freeze 复核。

### 7.3 Production contract

- [ ] 保留 missing -> device -> logical window -> native generation 的既有分类顺序。
- [ ] native generation **只**包含 `windowTitle/nativeHandle/processId`；不得比较整个 record，尤其不得把
  `windowRect/pauseRequested/stopRequested` 的合法变化当 generation drift。
- [ ] mismatch 复用 typed `TaskCheckpointTransitionException(TaskCheckpointDecision.turnWindowMismatch())`；
  不新增 protocol 字段、enum、exception 或业务状态。
- [ ] 同一 context 一旦实际观察到 B，generation invalidation 必须单调且不可由 value-equal A' 清除；状态必须
  context-local、非 static/global，并具有可审查的并发可见性/线性化边界。
- [ ] 每次 public checkpoint 仍只读一次 latest slot；pause loop 的后续读保持既有 250ms cadence，不新增 retry。
- [ ] exact metadata 的 stop/pause/interrupt 与 legacy delegate 语义、全部 public API shape 均不变。
- [ ] 不暴露 raw mutable metadata，不增加 wrapper/helper nesting、owner/session/ledger/TTL/durable workflow。

### 7.4 Named-test contract

- [ ] exact、missing、device、window、title、HWND、process 七类各自通过 public path，typed outcome 精确。
- [ ] A->B->A' 使用**同一个 initial-A context、三个 scripted slot、三次 public checkpoint**，第三次仍拒绝。
- [ ] A0/A' 既断言 value-equal，又断言 object-distinct；不能用 initial-B -> A' 一次 mismatch 代替历史证明。
- [ ] 每个 negative case 断言零 command、零 action、零 UUID、一次 metadata read、无内部 retry；A->B->A' 总读数为 3。
- [ ] stop/pause、250ms cadence、legacy surface、既有 8 tests 不删除或弱化。
- [ ] 不通过 reflection/private-method/source scan/wall-clock race 证明 BP1 checkpoint。当前 test `:684-690` 有冻结前
  已存在的 `TurnGameClient` UUID-supplier reflection seam；父级需明确“禁止新增 reflection”还是“整文件零 reflection”。
  在两文件写集内，直接计数 UUID 依赖该既有 seam，不能临场删 UUID 断言来迎合口径。

### 7.5 后续门

- [ ] 先做 parent production 与 assertion review，再做两名独立 reviewer；本 helper 不计 reviewer。
- [ ] writers 稳定后才运行授权 named test `mvn -q -Dtest=TaskExecutionContextTurnContractTest test` 和适用 Cloud compile。
- [ ] 当前 delivery 明确未运行 Maven/JUnit/compile/package；不得把源码自查写成 exit 0。
- [ ] unit test 只证明 metadata/checkpoint contract，不替代 TURN-41 real Win32 user runtime。

## 8. 最小修复写集（若父级采纳候选）

| File | 最小目的 | 不得扩大到 |
|---|---|---|
| `TaskExecutionContext.java` | 在现有方法原位加入 private、context-local、单调且并发可见的 generation invalidation；保留现有检查顺序与 typed outcome | public API、Service、protocol、Task、legacy branch |
| `TaskExecutionContextTurnContractTest.java` | 把当前 initial-B -> A' 单次用例改为同一 initial-A context 的 A0 -> B -> A' 三 slot/三 call 负证据，并补零 UUID/action/read 断言 | 新测试类、reflection checkpoint、runtime/race |
| TURN-34BP1 child card | append-only 记录 rework claim/delivery 与最终 SHA/line | parent card、主计划、`ACTIVE_WORK` |

不存在需要修改第三个 Java 文件、POM、DHXY 文件或 `TaskMaintenanceService` 的技术理由。

## 9. 潜在 P0/P1/P2 候选索引

| Candidate | Preflight evidence | 父级应复算的判定点 |
|---|---|---|
| P0 | 当前 preflight 未识别到 P0 候选 | 仍须确认没有越写集、raw mutable authority 或 wrong-window delegate/input 新入口；最终级别只归父级 |
| P1 candidate A | delivered production 是 stateless direct equality；B exception 后 A' 可再次通过 | `TaskExecutionContext.java:35-41,412-440`；用同一 context 三次调用复算 |
| P1 candidate B | delivered `valueEqualRebind...` 不是 A->B->A' 历史，只是 initial B -> A' 一次 mismatch + 新 B context pass | test `:443-457` 对照 child card `:37-38` |
| P2 candidate A | 新 negative/exact 断言只锁 `executeCalls`/`metadataReads`，未锁 `uuids.calls`、`actions`、script exhaustion | test `assertTransition:627-643` 及 exact test `:429-435` |
| P2 candidate B | delivery 写“未用 reflection”，但 named test 保留冻结前 constructor reflection | test `:40-42,684-690`；父级明确 no-new 与 no-any 的口径，不把继承 seam 冒充新改动 |
| P2 candidate C | 若后续加入 mutable safety latch，类级“only immutable”说明会失真 | production class JavaDoc `:24-29` 应与真实 private safety state 一致 |

以上均为 preflight candidate。本文既不批准 TURN-34BP1，也不宣布它 Blocked；parent/reviewer 必须从当前真尾
SHA 独立复算、写回卡片并执行各自职责。

**非父级批准；非 reviewer 结论；无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。**

TRUE_EOF PRECHECK_COMPLETE
