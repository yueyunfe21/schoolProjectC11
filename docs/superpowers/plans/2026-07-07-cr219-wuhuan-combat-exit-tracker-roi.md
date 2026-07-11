# CR219: 五环战斗后用 tracker ROI 做快脱战候选

## 背景

五环当前战斗结束后仍主要等 `AutoCombatService.handleCombatTick(...)` / 战斗雷达恢复信号，体感上从脱战到再次点击左侧绿色 tracker 链接偏慢。用户确认五环不是组队头像任务，不能用修罗/五倍头像 ROI 的 fast exit。五环应复用已经识别到的左侧五环 tracker 任务块区域，在战斗中截图做基线，战斗后用同一块区域变化作为“可能脱战”的候选信号。

## 任务范围

允许改：

- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingPhaseContext.java`
- 只有在确实需要一个非状态突变 probe/reconcile 边界时，才可小改：
  - `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
  - `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- CR219 文档记录：`docs/PACKAGE_ARCHITECTURE.md`、`docs/ACTIVE_WORK.md`
- dashboard 数据：`docs/cr-dashboard-data.js`

禁止改：

- OCR/template 阈值和模板资源。
- tracker reader 业务识别顺序、绿色链接点击坐标算法、云端协议。
- `GameStateUtil.isMovingByPixelDiff()`、导航、input queue、给鞋业务语义、修罗/五倍业务路径。
- 不新增/恢复/运行本地 automated tests、source guards、replay、case image、marked output。

## 必须实现的业务逻辑

1. ROI 来源：
   - 使用五环已经缓存的 tracker 任务块区域，例如 `FiveRingPhaseContext.wuhuanTrackerBlockRegion()` 或等价的五环 tracker block ROI。
   - 不能使用队伍头像、队长头像、battle avatar ROI。
   - 战斗前不需要截图当基线，只需要知道五环 tracker block 的窗口相对区域。

2. 战斗中基线：
   - 当五环进入/确认 `IN_COMBAT` 后，立即清掉本轮 tracker click 对应的旧五环 pathing intent，因为这次 tracker 点击已经被战斗消费。
   - 清理范围只能是五环 tracker source：`wuhuan-v2:prepared-tracker-panel-click:` 和 `wuhuan-v2:tracker-green-click:`。
   - 同时在同一个五环 tracker block ROI 截一张“战斗中基线”并记录到 `FiveRingPhaseContext` 或任务局部状态。
   - 如果 ROI 不存在，保留旧等待逻辑，不要猜。

3. 快脱战候选：
   - 仍处于 `IN_COMBAT` 时，可用当前五环 tracker block ROI 与“战斗中基线”比较。
   - ROI 明显变化只能表示“候选脱战”，不能直接修改 `GameContext.ActionState`，不能调用会把状态改成 `FREE` 的 avatar fast-exit 方法。
   - 如果 ROI 没变化，继续按现有 combat wait/park 逻辑等待。

4. 正证据释放状态：
   - 候选脱战后，必须先找正证据，才能把窗口状态释放为非战斗并继续五环：
     - 能消费/准备出五环绿色 tracker 链接：释放状态，然后点击该链接。
     - 或识别到五环完成类 story/dialog：释放状态，然后按既有完成收口。
   - “没有 tracker / 没有五环任务 / negative / no-green / no-link” 不能作为脱战正证据，因为战斗中本来就看不到 tracker。

5. 候选失败处理：
   - 候选脱战后，如果没有绿色链接，也没有完成类 dialog，不要盲目改状态。
   - 先问可信的 runner/battle 状态：
     - 如果可信状态仍是 `IN_COMBAT`，说明 ROI 候选是假阳性，保留/恢复等待战斗，并刷新战斗中 ROI 基线。
     - 如果可信状态不是战斗，可释放状态后回到现有 sync/retry 流程。

6. 给鞋 dialog 不属于这条路径：
   - 战斗后 fast-exit 路径不处理给鞋。
   - 给鞋只属于 `STOPPED_AWAY + 当前地图 == 大雁塔二层` 的 pathing 分支。

7. `syncTaskPanel()` combat gate：
   - 现在 `syncTaskPanel()` 入口有 `isWindowCombatActive()` gate。worker 需要避免候选脱战时直接走普通 `SYNC_TASK_PANEL` 被这个 gate 挡住。
   - 可以在 `WAIT_PATHING` 内做一条窄的 post-combat verification 分支：先检查绿色 tracker / 完成 dialog 的正证据，正证据成立后再释放 combat state 并进入原点击/完成逻辑。

## 日志要求

补充足够日志，方便 fresh runtime 复盘：

- 战斗中 ROI 基线是否捕获、ROI window-relative 坐标、窗口 id。
- 旧五环 tracker intent 在 combat entry 时是否清掉、source / reason。
- ROI 快脱战候选命中/未命中。
- 候选后找到的正证据类型：tracker link / completion dialog / none。
- none 时可信 battle 状态是什么，是否刷新基线继续等待。

## 验收方式

当前全项目 no-local-test 模式：

- 不创建、不运行、不引用本地 automated tests / source guards / replay / case image / marked output。
- 允许运行 `mvn -q -DskipTests compile`。
- 允许运行 `node scripts/generate-cr-dashboard-data.js` 和 `git diff --check`。

Fresh runtime 之后需要看：

- 战斗一进入就清旧五环 tracker pathing intent。
- 战斗中记录五环 tracker block ROI 基线。
- 脱战后如果 ROI 变化，先找绿色 tracker / 完成 dialog 正证据，再释放状态。
- 没有 tracker / negative / no-link 不会把状态改成 `FREE`。
- 给鞋 dialog 不会出现在战斗后 fast-exit 路径。
