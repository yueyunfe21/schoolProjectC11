# TURN-40A + TURN-T01 Worker B Report

## CLAIMED

- 领取时间：`2026-07-15T18:15:08-04:00`
- 状态：`CLAIMED`
- 角色：CR271 Worker B，唯一负责 `TURN-40A + TURN-T01`；不是 reviewer，不写 `APPROVED/BLOCKED`。
- 权威计划：`docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
- 协议规格：`docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
- 业务差异：无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。
- 禁止边界：不新增 session/owner/permit/ledger/TTL/durable workflow/自动 retry；不启动 application/server/Task/runtime/UI/capture/input；不执行 Git mutation。

## Exact Write Set

以下 production 文件在 DHXY 与 Cloud Brain 的
`src/main/java/com/bot/dhxy/cloud/turn/protocol/` 下各一份，且必须 byte-identical：

1. `TurnTaskCode.java`（create）
2. `TurnTaskQueueFailurePolicy.java`（create）
3. `TurnTaskStartRequest.java`（create）
4. `TurnTaskStartAck.java`（create）
5. `TurnWindowMetadata.java`（modify）
6. `TurnRequest.java`（modify）
7. `TurnResponse.java`（modify）
8. `TurnProtocolValidator.java`（modify）

以下 test 文件在两仓
`src/test/java/com/bot/dhxy/cloud/turn/protocol/` 下各一份，且必须 byte-identical：

1. `TurnCoreProtocolGoldenJsonTest.java`
2. `TurnActionGoldenJsonTest.java`
3. `TurnEnvelopeGoldenJsonTest.java`
4. `TurnProtocolValidatorContractTest.java`
5. `TurnTaskLifecycleProtocolGoldenJsonTest.java`

以下 fixtures 在两仓 `src/test/resources/cloud-turn/v1/` 下各一份，且必须 byte-identical：

1. `action-input-capture.json`
2. `outcome-completed.json`
3. `outcome-failed-with-frame.json`
4. `outcome-stopped.json`
5. `outcome-duplicate-or-uncertain.json`
6. `request-start.json`
7. `response-start-ack-idle.json`

文档写集仅为本报告。禁止修改其它 protocol production、POM、Service、host/server、runner、配置、主计划、
CR271、`ACTIVE_WORK`、迁移矩阵或 dashboard。

## Claim-Time Repository Baseline

### DHXY

- Branch：`thin-client-design`
- HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- `git status --short --branch`：存在大量父级及其它 Worker 的既有 dirty/untracked；全部保护，不处置。

### Cloud Brain

- Branch：`navigation-migration`
- HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- `git status --short --branch`：存在大量父级及其它 Worker 的既有 dirty/untracked；全部保护，不处置。

### Owned Production Baseline SHA-256

两仓领取瞬间完全一致：

| File | Baseline bytes | Baseline SHA-256 |
|---|---:|---|
| `TurnTaskCode.java` | absent | absent |
| `TurnTaskQueueFailurePolicy.java` | absent | absent |
| `TurnTaskStartRequest.java` | absent | absent |
| `TurnTaskStartAck.java` | absent | absent |
| `TurnWindowMetadata.java` | 278 | `4112B3A01BD1F551DE50E7205E4F74324F5DE29504D96A81E3948A7782ABF4A0` |
| `TurnRequest.java` | 203 | `63BAC2E0E402C6E4F09AA57E3AB9A26046F6865C919B07F96C8911609ECE157E` |
| `TurnResponse.java` | 186 | `5A581F6CC6D9ADB639964881B414BF3058CAE7CD480E45D8049ED6CC1823A858` |
| `TurnProtocolValidator.java` | 18670 | `1B4CC9E98B26D822CA44491FC13488118874071B1277DFB7CCB07183324DCDD9` |

五个 test class 与七个 fixture 在领取瞬间双仓均为 `absent`。

## Frozen Delivery Contract

