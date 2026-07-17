# Cloud startup policy leaf migration: Worker C

## Result

DONE. 两个 Cloud 迁移叶子文件已按源文件原字节复制，未修改业务逻辑。

## Source and target

| File | Source | Target |
|---|---|---|
| `TeamTaskProperties` | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\config\TeamTaskProperties.java` | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\config\TeamTaskProperties.java` |
| `TaskTeamAssignmentPolicy` | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\task\startup\TaskTeamAssignmentPolicy.java` | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\startup\TaskTeamAssignmentPolicy.java` |

## Gates and byte verification

- DHXY `HEAD`: `0114604e1ff5f15491d2910959c45252e893d04f`。
- 两个源文件分别执行 scoped `git diff --quiet HEAD -- <file>` 与 scoped `git status`；均为 HEAD-clean。
- 两个目标文件创建前均不存在；`target-absent=True`。
- `TeamTaskProperties`: source/target 均 `4370` bytes；SHA-256 均为 `037A1E24B961F1E06280163B19CC19CB84E82BC591769A1FBF9FE437F8142816`；二进制比较 `True`。
- `TaskTeamAssignmentPolicy`: source/target 均 `3323` bytes；SHA-256 均为 `6B2E1D88F20F428FAAF5D31F7D59E725BBFDBD3C9733DF8A285B4F9898784938`；二进制比较 `True`。

## Migration matrix and imports

- Matrix 角色：`TaskTeamAssignmentPolicy` 负责按身份把请求任务重映射为窗口实际任务，并保留 `shouldDetectRoleBeforeStart` 的启动前角色检测采集；`TeamTaskProperties` 属于宿主配置/运行态配置依赖闭包。
- 已核对 Cloud 已有 `TaskType`、`TeamRoleStatus`，且 Cloud `pom.xml` 已有 Spring 与 Lombok 依赖；源文件 imports 原样保留。
- 角色分配条件、默认值、日志、字段和注释均无业务差异：member + 五环/leader-only -> `AUTO_BATTLE`；solo/unknown + leader-only -> `UNKNOWN`；`UNKNOWN`/`AUTO_BATTLE` 原样放行。
- 无已批准业务差异；按基线等价迁移。

## Constraints observed

- 仅创建所需目标目录（如缺失）及两个目标 Java 文件和本报告。
- 未迁移 `TaskStartupCheckService` / `TeamRoleDetectionService`。
- 未修改 Spring host/config；未启动应用、task、poller、UI、截图或输入。
- 未运行测试，未运行 Maven；未执行 Git mutation，未提交、暂存、reset、revert、checkout 或 clean。

## Parent Build Review #1 - BLOCKED / ROLLED BACK - 2026-07-12

- 父级 fresh Cloud `mvn -q clean package` 在 compile 阶段失败：Cloud pom 不含 Spring Boot，源文件
  `TeamTaskProperties` 的 `org.springframework.boot.context.properties.ConfigurationProperties` 无法解析。
- exact-copy 的 imports-closed 前置判断不成立；不为该叶子引入整套 Spring Boot。Worker C 已只删除自己新增的两个
  Cloud 目标并确认均不存在，未动其它文件。该切片代码交付为零，后续改走 Cloud-native configless policy adaptation。
- 结论：`P0=0/P1=1/P2=0，BLOCKED/CLOSED AS ROLLED BACK`；不计入迁移数量。

`无已批准业务差异；按基线等价迁移。`

## Parent Build Repair #1 - ROLLED BACK / BLOCKED

- 父级 fresh Cloud `mvn -q clean package` 编译失败：`TeamTaskProperties.java` 引用 `org.springframework.boot.context.properties.ConfigurationProperties`，但 Cloud `pom.xml` 无 Spring Boot 依赖；此前“Cloud 已有 Spring 依赖”不足以证明 Boot annotation 可编译，exact-copy 前置门失败。
- 已删除本轮新增的 Cloud 目标文件：
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\config\TeamTaskProperties.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\startup\TaskTeamAssignmentPolicy.java`
- 未修改 `pom.xml`、未引入 Spring Boot、未修改 DHXY 源文件，未运行 Maven，未执行 Git mutation，未触碰其它文件。
- 结论：该 cohort 需后续 Cloud-native config contract 设计，不能 exact-copy。
