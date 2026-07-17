# Cloud QuestManagerService - External Worker A

## Parent Task Brief #1 - W-QM-D1 - 2026-07-13T04:18:00-04:00

### 目标

以 DHXY HEAD `0114604e` 的 `com.bot.dhxy.service.QuestManagerService` 为唯一业务权威，设计整类 Cloud
lift-and-shift：Cloud 持任务面板激活、滚动、标题/标签匹配、发光判定与任务详情 capture 编排；DHXY 只保留 exact window
capture、原子输入 bundle、窗口/输入安全门与本地 artifact 执行。不得改变三页扫描、title fallback、阈值、offset、sleep、
close-panel/finally 或 `keepOpen` 语义。

### 领取门

External Worker A 必须在 `2026-07-13T04:38:00-04:00` 前追加
`## External Worker A - CLAIMED - <timestamp>`，包含任务标题、领取时间与唯一写集。20 分钟只检查领取，领取后允许工作超过
20 分钟。父级 `DESIGN APPROVED` 前唯一写集为**仅本固定日志 append-only**，Java/resources/tests/schema 冻结。

### Design #1 必须闭合

1. 完整 inventory：全部 public/private API、唯一生产 caller、Spring 构造依赖、mutable/static/per-call state，以及 HEAD
   `activateTaskIfPresent` 与 `captureCurrentQuestDetailForTask` 的精确调用链。
2. 逐步业务矩阵：`Alt+Q -> anchor -> current-task tab -> 3 pages -> label variants -> title fallback -> scroll -> glow/click ->
   keepOpen/close`；详情 capture 的 exclusive 边界、finally close、ROI/absolute coordinate、PNG artifact 与空结果语义。
3. exact context/authority：tenant/user/device/clientSession/taskRun/window/stopEpoch/runRevision；机械能力只能经
   `CloudTaskServicePort` retained typed handle。不得复制 `GameClientTracker`、`CoordinateHelper` window authority、
   `InputSequences/InputProvider/InputActionScope`、HWND、temp-path owner 或本地 queue。
4. retained action identity 与 typed unwind：每个 panel step/capture/input 的 canonical phase/actionSlot/occurrence owner；
   UNKNOWN/STOPPED/NOT_EXECUTED 不得被压成 task-not-found，不得自动 retry/换 ID。pause/resume 必须保持 HEAD 同一业务流程，
   旧 revision request 不复活。
5. 模板/resource authority：`task_fenxiang.png`、`<task>.png/_active/_selected`、`<task>_title.png` 的 classpath/artifact
   归属与 deterministic candidate order；禁止 cwd 双重权威。发光 CPU 判定保留 `RGB>220`、`count>15`。
6. 与当前切片的依赖 DAG：A 已批准 capture-time `systemScaleRatio`；P Full R0、R whole-pass exclusive、B warning 都不得被
   本设计修改。划出至少一个可独立编译、无 host/caller 的最小叶子波次，并给 Cloud/DHXY 精确 New/Modify 文件表与最终双构建门。

### 约束

- 不新增 retry/TTL/takeover/thread/poller/测试；不启动 application/server/host/Task/UI/capture/input。
- 不改 DHXY dirty Java，不覆盖任何并行文件，不做 git mutation。
- Worker 自审只算 QA，不构成批准；交付 Design #1 后等待父级源码/设计审查。

**无已批准业务差异；按基线等价迁移。**

## Parent Next-task Handoff - `W-DCM-D1` - 2026-07-13T14:24:25-04:00（真实 EOF）

本任务四文件已 `SOURCE APPROVED`，最终 DHXY compile 由父级在 Internal Z 原子写集稳定后统一复验；A 不等待该共享门，也不得
修改 handler/operation cohort。下一独立任务已发布到
`docs/superpowers/plans/reports/2026-07-13-cloud-dialog-choice-memory-service-worker-a.md`：只做 HEAD-clean
`DialogChoiceMemoryService` 整类 Cloud lift Design #1。A 须在新日志 `2026-07-13T14:44:25-04:00` 前先写
`CLAIMED`；批准前零 Java。任务与 External B TeamReturn、Internal Y PlayerState、Internal Z SummonSkill 写集完全不重叠。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Task - W-QM-ARTIFACT-TYPES-IMP1 - 2026-07-13T13:40:00-04:00

`W-NAV-WF-IMP1` 已父级 FINAL APPROVED。External Worker A 现转回已经 DESIGN APPROVED 的 QuestManager D5，先实施
不触共享 wire/assembly 的 closed artifact 类型闭包。须在发布后 20 分钟内于本日志真实 EOF 追加 `CLAIMED`（task、
claimedAt、uniqueWriteSet），然后仅新建以下 4 个文件：

1. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/QuestArtifactTaskCode.java`：public closed enum，
   当前唯一 wire token 为 `XIULUO`；
2. Cloud `.../remote/QuestDetailArtifactIntent.java`：public immutable record，仅含 non-null
   `QuestArtifactTaskCode taskCode`，constructor 用现有 `RemoteProtocolValidation.required`；
3. DHXY `src/main/java/com/bot/dhxy/cloud/remote/RemoteQuestArtifactTaskCode.java`：public closed enum，
   token 与 Cloud 逐字一致，仅 `XIULUO`；
4. DHXY `.../cloud/remote/RemoteQuestDetailArtifactIntent.java`：按本地 DTO 惯例使用 `@Value @Builder @Jacksonized`，
   仅含 `RemoteQuestArtifactTaskCode taskCode`，不加默认值、路径、kind、自由文本或兼容 fallback。

本波只落真实协议值类型，不修改 `CaptureRequest`、payload/codec/digest/envelope/gate/handler/schema、workflow state、
assembly、Service、Task、host/caller；不得新增 tests，不启动运行面，不做 Git mutation。完成后分别运行 Cloud
`mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，向本日志追加精确文件、wire token 对称性、
构建结果与 self-QA。Worker self-QA 不构成父级批准；逾期只重发给 External A，绝不由内部 Worker 接管。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T04:23:50-04:00

领取父级任务 **W-QM-D1**（Cloud QuestManagerService 整类 lift-and-shift 设计）。唯一写集 = **仅本固定日志 append-only**（设计/源码核对，**Java/resources/tests/schema 零修改**）。领取截止 2026-07-13T04:38:00-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：以 DHXY HEAD 0114604e `com.bot.dhxy.service.QuestManagerService` 为唯一业务权威取证，随后追加 `External Worker A - Design #1` 逐条闭合父级 6 点（inventory / 业务矩阵 Alt+Q→anchor→current-task tab→3 pages→label variants→title fallback→scroll→glow/click→keepOpen/close / exact context+authority 经 CloudTaskServicePort retained handle / retained action identity+typed unwind / 模板 resource authority(task_fenxiang.png、<task>.png/_active/_selected、<task>_title.png，RGB>220 count>15) / 依赖 DAG+最小可独立编译叶子波次+双仓精确文件表+双构建门）。不改 Java/resources/tests/schema，不启动运行面，不做 git，不覆盖并行文件。批准前继续冻结。

## External Worker A - Design #1 - 2026-07-13

以 DHXY HEAD `0114604e` `com.bot.dhxy.service.QuestManagerService`（390 行）为唯一业务权威的整类 Cloud lift-and-shift 设计。仅设计，Java/resources/tests/schema 零修改，未启动运行面，未 git，未覆盖并行文件。全部引用真实符号；凡 Cloud 侧尚不存在的类型均标为 New/精确缺失 leaf，不虚构已存在。

### 1. 完整 inventory（精确 FQCN/行号）

- **Spring 构造依赖**（`@Component @RequiredArgsConstructor`，L39-49）：`InputSequences inputSequences`、`InputProvider inputProvider`、`WindowAwareInputCoordinator inputCoordinator`（**声明但方法体未用**，L45）、`GameClientTracker tracker`、`CoordinateHelper coordinateHelper`、`GameContext context`（声明但方法体未用，L48）、`WindowScopedTempPath windowScopedTempPath`。
- **常量**（L51-76）：`ANCHOR_PATH="images/template/task/task_fenxiang.png"`；左面板 ROI `OFFSET_X=-497,OFFSET_Y=8,W_LEFT=223,PANEL_H=295`；右详情 ROI `DETAIL_TEXT_OFFSET_X=-269,_Y=12,_W=264,_H=50`；当前任务页签 `CURRENT_TASK_TAB_X=-442,_Y=-25`；`P1_X=-209,P1_Y=37`（**未引用**）；`THRESHOLD_STRICT=0.85,THRESHOLD_NORMAL=0.80`；发光 `GLOW_RGB_MIN=220,GLOW_TARGET=15`；节奏 `SLOW=800,MID=500,FAST=200`；`WUHUAN_TASK_LINK_CLICK_HOLD_MS=150`（**未引用**）。
- **public API（3）**：`activateTaskIfPresent(String)`(L79)→`(task,false)`；`activateTaskIfPresent(String,boolean keepOpen)`(L81，sequenced lane)；`captureCurrentQuestDetailForTask(String)`(L178，exclusive lane 包 Direct)。
- **private API**：`activateTaskIfPresentDirect`(L116)、`findTaskLabelInRegion`(L151)、`taskLabelTemplatePaths`(L163)、`captureCurrentQuestDetailForTaskDirect`(L192)、`saveQuestDetailDebugImage`(L233)、`saveQuestDetailDebugImageToPath`(L242)、`ensurePanel`(L257)/`ensurePanelDirect`(L272)、`selectCurrentTaskTab`(L290)/`selectCurrentTaskTabDirect`(L304)、`isTextGlowing`(L319)、`click`(L337)/`clickDirect`(L345)、`scroll`(L354)/`scrollDirect`(L364)、`closePanel`(L377)/`closePanelDirect`(L381)、`findAnchor`(L388)。
- **两条并行 lane**：**sequenced**（`inputSequences.submitAndWait`/`submitExclusiveAndWait`，用于 `activateTaskIfPresent`/`click`/`scroll`/`closePanel`/`selectCurrentTaskTab`/`ensurePanel`）与 **Direct**（`inputProvider` + `InputActionScope.checkpoint()` + `TaskSleep.sleep()`，用于 `*Direct`）。public `captureCurrentQuestDetailForTask` 经 `submitExclusiveAndWait("quest:captureDetail:"+task, …)`(L180) 独占，内部**全用 Direct** 变体。
- **可变/静态/per-call state**：无 static 可变字段（常量全 final）；per-call 仅 `captureCurrentQuestDetailForTask` 的 `AtomicReference<QuestDetailCapture> result`(L179)。
- **唯一生产 caller**：`com.bot.dhxy.task.xiuluo.XiuluoTaskV2:6238` → `captureCurrentQuestDetailForTask(QUEST_PANEL_TASK_CODE)`（唯一外部调用点）；`activateTaskIfPresent` 公共面在 HEAD 无其它 DHXY 生产调用点（保留契约面，caller cohort 迁移时确认）。
- **精确调用链**：
  - `activateTaskIfPresent(task,keepOpen)`：`ensurePanel`→`getAbsoluteRectByAnchor`(左 ROI)→3 页循环{`findTaskLabelInRegion`→命中则 `isTextGlowing`?仅 log:`click`；`!keepOpen`→`closePanel`；return true｜否则 `findImageInRegion(<task>_title.png,STRICT)` 一次→`click(+30,+5,SLOW)` 置 titleClicked continue｜否则 `p<2`→`scroll(anchor,3)`}→`closePanel` return false。
  - `captureCurrentQuestDetailForTaskDirect`(L192)：null/blank→empty；`activateTaskIfPresentDirect(task,keepOpen=true)`→`!activated`→empty；`ensurePanelDirect`→null→empty；`try{ getAbsoluteRectByAnchor(右详情 ROI)→tracker.captureToMemory("quest-detail-image-"+task,rect)→null→empty→saveQuestDetailDebugImage→new QuestDetailCapture(image,latestPath) } finally { closePanelDirect() }`。
- **模型**：`com.bot.dhxy.model.quest.QuestDetailCapture{ BufferedImage image; String imagePath; empty(); hasImage(); }`。

### 2. 逐步业务矩阵（逐字保 HEAD，Cloud/DHXY 职责切分）

Cloud 持编排/CPU 判定；DHXY 只做 exact capture、原子 input bundle、窗口/输入安全门、本地 artifact。逐步：

