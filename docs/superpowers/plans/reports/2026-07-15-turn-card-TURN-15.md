# TURN-15 — UICleaner typed facade

## READY / PARENT FROZEN BRIEF

- 状态：`READY`；类型：`INTEGRATION`；`countDelta=0`；startDependsOn：TURN-13 SOURCE APPROVED。
- Cloud 唯一 Java 写集：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudUiCleanerPort.java`
  - 新 `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudUiCleanerLocalServiceClient.java`
  - 本报告。
- 目标：四个 public UI 方法保持原 terminal/TaskCheckpoint 语义，只把旧 executeLocalMacro 改为 HTTPS turn
  `UI_CLEAN_ALL/UI_CLOSE_GENERIC_WINDOWS/UI_CLEAN_LIGHTWEIGHT/UI_CLOSE_MAP_SEARCH_INPUT_BY_X2`；严格解析真实
  step result JSON，不伪 outcome、不自动 retry，不把 OCR/策略搬回 DHXY。
- 若 command port 缺合法注入/activation 边界，写 `BLOCKED` 并给精确证据，不得改 TURN-40/Server。
- 禁止修改 TURN-14/16、DHXY UICleanerService、协议、server/routes；不运行 Maven/tests/runtime/Git。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## CLAIMED

- 领取时间：`2026-07-15T17:02:32-04:00`；状态：`CLAIMED`；`countDelta=0`。
- 唯一 Java 写集：Cloud `remote/CloudUiCleanerPort.java`、新
  `turn/CloudUiCleanerLocalServiceClient.java`；唯一文档写集为本报告。
- 实施边界：按 `696a12b0` 等价保留四个 public terminal/`TaskCheckpoint`，仅把旧
  `executeLocalMacro` 替换为 HTTPS turn `LOCAL_SERVICE` action；不改 TURN-40/Server/routes/protocol，
  不造 stub，不运行 Maven/tests/runtime/Git，保护全部 dirty/untracked。

`CLAIMED`

## BLOCKED

- 交付时间：`2026-07-15T17:03:46-04:00`；状态：`BLOCKED / WRITE-SET-EXTERNAL COMMAND PORT ACTIVATION`；
  `countDelta=0`。
- Java 交付：无。未修改 `CloudUiCleanerPort.java`，未创建
  `CloudUiCleanerLocalServiceClient.java`；禁止用无法接通真实 ingress 的 stub/第二 exchange 冒充交付。

### 精确证据

1. 真实 `/api/v1/client/turn` ingress 所使用的 exchange 只在写集外
   `CloudBrainServer.java:87-93` 内以 `new CloudTurnExchange()` 手工创建，并立即封装进
   `CloudTurnRoutes.Bundle` 后注册两个 HTTP handler；server 没有把同一实例交给 Cloud service host。
2. `CloudTurnRoutes.java:42-68` 虽在 bundle 内保留 `CloudTurnCommandPort`，但
   `Bundle.commandPort()` 是 package-private。现有 `CloudUiCleanerPort` 位于 `remote` 包，Cloud host 位于
   `host` 包；授权两个文件内没有合法方式取得 server 正在使用的同一 command port。
3. `CloudServiceHost.java:35-46` 创建独立 Spring context 时只注册 scope、storage 与
   `CloudServiceConfiguration`。`CloudServiceConfiguration.java:23-30` 只扫描
   `com.bot.dhxy.service`，并仅 import 既有 CommonBox/TeamReturn assemblies；没有注册
   `CloudTurnCommandPort`、`CloudUiCleanerPort` 或新 UI-clean turn client。
4. `CloudUiCleanerPort.java:21-27` 当前虽标注 `@Component`，但它所在
   `com.yueyunfe.dhxy.cloudbrain.remote` 不在上述 host component scan 内；其现有唯一构造依赖也只有
   `TaskExecutionContextHolder`。把新 client 加为字段会要求写集外 configuration/assembly 提供该 bean 与真实
   server command port。
5. `CloudTurnCommandPort.execute(...)` 必须发布到 HTTP ingress 共享的同一个 single-slot exchange。
   在新 client 内自行 `new CloudTurnExchange()` 会创建与 `CloudBrainServer.java:88` 不相通的第二 slot，DHXY
   永远收不到 action；静态全局 accessor/service locator 同样是未批准 activation 层和假接线。

### 影响与解除条件

- 当前两个 Java 写集不足以形成真实 Cloud caller -> shared `CloudTurnCommandPort` -> HTTPS turn ingress -> DHXY
  `LOCAL_SERVICE` 闭环；继续编码只能得到不可运行 stub 或平行 exchange，违反冻结卡。
- 解除阻断必须由父级另行分配精确写集，在现有 server/host activation 边界中把
  `CloudBrainServer` 创建的同一 `CloudTurnCommandPort` 注入对应 tenant/user 的 Cloud service context，并注册
  UI-clean client/port bean；不得让 TURN-15 自行修改 TURN-40、Server/routes 或扩大 activation 设计。
- activation 卡 SOURCE APPROVED 后，本卡可原样重领，再在两个授权 Java 文件内完成四个 public
  terminal/`TaskCheckpoint` 的 `696a12b0` 等价切换与真实 step-result JSON 严格解析。
- 未运行 Maven/tests/runtime/Git；保护全部 dirty/untracked。父级是唯一 reviewer。

`BLOCKED`

## PARENT SOURCE REVIEW #1

- 审查时间：`2026-07-15T17:08:30-04:00`；结论：`P0/P1/P2=0/1/0 / BLOCKED`。
- 父级独立核对 `CloudBrainServer.java:87-93`、`CloudTurnRoutes.java:43-68`、
  `CloudServiceHost.java:35-46`、`CloudServiceConfiguration.java:23-30` 与全仓 command-port 引用；确认真实
  `/turn` exchange 只存在于 Server 局部 bundle，Cloud service host 没有同源 command-port Bean，且
  `CloudUiCleanerPort` 所在 remote 包不在 host component scan 内。
- 影响：两个授权 Java 文件无法闭合四个 UI operation 的真实 HTTPS turn 链；继续编码只会得到断开的第二 slot 或
  无法装配的 facade。
- 解阻条件：`TURN-13H` 先建立共享 exchange 的惰性 host capability/Bean 注入边界；不得启动 runtime，也不得提前
  执行 TURN-40 的用户可见 activation。前置源码批准后原样重发 TURN-15。

`PARENT BLOCKED / P1=1`

## AUTHORITATIVE REISSUE #2 / READY - 2026-07-15 20:33 EDT

- 原 `P1=1` 已由 `TURN-13H` 同源 host capability 与 `TURN-13C` exact bound client 源码通过而解除；旧
  `turn/` 路径和“无测试”口径作废。
- 状态：`READY`；startDependsOn：`TURN-02R`、`TURN-13C` source review passed。
- 唯一 production write set：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudUiCleanerPort.java`
  - 新 `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudUiCleanerLocalServiceClient.java`
