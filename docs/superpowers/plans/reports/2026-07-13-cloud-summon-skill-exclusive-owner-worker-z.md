# SummonSkill Whole-Pass Exclusive Owner Integration - Internal Worker Z

## Parent Task Brief #1 - `W-SS-X1-D1` - 2026-07-13T12:53:00-04:00

### 角色与唯一写集

- 你是 Internal Worker Z，只做设计/后续实现，不是 reviewer；父级是唯一 reviewer。
- 立即先在本日志真实末尾追加 `CLAIMED`，写明 task、claimedAt、唯一写集。本轮唯一写集仅本 append-only 日志；
  DHXY/Cloud Java、Maven、schema、resources、tests、A/B/Y 写集、host/caller 全冻结。
- 你不是独自在工作区。保护两仓全部 dirty/untracked 和他人并行改动；不回滚、不覆盖、不清理、不提交 Git。

### 开工必读与权威

完整读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\ACTIVE_WORK.md` 顶部 CR271、
`docs\superpowers\specs\2026-07-12-service-migration-matrix.md`、`docs\业务逻辑.md`，以及：

1. `docs\superpowers\plans\reports\2026-07-13-cloud-summon-skill-service-worker-n.md` 中已批准的 R-X0
   `CloudTaskExclusiveInteractionState` 结论；
2. Cloud 当前真实 `CloudTaskExclusiveInteractionState`、`CloudTaskRunAuthorityAssembly`、`CloudTaskServicePort`、
   `CloudTaskServiceExecutionContext`、`CloudTaskRunCurrentContextSlot` 与 retained action/receipt API；
3. DHXY committed HEAD `0114604e` 的 `SummonSkillService.java`、全部 production caller、`InputActionQueue`/
   `InputSequences` 与 stop/pause checkpoint 路径。

业务行为只以 HEAD `0114604e` 和 `docs/业务逻辑.md` 为权威；当前 dirty 业务差异只读，不得迁入。

### 目标

只完成 R-X1：为一次 `SummonSkillService` whole-pass exclusive interaction 设计真实的 owner/capability/assembly
集成，使 Cloud 业务可持有稳定的 whole-pass session/action identity，而 DHXY 永久保留 exact-window capture、模板/OCR、
物理 input queue 与 already-exclusive callback 内的直接 InputProvider 执行。不得暴露 raw handle、raw request/poll/outcome
或自由 action list，不得让 Cloud 持有 HWND/模板路径，也不得出现 queue-in-queue 自等待。

### Design #1 必交付

1. 给 R-X0 state 到 assembly-owned closed capability 的精确调用链，列 constructor/visibility/owner；业务 Service 不能自行
   `new`/mint session，稳定 session/action identity 只能由 retained owner 铸造并跨 runRevision 复用。
2. 给 same taskRun 的 ACTIVE -> PAUSED -> RESUMED/current-revision 交接：旧 request revision fence 必须拒绝，same semantic
   interaction 不得重铸 identity；说明 current context/port/state 如何原子取得同一 generation。
3. 给 whole-pass acquire/execute/terminal release 的状态表，覆盖 duplicate、late、`NOT_EXECUTED`、`UNKNOWN`、`STOPPED`、
   pause、stop、disconnect 与 terminal cleanup；UNKNOWN 不得被压成成功/普通 false，不新增自动 retry/TTL/takeover。
4. 明确本地 exclusive queue 边界：Cloud 只发一个 typed whole-pass intent；DHXY 在队列 owner 内完成 HEAD 原顺序，exclusive
   callback 内不得再次 `submitAndWait`。不得改点击、技能选择、识别、fallback、sleep、retry、stop 语义。
5. 给 tenant/global/per-run hard cap、原子 admission/removal、restart 无 durable restore 与运维诊断 owner；不借用无关 cap。
6. 给可直接编码的 exact New/Modify 文件及方法表，必须引用当前源码真实类型；只提出一个最小实施波次。若存在硬缺口，
   只列一个最小 blocker、证据和 owner，不制造占位 wrapper 或第二权威。
7. 明确与外部 A Navigation config、外部 B TeamReturn、内部 Y PlayerState 的文件写集零交叉。

### 禁止与交付门

- 本轮只写 Design #1，不改 Java，不运行 Maven/tests/application/server/host/Task/poller/UI/capture/input。
- 不新增/恢复 DHXY tests，不执行生产切换、凭据、外部付费、不可逆删除或 Git mutation。
- 结尾列 self-QA P0/P1/P2；Worker 自审不算 Approved，父级将独立给出 `DESIGN APPROVED` 或 `BLOCKED`。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Task - W-SS-X1-IMP1 - 2026-07-13T13:59:00-04:00（EOF 权威块）

Internal Worker Z 的 R1-R5 + D3 已父级 DESIGN APPROVED。立即在本日志真实 EOF 追加 `CLAIMED` 后，按 R4.1-R4.4 的
**完整唯一文件表**实施一个 dormant 双仓原子波；该表列出的 Cloud New/Modify、DHXY New/Modify 与本日志是你的唯一写集，
未列文件全部冻结。尤其必须一次闭合：

- Cloud retained whole-pass authority/capability、closed request/outcome/operation、stable semantic action identity、Full R0
  duplicate/late/final-consume，interaction projection 在 H 前预构造并随 K runtime 单点 publication；
- DHXY exact payload/codec/digest/ledger、registry-entry non-mintable continuation、ACTIVE->PAUSED->ACTIVE 双向 snapshot、
  STOPPING/terminal/replacement invalidation、detailed exclusive callback 与 per-checkpoint side-effect fence；
- Parent Review #3 绑定条件：`closeInFlightExclusive` 对同 owner 的 Queued/Active/Paused/Invalidated/Closed total + idempotent，
  terminal 已清 slot/entry 时不得在 finally 抛错覆盖 typed outcome；foreign handle 仍 fail closed；
- R1 的 deterministic post-pass UICleaner matrix：只在已证明 no-owner 的 terminal 结果执行一次，UNKNOWN/可能 owner 不清理；
  host/caller 保持 dormant，不启用 Task，不改 HEAD `SummonSkillService` / `UICleanerService` 业务实现。

你不是唯一在代码库工作的 Worker：所有 R4 已存在 dirty/untracked 都是受保护在途基线，须在其上增量实现，禁止回滚、覆盖、
格式化无关文件或 Git mutation。不得新增/恢复 tests，不启动 application/server/host/Task/poller/UI/capture/input。完成后运行
Cloud `mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，把精确文件、wire/digest 对称性、状态时序、
构建结果和 self-QA 追加回本日志；Worker self-QA 不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #3 - DESIGN APPROVED - 2026-07-13T13:54:00-04:00

父级对照 `RemoteTaskRunRegistry.publishTransition`、`beginStop`、`advanceStoppingProgress`、`releaseTerminal`、
`unregister`、stable `TaskPauseToken` 与 `InputActionScope` 当前调用链复审 D3。唯一 P1 已闭合：同一 non-mintable handle
现在具有可达的 `QueuedActive -> CallbackActive -> CallbackPaused -> CallbackActive` 双向 publication；ACTIVE->PAUSED
先在 `mutationLock` 内共同发布 registration/paused-or-invalidated snapshot，再把 `requestPause` 放最后；PAUSED->ACTIVE
先预构造 successor/readiness/generation 并共同发布，再把 `resume` 放最后。STOPPING、terminal、entry removal/replacement
均主动 invalidate 并清 slot，下一 direct side effect 必须经 detailed safety supplier 在同一 registry lock 下验证 exact
entry/generation/registration/token/snapshot；original command revision 始终不变，不能因 resume 复活。

结论：**DESIGN APPROVED，P0/P1/P2=0**。绑定实施条件：`closeInFlightExclusive(handle)` 必须对同 owner 的
Queued/Active/Paused/Invalidated/Closed 五种状态均 total + idempotent；slot 仍指向该 handle 时精确清空，slot 已由
terminal/replacement 清空或 entry 已移除时只完成/保留 handle 的 closed terminal marker，不得抛异常覆盖 handler finally
中的 typed outcome，也不得清理另一个 handle。foreign owner/entry handle 仍 fail closed。该条件不增加文件或新状态权威，
无需再交文字返修。

R5 仍须作为一个 dormant 双仓原子实施波，由父级确认 shared protocol/input/registry 写集释放后另行发单；落码前 Java、
schema、host/caller 继续冻结。Worker self-QA 不计批准，本条为唯一父级结论。

**无已批准业务差异；按基线等价迁移。**

## CLAIMED

- task: `W-SS-X1-IMP1`
- claimedAt: `2026-07-13T14:00:11-04:00`
- uniqueWriteSet: 本日志 + Parent-approved `R4.1-R4.4` 完整唯一文件表；未列文件全部冻结

## IMPLEMENTATION BLOCKED - R4 complete-file-table compile gap - 2026-07-13T14:24:07-04:00

- task: `W-SS-X1-IMP1`
- blocking severity: `P1`（唯一文件表与强制 `clean package` 不能同时满足）
- exact evidence: R4.2 要求在 `RemoteOperation.java` 新增 enum constant
  `SUMMON_SKILL_WHOLE_PASS`；但未列入 R4.2、因而明确冻结的
  `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
  在 `toTypedOutcome()` 使用无 `default` 的穷尽 enum switch expression（当前约第 108-114 行），只覆盖现有五个
  operation。对同一 Maven main source set 新增 enum constant 后，Java 编译期必然要求该 switch 覆盖新 constant；
  `RemoteGameCommandBroker` 内截获新 operation 只能改变运行路径，不能消除该冻结源码的编译期穷尽性错误。
- required parent decision: 将上述 `RemoteCommandOutcomeEnvelope.java` 精确加入 R4.2 唯一写集，以只增加
  `SUMMON_SKILL_WHOLE_PASS` closed payload decode 分支；或发布不新增 enum constant 的替代协议设计。未获明确扩权前
  Worker Z 不越界修改。
- protected state: 未运行 Maven/runtime/Git；未修改任何 Cloud Java。已在批准的 DHXY R4.3/R4.4 文件内增量落下
  payload/operation/codec/digest/ledger、registry continuation、detailed callback 与 handler 的在途实现；未触碰
  host/caller、tests、schema/resources 或任何未列文件。该在途波尚未构建、尚未交付，不能视为完成或 Approved。
- self-review: 本阻塞判断不是 reviewer approval；等待父级修订 EOF 权威实施块/唯一文件表后继续同一 atomic wave。

## Parent Scope Repair #1 / Resume - W-SS-X1-IMP1-R1 - 2026-07-13T14:30:30-04:00（真实 EOF 权威块）

父级复核 Worker Z 的阻塞判断成立。`RemoteOperation` 新增第六个 enum 后，
`RemoteCommandOutcomeEnvelope.toTypedOutcome()` 的无 `default` switch 必须同步闭合；这不是可拆开的后续优化，而是同一
closed protocol 原子波的遗漏文件。现对 R4.2 唯一文件表做**唯一一项扩权**：

- 新增 Modify：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteCommandOutcomeEnvelope.java`。
- 精确修改面：在 `toTypedOutcome()` 为 `SUMMON_SKILL_WHOLE_PASS` 增加穷尽分支，并按本波新建的
  `SummonSkillWholePassOutcome` closed shape 严格解码 payload；必须拒绝额外/缺失字段及不符合 mechanical-status、
  cleanup、callback-started、owner-released null/证明矩阵的 payload，然后照既有路径执行 outcome digest verify。
- 禁止面：不加 `default`，不把 payload/raw envelope 暴露给 business，不放宽其它五个 operation，不新增第二 codec/
  authority，不改 Maven/schema/tests/host/caller，不改变已批准 R1-R5 与 D3 状态机。

除上述一文件外，原 `W-SS-X1-IMP1` 唯一写集、构建门与冻结面全部不变。Worker Z 立即继续同一个原子实施波，完成后追加
`Implementation Repair #1`，列出两仓精确文件、wire/digest 对称性、continuation publication 与双构建结果。当前结论仍是
`BLOCKED`，直到完整源码和 Cloud `mvn -q clean package`（不可 skip）、DHXY `mvn -q -DskipTests compile` 均经父级复验。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Implementation Review #1A - BLOCKED / Repair #2 Reissued - 2026-07-13T15:39:00-04:00（真实物理 EOF 唯一权威块）

说明：同一返修结论先前因宽锚点误插入历史第 229 行。为保护 append-only，不删除、不改写该历史副本；**只有本物理
EOF 副本是当前权威返修指令**。父级复核 Worker Z 的双仓交付后结论仍为 **BLOCKED，P0=0/P1=2/P2=0**：

1. **P1：合法 `cleanupResult.message=null` 的两端 outcome digest 不一致。** DHXY
   `RemoteSummonSkillWholePassOutcomePayload.CleanupValue` 把 null/blank 规范为 `null`，closed payload 又要求九个 cleanup key，
   nested null 会进入本地 digest；Cloud `SummonSkillWholePassOutcome.CleanupValue` 经 `optionalText` 规范为 `""`，Cloud digest
   因而写入空串。同一合法 outcome 会在 Cloud digest verify 被拒绝。返修只允许把 DHXY transport cleanup message 规范为
   与 Cloud 相同的 canonical empty string；不得改变 `SummonSkillCleanupResult` 业务语义或放宽九字段 closed shape。
2. **P1：ACTIVE 先得到 UNKNOWN、随后 pause/resume 时 interaction state 未续代。**
   `CloudTaskExclusiveInteractionAuthority.parkPaused` 对 `UNRESOLVED_FENCE_HELD` 直接返回；若 state 尚未 parked，
   `prepareResumeGeneration` 不会 handoff revision/generation，却把旧 state 发布进 next ACTIVE projection。返修必须在 pause
   线性化时执行 restore -> PARKED_PAUSED -> re-hold unresolved，resume 再走既有 handoff，使 revision/generation 精确推进；
   原 retained request/action bytes 必须保持完全相同，禁止 requeue、redispatch、renew、TTL、takeover 或把 UNKNOWN 压成 terminal。

### 当前任务 `W-SS-X1-IMP1-R2`

原 Internal Worker Z 继续返修，唯一代码写集收窄为：

- Cloud Modify：`CloudTaskExclusiveInteractionAuthority.java`；
- DHXY Modify：`RemoteSummonSkillWholePassOutcomePayload.java`；
- 本 append-only 日志。

其余 R4 文件、业务 `SummonSkillService`/`UICleanerService`、schema/resources/tests/host/caller 全冻结。完成后运行 Cloud
`mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，并在本日志真实 EOF 追加
`Implementation Repair #2`：精确 diff、null/blank/nonblank 三类 message 规范化证据、UNKNOWN-before-pause 时序、双构建结果。
Worker self-QA 不构成父级批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Implementation Repair #1 - W-SS-X1-IMP1-R1 - 2026-07-13T15:17:23-04:00

- status: `IMPLEMENTED / READY FOR PARENT REVIEW`
- role: Internal Worker Z（只做 implementation / self-QA，不是 reviewer）
- authority: 仅执行真实 EOF `Parent Scope Repair #1 / Resume - W-SS-X1-IMP1-R1 - 2026-07-13T14:32:00-04:00`；原 R4.1-R4.4 唯一写集只增加父级扩权的 `RemoteCommandOutcomeEnvelope.java`
- dormant boundary: 未新增 tests，未启动 application/server/host/Task/poller/UI/capture/input，未修改 schema/resources/Maven model/host/caller/A/B/Y，未做 Git mutation

### 精确实施文件

Cloud New：

- `CloudTaskExclusiveInteractionAuthority.java`
- `CloudSummonSkillWholePassCapability.java`
- `SummonSkillWholePassRequest.java`
- `SummonSkillWholePassOutcome.java`

Cloud Modify：

- `CloudTaskExclusiveInteractionState.java`
- `CloudTaskRunAuthorityAssembly.java`
- `CloudTaskRunCurrentContextSlot.java`
- `CloudTaskServiceExecutionContext.java`
- `CloudTaskServicePort.java`
- `CloudTaskRunRetainedLifecycleActivationAdapter.java`
- `CloudTaskRetainedActionState.java`
- `CloudTaskRunActionLedger.java`
- `RemoteOperation.java`
- `RemoteRequest.java`
- `RemoteOutcome.java`
- `RemoteGameClientPort.java`
- `CloudTaskRunCommandExecutor.java`
- `RemoteGameCommandBroker.java`
- `RemoteFinalConsumptionCoordinator.java`
- `RemoteProtocolDigests.java`
- `RemoteCommandOutcomeEnvelope.java`（Scope Repair #1 唯一扩权）

