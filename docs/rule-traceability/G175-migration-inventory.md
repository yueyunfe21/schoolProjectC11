# G175 公共帧交付机制：后续迁移位置清单

盘点日期：2026-09-10。性质：**源码盘点与后续计划，不是批量迁移授权、测试结果或稳定性结论。**
所属卡：`docs/PACKAGE_ARCHITECTURE.md` 的 G175（白龙马提示交付与公共帧邮箱）。

## 1. 判定范围与基线

- Client=`D:/mavenProject/DHXY-cr271`，Cloud=`D:/mavenProject/dhxy-cloud-brain`，均`dev`。
- origin/dev：Client `2236357413ffc64621626917ca4a66183895390d`、Cloud `c10078142bdc769717cf8980ffcf56e2e45da8aa`。这是本次本地已知推送基线，不宣称重新获取了远端分支。
- 当前Cloud `CloudWholeTaskObserver`、`CloudWindowObservationInbox`含G175本地实现；`DecisionEngine`、`NpcClickSmartQueueStore`、`SmartClickRecognizer`、任务文件也有其它卡未提交差异。清单以**当前源码调用链**为准，不恢复或修改这些差异。
- 核查边界：观察收件箱的全部外部ROI/同帧快照消费者；Observer图像解码和发布路径；`DecisionEngine.decide`图像服务分发；Cloud图像端口、任务/服务直接匹配；Client对应采样与本地操作。调试工具、离线语料、健康检查、纯资产/策略/文字排序不是在线帧邮箱迁移项。
- **按“谁算像素”和“交互生命周期”分类，不按类名是否含Cloud/Local分类。** Cloud仓里的`LocalOcrClient`调用的是Cloud进程使用的OCR，并不代表游戏客户端本地匹配。
- 不在每个`ImageFinder.find`、模板循环或OCR函数外各加一个队列。一次帧分析内部的多模板、多颜色、候选优先级必须仍是一次原算法；接入点在生产供图→识别调用→结果确认的边界。

## 2. 公共接线位置（不要重新寻找）

| 层 | 已核实位置 | 后续职责与限制 |
|---|---|---|
| Client采样与上送 | `window/observation/WindowObservationSampler.sample`、`sampleRoi`、`sampleCurrentDialogInterestFact`；`WindowObservationRunner`的`client.send`；`HttpsObservationClient` | 同cycle图、事实、窗口几何及交互身份一起供图；按兴趣限频。不得停掉本地观察或可靠事件。需要实际框时复用该cycle的框与像素，不重新截屏。 |
| Cloud收件箱 | `cloudbrain/observation/CloudObservationHttpHandler`→`CloudWindowObservationInbox.accept` | 同请求配对；待处理只保留最新，已领取样本不被新心跳替换；不对图片内容去重。 |
| 已实现公共状态 | `cloudbrain/observation/DialogFrameMailbox` | 一在途、一待处理；claim/commit/finish/close。**目前参数仍是`SequencedDialogSample`/`ObservationDialogInterestFact`，不是任意Tracker/NPC帧即插即用的API。** 非Dialog接入应先设计同等身份的载荷适配，不伪造Dialog兴趣。 |
| 现有首适配 | `cloudbrain/turn/runtime/CloudWholeTaskObserver.probeQueuedDialogAttention` | 只读`WUBEI_PROBE_STORY`已接；`prepareParkedDialog`保留业务识别顺序，路由OPTION仍走原门。其它操作没自动切换。 |
| 结果槽/确认 | `service/dialog/CloudDialogPreparedActionState.publishIfSlotUnchanged`、`takeConsumedReceipt`、`clearForRunOwnerActionId` | 使用publish返回actionId；只读结果可用精确消费结束，**物理动作不能把consume/收图ACK当执行成功**。已有执行/verifier/取消身份需逐行接入。 |

上述Cloud简写均以`src/main/java/com/yueyunfe/dhxy/`或`src/main/java/com/bot/dhxy/`实际包路径为准；下面列出每组具体入口。

## 3. 优先候选：连续供图／异步结果链（A组）

这些是白龙马稳定后优先迁移的边界；不是本轮已批准改动。

