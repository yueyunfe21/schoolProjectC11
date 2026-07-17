# CR271 TURN-40B latest runtime/factory construction delta PRECHECK R1

## 0. 角色、边界与结论口径

- 角色：CR271 非绑定 readiness helper；不是 implementation Worker、owner、claimant、reviewer 或父级 final reviewer。
- 审计时点：`2026-07-16 07:44:23.540 -04:00`。本报告只描述该时点两仓实物与权威依赖，不改变任何卡片状态。
- 唯一写入：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-40B-latest-runtime-factory-readiness-helper-r1.md`；目标在落笔前不存在。
- 当前 PRECHECK 事实：TURN-40B 的唯一 source 启动公式仍是 `S=TURN-39+TURN-40A+TURN-13H`。40A 与 13H 已有父级 source/test-source review 证据，但 TURN-39 未形成 source-stable 交付；当前真实 Task、context、host 与 ingress 之间也没有可供 40B 无猜测消费的 construction seam。因此当前不得领取 40B 实现。
- 本报告不是实现许可、接口冻结、owner/claim 或卡片终审。下文把“权威已经固定的合同”与“必须由父级/前置卡先固定的空白”明确分开。
- 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input；未执行任何 Git mutation。

## 1. 已读取的权威输入与最新快照

### 1.1 完整读取/复核

1. `D:/mavenProject/DHXY/AGENTS.md` 全文，392 行，SHA-256 `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5`。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md` 全文，1349 行，SHA-256 `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621`。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 最新顶部 CR271 `:1-59`。落笔前 SHA-256 为 `7FAE5E8C39C69C3E3346A4D65B248CA5FCC3BCBE72EC0B346C189A555C785619`；其 `:39-41` 明确本 helper 被续派 TURN-40B delta，`:42-57` 明确当前 writer/HEAD/业务基线边界。
4. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 最新第 14-19 节全文；落笔前 1690 行，SHA-256 `99BE035AD7693D6FD636F1016580ECBC9EDB7D174026B0CCB603DF02EBBF8A2F`，mtime `07:39:13.900 -04:00`。
5. HTTPS turn 协议规格 `2026-07-15-https-turn-thin-client-protocol-design.md` 全文，383 行，SHA-256 `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB`。
6. 既有 TURN-40B PRECHECK 全文，386 行，SHA-256 `A2FB1C6E7EB50A589590A90DC4C7AA07F54272B57B930E12A92D8837AA9EBDCE`。
7. 最新 TURN-39 PRECHECK 全文，396 行，SHA-256 `B61FE4460F0D180422D85EED5724614AD76694CC47316BBAF4924AD1BA36843B`。
8. TURN-40A/T01 原卡、TURN-T01 测试卡、TURN-13H 原卡、TURN-38A PRECHECK、TURN-38M GameContext route PRECHECK，以及当前 38B1/B2/B3/B4/38C 相关状态与报告。
9. 两仓当前 protocol、exchange/handler/routes/server/host/configuration、Task factory/action client、四个真实 Task、`GameTask`、`TaskRunResult`、`TaskExecutionContext`/Holder、`GameContext` 及全部相关 production/test refs。

### 1.2 两仓只读状态

| Repo | Branch | HEAD | status 条目 | tracked entries | untracked | deleted |
|---|---|---|---:|---:|---:|---:|
| DHXY | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 655 | 44 | 611 | 1 |
| Cloud | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 550 | 9 | 541 | 0 |

- 计数来自 `git status --short --untracked-files=all`，只作保护快照；两条 HEAD 都不是业务基线。
- 唯一业务逻辑基线仍是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。
- Cloud 四个真实 Task、context、host 及大部分 turn source 都是当前 `??`；`CloudBrainServer.java` 是 `M`。它们不能被当成可由 HEAD 恢复的稳定构造合同。
- 本 helper 未回滚、覆盖、清理、删除、暂存或提交任何既有 dirty/untracked 内容。

## 2. 相对既有 TURN-40B PRECHECK 的 latest delta

