# TURN-34AT1 Independent Delivery Review R1

## 结论

**BLOCKED**

- Reviewer：TURN-34AT1 独立 delivery reviewer R1，非实现者，非 TURN-34A 父级最终 reviewer。
- 严重级别：**P0/P1/P2 = 0/3/0**。
- 当前 production SHA-256：`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`。
- 当前 test SHA-256：`b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292`。
- 冻结写集要求仍为 test-only；本轮未修改 production、test、AT1 子卡、TURN-34A 父卡或其他文档。

## P1 Findings

### P1-1：`FAILED` fixture 不是协议合法的 terminal outcome，七个 terminal case 实际少覆盖一类

行证据：

- `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java:848-869`：`nonCompletedOutcome(...)` 对所有 status 都生成 `failedStepIndex=null`，且唯一 step result 为 `NOT_RUN`。
- `dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java:95-122`：COMPLETED command 携带的 outcome 必须经过 `requireValid(...)` 和 failure-shape 校验。
- 同文件 `:355-370`：合法 `FAILED` 必须有非负 `failedStepIndex`，对应 step result 必须为 `FAILED`；当前 fixture 同时违反这两项。
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java:161-168` 与 `TurnInvocationResult.java:49-62,78-102`：command 返回后会构造并校验 `TurnInvocationResult`。
- `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java:625-630`：该 `IllegalArgumentException` 被收敛为 unavailable，因此测试可能继续并保持 `IN_COMBAT`，但走的是协议异常兜底，不是合法 `FAILED` terminal outcome 分支。
- 受影响用例为 test `:496-523` 和共享序列 `:533-562`；共享序列中的第五次调用 `:544` 不能证明合法 `FAILED` outcome 的零 fallback/retry 行为。

影响：AT1 要求的“7 terminal + 1 positive 共享 service”只有六类合法 terminal reply 加一个协议无效 fixture，不能作为七类 terminal 语义全部通过的证据。

返修门禁：仅修 test fixture。`FAILED` 应返回 `failedStepIndex=0` 且 index 0 的 `TurnStepResult.Status=FAILED`；`STOPPED`、`DUPLICATE_OR_UNCERTAIN` 继续遵守各自协议形状。返修后必须由共享 service 的八次序列重新证明 8 command、8 个规范且互异的 UUID、reply 全耗尽及 terminal/uncertain 零 fallback/retry。

### P1-2：同窗 30 秒 gate 用例与冻结 production 及 `696a12b0` 基线直接相反

行证据：

- test `AutoCombatServiceTurnContractTest.java:653-661`：同一 `team-1`、同一 `window-34a` 在 `now+10ms` 第二次 reserve 被断言为 `deferred()==false`。
- production `AutoCombatService.java:33,815-827`：guard 为 `30_000ms`，key 仅取 `teamKey`；第二次 age 为 `10ms`，确定返回 `deferred()==true`。
- 权威基线 `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/AutoCombatService.java:33,817-833` 与当前 gate 行为相同；该 baseline 文件已与 git object `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 逐行核对一致。
- TURN-34A 父卡 `:63,95` 要求 30 秒 team-sharing 基线不变，但父卡交付记录 `:304` 又写入“含同窗不自锁”；后者没有 `696a12b0` 源码依据，构成卡内冲突。

影响：以当前冻结 SHA 编译执行时，`refreshDueGateDoesNotLockOutTheSameWindow()` 是确定性失败。它也不能通过推动 AT1 修改 production 来修，因为 AT1 是 test-only，且未经批准不得改变 `696a12b0` 的 30 秒业务语义。

返修门禁：父级先按业务基线 gate 裁决卡内冲突。没有单独获批的业务 CR 时，默认保留 production，并把 test 调整为第二次同 team reserve 被 deferred；不得在 AT1 中添加同窗豁免。

### P1-3：minimal CAPTURE 仍未冻结两个内层可选机械字段

行证据：

