# DHXY-cr271 死代码审计报告(2026-07-28)

> **执行记录(2026-07-28,用户圈定:A 档全做、images/ 任何文件不动)**:A1-A6 已实施,
> `images/` 与 28 张孤儿模板一张未动。7 批次逐批编译绿,test-compile 绿,
> TurnProtocolValidator 21/21、PathingPolicy 5/5、WubeiLocalDialogPreparation 1/1、
> KandaConstants 2/2 过。**跳过项**:`GiveItemService.executeGive`(与
> executeGiveDirectForExclusive 互为回退对,非零调用)、`TaskRetryPolicy`(删除需改
> TaskExecutionContext 核心构造,归 B2 写集)、GameContext 兼容层(归 B2)、
> `WindowCapacityPolicy` 仅删 evaluate+Decision(canRegister 等在用,整类判死系审计误报)、
> `rectToString/saveDebugImage/isOptionGreen`(有内部活调用,审计误报)。
> **适配**:GameClientTracker 构造器少一参(eyes),7 个测试夹具同步去掉首个 null 实参。
> **既有红不背锅**:BackgroundInputIsolationContractTest 的 NpcArrivalFrameFifo Ctrl 守卫红
> 系 W-696 Ctrl 悬停链引入,本次未触碰该文件。

四路并行审计(整类 / 成员级 / 资源与配置 / 遗留路径),全部基于两仓 grep 证据,未改任何文件。
红线全程遵守:`cloud/turn/protocol/**` 与 observation 协议类(两仓 byte-identical 合同)未触碰;
测试在用 = 在用;@Deprecated 带 retained/对照注释的只标注。

## 结构性事实(先读)

1. **`images/template/` 是云端的下载缓存镜像,不是权威源**(`TurnTemplateCache` 按 SHA-256 从云端拉取)。
   只删本地会被重新下载;真孤儿 = 两仓 src 零命中 **且** 云端资源树无副本。
2. 模板真正的消费方是云端(243 处 .png 字面量),本仓仅 ~40 处 —— 单仓 grep 不能作删除判据。
3. 6 个目录共 259 张模板是**整目录动态加载**(green_digits/map_label/map_names/coord_digits/
   sheyaoxiang_digits/cancel),单文件名永不出现在代码里,不可按零命中删。

---

## A 档 — 高置信、无保留决定,建议直接删(等圈定)

### A1. 整类(3 个,~440 行)
| 类 | 证据摘要 |
|---|---|
| `capture/FixedOcrMaskPreprocessor` | 全仓零引用;MapSurveyService 删除后遗留;javadoc 自称"Cloud file"却留在客户端,云端无同名 |
| `task/transaction/TaskTurnCoordinator` | @Component 无注入点;历史调用方 TaskTransactionRunner 已不存在。同包 Outcome/Result/YieldPolicy 仍被 AutomationMetricsService 用,**不可连带删** |
| Robot 视觉整簇:`driver/AWTScreenCapture` + `config/VisionProvider` 接口 + `GameClientTracker.eyes` 字段 | eyes 字段声明后零使用 → 注入它的唯一实现与接口传递性死;现役截图链是 BoundWindowCaptureService |

### A2. GameClientTracker 成员(7 项)
`LATEST_VISION_PATH` 常量 / `getFullWindowTitle` / `getGameHwnd` / `getLastCaptureAudit`+`CaptureAudit` record+`lastCaptureAudit` 字段(只写不读整链) / 私有 `currentBinding()` / `bringWindowToFront`+`bringWindowToFrontWithoutLock`(前台时代置前残留) / `testBackgroundAlt8`(调试残留)。
另:`captureToMemoryIfIdle` 是注释自陈的兼容空壳(1 个 caller),可内联;`logCaptureResult` 的 `"ROBOT"` 分支恒 false 可删。

### A3. BagService 旧预扫链(17 项)
`withMainBagOpen` / 2 参 `findItemPageIndex` / 4 参 `findAndSelectItem` / `findAndUseItem`×2 /
`prescanMainBagTaskPageItem` / `prescanMainBagItemFromBack` / `useCachedMainBagReturnItem` /
`captureMainBagTaskPagePrescanSnapshots`(+Exclusive 私有) / `matchMainBagTaskPagePrescanSnapshots` /
`findItemInPrescanSnapshot` / `ReturnItemPrescanSnapshots` record / `findAndUseMainBagTaskPageItem` /
`isMainBagOpen` / `BAG_TAB_CLICK_WAIT_MS` / `BagOpenCheck.visible()` 工厂。
生产全走 `BagLocalOperationExecutor` 的 guarded/exclusive 链。

### A4. InputSequences 薄包装死壳(12 项)
`submit` / `submitFrozenExactWindowActionsAndWait`(被调用方绕过) / `clickLeft` / `doubleRightClick` /
`pressAlt1/2/6/T/U/C` / `pressCtrlU` / `typeTextEnterAndScroll`。
存活确认:submitAndWait(9)/submitExclusiveAndWait(17)/submitBackgroundExclusiveAndWait(1)/frozen 两重载/moveAndClickLeft(3)。

### A5. 其他成员
- UICleanerService:`safeSource` / `clickAbsolutePoint`(连带 A4 的 clickLeft)/ 5 个无用 import;
  `GENERIC_CLOSE_TEMPLATE_DIR` 常量两代理结论冲突(见 B6),暂缓。
