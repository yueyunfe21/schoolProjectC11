# CR218 - 五环 tracker title / 接任务确认状态收敛

## 背景

2026-07-07 fresh runtime 里，五环第一轮完成后，左侧 tracker 已经没有五环任务块，但屏幕上有完成/返回 dialog。任务线程却持续卡在 `RUNNER_PREPARED_NOT_READY`，没有进入既有的 `tryHandleAcceptReturnedDialogAfterTrackerMiss(...)`。

同时，17:08 fresh 证明刚接任务成功后，如果左侧五环 tracker prepared action 已经存在，任务线程不应该被 returned-dialog 检查拖慢到 prepared action stale。

## 用户确认的业务模型

五环这里的区别不是 `taskAccepted` 本身、不是是否先看见 `STORY`，也不是泛泛的“有没有绿色链接”。

核心判断是：

- 接任务阶段：点接任务后，左侧出现五环 tracker title / 五环任务块，才说明接任务成功。
- 后续每一环：每次回来都看左侧是否还有五环 tracker title / 五环任务块。
- 已确认接任务成功后，如果左侧没有五环 title / 五环任务块，说明有可能已完成，应该走既有 `tryHandleAcceptReturnedDialogAfterTrackerMiss(...)`。
- 未确认接任务成功时，如果左侧没有五环 title / 五环任务块，不能当完成处理，只能按接任务未成功/继续等待或重试处理。
- 如果左侧能识别五环 title / 五环任务块，并且有 fresh `TASK_TRACKER_PATHING`，应优先消费左链。

## 目标

把 `FiveRingTaskV2.syncTaskPanel(...)` 的 `RUNNER_PREPARED_NOT_READY` 分支修成上面的业务模型。

## 约束

- 只改五环 `FiveRingTaskV2` 相关窄路径；不要改 Runner、tracker reader、云端协议、OCR/template、点击坐标、导航、input queue。
- 不要恢复本地直接扫左侧 tracker；仍然消费 Runner prepared action / negative。
- 不要把 `TASK_FOUND_NO_GREEN` / `TASK_FOUND_NO_LINK` 设计成新业务分支；五环业务关心的是是否有五环 title / 五环任务块。
- 保留 `allowFinished` 边界：不允许完成收口的调用不处理完成 returned dialog。
- 遵守 no-local-test：不要新增/运行 automated tests、replay、source guard、marked testcase output。允许 `mvn -q -DskipTests compile`。

## 验收

- 刚接任务成功后，如果 Runner 有 fresh `TASK_TRACKER_PATHING`，任务线程优先消费左链，不跑 returned-dialog 检查导致 stale。
- 已确认接任务成功后，如果进入 `RUNNER_PREPARED_NOT_READY` 且没有 fresh 左侧五环任务块/action，先走既有 `tryHandleAcceptReturnedDialogAfterTrackerMiss(...)`。
- 日志不再用 `taskAccepted=true` 粗暴跳过这个既有机制。
- Fresh runtime：第一轮完成后应处理完成 dialog，进入第二轮或正确收口，不再一直刷 `runner prepared tracker action not ready`。
