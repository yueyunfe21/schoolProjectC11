# Cloud CommonBoxService lift-and-shift - External Worker A

## Parent Task Brief #1 - `W-CBOX-D1` - 2026-07-13T05:25:00-04:00

### 目标

以 DHXY HEAD `0114604e1ff5f15491d2910959c45252e893d04f` 的
`src/main/java/com/bot/dhxy/service/CommonBoxService.java` 为唯一业务基线，为整类 Cloud lift-and-shift 形成
implementation-ready Design #1。Cloud 持有 template 判定、pending/TTL/role/taskRun 状态与 consume 编排；DHXY 只保留 exact bound-window
ROI capture、输入执行、window/identity/taskRun 副作用前安全拒绝与 local artifact。无已批准业务差异。

### 领取门

External Worker A 必须在 `2026-07-13T05:45:00-04:00` 前在本日志追加真实：

```text
## External Worker A - CLAIMED - <timestamp>
- task: W-CBOX-D1
- claimedAt: <timestamp>
- uniqueWriteSet: only this append-only report
```

20 分钟只检查领取，不检查完成；领取后可工作超过 20 分钟。截止仍无 CLAIMED 才由父级内部接管。

### Design #1 必须闭合

1. 完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、迁移矩阵与本日志；确认源相对 HEAD 无 diff，列
   461 行类的 constructor/public/private/nested API、四个 public pending/detect/consume/clear 入口及全部 caller 行号。
2. 方法级逐分支冻结：role toggle、supported task、context role、window/taskRun/identity gates、`PENDING_TTL_MS=30000`、检测异步
   boundary、capture/template miss、pending replace/prune/keep-on-click-failure、consume true/false 与 clear-by-role；不得新增/删除 TTL、
   retry、verification、fallback 或 cleanup。
3. Cloud 是 `pendingByKey` 唯一 owner；key 必须包含 tenant scope、exact window tuple、taskRun、role/task，TTL 只用 monotonic deadline
   并保留 30 秒行为。DHXY 不留 pending map、不做 role/task/match 决策，只回传 typed capture/input outcome。
4. 保留 ROI `623,472,822,618`、template `leader_box_marker.png`、threshold `0.86`、click settle `80ms`、delay `120ms`；同一
   OBSERVED frame 在 Cloud classpath template 上匹配，match 点用 capture-time origin/scale 转 `WINDOW_CLIENT_PX`，禁止 raw path/HWND 上云。
5. 只经 retained `CloudTaskServicePort` 的现有三操作 `WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE`；给 detect occurrence、pending identity、
   consume click 的 stable address 与 UNKNOWN/NOT_EXECUTED/STOPPED/revision 处理。不得新增 `COMMON_BOX` raw operation、ThreadLocal/current-run、
   background executor 或自动 retry；HEAD async detect 的非阻塞调用语义由 Cloud task scheduling/retained workflow表达。
6. `AutoCombatService/AutoBattleTask/WubeiTask/XiuluoTaskV2/MainWindowController` caller/host 全冻结；给出可编译 DAG、最小独立叶子波、
   精确 New/Modify 文件表和双构建门。主体保持 dormant，不新增/恢复 tests，不启动运行面。

### 写集与并行边界

本轮唯一写集为本日志 append-only；DHXY/Cloud Java、Maven、schema、resources、tests 全冻结。不得修改 QuestManager、B LeftTop、
P2 Full R0、S Bag、warning 日志或 host/caller。Worker 自审只算 QA，不构成父级批准；交付后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T05:26:50-04:00
- task: W-CBOX-D1
- claimedAt: 2026-07-13T05:26:50-04:00
- uniqueWriteSet: only this append-only report

## External Worker A - Design #1 (implementation-ready) - 2026-07-13

以 DHXY HEAD `0114604e` `com.bot.dhxy.service.CommonBoxService.java`（461 行）为唯一业务基线的整类 Cloud lift-and-shift 设计。仅设计，DHXY/Cloud Java/Maven/schema/resources/tests 全冻结，不改 QuestManager/B LeftTop/P2 Full R0/S Bag/warning/host/caller，不启动运行面，不 git。**源相对 HEAD 无 diff**（已核）。已读 AGENTS/迁移矩阵/本日志基线约束。全部引用真实符号；Cloud 侧未落盘类型标 New/精确缺失 leaf，不虚构。

### 1. 完整 inventory（精确 FQCN/行号）

- **@Service `@RequiredArgsConstructor`**（L35-55）依赖：`BotProperties botProperties`、`GameClientTracker tracker`、`InputSequences inputSequences`、`WindowTaskContextHolder windowTaskContextHolder`。
- **常量**（L39-50）：ROI `ROI_LEFT=623/ROI_TOP=472/ROI_RIGHT=822/ROI_BOTTOM=618`；`PENDING_TTL_MS=30_000`；`TEMPLATE_PATH="images/template/common/leader_box_marker.png"`；`TEMPLATE_THRESHOLD=0.86`；`TASK_XIULUO="xiuluo_v2"`/`TASK_WUBEI="wubei"`；`CLICK_SETTLE_MS=80`/`CLICK_DELAY_MS=120`。
- **state**：`Map<String,PendingCommonBox> pendingByKey = ConcurrentHashMap`(L57)；`volatile BufferedImage cachedTemplate`(L58)。
- **public API（5）**：`detectLeaderBoxAfterReturnHome(ctx,sourceTask,source)`(L67→detectBox LEADER)、`detectMemberBoxAfterCombatExit`(L78→detectBox MEMBER)、`consumePendingBoxIfAllowed(ctx,sourceTask,source):boolean`(L90)、`hasPendingBoxForCurrentWindow(ctx,sourceTask):boolean`(L171)、`clearPendingForRole(role,source)`(L212)。
- **private**：`detectBox`(L230，含 async boundary)、`detectAndRecord`(L284)、`cachedTemplate`(L352)、`roleFor`(L371)、`isRoleEnabled`(L385)、`normalizeSupportedTask`(L392)、`pendingKey`(L403)、`taskRunKey`(L411)、`pruneExpiredPending`(L422)、`sameWindow`(L438)。
- **nested**：`record PendingCommonBox(windowId,nativeWindowHandle,sourceTask,taskRunKey,role,detectedAtMs,expiresAtMs,templateX,templateY,clickX,clickY,identityEpoch,source)`(L447-460)。
- **唯一生产 caller（行号）**：detectLeader→`WubeiTask:4638`、`XiuluoTaskV2:4771`；detectMember→`AutoCombatService:637`；consume→`AutoCombatService:836`、`AutoBattleTask:303`、`WubeiTask:3389`、`XiuluoTaskV2:4839`；hasPending→`AutoCombatService:826`；clearPendingForRole→`MainWindowController:609/612`（UI toggle off）。全部 caller/host **冻结**（W-CBOX-2 cohort，非-A）。

### 2. 方法级逐分支冻结（逐字保 HEAD）

- **consume(L90-159)**：invalid taskRun(taskRunKey null)→skip false；no window-runtime→skip false；role 未知(roleFor empty)→skip false；role toggle off(!isRoleEnabled)→`clearPendingForRole(role)`+false；pending null→false；`expired||staleWindow||staleIdentity||staleTaskRun`→remove+false；否则 `inputSequences.moveAndClickLeft(clickX,clickY,80,120)`→clicked 则 remove+`COMMON_BOX_CLICKED` log+true；**click 失败→保留 pending 至 TTL**+warn+false。
- **hasPending(L171-203)**：同 consume 的 taskRun/window/role/toggle gates（无 input）；pending 存在且 `expiresAt>now && sameWindow && identityEpoch== && taskRunKey==`→true。
- **detectBox(L230-282)**：`pruneExpiredPending`；`normalizeSupportedTask`(仅 xiuluo_v2/wubei)null 或 runtime empty→return；taskRun null→skip；toggle off→`clearPendingForRole`+skip；roleFor empty→remove pendingKey+skip；`actualRole!=role`→remove+skip；**async boundary**：`CompletableFuture.runAsync(runWith(window, detectAndRecord))`（CR235 恢复的非阻塞 detect）。
- **detectAndRecord(L284-350)**：`context.isStopRequested()`→return；binding null/!hasGeometry→skip；ROI=binding.x/y+(623,472)-(822,618)；`tracker.captureToMemory`→null 则 capture-miss return；`cachedTemplate`→null 则 template-unavailable return；`ImageFinder.find(raw,template,0.86)`→null 则 match-miss return；命中→`templateX/Y=x1/y1+round(match[0/1])`；`PendingCommonBox(now, now+30000, templateX,templateY,templateX,templateY(clickX=clickY=templateX/Y), identityEpoch, …)`→`pendingByKey.put`。
- **不新增/删除 TTL/retry/verification/fallback/cleanup**；prune/keep-on-click-failure/clear-by-role/consume true/false 逐字。

### 3. Cloud 是 pendingByKey 唯一 owner

