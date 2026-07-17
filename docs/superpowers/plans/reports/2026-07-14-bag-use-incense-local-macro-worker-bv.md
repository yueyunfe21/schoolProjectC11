# Internal Worker BV - BAG_USE_INCENSE closed local macro

## 2026-07-14 06:49:51 -04:00 - CLAIMED

- Worker: `Internal Worker BV`，只做实现，不做 reviewer。
- 基线：DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f`；用户批准口径为
  `无已批准业务差异；按基线等价迁移`。
- 简化路线：复用既有 `LOCAL_MACRO / BAG_RETURN_ITEM` 的 Cloud/DHXY typed request/outcome、
  strict codec、digest、local handler 与 schema 结构；新增 closed kind `BAG_USE_INCENSE`，请求不携带
  template path，本地固定调用 `BagService` 使用 `bag/sheyaoxiang_item.png`。
- 结果边界：仅 `EXECUTED` 携带严格 typed `USED / NOT_FOUND`；transport terminal 继续只允许
  `EXECUTED / NOT_EXECUTED / STOPPED / UNKNOWN`。
- 明确不做：不接业务 caller/host，不改 `PlayerStateService`、`TeamReturnService`、`DecisionEngine`，
  不新增 owner/session/ledger/TTL/retry，不改 quiet-period 或是否需要用香判断，不启动 runtime，
  不新增/运行测试，不做 Git mutation。
- 工作树保护：两仓均有大量既存 dirty/untracked 与并行写入；只做下列精确写集内的定点 patch，
  不 reset/checkout/clean/delete，不整文件覆盖。

### 精确写集 - DHXY

1. `docs/superpowers/plans/reports/2026-07-14-bag-use-incense-local-macro-worker-bv.md`（本 append-only 报告）
2. `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`
3. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java`
4. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroCommandPayload.java`（New）
5. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroResultPayload.java`（New）
6. `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagUseIncenseMacroCommandPayload.java`（New）
7. `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagUseIncenseMacroResultPayload.java`（New）
8. `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagReturnItemMacroCommandPayload.java`
9. `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagReturnItemMacroResultPayload.java`
10. `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
11. `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
12. `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
13. `src/main/java/com/bot/dhxy/service/BagService.java`

### 精确写集 - dhxy-cloud-brain

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroKind.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroCommand.java`（New）
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/BagReturnItemMacroCommand.java`
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/BagUseIncenseMacroCommand.java`（New）
5. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/BagUseIncenseMacroResult.java`（New）
6. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroRequest.java`
7. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroOutcome.java`
8. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandEnvelope.java`
9. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
10. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`
11. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java`
12. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java`
13. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java`
14. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`
15. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`
16. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java`

### 结构核对结论

- 既有 Cloud facade/executor/gate/request 被 `BagReturnItemMacroCommand` 具体类型硬编码；为让第二个 closed
  kind 可由 Cloud typed 调用，最小兼容改法是在 remote package 引入 sealed `LocalMacroCommand`，保留
  `bagReturnItem` accessor，同时增加 `bagUseIncense` variant。该改动不触碰业务 caller。
- DHXY handler 已有 remote-exclusive 单队列边界；`BagService` 只需增加一个必须运行在 input worker 内、
  无参数且固定模板的 macro entry，直接复用现有 `interactWithItemExclusive(..., ItemAction.USE, ...)`。
- 未发现必须越界触碰用户列出的停止文件，任务可继续实施。

## 2026-07-14 07:00:13 -04:00 - IMPLEMENTED / COMPILE PASS

### 实现结果

- Cloud 新增 sealed `LocalMacroCommand` 与无字段 `BagUseIncenseMacroCommand`；request typed tree 仅新增
  `bagUseIncense:{}` variant，flat wire 请求只有 `macroKind=BAG_USE_INCENSE`，没有 template/source/path。
- DHXY strict codec 按 `macroKind` 分派两种 closed command；`BAG_USE_INCENSE` 只接受唯一字段
  `macroKind`，额外字段直接 `INVALID_REQUEST`。
- DHXY local handler 复用既有 `submitRemoteExclusiveAndWaitDetailed(...)`，在同一个 input worker callback
  内仅调用一次 `BagService.runUseIncenseMacroDirectForExclusive(null)`，没有 queue-in-queue。
- `BagService` macro entry 无业务参数，固定 `bag/sheyaoxiang_item.png`，直接复用 committed
  `interactWithItemExclusive(MAIN_BAG, ..., null, ItemAction.USE, context)`；未改变页序、匹配、点击、延迟、
  关闭包裹或返回语义。
- 只有 callback 完成才返回 transport `EXECUTED`，并携带 typed `USED` 或 `NOT_FOUND`；未开始、停止、
  已开始但结果不确定继续分别为 `NOT_EXECUTED`、`STOPPED`、`UNKNOWN`，三者 typed result 均为空。
- 未接业务 caller/host；未修改 `PlayerStateService`、`TeamReturnService`、`DecisionEngine`；未启动 runtime；
  未新增或运行测试；未做 Git mutation。

### 基线与业务边界复核

- DHXY HEAD/批准基线：`0114604e1ff5f15491d2910959c45252e893d04f`。
- 已用 `git show 0114604e:.../BagService.java` 核对 `interactWithItemExclusive(...)`，新 entry 仍走相同
  main-bag 全页查找与 `ItemAction.USE` core。
- 已用 `git show 0114604e:.../PlayerStateService.java` 核对既有固定模板为
  `bag/sheyaoxiang_item.png`；未改 quiet-period、状态解释、是否需要用香或 outcome 上报。
- 已核对 `docs/业务逻辑.md` 的业务基线使用规则与“五倍接任务后预走路与移动中准备规则”：摄妖香检查
  的 phase/时序不属于本宏，保持不变。
- **无已批准业务差异；按基线等价迁移。**

### 编译门禁

- Cloud：`D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q compile`，exit `0`（约 4.0s）。
- DHXY：`D:\mavenProject\DHXY` 执行 `mvn -q -DskipTests compile`，exit `0`（约 22.3s）。
- 两条命令均未使用 `clean`；未运行测试；未启动 application/server/host/Task/poller/UI/capture/input。

### 最终文件 SHA-256 - DHXY

| 文件 | SHA-256 |
|---|---|
| `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | `679c7e9e0e0b32f8767c7231efc7bd1b552e130aa467ab2709bda7fea3af38b0` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java` | `98f6c54266c321e31c25f6687355dbd6670879c3ac2ec39d6ed75457192b3246` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroCommandPayload.java` | `00114346b659818276779db3fd5e608ff527114ed47e22461873d3befff09ba4` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroResultPayload.java` | `4ff025f9ddce205b12d2fcc3f434ec4139131c69271fbfff5ac777da3a057883` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagUseIncenseMacroCommandPayload.java` | `a9cbe2be563e5f462dc1c31525a810609b67826fe9b99d901204e15846558d18` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagUseIncenseMacroResultPayload.java` | `bc97254a85c6f11dc50649efefb41477b2dddd64545b3dc6609978efd363131d` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagReturnItemMacroCommandPayload.java` | `94c59edf9b6ccc831057e01b6aa5ac5613c0159054630b05ccc382b01c5a88dd` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteBagReturnItemMacroResultPayload.java` | `f695250af56371c98734966b34a6de0595227ac6ea2d563799f409959361d0e4` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java` | `369c2ffc74fc0cbedf0e8fb6108c9f0461828e6ece9c85d1fd1413d96793fdbf` |
| `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java` | `04db93a2cf45b1a106452b83f0bbe2a445dbc4a866bb977f947a29c8029e4554` |
| `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` | `df7b7d0e1d140d86e1c0d63efe5b7962c518fca0dd4d6e9b5da8381905295d30` |
| `src/main/java/com/bot/dhxy/service/BagService.java` | `154d1a7fbd7cf0d7ca9c51b9eeccd63aca94fcc62779ecfaebf0dcee87cfba44` |