DHXY New：

- `RemoteSummonSkillWholePassCommandPayload.java`
- `RemoteSummonSkillWholePassOutcomePayload.java`

DHXY Modify：

- `RemoteGameOperation.java`
- `RemoteOperationPayloadCodec.java`
- `RemoteProtocolDigests.java`
- `RemoteOperationLedger.java`
- `RemoteTaskRunRegistry.java`
- `LocalRemoteGameCommandHandler.java`
- `InputActionQueue.java`
- `InputActionRequest.java`
- `InputActionScope.java`

除上述文件与本固定日志外无写入。

### Wire / digest / final-consumption parity

- 两端 request 均为 closed 六字段：`expectedSkillCount`、`trustExpectedSkillCount`、`startSlotIndex`、`skipUltimateCornerCheck`、`exclusiveSessionId`、`bindingGeneration`；nullable/counter/canonical-text 条件对称。
- 两端 outcome 均为 closed 五字段：`mechanicalStatus`、`cleanupResult`、`callbackStarted`、`ownerNeverAcquired`、`ownerReleased`；cleanup 为 exact 九字段，`EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` 与 common execution state、owner proof/null matrix 对称。
- Cloud `RemoteCommandOutcomeEnvelope.toTypedOutcome()` 的 enum switch 无 `default`，新增 whole-pass 分支先拒绝额外/缺失 payload 与 cleanup 字段，再构造 typed outcome，最后走既有 outcome digest verify；其它五个 operation 未放宽。
- Cloud/DHXY digest 都从 typed closed shape 构造、忽略协议允许的 null optional request 字段并做同一 canonical JSON SHA-256；semantic address/action identity、duplicate、late resolution 与 final-consumed receipt 保持同一 retained request/outcome digest。
- `UNKNOWN` 保留 live owner/action/ledger，不消费 final、不续 TTL、不释放后重发；同 identity 再读只等待 broker retained late resolution，`awaitRetainedResolution(...)` 不 requeue/redispatch，因此没有第二次 physical whole-pass。

### Authority / continuation / cleanup 时序

- Cloud owner 以 stable run key + session/binding fence 保留唯一 whole-pass；hard caps 为 `1/run`、`64/tenant`、`1000/global`。initial/resume 的 interaction projection、successor runtime 与 K state 全在 H 前预构造；H 成功后只执行 `nextRuntime.stateActivationHandle = nextStateHandle` 与唯一 K `publication.set(preparedNextState)`，没有可失败 interaction commit。
- DHXY `RemoteTaskRunRegistry` 在同一 `mutationLock` 内对同一 non-mintable handle 发布 `QueuedActive -> CallbackActive -> CallbackPaused -> CallbackActive`；pause/resume successor 的 registration、readiness、snapshot 全先构造，赋值完成后才最后调用稳定 `TaskPauseToken.requestPause(...)` / `resume()`。
- same in-flight callback resume 后的 per-checkpoint fence 读取 successor ACTIVE registration/revision；旧 original command 只用于首次 worker admission，不能再次 admission，故旧 command 永久 stale。STOPPING、terminal、replacement 先发布 `InvalidatedSnapshot` 并 detach，再 resume token；`closeInFlightExclusive(...)` 对 same owner 的 queued/active/paused/invalidated/closed total + idempotent，foreign handle fail closed。
- `LocalRemoteGameCommandHandler` 只在 detailed exclusive callback 内调用 HEAD `SummonSkillService.cleanSummonSkillsOnce(...)`；direct callback 每个后续物理 side effect 前由 `InputActionScope.checkpoint()` 执行 handle fence。callback/owner 已确定终止后，才在 input queue 外按 HEAD 等价 matrix 至多一次调用现有 `UICleanerService.cleanLightweightInterruptions(...)`；`UNKNOWN` 或 owner 仍可能存活时不清理。

### 双构建证据

- Cloud：在确认 AA 的 Cloud Maven 进程已退出后，从 `D:\mavenProject\dhxy-cloud-brain` 独立执行 `mvn -q clean package`，`exit 0`，耗时 `72.0s`；未与 AA 的 clean 并发。
- DHXY：从 `D:\mavenProject\DHXY` 执行 `mvn -q -DskipTests compile`，`exit 0`，耗时 `3.1s`。
- 两条命令均为本交付前的 fresh full gate；未运行 tests/runtime。

### Self-QA（不构成 Approved）

- cross-repo closed wire/digest、R0 duplicate/late/final-consume、R1 post-pass matrix、R3 H/K publication、R4 continuation/invalidation 已做方法级 parity 自审。
- P0: `0`
- P1: `0`
- P2: `0`
- Worker Z 自审不构成父级 `Approved`；现停止修改，等待父级复审。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Implementation Review #1 - BLOCKED / Repair #2 Published - 2026-07-13T15:35:00-04:00（真实 EOF 权威块）

父级逐方法复核双仓 closed wire/digest、local detailed exclusive owner、Cloud retained late-resolution 与 H/K
pause-resume publication。worker 双构建证据有效，owner admission/checkpoint/terminal close 与不重发 retained request 的方向成立；
当前仍 **BLOCKED，P0=0/P1=2/P2=0**：

1. **P1：合法 `cleanupResult.message=null` 的两端 outcome digest 不同。** DHXY
   `RemoteSummonSkillWholePassOutcomePayload.CleanupValue:130` 把 null/blank 规范为 `null`；payload codec 又要求九个 cleanup key，
   因而本地 payload 会携带显式 `"message":null`。DHXY `RemoteProtocolDigests.mergeNonNullFields:221-229` 只剔除
   top-level null，会把 nested null 原样纳入摘要。Cloud `SummonSkillWholePassOutcome.CleanupValue:102` 经
   `RemoteProtocolValidation.optionalText` 把 null 变为 `""`，Cloud NON_NULL mapper 摘要因此包含空串。同一合法 wire outcome
   会在 `RemoteCommandOutcomeEnvelope.toTypedOutcome` digest verify 被拒绝。**返修条件：**不放宽九字段 closed shape；只把
   DHXY transport cleanup message 规范为与 Cloud 相同的 canonical empty string（或给出等价单一规范），保证 null/blank/nonblank
   三类输入的 wire tree 与 digest 完全一致；不改 `SummonSkillCleanupResult` 业务语义。
2. **P1：ACTIVE 状态先 UNKNOWN、后 pause/resume 时 interaction state 不续代。**
   `CloudTaskExclusiveInteractionAuthority.parkPaused:78-83` 对 `UNRESOLVED_FENCE_HELD` 直接 return；若 UNKNOWN 在 ACTIVE/
   ACQUIRE_BOUND 时形成，该 state 的 `parkedFrom==null`。随后 `prepareResumeGeneration:110-142` 的
   `retainUnresolvedFence` 为 false，既不 park/handoff，也不推进 `currentRunRevision/bindingGeneration`，却把旧 state 放进
   next ACTIVE projection。**返修条件：**在 pause 线性化时对 unresolved-but-not-yet-parked state 执行
   restore -> PARKED_PAUSED -> re-hold unresolved，resume 再走既有 handoff，使 state revision/generation 精确推进；必须保留
   原 retained request/action bytes，不能 requeue、redispatch、renew 或把 UNKNOWN 压成 terminal。

### 当前任务 `W-SS-X1-IMP1-R2`

原 Internal Worker Z 继续返修，唯一代码写集收窄为：

- Cloud Modify：`CloudTaskExclusiveInteractionAuthority.java`；
- DHXY Modify：`RemoteSummonSkillWholePassOutcomePayload.java`；
- 本 append-only 日志。

其余 R4 文件、业务 `SummonSkillService`/`UICleanerService`、schema/resources/tests/host/caller 全冻结。不得新增 tests、TTL、
takeover、自动 retry、第二 owner/map；不得启动 application/server/host/Task/poller/UI/capture/input。完成后运行 Cloud
`mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，追加精确 diff、三类 message 规范化静态证据、
UNKNOWN-before-pause 时序与双构建结果。Worker self-QA 不构成父级批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Implementation Task - W-SS-X1-IMP1 - 2026-07-13T14:00:00-04:00（EOF 权威块）

说明：同标题任务曾因 append 锚点过宽误插入旧历史位置；**仅本真实 EOF 块是当前任务权威**。Internal Worker Z 的
R1-R5 + D3 已父级 DESIGN APPROVED。立即在本日志真实 EOF 追加 `CLAIMED` 后，按 R4.1-R4.4 的**完整唯一文件表**实施
一个 dormant 双仓原子波；该表列出的 Cloud New/Modify、DHXY New/Modify 与本日志是你的唯一写集，未列文件全部冻结。
尤其必须一次闭合：

- Cloud retained whole-pass authority/capability、closed request/outcome/operation、stable semantic action identity、Full R0
  duplicate/late/final-consume，interaction projection 在 H 前预构造并随 K runtime 单点 publication；
- DHXY exact payload/codec/digest/ledger、registry-entry non-mintable continuation、ACTIVE->PAUSED->ACTIVE 双向 snapshot、
  STOPPING/terminal/replacement invalidation、detailed exclusive callback 与 per-checkpoint side-effect fence；
- Parent Review #3 绑定条件：`closeInFlightExclusive` 对同 owner 的 Queued/Active/Paused/Invalidated/Closed total + idempotent，
  terminal 已清 slot/entry 时不得在 finally 抛错覆盖 typed outcome；foreign handle 仍 fail closed；
- R1 deterministic post-pass UICleaner matrix：只在已证明 no-owner 的 terminal 结果执行一次，UNKNOWN/可能 owner 不清理；
  host/caller 保持 dormant，不启用 Task，不改 HEAD `SummonSkillService` / `UICleanerService` 业务实现。

你不是唯一在代码库工作的 Worker：所有 R4 已存在 dirty/untracked 都是受保护在途基线，须在其上增量实现，禁止回滚、覆盖、
格式化无关文件或 Git mutation。不得新增/恢复 tests，不启动 application/server/host/Task/poller/UI/capture/input。完成后运行
Cloud `mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，把精确文件、wire/digest 对称性、状态时序、
构建结果和 self-QA 追加回本日志；Worker self-QA 不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

## CLAIMED

- task: `W-SS-X1-D1 Design #1`
- claimedAt: `2026-07-13T12:54:19-04:00`
- uniqueWriteSet: `docs/superpowers/plans/reports/2026-07-13-cloud-summon-skill-exclusive-owner-worker-z.md`（仅该 append-only 日志）

## W-SS-X1-D1 Design #1

- designedAt: `2026-07-13T13:17:38-04:00`
- status: `DESIGN READY FOR PARENT REVIEW`
- role: Internal Worker Z（design / 后续 implementation；不是 reviewer）
- scope: 仅 R-X1；本段没有 Java、Maven/tests、运行面或 Git mutation

### 1. 权威基线与不可变结论

本设计只采用以下行为权威：

1. DHXY committed HEAD `0114604e` 的 `SummonSkillService.cleanSummonSkillsOnce(...)`、唯一 production caller
   `TaskMaintenanceService`、`InputSequences` / `InputActionQueue` 与 `TaskCheckpoint` / `TaskPauseToken` 路径；
2. `docs/业务逻辑.md` 的召唤兽技能槽位、识别、删除、终极技能、UNKNOWN fail-closed 与点击顺序基线；
3. Worker N 已由父级批准的 R-X0 `CloudTaskExclusiveInteractionState` 叶子，以及 Cloud 当前 retained
   action / receipt / current-context K/H owner。

明确保持：

- `SummonSkillCleanupRequest` 的四个输入字段原义不变：`expectedSkillCount`、`trustExpectedSkillCount`、
  `startSlotIndex`、`skipUltimateCornerCheck`；
- 一次 exclusive callback 内仍完整执行 HEAD 的截图、模板判断、槽位扫描、删除、终极技能处理、sleep、
  fallback、deadline、stop/pause checkpoint 与结果组装顺序；
- HEAD 的 `UICleanerService.cleanLightweightInterruptions("summon-skill:finish")` 仍只在 exclusive queue
  owner 已释放后执行；它不属于 exclusive session，且其失败不得改写已形成的 cleanup result；
- `EXECUTED + cleanupResult.success=false` 是已执行的业务失败，不是机械 `NOT_EXECUTED`；
- `UNKNOWN` 是机械不确定态，不得压成 `success=false`、不得消费成普通失败、不得自动重试；
- Cloud 不接触 HWND、窗口标题搜索、截图像素、ROI、模板路径、OCR 中间物、坐标、click 或自由
  `InputAction` 列表。

**无已批准业务差异；按基线等价迁移。**

### 2. 单一 owner 与 closed capability

#### 2.1 精确所有权链

唯一调用链如下，业务 Service 只能从当前 `CloudTaskServicePort` 取得最后一级 capability：

```text
CloudTaskRunRetainedLifecycleActivationAdapter
  -> CloudTaskRunAuthorityAssembly
  -> CloudTaskExclusiveInteractionAuthority            [package-private retained owner]
  -> CloudTaskRetainedActionState / CloudTaskRunActionLedger
  -> GenerationProjection                              [package-private, exact K generation]
  -> CloudTaskServiceExecutionContext
  -> CloudTaskServicePort
  -> CloudSummonSkillWholePassCapability               [public closed facade]
  -> CloudTaskExclusiveInteractionAuthority.execute(...)
  -> CloudTaskRunCommandExecutor / RemoteGameCommandBroker
  -> one SUMMON_SKILL_WHOLE_PASS request
  -> DHXY LocalRemoteGameCommandHandler
  -> InputActionQueue exclusive owner
  -> SummonSkillService HEAD direct worker-thread path
```

`CloudTaskExclusiveInteractionAuthority` 由 `CloudTaskRunAuthorityAssembly` 构造一次并持有；它不是 Spring
business bean，也不从业务 Service 注入。`CloudSummonSkillWholePassCapability` 的 constructor 为
package-private，业务侧不能 `new`。capability 不暴露 owner、requestId、actionId、sessionId、revision、
generation、raw request、poll、outcome、receipt 或 action handle。

#### 2.2 constructor / visibility / owner

| 类型 | visibility / constructor | owner | 对业务可见面 |
|---|---|---|---|
| `CloudTaskExclusiveInteractionAuthority`（New） | package-private `final`；constructor package-private，仅 assembly 调用 | `CloudTaskRunAuthorityAssembly` 单例字段 | 无 |
| `GenerationProjection`（authority 内部 nested） | package-private immutable；仅 owner `prepareInitialProjection` / `prepareResumeProjection` 铸造 | retained owner entry | 无 getter 暴露身份 |
| `CloudSummonSkillWholePassCapability`（New） | `public final`；constructor package-private | exact `GenerationProjection` | 仅 `execute(WholePassIntent)` |
| `WholePassIntent`（capability nested public record） | public value；四个基线字段 | business caller 只提供业务意图 | 无窗口/输入/模板字段 |
| `WholePassResult`（capability nested sealed result） | public sealed；`Executed`、`NotExecuted`、`Unknown`、`Stopped` | authority 从 typed outcome 构造 | 无 raw outcome |
| `SummonSkillWholePassActionHandle`（`CloudTaskRetainedActionState` nested） | package-private，non-mintable | retained state / action ledger | 无 |
| `SummonSkillWholePassRequest` / `SummonSkillWholePassOutcome`（New） | package-private protocol records | command executor / broker | business Service 不可见 |

`Executed` 内含与 HEAD `SummonSkillCleanupResult` 等价的 Cloud value：`success`、`skillCount`、
`nextStartIndex`、`observedSlotStatuses`、`ultimateSkillClicked`、`ultimateSkillSucceeded`、
`inspectedSlotCount`、`deletedSkillCount`、`message`。槽位状态使用 closed enum 镜像，不传图片或路径。

#### 2.3 稳定 session/action identity

authority 使用不含 revision 的 interaction address：

```text
(RemoteTaskRunScope, taskRunId, taskType, RemoteTaskRunWindow,
 admissionStopEpoch, ActionAddress("summon-skill", "whole-pass"))
```

首次显式业务调用在 authority monitor 下先查重，再做 quota reservation。只有 retained owner 能：

1. 生成一次 `exclusiveSessionId`；
2. 通过 `CloudTaskRetainedActionState` 向 `CloudTaskRunActionLedger` retain 一个
   `SUMMON_SKILL_WHOLE_PASS` action handle；
