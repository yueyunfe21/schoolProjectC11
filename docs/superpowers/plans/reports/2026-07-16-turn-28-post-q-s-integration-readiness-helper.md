# CR271 TURN-28 post-Q/S integration readiness delta helper

## 1. 角色、范围与结论边界

- 角色：CR271 Internal helper，只做 TURN-28 在 TURN-28Q Repair #3 与 TURN-28S2 source pass 之后的
  integration-readiness delta。
- 不是 implementation owner、reviewer、父级 manager/final reviewer，也不领取、批准、阻断或改变任何卡状态。
- 本报告只给父级提供下一张最小 implementation 子卡的冻结输入；真正 child-card 创建、true-EOF claim、
  source review、test review、build 与状态裁决仍归父级/后续 owner。
- 本轮唯一写集是本报告。未修改 Java、test、TURN 卡、`ACTIVE_WORK`、权威计划、dashboard、POM、resource、
  config、runtime 或 Git 状态。
- 未运行 Maven、JUnit、compile/package、application/server、Task/UI、capture/OCR/input，也未调用 Git。

Snapshot cutoff: `2026-07-16T11:35:33-04:00`。

## 2. 已完整读取与权威层级

本轮已完整读取并交叉核对：

1. `D:/mavenProject/DHXY/AGENTS.md` 与 `docs/DHXY_CONTEXT.md`；Cloud 仓未发现额外 `AGENTS.md`。
2. `docs/ACTIVE_WORK.md` 的 CR271 全段，并补读到当前顶部 `11:33` 增量。
3. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节及当前状态头。
4. TURN-28Q、TURN-28S2、TURN-28 parent 三张卡全部正文及各自 physical true EOF。
5. `2026-07-15-https-turn-thin-client-protocol-design.md` 与 protocol-foundation 附录全文。
6. `docs/业务逻辑.md` 全文，重点核对 direct-combat、NPC Click FIFO、strict-696 与零自动 retry。
7. 当前 Cloud `NpcClickService.java` 的 active S2/S3 调用路径、strict-696 mirror，以及 DHXY
   `LocalTurnActionExecutor`、`TurnInputStepExecutor`、`TurnInputActionMapper` 和 frozen queue/worker 路径。
8. 最新 Q Repair #3、S2 delivery/test acceptance、S3 readiness helper 报告。Helper 只作交叉证据，卡片、
   权威计划、协议和业务基线优先。

业务裁决固定为：

`无已批准业务差异；按 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7 基线等价迁移。`

## 3. 当前事实与条件化结论

### 3.1 当前 physical true EOF

| Card | 当前 true EOF | 当前事实 |
|---|---|---|
| TURN-28Q | `PARENT-REVIEW-6 REPAIR-3-REQUIRED P0P1P2=0/2/0 EXTERNAL-A-FRESH-RESTART CLAIM-REQUIRED THREE-FILE-WRITESET` | Repair #3 尚无 fresh claim/delivery/source pass。 |
| TURN-28S2 | `PARENT-RESTART FRESH-EXTERNAL-B-NEXT ZERO-OWNER INITIAL-SHA-UNCHANGED CLAIM-REQUIRED STRICT-696` | S2 尚无 fresh claim/delivery/source pass。 |
| TURN-28 parent | `PARENT S1-SOURCE-PASSED WHOLE-CARD-DECOMPOSED NEXT-SLICE-PREFLIGHT ACTIVE` | 整卡四文件 claim 不复活，后续必须继续按小片推进。 |

因此本报告不把“Q/S 已通过”写成当前事实。以下冻结是一个条件合同：只有 Q Repair #3 与 S2 各自出现
父级明确 source gate、owner release 和稳定字节后，父级才能据此创建下一张 child card。

### 3.2 两仓 status 证据

本轮明确禁止调用 Git，所以没有伪造一份“实时 git status”。最后一份已经存在的完整 status 证据来自
`2026-07-16-turn-28s2-test-acceptance-preflight-helper.md` 的 `11:25:35` 快照：

| Repo | 记录的 branch / HEAD | 记录的 porcelain |
|---|---|---:|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 732 项：43 `M`、1 `D`、688 `??` |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 项：9 `M`、541 `??` |