- **owner = Cloud**（新 `CommonBoxPendingStore`，per-run retained，随既有 authority）。**key** 逐字含并强化：`(RemoteTaskRunScope tenant/user/device/clientSession, exact window tuple[windowId,nativeHandle,processId,playerIdentityEpoch], taskRunId, role, taskKey)`——比 HEAD `windowId|hwnd|role|taskKey|taskRunKey` 增补 tenant scope + processId/identityEpoch，杜绝跨租户/漂移误命中。
- **TTL**：`monotonic deadline`（`now_mono + 30_000ms`，保留 30 秒行为）；HEAD wall-clock `expiresAtMs` 迁为可注入 monotonic（父级明令 monotonic + 保留 30s）。prune/expired 判定同 HEAD 逐分支。
- **DHXY 侧零 pending map、零 role/task/match 决策**，只回传 typed capture/input outcome；staleWindow/staleIdentity/staleTaskRun 全在 Cloud 判。

### 4. ROI/template/坐标（同一 OBSERVED frame）

- 保留 ROI `623,472,822,618`、template `leader_box_marker.png`、threshold `0.86`、click `settle 80ms/delay 120ms`。
- **同一 OBSERVED frame** 在 Cloud classpath template 上匹配：`CAPTURE(WINDOW_CLIENT_PX ROI)`→OBSERVED `imageBytes`→ImageIO→`CloudTemplateAssets.loadTemplate(leader_box_marker)`→`ImageFinder.find(roi,template,0.86)`→image-local match 点。
- **match 点→WINDOW_CLIENT_PX**：`clickClient = ( ROI.x + round(local/scale), ROI.y + round(local/scale) )`，用 **capture-time origin + `systemScaleRatio`**（A 已批准 capture-time scale，同 QM R1 公式）；client→screen 由 DHXY input 时以 bound window 唯一解析。**禁 raw path/HWND 上云**（Cloud 只经 opaque handle + observedWindow 四字段 correlation）。Cloud classpath 已有 `leader_box_marker.png`（与 DHXY 一致），**资源写集=零**。

### 5. retained CloudTaskServicePort（现有三操作）+ stable address + typed unwind

- 只经 retained `CloudTaskServicePort` 的 `WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE`；**不新增 `COMMON_BOX` raw operation、ThreadLocal/current-run、background executor、auto retry**。
- **stable address**（canonical phaseCode=`common-box`）：`detect-geometry`(WINDOW_FACT，如需 origin/scale)、`detect-capture-{role}`(CAPTURE，detect occurrence)、`consume-click-{role}`(EXECUTE_INPUT_BUNDLE，consume click)。**detect occurrence / pending identity / consume click** 各稳定 address（occurrence 承接 Full R0 frontier，同 QM/TMS，硬前置 M Full R0）。
- **typed unwind**：`UNKNOWN/NOT_EXECUTED/STOPPED` 不压成 detect-miss / no-pending：detect 的 `CAPTURE` UNKNOWN→typed unresolved（不建 pending、不重拍）；consume 的 `EXECUTE_INPUT_BUNDLE` UNKNOWN→**keep pending 至 TTL**（对齐 HEAD click-failure keep），NOT_EXECUTED（副作用前 fence 拒）→keep pending，STOPPED→typed stop unwind；OBSERVED-miss(模板未命中)=HEAD detect-miss(不建 pending)。旧 revision request 经三门反复活合同拒绝、不复活。
- **HEAD async detect 语义**（`CompletableFuture.runAsync` 非阻塞）由 **Cloud task scheduling / retained workflow** 表达：detect 的 role/window/taskRun 廉价校验同步，capture+match 作为 retained workflow 的非阻塞 step 排程离开 post-return/post-combat 关键路径，**不新增 Cloud 线程/background executor**。

### 6. 依赖 DAG + 最小叶子波 + 文件表 + 双构建门

**DAG**（→=前置）：
```
既有 CloudTaskServicePort/retained 权威 + CloudTemplateAssets(+leader_box_marker 已打包) + A capture-time systemScaleRatio(APPROVED) + Cloud CommonBoxRole(既有 model/maintenance)
  → [叶子 W-CBOX-0] Cloud CommonBoxPendingStore（per-run retained pending owner，monotonic TTL；可独立编译，无 host/caller）
  → [W-CBOX-1] Cloud CommonBoxService（整类编排：detect/consume/has/clear，经 retained handle + CloudTemplateAssets + monotonic TTL；typed unwind）
      ├ 硬前置 M Full R0（occurrence 源）
      └ 硬前置 activation/scheduling（async detect 非阻塞 step）
  → [W-CBOX-2 caller cohort，非-A] AutoCombatService/AutoBattleTask/WubeiTask/XiuluoTaskV2/MainWindowController caller 接线
禁改：QuestManager、B LeftTop、P2 Full R0、S Bag、warning、host/caller。
```

**最小可独立编译叶子 W-CBOX-0**：Cloud New `com.bot.dhxy.service.commonbox.CommonBoxPendingStore`（per-run pending map owner + PendingCommonBox 等价 record + monotonic TTL；纯 CPU/state，无 host/caller/port 依赖，可独立编译）。

**New/Modify 文件表**：

| 仓库 | 精确 FQCN | New/Modify | 门 / 归属 |
|---|---|---|---|
| Cloud | `service.commonbox.CommonBoxPendingStore` (+ pending record) | New（W-CBOX-0） | 本设计批准；Worker A |
| Cloud | `service.CommonBoxService`（整类编排 dormant） | New（W-CBOX-1） | 门=W-CBOX-0 + M Full R0 + scheduling/activation + 父级授权；Worker A |
| Cloud | `model.maintenance.CommonBoxRole` | 零修改（既有复用） | — |
| Cloud | `CloudTaskServicePort`/retained 权威/`CloudTemplateAssets`/`leader_box_marker.png` | 零修改 | 既有，冻结 |
| DHXY | exact ROI capture / moveAndClickLeft input / window·identity·taskRun 副作用前安全拒绝 / local artifact | 保留/非-A | DHXY 本地，caller cohort |
| — | AutoCombatService:637/826/836·AutoBattleTask:303·WubeiTask:3389/4638·XiuluoTaskV2:4771/4839·MainWindowController:609/612 caller 接线 | Modify | **非-A**：W-CBOX-2 cohort |

**双构建门**：W-CBOX-1 落地后（父级授权、并发写入稳定）Cloud `mvn -q clean package`（不 skip）+ DHXY `mvn -q -DskipTests compile`；不新增/恢复 tests，不启动运行面。W-CBOX-0 叶子可先独立编译。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design #1 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`；heartbeat 保持。

## Parent Design Review #1 - BLOCKED / Repair #1 Published - 2026-07-13T05:33:00-04:00

### 结论

- 整体 **BLOCKED**，当前不批准任何 Java 写入。
- P0=0，P1=5，P2=2。Worker 自审不构成父级批准。
- 父级简报中的 ROI 数值有误；本 review 以 HEAD `0114604e` 与 `docs/业务逻辑.md:94-98` 为准纠正，
  不要求 Worker 服从错误简报。

### P1-1：ROI inventory 与唯一业务基线不一致

- 证据：HEAD `CommonBoxService` 真实常量是 `ROI_LEFT=623`、`ROI_TOP=590`、`ROI_RIGHT=682`、
  `ROI_BOTTOM=618`；`docs/业务逻辑.md:94-98` 同样明确窗口相对 ROI 为 `(623,590)-(682,618)`、约
  `59x28`。Design #1 写成 `(623,472)-(822,618)`。
- 影响：会截取完全不同且大得多的区域，模板判定和点击点均不等价。
- Repair 条件：全篇改为 exact `(623,590)-(682,618)`；capture request 固定
  `CaptureRegion(WINDOW_CLIENT_PX,623,590,59,28)`，不得保留旧数值。

### P1-2：match 点再次被错误除以 systemScaleRatio

- 证据：HEAD `detectAndRecord` 对 `ImageFinder.find(raw,template,0.86)` 的结果直接计算
  `x1 + round(match[0])`、`y1 + round(match[1])`，没有 scale 算术；remote capture 的
  `WINDOW_CLIENT_PX` region 已由本地 binding 加客户区原点。
- 影响：非 1.0 scale 下会改变点击客户区坐标。
- Repair 条件：Cloud pending 点严格为
  `(623 + round(match[0]), 590 + round(match[1]))`，坐标空间为 `WINDOW_CLIENT_PX`；
  `systemScaleRatio/observedWindow` 只作 capture 完整性与绑定证据，不参与匹配点算术。

### P1-3：Cloud pending 单一权威与 UI toggle/clear-by-role 之间没有真实控制路径

- 证据：HEAD `MainWindowController:609/612` 在关闭队长/队员开关时直接调用
  `clearPendingForRole`；当前 Cloud `CloudTaskServiceMetadata` 不携带两个 common-box toggle，三种机械操作也都是
  Cloud→DHXY，不能把本地 UI 变更通知 Cloud。Design #1 又冻结 caller/transport，只写“Cloud 唯一 owner”。
- 影响：关闭开关后 Cloud 仍可能保留并消费旧 pending，或者为绕过该缺口在 DHXY 保留第二份 pending/业务判断。
- Repair 条件：给出 typed config/control authority 的精确既有或新增路径、revision 与 fan-out 范围：默认
  leader=true/member=false；toggle off 必须先让 Cloud 对 exact tenant 下该 role 的全部 active-run pending 完成清除/失效，
  后续 detect/consume 都读取同一 Cloud 配置真值。不得新增 raw 任意 endpoint，也不得让 DHXY 保留 pending map。

### P1-4：异步 detect 只有愿望描述，没有可编译调度 owner

