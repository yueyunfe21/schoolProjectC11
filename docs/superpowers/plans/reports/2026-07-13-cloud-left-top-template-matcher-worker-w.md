# Cloud Left-Top Template Matcher - Internal Worker W

## Parent Task Brief #1 - `W-LTSS-1-IMP1` - 2026-07-13T11:19:00-04:00

### 目标

实现已经在 `2026-07-13-cloud-left-top-status-switch-worker-b.md` 的 `Parent Design Review #3`
批准的 Cloud-only `W-LTSS-1` 叶子：对同一张左上角状态 OBSERVED PNG 分别执行 open/closed
`TM_CCOEFF_NORMED` 评分，并返回 image-local open-template 中心。该叶子不做 capture、坐标偏移、状态判定、
pending、输入、retained identity 或 Task 编排。

### 领取协议

Internal Worker W 开工时先在本文件末尾追加 `CLAIMED`，包含 task、claimedAt、唯一写集。领取后可工作超过
20 分钟；本线程父级是唯一 reviewer，Worker 自审不算批准。

### 唯一写集

1. New: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CloudLeftTopTemplateMatcher.java`
2. Append-only: 本报告文件

除此之外两仓所有 Java、Maven、schema、resources、tests、host、assembly、caller、A/B/U2 日志全部冻结。
多人共享工作区；不得回滚、覆盖或格式化他人改动，不得 git add/commit。

### 必须保持的不变量

1. 业务基线为 DHXY HEAD `0114604e` 的 `LeftTopStatusSwitchService.scoreTemplate`：
   `Imgproc.TM_CCOEFF_NORMED`、`Core.minMaxLoc`，中心计算严格为
   `round(maxLoc.x + templateWidth / 2.0)` / `round(maxLoc.y + templateHeight / 2.0)`。
2. 输入是一份非空 encoded PNG bytes；只解码一次为同一 `BufferedImage`，open/closed 两次评分必须共享它。
3. 模板只经注入的 `com.yueyunfe.dhxy.cloudbrain.host.CloudTemplateAssets` 加载，canonical id 固定为
   `LeftTopStatusDecision.LEFT_TOP_OPEN_TEMPLATE` 和 `LEFT_TOP_CLOSED_TEMPLATE`；禁止 cwd、Path、枚举、联网或写盘。
4. 任一输入不可解码、模板缺失/损坏、模板大于 observed image 时 fail-closed，返回 empty；不得用单模板结果继续决策。
5. 输出只含 `openScore`、`closedScore` 和 image-local open center；不得加 ROI `(8,147)`，不得读取/使用
   scale、window geometry、HWND 或 screen coordinate。最终 `WINDOW_CLIENT_PX` 偏移属于后续 W-LTSS-2。
6. 复用现有 public `com.bot.dhxy.core.OpenCvNativeLoader.ensureLoaded()`；可在本文件内做必要的
   `BufferedImage -> CV_8UC3 BGR Mat` 转换。所有 `Mat` 必须在所有分支释放。
7. 类保持无 Spring bean、无静态缓存/业务状态、无线程/重试/TTL/takeover，无 raw request/poll/outcome。
8. 建议可编译 API：public final class + constructor(`CloudTemplateAssets`)；
   `Optional<TemplateScores> score(byte[] observedPng)`；公开不可变 nested record
   `TemplateScores(double openScore, double closedScore, ImagePoint openCenter)` 与
   `ImagePoint(int x, int y)`。若当前源码迫使微调签名，先写到报告并停止，不自行扩写写集。
9. 不新增/恢复测试。完成后运行 Cloud `mvn -q clean package`（不可 skip）；报告命令、exit code、suite/test 总数。
10. 报告源码 diff 摘要、实际文件、与 HEAD 算法逐项等价证明、模板 SHA 不变、构建结果和 self-QA
    `P0/P1/P2`。不得启动 application/server/host/Task/poller/UI/capture/input。

**无已批准业务差异；按基线等价迁移。**

### CLAIMED

- task: `W-LTSS-1-IMP1` Cloud 左上状态模板匹配叶子实现
- claimedAt: `2026-07-13T15:00:00-04:00`
- uniqueWriteSet: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CloudLeftTopTemplateMatcher.java`（new）及本报告 append-only

### CLAIMED CORRECTION

- correctedClaimedAt: `2026-07-13T11:22:32-04:00`（America/New_York，本地实际时间）
- correction: 仅更正领取时间；历史 `CLAIMED` 记录保持不变。

### IMPLEMENTED / EVIDENCE

