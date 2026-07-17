# Cloud BagService lift-and-shift - Internal Worker S

## Parent Task Brief #1 - `W-BAG-D1` - 2026-07-13T04:48:00-04:00

### 目标

为 DHXY HEAD `0114604e1ff5f15491d2910959c45252e893d04f` 的
`src/main/java/com/bot/dhxy/service/BagService.java` 形成整类 Cloud lift-and-shift Design #1。保持 public API、页序、模板候选、
阈值、缓存、随机点、click/use、exclusive/ordinary 输入边界、close/finally、retry/fallback 与 stop/pause 语义；只迁移业务编排/CPU
判定，DHXY 继续拥有窗口绑定、capture、temp artifact、输入队列与副作用前安全拒绝。

### 基线与冲突门

- 开工先完整读取 `D:/mavenProject/DHXY/AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、本报告与迁移矩阵。
- 当前 DHXY `BagService.java` 有在途修改，**不得把本地差异当业务权威**。必须以 `git show
  0114604e:src/main/java/com/bot/dhxy/service/BagService.java` 为设计基线，并单列当前 worktree diff 只用于冲突识别；不得修改、覆盖或回滚。
- 先追加 `## Internal Worker S - CLAIMED - <timestamp>`，写明唯一写集为本报告；然后才追加 Design #1。

### Design #1 必答

1. HEAD 完整 inventory：构造依赖、public/private/nested API、常量、两类 caches、每个入口 caller、ordinary/exclusive/direct/input-worker
   分支、capture/template/temp-path 与 stop checkpoint。
2. 方法级业务矩阵：五页/任务页/反向扫描、known-page/cache、count/select/use/prescan、anchor/fallback、open/close 的确切顺序与每条
   return/exception/finally；标出纯 CPU、Cloud 编排、DHXY typed mechanical op。
3. exact context/retained action 地址：scope/taskRun/window/stopEpoch/runRevision 与每次 fresh capture/input 的 workflow occurrence；UNKNOWN
   不前进，同 bytes 重投，不随机铸新 ID。
4. 设计不得把 `GameClientTracker`、`InputSequences/InputProvider`、`WindowTaskContextHolder`、HWND、`WindowScopedTempPath`、本地 cache
   或 input-worker authority 搬云；机械操作只能经 retained typed Service port。不得开放 raw request/poll/outcome。
5. 模板/resource 与 artifact 单一属主、坐标/scale 同帧换算、缓存跨 revision/窗口的失效规则；禁止 cwd 双资源权威和可操作本地路径进入 Cloud。
6. 给出可编译依赖 DAG、最小独立叶子波、精确 New/Modify 文件表及后续双构建门。主体/caller/host 保持 dormant；不新增测试。

### Worker 约束

本轮只写本报告，Java/Maven/schema/resources/tests 全冻结；不运行 Maven，不启动 server/host/Task/poller/UI/capture/input，不做 Git
mutation，不创建 agent/reviewer，不修改 A/B/P/R 日志。Worker 自审只算 QA，不构成批准；交付后等待父级 review。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker S - CLAIMED - 2026-07-13T04:51:04-04:00

- 任务：`W-BAG-D1`。
- 角色：Internal Worker S（design worker，不是 reviewer）。
- 唯一写集：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-13-cloud-bag-service-worker-s.md`。
- 冻结边界：DHXY / Cloud Java、Maven、schema、resources、tests、host、Task、poller、UI、capture、input 与 A/B/P/R 日志全部只读；不做 Git mutation，不运行 Maven，不启动任何运行面，不创建 agent/reviewer。

## Internal Worker S - Design #1 - 2026-07-13T05:10:47-04:00

本设计只以 DHXY `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f` 的
`src/main/java/com/bot/dhxy/service/BagService.java` 为业务基线。Cloud 只接收业务编排、纯 CPU
模板判定与 retained workflow cursor；DHXY 继续持有 exact window/binding、capture、输入队列、input-worker、
窗口内坐标落地、temp artifact 与本地 cache。本文是设计交付，不是实现或 review；全部 Java/Maven/schema/resources/tests
继续冻结。

### 0. 基线、当前冲突与取证边界

| 取证项 | 结论 |
|---|---|
| DHXY branch / HEAD | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` |
| 唯一业务源码 | `git show 0114604e:src/main/java/com/bot/dhxy/service/BagService.java`，1206 行 |
| HEAD caller 取证 | 对 `GiveItemService`、`PlayerStateService`、`ReturnItemPrescanService`、`WubeiTask`、`FiveRingTaskV2`、`XiuluoTaskV2` 均使用 `git grep/git show 0114604e` |
| 当前 worktree 冲突 | `BagService.java` 相对 HEAD 为 `+134/-0`；新增 `ReturnItemPrescanSnapshots`、`captureMainBagTaskPagePrescanSnapshots(...)`、`matchMainBagTaskPagePrescanSnapshots(...)` 及其 private capture/match helper，并引入 `ImageIO/BufferedImage/IOException/Path` |
| 冲突处理 | 上述在途改动只作冲突识别，**不进入 Design #1 业务权威**。尤其其 `Path.of("images/template", target)` cwd 读取、关包后延迟 match 与新增 public API 均不得被本设计默认为已批准行为。后续实现碰到同文件必须停下交由当前 owner/父级合并，绝不覆盖、回滚或顺手吸收。 |
| Cloud 只读基线 | `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`；已有 `CloudTaskServicePort`、capture-time `systemScaleRatio`、`CloudTemplateAssets`、`ReturnItemCachePoint`，但 Bag 主体不存在；`CloudTaskExclusiveInteractionState` 仅为 R-X0 已批准 dormant policy leaf，R-X1/R-X2/R-X3 尚是硬前置 |

### 1. HEAD 完整 inventory

#### 1.1 类、构造依赖、常量与布局

- 类：`com.bot.dhxy.service.BagService`，`@Slf4j @Component @RequiredArgsConstructor`。
- 六个构造依赖：`InputSequences`、`InputProvider`、`GameClientTracker`、`CoordinateHelper`、
  `WindowScopedTempPath`、`WindowTaskContextHolder`。前五个均含本地机械/宿主能力；六者都不得复制到 Cloud。
- 模板：`anchor_huanzhuang.png` 主锚点；`task_tab_fallback_a.png -> task_tab_fallback_b.png` 固定候选序；
  `anchor_cunkuan.png` 只证明 panel visible，不提供 geometry anchor。
- 阈值：主锚点 `0.8`，tab fallback `0.8`，item `0.85`，`findAll` 近邻去重距离 `<24.0`。
- 等待：开包 `1200ms`，late render `700ms`，tab settle `500ms`，item/close settle `500ms`，
  mouse-away settle `120ms`。`BAG_TAB_CLICK_WAIT_MS=500` 与源码中的 tab literal `500` 同值，均保留。
- 页：普通可搜索页固定 `0..4`；任务页固定 index `5`；反向入口把 `maxBagIndex` clamp 到 `0..5`。
- 鼠标安全：锚点右侧 `60`；客户端固定 `1024x768`；安全区 x 为
  `max(anchorX+60,baseX+80)..baseX+944`，y 为 `baseY+90..baseY+678`；若 minX > maxX，
  minX 回退为 `baseX+864`；两个轴均 inclusive random。
- `MAIN_BAG`：主锚点、`autoManageUI=true`，grid `(-299,16,312,208)`，tab `(29,32,stepY=35)`。
- `GIVE_BAG`：无锚点、`autoManageUI=false`，grid `(359,276,308,206)`，tab `(681,292,stepY=35)`。
- `BagLayout` 是 public static class，public constructor；字段保持 package-private final。对象身份
  `layout == MAIN_BAG/GIVE_BAG` 有语义，不能按字段相等偷偷替换。

#### 1.2 本地可变状态

HEAD 有三张 `ConcurrentHashMap`，分为两类 cache：

| 类型 | 字段 | HEAD key/value 与精确作用 |
|---|---|---|
| 页 cache | `visiblePageCache` | `windowId|layout -> page 0..4`；每次 tab click 后立即记忆，任务页 5 不记 |
| 页 cache | `itemPageCache` | `windowId|layout|item=<template> -> page 0..4`；确认 match 后记忆，同时刷新 visible page |
| 锚点 cache | `lastMainBagAnchorCache` | `windowId|MAIN_BAG -> screen-absolute Point`；只供 Alt+E 前安全移鼠标，不作为“包已打开”业务事实 |

`bagCacheKey` 在没有 `WindowTaskContextHolder.rawCurrent()` 时退到 `global`。Cloud 激活路径必须始终带 exact
window context，因此不得在云端重建或共享这个 `global` map；legacy 本地路径在正式 cutover 前原样保留。

#### 1.3 nested/public API

| API | HEAD 语义 |
|---|---|
| `ItemAction { SELECT, USE }` | SELECT=左键，USE=右键 |
| `ItemCountResult(int count,Integer firstPageIndex)` | bounded count；无命中页时 firstPageIndex=null |
| `BagLayout(...)`、`MAIN_BAG`、`GIVE_BAG` | 上述固定几何/开合语义 |
| `findItemPageIndex(layout,template)` | 无 context overload，整段 exclusive，返回首命中普通页或 null |
| `findItemPageIndex(layout,template,context)` | 同上，带 stop/pause context |
| `<T> withMainBagOpen(source,context,Function<MainBagSession,T>)` | 主包开一次、callback 内多操作、finally 关一次；开包失败/队列未执行返回 null |
| `findAndSelectItem(...)` 两 overload | 普通页 known-first/cache-first，命中后左键；整段 exclusive |
| `findAndSelectItemDirectForExclusive(...)` | 若不在 input worker，退回普通入口；若已在 worker，直接执行，防 nested queue deadlock |
| `findAndUseItem(...)` 两 overload | 与 select 相同，但右键 |
| `findAndUseItemFromBack(...)` | current visible frame first，再从 clamp(max,0..5) 降到 0；右键 |
| `prescanMainBagTaskPageItem(...)` | current visible frame first，再 task page 5；只产 screen-absolute cache point，不点击 |
| `prescanMainBagItemFromBack(...)` | current visible frame first，再 max..0；只产 cache point |
| `useCachedMainBagReturnItem(...)` | cachedPoint=null 立即 false；否则开包后直接右键缓存点，不重新验模板/页/TTL |
| `findAndUseMainBagTaskPageItem(...)` | current visible frame first，再 task page 5；不扫普通五页 |
| `isMainBagOpen(context)` | 只探主锚点，不走 fallback，不开/关包，不进 input queue |
| `MainBagSession.findItemPageIndex(template)` | 已开主包内扫普通五页，不二次 acquire/open/close |
| `MainBagSession.countItemUpTo(template,required)` | required clamp >=0；0 立即 `(0,null)`；findAll 后按 cap 截断 |
| `MainBagSession.useItem(template,knownPage)` | 已开主包内 known-first/cache-first，右键，不二次 open/close |

private nested：`BagOpenCheck(anchor,panelVisible,visibleBy)`，工厂 `ready/visible/notVisible`；
`MainBagSession` 是 public final 非 static inner class，持有已确认 `baseAnchor` 与原 context。

#### 1.4 private API 全量

`findItemPageIndexExclusive`、`withMainBagOpenExclusive`、`ensureBagOpened`、`rememberMainBagAnchor`、
`moveMouseAwayFromCachedMainBagAnchor`、两个 `moveMouseAwayFromMainBagAnchor` overload、
`currentLogicalMousePoint`、`randomBetween`、`formatPoint`、`closeBagIfNeeded`、`interactWithItem`、
`interactWithItemExclusive`、`interactWithMainBagTaskPageItemExclusive`、
`findMainBagTaskPageItemPointExclusive`、`findMainBagItemFromBackPointExclusive`、
`useCachedMainBagReturnItemExclusive`、`toReturnItemCachePoint`、`findItemPageIndexInOpenMainBag`、
`countItemUpToInOpenMainBag`、`interactWithItemInOpenMainBag`、`interactWithItemFromBack`、
`interactWithItemFromBackExclusive`、`getBaseAnchor`、`checkBagOpened`、`deriveAnchorFromTaskTab`、
`findFirstTemplateInScreen`、`findTemplateInScreen`、`searchItemInTabOnly`、`searchItemsInTabOnly`、
`searchItemInCurrentPageOnly`、`switchBagTab`、`pageScanOrder`、`preferredStartPage`、
`rememberVisiblePage`、`rememberItemPage`、`isSearchablePage`、`itemCacheKey`、`bagCacheKey`、
`layoutName`、`displayPageOrder`、`executeSafeAction`、`throwIfStopRequested`、`throwIfInterrupted`、
`isInputWorkerThread`。Cloud 实现不得为了“适配”再叠 wrapper；上述主流程应在原 public/private ownership 下直接重建，
标准 checkpoint 直接用 `TaskCheckpoint`/Cloud exact context，不复制本地 `throwIfStopRequested` wrapper。