| ID / 场景 | Client生产→Cloud当前消费 | 后续改点 | 完成与清理门 |
|---|---|---|---|
| A1 通用对话观察：修罗、江湖历练、抓鬼、鬼王；五倍仍在云端的接任务；路线选项 | `WindowObservationSampler.sampleRoi`(`xiuluo-dialog`)+兴趣→Inbox→Observer `probeAttention` / `exactDialogObservationSample` / `sameCurrentDialogSample` / `prepareParkedDialog` / `prepareTaskInterest` | 复用G175入口但按operation建立适配；识别结果不能因后续无图心跳丢失。`isXiuluoCompatibleTask`明确列四任务，不把任务同族等同于每条对话都在云端。 | 只读与点击分别处理。入战/接任务/路线选择等要绑定原operation、attempt/round/intent及本地执行结果；不能直接复制STORY的consume-success。 |
| A2 Tracker绿链及任务盒：天庭、五环相关后台路径 | `sampleRoi`/标题伴随帧→Observer `probeTrackerPreparation`→Cloud `service/TaskTrackerPanelService.prepareTiantingPathingLinkFromObservation`、`prepareWuhuanPathingLinkFromObservation`→prepared槽 | 当前含`sameCurrentRoi`和标题事实相关门；提取“在途图＋结果保留”，不把任务盒变化判定/负结果当成可丢旧帧。五环按需标题链另见B2。 | 绿链实际点击/寻路确认，不是读取到结果即完成；保留天庭暗雷、taskBoxFingerprint、滚动一次性门、标题优先及negative语义。 |
| A3 新手对话优先＋Tracker | `sampleRoi`(`xinshou-dialog`/`xinshou-tracker`)+新手事实→Observer `probeXinshouPreparedActions`→`DialogService.prepareXinshouFirstBusinessOptionFromObservation`、Tracker `prepareXinshouPathingLinkFromObservation` | 接入帧结果保留时，不能把Dialog/Tracker变成互相独立可同时发动作的邮箱；保留同窗对话优先和原prepared槽。 | 复用已有新手机械动作终局、evidence/actionId、优先级释放与恢复；consume不证明领养/点击成功。 |
| A4 大理寺题目及答案提示 | Client `sampleUnknownPhasePresence`同批presence/bounds/ROI→Inbox `latestUnknownPhaseDialogSnapshot`→Cloud `task/dalisi/DalisiQuizTask`主循环→`answerOneQuestion`/`segmentOptionRows`/`QuizLibrary`；点击后`awaitHint`另抓帧 | **框存在本地判，题目/答案匹配在Cloud**，所以题目部分属于候选。以题目轮次和点击状态建立交互；点击后hint帧是另一时域，不能用点击前待处理帧冒充。 | accept送达门、答案点击返回值、hint学习暂存、失败预算和题目切换全部保留；本次不修其既有超时语义。 |

## 4. 已有类似机制：对齐／收敛评估，不叠第二层队列（B组）

| ID / 场景 | 已有生产链 | 后续方向与不能丢的东西 |
|---|---|---|
| B1 白龙马出现提示 | `DialogFrameMailbox`＋`probeQueuedDialogAttention`＋`WubeiTask.tryConsumePreparedWubeiDialog` | 当前首适配，仅编译、未测试未部署。先做用户授权的验收；实际框ROI裁剪仍待办。稳定以前不以它为已验证标准全站迁移。 |
| B2 五环对话需求、标题重读/rearm | Observer `probeWuhuanDialogDemand` / `consumeWuhuanTitleRearmDemand`；Inbox `requestPreparedFrame` / `acceptedPreparedFrame` / `completeAcceptedPreparedFrame` / `cancelPreparedFrame`；Client `ExactWindowPreparedFrameCapture` / `PreparedFrameCapture` | 已有demand、Cloud保留图/识别结果、消费回执。后续可抽共同状态，但不再外套mailbox；保留demandId/purpose/generation、title-first、旧轮退休、给鞋INSPECT槽交接。不是持续push的等价替换。 |
| B3 NPC智能点击（含白龙马头顶提示、黄字、候选、重供图与预期Dialog） | Client `NpcArrivalFrameFifoLocalExecutor`＋到达供图→`NpcArrivalFrameQueueEndpoint`→`DecisionEngine.npcArrivalFrameQueueResponse` / 到达帧入口→`NpcClickSmartQueueStore`→`SmartClickRecognizer`；结果经poll及outcome端点 | 这里已有**候选策略FIFO**、session、decisionId、CONTINUATION/END/WAIT、验证与续帧。图像邮箱不能替换候选顺序，更不能丢END或物理结果。要收敛只收“图像在途”，保留现有会话和验证；成功后停剩余候选的规则不重写。 |

