# Internal Worker AX - DHXY TaskTracker panel-rect fact DTO

## Parent Direct Implementation Task - `W-TTPS-RECT-DHXY-DTO-IMP1` - 2026-07-14T00:45:00-04:00

直接实现，不写 Design。只新增 DHXY 侧 closed DTO，镜像已父级批准的本地六态结果与 Cloud 合同；
本轮不接 handler，避免与 External C 的 `LocalRemoteGameCommandHandler` 写集冲突。

### 唯一写集

- New `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerPanelRectFact.java`
- Append-only 本报告

不得修改 `RemoteWindowFactKind`、`LocalRemoteGameCommandHandler`、本地 mechanics、Cloud、schema、pom、tests
或任何其它文件。不得回滚、覆盖、清理、删除或提交他人 dirty/untracked。

### 实现合同

1. 使用附近 DTO 既有 Lombok 风格，字段恰为 `state, anchorClientX, anchorClientY,
   panelClientLeft, panelClientTop, panelClientRight, panelClientBottom, matchScore, coordinateSpace`。
2. closed state 恰为 `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/
   REPOSITION_REQUIRED/MECHANICS_FAILED`；coordinateSpace 恒 `WINDOW_CLIENT_PX`。
3. 仅 PRESENT 携全部 7 个 observation 字段；坐标 non-negative，rectangle right>left/bottom>top，
   score non-null finite。其它状态 7 字段全 null。不得写死 0.82，不新增 threshold/timestamp。
4. 不新增 retry/TTL/owner/session/permit/ledger/thread；不接 handler、不发输入。

完成后 DHXY 运行 `mvn -q -DskipTests compile`，向 EOF 追加 CLAIMED、Implementation #1、SHA、构建结果与
self-QA。自审不算父级 Approved。

保护全部 dirty/untracked；禁止 Git mutation、禁止启动运行面。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED - Internal Worker AX - 2026-07-14T00:45:49-04:00

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md` 与本固定任务日志。
- 仅认领 `W-TTPS-RECT-DHXY-DTO-IMP1`；源码写集严格限定为新建
  `src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskTrackerPanelRectFact.java`，其余仅向本日志 EOF 追加。
- 已确认目标源码尚不存在，本固定日志为现有 untracked 文件；保护共享工作区全部 dirty/untracked，
  不回滚、不覆盖、不清理、不删除、不提交、不做 Git mutation，也不启动任何运行面。

## Implementation #1 - Internal Worker AX - 2026-07-14T00:47:24-04:00

- Status: `DONE`
- Exact implementation file:
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteTaskTrackerPanelRectFact.java`
- SHA256: `EE5B09EF8C0366847F5572FE051EB688744840DA83F5728B69F626E65D6A8994`
- Build: from `D:\mavenProject\DHXY`, `mvn -q -DskipTests compile` completed with exit `0`.
- Tests: not created or run, per active no-local-test mode and this task's fixed write scope.

### Self-QA

- DTO shape uses the neighboring remote DTO style: `@Value`, `@Jacksonized`, and constructor `@Builder`.
  Its fields are exactly `state`, `anchorClientX`, `anchorClientY`, `panelClientLeft`, `panelClientTop`,
  `panelClientRight`, `panelClientBottom`, `matchScore`, and `coordinateSpace`.
- Closed states are exactly `PRESENT`, `ABSENT`, `CAPTURE_UNAVAILABLE`, `TEMPLATE_UNAVAILABLE`,
  `REPOSITION_REQUIRED`, and `MECHANICS_FAILED`; every state requires `WINDOW_CLIENT_PX`.
- `PRESENT` requires all seven observation fields, non-negative coordinates, a rectangle with
  `right > left` and `bottom > top`, and a non-null finite score. Every non-`PRESENT` state requires all
  seven observation fields to be null.
- No `0.82` threshold, timestamp, retry, TTL, owner, session, permit, ledger, thread, handler wiring,
  physical input, task decision, or local-mechanics change was added.
- Workspace safety: no Git mutation, commit, rollback, overwrite, cleanup, deletion, runtime surface, server,
  application, Task, UI, capture, or input execution was performed; only the one new Java file and append-only
  entries in this fixed report were written.
- This self-QA is implementation evidence only and is not parent/reviewer approval.

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - APPROVED - 2026-07-14T00:50:00-04:00

父级逐行复核 `RemoteTaskTrackerPanelRectFact.java`，结论 `P0=0 / P1=0 / P2=0`：

- 字段恰为 state、window-client anchor、panel rectangle、score 与 coordinateSpace；closed 六态与已批准
  AV mechanics 完全一致，coordinateSpace 强制 `WINDOW_CLIENT_PX`。
- 仅 PRESENT 允许且要求全部七个 observation 字段，坐标 non-negative、rectangle 正尺寸、score finite；
  其它五态严格要求七字段全 null。没有硬编码 0.82、timestamp 或额外行为。
- 当前 SHA256 为 `EE5B09EF8C0366847F5572FE051EB688744840DA83F5728B69F626E65D6A8994`；
  Worker DHXY `mvn -q -DskipTests compile` exit 0。父级 fresh compile 留待整波 Java 稳定后执行。

本 DTO `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
