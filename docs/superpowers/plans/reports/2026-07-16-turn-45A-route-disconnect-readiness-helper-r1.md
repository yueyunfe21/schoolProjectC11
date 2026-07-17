# PRECHECK

- 角色与范围：本报告是 CR271 `TURN-45A old route disconnect` 的非实现、非评审、非父级预检，只审计当前
  `CloudBrainServer`、`RemoteTaskRunRoutes`、`RemoteTaskRunEndpoint`、`CloudApiGateway`、old route assembly、
  新 HTTPS turn routes 以及跨仓 path caller。报告不生成最终删除 manifest，不修改或删除生产/测试源码，不替任何
  owner 领取或实施卡。
- 已完整读取：`AGENTS.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、
  `2026-07-16-turn-44M45M-readiness-preflight-helper.md`、
  `2026-07-16-turn-44M45M-scc-decomposition-helper-r1.md`，以及当前两仓相关 production/test source。
- 当前行号均来自本次读盘，不是未来冻结行号。Cloud `CloudBrainServer.java` 与 `CloudApiGateway.java` 均为 tracked
  modified；`RemoteTaskRunRoutes.java`、`RemoteTaskRunEndpoint.java` 及 17 文件 SCC 均为 untracked。未来 40C、39、
  44M45M 或并发 writer 改变任一字节后，必须重新扫描，不能把本报告当作 patch 或 hash 授权。
- 写入前只读快照：DHXY=`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`，tracked dirty
  `44`、untracked `598`；Cloud=`navigation-migration` /
  `3b988caa010254973e03342272e6d1d6a9685b01`，tracked dirty `9`、untracked `541`。全部 dirty/untracked 均视为
  受保护输入。本报告创建后 DHXY untracked 计数自然增加一项，不代表清理或归属变化。
- 当前不是 40C 完成态：`host/CloudTurnRuntimeConfiguration.java`、
  `turn/runtime/CloudTurnTaskRuntime.java` 与 `host/CloudTurnActivationContractTest.java` 均不存在；
  `CloudServiceHost.create(...)` 仍无 production caller；`CloudBrainApplication` 也尚未接收计划要求的
  tenantId/userId/stateRoot。故当前 dirty Server 只能用于源图预检，不能用于预制 45A 修改。
- 计划中的最终 Cloud 44/45 manifest 文件当前不存在，本报告没有创建它，也没有记录 byte size、SHA-256 或最终
  文件分类。
- 本轮没有运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input；
  没有执行 Git mutation、暂存、提交、回滚、覆盖、清理或删除。

## 证据

**1. 权威依赖与 45A 可独立成立的前提**

1. 权威计划第 16 节给 `TURN-44M45M` 的前置为 `TURN-41 + TURN-39`；45A 又要求
   `44M45M + 40C parent freeze`。因此 45A 的真实输入必须同时包含：39 后 old facade/context 引用图、40C 后
   Server/host/runtime 最终 wiring、41 的用户 fresh runtime 门，以及父级冻结的逐文件 manifest。
2. 权威计划第 17 节固定 Cloud 删除顺序为 `45A -> 44A -> 45B`。45A 只断 Server 对 old route assembly 的
   构造与注册，44A 才处理 authority/context SCC，45B 只接收 44A 后的零引用 wire/transport 残余。
3. 权威计划第 19 节只给 45A 一个 source guard 和一个 Cloud compile gate。它没有授权改 gateway、host、turn
   handlers、17 文件 SCC、DHXY client 或业务 Task/Service。
4. 当前 40B/40C 激活源码尚未出现，最终 manifest 也尚未出现，所以本次只给 exact future write boundary 与
   停工条件，不形成可执行删除批次。

**2. 45A exact production/test write set**

| 类型 | 精确路径 | 未来动作 | 当前状态与边界 |
|---|---|---|---|
| production | `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java` | 修改 | tracked modified；只移除 40C final source 中的 old route 构造、持有和 gateway 加入点，保留其余 HTTP、host/runtime 与 close wiring。 |
| production | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteTaskRunRoutes.java` | 删除 | untracked；Server 是其唯一显式 production caller。文件本身装配四条 old route，并构造 old authority graph。 |
| production | `src/main/java/com/yueyunfe/dhxy/cloudbrain/api/RemoteTaskRunEndpoint.java` | 删除 | untracked；唯一显式 production 构造点是 `RemoteTaskRunRoutes.java:63`。 |
| test | `src/test/java/com/yueyunfe/dhxy/cloudbrain/OldRemoteRouteRemovalGuardTest.java` | 新建 | 当前不存在；权威计划点名的唯一 45A test write set。 |