- 证据：HEAD 明确 `CompletableFuture.runAsync`，保证调用点只做廉价同步 gates 后立即返回；当前 Cloud dormant runtime
  没有 Design #1 所称“Cloud task scheduling / retained workflow”的具体类型、队列、slot、assembly mount 或 wake 机制。
- 影响：直接同步调用会阻塞回程/退战关键路径；另起 executor/thread 又违反本切片边界；延后到任意下一 step 会改变检测时点。
- Repair 条件：列出真实可编译的非阻塞 retained detect-work item、唯一 owner、publication/wake、exact context/revision fence、
  同一调用 occurrence 与完成/取消规则；说明如何在不新增线程、不激活 host 的当前阶段保持 dormant，并把该 prerequisite
  纳入 DAG。若依赖尚不存在，W-CBOX-1 必须明确等待其切片，不能写成“已有 scheduling”。

### P1-5：retained handles 与 action renewal 同样没有铸造/挂载路径

- 证据：`CloudTaskServicePort` 只公开执行方法；`CaptureAction/InputBundleAction` 与
  `CloudTaskRetainedActionState.retain*` 都是 remote package-private。拟建 `service.CommonBoxService` 无法取得 handles；
  文件表也没有 authority adapter/assembly mount。
- 影响：设计不可接线，或实现时被迫开放 public free-form mint/bypass。
- Repair 条件：补 package-private fixed-slot adapter、opaque invocation bundle、exact runtime mount 和文件表；detect capture 与
  consume click 均使用固定 enum address。可信 `NOT_EXECUTED` 后只有在 Full R0 final-consumed compaction 完成时可由同一 retained
  invocation renewal；`UNKNOWN/STOPPED` 不得重投或铸新 ID。

### P2-1：pending owner 生命周期与 `clearPendingForRole` 范围未定义

- Design #1 一处称 store “per-run retained”，另一处要求 key 含 tenant/window/taskRun/role/task，并承接 UI 角色级清理。
  per-run store 无法天然完成 tenant 内跨 active-run 的 role clear。
- Repair 条件：明确 owner 是 tenant-scoped registry 还是 per-run state；若为 per-run，必须由同一 config authority 以 revision
  使所有 active runs 原子失效，不得全 JVM 跨租户清理。写出 lock/order 与 terminal cleanup，但不得新增 TTL/cleanup 语义。

### P2-2：30 秒计时不得悄然改变时钟语义

- HEAD 与业务文档定义从检测成功时刻起 30 秒，源码用 `System.currentTimeMillis()` 的 `detectedAtMs/expiresAtMs`。
  Design #1 改成未定义实现的 monotonic deadline，却仍声称无业务差异。
- Repair 条件：Repair 明确记录 wall-clock diagnostic 与 30 秒 elapsed deadline 的对应关系，TTL 从 **OBSERVED match 被写入
  pending** 时开始，不从 capture 发起/排队时开始；禁止 pause 延长、resume 重置或额外 grace。若采用 monotonic，只允许作为
  同一进程 elapsed 实现细节，并保留等价的 age/log 字段，不得宣称 durable restart 恢复。

### 下一任务与领取门

External Worker A 当前任务为 `W-CBOX-D2`：只在本日志追加 Design Repair #1 Delta，逐项关闭上述 P1/P2，未被点名章节不重抄；
DHXY/Cloud Java、Maven、schema/resources/tests/host/caller 全冻结。请在
`2026-07-13T05:53:00-04:00` 前追加 `CLAIMED`（task、claimedAt、唯一写集）。20 分钟只检查领取，领取后可继续工作超过 20 分钟。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T05:35:02-04:00
- task: W-CBOX-D2
- claimedAt: 2026-07-13T05:35:02-04:00
- uniqueWriteSet: only this append-only report

## External Worker A - Design Repair #1 Delta - 2026-07-13

关闭 `Parent Design Review #1` 的 P1×5+P2×2。仅设计，DHXY/Cloud Java/Maven/schema/resources/tests/host/caller 全冻结，不改 QuestManager/B/P2 Full R0/S/warning。未点名章节不重抄。取证：HEAD `CommonBoxService.java:39-50` 实测常量、`detectAndRecord:299-324`、既有 Cloud 配置 authority 家族（`CloudAutoBattle/BattleRadar/MaintenanceProperties(+Authority)`）、`CloudTaskServiceMetadata`(无 toggle)。

### V1（P1-1）：ROI 全篇改为 HEAD 真值 `(623,590)-(682,618)`

**撤回** Design #1 的 `(623,472)-(822,618)`（源于父级简报笔误，我误抄）。实测 HEAD 常量：`ROI_LEFT=623, ROI_TOP=590, ROI_RIGHT=682, ROI_BOTTOM=618`（约 `59x28`，与 `docs/业务逻辑.md:94-98` 一致）。capture request 固定 **`CaptureRegion(WINDOW_CLIENT_PX, 623, 590, 59, 28)`**（width=682-623=59，height=618-590=28）；全篇不留旧值。

### V2（P1-2）：match 点不除 systemScaleRatio（撤回 scale 算术）

**撤回** Design #1 §4 的 `round(local/scale)`。HEAD `detectAndRecord:323-324` 对 `ImageFinder.find(raw,template,0.86)` 结果直接 `x1+round(match[0]) / y1+round(match[1])`，**无 scale 算术**（与 QM 的 `findImageAbsoluteCoordinate` 除 scale 不同——两类 HEAD 语义本就不同，各自逐字保）。Cloud pending 点严格 = **`(623 + round(match[0]), 590 + round(match[1]))`**，坐标空间 `WINDOW_CLIENT_PX`；`systemScaleRatio/observedWindow` **只作 capture 完整性与 bound-window 绑定证据**，绝不参与匹配点算术。client→screen 由 DHXY input 时以 bound window 唯一解析。

### V3（P1-3）：typed config/control authority（UI toggle→Cloud clear 真实路径）

- **新增配置 authority（复刻既有家族）**：Cloud `com.bot.dhxy.config.CloudCommonBoxProperties`（public interface）+ `CloudCommonBoxPropertiesAuthority`（package-private，`CloudServiceScope` 绑定，`AtomicReference<Snapshot>`，`seedNoOverride/seedOverride`，revision CAS，`Source{BASELINE_NO_OVERRIDE, CONTROL_PLANE_OVERRIDE}`；同 `CloudMaintenancePropertiesAuthority` 形状）。字段/默认：`isLeaderCommonBoxEnabled()=true`、`isMemberCommonBoxEnabled()=false`（父级默认 leader=true/member=false）。
- **toggle off 控制路径**：HEAD `MainWindowController:609/612` 的 UI 关闭 → 走 control-plane override（该 role 置 false，revision++）。**fan-out**：override 对 exact tenant/user scope 下**全部 active-run** 生效；pending owner（V6）在该 role config 变 disabled 时，于同一 owner lock 内**原子清除/失效该 tenant 下该 role 的全部 pending**，然后 detect/consume 都读同一 Cloud config 真值（`isRoleEnabled(role)` = `config.isLeader/MemberEnabled`）。
- **不新增 raw 任意 endpoint、DHXY 不留 pending map**。（UI→control-plane 的具体传输接线属 host/config-plane cohort，非-A；本切片只定 authority 类型 + 语义 + revision/fan-out。）

### V4（P1-4）：async detect 的真实非阻塞调度 owner（据实标前置）

- HEAD `detectBox:273` = `CompletableFuture.runAsync(...)`：调用点只做廉价同步 gates（`pruneExpiredPending`/`normalizeSupportedTask`/`taskRunKey`/`isRoleEnabled`/`roleFor`/`actualRole==role`）后**立即返回**，capture+match 异步。
- **Cloud 现状据实**：dormant runtime **无**可编译的 detect-work scheduler/queue/slot/wake。故定义**最小非阻塞 retained detect-work item** 合同（**当前不存在→硬前置**，不虚构“已有 scheduling”）：
  - work-item：`CommonBoxDetectWorkItem`（capture ROI + template match + pending write），唯一 owner = per-run retained detect 调度 slot（随 activation owner）。
  - publication/wake：调用点同步 gates 通过后 **enqueue 一个 detect work item 并立即返回**；wake 由 cohort 调度切片（**与 TMS soft-wake 同族的非阻塞调度前置**）驱动，**不新增 Cloud 线程/background executor**、不激活 host（dormant 阶段只登记 work item，不执行）。
  - context/revision fence：work item 携 exact `(scope,taskRunId,window,stopEpoch,runRevision)`；执行前经三门反复活合同校验，stale→丢弃该 item（不写 pending）。
  - same-call occurrence：同一 detect 调用一个 occurrence；完成=写入/未命中/丢弃三终态；取消=run terminal/toggle off。
- **W-CBOX-1 detect 路径明确等待该调度切片**（硬前置纳入 DAG）；调度切片未批准前 detect 不可实施。consume/has/clear 不依赖该前置。

### V5（P1-5）：retained handle fixed-slot adapter + assembly mount

