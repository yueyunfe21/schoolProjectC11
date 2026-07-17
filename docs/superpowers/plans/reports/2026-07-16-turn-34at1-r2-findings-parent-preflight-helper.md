# TURN-34AT1 R2 P1 Parent Preflight Helper

## 角色与结论

- 角色：CR271 Internal helper，只读核实，不是实现者、reviewer 或批准者。
- 结论：**R2 的 P1 成立。** 当前 `FAILED` fixture 同时使用 `failedStepIndex=null` 和唯一 CAPTURE `NOT_RUN`，违反 HTTPS turn 协议。它在 `TurnInvocationResult` 的协议校验中抛出 `IllegalArgumentException`，随后被 `BattleRadarService` 的通用异常兜底收敛为 unavailable；因此测试虽保持 `IN_COMBAT`，却没有穿透合法 `FAILED` outcome terminal 分支。
- 性质：这是 test fixture / 验收证据缺口，不是本 helper 已确认的 production 缺陷，也不授权修改 production 业务语义。

## 冻结快照

| 文件 | 当前身份 |
| --- | --- |
| Cloud test `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java` | 1026 行；SHA-256 `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292` |
| Cloud production `src/main/java/com/bot/dhxy/service/AutoCombatService.java` | 852 行；SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` |

- 与 TURN-34AT1 原卡 `:203-222` 的父级 Review #3 冻结身份一致。
- `docs/ACTIVE_WORK.md:3-16` 仍把 AT1 记为父级 test-source 通过、双独立 review 与 build pending；R2 新发现说明该独立 review 门当前不能通过。
- 本 helper 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input，未执行 Git mutation。

## 精确证据链

### 1. 当前测试确实生成协议非法 `FAILED`

1. `AutoCombatServiceTurnContractTest.java:496-523` 遍历 `FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN`；`:510` 三者都调用同一个 `nonCompletedOutcome(...)`。
2. 同文件 `:533-563` 的共享八次序列在 `:544` 再次通过该 helper 排入名义 `FAILED`，`:545-547` 排入另外两个 outcome terminal，`:550-551` 才排入第八个 completed capture。
3. 同文件 `:848-858` 对所有 status 都把每个 step result 固定为 `TurnStepResult.Status.NOT_RUN`。
4. 同文件 `:859-868` 对所有 status 都把 `failedStepIndex` 固定为 `null`；`:869` 将该 outcome 包装为 command-level `COMPLETED`。
5. TURN-34AT1 原卡的交付说明也在 `:90-93` 明载三个 outcome status 共用 `NOT_RUN` helper；这段说明不能改变协议合法性。

### 2. 协议要求与合法 golden 形状相反

1. 协议规格 `2026-07-15-https-turn-thin-client-protocol-design.md:19-22` 明确列出 `failed step plus following NOT_RUN`；`:294-304` 要求失败 outcome 返回 failed step index/type、已完成结果与失败细节。
2. `TurnProtocolValidator.java:95-122` 对每个 completed command 携带的 outcome 执行完整校验，并在 `:118` 进入 failure-shape 校验。
3. `TurnProtocolValidator.java:355-370` 要求：
   - `FAILED.failedStepIndex` 非 null、非负且落在 `stepResults` 内；
   - failed index 之前的结果为 `COMPLETED`；
   - failed index 对应结果为 `FAILED`；
   - 只有 failed index 之后的结果才为 `NOT_RUN`。
4. `TurnProtocolValidator.java:478-481` 对任一违反条件抛出 `IllegalArgumentException`。
5. 双仓 canonical golden fixture `src/test/resources/cloud-turn/v1/outcome-failed-with-frame.json:19-46` 也采用 `failedStepIndex=1`、index 1 `FAILED`、后续 index 2 `NOT_RUN`，与 validator 一致。

### 3. 非法对象实际走异常兜底，不走合法 terminal 分支

调用顺序如下：

1. `AutoCombatService.java:223-230` 的 public `probeWindowCombatStateReadOnly(...)` 调用 `battleRadarService.checkAndSyncCombatState()`。
2. `BattleRadarService.java:118-133` 进入 Stage-1；`:120-125` 发起 auto-flag probe。
3. `BattleRadarService.java:528-561` 计算 exact ROI 后在 `:561` 调用 `TurnGameClient.capture(...)`。
4. `TurnGameClient.java:161-168` 只生成一个 UUID、发一个 command，并在 `:168` 调用 `TurnInvocationResult.from(...)`，没有 retry。
5. 测试 port `AutoCombatServiceTurnContractTest.java:994-1005` 在 `:1003` 生成 raw scripted reply、`:1004` 记录它、`:1005` 返回；所以 command/UUID 计数即使后续协议校验失败仍会保留。
6. `TurnInvocationResult.java:78-103` 先核 action/window/step correlation，再构造 `TurnInvocationResult`；其 compact constructor `:49-66` 在 `:55` 调用 `TurnProtocolValidator.requireValid(outcome)`。
7. 当前 `FAILED` 在该处抛出 `IllegalArgumentException`，`BattleRadarService.java:561` 因而没有取得 `TurnInvocationResult`。
8. `BattleRadarService.java:625-630` 捕获该 `RuntimeException`，返回通用 `CaptureObservation.unavailable(...)`。
9. `BattleRadarService.java:388-391` 把 unavailable 映射为 `SignalProbe.UNAVAILABLE`；`:369-376` 在当前已是 `IN_COMBAT` 时 fail-closed 保持战斗，最终 `:126-128` 提前返回。
10. 合法 outcome terminal 分支实际位于 `BattleRadarService.java:570-577`。合法 `FAILED` 会在 `:570` 取得 outcome，跳过仅针对 `STOPPED` 的 `:571-574`，再由 `:575-577` 正常返回 `outcome=FAILED` unavailable。当前非法 fixture 在到达 `:570` 之前已经抛出。

所以 R2 对结果的区分准确：

- 八次 invocation、八条 command、八个规范且互异 UUID 的计数轴仍成立。
- 当前 `FAILED` 只证明“协议异常后通用 fail-closed”，不证明“合法 `FAILED` terminal 后零 Stage-2/3、零 retry/fallback”。
- `STOPPED` 与 `DUPLICATE_OR_UNCERTAIN` 的 `failedStepIndex=null` 不违反 `TurnProtocolValidator.java:374-375`；P1 精确落在 `FAILED` case。

## 合法单 CAPTURE `FAILED` fixture

当前 action 只有 index 0 的一个 CAPTURE step，因此最小合法形状是：

```java
TurnStepResult failedCapture = new TurnStepResult(
        0,
        TurnStepType.CAPTURE,
        TurnStepResult.Status.FAILED,
        "CAPTURE_FAILED", // 任一稳定、非空 typed code；validator 在 :109 要求非空
        null,
        null);

