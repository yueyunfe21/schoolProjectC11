# TURN-10B Report — UICleanerService closed adapter

## CLAIMED

- 领取时间：`2026-07-15T15:44:00-04:00`。
- 状态：`CLAIMED`；`countUnit=N/A (INFRA closed UI adapter)`；`countDelta=0`。
- `startDependsOn`：父级已明确 `TURN-10P` 与 `TURN-01D` 均为 `SOURCE APPROVED / BUILD COHORT PENDING`，并续派本卡。
- 唯一 Java 写集：`src/main/java/com/bot/dhxy/cloud/turn/local/UiLocalOperationExecutor.java`；唯一文档写集为本报告。
- 只读：`LocalServiceExecution`、`UICleanerService`、冻结 `TurnLocalOperation`/protocol 类型。禁止修改
  `UICleanerService`、其它 Service/DTO/wire、主计划、CR271、配置及任一既有 dirty/untracked；不回滚、覆盖、
  清理、提交或其它 Git mutation。

## 两仓领取瞬间 git status

- DHXY：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`；已有 config/docs/pom、
  input/runner/service/task/window Java 修改，以及 `docs/superpowers/**`、`cloud/remote`、`cloud/turn`、
  多个 Service/model 路径未跟踪，全部保护。
- Cloud Brain：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`；已有 pom/server/
  engine/gateway/maps 修改，以及 logs、migration、`com/bot`、host/remote/turn 等未跟踪，全部保护。
- 本卡不写 Cloud Brain；不启动 Maven、tests、runtime/application/server/Task/poller/UI/capture/input。

## BLOCKED — required shared result absent

- 发现时间：`2026-07-15T15:44:00-04:00`；状态：`BLOCKED`。
- 精确证据：父级要求只读复用
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\local\LocalServiceExecution.java`，但当前
  `cloud/turn/local/` 目录及该文件均不存在；`Get-Content`/`Get-ChildItem` 均返回 path not found。
- 影响：`UiLocalOperationExecutor` 的返回类型及 completed/failed code、small JSON、可选 Quest frame 的冻结承载
  合同不可读取。自行新建或猜测 `LocalServiceExecution`、返回类型、constructors 或 wrapper 将扩大写集并改变 wire/
  typed contract，违反本卡唯一 Java 写集和父级“不得包装业务 DTO”的限制。
- 已完成只读核对：`UICleanerService` 现有可复用入口为 `cleanUpAll()`、`closeAllGenericWindows()`、
  `cleanLightweightInterruptions(String)` 与仅限既有 exclusive callback 的
  `closeMapSearchInputByX2Direct(String)`；后者不得由 adapter 二次排队或拆开。冻结 allowlist 也仅含父级给定四项。
- 所需父级裁决：请待 `TURN-10P` 的 `LocalServiceExecution.java` 在当前工作区实际可读后重派/恢复本卡，或明确
  允许的既有替代 typed result 路径。此前不创建 Java、不扩写集、不运行 Maven/tests/runtime。

## RESUMED — Parent Blocker Resolution #1

- 恢复时间：`2026-07-15T15:46:00-04:00`；父级已澄清冻结共享结果的真实只读路径为
  `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceExecution.java`，package 为
  `com.bot.dhxy.cloud.turn`，不位于 `cloud.turn.local`。
- 已完整读取该真实路径：它提供 immutable `LocalServiceExecution.completed(code,json,frame)` /
  `failed(code,json)`，因此 adapter 只需 import 复用，不新建、不修改该类型，也不扩大本卡唯一写集。
- 原 BLOCKED 仅因错误路径而产生，现恢复 `CLAIMED`；继续只写
  `cloud/turn/local/UiLocalOperationExecutor.java` 与本报告。`UICleanerService` 仍严格只读。

## SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15T15:49:00-04:00`；状态：`SOURCE DELIVERED / BUILD COHORT PENDING`；
  `countUnit=N/A (INFRA closed UI adapter)`；`countDelta=0`。
- 唯一 Java 交付：`src/main/java/com/bot/dhxy/cloud/turn/local/UiLocalOperationExecutor.java`，SHA-256
  `BE41C2224A1B2F484305D6DAAB2FAD311CB7B30BE1F6992159780C6830DD62D7`。
- 闭合 allowlist：`:39-45` 仅 switch
  `UI_CLEAN_ALL`、`UI_CLOSE_GENERIC_WINDOWS`、`UI_CLEAN_LIGHTWEIGHT`、
  `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2` 四项；default 返回
  `LocalServiceExecution.failed("UNSUPPORTED_LOCAL_OPERATION", null)`，未知 operation fail closed。
- 现有 UI 机械语义：`:52` 直接保留 `cleanUpAll()`；`:60` 保留 `closeAllGenericWindows()`；`:68` 保留
  `cleanLightweightInterruptions(source)`；`:76` 直接调用既有
  `closeMapSearchInputByX2Direct(source)`。adapter 本身没有 `InputActionQueue`、`submitAndWait`、
  `submitExclusiveAndWait`、`InputProvider` 或 click 调用，故不二次排队、不拆开 X2 的既有 direct macro。
- 参数边界：无参的 clean-all/generic operation 拒绝任一 argument group；两个 source operation 只接 nonblank
  UI source 且拒绝其它 group。结果仅复用父级指定的
  `com.bot.dhxy.cloud.turn.LocalServiceExecution`，以 typed `UiOperationResult(operation, handled)` 序列化为
  small JSON；未包装业务 DTO、未引入第五 Service、反射/任意 map/OCR/capture/input/business fallback/retry。
- 静态证据：本文件 `git diff --check` exit `0`；allowlist case=`4`、fail-closed default=`1`、直接
  `UICleanerService` 调用=`4`、queue/input API 调用=`0`。
- 当前 writers 活动，未运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input；未修改
  `LocalServiceExecution`、`UICleanerService` 或任何其它文件，未执行 Git mutation。等待父级唯一 reviewer
  复审；不自批、不领取下一卡。

## Parent Source Review #1

- 审查时间：`2026-07-15T15:51:00-04:00`；结论：`REPAIR REQUIRED / BUILD COHORT PENDING`；
  `P0=0 / P1=1 / P2=0`。
- P1：此前 adapter 对 X2 直接调用 `closeMapSearchInputByX2Direct`，而其它三项公共 UI flow 在需要时会自己
  submit queue，导致 dispatcher 若在 queue 外调用会让 X2 绕过 queue；若在 exclusive 内调用又会使前三项
  queue-in-queue deadlock。
- 父级精确返修：统一 adapter 由 input worker 外调用；构造注入既有 `InputSequences`；仅 X2 在 adapter 内
  一次 `submitExclusiveAndWait` callback 调用 direct macro；前三项仍直接走既有 public flow；不得改
  `UICleanerService`、拆 X2 或新增其它机制。

## Repair #1 CLAIMED / SOURCE DELIVERED

- 返修时间：`2026-07-15T15:53:00-04:00`；状态：`SOURCE DELIVERED / BUILD COHORT PENDING`。
- 仅修改原批准 Java 写集
  `src/main/java/com/bot/dhxy/cloud/turn/local/UiLocalOperationExecutor.java`，未修改
  `LocalServiceExecution`、`UICleanerService` 或其它文件。
- `:6/:19-26` 注入既有 `InputSequences`；`:82-84` 仅对
  `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2` 一次调用 `submitExclusiveAndWait("turn:ui-close-map-search-x2:" + source, ...)`，
  callback 内直接调用既有 `closeMapSearchInputByX2Direct(source)`，没有二次 queue 或拆分 capture+click。
- `:58/:66/:74` 的 `cleanUpAll()`、`closeAllGenericWindows()`、`cleanLightweightInterruptions(source)` 继续直接
  调用各自现有 public flow，未加外层 queue。`execute(...)` JavaDoc 已明确统一 input-worker 外边界，且禁止未来
  dispatcher 将整个 adapter 再包进 exclusive。
- 当前 SHA-256：`50CA240F0444B2B27C2EC97F4BA6650BBDC3E109DBC1106F406B40D4DBACEEF9`；本文件
  `git diff --check` exit `0`。静态核对：四项 allowlist 与 fail-closed default 未变；唯一
  `submitExclusiveAndWait` 命中是 X2 `:82`；另外三项 public UI calls 仅为 `:58/:66/:74`。
- 未运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input；未执行 Git mutation。等待父级唯一
  reviewer 复审；不自批、不领取下一卡。

## Parent Re-review #1

- 复审时间：`2026-07-15T16:00:00-04:00`；父级独立展开 Repair #1 源码，并回读 UICleanerService 四个入口与
  InputSequences exclusive API。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`，Parent Review #1 的 P1 关闭，
  External owner 释放。
- 证据：`:30-36` 已固定 adapter 从 input worker 外调用；`:54-74` 三个 public flow 不包外层 queue；`:77-85`
  仅 X2 通过一次 `submitExclusiveAndWait` 进入既有 direct macro，capture+click 未拆分，也没有直接 InputProvider。
- 当前 SHA 为 `50CA240F0444B2B27C2EC97F4BA6650BBDC3E109DBC1106F406B40D4DBACEEF9`。剩余门仅为
  DHXY Java writers 稳定后的父级 compile cohort；下一张 integration 卡等待 TURN-10C/10D source review 解锁。