3. 用上述 address、sessionId 与首次 ACTIVE revision 构造 R-X0
   `StableExclusivePassKey` / `CloudTaskExclusiveInteractionState`；
4. 把 exact retained handle、state 与单一 completion 放入同一个 owner entry。

`ActionAddress` 当前真实字段只有 `phaseCode/actionSlot`；occurrence/attempt 属于
`CloudTaskRunActionLedger.RetainedActionIdentity`，不得复制到第二张 map 或让 capability 自己计算。
同一未终结 interaction 跨 runRevision 始终复用同一个 sessionId、同一个 retained action handle、同一个
occurrence/attempt 和同一个 completion。只有前一 occurrence 已确定终结并完成 final consumption 后，后续
由原业务流程再次显式调用时，ledger 才能建立新 occurrence；authority 自身不续租、不重试、不 remint。

并发 duplicate 在 quota 检查前命中同一个 owner entry，join 同一个 completion；不会再次计数或再次发命令。
admission 采用 owner 内部 `RESERVING -> LIVE` 两阶段条目：reservation 已进入 map 后 duplicate 等待该
reservation；ledger retain 失败时在同一 monitor 精确回滚 map 和三层计数，不向业务暴露半成品 session。

### 3. ACTIVE -> PAUSED -> RESUMED 原子交接

#### 3.1 generation 投影

每个 `CloudTaskRunCurrentContextSlot.TaskServiceRuntime` 增加一个 immutable
`GenerationProjection` 引用；同一个引用同时传入该 runtime 的
`CloudTaskServiceExecutionContext` 和 `CloudTaskServicePort`，port 再据此构造 closed capability。
projection 固定包含 owner entry 引用、exact `RemoteTaskRunContext`、slot generation handle、
runRevision 与 R-X0 binding generation，但不提供业务 getter。

每次 `execute` 的 pre-side-effect gate 必须同时验证：

1. projection 仍是 authority entry 的 current projection；
2. `CloudTaskRunCurrentContextSlot` 当前 `TaskServiceRuntime` 持有同一个 projection 引用；
3. existing `CloudTaskRunExecutionGate` 接受 exact ACTIVE context；
4. scope/taskRun/taskType/window/stopEpoch 与 stable interaction key 相同。

任一不等即 stale/fenced，且在 broker mint/bind request 之前失败。旧 port/capability 因第 1、2 条必然拒绝；
业务不能用旧 capability 跳到新 revision。

#### 3.2 pause 与 resume 顺序

`CloudTaskRunRetainedLifecycleActivationAdapter.acquirePausedObservation(...)` 在现有 exact PAUSED snapshot
校验成功后，调用 authority 的幂等 `parkPaused(...)`。为覆盖没有 observation consumer 的正常暂停，
`resume(...)` 在调用 `assembly.resumeTaskServiceRuntime(...)` 前也必须以 gate-minted exact PAUSED context
执行同一幂等 park；因此 resume 不能绕过 PARKED 状态。

如果 whole-pass request 已 bind 且仍在 DHXY 队列中，R-X0 必须允许把 `ACQUIRE_BOUND` 的 origin 一并 park；
`PARKED_PAUSED` 保存 `parkedFrom=ACQUIRE_BOUND`。本地同一个 `TaskPauseToken` 等待，不释放、不 abort、不创建
新 queue request，不重置 session、nextStep、deadline 或 jitter。

resume 在现有 retained-entry monitor 和 K transition lock 内按以下顺序执行：

1. 校验 latest lifecycle handle 与 exact PAUSED snapshot；
2. authority `prepareResumeProjection(...)`：在同一 owner entry 上把 R-X0
   `PARKED_PAUSED -> HANDOFF_BOUND`，保留 session/action identity/nextStep；
3. assembly 用原 `CloudTaskRetainedActionState` 和 provisional projection 创建 next
   `CloudTaskServiceExecutionContext`、port、capability 与 `TaskServiceRuntime`；
4. `CloudTaskRunCurrentContextSlot.prepareResumeTransition(...)` 校验 next runtime 的 projection；
5. existing H activation 成功；
6. 在 K lock 未释放时 authority `commitResumeProjection(...)`，R-X0 generation `+1`、revision 更新，并恢复
   `parkedFrom`（whole-pass in-flight 时为 `ACQUIRE_BOUND`）；
7. K 发布同一个 projection 的 next runtime，然后 retained adapter 发布 next lifecycle handle。

第 6 与第 7 之间采用双引用 gate：old projection 已 stale，new projection 尚未成为 K current，因此两者都不能
执行；K 发布后 new context/port/state 才同时可用。若 H 或 prepare 失败，authority 取消 provisional handoff，
保持原 PARKED projection；不得产生新 session/action identity。

已 bind 的旧 request bytes 永不改写 revision。它只能作为已在执行的同一 local session 继续等待；resume 后重新
送达的旧 revision request 会被 DHXY initial revision fence 拒绝。尚未 bind request 的 retained identity 可沿用
`CloudTaskRunActionLedger.prepareActiveInvocation(...)` 在首次 bind 前更新到 current revision，仍不 remint identity。

### 4. whole-pass 状态与 outcome 表

本表中的 “release proof” 是 DHXY typed outcome 内部的机械证明：exclusive callback 已退出且 queue owner 已在
`finally` 释放。它不会进入 business result。

| 事件 | owner / R-X0 前态 | 必要动作与证明 | 终态、identity 与返回 |
|---|---|---|---|
| 首次 acquire | 无 entry | 原子 quota reservation；retain exact action；`DECLARED -> ACQUIRE_BOUND`；bind 一份 immutable request | 同一 live entry；发一个 typed intent |
| duplicate（Cloud business） | `RESERVING` / 任一非终态 | 按稳定 address 命中并 join；不得再 admission/mint/send | 原 completion；状态不变 |
| duplicate（wire/client） | request 已 bind | broker + DHXY `RemoteOperationLedger` 以 exact request/action/digest 去重 | 返回 retained outcome 或继续等待；绝不重跑 callback |
| local queue acquire + execute | `ACQUIRE_BOUND` | DHXY exact-window gates 后，由唯一 input worker acquire；callback 内直执 HEAD | Cloud 等 typed terminal outcome；不提前假定成功 |
| `EXECUTED` | `ACQUIRE_BOUND` 或其 PAUSED/handoff 恢复态 | 要求 callback-started、完整 cleanup payload、release proof；authority 依次 commit `activateAcquired -> bindStep -> completeStep -> bindRelease -> completeRelease` | `RELEASED`；final consume 后返回 `Executed(cleanupResult)`；其中 `success=false` 原样保留 |
| `NOT_EXECUTED` | `ACQUIRE_BOUND` | 必须证明 callback 从未开始且没有 owner；新增 `completeUnacquiredAbort(...)` | `ABORTED`；本 occurrence complete，不 renewal；返回 `NotExecuted` |
| `STOPPED`（未开始） | `ACQUIRE_BOUND` | exact stop/terminal fence + owner-not-acquired proof | `ABORTED`；返回 `Stopped`，无 cleanup boolean |
| `STOPPED`（已开始） | `ACQUIRE_BOUND` | stable stop token 使 HEAD checkpoint unwind；必须有 release proof | 先确认 acquired，再 `bindAbort -> completeAbort`；`ABORTED`；返回 `Stopped` |
| `UNKNOWN` | 任一 bound 状态 | `holdUnresolvedFence(...)` 保存 `unresolvedFrom`；不 final-consume | `UNRESOLVED_FENCE_HELD`；保留 session、action identity、quota；返回/传播 `Unknown`，禁止映射 false |
| exact late non-UNKNOWN | `UNRESOLVED_FENCE_HELD` | requestId/actionId/digest/context 与 retained identity 完全相同；新增 `restoreResolvedFence(...)` 回原 bound 状态 | 按 `EXECUTED` / `NOT_EXECUTED` / `STOPPED` 原路径唯一终结；不创建新 request |
| 非 exact late / stale revision | 任一 | retained ledger、current projection、DHXY registry 任一 fence 拒绝 | 状态不变；不消费、不执行 |
| PAUSED（等待队列或 callback 中） | `ACQUIRE_BOUND` | authority park 保存 bound origin；本地 stable `TaskPauseToken` 阻塞 | `PARKED_PAUSED`；不 release/abort/remint/reset |
| RESUMED | `PARKED_PAUSED` | current-generation handoff；same local session fence 接受 exact newer ACTIVE registration | generation +1，恢复 `ACQUIRE_BOUND`；old capability/request fence 拒绝 |
| stop while PAUSED | `PARKED_PAUSED` | stop token 唤醒并 unwind；若已 acquire 必须等 release proof | `ABORTED` 或在证据缺失时 `UNRESOLVED_FENCE_HELD` |
| disconnect before callback | `ACQUIRE_BOUND` | 无 owner-acquired proof | `UNKNOWN` fence；不自动 resend/retry/takeover |
| disconnect during/after callback | bound/active | 无 release proof即不猜测 | `UNKNOWN` fence；exact late outcome 可解 fence |
| terminal close，未 dispatch | `DECLARED` / 可证明未 acquire | exact terminal binding，abort；不发新 request | `ABORTED`，consume/compact 后移除 entry |
| terminal close，已 dispatch | bound/paused/unresolved | 发出既有 stop；必须等 local not-acquired 或 release proof | 有证明才 `ABORTED` 并 cleanup；无证明保留 terminal unresolved fence 与 quota |
| normal cleanup | `RELEASED` / `ABORTED` | final consumption 已接受 exact non-UNKNOWN，ledger occurrence compacted，且 local owner release 已证明 | authority monitor 下一次性 remove entry/decrement quota |

`UNKNOWN` 不触发 timer、TTL、自动 retry、request replacement、occurrence renewal、session takeover 或容量驱逐。
若 client 永久断线且始终没有 release proof，terminal unresolved entry 会保留到进程结束；这是 fail-closed 的明确代价，
由诊断暴露，不以猜测清理物理 owner。

### 5. DHXY local exclusive queue 边界

Cloud wire 只新增一个 `SUMMON_SKILL_WHOLE_PASS` operation。其业务 payload 只有四个 request 字段；协议内部 context
可带 retained request/action/session/generation fence，但这些字段不进入 business capability。

`LocalRemoteGameCommandHandler` 处理顺序固定为：

1. 现有 clientSession/taskRun/window/stopEpoch/revision/digest 与 exact bound
   `WindowRuntimeContext` gate；
2. decode closed `RemoteSummonSkillWholePassCommandPayload`，构造现有
   `SummonSkillCleanupRequest`；
3. 通过新增的 detailed exclusive API 向 `InputActionQueue` 只提交一个 callback；
4. worker acquire 后在 `WindowTaskContextHolder.callWith(exactContext, ...)` 范围内调用现有 public
   `SummonSkillService.cleanSummonSkillsOnce(request)`；因为当前线程就是唯一 input worker，HEAD 的
   `isInputWorkerThread()` 分支直接进入原 private workflow，不会再次 enqueue；
5. callback 内只允许 HEAD 的 direct `InputProvider` / `InputActionScope.checkpoint()` 路径，严禁
   `InputSequences.submitAndWait(...)`、`submitExclusiveAndWait(...)` 或任何 queue-in-queue；
6. callback 返回后 queue 在 `finally` 释放 owner；仅 `COMPLETED` 且取得 cleanup result 时，本地 handler 才在队列外
   调用 `UICleanerService.cleanLightweightInterruptions("summon-skill:finish")`，保持 HEAD 顺序；
7. 将 mechanical status、cleanup result（仅 EXECUTED）、callbackStarted 与 ownerReleased proof 编码为 closed outcome。

本地 detailed exclusive API 不是 action-list API：

```text
InputActionQueue.submitRemoteExclusiveAndWaitDetailed(
    description,
    Supplier<Boolean> exclusiveCallback,
    deadlineNanos,
    TaskPauseToken,
    Supplier<InputActionSafetyReason> safetyReason,
    Supplier<InputActionSafetyReason> workerAdmission)
```

它复用现有 `InputActionRequest` exclusive callback 分支与唯一 `InputActionWorker`，不新增 executor、queue 或物理 owner。
cleanup result 使用 handler 外部的单赋值 holder 与同步 queue completion 带回；不把回调改成自由 actions。
`InputActionScope.checkpoint()` 对 detailed request 额外读取同一个 session fence，使每个 HEAD 已有 checkpoint 都能看到
stop、pause、stale generation 与 exact-window invalidation；legacy request 路径保持原行为。

本地 in-flight fence 只是一份随 queue request 存活的 `RemoteExclusiveInteractionFence`，不是第二业务 authority：
它不能 mint session/action，不能调度或重试，只能凭 wire 内的 unforgeable session/generation 与
`RemoteTaskRunRegistry` 当前注册原子接受 same taskRun 的 exact resume generation。外部重新送达的旧 revision command
仍在 handler initial gate 被拒绝；只有已经 acquire 的同一个 callback 持有该 fence 并继续。

结果映射固定：

- queue `COMPLETED` + result present + owner released -> `EXECUTED`；
- queue 明确 `NOT_STARTED` + owner never acquired -> `NOT_EXECUTED`；
- exact stop 且 owner released -> `STOPPED`；
- callback started 但结果/释放/窗口 fence 任一无法证明 -> `UNKNOWN`。

不得新增点击、技能选择、识别、fallback、sleep、retry、stop 条件，也不得把 queue/runner negative signal 变成新的业务
真值。

### 6. hard cap、restart 与诊断

R-X1 专属 cap 由 `CloudTaskExclusiveInteractionAuthority` 自己持有，不借用：

- per stable taskRun：`1` 个 live/unresolved whole-pass session；
- per tenant（key 仅为 `RemoteTaskRunScope.tenantId()`）：`64` 个 live/unresolved session；
- process global：`1000` 个 live/unresolved session；
- DHXY physical execution：继续由现有单一 `InputActionQueue` / `InputActionWorker` 保证全局 `1` 个物理 owner，
  不再加一层数值 cap。

现有 action-ledger retained-slot cap、lifecycle-adapter cap 与 broker cap 都维持原 owner/含义，不能拿来替代上述三层。
admission/removal 只在 authority monitor 修改 `entries`、`tenantCounts`、`globalCount`；duplicate 先查重，
terminal removal 以 entry 内一次性 retirement flag 防双减。`UNKNOWN` 与无 release proof 的 terminal entry 不减 cap。

Cloud 与 DHXY restart 均不 durable restore session：

- Cloud restart 后不反序列化 owner entry、sessionId、generation 或 completion；旧 outcome 因新
  clientSession/taskRun authority fence 不能接管；
- DHXY restart 后不恢复 queue request 或 local fence；必须由正常 task/client registration 建立新 run；
- 不做跨进程 lease、TTL、takeover 或“猜测 owner 已释放”。

运维诊断 owner 仍是 Cloud authority。它只输出 read-only snapshot / structured log：
tenant live count、global live count、run 是否占用、R-X0 status、binding generation、runRevision、nextStep、
是否 unresolved、unresolved origin、local release-proof 状态和最后 mechanical outcome。日志对 sessionId/actionId/requestId
只给哈希/末尾掩码，不暴露 capability 或可重放 wire bytes；不得提供运维强制释放 API。

### 7. 唯一最小实施波次

这是一个原子 cross-repo 波次；protocol、Cloud retained owner 和 DHXY local execution 必须一起完成后才可启用。
不先提交“永远 NOT_EXECUTED”的 capability、占位 wrapper 或第二套 authority。业务 host/caller 激活仍由父任务后续
独立批准；本波只形成真实可调用闭环，不切 production caller。

#### 7.1 Cloud New

| 文件 | exact type / 方法 |
|---|---|
| `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java` | package-private `final` owner；`prepareInitialProjection(...)`、`parkPaused(...)`、`prepareResumeProjection(...)`、`commitResumeProjection(...)`、`cancelResumeProjection(...)`、`executeSummonSkillWholePass(...)`、`acceptLateOutcome(...)`、`closeTerminal(...)`、`diagnosticSnapshot()`；nested `GenerationProjection` / owner entry / quota reservation |
| `.../CloudSummonSkillWholePassCapability.java` | `public final` closed facade；package-private constructor；public `execute(WholePassIntent)`；nested `WholePassIntent`、sealed `WholePassResult`、`Executed`、`NotExecuted`、`Unknown`、`Stopped`、cleanup value 与 slot-status enum |
| `.../SummonSkillWholePassRequest.java` | package-private `RemoteRequest` implementation；只封装 common exact context、internal session/generation fence 与四字段 intent |
| `.../SummonSkillWholePassOutcome.java` | package-private `RemoteOutcome` implementation；closed mechanical status、可选 cleanup payload、started/release proof；constructor 校验 UNKNOWN 不带 cleanup boolean |

