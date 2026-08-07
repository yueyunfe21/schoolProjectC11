# G006 对话框鼠标禁停区

## 状态

- `IMPLEMENTED / SELF-TESTED / FRESH RUNTIME REQUIRED`
  - `mvn -o compile -DskipTests=false` exit 0
  - `DialogMouseNoParkZoneContractTest` 隔离运行 **7/7**
  - 自审不算 Approved；推进需外部 reviewer。
- Client：`D:\mavenProject\DHXY-cr271` / `thin-client-design`（**本卡只改 Client**）
- Cloud：`D:\mavenProject\dhxy-cloud-brain` —— **零改动**
- 协议 `com/bot/dhxy/cloud/turn/protocol/**` —— **零改动**
- 用户业务基线 `D:\mavenProject\DHXY` 只读，不写入。

## 问题

对话框的判读有两条路，两条路都不管鼠标停在哪：

1. **本地图片匹配**：`WindowObservationSampler.refreshSharedCycleFrame()`（[WindowObservationSampler.java:303](../../../src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java)）每周期整窗截一次，天庭选项模板从中裁剪匹配（ROI `200,250,640,300`，见 `TiantingDialogLocalMechanics`）。选项模板是 34–93px 的绿字裁片。
2. **云端 OCR 上传**：`CloudDialogDetectionPort.detectDialog` 截同一块 ROI 上传；云端 `DialogService.hasOptionInLowerHalf` 靠「绿像素 > 150」判 OPTION vs STORY。

光标压在选项文字上，会吃掉一条 34–93px 字形串的相当比例 —— 本地模板匹配失败，云端 OPTION 被误判成 STORY，于是走 story-click 去点一个选项框。

**而且是自己造成的**：`WindowObservationSampler` 点完选项后（`moveAndClickLeft("tianting:dialog-option", ...)`）光标就停在刚点的那一行上，下一拍的匹配必然带着光标。

## 用户确认合同

1. 采用**禁停区**方案，不是「在每个读取点之前插一次移开」。理由：读取点有很多且难以穷举，而写鼠标的地方只有一处；把规则放在写的时刻，读的时刻自然干净。
2. 禁停区 = **窗口相对 `(200, 379) – (520, 488)`**（unscaled）。
   - x 起点取 dialog ROI 的 `x=200`，右边界取 `200 + 640/2 = 520`
   - y 取 `DialogService` lower half（选项行带）：顶 `345 + SMALL_ONE_LINE_CROP_TOP_Y(34) = 379`，底 `345 + DIALOG_SMALL_H(143) = 488`
   - 屏幕绝对坐标 = `windowRect.left/top` 加上这组数；`CoordinateHelper.getScaledRect` 返回的正是同一空间（该方法只做 base+offset，不缩放）
3. 规则：**输入批次结束时**，若光标落在禁停区内，就把它挪到安全落点。不是每次读取前挪，不是无条件挪。
4. 安全落点（2026-08-02 用户两次修订）：
   - **必须随机**：固定落点是机器人指纹，反作弊可直接检测（"每次都在同一个地点这也太假了"）。
   - **停靠区 = 用户指定 ROI**：屏幕绝对 `(969,463)-(1264,681)`，按队长窗口（ID 67555）base `(254,23)` 换算为**窗口相对 `(715,440)-(1010,658)`**（右下方开阔场景区）。每次扫尾在其中均匀抽一点。
   - 该区与禁停区、任务追踪搜索区（x 6–207 / y 196–551）、小地图（x≥761 且 y≤147）全部不相交；窗口装不下该区（异常几何）时回退左下角 `(left+1, top+height-2)` 并仍受窗口内校验约束。
5. **（2026-08-02 用户追加）扫尾移动必须落在当前窗口坐标范围内，绝不移出窗口。** 窗口外的移动等于没移——光标停在原地、仍压着对话框——却会打出"已挪走"的假日志。实现：`DialogMouseNoParkZone.insideWindow` 在 `moveMouse` 之前校验落点（用与算落点相同的 binding 矩形），窗口外则 `warn` 跳过；合同 `theMoveMustStayInsideTheCurrentWindow`。
6. 故意把鼠标停在某处让东西显出来的悬停探测必须不被扫走。

   **实施结论：不需要开关。** 全部悬停探测都绕开输入队列直接调 `InputProvider`（清单见下），根本到不了本钩子；`pixelChangeProbe` 只有协议与结果管道、无生产调用者。开关只会是一段没人走的代码。真要新增「排进队列且有意停在本区域」的悬停时，再在此处加显式豁免——这一条已写进 `parkPointerOutOfNoParkZone` 的 javadoc。

### 用户裁决记录（不得在实施中「优化」掉）

实施前我用一张真实全窗口截图目测提出：该图对话框上下沿约 `y285–432`，绿链接「广寒宫（37,65）」约在 `y325/y355`，落在 `379–488` 之上，即本框可能盖不住链接、下半截悬在草地上；根因是 `379/488` 由**小对话框**基线 `DIALOG_SMALL=(250,345,529,143)` 推出，而 `DialogFrameClassifier` 认 143/164/210 三档面板高、面板上下位置会动。

