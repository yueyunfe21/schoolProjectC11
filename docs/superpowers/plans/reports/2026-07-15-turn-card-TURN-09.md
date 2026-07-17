# TURN-09 Report - DHXY 完整 input step

## CLAIMED

- 领取时间：`2026-07-15T14:38:47.4203430-04:00`
- 状态：`CLAIMED`（父级误判状态短暂关闭后，用户已明确恢复并要求继续本卡）
- 角色：Internal implementation worker；不是 manager/reviewer，不自批。
- `countUnit`：`N/A (INFRA typed input executor)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-00`（已 CLOSED）
- `approvalDependsOn`：`TURN-01D`
- 业务差异：无已批准业务差异；按基线等价迁移。

## 精确写集

- `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnKeyMapper.java`
- `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-09.md`（本报告，只追加后续状态/证据）

禁止触碰：任何其它 Java/protocol 文件、Cloud Brain 文件、`BoundWindowKeyboardService`、`InputActionQueue` 及
input queue 全部相关源码、Service/server/runner、Maven/config、主计划、CR271、`docs/ACTIVE_WORK.md` 和 dashboard。
不得回滚、覆盖、清理或提交任何既有 dirty/untracked。

## 两仓 Status 与基线

### DHXY

- 当前分支：`thin-client-design`
- HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- 当前分支无 upstream；最新远端参考 `origin/master`：`0468cc101b383700e224e7e4bf2fee551de930f1`。
- 领取时存在大量父级/其它 worker 的既有 dirty/untracked，包括 config、docs、`pom.xml`、input/service/task、
  `cloud/remote/`、`cloud/turn/`、mechanics/model 与模板；全部保护，不处置。
- 三个目标 Java 与本报告在 working tree、HEAD、`origin/master` 均不存在。

### Cloud Brain

- 当前分支：`navigation-migration`
- HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- 最新远端参考 `origin/main`：`48e37813913094cacd1143fcae02704033eecb93`。
- 领取时既有 Maven/server/engine/algorithm/gateway/maps 修改及大量 migration/turn/remote/template 未跟踪内容；
  本卡不写 Cloud Brain，全部保护。

## 只读依赖基线

- `BoundWindowKeyboardService.java`：`BB2F9C4C3E693760C52D8036900234C1A6B924B3E415B07F3BDAAFB5003B7F8D`
- `InputAction.java`：`3902E07C309BF1C15D2D341507CCDBC9760CB63DAB1C46BF2DBAAEE90B9DB13F`
- `InputActionType.java`：`102491A743B5156F51758ADA43E2950646B534357E3A519EC96515578147A343`
- `InputActionQueue.java`：`5C4391C8FBDA59E5E5B3EA7E971DEF1F8ABC03447F82C8E2BA1340A938A56BA9`
- `InputActionExecutionResult.java`：`207EF15A73B1B38634258DA14DFD55E206E0C20B0A4A67E7B70703A71FA0041F`
- `TurnInputAction.java`：`B727EC909648FA7232AD4DA1C53C75D1FB9F01BB1C16168E22D483599D3C8067`
- `TurnInputSpec.java`：`39E47A024D4AB6CF05AA6E8B5332A588DF9C49E12F61C69CA8721F96319AF4E0`
- `TurnStep.java`：`43267F87BB91FDFC78B703D0046DF16D858B82DC0A8F33FC52B0529050BA4F63`

## 执行边界

- 完整穷尽 `CLICK_LEFT`、`CLICK_RIGHT`、`DOUBLE_CLICK_LEFT`、`DOUBLE_CLICK_RIGHT`、`DRAG_LEFT`、
  `SCROLL`、`KEY_TAP`、`KEY_DOWN`、`KEY_UP`、`TEXT_INPUT`。
- 鼠标动作只映射为现有 `InputAction` 并通过现有全局 `InputActionQueue`；不得直接调用 `InputProvider`。
- 只有 `BoundWindowKeyboardService.AltShortcut.backgroundHwndSupported()==true` 的已验证 `KEY_TAP` 才调用 HWND；
  其它键、down/up 和 Unicode text 在现有只读 API 无法后台表达时返回 typed `BACKGROUND_KEY_UNSUPPORTED`，
  不得前台 fallback。
- 双击、滚轮等若底层没有单一 primitive，只能在一个 queue request 中形成原子 ordered actions，不改 queue 语义。
- 不新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/自动 retry。
- 不运行 runtime/application/server/Task/poller/UI/capture/input/tests。
- 本卡属于 Foundation build cohort；其它 Java writers 未稳定时交付标记
  `SOURCE DELIVERED / BUILD COHORT PENDING`，不伪造 Maven 通过。
- 交付后停止，等待父级独立审查。

## DELIVERED