#### 7.2 Cloud Modify

| 文件 | exact 修改点 |
|---|---|
| `.../CloudTaskExclusiveInteractionState.java` | 增加 `parkedFrom` shape；`parkPaused(...)` 接受 `ACTIVE` 或 `ACQUIRE_BOUND` 并保留 origin；handoff completion 恢复 origin；新增 `restoreResolvedFence(...)` 与 `completeUnacquiredAbort(...)`；保留 immutable/package-private/fail-closed |
| `.../CloudTaskRunAuthorityAssembly.java` | constructor 唯一构造 authority；initial/resume/terminal assembly 纳入 projection prepare/commit/rollback；`TaskServiceRuntime` 携带 exact projection；不把 owner 暴露给 host |
| `.../CloudTaskRunCurrentContextSlot.java` | `prepareResumeTransition(...)` 校验 next runtime/context/port 共用 exact projection；current-runtime validation 支持 capability 双引用 gate；不新增第二 current pointer |
| `.../CloudTaskServiceExecutionContext.java` | package-private constructors 接收 exact projection；initial 新 retained state、resume 复用原 retained state；向 port 传同一引用 |
| `.../CloudTaskServicePort.java` | package-private constructor 接收 projection/authority；新增 public `summonSkillWholePass()`，只返回 closed capability |
| `.../CloudTaskRunRetainedLifecycleActivationAdapter.java` | `acquirePausedObservation(...)` 成功时幂等 park；`resume(...)` 在 assembly resume 前强制 exact PAUSED park/prepare handoff，失败 rollback；`closeTerminal(...)` 先交 authority 做 proof-aware terminal close |
| `.../CloudTaskRetainedActionState.java` | fixed `ActionAddress("summon-skill","whole-pass")`；新增 package-private `retainSummonSkillWholePass(...)` 与 non-mintable `SummonSkillWholePassActionHandle`；不公开 occurrence/attempt |
| `.../CloudTaskRunActionLedger.java` | 新 operation 的 retain/prepare/bind/late-final/consume 分支；同一 retained identity 首次 bind 前可换 current context，bind 后 request bytes immutable；`NOT_EXECUTED` occurrence complete、`UNKNOWN` 不 consume，禁止自动 renewal |
| `.../RemoteOperation.java` | 增加 `SUMMON_SKILL_WHOLE_PASS` |
| `.../RemoteRequest.java` / `.../RemoteOutcome.java` | sealed permits 纳入新的 closed request/outcome |
| `.../RemoteGameClientPort.java` | package-private typed whole-pass send seam；不增加 raw/free-action public API |
| `.../CloudTaskRunCommandExecutor.java` | exact active gate 后执行 typed request；只把 typed outcome交 authority，不向 business 返回 raw envelope |
| `.../RemoteGameCommandBroker.java` | 新 operation 的 immutable command/outcome retain、duplicate 与 exact late correlation；不自动 retry |
| `.../RemoteFinalConsumptionCoordinator.java` | 对新 operation 强制 non-UNKNOWN 才 final consume；UNKNOWN 保留 fence；不改变既有 operation |
| `.../RemoteProtocolDigests.java` | 新 request/outcome canonical digest；字段顺序固定，不含 HWND/模板/图片/action list |

#### 7.3 DHXY New

| 文件 | exact type / 方法 |
|---|---|
| `DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteSummonSkillWholePassCommandPayload.java` | immutable closed payload；四个业务字段 + protocol-internal opaque session/generation；严格校验，不含窗口句柄/路径/actions |
| `.../RemoteSummonSkillWholePassOutcomePayload.java` | mechanical enum + exact cleanup value + callbackStarted/ownerReleased proof；UNKNOWN/STOPPED/NOT_EXECUTED shape fail-closed |
| `.../RemoteExclusiveInteractionFence.java` | package-private per-request mechanical fence；`checkBeforeSideEffect()`、`acceptExactResume(...)`、`markOwnerAcquired()`、`markOwnerReleased()`；无 map、cap、mint、retry、transport 权限 |

#### 7.4 DHXY Modify

| 文件 | exact 修改点 |
|---|---|
| `.../cloud/remote/RemoteGameOperation.java` | 增加 `SUMMON_SKILL_WHOLE_PASS` |
| `.../cloud/remote/RemoteOperationPayloadCodec.java` | closed command/outcome encode/decode；拒绝额外字段与自由 action list |
| `.../cloud/remote/RemoteProtocolDigests.java` | 与 Cloud 同序 canonical digest |
| `.../cloud/remote/RemoteOperationLedger.java` | 新 operation exact duplicate/outcome retain；UNKNOWN 不伪造 final，不重跑 callback |
| `.../cloud/remote/LocalRemoteGameCommandHandler.java` | 注入现有 `SummonSkillService`、`UICleanerService`；新增 typed branch，做 exact registry/window fence、单次 detailed exclusive enqueue、结果映射与队列外 finish cleanup |
| `.../input/action/InputActionQueue.java` | 新增 `submitRemoteExclusiveAndWaitDetailed(...)`；只接受 callback，不接受 actions |
| `.../input/action/InputActionRequest.java` | 增加 detailed exclusive callback constructor/factory，复用现有 callback 分支和 pause/stop/deadline metadata |
| `.../input/action/InputActionScope.java` | detailed request 的既有 checkpoint 追加 `RemoteExclusiveInteractionFence.checkBeforeSideEffect()`；legacy scope 语义不变 |

`SummonSkillService.java`、`TaskMaintenanceService.java`、模板、截图、OCR、导航、click 与 input provider 文件均不修改。
handler 复用 `SummonSkillService` 当前 worker-thread direct 分支，不新建同义 wrapper，也不暴露 private workflow。
`InputActionWorker.java` 复用现有 exclusive callback 分支，不修改。

#### 7.5 hard-gap 判定

无外部硬 blocker。当前源码确实尚无 `SUMMON_SKILL_WHOLE_PASS` protocol、assembly-owned exclusive authority、
bound-state pause origin、late UNKNOWN 恢复方法及 detailed remote exclusive callback API；这些是同一个闭环的组成部分，
已全部纳入上述唯一原子波次，不能拆成占位能力。实施前父级只需确认共享 protocol 文件没有被其他 worker 同时占用；
这属于写集调度，不是设计 blocker。

### 8. 与 A / B / Y 的零交叉

本轮真实 unique write set 始终只有本报告日志，已经与两仓所有 dirty/untracked 隔离。

后续唯一实施波次明确不触碰：

- 外部 A：任何 `Navigation*`、Navigation config / workflow state、地图/坐标/路径配置文件；
- 外部 B：任何 `TeamReturn*`、组队返回业务/本地执行文件；
- 内部 Y：`PlayerFirstAidDecision.java`、任何 `PlayerState*` / first-aid service、PlayerState protocol payload。

当前 Y 已声明的唯一 Java 写集 `dhxy-cloud-brain/.../PlayerFirstAidDecision.java` 与本设计表零交叉。若 A/B/Y 后续也要改
共享 `RemoteOperation` / codec / digest，父级必须串行分配，不能并发写；Z 不吞并其业务 operation，也不替它们做
shared-file 合并。

### 9. 实施验收点（非本轮执行）

后续获批实现时只验收结构与编译门，不新增本地自动测试：

1. business Service 无法构造 capability/session/action handle，也无法取得 raw protocol；
2. old revision capability 和重新送达的 old revision request 在任何副作用前拒绝；
3. same in-flight interaction resume 后 session/action identity/occurrence/nextStep 不变，仅 generation/revision 前进；
4. local callback 期间 queue 深度不会因 SummonSkill 自调用增加，且 callback 内没有 `submitAndWait`；
5. HEAD 的截图/模板/click/sleep/fallback/stop/pause 与 finish cleanup 顺序逐项保持；
6. duplicate 只执行一次，UNKNOWN 保留 fence/cap，late exact final 唯一解 fence；
7. tenant `64`、global `1000`、per-run `1` 的 admission/removal 账本一致，restart 不 restore；
8. 两仓适用 compile gate 在实现者后续获批 Java 波次执行；本 Design #1 不运行 Maven/tests/runtime。

### 10. Self-QA（不构成 Approved）

- P0: `0`
- P1: `0`
- P2: `0`
- 边界复核：一个 typed whole-pass intent；Cloud retained owner 单一；DHXY exact-window / capture /
  template/OCR / physical queue 单一；无 queue-in-queue；无 retry/TTL/takeover；无业务语义变化。
- 交付复核：本轮仅 append 固定日志；零 Java、零 Maven/tests、零运行面、零 Git mutation。
- reviewer 结论：**未给出**。Worker Z 自审不算 Approved，等待父级独立 `DESIGN APPROVED` 或 `BLOCKED`。

## Parent Design Review #1 - BLOCKED / Repair #1 Published - 2026-07-13T13:29:58-04:00

父级完整对照本报告、已批准 R-X0、Cloud 当前 H/K/assembly/retained-action 实现，以及 DHXY HEAD
`0114604e` 的 `SummonSkillService.cleanSummonSkillsOnce(...)`、稳定 `TaskPauseToken`、registry 与 input worker。
结论：**BLOCKED，P0=0/P1=3/P2=0**；本轮 Java 继续冻结。

1. **P1：post-pass `UICleaner` failure matrix 改变 HEAD。** 本设计第 5 节步骤 6（本日志 :266-268）只在 queue
   `COMPLETED` 且取得 cleanup result 时运行
   `cleanLightweightInterruptions("summon-skill:finish")`。但 HEAD
   `SummonSkillService.java:262-282` 在 `submitExclusiveAndWait(...)` 明确返回 `false` 时仍执行 :281 的 cleanup；
   已批准 R-X0 Design Review #3（旧日志 :967-969）也已冻结：只要得到 non-UNKNOWN 且已证明本地无 owner 的失败
   结论，仍须先冻结 failed result，再在独占外执行同一 cleanup。当前设计会在可信 `NOT_EXECUTED`/未开始
   `STOPPED` 路径漏掉基线 UI 清理，可能把残留面板带入下一轮业务。返修必须给 exact post-pass 表：
   `EXECUTED` 且 release proof、以及 `NOT_EXECUTED/STOPPED` 且 owner-never-acquired/owner-released 的确定结果，均在
   session 外运行一次 cleanup；只有 `UNKNOWN` 或仍可能持 owner 时禁止越过。cleanup 仍不得进入 session、payload、
   pass result 或 cooldown 决策。

2. **P1：same callback 的 resume revision fence 没有真实 caller/发布链。** 本设计 :287-290 声称
   `RemoteExclusiveInteractionFence.acceptExactResume(...)` 可接受 registry 的新 ACTIVE generation，文件表 :369 只列了
   该方法；但 :375-382 的 Modify 表没有任何 `applyConfirmedBinding`、resume-ready receipt、registry entry handle 或其它
   production caller 把新 revision 交给该 fence。与此同时 immutable old command 必须继续在 handler re-entry 拒绝，
   不能直接放宽 `request.runRevision`。缺口会导致已暂停 callback 要么在恢复后永远被旧 revision fence 拒绝，要么只能
   放宽为 stale request 复活。返修必须给一个可编译的 exact owner/call chain：初始 handler/worker admission 仍精确校验
   command revision；in-flight callback 只凭同一 registry entry、同一稳定 pause token、same scope/taskRun/window/
   stopEpoch/session 与已确认的 PAUSED->ACTIVE successor 更新自己的 opaque generation；说明由哪个现有方法铸造、哪个
   方法消费、何时在 `InputActionScope.checkpoint()` 前发布。不得新增第二 registry、默认 session 或接受重投旧 command。

3. **P1：H/K/interaction authority 的 resume 三方提交在 H 激活后仍有可失败步骤。** 本设计 :197-212 的顺序是
   H activation 成功后执行 `authority.commitResumeProjection(...)`，再发布 K；但当前
   `CloudTaskRunAuthorityAssembly.resumeTaskServiceRuntime` :285-293 在 H `activateResumed(...)` 成功后刻意只剩
   `nextRuntime.stateActivationHandle = ...` 与 `AtomicReference.set(...)`，没有新的校验/可抛 transition。若新增 authority
   commit 因 stale token/shape/counter 抛出，H 已指向新 generation，K 仍是旧 runtime，interaction projection 还可能处于
   provisional，无法原子回滚。返修必须把所有 identity/shape/stale/capacity 校验和可能失败的状态构造放到 H 激活前；
   H 成功后只能执行基于已验证 non-mintable prepared permit 的无校验、无分配、不可失败 publication（或给出可证明的
   等价原子机制），并补 H 失败、authority prepare 失败、terminal race、publication 前后异常的精确状态表。不能用文字
   “失败 rollback”替代当前不存在的 H rollback API。

### 下一任务 `W-SS-X1-D2`

同一 Internal Worker Z 只在本日志真实末尾追加 `CLAIMED` 与 Design Repair #1 Delta，逐项关闭上述 P1=3；两仓
Java/Maven/schema/resources/tests、A/B/Y 写集、host/caller 全冻结。保留已经成立的单一 typed whole-pass、本地
capture/template/OCR/input、stable session/action identity、hard caps、UNKNOWN/no-retry/no-TTL 结论，不重写全篇。
Delta 必须同步修正 exact New/Modify 表及唯一最小实施波次；Worker QA 不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

**无已批准业务差异；按基线等价迁移。**

## CLAIMED

- task: `W-SS-X1-D2`
- claimedAt: `2026-07-13T13:31:19-04:00`
- uniqueWriteSet: `docs/superpowers/plans/reports/2026-07-13-cloud-summon-skill-exclusive-owner-worker-z.md`（仅该 append-only 日志）

## W-SS-X1-D2 Design Repair #1 Delta

- repairedAt: `2026-07-13T13:37:52-04:00`
- status: `REPAIR READY FOR PARENT REVIEW`
- role: Internal Worker Z（不是 reviewer）
- scope: 只修 Parent Design Review #1 的 P1=3；Design #1 其余已成立边界继续有效

本 Delta 精确废止 Design #1 的三处表述：

1. 废止“仅 `COMPLETED + cleanup result` 才运行 post-pass UICleaner”；
2. 废止没有 production publisher 的独立 `RemoteExclusiveInteractionFence.acceptExactResume(...)`；
3. 废止 H 成功后再调用可校验/可抛的 `authority.commitResumeProjection(...)` 与 rollback 说法。

以下三节是对应替代设计；与本 Delta 冲突时以本 Delta 为准。

### R1. P1-1：HEAD 等价 post-pass UICleaner matrix

#### R1.1 冻结点与边界

DHXY local typed handler 在 queue call 返回后，先依据
`InputActionExecutionResult`、callback result holder 与 exact local continuation proof 冻结一个
`PostPassDisposition`。冻结发生在 exclusive queue owner 已由 worker `finally` 释放之后、调用
`UICleanerService` 之前。

`PostPassDisposition` 不是 wire/schema 类型，不进入 Cloud capability，也不改变
`SummonSkillCleanupResult`。它只回答两个本地问题：

1. pass mechanical terminal 是否已经确定；
2. 已确定没有物理 owner 后，是否必须按 HEAD 尝试一次
   `cleanLightweightInterruptions("summon-skill:finish")`。

凡是确定终态且证明 `ownerNeverAcquired || ownerReleased`，均尝试一次 cleanup；不是只看
`InputActionExecutionResult.completed`。只有 `UNKNOWN` 或仍可能持有 owner 时禁止越过 session 边界。

#### R1.2 exact matrix

