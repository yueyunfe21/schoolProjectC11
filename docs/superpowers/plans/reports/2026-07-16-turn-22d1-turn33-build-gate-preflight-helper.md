# CR271 TURN-22D1 / TURN-33 stable-writer build gate preflight

## 角色与边界

- 角色：CR271 Internal helper，仅做 TURN-22D1 与 TURN-33 build gate 静态预检；不是 implementation
  Worker、reviewer、父级批准者或 runtime 验收人。
- 最终只读快照：`2026-07-16T10:44:02.105-04:00`。
- 已读取：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、TURN-22D1 与
  TURN-33 两张原卡、TURN-22D1 独立 R1/R2、TURN-33 Repair #3 独立 R1/R2、权威计划第 14-19 节、两仓
  `pom.xml`、当前 `TurnInputStepExecutorContractTest` 与 `SummonSkillTurnContractTest` 全文，以及它们当前
  production/public seam 和共享 build/testCompile 表面。
- 仓库现场：DHXY=`thin-client-design`、`85` 个 status entries；Cloud=`navigation-migration`、`28` 个
  status entries。两仓 dirty/deleted/untracked/ignored 原样保护，没有 checkout/reset/clean/stage/commit。
- 本报告不是 review 或批准。未运行 Maven、JUnit、javac、compile/package、runtime/application/server、
  Task/UI/capture/input；未执行 Git mutation；未修改 Java、POM、原卡、计划、ACTIVE_WORK 或其它报告。

## 预检结论

1. 两张卡的 named test 均已由用户明确授权的 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 覆盖，精确命令见下表；
   本次用户明确禁止实际运行 Maven，因此这里只确认授权与命令，不执行。
2. 两仓是两个独立的单模块 Maven project，没有 `<modules>`、parent/reactor 关系或彼此 Maven dependency。
   D1 只在 DHXY 跑 named test + DHXY compile；TURN-33 只在 Cloud 跑 named test + Cloud compile。
3. 当前 **stable-writer gate 未打开**。External C 的 Cloud `TURN-34AT1` Repair #2 已于 `10:36:42`
   交付 `AutoCombatServiceTurnContractTest.java`，但原卡尚无父级 review pass/owner release，仍是未释放的
   test-only owner；该文件属于 Cloud 全量 `testCompile` 表面。External A 的 `TURN-28QP1` 已于 `10:38:00`
   获父级通过并释放，随后领取 Cloud `TURN-28S2`，又于 `10:43:15` 零 Java 字节 `OWNER RETURNED`；父级尚未
   接受该 return。B/D 当前无 claim。未释放 delivery/未被父级接受的 owner return，加上尚未暂停的新 claim
   队列，都不构成 stable-writer window；权威计划禁止此时并发 Maven/`clean`。
4. TURN-33 还存在独立于 writer 状态的高置信静态 build blocker：Cloud main tree 有 `75` 条显式内部 import
   在当前 Cloud source tree 无对应 top-level source，且 Cloud POM 没有 DHXY sibling dependency；main compile
   修复后，test tree 另有 `4` 条同类缺失 import。`-Dtest=SummonSkillTurnContractTest` 只筛 Surefire 执行，
   不会把 Maven `testCompile` 缩成这一类，因此当前不能预期 selected test 能到达 Surefire。

## 精确授权与适用命令

| 卡/门 | 固定工作目录 | 精确命令 | 授权与边界 |
|---|---|---|---|
| TURN-22D1 named test | `D:\mavenProject\DHXY` | `mvn -q -Dtest=TurnInputStepExecutorContractTest test` | `HTTPS_TURN_CONTRACT_TEST_FAMILY` 的 TURN-22 DHXY named test；当前源码唯一同名类，静态 `@Test=12` |
| TURN-22D1 compile | `D:\mavenProject\DHXY` | `mvn -q -DskipTests compile` | AGENTS Java compile gate + 权威计划第 18 节 DHXY cohort；不是 Cloud 命令 |
| TURN-33 named test | `D:\mavenProject\dhxy-cloud-brain` | `mvn -q -Dtest=SummonSkillTurnContractTest test` | `HTTPS_TURN_CONTRACT_TEST_FAMILY` 的 TURN-33 唯一 named test；当前源码唯一同名类，静态 `@Test=19` |
| TURN-33 compile | `D:\mavenProject\dhxy-cloud-brain` | `mvn -q clean compile` | 权威计划第 18 节 Cloud 非测试 source gate；不得添加任何 skip flag |