以上四项是 45A 的完整 production/test 边界。以下文件均只读，不得为让 45A 编译而顺手修改：

- `gateway/CloudApiGateway.java`、`CloudApiRoute.java`、`CloudApiEndpoint.java`；
- `turn/CloudTurnRoutes.java`、`CloudTurnHttpHandler.java`、`CloudTemplateHttpHandler.java`、
  `CloudTurnExchange.java`、`CloudTurnCommandPort.java`、`CloudTemplateCatalog.java`；
- `CloudBrainApplication.java`、`host/CloudServiceHost.java`、`host/CloudServiceConfiguration.java` 以及 40C 将创建的
  `host/CloudTurnRuntimeConfiguration.java`；
- 本报告第 6 项列出的 17 文件 SCC；
- DHXY `HttpRemoteTaskRunApiClient.java`、`HttpRemoteCommandTransport.java`、`HttpsTurnClient.java`、
  `TurnConfiguration.java`；
- 现有 `CloudTurnRoutesContractTest.java`、`CloudTurnHttpHandlerContractTest.java`、
  `CloudTemplateHttpHandlerContractTest.java`、`CloudServiceHostTurnCapabilityContractTest.java`。

当前 Server 内 old-route-specific 写点为：

- imports：`CloudBrainServer.java:4` 的 `GameContext` 与 `:12` 的 `RemoteTaskRunRoutes`；
- path 常量：`:42-44`；
- bundle 强引用：`:51`、constructor `:58`、assignment `:63`；
- assembly 构造与 gateway route 加入：`:86-91`；
- Server constructor 实参：`:103`。

这些行只用于说明当前引用闭包。40C 会重写同一文件，未来 worker 必须针对 40C final source 重新定位。Endpoint
依赖的 coordinator、action/request/response/error/receipt/run DTO 不属于 45A 写集；删除 endpoint 不等于可在本卡
删除这些类型。

**3. old HTTP path、endpoint 与跨仓 inbound refs**

| old HTTP path | Cloud 注册链 | DHXY 当前 production path ref | 当前处理合同 |
|---|---|---|---|
| `/api/cloud/remote/poll` | `CloudBrainServer.java:42,87` -> `RemoteTaskRunRoutes.java:59` -> `PollEndpoint:85-105` -> broker | `HttpRemoteCommandTransport.java:24,72` | root gateway 精确 path；POST；Bearer；long-poll request。 |
| `/api/cloud/remote/outcome` | `CloudBrainServer.java:43,88` -> `RemoteTaskRunRoutes.java:60` -> `OutcomeEndpoint:108-123` -> broker | `HttpRemoteCommandTransport.java:25,73` | root gateway 精确 path；POST；Bearer；outcome completion。 |
| `/api/cloud/remote/outcome/final-consumed-receipt` | Server outcome 常量 -> `RemoteTaskRunRoutes.java:61-62` 派生 suffix -> final receipt ingress | `HttpRemoteCommandTransport.java:26-27,74-75` | root gateway 精确 path；POST；Bearer；final-consumed receipt。 |
| `/api/cloud/remote/task-run` | `CloudBrainServer.java:44,89` -> `RemoteTaskRunRoutes.java:63` -> `RemoteTaskRunEndpoint` | `HttpRemoteTaskRunApiClient.java:23,54,790` | root gateway 精确 path；POST；Bearer；old lifecycle action。 |

`RemoteTaskRunRoutes.java:37-43` 的说明文字称“三条”route，但 `:58-63` 实际创建四个 `CloudApiRoute`；45A guard
必须按四个真实 URL 证明注册已消失，不能只检查三个 Server 常量。

Cloud `src/main`/`src/test` 全量显式搜索结果：