- `CloudTaskServicePort` 只暴露执行方法；`CaptureAction/InputBundleAction`、`CloudTaskRetainedActionState.retain*` 均 remote package-private → `com.bot.dhxy.service.CommonBoxService` **无法直接取 handle**。补：
  - **New package-private `com.yueyunfe.dhxy.cloudbrain.remote.CommonBoxServicePortAdapter`**（fixed-slot）：assembly mint，持 exact retained `CaptureAction`(detect)/`InputBundleAction`(consume) 于固定 enum address；对外只发 **opaque invocation bundle**（无 raw handle/mint）。
  - **固定 enum address**（phaseCode=`common-box`）：`DETECT_CAPTURE_LEADER/DETECT_CAPTURE_MEMBER`、`CONSUME_CLICK_LEADER/CONSUME_CLICK_MEMBER`（detect capture 与 consume click 各固定 slot）。
  - **assembly mount**：`CloudTaskRunAuthorityAssembly`/`TaskServiceRuntime` 创建并 resume 复用该 adapter（同 retainedActionState 跨 revision 复用形状）；`CommonBoxService` 构造注入该 opaque adapter。
  - **renewal**：可信 `NOT_EXECUTED` 仅在 Full R0 final-consumed compaction 完成后由**同一 retained invocation** renewal；`UNKNOWN/STOPPED` 不重投、不铸新 ID。
- 文件表补 adapter + assembly/runtime mount（见下）。

### V6（P2-1）：pending owner = tenant-scoped registry（撤回“per-run”歧义）

- **撤回** Design #1「per-run retained」与「含 tenant/window/taskRun/role/task key」的自相矛盾（per-run 无法做 tenant 内跨 active-run 的 role clear）。
- **owner = tenant-scoped `CommonBoxPendingRegistry`**（New，key = `(RemoteTaskRunScope, window tuple[windowId,nativeHandle,processId,playerIdentityEpoch], taskRunId, role, taskKey)`）；pending 记录是纯数据（coords/deadline/identity），存于 registry；机械 capture/click 用 per-run retained handle（V5）。
- **clearPendingForRole**：在 registry owner lock 内遍历**该 tenant scope** 下该 role 全部 entry 清除（对齐 HEAD `MainWindowController` 语义，但限定 exact tenant，**绝不全 JVM 跨租户**）。config revision override（V3）在同一 lock 内触发该 role clear。
- **lock/order**：registry lock 单点；config-authority revision 读在前、clear 在同 lock；terminal cleanup（run 终止）清该 run 的 entry。**不新增 TTL/cleanup 语义**。

### V7（P2-2）：30 秒时钟语义（从 OBSERVED match 写入起，wall-clock 诊断保留）

- **TTL 从 OBSERVED match 被写入 pending 时刻起 30 秒**（= HEAD `detectedAtMs=now`（match 写入时）、`expiresAtMs=now+30_000`），**不从 capture 发起/排队起**。
- 保留 HEAD `System.currentTimeMillis()` 的 `detectedAtMs/expiresAtMs` 及 `age`/log 字段（诊断等价）。若内部采 monotonic，**仅作同一进程 elapsed 实现细节**并保留等价 wall-clock age/log 字段；**禁 pause 延长、resume 重置、额外 grace**；**不宣称 durable restart 恢复**。prune/expired 判定逐字保 HEAD。

### 修订文件表增量（W-CBOX 实施期）

| 仓库 | 精确 FQCN | New/Modify | 门 / 归属 |
|---|---|---|---|
| Cloud | `config.CloudCommonBoxProperties` / `config.CloudCommonBoxPropertiesAuthority` | New×2 | V3 批准；Worker A（config 家族） |
| Cloud | `service.commonbox.CommonBoxPendingRegistry`（tenant-scoped，含 pending record） | New | V6 批准；Worker A |
| Cloud | `remote.CommonBoxServicePortAdapter`（fixed-slot，opaque bundle） | New | V5 批准；remote authority cohort 排序 |
| Cloud | `remote.CloudTaskRunAuthorityAssembly`/`TaskServiceRuntime`（mint+持+resume 复用 adapter） | Modify | V5 批准；authority cohort 排序（与 P/M/QM 在途） |
| Cloud | `service.CommonBoxService`（整类编排 dormant；经 opaque adapter+registry+config；ROI 623,590,59,28；无 scale 算术；detect 等待调度前置） | New | W-CBOX-1 门=上列 + M Full R0 + 调度切片(V4) + 父级授权 |
| Cloud | `model.maintenance.CommonBoxRole` / `CloudTemplateAssets`+`leader_box_marker.png` | 零修改 | 既有 |
| — | 非阻塞 detect 调度切片（V4，soft-wake 同族） | New | **非-A 硬前置**：独立切片 |
| — | AutoCombatService/AutoBattleTask/WubeiTask/XiuluoTaskV2/MainWindowController caller + UI→control-plane 接线 | Modify | **非-A**：W-CBOX-2 cohort |

**最小叶子波**修订：W-CBOX-0 = `CloudCommonBoxProperties(+Authority)`（config 家族，可独立编译）+ `CommonBoxPendingRegistry`（纯 state，可独立编译）；均无 host/caller。detect 路径（V4/V5）待调度切片 + adapter mount 前置。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #1 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #2 - PARTIAL PASS / Repair #2 Published - 2026-07-13T05:42:00-04:00

### 结论

- ROI `(623,590,59,28)`、零 scale 算术、typed config/control 方向、真实异步调度前置、fixed-slot retained adapter、tenant-scoped pending owner、30 秒 wall-clock 基线：方向均认可。
- 整体设计当前 P0=0、P1=1、P2=0，**仍 BLOCKED**；W-CBOX-1/adapter/assembly/caller/host 继续冻结。配置与 pending 类型也先不实施，避免把下面的竞态固化成 API。

### P1-1：toggle-off clear 与晚到 async detect 写 pending 尚未原子串行

- 证据：V3 让 config authority 用独立 `AtomicReference<Snapshot>` 更新，并称 toggle off 清空 pending；V4 的 detect work item 可在开关打开时入队，异步 capture/match 后才写 pending；V6 又称 config revision override 与 clear 在 registry lock 内，但未定义 config publish、pending clear、detect final write 共用同一个 governor/lock 或 revision CAS。
- 影响：时序 `detect 读 enabled -> UI off/publish revision/clear -> 旧 detect 晚到 put` 会在开关已关闭后复活 pending，后续 consume/has 的观察与 UI 真值不一致；这也是异步迁云后最危险的错窗输入来源之一。
- Repair 条件：定义唯一 `CommonBoxStateGovernor`（名称可调整但不能叠 wrapper）或等价单锁事务：
  1. detect enqueue 捕获 exact config revision；
  2. toggle override 的 revision publish + role pending clear 在同一 governor lock 内完成；
  3. detect final write 必须在同一 lock 内重新验证 exact tenant/scope/taskRun/window/stopEpoch/runRevision、role enabled 且 config revision 未变化，随后才原子写 pending；
  4. stale/toggle-off/terminal work item 只落丢弃终态，不写 pending、不 renewal、不产生输入；
  5. `has/consume/clear` 读写同一 registry/governor，不允许 config 与 pending 各自独立锁造成 TOCTOU。

### 下一任务 `W-CBOX-D3`

External Worker A 只追加 Design Repair #2 Delta，关闭上述 P1，并给出修订后的最小类型所有权和精确 New/Modify 表；Java、Maven、schema、resources、P2/B/S/Quest/warning/host/caller 全冻结。先追加 `CLAIMED`；领取截止为 `2026-07-13T06:02:00-04:00`。自审不算父级批准。

## External Worker A - CLAIMED - 2026-07-13T05:47:51-04:00
- task: W-CBOX-D3
- claimedAt: 2026-07-13T05:47:51-04:00
- uniqueWriteSet: only this append-only report

## External Worker A - Design Repair #2 Delta - 2026-07-13

关闭 `Parent Design Review #2` 的 P1-1（toggle-off clear 与晚到 async detect 写 pending 未原子串行）。仅设计，Java/Maven/schema/resources/P2/B/S/Quest/warning/host/caller 全冻结。已通过方向（ROI 623,590,59,28；零 scale；config/control；异步调度前置；fixed-slot adapter；tenant-scoped owner；30s wall-clock）不重写。

### W1（P1-1）：单一 `CommonBoxStateGovernor` 单锁事务（消除 config↔pending TOCTOU）

**撤回** V3 独立 `CloudCommonBoxPropertiesAuthority` 的自持 `AtomicReference<Snapshot>` 与 V6 registry 各自独立锁（config publish 与 pending clear/write 分锁→TOCTOU）。合并为**唯一单锁 owner**（不叠 wrapper）：

- **`com.yueyunfe.dhxy.cloudbrain.remote.CommonBoxStateGovernor`（New，package-private，单 `Object lock`）** 同时持有：
  - **config 状态**：`leaderEnabled`(默认 true)、`memberEnabled`(默认 false)、`configRevision`(单调 long)；
  - **tenant-scoped pending registry**：`Map<PendingKey, PendingCommonBox>`，key=`(RemoteTaskRunScope, window tuple[windowId,nativeHandle,processId,playerIdentityEpoch], taskRunId, role, taskKey)`。
  - `CloudCommonBoxProperties`（V3 的 public read-only 接口保留）由 governor **实现/暴露只读视图**，不再另设独立 authority 的第二锁/第二 snapshot。

**事务合同（全部在同一 `governor.lock` 内）**：

