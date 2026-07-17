# PRECHECK

**1. 角色、范围与证据口径**

- 角色：CR271 Internal 非绑定 helper，只做 TURN-45B post-44A residual-seed 静态预检；不是 implementation
  Worker、reviewer 或父级。
- 本文不实施 45B，不生成最终 delete manifest，不改变任何卡片状态，也不把文件名、目录名、`untracked` 或
  “当前无 Spring caller”当作删除依据。
- 已完整对照 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第
  14-19 节、HTTPS turn 协议、44M45M SCC 报告、45A route 报告、最新 44A post-45A 报告、TURN-39 与
  TURN-40B dependency 报告，以及当前 Cloud production/test source。
- 图口径：扫描 Cloud 全部 521 个 `src/main/java` 与 38 个 `src/test/java`；去除 block/line comment、
  string/char literal 后按 Java type token 建结构边，raw-only 名称另行检查。import、字段、参数、返回值、构造、
  interface/record component 和 nested-type 引用均算编译边。
- “post-44A”仅是条件投影：从当前 production 图减去未来 45A 的
  `RemoteTaskRunRoutes`/`RemoteTaskRunEndpoint`，再减去最新 44A 报告的最小 `17 core + 2 leaf`。当前源码中
  45A、44A 均尚未发生，本文不得被当作删除授权。
- 快照时间：`2026-07-16T07:22:10-04:00`。本 helper 未运行 Maven、JUnit、compile/package、runtime、
  application、server、Task、UI、capture 或 input，未执行任何 Git mutation。

**2. 两仓 dirty/untracked 保护快照**

| Repo | Branch / HEAD | 完整 porcelain 计数 | status SHA-256 |
|---|---|---:|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 649：44 tracked dirty、605 untracked | `F5B68026D1F84C40A42099A2820CA9037C87990943CD0B7E6B4A0F96C21F84C5` |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550：9 tracked dirty、541 untracked | `45967BA604F3562CA21703D8B88B2479D1F8AE4C75195E2AD4D5671A33D16CFA` |

- 本轮纳入残余图的 39 个 Cloud production 文件全部为 `??`；它们不在 Cloud 当前 HEAD，最终 manifest 必须在
  39/40C/41、45A、44A 全部结束后重新读取路径、引用和 SHA，不能复用本报告作为 baseline。
- `CloudBrainServer.java` 当前仍是 tracked modified，并在 `:12,51,58,86` 引用/构造
  `RemoteTaskRunRoutes`；`RemoteTaskRunRoutes.java:51-54,87-130` 仍构造 broker/ledger/final old route graph。
- 新 HTTPS ingress 当前同时存在：`CloudBrainServer.java:96-107` 枸造 `CloudTurnRoutes`，注册
  `CloudTurnHttpHandler.PATH` 与 `CloudTemplateHttpHandler.PATH_PREFIX`。45B 对这些文件和路径零写入。
- 唯一目标报告在写入前不存在；未触碰两仓任何既有 dirty/untracked 文件。

**3. 依赖事实：当前仍只是条件图**

| 前置 | 45B 必须消费的真实结果 | 当前只读事实 |
|---|---|---|
| 38M/38C | 五个 authority-state 逐项 DELETE 或 KEEP_REWIRE；KEEP 行对 old run 类型零引用 | 固定文件 `2026-07-15-turn-38-authority-state-classification.md` 不存在；五项当前仍直接持有 run 类型 |
| 38A/B1/B2/B3/B4 -> 39 | `TaskExecutionContext`、bag/return/startup/artifact 与 old facade/context/run authority 脱钩 | 当前这些结构入边仍存在；39 报告列出的 metadata、`InputSequences` 与 existing-test owner 裂口仍未形成 final source |
| 40B | 四个真实 Task 的 final in-memory runtime/factory/registry/control，不引入 old session/ledger/run owner | 计划中的 5 production + 2 test 路径当前全部不存在 |
| 40C | final host/runtime/server 激活只引用新 turn ingress/context，且 close order 完整 | `CloudTurnRuntimeConfiguration.java`、`CloudTurnActivationContractTest.java` 当前不存在；40C 与 45A 同写 Server，必须串行 |
| 41 | 用户 fresh runtime 证明新 turn/runtime 真正消费业务链 | 本轮未见 TURN-41 报告；41 不新增 unit test，也不能由静态图替代 |
| 44M45M | 在上述 final source 上冻结逐文件 action/path/symbol/ref/SHA/cohort | 当前只有 PRECHECK 材料，不是可执行 manifest |
| 45A | old HTTP route/bean/wiring 已断，guard 与 Cloud compile 已结束 | 当前 old route 仍注册 |
| 44A | 17-file SCC、两个仍保留旧边的 leaf 及父级明确归 44A 的 state 已原子处理；guard 与 Cloud compile 已结束 | 当前 17+2 均存在；本文只从图中条件减去它们 |