1. 权威公式未变：计划 `:1163` 仍是 `S=39+40A+13H`；计划 `:1432-1433` 仍锁定真实消费顺序 `40B -> 40C -> 40D`。
2. 40B 的五个 production 与两个 test 目标路径仍全部不存在；旧报告没有被实现实物追上。
3. 40A 当前八对 protocol 仍逐对 byte-identical；T01 lifecycle test 也两仓同字节。变化是 T01 Repair #1 后 test SHA 已稳定为 `57AABF...`，但 required Maven gates 仍没有本轮 fresh 证据。
4. 13H dormant host source/test-source 仍在，且 production `CloudServiceHost.create(...)` caller 仍为零。它证明“host 构造不激活 Task/thread”，不证明 real Task factory 已可构造。
5. 当前三大 whole Task 比旧 PRECHECK 暴露出更直接的静态 construction 断裂：它们仍调用不存在于当前 `TaskExecutionContext` 的 `builder()` 与 `getWindowRuntimeContext()`；同时仍导入 Cloud source tree 中不存在的 DHXY-only 类型。
6. 当前 `CloudTurnExchange` 仍只接受 metadata/outcome 后等待 action，并构造不带 start ack 的 response；handler 仍没有 request-correlated response validation。该缺口不在 40B 写集，且当前 40C 写集也不含 `CloudTurnExchange.java`。
7. TURN-38M 的 GameContext 两路线只是非绑定风险材料；权威计划 `:1159-1160,1325-1334` 仍显示 parent classification 未冻结。旧 PRECHECK 中任何倾向性 API/线程方案都不能升级为 40B 接口。

## 3. `S=39+40A+13H` 逐项核验

| Dependency | 最新权威/实物 | 对 TURN-40B source start 的含义 |
|---|---|---|
| TURN-39 | 计划 `:1161` 为 planned/precheck real blockers；最新报告 `:367-372` 仍列 DAG、active old refs、`InputSequences` owner、metadata 与 test ownership 缺口；唯一 named test 也不存在 | 直接 source 前置未满足；这是当前明确 stop-work 项 |
| TURN-40A | 计划 `:1162` 为 source review passed、test+Cloud build pending；当前双仓八对 production 同字节，validator `:46-63` 锁 exact request/ack correlation | source surface 可读且 owner 已释放；不得把未运行的 T01/Maven/Cloud build 当成验收完成 |
| TURN-13H | 计划 `:1109` 与原卡 `:134-158` 记录 source/test-source review 通过、named Maven+Cloud compile 待办；production host create caller 为零 | dormant host source surface可读；它刻意不提供 Task factory、host caller、runtime/thread activation，因此不能补掉 39/40B 的 construction seam |

### 3.1 startDependsOn 精确结论

- 唯一公式是权威计划 `:1163` 的 `TURN-39 + TURN-40A + TURN-13H`，不能加入 helper 自创 predecessor，也不能跳过 TURN-39。
- 40A、13H 的 source-stage 证据当前存在；TURN-39 的 source-stage 证据不存在。故 40B 当前不能 claim/开工。
- TURN-39 又依赖计划 `:1161` 的 `38B1/B2/B3/B4+38C`。当前 `:1155-1160` 显示这些卡仍是 PRECHECK/readiness/classification 状态，不是可供 39 消费的 source-stable 交付。

### 3.2 approvalDependsOn / build debt

权威计划 `:1602-1605` 规定 TURN-13G..40D 在 source 之后，最终验收仍需 `TURN-T01+T02+T03+T04` 中实际调用链相关部分：

1. T01：40B 直接消费 start request/ack、ordered queue、pause/stop protocol，明确适用。
2. T02：real Task 通过同一 `CloudTurnCommandPort/CloudTurnExchange` 等待 action outcome，exchange/HTTP 合同明确适用。
3. T03：Cloud action 最终由 DHXY client/executor/loop 消费；uncertain、stop、single-send 与 exact-window 证据适用。
4. T04：四个 allowlisted real Task 会经过四个永久本地 Service operation 边界，相关路径适用。
5. TURN-13H 自己的 `CloudServiceHostTurnCapabilityContractTest` 与 Cloud compile 仍是 host construction 的独立待验债。