#### 1.5 每个 caller 与输入 lane

| HEAD caller | 调用 | 必须保持的边界 |
|---|---|---|
| `GiveItemService:40` | `findAndSelectItem(GIVE_BAG,template,known)` | Bag 自己 acquire/release；随后 give-button 是 caller 的另一输入段，允许与 HEAD 相同的段间插入 |
| `GiveItemService:60` | `findAndSelectItemDirectForExclusive(...)` | caller 已在自己的 exclusive callback；Bag 必须 join 同一 capability，不能 nested acquire；选物与 give-button 之间不得插入其它窗口输入 |
| `PlayerStateService:611` | context overload `findAndUseItem(MAIN_BAG,...,context)` | 普通 Bag whole-pass exclusive |
| `PlayerStateService:1295` | `bag/sheyaoxiang_item.png` 经 item-user | 摄妖香模板与右键语义不变 |
| `FiveRingTaskV2:969` | `findItemPageIndex(MAIN_BAG,"wuhuan/shoe.png",context)` | 买鞋后记页，只扫 0..4 |
| `FiveRingTaskV2:1239-1246` | `withMainBagOpen`，callback 先用香，再 `countItemUpTo(shoe,required)` | 一个 session/一次开关包；**用香在前、数鞋在后**；count 不得单独 acquire |
| `ReturnItemPrescanService:170` | `useCachedMainBagReturnItem` | cache owner 仍在 ReturnItemPrescanService；失败才由 caller invalidate |
| `ReturnItemPrescanService:250/252` | 两个 prescan 入口 | Bag 只产点；策略随机、时机、降级、inProgress/done 不得吸收入 Bag |
| `WubeiTask:3152` | `findAndUseItemFromBack(...wubei_probe_item.png,5,context)` | current first，再 5..0 |
| `WubeiTask:4599` | `findAndUseMainBagTaskPageItem(wubei_return_item.png,context)` | current first，再 task page；不扩成五页 |
| `XiuluoTaskV2:5306/5380` | `findAndUseMainBagTaskPageItem(xiuluo_return_item.png,context)` | 启动一次 probe 与 cached fallback 后一次 probe 的 caller 顺序不归 Bag 改写 |

HEAD 无生产 caller：无-context `findItemPageIndex`、无-context `findAndUseItem`、context overload
`findAndSelectItem`、`isMainBagOpen`、`MainBagSession.findItemPageIndex`。API 仍保留，不能据此删除。

普通 public find/select/use/reverse/prescan/cached/task-page/batch 入口都用
`InputSequences.submitExclusiveAndWait` 包住整个 open -> 多次 capture/decision/input -> close callback；callback 固定 return true，
业务值经 `AtomicReference` 带出。`isMainBagOpen` 不进 queue。`DirectForExclusive` 与 `MainBagSession` 是唯一 direct/join
分支；direct 内只用 `InputProvider`，这是已持有 input-worker authority 后的机械实现，不是第二把锁。

#### 1.6 capture/template/temp/checkpoint inventory

| 路径 | HEAD producer/consumer |
|---|---|
| open check | `tracker.updateGlobalVision()` -> `getLatestVisionPath()`；同一 frame 依次匹配 primary、fallback A、fallback B、cunkuan |
| direct primary probe | `CoordinateHelper.findImageAbsoluteCoordinate(anchor,0.8)`；用于 `getBaseAnchor/isMainBagOpen` |
| page scan | `WindowScopedTempPath.resolve("bag_scan.png")` -> `tracker.captureToFile` -> `ImageFinder.findAll(path,"images/template/"+target,0.85,24.0)` |
| current-page scan | `WindowScopedTempPath.resolve("bag_scan_current.png")` -> capture -> `ImageFinder.find(...)` strongest match |
| template source | HEAD 同时依赖 cwd-style string path；迁移后必须消除 cwd owner，统一 Cloud packaged classpath resource |
| scale/absolute | HEAD 对 image-local match 做 `round(local/systemScaleRatio)+capture/window origin`；fallback/tab/grid offset 也按 scale 取整 |

`TaskCheckpoint` 在方法入口、循环、capture/match 前后；`InputActionScope.checkpoint()` 在 worker 内每个物理副作用前后；
`TaskSleep.sleepOrStop` 承载 120/500/700/1200ms。Cloud pause 必须 park 同一 whole-pass session/cursor，stop 必须 typed
unwind；不得把 `UNKNOWN` 或 stale revision 变成“未找到物品”。

### 2. HEAD 方法级业务矩阵与 Cloud/DHXY 切分

#### 2.1 `ensureBagOpened` / anchor / retry 的确切顺序

| 顺序 | HEAD 条件与结果 | Cloud 编排/CPU | DHXY retained typed mechanical op |
|---:|---|---|---|
| 0 | `autoManageUI=false`：refresh exact window state；失败 null，成功用 bound window base | 选择 GIVE 分支 | `REFRESH_BOUND_BASE`，返回 exact binding/base/scale；不传 HWND 对象 |
| 1 | MAIN initial fresh capture | 决定进入 open-check，按固定候选序匹配同一 bytes | `BAG_CAPTURE(OPEN_CHECK,GLOBAL_VISION_ARTIFACT)` |
| 2 | primary `0.8` 命中 | 同帧 local/scale -> anchor；cache anchor；条件式 mouse-away；return anchor | local cache write、pointer fact、需要时 input move+120 |
| 3 | primary miss，fallback A 再 B `0.8` | 首个候选命中即按 task tab index 5 反推 geometry anchor；**不点击 task tab**；随后同步骤 2 | 同上 |
| 4 | 前三者 miss，cunkuan `0.8` 命中 | `panelVisible=true,anchor=null`；initial 直接 null，**禁止 Alt+E** | 无输入 |
| 5 | 全 miss / confirmed capture failure | `notVisible`；读取本地 cached anchor，若有则 force mouse-away | state read + pointer fact + 可选 move input |
| 6 | first Alt+E | press Alt+E，wait 1200，checkpoint | `EXCLUSIVE_INPUT(PRESS_ALT_E,SLEEP 1200)` |
| 7 | fresh `after-alt-e-first` check | ready -> cache/mouse-away/return；visible-no-anchor -> null 且不再 toggle；notVisible -> 下一步 | fresh `BAG_CAPTURE` |
| 8 | late render | 只等 700，checkpoint，不加新 retry count | `EXCLUSIVE_INPUT(SLEEP 700)` |
| 9 | fresh `after-alt-e-late-render` check | ready -> return；visible-no-anchor -> null；全 miss -> 下一步 | fresh `BAG_CAPTURE` |
| 10 | second Alt+E | 再读 cached anchor 并 force move；press Alt+E，wait 1200 | local state/pointer + `EXCLUSIVE_INPUT` |
| 11 | fresh `after-alt-e-second` check | ready -> cache/mouse-away/return；否则 null。无第三次 toggle、无 TTL、无额外验证 | fresh `BAG_CAPTURE` |

open-check 的四种模板全部使用**同一张** fresh frame；只有 stage 变化才 fresh capture。capture confirmed-failure 在 HEAD
等价为 `notVisible`，因此仍走上述既有 first/late/second 分支；`UNKNOWN` 不是 capture failure，必须停在当前 retained step，
不得据此按包未开继续 toggle。

#### 2.2 普通五页、known/cache、count/select/use

| 方法/primitive | 精确 HEAD 行为 | Cloud/CPU owner | DHXY owner |
|---|---|---|---|
| `preferredStartPage` | visible page 0..4 优先；否则 item page 0..4；否则 null | Cloud 用 local typed snapshot 作 hint | local map read owner |
| `pageScanOrder(preferred,skip)` | 有效 preferred 且 !=skip 先放；随后 0,1,2,3,4，跳过 skip/重复 | 纯 CPU `BagPageScanPolicy` | 无 |
| `findItemPageIndex*` | open；按 cache order 扫五页；首 match 记 item page并 return；全 miss null；finally close | Cloud loop/match/return | tab input、capture、cache commits、close input |
| known-first select/use | known 仅当 0..4；先 click tab+capture+match；命中先记 cache，再 item click；若 click 未成功则继续 remaining，remaining 明确跳过 known | Cloud branch/order/action choice | state commit、left/right input |
| no/invalid known | 不单独扫 known；按 preferred+0..4 | Cloud | 同上 |
| `countItemUpTo` | required=`max(0,required)`；0 立即 `(0,null)`；每页 findAll，最多取 remaining；首个非空页记 firstPage；每个命中页记 item page；达到 cap 立即停 | Cloud cap/累计/first page | capture + cache commit |
| `MainBagSession` | 重用一个 anchor/context/session；每次 member call 不开关包、不 acquire | Cloud callback 与 retained child operation cursor | 同一 local session capability |

每个 tab primitive 固定：计算 tab 点 -> 左键 hold/delay 100 -> 记 visible page -> settle 500 -> fresh grid capture。
`findAll` 必须按 score 降序，再按欧氏距离 `<24.0` 去重，最后按 `maxMatches` 截断；不能用文件枚举顺序、
不能改成“第一个像素命中”。item raw point = 同一 capture 的 resolved origin + `round(matchLocal/thatCaptureScale)`。

#### 2.3 current page、任务页与反向扫描

| 入口 | 精确页序 | match 后动作/cache |
|---|---|---|
| `findAndUseMainBagTaskPageItem` | 开包 -> current visible grid **不点 tab** -> miss 才点 index 5 并 fresh scan | strongest match；右键；index 5 不进页 cache；finally close |
| `prescanMainBagTaskPageItem` | 同上 | current 命中 source 后缀 `:current-page`；否则 task 页后缀 `:task-page`；不点击 item；finally close |
| `findAndUseItemFromBack(...max)` | clamp max 0..5；current visible grid first；未成功才按 max,max-1,...,0 | 右键；页 0..4 才记 cache，页 5 不记；finally close |
| `prescanMainBagItemFromBack` | 同上 | 首 match 产 cache point，source `:current-page` 或 `:page-<1-based>`；页 0..4 才记；finally close |

current frame 与随后某个 tab frame即使视觉上是同一页也必须保留为两次独立 capture；不得去重。task-page 入口不得扩成
0..4 sweep；reverse max=5 时必须包含任务页 5，再到普通五页。

#### 2.4 item click、cached use 与 cache point

- `executeSafeAction` 对 raw point 做 x/y 各 `[-10,+10]` 的 retained random jitter；USE 右键，SELECT 左键；
  click delay 100，settle 500；前/后 checkpoint 任一不成立时 false。random 点在首次 action binding 时冻结，重投同 bytes，
  绝不在 resume/redelivery 再抽一次。
- `useCachedMainBagReturnItem(null,...)` 在 acquire/open 前立即 false。非 null 时开包，**不切页、不匹配、不看
  templatePath/learnedAtMs/source、不加 TTL/二次验证**，直接对 cached screen-absolute raw point 执行 USE，finally close。
- prescan `toReturnItemCachePoint` 仅在 point 非 null 时创建：原 target template、screen-absolute raw x/y、当时
  `System.currentTimeMillis()`、原 source 后缀。Bag 不把“点击成功”升级为“已回城”；五倍/修罗的 map verifier 仍在 caller。

#### 2.5 return / exception / close / finally 矩阵

| 场景 | HEAD 返回/异常 | UI close |
|---|---|---|
| queue/exclusive callback 未开始 | 先 stop checkpoint、再 interrupted check；仍无异常则 find/prescan/batch=null，action=false | 不开包、不 close |
| `ensureBagOpened` / GIVE base refresh 返回 null | public business null/false | **不进入 try，故不 close**；即便此前发过 Alt+E 也如此 |
| MAIN open 成功后正常 match/miss | 返回对应 page/result/point/bool | 所有 autoManageUI 入口 finally 恰好一次 Alt+E+500 |
| GIVE layout | 对应结果 | `autoManageUI=false`，close no-op |
| callback/scan/match/runtime exception after open | exception 传播 | finally 仍先尝试 close；若 close 自身抛出，Java finally 的异常覆盖规则保持 |
| `withMainBagOpen` operation 返回/早退/抛错 | 原值/原异常 | open 成功后 finally 恰好一次 close |
| prescan 在 try 内 current/task/page early return | point/null | finally close |
| cachedPoint=null | false | 无 acquire/open/close |
| `isMainBagOpen` | primary anchor 是否命中 | 无 open/close |
| close 前 checkpoint false | baseline close helper 静默 return | 不发 Alt+E；不得补第二次 cleanup |

Cloud whole-pass 的 `RELEASE/ABORT` 是输入所有权协议，不等于 UI 的 Alt+E close。正常路径必须先完成上述 UI close（如适用）再
RELEASE；`UNKNOWN` close 不能宣称 UI 已关或 session 已释放。stop 的 exact terminal ABORT 只释放本地 owner，不新发未经授权的
cleanup input。

