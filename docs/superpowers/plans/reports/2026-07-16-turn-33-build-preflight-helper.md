CLAIMED | UUID=`019f6acc-9b9a-7810-b5c9-8b42478ee0b1` | nickname=`Dalton` | role=`CR271 Internal helper` | assignment=`TURN-33 build preflight` | claim evidence=`docs/ACTIVE_WORK.md:10-12`

# CR271 TURN-33 Build Preflight PRECHECK

`PRECHECK_ONLY / 非父级批准`。本报告不是 implementation、review、approval 或 blocker 裁决；只向父级提供命令、依赖和可区分证据。本 helper 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input，未执行 Git mutation。

## 1. 快照与权限边界

- 证据快照：`2026-07-16T08:17:50.642-04:00`。
- DHXY：branch=`thin-client-design`，HEAD=`0114604e1ff5f15491d2910959c45252e893d04f`；`2026-07-16T08:15:37.898-04:00` 的 `git status --porcelain=v1 --untracked-files=all` 为 `659` 项（` D=1`、` M=43`、`??=615`），UTF-8/LF join/no-final-LF 规范化 SHA-256=`851BED09984BB5DE7A6AE9F850DDEBFFEF4F694F9C15D23C15134A9968B24079`。
- Cloud：branch=`navigation-migration`，HEAD=`3b988caa010254973e03342272e6d1d6a9685b01`；`2026-07-16T08:15:46.687-04:00` 的同口径 status 为 `550` 项（` M=9`、`??=541`），规范化 SHA-256=`E85B93E8CE8DD62EF5FA8D729E9E723F458BD4A9E8351081636D8DC2BD132C72`。
- 两仓 dirty/untracked 全部只读保护；以上 status hash 只是该时刻证据，不是 clean-worktree 声明。
- 最新父级状态：`docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-33.md:585-613` 为 Parent Review #5 `P0/P1/P2=0/0/0`；`:615-627` 接纳 R2 `0/0/0`；`:629-645` 接纳 R1 并确认双 reviewer `2/2`、两人均 `0/0/0`，状态仍为 `DUAL REVIEW PASSED / BUILD PENDING`。
- 权威注册表同结论：`docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:1148`。
- 当前不得执行命令：`docs/ACTIVE_WORK.md:6-12` 显示 Internal Euler 正占两个 DHXY Java test，`:26-35` 显示 External C 仍为 TURN-34A unique Java owner，并明确 Java writer 活动时不运行 Maven/JUnit/compile/package。权威门同样见计划 `:1443-1453`。

## 2. TURN-33 冻结输入 SHA/mtime

Cloud 根目录均为 `D:/mavenProject/dhxy-cloud-brain`。