## 5. Cloud同步／按次识图：登记候选，但不因在Cloud就强加队列（C组）

共用供图通常为Cloud调用`TurnGameClient`/`CloudTaskServicePort`→Client执行CAPTURE→`CloudTurnExchange`关联actionId/frame→Cloud算法；某些旧服务也接收`imagePayloadBase64`。单次调用本来等结果，没有连续帧积压时保留同步。下表只确认源码路径，不把兼容端点存在等同于所有任务当前都启用它。

| ID / 场景 | Cloud具体位置（省略`src/main/java`） | 后续处置 / 保留约束 |
|---|---|---|
| C1 前台Dialog识别、关键词/模板/最后fallback、医宝宝选项 | `com/bot/dhxy/service/DialogService`；`cloudbrain/remote/CloudDialogDetectionPort.detectDialog`、`CloudDialogOptionOcrImagePort.prepareOptionOcrImages`、`CloudDialogOptionOcrWordsPort.readOptionWords`、`CloudDialogWhiteStoryTemplatePort.observe`；`DecisionEngine.dialogPolicy`/`DialogOptionRecognizer` | 与A1后台入口分开；一次请求生成多个洗图/模板分析不另建队列。Cloud UI清理的fallback若在此识图，列入此行；纯本地关闭不迁移。框ROI问题单列，不顺带改模板和菜单顺序。 |
| C2 点击前验证 | `CloudDialogPreparedActionValidationPort`＋`DialogService`验证调用；NPC预期Dialog验证见B3 | 是**当前动作的验证图**，不能被“最新待处理”覆盖；没有完成验证就不能释放下次点击。不得因减少识图而删除原一次验证。 |
| C3 同步任务追踪器阅读/任务详情 | `service/TaskTrackerPanelService`；`DecisionEngine.trackerPanelRead`；`cloudbrain/TaskTrackerPanelService`；`QuestDetailTextRecognizer`、`ObjectiveTextRecognizer` | 图像阅读可复用传输身份；`TRACKER_LINK_RANKER`若仅排序已经提取的候选，不是独立供图入口。兼容reader按taskCode/imageMode分流，不能一刀切。 |
| C4 五倍目的地提示文字 | `WubeiDestHintRecognizer`、`DecisionEngine.wubeiDestHintReader`及`task/wubei/WubeiTask`提示解析调用 | 只读坐标/地图结果，对应一次提示交互；不要把不同目的地的新帧混为同链。 |
| C5 导航/世界地图/小地图/路线目的地校验 | `service/NavigationService`；`DecisionEngine.routeCandidate`/`routeMemory`/`minimapLocation`；`MiniMapRecognizer`；`CloudMapSurveyService` | 按打开地图、输入目的地、选路线、点击后的步骤分别关联；不是一张全局“最新导航图”。`NAVIGATION_ROUTE_PLAN`等纯图搜索/策略返回不走图像邮箱。 |
| C6 NPC辅助历史接口（排除现运行迁移） | `CloudNpcTaskTooltipPort`、`CloudNpcYellowTargetPort`、`CloudNpcPlayerAnchorPort`、`CloudNpcPreparedPointPort` | 全Cloud生产源码引用检索仅命中四个接口声明，无具体实现/注入消费者；注释也标注待共享集成。**不当成四条在线链，不列实施写集。** 当前NPC实际链按B3，不因接口名补造第二条链。 |
| C7 三技能/召唤兽静态槽识别 | `service/SummonSkillService` / `SummonSkillStaticSlotPolicy`；`SummonSkillRecognizer` / `DecisionEngine.summonSkill` | Cloud匹配的分支登记；Client封闭机械操作不搬。删除/点击前后帧不能混用，原槽扫描顺序和用户批准分支不变。 |
| C8 摄妖香/左上状态 | `service/playerstate/CloudPlayerStateIncenseStatusPort`、`CloudLeftTopTemplateMatcher`；`IncenseStatusRecognizer` / `SheyaoxiangStatusDecisionFacade` / `DecisionEngine.sheyaoxiangStatus` | 只读同步观测和使用道具后的验证分开。不能以旧状态永久完成整个run；Client本地对应状态事实排除。 |
| C9 人物位置/地图/血蓝及急救 | `service/PlayerStateService`；`CloudPlayerStateLocationPort.captureCoordinateStrip` / `PlayerStateLocationRecognizer`；`CloudPlayerStateFirstAidPort.captureBars` | Cloud仍调用的截图算法登记；主Runner本地坐标读数不迁移。位置是连续状态，不应一次消费后永远停止更新；需要观察周期/请求级适配而非STORY式整链完成。 |
| C10 团归按钮/队长信号/角色识别 | `CloudTeamReturnPortAssembly.observeButton`/`observeLeaderSignal`；`CloudTeamRolePreflightService.detect`/`readSuppliedLeaderId`；`DecisionEngine.teamRoleTooltip` | Cloud拿图匹配/OCR的部分登记；本地团队预判和zhao-watch不搬。按钮动作、队长等待、角色身份各自确认，不共用一条success。 |
| C11 通用盒子 | `CloudCommonBoxPortAssembly.observe` / `click`、`CloudCommonBoxPort` | Cloud匹配与Client点击分离的同步链；保留盒子删除/关闭时序及同action身份，不重复创建全局输入队列。 |
| C12 战斗/自动面板的Cloud同步补充路径 | `service/BattleRadarService.checkAndSyncCombatState` / `checkAndSyncMechanicalCombatSignal`、`AutoCombatPanelService.verifyAndAlignPanel` / `isAutoPanelVisible`；Observer `probeCombat` | Cloud有图算法不代表当前主任务都靠它；主Runner战斗事实/维护仍本地。不能为图像结果等待阻断战斗边沿，或把两帧运动/变化比较压成单帧。 |
| C13 天庭未知阶段图像补充、次数校准、封妖符文字 | `task/tianting/TiantingTask`的`latestRoi`补图与`ocrBannerText`；`TiantingDailyCompletionCalibrator.prepare` / `readConfirmed`；Client `sampleTiantingDialogProbe`上送`tianting-daily-count-first/fresh` | 次数校准是**同批一对证据**，必须作为整体，不能latest-only丢掉first。未知阶段presence本地、图像补充Cloud；封妖符每次坐标/滚动后文字各自关联。 |