| 步骤 | HEAD 机械 | Cloud 编排（CPU/判定） | DHXY 机械（retained typed op） |
|---|---|---|---|
| Alt+Q 开面板 | `ensurePanel*`：`findAnchor`→无则 pressAltQ+sleep SLOW→再 findAnchor | 决策是否需要开面板；解析 anchor 绝对点 | `EXECUTE_INPUT_BUNDLE`(pressAltQ + settle SLOW)；`CAPTURE` 全屏/绑定区求 anchor |
| anchor 定位 | `findImageAbsoluteCoordinate(task_fenxiang.png,0.80)` | capture→ImageIO→CloudTemplateAssets(ANCHOR)→ImageFinder.find(…,0.80)→中心点+ROI origin=absolute | `CAPTURE` |
| current-task 页签 | `selectCurrentTaskTab*`：getRandomizedPoint(anchor+(-442,-25),18,5)→clickLeft 100+sleep FAST | 计算 tab 抖动点（纯算术） | `EXECUTE_INPUT_BUNDLE`(clickLeft+settle FAST) |
| 左面板 ROI | `getAbsoluteRectByAnchor(anchor,-497,8,223,295)` | 纯算术（scale/origin 经 WINDOW_FACT） | `WINDOW_FACT`(GEOMETRY) 供 origin/scale |
| 3 页扫描 | `for p<3` | 循环控制、页序 | — |
| label variants | `findTaskLabelInRegion`：`<task>.png/_active/_selected` STRICT 0.85 顺序 | capture(ROI)→逐模板 ImageFinder.find(0.85)→首命中中心点 | `CAPTURE` |
| glow 判定 | `isTextGlowing`：capture(pt±40/±10)→count(RGB 三通道>220)→count>15 | capture→CPU RGB 计数>220、count>15 判定 | `CAPTURE` |
| 命中点击 | 命中且非发光→`click(taskPt,20,5,MID)`；发光→仅 log | 决策 click vs skip | `EXECUTE_INPUT_BUNDLE`(clickLeft+settle MID) |
| title fallback | `findImageInRegion(<task>_title.png,STRICT)` 一次→`click(+30,+5,SLOW)` titleClicked | 一次性 title 匹配+点击决策 | `CAPTURE` + `EXECUTE_INPUT_BUNDLE`(SLOW) |
| scroll | `scroll*`：getRandomizedPoint(anchor+(-400,174),50,100)→moveMouse+sleep FAST+scrollDown(3)+sleep MID | 计算滚动锚点 | `EXECUTE_INPUT_BUNDLE`(move+settle+scrollDown+settle) |
| keepOpen/close | `!keepOpen`→`closePanel(desc)`(pressAltQ)；未命中收尾亦 closePanel | keepOpen 语义、finally close 决策 | `EXECUTE_INPUT_BUNDLE`(pressAltQ) |
| detail capture | exclusive `submitExclusiveAndWait`→activate(keepOpen=true)→ensurePanel→右 ROI captureToMemory→save→`finally closePanelDirect` | 独占边界、右 ROI 计算、返回 image、finally close 编排 | `CAPTURE`(右 ROI，OBSERVED image bytes)；本地 debug PNG artifact（latest+history，WindowScopedTempPath）留 DHXY |
| 空结果语义 | 任一前置失败→`QuestDetailCapture.empty()` | 逐分支 empty 语义逐字保留 | — |

**exclusive 边界**：detail capture 的 `submitExclusiveAndWait` 独占语义对应 Cloud 侧的 per-run 独占编排（R whole-pass exclusive 切片提供，不由本设计新造）。**finally close** 逐字保留（capture 成功/失败/异常都 closePanel）。**ROI/absolute**：所有 offset/阈值/抖动半径/sleep 逐字保 HEAD，SCREEN_ABSOLUTE_PX。

### 3. exact context/authority

- 每次编排绑定 exact `RemoteTaskRunScope(tenant/user/device/clientSession)` + `taskRunId/window/stopEpoch/runRevision`，机械能力**只经** `CloudTaskServicePort` 的 retained typed handle（`WindowFactAction`/`CaptureAction`/`InputBundleAction`），经 `CloudTaskRetainedActionState.invoke` 锁 current handle + `executionGate.validate` gate 当前 revision。
- **禁止复制**（父级明令）：不复制 `GameClientTracker`、`CoordinateHelper` 的 window authority、`InputSequences/InputProvider/InputActionScope`、HWND、`WindowScopedTempPath` temp-path owner 或本地 input queue；这些留 DHXY 侧，Cloud 只经 opaque handle 驱动。
- `getRandomizedPoint`/`getAbsoluteRectByAnchor` 的**纯算术**在 Cloud 内联为纯函数（逐字 HEAD 算式，无 window authority 依赖）；scale/origin 经 `WINDOW_FACT`(GEOMETRY) 与 A 已批准 capture-time `systemScaleRatio` 取得，不自建第二 scale 源。

### 4. retained action identity 与 typed unwind

- canonical `phaseCode=quest`；`actionSlot` per step：`panel-open`(input)、`anchor-probe`(capture)、`current-tab`(input)、`panel-geometry`(window_fact)、`label-probe-p{0..2}`(capture)、`title-probe`(capture)、`glow-probe`(capture)、`label-click`/`title-click`/`scroll`/`panel-close`(input)、`detail-capture`(capture)。occurrence owner = 既有 retained 权威（`CloudTaskRetainedActionState` StableRunKey 跨 revision 复用；occurrence 源承接 M final-consumed frontier，同 TMS 结论，硬前置 M Full R0）。
- **typed unwind**：`UNKNOWN`/`STOPPED`/`NOT_EXECUTED` **绝不压成 task-not-found**（HEAD 的 `return false`/`empty()` 仅表示"确证未找到/未激活"，只有 OBSERVED-miss 有该语义）；机械 UNKNOWN→typed unresolved（复用 W-TMS-0B `MaintenanceUnresolvedException` 同型的 quest unwind，或既有 typed 边界），不自动 retry、不换 ID。
- **pause/resume**：保持 HEAD 同一业务流程（同 anchor/页序/阈值）；旧 revision 构造的 request 经三门反复活合同拒绝，绝不复活（同 TMS/AutoCombat 三门结论）。

### 5. 模板/resource authority

- 模板 classpath 归属 = DHXY `images/template/task/`（实测存在 `task_fenxiang.png`、`<task>.png`/`_active`/`_title`、部分 `_selected`）；Cloud 侧经 **`com.yueyunfe.dhxy.cloudbrain.host.CloudTemplateAssets`**（已存在）按 canonical id 读 `BufferedImage`。
- **deterministic candidate order（消除 cwd 双重权威）**：HEAD `taskLabelTemplatePaths`(L163) 用 `new File(candidate).exists()` 对 **CWD 文件系统**过滤——这是须消除的 cwd 双重权威。Cloud 改为**固定 candidate 顺序** `<task>.png → <task>_active.png → <task>_selected.png`，存在性由 CloudTemplateAssets 的 classpath 资源目录唯一裁定（缺失的 candidate 直接跳过，全缺则回落首个 canonical id，与 HEAD `List.of(candidates.get(0))` 语义等价），**绝不查 cwd**。`<task>_title.png` 同经 CloudTemplateAssets。
- **发光 CPU 判定保留**：`RGB>220`（三通道均 `>GLOW_RGB_MIN=220`）、`count>15`（`>GLOW_TARGET=15`）逐字保留，在 Cloud CPU 对 OBSERVED probe image 执行。匹配阈值 `STRICT=0.85`/`NORMAL=0.80` 逐字保留。

### 6. 依赖 DAG + 最小叶子波次 + 文件表 + 双构建门

**依赖 DAG**（→=前置）：
```
既有 CloudTaskServicePort/retained 权威 + CloudTemplateAssets（已存在）+ A 已批准 capture-time systemScaleRatio（已 APPROVED）
  → [叶子波 W-QM-0] 纯 DTO/model 叶子：Cloud CloudQuestDetailCapture（image bytes+artifact path，empty 语义）
  → [W-QM-1] Cloud QuestManagerService（整类编排：panel/scroll/label/title/glow/detail，经 retained handle + CloudTemplateAssets + 纯算术 ROI；typed unwind）
      ├ 硬前置 M Full R0（occurrence 源）
      └ 硬前置 trusted activation caller / R whole-pass exclusive（detail capture 独占边界）
  → [W-QM-2 caller cohort，非-A] cloud XiuluoTaskV2:6238 改经 Cloud QuestManagerService；DHXY 本地 artifact(debug PNG) 执行保留
禁止修改：P Full R0、R whole-pass exclusive、B warning（本设计零触碰）。
```

**最小可独立编译、无 host/caller 的叶子波 W-QM-0**（建议首个）：Cloud New `com.bot.dhxy.model.quest.CloudQuestDetailCapture`（immutable：OBSERVED image bytes + 可选 artifact path + empty()/hasImage()，与 HEAD QuestDetailCapture 语义同构，无 host/caller/port 依赖，可独立 javac/编译）。

**精确 New/Modify 文件表**：

| # | 仓库 | 精确 FQCN / 路径 | New/Modify | 前置门 / 归属 |
|---|---|---|---|---|
| 1 | Cloud | `com.bot.dhxy.model.quest.CloudQuestDetailCapture` | New（叶子 W-QM-0） | 本设计批准；Worker A |
| 2 | Cloud | `com.bot.dhxy.service.QuestManagerService`（整类编排，dormant） | New（W-QM-1） | 门=行1 + M Full R0 + activation/exclusive 前置 + 父级授权；Worker A |
| 3 | Cloud | quest 模板资源（若 CloudTemplateAssets 缺 `task/` 资源）→ 由资源切片补 | New/资源 | **非-A 或独立门**：以 CloudTemplateAssets 资源归属为准，冲突先报告 |
| — | Cloud | `CloudTaskServicePort`/retained 权威/`CloudTemplateAssets` | 零修改 | 既有，冻结 |
| — | DHXY | exact capture / input bundle / 安全门 / 本地 debug PNG artifact（WindowScopedTempPath） | 保留/非-A | DHXY 本地，caller cohort |
| — | — | XiuluoTaskV2:6238 caller 接线 | Modify | **非-A**：W-QM-2 caller cohort |

**双构建门**：W-QM-1 落地后（父级授权、并发写入稳定）执行 Cloud `mvn -q clean package`（不 skip）+ DHXY `mvn -q -DskipTests compile`；不新增/恢复测试，不启动运行面。W-QM-0 叶子可先单独编译验证。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design #1 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`；heartbeat 保持。

## Parent Design Review #1 - BLOCKED / Repair #1 Published - 2026-07-13T04:34:00-04:00

父级已对照 DHXY HEAD `0114604e` 的 `QuestManagerService.java:79-390`、唯一生产 caller
`XiuluoTaskV2.java:6248-6304`，以及当前 Cloud `CaptureOutcome`、`CloudTaskServicePort`、
`CloudTaskRetainedActionState.ActionAddress`、`CloudTemplateAssets` 与已打包 task 资源逐项复核。inventory、三页顺序、
阈值、template candidate 顺序和两种入口 lane 的识别基本正确，但当前设计仍不能直接实施。结论：
**BLOCKED，P0=0 / P1=4 / P2=1**。

1. **P1：anchor/ROI 坐标没有绑定同一次 capture，且把 edge rectangle 直接映射到 width/height wire 的规则缺失。**
   HEAD `findImageAbsoluteCoordinate` 用同一 full-frame 的 image-local 命中、`systemScaleRatio` 与窗口 base 得到绝对点；当前
   `CaptureOutcome` 已同时带 `systemScaleRatio` 和 `observedWindow`。Design #1 却另发 `panel-geometry` WINDOW_FACT，可能把另一时刻的
   geometry 与 capture 混用，并新增 HEAD 不存在的机械读取。另一方面 HEAD 左/右 ROI 是 `[left,top,right,bottom]`，remote
   `CaptureRegion` 是 `(x,y,width,height)`，glow 也是 `(pt.x-40,pt.y-10)-(pt.x+40,pt.y+10)`。返修必须固定：anchor 只用
   **同一 OBSERVED capture** 的 `observedWindow + systemScaleRatio` 换算；禁止另取 geometry 拼帧；明确左 ROI `223x295`、detail
   `264x50`、glow `80x20` 的 exact wire 值与 image-local 命中回到 SCREEN_ABSOLUTE_PX 的公式。
2. **P1：retained address 没有为同一流程内的 fresh probes 建立唯一 occurrence。** 现有 action address 是
   `(phaseCode,actionSlot,occurrence)`，首次 request bytes 绑定后不可替换。单一 `anchor-probe` 会同时覆盖开面板前探针、Alt+Q 后探针、
   detail 阶段第二次 `ensurePanel`；单一 `scroll` 也覆盖 p0/p1 两次滚动。复用会重交旧截图/旧输入，而不是 HEAD 的 fresh read。
   返修须列出每个 public invocation 的 retained workflow occurrence，以及至少 `anchor-before-open`、`anchor-after-open`、
   `anchor-detail-recheck`、`current-tab-*`、`scroll-p0/p1`、每页 label/title/glow/click/close 的唯一 address；重复调用 public API 也必须
   由 Full R0 owner 单调分配新 workflow occurrence，UNKNOWN 不前进、同 bytes 重投不重新随机。
