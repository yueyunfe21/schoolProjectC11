# TURN-10D QuestManagerService Closed Adapter

## CLAIMED

- 领取时间：`2026-07-15T15:54:35-04:00`。
- 角色：CR271 Internal implementation Worker；父级仍是唯一 manager/final reviewer。
- 卡片：`TURN-10D`；`countDelta=0`。
- 唯一 Java 写集：`src/main/java/com/bot/dhxy/cloud/turn/local/QuestLocalOperationExecutor.java`。
- 唯一报告：`docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-10D.md`。
- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威 HTTPS turn
  卡计划、协议规格、两仓 `git status`、当前及 `696a12b0` 基线 `QuestManagerService`、
  `QuestDetailCapture`、`LocalServiceExecution`、`TurnPngCodec`、`TurnFrame` 与冻结 Quest DTO/validator。
- 保护领取前全部 dirty/untracked；未回滚、覆盖、清理、提交，未启动 Maven/tests/runtime/application/server/
  Task/poller/UI/capture/input，也未执行其它 Git mutation。

## BLOCKED — Quest detail capture does not expose truthful frame region

- 阻断时间：`2026-07-15T15:54:35-04:00`；状态：`BLOCKED / PARENT DECISION REQUIRED`。
- `P0=0 / P1=1 / P2=0`；本 Worker 未创建 Java、未自批、未领取下一卡。
- 精确证据：`QuestManagerService.java:213-227` 在 private
  `captureCurrentQuestDetailForTaskDirect(...)` 内根据 anchor 计算屏幕绝对 `rightRect` 并只截图一次，但 public
  `captureCurrentQuestDetailForTask(...)` 只返回 `QuestDetailCapture`；`QuestDetailCapture.java:21-23` 仅承载
  `BufferedImage image` 与 debug `imagePath`，没有真实 `TurnRegion`/左上角绝对坐标。
- 协议影响：`TurnPngCodec.java:34-60` 创建 `TurnFrame` 时必须接收 `TurnRegion`，并把该 region 写入
  `TurnFrameMetadata`；`LocalServiceExecution.java:90-107` 又要求 Quest frame 的 dimensions 与 region 一致。
  adapter 若填 `(0,0,width,height)` 会把窗口相对假坐标冒充用户已冻结的屏幕绝对 ROI；若复制
  `DETAIL_TEXT_*` 常量、重新找 anchor 或自行 capture，则会复制 Quest 业务、产生第二条截图路径并扩大唯一写集。
- source-step 影响：`TurnLocalServiceCall`/`TurnQuestOperationArguments` 不携带 `sourceStepIndex`。后续 dispatcher
  可以把当前 step index 作为 adapter 参数传入，但在本卡尚未冻结该调用签名前，不能用 `null` 猜测一个步骤产生的
  `QUEST_DETAIL` frame。即使父级允许新增 execute 参数，真实 region 仍然缺失，因此当前阻断成立。
- activate 核对：当前 public `QuestManagerService.activateTaskIfPresent(task, keepOpen)` 能保留冻结参数，但它使用多个
  `submitAndWait` 片段；`696a12b0` 基线另有 `activateTaskIfPresentExclusive(task, keepOpen)`，当前工作区没有该 public
  API。本卡禁止修改 `QuestManagerService`，因此也没有把 adapter 放进外层 exclusive callback 造成 queue-in-queue，
  更没有反射调用 private direct 方法。
- 返修/解阻条件：由父级单独安排拥有 `QuestManagerService`/`QuestDetailCapture` 的前置卡，让现有一次 capture 的结果
  同时返回真实屏幕绝对 region（并明确 dispatcher 传入的 `sourceStepIndex` 调用签名）；或由用户明确批准其它不伪造
  坐标且不产生第二张图的 typed 合同。依赖可读后再原写集重派 `TURN-10D`。
- 基线结论：`无已批准业务差异；按基线等价迁移`。当前选择阻断，避免把缺失元数据变成未批准业务差异。

`BLOCKED`

## PARENT REVIEW CONFIRMED / REPAIR PREREQUISITE #1 CLAIMED

- 父级复审时间：`2026-07-15T16:04:00-04:00`；独立读取
  `QuestManagerService.java:213-227`、`QuestDetailCapture.java:17-35` 与 `TurnPngCodec` 后确认：
  `P0=0 / P1=1 / P2=0`，阻断成立。