#### 2.6 最终职责矩阵

| 类别 | Cloud | DHXY |
|---|---|---|
| 业务编排 | open stage、候选优先级、页序、known/cache hint 使用、count cap、select/use、return/finally | 无业务分支 |
| 纯 CPU | classpath template decode、`ImageFinder.find/findAll`、fallback derive、same-frame point formula、页序、retained jitter | capture rectangle 的 live-scale 机械落地与 bounds check |
| 状态 | retained workflow cursor/action addresses；一次 invocation 内的 anchor/scale/branch | `visiblePageCache/itemPageCache/lastMainBagAnchorCache` 唯一持久 owner；pointer/window facts |
| capture/artifact | 只消费 typed bytes/hash/scale/observed binding/resolved origin；不见路径 | exact HWND capture、scale bracket、`global vision`/`bag_scan*.png` window-scoped 写入 |
| input | 只经 opaque whole-pass capability 提交 typed action bundle | normal FIFO、exclusive control lane、focus/binding/stop final gates、InputProvider |

### 3. Cloud API、typed mechanical seam 与 exclusive 边界

#### 3.1 public API 保持与 context 获取

Cloud `com.bot.dhxy.service.BagService` 保留第 1.3 节全部 public/nested API、参数、返回类型与
`MainBagSession` callback 形状。显式 `TaskExecutionContext` overload 直接使用 exact Cloud context；legacy 无-context overload 只能在
`CloudTaskRunAuthorityAssembly` 为当前 trusted caller 建立的非伪造 `BagInvocationScope` 内取得
`CloudTaskRunCurrentContextSlot.current()`。它不是 `WindowTaskContextHolder`、ThreadLocal window singleton 或 title search；没有 exact
scope 时 fail closed，不能退到第一游戏窗口或 `global` cache。

每次 public invocation 由 package-private `CloudTaskBagWorkflowAuthority` 绑定一个 opaque
`BagWorkflowInvocation`；`BagService` 只能消费它，不能构造 occurrence、requestId/actionId/sessionId、renew attempt 或 raw wire。
无-context API 的 source compatibility 因此保留，但 host/caller 未迁移前不激活。

#### 3.2 whole-pass exclusive 的精确范围

- 普通 find/select/use/reverse/prescan/cached/task-page/batch 入口：ACQUIRE 成功后，从 HEAD callback 的首个 checkpoint 开始，
  覆盖 open、所有 capture/CPU 等待、tab/item input 与 finally close；callback 等价返回值冻结后 RELEASE。
- `MainBagSession` 三个方法复用父 `withMainBagOpen` capability，以 retained child operation ordinal 区分，不 nested acquire。
- `findAndSelectItemDirectForExclusive`：Cloud 不检查 thread name。若 trusted caller scope 已携带同 exact
  `(scope,taskRun,window,stopEpoch)` 的 active capability，则 join；没有 capability才走普通 self-acquire。DHXY input-worker 是否真的持有
  owner 仍由本地 authority 校验，Cloud 不能伪造“我是 worker”。
- `GiveItemService` 普通入口保持 Bag release 后 caller 再点 give；direct 入口保持一个 caller-owned session 覆盖 select+give。
- `isMainBagOpen` 只有一个 retained capture，不 ACQUIRE whole-pass。
- 以上硬依赖 R-X1/R-X2/R-X3。当前 R-X0 只有 package-private state policy，**不能**据其宣称 Bag 已可运行，也不能让 Bag
  自建第二个 exclusive registry/queue/session ID。

#### 3.3 retained typed Service port，不开放 raw 面

未来 `CloudTaskServicePort` 只增加 opaque Bag action/capability projection；Bag body 可见的机械形状固定为：

| typed op | 必要字段/结果 | 本地动作 |
|---|---|---|
| `readBagLocalState` | exact layout/template key -> visible/item page、cached main anchor、binding/geometry generation | 只读本地 maps；无路径/HWND |
| `writeBagLocalState` | `VISIBLE_PAGE`、`ITEM_PAGE`、`MAIN_ANCHOR` typed mutation | exact window key 幂等 put |
| `readBagPointer` | current logical pointer in window-client/screen space + exact binding | 本地 `MouseInfo` + live scale；无输入 |
| `refreshBagBase` | GIVE layout exact bound base/scale/observed binding | `refreshWindowState` 等价机械 fact |
| `captureBag` | `OPEN_CHECK/PRIMARY_ONLY/GRID/CURRENT_GRID`、layout geometry、base anchor、artifact intent | scale-before -> exact rect -> one HWND frame -> local artifact -> scale-after/post-binding fence |
| `executeInputInExclusiveInteraction` | opaque step handle、`WINDOW_CLIENT_PX` 或 cached point 的 `SCREEN_ABSOLUTE_PX`、ordered `InputActionDto` | R control lane；副作用前 exact binding/stop/session gate |
| `release/abort` | Service 不可直接铸；authority finally 驱动 | exact owner release only |

`captureBag` 是 Bag 所需的 typed mechanical projection，不把 target template 或 match decision发给 DHXY；GRID request只含已批准
layout 数值和 artifact enum，本地在同一个 scale bracket 内执行 HEAD 的 region 算式，并回传
`resolvedClientOrigin + resolvedScreenOrigin + systemScaleRatio + observedWindow + bytes/hash/size/provider`。这闭合了 prescan 的
screen-absolute point，同时不另读一份 geometry 拼帧。

Service 不得看到 `RemoteGameClientPort`、raw request/poll/outcome、`InputActionQueue`、callback、HWND、filesystem path、
cache map 或 action factory。`UNKNOWN/NOT_EXECUTED` 由 typed projection保留，不压成 boolean。

#### 3.4 outcome 映射

| typed outcome | Bag 处理 |
|---|---|
| OBSERVED capture + template match | 按本设计 branch |
| OBSERVED capture + no match | 这是唯一正常 template miss，可进下一候选/页 |
| confirmed local capture/artifact failure | 对应 HEAD `updateGlobalVision/captureToFile=false`：open check 为 notVisible；page/current scan为空并继续既有页序 |
| confirmed input未执行且 exact ACTIVE context 仍成立 | 对应 HEAD queue/checkpoint false 的 null/false 收口；不自动 renewal/retry |
| STOPPED / stale revision / binding foreign | typed unwind/abort；不构造业务 miss |
| UNKNOWN | 保持 action/session unresolved fence；不前进 open retry、下一模板、下一页、click、close 或 result；同 bytes 重投/等 exact late result |

### 4. exact context、workflow occurrence 与 action address

#### 4.1 stable identity 与 occurrence owner

每个 invocation 固定：

```text
RemoteTaskRunScope(tenantId,userId,deviceId,clientSessionId)
+ taskRunId/taskType
+ window(windowId,nativeHandle,processId,playerIdentityEpoch)
+ admissionStopEpoch
+ business callsite ActionAddress
+ bagWorkflowOccurrence W
+ exclusiveSessionId（需要 whole-pass 时）
```

`runRevision` 不进入 stable key；它与 `bindingGeneration/currentRunRevision/nextStep` 一起在 R 已批准的 same-session
pause handoff 中前进。W 由 Full R0 retained frontier 单调分配：同一 public API 重复调用只有在前一次 terminal/final-consumed 后才
得到 `W+1`；`UNKNOWN`、pause、transport redelivery 均保持同一个 W。`BagService`、random helper、host 与 caller 都不能
`UUID.randomUUID()` 或自行加计数铸 action identity。

`ActionAddress = (phaseCode="bag", actionSlot, occurrence=W)`。动态 page/stage/operation ordinal写进 actionSlot；同一 W
内 actionSlot 不复用。`withMainBagOpen` 的 callback member call 使用 retained `session-op-<ordinal>`；ordinal 只在该 member call 的
最终结果消费后前进，UNKNOWN 不前进。

#### 4.2 所有 open/close fresh action slot

以下 `<root>` = `entry-<public-method-or-caller-child>`；每一行是独立 retained address，条件未到达则不声明：

| stage | exact actionSlot |
|---|---|
| acquire/join | `<root>.exclusive.acquire`；direct join 用 caller 已有 session child，不新铸 acquire |
| GIVE base | `<root>.open.give.refresh-base.fact` |
| initial open frame | `<root>.open.initial.capture` |
| stage pointer | `<root>.open.<initial|after-first|late|after-second>.pointer.fact` |
| stage conditional move | `<root>.open.<stage>.pointer-move.input` |
| cached anchor read | `<root>.open.before-alt-e-<first|second>.anchor-cache.read` |
| forced pointer fact/move | `<root>.open.before-alt-e-<first|second>.pointer.fact` / `.pointer-move.input` |
| first toggle+wait | `<root>.open.alt-e-first.input` |
| first result frame | `<root>.open.after-alt-e-first.capture` |
| late wait | `<root>.open.late-render-wait.input` |
| late frame | `<root>.open.after-alt-e-late-render.capture` |
| second toggle+wait | `<root>.open.alt-e-second.input` |
| second result frame | `<root>.open.after-alt-e-second.capture` |
| finally UI close | `<root>.close.alt-e.input` |
| protocol terminal | `<root>.exclusive.release` / `.exclusive.abort`，由 authority late-bind exact bytes |

每个 pointer move/input bundle 的随机 target在该 slot首次 exact request binding 时冻结；若 pointer 已在安全区，该 fact 的
OBSERVED 结果冻结为“无需输入”，不声明 move slot。每个 open capture 都 fresh；同一 stage redelivery只能返原 bytes，不能再截图。

#### 4.3 扫描、cache、item action slot 模板

| primitive | exact actionSlot pattern |
|---|---|
| page-hint read | `<root>.<op>.page-hint.read` |
| known page tab/caches/settle/capture | `<root>.<op>.known-p<i>.tab.input`、`.visible-cache.write`、`.settle.input`、`.capture` |
| normal page i | `<root>.<op>.scan-p<i>.tab.input`、`.visible-cache.write`、`.settle.input`、`.capture` |
| reverse page i | `<root>.<op>.reverse-p<i>.tab.input`、`.visible-cache.write`（仅 i 0..4）、`.settle.input`、`.capture` |
| task page 5 | `<root>.<op>.task-p5.tab.input`、`.settle.input`、`.capture`；无 page-cache write |
| current visible | `<root>.<op>.current.capture`；无 tab/cache input |
| confirmed item page | `<root>.<op>.<known|scan|reverse>-p<i>.item-cache.write`（仅 0..4） |
| item click | `<root>.<op>.<location>.item-<select|use>.input` |
| count | `<root>.session-op-<n>.count.scan-p<i>...`；每页唯一 capture/cache slot |
| session find/use | `<root>.session-op-<n>.<find|use>...` |
| cached use | `<root>.cached-use.item-use.input`；无 scan/capture slot |

具体 `<op>` 固定为 `find-page`、`select`、`use`、`reverse-use`、`prescan-task`、`prescan-reverse`、
`task-use`；同一个 page 即便在 current 与 reverse/tab 两处出现也因 slot 不同而 fresh。tab click、visible cache write、settle、
capture 拆成独立 retained step，以保持 HEAD 的 click -> cache -> wait -> capture 顺序；session owner在这些 roundtrip 之间不释放。

#### 4.4 public invocation root

| public API | root / 特殊 occurrence |
|---|---|
| `findItemPageIndex` | `entry-find-page`; 每次调用独立 W |
| `withMainBagOpen` | `entry-main-bag-batch`; callback members 用 `session-op-n` |
| select/use ordinary | `entry-select` / `entry-use` |
| select direct | outer caller root 下 `child-bag-select-direct`; join outer W/session |
| reverse use | `entry-reverse-use` |
| task/reverse prescan | `entry-prescan-task` / `entry-prescan-reverse` |
| cached use | null 不分配；非 null 为 `entry-cached-use` |
| task-page use | `entry-task-use` |
| `isMainBagOpen` | `entry-is-open.primary.capture`，无 exclusive session |

#### 4.5 pause/stop/redelivery

- pause 只在安全 checkpoint把 `ACTIVE(g,r,nextStep=n)` park；不 close、不 release、不重置 wait/jitter、W、sessionId、
  anchor、page cursor 或已完成 step。resume 经 current-context slot 变为 `ACTIVE(g+1,r2,same n)`，继续同一 pass。
- 旧 revision request 永久 stale；new revision只 late-bind尚未绑定的下一 step。已绑定 request bytes不改 revision/ID/坐标。
- STOPPED 只经 exact terminal correlation走 ABORT；不返回 false/null让 caller误认为物品不存在。
- `UNKNOWN` 保留 bound request与 session fence，不前进 branch/cursor；verified `NOT_EXECUTED`也不自动换 requestId/actionId、
  不增加第三次 open、不重复同页。若 baseline分支把 confirmed capture failure当空帧，消费该 terminal后才使用**下一个预定义 slot**。