1. **detect enqueue 捕获 exact config revision**：`enqueueDetect(...)` 在 lock 内读当前 `configRevision` 与该 role `enabled`，返回携带 `capturedConfigRevision` 的 detect ticket；未 enabled 直接不入队（丢弃终态）。
2. **toggle override 原子**：`applyToggleOverride(role, enabled)` 在同一 lock 内 **`configRevision++` 发布 + 清除该 tenant scope 下该 role 全部 pending**（V3 fan-out + V6 clear 合并为单锁一步）。
3. **detect final write 重验后原子写**：`commitDetectPending(ticket, exactFences, PendingCommonBox)` 在同一 lock 内重新校验——exact `tenant/scope/taskRun/window/stopEpoch/runRevision` 全等、`role enabled` 为真、且 `configRevision == ticket.capturedConfigRevision`（未变化）——**全部通过才 `put` pending**；任一不符→**丢弃终态**（不写 pending、不 renewal、不产生输入）。这封死 `detect 读 enabled → UI off/publish/clear → 旧 detect 晚到 put` 复活窗口（commit 时 revision 已变或 role 已 disabled → 拒绝）。
4. **stale/toggle-off/terminal work item** 只落丢弃终态（no pending write / no renewal / no input）。
5. **`has/consume/clear` 读写同一 governor**：`hasPending`/`consumePendingIfAllowed`/`clearPendingForRole` 全部经 governor 单锁读写同一 registry + 同一 config 真值，**无 config 与 pending 各自独立锁**→无 TOCTOU。consume 内 click 前的 role/enabled/expired/staleWindow/staleIdentity/staleTaskRun 判定与 click 失败 keep-pending 均在 governor 语义下逐字保 HEAD（机械 click 本身经 retained adapter 在 lock 外执行，但 pending 的读取-判定-移除在 lock 内，click 结果回 lock 内落定）。

**并发线性化点** = `governor.lock` 单点；config revision CAS 与 pending map 变更同锁，杜绝跨锁 TOCTOU。**不新增 TTL/cleanup/retry 语义**（30s wall-clock、prune、keep-on-click-failure 逐字 HEAD）。

### 修订最小类型所有权 + 精确 New/Modify 表

| 仓库 | 精确 FQCN | New/Modify | 门 / 归属 |
|---|---|---|---|
| Cloud | `config.CloudCommonBoxProperties`（public read-only 接口；由 governor 暴露） | New | Worker A |
| Cloud | `remote.CommonBoxStateGovernor`（单锁 owner：config 状态 + configRevision + tenant pending registry + PendingCommonBox record；enqueueDetect/applyToggleOverride/commitDetectPending/hasPending/consume/clear） | New | W1 批准；remote authority cohort 排序 |
| Cloud | `remote.CommonBoxServicePortAdapter`（V5 fixed-slot：DETECT_CAPTURE_{LEADER,MEMBER}/CONSUME_CLICK_{LEADER,MEMBER} opaque bundle） | New | V5 批准 |
| Cloud | `remote.CloudTaskRunAuthorityAssembly`/`TaskServiceRuntime`（mint+持+resume 复用 governor + adapter，跨 revision 同对象） | Modify | authority cohort 排序（与 P/M/QM 在途） |
| Cloud | `service.CommonBoxService`（整类编排 dormant：经 opaque adapter + governor；ROI 623,590,59,28；零 scale；detect 走调度前置+ticket；consume/has/clear 经 governor） | New | W-CBOX-1 门=上列 + M Full R0 + 调度切片(V4) + 父级授权 |
| Cloud | `model.maintenance.CommonBoxRole` / `CloudTemplateAssets` + `leader_box_marker.png` | 零修改 | 既有 |
| — | 非阻塞 detect 调度切片（V4，soft-wake 同族；enqueueDetect ticket → 异步 capture/match → commitDetectPending） | New | **非-A 硬前置**：独立切片 |
| — | UI→control-plane toggle 接线（调 `applyToggleOverride`）+ AutoCombatService/AutoBattleTask/WubeiTask/XiuluoTaskV2/MainWindowController caller | Modify | **非-A**：W-CBOX-2 / config-plane cohort |

**撤回**：V3 独立 `CloudCommonBoxPropertiesAuthority`（第二锁/第二 snapshot 合并入 governor）、V6 独立 `CommonBoxPendingRegistry`（并入 governor 单锁）。

**最小叶子波 W-CBOX-0** 修订 = `CloudCommonBoxProperties`（read-only 接口）+ `CommonBoxStateGovernor`（单锁 config+pending owner，纯 state，可独立编译，无 host/caller）。detect 路径待 V4 调度切片 + V5 adapter mount。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #2 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #3 - BLOCKED / Repair #3 Published - 2026-07-13T05:54:00-04:00

### 结论

- toggle revision publish、role clear、late detect commit 统一到一把 governor lock 的时序已关闭上一 P1。
- 当前仍 `P0=0，P1=1，P2=1`，**BLOCKED**；W-CBOX-0/1、adapter/assembly/caller/host 继续冻结。

### P1-1：assembly 单例 governor 只有一份 config/revision，跨租户串权威

- 证据：W1 定义 assembly 只持一个 `CommonBoxStateGovernor`，其字段只有单份 `leaderEnabled/memberEnabled/configRevision`；
  `applyToggleOverride(role, enabled)`、`enqueueDetect(...)` 也没有 scope 参数。pending key 虽含 `RemoteTaskRunScope`，配置真值和
  revision 却没有按 tenant/user 分区。
- 影响：tenant A 关闭 member 会让 tenant B 的 detect 读到 disabled；tenant B 的 toggle 也会推进全局 revision，使 A 的合法
  detect ticket 被误判 stale。若 clear 按“该 tenant”执行，当前 API 甚至没有足够信息选出该 tenant，构成直接租户隔离破坏。
- Repair 条件：同一 governor 可继续作为 assembly 唯一实例与单锁线性化点，但必须在锁内持有 bounded
  `Map<CloudServiceScope,TenantState>`；每个 `TenantState` 独立拥有 leader/member/configRevision/pending entries。所有
  enqueue/apply/commit/has/consume/clear API 必须接 exact scope，并验证 `RemoteTaskRunScope -> CloudServiceScope` 一致；任何
  revision/clear/put 只触及该 scope，禁止全 JVM boolean/revision。

### P2-1：scope state/pending admission 需要硬容量与无部分写入拒绝

- 影响：authenticated scope 与 pending key 都可持续增长；若没有硬上限，未来 control-plane/active run 接线后会形成进程内存 DoS。
- Repair 条件：给出 assembly-injected 的全局/per-scope tenant-state 与 pending-entry hard cap；新 scope/pending 在任何 map 写入前
  原子检查，满额 fail-closed，不 TTL/LRU/静默淘汰。run terminal 只移除 exact run pending；config snapshot 是否保留必须与既有
  config-provider 生命周期一致，不得为省容量丢用户 override。

### 下一任务 `W-CBOX-D4`

External Worker A 只追加 Design Repair #3 Delta，关闭上述 P1/P2，修订所有 API 签名、owner map 与精确文件表；Java/Maven/
schema/resources/P2/B/T/Quest/warning/host/caller 全冻结。先追加 `CLAIMED`；领取截止
`2026-07-13T06:14:00-04:00`。自审不算父级批准。

## External Worker A - CLAIMED - 2026-07-13T05:54:47-04:00
- task: W-CBOX-D4
- claimedAt: 2026-07-13T05:54:47-04:00
- uniqueWriteSet: only this append-only report

## External Worker A - Design Repair #3 Delta - 2026-07-13

关闭 `Parent Design Review #3` 的 P1-1（跨租户串权威）+ P2-1（硬容量/无部分写入）。仅设计，Java/Maven/schema/resources/P2/B/T/Quest/warning/host/caller 全冻结。已通过（单 governor 单锁时序 W1）不重写。取证：`CloudServiceScope=(tenantId,userId)`；`RemoteTaskRunScope` 投影至 `(tenantId,userId)`；config authority 家族生命周期。

### X1（P1-1）：governor 内按 `Map<CloudServiceScope,TenantState>` 分区（撤回单份全局 config/revision）

**撤回** W1 governor 的单份 `leaderEnabled/memberEnabled/configRevision` 与无 scope 参数的 API（会跨租户串权威）。governor 仍是 **assembly 唯一实例 + 单锁线性化点**，但锁内持 **bounded `Map<CloudServiceScope, TenantState>`**：

- **`TenantState`**（per `CloudServiceScope`）独立持有：`leaderEnabled`(默认 true)、`memberEnabled`(默认 false)、`configRevision`(单调 long，**per-scope**)、`Map<PendingKey, PendingCommonBox> pending`（该 scope 的 entries）。
- **所有 API 接 exact scope 并校验 `RemoteTaskRunScope → CloudServiceScope` 一致**（`(scope.tenantId, scope.userId)` 投影相等，否则 typed reject）：
  - `enqueueDetect(CloudServiceScope, RemoteTaskRunScope, role, …)`：锁内取该 scope 的 `TenantState`，读其 `configRevision`/role enabled，返回携 `capturedConfigRevision` 的 ticket；未 enabled → 丢弃终态。
  - `applyToggleOverride(CloudServiceScope, role, enabled)`：**只**推进该 scope `TenantState.configRevision++` + 清该 scope 下该 role 全部 pending（**绝不动其它 scope**）。
  - `commitDetectPending(CloudServiceScope, ticket, exactFences, PendingCommonBox)`：锁内重验该 scope 的 `configRevision == ticket.capturedConfigRevision`、role enabled、exact scope/taskRun/window/stopEpoch/runRevision 全等，才 `put`；否则丢弃。
  - `hasPending/consumePendingIfAllowed/clearPendingForRole(CloudServiceScope, …)`：全部只读写该 scope `TenantState`。