| 文件与行 | SHA-256 | mtime (`-04:00`) | 核验用途 |
|---|---|---|---|
| `src/main/java/com/bot/dhxy/service/SummonSkillService.java:194-245,247-452,823-849,862-995,1271-1282` | `991DB945F7D621E86287D7DADB121BC9154DFE7375F6176CB4CA71434BCAED04` | `2026-07-16T06:07:17.861-04:00` | reviewed production；1431 行 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java:27-39` | `3EE97295B2D50B052E56347E420EB04C35BEA5472B327AEC48E02FB015E20A6D` | `2026-07-16T02:42:50.441-04:00` | fail-closed tombstone；123 行 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java:1-1198` | `91349697592CD33CF32870E5B6732A21470480C2CE6EF16BCA90A3444297ABCC` | `2026-07-16T02:47:30.238-04:00` | retained generic authority source gate；1198 行 |
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java:1-1683` | `6A755B0FB36152AFD90FF59244C74CBEAE086360DD8B53BF2F492CC83F968998` | `2026-07-16T06:09:38.195-04:00` | 唯一 named test；19 个 `@Test` |
| `pom.xml:12-24,76-132,141-190` | `F40967034F88E9B73EAF83A348DF199D4BB62CBDF23C3034A950BFE20891A6A3` | `2026-07-14T13:11:18.238-04:00` | Java 21、JUnit 5.10.2、Surefire 3.2.5、skip flag enforcer、package shade |

- 四份 TURN-33 source/test SHA 与原卡 `:570-578`、Parent Review #5 `:602-609`、双审裁决 `:631-642` 一致。
- 权威 exact production write set 是上述三份 main source，且不写 `TaskMaintenanceService`：计划 `:1300-1303`。唯一 test write set 是 `service/SummonSkillTurnContractTest`：计划 `:1641-1646`。
- 当前四份 TURN-33 文件的显式 `com.bot.dhxy.*` / `com.yueyunfe.dhxy.*` import 静态路径解析结果均为 `0` 个缺失。该结果只是一项 source preflight，不替代 javac/Maven。
- Cloud `.gitignore:15` 忽略整个 `src/test/`，所以该测试不会出现在普通 status 中；文件当前真实存在，父级执行前必须按上表绝对路径和 SHA 直接复核，不能用 status 缺席推断测试不存在。

## 3. 基线依赖

- 业务基线 commit=`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，commit time=`2026-06-30T01:43:39-04:00`，`src/main/java/com/bot/dhxy/service/SummonSkillService.java` blob SHA-1=`d8afb9e2f97aba9522393bd9a21d0cc4c48ed324`。
- `696a12b0:584-604` 在 generated NORMAL 删除后先 `deletedCount++`，再无条件进行唯一一次 post-delete observation；只有稳定 `EMPTY_SLOT` / `KEEP_SKILL` 成功，其余失败。当前 Cloud 对应 `SummonSkillService.java:823-849`。
- named test 的两个第五次删除 production-path fixture 位于 `SummonSkillTurnContractTest.java:413-453`，构造与最终形状断言位于 `:455-540`；它们直接引用基线 `696a12b0:584-604`。
- 用户业务合同见 `docs/业务逻辑.md:170-224`；其中 sealed/unobtained、inactive、healthy miss、mechanism failure 与反向尾扫边界位于 `:184-211`，无批准差异门位于 `:215-224`。
- 协议 fake-only 测试边界见 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:13-34`；input queue/click timing、exact-window probe、无自动重试分别见 `:56-67`、`:216-254`、`:294-308`。

## 4. 授权命令矩阵

固定工作目录：`D:\mavenProject\dhxy-cloud-brain`。

| 顺序/门 | 精确命令 | 当前授权 | 依据与结果记录 |
|---|---|---|---|
| 1. TURN-33 required named test | `mvn -q -Dtest=SummonSkillTurnContractTest test` | 已由 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 授权，但只可在父级确认所有 Java writers 稳定后执行 | 原卡 `:65-88`；计划 `:1470-1490,1645`。必须记录完整 command、exit、Tests run、Failures、Errors、Skipped；当前 source 预期 discovery 为 19 tests |
| 2. applicable Cloud non-test compile | `mvn -q clean compile` | 适用 TURN-33 build gate；同样等待 stable-writer window | 计划 `:1443-1450` 与状态机 `:1684-1693`；必须保留 fresh first compiler error 或 exit 0 |
| 3. final Cloud runtime package | `mvn -q clean package` | **当前未获本次单独授权，不得执行** | 计划 `:1448-1453,1682`：会运行全部现有测试，必须取得用户对该次 package/test run 的额外明确授权；named-test family 授权不覆盖它 |

- Cloud 命令不得添加 `-DskipTests`、`-Dmaven.test.skip`、`-Denforcer.skip`、`-Dmaven.test.skip.exec` 或 `-Dsurefire.skip`：`pom.xml:20-24,95-132,141-169`，计划 `:1485-1490`。
- TURN-33 exact write set 全在 Cloud，故本卡没有 DHXY compile 命令；不得把计划 `:1446` 的 DHXY cohort 命令追加为 TURN-33 独有门。
- 状态机顺序为 required test exit 0 后 applicable compile exit 0，再由父级决定 `CARD APPROVED`：计划 `:1684-1693`。本 helper 不执行、不批准。

## 5. 已知共享 compile cohort blocker 与区分证据

### 5.1 历史已执行证据

- `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T02A.md:84-92` 记录三条 Cloud named tests 与 `mvn -q compile` 均 exit=`1`、Tests run=`0`，失败发生在 main `compile`，未到 `testCompile`/Surefire；`:94-102` 记录当时首错为 `TaskTrackerPanelService.java:[3,25]` 缺 `GameClientTracker`，后续为 whole Service/Task 的 DHXY-only 类型缺失。
- 同一历史结论写入 `docs/ACTIVE_WORK.md:933-942`。这是已执行 Maven 的历史证据，不是本 helper 的新运行结果。

### 5.2 当前静态证据，不能冒充 javac 结果

- 历史首错已漂移：当前 `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java:3` 是 `ImagePreprocessCloudDecision` import，不再是 `GameClientTracker`；当前文件 SHA-256=`C2C48ACCC94F8C5096DC7C0D2B18AFB049E72EA0C98DA339A6CCA13C6B7CC4E7`，mtime=`2026-07-15T22:02:19.176-04:00`。因此后续不得复用旧首错文字作为 fresh compiler first error。
- 当前共享 legacy source 仍有直接缺类证据：`src/main/java/com/bot/dhxy/service/DialogService.java:3,5,39,42-45` 分别 import 当前 Cloud source tree 中不存在的 `GameClientTracker`、`InputProvider`、`CoordinateHelper`、`ObjectiveTextRecognitionService`、`WindowRuntimeContext`、`WindowScopedTempPath`、`WindowTaskContextHolder`。该文件 SHA-256=`9088644E80D27F1B32DC2DF92739BA51213BD8F439CF43A3B9D7BDE084420A9F`，mtime=`2026-07-16T01:56:51.029-04:00`。
- 第二组相同 cohort 证据：`NavigationService.java:4,7-8,37-51`；SHA-256=`66D5480722CF07C643BDABB9E53D84FFA203FD6184B8DFCAE6DEED313ED4AFF2`，mtime=`2026-07-15T03:02:03.425-04:00`。`NpcClickService.java:22,24-25,40-53`；SHA-256=`F4E3842CDB5F59580D8F25F0191ADE4847BFE8CA6C7939AC73A70BD561BFD870`，mtime=`2026-07-15T03:48:56.886-04:00`。
- `2026-07-16T08:15` 对 Cloud `src/main/java` 的 read-only explicit-import resolution 扫描得到 `75` 条当前 source tree 无对应 top-level Java source 的内部 import，规范化证据 SHA-256=`56A31A124FCC3C5955FE06A449955D3DF7A0CF97E951CCBF8D427475CD634C9B`。这只是静态 cohort indicator；实际 javac 首错只能由 writers 稳定后的授权 Maven 命令产生。
- 父级已把这类问题定性为 shared Cloud main/test compile cohort：`docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34A.md:231-255`，尤其 `:237-249`。该定性不等于 TURN-33 named test 已运行。

### 5.3 Fresh 结果分类建议

1. 执行前，父级先确认无 active Java writer，并复核第 2 节四份 TURN-33 SHA 与 `pom.xml` SHA；任一漂移则停止命令窗口并重新归属审查，不能沿用双审结论。
2. 若 named-test 命令在 main `compile` 失败，且 fresh 首错路径在 TURN-33 三份 production、一份 test 之外：记录 `BUILD COHORT PENDING / selected test NOT RUN`；Tests run 必须记 `0`，不能写“测试失败”或“测试通过”。
3. 若错误进入 `testCompile` 并指向 `SummonSkillTurnContractTest.java`，或 main compile 首错指向 TURN-33 production：记录为 card-local build evidence，交父级归属；本 helper不批准也不阻断。
4. 只有 Surefire 实际发现 `SummonSkillTurnContractTest` 并报告 Tests run=`19` 时，才可把 failures/errors 解释为 named contract result。无 Surefire report、Tests run=`0` 时不得据源码审查推断执行结果。
5. shared main compile 修复后必须 fresh 重跑原 named-test 命令，再运行 `mvn -q clean compile`；两者都 exit=`0` 才满足计划 `:1684-1693` 的 test/compile 两门。
6. `mvn -q clean package` 保持未执行，直到用户另行明确授权；它不是本次 named-test 授权的隐含组成。

## 6. 父级采用建议

- 当前建议状态文字：`TURN-33 DUAL REVIEW PASSED 0/0/0 / NAMED TEST + CLOUD COMPILE PENDING / WRITER WINDOW NOT STABLE`。依据：TURN-33 原卡 `:629-645`、最新 Active Work `:6-35`、权威计划 `:1148,1443-1453`。
- stable-writer window 打开后，父级按第 4 节原样运行并保留完整输出；不要改 POM、不要加 skip flag、不要用 IDE-only 或 stale target/jar 代替。
- shared blocker 的归属只使用 fresh first compiler error；历史 `TaskTrackerPanelService.java:3` 只能作为历史 cohort 证据，当前不可复制为新结论。
- 本报告只提供 preflight evidence，明确为“非父级批准”；最终命令执行、blocker 归属、返修、`CARD APPROVED` 或 package 授权均由父级/用户决定。

<!-- TRUE_EOF: CR271 TURN-33 BUILD-PREFLIGHT INTERNAL-HELPER UUID=019f6acc-9b9a-7810-b5c9-8b42478ee0b1 NICKNAME=DALTON NON-PARENT-APPROVAL PRECHECK_COMPLETE 2026-07-16T08:17:50.642-04:00 -->