- `RemoteTaskRunRoutes` 的组件外 production 引用只有 `CloudBrainServer.java:12,51,58,86`；测试零引用。
- `RemoteTaskRunEndpoint` 的 production 构造引用只有 `RemoteTaskRunRoutes.java:10,63`；测试零引用。
- 三个 Server old path 常量没有第二个 Cloud production/test 定义；第四个 path 只由 route factory 拼接。
- `CloudApiGateway` 的 production 构造 caller 只有 `CloudBrainServer.java:95`，handler registration 只有
  `CloudBrainServer.java:108`。它仍承载非 45A 路由，不能随 old route 一并移除。

DHXY 当前显式源码仍保存四个 old URL 的 concrete client，但未找到这些 concrete class 的外部 `new`、Spring
bean、component 或 configuration 构造点：

- `HttpRemoteTaskRunApiClient` 只有自身 declaration/constructor；
- `HttpRemoteCommandTransport` 只有自身三个 constructor；
- `RemoteCommandPollingLoop` 与 `RemoteTaskRunLifecycleService` 只接收接口依赖，也未找到 production 构造点；poller
  注释明确只能由显式 `start()` 启动。

这只能证明当前显式 wiring 是 dormant，不能证明未来 runtime 或反射绝无 caller，也不能替代 TURN-41/39 与最终
manifest。若 45A 开始前出现任何真实构造、bean、启动入口或 active request caller，必须停止本卡，先由前置卡移除。

**4. 当前 Cloud bean/manual wiring 全链**

old route 的唯一显式装配链为：

```text
CloudBrainApplication.java:12
  -> CloudBrainServer.start(...)
  -> RemoteTaskRunRoutes.create(...)
  -> coordinator + broker + ledger + finalCoordinator + authority assembly
  -> four CloudApiRoute values
  -> routes.addAll(remoteRoutes.routes())
  -> new CloudApiGateway(token, routes)
  -> HttpServer.createContext("/", gateway::handle)
```

- `CloudBrainApplication.java:12` 是当前唯一 production `CloudBrainServer.start(...)` caller；Server、route factory、
  endpoint 和 gateway 均不是 Spring bean，也没有 component annotation。
- `CloudApiGateway.java:35-36` 按 decoded exact path 查 map，`:42-52` 统一执行 POST/Bearer 门，`:53-61` 再把 JSON
  交给 endpoint。45A 只从 route collection 中移除四个 old entries，不应改 gateway dispatch/auth/JSON 行为。
- `RemoteTaskRunRoutes.java:50-57` 构造 old graph，`:58-64` 生成 routes 并返回 bundle；
  `AuthorityRouteBundle:68-82` 通过 `authorityAnchor` 强持有 assembly。Server 的 bundle field 被移除后，这个 graph
  才失去 Server registration/ownership 入边。
- 当前 `CloudServiceHost.create(...)` 只在 `CloudServiceHostTurnCapabilityContractTest.java:55,78,106-112` 出现；
  production 没有 host create caller。40C 将改变这一事实，所以 45A 不得依据当前 dormant host 删除
  `turnCommandPort`、template catalog、host 或 lifecycle 字段。

**5. 必须保留的新 HTTPS turn ingress 证明**

Cloud 当前新链不经过 old `CloudApiRoute` collection：

```text
CloudBrainServer.java:96-99
  -> CloudTurnRoutes.create(shared CloudTurnExchange, PackagedTemplateAssets, token)
  -> CloudBrainServer.java:106 createContext(CloudTurnHttpHandler.PATH, turnHandler)
  -> CloudBrainServer.java:107 createContext(CloudTemplateHttpHandler.PATH_PREFIX, templateHandler)
```

- `CloudTurnHttpHandler.java:27` 固定 `/api/v1/client/turn`，`:105-118` 要求 exact path、无 query、POST、唯一 Bearer；
  `CloudTemplateHttpHandler.java:23` 固定 `/api/v1/templates/`，`:50-75` 要求 GET、Bearer 和合法 prefix path。
- `CloudTurnRoutes.java:29-40` 用同一个 `CloudTurnExchange` 和 catalog 构造两个 handler，并把同一 command port/catalog
  暴露给 host/runtime wiring；`:62-75` 是 capability getter。45A 不能创建第二 exchange/catalog，也不能删这些
  capability。
- DHXY `TurnConfiguration.java:10-22` 已把 `HttpsTurnClient` 注册为 `TurnClient` bean，`:25-40` 把该 bean 交给
  template cache 与 turn loop factory；`HttpsTurnClient.java:47-48,74,158` 真实使用
  `/api/v1/client/turn` 与 `/api/v1/templates/`。
