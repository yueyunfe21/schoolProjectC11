# TURN-34BP3 post-BP2 readiness preflight - Internal helper

> 角色：`CR271 Internal helper`，仅做 `TURN-34BP3` 的 post-BP2 readiness preflight。
>
> 结论：**`PRECHECK_ONLY / NOT_READY / NOT_APPROVAL`**。
>
> 本报告不是实现交付、不是 source review、不是 reviewer approval、不是 CR/card 状态变更，也不创建、领取或暗示领取 `TURN-34BP3`。本文中所有“应/必须”均是未来固定子卡的候选边界，须由父级在 BP2 最终交付后重新核对并正式冻结。

## 1. 本次只读边界

- 唯一写入：本报告。
- DHXY 工作区其余 dirty/untracked 文件全部保护；未改 Java、卡片、权威计划、`docs/ACTIVE_WORK.md`、dashboard 或既有报告。
- Cloud 工作区 `D:/mavenProject/dhxy-cloud-brain` 全程只读。
- 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture、input；未启动进程；未做 Git mutation。
- 未创建或 claim `TURN-34BP3`；当前 `TURN-34BP2` owner 不受本 helper 干预。
- 既有 `2026-07-16-turn-34bp3-readiness-preflight-helper.md` 只作为 10:47 历史预检材料读取，未修改；它早于 BP1 Parent Review #3 和 BP2 固定卡，不是本轮冻结输入。

## 2. 已完整读取的权威与当前证据

### 2.1 DHXY 文档

- `AGENTS.md`：392 行，SHA-256 `ad737d5652e7abdffbd626a8e617077d5475df49d5433cf249e92757bbdd2fc5`。
- `docs/DHXY_CONTEXT.md`：1,349 行，SHA-256 `8a7838763ce04b12a2c62e09624896827fdec6be5d07ac99b71357c644557621`。
- `docs/ACTIVE_WORK.md` 顶部当前 CR271 段；本轮只读，未把其中并发快照当成卡片真尾。
- 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节；第 14-19 节覆盖前文冲突。
- HTTPS turn 协议 `2026-07-15-https-turn-thin-client-protocol-design.md` 全文。
- `docs/业务逻辑.md` 全文，修罗/五倍的默认行为权威为 pre-cloud commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。
- `TURN-34BP1` 当前卡及物理真尾。
- 两份既有 BP2 readiness 报告：
  - `2026-07-16-turn-34bp2-readiness-preflight-helper.md`，SHA-256 `731df53f...`；
  - `2026-07-16-turn-34bp2-readiness-delta-helper.md`，SHA-256 `a06c62aa...`。
- 新出现的固定 `TURN-34BP2` 卡全文及当前物理尾。

### 2.2 Cloud 当前源码、测试与 caller

- `TaskMaintenanceService.java` 全文。
- `TaskExecutionContext.java`、`TaskExecutionContextHolder.java` 全文。
- `TaskExecutionContextTurnContractTest.java` 全文。
- `AutoCombatService.java`、`AutoCombatServiceTurnContractTest.java` 全文。
- `AutoBattleTask.java` 全文，以及 `WubeiTask`、`XiuluoTaskV2`、`AutoCombatService` 对 19 个 maintenance public API 的生产调用扫描。
- `TaskMaintenanceRequest`、`TaskMaintenanceResult`、`TaskMaintenanceStatus`、`TeamSupportCapability`、`TeamMaintenanceWindowState`。
- `696a12b0` 中 `TaskMaintenanceService` 的 public surface、maintenance 主序、Summon gate/claim/cache、capability 开关和收尾分支。
- 当前未发现 `TaskMaintenanceTurnContractTest.java`；本 helper 没有创建或运行它。

## 3. 并发快照与即时结论

证据窗口：`2026-07-16T11:36:00-04:00` 至 `2026-07-16T11:47:10-04:00`。这是只读瞬时快照，任何后续 append 或源码变化都必须重新核验，不能被本文自动吸收。

### 3.1 BP1

`TURN-34BP1` 当前物理真尾为 Parent Delivery Review #3：