- `TurnTaskStartRequest` 仅携带 stable `startRequestId`、ordered `taskCodes` 和 queue failure policy。
- `TurnTaskStartAck` 必须与 exact `startRequestId` 关联，不引入 session/ledger。
- `TurnWindowMetadata` 新增 `pauseRequested` 并保留 `stopRequested`。
- `TurnRequest` 最多一个 start request；`TurnResponse` 最多一个 matching start ack。
- `SLEEP_COMPUTER` 不得进入 remote task allowlist。
- 生命周期测试覆盖 ordered task codes、failure policy、stable request/exact ack、pause/stop、unknown enum、
  invalid union、failed-step shape，以及 `SLEEP_COMPUTER` reject。
- 完成后只写 `SOURCE DELIVERED + TEST DELIVERED` 并等待父级独立审查，不自批。

## SCOPE AMENDED - TURN-40A-PROD

- 修订时间：`2026-07-15T18:20:00-04:00`
- 父级/用户恢复实施并缩小本 Worker 所有权：本卡现在仅实施 `TURN-40A` 双仓 production protocol。
- `TURN-T01` 的五个 test class 与七个 fixture 已从本 Worker 写集移除；本 Worker不得创建或修改
  `src/test/**`、`src/test/resources/**`。
- 唯一 Java 写集为上文列出的双仓八个 production protocol 文件；文档写集仍仅为本报告。
- 暂停期间复核结果：八个目标文件与领取基线 SHA 完全一致，四个 create 文件仍不存在；未发现其他 Writer
  改动本卡写集。
- 交付合同：闭合 ordered task queue、queue failure policy、stable `startRequestId`/exact ack、
  `pauseRequested`/`stopRequested` 与 `SLEEP_COMPUTER` reject；双仓八对文件必须 byte-identical。
- 验证门：DHXY 运行 `mvn -q -DskipTests compile`；Cloud Brain 运行 `mvn -q clean compile`。不运行测试。
- 最终状态只写 `SOURCE DELIVERED` 并等待父级源码审查；不自批。

## SOURCE DELIVERED - TURN-40A-PROD

- 交付时间：`2026-07-15T18:30:07-04:00`
- Worker 状态：`SOURCE DELIVERED`；等待父级独立源码审查，不自写 `APPROVED/BLOCKED`。
- 实际写集：仅双仓八个 `src/main/java/com/bot/dhxy/cloud/turn/protocol/` production 文件与本报告。
- `TURN-T01` 已按 scope amendment 移出；未创建或修改任何 `src/test/**` 或
  `src/test/resources/**`，双仓 `src/test/resources/cloud-turn/v1/` 均不存在。
- 无已批准业务差异；未引入 session/owner/permit/ledger/TTL/durable workflow/自动 retry。

### Production Contract Closed

- `TurnTaskCode` 以 closed enum 只允许 `WUHUAN_V2`、`WUBEI`、`XIULUO_V2`、
  `AUTO_BATTLE`；`SLEEP_COMPUTER` 不在 remote allowlist，未知值由反序列化/枚举边界拒绝。
- `TurnTaskStartRequest` 只携带 stable `startRequestId`、保持顺序的 immutable
  `taskCodes` 与 `CONTINUE_ON_FAILURE` / `STOP_ON_FAILURE` policy。
- `TurnTaskStartAck` 只回显 exact `startRequestId`；request/response 关联验证要求有 start
  就有 exact ack，无 start 则不得携带 ack。
- `TurnWindowMetadata` 同时携带 `pauseRequested` 与 `stopRequested`；兼容构造器只为既有
  Foundation caller 默认 `pauseRequested=false`，未增加任何新运行语义。
- `TurnRequest`/`TurnResponse` 分别新增单一 `taskStartRequest`/`taskStartAck` union 分支；
  validator 保留既有 action/outcome/failed-step shape 规则并新增生命周期字段验证。

### Final Byte Identity And SHA-256

