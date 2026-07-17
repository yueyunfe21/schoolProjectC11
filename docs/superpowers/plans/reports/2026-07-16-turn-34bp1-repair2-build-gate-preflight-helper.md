# CR271 TURN-34BP1 Repair #2 stable-writer named-test / Cloud compile gate preflight

## 角色、范围与结论级别

- 角色：`CR271 Internal helper`；只做 TURN-34BP1 Repair #2 的 stable-writer named-test / Cloud compile
  gate preflight，不是实现者、独立 reviewer、父级或批准者。
- 只读快照时间：`2026-07-16T11:51:32.068-04:00`。
- 本报告是 **PRECHECK**。它不是 named-test 通过记录、Cloud compile 通过记录、build pass、independent
  review、parent adjudication 或 `CARD APPROVED`。
- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部最新 CR271、权威计划
  `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节、HTTPS turn 协议、TURN-34BP1 原卡
  全文及 Parent Review #3、现有 BP1 helper 报告、两仓 status/POM、Cloud named-test discovery 表面和固定
  production/test 字节。
- 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture、OCR 或 input；
  未执行 Git mutation；未改 Java、POM、卡片、计划、`ACTIVE_WORK` 或 dashboard。除本报告外未写文件。

## TURN-34BP1 冻结快照

TURN-34BP1 原卡物理 true EOF 仍为 `PARENT DELIVERY REVIEW #3`（`2026-07-16T11:36:00-04:00`）：
`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`，implementation owner 已释放，状态明确为
`INDEPENDENT-REVIEW-BUILD-PENDING`，不是 `CARD APPROVED`。

| 冻结项 | 当前真值 |
|---|---|
| Cloud production | `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`；527 行；SHA-256 `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e` |
| Cloud named test | `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`；872 行；11 个 `@Test`；SHA-256 `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785` |
| Production Git surface | 当前为 untracked：`?? src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java` |
| Test Git surface | Cloud `.gitignore` 忽略整个 `src/test/`，当前精确状态为 `!!`；普通 `git status` 不会显示此测试，故必须用文件 SHA 校验存在性和字节身份 |

当前未发现 TURN-34BP1 latest-round independent R1/R2 固定报告。`ACTIVE_WORK` 最新 `11:47` 记录也明确
“BP1 R1/R2 与 build-gate preflight 尚无固定报告交付”；原卡最后一段仍只是 Parent Review #3。以后若 R1/R2
落盘，必须读取其最新结论；任何 `P0/P1/P2`、`BLOCKED`、`REPAIR REQUIRED` 或父级重开都会使本快照退出
最终验收路径。R1/R2 尚未落盘本身不等于 source failure，也绝不等于 independent approval。

## 两仓 status 与 POM 真值

以下 status 是创建本报告前的只读快照；本报告创建后，DHXY 会多一个受保护的 untracked Markdown 文件。

| 仓库 | branch / HEAD | status 快照 | POM 真值 |
|---|---|---|---|
| `D:\mavenProject\DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | dirty；`--untracked-files=all` 共 740 项：43 modified、1 deleted、696 untracked | `pom.xml` 为 modified；SHA-256 `b196481e3f5ec7e7c8d92e87f41a05653885204d7e4ea0b2b0e817e2257275d7`；单模块 `com.myrobot:DHXY2Robot:1.0-SNAPSHOT`，无 `<modules>` |
| `D:\mavenProject\dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | dirty；`--untracked-files=all` 共 550 项：9 modified、541 untracked | `pom.xml` 为 modified；SHA-256 `f40967034f88e9b73eaf83a348df199d4bb62cbdf23c3034a950bfe20891a6a3`；单模块 `com.yueyunfe.dhxy:dhxy-cloud-brain:0.1.0-SNAPSHOT`，无 `<modules>` |

Cloud POM 使用 Java 21、JUnit Jupiter `5.10.2` 和 Surefire `3.2.5`，没有自定义 test source 目录；因此
filesystem 中的 `src/test/java/.../TaskExecutionContextTurnContractTest.java` 是默认 test source，即使它被 Git
ignore。POM 将 `skipTests`、`maven.test.skip`、`enforcer.skip`、`maven.test.skip.exec`、`surefire.skip` 固定为
false，并由 Enforcer/Antrun 阻止 skip 绕过。本预检采用当前 dirty POM 字节作为命令身份，不得 checkout、重写或
用 HEAD POM 替代。

## stable-writer window 的精确定义

只有同时满足以下全部条件，才算本卡可使用的 stable-writer window：

1. 父级明确打开 build cohort，并暂停两仓新的 Java/test/POM claim；不能利用两个 heartbeat 之间的瞬时空档。
2. 两仓所有 `CLAIMED`、`REPAIR CLAIMED` 或 provisional source-start 都已在各自物理卡 true EOF canonical
   delivery/`OWNER RETURNED`，且父级已释放 owner。旧 heartbeat、旧 claim、lane Markdown 和 dirty status
   都不能单独证明有或没有 writer；doc-only helper 不阻塞，但任何可能写 Java/test/POM 的有效领取都阻塞。