固定串行关系仍为：

```text
39 + 40C + 41
  -> 44M45M final freeze
  -> 45A guard/compile
  -> 44A guard/compile
  -> post-44A full graph rescan
  -> 45B cohorts/guard/compile
  -> 46 dependency/property/config/doc cleanup
```

**4. 当前 post-45A + post-44A(17+2) 第一层 D8 seed**

下表路径前缀均为
`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/`。`当前 inbound` 是未投影源码事实；`post inbound`
是条件减去 45A 两文件与 44A 最小 19 文件后的 stripped production 结果。

| 条件 DELETE seed | 当前直接 inbound | post inbound | 直接 residual outbound |
|---|---|---|---|
| `CloudTaskTurnCoordination.java` | `CloudTaskTurnAuthority.java:997` | 零 | 无 |
| `NavigationWorkflowState.java` | `CloudTaskRetainedActionState.java:29,307` | 零 | 无 |
| `RemoteCommandOutcomeAck.java` | `RemoteGameCommandBroker.java:946-1011,1670-1688` | 零 | `RemoteOutcome`、`RemoteProtocolValidation` |
| `RemoteCommandOutcomeEnvelope.java` | broker `:946,1814`；routes `:119-120` | 零 | `RemoteClientScope`、operation/outcome/digests/validation/address |
| `RemoteCommandPollRequest.java` | broker `:809-810`；routes `:95-96` | 零 | `RemoteClientScope`、validation |
| `RemoteCommandPollResponse.java` | broker `:818-935,2007-2019` | 零 | `RemoteCommandEnvelope`、`RemoteFinalConsumedAck`、validation |
| `RemoteFinalConsumedReceiptAck.java` | `RemoteFinalConsumptionCoordinator.java:85-112,202` | 零 | validation |
| `RemoteTaskRunActionResponse.java` | `RemoteTaskRunEndpoint.java:12,62,69-70,258,269` | 零 | action、binding、error/error-code、receipt、validation |

- 八项均没有 structural test reference。
- D8 是当前条件图的精确第一前沿，不是最终 45B delete set。它只说明消费者已被 45A/44A 条件减去；未提供
  source-stable、SHA 或 compile 证据。
- `RemoteGameCommandBroker`、`RemoteFinalConsumptionCoordinator`、`CloudTaskRunActionLedger` 不在 D8，且永远
  不应重新出现在 45B：三者属于 44A 17-file SCC。

**5. 删除 D8 后的第二层 D2，以及图停止点**

删除 D8 后仅新增两个零 inbound singleton：

| 条件 DELETE | D8 前唯一 residual inbound | outbound |
|---|---|---|
| `RemoteCommandEnvelope.java` | `RemoteCommandPollResponse.java:6,27`；另有 44A broker `:1931,1971,1973` | operation/request/context/address/stop/window/validation shared types |
| `RemoteTaskRunError.java` | `RemoteTaskRunActionResponse.java:10,42` | `RemoteTaskRunErrorCode`、validation |

所以当前最小投影的 compile-safe partial order 是 `D8 -> D2`。两层可由 future manifest 合成一个原子 cohort，
也可按两次 consumer-first compile 分开；不能反向先删 D2 dependency 而保留引用它的 D8 consumer。

D2 后图停止，原因是 26 个 residual SCC 仍有真实外部根或受有外部根的 residual consumer 引用，而不是扫描遗漏。
其中唯一多文件 SCC 是：

```text
RemoteTaskRunBinding
<-> RemoteTaskRunScope
<-> RemoteTaskRunValidation
<-> RemoteTaskRunWindow
```

四项必须在其所有 consumer 消失后作为同一个原子边界处理；不得逐文件制造中间编译点。