- 唯一 test write set：
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/UiCleanerTurnContractTest.java`；本报告可写。
- 完成边界：四个 public UI operation 使用真实 HTTPS `LOCAL_SERVICE` closed action，并严格保留各自
  terminal/TaskCheckpoint 映射；X2 仍是一个 closed local macro，不在 Cloud 或 DHXY 重新嵌套 input queue。
- named test 必须覆盖 COMPLETED/FAILED/STOPPED/uncertain、四个 exact operation/window/args、一次 public
  invocation 一个 UUID/一个 command、成功/失败 JSON 和无自动 retry。
- 禁止第二 exchange、旧 macro fallback、stub、owner/session/ledger/TTL、DHXY UICleaner/Server/routes 修改。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-15 authoritative reissue 2 -->

## CLAIMED - AUTHORITATIVE REISSUE #2 IMPLEMENTATION

- 领取时间：`2026-07-15T20:39:37-04:00`。
- 身份：`CR271 TURN-15 implementation Worker`；不是 reviewer；父级是唯一 manager/final reviewer。
- Exact production write set（Cloud）：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudUiCleanerPort.java`
  - 新 `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudUiCleanerLocalServiceClient.java`
- Exact test write set：
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/UiCleanerTurnContractTest.java`
- Exact report write set：本报告。
- 其余 DHXY/Cloud 文件全部只读；共享 dirty workspace 中不回滚、覆盖或清理他人改动，不做 Git mutation。

`CLAIMED / IMPLEMENTATION IN PROGRESS`

## SOURCE + TEST SOURCES DELIVERED / PARENT REVIEW PENDING

- 交付时间：`2026-07-15T20:58:12-04:00`。
- 身份保持：`CR271 TURN-15 implementation Worker`；不是 reviewer；本节不构成 Approved，父级仍是唯一
  manager/final reviewer。
- 状态：`SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING`；`countDelta=0`。

### 交付文件与 SHA-256

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudUiCleanerPort.java`
   - SHA-256：`4830E9FB93FC1FE9FC25A71DA637089E42F4A36AA9A070B931E488E86EA8B076`
   - `138` 行，`6394` bytes。
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudUiCleanerLocalServiceClient.java`
   - SHA-256：`8061365BBB742231C576E242896F0C21A1752018D418C94550671BB7A6579901`
   - `203` 行，`9263` bytes。
3. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/UiCleanerTurnContractTest.java`
   - SHA-256：`D497AC7E3F031884BF6BB24BF5C2383CE5F713AA91CB5AA4158049B2C733BB76`
   - `492` 行，`21940` bytes。