3. 命令开始前没有并行 Maven/IDE build，也没有 source generator 或人工编辑会改变 Java、test 或 POM。
   named test 与 `clean compile` 必须由一个 build owner 串行执行，任何 Java writer 活动时不得并发 `clean`。
4. 命令前复算 BP1 production/test 与两仓 POM 四个 SHA，必须逐位等于本报告冻结值；同时保存两仓完整
   `git status --short --untracked-files=all` 快照。任一 SHA 不同即不是 Parent Review #3 的被审字节，停止门禁。
5. named test 结束后、`clean compile` 开始前再次复算四 SHA，并先读取/记录 Surefire 结果，因为下一条
   `clean` 会删除 Cloud `target/`。compile 结束后第三次复算四 SHA和两仓 status。
6. 命令前后四 SHA必须不变，status 不得出现非预期 source/test/POM 差异。任何中途漂移都按并发 writer 污染
   处理：整个结果无效，即使 Maven exit code 为 0，也不得写 build pass。

当前 **不在 stable-writer window**：TURN-34BP2 已于 `11:46:43` 在卡尾写入 External C claim 正文，父级
`11:47` 将其登记为 `provisional claim / 受保护单 writer`，并要求首窗补 canonical 真尾及 source 增量、交付或
归还。其 `TaskMaintenanceService.java` 在本快照仍为 1224 行 / `963b028c...`，但 imminent Cloud production
writer 已足以阻止 Maven；父级也尚未暂停 A/B/D 的 fresh READY 领取或显式打开 build cohort。

独立 R1/R2 与 stable-writer 是不同门：R1/R2 不决定“是否有人写文件”，但决定本卡能否最终批准。即便父级选择
在 review 并行期间运行 build，结果也只能绑定上述固定 SHA；任何后续 reviewer blocker 或 Repair 新字节都要求在
新 reviewed SHA 上重新运行，不得复用旧 pass。

## 唯一授权命令与顺序

stable-writer window 真正打开后，只允许在 `D:\mavenProject\dhxy-cloud-brain` 串行运行以下两条；本 helper
没有运行它们：

1. `mvn -q -Dtest=TaskExecutionContextTurnContractTest test`
2. `mvn -q clean compile`

第一条是用户显式授权的 `HTTPS_TURN_CONTRACT_TEST_FAMILY` named-test 命令。第二条是权威计划第 18 节规定的
Cloud 非测试 source compile gate。第二条不运行测试、不能替代第一条；第一条的实际结果必须在执行第二条之前
抄录固定。TURN-34BP1 全部 production/test 都在 Cloud 模块，本卡没有适用 DHXY compile 命令。

禁止给两条命令追加 `-DskipTests`、`maven.test.skip`、`enforcer.skip`、`maven.test.skip.exec`、
`surefire.skip`、`-DfailIfNoTests=false` 或 POM include/exclude 绕过。`mvn -q clean package` 会运行 Cloud 全测试，
需要单独用户授权，不属于本预检；也不得顺手运行其它 named test、全量 test、DHXY compile 或 package。

## 预期 discovery 与必须记录的结果

Surefire 唯一目标应为
`com.yueyunfe.dhxy.cloudbrain.runner.context.TaskExecutionContextTurnContractTest`，预期发现正好 11 个测试：

```text
turnNativeFactoryUsesOnlyFrozenValuesAndRejectsInvalidIdentity
holderProviderRestoresOutsideNestedAndExceptionalBindings
boundClientRejectsMissingAndWrongContextBeforeUuidOrPort
boundClientPreservesFourOutcomeClassesWithOneUuidAndOneCommandEach
turnCheckpointCoversActiveStopPauseAndIdentityFailures
nativeGenerationDriftOnTheSameLogicalWindowStopsAtTheCheckpoint
exactNativeGenerationPassesTheCheckpointWithZeroCommand
observingAnotherNativeGenerationRetiresTheContextForeverEvenIfTheSlotCyclesBack
checkpointAndSleepRestoreBaselineOverloadsNullAndInterruptSemantics
legacySurfaceRemainsAndTurnNativeOldAuthorityFailsClosed
constructionIsInertAndHolderHasNoTurnClientInjectionCycle
```

