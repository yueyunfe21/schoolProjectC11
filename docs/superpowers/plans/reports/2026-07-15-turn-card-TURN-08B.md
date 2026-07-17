# TURN-08B DHXY Explicit Local Template Match Worker Report

## CLAIMED

- 领取时间：`2026-07-15T15:41:53-04:00`
- 身份：TURN-08B implementation Worker；不是 manager/reviewer，不得自批。
- 状态：`CLAIMED / IMPLEMENTING`
- 基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- 类型：`INFRA`；`countDelta=0`
- startDependsOn：`TURN-07`、`TURN-08A`，均已父级 `SOURCE APPROVED / BUILD COHORT PENDING`。
- approvalDependsOn：冻结协议 cohort 与后续 TURN-11 action composition；本 Worker 不越过审批依赖。

### 精确写集

- Java：`src/main/java/com/bot/dhxy/cloud/turn/TurnMatchStepExecutor.java`
- 报告：`docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-08B.md`
- Cloud 全仓只读；不修改 protocol、TURN-07、TURN-08A、TURN-11、Service、Task、runner、主计划、CR271 或 dashboard。

### 实现合同

- 只执行冻结协议显式声明的 `MATCH_TEMPLATE`，不为 CAPTURE/INPUT/LOCAL_SERVICE 隐式匹配。
- 严格复用 `TurnTemplateCache` 解析 `templateKey + contentHash`，复用 `TurnCaptureStepExecutor` 从 exact bound-window
  后台抓取全窗或真实 screen-absolute ROI，并复用 `ImageFinder` 做单候选匹配。
- 坐标和图像不缩放；匹配结果从 capture-local 像素转换为真实 screen-absolute 坐标，并返回真实绝对 rectangle/center。
- miss 只返回 typed miss，绝不点击；`onMatch=CLICK` 也只返回待 TURN-11 组合的命中坐标，本卡不调用
  `InputProvider`、`InputActionQueue` 或任何输入 API。
- 不下沉候选排序、业务 fallback、OCR、retry、TTL、session 或 workflow；单 frame 规则不额外抓图。

### 风险

1. `ImageFinder` 返回 capture-local center，必须加实际 capture region 的 screen origin，不能错误使用固定 `(0,0)`。
2. template 尺寸必须参与绝对 rectangle 计算，并验证不大于 capture 图像，避免 OpenCV 断言或越界结果。
3. `RETURN_MATCH_RESULT_AND_IMAGE` 只能复用本次匹配 capture 形成的一帧，不得再次截图。
4. typed miss 与执行失败必须区分；阈值未命中不是物理失败，也不得触发点击或 retry。

### 领取时状态

- DHXY 分支：`thin-client-design`；Cloud 分支：`navigation-migration`。
- 两仓均有大量既有 dirty/untracked；全部保护，不回滚、不覆盖、不清理、不删除、不提交。
- 当前其他 Java writers 活动；本卡不运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input，
  不执行 Git mutation。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15T15:44:13-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- `countDelta=0`

### 实现结果

- `execute(window, step)` 硬性要求完整 step 的 `type=MATCH_TEMPLATE` 且存在 typed `match`，其他 step 不能隐式
  触发本地匹配。
- 模板只通过 `TurnTemplateCache.resolveTemplate(templateKey, contentHash)` 取得；本卡没有目录扫描、Cloud 算法、
  OCR 或 fallback。
- screenshot 只调用一次 `TurnCaptureStepExecutor.capture(...)`，purpose 为 `MATCH_EVIDENCE`；ROI 为 payload 的
  screen-absolute region，null 时复用 exact bound-window 全窗，坐标与像素均不缩放。
- source PNG 与 template PNG 均完整解码后交给既有 `ImageFinder.find(BufferedImage, BufferedImage, threshold)`；
  模板大于 capture 时 fail-closed，避免 OpenCV 越界/断言。
- 命中中心和 rectangle 以实际 captured frame metadata region 的 `x/y` 作为真实 screen origin 进行转换；没有固定
  `(0,0)`，也没有使用窗口标题搜索或重新刷新 binding。