## 预期模块与类

| 卡 | Maven 模块 | production / supporting surface | 唯一点名测试类 | Surefire 预期 |
|---|---|---|---|---|
| TURN-22D1 | `com.myrobot:DHXY2Robot:1.0-SNAPSHOT` | `com.bot.dhxy.cloud.turn.TurnInputStepExecutor`；共享 `TurnExecutionWindow`、`InputActionRequest/Queue/Worker` frozen action-list API | `com.bot.dhxy.cloud.turn.TurnInputStepExecutorContractTest` | 唯一同名类，当前 `12` tests |
| TURN-33 | `com.yueyunfe.dhxy:dhxy-cloud-brain:0.1.0-SNAPSHOT` | `com.bot.dhxy.service.SummonSkillService`；只读/冻结 `CloudSummonSkillWholePassCapability`、`CloudTaskExclusiveInteractionAuthority` 与 13 份 packaged templates | `com.yueyunfe.dhxy.cloudbrain.service.SummonSkillTurnContractTest` | 唯一同名类，当前 `19` tests |

## 冻结运行顺序

只有父级先确认所有 Java/test owner 已正式释放、接受所有 owner return，并暂停新 claim，才按以下顺序**串行**执行；
不得并发两个 Maven 进程，也不得把后一步当作前一步的替代证据：

1. 在 DHXY 执行 TURN-22D1 named test：`mvn -q -Dtest=TurnInputStepExecutorContractTest test`。
2. 上一步 fresh exit=`0` 后，在 DHXY 执行：`mvn -q -DskipTests compile`。
3. 在 Cloud 执行 TURN-33 named test：`mvn -q -Dtest=SummonSkillTurnContractTest test`。
4. 上一步 fresh exit=`0` 后，在 Cloud 执行：`mvn -q clean compile`。

任一步非零即记录该步真实 first error、exit code 与测试发现数，并停在对应卡的当前门；不得删无关 source/test、
改 POM include/exclude、加 skip flag 或使用旧 `target` 继续制造后续通过。

- D1 子卡不授权以 Cloud `TeamReturnTurnContractTest` 代替本测试；该类属于 TURN-22C1/父卡聚合门，不属于
  TURN-22D1 的单卡 named-test 命令。
- TURN-33 全部卡内 production/test 都在 Cloud；测试虽 import `com.bot.dhxy.*`，这些类型应来自 Cloud 仓自己
  的镜像 source tree，不会从 `D:\mavenProject\DHXY` 自动取类。TURN-33 没有适用 DHXY compile。
- Cloud POM 把 `skipTests`、`maven.test.skip`、`enforcer.skip`、`maven.test.skip.exec`、`surefire.skip` 固定为
  `false` 并在 `validate` 阶段拦截绕过。不得给 Cloud compile/test 增加 `-DskipTests` 等参数。
- `mvn -q clean package` 会运行 Cloud 全测试，不是本次两个 named-test/build-preflight 的隐含授权；按权威计划，
  需用户对该次 package/all-tests 另行明确授权。本 helper 不把 package 写入适用 compile 门。

## POM 与 Maven 生命周期表面

### DHXY

- `pom.xml`：124 行，SHA-256
  `b196481e3f5ec7e7c8d92e87f41a05653885204d7e4ea0b2b0e817e2257275d7`；Java release 21、
  JUnit 5.10.2、compiler 3.11.0、Surefire 3.2.5。
- 无 test compiler includes/excludes、无 Surefire 特殊 includes、无 `module-info.java`。named-test 命令进入
  Surefire 前会先编译 DHXY 全部 main source 和全部 test source。
- 当前对 DHXY `src/main/java` 与 `src/test/java` 做显式 `com.bot.dhxy.*` import 路径解析，main/test 均为
  `0` 个缺失。这只排除显式内部 import 缺文件，不证明方法签名、泛型、构造器或 javac 全树成功。

### Cloud

- `pom.xml`：193 行，SHA-256
  `f40967034f88e9b73eaf83a348df199d4bb62cbdf23c3034a950bfe20891a6a3`；Java release 21、
  JUnit 5.10.2、compiler 3.11.0、Surefire 3.2.5、Enforcer 3.5.0、Antrun skip-flag guard。
- 无 sibling DHXY dependency、无 test compiler includes/excludes、无 `module-info.java`。Surefire 的 `-Dtest`
  过滤发生在 main compile 和全 testCompile 之后。
