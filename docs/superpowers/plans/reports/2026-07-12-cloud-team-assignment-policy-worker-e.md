# Worker E：Cloud-native configless TaskTeamAssignmentPolicy

日期：2026-07-12

## 状态

DONE。唯一实现文件已新增；未提交、未回滚、未覆盖并行 dirty/untracked。

## 门禁与基线

- 源仓库：`D:\mavenProject\DHXY`
  - 分支：`thin-client-design`
  - `HEAD`：`0114604e1ff5f15491d2910959c45252e893d04f`
  - `src/main/java/com/bot/dhxy/task/startup/TaskTeamAssignmentPolicy.java` 相对 `HEAD` scoped clean：是，`git diff --quiet HEAD -- <source>` exit `0`。
  - 源文件存在于 `HEAD`，生成前 SHA-256：`6B2E1D88F20F428FAAF5D31F7D59E725BBFDBD3C9733DF8A285B4F9898784938`。
- Cloud 仓库：`D:\mavenProject\dhxy-cloud-brain`
  - 分支：`navigation-migration`
  - 生成前目标路径不存在：是（`TARGET_ABSENT`）。
  - 生成后目标文件 SHA-256：`1685661DB94F2BCC90B6E2D94D04D0AFE1EA46A17CC2FDC60B07E35C9B233375`。
- 其它工作区 dirty/untracked 均未触碰。

## 业务基线

已读取 `docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`，并核对
`docs/superpowers/specs/2026-07-12-service-migration-matrix.md` 的
`TaskTeamAssignmentPolicy` 行、fallback 条目和 Tier A 方法条目：

- member + 五环或 leader-only -> `AUTO_BATTLE`；
- solo/unknown + leader-only -> `UNKNOWN`；
- `UNKNOWN`/`AUTO_BATTLE` 原样放行；
- 五环或 leader-only 请求需要启动前实时角色检测。

无已批准业务差异；按基线等价迁移。

## 精确 structural diff

相对 DHXY 源文件，Cloud 文件唯一 intentional structural difference 为：

```diff
-import com.bot.dhxy.config.TeamTaskProperties;
-import lombok.RequiredArgsConstructor;
 import com.bot.dhxy.task.model.TaskType;
 import com.bot.dhxy.team.TeamRoleStatus;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Component;
@@
 @Slf4j
 @Component
-@RequiredArgsConstructor
 public class TaskTeamAssignmentPolicy {
 
-    private final TeamTaskProperties teamTaskProperties;
-
```

保留 `@Component`、`@Slf4j`、public class、全部 JavaDoc，以及所有决策方法与条件顺序。
未创建或复制 `TeamTaskProperties`。

## Method parity

使用机械方法块提取；对 CRLF/LF 与水平空白做规范化后比较 SHA-256：

| 方法 | source normalized SHA-256 | target normalized SHA-256 | parity |
|---|---|---|---|
| `resolveTaskForRole` | `1CF24B8C005BAAC45D165AFA97DB521B93306D6D326E7EC0036A0D26631350A1` | `1CF24B8C005BAAC45D165AFA97DB521B93306D6D326E7EC0036A0D26631350A1` | true |
| `shouldDetectRoleBeforeStart` | `D30BB2F8A0279AE51F6E0C5E017B0235844A8D95A3F3A2804FB84AEBC9A647F4` | `D30BB2F8A0279AE51F6E0C5E017B0235844A8D95A3F3A2804FB84AEBC9A647F4` | true |
| `isFiveRingTask` | `D4C74717A0618FBE22980F725D226C04F41E39B8D7B78B421F9A4BE023AADE00` | `D4C74717A0618FBE22980F725D226C04F41E39B8D7B78B421F9A4BE023AADE00` | true |
| `isLeaderOnlyTask` | `C64425D68D904CB5977B81E582DD8AD715459D8DB49B9121908DC1D2FE6A3AAC` | `C64425D68D904CB5977B81E582DD8AD715459D8DB49B9121908DC1D2FE6A3AAC` | true |

源文件实际显式声明上述 4 个方法；用户要求中的第 5 个方法若包含 Lombok
`@RequiredArgsConstructor` 生成的构造器，则它没有源方法体，且已随未使用字段和
`@RequiredArgsConstructor` 一并移除。Cloud configless 类保留 Java 的隐式无参构造能力。

## Scope restrictions

- 未修改 startup check、detection、context、remote、host。
- 未启动应用、Task、poller、UI、capture 或 input。
- 未运行测试或 Maven；由父级统一执行编译/验证。
- 未执行任何 Git mutation（未 add、commit、reset、checkout、branch、push）。

## Parent Implementation Review #1 - APPROVED - 2026-07-12

父级逐句对照 DHXY HEAD `0114604e` 与 Cloud 目标。唯一差异确为移除未被任何方法读取的
`TeamTaskProperties` import/field 和 Lombok 构造器注解；`resolveTaskForRole`、
`shouldDetectRoleBeforeStart`、`isFiveRingTask`、`isLeaderOnlyTask` 的条件顺序、日志和返回值逐句一致。

- member + 五环/leader-only -> `AUTO_BATTLE`，solo/unknown + leader-only -> `UNKNOWN`，以及
  `UNKNOWN`/`AUTO_BATTLE` 原样放行均保持。
- Cloud 目标没有引入 Spring Boot `ConfigurationProperties`，仍是可由现有 Spring context 构造的无状态 policy bean。
- 父级 fresh Cloud `mvn -q clean package` exit 0：4 suites / 21 tests / 0 failures / 0 errors / 0 skipped；shaded JAR
  119,507,069 bytes，SHA-256 `8D934E8FCF5B467B3D39014DC4F45D765051AF9315E02B3C7DF338B38B8DBBA8`。

结论：**APPROVED，P0=0/P1=0/P2=0**。该类保持 dormant，不代表 startup role 检测或 Task host 已激活。
**无已批准业务差异；按基线等价迁移。**