**6. 唯一当前可证的窄 MODIFY pivot：`RemoteProtocolDigests.java`**

`RemoteProtocolDigests.java` 不能删除：它当前被 `CaptureOutcome.java:28`、
`CloudDialogOptionOcrImagePort.java:62,68,70,139`、`CloudDialogWhiteStoryTemplatePort.java:132`、
`TaskTrackerFinalConsumedAttachment.java:52`、`TaskTrackerMaterializeRequest.java:29`、
`TaskTrackerReadOutcome.java:92` 结构消费，并继续拥有 active request/outcome canonical digest 逻辑。

它同时保留两组 post-old-path method block：

1. `:175-234` 的 final-consumed ack/receipt digest methods。当前外部 caller 只有 44A 的 ledger/broker/final
   coordinator；44A 后这些 method 可成为窄删除候选。
2. `:236-250` 的 `computeResumeFactDigest(...)` / `computeTaskRunActionDigest(...)`。当前 caller 是
   45A endpoint `:230-232` 与 `RemoteTaskRunCoordinator.java:357-359`；endpoint 先由 45A 删除，但 coordinator
   尚受 38/39 state/context 入边保护，所以这组 method 不能随 D8 提前移除。

若 final manifest 明确给 45B 一条 MODIFY 行，当前图支持的唯一生产 modify candidate 是：保留
`RemoteProtocolDigests.java` 及全部 active digest behavior，只删除已无 caller 的上述 old final/task-run method
blocks。不得重写 canonical JSON、PNG exclusion、request/outcome digest 或 active tracker behavior。

在仅移除 `:175-234` 后会额外暴露：

- `remote/RemoteFinalConsumedAck.java`：D8 前由 poll response + digests 消费；D8 后只剩 digests。
- `remote/RemoteFinalConsumedReceipt.java`：当前唯一 structural consumer 是 digests。

因此当前条件图最多给出 `MODIFY RemoteProtocolDigests + DELETE 2 final DTO` 的窄扩展证据；是否写入 future
manifest仍须父级在 44A 后按 final SHA/ref 冻结。45B 不得为了扩大删除集修改其它 active utility、Task、Service、
state owner 或 context。

**7. 17 个 run/task-run 条件残余及真实 predecessor 入边**

D8/D2 已包含 `RemoteTaskRunActionResponse` 与 `RemoteTaskRunError`。其余 run/task-run 条件上界恰为 17 文件：

- `remote/run/ExecutionConfirmationRecord.java`
- `remote/run/RemoteTaskRunAuthorization.java`
- `remote/run/RemoteTaskRunBinding.java`
- `remote/run/RemoteTaskRunCapacityException.java`
- `remote/run/RemoteTaskRunCoordinator.java`
- `remote/run/RemoteTaskRunPrepareRequest.java`
- `remote/run/RemoteTaskRunScope.java`
- `remote/run/RemoteTaskRunSessionConflictException.java`
- `remote/run/RemoteTaskRunStatus.java`
- `remote/run/RemoteTaskRunValidation.java`
- `remote/run/RemoteTaskRunWindow.java`
- `remote/run/ResumeConfirmationRequirement.java`
- `remote/RemoteTaskRunAction.java`
- `remote/RemoteTaskRunActionRequest.java`
- `remote/RemoteTaskRunErrorCode.java`
- `remote/RemoteTaskRunReceipt.java`
- `remote/ResumeExecutorReadinessFact.java`

当前 structural external roots 如下；这些文件不属于 45B modify write set：

| 被保护 run type | 当前 SCC 外 production inbound |
|---|---|
| `RemoteTaskRunCoordinator` | `CloudGameContextStateOwner.java:36,43`；`CloudPlayerStateStateGovernor.java:40,49,53,62` |
| `RemoteTaskRunAuthorization` | `TaskExecutionContext.java:14,314`；Bag owner/workflow；ReturnItem owner；`ScopedPngArtifactStore.java:4,205` |
| `RemoteTaskRunBinding` | ReturnItem owner；GameContext/paused/player-state owners；`CloudTaskRunExecutionContext.java:3,50-51` |
| `RemoteTaskRunScope` | `TaskExecutionContext.java:15,205,234`；bag/return/startup/artifact；五个 38M state 中多个；old run execution context |
| `RemoteTaskRunStatus` | `TaskCheckpointDecision.java:3,20,67`；GameContext/paused/player-state owners |
| `RemoteTaskRunWindow` | bag/return；GameContext/left-top/paused/player-state owners；old run execution context |
| `RemoteTaskRunActionRequest` / `ResumeExecutorReadinessFact` | coordinator 内边；另由 `RemoteProtocolDigests.java:236-250` 保留 method signature |

