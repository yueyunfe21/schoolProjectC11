# TURN-10P Report - 本地 Service 共享执行结果

## CLAIMED

- 领取时间：`2026-07-15T15:00:34-04:00`
- 角色：`Internal implementation worker`，不是 manager/reviewer，不得自批。
- 状态：`CLAIMED`
- `countUnit`：`N/A (INFRA shared local-Service execution result)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-01C`、`TURN-08A`；两卡均已父级 `SOURCE APPROVED / BUILD PENDING`。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；无已批准业务差异。
- 唯一 Java 写集：
  - `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceExecution.java`
- 唯一报告写集：
  - `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-10P.md`
- Cloud Brain：全仓只读。

## 实施边界与风险

- 只实现一个 immutable typed 结果；精确复用冻结的 `TurnStepResult.Status` 与 `TurnFrame`，不复制、包装或解释任何业务 DTO。
- 只允许 `COMPLETED` / `FAILED`；稳定 code 必须非空，任何其它状态或无 code 组合 fail-closed。
- `localResultJson` 只允许可选小 JSON 字符串，设置明确 UTF-8 byte 上限；字符串本身不可变，不引入任意 map。
- 可选 frame 只允许单个 `QUEST_DETAIL` raw PNG，并核对 metadata、尺寸、region、SHA-256 与实际 bytes；不允许第二张图。
- 不调用 `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService` 或其它 Service；不创建 dispatcher、wrapper 或 workflow。
- 禁止新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/自动 retry。
- 当前存在并行 Java writers，本卡禁止 Maven；不创建/运行 tests，不启动 runtime/application/server/Task/poller/UI/capture/input，不做 Git mutation。

## SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15T15:02:15-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- `countDelta`：`0`
- Java 交付：`src/main/java/com/bot/dhxy/cloud/turn/LocalServiceExecution.java`
- SHA-256：`1BF1566300C4E34D5F62D7F08467F51408C71DA13D92076EDCEC1C2683B2DD00`

### 实现结果

- 使用一个 Java record 承载冻结的 `TurnStepResult.Status`、稳定 code、可选 `localResultJson` 与可选
  `TurnFrame`；没有复制或包装业务 DTO。
- canonical constructor fail-closed：只允许 `COMPLETED` / `FAILED`，两种状态都要求非空且无首尾空白的 code；
  `NOT_RUN` 与 null 状态均拒绝。
- `localResultJson` 为可选不可变字符串；非 null 时必须非空白，UTF-8 编码不得超过 `64 KiB`。
- 单个 frame 只允许出现在 completed 结果，purpose 必须为 `QUEST_DETAIL`；raw PNG 上限沿用协议 `8 MiB`，并核对
  PNG signature、PNG header 实际宽高、metadata region/width/height、sourceStepIndex 与 SHA-256。failed 结果不能带图，
  类型本身也没有第二个 frame 槽。
- `TurnFrame` 已在冻结类型中对 raw PNG bytes 做构造与 accessor 防御性复制；本卡直接复用该不可变边界。
- 提供 `completed(...)` / `failed(...)` 两个合法组合工厂，不创建 dispatcher、workflow 或 Service wrapper。

### Scoped checks

- 唯一两个写集路径在 scoped `git status --short` 中均为新增；未触碰 Cloud Brain。
- Java 文件 trailing-whitespace 扫描：`NONE`。
- Service/dispatcher 扫描：无 `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService`、
  `LocalServiceStepDispatcher`、Spring bean 注解或 `com.bot.dhxy.service` import。
- 禁止策略扫描：无 owner/permit/session/ledger/compaction/durable workflow/TTL/retry/scheduler。
- 未运行 Maven：当前并行 Java writers 仍活动，构建按 Foundation cohort 由父级统一执行。
- 未创建/运行 tests，未启动 runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。

## Worker handoff

- 本 worker 到此停止，等待父级独立源码审查与 P0/P1/P2 裁决。
- 本报告不写 `APPROVED` / `CLOSED`，也不领取下一卡。

