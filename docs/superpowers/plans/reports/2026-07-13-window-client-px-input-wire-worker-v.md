# WINDOW_CLIENT_PX Input Wire - Internal Worker V

## Parent Task Brief #1 - `W-CLIENTPX-IMP1` - 2026-07-13T07:23:00-04:00

### 角色、目标与唯一写集

- 你是 Internal Worker V，只做实现与 Worker QA，不是 reviewer；父级独立审查。你不是独自在仓库工作，保护两仓全部
  dirty/untracked，不回滚、不覆盖他人改动、不提交。先在本日志追加 `CLAIMED`（任务、时间、唯一写集）再开始。
- 目标：落实已批准 LeftTop D3 的通用 typed input 前置，使现有 `EXECUTE_INPUT_BUNDLE` closed coordinate enum 真正支持
  `WINDOW_CLIENT_PX`。Cloud 只放行既有 enum；DHXY 必须在副作用提交前取得 current exact binding，把 client-relative
  coordinate action 转成 screen absolute，再进入现有单一 input queue、runRevision/stop/window/worker-admission safety fence。
- 唯一 Java/文档写集：
  1. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/InputBundleRequest.java`
  2. DHXY `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
  3. DHXY `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
  4. 仅确有必要时 DHXY `src/main/java/com/bot/dhxy/cloud/remote/RemoteInputActionMapper.java`
  5. `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`
  6. 本固定 append-only 日志。
- 其它 Java、DTO/enums、digest、Maven/resources/tests、business Service/Task、assembly/host/caller、A/B/U 日志全部冻结。

### 必须保持的不变量

1. 两仓 enum 已有 `WINDOW_CLIENT_PX`，不改 enum，不改 ordinal/string spelling，不增 protocol version。
2. 普通 `SCREEN_ABSOLUTE_PX` payload、canonical digest、校验、坐标、执行时序逐字/逐值不变。coordinateSpace 已在 digest
   内，新增合法分支不得改 field order或普通请求 bytes。
3. client-relative 坐标只允许有坐标的 action（click/right/double/move/drag start+end）做 `binding.x/y + relative`；sleep/
   key/text/scroll 等无坐标 action原样。使用 exact arithmetic，overflow/outside current client geometry 在任何 input step 前
   `NOT_EXECUTED + INVALID_REQUEST/WRONG_WINDOW`，不能 clamp。
4. 初次 handler admission可预检，但真正转换和 absolute inside-window validation 必须在 `callWith` 内、重新执行 current
   registration/binding/runRevision fence 后完成；转换后仍由 worker-admission fence和现有 mid-bundle safety控制窗口漂移。
5. 整个 bundle 仍一次提交 `InputActionQueue.submitRemoteAndWaitDetailed`，不得拆 move/click、不得 queue-in-queue、不得新增
   retry/TTL/thread/poller。副作用前失败为 NOT_EXECUTED；开始任一步后沿用现有 UNKNOWN/STOPPED矩阵。
6. 不改 LeftTop 业务、不启用 caller/host，不新增/恢复 DHXY tests。只运行强制构建：Cloud `mvn -q clean package`（不 skip）
   与 DHXY `mvn -q -DskipTests compile`。不启动 application/server/Task/UI/capture/input。
7. 修改前先记录两仓 git status 和 scoped diff，确认写集没有别的并行 Java writer；若任一目标文件在领取后出现未知新改动，
   停下写日志报告，不覆盖。

### 交付

- 在本日志追加 Implementation #1：精确改动、screen/client 两路径时序、普通 digest零变化依据、构建结果与 scoped status。
- Worker 自审不算 Approved。完成后通知父级源码复审；父级通过前不接 caller、不启动运行面。

**无已批准业务差异；按基线等价迁移。**

## Replacement Internal Worker V2 - CLAIMED - `W-CLIENTPX-IMP1-R1` - 2026-07-13T11:02:11.3526216-04:00

- 已完整读取 `Parent Implementation Review #1`。本返修只修改
  `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` 与本 append-only 日志；Cloud、codec、mapper、
  schema、queue/worker、digest、业务与运行面均冻结。
