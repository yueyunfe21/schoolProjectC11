# DHXY UI 设计记录

日期：2026-06-24

本文记录当前确认的 DHXY JavaFX 主控页 UI 方向。这里是视觉与交互约定，不改任务业务逻辑。

## Mock

当前主控页 mock：

![DHXY 主控页 mock](design-audit/mock-main-control-v1.png)

视觉基准文件：

- `docs/design-audit/mock-main-control-v1.png`
- `docs/design-audit/mock-main-control-imagegen-reference.png`

注意：以这张 ImageGen mock 为准。不要使用临时 HTML 复刻稿作为视觉基准；HTML 复刻稿和最终确认图存在明显风格差距。

当前真实界面截图参考：

- `docs/design-audit/02-main-window-complete.png`

当前 JavaFX 本地 mock 预览：

![DHXY JavaFX mock 预览](design-audit/mock-javafx-main-control-preview.png)

本地运行命令：

```bash
mvn -q org.codehaus.mojo:exec-maven-plugin:3.1.0:java "-Dexec.mainClass=com.bot.dhxy.ui.mock.DhxyMainWindowMockApp"
```

说明：

- 该 mock 入口是 `src/main/java/com/bot/dhxy/ui/mock/DhxyMainWindowMockApp.java`。
- 样式文件是 `src/main/resources/styles/dhxy-main-window-mock.css`。
- 该 mock 不接 Spring、不接窗口注册、不接任务启动、不发送任何输入。
- 正式 UI 仍然是 `MainWindowController`。确认 mock 后，再迁移布局和样式到正式 UI。

右上角顶栏参考：

![右上角顶栏参考](design-audit/topbar-reference.png)

## 主控页布局

- 左侧保留窄导航：`主控`、`设置`、`验证`、`调试`、`日志`、`说明`。
- 左下角显示版本号，例如 `版本 v1.0.0`。
- 左下角提供 `更新` 按钮，点击后检查是否有新版本。
- 左下角不要放装饰图标或无实际用途的标志。
- 顶部标题保留 `DHXY Robot 控制台`。
- 右上角快捷提示采用参考图风格：
  - `暂停 Ctrl+Shift+F11`
  - `/`
  - 红色强调 `紧急停止 Ctrl+Shift+F12`
  - 月亮图标 + `深色模式` + 开关。

## 指标区

主控页顶部指标只保留必要信息：

- `窗口`
- `运行中`
- `异常`

不要显示：

- `可接任务`
- `可移绑定`
- `后台截图 开`
- `后台键盘 开`
- `物理输入队列 空闲`
- `运行模式 多窗口`

这些技术状态可以留在调试页或日志里，不放在主控首屏。

## 窗口工具条

窗口与任务区域的工具条保留：

- `刷新窗口表`
- 筛选：`全部`
- 搜索框：`搜索角色 / ID / 服务器`
- `停止全部`
- `取消选择`
- `全选`
- `启动`

工具条不要放：

- `暂停`
- `停止所选`

原因：

- 暂停由表格行内的启动按钮切换状态完成。
- 停止单个窗口由表格行内停止按钮完成。

## 表格操作列

表格 `操作` 列只做单窗口运行控制，不放详情入口。

- 表格首列必须是窗口勾选框。
- 主控页表格至少预留 6 个账号/窗口行的视觉空间。
- 即使当前只有 1-2 个窗口，也保留空行，让用户知道这里是多窗口工作区。
- 空闲窗口：显示 `启动` 操作 + `停止` 操作。
- 运行中窗口：左侧操作变成绿色 `暂停`，右侧仍是红色 `停止`。
- 不显示 `详情` 按钮。

如果后续需要详情，优先使用选中行后的详情面板或双击行，不占用 `操作` 列。

## 任务选择

任务选择区保留卡片式选择：

- `五环`
- `五倍`
- `修罗`
- `自动战斗`
- `坐标调试`
- `地图校准`
- `导航压力测试`

每个任务卡片显示：

- 任务名
- 简短分类，例如 `日常`、`任务`、`挂机`、`诊断`
- 次数/时长 chip，例如 `2轮`、`100次`、`60分`、`手动`、`2点`、`5点`

选中任务应有明确蓝色边框和顺序角标。

## 风格

- 桌面工具工作台风格，清爽、紧凑、专业。
- 不做营销页、英雄区、装饰插图、渐变光斑或复杂背景。
- 主色使用克制蓝色。
- 停止/危险动作使用红色。
- 运行中暂停状态使用绿色。
- 控件圆角控制在 6-8px。
- 主要字体保持 `Microsoft YaHei UI` / `Segoe UI`。

## Mock 比例迭代

当前 JavaFX mock 预览图：

- `docs/design-audit/mock-javafx-main-control-preview.png`

2026-06-24 调整方向：

- 顶部 `窗口 / 运行中 / 异常` 不再做大展示卡片，改成 70px 高的紧凑状态卡。
- 顶部指标图标和数字降低视觉权重，避免抢占窗口表注意力。
- `窗口与任务` 表格保持主视觉，固定预留 6 行账号/窗口空间。
- `选择任务` 改成低矮任务选项区，卡片高度约 66px，图标和 chip 都收小。
- 区块标题从大标题降为工作台标题，整体密度更接近桌面控制台。

2026-06-24 图标调整：

- `五倍` 任务图标使用 `5x` 文本标识，不使用单独的叉号。
- 修罗怪物图标候选预览：`docs/design-audit/mock-xiuluo-monster-icon-options.png`。
