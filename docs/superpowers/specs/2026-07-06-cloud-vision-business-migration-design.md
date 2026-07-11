# 云端视觉业务迁移设计记录

日期：2026-07-06

## 背景

当前本地 Java 仍然保留不少 OCR/视觉判断能力，例如 `TextRecognizer`、dialog 选项 OCR、任务追踪面板解析、世界地图路线文字解析、NPC/怪物黄字识别、tooltip ID 识别等。随着后续“大脑/业务逻辑上云”，本地继续直接调用 OCR 会让架构变成两套判断链路：云端负责一部分决策，本地又保留一部分视觉理解能力。

最终方向不是“本地调用云端 OCR 再本地判断”，而是“本地调用云端视觉业务服务，OCR 只是云端内部能力”。

## 目标原则

本地不应具备直接调用 OCR 的业务能力。业务路径不应直接调用 `TextRecognizer.readText(...)`、`getAllTextResults(...)`、`getAllTextResultsLocalOnly(...)`、`getAllTextResultsForMatch(...)` 等 OCR API。

本地保留：

- 截图和窗口绑定；
- hwnd、base、ROI、坐标系换算；
- 鼠标键盘执行；
- 任务 phase 状态机；
- 输入队列和暂停/停止控制；
- 简单固定模板匹配，例如固定 ROI 内找按钮、取消叉、盒子、维护按钮。

云端负责：

- OCR 引擎；
- 洗图、候选框、文字行/文字块提取；
- dialog 选项理解；
- 任务追踪面板解析；
- 世界地图路线/坐标文字解析；
- NPC/怪物黄字、紫字、tooltip ID 等文字视觉判断；
- 返回结构化业务 JSON。

## 正确迁移顺序

先迁“视觉业务服务”上云，再替换 OCR 实现。

原因：如果先把 OCR 裸迁到云端，会形成临时链路：

```text
本地业务 -> 云端 OCR -> 本地业务继续判断
```

这不是最终架构，后续仍然要把本地业务判断再迁一次。更好的顺序是：

```text
第一步：本地调用云端视觉业务服务
第二步：云端视觉业务服务内部暂时使用现有 OCR provider/兼容 OCR
第三步：云端内部 OCR provider 切换为真正云端 OCR 引擎
第四步：删除本地业务 OCR 能力
```

这样本地 API 从一开始就是最终形态，后续替换 OCR 引擎时不需要再改本地业务代码。

## 本地到云端的业务接口形态

本地不调用 `CloudOcrClient.recognize(...)` 这种裸 OCR 接口。业务路径应调用按场景命名的云端视觉服务，例如：

- `findDialogOption`
- `parseTaskTracker`
- `findWorldMapRouteCoordinate`
- `detectTeamRoleTooltip`
- `findNpcNameCandidate`
- `parseObjectiveText`

云端内部可以调用 OCR、洗图、模板、候选检测，再返回业务 JSON。

示例：

```text
本地：截 dialog 图，带 task/phase/window 发送
云端：OCR + option 匹配
云端返回：found、option 文本、box、confidence、debug token
本地：只执行点击或切 phase
```

## 回滚策略

不在代码里维护本地 OCR 与云端 OCR 两套长期并行逻辑。迁移前先在 git 上保留明确回滚点，例如：

```text
pre-cloud-vision-business-migration
```

后续所有迁移在独立 branch/CR 中小步推进。需要对比或回滚时使用 git 历史，而不是让运行时代码长期保留双轨 OCR。

## 初步 CR 拆分

1. 建立云端视觉业务接口边界和通用请求/响应约定。
2. 迁移 dialog option 视觉判断。
3. 迁移任务追踪面板解析。
4. 迁移世界地图路线/坐标解析。
5. 迁移 NPC/怪物黄字、紫字、tooltip ID 等文字视觉判断。
6. 清理本地业务路径里的 `TextRecognizer` 直接调用。
7. 云端内部 OCR provider 切换到正式云端 OCR 引擎。
8. 移除本地 OCR sidecar/百度 OCR 对业务运行时的依赖。

## 当前结论

OCR 上云不是独立终点，而是视觉业务上云的内部前置能力。本地最终只保留执行层和简单机械视觉匹配；需要“理解图片”的判断都应由云端视觉业务服务完成。