补充底层与兼容面：`DecisionEngine.imagePreprocess` / `CloudNativeImageProcessor` / `ImageAlgorithms` / `TemplateMatcher` / `OpenCvTemplateMatcher` / `LocalOcrClient`属于算法、预处理或基础能力，不在函数内部加业务队列。`LegacyDecisionEndpoint`与`CloudTurnExchange`分别覆盖旧JSON与typed turn图像载体，登记为传输边界而不是另一业务队列。

## 6. 明确排除：本地匹配／非图片事实

| 本地链 | 已核实入口 | 排除理由 |
|---|---|---|
| 五倍入战选项 | Client `WindowObservationSampler.sampleWubeiEnterBattlePrepared`→prepared fact→Cloud `consumeClientPreparedDialog` | Client已匹配，仅上传准备结果；不等同于白龙马STORY云端识图。 |
| 天庭常驻/接取消/引妖香/封妖符等本地选项；鬼王/大理寺接任务探针 | Client `sampleTiantingDialogProbe` / `TiantingDialogLocalMechanics` / OptionSet | 匹配、重新确认及输入由本地机械链承担，云端消费事实不构成迁移资格。 |
| 修罗/江湖历练/抓鬼/鬼王接取消封闭本地操作 | Client `XiuluoAcceptDialogLocalOperation` / `JianghuLilianDialogLocalOperation` / `CatchGhostDialogLocalOperation` / `GhostKingDialogLocalOperation`，由`WholeTaskRuntimeLocalOperationExecutor`派发 | 本地模板匹配＋固定输入，不搬云端；同任务其它Cloud对话分支仍在A1/C1中。 |
| Dialog框存在、标题、停稳与坐标 | `DialogFramePresenceMechanics` / `UnknownPhasePresenceLocalMechanics` / `WuhuanPresenceLocalMechanics` / `LocalCoordinateStripReader` | 本地真值及事实生产保持原位；随事实附带图交Cloud分析的下游单列A/B，不删除生产者。 |
| 战斗/自动面板/归队观察 | `LocalCombatSignalMechanics` / `OpenCvLocalMaintenanceBroadcastHandler` / `TeamReturnZhaoWatchLocalMechanics` | 不因云端忙而停止采样，不将边沿事件改为latest-only图片。 |
| 本地UI清理、背包、给装备、任务面板机械操作 | Client `UICleanerService` / `BagService` / `GiveItemService` / `QuestManagerService` | 本地匹配部分不迁移；它们请求Cloud解析图片的下游按C1/C3另列，不能按整个Service粗暴排除。 |
| 新手本地anchor/恢复等 | `XinshouAnchorLocalMechanics` / `XinshouRecoveryLocalMechanics`及`sampleXinshouFact` | 本地机械判定与Cloud图像准备A3必须分开。 |
| 可靠事件、命令回报、策略与资产 | `ObservationKeyEvent`、action outcome、pathing/combat事实；`TASK_POLICY`/`TASK_RECOVERY`/能力开关/资产/记忆 | 不是可覆盖图像，必须保留各自可靠投递、顺序和幂等语义。 |