- verdict：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`；
- production：527 行 / 22,204 bytes / SHA-256 `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e`；
- test：872 行 / 43,936 bytes / SHA-256 `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785`；
- owner 已释放；
- 卡片同时明确：两份独立 latest-round review 与 stable-writer named-test/Cloud compile gate 仍待完成，**不是 `CARD APPROVED`**。

因此，BP1 两个 SHA 已具有父级 source+test source receipt，可作为后继源码工作的候选只读输入；但本 helper 不把 BP1 整卡称为“已通过/已批准”。若 BP1 在 BP3 claim 前出现任何新 Repair/Delivery，以上 SHA 立即失效，必须改用新父级回执确认的字节。

### 3.2 BP2

固定 `TURN-34BP2` 卡已于 `2026-07-16T11:46:43.410-04:00` 被 External C claim。当前卡片只到领取与边界声明，尚无 canonical source delivery、无 parent source receipt、无 final production SHA。

Claim 时及 `11:47` 复核的 Cloud production 仍为：

- `TaskMaintenanceService.java`：1,224 行 / 66,012 bytes / SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`。

这只是 **BP2 starting SHA**，不是 BP2 final SHA，更不能作为 BP3 的 post-BP2 起点。BP2 正在占用 BP3 唯一生产写文件，所以当前结论只能是：

**`TURN-34BP3 PRECHECK_ONLY / NOT_READY / SAME-FILE BP2 OWNER ACTIVE`**。

### 3.3 其它只读保持点

- `AutoCombatService.java`：852 行 / SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`。
- `AutoCombatServiceTurnContractTest.java`：1,026 行 / SHA-256 `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292`。
- BP3 精确目标报告在本次写入前不存在；没有同名文件被覆盖。

### 3.4 Closing concurrent recheck（仍非 BP2 冻结输入）

本报告首次写入后的 `11:51` 只读复核捕捉到 BP2 owner 的并发 production 增量：

- `TaskMaintenanceService.java` 暂态变为 1,261 行 / 67,673 bytes / SHA-256 `c37a0186f3eb3290f24b77f6b14e5cf14a3b6ff654f8f7566c543b6b4147f597`；
- BP2 卡仍为 98 行 / SHA-256 `191b0c4b739a6a0e104b9894c63f5eaaa3f137f1539549c47470cdd6e151627a`，物理尾仍停在 claim 边界，无 source increment/canonical delivery/parent receipt；
- 暂态源码底部已可见 `ExecutionScope`、`ScopedTeamKey`、`TeamRoundKey`、`MaintenanceClaimKind`、`MaintenanceClaimKey`、`ScopedWindowKey`、`ScopedLocalSessionKey`，但字段区四个 shared maps 在该读取时仍是 String key，说明这是活跃编辑中的中间字节而非完整 BP2 结果；
- 四个 BP3 per-window maps 在该读取时仍保持 String key，符合 BP2 卡的分工，但这也不能替代 BP2 最终 delivery/review。

`c37a...` 仅记录并发事实，**不得**写入未来 BP3 `Frozen Inputs`。BP3 只能采用 BP2 canonical delivery 后由父级从磁盘重算并回执的 final SHA。即使 BP2 随后继续变化，本 PRECHECK 的 `NOT_READY` 结论不变。

## 4. BP3 最小启动依赖

下面条件必须全部满足，父级才可考虑创建固定 BP3 卡；本文不替父级作出该决定。

1. **BP2 owner 结束写入。** BP2 卡须出现 canonical delivery 或明确 `OWNER RETURNED`；只出现 source increment 不够。
2. **BP2 production 有父级 source receipt。** 父级须对 canonical delivery 的整份 `TaskMaintenanceService.java` 重读/复核，并在 BP2 卡物理真尾写明 source verdict、最终行数、bytes、SHA-256、实际 private key 类型/字段索引及未解决 P0/P1/P2。
3. **同文件互斥解除。** BP2 owner 已停止编辑，且没有 BP2 Repair writer、parent repair、其它 maintenance writer 占用该文件。
4. **BP3 起点字节等于 BP2 parent-received final SHA。** 父级创建 BP3 卡前和未来 worker claim 前都要重新计算；任一处不相等即停止，不得在“当前最新本地字节”上猜测叠加。
5. **实际读取 BP2 最终类型。** BP3 只能复用 BP2 真正交付的 private records/enums 和 equality 边界，不能从 readiness 报告推测类型名后另造别名、wrapper 或第二套 scope key。
6. **BP1 输入没有被后续 Repair 取代。** BP3 卡须引用 BP1 最新父级 source receipt 的 production/test SHA；若 BP1 后续变更，则由父级更新冻结表并重新检查 BP2/BP3 的兼容关系。
7. **固定 BP3 卡存在且唯一 owner claim。** 卡内必须包含冻结输入、精确写集、source acceptance matrix、禁止项和真实 claim 真尾。本文不是该卡。
8. **caller/AutoCombat 保持字节重新确认。** 若 `AutoCombatService`、四个 Task caller、maintenance model 或协议在 BP3 claim 前漂移，必须重新读调用契约；不能沿用本文快照直接开始。

BP1 的独立 review/build 是 CR 总批准链的一部分；当前 BP2 卡只明确授权 BP2 从 Parent Review #3 的 source-passed SHA 启动。该授权**不能自动外推**为 BP3 的 start gate。BP3 是否可在 BP1 后续独立 review/build 完成前启动，必须由父级在固定 BP3 卡中显式写明；helper 不推定。

## 5. 未来 BP3 的最小生产目标

BP3 只处理 **maintenance per-window state 的 exact execution scope + native generation 隔离和旧代清理**。它不是 BP1 的第二个 checkpoint 实现，不是 BP2 的 scope-key 重做，也不是业务策略重构。

### 5.1 必须建立的等价关系

未来实现应在 BP2 最终结构上满足：

`logical window = exact execution scope(tenantId, userId, deviceId) + windowId`

`native generation = exact native title + HWND/handle + processId`

- supplied `TaskExecutionContext` 优先于 holder；只有 supplied 为 null 才能看 holder。
- supplied context 的 authority/identity 错误不得 broad-catch 后降级成 no-context、bare-window 或 player-only key。
- supplied 为 null 且 holder 也为空时，才允许使用显式 typed no-context variant；不能用可碰撞 delimiter string。
- 如果 legacy/no-context 分支只能取得 player epoch，应使用明确的 typed variant，不能与 exact native fingerprint 值域混合。
- fingerprint 只用于本 service 内存状态隔离与失效，不获得 owner/session/ledger/transport/runtime/lifecycle 权力。

### 5.2 四个 per-window map

BP2 卡明确把以下四个 starting-source map 留给 BP3；BP3 应复用 BP2 的 `ScopedWindowKey`（或 BP2 最终同义真实类型）作为逻辑窗口 key，并保持 value/时序语义：

- `lastSummonSkillCleanAtByWindow`；
- `lastSummonSkillNotDueLogAtByWindow`；
- `summonSkillUnknownRetryAfterByWindow`；
- `summonSkillStateByWindow`。

不得保留 String 主查找加 typed fallback，不得双写、前缀扫、拆 delimiter、兼容 alias 或新全局 key。

### 5.3 legitimate successive-context `A -> B -> A`

BP1 解决的是**同一个 turn context 在 latest metadata 槽内**看到 `A -> B -> A'` 后仍保持 monotonic mismatch。BP3 是另一层问题：同一个 logical window 可能依次收到三个各自合法的新 `TaskExecutionContext`，native fingerprint 为 `A -> B -> A`。