## Parent Source Review #1

- 审查时间：`2026-07-15T15:14:00-04:00`；父级独立逐行审查 `LocalServiceExecution.java`。
- 结论：`REPAIR REQUIRED / BUILD COHORT PENDING`；`P0=0 / P1=1 / P2=0`。
- **P1 - Quest PNG 未被完整解码验证。** `LocalServiceExecution.java:135-149` 只通过 `ImageReader`
  读取 width/height；该操作通常只读取 PNG header/metadata，截断 IDAT、损坏像素流或 CRC 异常的 PNG 仍可能
  通过本地结果边界，随后在 Cloud 完整 `ImageIO.read` 时冲突。
- 返修条件：保持 public API/字段/上限不变，在同一 helper 内完整解码 raw PNG 像素（如
  `ImageIO.read` 或 `ImageReader.read(0)`），确认非 null 且实际宽高匹配 metadata，并释放 image/reader；
  不修改 `TurnFrame`、protocol 或任何 Service。
- 复验点：父级确认完整像素 decode 与资源释放存在，且单 frame、QUEST_DETAIL、SHA、尺寸合同未弱化。
- Maven：writers 活动中，仍不运行；本结论不允许记 APPROVED/CLOSED。

## Repair #1 DELIVERED

- 返修时间：`2026-07-15T15:36:28-04:00`
- 状态：`REPAIR #1 DELIVERED / PARENT RE-REVIEW / BUILD COHORT PENDING`
- 修复 P1：`requireMatchingPngDimensions(...)` 不再调用 header-only 的 `ImageReader.getWidth/getHeight`；
  现在通过 `ImageReader.read(0)` 完整解码第一个 PNG image，要求结果非 null，并使用解码后的
  `BufferedImage.getWidth/getHeight` 对齐冻结 metadata。
- 资源释放：完整解码位于 `try/finally`；非 null `BufferedImage` 必定调用 `flush()`，`ImageReader` 必定调用
  `dispose()`，外围 `ImageInputStream` 继续由 try-with-resources 关闭。
- 保持不变：public record 字段、`completed(...)` / `failed(...)` API、`64 KiB` JSON 上限、`8 MiB` Quest frame
  上限、单 frame、`QUEST_DETAIL`、raw PNG signature、region/sourceStepIndex、SHA-256 与 completed-only 合同均未弱化。
- 新 SHA-256：`LocalServiceExecution.java=DD471D2E964AAB51FE9250EE098A13C55D324798AC543B6D13559E10749B7458`。

### Repair #1 scoped evidence

- 完整像素 decode：`reader.read(0)` 存在；decoded null fail-closed；宽高读取自 decoded `BufferedImage`。
- 资源释放：`decoded.flush()` 与 `reader.dispose()` 均位于同一 `finally`。
- header-only 扫描：无 `reader.getWidth` / `reader.getHeight`。
- Service/越界扫描：无四个永久本地 Service、`com.bot.dhxy.service` 或 dispatcher 引用；未修改
  `TurnFrame`、protocol、Service 或 Cloud Brain。
- trailing-whitespace：`NONE`；scoped status 仍只有本卡 Java 与本报告两个新增路径。
- 按指令未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。

本 worker 再次停止，等待父级复审；不写 `APPROVED` / `CLOSED`，不领取下一卡。

## Parent Re-review #1

- 复审时间：`2026-07-15T15:39:00-04:00`；父级独立读取 Repair #1 当前源码。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`。
- 证据：`LocalServiceExecution.java:136-164` 已通过 `ImageReader.read(0)` 完整解码像素，非 null 后以
  decoded image 核对宽高，并在 finally 中 `flush()`/`dispose()`；header-only `getWidth/getHeight` 路径已消失。
  public API、单 frame、QUEST_DETAIL、SHA/region/size 上限未弱化。
- 原 P1 已关闭，源码 owner 释放；`TURN-10A/10B/10C/10D` 的 start dependency 已满足。Maven 仍待
  writers 稳定后统一执行。