### 5. template/resource、artifact、坐标与 cache 单一属主

#### 5.1 Cloud template single owner

全部匹配只走现有 `CloudTemplateAssets.loadTemplate(new TemplateId(canonicalId))` 与 classpath loader；target public 参数
`bag/x.png` / `wuhuan/shoe.png` 只按 exact canonical规则映射为 `images/template/<target>`，不 trim成另一 ID、不接受
`..`/反斜杠/绝对路径。缺资源与 HEAD `ImageFinder` 读空模板等价为 no match；不能另查 cwd。

Cloud 与 DHXY 当前对应资源 SHA-256 已逐文件核对一致，本切片 **resources 写集=0**：

| canonical id | SHA-256 | HEAD 角色 |
|---|---|---|
| `images/template/bag/anchor_huanzhuang.png` | `dc8105e6ff9d44a10411b6326e5a343e04f8c841c0e7b392dfcfad1c454c9b26` | primary anchor |
| `images/template/bag/task_tab_fallback_a.png` | `6b7d2a22af53e7697b465f9a00c5f5fcf31a0c128026dae666e64f4953b2c665` | fallback #1 |
| `images/template/bag/task_tab_fallback_b.png` | `1de52a14aa771f06fb0d762c87bf2224f2dc35f57ef6aa9b18625decc14d57be` | fallback #2 |
| `images/template/bag/anchor_cunkuan.png` | `929fcc0cd47ed39db0832dd5c326e789635426ca4b74f585bc65eae74116d843` | visible-only extra anchor |
| `images/template/bag/sheyaoxiang_item.png` | `2bbc68195614ae7e4f89103b8e1d039f3814e2c303f00deb8b83ee47e0b80c1c` | PlayerState caller |
| `images/template/bag/wubei_probe_item.png` | `6c32bd4e3c5075f9339e04c6a0c91bbe56c69ea2aba525750e5c11f06060f62d` | 五倍 reverse probe |
| `images/template/bag/wubei_return_item.png` | `bc30f8ecc83486785b85d7a3863b7b97d8474f9adbbb4611b5a6dd5a926604f6` | 五倍 task return |
| `images/template/bag/xiuluo_return_item.png` | `b3b2215d300dacd2e48b6da9332352fe199c193983fe56dc2d73b9d8f6154bfc` | 修罗 task return |
| `images/template/bag/sousuo.png` | `8f2839197923dd8a63c67b5ab9d87f67ccbf92b5481e640aae06840aa394d18e` | 已打包但 HEAD BagService 无 direct caller |
| `images/template/wuhuan/shoe.png` | `e2938822c4c54cffacf3fff7b0e6e198bf80596714e262a623e94c9a6f1a9e25` | 五环鞋 |

Cloud 现有 `ImageFinder.find(BufferedImage,BufferedImage,threshold)` 可复用；需在同一类补一个
`findAll(BufferedImage,BufferedImage,threshold,minDistance)`，算法逐字复用 HEAD score sort + distance dedupe，禁止把 bytes落 cwd再调用
string-path overload。

#### 5.2 local artifact single owner

- `OPEN_CHECK/PRIMARY_ONLY` 的 frame与 tracker/global-vision artifact 由 DHXY adapter写；Cloud只拿 bytes/hash，不拿
  `getLatestVisionPath()`。
- tab grid 固定 local artifact intent `BAG_SCAN` -> `WindowScopedTempPath.resolve("bag_scan.png")`；current grid为
  `BAG_SCAN_CURRENT` -> `bag_scan_current.png`。artifact enum不是文件名/path fragment，实际 path只在 DHXY映射。
- artifact写失败保持 HEAD capture=false语义；不能出现“Cloud已 match，但本地 baseline temp写失败仍算成功”。
- 本设计不使用 `CloudArtifactStore` 保存第二份 Bag业务输入，也不把绝对路径、`Path`、目录枚举或下载 capability发云。

#### 5.3 same-frame coordinate/scale

所有 match 只用产生该 image bytes 的同一个 typed observation：

```text
screenX = capture.resolvedScreenOriginX + round(matchLocalX / capture.systemScaleRatio)
screenY = capture.resolvedScreenOriginY + round(matchLocalY / capture.systemScaleRatio)
clientX = capture.resolvedClientOriginX + round(matchLocalX / capture.systemScaleRatio)
clientY = capture.resolvedClientOriginY + round(matchLocalY / capture.systemScaleRatio)
```

fallback geometry：

```text
anchorClientX = taskTabClientX - round(layout.tabOffsetX / sameFrameScale)
anchorClientY = taskTabClientY
                - round((layout.tabOffsetY + 5 * layout.tabStepY) / sameFrameScale)
```

GRID rectangle由 DHXY capture owner在同一 live scale bracket内按 HEAD计算：
`start=baseAnchor+round(gridOffset/scale)`，`width/height=round(gridW/gridH/scale)`；outcome回传实际 resolved origin。
Cloud不另取 geometry fact拼另一时刻 origin/scale。普通 tab/item输入优先用 `WINDOW_CLIENT_PX`，由 local input-worker最后门转换；
prescan model所需 screen point只用 capture outcome同帧 screen origin。cached point仍按 HEAD `SCREEN_ABSOLUTE_PX`，本地最后门验证它属于
当前 exact bound window。

#### 5.4 cache invalidation/retention

| 变化 | 页 cache | main anchor cache | ReturnItemPrescan cache |
|---|---|---|---|
| pause -> higher runRevision，同 taskRun/window/binding | 保留 | whole-pass内保留 | 保留；HEAD key不含 revision |
| 新 taskRun、同 exact window | 保留，符合 service-wide HEAD cache | geometry未变可保留 | caller的 `PrescanKey` 含 taskRun，天然隔离 |
| windowId/nativeHandle/processId/playerIdentityEpoch 任一变化 | 不可读旧 key，清/隔离 | 立即失效 | caller key至少含 windowId/hwnd/taskRun；Bag不跨 key消费 |
| geometry generation变化 | page index仍可作 hint | 失效，禁止用旧 screen point移鼠标 | 本 CR不新增 TTL/二次模板验证；use时仍走 exact current-window副作用门 |
| scale变化 | page index保留 | 失效 | 不据 `learnedAtMs` 新增 expiry |

local store 的 exact key扩展 binding identity只阻止跨窗复用，不改变同一合法窗口的 page preference/order。cache永远只是首扫 hint；
它不是 item存在或 bag打开的业务事实。无 TTL、容量淘汰、新 verification/read 或 fail-closed business rule。

### 6. 可编译依赖 DAG、最小波次与精确文件表

#### 6.1 DAG 与激活门

```text
现有 CloudTemplateAssets + capture-time systemScaleRatio（已存在）
  -> W-BAG-0 纯 CPU leaf：BagPageScanPolicy

P Full R0 final implementation
  -> R-X1 DHXY input exclusive session owner/lane
  -> R-X2 whole-pass wire/broker/local handler
  -> R-X3 Cloud authority/opaque capability/Service port
  -> W-BAG-M0 Bag typed mechanical projection
       (local state + pointer/base fact + same-frame Bag capture/artifact + WINDOW_CLIENT input)
  -> W-BAG-1 Cloud BagWorkflowState + BagService body + in-memory findAll（全部 dormant）
  -> W-BAG-2 caller cohort/host activation（另卡；本设计不授权）
  -> fresh runtime log/screenshot acceptance（另行用户运行）
```

R-X0 已 FINAL APPROVED 但只是一文件 policy leaf；Full R0正在并行实施。W-BAG-M0不得抢写 A/P/R 当前文件，必须等各自父级
FINAL APPROVED/hash稳定后独立认领。当前 worktree `BagService.java +134` 也是 DHXY最终 cutover 的硬冲突门。

#### 6.2 最小独立叶子波 `W-BAG-0`

Cloud New：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/bag/BagPageScanPolicy.java`。
该包级纯类只含 `normalOrder(preferred,skip)`、`clampReverseMax(max)` 与 page有效性；输入输出为 primitive/immutable list，
无 Spring、I/O、template、context、cache、port、host/caller。它可独立编译且不激活任何 runtime；首波只证明页序合同，不宣称
Bag whole-pass可运行。

#### 6.3 `W-BAG-M0` typed mechanical projection（未来独立基础波）

Cloud New：

| 路径/FQCN | 用途 |
|---|---|
| `.../remote/BagMechanicalCommandKind.java` | closed `READ_LOCAL_STATE/WRITE_LOCAL_STATE/READ_POINTER/REFRESH_BOUND_BASE/CAPTURE_OPEN/CAPTURE_GRID` |
| `.../remote/BagMechanicalRequest.java` | exact typed request；无 path/template/business verdict |
| `.../remote/BagMechanicalOutcome.java` | typed state/fact/capture projection，保留 execution state |
| `.../remote/BagLocalStateSnapshot.java` | visible/item/anchor hint + exact binding generation |
| `.../remote/BagLocalStateMutation.java` | idempotent visible/item/anchor commit |
| `.../remote/BagCaptureArtifactIntent.java` | `GLOBAL_VISION/BAG_SCAN/BAG_SCAN_CURRENT` allowlist enum |

Cloud Modify（等 Full R0/R-X3 稳定后同波）：

| FQCN/文件 | delta |
|---|---|
| `remote.RemoteOperation`、`RemoteRequest`、`RemoteOutcome` | closed union加入 `BAG_MECHANICAL` |
| `remote.RemoteCommandEnvelope`、`RemoteCommandOutcomeEnvelope`、`RemoteProtocolDigests` | strict payload/digest；未知/多余字段拒绝 |
| `remote.CloudTaskRunActionLedger`、`CloudTaskRetainedActionState` | operation-specific opaque retained handles；不开放 mint |
| `remote.CloudTaskServicePort`、`RemoteGameClientPort`、`CloudTaskRunCommandExecutor` | public typed facade -> package-private delegate；无 raw面 |
| `remote.RemoteGameCommandBroker`、`CloudTaskRunExecutionGate` | Full R0 admission/final-dispatch/final-consumed复用 |
| `remote.CloudTaskExclusiveInteractionAuthority`、`CloudTaskRunAuthorityAssembly`、`CloudTaskServiceExecutionContext` | Bag step加入同一 whole-pass capability；无第二 registry |
| `remote.InputBundleRequest` | whole-pass Bag input允许 `WINDOW_CLIENT_PX`；仍由 local最后门转换/校验 |
| `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 双仓 closed wire、artifact enum、same-frame resolved origin、cache state合同 |

DHXY New：

| 路径/FQCN | 用途 |
|---|---|
| `com.bot.dhxy.cloud.remote.RemoteBagMechanicalCommandKind` | Cloud enum同构 |
| `com.bot.dhxy.cloud.remote.RemoteBagMechanicalCommandPayload` | closed request payload |
| `com.bot.dhxy.cloud.remote.RemoteBagMechanicalOutcomePayload` | state/fact/capture closed outcome |
| `com.bot.dhxy.window.runtime.BagLocalRuntimeStateStore` | 三张 cache唯一 owner，exact binding key/失效 |
| `com.bot.dhxy.cloud.remote.BagCaptureArtifactWriter` | artifact enum -> tracker/WindowScopedTempPath；不接 caller path |

DHXY Modify（同一原子波）：

| FQCN/文件 | delta |
|---|---|
| `cloud.remote.RemoteGameOperation`、`RemoteGameCommand`、`RemoteGameOutcomeEnvelope` | closed operation union |
| `cloud.remote.RemoteOperationPayloadCodec`、`RemoteProtocolDigests` | strict codec/digest与 Cloud同构 |
| `cloud.remote.LocalRemoteGameCommandHandler` | exact binding/scale/capture/artifact/local-state producer；只做 mechanical |
| `cloud.remote.RemoteOperationLedger` | Full R0 claim/terminal/idempotent replay复用 |
| `cloud.remote.RemoteCoordinateSpace`、`RemoteInputBundleCommandPayload` | `WINDOW_CLIENT_PX` whole-pass输入传递 |
| `input.action.InputActionRequest`、`InputActionQueue`、`InputActionWorker` | 只在 R-X1/R-X2 owner最终结构上做 client->screen最后门；无 nested callback |
| `src/main/java/com/bot/dhxy/service/BagService.java` | **不在 M0 修改**；最终 cutover才把 cache ownership交 store。当前 +134冲突未解决前禁止触碰 |

schema/remote/digest/broker/input文件与 P/R 重叠，因此 W-BAG-M0 是**顺序化后续波**，不能由 S 现在实施，也不能把本表当成对
A/P/R当前写集的授权。

#### 6.4 `W-BAG-1` Cloud主体 dormant 波