3. **P1：detail result 与本地 PNG artifact 没有端到端 producer。** Cloud 当前已经存在原名
   `com.bot.dhxy.model.quest.QuestDetailCapture`（与 HEAD 同型），所以新建 `CloudQuestDetailCapture` 会形成第二模型且没有真实 caller。
   当前 `CaptureOutcome` 只有 image bytes/hash/size/provider/scale/window，没有 local debug path；generic CAPTURE handler 也不会执行 HEAD
   `latest + history` 两次 `WindowScopedTempPath` 写盘。Design #1 的“可选 artifact path”因此无来源，还会把本地绝对路径误当 Cloud
   数据权威。返修必须选择一个闭合方案：复用现有模型或定义真正必要的 typed result，但只能有一个 owner；同时给出 DHXY 本地
   artifact 副作用的 typed request/handler、失败仍返回 image 的语义、Cloud 只拿 opaque artifact reference/diagnostic text（不得拿可操作
   本地路径）的精确 wire 与文件写集。若决定不回传路径，也要说明如何保持 HEAD 两次写盘和 caller 日志含义。
4. **P1：close/finally 与 exclusive 范围被概括成了比 HEAD 更宽的合同。** HEAD 仅在 activation 成功且第二次
   `ensurePanelDirect` 成功后进入 `try/finally`；activation miss 由自身 not-found close 收口，第二次 anchor 为空则直接 empty，普通
   `activateTaskIfPresent` 仍是多个 sequenced bundles，只有 detail API 是 whole-pass exclusive。返修须给逐分支 close matrix，明确每个
   `Alt+Q` 的 exact slot、是否已开面板、哪条失败路径 close/不 close；R whole-pass exclusive 只服务 detail API，不能把普通 activation
   扩成整段独占。
5. **P2：资源 inventory 的“若缺则后补”不再准确。** 当前 Cloud
   `src/main/resources/images/template/task/` 已有 `task_fenxiang/xiuluo/wuhuan/wubei` 相关资源，且与 DHXY 根目录
   `images/template/task/` 对应文件 bytes/SHA256 一致；`CloudTemplateAssets` 是 public canonical-id API但不提供 enumeration。Repair
   应把本切片资源写集固定为零，逐 candidate 调 `loadTemplate` 判存在，列出当前 Xiuluo 必需资源及 hash；不要留下条件式资源 owner。

### 下一任务：`W-QM-D2`（Design Repair #1 Delta）

External Worker A 必须在 `2026-07-13T04:55:00-04:00` 前追加
`## External Worker A - CLAIMED - <timestamp>`，写明任务标题、领取时间与唯一写集=仅本固定日志。20 分钟只检查领取，领取后可工作
超过 20 分钟。只追加一个短 Repair Delta 逐项关闭上述 P1×4/P2×1；Java/resources/tests/schema/Maven/runtime 全冻结，不得修改
A/B/P/R/host/caller 写集。Worker 自审只算 QA，完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T04:37:58-04:00

领取父级任务 **W-QM-D2**（Design Repair #1 Delta，闭合 Parent Design Review #1 的 P1×4+P2×1）。唯一写集 = **仅本固定日志 append-only**（设计/源码核对，**Java/resources/tests/schema/Maven/runtime 全冻结**，不改 A/B/P/R/host/caller 写集）。领取截止 2026-07-13T04:55:00-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：据 HEAD QuestManagerService + XiuluoTaskV2:6248-6304 + 真实 CaptureOutcome/CaptureRegion/CloudTemplateAssets/既有 Cloud QuestDetailCapture + 已打包 task 资源，追加短 `Design Repair #1 Delta`：P1-1 anchor/ROI 只用同一 OBSERVED capture 的 observedWindow+systemScaleRatio 换算(左 223x295/detail 264x50/glow 80x20 exact wire + image-local→SCREEN_ABSOLUTE_PX 公式，禁另取 geometry)；P1-2 逐 public invocation 唯一 workflow occurrence + anchor-before-open/after-open/detail-recheck/current-tab/scroll-p0/p1/每页 label/title/glow/click/close 唯一 address，重复调用单调新 occurrence，UNKNOWN 不前进/同 bytes 不重随机；P1-3 复用既有 QuestDetailCapture 单 owner + DHXY 本地 artifact typed request/handler(latest+history)、失败仍返 image、Cloud 只拿 opaque reference/diagnostic 不拿可操作路径；P1-4 逐分支 close matrix + exclusive 只服务 detail API；P2 资源写集=零、逐 candidate loadTemplate、列 Xiuluo 必需资源+hash。批准前继续冻结。

## External Worker A - Design Repair #1 Delta - 2026-07-13

关闭 `Parent Design Review #1` 的 P1×4+P2×1。仅设计，Java/resources/tests/schema/Maven/runtime 全冻结，不改 A/B/P/R/host/caller 写集。Design #1 已成立部分（inventory、三页序、阈值、candidate 顺序、两 lane 识别）不重写。取证：HEAD `CoordinateHelper.findImageAbsoluteCoordinate`(L136-151)/`getAbsoluteRectByAnchor`(L167-174)、`CaptureRegion(space,x,y,width,height)`、`CloudTemplateAssets.loadTemplate(TemplateId):Optional`、既有 Cloud `com.bot.dhxy.model.quest.QuestDetailCapture`、`XiuluoTaskV2.parseTaskPanelObjective(QuestDetailCapture,String)`(:6248-6304)。

### R1（P1-1）：anchor/ROI 绑定同一 OBSERVED capture；统一 WINDOW_CLIENT_PX；exact wire + 公式

**撤回** Design #1 的独立 `panel-geometry` WINDOW_FACT（会拼另一时刻 geometry）。HEAD 真实公式：`findImageAbsoluteCoordinate` = `abs = round(imageLocal / systemScaleRatio) + windowBase`（L144-145）；`getAbsoluteRectByAnchor` = `[anchor+offset, +w, +h]`（L169-173，offset/宽高不缩放）。据此：

- **全部 quest ROI/点统一用 `WINDOW_CLIENT_PX`**（schema §5.1「WINDOW_CLIENT_PX 由本地已核验 binding 唯一换算」）——Cloud 不再取 screen 原点、不发 geometry；image-local→点的换算只用**同一 OBSERVED capture** 的 `systemScaleRatio`，client→screen 由 DHXY 输入/截图时以 bound window 唯一解析（等价 HEAD 的 `+windowBase`）。`observedWindow` 仅作四字段 correlation（无 origin/scale），scale 只来自该 capture 的 `systemScaleRatio`。
- **anchor**：capture 整窗 client（`CaptureRegion(WINDOW_CLIENT_PX, 0, 0, clientW, clientH)`）→ OBSERVED → `anchorClient = ( round(localX / systemScaleRatio), round(localY / systemScaleRatio) )`（同一帧 scale）。
- **exact wire ROI**（`CaptureRegion(WINDOW_CLIENT_PX, x, y, w, h)`，w/h 为 HEAD 逻辑 px）：
  - 左面板：`x=anchorClient.x-497, y=anchorClient.y+8, w=223, h=295`（HEAD `[-497,8,223,295]`，`[left,top,right,bottom]`→`(x,y,width=right-left,height=bottom-top)`）。
  - 详情：`x=anchorClient.x-269, y=anchorClient.y+12, w=264, h=50`。
  - glow：HEAD `(pt.x-40,pt.y-10)-(pt.x+40,pt.y+10)` → `x=ptClient.x-40, y=ptClient.y-10, w=80, h=20`。
- **ROI 内命中回 client-point**：`pointClient = ( ROI.x + round(localX / scale), ROI.y + round(localY / scale) )`；click/scroll 抖动点（`getRandomizedPoint` 半径 20/5、18/5、50/100）在 client 空间纯算术；DHXY input 时 client→screen 唯一解析。绝不另取 geometry 拼帧。

### R2（P1-2）：每次 fresh probe 唯一 occurrence + address

- **workflow occurrence N** = 每个 public invocation（`activateTaskIfPresent` 或 `captureCurrentQuestDetailForTask`）由 **M Full R0 frontier owner 单调分配**（硬前置 M Full R0）；重复调用 public API → N+1（新 workflow，fresh 读全新 address）；UNKNOWN **不前进** N；同 bytes 重投复用同 retained request、**不重新随机**抖动点。
- **同一 occurrence N 内每 fresh probe/input 唯一 `ActionAddress(quest, <actionSlot>, N)`**（actionSlot 互异，杜绝复用旧截图/旧输入）：

| 阶段 | actionSlot | op |
|---|---|---|
| ensurePanel①开前探针 | `anchor-before-open` | CAPTURE |
| Alt+Q 开面板 | `anchor-open-altq` | INPUT |
| ensurePanel①开后探针 | `anchor-after-open` | CAPTURE |
| 当前任务页签 | `current-tab-click` | INPUT |
| 每页 p∈{0,1,2} 面板探针（左 ROI 单次 fresh capture，label candidates+title 于 Cloud CPU 顺序匹配同帧） | `panel-probe-p{p}` | CAPTURE |
| 每页命中后发光探针 | `glow-probe-p{p}` | CAPTURE |
| 每页 label 命中点击 | `label-click-p{p}` | INPUT |
| 每页 title fallback 点击 | `title-click-p{p}` | INPUT |
| 翻页滚动（仅 p0/p1 后） | `scroll-p{0,1}` | INPUT |
| 关面板 | `panel-close` | INPUT |
| detail：第二次 ensurePanel 复查探针 | `anchor-detail-recheck` (+`anchor-detail-altq`/`anchor-detail-after` 若需开面板) | CAPTURE/INPUT |
| detail：右详情截图 | `detail-capture` | CAPTURE |
| detail：finally 关面板 | `detail-close` | INPUT |

（detail public call 的 occurrence 覆盖其内 activate 子流程 + recheck + capture + close，全在同一 N 下用上述互异 slot；detail 内的 activate 子流程复用 `anchor-before-open…panel-close` 同名 slot，因同属该 detail 的 occurrence N 不冲突。）

### R3（P1-3）：复用既有 QuestDetailCapture 单 owner + DHXY 本地 artifact typed 边界

- **撤回** 新建 `CloudQuestDetailCapture`（Cloud 已存在同型 `com.bot.dhxy.model.quest.QuestDetailCapture`，会成第二模型且无 caller）。**唯一 result owner = 既有 `QuestDetailCapture`**（image + imagePath）；caller `XiuluoTaskV2.parseTaskPanelObjective` 只用 `capture.image()` 做 cloud OCR reader，path 仅 cleanup/debug。
- **image 来源**：`detail-capture` 的 OBSERVED `CaptureOutcome.imageBytes` → 解码为 `QuestDetailCapture.image`。**失败仍返回 image 语义**：任一前置失败/非 OBSERVED → `QuestDetailCapture.empty()`（逐字保 HEAD 各 empty 分支）。
- **本地 PNG artifact**：HEAD `saveQuestDetailDebugImage` 的 `latest + history` 两次 `WindowScopedTempPath` 写盘是 **DHXY 本地 artifact 副作用**，保留在 DHXY 侧（父级：DHXY 保留本地 artifact 执行）。**Cloud 不拿可操作本地路径**——若回传，仅 opaque artifact reference / diagnostic text（不可用于本地文件操作）。
  - **精确闭合方案（择一，本 Delta 采 A）**：
    - **方案 A（推荐，wire 零新增）**：debug PNG 写盘由 DHXY capture handler 在返回 OBSERVED image bytes 的同时作 side-effect 执行（latest+history、WindowScopedTempPath owner 全留 DHXY）；`QuestDetailCapture.imagePath` 在 Cloud 侧置为**空/opaque diagnostic**（caller 只依赖 image，path 空时日志含义=「本地已写、Cloud 不引用路径」，保持 HEAD 两次写盘不变）。**不回传可操作路径、不新增 wire 字段**。
    - 方案 B（若父级要求路径回显）：新增 typed opaque `artifactRef`（非文件路径）随 outcome 回传，须精确 wire + 文件写集；本 Delta 不选。
- 采方案 A → 本切片**无新 result 模型、无新 wire 字段**；DHXY 本地 artifact handler 归 DHXY caller cohort（非-A）。

### R4（P1-4）：逐分支 close matrix + exclusive 只服务 detail API

**撤回** Design #1 过宽的独占/close 概括。逐字对 HEAD：