- **PendingKey** 仍含 `RemoteTaskRunScope + window tuple + taskRunId + role + taskKey`，但**归属于其 CloudServiceScope 的 TenantState**——config 真值/revision/clear/put **只触及该 scope**，**无全 JVM boolean/revision**。tenant A 的 toggle 不影响 tenant B 的 enabled/revision/detect ticket。

### X2（P2-1）：assembly-injected 硬容量 + 原子无部分写入拒绝

- **assembly-injected 硬上限**（无 TTL/LRU/静默淘汰）：`maxTenantStates`（全局 authenticated scope 数上限）、`maxPendingEntriesPerScope`（每 scope pending 上限）。默认由 assembly 注入（如全局 1000 / per-scope 64，与既有 broker/adapter 容量族一致，最终值以父级/assembly 为准）。
- **原子 pre-write 检查**（governor 锁内，任何 map 写入前）：
  - 新 `CloudServiceScope` 首次出现（`applyToggleOverride`/`enqueueDetect`/`commit` 需创建 `TenantState`）→ 若 `tenantStates.size() >= maxTenantStates` 且该 scope 不存在 → **fail-closed typed reject**，不创建、不部分写。
  - `commitDetectPending` 写 pending 前 → 若该 scope `pending.size() >= maxPendingEntriesPerScope` 且 key 不存在 → **fail-closed**（该 detect 落丢弃终态，不写、不 renewal、不 input）。
- **run terminal**：只移除 **exact run** 的 pending（按 taskRunId 匹配该 scope 内 entry），不动其它 run/scope。
- **config snapshot 生命周期**：`TenantState` 的 config（leader/member/revision）保留策略**与既有 config-provider 生命周期一致**（不为省容量丢弃用户 override）；即 config override 一旦写入，不因 pending 容量或空 pending 而驱逐（唯一移除路径 = 与既有 config-provider 相同的显式失效/scope 退场）。容量拒绝只作用于**新增** scope/pending，绝不驱逐已有 override。
- 无部分写入：scope 创建 + config 更新 / pending put 在同锁内“检查→写”原子完成，失败即整体不写。

### 修订 API 签名 + owner map + 精确文件表

- **`CommonBoxStateGovernor`（New，remote，assembly 唯一实例，单 `Object lock`）**：
  - 锁内 `Map<CloudServiceScope, TenantState> tenantStates`（bounded `maxTenantStates`）。
  - `TenantState { boolean leaderEnabled=true; boolean memberEnabled=false; long configRevision; Map<PendingKey,PendingCommonBox> pending (bounded maxPendingEntriesPerScope); }`。
  - API（全接 `CloudServiceScope` + 校验 RemoteTaskRunScope 投影一致）：`enqueueDetect / applyToggleOverride / commitDetectPending / hasPending / consumePendingIfAllowed / clearPendingForRole / removeRunPending(scope, taskRunId)`。
  - 构造由 assembly 注入 `(maxTenantStates, maxPendingEntriesPerScope)`。

| 仓库 | 精确 FQCN | New/Modify | 门 / 归属 |
|---|---|---|---|
| Cloud | `config.CloudCommonBoxProperties`（public read-only 接口，per-scope 视图由 governor 暴露） | New | Worker A |
| Cloud | `remote.CommonBoxStateGovernor`（+ nested `TenantState`/`PendingKey`/`PendingCommonBox`；`Map<CloudServiceScope,TenantState>` bounded；assembly-injected caps） | New | X1/X2 批准；authority cohort 排序 |
| Cloud | `remote.CommonBoxServicePortAdapter`（V5 fixed-slot：DETECT_CAPTURE_{LEADER,MEMBER}/CONSUME_CLICK_{LEADER,MEMBER}） | New | V5 批准 |
| Cloud | `remote.CloudTaskRunAuthorityAssembly`/`TaskServiceRuntime`（mint+持+resume 复用 governor+adapter；注入 caps；run terminal→`removeRunPending`） | Modify | authority cohort 排序 |
| Cloud | `service.CommonBoxService`（整类编排 dormant：经 opaque adapter + governor(per-scope)；ROI 623,590,59,28；零 scale；detect 走调度前置+ticket+commit；has/consume/clear 接 scope） | New | W-CBOX-1 门=上列 + M Full R0 + 调度切片(V4) + 父级授权 |
| Cloud | `model.maintenance.CommonBoxRole` / `CloudTemplateAssets` + `leader_box_marker.png` | 零修改 | 既有 |
| — | 非阻塞 detect 调度切片（V4，soft-wake 同族） | New | **非-A 硬前置** |
| — | UI→control-plane toggle 接线(调 `applyToggleOverride(scope,role,enabled)`) + AutoCombatService/AutoBattleTask/WubeiTask/XiuluoTaskV2/MainWindowController caller | Modify | **非-A**：W-CBOX-2 / config-plane cohort |

**撤回**：W1 governor 的单份全局 config/revision 与无 scope API（并入 per-scope `TenantState`）。**最小叶子波 W-CBOX-0** = `CloudCommonBoxProperties` + `CommonBoxStateGovernor`（per-scope bounded config+pending 单锁 owner，纯 state，可独立编译）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #3 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #4 - LEAF APPROVED / Implementation Published - 2026-07-13T06:00:00-04:00

### 结论

- X1 的 bounded `Map<CloudServiceScope,TenantState>`、per-scope config revision 与 scope 投影校验关闭跨租户串权威；X2 的全局/per-scope hard cap、pre-write 原子拒绝与 exact-run terminal cleanup 关闭无界增长。该部分 `P0/P1/P2=0`。
- 按用户刚确认的 Runner 边界作父级定案：**持续观察、ROI capture/template match 与 observer wake 留在 DHXY 本地 Runner/observer；Cloud 不创建监控线程或非阻塞 capture scheduler。** Cloud 只接收带 exact scope/taskRun/window/stopEpoch/runRevision/configRevision 的 typed observation，并决定 pending/consume 业务状态。V4 的“Cloud 非阻塞 detect scheduler 硬前置”撤销，由本地 retained observer transport 后波承接。
- `W-CBOX-0` 状态叶子现 `DESIGN APPROVED`；完整 `CommonBoxService`、port adapter、observer transport、assembly/caller/host 仍冻结，不因叶子批准而启动运行面。

### 实施硬约束

1. governor 内不得执行 capture/match/click 或持锁 I/O；本轮只实现 per-scope config/pending/ticket/reservation 状态。
2. observation commit 必须校验 ticket 的 exact scope、完整 window tuple、taskRunId、stopEpoch、runRevision 与 captured configRevision；stale/disabled/capacity-full 只返回 typed reject，不写 pending。
3. consume 必须是 `reserve -> lock 外机械动作 -> settle`，reservation 绑定 pending generation + configRevision。toggle-off/clear/terminal 会使未派发 reservation 失效；不得用一次 `has` 后无栅栏点击。
4. `CloudCommonBoxProperties` 的 read-only 视图必须绑定一个 exact `CloudServiceScope`；禁止让无 scope 的全局 getter 读取“当前租户”。可由 governor 返回不可变 scoped snapshot/view，不得使用 ThreadLocal/static current。
5. hard cap 由构造注入且必须为正数；满额原子拒绝，不 TTL/LRU/静默驱逐。config override 生命周期不由 pending 清理缩短。

### 下一任务 `W-CBOX-0-IMP1`

External Worker A 直接实施以下唯一 Java 写集并在本日志追加 Implementation #1：

- New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\config\CloudCommonBoxProperties.java`
- New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CommonBoxStateGovernor.java`

其它 Java/Maven/schema/resources/tests、P2 写集、adapter/assembly/service/caller/host 全冻结；P2 正在写入，Worker A 不运行 Maven，由父级在写入稳定后统一 fresh package。先追加 `CLAIMED`（task、claimedAt、唯一写集），领取截止 `2026-07-13T06:20:00-04:00`。自审不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T06:07:41-04:00
- task: W-CBOX-0-IMP1
- claimedAt: 2026-07-13T06:07:41-04:00
- uniqueWriteSet: 2 New Java — dhxy-cloud-brain/src/main/java/com/bot/dhxy/config/CloudCommonBoxProperties.java + dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CommonBoxStateGovernor.java（不运行 Maven，父级统一 package）

## External Worker A - W-CBOX-0-IMP1 Implementation #1 - 2026-07-13T06:10:23-04:00

严格实施父级批准的唯一 Java 写集 **2 New / 0 Modify**；未改其它 Java/Maven/schema/resources/tests、P2 写集、adapter/assembly/service/caller/host；未启动运行面。

### 精确改动