| 仓库 | 精确路径/FQCN | New/Modify | 说明 |
|---|---|---|---|
| Cloud | `com.bot.dhxy.service.bag.BagWorkflowState` | New | retained open stage/page cursor/session-op ordinal/random decision；无 authority mint |
| Cloud | `com.bot.dhxy.service.BagService` | New | 本文整类 API/编排；不加 `@Component`，host不注册 |
| Cloud | `com.bot.dhxy.core.ImageFinder` | Modify | 仅加 BufferedImage `findAll`，算法与 HEAD逐字同构 |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskBagWorkflowAuthority` | New | package-private，assembly唯一构造；把 Full R0 occurrence/R-X3 capability投影给 Bag |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.host.CloudServiceConfiguration` | **0 修改** | 保持 dormant；本波不建 Bean |
| 两仓 | `images/template/bag/*`、`images/template/wuhuan/shoe.png` | **0 修改** | 已同 hash |

#### 6.5 `W-BAG-2` caller/host（非 S、另卡）

当前 Cloud 不存在 `GiveItemService/PlayerStateService/ReturnItemPrescanService/WubeiTask/FiveRingTaskV2/XiuluoTaskV2` 主体；
它们各自整类迁移属于独立 owner。只有这些 caller 的 persisted callsite occurrence 与 direct/join capability均到位后，才可 Modify
`CloudServiceConfiguration` 显式建 Bag bean并切流。不得为了“先接起来”在 host 手工 new service、复制本地 Window holder 或只迁一半 caller。

DHXY最终 cutover需由当前 `BagService.java` owner基于最新在途改动与本设计重新做三方合并：保留 local store/artifact/mechanical adapter，
删除/停用的业务编排只能在 fresh runtime验收后另卡处理；本 S 不预先决定删除当前 +134内容。

#### 6.6 后续双构建门与运行验收

- `W-BAG-0/W-BAG-1` 每次 Cloud Java落地：`D:/mavenProject/dhxy-cloud-brain` 执行其启动路径要求的 fresh
  `mvn -q clean package`；不得引用 stale jar。
- 任一 DHXY Java/schema/input/local-handler落地：`D:/mavenProject/DHXY` 执行 `mvn -q -DskipTests compile`。
- 双仓 wire波：两边均成功后才可 handoff；不支持 mixed version。
- no-local-test模式继续有效：本设计不新增/恢复 tests/source guards/replay testcase；运行验收看用户 fresh run 的
  `logs/dhxy-console.log`、window-scoped `bag_scan*.png` 与实际多窗输入隔离。本文按指令未运行 Maven/测试/host/Task/poller/UI/capture/input。

### 7. Worker S 自审（仅 QA，不构成 Approved）

- inventory 已覆盖六依赖、全部 public/private/nested API、常量、两类三张 cache、全部 HEAD caller、ordinary/exclusive/direct/
  input-worker、capture/template/temp/checkpoint。
- 业务矩阵保留五页、任务页、反向页序、known/cache、count/select/use/prescan、anchor候选、Alt+E两次+late wait、
  return/exception/finally与 direct join；未增加 TTL、probe、retry、fallback、verification、cleanup或 business gate。
- 每次 fresh capture/input/cache/fact均有 W + 唯一 actionSlot；UNKNOWN不前进，同 bytes重投，random只冻结一次；pause同 session续跑。
- Cloud不持有 `GameClientTracker`、`InputSequences/InputProvider`、`WindowTaskContextHolder`、HWND、
  `WindowScopedTempPath`、本地 cache/input-worker authority；没有 raw request/poll/outcome/path API。
- 资源写集为零，Cloud classpath单 owner；artifact留 DHXY；坐标只用 same-frame origin/scale；跨窗/binding cache不复用，
  revision内不凭空过期。
- 当前 worktree +134 只记录冲突，未修改/回滚/覆盖；A/B/P/R日志、Java/Maven/schema/resources/tests均零修改。

`无已批准业务差异；按基线等价迁移。`

Worker S 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED/Repair`。本节自审不算 reviewer approval。

## Parent Design Review #1 - BLOCKED / Repair #1 Published - 2026-07-13T05:22:00-04:00

父级以 `THIN_CLIENT_V1_FINAL_DESIGN.md:13-30`、现有三操作 `RemoteGameClientPort` 与 HEAD `0114604e` 复审。
inventory、caller/页序、Alt+E 两次+late wait、任务页/反向页、known/cache hint 顺序、direct/exclusive/finally、同帧
scale/坐标、UNKNOWN 不前进及资源 hash 均 PASS 并冻结。整体仍 **BLOCKED，P0=0/P1=3/P2=2**：

1. **P1：三张 Bag 业务缓存被继续留在 DHXY，形成第二业务权威。** Design:261-269、298-318、485-496、560 把
   `visiblePageCache/itemPageCache/lastMainBagAnchorCache` 交给本地 `BagLocalRuntimeStateStore`，并让 Cloud 远程 read/write。
   这三张表直接决定首扫页、已知物品页与开包前移鼠标分支，属于业务 hint/state，不是 binding/capture/input/safety fact；违反最终设计
   “云端持有全部业务状态，本地不保留第二状态机”。Repair 必须把三者迁到 Cloud 单一 owner：page caches 以 exact tenant scope +
   logical windowId + layout/template 保持 HEAD 跨 taskRun 的无 TTL hint；anchor 以 exact window tuple + geometry generation 保存
   window-client 坐标并在 binding/geometry 变化时失效。DHXY 不再提供 cache read/write API，只在动作落地前验证 exact window/坐标。
   `ReturnItemCachePoint` 仍由未来迁云的 caller owner 持有，不吸收入 Bag，也不得留本地业务 cache。
2. **P1：新增 `BAG_MECHANICAL` 把业务 layout/cache 合并成第四条 wire operation，越过已冻结的最小远程端口。**
   Design:527-573 要修改 `RemoteOperation/RemoteRequest/RemoteOutcome` closed union，request 还携带 Bag layout/state mutation；这既扩大
   全协议/ledger/broker，又让本地知道业务布局和 cache commit。Repair 固定只复用三条已有机械操作：
   `WINDOW_FACT` 增 closed pointer/bound-base facts，`CAPTURE` 接收 Cloud 已算好的 generic `CaptureRegion(WINDOW_CLIENT_PX)` 与 closed
   local artifact intent，`EXECUTE_INPUT_BUNDLE` 执行 whole-pass session 内动作。本地不收 target template、layout、page、match verdict 或
   cache mutation；`RemoteOperation` union 不新增 `BAG_MECHANICAL`。R-X session capability 只扩现有 port 的 authority envelope，不造 Bag raw 面。
3. **P1：无 context overload 依赖未定义的 ambient `BagInvocationScope`，可能重开 current-run/ThreadLocal 权威。** Design:273-283
   只说 trusted scope 能取 `CloudTaskRunCurrentContextSlot.current()`，却没有非伪造 owner、生命周期或 resume publication；这会让无参 API
   在并发 taskRun 下取错上下文。Repair 固定 Cloud `BagService` 为 assembly/activation 构造的 per-runtime 实例，构造时注入 exact
   `TaskExecutionContext` 与 package-private Bag workflow capability；无-context overload 只使用该实例绑定的 runtime，context overload
   必须验证同一 stable run。resume 由 current-slot 原子发布的新 runtime 构造新实例并复用 Cloud cache/workflow owner；禁止 static current、
   ThreadLocal、host registry、首窗口 fallback。
4. **P2：文件表声称要为 `WINDOW_CLIENT_PX` 修改已有类型，事实不符。** 当前 Cloud `CoordinateSpace.java` 与 DHXY
   `RemoteCoordinateSpace.java` 已同时包含 `SCREEN_ABSOLUTE_PX, WINDOW_CLIENT_PX`；Design:550、571 的
   `InputBundleRequest/RemoteCoordinateSpace/RemoteInputBundleCommandPayload` 不能仅为“允许该值”再改。Repair 删除这些伪 delta；若
   R-X authority envelope 未来需要 session capability，只列真实新增字段及 owner，不改坐标枚举语义。
5. **P2：`W-BAG-0 BagPageScanPolicy` 只是把两个私有 primitive 规则拆成无消费者的小类。** 它不形成真正协议/能力边界，违反
   不新增无意义 wrapper/abstraction 的约束。Repair 把 `normalOrder/clampReverseMax` 保持为未来 Cloud `BagService` 的直接私有逻辑；
   若需要一个当前可并行落地的纯 CPU 叶子，改选实际缺口 `ImageFinder.findAll(BufferedImage, BufferedImage, threshold, minDistance, maxMatches)`，
   并证明算法与 HEAD score-desc + `<24.0` 去重 + cap 完全一致，目标方法当前不存在且无 P2/A/B 写集冲突。

### 下一任务：`W-BAG-D2`（Design Repair #1 Delta）

Internal Worker S 只追加本日志，关闭上述 P1x3/P2x2；已通过的业务矩阵全部冻结。给出修订后的职责表、三操作 typed seam、
Cloud cache key/失效、per-runtime context owner、DAG 与精确文件表。Java/Maven/schema/resources/tests 继续冻结，不运行构建或运行面。
完成后等待父级复审；自审不算批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker S - Design Repair #1 Delta - 2026-07-13T05:25:24-04:00

本 Delta 只替换 Design #1 被 `Parent Design Review #1` 阻塞的 P1x3/P2x2；review 已 PASS 并冻结的
inventory、caller、页序、Alt+E 两次+late wait、任务页/反向页、known/cache hint 顺序、direct/exclusive/finally、
同帧坐标/scale、UNKNOWN 与资源 hash 不重开。本轮仍只写本报告，不实施任何源码/协议/资源。

### R1（P1-1）：三张 cache 全部改为 Cloud 单一业务 owner

撤回 Design #1 的 `BagLocalRuntimeStateStore`、`readBagLocalState`、`writeBagLocalState`、本地 cache mutation 及
“DHXY 保留三张 map”的全部设计。最终结构固定为一个 Cloud `CloudBagStateOwner`，持有与 HEAD 等价的三张表；
DHXY 不保存、读取、更新或返回任何 Bag 页/锚点业务 hint。

#### R1.1 exact Cloud key、value 与 lifetime

| HEAD state | Cloud key | value | lifetime / 失效 |
|---|---|---|---|
| `visiblePageCache` | `(RemoteTaskRunScope exact tenant scope, logicalWindowId, layoutKey)` | `pageIndex 0..4` | 不含 taskRunId/runRevision，无 TTL；保持 HEAD 同逻辑窗口跨 taskRun hint |
| `itemPageCache` | `(RemoteTaskRunScope exact tenant scope, logicalWindowId, layoutKey, canonicalTemplateId)` | `pageIndex 0..4` | 同上；template id 是 Cloud classpath canonical id，不是路径 capability |
| `lastMainBagAnchorCache` | `(RemoteTaskRunScope, RemoteTaskRunWindow exact tuple, geometryGeneration, MAIN_BAG)` | window-client `Point`，并保留产生它的 bound-base/scale correlation | HWND/process/player epoch 或 geometry generation 变化即不可命中并移除旧 anchor；无 TTL |

`layoutKey` 由 Cloud `BagService` 按对象身份固定为 `MAIN_BAG/GIVE_BAG`；public custom `BagLayout` 使用其 Cloud
canonical field tuple作 key，不把 layout 下发本地。`CloudBagStateOwner` 是 authenticated host/tenant scope 内的单一 owner，
由多个 per-runtime `BagService` 实例共享，所以 page hint 能跨 taskRun；它不是 static JVM global，也不跨 tenant scope。

`geometryGeneration` 由 Cloud owner 对同一 exact window tuple 的 closed `BOUND_BASE` fact进行单调版本化：初见 geometry 建 generation，
`x/y/width/height` 任一变化即前进 generation并删除旧 anchor。page cache不因 geometry/binding变化失效，因为 HEAD 页号是逻辑 hint；
anchor必须失效，因为它参与鼠标安全分支。pause/resume只换 runRevision，不改变上述 owner或 key，三张 cache均保留。

#### R1.2 与 HEAD 一致的 mutation 时点

- tab click得到 exact non-UNKNOWN `EXECUTED` 后、settle/capture 前写 visible page；任务页 5不写。
- item template在 Cloud CPU确证 match 后写 item page，并同步 visible page；只写 0..4。
- primary/fallback geometry anchor在 Cloud CPU确证后写 main anchor；cunkuan visible-only不写 anchor。
- `UNKNOWN`、template miss、capture未观察、输入未执行均不写对应 cache；重投同 action不会多造业务 transition。
- cache仍只是 preferred first-scan hint，不是“物品存在/包已开”事实；无 TTL、eviction、额外 verification或本地 fallback。

`ReturnItemCachePoint` 不属于上述三张 Bag cache。它继续由未来迁云的 `ReturnItemPrescanService` caller state按
`scope/window/taskRun/round/template` 持有；Bag只产/消费该 DTO，不吸收策略、时机、invalidate或成功判定。最终 Thin Client
不留这份业务 cache；原本地 caller仅在整体原子切换前继续作为生产基线。