该 status 是已存在的文档证据，不冒充本轮刷新。`11:35:33` 的非 Git 直接文件快照确认本切片相关字节仍为：

| 文件 | Lines | SHA-256 | 解释 |
|---|---:|---|---|
| DHXY `InputActionQueue.java` | 870 | `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a` | Q Repair #3 尚未落地。 |
| DHXY `InputActionWorker.java` | 811 | `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43` | Q Repair #3 尚未落地。 |
| DHXY `InputActionFrozenExclusiveContractTest.java` | 1283 | `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` | 当前仍是 19-case test source。 |
| DHXY `InputActionRequest.java` | 1148 | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` | Q Repair #3 只读 anchor。 |
| Cloud current `NpcClickService.java` | 3374 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` | S2 尚未落地。 |
| Cloud strict-696 mirror `NpcClickService.java` | 3374 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` | 与 current byte-identical。 |
| Cloud `SmartClickRecognizer.java` | 3026 | `ffbd984a4ed5841ccba6b87bf3378a1e0cb1e7d2bea68be3eed656be7324f102` | 下一片只读。 |
| Cloud `ObjectiveTextRecognizer.java` | 914 | `d3dc3cc247058ae85a6258e6173f8d9b56d7be119443c90a24c4bf6f180f3fe1` | reservation-only，只读/零 diff。 |
| Cloud `NpcClickTurnContractTest.java` | absent | n/a | parent named test 尚未创建。 |

Cloud `TaskExecutionContext.java` 当前另有 TURN-34BP1 provisional writer WIP。它与下一片没有写集交集，但
下一片读取其 public context/client surface；父级创建 child card 前必须在该文件稳定后重读实际 API，不能把本报告
中的旧 snapshot 当成稳定接口证明。

## 4. Post-Q/S 最小剩余 implementation

条件满足后，可立即交给一个 fresh implementation worker 的最小真实生产片建议固定为：

> **TURN-28S3 - 将 `exitDirectCombatClickModeAfterFailure(...)` 内唯一 active failure-exit right-click
> submission 迁到一次 exact-window HTTPS turn action。**

这是最小真实切片，理由是：

1. 它只替换一个 public production 路径中已经存在的物理输入边界，不新增 dormant facade。
2. 它直接复用 S2 已审查的 exact context、metadata correlation、terminal/fatal 投影，不增加 wrapper stack。
3. 它是 TURN-28 第一处真正消费 Q frozen exact-window action-list 语义的 mouse integration：一份 Cloud action
   必须在 DHXY 映射成一份不可拆分 queue request。
4. Recognizer-only 改动没有真实 caller；Ctrl probe 会同时耦合 ROI/raw PNG/Ctrl release/menu/recognizer；普通
   candidate left-click 又会把完整 FIFO/verifier 面一次拉进来。三者都不是 post-Q/S 的最小剩余生产闭环。
5. S3 完成不等于 TURN-28 完成；它只推进 right-click exit mechanics，剩余 mouse candidate、Ctrl/raw-PNG、
   recognizer facade 与 parent named test 仍留给父级后续重新分片。

本文不创建 `TURN-28S3` 卡，也不授予 owner。

## 5. 精确依赖与领取顺序

### 5.1 Source-start prerequisites

父级创建并开放 S3 child card 前，应按顺序取得以下事实：

1. TURN-28Q Repair #3 fresh owner 正式 claim，交付 queue/worker/同一 named test 增量；父级对最新字节记录
   source + test-source pass 并释放 Q owner。
2. TURN-28S2 fresh owner 正式 claim，交付四个 active Alt shortcut 的 `NpcClickService.java` final SHA；父级
   对最新字节记录 S2 source pass 并释放 S2 owner。
3. 重新计算 current Cloud `NpcClickService.java` SHA，必须等于 S2 `SOURCE DELIVERED` final SHA；这个 SHA
   成为 S3 child card 的 `initialSha256`。不得把当前 pre-S2 `cce8...3441` 恢复覆盖到 post-S2 文件。
4. 确认没有 S2 或其它 TURN-28 owner 仍持有 `NpcClickService.java`。
5. 在所有相关 read-dependency writer 稳定后重读：`TaskExecutionContext`、holder/checkpoint、
   `TurnGameClient`、`TurnInvocationResult`、`CloudTurnCommandResult`、turn protocol、DHXY action resolver/
   executor/mapper。S3 必须复用实际 public surface；任何签名/terminal shape 漂移都先刷新 child-card 合同。
6. 父级创建固定 S3 child card，写明本文第 6-9 节和初始 SHA；fresh worker 先在该卡 physical true EOF
   append claim，再编辑唯一 production file。

Q 与 S2 的 implementation 写集互斥，因此两者可并行 source-start。S3 与 S2 同写 `NpcClickService.java`，
所以 S3 必须串在 S2 source pass/owner release 后。Q named test exit 0 与 DHXY compile 是后续 approval/build gate，
不应被误写成 S3 的第二份实现；但 Q 最新 source/test-source gate必须先稳定，S3 才能声称接入的是修复后的
typed-order queue。

### 5.2 Inherited dependencies

- TURN-28 parent 继续继承 `TURN-23 + TURN-24 + TURN-26 + TURN-28P production API`。
- S3 不新增 protocol、factory、executor 或 recognizer dependency，也不重新打开旧 session/queue/poller 方案。
- TURN-27 继续等待 TURN-28 final public API；S3 source pass 不能提前释放 TURN-27。
- Parent final gate 仍保留 TURN-28P tests、TURN-28Q named test、完整 `NpcClickTurnContractTest`、独立 review 与
  stable-writer build。Source-start 和最终 approval/build gate继续分开。

## 6. 互斥写集

### 6.1 Q Repair #3 frozen write set

1. DHXY `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`；
2. DHXY `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`；
3. DHXY `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`；
4. append-only TURN-28Q card evidence。

`InputActionRequest.java`、`InputSequences.java`、turn executor/mapper、focus/keyboard/callers 均只读。

### 6.2 S2 frozen write set

1. Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`；
2. append-only TURN-28S2 card evidence。