TurnOutcome failedOutcome = new TurnOutcome(
        1,
        action.actionId(),
        window,
        TurnOutcome.Status.FAILED,
        0,
        "STEP_FAILED",
        "Stage-1 capture failed",
        List.of(failedCapture),
        null);
```

约束说明：

- `failedStepIndex` 必须是 `0`。
- index 0 CAPTURE result 必须是 `FAILED`，不能是 `NOT_RUN`。
- 单 step action 没有“后续 step”，所以本 fixture 内不应出现任何 `NOT_RUN`。
- 若以后该 action 真有 index 1+ 的后续 step，只有这些后续 step 才是 `NOT_RUN`；failed index 之前若存在 step，则必须是 `COMPLETED`。
- 当前 capture 使用 `fullWindowFailureEvidence=false`，协议 validator 允许失败时 frame 为 null；本次无需伪造成功 CAPTURE frame。
- step result 的 `code` 必须非空；outcome code/message 应继续使用稳定诊断值，不应把失败伪装成 completed。

## 最小测试返修写集

Java/test 返修只需一份文件：

`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`

建议在当前行号上只做两处定点修改：

1. 修改 `:848-869 nonCompletedOutcome(...)`：当 `status==FAILED` 时设置 `failedStepIndex=0`，index 0 result 设置为 `FAILED`；`STOPPED`、`DUPLICATE_OR_UNCERTAIN` 保持各自现有合法形状。若 helper 保留未来多 step 兼容，index 1+ 才生成 `NOT_RUN`。
2. 在 `:496-523 stage1OutcomeFailuresKeepInCombatWithExactlyOneCommand` 的 public probe 返回后，从 `harness.port.results.get(0).outcome()` 取 raw reply，在测试体外层直接执行 `TurnProtocolValidator.requireValid(...)`，并显式断言 `FAILED -> failedStepIndex=0 / step[0]=FAILED`。该断言必须位于 `probeWindowCombatStateReadOnly(...)` 调用之后，不能只放进 scripted reply lambda；lambda 内抛出的普通 `RuntimeException` 仍会被 `BattleRadarService.java:625-630` 捕获，无法阻止原假阳性。

不需要也不允许为本 P1 修改：

- `AutoCombatService.java`；
- `BattleRadarService.java`、`TurnGameClient.java`、`TurnInvocationResult.java` 或 protocol classes；
- POM/resources/production hook/第二测试文件；
- business phase、ROI、阈值、fallback、retry、timing 或输入行为。

流程卡的返修/交付记录由有权 implementation/parent owner 另行追加；本 helper 不写原卡。

## 返修验收点

1. 新 test SHA 冻结后，production SHA 仍为 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`。
2. `FAILED` raw outcome 在 public probe 返回后的测试体中通过 `TurnProtocolValidator.requireValid(...)`；字段明确为 `failedStepIndex=0`、index 0 CAPTURE `FAILED`、frame null。
3. 静态复审确认 `TurnInvocationResult.java:55` 对该对象不再抛异常，调用实际到达 `BattleRadarService.java:570-577`，而不是 `:625-630`。
4. `stage1OutcomeFailuresKeepInCombatWithExactlyOneCommand` 对 `FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN` 仍各自保持 `IN_COMBAT`，每例 exactly one invocation/command，scripted reply 耗尽。
5. `everyStage1InvocationEmitsAFreshCanonicalUuidAcrossTerminalAndPositiveCases` 仍是 4 command terminal + 3 **协议合法** outcome terminal + 1 completed，共 8 invocations、8 commands、8 个 canonical 且 pairwise-distinct UUID；第八项仍为真实 completed battle-flag capture。
6. Stage-2/3、自动 retry/resend/replay、compensation、fallback 与第二 action 均为零；没有 production 业务差异。
7. 有权 owner 在 writers 稳定后按原卡门禁运行唯一 named test 与适用 Cloud compile/build；本 helper 未运行也不声称通过这些动态门。
8. 更新后的冻结 test SHA 重新接受两名独立 reviewer；本报告不是 approval。

## 基线边界

- `docs/业务逻辑.md:215-224` 要求无用户批准时按基线等价迁移，不新增 retry、fallback、fail-closed 或 phase/timing 改动。
- 同文件 `:230-232,262-268,273-281` 保留 battle radar 的可信兜底及既有 ROI/阈值/节奏边界。
- 本返修只纠正测试输入的协议合法性，不改变 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的战斗决策。

无已批准业务差异；按基线等价迁移。

TRUE_EOF