### R2（P1-2）：删除 `BAG_MECHANICAL`，只复用三条既有 operation

撤回 `BAG_MECHANICAL` operation、六个 Cloud Bag wire DTO、三个 DHXY Bag wire DTO、所有 local state read/write command，
以及对 `RemoteOperation/RemoteRequest/RemoteOutcome` closed union增加第四分支的文件表。最终 `RemoteOperation` 仍严格只有：

```text
WINDOW_FACT | CAPTURE | EXECUTE_INPUT_BUNDLE
```

#### R2.1 `WINDOW_FACT`：仅新增 closed mechanical facts

| fact kind | closed payload | Cloud用途 | 本地禁止 |
|---|---|---|---|
| `BOUND_BASE` | `windowId,nativeHandle,processId,playerIdentityEpoch,x,y,width,height,SCREEN_ABSOLUTE_PX` | 建立 exact bound-base correlation、Cloud geometryGeneration、screen/client换算 | 不解释 layout/cache/bag-open |
| `POINTER_POSITION` | `windowId,nativeHandle,processId,playerIdentityEpoch,x,y,SCREEN_ABSOLUTE_PX` | Cloud按 HEAD判断 pointer是否已在 anchor右侧及计算 retained safe target | 不决定是否移动、不生成 fallback点 |

pointer不可读或 binding/geometry不成立时返回既有 typed `NOT_EXECUTED/FACT_UNAVAILABLE`；不返回伪 `(0,0)`。
这两个 variant 只扩 `WINDOW_FACT` 的 closed kind/fact union，不新增 remote operation。Cloud本地 cache read/write不经过该 fact。

#### R2.2 `CAPTURE`：generic ROI + closed local artifact intent

- Cloud用自身 `BagLayout`、Cloud anchor/cache 与已通过的同帧 scale公式，直接构造既有
  `CaptureRegion(WINDOW_CLIENT_PX,x,y,width,height)`；DHXY只把该 generic ROI落到 exact HWND并 capture。
- `CaptureRequest` 只增加一个 closed `CaptureArtifactIntent`：`NONE`、`GLOBAL_VISION_LATEST`、
  `BAG_SCAN_LATEST`、`BAG_SCAN_CURRENT_LATEST`。它是固定 local diagnostic slot，不是 path/fileName/path fragment。
- DHXY在同一次 capture stack内按 intent写现有 tracker/window-scoped artifact；写失败保持 HEAD capture=false对应的 typed失败。
  outcome不回传绝对路径，Cloud只拿 bytes/hash/size/provider/systemScaleRatio/observedWindow。
- request不携带 target template、layout、page index、match verdict、cache key/mutation或 click决定；本地不做模板候选/fallback。
- 已通过的坐标合同不变：match只用该 `CaptureOutcome.systemScaleRatio` 与请求的 client ROI origin；screen点使用同 exact
  `BOUND_BASE` correlation。local capture post-binding gate或后续 input final gate发现 window变化即拒绝，Cloud不拼错窗结果。

#### R2.3 `EXECUTE_INPUT_BUNDLE`：原 action DTO、whole-pass authority envelope

- Alt+E、sleep、mouse move、tab left-click、item left/right-click均继续使用现有 `InputActionDto` 与
  `EXECUTE_INPUT_BUNDLE`；Bag不新增 input action type或 local callback。
- input coordinate contract保持当前 `SCREEN_ABSOLUTE_PX`。Cloud依据 retained bound-base、same-frame match和已冻结 jitter生成
  exact点；DHXY在 input worker最后副作用门验证 exact window/坐标/stop/session，不改点、不换 fallback。
- R-X whole-pass只给现有 `CloudTaskServicePort` 三种调用包一层 package-private authority envelope：opaque
  `exclusiveSessionCapability`、`bindingGeneration`、`stepSequence`、`previousOutcomeDigest`及对应 retained step handle；字段/铸造/
  ACQUIRE/RELEASE/ABORT仍由 R-X owner，不由 Bag定义或暴露 raw request/poll/outcome。
- Bag action address、UNKNOWN不前进、pause同 session handoff继续沿用 Design #1已通过部分；不建立 Bag session registry或第四 wire lane。

#### R2.4 修订后三操作职责表

| operation | Cloud决定/持有 | DHXY机械执行/拒绝 |
|---|---|---|
| `WINDOW_FACT` | 何时请求 pointer/base；解释事实；更新 Cloud geometry generation | 读 exact binding/geometry/pointer，返回 closed fact |
| `CAPTURE` | ROI、artifact enum、模板候选、阈值、match、页序、cache mutation | exact HWND capture、scale bracket、固定 artifact slot写入 |
| `EXECUTE_INPUT_BUNDLE` | 原子 action list、坐标、jitter、等待与 next business branch | whole-pass input lane串行执行及最后安全门 |

### R3（P1-3）：per-runtime `BagService`，没有 ambient current

撤回未定义 `BagInvocationScope`、ambient `CloudTaskRunCurrentContextSlot.current()` lookup、static current、ThreadLocal、host registry、
`global` cache与首窗口 fallback。Cloud `BagService` 不是 application singleton `@Component`；它由
`CloudTaskRunAuthorityAssembly` / activation path **按一个 exact runtime实例化**。

#### R3.1 构造时绑定的不可变字段

```text
TaskExecutionContext exactContext
+ CloudTaskBagWorkflowAuthority.BagWorkflowCapability opaqueCapability
+ CloudBagStateOwner sharedCloudCacheOwner
+ CloudTemplateAssets
```

`BagWorkflowCapability` package-private且无 public constructor/accessor，绑定
`(scope,taskRunId,taskType,window,admissionStopEpoch,retained workflow owner)`；Bag只能请求已声明的 action handles，不能 mint
occurrence/session/request identity。构造不做 capture/input，也不注册 host bean。

#### R3.2 public overload行为

- 无-context overload只使用该实例的 `exactContext`；没有查询“当前 task”的步骤，因此并发 taskRun不可能串上下文。
- context overload先验证 passed context的
  `(scope,taskRunId,taskType,window tuple,stopEpoch)` 与实例 stable run完全相同，再验证它是该实例绑定的 current runRevision；
  foreign/stale context typed unwind，绝不换到实例 context继续。
- `findAndSelectItemDirectForExclusive` 只消费 capability中已由 caller/assembly绑定的 outer exclusive child；没有 child时按已通过的
  ordinary self-acquire语义执行。Cloud不凭线程名或 ambient状态猜 input-worker owner。
- `MainBagSession` 是同一 per-runtime `BagService` 产生的 child，持有同一 opaque capability与 retained operation ordinal。

#### R3.3 resume publication与 owner复用

pause时旧实例随旧 runRevision park，不被修改或重新发布。resume由 `CloudTaskRunCurrentContextSlot` 的既有原子 publication路径：

1. 以同一 stable run/retained action state准备 next `TaskExecutionContext`；
2. assembly构造新的 per-runtime `BagService` 与新 revision capability projection；
3. 新实例复用同一个 `CloudBagStateOwner` 与 retained `BagWorkflowState`，所以 cache、W、session cursor不重置；
4. current slot一次性发布新 runtime；旧实例/capability永久 stale。

没有可由普通 Service调用的 instance registry，也没有从 host按 windowId查 BagService的 API。

### R4（P2-1）：删除已存在 `WINDOW_CLIENT_PX` 的伪 delta

Design #1 文件表中以下修改声明全部撤回：

- Cloud `CoordinateSpace`：零修改；已含 `SCREEN_ABSOLUTE_PX,WINDOW_CLIENT_PX`。
- DHXY `RemoteCoordinateSpace`：零修改；已含同样两个值。
- DHXY `RemoteInputBundleCommandPayload`：不为“支持 WINDOW_CLIENT_PX”修改。
- Cloud `InputBundleRequest`：Bag不修改其 coordinate-space语义；当前 input继续 `SCREEN_ABSOLUTE_PX`。
- `InputActionRequest/InputActionQueue/InputActionWorker`：Bag不因坐标 enum修改；未来 whole-pass session能力仅由 R-X批准写集接入。

真实 Bag delta只有 R2 的两个 WINDOW_FACT variant与 CAPTURE artifact-intent字段；R-X authority envelope的真实字段归 R-X
owner并作为前置消费，Bag文件表不重复认领、重定义或伪装成坐标修改。

### R5（P2-2）：删除 trivial `BagPageScanPolicy`，首叶改为真实 in-memory matcher缺口

撤回并删除未来 `com.bot.dhxy.service.bag.BagPageScanPolicy` 文件计划。`pageScanOrder`、`isSearchablePage`、
`clamp reverse max` 保持未来 Cloud `BagService` 的 direct private逻辑，和 HEAD一样由真实 caller消费，不建 wrapper。

修订后的最小独立纯 CPU叶子为现有 Cloud
`com.bot.dhxy.core.ImageFinder#findAll(BufferedImage sourceImage, BufferedImage targetImage, double threshold,
double minDistance, int maxMatches)`。当前类只有 path-path `findAll(...)`，该 in-memory overload真实缺失；它消除 cwd temp/template
依赖并直接服务未来 Bag capture bytes。

算法合同逐项固定：

1. `source/target` null或 OpenCV Mat empty -> empty list；两 Mat始终 release。
2. `TM_CCOEFF_NORMED` 收集所有 `score >= threshold` 的模板中心点。
3. raw hits按 score descending排序；相同 score保持现有稳定排序结果，不另加坐标 tie-break。
4. 依次接受候选；它与任一已接受点欧氏距离 `< minDistance` 才视为 duplicate，`== minDistance` 仍接受。
5. dedupe后按正数 `maxMatches` cap立即停止；Bag caller仍先做 HEAD `safeMaxMatches=max(1,requested)`。
6. `BAG_ITEM_MATCH_RATE=0.85`、`minDistance=24.0`与 cap顺序不变；不做 resize、颜色预处理、阈值回退或资源读取。

这是现有算法类的一项真实能力补齐，不新建 abstraction/Bean/host/caller，且父级已确认与 P2/A/B当前写集无冲突；本轮仍不实施。

### 修订后的 DAG 与精确 New/Modify 文件表

#### D1. 依赖 DAG

```text
W-BAG-0I: Cloud ImageFinder in-memory findAll leaf（单文件、dormant）

P Full R0 final + R-X whole-pass authority final
  -> W-BAG-F0: existing WINDOW_FACT/CAPTURE typed delta（双仓、仍只有三 operations）
  -> W-BAG-C0: CloudBagStateOwner + BagWorkflowState（Cloud单一业务 state）
  -> W-BAG-S0: per-runtime Cloud BagService body（private页序；dormant）
  -> W-BAG-A0: assembly/current-slot resume publication + caller cohort + host activation（另卡）
  -> THIN_CLIENT_V1整体原子切换后的 fresh runtime验收
```

`W-BAG-0I` 不代表 whole-pass可运行；`W-BAG-F0` 必须等 P/R重叠文件 FINAL APPROVED/hash稳定后顺序实施。
主体、caller、host在本 Design Repair中继续 dormant。

#### D2. `W-BAG-0I` 单文件 CPU leaf

| 仓库 | 精确文件 | delta |
|---|---|---|
| Cloud | `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/core/ImageFinder.java` | Modify：增加第 R5 节 BufferedImage `findAll(...,maxMatches)`；无新类 |

#### D3. `W-BAG-F0` 三操作 typed delta

Cloud New：

| FQCN | 用途 |
|---|---|
| `com.yueyunfe.dhxy.cloudbrain.remote.CaptureArtifactIntent` | closed `NONE/GLOBAL_VISION_LATEST/BAG_SCAN_LATEST/BAG_SCAN_CURRENT_LATEST` |

Cloud Modify：

| FQCN/文件 | exact delta |
|---|---|
| `remote.WindowFactKind` | 增 `BOUND_BASE,POINTER_POSITION`；不增 operation |
| `remote.WindowFact` | sealed union增 `BoundBaseFact/PointerFact` closed records |
| `remote.WindowFactOutcome` | kind/variant strict matching |
| `remote.CaptureRequest` | 增 required `CaptureArtifactIntent` |
| `remote.RemoteCommandEnvelope` | strict request reconstruction接受新增 fact kind/artifact字段 |
| `remote.RemoteCommandOutcomeEnvelope` | strict重建两个 fact variant；capture outcome shape不增加 local path |
| `remote.RemoteProtocolDigests` | artifact intent与 closed fact进入既有 canonical digest |
| `remote.CloudTaskServicePort` | `capture(...)`显式接收 artifact intent；fact方法沿用 existing kind |
| `remote.RemoteGameClientPort` | package-private delegate透传 artifact intent |
| `remote.CloudTaskRunCommandExecutor` | 在既有 CAPTURE request中透传 intent |
| `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 仅三操作内的 closed fact/capture字段与原子 cutover |

DHXY New：

| FQCN | 用途 |
|---|---|
| `com.bot.dhxy.cloud.remote.RemoteCaptureArtifactIntent` | Cloud artifact enum同构 |
| `com.bot.dhxy.cloud.remote.RemoteBoundBaseFact` | closed exact binding/base/geometry fact |
| `com.bot.dhxy.cloud.remote.RemotePointerFact` | closed exact binding/pointer fact |

DHXY Modify：

| FQCN/文件 | exact delta |
|---|---|
| `cloud.remote.RemoteWindowFactKind` | 增两个 fact kind |
| `cloud.remote.RemoteCaptureCommandPayload` | 增 required artifact intent |
| `cloud.remote.RemoteOperationPayloadCodec` | strict encode/decode新增 enum/fact payload；不改 operation union |
| `cloud.remote.RemoteProtocolDigests` | 与 Cloud同构 canonical字段 |
| `cloud.remote.LocalRemoteGameCommandHandler` | `executeWindowFact`产 pointer/base；`executeCapture`按 closed intent写固定本地 artifact；无 Bag业务分支 |
| `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 与 Cloud表为同一个共享原子 schema delta |