- 目标：为 `WINDOW_CLIENT_PX` 的 callWith-side conversion 保存 exact `x/y/width/height` snapshot，并仅把该 snapshot 接入
  worker-admission 与现有 detailed-safety supplier；`SCREEN_ABSOLUTE_PX` 保持原 supplier 语义。

## Replacement Internal Worker V2 - Repair #1 - `W-CLIENTPX-IMP1-R1` - 2026-07-13T11:05:23.8587359-04:00

- P1 修复仅落在 `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`：`WINDOW_CLIENT_PX`
  在 `callWith` 内经过 current registration/binding/runRevision fence 后，将换算所用的 exact `x/y/width/height` 固化为
  `ClientInputGeometry`，同一快照既用于 `Math.addExact` 的 screen-absolute action 构造，也闭合到两个现有 queue gate。
- worker admission：`workerAdmissionRevisionFence(command, runner, inputGeometry)` 先保留既有 runRevision gate；仅当
  `inputGeometry` 非 null 时，再比较 runner 当前 binding 的 `x/y/width/height` 与有效 geometry。改变、binding 缺失或任一
  geometry 不可用返回 `WINDOW_BINDING_CHANGED`，由现有 worker admission 矩阵在首 step 前形成
  `NOT_EXECUTED/WINDOW_BINDING_CHANGED`。
- mid-bundle：外部 detailed-safety supplier 仍由现有 `InputActionQueue`/`InputActionRequest` 在入队前、worker 边界、每个
  `tryStartStep` 与可中断 sleep segment 调用；`remoteInputSafetyReason(..., inputGeometry)` 只在 client snapshot 非 null 时
  增加同一 exact geometry 比较。已经开始的 bundle 因此沿现有矩阵得到 `UNKNOWN/WINDOW_BINDING_CHANGED`；stop 的既有
  `STOPPED` 分类未改。没有重新换算、clamp、retry、thread 或 queue/worker 修改。
- `SCREEN_ABSOLUTE_PX` 明确传入 null snapshot，仍只执行原 registration/window/stop/revision safety 判定，不会因 geometry
  拒绝；其 payload、canonical digest、queue action bytes 和 ordinary input sequence 未改。
- 构建证据：Cloud `D:\mavenProject\dhxy-cloud-brain` 已运行 `mvn -q clean package`，exit `0`（101.2s）；DHXY 根目录已运行
  `mvn -q -DskipTests compile`，exit `0`（28.2s）。遵守 no-local-test，未新增、恢复或运行测试，未启动任何运行面。
- scoped status：本返修后唯一改动文件仍为本日志与既有 untracked 的
  `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`；未提交。Worker QA 不算 Approved，交父级再审。

## Internal Worker V - CLAIMED - `W-CLIENTPX-IMP1` - 2026-07-13T07:29:11-04:00

- 已完整读取 Parent Task Brief #1、`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部、
  `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` 与两仓最新 `git status`。
- 唯一写集：Cloud `InputBundleRequest.java`；DHXY `RemoteOperationPayloadCodec.java`、
  `LocalRemoteGameCommandHandler.java`，仅确有必要时 `RemoteInputActionMapper.java`；协议 schema 与本固定日志。
- scoped status：Cloud 目标 Java 为既有 untracked；DHXY 三个 remote Java 为既有 untracked，schema 为既有 modified。
  已记录领取时文件哈希与 scoped diff；不覆盖领取后出现的未知写入，不触碰其它 Java/DTO/enums/digest/Maven/
  resources/tests/business/assembly/host/caller 或其它 Worker 日志。
- 本轮只实现 typed `WINDOW_CLIENT_PX` input bundle 本地换算与执行门；不启动运行面，不提交 Git。Worker QA 不算
  Approved，完成后交父级独立审查。
## Replacement Internal Worker V2 - CLAIMED - 2026-07-13T10:52:54.1130526-04:00