- `SummonSkillTurnContractTest` 使用相对路径 `src/main/java` 做已批准的 source gate，并读取 classpath template；
  因此必须从 Cloud 根目录运行。其列出的 13 份 `images/template/zhaohuanshou/...` 资源当前全部存在。

## TURN-22D1 当前表面

- production `TurnInputStepExecutor.java`：264 行，SHA-256
  `a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e`。
- named test `TurnInputStepExecutorContractTest.java`：695 行，SHA-256
  `f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81`，12 个 `@Test`。
- 两个 SHA 与 D1 原卡、独立 R1/R2 采用值一致；父级已于 `10:00:30` 写明独立 review `2/2 APPROVED`、
  `BUILD PENDING`。本 helper不重新 review 或改写该结论。
- 当前 named test 的显式内部 imports 全可在 DHXY 模块解析；`MultiWindowTaskManager`/`WindowTaskRunner`
  test subclass 的 `super(...)` 实参数量与当前 22/20 参数构造器一致；覆盖的
  `submitFrozenExactWindowActionsAndWait(String, WindowRuntimeContext, WindowNativeBinding, List<InputAction>)`
  与当前 public API 签名一致。
- `sun.misc.Unsafe` 只用于分配 test-owned runner；在本预检中它不是“缺失内部 import”。是否产生 JDK warning、
  module/运行时限制只能由授权 named test 的真实 compiler/JVM 结果确认。
- 共享依赖当前 SHA：`InputActionRequest/Queue/Worker` 分别为 `7f4f8fdc...`、`c53a423e...`、
  `225a9f3b...`；`InputActionFrozenExclusiveContractTest` 为 `f72c7db0...`（1283 行/19 tests）。QT1 已交付并
  释放；QP1 的单行 `java.util.Objects.equals(...)` symbol-resolution 修复也已于 `10:38:00` 父级通过并释放。
  这些共享字节尚无 fresh compile/test 证据；且全局 owner window 仍未稳定，因此不能把 D1 named test/compile
  当成 stable-writer fresh 证据。

## TURN-33 当前表面

- production `SummonSkillService.java`：1431 行，SHA-256
  `991db945f7d621e86287d7dadb121bc9154dfe7375f6176cb4ca71434bcaed04`。
- named test `SummonSkillTurnContractTest.java`：1683 行，SHA-256
  `6a755b0fb36152afd90ff59244c74cbeae086360dd8b53bf2f492cc83f968998`，19 个 `@Test`。