| 入口/分支 | HEAD 行为 | close/slot |
|---|---|---|
| `activateTaskIfPresent`（普通，sequenced，**非独占**） | 多个 sequenced bundles | 命中且 `!keepOpen`→`panel-close`("quest:activateClose")；命中且 `keepOpen`→**不 close**；三页未命中→`panel-close`("quest:activateNotFoundClose") |
| activation 未开到面板（anchor null） | `ensurePanel` 返回 null → 直接 `return false` | **不进 try/finally、不 close**（面板未开） |
| `captureCurrentQuestDetailForTask`（**whole-pass exclusive，仅此**） | `submitExclusiveAndWait` 独占整段 | 见下 |
| detail：activation miss（`!activated`） | 直接 `empty()` | activation 自身的 not-found close 已收口；**detail 不再 close** |
| detail：第二次 `ensurePanelDirect` 返回 null | 直接 `empty()` | **不进 try/finally、不 close** |
| detail：activation 成功 **且** 第二次 anchor 成功 | 进 `try{ capture }finally{ closePanelDirect }` | 无论 capture 成功/null/异常→`detail-close`(finally) |

- **exclusive 边界**：`R whole-pass exclusive` **只包 detail API**（`captureCurrentQuestDetailForTask`）；普通 `activateTaskIfPresent` 保持多 sequenced bundles，**不扩成整段独占**。keepOpen 语义逐字：仅命中且 keepOpen 才不 close。

### R5（P2）：资源写集=零 + 逐 candidate loadTemplate + Xiuluo 必需资源 hash

- **撤回** Design #1「若缺则后补」。Cloud `src/main/resources/images/template/task/` 已打包所需资源且与 DHXY 根目录 bytes/SHA256 一致；**本切片资源写集固定为零**。
- `CloudTemplateAssets.loadTemplate(TemplateId)` 为 public canonical-id API、**无 enumeration**：逐 candidate 顺序 `<task>.png → <task>_active.png → <task>_selected.png` 调 `loadTemplate`，`Optional.empty()`=该 candidate 缺失即跳过（等价 HEAD 存在性过滤，但**绝不查 cwd 文件系统**）；全缺回落首 canonical id（等价 HEAD `List.of(candidates.get(0))`）。`<task>_title.png`、`task_fenxiang.png`(anchor) 同经 loadTemplate。
- **Xiuluo（`TASK_CODE`）必需资源 + SHA256(前16)**（Cloud=DHXY 实测一致）：`task_fenxiang.png`=2f31d961b427b6c1、`xiuluo.png`=c465f8a0759a2269、`xiuluo_active.png`=6c477bef711b40b4、`xiuluo_title.png`=ad9848d521260dae；`xiuluo_selected.png`=**两仓皆 ABSENT**（candidate 顺序自然跳过）。无条件式资源 owner。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #1 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #2 - PARTIAL PASS / Repair #2 Published - 2026-07-13T04:53:00-04:00

父级再次对照 HEAD `QuestManagerService` 与 `CoordinateHelper.findImageInRegion`、现有 `CaptureRequest` 复审。D2 的同帧
scale/window 坐标、ROI width/height、资源零写集和普通/exclusive 主边界通过且不得重开；但仍有四个会改变 HEAD 或无法生产数据的缺口。
结论：**BLOCKED，P0=0 / P1=4 / P2=0**。

1. **P1：D2 把 HEAD 的多次 fresh capture 合并成一帧，改变模板判断时序。** HEAD 每次调用
   `CoordinateHelper.findImageInRegion` 都重新 `captureToFile`：每个 label candidate 各一帧，label 全 miss 后 title 又是一帧；不是
   `panel-probe-p` 一帧同时匹配 label/title。AGENTS 基线门禁止减少 read/verification。Repair 必须按 candidate 顺序给
   `label-probe-p{p}-base/active/selected` 与 `title-probe-p{p}` 独立 retained CAPTURE address（缺失模板仍按 HEAD 文件存在性过滤），
   逐 outcome 决定是否继续下一次 capture；不得批量同帧优化。glow 仍是命中后独立 fresh capture。
2. **P1：generic CAPTURE 无法凭空执行 task-specific latest+history artifact。** 当前 `CaptureRequest` 只有 region/imageFormat 与
   `DIAGNOSTIC|CLOUD_SERVICE_INPUT` purpose，没有 task code、artifact intent、safe filename 或 latest/history 合同。D2 方案 A 声称 handler
   自动写两份图，却未给可识别的 typed request/producer，仍不可实现。Repair 必须定义最小 typed artifact intent（可为 capture spec 的
   digest-covered closed field/variant），只允许 canonical quest-detail kind + canonical task code；DHXY handler 在返回 image 前后按 HEAD
   fail-soft 写 latest+timestamp history，写失败仍返回 image。Cloud 只能收到 opaque diagnostic/ref 或空，不得收到可操作本地路径；列出
   双仓 wire/digest/schema/handler 精确写集，或给出另一个真实可达且同等安全的 typed local capability。
3. **P1：Full R0 frontier 不是跨 actionSlot 的 workflow invocation owner。** Full R0 occurrence 是每个 semantic slot 的 no-gap
   attempt frontier，不能自动保证十几个不同 actionSlot 共用同一个 invocation `N`。Repair 必须新增/复用一个 package-private retained
   `QuestManagerWorkflowState` owner：按 exact run + public API invocation 稳定分配 workflowId，保存 current step/page/titleClicked/
   close state；每个 step 再从 Full R0 mint 自己 slot occurrence。UNKNOWN 保留同 workflow/step/exact bytes，只有 terminal consumed 才前进；
   重复 public 调用必须创建新 workflowId，不能靠各 slot 独立碰巧同 N。
4. **P1：direct failure/close matrix 仍少了 HEAD 的可见分支。** `ensurePanelDirect` 在 anchor 已存在但
   `selectCurrentTaskTabDirect` checkpoint/sleep 失败时返回 null，面板可能已打开且 HEAD **不 close**；`clickDirect`/`scrollDirect` 的 false
   又被 `activateTaskIfPresentDirect` 忽略，流程仍按命中/下一页继续；`closePanelDirect` 自身 checkpoint false 时也会跳过 Alt+Q。
   Repair 必须把这些返回值是否被调用方消费逐项列入状态机，不能把 mechanical NOT_EXECUTED 自动升级成 HEAD 不存在的业务失败/cleanup。

### 下一任务：`W-QM-D3`（Design Repair #2 Delta）

External Worker A 必须在 `2026-07-13T05:13:00-04:00` 前追加 CLAIMED，唯一写集仍为本日志。只关闭上述 P1×4；D2 已通过的
坐标/ROI/resource/exclusive 主边界冻结。Java/resources/tests/schema/Maven/runtime 全冻结，不修改 P Full R0/B/S/host/caller；完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T04:58:29-04:00

领取父级任务 **W-QM-D3**（Design Repair #2 Delta，闭合 Parent Design Review #2 的 P1×4）。唯一写集 = **仅本固定日志 append-only**（设计/源码核对，**Java/resources/tests/schema/Maven/runtime 全冻结**，不改 P Full R0/B/S/host/caller）。领取截止 2026-07-13T05:13:00-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：据 HEAD Direct lane（ensurePanelDirect/selectCurrentTaskTabDirect/clickDirect/scrollDirect/closePanelDirect/activateTaskIfPresentDirect）+ findImageInRegion 每次 fresh capture + 真实 CaptureRequest/CapturePurpose，追加短 Design Repair #2 Delta：S1 撤回同帧合并，按 candidate 顺序给 label-probe-p{p}-base/active/selected + title-probe-p{p} + glow-probe-p{p} 各独立 fresh CAPTURE address，逐 outcome 决定是否续拍，不批量优化；S2 定义最小 typed artifact intent(capture spec digest-covered closed field/variant，canonical quest-detail kind+canonical task code)，DHXY handler fail-soft 写 latest+history、写失败仍返 image，Cloud 只收 opaque ref/空、不收可操作路径，列双仓 wire/digest/schema/handler 写集；S3 新增 package-private retained QuestManagerWorkflowState owner(per run+public invocation 稳定 workflowId，存 step/page/titleClicked/close，每 step 从 Full R0 mint 自己 slot occurrence，UNKNOWN 保持同 workflow/step/bytes，重复调用新 workflowId)；S4 逐项 Direct return-value 消费矩阵(ensurePanelDirect anchor 存在但 selectCurrentTaskTabDirect 失败返 null 且不 close；clickDirect/scrollDirect false 被 activate 忽略续流程；closePanelDirect checkpoint false 跳过 AltQ)，不把 mechanical NOT_EXECUTED 升级成 HEAD 不存在的业务失败/cleanup。D2 已通过坐标/ROI/resource/exclusive 主边界不重开。批准前继续冻结。

## External Worker A - Design Repair #2 Delta - 2026-07-13

关闭 `Parent Design Review #2` 的 P1×4。仅设计，Java/resources/tests/schema/Maven/runtime 全冻结，不改 P Full R0/B/S/host/caller。D2 已通过部分（同帧 scale/window 坐标、ROI width/height、资源零写集、普通/exclusive 主边界）冻结不重开。取证：HEAD `CoordinateHelper.findImageInRegion`(L193-)每次 `captureToFile("ROI-Scan",…)` 重新截图；`CaptureRequest(context,captureId,region,imageFormat,capturePurpose)`，`CapturePurpose{DIAGNOSTIC,CLOUD_SERVICE_INPUT}`（无 task code）；Direct lane L272-386。

### S1（P1-1）：撤回同帧合并，逐 candidate 独立 fresh CAPTURE（保 HEAD read 数）

**撤回** D2 的 `panel-probe-p{p}` 单帧同匹配 label/title。HEAD `findImageInRegion` 每次调用都 `captureToFile` 重新截 ROI，故每个 candidate/title 各一帧。AGENTS 基线禁止减少 read/verification。改为逐 candidate 独立 retained CAPTURE address（同一左 ROI，逐次 fresh），**逐 outcome 决定是否续拍**：

| 页 p / 步骤 | actionSlot（各独立 fresh CAPTURE 左 ROI） | 续拍规则 |
|---|---|---|
| label candidate `<task>.png` | `label-probe-p{p}-base` | Cloud 匹配 base；OBSERVED-hit→停(去 glow/click)；OBSERVED-miss→续下一 candidate |
| label candidate `<task>_active.png` | `label-probe-p{p}-active` | 同上（仅存在性过滤后存在的 candidate 才拍） |
| label candidate `<task>_selected.png` | `label-probe-p{p}-selected` | 同上 |
| title `<task>_title.png`（label 全 miss 后，titleClicked gate 一次） | `title-probe-p{p}` | 独立 fresh CAPTURE 左 ROI；hit→click；miss→scroll(p<2) |
| 命中后发光 | `glow-probe-p{p}` | **命中后独立 fresh CAPTURE**(glow 80x20 ROI)，绝不复用 label 帧 |

- candidate 缺失（R5 loadTemplate empty / HEAD 文件存在性过滤）→该 candidate **不发 capture、跳过**；不批量同帧优化、不减读。每个 address 首帧 bytes 绑定后 UNKNOWN 同 bytes 重投（S3），不换帧。

### S2（P1-2）：最小 typed artifact intent（真实可达 producer）+ fail-soft latest/history

**撤回** D2 方案 A「handler 自动写两份图但无 typed 触发」。定义最小 typed artifact intent，作为 **capture spec 的 digest-covered closed 字段/variant**：

- **wire（Cloud `CaptureRequest` + DHXY 镜像）新增闭合可选字段** `questDetailArtifact: { kind: QUEST_DETAIL, taskCode: <canonical non-blank> } | 键缺失`：
  - NON_NULL canonical：**键缺失=普通 capture**（所有非 quest-detail capture 的 bytes/`requestDigest` 逐字不变，仅 `detail-capture` address 携带）；出现时 `kind` 仅允许 canonical `QUEST_DETAIL`、`taskCode` 为该 run 的 canonical 非空 task code。
  - **digest-covered**：出现时并入 `requestDigest`（对象内 canonical string/enum，走既有 canonicalizer，无新 floating）。
- **DHXY capture handler**（`LocalRemoteGameCommandHandler.executeCapture`）：当 request 携带 `questDetailArtifact` 时，在返回 OBSERVED image 前后按 HEAD **fail-soft** 写 `quest_detail_<safeTask>.png`(latest) + `quest_detail_<safeTask>_<timestamp>.png`(history)（`WindowScopedTempPath` owner 留 DHXY，safeTask 由 canonical taskCode 生成安全文件名，逐字保 HEAD `saveQuestDetailDebugImage`）；**写失败仍返回 image**（HEAD fail-soft 语义）。
- **Cloud 侧**：outcome **不含可操作本地路径**；`CaptureOutcome` schema 不加 path 字段——Cloud 只拿 image bytes（+ 现有 scale/window/hash）；`QuestDetailCapture.imagePath` 在 Cloud 侧置**空/opaque diagnostic**（非本地路径，caller 只依赖 `image()`）。artifact 写入仅经 DHXY handler log 确认。
- **精确双仓写集（W-QM-1 实施期，本 Delta 仅设计）**：Cloud `CaptureRequest`（+闭合字段+ `ArtifactKind` enum）、DHXY `RemoteCaptureCommandPayload`（镜像字段）、DHXY `RemoteOperationPayloadCodec`（该闭合字段 strict schema：缺失/存在/canonical 校验）、DHXY `LocalRemoteGameCommandHandler.executeCapture`（intent→fail-soft latest+history）、两仓 digest 无需改（对象字段走既有 canonicalizer）、schema §5.1（新增 closed optional `questDetailArtifact` 定义与 digest 规则）。非 quest capture 零变化。