这些是后续验收依赖，不改变当前首先被 TURN-39 卡住的 source start。按计划 `:1432-1444`，activation 四卡 source stable 前也不应并发跑双仓 build cohort。

## 4. 当前 40A protocol 可冻结事实

### 4.1 双仓八对 production parity

| File | 当前相等 | SHA-256 |
|---|---|---|
| `TurnTaskCode.java` | 是 | `A116361EE173F37639967459111AB1CC595469EE1C8F8E3BA91E108BDB2895F7` |
| `TurnTaskQueueFailurePolicy.java` | 是 | `2B9DBFF0F75612DCC818B89825F2EC003A8BD08ADF05176D7592F4C570B5C97C` |
| `TurnTaskStartRequest.java` | 是 | `D4AF7B55DD1B4A6B01DF5EED4E9F2468B745A31241314762242C340D0FF03117` |
| `TurnTaskStartAck.java` | 是 | `B5C196C7084211AE917DB543411C08247611B0A962CD60219E478660BC1D299B` |
| `TurnWindowMetadata.java` | 是 | `E1430169AAE3E35AC9F6295E41EA401E66EFF910E0B4B4DFF954E72C9416AF1B` |
| `TurnRequest.java` | 是 | `BF7EA75A8FF44CBD5B7FB8F73ECF172BB7466B00EECC1407087688E5D74B571C` |
| `TurnResponse.java` | 是 | `646F9738FC296949BE6D1787481B8F946F2CF4CFA7D2971D5B4B987BA2F1E75F` |
| `TurnProtocolValidator.java` | 是 | `3929CFB6AFAD86FFECCF1573E108A59878541D0D06D439A4CE95F6046252B2B0` |

两仓 `TurnTaskLifecycleProtocolGoldenJsonTest.java` 也同字节，SHA-256 `57AABF91B654A688A175BBAC7E3A7D1AC2D238852DC2D8A74A2FE24375BEE288`；这里只能引用测试源码事实，不能声称本轮执行通过。

### 4.2 固定字段与边界

- `TurnTaskStartRequest` 只有 stable `startRequestId`、defensive-copy ordered `taskCodes`、`failurePolicy`。
- `TurnTaskCode` 只有 `WUHUAN_V2/WUBEI/XIULUO_V2/AUTO_BATTLE`。
- `TurnTaskQueueFailurePolicy` 只有 `CONTINUE_ON_FAILURE/STOP_ON_FAILURE`。
- `TurnProtocolValidator.java:416-425` 只要求非空 ID、非空 list、元素非 null、policy 非 null；当前没有禁止重复 task code。
- `TurnProtocolValidator.java:46-63` 要求有 start request 就必须有同 ID ack，无 start request 就禁止 unsolicited ack。
- 协议没有 sessionId、owner、workflow ledger、payload hash、TTL、history、durable retry 或 Task completion payload。

因此 40B 只能对 transport redelivery 的同一 stable ID 做内存去重；不能擅自把协议扩成 lifecycle/session/ledger。

## 5. allowlist 与 real Task 映射

| Protocol code | 唯一 real Task | 当前 `getTaskCode()` | 精确证据 |
|---|---|---|---|
| `WUHUAN_V2` | `FiveRingTaskV2` | `wuhuan_v2` | class/bean `:102-105`，constant `:107`，getter `:220-223` |
| `WUBEI` | `WubeiTask` | `wubei` | class/bean `:113-116`，constant `:118`，getter `:317-320` |
| `XIULUO_V2` | `XiuluoTaskV2` | `xiuluo_v2` | class/bean `:119-122`，constant `:124`，getter `:292-295` |
| `AUTO_BATTLE` | `AutoBattleTask` | `auto_battle` | class/bean `:39-42`，getter `:79-81` |

