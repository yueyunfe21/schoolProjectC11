# CR271 GAMESTATE-OWNER-P1

## 状态

`SOURCE REVIEW PASSED / P0=0 P1=0 P2=0 / FRESH RUNTIME REQUIRED`

## 事故与基线

- 事故：2026-07-22 19:54，修罗队长已在 `灵兽村(117,70)`，维护目标为
  `(117,69)`、容差 `5`，却再次进入小地图导航，没有进入 NPC smart-click。
- 唯一业务基线：
  `D:\mavenProject\DHXY` /
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` /
  `com.bot.dhxy.tools.GameStateUtil`。
- 基线 `isNearCoordinate`：`targetMapName` 为空时只比较坐标；非空时才要求
  canonical 地图相同；负容差钳为 `0`。
- 当前 Cloud `NavigationService.isNearCoordinate` 无条件要求两个地图名相同，
  因而维护路径的 `null ... null` 坐标检查恒为 false。

## GameStateUtil 逐项职责矩阵

| 基线公开职责 | 基线外部调用 | 当前 owner | 结论 |
|---|---:|---|---|
| `isMovingByPixelDiff` | 1 | Client current-map 导航本地机械确认链 | 已迁移为本地截图/移动确认机械，不恢复 Cloud 业务副本 |
| `isDirectCombatClickModeLikely` | 2 | Cloud `NpcClickService` 消费绑定窗口 raw frame/status facts | 已有唯一业务入口；本卡不改 |
| `detectFlyingState` | 2 | Cloud `NpcClickService` 与 startup preparation，输入仍走 Client exact-window turn | 已迁移；本卡不改 |
| `captureCurrentMapLabelSnapshot` | 0 | 无活调用 | 基线遗留，不恢复 |
| `isCurrentMapLabelChangedFrom` | 0 | 无活调用 | 基线遗留，不恢复 |
| `confirmCurrentMap` | 0 | 无活调用 | 基线遗留，不恢复 |
| `confirmCurrentMapFresh` | 8 | Cloud `NavigationService`，通过当前绑定窗口 fresh location sync | owner 明确；本卡仅校验调用不改时序 |
| `isSameMapName` | 22 | Cloud `NavigationService`、`XiuluoTaskV2`、`WubeiTask` 重复 | **缺单一 owner；本卡收口到 `NavigationService`** |
| `isNearCoordinate` | 10 | Cloud `NavigationService`、`XiuluoTaskV2` 重复，`FiveRingTaskV2` 仅转发 | **语义漂移；本卡恢复 696 exact 并收口到 `NavigationService`** |
| `recordMovementIntent` | 10 | Client `WindowRuntimeContext`，Cloud 经 typed whole-task local op 记录 | 本地唯一状态 owner 已存在；本卡不改 |
| `detectMovementState` | 0 | 无活调用；移动观察已由 Client runner/本地机械承担 | 基线遗留，不恢复 |
| `isInBattle` | 0 | 无活调用；战斗事实由 observation/combat owner 承担 | 基线遗留，不恢复 |

## 固定设计

1. 不恢复完整 `GameStateUtil`，不复制 832 行混合职责类。
2. 不新增 `CoordinatePolicy`、第二 store、第二协议或 Client 往返。
3. Cloud `NavigationService` 是地图名相等和近坐标纯规则的唯一 owner：
   - `isSameMapName`：两侧 canonicalize + trim；任一 null/blank 均 false。
   - `isNearCoordinate`：目标地图非空时调用唯一 `isSameMapName`；目标地图为空时跳过地图 gate；
     `safeTolerance = Math.max(0, tolerance)`；X/Y 分别按绝对差判断。
4. `XiuluoTaskV2` 与 `WubeiTask` 删除自己的地图名语义副本，直接调用
   `NavigationService`。
5. `XiuluoTaskV2` 删除自己的近坐标语义副本，直接调用 `NavigationService`。
6. `FiveRingTaskV2` 允许保留现有 protected seam，但只能无条件转发唯一
   `NavigationService.isNearCoordinate`，不得再含业务判断；同步纠正“仍在 Client
   `GameStateUtil` 执行”的过时注释。
7. 不改变修罗/五倍/五环 phase、重试、fallback、导航顺序、NPC 点击、截图、输入或 timing。

## Worker 固定写集

- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NavigationService.java`
- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\xiuluo\XiuluoTaskV2.java`
- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java`
- 仅在纠正转发 seam 注释确有需要时：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\wuhuan\FiveRingTaskV2.java`

禁止修改 Client、用户只读基线、协议 DTO、测试、runtime/UI/input。不得回退三个 dirty
工作树的任何既有改动。

## 验收

- 静态逐文件对照 `696a12b0`，确认两条纯规则逐条件等价。
- Cloud 全源码中只允许 `NavigationService` 持有地图名/近坐标判断语义；任务侧只允许直接调用或无条件转发。
- `null,currentX,currentY,null,targetX,targetY,5` 可按坐标判近。
- 负 tolerance 等价钳为 `0`。
- Cloud `mvn -q compile` 成功。
- 无已批准业务差异；按基线等价迁移。
- fresh runtime 独立验收：上述巫医近点场景不得再开小地图，必须进入 NPC smart-click。

## Worker 交付

- Worker：`Beauvoir`（`019f8cf2-0f4a-7400-8d11-4ec71810a85b`）。
- `NavigationService.java`
  `77AB0AF01372FC2CFABF24EF9F974C83AF39D262060BC2674D9BF8E15A934F7C`。
- `XiuluoTaskV2.java`
  `F2302FB8AFE9C41692745DA16940A41C955914811B5C42A308EF5100918DD25F`。
- `WubeiTask.java`
  `C3F2A96D841E956608A64E0B16B9706D53DD97237F63410854F2AD3346C0E974`。
- `FiveRingTaskV2.java`
  `8B06085761FF9CD996D8F22564E8A953856733DE52AB4AF2799967AE0B583B9B`
  （仅纠正无条件转发 seam 的过时注释）。
- Worker Cloud `mvn -q compile`：exit `0`。

## 父级终审

- 结论：`P0/P1/P2 = 0/0/0`，`SOURCE REVIEW PASSED`。
- `NavigationService.isNearCoordinate` 已逐条件恢复 `696a12b0`：空白 target map
  跳过地图 gate；非空调用唯一 canonical map 比较；负 tolerance 钳为 `0`。
- 修罗 5 个 near-coordinate 调用全部直接进入 `NavigationService`；修罗与五倍的
  task-private `isSameMapName`/`isNearCoordinate` 语义副本已删除。
- 五环只保留无条件转发 seam，不持有第二份判断语义。
- 全 Cloud source 扫描仅 `NavigationService` 持有地图相等/近坐标业务实现；未增加
  Client 往返、协议、store、phase、timing、fallback 或输入变化。
- 父级独立 Cloud `mvn -q compile`：exit `0`。
- 未运行测试、runtime、UI、capture 或 input。fresh runtime 仍按本卡事故场景验收。