- 实际 Java 文件：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\CloudLeftTopTemplateMatcher.java`
- 源码摘要：新增无 Spring、无静态业务状态的 `CloudLeftTopTemplateMatcher`；构造器注入 `CloudTemplateAssets`；`score(byte[])` 对同一份非空 PNG 只解码一次，读取两个 canonical `LeftTopStatusDecision` 模板，以同一 observed BGR `Mat` 分别执行 open/closed `TM_CCOEFF_NORMED` + `Core.minMaxLoc`，返回双分数与 image-local open 中心。
- 基线等价证明：
  - 算法仍为 `Imgproc.matchTemplate(..., Imgproc.TM_CCOEFF_NORMED)` 与 `Core.minMaxLoc`。
  - 中心严格为 `round(maxLoc.x + templateWidth / 2.0)` / `round(maxLoc.y + templateHeight / 2.0)`；未加入 ROI `(8,147)`、scale、窗口、HWND 或 screen 坐标偏移。
  - 两个模板只经注入的 `CloudTemplateAssets.TemplateId` 加载，canonical id 固定为 `LEFT_TOP_OPEN_TEMPLATE` 与 `LEFT_TOP_CLOSED_TEMPLATE`；不使用 cwd、Path、联网或写盘。
  - observed 图像只转换一次为共享 `Mat`；open/closed 模板均独立评分；模板缺失/损坏、输入不可解码、模板尺寸超出 observed 时返回 `Optional.empty()`，不使用单模板结果。
  - `observed`、open、closed 与每次评分 result 的 `Mat` 均在成功、异常及创建中途失败分支释放。
  - 使用当前 Cloud 工作区已有公共 `com.bot.dhxy.core.OpenCvNativeLoader.ensureLoaded()`；Brief 中的 `com.yueyunfe.dhxy.cloudbrain.core` 包名在当前源码不存在，未修改冻结 loader 文件。
- 模板 SHA256（Cloud source 与 DHXY `images/template/status` 对应文件一致，未改资源）：
  - `left_top_open.png`: `24586642CBB30FBE06E859E4402A6E71FD4D6275AC5231EE1CED3DEBAEF32ED5`
  - `left_top_closed.png`: `E9109BF66A40168F2C1746AD6EAA48BDF657D0AB3BCE24A4E1DD8A1FF64E3D46`
- 构建证据：
  - 命令：`mvn -q clean package`（未使用任何 skip 参数）。
  - 第一次执行因工具 124 秒上限超时，未产生 Maven 失败堆栈；随后同一命令完整执行成功，exit code `0`。
  - 成功构建统计：suite `4`，test `21`，failures `0`，errors `0`，skipped `0`；生成 `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`。
- 范围确认：未新增/恢复测试，未启动 application/server/host/Task/poller/UI/capture/input，未 `git add`、未 commit；共享工作区其他改动未回滚、覆盖或格式化。
- Worker self-QA：`P0=0`、`P1=0`、`P2=0`；无已批准业务差异，按基线等价迁移。父级 reviewer 仍需独立复审。

## Parent Implementation Review #1 - APPROVED - 2026-07-13T11:33:00-04:00

父级逐行复核唯一新增文件 `CloudLeftTopTemplateMatcher.java`，并对照 DHXY HEAD `0114604e`
`LeftTopStatusSwitchService.scoreTemplate(...)`、当前 `CloudTemplateAssets` 与 `LeftTopStatusDecision`：

- 同一 observed PNG 只解码/转换一次，open/closed 两个 canonical template 对同一 observed `Mat` 分别执行
  `TM_CCOEFF_NORMED + Core.minMaxLoc`；中心仍严格使用 `round(maxLoc + template/2.0)`。
- 输出只有双 score 与 image-local open center；没有 ROI `(8,147)`、scale、window/HWND/screen 坐标、capture、input、
  pending、retained identity、线程、retry、TTL 或 Spring/host/caller 激活。
- 两个模板只经 `CloudTemplateAssets.TemplateId` allowlist 读取；输入、模板、尺寸或 OpenCV异常均 fail-closed；全部 `Mat`
  在成功和异常路径释放。
- 唯一 Java 写集与任务一致，未新增测试，未触碰 A/B/U2 或两仓其它文件。

父级 fresh 验证：在 `D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q clean package`，exit `0`；
Surefire `4 suites / 21 tests / failures=0 / errors=0 / skipped=0`。产物
`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar` SHA256
`DCE95F8175D900808A3AC80504C23C4C64A193BBA09DA27A9E068422BA474BE2`。

**父级结论：APPROVED，P0=0/P1=0/P2=0。** 本批准只收口 W-LTSS-1 评分叶子，不代表 W-LTSS-2
坐标换算、远程 capability、assembly 或 caller 已启用。无已批准业务差异；按基线等价迁移。
