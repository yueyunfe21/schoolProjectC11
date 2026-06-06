# 五倍模板约定

五倍任务代码已经按下面固定路径接好。实际 PNG 需要用游戏截图裁好后放到这些路径。

- `images/template/dialog/wubei/wubei_accept_chumoweiguo.png`
  - 接任务对话框第一条绿色选项左侧模板：`除魔卫`。点击时从模板起点向右随机偏移，避免模板过长导致匹配脆弱。
- `images/template/dialog/wubei/wubei_enter_battle_xiaomie.png`
  - 进入战斗对话框绿色选项：`消灭它`
- `images/template/dialog/wubei/wubei_probe_story_koukou.png`
  - 双绿字探查道具使用成功后的白字 story：`开口说起话来`。匹配到后说明目标已出现，不再跑第二个绿点。
- `images/template/task/wubei_tracker_anchor.png`
  - 任务追踪标题 anchor：`任务追踪`。五倍读取任务追踪黄字/绿字时先在窗口相对区域
    `[+6,+196] -> [+207,+551]` 找这个 anchor，再按 anchor 偏移裁出任务内容区
    `[-96,+12] -> [+86,+73]`。
  - 如果窄区域找不到 anchor，会先确认任务栏是否仍有五倍 active；有任务时再用 OCR 默认全窗口
    推荐区域扩大找 anchor。扩大后仍失败则重新接任务并重试，最多 5 次。
- `images/template/task/wubei_title.png`
  - 任务栏左侧分组标题：`降魔`。用于打开/定位五倍所属任务分组。
- `images/template/task/wubei_active.png`
  - 任务栏左侧当前五倍任务：`三藏`。启动时匹配到它就说明身上已经有五倍任务，
    可以跳过接任务，直接回到任务追踪绿字流程。
- `images/template/task/wubei_tracker_combat_green.png`
  - 普通五倍任务追踪里直接自动寻路到怪的绿色链接
- `images/template/task/wubei_tracker_probe_first_green.png`
  - 双绿字探查任务的第一条绿色链接
- `images/template/task/wubei_tracker_probe_second_green.png`
  - 双绿字探查任务的第二条绿色链接
- `images/template/bag/wubei_probe_item.png`
  - 双绿字探查任务使用的任务道具
- `images/template/bag/wubei_return_item.png`
  - 战斗完成后回到接任务点的五倍回程道具

原始截图统一留在本目录，使用 `source_*` 命名，避免和正式模板路径混在一起。

暗雷怪分支不走模板匹配。任务名前缀会变化，所以这里只保留 `source_tracker_dark_thunder.png`
作为样本；正式判断直接洗黄字后 OCR 识别 `暗雷怪` 关键词。

探查分支当前规则：

1. 接任务后先定位 `wubei_tracker_anchor.png`，并只截一次任务追踪内容区。
2. 同一张任务追踪截图会同时用于黄字 OCR 和绿字分段；黄字里识别到 `显行`，或绿字分段判断为两段，都会进入探查分支。
3. 匹配第一条探查绿字并点击。
4. 等移动停稳，读取一次当前地图用于日志确认。
5. 在最后一页任务包裹使用探查道具。
6. 如果弹出白字 story 并匹配 `wubei_probe_story_koukou.png`，说明目标已出现，直接查找 tooltip。
7. 如果屏幕内出现通用任务 tooltip：`images/template/npc/npc_task_tooltip.png`，点击 tooltip 进入战斗确认流程。
8. 如果没出现 story/tooltip，再走第二条探查绿字并重复上面步骤。

绿字寻路浮框规则：

1. 点任务追踪绿字后，游戏会短暂显示黄色浮框，例如：`正在自动寻路前往莲花洞(51,32)`。
2. 五倍在窗口 base 相对区域 `[+156,+455] -> [+427,+489]` 采样这个浮框，洗黄字后 OCR。
3. 解析规则是从 `前往` 后读取地图名，到第一个括号前结束；括号内读取目标逻辑坐标。
4. 解析结果只保存到本轮五倍上下文，用来做“已经到目的地但进入战斗框没弹出”的 tooltip 兜底确认。
5. 这一步只做截图、洗图和 OCR，不发送鼠标键盘输入；它不应该占用物理输入权。

连续战斗特殊怪当前规则：

1. 接任务后先洗任务追踪黄字并 OCR；只有黄字里识别到 `黄袍`，本轮才启用连续战斗检测。
2. 普通五倍怪战斗结束后直接尝试五倍回程道具。
3. 黄袍分支不再扫战斗中的右上角 marker；战斗中看到黄袍不能证明战后仍需要续打。
4. 战斗结束后重新读取左侧任务追踪黄字：还有 `黄袍` 就继续点任务追踪绿字，不先回城。
5. 如果战后任务追踪没有 `黄袍`，再尝试五倍回程道具。
6. 本轮最多连续进入 5 次战斗；达到上限后如果仍不能回程，则该轮失败，后面再接导航回宝象国的 fallback。
