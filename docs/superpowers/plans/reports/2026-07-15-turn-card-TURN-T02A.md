# TURN-T02A Cloud Exchange/Ingress Contract Tests

## CLAIMED

- 领取时间：`2026-07-15`（America/New_York）。
- 状态：`TEST BLOCKED / SOURCE DELIVERED`。
- 角色：CR271 Worker F，唯一负责 `TURN-T02A` Cloud exchange/ingress tests；父级是唯一 reviewer，本 Worker 不自批。
- 权威计划：`docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 19 节。
- 协议规格：`docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
- 业务差异：`无已批准业务差异；按基线等价迁移`。本卡只验证 transport/exchange 合同，不改变业务语义。
- 运行边界：只运行本卡点名的三个 JUnit test class 与 Cloud compile；不启动 application/server/Task/runtime、桌面输入、截图或 OCR。
- Git 边界：不切分支、不 stage、不 commit、不 clean、不回滚，不覆盖两仓任何既有 dirty/untracked。

## Exact Write Set

1. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnExchangeContractTest.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnExchangeFrameResultContractTest.java`
3. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnHttpHandlerContractTest.java`
4. 本报告。

禁止修改 production、`src/test/resources/cloud-turn/v1/frame-2x2.png`（TURN-T02B 所有）、其它测试、Maven/config、主计划、CR271、`ACTIVE_WORK` 或 dashboard。

## Claim-Time Baseline

### DHXY

- Branch：`thin-client-design`。
- HEAD：`0114604e`（`docs(thin-client): 全量云端业务大脑目标架构设计工件基线`）。
- 工作区已有大量 modified/deleted/untracked；全部视为父级及并行 Worker 工作并保护。

### Cloud Brain

- Branch：`navigation-migration`。
- HEAD：`3b988ca`（`CR257 复审返修二轮(P1-2): checkHealth 校验 launcher 下发的期望 model fingerprint`）。
- `pom.xml` 已 modified，`src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/` 已 untracked；三份本卡 test class 与 2x2 fixture 在领取时均不存在。
- 最新 pushed tree 不含 Cloud turn production/test 路径；当前 turn production 属于并行未跟踪交付，测试按已批准 spec/计划合同编写，最终执行前必须重读 `TURN-02R` 与 `TURN-40A` 最终交付。

## Frozen Assertions

- `CloudTurnExchangeContractTest`：command-first/wait-first、busy/duplicate/late/interrupt、同一 action 不二次下发且无自动 retry。
- `CloudTurnExchangeFrameResultContractTest`：outcome + raw frame 经同一 future 原子返回、构造/访问防御性复制、latest metadata 替换且不保留图片历史。
- `CloudTurnHttpHandlerContractTest`：JSON-only/multipart、唯一 auth header、坏 SHA/尺寸/PNG/bounds/part count 拒绝、`IDLE` 仍确认 previous outcome、无二次 action/retry。

## Delivery Evidence

## TEST BLOCKED / SOURCE DELIVERED

### Delivered Test Sources

| File | Test methods | Bytes | SHA-256 |
|---|---:|---:|---|
| `CloudTurnExchangeContractTest.java` | 7 | 20821 | `FFF4D7DEB53EFD961348F99C01BDC394C717077ED3AF0564B0E23005918A404B` |
| `CloudTurnExchangeFrameResultContractTest.java` | 4 | 9829 | `9F1F6103A537D1B880D3E1CDA86826B07287C95A1F19EE5E38A30360A61208B7` |
| `CloudTurnHttpHandlerContractTest.java` | 6 | 20937 | `5EE49610BF9652F1F38FD95515E68556A908FBD4E244041E34A6821223011CCD` |

覆盖结果：

- Exchange：command-first、wait-first、busy、duplicate、timeout 后 late outcome、command wait interrupt、HTTP/client wait interrupt；同一 unresolved action 只重发原 payload，不产生替代 action 或自动 retry。
- Frame result：同一 command future 原子返回 outcome + raw PNG；partial/mismatched pair fail-closed；构造和访问边界防御性复制；latest metadata 覆盖完整 `pauseRequested/stopRequested`，完成后 exchange state 不保留 frame/outcome/future。
- HTTP ingress：JSON-only、raw multipart、唯一/错误/重复 auth、JSON/body/frame bounds、坏 SHA、decoded dimensions、坏 PNG、缺/多 part；坏 frame 不消费原 action；有效 previous outcome 即使返回 `IDLE` 也完成并确认，重放仍 `IDLE`，同 actionId 后续为 duplicate 且不 retry。