本报告自身为 append-only 文件；最终 self SHA 在 append 完成后计算并由交付消息给出，避免把 self SHA
写回文件后再次改变该 SHA。

### 最终文件 SHA-256 - dhxy-cloud-brain

| 文件 | SHA-256 |
|---|---|
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroKind.java` | `97c8903d3c403a7b5daf720c7323df77e66d2babde604dc3b48ee4d1b016f60d` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroCommand.java` | `98ec4a08c31fd8af7a6382b424748d58022b3581ec286c6a4689d9ee7ec9c4c7` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/BagReturnItemMacroCommand.java` | `6437fd25cdab886afad4241ee8cce1a104458b52431fed4bccc6b19bdc0ebc75` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/BagUseIncenseMacroCommand.java` | `40cad3c0536b2be2d65c4270cfc3672d4eed6c714b76d99316a6cdeaf4eac46a` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/BagUseIncenseMacroResult.java` | `7ab2018e54071bcaf7f3afd4ec1e036ef5a994c82b66edfcd3658e91e39b42b2` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroRequest.java` | `2970b85c8ef1af9890d324668b0df737ab1fb7cb0d257e4c6de1cdf7968e7857` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroOutcome.java` | `3ddb738b2d9bb7ee90f7049fda23fb9ec5035a3316a3f9614c8b3df8f7ea0e49` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandEnvelope.java` | `9ef619ea76e354b04ad74e15baa7af466d5adc13c7844ec98b654a14435584c6` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java` | `47dbce966a6d11b62a64143347f16cf541061cde83d5f2cfa014e86c8d5d6109` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java` | `78b73263a231dd1730b215542308be8359a2b8269fd20706c4b7a56e184998ec` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java` | `34ef38528f7e0f3f1690688296515d0775b15d7081f394cec7db549cea314875` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java` | `0c5bc991665a869d3515701eeada15c500c66aa4ae5ab81ff7c029ad353e0459` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java` | `14e52f56a84c4b2a2d3c16e25c95a025211df1228a0b7de1c8fd47e7f4f6303d` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java` | `cc8e8256853bc1310d5d92f830267542fe0ecb2e733d3bb9baa6c75b86bed3c9` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java` | `6c6e3610ad37163c22d8edc0a34ca4f45c458264b3a61f9cf27df673e904e9ce` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java` | `86d351d2ee2ebec10b44038e643a7257be38dce3768f0986e8540bce78795717` |