明确零修改：两仓 `RemoteOperation/RemoteGameOperation`、`RemoteRequest/RemoteOutcome` operation union、
`CoordinateSpace/RemoteCoordinateSpace`、`InputBundleRequest/RemoteInputBundleCommandPayload`、DHXY `BagService`、所有 cache API、
resources/tests。R-X可能修改的 ledger/broker/input/session文件不由 W-BAG-F0重复列入或认领。

#### D4. Cloud state/body dormant wave

| 波次 | 精确 FQCN | New/Modify | 说明 |
|---|---|---|---|
| W-BAG-C0 | `com.bot.dhxy.service.bag.CloudBagStateOwner` | New | 三张 Cloud cache唯一 owner；tenant/window keys、geometry generation、无 TTL |
| W-BAG-C0 | `com.bot.dhxy.service.bag.BagWorkflowState` | New | retained open/page/session-op cursor与一次性 random决定；不持 HWND/path |
| W-BAG-S0 | `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskBagWorkflowAuthority` | New | package-private opaque capability；唯一 owner是 assembly |
| W-BAG-S0 | `com.bot.dhxy.service.BagService` | New | per-runtime、无 `@Component`、保留 public API；页序/clamp为 private |
| W-BAG-S0 | `com.yueyunfe.dhxy.cloudbrain.host.CloudServiceConfiguration` | 0 Modify | 不注册 Bag bean，保持 dormant |
| W-BAG-S0 | Cloud/DHXY template resources | 0 Modify | Design #1 hash结论冻结 |

#### D5. activation/resume wave（非 S、另卡）

| 仓库 | 精确文件/cohort | delta |
|---|---|---|
| Cloud | `remote.CloudTaskRunAuthorityAssembly` | Modify：为 exact runtime构造 BagService/capability，不建 global registry |
| Cloud | `remote.CloudTaskRunCurrentContextSlot` | Modify仅在 R-X最终 publication seam确有缺口时：原子发布含新 Bag实例的 next runtime；不加 ambient getter |
| Cloud | caller cohort：`GiveItemService`、`PlayerStateService`、`ReturnItemPrescanService`、`WubeiTask`、`FiveRingTaskV2`、`XiuluoTaskV2` | 各自迁移卡负责 persisted callsite occurrence与 direct child capability；Bag卡不代写 |
| DHXY | final Thin Client cutover | 删除/停用本地 Bag业务 state/branch须等当前 `BagService.java +134` owner三方合并及整体 fresh验收；本 D2不触碰 |

### Repair #1 自审（仅 QA）

- P1-1：三张 cache与 `ReturnItemCachePoint`最终业务 owner均在 Cloud；DHXY无 cache read/write/state machine。
- P1-2：`BAG_MECHANICAL`与第四 operation完全撤回；只扩现有 WINDOW_FACT/CAPTURE typed payload并复用 INPUT。
- P1-3：BagService为 exact per-runtime实例；无 ambient current/ThreadLocal/static/registry/首窗口 fallback；resume新实例原子发布。
- P2-1：删除两仓坐标 enum、InputBundle与input worker的伪 delta；input继续当前 screen-absolute合同。
- P2-2：不建 `BagPageScanPolicy`；最小叶为现有 `ImageFinder`真实缺失的 in-memory capped findAll。
- 已通过的 HEAD业务顺序、阈值、cache hint效果、随机点、close/finally、UNKNOWN与资源结论均未改；无 TTL、额外 probe/
  retry/fallback/verification/cleanup。
- 本轮唯一写入仍是本报告；Java/Maven/schema/resources/tests/运行面/Git与 A/B/P报告全部未修改。

`无已批准业务差异；按基线等价迁移。`

Worker S 完成 `W-BAG-D2` 后停止，等待父级复审；本自审不构成 `Approved`。

## Parent Design Review #2 - PARTIAL PASS / W-BAG-0I APPROVED / Repair #2 Published - 2026-07-13T05:36:00-04:00

### 结论

- R1 Cloud 三 cache 单一 owner、R3 per-runtime context、R4 坐标伪 delta 删除、R5 in-memory matcher：**DESIGN APPROVED**。
- `W-BAG-0I` 单文件 CPU 叶子 **批准实施**。
- `W-BAG-F0` 共享 wire 仍 **BLOCKED**：P0=0，P1=1，P2=2。主体/assembly/caller/host 继续冻结。

### P1-1：`artifactIntent` 与已批准 Quest wire 合同冲突，不能同时合入

- 证据：QuestManager Parent Review #5 已冻结 `CaptureRequest.artifactIntent` 为 **optional structured object**：普通 capture
  显式 null 且 NON_NULL 序列化省略该键，因而普通 request bytes/digest 零变化；quest 值形状为
  `{"taskCode":"XIULUO"}`。本 Repair 却另建 required enum `CaptureArtifactIntent`，包含 `NONE` 和 Bag slots，并要求
  `CloudTaskServicePort.capture(...)` 所有 caller 必填。
- 影响：同名字段 Java/wire 类型冲突；若普通请求携 `NONE`，所有既有 capture bytes/digest 被无关改写；A/S 两波无法顺序合并。
- Repair 条件：Bag 不再认领/重定义 `CaptureRequest.artifactIntent`。推荐只扩现有 closed
  `CapturePurpose/RemoteCapturePurpose` 为固定 Bag 机械用途（例如 `BAG_SCAN_LATEST`、
  `BAG_SCAN_CURRENT_LATEST`、确有 HEAD consumer 才保留 `GLOBAL_VISION_LATEST`），由 DHXY 对该 enum 的本地常量表选择
  window-scoped artifact；普通 capture 与 Quest optional `artifactIntent` 合同零变化。若坚持共享 artifact union，则必须先提交
  一份 A/S 共同可合并的单一 tagged shape、普通 null-absent digest 证明及两仓 strict field matrix，未经父级重新批准不得改 wire。

### P2-1：`CloudBagStateOwner` 的共享挂载仍需落成真实字段

- Repair 已说明 owner 由多个 per-runtime BagService 共用，但文件表只在 activation wave 泛称 assembly 构造 BagService。
- 实施约束：最终 D5 必须在 `CloudTaskRunAuthorityAssembly` 里持有唯一 `CloudBagStateOwner` 实例，runtime create/resume
  都引用同一对象；禁止每个 runtime/new revision 新建 cache owner。owner 内 key 必含 exact scope，clear/terminal 不跨 tenant。

### P2-2：geometry generation 必须拒绝乱序旧 fact 回写

- 多 invocation 的 `BOUND_BASE` outcome 可能按不同 action record 并发返回；只按“到达时几何不同就 generation++”可能让旧 geometry
  晚到后把 owner 倒回旧 base。
- 实施约束：更新 anchor generation 时必须在 owner lock 内验证 outcome 的 exact current context/revision 与 retained observation
  sequence；只接受不早于当前 accepted sequence 的 fact。旧/foreign revision 只能丢弃，不能前进 generation、删除新 anchor 或成为
  screen-point 基准。该 sequence 是同进程 ordering fence，不是业务 TTL/重试。

### 当前实施任务 `W-BAG-0I-IMP1 + W-BAG-D3`

Internal Worker S：

1. 只修改 Cloud `src/main/java/com/bot/dhxy/core/ImageFinder.java`，新增
   `findAll(BufferedImage,BufferedImage,double,double,int)`；算法严格按 Repair R5：row-major raw hits、稳定 score-desc、
   距离 `< minDistance` 去重、取前 `maxMatches`；`maxMatches<=0` 明确返回空，不读路径/资源、不新建类/Bean；
2. 同时只在本日志追加 Design Repair #2 Delta，关闭 P1-1/P2-1/P2-2；
3. 唯一 Java 写集为上述 1 Modify；P2/A/B、remote/wire/schema/Maven/resources/tests/host/caller 全冻结；
4. P2 仍写入期间不运行 Maven；交付 diff、自审与“构建待父级在 P2 稳定后统一执行”的明确声明。

请先追加真实 `CLAIMED` 再动 Java。自审不算父级批准；无已批准业务差异，按基线等价迁移。

## Internal Worker S - CLAIMED - 2026-07-13T05:38:52-04:00

- Task：`W-BAG-0I-IMP1 + W-BAG-D3`。
- Role：Internal Worker S，仅执行已批准的 Cloud CPU 叶子实现与 BagService Design Repair #2；不是 reviewer，自审不构成 `Approved`。
- 已完整读取本报告 EOF 的 `Parent Design Review #2 - PARTIAL PASS / W-BAG-0I APPROVED / Repair #2 Published`，并以其中批准算法及 P1-1/P2-1/P2-2 repair 条件为本轮约束。
- 唯一写集：Cloud `src/main/java/com/bot/dhxy/core/ImageFinder.java` 1 Modify；以及仅追加本报告。
- 冻结：P2/A/B、remote/wire/schema/Maven/resources/tests/host/caller；不修改、回滚或覆盖任何在途内容，不做 Git mutation，不创建 agent/reviewer。
- 构建门禁：P2 写入期间不运行 Maven；本轮交付源码 diff 与只读自审证据，构建待父级在 P2 稳定后统一执行。
- 业务差异：无已批准业务差异；按基线等价迁移。

## Internal Worker S - W-BAG-0I-IMP1 Implementation - 2026-07-13T05:44:22-04:00

### 实际写入

- Cloud `src/main/java/com/bot/dhxy/core/ImageFinder.java`：只新增
  `findAll(BufferedImage,BufferedImage,double,double,int)`，无其它 Java 文件变化；未新增 class、Bean、host 或 caller。
- 目标文件在领取前的 Cloud worktree 状态为既有 `??` untracked；本轮没有据此回滚、覆盖或纳入 Git，只在已读取的现有内容中追加一个方法。
- 与 DHXY 当前对应 `ImageFinder.java` 做只读 `git diff --no-index`，差异只有该方法的单一插入 hunk；未执行任何 Git mutation。

### 批准算法逐项落地

| Review #2 / R5 合同 | 实现证据 |
|---|---|
| 内存输入；不读 path/resource | overload 只接收两个 `BufferedImage`，只调用现有 `bufferedImageToMat` |
| null 或 `maxMatches<=0` 返回空 | 在创建/转换 Mat 前直接返回新空 `ArrayList` |
| Mat empty 返回空且 native 资源释放 | empty 分支位于 `try` 内；`source/target/result` 均由 `finally` release |
| `TM_CCOEFF_NORMED`、`score>=threshold` | 直接调用 `Imgproc.matchTemplate`，阈值判断无回退 |
| row-major raw hits | 外层 `y`、内层 `x` 收集中心点 |
| 稳定 score-desc，同分无坐标 tie-break | 仅用 score comparator；Java stable `List.sort` 保留 row-major 同分顺序 |
| 距离严格 `< minDistance` 才去重 | 复用欧氏距离判断；`== minDistance` 不进入 duplicate 分支 |
| dedupe 后取前 `maxMatches` | 每次接受非重复点后检查 cap 并立即停止 |

未加入 resize、颜色预处理、阈值回退、路径读取、额外匹配轮次或 caller 逻辑；方法当前保持 dormant，未激活 Bag 运行面。

### 构建与自审门禁

- 按 Parent Review #2 的明确约束，P2 仍写入期间 **未运行 Maven**；resources/tests 也全部冻结，未创建或运行测试。
- 因该门禁，本轮不声称 compile/pass。构建待父级确认 P2 写入稳定后统一执行。
- 只读源码自审未发现偏离上述批准算法的已知 P0/P1/P2；这是 Worker 自审，不构成父级 `Approved`。

## Internal Worker S - Design Repair #2 Delta - 2026-07-13T05:44:22-04:00