### S3（P1-3）：新增 retained `QuestManagerWorkflowState` invocation owner（撤回“共用 N”）

**撤回** D2「workflow occurrence 由 M Full R0 单调分配、十几 slot 共用同一 N」。Full R0 frontier 是**每 semantic slot 的 no-gap attempt frontier**，不跨 slot 保证同 invocation。新增：

- **FQCN（New，package-private）**：`com.yueyunfe.dhxy.cloudbrain.remote.QuestManagerWorkflowState`，per exact run（绑定 scope/taskRunId/window/stopEpoch/runRevision，随 run execution context 生死）。
- **职责**：按 **exact run + 每次 public API invocation** 稳定分配单调 `workflowId`；保存该 invocation 的 `currentStep/page/titleClicked/closeState`。**workflowId 是 caller-owned workflow 身份，不是共用 occurrence 数**。
- **每 step 从 Full R0 各自 mint 自己的 slot occurrence**（per-slot no-gap frontier，S1 各 actionSlot 独立）；workflowId 只做 step 归属与状态机推进，不替代 slot occurrence。
- **方法签名（建议最小）**：`WorkflowHandle beginInvocation(RemoteTaskRunAuthorization auth, QuestPublicApi api)`（api∈{ACTIVATE, CAPTURE_DETAIL}）；`QuestStep currentStep(WorkflowHandle)`；`void advance(WorkflowHandle, QuestStep consumedTerminal)`；`void retire(WorkflowHandle)`。
- **UNKNOWN 语义**：保持**同 workflow/step/exact bytes**（复用该 step 的 retained request，不重新随机抖动点、不换 ID）；**仅 terminal consumed 才前进 step**。
- **重复 public 调用**：必须 `beginInvocation` 铸**新 workflowId**（新 workflow，全新 step 序列与 slot occurrence），**绝不靠各 slot 碰巧同 N**。
- 硬前置：Full R0（slot occurrence 源）+ 本 owner 的 assembly 装配点（非-A，随 activation owner）。

### S4（P1-4）：Direct lane return-value 逐项消费矩阵（不升级 mechanical NOT_EXECUTED）

据 HEAD Direct lane 逐行，明确每个 mechanical 返回值**是否被调用方消费**；mechanical `NOT_EXECUTED/UNKNOWN` **绝不**自动升级成 HEAD 不存在的业务失败/cleanup：

| HEAD 点（行） | mechanical 结果 | 调用方是否消费 | Cloud typed 映射 |
|---|---|---|---|
| `ensurePanelDirect`：anchor 存在但 `selectCurrentTaskTabDirect` 返 false（checkpoint/sleep 失败，L284） | tab click 未完成 | **消费** → ensurePanelDirect 返 null | 返 null-equiv；**面板可能已开、HEAD 不 close → 不 close、不 cleanup** |
| `ensurePanelDirect`：开面板阶段 checkpoint/sleep false（L275/279） | AltQ/sleep 未完成 | **消费** → 返 null | 返 null-equiv，不 close |
| `activateTaskIfPresentDirect`：`clickDirect(...)` 返 false（L131 label 命中点击） | click 未完成 | **不消费**（返回值未检查）→ 仍走 `!keepOpen closePanelDirect` + `return true` | mechanical NOT_EXECUTED **不改流程**，逐字保 HEAD（不 abort、不额外 cleanup） |
| `activateTaskIfPresentDirect`：`clickDirect(...)` 返 false（L139 title 点击） | click 未完成 | **不消费** → titleClicked=true, continue | 同上 |
| `activateTaskIfPresentDirect`：`scrollDirect(...)` 返 false（L144） | scroll 未完成 | **不消费** → 下一页 continue | 同上 |
| `closePanelDirect`：自身 checkpoint false（L382） | 跳过 AltQ（未 close） | 自身 return void | close 未执行即 **NOT_EXECUTED，不升级**、不重试 |
| `ensurePanelDirect` anchor 全程 null | 未找到面板 | **消费** → `activateTaskIfPresentDirect` 返 false（**无 close**，L118） | OBSERVED-miss 语义（未找到），非 mechanical 失败 |

原则：只有 HEAD **实际消费**的返回值改变流程（tab 失败→null、anchor null→false）；HEAD **忽略**的 mechanical 结果（click/scroll false、close checkpoint false）在 Cloud 侧同样**不改流程、不 cleanup、不 task-not-found**；typed UNKNOWN/STOPPED 仅在 HEAD 本就 stop 的边界走 typed stop unwind。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #2 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #3 - PARTIAL PASS / Repair #3 Published - 2026-07-13T05:04:57-04:00

父级对照 DHXY HEAD `QuestManagerService.java:79-390`、当前两仓 capture wire 与 retained port 链复审。S1 已恢复每个
candidate/title/glow 的独立 fresh capture；S4 已逐项保持 Direct lane 对 false/null/close 的真实消费方式。这两部分通过且不得重开。
typed artifact intent 与 workflow owner 方向也成立，但当前文件/DAG和身份合同仍不足以实施。结论：
**BLOCKED，P0=0 / P1=4 / P2=1**。

1. **P1：typed artifact intent 的跨仓构造/封装链写集不完整，当前方案会在 Cloud 重建时丢字段。** D3:326 称“两仓 digest
   无需改”，但 Cloud `RemoteProtocolDigests.withComputedRequestDigest(CaptureRequest)` 在
   `RemoteProtocolDigests.java:39-41` 明确逐参数 `new CaptureRequest(...)`；不修改它会在算完 digest 后把新 intent 丢回 null。
   `RemoteCommandEnvelope.java:68-72` 也手工写 capture payload，不修改则 wire 根本不带字段。所有 capture request 还经
   `CloudTaskServicePort.capture` -> `CloudTaskRunCommandExecutor.capture` -> `CloudTaskRunExecutionGate.newCaptureRequest`
   （normal/observation 两处）构造并在 executor:116-120 做 retained spec equality；这些层不显式携带/比较 intent，就会出现 digest、
   retained bytes 与调用者语义分叉。Repair 必须列全并固定 exact API：至少 Cloud `CaptureRequest`、artifact intent/enum、
   `RemoteProtocolDigests`（只修 record 重建，canonical 算法不改）、`RemoteCommandEnvelope`、`CloudTaskRunExecutionGate`、
   `CloudTaskRunCommandExecutor`、`CloudTaskServicePort`，以及若 public delegate 保持同一签名则 `RemoteGameClientPort`；DHXY
   payload/codec/handler/schema 保留。普通 intent=null 必须 NON_NULL 省略并证明旧 request canonical bytes/digest 不变。
2. **P1：`taskCode` 仅校验 non-blank，会把 Cloud 字符串直接变成本地文件名。** D3:321-325 允许任意 canonical 非空文本，随后
   handler 用它生成 `quest_detail_<safeTask>.png`；这既未定义 `safeTask` 算法，也允许路径分隔符/保留名/超长文本影响
   `WindowScopedTempPath`，并可能把 HEAD 的中文 task 名改写成另一文件名。Repair 必须把 wire 收敛为 closed allowlisted
   `QuestArtifactTaskCode`（当前实际任务集）或 strict enum code；DHXY 只按本地常量表映射到 HEAD 的 exact leaf name，拒绝未知值，
   绝不把 wire 文本直接拼路径。latest/history 的 leaf、`System.currentTimeMillis()` 历史后缀和 fail-soft 两次写盘保持 HEAD。
3. **P1：workflow owner 错把 `runRevision` 放进生命周期 key，pause/resume 会丢失未完成 invocation。** D3:332 声明 state
   绑定 runRevision 并“随 run execution context 生死”，但 retained action/Full R0 的 StableRunKey 必须跨 revision 保留；旧 revision
   request 被 fence 后，workflow 的 step/slot occurrence/UNKNOWN 账不能消失或换 workflow。Repair 必须把 owner key 固定为稳定
   scope + taskRunId + immutable window/stop/task metadata（不含 runRevision），把 current context/port 作为 assembly-owned generation
   原子重绑；stale context 只能拒绝发命令，不能删除 workflow。
4. **P1：`beginInvocation(auth, api)` 每次调用铸新 workflowId，允许 UNKNOWN 被方法重入绕过。** D3:335-337 同时要求
   UNKNOWN 保持原 workflow，却又规定重复 public 调用必铸新 ID；调用栈重试/恢复只需再次调用 API 就能建立第二 workflow、跳过
   第一条 unresolved step。Repair 必须增加 caller-retained invocation identity，或规定每 `(stableRun,api)` 仅一个 active workflow：
   同一逻辑调用/re-entry 必须 `retainOrResume` 返回 exact handle；只有前一 workflow 已 terminal-consumed 且 caller 明确开始新的业务调用
   才单调分配新 ID。UNKNOWN/CONSUMPTION_UNKNOWN 时 begin 必须 fail-closed 或返回原 handle，绝不 mint。
5. **P2：`QuestDetailCapture.imagePath` 的目标语义仍是“空/opaque”二选一。** HEAD caller 日志打印 latest path；Cloud 又不能拿
   可操作本地路径。Repair 必须选择一个唯一合同：建议固定为空字符串，并明确 caller 的 Cloud 日志改为 artifact kind/task code +
   local-handler 两次写盘日志，或增加 digest-covered/non-operable artifact reference 并列全 outcome wire 写集。不能让实施者自行选择，
   也不能把本地绝对路径回传 Cloud。

### 下一任务：`W-QM-D4`（Design Repair #3 / implementation-ready Delta）

External Worker A 必须在 `2026-07-13T05:24:57-04:00` 前追加 CLAIMED（任务、时间、唯一写集=仅本日志），只关闭上述
P1x4/P2x1。S1 fresh capture、S4 Direct failure matrix、D2 坐标/ROI/resource/exclusive 均冻结不得重开。Java/resources/tests/
schema/Maven/runtime 继续冻结；P Full R0 写集仍由 P 独占。完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T05:11:14-04:00

领取父级任务 **W-QM-D4**（Design Repair #3 / implementation-ready Delta，闭合 Parent Design Review #3 的 P1×4+P2×1）。唯一写集 = **仅本固定日志 append-only**（设计/源码核对，**Java/resources/tests/schema/Maven/runtime 全冻结**，P Full R0 写集由 P 独占，不改 B/S/host/caller）。领取截止 2026-07-13T05:24:57-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：据真实构造链（withComputedRequestDigest(CaptureRequest):39-41、RemoteCommandEnvelope:68-72、CloudTaskRunExecutionGate.newCaptureRequest normal/observation、CloudTaskRunCommandExecutor.capture:116-120 retained spec equality、CloudTaskServicePort.capture、RemoteGameClientPort）追加 implementation-ready Delta：T1 列全 artifact intent 跨仓构造/封装/digest/retained 写集(canonical 算法不改，普通 intent=null NON_NULL 省略证旧 bytes 不变)；T2 taskCode 收敛为 closed allowlist enum QuestArtifactTaskCode，DHXY 本地常量表映射 exact leaf name 拒未知、绝不拼 wire 文本；T3 workflow owner key=stable scope+taskRunId+immutable window/stop/task(不含 runRevision)，current context/port assembly generation 原子重绑，stale 只拒发不删 workflow；T4 retainOrResume per (stableRun,api) 至多一 active workflow，同调用/re-entry 返 exact handle，UNKNOWN begin fail-closed/返原不 mint；T5 imagePath 固定空串+caller Cloud 日志改 artifact kind/taskCode+handler 两次写盘日志。S1/S4/坐标/ROI/resource/exclusive 冻结不重开。批准前继续冻结。

## External Worker A - Design Repair #3 Delta (implementation-ready) - 2026-07-13

关闭 `Parent Design Review #3` 的 P1×4+P2×1。仅设计，Java/resources/tests/schema/Maven/runtime 全冻结，P Full R0 由 P 独占，不改 B/S/host/caller。已通过部分（S1 逐 candidate fresh capture、S4 Direct 消费矩阵、D2 坐标/ROI/resource/exclusive）冻结不重开。取证：`RemoteProtocolDigests.withComputedRequestDigest(CaptureRequest)`(:39-41 逐参重建)、`RemoteCommandEnvelope.payload(CaptureRequest)`(:76-82 手工 payload)、`CloudTaskRunExecutionGate.newCaptureRequest`(:169-180)/`newPausedObservationCaptureRequest`(:310-321)、`CloudTaskRunCommandExecutor.capture`(:116-120 retained spec equality)、`CloudTaskServicePort.capture`(:59)、`RemoteGameClientPort.capture`(:41)、`QUEST_PANEL_TASK_CODE="xiuluo"`。