### 交付状态

`IMPLEMENTED / COMPILE PASS / READY FOR EXTERNAL REVIEW`。Internal Worker BV 不作 reviewer 结论。

## Parent Source Review #1 - 2026-07-14T07:08:00-04:00

**SOURCE APPROVED，P0/P1/P2=0；consolidated fresh 双仓门待本轮所有 Java writer 稳定后统一执行。**

父级逐层读取 Cloud request/outcome/envelope/digest/gate/executor/service-port/broker 与 DHXY strict codec/digest/
handler/`BagService`：

- `BAG_USE_INCENSE` 是 closed enum + sealed command/result variant；Cloud 请求 canonical tree 仅新增
  `bagUseIncense:{}`，flat wire 只含 `macroKind`，不允许 Cloud 传 template/path。
- 所有 terminal flat payload 恰为 `macroKind/operation/state/cachePoint` 四键；`EXECUTED` incense 只允许
  `USED/NOT_FOUND` 且 operation/cachePoint 显式 null，`NOT_EXECUTED/STOPPED/UNKNOWN` 三个 typed 字段显式 null；
  `OBSERVED` 与未知 enum 均在 digest 前拒绝。Cloud/DHXY canonical tree 形状一致。
- 本地 handler 继续只通过既有 `submitRemoteExclusiveAndWaitDetailed(...)` 进入单一输入队列；callback 内直接调用
  `BagService.runUseIncenseMacroDirectForExclusive(null)`，没有 queue-in-queue。Bag 入口固定
  `bag/sheyaoxiang_item.png` 并直接复用 committed `interactWithItemExclusive(MAIN_BAG,...,USE,...)` 的页序、匹配、
  点击、delay、关闭包裹与返回语义。
- 未接 caller/host，未修改是否用香、quiet-period、状态解释或 outcome 后续业务；未新增 per-Service
  owner/permit/session/ledger/TTL/retry。共享 retained identity 仅复用既有 LOCAL_MACRO 通道。

Worker Cloud `mvn -q compile` 与 DHXY `mvn -q -DskipTests compile` 均 exit 0。源码门通过；本切片是本地机械宏
基础设施，完整 PlayerState Cloud 编排尚未接通，暂不增加 `189/407`。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**