- 四类当前均标注 prototype；这只说明原类的 Spring scope intent，不说明 current Cloud host 已能发现/构造它们。
- `SLEEP_COMPUTER` 不在 enum，计划 `:1070` 也明确排除；不得创建 fallback/default Task 或扫描后按字符串容忍未知值。
- `CloudTurnActionFactory` 是 turn action mechanics factory：`CloudTurnActionFactory.java:16-105` 接 caller-provided actionId 构造 `TurnAction`。它不是本卡缺失的 real Task factory，不能复用名称混淆职责。

## 6. 当前真实 factory/construction seam

### 6.1 40B 七个目标仍为零实物

以下五个 production 与两个 test 路径在快照时全部 `ABSENT`：

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactory.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRegistry.java`
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskStartResult.java`
5. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnControlPort.java`
6. `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java`
7. `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactoryAllowlistTest.java`

### 6.2 current host 不构造 Task

- `CloudServiceHost.create(...)` 仅注册 fixed `CloudServiceScope`、storage、route-owned command port/catalog，再 refresh configuration，见 `CloudServiceHost.java:39-65`。
- production `CloudServiceHost.create(...)` caller 为 `0`；当前命中只在 13H named test `:55,78,106-112`。
- `CloudServiceConfiguration.java:27-40` 只扫描 `com.bot.dhxy.service` 与 `turn.client`，并未扫描 `com.bot.dhxy.task`，也没有显式四 Task provider 或 `GameContext` bean。
- `CloudBrainServer.java:96-105` 只创建 turn routes 并保留 command port/catalog；`close()` `:118-122` 只停 server/executor。Host/runtime activation 属于 40C，当前不存在。
- 全 Cloud production/test 对 `new FiveRingTaskV2/WubeiTask/XiuluoTaskV2/AutoBattleTask`、`getBean(Task.class)` 或对应 `ObjectProvider<Task>` 的命中均为 `0`。

### 6.3 当前 Task source 不能作为稳定 constructor contract

| Task | 当前静态断裂 |
|---|---|
| `FiveRingTaskV2` | `:263,296` 调 `getWindowRuntimeContext()`，`:2751-2756` 调 `TaskExecutionContext.builder()`；当前 context 两者均不存在；另有 14 个 import source 路径缺失，包括 tracker/input/window/temp/transaction/OCR |
| `WubeiTask` | `:367,413` 调 `getWindowRuntimeContext()`，`:4250-4255` 调不存在的 builder；另有 14 个 import source 路径缺失，包括 `MultiWindowTaskManager`、window holder、transaction/input 类型 |
| `XiuluoTaskV2` | `:331,399,2411` 调 `getWindowRuntimeContext()`，`:3889-3894` 调不存在的 builder；另有 16 个 import source 路径缺失，包括 old Quest/OCR/window/transaction 类型 |
| `AutoBattleTask` | `:55-73` 的显式十参数构造图依赖 `GameContext`、context holder、step executor 与七个 Service；该图又受 34A/34B/34C、38A/38C/39 最终形状约束 |

三个 whole Task 都是当前 untracked 中间态；静态 symbol/import 证据足以说明它们尚不是 40B 可消费的 final provider API。本 helper没有运行 compile，也不会用猜测 constructor、手工 `new`、反射或 mock 替代前置交付。

## 7. queue 与 `startRequestId` dedupe 边界

### 7.1 权威已经固定

1. Queue 来源是 request 中的 ordered `taskCodes`，不是 Cloud 自动发现或 durable workflow。
2. Registry 只能保留每个 current exact window runtime 与最后 accepted `startRequestId/ack`，计划证据 `:1052,1064-1067,1362`。
3. 同一 exact window 的同一 stable ID 网络重送不得二次启动 Task；返回保存的 exact ack。
4. 不持久化、不 TTL、不自动 Task/action retry；transport uncertainty 保留 exchange fence，只有业务显式 retry 才产生新 actionId。
5. fixed host scope 内 `deviceId+windowId` 是 runtime key；tenant/user/stateRoot 来自进程配置，不从 request body 推断。

### 7.2 当前没有固定，Worker 不得临场选择

1. 同一 `startRequestId` 但 payload/task order/policy 不同，是回原 ack、typed conflict 还是其它结果；协议没有 payload hash 字段。
2. active runtime 收到新的 `startRequestId` 时的冲突形状与 HTTP 映射。
3. request 中重复 task code 的语义；当前 validator 允许重复，故 factory/runtime 不得自行 list-dedupe。
4. `SUCCESS/FAILED/STOPPED/SKIPPED` 对两种 queue policy 的逐项继续/短路矩阵，以及 Task 抛异常、Error、factory failure、worker start failure的映射。
5. ack 的原子时点：在 registry reservation、全部 Task materialization、worker start 中哪一步之后才可保存/返回 ack。
6. concurrent same-ID/new-ID race 的唯一 linearization point、start result closed shape、close/join timeout 与 interrupt 顺序。

以上空白都影响 production public API 和 named tests。父级必须先在权威卡/计划中冻结；不能由本 PRECHECK 发明 `CloudTurnTaskStartResult` 字段、`CloudTurnControlPort` 方法或 registry 状态机。

## 8. pause/stop 与 context construction seam

### 8.1 当前可复用的 exact-context 事实

- `TaskExecutionContext.turnNative(...)` 位于 `:96-109`，要求 exact invocation、initial metadata、task metadata/run ID 与 bound `TurnGameClient`。
- turn-native checkpoint `:385-429` 每 250ms 读取最新 exact metadata；STOP 优先，PAUSE 在同一调用栈等待，不创建 replacement context。
- `TaskExecutionContextHolder.callWith(...)` `:19-30` 会在正常/异常退出后恢复 previous context，支持 A -> B -> A。
- `TurnGameClient.java:73-82,156-176` 在 exact bound context 下读取 metadata/生成一次 UUID并走同一 command port。

### 8.2 当前不能闭合的 construction

- `TaskExecutionContext` 仍保留 old delegate 与 `CloudTaskServiceMetadata`，见 `:35-39,51-109,204-352,438-455`；这正是 TURN-38A/39 尚未完成的依赖面。
- 三大 Task 仍尝试自行构造 context，且调用当前不存在的 builder；40B 不得增加 compatibility builder 或在 factory 中伪造 role/team/retry/startup metadata。
- HTTPS `TurnWindowMetadata` 与 start request 没有 local-team role/session/leader/support、retry policy、startup mode 等 authority 字段；这些 active consumers 的来源仍由 TURN-39/前置卡父级冻结。

### 8.3 stop 的真实跨线程风险

四个 Task 的 `stop()` 都写 mutable `GameContext`：

- `FiveRingTaskV2.java:321-325`
- `WubeiTask.java:327-331`
- `XiuluoTaskV2.java:412-416`
- `AutoBattleTask.java:172-176`

`GameContext.java:18-19` 使用 singleton `defaultState` + `ThreadLocal.withInitial(defaultState)`；`:127-143` 的 projection finally 只 remove，不支持 nested previous restore。若 HTTP/control 线程裸调 `activeTask.stop()`：未投影会写共享 defaultState；并发投影同一 State 又会产生 worker/control 双写。

TURN-38M 报告 `:263-273,338-344` 已把该风险列给父级，但路线尚未冻结。因此 40B 当前只能冻结验收条件，不能冻结实现 API：

1. pause/resume 必须保持同一 Task instance、TaskExecutionContext、exact device/window 与同一 mutable State。
2. stop 权威来源、worker interrupt/checkpoint、是否/何时调用 `Task.stop()`、调用线程与先后必须由父级明确。
3. 不得从 control/HTTP 线程直接写 Task State；不得用新 TTL/retry/session/owner 绕过 stop race。

## 9. host/context ownership

1. 13H 已固定 `CloudServiceHost.create(scope,stateRoot,commandPort,catalog)` 的 fixed-scope/dormant 合同，但没有 runtime owner 或 Task provider。
2. 未来 runtime、四 Task、全部 Service 必须看到 object-identical host-local `GameContext`；当前 host config 没有该 bean。
3. `CloudBrainServer.java:86-90` 给 old remote routes 单独 `new GameContext()`；它不是 future host graph 的共享对象，40B 不得复用。
4. `GameContext.State` 由 TURN-38C rewire 旧 owner还是由 40B runtime 直接持有，当前只有 38M 非绑定路线分析；权威 classification 文件仍不存在，计划 `:1159-1160,1327-1334` 明确要求 parent freeze。
5. 40B 五文件写集不含 host/config/GameContext/Task/context 文件；40C 六文件写集才拥有 host runtime configuration。任何需要改这些文件的 construction 方案都必须先由父级修订卡/写集。

所以当前不能猜 `CloudTurnTaskFactory` 使用 Spring `ObjectProvider`、supplier、host lookup 还是其它 API，也不能猜 State acquire/project/release 方法。唯一可写事实是：最终 factory 必须消费 predecessor 真实 public construction seam，构造时 inert，且每个 queue occurrence 获得正确 prototype。

## 10. start request / ack 的真实 ingress seam

### 10.1 当前 caller 事实

- Cloud production 对 `TurnTaskStartRequest/TurnTaskStartAck/TurnTaskCode/TurnTaskQueueFailurePolicy` 的引用目前只在 protocol records 与 validator 中；没有 runtime consumer。
- `CloudTurnExchange.exchange(...)` `:118-132` 只 validate request、接受 metadata/outcome、等待 action，并用二参数 constructor 创建 ack 为 null 的 `TurnResponse`。
- metadata 在无 outcome 时于 `CloudTurnExchange.java:140-147` 写入；有 outcome 时于 `:157-183` 写入；随后 `:190-202` 立即 long-wait action。
- `CloudTurnHttpHandler.java:152-170` 只调用 exchange/serialize；它没有调用 `TurnProtocolValidator.requireValid(response, request)`。
- 因此当前携带 start request 的 ingress 会得到缺 ack response；该 response 若按 validator `:46-63` 与 request 校验会被拒绝。当前 production handler 没有执行这一步 correlation validation。

### 10.2 写集断点

- 40B 只能写 runtime 五文件，不能改 handler/exchange/routes/server/host。
- 40C 可写 handler/routes/server/host/config/application，但权威 `:1364-1372` 当前不含 `CloudTurnExchange.java`。
- 首次 metadata 必须在 Task checkpoint 前对同一 exchange 可见；但“exchange 内 hook、handler 两阶段调用或其它 ordering”尚无父级固定合同。

故父级在 40B 实现前必须冻结并持久化：谁调用 `CloudTurnControlPort`、何时提交 accepted metadata、何时得到 start result/ack、factory/worker failure 如何映射 HTTP，以及是否修订 40C exact write set。本文不选择接口名、方法签名或方案。

## 11. terminal cleanup 边界

### 11.1 权威要求

1. terminal 后 registry 移除 current runtime；Task、queue、worker、TaskExecutionContext、Holder projection 与 mutable State 不再作为 active refs 保留。
2. 只保留 last accepted `startRequestId/ack`，以便相同 ID redelivery 不二启；不保留 Task result/exception/history/时间戳。
3. pause 不是 terminal；resume 继续同一 runtime/context/state。
4. stop/terminal 后 defaultState sentinel 不得变化；后续新 ID 使用新 runtime/state，旧 window state 不泄漏。
5. construction、registry lookup 与 host startup 都 inert；只有显式 accepted start 才允许创建/启动 worker。

### 11.2 不能由 40B 猜的 cleanup 顺序

- Task terminal 回调、可选 `Task.stop()`、Holder unwind、GameContext projection unwind、worker terminal publication、registry remove 的 exact order。
- factory failure、partial queue materialization、thread-start failure、Task exception/Error、runtime close 与 concurrent stop 的 cleanup/ack 结果。
- `CloudTurnExchange` 当前还保留 unresolved action fence、last action ID/hash 与 latest metadata，且没有 remove API。它属于 transport authority，40B terminal cleanup 不得盲清；否则可能把 uncertain action 变成物理输入重执行。若未来需要 eviction，必须由 40C/父级给出无 unresolved action/waiter 的 exact 条件和 test ownership。

## 12. TURN-40B exact write set 与只读边界

### 12.1 唯一 production write set

权威计划 `:1355-1362` 只允许创建：

1. `turn/runtime/CloudTurnTaskFactory.java`
2. `turn/runtime/CloudTurnTaskRuntime.java`
3. `turn/runtime/CloudTurnTaskRegistry.java`
4. `turn/runtime/CloudTurnTaskStartResult.java`
5. `turn/runtime/CloudTurnControlPort.java`

### 12.2 唯一 test write set

权威计划 `:1660` 只允许创建：

1. `turn/runtime/CloudTurnTaskRuntimeContractTest.java`
2. `turn/runtime/CloudTurnTaskFactoryAllowlistTest.java`

### 12.3 明确只读

- 四个 real Task、`GameTask`、`TaskExecutionContext`/Holder、`GameContext`、Services、protocol 八文件。
- `CloudTurnExchange`、`CloudTurnHttpHandler`、`CloudTurnRoutes`、`CloudBrainServer/Application`、`CloudServiceHost/Configuration`。
- 13H/T01/13C/38A/39 既有 tests、POM、fixtures、主计划、ACTIVE_WORK、CR/card/dashboard/matrix。
- 不新建第三个 test、fixture、source guard、helper DTO、session/owner/ledger/history/TTL 文件。

若 implementation 需要第八个文件或修改上述只读文件，按计划 `:1040-1042` 必须 stop-work，由父级先修权威写集。

## 13. named-test ownership matrix

### 13.1 `CloudTurnTaskFactoryAllowlistTest`

必须通过 production public factory seam 验证：

1. 四个 enum 各只映射表中唯一真实 Task class/getTaskCode；未知/null 与 `SLEEP_COMPUTER` 无 fallback。
2. 每个 queue occurrence 获得独立 prototype，不缓存 Task singleton，不扫描/接受第五 Task。
3. factory construction/create 不启动 thread、不执行 Task、不读取 metadata、不生成 actionId、不调用 command port。
4. 测试不得复制 production switch、用 source-string/reflection 代替调用，或以 mock class 冒充四个真实 Task 类型。
5. 在 predecessor constructor graph 未稳定前，该测试只能冻结矩阵，不能猜 constructor/provider 接口。

### 13.2 `CloudTurnTaskRuntimeContractTest`

在父级补齐第 7-11 节空白后，必须经 production public runtime/registry/control API 覆盖：

1. ordered queue 与两种 policy 的每个 `TaskRunResult` 分支，精确执行次数与后续 Task 短路。
2. 同 exact window 同 ID 的串行和并发 redelivery 均返回 exact ack，factory/worker/Task 无第二次调用。
3. 两个 exact windows 相互隔离；相同文本 ID 不跨 window 串状态。
4. pause/resume 保持 object-identical Task/context/State，pause 不另起 worker、不重跑已完成 Task。
5. stop 在已冻结线程/interrupt/checkpoint 顺序下终止；stop 与 terminal/start race 不泄漏 active ref。
6. success/failure/stopped/skipped/exception、factory/partial materialization/thread-start/close 的 terminal cleanup。
7. terminal 后 current runtime 清零而 last ID/ack 保留；新 ID 获取新 State；defaultState sentinel 始终不变。
8. 无自动 retry、TTL、session/owner/ledger/history；construction inert。

这些测试必须能在没有 40B production 的旧代码上因 public class/API 缺失或行为缺失而失败，不能只断言 test-local fake。

### 13.3 不属于 40B 的测试

- start request 从 HTTP ingress 到 runtime、same host/exchange、fixed scope、close order、startup inert 属于 40C 的 `host/CloudTurnActivationContractTest.java`，计划证据 `:1661`。
- old facade active caller 清零属于 TURN-39 的 `turn/client/OldFacadeRemovalContractTest.java`，当前文件不存在。
- protocol JSON/ack/pause/stop 属于两仓 T01 lifecycle test；host dormant capability 属于 13H host test。40B 不得复制它们来冒充相邻卡验收。

## 14. 当前 blockers 与 stop-work 条件

| ID | 当前事实 | 影响/解除条件 |
|---|---|---|
| `B40-DEP39` | TURN-39 及其 38B1-B4/38C 前置未 source stable | 父级完成/审查 predecessor，并给 39 source-stable 交付后复扫 |
| `B40-TASK-SOURCE` | 三大 Task 调不存在的 context API且有缺失 import；AutoBattle constructor 图也未完成 34A/B/C 收口 | 35/36/37/34C/38A/39 final public source 与 named-test ownership稳定 |
| `B40-FACTORY-SEAM` | host 不扫描/注册 Task，零 real construction caller/provider | 父级基于 final Task constructors 冻结 40B factory consumer 与 40C registration，不能猜接口 |
| `B40-METADATA` | role/team/retry/startup authority 不在 minimal protocol，current context 仍绑 old metadata | 39/前置卡给出 powerless production source或消除 consumers |
| `B40-STATE-OWNER` | GameContext route 仅有非绑定 38M 分析，38C classification 未冻结 | 父级逐 symbol 固定 owner、共享 bean、projection/terminal/new-start合同 |
| `B40-STOP-THREAD` | 四 Task `stop()` 写 mutable GameContext；control-thread 调用不安全 | 父级冻结 signal/interrupt/checkpoint/可选 Task.stop 的线程与顺序 |
| `B40-ACK-INGRESS` | exchange/handler 当前不产 exact ack；40B 无 ingress 写权，40C 也未含 exchange | 父级固定 accepted-before-runtime 时序、caller/API、HTTP failure mapping 与必要写集 |
| `B40-QUEUE-GAPS` | same-ID changed payload、new-ID active、duplicate code、result/exception/ack原子点未固定 | 在权威卡中给出 closed matrix，再实现/写断言 |
| `B40-TERMINAL` | runtime 尚不存在；exchange fence 无 remove API | 固定 runtime cleanup 与 transport fence 分界，不由 40B清 uncertain action |
| `B40-TEST-BUILD` | T01/13H test source 在，但 required Maven/Cloud compile无本轮 fresh证据；T02/T03/T04适用债仍在 | stable writer cohort 后按第19节逐项执行并记录；本 helper 不运行 |
| `B40-WORKTREE` | 两仓大规模 dirty/untracked，相关 Cloud source多为 `??` | claim 前重取 SHA/status/owner，确保无并行同写集；不得清理现有文件 |

任一 blocker 未由父级持久化解除，或任一目标/依赖 hash、caller、write set 发生变化，都应停止领取并刷新 PRECHECK。尤其不得以新增兼容 wrapper、手工 `new Task`、整包扫描、第二 exchange/metadata cache、defaultState、session/TTL/retry/ledger 来绕开。

## 15. 696a12b0 等价与机制排除

- 无已批准业务差异；40B 只能建立 runtime/factory/lifecycle plumbing，不得改变四 Task phase、条件、次数、顺序、delay、fallback、park/terminal 或显式业务 retry。
- start dedupe 不是 Task retry；相同 ID transport redelivery 不产生新 Task/action。
- pause 不替换 context/state；stop 不等待业务 retry；terminal cleanup 不等于删除 uncertain exchange fence。
- 不新增 TTL、sessionId、owner、workflow/operation ledger、payload history、auto retry、startup auto-run、Task singleton、全包 Task scan、额外 metadata authority 或跨线程 mutable State 写入。

## 16. 只读执行记录

- 只读使用 `Get-Content`、`rg`、`Get-FileHash`、`Get-Item`、`Get-Date`、`git status`、`git branch --show-current`、`git rev-parse HEAD`。
- 未运行 Maven/JUnit/compile/package；未启动 runtime/application/server/Task/UI/capture/input。
- 未执行 `git add/commit/checkout/reset/clean/stash/rebase/merge/switch` 或其它 Git mutation。
- 未改 Java、test、权威计划、ACTIVE_WORK、CR/card、matrix/dashboard 或既有报告；除本报告外没有写入。

PRECHECK_COMPLETE TRUE_EOF