### T1（P1-1）：artifact intent 全构造/封装/digest/retained 写集（撤回“digest 无需改”）

**撤回** D3「两仓 digest 无需改」。intent 必须贯穿每个 CaptureRequest 重建/序列化/retained 比较点，否则 digest 后丢字段或 wire 不带字段。**exact 写集（W-QM-1 实施期）**：

| 仓库 | FQCN / 点 | 改动 |
|---|---|---|
| Cloud | `remote.QuestDetailArtifactIntent`（New record `{ QuestArtifactTaskCode taskCode }`，kind 固定 QUEST_DETAIL） | New |
| Cloud | `remote.QuestArtifactTaskCode`（New closed enum，见 T2） | New |
| Cloud | `remote.CaptureRequest` | 增 nullable `QuestDetailArtifactIntent artifactIntent`（NON_NULL；键缺失=普通） |
| Cloud | `remote.RemoteProtocolDigests.withComputedRequestDigest(CaptureRequest)`(:39-41) | 逐参重建带上 `request.artifactIntent()`（**canonical 算法不改**；intent 为 string-enum 对象走既有分支；null 由 NON_NULL mapper 省略） |
| Cloud | `remote.RemoteCommandEnvelope.payload(CaptureRequest)`(:76-82) | `if (capture.artifactIntent()!=null) payload.set("questDetailArtifact", MAPPER.valueToTree(intent))`（null 省略） |
| Cloud | `remote.CloudTaskRunExecutionGate.newCaptureRequest`(:169-180) | 形参带 intent 入 `new CaptureRequest(...)` |
| Cloud | `remote.CloudTaskRunExecutionGate.newPausedObservationCaptureRequest`(:310-321) | **恒传 null**（quest detail 是 ACTIVE，非 PAUSED observation），显式记零变化 |
| Cloud | `remote.CloudTaskRunCommandExecutor.capture`(:116-120) | retained spec equality 纳入 intent（同 bytes 重投须 intent 相等） |
| Cloud | `remote.CloudTaskServicePort.capture`(:59) | 加 intent 形参（或 `captureQuestDetail(...)` overload），供 Cloud QuestManagerService 传 |
| Cloud | `remote.RemoteGameClientPort.capture`(:41) | 若 public delegate 保持单签名则同步加 intent 形参 |
| DHXY | `RemoteCaptureCommandPayload`/`RemoteOperationPayloadCodec`/`LocalRemoteGameCommandHandler.executeCapture`/schema §5.1 | 镜像闭合字段 strict schema + intent→fail-soft latest/history（D3 保留） |

- **旧 request 不变证明**：intent=null 时**每个**构造/序列化点（record 重建、envelope payload、digest tree）均因 NON_NULL 省略键→所有现有非 quest capture 的 canonical bytes、`requestDigest`、retained bytes **逐字不变**；仅 `detail-capture` address 携带非空 intent。

### T2（P1-2）：taskCode 收敛为 closed allowlist enum + DHXY 本地 leaf 表（撤回任意文本拼路径）

- **撤回** D3 的「任意 canonical 非空 taskCode → `quest_detail_<safeTask>.png`」。wire 收敛为 **closed enum `QuestArtifactTaskCode`**，值 = 当前实际 quest-detail 任务集：**`XIULUO`**（唯一 detail-capture caller `QUEST_PANEL_TASK_CODE="xiuluo"`；未来新增任务再加 enum 值）。strict schema 拒未知值。
- **DHXY 本地常量表**映射 enum→HEAD exact leaf name（绝不拼 wire 文本、无路径分隔/保留名/超长风险）：`XIULUO → "xiuluo"` → latest `quest_detail_xiuluo.png` + history `quest_detail_xiuluo_<System.currentTimeMillis()>.png`（逐字保 HEAD `saveQuestDetailDebugImage` 的 leaf/后缀/两次 fail-soft 写盘/`WindowScopedTempPath` owner）。未知 enum（不可能，closed）→拒绝。
- wire 只传 enum 名（canonical），本地文件名只来自本地表，**wire 文本永不进路径**。

### T3（P1-3）：workflow owner key 去 runRevision，跨 revision 保留（撤回随 revision 生死）

- **撤回** D3「state 绑 runRevision、随 run execution context 生死」。retained/Full R0 的 `StableRunKey` 必须跨 revision 保留。
- `QuestManagerWorkflowState` owner key 固定 = **`(scope, taskRunId, taskType, immutable window, nonTerminalStopEpoch)`**（与 `CloudTaskRetainedActionState.StableRunKey` 同构，**不含 runRevision**）→ 同一 taskRun 跨 revision 复用。
- **current context/port = assembly-owned generation**，resume 时原子重绑（同 P adapter latest-generation 模式）；**stale context 只拒绝发命令，绝不删除 workflow**（step/slot occurrence/UNKNOWN 账跨 revision 保留）。旧 revision request 被三门 fence 拒绝，但 workflow 账不消失、不换 workflow。

### T4（P1-4）：retainOrResume 单 active workflow，UNKNOWN 不得绕过（撤回每调用铸新 ID）

- **撤回** D3「每次 public 调用必铸新 workflowId」（re-entry 可建第二 workflow 跳过 unresolved step）。
- **每 `(stableRun, api)` 至多一个 active workflow**（api∈{ACTIVATE, CAPTURE_DETAIL}）。方法改为 **`retainOrResume(stableRun, api): WorkflowHandle`**：同一逻辑调用 / 调用栈重试 / 恢复 → 返回**exact 现有 handle**（同 workflowId、同 step、同 slot occurrence、同 UNKNOWN 账）。
- **仅当**前一 workflow 已 **terminal-consumed** 且 caller **明确开始新业务调用** → 单调分配新 workflowId。
- **UNKNOWN / CONSUMPTION_UNKNOWN 时 `retainOrResume` 必 fail-closed 或返回原 handle，绝不 mint** 新 ID（结构性禁止绕过）。
- caller-retained invocation identity：Cloud QuestManagerService 每 public API 持该 handle 至 terminal；re-entry 重新 `retainOrResume` 取回同一 handle。

### T5（P2）：imagePath 唯一合同 = 固定空串（撤回二选一）

- `QuestDetailCapture.imagePath`（既有模型复用，T-D2 R3）Cloud 侧**固定为空串 `""`**（唯一合同，非实施者选择）。
- caller（`XiuluoTaskV2` 及 QM 编排）的 **Cloud 日志改为打印 artifact kind + taskCode**（`QUEST_DETAIL` + `XIULUO`），不再打印本地路径；latest/history 实际写盘由 **DHXY 本地 capture handler 两次 fail-soft 写盘日志**记录。
- **outcome wire 零改动**：不给 `CaptureOutcome` 加 path/ref 字段；Cloud 永不收到本地绝对路径。

### 修订文件表（W-QM-1 实施期，Worker A 可拥有；他仓/worker 引用）

| 仓库 | FQCN | New/Modify | 门 |
|---|---|---|---|
| Cloud | `remote.QuestDetailArtifactIntent` / `remote.QuestArtifactTaskCode` | New×2 | 本 Delta 批准 |
| Cloud | `remote.CaptureRequest` / `RemoteProtocolDigests`(record 重建) / `RemoteCommandEnvelope` / `CloudTaskRunExecutionGate` / `CloudTaskRunCommandExecutor` / `CloudTaskServicePort` / `RemoteGameClientPort` | Modify | **wire cohort，需父级确认与 P/M semanticAddress 在途不冲突**（这些类正被并发扩展） |
| Cloud | `remote.QuestManagerWorkflowState`（key 去 runRevision，retainOrResume） | New | T3/T4 批准 + assembly generation 前置 |
| Cloud | `service.QuestManagerService`（整类编排，dormant） | New | W-QM-1 门=上列 + M Full R0 + exclusive/activation 前置 + 父级授权 |
| Cloud | `model.quest.QuestDetailCapture` | 零修改（复用；imagePath 由编排置空串） | — |
| DHXY | `RemoteCaptureCommandPayload`/`RemoteOperationPayloadCodec`/`LocalRemoteGameCommandHandler`/`docs schema §5.1` + 本地 leaf 常量表 | Modify | wire cohort 门 |
| — | P Full R0 / B / S / host / caller(XiuluoTaskV2 log) | 非-A | 各自 cohort |

**注**：wire cohort（CaptureRequest 及其构造链）当前正被并发 worker 扩展（semanticAddress 等）；实施期须父级统一排序，避免与在途 dirty 冲突——本 Delta 只固定 exact API 与写集，不代改。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #3 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #4 - PARTIAL PASS / Repair #4 Published - 2026-07-13T05:17:00-04:00

父级按当前双仓源码复审 D4。closed `XIULUO` allowlist、wire 文本不入路径、workflow key 去 `runRevision`、同一 active
workflow 不因 UNKNOWN 铸新 ID，以及 Cloud `imagePath=""` / outcome 不回传本地路径，方向均 PASS 并冻结。主体仍
**BLOCKED，P0=0/P1=4/P2=1**；只补以下实现级缺口，不重开 S1/S4/D2/T2/T5 已通过语义。

1. **P1：Cloud typed request 与 transport payload 的字段名不一致，双仓 digest 必然拒绝。** D4 T1 把
   `CaptureRequest` record component 定为 `artifactIntent`，但 `RemoteCommandEnvelope.payload` 又写键
   `questDetailArtifact`。当前 Cloud `RemoteProtocolDigests:32-41` 直接把 record 转 tree，DHXY
   `RemoteProtocolDigests:42-63` 则把 transport payload 键原样 merge 到 typed request root；因此非空 intent 时 Cloud hash
   的键是 `artifactIntent`，DHXY 重建键是 `questDetailArtifact`，副作用前 `requestDigestMatches` 必失败。Repair 必须选择一个
   唯一 canonical key（固定为 `artifactIntent`）并让 Cloud record、envelope、DHXY payload、strict codec、schema 全部逐字同名；
   给出 intent 非空与 null 两棵 canonical request shape，null 时键缺失且旧 bytes/digest 不变。
2. **P1：DHXY strict wire 写集仍不可编译且不够 strict。** D4 只列现有 `RemoteCaptureCommandPayload`/codec/handler，未列承接
   Cloud enum/object 的本地 closed DTO/enum；当前 codec `CAPTURE_FIELDS` 同时充当 allowed+required（`RemoteOperationPayloadCodec:14-15,41-45`）。
   Repair 必须列出本地 `RemoteQuestDetailArtifactIntent` 与 `RemoteQuestArtifactTaskCode`（或同文件真实 nested closed 类型）的精确
   New/Modify 路径，固定 enum wire token `XIULUO`；把 capture allowed fields 与 required fields 分开：旧四字段仍 required，
   `artifactIntent` 仅 optional，但一旦出现必须 non-null object、仅含 required `taskCode`，未知键/未知 enum/null 全拒绝。
3. **P1：`QuestManagerWorkflowState` 没有进入现有 authority assembly/runtime，跨 revision 保留只是声明。** 当前
   `CloudTaskRunAuthorityAssembly.TaskServiceRuntime:305-359` 只持 service/task context、retainedActionState、metadata；resume
   `:218-236` 只复用 retainedActionState。D4 文件表却把 assembly 标成非 A 且没有任何 owner/publication 修改，因此新 state
   无构造、无 retained 引用、无 generation 原子重绑入口。Repair 必须给出唯一真实挂载：assembly 创建一个 stable-run
   `QuestManagerWorkflowState`，`TaskServiceRuntime` 初始持有并在 resume 复用同一对象；current context/port 只能从 slot 已发布的
   exact runtime generation 取得，不在 workflow state 里另存可陈旧 port。列出 assembly/runtime/slot 的精确 Modify 点和顺序。
4. **P1：workflow 新调用与 re-entry 的线性化合同仍缺少真实 capability/state transition。** 仅写
   `retainOrResume(stableRun, api)` 无法区分并发 caller 的旧 handle 重入与 terminal-consumed 后的新业务调用，两个线程可在边界上
   一边继续旧 workflow、一边 mint 新 workflow。Repair 必须定义 package-private、owner-bound 的 opaque
   `WorkflowHandle`/generation：`retainOrResume(api, expectedHandleOrEmpty)` 在同一 owner lock 下执行；active/UNKNOWN 返回 exact
   handle，只有 exact terminal-consume capability 成功关闭后，携带明确 `NEW_INVOCATION` 意图的下一次调用才单调 mint；旧 handle
   此后 fail-closed。列清状态、transition、并发结果与 terminal-consume 触发点，不新增 durable/TTL/retry。