| File | Bytes | SHA-256 | DHXY = Cloud |
|---|---:|---|---|
| `TurnTaskCode.java` | 129 | `A116361EE173F37639967459111AB1CC595469EE1C8F8E3BA91E108BDB2895F7` | yes |
| `TurnTaskQueueFailurePolicy.java` | 131 | `2B9DBFF0F75612DCC818B89825F2EC003A8BD08ADF05176D7592F4C570B5C97C` | yes |
| `TurnTaskStartRequest.java` | 338 | `D4AF7B55DD1B4A6B01DF5EED4E9F2468B745A31241314762242C340D0FF03117` | yes |
| `TurnTaskStartAck.java` | 101 | `B5C196C7084211AE917DB543411C08247611B0A962CD60219E478660BC1D299B` | yes |
| `TurnWindowMetadata.java` | 681 | `E1430169AAE3E35AC9F6295E41EA401E66EFF910E0B4B4DFF954E72C9416AF1B` | yes |
| `TurnRequest.java` | 505 | `BF7EA75A8FF44CBD5B7FB8F73ECF172BB7466B00EECC1407087688E5D74B571C` | yes |
| `TurnResponse.java` | 328 | `646F9738FC296949BE6D1787481B8F946F2CF4CFA7D2971D5B4B987BA2F1E75F` | yes |
| `TurnProtocolValidator.java` | 21484 | `1590FED78E690E36905F9EE3F697C5A03E16F5CC7F89AD077A3CD3FC2E0BB9C2` | yes |

### Compile Gates

1. DHXY：`mvn -q -DskipTests compile`
   - exit：`0`
   - elapsed：`43.1s`
   - tests：未运行（按修订 scope）。
   - result：通过。
2. Cloud Brain：`mvn -q clean compile`
   - exit：`1`
   - elapsed：`18.8s`
   - tests：未运行（compile phase）。
   - result：全仓 cohort 编译未通过；首个错误为
     `TaskTrackerPanelService.java:[3,25] package com.bot.dhxy.core does not exist`，随后为
     `GameClientTracker`、`TextRecognizer`、`CoordinateHelper`、`OcrWindowScanService`、
     `WindowScopedTempPath` 等写集外依赖缺失。
   - attribution：编译输出没有任何 `com/bot/dhxy/cloud/turn/protocol` 文件错误；本 Worker 不越界
     修复其它并行迁移写集，Cloud 全仓 compile gate 留待父级在 writers 稳定后的 cohort 构建裁决。

### Static Scope Verification

- 八对 production 文件逐字节比较全部为 `True`；无尾随空白。
- owned Java 未出现 `sessionId`、ledger、auto retry、owner、permit、TTL、durable workflow 或
  `SLEEP_COMPUTER`。
- 未运行 runtime/application/server/Task/poller/UI/capture/input；未执行 Git mutation。

## TURN-T01 CLAIMED

- 领取时间：`2026-07-15T18:34:39-04:00`
- 状态：`CLAIMED`
- 角色：CR271 Worker B，负责 `TURN-T01` 实现与点名测试执行；不是 reviewer，不写
  `APPROVED/BLOCKED`。
- start dependency：`S=TURN-01D`。
- approval dependency：`A=TURN-40A`；父级已独立完成 TURN-40A production 源码复审，结论为
  `SOURCE REVIEW PASSED / TEST+BUILD PENDING`，`P0/P1/P2=0/0/0`。
- 权威测试名采用第 19.3、第 19.4 与本报告既有冻结名
  `TurnTaskLifecycleProtocolGoldenJsonTest.java`；不创建笔误名
  `TurnTaskLifecycleProtocolContractTest.java`。

### TURN-T01 Exact Write Set

以下五个 test class 在 DHXY 与 Cloud Brain 的
`src/test/java/com/bot/dhxy/cloud/turn/protocol/` 下各一份，交付时必须 byte-identical：

1. `TurnCoreProtocolGoldenJsonTest.java`
2. `TurnActionGoldenJsonTest.java`
3. `TurnEnvelopeGoldenJsonTest.java`
4. `TurnProtocolValidatorContractTest.java`
5. `TurnTaskLifecycleProtocolGoldenJsonTest.java`

以下七个 fixture 在双仓 `src/test/resources/cloud-turn/v1/` 下各一份，交付时必须
byte-identical：

