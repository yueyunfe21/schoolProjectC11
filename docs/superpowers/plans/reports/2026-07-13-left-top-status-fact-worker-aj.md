# Internal Worker AJ - Left-Top Status Typed Local Fact

## Parent Task Brief #1 / `W-LTS-FACT-IMP1` - 2026-07-13T19:54:00-04:00

直接实现，不写 Design #N。你不是仓库中唯一 Worker；保护两仓全部 dirty/untracked，不回滚、覆盖、清理、
重命名或提交他人改动。先读 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、
`docs\superpowers\plans\2026-07-13-direct-service-input-bundle-migration.md`，以 committed `0114604e` 的
`LeftTopStatusSwitchService` 状态/阈值/ROI/template 顺序为业务依据。

目标：把“左上状态开关本地截图+模板判断”暴露为现有 `WINDOW_FACT` 的 closed typed fact；只读，不点击。
Cloud 后续 Service 收到 OPEN 点位后再通过一个普通 InputBundle 点击。不得新增 operation、状态机、owner、
permit、ledger、TTL、线程、轮询或 retry。

唯一 Java 写集：

- Cloud：`WindowFactKind.java`、`WindowFact.java`、`WindowFactOutcome.java`、
  `RemoteCommandOutcomeEnvelope.java`。
- DHXY：`RemoteWindowFactKind.java`、新增一个最小 immutable fact DTO（放 `com.bot.dhxy.cloud.remote`）、
  `LocalRemoteGameCommandHandler.java`、`LeftTopStatusSwitchService.java`。
- 如且仅如构造签名导致编译必须，允许修改现有 dormant handler 的唯一装配文件；先在报告写明实际文件。

实现约束：

- 新 kind 命名 `LEFT_TOP_STATUS`；fact closed state 与基线 `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED` 对齐，
  不发送 `SKIPPED`。
- fact 携带 openScore、closedScore、仅 OPEN 时的 clickX/clickY、明确
  `SCREEN_ABSOLUTE_PX`；非 OPEN 点位必须 null。不得传 raw path、图片 bytes 或模板内容。
- DHXY `LeftTopStatusSwitchService` 只增加一个复用现有 `detect(...)` 的 read-only typed probe；不得复制
  `resolveState`、不得点击、不得改变现有 public flow/pending 语义。
- handler 必须使用 exact admitted `BindingAccess`，在 `windowTaskContextHolder.callWith(access.context(), ...)`
  内调用 probe，并保留执行后 registration/binding/revision fence；不得 title-search。
- Cloud parse/match 必须严格 kind/variant 对应；现有四种 fact 的 wire/digest 零变化。
- 不改 `CloudTaskServiceExecutionContext.java`、`CloudTaskServicePort.java`、input facade 文件、host、tests；
  不新增/恢复测试。

完成后先运行 DHXY `mvn -q -DskipTests compile`，再运行 Cloud `mvn -q clean package`（不得 skip tests）。
向本日志真实 EOF 追加 `Internal Worker AJ - W-LTS-FACT-IMP1 Implementation #1`，列精确文件、wire 对称性和
双构建统计。自审仅 QA，不算父级批准。无已批准业务差异；按 `0114604e` 基线等价迁移。

## CLAIMED

- task: `W-LTS-FACT-IMP1`
- claimedAt: `2026-07-13T19:59:55-04:00`
- uniqueWriteSet:
  - Cloud: `WindowFactKind.java`, `WindowFact.java`, `WindowFactOutcome.java`, `RemoteCommandOutcomeEnvelope.java`
  - DHXY: `RemoteWindowFactKind.java`, one new immutable fact DTO under `com.bot.dhxy.cloud.remote`, `LocalRemoteGameCommandHandler.java`, `LeftTopStatusSwitchService.java`
  - Report: this file, append-only

## Internal Worker AJ - W-LTS-FACT-IMP1 Implementation #1

