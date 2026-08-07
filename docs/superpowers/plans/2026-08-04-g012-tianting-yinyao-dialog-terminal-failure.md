# G012 天庭引妖窗口漏处理与错误终态链

## 状态

- 状态：`INVESTIGATED / P1 CONFIRMED / 待用户批准实施`。
- 建卡日期：`2026-08-04`。
- Client 工作树：`D:\mavenProject\DHXY-cr271`，分支/HEAD：
  `thin-client-design` / `2f083c14152106ba6ad418a0c29e3e0e2148e14a`。
- Cloud 工作树：`D:\mavenProject\dhxy-cloud-brain`，分支/HEAD：
  `navigation-migration` / `363d0e3fae73c0c55f4920f6f1c61338a0458d73`。
- `D:\mavenProject\DHXY` 是保护基线，本卡不得修改。

## 现场结论

队长窗口 `hwnd-F99187E`、角色 `468413443` 的天庭 run
`remote-turn-e315af9f-f23e-4511-aeb6-fad210066e99:0:TIANTING` 不是 Java 崩溃。Cloud 在
`2026-08-04 03:57:11.934` 主动返回 `FAILED`，`phase=FAILED`、`round=3`、`exceptionType=null`。

第一处故障发生在 `03:48:26`：

1. `03:47:00.828` 本地点击 `kaida.png`，`03:47:04.354` 确认正常任务入战；本场不是暗雷巡逻战斗。
2. `03:48:18.500` Runner 确认脱战。
3. 战后一次性 `TIANTING_YINYAO` 探针在 `03:48:21.763` 建立，因当时窗口尚未出现而 miss，并在
   `03:48:23.090` 被清除。
4. `03:48:26.616` 本地点击下一条 Tracker 绿色链接 `(392,316)`。点击成功后才弹出“是否使用天庭秘制
   引妖香”窗口；保存帧为
   `images/captures/20260804/hwnd-F99187E/20260804_034827_386_6804_HWND_PRINTWINDOW_271_39_1024x768.png`。
5. 此时只安装了 `TIANTING_COMBAT_OPTION`，没有 `TIANTING_YINYAO`。因此 Runner 持续上传 dialog ROI，
   但没有消费者会匹配/点击 `yinyao.png`。
6. `03:51:33.890` 子任务 watchdog 超时，Cloud 强制进入 `RETURN_HOME`。

## 连锁故障

1. watchdog 在旧天庭任务仍存在、引妖窗口尚未被正常完成时强制回城并重新接任务，破坏了原小任务上下文。
2. `03:53:15.416` 到达 `天宫 (143,113)`，`03:53:41.256` 点击李靖；随后 `03:53:44.912` 又直接点击
   Tracker 绿色链接。该点击没有产生位置变化，`03:53:58.392` 被判 `STOPPED_AWAY`，仍在 `天宫 (143,113)`。
3. 当前决策把 `darkThunder=true + STOPPED_AWAY` 直接解释为“暗雷目的地已到”，没有校验当前地图是否属于
   已测巡逻地图。于是 `03:54:00.165` 进入 `tianting:dark-thunder`，在天宫反复检查飞行状态和坐标。
4. `TiantingGeometry` 只为 `蟠桃园/瑶池/御马监/长寿村外` 定义巡逻点；`天宫` 必然落入
   `UNKNOWN_MAP`。等待 `DARK_THUNDER_UNKNOWN_MAP_TIMEOUT_MS=120000` 后，`runDarkThunder()` 返回 false，
   `RUN_DARK_THUNDER` 将本轮映射为 `FAILED`，最终产生 `03:57:11.934` 的任务异常。

## 根因分级

### P1-A：引妖探针时机错误

`clearYinyaoDialog()` 只在脱战后的一个短窗口运行；真实引妖窗口可以由之后的 Tracker 绿链点击才打开。
当前代码在绿链 leg 只安装 `TIANTING_COMBAT_OPTION`，导致该窗口永久无人处理。这是本次事故的首因。

### P1-B：暗雷入口缺少地图/到达语义栅栏

`TiantingSubtaskDecision` 把所有 `ARRIVED/STOPPED_AWAY` 都视为可进入当前 `darkThunder` 分支；没有要求
当前位置属于 `TiantingGeometry.patrolMapNames()`，也没有区分“点击后没有移动”与“确实到达暗雷区域”。
本次因此在 `天宫` 启动暗雷逻辑，并最终主动失败。

### P2：watchdog 恢复缺少旧任务后态证明

watchdog 直接回城重接；没有先证明引妖/option 已清、旧 Tracker 已结束，也没有在重新接任务后证明接任务
option 已消费再允许点击 Tracker。这使首个漏处理被放大成错误返程和错误任务上下文。

## 拟议修复边界

1. 保持引妖为 Client 本地模板匹配，不改模板、ROI、阈值和点击动作；但它必须在天庭主循环可见 dialog
   事实中可被识别，不能只存在于一次脱战探针。无引妖模板时立即继续，不做多次空 miss 重试；真实点击后
   才按既有 Tracker 变化规则决定是否重试。
2. 绿链点击后的 dialog interest 必须覆盖既有天庭选项与 `TIANTING_YINYAO`，并保持 Cloud 只消费 typed
   outcome；Client 不推断业务 phase。
3. `RUN_DARK_THUNDER` 前增加业务栅栏：当前位置必须是已测巡逻地图，且当前 leg 的停止事实必须与该绿链
   action/intent 对应。`天宫` 等非巡逻地图上的 `STOPPED_AWAY` 不得进入暗雷，必须回到 Tracker/dialog
   恢复路径。
4. watchdog 恢复不得把旧任务仍存在的画面直接当成“已重新接任务”。接任务 option 未确认消费时，不得
   继续点击 Tracker。

## 连通性验收

1. 脱战时无引妖窗口、随后绿链点击才弹出引妖：本地命中 `yinyao.png`、点击一次、Cloud 收到 typed
   outcome，再继续当前小任务；不得等待 watchdog。
2. 脱战及绿链后均无引妖：不因空 miss 增加循环等待。
3. 引妖点击后 Tracker 未变化：只在真实点击后按既有上限重试；纯 miss 不重试。
4. `darkThunder=true` 但当前位置为 `天宫`：不得执行 `Alt+U/Alt+C` 或巡逻，不得等待 120 秒后失败。
5. 接任务 NPC 点击后 option 未确认：不得发布下一条 Tracker 点击。
6. fresh runtime 必须观察到原事故链在 `03:48:27` 对应位置闭合，且任务不再以
   `round terminal phase=FAILED outcome=FAILED` 结束。

