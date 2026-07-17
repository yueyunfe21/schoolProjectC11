# TURN-08A Report - DHXY exact-window metadata 与后台截图

## CLAIMED

- 领取时间：`2026-07-15T14:33:40-04:00`
- 角色：`Internal implementation worker`，不是 manager/reviewer。
- 状态：`CLAIMED`
- `countUnit`：`N/A (INFRA exact-window metadata and background capture)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-00`（已 CLOSED）
- `approvalDependsOn`：`TURN-01D`
- 精确写集：
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnFrame.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnPngCodec.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`
  - 本报告（仅追加领取、交付和父级审查记录）
- 只读复用：`BoundWindowCaptureService`、`MultiWindowTaskManager`、`WindowTaskRunner`，以及完成绑定刷新所需的既有 runtime/model API。
- 禁止触碰：除上述四个 Java 文件和本报告外的两仓全部文件；尤其不得修改主计划、CR271、
  `docs/ACTIVE_WORK.md`、dashboard、协议 cohort 文件、capture/runner/runtime 既有实现。

## 两仓 status 与基线（领取瞬间）

### DHXY

- 当前分支：`thin-client-design`
- 当前 HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- 可用远端基线：`origin/dev=e543d024bf900853944b36d27d0f736005d9eeb9`；当前本地分支无
  `origin/thin-client-design` 跟踪引用。
- 本卡四个 Java 路径在当前 HEAD、领取前 working-tree diff 和现有 `cloud/turn` 目录中均不存在；因此本卡只新增文件，
  不覆盖既有实现。
- `git status --short --branch` 显示当前仓库已有大量他人修改/未跟踪内容，包括配置、CR/上下文文档、thin-client
  specs/plans、`pom.xml`、多个 Service/Task/input/window Java 文件、`cloud/remote/` 与多个迁移目录。
- 保护声明：不回滚、不覆盖、不清理、不提交上述任何 dirty/untracked；完整原始 status 以领取时仓库输出为准。

### dhxy-cloud-brain

- 当前分支：`navigation-migration`
- 当前 HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- 可用远端基线：`origin/main=48e37813913094cacd1143fcae02704033eecb93`；当前分支未显示远端跟踪关系。
- `git status --short --branch` 显示既有 `pom.xml`、server/gateway/算法/config 修改，以及 `logs/`、迁移备份、
  `com/bot/`、host/remote 与模板等未跟踪内容。
- 本卡不写 Cloud Brain；全部既有 dirty/untracked 保持原状。

## 实施边界

- 每个 action 通过 `MultiWindowTaskManager` 定位唯一 runner/context，并只刷新一次 binding。
- 元数据使用刷新后真实 `windowTitle/nativeHandle/processId/windowRect(left,top,width,height)` 与 stop 状态。
- capture 只调用 `BoundWindowCaptureService` 的 HWND 后台路径；禁止 Robot/前台 capture、focus 或物理点击。
- ROI 坐标为屏幕绝对像素，按真实窗口 left/top 裁剪；不缩放图像或坐标。
- PNG 使用 raw bytes；metadata 的宽高、SHA-256、region 必须与实际编码像素一致。
- 不新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/自动 retry。
- 不运行 runtime/application/server/Task/poller/UI/capture/input/tests；只在 cohort 源码稳定且可用时运行规定 compile。
- 若 TURN-01 protocol cohort 尚未齐导致编译不可用，交付状态写为 `SOURCE DELIVERED / BUILD COHORT PENDING`。
- 交付后停止，等待父级独立源码审查；本 worker 不自批、不写 APPROVED/CLOSED。

## CLAIM SUPPLEMENT - 父级合同补充

- 接收时间：`2026-07-15T14:36:00-04:00`
- 已重新读取父级更新后的 spec、权威主计划与 Foundation 附录。
- exact-window metadata 唯一协议类型固定为
  `TurnWindowMetadata(String deviceId, String windowId, String windowTitle, String nativeHandle, long processId, TurnWindowRect windowRect, boolean stopRequested)`。
- 本卡只消费 `protocol.TurnWindowMetadata`，不得在 `TurnExecutionWindow` 或其它本卡文件中另造窗口 metadata DTO。
- frame purpose 只消费 `TurnFramePurpose` 四值：`CAPTURE`、`MATCH_EVIDENCE`、`QUEST_DETAIL`、
  `FAILURE_EVIDENCE`；本卡 capture 产出使用 `CAPTURE`，不新增本地 purpose 枚举。

## DELIVERED

- 交付时间：`2026-07-15T14:38:17-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- `countUnit`：`N/A (INFRA exact-window metadata and background capture)`
- `countDelta`：`0`
- 完成内容：
  - `TurnExecutionWindow.resolveForAction(...)` 通过 `MultiWindowTaskManager.getRunner(action.windowId())`
    取得唯一 runner/context，并只调用一次 `WindowNativeBindingRefreshService.refreshAndCommit(...)`；执行快照直接持有
    协议权威 `TurnWindowMetadata`，没有另造窗口 metadata DTO。
  - metadata 使用刷新后真实 title、字符串 HWND、processId 与屏幕绝对 `left/top/width/height`；
    `stopRequested` 只反映既有 task stop token 或窗口 `STOPPING/STOPPED` 状态，不产生业务失败判断。
  - `TurnCaptureStepExecutor` 对全窗调用 `BoundWindowCaptureService.captureWindow(...)`，对 ROI 调用
    `captureRegion(...)`；ROI 必须包含于刷新后的窗口矩形，坐标和图像不缩放。
  - `TurnPngCodec` 只用 `ImageIO` 编码 raw PNG，metadata 的 width/height 直接取实际像素，region 尺寸必须与像素一致，
    SHA-256 直接由最终 PNG bytes 计算。
  - `TurnFrame` 防御性复制 PNG bytes；capture frame 只消费协议 `TurnFramePurpose.CAPTURE`，通用 capture 入口也只接受
    协议 `TurnFramePurpose` 四值。
