# DHXY Codex 维护交接（2026-08-25）

## 新会话恢复提示词

> 请在 `D:\mavenProject\DHXY-cr271` 的 `dev` 分支继续。先读 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、本交接和 G105。先运行 keep-codex-fast 只读报告，核对退出后维护日志与计划任务 `CodexHealthWatch-ReadOnly`；不得修改或清理基线工作树 `D:\mavenProject\DHXY`，不得覆盖当前大量未提交的用户/其他卡改动。

## 当前基线与边界

- 活跃工作树：`D:\mavenProject\DHXY-cr271`
- 分支：`dev`
- 本轮开始时 HEAD / `origin/dev`：`5e05a957`
- 工作树已有大量无关未提交改动；G105 只触及规则、维护脚本和卡片文档，禁止 broad reset/checkout。
- G102 已源码交付、待五窗 fresh；G103 五审仍有 P1/P2，禁止 fresh；G104 源码/Cloud 编译/真实回放通过，待 fresh。

## G105 已查明事实

- D 盘刷爆并非 Codex 自身“索引器”：现场捕获到 Codex 子 PowerShell 从仓库根执行无边界 `Get-ChildItem -Recurse -File ...`，遍历整个工作树后才过滤日志名。
- 复现时 Codex 进程组磁盘吞吐约 156–171 MB/s，内存继续上升；命令结束后 D 盘队列回落。
- pagefile 在 `H:\pagefile.sys`，所以当时 D 盘饱和主要是实际文件枚举，不是 D 盘换页。
- keep-codex-fast 只读报告：active sessions 55.350 GB、archived sessions 162.588 GB、71 个超过 10 天的候选约 49.845 GB、日志约 1.355 GB、82 个 active threads、41 个 metadata repair 候选、1227 个扩展 cwd 路径。

## 已落地与待复查

- 全局与项目 `AGENTS.md` 增加禁止无边界递归扫描规则。
- `scripts/watch-codex-health.ps1` 只读采样 Codex 私有内存、D 盘队列和危险递归子命令；只告警，不杀进程。
- Windows 计划任务 `CodexHealthWatch-ReadOnly` 已注册并处于运行状态，登录时隐藏启动该监控。安装后烟测：private 5.27 GB、D queue 0、危险递归子进程 0。
- 完整维护前备份：`C:\CBK\kcf-20260825-1007`。另有一次因 Windows 长路径失败的、不完整备份目录：`C:\Users\Yunfeng Yue\Documents\Codex\codex-backups\keep-codex-fast-20260825-100651`，未自动删除。
- 一次性任务 `CodexMaintenanceAfterExit-20260825` 已运行并等待 Codex 退出；之后把超过 10 天的 active session 移到 archive、轮转大日志、修复 metadata/path。全部是先备份再移动，不删除会话。维护 stdout/stderr 位于 `C:\CBK\kcf-apply-20260825\`。

## 新会话第一轮复查

1. 查看 `C:\CBK\kcf-apply-20260825\maintenance.stdout.log` 与 `.stderr.log`。
2. 再运行 keep-codex-fast report-only，比较 active sessions、logs、metadata repair、extended cwd 数量。
3. `Get-ScheduledTask -TaskName CodexHealthWatch-ReadOnly` 确认状态；运行监控脚本 `-Once` 查看快照。
4. 把结果写回 G105 和 `docs/ACTIVE_WORK.md`；若 G105 状态变化，刷新 `docs/cr-dashboard-data.js`。