| queue/pass 证据 | 冻结的 pass 结论 | owner 证明 | post-pass UICleaner | wire/business 结论 |
|---|---|---|---|---|
| callback 完成并有完整 result | `EXECUTED(result)`；`result.success` 可 true/false | `ownerReleased=true` | session 外恰好调用一次；boolean 返回值忽略 | 原样 `Executed(result)` |
| callback 明确未开始 | `NOT_EXECUTED(reason)` | `ownerNeverAcquired=true` | session 外恰好调用一次；boolean 返回值忽略 | `NotExecuted` |
| queue 已 acquire 但在第一项业务副作用前确定未执行 | `NOT_EXECUTED(reason)` | `ownerReleased=true` | session 外恰好调用一次；boolean 返回值忽略 | `NotExecuted` |
| exact stop 在 worker admission 前生效 | `STOPPED` | `ownerNeverAcquired=true` | session 外恰好调用一次；boolean 返回值忽略 | `Stopped` |
| exact stop 由 callback 内 HEAD checkpoint 观察并退出 | `STOPPED` | `ownerReleased=true` | session 外恰好调用一次；boolean 返回值忽略 | `Stopped` |
| callback/result/progress 不足以证明发生了什么 | `UNKNOWN` | owner 可能仍持有 | **不调用** | `Unknown`；不带 cleanup boolean |
| disconnect/异常且缺 release proof | `UNKNOWN` | owner 可能仍持有 | **不调用** | `Unknown` |
| exact retained duplicate / late outcome replay | 已由首次执行冻结 | 首次执行已记录 | **不再次调用** | replay retained outcome |

顺序固定为：

```text
exclusive callback exits
  -> InputActionWorker releases queue owner in finally
  -> local continuation records ownerNeverAcquired/ownerReleased
  -> freeze EXECUTED / NOT_EXECUTED / STOPPED / UNKNOWN
  -> deterministic + no-owner only: call UICleaner once outside session
  -> publish the already-frozen typed outcome
```

`UICleanerService.cleanLightweightInterruptions(...)` 当前真实返回 `boolean`；与 HEAD 一样不读取该 boolean 来改写
pass result。它不进入 exclusive session、command/outcome payload、Cloud retained identity、cooldown 或
`TaskMaintenanceService` 的既有决策。

若 UICleaner 自身抛出未捕获异常，则不得把已经执行的 pass 伪装成 `NOT_EXECUTED` 或普通
`success=false`：该 local operation 没有完成 post-pass publication，机械结果为 `UNKNOWN`，不自动重跑 cleanup，
不自动重发 command。这样保留 HEAD “cleaner 异常阻止正常返回”的效果，同时不把异常塞进 cleanup result。

exact-once owner 是现有 `RemoteOperationLedger` 首次 `EXECUTE` claim：只有首次执行分支能进入上述 matrix；
duplicate/replay 直接读 retained outcome。无需新增 UICleaner 状态表、payload 字段或第二 ledger。

### R2. P1-2：same in-flight callback 的真实 resume revision fence

#### R2.1 选择现有 registry entry，不新增第二 registry

删除 Design #1 的 New 文件 `RemoteExclusiveInteractionFence.java`。替代物是
`RemoteTaskRunRegistry.InFlightExclusiveHandle`：它是 package-private、non-mintable nested capability，
由当前 `RemoteTaskRunRegistry.RegistryEntry` 持有唯一引用。每个 entry 至多一个 live handle，正好对应
Cloud per-run `1` 个 live/unresolved whole-pass；它不 mint session/action、不发命令、不重试、不创建 registration。

handle 捕获并始终校验：

```text
RemoteTaskRunRegistry owner identity
RegistryEntry object identity + entryGeneration
same TaskPauseToken object identity
tenant/user/device/clientSession
taskRunId/taskType
windowId/nativeHandle/processId/playerIdentityEpoch
stopEpoch
opaque exclusiveSessionId
initial command runRevision
current admitted continuation generation
```

wire 中的 `exclusiveSessionId` 只由 Cloud retained owner 铸造；registry 只 bind/比较。不存在默认 session。

#### R2.2 exact production caller / publisher / consumer chain

现有 production lifecycle caller 不修改，真实链如下：

```text
RemoteTaskRunLifecycleService.resume(scope, taskRunId)
  -> RemoteTaskRunApiClient.resume(... expected paused revision ...)
  -> RemoteTaskRunLifecycleService.applyConfirmed(scope, confirmed ACTIVE)
  -> RemoteTaskRunRegistry.applyConfirmedBinding(clientSession, registration)
  -> RemoteTaskRunRegistry.publishTransition(entry, PAUSED, ACTIVE)
  -> publish same-entry InFlightExclusiveHandle successor
  -> publish entry.registration + existing PendingExecutorReadiness
  -> same TaskPauseToken.resume()                         [最后一步]
  -> input worker / callback wakes
  -> InputActionScope.checkpoint()
  -> InputActionRequest.checkDetailedSafety("exclusive-callback")
  -> registry.checkInFlightExclusive(handle)
  -> next direct InputProvider side effect
```

“新 revision 交给 fence”的唯一 production publisher 是现有
`RemoteTaskRunRegistry.publishTransition(...)`；它由现有
`RemoteTaskRunLifecycleService.applyConfirmed(...)` 调用
`applyConfirmedBinding(...)` 触发。consumer 是现有 HEAD 直执路径每次调用的
`InputActionScope.checkpoint()`，不是 host、poller 或新线程。

#### R2.3 initial command 永久 stale，只有已 admission callback 可续

`LocalRemoteGameCommandHandler` 仍先执行当前真实链：

1. `commandAdmissionSnapshot(clientSession, command)`；
2. `RemoteOperationLedger.claim(command, snapshot)`；
3. `isCurrent(snapshot, command)`。

三步都要求 immutable `command.runRevision == current ACTIVE registration.runRevision`。通过后，typed branch 才调用：

```text
RemoteTaskRunRegistry.openInFlightExclusive(
    CommandAdmissionSnapshot,
    RemoteGameCommand,
    exclusiveSessionId)
```

该方法在 `mutationLock` 内再次做同一 exact command/revision/entry 校验，返回 phase=`QUEUED` 的 opaque handle。
queue 的 one-shot `workerAdmission` supplier 调用：

```text
RemoteTaskRunRegistry.admitInFlightExclusive(handle, originalCommand)
```

它仍要求 original command revision 等于当前 ACTIVE registration；只有成功后 phase 才成为
`CALLBACK_ADMITTED`。因此：

- PAUSED/RESUMED 发生在 worker admission **之前**：old command 已 stale，worker admission 返回
  `TASK_RUN_MISMATCH/NOT_EXECUTED`，不得借 continuation 复活；
- PAUSED/RESUMED 发生在 worker admission **之后**：这是 same in-flight exclusive callback，允许 handle
  只沿 registry-confirmed successor 前进；
- 相同 old command 被 HTTP/poller/duplicate 再次送到 handler：重新走 initial gate，revision 永久不等，仍 stale；
- handle 不可由 command 重建，也不能从 sessionId lookup；只有已经入队的原 request closure 持有它。

#### R2.4 PAUSED -> ACTIVE successor publication

`publishTransition(entry, previous, current)` 在现有 `mutationLock` 内为 live
`CALLBACK_ADMITTED` handle 预构造 immutable `ContinuationSnapshot`。接受条件全部必须成立：

- handle 仍由同一个 `RegistryEntry` / `entryGeneration` 持有；
- stable scope/taskRun/taskType/window/stopEpoch/clientSession/sessionId 全相同；
- handle 当前 snapshot 精确对应 `previous`；
- `previous.status=PAUSED`，`current.status=ACTIVE`；
- `current.runRevision > previous.runRevision`；
- 使用同一个 `TaskPauseToken` 实例。

所有字符串/identity/shape 校验、`Math.incrementExact`、new snapshot 与现有
`PendingExecutorReadiness` 构造均在任何 publish/wakeup 之前完成。之后只在 `mutationLock` 内按以下顺序赋值：

```text
entry.registration = current
entry.pendingReadiness = preparedReadiness
handle.currentSnapshot = preparedContinuationSnapshot
entry.pauseToken.resume()
```

`resume()` 必须最后执行。worker 醒来后才可能到达 `InputActionScope.checkpoint()`；该 checkpoint 在 pause wait
返回后、下一次 direct input 前调用 `checkDetailedSafety(...)`，supplier 再进入 registry
`mutationLock`，因此只能看到旧整组或新整组，不会看到半发布。

新 remote exclusive request 保持 HEAD wall-clock deadline：它不启用
`excludePauseFromDeadline`，pause 不补偿、不重置 pass deadline/jitter/session。其 pause wait 不在
`TaskPauseToken` monitor 内调用 registry supplier，避免 `pauseToken monitor -> mutationLock` 与
publisher 的 `mutationLock -> pauseToken.resume()` 锁反转。

`checkInFlightExclusive(handle)` 只接受 handle 自己已由 publisher 写入的 current snapshot；它不读取或放宽
original command revision。STOPPING/terminal、entry replace/unregister、window/player epoch/stopEpoch 变化、
foreign session 或缺 successor 一律返回 typed mismatch/stop，先于下一 direct side effect fail closed。

callback 退出且 queue owner release proof 已形成后，handler 在 `finally` 调用
`closeInFlightExclusive(handle)` 精确清空 entry 的同一引用。它不影响 Cloud UNKNOWN fence/cap，只释放 DHXY
process-local continuation slot。

### R3. P1-3：H/K/interaction authority 的 no-fail publication

#### R3.1 取消第三个 post-H commit

`CloudTaskExclusiveInteractionAuthority` 不再持有一个需要在 resume 后单独切换的
`currentProjection AtomicReference`。stable owner entry 只持有 session/action identity、quota、completion 与一个
interaction transition lock；current generation 的 `GenerationProjection` 和 R-X0 immutable state snapshot
直接嵌入 `CloudTaskRunAuthorityAssembly.TaskServiceRuntime`，继而嵌入 K 的
`CloudTaskRunCurrentContextSlot.ActiveRuntime`。

因此 K 的单次 `AtomicReference<SlotState>.set(preparedNextActive)` 同时发布：

- new `CloudTaskRunExecutionContext` / runRevision；
- new `CloudTaskServiceExecutionContext`；
- new `CloudTaskServicePort` / closed capability；
- same retained action state/session identity；
- precomputed interaction binding generation / R-X0 state；
- exact K slot generation。

authority 不再有 `commitResumeProjection(...)`、`cancelResumeProjection(...)` 或 post-H CAS。capability 的
current gate 只让 K current runtime 中的 exact projection 生效；old port/capability 仍因 K generation 不匹配而 stale。

#### R3.2 H 前 prepare

`CloudTaskRunAuthorityAssembly.resumeTaskServiceRuntime(...)` 保持当前 K transition lock，并采用固定 lock order：

```text
K ResumeTransitionPermit / transitionLock
  -> live interaction entry transitionLock（若该 run 有 live whole-pass）
```

在调用 `CloudGameContextStateOwner.activateResumed(...)` 之前完成所有可能失败工作：

1. 校验 K expected generation、authority identity、stable run key、terminal 状态；
2. 取得 exact PAUSED context；幂等 park live interaction；
3. 校验 retained entry/session/action/quota accounting 未替换、未退休；
4. 由 R-X0 old parked state 预计算
   `bindCurrentGenerationHandoff -> completeCurrentGenerationHandoff` 的最终 next snapshot；
5. 构造 next `GenerationProjection`、service context、port/capability、task context 与
   `TaskServiceRuntime`；
6. `CloudTaskRunCurrentContextSlot.prepareResumeTransition(...)` 校验 previous/next retained references、
   projection owner、next revision/generation，并预构造 `ActiveRuntime`、next slot handle；
7. 把所有 publication references 放入 non-mintable `PreparedResumeTransition`。

步骤 1-7 的 validation、allocation、counter check、`Objects.requireNonNull`、`Math.incrementExact` 或 state
transition 任一失败，都发生在 H 前；释放 prepared locks 后 K、H、old interaction projection 均保持原样，
不需要 rollback。

live interaction entry lock 从 prepare 一直持有到 H/K publication 完成，outcome/late/terminal transition 无法在
prepare 与 K publish 之间改写 R-X0 state。固定顺序为 K lock -> interaction lock；normal outcome 只拿
interaction lock并用 lock-free K reference 重验，绝不反向取得 K lock。

#### R3.3 H 成功后的唯一指令

H 调用仍是现有：

```text
nextStateHandle = gameContextStateOwner.activateResumed(
    previousStateHandle,
    nextRuntime.taskExecutionContext())
```

一旦它正常返回，代码只执行当前模式已有的两次无校验 publication：

```text
nextRuntime.stateActivationHandle = nextStateHandle
publication.set(preparedNextState)
```

`preparedNextState` 已含 interaction projection；第二行是 K 与 interaction generation 的共同线性化点。
随后返回 H 前已经构造好的 `preparedNextGeneration` 并释放 locks。post-H 不调用 authority 方法、不构造对象、
不做 stale/shape/capacity 校验、不递增 counter、不做可能失败 CAS。

本设计沿用当前 H contract：`activateResumed(...)` 抛出/中断表示 H 未 commit；正常返回表示 H 已 commit 且
handle 可直接赋值。R-X1 不另造 H rollback。

#### R3.4 exact failure / race table

| 时点 | H | K | interaction projection/state | 处理 |
|---|---|---|---|---|
| authority prepare 失败 | old | old | old PARKED/bound state | H 未调用；丢弃未发布对象，释放 locks |
| service/runtime/K prepare 失败 | old | old | old | H 未调用；无 rollback |
| terminal 在 resume 取得 K lock 前胜出 | terminal/由现有 close 管理 | CLOSED | terminal owner 处理 last projection | `beginResumeTransition` fail closed |
| resume 已持 K lock，terminal 到达 | 尚未决定 | old | interaction lock 被 resume 持有 | terminal 等 K lock，不可穿插 |
| H `activateResumed` 抛出/中断 | old（按既有 H contract） | old | old；prepared next 不可见 | 释放 locks；可按现有 lifecycle 路径重试/收敛 |
| H 正常返回、K set 之前 | new H | old K | prepared next 尚不可达；old capability 后续 H/current gate fail closed | 仅执行两次赋值，不运行可失败 commit |
| `publication.set(preparedNextState)` 完成 | new H | new K | new projection 与 context/port 同一原子引用可见 | old capability stale；new capability 可用 |
| terminal 在 K publish 后取得 lock | new H | new K 后转 CLOSED | terminal handle 持有 exact new runtime/projection | 现有 terminal close/release 路径 |
| normal Java 异常“publication 后” | new | new | new | 没有设计中的可抛调用；只返回预构造 handle/释放已持 lock |

field assignment 与 `AtomicReference.set` 不分配、不校验。若 JVM/进程在 H 返回与 K set 之间发生不可恢复的
VM termination，则该进程不允许继续服务；restart 按已冻结结论不 restore H/K/interaction session。不得捕获此类
fatal condition 后让旧 K 继续运行。

`CloudTaskRunRetainedLifecycleActivationAdapter.resume(...)` 在调用 assembly 前已预构造 next lifecycle handle；
assembly 返回代表 H/K/interaction 已共同发布。其后只保留当前已有的 retained reference/primitive 赋值，不新增
interaction commit。`acquirePausedObservation(...)` 与 `resume(...)` 的 PAUSED park 都发生在 H 前。

### R4. 修正后的 exact New/Modify 文件与方法表（v2）

本表整体替代 Design #1 第 7.1-7.4 节。未列为 Modify 的源码保持冻结。

#### R4.1 Cloud New

| 文件 | exact type / 方法 |
|---|---|
| `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java` | package-private `final` owner；`prepareInitialGeneration(...)`、`parkPaused(...)`、`prepareResumeGeneration(...)`、`executeSummonSkillWholePass(...)`、`acceptLateOutcome(...)`、`closeTerminal(...)`、`diagnosticSnapshot()`；nested immutable `GenerationProjection`、`PreparedInteractionGeneration`、stable owner entry/quota reservation。**无** `commitResumeProjection` / `cancelResumeProjection` / current-projection pointer |
| `.../CloudSummonSkillWholePassCapability.java` | D1 保持：package-private constructor；public `execute(WholePassIntent)`；closed nested intent/result/value |
| `.../SummonSkillWholePassRequest.java` | D1 保持：package-private typed `RemoteRequest` |
| `.../SummonSkillWholePassOutcome.java` | D1 保持：package-private typed `RemoteOutcome`；mechanical result 与 release proof shape |

#### R4.2 Cloud Modify