- miss 返回 `TurnMatchResult(false, 0.0, null, null, null)`；不点击。命中且 `onMatch=CLICK` 时只把
  `Execution.clickRequested=true` 与绝对坐标交给后续 TURN-11，本卡不发送输入。
- `RETURN_MATCH_RESULT_AND_IMAGE` 返回本次匹配已使用的同一 `TurnFrame`；普通 `RETURN_MATCH_RESULT` 不返回 frame，
  两种模式均没有第二次 capture。
- `TurnMatchStepExecutor` 暂不标记为 Spring component；`TurnTemplateCache` 的 bean 与完整 wiring 由计划中的
  TURN-13 `TurnConfiguration` 统一建立，避免在 wiring 卡之前让当前应用上下文产生缺失 bean。

### SHA-256 与 scoped evidence

```text
B9EC094721624B60A2F24C5863A11F44824BB7A105C0E422F1BBF5D00482A64F  TurnMatchStepExecutor.java
```

- scoped status 仅列出本卡两个新文件：`TurnMatchStepExecutor.java` 与本报告。
- `captureStepExecutor.capture(` 调用数：`1`；`ImageFinder.find(` 调用数：`1`。
- `InputProvider`：`0`；`InputActionQueue`：`0`；OCR/TextRecognizer：`0`；loop：`0`；retry：`0`。
- `git diff --no-index --check -- NUL <file>` 对 Java 与报告没有 whitespace 诊断；仅有 Windows LF/CRLF 提示。
- Cloud 全仓只读；未修改 TURN-07、TURN-08A、protocol、Service、Task、runner、主计划、CR271 或 dashboard。

### 未运行项与停止点

- Maven：未运行，当前仍有其他 Java writers，遵守 build cohort 门禁。
- Tests/runtime：未创建或运行 tests；未启动 runtime/application/server/Task/poller/UI/capture/input，也未实际
  读取模板、截图、匹配或发送输入。
- Git：未 stage、commit、checkout、reset、clean、删除或覆盖任何既有 dirty/untracked。
- 业务差异：无已批准业务差异；按 `696a12b0` 基线等价迁移。
- Worker 在源码交付后停止，等待父级独立源码审查；不写 `APPROVED/CLOSED`，不自领下一卡。

### Heartbeat 状态

- 父级 `CR271父级持续审核` 已从 `F:\CodexHome\.codex\automations\cr271\automation.toml` 核实为
  `status=ACTIVE`、`FREQ=MINUTELY;INTERVAL=5`；其任务明确覆盖全部新 CLAIMED/SOURCE DELIVERED 卡的独立审查。
- 本 Worker 两次通过正式 `automation_update` API 创建 `TURN-08B返修跟进` heartbeat，调用均持续超时；终止调用后
  再查自动化目录，没有生成 TURN-08B 自动化。因此不得宣称专属 heartbeat 已建立。父级 heartbeat 仍会在下一轮
  发现本报告；若父级写入返修意见，需要由可用的 Worker 继续原卡修复。

## Parent Source Review #1

- 审查时间：`2026-07-15T15:55:00-04:00`；父级独立展开本卡源码，并回读 `ImageFinder.find`、
  `TurnCaptureStepExecutor.capture`、`TurnTemplateCache.resolveTemplate` 与冻结 match DTO。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`，owner 释放。
- 精确证据：`:45-59` 只接受显式 MATCH_TEMPLATE 且只 capture 一次；`:61-76` 对同一 frame 完整解码、单候选匹配并
  仅在协议要求时返回同一 frame；`:124-140` 用 captured metadata region 的真实 screen origin 转绝对中心与矩形；
  本卡没有 input/click/OCR/retry/fallback。`clickRequested` 只作为 TURN-11 后续组合信号，不产生物理输入。
- Worker 尝试建立卡片专属 automation 没有落盘，也不构成 review/approval 信号；该过程未改变源码或运行面，
  不要求源码返修。剩余门仅为 Java writers 稳定后的父级 DHXY compile cohort。