- 精确影响：现有一次 capture 已计算真实 absolute `rightRect[0]/[1]`，但返回对象只携带 image/path；若 adapter
  写 `(0,0)`、复用窗口原点或再抓一张图，都会破坏冻结的绝对坐标/单帧合同。
- 原 Worker 已领取 `TURN-10D Repair Prerequisite #1`，唯一 Java 写集临时改为
  `model/quest/QuestDetailCapture.java` 与 `service/QuestManagerService.java`：只给成功 capture 同步返回
  absolute-screen `screenX/screenY`，保持原 activation、anchor、ROI、单次 capture、debug save 与 close 顺序；
  禁止导入 turn/protocol 类型、第二次截图或业务变化。
- prerequisite 经父级源码通过前不得创建 `QuestLocalOperationExecutor`。通过后原 `TURN-10D` 仍用原 adapter
  唯一写集恢复，调用签名显式接收 dispatcher 的 `sourceStepIndex`，不得猜测 frame 来源步骤。
- 当前状态：`BLOCKED / REPAIR PREREQUISITE #1 CLAIMED`；`countDelta=0`，hard ledger `189/407`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## ORIGINAL TURN-10D PARENT SOURCE APPROVED / BUILD COHORT PENDING

- 父级终审时间：`2026-07-15T16:09:00-04:00`；结论：`P0=0 / P1=0 / P2=0`，
  `SOURCE APPROVED / BUILD COHORT PENDING`，owner 释放。
- 父级独立读取 `QuestLocalOperationExecutor.java`，当前 SHA-256
  `2D0033887FD8599B668569C290853541965ED1D706C424A770FC08B68034B05E` 与交付一致。
- activate/capture 两个 closed case 的 argument gate 与冻结 validator 一致；负 `sourceStepIndex` 在任何 Service
  调用前拒绝。adapter 从 input worker 外调用现有 public Quest API，不建立外层 queue。
- capture 只消费一次 Service 返回，失败无 frame；成功使用同一 `BufferedImage`、truthful absolute
  `screenX/screenY`、image dimensions 与传入 step index 编码唯一 `QUEST_DETAIL` frame，并在 `finally` flush。
- 无第二次截图、伪坐标、OCR/match/click/retry/reflection/第五 Service。当前后续 Java writer 将继续滚动，父级
  暂不运行 Maven；hard ledger `189/407`，本卡 `countDelta=0`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Review — BLOCKED Confirmed

- 父级确认原阻断：`BLOCKED / P1=1`。
- 父级续派 `TURN-10D Repair Prerequisite #1`，仅允许修改
  `QuestDetailCapture.java`、`QuestManagerService.java` 与本报告；原 adapter 继续禁止创建，直到父级独立批准本前置修复。

## Repair Prerequisite #1 — SOURCE DELIVERED

- 交付时间：`2026-07-15T15:57:48-04:00`；状态：`SOURCE DELIVERED / PARENT REVIEW PENDING`；
  `countDelta=0`。
- `QuestDetailCapture.java:13-27` 仅新增 primitive `screenX`/`screenY`，JavaDoc 明确它们是屏幕绝对像素，且仅在
  `hasImage()` 为 true 时有效；没有 import 任一 `cloud/turn/protocol` 类型。
- `QuestDetailCapture.java:33` 将失败结果固定为 `new QuestDetailCapture(null, "", 0, 0)`；成功结果的
  width/height 仍唯一来自既有 `BufferedImage`，没有新增或缓存伪尺寸。
- `QuestManagerService.java:213-229` 的 activation、anchor、`rightRect` ROI 计算、唯一一次
  `captureToMemory(...)`、debug save、成功返回、`finally closePanelDirect()` 顺序保持原样；唯一语句差异为
  `:227` 将已经用于截图的真实屏幕绝对 `rightRect[0]`/`rightRect[1]` 随同原 image/path 返回。
- 静态差异核对：两份 Java 的 `git diff` 只有上述字段/构造参数变更；没有第二个 capture、Cloud DTO、业务 fallback、
  retry、反射、queue/input 变更或 adapter 文件。
- 按父级并行 writer 门，本 Worker 未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行
  Git mutation；保护全部其它 dirty/untracked。等待父级独立源码审查，不自批、不恢复原 adapter、不领取下一卡。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## Repair Prerequisite #1 — PARENT SOURCE APPROVED

- 父级独立结论：`P0=0 / P1=0 / P2=0`；原 `TURN-10D` adapter 写集恢复。
- 父级核验 SHA-256：`QuestDetailCapture.java`
  `E174768ECC0B04CB1B92C63CBD16A697A00F7239B8F6AD6E194B307189D6253F`；
  `QuestManagerService.java`
  `7AA92ACC824B72443819EC6F7D1D019E49F7774CC9BA4271591CD5693765291A`。