| 文件 | exact 修改点 |
|---|---|
| `.../CloudTaskExclusiveInteractionState.java` | D1 的 `parkedFrom`、bound pause、`restoreResolvedFence(...)`、`completeUnacquiredAbort(...)` 保持；handoff 的 next immutable snapshot 可在 H 前完整构造，不要求发布 intermediate `HANDOFF_BOUND` |
| `.../CloudTaskRunAuthorityAssembly.java` | constructor 唯一构造 authority；`TaskServiceRuntime` 增加 exact `GenerationProjection`；initial/resume 在 H 前调用 `prepareInitialGeneration(...)` / `prepareResumeGeneration(...)` 并把 projection 装入 runtime；H 后只赋 H handle + K `publication.set` |
| `.../CloudTaskRunCurrentContextSlot.java` | `prepareResumeTransition(...)` 在 H 前验证 next runtime projection 与 retained owner/reference/revision/generation；`ActiveRuntime` 携带该 runtime；新增 package-private `requireCurrentProjection(...)` 做 capability current gate；K set 是唯一 generation publication |
| `.../CloudTaskServiceExecutionContext.java` | constructors 接收 runtime 内同一 projection；resume 复用 retained action state |
| `.../CloudTaskServicePort.java` | constructor 接收同一 projection；`summonSkillWholePass()` 只返回 closed capability |
| `.../CloudTaskRunRetainedLifecycleActivationAdapter.java` | `acquirePausedObservation(...)` 幂等 park；`resume(...)` 在 assembly/H 前强制 park，不增加 post-H interaction commit；terminal 交 authority proof-aware close |
| `.../CloudTaskRetainedActionState.java` | D1 fixed address 与 non-mintable whole-pass action handle 保持 |
| `.../CloudTaskRunActionLedger.java` | D1 same identity / unbound context advance / UNKNOWN no-consume / no-renewal 保持 |
| `.../RemoteOperation.java`、`.../RemoteRequest.java`、`.../RemoteOutcome.java` | D1 closed `SUMMON_SKILL_WHOLE_PASS` operation 与 permits 保持 |
| `.../RemoteGameClientPort.java`、`.../CloudTaskRunCommandExecutor.java`、`.../RemoteGameCommandBroker.java`、`.../RemoteFinalConsumptionCoordinator.java`、`.../RemoteProtocolDigests.java` | D1 typed transport、duplicate/late/final-consume/digest 边界保持；无 free action/raw outcome |

#### R4.3 DHXY New

| 文件 | exact type / 方法 |
|---|---|
| `DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteSummonSkillWholePassCommandPayload.java` | D1 closed four-field intent + opaque owner fences |
| `.../RemoteSummonSkillWholePassOutcomePayload.java` | D1 closed mechanical status + optional cleanup value + started/release proof |

**删除 D1 计划中的 New `RemoteExclusiveInteractionFence.java`**；continuation 必须属于现有
`RemoteTaskRunRegistry.RegistryEntry`。

#### R4.4 DHXY Modify

| 文件 | exact 修改点 |
|---|---|
| `.../cloud/remote/RemoteGameOperation.java` | D1：增加 `SUMMON_SKILL_WHOLE_PASS` |
| `.../cloud/remote/RemoteOperationPayloadCodec.java`、`.../RemoteProtocolDigests.java`、`.../RemoteOperationLedger.java` | D1 closed codec/digest/exact duplicate；UNKNOWN 不伪 final |
| `.../cloud/remote/RemoteTaskRunRegistry.java` | 新增 package-private `openInFlightExclusive(...)`、`admitInFlightExclusive(...)`、`checkInFlightExclusive(...)`、`closeInFlightExclusive(...)`；nested non-mintable `InFlightExclusiveHandle` / immutable `ContinuationSnapshot`；修正现有 `publishTransition(...)`，在 `pauseToken.resume()` 前预构造并共同发布 registration/readiness/continuation |
| `.../cloud/remote/LocalRemoteGameCommandHandler.java` | typed branch 在现有 admission snapshot/ledger/current gates 后 open handle；workerAdmission 使用 exact original command；per-checkpoint safety supplier 使用 handle；queue 后按 R1 matrix 冻结结果、session 外调用现有 UICleaner、发布 typed outcome，finally exact close handle |
| `.../input/action/InputActionQueue.java` | 新增 `submitRemoteExclusiveAndWaitDetailed(...)`，参数为 callback、wall-clock deadline、stable pause token、per-checkpoint safety supplier、one-shot workerAdmission；不接受 actions，不启用 pause deadline compensation |
| `.../input/action/InputActionRequest.java` | 新增 detailed exclusive callback constructor；沿用现有 `externalSafetyReason` / `workerAdmission`，`excludePauseFromDeadline=false`；无 registry/command 类型依赖 |
| `.../input/action/InputActionScope.java` | `checkpoint()` 在标准 pause wait 返回后调用 current request 的 `checkDetailedSafety("exclusive-callback")`，再允许下一 direct input；legacy request 无 detailed gate 时行为不变 |

明确 **No Modify**：

- `RemoteTaskRunLifecycleService.java`：现有 `resume -> applyConfirmed -> registry.applyConfirmedBinding` 就是 production caller；
- `TaskPauseToken.java`：复用同一个稳定 token；
- `InputActionWorker.java`：复用现有 one-shot `admitWorkerStart` 与 exclusive callback 分支；
- `SummonSkillService.java`：复用 HEAD worker-thread direct path；
- `UICleanerService.java`：调用现有方法并忽略 boolean，不改实现；
- `TaskMaintenanceService.java` 与任何 business host/caller；
- 两仓 schema/resources/tests、Maven model。

### R5. 唯一最小实施波次 v2

仍是一个不可拆分、默认 dormant 的 atomic cross-repo wave，顺序如下：

1. closed whole-pass protocol/value/digest 分支；
2. Cloud retained owner + R-X0 delta + pre-H prepared interaction generation + K-embedded publication；
3. DHXY registry-entry continuation + detailed exclusive callback + exact post-pass matrix；
4. typed broker/handler/final-consumption 闭环接线。

四组必须在同一波次完成后才算“真实 capability”，但本波 **不** 修改/启用 business host/caller，不切
`TaskMaintenanceService`，不启动 Task/poller/UI/capture/input。不得先落一个永远
`NOT_EXECUTED` 的 placeholder，也不得先放宽 old command revision。

implementation owner 后续只可占用 R4 表列出的文件；开始前必须由父级确认这些 shared protocol/input/registry 文件已
从其他 worker 写集中释放。本 D2 只发布设计，不取得未来 Java 写权。

### R6. A / B / Y 与冻结面

- 外部 A Navigation config/workflow：零文件交叉；
- 外部 B TeamReturn：零文件交叉；
- 内部 Y `PlayerFirstAidDecision` / PlayerState / first-aid：零业务文件交叉；
- 当前若 A/B/Y 正占用共享 protocol 文件，R-X1 implementation 必须等待父级串行调度，Z 不合并或覆盖；
- 本轮两仓 Java/Maven/schema/resources/tests、host/caller 全部未写，所有 dirty/untracked 原样保护。

### R7. Repair Self-QA（不构成 Approved）

- P0: `0`
- P1: `0`（Worker Z 自评：三项 parent P1 均有 exact replacement）
- P2: `0`
- P1-1 closure：确定 `EXECUTED/NOT_EXECUTED/STOPPED` + no-owner 全部 session 外 cleanup 一次；
  UNKNOWN/possible-owner 不越界；cleaner boolean 不影响 result/cooldown。
- P1-2 closure：existing production caller
  `RemoteTaskRunLifecycleService.applyConfirmed -> RemoteTaskRunRegistry.applyConfirmedBinding/publishTransition`
  在 stable token wake 前发布 same-entry continuation；old command re-entry 永久 stale。
- P1-3 closure：interaction projection 嵌入 prepared K runtime；所有校验/构造在 H 前；H 后无 authority commit，
  只做 H handle field assignment + K `AtomicReference.set`。
- 交付复核：仅 append 固定日志；零 Java、零 Maven/tests、零 runtime、零 Git mutation。
- reviewer 结论：**未给出**。Worker 自审不算 Approved，等待父级独立 `DESIGN APPROVED` 或 `BLOCKED`。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #2 - BLOCKED / Repair #2 Published - 2026-07-13T13:48:00-04:00

父级复审 D2。R1 已恢复 HEAD 的 deterministic no-owner post-pass UICleaner matrix，并在 cleaner 异常时保持机械 UNKNOWN；
R3 已把 interaction projection 嵌入 K prepared runtime，所有校验/分配前移到 H 前，H 成功后只剩既有 handle field assignment
与单次 K publication。这两项通过，不得重开。

整体仍 **BLOCKED，P0=0/P1=1/P2=0**：

1. **P1：local in-flight handle 没有 ACTIVE -> PAUSED snapshot publication，导致 resume predecessor 条件不可达。**
   R2.4 要求 PAUSED -> ACTIVE 时 `handle.currentSnapshot` 必须精确对应 `previous` PAUSED registration，再发布 successor；
   但 R2/R4 只设计了 resume successor，未规定现有 `RemoteTaskRunRegistry.publishTransition(ACTIVE, PAUSED)` 如何把 live
   `CALLBACK_ADMITTED` handle 从 ACTIVE snapshot 原子更新为 PAUSED。当前源码 :655-662 仅 request pause、清 readiness、写
   registration；若 handle 仍是 ACTIVE，resume 的 exact-predecessor 校验必失败；若跳过校验，则可把未确认的 stale callback
   续活。返修必须给同一 `mutationLock` 内的双向状态表与顺序：ACTIVE -> PAUSED 预构造 paused continuation，原子发布
   registration + handle snapshot 后请求 stable token pause；PAUSED -> ACTIVE 预构造 successor/readiness，原子发布后 token
   resume 最后；STOPPING/terminal/replacement 必须 invalidate 同一 handle，下一 direct side effect 前 fail closed。

### 当前任务 `W-SS-X1-D3`

同一 Internal Worker Z 只在本日志真实 EOF 追加 `CLAIMED` 与 Design Repair #2 Delta，关闭上述唯一 P1；两仓 Java/Maven/
schema/resources/tests、A/B/Y、host/caller 全冻结。只补 local handle 的 ACTIVE/PAUSED/ACTIVE/terminal exact transition 表、
publisher/consumer 顺序和 R4 对 `RemoteTaskRunRegistry` 的对应方法表；R1 cleanup、R3 pre-H/no-fail publication、typed whole-pass、
caps/UNKNOWN/no-retry/no-TTL 结论全部冻结。Worker QA 不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

## CLAIMED

- task: `W-SS-X1-D3`
- claimedAt: `2026-07-13T13:47:18-04:00`
- uniqueWriteSet: `docs/superpowers/plans/reports/2026-07-13-cloud-summon-skill-exclusive-owner-worker-z.md`（仅该 append-only 日志）

## W-SS-X1-D3 Design Repair #2 Delta

- repairedAt: `2026-07-13T13:49:11-04:00`
- status: `REPAIR READY FOR PARENT REVIEW`
- role: Internal Worker Z（不是 reviewer）
- scope: 只关闭 Parent Design Review #2 的唯一 P1

本 Delta 只替换 D2 的 R2.4 与 R4.4 中 `RemoteTaskRunRegistry` 行。D2 R1 cleanup matrix、R3
pre-H/no-fail publication，以及 single typed whole-pass、hard caps、UNKNOWN/no-retry/no-TTL 全部冻结，不重开。

### D3.1 同一 handle 的 snapshot 模型

`RemoteTaskRunRegistry.RegistryEntry` 仍是唯一 local registration owner。entry 增加至多一个
`inFlightExclusiveHandle` 引用；整个 ACTIVE -> PAUSED -> ACTIVE 周期始终是**同一个**
`InFlightExclusiveHandle` 对象，只替换该 handle 内的 immutable `currentSnapshot`。

snapshot 是 package-private sealed nested value，状态固定为：

| snapshot | 含义 | 是否可允许下一 direct side effect |
|---|---|---|
| `QueuedActiveSnapshot` | exact old command 已通过 handler admission，但 worker one-shot admission 尚未成功 | 只允许 worker admission 重验 original command；不具备 resume continuation 权限 |
| `CallbackActiveSnapshot` | worker admission 已在 exact ACTIVE registration 上成功；same callback 已取得 continuation 权限 | exact current check 成功后允许 |
| `CallbackPausedSnapshot` | 同一 admitted callback 已收到 cloud-confirmed PAUSED predecessor | 不允许；等待 stable pause token |
| `InvalidatedSnapshot` | STOPPING/terminal/entry removal/replacement/stale predecessor | 永久不允许 |
| `ClosedSnapshot` | callback 已退出且 local owner proof 已冻结，handler 正常关闭 handle | 不允许 |

每个 snapshot 固定携带 owner identity、`RegistryEntry` identity、`entryGeneration`、同一
`TaskPauseToken` identity、stable scope/clientSession/taskRun/taskType/window/stopEpoch/sessionId，以及自己的
registration object identity、runRevision 和单调 `localTransitionGeneration`。PAUSED snapshot 另带
`pausedFromActiveRevision`；resumed ACTIVE successor 另带 `resumedFromPausedRevision` 与 exact readiness slot
generation。

local `requestPause(...)` 只请求 stable token，不铸造 `CallbackPausedSnapshot`。只有 cloud-confirmed binding
进入现有 `applyConfirmedBinding(...) -> publishTransition(ACTIVE, PAUSED)` 后，PAUSED snapshot 才成为
authoritative；未确认 pause 不能推进 continuation generation。

### D3.2 mutationLock 内双向 transition 状态表

所有下表的 predecessor 校验、snapshot/readiness/invalidation 构造和赋值均持有现有
`RemoteTaskRunRegistry.mutationLock`。handle-only mismatch 不阻塞合法 lifecycle publication；它把 handle
变为 `InvalidatedSnapshot`，让 callback fail closed。

| Registry transition / event | handle 前态 | 预构造 | 同一 handle 后态 | token 最后动作 |
|---|---|---|---|---|
| handler `openInFlightExclusive` on exact ACTIVE | 无 | `QueuedActiveSnapshot(ACTIVE rN)` | `QueuedActiveSnapshot` | 无 |
| worker `admitInFlightExclusive`，original command 仍 exact ACTIVE rN | `QueuedActiveSnapshot(rN)` | `CallbackActiveSnapshot(rN)` | `CallbackActiveSnapshot` | 无 |
| worker admission 前 registration 已变化 | `QueuedActiveSnapshot` | `InvalidatedSnapshot(STALE_BEFORE_ADMISSION)` | `InvalidatedSnapshot`，entry slot 清空 | 无 |
| confirmed ACTIVE rN -> PAUSED rN+1 | `CallbackActiveSnapshot(rN)` 且 stable tuple/entry/token 全匹配 | `CallbackPausedSnapshot(rN -> rN+1)`；readiness=`null` | **同一 handle** -> `CallbackPausedSnapshot` | `requestPause(...)` 最后 |
| confirmed ACTIVE -> PAUSED，但 handle 仍 queued | `QueuedActiveSnapshot` | `InvalidatedSnapshot(PAUSED_BEFORE_ADMISSION)` | invalidated；旧 command 不能续活 | `requestPause(...)` 最后 |
| confirmed ACTIVE -> PAUSED，handle predecessor 不 exact | 任意不匹配 live snapshot | `InvalidatedSnapshot(STALE_ACTIVE_PREDECESSOR)` | invalidated | `requestPause(...)` 最后 |
| duplicate confirmed PAUSED same object/revision | exact `CallbackPausedSnapshot` | 无新 generation | 保持同一 paused snapshot | idempotent `requestPause(...)` 最后 |
| confirmed PAUSED rP -> ACTIVE rA | exact `CallbackPausedSnapshot(rP)` | `CallbackActiveSnapshot(rA, resumedFrom=rP)` + `PendingExecutorReadiness` + next slot generation | **同一 handle** -> resumed `CallbackActiveSnapshot` | `resume()` 最后 |
| confirmed PAUSED -> ACTIVE，paused predecessor/session/entry/token 任一不 exact | 任意不匹配 snapshot | active registration/readiness 仍可发布；另构造 `InvalidatedSnapshot(STALE_PAUSED_PREDECESSOR)` | invalidated，绝不宽放 | `resume()` 最后 |
| ACTIVE/PAUSED -> STOPPING | 任一 live snapshot | target STOPPING registration + `InvalidatedSnapshot(STOPPING)`；readiness=`null` | invalidated，entry slot 清空 | `resume()` 最后唤醒 |
| ACTIVE/PAUSED/STOPPING -> STOPPED/COMPLETED | 任一 live snapshot | terminal registration + `InvalidatedSnapshot(TERMINAL)`；readiness=`null` | invalidated，entry slot 清空 | `resume()` 最后唤醒 |
| `releaseTerminal` / `unregister` | 已 invalidated；若异常仍 live 则防御性 invalidate | `InvalidatedSnapshot(ENTRY_REMOVED)` | invalidated；old entry/handle 永不复用 | remove/decrement 后 `resume()` 最后 |
| terminal removal 后同 taskRunId 新 register | old handle 属于 old entryGeneration | 新 `RegistryEntry`、新 token、无 handle | old handle 永久 invalidated；不能 attach 到 replacement | old token 已在 removal 最后 resume |
| callback normal close | exact queued/active handle | `ClosedSnapshot` | closed，entry slot 清空 | 无 |