BP3 必须做到：

1. 首次 A 建立当前 generation；
2. B 与当前 A 不同，原子失效 A 的本 service 旧代状态，再把 B 设为当前 generation；
3. 返回 A 时，A 与当前 B 不同，再原子失效 B 并建立**全新 A**；
4. 第三步不得恢复第一次 A 的 cooldown/cache/claim/formal-window/local-participation 状态；
5. 连续相同 fingerprint 不失效、不重置、不产生额外状态抖动。

这是单 logical-window 的“当前代”闩锁，不是历史 registry、TTL、lease、owner 或永久世代账本。

### 5.4 旧代清理的最小闭包

只清理“若保留就会使旧 native generation 继续影响新代”的 TaskMaintenance 私有状态：

1. 上述四个 per-window Summon cooldown/cache/state map 的 exact logical-window 条目；
2. BP2 最终 claim set 中由该 logical window 持有的 owner/participation；清理必须靠 typed equality，不能扫字符串前缀；
3. 与该 old binding 实际关联的 formal team maintenance participation/window evidence；不得清除其它 scope、其它 team、其它 window 的状态；
4. local-team session 中该 exact participant 的 generation evidence；若漂移者是 leader，须同时撤销由该旧 leader generation 打开的 capability evidence；普通 member 漂移不得误清仍有效 leader；
5. `completeLocalTeamSessionWindow` 现有“所有候选 terminal 后清 session/claim”的边界继续有效，但不得为此新增 Task terminal callback 或激活当前零 caller 的 lifecycle API。