- Work item: `W-CLIENTPX-IMP1`
- claimedAt: `2026-07-13T10:52:54.1130526-04:00`
- 接管说明：原 V 会话在桌面重启后 not_found；本次仅接续其唯一写集内已存在的可辨识改动，绝不回滚、覆盖或触碰其它 dirty/untracked 文件。
- 唯一写集：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\InputBundleRequest.java`、`src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`、`src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`、仅确有必要时 `src/main/java/com/bot/dhxy/cloud/remote/RemoteInputActionMapper.java`、`docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`、本日志。

## Replacement Internal Worker V2 - Implementation #1 - 2026-07-13T10:58:44.4624760-04:00

- 精确改动：Cloud `InputBundleRequest.java` 与 DHXY `RemoteOperationPayloadCodec.java` 放行既有 closed enum 的
  `SCREEN_ABSOLUTE_PX | WINDOW_CLIENT_PX`，未改 enum、protocol version、payload field order 或 digest 实现。
  `LocalRemoteGameCommandHandler.java` 为 `WINDOW_CLIENT_PX` 增加受限转换：仅 pointer action 的 click/right/double/move
  与 drag start/end 使用 `Math.addExact(binding.x/y, relative)`；key/text/scroll/sleep 等无坐标 action 字段原样复制。
  `RemoteInputActionMapper.java` 已检查、未修改。
- 双路径时序：`SCREEN_ABSOLUTE_PX` 保持原 handler 顺序：外层 binding inside-window 校验 -> mapper -> `callWith` 内再次
  校验 -> 一次 `submitRemoteAndWaitDetailed`。`WINDOW_CLIENT_PX` 只在 `callWith` 内完成 current
  registration/binding/runRevision 二次 fence 后转换和 absolute inside-window 校验，随后一次 mapper 和同一 queue
  submission；现有 worker-admission revision fence 与 mid-bundle safety supplier 未动。overflow 返回
  `NOT_EXECUTED/INVALID_REQUEST`，geometry 缺失返回既有 `NOT_EXECUTED/WRONG_WINDOW`，outside 返回既有
  `NOT_EXECUTED/INVALID_REQUEST`，均发生在任何 input step 前。
- 普通 digest 零变化依据：`coordinateSpace` 原本已经属于 typed request/payload 并进入 canonical request digest；本次没有
  改动 Cloud/DHXY digest canonicalizer、字段顺序、`SCREEN_ABSOLUTE_PX` payload 值或该路径的 mapped action 值。
- 协议同步：`docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` 的 input request 更新为双值 enum，并记录
  `callWith` 内 fence、exact arithmetic、inside-window 与无坐标 action 不变的规则。该 schema 在接管前已是其它并行工作
  的大范围 modified 文件；本次仅接续其 input section 的允许改动，未回滚或覆盖其它内容。
- 构建：`D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q clean package`，exit `0`（84.4s）；DHXY 根目录执行
  `mvn -q -DskipTests compile`，exit `0`（27.1s）。遵守 no-local-test，未新增、恢复或运行 DHXY 测试，也未启动任何
  application/server/Task/UI/capture/input。
- scoped status：Cloud `InputBundleRequest.java` 为既有/仍为 untracked；DHXY
  `LocalRemoteGameCommandHandler.java`、`RemoteOperationPayloadCodec.java`、`RemoteInputActionMapper.java` 为既有/仍为
  untracked，schema 为既有/仍为 modified，本日志为 untracked。未提交；未写入唯一写集以外文件。Worker QA 不是 Approved。

## Parent Implementation Review #1 - BLOCKED - 2026-07-13T11:01:00-04:00

父级逐行复核 `LocalRemoteGameCommandHandler.executeInputBundle`、`InputActionQueue.submitRemoteAndWaitDetailed`、
`InputActionRequest.admitWorkerStart/checkDetailedSafety/tryStartStep` 与 `InputActionWorker`。Cloud/codec 双值枚举、client 坐标
exact arithmetic、单 bundle 入队和普通 `SCREEN_ABSOLUTE_PX` digest 零修改均成立；整体仍 **BLOCKED，P0=0/P1=1/P2=0**。

1. **P1：client-relative 换算后的窗口 geometry 没有延续到 worker admission 与逐 step safety。** 当前在
   `LocalRemoteGameCommandHandler.java:523-529` 用 `currentAccess.binding().x/y` 铸出 screen-absolute actions，随后传入的
   `remoteInputSafetyReason(command, runner)` / `workerAdmissionRevisionFence(command)` 只校验 scope、taskRun、revision、
   stop、windowId、HWND、processId、playerIdentityEpoch；`classifyRemoteRun` 在 `:1050-1071` 不比较
   `x/y/width/height`。因此同一 HWND 在队列等待、pause wait、focus 前或 bundle 中途移动/缩放时，已换算的旧绝对坐标仍可
   被执行，违反 Parent Brief #1 的“转换后由 worker-admission fence 和 mid-bundle safety 控制窗口漂移”，并可能把输入发到
   错误屏幕位置。

### 返修条件 `W-CLIENTPX-IMP1-R1`

- 只修改 DHXY `LocalRemoteGameCommandHandler.java` 与本 append-only 日志；Cloud/codec/mapper/schema、input queue/worker、
  digest、业务 Service/Task/host/caller 全冻结。
- `WINDOW_CLIENT_PX` 在 `callWith` 内换算时保存 exact geometry snapshot：`x/y/width/height`；其 worker-admission supplier
  必须在首物理 step 前比较当前 exact binding geometry，外部 detailed-safety supplier 必须在现有每-step 检查中重复比较。
  任一字段变化或 geometry 不可用均返回 `WINDOW_BINDING_CHANGED`。不得 title-search、不得重新换算、不得 clamp。
- 首 step 尚未开始时沿现有矩阵得到 `NOT_EXECUTED/WINDOW_BINDING_CHANGED`；已开始后得到 `UNKNOWN/WINDOW_BINDING_CHANGED`
  （stop 仍按既有 STOPPED）。`SCREEN_ABSOLUTE_PX` 必须继续使用原 supplier 语义，不能因本修复新增 geometry 拒绝。
- 不拆 bundle、不改 queue、不新增 retry/TTL/thread/test。完成后追加 Repair #1、精确时序与双构建证据；Worker QA 不算批准。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #2 - APPROVED - 2026-07-13T11:09:00-04:00

父级对 Repair #1 与当前双仓源码做 fresh 复审，结论 **APPROVED，P0=0/P1=0/P2=0**。

- Cloud `InputBundleRequest` 与 DHXY strict codec 只放行既有 enum 的
  `SCREEN_ABSOLUTE_PX | WINDOW_CLIENT_PX`；未改 protocol version、canonical field order 或 digest 实现。
- `LocalRemoteGameCommandHandler.java:513-545` 只在 exact `callWith` registration/binding/revision fence 后，将
  `WINDOW_CLIENT_PX` 以 `Math.addExact` 转为 screen pixels，并保持单次 `submitRemoteAndWaitDetailed`；无坐标 action 原值保留，
  越界/overflow 在首物理 step 前 fail-closed。
- `ClientInputGeometry` 固化换算时 `x/y/width/height`；`:1015-1045` 分别把它接入 one-shot worker admission 与现有
  detailed-safety supplier。queue refresh、pause wait、首 step 与逐 step 发现 geometry 漂移时返回
  `WINDOW_BINDING_CHANGED`；现有 startedStepIndex 矩阵保证首步前 `NOT_EXECUTED`、开始后 `UNKNOWN`，stop 仍优先为
  `STOPPED`。`SCREEN_ABSOLUTE_PX` 传 null snapshot，未新增 geometry gate，原执行语义保持。
- 未改 `InputActionQueue`/`InputActionWorker`/mapper、未拆 bundle、未新增 retry/TTL/thread/test，未启动运行面。
- 父级 fresh 验证：Cloud `mvn -q clean package` exit 0，Surefire `4 suites / 21 tests / failures=0 / errors=0 /
  skipped=0`，shaded JAR SHA256 `700C236B4277890224949EA5FFFAAFB83B5F6070165EFF87CBC558F789421361`；DHXY
  `mvn -q -DskipTests compile` exit 0。

本切片收口；运行面继续 dormant，等待后续已批准 caller 切片接入。

**无已批准业务差异；按基线等价迁移。**
