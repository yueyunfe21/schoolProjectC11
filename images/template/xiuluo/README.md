# 修罗模板源图

这里放修罗任务的原始截图。正式运行使用 `images/template/` 下的洗图模板，不直接使用这里的源图。

## 源图文件

- `accept_dialog.png`: 灵兽村使者正常接任务对话，包含“闲来无事，要我帮忙吗”和“我想取消任务”。
- `under_five_dialog.png`: 队伍不足 5 人确认对话，包含“确定”和“我再想想”。
- `enter_battle_dialog.png`: 点击修罗后进入战斗确认对话，包含“看打！”。
- `return_item.png`: 修罗回城道具图标。
- `objective_story_example.png`: 接任务后 story 文本样例，只作为解析目标格式的参考。

修罗点怪主路径使用通用 NPC 标签模板：

- 源样本：`images/template_sources/common/npc_menu_clean_sample.png`
- 输出模板：`images/template/npc/npc_tag.png`

运行时会在 Ctrl 菜单洗黄图里匹配所有 `(NPC)` 候选，并逐个点击，直到出现并点击“看打！”确认项。
`嗜血修罗 (NPC)` 黄字截图本身不需要作为专用模板；如果以后遇到同屏多个 NPC 且通用标签误点率高，再补任务专用名字模板。

## 生成模板

```powershell
javac scripts\BuildXiuluoTemplates.java
java -cp scripts BuildXiuluoTemplates
```

输出：

- `images/template/dialog/xiuluo_accept_xianlaiwu.png`
- `images/template/dialog/xiuluo_cancel_task.png`
- `images/template/dialog/xiuluo_underfive_confirm.png`
- `images/template/dialog/xiuluo_underfive_wait.png`
- `images/template/dialog/xiuluo_enter_battle_kanda.png`
- `images/template/item/xiuluo_return_item.png`
- `images/template/npc/npc_tag.png`