4. 本报告已在真实 EOF 追加领取与交付记录；报告不自录 SHA，避免写入 SHA 后再次改变自身内容。

### Production 实施摘要

- `CloudUiCleanerPort` 保留四个原 public signature、`120000ms` timeout，以及每次 command 前后各一次
  `TaskCheckpoint.throwIfStopRequested(context, ...)`；无 active context 仍 fail-closed。
- 四个调用精确映射为 `UI_CLEAN_ALL`、`UI_CLOSE_GENERIC_WINDOWS`、`UI_CLEAN_LIGHTWEIGHT`、
  `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2`。前两个 `ui=null`，后两个只携带 normalized nonblank `source`；Bag/Give/Quest
  arguments 全为 null。
- 新 client 通过 host 已冻结窄扫描中的同一个 constructor-injected `TurnGameClient`，每次 `execute(...)` 只调用一次
  `turnGameClient.localService(call, false, timeout)`；`@Import(CloudUiCleanerPort.class)` 只把对应 facade 精确注册进该
  dormant host graph，不创建 exchange/host/runtime/thread。
- terminal 等价映射：`COMPLETED + COMPLETED step + exact JSON` -> `EXECUTED`；`FAILED + FAILED step` ->
  `NOT_EXECUTED`；`STOPPED` -> `STOPPED`；`DUPLICATE_OR_UNCERTAIN` 及所有 non-completed command status ->
  `UNKNOWN`。facade 随后保持旧映射：boolean 的 `NOT_EXECUTED=false`、void no-op，`STOPPED/UNKNOWN` 抛
  `TaskFatalException`。
- `localResultJson` 只接受 exact `{"operation":"<exact enum>","handled":<boolean>}`；拒绝 operation mismatch、
  缺 `handled`、未知字段、尾随 token、FAILED 携带 JSON，以及 `UI_CLEAN_ALL handled=false`。
- X2 在 Cloud 仍只有一个 `LOCAL_SERVICE` step/一个 command；未增加 INPUT step、第二 command 或 Cloud queue。
  DHXY 已有 X2 local adapter/`UICleanerService` queue ownership 全部只读未改。

### Test source 覆盖与静态自检

- 唯一 named test 使用 scripted `CloudTurnCommandPort`、fixed UUID supplier 与 turn-native exact context；不启动
  application/server/Task/runtime，不触发 UI/capture/input。
- 七个 test method 覆盖：四 public operation 的 exact device/window/args；每 public invocation 一 UUID/一 command；
  `handled=true/false` JSON；四 operation 的 FAILED 映射；STOPPED、outcome uncertain、timeout uncertain、BUSY；
  malformed JSON；checkpoint 前 stop/command 后 stop；invalid address/source 在 UUID/command 前拒绝。
- X2 断言 action 恰一条 `LOCAL_SERVICE` step，`inputAction/input` 均 null，且该 operation 恰一个 command；无自动 retry。
- 限定 production 写集静态搜索：`turnGameClient.localService` 恰一个调用点；无 `executeLocalMacro`、
  `CloudTurnExchange`、`CloudBrainServer`、`CloudTurnRoutes`、`InputSequences`、`submitExclusiveAndWait` 或 retry 实现。