其余 run 文件虽无 SCC 外 caller，仍被 coordinator 或上述四节点 SCC 消费，不能越过根节点先删。只有在 38M/38C、
38A/B1-B4、39 的 final source 已逐项清掉上表入边，40B/C 又没有引入任何 old run symbol 后，才可重算这 17 项。

在“上表外部入边全为零、endpoint 已删、digests old task-run methods 与对应 DTO 同 cohort 处理”的纯条件图中，
consumer-first 原子层为：

1. `RemoteTaskRunCoordinator`（`RemoteTaskRunActionResponse` 已在 D8）。
2. `ExecutionConfirmationRecord`、`RemoteTaskRunAuthorization`、`RemoteTaskRunCapacityException`、
   `RemoteTaskRunSessionConflictException`、`ResumeConfirmationRequirement`，以及已无 response/error/coordinator
   consumer 的 `RemoteTaskRunErrorCode`。
3. 原子修改 `RemoteProtocolDigests` 的 old task-run methods，并删除 `RemoteTaskRunActionRequest`；
   `RemoteTaskRunReceipt` 可在 `ExecutionConfirmationRecord` 消失后进入本层或下一层。
4. `RemoteTaskRunAction`、`RemoteTaskRunPrepareRequest`、`RemoteTaskRunReceipt`。
5. 原子四节点 SCC：`RemoteTaskRunBinding`、`RemoteTaskRunScope`、`RemoteTaskRunValidation`、
   `RemoteTaskRunWindow`。
6. `RemoteTaskRunStatus`、`ResumeExecutorReadinessFact`。

这些层是当前源码的 compile partial order，不是 future manifest cohort 编号。父级可以把相邻 consumer/dependency 层
合并成更大的原子 cohort，但不能拆开四节点 SCC，也不能在 coordinator caller 尚存时移除 digests task-run method。

**8. `RemoteClientScope` 的单独条件边界**

命令 D8 消失后，`RemoteClientScope.java` 仍有两个非命令 structural caller：

- `CloudPausedReadOnlyObservationContext.java:104-105`
- `CloudTaskRunExecutionContext.java:96-97`

因此它不是当前 D8/D2，也不能凭“transport”名称删除。只有 38M/38A/39 final source 删除或重接这两个 caller，且
40B/C 不再消费 old client scope 时，它才成为一个独立零 inbound DELETE candidate。若任一 KEEP_REWIRE 行选择继续
使用它，则它退出 45B 条件上界。

**9. 明确保留的 shared transport/value types**

以下九项在当前业务 wire/model 中有真实 caller，不能归入 45B delete set：

| 保留 source | active structural evidence |
|---|---|
| `RemoteOperation.java` | capture/input/local-macro/task-tracker/window-fact 等 17 类 request/outcome |
| `RemoteOutcome.java` | 8 个 active outcome record |
| `RemoteProtocolDigests.java` | 第 6 节六个 active caller；只允许 future exact old-method prune |
| `RemoteProtocolValidation.java` | capture/input/dialog/NPC/tracker/window-fact 等大量 active command/result/model |
| `RemoteRequest.java` | 8 个 active request record |
| `RemoteSemanticAddress.java` | task-tracker attachment/materialize 及 active request context |
| `RequestContext.java` | capture/input/local-macro/task-tracker/window-fact 等 8 个 active request |
| `StopRef.java` | `RequestContext`，另有 paused/run context |
| `WindowBindingRef.java` | `RequestContext`，另有 paused/run context |

同样受保护的还有全部 active capture/input/local-macro/window-fact、Cloud-owned business result/model、四个永久本地
Service port/command/result、NPC reference/shadow 和 task-tracker types。45B guard 不能用 `remote/**`、`Remote*`、
文件名前缀或目录整体不存在来代替逐 path/symbol 证据。

**10. R1 exact delete/modify/test boundary，不是最终 manifest**

当前证据把 future 45B 上界拆为四块：