S2 test write set 精确为空。

### 6.3 Proposed S3 implementation write set

未来 owner 只允许：

1. Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`；
2. 未来 append-only
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S3.md`。

S3 test write set 精确为空。以下全部只读：Q/S2/parent cards、`SmartClickRecognizer.java`、
`ObjectiveTextRecognizer.java`、所有 tests、protocol/client/context/executor/mapper/factory、全部 DHXY Java、
Dialog/BattleRadar/Navigation/Task/caller、POM/resources/config/baseline trees、`ACTIVE_WORK`、计划/dashboard。

### 6.4 Later parent test-source reservation

未来只有父级另行明确 test owner 后，才允许写：

1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java`；
2. append-only TURN-28 parent card evidence。

该 test file 当前 absent。不得新增第二个 `NpcClick*Test`、共享 test helper、fixture/resource、POM 依赖或
source guard。S2/S3 的 assertion 都必须并入这一份完整 parent named test；现在不能并发领取 partial test slice
冒充 TURN-28 test gate。

## 7. S3 exact HTTPS action contract

### 7.1 唯一 active site

只改 Cloud `NpcClickService.exitDirectCombatClickModeAfterFailure(NpcClickRequest)` 中当前 description 为
`npcClick:directCombat:exitRightClick` 的一处 local submission。当前 strict-696 行为是：

```text
MOVE_MOUSE(exitX,exitY)
-> SLEEP(120)
-> CLICK_RIGHT(exitX,exitY, clickDelay=120)
-> SLEEP(600)
```

每个实际到达的 business attempt 只调用一次 public
`TurnGameClient.execute(steps, false, Duration.ofMillis(120_000L))`。120 秒只是同步 transport fence，不是业务
TTL、重试预算或额外 sleep。

### 7.2 Exact three protocol steps

| Index | Step | Exact payload |
|---:|---|---|
| 0 | `INPUT / MOVE_MOUSE` | `x=exitPoint.x`, `y=exitPoint.y`；其它 input 字段全 null。 |
| 1 | `WAIT` | `waitMs=120`。 |
| 2 | `INPUT / CLICK_RIGHT` | 同一 x/y，`clickDelayMs=120`，`queueHoldMs=600`；其它 input 字段全 null。 |

Action 固定：`contractVersion=1`、当前 exact `deviceId/windowId`、
`fullWindowFailureEvidence=false`。没有 CAPTURE、MATCH_TEMPLATE、LOCAL_SERVICE、raw frame、failure-evidence
request、第二 command 或 local/foreground fallback。

不得增加第四个 protocol `WAIT 600`。`queueHoldMs=600` 已由 mapper 生成同一 queue request 内的第四个
physical action；再加 protocol wait 会重复延迟并破坏原 queue ownership。

### 7.3 One frozen queue request

DHXY 当前 production 组合关系必须保持：

1. `LocalTurnActionExecutor.findMouseSequenceEndExclusive(...)` 把
   `MOVE_MOUSE -> WAIT120 -> CLICK_RIGHT` 识别为同一 mouse sequence。
2. `TurnInputStepExecutor.executeMouseSequence(...)` 将三个 steps 映射为恰好四个 `InputAction`：
   `MOVE_MOUSE -> SLEEP120 -> CLICK_RIGHT(delay120) -> SLEEP600`。
3. 完整 list 只调用一次
   `InputActionQueue.submitFrozenExactWindowActionsAndWait(...)`，同一 transaction、同一 frozen binding、
   同一 focus；MOVE/WAIT/CLICK/HOLD 不能拆 command、拆 queue 或 re-enqueue。

这正是 post-Q integration 的消费点。Q Repair #3 后必须满足：

- pre-enqueue 同时 STOP + A->B->A' 时，typed STOP 在 generation witness 前，返回
  `NOT_STARTED/STOP_REQUESTED`，zero take/focus/input/refresh；
- queued/taken 后同时 STOP + identity/generation drift 时，worker typed safety 在 generic epoch/generation
  drift 前，返回 `NOT_STARTED/STOP_REQUESTED`，one take、zero focus/input/refresh；
- action-list 与 callback frozen entry 的 production 排序一致，但 S3 实际消费 action-list entry；
- STOP 不得被重标为 `WINDOW_BINDING_CHANGED`，也不得因此执行任何 S3 right-click fragment。

### 7.4 Context, UUID, completion and terminal

每个 attempt 独立执行：

1. 从现有 holder 取得 non-null current `TaskExecutionContext`，直接调用 `TaskCheckpoint`；不加 checkpoint wrapper。
2. 保持同一个 context object，取得其现有 `TurnInvocationContext` 与 bound `TurnGameClient`。
3. anchor probe 后读取本 attempt 最新 metadata，核 exact device/window/title/HWND/process/positive rect；anchor
   null 时只用该 exact rect 计算 baseline fallback `(left+512, top+424)`，并要求 point 位于同一 rect 内。
4. UUID-producing `execute` 紧前再次 checkpoint/context/metadata gate。任何 pre-command STOP、missing、drift、
   invalid point 都是 zero UUID/command/input/probe/sleep/later attempt。
5. 每个 reached attempt 的 UUID 只由 public `TurnGameClient.execute` 内部生成一次；不同 attempt 用不同 canonical
   UUID。`NpcClickService` 不生成、传入、缓存或复用 actionId。

只有以下 exact shape 可继续到 mode probe：command `COMPLETED`、outcome `COMPLETED`、action/window metadata
全相关、`failedStepIndex=null`、恰好三个 results
`0/INPUT/COMPLETED, 1/WAIT/COMPLETED, 2/INPUT/COMPLETED`、每个 match/local result null、outcome/invocation
frame 均 null。

`BUSY`、`DUPLICATE_ACTION_ID`、`TIMED_OUT_UNCERTAIN`、`INTERRUPTED_UNCERTAIN`、outcome
`FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN`、null/malformed、任一 metadata/step/frame/context correlation drift
均不得压成 ordinary click miss、`submitted=false`、mode-still-active 或 position-refresh business fact。Confirmed
STOP 走现有 task-stop/checkpoint 路径；其它 terminal/uncertain 走现有 task-fatal 路径。之后 zero probe、zero
300ms、zero later attempt/UUID/command、zero local fallback/retry/replay/resend/cleanup/compensation。

## 8. Strict-696 执行顺序

### 8.1 Ordinary pipeline 与 S2 位置

每一条 ordinary pipeline 保持：

1. request/STOP/window safety；
2. 非 Wubei、非 combat target 的 early dialog gate与 early learned memory；
3. 实际到达 name-layer 时执行 S2 `ALT_4 -> WAIT400`，每条 pipeline 最多一次；direct-combat 路径零 Alt+4；
4. Wubei tooltip-first；
5. main dialog gate：STORY 只处理一次并 re-detect，OPTION 阻止继续；
6. 未在 early 跑过时的 learned memory；
7. non-Wubei tooltip；
8. post-tooltip dialog observation；
9. `TENTATIVE` cutoff；
10. `YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES`；
11. first pipeline miss 后，`COMBAT_TARGET` 直接 false；其它目标恰好一次 S2 `ALT_C -> WAIT700`，再跑恰好
    一条完整 second pipeline，绝无第三条。

Candidate budgets、click hold、yellow single retry、purple no retry、Ctrl 1/9/17 profile、3px origin dedup、15px
formula filter、dialog/combat verifier 次数全部保持 TURN-28 parent frozen 696 contract，本片不改。

### 8.2 Direct-combat 与 S2/S3 接缝

Direct-combat 保持：

1. null/STOP gate；
2. flying-state read：`FLYING` 执行 S2 `ALT_C -> WAIT700`；`UNKNOWN` 零动作并 skip；grounded 零 dismount；
3. 严格成功后执行 S2 `ALT_A -> WAIT350`；
4. 跑既有 direct-combat candidate pipeline，跳过 Alt+4 与普通 dialog pregate；只有 BattleRadar verifier 可形成
   combat success；
5. candidate pipeline verified success 直接返回；未 verified 且未 STOP 才进入 S3 exit loop；
6. 每个 S3 attempt：重新算 player anchor；null 才用 exact current rect fallback；执行一次三-step/four-action
   right-click turn；严格 mechanics completion 后 checkpoint；恰好一次 mode probe；
7. probe `false` 立即 exit success，caller 保持现有 `positionRefreshRequired("direct-combat-failed-after-alt-a")`；
8. probe `true` 执行 baseline `TaskSleep.sleep(300)`，再由 loop 决定下一 attempt；最多三个 right-click attempts；
9. 三个 probe 都为 `true` 时保留 error + `false`，caller 保留现有 `IllegalStateException`；没有第四次 click、
   cleanup、navigation、restart 或 hybrid fallback。

### 8.3 300ms 的 true baseline delta

当前 Cloud source 与 strict-696 mirror 在每次 `modeLikely==true` 后都执行 `TaskSleep.sleep(300)`，包括第三次
probe 仍为 true 的情况。因而：

- `false`：1 command / 1 probe / 0 次 300ms；
- `true,false`：2 commands / 2 probes / 1 次 300ms；
- `true,true,false`：3 commands / 3 probes / 2 次 300ms；
- `true,true,true`：3 commands / 3 probes / **3 次 300ms**，随后 false/caller throw，zero fourth command。

TURN-28 parent 旧短句“WAIT300 only before another attempt”比 strict source 少描述第三次 sleep。父级创建 S3
child card 时应以这里的 actual strict-696 顺序写明“第三次仍 sleep 300”，不得照抄旧短句造成未批准业务差异。
本 helper 不改 parent 卡、不作状态裁决。

## 9. Named tests 与验收归属

### 9.1 TURN-28Q Repair #3 named test

唯一 test file：DHXY `InputActionFrozenExclusiveContractTest.java`。

Repair #3 在现有 19 cases 上只新增两个 deterministic public action-list cases，目标总数 21：

1. `preEnqueueStopWinsOverValueEqualRebindThroughThePublicFrozenActionListPath`；
2. `takenFrozenRequestStopWinsOverIdentityAndGenerationDriftBeforeWorkerHandling`。

必须使用真实 public `InputSequences -> InputActionQueue -> InputActionWorker`，保留
`CLICK_LEFT(delay=150) -> SLEEP(500)`；第一例 zero take，第二例 one real take handoff latch；两例 zero
focus/input/refresh，无 polling/scheduling sleep/retry/re-enqueue。

权威命令（本轮未运行）：

```text
cd D:/mavenProject/DHXY
mvn -q -Dtest=InputActionFrozenExclusiveContractTest test
```

### 9.2 S2/S3 child tests

- S2 test write set：空。
- S3 test write set：空。
- 两片的 source pass 都不能冒充 TURN-28 parent named-test gate。

S2 assertions 后续进入 parent test：ordinary `ALT_4/400 -> ALT_C/700 -> ALT_4/400` topology、combat-target
zero generic retry、FLYING/grounded/UNKNOWN、one action/UUID、exact two-step/no-frame、terminal/uncertain at each
site zero later action。

S3 assertions 后续进入 parent test：第 8.3 节四种 probe 序列、exact three protocol/four queue actions、anchor/
fallback point、pre-UUID gates、metadata/step/frame correlation、attempt 1-3 terminal immediate stop/fatal、zero local
right-click fallback 与 zero fourth command。

### 9.3 TURN-28 parent named test

唯一 test file：Cloud `service/NpcClickTurnContractTest.java`，当前 absent。

权威命令（本轮未运行）：

```text
cd D:/mavenProject/dhxy-cloud-brain
mvn -q -Dtest=NpcClickTurnContractTest test
```

该 test 必须最终驱动真实 production `NpcClickService` 与 public `TurnGameClient`，统一覆盖：完整 FIFO/TENTATIVE、
各 strategy budget、formula immediate/final Ctrl、1/9/17 probe、3px/15px/no-center、raw PNG/after frame、release/
STOP/uncertain、provider-order OCR、atomic click timing、dialog one-read、combat 4 reads/4 false waits、S2 key
topology、S3 right-click exit、pending proof、metadata/correlation terminal、zero legacy/shadow production path。
不能只交 S2/S3 happy path。

TURN-28P 的 `TurnCapturePixelChangeProbeContractTest`、DHXY `TurnInputStepExecutorContractTest`、Cloud
`TurnCapturePixelChangeInvocationContractTest` 及双仓 golden/validator 仍是 parent integration gate；S3 不修改或
重跑它们。所有 named tests 与 compile 必须等相关 writers 稳定后按父级门执行，本 helper 未执行任何命令。

## 10. S3 之后仍保留的 parent 工作

S3 source pass 后，TURN-28 仍至少保留以下 parent-owned scope，不能由本报告提前 claim 或命名最终写集：

1. ordinary MEMORY/TOOLTIP/YELLOW/PURPLE candidate 的原子 left-click HTTPS cutover；
2. Ctrl 1/9/17 probe 的 exact-HWND pixel-change CAPTURE、finally release、raw after PNG 与后续 menu click；
3. 与真实 `NpcClickService` caller 同片的 typed in-memory PNG recognizer facade；禁止 JsonNode/Base64/session
   queue/poller，`ObjectiveTextRecognizer` 继续 reservation-only，除非真实 reuse 需要最小 diff；
4. parent 唯一完整 `NpcClickTurnContractTest`、source/test review、independent reviews、stable-writer Cloud/DHXY
   build gates与独立 fresh runtime evidence。

因此 post-Q/S 的**立即下一片只有 S3 right-click exit**；这不是剩余整卡的一次性重发。

## 11. 本 Helper 操作记录

- 唯一创建文件：
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-28-post-q-s-integration-readiness-helper.md`。
- Java、test、cards、`ACTIVE_WORK`、dashboard、计划、协议、业务逻辑、POM/config/resource 均未修改。
- Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/OCR/input 均未运行。
- Git status/branch/log/diff/stage/commit/checkout/reset/restore/clean 等 Git 命令均未调用；两仓既有 dirty/untracked
  内容保持原样。
- 本报告没有创建 owner、claim、review verdict、approval 或 card status。

TRUE_EOF PRECHECK_COMPLETE