1. **New `dhxy-cloud-brain/src/main/java/com/bot/dhxy/config/CloudCommonBoxProperties.java`**：public read-only 接口，**绑定 exact `CloudServiceScope`**（`scope()`），暴露 `configRevision()`/`isLeaderCommonBoxEnabled()`/`isMemberCommonBoxEnabled()`；**无 scope-less 全局 getter、无 ThreadLocal/static current**（约束4）。
2. **New `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CommonBoxStateGovernor.java`**（package-private final，单锁 owner）：
   - 锁内 bounded `Map<CloudServiceScope, TenantState>`；每 `TenantState` 独立 `leaderEnabled=true/memberEnabled=false/configRevision/pending Map`（per-scope，无全局 boolean/revision）。
   - **无 capture/match/click、无持锁 I/O**（约束1）：只有 per-scope config/pending/ticket/reservation 状态。
   - `properties(scope)` 返回不可变 scoped snapshot `ScopedSnapshot implements CloudCommonBoxProperties`（约束4）。
   - `applyToggleOverride(scope,role,enabled)`：同锁 `configRevision++` + disable 时 `clearRole`。
   - `beginObservation(...)`：校验 `RemoteTaskRunScope→CloudServiceScope` 投影一致 + role enabled，捕获 `capturedConfigRevision`，返回 `DetectTicket`（不写 pending）。
   - `commitObservation(ticket,templateX,templateY,detectedAtMs)`（约束2）：同锁重验 `configRevision==captured`（否则 REJECTED_STALE）、role enabled（REJECTED_DISABLED）、容量（REJECTED_CAPACITY，pre-write 原子拒绝，约束5）；通过才写 pending（TTL=detectedAtMs+30000，wall-clock 由 caller 传，约束 V7）。
   - `reserveConsume(...)`→lock 外机械动作→`settleConsume(reservation,clicked)`（约束3）：reservation 绑 pending `generation`+`configRevision`；toggle-off/clear/terminal/generation 变更使 reservation 失效（settle no-op）；clicked 才移除 pending，失败保留至 TTL（逐字 HEAD keep-on-click-failure）。
   - `hasPending/clearPendingForRole/removeRunPending`：全接 exact scope；removeRunPending 只移 exact taskRunId 的 entry（约束5 terminal cleanup）。
   - hard cap 构造注入 `(maxTenantStates,maxPendingEntriesPerScope)` 正数校验；满额原子拒绝、**无 TTL/LRU/静默驱逐**；config override 不因 pending 清理缩短（约束5）。

### 构建证据