- test `AutoCombatServiceTurnContractTest.java:404-416` 已断言 CAPTURE 的外层 union 字段、region 和 result mode，但没有断言 `step.capture().clearPointerIfOverRegion()==null` 与 `step.capture().pixelChangeProbe()==null`。
- `dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java:7-21`：这两个字段分别可引入 pointer-clear input 与 Ctrl-hover pixel-change mechanics，不属于本卡要求的 minimal capture。
- 当前 `CloudTurnActionFactory.java:30-37` 使用二参数 `TurnCaptureSpec`，所以冻结 production 此刻确实把两项置 null；缺口在 golden test 未保护该事实。

影响：以后若 Stage-1 CAPTURE 被附加 pointer read/input 或 pixel-change probe，当前 AT1 positive test 仍会通过，不能满足“one minimal CAPTURE”的冻结要求。

返修门禁：仅在 positive test 增加上述两个显式 null 断言，不改 production。

## 已确认范围

- `AutoCombatService.java` 当前 SHA 与冻结值一致。与 `696a12b0` 的逐行 diff 只见 turn-native context/state ownership、`CloudUiCleaner` port 和旧 coordinator removal 等迁移改造；本轮未发现 phase 顺序、维护时序、恢复优先级或 fallback/retry 的新增业务差异。
- Positive Stage-1 路径当前确为一个 index 0 CAPTURE：`CloudTurnActionFactory.java:30-37`；ROI 由 exact current window metadata 推导，timeout 固定为 120 秒，Stage-1 命中后在 `BattleRadarService.java:118-133` 立即返回，不进入 Stage-2/3。
- Positive test `:378-434` 使用真实共享 service，断言一条 command、exact device/window、单 CAPTURE、`UPLOAD_IMAGE`、120 秒、exact outcome metadata、raw `image/png` frame 的 region/width/height/sourceStepIndex/SHA。
- 生产接收链在 `BattleRadarService.java:537-561,570-624` 校验 exact current metadata、action/step correlation、raw PNG signature、SHA、ROI 尺寸和解码尺寸；不是 Base64 或 JSON 内图片替代。
- 四种 command terminal status 与两种协议合法的 outcome terminal status 均由 unavailable 短路保持 `IN_COMBAT`；`BattleRadarService.java:126-128` 保证 Stage-1 unavailable 时不进入 Stage-2/3。测试 port `:994-1005` 以 reply 耗尽即失败约束额外 command。
- 共享测试结构 `:533-562` 已包含 7 terminal reply 加 1 positive reply，并在同一 service 上要求 8 command；UUID helper 要求规范 UUID 且集合大小为 8。由于 P1-1，当前结构尚不能被接受为七类合法 terminal outcome 的完整证据。

## 审查边界

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、HTTPS turn 协议规格、`docs/业务逻辑.md`、AT1 子卡、TURN-34A 父卡、两个冻结文件及相关协议/调用链。
- 已只读检查 DHXY `thin-client-design` 与 Cloud `navigation-migration` 两仓状态；两仓已有 dirty/untracked 全部原样保护，未回滚、覆盖、清理、删除、暂存、提交或执行其他 Git mutation。
- 按用户禁令未运行 Maven、测试、compile、runtime、application、server、Task、UI、capture 或 input。本结论来自冻结源码、协议和基线的逐行静态审查。
- AT2 及后续父卡剩余 named-test 范围不在本 R1 结论内；本报告不构成 TURN-34A 父级最终 review。

## R1 判定

当前冻结交付 **BLOCKED**。P1-1、P1-2、P1-3 全部关闭，并在更新后的唯一 test SHA 上取得适用的显式测试与编译证据后，才可重新提交独立 delivery review。无已批准业务差异；production 应继续按 `696a12b0` 基线冻结。

<!-- TRUE_EOF: TURN-34AT1 INDEPENDENT-DELIVERY-REVIEW-R1 BLOCKED P0P1P2=0/3/0 PROD=532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9 TEST=b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292 2026-07-16T10:57:17-04:00 -->