5. **P2：artifact 写盘的副作用时序未固定。** 当前 DHXY handler 在 capture/PNG encode 后还会做 timeout、registration/revision、
   bound-window/geometry 复验（`LocalRemoteGameCommandHandler:385-412`）。若 intent 写盘放在这些 fence 前，stale/timeout 请求也会污染
   latest/history；若放在 OBSERVED/outcome 持久化后，则 outcome 可先声称成功而 artifact 尚未执行。Repair 固定：仅在 PNG bytes
   已生成且 timeout + exact registration/runRevision + binding/geometry 全部复验通过后、构造/记录 OBSERVED outcome 前，按本地
   enum 常量表写 latest/history；两次写盘各自 fail-soft 只记日志，不改变 capture outcome，UNKNOWN/NOT_EXECUTED 不写盘。

### 下一任务：`W-QM-D5`（Design Repair #4 / final implementation contract）

External Worker A 必须在 `2026-07-13T05:37:00-04:00` 前追加 CLAIMED（任务、时间、唯一写集=仅本日志）。只关闭上述
P1x4/P2x1；T2 closed enum、T5 空 path、S1/S4/D2 全冻结。为了消除实施者选择，port 链固定为修改既有
`capture(..., QuestDetailArtifactIntent nullable, ...)` 单路径，所有普通 caller 显式传 null；不得再写“加形参或 overload”。
P2 Full R0 重叠 wire 文件继续只读，Java/resources/tests/schema/Maven/runtime 冻结。完成后等待父级复审。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T05:19:29-04:00

领取父级任务 **W-QM-D5**（Design Repair #4 / final implementation contract，闭合 Parent Design Review #4 的 P1×4+P2×1）。唯一写集 = **仅本固定日志 append-only**（设计/源码核对，**Java/resources/tests/schema/Maven/runtime 全冻结**，P2 Full R0 重叠 wire 文件只读，不改 B/S/host/caller）。领取截止 2026-07-13T05:37:00-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：追加 final implementation contract Delta：U1 唯一 canonical key 固定 `artifactIntent`（Cloud record/envelope/DHXY payload/codec/schema 逐字同名），给 intent 非空/null 两棵 canonical request shape，null 键缺失旧 bytes 不变；U2 DHXY 列 New RemoteQuestDetailArtifactIntent+RemoteQuestArtifactTaskCode(wire token XIULUO)，codec allowed/required 分离(旧四 required、artifactIntent optional 出现须 non-null object 仅含 required taskCode，未知键/enum/null 全拒)；U3 WorkflowState 挂载=assembly 建 stable-run + TaskServiceRuntime 初持+resume 复用同对象，context/port 只从 slot 已发布 runtime generation 取，列 assembly/runtime/slot 精确 Modify 点与顺序；U4 opaque owner-bound WorkflowHandle/generation，retainOrResume(api,expectedHandleOrEmpty) 同 owner lock，active/UNKNOWN 返 exact handle，仅 exact terminal-consume capability 关闭后携 NEW_INVOCATION 才单调 mint、旧 handle fail-closed，列状态/transition/并发结果/terminal-consume 触发点；U5 artifact 写盘固定在 PNG 已生成且 timeout+registration/runRevision+binding/geometry 全复验通过后、构造 OBSERVED outcome 前，两次 fail-soft 只记日志不改 outcome，UNKNOWN/NOT_EXECUTED 不写。port 链固定=修改既有 capture(...,QuestDetailArtifactIntent nullable,...) 单路径普通 caller 显式传 null。批准前继续冻结。

## External Worker A - Design Repair #4 Delta (final implementation contract) - 2026-07-13

关闭 `Parent Design Review #4` 的 P1×4+P2×1。仅设计，Java/resources/tests/schema/Maven/runtime 全冻结，P2 Full R0 重叠 wire 只读，不改 B/S/host/caller。已通过语义（S1/S4/D2、T2 closed enum、T5 空 path）冻结不重开。port 链固定 = 改既有 `capture(..., QuestDetailArtifactIntent nullable, ...)` 单路径，普通 caller 显式传 null。取证：DHXY codec `requireObjectWithFields(payload, allowed, required, …)`(CAPTURE_FIELDS 现 allowed=required)、handler OBSERVED 路径 `timedOut(:385)→requireBoundWindow(:395)→requireRegistration(:396)→hasSameGeometry(:397)→builder(:403)`、assembly `TaskServiceRuntime` 持 `retainedActionState`(:310) 且 resume 复用同对象(:228/:235)。

### U1（P1-1）：唯一 canonical key = `artifactIntent`（Cloud/transport 逐字同名）

**撤回** D4 的 envelope 键 `questDetailArtifact`（与 record component `artifactIntent` 不一致→Cloud hash 键 `artifactIntent`、DHXY 重建键 `questDetailArtifact`→`requestDigestMatches` 必失败）。**唯一 canonical key 固定 `artifactIntent`**，逐字同名于：Cloud `CaptureRequest` record component、`RemoteCommandEnvelope.payload` 写键、DHXY `RemoteCaptureCommandPayload` 字段、codec 键、schema §5.1。两棵 canonical request shape：

```text
null intent（所有普通 capture）：      非 quest：键 "artifactIntent" 缺失（NON_NULL 省略）→ request bytes/requestDigest 逐字不变
non-null intent（仅 detail-capture）： "artifactIntent": { "taskCode": "XIULUO" }   （digest-covered）
```

Cloud digest（record→tree，NON_NULL 省略）与 DHXY digest（transport payload 键原样 merge 进 typed request root）此后同用 `artifactIntent` 键→副作用前 `requestDigestMatches` 通过。

### U2（P1-2）：DHXY strict wire DTO/enum + allowed/required 分离

- **New DHXY**：`com.bot.dhxy.cloud.remote.RemoteQuestArtifactTaskCode`（closed enum，wire token 恰 `XIULUO`）；`com.bot.dhxy.cloud.remote.RemoteQuestDetailArtifactIntent`（closed DTO，仅 `RemoteQuestArtifactTaskCode taskCode`）。镜像 Cloud `QuestArtifactTaskCode`/`QuestDetailArtifactIntent`（逐字 enum token/字段同构）。
- **Modify DHXY `RemoteCaptureCommandPayload`**：增 optional `RemoteQuestDetailArtifactIntent artifactIntent`。
- **Modify DHXY `RemoteOperationPayloadCodec`**：拆分 capture 字段——`CAPTURE_REQUIRED_FIELDS={captureId,region,imageFormat,capturePurpose}`（旧四仍 required）、`CAPTURE_ALLOWED_FIELDS=required ∪ {artifactIntent}`；`requireObjectWithFields(payload, CAPTURE_ALLOWED_FIELDS, CAPTURE_REQUIRED_FIELDS, "capture payload")`。`artifactIntent` **optional**；一旦出现必为 non-null object，字段恰 `{taskCode}`（新 `ARTIFACT_INTENT_FIELDS={taskCode}` allowed=required），`taskCode` 必为已知 enum token（`XIULUO`）；未知键/未知 enum/null → `RemotePayloadException` 拒。
- handler 从解码 payload 读 intent（U5 用）。

### U3（P1-3）：WorkflowState 挂载进 assembly/runtime（唯一真实入口）

复刻 `retainedActionState` 的挂载/复用形状（跨 revision 同一对象）：

- **Modify `CloudTaskServiceExecutionContext`**：随 stable run 创建/持有 `QuestManagerWorkflowState`（与 `retainedActionState()` 同位一次创建；owner key = `(scope,taskRunId,taskType,window,nonTerminalStopEpoch)`，不含 runRevision）。
- **Modify `CloudTaskRunAuthorityAssembly.TaskServiceRuntime`**（:305-359）：加 `questManagerWorkflowState` 字段 + 构造形参（与 `retainedActionState` 并列，:310/:314）。
- **Modify `createCurrentContextSlotActivation`**（:164-168）：`new TaskServiceRuntime(..., serviceContext.questManagerWorkflowState())`。
- **Modify `resumeTaskServiceRuntime`**（:228/:235）：`previousRuntime.questManagerWorkflowState()` **复用同一对象**（同 retainedActionState 跨 revision）。
- **current context/port 只从 slot 已发布的 exact runtime generation 取得**（`runtime.serviceExecutionContext()/servicePort()`）；`QuestManagerWorkflowState` **只存 stable-run 身份 + step/handle 账，绝不另存可陈旧 port**。顺序：K transition→H activation→slot publication 后，workflow state 经已发布 runtime 取当前 port；stale context 只拒发命令、不删 workflow。
- **写集归属**：此 3 类（ServiceExecutionContext/assembly/runtime）属 remote authority cohort，正被 P/M 并发扩展→实施期须父级排序，本 Delta 只固定 exact Modify 点与顺序。

### U4（P1-4）：opaque owner-bound WorkflowHandle + 线性化 transition

- **`WorkflowHandle`**：package-private、owner-bound opaque（持 owner 引用 + `generation(long)`，**无 accessor**，模式同 adapter `LifecycleActivationHandle`）。
- **`retainOrResume(QuestPublicApi api, Optional<WorkflowHandle> expectedHandleOrEmpty)`**（在 `QuestManagerWorkflowState` **单 owner lock** 内）：
  - `(stableRun, api)` **至多一个 active workflow**。存在 active（含 UNKNOWN 态）→ 返回 **exact 现有 handle**；`expectedHandleOrEmpty` 非空则须 `==` 现有 handle，否则 fail-closed（防并发旁路）。
  - **仅当**前一 workflow 经 exact **terminal-consume capability** 成功关闭（RETIRED）**且**下一次调用携带明确 `NEW_INVOCATION` 意图**且** `expectedHandleOrEmpty` 为空 → 单调 mint 新 generation；**旧 handle 此后 fail-closed**。
  - **UNKNOWN / CONSUMPTION_UNKNOWN → 返回 exact handle，绝不 mint**。
- **状态机**：`ACTIVE(step) → UNKNOWN(step) →(resolve) ACTIVE(step) → TERMINAL_CONSUMED →(consume cap) RETIRED`；新 workflow 只能 `RETIRED + NEW_INVOCATION`。
- **terminal-consume 触发点**：package-private `consumeTerminal(WorkflowHandle, QuestStep terminalStep)`，仅 caller 明确 terminal step（activate 的 return true/false 收口、detail 的 finally close 后）才调用→RETIRED。
- **并发结果**：两线程经 owner lock 串行——一个取回 exact handle（续旧 workflow），另一个即便携 NEW_INVOCATION 也只在 RETIRED 才成功，否则 fail-closed。每 step 仍各自从 Full R0 mint 自己 slot occurrence，workflowId/generation 只做归属与推进。**无 durable/TTL/retry**。

### U5（P2）：artifact 写盘固定时序（fail-soft，不改 outcome）

DHXY `LocalRemoteGameCommandHandler.executeCapture`：当且仅当 request 携带非空 `artifactIntent`，且 PNG bytes 已生成、**`timing.timedOut()`(:385) 为 false、`requireBoundWindow(command,true)`(:395)、`requireRegistration(...,true)`(:396)、`hasSameGeometry`(:397) 全部复验通过后**，在 **构造/记录 OBSERVED outcome（`RemoteCaptureOutcomePayload.builder()` :403）之前**，按**本地 enum 常量表**写：`quest_detail_xiuluo.png`(latest) + `quest_detail_xiuluo_<System.currentTimeMillis()>.png`(history)。

- 两次写盘**各自 fail-soft**（异常只 `log.warn`，不改变 capture outcome、不改 image bytes）。
- **UNKNOWN/NOT_EXECUTED 分支**（在 :385/:395 前 return，或 scale bracket 失败）→ **不写盘**（stale/timeout 不污染 latest/history；outcome 不会先声称成功而 artifact 未执行）。
- 文件名只来自本地 enum 表（U2），wire 文本永不进路径（T2 冻结）。

### 修订最终文件表（W-QM-1 实施期）

