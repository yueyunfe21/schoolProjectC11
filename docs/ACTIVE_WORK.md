# DHXY Active Work

### Codex - 2026-06-17 五倍白龙马 tooltip 后必须等待 Runner 进战斗结果

Status: DONE / compile passed.

Context:

- 23:24 白龙马实测中，队长点击白龙马 tooltip 后只等了 `900ms` 的 `WUBEI_ENTER_BATTLE` prepared action。
- 前台在 `23:24:20.505` 判定 not prepared 并立刻进入 direct-combat fallback，`Alt+A` 被入队；Runner 在 `23:24:20.507` 才发布 `wubei.enterBattle.prove`，只晚了 2ms。
- 用户明确要求：白龙马 tooltip 点完后，必须等 Runner 对进战斗 dialog 给出回复，再按 Runner prepared 的结果点；不能因为 900ms 短等抢跑。
- `Alt+A` / direct-combat fallback 是必要后备逻辑，不能被删除。约束是：它不能抢在 Runner 回复之前执行；只有 Runner 明确回复进战斗模板未命中等失败结果后，才允许进入后备。

Changed:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
  - 绿色模板 prepare 增加 optional miss signal：当 OPTION dialog 可见但模板都没命中时，可以返回 `clickRequired=false` 的 prepared action。
- `src/main/java/com/bot/dhxy/task/wubei/WubeiDialogCatalog.java`
  - 新增 `wubei.enterBattle.notFound`，表示 Runner 已检查进战斗 dialog 但三个模板都未命中。
- `src/main/java/com/bot/dhxy/task/wubei/WubeiDialogPreparationProvider.java`
  - `WUBEI_ENTER_BATTLE` 模板 miss 时发布 `wubei.enterBattle.notFound`，不再让前台只能看到 absent。
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 接任务仍保留 `900ms` quick wait。
  - 进战斗 dialog 等待改为“等 Runner 回复”，不再使用 `900ms`，也不引入新的固定秒数。
  - 白龙马 story-confirmed tooltip 后，如果 Runner 没有回复，不允许直接进入 `Alt+A` fallback。
  - 只有 Runner 明确返回 `GREEN_TEMPLATE_NOT_FOUND`，才允许保留的 direct-combat / `Alt+A` 后备继续执行。

Verify:

- `mvn -q -DskipTests compile` passed.

### Codex - 2026-06-17 五倍 NPC 黄字颜色范围收紧

Status: DONE / compile passed / testcase replay passed.

Context:

- 用户指出 `降魔侍卫` 测试必须使用同一张宝象国原始截图，不能混入其它地点样本。
- 旧黄字候选会把任务追踪面板、底部自动战斗 UI 的亮黄字也洗出来，导致候选噪点过多。

Changed:

- `src/main/java/com/bot/dhxy/vision/GameTextLineOcrService.java`
  - 给 NPC/怪物点击专用黄字新增更窄的 `YELLOW_NPC_TARGET` 颜色模式。
  - 仅 `findYellowTarget(...)` 和黄字目标候选走新模式；普通黄字 OCR、route/dialog 黄字逻辑不变。
  - 新模式保留低亮度、近灰金色 NPC 名字黄字，排除任务面板和 UI 使用的高亮黄。

Verify:

- `mvn -q -DskipTests compile` passed.
- Testcase input:
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png`
- Replay command:
  - `java -Dfile.encoding=UTF-8 -cp <target/classpath.txt;target/classes> com.bot.dhxy.debug.YellowTargetFindReplayDebug images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png 降魔侍卫 images/test-cases/npc-click/jiangmo-guard-yellow-target/npc_tuned_proof_root_selected_line.png images/test-cases/npc-click/jiangmo-guard-yellow-target/npc_tuned_proof_root_marked.png images/test-cases/npc-click/jiangmo-guard-yellow-target/npc_tuned_proof_root_yellow_washed.png images/test-cases/npc-click/jiangmo-guard-yellow-target/npc_tuned_proof_root_yellow_overlay.png`
- Replay result:
  - `hit=true`
  - `center=(328,665)`
  - `click=(328,615)`
  - 任务面板黄字和底部自动战斗亮黄不再进入洗黄图。
- Proof images:
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/npc_tuned_proof_root_yellow_washed.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/npc_tuned_proof_root_marked.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/npc_yellow_before_after_contact.png`

### Codex - 2026-06-17 五倍降魔侍卫黄字候选不再被弱命中截停

Status: DONE / compile passed / testcase replay passed.

Context:

- 五倍队长接任务时，黄字扫描窗口本身没有问题；问题在 `findYellowTarget(...)` 的候选规则。
- 失败样本里旧逻辑先 OCR 到 `宝象国品侍卫`，因为它和 `降魔侍卫` 共享 `侍卫` 两字，被当成 yellow hit 后提前停止。
- 真实 `降魔侍卫` 在同一张 full-window 图的左下方，但视觉分低于 UI/自动战斗文字，需要继续扫描后面的候选才会被 OCR 到。

Changed:

- `src/main/java/com/bot/dhxy/vision/GameTextLineOcrService.java`
  - `findYellowTarget(...)` 对带目标名的精确黄字识别使用更大的候选池，但普通黄字 fallback 候选仍保持原 top3。
  - 严格目标（目前 `降魔侍卫`）的两字弱命中不再触发 early stop。
  - 候选评分改为强命中优先，弱命中只保留低分诊断，不能压过后续真正目标。
- `src/main/java/com/bot/dhxy/debug/YellowTargetFindReplayDebug.java`
  - 新增离线 replay 工具，直接调用生产 `findYellowTarget(raw, target, output)`，并输出标注图；不捕获窗口、不发输入。

Verify:

- `mvn -q -DskipTests compile` passed.
- Testcase input:
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/before_npc_yellow_candidates_overlay.png`
- Replay command:
  - `java -cp <target/classpath.txt;target/classes> com.bot.dhxy.debug.YellowTargetFindReplayDebug images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png 降魔侍卫 images/test-cases/npc-click/jiangmo-guard-yellow-target/selected_line_after.png images/test-cases/npc-click/jiangmo-guard-yellow-target/marked_after.png`
- Replay result:
  - `yellow-target-shadow index=3` still sees weak `宝象国品侍卫` but `strong=false` and does not stop.
  - `yellow-target-shadow index=4` hits `降魔侍卫` with `strong=true`, `dist=0`, `common=4`.
  - Marked output:
    - `images/test-cases/npc-click/jiangmo-guard-yellow-target/marked_after.png`
  - Selected OCR line:
    - `images/test-cases/npc-click/jiangmo-guard-yellow-target/selected_line_after.png`

Next:

- 下一轮五倍接任务实测看 `findYellowTarget done`：
  - `normalizedText` 应为 `降魔侍卫`；
  - 不应再出现 `宝象国品侍卫` 被选为 best；
  - 如果 full-window 黄字识别仍慢，再单独优化候选 OCR 次数，不和本次规则修复混在一起。

### Codex - 2026-06-17 NPC OCR ROI 必须同时包含黄字目标和紫色玩家名

Status: DONE / compile passed / testcase marked.

Context:

- 五倍队长在宝象国点 `降魔侍卫` 时，旧 vision memory 推荐了 `355,0 -> 902,407` 这种黄字单独成功过的 ROI。
- 这个 ROI 是从旧 `yellow-name` policy 里按黄字 `textRect/clickPoint` 加 padding 推出来的，保存时没有验证同一区域里是否有当前玩家紫色名字。
- 后续玩家锚点公式复用同一个 ROI 时，可能把区域里的其它紫字/tooltip 当成玩家 anchor，最终算出窗口外点击点。

Changed:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
  - NPC OCR 区域推荐只读取新的 `yellow-player-anchor-v2` ROI policy key；旧 `yellow-name` 和刚才错误写出的 `yellow-player-anchor` 都不再命中。
  - 旧 `yellow-name` ROI policy、旧 click sample 推导区域、legacy `MemoryEntry.recommendedRoi` 都不再作为 NPC OCR crop 来源。
  - 没有联合 ROI 时直接回退全窗口 masked scan。
  - learned direct-click policy 读取前会按当前规则重新刷新，只采纳已经被真实 dialog/template 消费确认的样本；未确认的预测点不再参与平均点和 spread。
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - 黄字策略在同一张区域截图里额外洗紫字并 OCR 当前玩家名。
  - 只有 `黄字目标命中 + 同区域玩家紫名命中` 的结果才允许写 ROI evidence。
  - ROI evidence 的 matched rect 现在会合并黄字目标框和玩家紫名框，推荐 ROI 会包住两者，而不是只围绕黄字缩小。
  - Runner-owned 的直接点击点学习改成 pending/confirm 两段：`clickNpcSmart` 只暂存点击点；任务后续真实消费到对应 dialog 后才写 click sample。
  - 公式点不按来源排除：公式点如果真的打开了对应 NPC dialog，会在确认阶段保存；如果没打开，就不会保存为成功点。
  - 直接点击点学习继续保留：策略实际发出点击并且后续被确认命中后，就记录 click sample，供下一次 learned-memory 直接点。
  - ROI 学习和直接点击点学习分开：ROI 必须有黄字+紫字联合视觉证明；直接点击点按上次点击结果学习。
  - 公式算出的点如果落在窗口外，直接跳过，且不再把坏点加入 Ctrl 探测来源。
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 五倍接任务 dialog 被 Runner prepared action 成功消费后，调用 `confirmPendingSmartClick(...)` 确认本轮 NPC 点击点。

Verify:

- `mvn -q -DskipTests compile` passed.
- Testcase input:
  - `images/test-cases/npc-click/joint-roi-jiangmo-guard/input_center_scan_layer1.png`
  - `images/test-cases/npc-click/joint-roi-jiangmo-guard/input_npc_yellow_candidates_overlay.png`
- Marked output:
  - `images/test-cases/npc-click/joint-roi-jiangmo-guard/marked_joint_roi_guard.png`
- Replay/mark command:
  - PowerShell `System.Drawing` marked old yellow-only ROI, yellow target hit, bad purple anchor, and off-window formula click.
- Log replay:
  - 对 `player:80,75` 的 direct-click policy 按新规则重算后，只剩 5 个黄字实测点：`(627,157), (629,155), (628,154), (628,155), (628,154)`，平均 `(628,155)`，`spread=2`。旧公式预测点 `(764,91)` 不再参与 learned direct-click。
  - 16:55 的全窗口联合样本日志显示黄字框 `595,198 -> 659,217`，玩家名框约 `482,425 -> 560,449`，v2 ROI 会用二者 union 后再 expand。当前全窗口截图已被后续小 ROI 覆盖，下一轮实测会补真实 v2 marked image。

Next:

- 下一轮五倍接任务看日志：
  - 第一次没有新联合 ROI 时，应看到 `regions=[0,0 -> 1024,768]` 作为 fallback，而不是旧 `355,0 -> 902,407` / `355,0 -> 900,408`。
  - NPC 点击后应先看到 `smart-click evidence pending runner confirmation`；只有接任务选项被消费后，才看到 `NPC click attempt recorded`。
  - `learned NPC point ready from policy` 应优先给出 `628,155` 附近的 direct-click point，避免每轮进入 7-8 秒黄字 OCR。
  - 黄字成功时应出现 `NPC yellow ROI joint-anchor check ... matched=true/false`。
  - 只有 matched=true 的样本才会生成新的 `yellow-player-anchor-v2` ROI policy，并且 ROI 应覆盖黄字和玩家名。

### Codex - 2026-06-17 修罗灵兽村特殊路线不再覆盖当前导航 intent

Status: DONE / compile passed / waiting live validation.

Context:

- 最新修罗启动后，队长从宝象国导航接任务 NPC。世界地图本轮输入和点击的是 `长安`，但 active pathing intent 被外层 `灵兽村` 覆盖。
- 结果 Runner 看到的是宝象国传送 OPTION，OCR 读到 `宝象国皇宫 / 平顶山胡杨林 / 大雁塔 / 我哪儿也不去`，却拿 `灵兽村` 去匹配，最终 `OPTION_KEYWORD_NOT_FOUND`，队长卡在传送 dialogue。

Changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
  - `navigateToMap(灵兽村)` 进入灵兽村特殊路线后，标记当前 pathing intent 由内部路线拥有。
  - 如果内部 `navigateToMap(长安)` 已经返回 `PATHING_STARTED`，外层 `finally` 不再用 `灵兽村` request 补注册 intent。
  - 张闻前当前地图走位显式设置 `targetMapName=长安`，避免“去张闻”这段也带着最终目标 `灵兽村`。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮修罗实测重点看宝象国 -> 长安传送菜单：
  - 应只看到 `targetMap=长安` 的 active intent；
  - Runner route prepare 应按 `长安` 匹配 `大雁塔` 或对应已记忆路线选项；
  - 不应再出现 `target=灵兽村` + OCR 文本为 `宝象国皇宫/平顶山胡杨林/大雁塔` 的 mismatch。

### 谢帅 - 2026-06-17 五倍白龙马恢复旧分支但保留 Runner 判定

Status: DONE / compile passed / waiting live validation.

Context:

- 用户指出白龙马原本逻辑是对的：显形镜后应按旧白龙马分流处理，不能被改成直接 Auto+A、直接回程、或卡死在第一条 prompt。
- 同时用户明确要求：dialog/story 仍必须由 Runner 后台判定，前台任务只消费 prepared action，不允许在 `WubeiTask` 里重新前台 OCR/模板识别。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 白龙马显形镜前注册 `WUBEI_PROBE_STORY` interest，使用显形镜后当前 turn 只短等 Runner prepared action。
  - 如果 Runner 暂时没有 prepared story，不再伪造成 `WHITE_TEMPLATE_NOT_FOUND`，也不切第二条 prompt；保持当前 probe，放权等待下一轮继续消费 Runner 结果。
  - 如果 Runner 明确给出 `target-ready`，沿用旧分支：点击白龙马 tooltip 后只消费 Runner prepared 的进战斗 dialog。
  - 如果 Runner 明确给出 `wrong-position`，沿用旧分支：回到当前绿字重新寻路。
  - 删除刚才误加的前台 `dialogService.handleDialog(...)` 兜底；`tryClickKnownEnterBattleDialog(...)` 现在只消费 Runner prepared action。
  - target-ready story 后如果 tooltip/dialog 路径没点进战斗，恢复旧逻辑：继续走 direct-combat / Auto+A 兜底。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮白龙马实测重点看：
  - 显形镜后若 Runner 尚未准备好，应看到 `probe story still waiting for runner`，而不是直接切第二条或回程；
  - tooltip 点击后优先等/消费 `WUBEI_ENTER_BATTLE` prepared action；如果没消费到，应走旧的 Auto+A/direct-combat 兜底；
  - 如果需要恢复“Runner 明确 no-story 后切第二条 prompt”，应先让 Runner/provider 能产出明确 no-story 结果，再接旧分支，不能由任务侧超时猜。

### 谢帅 - 2026-06-17 五倍白龙马超时不计暂停且不用任务道具回程

Status: DONE / compile passed / waiting live validation.

Context:

- 用户实测白龙马任务在暂停后恢复，实际 active 时间不到一分钟，却因为 `currentProbeTaskStartedAt` 使用 wall-clock 被判定超过 5 分钟。
- 超时恢复路径还调用了 `useReturnItemAndVerifyStartMap(...)`，会打开任务道具；但白龙马/五倍 probe 超时不能用任务道具回程，必须回到导航接任务流程。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 在 `runRoundPhases(...)` 的公共 `TaskCheckpoint` 后补偿 probe 计时器：暂停阻塞时间会加回 `currentProbeTaskStartedAt`，并同步补偿正在等待 runner story 的 `currentProbeStoryWaitStartedAt`。
  - `timeoutProbeTaskBeforeBattleIfNeeded(...)` 超时后不再调用 `useReturnItemAndVerifyStartMap(...)`。
  - probe 超时现在只清理当前 probe/战斗等待状态，然后跳回 `ROUTE_TO_MAIN_TASK`，让五倍按导航路线重新接任务。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮白龙马实测重点看：
  - 暂停恢复后日志应出现 `probe timer paused`，且不会立刻因为暂停时长触发 5 分钟超时；
  - 如果真的 active 超时，日志应为 `route back and reaccept`，不应再出现 `probe-enter-battle-timeout` 触发任务道具回程。

### 唐德 - 2026-06-17 队员战后快速补给恢复老补给输入方式

Status: DONE / compile passed / waiting live validation.

Context:

- 用户确认旧补给路径以前能加血，但新的“后台预检 + 排队 + 快速右键”路径实测没有补上。
- 旧补给输入方式是 `submitExclusiveAndWait("playerState:healAll", callback)`，由 input worker 在 callback 前 focus，然后在 callback 内直接 `inputProvider.clickRight(...)`，每次右键后等待 800ms。
- 新快路径不应回退成旧检测，也不应新增拿权后截图；用户要求检测仍然全部在后台做，但真实右键输入必须和旧补给一致。

Changed:

- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  - 保留 `needsFirstAidSupplyNoFocus(...)` 的后台预检和 `pendingNoFocusFirstAidPlan`。
  - `performCachedFirstAidPlanNow(...)` 执行阶段改回老补给同款输入方式：
    - `inputSequences.submitExclusiveAndWait("playerState:healCachedPlan", callback)`；
    - callback 内先 `moveMouseAwayBeforeBarsSnapshotDirect()`；
    - 对后台预计算目标逐个 `inputProvider.clickRight(absX, absY, 100)`；
    - 每次右键后等待 800ms。
  - 不在拿权后重新截图，不做补后复检；本次只修“新快路径的输入方式必须复用老补给输入层”。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮五倍/五环实测重点看队员战后补给日志：
  - 应出现 `按旧补给输入方式右键补充`；
  - 对应 input request 应为 `exclusive=true actions=0`，和旧 `healAll` callback 输入层一致；
  - 如果仍然肉眼无 focus/无补给，下一步应查 `WindowFocusService.focusWithoutLock(...)` 对该 hwnd 的真实 foreground 状态，而不是再改补给判断。

### 唐德 - 2026-06-17 NPC 点击前置清理旧 Story Dialog

Status: DONE / compile passed / waiting live validation.

Context:

- 一夜之秋在五环 V2 买鞋阶段卡住：窗口前面已有一个 story dialog，`NpcClickService.clickNpcSmart(服装店老板)` 前后都能检测到 `DialogType.STORY`，但旧逻辑只把它当成“已有 dialog，跳过若干策略”，没有清掉。
- Watcher 和五环 `HANDLE_DIALOG` 都能看见这个 story，但当前五环策略对普通 story 是 `STORY_IGNORED`，所以它会转去同步任务栏，而不会清理阻挡 NPC 点击的旧 dialog。
- 用户确认这是 NPC 交互的通用前置条件：准备点 NPC 前如果已有 story dialog，应先清掉，否则后续点击/tooltip/验证都会被挡住。

Changed:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - 在 `clickNpcSmart(...)` 的 `before-learned-memory` dialog 检测后增加前置清理。
  - 前置 dialog 判断优先读取 `WindowRuntimeContext.getVisibleDialogSnapshot(3000ms)` 的 runner 缓存；只有没有 fresh snapshot 时才 fallback 到 `detectDialogTypeNoFocus(...)`。
  - 如果检测到 `STORY`，调用现有 `DialogHandleRequest.clickStory("npc-click:pre-clean-story:...")` 清一次，再重新检测。
  - 如果清理后仍然不是 `NONE`，直接返回 false，避免继续跑 ROI/黄字/Ctrl 菜单制造无效点击。
  - 如果一开始检测到 `OPTION`，暂时只打日志并返回 false，不做通用 fallback 点击；option 可能是给予、购买、传送等业务选项，必须由业务层明确处理。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮看一夜之秋/买鞋场景日志：
  - 应出现 `NPC smart click pre-cleaned blocking story dialog`。
  - 如果 story 清掉，后续 `clickNpcSmart(服装店老板)` 应继续走 learned/tooltip/黄字等正常 NPC 点击策略。
  - 如果剩余 dialog 是 option，需要回到业务层判断是否应专门处理，不在通用 NPC 点击层乱点。

### 唐德 - 2026-06-16 队员战后补给改为 task-turn 排队等待

Status: DONE / compile passed.

Context:

- 用户要求本地战后加血不要再“慢慢等普通巡查碰运气”，而是队长放权后三秒内，已经后台预检好的队员必须尽快补给。
- 旧链路问题：
  - `AutoCombatService` 战后只把队员补给标成 `pendingFollowerFirstAid`。
  - 后续用 `TaskTurnCoordinator.tryRun(...)`，抢不到 task turn 就返回 false，然后 500ms 后再轮询。
  - 这导致队长放权后，pending 队员不一定已经排在 coordinator 等待队列里，视觉上明显慢于医宝宝广播。

Changed:

- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
  - 战后 `consumeExitAndRecover(...)` 后立即尝试执行 pending follower first-aid。
  - `runPendingFollowerFirstAidIfAllowed(...)` 不再使用非阻塞 `tryRun(...)`。
  - 改为调用 `TaskTurnCoordinator.enter(...)` 阻塞进入公平 task-turn 等待队列。
  - 删除 `pending follower first-aid waiting for task turn` 的 5 秒等待日志路径。
  - 保留 `PlayerStateService.performCachedFirstAidPlanNow(...)`，所以拿到权后仍然执行后台预计算好的快速右键计划，不重新扫四条血蓝。

Expected:

- 队员战后日志应先出现 `post-combat first-aid queued`。
- 如果队长仍持有 task turn，队员线程应进入 `task turn waiting ... pending-follower-first-aid`。
- 队长释放后，应尽快出现 `pending follower first-aid acquired task turn` 和 `执行后台预计算补给计划`。
- 下一轮实测重点核对 leader release 到 member acquired / clickRight 的间隔，目标是 3 秒内。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-16 五倍白龙马 hint 诊断截图防覆盖

Status: DONE / compile passed.

Context:

- 用户指出白龙马 `destination hint` 诊断截图会被固定文件名覆盖，导致失败现场无法复盘。
- 需要保留每一轮 `first-probe` / `second-probe` 的 raw/yellow 截图，后续才能判断是绿字没点中、story 挡住，还是截图区域问题。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - `clickTaskTrackerGreen(...)` 增加点击前/点击后 `DialogType` 日志，记录 `dialogBefore/dialogAfter`。
  - `wrong-position story` 分支日志增加 `storyStatus/storyAction/storyClicked`。
  - `destination hint` 截图文件名增加 `requestId`，避免 `wubei_tracker_destination_hint_first-probe_1_raw.png` 这类固定文件名互相覆盖。

Next:

- 用户下一轮白龙马实测后，直接查看对应 `requestId` 的 raw/yellow 图，确认 hint 没出现时屏幕上到底是什么。

### 谢帅 - 2026-06-16 五倍白龙马进战斗五分钟超时回城重接

Status: DONE / compile passed.

Context:

- 用户要求：如果当前五倍任务是白龙马/显形镜任务，从开始任务到进入战斗超过 5 分钟，就回城并重新接任务。
- 现有 `ENTER_BATTLE` 只有进入打怪阶段后的 180 秒等待超时，不能覆盖“接完任务 -> 点左侧绿字 -> 显形镜 -> 进入战斗”整条白龙马链路。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 增加 `PROBE_ENTER_BATTLE_TIMEOUT_MS=300_000`。
  - `READ_TRACKER` 识别到 `probeObjective` / 显形镜任务时记录 `currentProbeTaskStartedAt`。
  - 每个 phase 执行前检查该计时；超过 5 分钟且还没进入战斗时：
    - 关闭五倍队伍维护窗口；
    - 清理本轮 probe runtime / tracker hint / enter/wait battle runtime；
    - 使用回程道具确认回到宝象国；
    - 回到 `ROUTE_TO_MAIN_TASK` 重新接任务。
  - `ENTER_BATTLE` / `WAIT_BATTLE_FINISH` 首次确认 `IN_COMBAT` 时清掉白龙马超时计时。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-16 撤回五倍 STOPPED_AWAY 直接重走绿字尝试

Status: DONE / compile passed.

Context:

- 用户澄清五倍显形镜原循环应该保留：绿字点击后即使 runner 判定停下，也应先允许使用显形镜，再根据“位置不对” story 回到同一条绿字。
- 之前新增的 `STOPPED_AWAY && no hint -> TRACKER_PATHING` 分支会提前打断这个验证循环，方向不对。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 撤回 `resolveProbeAfterPathing(...)` 中的 `STOPPED_AWAY && currentTrackerDestinationHint == null` 直接回 `TRACKER_PATHING` 分支。
  - 保留原有 `useProbeItemWithRuntimeRecord(...) -> inspectProbeStoryOnce(...) -> wrong-position story -> TRACKER_PATHING` 流程。

Next:

- 继续查真正问题：哪些轮次没有从“位置不对” story 稳定进入 `TRACKER_PATHING`，或者进入后没有实际点击左侧绿字。

### 谢帅 - 2026-06-16 五倍显形镜绿字点击无移动不再用道具

Status: DONE / compile passed.

Context:

- 用户指出 `17:44:26` 五倍 first-probe 绿字点击后角色根本没有移动，不能继续猜测为 dialog/位置问题。
- 复查 `logs/dhxy-console.log`：
  - `17:44:26.090` 执行 `clickLeft x=297 y=332`。
  - `17:44:33.060` runner 仍读到 `current=火云洞(17,11)`。
  - `17:44:39.014` runner 更新为 `STOPPED_AWAY`，`observedStationaryMs=5954`。
- 结论：这次 tracker 绿字点击没有触发寻路；旧逻辑在 `STOPPED_AWAY` 后仍继续 `useProbeItemWithRuntimeRecord(...)`，导致原地使用显形镜并触发“位置不对”循环。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - `resolveProbeAfterPathing(...)` 中，如果当前 snapshot 是同一个 `wubei:tracker-green-click:*` intent，状态为 `STOPPED_AWAY`，且没有 tracker destination hint，则判定为“绿字点击没有产生移动证据”。
  - 该分支现在直接回 `TRACKER_PATHING` 重试当前绿字，不再继续用显形镜。
  - 新日志关键字：`probe tracker click produced no movement; retry current green link before using item`。

Verify:

- `mvn -q -DskipTests compile` passed.

### 何黎 - 2026-06-16 小地图单数字坐标与五倍 UNKNOWN 等待修正

Status: DONE / compile passed / minimap replay passed.

Context:

- 五倍显形镜路线停在 `火云洞 [4,3]` 后，`WindowTaskRunner` 反复写入 `WindowPathingState.UNKNOWN`。
- 根因不是角色仍在移动，而是 `MiniMapCoordinateReader.findBracketSpan(...)` 对坐标括号宽度有 `width < 35` 硬门槛。
- 实测 `[4,3]` 坐标括号 span 宽度为 33px，因此坐标在进入数字识别前就被拒绝。
- 五倍 `runResolveAfterPathingPhase(...)` 和 `resolveProbeAfterPathing(...)` 又把 `UNKNOWN` 当成 still active，导致 `probe runner pathing still active` 无限让权。

Changed:

- `src/main/java/com/bot/dhxy/vision/MiniMapCoordinateReader.java`
  - 增加 `COORD_BRACKET_MIN_WIDTH=30` / `COORD_BRACKET_MAX_WIDTH=80`。
  - 放宽括号候选最小宽度，保留后续逗号和两侧数字校验，覆盖 `[4,3]` 这类单数字坐标。
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - `UNKNOWN` 不再作为无限等待条件。
  - `ACTIVE` / `probeInProgress` 仍然继续让权等待 runner。
  - `UNKNOWN` 会打 warning，然后交回当前 phase 的恢复逻辑继续处理。

Replay:

- 已枚举的小地图离线 testcase 范围：
  - `images/test-cases/minimap/raw`：47 张原始小地图坐标条。
  - `images/test-cases/minimap/failure-location`：44 个失败样本目录。
  - 当前 repo 中没有发现其他小地图离线 replay testcase 目录；`MiniMapLabelLiveProbeDebugMain` 是 live probe，不属于离线 testcase replay。
- 新增失败样本：
  - 输入：`images/test-cases/minimap/failure-location/20260616_huoyundong_4_3/tmp_pos.png`
  - 元数据：`images/test-cases/minimap/failure-location/20260616_huoyundong_4_3/metadata.txt`
  - 标注：`images/test-cases/minimap/failure-location/20260616_huoyundong_4_3/marked.png`
- 命令：
  - `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.MiniMapCoordinateReplayDebugMain"`
- 结果：
  - `MINIMAP_COORD_REPLAY total=91 passed=91 failed=0`

Verify:

- `mvn -q -DskipTests compile` passed.
- MiniMap coordinate replay passed.

### 唐德 - 2026-06-16 降魔侍卫黄字直接点击强命中门槛

Status: DONE / compile passed / visual testcase pending.

Context:

- 用户确认五倍 `降魔侍卫` 数据已经清空，应该趁现在没有新数据时把弱/强判定修掉。
- 旧链路里 `GameTextLineOcrService.findYellowTarget(...)` 为了兼容短 NPC 名，允许两字公共子串/短名 fuzzy 命中。
- 这类弱命中适合作为 Ctrl 探测候选，但不适合直接落鼠标，也不应该写入 direct click policy。
- 历史备份里曾出现 `降魔侍卫` 目标下识别出 `lt2白龙马` 的 not-verified 记录，说明弱命中确实会污染五倍点击链路的判断。

Changed:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - 增加严格目标直接点击门槛：`降魔侍卫` 必须是完整命中，或至少 3/4 字的近完整连续命中且 edit distance <= 1。
  - 严格目标如果只是短名/fuzzy 弱命中，不再直接点击；返回 `TARGET_NOT_FOUND` 或 `TARGET_NOT_FOUND_WITH_CANDIDATES`，允许后续 Ctrl fallback 继续尝试。
  - 严格目标选词不再 fallback 到 all words；选不到强 span 时直接拒绝 direct click，避免红点被同一行噪点拖偏。
Invalidated replay:

- 原先使用的 `images/test-cases/npc-yellow-target/raw/wubei_130644_bailongma_ocr_boxes.png` 是根据 OCR boxes 做出来的坐标演示图，不是游戏真实 raw screenshot。
- 用户指出该图不能作为 testcase；已删除该 raw 图和对应 marked 输出：
  - `images/test-cases/npc-yellow-target/raw/wubei_130644_bailongma_ocr_boxes.png`
  - `images/test-cases/npc-yellow-target/output/wubei_130644_bailongma_ocr_boxes_marked.png`
  - `images/test-cases/npc-yellow-target/output/wubei_130644_bailongma_strict_replay_marked.png`
  - `images/test-cases/npc-yellow-target/output/wubei_130644_bailongma_as_jiangmo_rejected_marked.png`
- 当前只保留代码门槛和编译验证；黄字点击 visual testcase 需要下一次用真实游戏 raw screenshot 重新补。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-16 五倍 Auto+A 白龙马黄字点击点修正

Status: DONE / compile passed.

Context:

- 用户指出我上一轮生成的 marked 图明显没有命中 `白龙马`，红点落在上方噪点附近，不能作为验证。
- 复查 `13:06:44` 日志，`findYellowTarget` 确实命中 `白龙马`，但同一 OCR line 里还有噪点：
  - `LT@(463,479,458,476,10,6)`
  - `2@(325,553,318,548,14,11)`
  - `白龙马@(217,684,194,675,46,18)`
- 旧逻辑用 `centerOfWords(result.lineResult().words())`，把 `LT` 和 `2` 一起纳入外接框，导致旧点击点被拉到 `(331,534)`。

Changed:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - 黄字目标 OCR 命中后，先从 OCR words 中选择真正组成目标名的最小连续 word span。
  - 点击点和 evidence textRect 改为基于该目标 word span 计算。
  - 如果选不到目标 span，才回退旧的 all-words 行为并打 warning。
  - 日志增加 `targetWords`，下次可以直接看到实际用于点击的是哪几个 OCR box。

Coordinate Replay Invalidated:

- 原先这里引用的是根据 `13:06:44` OCR boxes 做出来的坐标演示图，不是游戏真实 raw screenshot。
- 用户指出该图不能作为 testcase；相关 raw/marked 图已经删除。
- 这条历史改动只能说明“all-words 外接框会被噪点拖偏”的代码原因，不能作为视觉验收。
- 下一次实跑必须保存真实 raw screenshot 和真实 marked output，再确认最终红点是否落在 `白龙马` 身上。

Verify:

- `mvn -q -DskipTests compile` passed.
- 追加真实 replay 输入：
  - `images/img.png`
  - 命令：`mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.bot.dhxy.debug.YellowTargetWordSelectionReplayDebug" "-Dexec.args=images/img.png 白龙马 images/temp/yellow_target_word_selection_marked.png"`
  - 输出：`images/temp/yellow_target_word_selection_marked.png`
  - 结果：OCR 同时读到中间说明文字 `selected target word:白龙马` 和底部真实 `白龙马`；修正后 `selected=[白龙马@(662,1197,570,1161,184,73)]`，不再选择中间说明文字。实际点击点仍按生产规则取目标文字中心上方 50px：`(662,1147)`。

### 唐德 - 2026-06-16 五环 ready 调度降抖二次修正

Status: DONE / compile passed.

Context:

- 用户新跑一轮后反馈视觉上仍然“很笨”：有窗口完成后，剩余窗口没有明显更快接上。
- 复查最新 `logs/dhxy-console.log`，上一轮调度改动确实触发：
  - `prepared action ready published` 出现 28 次。
  - `prepared-action-priority-yield` 出现 1153 次。
  - `pathing-terminal-priority-yield` 出现 901 次。
- 任务栏 prepared 到真实点击多数已在 200-600ms 内完成，说明 P1 prepared 点击链路本身有效。
- 主要问题变成调度抖动：
  - `WindowReadyEventBus` 仍按 `windowId:type` 只保存一条 latest，普通 `dialog-visible:STORY/OPTION` 会覆盖刚发布的 prepared action。
  - P2 `PATHING_TERMINAL` 在 `WAIT_PATHING` 窗口之间反复让权，造成互相等待感。

Changed:

- `src/main/java/com/bot/dhxy/window/runtime/WindowReadyEventBus.java`
  - 增加 prepared-action 专用 latest 缓存。
  - `latestOtherFreshPreparedAction(...)` 改读专用缓存，避免可执行 prepared action 被普通 STORY/OPTION ready 覆盖。
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
  - P2 terminal 优先级不再打断当前窗口的 `WAIT_PATHING` 阶段。
  - P1 prepared action 仍可让其他窗口优先消费。

Expected:

- 下一轮应明显减少 `pathing-terminal-priority-yield` 对 `WAIT_PATHING` 的刷屏。
- `prepared-action-priority-yield` 应更稳定地指向真正可执行的 `TASK_TRACKER_PATHING / ROUTE_TRANSFER`，不再被普通 `dialog-visible` 覆盖。

Verify:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-16 五环 prepared/terminal 调度优先级补齐

Status: DONE / compile passed.

Context:

- 用户观察到多窗口五环里存在“窗口已经有可处理 dialog / 任务 Panel / 停止移动，但迟迟拿不到权”的现象。
- 旧逻辑里 `WindowReadyEventBus` 只是软通知；五环看到其他窗口 ready 后只会 `ready-dialog-priority-yield`，但没有保证 ready 窗口马上被消费。
- 任务 Panel 绿字 prepared 成功后以前只缓存到 `WindowRuntimeContext`，没有统一发布带 `operation` 的 ready 事件，容易被普通 STORY/OPTION 事件淹没。
- 鞋铺/给鞋路线里 `STOPPED_AWAY + fresh ROUTE_TRANSFER prepared action` 应该按 P1 处理，不能只当 P2 terminal 等到 route prepared 过期。

Changed:

- `src/main/java/com/bot/dhxy/window/runtime/WindowReadyEventBus.java`
  - 增加 `latestOtherFreshPreparedAction(...)`：只返回带 `operation` 的可执行 prepared action ready。
  - 增加 `latestOtherFreshPathingTerminal(...)`：只返回 `ARRIVED / STOPPED_AWAY` terminal。
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
  - `ROUTE_TRANSFER` prepared、`TASK_TRACKER_PATHING` prepared、prepared action 重新验证成功时发布 `TASK_ATTENTION_REQUIRED`，并带上 `operation/target`。
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
  - 五环 ready 优先级改成：
    - P0：当前窗口 fresh prepared action，当前窗口先消费。
    - P1：其他窗口 fresh prepared action，当前窗口让权。
    - P2：其他窗口 fresh pathing terminal，当前窗口让权。
    - P3：普通公平兜底。
  - 泛 `STORY/OPTION visible` 不再触发跨窗口让权，避免大家互相空让。
  - `BUY_SHOES` 纳入 outside/priority 检查边界，使鞋铺 route dialog prepared 不再只按普通队列等待。

Notes:

- Route dialog 的实际点击仍由 `NavigationService` 消费 prepared action，避免绕过 intent/target 校验。
- 任务 Panel 绿字 prepared 仍由 `FiveRingTaskV2.clickPreparedWuhuanTrackerGreen(...)` 消费。

Verify:

- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-16 五环完成框只识别不点击

Status: DONE / compile passed.

Context:

- 五环多窗口收尾时，完成白字模板已经命中，但任务随后又调用 `clickStory(...)` 关闭完成框。
- 这会进入 `dialog:storyClick`，导致已完成窗口再次 focus 并物理左键点击，抢占其他窗口。

Changed:

- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
  - `handleDialog(...)` 命中完成 story 后直接返回 `FINISHED`，不再关闭完成框。
  - `handleTrackerMiss(...)` 命中完成 story 后直接返回 `FINISHED`，不再关闭完成框。
  - 接任务阶段遇到“今日上限/完成”终止 story 时直接结束，不再点击关闭。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮五环收尾看日志里不应再出现 `source=wuhuan-v2:finished-story-close` 和 `request=dialog:storyClick`。

### 唐德 - 2026-06-15 combat target 跳过 clickNpcSmart 内部 Alt+C 二轮

Status: DONE / compile passed.

Context:

- 用户希望减少战斗目标点怪的重复尝试。
- 普通 NPC 仍需要 `clickNpcSmart` 内部第一轮失败后自动 `Alt+C` 再点一次，例如接任务、传送、买鞋等 NPC。
- 修罗/五倍战斗目标已经有任务层 recovery：先检查已有进战斗对话框，再走 `Alt+A` direct-combat，最后 phase retry 前由任务层按 `Alt+C`。

Changed:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - `clickNpcSmart(...)` 第一轮 pipeline 失败后，如果 `request.targetRole() == NpcRole.COMBAT_TARGET`，直接返回 `false`。
  - 非战斗 NPC 保持原来的内部 `Alt+C` + 第二轮 pipeline 行为。

Resulting policy:

- 战斗目标单个 phase：普通 smart click 一轮 -> recovery 检查“看打” -> `Alt+A` direct-combat 一轮。
- 只有进入任务层 phase retry 时才按 `Alt+C`。
- 普通 NPC 不受影响。

Verify:

- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-16 Dialog STORY 误判阈值收紧

Status: DONE / compile + offline replay passed.

Context:

- 五环 `忆叶知秋 / hwnd-1E370FB2` 在接任务 NPC 附近卡住，根因之一是 `DialogService` 把无 dialog 的场景图误判成 `STORY`。
- 误判图里只有人物、白衣服和光效，但旧规则 `totalTextPixels > 200 && textLineStats.matched()` 过于宽松。

Changed:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
  - `hasStoryInUpperHalf(...)` 改为更严格的组合阈值：
    - `total >= 450`
    - `textRows >= 10`
    - `maxRowWhite >= 40`
    - `maxClusters >= 20`
    - `maxSpan >= 120`
  - 保留原有日志字段，方便继续观察 `thinWhite/green/total/textRows/maxRowWhite/maxClusters/maxSpan`。

Replay:

- 用户提供的最少 story 样本：
  - 输入：`images/Snipaste_2026-06-16_00-43-06.png`
  - top42 统计：`thin=506 green=0 total=506 rows=14 maxWhite=48 maxClusters=29 maxSpan=154`
  - 新规则：`true`
  - 标注：`images/temp/story_threshold_debug/Snipaste_2026-06-16_00-43-06_marked_top42.png`
  - 洗白：`images/temp/story_threshold_debug/Snipaste_2026-06-16_00-43-06_top42_white.png`
- 五环误判样本 1：
  - 输入：`images/temp/hwnd-1E370FB2/dialog_detect_handle-dialog_INSPECT_story_upper_raw.png`
  - 统计：`thin=321 green=50 total=371 rows=8 maxWhite=28 maxClusters=7 maxSpan=268`
  - 新规则：`false`
- 五环误判样本 2：
  - 输入：`images/temp/hwnd-1E370FB2/dialog_detect_green-template-click_wuhuan-v2_accept-dialog_story_upper_raw.png`
  - 统计：`thin=375 green=0 total=375 rows=9 maxWhite=22 maxClusters=6 maxSpan=295`
  - 新规则：`false`
- 真实 story 对照样本：
  - `images/temp/hwnd-61F5A/dialog_detect_handle-dialog_CLEANUP_story_upper_raw.png`
  - 统计：`thin=604 green=0 total=604 rows=14 maxWhite=62 maxClusters=30 maxSpan=186`
  - 新规则：`true`

Verify:

- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-15 修罗目标改为固定 NPC 记忆

Status: implemented / compile passed

Context:

- 用户指出修罗怪不是游走怪，任务给出的地图坐标是固定目标。
- 之前 `XiuluoTaskV2.toXiuluoObjective(...)` 把修罗目标标为 `NpcMovementType.ROAMING`，导致 `NpcTarget.toClickRequest(...)` 生成 `roamingTarget=true`。
- 结果是黄色 OCR ROI 记忆走 `clickable-target` / bucket / `any-name` 策略，不符合修罗目标的实际语义。

Changed:

- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  - 修罗任务目标从 `NpcMovementType.ROAMING` 改为 `NpcMovementType.FIXED`。
- `config/vision_memory.json`
  - 将 263 条 `targetName=修罗` 的 `targetCandidateSamples` 从 roaming/clickable-target 迁为 fixed-npc。
  - 将 2259 条旧 `clickable-target` 修罗 ROI policy 合并为 251 条 fixed-npc policy。
  - 保留原有 NPC click attempt 记录；这些本来已经是 `npc-click|地图|修罗|x,y` 的固定 key。

Verify:

- JSON 校验：
  - `xiuluoRoamingCandidates=0`
  - `xiuluoFixedCandidates=263`
  - `xiuluoClickablePolicies=0`
  - `xiuluoFixedPolicies=251`
- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-15 世界地图路线输入等待缩短

Status: implemented / compile passed

Context:

- 用户确认修罗无维护时接任务后先离村，再进入目标导航。
- 为避免离村路程缩短后仍被世界地图输入等待吃掉时间，先缩短已确认的保守等待，不改路线点击/OCR算法。

Changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
  - `WORLD_MAP_SEARCH_TYPE_SETTLE_MS`: `500ms -> 200ms`。
  - `MAP_RESULT_SCROLL_SETTLE_MS`: `300ms -> 200ms`。
  - 复用已打开路线输入框时：
    - 输入目标后等待 `300ms -> 200ms`。
    - 点搜索后等待 `500ms -> 200ms`。

Investigation:

- 修罗离村预走调用当前地图小地图点击时，现有固定等待包括：
  - `Alt+1` 打开小地图后等 `500ms`。
  - 点小地图坐标后等 `250ms`。
  - 如果需要关闭小地图，关闭确认还有 `300ms`。
- 所以“点离村后到打开世界地图”中间本来就可能有接近一秒以上的输入/确认等待；本次暂不改小地图确认逻辑。

Verify:

- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-15 修罗队长目标寻路期间允许三技能维护

Status: DONE / compile passed.

Context:

- 日志确认队长窗口 `hwnd-2291C52` 只有 maintenance broadcast 检查，没有自己的 `summon skill due/start summon skill clean`。
- 原因是修罗队长的 `after-accept` / `before-route` 维护点明确设置了 `.cleanSummonSkill(false)`；三技能主要由队员 `AutoBattleTask` 触发。
- 用户确认队长三技能阶段应与队员一致：必须出了灵兽村后；但队长在 `navigateInCurrentMap` 等待期间可以跑三技能，因为队长本身正在等路。

Changed:

- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  - 在 `continueIfNavigationStillPathing(...)` 的目标寻路等待分支接入队长三技能维护。
  - 只在 `state.phase() == NAVIGATE_TO_TARGET` 且窗口角色不是 `MEMBER` 时触发。
  - 使用现有 `TaskMaintenanceService.runOpportunisticMaintenance(...)`，不新增召唤兽逻辑：
    - `cleanSummonSkill(true)`
    - `oneSummonSkillPerTeamRound(true)`
    - `teamMaintenanceKey(TASK_CODE)`
    - `teamRound(state.round())`
    - `requireOpenTeamMaintenanceWindow(false)`
  - 保持队员原有 `AutoBattleTask` gate 不变；队员仍可依赖 `requireOpenTeamMaintenanceWindow`。

Notes:

- 队长三技能维护不依赖 team pathing window，因为 current-map 等待时该窗口会被关闭。
- 维护成功后仍返回原来的 pathing/yield 流程，不改变修罗目标导航结果。

Verify:

- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-15 三技能尾部安全 cache 跳过面板

Status: DONE / compile passed.

Context:

- 用户观察到某些窗口三技能维护已经缓存到 `cachedStartSlot=9`，但后续仍会打开召唤兽技能面板空跑。
- 不能用 `skillCount=6/8` 判断是否完成，因为它只是布局格子数量，不代表召唤兽实际开放/有效到第几个格子。
- 也不能用 `LOCKED_SLOT` 反推最后有效格；`LOCKED_SLOT` 只表示当前格不是有效格。

Changed:

- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
  - 新增 2 小时尾部安全 cache TTL：`SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS`。
  - 成功清理后，只从本次 `observedStatusesByIndex` 中识别最后一个有效格：
    - `NORMAL_SKILL`
    - `KEEP_SKILL`
    - `EMPTY_SLOT`
  - 如果 `nextStartIndex > lastConfirmedEffectiveSlotIndex`，记录 `tailSafeCachedAt`。
  - 下次三技能到期时，如果 cache 未超过 2 小时，直接跳过整次召唤兽技能面板打开/检查，并刷新普通维护 cooldown。
  - 如果 cache 超过 2 小时，清空 tail-safe 状态和 `nextStartIndex`，让下一次按现有扫描逻辑重新确认。

Notes:

- 没有修改 `SummonSkillService` 的格子扫描/停止逻辑。
- `LOCKED_SLOT` 不参与最后有效格推导。
- 跳过逻辑发生在 team-round claim 之前，避免“本窗口本来不用检查，却占用本轮三技能名额”。

Verify:

- `mvn -q -DskipTests compile` passed。

Next live-check:

- 看 `navigation map search split: stage=prepare` 是否比之前少约 `300-600ms`。
- 如果仍然 `prepare/scan-click` 大于 5 秒，下一步应查 OCR/截图/滚动耗时，而不是继续盲砍 sleep。

### 谢帅 - 2026-06-15 修罗接任务后无维护先离村

Status: implemented / compile passed

Context:

- 用户确认修罗接完任务后的顺序需要分快线/保守线。
- 无医保宝/修装备维护时，不应原地先读完 story objective 再离村；可以先点灵兽村出口，让角色移动时再读 story objective。
- 有维护 due 时必须保持先读 objective，因为医保宝/修装备会打开业务 dialog，可能覆盖接任务后的 story dialog。

Changed:

- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoRoundContext.java`
  - 新增 `startExitPrepathStarted` 状态位，记录本轮已经在读 objective 前启动过离村预走。
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  - 接任务选项点击成功后走 `continueAfterAcceptOptionClicked(...)`。
  - 如果医保宝或修装备维护 due：保持旧顺序，进入 `READ_OBJECTIVE`。
  - 如果没有维护 due：先调用 `startLeavingStartMapIfPresent(...)`，开始离村后再读 objective。
  - `READ_OBJECTIVE` 发现已经启动离村预走时，读到目标后直接进入 `NAVIGATE_TO_TARGET`，避免再走维护/再点一次离村。
  - `startLeavingStartMapIfPresent(...)` 遇到已启动离村预走时直接跳过，防止重复点击出口。

Verify:

- `mvn -q -DskipTests compile` passed。

Next live-check:

- 无维护 due 的修罗接任务后应看到：
  - `accept option clicked; start exit before objective read`
  - 后续 `objective:story:after-start-exit-prepath`
- 有医保宝/修装备 due 时应看到：
  - `accept option clicked; read objective before maintenance`
  - 后续仍进入 `AFTER_ACCEPT_MAINTENANCE_CHECK` / `BEFORE_ROUTE_MAINTENANCE_CHECK`。

### 唐德 - 2026-06-15 修罗目标当前地图寻路关闭三技能维护窗口

Status: implemented / compile passed

Context:

- 用户观察 16:03 左右队长已经在目标地图并开始靠近修罗怪，队员仍然触发/重试召唤兽三技能维护，抢占输入队列。
- 日志确认现有 gate 存在，但粒度过宽：`target-navigation-pathing-started` 会打开 team maintenance window；只有任务线程后续消费 watcher arrival / stopped-away / wait-ended 时才关闭。
- 需求确认：跨地图路上可以允许队员维护；一旦队长已经进入目标地图、开始当前地图靠近怪物，就应该关闭三技能维护窗口。

Changed:

- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  - 复用已有 `closeTeamPathingMaintenanceWindow(...)`，没有新增 watcher/service 注入链路。
  - `navigateToTarget(...)` 收到 `PATHING_STARTED` 时：
    - 如果 `NavigationResult.message == "current-map mini-map click started pathing"`，直接关闭 team maintenance window，source=`target-current-map-pathing-started`。
    - 其他跨地图/route pathing 仍然按旧逻辑打开 maintenance window，source=`target-navigation-pathing-started`。

Verify:

- `mvn -q -DskipTests compile` passed。

Next live-check:

- 修罗队长到目标地图后，日志应先出现：
  - `navigate in current map mini-map click started pathing`
  - `maintenance team pathing window closed: teamRound=xiuluo_v2#... source=xiuluo-v2:target-current-map-pathing-started`
- 这个时间点之后，队员不应再因为同一轮 `source=auto-battle` 重新 claim 召唤兽三技能维护。

### 唐德 - 2026-06-15 小地图关闭误判证据截图

Status: implemented / compile passed

Context:

- 用户观察 14:55:02 附近可能是 `close-after-confirmed-pathing` 后，小地图可见性检测误判仍然开着，导致 fallback 再按 `Alt+1`，视觉上像小地图又开又关。
- 本轮只加 debug 证据，不改变关闭/重试行为。

Changed:

- `NavigationService.isMiniMapPanelVisible(...)`
  - 保留普通检测路径不变。
  - 新增关键分支 debug 检测：`closeMiniMapAfterConfirmedPathing(...)` 的 after-close check 会保存一次 ROI 图。
  - 输出日志：
    - `mini-map panel visible debug matched`
    - `mini-map panel visible debug miss`
    - 命中时包含 `template/roi/rect/local/absolute/source`。
  - debug 图路径形如：
    - `images/temp/<windowId>/mini_map_panel_visible_check_yyyyMMdd_HHmmss_SSS_roi.png`

Verify:

- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-15 Alt+A direct-combat OCR 跳过默认 mask

Status: DONE / compile passed.

Context:

- 用户现场验证修罗怪刷在洛阳角落，目标坐标接近 `(463,1)`，角色也在边缘。
- `Alt+A` 进入直接点怪模式后，游戏会隐藏 HUD/任务栏等干扰项；此时原来的 full-window default mask 反而会把底部/边角有效怪名和角色紫名一起遮掉。
- 普通 NPC 点击仍需要 default mask，因为普通模式下 HUD、聊天、快捷栏还在。

Changed:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - `runNpcClickPipeline(...)` 在 `verificationMode=direct-combat` 时写入 `skipDefaultOcrMask=true`。
  - 黄名 OCR 路径 `clickNpcByYellowTargetName(...)` 支持按 pipeline state 跳过 default mask。
  - 紫名锚点公式路径 `calculatePlayerAnchorFormulaPoint(...)` 支持按 pipeline state 跳过 default mask。
  - `Alt+A` 失败退出时查找紫名锚点也跳过 default mask，避免边缘角色名被遮掉。
  - 全局 `OcrWindowScanService.DEFAULT_MASKS` 未改；非 `direct-combat` 链路仍按原逻辑 mask。

Verify:

- `mvn -q -DskipTests compile` passed。

Next live check:

- 下次跑角落修罗 `Alt+A` fallback 时，日志应出现：
  - `NPC purple player-anchor default mask skipped for direct-combat mode`
  - `NPC yellow target default mask skipped for direct-combat mode`
- 重点检查窗口 scoped debug 图：
  - `images/temp/<window-id>/center_scan_layer1.png`
  - `images/temp/<window-id>/npc_yellow_target.png`
- 这些图在 `Alt+A` 模式下应该保留完整 full-window 内容，不再把底边/角落 mask 成白块。

Next live-check:

- 下次如果再次出现：
  - `mini-map still visible after confirmed pathing close; falling back to generic close button`
- 同一时间附近应能找到：
  - `mini-map panel visible debug matched ... roi=...`
  - 或 `mini-map panel visible debug miss ... roi=...`
- 直接打开对应 `roi` 图片，确认到底是真有小地图 checkbox，还是模板误命中。

### 唐德 - 2026-06-15 Alt+6 屏蔽确认恢复三次尝试

Status: implemented / compile passed

Context:

- 用户确认启动前 `Alt+6` 屏蔽不能只按一次，应该恢复旧语义：最多尝试三次，命中确认模板就停止。
- 本轮只改启动屏蔽确认逻辑，不改任务业务流程、地图/NPC/dialog 点击算法。

Changed:

- `TaskStartupWindowPreparationService.ensureAlt6Visibility()`
  - 新增 `ALT6_VISIBILITY_MAX_ATTEMPTS = 3`。
  - 每次尝试：后台 HWND 发送 `Alt+6` -> 等 `500ms` -> 查 `images/template/status/blacklist_crowd.png`。
  - 任意一次模板命中即成功，并继续等待 `1000ms` 让浮层淡出。
  - 每次失败日志带 `attempt=x/3`；三次都失败后输出最终 warning。

Verify:

- `mvn -q -DskipTests compile` passed。
- `git diff --check -- src/main/java/com/bot/dhxy/window/startup/TaskStartupWindowPreparationService.java` passed，仅 CRLF warning。

Next live-check:

- 启动五环/五倍时日志应出现：
  - `task startup visibility: send Alt+6 ... attempt=1/3`
  - 如果第一次没命中，应继续 `attempt=2/3`、`attempt=3/3`。
  - 成功时应看到 `confirmed by template after background Alt+6 attempt=x/3`。

### 谢帅 - 2026-06-15 摄妖香 cyan 小时 / green 分钟识别

Status: implemented / compile passed

Context:

- 摄妖香状态栏截图区域已扩到用户实测框，先用 `images/template/status/sheyaoxiang_buff.png` 匹配状态图标。
- 匹配后按图标左右边界裁一条竖列，一直裁到状态截图底部，避免使用旧固定数字位置。
- 业务规则调整为：cyan 数字表示剩余小时；cyan 没读到时，再读 green 数字，green 数字表示剩余分钟。
- 模板命中但 cyan/green 都读不到，或模板本身没命中，按保守策略补一根摄妖香，重建 1 小时计时。
- 剩余时间小于等于 20 分钟时主动补香，大于 20 分钟时跳过补香。

Changed:

- `PlayerStateService`
  - `probeIncenseStatus(...)` 读取匹配图标下方竖列。
  - `readSheyaoxiangRemainingTime(...)`：cyan OCR 结果按小时换算。
  - `readSheyaoxiangRemainingMinutesGreen(...)`：green OCR 结果按分钟换算。
  - `IncenseStatusProbe` 改为保存剩余毫秒和诊断文本，例如 `cyan-hours=1`、`green-minutes=39`。
- `DebugSheYaoXiangStatusCaptureMain`
  - 无 UI 调试入口，注册当前游戏窗口后只截图/匹配/洗图/读数，不点击、不打开包裹。

Verify:

- Command:
  - `java "-Dfile.encoding=UTF-8" -cp "target/classes;$cp" com.bot.dhxy.debug.DebugSheYaoXiangStatusCaptureMain`
- Input/output images:
  - `images/temp/hwnd-1203DA/sheyaoxiang_status_debug_manual_raw.png`
  - `images/temp/hwnd-1203DA/sheyaoxiang_status_debug_manual_matched_column_raw.png`
  - `images/temp/hwnd-1203DA/sheyaoxiang_status_cyan_digits.png`
  - `images/temp/hwnd-1203DA/sheyaoxiang_status_green_digits.png`
- Observed result:
  - cyan returned no hour digits.
  - green returned `39` and logged `remaining=green-minutes=39`.
- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-15 ready/prepared latency + 长 phase yield checkpoint

Status: implemented / compile passed

Context:

- 根据最新 MD 任务执行；本轮同时接唐德和谢帅的收尾任务。
- 目标是在现有协作式优先级模型里补观测和轻量 checkpoint，不做全局 priority queue，不重写 `TaskTurnCoordinator`。
- 本轮不改世界地图/小地图/NPC/dialog 视觉匹配或点击坐标算法。

Changed:

- `WindowReadyEvent`
  - ready event 增加 `hwnd`、`operation`、`targetKeyword`，用于串起 ready 发布、prepared action 和五环消费/yield。
- `WindowReadyEventBus.publish(...)`
  - ready 发布日志补充 `hwnd/operation/target/createdAtMs/ageMs`。
- `WindowTaskRunner`
  - `TASK_ATTENTION_REQUIRED` 发布时带当前 hwnd。
  - prepared follow-up ready event 带 `PreparedDialogAction.operation/targetKeyword`。
  - pathing terminal ready event 带 hwnd 和目标地图。
- `FiveRingTaskV2`
  - 在 `WAIT_PATHING`、`ACCEPT_TASK`、`HANDLE_DIALOG`、`SYNC_TASK_PANEL` 进入前沿用现有 outside-phase priority gate，补充明确 checkpoint 日志。
  - 当前窗口有 fresh prepared tracker action 时，优先消费，并记录 ready -> consume latency、prepared age、verified age、click point。
  - 当前窗口有 route prepared action 时，不抢 Navigation 的消费权，只记录该 action 已可用并保留给 Navigation。
  - 其他窗口有 fresh `TASK_ATTENTION_REQUIRED` 时，当前窗口短 yield，并记录 ready -> yield latency。
  - 超过 3000ms 未消费/未处理时输出：
    - `ready dialog pending too long`
    - 字段包含 `ageMs/readyWindowId/readySource/phase/currentWindowId/preparedUsable/staleReason`。

Verify:

- `mvn -q -DskipTests compile` passed。

Next live-check:

- 路线 option / 任务面板绿字出现后，应能从日志串到：
  - `event=window.ready.publish ... source=dialog-visible...`
  - `event=window.ready.publish ... source=dialog-visible-prepared... operation=... target=...`
  - `[five-ring-v2 priority] long phase checkpoint yields because another window has ready dialog ...`
  - 或 `[five-ring-v2 priority] long phase consumes current prepared action before continuing ...`
- 如果 ready 超过 3 秒还没被处理，应看到 `ready dialog pending too long` 和 `staleReason`，下一轮再按原因判断是否需要真正改调度器。

### 唐德 - 2026-06-15 五环买鞋进店 stale target retry 修复

Status: implemented / compile passed

Context:

- 根据 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 里 “唐德任务：修复买鞋进店后的 stale target retry” 执行。
- 目标只限 `FiveRingTaskV2` 买鞋进店链路：角色已经进入 `牛记布店` 后，不允许再用旧目标 `长安(130,130)` 重新打开世界地图搜索长安。
- 本轮不改小地图/世界地图点击算法、NPC 点击算法、OCR/template 算法，也不改 route dialog 架构。

Changed:

- `FiveRingTaskV2.buyShoes(...)`
  - `handleShoeShopDoorAfterArrival(...)` 返回 false 后，新增一次 fresh check `牛记布店`。
  - 如果已经在 `牛记布店`，直接切到 `BUY_SHOES`，不再落回 `clickShoeShopEntryExact(...)`。
  - 每次准备调用 `clickShoeShopEntryExact(...)` 前，新增 `牛记布店` fresh check。
  - 原 `entry failure` 后的店内确认也统一走同一个短路逻辑。
- 新增日志：
  - `skip shoe-shop-entry exact navigation because current map is 牛记布店`
  - 字段包含 `windowId/currentMap/current/oldTargetMap/oldTarget/source`，用于确认是主动阻止旧 `长安(130,130)` target retry。

Verify:

- `mvn -q -DskipTests compile` passed。
- `git diff --check` passed，仅有 CRLF 转换 warning，无 whitespace error。

Next live-check:

- 如果角色已经进 `牛记布店`，后续同窗口不应再出现：
  - `navigation.toNpc target=长安(130,130)`
  - `navigation map search: type target map=长安`
  - `source=wuhuan-v2:shoe-shop-entry-exact-130-130`
- 应看到：
  - `skip shoe-shop-entry exact navigation because current map is 牛记布店 ... oldTargetMap=长安 oldTarget=(130,130)`

### 谢帅 - 2026-06-14 Phase 5C-B route dialog stale/retry 日志补齐

Status: implemented / compile passed

Context:

- 根据 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 5C 工作包 B 执行。
- 本轮只补 `NavigationService` 调用方验收点和日志，不改世界地图点击、小地图点击、NPCClickSmart、五环/五倍/修罗业务策略。

Changed:

- `NavigationService.isPreparedRouteDialogActionUsable(...)`
  - 除 operation / target / window binding / verified age 外，额外校验 `PreparedDialogAction.intentId` 是否匹配当前 active pathing intent。
  - 避免旧 intent 准备出来的 route option action 被后续导航误认为可用。
- `NavigationService.consumePreparedRouteDialogAction(...)`
  - prepared action 被消费时，日志补充 `windowId/title/hwnd/actionIntentId/activeIntentId/actionSource/visibleType/visibleSource/visibleAgeMs/matchedText/click/verifiedAgeMs`。
  - consumed 后发现 stale / binding mismatch / intent mismatch 时，日志明确 `sameBinding`、`sameIntent` 和 click point。
- `NavigationService.shouldYieldForRouteDialogBeforeWorldMap(...)`
  - route dialog world-map gate 日志补充 `reason`。
  - fresh 时能看到 `visible-option` / `requested` / `preparing` / `prepared`。
  - 允许重新打开世界地图时能看到 `allow-world-map-retry:<visibleReason>/<statusReason>/<preparedReason>`，例如 `visible-stale`、`prepared-intent-mismatch`、`prepared-stale`。
  - 日志补充 `title/hwnd/activeIntentId/activeIntentTarget/activeIntentSource/intentAgeMs/visibleAgeMs/requestAgeMs/preparedAgeMs`。

Verify:

- `rg -n "legacy-foreground-route-ocr|handleRouteKeywordOption|handleRememberedRouteOption|findUsableRoute" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - no matches。
- `rg -n "DialogPreparationRequest\\.builder|updateDialogPreparationRequest\\(|detectDialogTypeNoFocus" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - no matches。
- `mvn -q -DskipTests compile` passed。

Next:

- 实跑时重点看：
  - `route dialog world-map gate: result=false reason=allow-world-map-retry:...` 是否能解释为什么重开世界地图。
  - `route dialog uses consumed prepared action` 是否带同一个 `actionIntentId` / `activeIntentId`。
  - 如果仍重复打开世界地图，按日志里的 `visibleReason/statusReason/preparedReason` 定位是 absent、stale 还是 mismatch。

### 谢帅 - 2026-06-14 Phase 5B NavigationService 退到动作边界

Status: implemented / compile passed

Context:

- 根据 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 5 工作包 B 执行。
- Phase 5A 已让 Runner 可以基于 active pathing intent + visible OPTION 准备 route dialog action；本轮让 `NavigationService` 不再主动创建 route dialog preparation request。

Changed:

- `NavigationService.clickRouteDialogOption(...)`
  - 删除主动 `runtime.updateDialogPreparationRequest(...)` / `DialogPreparationRequest.builder()`。
  - 不再制造 REQUESTED/PREPARING 等待分支。
  - 只优先消费 Runner 已准备好的 `PreparedDialogAction`；没有可用 action 时，再经过 runtime fresh-state gate，最后才走 legacy memory / OCR fallback。
- `NavigationService.submitWorldMapSearchAndClickDestination(...)`
  - 世界地图路线链接点击成功后只调用 `registerWindowPathingIntent(...)` 登记 active pathing intent。
  - 不再写 route dialog preparation request。
- `NavigationService`
  - 删除 `requestRouteDialogPreparationAfterMapRouteClick(...)`。
  - 删除 `DialogPreparationRequest` import 和 request-only 常量/旧 helper。

Verify:

- `rg -n "DialogPreparationRequest\\.builder|updateDialogPreparationRequest\\(" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - no matches。
- `rg -n "handleRouteKeywordOption|legacy-foreground-route-ocr|detectDialogTypeNoFocus" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - `detectDialogTypeNoFocus` no matches。
  - `handleRouteKeywordOption` 只剩 `legacy-foreground-route-ocr` fallback。
- `rg -n "registerPathingIntent|WindowPathingIntent\\.builder|submitWorldMapSearchAndClickDestination|performWorldMapSearchAndClickDestination" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - world-map submit wrapper 仍存在；成功后登记 `WindowPathingIntent`。
- `mvn -q -DskipTests compile` passed。

Next:

- 交给 Phase 5 验收 / 实跑：
  - 世界地图路线点击后日志应只看到 pathing intent registered，不应再看到 route dialog preparation requested after map route click。
  - route option dialog 出现后，应由 Runner 输出 `route dialog preparation: result=prepared ... intentId=...`。
  - 若仍进入 `legacy-foreground-route-ocr`，需要用日志解释 fresh Runner 状态为什么不可用。

### 谢帅 - 2026-06-14 Phase 4B Navigation 世界地图入口 route dialog gate

Status: implemented / compile passed

Context:

- 根据 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 4 工作包 B 执行。
- 目标是让 `NavigationService` 在任何准备打开/重试世界地图前，先消费 watcher 已准备好的 route dialog action；如果 watcher 看到 route dialog 或正在准备，则返回 `DIALOG_PREPARING`，不要重新打开世界地图。

Changed:

- `NavigationService.navigateToMap(...)`
  - 保留 `navigateToMap:before-world-map` gate，正式 world-map submit 前先走 `routeDialogGateBeforeWorldMap(...)`。
- `NavigationService.submitWorldMapSearchAndClickDestination(NavigationRequest, String, String)`
  - 成为唯一业务入口。
  - 入口内部再次执行 `routeDialogGateBeforeWorldMap(...)`，防止未来调用方绕过 `navigateToMap` 前置 gate。
  - 绿色路线链接点击成功后统一调用 `requestRouteDialogPreparationAfterMapRouteClick(...)`，让 watcher 接管后续 route dialog preparation。
- `NavigationService.performWorldMapSearchAndClickDestination(...)`
  - 原底层 boolean 搜索/点击方法改名为 `perform...`，只负责实际世界地图输入和点击，不再作为可直接调用的业务入口。
- `NavigationService.retryWorldMapDestinationClick(...)`
  - 开 route panel 前先执行 route dialog gate。
  - fallback 也改走带 gate 的 `submitWorldMapSearchAndClickDestination(...)` wrapper。

Verify:

- `rg -n "submitWorldMapSearchAndClickDestination\\(|performWorldMapSearchAndClickDestination\\(|retryWorldMapDestinationClick\\(|openWorldMapRoutePanelDirect\\(|requestRouteDialogPreparationAfterMapRouteClick\\(" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - `submitWorldMapSearchAndClickDestination` 只剩正式 wrapper 和调用点；底层实现为 `performWorldMapSearchAndClickDestination`。
- `rg -n "detectDialogTypeNoFocus|handleRouteKeywordOption|legacy-foreground-route-ocr|waitForPreparedRouteDialogAction" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - `detectDialogTypeNoFocus` / `waitForPreparedRouteDialogAction` no matches。
  - `handleRouteKeywordOption` 仍只在 `legacy-foreground-route-ocr` fallback。
- `mvn -q -DskipTests compile` passed。

Next:

- 实跑重点看：
  - `route dialog world-map gate: result=true`
  - `route dialog uses consumed prepared action`
  - `route dialog preparation requested after map route click`
  - 正常情况下不应该在 route dialog 已经打开时继续重新打开世界地图。

### 谢帅 - 2026-06-14 Phase 3 Navigation route dialog 降权

Status: implemented / compile passed

Context:

- 根据 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 3 工作包 B 执行。
- 目标是让 `NavigationService` 不再拥有 route dialog 的 OCR/模板识别主流程，只负责注册 route preparation request、消费 watcher 准备好的 action，以及在 modal/preparation 新鲜时让权。

Changed:

- `NavigationService.navigateToMap(...)`
  - 删除 pathing-active 分支里直接 `detectDialogTypeNoFocus(...)` 的 rescue 探测。
  - 改为只读取 `WindowRuntimeContext` 中 watcher 写入的 visible dialog snapshot / preparation status / prepared action，再决定 `DIALOG_PREPARING` 或继续原路径。
- `NavigationService.clickRouteDialogOption(...)`
  - 移除 `waitForPreparedRouteDialogAction(...)` 200ms 主流程等待。
  - 请求 background route preparation 后只立即尝试 `consumePreparedRouteDialogAction(...)`；如果 watcher 正在 `REQUESTED/PREPARING`，直接返回 `DIALOG_PREPARING`。
  - 记忆点点击和 `handleRouteKeywordOption(...)` 前台 OCR 都降级为 legacy fallback，并加上 `legacy-foreground-route-memory` / `legacy-foreground-route-ocr` 日志。
- 删除旧 `waitForPreparedRouteDialogAction(...)` 方法和相关 wait 常量。

Verify:

- `rg -n "WindowReadyWaitService|waitForPathingWakeOrTimeout|TASK_TRACKER_PREPARED|DIALOG_PREPARED|publishPreparedActionEvent|latestFreshHigherPriority|latestFreshForWindow|currentReadyPriority|priority gate|higherPriority|READY_PRIORITY" src/main/java/com/bot/dhxy -S`
  - no matches。
- `rg -n "clickRouteDialogOption|waitForPreparedRouteDialogAction|handleRouteKeywordOption|detectDialogTypeNoFocus|legacy-foreground-route-ocr" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - `waitForPreparedRouteDialogAction` / `detectDialogTypeNoFocus` no matches。
  - `handleRouteKeywordOption` 只剩 `legacy-foreground-route-ocr` fallback 分支。
- `rg -n "refreshDialogPreparationSignal|updatePreparedDialogAction|markDialogPreparationStarted|markDialogPreparationFailed" src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java -S`
  - Runner producer still present。
- `rg -n "consumePreparedDialogAction" src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - Runtime consumer API and Navigation consumer still present。
- `mvn -q -DskipTests compile` passed。

Next:

- 如果编译通过，下一轮实跑重点看 `route dialog world-map gate`、`route dialog uses consumed prepared action`、`legacy-foreground-route-ocr` 是否符合预期；正常路径不应该频繁进入 legacy OCR。

### 谢帅 - 2026-06-14 Agent 提问响应规则更新

Status: rule updated

Context:

- 用户指出：当用户询问“为什么/怎么看/哪里改/怎么改”时，Agent 不应直接开始写代码。
- 正确流程应该是先定位问题、解释原因、给出修改方案；只有用户明确同意后才开始改代码。

Changed:

- `AGENTS.md`
  - 在 `Important behavior constraints` 中新增第 7 条 `Investigation-first rule for user questions`。
  - 规定涉及行为、导航、OCR/模板匹配、runner/watcher、任务流程、输入等改动时，必须先查日志/截图/调用链/状态流，再给方案。
  - 只有用户明确说“可以 / 按这个改 / 继续做”等等后，才能开始代码改动。

Follow-up:

- 后续 Agent 新开线程时必须先读 `AGENTS.md`，并遵守这条“先定位和给方案，用户同意后再改”的协作规则。

### 何黎 - 2026-06-14 Window ready 优先级让权调度

Status: implemented / compile passed

Context:

- 用户指出五环/五倍/修罗多窗口时，已经有 dialog 或 prepared action 的窗口没有优先处理，普通窗口仍会按 fair lock 顺序继续拿权。
- 典型现象：
  - 窗口 A 已经有路线 dialog / 给物品 dialog 挂着；
  - 窗口 B 只是普通下一阶段；
  - 由于 task turn 原来只按 `ReentrantLock(true)` 公平排队，窗口 B 可能先拿到 turn，导致窗口 A 的 dialog 继续过期或被重新导航覆盖。

Decision:

- 不让 watcher 直接点 dialog，也不让 watcher 执行业务。
- watcher 只发布“这个窗口需要任务层尽快回来处理”的软优先级信号。
- 任务层拿到 turn 后仍必须重新读取 runtime state / dialog / prepared action，再决定怎么处理。
- 优先级只影响 `TaskTurnCoordinator` 的拿权顺序，不改输入队列串行化、不绕过任务自己的 `handleDialog`。

Changed:

- `WindowReadyEventType`
  - 新增 `TASK_ATTENTION_REQUIRED`。
- `WindowReadyEvent`
  - 新增 `priority`。
- `WindowReadyEventBus`
  - 给 ready event 增加默认优先级：
    - `DIALOG_PREPARED = 100`
    - `TASK_ATTENTION_REQUIRED = 90`
    - `PATHING_TERMINAL = 70`
    - `TASK_TRACKER_PREPARED = 60`
  - 新增 `latestFreshForWindow(...)` / `latestFreshHigherPriority(...)`，只返回未过期的新鲜事件。
  - publish 日志增加 `priority` / `eventAgeMs`。
- `WindowTaskRunner`
  - prepared route / task tracker action 准备好后发布对应 ready event。
  - 如果没有 prepared action，但当前窗口 no-focus 检测到 `OPTION` 或 `STORY` dialog，发布 `TASK_ATTENTION_REQUIRED`。
  - 这个信号只是调度 hint，不点击、不清理、不判断业务含义。
- `TaskTurnCoordinator`
  - 拿 turn 前最多短等 `500ms`，如果别的窗口有更高优先级 fresh ready event，则让它先拿。
  - 已经拿到 fair lock 后还会再检查一次；如果此时发现别的窗口优先级更高，会主动释放并短暂让出。
  - 解决“高优先级事件到来前，低优先级窗口线程已经排在 fair lock 前面”的情况。
- `FiveRingTaskV2` / `WubeiTask` / `XiuluoTaskV2`
  - `PATHING_STARTED` 让权等待现在接受 `TASK_ATTENTION_REQUIRED` 早醒。
  - 五环仍额外接受 `TASK_TRACKER_PREPARED`。

Verify:

- `mvn -q -DskipTests compile` passed。

Next log markers:

- `event=window.ready.publish ... type=TASK_ATTENTION_REQUIRED priority=90`
- `task attention published: task=... visibleDialog=...`
- `task turn priority defer ... higherWindowId=... higherType=...`
- `task turn acquired then yielded to ready window ...`

Important note:

- 如果日志显示窗口已经靠 `TASK_ATTENTION_REQUIRED` 优先拿权，但业务仍没有处理掉 dialog，那下一步就是对应任务的 `handleDialog` / 模板识别问题，不再是调度层没有把窗口排到前面。

### 何黎 - 2026-06-14 修罗跑前 WindowReady CR 修补

Status: implemented / compile passed

Context:

- 用户准备实跑修罗前，要求 Rawls / Mencius 做一次 CR，优先找会影响修罗稳定性的漏洞。
- 两个 agent 都指出 `WindowReadyWaitService` 的旧 prepared action 快捷唤醒风险较高：只要当前窗口里还有任意 prepared action，就会绕过 event sequence 等待，可能被旧的或非当前目标的 dialog prepared 状态误唤醒。

Changed:

- `WindowReadyWaitService`
  - 删除 `consumePreparedAlready` 捷径。
  - 现在只消费 `WindowReadyEventBus` 发布的新事件或 timeout；任务醒来后仍按原流程重读 runtime state。
- `WindowTaskRunner`
  - `PATHING_TERMINAL` soft event 改为在 `settlePendingTransferChoiceMemory(...)` 之后发布。
  - 避免任务被 terminal event 唤醒后清 pathing signal，导致 pending route memory 还没落库就被清掉。
- `WindowRuntimeContext`
  - `clearDialogPreparationRequest(...)` / `updateDialogPreparationRequest(null)` 改为经过 `clearPreparedDialogAction(...)` 清理 prepared action。
  - 这样后续日志能看到 `event=window.ready.clearPrepared`，方便定位 prepared action 是过期、绑定变化还是 runtime reset 清掉的。

CR notes:

- Rawls / Mencius 都认为 `DIALOG_PREPARED` 的实际消费端仍会校验 route target / binding / TTL；本次删除旧 prepared shortcut 是最小风险修补。
- Mencius 还指出 route dialog 的 `SHARED_STATE_TRIGGERED` 固定 sleep 后续可以继续接 soft wake，但这次先不扩大修改范围。

Verify:

- `mvn -q -DskipTests compile` passed.

### 何黎 - 2026-06-14 任务让权等待接入 WindowReadyEventBus

Status: implemented / compile passed

Context:

- 用户指出多窗口移动中的响应仍然偏慢：窗口 watcher 已经知道某个窗口 `ARRIVED` / `STOPPED_AWAY`，但任务线程经常还在固定 handoff sleep 或等下一轮轮询。
- 2026-06-13 已经完成 `WindowReadyEventBus` 和 watcher 端 `PATHING_TERMINAL` 发布，本次只做消费端第一步接入。

Decision:

- 新增 `WindowReadyWaitService`：
  - 只等待当前 `TaskExecutionContext.windowId` 的 `PATHING_TERMINAL` 事件或原 timeout；
  - 不抢 task turn、不 focus、不点击、不推进业务；
  - 事件只作为 soft wake hint，任务醒来后仍必须按原流程重新检查当前状态。
- 只接 `TaskTransactionResult.PATHING_STARTED` 后的 handoff wait。
- `SHARED_STATE_TRIGGERED`、战斗、维护广播、归队等待继续走原 fixed delay，避免这次改动扩大到战斗/广播节奏。

Changed:

- `WindowReadyWaitService`
  - `waitForPathingTerminalOrTimeout(...)`
  - 记录 `[latency] event=window.ready.consume...` / timeout debug 日志。
- `WubeiTask`
  - `PATHING_STARTED` 的 `yieldAfterMustYield(...)` 等 watcher terminal 或原 `delayMs`。
  - 保留后续 `maybeRunLeaderPathingSummonMaintenance(...)`。
- `XiuluoTaskV2`
  - `PATHING_STARTED` 的 `yieldAfterMustYield(...)` 等 watcher terminal 或原 `delayMs`。
- `FiveRingTaskV2`
  - `PATHING_STARTED` 的 `yieldAfterMustYield(...)` 等 watcher terminal 或原 `delayMs`。

Verify:

- `mvn -q -DskipTests compile` passed.

Follow-up:

- 下一步可以用五环/五倍/修罗实跑日志观察：
  - `event=window.ready.publish`
  - `event=window.ready.consume`
  - `event=window.ready.consumeTimeout`
- 如果消费 timeout 很多，说明任务的 handoff delay 太短或 watcher terminal 来得更晚；下一步应在具体 phase 的等待点接 watcher snapshot，而不是继续加固定 sleep。

### 何黎 - 2026-06-13 Window Ready Event 软 push 骨架

Status: implemented / compile passed

Context:

- 用户指出当前多窗口轮询模型反应太慢：窗口后台 watcher 已经知道某个窗口停下/到达，但任务通常要等下一轮拿到 turn 后才重新检查。
- Rawls / Mencius 只读评估后建议先做一个保守的 soft push 机制：
  - watcher 只发布“窗口状态已更新”的 wake hint；
  - 不在事件里抢权、focus、点击或推进业务；
  - 任务消费事件前必须重新读取 `WindowRuntimeContext` / `WindowPathingSnapshot`。

Decision:

- 新增 `WindowReadyEventBus` 作为内存事件总线，只保存每个 window/type 最新事件并唤醒等待者。
- 第一版只发布 `PATHING_TERMINAL`：
  - `ARRIVED`
  - `STOPPED_AWAY`
- 发布时机在 `WindowTaskRunner.updatePathingFromLocation(...)` 内：
  - 先更新 `WindowRuntimeContext.pathingSnapshot`；
  - 再发布 soft event；
  - 再执行原有 route memory settle。
- 目前不接入五环/五倍/修罗业务消费，避免一次性改动放权语义。

Changed:

- `WindowReadyEventType`
- `WindowReadyEvent`
- `WindowReadyEventBus`
- `WindowTaskRunner`
  - 注入 `WindowReadyEventBus`。
  - pathing watcher 状态变化到 terminal 时发布 `PATHING_TERMINAL`。
- `MultiWindowTaskManager`
  - 创建 `WindowTaskRunner` 时传入 event bus。

Follow-up:

- 下一步如果要接业务，只在任务等待点用 `awaitNewer(...)` 缩短等待。
- 消费端必须把 event 当作“醒来重新检查”的信号，不能直接相信 event 里的旧 snapshot 发鼠标/键盘。
- 建议先接一个 debug/导航等待点验证日志，再接五环/五倍/修罗。

### 谢帅 - 2026-06-13 NPCClickSmart 首轮失败后 Alt+C 再重试

Status: implemented / compile passed

Context:

- 用户希望 `NPCClickSmart` 第一次失败后，第二次 retry 前先按一次 `Alt+C`。
- 目标是处理角色骑乘/坐骑遮挡 NPC 或遮挡名字时，第一次点不到 NPC，第二次先下坐骑再尝试。

Changed:

- `NpcClickService.clickNpcSmart(...)`
  - 首轮 `runNpcClickPipeline(..., "dialog")` 成功则直接返回。
  - 首轮失败且任务未停止时，走输入队列发送 `Alt+C`，等待 `700ms`。
  - 然后用同一个 `NpcClickRequest` 再跑一轮 `runNpcClickPipeline(..., "dialog-after-alt-c")`。
  - 未改 tooltip、黄字 OCR、记忆点、Ctrl 菜单、公式点击等内部识别/点击算法。

Verify:

- `mvn -q -DskipTests compile` passed.

Follow-up cleanup:

- 用户确认游戏内没有旧的无效下坐骑快捷键用途。
- 已删除输入层旧快捷键链路：
  - `InputActionType` 里的旧 action type。
  - `InputAction` 里的旧 action factory。
  - `InputSequences` 里的旧 shortcut helper。
  - `WinApiMouseController` 里的旧物理 fallback。
- 全局搜索确认不再存在旧快捷键相关标识。

### 何黎 - 2026-06-13 修罗 story 目标坐标用地图范围校验修正

Status: implemented / replay passed

Context:

- 修罗接任务后的 story objective 明明是 `瑶池(78,64)`，日志里却被识别成 `瑶池(778,64)`，随后接近点被算成 `(776,62)`，导致小地图导航目标完全错误。
- 子智能体 Nash 做了只读复查，确认根因在 `ObjectiveTextRecognitionService`：坐标字符串只要满足 `\d{1,3},\d{1,3}` 就会被接受，没有结合地图名做范围校验。

Changed:

- `ObjectiveTextRecognitionService`
  - 注入 `CoordinateHelper`。
  - 坐标识别后先用已匹配到的地图名做 `isLogicalCoordinatePlausible(...)` 校验。
  - 如果原始坐标超出该地图范围，并且去掉 X 最左侧一个疑似噪声数字后落入地图范围，则修正坐标。
  - 不做硬编码，不禁止合法三位数坐标；未知地图仍保持原行为。

Testcase:

- Input: `images/test-cases/objective-text/raw/story_yaochi_78_64_extra7_raw.png`
- Manifest: `images/test-cases/objective-text/manifest.csv`
- Marked output: `images/test-cases/objective-text/output/story_yaochi_78_64_extra7_raw_marked.png`
- Replay tool: `src/main/java/com/bot/dhxy/debug/ObjectiveTextRecognitionReplayDebugMain.java`

Verify:

- `git diff --check -- src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java src/main/java/com/bot/dhxy/debug/ObjectiveTextRecognitionReplayDebugMain.java images/test-cases/objective-text/manifest.csv`
- `mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.bot.dhxy.debug.ObjectiveTextRecognitionReplayDebugMain" "-Dexec.args=--all"`
- Result: `OBJECTIVE_TEXT_REPLAY total=1 passed=1 failed=0`

### 谢帅 - 2026-06-13 小地图楼层模板命中后不再被 OCR 覆盖

Status: implemented / compile passed

Context:

- 用户追查 `19:36` 附近修罗路线时发现：小地图模板已经命中 `龙窟六层`，但后续仍走楼层 OCR 复核，并被 OCR/canonicalize 改成其它楼层，导致任务认为当前位置不是目标地图并重新导航。
- 结论：小地图模板匹配比 OCR 更适合判断楼层名；OCR 不能作为更高优先级结果覆盖已命中的模板。

Changed:

- `LocationVisionService.scanByMiniMapTemplate(...)`
  - 楼层地图模板命中后始终返回模板结果。
  - 分数低于阈值时仍允许跑 OCR 学习缺失 map label 模板，但 OCR 结果只打日志/学习，不再 `return local` 覆盖模板地图。
  - 日志从 `floor template corrected by OCR` 改为 `floor template OCR disagreed but template kept`。

Verify:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-13 队长路线传送 prepared action 提到 pathing guard 之前

Status: implemented / compile passed

Context:

- 用户观察 `22:35:30` 到 `22:36:30` 队长前往 `兰若寺` 时，世界地图/路线相关窗口看起来一直开着没人关。
- 日志确认：
  - `22:36:09.723` 已匹配路线面板 x2；
  - `22:36:10.141` `route panel x2 close ... closed=true`，所以世界地图路线搜索框实际已关闭；
  - `22:36:12.646` watcher 已准备好 `ROUTE_TRANSFER` prepared action，点击点 `(1057,797)`；
  - 但 `navigateToMap` 先走 stale-cache/pathing guard，旧 `PATHING_ACTIVE` 状态会让任务先返回，导致 prepared action 到 `22:36:17` 才被消费。

Changed:

- `NavigationService.navigateToMap(...)`
  - 在 stale-cache/pathing guard 前增加高优先级检查：如果当前窗口已有同目标、同绑定、未过期的 `ROUTE_TRANSFER` prepared action，立即调用 `clickRouteDialogOption(...)` 消费。
  - 保持 watcher 负责后续到达确认；不改世界地图搜索、OCR、路线记忆算法。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-12 五环/五倍路线对话框 prepared action 有效期放宽到 10 秒

Status: implemented / compile passed

Context:

- 用户在多窗口测试中看到多个窗口已经弹出路线/传送 option dialog，但切回前台时没有直接点击，后续又重新打开世界地图搜索。
- 日志确认其中一个关键原因是后台 prepared action 已经算出点击点，但前台拿回 turn 时超过了 `ROUTE_DIALOG_PREPARED_CLICK_MAX_AGE_MS=2500ms`：
  - `hwnd-1760240` 在 `20:19:51.256` 准备好 `皇宫门口（800两）`，`20:19:55.355` 前台回来时 `verifiedAgeMs=4099`，被判定不可直接使用。
  - 大叔窗口同类路线 OCR/准备也能成功，但后台 OCR 有时耗时 9-12 秒，仍需后续性能优化。

Changed:

- `NavigationService`
  - `ROUTE_DIALOG_PREPARED_CLICK_MAX_AGE_MS` 从 `2500ms` 放宽到 `10000ms`。
  - 不改路线 OCR、记忆点、世界地图点击算法。

Related finding:

- 用户提到的忍者窗口这次“断了”不是同一个 route dialog 过期问题。
- 最新日志中 `hwnd-12C0E9A / 『忍者』影 / ID=443075411` 是在 `BUY_SHOES` 阶段失败：
  - `20:23:04.527` `wuhuan-v2:BUY_SHOES result=FAILED`
  - `20:23:04.529` `phase=BUY_SHOES result=FAILED message=shoe-shop buy phase exceeded retries`
  - 当时窗口仍在 `万寿山(212,13)`，说明买鞋进店路径/确认没有成功，超过了买鞋阶段 retry 上限。

Follow-up change:

- `FiveRingTaskV2.buyShoes(...)`
  - 删除 `BUY_SHOES` 阶段的固定 retry 上限退出。
  - 买鞋/进店失败继续使用原来的 `retrySamePhase(...)` 路径放权重试，不再把五环 V2 标成 `FAILED`。
  - 保留任务停止/中断的 `TaskCheckpoint`，人工停止仍然会停。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-12 五倍接任务 NPC tooltip 命中后不再继续黄字/Ctrl 兜底

Status: implemented / compile passed

Context:

- 用户查看 `15:20:50` 附近日志时发现：五倍接任务 NPC `降魔侍卫` 已经被点击并打开接任务对话框，但后续仍然进入黄字 OCR 和 Ctrl 菜单探测。
- 日志链路：
  - `15:20:36` tooltip 模板命中并点击 `降魔侍卫`。
  - `15:20:39` `wubei_accept_chumoweiguo.png` 验证成功。
  - 之后仍继续执行 player-anchor、yellow target、Ctrl menu fallback。

Root cause:

- `NpcClickService.runNpcClickPipeline(...)` 中，`TaskType.WUBEI` 先走 `tryNormalTooltipStrategy(...)`，但之前没有检查返回值。
- 即使 tooltip 点击已经验证成功，pipeline 仍继续执行后续策略。

Changed:

- `NpcClickService.runNpcClickPipeline(...)`
  - 五倍 tooltip 策略返回 `true` 时立刻设置 `result=true` 并返回。
  - 不改黄字 OCR、Ctrl 菜单、公式点击算法本身。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-12 五倍 probe story 白名单处理

Status: implemented / compile passed

Context:

- 五倍白龙马显形镜 probe fallback 里，之前会先检查 story 模板，后面 `NpcClickService` 又可能执行 `closeStoryBeforeDirectSceneClick`，导致重复截图/洗图/handleDialog。
- 用户确认语义应是白名单式处理：
  - 命中“目标出现/可点怪”模板：不关 story，直接继续尝试进战斗。
  - 命中“位置不对”模板：不关 story，回滚本次显形镜尝试并重走当前绿字寻路。
  - 有 story 但两个模板都没命中：认为是未知 fallback blocker，五倍自己清一次 story。

Changed:

- `WubeiTask.resolveProbeAfterPathing(...)`
  - 已知两个 probe story 模板都只做分流，不清 dialog。
  - 新增未知 story 清理：仅当 `VERIFY_WHITE_TEMPLATE` 返回 `WHITE_TEMPLATE_NOT_FOUND` 且 `dialogType=STORY` 时调用一次 `DialogHandleRequest.clickStory(...)`。
- `WubeiTask.tryClickTrackerCombatTargetSmart(...)`
  - `NpcClickRequest.closeStoryBeforeDirectSceneClick(false)`，避免 `NpcClickService` 对五倍 probe 再做盲清。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-12 单窗口硬放权回收慢的架构待优化点

Status: noted / not implemented

Context:

- 用户观察到只开一个队长窗口时，任务仍然会按多窗口模型硬放权。
- 在没有其它窗口可运行的情况下，放权后仍可能等数秒才重新拿回任务 turn，表现为“明明没有别人要做事，当前窗口也站着等”。
- 这个问题不是某一个业务步骤必然要等，而是 runner / watcher / task yield 的调度策略还偏向多窗口公平轮询。

Current concern:

- 多窗口场景下，移动、战斗、长等待放权是必要的。
- 单窗口或“其它窗口都不可运行”的场景下，硬放权会变成纯延迟。
- 如果当前窗口已经有 watcher 结果或 prepared dialog action，任务应该更快恢复，不应等待旧 pathing 状态自然老化。

Future optimization idea:

- 区分 hard yield 和 soft yield。
- 单窗口 fast path：只有一个 active window 时，yield 后只给 watcher 一个很短的刷新机会，然后快速恢复当前任务。
- 多窗口 fast path：如果没有其它 runnable window，也快速恢复当前窗口。
- prepared action 优先：后台已经算好的对话框点击点应高于普通轮询等待。

Open decision:

- 需要后续检查 `WindowTaskRunner` / task turn coordinator 的调度策略，确认 yield 后慢在哪里，再决定是否实现 `no other runnable window -> immediate/short resume`。

### 谢帅 - 2026-06-12 五倍路线对话框 prepared action 不再被 pathing gate 卡住

Status: implemented / compile passed

Context:

- 最新日志里 `平顶山 -> 宝象国` 的路线对话框已经由 watcher 使用记忆点预处理成功：
  - `dialog prepare remembered route result ... prepared=true totalMs=879`
  - 但五倍 `accept NPC pathing still active` 门禁一直按旧 pathing intent yield，直到 watcher 老化成 `STOPPED_AWAY` 才重新进入 `NavigationService`。
- 所以用户看到的 5-10 秒延迟不是记忆点慢，而是五倍任务层没有让已经 ready 的路线对话框优先被消费。

Changed:

- `WubeiTask.waitForAcceptNpcPathingIfStillActive(...)`
  - 在继续 yield 前检查当前窗口是否已有同目标 `ROUTE_TRANSFER` prepared action。
  - 如果 action 仍在有效期内，释放五倍 pathing gate，让 `NavigationService` 立刻消费后台算好的点击点。
  - 不改 OCR、记忆点算法、导航点击算法。

Verify:

- `mvn -q -DskipTests compile` 通过。

### 谢帅 - 2026-06-12 当前位置 OCR 地图名入库前校对

Status: implemented / compile passed

Context:

- `PlayerStateService.syncMyPosition()` 会把 `LocationVisionService.scanCurrentLocation()` 返回的 `LocationInfo.mapName` 直接写入 `me.currentMapName`。
- 小地图模板路线通常返回合法模板名，但 local/Baidu OCR 路线之前会把 OCR 原始地图名直接返回，可能把 `火云洞` 存成 `火云铜`。

Changed:

- `LocationVisionService`
  - 注入 `MapNameCanonicalizer`。
  - local OCR、Baidu OCR、楼层模板 OCR 校验结果进入业务状态前统一执行地图名 canonicalize。
  - 坐标保持不变，只替换 `LocationInfo.mapName`。
  - 新增日志 `[location] OCR map name canonicalized...`，便于追踪 raw/canonical 映射。

Verify:

- `mvn -q -DskipTests compile` 通过。

### 谢帅 - 2026-06-12 五倍 destination hint 地图名解析即刻校对

Status: implemented / compile passed

Context:

- 五倍绿字点击后的寻路 hint OCR 曾把 `火云洞` 读成 `火云铜`。
- 项目已有 `MapNameCanonicalizer`，但五倍 `parseTrackerDestinationHint(...)` 之前只在后续地图比较时临时校对，`TrackerDestinationHint.mapName` 本身仍保存 OCR 原文。

Changed:

- `WubeiTask.parseTrackerDestinationHint(...)`
  - 解析出 raw map name 后立刻调用 `mapNameCanonicalizer.canonicalize(...)`。
  - `TrackerDestinationHint` 传给后续流程的 `mapName` 改为 canonical 后的合法地图名。
  - `rawText` 仍保留原始 OCR 文本，方便日志追查 OCR 原始结果。

Verify:

- `mvn -q -DskipTests compile` 通过。

### 谢帅 - 2026-06-11 五倍 task panel 停止保存 wide 图

Status: implemented / compile blocked by existing unrelated errors

Context:

- 用户指出五倍 task panel test case/temp 里同时保存了两种大小的图。
- 大图 `wubei_tracker_panel_*_wide_raw.png` 只是早期诊断用，不应继续写入该路径。

Changed:

- `WubeiTask.captureTrackerPanelSnapshot(...)`
  - 删除额外的 wide tracker panel 截图保存。
  - 日志不再输出 `wideCaptured` / `wideRect` / `wide` 路径。
- 清理已有 `*_wide*.png`：
  - `images/temp/**/wubei_tracker_panel*_wide*.png`
  - `images/test-cases/task-tracker/wubei-task-panel/**/wide*.png` / `*wide*.png`

Verify:

- 检查结果：`tempWide=0 testCaseWide=0`。
- `mvn -q -DskipTests compile` 当前未通过，但失败来自现有未完成改动：
  - `TaskTrackerPanelService` 缺少返回语句/缺少方法；
  - `WubeiTask` / `FiveRingTaskV2` 调用了当前 `TaskTrackerPanelService` 中不存在的方法。
  - 本次 wide 图清理没有新增这些编译错误。

### 谢帅 - 2026-06-11 自动战斗面板拖动点改用原图蓝点锚点

Status: implemented / compile passed

Context:

- 用户确认自动战斗面板拖动不应再用 `fallback anchor + 30Y` 推出来的绿字/红点位置。
- 当前策略需要区分两个点：
  - `panelCenter`：真实拖动起点，使用原图 `auto_panel_fallback_anchor.png` 命中的蓝点。
  - `greenMarker`：只用于回合红字截图，由蓝点按 `(+30, +30)` 推出到“自动”模板中心。
- 如果主锚点失败并走“自动”绿字兜底，则用绿字中心按 `(-30, -30)` 反推拖动起点。

Changed:

- `AutoCombatPanelService`
  - 面板拖动目标 offset 从 `(450, 760)` 调整为 `(489, 726)`，对应用户给定 base `(1490, 561)` 下目标屏幕点约 `(1979, 1287)`。
  - `panel-anchor` 命中时返回 `panelCenter=fallbackPoint`、`greenMarker=fallbackPoint + (30, 30)`。
  - `green-auto` 命中时返回 `panelCenter=greenPoint - (30, 30)`、`greenMarker=greenPoint`。
  - 绿字兜底日志增加 `inferredPanelAnchor`，方便确认反推点。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 自动战斗面板改为原图锚点优先

Status: implemented / compile passed

Context:

- 用户要求先确认五个窗口里 `auto_panel_fallback_anchor.png` 是否都能命中；如果稳定，就把该原图模板作为主要匹配对象。
- 当前日志中，多数窗口原本都走 fallback anchor；只有 `hwnd-28812BE` 经常先命中旧的整条绿字模板。

Findings:

- 当前五个窗口 `latest_vision.png` 离线匹配 `auto_panel_fallback_anchor.png` 全部成功：
  - `hwnd-2C416E6`: `0.9967`
  - `hwnd-E60101E`: `0.9967`
  - `hwnd-F0D1194`: `0.9967`
  - `hwnd-28812BE`: `0.9518`
  - `hwnd-3D80DD0`: `0.9967`
- 旧的 `quxiao_zidong_green.png` 整条绿字模板在失败图上最高只有 `0.3887`，低于 `0.80` 阈值。

Changed:

- 新增 `images/template/battle/zidong_green.png`，从旧绿字模板裁出“自动”两个字作为兜底模板。
- `AutoCombatPanelService.findAutoCombatBox(...)`
  - 优先在原图上匹配 `auto_panel_fallback_anchor.png`。
  - 命中后仍按 `fallbackPoint.y + 30` 反推自动面板 marker。
  - 只有原图 anchor 失败时，才洗绿字并尝试匹配 `zidong_green.png`。
  - 日志 method 调整为 `panel-anchor` / `green-auto`，更容易区分主路径和兜底路径。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 五倍接任务 NPC 到达容差接入

Status: implemented / compile passed

Context:

- 用户实跑五倍时，队长已经在 `降魔侍卫` 附近，但仍反复打开小地图点击 `宝象国(86,87)`。
- 日志显示当前位置曾为 `宝象国(80,86)`，默认导航容差为 5，`dx=6` 被判定未到达。
- 五倍任务自身已有 `ACCEPT_NPC_DIRECT_CLICK_DISTANCE = 12`，但之前没有传给 `NavigationRequest`。

Changed:

- `WubeiTask.runRouteToNPC(...)`
  - 接任务 NPC 导航请求增加 `.arrivalTolerance(ACCEPT_NPC_DIRECT_CLICK_DISTANCE)`。
  - 含义：到 `降魔侍卫` 附近 12 格以内时，认为已经到达，可以进入后续 NPC 点击/接任务流程，不再继续小地图点同一个目标坐标。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 自动战斗面板识别与回合 OCR 增加日志

Status: implemented / compile passed

Context:

- 用户需要实测自动战斗面板识别是否命中，以及回合红字 OCR 是否成功。
- 本次是加 console log，不是新增测试用例。

Changed:

- `AutoCombatPanelService`
  - 面板可见日志增加 `method`、`center`、`marker`：
    - `green-marker` 表示通过 `quxiao_zidong_green.png` 命中。
    - `fallback-anchor` 表示通过 `auto_panel_fallback_anchor.png` 命中后反推 marker。
  - 红字回合 OCR 前打印截图计划：
    - `source`
    - `method`
    - `center`
    - `marker`
    - 截图 `rect`
  - 红字 OCR 成功时打印：
    - `rounds`
    - `redPixels`
    - OCR 文本
    - 截图 rect
  - 红字 OCR 失败或异常时才保留 raw/washed 图片，并在日志里打印路径。
  - 红字 OCR 成功时删除临时 washed 图，避免正常跑时持续堆截图。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 自动战斗面板拖动锚点改为取消自动中心

Status: implemented / compile passed

Context:

- 用户确认当前自动战斗面板拖动应以“取消自动”模板命中的中心点作为鼠标按下位置。
- 实测窗口 base 为 `(1168, 194)`，目标拖动屏幕点为 `(1618, 954)`。

Changed:

- `AutoCombatPanelService`
  - 面板拖动目标 offset 调整为 `(450, 760)`。
  - 绿字命中后，`panelCenter`/拖动点改为 `greenPoint` 本身，不再使用旧的 `(+20, -28)` 偏移。
  - Snipaste fallback 推出的 `inferredGreenMarker` 也直接作为拖动点。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 自动战斗面板兜底改用原图锚点

Status: implemented / compile passed

Context:

- 用户确认不再使用 `zidonghai_white.png` / 白字“自动还”作为自动战斗面板 fallback。
- 新增 `images/template/battle/auto_panel_fallback_anchor.png`，用于在原始截图上直接 image match。

Changed:

- `AutoCombatPanelService`
  - 删除白字 fallback 路径常量和 `countThinWhitePixelsHSV(...)` fallback 匹配。
  - 新增 `AUTO_PANEL_FALLBACK_ANCHOR_PATH`，在 raw screenshot 上直接匹配 Snipaste 原图模板。
  - Snipaste fallback 命中后，按 `inferredGreenMarker = (fallbackPoint.x, fallbackPoint.y + 30)` 反推取消自动中心点。
  - 反推点继续作为红字回合截图锚点，并继续使用当前 `quxiao_zidong_green.png` 的宽度计算红字区域。

Verify:

- `rg` 确认 `AutoCombatPanelService` 不再引用 `zidonghai` / `debug_thin_white_text` / white marker。
- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 自动战斗回合红字截图改用取消自动绿字锚点

Status: implemented / compile passed

Context:

- 自动战斗面板更新后，原先按固定面板宽高从中心点推算红字回合区域不够稳。
- 用户要求以“取消自动”绿字模板命中点作为红字截图锚点。

Changed:

- `AutoCombatPanelService`
  - 内部面板匹配结果从单个 `Point` 扩展为私有 `AutoCombatPanelMatch`，保留对外 `ensurePanelVisible(...) -> Point` 接口不变。
  - 绿字命中后记录 `greenMarker` 和当前取消自动模板宽度。
  - `readRemainingRounds(...)` 优先按绿字 marker 截红字区域：
    - `left = greenMarker.x`
    - `top = greenMarker.y - 96`
    - `right = left + quxiaoTemplateWidth / 2`
    - `bottom = top + 30`
  - 白字 fallback 命中但没有绿字 marker 时，继续用旧的面板中心区域兜底。
  - 如果面板被拖动，拖动后重新扫描一次面板，避免继续使用拖动前的绿字坐标。

Verify:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 current-map 小地图改为确认移动后同步关闭

Status: implemented / compile passed

Changed:

- `NavigationService.clickMiniMapPointForHandoff(...)`
  - 删除 current-map handoff 成功后的异步小地图关闭。
  - 确认小地图点击已经触发移动后，改为同步通过 input queue 发送 `Alt+1` 关闭小地图，再返回 `PATHING_STARTED`。
- `NavigationService.closeMiniMapAfterConfirmedPathing(...)`
  - 新增同步收尾方法，明确 current-map 小地图生命周期由 current-map 自己关闭。
  - `Alt+1` 后会用小地图 checkbox 模板确认是否仍可见；如果还可见，复用 `UICleanerService.closeAllGenericWindows()` 走通用关闭按钮兜底。
  - 如果同步关闭请求被中断/取消，不再继续跑模板确认或通用关闭兜底。
- `NavigationService.registerWindowPathingIntent(...)`
  - 删除 current-map `PATHING_STARTED` 后继续标记 `uiCleanupRecommended` 的旧逻辑；现在小地图已经在 handoff 返回前同步关闭，不再让五环后续额外消费旧 cleanup 标记。

Behavior:

- 点击失败 / 未触发移动：不关闭小地图，继续在本轮 fallback 下一个候选点。
- 点击成功 / 已触发移动：立刻同步关闭小地图；如果模板仍显示小地图存在，再点通用关闭按钮兜底，然后才注册 intent / 让权。
- 所有候选失败、超时或普通退出：仍走 `navigateInCurrentMap:finish` 的 `closeMiniMapIfOpen(...)` 收尾。

Reason:

- current-map 的小地图不会像跨地图路线那样被游戏自动关闭；异步关闭责任边界不清晰，也容易让后续窗口动作看起来被后台 UI 动作干扰。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 current-map retry 恢复为本轮确认移动后再放权

Status: implemented / compile passed

Changed:

- `NavigationService.navigateInCurrentMap(...)`
  - 撤销“watcher `STOPPED_AWAY` 后下一轮推进 fallback”的方案。
  - `failedMiniMapClicks` 恢复为本次调用内的局部计数。
  - 让权模式下，小地图点击必须先确认已经触发移动，才注册 `WindowPathingIntent` 并返回 `PATHING_STARTED`。
  - 如果一次小地图点击没有触发移动，不放权、不注册 intent，直接在同一个 foreground turn 里继续尝试下一个 fallback 点。
- `NavigationRuntimeState`
  - 删除 current-map retry 目标和失败计数字段，避免同一套 retry 语义被拆到多个 turn。
- `NavigationService.clickMiniMapPointForHandoff(...)`
  - 恢复快速边缘像素确认 + 坐标 fallback 确认。
  - 只有确认 `PATHING_STARTED` 后才异步关闭小地图并交给 runner 后台观察。

Reason:

- 用户实测认为小地图点失败时不能放权，否则第 N 个 fallback 点才成功的场景会被拆成 N 轮，整体延迟过大甚至超时。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-10 current-map handoff 改为点击提交即交给 runner

Status: implemented / compile passed

Changed:

- `NavigationService.navigateInCurrentMap(...)`
  - 删除每轮 `context.setCurrentActionState(GameContext.ActionState.NAVIGATING)`，不再由 current-map loop 硬写动作状态。
  - 让权模式的小地图点击不再前台跑 `isMovingByPixelDiff` / 坐标确认；`submitMiniMapClick(...)` 成功就返回 `PATHING_STARTED`。
  - `PATHING_STARTED` 后立即注册 `WindowPathingIntent`，再异步关闭小地图。
- `NavigationService.clickMiniMapPointForHandoff(...)`
  - 改成纯 handoff 提交函数：只负责提交小地图点击，移动/到达/停半路交给 runner/watcher。
  - 删除旧的 `confirmMiniMapPathingStartedForHandoff(...)` 前台确认路径。
- `isCurrentCachedCoordinateNear(...)`
  - 如果当前窗口有同目标的新鲜 pathing snapshot，优先按 snapshot 判断是否 `ARRIVED`，避免旧 `PlayerCharacter` 缓存抢先判到达。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-10 current-map pathing intent 注册点前移

Status: implemented / compile passed

Changed:

- `NavigationService.navigateInCurrentMap(...)`
  - 增加 `pathingIntentRegistered` 标记，避免 finally 重复注册。
  - 在 current-map 小地图点击确认 `PATHING_STARTED` 时，立即注册 `WindowPathingIntent`。
  - 在让权模式下，如果点击已提交但本地短确认没有看到移动，也立即注册 intent，交给 runner/watcher 判定 `ACTIVE / ARRIVED / STOPPED_AWAY`。
- `registerWindowPathingIntent(...)`
  - 从 `void` 改为 `boolean`，返回是否实际写入当前窗口 `WindowRuntimeContext`。

Intent registration locations:

- `navigateToMap`：跨地图路线返回 `PATHING_STARTED` 后注册，坐标为空。
- `navigateInCurrentMap`：当前地图小地图点击后注册，带目标坐标。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-10 current-map 导航战斗检测改为消费已同步状态

Status: implemented / compile passed

Changed:

- `NavigationService.navigateInCurrentMap(...)`
  - 删除 current-map loop 里的主动 `battleRadarService.checkAndSyncCombatState()`。
  - 改为只读取 `GameContext.ActionState.IN_COMBAT`。
  - 含义：战斗截图/同步由 runner 的 window combat watcher 负责；导航只消费已经同步好的战斗状态。

Validation:

- `mvn -q -DskipTests compile` passed.
- `NavigationService` 中已无 `battleRadarService.checkAndSyncCombatState()` 调用。

### 谢帅 - 2026-06-10 删除小地图 exact 分支并收拢随机开关

Status: implemented / compile passed

Changed:

- 删除 `NavigationRequest.exactMiniMapClickOnly` 字段。
- 删除 `NavigationService.navigateInCurrentMap(...)` 里的 exact mini-map click 专门 block。
- `CoordinateHelper.resolveMiniMapClickPoint(...)` 保持唯一 public 入口，不保留 overload。
- `NavigationRequest.randomizeMiniMapClickPoint` 控制小地图物理点是否加随机偏移：
  - 默认 `true`，普通导航仍然随机。
  - 五环买鞋入口传 `false`，走同一条导航流，但 130,130 不加随机偏移。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-10 旧 FiveRingTask 停用后的入口与移动函数改名收尾

Status: implemented / compile passed

User change:

- 旧 `FiveRingTask.java` 已被整文件 comment 掉。
- `GameStateUtil` 中原来用于“刚点击后快速确认移动”的方法改名为 `isMovingByPixelDiff(String reason)`。

Changed:

- `DefaultTaskFactory`
  - 移除旧 `ObjectProvider<FiveRingTask>` 注入。
  - `TaskType.WUHuan` 现在直接返回 `FiveRingTaskV2`，所以 UI/队列里原来的“五环”入口不会因为旧 class 停用而编译失败。
  - `TaskType.WUHuan_V2` 仍然返回同一个 `FiveRingTaskV2`。
- `NavigationService` / `DebugNavigationStressTask`
  - 把旧 `confirmPathingStartedByEdgePixelDiff(...)` 调用统一改成新的 `isMovingByPixelDiff(String reason)`。

Notes:

- `TaskTeamAssignmentPolicy.isFiveRingTask(...)` 和 `DefaultWindowTaskStartupInitializer.isFiveRingTask(...)` 保留；它们是任务类型策略判断，不依赖旧 `FiveRingTask` class。
- `FiveRingTask.java` 当前只剩注释文本引用，未删除文件，方便用户继续对照旧逻辑。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-10 通用路线 Dialog 记忆记录接入

Status: implemented / compile passed

User decision:

- 先不把 `fromX/fromY` 纳入可用性过滤，保留现有 `fromMap -> targetMap` 维度，实跑观察是否会误用。
- `relativeX/relativeY` 继续按现有大 Dialog 扫描区域的左上角计算，不改存储结构。

Changed:

- `NavigationService.clickRouteDialogOption(...)`
  - prepared action、late prepared action、remembered action、OCR/fallback action 只要实际成功点击路线选项，就统一调用 `TransferChoiceMemoryService.recordSuccess(...)`。
  - 记录 source 会带上 `:prepared` / `:late-prepared` / `:memory` / `:ocr`，方便后续从日志里区分是哪条路径学到的。
- 移除了只给灵兽村使用的旧 `recordRouteDialogOutcome(...)` 死代码，避免同时存在“点击后记录”和“到达后记录”两套语义。

Validation:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮实跑看普通路线 dialog 是否出现 `[transfer-memory] success key=...`，并确认灵兽村不会重复累计 success。
- 如果后面出现误用记忆点，再把 `fromX/fromY` 或当前角色坐标容差加回 `findUsable(...)`。

### 谢帅 - 2026-06-08 NPC 点击恢复完整策略 / 黄字等待降到 800ms

Status: implemented / compile passed

User request:

- 黄字坐标回填确认后，把之前临时 comment 掉的非黄字 NPC 点击策略全部恢复。
- 黄字点击后的首次验证等待从 2000ms 降到 800ms，减少可见卡顿。

Changed:

- `NpcClickService.runNpcClickPipeline(...)`
  - 恢复 task tooltip、learned memory、普通 tooltip、story close、player-anchor formula、formula Ctrl、yellow target、最终 Ctrl 菜单兜底的原有 pipeline 顺序。
  - 移除“黄字-only 调试”的临时 DEBUG 跳过逻辑。
- `NpcClickService.clickNpcByYellowTargetName(...)`
  - `npcClick:yellowTargetMoveClick` 的首次等待从 `2000ms` 调整为 `800ms`。

Validation:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮实跑重点看 `npcClick:yellowTargetMoveClick` 从点击到 `firstVerify` 的耗时是否下降；如果 Dialog 验证仍慢，再看是否能跳过验证前的 `Alt+4`。

### 谢帅 - 2026-06-08 黄字 NPC OCR 坐标回填修正

Status: implemented / compile passed

User report:

- 黄字路径识别 `降魔侍卫` 后点击到了 `(544,445)`，但用户实测名字中心应在 `(987,770)` 附近。
- 后台用同一张 `latest_vision.png` 洗黄字后，候选真实位置可算到屏幕 `(993,770)`，说明文本候选本身可定位，问题在 OCR 坐标回填。

Changed:

- `GameTextLineOcrService.collectYellowCandidates(...)`
  - 候选 packed 图 OCR 后，不再直接把 packed 图坐标当作原图坐标。
  - 新增使用已有 `mapPackedWordsToRaw(packedWords, packedLines)`，后续 `joinText`、匹配、候选记录和点击坐标都使用还原到原始截图坐标的 `words`。

Validation:

- 后台离线计算确认目标黄字候选中心约为屏幕 `(993,770)`，与用户标注 `(987,770)` 只差约 6px。
- `mvn -q -DskipTests compile` passed.

Next:

- 继续实跑黄字-only 路径，确认日志里的 `NPC yellow target matched` 点击点不再是 `(544,445)`，应接近 `(993,720)` 或按当前点击偏移落在目标 NPC 上方合理位置。

### 谢帅 - 2026-06-08 NPC 点击临时黄字-only 调试

Status: implemented / compile passed

User request:

- 临时把 `NpcClickService` 里非黄字 NPC 点击路径全部停掉，只看黄字识别。

Changed:

- `NpcClickService.runNpcClickPipeline(...)`
  - 临时跳过任务 tooltip、记忆点、玩家紫名公式点、公式点附近 Ctrl 菜单、最终 Ctrl 菜单兜底。
  - 当前只执行 yellow-name visual path，即 `clickNpcByYellowTargetName(...)`。
  - 保留黄字 OCR 日志和 yellow evidence 记录，用于下一轮实跑直接确认黄字截图/洗图/OCR/点击验证结果。

Validation:

- `mvn -q -DskipTests compile` passed.

Important:

- 这是临时调试状态，不是生产策略。黄字问题确认后，需要恢复完整 NPC click pipeline。

### 谢帅 - 2026-06-08 五倍启动 Alt+6 / 补给 / 摄妖香补齐

Status: implemented / compile passed

User report:

- 五倍启动时队长没有按 Alt+6 屏蔽队员。
- 启动时没有吃摄妖香。
- 启动时人物/宝宝血法没有补给。

Findings:

- 最新日志里五倍 leader 已进入 `DefaultWindowTaskStartupInitializer`，但 `config/ui-game-settings.properties` 当前 `taskStartupPreparationEnabled=false`。
- 因为这个开关关闭，`TaskStartupWindowPreparationService.prepareTaskStartupWindow()` 直接打印 `task startup preparation skipped: develop switch disabled` 并跳过 map/Alt+6。
- 五倍自身 `WubeiTask.execute()` 没有调用 `PlayerStateService.performStartupFirstAidCheck(...)`，所以启动前不会做人物/宝宝血法补给。
- 五倍原本只在战后 `wubei:post-combat` 路径检查摄妖香；日志确认第一场战斗后才补了摄妖香，不是启动前补。

Changed:

- `DefaultWindowTaskStartupInitializer`
  - 五倍 leader 启动时先执行一次 `ensureAlt6VisibilityOnly()`，即使重型“任务启动前置检查”开关关闭，也会保证 Alt+6 隐藏玩家浮层。
  - 队员/auto-battle 窗口仍然跳过启动热键，避免挂机队员抢窗口。
- `WubeiTask`
  - 任务开始后、第一轮接任务前调用 `performStartupFirstAidCheck(context)`。
  - 同一位置调用 `ensureSheYaoXiangActiveForLeaderTask("wubei:startup", context)`，只允许 leader/solo 主任务窗口处理摄妖香。

Validation:

- `mvn -q -DskipTests compile` passed.

Next:

- 下次实跑五倍启动时重点看：
  - `startup init: wubei ensure Alt+6 visibility before leader prep`
  - `task startup visibility: press Alt+6` 或 `already confirmed`
  - `🩺 启动急救检查`
  - `摄妖香检查允许：source=wubei:startup`

### 谢帅 - 2026-06-07 五环 V2 代码膨胀复盘

Status: investigated / refactor plan needed before adding more fixes

User concern:

- `FiveRingTaskV2.java` 已膨胀到 2844 行，远超五环业务本身复杂度。
- 当前症状不是单个判断 bug，而是主 task 同时承担太多职责，导致每次修补都容易引入新的窗口让权、tracker、dialog 或导航副作用。

Findings:

- `FiveRingTaskV2` 当前混在一起的职责：
  - 五环主状态机和 turn / handoff 策略。
  - 启动供给检查、摄妖香、鞋子检测、快捷买鞋、店铺买鞋、出店兜底。
  - 接任务、已有任务接管、云游大师点击、接任务 dialog 分支。
  - 左侧任务追踪 panel 截图、黄字找“五环”、绿字分段、点击点计算、debug 图片输出。
  - watcher pathing intent 注册与等待策略。
  - 若干临时 retry / cleanup / legacy QuestManager 收尾确认。
- `TaskTrackerPanelService` 已经存在，并且已经能做左侧五环任务追踪 panel 后台预计算；但 `FiveRingTaskV2` 里仍保留一套类似的 tracker 截图/洗图/绿字解析/点击算法。这是最明显的重复和膨胀来源。
- 买鞋逻辑约占 `FiveRingTaskV2` 前半段大量代码，但买鞋是供给/包裹/NPC 店铺流程，不应该长期和主状态机、tracker 解析塞在同一个 class 里。
- 对照旧版 `FiveRingTask.java`：旧版约 904 行，主结构是 `prepare -> handover/setup -> main loop`，业务边界更窄。V2 约 2844 行但稳定性更差，说明问题不是五环任务复杂，而是 V2 把太多补救/识别/让权细节塞进了任务类。

Proposed cleanup direction:

1. 暂停继续往 `FiveRingTaskV2` 里加补丁，先把状态机主干压回旧五环这种窄流程。
2. 不建议在当前 2844 行 V2 上继续局部修；更合理的是以旧 `FiveRingTask` 的 900 行骨架为基准，重新接入已经验证过的少数新能力。
3. 新五环应只保留这些新增点：
   - 左侧任务追踪 panel 作为任务进行中的来源，不再中途读 Auto+Q。
   - `DebugNavigationStressTask` 验证过的 pathing 后快速让权模型。
   - 必要的买鞋能力，但买鞋应作为供给能力被调用，而不是散落在主状态机里。
4. 优先删除/迁移重复的左侧 tracker 解析逻辑：
   - 五环主 task 只调用 `TaskTrackerPanelService` 或其扩展结果。
   - 主 task 不再自己维护 `findWuhuanTitleAnchor`、`scanTrackerGreenLinks`、`splitTrackerGreenLinkSegments` 这一整套算法。
5. 把买鞋流程从主状态机中抽离到现有供给/包裹/NPC 能力边界附近；如果必须保留五环专属策略，也要让主 task 只看到一个“确保有鞋”的结果，而不是直接持有所有店铺细节。
6. 保留五环主文件只做：
   - `PREPARE -> HANDOVER_DETECT -> ACCEPT_TASK -> SYNC_TASK_PANEL -> WAIT_PATHING -> CHECK_COMBAT -> HANDLE_DIALOG` 的状态流转。
   - turn/yield 决策。
   - 失败 message 汇总。
7. 每次瘦身后必须跑 `mvn -q -DskipTests compile`，并用现有 replay/debug case 验证任务追踪绿字点击没有退化。

Next:

- 先不要继续改导航/runner 算法。
- 下一步应先切掉 `FiveRingTaskV2` 中和 `TaskTrackerPanelService` 重复的 tracker reader 代码，让主 task 只消费 prepared/click result。

### 谢帅 - 2026-06-06 五环 V2 左侧任务追踪 panel 后台预计算

Status: implemented / compile passed / waiting live rerun

2026-06-07 correction after user review:

- Do not confuse two different 五环 V2 limits:
  - 接任务阶段的 dialog / 云游大师确认框属于 `ACCEPT_TASK` / dialog handling path.
  - 左侧任务追踪 panel 找不到“五环”属于 `SYNC_TASK_PANEL` / tracker reader path.
- The latest hard-fail message for “五环左侧任务追踪找不到” only applies after the task is already expected to be visible in the left tracker. It is not the accept-dialog retry limit.
- Reason for keeping this distinction: if a character already accepted a task, falling back from left-tracker-missing into `ACCEPT_TASK` can make it click 云游大师 again and restart the wrong flow. If the accept dialog itself is wrong, fix the accept-dialog handling path instead of hiding it inside tracker retry logic.

Correction:

- 先前一版错误地把五环 tracker prepared action 放进了 `QuestManagerService`。
- 已撤回该方向；五环 V2 不应重新走 Quest Manager。
- 正确归属是左侧“任务追踪 panel”读取能力。

Changed:

- 新增 `TaskTrackerPanelService`
  - 只负责左侧任务追踪 panel 的截图、黄字找“五环”、裁五环任务块、绿字分段、选择括号/进度前的可点击目标、生成 validation fingerprint。
  - 不发送任何真实输入。
- `WindowTaskRunner`
  - 注入 `TaskTrackerPanelService`。
  - watcher 在 `WUHuan_V2` 且没有 route dialog request 时，后台准备 `TASK_TRACKER_PATHING`。
  - 不覆盖 route dialog prepared action。
- `FiveRingTaskV2`
  - `SYNC_TASK_PANEL`/tracker 点击前先消费 watcher 准备好的 panel green action。
  - 有新鲜 prepared action 时直接点击；没有或过期才走原来的现场扫描兜底。
  - 2026-06-07 follow-up: 左侧 panel 绿色链接点击提交后，不再在前台做 edge-pixel 移动确认；直接记录 movement intent 并进入 `WAIT_PATHING`，让 watcher/等待阶段负责确认移动、dialog 或重试，和 `DebugNavigationStressTask` 的快速让权模型对齐。
  - 2026-06-07 follow-up: `ACCEPT_TASK` 改为 turn 外执行，沿用原 `NavigationService` 算法和 `InputSequences` 物理输入队列，但不再把地图确认/小地图点击/移动确认这段包在 coarse task turn 里；新增 `acceptOutsideTurnStart/End` latency 日志。
  - 2026-06-07 follow-up: `SYNC_TASK_PANEL` 改为 turn 外执行。左侧 tracker 截图、OCR/洗图、prepared action 验证不再占用 coarse task turn；真实鼠标点击仍通过 `InputSequences` 串行化。新增 `trackerSyncOutsideTurnStart/End` latency 日志。
  - 2026-06-07 follow-up: `HANDLE_DIALOG` 也改为 turn 外执行。五环给鞋/故事框/不可处理 dialog 的识别不再占用 coarse task turn；真正点击和给物品流程仍通过 `InputSequences`/exclusive input worker 串行化，避免后台 OCR/handle 阻塞其他窗口接权。
  - 2026-06-07 follow-up: 修复 turn 外 phase 继承旧 task turn 的释放漏洞。`PREPARE/BUY_SHOES` 仍保持原来的 transaction 行为；但 `ACCEPT_TASK` / `SYNC_TASK_PANEL` / `HANDLE_DIALOG` / `WAIT_PATHING` 如果在 turn 外返回 `PATHING_STARTED`、`SHARED_STATE_TRIGGERED`、失败或停止，会显式 `forceReleaseTurn(...:outside-yield)`，避免日志已经显示移动开始但其他窗口仍卡在旧 turn 上。
- `DialogOperation`
  - 增加 `TASK_TRACKER_PATHING`，用于区分左侧任务追踪 panel 点击，不混入 route dialog。

Validation:

- `mvn -q -DskipTests compile` passed.
- 2026-06-07 follow-up compile passed after moving `HANDLE_DIALOG` outside the coarse task turn.
- 2026-06-07 follow-up compile passed after adding outside-yield turn release.

Follow-up rule implemented:

- 五环 V2 启动/热启动/中途同步不再打开 Auto+Q / QuestManager 右侧任务面板确认五环。
- `detectHandover(...)` 只看左侧任务追踪 panel；左侧没有五环就进入接任务流程。
- `SYNC_TASK_PANEL` 仍以左侧 panel 为准；只有本轮已经确认接过任务、并且左侧没有五环时，才允许调用 legacy QuestManager 做最终完成确认。
- 当前 V2 中 `QuestManagerService` 只保留在 `isWuhuanAbsentByFinalLegacyTaskPanel(...)` 这条收尾确认路径，不要再把它接回启动或中途路径。
- 左侧 panel 连续找不到五环达到上限时，不再只返回普通 `FAILED`；现在抛 `TaskFatalException`，message 会进入 `WindowTaskRunner` 的任务结束信息，后续 UI 可直接展示这个原因。

Next live check:

```powershell
rg --color never "task tracker panel prepared|prepared 五环 green link|click prepared panel green link|prepared panel action stale|wuhuan-v2:SYNC_TASK_PANEL" logs/dhxy-console.log
```

Expected:

- 看到 watcher 的 `task tracker panel prepared`。
- 五环前台优先出现 `click prepared panel green link`。
- `SYNC_TASK_PANEL` 不应再出现 `task turn acquired/release`，应出现 `trackerSyncOutsideTurnStart/End`。
- `HANDLE_DIALOG` 不应再出现 `task turn acquired/release`，应出现 `handleDialogOutsideTurnStart/End`。
- 移动开始后应看到 `task.turn.release ... outside-yield`；如果只在停止任务时看到 `execute-finished` 强制释放，说明仍有 phase 绕过了让权桥。
- 如果仍在前台现场算，说明 watcher 没准备到或 prepared action 过期，需要看 panel reader 耗时和 watcher 是否被其他窗口/OCR拖住。

### 谢帅 - 2026-06-06 五环 V2 按 debug handoff 模型释放 task turn

Status: implemented / compile passed / waiting live rerun

Problem:

- 用户实测五环 V2 五窗口时仍有窗口长时间霸占 task turn，表现为：
  - `WAIT_PATHING` 还在拿 task turn 等 watcher/战斗/对话框；
  - `SYNC_TASK_PANEL` 找不到左侧五环块或点击未确认时，会继续同窗口重试；
  - 给鞋失败会被映射成硬失败，并且同窗口继续同步 tracker；
  - tracker 绿色链接点击后还会在前台做一次 dialog inspect，增加持有 turn 的时间。
- 这些行为和已经验证过的 `DEBUG_NAVIGATION_STRESS` 模型不一致；debug 模型里，移动提交后等待 watcher 的阶段不应占用粗粒度 task turn。

Changed:

- `FiveRingTaskV2.runPhases(...)`
  - `WAIT_PATHING` 改为 `runWaitPathingWithoutTaskTurn(...)`，不再通过 `taskTransactionRunner.run(...)` 抢 task turn。
  - 新增日志：
    - `[five-ring-v2 latency] pathWaitOutsideTurnStart`
    - `[five-ring-v2 latency] pathWaitOutsideTurnEnd`
- `FiveRingTaskV2.waitPathing(...)`
  - 外置等待路径只调用 `autoCombatService.handleWindowCombatGuardTick(...)`。
  - 不在 task turn 外消费战斗退出、不做战后恢复、不发送完整 auto-combat 维护输入；真正处理留给后续 `CHECK_COMBAT`。
- `FiveRingTaskV2.handleDialog(...)`
  - `GIVE_ITEM_FAILED` / `FAILED` 映射为 `RETRYABLE_ERROR`。
  - 给鞋失败后的五环状态返回 `sharedState(...)`，先释放 turn，再回到 `SYNC_TASK_PANEL` 重试。
  - 连续给鞋失败达到 6 次后，当前五环轮次进入 failed，避免无限 shared retry。
- `FiveRingTaskV2.syncTaskPanel(...)`
  - tracker 找不到五环块时返回 `sharedState(...)`，不再同窗口持有 turn 连续重扫。
  - tracker 连续找不到达到 9 次后，当前五环轮次进入 failed；达到普通 UI 阈值时只清 tracker 缓存，不重置失败计数。
- `FiveRingTaskV2.clickWuhuanTrackerGreen(...)`
  - 保留短 edge-pixel 移动确认。
  - 移除 post-click `DialogService.handleDialog(inspect...)` 前台兜底，避免绿色链接点击后仍拿着 turn 做 dialog/OCR。
  - 如果物理点击已提交但 edge-pixel 没确认移动，仍进入 `WAIT_PATHING` 并释放 turn；后续由外置等待检查 dialog/移动/重试，避免丢掉“点击后直接弹 dialog”的成功路径。

Validation:

- `mvn -q -DskipTests compile` passed.
- Hook/Kant 只读 CR 后指出两个阻塞风险，已处理：
  - 点击绿字后直接弹 dialog 会被误判为 click failed。
  - `sharedState` 重试缺少硬出口。

Next verification:

- 重跑五环 V2 五窗口，重点 grep：

```powershell
rg --color never "pathWaitOutsideTurn|wuhuan-v2:WAIT_PATHING|wuhuan-v2:SYNC_TASK_PANEL|give-item-failed|tracker-retry-later|green click did not confirm|task.turn.release|task.turn.handoff" logs/dhxy-console.log
```

Expected:

- 不应再看到新的 `wuhuan-v2:WAIT_PATHING` 走 `task turn acquired`。
- 应看到 `pathWaitOutsideTurnStart/End`。
- `SYNC_TASK_PANEL` 的慢 hold 应明显减少；如果仍慢，下一步继续拆 tracker capture/OCR/link-click 三段耗时。
- 给鞋失败和 tracker not found 应释放 turn，不应让同一个窗口连续霸占。
- 如果持续识别失败，应在达到上限后进入 failed，而不是无限循环。

Risk:

- `WAIT_PATHING` 在 task turn 外只允许观察；当前已改为 `handleWindowCombatGuardTick(...)`，不要在这条路径里加入带真实输入的恢复/维护动作。

### 谢帅 - 2026-06-06 watcher stale ACTIVE 误判重导修复方案

Status: implemented / compile passed / waiting live rerun

Problem:

- 五窗口 `DEBUG_NAVIGATION_STRESS` 中，`岁月醉白头` 在 `#3 大唐边境(137,121)` 重复输入 `大唐边境`。
- 关键链路：
  - `09:30:25` 当前地图坐标点击 `大唐边境(137,121)`，`coordinateIntent=true`。
  - 游戏自动寻路实际绕路：`大唐边境 -> 北俱芦洲 -> 洛阳城 -> 四圣庄 -> 大唐边境`。
  - `09:30:28` watcher 新扫到 `北俱芦洲(46,30)`。
  - `09:30:33` stress task 用 `now - locationChangedAtMs = 5113ms` 判定停住，清 pathing signal 并重新 world-map 导航。
  - `09:30:34` watcher 才扫到 `洛阳城(152,46)`；`09:30:47` watcher 到达 `大唐边境(135,121)`，证明原始自动寻路并没有失败。

Root cause:

- `WindowTaskRunner` 的 pathing watcher 是同步执行 `MiniMapCoordinateReader.readCurrentTemplateLocation()`，识别完成后才更新 snapshot，然后再 sleep。
- `WINDOW_PATHING_PROBE_ACTIVE_INTERVAL_MS=1000ms` 只表示“一轮识别结束后最多睡 1 秒”，不是“每秒一定产出新坐标”。
- 五窗口下单轮截图/地图模板/坐标 OCR 可能耗时 2-9 秒，所以 `snapshot` 几秒未变不等于角色停住。
- `DebugNavigationStressTask` 把 `locationChangedAtMs` 当成实时运动证据使用，用 task 自己的 wall clock 熬出 5 秒停滞，这是语义错误。

Hook/Jason consensus:

- 不改绿色链接点击算法。
- 不改 `GameStateUtil.isMovingByPixelDiff()`。
- 不靠单纯把 `PATHING_STATIONARY_RETRY_MS` 调大解决。
- watcher 应明确暴露 probe 生命周期/耗时，task 侧只用“fresh watcher 完成观察”判断是否真的停住。
- `coordinateIntent=true` 时，当前地图坐标寻路也可能临时跨图绕行；非目标地图上的 ACTIVE 不能直接按 5 秒停滞重开世界地图。

Implementation plan:

1. `WindowPathingSnapshot`
   - 增加 watcher probe 字段：
     - `probeStartedAtMs`
     - `probeFinishedAtMs`
     - `probeInProgress`
     - 可选 `probeElapsedMs`，也可以由 `finished-started` 计算。
   - 不改变 `updatedAtMs` 语义：它仍只表示“上一轮完成并写入可消费观察”的时间。

2. `WindowTaskRunner.refreshPathingSignal(...)`
   - 识别开始时写入 probe heartbeat：
     - 保留旧 `state/currentMap/currentX/currentY/locationChangedAtMs/updatedAtMs`。
     - 只更新 `probeStartedAtMs` 和 `probeInProgress=true`。
   - 识别完成后：
     - `updatePathingFromLocation(...)` / `updateUnknownPathing(...)` 写 `probeInProgress=false`、`probeFinishedAtMs=now`。
   - 慢 probe 打 info 日志，建议阈值 `>=1500ms` 或 `>=2500ms`。
   - 日志字段至少包括：`probeMs`、`snapshotAgeBeforeMs`、`observedStationaryMs`、`wallStationaryMs`、`target`、`current`。

3. `WindowTaskRunner.classifyPathingState(...)`
   - `coordinateIntent=true` 且 `currentMapName != targetMapName` 时，不使用 2.2 秒坐标 stopped-away 阈值。
   - 使用 map-route 阈值或单独常量，例如 `COORDINATE_ROUTE_AWAY_STOPPED_MS=10000~15000`。
   - 目的：中间地图短暂停留不能被 watcher 自己过早标成 `STOPPED_AWAY`。

4. `DebugNavigationStressTask.waitForPathing(...)` ACTIVE 分支
   - 拆开两个概念：
     - `wallStationaryMs = now - locationChangedAtMs`，只做日志。
     - `observedStationaryMs = snapshot.updatedAtMs - locationChangedAtMs`，只用 watcher 已完成观察来判断是否真停住。
   - `coordinateIntent=true && currentMap != targetMap` 时：
     - 视为 `coordinate-leg map-transit`，继续等待 watcher terminal state。
     - 在 grace 内不允许 world-map retry。
     - 建议常量：`COORDINATE_LEG_CROSS_MAP_GRACE_MS=30000`。
   - 真正允许重入导航的条件应同时满足：
     - snapshot fresh；
     - `!probeInProgress`；
     - `observedStationaryMs >= PATHING_STATIONARY_RETRY_MS`；
     - 当前坐标不在目标附近；
     - 当前不是 coordinate intent 的跨图中间态；
     - 最后再用轻量 pixel diff 做防误判确认。

5. `DebugNavigationStressTask.waitForPathing(...)` UNKNOWN 分支
   - 同步应用 probe/fresh 规则。
   - probe 正在跑或 snapshot stale 时，不直接 retry。
   - 保留现有 edge pixel diff，但它只能作为补充证据，不能在 watcher 正在慢扫时强行重导。

6. Recovery cooldown
   - 可在 `NavigationStressState` 中记录：
     - `lastPathingRecoveryAtMs`
     - `pathingRecoveryRetryCount`
   - 避免同一个 target 每 5 秒重复 world-map 输入。
   - 真正重入时日志改成更明确的：
     - `re-enter navigation after confirmed stalled fresh snapshot`
   - 不再使用容易误导的 `observer active but position stalled` 作为最终判定日志。

Validation plan:

- 跑五窗口 navigation stress。
- 重点 grep：

```powershell
rg --color never "hwnd-311168|target=#3 大唐边境|coordinate-leg|probeMs|probeElapsedMs|re-enter navigation after confirmed stalled|observer active but position stalled" logs/dhxy-console.log
```

Expected:

- `北俱芦洲(...) coordinateIntent=true` 后，不再出现旧的 `observer active but position stalled; re-enter world-map navigation`。
- 应出现类似 `coordinate-leg active on off-target map; keep waiting for map transit`。
- watcher 最终 `ARRIVED` 或 active-near-target 后，task 消费完成当前 target。
- 慢 watcher 日志能解释每轮识别耗时，而不是只能看到几秒没有更新。

Risk:

- 如果当前地图点击真的失败，等待可能比现在长；但有全局 timeout 和 confirmed stalled recovery，比中途重复打开世界地图更安全。
- probe heartbeat 不能刷新 `updatedAtMs`，否则会污染所有 recent snapshot 判断。
- 日志要节流：state change、terminal、slow probe、confirmed retry 用 info，其余 debug。

Next concrete step:

- 已实现 `WindowPathingSnapshot` + `WindowTaskRunner` probe 字段和慢 probe 日志。
- 已改 `DebugNavigationStressTask` ACTIVE/UNKNOWN retry 条件。
- Hook/Jason CR 后补了三处问题：
  - 慢 probe 结束前校验当前 active intent，不允许旧 probe 结果覆盖已清除/新注册的 pathing intent。
  - UNKNOWN probe miss 不再刷新 `updatedAtMs`，避免“旧坐标 + 新更新时间”伪造静止证明。
  - coordinateIntent 非目标地图的 stopped-away 阈值和 stress task 的 30 秒 grace 对齐，并且 STOPPED_AWAY 分支也尊重 grace。
- `mvn -q -DskipTests compile` passed.
- 下一步让用户重跑五窗口压测，重点看是否出现：
  - `coordinate-leg active on off-target map; keep waiting for map transit`
  - `discard stale pathing probe result`
  - `pathing watcher slow probe`
  - 不应再出现旧的 `observer active but position stalled; re-enter world-map navigation`

### 谢帅 - 2026-06-06 route dialog prepared 后及时抢回导航 turn

Status: implemented / compile passed

Observed:

- 08:19 附近日志显示 `hwnd-531070A` 后台已经算出 `长安桥` 的 prepared action：
  - `dialog prepared: ... target=长安 matched=长安桥（400两） click=(1156, 811)`
- 但该窗口任务线程仍处于 `wait:1-长安` 的 pathing 等待里，继续等 watcher 的 `ARRIVED` / `STOPPED_AWAY`。
- 紧接着 input worker 去服务其它窗口的 `submitWorldMapSearchAndClickDestination:长安城东`，所以用户看到像是“刚切回来准备点 dialog 又被别的窗口抢走”。
- 根因不是坐标点错，而是 debug navigation stress 的等待状态没有把 `PreparedDialogAction` 当成一个可以立即恢复导航 turn 的终态信号。

Changed:

- `DebugNavigationStressTask.waitForPathing(...)`
  - 在 pathing 等待开始处检查当前窗口 runtime 里是否已有匹配当前目标地图的 `PreparedDialogAction`。
  - 如果匹配 `DialogOperation.ROUTE_TRANSFER + target.mapName`，立即结束 pathing wait，返回 `READY_TO_CONTINUE`。
  - 下一轮会回到 `NavigationService`，优先消费已准备好的 route dialog 点击，避免继续等待 watcher 导致其它窗口插入一整段世界地图搜索。

Validation:

- `mvn -q -DskipTests compile` passed.

Next verification:

- 下一轮如果后台先算出 route dialog，应看到：
  - `prepared route dialog interrupts pathing wait; re-enter navigation`
  - 随后 `route dialog probe uses prepared action`
- 如果仍出现“prepared 了但没点”，继续查 `PreparedDialogAction.matches(...)` 是否因为目标名不一致没有命中。

### 谢帅 - 2026-06-06 导航压测结果与 route dialog 预计算 Alt+4 降噪

Status: implemented / compile passed

Observed:

- 最新一轮导航压测 5 个窗口全部完成：`导航压力测试 -> SUCCESS` 共 5 个。
- 没有新的 `ERROR` / `Exception` / NPE；上一轮 `WindowPathingSnapshot.currentX/currentY == null` 的崩溃已消失。
- 点寻路后早关世界地图已生效：`close world map immediately after xunlu click` 出现 28 次。
- route dialog 后台预计算开始发挥作用：`route dialog probe uses prepared action` 出现 13 次，`prepared action not usable` 为 0。
- 仍发现一个输入队列噪音：`dialog:hidePlayerNames:window-dialog-preparation...` 出现约 390 次。后台 route dialog watcher 不应该为了预判 dialog 去按 `Alt+4`，这会占用全局输入队列并拖慢多窗口节奏。

Changed:

- `DialogService.detectDialogTypeNoFocus(String reason, boolean hidePlayerNames)`
  - 增加一个可控版本，允许调用方明确不发送 `Alt+4`。
  - 普通业务 dialog 检测仍保留原来的 `detectDialogTypeNoFocus(String reason)`，默认会隐藏玩家名。
- `WindowTaskRunner.refreshDialogPreparationSignal(...)`
  - route dialog 后台预计算的预判断改为 `hidePlayerNames=false`。
  - 这条 watcher 路径只做后台截图判断，不再抢输入队列；真正点击仍交给任务拿到 turn 后执行。

Validation:

- `mvn -q -DskipTests compile` passed.

Next verification:

- 下一轮重点看 `dialog:hidePlayerNames:window-dialog-preparation` 是否从几百次降到 0 或接近 0。
- 继续观察 `route dialog probe uses prepared action` 是否仍能命中；如果命中下降，说明不按 `Alt+4` 后需要单独优化 route dialog 的截图/洗图，而不是恢复全局 Alt+4。
- 继续看 `stage=world-map-submit` 的大耗时；现在日志里的大值多半包含等待输入队列，后续如果还慢，需要再拆“排队等待”和“实际地图 OCR/点击”两段。

### 谢帅 - 2026-06-06 导航压测小地图 handoff 确认与 map-leg 等待日志优化

Status: implemented / compile passed

Changed:

- `NavigationService.clickMiniMapPointForHandoff(...)`
  - 小地图 handoff 点击后，先用 `GameStateUtil.confirmPathingStartedByEdgePixelDiff(...)` 做快速移动确认。
  - 如果边缘像素没有确认移动，再回到原来的 `confirmMiniMapPathingStarted(...)` 小地图坐标确认。
  - 这样保留失败重试语义：快速确认成功时更快放权；快速确认不成功时仍用坐标确认判断是否需要交给 watcher / 后续 retry。
- `DebugNavigationStressTask.waitForPathing(...)`
  - `map-leg ACTIVE + stationary` 仍然不重开世界地图，继续等 watcher 的 `ARRIVED` / `STOPPED_AWAY` / 全局 timeout。
  - 该等待日志增加 5 秒节流，并打印 `timeoutMs`，避免大雁塔二层中间地图停顿时刷屏，同时保留可诊断性。
- `WindowTaskRunner.refreshDialogPreparationSignal(...)`
  - route dialog 后台 preparation 先调用轻量 `DialogService.detectDialogTypeNoFocus(...)`。
  - 只有当前画面明确是 `DialogType.OPTION` 时，才进入完整 route option OCR / remembered click preparation。
  - `NONE` / `STORY` 不再标记 prepare failed，也不跑完整 OCR；request 保留给 watcher 下一轮继续看，避免无 dialog 背景反复重 OCR。
- `NavigationService.navigateToMap(...)`
  - 增加 `[productionNavigate-latency]` 分层耗时日志：
    - `stage=map-confirm`
    - `stage=route-dialog-precheck`
    - `stage=world-map-submit`
    - `stage=loop-position-sync`
  - 下一轮可以直接区分慢在地图确认、route dialog 预处理、世界地图 OCR/点击，还是循环里的坐标同步。
- `NavigationService.submitWorldMapSearchAndClickDestination(...)`
  - 正常点击到 `寻路` 按钮后，立刻按一次 `Alt+2` 关闭底层世界地图，再继续在 route panel 输入目标。
  - 原因：点中 `寻路` 能证明世界地图此刻一定打开；如果等路线链接点击后再关，游戏可能已经自动关闭世界地图，晚到的 `Alt+2` 反而会把世界地图重新打开。
- `DebugNavigationStressTask.waitForPathing(...)`
  - 补齐 watcher snapshot 空坐标保护。刚注册 pathing intent 时可能是 `ACTIVE current=null(null,null)`，此时不能直接拿 `currentX/currentY` 算 near target。

Validation:

- `mvn -q -DskipTests compile` passed.

Next verification:

- 重跑导航压测，重点看：
  - 小地图当前地图点击后是否出现 `mini-map handoff pathing confirmed by fast edge pixels`，如果出现，说明本轮没有走重坐标确认。
  - 如果 fast edge 没确认，是否出现 `mini-map handoff coordinate fallback completed`，并关注 `fallbackElapsedMs`。
  - 大雁塔二层途中可以出现 `map-leg active position stalled but still waiting for observer terminal state`，但应该最多约 5 秒一条，不应恢复成 5 秒重开世界地图。
  - `dialog preparation probe miss` 数量应该明显下降；正常无 dialog 时应更多看到 debug 级 `dialog preparation probe skipped ... visibleType=NONE`。
  - 若还有 20 秒以上导航，按 `[productionNavigate-latency]` 的 stage 定位具体慢段。
  - 正常世界地图搜索应在点击寻路后出现 `close world map immediately after xunlu click`，后面不应因为晚到 `Alt+2` 把世界地图重新打开。

### 唐德 - 2026-06-06 导航压测避免 near target 反复点与 map-leg 重开世界地图

Status: implemented / compile passed

Observed:

- 最新一轮日志开头已滚到 `23:49:42` 的 `#4 龙宫`，所以 `#3 长安城东(166,118)` 的最早完整过程不在当前 `dhxy-console.log` 里。
- 当前日志仍能看到同类风险：
  - watcher 在 coordinate leg 下可能已经读到接近目标的坐标，但任务层只等 `ARRIVED` 或停滞分支，容易多提交一次小地图点击。
  - `#5 大雁塔二层(76,73)` 的 map leg 中，窗口仍处于 `大雁塔一层` / `长安城东` 等中间状态时，旧逻辑把 `ACTIVE + stationaryMs >= 5000` 当作需要 `READY_TO_CONTINUE`，从而可能重新打开世界地图再搜目标。

Changed:

- `DebugNavigationStressTask.waitForPathing(...)`
  - 对同一 watcher intent，如果 `coordinateIntent=true` 且当前 watcher 坐标已经 near target，立即消费并完成目标，不再等 `ARRIVED` 状态或停滞分支。
  - 对 `map-leg` 的 `ACTIVE + stationary`，不再清 pathing signal / 不再重进世界地图；继续等待 watcher 给出 `ARRIVED` / `STOPPED_AWAY` / 全局超时。

Reason:

- 当前地图坐标点击是否需要重试，应由 coordinate intent 的 near target / ARRIVED 来决定。
- 跨地图 map leg 中间可能会经过大雁塔一层、长安城东等地图，短时间坐标不变不等于 route 失败；重开世界地图反而会打断正在进行的路线。

Next verification:

- 再跑导航压测，重点看：
  - 到 `长安城东(166,118)` 附近后，如果 watcher 坐标已在 tolerance 内，应直接 `target reached by active watcher coordinate`，不再二次小地图点击。
  - 到 `大雁塔二层` 途中，如果仍在 `大雁塔一层` 移动/等待，不应再出现 `observer active but position stalled; re-enter world-map navigation`。

### 唐德 - 2026-06-06 小地图第一次点击跳过面板匹配

Status: implemented / compile passed

Decision:

- 用户确认当前地图小地图点击不要每次都先匹配面板。
- 第一次尝试直接假设 Alt+1 小地图是关闭的：按一次 `Alt+1`，等待短暂 settle，然后直接点击目标坐标。
- 只有第二次及后续重试才做 `isMiniMapPanelVisible()` 正向检测：
  - 命中时说明小地图面板已经打开，跳过 `Alt+1`，避免把已打开的面板关掉。
  - 未命中时不当成硬失败，仍按一次 `Alt+1` 后点击，因为模板 miss 不能证明面板一定没开。

Changed:

- `NavigationService.submitMiniMapClick(...)` 新增 `checkPanelBeforeOpen` 参数。
- `navigateInCurrentMap(...)` 用 `failedMiniMapClicks > 0` 决定是否启用面板检测。
- `clickMiniMapPointForHandoff(...)` / `clickMiniMapPointAndConfirm(...)` 透传该策略。
- `closeAfterClick=false` 的当前地图点击不再为了关闭面板而额外匹配一次，保留“未知/可能打开”状态；如果需要重试，由重试路径做正向检测。

Next verification:

- 重跑 `岁月醉白头` 到 `长安城东(166,118)`。
- 第一次当前地图点击日志应直接出现 `mini-map Alt+1 open assumed ... checkPanelBeforeOpen=false`，不应先出现 `mini-map panel visible before coordinate click`。
- 如果第一次 `NO_PATHING`，第二次重试才允许出现 panel visible/skip Alt+1 相关日志。

### 谢帅 - 2026-06-06 导航压测输入取消与 route dialog 交接修复

Status: second review fixes implemented / compile passed / waiting live rerun

Why this entry exists:

- 多窗口导航压测中，旧窗口有时在已经失去任务等待方以后还继续执行 direct input，表现为某个窗口刚 focus 准备重试，马上又被旧输入动作抢走。
- CR 指出 `InputActionQueue.await()` 取消等待方以后，只能取消还没被 worker 取走的请求；如果 exclusive callback 已经进入 worker，里面的 direct input 仍可能继续跑。
- 另一类问题是 route dialog 已经弹出或后台准备中时，任务层会过早重新打开世界地图，导致窗口互相抢和重复导航。

Changed files:

- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionScope.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`

Done:

- `InputActionQueue.await()` 被打断或等待失败时，会取消 request 并尝试从队列移除，日志增加 `removedFromQueue`。
- `InputActionWorker` 取到已取消 request 时不再 focus；执行 request 时通过 `InputActionScope` 暴露当前 request 给 exclusive callback。
- `NavigationService.submitWorldMapSearchAndClickDestination(...)` 在 direct input 的关键步骤之间检查 `InputActionScope.isCancelled()`。
- 如果 route 搜索 exclusive callback 已取消，失败收尾不会再调用 `closeMapSearchInputAfterRouteClick(...)` 做额外 direct input，避免旧导航抢新窗口。
- 世界地图搜索结果滚动 helper 也补了取消检查，取消后不会继续滚完本轮滚轮。
- route dialog prepared action 和 remembered route option 的前台点击改为 `moveAndClickLeft(...)`，保持 move+click 原子序列。
- map-route 类型的 `STOPPED_AWAY` 阈值和坐标导航阈值分开：坐标仍为 2.2 秒，map route 使用 8 秒，避免路线弹窗/跨图阶段过早判停。
- watcher 在 pathing active 且 dialog preparation active 时也会刷新 dialog preparation，避免 route dialog 已经弹出但任务层一直等 pathing 停止。
- 未改 `长安 -> 大雁塔` 这类路线别名，也未改世界地图绿色链接选择算法。

Validation:

- `mvn -q -DskipTests compile` passed.

Second CR findings and follow-up fixes:

- Jason/Ferade 二次 review 结论：上轮改动已经缓解旧输入抢窗口和 prepared route 消费，但还不能说彻底解决。
- 已补 P0：`clickDestinationFromWorldMapSearchResults(...)` 在 destination OCR 后、coordinate OCR 后、direct route click 前都会检查 `InputActionScope.isCancelled()`，避免旧 exclusive callback 在长 OCR 结束后继续点击。
- 已补 P1：prepared route action 正常点击成功后同时清理 `DialogPreparationRequest` 和 `PreparedDialogAction`，和 late prepared 分支保持一致，避免 stale prepared action 影响下一轮。
- 已补 P1：`navigateToMap` 在正式重新打开世界地图前，如果最近 pathing snapshot 是同目标的 `STOPPED_AWAY`，会尝试一次 `visible-route-dialog-rescue`。这个 rescue 不创建新的后台 preparation request，避免重新引入无 dialog 空算。
- 已补 P1：`WindowTaskRunner` pathing watcher 首帧 `previous == null` 时不再空指针；首次 mini-map miss 会生成 UNKNOWN snapshot，而不是让 watcher 线程异常退出。
- 已补 P2：`DialogService` 在 input worker 内消费 remembered/prepared route click 前也检查 `InputActionScope.isCancelled()`。

Next verification:

- 重新跑 3 到 5 窗口导航压测，重点看：
  - 被 stop/重试取消的旧 `submitWorldMapSearchAndClickDestination` 是否还会在之后抢窗口；
  - 日志是否出现 `navigation map search cleanup skipped because input request was cancelled`；
  - 如果 OCR 期间发生取消，是否出现 `navigation map search cancelled after destination OCR` 或 `navigation map search cancelled after coordinate OCR`；
  - 若 route dialog 已经弹出但 request 丢失/过期，是否出现 `try visible route dialog rescue before world-map search`，并且不再直接重新 Alt+2；
  - route dialog 已弹出时是否优先消费 prepared/memory action，而不是重新打开世界地图；
  - 多窗口中是否还出现某窗口刚 focus 就立刻被另一个旧导航动作抢走。
- 如果仍出现抢窗口，下一步检查 `WindowAwareInputCoordinator` 的 focus transaction 是否需要在 focus 前后感知 request cancellation。

### 唐德 - 2026-06-05 岁月醉白头长安城东小地图点击状态修正

Status: implemented / compile passed

Observed:

- 用户确认本轮只看 `岁月醉白头`，问题不是“它输入导航”，而是它已经在 `长安城东` 后没有正常点击当前地图小地图目的地，最后视觉上又看到路线面板绿色链接点击。
- 日志对应窗口为 `hwnd-14210A0` / hwnd `21106848`。
- 关键链路：
  - `22:28:24` watcher 已确认 map leg 到达：`current=长安城东(27,231)`。
  - 随后任务进入 `navigateInCurrentMap`，目标是 `长安城东(166,118)`。
  - `22:28:25`、`22:29:25`、`22:29:45` 多次点击 `pixel=(1851/1852,550/551)` 后坐标仍为 `(27,231)`，返回 `NO_PATHING`。
  - 这些重试前出现 `mini-map already open before coordinate click`，说明代码复用了 `miniMapOpenByNavigation=true`，没有重新 `Alt+1` 打开/确认小地图。

Root cause:

- `NavigationService.submitMiniMapClick(...)` 曾经相信本地 `miniMapOpenByNavigation` 标志。
- 但跨地图、传送、游戏 UI 切换会自动关闭 Alt+1 小地图；这个动作不会回写 Java 内存状态。
- 一旦 stale 为 true，当前地图坐标点击会跳过 `Alt+1`，直接在错误 UI 层/旧面板层点坐标，表现就是“看起来没有点小地图目的地”。
- Alt+1 panel 的 checkbox 模板也不能作为硬条件；模板 miss 不能直接让导航失败，也不能因为 miss 就盲按第二次 Alt+1，否则可能把真实已打开的 panel 关掉。

Changed:

- `NavigationService.submitMiniMapClick(...)`
  - 移除 `miniMapOpenByNavigation` 决策，不再用内存判断 Alt+1 小地图是否打开。
  - 当前地图坐标点击采用“正向检测可信，反向检测不可信”：
    - 如果 `isMiniMapPanelVisible()` 命中 checkbox 区域，说明 Alt+1 panel 已开，直接点击坐标，不再按 Alt+1。
    - 如果 `isMiniMapPanelVisible()` miss，不证明 panel 关闭；但默认按“关闭”处理，先按一次 Alt+1，再继续尝试坐标点击。
    - 按 Alt+1 后如果 panel 仍然 miss，只记录 warning，不把模板 miss 当成硬失败。
  - 点击后只有在 `isMiniMapPanelVisible()` 正向命中时才请求关闭 Alt+1 小地图；如果 panel miss，不盲按 Alt+1，避免反向打开它。
  - 不允许用 `Alt+2` 世界地图标题 `world_map_title.png` 来校验 Alt+1 小地图状态；世界地图路线面板和当前地图小地图不是同一个 UI 语义。
  - 已拆分职责：
    - `isWorldMapTitleVisible()` 只判断 Alt+2 世界地图/路线面板标题。
    - `isMiniMapPanelVisible()` 只判断 Alt+1 小地图/地图设置面板 checkbox 区域。

Verify:

- `mvn -q -DskipTests compile` passed.

Next verification:

- 重新只跑/只观察 `岁月醉白头`。
- 到 `长安城东` 后如果需要点击 `(166,118)`，应看到：
  - 如果 panel 正向命中：`mini-map panel visible before coordinate click; skip Alt+1 open`；
  - 如果 panel miss：`mini-map Alt+1 sent ... :open`，之后即使仍 miss 也继续点击；
  - 不应再用 `world_map_title.png` 判断小地图开关。
- 如果仍点击 `185x,55x` 不移动，下一步要看该窗口的 `CoordinateHelper.resolveMiniMapClickPoint(...)` 对 `长安城东(166,118)` 映射是否错误，而不是再改 route dialog。

### 唐德 - 2026-06-05 导航压测 ACTIVE 无坐标不再提前重进

Status: implemented / compile passed

Why this entry exists:

- 最新日志里 watcher 后来确实报了坐标 leg 的 `ARRIVED`，但 `DebugNavigationStressTask` 已经在 5 秒 `ACTIVE + current=null` 时清掉 pathing signal 并退出等待。
- 这不是“等 5 秒不够”的问题；本质是把“observer 还没产出坐标”误当成“导航失败”。
- 如果只把 5 秒放宽成 15 秒，下一次 capture/窗口负载导致 20 秒才产出坐标时仍然会复现。

Changed files:

- `src/main/java/com/bot/dhxy/task/DebugNavigationStressTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- `waitForPathing(...)` 的 `WindowPathingState.ACTIVE` 分支不再因为 `current=null(null,null)` 超过 5 秒就 `clearPathingSignal()` / `finishWaitingForPathing()`。
- `ACTIVE` 但没有坐标现在只表示 watcher 还没有可用 mini-map sample，任务继续返回 `PATHING_STARTED` 等待。
- 仍保留“有具体坐标且停滞 5 秒”的重进逻辑，因为那个才是可以用于判断 stopped/stalled 的证据。

Next verification:

- 重新跑导航压力测试，重点看之前的 `observer active without position; re-enter navigation` 是否消失。
- 如果 watcher 后续报 `ARRIVED`，任务应在 wait loop 里消费并完成当前 target，而不是重新进入完整 `navigateToNPC()`。
- 22:10 后续日志又确认另一个问题：`hwnd-70D66 / うprinoe大叔` 在 `#2 长安城东(166,118)` 时，watcher 报的是 map-only arrival：
  - `target=长安城东(null, null)`
  - `current=长安城东(27, 231)`
  - 但 `navigateNextTarget(...)` 的 pre-navigation 快路径把任何 `ARRIVED` 都当成目标完成，直接进入 `#3 大唐边境`。
- 已修正：pre-navigation 快路径只有在 coordinate intent 到达，或当前坐标确实接近目标坐标时，才 `completeCurrentTarget(...)`。
- map-only `ARRIVED` 现在只会被清掉并继续进入当前地图坐标导航，避免刚进地图边缘就跳下一目标。

### 谢帅 - 2026-06-05 route dialog 空算与漏算修复

Status: second fix implemented / compile passed / waiting live rerun

Why this entry exists:

- 五窗口导航压测里，`岁月醉白头` 对应窗口能拿到 turn，但一直停在 `洛阳城(311,116)` 目标 `长安(216,129)`。
- 日志显示它反复进入 `route dialog preparation reuses active request ... phase=PREPARING`，随后 `DIALOG_PREPARING` 让出窗口。
- 第一轮核心原因不是 watcher 慢，而是 `NavigationService.navigateToMap()` 一开始无条件调用 `clickRouteDialogOption("navigation:existing-route-dialog", ...)`。
- 当时窗口没有移动、也没有任何 dialog，这个“existing-route-dialog probe”仍然创建 `DialogPreparationRequest`，导致 watcher 对空背景做 route dialog 准备并不断 miss。
- 后续 19:17 实测又暴露相反问题：世界地图路线链接已经点下去，游戏必然弹出路线/传送 dialog，但当前代码只注册 pathing intent，没有注册 `DialogPreparationRequest`，所以 watcher 不会检测 dialog，最后只会把窗口判成 `STOPPED_AWAY` 并重新打开世界地图。
- 21:10 实测确认 request 已经能挂上，但 watcher 在处理 dialog preparation 之前先跑 `refreshPathingSignal()` / 小地图截图。多窗口下小地图捕获出现 `captureMs=7256/17277`，导致 route dialog prepare 20 秒后才完成，另一个窗口直接 request expired。

Changed files:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 撤掉 `NavigationService.navigateToMap()` 开局的 `existing-route-dialog` 处理；`navigateToMap` 是通用导航入口，不能每次导航都先扫/处理 dialog。
- 启动/热启动时残留 dialog 应该在任务启动或恢复层处理，不放在每次地图导航开局。
- 导航等待循环里的 speculative route-dialog probe 已撤掉；停下后优先做当前位置确认，不再因为每次 stopped movement 都准备 route dialog。
- 之前加的 `ROUTE_DIALOG_PREPARING_YIELD_MAX_MS=1500` 仍保留为慢 watcher 兜底，但正常无 dialog 场景不应该再创建 PREPARING。
- 新增精确触发点：只有 `submitWorldMapSearchAndClickDestination(...)` 成功点击路线链接后，才给当前窗口登记 `ROUTE_TRANSFER` 的后台 dialog preparation request。
- `navigateToMap()` 重新进入时，如果当前窗口已有同目标的 `REQUESTED/PREPARING/READY` route-dialog 状态或可用 prepared action，会先调用现有 `clickRouteDialogOption(...)` 消费 dialog，再考虑重新打开世界地图。
- `WindowTaskRunner` 现在在存在 dialog preparation request / prepared action 时，先执行 `refreshDialogPreparationSignal(...)`，再考虑 pathing/minimap watcher，避免路线 dialog 被慢截图挡住。
- `ROUTE_DIALOG_PREPARE_REQUEST_TTL_MS` 从 15 秒调整到 45 秒，给五窗口排队和慢捕获留出余量；这不是导航算法变化，只是防止正确 request 在 watcher 轮到前过期。
- 21:17 复测发现上面的优先级过硬：`prepare miss` 会把状态标成 `FAILED`，但 request 仍保留，导致 watcher 每轮都优先尝试 dialog preparation，不再刷新 pathing/minimap。已修正为：只有 `REQUESTED/PREPARING/READY` 或已有 prepared action 时才阻止 pathing；`FAILED` 状态必须继续刷新 pathing，避免全窗口停在旧快照。
- 21:21 复测后仍出现“其他窗口都等着不动”。日志显示它们并不是没跑线程，而是 `DebugNavigationStressTask.waitForPathing()` 一直收到 `WindowPathingState.ACTIVE`，其中有的 `current=null(null,null)`，有的坐标几十秒不变。旧逻辑对 ACTIVE 只等 90 秒超时，不会提前重试。已加 5 秒停滞出口：ACTIVE 但无位置、或位置不变且未到目标时，清理 pathing signal 并返回 `READY_TO_CONTINUE` 重新进入导航。

Validation:

- `mvn -q -DskipTests compile` passed.

Next verification:

- 下一轮重点看 `岁月醉白头` 是否还会在无 dialog 状态下反复出现 `route dialog preparation reuses active request`。
- 普通 `navigateToMap` 不应出现 `navigation:existing-route-dialog` 的 `handleDialog` 或 route preparation request。
- 点完世界地图路线链接后，应出现 `route dialog preparation requested after map route click`。
- 真正出现路线 option dialog 时，应由 watcher 准备，随后 `navigateToMap()` 先消费 prepared action / memory / OCR 原路径，而不是直接重新搜索世界地图。
- 下一轮如果仍卡 dialog，重点看 `dialog preparation probe start` 是否紧跟 `route dialog preparation requested after map route click`，以及 `requestAgeMs` 是否还会超过 5 秒。
- 同时确认 `dialog preparation probe miss` 后仍能看到对应窗口的 `[minimap-location]` / `pathing watcher update`，不能再出现所有窗口只报 `PATHING_STARTED` 但位置状态不更新。
- 下一轮还要看是否出现 `observer active without position` / `observer active but position stalled`，出现后对应窗口应重新进入 `navigate` transaction，而不是继续无限 `wait`。

### 谢帅 - 2026-06-05 后台 Dialog 预计算 memory 快路径

Status: consume/cleanup fix implemented / compile passed / waiting live rerun

Why this entry exists:

- 用户提醒多人协作规则：关键实验结论和下一步改动必须写入 ActiveMD，不能只在聊天里说。
- 2026-06-05 最近一次导航压测显示，后台 Dialog 预计算调度已经能更快启动，但没有真正被消费：
  - 日志出现 `route dialog prepared wait finished ... usable=false`，短等 200ms 后仍没有可用 prepared action。
  - 没有看到 `route dialog probe uses prepared action`。
  - watcher 请求年龄已经降到几十到两百毫秒级，但完整 prepare 仍慢，例如 `detectMs=884 ocrMs=1152 totalMs=2036`、`detectMs=1404 ocrMs=1172 totalMs=2576`。
- 结论：问题不再主要是 watcher 启动慢，而是每次 route dialog prepare 还在跑重 OCR。正常路径经常先用 `transfer-memory` 点完，prepared action 才算出来并被废弃。

Changed files:

- `src/main/java/com/bot/dhxy/model/dialog/DialogPreparationRequest.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/vision/MiniMapCoordinateReader.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `WindowTaskRunner` 的 active dialog prepare 轮询间隔已调到 `200ms`，只在存在 preparation request 或 prepared action 时生效。
- `NavigationService` 在声明 route dialog preparation request 后，短等最多 `200ms`，如果已有可用 prepared action 就直接走缓存点击，否则回到原来的 `transfer-memory / OCR` 路径。
- `DialogPreparationRequest` 现在可以携带 `fromMap`、已记忆的 route 选项相对点击点、以及记忆选项文本。
- `NavigationService` 会先查 `TransferChoiceMemoryService.findUsable(fromMap, targetMapName)`；如果已有可用记忆点，会把这个点放进 preparation request。
- `WindowTaskRunner` 收到带记忆点的 request 时，优先调用 `DialogService.prepareRememberedRouteOption(...)`，只检测当前是否为 option dialog，并在记忆点附近生成 fingerprint，不再先跑整轮 route OCR。
- 如果没有记忆点，仍回退到现有 `DialogService.prepareRouteKeywordOption(...)`。
- `MiniMapCoordinateReader` 额外修了小地图 label 裁剪尾部 `[` 干扰的问题，并保存 low-score debug 图，避免 `长安城东 [` 这类裁剪导致低分误判。

Validation:

- `mvn -q -DskipTests compile` passed.
- 小地图 low-score 离线样本已确认：之前的 9 张 `长安城东 [` 类低分样本，裁掉尾部 bracket 后均可回到 `长安城东 score=1.0`。

Next verification:

- 下一轮 2 窗口导航压测重点看这些日志：
  - `route dialog preparation requested ... memory=true`
  - `dialog prepare remembered route result`
  - `route dialog prepared wait finished ... usable=true`
  - `route dialog probe uses prepared action`
- 2026-06-05 13:40-13:43 实跑验证到 runner 已经接上 memory 快路径，但还没完全达到目标：
  - 已出现 `memory=true` 和 `dialog prepare remembered route result`。
  - 没有出现 `route dialog probe uses prepared action`。
  - 典型耗时：runner `requestAgeMs=1` 启动，但 `prepareRememberedRouteOption` 仍约 `880-1113ms`；`NavigationService` 只短等 `200ms`，所以主线先回到正常 `CLICK_REMEMBERED_OPTION`。
  - route 点击后旧 request 没及时清理，导致后续不断出现 `dialog prepare remembered route miss ... type=NONE`，runner 在无 dialog 状态下白算。
- 已补两个修正：
  - 短等结束后重新取当前时间判断 prepared action 是否可用，避免用发 request 前的旧 `now`。
  - 进入 `CLICK_REMEMBERED_OPTION` 前再检查一次最新 prepared action；如果 runner 在短等后、正式 memory 点击前算好了，就直接点 prepared 坐标。
  - memory 正常点击成功后立即清 `DialogPreparationRequest` 和旧 `PreparedDialogAction`，避免 route 已经提交后 watcher 继续反复探测旧 dialog。
- 下一轮还要额外看：
  - 是否出现 `route dialog memory path uses late prepared action`。
  - route 点击成功后是否不再反复出现同一 target 的 `dialog prepare remembered route miss ... type=NONE`。
- 如果 `memory=true` 仍然没有在 200ms 内变成 usable，下一步不是再加等待，而是继续拆 `detectDialogSnapshotDirect(...)` 的耗时，或者让 watcher 在 pathing intent 阶段更早预热。
- 如果 prepared action 已被消费，再对比 route dialog 占权是否从 2-12 秒下降到接近一次短点击。

### 唐德 - 2026-06-05 导航压力测试暂停超时补偿

Status: completed / compile passed

Why this entry exists:

- 最近一次 2 窗口导航压力测试在用户暂停后恢复，两个窗口立刻失败：
  - `hwnd-180B4E`：`ageMs=140801 timeoutMs=90000`
  - `hwnd-860A3C`：`ageMs=130500 timeoutMs=90000`
- 日志显示暂停约 121 秒被算进了 pathing wait timeout，导致恢复瞬间触发超时。

Changed files:

- `src/main/java/com/bot/dhxy/task/DebugNavigationStressTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- 在 `DebugNavigationStressTask.waitForPathing(...)` 的暂停 checkpoint 后增加计时补偿。
- 如果 checkpoint 因暂停阻塞超过 1 秒，会把 `pathingStartedAt`、`lastYieldAt`、`lastPathingSyncAt` 往后平移对应阻塞时长。
- 新增日志：
  - `[nav-stress] pathing wait timer paused`
- 这样暂停期间不会计入 90 秒寻路等待超时，恢复后继续按真实运行时间判断。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-04 后台 Dialog 预计算设计

Status: phase 1 implemented / compile passed

Why this entry exists:

- 导航压测里已经确认，窗口之间放权/接权本身可以很快，但 `route dialog` 处理会在拿到窗口后再做 OCR/模板匹配，单次可能占用 4-12 秒。
- 目标是让 watcher 在后台先把“当前 Dialog 应该点哪里”算好。等窗口真正拿到输入权时，只做一次短点击，减少窗口切换后的等待。
- 用户明确要求：不要新增 `RouteDialog` 类型，不要用 OCR 做二次验证，不要全屏扫，不要让 watcher 自己点击或推进任务阶段。

Core design:

- Dialog 类型仍然只使用现有 `DialogType.NONE / OPTION / STORY`。
- “路线传送框”不是新类型，而是 `DialogType.OPTION + DialogOperation.ROUTE_TRANSFER` 的一种业务操作。
- 第一次发现 Dialog 时，后台只截固定 Dialog 区域，不截全屏。
- 第一次匹配可以复用现有 `DialogService` 的绿色/黄色洗图、OCR、模板匹配结果；不要为了 fingerprint 再整张重洗一次。
- 命中目标后，从已经洗过的图里裁一个很小的验证区域，生成 fingerprint 字符串，并记录绝对点击点。
- 后续 watcher 验证只截命中目标附近的小区域，按第一次使用的同一套洗色规则生成 fingerprint；只比较 fingerprint 相似度，不再 OCR，也不再模板匹配。
- fingerprint 不能要求完全相等，要允许轻微抗锯齿/闪烁差异，可以用汉明距离或黑白像素差异阈值。
- 缓存不是只靠 1.5-2.5 秒 TTL。只要 watcher 持续验证 fingerprint 没变，prepared action 就可以继续有效；短 TTL 只用于“最后一次验证距离真正点击太久”的兜底。

Prepared action should contain:

- window id / hwnd binding；
- `DialogType`、`DialogOperation`、目标关键词或业务目标；
- 命中的 OCR 文本或模板名；
- 屏幕绝对点击点；
- 小验证区域的屏幕绝对矩形；
- 洗色类型，例如 green/yellow/template-specific；
- fingerprint 字符串、`preparedAtMs`、`lastVerifiedAtMs`；
- debug 图片路径或 source 标记，方便日志追踪。

Execution rule:

- watcher 只能准备和验证 prepared action，不能点击，不能切 phase，不能改变任务状态。
- 任务真正拿到窗口输入权时，先查当前窗口是否有同 operation/target 的 prepared action。
- 如果 prepared action 最近被 watcher 验证过，就直接走输入队列点击缓存坐标。
- 如果没有 prepared action、operation 不匹配、fingerprint 已变化、验证太旧，就回到现有 `DialogService.handleDialog(...)` 正常路径。

Safety / fallback:

- `DialogType.NONE` 必须直接返回 `NO_DIALOG`，不能拿非 Dialog 背景去做 route OCR。
- 如果 watcher 发现 Dialog 区域变化，必须立即废弃旧 prepared action，重新跑完整匹配。
- 所有截图/临时图仍然走窗口绑定和 window-scoped temp path，不能共享固定 temp 文件导致多窗口串图。
- 真正鼠标点击仍必须走 `InputSequences`，并保持 move + click 原子动作。

Implementation steps:

- 已加 Lombok `@Value + @Builder` 的 prepared action model，放在 dialog model 包里，不放 service 实现包。
- 已在 `WindowRuntimeContext` 增加 per-window preparation request 和 prepared action 引用，供 watcher 写入、任务读取。
- 已在 `ImagePreprocessor` 增加小图 binary fingerprint 计算/距离比较能力。
- 已让 `DialogService` 的 route/keyword OCR 命中结果能生成 prepared action；正常 handle 路径仍会按原逻辑点击。
- 已加 `DialogService.prepareRouteKeywordOption(...)`，复用现有 route OCR，但 prepare-only 不发送点击。
- 已接 `WindowTaskRunner` watcher：只有当前窗口存在 `DialogPreparationRequest` 时才后台 prepare，当前只覆盖 `DialogOperation.ROUTE_TRANSFER`。
- 已接 watcher 小区域 fingerprint 验证：prepared action 存在时先截命中点附近小区域，按 green/yellow/template-specific 洗图后比较 fingerprint；验证通过刷新 `lastVerifiedAtMs`，验证失败废弃缓存并重新 prepare。
- 已接 `NavigationService.clickRouteDialogOption(...)`：声明 route preparation request；若当前窗口已有同 target 且最近验证的 prepared action，优先点缓存坐标，否则回到 transfer memory / OCR 原路径。

Remaining:

- `NavigationService` 只有进入 route dialog 分支时才声明 preparation request；如果要更早预热，需要在更上层确认 route dialog 即将出现时提前写 request。
- 后续实测要看日志里的 `dialog prepared` 和 `route dialog probe uses prepared action` 是否成对出现，以及 route dialog 占权是否下降。

Validation:

- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-04 导航压力测试调度修正

Status: implemented / compile passed / latest 2-window rerun success

Why this entry exists:

- 接昨天的导航压力测试复盘继续处理两个问题：
  - 跑路过程中不应该再次打开/点击小地图；
  - `route-dialog` 在 task turn 内跑完整 `DialogService.handleDialog(...)`，导致一个窗口占权 4-12 秒，其他窗口接权慢。
- 用户明确要求不要动已验证的正式导航算法。本次只改 `DebugNavigationStressTask`，没有改生产 `NavigationService`。

Changed files:

- `src/main/java/com/bot/dhxy/task/DebugNavigationStressTask.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 在 `observer UNKNOWN` 准备判定 stalled 并重新进入 debug-local navigation 前，重新读取一次最新 `WindowPathingSnapshot`。
- 如果边缘像素检测期间 watcher 已经刷新为 `ARRIVED`，或最新坐标已经接近目标，直接消费 arrival 并完成当前目标，不再重新点小地图。
- 如果 watcher 在边缘像素检测期间刷新过，但还没有明确到达，则跳过旧 snapshot 的重试，下一轮用新 snapshot 判断，避免移动中基于旧坐标误重试。
- `pendingRouteDialog` 不再通过 `TaskTransactionRunner.run(...)` 持有粗粒度 task turn；它现在和 debug navigation 一样在 task turn 外执行。
- 真实鼠标点击仍由 `DialogService` 内部输入队列串行处理；本次只移除 route dialog OCR/匹配期间的 task-turn 占用。

Validation:

- `mvn -q -DskipTests compile` passed.
- 2026-06-04 12:20 重新绑定 2 个窗口后 live rerun：
  - 注册结果：`requested=2 success=2`。
  - `hwnd-E9058C / 刑部ㄨ忍者`：2 个目标完成，`task finished: 导航压力测试 -> SUCCESS`。
  - `hwnd-59099E / 忆叶知秋`：2 个目标完成，`task finished: 导航压力测试 -> SUCCESS`。
  - 旧快照兜底生效：`observer refreshed while checking edge pixels; consume arrival before retry`，没有因为旧 UNKNOWN 快照再重开小地图。
  - 视觉上可能像只有一个号在跑，是因为其中一个窗口起点已经接近第一个目标，第一段很快完成并放权。

### 谢帅 - 2026-06-04 导航压力测试收尾复盘

Status: latest 2-window stress run finished / remaining latency bottleneck identified

Why this entry exists:

- 用户要求睡前把本轮导航压力测试日志看完并写入 MD，明天继续。
- 当前目标仍然是验证五环式多窗口跑图的放权/接权延迟：一个窗口点完小地图开始移动后，应尽快释放 task turn，下一个窗口接手不应超过约 3 秒。
- 用户明确要求不要再改已经验证过的正式导航算法；后续如果要试新的导航节奏，应在 `DebugNavigationStressTask` 或单独 debug copy 里做，不要动生产 `NavigationService` 的核心寻路算法。

Latest run observed:

- 日志文件：`logs/dhxy-console.log`
- 本轮可见窗口：
  - `hwnd-2471120`，title/角色包含 `忆叶知秋`
  - `hwnd-412B2`，title/角色包含 `刑部ㄨ忍者`
- 两个窗口最终都完成：
  - `03:01:55.019`，`hwnd-2471120`：`task finished: 导航压力测试 -> SUCCESS`
  - `03:01:59.257`，`hwnd-412B2`：`task finished: 导航压力测试 -> SUCCESS`

What was fixed/confirmed in this round:

- 之前 `observer UNKNOWN` 时会一直 `keep yielding without navigation retry`，可能空转到 `PATHING_TARGET_WAIT_TIMEOUT_MS=90000`。
- 当前 debug 路径已不再这样死等：UNKNOWN 时会优先看 watcher snapshot 的当前坐标；坐标停住太久才用边缘像素兜底确认是否仍在移动；确实停住才重新进入 debug-local navigation。
- 最新日志没有再出现旧的 90 秒卡死。`waitPathing` 多数 transaction 是 `0-4ms`，窗口 handoff 多数是 `0-260ms`。
- 边缘像素确认移动的兜底耗时约 `887-916ms`，仍低于 3 秒目标。

Remaining problems:

- 最大延迟已经转移到 route dialog 处理，而不是 task turn 本身：
  - `03:00:43.275`，`hwnd-2471120`，`debug-nav-stress:route-dialog:1-长安` held `12247ms`
  - `03:01:16.689`，`hwnd-2471120`，`debug-nav-stress:route-dialog:2-长安城东` held `5104ms`
  - `03:01:30.700`，`hwnd-412B2`，`debug-nav-stress:route-dialog:2-长安城东` held `4512ms`
  - `hwnd-412B2` 的 `route-dialog:1-长安` 本轮约 `2864ms`，勉强在 3 秒内。
- `DebugNavigationStressTask` 现在的 `routeDialogProbe` 仍走 `DialogService.handleDialog(...)`，会做较重的 OCR/选项处理，并且在 task turn 内执行，所以会挡住其他窗口接权。
- `03:01:58.036` watcher 已经报 `hwnd-412B2` 到达 `长安城东(166,117)`，但随后 task wait 分支仍基于旧 snapshot `长安城东(110,162)` 继续边缘像素兜底，最后 `03:01:59.257` 才通过 `cached coordinate already near target` 收尾。这里不是死循环，但有一次不必要的重入/兜底。
- 用户肉眼观察到：窗口正在跑路过程中，中间仍然又打开/点击了几次小地图。这不应该发生；它说明 debug wait/pathing 判断链路某一刻把“仍在移动”误判成“停下或需要重试”，于是重新进入导航点击。明天需要从日志里把这些重复小地图点击的时间点串出来，重点查 `observer UNKNOWN`、旧 snapshot、边缘像素兜底、`READY_TO_CONTINUE` 重入之间是哪一步导致了移动中重试。

Next steps:

- 不动正式 `NavigationService` 寻路算法。
- 优先处理 `DebugNavigationStressTask` 的 route dialog 占权：
  - 方案 A：把 debug route dialog 的重 OCR/handleDialog 从 task turn 内移走，只把真正需要物理点击的短输入动作排队。
  - 方案 B：给压测写一个 debug-only 的轻量 route option 点击路径，不走完整业务 `DialogService.handleDialog(...)` 两轮兜底。
- 在 `waitForPathing` 的边缘像素兜底后，决定 `re-enter debug-local navigation` 前重新读取一次最新 `WindowPathingSnapshot`；如果 watcher 已经是 `ARRIVED`，直接完成目标，避免旧快照造成额外 1 秒左右尾巴。
- 专门复盘“移动中再次点击小地图”的时间线：只要已经确认 PATHING_STARTED，除非 watcher 明确 `STOPPED_AWAY` 或最新坐标长时间静止且边缘像素也确认没动，否则不应该重新点击小地图。
- 继续用 2 窗口、小目标数压测，要求日志能直接看出：
  - 上一个窗口点击路线后什么时候释放；
  - 下一个窗口什么时候接权；
  - route dialog、waitPathing、navigate 每段各自耗时；
  - 是否还有任何单段超过 3 秒。

### Tang De - 2026-06-03 路线结果原生测试图整理

Status: copied / no source files moved

Why this entry exists:

- 用户希望把非 failure-case 的路线结果原生截图整理到 `failure-cases` 旁边，作为后续回归测试样本库。
- 只收原生路线结果图，不收 `yellow`、`green`、`marked`、`mask` 等派生处理图。

Changed files:

- `images/test-cases/world-map-route/raw/*`
- `images/test-cases/world-map-route/raw/manifest.csv`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 从 `images/temp/world_map_route_online_dry_run/**/case_*_raw.png`、各窗口 `map_result_scan.png`、`route_guard_ascii/raw_current.png`、`route_replay_ascii/raw.png` 复制样本。
- 没有移动或删除原始临时图，也没有改动 `images/failure-cases`。

Validation:

- 共复制 `272` 张 raw PNG。
- 文件名派生图检查：`yellow|green|marked|mask|preview|after_click` 命中数为 `0`。
- 追加内容清洗：移出 `5` 张没有导航绿字/不是路线结果的图到 `images/test-cases/world-map-route/rejected/no-green-or-no-route/`。
- 清洗后 raw 测试集剩余 `264` 张 PNG；绿色像素复查 `GreenLt1=0`，没有绿字为 0 的图。

### Tang De - 2026-06-03 小地图大唐境内模板重建

Status: implemented / live probe passed

Why this entry exists:

- 用户确认 `『忍者』影` 当前在大唐境内，但本地小地图模板识别返回 `大唐边境 score=0.652`。
- 检查模板尺寸发现 `大唐境内.png` 被裁成 `51x14`，而当前清洗图和同类四字地图模板通常是约 `55/56x18`，导致 `大唐边境.png` 更容易被误收。

Changed files:

- `images/template/map_label/大唐境内.png`
- `docs/ACTIVE_WORK.md`

Validation:

- 用 `images/temp/hwnd-20097C/minimap_map_label_clean.png` 覆盖重建 `大唐境内.png`，尺寸变为 `55x18`。
- 重新跑无输入本地探针后，`『忍者』影` 命中 `map=大唐境内 coord=(54,143) score=1.000 provider=MINIMAP_TEMPLATE`。

### Tang De - 2026-06-03 五环 WAIT_PATHING 战斗后空转修正

Status: implemented / compile passed

Why this entry exists:

- 用户复盘 12:49 左右五环五开日志，指出窗口从战斗出来后仍停在 `WAIT_PATHING`，随后花数秒做移动检测、`CHECK_COMBAT`、再进入 `giveItemAndTriggerPathing`，整条链路都不应该发生。
- 典型例子：`岁月醉白头 hwnd-2520B6C` 在 `12:49:30` 左右拿到 turn 后，仍按寻路等待处理，约 4 秒后才判停，再无条件进入对话/给鞋分支。

Root cause:

- 五环 V2 的 `WAIT_PATHING` 同时承担“绿字寻路后等待移动”“战斗中等待结束”“弹窗后继续处理”三种语义。
- 战斗可能由 window-level combat watcher 发现并维护，但五环自己的 phase 仍停留在旧的 `WAIT_PATHING`。
- `CHECK_COMBAT` 在无战斗时默认进入 `HANDLE_DIALOG`，而 `HANDLE_DIALOG` 默认先尝试 `giveItemAndTriggerPathing`，导致没有点 NPC、没有交鞋场景时也会 focus 并尝试给鞋。

Changed files:

- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingPhaseContext.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `WAIT_PATHING` 开头先调用 `autoCombatService.handleCombatTick(...)`，优先处理战斗进入/退出。
- 如果战斗已退出，直接进入 `SYNC_TASK_PANEL`，不再调用 `detectMovementState()` 证明角色停下。
- 如果战斗仍在进行，记录 `combatObservedSincePathing` 并 yield 到 `CHECK_COMBAT`。
- 只有已经看到过真实移动的 `WAIT_PATHING`，才允许调用重型 `gameStateUtil.detectMovementState()` 判断停稳。
- 如果尚未观察到移动，只做轻量弹窗检查和短重试；超过轻量确认次数后直接回 `SYNC_TASK_PANEL`，不再把“没动过”当成“移动后停下”。
- `CHECK_COMBAT` 对 `pathing-dialog-before-move-check-combat`、`pathing-combat-running` 或 `combatObservedSincePathing=true` 的状态，在无战斗时直接回 `SYNC_TASK_PANEL`，不再落入 `HANDLE_DIALOG -> giveItemAndTriggerPathing`。

Validation:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-03 五环任务追踪块测试图整理

Status: completed

Goal:

- Preserve the useful 五环 task-tracker block screenshots from the heavy experiment directory as a focused test-case set.

Changed files:

- `images/test-cases/task-tracker/wuhuan-task-panel-block/README.md`
- `images/test-cases/task-tracker/wuhuan-task-panel-block/manifest.csv`
- `images/test-cases/task-tracker/wuhuan-task-panel-block/raw/**`
- `images/test-cases/task-tracker/wubei-task-panel/README.md`
- `images/test-cases/task-tracker/wubei-task-panel/manifest.csv`
- `images/test-cases/task-tracker/wubei-task-panel/raw/**`
- `docs/ACTIVE_WORK.md`

Done:

- Copied only the 303 `wuhuan_tracker_*_block_raw.png` images from `images/temp/hwnd-20B3E`.
- Did not move or delete the original temp images.
- Kept the 五环 images flat under `wuhuan-task-panel-block/raw/`; no nested folders.
- Excluded `yellow`, `click_debug`, and marked/derived images.
- Added `manifest.csv` with source path, target path, file size, and source timestamp.
- Copied 342 五倍 task-tracker raw images into `wubei-task-panel/raw/`, also flat with no nested folders.
- Prefixed 五倍 raw filenames with source hwnd to avoid collisions across temp directories.
- 五倍 categories are recorded in the manifest only:
  - `panel-raw`: 144
  - `panel-wide-raw`: 141
  - `destination-hint-raw`: 57

Validation:

- Verified 五环 raw PNG count is 303 and `raw/` has 0 subdirectories.
- Verified 五倍 raw PNG count is 342 and `raw/` has 0 subdirectories.

### Tang De - 2026-06-02 五倍黄袍连战队员补给窗口

Status: implemented / compile passed

Why this entry exists:

- 用户反馈 23:10 左右黄袍冠/黄袍怪战斗结束后，其他角色没有得到补给机会。
- 日志显示 `23:11:00` 队员 `hwnd-50CB4` 战后 no-focus 预检发现人物法力低于 50%，并设置了 pending first-aid。
- 但 `23:11:01` 队长已经继续点击下一场 `wubei:enter-battle`，`23:11:03` 该队员重新进入战斗，pending first-aid 没来得及拿到 task turn。

Root cause:

- 黄袍连战路径不是普通 phase handoff，而是在 `returnHomeAfterCombatOrContinueSpecialTarget(...)` 里用内部 `while` 连续完成“战后扫任务追踪 -> 点下一场 -> 等下一场战斗结束”。
- 这个内部循环持有队长 task turn，绕过了之前为 `post-battle-chained-recovered` 加的 handoff delay，所以队员只能标记 pending first-aid，无法真正执行补给。

Changed files:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 移除黄袍连战内部 `waitForBattleAndFinish(...)` 隐藏循环。
- `RETURN_HOME` 阶段现在每次只处理一次黄袍战后追踪判断：
  - 如果任务追踪还有 `黄袍`，点击下一场后返回 `WAIT_BATTLE_FINISH` 的 shared-state outcome，让状态机释放 task turn。
  - 如果任务追踪不再有 `黄袍`，才使用回程物品并进入归队检查。
- 增加 `currentRoundChainedCombatContinueCount` 记录本轮黄袍连战次数，仍保留 `MAX_CHAINED_COMBAT_ATTEMPTS` 上限。
- 这样每场黄袍之间都会回到主状态机，队员 pending first-aid 有机会抢到 task turn 补给。
- 23:15 复盘追加：队长不是没进黄袍战斗，而是 `23:11:04` 已进战斗；`23:14:48` 因黄袍战斗等待超过原 180 秒被误判 timeout，随后恢复流程重新去点接任务 NPC `降魔侍卫`。
- 黄袍连战等待战斗结束 timeout 单独放宽到 300 秒，普通五倍战斗仍保留 180 秒；timeout 日志现在会打印 chained/elapsed/timeout。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-06-02 五倍医保宝维护选项兜底

Status: implemented / compile passed

Why this entry exists:

- 用户反馈最近五倍卡在医保宝/沙拉买提附近，队员看起来也没有收到医保宝处理。
- 日志显示队长已经到达沙拉买提，并且 `NpcClickService` 已验证 `heal_pet_option.png` 可见；真正失败点在后续 `DialogService` 的 `wubei:heal-pet-broadcast` 点击阶段返回 `BUSINESS_OPTION_NOT_FOUND`。

Changed files:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 保留自动战斗维护轮询用的固定小区域 fast path，避免每次维护扫描都扩大成本。
- 固定区域也已同步调宽：医保宝和修装备的左上角 Y 各自按原始区域上移约 30px，避免只截到文字下半段或边缘。
- 当固定小区域的 `heal-pet` 和 `repair-equipment` 都未命中时，新增一次通用业务选项兜底：重新检测当前对话框，然后复用已有 `handleBusinessOption(false, detection)`。
- 这个兜底仍然在 `handleDialog` 入口内执行，不新增外部快捷链路，也不绕过对话框处理策略。
- 队员没看到医保宝的直接原因是队长这次 broadcast 选项没有点成功，成员窗口还在等待 task turn / 维护处理，随后用户发起了暂停。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-06-02 通用地图名 OCR 纠错服务

Status: implemented / compile passed / applied to shared map-name entrances

Why this entry exists:

- 用户指出地图名不应该完全相信 OCR 原文，例如 `莲花洞` 被任务追踪浮框 OCR 成 `莲花同`，后续所有用地图名的逻辑都可能误判。
- 项目里已经有合法地图名来源：`images/template/map_label/*.png` 和 `config/maps.json`，可以用来做最近匹配纠错。

Changed files:

- `src/main/java/com/bot/dhxy/service/MapNameCanonicalizer.java`
- `src/main/java/com/bot/dhxy/vision/LocationVisionService.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 新增 `MapNameCanonicalizer`：
  - 第一次调用时懒加载合法地图名集合。
  - 来源包括小地图名字模板文件名和 `config/maps.json` key。
  - 后续只做内存字符串编辑距离匹配，不重复读磁盘。
- 匹配规则：
  - exact match 直接返回合法地图名。
  - 编辑距离 `1` 直接纠正。
  - 编辑距离 `2` 只在第一名明显优于第二名时纠正。
  - 模糊时保留 OCR 原文并打 WARN，避免误改成别的地图。
- 五倍 `sameLooseMapName(...)` 已改为先 canonicalize 当前地图名和任务追踪目的地地图名，再比较。
- `LocationVisionService.scanCurrentLocation()` 已接入纠错：小地图模板、本地 OCR、百度 OCR 返回的位置都会先规范地图名再进入 `syncMyPosition()`/全局记忆。
- `GameStateUtil.isSameMapName(...)` 和 `confirmCurrentMap(...)` 已接入纠错：导航、修罗、五倍等共享地图确认逻辑会统一比较 canonical map name。
- `ObjectiveTextRecognitionService` 已接入纠错：修罗/任务 story objective 输出的 `ObjectiveTextResult.mapName` 会规范化后再进入后续导航。
- 移除了五倍内部临时的 `同 -> 洞` 规则；后续新增地图名 OCR 入口应优先复用 `MapNameCanonicalizer`。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-06-02 五倍显形镜目的地校验修正

Status: implemented / compile passed

Why this entry exists:

- 用户反馈 19:59-20:00 五倍队长做显形镜任务时没有打开包裹、没有使用显形镜，随后判定任务失败。
- 日志确认本轮已识别到任务追踪黄字 `宝象述情|显|形镜`，并进入 `probe-objective tracker detected` 分支。
- 第一条绿字寻路读到目的地浮框 `莲花同(62,44)`，实际小地图识别为 `莲花洞(71,43)`；因为地图名 OCR 把“洞”误读成“同”，且坐标 dx=9 超过原容差 8，被判 `near=false`。
- 第二条绿字没有读到可信目的地浮框，代码按保护逻辑 `refuse item usage`，所以没有打开包裹使用显形镜。

Changed files:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 五倍任务追踪目的地地图名归一新增 `同 -> 洞`，覆盖 `莲花洞` 被 OCR 成 `莲花同` 的情况。
- 显形镜目的地到达容差从 `8` 调整为 `12`，避免已经接近目标点但小地图/浮框坐标有轻微偏差时拒绝使用显形镜。
- 仍保留“必须有目的地 hint 且当前位置接近 hint 才能用显形镜”的保护，不会无条件开包裹。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-06-02 五倍黄袍连战队员补给缓冲

Status: implemented / compile passed

Why this entry exists:

- 用户在 19:48 的五倍日志里观察到：打完黄袍后，`岁月醉白头` 明显要补法，但队长已经点进下一场，补给时间不够。
- 日志确认：队长 `hwnd-2043A` 在 `19:48:20.302` 先出战斗并完成战后体检，`19:48:29.971` 已点击 `wubei:enter-battle`；队员 `岁月醉白头 hwnd-100060A` 到 `19:48:29.477` 才确认出战斗，`19:48:31.190` 才执行 `playerState:healAll` 右键补法。
- 这不是队员没有触发补给，而是五倍黄袍连战续打太快；原本只有约 `800ms + 900ms` 的补给窗口，不足以覆盖队员晚出战斗的情况。

Changed files:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 在五倍 `post-battle-chained-recovered` 的 task-turn handoff 上增加按窗口数计算的队员补给缓冲。
- 缓冲发生在释放 task turn 之后，避免队长占着回合睡眠，确保队员窗口能拿到回合执行 `AutoCombatService` 的战后检测和 `playerState:healAll`。
- 当前计算：每个队员窗口 `2200ms`，最大 `10000ms`。五开时通常给约 `8800ms`，覆盖本次日志里队员比队长晚出战斗约 9 秒的场景。
- 只影响五倍黄袍类 chained combat 战后续打；普通五倍回程、五环、修罗不受影响。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-06-02 本地 OCR 启动命令修正

Status: implemented / compile passed

Why this entry exists:

- UI 启动已改成必须等待本地 OCR 就绪后才允许扫描/控制窗口，但本机 `python` 命令解析到 WindowsApps 商店别名，sidecar 进程会启动失败或直接退出。
- `py -3` 能正常加载 RapidOCR 和 ONNXRuntime，因此启动命令应优先走 Windows Python launcher。

Changed files:

- `src/main/java/com/bot/dhxy/ui/LocalOcrSidecarService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `LocalOcrSidecarService.ensureProcessStarted()` 现在优先执行 `py -3 scripts/local_ocr_server.py --host ... --port ...`。
- 只有 `py -3` 启动失败时才回退到 plain `python`。
- 这避免被 WindowsApps 的假 `python.exe` 吞掉 OCR sidecar 启动。
- 手动拉起的 OCR 进程已关闭，当前 18761 health 不可用，便于用户从 UI 启动链路重新验证。

Validation:

- `py -3` 可以成功初始化 RapidOCR engine。
- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-31 自动战斗维护弹窗 fast-path

Status: implemented / compile passed / heal-pet region tested

Why this entry exists:

- 用户指出自动战斗只关心医保宝和修装备两个维护弹窗，其他 dialog 不需要完整识别，应直接忽略。
- 约束：仍必须通过 `DialogService.handleDialog(...)` 入口，不在外部新增快捷检测入口。

Changed files:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `handleDialog(...)` 现在会先识别自动战斗维护选项请求：`sourceTask` 以 `auto-battle` 开头、`CLICK_BUSINESS_OPTION`、且不包含 cleanup 选项。
- 命中该场景时，不走通用 dialog mask / story / option / OCR 流程，直接截当前绑定窗口的两个固定相对区域：
  - 医保宝：`(262,382)-(372,402)`
  - 修装备：`(258,390)-(338,414)`
- 两个区域只匹配各自模板：`heal_pet_option.png` / `repair_equipment_option.png`。命中才点击，未命中返回 `BUSINESS_OPTION_NOT_FOUND`。
- 其他任务 dialog 和非自动战斗业务选项仍走原有通用 `handleDialog` 流程。

Validation:

- `mvn -q -DskipTests compile` passed.
- 用户当前游戏窗口 base 为 `(1316,358)`，按绝对区域 `(1578,740)-(1688,760)` 截取医保宝区域，洗绿字后与 `images/template/dialog/maintenance/heal_pet_option.png` 匹配结果为 `[63.5, 11.0, 1.0]`，说明该区域和模板能命中。
- 后续在修装备弹窗上验证：原用户给定修装备区域截到的是“修理身上”上一行，不能命中 `确认修理`；改为相对 `(258,390)-(338,414)` 后，绝对 `(1574,748)-(1654,772)` 截图与 `images/template/dialog/maintenance/repair_equipment_option.png` 匹配结果为 `[39.0, 11.5, 1.0]`。
- 未做实际点击测试，避免误点当前窗口。

### Tang De - 2026-05-31 删除 AutoBattleTask 内部 follower-support 模式

Status: implemented / compile passed

Why this entry exists:

- 用户明确要求不要再有单独的 follower-support 模式；队员窗口如果被分配到自动战斗，也应该只跑同一种自动战斗逻辑。
- 之前先把 `700ms` 降到 `3000ms`，但这仍保留了第二套内部分支；本次直接删掉 `AutoBattleTask` 里的 follower-support 分支。

Changed files:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/startup/TaskTeamAssignmentPolicy.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `AutoBattleTask` 不再判断 `windowRole=MEMBER` / `requestedTaskCode != auto_battle`，也不再有 follower 专属 combat tick、归队、补给、维护广播或三技能路径。
- 所有进入 `AutoBattleTask` 的窗口都走同一套循环：战斗 tick -> 空闲维护 -> 统一轮询间隔。
- `TaskTeamAssignmentPolicy` 仍可把不能跑主任务的队员分配到 `AUTO_BATTLE`，但这只是任务分配，不再代表第二种内部模式。
- 后续仍需要单独处理维护顺序：三技能/维护未到时间时，不应先跑维护弹窗检测。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-31 五倍接入 Alt+A 直点战斗兜底

Status: implemented / compile passed

Why this entry exists:

- 用户确认修罗的 Alt+A 直点战斗兜底也需要接到五倍上，用来处理怪物头顶任务 tooltip 被固定 UI 挡住、普通 `clickNpcSmart` 无法触发进战斗弹窗的情况。
- 当前不先加 NPC attribute；五倍先靠调用位置限制风险，只在战斗目标路径使用，不碰接任务/补给/修理 NPC。

Changed files:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 五倍 `tickWaitBattleFinish(...)` 现在在任务追踪寻路停稳、目的地 hint 判定已到达、已知进战斗弹窗未命中、普通任务 tooltip fallback 也未命中后，才调用 `npcClickService.tryDirectCombatTargetClick(...)`。
- 直点目标名从任务追踪黄字里解析；连续战斗的黄袍场景优先使用 `黄袍` 关键字。
- 直点请求使用目的地浮框 OCR 出来的地图和坐标，标记为 roaming target，并继续复用 `NpcClickService` 的 smart-click pipeline 和 Alt+A 退出验证。
- `ACCEPT_NPC_NAME` 会被过滤；五倍接任务 NPC、补召唤兽、修装备仍只走普通 smart click / 业务弹窗流程。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-30 主控停止按钮语义和停止结果修正

Status: implemented / compile passed

Why this entry exists:

- 用户反馈战斗中点“停止运行”看起来没有停。最新日志显示选中的 `hwnd-264100A` 已立即收到 stop 并停止，但其他窗口仍在运行，UI 的“停止”语义容易被理解成停止全部。
- 之前 `stopWindows(...)` 对已经没有活任务的窗口也会计为成功，导致后续点击停止/暂停的提示容易误导调试。

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 主控工具栏红色按钮改为 `停止所选`，旁边新增可见的 `停止全部`，避免误把所选停止当成全局停止。
- `WindowTaskRunner.stopCurrentTask()` 改为返回 boolean：只有存在活任务，或正在清理 ERROR/STOPPING 终态时，才算接受停止。
- `WindowTaskControlService.stopWindows(...)` 现在会区分 `已请求停止`、`当前没有运行任务`、`窗口不存在`。
- `WindowTaskControlService.stopAll()` 现在返回实际接受停止的窗口数，而不是把所有注册窗口都算成功。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-30 运行/暂停期间锁定配置修改

Status: implemented / compile passed

Why this entry exists:

- 用户决定把配置生效规则定死：任务运行或暂停期间不允许改配置，避免有些参数热生效、有些参数需要重启任务的灰区。
- 用户期望用户先停止任务，等窗口不再运行/暂停后，再改设置并重新启动。

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 设置页新增锁定提示：所有窗口任务都停稳后才允许修改。
- 只要任一窗口处于 `QUEUED` / `RUNNING` / `PAUSED` / `STOPPING`，设置页任务次数、三技能/维护、补给相关控件和应用按钮都会禁用。
- 主控任务方块的次数快捷编辑也会在 busy 状态禁用；真正 apply 时再做一次保护检查。
- 锁定的是“脚本任务配置”，不要求关闭游戏客户端或重启整个 APP；要求先停止全部任务。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-30 主控隐藏地图测绘入口并新增暂停快捷键

Status: implemented / compile passed

Why this entry exists:

- 用户希望主控任务选择下面的地图校准/测绘按钮先不要显示，后续如果需要再恢复。
- 用户希望增加全局快捷暂停键，使用 `Ctrl+Shift+F11`；现有 `Ctrl+Shift+F12` 继续作为紧急停止。

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/input/GlobalEmergencyStopHotkeyService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 主控任务选择面板不再挂载地图校准名、地图测绘按钮和提示文案；相关按钮/后端方法暂时保留，未删除。
- 全局 hotkey service 新增注册 `Ctrl+Shift+F11`，触发 `WindowTaskControlService.pauseAll()`。
- `Ctrl+Shift+F12` 仍触发 `WindowTaskControlService.stopAll()`。
- 顶部提示改为同时展示暂停和紧急停止快捷键。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-30 主控任务入口清理：只展示新版修罗

Status: implemented / compile passed

Why this entry exists:

- 用户要求 UI 里不要同时出现“修罗”和“修罗V2”；现在只保留新版修罗入口，对外显示为“修罗”。
- 用户还要求从主控任务选择里移除 `队伍识别测试`、`修罗Story目标测试`、`修罗任务栏目标测试`、`修罗模拟目标导航测试`。

Changed files:

- `src/main/java/com/bot/dhxy/task/model/TaskType.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `TaskType.XIULUO_V2` 的 display name 改为 `修罗`。
- `XiuluoTaskV2` 的任务运行名改为 `修罗`，日志/运行任务列不再显示 `修罗V2`。
- UI 下拉框和任务方块改走同一个 `selectableTaskTypes()` 过滤列表。
- `selectableTaskTypes()` 隐藏旧 `XIULUO` 和上述 4 个调试任务；enum 暂时保留，避免旧保存值/并行代码引用直接断裂。
- 删除主控里单独的 `队伍识别测试` 按钮入口。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-30 修复暂停卡在 NPC Ctrl 探测后才生效

Status: implemented / compile passed

Why this entry exists:

- 用户反馈点暂停后没有及时暂停。
- 最新 `logs/dhxy-console.log` 显示 UI 在 `14:11:27.346` 已经给 5 个窗口发出暂停请求，4 个窗口约 1.3 秒后到达 pause checkpoint；`hwnd-3FD0F90` 卡在 `NpcClickService` 的 `npcClick:ctrlMenuScan:灵兽村使者` 探测循环里，直到 `14:11:41.050` 才碰到 checkpoint。

Changed files:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `NpcClickService` 注入 `TaskExecutionContextHolder`。
- `clickNpcByCtrlMenuScan(...)` 在 Ctrl 探测开始前、每个 probe 前、每个 probe 后直接调用 `TaskCheckpoint.throwIfStopRequested(...)`。
- 单次已经进入 input worker 的 Ctrl 原子探测不被中途拆开，但外层不会再连续跑完整个 probe 列表才响应暂停。

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-29 通用维护入口第一版落地

Status: implemented / compile passed

Why this entry exists:

- 用户要求把“医保宝 / 修装备 broadcast / 三技能”这套维护能力落实成通用维护入口，避免继续散在
  `AutoBattleTask`、`UICleanerService` 和任务 hook 里。
- 当前第一版只做调度边界迁移：具体识别、点击、面板操作仍然复用已有服务，不重写业务算法。

Changed files:

- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- `src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceRequest.java`
- `src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceResult.java`
- `src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceStatus.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 新增薄的 `TaskMaintenanceService`：
  - 医保宝 / 修装备 broadcast 继续走 `DialogService.handleDialog(...)`。
  - 三技能继续走 `SummonSkillService.cleanSummonSkillsOnce()`。
  - 维护服务只负责优先级、cooldown、状态切换和日志。
- `AutoBattleTask` 不再自己维护三技能 cooldown，也不再通过 `UICleanerService` 对外处理维护 broadcast。
- 删除 `UICleanerService.handleMaintenanceBroadcast(...)` 旧入口，避免后续继续把业务维护放回 cleanup。
- 自动战斗真实挂机窗口会按顺序处理：归队按钮 -> broadcast -> 三技能。
- reassigned member / follower-support 窗口现在每 3 秒节流跑维护入口：broadcast 优先，三技能只能通过团队 round gate，避免成员窗口各自乱抢。
- 修罗 V2 的两个维护 hook 已接到通用入口，但第一版只处理已出现的 broadcast，不在修罗关键链路里执行长时间三技能。
- 修罗第一个维护 hook 已接主动医保宝：
  - `AFTER_ACCEPT_MAINTENANCE_CHECK` 会在灵兽村导航到 `超级巫医(116,70)`。
  - 导航目标会通过 `CoordinateHelper.getRandomizedPoint(...)` 在逻辑坐标附近轻微随机，且短距离不主动放权。
  - 到达后用 `NpcClickService.clickNpcSmart(...)` 点击 NPC，并用 `npc_wuyi_tooltip.png` / `heal_pet_option.png` 验证/处理医保宝选项。
  - 医保宝 hook 有独立间隔 `xiuluoHealPetMaintenanceIntervalMs`，默认 30 分钟，UI 游戏设置页可调。
  - 如果导航/点击/选项处理失败，只记录并清轻量干扰，不中断修罗主线。
- 第二步补上三技能 round gate：
  - `TaskMaintenanceService.beginTeamMaintenanceRound(...)` 记录当前正式团队任务轮次。
  - `TaskMaintenanceRequest.oneSummonSkillPerTeamRound=true` 时，同一个 `teamKey#round` 只允许一个窗口 claim 三技能名额。
  - follower-support 队员在补给、归队、broadcast 之后才会尝试三技能，而且被 3 秒巡查节流和 round gate 限制。
  - 修罗队长启动时也初始化三技能 cooldown，和自动战斗窗口共用 `summonSkillCleanRunImmediatelyOnStart` 语义。
  - 修罗队长在目标导航已经开始并放权后，等 handoff delay，再作为候选尝试一次三技能；如果队员已 claim，本轮跳过。

Validation:

- `mvn -q -DskipTests compile` passed.

Next:

- 实跑修罗时重点看日志里的 `maintenance: summon skill round claimed`，确认每轮最多一个窗口 claim。
- 如果后面要严格等“所有窗口短维护都完成”再放三技能，需要再加窗口级 ready 统计；当前版本是机会式 gate。

### Tang De - 2026-05-29 降低多窗口截图诊断日志噪声

Status: implemented / compile passed

Why this entry exists:

- 用户反馈修罗/多窗口运行时 `logs/dhxy-console.log` 基本读不了；五开时同一个底层扫描动作会乘以窗口数刷出大量 INFO。
- 最新日志显示主要噪声来自成功截图、截图指标累计、ROI 模板 miss/latency，而不是任务主流程本身。

Changed files:

- `src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java`
- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `src/main/java/com/bot/dhxy/window/diagnostics/WindowInteractionMetricsService.java`
- `src/main/java/com/bot/dhxy/tools/CoordinateHelper.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- `HWND capture probe` 成功探针日志从 INFO 降为 DEBUG；空白/兜底 WARN 保留。
- `Capture result` 成功且 provider 为 HWND 的日志从 INFO 降为 DEBUG；失败和 Robot fallback 仍保留 INFO。
- `Interaction metrics` 的普通 HWND capture 累计从 INFO 降为 DEBUG；失败和 Robot capture 仍保留 INFO。
- `coordinate.findImageInRegion` 的普通 matched/miss latency 从 INFO 降为 DEBUG；异常仍保留 WARN。

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-29 本地 OCR 随 UI 启动后台拉起

Status: implemented / compile passed

Why this entry exists:

- 用户发现主控页点“启动”时不会自动确认本地 OCR sidecar 是否已运行，导致后续本地 OCR 入口仍依赖手动先启动服务。
- 目标是启动任务前先后台检查 `bot.ocr.local-endpoint`，若本地 OCR 未响应，则异步启动 `scripts/local_ocr_server.py`，不阻塞窗口扫描和任务启动。

Changed files:

- `src/main/java/com/bot/dhxy/ui/LocalOcrSidecarService.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Implementation notes:

- 新增 `LocalOcrSidecarService.ensureRunningAsync()`：先做 `/health` 检查；未运行时用后台单线程启动本地 OCR 进程。
- 默认启动命令来自当前工作目录：`python scripts/local_ocr_server.py --host 127.0.0.1 --port 18761`，如果 `python` 启动失败，再尝试 `py -3`。
- OCR sidecar 的 stdout/stderr 追加写入 `logs/local-ocr-sidecar.log`，方便排查 RapidOCR/Python 依赖问题。
- `MainWindowController` 的主启动、队列启动、指定窗口启动入口都会先触发这个后台检查；启动不会等待 OCR 完全加载。

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-29 修罗暂存与五倍切换交接

Status: paused / Xiuluo mainline stored / next focus is 五倍

Why this entry exists:

- 用户决定先暂停修罗主线，后续再继续打磨修罗；当前更急的是开始写五倍任务。
- 本条把修罗目前已经形成的结构、已验证点、刚修过的问题和未完成风险集中存档，避免后续新线程或新任务把修罗上下文重新猜一遍。

Current Xiuluo code shape:

- Formal leader flow is now `XiuluoTaskV2` with explicit phase/context:
  - `XiuluoPhase`
  - `XiuluoRoundContext`
  - `XiuluoStepOutcome`
- 修罗 phase 是当前恢复/热启动的主线状态，不要再回到旧的“一大坨 while + 隐式分支”写法。
- `XiuluoRoundContext` 保存本轮目标、是否正在等待 pathing、是否由修罗自己点 `看打` 进入战斗、当前 phase retry 次数和 recovery 次数。
- `NavigationService` 已向 `NavigationRequest` / `NavigationResult` 方向收敛。任务层决定 phase/retry/fallback，导航层只报告结果，不应该知道修罗业务。
- `NpcClickService.clickNpcSmart(...)` 是正式点 NPC/怪的统一入口。修罗接任务 NPC、修罗战斗目标都应该走这个入口，不要再另起修罗专用 Ctrl 点击链。
- `DialogService.handleDialog(DialogHandleRequest)` 是正式 dialog 入口。修罗业务只根据 `DialogResult` 决定 phase，不让 `DialogService` 知道 `XiuluoPhase`。

Important recent fix:

- 修罗接任务 dialog 的第一行 `闲来无事，要我帮忙吗` 有时会被游戏高亮成黄色，而不是绿色。
- 原来 `VERIFY_GREEN_TEMPLATE` 只洗绿色，导致正确 dialog 已经打开但模板找不到。
- 已新增/接入 `ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(...)`，只用于 dialog option 模板匹配路径。
- 这个新洗图保留绿色选项和高亮黄色选项，但不改变通用绿色 OCR，不影响 route transfer 的黄字逻辑。
- Route/车夫传送 dialog 本来已有黄字兜底路径：`handleRouteKeywordOptionWithRetry(...)` / `processOptionsWithOCRDetailed(...)`。

Known Xiuluo templates / dialogs:

- 接任务 option：`xiuluo.acceptTask`，常用模板是 `xiuluo_accept_xianlaiwu.png`。
- 接任务同屏备用证明：取消任务模板能证明当前是修罗任务 NPC 的 option dialog，但当前不一定要点击它。
- 目标 story：接任务后出现，里面有目标地图和坐标；修罗读取后进入导航目标。
- 人数不足五人 option：由修罗决定是否继续或等待，取决于 UI 配置。
- 三人以下 blocked story/dialog：模板已加入，应该作为硬阻塞/等待类结果处理，不要泛清理后无限 retry。
- 进入战斗 option：`xiuluo.enterBattle`，匹配 `看打` 后进入 `WAIT_COMBAT`，并标记 `enteredBattleByXiuluo=true`。

Known validated / useful behavior:

- 三开测试中，两个队长和一个自动战斗窗口可以跑到修罗接任务 NPC 附近；窗口串扰比早期低。
- 修罗导航、接受任务、读 story/objective、地图导航、点怪、进入战斗的主链已经多次跑通过局部片段。
- 五环/修罗都应继续遵守：移动/导航开始后才是安全放权点；普通准备动作不要过早放权。
- HWND 截图和后台 Alt 快捷键方向仍然有效；鼠标点击仍按真实输入队列处理。

Current unfinished Xiuluo items:

- Fallback 还没有最终稳定：
  - phase 内失败应先本地 retry；
  - 再清理 UI 后 retry 当前 phase；
  - 再根据具体 phase 恢复到上一关键状态或回接任务；
  - 不应该一遇到 `FAILED` 就结束整个任务。
- `RETURN_HOME` 需要继续确认：
  - 使用修罗回城道具后要验证是否回到灵兽村；
  - 使用失败时 fallback 到导航回灵兽村；
  - 战斗热启动退出后不能直接默认进入回城，除非确认是修罗目标战斗或任务栏目标已消失。
- `WAIT_COMBAT` / auto-battle handoff 需要继续看多窗口效率：
  - 队长进入战斗后必须放权；
  - 成员应能及时进入自动战斗；
  - 如果某个窗口在战斗内长期不动，优先看 battle radar、task turn、auto-battle触发日志。
- `NAVIGATE_TO_TARGET` / `CLICK_TARGET_NPC` 仍有一些真实地图边缘和目标点误差问题，失败样本应该继续保存到按类别区分的样本目录。
- 修罗次数统计未完成。推荐以后以 phase 状态和任务面板校验结合：
  - 正常完成一轮以后自增预测次数；
  - 只有任务面板刚好被打开时顺便读真实次数并校正，不要每轮强制 OCR。
- 医宝宝/修装备/三技能维护只保留 hook，不要现在强塞进修罗主线。长期应通过薄的 `TaskMaintenanceService` 统一调度。

Files to inspect first when resuming Xiuluo:

- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoPhase.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoRoundContext.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoStepOutcome.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/tools/ImagePreprocessor.java`
- `src/main/java/com/bot/dhxy/tools/CoordinateHelper.java`
- `config/vision_memory.json`
- `config/transfer_choice_memory.json`

Suggested Xiuluo resume prompt:

> 请先阅读 AGENTS.md、docs/DHXY_CONTEXT.md、docs/ACTIVE_WORK.md 里 2026-05-29 的“修罗暂存与五倍切换交接”。我们继续修罗 V2，不要重写架构，先从 fallback、RETURN_HOME 验证、WAIT_COMBAT 放权和失败样本保存继续。

五倍切换提醒:

- 写五倍前优先复用修罗沉淀出来的公共能力：`NavigationService`、`NpcClickService.clickNpcSmart(...)`、`DialogService.handleDialog(...)`、`TaskTransactionRunner` / `TaskTurnCoordinator`。
- 不要把五倍的 dialog / NPC 点击 / 导航再写成一套独立专用链。五倍只应该定义自己的 task phases、模板、目标读取和业务 retry 策略。
- 如果五倍需要维护、医保宝、修装备、三技能，只先预留 hook；不要在五倍里复制修罗的维护细节。

### Xie Shuai - 2026-05-28 通用维护入口边界讨论

Status: proposal / waiting for Xiuluo owner review

Context:

- 用户在修罗长跑中没有看到“三技能维护”触发。
- 代码检查后发现三技能能力本身存在于 `SummonSkillService.cleanSummonSkillsOnce()`，医保宝/修理
  弹窗能力也存在于 `DialogService` 的 scoped dialog handling 里。
- 但当前没有一个真正被所有任务调用的“通用维护调度入口”。现状是维护逻辑散在多个地方：
  - `AutoBattleTask.maybeRunIdleMaintenance(...)` 调用医保宝/修理 broadcast 和三技能，但只覆盖真正
    auto-battle 空闲窗口。
  - follower-support 成员模式会跳过个人三技能维护。
  - `XiuluoTaskV2` 有 `AFTER_ACCEPT_MAINTENANCE_CHECK` 和
    `BEFORE_ROUTE_MAINTENANCE_CHECK` 两个维护阶段，但目前只是 log `hook skipped` 后继续。
  - `UICleanerService.handleMaintenanceBroadcast(...)` 当前负责医保宝/修理 broadcast，语义不合适：
    医保宝/修装备是业务维护，不是 UI cleanup。

Problem statement:

- “三技能”不应该挂在修罗专属逻辑上；它和医保宝、修理一样，属于任务运行期间的通用维护。
- “医保宝/修理”也不应该继续由 `UICleanerService` 对外承载。`UICleanerService` 应只负责关闭/清理
  UI 干扰，例如地图、普通 X 窗口、取消/离开/放弃修理这类关闭行为。
- 当前任务如果想使用维护，只能各自知道零散服务和调用顺序，后续抓鬼/修罗/五环都会重复或漏接。

Proposed boundary:

- 新增一个单一通用维护调度服务，建议名：`TaskMaintenanceService`。
- `TaskMaintenanceService` 只负责任务维护的调度、优先级、冷却、任务权控制和日志，不把具体点击算法
  全搬进去。
- 具体能力继续复用现有服务：
  - 医保宝/修理 broadcast：`DialogService.handleDialog(DialogHandleRequest.handleMaintenanceBroadcastOption(...))`
  - 三技能：`SummonSkillService.cleanSummonSkillsOnce()`
  - 血法补给：`PlayerStateService`
  - 归队/等队员：`TeamReturnService`
  - 普通窗口关闭：`UICleanerService`

Suggested maintenance priority:

1. 团队 broadcast 弹窗优先，例如医保宝、修装备。它们由队长触发，队员错过会影响团队节奏。
2. 归队/等队员这类团队状态优先于个人维护。
3. 血法补给优先于三技能；如果本轮需要补血/补蓝，就不要同时清三技能。
4. 三技能最后处理。三技能失败不更新时间，下一轮有空再重试。

Task integration proposal:

- 修罗、五环、未来抓鬼等任务不要直接写医保宝/修理/三技能细节。
- 任务只在安全阶段调用一个通用入口，例如：
  `taskMaintenanceService.runOpportunisticMaintenance(context, request)`。
- `XiuluoTaskV2` 当前两个维护阶段可以作为第一批接入点：
  - `AFTER_ACCEPT_MAINTENANCE_CHECK`：读到任务目标后、离开接任务区域前。
  - `BEFORE_ROUTE_MAINTENANCE_CHECK`：长距离寻路前。
- `AutoBattleTask.maybeRunIdleMaintenance(...)` 也应改为调用同一个维护入口，而不是自己调
  `UICleanerService` 和 `SummonSkillService`。

Task-turn / input constraints:

- 任何会 focus、点击、拖动的维护动作都必须经过 `TaskTurnCoordinator` 或当前任务已持有的任务权。
- 三技能必须拿到权限后从打开面板到检查/删除/确认一整套做完再放权。
- 三技能失败必须返回失败并且不刷新 cooldown。
- 当队长仍在关键路径中持权，例如战后还没回程/还没进入下一轮安全移动阶段，成员窗口不能插入三技能。

Open review questions for the Xiuluo owner:

- 修罗两个维护阶段是否就是合适的通用维护调用点，还是需要只保留其中一个？
- 修罗在 `BEFORE_ROUTE_MAINTENANCE_CHECK` 执行维护时，是否允许处理血法补给和三技能，还是只允许团队 broadcast？
- `TaskMaintenanceService` 的首次落地是否先只迁医保宝/修理 + 三技能，归队/血法补给后续再并入？

### He Li Review - 2026-05-28 通用维护入口边界

Status: reviewed / recommend deferring implementation until Xiuluo mainline stabilizes

Overall take:

- `TaskMaintenanceService` 这个边界方向是对的，但它必须保持很薄。
- 它应该只负责维护调度、优先级、冷却、任务权语义和日志，不应该把医保宝、修装备、三技能、血法补给等具体点击算法搬进去。
- 具体动作仍然应该复用现有能力：
  - `DialogService` 处理医保宝/修装备这类业务弹窗；
  - `SummonSkillService` 处理三技能；
  - `PlayerStateService` 处理血法/摄妖香等角色状态；
  - `TeamReturnService` 处理归队/等队员；
  - `UICleanerService` 只处理 UI 干扰清理。

Important boundary clarifications:

1. `UICleanerService` 不应该继续承载医保宝/修装备业务语义。
   - 它只能负责关闭/清理窗口、地图、普通 X 窗口、取消/离开等干扰。
   - 医保宝/修装备是任务维护，不是 UI cleanup。

2. 维护失败不能让主任务失败。
   - 三技能失败、医保宝/修装备弹窗没识别到、维护窗口没打开，都应该返回类似 `SKIPPED`、`DEFERRED`、`FAILED_RETRY_LATER` 的语义。
   - 这些结果不能映射成修罗 phase `FAILED`，更不能让窗口任务结束。
   - 只有明确的用户停止、配置禁止继续、或任务自身硬阻塞，才应该终止任务。

3. Task turn 和 physical input 是两层锁。
   - `TaskTurnCoordinator` 只决定哪个窗口的业务可以继续推进。
   - 鼠标/键盘仍然必须走 `InputSequences` / input queue。
   - 维护动作如果会 focus、点击、拖动，必须同时满足：当前任务持有 task turn，且物理输入通过 input queue 串行执行。

4. 修罗当前两个维护 hook 可以保留，但不要急着接满逻辑。
   - `AFTER_ACCEPT_MAINTENANCE_CHECK`：读到任务目标后、离开接任务区域前。
   - `BEFORE_ROUTE_MAINTENANCE_CHECK`：长距离寻路前。
   - 这两个位置作为预留点合理，但当前修罗主线还在调 phase/retry/fallback，建议先只保留 hook 和日志，不马上把三技能接进正式修罗主线。

5. 建议先定义 request/result，而不是直接写完整业务。
   - `TaskMaintenanceRequest` 描述当前任务、窗口角色、允许的维护类型、安全点、是否允许放权、当前阶段等。
   - `TaskMaintenanceResult` 描述执行了什么、跳过了什么、是否需要稍后重试、是否发生硬阻塞。
   - result 不能直接返回任务 phase；调用方任务自己决定下一步。

Recommendation:

- 短期：不要现在实现完整 `TaskMaintenanceService`。先把修罗主线的失败恢复、点怪、回接任务流程跑稳。
- 中期：先落一个很薄的 `TaskMaintenanceService` 壳，只接入最安全的一两个动作，并保证维护失败只会 defer/retry，不会中断主任务。
- 长期：五环、修罗、抓鬼、五倍、天庭都通过同一个维护入口调用，不再各自散落调用医保宝/修装备/三技能。

### Tang De - 2026-05-28 UI game settings persistence

Status: implemented / compile passed 2026-05-28

Goal:

- Fix the issue where UI task counts and game settings reset to defaults after restarting the app.

Root cause:

- The Settings tab and main task-tile count editor only updated the in-memory `BotProperties`.
- On restart, controls were rebuilt from `application.properties`, so user edits disappeared.

Changed files:

- `src/main/java/com/bot/dhxy/ui/GameUiSettingsStore.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `.gitignore`
- `docs/ACTIVE_WORK.md`

Done:

- Added `config/ui-game-settings.properties` as a local persisted UI settings file.
- UI startup now loads saved game settings into `BotProperties` before controls are created.
- Applying game settings, applying supply settings, and applying the main-page task count editor now
  save the current values.
- The local UI settings file is ignored by Git.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-28 stop during runner preflight

Status: implemented / compile passed 2026-05-28

Goal:

- Diagnose why pressing stop could leave some windows showing `停止中` instead of reaching `已停止`.
- Fix the runner-level stop path without changing Xiuluo/Five Ring business logic.

Log finding:

- Latest stop sequence showed stop at `17:11:23.895`.
- Windows already inside `AutoBattleTask` stopped normally.
- Other windows were still in pre-task team-role detection / task reassignment (`teamRole:*`,
  `task reassigned by team role`) when stop arrived.
- Those preflight paths used task-context stop checks but did not consistently convert thread
  interruption into a queue-level STOPPED result, so the UI could keep seeing `STOPPING`.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Done:

- `WindowTaskRunner` now logs each stop request with queue/progress/current task.
- Queue execution now catches stop/cancel during preflight and always writes a STOPPED queue finish.
- Team-role detection boundaries now use `TaskCheckpoint` so stop/interrupt is honored before and
  after role detection/reassignment.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-28 scoped DialogResult design for DialogService cleanup

Status: implemented by 谢帅 / compile passed 2026-05-28

Goal:

- Clean up `DialogService` so task code can use one structured dialog result instead of scattered
  template checks, while avoiding slow "scan every known dialog" behavior during normal gameplay.

Core boundary:

- Task code decides **when** a dialog should be inspected. `DialogService` must not run as a
  background scanner.
- `DialogService` owns screenshot/detection/template/OCR click mechanics and returns a structured
  result.
- The current task owns business phase decisions. `DialogService` should not know `XiuluoPhase`.

Unified result direction:

- Use one structured result type, tentatively `DialogResult`.
- The result should distinguish whether it is an action result or a text/objective result.
- Suggested fields:
  - `kind`: `ACTION`, `TEXT`, `UNKNOWN`, `NO_DIALOG`, `FAILED`, etc.
  - `dialogType`: `OPTION`, `STORY`, `NONE`, etc.
  - `actionKey`: stable key such as `xiuluo.acceptTask`, `xiuluo.enterBattle`,
    `xiuluo.underFiveConfirm`, `xiuluo.underThreeBlocked`; null for text/no-dialog cases.
  - `objective`: optional `NpcTarget` or task objective payload for text/story readers.
  - `clicked`: whether the service clicked an option.
  - `matchedText`: OCR/template text that produced the result.
  - clicked point fields: absolute and/or dialog-relative coordinates when available.

Scope rule to control latency:

- Every `handleDialog` call must include a narrow scope/request. Do not scan every known task dialog
  just because the current task is 修罗.
- Example scopes:
  - `XIULUO_HOT_START`: startup-only; may check multiple known 修罗 option dialogs.
  - `XIULUO_ACCEPT_TASK`: only match accept-task / under-five / under-three dialogs.
  - `XIULUO_READ_OBJECTIVE`: only read the accepted-task story/objective text.
  - `XIULUO_ENTER_BATTLE`: only match/click "看打".
  - `ROUTE_TRANSFER`: route/carriage destination dialog; not 修罗-specific.
  - `GENERIC_CLEANUP`: generic close/ignore policy only; no task-template sweep.

Xiuluo known dialog mapping:

- Accept task option: `xiuluo.acceptTask` -> 修罗 maps this to `READ_OBJECTIVE`.
- Under-five confirm/wait option: `xiuluo.underFiveConfirm` or `xiuluo.underFiveWait` -> 修罗 decides
  whether to continue/read objective or wait based on config.
- Under-three blocked dialog: `xiuluo.underThreeBlocked` -> 修罗 should stop/wait/fail according to
  the later policy; do not repeatedly generic-clean/retry it.
- Objective story: result kind `TEXT` with objective/NpcTarget -> 修罗 maps this to
  `NAVIGATE_TO_TARGET`.
- Enter battle option: `xiuluo.enterBattle` -> 修罗 maps this to `WAIT_COMBAT` and marks
  `enteredBattleByXiuluo=true`.

Implementation notes for the cleanup agent:

- Prefer placing cross-boundary request/result/value objects under `model.dialog` unless they are
  private to `DialogService`.
- Keep task-specific action keys stable. Prefer enums if they cross service/task boundaries; avoid
  hard-coded strings spread through task code.
- Do not make `DialogService` return task phases.
- Do not broaden normal runtime checks. Hot start can afford broader matching; normal phase calls
  should be narrow and fast.

Implementation note:

- `DialogService.handleDialog(DialogHandleRequest)` is now the structured public entry for scoped
  dialog handling.
- Green-template option handling now uses `DialogHandleRequest.handleGreenTemplateOption(...)` with a
  narrow list of `GreenTemplateClickSpec`; the returned `DialogResult.actionKey` is the task-owned
  stable action key.
- Green-template option handling is also entered through `DialogService.handleDialog(...)`; the
  concrete template click implementation stays private. Click ranges live in each
  `GreenTemplateClickSpec` instead of separate `withRange`/direct click methods.
- `XiuluoTaskV2` has been migrated for accept-task, under-five, and enter-battle template clicks.
- `XiuluoTaskV2` recovery paths now also use `DialogService.handleDialog(...)` for:
  - accept NPC click false-positive recovery: click the known accept-task option if it is already open,
    or recognize an already-open story dialog and continue to objective reading.
  - target click false-positive recovery: click the known enter-battle template first, then OCR-click
    `看打` through the same structured handler before cleaning the UI.
- `DialogResult` now carries an optional `ObjectiveTextResult` payload for story/objective readers.
  `DialogHandleRequest.readStoryObjective(...)` and `DialogOperation.READ_STORY_OBJECTIVE` let 修罗
  read the accepted-task story dialog through `handleDialog(...)` without turning DialogService into
  a task phase machine.
- `XiuluoTaskV2` now calls `handleDialog(...)` for all formal dialog interactions. The task still maps
  the returned `ObjectiveTextResult` into its own `NpcTarget`, so DialogService does not know
  `XiuluoPhase` or 修罗 business transitions.
- Navigation route-transfer dialogs now also enter through `DialogService.handleDialog(...)`:
  - remembered transfer-option points use `DialogHandleRequest.handleRememberedRouteOption(...)`;
  - OCR route choices use `DialogHandleRequest.handleRouteKeywordOption(...)`;
  - uncertain route dialogs may still OCR the captured dialog image, preserving the old transfer
    recovery behavior without exposing `handleKeywordOptionWithPoint(...)` to `NavigationService`.
- `NpcClickService` expected-dialog verification now uses `handleDialog(...)` in inspect-only mode:
  - no expected template: verify that an option dialog is visible;
  - expected green template: verify that the template is visible without clicking the option.
- `TaskHotStartService` now uses `DialogHandleRequest.inspect(...)` through `handleDialog(...)` to
  classify startup dialogs without clicking them.
- `UICleanerService` now uses `handleDialog(...)` for maintenance precheck, story fast-click, and
  generic dialog inspection. Generic OCR close/fallback-last behavior remains owned by UICleaner.
- `FiveRingTask` now also uses `DialogHandleRequest.inspect(...)` through `handleDialog(...)` for
  the remaining formal dialog-type probes, while preserving the original 五环 accept/P1 branch logic.
- Remaining direct `DialogService` calls outside `handleDialog(...)` are limited to commented legacy
  修罗/debug code and `DebugXiuluoStoryObjectiveTask`; formal runtime paths have been moved to the
  unified entry.
- `DialogService` public surface is now reduced to the formal `handleDialog(...)` entry plus the
  existing debug-only story capture helper. Old keyword/remembered-point/green-template/story-text
  public helpers were removed or made private after formal callers moved to the structured request.
- `DialogHandleResult` has been removed. Internal dialog option/give/business helpers now return
  `DialogResultStatus` directly, so `DialogResultStatus` is the single status enum crossing the
  dialog service boundary.
- `XiuluoTaskV2` now has a narrow known-option router for 修罗 option dialogs. Accept-task, enter-battle,
  and under-five confirm/wait templates are matched through the structured `handleDialog(...)` path,
  and the task maps the returned action key to `READ_OBJECTIVE`, `WAIT_COMBAT`, or `WAIT_TEAM_RETURN`.
  `READ_OBJECTIVE` uses this same router after story/task-panel objective parsing misses, so
  under-five prompts no longer fall through the generic objective failure recovery.
- Xiuluo dialog template boundary:
  - `xiuluo_accept_xianlaiwu.png`, `xiuluo_cancel_task.png`, `xiuluo_underfive_confirm.png`,
    `xiuluo_underfive_wait.png`, and `xiuluo_enter_battle_kanda.png` are generated black/white
    templates, but their runtime source is green option text. They must use the green option
    template path.
  - `xiuluo_cancel_task.png` is visibility-only proof for the accept-task dialog. Do not click it in
    the accept flow; it only tells 修罗 that the correct NPC option dialog is open when the accept
    template itself missed.
  - `xiuluo_underthree_yichangqiangda.png` is different: its runtime source is a white story/prompt
    dialog with no option row. It uses `DialogHandleRequest.verifyWhiteTemplate(...)` and maps to
    `xiuluo.underThreeBlocked`, which 修罗 treats as a hard blocked state rather than retrying or
    generic-cleaning the dialog.
- Xiuluo V2 now reserves two no-op team-maintenance hook phases without changing current runtime
  behavior:
  - `AFTER_ACCEPT_MAINTENANCE_CHECK`: after objective is read and before leaving the task-giver area.
    This is the future cheap insert point for heal-pet style team maintenance.
  - `BEFORE_ROUTE_MAINTENANCE_CHECK`: immediately before long target navigation. This is the future
    insert point for repair-equipment style detours, after which the same 修罗 objective should resume.
  - These hooks only log and continue today. The actual heal-pet/repair transaction should be shared
    across long team tasks rather than implemented as 修罗-only business logic.
- Xiuluo V2 return cleanup now has an explicit fallback phase:
  - If the Xiuluo return item cannot be found/used or does not verify arrival at 灵兽村 after retry,
    the task enters `NAVIGATE_BACK_TO_START` instead of immediately marking the round done.
  - `NAVIGATE_BACK_TO_START` uses the normal NavigationService route to the fixed 灵兽村使者 location,
    yields while pathing, and only finishes the current round after the start-area navigation arrives.
  - This keeps max-run accounting from reporting success while the leader is still stranded on a
    remote map.
- Xiuluo V2 objective-read recovery now rechecks scoped 修罗 dialogs before generic cleanup:
  - If story objective and task-panel objective both miss, it first routes known 修罗 option dialogs
    through the same action-key path used by normal phases.
  - It then checks the white under-three blocked prompt.
  - Only after those scoped checks miss does it close generic X windows and retry/recover. This avoids
    accidentally treating known 修罗 prompts as unknown UI while still keeping unrelated dialogs out of
    the task-specific template scan.

### He Li - 2026-05-27 backlog: mounted purple player-name anchor

Status: backlog / paused

Context:

- While debugging 修罗 route click through `张闻`, the `PLAYER_ANCHOR_FORMULA` path failed because the purple player-name anchor could not be extracted.
- The failing run knew the bound role name was `『忍者』影`, but the purple OCR path returned no words and then rejected the blob fallback:
  - `center_scan_player.png` OCR returned no text.
  - blob fallback saw a large noisy mask, for example `darkPixels=5592 rect=(36,143)-(308,314) size=273x172`, and correctly refused to use it as a player-name anchor.
- A temporary local experiment captured the mounted scene and produced:
  - `purpleWords=-`
  - `wordCount=0`
  - `blackPixels=21152`
  - result `name-not-matched`
- Visual inspection showed the washed purple image was dominated by mount/effect noise; the actual role-name text was not isolated into OCR-friendly lines.

Decision:

- Pause this work for now. It is not blocking the immediate 修罗 route/debug priority.
- Do not broaden production `NpcClickService` for this until we have a clean, name-aware purple candidate extraction experiment.

Future direction:

- Build a safe non-clicking experiment that captures one bound window and extracts multiple small purple text-line candidates.
- Use the known bound role name from `ClientIdentityService` / `GameContext.State.me` as the required match target.
- Reject large mount/effect blobs before OCR; only OCR compact, horizontal, text-like candidates.
- If a candidate matches the known role name or a strong fragment, return a `PlayerAnchorMatch`; otherwise return no anchor.
- Keep the experiment outside the formal task path until it is reliable on mounted characters.

### Tang De - 2026-05-27 task checkpoint consolidation

Status: implemented / compile passed

Goal:

- Stop each task/service from reimplementing task stop and thread-interrupt checkpoints differently.

Changed files:

- `src/main/java/com/bot/dhxy/runner/stop/TaskCheckpoint.java`
- `src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `src/main/java/com/bot/dhxy/vision/LocationVisionService.java`
- `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `TaskCheckpoint` as the shared stop/interruption checkpoint boundary.
- `TaskCheckpoint` supports explicit `TaskExecutionContext` and current-thread `TaskExecutionContextHolder` checks.
- `TaskSleep.sleepOrStop(...)` now delegates pre/post stop checks to `TaskCheckpoint`.
- Rule tightened after review: task/service code should call `TaskCheckpoint` directly for standard stop/interruption checkpoints. Do not add local wrappers such as `checkpoint(...)`, `checkpointTask(...)`, `throwIfStopRequested(...)`, or ad-hoc interruption-to-exception blocks unless the helper adds real domain behavior.
- Removed `NavigationService.checkpointTask()` and replaced its call sites with direct `TaskCheckpoint.throwIfStopRequested(...)`.
- Left direct interruption checks in worker loops, debug tasks, and boolean "is still running" helpers alone because those are control-loop conditions, not task checkpoint policies.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC target model seed

Status: implemented / compile passed

Goal:

- Add a canonical NPC/monster model so task code can describe "what target is this" instead of spreading name, map, coordinate, purpose, and fixed/roaming flags across call sites.

Changed files:

- `src/main/java/com/bot/dhxy/model/npc/NpcTarget.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcRole.java`
- `src/main/java/com/bot/dhxy/model/npc/NpcMovementType.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `NpcTarget` with map name, logical X/Y coordinate, primary name, aliases, role, movement type, formula tune offsets, expected dialog template, key, and source.
- Added `NpcRole` for task giver, combat target, interaction target, and debug target.
- Added `NpcMovementType` for fixed, roaming, floating, and unknown targets.
- Added `NpcTarget.toClickRequest(PlayerCharacter)` so the model can feed the current `NpcClickRequest` pipeline without forcing a big refactor now.
- Boundary decision: do not pass the full `NpcTarget` into `NavigationService`. Navigation should keep using its narrow request/coordinate inputs because it only needs map and logical coordinates, not NPC role, aliases, OCR template, or click tuning.
- Cleanup after boundary review:
  - Removed `NpcNavigationRequest.fromTarget(NpcTarget)`.
  - Removed large static `NpcTarget` builder constants from task constant sections.
  - Navigation call sites now build `NpcNavigationRequest` from narrow map/coordinate/name fields.
- Migrated first examples:
  - 五环 accept NPC now has `NpcTarget ACCEPT_NPC` and uses it for debug click, navigation coordinates, logs, and smart-click request creation.
  - 修罗 accept NPC now has `NpcTarget ACCEPT_NPC` and uses it for navigation and smart-click request creation.
  - 修罗 combat objective now builds a per-objective `NpcTarget` with role `COMBAT_TARGET` and movement type `ROAMING` before entering the smart-click pipeline.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-26 approach coordinate boundary

Status: implemented / compile passed

Decision:

- `NavigationService` only navigates to the logical coordinate it is given. It should not know whether that coordinate came from an NPC, 修罗怪, or another task target.
- Task flows that need to stand near a target should first call `CoordinateHelper.calculateApproachCoordinate(mapName, targetX, targetY)`.
- The returned coordinate is still a logical in-game map coordinate and is then passed to `NavigationService.navigateInCurrentMap(...)`.
- 修罗 now derives its approach coordinate through `CoordinateHelper` before current-map navigation; the benchmark probe uses the same helper.

Changed files:

- `src/main/java/com/bot/dhxy/tools/CoordinateHelper.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/debug/XiuluoAcceptBenchmarkMain.java`

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-26 Java/Spring/Lombok/logging SOP

Status: decided

Rule:

- Use Spring Boot beans and constructor injection for real services/collaborators. Do not manually `new` service dependencies in task/business code.
- Put shared request/result/value objects in a proper model package, not under service implementation packages.
- For immutable request/result/value objects, use the existing Lombok pattern: `@Value` + `@Builder`, with `@Builder.Default` for defaults. Static factories should call `builder()` and then `build()`.
- Use enums for operation/status/policy values that cross service/task boundaries.
- Use SLF4J logging for normal app code. Avoid `System.out.println` outside temporary local debug tools.
- Logs for automation-sensitive paths should include source task, window context when available, target map/NPC/coordinate, result status, and timing where useful.

### He Li - 2026-05-26 Java file layout SOP

Status: decided

Rule:

- Keep public classes, public APIs, and the main workflow near the top of a Java file.
- Put private nested helper types (`private class`, `private record`, `private enum`, private interfaces) at the bottom of the enclosing class/file, after the main public and private workflow methods.
- Do not insert private helper types in the middle of a business flow unless Java syntax requires it; this keeps task code and service entry points easier to review.

### He Li - 2026-05-26 latency log seed

Status: implemented / compile passed

Goal:

- Add lightweight timing logs to high-frequency automation boundaries so later UI/dashboard work can graph latency without parsing ad-hoc business messages.

Decision:

- Use one stable log marker: `[latency] event=<name> elapsedMs=<ms> detail=<key-values>`.
- Instrument boundary methods, not every helper loop, to avoid log spam.
- Keep OCR-specific timings already present in OCR/vision services; add timing around orchestration layers that combine input, navigation, dialog matching, and task-turn ownership.

Changed files:

- `src/main/java/com/bot/dhxy/tools/LatencyMetrics.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/task/transaction/TaskTransactionRunner.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`

Events now emitted:

- `input.request`
- `task.transaction`
- `npc.click.smart`
- `navigation.mapCoordinate`
- `navigation.toMap`
- `navigation.currentMap`
- `dialog.detect`
- `dialog.greenTemplateClick`
- `dialog.greenTemplateFirst`
- `bag.itemAction`
- `player.sheyaoxiang.ensure`
- `player.position.sync`
- `location.scanCurrent`

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Xiuluo map confirm wrapper cleanup

Status: completed

Goal:

- Remove thin Xiuluo task wrappers around current-map confirmation so map checks call `GameStateUtil` directly.

Changed files:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Removed `XiuluoTask.isAlreadyInTargetMap(...)`.
- Inlined its only call site in `runObjectiveReadyFlow(...)` with a direct `gameStateUtil.confirmCurrentMap(...)` call.
- Kept Xiuluo-specific logs at the call site so the formal pathing precheck remains readable without another wrapper method.
- Re-scanned map confirmation usages: remaining normal flow calls go directly through `GameStateUtil.confirmCurrentMap(...)` or `confirmCurrentMapFresh(...)`.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC region resolution moved to memory service

Status: completed

Goal:

- Move current-window coordinate conversion out of `NpcClickService` so consumers receive already resolved NPC click regions.

Changed files:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/debug/XiuluoCtrlClickDebugMain.java`
- `src/main/java/com/bot/dhxy/debug/NpcTextCandidateGameWindowDebugMain.java`
- `docs/ACTIVE_WORK.md`

Done:

- `OcrRoiMemoryService.recommendNpcClickRegions(...)` now returns `ResolvedNpcClickRegion`, which includes:
  - persisted window-relative region;
  - current window base;
  - screen-absolute rectangle.
- The conversion uses the current bound `WindowRuntimeContext` native binding when present, and falls back to `GameClientTracker` only for standalone/debug paths.
- Added `recommendNpcClickWindowRegions(...)` for debug tools that still need raw window-relative regions.
- Removed the temporary `NpcClickService.NpcScanRegion`; `NpcClickService` now consumes the resolved region from the recommendation service directly.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC scan region coordinate boundary

Status: completed

Goal:

- Stop each NPC click strategy from manually converting recommended window-relative regions to screen-absolute rectangles.

Changed files:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `NpcScanRegion`, a resolved scan-region record that keeps both:
  - `windowRegion`: 1024x768 game-window-relative region used for OCR memory and evidence.
  - `screenRect`: screen-absolute rectangle used for screenshot/template capture.
- `resolveNpcScanRegions(...)` now converts recommended regions once using the current bound window base.
- Tooltip template, yellow-name OCR, and purple player-anchor formula now receive resolved regions instead of recalculating `base + x/y` independently.
- `captureCleanNameRegionToMemory(...)` now captures with the resolved screen-absolute rectangle directly.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC visual work region semantics

Status: completed

Goal:

- Treat learned NPC click regions as visual work areas that can support both yellow target-name OCR and purple player-anchor formula OCR.

Changed files:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Updated the recommendation JavaDoc to describe visual work regions instead of tight OCR-only boxes.
- Replaced shrinking success-count-based ROI sizing with a fixed work-region sizing policy:
  - padding: `240 x 190`
  - minimum size: `520 x 360`
- Both policy-derived regions and click-sample-derived regions now use the same `npcVisionWorkRegion(...)` helper.
- This keeps the learned region broad enough to include the target yellow name and the current player's purple name after navigation moves the character near the target.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC OCR region recommendation cap

Status: completed

Goal:

- Keep NPC OCR region recommendations small enough to avoid repeated screenshot/OCR scans.

Changed files:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Fixed targets now return at most one learned/recommended OCR region plus the default masked full-window fallback.
- Roaming targets now return at most two learned/recommended OCR regions plus the default masked full-window fallback.
- The recommendation collector can still consider policy, sample, and legacy sources, but the returned list is capped before default is appended.
- Logs now include `learnedCandidates` and `maxLearned` so it is visible when many historical candidates were trimmed.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NPC OCR mask path cleanup

Status: completed

Goal:

- Make yellow NPC-name OCR and purple player-anchor OCR use the same default full-window mask rule.

Changed files:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Replaced the yellow-only `prepareYellowTargetScanImage(...)` helper with shared `prepareNpcOcrScanImage(...)`.
- Yellow target OCR and purple player-anchor OCR now both capture to `BufferedImage` first and use the same default-region mask decision.
- Purple player-anchor still writes the prepared image to a temp file before washing because `ImagePreprocessor.washPurpleTextToBlackAndWhite(...)` currently accepts file paths.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Position scan gateway cleanup

Status: completed

Goal:

- Reduce normal business use of `LocationVisionService.scanCurrentLocation()` so current-position reads go through the player state sync gateway.

Changed files:

- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `docs/ACTIVE_WORK.md`

Done:

- Changed `PlayerStateService.syncMyPosition()` into the central business entry for no-input current-position scans; it now returns the latest recognized location so no extra wrapper method is needed.
- Updated map navigation arrival checks, NPC first-shot debug, NPC player-anchor formula, and `GameStateUtil.confirmCurrentMap(...)` to use `syncMyPosition()`.
- Normal service/task code no longer directly calls `LocationVisionService.scanCurrentLocation()` outside `PlayerStateService`.
- Remaining direct calls are limited to debug/calibration helpers:
  - `debug/XiuluoAcceptBenchmarkMain`
  - `tools/AutoGridCalibrator`
  - `vision/PlayerNameOcrDebugService`

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Pause/stop movement detection checkpoint

Status: completed

Goal:

- Diagnose why pause/stop can feel slow after sleep consolidation.
- Fix the concrete slow checkpoint found in the latest log.

Changed files:

- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `docs/ACTIVE_WORK.md`

Findings:

- Latest log showed the UI command was pause, not stop.
- Four windows reached `TaskPauseToken` checkpoint quickly, but one Xiuluo window continued inside `GameStateUtil` movement detection for about six seconds before pausing.
- The slow path was the movement detector's coordinate sampling plus pixel fallback. It only checked thread interruption, not the current task pause/stop token.

Done:

- `GameStateUtil` now reads the current task context through `TaskExecutionContextHolder`.
- Coordinate movement detection and pixel fallback loops now call a shared movement checkpoint before/after waits and captures.
- Pause requests can now be observed inside movement detection instead of waiting for the whole detector to finish.
- Thread interruption inside movement detection now becomes a `TaskStopRequestedException`, so stop exits through the normal STOPPED task path.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 Task sleep utility consolidation

Status: completed

Goal:

- Stop duplicating small `Thread.sleep` / interrupt handling helpers in every task/service file.
- Give task waits one shared interrupt policy so stop/pause responsiveness is easier to audit.

Changed files:

- `src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java`
- `src/main/java/com/bot/dhxy/task/template/BaseTaskTemplate.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/GiveItemService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/service/TeamReturnService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/vision/MapSurveyService.java`

Done:

- Added `TaskSleep` as the shared task/service sleep helper.
- `TaskSleep.sleep(...)` returns false on interruption and always restores the interrupted flag.
- `TaskSleep.sleepOrStop(...)` checks the task context before and after sleeping, and throws `TaskStopRequestedException` when interrupted.
- Replaced duplicate local sleep helpers in the main task/navigation/dialog/NPC/item/team-return/vision flows.
- Follow-up scan tightened the rule: non-debug Java code now uses `TaskSleep` instead of local `Thread.sleep` helpers, including driver, tracker, input worker, focus, team role detection, movement detection, and standalone tool classes that compile with the main source set.
- Explicit `Debug*` classes and `debug` package experiments are left alone per user direction because they are temporary and may be deleted later.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-25 NPC smart-click learning record boundary

Status: implemented in `NpcClickService`; cleanup rule remains active

Decision:

- `NpcClickService.clickNpcSmart(...)` is the single production entry for clicking NPCs and task targets.
- Vision-memory learning for NPC/monster click behavior must be centralized behind this entry.
- Task code should only provide target facts through `NpcClickRequest`: map name, logical target coordinate, target name/keyword, roaming/fixed flag, and expected verification template.
- Internal strategies may differ, but they are implementation details of `clickNpcSmart(...)`:
  - task-tooltip template direct click;
  - learned direct click point;
  - yellow target-name OCR;
  - player-anchor coordinate formula;
  - Ctrl nearby-menu fallback.
- A strategy should return structured evidence to the `clickNpcSmart(...)` coordinator instead of writing learning data independently.
- The coordinator should be the only place that decides how to record:
  - successful click point samples;
  - failed or weak samples;
  - scan region used;
  - matched text/template rectangle;
  - actual clicked point;
  - verification strength and outcome.

Rationale:

- The project is converging on one click path for both NPC interaction and fixed/roaming monster interaction.
- If learning writes are scattered across yellow OCR, tooltip matching, formula clicks, and Ctrl probing, the JSON history becomes hard to trust and hard to debug.
- A single input point plus a single learning output point makes later ROI shrinking and direct-click learning inspectable.
- Template-based hits are valid learning evidence even when no OCR text was used, but only after the click verifies the expected dialog or battle state.

Implementation intent:

- Introduce or evolve an internal result shape similar to `NpcClickStrategyResult`.
- Each strategy should report, at minimum:
  - strategy/source name;
  - status;
  - window-relative scan region, if any;
  - matched rectangle, if any;
  - screen-absolute and window-relative reusable direct-click point, if any;
  - verification result;
  - diagnostic message.
- `clickNpcSmart(...)` should call a central recorder, for example `recordSmartClickEvidence(request, result)`.
- Verified results should write strong evidence:
  - `recordNpcClickAttempt(...)` for learned direct click points;
  - ROI policy/sample evidence when the strategy had a meaningful scan region and matched rectangle or click point.
- Unverified candidates may be recorded as weak/failure diagnostics, but must not become recommended click points.
- Existing older direct calls to `recordNpcClickAttempt(...)` or `recordNpcTargetOcrObservation(...)` from inside individual smart-click strategies should be treated as migration targets. They should either be removed or routed through the central `clickNpcSmart(...)` recording method.

Reusable click-point semantics:

- The stored click point means "a point that should be safe to left-click directly on the NPC/monster in a future run".
- It does not always equal the exact UI point physically clicked by the current strategy.
- Task-tooltip template path:
  - physically clicks the matched tooltip center to open the dialog;
  - records the reusable direct-click point as `tooltipCenter.x, tooltipCenter.y + 90`.
- Yellow-name OCR path:
  - records the same final direct left-click point used by the strategy.
- Player-anchor formula path:
  - records the same final formula direct left-click point used by the strategy.
- Ctrl-menu fallback:
  - physically clicks the yellow Ctrl-menu text candidate after the menu opens;
  - records the original Ctrl hover/probe point that caused the nearby menu to appear;
  - must not record the yellow menu text click point as the reusable NPC/monster point.

Boundary:

- This rule applies to NPC/monster click learning only.
- Other OCR diagnostics, map-label recognition, task-panel parsing, mini-map coordinate reading, and debug-only probes may keep their own records if they are not trying to learn NPC/monster click points.
- Debug mains may log or record temporary diagnostics, but they must not become a second production learning path.

Next owner guidance:

- Do not redesign `config/vision_memory.json` for this cleanup.
- Preserve the current JSON streams and append fields compatibly if needed.
- First clean up `NpcClickService` so all smart-click strategy evidence flows through one coordinator method.
- After that, update individual strategies one by one without changing their click order.

Current code status:

- `NpcClickService` now contains the first concrete internal structure:
  - `NpcClickStrategySource`;
  - `NpcClickStrategyStatus`;
  - `NpcClickStrategyResult`;
  - `recordSmartClickEvidence(request, result)`.
- The production `clickNpcSmart(...)` strategy pipeline now records through this single boundary:
  - task-tooltip template direct click;
  - learned direct click point;
  - yellow target-name OCR;
  - player-anchor coordinate formula;
  - Ctrl nearby-menu fallback.
- The old yellow/formula/learned/Ctrl strategy-local writes have been removed from the production smart-click path.
- Debug-only first-shot tooling may still write its own diagnostics; that is outside this production learning boundary.

Xieshuai/Solart/Humble review - smart-click recorder gates:

- Review status: implemented by He Li / compile passed.
- Risk confirmed:
  - `recordSmartClickEvidence(...)` must not write every non-skipped strategy result into `npcClickSamples`.
  - `OcrRoiMemoryService.recommendedNpcClickPoint(...)` rejects a learned direct-click point when the latest sample for the same target is not `clicked && success`.
  - Therefore a `NOT_FOUND` or pure `FAILED` sample can suppress an older good learned click point. In the current strategy order this can even happen inside one `clickNpcSmart(...)` call if an early tooltip miss is recorded before the learned-memory strategy runs.
- `npcClickSamples` must mean "real direct-click attempt result", not "any strategy result":
  - record `VERIFIED` when `clicked=true` and a reusable direct-click point exists;
  - record `CLICK_NOT_VERIFIED` only when `clicked=true` and a reusable direct-click point exists, because this is a real negative click sample;
  - do not record `NOT_FOUND`, pure `FAILED`, interrupted, screenshot-failed, OCR-miss, or Ctrl-scan-exhausted results into `npcClickSamples`.
- Suggested click-sample gate:

```java
boolean shouldRecordClickSample =
        result.clicked()
                && result.clickPointAbs() != null
                && result.clickPointRel() != null;
```

- ROI evidence must not be polluted by Ctrl-menu text:
  - `CTRL_MENU` can write a verified click sample using the original Ctrl probe/hover point as the reusable point;
  - `CTRL_MENU` must not call `recordNpcTargetOcrObservation(...)`, because its matched rectangle belongs to the Ctrl popup menu, not the in-scene NPC/monster yellow name.
- Suggested ROI-evidence gate:

```java
boolean shouldRecordRoiEvidence =
        result.source() != NpcClickStrategySource.CTRL_MENU
                && result.scanRegion() != null
                && (result.matchedRect() != null
                    || result.clickPointRel() != null
                    || result.source() == NpcClickStrategySource.YELLOW_TARGET_OCR);
```

- `YELLOW_TARGET_OCR` misses may still be recorded as ROI/target observations with `matched=false, verified=false` so repeated misses can mark the policy stale; they must not become direct-click samples.
- `TASK_TOOLTIP_TEMPLATE` verified results may provide visual cue evidence, but tooltip-not-found should normally remain a log-only miss and must not suppress learned direct-click points.
- Strong verification compatibility:
  - The migrated central recorder writes `verificationStrength="DIALOG_TEMPLATE"` for verified smart-click results.
  - `OcrRoiMemoryService.hasStrongNpcVerification(...)` historically recognized `DIALOG_OPTION` or `actualClickMeasured=true`.
  - Next owner should either make `hasStrongNpcVerification(...)` recognize `DIALOG_TEMPLATE`, or have the recorder write the legacy `DIALOG_OPTION`. Prefer recognizing `DIALOG_TEMPLATE` because it preserves the new semantics.

He Li implementation note:

- `recordSmartClickEvidence(...)` now writes `npcClickSamples` only when the strategy actually clicked and provides both screen-absolute and window-relative reusable click points.
- `NOT_FOUND`, pure `FAILED`, interrupted, screenshot/OCR/template misses, and exhausted Ctrl scans no longer write direct-click samples, so they cannot suppress an older good learned point through the latest-sample gate.
- ROI evidence now skips `CTRL_MENU`; Ctrl popup text rectangles are not fed into scene-level NPC/monster ROI learning.
- Yellow OCR misses may still write ROI/target observations, allowing repeated misses to stale the ROI policy without becoming learned direct-click samples.
- `OcrRoiMemoryService.hasStrongNpcVerification(...)` now treats `DIALOG_TEMPLATE` as strong verification alongside the legacy `DIALOG_OPTION`.

### Xieshuai - 2026-05-25 vision memory JSON schema decision

Status: decided

Decision:

- Keep the current `config/vision_memory.json` structure for now. Do not split or migrate it just to separate OCR ROI policy from raw observations.
- Reason: the file is not only for shrinking OCR regions. It is also the shared historical vision memory for:
  - OCR attempts and matched text rectangles;
  - player-name anchor samples;
  - NPC/monster target coordinates;
  - actual/predicted mouse click points;
  - camera/scale-related context;
  - verification outcomes that later decide whether a point can be trusted.
- Future learning should derive policy from the existing sample streams instead of discarding or reshaping them prematurely.
- If a future derived model becomes large or difficult to inspect, add a separate derived-policy file or section while preserving the existing raw sample schema and data.

Rule for agents:

- Do not propose a JSON schema migration for `vision_memory.json` unless the user explicitly reopens this decision.
- Add fields compatibly when needed, but preserve existing top-level streams and historical samples.

### He Li - 2026-05-25 Yellow target candidate contract

Status: implemented by Xie Shuai

Context:

- Xiuluo/NPC smart click should keep useful visual evidence even when exact yellow-name OCR does not match the target name.
- The next Ctrl-menu fallback should probe around high-confidence physical candidate points instead of only probing the window center.
- Another agent may implement the yellow-text candidate extraction; this section defines the expected return shape.
- The first production implementation now lives in `GameTextLineOcrService` and is consumed by `NpcClickService.clickNpcSmart(...)`.

Current implementation:

- Yellow washing/candidate extraction:
  - Service: `GameTextLineOcrService`.
  - API: `findYellowTextCandidateResult(BufferedImage raw, Path washedPath, Path overlayPath)`.
  - Convenience API: `findYellowTextCandidates(BufferedImage raw, Path washedPath, Path overlayPath)`.
  - Return: `TextCandidateScanResult`, whose `candidates()` list is immutable and sorted by score descending.
  - Candidate coordinates are image-local to the supplied screenshot or cropped scan image.
- Formal NPC click integration:
  - Service: `NpcClickService.clickNpcSmart(...)`.
  - Exact target path still tries `GameTextLineOcrService.findYellowTarget(...)` first.
  - If exact target OCR does not match, `NpcClickService` calls `findYellowTextCandidateResult(...)`, converts ranked candidates to screen-absolute points, and appends them to the Ctrl-menu probe origins.
  - These fallback candidates are not left-clicked directly; they are only used as Ctrl probe origins.
- Current yellow mask behavior:
  - Keeps sampled NPC yellow strokes including dark edge pixels such as `94,94,18`, `109,109,16`, `126,126,14` and bright pixels such as `213,213,5`, `253,253,50`, `251,253,77`, `248,250,158`.
  - Rejects the stall/vendor gold family around `203,181,88..106` with the characteristic red-green separation.
  - Penalizes high/skinny fragments, tiny fragments, and weak sparse blobs so non-text crumbs do not rank above real NPC-name text.

Recommended API shape:

- Do not return `Queue` or `Stack`.
- Return a result object that owns an already sorted immutable `List`.
- The list represents scored visual candidates, not a mutable work queue.

Suggested records:

```java
public record YellowTextCandidate(
        Point textCenterAbs,
        Point clickPointAbs,
        OcrWindowRegion textRectAbs,
        double score,
        String sourceText,
        String reason
) {}

public record YellowTargetScanResult(
        YellowTargetMatchStatus status,
        Point matchedClickPointAbs,
        List<YellowTextCandidate> fallbackCandidates
) {
    public List<Point> fallbackClickPoints() {
        return fallbackCandidates.stream()
                .map(YellowTextCandidate::clickPointAbs)
                .toList();
    }
}

public enum YellowTargetMatchStatus {
    TARGET_MATCHED,
    TARGET_NOT_FOUND_WITH_CANDIDATES,
    TARGET_NOT_FOUND,
    SCAN_FAILED
}
```

Contract:

- `fallbackCandidates` must be sorted by `score` descending before returning.
- Limit fallback candidates to the best 2 by default; best 3 is acceptable if diagnostics show it helps.
- `clickPointAbs` must be the actual point the yellow-target strategy would click after applying its vertical/target offset, not merely the yellow text center.
- The extractor should reject blobs that do not look like text. Use shape/quality filters such as minimum pixel count, width/height bounds, aspect ratio, connected-component sanity, and line-like text structure.
- Yellow background, skin, effects, or large decorative blobs must not become candidates.
- `TARGET_MATCHED`: exact/fuzzy target name matched; caller should try `matchedClickPointAbs` first and may also add it to Ctrl probe origins if the click does not verify.
- `TARGET_NOT_FOUND_WITH_CANDIDATES`: no target-name match, but text-like yellow candidates exist; caller should not left-click them blindly, only add their `clickPointAbs` values to Ctrl probe origins.
- `TARGET_NOT_FOUND`: scan succeeded but no usable target or fallback candidate exists.
- `SCAN_FAILED`: screenshot/OCR/washing failed or the result is untrustworthy; caller should not add fallback candidates from this scan.

Integration intent:

- `NpcClickService` should collect Ctrl probe origins from prior evidence:
  - yellow exact matched click point when it fails verification;
  - yellow fallback candidate click points;
  - player-name formula point;
  - purple-blob formula fallback point;
  - learned/previous attempted points if available;
  - window center only as the final fallback.
- Ctrl probing should iterate this de-duplicated ordered point list, then apply `DENSE_BLIND_OFFSETS` around each origin.

### Tangde - 2026-05-25 settings page game-config cleanup

Status: implemented

Changed:

- Removed the duplicate Window Registration block from the JavaFX Settings tab; window scan/register remains owned by the Main tab.
- Settings now focuses on in-game configuration:
  - editable task run-count fields for 修罗、五倍、天庭、抓鬼, with 五环 constrained to a 1/2 dropdown;
  - summon third-skill maintenance enable switch and minute interval dropdown;
  - existing supply thresholds.
- Added shared `BotProperties` fields and default `application.properties` entries for the new game task count settings, so future task implementations can consume one central config object.
- Changed 主控 role detail from an overlay to a real right-side layout panel, so opening details no longer covers table columns/text.
- Restored the top bar as a global root-level header so all tabs keep the same structure; removed the shell's forced 640px min-height so the main content does not overflow upward over the header in small/short windows.
- Retuned dark mode toward a Codex/GitHub-like black-gray palette and added explicit dark overrides for buttons, text fields, combo boxes, tables, lists, and task tiles to avoid black-on-black or overly bright blue areas.

### Xieshuai - 2026-05-25 Xiuluo Alt+1 Maven/IntelliJ benchmark rerun

Status: diagnostic / verified

Changed:

- Added IntelliJ Application run configs for the packaged benchmark main:
  - `XiuluoAcceptBenchmarkMain - WindowMessageAlt1`
  - `XiuluoAcceptBenchmarkMain - MiniMapProbe`
- Both configs run `com.bot.dhxy.debug.XiuluoAcceptBenchmarkMain` with project Make enabled, `$PROJECT_DIR$`
  as the working directory, and UTF-8 JVM output flags.

Rerun findings:

- Maven compile/classpath preparation passed with:
  `mvn -q -DskipTests compile dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"`.
- Running the same main class as IntelliJ would launch, with
  `-Dxiuluo.benchmark.onlyWindowMessageAlt1=true`, selected the correct bound window.
- `-Mode windowMessageAlt1` selected the correct bound window `hwnd-1E0DEC` / handle `1969644`.
- The JavaFX experiment service path still reports `posted=false` for `Alt+1` when launched from the current non-elevated process.
- This confirms the current blocker is process integrity/permission, not a dead runner or broken packaged main.
- If IntelliJ is launched as administrator, use the new IntelliJ configs directly to compare elevated vs non-elevated behavior.

### He Li - 2026-05-25 no-UI background input benchmark alignment

Status: diagnostic / current finding

Goal:

- Make the no-UI Xiuluo accept benchmark follow the same window/input shape documented by the previous agents.
- Verify whether current `Alt+1` background keyboard failure is a benchmark mistake or a real Win32 message failure.

Changed:

- `tools/XiuluoAcceptBenchmarkRunner.java` now splits the minimap probe into:
  - pure `Alt+1 + sleep` queue requests, so `InputActionWorker` can use the formal HWND-background keyboard path;
  - one focused real mouse `moveMouse + clickLeft` sequence for the minimap coordinate click.
- The benchmark also has `-Dxiuluo.benchmark.onlyWindowMessageAlt1=true`, which directly calls the same `WindowMessageInputExperimentService.postAlt1(...)` used by the JavaFX `后台按键 Alt+1` button.
- `BoundWindowKeyboardService` now logs per-message `PostMessage` results and `Native.getLastError()` for failed HWND shortcuts.

Current finding:

- The selected game window was registered and bound correctly: `windowId=hwnd-1E0DEC`, `hwnd=1969644`.
- HWND screenshots still work through `HWND_BITBLT`.
- The JavaFX-experiment service path itself currently reports `posted=false` for `Alt+1`.
- The formal `BoundWindowKeyboardService` also fails all four `WM_SYSKEY*` messages with `lastError=5` (`ERROR_ACCESS_DENIED`).
- Therefore the current failure is not caused by the benchmark skipping the existing experiment path. It is a real Win32 message permission/integrity issue for the current process/window state.
- The input worker fallback still focuses the bound window and sends the real `Alt+1`, so the task can continue, but it will not be background-only in this state.
- Process token check confirmed the mismatch:
  - game process `xy2_tab_x64 pid=10500`: elevated/high integrity (`S-1-16-12288`);
  - normal PowerShell/Java process: medium integrity (`S-1-16-8192`).
- Running the same no-UI probe elevated confirms the old conclusion still holds when integrity levels match:
  - `WindowMessageInputExperimentService.postAlt1(...)` reports `posted=true`;
  - formal `BoundWindowKeyboardService` reports `Alt+1 result=true`, all four `PostMessage` calls have `lastError=0`;
  - no fallback focus was needed for pure Alt+1 requests.

Helper:

- `tools/RunXiuluoWindowMessageAlt1Probe.ps1`
  - `-Mode windowMessageAlt1`: run the JavaFX-experiment service path without the JavaFX UI.
  - `-Mode miniMapProbe`: run the formal input queue path where pure Alt+1 uses background HWND keyboard and minimap click uses focused real mouse input.
  - Intended to be launched with `Start-Process -Verb RunAs` when comparing against an elevated game client.

Next diagnostic direction:

- If the game client is elevated and we want background keyboard, run the Java/IDE/Codex process elevated too.
- If the Java process remains medium while the game is high, expect HWND keyboard to fail with error 5 and fall back to focused real input.

### Tangde - 2026-05-24 matcher internal comments

Status: implemented

Changed:

- Added internal section comments to matcher/recognition logic so reviewers can follow each stage without reverse-engineering the loops.
- Covered mini-map coordinate and map-label matching in `MiniMapCoordinateReader`.
- Covered objective map-name and coordinate matching in `ObjectiveTextRecognitionService`.
- Covered shared image/template matching internals in `ImageFinder`.
- Covered map survey map-label matching handoff in `MapSurveyService`.

Scope:

- Comments only. No business logic, thresholds, provider order, OCR behavior, or input behavior changed.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 agent code documentation rule

Status: documented

Changed:

- Added a mandatory code documentation rule to `AGENTS.md`.
- Future code changes must include production-grade comments/JavaDoc for new or modified public APIs, complex private helpers, business decisions, fallback chains, threading/input behavior, native-window handling, OCR/template matching, debug paths, configuration switches, and persisted data formats.
- Method JavaDoc must document what the method does, every parameter, return/failure semantics, side effects, and safety assumptions. Long methods must also use internal block comments to explain each meaningful stage.
- Comments should explain intent, assumptions, edge cases, invariants, and safety constraints. Low-value comments that merely restate code are not acceptable.
- Agents touching undocumented code should add comments for the touched logic instead of leaving it undocumented.

### He Li - 2026-05-24 unified NPC Ctrl-menu click contract

Status: implemented

Changed:

- Replaced the separate Xiuluo Ctrl-click paths with one `NpcClickService.clickNpcByCtrlMenuScan(targetKeyword, npcTagTemplatePath, expectedDialogTemplatePath)` entry.
- The unified Ctrl path tries `(NPC)` template candidates first, then falls back to OCR keyword matching.
- `NpcClickService` only clicks the NPC/menu candidate and verifies that the expected option-dialog template is visible. It no longer clicks the task option itself.
- Xiuluo now passes `修罗`, `images/template/npc/npc_tag.png`, and `images/template/dialog/xiuluo/xiuluo_enter_battle_kanda.png`.
- Wuhuan `clickNpcSmart(...)` now receives `images/template/dialog/wuhuan/wuhuan_accept_first_option.png` as the expected accept-dialog template.
- Removed the public old split methods `clickNpcByCtrlMenuKeyword(...)` and `clickNpcByCtrlMenuNpcTagCandidates(...)`.
- Added `DialogService.isGreenTemplateOptionVisibleDirectForExclusive(...)` so NPC-click code can verify a business dialog without clicking its option.

Behavior:

- Dialog option clicking remains owned by the task/DialogService flow.
- Wuhuan still clicks the accept option in its accept transaction.
- Xiuluo still clicks `看打!` through `tryConfirmEnterBattleDialog(...)`.
- The Ctrl-menu service is now a click-and-verify helper, not a task-progress helper.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 sync position provider cascade

Status: implemented

Goal:

- Make `syncMyPosition` prefer the fastest local source before falling back to OCR/cloud.
- Keep Baidu OCR as the last fallback only, not the default first choice.

Changed:

- `LocationVisionService.scanCurrentLocation()` now resolves location in this order:
  1. `MINIMAP_TEMPLATE`: read mini-map coordinate digits with local templates, then recognize the cleaned map-label image against `images/template/map_label`.
  2. `LOCAL_OCR`: if template location fails, capture the coordinate strip and parse it with the local OCR sidecar only.
  3. `BAIDU_OCR`: if local OCR cannot produce a valid map/coordinate, call Baidu OCR as the final fallback.
- `MiniMapCoordinateReader` now exposes `readCurrentTemplateLocation()`, returning map name, coordinate, template score, and the saved clean label debug image path.
- No new matching service was added. The existing mini-map map-label template logic is now exposed as `recognizeMapLabelImage(...)`, and `MapSurveyService` reuses it instead of keeping a duplicate private matcher.
- `TextRecognizer` now exposes `parseLocationLocalOnly(...)` and `parseLocationBaiduOnly(...)` so location fallback order is explicit and not hidden inside provider config.

Runtime logs to check:

- `[location] selected provider=MINIMAP_TEMPLATE ... templateElapsedMs=...`
- `[location] selected provider=LOCAL_OCR ... localElapsedMs=...`
- `[location] selected provider=BAIDU_OCR ... baiduElapsedMs=...`
- `[ocr-location] provider=local-only ...`
- `[ocr-location] provider=baidu-only ...`

Expected behavior:

- Template/minimap should usually be fastest because it is local image/template matching and does not call OCR/network.
- If map-label templates are missing or score is too low, the chain falls through automatically; it should not block `syncMyPosition`.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed.

### Tangde - 2026-05-24 vision memory auto data capture

Status: implemented

Goal:

- Store the data needed for the two-step vision plan without requiring manual bookkeeping:
  1. shrink OCR scan regions from full masked window toward learned ROI/edge/center regions;
  2. later learn enough NPC click samples to click known NPC coordinates without OCR.

Changed:

- `OcrRoiMemoryService` now stores three sample streams in `config/vision_memory.json`:
  - `ocrAttempts`: every keyed masked-window/ROI OCR attempt, including region type, scan region, target text, matched text rectangle, word count, success/failure, and message.
  - `playerAnchorSamples`: existing player-name anchor samples with map coordinate, anchor, center delta, and camera state.
  - `npcClickSamples`: NPC first-shot prediction/click samples, including current map coordinate, target NPC/name/coordinate, player anchor, predicted/actual click point relative to the game window, tune values, formula version, click outcome, and verification signal.
- `OcrWindowScanService` now records both learned-ROI attempts and full-masked-window fallback attempts.
- `NpcClickService.clickNpcSmart(...)` now records the first-shot prediction result after the move+click verification, and also records skipped first shots when current location or player anchor is missing.
- `NpcClickService.clickNpcSmart(...)` no longer accepts caller-provided OCR regions in the formal request. NPC OCR regions are resolved through `OcrRoiMemoryService.recommendNpcClickRegions(...)` so old hardcoded task/window rectangles cannot silently re-enter the production click path.
- `clickNpcSmart(...)` now tries yellow target-name OCR first with `GameTextLineOcrService.findYellowTarget(...)`, then falls back to the old purple self-name anchor + coordinate formula, and uses Ctrl-menu dense scan last.
- The three NPC click strategies are split into independent methods: `clickNpcByYellowTargetName(...)`, `clickNpcByPlayerAnchorFormula(...)`, and `clickNpcByCtrlMenuScan(...)`. `clickNpcSmart(...)` is now just the default ordered composition.
- `NpcClickService.debugClickNpcSmartFirstShot(...)` records the debug first-shot point as an unverified debug sample.

Behavior:

- This is record-only. It does not change the NPC click formula, the Ctrl-probe fallback, dialog handling, or task business logic.
- The data is now sufficient to start implementing ROI shrinking policy and later NPC direct-click learning on top of `vision_memory.json`.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-24 local OCR / vision memory review

Status: review notes for Tangde / Xie Shuai

Current decision:

- Stop using the current map-survey projection/interpolation algorithm as the main solution for player/NPC screen-point estimation.
- Move the next experiment direction to local OCR plus saved vision observations.
- Treat `config/vision_memory.json` as the main raw-data memory for OCR and future learning.

Review after reading `docs/ACTIVE_WORK.md`, `docs/LOCAL_OCR_EXPERIMENT.md`, `OcrRoiMemoryService`, and `OcrWindowScanService`:

1. The general direction looks right.
   - `vision_memory.json` is record-only for now, which is important.
   - It should not immediately change business behavior until we have enough real samples and can verify accuracy.
   - Storing OCR attempts, player-name anchor samples, and NPC click samples in one place is reasonable for the current experiment phase.

2. Please keep raw observations separate from learned policy.
   - Current `MemoryEntry` stores both samples and `recommendedRoi`.
   - That is okay short term, but long term we should mentally separate:
     - raw evidence: OCR attempts / anchor samples / click samples;
     - derived policy: recommended ROI / future click model.
   - If a learned ROI becomes bad, we need to be able to clear or recompute policy without losing raw samples.

3. `saveMemory(...)` should eventually use safe writes.
   - Current `OcrRoiMemoryService.saveMemory(...)` writes directly to `config/vision_memory.json`.
   - This data will become expensive to recreate after many OCR/click samples.
   - Recommendation: mirror the safer config-write style used elsewhere: write a sibling temp file, then atomic move/replace.

4. Provider/preprocess identity should be explicit in saved data.
   - The local OCR experiment will mix local-only, compare, Baidu, masked-window, learned-ROI, and segmented-center paths.
   - Current samples store `source`, `purpose`, and `regionType`, but we should make sure every OCR attempt can answer:
     - OCR provider: local / baidu / compare-returned-baidu / hybrid-local / hybrid-fallback;
     - preprocessing variant: full masked window / learned ROI / segmented purple line / segmented yellow line / task panel crop;
     - debug image paths or stable image ids for replay.
   - Without this, good and bad samples from different pipelines may get blended under the same key.

5. Memory keys may need stronger namespacing.
   - `player-name|<name>` is useful, but can become too broad if the same role name, server, task, or window layout differs.
   - NPC click keys should also stay task/map/NPC/target specific.
   - Suggested key dimensions where available:
     - purpose/task;
     - server/player name/player id;
     - map name and map coordinate;
     - target text or target NPC;
     - window size;
     - OCR/preprocess path.

6. NPC click samples should not treat normal-run `actualClick` as ground truth unless it was measured.
   - In normal first-shot flow, `actualClickAbs` may be the same point we predicted/clicked, not an independently verified true NPC point.
   - The useful supervision signal is `success + verification`.
   - If we later train/derive a click correction model, we should only use samples with a strong verification signal or explicit manual measurement.

7. Player-anchor samples need enough context to debug OCR mistakes.
   - The current fields `mapName`, `mapX/Y`, `anchor`, `anchorDelta`, `cameraState`, matched text/fragment/mode/score are good.
   - I would also keep/record the image path or image hash for the sample if possible, because OCR mistakes are hard to reason about from coordinates alone.
   - If the minimap coordinate read is unstable, store that confidence/source too, so wrong map coordinates do not pollute later analysis.

8. Retention policy is okay for logs, but training data may need a protected subset.
   - `MAX_OCR_ATTEMPTS = 1000`, `MAX_GLOBAL_SAMPLES = 600`, `MAX_NPC_CLICK_SAMPLES = 600` is fine for rolling diagnostics.
   - If we manually validate high-value samples later, they should not be trimmed away with ordinary rolling attempts.
   - Consider a future `acceptedSamples` / `pinnedSamples` section or a separate curated file.

9. Current local config check:
   - I only see `config/ocr_roi_memory.json` locally right now, not `config/vision_memory.json`.
   - That may simply mean the new path has not been run yet.
   - First validation should confirm the new file is created and contains the three expected top-level streams.

Recommended next steps:

1. Run local OCR debug enough times to generate real `vision_memory.json` samples.
2. Verify the file has reproducible sample context: provider, preprocessing path, crop/region, map/coord, matched text, score, image path/id.
3. Add safe-write for `vision_memory.json` before collecting lots of manual data.
4. Keep this record-only until we have enough sample volume and can inspect false positives/false negatives.

### Tangde - 2026-05-24 response to He Li vision-memory review

Status: implemented

Accepted review points:

- Kept `vision_memory.json` record-only. No OCR ROI policy or NPC click model is used by business logic yet.
- Added safe-write for `config/vision_memory.json`: write sibling temp file first, then atomic move when supported, falling back to replace-existing move.
- Added explicit OCR sample context:
  - `provider`
  - `preprocessVariant`
  - `rawPath`
  - `maskedPath`
  - `overlayPath`
  - `roiPath`
- Added explicit player-anchor context:
  - `provider`
  - `preprocessVariant`
  - `imagePath`
  - `secondaryImagePath`
  - `locationSource`
- Added NPC-click supervision clarity:
  - `actualClickMeasured`
  - `actualClickSource`
  - `verificationStrength`
  - Normal first-shot samples now mark `actualClickMeasured=false`; they should be used as "prediction + verification result", not as an independent true NPC point.
- Exposed `TextRecognizer.currentProviderName()` so OCR sample records can distinguish configured provider paths. In compare mode, masked-window samples label provider as `compare-returned-baidu` because the returned business result is Baidu while local is only logged for comparison.

Data sufficiency conclusion:

- For step 1, shrinking OCR regions, the stored data is now enough to implement policy later: attempts include key, target, provider, preprocessing variant, scan region, matched text rectangle, success/failure, image paths, and rolling recommended ROI.
- For step 2, future NPC direct-click learning, the stored data is now enough to start learning safely: samples include current map coordinate, target NPC/map coordinate, player anchor, predicted click point, formula version, tune values, and verification outcome. Strong model training should still filter for `verificationStrength` and avoid treating unmeasured click points as ground truth.

Remaining future-only items:

- If we later manually validate high-value samples, add a curated/pinned section so those samples are never trimmed by rolling retention.
- Per the 2026-05-25 schema decision above, do not split or migrate `vision_memory.json` now. If raw/policy separation becomes cumbersome later, add a compatible derived-policy layer while preserving existing raw sample streams and historical samples.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 masked full-window OCR and ROI memory

Status: implemented / debug entry connected

Goal:

- Start the OCR-region plan with a safe baseline: capture the full 1024x768 game window, mask UI areas that should not be sent to OCR, then run OCR on the masked image.
- Add a lightweight ROI memory layer so repeated successful detections can prefer a smaller learned region before falling back to the full masked window.

Default masked-out relative regions:

- `0,0 -> 258,200`
- `0,0 -> 1024,54`
- `768,58 -> 1020,160`
- `4,735 -> 706,768`
- `710,700 -> 1024,768`

Changed:

- Added `OcrWindowRegion`.
- Added `OcrWindowScanService`.
  - Captures the current bound game window using current tracker base.
  - Writes raw and masked debug images through `WindowScopedTempPath`.
  - Also writes `*_mask_overlay.png`; red areas are masked out, blue area is the learned ROI if one exists.
  - If ROI memory exists for the key, scans that ROI first.
  - If the ROI scan misses the target text, falls back to the full masked window.
- Added `OcrRoiMemoryService`.
  - Upgraded the memory file to `config/vision_memory.json`; if only the old `config/ocr_roi_memory.json` exists, it is read as a legacy source and future writes go to `vision_memory.json`.
  - Recomputes a recommended ROI from recent successful samples.
  - Keeps the first version intentionally conservative: learned ROI only narrows the first attempt; full-window masked OCR remains the fallback.
  - Successful player-anchor samples now store map name/coordinate, anchor point, text rectangle, OCR source, matched text/fragment/mode, score, window size, center point, `anchorDelta`, and a coarse `cameraState`.
- Connected the vision-memory write to `PlayerNameOcrDebugService`.
  - The current debug button path uses the segmented/enhanced center-crop OCR result as the player-name anchor source.
  - `OcrWindowScanService` and masked full-window ROI memory are available as the next fallback/integration point, but the current button does not rely on it as the primary anchor path.
  - Successful player-name anchors are recorded under key `player-name|<name>`.
  - The debug result now reports structured anchor output:
    - `segmentedMatch`: center segmented-enhanced OCR result.
    - `selected`: the match actually used for the anchor.
    - `anchorSource`: currently `SEGMENTED_CENTER` or `NONE`.
    - Each match includes anchor point, matched text, text rectangle, fragment/mode, and score.
  - On successful name-anchor recognition, `PlayerNameOcrDebugService` now also reads the current mini-map coordinate under the selected window context and writes a full vision-memory sample.

Current scope:

- This does not replace formal NPC/menu/dialog/task OCR yet.
- Next integration candidates are NPC first-shot player-anchor OCR and target/NPC text OCR after the debug path proves stable.
- The current `cameraState` is a coarse screen-center delta classification (`CENTERED`, `LEFT`, `RIGHT`, `UP`, `DOWN`, or combined). It is meant as raw training data, not the final camera model.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 live window geometry refresh

Status: completed

Goal:

- Keep backend window base coordinates synchronized after the user drags a registered game window.
- Avoid continuing to use stale `WindowNativeBinding` geometry after hwnd position changes.

Changed:

- Added `WindowNativeBindingRefreshService`, a no-focus/no-input helper that reads the current hwnd rect through `IsWindow + GetWindowRect`.
- Added `WindowNativeBinding.withGeometry(...)` and `hasSameGeometry(...)`.
- `MultiWindowTaskManager` now refreshes live native geometry before task submission and before producing UI/system snapshots.
- `GameClientTracker` now refreshes the current bound window geometry before updating `windowBaseX/windowBaseY`, instead of trusting the old stored binding.
- `TaskWindowRuntimeService` now returns a refreshed binding when it resolves task-window runtime geometry.

Expected behavior:

- If the game window is moved while still registered, the UI Base column should update on the next UI refresh.
- Subsequent screenshots/click coordinate calculations should use the moved window position.
- If the hwnd is gone or has no live rect, task submission fails as stale binding instead of running against the old coordinates.

Known limitation / follow-up:

- Moving a game window while a task is actively clicking or reading coordinates is not treated as a synchronized operation yet.
- Current behavior is poll/use-time refresh: after the drag settles, the next UI refresh or backend coordinate refresh should pick up the new geometry.
- Future optimization can pause or debounce per-window task execution while geometry is changing, then resume after the window rect is stable for a short period.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-24 local OCR direction and name-anchor debug

Status: active experiment / user testing

Goal:

- Validate local OCR accuracy before replacing Baidu OCR in normal task flows.
- Start with the player-name / name-anchor use case because Wuhuan already uses OCR name fragments to estimate the character screen anchor.
- Keep the test easy to run from the main page.

Current decision:

- Use the local RapidOCR sidecar as the preferred experiment path for new OCR validation.
- Do not globally replace Baidu OCR yet.
- Current OCR validation uses `bot.dhxy.ocr.provider=hybrid`: business flows try local OCR first, and target-matching OCR paths should retry Baidu when local text does not match the expected target.
- Debug-only local OCR calls may bypass provider routing and use the local sidecar directly.

Changed:

- Added `scripts/local_ocr_server.py` and `scripts/requirements-local-ocr.txt`.
- Added `docs/LOCAL_OCR_EXPERIMENT.md` with install/start/provider-mode notes.
- Added `TextRecognizer.getAllTextResultsLocalOnly(...)` for debug-only local OCR.
- Added `PlayerNameOcrDebugService`.
- Added main-page button `本地OCR测名字`.

How the name debug works:

- Select exactly one registered window on the main page.
- Click `本地OCR测名字`.
- The debug focuses the selected game window, waits briefly, captures the bound HWND, crops a larger center region, saves raw and washed images, runs local OCR on the washed image, and logs detected words plus relative/absolute anchor coordinates.
- Images are written to `images/temp/player_name_ocr/<windowId>/latest_raw.png` and `latest_washed.png`.

Next:

- User will run the main-page debug button and inspect whether local OCR finds enough of the player name despite special symbols.
- If local OCR is accurate enough, consider switching specific OCR-heavy debug paths to `local` or normal flows to `hybrid`.

### He Li - 2026-05-23 map survey UI for map labels and camera bounds

Status: implemented / needs user calibration samples

Goal:

- Add a UI-assisted long-term map survey path for replacing fragile OCR/player-anchor guesses.
- Reuse one UI map name for both minimap-label template sampling and camera-bound recording.
- Keep this as an explicit debug/calibration action, not normal task startup behavior.

Changed:

- Added `MapSurveyService`.
- Added main task-selector buttons:
  - `保存地图名样本`
  - `测试地图名`
  - `记左边界`
  - `记右边界`
  - `记上边界`
  - `记下边界`
  - `测角色点`
- The buttons all use the existing `地图校准名` input.
- Minimap map-label samples are saved under `images/template/map_label/<地图名>.png`.
- Camera-bound samples are saved into `config/map_camera_bounds.json`.
- `MiniMapCoordinateReader` now exposes public helpers to extract a clean map-label image and to read a location snapshot from an already captured minimap strip.

How to use:

- Select exactly one registered window.
- Enter the map name in `地图校准名`.
- For map-label recognition:
  - click `保存地图名样本`;
  - click `测试地图名` to verify current minimap label matches the saved template.
- For camera bounds:
  - walk to the map's left/right/top/bottom camera edge;
  - place the mouse on the character body/feet;
  - click the corresponding boundary button.
- After all four boundaries are recorded, `测角色点` reads the current minimap coordinate and estimates the character's screen-relative point.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 task pause checkpoints in navigation

Status: completed

Goal:

- Make pause/stop requests reach long-running navigation detection loops promptly.
- Use a task execution context holder so deep services can checkpoint without widening business method signatures.
- Keep real input sequences atomic and avoid pausing inside input worker callbacks.

Owns:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContextHolder.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`

Avoids:

- Changing FiveRing/Xiuluo business flow.
- Changing movement/dialog detection thresholds.
- Inserting pause waits inside atomic input queue callbacks.

Changed:

- Added `TaskExecutionContextHolder` as a task-thread `ThreadLocal` holder with `checkpointIfPresent()`.
- `WindowTaskRunner` now binds each task execution context around startup initialization and task execution.
- `MultiWindowTaskManager` injects the holder into each runner.
- `NavigationService` now checkpoints pause/stop in long-running navigation and mini-map pathing confirmation loops:
  - before/after combat-state polling;
  - before/after movement/dialog/location detection;
  - after long sleeps;
  - between retry attempts.
- Checkpoints are intentionally not added to the generic sleep helper or inside input worker callbacks.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 UI map transform calibrator

Status: implemented / needs user calibration test

Goal:

- Add an explicit UI-triggered debug task for writing missing mini-map transforms such as `瑶池` into `config/maps.json`.
- Keep calibration manual and safe: no automatic clicks, no normal task behavior changes.

Changed:

- Added task type `debug_map_calibrator` / `地图校准`.
- Added `DebugMapCalibratorTask`, which reads the map name from UI runtime config, waits for two stable mouse points, OCRs coordinate candidates from a full-window debug capture, picks the candidate nearest the mouse, calculates `CoordinateHelper.MapTransform`, and writes `config/maps.json`.
- Added a `地图校准名` input on the task selector UI. The value is synced into `BotProperties.debugMapCalibratorMapName` before task start.
- Skipped normal startup initialization for `debug_map_calibrator`, so map-tracking setup / Alt+6 prep does not disturb a manually prepared calibration screen.

How to use:

- Select one bound window.
- Enter the target map name, e.g. `瑶池`, in `地图校准名`.
- Select `地图校准` and start.
- Open/prepare the map in-game, place the mouse on point A until the task beeps/logs success, then move to point B and hold again.
- Pick two points whose logical X and Y both differ.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 main refresh scans game windows

Status: completed

Goal:

- Make the main page refresh button perform real game-window scan/register instead of only repainting the table.
- Give visible UI feedback while scanning and after scan results return.
- Keep start-button scan/start behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing task start assignment behavior.
- Changing window discovery service behavior.

Changed:

- Main page refresh button now calls `scanAndRefreshGameWindowsFromMain()` instead of only repainting the table.
- The refresh action runs `GameWindowRegistrationService.registerDetectedGameWindows(...)`, so it scans real game windows and updates registrations/bindings.
- The UI now logs and shows an action hint immediately while scan is running.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 Xiuluo hot-start and task takeover consensus

Status: active design / next implementation target

Goal:

- Make Xiuluo leader task startup/takeover robust from real in-game states, not only from a clean empty screen.
- Let other agents share the same Xiuluo assumptions before touching UI, framework, or dialog code.
- Keep Wuhuan's existing hot-start path separate unless the user explicitly asks to merge it.

Owner:

- He Li owns Xiuluo task takeover, Xiuluo dialog/template flow, and Xiuluo task-state transitions.

Other-agent handoff:

- Startup visibility prep should keep map-tracking setup and Alt+6 visibility confirmation before task navigation.
- After Alt+6 visibility is confirmed, wait about 1s before dialog detection so the floating "hide players" toast can disappear.
- `NavigationService.navigateToNPC(...)` should not run generic UI cleanup if a dialog is already open after arrival. Business dialogs must be left for the current task to classify.

Do not:

- Put Xiuluo-specific templates into `NavigationService`.
- Randomly click unknown option dialogs.
- Widen map-coordinate arrival checks as a workaround for NPC-name clicks.
- Change Wuhuan's validated hot-start behavior while implementing Xiuluo.

Shared rules now agreed:

- Startup order is: generic startup prep -> Alt+6 visibility/fade wait -> task-level dialog hot-start detection -> navigation/click NPC only if no recognized dialog state exists.
- STORY dialogs are usually ignored during normal task progress. Xiuluo only reads the accept-task STORY immediately after accepting a task, because that story contains target map and coordinate.
- OPTION dialogs are high-priority after startup prep. The active task must classify whether the option belongs to its own stage.
- Unknown OPTION dialogs should be cleaned or skipped by policy, not blindly clicked.
- If current-map navigation to an NPC opens a dialog, that counts as arrival success even if player coordinates do not equal the clicked mini-map target. This matters for Xiuluo because clicking the yellow NPC name can make the game auto-walk to the NPC body and open the dialog.

Xiuluo hot-start states to support:

- Accept-task option already open: match the Xiuluo accept template such as `xiuluo_accept_xianlaiwu.png`, click the accept option, then read the accept STORY.
- Under-five prompt already open: match the under-five confirm/wait templates and follow the configured user policy.
- Accept STORY already open: read target map/coordinate from the story and continue navigation.
- Existing task but no useful story: open Quest Manager and read the Xiuluo objective from the task panel as fallback.
- Enter-battle option already open: match the Xiuluo battle option such as "看打!", click it, then enter auto-battle/wait-combat flow.
- In combat at startup: wait for combat to finish, then continue into Xiuluo post-combat handling.
- Post-combat return state is still a known gap: after combat, Xiuluo should use the return item, return to the task NPC, accept the next round, then only yield when the leader has started meaningful movement or a safe wait state.

Recent evidence:

- A run around `2026-05-23 12:33` showed the Xiuluo accept dialog was visible, but early dialog detection missed it while the Alt+6 toast was likely still fading.
- Later the same run detected an OPTION dialog during `navigateInCurrentMap:dialog-arrived`, which confirms dialog-arrival should be treated as current-map navigation success.
- Generic arrival cleanup then saw an OPTION dialog through `ui-cleaner:force-close`, so cleanup must not close task-owned business dialogs before Xiuluo classifies them.
- Current dialog debug images use fixed filenames and can be overwritten; when diagnosing timing-sensitive dialog misses, add timestamped or reason-scoped evidence before drawing conclusions.

Files likely involved:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`

Implementation checkpoint:

- Added a generic `task.hotstart` screen classifier:
  - `TaskHotStartService`
  - `TaskHotStartSnapshot`
  - `TaskHotStartScreenState`
- The generic classifier only reports coarse current-screen state: `IN_COMBAT`, `OPTION_DIALOG`, `STORY_DIALOG`, or `NONE`.
- Xiuluo owns the task-specific interpretation of those states.
- `XiuluoTask` now probes current screen at round start before doing accept-NPC navigation.
- Xiuluo can now take over from:
  - already in combat;
  - enter-battle option already open;
  - accept-task option already open;
  - under-five prompt already open;
  - accept-task STORY already open;
  - existing Xiuluo task in Quest Manager when no useful dialog is visible.
- `clickTargetAndEnterBattle(...)` also checks whether the battle-confirm option is already open before trying another target click.
- Xiuluo no longer consumes `IN_COMBAT` in the outer execute loop; combat is now handled inside the round flow so post-combat return can run.
- Xiuluo hot-start decisions now emit high-signal `[XIULUO_HOT_START]` logs with `source`, `screen`, `action`, and objective target when available.
- Xiuluo only uses Quest Manager "existing task" hot-start on the first loop/true startup. After a completed combat-return round, the next loop skips that fallback so it does not misread stale task-panel objectives before accepting the next round.
- Auto-battle follower support mode still skips general idle maintenance, but now keeps the return-team button check so members can归队 while the leader waits after Xiuluo return.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 capture resource release hygiene

Status: completed

Goal:

- Reduce screenshot/detection memory pressure from high-frequency multi-window capture.
- Explicitly release copied/intermediate `BufferedImage` and graphics resources where safe.
- Keep detection behavior and task logic unchanged.

Owns:

- `src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java`
- `src/main/java/com/bot/dhxy/tools/ImagePreprocessor.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing capture provider selection or fallback behavior.
- Changing battle/movement/dialog detection thresholds.

Changed:

- `BoundWindowCaptureService.captureRegion(...)` now releases the full-window image after copying the requested crop.
- `BoundWindowCaptureService.captureRegionToFile(...)` now releases the cropped image after writing it to disk.
- Blank PrintWindow images are released when BitBlt succeeds and becomes the returned provider.
- `ImagePreprocessor` now disposes temporary `Graphics2D` objects and flushes temporary BGR conversion images after copying data into OpenCV `Mat`.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 Alt6 visibility template confirmation

Status: completed

Goal:

- Replace blind startup `Alt+6` double press with a template-confirmed visibility preparation loop.
- Confirm the game is in the desired "other players hidden/name-only" state by matching `images/template/2.png` in the user-provided window-relative region.

Owns:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing task business logic.
- Changing HWND screenshot or input worker internals.

Changed:

- Convert user-provided absolute region `(1661,690)-(1978,978)` with base `(1302,419)` into window-relative `(359,271)-(676,559)`.
- `NavigationService.prepareTaskStartupWindow()` now calls `ensureAlt6VisibilityDirect()` after map-tracking setup.
- The visibility helper checks `images/template/2.png` before pressing.
- If not confirmed, it presses `Alt+6`, waits 500ms, re-checks, and stops once matched; max attempts is 3.
- Startup visibility failure now makes `prepareTaskStartupWindow()` return false, so a task will not continue when the hidden-name state cannot be confirmed.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 guard member story fast click

Status: completed

Goal:

- Allow UI cleaner story fast-click for leader windows under the existing cleanup conditions.
- Restrict member windows so story fast-click only runs while the current window is in combat.
- Keep pure dialog detection no-focus and avoid changing business dialog/option click behavior.

Owns:

- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing FiveRing/Xiuluo task business logic.
- Changing dialog detection/template matching logic.

Changed:

- `UICleanerService.forceCloseDialog()` now checks role/state before fast-clicking a STORY dialog.
- Leader or unknown-role windows keep the existing UI cleaner behavior.
- Member windows only fast-click STORY dialogs while `GameContext.ActionState` is `IN_COMBAT`; outside combat they log and skip.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 make dialog detect default no-focus

Status: completed

Goal:

- Make `DialogService.detectDialogType()` a pure no-focus detection path by default.
- Remove the old input-queue wrapping that existed for Robot screenshot/focus requirements.
- Keep real click/keyboard dialog operations focused through their existing input queue paths.

Owns:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Changing dialog business policies or task flow decisions.
- Changing Npc/FiveRing/Xiuluo click logic beyond detection focus behavior.

Changed:

- `DialogService.detectDialogType()` now delegates to `detectDialogTypeNoFocus("detect-dialog-type")` directly.
- `DialogService.handleDialog(...)` uses no-focus detection after an optional initial click as well; the initial click itself remains on the existing focused input path.
- Real dialog input paths such as story click, green option click, give-item, and template-option click still use the input queue/focus where they send mouse input.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### He Li - 2026-05-23 Xiuluo follower auto-battle quiet mode

Status: completed

Goal:

- Prevent Xiuluo member windows that were auto-reassigned to AutoBattle from stealing input while the leader is still accepting/pathing.
- Keep explicit standalone AutoBattle behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Changed:

- `TaskExecutionContext` now carries `requestedTaskCode` / `requestedTaskName` in addition to the resolved running task.
- `WindowTaskRunner` preserves the originally requested task when a member window is reassigned from a leader task such as Xiuluo to `AUTO_BATTLE`.
- `AutoBattleTask` detects follower-support mode when `windowRole=MEMBER`, current task is AutoBattle, and requested task differs from AutoBattle.
- In follower-support mode, AutoBattle still polls combat state, but skips FREE-state idle maintenance such as return-team clicking, maintenance-broadcast dialog handling, and summon-skill cleanup until combat is actually detected.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-24 Main UI Base Coordinate Probe

Status: completed

Goal:

- Let the user observe which top-left base coordinate the UI/runtime is currently using after moving or detaching the game chat window.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added a `Base` column immediately after the role-name column in the main window table.
- The value is calculated the same way `GameClientTracker.updateBaseFromBinding(...)` calculates task base coordinates:
  - `baseX = nativeBinding.x / CoordinateHelper.getScaleRatio()`
  - `baseY = nativeBinding.y / CoordinateHelper.getScaleRatio()`
- The cell tooltip shows the logical tracker base, native rect, scale ratio, and hwnd.
- This is a UI diagnostic only; no task/capture/click logic changed.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-23 dialog empty-detect no-focus

Status: completed

Goal:

- Stop pure empty dialog checks from bringing game windows to foreground during AutoBattle/UI cleanup patrol.
- Keep real dialog clicks/give-item/story-click paths on focused input.

Owns:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `docs/ACTIVE_WORK.md`

Changed:

- `DialogService.handleDialog(...)` now uses `detectDialogTypeNoFocus(...)` for requests without an initial click. If no dialog exists, it returns `NO_DIALOG` without entering `dialog:detectType` focused input queue.
- Requests with an initial click still use the focused path after the click, and detected dialogs still use existing focused click/ocr/give-item paths where real input is needed.
- `UICleanerService.forceCloseDialog()` now also starts with no-focus detection, so empty cleanup checks do not focus a game window.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

Expected log change:

- Empty `handleDialog(CLICK_BUSINESS_OPTION)` patrols should show `dialog detect no-focus: reason=handle-dialog:CLICK_BUSINESS_OPTION result=NONE`.
- They should no longer be followed by `Interaction metrics ... event=focus action=queued:dialog:detectType`.

### Tangde - 2026-05-23 restore centered task count badge

Status: completed

Goal:

- Restore the main task selector count badge toward the earlier HTML mock: task name, meta, and a small centered count pill inside the task tile.
- Keep this as a UI-only correction after the user rejected the bottom-right badge / dialog-like visual direction.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` task tile layout only
- `src/main/resources/styles/dhxy-fluent.css` task tile styles only
- `docs/ACTIVE_WORK.md`

Avoids:

- Task execution semantics.
- Window/task backend behavior.
- Other agents' task business logic.

Changed:

- `MainWindowController.buildTaskTile(...)` now lays out task name, meta text, and the small count badge in one centered vertical stack, matching the earlier HTML mock direction.
- The order badge remains a small top-right dot.
- Task tile size is back to the mock-like square proportion.
- `dhxy-fluent.css` restored the count badge to a centered small pill style instead of the bottom-right corner badge.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

Open:

- Follow-up after user feedback: the first restore was still too large and did not make count editing visible enough.
- Task tiles were reduced to 70x70.
- Clicking the centered count pill now opens an inline count editor under the task grid with `- / input / + / apply / cancel`.
- `mvn -q -DskipTests compile` passed again after rerunning with dependency/network access.
- Follow-up: user clarified the count marker should not be a rounded pill. It is now a plain clickable text marker, bottom-centered on the task tile's own square base, with no separate border/background.
- `mvn -q -DskipTests compile` passed after the bottom-marker adjustment.
- Follow-up: user clarified the count marker should sit on the task tile bottom border line itself. The marker is now bottom-centered and translated downward so the tile's bottom line visually crosses through the count text.
- `mvn -q -DskipTests compile` passed after the border-line placement adjustment.
- Follow-up: user found the original reference in `docs/DHXY_FLUENT_MOCK.html` and asked to follow that mock instead of the border-line interpretation.
- Reverted the JavaFX task selector toward the mock:
  - title row has `任务选择` plus right-side selected-task summary and light `次数` button;
  - task tiles are back to 82x82 mock-like cards;
  - count summaries are small rounded count badges inside the task card content;
  - the inline count editor remains below the grid, matching the mock's lightweight count-popover direction.
- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.
- Follow-up: user screenshot showed the count badge was still escaping below the JavaFX task tile. The task tile now uses `ContentDisplay.GRAPHIC_ONLY` plus fixed internal graphic/text-stack sizes so `name / meta / count` stay inside the 82x82 card.
- `mvn -q -DskipTests compile` passed after the fixed-card-layout correction.
- Follow-up: user pointed out the mock is not vertically centered; its content is top-flowed with `strong` taking a 32px block and the count badge near the bottom. JavaFX task tiles now remove button padding and explicitly lay out name/meta/count from a 12px top inset, with the count badge below meta and near the card bottom.
- `mvn -q -DskipTests compile` passed after the mock-padding layout correction.
- Follow-up: fixed task-count interactions after user feedback:
  - count editor keeps a fixed reserved height while hidden so opening it does not move task cards;
  - clicking the same task count badge again preserves the in-progress value instead of resetting from the badge text;
  - count +/- buttons add/subtract 1 on normal click and repeat by 10 while held down.
- `mvn -q -DskipTests compile` passed after the interaction fixes.
- Follow-up: slowed the held +/- repeat rate for task count editing. Hold still changes by 10, but repeat interval is now 350ms after a 550ms hold delay for better control.
- Validation is currently blocked by unrelated in-progress `XiuluoTask.java` / `BotProperties` compile errors from another lane, not by the UI files.

This file is the short-term multi-agent coordination board for the DHXY project.

Every agent must read these files before editing:

1. `AGENTS.md`
2. `docs/DHXY_CONTEXT.md`
3. `docs/ACTIVE_WORK.md`

## Document Roles

### `docs/DHXY_CONTEXT.md`

Long-term project memory. Use it for:

- architecture direction;
- settled design decisions;
- tested conclusions;
- important historical bugs and root causes;
- long-term multi-agent ownership principles;
- session-resume guidance.

Do not add every small implementation detail there.

### `docs/ACTIVE_WORK.md`

Short-term collaboration board. Use it for:

- who is currently working on what;
- which files each agent owns right now;
- which files each agent should avoid;
- unfinished risks;
- handoff notes;
- interface/field requests between agents.

This file can be updated frequently.

## Required Update Rules

Each agent must update `docs/ACTIVE_WORK.md` in these cases:

1. Before starting a new task.
   - State the goal, owned files, avoided files, and planned files.
2. Before editing a high-conflict file.
   - High-conflict examples: `FiveRingTask.java`, `BattleRadarService.java`, `QuestManagerService.java`, `WindowTaskRunner.java`, `MultiWindowTaskManager.java`, `WindowTaskControlService.java`, `MainWindowController.java`, `SummonSkillService.java`, `AutoBattleTask.java`.
3. After finishing a meaningful phase.
   - State what changed, which files changed, validation status, and open issues.
   - Tell the user the next planned step in the chat response, not as a required `docs/ACTIVE_WORK.md` entry.
4. When another agent is needed.
   - Do not broad-edit another agent's files. Record the needed interface/field and the reason.
5. When pausing, changing direction, or abandoning a plan.
   - Leave a clear status so the next agent does not continue stale work.

Update `docs/DHXY_CONTEXT.md` only when:

1. architecture direction changes;
2. a design decision is settled;
3. a test result is confirmed;
4. a bug pattern is likely to recur;
5. session-resume instructions need to change.

## File Ownership Rules

- The agent that declares `Owns` has priority for those files.
- If another agent needs to edit an owned file, it must first record the need here and ask the user or owning agent to coordinate.
- Prefer asking for a small interface/field instead of directly changing another agent's implementation.
- Always run `git status` and inspect this file before editing.
- Do not revert unrelated dirty work.

## Current Named Agent Lanes

Name mapping:

- 何黎: framework / multi-window foundation
- 谢帅: summon skill / auto battle
- 唐德: UI

### He Li - 2026-05-23 Xiuluo task-panel fallback reuse

Status: completed

Goal:

- Make formal `XiuluoTask` use the newly verified one-shot task-detail capture for task-panel fallback.
- Avoid reopening the task panel separately for template fallback and OCR fallback.

Owns:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java` only if the existing capture API needs a tiny adjustment.

Avoids:

- Changing Wuhuan flow.
- Broad navigation/dialog/business rewrites.

Changed:

- `XiuluoTask` task-panel fallback now calls `QuestManagerService.captureCurrentQuestDetailForTask(...)` once.
- Template fallback and OCR fallback reuse the same saved right-detail screenshot.
- The already-tested story objective path was already formal; this change only aligns the rare task-panel fallback path.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 Xiuluo first workflow skeleton

Status: phase 1 completed

Goal:

- Replace the Xiuluo placeholder task with the first real leader workflow skeleton based on the user-defined flow.
- Keep member windows out of Xiuluo business logic; members should use AutoBattle/maintenance paths.
- Build the main loop around: accept task, read objective story, pre-move to Ling Shou Village exit, establish world-map pathing, maintain during formal pathing, click 修罗, enter combat, use return item, repeat.

Owns:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/task/model/TaskType.java`
- `src/main/java/com/bot/dhxy/task/DefaultTaskFactory.java`
- small Xiuluo-specific additions in `DialogService`, `BagService`, and `NavigationService` if needed.

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- broad framework/window runner changes.
- changing validated Wuhuan behavior.

Plan:

- Wire Xiuluo into task type/factory.
- Add reusable targeted dialog helpers for Xiuluo accept/confirm templates without making DialogService globally auto-handle Xiuluo.
- Add a BagService path to use a target item by scanning bag pages from the back, for the Xiuluo return item.
- Implement the first Xiuluo loop with clear TODO logs for missing image templates.

Open:

- The screenshot-derived template PNG files still need to be created in the expected paths before runtime testing can pass.

Changed:

- `XiuluoTask` now runs the first leader workflow skeleton instead of returning `SKIPPED`.
- `TaskType` and `DefaultTaskFactory` now expose/create `XIULUO`.
- `TaskTeamAssignmentPolicy` treats Xiuluo like Wuhuan for member reassignment to AutoBattle.
- `DialogService` has generic helpers for clicking a green option by template and reading green story text.
- `NavigationService` has no-yield mini-map/world-map pathing triggers for task-owned chained transactions.
- `BagService` can scan item pages from back to front for Xiuluo return item usage.
- `NpcClickService` has a Ctrl-menu keyword click path that requires the yellow menu text to contain the keyword, avoiding the older generic NPC-tag match.
- `BotProperties` / `application.properties` include first Xiuluo config defaults:
  - `bot.dhxy.xiuluo-max-runs=1`
  - `bot.dhxy.xiuluo-allow-under-five-members=false`

Validation:

- `mvn -q -DskipTests compile` passed.

Open after phase 1:

- Need create/verify template PNGs:
  - `images/template/dialog/xiuluo/xiuluo_accept_xianlaiwu.png`
  - `images/template/dialog/xiuluo/xiuluo_underfive_confirm.png`
  - `images/template/dialog/xiuluo/xiuluo_underfive_wait.png`
  - `images/template/dialog/xiuluo/xiuluo_enter_battle_kanda.png`
  - `images/template/item/xiuluo_return_item.png`
- Task-panel fallback target parsing is logged as not implemented yet.
- Cancel-task recovery branch is not implemented yet.

Template tooling update:

- Added `scripts/BuildXiuluoTemplates.java` to generate washed Xiuluo templates with Java `ImageIO`.
- Added `images/template_sources/xiuluo/README.md`.
- Source screenshots are now named:
  - `accept_dialog.png`
  - `under_five_dialog.png`
  - `enter_battle_dialog.png`
  - `return_item.png`
  - `objective_story_example.png`
- Generated and visually checked the current Xiuluo templates:
  - `images/template/dialog/xiuluo/xiuluo_accept_xianlaiwu.png` = "闲来无"
  - `images/template/dialog/xiuluo/xiuluo_cancel_task.png` = "我想取消任务"
  - `images/template/dialog/xiuluo/xiuluo_underfive_confirm.png` = "确定"
  - `images/template/dialog/xiuluo/xiuluo_underfive_wait.png` = "我再想想"
  - `images/template/dialog/xiuluo/xiuluo_enter_battle_kanda.png` = "看打!"
  - `images/template/item/xiuluo_return_item.png`
  - `images/template/npc/npc_tag.png` = "(NPC)"
- `ImageFinder` now has `findAll(...)` for multi-candidate template matching.
- Xiuluo target click now first uses Ctrl-menu `(NPC)` template matching, tries all matched candidates top-to-bottom, and confirms the correct target by matching/clicking the Xiuluo "看打!" dialog option.
- OCR keyword matching through `NpcClickService.clickNpcByCtrlMenuKeyword("修罗")` remains only as fallback after the `(NPC)` template candidate path fails.
- Added Xiuluo task-panel fallback:
  - `images/template/task/xiuluo_title.png` = collapsed/expandable "常规" group.
  - `images/template/task/xiuluo.png` = concrete "修罗" task label.
  - `images/template/task/xiuluo_active.png` = active/highlighted "修罗" task label.
  - `QuestManagerService.readCurrentQuestDetailTextForTask("xiuluo")` activates the task, captures the right detail panel, and returns OCR text.
  - `XiuluoTask` now falls back to task-panel OCR when the accept story objective cannot be parsed.
  - The task-panel scanner does not treat "常规" as the task. It first looks for any Xiuluo task-label variant, clicks "常规" only once if the group is collapsed, then searches again.
  - The Xiuluo task-detail OCR crop is narrowed to anchor-relative `(-269, 12, 264x50)` based on the user-measured task panel coordinates.

### Tangde - 2026-05-22 license worker integration

Status: completed

Goal:

- Connect DHXY main project to the shared `dhxy-license-worker` with a separate `appId=dhxy`.
- Keep DHXY and auto-battle license codes isolated by worker-side `app_id`.
- Add a real renewal endpoint to the worker so expired licenses can be extended by 30 days through the same service.

Owns:

- `src/main/java/com/bot/dhxy/auth/*`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` authentication-tab wiring only
- `src/main/resources/application.properties` license settings
- external local worker files under `D:/mavenProject/dhxy-license-worker/*`

Avoids:

- Wuhuan flow and task business logic.
- Input queue/window runtime/framework files owned by 何黎.
- `SummonSkillService.java` and `AutoBattleTask.java` owned by 谢帅.

Plan:

- Add worker `/api/license/renew` with `appId + licenseCode + deviceFingerprint + days`.
- Add DHXY license client service that posts `appId=dhxy`.
- Replace the placeholder authentication tab with verify/status/renew controls.

Changed:

- Added DHXY auth client package:
  - `src/main/java/com/bot/dhxy/auth/DeviceFingerprintService.java`
  - `src/main/java/com/bot/dhxy/auth/LicenseActionType.java`
  - `src/main/java/com/bot/dhxy/auth/LicenseAuthResult.java`
  - `src/main/java/com/bot/dhxy/auth/LicenseAuthService.java`
- Added DHXY license worker config in `src/main/resources/application.properties`.
- Replaced the `验证` tab placeholder with DHXY license verify / refresh / 30-day renewal controls in `MainWindowController.java`.
- Updated the external local `dhxy-license-worker` project:
  - added `migrations/0002_add_app_id.sql`;
  - added `appId` validation to verify/status/unbind;
  - added `/api/license/renew`;
  - updated license creation scripts to write `app_id`.
- Updated the external local `dhxy-auto-battle` project so its auth requests send `appId=dhxy-auto-battle`.

Validation:

- `D:/mavenProject/dhxy-license-worker`: `npx tsc --noEmit` passed.
- `D:/mavenProject/dhxy-license-worker`: `node --check scripts/create-license.js` passed.
- `D:/mavenProject/dhxy-license-worker`: `node --check scripts/create-license-menu.js` passed.
- `D:/mavenProject/DHXY`: `mvn -q -DskipTests compile` passed after rerunning with network/dependency access.
- `D:/mavenProject/dhxy-auto-battle`: `./mvnw.cmd test` passed.

Open:

- The worker directory is not a git repository, so its files must be deployed/copied through the existing worker deployment process.
- Remote D1 still needs migration `0002_add_app_id.sql` before deploying the new worker.
- Existing remote license rows will default to `app_id='dhxy-auto-battle'`; create or migrate separate DHXY license rows with `app_id='dhxy'`.
- User does not want to run/deploy the worker or remote D1 migration yet. Before generating real license codes later, remind the user to first run the worker deployment/migration steps, especially `0002_add_app_id.sql`, otherwise `app_id=dhxy` / `app_id=dhxy-auto-battle` isolation and renew responses will not exist remotely.

### Tangde - 2026-05-23 stale hwnd startup binding fix

Status: completed

Goal:

- Fix UI `启动` using stale selected window ids after the automatic scan/register step.
- Remove idle old native-bound runners whose hwnds are not present in the latest scan, so tasks do not start against dead bindings.

Files changed:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/window/discovery/GameWindowRegistrationService.java`

Changed:

- `GameWindowRegistrationService.registerDetectedGameWindows(...)` now prunes idle stale registrations before registering current scan results.
- Stale means either an idle manual/unbound runner or an idle native-bound runner whose `windowId` is not in the latest scan result.
- `scanRegisterAndStartIndependentWindows(...)` uses the same stale-prune step instead of removing every runner, so busy windows are not killed during scan/start.
- `MainWindowController.startMainSelectedTasks()` now scans/registers first, then recomputes target window ids from latest snapshots.
- If no windows were selected, it starts all latest `isAcceptingTaskQueue()` windows.
- If windows were selected, it only starts selected ids that still exist after scan and are still accepting task queues.
- If the selected ids went stale or became unavailable, startup returns a clear message and does not submit the old binding.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tangde - 2026-05-23 no-focus dialog capture fix

Status: completed

Goal:

- Prevent pure dialog/story screenshot or detection paths from bringing game windows to foreground when HWND capture is available.
- Keep real mouse/keyboard dialog actions on the existing focused input queue path.

Owns:

- `src/main/java/com/bot/dhxy/service/DialogService.java` no-focus detection/capture path only
- `src/main/java/com/bot/dhxy/core/GameClientTracker.java` capture provider/fallback focus check only if needed

Avoids:

- Dialog click/keyboard business behavior (`handleDialog`, green option click, story fast click) unless required for compile.
- FiveRing/Xiuluo task flow logic.

Changed:

- `DialogService.detectDialogType()` now logs the focused/queued path explicitly.
- Added `DialogService.detectDialogTypeNoFocus(...)` for capture-only detection; it calls `detectDialogTypeDirect()` without entering `InputSequences.submitExclusiveAndWait(...)`.
- `DialogService.readCurrentStoryGreenText(...)` and `captureCurrentStoryImage(...)` now use the no-focus detection path.
- `GameClientTracker` focus-failure capture logs now mark `provider=ROBOT`, so HWND success vs Robot fallback is visible in logs.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 task tile count badge restore

Status: completed

Goal:

- Restore the lighter task count/parameter badge in the main task selector.
- Make the small badge clickable without toggling task queue selection.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` task tile UI only
- `src/main/resources/styles/dhxy-fluent.css` task tile styles only
- `docs/ACTIVE_WORK.md`

Avoids:

- Task execution semantics and backend task parameter contracts.

Changed:

- Task tile count/parameter badge moved back to a small bottom-right badge instead of taking a full row in the tile.
- Selection order badge stays as a small top-right dot.
- Clicking the count/parameter badge opens a small edit dialog and does not toggle task queue selection.
- Task tile size was reduced from the larger square feel to a lighter compact tile.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### Tangde - 2026-05-23 pure-vision no-focus audit

Status: completed

Goal:

- Audit formal Xiuluo / task objective reading paths for pure screenshot/OCR work that still enters focused exclusive input.
- Convert only pure detection/capture paths to no-focus; keep paths that open panels, click options, move mouse, or press keys on focused input queue.

Owns:

- `src/main/java/com/bot/dhxy/service/LocationVisionService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java` read-only first; edit only if a pure-capture subpath can be separated safely
- `docs/ACTIVE_WORK.md`

Avoids:

- Xiuluo business decisions and target flow.
- Dialog/NPC click behavior.
- Broad framework/input queue changes.

Findings:

- `QuestManagerService` task-detail fallback opens/selects/clicks the task panel before reading detail text, so its focused exclusive transaction is intentional and was left unchanged.
- `LocationVisionService.scanCurrentLocation()` was a true pure-vision path but still entered `submitExclusiveAndWait("location:scanCurrent", ...)` when a window context existed.

Changed:

- `LocationVisionService.scanCurrentLocation()` now uses no-focus capture when a bound window context exists.
- Legacy no-context fallback still calls `tracker.bringWindowToFront()` because title-search/Robot-style single-window operation needs a visible foreground target.
- Location capture element is now logged as `location-current`.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

Open:

- Next runtime check should confirm position sync logs show `[location] scan current no-focus` and capture provider `HWND_PRINTWINDOW` without `location:scanCurrent` focus events.

### Tangde - 2026-05-23 main UI task queue selection fix

Status: completed

Goal:

- Remove the default Wuhuan task selection from the main task selector.
- Make the main task tiles build a real multi-task queue instead of being overwritten by the current combo-box task at start time.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java` task selection / pending queue UI only
- `docs/ACTIVE_WORK.md`

Avoids:

- Window runtime/control backend behavior.
- Non-task-selector layout refactors.

Changed:

- Removed the startup default `pendingTaskQueue.add(TaskType.WUHuan)`, so the main task selector starts empty.
- Removed the combo-box listener that turned current task changes into a single selected queue item.
- Task tiles now toggle membership in `pendingTaskQueue`: click to append, click again to remove.
- Main `启动` now submits the existing `pendingTaskQueue` as-is instead of clearing it and replacing it with the combo-box value.
- Main `启动` is disabled when the queue is empty.

Validation:

- `mvn -q -DskipTests compile` passed after rerunning with dependency/network access.

### 何黎: Framework / Multi-Window Foundation

Status: completed

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- input queue/framework classes when needed
- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- large UI refactors in `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- 五环 core behavior unless logs justify it

Current focus:

- Keep `MultiWindowTaskManager -> WindowTaskRunner -> WindowTaskQueue -> TaskType` clean.
- Keep single-window runs on the same multi-window path.
- Expose framework state to UI through snapshots instead of UI touching runners directly.
- Keep physical input serialized through `InputActionQueue`.

Recent status:

- `WindowTaskQueue` has first-class factories and display/log helpers.
- `WindowTaskStartRequest` can carry a `WindowTaskQueue`.
- `WindowTaskControlService` routes `SAME_TASK` through `startSameQueue(...)`.
- `RunningTaskHandle` records queue progress.
- `WindowTaskSnapshot` exposes running queue display/progress/size.
- Compile passed after these changes with `mvn -q -DskipTests compile`.

### He Li - 2026-05-21 startup input serialization fix

Goal:

- Diagnose why a 5-window Wuhuan start visually appeared to stop after opening mini maps.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`

Result:

- Root cause from logs: each window queued `ensureMapTrackingOption` first, then queued the separate startup `Alt+6` sequence. With five windows, the global input queue ran all map checks before the first window's `Alt+6`, delaying real Wuhuan entry and making the UI look stuck at mini-map startup.
- Added `NavigationService.prepareTaskStartupWindow()` so map tracking check and the two `Alt+6` presses run in one exclusive input callback for the same window.
- `DefaultWindowTaskStartupInitializer` now calls this combined startup preparation.
- Removed the separate `WindowTaskRunner` startup `Alt+6` step to avoid duplicated visibility preparation.
- Validation: `mvn -q -DskipTests compile` passed.

Open issue:

- The latest user run was stopped before prepare reached bag checks; next run should confirm first Wuhuan window reaches `五环战前准备-4` sooner and no longer looks stuck after mini-map startup.

### 何黎 - 2026-05-21 broad framework scan

Goal:

- Broad-scan framework/multi-window cleanup instead of only one small point.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `src/main/java/com/bot/dhxy/input/*`
- selected navigation framework/input cleanup in `NavigationService.java`
- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- large UI refactors in `MainWindowController.java`
- RO/role-assignment cleanup while 谢帅 owns backend role recognition

Findings:

- `WindowRuntimeContext.markQueued(...)` and `markStarted(...)` still mutate `selectedTaskType`; this can make UI/default selected task drift when a queue runs multiple task types.
- `markQueueFinished(...)` no longer overwrites `lastResult` / `lastResultMessage`; queue-level result/message lives in the dedicated queue fields.
- Input paths are mostly serialized through `InputSequences`; remaining direct `InputProvider` calls are mainly inside `submitExclusiveAndWait(...)`, debug paths, or 谢帅-owned summon/auto-battle code.
- `NavigationService` has two distinct map-close paths: `Alt+1` mini-map popups close with `Alt+1`, while `Alt+2` world-map search results still close with double-right-click because it closes the search input and the world map together.
- `WindowScopedTempPath` did not respect `bot.window.scoped-temp-path-enabled`; it now honors the switch, and the default config is set to true for multi-window runs.
- Old `bot.run.initGameWindow` config remains in `TaskRunProperties` logging but is no longer an active startup path because `AutoBot` ignores auto-start in multi-window mode.
- Old detected-role/RO start path is still present but deprecated/frozen while 谢帅 works on backend role recognition.

Changed:

- `NavigationService`: corrected map-close semantics after review; `ensureMapTrackingOption()` / mini-map actions use `Alt+1`, while world-map search-result close keeps double-right-click.
- `WindowScopedTempPath`: resolves per-window paths only when `isScopedTempPathActive()` is true.
- `application.properties` / `DHXY_CONTEXT.md`: scoped temp path default documented as enabled.

Validation:

- `mvn -q -DskipTests compile` passed after the code/config changes.

Open:

- Decide whether queue finish should stop writing queue result into per-task `lastResult`.

### He Li - 2026-05-21 capture focus binding investigation

Goal:

- Fix 5-window cross-window screenshot/OCR/template matching caused by screenshots reading a covered or wrong window region.
- Keep Wuhuan/navigation business logic unchanged because the 1-2 window logic has already been validated.

Finding:

- A first attempt to focus the bound window before every `GameClientTracker.captureToFile(...)` / `captureToMemory(...)` was too broad.
- In 5-window mode, ordinary status checks/OCR/template scans become high-frequency foreground switching, making the game windows visibly jump and causing more interference.
- The broad capture-focus change was removed. Future fixes should focus only inside deliberate input/exclusive action segments or replace screen-coordinate capture with real hwnd/window capture.

Validation:

- `mvn -q -DskipTests compile` passed after removing the broad capture-focus behavior.

Open:

- Need a narrower design: focus only for screen-to-click atomic workflows, or implement/test hwnd-based capture that does not require foreground switching.

### He Li - 2026-05-21 dialog atomic screenshot-click fix

Goal:

- Reduce five-window cross-window dialog handling caused by taking a dialog screenshot on one visible window and clicking later after another window has stolen focus.
- Keep Wuhuan/navigation business logic unchanged.

Changed:

- `DialogService` now uses the input queue's exclusive transaction for dialog workflows where screenshot/template detection and the final click must belong to the same bound window.
- Five-ring accept-dialog template click, green option fallback click, and give-option detection/click now perform their capture/detection plus direct click inside one exclusive input callback.
- Dialog OCR/business-option raw captures now run through a short exclusive capture callback, while slower OCR/template processing remains outside the input lock.
- Direct `InputProvider` usage is only inside exclusive callbacks to avoid queue-in-queue deadlock.

Changed files:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 interaction metrics HTML dashboard

Status: completed

Goal:

- Make the focus/capture/keyboard counters visible in a local HTML dashboard instead of requiring manual log reading.

Changed:

- `WindowInteractionMetricsService` now writes `logs/interaction-metrics-dashboard.html`.
- The dashboard auto-refreshes every 3 seconds and shows per-window bars for focus, HWND capture, Robot capture, failures, and HWND keyboard.
- `MainWindowController` exposes a `统计 Dashboard` button that writes the latest dashboard and opens it with the OS browser.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 interaction metrics logging

Status: completed

Goal:

- Add log counters so the next five-window run can quantify how much focus switching remains after HWND capture and background Alt shortcuts.

Changed:

- Added `WindowInteractionMetricsService`.
- Focus attempts, capture provider results, and HWND keyboard shortcuts now emit cumulative `Interaction metrics` log lines per `windowId`.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 Alt+1 background input experiment

Status: completed

Goal:

- Add one more explicit debug experiment for `Alt+1` so the user can verify that number-key shortcuts also work through HWND background keyboard messages.

Changed:

- `WindowMessageInputExperimentService` now supports `postAlt1(...)`.
- `MainWindowController` now exposes a `后台按键 Alt+1` debug button beside `后台按键 Alt+Q`.

Validation:

- `mvn -q -DskipTests compile` passed.
- User tested `后台按键 Alt+1`; it opened the mini-map successfully, confirming number-key Alt shortcuts work through HWND messages on the tested client.

### He Li - 2026-05-22 bounded HWND keyboard integration

Status: completed

Goal:

- Promote the verified background Alt-key experiments into the normal input queue for the narrow safe case.
- Keep mouse and unverified keyboard shortcuts on the existing focus + real input path.

Owns:

- `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`
- `src/main/java/com/bot/dhxy/config/WindowIsolationProperties.java`
- `src/main/resources/application.properties`

Plan:

- Add a no-focus input transaction mode for pure background-keyboard sequences.
- Use HWND Alt shortcuts only when a queued request contains only supported `PRESS_ALT_*` actions and `SLEEP`.
- If HWND posting fails or is disabled, focus the bound window and fall back to the existing `InputProvider.pressAltQ()`.

Changed:

- Added `BoundWindowKeyboardService` for the verified HWND Alt shortcut path.
- Added `bot.window.hwnd-keyboard-enabled=true`.
- `InputActionWorker` now skips focus only for queued requests made solely of supported Alt shortcuts and `SLEEP`.
- Supported background shortcuts are `Alt+1`, `Alt+2`, `Alt+4`, `Alt+6`, `Alt+8`, `Alt+T`, `Alt+O`, `Alt+E`, and `Alt+Q`.
- If HWND posting is not attempted or fails, the worker focuses the bound window inside the active input transaction and uses the original real-input shortcut method.
- Mouse actions and mixed keyboard/mouse sequences still use the focused real-input path.

Validation:

- `mvn -q -DskipTests compile` passed.

### He Li - 2026-05-22 HWND capture experiment follow-up

Status: active

Goal:

- Promote the successful per-HWND screenshot experiment into a reusable capture path.
- Reduce or remove the need to foreground/focus game windows before screenshots.
- Keep existing Robot screenshot behavior as a fallback until enough task paths are validated.

Owns:

- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `src/main/java/com/bot/dhxy/window/diagnostics/WindowCaptureExperimentService.java`
- planned capture provider/facade classes under `src/main/java/com/bot/dhxy/window/diagnostics` or `src/main/java/com/bot/dhxy/driver`
- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`

Avoids:

- Wuhuan business rules unless logs require it.
- `SummonSkillService.java` / `AutoBattleTask.java` while 谢帅 owns them.
- broad UI refactors in `MainWindowController.java`; only keep the existing debug button if needed.

Finding:

- User tested the new `后台截图实验` with two game windows overlapped, browser covering the game, and IntelliJ IDEA covering the game.
- Both `PrintWindow(PW_RENDERFULLCONTENT)` and `GetWindowDC + BitBlt` produced non-blank images for the selected bound HWNDs.
- The captured images preserved each window's own content and did not capture the covering browser/IDE/game window.
- This is a major architecture finding: for this client/machine, per-HWND capture can likely replace many `Robot` visible-screen screenshots and reduce five-window focus thrashing.

Planned direction:

- Extract the successful experiment code into a reusable bound-window capture service.
- Let `GameClientTracker.captureToMemory(...)` / `captureToFile(...)` prefer HWND capture when a current `WindowRuntimeContext.nativeBinding` exists.
- Convert absolute screen rects to window-relative rects by subtracting the tracked window base before cropping the HWND image.
- Keep Robot capture as fallback and log provider=`HWND_PRINTWINDOW` / provider=`HWND_BITBLT` / provider=`ROBOT`.
- Add a config switch before fully relying on the new provider.

Done:

- Added `BoundWindowCaptureService` as the reusable per-HWND provider.
- Added config switches:
  - `bot.window.hwnd-capture-enabled=true`
  - `bot.window.hwnd-capture-fallback-to-robot-enabled=true`
- `GameClientTracker.captureToMemory(...)` and `captureToFile(...)` now try HWND capture first when a bound window context is present.
- If HWND capture succeeds, the screenshot path no longer focuses/foregrounds the game window.
- If HWND capture fails, the old Robot screenshot path remains available as fallback.
- Capture logs now include `provider=HWND_PRINTWINDOW`, `provider=HWND_BITBLT`, `provider=ROBOT`, or `provider=HWND` for failed no-fallback cases.

Validation:

- `mvn -q -DskipTests compile` passed after the provider/tracker changes.

Open:

- Need decide whether the first production provider should use `PrintWindow`, `BitBlt`, or try `PrintWindow` then fallback to `BitBlt`.
- Need test minimized windows separately; covered windows worked, but minimized windows are a different case.

### He Li - 2026-05-22 background input message experiment

Status: completed

Goal:

- Test whether keyboard/mouse input can also be sent to a bound game HWND without foreground focus.
- Keep this as an explicit UI-triggered diagnostic, not automatic task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/window/diagnostics/WindowMessageInputExperimentService.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added a debug service that posts Win32 `WM_*` messages directly to selected HWNDs.
- Added UI debug buttons:
  - `后台按键 Alt+Q`
  - `后台鼠标中心左键`
  - `后台鼠标中心右键`
  - `子窗口中心右键`
- Each experiment saves before/after HWND screenshots under `images/temp/window_input_experiment`.
- The experiment does not use Robot/SendInput, does not move the physical cursor, and does not require foreground focus.
- `子窗口中心右键` first enumerates child HWNDs under the selected game window, logs class/title/rect, chooses the largest visible child, then posts right-click messages to that child.

Validation:

- `mvn -q -DskipTests compile` passed.
- `Alt+Q` experiment produced `posted=true`; before/after HWND screenshots showed the task panel opening, so background keyboard message worked on the tested client/window.
- Top-level `WM_LBUTTON` / `WM_RBUTTON` experiments produced `posted=true` but no visible game response, even when the game window was foreground and unobstructed.
- Child-window scan found a large visible `Win32Window` child matching the game render area. Posting right-click to that child also produced `posted=true` but no click response; before/after screenshots only showed normal animation/chat changes.

Open:

- Background keyboard messages are promising and can be tested per shortcut.
- Background mouse via normal `WM_MOUSE*` messages should be treated as unavailable for now; mouse clicks should continue using focus + serialized real input.

Open:

- Next five-window test should verify dialog accept/fallback clicks no longer land on another window, without the severe foreground flicker caused by broad capture focus.

### 何黎 - 2026-05-21 selected task semantics update

Goal:

- Keep `selectedTaskType` as the persistent configured/default task for a window.
- Prevent runtime queue/task execution from changing the selected/default task.

Changed:

- `WindowRuntimeContext.markQueued(...)` now updates only `lastTaskType` / status/message.
- `WindowRuntimeContext.markStarted(...)` now updates only `lastTaskType` / status/message/timestamps.
- Added a small runtime-event resolver so unknown runtime events still fall back to the configured selected task for display without mutating it.

Result:

- `WindowTaskSnapshot.getSelectedTaskType()` remains stable across queued task execution.
- Running/current task display continues to come from `RunningTaskHandle` / `runningTaskType`.
- Last task display continues to use `lastTaskType`.

### 何黎 - 2026-05-21 queue result separation update

Goal:

- Keep per-task result fields and per-queue result fields semantically separate.

Changed:

- `WindowRuntimeContext.markQueueFinished(...)` still updates window status, finish time, general `lastMessage`, and dedicated queue fields.
- It no longer writes queue-level result/message into `lastResult` / `lastResultMessage`.

Result:

- `WindowTaskSnapshot.getLastResult()` / `getLastResultMessage()` now describe the last concrete task event.
- `WindowTaskSnapshot.getLastQueueResult()` / `getLastQueueMessage()` describe the submitted queue as a whole.
- UI can safely show both without one overwriting the other.

### 何黎 - 2026-05-21 queue boundary scan

Goal:

- Scan queue start/stop/failure/snapshot boundaries after separating selected task, task result, and queue result semantics.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `src/main/java/com/bot/dhxy/window/control/*`
- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`

Avoids:

- 五环 business flow files unless logs require it.
- `SummonSkillService.java` / `AutoBattleTask.java`.
- large UI edits in `MainWindowController.java`.

Planned scan:

- Stop/cancel path: `WindowTaskRunner.stopCurrentTask()` and `RunningTaskHandle`.
- Failure path: task creation failure, task exception, `STOP_ON_FAILURE` / `CONTINUE_ON_FAILURE`.
- Snapshot path: running queue vs last queue vs last task display after task/queue completion.

Findings:

- `Future.cancel(true)` can mark the future done before the runner thread has fully exited. If `RunningTaskHandle.isRunning()` only checks `future.isDone()`, UI/scheduler can think the window accepts a new queue too early.

Changed:

- `RunningTaskHandle.isRunning()` now treats the handle as running while the runner thread is still alive, even if the future was cancelled.
- `WindowTaskRunner` now clears stale inactive handles through `getActiveTaskHandle()`.
- `runQueue(...)` only clears `currentTask` if it is still clearing the same handle, so a later handle cannot be accidentally erased.

Expected result:

- Stop/cancel no longer opens the window for another queue until the previous runner thread has really left its serialized task section.

### 谢帅: Summon Skill / Auto Battle

Status: active in this thread

Owns:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- summon skill deletion templates/config/service code
- auto-battle behavior and related config

Avoids:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`

If framework support is needed:

- Record the requested field/interface here.
- Let 何黎 or the user approve framework changes.

### 唐德: UI

Status: active or planned in another thread

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- UI table/button/status display
- displaying `WindowTaskSnapshot` fields
- future task queue UI controls

Avoids:

- changing task execution behavior;
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`;
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`;
- 五环 core logic.

If backend support is needed:

- Record the requested snapshot field/control API here.
- Let 何黎 add or approve framework-facing fields.

Recent UI status:

- `docs/DHXY_CONTEXT.md` notes that UI queue controls and table queue display may already have been added by the UI thread.
- Before editing UI again, inspect current `MainWindowController.java` and `git status`.

## Agent Start Template

Use this before starting a task:

```md
## Agent X - yyyy-MM-dd HH:mm

Status: active

Goal:
- ...

Owns:
- ...

Avoids:
- ...

Planned files:
- ...

Needs from others:
- none

### He Li - 2026-05-21 wuhuan transaction model cleanup

Status: active

Goal:

- Apply the settled transaction/yield model to Wuhuan.
- Stop treating story dialogs as Wuhuan advancement work.
- Keep Wuhuan option/give/task-panel chains atomic until they reach pathing, retry, finish, or failure.

Owns:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- summon/auto-battle owned files
- broad framework changes
- changing validated OCR/navigation business targets

Planned files:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-22 summon-skill clean cooldown fix

Status: completed

Done:

- Fixed AUTO_BATTLE summon-skill maintenance cooldown handling.
- `lastSummonSkillCleanAt` now updates only when `SummonSkillService.cleanSummonSkillsOnce()` returns true.
- Failed or incomplete summon-skill cleanup no longer consumes the long maintenance cooldown, so the next eligible idle/maintenance window can retry.

Changed files:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Later design should let broadcast maintenance preempt starting summon-skill cleanup, because team broadcast is higher priority than personal long-cycle maintenance.

Needs from others:

- none

### 谢帅 - 2026-05-22 auto-battle free patrol interval update

Status: completed

Done:

- Changed AUTO_BATTLE free-state patrol sleep to 3000ms inside `AutoBattleTask`.
- Kept `BattleRadarService.getDynamicPollingIntervalMs()` unchanged so Wuhuan/navigation paths are not affected.

Changed files:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Combat-state polling can be optimized further with an early-combat grace window, but that should be designed separately from the free-state broadcast patrol interval.

Needs from others:

- none

Done:

- `DialogHandleRequest.giveItemIfAvailable(...)` now ignores story dialogs instead of clicking through them.
- Wuhuan main loop no longer treats story dialogs as handled advancement work; story returns `STORY_IGNORED` and the loop continues toward task-panel P2/P1 advancement.
- Wuhuan unknown option dialogs without a give entry are treated as retryable abnormal UI: clean UI, set `needTaskSync=true`, and yield this loop.
- Removed the temporary Wuhuan "give item or story" direct path so the Wuhuan dialog transaction only owns option/give behavior.
- Added a thin task transaction layer: `TaskTransactionRunner`, `TaskTransactionResult`, `TaskYieldPolicy`, and `TaskTransactionOutcome`.
- Wuhuan initial accept, give-item, handover, task sync, and combined P2/P1 advancement now declare transaction names, expected results, and yield policies before running their exclusive input work.
- `TaskTransactionRunner` now also supports non-exclusive transactions for semantic chains that must not hold the input worker, such as preparation and post-combat maintenance.
- Wuhuan startup preparation is declared as `READY_TO_CONTINUE + CONTINUE_CHAIN`.
- Wuhuan post-combat recovery is declared as `READY_TO_CONTINUE + CONTINUE_CHAIN`, so heal/incense recovery remains part of the current chain and does not become a yield point.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-21 scan registration cleanup

Status: completed

Done:

- Found that the `roleA`/manual window row comes from manual/test registration defaults, not from team-role detection.
- Updated scan registration to prune idle manual registrations without native bindings before registering real scanned game windows.
- Running windows and native-bound game windows are left untouched.

Changed files:

- `src/main/java/com/bot/dhxy/window/discovery/GameWindowRegistrationService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- If a stale row has a native handle from a closed game window, this cleanup will not remove it yet.

Needs from others:

- none

### 谢帅 - 2026-05-21 auto-battle quiet flow audit update

Status: completed

Done:

- Audited the auto-battle input/action chain for excessive member-window operations.
- AutoBattleTask now calls `BattleRadarService.checkAndSyncCombatState(false)`, so free-state radar polling does not trigger extra first-aid checks.
- Post-combat first-aid remains handled by `AutoBattleTask.maybeHandleCombatExit(...)` only after an actual combat-exit signal.
- This keeps navigation/five-ring radar behavior unchanged while making member auto-battle quieter.

Changed files:

- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs repeat five-window observation to confirm member windows no longer do free-state health checks before any real combat exit.

Needs from others:

- none

### 谢帅 - 2026-05-21 auto-battle quiet member mode update

Status: completed

Done:

- Investigated the five-window five-ring test where member windows were reassigned to auto-battle but still pressed Alt+6 and switched windows too often.
- Root cause: reassigned AUTO_BATTLE still ran the generic task startup initializer, which performs map tracking setup and Alt+6 visibility preparation.
- Root cause: AUTO_BATTLE startup repeated team role detection even though the runner had just detected MEMBER for reassignment.
- Root cause: idle auto-battle lightweight cleanup ran every polling loop, causing frequent dialog scans and focus/input transactions.
- AUTO_BATTLE now skips the generic startup initializer.
- Runner now syncs detected LEADER/MEMBER into `WindowRuntimeContext`, so reassigned auto-battle can pass startup checks without another Alt+T team probe.
- Auto-battle idle lightweight cleanup is throttled by `auto-battle-ui-clean-interval-ms`.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs repeat five-window test: expected member windows should only do initial role probe, then enter quiet auto-battle loop without Alt+6 startup preparation and without rapid dialog-clean focus churn.

Needs from others:

- none

### 谢帅 - 2026-05-21 task startup team assignment update

Status: completed

Done:

- Added a task startup team assignment policy before `WindowTaskRunner` creates the actual task.
- When a window is asked to run five-ring and is clearly detected as MEMBER, the runner reassigns that window to AUTO_BATTLE.
- LEADER / SOLO / UNKNOWN currently keep the requested five-ring task, because five-ring can be run solo.
- Added a policy hook for future leader-only tasks such as 抓鬼 / 修罗: those can later reject SOLO and reassign MEMBER to auto-battle from the same place.

Changed files:

- `src/main/java/com/bot/dhxy/task/startup/TaskTeamAssignmentPolicy.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game queue validation: start five-ring on five windows; expected result is leader runs five-ring and four members are reassigned to auto-battle.

Needs from others:

- none

### 谢帅 - 2026-05-21 team-role auto-battle gate update

Status: completed

Done:

- Connected the validated team role detector to auto-battle startup checks.
- Auto battle now requires MEMBER by config; LEADER / SOLO / UNKNOWN skip when the gate is enabled.
- Five-ring startup no longer performs team role detection unless `five-ring-requires-leader` is enabled.
- Navigation lightweight cleanup is limited to the current window's AUTO_BATTLE task path, so five-ring navigation will not open the team panel only for cleanup gating.
- Reused the already-detected role inside startup checks instead of detecting twice.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/resources/application.yml`
- `docs/ACTIVE_WORK.md`

Validation:

- Five-window team-role debug test passed in logs: one LEADER and four MEMBER windows detected.
- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs one in-game auto-battle queue test: leader should skip auto battle, member windows should enter auto battle.

Needs from others:

- none

### Xie Shuai - 2026-05-21 team role hover short-circuit check

Status: completed

Done:

- Checked latest debug logs and confirmed one window stopped after hover because tooltip probe returned empty, so it never entered the Alt+T panel probe.
- Changed hover probing so a negative tooltip match no longer records an input dead-letter failure.
- Added explicit best-effort focus inside team-role hover and panel probes so the debug task can still work when startup Alt+6 visibility prep is skipped.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs another in-game debug run to verify the previously empty hover probe now sees the tooltip.

Needs from others:

- none
```

## Agent Update Template

Use this after finishing, pausing, or getting blocked:

```md
## Agent X - yyyy-MM-dd HH:mm update

Status: completed / paused / blocked

Done:
- ...

Changed files:
- ...

Validation:
- `mvn -q -DskipTests compile` passed / not run

Open issues:
- ...

Needs from others:
- none
```

## Active Log

### Tangde - 2026-05-22 15:04 update

Status: completed

Done:

- Adjusted the JavaFX main table checkbox column to better match `docs/DHXY_FLUENT_MOCK.html`.
- Reduced the selection column width from 34px to 28px.
- Reduced the checkbox visual size and styled it as a lightweight 12px control instead of the default large JavaFX checkbox.
- Kept real multi-select checkbox behavior from the previous update.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check the checkbox against the mock. If JavaFX checkbox styling still feels heavy, replace it with a custom tiny glyph button while preserving checkbox behavior.

### Tangde - 2026-05-22 14:56 update

Status: completed

Done:

- Replaced the main table's left `✓` selection indicator with a real checkbox column.
- Users can now select/unselect multiple windows by clicking the checkbox directly without needing Ctrl-click table selection.
- Checkbox changes keep existing selected windows selected unless that specific row is unchecked.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should verify checkbox multi-select behavior in the main control table.

### Tangde - 2026-05-22 14:49 update

Status: completed

Done:

- Fixed selected table row text becoming nearly invisible after clicking a row in the JavaFX main-control window table.
- Root cause: JavaFX selected table cells were still using the default selected text color while the custom selected background is light blue.
- Added CSS so selected row cells keep normal text color in both light and dark themes.

Changed files:

- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check row selection; selected rows should remain readable while still visibly selected.

### Tangde - 2026-05-22 14:42 update

Status: completed

Done:

- Improved selected-window visibility in the JavaFX main table.
- Added a narrow left selection indicator column that shows `✓` for selected rows.
- Strengthened selected row background and border color so `全选` is visually obvious.
- Added a selected-items listener to refresh the table selection indicator whenever multi-selection changes.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check whether selected rows are obvious enough; if still weak, switch the checkmark column to actual checkbox visuals.

### Tangde - 2026-05-21 14:31 update

Status: completed

Done:

- Fixed the blank filler area on the right side of the JavaFX main window table.
- Set the window table resize policy to constrained mode so columns fill the available table width instead of leaving a large empty white area after `操作`.
- This is display-only and does not change window/task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Re-check the table at normal width; if operation buttons become too spread out, cap the action column and add a lightweight status/message column instead.

### Tangde - 2026-05-21 14:25 update

Status: completed

Done:

- Compared the main-control page against `docs/DHXY_FLUENT_MOCK.html` after user pointed out missing row stop actions and hidden task selector.
- Added row-level stop action for problem/stopped rows, so the `操作` column keeps a visible stop affordance alongside retry/detail where appropriate.
- Reduced the default window table height from 340px to 260px and lowered its min height to 150px.
- Reduced task tile size from 82px to 76px and tightened task selector spacing so `任务选择` is visible in a normal-height console.
- Reduced shell minimum height from 760px to 640px and slightly tightened card/metric padding.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should re-check the default console height. If task selector is still low, move it above the window table as a compact strip.

### Tangde - 2026-05-21 14:14 update

Status: completed

Done:

- Fixed the JavaFX pause/resume UI state bug reported by the user.
- Root cause: `WindowTaskSnapshot.isRunning()` can remain true after pause because the task thread is still alive, so paused windows were still rendered as pause-able instead of resume-able.
- Row actions now check `WindowRuntimeStatus.PAUSED` before `snapshot.isRunning()`, so paused rows show `▶` continue.
- The top bulk pause/resume button now uses status semantics: show `继续` when selected windows include paused windows and no selected window is `RUNNING` / `QUEUED` / `STOPPING`; mixed running+paused still shows `暂停`.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should retest pause then resume from the main UI; if status refresh is still delayed, add a short optimistic UI state after pause/resume command submission.

### Tangde - 2026-05-21 14:04 update

Status: completed

Done:

- Reworked `角色详情` again based on user feedback that opening details below the table felt disconnected from the row `详情` button.
- Detail now appears as a floating panel near the right side of the window table instead of a bottom drawer.
- The floating panel does not participate in the workbench left/right layout, so it does not squeeze table columns.
- Kept explicit `详情` open and `收起` close behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- User should check whether the floating panel feels natural enough; if it covers too much table content, next option is a compact row-expanded detail directly below the selected row.

### Tangde - 2026-05-21 13:55 update

Status: completed

Done:

- Changed `角色详情` from a narrow right-side panel into a bottom detail drawer under the window table.
- Opening details no longer reduces the window table width, so role/server/id/task columns stay readable.
- Detail content now uses the full main-panel width, giving long hwnd/message/title fields room to wrap naturally.
- Kept explicit `详情` open and `收起` close behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Re-check the runtime screenshot; if the bottom drawer takes too much vertical space, make it collapsible to a compact one-row summary plus an expanded detail body.

### Tangde - 2026-05-21 13:44 update

Status: completed

Done:

- Fixed the main-control layout issue shown in the user screenshot where the right detail panel squeezed toolbar buttons and table cells into `...`.
- The `角色详情` panel no longer opens automatically from normal table selection or bulk select.
- Row `详情` now explicitly opens the detail panel for that window, and the panel has a `收起` button to return space to the table.
- Added minimum widths for the main toolbar controls so `刷新` / `全选` / `启动` / `停止` are not compressed into ellipses.
- Reduced the main table column footprint and narrowed the detail panel from 340px to 300px to leave more room for the window list.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Re-check the actual UI screenshot/runtime layout; if text is still cramped, move the detail panel into a bottom drawer or floating inspector instead of keeping it in the right column.

### Tangde - 2026-05-21 13:32 update

Status: completed

Done:

- Tightened the JavaFX main shell and main-control layout toward `docs/DHXY_FLUENT_MOCK.html`.
- Matched the left sidebar width to the mock-style 184px layout and gave the shell a 760px minimum height with a softer container shadow.
- Removed obsolete top-tab CSS rules from `dhxy-fluent.css`.
- Added a title-row counter in the `窗口与任务` panel: `已选窗口：N`, matching the mock's panel-title information pattern.
- Renamed the JavaFX shell builder from `buildMainTabs()` to `buildMainShell()` to reflect that this is now a left-sidebar shell, not a top tab layout.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` passed after Maven plugin resolution was allowed.

Next:

- Continue comparing the running JavaFX main page against the HTML mock, then tune the toolbar density and task selector/detail panel proportions if the app still feels off.

### Tangde - 2026-05-21 13:24 update

Status: completed

Done:

- Changed the JavaFX shell navigation from top tabs to a left-side sidebar like `docs/DHXY_FLUENT_MOCK.html`.
- Sidebar entries now switch pages for main control, settings, authentication, debug, logs, and notes.
- Removed the old unused JavaFX `Tab` / `TabPane` references and dead `buildTab(...)` helper so the intended layout is unambiguous.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- pending after this update.

Next:

- Continue tightening main-control spacing against the HTML mock, especially sidebar proportions, workbench width, and detail-panel behavior.

### 唐德 - 2026-05-21 13:16

Status: active

Goal:

- Move JavaFX page navigation from top tabs to a left-side sidebar like the accepted HTML mock.
- Keep existing page content, but change shell/navigation layout.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 13:06

Status: active

Goal:

- Continue JavaFX main-control layout refinement.
- Tune table density, row coloring, and row action styling to better match the mock and reduce visual noise.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 13:09 update

Status: completed

Done:

- Tuned JavaFX main table visual density.
- Added fixed table row height and tighter cell padding.
- Softened status row background colors to reduce visual noise.
- Made row action buttons lighter and narrower, with hover color states.

Changed files:

- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed.

Open issues:

- Needs visual review in the running APP to see whether JavaFX table styling renders exactly as intended.

Needs from others:

- none

### 唐德 - 2026-05-21 12:54

Status: active

Goal:

- Continue JavaFX main-control layout refinement.
- Replace the right-side selected-window detail ListView with structured key/value detail rows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 13:00 update

Status: completed

Done:

- Reworked the JavaFX right-side `角色详情` panel from a ListView-style text log into structured key/value rows.
- Detail rows now show window, role, status, binding, current task, previous execution, recent task, end time, message, and native title.
- Added CSS for `detail-row`, `detail-key`, and `detail-value`.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.
- User should review whether all detail rows are needed or whether some should move behind an expanded view.

Needs from others:

- none

### 唐德 - 2026-05-21 12:42

Status: active

Goal:

- Bring the JavaFX main-control layout closer to the accepted HTML mock.
- Put the window toolbar and table into one left workbench panel, with the detail panel on the right and task selector below.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:46 update

Status: completed

Done:

- Adjusted the JavaFX main layout closer to the HTML mock.
- Moved the window toolbar, hint, and table into one left `窗口与任务` workbench panel.
- Kept the right-side selected-window detail panel beside that workbench.
- Left summary metrics above the workbench and task selector below it.
- Narrowed the right-side detail panel slightly to better match the mock proportions.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.
- Continue tuning right-detail content density and toolbar/table spacing after user tries this layout.

Needs from others:

- none

### 唐德 - 2026-05-21 12:31

Status: active

Goal:

- Make JavaFX main start auto-select the windows it is about to start when the user has no manual selection.
- Keep the UI selection aligned with the auto-discovered/auto-targeted windows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:35 update

Status: completed

Done:

- Updated JavaFX main start flow so auto-targeted windows become selected in the table.
- When the user clicks `启动` with no manual selection, the auto-discovered accepting windows are remembered.
- After the command returns and the table refreshes, those windows are selected in the UI.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.

Needs from others:

- none

### 唐德 - 2026-05-21 12:22

Status: active

Goal:

- Fix the JavaFX main `启动` button flow so it can be used without manually scanning windows first.
- The start button should be enabled when tasks are selected, auto-discover/register game windows, then start available windows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:25 update

Status: completed

Done:

- Fixed the JavaFX main `启动` button so it no longer requires selected windows.
- The start button is enabled whenever the task selector has at least one selected task.
- Clicking `启动` now logs that it is auto-refreshing/discovering game windows before starting.
- Existing `startMainSelectedTasks()` path already auto-registers detected game windows and starts selected windows, or all accepting windows when no windows are selected.
- Updated the no-selection hint to explain that start auto-refreshes windows.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- Full Maven compile still needs to wait for unrelated non-UI compile errors in backend files to be resolved.

Needs from others:

- none

### 唐德 - 2026-05-21 12:08

Status: active

Goal:

- Continue JavaFX main-control layout refinement.
- Replace verbose overview text with compact summary metrics and reduce main page visual clutter.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 12:14 update

Status: completed

Done:

- Continued JavaFX main-control layout refinement.
- Replaced the verbose `运行概览` text card with three compact summary metric cards:
  - `窗口`
  - `运行中`
  - `异常`
- Moved detailed registered/selected/visible/accepting/binding information into the lighter operation hint text.
- Added CSS for summary metric cards.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- Verified summary metric UI code and CSS are present.

Open issues:

- Full Maven compile still needs to be rerun after unrelated non-UI compile errors in active backend files are resolved.
- Next main-layout pass should look at right-side detail panel density and table/action spacing.

Needs from others:

- none

### 唐德 - 2026-05-21 11:55

Status: active

Goal:

- Refine mixed selected-window pause/resume behavior in the JavaFX main toolbar.
- Mixed running+paused selection should show pause, not resume.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:59 update

Status: completed

Done:

- Refined selected-window pause/resume behavior for mixed selections.
- If selected windows include any running window, the top bulk action shows `暂停` and calls pause.
- The button shows `继续` only when the selected set has paused windows and no running windows.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- `mvn -q -DskipTests compile` is currently blocked by unrelated non-UI compile errors in `PlayerStateService.java` and `NavigationService.java`.

Open issues:

- Re-run full Maven compile after the other active backend/thread changes restore compile.

Needs from others:

- none

### 唐德 - 2026-05-21 11:45

Status: active

Goal:

- Fix the JavaFX main toolbar selected-window pause button so it can resume paused windows.
- Make the bulk pause/resume control reflect selected window state.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:49 update

Status: completed

Done:

- Fixed the JavaFX main toolbar selected-window pause control.
- The top bulk pause button now changes to `继续` when any selected window is paused.
- Clicking that same button now calls `resumeWindows(...)` for selected paused windows; otherwise it calls `pauseWindows(...)`.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed after Maven network/plugin resolution was allowed.
- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.

Open issues:

- If mixed selected windows include both running and paused windows, the current top action prioritizes `继续` when any selected window is paused. We can refine that if the mixed-state behavior feels wrong in use.

Needs from others:

- none

### 唐德 - 2026-05-21 11:22

Status: active

Goal:

- Translate the accepted main-control mock into the JavaFX app.
- Focus on the usable main page first: compact toolbar, window table row actions, right detail panel, and quiet square task selector.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files unless a compile issue exposes a narrow UI API mismatch
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:36 update

Status: completed

Done:

- Translated the accepted main-control mock into the JavaFX `MainWindowController`.
- Main page now uses a compact toolbar with refresh/filter/search, selected-window pause/stop, selection reset, select-all, and primary start.
- Added search filtering by role, server, player id, native title, or window id.
- Simplified the window table to role/server/id/status/running task/progress/actions.
- Added per-row action controls for start/resume, pause, stop, retry, and detail selection.
- Replaced the old visible queue builder in the main page with quiet square task tiles backed by the existing `pendingTaskQueue`.
- Main `启动` now performs automatic game-window discovery first, then starts the selected task queue on selected windows, or on accepting windows when none are selected.
- Added CSS for task tiles, row actions, toolbar, and stronger start/stop actions.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed after Maven network/plugin resolution was allowed.
- `git diff --check -- src\main\java\com\bot\dhxy\ui\MainWindowController.java src\main\resources\styles\dhxy-fluent.css docs\ACTIVE_WORK.md` passed with only the existing CRLF warning on `MainWindowController.java`.
- Verified the new JavaFX task selector, row actions, search filter, and auto-discovery start path are present.

Open issues:

- Needs hands-on UI review in the running APP to tune spacing, right-side detail width, and task tile density.
- Task count badges are visible as summaries in JavaFX, but the clickable count popover from the HTML mock is not implemented yet.

Needs from others:

- none

### 唐德 - 2026-05-21 11:12

Status: active

Goal:

- Remove native number spinner controls from the task count popover input in the main mock.
- Keep +/- buttons as the only stepper controls.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:14 update

Status: completed

Done:

- Removed native browser spinner controls from the task count popover number input.
- Kept the external `-` / `+` buttons as the only visible stepper controls.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified spinner-hiding CSS for WebKit and Firefox-style number inputs is present.

Open issues:

- none

Needs from others:

- none

### 唐德 - 2026-05-21 11:00

Status: active

Goal:

- Try interactive task-count badges in the main mock.
- Clicking a task count badge should show a lightweight count editing popover.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 11:05 update

Status: completed

Done:

- Made main-page task count badges visually clickable in the mock.
- Clicking a count badge now opens a lightweight count popover.
- The popover shows task name, stepper controls, unit text, apply, and cancel.
- Count badge clicks stop propagation so they do not toggle task selection.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `task-count`, `count-popover`, and popover JS handlers are present.

Open issues:

- User should review whether the popover belongs inline below the task grid or should appear closer to the clicked badge in the final JavaFX implementation.

Needs from others:

- none

### 唐德 - 2026-05-21 10:48

Status: active

Goal:

- Add a lightweight task-count shortcut/summary to the main page mock.
- Keep full task count configuration in Settings, but make counts visible near task selection.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 10:52 update

Status: completed

Done:

- Added a lightweight task-count summary to the main task selector.
- Each task tile now shows a small count badge such as `1轮`, `60分`, `3轮`, or `按需`.
- Added a subtle `次数` shortcut button near the task selection heading.
- Kept the full editable task-count form in the Settings tab.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `task-count` badges and the `次数` shortcut are present.

Open issues:

- User should review whether counts belong inside task tiles, in a separate compact row, or only behind the `次数` shortcut.

Needs from others:

- none

### 唐德 - 2026-05-21 10:34

Status: active

Goal:

- Replace the placeholder settings tab in the main mock with direct, visible configuration groups.
- Include supply settings, summon skill settings, task count settings, and run safety settings without nested dialogs.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 10:40 update

Status: completed

Done:

- Replaced the placeholder settings tab in the main mock with direct visible configuration groups.
- Added `补给设置` with character/summon HP/MP thresholds and post-combat supply switches.
- Added `召唤兽技能` with third-skill maintenance, interval, retry, strategy, and timing controls.
- Added `任务次数` with 五环 / 抓鬼 / 修罗 / 自动战斗 counts.
- Added `运行安全` with startup auto-refresh, skip-running, default idle selection, exception handling, and emergency hotkey.
- Removed abstract placeholder blocks such as `窗口注册`, `任务默认值`, and `窗口行为`.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified the new settings group headings are present and old abstract settings headings are gone.

Open issues:

- User is still deciding whether task counts belong only in settings or also need a lightweight main-page shortcut/summary.

Needs from others:

- none

### 唐德 - 2026-05-21 10:22

Status: active

Goal:

- Clean up the main mock table and task selector after user review.
- Remove the redundant result column from the window table.
- Remove the duplicate start button from the task selector panel and simplify task selection reset wording.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team role debug startup skip

Status: active

Goal:

- Make `队伍识别测试` faster and cleaner for repeated testing by skipping general window startup preparation.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Reason for touching high-conflict file:

- `WindowTaskRunner` owns the universal Alt+6 visibility preparation, so skipping it for one debug task requires a small targeted guard there.

Planned:

- Skip map-tracking startup initializer for `debug_team_role`.
- Skip Alt+6 visibility preparation for `debug_team_role`.
- Do not change normal 五环 / 自动战斗 startup behavior.

### 谢帅 - 2026-05-21 team role debug startup skip update

Status: completed

Done:

- `DefaultWindowTaskStartupInitializer` now skips map-tracking startup init for `debug_team_role`.
- `WindowTaskRunner` now skips the universal Alt+6 visibility preparation for `debug_team_role`.
- Normal 五环 / 自动战斗 startup behavior is unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs repeated two-window `队伍识别测试` runs to confirm the test is faster and remains attempt-1 stable.

### 唐德 - 2026-05-21 10:25 update

Status: completed

Done:

- Removed the `结果` column from the main mock window table.
- Kept current window state in the table and left historical result information for the right-side detail panel.
- Removed the duplicate `启动` button from the task selector panel.
- Changed the task selector reset action to `清空任务选择`.
- Kept the only task start entry in the top bulk action area.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `结果`, `SUCCESS`, `FAILED`, and `取消任务选择` are gone from the main table/task selector area.

Open issues:

- User should review whether the task selector panel needs any action button at all besides `清空任务选择`.

Needs from others:

- none

### 唐德 - 2026-05-21 10:12

Status: active

Goal:

- Reduce top toolbar density in the main Fluent mock.
- Shorten filter/search widths and make the bulk stop action match the start action's visual weight.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team panel exclusive probe

Status: active

Goal:

- Test whether the leader first-attempt miss is caused by another window taking focus/foreground between Alt+T open and panel screenshot.

Owns:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Hypothesis:

- Current panel probing serializes each input action, but the whole open-wait-capture-close sequence is not one exclusive transaction.
- Because screenshots use Robot screen pixels, another window can cover or change foreground before the panel crop.

Planned:

- Wrap `Alt+T -> wait -> transfer/member screenshots -> Alt+T close` in one `submitExclusiveAndWait(...)` callback.
- Use direct `InputProvider` calls inside the exclusive callback to avoid queue-in-queue deadlock.

### 谢帅 - 2026-05-21 team panel exclusive probe update

Status: completed

Done:

- `TeamRoleDetectionService` now wraps `Alt+T -> wait -> transfer/member screenshots -> Alt+T close` in a single exclusive input queue callback.
- Inside the exclusive callback it uses direct `InputProvider.pressAltT()` rather than nested `InputSequences` calls.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs another two-window `队伍识别测试` run. If leader still needs attempt 2, the remaining cause is likely Alt+T toggle state / panel-open detection rather than cross-window foreground interference.

### 唐德 - 2026-05-21 10:15 update

Status: completed

Done:

- Reduced the top toolbar density in the main mock.
- Shortened the filter dropdown column and search input width.
- Made the bulk stop action match the primary start action size/weight.
- Shortened `停止选中` to `停止` while keeping it red and grouped with selected-window actions.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified compact toolbar widths and `bulk-danger-action` styling are present.

Open issues:

- User should review whether `停止` is clear enough in context or should return to `停止选中`.

Needs from others:

- none

### 唐德 - 2026-05-21 10:02

Status: active

Goal:

- Clarify the bulk selection reset wording in the main mock.
- Make the primary start action more visually prominent and easier to click.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 10:05 update

Status: completed

Done:

- Renamed bulk `清空` to `取消选择` in the main mock.
- Renamed task selector `清空任务选择` to `取消任务选择`.
- Made the primary `启动` button wider, taller, and visually stronger with a subtle shadow.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified selection reset wording and stronger start button styling are present.

Open issues:

- The logs tab still has a separate `清空` button for clearing logs; that is unrelated to window selection.

Needs from others:

- none

### 唐德 - 2026-05-21 09:52

Status: active

Goal:

- Refine the main mock bulk action order and wording.
- Put `全选` next to the primary start action, shorten start wording, and separate stop from the start cluster.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 09:56 update

Status: completed

Done:

- Reordered the main mock bulk action cluster.
- Put `全选` directly next to the primary `启动` button.
- Shortened start buttons from `启动所选任务` to `启动`.
- Renamed stop to `停止选中` to make clear it is not global stop-all.
- Separated stop/clear from the start cluster with a subtle divider.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified old `启动所选任务` / `停止所选` labels are gone from the mock.

Open issues:

- User should review whether `停止选中` belongs in this toolbar or should move to another location later.

Needs from others:

- none

### 唐德 - 2026-05-21 09:42

Status: active

Goal:

- Correct the main mock toolbar/action icon direction after user feedback.
- Restore top batch window controls to text buttons and fix row pause/stop icon proportions.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team panel delay tune

Status: active

Goal:

- Tune team panel open delay after logs showed the first leader probe captured before the panel was rendered.

Owns:

- `src/main/resources/application.yml`
- `docs/ACTIVE_WORK.md`

Avoids:

- Team role algorithm changes beyond this delay.

Planned:

- Increase `bot.team.team-panel-open-delay-ms` from 500 to 800.

### 谢帅 - 2026-05-21 team panel delay tune update

Status: completed

Done:

- Increased `bot.team.team-panel-open-delay-ms` from 500 to 800.

Changed files:

- `src/main/resources/application.yml`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs one more in-game team-role debug run to confirm leader probe no longer needs the second retry.

### 唐德 - 2026-05-21 09:45 update

Status: completed

Done:

- Restored top batch controls to text buttons: `暂停所选` and `停止所选`.
- Removed unused square/circular toolbar icon-button styling from the mock.
- Replaced row pause/stop symbols with better-proportioned `⏸` and `⏹`.
- Kept `详情` as text.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `icon-button`, `toolbar-divider`, old `Ⅱ`, and old `■` are gone.

Open issues:

- User should review whether row icons should remain as symbols or be replaced later by real icon assets/library icons in JavaFX.

Needs from others:

- none

### 唐德 - 2026-05-21 09:30

Status: active

Goal:

- Fix ugly row action icon styling in the main mock.
- Remove square icon-button framing from row actions and restore details as text.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 09:34 update

Status: completed

Done:

- Removed the ugly square framing from row action icons in the main mock.
- Restored `详情` as a text action instead of an icon.
- Row start/pause/retry actions are now lightweight frameless icon actions.
- Row stop action is now a subtle circular danger action.
- Batch pause/stop buttons now use circular framing instead of square boxes.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `ⓘ` is gone and row actions use `detail-action` / `stop-action`.

Open issues:

- User should review whether row actions should be even quieter or whether some actions should return to text.

Needs from others:

- none

### 唐德 - 2026-05-21 09:18

Status: active

Goal:

- Reduce clutter in the main mock window toolbar.
- Change per-row operations from text buttons to compact icon buttons for start/pause/resume/stop/detail.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 09:22 update

Status: completed

Done:

- Reduced top toolbar clutter in the main Fluent mock.
- Split toolbar layout into left-side refresh/filter/search and right-side compact batch actions.
- Shortened batch selection buttons to `全选` / `清空`.
- Changed batch pause/stop to icon buttons with titles.
- Changed per-row operations to compact icon buttons:
  - `▶` for start/resume;
  - `Ⅱ` for pause;
  - `■` for stop;
  - `↻` for retry;
  - `ⓘ` for details.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified icon-button and row-action markup is present and old top-bar labels are absent.

Open issues:

- User should review whether the top toolbar still feels too dense and whether the icon choices are clear enough.

Needs from others:

- none

### 唐德 - 2026-05-21 09:05

Status: active

Goal:

- Update the main Fluent mock top window toolbar and table actions based on the latest UI decision.
- Keep filtering/search separate from selection, expose select-current-list, and move single-window actions into each row.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team role UI shortcut

Status: active

Goal:

- Add a direct selected-window team-role test button because the current task / queue UI is too unclear for quick in-game validation.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- Backend task execution changes.
- Broad UI redesign.

Planned:

- Add a single button that starts `TaskType.DEBUG_TEAM_ROLE` on the selected windows.
- Keep the existing queue and selected-task behavior unchanged.

### 谢帅 - 2026-05-21 team role UI shortcut update

Status: completed

Done:

- Added a direct `队伍识别测试` button in the main task-control row.
- The button starts `TaskType.DEBUG_TEAM_ROLE` on the selected windows through the existing `WindowTaskStartRequest.sameTask(...)` path.
- Existing current-task, selected-task, and queue behavior is unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The broader task/queue UI is still confusing and should be simplified by 唐德 later, but the team-role test now has a direct path.

### 唐德 - 2026-05-21 09:10 update

Status: completed

Done:

- Updated the main Fluent mock window toolbar:
  - kept refresh;
  - added status filter dropdown;
  - added search by role / ID / server;
  - exposed `全选当前列表` and `清空选择`;
  - kept batch `暂停所选` / `停止所选` / `启动所选任务`.
- Removed the vague `窗口操作`, `启动当前任务`, and `启动已选任务` top-bar actions from the mock.
- Added per-row window operations in the table:
  - running row: pause / stop / detail;
  - idle row: start / detail;
  - paused row: resume / stop / detail;
  - problem row: retry / detail.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified old top-bar action labels are gone and row action controls are present.

Open issues:

- User should review whether the toolbar density and row action column feel right before translating to JavaFX.

Needs from others:

- none

### 唐德 - 2026-05-21 08:52

Status: active

Goal:

- Finalize task selector direction as option A in the main mock.
- Remove the temporary C/A comparison from the main mock and keep only the quiet square task-tile selector.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files until the mock direction is confirmed enough to translate
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:57 update

Status: completed

Done:

- Finalized the main mock task selector direction as option A.
- Removed the temporary C/A comparison from the main Fluent mock.
- Main mock now keeps only quiet square task tiles with click-order badges.
- Noted runtime guidance: JavaFX should keep tile clicks local/lightweight and only call backend when the user presses start.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified C comparison text/styles are gone from the main mock and `task-card-grid` remains.

Open issues:

- Next UI implementation should translate A into JavaFX using lightweight local selection state first, then submit selected tasks only from the start action.

Needs from others:

- none

### 唐德 - 2026-05-21 08:40

Status: active

Goal:

- Put task selector options A and C into the main Fluent HTML mock for in-context comparison.
- Keep this design-only and do not modify JavaFX or backend code.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:45 update

Status: completed

Done:

- Integrated task selector options C and A into the main Fluent mock for in-context comparison.
- Left side shows C: compact checkbox-list style.
- Right side shows A: quiet square task-tile style.
- Kept the comparison design-only and did not touch JavaFX or backend files.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified `C. 紧凑勾选列表`, `A. 安静小方块`, and comparison layout classes are present.
- Verified A no longer has inner checkbox pseudo-element styling in the main mock.

Open issues:

- User should pick A or C after viewing them inside the main mock context.

Needs from others:

- none

### 唐德 - 2026-05-21 08:30

Status: active

Goal:

- Refine option A in the standalone task selector mock.
- Remove the inner checkbox square from A so each task uses only one square container.

Owns:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- `docs/DHXY_FLUENT_MOCK.html` unless the user picks a final direction
- JavaFX implementation files
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:32 update

Status: completed

Done:

- Refined option A in the standalone task selector mock.
- Removed the inner checkbox square from A task tiles.
- Option A now uses one square container only, with selected state shown by border and order badge.

Changed files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_TASK_SELECTOR_OPTIONS.html docs\ACTIVE_WORK.md` passed.
- Verified A no longer has `task-square::before` checkbox styling.

Open issues:

- User should review whether A now feels cleaner or still needs a different selected-state cue.

Needs from others:

- none

### 唐德 - 2026-05-21 08:18

Status: active

Goal:

- Create a standalone task-selector design comparison mock with options A/B/C.
- Keep the main HTML mock unchanged while comparing task selection UI directions.

Owns:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:23 update

Status: completed

Done:

- Added a standalone task selector comparison mock with options A/B/C.
- Option A shows quiet square task tiles.
- Option B shows icon-style task tiles.
- Option C shows compact checkbox-list selection.
- Kept this separate from the main fluent mock so layout discussion stays focused.

Changed files:

- `docs/DHXY_TASK_SELECTOR_OPTIONS.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_TASK_SELECTOR_OPTIONS.html docs\ACTIVE_WORK.md` passed.
- Verified all three option headings are present in the HTML.

Open issues:

- User should choose A/B/C direction before integrating the selected pattern into the main mock.

Needs from others:

- none

### 唐德 - 2026-05-21 08:05

Status: active

Goal:

- Quiet down the compact task selector in the HTML mock.
- Change task tiles from narrow rectangles to fixed square tiles with subtler selected state.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 08:08 update

Status: completed

Done:

- Changed compact task items from narrow rectangles into fixed 82px square tiles.
- Reduced selected-state noise by removing the large blue fill.
- Kept a subtle selected border, small checkbox marker, and numbered order badge.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified square task tile CSS is present.

Open issues:

- User should review whether the square tile size and selected-state contrast feel right.

Needs from others:

- none

### 唐德 - 2026-05-21 07:55

Status: active

Goal:

- Adjust the HTML mock task selector from large task cards to compact checkbox-like task tiles.
- Keep selected execution order badges, but make each task item small enough for several per row.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 team role debug follow-up

Status: active

Goal:

- Fix the team-role one-click debug path after logs showed concurrent hover screenshots could be affected by another window's queued mouse move.

Owns:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `images/template/team/member_marker.png`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 core flow.
- UI/controller changes unless the user asks.

Planned:

- Keep hover, delay, and tooltip screenshot in one exclusive input queue callback.
- Replace the too-small member marker template with the actual captured member marker area.

### 谢帅 - 2026-05-21 team role debug follow-up update

Status: completed

Done:

- `TeamRoleDetectionService` now performs hover, hover delay, and tooltip capture inside one exclusive input queue callback, so another window cannot move the mouse away before the tooltip screenshot.
- Tooltip logs now include the randomized hover point.
- Replaced `images/template/team/member_marker.png` with the actual captured member marker region (`暂时`) from the failing member window.

Changed files:

- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `images/template/team/member_marker.png`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java docs/ACTIVE_WORK.md` passed with only CRLF warning.

Open issues:

- Needs one more in-game debug run on both windows to confirm the member path now reports `MEMBER` instead of `UNKNOWN`.

### 唐德 - 2026-05-21 07:58 update

Status: completed

Done:

- Changed the HTML mock task selector from large cards to compact checkbox-like task tiles.
- Each task tile is now small enough for several items in one panel row.
- Kept selected order badges and automatic order rerendering.
- Added two extra example task tiles so the density is visible in the mock.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs\DHXY_FLUENT_MOCK.html docs\ACTIVE_WORK.md` passed.
- Verified compact tile CSS and six visible task options are present.

Open issues:

- User should review whether this compact density is right before translating the pattern back to JavaFX.

Needs from others:

- none

### 唐德 - 2026-05-21 07:35

Status: active

Goal:

- Update the HTML mock so the main task area uses task selection cards instead of queue terminology.
- Show selected task execution order with small number badges on cards.
- Keep this design-only; do not touch JavaFX implementation.

Owns:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 07:45 update

Status: completed

Done:

- Replaced the main mock's queue strip with task selection cards.
- Reduced the top summary to three clear metrics: window count, running count, and exception count.
- Added click-order badges on task cards so selected tasks show execution order.
- Added clear selection behavior and updated visible actions to `启动所选任务` / `清空任务选择`.
- Removed the visible `待提交队列` / `加入任务` / `队列操作` wording from the HTML mock.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- Verified the HTML mock contains `task-card`, `toggleTask`, and `启动所选任务`.
- Verified the HTML mock no longer contains `待提交队列`, `加入任务`, `队列操作`, or `启动队列`.

Open issues:

- After user review, decide the exact task-card names and whether execution order adjustment needs drag/drop or small arrow controls later.

Needs from others:

- none

### 唐德 - 2026-05-21 07:15

Status: active

Goal:

- Switch UI workflow from direct JavaFX tweaking to a standalone mock-view design phase.
- Create an HTML/CSS mock for the selected E / Windows 11 Fluent Light direction.
- Use the mock to discuss overall layout, panels, and control hierarchy before further JavaFX implementation.

Owns:

- `docs/ui-mockups/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- JavaFX implementation files for this phase unless a tiny documentation link is needed
- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 07:15 update

Status: completed

Done:

- Switched UI work to a standalone mock-view phase instead of continuing direct JavaFX layout tweaks.
- Added `docs/DHXY_FLUENT_MOCK.html` as a single-file HTML/CSS mock for the selected E / Windows 11 Fluent Light direction.
- The mock includes:
  - left navigation shell;
  - light/dark theme toggle;
  - `主控`, `设置`, `验证`, `调试`, `日志`, `说明` sections;
  - main workbench with summary metrics, window table, right-side role detail panel, and queue strip;
  - settings/debug/log placeholder panels for discussion.
- This file is design-only and does not affect JavaFX runtime or backend behavior.
- Attempted to create `docs/ui-mockups/`, but Windows returned access denied for creating that subdirectory; used `docs/DHXY_FLUENT_MOCK.html` instead.

Changed files:

- `docs/DHXY_FLUENT_MOCK.html`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- docs/DHXY_FLUENT_MOCK.html docs/ACTIVE_WORK.md` passed.
- Verified the mock contains all six planned sections and the theme toggle script.

Open issues:

- Need user review of the mock at the layout/panel level before translating any of it back into JavaFX.
- If desired later, move mock files into a dedicated folder after the directory creation permission issue is resolved.

Needs from others:

- none

### 唐德 - 2026-05-21 06:55

Status: active

Goal:

- Reduce button clutter in the JavaFX main control tab.
- Collapse low-frequency window selection/management/runtime actions into menus.
- Keep backend behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 06:55 update

Status: completed

Done:

- Reduced main-tab button clutter by collapsing low-frequency actions into menus.
- Replaced many always-visible window buttons with three menus:
  - `选择窗口`: all/running/idle/problem/bound/unbound/clear selection;
  - `窗口管理`: unregister selected/all;
  - `运行控制`: pause/resume selected/all and stop selected/all.
- Kept high-frequency actions visible:
  - refresh;
  - filter;
  - start current task;
  - start selected task.
- Menu items now follow selection-based disabled states for selected-window operations.
- Backend behavior remains unchanged; menu actions reuse the existing control-service calls.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Queue controls are still expanded. If they feel crowded in actual use, the next UI pass can collapse queue reorder/clear/presets into a `队列操作` menu.

Needs from others:

- none

### 唐德 - 2026-05-21 06:35

Status: active

Goal:

- Move the main tab closer to the selected E layout with a center table and right-side detail panel.
- Hide the right-side detail panel when no window is selected.
- Add light/dark theme switching without changing backend behavior.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 06:35 update

Status: completed

Done:

- Moved the main tab closer to the selected E / Windows 11 Fluent layout:
  - top controls stay above the work area;
  - window table is the center workbench;
  - selected-window detail is a right-side panel.
- The right-side selected-window detail panel now hides when no window is selected and no longer shows an empty placeholder.
- Added a `深色模式` toggle in the top bar.
- Added dark-theme CSS variables and overrides in `dhxy-fluent.css`.
- Kept existing pause/resume UI buttons from the framework pause work intact.
- Backend task behavior remains unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java src/main/resources/styles/dhxy-fluent.css docs/ACTIVE_WORK.md` passed with only CRLF warnings on Java files.
- `mvn -q -DskipTests compile` is currently blocked by unrelated in-progress 五环 pause work:
  - `FiveRingTask.java` calls `checkpoint(TaskExecutionContext)`;
  - no matching method is currently available in the scanned task/runner/window files.

Open issues:

- Dark-mode rendering needs a real JavaFX window check; CSS is present but visual contrast should be verified manually.
- Compile should be rerun after the framework/FiveRing pause checkpoint work is completed.

Needs from others:

- 何黎 / framework pause owner: finish or expose the missing `checkpoint(TaskExecutionContext)` support used by `FiveRingTask.java`.

### 何黎 - 2026-05-21 pause safe-point control

Status: active

Goal:

- Add first-version per-window task pause/resume.
- Pause should happen at task safe checkpoints, not by stopping the global input worker.
- Already-submitted physical input sequences are allowed to finish naturally.

Owns:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `src/main/java/com/bot/dhxy/runner/stop/TaskPauseToken.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/model/WindowRuntimeStatus.java`
- small pause/resume button wiring in `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 business logic
- `SummonSkillService.java`
- `AutoBattleTask.java`
- broad UI layout or styling changes

Planned files:

- same as Owns.

Needs from others:

- none

### 何黎 - 2026-05-21 pause safe-point control update

Status: completed

Done:

- Added cooperative per-window task pause/resume.
- Pause is stored on the active `RunningTaskHandle` through `TaskPauseToken`.
- Existing task stop checkpoints now also wait while paused via `TaskExecutionContext.throwIfStopRequested()`.
- Stop still wakes and interrupts a paused task.
- Added backend control APIs for selected/all pause and resume.
- Added small UI buttons for selected/all pause and resume without changing broad UI layout.

Changed files:

- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `src/main/java/com/bot/dhxy/runner/stop/TaskPauseToken.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/model/WindowRuntimeStatus.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- This is a safe-point pause. If the current task is inside a long input sequence or a loop without `executionContext.throwIfStopRequested()`, it will pause after that section reaches the next checkpoint.
- After a follow-up scan, 五环 received extra checkpoints around prepare, initial navigation/NPC click, post-combat supply, dialog handling, task sync, and P2/P1 pathing triggers.
- User test showed pause requests arrived, but the 摄妖香补给 path kept scanning bag pages because `PlayerStateService.ensureSheYaoXiangActiveForLeaderTask(...)` called `BagService` without `TaskExecutionContext`.
- Fixed by adding context-aware `PlayerStateService` overloads and wiring 五环/AutoBattle post-combat calls through them.
- `TaskPauseToken` now logs when a pause checkpoint is reached and resumed.

Needs from others:

- none

### 唐德 - 2026-05-21 06:15

Status: active

Goal:

- Start applying the selected E / Windows 11 Fluent Light visual direction.
- Add a JavaFX stylesheet and migrate controller inline styling toward reusable style classes.
- Keep backend behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowService.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowService.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 06:15 update

Status: completed

Done:

- Started applying the selected E / Windows 11 Fluent Light visual direction.
- Added a reusable JavaFX stylesheet:
  - `src/main/resources/styles/dhxy-fluent.css`
- `MainWindowService` now loads `/styles/dhxy-fluent.css` into the JavaFX `Scene`.
- Increased initial window size from `980x640` to `1120x720` to better fit the tabbed console layout.
- Migrated UI styling away from inline JavaFX style strings toward reusable style classes:
  - root/top bar/tab content;
  - tab pane;
  - section cards and titles;
  - primary/secondary/danger buttons;
  - status/hint/queue summary text;
  - table/list/log styling;
  - row status styles for running/accepting/stopped/error windows.
- Backend task behavior remains unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowService.java`
- `src/main/resources/styles/dhxy-fluent.css`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java src/main/java/com/bot/dhxy/ui/MainWindowService.java src/main/resources/styles/dhxy-fluent.css docs/ACTIVE_WORK.md` passed with only CRLF warnings on Java files.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Visual result still needs real JavaFX window inspection; CSS compiles as a resource but JavaFX runtime rendering should be checked manually.
- Later polish can move more layout spacing into CSS and split `MainWindowController` into tab-specific components.

Needs from others:

- none

### 唐德 - 2026-05-21 05:50

Status: active

Goal:

- Fill missing functional UI tabs before visual polish.
- Add separate log and diagnostics/debug tabs.
- Keep implementation UI-only and reuse existing task/queue paths.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 02:05 update

Status: completed

Done:

- Added `DebugTeamRoleTask`, a one-shot task for testing backend team-role detection on the selected/bound window.
- Added `TaskType.DEBUG_TEAM_ROLE`, so the task appears in existing task selectors/queues.
- Registered the task in `DefaultTaskFactory`.
- Added `TeamRoleDetectionService.detectCurrentRoleForDebug(...)`, which intentionally bypasses `roleDetectionEnabled` for manual debug runs only.

Changed files:

- `src/main/java/com/bot/dhxy/task/DebugTeamRoleTask.java`
- `src/main/java/com/bot/dhxy/task/model/TaskType.java`
- `src/main/java/com/bot/dhxy/task/DefaultTaskFactory.java`
- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The debug task is available through the existing task selector/start flow. A dedicated one-click UI button was not added yet because `MainWindowController.java` has high churn/encoding-fragile text in this area.

Needs from others:

- none

### 唐德 - 2026-05-21 05:50 update

Status: completed

Done:

- Added missing functional UI tabs before visual polish:
  - `调试`: task diagnostics/debug entry points;
  - `日志`: window command/UI operation logs.
- Moved command log display from the bottom area into the `日志` tab so it no longer crowds the main window table.
- Added initial diagnostics controls:
  - set current task selectors to `坐标调试`;
  - add `坐标调试` to the pending queue.
- Added diagnostics notes for important log files:
  - `logs/dhxy-console.log`;
  - `logs/tracker-coordinate.log`.
- Updated the `说明` tab to describe `调试` and `日志`.
- Backend behavior remains unchanged; debug controls reuse existing task selection/queue paths.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- `调试` tab currently exposes only coordinate-debug helpers and placeholders for NPC first-shot, screenshot/OCR, and template matching tools.
- `验证` tab remains a placeholder until captcha/authentication behavior exists.

Needs from others:

- none

### 唐德 - 2026-05-21 05:30

Status: active

Goal:

- Correct the UI layout direction after user feedback.
- Remove whole-page scrolling and split the JavaFX UI into tabs.
- Keep scrolling only inside detail/list controls such as selected-window details.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 05:30 update

Status: completed

Done:

- Corrected the UI layout direction based on user feedback.
- Removed the whole-page `ScrollPane` from the central window-control area.
- Added a `TabPane` with four tabs:
  - `主控`: day-to-day window selection, task control, window table, selected-window detail, and task queue;
  - `设置`: window registration/scanning and supply configuration;
  - `验证`: placeholder for future captcha/authentication workflows;
  - `说明`: short explanation of the tab layout and current UI responsibilities.
- Kept scrolling local to list/detail controls such as the selected-window detail list and logs.
- Removed leftover registration/discovery rows from the main tab so JavaFX controls are not attached to duplicate parents.
- Backend task behavior remains unchanged.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- The `验证` tab is currently a placeholder until the captcha/authentication flow exists.
- Main-tab spacing should be checked in the actual JavaFX window, especially on smaller resolutions.

Needs from others:

- none

### 唐德 - 2026-05-21 05:05

Status: active

Goal:

- Do a broader UI-only cleanup pass after scanning current JavaFX UI and snapshot fields.
- Improve status hierarchy, table readability, selected-window diagnostics, queue summary, and command logs together.
- Keep backend behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 05:05 update

Status: completed

Done:

- Scanned the current JavaFX UI controller and available `WindowTaskSnapshot` fields.
- Added a broader UI-only cleanup pass:
  - wrapped the top status text into a dedicated `运行概览` section;
  - added light section borders/backgrounds for clearer grouping;
  - added pending queue summary text above the queue list;
  - increased selected-window detail height and added more diagnostics;
  - selected-window details now show last task/result timestamps, last queue message, last result message, and queue failure policy;
  - main table now includes recent task and recent result columns;
  - table rows are lightly colored by state: running, stopped, error, and task-accepting windows;
  - window overview now includes visible accepting-window count and bound-window count;
  - command detail logs now use `[成功]` / `[失败]` prefixes for faster scanning.
- This is UI display/layout/logging only and does not change backend task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Current styling is inline JavaFX CSS inside the controller. Later, if the UI keeps growing, moving styles into a stylesheet would be cleaner.
- No visual runtime screenshot was captured in this headless/tooling pass; user should verify the actual JavaFX window sizing/scroll behavior in the app.

Needs from others:

- none

### 唐德 - 2026-05-21 04:45

Status: active

Goal:

- Continue JavaFX UI layout cleanup after the scroll/table visibility fix.
- Reduce crowded horizontal control rows by allowing action rows to wrap.
- Keep changes UI-only.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 04:45 update

Status: completed

Done:

- Replaced crowded one-line JavaFX action rows with wrapping `FlowPane` control rows.
- Registration, discovery, window selection, task control, supply config, and queue controls now wrap when the window is narrow.
- Split task queue controls into separate rows for adding tasks, managing queue order, and applying presets.
- Kept the previous scroll/table visibility fix intact.
- This is UI layout only and does not change backend task behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- Visual styling is still utilitarian. Later polish can add spacing/section styling, but the immediate usability issue should be improved.

Needs from others:

- none

### 何黎 - 2026-05-21 03:44 update

Status: completed

Done:

- Scanned potentially overlapping window snapshot/runtime fields.
- No code changes were made.
- Current interpretation:
  - `roleName` is legacy/display fallback identity and still useful when structured identity is empty.
  - `playerName` / `playerId` / `serverName` are the preferred structured player identity fields.
  - `selectedTaskType` is the configured/default task for the window.
  - `runningTaskType` is the current task inside the active queue.
  - `lastTaskType` / `lastResult` describe the most recent single task result.
  - `lastQueueDisplayText` / `lastQueueResult` / `lastQueueMessage` describe the last submitted queue/batch result.
  - `lastMessage` is the latest high-level window status message.
  - `lastResultMessage` is the latest single-task/finish message.
  - `lastQueueMessage` is the latest queue-level finish message.
- 唐德's UI already prefers `WindowTaskSnapshot.getPlayerName()/getServerName()/getPlayerId()` before native-title parsing, so the new snapshot identity interface is consumed.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Scan-only / docs-only update; compile not run.

Open issues:

- Do not remove the overlapping fields yet; they represent different time scopes and UI/debug consumers still use them.

Needs from others:

- none

### 何黎 - 2026-05-21 03:35 update

Status: completed

Done:

- Scanned framework/input/window naming for misleading legacy/debug/test names after `GlobalInputLock` cleanup.
- No Java code changes were made.
- Findings:
  - No remaining misleading bean name comparable to `legacyGlobalInputLock` was found in active framework/input code.
  - `WindowInteractionDiagnostics`, `WindowInteractionReport`, `TaskWindowRuntimeService`, and `TaskWindowBindingResolver` names match their current support/diagnostic responsibilities.
  - `DefaultWindowTaskStartupInitializer` / `WindowTaskStartupInitializer` names match current startup behavior.
  - The main remaining "test/role assignment" naming is the old RO/assignment path (`DETECTED_ROLE`, `startByDetectedRoleForTest`, `WindowTaskAssignmentPolicy`, etc.), but this is intentionally frozen while 谢帅 works on backend role recognition.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Scan-only / docs-only update; compile not run.

Open issues:

- Revisit RO/assignment naming only after 谢帅's backend role recognition work settles.

Needs from others:

- none

### 唐德 - 2026-05-21 04:30

Status: active

Goal:

- Fix JavaFX UI layout usability after user testing.
- Make the window table visible without manual resizing.
- Add scrolling to the main window-control area and move the table higher in the layout.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- backend execution/control/runtime framework files
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 04:30 update

Status: completed

Done:

- Fixed the JavaFX window-control layout so the registered-window table is visible without manually dragging the app taller.
- Wrapped the main window-control area in a `ScrollPane` with vertical and horizontal scrollbars as needed.
- Moved the window table higher in the page, before the detail/queue/registration sections.
- Gave the window table and selected-window detail list stable minimum/preferred heights.
- Reduced bottom command-log panel height so it does not crowd out the main table.
- Slimmed the main table by removing low-frequency `绑定标题` and redundant `运行中` columns; full title remains visible in selected-window details.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- The control rows are still long horizontal rows. A later UI pass can split registration, queue, and selection actions into tighter rows if the app window is narrow.

Needs from others:

- none

### 何黎 - 2026-05-21 03:26 update

Status: completed

Done:

- Renamed the `GlobalInputLock` Spring bean from `legacyGlobalInputLock` to `globalInputLock`.
- Checked there are no `@Qualifier("legacyGlobalInputLock")` usages before renaming.
- The class remains the active global physical-input lock used by `WindowAwareInputCoordinator`, `WindowFocusService`, and `GameClientTracker`.

Changed files:

- `src/main/java/com/bot/dhxy/input/GlobalInputLock.java`
- `docs/ACTIVE_WORK.md`

Validation:

- Pending compile after this small bean-name cleanup.

Open issues:

- none

Needs from others:

- none

### 唐德 - 2026-05-21 04:10

Status: active

Goal:

- Continue JavaFX UI-only improvements after reading the latest coordination board.
- Make selected-window diagnostics easier to read without changing backend execution behavior.
- Surface structured queue submit diagnostics already provided by framework command details.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 04:10 update

Status: completed

Done:

- Reworked selected-window diagnostics in `MainWindowController` from one long label into a structured detail list.
- Detail rows now separate:
  - window/status/accepting-task state;
  - role/server/player id identity;
  - native binding hwnd/class/pid;
  - selected/running task and queue progress;
  - current/last queue and queue result;
  - full native title and last message.
- Command detail logs now surface structured queue submit diagnostics when present:
  - submitted queue display text;
  - submit status;
  - queue failure policy.
- This is UI display/logging only and does not change task execution behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed after rerunning with network permission for Maven plugin resolution.

Open issues:

- The table is still very wide; the next UI-only pass can move low-frequency columns such as full title/message out of the table and rely on the structured detail list instead.

Needs from others:

- none

### 何黎 - 2026-05-21 03:18 update

Status: completed

Done:

- Scanned for old single-window / old runner leftovers after the current documentation sync.
- Confirmed deleted-old-path statement is accurate:
  - no `GameWindowService` source remains;
  - no old `runner/execution/TaskRunner` or `runner/execution/TaskQueue` source remains;
  - no old task registry/history/log/viewmodel classes remain in source.
- Remaining `runner/*` files are current support types:
  - `runner/context/TaskExecutionContext`;
  - `runner/policy/TaskRetryPolicy`;
  - `runner/stop/*`.
- Remaining `window/interaction/*` files are current support/diagnostic types, not the old mouse/screenshot service stack:
  - `WindowFocusService`;
  - `TaskWindowRuntimeService`;
  - `TaskWindowBindingResolver`;
  - `WindowInteractionDiagnostics`;
  - `WindowInteractionReport`.
- `GlobalInputLock` bean has since been renamed to `globalInputLock`; it is actively used by `WindowAwareInputCoordinator`, `WindowFocusService`, and `GameClientTracker`; do not delete it as old code.
- No Java code was changed.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Scan-only / docs-only update; compile not run.

Open issues:

- The confusing old `legacyGlobalInputLock` bean name has been cleaned up. `GlobalInputLock` is not dead code.

Needs from others:

- none

### 何黎 - 2026-05-21 03:08 update

Status: completed

Done:

- Scanned long-term docs against the current framework code for recently changed APIs.
- Updated `docs/DHXY_CONTEXT.md` framework status to include:
  - `WindowTaskQueue` failure policy;
  - default `CONTINUE_ON_FAILURE` behavior and future `STOP_ON_FAILURE`;
  - queue result aggregation and runtime/snapshot queue fields;
  - structured snapshot player identity fields;
  - structured submit status/details;
  - startup initializer behavior and `debug_coordinate` skip;
  - runner-level `ALT+6` visibility preparation.
- No Java code was changed.

Changed files:

- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Validation:

- Documentation-only change; compile not run.

Open issues:

- `AGENTS.md` still has older high-level summaries; this is acceptable because `docs/DHXY_CONTEXT.md` carries the detailed current state.

Needs from others:

- none

### 何黎 - 2026-05-21 02:58 update

Status: completed

Done:

- Documented the settled direct-input rule in `docs/DHXY_CONTEXT.md`.
- Rule summary:
  - normal task input goes through `InputSequences` / `InputActionQueue`;
  - direct `InputProvider` calls are allowed inside `submitExclusiveAndWait(...)` callbacks because the worker is already in a serialized exclusive section;
  - do not enqueue `submitAndWait(...)` from inside an exclusive callback;
  - debug-only direct input paths must stay clearly marked as debug/direct;
  - move + click remains atomic as one queued action list or one exclusive callback section.
- No Java code was changed.

Changed files:

- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md`

Validation:

- Documentation-only change; compile not run.

Open issues:

- Later code comments may be useful around special direct-input methods, but avoid touching `SummonSkillService` while 谢帅 owns it.

Needs from others:

- none

### 唐德 - 2026-05-21 01:07

Status: active

Goal:

- Consume framework-provided structured player identity fields in the JavaFX UI.
- Prefer `WindowTaskSnapshot.playerName/playerId/serverName` over native title parsing.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 01:07 update

Status: completed

Done:

- Updated `MainWindowController` identity display to prefer structured snapshot fields:
  - `getPlayerName()`
  - `getServerName()`
  - `getPlayerId()`
- Native-title regex parsing remains only as fallback.
- Final fallback still uses the old roleName/title-derived value when structured identity and title parsing are unavailable.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed.

Open issues:

- UI will show `-` for structured identity until a task path has run `PlayerStateService.syncMyIdentity()` for that window.

Needs from others:

- none

### 何黎 - 2026-05-21 02:44

Status: completed

Goal:

- Add structured player identity fields to `WindowTaskSnapshot` for 唐德's UI thread.
- Source identity from each window's bound `GameContext.State.me`.
- Keep UI untouched and preserve existing snapshot constructors.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- RO/leader-member recognition logic owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- Added structured player identity fields to `WindowTaskSnapshot`:
  - `playerName`
  - `playerId`
  - `serverName`
- `WindowTaskRunner.snapshot()` now reads the current window's dedicated `GameContext.State.me` and passes identity into the snapshot.
- Empty identity values are normalized to `null`.
- Existing snapshot constructors are preserved for compatibility.
- UI was not edited; 唐德 can now prefer snapshot identity fields and keep native-title parsing as fallback.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI thread still needs to consume the new getters.

### 何黎 - 2026-05-21 02:32 update

Status: completed

Done:

- Scanned framework/business boundary points while intentionally avoiding RO/leader-member cleanup because 谢帅 is working on that backend feature.
- No code changes were made.
- Window layer still has RO-shaped fields/helpers (`WindowRole`, `WindowRuntimeContext.role`, `WindowRegistrationRequest.role`) but these are frozen for now per user direction.
- Direct `InputProvider` usage classification:
  - Looks acceptable / intentional:
    - `NavigationService.openMapInputTargetAndClickLastNavPointExclusive(...)`: direct input is inside `submitExclusiveAndWait(...)`.
    - `NavigationService.ensureMapTrackingOption(...)`: direct input is inside `submitExclusiveAndWait(...)`.
    - `SummonSkillService` direct input calls are mostly inside `submitExclusiveAndWait(...)`; owned by 谢帅, do not modify here.
    - `NpcClickService` Ctrl probe direct input is inside `submitExclusiveAndWait(...)`, matching the no-nested-queue rule.
  - Needs future review, not changed now:
    - `NpcClickService.executeClickAndVerifyDirect(...)` / direct first-shot debug helpers still use `InputProvider` directly. They appear tied to direct/debug or exclusive paths, but should stay documented as special-case only.
    - Some services still inject both `InputProvider` and `InputSequences`; this is allowed only when direct calls are inside an exclusive input transaction or a debug-only path.
- No obvious active path was found that directly creates/runs `WindowTaskRunner` outside `MultiWindowTaskManager`.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Code not changed in this scan; compile not rerun.

Open issues:

- Later framework cleanup could add comments around direct-input special cases, but avoid touching `SummonSkillService` while 谢帅 owns it.

Needs from others:

- none

### 何黎 - 2026-05-21 02:20 update

Status: completed

Done:

- Scanned remaining framework compatibility/legacy APIs without deleting code.
- Current backend execution shape is still clean: `MultiWindowTaskManager -> WindowTaskRunner -> WindowTaskQueue -> TaskType`.
- Cleanup candidates found:
  - old/test role-assignment flow:
    - `WindowTaskStartMode.DETECTED_ROLE`
    - `WindowTaskStartRequest.detectedRole(...)`
    - `WindowTaskControlService.startByDetectedRole(...)`
    - `WindowTaskControlService.startByDetectedRoleForTest(...)`
    - `GameWindowRegistrationService.registerDetectedGameWindowsByRoleForTest(...)`
    - deprecated role-mapping helpers in `WindowRegistrationBatchBuilder` / `NativeWindowRegistrationMapper`
  - boolean/single-task compatibility wrappers in `MultiWindowTaskManager`, such as `submit(...)`, `submitSelectedTask(...)`, `submitSelectedTasks(...)`, and `submit(Collection, TaskType)`.
- Keep for now:
  - `WindowTaskStartMode.SELECTED_TASK`, because current UI still uses selected-task startup.
  - `WindowTaskStartRequest.sameTask(...)`, because current UI single-task start uses it.
  - `WindowTaskControlService.startIndependentWindows(...)`, until UI/discovery naming is cleaned up.
- Do not delete the old/test role-assignment flow until the user confirms no thread still needs the test path.

Changed files:

- `docs/ACTIVE_WORK.md`

Validation:

- Code not changed in this scan; compile not rerun.

Open issues:

- Next cleanup can either mark more legacy factories as `@Deprecated`, or leave them untouched until UI and discovery flows settle.

Needs from others:

- none

### 唐德 - 2026-05-21 00:52

Status: active

Goal:

- Investigate and improve JavaFX window identity display.
- Keep role/name/server/id display separate from the full native window title.
- Prefer UI-side parsing/display first; avoid backend binding changes unless needed.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:52 update

Status: completed

Done:

- Fixed UI identity display so `角色名` no longer shows the full native window title when the title is parseable.
- Reused the same title shape as `ClientIdentityService`: `- 服务器 - 角色名 (ID:123)`.
- Added separate table columns for `服务器` and `ID`.
- Updated selected-window detail to show parsed role/server/id separately from the full native title.
- This is UI display only. Backend registration, title binding, and task identity sync were not changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- `mvn -q -DskipTests compile` passed.

Open issues:

- If the game title format changes, UI parsing and `ClientIdentityService` should eventually share a common parser instead of duplicating the regex.

Needs from others:

- none

### 何黎 - 2026-05-21 02:08

Status: completed

Goal:

- Preserve structured submit diagnostics in `WindowTaskCommandDetail`.
- Avoid forcing UI/log code to parse free-form message strings later.
- Keep existing messages and UI behavior unchanged.

Owns:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- summon skill / auto battle files owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/control/WindowTaskCommandDetail.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- `WindowTaskSubmitResult` now exposes queue failure policy and submit status display text.
- `WindowTaskCommandDetail` can now preserve structured submit diagnostics:
  - submit status;
  - task queue display text;
  - task queue failure policy.
- `WindowTaskControlService` now builds task-start command details from `WindowTaskSubmitResult`, while keeping the existing message text.
- Registration/stop/remove details remain simple message-only details.
- UI was not edited.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskCommandDetail.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI still logs only detail messages. It can use `getSubmitStatusDisplayName()` and queue/policy getters later if needed.

### 何黎 - 2026-05-21 01:55

Status: completed

Goal:

- Add structured queue failure policy fields to runtime/snapshot data.
- Keep UI behavior unchanged; only expose backend state for later display/debug.
- Avoid editing `MainWindowController.java` while 唐德 owns UI.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- summon skill / auto battle files owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- `WindowRuntimeContext` now stores the last queue failure policy together with last queue result/message.
- `RunningTaskHandle` exposes the active queue failure policy.
- `WindowTaskSnapshot` now has structured running/last queue failure policy getters and display-name helpers.
- `WindowTaskRunner` passes the active policy into both snapshot construction and queue-finished runtime state.
- UI was not edited.

Changed files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI can optionally display `getRunningQueueFailurePolicyDisplayName()` / `getLastQueueFailurePolicyDisplayName()` later, but this is not required for current behavior.

### 唐德 - 2026-05-21 00:46

Status: active

Goal:

- Continue UI work autonomously within the UI lane.
- Add task queue preset helpers so common queues can be built quickly.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:46 update

Status: completed

Done:

- Added task queue preset buttons to the JavaFX queue panel:
  - `预设:当前任务`
  - `预设:五环`
  - `预设:自动战斗`
  - `预设:五环+自动战斗`
- Presets only update the UI pending queue and do not change backend queue execution semantics.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/ui/MainWindowController.java docs/ACTIVE_WORK.md` passed with only CRLF warnings.
- Full `mvn -q -DskipTests compile` is currently blocked by an unrelated framework signature mismatch in `WindowTaskRunner.markQueueFinished(...)` / `WindowRuntimeContext.markQueueFinished(...)`, owned by 何黎's active framework lane.

Open issues:

- Re-run full compile after the framework lane finishes reconciling `markQueueFinished(...)`.

Needs from others:

- 何黎: finish or reconcile the framework `markQueueFinished(...)` signature before full-project compile can pass.

### 何黎 - 2026-05-21 01:42

Status: completed

Goal:

- Expose queue failure policy through the backend start/submit APIs with thin overloads.
- Keep existing UI/default behavior unchanged.
- Avoid UI edits; leave actual policy selection for a later user/UI decision.

Owns:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- summon skill / auto battle files owned by 谢帅
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/control/WindowTaskStartRequest.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- Added thin policy-aware overloads to the backend start/submit chain:
  - `WindowTaskStartRequest.sameTask(..., failurePolicy)`
  - `WindowTaskControlService.startSameTask(..., failurePolicy)`
  - `MultiWindowTaskManager.submitWithResult(..., failurePolicy)`
- Existing default UI/control calls still use `CONTINUE_ON_FAILURE` through `WindowTaskQueue.single(...)`.
- No UI behavior or task behavior changed.

Changed files:

- `src/main/java/com/bot/dhxy/window/control/WindowTaskStartRequest.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI still does not expose failure policy selection. That is intentional for now.

### 何黎 - 2026-05-21 01:30

Status: completed

Goal:

- Add explicit queue failure policy scaffolding for `WindowTaskQueue`.
- Keep the current default behavior unchanged: task `FAILED` still lets later queued tasks continue, while `STOPPED` stops the queue.
- Make the policy visible in runner logs so future UI/task configuration can decide `CONTINUE_ON_FAILURE` vs `STOP_ON_FAILURE` without burying that rule in `WindowTaskRunner`.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskQueue.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskFailurePolicy.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

Update:

- Added `WindowTaskFailurePolicy` with `CONTINUE_ON_FAILURE` and `STOP_ON_FAILURE`.
- `WindowTaskQueue` now carries a failure policy; all existing constructors/factories default to `CONTINUE_ON_FAILURE`, so current queue behavior is unchanged.
- `WindowTaskRunner` now reads the queue policy when deciding whether `FAILED` should stop later queued tasks.
- Queue finish messages now include the active failure policy for diagnostics.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskFailurePolicy.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskQueue.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- No UI/control path selects `STOP_ON_FAILURE` yet. That should be wired only after we decide which queued tasks need fail-fast behavior.

### 唐德 - 2026-05-21 00:40

Status: active

Goal:

- Continue UI work autonomously within the UI lane.
- Improve the window control panel structure and display queue-level results now exposed by the framework.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:40 update

Status: completed

Done:

- Reorganized the JavaFX window panel into functional sections: window registration, supply config, task queue, window selection, and task control.
- Updated the table to display framework-provided queue-level result data:
  - `上次队列`
  - `队列结果`
- Updated selected-window detail to show last queue/result instead of only last single task/result.
- Added a `当前任务入队` button to quickly add the main task ComboBox value into the pending queue.
- Kept all changes UI-only; backend execution and queue semantics were not changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI is better grouped but still uses plain JavaFX controls; later visual polish can improve spacing/style.
- Queue presets are still in-memory only; persistent presets could be a later feature if useful.

Needs from others:

- none

### 何黎 - 2026-05-21 01:18 update

Status: completed

Done:

- Added queue-level result memory to `WindowRuntimeContext`.
- `WindowTaskRunner` now aggregates each submitted `WindowTaskQueue` into a final queue result after the batch finishes.
- The aggregation does not change current execution flow:
  - failed tasks still do not stop later tasks;
  - stopped tasks still stop the queue;
  - any failure makes the final queue result `FAILED`;
  - any stop makes the final queue result `STOPPED`;
  - otherwise successful work makes it `SUCCESS`.
- `WindowTaskSnapshot` now exposes last queue display/result/message for UI and diagnostics.

Changed files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI does not yet display last queue result/message. It can use the new snapshot getters later.
- Queue failure policy is still "continue after FAILED"; changing that should be a separate explicit decision.

Needs from others:

- none

### 唐德 - 2026-05-21 00:35

Status: active

Goal:

- Improve UI command log readability.
- Remove visible role/leader/member controls from the JavaFX UI where they are not useful to the user.
- Keep backend role fields and assignment logic untouched.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:35 update

Status: completed

Done:

- Removed visible role/leader/member UI controls from `MainWindowController`.
- Removed the `显示身份` table column.
- Removed the `测试按身份启动` button from the action row.
- Manual window registration now passes `WindowRole.UNKNOWN`; backend role fields and assignment logic remain untouched.
- Improved UI command log readability with a command summary line and clearer `成功` / `失败` detail prefixes.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Backend still contains deprecated/test role-assignment paths. They are no longer exposed in this UI, but framework cleanup should be owned by 何黎 if needed.
- UI log still does not consume structured `WindowTaskSubmitStatus` directly because `WindowTaskCommandDetail` currently exposes message text only.

Needs from others:

- none

### 何黎 - 2026-05-21 01:10

Status: active

Goal:

- Inspect queue-level execution result semantics.
- Decide whether framework needs an explicit queue-level result/status instead of exposing only the latest task result.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `src/main/java/com/bot/dhxy/window/runtime/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- TBD after scan; likely `WindowTaskRunner`, `WindowTaskSnapshot`, possibly `WindowRuntimeContext`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:35

Status: active

Goal:

- Move lightweight cleanup role gating away from `WindowRole`.
- Use backend team-role detection (`TeamRoleDetectionService`) as the single role decision entry point.

Owns:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- UI/window role assignment behavior
- `FiveRingTask.java`
- framework execution/control files owned by 何黎

Planned files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `src/main/java/com/bot/dhxy/config/TeamTaskProperties.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputAction.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionType.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/WinApiMouseController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:35 update

Status: completed

Done:

- Confirmed the existing backend entry point is `TeamRoleDetectionService`; window/UI role should not be the source of truth for real leader/member decisions.
- Added `TeamRoleDetectionService.shouldRunLightweightCleanup(...)` as the central rule for lightweight cleanup eligibility.
- Added team config switches:
  - `lightweightCleanupRequiresMember`, default `true`
  - `allowLightweightCleanupWhenRoleUnknown`, default `false`
- `NavigationService` now asks `TeamRoleDetectionService` instead of checking `WindowRole` directly.
- Added the first real team-role detection flow behind `roleDetectionEnabled=false`:
  - hover configured team area and inspect configured tooltip rect for white + purple pixels;
  - return `SOLO` when no team tooltip is detected;
  - press Alt+T and match configured transfer-leader template for `LEADER`;
  - match configured member marker template for `MEMBER`;
  - return `UNKNOWN` when neither leader nor member marker matches;
  - retry Alt+T panel detection according to `teamPanelRoleDetectionMaxAttempts`;
  - close the team panel with a single Alt+T after each panel probe.
- Added queued Alt+T input support.
- Added `bot.team` config keys in `application.yml`, including hover delay, hover random radius, panel open/close delay, retry count, and leader/member template rects.
- Renamed provided team templates to `transfer_leader_button.png` and `member_marker.png`.
- Filled tooltip detection rect from user coordinates: `(1672,510)-(1783,579)` relative to base `(992,386)` => `(680,124,w=111,h=69)`.
- Strengthened team tooltip detection from only white+purple pixels to white+purple plus text-like distribution checks:
  - colored pixels must span enough rows and columns;
  - rows must contain enough foreground/background transitions;
  - a single row cannot be mostly filled by one continuous color block.

Changed files:

- `src/main/java/com/bot/dhxy/config/TeamTaskProperties.java`
- `src/main/java/com/bot/dhxy/team/TeamRoleDetectionService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputAction.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionType.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/WinApiMouseController.java`
- `src/main/resources/application.yml`
- `images/template/team/transfer_leader_button.png`
- `images/template/team/member_marker.png`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Leader/member panel rects and templates are filled from the user's provided coordinates/templates.
- `roleDetectionEnabled` defaults to `false`, so behavior remains safe until coordinates/templates are filled.
- Tooltip text-distribution thresholds may need tuning from real debug logs.

Needs from others:

- none

### 唐德 - 2026-05-21 00:30

Status: active

Goal:

- Improve JavaFX UI control states for window/task actions.
- Disable or hint actions when selection/queue state makes them unusable.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:30 update

Status: completed

Done:

- Added an operation hint label to the JavaFX window panel.
- Start/stop/remove buttons now reflect the current table selection state.
- Queue controls now reflect the current pending queue size.
- `启动队列` is disabled when no window is selected or the pending queue is empty.
- During background window commands, controls remain disabled and the hint shows that a command is running.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- This is still functional gating only. Later visual polish should group controls and make disabled reasons more discoverable.

Needs from others:

- none

### 唐德 - 2026-05-21 00:27

Status: active

Goal:

- Use framework-provided queue acceptance state in the JavaFX UI.
- Show whether windows can accept task queues and warn before starting tasks on unavailable windows.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:05

Status: active

Goal:

- Treat `UICleanerService.cleanLightweightInterruptions(...)` as a role-agnostic cleanup.
- Wire it into low-risk navigation/movement waits before touching 五环 task logic.

Owns:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- framework execution/control files owned by 何黎
- UI files owned by 唐德

Planned files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 01:05 update

Status: completed

Done:

- Treated lightweight interruption cleanup as role-agnostic/common behavior.
- Wired `UICleanerService.cleanLightweightInterruptions(...)` into `NavigationService` movement/wait loops.
- Added per-window navigation throttling so lightweight cleanup runs at most once every 2500ms during navigation waits.
- Did not edit `FiveRingTask`; 五环 will benefit indirectly when it uses `NavigationService`.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game validation that business dialog handling during navigation does not interfere with target-map dialog selection.
- If navigation feels slower, tune `LIGHTWEIGHT_CLEAN_INTERVAL_MS`.

Needs from others:

- none

### 谢帅 - 2026-05-21 01:20 update

Status: completed

Done:

- Rechecked role logic: leader windows own main task routing, OCR dialogs, NPC/task progress; member windows own auto-battle, status maintenance, and simple popups.
- Adjusted `NavigationService` lightweight cleanup so navigation only runs it for explicit MEMBER windows.
- LEADER, UNKNOWN, and no-window-context navigation skip lightweight cleanup to avoid stealing task dialogs from leader OCR/business flow.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- If we later want UNKNOWN windows to behave like member windows, make that a deliberate config decision instead of the default.

Needs from others:

- none

### 唐德 - 2026-05-21 00:27 update

Status: completed

Done:

- Used `WindowTaskSnapshot.isAcceptingTaskQueue()` in the JavaFX UI.
- Added a `可接任务` table column.
- Added `可接任务` to the selected-window detail line.
- `启动当前任务`, `启动已选任务`, and `启动队列` now write a UI log warning when selected windows are not accepting task queues.
- The warning does not block submission; backend rules still decide the final command result.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI could later disable start buttons or split accepted/rejected windows, but this phase intentionally only warns.

Needs from others:

- none

### 唐德 - 2026-05-21 00:21

Status: active

Goal:

- Continue UI functionality without touching framework files currently owned by 何黎.
- Add a table filter for registered windows so multi-window debugging is easier.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:21 update

Status: completed

Done:

- Added a JavaFX table filter for registered windows.
- The window table can now show all/running/idle/bound/unbound windows.
- The window summary line now includes the visible row count after filtering.
- This is UI-only and does not change task execution, registration, or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Selection count currently reflects selected visible rows. Hidden rows are not acted on by UI commands, which is safer for filtered views.

Needs from others:

- none

### 唐德 - 2026-05-21 00:22

Status: active

Goal:

- Add problem-window selection/filter helpers to the UI.
- Keep this limited to snapshot display/selection state.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- framework files currently owned by 何黎
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:22 update

Status: completed

Done:

- Added an "异常/停止" window selection helper.
- Added an "异常/停止" table filter option.
- Both helpers are snapshot/UI-only and do not change task execution or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI controls are now functionally richer but visually crowded; visual grouping remains a later cleanup task.

Needs from others:

- none

### 何黎 - 2026-05-21 00:40

Status: active

Goal:

- Continue framework cleanup around structured queue submit results.
- Add machine-readable submit status so UI/logging does not need to parse message text.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitStatus.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 何黎 - 2026-05-21 00:45 update

Status: completed

Done:

- Added `WindowTaskSubmitStatus` for machine-readable queue submit outcomes.
- `WindowTaskSubmitResult` now carries a structured status while keeping old success/message/task getters.
- `MultiWindowTaskManager.submitQueueWithResult(...)` maps submit failures to explicit statuses:
  - `INVALID_WINDOW_ID`
  - `INVALID_QUEUE`
  - `WINDOW_NOT_REGISTERED`
  - `RUNNER_CLOSED`
  - `WINDOW_BUSY`
  - `SUBMIT_REJECTED`

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitStatus.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- UI does not use `WindowTaskSubmitStatus` yet; it can adopt it later for clearer warnings/disable states.

Needs from others:

- none

### 何黎 - 2026-05-21 00:30

Status: active

Goal:

- Continue framework cleanup around window task command result and queue-facing diagnostics.
- Check whether control/result models still expose only single-task semantics where queue semantics should be visible.

Owns:

- `src/main/java/com/bot/dhxy/window/control/*`
- `src/main/java/com/bot/dhxy/window/execution/*`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- TBD after scan; likely only control/execution result models if needed
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 00:35 update

Status: planning next phase

Decision:

- The old auto-battle project avoided handling many dialogs because five roles shared one game window and tab switching left little time for maintenance.
- DHXY now targets independent windows per role, so each window has enough time during movement, waiting, and idle polling to handle lightweight dialogs or close interfering windows.
- Known business dialogs such as 医宝宝 / 修装备 / 装备无需修理 should remain shared `DialogService` capabilities, not auto-battle-only logic.

### 谢帅 - 2026-05-21 00:45 update

Status: completed

Done:

- Kept lightweight maintenance inside the existing `UICleanerService` instead of adding another service entry point.
- Added `UICleanerService.cleanLightweightInterruptions(...)` for conservative movement/wait/idle cleanup.
- The lightweight cleanup path currently handles known business dialog options first, then closes safe generic windows.
- AutoBattleTask now calls this existing cleaner during idle maintenance.

Changed files:

- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game validation for the three business dialog templates and safe generic-window close behavior.
- FiveRingTask is not wired to this yet because 五环 core behavior is high conflict and should be coordinated before editing.

Needs from others:

- none

Next suggested work:

- Add a shared "lightweight dialog/window maintenance" entry point that tasks can call while moving or waiting.
- The first scope should be conservative: handle known business dialog options, close safe generic popups, and avoid broad fallback clicking.
- AutoBattleTask should keep using this maintenance path while FREE or moving/waiting.
- FiveRingTask and other tasks can later call the same maintenance path during navigation waits, after confirming it does not interfere with NPC/task dialogs.
- Keep all real clicks through `InputSequences`; any move+click remains one atomic sequence.

Recommended next implementation owner:

- 谢帅 can implement the shared maintenance service/API and wire it into AutoBattleTask first.
- If FiveRingTask integration is needed, coordinate before editing because 五环 core behavior is high conflict.

Needs from others:

- 何黎: no framework change needed right now, unless maintenance needs a standardized moving/waiting hook in the runner later.
- 唐德: no UI change needed right now.

### 唐德 - 2026-05-21 00:14

Status: active

Goal:

- Continue JavaFX UI functionality work.
- Add practical window selection helpers for multi-window debugging and control.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- framework execution/control files currently owned by 何黎

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:14 update

Status: completed

Done:

- Added UI selection helper buttons for registered windows.
- The window panel can now select running, idle, bound, or unbound windows directly from the table.
- These controls only update JavaFX table selection state; they do not start tasks, stop tasks, or send input.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Button row is getting crowded; visual grouping/polish should happen later after functional coverage is acceptable.

Needs from others:

- none

### 唐德 - 2026-05-21 00:15

Status: active

Goal:

- Add a direct "start current task" UI action for selected windows.
- Use existing backend `sameTask` request path without changing execution semantics.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- framework execution/control files currently owned by 何黎
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:15 update

Status: completed

Done:

- Added a "启动当前任务" button for selected windows.
- The action starts the task currently selected in the UI task ComboBox on selected windows.
- It uses the existing request path: `WindowTaskStartRequest.sameTask(...) -> WindowTaskControlService.start(...)`.
- No backend execution or task behavior changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The action row now has several start modes; later UI polish should group them by "current task", "saved selected task", "role test", and "queue".

Needs from others:

- none

### 何黎 - 2026-05-21 00:34 update

Status: completed

Done:

- Added explicit queue-acceptance state to the framework snapshot.
- `WindowTaskRunner.canAcceptTaskQueue()` now centralizes the current rule: a window accepts a new queue only when the runner is not shut down and no task is running.
- `WindowTaskSnapshot.isAcceptingTaskQueue()` exposes that rule to UI/readers without requiring them to inspect runner internals.
- `MultiWindowTaskManager.submitQueueWithResult(...)` now uses `canAcceptTaskQueue()` for the idle check.
- Removed an accidental UTF-8 BOM from `MultiWindowTaskManager.java` after compile caught it.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSnapshot.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- Current queue behavior is batch-submit only. Appending to a running queue is not implemented yet.

Needs from others:

- none

### 何黎 - 2026-05-21 00:20

Status: active

Goal:

- Continue framework cleanup around window task queue submission results.
- Make submit diagnostics queue-aware instead of only first-task-aware.

Owns:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 谢帅 - 2026-05-21 00:25 update

Status: completed

Done:

- Renamed the shared request factory to `DialogHandleRequest.handleBusinessOption(...)`.
- Clarified the intent: 医宝宝 / 修装备 / 装备无需修理 are shared `DialogService` capabilities available to any task.
- Kept auto battle as a narrow caller that only uses this shared business-option capability during idle maintenance.

Changed files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Other tasks can now call `dialogService.handleDialog(DialogHandleRequest.handleBusinessOption("task-name"))` when they want this same known-business-dialog behavior.

Needs from others:

- none

### 谢帅 - 2026-05-21 00:25

Status: active

Goal:

- Clarify that known business dialog options are a shared `DialogService` capability, not an auto-battle-only capability.
- Keep auto battle as a narrow caller that uses only this shared capability during idle maintenance.

Owns:

- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- framework execution/control files owned by 何黎
- UI files owned by 唐德
- 五环 core behavior

Planned files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 何黎 - 2026-05-21 00:25 update

Status: completed

Done:

- Made `WindowTaskSubmitResult` queue-aware while keeping first-task getters for compatibility.
- `MultiWindowTaskManager.submitQueueWithResult(...)` now returns the full submitted `WindowTaskQueue` in success/failure results.
- `WindowTaskControlService.startSameQueue(...)` now reports queue display text in command details.

Changed files:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskSubmitResult.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` passed with only CRLF warnings.

Open issues:

- `submitSelectedTaskWithResult(...)` still naturally wraps one selected task into a one-item queue.
- Framework does not yet support appending to a running window queue; current queue is submitted as one batch before execution starts.

Needs from others:

- none

### 唐德 - 2026-05-21 00:07

Status: active

Goal:

- Continue JavaFX UI functionality work for the window control panel.
- Add more useful runtime/result display without changing task execution behavior.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- framework execution/control files unless a small interface request is recorded first

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:07 update

Status: completed

Done:

- Added more runtime/result columns to the JavaFX window table.
- The table now shows last task, last result, last finished time, and bound native window title.
- The window summary label now includes selected window count.
- This is display-only and does not change task execution, registration, or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- The table is becoming wide; later visual polish should decide whether to split detailed binding/runtime info into a side panel or detail area.

Needs from others:

- none

### 唐德 - 2026-05-21 00:08

Status: active

Goal:

- Add a selected-window detail display to the JavaFX UI.
- Make native binding and last-run diagnostics visible without requiring more table columns.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- task execution behavior
- framework execution/control files
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:08 update

Status: completed

Done:

- Added a selected-window detail label to `MainWindowController`.
- Selecting a window now shows native title, hwnd, class name, process id, running queue, last task/result, and last message.
- Multiple selection shows the selected count and details for the first selected row.
- This is display-only and does not change backend registration, execution, or input behavior.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Detail display is still a compact text line; later UI polish may split it into structured fields.

Needs from others:

- none

### 谢帅 - 2026-05-21 00:10

Status: active

Goal:

- Add auto-battle business dialog handling through the existing `DialogService.handleDialog(...)` request/policy model.
- Keep the behavior auto-battle-scoped: only match the known business templates for 医宝宝 / 修装备 / 装备无需修理离开, and do not add broad fallback clicking to other task flows.

Owns:

- `src/main/java/com/bot/dhxy/service/dialog/*`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- related dialog templates under `images/template/dialog/`
- `docs/ACTIVE_WORK.md` for this work log

Avoids:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- framework execution/control files owned by 何黎
- UI files owned by 唐德

Planned files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionPolicy.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleResult.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `images/template/dialog/maintenance/heal_pet_option.png`
- `images/template/dialog/maintenance/repair_equipment_option.png`
- `images/template/dialog/maintenance/repair_equipment_option_giveup.png`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:01

Status: active

Goal:

- Continue JavaFX UI functionality work after reading the updated coordination rules.
- Keep changes focused on UI controls/status display and short-term coordination notes.
- Improve the existing task queue UI so it is safer and more useful before visual polish.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- UI table/button/status display
- `docs/ACTIVE_WORK.md` for this UI work log

Avoids:

- 五环 core behavior
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- framework execution/control files unless a small interface request is recorded first

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:01 update

Status: completed

Done:

- Added a UI button to batch-set the selected task for selected registered windows.
- The new action preserves each selected window's role, role name, and native binding by rebuilding `WindowRegistrationRequest` from `WindowTaskSnapshot`.
- The action uses the existing `WindowTaskControlService.registerWindows(...)` path; it does not bypass runner/framework rules.
- Running windows still follow the existing backend rule: `WindowTaskRunner.refreshRegistration(...)` only changes selected task when the runner is not running.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI task queue ordering is still minimal: add/remove/clear/start exists, but there are no move up/down controls yet.
- UI layout is still functional-first and not visually polished.

Needs from others:

- none

### 唐德 - 2026-05-21 00:02

Status: active

Goal:

- Improve the UI task queue builder with basic ordering controls.
- Keep this as UI-only behavior over the existing `pendingTaskQueue`.

Owns:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Avoids:

- task execution semantics
- framework execution/control files
- 五环 core behavior
- `SummonSkillService.java`
- `AutoBattleTask.java`

Planned files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Needs from others:

- none

### 唐德 - 2026-05-21 00:02 update

Status: completed

Done:

- Added up/down ordering controls to the UI task queue builder.
- Queue items can now be added, removed, moved up, moved down, cleared, and started on selected windows.
- Remove/move operations keep a useful queue selection when possible.
- This remains UI-only over `pendingTaskQueue`; no backend task execution behavior changed.

Changed files:

- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- UI layout is still functional-first and may need visual grouping/polish later.
- There is still no persisted task queue preset; queue is in-memory for the current UI session.

Needs from others:

- none

### 何黎 - 2026-05-20 update

Status: active

Done:

- Created this active coordination board.
- Added a long-term pointer from `docs/DHXY_CONTEXT.md` to this file.

Changed files:

- `docs/ACTIVE_WORK.md`
- `docs/DHXY_CONTEXT.md`

Validation:

- Documentation-only change; compile not required.

Open issues:

- Other active agents should add their own latest status entries here before further edits.

### 谢帅 - 2026-05-21 00:10 update

Status: completed

Done:

- Added auto-battle-scoped business dialog handling through `DialogService.handleDialog(...)`.
- Added a `CLICK_BUSINESS_OPTION` request/policy/result path for known auto-battle maintenance dialogs.
- `DialogService` now scans window-scoped `business_dialog_raw.png` / `business_dialog_washed.png` and matches heal-pet, repair-equipment, and repair-giveup templates in order.
- `AutoBattleTask` now calls `DialogHandleRequest.businessOption("auto-battle")` during idle maintenance, with no broad fallback option click.

Changed files:

- `src/main/java/com/bot/dhxy/service/dialog/DialogOptionPolicy.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogOperation.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleResult.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `docs/ACTIVE_WORK.md`

Validation:

- `mvn -q -DskipTests compile` passed.

Open issues:

- Needs in-game validation against the actual 医宝宝 / 修装备 / 装备无需修理 dialogs.
- Business option template threshold is currently `0.70`; tune only if logs/screenshots show misses or false positives.

Needs from others:

- none
### He Li - 2026-05-21 bag exclusive input fix

Goal:

- Diagnose five-window Wuhuan bag chaos where bags opened but later clicks landed on the map/wrong window.

Files changed:

- `src/main/java/com/bot/dhxy/service/BagService.java`

Result:

- Logs showed different windows reusing the same bag scan rect/tab click coordinates even when their window bases were different. Example: windows with bases around `(1405,441)` and `(1223,478)` still scanned/clicked around `(1206,755)-(1518,963)` / `(1534,806)`, which means bag anchor detection had been contaminated by another visible window.
- Root cause: bag operations were split into separate queue actions (`bag:openAltE`, `bag:switchTab`, `bag:itemAction`, `bag:closeAltE`) while screenshots/template matching happened between them. With five windows, another window could focus/cover the target between bag open, anchor detection, tab switch, scan, and item click.
- `BagService.findItemPageIndex(...)` and item actions now run as one `submitExclusiveAndWait(...)` transaction.
- Inside that exclusive transaction, bag open/close/tab click/item click use direct `InputProvider` calls, avoiding queue-in-queue deadlocks while keeping the whole bag workflow serialized for one window.
- Validation: `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify bag scan rects differ correctly by each window base and no longer reuse another window's bag anchor coordinates.

### He Li - 2026-05-21 navigation current-map resync

Goal:

- Diagnose why five-window Wuhuan still opened the world map for `长安` even when the characters were already in Chang'an.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`

Result:

- Latest logs showed the skip logic itself still works: the one window with `navigate to map: 长安 current=长安` skipped world-map navigation and went straight to `navigate in map`.
- Other windows logged `navigate to map: 长安 current=null`, so they had no current-map memory and fell through to `openMapInputTargetAndClickLastNavPoint`.
- `navigateToMap(...)` now performs one `playerStateService.syncMyPosition()` when `currentMapName` is null/blank, then re-checks the target map before opening the world map.
- Validation: `mvn -q -DskipTests compile` passed.

Open:

- If OCR/sync still returns null for specific windows, inspect their `tmp_pos.png` / coordinate strip images instead of changing navigation semantics.

### He Li - 2026-05-21 focused location capture fix

Goal:

- Fix the root cause behind five-window startup `syncMyPosition()` returning null: coordinate screenshots can be taken while another game window is covering the target window.

Files changed:

- `src/main/java/com/bot/dhxy/service/LocationVisionService.java`

Result:

- `LocationVisionService.scanCurrentLocation()` now captures the mini-map coordinate strip through `InputSequences.submitExclusiveAndWait(...)` whenever it is running inside a bound window task context.
- The input worker's existing window-aware transaction brings the bound window to front before the screenshot and prevents another physical input sequence from interleaving during capture.
- OCR parsing stays outside the focused input transaction so slow OCR does not hold the global input queue.
- Calls already running on `dhxy-input-action-worker` still use direct capture to avoid queue-in-queue deadlock.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should inspect each window-scoped `tmp_pos.png`; it should contain the actual mini-map coordinate strip for that hwnd instead of roof/map/other-window content.

### He Li - 2026-05-21 UI cleaner close-click atomic fix

Goal:

- Fix the case where a character reaches the NPC coordinate, then immediately gets moved away before NPC accept-click.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`

Result:

- Latest logs for `hwnd-B21276` / `『忍者』影` showed `arrived: (87, 174)` at 15:01:48, then `UICleanerService.cleanUpAll()` ran from `NavigationService.navigateToNPC()`.
- The better root cause is not "cleanup exists", but that generic close-button scan/click was split: one window could be captured/scanned while another window was focused for the later click.
- `navigateToNPC()` keeps the post-navigation cleanup behavior.
- `UICleanerService.clickCloseButtonOnce()` now runs screenshot, template match, and close click inside one `submitExclusiveAndWait(...)` transaction.
- Inside that transaction the click uses direct `InputProvider`, avoiding queue-in-queue and preventing another window from interleaving between finding the X and clicking it.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- If generic window cleanup still false-clicks elsewhere, `UICleanerService.closeAllGenericWindows()` needs a stricter window/header guard before accepting `x1/x2/x3` matches.

### He Li - 2026-05-21 location OCR concurrency fix

Goal:

- Address the real reason initial `syncMyPosition()` often left `currentMapName=null` during five-window startup.

Files changed:

- `src/main/java/com/bot/dhxy/core/TextRecognizer.java`

Result:

- Logs confirmed `syncMyPosition()` was called for each window and coordinate-region captures succeeded, but several windows still logged "radar could not read current position" and did not update `GameContext.State.me`.
- The failure pattern appears when several window task threads call the singleton Baidu `AipOcr` client concurrently during startup.
- `TextRecognizer` now serializes all direct Baidu OCR client calls (`basicGeneral` / `general`) through one lock so concurrent windows do not hit the shared OCR client at the same time.
- Validation: `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should show each startup `syncMyPosition()` either updating `currentMapName=长安` or, if still failing, leaving a screenshot/recognition issue to inspect per window rather than a concurrent OCR-client race.

### He Li - 2026-05-21 navigation and quest transaction tightening

Goal:

- Reduce five-window window-hopping by grouping more input-sensitive navigation/task-panel work into larger transactions.
- Add route-click diagnostics without changing the validated green-coordinate click target.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/window/interaction/WindowFocusService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `navigateToMap(...)` now checks movement/pathing intent before trying to handle the arrival dialog, so a just-clicked route link is allowed to start moving before dialog detection competes for foreground.
- Route coordinate clicking still uses the OCR-returned green coordinate link. It now logs `windowId`, bound hwnd, foreground hwnd, base, map rect, image path, relative point, and absolute point at click time.
- Wuhuan P1/P2 native pathing now runs as one exclusive task-panel transaction. Inside the transaction, panel opening, task activation, scrolling, P1/P2 click, and close use direct `InputProvider` calls to avoid splitting these steps across multiple queued requests.
- Added `WindowFocusService.getForegroundNativeHandleText()` for diagnostics.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should compare `boundHwnd` and `foregroundHwnd` in `navigation route coordinate click` logs when a route click appears visually wrong.

### He Li - 2026-05-21 dialog detect and route target click fix

Goal:

- Fix latest five-window Wuhuan issues where navigation dialogs were reported as `NO_DIALOG`, and one window clicked the wrong route result after typing Chang'an in the world map search.

Files changed:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `DialogService.detectDialogType()` now runs its dialog mask/option/story screenshots inside one `InputSequences.submitExclusiveAndWait(...)` transaction when called from task threads.
- Calls that are already inside the input worker fall through to a direct detector to avoid queue-in-queue deadlock.
- A temporary route-target-text click idea was removed after user review: yellow destination names are not clickable. Route clicking must remain on the OCR-returned green coordinate link.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify `navigation` dialog handling no longer returns `type=NONE result=NO_DIALOG` when the Chang'an option dialog is visibly open.

### He Li - 2026-05-21 route coordinate substring click fix

Goal:

- Fix route-result OCR clicking where the regex matched a green coordinate substring, but the actual click point used the center of the whole OCR text block.
- Clean up NavigationService route logs so the file no longer carries garbled navigation diagnostics.

Files changed:

- `src/main/java/com/bot/dhxy/core/TextRecognizer.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `TextRecognizer.findLastCoordinateLink(...)` now computes the click point from the regex-matched coordinate substring range within the OCR block, instead of clicking the center of the whole OCR block.
- The route log now prints `OCR coordinate match` with the matched text, block range, estimated coordinate substring range, and final relative point.
- `NavigationService` map-search logs were normalized to ASCII labels, and route-click diagnostics include `windowId`, bound hwnd, foreground hwnd, window base, map result rect, image path, relative point, and absolute point.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next route-click test should compare `OCR coordinate match ... point=(x,y)` with `navigation route coordinate click ... relative=(x,y)` and verify the clicked pixel lands on the green `(x,y)` coordinate text rather than the middle of the whole OCR sentence.

### He Li - 2026-05-21 navigation moving yield

Goal:

- Reduce five-window focus thrashing while a character is already auto-pathing.

Files changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Result:

- `navigateToMap(...)` now checks `GameStateUtil.detectMovementState()` before dialog handling.
- If the current window is `MOVING`, `PATHING_ACTIVE`, or `MAYBE_MOVING`, navigation resets the stuck counter, logs a yield message, sleeps for `1500ms`, and lets other windows use the input queue.
- Dialog handling, location OCR, and route retries now happen only after movement is no longer active.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next multi-window test should watch whether focus switching drops during long auto-pathing sections. If it is still too chatty, the next knob is `MOVING_NAVIGATION_YIELD_MS`.

### He Li - 2026-05-21 wuhuan task sync pathing transaction

Goal:

- Prevent another window from interleaving between "check/activate Wuhuan task" and "click P2/P1 pathing".
- After initial task accept or task sync, push the current window into a pathing/movement-intent state before yielding control.

Files changed:

- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Result:

- Added `QuestManagerService.activateAndTriggerWuHuanPathing()`.
- The new method runs one exclusive transaction: activate Wuhuan task, try P2 pathing, then try P1 pathing if P2 is unavailable.
- `FiveRingTask` now uses this combined transaction after initial task accept and whenever `needTaskSync=true`.
- Normal loop pathing also uses the combined transaction, so P2 failure and P1 fallback are no longer split across two input queue turns.
- Successful combined pathing records movement intent (`wuhuan:syncPathing`, `wuhuan:syncPathingAfterCleanup`, or `wuhuan:combinedPathing`) before yielding.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify that after a character accepts Wuhuan or checks the task panel, it reaches a movement/pathing intent before other windows visually take over.

### He Li - 2026-05-21 wuhuan handover task detection transaction

Goal:

- Fix five-window startup where characters that already had Wuhuan still reported "task not found" and went back to initial task setup.
- Keep startup handover detection consistent with the newer exclusive task-panel transaction style.
- Do not yield after merely opening/checking the task panel; if the task exists, trigger P2/P1 pathing before handing control to another window.

Files changed:

- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Result:

- Added `QuestManagerService.activateTaskIfPresentExclusive(task, keepOpen)`, which wraps the existing direct task-panel scanner in one `submitExclusiveAndWait(...)`.
- `FiveRingTask.detectHandover(...)` now uses `activateAndTriggerWuHuanPathing()` as the main handover path, so an existing Wuhuan task is activated and immediately pushed into P2/P1 pathing before the task yields.
- Successful handover pathing records movement intent as `wuhuan:handoverPathing`.
- The two "confirm task after accepting initial dialog" checks now also call `activateAndTriggerWuHuanPathing()` instead of merely confirming the task exists.
- Successful initial accept pathing records movement intent as `wuhuan:initialAcceptPathing` or `wuhuan:currentScreenAcceptPathing`.
- Initial accept is now a single exclusive transaction: verify/click Wuhuan accept dialog, wait for server response, activate the task panel, trigger P2/P1, and briefly wait for pathing to start before yielding.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window test should verify that windows with an existing Wuhuan task enter handover/takeover mode instead of returning to initial NPC setup.

### He Li - 2026-05-21 wuhuan dialog/navigation transaction tightening

Goal:

- Scan for more five-window interleaving risks beyond the shoe give-item path.
- Keep validated Wuhuan business decisions unchanged; only tighten input transaction boundaries.

Files changed:

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/GiveItemService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Result:

- Wuhuan give-item now runs as one exclusive input transaction: detect give option, click give option, select shoe, click give button, then immediately trigger P2/P1 pathing before yielding.
- Generic give-item handling also no longer splits "click give option" and "select/click item" across separate queued input turns.
- Wuhuan story dialog handling during the same dialog checkpoint can now be clicked inside the same dialog transaction instead of detecting once and then re-detecting in a later input turn.
- Navigation cached route reclick now opens the world map/search UI, clicks the cached green coordinate point, closes the world-map search UI, and records movement intent inside one exclusive input transaction.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Next five-window Wuhuan test should watch whether give-item/story-dialog/task-panel phases now run to a movement/pathing state before another window takes over.

### He Li - 2026-05-21 Wuhuan turn-yield audit

Status: completed

Goal:

- Audit Wuhuan task-turn boundaries after the five-window run where one window kept the turn while already pathing.
- Fix both directions: pathing must release, but cleanup/retry confirmation must not continue doing input after a premature release.

Changed files:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Wuhuan movement intent is now recorded inside the pathing transaction before the transaction releases the task turn.
- Handover, initial accept, give-item, task-sync, and combined P2/P1 pathing follow the same order: trigger pathing, record movement intent, return `PATHING_STARTED`, then release turn.
- Empty task-panel verification no longer releases as `TASK_FINISHED` before the confirm-cleanup pass. First empty result keeps the turn, runs cleanup as `READY_TO_CONTINUE + CONTINUE_CHAIN`, then retries task-panel pathing; only the second confirmed empty result can finish and release.
- UI cleanup after repeated task-panel/pathing errors is wrapped as `RETRYABLE_ERROR + RETRY_LATER` so it reacquires safely and then yields.
- Navigation failure exits force-release any held task turn so a failed map/current-map navigation cannot hold the global task turn forever.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Legacy unused Wuhuan P1/P2 helper methods still exist near the bottom of `FiveRingTask`; current flow uses `activateAndTriggerWuHuanPathingDirectForExclusive()` instead. They should be removed later when the file is cleaned, but they are no longer part of the active path.

### Xie Shuai - 2026-05-22 Auto8 quiet patrol rules

Status: completed

Goal:

- Apply the agreed Auto8 behavior rules so member auto-battle windows stay quiet outside explicit maintenance actions.

Changed files:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `docs/ACTIVE_WORK.md`

Done:

- Auto8 no longer focuses the game window at task startup.
- FREE-state Auto8 patrol uses a fixed 3 second interval.
- FREE-state patrol handles only maintenance broadcast business options through `UICleanerService.handleMaintenanceBroadcast(...)`.
- Maintenance broadcast matching includes 医保宝 / 修装备 style options and deliberately excludes 放弃修理.
- Generic close-window cleanup is no longer part of FREE-state Auto8 patrol.
- Summon skill cleanup remains lower priority than broadcast handling and still updates cooldown only after success.
- Combat-state generic window cleanup is throttled to 40 seconds inside Auto8 maintenance.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Post-combat HP/MP supply is still owned by `PlayerStateService`; batching one window's full person/pet supply before releasing input is the next supply-line cleanup if logs show interleaving.

### Xie Shuai - 2026-05-22 Battle radar timing split

Status: completed

Goal:

- Move battle-entry timing decisions out of `BattleRadarService` so the radar stays closer to detection/state signaling and Auto8 owns its own combat maintenance schedule.

Changed files:

- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- `BattleRadarService.checkAndSyncCombatState()` no longer accepts/runs a free-state first-aid option.
- `BattleRadarService` no longer sleeps/cleans generic windows/auto-aligns the combat panel immediately inside `onEnterCombat()`.
- Battle enter is now exposed as `consumeCombatEnterSignal()`.
- Auto8 consumes the battle-enter signal, waits 4 seconds, then performs one entry maintenance pass: generic window close plus auto-combat panel verify/align.
- Auto8 retains the 40 second combat generic-window cleanup throttle after that.
- FiveRing now uses the simplified `checkAndSyncCombatState()` signature; its post-combat supply path remains task-owned.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Superseded by "Auto-combat panel split" below: panel helpers have been moved out of `BattleRadarService`.

### Xie Shuai - 2026-05-22 Unified auto-combat state service

Status: completed

Goal:

- Make "auto combat" a shared state capability instead of behavior owned by the standalone AutoBattle task.

Changed files:

- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `AutoCombatService` as the shared combat-state automation layer.
- `AutoCombatService` owns per-window combat maintenance timing:
  - battle-enter signal consumption;
  - 4 second delayed entry maintenance;
  - generic window cleanup during combat;
  - auto-combat panel verify/align;
  - refresh interval from `BotProperties`;
  - battle-exit recovery with first-aid;
  - optional leader-task sheyaoxiang check after combat.
- `AutoBattleTask` is now a thin hanging/free-patrol task:
  - it delegates combat state/maintenance/recovery to `AutoCombatService`;
  - it keeps only FREE-state member patrol work such as return-team, maintenance broadcast, and summon skill maintenance.
- `FiveRingTask` now delegates its combat phase to `AutoCombatService`.
  - On unified combat exit recovery, Wuhuan sets `needTaskSync=true` and resumes its own task-panel/P2/P1 logic.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- `BattleRadarService` still exposes panel verify/align helpers used by `AutoCombatService`. A later cleanup can move those helpers fully into `AutoCombatService` or a dedicated panel service.

### Xie Shuai - 2026-05-22 Auto-combat panel split

Status: completed

Goal:

- Finish the responsibility split so `BattleRadarService` does not own auto-combat panel behavior.

Changed files:

- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `AutoCombatPanelService`.
- Moved auto-combat panel template detection, Alt+8 opening, drag alignment, and panel-round estimate state out of `BattleRadarService`.
- `AutoCombatService` now calls `AutoCombatPanelService.verifyAndAlignPanel()` for entry maintenance and refresh maintenance.
- `AutoCombatService` now calls `AutoCombatPanelService.recordCombatExit()` when unified combat exit recovery runs.
- `BattleRadarService` now keeps only combat detection/state signaling and polling interval logic.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- `BattleRadarService.getDynamicPollingIntervalMs()` still lives in radar. We can later move polling policy into `AutoCombatService` if we want all timing policy outside radar as well.

### Xie Shuai - 2026-05-22 Post-combat supply batching

Status: completed

Goal:

- Make one window finish its whole post-combat HP/MP supply pass before another window can interleave physical input.

Changed files:

- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `docs/ACTIVE_WORK.md`

Done:

- `PlayerStateService.healAll()` now runs the full person/pet HP/MP supply pass inside one `InputSequences.submitExclusiveAndWait("playerState:healAll", ...)` transaction.
- Inside that exclusive transaction, supply uses direct `InputProvider` mouse movement/right-clicks instead of nested `submitAndWait(...)`, avoiding input-queue deadlock.
- The full batch now covers:
  - moving the mouse away before bar screenshots;
  - initial bar snapshot;
  - secondary confirmation snapshots;
  - all needed person HP/MP and pet HP/MP right-click supply actions.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Runtime logs should confirm fewer interleaved `playerState:heal:*` actions because the visible queue item is now the single `playerState:healAll` transaction.

### He Li - 2026-05-22 Wuhuan combat Alt+Q audit

Status: completed

Goal:

- Diagnose the latest five-window Wuhuan run where several windows appeared to press `Alt+Q` after entering combat.
- Check why `忆叶知秋` appeared to wait at startup.

Findings:

- `忆叶知秋` did not deadlock. It waited for the global task turn from `00:08:32` to `00:09:20` because two earlier windows held the turn through prepare/handover/navigation until pathing. This matches the current "keep turn until pathing" rule, but it makes five-window startup visibly serial.
- The original "P1/P2 immediately triggered combat" explanation was too broad. Wuhuan usually has a visible pathing interval between clicking the task link and entering combat.
- The stronger root cause is `BattleRadarService`: after two missing combat detections it can mark `IN_COMBAT -> FREE`, then Wuhuan consumes the combat-exit signal and opens the task panel with `Alt+Q` while the user still visually sees combat.
- Wuhuan also allowed `BattleRadarService.checkAndSyncCombatState()` to run free-state first aid before Wuhuan's own post-combat recovery consumed the combat-exit signal, causing duplicate post-combat supply checks.

Changed files:

- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Removed the success-path `Alt+Q` close from Wuhuan P1/P2 pathing clicks. Failure/cleanup paths still close the task panel explicitly.
- Wuhuan now calls `battleRadarService.checkAndSyncCombatState(false)` and lets `wuhuan:postCombatRecovery` own post-combat first-aid/supply work.
- Battle exit now requires both repeated missing battle signals and a readable minimap coordinate, so a temporary loss of combat templates alone will not let Wuhuan open the task panel during combat.

Validation:

- `mvn -q -DskipTests compile` passed.

Open:

- Five-window startup is still intentionally serial until each window reaches pathing. If this feels too slow, the next design task is to split safe startup checks from input-sensitive preparation without reintroducing bag/window crossing.

### Xie Shuai - 2026-05-22 Summon skill exclusive cleanup

Status: completed

Goal:

- Ensure summon skill cleanup keeps the physical input permission for the whole open/inspect/delete/confirm pass, so other windows cannot interleave while it is maintaining summon skills.

Changed files:

- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `docs/ACTIVE_WORK.md`

Done:

- `cleanSummonSkillsOnce()` now wraps the whole cleanup pass in one `InputSequences.submitExclusiveAndWait("summonSkill:cleanOnce", ...)` transaction.
- Substeps now detect when they are already running on the input worker and call direct `InputProvider` operations instead of nesting another queue request.
- This direct-when-owned path covers panel open, extra-slot hover, skill-slot inspect, delete button click, and forget-confirm click.
- `uiCleanerService.cleanUpAll()` runs only after the summon cleanup transaction releases the input queue.
- Failure still returns `false`, so `AutoBattleTask` will not update the summon-clean timestamp and the next idle maintenance round can retry.
- Added a 40-second total deadline for one summon cleanup pass. If the business flow exceeds that deadline at a check point, it aborts with `false`, releases the exclusive input transaction, and leaves the cooldown timestamp untouched for retry.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-22 Team return service split

Status: completed

Goal:

- Split return-team handling out of `AutoBattleTask` and make the leader/member behavior explicit.

Changed files:

- `src/main/java/com/bot/dhxy/service/TeamReturnService.java`
- `src/main/java/com/bot/dhxy/config/BotProperties.java`
- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/resources/application.properties`
- `docs/ACTIVE_WORK.md`

Done:

- Added `TeamReturnService` as the shared team-maintenance capability for return-team handling; member clicks use `images/template/status/gui.png`, while leader-side wait detection uses `images/template/status/zhao.png`.
- Member/auto-battle behavior: if the return signal is present, click it through the input queue.
- Leader behavior: the return signal is only checked at task-defined safe points. The leader does not click the return button and does not release the task turn while waiting.
- Leader wait timing is configurable with `bot.dhxy.return-team-leader-wait-timeout-ms` and `bot.dhxy.return-team-leader-wait-poll-ms`.
- `AutoBattleTask` now delegates return-team handling to `TeamReturnService` instead of owning template/coordinate logic directly.
- Removed the generic `FiveRingTask` leader wait check because it could wait in the wrong location after battle.
- `XiuluoTask` now checks and waits for member return after the return item succeeds, which is the first safe point after returning to town.

Validation:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-05-26 停止后异常状态未清理

Status: completed

Goal:

- 处理 UI 中某个窗口任务已经失败结束后，用户点停止仍一直显示“异常”的状态语义问题。

Log finding:

- 最新 `logs/dhxy-console.log` 显示 `hwnd-3300F7A / 刑部ㄨ忍者` 在 00:08:35 已经结束：`window [hwnd-3300F7A] task finished: 修罗 -> FAILED`。
- 失败原因是 `navigateInCurrentMap:retry` 五个候选点击都没有移动，随后 `generic navigation to objective failed`。
- 00:09:18 用户点停止时，另外 4 个仍在自动战斗的队员立即 `STOPPED`；队长已经没有 active task，所以没有新的 task stopped 日志。
- 因此不是“停不下来”，而是失败后的 terminal `ERROR` 状态没有被停止命令清成 `STOPPED`。

Changed files:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `docs/ACTIVE_WORK.md`

Done:

- 新增 `WindowRuntimeContext.markStoppedAfterTerminalStop(...)`。
- `WindowTaskRunner.stopCurrentTask()` 在没有 active task、但窗口处于 `ERROR` 或 `STOPPING` 时，会把窗口级状态改成 `STOPPED`。
- 保留原来的 `lastResult=FAILED` 和失败信息，方便详情面板继续看见上一次为什么异常。

Validation:

- `git diff --check` passed for touched Java files.
- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-05-25 修罗停止卡在目标文字识别

Status: completed

Goal:

- 处理 UI 点“停止”后修罗仍要等很久才真正停止的问题。
- 保持目标文字识别算法不变，只让长时间本地模板扫描能响应任务 stop token。

Log finding:

- `logs/dhxy-console.log` 显示 21:10:34 UI 已经请求停止 `hwnd-3300F7A`，`XiuluoTask` 也收到 stop requested。
- 任务直到 21:12:18 才退出，期间卡在 `ObjectiveTextRecognitionService` 的 objective map/template scan。
- 中间用户多次点启动，UI 正确刷新并过滤 busy/not accepting 窗口，所以表现成“启动没反应/次数没刷新”。

Changed files:

- `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`
- `docs/ACTIVE_WORK.md`

Done:

- `ObjectiveTextRecognitionService` 注入 `TaskExecutionContextHolder`。
- 在 objective 识别入口、地图模板扫描、坐标模板扫描、前景裁剪、glyph trim、foreground similarity 等长循环里加入 cooperative stop checkpoint。
- `TaskStopRequestedException` 不再被目标识别的 `catch (Exception)` 当普通识别失败吞掉，而是记录 stopped 日志后重新抛给 runner。
- 未修改修罗业务流程、目标识别阈值、模板匹配算法或窗口启动逻辑。

Validation:

- `git diff --check -- src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java` passed.
- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 NavigationService task-turn comments

Status: completed

Goal:

- Explain confusing high-frequency task-turn helpers in `NavigationService`.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added JavaDoc for `ensureTaskTurn(String source)` explaining that it acquires the task-level turn, not the physical input queue.
- Added JavaDoc for `releaseTaskTurnAfterPathing(String source)` explaining why navigation releases ownership once game auto-pathing starts.
- Clarified that later focused/state-mutating navigation actions must call `ensureTaskTurn(...)` to re-enter the business turn.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/service/NavigationService.java` passed.

### Tang De - 2026-05-26 NavigationService remove map internal wrapper

Status: completed

Goal:

- Remove the redundant `navigateToMapInternal(...)` wrapper split because both methods were private and had identical parameters.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Merged `navigateToMapInternal(String, boolean)` back into `navigateToMap(String, boolean)`.
- Kept latency tracking in `navigateToMap(...)` using `try/finally`, so early returns still emit `navigation.toMap` metrics.
- Preserved the existing navigation stages and comments; no route/search/retry behavior was changed.

Validation:

- `rg navigateToMapInternal` shows no remaining method/call.
- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/service/NavigationService.java` passed.

### Tang De - 2026-05-26 main-method input JavaDoc rule

Status: completed

Goal:

- Clarify the lightweight comment policy: high-frequency/main methods still need proper top-level input/output documentation.

Changed files:

- `AGENTS.md`
- `docs/ACTIVE_WORK.md`

Done:

- Added a mandatory rule that main/high-frequency methods must have JavaDoc explaining inputs and output.
- For each parameter, agents must state what it represents and include coordinate space, unit, and nullability when relevant.
- This is explicitly mandatory for navigation, OCR, input, window binding, task execution, and UI command entry methods.
- The broader policy remains lightweight: trivial helpers and obvious UI plumbing do not need forced heavy comments.

### Tang De - 2026-05-26 NavigationService high-frequency comments

Status: completed

Goal:

- Add concise internal comments to high-frequency navigation paths without changing behavior.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Documented the map-and-coordinate navigation stage split: cross-map route, current-map coordinate click, and arrival cleanup.
- Documented the `navigateToMapInternal(...)` loop stages: cached-map fast path, one-time unknown-map sync, first world-map route submission, movement wait/yield, dialog handling, OCR arrival check, and stuck retry policy.
- Kept the wrapper/internal separation intact; no navigation logic was changed.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/service/NavigationService.java` passed.

### Tang De - 2026-05-26 FiveRingTask mojibake cleanup

Status: completed

Goal:

- Remove the remaining mojibake from the whole FiveRingTask file after the LocationVisionService cleanup.
- Keep the change limited to readable strings/log messages and avoid task-flow changes.

Changed files:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Replaced garbled task/map/NPC display strings with readable text: `五环`, `长安`, `云游大师`.
- Rewrote garbled log lines and step display names into readable Chinese/English diagnostics.
- Removed broken emoji/mojibake fragments such as `馃`, `鈿`, `鈻`, `宺esult`, and `歳eason`.
- Re-scanned the full file for common mojibake patterns and found no remaining matches.

Validation:

- `mvn -q -DskipTests compile` passed.

### Tang De - 2026-05-26 LocationVisionService cleanup

Status: completed

Goal:

- Clean up mojibake comments/log-adjacent text in `LocationVisionService`.
- Move public API members above private helpers so the file reads public-first, private-detail-second.

Changed files:

- `src/main/java/com/bot/dhxy/vision/LocationVisionService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Fixed garbled Chinese comments in the class header, floor-template verification notes, and player-anchor width comment.
- Corrected the dungeon floor map matcher from garbled text to `.*[一二三四五六七八九十]+层$`.
- Moved `extractPlayerPhysicalAnchor(...)`, `extractPlayerAnchorMatch(...)`, and public record `PlayerAnchorMatch` above the private helper section.
- Removed redundant same-package imports.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src/main/java/com/bot/dhxy/vision/LocationVisionService.java` passed.

### Tang De - 2026-05-26 lightweight code comment policy

Status: completed

Goal:

- Temporarily reduce comment/JavaDoc weight so agents can spend less context and token budget on obvious code while still documenting risky automation behavior.

Changed files:

- `AGENTS.md`
- `docs/ACTIVE_WORK.md`

Done:

- Replaced the previous heavy documentation rule with a lightweight policy.
- Public JavaDoc is now required only when behavior is non-obvious, externally reused, or safety-sensitive.
- Mandatory comments remain for input/focus/HWND binding, OCR/template fallback order, stop/pause/transaction behavior, config/debug switches, persisted formats, and coordinate-space conversions.
- Complex methods should have concise decision-point comments, not full SOP-style narration for every branch.
- Agents should document only the touched risky section and avoid broad unrelated documentation passes.

### Tang De - 2026-05-31 Alt+A direct combat fallback

Status: completed

Goal:

- Add a 修罗 combat-target fallback for monsters whose tooltip/dialog trigger is blocked by fixed game UI or screen-edge layout.
- Reuse the existing `NpcClickService.clickNpcSmart(...)` targeting strategy instead of creating a second NPC-click algorithm.

Changed files:

- `src/main/java/com/bot/dhxy/input/action/InputActionType.java`
- `src/main/java/com/bot/dhxy/input/action/InputAction.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/driver/WinApiMouseController.java`
- `src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added queued/background-capable `Alt+A` input support.
- Added `NpcClickService.tryDirectCombatTargetClick(...)`.
- The new method presses `Alt+A`, then runs the same learned-memory, tooltip, player-anchor formula, yellow OCR, and Ctrl-menu pipeline used by `clickNpcSmart(...)`.
- Normal `clickNpcSmart(...)` still verifies the expected dialog; direct-combat fallback verifies by `BattleRadarService.checkAndSyncCombatState()`.
- Direct-combat mode probe now avoids side-effect probes such as `Alt+E`.
- `GameStateUtil.isDirectCombatClickModeLikely(...)` now uses an AND check:
  - mini-map coordinate digit reader cannot read the coordinate;
  - top-right HP/MP bars are not visible.
- `PlayerStateService.areStatusBarsVisibleNoFocus(...)` captures only the small HP/MP strip and counts red/blue bar pixels. It does not move the mouse, open UI, heal, or run OCR.
- If direct-combat clicks fail normally, `NpcClickService` right-clicks near the current purple/player anchor to exit the mode. If the task is stopped/interrupted, it does not perform cleanup, per user preference.
- Exit is now verified with `GameStateUtil.isDirectCombatClickModeLikely(...)` after each right-click. It retries the exit click up to 3 times; if the mode still appears active, the service aborts follow-up cleanup/retry instead of continuing while stuck in Alt+A mode.
- `XiuluoTaskV2.recoverTargetClickFailure(...)` now tries the direct-combat fallback after the normal template/OCR "看打!" dialog recovery misses and before UI cleanup/retry.

Validation:

- `mvn -q -DskipTests compile` passed.

Next:

- Run 修罗目标点击 on a blocked/edge monster case and confirm logs show `NPC direct-combat click mode entered`, direct-combat verification attempts, and either battle radar success or right-click exit.
- If mode detection is too strict/loose, tune only the status-bar pixel thresholds or mini-map readability probe; do not add package-opening probes.

### Tang De - 2026-06-02 Local OCR startup gate for task start

Status: completed

Goal:

- Prevent task startup from controlling game windows when the local OCR sidecar is not healthy.
- Fix the observed 五环 loop where world-map search already showed `长安`, but local OCR was unavailable, so the route guard read an empty destination and repeatedly closed/retyped the search input.

Changed files:

- `src/main/java/com/bot/dhxy/ui/LocalOcrSidecarService.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `docs/ACTIVE_WORK.md`

Done:

- `LocalOcrSidecarService` now exposes `ensureRunningBlocking()`, waiting up to 60 seconds for `/health`.
- Main task start, queue start, and per-window start now run an OCR readiness gate inside the background UI worker before scanning/registering windows or submitting tasks.
- If local OCR is not healthy, the command returns a clear failure message and does not touch game windows.
- The old UI message saying local OCR startup "does not block window startup" was replaced with a startup-gate message.

Validation:

- `mvn -q -DskipTests compile` passed.

Notes:

- Latest log showed the OCR sidecar was requested at `2026-06-02 00:09:15`, but the previous async warmup timed out after 12 seconds while tasks continued anyway.
- The route failure itself was not a bad map result: archived `raw.png` visibly contained `长安`; the failure was caused by OCR unavailable and the guard returning `actual=` blank.

### Tang De - 2026-05-26 Xiuluo stop during location OCR

Status: completed

Goal:

- Diagnose why one selected Xiuluo window still took several seconds to stop after the UI stop command.
- Prevent a normal user stop from being reported as a task failure when it happens inside the Xiuluo objective-prepare transaction.

Log finding:

- Latest log showed the stop request at `00:56:31.371`.
- The slow window was the Xiuluo leader `hwnd-3300F7A / 刑部ㄨ忍者`.
- It exited at `00:56:37.689` after `LocationVisionService.scanCurrentLocation()` finished mini-map template, local OCR, and Baidu OCR fallback.
- Root cause: location scanning had no cooperative stop checkpoints before the slow OCR fallback stages, and the stopped transaction was later mapped to a generic Xiuluo hot-start failure.

Changed files:

- `src/main/java/com/bot/dhxy/vision/LocationVisionService.java`
- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added task stop checkpoints to `LocationVisionService.scanCurrentLocation()` before/after mini-map template scanning, coordinate-strip capture, local OCR, and Baidu OCR.
- Added an explicit checkpoint before Baidu OCR so a user stop does not enter the slow network/token fallback after stop has already been requested.
- Re-throw `TaskStopRequestedException` from the mini-map template helper instead of swallowing it as a generic template miss.
- Added a `STOPPED` Xiuluo hot-start state so `xiuluo:prepareObjectiveForPathing` can preserve `TaskTransactionResult.STOPPED` and return `TaskRunResult.STOPPED` instead of `FAILED`.

Validation:

- `mvn -q -DskipTests compile` passed.

### Xie Shuai - 2026-05-23 Xiuluo return map-label verification

Status: completed

Goal:

- Confirm the Xiuluo leader has actually returned to town before checking the return-team signal.
- Reuse the mini-map coordinate strip capture, but compare the washed map-name label image instead of OCR or coordinate movement.

Changed files:

- `src/main/java/com/bot/dhxy/task/XiuluoTask.java`
- `src/main/java/com/bot/dhxy/tools/GameStateUtil.java`
- `src/main/java/com/bot/dhxy/service/MiniMapCoordinateReader.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/config/BotProperties.java`
- `src/main/resources/application.properties`
- `docs/ACTIVE_WORK.md`

Done:

- `GameStateUtil.detectMovementState()` already uses `MiniMapCoordinateReader.readCurrentCoordinate()` first; the old pixel diff is only a fallback when coordinate samples are insufficient.
- `MiniMapCoordinateReader.readCurrentMapLabelImage()` now returns a washed binary image of the mini-map coordinate strip's map-name label, cropped before the coordinate bracket.
- `GameStateUtil` now owns the reusable map-label verification helpers:
  - `captureCurrentMapLabelSnapshot(...)`
  - `isCurrentMapLabelChangedFrom(...)`
- `XiuluoTask.useReturnItem(...)` now reads a map-name label baseline through `GameStateUtil` before opening the bag and using the return item.
- Before reading the baseline, Xiuluo verifies it is no longer in combat and the main bag is not open.
- After the return item is used, Xiuluo polls the washed map-name label image until it differs from the baseline, then treats return-to-town as complete.
- This avoids OCR cost and avoids false success from a small accidental coordinate movement.
- During the post-return polling, if the main bag is still open, that sample is skipped so bag UI cannot pollute the location strip.
- If the map label does not change within the configured timeout, the return item step returns a retryable error instead of continuing to member-return waiting.
- Return verification now only needs timing config:
  - `bot.dhxy.xiuluo-return-verify-timeout-ms`
  - `bot.dhxy.xiuluo-return-verify-poll-ms`

Validation:

- `mvn -q -DskipTests compile` passed.

Follow-up:

- User observed auto-battle member windows stealing foreground during idle patrol.
- Log root cause: `UICleanerService.handleMaintenanceBroadcast(...)` called `DialogService.handleDialog(...)` every patrol, and `handleDialog` used queued/focused `dialog:detectType` even when no dialog existed.
- Added a no-focus precheck with `dialogService.detectDialogTypeNoFocus(...)`; auto-battle now only enters focused/click-capable dialog handling when a dialog is actually visible.
- `mvn -q -DskipTests compile` passed after this change.
- User clarified the real issue was the Xiuluo leader releasing the task turn before the agreed formal movement point.
- Log root cause: Xiuluo used generic `NavigationService.navigateToNPC(...)` while going to the accept NPC; generic navigation releases the task turn whenever pathing starts.
- Added `NavigationService.navigateToNPCWithoutTurnRelease(...)` and wired Xiuluo accept-NPC navigation to it, preserving old release behavior for other tasks.
- Xiuluo current-screen accept precheck now uses no-focus dialog detection before opening a transaction, so the normal "no current dialog" case no longer creates a failed transaction that can release the task turn.
- `mvn -q -DskipTests compile` passed after this change.

### He Li - 2026-05-22 Screenshot focus binding

Status: completed

Goal:

- Fix the root cause where multi-window `Robot` screenshots could capture another visible window even when the logical `windowId/base/hwnd` belonged to the current task window.

Changed files:

- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `docs/ACTIVE_WORK.md`

Done:

- `GameClientTracker.captureToFile(...)` and `captureToMemory(...)` now focus the current bound window inside the same `GlobalInputLock` immediately before calling the `Robot` screenshot provider.
- If the foreground hwnd is still not the current bound window after the focus attempt, the screenshot fails with `FOCUS_NOT_CONFIRMED` instead of capturing polluted visible pixels.
- Capture logs now include the current foreground hwnd, making it visible when a screenshot was taken while another window was still foreground.
- Tracker diagnostics now write `action=capture-focus` with expected hwnd, foreground before/after, and focus confirmation.
- BattleRadar now treats battle-region screenshot failure while already in `IN_COMBAT` as "evidence unavailable, keep combat state" so stale or polluted images cannot trigger a false combat exit.

Why:

- Input actions were already serialized and focused, but screenshots were only serialized by the global lock. Since the screenshot provider captures visible screen pixels, a different game window could cover the target window and pollute BattleRadar/minimap/task-panel scans.

Validation:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-02 五倍战斗等待暂停计时修正

Status: completed

Goal:

- Fix 五倍 `WAIT_BATTLE_FINISH` timeout when the user pauses during combat.
- The timeout should measure active waiting time, not wall-clock time spent paused.

Changed files:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- Reverted the temporary chained-combat timeout widening; 五倍战斗等待 still uses the normal `180_000ms` timeout.
- Moved the stop/pause checkpoint ahead of the timeout test in `tickWaitBattleFinish(...)`.
- Measured how long the checkpoint blocked. If it blocked for at least `1_000ms`, the code shifts `waitBattleStartedAt` and `waitBattleNextTrackerRetryAt` forward by that blocked duration.
- Added log marker:
  - `[wubei] wait battle timer paused: blockedMs=... adjustedStartAt=... adjustedNextRetryAt=...`

Why:

- Logs showed the leader entered 黄袍 combat at about `23:11:04`, paused at `23:11:08`, resumed at `23:14:48`, and immediately hit `wait battle timeout`.
- The old logic used wall-clock `System.currentTimeMillis()` without subtracting pause duration, so paused time was incorrectly counted as active battle waiting time.

Validation:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-02 全局暂停快捷键改为暂停/继续切换

Status: completed

Goal:

- Make `Ctrl+Shift+F11` behave like a pause/resume toggle instead of only sending pause.

Changed files:

- `src/main/java/com/bot/dhxy/input/GlobalEmergencyStopHotkeyService.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `docs/ACTIVE_WORK.md`

Done:

- Added `WindowTaskControlService.togglePauseResumeAll()`.
- If there are no live tasks, the command returns a clear empty result.
- If all live tasks are already `PAUSED`, the next `Ctrl+Shift+F11` sends `resumeAll()`.
- Mixed state intentionally sends `pauseAll()`, so one still-running window will be paused rather than accidentally resumed into unsafe motion.
- Global hotkey `Ctrl+Shift+F11` now calls the toggle method.

Validation:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-03 五环当前地图导航放权延迟优化

Status: completed

Goal:

- Reduce the delay between current-map mini-map pathing confirmation and task-turn release.
- Target: after movement is confirmed, the current window should yield quickly so the next window can start its own route instead of waiting 2-3 seconds for UI cleanup.

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- In `navigateInCurrentMap(...)`, when `returnOnPathingStarted=true` and the result is `PATHING_STARTED`, skip the final `closeMiniMapIfOpen("navigateInCurrentMap:finish")`.
- Added log marker:
  - `navigate in current map skips mini-map close before yield`

Why:

- Logs showed current-map navigation confirmed pathing, then spent about 1.5-2.9 seconds closing the mini-map before releasing the task turn.
- For phase/yield navigation, the caller only needs to submit the movement and release the shared task turn. UI cleanup can happen later when that same window resumes or enters combat.

Validation:

- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-03 战斗进入后后台快速补开自动战斗

Status: completed

Goal:

- When a window-level combat watcher detects battle entry, quickly ensure automatic combat is opened.
- Do not wait for the 五环 main task turn to reach `CHECK_COMBAT` before sending the first auto-combat shortcut.

Changed files:

- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `docs/ACTIVE_WORK.md`

Done:

- `AutoCombatService.handleWindowCombatGuardTick(...)` now consumes the combat-enter signal and calls the same combat-entry handler used by the task tick.
- Refactored auto-combat panel handling into shared steps:
  - `ensurePanelVisible(...)`: check panel, send background `Alt+8` if missing, then recheck.
  - `alignPanelIfNeeded(...)`: drag panel only during the full task-owned verify flow.
  - `verifyRemainingRounds(...)`: OCR/refresh rounds only during the full task-owned verify flow.
- Added `AutoCombatPanelService.ensureAutoCombatPanelVisibleFast(...)` as the combat-watcher entry point. It only calls `ensurePanelVisible(...)`.
- The fast path intentionally does not drag the panel, OCR remaining rounds, run first-aid, or do post-combat recovery. Those stay in the owning task flow.

Why:

- Logs showed `window-combat-watch-*` detected battle entry several seconds before the 五环 task reached `CHECK_COMBAT`.
- The watcher previously only updated combat state, so auto-combat panel opening waited behind task-turn scheduling.

Validation:

- `mvn -q -DskipTests compile` passed.

### 何黎 - 2026-06-07 五环绿字 pathing intent 语义修正

Status: implemented / compile passed

Decision:

- 不新增“绿字 watcher”。仍然使用 `WindowTaskRunner` 里的同一个 window pathing watcher。
- 但 `WindowPathingIntent` 需要区分语义：
  - `TARGETED`: 有目标地图/坐标的普通导航，可以产生 `ARRIVED`。
  - `UNTARGETED_TRACKER`: 五环任务追踪绿字点击。目的地由游戏任务盘决定，代码没有坐标，因此 watcher 不能判断“到达”，只能观察是否开始移动、是否还在移动、是否停住、是否进入/退出战斗。
- 五环左侧绿字注册为 `UNTARGETED_TRACKER`，`targetMapName/targetX/targetY` 仍为空。
- `UNTARGETED_TRACKER` 不走 `hasArrived(...)`，避免空目标被误判成 `ARRIVED`。
- 五环消费 watcher 时，绿字路径应把 terminal 状态理解为“跑路已停/需要继续处理任务盘或对话框”，不是“到达目标坐标”。

Why:

- 五环没有任务目标坐标，点左侧绿字后只能等待游戏自己寻路、弹对话或进战斗。
- 之前通用 observer 文档只描述坐标导航压测，`ARRIVED / STOPPED_AWAY` 语义适合修罗/五倍/买鞋入口，不适合五环绿字。
- 这次修正保持同一个 watcher，只修正 intent 类型和分类语义，避免再加一套“任务盘 watcher”。

Files changed:

- `src/main/java/com/bot/dhxy/window/model/WindowPathingIntentType.java`
- `src/main/java/com/bot/dhxy/window/model/WindowPathingIntent.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`

### 何黎 - 2026-06-03 窗口层 pathing observer 实验层

Status: in progress / experimental

Goal:

- 先把“窗口后台观察跑路状态”这一层搭起来，用导航压力测试验证。
- 暂时不要接入五环、五倍、修罗正式业务流程。
- 目标是证明：当任务触发 pathing 并放权后，窗口层可以在后台通过小地图模板持续更新当前地图/坐标，并输出 `ACTIVE / ARRIVED / STOPPED_AWAY / UNKNOWN` 状态。

Why:

- 五开时当前任务轮转太慢。很多窗口已经移动到位，但等重新拿到任务权后才开始同步位置、判断是否到达，导致每个窗口之间反应很慢。
- 这个问题更像窗口调度/后台观察层的问题，不应该先在五环、五倍、修罗业务里各自硬补。

Current design decision:

- `WindowTaskRunner` 增加窗口层 observer 能力。
- `WindowRuntimeContext` 保存窗口自己的 `WindowPathingSnapshot`。
- `NavigationService` 只有在 `NavigationRequest.publishWindowPathingIntent=true` 时才会登记 pathing intent。
- `NavigationRequest.publishWindowPathingIntent` 默认是 `false`，所以正式任务现在不会自动接入。
- 目前只有 `DebugNavigationStressTask` 设置 `publishWindowPathingIntent(true)`。
- `DEBUG_NAVIGATION_STRESS` 会启动纯 pathing observer，但不会跑 combat guard，不会发送自动战斗输入。

Files changed:

- `src/main/java/com/bot/dhxy/model/navigation/NavigationRequest.java`
- `src/main/java/com/bot/dhxy/window/model/WindowPathingIntent.java`
- `src/main/java/com/bot/dhxy/window/model/WindowPathingSnapshot.java`
- `src/main/java/com/bot/dhxy/window/model/WindowPathingState.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/execution/MultiWindowTaskManager.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/task/DebugNavigationStressTask.java`

Important guardrails for other agents:

- Do not wire this observer into 五环/五倍/修罗 yet.
- Do not make tasks consume `WindowPathingSnapshot` until the navigation stress test proves the observer is stable.
- Do not turn `publishWindowPathingIntent` on by default.
- Do not add task-specific fallback logic here. This layer should only observe and cache window state.
- Do not send input from the pathing observer. It may screenshot/read mini-map state only.

Logs to watch:

- `window observer started`
- `window pathing intent registered`
- `pathing watcher update: ... state=ACTIVE`
- `pathing watcher update: ... state=ARRIVED`
- `pathing watcher update: ... state=STOPPED_AWAY`
- `pathing watcher unknown`

How to test:

- Run the existing navigation pressure task, not 五环/五倍/修罗:
  - UI: select `导航压力测试`
  - or IntelliJ: run `src/main/java/com/bot/dhxy/debug/NavigationStressDebugMain.java`
- Recommended first test:
  - start with 1 window;
  - then 2 windows;
  - only after observer logs are stable, test more windows.
- Expected result:
  - after `PATHING_STARTED`, the task should release turn;
  - the observer should continue logging current map/coordinate changes in the background;
  - when the window reaches the target, observer should log `ARRIVED` without the task needing to reacquire and run a slow full sync first.

Validation:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` on the touched Java files only reported existing CRLF warnings, no whitespace errors.

Open decision:

- After the observer is verified, decide how to expose readiness to the scheduler:
  - option A: tasks consume cached `WindowPathingSnapshot` after reacquiring the turn;
  - option B: task-turn scheduler prioritizes windows whose observer reports `ARRIVED` or `STOPPED_AWAY`;
  - option C: combine both, but only after logs prove this observer is reliable.

### 唐德 - 2026-06-05 Map label 模板尺寸统计

Status: completed / report only

Goal:

- 先统计 `images/template/map_label/*.png` 的实际尺寸分布，后续再决定是否统一模板尺寸。

Result:

- 新增记录文件：`docs/map-label-template-size-report.md`
- 当前共 47 张 map label 模板。
- 高度大多是 18 px，只有 `四圣庄.png` 和 `金兜洞.png` 是 17 px。
- 宽度按地图名字长度分散为 13 个尺寸组。

No image files were modified.

### 唐德 - 2026-06-05 导航压测当前地图点击误判修正

Status: implemented / compile passed

Goal:

- 修正导航压力测试里当前地图点击已经触发移动、但 1 秒坐标确认窗口没有读到变化时被误判为 `POINT_NOT_REACHED` 的问题。

Observed:

- 用户肉眼确认两个窗口从 `大唐边境(22,271)` 点击后确实移动并最终到达目标附近。
- 日志显示物理输入已成功：
  - `Alt+1 success=true`
  - `physical operation=clickLeft`
- 但 `NavigationService.confirmMiniMapPathingStarted(...)` 只在约 1 秒内轮询小地图坐标，期间仍读到 `baseline=(22,271) current=(22,271)`，于是返回 `NO_PATHING`。
- `DebugNavigationStressTask` 的 `MAX_NAVIGATION_RETRY=0`，导致这个短确认误判直接让任务失败。

Changed files:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `docs/ACTIVE_WORK.md`

Done:

- 只在 `returnOnPathingStarted=true` 且 `publishWindowPathingIntent=true` 的 observer 压测路径里改判定。
- 当前地图 mini-map 点击已经成功发出，但短坐标确认没有看到 delta 时，不再立刻返回 `POINT_NOT_REACHED`。
- 改为返回 `PATHING_STARTED` 并注册窗口级 pathing intent，让 `WindowTaskRunner` 的后台 observer 后续判断 `ACTIVE / ARRIVED / STOPPED_AWAY`。
- 正式业务里没有开启 `publishWindowPathingIntent` 的路径暂不改变。

Next validation:

- Re-run `导航压力测试`，看 `current-map mini-map click submitted; observer will confirm pathing` 后 watcher 是否继续更新到 `ACTIVE` 或 `ARRIVED`，而不是立即失败。
- `mvn -q -DskipTests compile` passed.

### 唐德 - 2026-06-05 导航压测 loop guard 误杀修正

Status: implemented / compile passed

Observed:

- 14:38 最新导航压力测试最后两个窗口不是因为 `POINT_NOT_REACHED` 失败。
- 两个窗口最终都在第 5 个目标 `大雁塔二层(76,73)` 失败：
  - `hwnd-4A81470`：`[nav-stress] loop guard exceeded: index=4 waiting=true target=#5 大雁塔二层(76,73)`
  - `hwnd-C117E`：同样是 `loop guard exceeded`。
- 当时 watcher 仍在正常报告 `ACTIVE`，例如 `current=长安城东(308,173)`，说明这是 debug task 自己的保护计数误杀，不是导航输入失败。

Cause:

- `DebugNavigationStressTask` 的 `MAX_LOOP_GUARD=600` 在主循环每次都递增。
- 现在 pathing wait 会每 250ms 轮询一次后台 observer；长路线/多目标会把 600 次很快消耗掉。
- pathing wait 本身已有 90 秒 wall-clock timeout，不能再用循环次数作为失败条件。

Changed files:

- `src/main/java/com/bot/dhxy/task/DebugNavigationStressTask.java`
- `docs/ACTIVE_WORK.md`

Done:

- `waitingPathing=true` 时不再消耗 `loopGuard`。
- loop guard 只保留给非等待阶段的异常状态 churn。
- pathing 等待是否失败继续由 `PATHING_TARGET_WAIT_TIMEOUT_MS=90000` 和 watcher 状态判断。

Next validation:

- 重新跑 `导航压力测试`，确认第 5 个目标不会因为 `waiting=true` 的 observer 轮询触发 `loop guard exceeded`。
- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-05 route dialog 后台预计算不抢权

Status: implemented / compile passed

Goal:

- route dialog 预计算没有完成时，窗口不要拿到输入机会后在前台干等或重复 OCR。
- 后台已经在算同一个 route dialog 时，任务层先让出；后台没开始算时，前台接管并取消后台 request。

Changed files:

- `src/main/java/com/bot/dhxy/model/dialog/DialogPreparationPhase.java`
- `src/main/java/com/bot/dhxy/model/dialog/DialogPreparationStatus.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/model/dialog/DialogResultStatus.java`
- `src/main/java/com/bot/dhxy/model/navigation/NavigationResult.java`
- `src/main/java/com/bot/dhxy/model/navigation/NavigationResultStatus.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/DebugNavigationStressTask.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`

Done:

- `WindowRuntimeContext` 现在记录 dialog preparation lifecycle：`REQUESTED / PREPARING / READY / FAILED`。
- `WindowTaskRunner.refreshDialogPreparationSignal(...)` 在 watcher 真正开始算、算空、异常、成功时更新状态。
- `NavigationService.clickRouteDialogOption(...)` 遇到同目标 `PREPARING` 时返回 `DIALOG_PREPARING`，不继续前台 OCR。
- 如果只有 `REQUESTED` 但 watcher 还没开始，前台会清掉 request 并自己同步处理，避免后台稍后重复算。
- `DialogService.handleRememberedOption(...)` 在真正点击 remembered point 前再次检查并消费 prepared action，解决后台结果比 NavigationService 的 200ms 等待稍晚才出现时无法被用上的问题。
- `DebugNavigationStressTask` 遇到 `DIALOG_PREPARING` 用短让出，不走 3 秒 retry backoff。
- `FiveRingTaskV2` 遇到 `DIALOG_PREPARING` 走 shared-state retry，避免持有任务权等待后台。

Logs to watch:

- `dialog preparation probe start`
- `dialog prepared`
- `route dialog preparation still running; yield before foreground OCR`
- `route dialog preparation not started; foreground takes over`
- `dialog remembered option uses prepared action`
- `[nav-stress-latency] route dialog preparing in background; yield before retry`

Next validation:

- 重新跑 `导航压力测试` 两窗口/五窗口。
- 重点看 route dialog 场景是否出现：
  - 后台 `PREPARING` 时当前窗口短让出；
  - prepared action ready 后前台直接点击；
  - 不再出现同一个 route dialog 先后台 prepare、再前台 OCR 的双算链。
- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-05 15:26 导航压力测试结果复盘

Status: tested / needs follow-up

Observed:

- 本轮只注册并启动了 2 个窗口：
  - `hwnd-2710776` / `刑部ㄨ忍者（ID：67555）`
  - `hwnd-59094A` / `忆叶知秋（ID：451753529）`
- 两个窗口最终都完成：
  - `15:30:41.662` `hwnd-2710776` `导航压力测试 -> SUCCESS`
  - `15:31:12.905` `hwnd-59094A` `导航压力测试 -> SUCCESS`
- 没有看到本轮任务级 `FAILED` 或异常退出。

Latency notes:

- 多个 `productionNavigate` 仍明显超过 3 秒。
- 跨地图 route submit 常见在 7-12 秒，例如：
  - `#1 长安`：`7003ms` / `11571ms`
  - `#2 长安城东`：`7443ms` / `8105ms`
  - `#4 龙宫`：`9294ms`
  - `#5 大雁塔二层`：`12507ms`
- 当前地图小地图点击阶段多在 2.8-4.2 秒，部分仍超过 3 秒，例如：
  - `#5 大雁塔二层`：`2979ms`
  - `#3 大唐边境`：`4040ms` / `4173ms`

Route dialog preparation result:

- 后台 route dialog 预计算机制这轮没有真正命中。
- 每次都是：
  - `route dialog preparation requested`
  - 约 `200ms` 后 `route dialog preparation not started; foreground takes over`
  - `route dialog prepared wait finished ... usable=false`
  - `route dialog prepared action unavailable; continue normal path`
- 没有出现：
  - `DIALOG_PREPARING`
  - `route dialog preparation still running; yield before foreground OCR`
  - `dialog remembered option uses prepared action`
- 结论：当前 request 被创建后，前台 200ms 等待太短或 watcher 没有及时进入 `PREPARING`，所以实际还是前台同步处理 route dialog，后台预计算没有吃到这轮时间差。

Next steps:

- 调整 route dialog request 的接管策略：`REQUESTED` 时不要 200ms 后马上取消，至少先让 watcher 获得一次扫描机会，或者由任务层直接短让出。
- 给 watcher 开始处理 preparation 的路径补更明确耗时日志，区分“没有扫到 dialog”和“扫到了但还没算完”。
- 再跑同样两窗口测试，目标是能看到 `DIALOG_PREPARING` 或 `dialog remembered option uses prepared action` 至少一种路径命中。

### 谢帅 - 2026-06-05 18:32 三窗口导航压力测试结果

Status: tested / improved / needs follow-up

Observed:

- 本轮注册并启动了 3 个窗口：`hwnd-2D70B12`、`hwnd-55A06DE`、`hwnd-A41144`。
- 三个窗口最终都完成，没有任务级失败：
  - `18:31:37.923` `hwnd-55A06DE` `导航压力测试 -> SUCCESS`
  - `18:31:46.908` `hwnd-2D70B12` `导航压力测试 -> SUCCESS`
  - `18:32:08.813` `hwnd-A41144` `导航压力测试 -> SUCCESS`

Route dialog preparation result:

- 后台 route dialog 预计算这轮开始真正命中，比 2 窗口测试有改善。
- `hwnd-A41144` 在 `长安` route dialog 上命中：
  - `18:28:05.331` watcher `dialog preparation probe start`
  - `18:28:05.507` 前台返回 `DIALOG_PREPARING`
  - `18:28:06.291` watcher `dialog prepared: target=长安 matched=长安桥（400两）`
  - `18:28:07.431` 前台点击 prepared route option，`PATHING_STARTED`
- `hwnd-A41144` 在 `龙宫` route dialog 上也命中：
  - `18:31:02.057` watcher `dialog preparation probe start`
  - `18:31:02.130` 前台返回 `DIALOG_PREPARING`
  - `18:31:03.253` watcher `dialog prepared: target=龙宫 matched=龙宫（400两）`
  - `18:31:09.643` 前台点击 route option，`PATHING_STARTED`

Remaining issues:

- 仍有很多 route dialog 走了 `route dialog preparation not started; foreground takes over`，说明 200ms 内 watcher 没启动的情况还很多，预计算命中率不够稳定。
- prepared action 点击后出现过 stale invalidation 日志：
  - `18:28:07.493` `target=长安 distance=621 maxDistance=8`
  - `18:31:09.791` `target=龙宫 distance=441 maxDistance=8`
  这可能是点击/转场后旧 prepared state 没及时清干净，也可能是 invalidation 时机太晚，需要下一步确认。
- 当前地图小地图点击阶段仍常见约 3 秒上下，最终样本里 `navigateInCurrentMap:click` input request 约 `2298ms`，整体 current-map step 约 `2951ms`，已经接近但还没有稳定低于 3 秒。

Next steps:

- 把 route dialog `REQUESTED` 的处理改成“优先让 watcher 至少吃到一轮”：第一次看到同目标 `REQUESTED` 可短让出，而不是 200ms 后立刻前台接管。
- route option 成功点击或 `PATHING_STARTED` 后及时清理/消费 prepared action，减少后续 stale invalidation 噪声。
- 再跑 3-5 窗口，重点看 `DIALOG_PREPARING` 命中率和窗口交接是否仍保持顺滑。

### 谢帅 - 2026-06-05 route dialog 五窗口测试前调整

Status: implemented / compile passed

Changed:

- `NavigationService.clickRouteDialogOption(...)`
  - 同目标 route dialog 处于 `REQUESTED` 且 request 还很新时，不再 200ms 后马上前台接管。
  - 新逻辑会返回 `DIALOG_PREPARING`，让任务短让出，给 watcher 至少一次轮询机会。
  - 如果 request 超过 `800ms` 仍没进入 `PREPARING/READY`，再清掉 request 并走前台正常路径，避免永久等后台。
  - 新日志：`route dialog preparation requested; yield for watcher start`。
- `WindowTaskRunner`
  - 有 dialog preparation request/prepared action 时，watcher active interval 从 `200ms` 降到 `100ms`。
  - prepared action validation 如果发现 request 已经被前台消费/清掉，不再输出 stale invalidation，只打 debug 级 `request-consumed`。

Why:

- 三窗口实测已经证明后台 prepare 能工作，但很多 dialog 还是因为 watcher 没在 200ms 内启动而被前台抢回。
- 五窗口测试前先把 request->watcher 的接力窗口放宽一点，目标是提高 `DIALOG_PREPARING` / prepared action 命中率，同时保留前台兜底。

Verify:

- `mvn -q -DskipTests compile` passed.

Next log checks:

- 期望看到更多：
  - `route dialog preparation requested; yield for watcher start`
  - `dialog preparation probe start`
  - `route dialog preparation still running; yield before foreground OCR`
  - `route dialog probe uses prepared action`
  - `route dialog memory path uses late prepared action`
- 如果仍大量出现 `route dialog preparation not started; foreground takes over`，再看 requestAgeMs 是不是已经超过 `800ms`，判断 watcher 是否被其他工作卡住。

### 谢帅 - 2026-06-05 18:55 五窗口测试中断复盘

Status: bug found / fixed / compile passed

Observed:

- `岁月醉白头` 对应窗口：`hwnd-2000A3C` / hwnd `33557052`。
- 它不是拿不到 turn；日志显示它反复拿到 turn：
  - `requestTurn transaction=debug-nav-stress:navigate:1-长安`
  - `outsideTurnStart transaction=debug-nav-stress:navigate:1-长安`
- 画面上不动的原因是它第一步一直卡在 `长安` route dialog preparation：
  - 当前地图一直是 `洛阳城(311,116)`。
  - 多次返回 `DIALOG_PREPARING`，没有真正进入 route dialog 前台点击。
  - watcher 反复 `dialog preparation probe miss`，且单次 prepare miss 可达 `7s / 12s / 13s`。

Root cause:

- 上一版只给 `REQUESTED` 阶段加了 `800ms` 上限。
- 一旦 watcher 进入 `PREPARING`，前台会一直让出，等待后台完成。
- 如果后台 OCR/模板准备很慢并且最终 miss，窗口就会反复拿 turn、反复让出，但永远不执行前台兜底。

Changed:

- `NavigationService.clickRouteDialogOption(...)`
  - 新增 `ROUTE_DIALOG_PREPARING_YIELD_MAX_MS = 1500ms`。
  - 同目标 `PREPARING` 若超过 1.5 秒还没有 prepared action，前台清掉 request 并接管正常 route dialog 流程。
  - 新日志：`route dialog preparation too slow; foreground takes over`。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 再跑 5 窗口。
- `岁月醉白头` 这类窗口最多应让 watcher 一小段时间；如果 watcher 慢/miss，应出现 `preparation too slow; foreground takes over`，随后进入正常前台点击，而不是一直 `DIALOG_PREPARING`。

### 唐德 - 2026-06-05 五窗口启动无动作 / DIALOG_PREPARING 空转修正

Status: implemented / compile passed

Observed:

- 用户启动后 UI 显示 5 个窗口运行中，但游戏里没有任何输入反应。
- 最新日志显示本轮实际启动的是 `DEBUG_NAVIGATION_STRESS`，5 个窗口都在处理 `#1 长安` route dialog：
  - 任务反复输出 `route dialog preparation requested; yield for watcher start`
  - watcher 反复输出 `dialog preparation probe start` -> `dialog preparation probe miss`
  - 没有继续出现 `submitWorldMapSearchAndClickDestination:长安` 或后续 `INPUT_TRACE`
- 所以 UI 的“运行中”不是假状态；任务线程确实在跑，只是卡在 route dialog 后台准备状态，没有进入真实输入路径。

Root cause:

- `NavigationService.clickRouteDialogOption(...)` 每次重试都会重新创建同一个 target 的 `DialogPreparationRequest`。
- 这会把 `createdAtMs` 重置，导致 `ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS=800ms` 的前台兜底永远等不到超时。
- watcher miss 以后，下一轮又重新 request，同样继续 `DIALOG_PREPARING`，形成五窗口空转。

Changed:

- `NavigationService.clickRouteDialogOption(...)`
  - 同一个 route target 如果已有 `REQUESTED/PREPARING` request，不再重复创建 request，只复用已有状态，让 request age 能正常增长并触发前台兜底。
  - 如果同一个 target 刚刚被 watcher 标记 `FAILED`，短时间内清掉 request 并直接让前台路径接管，避免 miss 后马上再 request。
  - 新增 `ROUTE_DIALOG_FAILED_FOREGROUND_COOLDOWN_MS=2000ms`，只用于防止同目标 watcher miss 后立即循环。

Verify:

- `mvn -q -DskipTests compile` passed.

Next test:

- 停掉当前运行中的任务并重启应用/重新启动任务，确认新代码生效。
- 观察是否从 `DIALOG_PREPARING` 空转变成前台 route option OCR 或 prepared action 命中。

### 谢帅 - 2026-06-06 北俱芦洲 route dialog 被遗忘复盘

Status: investigated / small fix / compile passed

Observed:

- 用户暂停前，`一叶知秋`、`仁者有容`、`刑部` 等窗口在 `北俱芦洲 -> 大唐边境` 路线对话框处卡住。
- 画面上 route dialog 已经弹出，但窗口再次拿到 turn 后没有直接点击，反而重新打开世界地图并再次搜索导航。
- 日志里坏路径很明确：
  - `dialog preparation expired: operation=ROUTE_TRANSFER target=大唐边境 source=navigateToMap:map-route-clicked`
  - 下一次进入导航时 `route dialog preparation snapshot before world-map search ... statusPhase=NONE ... usable=false`
  - 随后立刻 `navigation map search start: target=大唐边境`
- 好路径则是：
  - `statusPhase=READY ... usable=true`
  - `consume prepared route dialog before world-map search`
  - `route dialog probe uses prepared action`

Root cause:

- route dialog preparation request 的 TTL 原来是 `45s`。
- 五窗口压测时，一个窗口点出路线对话框后可能长时间排队，等它重新拿 turn 时 request 已经过期。
- `WindowRuntimeContext.clearDialogPreparationRequest(...)` 会同时清掉 request 和 prepared action；所以肉眼看到 dialog 还在，但代码已经没有“这个 dialog 该点哪里”的准备状态。
- 另外 `visible route dialog rescue` 只接受 `10s` 内的 `STOPPED_AWAY` snapshot；五窗口排队时也偏短，容易错过救援窗口。

Changed:

- `NavigationService`
  - `ROUTE_DIALOG_PREPARE_REQUEST_TTL_MS`: `45_000ms -> 120_000ms`
  - `ROUTE_DIALOG_VISIBLE_RESCUE_SNAPSHOT_MAX_AGE_MS`: `10_000ms -> 120_000ms`
- 没有改世界地图搜索、绿色链接点击、OCR 选项算法，只延长 route dialog 已弹出后的准备/救援有效期。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 重启应用后再跑五窗口导航压测。
- 重点观察 `北俱芦洲 -> 大唐边境`：
  - 预期减少 `dialog preparation expired` 后立刻 `navigation map search start`。
  - 预期更多看到 `consume prepared route dialog before world-map search` 或 `try visible route dialog rescue before world-map search`。
  - 如果仍旧出现 dialog 明明在但 status 为 `NONE`，下一步应改 request 过期时的清理策略，不要把仍可验证的 prepared action 一起清掉。

### 谢帅 - 2026-06-06 长安城东 map-only arrival 后旧 route dialog 清理

Status: implemented / compile passed

Observed:

- 用户问 08:32-08:33 附近 `大叔` 已经在 `长安城东`，为什么不像是直接打开小地图导航，反而像还在点 route 链接。
- 按窗口 title/ID 拆日志后，`大叔` 实际已经走到当前地图导航：
  - 08:32:34 已同步到 `长安城东 (27,231)`。
  - 08:32:53 执行 `Alt+1`，随后点击当前地图逻辑坐标 `(166,118)`。
- 真正异常的是同窗口后面仍有一个旧的 `ROUTE_TRANSFER target=长安城东` 后台准备请求：
  - `dialog preparation probe start ... target=长安城东 ... requestAgeMs=55102`
  - 但当前 route dialog OCR 里没有 `长安城东` 选项，最终 miss，浪费十几秒且污染日志判断。

Root cause:

- `navigateToMap` fresh confirm 已经确认当前地图就是目标地图时，会直接返回 `ARRIVED`，但没有清掉同目标的旧 route dialog preparation/prepared action。
- `DebugNavigationStressTask` 消费 map-only `ARRIVED` 并准备继续当前地图坐标导航时，也只清 `pathingSignal`，没有清同目标 route dialog preparation。
- 这样窗口已经进入当前地图坐标导航后，watcher 仍可能拿旧 target 做 route dialog 准备。

Changed:

- `NavigationService.navigateToMap(...)`
  - 当 stale-cache/fresh map guard 确认已在目标地图，并且同目标存在 `ROUTE_TRANSFER` preparation/action 时，清理该旧准备状态。
- `DebugNavigationStressTask`
  - 消费 map-only arrival、准备继续坐标导航时，同步清理同目标 `ROUTE_TRANSFER` preparation/action。
- 没有改世界地图搜索、绿色链接点击、当前地图坐标点击算法。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 下轮压测看 `长安城东` 已经到图后，是否还出现同窗口旧的 `dialog preparation probe start ... target=长安城东 requestAgeMs=...`。
- 如果还有，继续查是谁在到图后重新创建 route preparation，而不是改点击算法。

### 谢帅 - 2026-06-06 岁月醉白头龙宫失败前旧 route preparation 清理

Status: implemented / compile passed

Observed:

- 用户指出 `岁月醉白头`（最新运行窗口 `hwnd-311168`，ID `387545229`）在异常失败前像是和其他窗口打架。
- 失败点：
  - `08:44:19.409` `target=#4 龙宫(110,54) status=MAP_NOT_REACHED message=map route submit failed`
  - `08:44:19.417` 窗口任务直接 `FAILED`
- 复查 `08:44:02-08:44:19` 的 `INPUT_TRACE` 后确认：
  - 这段是在 `submitWorldMapSearchAndClickDestination:龙宫` 的一个 exclusive input request 内。
  - 物理输入全是 `hwnd-311168`，没有其他窗口插入鼠标/键盘。
  - 所以这次不是经典的 input queue 串窗抢输入。
- 但进入 `龙宫` 导航前 runtime 里还挂着旧状态：
  - `route dialog preparation snapshot ... target=龙宫 statusPhase=REQUESTED statusTarget=大唐边境 preparedTarget=null`
  - 这说明上一个 route dialog preparation 没有在目标切换时被清干净。
- Debug 压测当前 `MAX_NAVIGATION_RETRY=0`，所以一次 map route submit 失败就会直接让该窗口结束。

Changed:

- `NavigationService.navigateToMap(...)`
  - 在开始新 target 的 route-dialog precheck 前，如果 runtime 中存在旧的 `ROUTE_TRANSFER` preparation/action，且 `targetKeyword` 不是当前 `targetMapName`，立即清理。
  - 只清 stale target，不清同 target 的 watcher/prepared action。
  - 不改世界地图搜索、绿色链接点击、route dialog OCR/点击算法。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 再跑五窗口压测，重点看 `route dialog preparation snapshot before world-map search`：
  - 预期不会再看到同一窗口 `target=龙宫 statusTarget=大唐边境` 这种跨目标旧状态。
  - 如果还出现 `map route submit failed`，下一步查 `DebugNavigationStressTask` 是否应允许 route submit transient failure 重试，而不是 `retry=0/0` 直接终止。

### 谢帅 - 2026-06-06 Jason/Hooke route dialog 架构 CR 后的小修

Status: implemented / compile passed

Review summary:

- Jason 和 Hooke 都认为当前主要问题不是绿色链接点击算法，而是 route dialog / watcher / task-yield 状态消费不统一。
- 共同风险：
  - `DebugNavigationStressTask` 对 `REQUESTED/PREPARING` 最多等 30 秒，绕开了 `NavigationService` 自己 3 秒左右的前台兜底。
  - `READY` 但 prepared action 已超过可点击年龄时，仍可能被 `hasMatchingRouteDialogPreparation(...)` 当成可消费状态。
  - `MAX_NAVIGATION_RETRY=0` 会把一次 transient `map route submit failed` 直接放大成窗口 FAILED。

Changed:

- `DebugNavigationStressTask`
  - 新增 `ROUTE_DIALOG_REQUESTED_WAIT_TIMEOUT_MS=3000ms`。
  - `REQUESTED` 只短等 watcher 接手；超过 3 秒就结束等待并重新进入 `NavigationService` 前台路径。
  - `PREPARING` 从 30 秒收短到 10 秒；超过后重新进入前台路径。
  - `MAX_NAVIGATION_RETRY` 从 `0` 改为 `1`，避免一次 route submit 抖动直接杀掉压测窗口。
- `NavigationService.hasMatchingRouteDialogPreparation(...)`
  - 只有 `isPreparedRouteDialogActionUsable(...)` 通过的 prepared action 才算可直接消费。
  - `READY` 但 action 过期/绑定不匹配时返回 false，并记录 `verifiedAgeMs/maxAgeMs`，避免继续卡在 consume prepared 路径。
- 没有改世界地图绿色链接点击算法，没有改 OCR 结果选择算法。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 再跑 3-5 窗口导航压测。
- 重点看：
  - `route dialog request waiting for watcher` 是否最多 3 秒后转为 `re-enter navigation foreground path`。
  - `route dialog preparation ready but prepared action is not directly usable` 出现后是否不再长时间空等。
  - `retry=1/1` 是否能吸收一次 `map route submit failed`，而不是直接 FAILED。

### 唐德 - 2026-06-06 自动战斗手动启动按队员窗口注册

Status: implemented / compile passed

Observed:

- 用户在主控点“自动战斗”后，UI 成功提交 `[auto_battle]` 到 5 个窗口，但所有窗口几秒后回到空闲/未知任务。
- 最新 `logs/dhxy-console.log` 显示每个窗口都进入了 `AutoBattleTask`，但上下文 role 都是 `UNKNOWN`。
- `TaskStartupCheckService.checkAutoBattle(...)` 在当前配置 `auto-battle-requires-member=true`、`allow-auto-battle-when-role-unknown=false` 下直接返回 `SKIPPED`：
  - `自动战斗前置判断未通过 ... role=UNKNOWN | role unknown and live role detection is skipped`
- 用户确认产品规则：手动点“自动战斗”就表示这些窗口按队员挂机窗口处理，不需要再等队伍身份识别。

Changed:

- `NativeWindowRegistrationMapper.toIndependentRegistrationRequests(...)`
  - 当扫描/注册任务类型是 `TaskType.AUTO_BATTLE` 时，注册请求直接写入 `WindowRole.MEMBER`。
  - 其他独立任务仍保持 `WindowRole.UNKNOWN`，不恢复旧的“第一个窗口队长”规则。
  - 这样 `TaskExecutionContext.windowRole` 会是 `MEMBER`，自动战斗前置判断可以按队员窗口放行。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 用户再次点“自动战斗”后，日志中应看到 `AutoBattleTask` 上下文 role 为 `MEMBER`，并出现 `自动战斗前置判断通过 ... allowed by preflight role`。
- 如果仍回到空闲，下一步查 `WindowRuntimeContext.applyRegistration(...)` 是否被其他刷新路径用 `UNKNOWN` 覆盖 role。

### 谢帅 - 2026-06-06 导航压测 watcher 坐标刷新滞后导致重复导航

Status: investigating / design review requested from Hook + Jason

Observed:

- 五窗口 `DEBUG_NAVIGATION_STRESS` 压测中，`岁月醉白头`（`hwnd-311168`，ID `387545229`）在目标 `#3 大唐边境(137,121)` 出现重复输入 `大唐边境`。
- 实际日志链路：
  - `09:30:20.653` watcher 到达地图：`current=大唐边境(22,271)`。
  - `09:30:25.084` 当前地图坐标点击已触发：`target=#3 大唐边境(137,121)`，`coordinateIntent=true`。
  - 游戏自动寻路从大唐边境点位绕回中间地图：`北俱芦洲 -> 洛阳城 -> 四圣庄 -> 大唐边境`。
  - `09:30:28.484` watcher 新扫到 `北俱芦洲(46,30)`。
  - `09:30:33.597` `DebugNavigationStressTask` 看到 snapshot 已 5 秒未变，按 `stationaryMs=5113` 判定 stalled，清掉 pathing signal 并重新走 world-map 导航，导致第二次输入 `大唐边境`。
  - `09:30:34.223` watcher 才扫到 `洛阳城(152,46)`，这次扫描本身很慢：`captureMs=2469 coordMs=1843`。
  - `09:30:47.158` watcher 最终扫到 `大唐边境(135,121)`，证明原始自动寻路其实可以到达目标。

Current understanding:

- `WindowTaskRunner` 的 pathing watcher 不是固定每秒产出坐标；它是同步执行 `MiniMapCoordinateReader.readCurrentTemplateLocation()`，成功/失败后再 sleep。
- 有 active pathing intent 时，sleep 间隔上限是 `WINDOW_PATHING_PROBE_ACTIVE_INTERVAL_MS=1000ms`，但实际刷新间隔约等于“一次识别耗时 + sleep”。
- 多窗口压测时单次 mini-map 模板/坐标识别可能耗时 2-5 秒，所以 `snapshot` 几秒不更新不等于角色停住。
- 当前 debug runner 在 `ACTIVE + hasObservedPosition + stationaryMs >= PATHING_STATIONARY_RETRY_MS` 分支中，把“最后一次成功识别的位置没更新”当成“人物停住”，会在跨图绕路时误重试。

Open design question:

- 如何把 `snapshot 没更新`、`watcher 正在慢扫/识别滞后`、`角色真的停住` 三种状态分开？
- 如何让下次不重复打开世界地图；如果 watcher 仍然慢扫，应该如何补救？

Candidate fix directions to review:

- 在 `DebugNavigationStressTask` 中，`coordinateIntent=true` 且 observed state 仍是 `ACTIVE` 时，不允许只靠 `stationaryMs` 进入 world-map retry。
- 对 current-map coordinate leg 增加跨图 grace period：如果当前地图不是目标地图，但路径年龄未超过较长阈值，应认为可能在自动寻路跨图绕路，继续等 watcher。
- retry 前引入更强证据：必须 watcher 明确 `STOPPED_AWAY`，或 snapshot 未更新且轻量移动检测也确认画面不动，才允许 retry。
- 给 watcher 增加扫描开始/结束/耗时日志，或在 `WindowPathingSnapshot` 中记录本轮 scan started/finished/elapsed，避免只看到成功结果却不知道中间是否在慢扫。
- 不应改世界地图绿色链接点击算法，不应改 `GameStateUtil.isMovingByPixelDiff()` 这类已验证底层逻辑。

Next:

- 等 Hook/Jason 对 `WindowTaskRunner`、`WindowPathingSnapshot`、`DebugNavigationStressTask` 的方案 review。
- 汇总后先做最小 patch：优先改 debug runner 的 retry 条件和日志，不动生产导航点击算法。

### 谢帅 - 2026-06-06 pathing watcher slow probe 节流试验

Status: implemented / compile passed

Observed:

- 五窗口导航压测已经能全部完成，但 `pathing watcher slow probe` 仍较多。
- 当前 watcher slow probe 不是 OCR 慢，而是 `WindowTaskRunner` 后台 watcher 调 `MiniMapCoordinateReader.readCurrentTemplateLocation()` 时被截图/模板读取拖慢。
- `GameClientTracker.captureToMemory(...)` 仍会进入全局截图锁，多窗口并发时一次 mini-map probe 可能排队数秒。
- 当 dialog preparation active 时 watcher loop 会被拉到 `100ms` cadence；如果每次 loop 都尝试 pathing probe，会制造额外截图锁竞争。

Changed:

- `WindowTaskRunner.refreshPathingSignal(...)`
  - 增加 `WINDOW_PATHING_PROBE_MIN_INTERVAL_MS=2000ms`。
  - 同一个 `WindowPathingIntent` 已有新鲜 snapshot 时直接复用，不重复截图。
  - 如果旧 snapshot 标记 `probeInProgress=true`，也直接复用，避免同 intent 叠加 probe。
- 不改 `NavigationService`、世界地图绿字点击、小地图点击、`GameStateUtil` 移动判断。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 再跑 3-5 窗口导航压测。
- 对比本轮和上一轮：
  - `pathing watcher slow probe` 数量是否明显下降。
  - 是否还出现 8-12 秒单次 probe。
  - 是否仍能及时出现 `state=ARRIVED` 和成功完成全部窗口。

### 唐德 - 2026-06-06 五环任务框绿字点击 replay 输出图

Status: implemented / replay output regenerated / waiting visual spot-check

Changed:

- `WuhuanTrackerGreenReplayDebugMain`
  - replay 输入改为 `images/test-cases/task-tracker/wuhuan-task-panel-block/raw`。
  - 为每个五环 testcase 生成带点击点的输出图到 `images/test-cases/task-tracker/wuhuan-task-panel-block/output`。
  - 红色标记从大十字/大圈改成 5px 小红点，方便肉眼检查是否落在绿字中心。
  - 单段绿色链接也会被接受为可点击目标，避免只有目标词时被误判为 `NO_LINK_SEGMENT`。
  - replay 先用左上黄色短标题筛掉非五环任务块；混入的“浮生半日闲”样本已从五环 testcase raw 中删除。
  - 2026-06-06 correction: testcase raw 又按绿色任务行像素签名去重；同一条绿色内容只保留一张，避免 `龙宫(7,60)...[4/5]` 这类重复样本刷屏。
- `FiveRingTaskV2`
  - 运行时任务框绿字点击点改为取所选绿色链接 box 的几何中心，不再用绿色像素重心或首段 run。
  - 运行时 debug 图同样使用小红点。
  - 单段绿色链接与 replay 工具保持一致。
  - 2026-06-06 correction: 五环任务追踪绿字目标不是整条 `地图(坐标)目标名` 的中心；运行时和 replay 都改成先按绿色文字行拆分，再优先选坐标括号后的目标名段。若目标名换行，优先使用进度 `[n/5]` 前的可用名字段；只有下一行只剩单字尾巴时，回退到上一行目标名主体，避免点到坐标或单字尾巴。
  - 五环任务追踪绿字点击不加 random/jitter。真实运行链路为 `resolveTrackerGreenClickPoint(...) -> InputAction.moveMouse/clickLeft -> WinApiMouseController`，当前传递的是精确 screen-absolute 坐标；后续不要在这条窄目标链路上套 `CoordinateHelper.getRandomizedPoint(...)`。

Verify:

- 2026-06-06 latest sample cleanup:
  - `raw_count=141`
  - `unique_green=141`
  - `duplicate_groups=0`
  - `output_count=141`
  - `generated=141`
  - `miss=0`
- 2026-06-06 target-name replay:
  - `mvn -q -DskipTests compile` passed.
  - `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.WuhuanTrackerGreenReplayDebugMain"` passed.
  - `samples=141 ok=141 skipped=0 rejected=0 failed=0 warned=0`
- 2026-06-06 correction: 红点标准明确为 selected green link box 几何中心；`道号贼` 这类三字目标应落在中间字附近。

Output:

- `images/test-cases/task-tracker/wuhuan-task-panel-block/output`
- `images/test-cases/task-tracker/wuhuan-task-panel-block/rejected-non-wuhuan`

### 唐德 - 2026-06-07 状态面板飞行状态检测

Status: implemented / compile passed / not yet wired into 五环买鞋

Changed:

- `InputActionType` / `InputAction` / `InputActionWorker` / `InputSequences`
  - 补齐 `Alt+U` 队列动作，用于打开角色状态面板。
  - 补齐 `Ctrl+U` 队列动作，用于检测后关闭状态面板。
- `BoundWindowKeyboardService`
  - 补齐后台 HWND `Alt+U` 枚举，键盘-only 队列可以优先走后台快捷键。
- `WinApiMouseController` / `InputProvider`
  - 补齐 `pressCtrlU()`。
- `GameStateUtil`
  - 新增 `FlyingState { FLYING, NOT_FLYING, UNKNOWN }`。
  - 新增 `detectFlyingState(reason)`：
    - 先按 `Alt+U` 打开状态面板。
    - 按用户给定区域检测：base `(767,169)` 下绝对区域 `(1459,690)-(1518,718)`，换算为窗口相对 `(692,521)-(751,549)`。
    - 先匹配 `images/template/status/flying.png`，命中返回 `FLYING`。
    - 再匹配 `images/template/status/unflying.png`，命中返回 `NOT_FLYING`。
    - 都没命中返回 `UNKNOWN`。
    - `finally` 中按 `Ctrl+U` 关闭状态面板。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 将 `detectFlyingState(...)` 接入五环买鞋入口逻辑：到达/接近长安 `130,130` 后先检测是否仍在飞行；仅在 `FLYING` 时执行下坐骑/进店补救，`UNKNOWN` 时记录日志并走现有 retry。

### 唐德 - 2026-06-07 五环买鞋入口下坐骑策略

Status: implemented / compile passed / waiting runtime validation

Changed:

- `FiveRingTaskV2.buyShoes(...)`
- 删除原来的 `phaseRetryCount > 0` 就在 retry 前盲按坐骑切换快捷键的逻辑。
  - 只在鞋店入口 watcher 返回 `ARRIVED` 后执行门口处理。
- 新增 `handleShoeShopDoorAfterArrival(...)`
  - 到达长安 `130,130` 后先短等 `2s`，看是否自动进入 `牛记布店`。
  - 未进入时，默认可能仍在坐骑/飞行状态，先按一次 `Alt+C`。
  - 再短等 `1.5s` 确认是否进店。
  - 仍未进店时调用 `GameStateUtil.detectFlyingState(...)`：
    - `FLYING`：再按一次 `Alt+C`，再确认进店。
    - `NOT_FLYING`：不再多按，直接让后续 retry 重新精确点 `130,130`。
    - `UNKNOWN`：记录 warning，直接让后续 retry 重新精确点 `130,130`。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 跑一次缺鞋五环，重点看 `『忍者』影` 到达 `长安(130,130)` 后日志：
  - `arrived at entry door`
  - `door auto-enter missed, first dismount submitted=...`
  - 如仍未进店，应出现 `[flying-status]` 和 `flying state after failed first dismount`
  - 最终应进入 `牛记布店` 或明确进入 `retry exact 130,130`。

### 唐德 - 2026-06-07 五环买鞋入口误触发坐骑检测修正

Status: implemented / compile passed / waiting runtime validation

Issue:

- 最新日志里 `『忍者』影` 还没到长安 `130,130`，就开始按坐骑切换快捷键和打开飞行/坐骑状态面板。
- 具体时间线：
  - `10:42:10` 第一次去长安失败，停在 `大唐境内(309,154)`。
  - `10:43:25` 第二次只完成了地图级到达：`current=长安(211,105)`，对应 intent 目标是 `target=长安(null,null)`。
  - 旧逻辑只看 watcher 返回 `ARRIVED`，把这个地图级到达误当成鞋店门口到达，于是提前执行门口下坐骑检测。
- 状态面板没关闭的原因：日志确认发送的是 `Ctrl+U`，但实际游戏里没有关闭该面板；这个面板应按 `Alt+U` toggle 关闭。

Changed:

- `FiveRingTaskV2`
  - 新增门口到达过滤：只有 pathing intent 的目标坐标是长安 `130,130`，且当前坐标接近 `130,130` 时，才允许进入鞋店门口下坐骑/飞行检测。
  - 地图级到达 `长安(null,null)` 会记录 `arrived in target map but not door intent; skip dismount probe`，不会再触发坐骑检测。
- `GameStateUtil.detectFlyingState(...)`
  - 状态面板关闭从 `Ctrl+U` 改为再次按 `Alt+U`，和打开动作保持同一个 toggle。

Verify:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- FiveRingTaskV2.java GameStateUtil.java` only reported CRLF conversion warnings.

Next validation:

- 再跑一次缺鞋五环时，先看 `『忍者』影` 到达 `长安(211,105)` 是否只出现 skip 日志。
- 只有后续精确点击/移动到 `130,130` 后，才应该出现 `arrived at entry door`、`door auto-enter missed`、`[flying-status]`。
- 检查 `Alt+U` 打开的状态面板是否在检测后关闭。

### 唐德 - 2026-06-07 五环买鞋已在 130,130 但未下坐骑修正

Status: implemented / compile passed / waiting runtime validation

Issue:

- 最新一轮 `『忍者』影` 已经被 fresh location 读到 `长安(130,130)`，但仍没有触发下坐骑。
- 日志显示 exact mini-map click 后边缘像素没有确认移动：
  - `navigation.currentMap result=POINT_NOT_REACHED`
  - 随后 fresh position 立即读到 `map=长安 coord=(130,130)`。
- 旧门口处理只在 watcher terminal `ARRIVED` 分支执行；`POINT_NOT_REACHED` 失败分支即使 fresh 坐标已经在门口，也只是 retry，所以不会按坐骑切换快捷键下坐骑。

Changed:

- `FiveRingTaskV2`
  - `POINT_NOT_REACHED` 后新增 fresh coordinate fallback：
    - 调用 `playerStateService.syncMyPosition()`。
    - 如果当前 fresh 坐标是 `长安(130,130)` 附近，构造一个门口 arrival snapshot，并复用 `handleShoeShopDoorAfterArrival(...)`。
    - 这样“已经站在门口但没有移动像素变化”的情况，也会进入下坐骑/飞行状态检测/进店确认流程。

Verify:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- FiveRingTaskV2.java` only reported CRLF conversion warning.

Next validation:

- 下一次如果日志出现 `POINT_NOT_REACHED` 后马上 `map=长安 coord=(130,130)`，应继续看到：
  - `failed entry click but fresh position is at door; run door handling`
  - `door auto-enter missed, first dismount submitted=...`
  - 必要时 `[flying-status]` open/close 都应为 `Alt+U`。

Correction:

- 上面的 fresh-coordinate fallback 是救急方向，已撤掉；五环任务不应该自己判断 `130,130` 门口到达。
- 真正问题在 `NavigationService.navigateInCurrentMap(...)` 的 exact mini-map click 分支：
  - 普通 handoff click 在 `NO_PATHING` 时已经会发布 intent 给 watcher。
  - 但 `exactMiniMapClickOnly` 分支仍使用旧逻辑，边缘像素未确认移动就直接返回 `POINT_NOT_REACHED`，导致 `target=(130,130)` intent 没注册/没保留。
- 已改为：当 request 设置了 `returnOnPathingStarted=true` 且 `publishWindowPathingIntent=true` 时，exact click 即使没有 immediate edge delta，也返回 `PATHING_STARTED`，由 finally 注册 window pathing intent。
- 这样 watcher 继续作为到达判定权威；如果后台坐标读到 `长安(130,130)`，watcher 应产出 `ARRIVED`，五环再进入门口下坐骑/进店处理。

Verify:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- NavigationService.java FiveRingTaskV2.java` only reported CRLF conversion warnings.

Next validation update:

- 下一次看 `『忍者』影` exact click，如果边缘像素仍未确认，应出现：
  - `exact mini-map click submitted without immediate edge delta; handoff to window observer`
  - `window pathing intent registered ... target=(130, 130)`
  - watcher 后续 `state=ARRIVED ... current=长安(130, 130)`
  - 然后五环才出现 `arrived at entry door` 和下坐骑逻辑。

### 谢帅 - 2026-06-07 五环 V2 PREPARE 启动清理瘦身

Status: implemented / compile passed / waiting runtime validation

Issue:

- 最新日志中刑部 `hwnd-4188C` 在 `13:48:21.297` 已经因为 `ACCEPT_TASK:outside-yield` 放权，`afterReleaseMs=0`，说明导航放权本身是快的。
- 但大叔 `hwnd-2210D8` 接权后进入 `wuhuan-v2:PREPARE`，先跑 `uiCleanerService.cleanUpAll()`。
- `cleanUpAll()` 会进入 `forceCloseDialog()`，从而触发 `DialogService.handleDialog(INSPECT)`、`Alt+4` 等完整 dialog 检测；大叔第一次真实 focus 到 `playerState:healAll` 是 `13:48:26.816`，比拿权晚约 5.5 秒。

Changed:

- `UICleanerService`
  - 新增 `cleanTaskStartupChromeOnly(source)`：只关世界地图和普通 X 窗，不执行 `forceCloseDialog()`，避免启动前置误跑业务 dialog 检测。
- `FiveRingTaskV2`
  - `prepare-1` 从 `uiCleanerService.cleanUpAll()` 改为 `uiCleanerService.cleanTaskStartupChromeOnly("wuhuan-v2:prepare")`。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 下一轮五环 V2 日志里，`wuhuan-v2:PREPARE` 开头应看到 `UI startup chrome cleanup...`，不应再出现 `ui-cleaner:force-close` / `dialog:hidePlayerNames:handle-dialog_INSPECT`。
- 刑部导航 `outside-yield` 后，大叔进入 `PREPARE` 到第一次 focus 的间隔应明显缩短；剩余耗时主要应来自体检和包裹检查。

### 唐德 - 2026-06-07 坐骑快捷键纠正为 Alt+C

Status: implemented / compile passed / waiting runtime validation

Issue:

- 五环买鞋入口下坐骑之前误用了无效快捷键。
- 用户确认游戏里的坐骑/飞行切换快捷键是 `Alt+C`。
- 旧的无效快捷键链路已经不应再用于下坐骑。

Changed:

- `InputActionType` / `InputAction` / `InputProvider` / `InputSequences`
  - 新增 `PRESS_ALT_C` / `pressAltC(...)`。
- `BoundWindowKeyboardService`
  - `AltShortcut` 新增 `ALT_C("Alt+C", 0x43, 0x2E)`，键盘-only 场景优先走 HWND 后台投递。
- `InputActionWorker`
  - 把 `PRESS_ALT_C` 接入 Alt shortcut 分组、后台快捷键映射和真实输入 fallback。
- `WinApiMouseController`
  - 新增物理 fallback `pressAltC()`。
- `FiveRingTaskV2`
  - 鞋店门口第一次下坐骑和确认 `FLYING` 后第二次下坐骑都改为 `inputSequences.pressAltC(...)`。
- `XiuluoTaskV2`
  - retry 前切坐骑也改为 `Alt+C`，避免同类错误。

Verify:

- `mvn -q -DskipTests compile` passed.
- `git diff --check` on touched input/task files passed, only CRLF conversion warnings.

Next validation:

- 下一轮缺鞋五环看 `『忍者』影` 到达 `长安(130,130)` 后日志应出现 `PRESS_ALT_C` / `shortcut=Alt+C` / HWND `Alt+C`。
- 如果 `Alt+C` 仍未下坐骑，再查 HWND 后台投递是否被游戏接收；但第一层错误已经确定是快捷键写错。

### 谢帅 - 2026-06-08 五倍恢复老式 blocking 导航

Status: implemented / compile passed / waiting runtime validation

Issue:

- 五倍仍是老式 blocking 导航，没有完整接入 `DebugNavigationStressTask` 的后台 route-dialog preparation 状态机。
- 但 `NavigationService.navigateToMap(...)` 在世界地图路线点击后无条件创建 `DialogPreparationRequest`。
- 因此五倍会出现半套新架构：watcher 后台 OCR route dialog，前台 blocking loop 继续等/重试，导致 `宝象国商会` prepared action 被算出后又被 retry 导航打失效。

Changed:

- `NavigationService`
  - `submitWorldMapSearchAndClickDestination(...)` 成功后，只有 `request.isReturnOnPathingStarted()` 为 true 才调用 `requestRouteDialogPreparationAfterMapRouteClick(...)`。
  - 五倍当前 `returnOnPathingStarted=false` 的导航会回到老方式：不登记后台 route-dialog preparation，不让 watcher 半路帮它算 `宝象国` 传送选项。

Verify:

- `mvn -q -DskipTests compile` passed.

Next validation:

- 下一轮五倍到 `宝象国` 时，不应再出现 `window-dialog-preparation:wubei:宝象国` / `dialog prepared action invalidated` 这种后台 prepared action 被打废的日志。
- 如果仍慢，应继续查老式 blocking 导航自身的前台 route dialog OCR/点击耗时，而不是 watcher-prepared-action 生命周期。

### 唐德 - 2026-06-11 清理维护/兜底导航的 returnOnPathingStarted=false

Status: implemented / compile passed / waiting runtime validation

Context:

- 用户希望逐步收掉 `returnOnPathingStarted(false)` 的旧 blocking 分支，避免长期维护两套路由/寻路语义。
- 本次先处理用户点名的几条：五倍医保宝/修装备维护、修罗医保宝维护、五环买鞋失败 fallback。Debug 工具暂不管；修罗接任务短距离动态 false 单独保留待讨论。

Changed:

- `WubeiTask`
  - 五倍维护 hook 导航从 `returnOnPathingStarted(false)` 改为 `true`。
  - 新增 `PATHING_STARTED` / `STOPPED` 分支：维护 NPC 导航一旦开始寻路，就返回 `WubeiStepOutcome.pathingStarted(...)`，不再继续点 NPC。
- `XiuluoTaskV2`
  - heal-pet 维护 hook 调用 `runMaintenanceBroadcastAttempt(...)` 的最后一个参数从 `false` 改为 `true`。
  - repair-equipment 原本已经是 `true`。
- `FiveRingTaskV2`
  - 买鞋失败后的 fallback 导航到修装备 NPC 显式加上 `returnOnPathingStarted(true)`。

Verify:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- WubeiTask.java XiuluoTaskV2.java FiveRingTaskV2.java` passed, only CRLF conversion warnings.

Follow-up:

- 正式 task 中已无显式 `returnOnPathingStarted(false)`。
- `XiuluoTaskV2` 接任务 NPC 短距离动态 false 也已移除；近距离仍优先直接 `clickNpcSmart(...)`，需要导航时统一 `returnOnPathingStarted(true)`。
- Debug 工具 `CurrentToChanganEastDebugMain` 仍未显式设置，默认 false，按用户要求暂不处理。

Architecture conclusion:

- 正式业务导航已经迁移到 `returnOnPathingStarted(true)` 语义。
- 以后 `NavigationService.navigateToNPC(...)` 的正式 task 调用，只要提交了移动/寻路，就应该返回 `PATHING_STARTED` 并放权，由 window watcher / phase state 后续确认到达、停错点、弹窗或重试。
- 不再维护 `returnOnPathingStarted(false)` 的 blocking 业务分支；后续看到专门为 false case 写的判断、prepared-dialog 分叉、blocking 等待逻辑，应优先评估删除或合并到 true/handoff 语义。
- `NavigationRequest` 当前 builder 默认值仍是 `false`，这是残留风险。正式业务新调用必须显式写 `.returnOnPathingStarted(true)`，除非是 debug/local one-off，并在代码注释里说明为什么需要 blocking。
- Debug 工具和注释掉的旧任务代码不纳入本结论。

### 唐德 - 2026-06-11 五环队员窗口改派自动战斗

Status: implemented / compile passed / waiting runtime validation

Issue:

- 用户确认：五环如果是队员窗口启动，不应执行五环业务动作；不要接任务、导航、点 NPC、买鞋等。
- 队员窗口只需要直接进入自动战斗/挂机逻辑。

Changed:

- `TaskTeamAssignmentPolicy`
  - 队员窗口请求 `WUHuan` / `WUHuan_V2` 时，一律改派 `TaskType.AUTO_BATTLE`。
  - 五环启动前现在始终需要 role preflight；UNKNOWN 仍允许跑五环，MEMBER 则改派自动战斗。
- `WindowTaskRunner`
  - live role detection 返回 UNKNOWN 时，如果 window context 已经有 `MEMBER` / `LEADER`，使用已有窗口身份参与分派，避免已有队员身份被 UNKNOWN 覆盖。

Verify:

- `mvn -q -DskipTests compile` passed.

Runtime expectation:

- 队员窗口启动五环时，日志应出现 `task reassigned by team role: requested=WUHuan... role=MEMBER resolved=AUTO_BATTLE` 或 `WUHuan_V2 -> AUTO_BATTLE`。
- 队员窗口不应进入 `FiveRingTaskV2` 的 prepare/accept/navigation/buy-shoe 流程。

### 谢帅 - 2026-06-11 收敛 current-map 到 pathing handoff 语义

Status: implemented / compile passed

Context:

- 另一个 agent 已确认正式 task 中没有显式 `returnOnPathingStarted(false)` 调用。
- 继续保留 current-map 的 true/false 双分支会让五环/压力测试维护两套路由行为，尤其容易出现“点了小地图后是本轮继续 retry 还是交给 watcher”的歧义。

Changed:

- `NavigationRequest`
  - builder 默认 `returnOnPathingStarted` 从 `false` 改为 `true`。
  - 注释更新为：正式 task 默认使用 pathing handoff；debug 如需旧 blocking 行为必须显式 opt out 并说明原因。
- `FiveRingTaskV2`
  - `clickShoeShopEntryExact(...)` 去掉 `returnOnPathingStarted` 透传参数，内部固定使用 `true`。
- `XiuluoTaskV2`
  - `runMaintenanceBroadcastAttempt(...)` 去掉 `returnOnPathingStarted` 透传参数，医保宝/修装备维护 NPC 导航固定使用 `true`。
- `NavigationService.navigateInCurrentMap(...)`
  - 移除旧 blocking/current-map false 分支。
  - 不再做进入 current-map 前的长 movement probe。
  - 小地图点击仍必须先确认已触发移动；确认后同步关闭小地图，再注册 pathing intent 并返回 `PATHING_STARTED`。
  - 移除旧的 `clickMiniMapPointAndConfirm(...)` / `syncAndCheckArrived(...)` helper。
  - 删除 current-map 里只服务旧 debug false/特殊 close 的残留分支。

Verify:

- `mvn -q -DskipTests compile` passed.

Remaining:

- `NavigationService.navigateToMap(...)` 里仍有若干 `request.isReturnOnPathingStarted()` 分支。正式调用现在都会走 true/default true，但跨地图 false/blocking loop 的删除范围更大，建议单独一轮处理，避免把 world-map route dialog 已验证逻辑一起改乱。

### 谢帅 - 2026-06-11 清理 navigateToMap 旧 blocking 分支

Status: implemented / compile passed

Context:

- 正式业务导航和 debug pressure path 现在都走 `returnOnPathingStarted=true` / watcher handoff。
- `navigateToMap(...)` 里仍保留一整段旧 blocking loop：提交世界地图路线后，前台继续检测移动、同步坐标、retry 世界地图。这段只服务旧 false 语义，会和 watcher/runner 的职责重复。

Changed:

- `NavigationService.navigateToMap(...)`
  - route dialog preparation target 变更清理、prepared route dialog 消费改为无条件按 handoff 语义执行，不再包 `isReturnOnPathingStarted()`。
  - 世界地图路线提交失败后立即返回 `MAP_NOT_REACHED`，交给 task 层清理/retry。
  - 世界地图路线提交成功后立即创建 route-dialog preparation request，返回 `PATHING_STARTED`，由 window watcher 判断移动、到达、停错点或后续 retry。
  - 删除旧 180 秒 blocking loop：不再在 `navigateToMap(...)` 里前台 `detectMovementState()`、`syncMyPosition()`、重复点世界地图结果。
  - 删除旧 loop 专用常量和 `isActiveNavigationMovement(...)`。

Verify:

- `mvn -q -DskipTests compile` passed.

Remaining:

- `NavigationService` 里仅剩日志打印 `returnOnPathing` 字段，没有业务分支依赖 false。
- 下一轮实测重点看跨地图 route dialog：提交路线后是否快速放权、watcher 是否能正确把 `PATHING_STARTED` 推进到 ARRIVED / STOPPED_AWAY / prepared dialog click。

### 谢帅 - 2026-06-11 删除 NavigationRequest.returnOnPathingStarted 字段

Status: implemented / compile passed

Context:

- `navigateInCurrentMap(...)` 和 `navigateToMap(...)` 已经统一为 pathing handoff 语义。
- 代码中不再存在 false/blocking 分支，继续保留 request 字段会制造“还有两套导航模式”的错觉。

Changed:

- `NavigationRequest`
  - 删除 `returnOnPathingStarted` 字段。
- `NavigationService`
  - 删除日志里的 `returnOnPathing` 输出。
  - 更新 `navigateToNPC(...)` JavaDoc，不再描述可配置 return-on-pathing 行为。
- `DebugNavigationStressTask` / `FiveRingTaskV2` / `XiuluoTaskV2` / `WubeiTask`
  - 删除所有 `.returnOnPathingStarted(true)` builder 调用。

Verify:

- `rg -n "returnOnPathingStarted|isReturnOnPathingStarted|getReturnOnPathingStarted" src/main/java -S` returns no matches.
- `mvn -q -DskipTests compile` passed.

Follow-up:

- 后续导航设计不要再新增 blocking/return-on-pathing 开关；如果某个 debug 需要同步等待，应另起明确 debug-only API 或局部测试代码，不能回塞到正式 `NavigationRequest`。

### 谢帅 - 2026-06-11 删除 NavigationRequest.publishWindowPathingIntent 字段

Status: implemented / compile passed

Context:

- `returnOnPathingStarted` 已移除后，所有 `PATHING_STARTED` 都应交给 window watcher/runner 后续判断。
- `publishWindowPathingIntent=false` 会造成裂缝：导航已经返回 `PATHING_STARTED`，但 runner 没有对应 intent，任务拿不到明确 ARRIVED / STOPPED_AWAY 目标状态。

Changed:

- `NavigationRequest`
  - 删除 `publishWindowPathingIntent` 字段。
- `NavigationService`
  - `registerWindowPathingIntent(...)` 不再检查 request 开关；只要有当前 window runtime，就登记 pathing intent。
- `DebugNavigationStressTask` / `FiveRingTaskV2`
  - 删除 `.publishWindowPathingIntent(true)` builder 调用。

Kept:

- `NavigationRequest.randomizeMiniMapClickPoint` 保留。
  - 这是有效业务参数：普通导航默认加随机偏移；五环买鞋进店这种精确小地图点可设为 `false`。

Verify:

- `rg -n "publishWindowPathingIntent|isPublishWindowPathingIntent|getPublishWindowPathingIntent" src/main/java -S` returns no matches.
- `mvn -q -DskipTests compile` passed.

### 谢帅 - 2026-06-11 更新自动战斗取消模板

Status: implemented

Context:

- 游戏更新后自动战斗面板尺寸/位置可能变化，用户在 `images/template/battle` 放入了新的 `222.png` 作为取消自动区域截图。

Changed:

- 使用 `ImagePreprocessor.countGreenPixelsHSV(...)` 同等阈值（HSV `50..75 / 150..255 / 180..255`）将 `images/template/battle/222.png` 洗成绿字 mask。
- 覆盖 `images/template/battle/quxiao_zidong_green.png`。

Verify:

- 新模板尺寸：`102x19`。
- 绿字像素数：`241`。

Note:

- 本次只替换图片资产，没有调整 `AutoCombatPanelService` 的面板中心 offset；如果后续实测仍偏移，再单独校准 offset。

### 唐德 - 2026-06-11 自动战斗轮数改为估算刷新，不再正式 OCR

Status: implemented / compile passed

Context:

- 多窗口进战斗后，原链路会在 entry maintenance 里对自动战斗面板剩余轮数做本地 OCR。
- 五个窗口几乎同时进战斗时，Python OCR 会出现 CPU 峰值。
- 用户确认：正式链路不需要读取屏幕轮数；只要按 `Alt+8` 成功，自动战斗轮数可直接视为重置到 25。

Changed:

- `GameContext.State`
  - 新增窗口级 `autoCombatEstimatedRounds` 和 `lastAutoCombatRefreshAt`。
  - 这些状态跟随每个窗口自己的 `GameContext.State`，不再作为任务局部状态。
- `AutoCombatPanelService`
  - `verifyAndAlignPanel()` 仍确认面板存在并按需拖拽。
  - 正式链路不再调用 `readRemainingRounds(...)`，即不再通过本地 OCR 读轮数。
  - 轮数刷新改成估算策略：
    - 轮数未知：按 `Alt+8`，直接记 `25`。
    - 估算轮数 `<= 10`：按 `Alt+8`，直接记 `25`。
    - 距离上次刷新超过 `bot.dhxy.auto-battle-refresh-interval-ms`：按 `Alt+8`，直接记 `25`。
    - 其他情况只记录“估算健康”，不 OCR。
  - `ensurePanelMatchVisible(...)` 如果因为面板未识别而主动发送 `Alt+8`，也会把窗口级估算轮数重置到 `25`。
  - `recordCombatExit()` 现在从 `GameContext` 读取/扣减估算轮数，每场战斗按 `3` 扣。

Verify:

- `mvn -q -DskipTests compile` passed.

Follow-up:

- 下一轮实测看日志中是否不再出现正式链路的 `auto-combat panel rounds OCR result`。
- 如果仍有 OCR 日志，优先确认是否来自 debug 工具或未迁移的调用点，而不是恢复旧 OCR 逻辑。

### 唐德 - 2026-06-11 本地 OCR sidecar 限制并发与底层线程

Status: implemented / sidecar restarted

Context:

- 用户在任务管理器看到 `Python` 持续/反复接近 40% CPU。
- 自动战斗轮数 OCR 已从正式链路移除，但启动阶段仍可能有队伍角色、摄妖香、地图路线等 OCR 请求。
- `scripts/local_ocr_server.py` 使用 `ThreadingHTTPServer`，多窗口请求会并发进入 RapidOCR；RapidOCR/ONNX/OpenMP 默认可为单个请求开多条 native worker 线程，因此并发窗口会把 CPU 峰值放大。

Changed:

- `scripts/local_ocr_server.py`
  - 在 RapidOCR import/load 前设置线程环境变量：
    - `OMP_NUM_THREADS=1`
    - `MKL_NUM_THREADS=1`
    - `OPENBLAS_NUM_THREADS=1`
    - `NUMEXPR_NUM_THREADS=1`
    - `OMP_WAIT_POLICY=PASSIVE`
  - 新增全局 `OCR_LOCK`，所有 `/ocr/text` 和 `/ocr/words` 请求串行进入 `load_engine()(...)`。
  - HTTP server 仍可并发接请求，但 OCR 计算本体排队执行，避免五窗口同时把 CPU 拉满。

Verify:

- 已重启本地 OCR sidecar，当前监听 `127.0.0.1:18761` 的 Python PID 为 `242012`。
- `/health` 返回 `{"ok":true}`。
- `python -m py_compile scripts/local_ocr_server.py` passed。
- 空闲 5 秒采样：Python CPU 约 `0%`。

Follow-up:

- 下一轮多窗口启动时观察 Python 峰值是否明显下降；如果仍偏高，再考虑把高频 OCR 调用点本身继续降频/缓存。

### 谢帅 - 2026-06-11 五倍显形镜寻路接入 runner，目标坐标只认 destination hint

Status: implemented / compile passed

Context:

- 用户复盘五倍 23:36 左右日志时发现：
  - 左侧任务追踪绿字点击后，显形镜阶段等待停稳用了本地 `GameStateUtil.detectMovementState()`，没有优先使用窗口 runner 的 pathing snapshot。
  - `destination hint` 解析失败后，白龙马/显形镜目标点击直接被 `no-destination-hint` 阻断，即使“点击可/目标出现”story 已经识别成功。
- 复核后明确：五倍任务怪的目标地图/坐标唯一可信来源就是绿字点击后短暂浮出的 `destination hint`。
- runner snapshot / `gameContext.getMe()` 只能代表当前角色站位，不能冒充任务怪坐标。
- 约定：确认移动/停稳交给 runner；目标坐标只从 `destination hint` 来。

Changed:

- `WubeiTask.clickTaskTrackerGreen(...)`
  - 绿字点击成功后注册 `WindowPathingIntentType.UNTARGETED_TRACKER` 到当前 `WindowRuntimeContext`。
  - 保留原 `GameStateUtil.recordMovementIntent(...)` 作为兼容信号。
- `WubeiTask.resolveProbeAfterPathing(...)`
  - 优先读取当前窗口 `WindowPathingSnapshot`。
  - runner 仍为 `ACTIVE/UNKNOWN/probeInProgress` 时直接让权等待。
  - 只有没有可用 tracker snapshot 时，才退回旧的本地 pixel/坐标移动判断。
- `WubeiTask.tryClickTrackerCombatTargetSmart(...)` / `tryDirectCombatFromTrackerHint(...)`
  - 删除上一版错误的 runner/player 坐标兜底。
  - 构造 `NpcClickRequest` 时，`mapName/mapX/mapY` 只使用 `currentTrackerDestinationHint`。
  - 如果 hint 没解析到，当前坐标不足以定位任务怪，必须清晰记录 `no-destination-hint` 并走任务自己的重试/恢复，而不是伪造目的地。

Verify:

- `mvn -q -DskipTests compile` passed。

### 何黎 - 2026-06-14 窗口 watcher prepared-action 软唤醒

Status: implemented / compile passed

Context:

- 用户指出窗口 watcher 有时已经在后台算好了可点击的对话框/任务追踪动作，但任务线程仍要等下一轮轮询才处理，导致多窗口切换和接续动作慢。
- 前一版 `WindowReadyEventBus` 只发布 `PATHING_TERMINAL`，只能唤醒“移动到达/半路停住”。
- 实际 watcher 还会准备：
  - 路线/传送 option 的 `PreparedDialogAction`。
  - 五环任务追踪面板绿字点击的 `PreparedDialogAction`。

Changed:

- `WindowReadyEventType`
  - 新增 `DIALOG_PREPARED`。
  - 新增 `TASK_TRACKER_PREPARED`。
- `WindowTaskRunner`
  - `refreshDialogPreparationSignal(...)` 成功写入 `WindowRuntimeContext.preparedDialogAction` 后发布 `DIALOG_PREPARED`。
  - `refreshTaskTrackerPreparationSignal(...)` 成功写入任务追踪 prepared action 后发布 `TASK_TRACKER_PREPARED`。
  - 事件只是 wake hint；真实业务仍必须重新读取 `WindowRuntimeContext`，不会让 watcher 直接点击/focus/推进 phase。
- `WindowReadyWaitService`
  - `waitForPathingTerminalOrTimeout(...)` 改为 `waitForPathingWakeOrTimeout(...)`。
  - 等待类型扩展为 `PATHING_TERMINAL` / `DIALOG_PREPARED` / `TASK_TRACKER_PREPARED`。
  - 如果当前窗口已经存在 prepared action，直接早醒返回，避免继续固定 sleep。
- `WubeiTask` / `XiuluoTaskV2` / `FiveRingTaskV2`
  - `PATHING_STARTED` 让权后的短等待改成调用新的 wake wait 方法。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下次实跑看 `logs/dhxy-console.log` 中：
  - `event=window.ready.publish type=DIALOG_PREPARED`
  - `event=window.ready.publish type=TASK_TRACKER_PREPARED`
  - `event=window.ready.consume ... type=...`
- 如果 prepared action 已经明显能早醒，再考虑把部分固定 `900ms` 让权等待调短或按任务阶段调参；不要一口气扩大到所有等待点。

### 谢帅 - 2026-06-13 修罗当前地图短距离导航不放权

Status: implemented / compile passed

Context:

- 用户观察到修罗队长回城后，若离接任务 NPC / 张闻只差一点距离，会打开小地图点坐标并触发 `PATHING_STARTED`，导致队长放权。
- 同类问题也出现在接任务后去旁边 `超级巫医` 医宝宝：这一步后面马上要点 NPC 触发 broadcast，不应该在短距离小地图移动时让队友插进来。
- 普通打怪目标导航仍然应该在移动开始后放权；本次只收窄修罗的短距离 leader-only 修正。

Changed:

- `NavigationRequest`
  - 新增 `keepTurnOnCurrentMapPathing`，默认 `false`。
- `NavigationService.navigateInCurrentMap(...)`
  - 小地图点击确认移动后，默认仍返回 `PATHING_STARTED`。
  - 当 `keepTurnOnCurrentMapPathing=true` 时，注册 watcher intent 后继续在当前调用里等坐标到达；到达后返回 `ARRIVED`，不把短移动变成 task turn 放权点。
- `XiuluoTaskV2`
  - `xiuluo-v2:acceptNpc`、`xiuluo-v2:returnFallback`、`heal-pet` NPC 导航启用该策略。
  - `xiuluo-v2:target` 打怪目标导航未启用，仍按普通移动放权。
- `NavigationService.navigateToLingShouVillageViaZhangWen(...)`
  - 张闻 approach 这段当前地图短距离移动也显式启用该策略，避免特殊路线绕过修罗调用侧的 keep-turn 语义。

Verify:

- `mvn -q -DskipTests compile` passed。

Next log checks:

- 回城后接任务前不应再看到 `xiuluo-v2:acceptNpc` 因短距离 current-map click 直接释放 turn。
- 医宝宝 hook 到 `超级巫医` 前不应因短距离 current-map click 释放 turn；真正放权点应仍是 broadcast 被处理后。

Follow-up:

- 下一轮五倍显形镜实测重点看：
  - 是否出现 `[wubei] window pathing intent registered for tracker click`。
  - 显形镜停稳是否走 `[wubei] resolve probe after runner pathing`，不再卡在本地 `UNKNOWN` 一分钟。
  - 如果出现 `no-destination-hint`，重点查 hint 截图/OCR/采样时机，而不是从 runner/player 坐标兜底。

### 唐德 - 2026-06-11 路线弹窗记忆改为 watcher 确认后才写成功

Status: implemented / compile passed

Context:

- 23:40 左右五倍队长从 `平顶山` 导航去 `宝象国` 时，路线弹窗实际选项为：
  - `乌鸡国城外（400两）`
  - `宝象国北驿寺（400两）`
  - `清水湖（400两）`
  - `我哪儿也不去`
- 旧记忆 `平顶山->宝象国` 存的是 `rel=(80,92)` / `宝象国皇宫（400两）`，该点在当前弹窗上会落到第一项附近。
- 更大的 bug 是点击路线记忆点后立即 `recordSuccess`，没有等 watcher 确认真的到达目标地图。

Changed:

- 删除 legacy `config/transfer_choice_memory.json` 中错误的 `平顶山->宝象国` 记忆，避免下次迁移到 `dialog_choice_memory.json` 后继续误用。
- `NavigationService` 路线弹窗点击后不再立即写成功记忆，改为写入窗口级 pending route memory。
- `WindowTaskRunner` pathing watcher 负责结算 pending route memory：
  - `ARRIVED` 且目标地图匹配时，调用 `DialogChoiceMemoryService.recordRouteSuccess(...)`。
  - `STOPPED_AWAY` 时，调用 `DialogChoiceMemoryService.recordRouteFailure(...)`，沿用现有连续失败 disable 机制。
- 暂时不加二次 OCR 验证，按用户要求只依赖 watcher 到达确认。

Verify:

- `config/transfer_choice_memory.json` 可被 `ConvertFrom-Json` 正常解析，且 `平顶山->宝象国` 已不存在。
- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮五倍从 `平顶山` 去 `宝象国` 时观察：
  - 不应再使用旧的 `宝象国皇宫（400两） rel=(80,92)`。
  - 日志应先出现 `[dialog-choice-memory] pending route click`。
  - 只有 watcher 看到 `ARRIVED` 后才出现 `[dialog-choice-memory] success`。

### 谢帅 - 2026-06-12 五倍暗雷重抽后左侧任务面板缓存失效

Status: implemented / compile passed

Context:

- 用户复盘 12:53-12:55 五倍日志时发现：第一次接到 `暗雷怪` 后重抽合理，但后面继续接任务后仍然直接取消。
- 日志证据：
  - 第一次 `READ_TRACKER` 有完整 `TaskTrackerPanelService panel read`，读到 `殿前献艺|3只|暗雷怪`，触发 `dark-thunder reroll` 是正常业务规则。
  - 第二次接任务成功后，`READ_TRACKER` 同一毫秒直接再次输出 `dark-thunder task detected`，中间没有新的 `TaskTrackerPanelService panel read`。
- 原因：`currentTrackerPanel` 在接任务成功后没有清空；下一次 `READ_TRACKER` 看到缓存仍是 found，就复用了上一轮/上一次重抽的左侧任务追踪结果。

Changed:

- `WubeiTask.runAcceptTaskPhase(...)`
  - 接任务成功后清空 `currentTrackerPanel`，强制下一步 `READ_TRACKER` 重新截图读取左侧任务面板。
- `WubeiTask.runReadTrackerPhase(...)`
  - 命中 `暗雷怪` 并准备重抽前清空 `currentTrackerPanel`，避免重抽路径继续带着旧快照。
  - 后续复查发现只在接任务/暗雷分支清缓存仍然偏局部，已把 `READ_TRACKER` 改成统一刷新边界：每次进入该 phase 都重新调用 `resolveTrackerPanelWithAnchorRecovery(...)`，不再因为 `currentTrackerPanel.isFound()` 复用旧快照。
- 暗雷怪重抽规则本身没有改。

Verify:

- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-13 五环买鞋入口必须 exact 到 130,130

Status: implemented / compile passed

Context:

- 用户指出 ID 末尾 `3529` 的窗口去买鞋时停在 `长安(130,129)`，但买鞋入口逻辑要求必须到 `长安(130,130)`。
- 日志确认旧行为：
  - `source=wuhuan-v2:shoe-shop-entry-exact-130-130`
  - `target=长安(130, 130)`
  - `current=长安(130, 129)`
  - watcher 仍返回 `state=ARRIVED`
- 根因：
  - `NavigationRequest.arrivalTolerance` 默认是 5。
  - `clickShoeShopEntryExact(...)` 没显式设置 `arrivalTolerance(0)`。
  - `ARRIVED/SUCCESS` 后手动注册 `WindowPathingIntent` 时也写了 `tolerance(5)`。
  - `handleShoeShopDoorAfterArrival(...)` 还允许 `distance <= 6` 进入门口/下坐骑逻辑。

Changed:

- `FiveRingTaskV2.clickShoeShopEntryExact(...)`
  - 增加 `.arrivalTolerance(0)`，只允许当前坐标等于 `130,130` 才算到达。
- `FiveRingTaskV2.buyShoes(...)`
  - 手动注册 `SHOE_SHOP_ENTRY_NAV_SOURCE` watcher intent 时改为 `.tolerance(0)`。
- `FiveRingTaskV2.handleShoeShopDoorAfterArrival(...)`
  - 门口处理不再接受 `distance <= 6`。
  - 只有 `currentX == 130 && currentY == 130` 才进入 auto-enter / dismount 检查。
  - `130,129` 这类附近点会跳过门口处理并继续重试 exact 入口导航。

Verify:

- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-13 DialogService 点击绿字模板不再重复 Alt+4

Status: implemented / compile passed

Context:

- 用户在 19:29:30 附近观察到队长点击修罗 tooltip 进入战斗前有两次 `Alt+4`。
- 日志来源：
  - 第一次：`dialog:hidePlayerNames:before-learned-memory`，来自 `NpcClickService` 点击目标前的全屏/场景探测，保留。
  - 第二次：`dialog:greenTemplateOption:xiuluo-v2:enter-battle:target-clicked`，来自 `DialogService` 已知进入战斗 option 后的绿字模板点击验证，不需要。

Changed:

- `DialogService.handleGreenTemplateOptionDirect(...)`
  - `CLICK_GREEN_TEMPLATE` 自己验证 dialog 类型时，改为 `detectDialogSnapshotDirect(..., false)`。
  - 只跳过这条已知 dialog 内处理路径的 `Alt+4`。
  - 未改 `NpcClickService` 的前置隐藏玩家名，也未改其他全屏探测路径。

Verify:

- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-13 视觉点击修改必须走 testcase replay 规则

Status: documented

Context:

- 用户明确要求：凡是修改“怎么识别/怎么点击”的逻辑，不能只靠口头解释或现场观察，必须用 testcase 跑出来并画标记图。
- 适用范围包括：
  - 小地图匹配和点击；
  - 世界地图输入框、路线结果、绿色坐标点击；
  - 任务追踪绿字点击；
  - NPC/template/dialog option 这类截图/OCR/模板驱动的点击点。

Changed:

- `AGENTS.md`
  - 新增规则：visual matching or click-target changes must be verified through testcase replay。
  - 要求使用或新增 `images/test-cases/...` 下的原始截图，用 replay/debug 工具跑当前算法，输出带红点/红框的 marked image。
  - marked image 必须能看出识别锚点、匹配框、最终点击点。
  - 修改后要在 `docs/ACTIVE_WORK.md` 记录 testcase 输入、输出图路径和执行命令。

Verify:

- Markdown-only change，未运行编译。

### 唐德 - 2026-06-12 路线结果回放图增加目的地标记

Status: implemented / compile passed

Context:

- 用户给出“从龙宫到长安”的路线结果截图，怀疑当前算法会误点 `龙宫(140,56)`，需要用当前代码实际跑图并把识别到的目的地画出来。
- 正式路线识别仍在 `GameTextLineOcrService.verifyWorldMapRouteDestination(...)` 和 `findLastWorldMapRouteCoordinate(...)`，本次只增强离线回放标记，不改正式导航业务。

Changed:

- `WorldMapRouteGuardReplayDebug.writeMarkedImage(...)`
  - 原来只标红最终绿色坐标点击点 `CLICK`。
  - 现在额外在黄色目的地 OCR 中心画红框/十字并标 `DEST`，用于确认代码到底把哪个黄色文本当成目的地。

Verify:

- `mvn -q -DskipTests compile` passed。
- 已用当前代码回放本地同类路线图：
  - input: `images/temp/hwnd-3CE0D38/map_result_scan.png`
  - expected: `兰若寺`
  - output: `images/temp/world_map_route_guard_replay/20260612_233330/hwnd-3CE0D38_marked.png`
  - 结果：`DEST` 标到 `兰若寺`，`CLICK` 标到 `长安(374,16)`。

Follow-up:

- 用户这张“龙宫 -> 长安”的原图如果需要精确验证，需要保存为本地文件后用同一个 replay 命令跑：
  - `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.WorldMapRouteGuardReplayDebug" "-Dexec.args=长安 <raw-image-path>"`

Update:

- 用户已把原图保存为 `images/test-cases/world-map-route/raw/img.png`。
- 复现旧结果：
  - 整图黄字 OCR 只读到第一段里的 `长安城`。
  - `matchShortName` 把 `长安城` fuzzy 成目标 `长安`，导致 packed-segment OCR 没有继续跑。
  - `DEST` 错误落在第一段 `长安城`，`CLICK` 随后落到第一段 `龙宫(140,56)`。
- 修复：
  - `GameTextLineOcrService.findLastWorldMapRouteDestination(...)`
    - 当整图目的地不是“精确等于 expected”时，即使它 fuzzy matched，也继续跑 packed-segment OCR。
    - 如果 packed-segment 找到精确 expected，优先用 packed 的目的地坐标覆盖 fuzzy 结果。
- 验证：
  - `mvn -q -DskipTests compile` passed。
  - replay 命令：
    - `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.WorldMapRouteGuardReplayDebug" "-Dexec.args=长安 D:\mavenProject\DHXY\images\test-cases\world-map-route\raw\img.png"`
  - output:
    - `images/temp/world_map_route_guard_replay/20260613_000057/raw_marked.png`
  - 新结果：`DEST` 标到底部真正 `长安`，`CLICK` 标到第二段 `长安城东(306,188)`，不再点第一段 `龙宫(140,56)`。

Testcase replay correction:

- 用户指出单图验证不等于走完整 testcase。已补 `WorldMapRouteGuardReplayDebug --testcase-all`：
  - 从 `images/temp/world_map_route_online_dry_run/**/summary.csv` 读取 expected map。
  - 映射到 `images/test-cases/world-map-route/raw` 下的 raw testcase 图片运行。
  - 自动附加手工 case `images/test-cases/world-map-route/raw/img.png`，默认 expected=`长安`。
  - marked 输出文件名改为原图 stem，避免多张图都写成 `raw_marked.png` 被覆盖。
- 有效全套命令：
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.WorldMapRouteGuardReplayDebug" "-Dexec.args=--testcase-all"`
- 全套结果：
  - total=177
  - passed=177
  - failed=0
  - outputDir=`images/temp/world_map_route_guard_replay/20260613_001405`
  - marked image count=177
- 这次才算真正按“视觉点击修改必须走 testcase replay”的规则完成验证。

### 谢帅 / Jason / Hook - 2026-06-12 修罗 V2 Runner/Watcher 框架评估

Status: reviewed / proposal recorded / no code changes in this entry

Context:

- 用户要求重新评估修罗 V2 框架，重点不是修罗业务逻辑，而是 `WindowTaskRunner` / watcher / prepared dialog / pathing snapshot 是否真正被修罗消费。
- 这轮拉了两个只读评估智能体：
  - Jason：重点看 `XiuluoTaskV2` 状态机如何接 `NavigationService` 返回值。
  - Hook：重点看 `WindowTaskRunner`、`WindowRuntimeContext`、`NavigationService` 的 watcher/prepared-action 生命周期。
- 两边结论一致：修罗 V2 不是完全没接 runner；`TaskType.XIULUO_V2` 已经进入 watcher，route dialog preparation 和 pathing watcher 都能工作。问题在修罗消费层只接了一半，导致后台已经算出的结果没有及时变成前台动作。

Verified current wiring:

- `WindowTaskRunner.shouldRunWindowObserver(...)` 已包含 `TaskType.XIULUO_V2`。
- `WindowTaskRunner.refreshDialogPreparationSignal(...)` 已处理 `DialogOperation.ROUTE_TRANSFER`，能后台准备路线/传送 dialog 点击点。
- `WindowTaskRunner.refreshPathingSignal(...)` 能为修罗 pathing intent 产出 `WindowPathingSnapshot`。
- `NavigationService.navigateToMap(...)` 已经会：
  - 优先消费 ready `PreparedDialogAction`；
  - 在路线链接点击后注册 `DialogPreparationRequest`；
  - 返回 `DIALOG_PREPARING` 让任务层短让权。
- `XiuluoTaskV2.navigationOutcome(...)` 已补 `DIALOG_PREPARING` 分支：不再把后台正在算路线 dialog 的状态当作 nav failure 进入 cleanup。

Main gaps:

1. `XiuluoTaskV2.continueIfNavigationStillPathing(...)` 没有优先消费 watcher snapshot。
   - 当前主要还是 `hasReachedTargetApproach(...)` + `GameStateUtil.detectMovementState()`。
   - 如果 watcher 已经给出 `ARRIVED` / `STOPPED_AWAY` / ready prepared action，修罗仍可能继续按旧前台探测等待。
   - 这会造成“后台已经知道到了/停了/可点 dialog，但前台继续等或重试”的延迟。

2. `NavigationService.navigateToMap(...)` 复用已有 active pathing snapshot 时，仍可能在 finally 里重新注册 intent。
   - 重新 `markPathingStarted(...)` 会刷新 snapshot 时间线。
   - 这可能推迟 `STOPPED_AWAY` 或 arrival 判断，让修罗感觉“走了很久还没结算”。

3. 修罗维护 hook 手写导航状态处理，漏接部分新状态。
   - `runMaintenanceBroadcastAttempt(...)` 当前主要识别 `PATHING_STARTED` / `STOPPED` / `ARRIVED`。
   - `DIALOG_PREPARING`、`DIALOG_OPENED`、`POINT_NOT_REACHED` 这类导航层状态容易被当成普通 hook retry。
   - 后续 heal-pet / repair 维护失败路径可能触发 cleanup，把正常的后台准备状态清掉。

4. `POINT_NOT_REACHED` 在主导航桥里仍容易进入 failure/recovery。
   - 小地图点击没确认移动、当前地图点位未触发时，不应该立刻升级成完整 nav recovery / `cleanUpAll()`。
   - 更合理的是轻量 retry 或 shared-state retry，让下一轮根据 watcher snapshot 再判断。

5. `DialogPreparationRequest` 和 `PreparedDialogAction` 清理耦合偏脆。
   - `WindowRuntimeContext.clearDialogPreparationRequest()` 当前会同时清 request、prepared action、status。
   - 导航的一些 pathing-active / pathing-consumed 分支可能误清同 target 已准备好的 action。

6. 灵兽村特殊路线仍偏阻塞。
   - `navigateToLingShouVillageViaZhangWen(...)` 点击张闻/传送后仍有同步确认逻辑。
   - 它没有完整透传 `DIALOG_PREPARING`，某些 route dialog 状态可能被压成 `MAP_NOT_REACHED`。

7. watcher 启动日志有误导。
   - 当前日志里的 `pathingProbe` 容易让人误以为只有 `DEBUG_NAVIGATION_STRESS` 才跑 pathing probe。
   - 实际 `XIULUO_V2` 只要有 active pathing intent 就会跑 watcher。

Recommended fix order:

1. P0: 先改 `XiuluoTaskV2.continueIfNavigationStillPathing(...)`。
   - 优先读取当前窗口 `WindowPathingSnapshot`。
   - `ARRIVED` / near-target：消费终态，进入下一 phase。
   - `STOPPED_AWAY`：消费终态，轻量 retry 当前导航。
   - `ACTIVE` / `UNKNOWN` / probe in progress：继续短让权。
   - 如果存在同 target ready `PreparedDialogAction`，不要继续等移动，直接让当前 phase 重新进入 `NavigationService` 消费 prepared action。

2. P0: 修 `NavigationService.navigateToMap(...)` 的重复注册。
   - 如果本轮返回 `PATHING_ACTIVE` 是因为已有 snapshot/intent 正在工作，不要再次 `markPathingStarted(...)`。
   - 保留原 watcher 时间线，让 `locationChangedAt` / `createdAt` 能真实反映这次移动。

3. P1: 统一修罗导航桥接策略。
   - `navigationOutcome(...)` 显式处理 `POINT_NOT_REACHED`，优先 shared retry，不要直接进重 recovery。
   - `DIALOG_OPENED` 如果后续导航层会返回，应按“可继续交给 dialog/下一阶段处理”的成功类状态，而不是失败。

4. P1: `runMaintenanceBroadcastAttempt(...)` 复用同一套导航状态规则。
   - `DIALOG_PREPARING`：shared yield，同 phase retry。
   - `POINT_NOT_REACHED`：轻量 retry，不立刻 cleanup。
   - `DIALOG_OPENED`：继续交给维护 dialog 处理，而不是走 hook failure。

5. P1/P2: 拆开 dialog preparation 清理语义。
   - 不要让所有 `clearDialogPreparationRequest()` 场景都连带清掉同 target ready action。
   - 至少保证 pathing-active/同目标重入时不会误删已准备好的点击点。

6. P2: 修灵兽村特殊路线状态透传。
   - `navigateToLingShouVillageViaZhangWen(...)` 对 `DIALOG_PREPARING` 直接返回，不压成 `MAP_NOT_REACHED`。
   - 后续再考虑把它改成 watcher handoff，而不是同步阻塞确认。

7. P2: 补日志。
   - `WindowTaskRunner` observer 启动日志显示真实 watcher 能力，而不是只显示 debug stress。
   - 修罗 pathing wait 日志增加 snapshot state、prepared action 是否 ready、target/source，方便下一轮实测看 runner 是否真的被消费。

Do-not-touch for this pass:

- 不改修罗接任务、读 objective、点怪、回城等业务判断。
- 不改 `GameStateUtil.isMovingByPixelDiff()` 算法。
- 不改 world-map / minimap 选点算法。
- 不把维护逻辑重新塞进修罗主线，只修状态桥接和 runner 消费。

Next concrete steps:

1. Done: 实现 P0-1：修罗 `continueIfNavigationStillPathing(...)` 消费 watcher snapshot / prepared action。
2. Next: 做一轮修罗实测，重点看 `pathing watcher update` 后是否马上进入下一 phase 或消费 prepared dialog。
3. Next: 实现 P0-2：避免 `navigateToMap(...)` 对已有 active intent 重复注册。
4. Next: 再跑修罗，比较 `PATHING_ACTIVE` / `STOPPED_AWAY` 的等待时间是否缩短。
5. Next: 实现维护 hook 的 `DIALOG_PREPARING` / `POINT_NOT_REACHED` 轻量处理。
6. Later: 根据实测再决定是否拆 `clearDialogPreparationRequest()` 与 `PreparedDialogAction` 清理。

Update:

- `XiuluoTaskV2.continueIfNavigationStillPathing(...)`
  - 已接入 `WindowTaskContextHolder.rawCurrent()`，读取当前窗口 `WindowPathingSnapshot` 和 `PreparedDialogAction`。
  - 如果 watcher 已准备好同目标 `ROUTE_TRANSFER` action，修罗不再继续前台移动探测，直接回到导航阶段让 `NavigationService` 消费缓存点击点。
  - 如果 watcher snapshot 是 `ARRIVED`，清理 pathing signal，并让当前 phase 继续下一步。
  - 如果 watcher snapshot 是 `STOPPED_AWAY`，清理 pathing signal，并让当前 phase 轻量重试导航。
  - 如果 snapshot 仍是新鲜的 `ACTIVE/UNKNOWN` 或 probe 正在跑，继续让权，不进入旧前台探测。
  - 旧的 `hasReachedTargetApproach(...)` 和 `GameStateUtil.detectMovementState()` 保留为 watcher 不可用/过期后的兜底。
- 新增 `OBSERVER_SNAPSHOT_MAX_AGE_MS=3000ms`，只用于避免吃太旧的 ACTIVE/UNKNOWN watcher 状态。
- `mvn -q -DskipTests compile` passed。

CR follow-up:

- Jason / Hook 复核后指出 P0-1 还缺一个关键边界：不能消费任意窗口 runtime 里的 pathing snapshot / prepared action，必须确认它属于当前修罗 phase。
- 已补内联归属校验：
  - `ACCEPT_TASK_NAVIGATE_TO_NPC` 只吃 `xiuluo-v2:acceptNpc...` -> `灵兽村`。
  - `AFTER_ACCEPT_MAINTENANCE_CHECK` 只吃 `xiuluo-v2:healPetNpc...` -> `灵兽村`。
  - `BEFORE_ROUTE_MAINTENANCE_CHECK` 只吃 `xiuluo-v2:repairEquipmentNpc...` -> `洛阳城`。
  - `NAVIGATE_TO_TARGET` 只吃 `xiuluo-v2:target...` -> 当前 objective map。
  - `NAVIGATE_BACK_TO_START` 只吃 `xiuluo-v2:returnFallback...` -> `灵兽村`。
- Prepared route action 现在也要求同 windowId / hwnd 绑定且 10 秒内验证过，避免旧 dialog action 把当前 phase 提前唤醒。
- watcher probe 卡死保护已补：`probeInProgress` 只有在 10 秒内才会继续压住修罗 phase；超过后打 warn 并回落到旧兜底判断。
- P0-2 已完成：`NavigationService.navigateToMap(...)` 如果只是 stale-cache guard 发现同一路径已经被 watcher 跟踪中，不再在 finally 里重复注册新的 pathing intent；真正点击路线/传送产生的新 `PATHING_STARTED` 仍正常注册。
- P1 维护 hook 桥接已补：
  - `runMaintenanceBroadcastAttempt(...)` 现在把 `DIALOG_PREPARING` 当成 watcher 正在准备路线 dialog，shared-state 让权，不进入 cleanup。
  - `POINT_NOT_REACHED` 现在轻量 shared retry 下一轮，不走 `cleanupAndLogMaintenanceRetry(...)`。
  - `DIALOG_OPENED` 继续进入后面的维护 broadcast 处理，不再被普通失败分支吞掉。
- P1 主导航桥接已补：
  - `navigationOutcome(...)` 现在把 `POINT_NOT_REACHED` 当成 shared-state retry，不直接进入失败/recovery。
  - `DIALOG_OPENED` 现在视为导航已把业务 dialog 打开，继续到下一 phase 交给任务/dialog 层处理。
- P2 灵兽村特殊路线状态透传已补：
  - `navigateToLingShouVillageViaZhangWen(...)` 不再只透传 `PATHING_STARTED`。
  - 到长安、靠近张闻、处理张闻传送框时，`DIALOG_PREPARING` 会原样返回给任务层让权等待 watcher。
  - 子步骤失败时优先返回原始 `NavigationResult`，不再全部压成 `MAP_NOT_REACHED` / `POINT_NOT_REACHED`。
- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-12 修罗路线 dialog preparing 不再进入 nav recovery

Status: implemented / compile passed

Context:

- 用户追查 22:23 附近修罗从长安去万寿山时，路线 dialog 已经弹出，但没有触发移动，随后进入 UI cleanup / nav recovery。
- 日志确认 `UI cleanup` 不是根因：
  - `22:23:07` watcher 已看到 `OPTION` 并开始 `ROUTE_TRANSFER` 后台准备。
  - `22:23:11` `NavigationService` 返回 `DIALOG_PREPARING`，表示后台还在算路线 dialog 点击点。
  - 同一时刻修罗把该结果当普通失败处理，进入 `recoverTargetNavigationFailure(...)`，从而触发 `uiCleanerService.cleanUpAll()`。
  - `22:23:12` 后台实际算出了 `万寿山（800两） click=(1083,848)`，但前台已经走进 recovery 链，错过了正常移动触发窗口。

Changed:

- `XiuluoTaskV2.navigationOutcome(...)`
  - 新增 `NavigationResultStatus.DIALOG_PREPARING` 分支。
  - 该状态现在会 `sharedState(state.retrySamePhase(...))` 并让权等待 watcher/prepared action。
  - 不再落入 `FAILED`，因此不会因为“后台正在准备路线 dialog”而立刻触发目标导航恢复和 UI cleanup。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮修罗实测重点看 22:23 这类链路：
  - `navigation phase result ... status=DIALOG_PREPARING` 后应让权等待；
  - 下一轮应消费 `dialog prepared` 的 `click=(...)`；
  - 不应马上出现 `navigation-retry:NAVIGATE_TO_TARGET:nav-recovery` 和 `UI cleanup started`。

### 谢帅 - 2026-06-12 修罗回城后跳过重复 UI cleanup

Status: implemented / compile passed

Context:

- 用户观察到修罗使用回程道具后，角色已经回到灵兽村，但进入下一轮接任务前会站住几秒。
- 日志和代码确认：正常回城成功后，下一轮从 `PREPARE_ROUND` 开始，而 `prepareRound()` 里无条件执行 `uiCleanerService.cleanUpAll()`。
- 修罗各 phase 的失败恢复路径本身已经有 cleanup/fallback；正常成功回城后的每轮 cleanup 属于重复保险。

Changed:

- `XiuluoTaskV2.prepareRound(...)`
  - 只在 `round == 1` 时执行 broad `cleanUpAll()`，保留启动/热启动脏 UI 保护。
  - 后续正常轮次跳过 prepare cleanup，直接进入接任务流程。
  - 失败路径仍由各 phase 自己的 recovery/fallback 清 UI。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮修罗实测重点看 `return item verified` 之后到 `ACCEPT_TASK_NAVIGATE_TO_NPC` 的间隔是否明显缩短。
- 日志应看到后续轮次：`prepare round: skip clean UI round=... reason=phase-fallbacks-own-cleanup`。

### 谢帅 - 2026-06-12 五倍显形镜目标点击空 hint 异常

Status: implemented / compile passed

Context:

- 用户反馈最新五倍又直接任务异常。
- 最新异常发生在 `2026-06-12 19:02:09`，窗口 `hwnd-161B42 / 『忍者』影`，phase 为 `wubei:RESOLVE_AFTER_PATHING`。
- 日志链路：
  - `resolve probe after runner pathing ... hint=null`
  - 使用显形镜成功，随后 `wubei_probe_story_koukou.png` 命中，说明目标已出现。
  - 进入 `tryClickProbeSpawnedTarget(...) -> tryClickTrackerCombatTargetSmart(...)`。
  - `tryClickTrackerCombatTargetSmart(...)` 直接读取 `currentTrackerDestinationHint.mapName()`，因为 hint 为空抛出 NPE。

Changed:

- `WubeiTask.tryClickTrackerCombatTargetSmart(...)`
  - 不再直接对 `currentTrackerDestinationHint.mapName()` 解引用。
  - hint 存在时继续使用 hint 的 map/x/y。
  - hint 缺失时给 `NpcClickRequest` 传 `mapName=""`、`mapX=0`、`mapY=0`，让后续点击策略自己判断哪些路径可用。
- `WubeiTask.tryDirectCombatFromTrackerHint(...)`
  - 同样使用空 map fallback，避免空 hint NPE。

Rationale:

- 五倍显形镜/黄袍连战里，hint 只能作为目的地辅助验证，不能决定“是否允许进战斗”。
- 当 story 或左侧任务文本已经证明战斗目标出现时，缺失浮框 OCR 不应该让任务异常退出。
- 不用 runner snapshot 冒充任务怪坐标；这里只做空值防护。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮实测关注 `try smart combat target click ... requestMap=(0, 0)` 是否出现。
- 如果出现后点怪仍慢或失败，再查 hint 截图/OCR/采样时机，而不是在这里加新的定位兜底。

Update:

- 用户进一步要求未知坐标不要用 `0,0`，改为 `-1,-1` 表示未知。
- `WubeiTask`
  - 缺 `currentTrackerDestinationHint` 时构造 `NpcClickRequest` 使用 `mapName=""`、`mapX=-1`、`mapY=-1`。
- `NpcClickService`
  - 增加统一保护：`mapX/mapY < 0` 视为未知目标坐标。
  - 未知目标坐标时跳过 learned-memory 策略。
  - 未知目标坐标时跳过 player-anchor formula 策略。
  - 未知目标坐标时不写 smart-click evidence / vision memory，避免写入 `npc-click||白龙马|(-1,-1)` 这类脏 key。
  - tooltip / 黄字 / Ctrl 菜单等视觉路径仍可继续尝试。
- `WubeiTask.runRoundPhases(...)`
  - 单个 phase 抛 `RuntimeException` 时，不再直接让整个五倍任务异常退出。
  - 现在会记录 `[wubei] phase exception; recover current round`，然后走现有 `recoverRoundAfterFailure(...)` 回到接任务恢复链。
  - JVM `Error` / 类加载 / 内存这类 fatal 问题不吞，仍保留硬失败。
- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-12 五倍黄袍怪连战绿字不再注册寻路

Status: implemented / compile passed

Context:

- 用户在 18:17 附近观察到黄袍怪第二次/连续战斗时，绿字已经点了，但进入战斗对话框处理明显慢。
- 日志链路确认不是“截图太早错过 dialog”：
  - `18:17:05.691` 已点击 `chained-combat-1` 绿字。
  - 随后旧逻辑把它当成一次 `tracker pathing`，进入 `WAIT_BATTLE_FINISH`。
  - 到 `18:17:13` 左右才回到 `ENTER_BATTLE` resolver，中间被固定等待拖慢约 6 秒。
- 黄袍怪连战的绿字不是远距离寻路，怪就在附近，点完后应该短等并马上处理进战斗 dialog。

Changed:

- `WubeiTask.clickTaskTrackerGreen(...)`
  - `chained-combat-*` label 不再调用 `gameStateUtil.recordMovementIntent(...)`。
  - `chained-combat-*` label 不再注册 runner pathing intent。
  - `chained-combat-*` label 继续跳过 destination hint capture。
  - 新增日志：`tracker pathing intent skipped: label=... reason=chained-combat-continuation`。
- `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)`
  - 黄袍怪战后左侧任务追踪仍显示黄袍怪时，点击下一次绿字后只短等 `450ms`。
  - 下一阶段从旧的 `WAIT_BATTLE_FINISH` 改为直接 `ENTER_BATTLE`，让 `tryClickKnownEnterBattleDialog(...)` 尽快接手。

Verify:

- `mvn -q -DskipTests compile` passed。

Next test:

- 下一轮黄袍怪连战看日志是否出现：
  - `tracker pathing intent skipped: label=chained-combat-...`
  - `chained combat target continues: ... nextState=ENTER_BATTLE`
  - 绿字点击后约 1 秒内进入 `wubei:enter-battle` 处理，而不是再等 6 秒。

### 谢帅 - 2026-06-12 五倍接任务 option 记忆点快路径

Status: implemented / compile passed

Context:

- 用户查看 17:59:45 附近五倍接任务日志时发现：NPC dialog 已经弹出后，记忆点点击仍明显等了几秒。
- 日志拆解确认：
  - `NpcClickService` 已通过 `VERIFY_EXPECTED_DIALOG` 确认接任务 OPTION dialog 可见。
  - 随后的 `tryRememberedAcceptOption(...)` 又走了一次 `CLICK_REMEMBERED_OPTION` 的完整 `detectDialogTypeNoFocus`，该次耗时约 1727ms。
  - 真正 `dialog:rememberedOption:wubei.acceptTask` 输入队列点击只耗时约 341ms。

Changed:

- `DialogHandleRequest`
  - 保持唯一的 `handleRememberedChoiceOption(...)` 工厂函数，不再新增重载。
  - 五倍这个快路径直接通过 request builder 设置 `verifyDialogType=false`，避免为了一个开关多暴露一个入口。
- `DialogService.handleDialog(...)`
  - 当 `CLICK_REMEMBERED_POINT` 且 `verifyDialogType=false` 时，使用默认大 dialog 矩形直接执行记忆点点击。
  - 新日志：`dialog remembered option fast path without detect`。
  - 其他普通记忆点、route dialog 记忆点仍保持原有验证。
- `WubeiTask.tryRememberedAcceptOption(...)`
  - 五倍接任务 option 记忆点改为 `verifyDialogType=false`，避免重复验证。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮五倍实测看 `normal-round-start:memory` 附近是否出现：
  - `dialog remembered option fast path without detect`
  - 不再出现第二次 `reason=handle-dialog:CLICK_REMEMBERED_OPTION` 的 `dialog.detect` 1727ms 级耗时。

### 谢帅 - 2026-06-12 五倍追踪绿字点击后 hint 改为后台解析

Status: implemented / compile passed

Context:

- 16:29:20-16:29:35 日志显示，队长点击左侧任务追踪绿字后已经注册移动 intent：
  - `16:29:27.932 window pathing intent registered for tracker click`
  - 但直到 `16:29:32.935` 才释放任务权。
- 中间约 5 秒被 `destination hint capture` 三次采样和 OCR 阻塞：
  - sample1/2/3 同步等待。
  - 第一张 OCR 约 `ocrMs=3222`。
- 业务结论：hint 只是点击后的辅助信息；点击绿字并注册移动 intent 后应立即放权，hint 不应占住前台 turn。

Changed:

- `WubeiTask.clickTaskTrackerGreen(...)`
  - 点击成功后不再同步调用 `captureTrackerDestinationHint(...)`。
  - 改为 `scheduleTrackerDestinationHintCapture(...)` 后台执行。
- 后台任务会用当前 `WindowRuntimeContext` 临时绑定线程，保证截图/temp 路径仍然是窗口隔离的。
- 增加 `trackerDestinationHintRequestId`，旧后台 hint 结果如果晚到，会被识别为 stale 并忽略，避免覆盖新一轮任务 hint。
- `currentTrackerDestinationHint` 改为 `volatile`，允许后台写入、任务线程读取。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮五倍实测重点看：
  - 绿字点击后应很快出现 `task.turn.release ... result=PATHING_STARTED`。
  - `destination hint capture scheduled async` 应出现在释放前后，但不应再拖住 `TRACKER_PATHING` transaction 5 秒。
  - 后台若成功解析，应看到 `destination hint stored async`。

Follow-up:

- 下一轮五倍测试重点看：每次 `ACCEPT_TASK -> READ_TRACKER` 后都应重新出现 `TaskTrackerPanelService panel read`。
- 热启动会先读一次左侧面板，进入 `READ_TRACKER` 后还会再读一次；这是为了换取缓存安全，后续如果要优化性能，可以给热启动快照加明确版本/时间戳再复用。
- 如果第二次仍被取消，要看新的 `TaskTrackerPanelService panel read` 读出的 `yellow='...'` 是什么；不能再只凭旧的 `殿前献艺|3只|暗雷怪` 缓存取消。

### 谢帅 - 2026-06-12 Dialog 白字 story 模板参数升级为列表

Status: implemented / compile passed

Context:

- 13:05 左右五倍白龙马 probe 失败复盘发现，`WubeiTask` 连续调用两次 `handleDialog(VERIFY_WHITE_TEMPLATE)`：
  - 一次查 `probe target-ready` 白字模板。
  - 一次查 `probe wrong-position` 白字模板。
- 每次 `handleDialog` 都会重新 `Alt+4`、截图、洗白字和匹配，后续还有 `NpcClickService` 的 story cleanup，导致失败 fallback 时间被放大。

Changed:

- `DialogHandleRequest`
  - 把白字模板参数从单个 `expectedTemplateActionKey/expectedTemplatePath` 升级为 `List<WhiteTemplateSpec>`。
  - 保留原来的 `verifyWhiteTemplate(source, actionKey, templatePath)` 工厂方法，内部包装成单元素 list，旧调用点不用改。
- `DialogService.verifyWhiteStoryTemplate(...)`
  - 保持原来的 `VERIFY_WHITE_TEMPLATE` 入口不变。
  - 一次 dialog snapshot、一次洗白字后，遍历 `whiteTemplateSpecs` 匹配，命中后返回对应 `actionKey/templatePath`。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一步再改 `WubeiTask`，把 `probe target-ready` 和 `probe wrong-position` 两次白字检查合并成一次 `verifyWhiteTemplates(...)` 请求。
- 之后再决定是否让五倍 probe 调 `NpcClickService` 时跳过重复 story cleanup。

Update:

- 已把 `WubeiTask` 白龙马显形镜后的两个 story 检查合并：
  - 主流程仍保留两个 if：先看 `probe target-ready`，再看 `probe wrong-position`。
  - 底层只调用一次 `inspectProbeStoryOnce(...)` / `handleDialog(VERIFY_WHITE_TEMPLATE)`。
  - `DialogService` 在同一张 dialog snapshot、同一张白字 washed 图上匹配两个 `WhiteTemplateSpec`。
- `handleDialog(...)` 默认不再按 `Alt+4`；`Alt+4` 只保留给 `detectDialogTypeNoFocus(reason)` 这类“主动探测是否有 dialog”的入口。
- 白字 story 模板检查、路线弹窗、任务/维护等业务 `handleDialog(...)` 不再默认占用输入队列去隐藏玩家名字。
- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-12 五倍黄袍链战后 title 消失视为完成

Status: implemented / compile passed

Context:

- 15:19 左右五倍黄袍怪连续战斗结束后，`RETURN_HOME` 阶段读取左侧任务追踪：
  - 面板区域能截到，但五倍 title 模板全部 `not matched`。
  - 旧逻辑把 `postCombatPanel.isFound()==false` 当成 `chained combat tracker unreadable`，进入失败恢复。
  - 失败恢复又把状态送回 `ROUTE_TO_MAIN_TASK`，表现为没有用回程道具，而是直接跑回接任务 NPC。
- 用户确认业务语义：该运行态已经记住本轮是在打黄袍怪链；战后左侧五倍 title 消失，说明黄袍链结束，应正常使用回程道具，不是任务失败，也不是热启动未知状态。

Changed:

- `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)`
  - 在 `currentRoundChainedCombatExpected` 的战后分支里，`postCombatPanel.isFound()==false` 改为“黄袍链完成”。
  - 清空 `currentRoundChainedCombatContinueCount`。
  - 调用 `useReturnItemAndVerifyStartMap(context, "chained-combat-title-gone")`。
  - 成功后进入 `WAIT_TEAM_RETURN`，不再走 generic accept-task recovery。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮黄袍怪实测重点看：
  - `chained combat tracker title gone after battle; treat as completed`
  - 随后是否出现 `use return item and verify start map: source=chained-combat-title-gone`
  - 不应再因为战后 title 消失进入 `recover current round from accept task`。

### 谢帅 - 2026-06-12 五倍接任务 NPC probing 增加已有 dialog gate

Status: implemented / compile passed

Context:

- 15:53:45 到 15:54:28 暂停前，队长在五倍接任务恢复链路里表现为“站着不动、鼠标乱探测”。
- 日志链路：
  - `READ_TRACKER` 连续 5 次没有命中五倍 title，进入 `recover current round from accept task`。
  - 导航认为队长已在 `宝象国(92,90)`，接任务 NPC 目标是 `(86,87)`，所以没有移动。
  - `NpcClickService` tooltip 没找到，使用 learned-memory 点 `(532,634)`。
  - 点完后 `DialogService` 已检测到 `OPTION`，但 expected template 没命中，于是继续黄字 OCR / Ctrl probe。
  - 后续 Ctrl probe 用 learned point 和黄字噪点候选 `(365,634)/(599,797)/(667,970)...` 反复 `holdCtrl -> moveMouse -> releaseCtrl`，形成“乱点”现象。

Changed:

- `NpcClickService.clickNpcSmart(...)`
  - 在 learned-memory 点击前加已有 dialog gate。
  - 在 learned-memory 点击失败后再加一次已有 dialog gate，拦住“已弹出 dialog 但模板没验证上”的情况。
  - 在 Ctrl probe 前加已有 dialog gate。
- gate 使用 `dialogService.detectDialogTypeNoFocus(...)`。如果发现 `OPTION/STORY`，直接停止后续 NPC probing，并让调用方继续走自己的 `DialogService` 业务处理。

Findings:

- 本次 `READ_TRACKER` 失败不是 OCR 算错；保存图 `images/temp/hwnd-161B42/task_tracker_detail_wubei-attempt-5.png` 显示左侧内容是 `[引导] 手揪枯荣[社树之心]`，不在当前五倍 title 模板集合内。
- 当前五倍 title 模板只有：`三藏封魔`、`宝象谜情`、`殿前献艺`、`智斗黄袍`、`魁星归位`。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮五倍接任务实测重点看：
  - 出现已有 dialog 时，应看到 `NPC smart click skips further probing because dialog is already open`。
  - 不应继续进入 `npcClick:ctrlMenuScan:降魔侍卫`。
  - 如果 READ_TRACKER 仍失败，需要确认 `[引导] 手揪枯荣[社树之心]` 是正常五倍分支 title、临时引导任务，还是任务追踪面板截错/残留。

Update:

- 复盘用户截图后，确认还有一个时序风险：接任务选项点击成功后，左侧任务追踪不是瞬间刷新；旧代码马上进入 `READ_TRACKER`，5 次 title retry 之间也没有等待，容易连续读到接任务前/旧任务追踪内容。
- `WubeiTask.runAcceptTaskPhase(...)`
  - 接任务成功后先清 `currentTrackerPanel`。
  - 新增明确日志：`accept task clicked; waiting tracker refresh before READ_TRACKER`。
  - 等待 `TRACKER_REFRESH_AFTER_ACCEPT_MS=1000ms` 后再进入 `READ_TRACKER`。
- `WubeiTask.resolveTrackerPanelWithAnchorRecovery(...)`
  - 每次 title miss 后，下一次 retry 前等待 `350ms`，避免 5 次截图都发生在同一个左侧面板刷新窗口里。
- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-12 队员三技能维护未触发原因与修复

Status: implemented / compile passed

Context:

- 用户观察到五倍里队长已经做过三技能维护，但四个队员一次都没做。
- 日志里队员窗口只有 `auto-battle` 的补给 / broadcast dialog / 自动战斗面板维护，没有任何 `summon skill due` 或 `start summon skill clean`。
- 代码确认原因在 `AutoBattleTask.maybeRunIdleMaintenance(...)`：队员作为主任务 follower-support 时，旧逻辑使用 `cleanSummonSkill(!followerSupportMode)`，等于明确禁止队员三技能维护。

Changed:

- `AutoBattleTask.maybeRunIdleMaintenance(...)`
  - follower-support 队员不再禁用三技能维护。
  - 队员三技能维护接入 `oneSummonSkillPerTeamRound`，使用当前 `requestedTaskCode` 作为 teamMaintenanceKey。
  - 保持每轮最多一个窗口执行三技能维护，避免五个窗口同一轮全部抢着做。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮实测看队员日志是否出现：
  - `maintenance: summon skill due source=auto-battle`
  - `maintenance: summon skill round claimed teamRound=wubei#... windowKey=... source=auto-battle`
  - `maintenance: start summon skill clean source=auto-battle`
- 如果仍然只有队长做，下一步要检查队长 `leader-pathing` 是否总是先抢到 `wubei#round` claim，需要做简单轮转或队员优先策略。

### 谢帅 - 2026-06-12 五倍维护阶段拆分与修装备导航等待

Status: implemented / compile passed

Context:

- 用户在 17:19:50 附近观察到五倍触发修装备维护后，队长已经导航到修装备 NPC 附近，但没有继续点 NPC 触发修装备 broadcast。
- 日志链路显示五倍在 `TRACKER_PATHING` phase 里同时检查 `heal-pet` 和 `repair-equipment`：
  - `repair-equipment` 的 `navigateToNPC(...)` 返回 `PATHING_STARTED` 后，下一轮又重新进入同一个 `TRACKER_PATHING`。
  - 由于五倍没有像修罗一样等待“维护 NPC 导航结束”，后续会重新跑维护判断，甚至串到另一个维护 hook。
- 修罗 V2 已经把维护拆成两个阶段：
  - `AFTER_ACCEPT_MAINTENANCE_CHECK`：接任务/读目标后处理医宝宝。
  - `BEFORE_ROUTE_MAINTENANCE_CHECK`：正式去目标前处理修装备。

Changed:

- `WubeiPhase`
  - 新增 `AFTER_ACCEPT_MAINTENANCE_CHECK`。
  - 新增 `BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK`。
- `WubeiRoundContext`
  - 新增 `waitingPathing`，用于记录当前 phase 已经提交了维护 NPC 导航，下一轮应先等待导航稳定。
  - 新增 `waitForPathing(...)` / `clearPathingWait(...)`。
- `WubeiTask`
  - `READ_TRACKER` 成功后不再直接进 `TRACKER_PATHING`，而是进入 `AFTER_ACCEPT_MAINTENANCE_CHECK`。
  - `AFTER_ACCEPT_MAINTENANCE_CHECK` 只处理医宝宝，到下一段维护。
  - `BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK` 只处理修装备，到真正 `TRACKER_PATHING`。
  - `TRACKER_PATHING` 不再绑定医宝宝/修装备，避免维护失败后串阶段绕路。
  - 维护 NPC 导航返回 `PATHING_STARTED` 后，保留当前维护 phase 并设置 `waitingPathing`。
  - 下一轮同一维护 phase 会先调用 `GameStateUtil.detectMovementState()`，仍在移动则继续放权等待；停止后才继续点维护 NPC。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮五倍实测重点看：
  - 修装备 hook 出现 `repair-equipment maintenance navigation still pathing` 后，不应立刻串到医宝宝或主追踪绿字。
  - 到达李道宗附近后，应继续 `NPC smart-click` 并触发修装备 broadcast。
  - 医宝宝和修装备应该出现在不同 phase 日志里，顺序清晰。

Update:

- 用户进一步明确五倍维护链的业务顺序：
  - 医宝宝和修装备都属于“接完任务以后、打怪以前”的维护。
  - 顺序必须是：先医宝宝，再修装备，最后去打怪。
  - 修装备完成后，不管成功还是失败，本轮都不能回头再医宝宝或再次修装备。
  - 失败的维护留到下一轮再尝试；如果连续多轮失败，需要停止反复绕路。
- `WubeiTask`
  - 保持 `READ_TRACKER -> AFTER_ACCEPT_MAINTENANCE_CHECK -> BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK -> TRACKER_PATHING` 单向链。
  - 增加 `MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES=3`。
  - 医宝宝/修装备成功后分别重置自己的连续失败计数。
  - cooldown 未到的跳过不算失败。
  - 真正尝试完 `MAX_MAINTENANCE_HOOK_ATTEMPTS` 仍失败才累计一次失败。
  - 连续失败达到上限后，后续轮次直接跳过该维护，避免每轮都绕去失败的维护 NPC。
- `mvn -q -DskipTests compile` passed。

### 谢帅 - 2026-06-13 五环买鞋入口改为 watcher 门口处理

Status: implemented / compile passed

Context:

- 用户要求导航/dialog 不再保留两套老分支，五环买鞋也要依赖 watcher / preparation 模型。
- 12:45-12:48 的 `hwnd-1C50FA4` 日志显示，窗口已经在长安 130,129 附近，但买鞋入口阶段反复得到 `NavigationResult.ARRIVED`，一直没有触发 `shoe-shop-door-first-dismount` / `Alt+C` 下坐骑。
- 根因是 `FiveRingTaskV2.buyShoes(...)` 的直接导航分支把 `ARRIVED/SUCCESS` 当成“本前台 turn 内完成”，没有注册 watcher intent，所以后续不会进入统一的 `handleShoeShopDoorAfterArrival(...)` 门口/下坐骑流程。

Changed:

- `FiveRingTaskV2.buyShoes(...)`
  - `clickShoeShopEntryExact(...)` 返回 `ARRIVED/SUCCESS` 时，不再直接 `continueTo(...shoe-shop-entry-clicked-success...)`。
  - 改为注册 `WindowPathingIntent`：`source=SHOE_SHOP_ENTRY_NAV_SOURCE`，目标 `长安(130,130)`，`tolerance=5`。
  - 随后返回 `PATHING_STARTED` 并放权，下一轮通过 watcher snapshot 统一进入 `handleShoeShopDoorAfterArrival(...)`。
  - `DIALOG_OPENED` 不再被当作入口成功，改为重试并交回 watcher / prepared flow。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮买鞋实测看日志是否出现：
  - `entry navigation returned ARRIVED; registered watcher intent instead of completing in foreground`
  - 下一轮 watcher snapshot 后出现 `arrived at entry door, wait for auto-enter before dismount`
  - 如果未自动进店，应出现 `shoe-shop-door-first-dismount`。
- 不应再出现 `shoe-shop-entry-clicked-success` 连续 retry 到 phase loop guard 的情况。

### 唐德 - 2026-06-13 Dialog STORY 白字行特征防误判

Status: implemented / compile passed / testcase replay passed

Context:

- 五环 `ID=451753529 / 忆叶知秋` 在接任务前调用 `NpcClickService.clickNpcSmart(...)`。
- `NpcClickService` 在真正点击云游大师前先调用 `DialogService.detectDialogTypeNoFocus(...)`。
- 日志显示 `dialog story upper check` 因 `thinWhite=203 green=0 total=203` 被判为 `STORY`，随后 NPC probing 被短路为成功。
- 但保存的 `story_upper_raw` 实际是游戏场景/人物/坐骑区域，不是真实 dialog；单纯白点数量阈值太宽。

Changed:

- `ImagePreprocessor`
  - 新增 `detectThinWhiteTextLinePattern(BufferedImage)`。
  - 检查 story 上半区是否存在横向白字文本行：每行白点数量、白点簇数量、横向跨度，避免散落场景高光/人物边缘误判。
- `DialogService.hasStoryInUpperHalf(...)`
  - 保留原来的 `thinWhite + green > 200` 基础阈值。
  - 新增 `textLineStats.matched()` 作为 STORY 必要条件。
  - 日志增加 `textRows/maxRowWhite/maxClusters/maxSpan`，方便后续看误判来源。
- `DialogStoryDetectionReplayDebug`
  - 新增 repo-local replay 工具，读取 `images/test-cases/dialog/story-detection/raw`，输出带红色文本行标记的图到 `output`。

Testcases:

- 负例：`images/test-cases/dialog/story-detection/raw/false_scene_player_names_story_upper.png`
  - 来源：本次 `npc-click:before-learned-memory` 误判图。
  - Replay 结果：`story=false rows=0 maxWhite=15 clusters=5 span=30`。
  - 输出：`images/test-cases/dialog/story-detection/output/false_scene_player_names_story_upper_story_replay.png`
- 正例：`images/test-cases/dialog/story-detection/raw/true_story_white_template_story_upper.png`
  - 来源：真实 story 白字上半区。
  - Replay 结果：`story=true rows=14 maxWhite=161 clusters=75 span=498`。
  - 输出：`images/test-cases/dialog/story-detection/output/true_story_white_template_story_upper_story_replay.png`

Verify:

- `mvn -q -DskipTests compile` passed。
- `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.DialogStoryDetectionReplayDebug"` passed。

Follow-up:

- 这只修 `STORY` 误判来源。
- 后续仍需要整理 `NpcClickService.isDialogOpenBeforeNpcProbe(...)` 的业务边界：不能因为任意 `STORY/OPTION` 就把 NPC 点击判为成功；有 expected template 时，至少要验证模板或清理后重试。

### 何黎 - 2026-06-13 窗口 watcher 软 push 事件骨架

Status: implemented / compile pending

Context:

- 用户指出当前多窗口调度太像“轮询”：窗口 watcher 已经知道某个窗口 pathing 到达/停住，但任务线程往往要等到下一轮拿权后才重新读取 snapshot。
- 目标不是让 watcher 执行业务，而是让 watcher 能把“这个窗口有新状态了”推给调度/任务等待处，后续再逐步把五环/五倍/修罗的等待逻辑接上。

Decision:

- 先做 soft push，不做 hard callback。
- watcher 只发布事件，不点击、不 focus、不推进任务 phase。
- `WindowRuntimeContext` / `WindowPathingSnapshot` 仍然是事实来源；消费方收到事件后必须重新读取 runtime snapshot 再行动。
- 第一版只发布 `PATHING_TERMINAL`，表示当前 active pathing intent 已经到达 `ARRIVED` 或停在半路 `STOPPED_AWAY`。
- 事件按 `windowId + type` 合并，只保留最新事件，避免每秒刷屏。

Changed:

- 新增 `WindowReadyEventType` / `WindowReadyEvent`。
- 新增 `WindowReadyEventBus`：
  - `publish(...)`：记录最新事件并 `notifyAll`。
  - `awaitNewer(...)`：后续任务等待处可按 `windowId + type + sequence` 早醒。
  - `latest(...)`：查看最近事件。
- `WindowTaskRunner.refreshPathingSignal(...)` 在 pathing state 首次变成 `ARRIVED` 或 `STOPPED_AWAY` 时发布 `PATHING_TERMINAL`。
- `MultiWindowTaskManager` 将 Spring 注入的 `WindowReadyEventBus` 传给每个 `WindowTaskRunner`。

Not yet wired:

- 五环/五倍/修罗还没有消费这个事件。
- 下一步可以先接一个低风险等待点：例如任务自己的 `PATHING_STARTED` 让权等待处，用 `awaitNewer(... PATHING_TERMINAL ...)` 代替固定 sleep 或缩短下一轮轮询延迟。

### 唐德 - 2026-06-13 Dialog no-focus 检测等待参数化

Status: implemented / compile passed

Context:

- 用户指出 `DialogService.detectDialogTypeNoFocus(...)` 内部固定 `700~800ms` sleep 不合理。
- “点击/按键之后等待 dialog 出现”需要等待；但“纯粹检查当前屏幕有没有 dialog”不应先睡。

Changed:

- `DialogService`
  - 新增 `detectDialogTypeNoFocus(String reason, boolean hidePlayerNames, long waitBeforeCaptureMs)`。
  - 新增内部 `detectDialogSnapshotDirect(String reason, boolean hidePlayerNames, long waitBeforeCaptureMs)`。
  - 旧重载保留默认 `700 + random(100)` 行为，避免未审计调用行为突变。
- 改成 `0ms` 的纯当前屏幕探测：
  - `NpcClickService` 的 `before-learned-memory` dialog precheck。
  - `NpcClickService` 的 `after-tooltip` dialog precheck。
  - `NavigationService` 的 pathing-active dialog rescue。
  - `WindowTaskRunner` 的 route dialog preparation probe。
  - `DialogService.prepareRouteOption(...)` / `prepareRememberedRouteOption(...)`。
  - `DialogService.captureCurrentStoryImage(...)`。
- 暂时保留默认等待的路径：
  - `handleDialog(...)` 主分类。
  - `green-template-click` 类型验证。
  - 鞋店 OCR fallback 等广泛业务路径。
  - 原因：这些入口有不少接在点击/按键之后，先不把行为一次性改大。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 继续整理 `NpcClickService` 的 true/false 语义：服务只负责点击是否成功，业务层负责 cleanup/retry。
- 如果后续确认某个 `handleDialog(...)` 调用只是当前屏幕读取，再单独改成 `0ms`，不要全局一刀切。

### 谢帅 - 2026-06-13 五环结束 story 阈值调整

Status: implemented / replay verified

Context:

- 行部/刑部窗口 `hwnd-FF06CC` 在结束五环后显示异常。
- 日志显示代码确实执行了结束 story 检查：
  - `source=wuhuan-v2:tracker-miss-finished-story:finished`
  - `status=WHITE_TEMPLATE_NOT_FOUND`
- 保存下来的 story 截图实际是“恭喜你完成了一次五环任务...”，说明失败不是没走结束逻辑，而是旧模板未过阈值。
- 用户确认第一次/第二次完成五环的 story 文案不同，因此不能换成只覆盖当前文案的“了一次五”模板，应继续使用原“恭喜你完”模板。

Changed:

- `images/template/dialog/wuhuan/wuhuan_task_finished_story.png`
  - 已恢复为原来的“恭喜你完”模板。
- `DialogService`
  - 仅把白字 story 模板匹配阈值抽为 `WHITE_STORY_TEMPLATE_THRESHOLD=0.80`。
  - 绿字 option/template 匹配阈值仍保持 `0.85`。
  - 原因：旧模板在失败图上的最佳分是 `0.822888`，0.85 不过，0.80 可过。

Replay testcase:

- Input raw:
  - `images/test-cases/dialog/wuhuan/finished_story_20260613_story_upper_raw.png`
- Input washed:
  - `images/test-cases/dialog/wuhuan/finished_story_20260613_story_upper_white.png`
- Marked output:
  - `images/test-cases/dialog/wuhuan/finished_story_20260613_marked.png`
- Command:
  - `jshell --class-path target/classes;<maven dependency classpath>` 调用 `ImageFinder.find(...)`。
- Result:
  - old template full story at 0.80: `[48.5, 56.0, 0.822888195514679]`
  - old template upper story at 0.80: `[48.5, 23.0, 0.8228883147239685]`
  - old template upper story at 0.85: `null`

Follow-up:

- 下一轮五环结束时看是否出现：
  - `[five-ring-v2 finish] completion story visible`
  - `task finished: 五环V2 -> SUCCESS`

### 何黎 / Rawls - 2026-06-14 Window ready soft wake 反方审查收敛

Status: implemented / compile passed

Context:

- 用户要求先找另一个智能体用反方角度审查“runner prepared 后要不要更快唤醒窗口”的方案。
- Rawls 的结论：
  - 现在不要做真正的 priority scheduler。
  - 不要改 `TaskTurnCoordinator` 的 fair lock。
  - 不要改 `InputActionQueue` / 输入串行化策略。
  - ready event 只能作为“软唤醒”，不能直接赋予点击权。
  - 任务拿到唤醒后仍必须重新读取 `WindowRuntimeContext` / prepared action，确认 windowId/hwnd/operation/target/age 后再执行。

Changed:

- `WindowReadyWaitService`
  - `waitForPathingWakeOrTimeout(...)` 增加 `EnumSet<WindowReadyEventType> wakeTypes`。
  - 调用方必须显式声明当前 phase 接受哪些 watcher event。
  - 只有调用方允许 `DIALOG_PREPARED` 或 `TASK_TRACKER_PREPARED` 时，才会因为已有 prepared action 提前醒。
  - 纯 `PATHING_TERMINAL` 等待不会被 prepared action 误唤醒。
  - 消费日志增加 `eventAgeMs`，已有 prepared action 早醒日志增加 `preparedAgeMs` / `verifiedAgeMs`。
- `WindowReadyEventBus`
  - 发布日志增加 `eventAgeMs`，用于判断 watcher 发布的是新鲜信号还是旧 snapshot。
- `WindowRuntimeContext`
  - `clearPreparedDialogAction(...)` 增加清理日志，包含 reason、operation、target、prepared/verified age。
- `FiveRingTaskV2`
  - pathing handoff 等待：
    - `PATHING_TERMINAL`
    - `DIALOG_PREPARED`
    - `TASK_TRACKER_PREPARED`
  - 原因：五环左侧任务追踪 prepared action 是有效推进信号。
- `WubeiTask` / `XiuluoTaskV2`
  - pathing handoff 等待：
    - `PATHING_TERMINAL`
    - `DIALOG_PREPARED`
  - 原因：五倍/修罗当前不应被五环式 task tracker prepared action 唤醒。

Verify:

- `rg -n "waitForPathingWakeOrTimeout|waitForPathingTerminalOrTimeout" src/main/java`
  - 只剩三个任务调用点和 `WindowReadyWaitService` 定义。
- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮实测重点看：
  - `window.ready.consume` 是否能减少“准备好的 dialog/tracker 等下一轮轮询”的延迟。
  - 是否出现误唤醒后拿权但无事可做；若有，先补 event age/drop reason 日志，不要立刻改调度锁。
  - 只有日志证明 fair lock 顺序本身成为瓶颈时，再讨论 coordinator hint/priority。

### 谢帅 - 2026-06-14 Route dialog 传送后同轮接当前地图导航

Status: implemented / compile passed

Context:

- 用户指出：route dialog 传送选项和 world-map 绿色路线链接不是同一种动作。
- 如果已经点了“长安 -> 洛阳”这种 route dialog 选项，目标就是当前导航目标地图；传送完成后角色通常处于停住状态。
- 旧逻辑在 route dialog 点成功后直接返回 `PATHING_STARTED`，修罗任务层会 `waitForPathing(...)` 放权，导致到达目标地图后还要下一轮才继续 `navigateInCurrentMap(...)`，中间出现明显延迟。

Changed:

- `NavigationService.navigateToMap(...)`
  - prepared route dialog 优先消费路径点成功后，不再默认直接返回 `PATHING_STARTED`。
  - 普通 prepared route dialog 消费路径点成功后，也不再默认直接返回 `PATHING_STARTED`。
  - 两处都先调用 `gameStateUtil.confirmCurrentMapFresh(targetMapName, ROUTE_DIALOG_ARRIVAL_CONFIRM_TIMEOUT_MS, ...)`。
  - 如果确认已到目标地图：
    - 调用 `closeMapSearchInputAfterRouteDialog(...)` 做轻量收尾；
    - 返回 `NavigationResult.arrived(...)`；
    - `navigateToNPC(...)` 会在同一轮继续进入 `navigateInCurrentMap(...)`。
  - 如果确认不到目标地图：
    - 保留原有 `PATHING_STARTED` 行为，让 watcher/pathing 兜底。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 实测修罗“长安 route dialog -> 洛阳 -> 洛阳当前地图坐标”时，重点看：
  - route dialog 点成功后是否出现 `target map confirmed`；
  - 是否不再在洛阳落地后短暂放权；
  - 如果确认超时，是否仍能走原来的 watcher/pathing 兜底而不死锁。

### 谢帅 - 2026-06-14 Runner visible dialog snapshot 接线

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 1 分工执行。
- 本阶段只做 Runner 看到弹窗后的 runtime 可观测状态，不改导航 gating、不迁移 route preparation、不改五环/五倍/修罗业务行为。

Changed:

- `WindowRuntimeContext`
  - 已有 `WindowDialogSnapshot` / `visibleDialogSnapshot` 前置被确认存在。
  - 补齐重复检查后保留单一 `formatRect(...)`，确保 visible update/clear 日志可编译。
- `WindowTaskRunner.publishTaskAttentionIfDialogVisible(...)`
  - no-focus 检测到 `OPTION/STORY` 时，写入 `WindowRuntimeContext.updateVisibleDialogSnapshot(...)`。
  - snapshot 字段只包含窗口可见事实：`windowId`、`hwnd`、`type`、`source`、`detectedAtMs`、`captureProvider`，不包含 route target / task / business 字段。
  - 保留原有 `TASK_ATTENTION_REQUIRED` 发布行为。
  - 明确检测到 `DialogType.NONE` 时，调用 `clearVisibleDialogSnapshot("detected-none:...")`。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一轮实测只看日志，不判断业务行为：
  - `event=window.dialog.visible.update`
  - `event=window.dialog.visible.clear`
  - `event=window.ready.publish type=TASK_ATTENTION_REQUIRED`
- Phase 1 验收前不要继续做 Navigation gating / prepared action consumer。

### 唐德 - 2026-06-13 五环接任务 NPC 点击失败后的当前屏幕分流

Status: implemented / compile passed

Context:

- 用户要求捋清 `NpcClickService.clickNpcSmart(...)` 的 false 语义：
  - `NpcClickService` 只负责证明目标 NPC 是否点击成功。
  - 五环业务层不能把 `false` 直接等同于“屏幕上没有 dialog”。
  - 但也不能在 false 分支另写一套套娃 handler；应复用 `tryAcceptInitialTaskFromCurrentScreen(...)` 这个接任务入口。

Changed:

- `FiveRingTaskV2.tryAcceptInitialTaskFromCurrentScreen(...)`
  - `DialogType.NONE`
    - 直接返回 `null`，交给外层 NPC retry。
  - `DialogType.OPTION`
    - 先走原有接任务模板：
      - actionKey: `wuhuan.acceptTask`
      - template: `images/template/dialog/wuhuan/wuhuan_accept_first_option.png`
      - source: `wuhuan-v2:accept-dialog`
    - 如果接任务模板未命中，再验证“已有任务”模板：
      - template: `images/template/dialog/wuhuan/wuhuan_already_has_task_option.png`
      - source: `wuhuan-v2:current-screen-already-has-task`
    - 已有任务命中时清理当前 option，并进入 `SYNC_TASK_PANEL`。
    - 两个 option 都未命中时，清理意外 option 并返回 `null`。
  - `DialogType.STORY`
    - 匹配 daily limit story：
      - actionKey: `wuhuan.dailyLimit`
      - template: `images/template/dialog/wuhuan/wuhuan_daily_limit_story.png`
      - source: `wuhuan-v2:current-screen-accept-story:daily-limit`
    - 匹配 finished story：
      - actionKey: `wuhuan.finished`
      - template: `images/template/dialog/wuhuan/wuhuan_task_finished_story.png`
      - source: `wuhuan-v2:current-screen-accept-story:finished`
    - 任意一个 story 命中则点掉 story 并结束五环。
    - 都未命中则点掉/关闭当前 story，返回 `null` 让外层重试 NPC。
- `FiveRingTaskV2.acceptTask(...)`
  - `clickInitialNpcForAccept(...)` 返回 false 且当前屏幕接任务入口无法处理时，先执行一次轻量 UI cleanup，再重试 NPC。

Verify:

- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-14 Runner visible dialog snapshot Phase 1

Status: implemented / compile passed

Context:

- 根据 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 1 分工，先只让 Runner 看到的弹窗事实落到 `WindowRuntimeContext`。
- 本阶段不改 `NavigationService` gating，不迁移 route dialog preparation，不改五环/五倍/修罗业务逻辑。

Changed:

- 新增 `WindowDialogSnapshot`：
  - file: `src/main/java/com/bot/dhxy/window/model/WindowDialogSnapshot.java`
  - 字段只保留窗口可见事实：`windowId`、`hwnd`、`DialogType type`、`source`、`detectedAtMs`、`dialogRect`、`captureProvider`。
  - 没有加入 `targetKeyword` / `suspectedOperation` / 任务名等业务字段。
- `WindowRuntimeContext`：
  - 增加 `AtomicReference<WindowDialogSnapshot> visibleDialogSnapshot`。
  - 增加 `getVisibleDialogSnapshot()`、`updateVisibleDialogSnapshot(...)`、`clearVisibleDialogSnapshot(...)`。
  - update/clear 会写 `window.dialog.visible.update/clear` 日志，包含 windowId、hwnd、type/source、reason、age、rect、provider。
  - native binding 变化和 runtime reset 时会清理 visible snapshot。
- `WindowTaskRunner.publishTaskAttentionIfDialogVisible(...)`：
  - 沿用原来的探测频率和 `TASK_ATTENTION_REQUIRED` recent-skip。
  - 正常探测到 `OPTION/STORY` 时写入 runtime visible snapshot，并继续发布原有 soft wake event。
  - 正常探测到 `NONE` 时清理 runtime visible snapshot。
  - RuntimeException / 探测失败不会清理旧 snapshot。

Verify:

- `mvn -q -DskipTests compile` passed。

Follow-up:

- 下一步应实测包含路线 OPTION 的场景，确认日志里出现 `window.dialog.visible.update` 和 `window.dialog.visible.clear`。
- Phase 1 验证后，再讨论是否进入 `consumePreparedDialogAction(...)`、Navigation 只读 modal gating、Runner route prepare 迁移。

### 唐德 - 2026-06-14 Runner visible dialog Phase 1 返工收窄

Status: implemented / compile passed

Context:

- 何黎验收指出 Phase 1 混入了 Phase 2/调度/任务等待改动，需要只保留 visible dialog state。

Changed:

- 保留：
  - `WindowDialogSnapshot`
  - `WindowRuntimeContext.visibleDialogSnapshot` 的 get/update/clear
  - `WindowTaskRunner.publishTaskAttentionIfDialogVisible(...)` 写 visible snapshot
  - `WindowReadyEventType.TASK_ATTENTION_REQUIRED`
- 撤出/确认不再存在：
  - `WindowReadyEvent.priority`
  - `WindowReadyEventBus` priority / `latestFreshHigherPriority(...)`
  - `TaskTurnCoordinator` priority gate
  - `WindowReadyWaitService`
  - 五环/五倍/修罗 `waitForPathingWakeOrTimeout(...)` 接入
  - `DIALOG_PREPARED` / `TASK_TRACKER_PREPARED`
  - `WindowTaskRunner.publishPreparedActionEvent(...)`
- `DialogType.NONE` 当前只打 `visible dialog probe none` debug 日志，不清旧 snapshot。
  - 原因：当前 Runner 只拿到 `DialogType`，没有 capture success / imagePresent 结果；为避免截图失败误清，Phase 1 先不做 NONE clear。
- 把 `WINDOW_DIALOG_PREPARED_RECENT_MS` 改名为 `WINDOW_DIALOG_ATTENTION_RECENT_MS`，避免和 prepared action wake 混淆。

Verify:

- `rg -n "WindowReadyWaitService|waitForPathingWakeOrTimeout|TASK_TRACKER_PREPARED|DIALOG_PREPARED|publishPreparedActionEvent|latestFreshHigherPriority|latestFreshForWindow|currentReadyPriority|priority gate|higherPriority|READY_PRIORITY|WINDOW_DIALOG_PREPARED_RECENT_MS" src/main/java/com/bot/dhxy/window src/main/java/com/bot/dhxy/task`
  - no matches。
- `mvn -q -DskipTests compile` passed。

Follow-up:

- Phase 1 实测时重点看 `window.dialog.visible.update` 和 `event=window.ready.publish type=TASK_ATTENTION_REQUIRED`。
- 如果需要安全 clear，下一步应让 `DialogService` 暴露包含 capture success/imagePresent 的轻量 detection result，再清 `NONE`。

### 谢帅 - 2026-06-14 Runner visible dialog snapshot Phase 1 返工

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 最新验收意见收窄 Phase 1 边界。
- Phase 1 只允许保留窗口可见弹窗事实和 `TASK_ATTENTION_REQUIRED` 软唤醒；不允许混入 task 等待服务、prepared wake、priority scheduler。

Changed:

- 删除 `WindowReadyWaitService`。
- 移除 `FiveRingTaskV2` / `WubeiTask` / `XiuluoTaskV2` 对 `WindowReadyWaitService` 的注入和等待调用，恢复为普通 `TaskSleep.sleepOrStop(...)` handoff delay。
- `WindowReadyEventType` 只保留：
  - `PATHING_TERMINAL`
  - `TASK_ATTENTION_REQUIRED`
- `WindowReadyEvent` 移除 `priority` 字段。
- `WindowReadyEventBus` 移除 priority 计算、fresh priority 查询和 priority publish 日志。
- `TaskTurnCoordinator` 移除 ready-event priority gate，恢复 fair lock 基础交权逻辑。
- `WindowTaskRunner`：
  - 不再发布 `DIALOG_PREPARED` / `TASK_TRACKER_PREPARED` wake event。
  - `DialogType.NONE` 不再清理 `visibleDialogSnapshot`；只写 debug 日志，避免一次不可靠 no-focus 探测把旧可见事实抹掉。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮只验证 Phase 1 日志：
  - `window.dialog.visible.update`
  - `event=window.ready.publish ... type=TASK_ATTENTION_REQUIRED`
  - 不应再出现 `DIALOG_PREPARED` / `TASK_TRACKER_PREPARED` event。

### 唐德 - 2026-06-14 Phase 2 PreparedDialogAction 原子消费 API

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 2 唐德工作包执行。
- 本次只改 `WindowRuntimeContext`，不改 `NavigationService`、任务层、Runner route preparation、EventBus 或调度逻辑。

Changed:

- `WindowRuntimeContext`
  - 增加 `consumePreparedDialogAction(String reason)`。
    - 使用 `preparedDialogAction.getAndSet(null)` 原子拿走 action。
    - 消费后清理 `DialogPreparationStatus.READY`。
    - action 不存在时记录 `result=absent` 日志。
  - 增加 `consumePreparedDialogAction(DialogOperation expectedOperation, String expectedTargetKeyword, String reason)`。
    - 匹配 windowId / hwnd / operation / target。
    - 匹配失败只记录 `result=mismatch`，不清 action。
    - 匹配成功后用 `getAndSet(null)` 消费，并清理 READY 状态。
    - 如果 get 后发生竞态导致实际取出的 action 不匹配，会尝试在引用仍为空时恢复，并记录 `race-mismatch-restored`。
  - 保留 `clearPreparedDialogAction(...)` 作为 stale/reset/binding changed 的清理入口。
  - consume 日志包含 windowId、hwnd、reason、operation、target、source、expectedOperation、expectedTarget、preparedAgeMs、verifiedAgeMs、result。

Verify:

- `rg -n "consumePreparedDialogAction" src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
  - method found.
- `rg -n "WindowReadyWaitService|waitForPathingWakeOrTimeout|TASK_TRACKER_PREPARED|DIALOG_PREPARED|publishPreparedActionEvent|latestFreshHigherPriority|latestFreshForWindow|currentReadyPriority|priority gate|higherPriority|READY_PRIORITY" src/main/java/com/bot/dhxy/window src/main/java/com/bot/dhxy/task`
  - no matches.
- `mvn -q -DskipTests compile` passed。

Follow-up:

- 谢帅可以在 `NavigationService` 里把 route prepared action 的 get/clear 路径改成调用匹配版 consume API。

### 谢帅 - 2026-06-14 NavigationService route prepared action 消费与 world-map gate

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的谢帅工作包执行。
- 目标是让 `NavigationService` 不再用 `getPreparedDialogAction() -> click -> clearPreparedDialogAction(...)` 的非原子路径处理 route dialog。
- 本次不改 Runner route preparation，不改五环/五倍/修罗任务流程，不新增 OCR/template 识别到 `NavigationService`。

Changed:

- `NavigationService`
  - prepared route dialog 的优先点击路径改为调用 `runtime.consumePreparedDialogAction(DialogOperation.ROUTE_TRANSFER, targetMapName, reason)`。
  - 抽出 `consumePreparedRouteDialogAction(...)`，统一做：
    - 原子 consume；
    - window binding / verified age 校验；
    - 真实鼠标点击；
    - 成功后清理 dialog preparation request；
    - 点击失败时不复用旧 action。
  - 打开/重开世界地图前增加只读 gating：
    - fresh visible `OPTION` 且 active pathing intent 仍是同目标时，返回 `DIALOG_PREPARING`；
    - 同目标 `REQUESTED` / `PREPARING` 且未过期时，返回 `DIALOG_PREPARING`；
    - usable prepared action 会先被 consume/click，不再走旧 foreground OCR。
  - 增加 `route dialog world-map gate` 日志，包含 target、windowId、visible snapshot type/age/source、preparation phase/age、prepared target/age/usable、sameTargetIntent。
  - 将 `ROUTE_DIALOG_PREPARED_*` 常量重命名为 `ROUTE_PREPARED_DIALOG_*`，避免误中废弃 `DIALOG_PREPARED` event 验收搜索。
- `FiveRingTaskV2`
  - 仅把 `clearPreparedDialogAction("wuhuan tracker panel action consumed")` 的 reason 改为 `handled`，避免验收搜索把日志 reason 当成非原子 consume 残留；行为不变。

Verify:

- `rg -n 'clearPreparedDialogAction\(\".*consumed|getPreparedDialogAction\(\).*clearPreparedDialogAction|WindowReadyWaitService|DIALOG_PREPARED|TASK_TRACKER_PREPARED|latestFreshHigherPriority|latestFreshForWindow' src/main/java/com/bot/dhxy -S`
  - no matches。
- `mvn -q -DskipTests compile` passed。

Next:

- 实测 route dialog 场景时重点看：
  - `event=window.ready.consumePrepared`
  - `route dialog uses consumed prepared action`
  - `route dialog world-map gate`
  - 不应再看到已经有 route dialog / watcher 正在准备时又重复打开世界地图。

### 唐德 - 2026-06-14 Phase 3 Runner route dialog producer 收口

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 3 唐德工作包 A 执行。
- 本次只收紧 `WindowTaskRunner.refreshDialogPreparationSignal(...)` 的 route dialog producer 边界。
- 除了给 `NavigationService` 补回两个缺失的 prepared-route wait 常量以恢复编译外，没有改它的行为；也没有改五环/五倍/修罗任务 phase、EventBus priority、任务等待服务或输入队列。

Changed:

- `WindowTaskRunner`
  - route dialog preparation 仍以 `refreshDialogPreparationSignal(...)` 为 Runner 侧主入口。
  - 准备目标的来源顺序固定为：
    - `DialogPreparationRequest.targetKeyword`
    - request 缺目标时，回退到当前 active pathing intent 的 `targetMapName`
  - 只处理 `DialogOperation.ROUTE_TRANSFER`。
  - 准备前必须先读取 `WindowRuntimeContext.visibleDialogSnapshot`：
    - windowId 必须匹配当前 runtime；
    - hwnd 必须匹配当前 native binding；
    - snapshot 不能超过 `WINDOW_DIALOG_VISIBLE_MAX_AGE_MS`；
    - visible type 必须是 `DialogType.OPTION`。
  - `visible` 缺失、不是 OPTION、过期或绑定不匹配时，Runner 不准备 action；绑定/过期类失败会标记当前 request failed，但不会清 visible snapshot。
  - 准备成功后仍通过 `WindowRuntimeContext.updatePreparedDialogAction(...)` 写入 action 和 READY 状态。
  - 准备失败只调用 `markDialogPreparationFailed(...)`，不关闭窗口、不点击鼠标、不改任务 phase。
  - route preparation 日志统一为 `route dialog preparation`，包含 result、windowId、hwnd、taskType、operation、target、source、actionSource、visibleType、visibleAgeMs、requestAgeMs、matchedText、click、elapsedMs。
- `NavigationService`
  - 只补回缺失常量：
    - `ROUTE_PREPARED_DIALOG_WAIT_MS = 200L`
    - `ROUTE_PREPARED_DIALOG_WAIT_POLL_MS = 50L`
  - 这是为了修复当前工作树已有改名残留导致的 compile failure，不改变 route dialog 行为。

Verify:

- `mvn -q -DskipTests compile` passed。
- `rg -n "WindowReadyWaitService|waitForPathingWakeOrTimeout|TASK_TRACKER_PREPARED|DIALOG_PREPARED|publishPreparedActionEvent|latestFreshHigherPriority|latestFreshForWindow|currentReadyPriority|priority gate|higherPriority|READY_PRIORITY" src/main/java/com/bot/dhxy -S`
  - no matches。
- `rg -n "refreshDialogPreparationSignal|route dialog preparation|detectDialogTypeNoFocus|prepareRememberedRouteOption|prepareRouteKeywordOption|updatePreparedDialogAction|markDialogPreparationFailed" src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java -S`
  - confirmed Runner producer path and attention probe only.

Follow-up:

- 下一步应由谢帅继续 Phase 3 工作包 B，确认 `NavigationService.clickRouteDialogOption(...)` 的 foreground OCR 路径只作为明确 `legacy-foreground-route-ocr` fallback，而不是主流程。

### 唐德 - 2026-06-14 Phase 4 Runtime/Runner route dialog 状态完整性

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 4 唐德工作包 A 执行。
- 本阶段不改世界地图点击算法、不改任务 phase、不改 NPC click pipeline、不新增 service。
- 目标是让 Runtime/Runner 写入的 visible/request/preparing/ready/failed 状态足够可观测，方便 Navigation 判断“不能重复打开世界地图”。

Changed:

- `WindowRuntimeContext`
  - 保持 `visibleDialogSnapshot` 作为窗口事实模型，不加入业务字段。
  - 继续在 native binding changed / runtime reset 时清理 visible 和 dialog preparation 状态，避免串 hwnd。
  - `updateDialogPreparationRequest(...)` 现在记录 `window.dialog.prepare.state phase=requested/request-cleared` 日志。
  - `clearDialogPreparationRequest(...)` 现在记录 `phase=request-clear` 日志。
  - `markDialogPreparationStarted(...)` 现在记录 `phase=preparing` 日志。
  - `markDialogPreparationFailed(...)` 现在记录 `phase=failed` 日志。
  - `updatePreparedDialogAction(...)` 在 READY 时保留同一 request 的 `requestCreatedAtMs/preparingStartedAtMs`，并记录 `phase=READY` 日志，包含 requestAgeMs、preparingAgeMs、preparedAgeMs、verifiedAgeMs、matchedText、click。
- `WindowTaskRunner`
  - 沿用 Phase 3 的 visible snapshot gating：
    - visible snapshot 过期/绑定不匹配/不是 OPTION 时不准备 route action；
    - 准备失败只标记 request failed，不清 visible snapshot；
    - `DialogType.NONE` 只打 debug，不清 visible snapshot。
- `NavigationService`
  - 当前工作树已有 Phase 4 B 的半截调用：
    - `routeDialogGateBeforeWorldMap(...)`
    - `submitWorldMapSearchAndClickDestination(request, targetMapName, source)`
  - 为恢复编译并保持 Phase 4 gate 方向，补齐这两个最小入口：
    - `routeDialogGateBeforeWorldMap(...)` 先 consume prepared route action，再走 `shouldYieldForRouteDialogBeforeWorldMap(...)`；
    - 新版 `submitWorldMapSearchAndClickDestination(...)` 开图前再过一次 gate，然后复用原 `submitWorldMapSearchAndClickDestination(String)`。
  - 没有改 world-map 搜索/点击算法。

Verify:

- `mvn -q -DskipTests compile` passed。
- `rg -n "WindowReadyWaitService|DIALOG_PREPARED|TASK_TRACKER_PREPARED|latestFreshHigherPriority|waitForPathingWakeOrTimeout|READY_PRIORITY|currentReadyPriority" src/main/java/com/bot/dhxy -S`
  - no matches。
- `rg -n "window\\.dialog\\.prepare\\.state|window\\.dialog\\.visible|clearVisibleDialogSnapshot|DialogType\\.NONE|route dialog preparation|updatePreparedDialogAction" src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java -S`
  - confirmed visible/preparation logs and no NONE clear path.

Follow-up:

- 下一步应由谢帅执行 Phase 4 工作包 B：统一 `NavigationService` 所有世界地图入口的 consume/gate 检查，确保 legacy fallback 前也先过 gate。

### 唐德 - 2026-06-14 Phase 5A Runner-owned route dialog preparation

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 5 唐德工作包 A 执行。
- 本阶段不改 Navigation 世界地图点击算法、不改任务业务 option、不新增 service、不让 Runner 点击鼠标。
- 目标是让 route dialog 的 prepared action 可以由 `WindowTaskRunner` 根据 active pathing intent 生产，而不是依赖 Navigation 预先写 `DialogPreparationRequest`。

Changed:

- `WindowPathingIntent`
  - 新增 `intentId`，默认 UUID，用作单次导航 intent 的 trace / stale guard。
- `PreparedDialogAction`
  - 新增 `intentId`，route prepared action 会绑定当前 active pathing intent。
- `WindowRuntimeContext`
  - `consumePreparedDialogAction(...)` 现在会用 action intentId 对比当前 active pathing intent，旧 intent action 不会被消费。
  - consume 日志增加 `intentId` / `activeIntentId`，方便判断 prepared action 是否串 intent。
- `WindowTaskRunner`
  - watcher 在 active pathing intent 存在时也会刷新 route dialog preparation，不再要求必须有 `DialogPreparationRequest`。
  - route preparation 目标来源顺序：
    - request target；
    - active pathing intent target map。
  - visible snapshot 仍必须匹配 windowId / hwnd / OPTION / fresh age。
  - 无 request 时会用 `DialogChoiceMemoryService.findUsableRoute(fromMap, targetMap)` 读取 route memory。
  - prepared action 发布前再次检查 request 或 active intent 未被替换。
  - prepared action validation 支持 intent-owned route action：有 intentId 时校验 active intent；无 intentId 时才走旧 request 校验。
  - route preparation 日志增加 `intentId` / `intentAgeMs`。

Verify:

- `mvn -q -DskipTests compile` passed。
- 待本轮最终确认：
  - `rg -n "route dialog preparation: result=prepared|window\\.dialog\\.prepare\\.state|intentId|PreparedDialogAction" src/main/java/com/bot/dhxy/window src/main/java/com/bot/dhxy/model -S`

Follow-up:

- 下一步应由谢帅继续 Phase 5 工作包 B：把 `NavigationService` 从 route dialog preparation request 生产者退回动作边界，确认 `DialogPreparationRequest.builder()` / `updateDialogPreparationRequest(...)` 不再出现在 Navigation route path。

### 唐德 - 2026-06-14 Phase 5C Navigation legacy route dialog cleanup

Status: implemented / compile passed

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 5C 唐德工作包 A 执行。
- 本阶段只收 `NavigationService` route dialog legacy path。
- 不改世界地图搜索/结果点击算法，不改小地图/NPC click，不改五环/五倍/修罗业务策略，不新增 service。

Changed:

- `NavigationService`
  - 删除 `legacy-foreground-route-ocr` 路径。
  - 删除 route dialog 内部直接调用 `DialogService.handleRouteKeywordOption(...)`。
  - 删除 route memory 直接点击路径，不再调用 `DialogService.handleRememberedRouteOption(...)`。
  - 删除 `NavigationService` 内对 `DialogChoiceMemoryService.findUsableRoute(...)` 的直接读取。
  - `clickRouteDialogOption(...)` 现在只做三件事：
    - consume Runtime 已准备好的 `ROUTE_TRANSFER` prepared action；
    - 如果 Runtime 有 fresh visible/preparing/prepared route state，则返回 `DIALOG_PREPARING`，避免重开世界地图；
    - 如果没有 fresh state，则返回 `NO_DIALOG`，交给调用方决定是否重试导航。
  - 保留 prepared action 点击成功后的 pending transfer memory 写入；这只是记录实际点击结果，不再由 Navigation 根据 memory 自己点击。

Verify:

- `rg -n "legacy-foreground-route-ocr|handleRouteKeywordOption|handleRememberedRouteOption|findUsableRoute" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - no matches。
- `rg -n "DialogPreparationRequest\\.builder|updateDialogPreparationRequest\\(|detectDialogTypeNoFocus" src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - no matches。
- `rg -n "prepareRouteKeywordOption|prepareRememberedRouteOption|findUsableRoute|route dialog preparation" src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java src/main/java/com/bot/dhxy/service/DialogService.java -S`
  - Runner/DialogService route preparation still present。
- `rg -n "consumePreparedDialogAction|intentId|window.ready.consumePrepared" src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java src/main/java/com/bot/dhxy/service/NavigationService.java -S`
  - consume / intent logs present。
- `mvn -q -DskipTests compile` passed。

Follow-up:

- 需要谢帅继续 Phase 5C 工作包 B：确认 fresh route dialog gate / stale timeout 日志足够解释“为什么等待”和“为什么允许重开世界地图”。
- 实跑时重点看是否仍出现 route dialog 可见但重复打开世界地图；如果出现，应从 `route dialog world-map gate`、`window.ready.consumePrepared`、`route dialog preparation` 三类日志定位。

### 何黎 - 2026-06-15 Route dialog visible same-tick preparation

Status: implemented / compile passed

Context:

- 用户 23:59:30 后五窗口实跑：多个窗口卡在路线 option dialog；部分窗口在仍未处理 dialog 时又重开世界地图重复导航。
- 日志显示 Runner 已能 no-focus 检测到 `OPTION` 并写 `window.dialog.visible.update`，但当时只发布 `TASK_ATTENTION_REQUIRED`。
- 下一轮 watcher 再做 `refreshDialogPreparationSignal(...)` 时，visible snapshot 已经约 9 秒，被 `visible-expired` 拒绝，导致 `NavigationService` 没有 prepared action 可消费，并继续世界地图重试。

Changed:

- `WindowTaskRunner`
  - `publishTaskAttentionIfDialogVisible(...)` 从 boolean 改为返回 `PreparedDialogAction`。
  - 在 no-focus 检测到 visible dialog 并写入 `WindowRuntimeContext` 后，立即同轮调用 `refreshDialogPreparationSignal(...)`。
  - 仍然只发布 `TASK_ATTENTION_REQUIRED` 软唤醒，不点击、不关闭 dialog、不新增 `DIALOG_PREPARED` 事件。
  - attention 日志增加 `preparedRoute=true/false`，用于实跑确认是否在 fresh visible 窗口内完成 route prepared action。

Verify:

- `rg -n "publishTaskAttentionIfDialogVisible" src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java -S`
- `mvn -q -DskipTests compile` passed。

Next live-check:

- 路线 option dialog 出现后，应看到：
  - `event=window.dialog.visible.update ... type=OPTION`
  - 紧接着 `route dialog preparation: result=start/prepared`
  - `task attention published ... preparedRoute=true`
  - 任务/导航消费时出现 `event=window.ready.consumePrepared ... result=consumed` 或 `route dialog uses consumed prepared action`
- 如果仍重开世界地图，重点看是否仍有 `visible-expired`、`prepare-miss`，或 prepared action 是否被 task generic dialog 分支抢先处理。

### 唐德 - 2026-06-15 Phase 6C-A/B Navigation world-map input split and replay

Status: PARTIAL / compile passed / replay reproduced failure.

Context:

- 按 `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md` 的 Phase 6C 唐德任务执行。
- 07:47 实跑样本证明 same-tick prepared route patch 已加载，但部分窗口仍在 route dialog 缺 fresh prepared action 时重开世界地图。
- `hwnd-61F5A / 岁月醉白头` 的 `submitWorldMapSearchAndClickDestination:长安` 在旧日志里约占住输入队列 8 到 9 秒；同段出现 `destination mismatch actual=` failure case。

Changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
  - 将 `performWorldMapSearchAndClickDestination(...)` 从一个大 exclusive callback 拆成三段：
    - 短 exclusive：打开世界地图/寻路面板、输入目标、滚动结果；
    - 非 exclusive：截图、OCR、目标判定、failure case 归档；
    - 短 exclusive：点击最终路线候选并关闭路线面板。
  - 新增 `prepareWorldMapSearchResultsDirect(...)`，只放真实物理输入。
  - `clickDestinationFromWorldMapSearchResults(...)` 的最终点击改为短 exclusive，并在点击后关闭路线搜索面板。
  - 新增分段 latency 日志：`navigation map search split: stage=prepare` / `stage=scan-click`。

Replay:

- 输入图：`images/failure-cases/world-map-route/20260615_074825_705_长安_destination-mismatch/raw.png`
- 输出图：`images/temp/world_map_route_guard_replay/20260615_081025/raw_marked.png`
- 命令：
  - `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.WorldMapRouteGuardReplayDebug" "-Dexec.args=长安 D:\mavenProject\DHXY\images\failure-cases\world-map-route\20260615_074825_705_长安_destination-mismatch\raw.png"`
- 结果：
  - `ok=false`
  - expected=`长安`
  - actual 为空
  - OCR words=`[]`
  - final click point 为空
- 结论：
  - failure case 的 raw crop 不是完整路线结果行，只截到路线窗口底部/侧边碎片；因此 actual 为空是截图/crop 内容不完整，不是 OCR 把 `长安` 识别成别的目标。
  - 本轮没有改世界地图视觉匹配或点击坐标算法；replay 是定位和记录用，不是坐标算法改动验收。

Verify:

- `mvn -q -DskipTests compile` passed。
- `rg -n "legacy-foreground-route-ocr|handleRouteKeywordOption|handleRememberedRouteOption|route dialog probe uses prepared action|route dialog preparation requested after map route click" src\main\java\com\bot\dhxy\service\NavigationService.java -S`
  - no matches。

Next:

- 下一轮实跑重点看新分段日志：
  - `stage=prepare` 是否明显短于旧 6 到 9 秒整段 exclusive；
  - prepared route action 是否仍排在长世界地图动作后；
  - 如果 `destination mismatch actual=` 再现，先检查 archived raw crop 是否还是不完整路线结果区域。

### 谢帅 - 2026-06-15 Phase 6C-C Runner watcher tick latency split

Status: implemented / compile passed.

Context:

- 07:47 旧日志只能看到 route dialog prepared 覆盖率低，但不能在同一行里区分 watcher 本轮到底慢在 pathing、route dialog prepare、左侧任务面板 prepare，还是 attention probe。
- 旧样本里 route dialog preparation 可以到 9 秒级；下一轮实跑需要更直接的分段耗时。

Changed:

- `WindowTaskRunner`
  - 在 watcher loop 内记录：
    - `pathingMs`
    - `routePrepareMs`
    - `taskTrackerPrepareMs`
    - `attentionMs`
    - `totalMs`
    - `nextIntervalMs`
  - 新增日志：
    - `window observer tick: task=... branch=... totalMs=... pathingMs=... routePrepareMs=... taskTrackerPrepareMs=... attentionMs=...`
  - 日志同时带 active intent id / target / age、pathing snapshot state / current map / target、prepared action operation / target。

Behavior boundary:

- 不改 Runner 点击行为。
- 不改 Navigation / Dialog / 五环 / 五倍 / 修罗业务逻辑。
- 只在慢 tick、存在 prepared action、或 active pathing snapshot 时打印，避免 100ms tick 刷屏。

Verify:

- `mvn -q -DskipTests compile` passed。

Next live-check:

- 如果窗口长时间不接权，看 `window observer tick` 中哪一段超过 1000ms。
- 如果 `routePrepareMs` 高但没有 prepared，继续看同窗口附近的 `dialog prepare route result/miss`。
- 如果 `pathingMs` 高，结合 `location.scanCurrent.breakdown` 判断是不是截图/capture 慢。

### 唐德 - 2026-06-15 Phase 6C-A/B rework: atomic consume and positive route replay

Status: READY_FOR_REVIEW / compile passed.

Context:

- 何黎验收指出两个必须返工点：
  - `WindowRuntimeContext.consumePreparedDialogAction(...)` targeted consume 不是条件原子消费；
  - world-map replay 只有旧 failure 复现，没有 positive PASS 样本。

Changed:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
  - `consumePreparedDialogAction(DialogOperation expectedOperation, String expectedTargetKeyword, String reason)` 改成 CAS loop。
  - mismatch 现在非破坏性返回，不会先清空再尝试恢复。
  - consume 成功后只清理仍然匹配 consumed action 的 READY 状态，避免清掉 watcher 刚写入的新 READY。
  - `consumePreparedDialogAction(String reason)` 也改成 CAS loop；当前外部调用仍只有 targeted consume。
- `src/main/java/com/bot/dhxy/window/model/WindowDialogSnapshot.java`
  - 确认这是 route dialog runtime 方案产物，当前被 Runtime/Runner 引用；提交时必须纳入。
- `images/test-cases/world-map-route/positive/route_result_changan_complete_raw.png`
  - 新增真实完整路线结果 positive testcase。

Replay:

- Positive sample:
  - 输入图：`images/test-cases/world-map-route/positive/route_result_changan_complete_raw.png`
  - 命令：`mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.WorldMapRouteGuardReplayDebug" "-Dexec.args=长安 D:\mavenProject\DHXY\images\test-cases\world-map-route\positive\route_result_changan_complete_raw.png"`
  - 结果：`passed=1 failed=0`，actual=`长安`，allowClick=`true`，click point=`(120,93)`。
  - marked：`images/temp/world_map_route_guard_replay/20260615_090803/route_result_changan_complete_raw_marked.png`
- Negative diagnostic sample:
  - 输入图：`images/failure-cases/world-map-route/20260615_074825_705_长安_destination-mismatch/raw.png`
  - 命令：`mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.WorldMapRouteGuardReplayDebug" "-Dexec.args=长安 D:\mavenProject\DHXY\images\failure-cases\world-map-route\20260615_074825_705_长安_destination-mismatch\raw.png"`
  - 结果：`passed=0 failed=1`，actual 为空，OCR words=`[]`。
  - marked：`images/temp/world_map_route_guard_replay/20260615_090821/raw_marked.png`
  - 结论：旧 failure raw 是坏 crop 诊断样本；尺寸为 `323x138`，但内容不是完整路线结果行。

Verify:

- `mvn -q -DskipTests compile` passed。
- `rg -n "getAndSet\(null\)|race-mismatch-restored|clearReadyDialogPreparationStatus\(" src\main\java\com\bot\dhxy\window\runtime\WindowRuntimeContext.java -S`
  - targeted consume 中不再有 `getAndSet(null)` / restore 逻辑。

Next:

- 重新跑一轮五环 route option 样本，看 CAS consume 后是否还出现 prepared action 被错误清空。
- 如果 `destination mismatch actual=` 再现，先看 archived raw 是否仍是坏 crop；若是，再查 capture 时机/route panel 完整刷新，而不是改 OCR 文字匹配。

### 谢帅 - 2026-06-15 修罗 hot-start STORY 白字模板确认

Status: DONE / compile + replay passed.

Context:

- 热启动时 `TaskHotStartService` 可能把固定区域里的玩家/NPC 名字误判成 `STORY_DIALOG`。
- 修罗不能只凭 `STORY_DIALOG` 进入 `READ_OBJECTIVE`；必须确认当前 story 文本里有修罗目标提示。

Changed:

- `images/template/dialog/xiuluo/xiuluo_story_miexiu_confirm.png`
  - 由用户新增原图 `Snipaste_2026-06-15_14-22-49.png` 洗白字生成，内容为“消灭修罗”。
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoHotStartResolver.java`
  - `STORY_DIALOG` hot-start 改为调用 `DialogService.handleDialog(VERIFY_WHITE_TEMPLATE)`。
  - 只有命中 `xiuluo_story_miexiu_confirm.png` 时才进入 `READ_OBJECTIVE`。
  - 未命中时回到正常接任务入口 `XiuluoRoundContext.start(round)`，避免误把非任务 story 截图当成修罗目标。
- `src/main/java/com/bot/dhxy/debug/XiuluoHotStartStoryTemplateReplayDebug.java`
  - 新增只读 replay 工具，复用生产链路：`washThinWhiteTextToBlackAndWhite` + `ImageFinder.find`。

Replay:

- Positive sample:
  - 输入图：`images/test-cases/dialog/xiuluo-hotstart-story/raw/positive_story_raw.png`
  - 结果：matched=`true`，score=`1.0000`，point=`(31.5,10.0)`。
  - marked：`images/test-cases/dialog/xiuluo-hotstart-story/output/positive-template-source_marked.png`
  - washed：`images/test-cases/dialog/xiuluo-hotstart-story/output/positive-template-source_white.png`
- Negative sample:
  - 输入图：`images/test-cases/dialog/xiuluo-hotstart-story/raw/false_story_labels_raw.png`
  - 来源：旧日志里只有场景/人物/NPC 名字、但曾进入 `READ_STORY_OBJECTIVE` 的误判截图。
  - 结果：matched=`false`。
  - marked：`images/test-cases/dialog/xiuluo-hotstart-story/output/false-story-labels_marked.png`
  - washed：`images/test-cases/dialog/xiuluo-hotstart-story/output/false-story-labels_white.png`
- 命令：
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests exec:java "-Dexec.mainClass=com.bot.dhxy.debug.XiuluoHotStartStoryTemplateReplayDebug"`

Notes:

- 不改 `DialogService` 阈值。
- 不改变 STORY/OPTION 通用检测，只给修罗 hot-start 加二次确认。
- `compile` 和 `exec:java` 不要并行跑；并行时可能因为 `target/classes` 正在被重写而出现 transient class-load failure。

### 谢帅 - 2026-06-15 current-map 点击触发跨图绕路修正

Status: DONE / compile passed.

Context:

- 用户观察到 14:57:00-14:57:30 队长已经在导航途中，却又打开小地图点了一次，第二次点歪打断原路线。
- 日志确认：
  - `14:57:20.428` 第一次 `navigateInCurrentMap` 点击 `logical=(371,80)`。
  - `14:57:23.073` 点击后小地图读到 `北俱芦洲(40,9)`，而 expected map 是 `大唐边境`。
  - 旧逻辑把“坐标变化但地图变成 unexpected map”判为 `NO_PATHING`，于是同一 foreground turn 进入 fallback 第二次点击。

Changed:

- `src/main/java/com/bot/dhxy/service/NavigationService.java`
  - `confirmMiniMapPathingStarted(...)` 中，点击后坐标发生变化但地图短暂变成非 expected map 时，不再返回 `NO_PATHING`。
  - 改为记录 `coordinate changed on unexpected map, treat as pathing started` 并返回 `PATHING_STARTED`。

Reason:

- 游戏的“当前地图寻路”可能借道传送点，从 A 到 C 时短暂经过 B 图；这说明点击已经触发寻路，不应该再 fallback 点第二次小地图。

Verify:

- `mvn -q -DskipTests compile` passed。

### 唐德 - 2026-06-16 NPC click vision memory 聚合与五倍残数据重置

Status: DONE / compile passed.

Context:

- 本地 `config/vision_memory.json` 中五倍 `宝象国|降魔侍卫|86,87` 数据已经只剩残样本，和 ROI 推荐数据发生断层。
- 用户决定不再恢复旧五倍数据，改为重新统计；修罗已有数据需要保留并整合。
- 新约束：同一个目标 + 同一个玩家站位不应无限追加重复样本，应该按主键聚合统计。

Changed:

- `src/main/java/com/bot/dhxy/vision/OcrRoiMemoryService.java`
  - `recordNpcClickAttempt(...)` 现在同步更新 `policies.clickPolicies`。
  - `clickPolicies` 主键为 `npcClickKey + player:x,y`，也就是目标身份和玩家逻辑坐标绑定在一起。
  - 同一主键的 `npcClickSamples` 只保留最新一条诊断样本，避免同站位重复膨胀。
  - 直接点击推荐和 NPC 点击 ROI 推荐优先读取 `clickPolicies`；旧 `npcClickSamples` 只作为兼容 fallback。
  - `ClickPolicy` 增加目标、玩家坐标、成功/失败次数、连续成功/失败、最近强验证样本、置信度等字段。
- `config/vision_memory.json`
  - 先备份到 `config/vision_memory.json.backup-20260616_144443`。
  - 删除五倍 `降魔侍卫` 相关残留 NPC click / target candidate / ROI policy 数据，让五倍重新学习。
  - 将其余 NPC click 样本迁移为按主键聚合的 `clickPolicies`。

Data result:

- `npcClickSamples`: `84 -> 50`
- `targetCandidateSamples`: `73 -> 59`
- `roiPolicies`: `64 -> 57`
- `clickPolicies`: `0 -> 50`
- `降魔侍卫` 旧 NPC click / ROI 数据：`0`
- 修罗 click policy：`26`

Verify:

- `mvn -q -DskipTests compile` passed。
- `node` 校验确认 `wubeiNpcSamples=0`、`wubeiRoiPolicies=0`、`xiuluoPolicies=26`。

Next:

- 下一轮五倍跑 `降魔侍卫` 时，从 full masked window fallback 重新统计；成功验证达到稳定阈值后才会重新启用 learned click/ROI。
- 如果再出现 ROI 有数据但 click point 没数据的断层，优先查 `clickPolicies` 是否缺失对应 `npcClickKey + player:x,y`，不要单独相信旧 ROI。

### 谢帅 - 2026-06-16 NPC Ctrl prompt 去中心兜底

Status: DONE / compile passed / waiting live validation.

Context:

- 用户判断 `window-center` Ctrl prompt 兜底不稳定，要求先不要再用中心点兜底。
- 更可靠的兜底应该来自紫色玩家名字位置：紫色名字/紫色 blob 基本能代表当前角色所在位置，用它作为 Ctrl prompt origin 比窗口中心更接近目标交互区。
- 额外约束：非战斗 NPC 的 Ctrl prompt 只考虑靠近目标屏幕预测点的候选 origin；不是按“人物到 NPC 的地图距离”过滤。

Changed:

- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - 默认 `clickNpcByCtrlMenuScan(...)` 不再追加 `window-center` origin。
  - `tryPlayerAnchorFormulaStrategy(...)` 在紫色玩家 anchor 可用时，把 `purple-player-anchor` 加入后续 `state.ctrlProbeOrigins`，供最终 Ctrl prompt 使用。
  - `tryCtrlMenuStrategy(...)` 对非 `NpcRole.COMBAT_TARGET` 增加候选点距离 gate：先用紫色 player-anchor 公式得到 `predictedClickAbs` 作为目标屏幕参照点，再过滤 `state.ctrlProbeOrigins`；候选 origin 离参照点超过 `15px` 时跳过。
  - 战斗目标继续允许 Ctrl prompt / direct combat 相关兜底，不受上述距离 gate 影响。

Notes:

- 紫色 anchor 截图入口仍会先按 `Alt+4`，代码路径是 `captureCleanNameRegionToMemory(...)`，用于隐藏其他玩家名字后再截图洗紫色。
- `direct-combat exit` 内部仍保留一个独立的 `window-center fallback`，本次没有动；它不是普通 NPC prompt 的最终兜底。
- 本次没有新增点击算法，也没有改黄字/tooltip/learned memory 的匹配顺序。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮实测重点看日志 `NPC ctrl menu probe origins`：
  - 正常不应再出现 `window-center:FULL_RING`。
  - 如果紫色 anchor 成功，应出现 `purple-player-anchor:FULL_RING@(x,y)`。
  - 非战斗 NPC 的远处候选点应出现 `NPC ctrl menu origin skipped for non-combat target ... maxDistancePx=15`。

### 谢帅 - 2026-06-16 五倍队员三技能 gate

Status: DONE / compile passed / waiting live validation.

Context:

- 实测观察到五倍队长刚接完任务、还没有点击左侧任务追踪绿字寻路时，队员 AutoBattle 已经插进来跑三技能。
- 日志也能复现：队员在 `source=auto-battle` 下 claim 了 `teamRound=wubei#6`，而队长稍后才进入 `TRACKER_PATHING result=PATHING_STARTED`。
- 既有 `TaskMaintenanceService` 已有团队维护窗口 gate，但 `AutoBattleTask` 之前只对 `xiuluo_v2` 要求该 gate；五倍只注册 round，没有打开/关闭窗口。

Changed:

- `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
  - 队员辅助模式下，`requestedTaskCode=wubei` 现在和 `xiuluo_v2` 一样，三技能必须等 `requireOpenTeamMaintenanceWindow=true` 的 gate 打开。
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 左侧任务追踪绿字点击成功并注册 tracker pathing intent 后，打开本轮 `wubei` team pathing maintenance window。
  - 进入 `ENTER_BATTLE` 阶段开头关闭该 window，不依赖地图/坐标读取；这样覆盖普通怪、显形镜白龙马、黄袍怪触发开打 dialog 前的关闭点。
  - 当前轮恢复入口也会关闭 gate，避免 tracker pathing 后、未进入战斗前失败恢复导致 gate 残留。

Expected behavior:

- 队长没有点击左侧任务追踪绿字寻路前，队员三技能应被 `team pathing window closed` 拦住。
- 队长点击左侧绿字后到进入打怪处理前，队员最多按本轮 claim 限制跑一个三技能。
- 进入 `ENTER_BATTLE` 后 gate 关闭；打怪弹窗/显形镜/黄袍怪续战阶段不允许队员三技能抢输入。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮五倍实测看日志：
  - 接任务后、左侧寻路前，队员如三技能到期应出现 `summon skill deferred: team pathing window closed`。
  - 队长点击 tracker 绿字后应出现 `maintenance team pathing window opened: teamRound=wubei#...`。
  - 进入 `ENTER_BATTLE` 后应出现 `maintenance team pathing window closed: teamRound=wubei#...`。

### 唐德 - 2026-06-16 本地补给改为后台预计算计划

Status: DONE / compile passed / waiting live validation.

Context:

- 用户要求本地战后补给必须尽量像医宝宝维护广播一样快：后台 watcher 先算好每个窗口到底要补哪几条，轮到窗口时只做短点击动作。
- 旧链路虽然第一步已经是一张截图检查四条血蓝，但 pending 队员拿到权后还会重新 no-focus 预检，并且 focused `healAll` 会逐条确认、逐条等待，视觉上明显慢于医宝宝。

Changed:

- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  - `probeFirstAidSupplyNoFocus(...)` 现在会把后台截图中命中的补给目标保存成 `pendingNoFocusFirstAidPlan`，目标包含人物血/人物法/宝宝血/宝宝法的窗口相对点击点和阈值。
  - 新增 `performCachedFirstAidPlanNow(...)`：轮到窗口时直接消费后台预计算计划，不再重新扫四条血蓝。
  - 缓存计划执行时走一个 exclusive input transaction，按预计算目标快速右键；当前每个右键后等待 `220ms`，最后等待 `300ms`。
- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
  - pending follower first-aid 获取 task turn 后优先执行缓存补给计划；没有计划时才 fallback 到原来的 focused check。

Expected behavior:

- 战斗退出后，队员窗口应该在 no-focus 阶段输出 `first-aid no-focus precheck result: needed=true targets=[...]`。
- 队员拿到 pending 补给权后，应该优先输出 `执行后台预计算补给计划`，而不是重新跑 `performFirstAidCheckNowIfNeeded`。
- 多个队员需要补给时，切到每个窗口后只应执行快速右键序列，不再逐条 350ms 二次确认。

Verify:

- `mvn -q -DskipTests compile` passed。
- `git diff --check -- src/main/java/com/bot/dhxy/service/PlayerStateService.java src/main/java/com/bot/dhxy/service/AutoCombatService.java` passed（仅有 CRLF warning）。

Next:

- 下一轮五倍/五环实测重点看 `first-aid no-focus precheck result`、`执行后台预计算补给计划`、`playerState:healCachedPlan`。
- 如果右键过快导致某条没吃上，再只调 `FAST_HEAL_CLICK_DELAY_MS`，不要回退到逐条截图确认。

### 谢帅 - 2026-06-16 五倍白龙马 hint 抢时机收窄

Status: DONE / compile passed / waiting live validation.

Context:

- 日志确认 `first-probe` 与 `second-probe` 的 hint 截图区域一致，都是 `region=350,370 -> 679,463`，实际截图 `rect=(364,452)-(693,545)`。
- `second-probe` 失败不是区域问题，而是时机问题：物理左键在 `21:14:29.171`，但原逻辑等 `dialogAfter` 检测结束后才 schedule hint，导致第一张截图到 `21:14:33.571`，约 4.4 秒后，浮层已经消失。
- 用户确认：普通五倍怪左键能直接进入战斗，destination hint 只对白龙马显形镜 probe 有意义。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 左侧任务追踪绿字点击成功后，先 schedule 白龙马 hint 截图，再做 `dialogAfter` 诊断日志。
  - hint 采样从 `1500/2500/3500ms` 提前到 `500/1000/1500ms`。
  - hint 捕获条件收窄为 `first-probe` / `second-probe`，普通怪和黄袍怪不再抓 destination hint。
  - 保留普通 tracker 绿字的 runner pathing intent；只是不再为非白龙马抓 hint。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮白龙马实测看 `destination hint capture start` 是否紧贴 `tracker-green-click:second-probe`。
- 重点确认 `second-probe` 的第一张 raw 图是否已经能截到“正在自动寻路前往...”浮层。

### 谢帅 - 2026-06-17 五倍白龙马 story prepared 快路径

Status: DONE / compile passed / waiting live validation.

Context:

- 白龙马显形镜 story 本质是 0.5-1 秒级短交互，不应该像长路径导航一样“用完道具就普通放权等下一轮”。
- 旧逻辑在 `consumePrepared=null` 时会走 no-story 尝试点怪、随后 `markProbeResolved` 并切下一条绿字；这会把“runner 还没准备好”误判成“当前 prompt 失败”。
- Runner 原 idle interval 是 6000ms；只注册 `WUBEI_PROBE_STORY` interest 不能保证已经睡眠中的 watcher 立刻醒来。

Changed:

- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
  - 新增 `observerWakeSeq`；`updateDialogInterest(...)` 会递增 wake 序号并打印 `wakeSeq`。
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
  - observer sleep 改为 100ms 小片段检查 `observerWakeSeq`，让 task 更新 dialog interest 时可以提前唤醒 watcher，不必等满 6 秒 idle。
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - `resolveProbeAfterPathing(...)` 在使用显形镜前注册 `WUBEI_PROBE_STORY` interest。
  - 使用显形镜后当前 turn 短等 800ms，每 80ms 只消费 runner prepared action，不做前台 OCR/截图/模板匹配。
  - 短等没拿到 prepared 时只保持当前 prompt 等 runner；6 秒总等待超时后重走当前绿字，不再 mark resolved，不再切第二条 prompt。
  - `wrong-position` story 仍然回到当前绿字重新寻路；`target-ready` story 仍然立即点击白龙马。

Expected behavior:

- 白龙马显形镜弹出 story 后，日志应出现 `window.dialog.interest.update ... WUBEI_PROBE_STORY ... wakeSeq=...`，runner 不应因为 idle sleep 拖 6 秒。
- 如果 runner 800ms 内准备好，当前 task turn 直接消费并点击白龙马。
- 如果 runner 没准备好，日志应是 `probe story not prepared yet; keep current probe waiting`，不应再出现因为 story missing 而切到 `probe-next-unused`。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮白龙马实测重点看：
  - `probe story not prepared yet` 是否明显减少；
  - `target-ready story matched` 后是否直接点白龙马；
  - 不应再出现 `probe story still missing after retries; switch to next unused probe` 这类切第二条 prompt 的旧日志。

### 谢帅 - 2026-06-17 五倍白龙马 Runner 框架回归旧业务分支

Status: DONE / compile passed / waiting live validation.

Context:

- 用户确认新版可以继续使用 Runner/PreparedDialogAction 框架，但白龙马业务决策必须按旧版 `6698793` 的逻辑走。
- 旧版核心分支：
  - `target-ready` story：标记当前 probe resolved，马上点白龙马，进入 `WAIT_BATTLE_FINISH`。
  - `wrong-position` story：回滚显形镜尝试次数，不标记 resolved，不切第二条 prompt，回 `TRACKER_PATHING` 重点当前绿字。
  - story 存在但两个白字模板都没命中：先清一次未知 story，再尝试点白龙马 tooltip；点不到才标记当前 prompt resolved 并切下一条。
  - 没有 story：尝试点白龙马 tooltip；点不到才标记当前 prompt resolved 并切下一条。
- 新架构下 `consumePrepared=null` 只能说明 runner 暂时没准备出 `WUBEI_PROBE_STORY`，但最终业务分支仍要映射回旧版 `WHITE_TEMPLATE_NOT_FOUND + DialogType` 结果，而不是无限等待或直接判失败。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - `waitForPreparedProbeStory(...)` 改为返回 `DialogResult`，800ms 内只消费 runner prepared action。
  - 800ms 内没拿到 prepared 时，读取 runner 的 fresh visible dialog snapshot：
    - fresh `STORY` -> 返回旧版等价的 `WHITE_TEMPLATE_NOT_FOUND/STORY`；
    - 无 fresh story -> 返回旧版等价的 `WHITE_TEMPLATE_NOT_FOUND/NONE`。
  - 恢复旧版 `closeUnknownProbeStoryIfNeeded(...)`：未知 story 只清一次，再走 tooltip/direct combat fallback。
  - `target-ready` / `wrong-position` 分支保持旧版语义；wrong-position 不切 prompt，只回当前绿字。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮白龙马实测看：
  - runner 已看到 `STORY visible-only` 但没 prepared 时，前台是否按旧版 fallback 继续处理；
  - `wrong-position` 是否稳定重走当前绿字；
  - 只有 tooltip/direct combat fallback 也失败时才切第二条 prompt。

### 谢帅 - 2026-06-17 五倍降魔侍卫黄字目标匹配 replay

Status: DONE / compile passed / offline testcase passed.

Context:

- 五倍 `NPCClickSmart` 黄字目标匹配会把共享“侍卫”的非目标文本排到前面，例如 `宝象国品侍卫`，导致 `降魔侍卫` 没有成为第一顺位。
- 扫描窗口本身没有改；本次只收紧目标匹配排序和强命中条件。

Changed:

- `src/main/java/com/bot/dhxy/vision/GameTextLineOcrService.java`
  - `findYellowTarget(...)` 在目标模式下扩大候选数量，但只有强命中才提前结束。
  - 对 `降魔侍卫` 增加严格目标判断，必须有足够公共字符并优先完整 OCR 命中，避免共享后缀的弱命中抢第一。
  - 严格目标没有 strong hit 时返回 `hit=false`，避免 `宝象国品侍卫` 这类共享“侍卫”的候选被当成可点击目标。
- `src/main/java/com/bot/dhxy/debug/YellowTargetFindReplayDebug.java`
  - 离线 replay 现在同时输出完整洗黄字图、最终选中的洗后候选行、原图点击标记图。
  - `yellow_marked.png` 直接在洗黄字图上画最终 accepted target；`hit=false` 时不画点击点。

Replay:

- Command:
  - `mvn -q -DskipTests compile`
  - `java "-Dfile.encoding=UTF-8" -cp <target classpath> com.bot.dhxy.debug.YellowTargetFindReplayDebug images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png 降魔侍卫 images/test-cases/npc-click/jiangmo-guard-yellow-target/selected_line_after.png images/test-cases/npc-click/jiangmo-guard-yellow-target/marked_after.png images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_washed_after.png images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_overlay_after.png`
- Input:
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png`
- Outputs:
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_washed_after.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_overlay_after.png`（洗黄字图上的最终命中/点击标记）
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/selected_line_after.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/marked_after.png`
- Result:
  - `variant=yellow-target-shadow`
  - selected text `降魔侍卫`
  - selected relative center `(328,664)`
  - final click `(328,614)`

Additional replay set:

- `images/test-cases/npc-click/jiangmo-guard-yellow-target/hwnd-6C71E0A/yellow_marked.png`
  - `hit=true`
  - selected text `降魔侍卫`
  - selected relative center `(255,205)`
  - final click `(255,155)`
- `images/test-cases/npc-click/jiangmo-guard-yellow-target/hwnd-D7D0DEA/yellow_marked.png`
  - `hit=true`
  - selected text `降魔侍卫`
  - selected relative center `(257,372)`
  - final click `(257,322)`
- `images/test-cases/npc-click/jiangmo-guard-yellow-target/hwnd-1EA0D2E/yellow_marked.png`
  - `hit=false`
  - best raw weak text was `宝象国品侍卫`, but it is rejected because it is not a strong `降魔侍卫` match.
- `images/test-cases/npc-click/jiangmo-guard-yellow-target/hwnd-1E370FB2/yellow_marked.png`
  - `hit=false`
  - selected diagnostic text `服装店老板`, not accepted.
- `images/test-cases/npc-click/jiangmo-guard-yellow-target/hwnd-4010E56/yellow_marked.png`
  - `hit=false`
  - selected diagnostic text `IL1` / `教书先生`, not accepted.

### 谢帅 - 2026-06-17 五倍降魔侍卫 NPC 黄字颜色范围收紧

Status: DONE / compile passed / replay passed.

Context:

- 用户指出上一轮混用了不同地点的截图；本次只使用原始场景图验证。
- 目标是把 NPC 名字黄和任务面板/UI 黄分开：NPC target 候选可以看到 `降魔侍卫`，但不要让左侧任务追踪黄、底部战斗面板亮黄进入候选。

Changed:

- `src/main/java/com/bot/dhxy/vision/GameTextLineOcrService.java`
  - 新增 `YELLOW_NPC_TARGET` 颜色模式，仅用于 NPC/怪名字 target 候选。
  - `findYellowTextCandidateResult(...)` 和 `findYellowTarget(...)` 的 target 候选改用更窄的 NPC 黄范围。
  - 通用 `YELLOW_LOOSE` 保留给路线黄字、普通黄字 OCR、dialog fallback，不跟随本次收窄。
  - 日志 variant 从 `yellow-target-loose` 改为 `yellow-target-npc`，避免误导后续排查。

Replay:

- Command:
  - `mvn -q -DskipTests compile`
  - `java "-Dfile.encoding=UTF-8" -cp <target classpath> com.bot.dhxy.debug.YellowTargetFindReplayDebug images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png 降魔侍卫 images/test-cases/npc-click/jiangmo-guard-yellow-target/selected_line_npc_tuned.png images/test-cases/npc-click/jiangmo-guard-yellow-target/marked_npc_tuned.png images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_washed_npc_tuned.png images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_overlay_npc_tuned.png`
- Input:
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/input_center_scan_layer1.png`
- Outputs:
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_washed_npc_tuned.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/yellow_overlay_npc_tuned.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/selected_line_npc_tuned.png`
  - `images/test-cases/npc-click/jiangmo-guard-yellow-target/marked_npc_tuned.png`
- Result:
  - `hit=true`
  - `bestVariant=yellow-target-npc`
  - selected text `降魔侍卫`
  - selected relative center `(328,665)`
  - final click `(328,615)`
  - 洗图中左侧任务追踪黄和底部战斗面板亮黄已被排除；仍保留同类场景 NPC 黄，例如 `宝象国一品侍卫`。

### 唐德 - 2026-06-17 修罗任务栏 hot-start 去掉模板兜底

Status: DONE / compile passed.

Context:

- 14:53:33 修罗启动 hot-start 截到 `images/temp/hwnd-1EA0D2E/quest_detail_xiuluo.png`。
- 这张图是任务栏右侧“任务目的”普通说明文字，不是 `前往 地图(x,y)` 目标。
- 14:53:46 百度 OCR 已经读出完整说明文字并确认没有目标坐标，但旧逻辑仍继续走 `ObjectiveTextRecognitionService` 模板兜底，额外耗时约 24 秒。

Changed:

- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  - `parseTaskPanelObjective(...)` 现在只信任务栏 OCR 解析出的 `前往 地图(x,y)`。
  - OCR 没有解析出修罗目标时直接返回 empty，表示 task-panel hot-start miss。
  - 删除修罗任务栏路径里的模板 fallback，避免普通任务说明被继续扫模板、拖慢启动或产生陈旧目标。

Verify:

- `mvn -q -DskipTests compile` passed。

Next:

- 下一轮修罗启动观察：如果任务栏右侧仍是普通说明，应该在 OCR miss 后直接进入正常接任务流程，不再出现 20 秒以上的 `template-fallback` 扫描。

### 唐德 - 2026-06-17 五倍接任务 NPC 点击后不再放权等待 accept prepared

Status: DONE / compile passed.

Context:

- 23:29:30 左右五倍队长已经用 learned memory 点中 `降魔侍卫`，但 `WUBEI_ACCEPT_TASK` prepared action 没有立刻 ready。
- 旧逻辑只在前台等 `900ms`，等不到就返回 `SHARED_STATE_TRIGGERED`，触发 `MUST_YIELD` 和 `task turn handoff delay=900ms`。
- watcher 本身是后台线程，是否继续准备 dialog 和 task turn 是否放给其他窗口无关；接任务 NPC 已点开的情况下放权会让已打开的接受任务框空等。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - `runAcceptTaskPhase(...)` 在 NPC 已点击后改为继续持有当前 task turn 等待 `WUBEI_ACCEPT_TASK` prepared action。
  - 接任务 accept 等待时间改为使用 dialog interest TTL (`15s`) 作为硬超时，超时后按 phase failed 走恢复，不再返回 `SHARED_STATE_TRIGGERED`。
  - 删除旧的 `WUBEI_ACCEPT_DIALOG_QUICK_WAIT_MS = 900` 常量，避免后续再走 900ms handoff 语义。

Verify:

- `mvn -q -DskipTests compile` passed.
- `git diff --check -- src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java docs\ACTIVE_WORK.md` passed with CRLF-only warnings.

Next:

- 下一轮五倍观察 `ACCEPT_TASK`：点开降魔侍卫后不应再出现 `source=accept-dialog-wait-prepared delayMs=900`。
- 如果仍慢，重点看 runner 日志里的 `attentionDetectMs` 与 `attentionRoutePrepareMs`，而不是 task turn handoff。

### 唐德 - 2026-06-18 五倍启动 Alt+6 只保留一条准备链

Status: DONE / compile passed.

Context:

- 用户观察到启动五倍时会做两轮 Alt+6/屏蔽检测。
- 日志确认同一个 leader 窗口先走 `startup init: wubei ensure Alt+6 visibility before leader prep`，随后又走 `startup init: leader ensure map tracking option and Alt+6 visibility`。
- `ensureAlt6Visibility()` 本身最多会重试 3 次；在两条准备链都调用它时，会出现两轮检查，甚至因为 Alt+6 是 toggle 导致第二轮把已正确状态打坏再重试。

Changed:

- `src/main/java/com/bot/dhxy/window/execution/DefaultWindowTaskStartupInitializer.java`
  - 注入 `BotProperties`。
  - 五倍窄版 Alt+6 guard 只在 `bot.dhxy.task-startup-preparation-enabled=false` 时运行。
  - 当完整 startup preparation 开启时，五倍只走后面的 `prepareTaskStartupWindow()`，由完整准备链负责唯一一次 Alt+6 可见性检查。
- `src/main/java/com/bot/dhxy/window/startup/TaskStartupWindowPreparationService.java`
  - `ensureAlt6Visibility()` 先截图确认 `blacklist_crowd` 状态；已经确认时不再先按 Alt+6，避免 toggle 把状态反向切掉。
  - 保留最多 3 次后台 Alt+6 重试，用于当前状态未确认的场景。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮五倍启动看日志：完整准备开启时应只看到 `wubei full preparation will perform Alt+6 visibility check`，不应再看到五倍窄版和完整准备各跑一轮 Alt+6。

### 谢帅 - 2026-06-18 五倍进入战斗后删除固定等待

Status: DONE / compile passed.

Context:

- 00:12:02-00:12:06 日志显示五倍队长进入 `ENTER_BATTLE` 后，runner 在 00:12:03.543 准备好 `WUBEI_ENTER_BATTLE`，00:12:03.592 被前台消费，00:12:04.476 已确认点击成功。
- 后续慢点包含五倍代码里的固定 `1200ms` 点击后等待，以及普通 `SHARED_STATE_TRIGGERED` 的 `900ms` task turn handoff delay。
- 用户明确要求删掉这两个固定等待，不在这里继续做固定 sleep。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 删除普通进入战斗、priority prepared 进入战斗、direct combat fallback 三处点击后 `1200ms` 固定等待。
  - 删除五倍默认 `TASK_TURN_HANDOFF_DELAY_MS=900`，默认 handoff delay 改为 `0`。
  - `yieldAfterMustYield(...)` 只有在特殊场景返回正数 delay 时才 sleep；普通 shared/yield 不再固定睡 900ms。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮五倍看 `ENTER_BATTLE`：点击成功后不应再看到 `battle-dialog-clicked delayMs=900`，也不应再因为固定 `1200ms` 导致 `heldMs` 接近 3-4 秒。
- 如果用户体感仍觉得“进入战斗前”慢，下一步重点看 runner prepare 前的 dialog 出现时间、`window-task-attention` 检测时间，以及 `waitForPreparedWubeiDialogReply(...)` 是否重复刷新 interest。

### 唐德 - 2026-06-18 五倍显形镜 story dialog interest 优先于 pathing probe

Status: DONE / compile passed.

Context:

- 用户复盘白龙马显形镜流程，关注 `00:13:35.549 -> 00:13:40.547`：task 已注册 `WUBEI_PROBE_STORY` interest，前台每约 `80ms` 消费 prepared action，却一直 `absent`。
- 日志确认不是模板匹配慢：`WUBEI_PROBE_STORY` prepare 本身 `301ms`，`WUBEI_ENTER_BATTLE` prepare `154ms`。
- 真正慢点在 runner observer：`window observer tick totalMs=4708 pathingMs=3871`，即 active-pathing 分支先跑小地图/pathing probe，坐标读取耗时近 4 秒，导致已注册的 dialog interest 被延后处理。
- 显形镜是在停止点使用，业务上此时更关心“显形镜 story 是否出现”，不应该先等小地图 pathing probe。

Changed:

- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
  - active-pathing 分支新增 `active-pathing-dialog-first` 路径。
  - 当当前窗口存在本任务 `dialogInterest` 时，runner 先执行 `publishTaskAttentionIfDialogVisible(...)` / `refreshTaskDialogInterestPreparationSignal(...)`。
  - 如果已经准备出 `PreparedDialogAction`，本轮不再先跑 `refreshPathingSignal(...)`，避免显形镜 story 被小地图 OCR/坐标读取拖住。
  - 如果没有准备出 dialog action，则回到原 active-pathing 的 pathing probe 逻辑。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮五倍显形镜看日志：白龙马 story 出现后应更快看到 `task dialog prepared: operation=WUBEI_PROBE_STORY`。
- 重点观察是否出现 `branch=active-pathing-dialog-first`，且该 tick 不再先出现 `pathingMs=3xxx` 才处理 `WUBEI_PROBE_STORY`。

### 唐德 - 2026-06-18 五倍 ENTER_BATTLE 提前注册进战斗 interest

Status: DONE / compile passed.

Context:

- 用户观察 `00:32:55-00:33:10` 日志，感觉进战斗识别到以后又等了接近 10 秒。
- 复盘确认 `00:33:03.973` 才真正进入 `wubei:ENTER_BATTLE`。
- `00:33:04.316` runner 已经识别到 `OPTION` 对话框，但当时没有 `WUBEI_ENTER_BATTLE` interest，只发布了 `dialog-visible:OPTION`，没有准备点击。
- `00:33:05.467` task 才第一次注册 `WUBEI_ENTER_BATTLE` interest；`00:33:08.505` runner 才准备出 `WUBEI_ENTER_BATTLE`，模板匹配本身只耗时 `318ms`。

Changed:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  - 在 `tickEnterBattle(...)` 首次进入 `ENTER_BATTLE` 阶段时立即注册一次 `WUBEI_ENTER_BATTLE` interest，source 为 `wubei:enter-battle:phase-start`。
  - 保留原来的 `tryClickKnownEnterBattleDialog(...)` 等待/消费逻辑，后续仍由它负责轮询、续期和点击 prepared action。
  - 不修改模板、点击坐标、NPC retry 或战斗业务逻辑。

Verify:

- `mvn -q -DskipTests compile` passed.

Next:

- 下一轮五倍进战斗看日志：进入 `ENTER_BATTLE` 后应先看到 `window.dialog.interest.update ... source=wubei:enter-battle:phase-start`。
- 如果 runner 同一轮看到 `OPTION`，应更早出现 `task dialog prepared: operation=WUBEI_ENTER_BATTLE`，避免之前 `OPTION visible` 但 `preparedRoute=false` 的空转。