## 7. 后续实施顺序与每项验收义务

1. 先验B1白龙马，包括同帧＋新心跳、结果等槽、精确消费、取消/换run、相同内容新帧重试。当前用户未许可测试，不能报告这些已过。
2. A1按operation逐个接；A2/A3先补非Dialog身份/优先级适配；A4按题目与答案后的hint拆交互。不是改一个总开关。
3. B2/B3优先复用已有状态，不允许队列套队列。C组只有确认存在多帧在途/重复识别收益时再接，不为了统一外观强加等待。
4. 每项实施前对照本清单的producer、transport、consumer、completion及当时最新推送diff；本次盘点可免去重新全树找入口，但不能省略期间改动复核和业务基线授权。
5. 所有项必须写清：window/HWND/run/interaction/operation/attempt/round/intent；帧与框/窗口原点同批；已有结果不重算；采样订阅暂停不冒充业务取消；迟到回执不影响新链；只读结束与真实点击验证分开；超时不冒充失败。
6. 需要点击的行沿用现有click precheck和post-verifier，不能复制B1的只读consume完成；双帧证据按包保留；无图/无回执不可解释为任务已完成或可以接新任务。

## 8. 定向检索入口（复核期间差异用，不是测试命令）

从Cloud仓执行只读检索：

```powershell
rg -n 'latestRoi\(|latestUnknownPhaseDialogSnapshot\(|acceptedPreparedFrame\(' src/main/java -g '*.java'
rg -n 'decodeObservationFrame|sameCurrentDialogSample|sameCurrentRoi|prepareTaskInterest' src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java
rg -n 'case .* ->|case .*->' src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java
rg -l 'LocalOcrClient\.|ImageFinder\.|ImageAlgorithms\.|ImageIO.read\(' src/main/java/com/bot/dhxy/service src/main/java/com/bot/dhxy/task src/main/java/com/yueyunfe/dhxy/cloudbrain -g '*.java'
```

从Client仓对照`WindowObservationSampler`中的`sampleRoi`、`sampleWubeiEnterBattlePrepared`、`sampleTiantingDialogProbe`、`sampleUnknownPhasePresence`及`WholeTaskRuntimeLocalOperationExecutor`，先判真正的计算位置再归类。

本清单以源码可达形状和集中入口为证据，未运行任务、图像回放、测试或编译；旧兼容端点/少用分支的实际启用率未作运行验证。