`advanceStoppingProgress(...)` 只允许现有 STOPPING registration 前进 revision；entry 此时必须已无 live handle。
若发现旁路遗留 handle，先发布 `InvalidatedSnapshot(STOPPING_PROGRESS)` 并清空 slot，再写 STOPPING revision；
不得恢复 continuation。

### D3.3 ACTIVE -> PAUSED 的预构造与 publication 顺序

`publishTransition(entry, previousActive, currentPaused)` 在 `mutationLock` 内先做完所有可能失败工作：

1. 校验 lifecycle 本身是合法、严格前进的 `ACTIVE -> PAUSED`；
2. 捕获 `entry.inFlightExclusiveHandle`；
3. 对 exact `CallbackActiveSnapshot` 预构造 immutable
   `CallbackPausedSnapshot(previousActive, currentPaused, nextLocalTransitionGeneration)`；
4. 对 queued/mismatch handle 预构造对应 `InvalidatedSnapshot`；
5. 将 prepared readiness 固定为 `null`，并预构造完整 `PreparedEntryTransition`；
6. 完成所有 text/identity/shape 校验与 `Math.incrementExact`，此时尚未改 registration、snapshot 或 token。

prepare 成功后，只做以下 publication：

```text
entry.pendingReadiness = null
entry.registration = currentPaused
handle.currentSnapshot = preparedPausedOrInvalidatedSnapshot
if invalidated: entry.inFlightExclusiveHandle = null
entry.pauseToken.requestPause("cloud-confirmed pause revision=" + currentPaused.runRevision)  // 最后
```

registration 与 handle snapshot 在同一 `mutationLock` critical section 内先发布，stable token 的
`requestPause(...)` 最后执行。它即使此前已由 local pause request 置位也只是幂等保持；不换 token。
side-effect consumer 同样必须取得 `mutationLock`，因此在 lock 释放前看不到半组状态。

若 token request 出现非预期异常，registration/handle 已是 PAUSED/invalidated；consumer 的 typed current check
仍拒绝 direct input，不会把 callback 误放行。不得 rollback 到 ACTIVE。

### D3.4 PAUSED -> ACTIVE successor/readiness 的预构造与 publication 顺序

`publishTransition(entry, previousPaused, currentActive)` 在任何赋值/唤醒前预构造：

1. `preparedSlotGeneration = Math.incrementExact(entry.nextSlotGeneration)`；
2. existing `PendingExecutorReadiness(preparedSlotGeneration, pausedRevision, activeRevision)`；
3. exact paused predecessor 对应的 immutable resumed `CallbackActiveSnapshot`，或 fail-closed
   `InvalidatedSnapshot`；
4. 完整 `PreparedEntryTransition`。

prepare 成功后，只做：

```text
entry.nextSlotGeneration = preparedSlotGeneration
entry.registration = currentActive
entry.pendingReadiness = preparedReadiness
handle.currentSnapshot = preparedActiveSuccessorOrInvalidatedSnapshot
if invalidated: entry.inFlightExclusiveHandle = null
entry.pauseToken.resume()  // 最后
```

`resume()` 是最后一步。pause wait 被唤醒时，ACTIVE registration、readiness 和 same-handle successor 已经共同
发布；若 predecessor 不 exact，则唤醒后看到的是 invalidation，不是宽松 ACTIVE。

### D3.5 STOPPING / terminal / replacement invalidation 顺序

现有 `beginStop(...)` 是 `publishTransition(...)` 之外的 registration writer，因此必须使用相同 publication
contract。`beginStop(...)` 和 `publishTransition(... -> STOPPING/terminal)` 都先预构造 target registration、
`InvalidatedSnapshot` 与 readiness=`null`，然后在 `mutationLock` 内：

```text
entry.pendingReadiness = null
entry.registration = targetStoppingOrTerminal
handle.currentSnapshot = preparedInvalidatedSnapshot
entry.inFlightExclusiveHandle = null
entry.pauseToken.resume()  // 最后
```

`releaseTerminal(...)` / `unregister(...)` 在 remove 前再次防御性 invalidate，赋值 invalidated snapshot、清 slot，
再 `registrations.remove(...)` / decrement usage，最后 `pauseToken.resume()`。后续同 taskRunId 的
`register(...)` 必须 new `RegistryEntry`、递增 `entryGeneration` 并 new `TaskPauseToken`；old handle 的 owner
entry identity、generation 和 token identity 三重不匹配，永久 stale。

这些 terminal/replacement 路径不等待 callback 自己“接受” invalidation。publisher 直接写 handle snapshot；
callback 下一 checkpoint 只消费。

### D3.6 InputActionScope consumer side-effect fence

publisher 是 `RemoteTaskRunRegistry`，consumer 固定为已设计的 per-checkpoint supplier：

```text
InputActionScope.checkpoint()
  -> interruption/cancel/stop check
  -> stable TaskPauseToken.waitIfPaused(...)（若已 request）
  -> InputActionRequest.checkDetailedSafety("exclusive-callback")
  -> LocalRemoteGameCommandHandler closure
  -> RemoteTaskRunRegistry.checkInFlightExclusive(handle) under mutationLock
  -> only exact CallbackActiveSnapshot may return CLEAR
  -> next HEAD direct InputProvider side effect
```

`checkInFlightExclusive(handle)` 在同一 `mutationLock` 内必须同时证明：

- `registrations.get(taskRunId)` 仍是 handle 的 exact entry；
- `entry.entryGeneration`、`entry.inFlightExclusiveHandle` 与 handle identity 全相同；
- snapshot 是 `CallbackActiveSnapshot`，且 `entry.registration == snapshot.registration`；
- current registration 为 ACTIVE；
- scope/clientSession/taskRun/taskType/window/stopEpoch/sessionId/token identity 全相同。

`CallbackPausedSnapshot` 返回 typed PAUSED；`InvalidatedSnapshot`/`ClosedSnapshot`、slot 已清、entry removed/replaced、
STOPPING/terminal 均返回 typed stop/mismatch。任何非 CLEAR 都在下一 direct side effect 前令 request fail closed。

pause publication 与 token request 之间即便存在方法内微小窗口，consumer 要拿同一个 `mutationLock`，只能在
`requestPause(...)` 执行并释放 lock 后进入。resume consumer 则只能在 `resume()` 唤醒后看到已发布 successor。
original `RemoteGameCommand.runRevision` 从不更新；handler re-entry 仍永久 stale。

### D3.7 R4 `RemoteTaskRunRegistry` exact methods 修订

D2 R4.4 中 `RemoteTaskRunRegistry.java` 一行替换为：

| 文件 | exact 修改点 |
|---|---|
| `DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunRegistry.java` | package-private `openInFlightExclusive(CommandAdmissionSnapshot, RemoteGameCommand, String)`；`admitInFlightExclusive(InFlightExclusiveHandle, RemoteGameCommand)`；`checkInFlightExclusive(InFlightExclusiveHandle)`；`closeInFlightExclusive(InFlightExclusiveHandle)`。nested sealed `ContinuationSnapshot`（`QueuedActiveSnapshot` / `CallbackActiveSnapshot` / `CallbackPausedSnapshot` / `InvalidatedSnapshot` / `ClosedSnapshot`）、non-mintable `InFlightExclusiveHandle`、private immutable `PreparedEntryTransition`。现有 `publishTransition(...)` 完整处理 ACTIVE->PAUSED、PAUSED->ACTIVE、STOPPING/terminal；现有 `beginStop(...)`、`advanceStoppingProgress(...)`、`releaseTerminal(...)`、`unregister(...)` 共享同一 invalidation/publication contract。所有 prepare 在赋值前；pause request/resume 永远最后 |

`admitInFlightExclusive(...)` / `checkInFlightExclusive(...)` 返回 package-private typed
`InFlightExclusiveCheck`（`CLEAR`、`PAUSED`、`STOPPED`、`MISMATCH`），由
`LocalRemoteGameCommandHandler` closure 映射到现有 `InputActionSafetyReason`；`InputActionRequest` 不依赖
registry/command 类型。

不新增文件，不修改 `RemoteTaskRunLifecycleService`、`TaskPauseToken`、`InputActionWorker`、host/caller。
D2 R5 唯一最小实施波次 v2 不变；其第 3 步
“DHXY registry-entry continuation”现在必须一次实现本 Delta 的双向 publication 与所有 invalidation 旁路，
不得只实现 resume successor。

### D3.8 冻结面与 Self-QA（不构成 Approved）

- R1 cleanup：冻结，通过项不重开；
- R3 pre-H/no-fail H/K publication：冻结，通过项不重开；
- typed whole-pass、stable identity、hard caps、UNKNOWN/no-retry/no-TTL：冻结；
- 两仓 Java/Maven/schema/resources/tests、A/B/Y、host/caller：本轮全部未写；
- P0: `0`
- P1: `0`（Worker Z 自评：唯一 P1 已给出双向可达 publication、terminal/replacement invalidation）
- P2: `0`
- reviewer 结论：**未给出**。Worker 自审不算 Approved，现停止并等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #3 - DESIGN APPROVED - 2026-07-13T13:55:00-04:00（EOF 权威块）

说明：同标题 review 曾因 append 锚点过宽误插入旧历史位置；**仅本真实 EOF 块是当前权威结论**。父级对照
`RemoteTaskRunRegistry.publishTransition`、`beginStop`、`advanceStoppingProgress`、`releaseTerminal`、`unregister`、
stable `TaskPauseToken` 与 `InputActionScope` 当前调用链复审 D3。唯一 P1 已闭合：同一 non-mintable handle 现在具有可达的
`QueuedActive -> CallbackActive -> CallbackPaused -> CallbackActive` 双向 publication；ACTIVE->PAUSED 先在
`mutationLock` 内共同发布 registration/paused-or-invalidated snapshot，再把 `requestPause` 放最后；PAUSED->ACTIVE
先预构造 successor/readiness/generation 并共同发布，再把 `resume` 放最后。STOPPING、terminal、entry
removal/replacement 均主动 invalidate 并清 slot，下一 direct side effect 必须经 detailed safety supplier 在同一 registry
lock 下验证 exact entry/generation/registration/token/snapshot；original command revision 始终不变，不能因 resume 复活。

结论：**DESIGN APPROVED，P0/P1/P2=0**。绑定实施条件：`closeInFlightExclusive(handle)` 必须对同 owner 的
Queued/Active/Paused/Invalidated/Closed 五种状态均 total + idempotent；slot 仍指向该 handle 时精确清空，slot 已由
terminal/replacement 清空或 entry 已移除时只完成/保留 handle 的 closed terminal marker，不得抛异常覆盖 handler finally
中的 typed outcome，也不得清理另一个 handle。foreign owner/entry handle 仍 fail closed。该条件不增加文件或新状态权威，
无需再交文字返修。

R5 仍须作为一个 dormant 双仓原子实施波，由父级确认 shared protocol/input/registry 写集释放后另行发单；落码前 Java、
schema、host/caller 继续冻结。Worker self-QA 不计批准，本条为唯一父级结论。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Task - W-SS-X1-IMP1 - 2026-07-13T14:01:00-04:00（EOF 权威块）

说明：同标题任务曾因 append 锚点过宽误插入旧历史位置；**仅本真实 EOF 块是当前任务权威**。Internal Worker Z 的
R1-R5 + D3 已父级 DESIGN APPROVED。立即在本日志真实 EOF 追加 `CLAIMED` 后，按 R4.1-R4.4 的**完整唯一文件表**实施
一个 dormant 双仓原子波；该表列出的 Cloud New/Modify、DHXY New/Modify 与本日志是你的唯一写集，未列文件全部冻结。
尤其必须一次闭合：

- Cloud retained whole-pass authority/capability、closed request/outcome/operation、stable semantic action identity、Full R0
  duplicate/late/final-consume，interaction projection 在 H 前预构造并随 K runtime 单点 publication；
- DHXY exact payload/codec/digest/ledger、registry-entry non-mintable continuation、ACTIVE->PAUSED->ACTIVE 双向 snapshot、
  STOPPING/terminal/replacement invalidation、detailed exclusive callback 与 per-checkpoint side-effect fence；
- Parent Review #3 绑定条件：`closeInFlightExclusive` 对同 owner 的 Queued/Active/Paused/Invalidated/Closed total + idempotent，
  terminal 已清 slot/entry 时不得在 finally 抛错覆盖 typed outcome；foreign handle 仍 fail closed；
- R1 deterministic post-pass UICleaner matrix：只在已证明 no-owner 的 terminal 结果执行一次，UNKNOWN/可能 owner 不清理；
  host/caller 保持 dormant，不启用 Task，不改 HEAD `SummonSkillService` / `UICleanerService` 业务实现。

你不是唯一在代码库工作的 Worker：所有 R4 已存在 dirty/untracked 都是受保护在途基线，须在其上增量实现，禁止回滚、覆盖、
格式化无关文件或 Git mutation。不得新增/恢复 tests，不启动 application/server/host/Task/poller/UI/capture/input。完成后运行
Cloud `mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，把精确文件、wire/digest 对称性、状态时序、
构建结果和 self-QA 追加回本日志；Worker self-QA 不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

## CLAIMED

- task: `W-SS-X1-IMP1`
- claimedAt: `2026-07-13T14:00:35-04:00`
- authorityBlock: `Parent Implementation Task - W-SS-X1-IMP1 - 2026-07-13T14:01:00-04:00（EOF 权威块）`
- uniqueWriteSet: 本日志 + Parent-approved `R4.1-R4.4` 完整唯一文件表；未列文件全部冻结

## IMPLEMENTATION BLOCKED - R4 complete-file-table compile gap - 2026-07-13T14:24:07-04:00（EOF 权威块）

说明：同内容阻塞块曾因 append 锚点过宽误插入历史位置；**仅本真实 EOF 块是当前权威阻塞记录**。

- task: `W-SS-X1-IMP1`
- blocking severity: `P1`（唯一文件表与强制 `clean package` 不能同时满足）
- exact evidence: R4.2 要求在 `RemoteOperation.java` 新增 enum constant
  `SUMMON_SKILL_WHOLE_PASS`；但未列入 R4.2、因而明确冻结的
  `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
  在 `toTypedOutcome()` 使用无 `default` 的穷尽 enum switch expression（当前约第 108-114 行），只覆盖现有五个
  operation。对同一 Maven main source set 新增 enum constant 后，Java 编译期必然要求该 switch 覆盖新 constant；
  `RemoteGameCommandBroker` 内截获新 operation 只能改变运行路径，不能消除该冻结源码的编译期穷尽性错误。
- required parent decision: 将上述 `RemoteCommandOutcomeEnvelope.java` 精确加入 R4.2 唯一写集，以只增加
  `SUMMON_SKILL_WHOLE_PASS` closed payload decode 分支；或发布不新增 enum constant 的替代协议设计。未获明确扩权前
  Worker Z 不越界修改。
- protected state: 未运行 Maven/runtime/Git；未修改任何 Cloud Java。已在批准的 DHXY R4.3/R4.4 文件内增量落下
  payload/operation/codec/digest/ledger、registry continuation、detailed callback 与 handler 的在途实现；未触碰
  host/caller、tests、schema/resources 或任何未列文件。该在途波尚未构建、尚未交付，不能视为完成或 Approved。
- self-review: 本阻塞判断不是 reviewer approval；等待父级修订 EOF 权威实施块/唯一文件表后继续同一 atomic wave。

## Parent Scope Repair #1 / Resume - W-SS-X1-IMP1-R1 - 2026-07-13T14:32:00-04:00（真实 EOF 权威块）