按父级指示**未运行 Maven**（P2 正在写入 `target/`，父级在写入稳定后统一 fresh package）。本 Worker 仅做**只读隔离 javac**（输出到 scratch、classpath 用现有 `target/classes`，**不写/不触碰 target/**）：
```
javac -cp target/classes -d <scratch> CloudCommonBoxProperties.java CommonBoxStateGovernor.java
JAVAC_EXIT=0 → 8 class（含 CommonBoxStateGovernor 及 nested DetectTicket/ConsumeReservation/PendingKey/PendingEntry/TenantState/ScopedSnapshot/CommitOutcome）
```
即语法/类型自洽通过；权威构建仍由父级统一 Maven package 判定。

W-CBOX-0-IMP1 到此停止等待父级源码/构建复审。完整 CommonBoxService、port adapter、observer transport、assembly/caller/host 仍冻结。heartbeat 保持。

## Parent Implementation Review #1 - BLOCKED - 2026-07-13T06:13:29-04:00

父级逐行复核两个 New 文件并对照 Design Review #4 硬约束及 DHXY HEAD `CommonBoxService`。隔离 `javac` 只证明类型可编译；当前 `P0=0 / P1=4 / P2=1`，不得 APPROVED：

1. **P1：observation ticket 携带的 authority fence 从未与“当前值”比较。** `CommonBoxStateGovernor.java:135-162` 的 `commitObservation` 只重验 config revision/role/capacity；`DetectTicket` 的完整 window tuple、`taskRunId/stopEpoch/runRevision` 只是自带字段，没有 current execution fence 参数或 coordinator/assembly authority read。pause/resume、stop、窗口 incarnation 变化后，旧 ticket 仍可 commit pending，违反 Review #4 第 2 条。返修必须让 commit 接收由当前 retained execution context 投影出的 exact current fence，并逐字段比较 scope、完整 run scope、window 4-tuple、taskRunId、stopEpoch、runRevision；任一不等返回 typed stale 且零写入。
2. **P1：consume 路径遗漏 stop/revision/session fence。** `:173-205,234-262` 只比较 tenant/user 投影、windowId/nativeHandle/processId/playerIdentityEpoch/taskRun/taskCode，未比较 `RemoteTaskRunScope.deviceId/clientSessionId`、stopEpoch、runRevision；旧 runRevision pending 可在 resume 后被新 context 取出。返修须把 pending origin 与 current exact fence 全量比较，普通 has/reserve 都不得绕过。
3. **P1：reserve 没有原子占用态，可并发双击；boolean settle 又压扁 uncertain。** `:198-205` 返回 reservation 时不修改 entry，两个线程可拿到同 generation 并都在锁外发机械动作；`:214-227` 的 `boolean clicked` 也无法区分可信 `NOT_EXECUTED` 与 `UNKNOWN/STOPPED`，后者若按 false 保留会再次 reserve。返修须在锁内做唯一 reservation claim；settle 使用 closed typed outcome：`EXECUTED` exact remove，可信 `NOT_EXECUTED` 才 release claim，`UNKNOWN/STOPPED` 保守封存该 invocation（不铸新 identity、不再次 reserve），toggle/clear/terminal 仍使 claim 失效。
4. **P1：expired pending 永不回收，hard cap 会永久耗尽。** `:154-161` capacity check 前不 prune；`:199-203,259-261` 对过期项只返回 null/false而不 remove。不同 key 的 30 秒旧记录会一直占 `maxPendingEntriesPerScope`，最终所有新 observation 永久 `REJECTED_CAPACITY`，与 HEAD 每次 detect/consume/has 前 `pruneExpiredPending()` 不等价。返修须在同一 governor lock 内、capacity admission 和 reads 前按 HEAD wall-clock 30 秒 exact prune，并确保 reservation/uncertain 规则与 TTL 到期 removal 一致。
5. **P2：tenant hard cap 缺显式 scope 退场。** `tenantStates` 只增不减；即使 run terminal 清空 pending，历史 scope 仍永久占 `maxTenantStates`。Design D4 已规定只允许 trusted config-provider/scope lifecycle 显式退场，禁止 TTL/LRU。返修须增加 exact scope retirement API，并明确它只能由 authority lifecycle 调用、原子失效该 scope 的 pending/reservation/config snapshot；不得由 pending cleanup 暗删 override。

### 下一任务 `W-CBOX-0-IMP1-R1`

External Worker A 先追加 `CLAIMED`（领取截止 `2026-07-13T06:34:00-04:00`），仍只修改本日志与上述两个 New Java 文件。按五项定点返修并追加 Implementation Repair #1；adapter/assembly/service/caller/host、P2 写集、其它 Java/Maven/schema/resources/tests 全冻结。P2 正在写入，不运行 Maven；可用隔离 scratch `javac`。Worker 自审不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T06:16:12-04:00
- task: W-CBOX-0-IMP1-R1
- claimedAt: 2026-07-13T06:16:12-04:00
- uniqueWriteSet: 本日志 + 2 已批 New Java（config/CloudCommonBoxProperties.java + remote/CommonBoxStateGovernor.java），仅返修不扩范围（不跑 Maven，隔离 javac 自检）

## External Worker A - W-CBOX-0-IMP1 Implementation Repair #1 - 2026-07-13T06:18:29-04:00

按父级 Implementation Review #1 五项定点返修，仅改已批 `CommonBoxStateGovernor.java`（`CloudCommonBoxProperties.java` 无需改），零扩范围。

### 逐项修复

1. **P1 commit fence 逐字段比较**：新增 `record Fence(RemoteTaskRunScope runScope, windowId, nativeHandle, processId, playerIdentityEpoch, taskRunId, stopEpoch, runRevision)`（值相等一次校验全字段）。`beginObservation(scope,role,taskCode,Fence)` 捕获 fence；`commitObservation(ticket, Fence currentFence, ...)` 先 `ticket.fence().equals(currentFence)`（含完整 run scope/window 4-tuple/taskRunId/stopEpoch/runRevision），不等即 `REJECTED_STALE` 零写入；再校 configRevision/role/capacity。pause/resume/stop/incarnation 变化后旧 ticket 必 stale。
2. **P1 consume 全量 fence**：`reserveConsume`/`hasPending` 现接 `Fence currentFence` 并对 `entry.fence.equals(current)` 全字段比较（含 `deviceId/clientSessionId`(run scope)、stopEpoch、runRevision）；旧 runRevision pending 在 resume 后不匹配当前 fence→不取出。has/reserve 均不绕过。
3. **P1 原子 claim 三态 + typed settle**：PendingEntry 加 `ClaimState{UNCLAIMED,CLAIMED,SEALED}`。`reserveConsume` 在锁内仅当 `UNCLAIMED` 才置 `CLAIMED` 并返 reservation（并发第二 caller 得 null，杜绝双击）。`settleConsume(reservation, ConsumeOutcome{EXECUTED,NOT_EXECUTED,UNKNOWN_OR_STOPPED})`：EXECUTED→exact remove；可信 NOT_EXECUTED→release(→UNCLAIMED 可 exact retry)；UNKNOWN_OR_STOPPED→`SEALED`（永不再 reserve、不铸新 identity，仅 TTL 到期移除）。toggle/clear/terminal/generation/configRevision 变更使 settle no-op。
4. **P1 prune 过期**：新增 `pruneExpired(state, nowMs)`（`expiresAtMs<=nowMs` remove），在 **commit 的 capacity admission 前**、**reserve/has 读取前**于同 governor lock 内调用（对齐 HEAD 每次 detect/consume/has 前 `pruneExpiredPending()`，wall-clock 30s）；hard cap 不再被过期项永久占用。
5. **P2 scope 退场**：新增 `retireScope(CloudServiceScope)`——原子移除该 scope 的 pending/reservation/config snapshot，释放 tenant-state 槽；文档明确**仅 authority lifecycle 调用**；`removeRunPending`/`clearPendingForRole` 只清 pending、**绝不删 scope/override**。无 TTL/LRU。

### 构建证据

按父级指示**未运行 Maven**（P2 在写 target/）。只读隔离 `javac -cp target/classes -d <scratch>` 两文件 → **JAVAC_EXIT=0**（含新 nested Fence/ConsumeOutcome/ClaimState/DetectTicket/ConsumeReservation/PendingKey/PendingEntry/TenantState/ScopedSnapshot/CommitOutcome，共 11 class），不写/不触碰 target/。权威构建仍由父级统一 Maven package。

W-CBOX-0-IMP1-R1 到此停止等待父级复审。adapter/assembly/service/caller/host、P2 写集、其它 Java/Maven/schema/resources/tests 仍冻结。heartbeat 保持。

## Parent Implementation Review #2 - BLOCKED - 2026-07-13T06:32:00-04:00

父级逐行复核 Repair #1 后的 `CommonBoxStateGovernor.java`。上一轮 stale fence、唯一 claim、typed settle、TTL prune 与 scope retirement 已修复，不重开；但当前仍有 `P0=0 / P1=3 / P2=0`，不得 SOURCE APPROVED：

1. **P1：新的 observation 可以覆盖正在执行或已封存的 pending，重新打开第二次点击。** `CommonBoxStateGovernor.java:145-153` 对同 key 无条件 `put`；即使旧 entry 已在 `reserveConsume` 中置为 `CLAIMED`，或 uncertain 后置为 `SEALED`，新 detect 仍用下一 generation 替换为 `UNCLAIMED`。旧机械点击尚在锁外执行时，第二 caller 可立刻 reserve 新 entry，形成同一提示并发双击；`SEALED` 的“不再 reserve”合同也被绕过。返修必须在同锁 commit 中禁止覆盖 `CLAIMED/SEALED`，返回 closed typed busy/sealed reject；只有 `UNCLAIMED` 才允许按 HEAD 更新坐标/generation。TTL 到期仍可由既有 prune 移除。
2. **P1：`PendingKey` 仍不是已批准的完整隔离键。** `:399-401,428-450` 只含 `windowId/nativeHandle/role/taskCode/taskRunId`，遗漏 `RemoteTaskRunScope.deviceId/clientSessionId`、`processId/playerIdentityEpoch` 与 `stopEpoch`。虽然 entry 内 fence会在读取时比较，但另一个 device/session 或窗口 incarnation 的 commit 会先按这个短 key 覆盖原 entry，尤其会破坏正在执行的 claim。返修须让 key 至少包含完整 `RemoteTaskRunScope`、完整 window 4-tuple、taskRunId、stopEpoch、role、taskCode；runRevision 继续由 entry fence/claim 约束，但不得让不同稳定运行身份共享同一 map slot。
3. **P1：`retireScope` 后 reservation/ticket 存在 ABA。** `:278-282` 删除 `TenantState` 后，新建同一 `CloudServiceScope` 会把 `configRevision` 与每-key generation 都从 `0/1` 重新开始；旧 `ConsumeReservation` 只携 scope/key/generation/configRevision（`:434-441`）。若相同 run/key 被重建，旧 settle 可误命中新 entry，旧 DetectTicket 也可能在字段恰好复用时写入。返修须增加 governor 单调 scope-state incarnation（每次创建 TenantState 唯一），让 DetectTicket、PendingEntry、ConsumeReservation 与所有 commit/settle 精确绑定该 incarnation；retire 后旧 capability 永久失效，不得仅靠可重置 revision/generation。

### 下一任务 `W-CBOX-0-IMP1-R2`

External Worker A 仅修改本日志与已批准的两个 New Java 文件，定点关闭上述三项；adapter/assembly/service/caller/host、P2/B/T 写集及其它 Java/Maven/schema/resources/tests 全冻结。先在本日志追加 `CLAIMED`（task、claimedAt、唯一写集），领取截止 `2026-07-13T06:52:00-04:00`。P2 正在返修，Worker A 不运行 Maven，可做隔离 `javac`；自审不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T06:34:47-04:00
- task: W-CBOX-0-IMP1-R2
- claimedAt: 2026-07-13T06:34:47-04:00
- uniqueWriteSet: 本日志 + 已批 remote/CommonBoxStateGovernor.java（仅定点返修，config 接口无需改；不跑 Maven，隔离 javac 自检）

## External Worker A - W-CBOX-0-IMP1 Implementation Repair #2 - 2026-07-13T06:36:51-04:00

按父级 Implementation Review #2 三项定点返修 `CommonBoxStateGovernor.java`；config 接口无需改，零扩范围。上轮已修（stale fence/唯一 claim/typed settle/TTL prune/scope retire）不重开。

### 逐项修复

1. **P1 commit 禁覆盖 CLAIMED/SEALED**：`commitObservation` 在同锁 `put` 前查该 key 现有 entry 的 claim——`CLAIMED`→`REJECTED_BUSY`、`SEALED`→`REJECTED_SEALED`；**仅 UNCLAIMED 或 absent 才允许**按 HEAD 更新坐标/generation。执行中的旧点击不会被第二 entry 抢开双击；SEALED“不再 reserve”合同不被绕过；TTL 到期仍由 `pruneExpired` 移除。新增 `CommitOutcome{REJECTED_BUSY,REJECTED_SEALED}`。
2. **P1 PendingKey 全隔离维度**：`record PendingKey(RemoteTaskRunScope runScope, windowId, nativeHandle, processId, playerIdentityEpoch, taskRunId, stopEpoch, role, taskCode)`——含完整 `RemoteTaskRunScope`(tenant/user/**device/clientSession**)、完整 window 4-tuple、taskRunId、stopEpoch、role、taskCode；**runRevision 不入 key**（由 entry.fence/claim 约束）。不同 device/session/窗口 incarnation/stopEpoch 不再共享 map slot，短 key 覆盖破坏 claim 的路径消除。
3. **P1 incarnation 防 retire ABA**：governor 加单调 `incarnationSeq`，每次 `newState` 分配唯一 `TenantState.incarnation`（retire 后重建必得**严格更大**的 incarnation）。`DetectTicket`/`PendingEntry`/`ConsumeReservation` 均携 incarnation；`commitObservation` 校 `state.incarnation==ticket.incarnation`（否则 STALE）、`settleConsume` 校 `state.incarnation==reservation.incarnation`（否则 no-op）。retire+recreate 后旧 ticket/reservation 永久失效，不再靠可重置 revision/generation。`beginObservation` 现 get-or-create state（capacity 满则返 null）以绑 incarnation。

### 构建证据

按父级指示**未运行 Maven**（P2 返修中）。只读隔离 `javac -cp target/classes -d <scratch>` 两文件 → **JAVAC_EXIT=0**（CommonBoxStateGovernor 及 nested Fence/DetectTicket/ConsumeReservation/PendingKey/PendingEntry/TenantState/ClaimState/CommitOutcome/ConsumeOutcome/ScopedSnapshot 共 11 class），不写/不触碰 target/。权威构建仍由父级统一 Maven package。

W-CBOX-0-IMP1-R2 到此停止等待父级复审。adapter/assembly/service/caller/host、P2/B/T 写集及其它 Java/Maven/schema/resources/tests 仍冻结。heartbeat 保持。

## Parent Implementation Review #3 - SOURCE APPROVED - 2026-07-13T06:41:00-04:00

父级逐行复核 Repair #2 后的 `CommonBoxStateGovernor.java` 与 `CloudCommonBoxProperties.java`。本轮三个开放 P1 均已关闭：

1. `commitObservation` 在 governor 同锁内先检查同 full key 的旧 entry，`CLAIMED` 返回 `REJECTED_BUSY`、`SEALED` 返回 `REJECTED_SEALED`，只有 absent/`UNCLAIMED` 才可写下一 generation；新 detect 不再重开执行中或 uncertain click。
2. `PendingKey` 已包含完整 `RemoteTaskRunScope`、window 4-tuple、`taskRunId`、`stopEpoch`、role 与 taskCode；device/session、process/player epoch 或 stop incarnation 不再共享 map slot，`runRevision` 继续由 entry 的 exact `Fence` 比较而不制造跨 revision 新槽。
3. governor 以 `Math.incrementExact` 的单调 `incarnationSeq` 给每次 `TenantState` 创建分配唯一 incarnation，ticket/entry/reservation 都携带并在 commit/settle 精确验证；`retireScope` 后旧 capability 不可能 ABA 命中新 state。

结合上一轮已通过的 current fence、唯一 claim、typed settle、30 秒 prune 与显式 scope retirement，当前源码审查结论为 `P0=0 / P1=0 / P2=0`，`SOURCE APPROVED`。该批准只覆盖两个 dormant Cloud 状态叶子，不批准 adapter/assembly/service/caller/host，也不增加同路径计数。P2 正在返修双仓 transaction，父级暂不并发运行 Maven；最终构建批准须等写入稳定后执行 fresh Cloud `mvn -q clean package`（不 skip）。

**无已批准业务差异；按基线等价迁移。**