- 状态：实现完成，等待父级 QA/批准；本轮仅做实现与构建验证，未写 Design、未提交。
- 精确写入文件：
  - Cloud：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/WindowFactKind.java`、
    `WindowFact.java`、`WindowFactOutcome.java`、`RemoteCommandOutcomeEnvelope.java`。
  - DHXY：`src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java`、
    `RemoteLeftTopStatusFact.java`、`LocalRemoteGameCommandHandler.java`、
    `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`。
  - 未修改任何构造装配文件：仓库内未发现 `new LocalRemoteGameCommandHandler(...)` 调用，且 DHXY
    编译已通过，无构造签名导致的额外编译阻塞。
- 实现摘要：
  - 两侧新增现有 `WINDOW_FACT` 的 `LEFT_TOP_STATUS` kind；Cloud sealed `WindowFact` 增加
    `LeftTopStatusFact`，`WindowFactOutcome` 做 kind/variant 严格匹配，
    `RemoteCommandOutcomeEnvelope.parseFact` 增加对应解析分支。
  - DHXY `LeftTopStatusSwitchService.probeLeftTopStatusFact(source)` 只调用既有
    `detect(source)`，映射 `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED`，输出 `openScore`、
    `closedScore`、`SCREEN_ABSOLUTE_PX`；只有 `OPEN` 输出成对 `clickX/clickY`，其它状态为 null，
    不携带 raw path、图片 bytes 或模板内容，不改变 pending/public flow，也不发送输入。
  - `LocalRemoteGameCommandHandler.executeWindowFact` 在已准入的 `BindingAccess` 上使用
    `windowTaskContextHolder.callWith(access.context(), ...)` 调用 probe；既有执行后
    registration/binding/revision fence 原样保留。现有四类 fact 的 wire/digest 分支未改动。
- 基线核对：`LeftTopStatusSwitchService` 相对 committed `0114604e1ff5f15491d2910959c45252e893d04f`
  的业务实现仅增加上述 read-only typed probe；ROI、template 顺序、阈值、状态判定、点击与 pending
  语义均复用/保持不变。无已批准业务差异；按 `0114604e` 基线等价迁移。
- 构建证据（按要求顺序）：
  - DHXY：`mvn -q -DskipTests compile`，exit code `0`，成功。
  - Cloud：第一次 `mvn -q clean package` 在既有 shade artifact 替换阶段瞬态失败；未出现本次代码编译错误。
    未改 pom 或清理用户文件，按同一命令重试。
  - Cloud 重试：`mvn -q clean package`，exit code `0`，成功；测试未跳过。
- 保护确认：未回滚、覆盖、清理、重命名或提交；未触碰 `CloudTaskServiceExecutionContext.java`、
  `CloudTaskServicePort.java`、AI context/input facade、host 或 tests；两仓其它 dirty/untracked 保持原状。

## Parent Source Review #1 - APPROVED - 2026-07-13T20:16:00-04:00

父级已逐行复核八个实际文件及既有 post-operation fence，结论 `P0=0 / P1=0 / P2=0`：

- 双仓 kind/state/字段名和 `SCREEN_ABSOLUTE_PX` 枚举值严格对称；Cloud sealed union、parser 与
  `WindowFactOutcome` kind/variant 门均已闭合。OPEN 必须成对坐标，非 OPEN 禁止坐标，scores 必须 finite。
- 本地 probe 只调用 baseline 既有 `detect(source)`，没有复制 `resolveState`、点击、pending mutation、图片/raw path
  上 wire 或新增 retry；`0114604e` ROI/template/阈值/顺序与原 public flow 未改。
- handler 使用 admitted `BindingAccess` 的 exact `WindowRuntimeContext` 执行 probe，并在返回前继续走既有
  registration runRevision 与 nativeHandle/processId/playerIdentityEpoch 复验；没有 title-search 或错窗回退。
- Worker DHXY compile exit 0，Cloud clean package 重试后 exit 0 且未 skip tests；第一次 shade artifact 替换失败
  没有对应源码编译错误。父级 fresh 构建仍在本批 B/D 接缝修复稳定后统一重跑。

结论：`W-LTS-FACT-IMP1 SOURCE APPROVED`。无已批准业务差异；按 `0114604e` 基线等价迁移。