1. `action-input-capture.json`
2. `outcome-completed.json`
3. `outcome-failed-with-frame.json`
4. `outcome-stopped.json`
5. `outcome-duplicate-or-uncertain.json`
6. `request-start.json`
7. `response-start-ack-idle.json`

文档写集仍仅为本报告。所有 production、POM、其它测试、主计划、CR271、`ACTIVE_WORK`、迁移矩阵与
dashboard 只读；不启动 application/runtime/server/Task/UI/capture/input，不执行 Git mutation。

### TURN-T01 Claim Baseline

- DHXY：branch `thin-client-design`，HEAD
  `0114604e1ff5f15491d2910959c45252e893d04f`。
- Cloud Brain：branch `navigation-migration`，HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`。
- 领取时前三个 Golden test 已存在且双仓逐字节一致：
  - `TurnCoreProtocolGoldenJsonTest.java`：12467 bytes，
    `7D35593E9CCC65624A18D00F40C3E7A782567D0A0A8ECA68317ADEC4EFFA3DDC`；
  - `TurnActionGoldenJsonTest.java`：3166 bytes，
    `D3AFE584104F55DE0F9BDEDD415E5ECCBBC6BE0610474554CB11E8236B31FF98`；
  - `TurnEnvelopeGoldenJsonTest.java`：4181 bytes，
    `011B2126F11654B823418EF4680C585267CCCFF89DD88E1626E9056967D95550`。
- `TurnProtocolValidatorContractTest.java`、`TurnTaskLifecycleProtocolGoldenJsonTest.java` 与七个 fixture
  领取时双仓均为 `absent`。
- 验收范围固定为 `PG+EX+LIFE`：双向 canonical JSON、unknown field/enum fail-closed、failed-step
  shape、ordered queue/failure policy、stable `startRequestId`/exact ack、pause/stop、
  `SLEEP_COMPUTER` reject，以及双仓 production/test/fixture SHA parity。
- 只运行上述五个点名测试，两个仓各自执行；最终只写 `SOURCE+TEST DELIVERED` 并等待父级审查。

## PARENT SOURCE REVIEW #1 - 2026-07-15 18:33 EDT

- Review authority: parent Codex; Worker self-report was not used as approval.
- Verdict: `P0/P1/P2=0/0/0`.
- Status: `TURN-40A SOURCE REVIEW PASSED / TEST + CLOUD BUILD PENDING`; this is not card approval.
- Independent evidence:
  - Parent recomputed SHA-256 for all eight DHXY/Cloud production pairs; all 8/8 pairs are byte-identical and
    match the delivered SHA table.
  - `TurnTaskCode.java:3-8` is a closed remote allowlist containing only Wuhuan V2, Wubei, Xiuluo V2 and
    auto-battle; local-only `SLEEP_COMPUTER` is absent.
  - `TurnTaskStartRequest.java:5-12` carries only stable request id, ordered immutable task codes and closed
    failure policy. `List.copyOf` prevents mutation after validation/serialization.
  - `TurnWindowMetadata.java:3-22`, `TurnRequest.java:3-16` and `TurnResponse.java:3-15` add only the frozen
    pause/stop, optional start request and optional ack fields while retaining compatibility constructors for
    pre-activation callers.
  - `TurnProtocolValidator.java:15-65,336-365` validates request/start fields, response shape, exact
    device/window action correlation, ack absence without a start request, ack presence with a start request,
    and exact `startRequestId` equality.
  - Parent repository scans found only the expected compatibility-constructor callers; no stale constructor or
    alternate protocol copy escaped the two owned paths. Scoped `git diff --check` is clean.
- Build evidence: DHXY compile was reported exit 0 and the parent verified no owned-file static inconsistency.
  Cloud compile remains cohort-pending because the first compiler failure is the independently verified
  out-of-scope `TaskTrackerPanelService.java:3` import of absent Cloud `GameClientTracker`; another Cloud writer
  is active, so the parent did not run a competing clean build.
- Acceptance still pending: TURN-T01 named tests and fixtures, parent test review, fresh named test exits, and
  applicable Cloud compile/package. Production ownership is released; Nash is immediately continued on T01.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## OWNERSHIP COLLISION RELEASED

- 释放时间：`2026-07-15T18:44:38-04:00`
- 状态：`OWNERSHIP COLLISION RELEASED`；本 Worker 立即停止 `TURN-T01` 测试/fixture 编辑并释放全部
  TURN-T01 所有权，等待父级续派互斥 `TURN-T03B` exact write set。
- 真实冲突：`TURN-T01-TEST` 已在本 Worker `2026-07-15T18:34:39-04:00` CLAIMED 前由
  Worker E/Ohm 领取；本 Worker 的领取基线已看到其前三个 Golden test，但当时错误继续领取。
- 本段之后不回滚、不覆盖、不清理任何现有内容；不再运行 Maven，不执行 Git mutation；不自写
  `APPROVED/BLOCKED`。

### Files Actually Touched By This Worker After 18:34

以下 SHA-256 为释放取证时的当前内容；这些文件保留原状交由父级/真实 owner 裁决：

| Repo | File | Bytes | Current SHA-256 |
|---|---|---:|---|
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` | 9399 | `49D98DD8190D3626348FFB49317E1D1728556BAC0FDDDE2EA98C09EDE90EAB27` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java` | 7479 | `FD66B0A686978F17B79D4972F38946B919CD68B4AED12168A2D810A9E298F41B` |
| DHXY | `src/test/resources/cloud-turn/v1/action-input-capture.json` | 1476 | `17D27AC802DB8B4047F235D71D5C2806E98460CA0CFB2878C8DE8F3179CAD429` |
| DHXY | `src/test/resources/cloud-turn/v1/outcome-completed.json` | 1563 | `06576660CFEF6CCCE0E945FDCB09460F07CE4FAE2338A0686252F84B323BFB28` |
| DHXY | `src/test/resources/cloud-turn/v1/outcome-failed-with-frame.json` | 1355 | `9FA5CA5B11A32D0E01D1A9F18466288BC147DC7C1C0472C5DDC933797B4E20F1` |
| DHXY | `src/test/resources/cloud-turn/v1/outcome-stopped.json` | 900 | `79B60B085B8E06EEE8539EEDC1616CEF9357868726FB7E13DDF6AEAA6C503370` |
| DHXY | `src/test/resources/cloud-turn/v1/outcome-duplicate-or-uncertain.json` | 618 | `72A022E1A56356BC588F9239E8C50402E1141E3ECA4C53DE9FD9691D762011D0` |
| DHXY | `src/test/resources/cloud-turn/v1/request-start.json` | 643 | `6354AD76A3FBE144C1D4FC2CC7ACA4E1A6DDD8A04C37C4E62E54DCD90D1DA027` |
| DHXY | `src/test/resources/cloud-turn/v1/response-start-ack-idle.json` | 108 | `722DDBA237D6EE09DBA21CD3F2228E459766A47E04F0B40F2BE0322068624376` |
| Cloud | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` | 9399 | `49D98DD8190D3626348FFB49317E1D1728556BAC0FDDDE2EA98C09EDE90EAB27` |
| Cloud | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java` | 7479 | `FD66B0A686978F17B79D4972F38946B919CD68B4AED12168A2D810A9E298F41B` |

- 本报告 `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-40A-T01.md` 也由本 Worker以
  append-only 方式写入；报告自身 SHA 会因本释放段落写入而变化，因此不写自引用 SHA。
- 本 Worker没有写入三份既有 `TurnCore/TurnAction/TurnEnvelope` Golden test，也没有写入 Cloud 七个
  fixture；它们属于并发真实 owner 的内容。
- 释放指令到达前，本 Worker 已对 DHXY 依次发起五个点名 Maven test 命令；五条均在全仓
  `testCompile` 被写集外既有测试源码错误终止，未形成当前测试通过证据。Maven 已更新 DHXY
  `target/**` 生成物；按父级指令不清理、不回滚，也不继续运行 Cloud Maven。
