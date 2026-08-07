# G020 五环关闭寻路窗口后立即放权

## 目标

只对五环黄色世界地图导航，把寻路窗口 `x2` 关闭完成定义为 `PATHING_STARTED` 的最后前台动作；之后立即
返回并释放 task turn。

## 实施

1. 在 `NavigationRequest` 增加显式的“关闭寻路窗口即结束”策略，默认关闭。
2. `FiveRingTaskV2.navigateWithTaskTurn(...)` 对所有五环导航请求开启该策略。
3. typed UI 参数把“物理 X2 点击后立即返回”送到 Client；Client 仅对该参数跳过既有 post-click `250ms`。
4. `NavigationService` 在最终坐标 pathing proof 成功后，先写路线点击状态，再执行既有小地图/寻路窗口关闭。
5. 五环 `x2` 完成后跳过移鼠、世界地图残留检查、`Alt+2` 与 pending route outcome；其他任务保持原样。
6. 用两侧定向合同锁定五环显式启用、关闭后零副作用和其他任务默认不变，再运行两仓 compile。

## 写集

- Client：`TurnUiOperationArguments.java`、`UiLocalOperationExecutor.java`、`UICleanerService.java`、G020 定向合同及
  业务规则、卡、trace、active-work、错误清单、dashboard。
- Cloud：`TurnUiOperationArguments.java`、`CloudUiCleanerLocalServiceClient.java`、`CloudUiCleanerPort.java`、
  `NavigationRequest.java`、`NavigationService.java`、`FiveRingTaskV2.java`、G020 定向合同。

## 禁止项

- 不改任何点击坐标、OCR、模板或阈值。
- 不改 Runner 的 `ARRIVED` / `STOPPED_AWAY` 判定。
- 不启动 runtime/application/UI/capture/input。