| 仓库 | FQCN | New/Modify | 门 / 归属 |
|---|---|---|---|
| Cloud | `remote.QuestArtifactTaskCode` / `remote.QuestDetailArtifactIntent` | New×2 | Worker A |
| Cloud | `remote.CaptureRequest`(+`artifactIntent`) / `RemoteProtocolDigests`(record 重建带 intent，canonical 不改) / `RemoteCommandEnvelope`(键 `artifactIntent`) / `CloudTaskRunExecutionGate`(newCaptureRequest 带 intent、newPausedObservation 恒 null) / `CloudTaskRunCommandExecutor`(retained equality 含 intent) / `CloudTaskServicePort.capture`(+nullable intent 单路径) / `RemoteGameClientPort.capture`(+nullable intent) | Modify | **wire/authority cohort，父级排序避与 P/M semanticAddress 冲突** |
| Cloud | `remote.CloudTaskServiceExecutionContext` / `CloudTaskRunAuthorityAssembly`(TaskServiceRuntime+create+resume) / `remote.QuestManagerWorkflowState`(New) | Modify×2 + New | U3/U4 批准 + authority cohort 排序 |
| Cloud | `service.QuestManagerService`(整类编排 dormant) | New | W-QM-1 门=上列 + M Full R0 + exclusive/activation + 父级授权 |
| Cloud | `model.quest.QuestDetailCapture` | 零修改（复用，imagePath 由编排置 ""） | — |
| DHXY | `RemoteQuestArtifactTaskCode`/`RemoteQuestDetailArtifactIntent` New；`RemoteCaptureCommandPayload`/`RemoteOperationPayloadCodec`/`LocalRemoteGameCommandHandler`(U5 时序+本地 leaf 表)/`docs schema §5.1` Modify | New×2 + Modify×4 | wire cohort 门 |
| — | P Full R0 / B / S / host / caller(XiuluoTaskV2 日志改 kind+taskCode) | 非-A | 各自 cohort |

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #4 Delta（final implementation contract）到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #5 - DESIGN APPROVED / Implementation Sequenced - 2026-07-13T05:25:00-04:00

父级复审 D5：唯一 `artifactIntent` canonical key、DHXY optional-but-strict DTO/enum、closed `XIULUO` leaf map、跨 revision
同一 workflow state、opaque handle 线性化、空 `imagePath`、本地写盘 fence 与完整文件表均已闭合。结论：
**DESIGN APPROVED，P0/P1/P2=0**。QuestManager Java/wire 实施仍须等 P2 Full R0 稳定并由父级做写集合并，不得抢写重叠文件。

批准附带三条不再返工设计的绑定实施修正：

1. resume 构造 `CloudTaskServiceExecutionContext` 和 `TaskServiceRuntime` 时必须把
   `previousRuntime.questManagerWorkflowState()` 同时传入两者，并以引用相等校验，禁止一个 runtime 出现两份 state。
2. U4 的单方法签名须显式增加 closed `QuestWorkflowInvocationMode { RESUME_OR_REENTER, NEW_INVOCATION }`；
   `NEW_INVOCATION` 仅在 owner 内部已是 RETIRED 且 expected handle 为空时合法，不能靠 Optional 为空隐式猜意图。
3. artifact 两次 fail-soft 写盘后再检查一次 monotonic timeout：若写盘期间越过 deadline，文件可已存在但 outcome 必须是
   `UNKNOWN/TIMEOUT`，不得宣称 OBSERVED，也不得重做；pre-fence timeout/stale 仍零写盘。该 transport 结局不改变 HEAD 业务分支。

### Next-task handoff

A 不等待 P2。下一独立任务 `W-CBOX-D1` 已写入
`docs/superpowers/plans/reports/2026-07-13-cloud-common-box-service-worker-a.md`，只做 HEAD-clean
`CommonBoxService` 整类 Cloud lift Design #1；与 B LeftTop、P2 Full R0、S Bag Repair 写集不重叠。QuestManager 设计到此冻结。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Task - W-QM-ARTIFACT-TYPES-IMP1 - 2026-07-13T13:40:00-04:00（EOF 权威块）

说明：同标题任务块曾因 append 锚点过宽误插入旧历史位置；**仅本真实 EOF 块是当前任务权威**。Full R0 已最终通过，
`W-NAV-WF-IMP1` 也已父级 FINAL APPROVED。External Worker A 现转回已经 DESIGN APPROVED 的 QuestManager D5，先实施
不触共享 wire/assembly 的 closed artifact 类型闭包。须在发布后 20 分钟内于本日志真实 EOF 追加 `CLAIMED`（task、
claimedAt、uniqueWriteSet），然后仅新建以下 4 个文件：

1. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/QuestArtifactTaskCode.java`：public closed enum，
   当前唯一 wire token 为 `XIULUO`；
2. Cloud `.../remote/QuestDetailArtifactIntent.java`：public immutable record，仅含 non-null
   `QuestArtifactTaskCode taskCode`，constructor 使用现有 `RemoteProtocolValidation.required`；
3. DHXY `src/main/java/com/bot/dhxy/cloud/remote/RemoteQuestArtifactTaskCode.java`：public closed enum，
   token 与 Cloud 逐字一致，仅 `XIULUO`；
4. DHXY `.../cloud/remote/RemoteQuestDetailArtifactIntent.java`：按本地 DTO 惯例使用 `@Value @Builder @Jacksonized`，
   仅含 `RemoteQuestArtifactTaskCode taskCode`，不加默认值、路径、kind、自由文本或兼容 fallback。

本波只落真实协议值类型，不修改 `CaptureRequest`、payload/codec/digest/envelope/gate/handler/schema、workflow state、
assembly、Service、Task、host/caller；不得新增 tests，不启动运行面，不做 Git mutation。完成后分别运行 Cloud
`mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，向本日志追加精确文件、wire token 对称性、
构建结果与 self-QA。Worker self-QA 不构成父级批准；逾期只重发给 External A，绝不由内部 Worker 接管。

**无已批准业务差异；按基线等价迁移。**

## Parent Claim Gate #1 - UNCLAIMED / REISSUED TO EXTERNAL A - 2026-07-13T14:00:25-04:00

截至首轮领取截止 `2026-07-13T14:00:00-04:00`，本日志真实 EOF 未出现
`W-QM-ARTIFACT-TYPES-IMP1` 的新 `CLAIMED`。这只表示 External A 未在 20 分钟内登记领取，不表示任务应由他人完成；
按用户明确规则，**绝不内部接管**，同一任务原样重发给 External A。

External A 看到本条后须在 `2026-07-13T14:20:25-04:00` 前先追加：task、claimedAt、uniqueWriteSet（上述四个 New 文件
+ 本日志）；领取后可工作超过 20 分钟。实现范围、双构建门、no-tests/no-runtime/no-Git 与上一任务块逐字不变；若目标文件
已被其它在途改动创建，先报告冲突，不覆盖。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T14:12:31-04:00

- task: W-QM-ARTIFACT-TYPES-IMP1（Claim Gate #1 重发，截止 2026-07-13T14:20:25-04:00 内领取）
- claimedAt: 2026-07-13T14:12:31-04:00
- uniqueWriteSet: ① Cloud New `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/QuestArtifactTaskCode.java`；② Cloud New `.../remote/QuestDetailArtifactIntent.java`；③ DHXY New `src/main/java/com/bot/dhxy/cloud/remote/RemoteQuestArtifactTaskCode.java`；④ DHXY New `.../cloud/remote/RemoteQuestDetailArtifactIntent.java`；⑤ 本 append-only 日志。只落协议值类型；不改 CaptureRequest/payload/codec/digest/envelope/gate/handler/schema/workflow state/assembly/Service/Task/host/caller；不新增 tests、不启动运行面、不做 Git mutation。目标文件若已被在途改动创建则先报告冲突不覆盖。完成后 Cloud `mvn -q clean package`（不 skip）+ DHXY `mvn -q -DskipTests compile` + 精确文件/wire token 对称性/构建结果/self-QA。self-QA 不构成父级批准。

## External Worker A - W-QM-ARTIFACT-TYPES-IMP1 实施证据 - 2026-07-13

写集恰为授权四文件+本日志；四个目标文件此前均不存在（无在途冲突），未改 CaptureRequest/payload/codec/digest/envelope/gate/handler/schema/workflow state/assembly/Service/Task/host/caller，未新增 tests，未启动运行面，未做 Git mutation。

### 精确文件

1. **Cloud New `remote/QuestArtifactTaskCode.java`**：public closed enum，唯一成员 `XIULUO`；javadoc 声明 enum 名即 canonical wire token，与 DHXY 镜像逐字一致。
2. **Cloud New `remote/QuestDetailArtifactIntent.java`**：public immutable `record QuestDetailArtifactIntent(QuestArtifactTaskCode taskCode)`，compact 构造 `taskCode = RemoteProtocolValidation.required(taskCode, "taskCode")`（既有 :82 签名）；无默认值/路径/kind/自由文本/fallback。
3. **DHXY New `cloud/remote/RemoteQuestArtifactTaskCode.java`**：public closed enum，唯一成员 `XIULUO`。
4. **DHXY New `cloud/remote/RemoteQuestDetailArtifactIntent.java`**：`@Value @Builder @Jacksonized`（与同包 `RemoteBindingFact` 惯例逐字同形），唯一字段 `RemoteQuestArtifactTaskCode taskCode`；无默认值/路径/kind/自由文本/兼容 fallback。

### wire token 对称性

两侧 enum 成员集恰为 `{XIULUO}`，字符逐字一致；序列化 token=enum 名（Jackson 默认），双向镜像无扩展成员、无别名。

### 构建结果

- **Cloud `mvn -q clean package`（未 skip）：exit 0**（含既有 4 suites/21 tests 全绿）。
- **DHXY `mvn -q -DskipTests compile`：失败**——错误全部位于 `LocalRemoteGameCommandHandler.java` :132/:323/:825 三处 `switch (command.getOperation())` 穷尽性（"switch 表达式不包含所有可能的输入值"）。**与本写集零关联的反证**：临时移除本任务两个 DHXY 新文件后重新编译，**同样的三处错误原样存在**（随后已还原文件）。根因=并行在途改动：`com/bot/dhxy/cloud/remote/` 整包为未跟踪的在途新代码，`RemoteOperation` 新增了成员（Cloud 侧存在 `TaskTrackerMaterializeRequest/TaskTrackerReadRequest`）而该 handler 的三个穷尽 switch 尚未补齐——属其它 Worker 写集，本任务按约不触碰 handler。本写集的两个 DHXY 纯类型文件不被任何现有代码引用，语法/依赖自洽（同形惯例 + 同包 enum 引用）。
- 待并行 handler 写集补齐后 DHXY compile 门可复验；Cloud 侧本写集已过全量 package 门。

self-QA：**P0=0 / P1=0 / P2=0**（DHXY 门阻塞项为外部在途写集，已给反证与根因，非本写集缺陷；仅 QA，不构成父级批准）。W-QM-ARTIFACT-TYPES-IMP1 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Source Review #1 - SOURCE APPROVED / FINAL BUILD PENDING - 2026-07-13T14:20:55-04:00

父级独立读取四个新增文件并对照已批准 D5 与本任务文件表复审。Cloud/DHXY 两侧 closed enum 成员均且仅为
`XIULUO`；Cloud intent 是经 `RemoteProtocolValidation.required` 收紧的 immutable record；DHXY intent 按同包惯例使用
`@Value + @Builder + @Jacksonized` 且只持 typed enum。未发现路径、自由文本、默认值、fallback、额外 wire token 或越界
修改。**本四文件源码范围 P0=0/P1=0/P2=0，SOURCE APPROVED。**

最终构建门暂不冒充通过：Cloud fresh `mvn -q clean package` 已 exit 0（4 suites/21 tests）；DHXY compile 的三处
non-exhaustive switch 位于当前 Internal Z 正连续写入的 `RemoteOperation`/codec/handler 原子 cohort。A 已用临时移除本任务
两个 DHXY 类型后错误仍逐字存在的反证证明该失败不由本四文件引入。故 A 无需修改本写集，也不背并行 cohort 的返修；
但在 Z 写入稳定后，父级必须亲自运行一次 fresh `mvn -q -DskipTests compile`，成功后本任务才可标
`FINAL APPROVED`。截至该门完成，本结论是“源码通过、最终构建待统一复验”，不是 Blocked，也不是最终完成。

**无已批准业务差异；按基线等价迁移。**

## Parent Next-task Handoff - `W-DCM-D1` - 2026-07-13T14:24:25-04:00（真实 EOF 权威块）

说明：同标题 handoff 因 append 锚点过宽误插入旧历史位置；不删除、不改写历史，**仅本真实 EOF 块是当前权威入口**。
本任务四文件已 `SOURCE APPROVED`，最终 DHXY compile 由父级在 Internal Z 原子写集稳定后统一复验；A 不等待该共享门，也不得
修改 handler/operation cohort。下一独立任务已发布到
`docs/superpowers/plans/reports/2026-07-13-cloud-dialog-choice-memory-service-worker-a.md`：只做 HEAD-clean
`DialogChoiceMemoryService` 整类 Cloud lift Design #1。A 须在新日志 `2026-07-13T14:44:25-04:00` 前先写
`CLAIMED`；批准前零 Java。任务与 External B TeamReturn、Internal Y PlayerState、Internal Z SummonSkill 写集完全不重叠。

**无已批准业务差异；按基线等价迁移。**