本 Delta 只替换 Repair #1 的 R2.2 artifact-intent 方案及对应文件表，并补实 D4/D5 的 owner 挂载和 geometry ordering fence。
Review #2 已批准的三 cache Cloud owner、per-runtime context、无 ambient current、无坐标伪 delta、in-memory matcher，以及更早已冻结的
HEAD 业务顺序、阈值、fallback、cache mutation 时点、UNKNOWN、whole-pass 与资源结论均不重开。

### D3-1（关闭 P1-1）：Bag 不认领 `artifactIntent`，扩既有 `CapturePurpose`

Repair #1 新建 required `CaptureArtifactIntent/RemoteCaptureArtifactIntent`、`NONE` 值及全 caller 必填参数的方案全部撤回。Bag 对 Quest
已批准的 optional structured `CaptureRequest.artifactIntent` **零字段、零类型、零序列化、零 digest 认领**；Bag 请求始终令该字段为 null，
沿用 NON_NULL 的 absent wire 形状。

只在既有 required `CaptureRequest.CapturePurpose` / `RemoteCapturePurpose` closed enum 中同构增加以下三个值：

| purpose | HEAD consumer / 语义 | DHXY fixed window-scoped artifact |
|---|---|---|
| `GLOBAL_VISION_LATEST` | `checkBagOpened` 的 `updateGlobalVision()` 后读取 latest vision，故有真实 HEAD consumer | `WindowScopedTempPath.resolve("latest_vision.png")` |
| `BAG_SCAN_LATEST` | 切 tab 后 grid ROI 的 `bag-scan` | `WindowScopedTempPath.resolve("bag_scan.png")` |
| `BAG_SCAN_CURRENT_LATEST` | 反向搜索前 current visible grid ROI 的 `bag-scan-current` | `WindowScopedTempPath.resolve("bag_scan_current.png")` |

没有 `NONE`。Bag 不发送 path、fileName、path fragment、template、layout、page、match verdict 或 cache mutation；DHXY 只以 enum 常量表/
closed switch 选上述固定 slot，在同一次 exact-HWND capture stack 写入。artifact 写失败继续映射 HEAD 的 capture=false typed failure；outcome
仍只回 bytes/hash/size/provider/systemScaleRatio/observedWindow，不回本地绝对路径。

#### 兼容矩阵

| capture family | existing `capturePurpose` | Quest optional `artifactIntent` | bytes/digest 与行为 |
|---|---|---|---|
| 普通既有 capture | `DIAGNOSTIC` 或 `CLOUD_SERVICE_INPUT` | null，因此 key absent | 字段和值均不变，request bytes/digest 零变化 |
| Quest capture | Quest 已选的既有 purpose | `{"taskCode":"XIULUO"}` | 完全归 A/Quest 合同，Bag 不读不写 |
| Bag capture | 上述三个 Bag purpose 之一 | null，因此 key absent | 只让既有 required purpose 字符串取新 closed 值，并选择固定本地 artifact |

既有 canonical digest 已覆盖 `capturePurpose`，因此不增加 digest 字段；strict enum decode 只扩闭集。Bag 不增加 port 参数：未来 caller 继续通过
现有 `capture(..., CapturePurpose, ...)` 参数选择 purpose。普通 capture 不因 Bag 出现新 key、新默认值或 `NONE`。

#### 取代 Repair #1 的 future wire 文件 delta

| 仓库 | 精确文件 | 修订后 delta |
|---|---|---|
| Cloud | `remote/CaptureRequest.java` | 仅给既有 nested `CapturePurpose` 增三个 closed 值；Quest optional `artifactIntent` 原样保留 |
| DHXY | `cloud/remote/RemoteCapturePurpose.java` | 同构增三个 closed 值 |
| DHXY | `cloud/remote/LocalRemoteGameCommandHandler.java` | 仅新增 purpose 到固定 window-scoped artifact 的 closed mapping；不含 Bag 决策 |
| 两仓 | `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 原子扩 existing capture-purpose enum 表；Quest optional artifact shape 不变 |

明确删除/零修改：Cloud `CaptureArtifactIntent`、DHXY `RemoteCaptureArtifactIntent` 均不创建；Bag 不修改
`CloudTaskServicePort`/`RemoteGameClientPort`/`CloudTaskRunCommandExecutor` 的签名，不为 artifact 修改 request/outcome envelope、payload codec、
`RemoteCaptureCommandPayload` 或 digest shape。两个 `BOUND_BASE/POINTER_POSITION` fact 的既有 W-BAG-F0 计划不受本节改变。

### D3-2（关闭 P2-1）：assembly 真实持有唯一 cache owner

最终 D5 在 `CloudTaskRunAuthorityAssembly` 落成明确字段，而不是由 activation wave 隐式约定：

```java
private final CloudBagStateOwner bagStateOwner;
```

- 该字段在 authority assembly 私有构造器中只 `new` 一次，生命周期等于该 assembly/coordinator object graph；不是 static、Spring singleton、
  host registry 或 runtime-local owner，也不提供 public lookup/get-by-window API。
- `createCurrentContextSlotActivation(...)` 构造初始 per-runtime `BagService` 时传入 `this.bagStateOwner`；
  `resumeTaskServiceRuntime(...)` 构造新 revision 的 per-runtime `BagService` 时仍传入同一字段。禁止在 `BagService`、`TaskServiceRuntime`、
  resume 分支或每个 `BagWorkflowState` 内再次 `new CloudBagStateOwner()`。
- `TaskServiceRuntime` 最终增加一个 immutable `BagService` 引用并随 slot 原子发布；旧 runtime 仍永久 stale。BagService 实例按 revision 更换，
  owner 对象不换，因此三张 cache 与 retained observation sequence 不因 pause/resume 重置。
- owner 的每个 cache/order key 第一部分都是完整 `RemoteTaskRunScope`（tenant/user/device/clientSession），后接 prior R1.1 的 exact window/layout/
  template/generation 维度；仅凭 `windowId` 或 `taskRunId` 不可定位或清理状态。
- 按 R1.1，三张 page/anchor hint 不在 task-run terminal 全清，它们保持跨 taskRun、无 TTL 的 HEAD hint lifetime。terminal 只释放 exact
  `(RemoteTaskRunScope, taskRunId, RemoteTaskRunWindow)` 的 run-specific `BagWorkflowState`/capability；任何 scope teardown 也只能在 owner lock
  内按完整 scope equality 删除该 scope 的 keys。不存在无参数 `clear()`，一个 tenant/window terminal 不能清空其它 tenant/window。

修订 D5 精确项：Cloud `remote.CloudTaskRunAuthorityAssembly` 必须 Modify 上述字段、单次构造以及 initial/resume 同对象注入；
`TaskServiceRuntime` 随同该文件持有 per-runtime BagService。`CloudTaskRunCurrentContextSlot` 仍仅在最终 publication 结构确有编译缺口时改，
不增加 ambient getter。

### D3-3（关闭 P2-2）：owner-lock observation sequence 拒绝乱序 geometry

`CloudBagStateOwner` 对每个 `(RemoteTaskRunScope exact scope, RemoteTaskRunWindow exact tuple)` 保留一个私有 geometry stream：

```text
nextObservationSequence
acceptedObservationSequence
acceptedBoundBaseCorrelation
geometryGeneration
```

sequence 是 assembly-shared owner 内的单调 `long`，跨 per-runtime/resume 保留；不是 wire 字段、时间戳、TTL、重试次数或业务版本。
实现时使用 `CloudBagStateOwner` 文件底部的 owner-private opaque observation handle，绑定 owner identity、exact scope/taskRun/window/
stopEpoch/runRevision、retained action-record identity 与分配到的 sequence，不新建 public registry 或 ambient context。

#### 发出与接收协议

1. 发出 `BOUND_BASE` 前，在 owner lock 内用注入的 per-runtime `TaskExecutionContext.revalidate()` 验证 exact ACTIVE/current revision；
   从对应 geometry stream 分配严格递增 sequence，并把它与该 retained action record 一起保存。随后释放 lock，再调用既有 `WINDOW_FACT`；
   owner lock 绝不跨 remote/broker wait。
2. outcome 返回后，在同一 owner lock 内再次对该实例注入的 exact context 执行 `revalidate()`，并逐字段验证 handle 的
   scope/taskRun/window/stopEpoch/runRevision、owner identity、action-record identity 与 outcome 的 exact observed-window correlation。
   这里不查 current slot、不读 ThreadLocal/static current，也不按 windowId 找“当前实例”。
3. context 已 stale、pause/resume 后成为 old revision、foreign scope/window/action，或 outcome correlation 不等时直接丢弃；它们不能更新
   `acceptedObservationSequence`、不能前进 generation、不能删除 anchor，也不能成为 screen/client 换算基准。
4. 合法 outcome 的 `sequence < acceptedObservationSequence` 时丢弃。`sequence == acceptedObservationSequence` 只允许与已接收 base
   完全相同的幂等 replay；同 sequence 不同 geometry 作为冲突 replay 丢弃且零 mutation。
5. `sequence > acceptedObservationSequence` 时原子接受：先把该 sequence/base 作为最新 observation；若 x/y/width/height 与 accepted base
   相同，只推进 ordering fence，不改 generation；若不同才 `geometryGeneration++`，并只删除同 exact scope/window 的旧-generation anchor，
   再安装新 correlation。任何异常都不能留下 sequence/base/generation 的半更新。

因此 invocation N+1 先返回会成为 accepted fence，随后才到的 N 必然被拒绝；resume 后旧 revision 即使携更大/更晚到 sequence，也先被
exact-current 检查拒绝。新 revision 从 owner 的下一 sequence 继续，不会因 runtime 重建回到零。该 fence不新增 capture/probe、park/yield、
retry/fallback、verification count、expiry 或业务分支。

修订 D4 精确项：`com.bot.dhxy.service.bag.CloudBagStateOwner` 除三张 cache外，同时拥有上述 geometry stream、owner lock 与 nested opaque
observation handle；`BagWorkflowState` 只保留 action/session cursor及 handle reference，不复制 generation/sequence owner。

### Repair #2 自审与交付状态

- P1-1：required enum artifactIntent 方案已完全撤回；普通/Quest capture wire不变，Bag只扩 existing purpose closed values。
- P2-1：assembly 的唯一 owner 字段、单次构造、initial/resume 同对象注入及 exact-scope terminal边界已落成设计合同。
- P2-2：sequence 分配、二次 exact-current 校验、`< / == / >` 接收规则和原子 anchor invalidation 已落成设计合同。
- `W-BAG-0I-IMP1` 仅实现批准的 dormant CPU leaf；`W-BAG-F0/C0/S0/A0` 仍等待父级复审/后续独立批准，本轮未实施。
- P2/A/B、remote/wire/schema/Maven/resources/tests/host/caller/运行面/Git 均未写；唯一源码写入仍是 Cloud `ImageFinder.java`。
- 无已批准业务差异；按 `0114604e` BagService 与 `docs/业务逻辑.md` 基线等价迁移。

Worker S 在此停止并等待父级复审；上述自审不构成 `Approved`，构建待父级在 P2 稳定后统一执行。

## Parent Source/Design Review #3 - SOURCE APPROVED / DESIGN APPROVED - 2026-07-13T05:49:00-04:00

### 结论

- `W-BAG-0I-IMP1`：父级源码审查 `P0/P1/P2=0`，**SOURCE APPROVED**。Cloud `ImageFinder` 只新增批准的
  `findAll(BufferedImage,BufferedImage,double,double,int)`；内存输入、row-major 收集、stable score-desc、严格
  `< minDistance` 去重、cap、null/`maxMatches<=0` 空结果与 native resource `finally` 释放均符合合同。P2 仍写入，故
  FINAL implementation approval 等父级 fresh Cloud package 后补，不以 Worker 的 standalone/自审替代构建门。
- `W-BAG-D3`：**DESIGN APPROVED，P0/P1/P2=0**。Bag 不再认领 Quest optional `artifactIntent`；仅按真实 HEAD
  consumer 扩既有 required `CapturePurpose` closed values。`CloudBagStateOwner` 由 authority assembly 单次持有并跨
  runtime/revision 复用；geometry sequence 在 owner lock 内分配并以 exact context/action/correlation 二次校验，旧 outcome
  不得倒写新 anchor/generation。

### 实施顺序与绑定修正

1. shared wire 波必须按 `P2 Full R0 -> Quest optional artifact intent implementation -> Bag CapturePurpose delta` 顺序方法级合并；
2. ordinary capture 的 null-absent payload/digest 必须保持逐字不变；Bag 不得新增 `NONE` 或第二个 artifact owner；
3. assembly owner 不按 runtime/revision 重建，local handler 只做 enum 到固定 window-scoped artifact 的机械映射；
4. 本批准不解冻 Bag 主体/host/caller，也不授权运行面。

Internal Worker S 本任务已完成，可关闭释放内部槽。**无已批准业务差异；按基线等价迁移。**