- CoordinateHelper:`MiniMapClickPoint` 孤儿 DTO / `findImagesInRegion`(仅自身日志字符串引用)。
- WindowObservationSampler:`SampleBatch.empty()` + 4 参便捷构造。
- InputActionWorker:`pressAltShortcut` 两个死形参 + 恒等三元分支(两分支实参逐字相同)。
- 窗口控制死包装:`WindowTaskControlService.startIndependentWindows/startSameTask×2/startSameQueue`;
  `WindowTaskContextHolder.current()`(全仓只用 rawCurrent,单窗口降级语义无消费者)。
- `GiveItemService.executeGive` 非独占旧入口(+ BagService.findAndSelectItem 4 参降级支路)。
- `GameContext` ThreadLocal 兼容层全部委托方法(生产只走 WindowRuntimeContext.getGameState)。
- `AutomationMetricsService` 本地生命周期埋点 4 方法(现役只有云驱动 recordWireEvent + UI writeDashboardNow)。
- `ImagePreprocessor` 死方法群(washYellowText 系/thin-white 系/binaryFingerprintDistance 等 ~10 个,
  云端已有对应实现,计划明令不留双实现)。
- 恒 false 开关死链:`hwndCaptureFallbackToRobotEnabled` 字段+访问器(零 caller;**application.properties
  那一行被 BackgroundInputIsolationContractTest 钉死必须留**);`TaskRetryPolicy` 整类;
  `WindowCapacityPolicy.evaluate` 从未被调(容量策略实际不生效——删或接线,二选一);
  `TaskRunProperties.loop/testMode/hasTasks`。

### A6. 资源与配置
- **28 个真孤儿模板**:`images/template/xinshou/` 整目录 25 张 + `bag/anchor_zhengli.png` +
  `battle/out_battle.png` + `dialog/wuhuan/wuhuan_cooldown_not_ready_story_raw_source.png`
  (⚠ 最后一张是洗字母图素材,删前确认不再需要重生成)。
- **`application.properties` 旧 `cloud.*` 块 118 个键**(非 cloud.turn.*;被 TURN-40 取代的上一代
  混合云配置)。删前扫 `scripts/run-cloud-brain-server.ps1`。
- **BotProperties 15 个无人读字段**及对应配置键(anchor_windowTo_map_* 4 / returnTeam* 7 /
  autoBattle*IntervalMs 2 / summonSkill 散件 3);另 `summon-skill-clean-*` 三键 yml/properties 重复定义。

---

## B 档 — 需对账或另立单,本次不删

| 项 | 原因 |
|---|---|
| B1 `DialogStoryAdvanceLocalMacroMechanics` | W-696 交付但从未接线;删前与卡对账、确认云端不下发对应 op |
| B2 `WindowRuntimeContext` ~60 个死 API 簇 + `GameContext` 兼容层 | 属 TURN-40F"thick-task 零引用退役"计划写集(plan.md:92 已列待办),按那张单做 |
| B3 `InputActionQueue` retained-session 6 个 API | 疑似 TURN 系列在建接口,对卡 |
| B4 `bot.team.*` 39 键 | 云端 TeamTaskProperties 同名绑定,需确认 sidecar 配置来源是否读本仓 yml(顺带:yml 800 vs 云端默认 500 的不一致) |
| B5 ~51 个"两仓皆零引用但云端已打包"模板(guzhu 10/Snipaste 母图/source_* 母图/p2_* 等) | 必须两仓同步删,且含裁剪母图,逐张圈定 |
| B6 `cancel/` 目录与 `GENERIC_CLOSE_TEMPLATE_DIR` | 两代理矛盾(常量死 vs 目录扫描),须先读 closeButtonTemplates 数组定案 x4~x7.png 命运 |
| B7 `BagOpenCheck.panelVisible` 语义塌陷 4 处不可达分支 | 逻辑失效而非无人用,需还原设计意图 |
| B8 UI 8 个开关无运行时效果(补给阈值/队长队员盒子/前置检查/三技能/修罗维护间隔) | **这是缺陷不是死代码**:用户可点但不进任何 turn 请求,单独立卡 |
| B9 `images/captures/` 运行期取证(占 19 万文件绝大头) | 运维清理,另行处理 |
| B10 `case.upload.*` 空转链 | 注释表明有意 opt-in,留 |

## C 档 — 标注不删
4 个 @Deprecated 旧测试流程成员(3 个注释明写保留);协议合同类;测试钉死的常量与配置行;
259 张动态加载模板;~160 张云端在用模板(含 map_label 是云端子集);Sampler 测试专用成员;
`locateWindow` 标题搜索(非完全不可达,建议只降 private)。

## 附:代理间冲突裁决记录
- AWTScreenCapture:整类审计判"活"(有注入点)× 遗留审计判 eyes 字段死 → 整簇传递性死,入 A1。
- `CoordinateHelper.initScaleRatio`:遗留审计误报零调用 → @PostConstruct,活,不列。
- `wuhuan_cooldown_not_ready_story_raw_source.png`:资源审计判死 × 它是 07-27 洗字模板的母图 → 留⚠标。