说明：同标题父级块曾因 append 锚点过宽误插入历史位置；**仅本真实 EOF 块是当前权威扩权与恢复指令**。
父级复核 Worker Z 的阻塞判断成立。`RemoteOperation` 新增第六个 enum 后，
`RemoteCommandOutcomeEnvelope.toTypedOutcome()` 的无 `default` switch 必须同步闭合；这不是可拆开的后续优化，而是同一
closed protocol 原子波的遗漏文件。现对 R4.2 唯一文件表做**唯一一项扩权**：

- 新增 Modify：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteCommandOutcomeEnvelope.java`。
- 精确修改面：在 `toTypedOutcome()` 为 `SUMMON_SKILL_WHOLE_PASS` 增加穷尽分支，并按本波新建的
  `SummonSkillWholePassOutcome` closed shape 严格解码 payload；必须拒绝额外/缺失字段及不符合 mechanical-status、
  cleanup、callback-started、owner-released null/证明矩阵的 payload，然后照既有路径执行 outcome digest verify。
- 禁止面：不加 `default`，不把 payload/raw envelope 暴露给 business，不放宽其它五个 operation，不新增第二 codec/
  authority，不改 Maven/schema/tests/host/caller，不改变已批准 R1-R5 与 D3 状态机。

除上述一文件外，原 `W-SS-X1-IMP1` 唯一写集、构建门与冻结面全部不变。Worker Z 立即继续同一个原子实施波，完成后追加
`Implementation Repair #1`，列出两仓精确文件、wire/digest 对称性、continuation publication 与双构建结果。当前结论仍是
`BLOCKED`，直到完整源码和 Cloud `mvn -q clean package`（不可 skip）、DHXY `mvn -q -DskipTests compile` 均经父级复验。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Implementation Repair #1 - W-SS-X1-IMP1-R1 - 2026-07-13T15:20:22-04:00（真实 EOF 权威交付块）

说明：同内容交付块曾因同文 append 锚点命中历史副本而误插旧位置；**仅本物理 EOF 块是当前 Implementation Repair #1 权威交付**。历史内容保持不删不改。

- status: `IMPLEMENTED / READY FOR PARENT REVIEW`
- role: Internal Worker Z（只做 implementation / self-QA，不是 reviewer）
- authority: 仅执行真实 EOF `Parent Scope Repair #1 / Resume - W-SS-X1-IMP1-R1 - 2026-07-13T14:32:00-04:00`；原 R4.1-R4.4 唯一写集只增加父级扩权的 `RemoteCommandOutcomeEnvelope.java`
- dormant boundary: 未新增 tests，未启动 application/server/host/Task/poller/UI/capture/input，未修改 schema/resources/Maven model/host/caller/A/B/Y，未做 Git mutation

### 精确实施文件

Cloud New：

- `CloudTaskExclusiveInteractionAuthority.java`
- `CloudSummonSkillWholePassCapability.java`
- `SummonSkillWholePassRequest.java`
- `SummonSkillWholePassOutcome.java`

Cloud Modify：

- `CloudTaskExclusiveInteractionState.java`
- `CloudTaskRunAuthorityAssembly.java`
- `CloudTaskRunCurrentContextSlot.java`
- `CloudTaskServiceExecutionContext.java`
- `CloudTaskServicePort.java`
- `CloudTaskRunRetainedLifecycleActivationAdapter.java`
- `CloudTaskRetainedActionState.java`
- `CloudTaskRunActionLedger.java`
- `RemoteOperation.java`
- `RemoteRequest.java`
- `RemoteOutcome.java`
- `RemoteGameClientPort.java`
- `CloudTaskRunCommandExecutor.java`
- `RemoteGameCommandBroker.java`
- `RemoteFinalConsumptionCoordinator.java`
- `RemoteProtocolDigests.java`
- `RemoteCommandOutcomeEnvelope.java`（Scope Repair #1 唯一扩权）

DHXY New：

- `RemoteSummonSkillWholePassCommandPayload.java`
- `RemoteSummonSkillWholePassOutcomePayload.java`

DHXY Modify：

- `RemoteGameOperation.java`
- `RemoteOperationPayloadCodec.java`
- `RemoteProtocolDigests.java`
- `RemoteOperationLedger.java`
- `RemoteTaskRunRegistry.java`
- `LocalRemoteGameCommandHandler.java`
- `InputActionQueue.java`
- `InputActionRequest.java`
- `InputActionScope.java`

除上述文件与本固定日志外无写入。

### Wire / digest / final-consumption parity

- 两端 request 均为 closed 六字段：`expectedSkillCount`、`trustExpectedSkillCount`、`startSlotIndex`、`skipUltimateCornerCheck`、`exclusiveSessionId`、`bindingGeneration`；nullable/counter/canonical-text 条件对称。
- 两端 outcome 均为 closed 五字段：`mechanicalStatus`、`cleanupResult`、`callbackStarted`、`ownerNeverAcquired`、`ownerReleased`；cleanup 为 exact 九字段，`EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` 与 common execution state、owner proof/null matrix 对称。
- Cloud `RemoteCommandOutcomeEnvelope.toTypedOutcome()` 的 enum switch 无 `default`，新增 whole-pass 分支先拒绝额外/缺失 payload 与 cleanup 字段，再构造 typed outcome，最后走既有 outcome digest verify；其它五个 operation 未放宽。
- Cloud/DHXY digest 都从 typed closed shape 构造、忽略协议允许的 null optional request 字段并做同一 canonical JSON SHA-256；semantic address/action identity、duplicate、late resolution 与 final-consumed receipt 保持同一 retained request/outcome digest。
- `UNKNOWN` 保留 live owner/action/ledger，不消费 final、不续 TTL、不释放后重发；同 identity 再读只等待 broker retained late resolution，`awaitRetainedResolution(...)` 不 requeue/redispatch，因此没有第二次 physical whole-pass。

### Authority / continuation / cleanup 时序

- Cloud owner 以 stable run key + session/binding fence 保留唯一 whole-pass；hard caps 为 `1/run`、`64/tenant`、`1000/global`。initial/resume 的 interaction projection、successor runtime 与 K state 全在 H 前预构造；H 成功后只执行 `nextRuntime.stateActivationHandle = nextStateHandle` 与唯一 K `publication.set(preparedNextState)`，没有可失败 interaction commit。
- DHXY `RemoteTaskRunRegistry` 在同一 `mutationLock` 内对同一 non-mintable handle 发布 `QueuedActive -> CallbackActive -> CallbackPaused -> CallbackActive`；pause/resume successor 的 registration、readiness、snapshot 全先构造，赋值完成后才最后调用稳定 `TaskPauseToken.requestPause(...)` / `resume()`。
- same in-flight callback resume 后的 per-checkpoint fence 读取 successor ACTIVE registration/revision；旧 original command 只用于首次 worker admission，不能再次 admission，故旧 command 永久 stale。STOPPING、terminal、replacement 先发布 `InvalidatedSnapshot` 并 detach，再 resume token；`closeInFlightExclusive(...)` 对 same owner 的 queued/active/paused/invalidated/closed total + idempotent，foreign handle fail closed。
- `LocalRemoteGameCommandHandler` 只在 detailed exclusive callback 内调用 HEAD `SummonSkillService.cleanSummonSkillsOnce(...)`；direct callback 每个后续物理 side effect 前由 `InputActionScope.checkpoint()` 执行 handle fence。callback/owner 已确定终止后，才在 input queue 外按 HEAD 等价 matrix 至多一次调用现有 `UICleanerService.cleanLightweightInterruptions(...)`；`UNKNOWN` 或 owner 仍可能存活时不清理。

### 双构建证据

- Cloud：在确认 AA 的 Cloud Maven 进程已退出后，从 `D:\mavenProject\dhxy-cloud-brain` 独立执行 `mvn -q clean package`，`exit 0`，耗时 `72.0s`；未与 AA 的 clean 并发。
- DHXY：从 `D:\mavenProject\DHXY` 执行 `mvn -q -DskipTests compile`，`exit 0`，耗时 `3.1s`。
- 两条命令均为本交付前的 fresh full gate；未运行 tests/runtime。

### Self-QA（不构成 Approved）

- cross-repo closed wire/digest、R0 duplicate/late/final-consume、R1 post-pass matrix、R3 H/K publication、R4 continuation/invalidation 已做方法级 parity 自审。
- P0: `0`
- P1: `0`
- P2: `0`
- Worker Z 自审不构成父级 `Approved`；现停止修改，等待父级复审。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Implementation Review #1B - BLOCKED / Repair #2 Reissued - 2026-07-13T15:40:00-04:00（真实物理 EOF 唯一权威块）

说明：同一返修结论先前两次因宽锚点误插入历史位置。为保护 append-only，不删除、不改写历史副本；**只有本物理 EOF
副本是当前权威返修指令**。父级复核 Worker Z 的双仓交付后结论仍为 **BLOCKED，P0=0/P1=2/P2=0**：

1. **P1：合法 `cleanupResult.message=null` 的两端 outcome digest 不一致。** DHXY transport 把 null/blank 规范为
   `null`，nested null 会进入本地 digest；Cloud transport 把同值规范为 `""`，Cloud digest 写入空串。同一合法 outcome 会在
   Cloud digest verify 被拒绝。只允许把 DHXY transport cleanup message 规范为与 Cloud 相同的 canonical empty string；
   不得改变 `SummonSkillCleanupResult` 业务语义或放宽九字段 closed shape。
2. **P1：ACTIVE 先得到 UNKNOWN、随后 pause/resume 时 interaction state 未续代。** 当前 unresolved state 若尚未 parked，
   resume 不 handoff revision/generation，却把旧 state 发布进 next ACTIVE projection。pause 线性化必须执行
   restore -> PARKED_PAUSED -> re-hold unresolved，resume 再走既有 handoff，使 revision/generation 精确推进；原 retained
   request/action bytes 完全不变，禁止 requeue、redispatch、renew、TTL、takeover 或把 UNKNOWN 压成 terminal。

### 当前任务 `W-SS-X1-IMP1-R2`

原 Internal Worker Z 继续返修，唯一代码写集收窄为：

- Cloud Modify：`CloudTaskExclusiveInteractionAuthority.java`；
- DHXY Modify：`RemoteSummonSkillWholePassOutcomePayload.java`；
- 本 append-only 日志。

其余 R4 文件、业务 `SummonSkillService`/`UICleanerService`、schema/resources/tests/host/caller 全冻结。完成后运行 Cloud
`mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，并在本日志真实 EOF 追加
`Implementation Repair #2`：精确 diff、null/blank/nonblank 三类 message 规范化证据、UNKNOWN-before-pause 时序、双构建结果。
Worker self-QA 不构成父级批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Implementation Repair #2 - W-SS-X1-IMP1-R2 - 2026-07-13T15:43:32-04:00（真实物理 EOF 交付块）

- status: `REPAIRED / READY FOR PARENT REVIEW`
- role: Internal Worker Z（implementation / self-QA，不是 reviewer）
- authority: 真实物理 EOF `Parent Implementation Review #1B - BLOCKED / Repair #2 Reissued - 2026-07-13T15:40:00-04:00`
- exact write set: `CloudTaskExclusiveInteractionAuthority.java`、`RemoteSummonSkillWholePassOutcomePayload.java`、本 append-only 日志；其余全部冻结

### 精确 diff

1. Cloud `CloudTaskExclusiveInteractionAuthority.parkPaused(...)`
   - 从 terminal/idempotent early return 中移除未区分的 `UNRESOLVED_FENCE_HELD`。
   - 在同一 `entry.transitionLock` 内：若 unresolved 已有 `parkedFrom` 则幂等返回；否则执行 `restoreResolvedFence(...) -> parkPaused(...) -> holdUnresolvedFence(...)`，最后一次性赋给 `current.generationState.state`。
   - 未修改 `LiveInteraction`、retained action/request、broker、deadline 或 final-consumption 路径。
2. DHXY `RemoteSummonSkillWholePassOutcomePayload.CleanupValue`
   - 唯一表达式从 `message == null || message.isBlank() ? null : message.trim()` 改为 `message == null || message.isBlank() ? "" : message.trim()`。
   - 未修改本地 `SummonSkillCleanupResult`、九字段 closed shape、codec 或 digest 算法。

### Message 三类静态规范化证据

- `null -> ""`：transport payload 固定写出第九个 key `"message":""`，不再产生 nested null。
- blank（`""` 或仅空白）`-> ""`：与 Cloud `RemoteProtocolValidation.optionalText(null) -> ""` 的 canonical empty string 一致。
- nonblank（如 `"  done  "`）`-> "done"`：DHXY wire tree 与 Cloud typed outcome 接收的 wire value 完全相同。
- 因此 local `mergeNonNullFields(...)` 与 Cloud NON_NULL typed mapper 均摘要同一 cleanup object；closed 九字段校验保持严格。

### UNKNOWN-before-pause 时序

- 起点：`UNRESOLVED_FENCE_HELD(unresolvedFrom=ACTIVE|ACQUIRE_BOUND, parkedFrom=null, revision=r, generation=g)`，原 live request/action bytes 保留。
- pause 线性化：`restore` 回原 bound status -> `PARKED_PAUSED(parkedFrom=原 status, r, g)` -> `re-hold UNRESOLVED_FENCE_HELD(parkedFrom=原 status, r, g)`，随后发布给当前 projection。
- resume：既有 `prepareResumeGeneration(...)` 识别 `parkedFrom != null`，执行 restore -> `HANDOFF_BOUND(targetRevision=r')` -> complete handoff；其中 `r' > r` 且 `bindingGeneration=g+1`，再 re-hold unresolved 并装入 successor ACTIVE projection。
- 全链只续代 interaction state；不 requeue、redispatch、renew、增加 TTL/takeover，也不把 UNKNOWN 变成 terminal。

### 双构建证据

- Cloud：`D:\mavenProject\dhxy-cloud-brain`，`mvn -q clean package`，started `2026-07-13T15:42:03-04:00`，finished `2026-07-13T15:43:13-04:00`，`exit 0`，`69.3s`，未 skip。
- DHXY：`D:\mavenProject\DHXY`，`mvn -q -DskipTests compile`，started `2026-07-13T15:43:19-04:00`，finished `2026-07-13T15:43:22-04:00`，`exit 0`，`3.0s`。
- 未新增/运行 tests；未启动 runtime/application/server/host/Task/poller/UI/capture/input；未做 Git mutation。

### Self-QA（不构成 Approved）

- Parent #1B 两项 P1 已按指定路径逐项关闭；P0=`0`、P1=`0`、P2=`0`（Worker 自评）。
- Worker self-QA 不构成父级 `Approved`；现停止修改并等待父级复审。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #2 - SOURCE APPROVED / Final DHXY Build Pending - 2026-07-13T15:53:00-04:00（真实物理 EOF 权威块）

父级逐行复核两文件与两端 canonical digest tree。DHXY transport cleanup message 现将 null/blank 统一为 `""`，nonblank
保持 trim 后的 wire 值；九字段 closed shape 与业务 `SummonSkillCleanupResult` 均未改变。Cloud pause 对尚未 parked 的
`UNRESOLVED_FENCE_HELD` 在同一 transition lock 内先 restore、再 park、再 re-hold；resume 由既有 handoff 把
runRevision 严格推进并把 bindingGeneration +1，retained request/action bytes、UNKNOWN 与 final-consume 均未重铸或降级。

结论：**SOURCE APPROVED，P0=0/P1=0/P2=0**。父级独占 fresh Cloud `mvn -q clean package` 已 `exit 0`，
4 suites/21 tests、0 failures/errors/skipped。DHXY 父级 fresh compile 将在 External B 的同模块单文件 repair 停止写入后合并
复跑，避免用并发写入期结果冒充最终门；仅构建失败才回退本源码结论。Worker Z 到此停止，不再持有写集。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Final Build Review #3 - FINAL APPROVED - 2026-07-13T15:55:00-04:00（真实物理 EOF 权威块）

External B 的同模块 repair 已停止写入并通过父级源码审查。父级随后从 `D:\mavenProject\DHXY` 独占执行
fresh `mvn -q -DskipTests compile`，结果 `exit 0`；结合 Parent Source Review #2 的父级 fresh Cloud
`mvn -q clean package`（4 suites/21 tests、0 failures/errors/skipped），双仓最终构建门完整通过。

结论：`W-SS-X1-IMP1-R2` **FINAL APPROVED，P0=0/P1=0/P2=0**。Worker Z 已关闭；不启动
application/server/host/Task/poller/UI/capture/input，真实运行证据仍属于独立切换验收。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