若 BP2 最终数据结构无法在不新增业务 authority 的情况下表达第 2-4 项，父级必须先把具体设计冲突写入 BP3 卡并决策，不能由 worker自行扩大语义。

### 5.5 原子性与位置

- generation compare、旧代最小清理、current generation 替换必须在现有 service 的同一同步边界内完成，不能暴露半清状态。
- `runOpportunisticMaintenance` 的首个 `TaskCheckpoint.throwIfStopRequested(context)` 必须仍先于任何 generation registry mutation；初始 STOP/missing/device/window/native mismatch 维持零 maintenance 状态变更、零 delegate、零 action/UUID。
- 不新增 checkpoint、latest-metadata read、poll、sleep、retry 或 timer。BP3 读取 turn 的不可变 initial binding；不复制 BP1 的 monotonic latest-slot 逻辑。
- 优先在现有方法/私有 state 中直接收敛；不增加同层 wrapper/helper 链。

## 6. 互斥写集

### 6.1 本 helper 的实际写集

唯一是：

`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-34bp3-post-bp2-readiness-preflight-helper.md`

### 6.2 未来 BP3 建议精确写集

在父级固定卡批准后，implementation worker 最多写：

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`；
2. append-only 固定 `TURN-34BP3` 子卡。

严格只读：BP1 context/source test、holder/client/protocol/model/POM、AutoCombat、所有 Task caller、Dialog/Summon/TeamReturn/CommonBox 服务、全部 tests、DHXY Java、父卡、计划、`ACTIVE_WORK`、dashboard。

### 6.3 必须串行的 owner

- BP2 与 BP3 修改同一个 production file，必须完全串行；当前 BP2 已 claim，BP3 不可 claim。
- 任何 BP2 repair 与 BP3 同样互斥。
- 将来的 `TaskMaintenanceTurnContractTest` writer 即使写的是 test 文件，也必须基于 parent-received BP3 final production SHA；不能在 BP3 仍写 production 时把测试结果当成最终 contract 证据。
- AutoCombat/Task caller writer 与 BP3 虽可能物理文件不重叠，但若改变六个 caller-facing API 或 invocation order，构成语义冲突，须先由父级重切卡。

## 7. Public API 与 constructor 保持点

以下 19 个 public 方法的名称、参数、返回类型和异常/阻塞语义必须保持 byte-compatible：

1. `initializeForTaskStart`
2. `beginTeamMaintenanceRound`
3. `openTeamPathingMaintenanceWindow`
4. `openTeamFirstAidMaintenanceWindow`
5. `closeTeamMaintenanceWindow`
6. `openLocalTeamReturnSupportWindow`
7. `closeLocalTeamReturnSupportWindow`
8. `isTeamPathingMaintenanceWindowOpen`
9. `awaitTeamFirstAidMaintenanceWindowOpen`
10. `awaitLocalTeamSupportCapabilityOpen`
11. `isLocalSupportMemberSession`
12. `registerLocalTeamSessionCandidate`
13. `markLocalTeamWindowRoleDetected`
14. `isLocalSupportMemberCandidate`
15. `isPendingLocalSupportLeaderDetection`
16. `markLocalTeamLeaderDetected`
17. `isLocalTeamSupportCapabilityOpen`
18. `completeLocalTeamSessionWindow`
19. `runOpportunisticMaintenance`

五个 constructor collaborator 保持：`BotProperties`、`GameContext`、`DialogService`、`SummonSkillService`、`TaskExecutionContextHolder`。不得因 fingerprint 解析新增 service collaborator 或手工 singleton。

## 8. Caller 保持点

### 8.1 TURN-34A / AutoCombat-facing 六个 API

以下六个由 `AutoCombatService` 使用的 maintenance API 必须保持调用和返回语义：

- `awaitTeamFirstAidMaintenanceWindowOpen`
- `awaitLocalTeamSupportCapabilityOpen`
- `isLocalSupportMemberSession`
- `isLocalSupportMemberCandidate`
- `isPendingLocalSupportLeaderDetection`
- `isLocalTeamSupportCapabilityOpen`

`AutoCombatService` 已有 `LogicalStateKey(scope + windowId)` 与 `NativeFingerprint(title + handle + pid)` 的 `A -> B -> A` fresh-state precedent；BP3 可借鉴其语义，但不得调用其 private state、修改它、复制成跨 service authority 或改变其六个 caller-facing 调用。

### 8.2 Task callers

- `WubeiTask`：formal round/window、local return support 和 opportunistic maintenance 的现有调用点不改。
- `XiuluoTaskV2`：同上；不改修罗 phase、keep-turn/park、retry/fallback、verification 或 completion 语义。
- `AutoBattleTask`：start 初始化、leader pending、local capability query、maintenance/return/common-box 顺序不改。
- `AutoCombatService`：只读六个 API，不新增 maintenance lifecycle call。

生产扫描中以下四个 lifecycle API 当前无 service 外 caller：

- `registerLocalTeamSessionCandidate`
- `markLocalTeamWindowRoleDetected`
- `markLocalTeamLeaderDetected`
- `completeLocalTeamSessionWindow`

BP3 只可修正其内部 typed generation 数据；不得借 BP3 激活调用路径。

## 9. `696a12b0` 业务保持点

BP3 是 ownership/keying/失效 plumbing，不是行为 CR。必须保持：

- `normalize -> first checkpoint -> optional broadcast -> handled/failure/interrupted short-circuit -> at most one Summon delegate -> no-action` 顺序；
- CommonBox 优先级和既有 Dialog 解释；
- TeamReturn 只开放 capability，不产生第二套任务推进 authority；
- Summon feature/interval/FREE/due/UNKNOWN/static-tail gates、2 小时既有 tail cache、既有 UNKNOWN retry-after、claim acquire/release/retain、`GameContext.ActionState` 恢复；
- failure 且未发生 state change 时释放 claim；发生 state change 后 failure 保留 claim；
- capability open/close 数量与集合 `5/1/5/2`；
- formal round/window、local-team session、leader/member evidence 和 completion 的既有业务含义；
- 五倍/修罗的 prompt、OCR/template/click/navigation 顺序、phase、keep-turn/park、retry/fallback、verification count 和 expiry semantics 全部不变。

禁止新增或重置：TTL、额外验证/read、checkpoint、park/yield、retry、cleanup policy、fail-closed rule、cloud gate、sleep、timer、owner、lease、ledger、queue、durable workflow。这里“旧代精确失效”只能响应已确认的 native fingerprint transition，不得演化为周期清扫或业务过期策略。

预期业务差异声明：**`无已批准业务差异；按 696a12b0 等价迁移`**。

## 10. Terminal / UUID / zero-action 验收轴

以下是未来 source review/named-test 应覆盖的验收轴，不是本 helper 已运行的测试结论。

| 轴 | 输入/转移 | 必须观察到 | 禁止观察到 |
|---|---|---|---|
| T1 | 同 scope/window、同 fingerprint `A -> A` | 原状态连续；不失效 | delegate、command、action、UUID、额外 metadata read |
| T2 | 三个合法 context，`A -> B -> A` | 每次异代原子清旧代并建 fresh current；第三次 A 不恢复第一次 A | 历史 A cache/claim/formal/local evidence 复活；retry/TTL |
| T3 | 同 windowId、不同 tenant/user/device | 完全隔离 | 跨 scope cooldown/cache/claim/session 可见 |
| T4 | supplied context 与 holder 冲突 | supplied context 胜出 | broad-catch 降级、holder 覆盖 supplied、bare-window key |
| T5 | `runOpportunisticMaintenance` 首 checkpoint 得到 missing/device/window/native mismatch、STOP/paused terminal | typed checkpoint 结果/异常按现有路径一次传播；maintenance state 与 delegates 均为零变化 | Dialog/Summon、`execute`、action、UUID、generation mutation |
| T6 | broadcast 返回现有 handled/business-option、FAILED 或 INTERRUPTED 短路状态 | 立刻按现有 result 返回；不进入 Summon | 第二 delegate、第二 command、retry/replay/resend |
| T7 | Summon 不 eligible / not due / UNKNOWN backoff | 现有 no-action result 与 cache/日志节流语义 | Summon delegate、command、action、UUID |
| T8 | Summon eligible | 恰好一次 `cleanSummonSkillsOnce` delegate；claim/state 收尾保持 | TaskMaintenance 自行 mint UUID；第二次 delegate；自动重试 |
| T9 | delegate 完成或抛 terminal/uncertain | `COMPLETED`、`FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN` 按现有边界一次返回/传播；每次显式命令沿 HTTPS turn 协议只有一个 actionId UUID | terminal 后重放、UUID 复用、伪造 success、第二命令 |
| T10 | claim 竞争 | 同 team/round/maintenance 的现有限额与 duplicate 语义保持，scope/window typed 隔离 | delimiter collision、旧代 owner 阻塞新代、跨 scope 抢占 |
| T11 | old leader generation 漂移 | 只撤销该旧 leader 产生的 local capability evidence | 清除其它有效 leader/team/scope；member 漂移误清 leader |
| T12 | local session 全候选 terminal | 仅按现有 completion 条件清 exact session/claim/generation evidence | 新 Task terminal hook、激活零-caller API、额外 action/UUID |

计数口径必须分开：

- **TaskMaintenance 自身**不得创建 `TurnAction` 或 UUID。
- 纯 key/generation/query/cleanup 路径必须是零 Dialog、零 Summon、零 `TurnGameClient.execute`、零 action、零 UUID。
- 允许的 Dialog 或 Summon 分支中，delegate 可能按自身已批准 turn contract 发出一个命令；maintenance 只能调用既有 delegate 至多一次，不能把“delegate 的一个 action”错误报告成全路径零 action。
- actionId 是每次显式 command invocation 的新 UUID；不得跨重试/调用复用。由于本路径禁止自动 retry，terminal/uncertain 后没有第二个 actionId。

## 11. BP1/BP2 SHA 如何成为正式冻结输入

### 11.1 当前候选与缺口

| 上游 | 当前可见身份 | 当前证据强度 | 能否单独授权 BP3 |
|---|---|---|---|
| BP1 production | `a9c34d4e...` / 527 行 / 22,204 bytes | Parent Review #3 source passed | 否；仍须在 BP3 卡复制并确认未被后续 Repair 取代 |
| BP1 test | `3b117895...` / 872 行 / 43,936 bytes | Parent Review #3 test-source passed | 否；同上，且 BP1 整卡独立 review/build 仍待办 |
| BP2 production start | `963b028c...` / 1,224 行 / 66,012 bytes | 固定卡 starting identity，worker 已 claim | **绝对不能**；这是 pre-BP2 字节 |
| BP2 production final | 未知 | 尚无 canonical delivery / parent receipt | 否；这是当前硬 blocker |

### 11.2 正式冻结链

1. **BP1 parent receipt 层**：采用 BP1 卡物理真尾最新、且没有被后续 delivery 覆盖的 parent source+test receipt。当前候选为 `a9c34d4e...` / `3b117895...`；任何后续 Repair 都必须产生新 receipt。
2. **BP2 delivery 层**：BP2 worker 在同一卡 append canonical delivery，写明 starting SHA、final SHA、lines、bytes、实际四个 typed shared-map 字段和 private type 索引、精确写集及业务差异。
3. **BP2 parent receipt 层**：父级从磁盘重算 BP2 final `TaskMaintenanceService.java`，把 exact final SHA/lines/bytes、source verdict 和未解决 P0/P1/P2 写到 BP2 卡的新物理真尾。Worker 自报、helper 报告或聊天 SHA 均不能替代这一层。
4. **BP3 fixed-card 层**：只有第 3 步完成后，父级才创建固定 BP3 卡。卡内 `Frozen Inputs` 至少逐字复制：
   - BP1 parent-received production SHA、test SHA、lines、bytes、receipt marker；
   - BP2 starting SHA 与 parent-received final production SHA、lines、bytes、receipt marker；
   - BP2 最终实际 typed key/type 名及四个已迁移 shared map；
   - 当前只读 AutoCombat production/test SHA 和 caller/API scan 日期。
5. **BP3 claim 层**：未来 worker 在 append claim **之前**重新计算 Cloud 文件。`TaskMaintenanceService.java` 必须精确等于 BP2 parent-received final SHA，BP1 两文件必须精确等于 BP3 卡冻结值；否则 `NO CLAIM / RETURN TO PARENT`。
6. **BP3 delivery/review 层**：BP3 最终也须有 canonical final SHA 和 parent source receipt。后续 named test writer 只能以这个 parent-received BP3 SHA 为 production target，不能以本 PRECHECK 或 BP2 start SHA 为准。

“正式冻结”是 **父级 receipt + 固定子卡 + worker claim 前重算** 三者同时成立，不是报告中出现了一个哈希，也不是文件当时恰好未变化。

## 12. 未来固定 BP3 卡应写死的验收索引

固定卡至少应明确：

- BP2 final private key/type 的真实名称和结构，不接受 readiness 报告里的推测别名；
- 四个 per-window map 的 before/after key 类型；
- current-generation registry 的 logical key 与 native fingerprint 字段；
- `A -> B -> A` 的 fresh-state、zero-delegate、zero-action/UUID 断言；
- old-generation 清理包含/不包含的 exact 私有字段；
- leader drift 与 member drift 的不同清理结果；
- supplied-context precedence、authority failure 不降级、typed no-context 边界；
- 19 public signatures、五 constructor collaborators、六 AutoCombat-facing APIs、四个零生产-caller lifecycle APIs；
- `696a12b0` 顺序、`5/1/5/2`、CommonBox、TeamReturn、Summon/claim/UNKNOWN/static-tail 行为；
- 零新增 metadata read/checkpoint/delegate/command/action/UUID/retry/sleep/timer/TTL/authority；
- exact two-item implementation write set和 owner 互斥；
- named test/compile 属于后续 stable-writer gate，implementation worker 不自跑、不自批。

## 13. 当前 blocker 与可解除证据

| Blocker | 当前证据 | 唯一可接受的解除证据 |
|---|---|---|
| BP2 同文件 owner active | BP2 11:46 claim；无 delivery | BP2 卡 canonical delivery/owner return，且无继续写入 owner |
| BP2 final SHA 未知 | production 仍是 start `963b...` | BP2 parent receipt 的 final SHA/lines/bytes |
| BP3 固定卡不存在 | 本 helper 仅是报告 | 父级在 BP2 receipt 后创建的固定 BP3 卡 |
| BP3 write set 未正式授予 | 只有 preflight 建议 | 固定卡明确 exact two-item write set 和 claim gate |
| BP1 整卡非 Approved | Parent Review #3 明示 independent review/build pending | 按权威流程后续 review/build；是否阻塞 BP3 source start由父级卡显式决定 |
| maintenance named test 不存在 | 当前 Cloud 路径无该测试 | BP3 parent-received source 后由授权 stable writer 创建并按流程运行；本 helper 不执行 |

## 14. 父级 post-BP2 决策清单

父级在 BP2 结束后应逐项回答，任何一项为否都不能标 `READY`：

- [ ] BP2 卡最新物理真尾是 canonical delivery 后的 parent source receipt，而不是 claim/source increment。
- [ ] 当前 `TaskMaintenanceService.java` SHA 与该 receipt 完全一致。
- [ ] BP2 实际只迁移四个 shared maps，没有提前修改 BP3 四个 per-window maps/fingerprint/generation/cache。
- [ ] BP2 最终 public 19 methods、constructor 五依赖、六 AutoCombat-facing APIs 和 `696a12b0` 行为通过 source review。
- [ ] 没有 BP2/repair/其它 maintenance writer 持有同文件。
- [ ] BP1 最新 parent-received production/test SHA 未被新 Repair 覆盖。
- [ ] 固定 BP3 卡复制了 BP1/BP2 receipt 的 exact SHA/lines/bytes，而不是引用“latest”。
- [ ] 固定卡只授予 `TaskMaintenanceService.java + append card`，tests/callers/context/AutoCombat 只读。
- [ ] 固定卡明确 T1-T12、`A -> B -> A` 不复活、terminal/UUID/zero-action 计数口径。
- [ ] 固定卡写明 `无已批准业务差异；按 696a12b0 等价迁移`。

## 15. PRECHECK 收口

当前不能创建/claim/实现/批准 BP3。最小安全下一事件是：等待现有 BP2 owner 产生 canonical delivery，再等待父级把 BP2 final `TaskMaintenanceService.java` SHA 写成物理真尾 source receipt。届时父级必须基于实际 BP2 类型和最终字节重新执行第 14 节清单；本报告不会自动从 `PRECHECK` 升级为 `READY`。

本 helper 未改任何生产/测试/卡片/计划/ACTIVE_WORK/dashboard，未运行构建或运行态，未触碰 Git 状态。

TRUE_EOF PRECHECK_COMPLETE