named-test pass 的必要记录是：完整工作目录与命令、开始/结束时间、exit code `0`、fully-qualified class、
`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。必须在 `clean compile` 前读取并记录：

- `target/surefire-reports/com.yueyunfe.dhxy.cloudbrain.runner.context.TaskExecutionContextTurnContractTest.txt`
- `target/surefire-reports/TEST-com.yueyunfe.dhxy.cloudbrain.runner.context.TaskExecutionContextTurnContractTest.xml`

少于或多于 11、任何 skipped/failure/error、`No tests matching pattern`、只有 testCompile 没有 Surefire discovery，
或 exit 0 但缺少可核验的 11-test report，均不是 named-test pass。若 main compile/testCompile 先失败，必须记录
`selected test not discovered / Tests run=0`，不能冒充测试执行过。

Cloud compile pass 的必要记录是完整工作目录与命令、开始/结束时间、exit code `0`、首个 warning/error 摘要以及
compile 前后四 SHA/status 真值。它只证明当前 Cloud main source cohort 可 compile，不证明测试、runtime、输入或
TURN-34BP1 已批准。

## 命令前后 SHA / POM 证据表

实际执行者必须填写三个时点，且每格都与下表 expected 值完全一致：

| 证据 | Expected | T0 named-test 前 | T1 named-test 后 / clean 前 | T2 compile 后 |
|---|---|---|---|---|
| BP1 production SHA | `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e` | 待实际记录 | 待实际记录 | 待实际记录 |
| BP1 test SHA | `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785` | 待实际记录 | 待实际记录 | 待实际记录 |
| Cloud POM SHA | `f40967034f88e9b73eaf83a348df199d4bb62cbdf23c3034a950bfe20891a6a3` | 待实际记录 | 待实际记录 | 待实际记录 |
| DHXY POM SHA | `b196481e3f5ec7e7c8d92e87f41a05653885204d7e4ea0b2b0e817e2257275d7` | 待实际记录 | 待实际记录 | 待实际记录 |
| 两仓 status | 保存完整快照；无 source/test/POM 漂移 | 待实际记录 | 待实际记录 | 待实际记录 |

这些“待实际记录”刻意留空：本 helper 没有运行命令，不能预填 exit code、测试计数或 compile pass。普通 Git status
不会显示被 ignore 的 test，因此每个时点都必须单独计算 test SHA；不能用“status 没变”替代它。

## 失败归属与停止规则

| 首个真实失败 | 归属/动作 |
|---|---|
| 命令前 SHA/POM 不符、writer 未释放、父级未打开 cohort | stable-writer/cohort 控制门未满足；不运行 Maven，不判 BP1 源码失败 |
| Maven model、Enforcer、dependency/toolchain/environment 在编译前失败 | 记录完整首错，归 POM/cohort 或环境 blocker；selected test 未运行，不得判 named-test pass |
| main compile 或 testCompile 首错指向 BP1 两个固定文件 | TURN-34BP1 build/test gate blocker；交原实现/父级处理，不改 POM或削断言 |
| main compile 或 testCompile 首错指向其它 Cloud 文件 | 归该文件的当前卡/build cohort；不能把无关首错记成 BP1 assertion failure，也不能删/改无关 dirty source 绕过 |
| Surefire 未发现目标类、发现数不是 11 或 report 缺失 | TURN-34BP1 named-test discovery gate blocked；禁止 `failIfNoTests=false`、IDE-only 或 stale report 代替 |
| 11 个测试任一 assertion/error | 记录方法名、首个 stack/error 与四 SHA，退 TURN-34BP1/父级裁决；禁止删断言、改 fixture 迎合或加 retry |
| named test exit 0，但 `clean compile` 非 0 | build gate 未通过；保留 test 记录但不得写 build pass/CARD APPROVED |
| 任一命令期间或之后 production/test/POM SHA 漂移 | 并发 writer 污染；本轮全部 Maven 证据作废，等待新 reviewed snapshot 与新 stable window |
| 两条均 exit 0，但 R1/R2 仍缺或后来 Blocked | 只能记“named test + compile 对固定 SHA 通过”；不是 independent approval 或 CARD APPROVED；Repair 后必须按新 SHA 重跑 |

任何失败都先保留真实 first error、命令、exit code、discovery 状态和 SHA，不得继续用旧 `target`、旧 jar、IDE
增量输出或另一卡的 pass 制造通过。该失败若需要写卡/返修，只能由父级/对应 owner 按流程处理；本 helper 不改卡。

## runtime、input 与工作区禁区

- 测试只能使用 fake/scripted context、command port、metadata、UUID/action evidence；禁止真实窗口、Win32、截图、
  OCR sidecar、网络 Cloud runtime、物理鼠标/键盘、focus、input queue worker 或用户游戏客户端。
- 禁止启动 application、Cloud server、Task runtime、UI、capture、OCR runtime、input、`spring-boot:run`、
  `exec:java`、`java -jar` 或任何本地服务。TURN-41 fresh runtime 是独立用户门，不能由本卡命令替代。
- 禁止任何 Git mutation，包括 add/commit/checkout/switch/reset/restore/stash/clean/merge/rebase；不得删除、移动、
  覆盖或“清理”两仓 dirty/untracked。授权的 `mvn clean compile` 只允许 Maven 清理 Cloud `target/`，不授予
  filesystem/Git cleanup 权限。
- 禁止改 POM、Java、test、fixtures、卡片或计划来让命令通过；禁止并发 Maven、并发 writer、自动 retry、
  package 或扩大测试族。

## 当前 PRECHECK 结论

BP1 production/test 当前 SHA 与 Parent Review #3 完全一致，Cloud POM 可发现该 filesystem named test，候选命令
及证据格式已冻结。但截至本快照，BP2 provisional writer 阻塞 stable-writer window，BP1 R1/R2 也尚未落盘；
因此 **当前不得运行两条 Maven 命令，也没有 build pass 或批准结论**。

无已批准业务差异；按 exact-window generation、HTTPS 最小 turn 与 `696a12b0` 等价迁移。

TRUE_EOF PRECHECK_COMPLETE
