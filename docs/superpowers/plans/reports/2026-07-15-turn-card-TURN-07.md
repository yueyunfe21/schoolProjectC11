# TURN-07 DHXY Template Cache Worker Report

## CLAIMED

- 领取时间：`2026-07-15T14:58:11-04:00`
- 身份：TURN-07 implementation Worker；不是 manager/reviewer，不得自批。
- 状态：`CLAIMED / IMPLEMENTING`
- 基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- startDependsOn：`TURN-03B`、`TURN-06` 均已父级 `SOURCE APPROVED / BUILD COHORT PENDING`。
- approvalDependsOn：`TURN-01D`；本 Worker 不越过该审批依赖。

### 精确写集

- Java：`src/main/java/com/bot/dhxy/cloud/turn/TurnTemplateCache.java`
- 报告：`docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-07.md`
- Cloud 全仓只读；不修改主计划、CR271、dashboard、Maven、protocol、TURN-06 或其他 Worker 写集。

### 实现合同

- 只接受固定 wire key `images/template/...png` 与目标 `contentHash`。
- 现有本地文件 SHA-256 精确命中时直接返回，不发网络请求。
- 缺失或过期时只调用一次 `TurnClient.downloadTemplate(...)`；存在旧文件时带该本地内容的 SHA ETag 做条件 GET。
- `200` 必须重新校验 raw PNG 解码、SHA-256 与目标 hash，随后同目录临时文件原子替换。
- `304` 仅在当前目标文件仍存在且重新计算 hash 精确命中时接受，不能掩盖缺失、损坏或竞态替换。
- 模板目标必须保持在注入的既有模板根内；拒绝绝对路径、`..`、反斜杠/编码绕过及非 `.png`。
- 无重启、数据库、目录同步、后台刷新、自动 retry 或业务语义变更。

### 主要风险

1. wire key 含百分号编码或平台分隔符时可能形成路径绕过，必须在落盘前 fail-closed。
2. 本地文件在 hash 检查与 `304` 返回之间可能被替换，故 `304` 后必须重新读取并验证当前文件。
3. 下载内容即使 SHA/签名看似正确仍可能不是可解码 PNG，必须使用 `ImageIO` 完整解码确认。
4. 原子移动在文件系统不支持时不得静默降级为非原子覆盖；应返回 typed transport/config failure。

### 领取时仓库状态摘要

- DHXY 分支：`thin-client-design`，存在大量既有 dirty/untracked；全部保护。
- Cloud 分支：`navigation-migration`，存在大量既有 dirty/untracked；本卡全仓只读。
- 无已批准业务差异；按基线等价迁移。

## SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15T15:00:57-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- `countDelta=0`

### 实现结果

- `TurnTemplateCache(Path, TurnClient)` 把配置的既有模板根解析为真实绝对目录；返回路径始终限制在该根内。
- `resolveTemplate(templateKey, contentHash)` 精确消费冻结协议的 64 位十六进制 hash，并归一化为 lowercase：
  - 本地 regular non-symlink 文件 SHA 命中且 PNG 可解码时直接返回，网络调用为零；
  - 缺失时无条件下载一次；过期时以本地实际 SHA 构造精确 quoted ETag，条件下载一次；
  - `304` 后重新读取当前目标文件，只有目标 SHA 仍命中且 PNG 可解码时才接受；
  - `200` 同时核对 `TurnTemplateDownload.sha256()`、raw PNG 重算 SHA 与 payload 目标 hash，并验证 PNG
    signature 和 `ImageIO` 解码，之后才同目录临时写入及 `ATOMIC_MOVE + REPLACE_EXISTING`；
  - 替换后再次计算已安装文件 SHA 并验证 PNG 可用性。
- key 校验固定为 `images/template/...png`，拒绝空段、`.`、`..`、反斜杠、百分号编码、冒号、query、fragment、
  NUL、非 `.png`、绝对/归一化逃逸及现有 ancestor 的 real-path 越界。
- 缓存只处理本次 payload 指定的一个模板；未加入扫描、manifest/folder sync、数据库、后台线程、刷新器或 retry。
- Cloud 仓全程只读；未修改 TURN-03B、TURN-06、protocol、Maven、Service、Task、runner、主计划、CR271 或 dashboard。

### SHA-256

```text
14403D2EBB89517D5371C5B47BDB568ECFF471F78C2FFC20CE64E32095358A90  TurnTemplateCache.java
```

### Scoped grep / diff-check 证据

- scoped status 仅列出：
  - `?? src/main/java/com/bot/dhxy/cloud/turn/TurnTemplateCache.java`
  - `?? docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-07.md`
- `downloadTemplate(` 调用点为 `1`；`ATOMIC_MOVE` 为 `1`。
- `Files.list`、`Files.walk`、`DirectoryStream`、`ExecutorService`、scheduler API、Base64、WebSocket、raw Socket
  与 retry loop 调用均为 `0`。
- `git diff --no-index --check -- NUL <file>` 对 Java 与报告均无 trailing whitespace、space-before-tab 或
  EOF whitespace 诊断；仅有 Windows LF/CRLF 提示。
- 与刚落地的 `TurnProtocolValidator.requireSha256(...)` 对照：validator 接受 64 位大小写十六进制；本缓存
  同样接受并在比较/ETag 前归一化，未形成 validator 通过而缓存拒绝的协议裂缝。

### 未运行项

- Maven：未运行。当前仍有并行 Java writers，遵守 build cohort 门禁。
- Tests：未创建、恢复或运行 automated/replay/source-guard test。
- Runtime：未启动 DHXY/Cloud application、server、Task、poller、UI、capture 或 input；未发送网络请求。
- Git：未 stage、commit、checkout、reset、clean、删除或覆盖任何既有 dirty/untracked。
- 业务差异：无已批准业务差异；按 `696a12b0` 基线等价迁移。

### Worker 停止点

TURN-07 源码已交付。当前 Worker 不是 manager/reviewer，不写 `APPROVED/CLOSED`，不领取下一卡，等待父级
独立源码审查与后续 cohort Maven。

## Parent Source Review #1

- 审查时间：`2026-07-15T15:14:00-04:00`；父级独立逐行审查 `TurnTemplateCache.java`。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`。
- 证据：wire key/root/ancestor 路径限制 fail-closed；本地 SHA 命中零网络；缺失/过期只发一次条件 GET；
  `304` 重新核对当前目标 hash 与 PNG；`200` 同时核对 typed SHA、raw bytes SHA、PNG 解码与 payload hash；
  安装仅允许同目录 `ATOMIC_MOVE + REPLACE_EXISTING`，不静默降级；无目录同步、后台刷新或 retry。
- 影响：源码 owner 释放，`TURN-08B` 的 start dependency 已满足，可立即并行领取。
- Maven：writers 活动中，待 cohort 汇合后由父级执行；当前不写 CLOSED。
- 业务差异：无已批准业务差异；按 `696a12b0` 基线等价迁移。