| 块 | action | 文件数 | 当前证据 |
|---|---|---:|---|
| D8 + D2 | DELETE | 10 | 条件减去 45A + 44A 最小 19 后可直接按两层暴露 |
| final pair | DELETE | 2 | 需同一 45B patch 窄改 `RemoteProtocolDigests:175-234` 后才为零 inbound |
| remaining run/task-run | DELETE | 17 | 需 38/39 外部入边、40C 新入边、existing tests 全部先闭合，再按第 7 节重算 |
| client scope | DELETE | 1 | 需 paused/run context 两个 caller 先消失或重接 |

所以条件最大包络是 30 个 DELETE candidate；其中当前最小 `17+2` 投影只直接证明前 10 个。30 不是 final
delete manifest：任何 predecessor 保留/新增 consumer、38M KEEP_REWIRE 结果或 final SHA 漂移都会缩小或重排它。

唯一 production MODIFY candidate：

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`，仅删除 final source 已无 caller 的
  old final/task-run method blocks；文件和 active digest behavior保留。

唯一 future test write set：

- `src/test/java/com/yueyunfe/dhxy/cloudbrain/OldRemoteWireRemovalGuardTest.java`，CREATE；当前不存在。

除上述条件 production 边界与唯一 guard 外，45B 对 production/test 零写入。若实现必须修改 active caller 才能让
某类型归零，该修改必须回到 38/39/40C 或父级重新分配，不能由 45B 顺手完成。

**11. Existing test-compile caller 与唯一 named guard ownership**

当前 D8/D2 十项没有 structural test ref，但 run 条件上界仍被两个既有测试直接 import/构造：

| Existing test | structural refs |
|---|---|
| `runner/context/TaskExecutionContextTurnContractTest.java` | `RemoteTaskRunAuthorization:28,501`；`RemoteTaskRunScope:29,499`；同一测试 `:494-506` 仍要求 old context/facade public surface |
| `service/LeftTopStatusTurnContractTest.java` | Binding `:29,576,956`；Coordinator `:30,538,543,954`；PrepareRequest `:31,578`；Scope `:32,566,604,955`；Window `:33,571` |

`LeftTopStatusTurnContractTest.java:538-604` 还以 reflection 构造 44A 的 broker/ledger/final/assembly；这些 raw class-name
字符串不形成 test-compile type edge，但 44A 后执行旧 fixture 会失败。两类现有测试均不在 45B test write set；父级
必须先给 predecessor/原 owner 精确更新或退役边界。

未来运行
`mvn -q -Dtest=OldRemoteWireRemovalGuardTest test` 时 Maven 仍会先 test-compile 全部测试源码，所以不能用“只点名
新 guard”隐藏上述 imports。guard 应逐 final manifest 行证明：

1. 每个 DELETE path 不存在，且其 exact symbol 在 production stripped graph 中零引用。
2. 每个 MODIFY path 与冻结 SHA/action 一致；`RemoteProtocolDigests` 不再出现被删 old DTO/method signature，同时
   active digest API 仍存在。
3. 45A route/endpoint、44A authority SCC 不被重复归属；new turn/template ingress exact path/symbol 继续存在。
4. retained shared types使用 explicit allowlist；不做 broad prefix/directory/string guard。

本文未创建或运行该 test。

**12. Future compile sequencing**

1. 45A 先完成其唯一 guard 与 Cloud `mvn -q clean compile`；45B 不与 45A 共写 Server/routes。
2. 44A 再原子处理其完整 SCC/leaf/classified state，完成 `OldAuthorityRemovalGuardTest` 与 Cloud clean compile；两者之间
   不插入 45B 删除。
3. 在 44A compile 后重扫 521-file production 图、全部 test refs、30 个条件 candidate 的 SHA/status，并由
   44M45M parent material冻结 final action/cohort；本报告列表不能直接执行。
4. 对 final manifest 的每个 deletion cohort按 consumer-first 顺序处理；每个 cohort 后执行 Cloud
   `mvn -q clean compile`。D8/D2、final pair、run tiers、client scope 的合并/拆分只能服从第 5-8 节的边。
5. final 45B source 上运行唯一 named command
   `mvn -q -Dtest=OldRemoteWireRemovalGuardTest test`，随后再次执行 Cloud `mvn -q clean compile`。
6. 只有 45B 最终 gate 后才交给 TURN-46；46 再按实际 imports 处理两仓 POM/property/config/doc。本文没有运行任何
   上述命令。

**13. 与 45A、44A、46 的写集互斥证据**

- 45A 独占 `CloudBrainServer.java` MODIFY、`RemoteTaskRunRoutes.java` DELETE、
  `RemoteTaskRunEndpoint.java` DELETE、`OldRemoteRouteRemovalGuardTest.java` CREATE。45B 的“routes residue”只指
  route-related DTO/wire 的 post-route 零引用残余，不能再次写这四项。
- 44A 独占 17-file authority/context/final-consumption SCC、两个仍保留旧边的 leaf、父级分类中明确归 44A 的 state，
  以及 `OldAuthorityRemovalGuardTest.java`。45B 不接 broker/ledger/final coordinator，也不与 44A 并发删同一组件。
- 46 独占两仓 `pom.xml`、`application.properties`、精确 config/doc/CR/dashboard cleanup 和两仓
  `HttpsTurnDependencyCleanupGuardTest.java`。45B 不改 dependency/property/config/doc；尤其
  `RemoteProtocolDigests` 仍有 active Jackson usage，46 必须按 final imports决定存废，不能把 45B 当作宽泛删除
  Jackson/HTTP/image dependency 的理由。
- Cloud Java 删除线保持唯一且严格串行 `45A -> 44A -> 45B`；本 helper 只写本报告。

**14. STOP-WORK 条件**

出现任一项即停止 45B source action并返回对应 predecessor/父级补证据：

1. `CloudBrainServer` 仍注册 old task-run/poll/outcome/final-consumed route，或 45A guard/compile 尚未形成 fresh 证据。
2. 44A 的 17 core、两个 compile-closure leaf、任一父级归 44A 的 state 仍存在，或 44A guard/compile 尚未结束。
3. 试图把 broker、ledger、final coordinator 从 44A 搬到 45B，或让 44A/45B 并发各删 SCC 一半。
4. 38M 固定分类仍缺失；任一 KEEP_REWIRE state 仍引用 old run/client-scope；任一 DELETE state 未进入 44A exact
   cohort。
5. `TaskExecutionContext`、bag/return/startup/artifact、checkpoint、state owner 或 active Task/Service 对拟删 symbol
   仍有 structural reference。
6. TURN-39 final facade/context/metadata source 尚未稳定，或 40B/C final runtime/host/server 尚不存在、仍引用 old
   run/session/ledger/wire。
7. TURN-41 fresh runtime 证据门尚未完成，或新 HTTPS turn/template ingress 没有被 45A/40C 保留。
8. `TaskExecutionContextTurnContractTest`、`LeftTopStatusTurnContractTest` 或任何新增 test 仍 import/构造拟删类型；
   不得让 45B 越界改 existing tests。
9. `RemoteProtocolDigests` 任一 old method仍有 production/test caller；不得为了删除 old DTO 改 active canonical
   digest behavior。
10. 任一 candidate 的 final source path/ref/SHA 与 44M45M freeze 不一致，或当前 dirty/untracked/并行 writer 造成
    图漂移；必须重算，不沿用本报告行号。
11. 需要 wildcard、目录删除、`Remote*` 前缀、raw string 零命中或“untracked”来补足证据。
12. 需要修改本文第 10 节以外的 production/test 文件才能编译；先回 predecessor 或修订 parent material。
13. 45B patch 触碰 Server/routes、44A SCC、POM/property/config/doc、CR/ACTIVE_WORK/矩阵/dashboard 或 46 guard。
14. 任何步骤要求回滚、覆盖、清理、删除非 manifest 文件、暂存、提交或其它 Git mutation来整理工作树。

**15. 本 helper 写集与未执行项**

- 唯一写入：
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-45B-post-44A-residual-readiness-helper-r1.md`。
- 未修改 Java、test、权威计划、CR271、`ACTIVE_WORK.md`、矩阵、dashboard 或其它报告；未删除任何文件。
- 未运行 Maven/JUnit/compile/package、runtime/application/server/Task/UI/capture/input；未做 Git mutation。
- 本文只提供当前 source graph 的 PRECHECK 与 future exact-boundary 约束，不提供最终 delete manifest或卡片结论。

PRECHECK_COMPLETE TRUE_EOF
