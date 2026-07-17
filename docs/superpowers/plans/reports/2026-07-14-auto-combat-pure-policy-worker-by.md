# Internal Worker BY - AutoCombat Pure Policy

## 状态

- `CLAIMED`: `2026-07-14T07:30:17.9939720-04:00`
- 角色：Internal Worker BY，只做实现与自审，不充当 reviewer。
- Java 写集：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java`。
- 文档写集：本报告。
- 结果：`NO_CODE_CHANGE_ALREADY_EXACT / COMPILE_PASSED`。

## 基线与工作区保护

- 已完整读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、
  `docs\ACTIVE_WORK.md` 顶部和
  `docs\superpowers\plans\2026-07-13-direct-service-input-bundle-migration.md`。
- DHXY：分支 `thin-client-design`，HEAD 与业务基线均为
  `0114604e1ff5f15491d2910959c45252e893d04f`；工作区有大量他人 dirty/untracked。
- Cloud：分支 `navigation-migration`，HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`；工作区同样有大量他人 dirty/untracked，目标 Java 为既有
  untracked 文件。
- 未 reset、checkout、clean、delete、stage、commit、branch、worktree 或做其它 Git mutation；未回滚、
  覆盖或清理任何他人文件。

## 派单前置条件冲突

- 派单要求先确认两个目标方法不存在，但本 Worker 首次读取目标 Java 时，两个方法已经各存在一个完整定义，
  类 JavaDoc 也已经说明这两个纯策略存在。
- 目标文件创建时间与最后写入时间均为 `2026-07-14T04:40:12.7344533-04:00`，早于本 Worker 领取；
  首次读取 SHA-256 已是
  `6c790beb51b98569fe395ae8638cf3a52af41f596b14d6e8bf35f5442ae822e3`。
- 为保护并行工作和避免重复定义，本 Worker 没有再次插入方法，也没有制造无意义 JavaDoc 改写；以下证据确认
  当前源码已经逐字符满足本切片。

## committed 源块与当前目标块

来源：DHXY committed
`0114604e1ff5f15491d2910959c45252e893d04f:src/main/java/com/bot/dhxy/service/AutoCombatService.java`。

```java
    private static boolean requiresEnterBattleAuthorization(TaskExecutionContext context) {
        String taskCode = context == null ? null : context.getTaskCode();
        return "xiuluo_v2".equalsIgnoreCase(taskCode) || "wubei".equalsIgnoreCase(taskCode);
    }
```

```java
    private PostCombatRecoveryPolicy legacyPostCombatRecoveryPolicy(boolean checkSheYaoXiangForLeaderTask) {
        return checkSheYaoXiangForLeaderTask
                ? PostCombatRecoveryPolicy.FULL_RECOVERY_WITH_LEADER_INCENSE
                : PostCombatRecoveryPolicy.FULL_RECOVERY;
    }
```

当前 Cloud 目标文件提取出的两个完整块与上述文本完全相同。

### 规范化块 SHA-256

规范化方式：完整方法块保留行首缩进和全部 token，仅把 CRLF/CR 统一为 LF；块末不附加换行，按 UTF-8
计算 SHA-256，并用 ordinal comparison 比较。

| 完整块 | Source/Target length | Source SHA-256 | Target SHA-256 | Exact |
|---|---:|---|---|---|
| `requiresEnterBattleAuthorization(TaskExecutionContext)` | `264/264` | `dcf96cfd2b2b1b190342caf41d097c05b7623cbbfd84acba79d4c1b1bb63b543` | `dcf96cfd2b2b1b190342caf41d097c05b7623cbbfd84acba79d4c1b1bb63b543` | `True` |
| `legacyPostCombatRecoveryPolicy(boolean)` | `294/294` | `c04e6f76096bc6181f34551023a1b8544d66ef07ced69cbe51a0432fc53190c9` | `c04e6f76096bc6181f34551023a1b8544d66ef07ced69cbe51a0432fc53190c9` | `True` |

### 定义与 dormant 计数

