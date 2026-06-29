# 远程诊断 Case 上报设计

日期：2026-06-27

## 背景

DHXY 目前主要依赖本地 `logs/dhxy-console.log` 和运行报告定位任务问题。开发者本机可以直接查日志，但其他用户本地运行时，如果某一轮修罗、五倍或五环失败、卡住或出现异常延迟，维护者通常拿不到那段日志。

第一版目标不是做完整云端日志平台，而是让客户端自动整理“可疑 case”，把小体积结构化 JSON 上传到维护者可访问的位置。

## 推荐架构

使用独立 Cloudflare Worker + R2：

```text
DHXY 客户端
  -> 本地生成 case.json
  -> POST /api/case/upload 到 dhxy-case-worker
  -> Worker 校验 token/license/schema/大小
  -> Worker 写入 R2
  -> Worker 更新轻量索引
  -> 维护者网页读取索引和 case JSON
```

不复用 `dhxy-license-worker` 存 case 正文。现有授权项目是 Worker + D1，适合授权码、设备绑定和事件记录；诊断 case 包含日志片段，体积和生命周期都更适合 R2。

## 新项目边界

建议新建 sibling 项目：

```text
D:/mavenProject/dhxy-case-worker
```

职责：

- 提供 `/api/case/upload`。
- 校验客户端上传权限。
- 校验 `case.json` schema/version/大小。
- 写入 R2 object。
- 维护按日期的索引 JSON。

不负责：

- 授权码发放。
- 修改现有 license D1 表。
- 分析日志内容。
- 直接接收任意大文件。

## 客户端 Case JSON

每个 case 是一个独立 JSON 文件，先落本地，再异步补传。

建议本地路径：

```text
logs/cases/YYYY-MM-DD/<task>_<playerId>_<round>_<timestamp>.case.json
```

建议字段：

- `schemaVersion`
- `caseId`
- `createdAt`
- `taskType`: `xiuluo_v2` / `wubei` / `wuhuan_v2`
- `caseType`: `failed` / `timeout` / `slow_latency` / `stopped_with_open_issue` / `unexpected_exception`
- `severity`: `info` / `warning` / `error`
- `window`: window id, hwnd, title snapshot
- `player`: player name, player id when known
- `run`: taskRunId, roundId, phase, maxRuns
- `timeRange`: logStart, logEnd
- `latency`: known route/combat/return/recovery/maintenance durations
- `summary`: short human-readable summary
- `logExcerpt`: selected relevant lines only
- `app`: app version, build time, git commit if available
- `environment`: anonymous machine hash, OS/JVM summary

第一版不内嵌截图。以后如果确实需要图，再扩展为：

```json
{
  "attachments": [
    {
      "type": "marked-image",
      "r2Key": "attachments/...",
      "sha256": "..."
    }
  ]
}
```

## 触发时机

本地生成 case：

- 单轮任务失败。
- 任务 phase 超时。
- 关键链路耗时超过阈值，例如导航、进战斗、脱战回程、战后恢复。
- 未预期异常被单轮隔离捕获。
- 用户手动停止时，如果停止前存在 open/review CR 关注点或 pending anomaly。

上传：

- 任务自然结束时上传当天未上传 case。
- 用户手动停止时上传当天未上传 case。
- 下次启动时补传上次未成功上传的 case。

上传失败不能影响游戏任务，只能记录本地状态，等待下次补传。

## 安全与隐私

- 客户端不能持有 R2 写入密钥。
- Worker 是唯一写 R2 的组件。
- Worker 至少校验一个 upload token；更好的方式是结合 license code/device hash 或签发短期 upload token。
- 默认不上传完整日志。
- 默认不上传真实机器名、Windows 用户名、本地路径。
- 机器标识只传 hash。
- 单个 JSON 需要大小上限，第一版建议 1MB 或 2MB。

## 后续网页

第一版可以只提供 R2 对象和索引。后续再做一个轻量网页：

- 按日期筛选。
- 按任务类型筛选。
- 按 case 类型和严重级别筛选。
- 点开查看摘要、关键日志、耗时和原始 JSON。

这个网页可以是 Cloudflare Pages 静态页，也可以和 case Worker 共用只读接口。

## 验收标准

- 本地能生成 `case.json`，不依赖网络成功。
- Worker 能拒绝非法 token、过大 payload、错误 schema。
- Worker 能把合法 case 写入 R2。
- 客户端停止/自然结束/下次启动补传路径都不会阻塞任务线程。
- 上传失败有本地 retry 状态。
- 维护者能按日期找到上传 case。

## 非目标

- 第一版不做自动 AI 分析。
- 第一版不上传完整 `dhxy-console.log`。
- 第一版不上传 zip。
- 第一版不改任务业务决策。
- 第一版不把诊断正文存进 D1。
