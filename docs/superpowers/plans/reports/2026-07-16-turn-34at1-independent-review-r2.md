# TURN-34AT1 独立 Delivery Review R2

## 结论

**BLOCKED**

- 严重级别计数：`P0=0 / P1=1 / P2=0`。
- 本结论由 TURN-34AT1 delivery reviewer R2 独立形成；未采用、转述或以 R1 结论作为审查依据。
- 本次是冻结源码静态审查，不是父级最终 reviewer 结论。
- 阻断原因：八类共享测试中的 `FAILED` outcome 不是 HTTPS turn 协议允许的合法形状，因此该案例没有穿透 production 的合法 `FAILED` 终态路径。当前证据不能支持“7 terminal + 1 completed 的八类语义均已覆盖”。

## 冻结输入与纪律

### 冻结文件

- Cloud production：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java`
  - 行数：`852`
  - SHA-256：`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`
- Cloud test：`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`
  - 行数：`1026`
  - SHA-256：`b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292`

### 两仓只读快照

- DHXY：分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；读取时 `git status --short` 共 `85` 项。
- dhxy-cloud-brain：分支 `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`；读取时 `git status --short` 共 `28` 项。
- 两仓已有 dirty/untracked 全部保留。本轮未执行回滚、覆盖、清理、删除、暂存、提交或其它 Git mutation。
- 按用户禁令，本轮未运行 Maven、JUnit、runtime、application、server、Task、UI、capture 或 input；报告不声称动态测试或编译通过。

## P1 阻断发现

### `[P1] FAILED 测例使用协议非法的 TurnOutcome，命中异常兜底而非合法 FAILED 终态分支`

**证据链**

1. `AutoCombatServiceTurnContractTest.java:848-869` 的共享 helper `nonCompletedOutcome(...)` 用同一形状创建所有非 completed outcome。
2. `AutoCombatServiceTurnContractTest.java:850-858` 把唯一 CAPTURE step result 固定为 `TurnStepResult.Status.NOT_RUN`；`AutoCombatServiceTurnContractTest.java:864` 同时把 `failedStepIndex` 固定为 `null`。
3. `AutoCombatServiceTurnContractTest.java:495-523` 的 outcome terminal 参数化测试在 `:510` 使用该 helper；`AutoCombatServiceTurnContractTest.java:532-563` 的八类共享测试也在 `:544` 用它创建名义上的 `TurnOutcome.Status.FAILED`。
4. `TurnProtocolValidator.java:355-370` 明确要求 `FAILED` outcome 具有非负且落在 steps 内的 `failedStepIndex`，并且该索引对应的 step result 必须是 `Status.FAILED`。当前测试对象同时违反这两个条件。
5. `TurnInvocationResult.java:49-66` 在 completed command result 构造时调用 `TurnProtocolValidator.requireValid(outcome)`；`TurnInvocationResult.java:78-103` 的 `from(...)` 路径会经过该构造校验。因此这个名义 `FAILED` reply 会在进入正常 outcome terminal 判定前抛出 `IllegalArgumentException`。
6. `BattleRadarService.java:625-630` 捕获 `RuntimeException` 后返回通用 `CaptureObservation.unavailable`；这仍会 fail-closed 保持 `IN_COMBAT`，所以断言表面可通过。
7. 但合法 outcome terminal 的 production 分支位于 `BattleRadarService.java:570-577`。当前 `FAILED` 案例没有到达该分支，不能作为 public production path 的合法 `FAILED` 穿透证据。

**影响**

- 八次调用、八条 command 和八个规范互异 UUID 的计数证据仍成立。
- `FAILED` 的预期终态语义证据不成立；它实际验证的是“协议校验异常后通用 fail-closed”。
- 因而 TURN-34AT1 当前冻结测试不能证明完整的“4 个 command terminal + 3 个 outcome terminal + 1 个 completed”八类合同。
- 这是交付验收证据缺口，不是本轮已确认的 production 实现缺陷；但它直接阻断 AT1 delivery approval。

**必须返修**

- `FAILED` case 必须构造协议合法的 outcome：`failedStepIndex=0`，CAPTURE step result 为 `TurnStepResult.Status.FAILED`，并保留合法 code/detail；若将来有后续 step，后续 step 才是 `NOT_RUN`。
- `STOPPED`、`DUPLICATE` 可继续使用无 `failedStepIndex` 的非 completed 合法形状。
- 返修后必须仍证明每个案例只有一次 invocation、一次 command、一个规范 UUID，且没有 Stage2/Stage3/自动重试。
- 新冻结测试 SHA 需重新接受独立 review；本报告不授权修改 production 业务语义。

## 其余重点核验

### Public production path 穿透

- `AutoCombatServiceTurnContractTest.java:388-389` 调用 public `AutoCombatService.probeWindowCombatStateReadOnly(...)`，不是直接调用私有 helper。
- `AutoCombatServiceTurnContractTest.java:911-952` 构造真实 `AutoCombatService`、`BattleRadarService`、`TurnGameClient`、`CloudTurnActionFactory` 与打包模板；替身边界是 turn command port。
- `AutoCombatService.java:223-230` 从 public probe 进入 `battleRadarService.checkAndSyncCombatState(...)`。
- `BattleRadarService.java:118-133` 执行 Stage1；其 CAPTURE 路径见 `:528-624`，并经 `TurnGameClient.java:95-104`、`:161-168` 发出 turn command。
- 除上述 `FAILED` 非法形状造成的分支偏移外，public caller 到真实 Stage1 production path 的装配与调用关系成立。

### CAPTURE tagged-union null shape

- `AutoCombatServiceTurnContractTest.java:404-416` 核对 step index `0`、type `CAPTURE`，并要求 `inputAction`、`input`、`waitMs`、`match`、`localService` 全为 `null`，`capture` 非空。
- `CloudTurnActionFactory.java:35-37` 以 `new TurnStep(0, CAPTURE, null, null, null, capture, null, null)` 创建该封闭 union；未发现非 CAPTURE payload 渗入。

### Exact ROI、raw PNG 与 correlation

- 测试在 `AutoCombatServiceTurnContractTest.java:101,107-108` 固定窗口原点与期望 screen ROI，结果为 `(1074,680,51,20)`；`BattleRadarService.java:70-73` 的 local ROI 为 `(974,630,51,20)`，阈值在 `:87` 为 `0.85`。
- `AutoCombatServiceTurnContractTest.java:415-433` 核对 `UPLOAD_IMAGE`、精确 region、超时、action/correlation metadata、frame purpose/content type/region/尺寸/source step/SHA。
- `AutoCombatServiceTurnContractTest.java:730-768` 读取真实模板并把它绘入原始 PNG；`:813-842` 构造带精确 metadata 的 completed frame。
- `BattleRadarService.java:379-425` 运行真实 normalized correlation；`:570-624` 校验 outcome/action/window/step/frame/raw PNG 签名、SHA 与解码尺寸。未发现缩放、替代 ROI 或绕开相关性判断的证据。

### 7 terminal + 1 completed、UUID 与零 Stage2/3/重试

- `AutoCombatServiceTurnContractTest.java:540-551` 排入 4 个 command terminal、3 个 outcome terminal 与 1 个 completed；`:553-562` 发起 8 次调用并断言 `executeCalls=8`、脚本耗尽及第八次 completed。
- `AutoCombatServiceTurnContractTest.java:565-582` 以 `UUID.fromString(...)` 规范化并用集合证明八个 invocation/command UUID 两两互异。
- 计数与 UUID 轴通过静态核验；终态语义轴因上述非法 `FAILED` outcome 而阻断。
- 正向 case 在 `AutoCombatServiceTurnContractTest.java:394-397`、terminal loops 在 `:483-487` 与 `:519-523`、八类共享 case 在 `:558-562` 均以 command 次数及脚本耗尽约束单次调用。
- `BattleRadarService.java:129-133` 在 Stage1 visible 时立即返回；`:126-128` 在当前 `IN_COMBAT` 且 Stage1 observation unavailable 时 fail-closed 返回。`TurnGameClient.java:161-168` 是单 command 调用，没有自动重试循环。未发现 Stage2、Stage3 或 retry 被当前合同触发。

## 基线与协议对照

- `migration-baseline/696a12b0/.../AutoCombatService.java:222-229` 与冻结 production `AutoCombatService.java:223-230` 的 public read-only probe 决策一致。
- 696a12b0 的 Stage1 local ROI、`0.85` 阈值与当前 `BattleRadarService` 一致；未发现 AT1 借迁移改变修罗业务判定、顺序、fallback 或重试语义。
- `docs/业务逻辑.md:215-224,273-281` 要求按确认基线等价迁移并保留快速 probe 的 ROI/阈值/节奏。
- HTTPS turn 协议 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:58-80,108-113,151-157,271-292` 要求封闭 step union、单 raw frame、精确 metadata、不得因不确定性自动重执行，以及原始 PNG/非缩放坐标。
- 权威计划第 19 节的 `EX/IMG/BC4/BASE` 验收轴与 TURN-34AT1 子卡的 Stage1 范围已逐项对照；本报告只裁决 AT1 当前冻结交付证据，不扩大 TURN-34A 父卡范围。

## 复审门

1. 修复测试中 `FAILED` outcome 的协议形状，并冻结新的 test SHA。
2. 重新独立确认合法 `FAILED` 确实到达 `BattleRadarService.java:570-577`，而不是 `:625-630` 的异常兜底。
3. 重新确认八类仍各有一次 invocation/command、八个规范互异 UUID，且 Stage2/Stage3/retry 均为零。
4. production SHA 若发生变化，必须重新做完整 production path 审查；当前报告不对任何新字节自动继承结论。

TRUE_EOF