- SHA 与 TURN-33 原卡 Parent Review #5、独立 R1/R2 一致；原卡状态仍是 `DUAL REVIEW PASSED / BUILD
  PENDING`。本 helper不重新 review 或批准。
- named test 自身所有显式内部 imports 当前均能在 Cloud main/test tree 解析；两个 Repair #3 第五次
  generated-delete fixture、production public API harness、13 份 packaged assets 均仍在当前源码。
- 同一 Cloud test tree 的 `AutoCombatServiceTurnContractTest.java` 已于 `10:36:42` 交付 Repair #2，当前
  SHA=`b5438da5...`、1026 行/22 tests，但尚无父级 review pass/owner release；它不是 TURN-33 的测试类，却会被
  Maven 在 Surefire 选择 TURN-33 前一并 `testCompile`。

## 潜在静态 build/testCompile blocker

### DHXY / D1

- 未发现 D1 自身或 DHXY 全树的显式内部 import 缺文件 blocker。
- 仍不能写“testCompile 可通过”：当前 frozen request/queue/worker 与 1283 行共享 contract test 都从未 fresh
  compile；Maven 会 testCompile 全树，任何同树语法/API 首错都能在 D1 selected class 执行前阻断。
- 因此 D1 当前没有已证明的本类 javac 错误；它的门是 **未编译 shared cohort + 尚未由父级宣布稳定的全局
  writer window**。旧报告中的“TURN-28QT1 active”已失效，不再作为当前 blocker。

### Cloud / TURN-33

- main compile 前置静态候选共 `75` 条。代表性直接缺类：
  - `DialogService.java` imports 当前 Cloud source/POM 均无来源的 `GameClientTracker`、`InputProvider`、
    `CoordinateHelper`、`ObjectiveTextRecognitionService`、`WindowRuntimeContext`、`WindowScopedTempPath`、
    `WindowTaskContextHolder`；
  - `NavigationService.java`、`NpcClickService.java` 及三个 whole Task 还有同类缺失。
- 因 Cloud POM 没有 DHXY artifact dependency，这些不是由 sibling workspace 自动补齐的类型。真实 Maven 的
  first compiler error 尚未产生，但按当前 source/POM，TURN-33 named-test 命令会高概率在 main `compile`
  阶段退出，`Tests run=0`，尚未进入 `testCompile` 或 Surefire。
- 即使 main compile cohort 先修复，当前 test tree 还有 `4` 条显式缺失 import：
  - `PlayerStateTurnContractTest.java:31` -> `com.bot.dhxy.vision.LocationVisionService`；
  - `FiveRingTaskTrackerTurnContractTest.java:31-33` -> `GameStateUtil`、`WindowRuntimeContext`、
    `WindowTaskContextHolder`。
  这些无关测试也会被 Maven 全量 testCompile，因此仍可阻止 `SummonSkillTurnContractTest` 到达 Surefire。
- TURN-34AT0 先前两个不存在的 `.remote` LocalServiceClient imports 已在当前
  `AutoCombatServiceTurnContractTest.java` SHA=`4b8460b0...` 修为 `.turn.client`，并于 `09:59:30` 父级
  source review 通过；它们不再属于上述缺失 import 清单。当前 AT1 Repair #2 已交付 SHA=`b5438da5...`，但 owner
  尚未释放，AT2+ 也仍是后续 test tranche；整体尚未 compile。
- 以上是只读文本/路径解析，不是 Maven/javac verdict。最终 blocker 归属必须以 stable-writer 窗口中的 fresh
  first error 为准，不能复制历史首错。

## writers 何时算稳定

以下条件同时满足才可打开本次 gate：

1. 两仓所有 true-EOF `CLAIMED/ACTIVE/REPAIR owner` 均已通过正式 delivery + 父级 owner release，或正式
   `OWNER RETURNED` + 父级接受；不能以 lane 静默、mtime 暂停或 Worker 自述“停止”代替释放。
2. TURN-34AT1 Repair #2 必须取得父级 review pass/owner release；TURN-28S2 的 A `OWNER RETURNED` 必须由
   父级正式接受。其它已领取 Java 片也必须无 active/unreleased owner。READY/assigned 但未 claim 的卡本身不是
   writer，但父级必须在 gate 命令期间暂停新 claim，避免 `clean` 与新写入并发。
3. 父级在最后一个 owner 释放后重新读取相关原卡 true EOF，并复算本报告列出的 D1、TURN-33、共享 API/test、
   两份 POM SHA；任一 drift 都先回到相应 source/review gate，不能沿用旧测试结论。
4. 在命令开始到结束期间，两仓 Java/test/POM 保持只读。只有“不再有人能合法写入”，而不只是某一瞬间没有
   mtime 变化，才是 stable-writer window。

按 `10:44:02` 快照，TURN-34AT1 Repair #2 delivery 尚待父级释放，TURN-28S2 的零字节 owner return 尚待父级
接受，且父级尚未宣布暂停新 claim，故当前明确 `WRITER WINDOW NOT STABLE / DO NOT RUN MAVEN`。

## 父级执行时的结果分类

1. 命令前复核 cwd、test/POM/shared SHA；不要用 IDE、stale `target` 或 sibling repo classpath 代替。
2. 若 selected-test 命令在 main compile 失败：记录真实 first error、exit code 与 `Tests run=0`，状态写
   `MAIN COMPILE COHORT BLOCKED / SELECTED TEST NOT RUN`，不能写“named test failed/passed”。
3. 若进入 testCompile 后在别的 test class 失败：记录 `TEST-COMPILE COHORT BLOCKED / SELECTED TEST NOT RUN`；
   不得删除无关测试、改 POM include 或加 skip flag来制造通过。
4. 只有 Surefire 实际发现唯一目标类，D1 报告预期 12 tests、TURN-33 报告预期 19 tests，且完整命令
   exit=`0`、Failures=`0`、Errors=`0`，才满足各自 named-test 门。
5. named test 与适用 compile 都 fresh exit=`0` 后，才把结果交父级作卡片裁决；本 helper 的 PRECHECK 不构成
   `CARD APPROVED/CLOSED`。

## 未执行记录

- Maven/JUnit/javac/compile/package：未运行。
- runtime/application/server/Task/UI/capture/input：未运行。
- Git stage/commit/checkout/reset/clean 或其它 mutation：未执行。
- 唯一写入：本 preflight 报告。

TRUE_EOF PRECHECK_COMPLETE