- 源码证据：
  - 四个 Java 文件 SHA-256：`TurnExecutionWindow=A54B84E08AD65B16046BE6683421126F061F40AB8473FDF2C19F25F70D0C0666`，
    `TurnFrame=91F06FC2D6B4AFCBF5E49E75759CB68BCDF6A6619B33F9A88BC0F3AD8B6DE75A`，
    `TurnPngCodec=8A2C41B86B292E62769E47650F797661C3D3B3D078214404587B662F7AEE9B61`，
    `TurnCaptureStepExecutor=87B6D08FBC469CC5AB706AC4ABA1D61D6AE4443C19DBBD71A18C10DD79948433`。
  - 必需路径扫描命中 `refreshAndCommit` 一处、`new TurnWindowMetadata` 一处、HWND `captureWindow/captureRegion`、
    `ImageIO.write("png")` 与 `SHA-256`；四文件没有 `InputProvider`、`InputActionQueue`、click、focus/Robot capture、
    Base64、scheduler 或 retry 调用。
  - 本卡五个文件 trailing-whitespace 扫描为 `NO_TRAILING_WHITESPACE`。
- Maven 门：已运行 `mvn -q -DskipTests compile`，exit `1`；编译器只报告并行 protocol cohort 尚未交付的
  `TurnAction`、`TurnCaptureSpec`、`TurnFrameMetadata` 三个类型缺失。领取后已落盘的 `TurnWindowMetadata`、
  `TurnFramePurpose`、`TurnRegion`、`TurnWindowRect` 与本卡使用完全一致。本卡不得修改协议写集，因此按合同标记
  `BUILD COHORT PENDING`，等待 TURN-01B/01C 源码稳定后的父级 fresh Maven cohort 门。
- 边界确认：未运行 runtime/application/server/Task/poller/UI/capture/input/tests；未修改 Cloud Brain；未修改
  主计划、CR271、ACTIVE_WORK 或 dashboard；未执行 Git 写操作。
- 业务差异：无已批准业务差异；按基线等价迁移。
- 待父级：独立源码审查及 cohort compile。当前 worker 在 `DELIVERED` 停止，不自批、不写 APPROVED/CLOSED。

## PARENT SOURCE REVIEW

- 审查时间：`2026-07-15T10:41:00-04:00`
- P0：`0`
- P1：`0`
- P2：`0`
- 源码证据：父级独立展开四文件并核对既有 API。`resolveForAction` 仅按 windowId 找唯一 runner/context 并调用一次
  `refreshAndCommit`；全窗/ROI 均调用 `BoundWindowCaptureService` HWND 路径，ROI 使用真实 window left/top 与绝对像素，
  无 focus/Robot/input。PNG 宽高、region 与最终 bytes SHA 一致，bytes 防御性复制，metadata 只使用协议共享类型。
- 构建判断：worker compile exit 1 只因并行 TURN-01B/01C 尚未提供 `TurnAction`、`TurnCaptureSpec`、
  `TurnFrameMetadata`；错误与本卡合同一致，禁止越界造临时类型。最终编译归 cohort。
- 影响：提供精确窗口 metadata 与后台截图机械边界；不运行 capture、不产生业务判断或 407 ledger 增量。
- 返修条件：无。

**SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING；源码 owner 已释放，可领取下一张 READY。**