- 现有 `CloudTurnRoutesContractTest.java:27-83` 证明 bundle 内共享 exchange/catalog 和 handler 行为；handler 两个
  contract test 证明 path/method/auth/body 合同；host capability test 证明同一 capability 可进入 host。这些测试均不
  直接检查 `CloudBrainServer.createContext(...)`，所以未来 45A source guard 必须补上 Server registration 断言，
  不能只引用现有测试名称。
- `CloudBrainServer.java:108` 的 root gateway 还承载 `/api/cloud/decision`、
  `/api/cloud/route-memory/outcome`、`/api/cloud/route-memory/migrate`、
  `/api/cloud/npc-click-smart/outcome`、`/api/v1/xiuluo/brain/start` 和 `/api/cloud/ocr/health`。45A 不能通过删除
  root context 或整个 gateway 来移除 old route。

**6. 与 17-file 44A SCC 的只读边界**

45A 对以下 `17/17` 文件的动作数必须为零；它们当前全部为 Cloud untracked，仍是受保护源码：

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionState.java`
5. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`
6. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunActionLedger.java`
7. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java`
8. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java`
9. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCurrentContextSlot.java`
10. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java`
11. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunRetainedLifecycleActivationAdapter.java`
12. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java`
13. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`
14. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteFinalConsumptionCoordinator.java`
15. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java`
16. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java`
17. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTransactionAction.java`

45A 只移除 `RemoteTaskRunRoutes` 对 SCC 的四类外部构造/持有边：

- `RemoteTaskRunRoutes.java:51` -> `RemoteGameCommandBroker`；
- `:52` -> `CloudTaskRunActionLedger`；
- `:53-54,61-62,127-138` -> `RemoteFinalConsumptionCoordinator`；
- `:55-57,64,68-82` -> `CloudTaskRunAuthorityAssembly`/authority anchor。

它不会改变 SCC 内边。当前 `CloudTaskRunActionLedger.java:676-803` 仍直接消费 broker nested types，
`RemoteGameCommandBroker.java:385-637` 仍反向消费 `CloudTaskRunActionLedger.CompactionPlan`。所以 45A 后预期状态是：
Server 已不再构造 old graph，但 17 文件仍完整存在并可一起编译。若为让 45A 编译而需要修改其中任何文件，说明
45A 写集或 predecessor 图已变化，不能把修改并入本卡。

active Task/Service 使用的 old facade/port/result/macro、permanent-local Service model 与 NPC reference/shadow 也
全部在 45A 之外。本卡不能因为 route 断开就把仍被 Task/Service 编译调用的类型判作可移除对象。

**7. 45A 文件级先后顺序与编译点**

前置顺序：

1. 39、40B/40C/40D、41 与 44M45M parent freeze 先完成；基于最终 Server/source graph 重新取 exact refs/hash。
2. 确认 Cloud/DHXY 均无 active old route caller，确认 40C host/runtime 与 close order 已稳定，确认没有并发 writer
   占用 Server 或 45A test path。
3. 在同一 45A cohort 中创建唯一 guard test；它在 production cohort 完成前预期不能通过，不作为提前删文件的依据。

production 文件顺序：

1. 先修改 `CloudBrainServer.java`，移除 old imports/constants/bundle field/constructor argument、
   `RemoteTaskRunRoutes.create(...)`、`routes.addAll(remoteRoutes.routes())` 与 old bundle constructor 实参；同时原样
   保留 40C final host/runtime/close wiring、新 turn/template contexts、root gateway 其它 routes。
2. Server 不再引用后，再删除 `RemoteTaskRunRoutes.java`。这一步同时去掉 endpoint 唯一构造 caller和四条 old
   gateway entries。
3. route factory 不再存在后，再删除 `RemoteTaskRunEndpoint.java`。不能先删 endpoint 后留一个引用缺失类型的 route
   source。
4. 三个 production 动作与 guard test 必须作为一个 compile cohort 完成；中间不宣称存在独立交付编译点。
5. 45A gate 完成后重新扫描 Cloud 全生产图；只有这个新快照才能作为 44A 输入。44A 不得与 45A 并行，45B 又必须
   等 44A 后重算残余。