- 三份 Java/test source 的行尾空白扫描零命中；四个 facade public signature 均仍存在。
- Cloud 当前 `.gitignore:15` 忽略整个 `src/test/`，所以 named test 物理文件存在但不会出现在普通 `git status`；本卡无权
  修改 `.gitignore`，且按指令未执行任何 Git mutation。父级 review/test gate 应直接读取上述绝对 test path。
- 其余 DHXY/Cloud 文件均只读；未修改 DHXY `UICleanerService`、Server/routes、协议或任何并行 Worker 文件。

### 未运行门

- 按父级冻结命令，本 Worker **未运行** `mvn -q -Dtest=UiCleanerTurnContractTest test`。
- 未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；未执行 Git mutation。
- 因此当前只交付 production/test source 与静态证据，等待父级源码审查、断言审查及后续 fresh named test/compile gate。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

`SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE + TEST-SOURCE REVIEW #1 - 2026-07-15 21:05 EDT

- 父级独立逐行审查两份 production、完整 named test、`TurnGameClient`/`TurnLocalServiceResult`、
  TURN-10B 本地 adapter、冻结协议和旧 `UI_CLEAN` terminal 合同；Worker 自述不作为批准依据。
- 结论：`P0/P1/P2=0/1/0 / REPAIR #1 REQUIRED`。
- 已通过部分：四个 public signature、120 秒、前后 `TaskCheckpoint`、四个 exact operation/source、每次调用
  一个 `LOCAL_SERVICE` command/UUID、X2 单 closed macro、FAILED/STOPPED/uncertain terminal 映射，以及无第二
  exchange/旧 macro fallback/自动 retry，均与冻结 brief 和 `696a12b0` 等价。
- **P1-1 strict JSON 实际未闭合：**
  `CloudUiCleanerLocalServiceClient.java:31-34` 只启用 unknown-property 与 trailing-token 检查；未启用 duplicate
  detection、missing/null creator gate，也未关闭 scalar coercion。相邻已审同类 client 已使用这些 strict 门。
  `UiCleanerTurnContractTest.java:194-228` 只覆盖 mismatch/missing/extra/trailing/clean-all false，未覆盖 duplicate
  key、numeric enum、string boolean 与 null。影响是畸形 local result 可能被 Jackson coercion/last-key-wins 接受，
  进而被 facade 当成真实 `EXECUTED/handled`，违反 fail-closed JSON 边界。
- Repair #1 精确写集仅限：
  `turn/client/CloudUiCleanerLocalServiceClient.java`、`service/UiCleanerTurnContractTest.java` 与本报告；
  `CloudUiCleanerPort.java` 和其余两仓文件全部只读。要求与已审 Quest/Bag client 对齐 strict mapper，并新增上述
  四类负例，每例保持一个 UUID/command、无 retry。不得改变 operation/terminal/checkpoint/X2 业务语义。
- 三份交付 SHA-256 已由父级重算并与报告一致。Java writers 活动期间不运行 Maven；返修后先父级复审，
  named test/compile 仍进入稳定 writer cohort。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-15 parent review 1 repair required -->

## REPAIR #1 SOURCE + TEST SOURCE DELIVERY - 2026-07-15 21:09 EDT

- 返修时间：`2026-07-15T21:09:05-04:00`。
- 状态：`REPAIR #1 SOURCE+TEST SOURCE DELIVERED / PARENT RE-REVIEW PENDING`；本 Worker 不是 reviewer，
  本节不构成 Approved。
- Repair #1 实际写集严格为：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudUiCleanerLocalServiceClient.java`
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/UiCleanerTurnContractTest.java`
  - 本报告。
- `CloudUiCleanerPort.java` 全程只读，复算 SHA-256 仍为
  `4830E9FB93FC1FE9FC25A71DA637089E42F4A36AA9A070B931E488E86EA8B076`，与 Parent Review #1 前一致。

### Repair #1 文件 SHA-256

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudUiCleanerLocalServiceClient.java`
   - SHA-256：`7EB79EB3E9A21C627C3FF6994ECA5746E0013498559A10A75E7308F6CD97BDE1`
   - `214` 行，`9886` bytes。
2. `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/UiCleanerTurnContractTest.java`
   - SHA-256：`AEFFA45A74A8D637DABF6614C39BE85F9CE51193102876034922718AD27EEDBC`
   - `504` 行，`22715` bytes。
3. 本报告继续不自录 SHA，避免自引用改变。