- 批准范围仅为 truthful `screenX/screenY` 与既有 `rightRect` origin；单次 capture 和原顺序不变。

## Original TURN-10D Resumed — SOURCE DELIVERED

- 状态：`SOURCE DELIVERED / PARENT REVIEW PENDING`；`countDelta=0`。本 Worker 已停止，不自批、不领下一卡。
- 唯一 Java 交付：`src/main/java/com/bot/dhxy/cloud/turn/local/QuestLocalOperationExecutor.java`；SHA-256
  `2D0033887FD8599B668569C290853541965ED1D706C424A770FC08B68034B05E`。
- `:18-31` 为 Spring `@Component`，只构造注入 `QuestManagerService`、`TurnPngCodec`、`ObjectMapper`。
- `:41-53` 公开 `execute(call, sourceStepIndex)`；closed switch 只允许 `QUEST_ACTIVATE` 与
  `QUEST_CAPTURE_DETAIL`，其它 operation 一律 `UNSUPPORTED_LOCAL_OPERATION`。两个合法 case 均在任何 Service
  调用前拒绝负 `sourceStepIndex`。
- `:56-67` 精确要求 only quest args、nonblank task、nonnull keepOpen；从 input worker 外只调用一次现有 public
  `QuestManagerService.activateTaskIfPresent(task, keepOpen)`，返回 completed `OK` 与最小 typed JSON
  `activated`，不返回 frame。
- `:69-93` 精确要求 only quest args、nonblank task、null keepOpen；只调用一次现有 public
  `captureCurrentQuestDetailForTask(task)`。无 image 时返回 FAILED `QUEST_DETAIL_CAPTURE_FAILED` 且无 frame；
  成功时用同一个 image、truthful `capture.screenX/screenY`、image width/height 和传入 step index 调用一次
  `TurnPngCodec.encode(..., QUEST_DETAIL, ...)`，返回 completed `OK` 与唯一 frame。
- `:77-92` 对 Service 返回的唯一 `BufferedImage` 用 `try/finally` 管理；无论编码或结果构造成功/异常，均在
  `finally` 中 flush；没有第二张图或 image 泄漏。
- 静态核对：本 adapter 无 `InputSequences`/outer queue、`InputProvider`、capture、OCR、match、click、retry、
  reflection 或第五 Service；`git diff --check` exit `0`。
- 按父级并行 writer 门，未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git
  mutation；未再次修改已批准的 `QuestDetailCapture`/`QuestManagerService`，保护其它全部 dirty/untracked。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## REPAIR PREREQUISITE #1 PARENT SOURCE APPROVED / TURN-10D RESUMED

- 父级复审时间：`2026-07-15T16:06:00-04:00`；结论：`P0=0 / P1=0 / P2=0`，前置源码通过。
- 父级独立核对 `QuestDetailCapture.java` SHA-256
  `E174768ECC0B04CB1B92C63CBD16A697A00F7239B8F6AD6E194B307189D6253F` 与
  `QuestManagerService.java` SHA-256
  `7AA92ACC824B72443819EC6F7D1D019E49F7774CC9BA4271591CD5693765291A`；diff 只增加 absolute-screen
  `screenX/screenY` 及既有 `rightRect[0]/[1]` 成功返回，capture 数量和业务顺序不变。
- 原 `TURN-10D` 已恢复 `CLAIMED`，唯一 Java 写集回到
  `cloud/turn/local/QuestLocalOperationExecutor.java`；adapter 必须从 queue 外调用 public Quest Service，显式接收
  nonnegative `sourceStepIndex`，并用同一 image + `screenX/screenY` 生成唯一 `QUEST_DETAIL` frame。
- 当前仍有 Java writer，未运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input；
  hard ledger `189/407`，本卡 `countDelta=0`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## TURN-10D Worker Final Delivery

- 状态：`SOURCE DELIVERED / PARENT REVIEW PENDING`；adapter SHA-256
  `2D0033887FD8599B668569C290853541965ED1D706C424A770FC08B68034B05E`。
- 交付范围与上方 `Original TURN-10D Resumed — SOURCE DELIVERED` 证据一致；未修改前置修复文件，未运行
  Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。
- Worker 到此停止，不自批、不领取下一卡。

`SOURCE DELIVERED / PARENT REVIEW PENDING`
