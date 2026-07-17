# TURN-14 — Bag typed facade and caller cutover

## READY / PARENT FROZEN BRIEF

- 状态：`READY`；类型：`INTEGRATION`；`countDelta=0`；startDependsOn：TURN-13 SOURCE APPROVED。
- Cloud 唯一 Java 写集：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudBagUseIncensePort.java`
  - `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
  - `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  - 新 `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudBagLocalServiceClient.java`
  - 本报告。
- 目标：把现有 Bag local macro 调用改为 HTTPS turn 的 `BAG_RETURN_ITEM/BAG_USE_INCENSE` closed action；保留
  prescan 策略、缓存点、caller 顺序、TaskCheckpoint 与 terminal 映射。新 client 使用现有 `CloudTurnActionFactory`
  与 `CloudTurnCommandPort`，解析真实 `TurnOutcome/TurnStepResult.localResultJson`；不得伪 outcome、自动 retry、
  owner/session/ledger 或修改永久本地 `BagService`。
- 若现有 command port 尚无合法注入/activation 边界，写 `BLOCKED` 并给精确证据，不得偷改 TURN-40/Server。
- 禁止修改 TURN-15/16 文件、Task、协议、DHXY、server/routes；不运行 Maven/tests/runtime/Git。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## CLAIMED

- 领取时间：`2026-07-15T17:03:16-04:00`；状态：`CLAIMED`；类型：`INTEGRATION`；`countDelta=0`。
- Worker：CR271 Internal implementation Worker；父级是唯一 manager/final reviewer，不自批、不领取下一卡。
- 唯一 Cloud Java 写集：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudBagUseIncensePort.java`
  - `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
  - `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudBagLocalServiceClient.java`
- 只按 `696a12b0` 等价把 Bag closed macro caller 改为 HTTPS turn action；Task、协议、Server/routes、TURN-40、
  DHXY 与永久本地 `BagService` 均禁止修改。若 command port 缺合法注入/activation，精确 `BLOCKED`，不造 stub。
- 保护两仓全部 dirty/untracked；不回滚/覆盖他人改动，不运行 Maven/tests/runtime/application/Task/poller/UI/
  capture/input，不执行 Git mutation。

`CLAIMED`

## BLOCKED — WRITE-SET-OUTSIDE COMMAND PORT ACTIVATION PREREQUISITE

- 阻断时间：`2026-07-15T17:04:37-04:00`；状态：`BLOCKED`；`countDelta=0`。
- 阻断结论：当前没有可由 TURN-14 四文件合法复用/注入的真实 `CloudTurnCommandPort`。继续写
  `CloudBagLocalServiceClient` 只能得到不可激活的 facade，或被迫私建第二个 `CloudTurnExchange`；后者与 HTTP ingress
  使用的真实 slot 分离，无法收到 DHXY outcome，违反 frozen contract。

### Exact evidence

- `CloudBrainServer.java:87-90` 在 Server wiring 内直接以 `new CloudTurnExchange()` 创建唯一 HTTP turn bundle；
  `:92-93` 只从 bundle 取 `turnHandler()` 与 `templateHandler()` 注册 context，没有把 command capability 交给
  Cloud service graph。
- `CloudTurnRoutes.java:43-55` 虽在 `Bundle` 保存同一 `CloudTurnCommandPort`，但 `:66-68` 的
  `commandPort()` 是 package-private，且当前全仓没有调用者。
- 全仓 `CloudTurnCommandPort` 引用仅有接口、`CloudTurnExchange implements` 与 `CloudTurnRoutes.Bundle` 字段；不存在
  `@Bean`、host registration 或 caller constructor wiring。`CloudTurnActionFactory` 同样没有现有业务 caller。
- `CloudServiceConfiguration.java:24-29` 只扫描 `com.bot.dhxy.service`，并只 import `BotProperties`、
  `CloudCommonBoxPortAssembly`、`CloudTeamReturnPortAssembly`；它既不扫描新
  `com.yueyunfe.dhxy.cloudbrain.turn.CloudBagLocalServiceClient`，也不提供 `CloudTurnCommandPort` Bean。
- `CloudServiceHost.java:40-45` 仅注册 scope、storage 与上述 configuration 后 refresh；没有接收或注册 Server 创建的
  bundle/command port。因此在本卡内给 service/client 增加 constructor dependency 会让 host 缺 Bean，不能构成真实切流。

### Impact and unblock condition

- 影响：`CloudBagUseIncensePort`、`ReturnItemPrescanService`、`PlayerStateService` 仍只能看到旧 retained
  `context.getGameClient().executeLocalMacro(...)` 边界；本轮没有修改其 caller 顺序、prescan 策略、缓存、
  `TaskCheckpoint` 或 terminal 映射。
- 解阻必须由父级在 TURN-14 写集外先建立**同一个** Server `CloudTurnExchange` 到 Cloud service host 的显式
  injection/activation 边界，并决定 host/scope/window 的 command-port wiring。涉及候选文件至少为
  `CloudBrainServer.java`、`CloudTurnRoutes.java`、`CloudServiceHost.java` 或 `CloudServiceConfiguration.java`，属于
  Server/TURN-40/host integration 范围，本 Worker 无权修改。
- 禁止的伪解法：在新 client 内 `new CloudTurnExchange()`、nullable/optional fallback 到旧 macro、自动注册、stub
  outcome 或 retry。上述做法均不能复用 HTTP handler 的真实 slot。

### Protected source evidence

- TURN-14 三个现有 Cloud Java 未修改，当前 SHA-256：
  - `CloudBagUseIncensePort.java`：
    `DB5886566CF7312FCB27FA39CAF7F8F9DB527717017CE490E0DDF2C5381187C0`
  - `ReturnItemPrescanService.java`：
    `3D78417E2834AD332FCE26037E72116224FFC4727914C732287299CAF81E21BD`
  - `PlayerStateService.java`：
    `1A690CBC0830B7DF47F77F3E1C82019FBFBCFBC05735F5236E43EEB455CB49D8`
- `CloudBagLocalServiceClient.java` 仍不存在；未创建 stub。
- 未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

`BLOCKED / WRITE-SET-OUTSIDE PREREQUISITE`

## PARENT SOURCE REVIEW #1

- 审查时间：`2026-07-15T17:08:30-04:00`；结论：`P0/P1/P2=0/1/0 / BLOCKED`。
- 父级独立读取 `CloudBrainServer.java:87-93`、`CloudTurnRoutes.java:43-68`、
  `CloudServiceHost.java:35-46`、`CloudServiceConfiguration.java:23-30`，并执行全仓
  `CloudTurnCommandPort|CloudTurnExchange|commandPort(` 引用扫描。Worker 的阻断事实成立：HTTP handler 与
  command caller 必须共享 Server 创建的同一个 `CloudTurnExchange`，但该 capability 当前既未暴露给 host，也未
  注册为 host Bean；全仓没有第二个合法 provider/caller。
- 影响：TURN-14 写集内无法形成真实 Bag turn 闭环；新增 client 或替换 caller 会成为不可激活 source-only facade。
- 返修/解阻条件：先由新前置卡 `TURN-13H` 建立同一 exchange 到 dormant `CloudServiceHost` 的显式构造与 Bean
  注入边界；只允许惰性 wiring，不启动 host/server/loop，不做用户 activation。`TURN-13H` 父级源码批准后原样重发
  TURN-14。禁止第二 exchange、static holder/service locator、nullable fallback、stub outcome 或自动 retry。

`PARENT BLOCKED / P1=1`

## AUTHORITATIVE REISSUE #2 / READY - 2026-07-15 20:33 EDT

- 原 `P1=1` 已由 `TURN-13H` 的同源 command-port/catalog 惰性 host capability 与 `TURN-13C` 的 exact bound
  `TurnGameClient` 源码通过而解除；旧 brief 的 `turn/` 路径和“无测试”口径作废。
- 状态：`READY`；startDependsOn：`TURN-02R`、`TURN-13C` source review passed。
- 唯一 production write set：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudBagUseIncensePort.java`
  - `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
  - `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  - 新 `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`
- 唯一 test write set：
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ReturnItemPrescanTurnContractTest.java`；本报告可写。
- 完成边界：把 prescan 与普通 incense caller 改为一个真实 HTTPS `LOCAL_SERVICE` closed action，严格解析
  `TurnLocalServiceResult` 与 `localResultJson`；保留 prescan slot/priority/cache/caller 顺序和 terminal/checkpoint
  语义。FiveRing open-main-bag caller 明确留给 TURN-36。
- named test 必须覆盖 COMPLETED/FAILED/STOPPED/uncertain、exact operation/window/args、一次 public invocation
  一个 UUID/一个 command、raw frame 对应和无自动 retry。Worker 不运行 Maven；父级在 writer 稳定后运行。
- 禁止第二 exchange、fallback 到旧 macro、stub outcome、owner/session/ledger/TTL、Task/DHXY/Server/routes 修改。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-14 authoritative reissue 2 -->

## CLAIMED — AUTHORITATIVE REISSUE #2 IMPLEMENTATION

- 领取时间：`2026-07-15T20:40:31-04:00`；状态：`CLAIMED`；卡片：`CR271 TURN-14`。
- Worker：`CR271 TURN-14 implementation Worker`，不是 reviewer；父级是唯一 manager/final reviewer。
- Exact production write set：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudBagUseIncensePort.java`
  - `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
  - `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  - 新 `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`
- Exact test/report write set：
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ReturnItemPrescanTurnContractTest.java`
  - `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-14.md`
- 其余两仓文件全部只读；共享 dirty workspace 中不回滚、覆盖或清理他人改动。
- 仅按 authoritative reissue #2 实施 prescan 与普通 incense caller 的同源 HTTPS `LOCAL_SERVICE` closed action；
  FiveRing open-main-bag 留给 TURN-36。禁止第二 exchange、旧 macro fallback、stub、自动 retry、
  owner/session/ledger/TTL，以及 Task/DHXY/Server/routes 修改。
- 本 Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行 Git mutation。

`CLAIMED / AUTHORITATIVE REISSUE #2`

## SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING — 2026-07-15T21:25:52-04:00

- Worker：`CR271 TURN-14 implementation Worker`；本节仅为实施交付，不是 review/Approved 结论。
- 状态：`SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING`；`countDelta=0`。

### Production source delivered

- 新 `turn/client/CloudBagLocalServiceClient` 复用 host graph 中唯一真实 `TurnGameClient`；两个 public action
  各直接调用一次 `TurnGameClient.localService(...)`，分别发送 `BAG_RETURN_ITEM` 与无参数组的
  `BAG_USE_INCENSE`，每次 action 请求 `fullWindowFailureEvidence=true`，无第二 exchange、无 retry/fallback。
- `BAG_RETURN_ITEM` 三个 intent 的参数形状逐项冻结：task-page=`target/-1/null/source`，from-back=
  `target/0..4/null/source`，cached-use=`null/-1/optional-cache/source`；cache point 五字段完整投影。
- 只对真实 `COMPLETED` step 严格解析 private nested JSON record；拒绝 missing/extra/duplicate/trailing、
  scalar coercion、numeric enum、intent/state/cache shape mismatch。`FAILED -> NOT_EXECUTED`，`STOPPED` 与
  command/duplicate uncertainty 保持独立 terminal；失败 raw PNG 校验 purpose/content-type/SHA/dimensions/
  exact full-window region/sourceStepIndex，并防御性复制。
- `ReturnItemPrescanService` 仅把原三处 macro transport 替换为上述 client；prescan strategy/priority、
  forced slot、4s+8..18s combat delay、cache/invalidate/completeRound、caller 顺序及直接 checkpoint 保持。
- `CloudBagUseIncensePort` 保持原前后两个直接 `TaskCheckpoint` 和 terminal 映射：USED=true，NOT_FOUND/
  NOT_EXECUTED=false，STOPPED/UNKNOWN=fatal。`PlayerStateService` 仅将普通 incense lambda/Bean 字段切到该 port。
- `ensureSheYaoXiangActiveInOpenMainBag(BagService.MainBagSession, ...)` 原方法逐字未改，Cloud `BagService`
  Bean 字段已移除；该 FiveRing open-main-bag caller 按 authoritative reissue #2 留给 TURN-36。

### Named test source delivered

- 新 `ReturnItemPrescanTurnContractTest` 共 `12` 个 `@Test`，使用真实 `TurnGameClient` + scripted
  `CloudTurnCommandPort`，覆盖 `BC4+BASE+LS+IMG`：COMPLETED/FAILED/STOPPED/uncertain、两类 prescan + cached
  use + ordinary incense、exact device/window/operation/argument union、120s timeout、每次有效 public action
  一个 UUID/一个 command、零自动 retry、stop-before 零 command、stop-after checkpoint、raw frame/defensive copy、
  strict JSON，以及 4s+8..18s baseline park。
- 既有 Cloud `.gitignore:15` 忽略整个 `src/test/`；named test 物理文件已交付且静态读取正常，但不会出现在普通
  untracked status 中。未修改 `.gitignore`，父级集成时需按既有 test-source 流程纳入。

### File inventory / SHA-256

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudBagUseIncensePort.java`：
  `348B2FF662D0ECD9F4C2CE6AC17744724047E70BB53E49BF6AD128F70C207C29`
- `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`：
  `FE31D4C9F13C4347639707346088445429737CE106D3C2B04EE7D3890AC5BEE6`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`：
  `511A831D3718792B158500891BFADFA03D827B7936ECBDB19ACEEBA65B2A4E33`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`：
  `477A2777584F96BEDB1F58983E1EB02A7832137626A7014DEFC8BA8225C80F32`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ReturnItemPrescanTurnContractTest.java`：
  `F682DF23AC9ACB8F820113818D7510C83E3520E2B6E85C7EC09E7945BB037E97`
- 本报告为 append-only 自引用交付载体，正文不嵌入会因本次追加而立刻失效的自身 SHA；父级可在 true EOF
  复核其最终原字节 SHA。

### Static self-check

- 四个 production 文件：`TurnGameClient.localService(`=`2`，public Bag action=`2`；`executeLocalMacro`=`0`，
  legacy Bag/LocalMacro type=`0`，`new CloudTurnExchange`=`0`。ReturnItem client call=`1`，PlayerState ordinary
  port call=`1`、旧 `bagService.findAndUseItem`=`0`，open-main-bag method=`1`。
- 将 `PlayerStateService` 本轮四项内容替换反向撤销并恢复原 CRLF 后，SHA 精确回到 claim 前
  `1A690CBC0830B7DF47F77F3E1C82019FBFBCFBC05735F5236E43EEB455CB49D8`，证明其余大类内容未改。
- 从 preserved candidate 精确删除已批准移除的 `hasCached(...)` 后，重建 claim 前 ReturnItem SHA 精确为
  `3D78417E2834AD332FCE26037E72116224FFC4727914C732287299CAF81E21BD`；当前与其 public API diff=`0`，
  strategy candidate order 与 `4000/8000/18000ms` 常量均 exact。
- 五个 Cloud source/test 文件 trailing whitespace=`0`、均有 final newline；PlayerState 恢复全 CRLF，其余保持 LF。
- 未创建/修改 Task、DHXY production、Server/routes、协议、owner/session/ledger/TTL/stub；未回滚、覆盖或清理
  共享 dirty workspace 的任何他人改动。

### Baseline and unrun gates

- 已核对 `docs/业务逻辑.md:215-224` 基线/选择性变更门、`:228-281` 回程不加等待/二次验证、`:470-471`
  五倍回城道具预扫时点、`:1253-1294` 修罗 `696a12b0` fallback/STOP/启动热恢复。本卡没有批准或引入业务差异：
  **无已批准业务差异；按 `696a12b0` 基线等价迁移。**
- 按 authoritative reissue #2 明令，Worker **未运行** Maven/JUnit/test/compile/package/runtime/application/server/
  Task/UI/capture/input；这些门没有伪造通过，交由父级在 writer 稳定及适用 build cohort 到齐后执行。
- 未执行任何 Git mutation（无 add/commit/checkout/reset/clean/stash/branch）。

`SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-15T21:35:07-04:00

- 结论：`P0/P1/P2=0/1/0 / REPAIR #1 REQUIRED`；本结论来自父级独立逐文件源码与 named-test
  源码审查，不采用 Worker 自述，也未运行 Maven。
- 已核对五个交付文件的最终 SHA-256 与报告清单一致；写集、真实 `TurnGameClient.localService(...)`、
  一个 public invocation 一个 UUID/command、三种 Bag intent、ordinary incense、terminal 映射、raw failure PNG、
  strict JSON、TaskCheckpoint、prescan 顺序/cache/fallback 与 `696a12b0` 基线均未发现其它 P0/P1/P2。

### P1 - FOUND cache point 未绑定本次请求模板

- 精确证据：`CloudBagLocalServiceClient.executeReturnItem(...)` 在 `:84-95` 构造并发送含
  `targetItemTemplate` 的 request，但 `mapResult(...)` 在 `:273-283` 只校验返回 `intent`，随后
  `toDomainCachePoint(...)` 在 `:385-400` 仅要求 `cachePoint.templatePath` 非空，未要求其等于本次请求的
  `targetItemTemplate`。`ReturnItemPrescanService.java:272-280` 会把这个返回点直接写入本轮 state，
  `:170-194` 后续会按该点执行 cached use。现有
  `ReturnItemPrescanTurnContractTest.java:428-478` 的 strict JSON 负例没有覆盖 FOUND 返回另一模板路径。
- 影响：若 local result 因适配器回归、错响应关联或坏 JSON 返回了另一物品的合法 cache point，Cloud 会把它
  当成本次目标的扫描结果缓存，并在回程阶段点击错误物品。当前 DHXY
  `BagLocalOperationExecutor`/`BagService` 的正常实现确实从请求模板生成点，但这不能替代 HTTPS typed result
  边界的 request/result 关联校验。
- Repair #1 返修条件（仅原写集）：让 `executeReturnItem(...)` 将 prescan 请求的规范
  `targetItemTemplate` 传入结果映射；仅当 `PRESCAN_TASK_PAGE/PRESCAN_FROM_BACK + FOUND` 时要求返回
  `cachePoint.templatePath` 与请求模板精确相等，不改变 cached-use、source、坐标或 terminal 语义。给同一
  named test 增加 mismatch 负例，断言抛出 typed contract error，且仍只有一个 UUID、一个 command、零 retry。
  禁止改协议、DHXY、本地 adapter、caller、Task、Server/routes 或引入 fallback/retry。

`PARENT REVIEW BLOCKED / P1=1 / REPAIR #1 RETURNED TO ORIGINAL WORKER`

## Repair #1 SOURCE+TEST SOURCES DELIVERED / PARENT RE-REVIEW PENDING — 2026-07-15T21:42:06-04:00

- Worker：`CR271 TURN-14 implementation Worker`，不是 reviewer；本节仅交付 Parent Review #1 的 P1
  返修源码与 test source，不作 Approved/Done 结论。
- 状态：`Repair #1 SOURCE+TEST SOURCES DELIVERED / PARENT RE-REVIEW PENDING`；仅修改
  `CloudBagLocalServiceClient.java`、`ReturnItemPrescanTurnContractTest.java`，并在本报告 true EOF
  append 本节；未触碰原 write set 的其它文件。

### Repair #1 source evidence

- `CloudBagLocalServiceClient.java:84-99` 仍先由 `returnItemArguments(...)` 校验并规范请求参数，再把真实发送的
  `arguments.targetItemTemplate()` 传入 result mapping；`:246-274` 只把该 request-bound 值继续传给单次
  completed-result parser，不增加 exchange、command 或 retry。
- `CloudBagLocalServiceClient.java:279-300` 先严格解析真实 `localResultJson` 并校验 intent；`:287-297` 对
  `PRESCAN_TASK_PAGE/PRESCAN_FROM_BACK + FOUND` 使用 `String.equals(...)` 将请求规范模板与原始解析出的
  `cachePoint.templatePath` 精确关联，另一模板（包括仅靠 trim 才相等的值）立即抛出
  `IllegalArgumentException`，不会形成可写入 prescan state 的 `BagOperationOutcome`。FOUND/null 仍由既有
  outcome shape fail closed。
- `USE_CACHED_RETURN_ITEM` 不属于上述 intent 条件；incense 在 `:125` 传 null 且走独立 operation parser。
  cached-use、source、坐标校验、terminal/checkpoint/caller 语义均未修改。

### Repair #1 named-test evidence

- 新 `ReturnItemPrescanTurnContractTest.java:481-516`
  `foundPrescanRejectsMismatchedCachePointTemplateWithoutRetry()` 分别覆盖 `PRESCAN_TASK_PAGE` 与
  `PRESCAN_FROM_BACK`：请求 `bag/requested_return_item.png`，typed FOUND result 返回
  `bag/other_return_item.png`，断言 `IllegalArgumentException` 及 expected/actual 模板证据。
- 同一 mismatch case 在 `:509-515` 复核 exact `BAG_RETURN_ITEM` union/intent/request template，并显式断言
  UUID=`1`、command=`1`、action=`1`；`:646-669` 的既有 `bagArguments/assertExactActions` 继续校验 exact
  operation、window binding、UUID/action id、failure-evidence flag 与 120s timeout，因此每个 public
  invocation 都是一个 UUID/一个 command、零自动 retry。
- named test source 现为 `13` 个 `@Test`；本轮只新增上述 mismatch contract test，未改协议或本地 adapter。

### Repair #1 exact SHA-256

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`：
  `9CFC71F4583133E3F293FD5AEF8547F73B39B5111D7FB6892BD4F1A2A59FDD4F`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ReturnItemPrescanTurnContractTest.java`：
  `1AD4CF6166FFB8E9156734F7745CF8F2BFDE2FC9D2351654C2B5FEB7AB159753`
- 本报告为 append-only 自引用交付载体，不嵌入追加后会立即失效的自身 SHA；父级可在 true EOF 读取后复核。

### Repair #1 static self-check / unrun gate

- client 文本计数：真实 `turnGameClient.localService(`=`2`；`executeLocalMacro/LocalMacro/new CloudTurnExchange`=`0`；
  `retry` token=`0`。两个本轮 source/test 文件 trailing whitespace=`0`，均保留 final LF。
- 未修改协议、DHXY、本地 adapter/caller、Task、Server/routes、owner/session/ledger/TTL，也未引入
  fallback、第二 exchange 或 retry。
- 按本轮明令，Worker **未运行** Maven/JUnit/test/compile/package/runtime/application/server/Task/UI/capture/input；
  未伪造任何门禁通过。未执行任何 Git mutation（无 add/commit/checkout/reset/clean/stash/branch）。

`Repair #1 SOURCE+TEST SOURCES DELIVERED / PARENT RE-REVIEW PENDING`

## PARENT RE-REVIEW #2 - 2026-07-15T21:55:00-04:00

- 父级独立重读 `CloudBagLocalServiceClient` 的 request 构造、completed-result parser、domain shape guard，
  以及 `ReturnItemPrescanTurnContractTest` 新 mismatch 用例；重算两文件 SHA 与 Repair #1 报告完全一致。
- 结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。`PRESCAN_TASK_PAGE` 与
  `PRESCAN_FROM_BACK` 的 `FOUND` 只有在返回 `cachePoint.templatePath` 与真实发送的规范
  `targetItemTemplate` 按 `String.equals` 精确相等时才可形成 outcome；null point 仍由既有 FOUND shape
  fail-closed，`USE_CACHED_RETURN_ITEM` 与 incense 路径不受影响。
- 新负例逐 intent 断言 typed `IllegalArgumentException`、请求/返回路径证据、UUID=`1`、command=`1`、
  action=`1`；没有第二 exchange、fallback 或 retry。Repair #1 原 P1 已关闭，无新增 P0/P1/P2。
- 本结论仅是 production/test source review passed；named test 与适用 Cloud compile/build 仍进入全部 Java
  writers 稳定后的父级 cohort，因此当前不冒充 `CARD APPROVED/CLOSED`。

`SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD COMPILE PENDING`