- `requiresEnterBattleAuthorization(...)` 完整定义数：`1`；全 Cloud 调用语法计数：`1`，即仅声明、无 caller。
- `legacyPostCombatRecoveryPolicy(...)` 完整定义数：`1`；全 Cloud 调用语法计数：`1`，即仅声明、无 caller。
- 类 JavaDoc 对这两个 pure baseline helper 的说明命中 `1` 个完整条目。
- 目标方法复用现有 Cloud `TaskExecutionContext` 和现有 nested `PostCombatRecoveryPolicy`；未新增 import、
  state owner、constructor、remote、input、capture、wrapper、session、ledger、TTL、retry 或 clock。

## 文件 SHA-256

- committed 源文件统一换行后 SHA-256：
  `e57d99bb213bd92a879f56bd2a9f2655ee29b497eb7496123d8e5614e2013e8f`。
- Cloud 目标 Java 本 Worker 首次读取 SHA-256：
  `6c790beb51b98569fe395ae8638cf3a52af41f596b14d6e8bf35f5442ae822e3`。
- Cloud 目标 Java 最终 SHA-256：
  `6c790beb51b98569fe395ae8638cf3a52af41f596b14d6e8bf35f5442ae822e3`。
- 前后 SHA 相同；本 Worker 对唯一 Java 写集实际写入为零。

## Compile Gate

- 命令：在 `D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q compile`，未执行 `clean`。
- 第一次结果：exit `1`。
- 等待 15 秒排除并行写入瞬态后 fresh 重跑：exit `1`。
- 关键越界错误包括：
  - `NavigationService.navigationTaskCode(NavigationRequest, String)` 重复定义；两次输出行号从 `354`
    变为 `360`，表明并行源仍在变化；
  - `DialogResultBuilder`、`TaskRetryPolicy.builder()` 以及多处 Lombok `log`/getter/builder 不可解析；
  - `ImageProcessorService` 调用不存在的 `getCandidatePoints()`；
  - `SheyaoxiangStatusDecisionFacade`、`FiveRingPhaseContext` 等其它并行文件存在大量符号/构造器错误。
- 两次失败输出均未报告 `AutoCombatService.java` 错误。由于失败文件均不在 BY 唯一 Java 写集内，本 Worker
  未越界修复。
- 待并行 `NavigationService.java` 写入稳定约 1 分钟后，第三次执行同一命令 fresh exit `0`，耗时约 `4s`；
  最终 Cloud compile 门禁通过。
- 未运行测试，未启动 application/server/host/Task/poller/UI/capture/input。

## 自审

- `requiresEnterBattleAuthorization(...)` 保持 `context == null` 后取 `taskCode`，再按
  `xiuluo_v2`、`wubei` 顺序做 case-insensitive OR；未改 null/taskCode 判定和短路顺序。
- `legacyPostCombatRecoveryPolicy(...)` 保持 `true -> FULL_RECOVERY_WITH_LEADER_INCENSE`、
  `false -> FULL_RECOVERY` 映射。
- 两块与 committed `0114604e` 完整块 ordinal exact，均保持 private、dormant 且无 caller。
- 本 Worker 仅创建本报告；Cloud Java 前后 SHA 不变，未触碰其它源码、文档、配置、资源或测试。
- 本节是 Worker 自审，不构成 `Approved`；等待父级独立 review。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-14T07:36:00-04:00

**APPROVED，P0/P1/P2=0，NO_CODE_CHANGE_ALREADY_EXACT。** 父级分别从 committed `0114604e`
与当前 Cloud 的真实 private 方法声明抽取完整块：`requiresEnterBattleAuthorization(...)` 和
`legacyPostCombatRecoveryPolicy(...)` 均逐字符相同、定义各 1、目标仅声明无 caller。前者保持
`context == null`、`xiuluo_v2`、`wubei` 的 case-insensitive OR 顺序；后者保持 leader-incense boolean 到两个
`PostCombatRecoveryPolicy` 值的 exact mapping。

目标 Java 前后 SHA-256 均为
`6c790beb51b98569fe395ae8638cf3a52af41f596b14d6e8bf35f5442ae822e3`，BY 正确没有重复插入或制造
无意义 diff。并行源码稳定后的第三次 Cloud `mvn -q compile` exit 0；未新增 state/caller/remote/input/capture/
wrapper/owner/session/ledger/TTL/retry。本 prerequisite 已在本波前存在，不重复计入 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
