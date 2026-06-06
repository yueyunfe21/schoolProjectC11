# 五倍任务追踪面板测试图

这里保存五倍任务追踪面板相关的原生截图，用于后续回放和验证五倍任务追踪识别、目的地提示识别等逻辑。

来源：

- `images/temp/**/wubei_tracker_panel_*_raw.png`
- `images/temp/**/wubei_tracker_destination_hint_*_raw.png`

目录：

- `raw/`：平铺保存所有五倍任务追踪原图，不按 hwnd 或截图类型创建子目录。

约束：

- 这里只收 `raw` 原图，不收 `yellow`、标注图或派生调试图。
- 因为不同 hwnd 目录里有同名文件，`raw/` 里的文件名前缀会带来源 hwnd，例如 `hwnd-130B2C__...png`。
- `manifest.csv` 记录目标文件、来源分类、原始来源、大小和原始修改时间。