**用户已知悉并裁定：就按 `(200,379)-(520,488)` 实施。** 实施按此执行，不得自行扩成三档并集。若真机验证发现漏盖，作为后续卡处理。

## 设计

### 唯一钩子

`InputActionWorker.handle(InputActionRequest)`（[InputActionWorker.java:94](../../../src/main/java/com/bot/dhxy/input/action/InputActionWorker.java)）中 `inputCoordinator.callInputTransaction(...)`（同文件 `:137`）的**收尾**。

选它的理由：

- 该事务包住全部三条执行路径（legacy actions / `runFrozenExactWindowActions` / `runFrozenExactWindowExclusive`），一处覆盖全部；
- 跑在**同一 worker 线程、同一个全局输入事务里**，锁已经握着 —— 不新增锁竞争（`PrintWindow` / `globalInputLock` 争用是已知痛点）；
- 不经过 `TurnInputStepExecutor.submitMouseActionsRaw`，**不受 `isLocalCombatVisible()` → `COMBAT_ACTIVE` 门限制**（对话框在战斗中照样弹）。

### 为什么不是别的位置

- ❌ **每个读取点之前插一次移开**：读取点穷举不完；云端那条还要穿协议告诉客户端「这次截图前先挪」，得给 `ClearPointerIfOverRegion` 加触发矩形字段（它的触发矩形写死是截图区域本身，且校验落点必须在区域外，表达不了子矩形），两仓协议同步 + validator + executor，成本远大于收益。
- ❌ **turn 里插一个 `INPUT MOUSE_MOVE` 前置步**：被战斗门拦成 `LOCAL_COMBAT_ACTIVE`，整个 turn 失败。
- ❌ **驱动层 `WinApiMouseController` 每个方法收尾挪**：`moveAndClickLeft` 是 MOVE + CLICK 两个 action，在 MOVE 后挪走会让 CLICK 落到落点上。必须是**批次**收尾，不是单动作收尾。
- ❌ **采样器周期里无条件挪**：那个周期一直在跑，会每拍抢全局输入锁并和任务自己的鼠标打架。

### 已知不经过本钩子的直调点（本卡不改，仅登记边界）

以下调用绕开输入队列直接 `inputProvider.moveMouse`，批次收尾管不到；本卡不处理，作为已知边界写进合同测试的注释：

- `TurnCaptureStepExecutor:268`（Ctrl-hover 像素探测，无生产调用者）
- `NpcArrivalFrameFifoLocalExecutor:420/469`
- `XinshouCombatLocalMechanics:634`
- `LocalServiceStepDispatcher:353`、`XinshouTrackerLinkChainLocalOperationExecutor:117`
- `QuestManagerService:382`

## 写集

### Client（只此一仓，已实施）

- 新增 `com/bot/dhxy/input/action/DialogMouseNoParkZone.java` —— 几何与判定的唯一 owner
- `InputActionWorker`
  - 事务 lambda 体抽成 `runInputTransaction(request, preferBackgroundKeyboard, focusBeforeInput)`，
    让扫尾能成为三条路径共同的 `finally`
  - 新增 `parkPointerOutOfNoParkZone(request)`：读 `MouseInfo` → 在区内才 `inputProvider.moveMouse`；
    异常只 warn，绝不让扫尾把一个已完成的请求判失败
- 新增 `DialogMouseNoParkZoneContractTest`（7 条）

### Cloud

无。

## 验收

1. 几何合同：禁停区 = 窗口相对 `(200,379)-(520,488)`；数字只有一个 owner，其它地方引用不复制。
2. 位置合同：钩子在 `callInputTransaction` 收尾，且覆盖 legacy / frozen-actions / frozen-exclusive 三条路径。
3. 条件合同：光标在区外时**不产生任何输入**（不是无条件移动）。
4. 落点合同：落点在窗口内、在禁停区外、不落在任务追踪搜索区（x 6–207 / y 196–551）内、不落在小地图（relX≥761 且 relY≤147）内。
5. 战斗合同：本移动不经过 `submitMouseActionsRaw`，`isLocalCombatVisible()` 为 true 时**仍然执行**。
6. 顺序合同：MOVE+CLICK 批次中，落点移动发生在 CLICK **之后**，不得改变 CLICK 的落点。
7. 豁免合同：开关打开时收尾不移动。
8. Cloud 仓与协议目录零 diff。
9. 真机：天庭点完选项后，下一拍日志里光标不再位于选项行；`dialog option lower check` 的 `green=` 不再出现被光标压低的抖动。

## 未决

- 三档面板高度（143/164/210）导致 lower half 静态带子与真实面板错位 —— 这同时意味着**云端 `hasOptionInLowerHalf` 自己也在拿可能错位的带子数绿像素**。已按用户裁决不在本卡处理，另立卡。
- 任务追踪面板（x 6–207 / y 196–551）悬停会高亮条目 → 标题匹配失败，是同一机制的另一个面。本卡不含。
