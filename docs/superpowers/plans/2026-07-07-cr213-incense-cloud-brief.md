# CR213 Worker Brief: 摄妖香检查与剩余时间决策迁云端

## 背景

CR213 是 CR208 的子卡 7/8：

7. `PlayerStateService` 摄妖香青色小时数字 OCR。
8. `PlayerStateService` 摄妖香绿色分钟数字 OCR 和相关模板学习。

用户已经确认选 **方案 B**：摄妖香检查策略和剩余时间识别都归云端。本地 DHXY 不再有“是否要检查/是否要补香”的业务脑，也不得保留生产路径本地 OCR 或本地模板学习 fallback。

## 必读

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md` 顶部 CR213 记录
- `D:\mavenProject\DHXY\docs\PACKAGE_ARCHITECTURE.md` 中 CR208 与 CR213 卡片

## 当前本地基线

主项目当前摄妖香逻辑在：

- `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
- `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\vision\SheyaoxiangDigitTemplateReader.java`

当前生产路径大致是：

- `ensureSheYaoXiangActive(...)`
- `probeIncenseStatus(...)`
- `readSheyaoxiangRemainingTime(...)`
- `readSheyaoxiangRemainingMinutesGreen(...)`

这些路径目前会本地匹配 `images/template/status/sheyaoxiang_buff.png`、本地 OCR 青色小时、绿色分钟，并用 `SheyaoxiangDigitTemplateReader` 做本地模板学习。CR213 要把这些生产判断迁到 cloud-brain。

外部云端项目在：

- `D:\mavenProject\dhxy-cloud-brain`

当前 cloud-brain 有 gateway / endpoint 风格，例如：

- `CloudBrainServer`
- `CloudApiGateway`
- `CloudApiEndpoint`
- `XiuluoBrainStartEndpoint`
- `LocalOcrClient`

## 目标设计

### 本地 DHXY 职责

本地只做执行层：

1. 在原本允许摄妖香检查/用物品的安全点 tick 云端。
2. 若云端要求截图，则截现有摄妖香状态栏 ROI。
3. 把截图和上下文发给云端。
4. 若云端明确返回 `USE_INCENSE`，才使用现有摄妖香物品路径。
5. 把执行结果回报云端，至少日志要能复盘 decision id / action / outcome。

本地必须 fail closed：

- 云端不可用、超时、返回非法、schema 缺字段、无法解析，都不能本地猜测，也不能自动补香。
- 不允许调用 `TextRecognizer.getAllTextResultsLocalOnly(...)` 作为摄妖香生产 fallback。
- 不允许在摄妖香生产路径继续使用 `SheyaoxiangDigitTemplateReader` 作为本地 fallback。

保留本地已有安全边界：

- `ensureSheYaoXiangActiveInOpenMainBag(...)` 如果云端返回 `USE_INCENSE`，必须仍用传入的 `mainBag.useItem(...)`，不要重新打开包裹。
- 不改状态栏 ROI、包裹道具模板、点击坐标、任务 phase 顺序、leader/member 权限边界、非摄妖香逻辑。

### 云端职责

云端负责全部业务判断：

1. 是否现在需要检查摄妖香。
2. 是否需要状态栏截图。
3. 图标模板匹配。
4. 青色小时数字识别。
5. 绿色分钟数字识别。
6. 绿色分钟模板学习/识别。
7. 是否需要补香。
8. 失败原因、冷却、重试、记忆 offset。

云端可以继续调用现有 Local OCR bridge，但这个调用发生在 cloud-brain 内部；DHXY 本地不能直接 OCR 摄妖香。

## 推荐协议

你可以按现有项目风格微调命名，但含义必须清楚。

### Service id / endpoint

建议使用：

- `SHEYAOXIANG_STATUS` 或 `INCENSE_STATUS`

如果 DHXY 当前已有通用 cloud decision client，就复用它；如果 cloud-brain 现在更适合直接 gateway endpoint，也可以加专门 endpoint。保持最小改动。

### Tick request

本地第一次 tick 不带图片，至少包含：

- `windowId`
- `taskRunId` 或本地能提供的 run/session id
- `taskCode`
- `source`
- `role`
- `nowMs`
- `lastIncenseUsedTime`
- `nextIncenseRetryTime`
- `knownIncenseIconOffset`（如果本地已有记忆）
- `openMainBagSession=true/false`

云端可返回：

- `NO_ACTION`
- `CAPTURE_STATUS`
- `USE_INCENSE`
- `RETRY_LATER`
- `FAIL_CLOSED`

### Capture request

当云端返回 `CAPTURE_STATUS` 后，本地截现有 ROI 并发送：

- 原始 PNG/base64
- ROI 的 window-relative 和 screen-absolute 坐标
- 上一次 decision id
- 上下文字段同 tick

云端返回：

- `action`
- `present`
- `remainingMs`
- `remainingSource`：例如 `cyan-hour` / `green-minute-ocr` / `green-minute-template` / `icon-only` / `not-found`
- `iconBox`
- `text`
- `confidence`
- `reason`
- `decisionId`
- `rememberIconOffset`（可选）

### Outcome report

本地执行 `USE_INCENSE` 后回报：

- `USED`
- `ITEM_NOT_FOUND`
- `STOPPED`
- `FAILED`

## 验收要求

主项目 DHXY 当前 `No-local-test mode`：

- 不要新增/恢复/运行/引用 DHXY 本地 automated tests / guards / replay / testcase images。
- 可以运行编译类验证；如果你认为不能跑，说明原因。

外部 `dhxy-cloud-brain` 不受主项目 no-local-test 限制：

- 云端新增接口/识别逻辑必须跑合适的 cloud-brain 测试或 package/compile。
- 结果写入 CR213 卡片。

代码 review 必须能证明：

- 摄妖香生产路径不再调用本地 OCR fallback。
- 摄妖香生产路径不再调用本地 `SheyaoxiangDigitTemplateReader` fallback。
- 云端失败时本地 fail closed。
- `ensureSheYaoXiangActiveInOpenMainBag(...)` 仍复用 open main bag session。
- 没有改非摄妖香 OCR/template/click/navigation/phase 逻辑。

## 文档要求

实现后更新：

- `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md`
- `D:\mavenProject\DHXY\docs\PACKAGE_ARCHITECTURE.md` 的 CR213 卡片
- 运行 `node scripts/generate-cr-dashboard-data.js`

不要把 CR208 第 7/8 项划掉；只有双 reviewer 通过后由 manager 收口。

## Worker 输出

完成后请报告：

- `DONE` / `DONE_WITH_CONCERNS` / `NEEDS_CONTEXT` / `BLOCKED`
- 改了哪些 DHXY 文件
- 改了哪些 cloud-brain 文件
- 云端验证命令和结果
- DHXY 侧是否编译/为什么没编译
- 任何风险或需要 manager 决策的点