- 交付时间：`2026-07-15T14:42:55.9449139-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- 完成内容：
  - `TurnInputActionMapper` 穷尽六种鼠标动作。左右单击、左右双击、左键拖动均使用 screen-absolute
    coordinate；双左击的两个 click 与 scroll 的 `moveMouse + wheel` 保持在同一个 queue request。
  - `SCROLL` 使用非零 signed `scrollDelta`：正值向下、负值向上；鼠标点与 drag 两端都必须位于当前 refreshed
    `TurnWindowRect` 内，不做坐标缩放。
  - `TurnKeyMapper` 只解析现有 `AltShortcut` 中 `backgroundHwndSupported()==true` 的 key tap，接受其
    `Alt+Q` display 形式或 `ALT_Q` enum 形式；`ALT_A/ALT_C` 等未验证键不会穿透。
  - `TurnInputStepExecutor` 使用 `WindowTaskContextHolder.callWith(window.context(), ...)` 绑定 exact window；
    鼠标只提交现有 `InputActionQueue`，已验证 key tap 只调用 `BoundWindowKeyboardService.pressShortcut`。
  - `KEY_DOWN`、`KEY_UP`、`TEXT_INPUT`、未知或未验证 `KEY_TAP` 全部返回 typed
    `BACKGROUND_KEY_UNSUPPORTED`；不存在前台 keyboard/text fallback。
  - executor 返回 typed `Status/Code/Result`，区分 `COMPLETED`、`FAILED`、`STOPPED`、无效输入、queue 失败、
    unsupported background key 与 HWND delivery failure；`waitFor` 使用 `TaskSleep.sleep` 保持 WAIT 可中断。
- 目标源码 SHA-256：
  - `TurnInputActionMapper.java`：`A8940C3AF34ED732BDE879F4FC83A286BA88FE7B624A6BB0B1E53CFCCE4A3A36`
  - `TurnInputStepExecutor.java`：`AC38E86A3ED847681FC4A5A929FFB7129DFFB3CCFBF110AF423B92D5F11E0432`
  - `TurnKeyMapper.java`：`D2BF68450C2A0709045B6BC3F03C9447E84DD195940713019E3B0CFBBA0A8958`
- 只读依赖复核：领取时记录的八个 dependency hash 交付时逐项完全一致；未修改
  `BoundWindowKeyboardService`、`InputAction`、`InputActionType`、`InputActionQueue`、
  `InputActionExecutionResult` 或 protocol DTO。
- 静态证据：十值 switch 穷尽；目标源码 `InputProvider`、foreground key、`pressEnter`、`typeTextUnicode`、
  `pasteText`、直接 `pressAlt*` 命中 `0`；trailing whitespace `0`。唯一 mouse sink 为
  `inputActionQueue.submitAndWait`，唯一 keyboard sink 为 `keyboardService.pressShortcut`。
- 精确 scope 证据：scoped status 仅显示本报告及三个目标 Java 为未跟踪；未修改写集外文件，未回滚、覆盖、
  清理或提交任何既有 dirty/untracked。
- 构建状态：未运行 Maven。`TURN-01D` 的 `TurnProtocolValidator.java` 尚未交付，且 Foundation Java writers
  仍在活动写入期；按主计划由父级在 cohort 稳定后统一执行 fresh DHXY compile，不伪造单卡构建通过。
- 未运行：runtime/application/server/Task/poller/UI/capture/input/tests。
- `countDelta`：`0`；未产生 407 ledger 增量。
- 业务差异：无已批准业务差异；按基线等价迁移。
- 下一步：停止实现并等待父级独立源码审查、cohort Maven 门与裁决；本 worker 不自批。

## PARENT SOURCE REVIEW #1

- 审查时间：`2026-07-15T14:55:00-04:00`
- 结论：`SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING`；`countDelta=0`。
- 父级独立展开三个目标 Java，并对照 `InputAction`、`InputActionWorker`、`InputActionQueue`、
  `BoundWindowKeyboardService`、`WindowTaskContextHolder` 与冻结 input 合同复核：六种鼠标动作均以
  screen-absolute 坐标进入单一 queue request；没有直接 `InputProvider`；只有
  `backgroundHwndSupported()==true` 的既有 shortcut 进入 HWND；其它 keyboard/text 形态返回 typed
  `BACKGROUND_KEY_UNSUPPORTED`，没有前台 fallback；WAIT 使用可中断 `TaskSleep`。
- 原计划中“现有 API 无法表达即将本卡 BLOCKED”的歧义由父级按用户已批准协议收口：V1 必须保留完整 typed
  vocabulary，但本地无法后台安全表达的动作以 typed failure 闭合，不要求扩写本地 keyboard API，也不阻断本卡源码。
- Build gate：等 `TURN-01B/01C/01D` 与 Foundation cohort 稳定后由父级统一执行 fresh DHXY compile；当前
  Java writers 活动，不运行 Maven。源码 owner 已释放，可领取下一张 READY。