**8. future named-test 与 Cloud compile gate**

未来唯一 test path：
`src/test/java/com/yueyunfe/dhxy/cloudbrain/OldRemoteRouteRemovalGuardTest.java`。

未来执行顺序与精确命令：

1. 在 `D:/mavenProject/dhxy-cloud-brain` 运行
   `mvn -q -Dtest=OldRemoteRouteRemovalGuardTest test`。
2. 点名 test fresh exit 0 后，在同仓运行 `mvn -q clean compile`。

本 helper 没有执行上述命令。未来 guard 必须是无 server/application/runtime 启动的 source guard，并逐项证明：

- `RemoteTaskRunRoutes.java` 与 `RemoteTaskRunEndpoint.java` 两个 exact path 不存在；
- production 中 `RemoteTaskRunRoutes`、`RemoteTaskRunEndpoint`、三个 Server old path 常量及派生的第四个 old URL
  均无注册引用；
- `CloudBrainServer` 仍构造 40C final turn bundle/runtime，并仍注册
  `CloudTurnHttpHandler.PATH=/api/v1/client/turn` 与
  `CloudTemplateHttpHandler.PATH_PREFIX=/api/v1/templates/`；
- `CloudApiGateway` root context 和六条非 45A gateway route 仍在；
- 17 个 SCC exact path 仍存在且未进入本卡 diff；byte/hash 不变由 frozen manifest/diff 证据逐文件核对；
- 检查使用 exact path/symbol/URL 列表，不能用 `remote/**`、类名前缀、目录整体不存在或宽泛字符串扫描替代。

现有 turn/handler/host tests 是只读支撑证据，不属于 45A test write set，也不能替代 Server-level source guard。
45A 不运行 whole package，不改 POM，不用 skip/enforcer 选项，不启动 loopback server。

**9. STOP-WORK 条件**

出现以下任一条件立即停止本卡并交父级刷新依赖、manifest 或写集，不在 45A 内自行扩张：

1. 40C final source、`CloudTurnRuntimeConfiguration`、host/runtime lifecycle 或 close order 尚未落定；或者 40C 后
   `CloudBrainServer` 的 ref/hash 与 frozen manifest 不一致。
2. 39/41/44M45M 前置尚未满足，最终 manifest 未冻结，或 manifest 中两个删除路径与当前字节/引用不一致。
3. Cloud Server 外出现 `RemoteTaskRunRoutes` caller，route factory 外出现 `RemoteTaskRunEndpoint` caller，或 DHXY
   old HTTP client 出现真实 bean/new/start/active request wiring。
4. 45A 需要第五个 production/test 文件，或需要改 `CloudApiGateway`、turn handlers/routes、host/runtime、DHXY
   client、17 文件 SCC、active Task/Service 或协议 DTO。
5. 删除 old route 的方案同时移除/改写 `/api/v1/client/turn`、`/api/v1/templates/`、共享 exchange/catalog、
   root gateway 非 45A routes 或 40C close order。
6. 方案依赖 wildcard、目录/前缀删除、宽泛 `Remote*` 匹配，或把仍有 active Task/Service caller 的类型当作本卡
   清理对象。
7. named source guard 需要启动 application/server/runtime/Task、发送真实 HTTP、capture/input，或不能逐项证明四个
   old URL 与两个新 ingress。
8. 点名 test 或 Cloud compile 的首个真实错误要求改写集外文件；应先归属错误和刷新 card，不能顺手修复。
9. Cloud 仍有 Java writer 活动，不能安全运行 `clean`；或任一受保护 dirty/untracked 在实施期间出现无法归属的并发
   变化。

**10. 本 helper 实际写入与保护结论**

- 唯一实际写入：
  `docs/superpowers/plans/reports/2026-07-16-turn-45A-route-disconnect-readiness-helper-r1.md`，首次创建。
- 没有写 Java、test、权威计划、ACTIVE_WORK、CR271、矩阵或 dashboard；没有生成最终 manifest，没有删除任何文件。
- 当前证据支持的最小 45A 边界是 `3` 个 production 文件加 `1` 个 named source guard；它对 17 文件 SCC 为
  `0/17` 写入。该边界只有在 predecessor 与 parent freeze 完成且 source graph 未漂移时才可由未来实施者使用。

PRECHECK_COMPLETE true EOF