### P1-1 修复内容

- `RESULT_MAPPER` 已与已审 Quest/Bag strict boundary 对齐：启用
  `JsonParser.Feature.STRICT_DUPLICATE_DETECTION`、`FAIL_ON_UNKNOWN_PROPERTIES`、
  `FAIL_ON_TRAILING_TOKENS`、`FAIL_ON_MISSING_CREATOR_PROPERTIES`、
  `FAIL_ON_NULL_CREATOR_PROPERTIES`，显式启用 `FAIL_ON_NUMBERS_FOR_ENUMS`，并关闭
  `MapperFeature.ALLOW_COERCION_OF_SCALARS`。
- private result record compact constructor 对 `operation`、`handled` 再做 nonnull gate；strict parse 同 Quest/Bag
  一样将 `JsonProcessingException` 或 creator/runtime rejection 统一转为 fail-closed `IllegalArgumentException`。
- named test 新增四类精确负例：
  - duplicate key：同一 JSON 两次 `handled`；
  - numeric enum：`operation:3`，该 ordinal 若被接受会恰好伪装成
    `UI_CLOSE_GENERIC_WINDOWS`，因此可真实检出 numeric-enum coercion；
  - string boolean：`handled:"true"`；
  - null creator：`handled:null`。
- 四类负例均进入既有逐例断言：`uuids.calls=1`、`executeCalls=1`、`actions.size=1`；异常后没有第二 command/retry。
  原 mismatch、missing、extra、trailing-token、`UI_CLEAN_ALL handled=false` 负例全部保留。

### 不变项与静态门

- 未修改 operation 构造、terminal 映射、120 秒 timeout、checkpoint、X2 closed macro 或任何 public facade 语义。
- 限定 client 静态扫描仍只有一个 `turnGameClient.localService(...)` 调用点；无第二 exchange、旧 macro fallback、
  input queue、retry、owner/session/ledger/TTL。
- 两份返修 Java/test source 行尾空白扫描零命中；其余两仓文件全部只读。
- 按冻结要求未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，未执行 Git mutation。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

`REPAIR #1 SOURCE+TEST SOURCE DELIVERED / PARENT RE-REVIEW PENDING`

## PARENT RE-REVIEW #1 - 2026-07-15 21:16 EDT

- 父级独立复读 Repair #1 后的完整 client、完整 named test 与只读 facade，并重算三份 Java/test source
  SHA-256；Worker 自述不作为批准依据。
- 结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。原 `P1-1` 已关闭：production mapper
  同时启用 duplicate detection、unknown/trailing/missing/null creator 与 numeric-enum 拒绝，并关闭 scalar
  coercion；private result record 对 `operation/handled` 再做 nonnull gate。异常统一 fail-closed，不能形成
  `EXECUTED/handled`。
- named test 直接穿过真实 `CloudUiCleanerPort -> CloudUiCleanerLocalServiceClient -> TurnGameClient` 边界，新增
  duplicate `handled`、numeric enum、string boolean、null creator 四类负例；每例均断言 `UUID=1`、
  `command=1`、`action=1`，异常后无第二 command/retry。原 mismatch/missing/unknown/trailing 与
  `UI_CLEAN_ALL handled=false` 负例保留。
- `CloudUiCleanerPort.java` SHA-256 仍为
  `4830E9FB93FC1FE9FC25A71DA637089E42F4A36AA9A070B931E488E86EA8B076`；client/test SHA-256 分别为
  `7EB79EB3E9A21C627C3FF6994ECA5746E0013498559A10A75E7308F6CD97BDE1`、
  `AEFFA45A74A8D637DABF6614C39BE85F9CE51193102876034922718AD27EEDBC`，与 Repair #1 交付表一致。
- 四个 public signature、120 秒 timeout、前后 checkpoint、exact operation/source、FAILED/STOPPED/uncertain
  terminal、X2 单 `LOCAL_SERVICE` closed macro 及无第二 exchange/旧 macro fallback/自动 retry 均无漂移。
- 其余 Java writers 仍活动，本轮不运行 Maven/JUnit/compile。当前状态是源码与测试源码审查通过，仍需 fresh
  `UiCleanerTurnContractTest` 与适用 Cloud compile/build cohort；不是 `CARD APPROVED/CLOSED`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-15 parent re-review 1 passed -->