### Fixture Evidence (TURN-T02B Read-Only)

- `src/test/resources/cloud-turn/v1/frame-2x2.png`
- bytes：`126`
- dimensions：`2x2`
- SHA-256：`0B4B8834D9FA2A0EE891481CD9E90EB8434A680BF92AF684E33D7BD4FB0F8754`
- 本 Worker 只读取并逐字节断言；未创建、修改或替换该 fixture。

### Final Production Re-read

- `TURN-02R` 报告已为 `SOURCE DELIVERED`，父级 source review 为 `P0/P1/P2=0/0/0`；最终
  `CloudTurnCommandResult`/`CloudTurnCommandPort`/`CloudTurnExchange` SHA-256 分别为
  `F54A5B9F...B87F`、`CF2A5397...D4D8`、`86CD29B1...5547`。
- `TURN-40A-PROD` 报告已为 `SOURCE DELIVERED`；Cloud 八个 protocol 文件 SHA 与交付表 8/8 一致，
  `TurnWindowMetadata` 已含 pause/stop，`TurnRequest`/`TurnResponse` 已含 optional start/ack 且保留 Foundation
  compatibility constructors。
- T02A 最终测试只发送 `taskStartRequest=null` 并断言 turn exchange/ingress；未侵入 TURN-T01 lifecycle 所有权。

### Required Command Results

工作目录均为 `D:\mavenProject\dhxy-cloud-brain`，未使用任何 skip/enforcer/IDE 参数：

| Command | Exit | Tests run | Failures | Errors | Result |
|---|---:|---:|---:|---:|---|
| `mvn -q -Dtest=CloudTurnExchangeContractTest test` | 1 | 0 | 0 | 0 | Maven main `compile` 阶段阻断，未到 `testCompile`/Surefire |
| `mvn -q -Dtest=CloudTurnExchangeFrameResultContractTest test` | 1 | 0 | 0 | 0 | 同一 main-source compile 阻断 |
| `mvn -q -Dtest=CloudTurnHttpHandlerContractTest test`（最终字节 fresh rerun） | 1 | 0 | 0 | 0 | 同一 main-source compile 阻断 |
| `mvn -q compile` | 1 | 0 | 0 | 0 | Cloud compile gate 未通过 |

`target/surefire-reports` 不存在，且 target 下无三项 named test execution artifact，确认不是断言失败后误报为
`tests run=0`，而是 Surefire 根本未启动。

### Exact Blocker Evidence

- 每条命令首个 compiler error 均为：
  `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java:[3,25] cannot find symbol: class GameClientTracker in package com.bot.dhxy.core`。
- 同批后续错误来自写集外在途 whole-Service/Task，例如 `TaskTrackerPanelService` 缺
  `TextRecognizer`/`CoordinateHelper`/`OcrWindowScanService`/`WindowScopedTempPath`，以及
  `WubeiTask`、`NavigationService`、`NpcClickService` 等缺 DHXY-local dependencies。
- Maven 输出未指向本卡三份 test source、`CloudTurnExchange*`、`CloudTurnHttpHandler`、
  `TurnMultipartReader` 或 TURN-40A protocol production；由于 main compile 未完成，也没有证据可以声称三份测试已成功编译或执行。
- 归属：Cloud whole-Service/Task migration cohort / 父级整仓 build owner。T02A 不修改这些 production 文件，
  不使用 compiler includes、skip flag、stale classes 或手工旁路来制造绿色结果。
- 复验点：写集外 Cloud compile cohort 恢复后，原样 fresh 重跑上表三项 named tests 和 `mvn -q compile`；
  在四条命令 exit 0 前，本卡保持 `TEST BLOCKED`，等待父级唯一 reviewer 裁决。

### Concurrent Write Note

- 实施期间旧宽范围 `2026-07-15-turn-card-TURN-T02.md` 仍以 `CLAIMED` 列出重叠测试写集，并在本 Worker
  首版完成后改写了 `CloudTurnHttpHandlerContractTest.java`。
- 本 Worker没有回滚该并发改动；逐行保留其有效 JSON/auth/multipart/bounds/SHA/dimensions 断言，只补回 T02A
  冻结缺口（坏 PNG、part count、错误 token、IDLE replay/no retry、invalid frame 后原 action 完成），最终 hash
  见上表。

未修改 production、其它测试、fixture、Maven/config、CR271/dashboard；未启动 runtime/application/server/Task，
未执行 Git mutation。父级仍是唯一 reviewer，本报告不写 `APPROVED`。
