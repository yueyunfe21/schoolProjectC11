# TURN-17 - Quest local-Service turn client

## READY / PARENT FROZEN BRIEF - 2026-07-15 20:33 EDT

- 状态：`READY`；类型：`INTEGRATION`；startDependsOn：`TURN-02R`、`TURN-13C` source review passed。
- 本卡只交付可由后续真实 Task caller 注入的 typed client；Cloud 当前没有 Quest activate caller，
  `XiuluoTaskV2.tryReadObjectiveFromTaskPanel(...)` 的 caller cutover 严格留给 TURN-37，不扩大本卡写集。
- 唯一 production write set：新
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudQuestLocalServiceClient.java`。
- 唯一 test write set：新
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/QuestTurnClientContractTest.java`；本报告可写。
- client 只能复用现有 `TurnGameClient.localService(...)`，以现有 `TurnLocalServiceResult` / `TurnInvocationResult`
  暴露四种 command/terminal 状态；JSON 解析 helper/result DTO 只能是该文件 private nested type。
- `QUEST_ACTIVATE` 要求非空 task、非 null keepOpen；COMPLETED 严格解析 `{"activated":true|false}` 且无 frame。
- `QUEST_CAPTURE_DETAIL` 要求非空 task、keepOpen=null；同一 command 返回 `{"captured":true}` 与唯一 raw PNG，
  `purpose=QUEST_DETAIL`、`sourceStepIndex=0`，region x/y 是 `QuestDetailCapture.screenX/screenY` 的屏幕绝对坐标；
  SHA、尺寸、region 与 PNG bytes 对齐。禁止 Base64、临时路径、二次截图或下载。
- named test 覆盖 COMPLETED/FAILED/STOPPED/uncertain、exact operation/window/args、一次 public invocation 一个
  UUID/一个 command、raw PNG 对应和任何状态均无自动 retry。非 COMPLETED 不得折叠为 false/成功。
- 禁止修改 XiuluoTask/QuestManagerService/DHXY/协议/Server/routes，禁止 Task/runtime/input/capture 与 Git mutation。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-17 parent frozen brief -->

## CLAIMED - 2026-07-15 TURN-17 implementation Worker

- Role: implementation Worker; parent remains the sole manager/final reviewer.
- Scope claimed: `CloudQuestLocalServiceClient.java`, `QuestTurnClientContractTest.java`, and this fixed report only.
- Gates: source and test-source delivery only; no Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input or Git mutation.

## SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING - 2026-07-15

### Baseline and scope evidence

- Cloud working baseline observed before implementation: branch `navigation-migration`, HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`; no upstream was configured. The Cloud worktree already contained
  extensive unrelated modified/untracked files. This Worker did not revert, overwrite, stage, clean, or otherwise
  mutate those changes.
- Authority checked: complete `AGENTS.md`, complete `docs/DHXY_CONTEXT.md`, authority plan Sections 14-19,
  `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`, and this frozen report.
- Applicable `docs/业务逻辑.md` baseline checked: 修罗非快捷目标读取 reuses one already-captured image, performs
  no second capture/input/UI action, and treats an empty/failed read as failure without a same-phase replacement
  capture; the phase fallback table remains governed by baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- No Task caller was changed. `XiuluoTaskV2`/`QuestManagerService` remain read-only; caller cutover remains TURN-37.

### Delivered files and SHA-256

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudQuestLocalServiceClient.java`
  - bytes: `12701`; lines: `279`
  - SHA-256: `b1a0481c605ed16880ca52990d22c3f754530afb790565d07e09ea61ee45cf8d`
- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/QuestTurnClientContractTest.java`
  - bytes: `20606`; lines: `479`
  - SHA-256: `68efa9581b056d9e0199899b8135585eb1a32c4e650372e0392cd139c6306b7d`
- This fixed report was appended at true EOF. A self-hash is not embedded because embedding it would change the
  report hash; parent may hash the final report during review.

### Source delivery

- Added injectable `CloudQuestLocalServiceClient`, backed only by the existing
  `TurnGameClient.localService(...)`. Each public operation performs one explicit local-Service invocation with
  `fullWindowFailureEvidence=false`; there is no retry, second exchange/client, owner/session/ledger/TTL state, or
  caller wiring.
- `QUEST_ACTIVATE` passes exact task + non-null keepOpen, preserves `TurnLocalServiceResult` /
  `TurnInvocationResult` command and terminal states, interprets only a real COMPLETED outcome, strictly parses
  the private nested `QuestActivateResult`, and rejects any frame.
- `QUEST_CAPTURE_DETAIL` passes exact task + `keepOpen=null`, interprets only a real COMPLETED outcome, strictly
  requires `{"captured":true}`, and consumes the single raw PNG from that same invocation. It validates
  `QUEST_DETAIL`, `sourceStepIndex=0`, exact unrebased absolute region x/y, region/dimension agreement, PNG
  signature/IHDR dimensions, and raw-byte SHA-256. No Base64, temp/debug path, download, or second capture exists.
- Non-COMPLETED command states and FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN outcomes return unchanged; semantic
  extraction rejects them instead of turning them into false/success.
- JSON result DTOs are exactly two private nested records in the production file. Strict parsing rejects missing,
  null, scalar-coerced, duplicate, unknown, and trailing fields/tokens.

### Test-source delivery and static self-check

- Added the named `QuestTurnClientContractTest` with 9 fake-port-only tests covering `BC4+BASE+LS+IMG`:
  COMPLETED true/false activation, FAILED/STOPPED/uncertain terminals, four non-COMPLETED command states, exact
  operation/device/window/task/keepOpen/step shape, one valid UUID and one command per public invocation, strict
  malformed JSON, activate no-frame, detail same-command raw PNG, absolute negative-x region preservation,
  source step 0, SHA/dimensions/defensive copy, and no retry on every rejection/status path.
- Static counts: production `turnGameClient.localService(...)` call sites=`2` (one in each operation), private
  nested result records=`2`, named test methods=`9`.
- Static forbidden-boundary scan found no production import/call for Base64, filesystem paths, `CloudTurnExchange`,
  `XiuluoTaskV2`, direct `QuestManagerService`, a second `TurnGameClient`, owner/session/ledger/TTL, runtime, input,
  capture, application, or server activation. The test uses a scripted `CloudTurnCommandPort` only.
- Write-set audit: this Worker wrote only the two new Java files above and this report. All protocol, DHXY,
  Server/routes, Task, and local Service files remained read-only.

### Intentionally unrun gates

- Per the frozen Worker brief, **not run**: Maven, JUnit, compile, package, runtime, application, server, Task, UI,
  capture, input, OCR, or any Git mutation.
- Required parent-cohort command remains unexecuted:
  `mvn -q -Dtest=QuestTurnClientContractTest test` from `D:/mavenProject/dhxy-cloud-brain`, followed by the
  applicable Cloud compile gate only when the parent declares writers stable.
- Delivery status is source + test source only. Parent source review, parent assertion review, named test result,
  and compile result are all pending; this Worker makes no approval claim.

**无已批准业务差异；按基线等价迁移。**

## PARENT SOURCE + TEST-SOURCE REVIEW - 2026-07-15 20:58 EDT

- Parent independently read the complete production client, all nine named-test methods, the frozen protocol
  validator, `TurnGameClient`/`TurnInvocationResult`/`TurnLocalServiceResult`, the DHXY local Quest operation
  contract, and the applicable `696a12b0` Quest baseline. Worker self-review was not used as approval.
- Verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`.
- Production evidence:
  - `CloudQuestLocalServiceClient.java:62-110` performs exactly one `TurnGameClient.localService(...)` call per
    public operation with exact `QUEST_ACTIVATE` or `QUEST_CAPTURE_DETAIL` arguments and no retry/second exchange.
  - `CloudQuestLocalServiceClient.java:121-193` preserves command/outcome terminals and strictly accepts only the
    frozen completed JSON shapes; non-completed states are not collapsed to false/success.
  - `CloudQuestLocalServiceClient.java:195-230` validates same-command raw PNG purpose, step index, absolute region,
    SHA and dimensions without Base64, path, download or second capture. DTOs remain private nested records at
    `:268-277`.
- Test-source evidence:
  - `QuestTurnClientContractTest.java:58-190` proves exact operation/device/window/task/keepOpen, one UUID/command,
    COMPLETED/FAILED/STOPPED/uncertain and all non-completed command states without retry.
  - `QuestTurnClientContractTest.java:192-325` rejects malformed JSON, forbidden/missing frames and wrong
    purpose/step/SHA/dimensions, while the valid case at `:92-132` proves raw 2x2 PNG, negative screen-absolute X,
    sourceStepIndex=0 and defensive copying.
- Parent recomputed SHA-256 and matched the delivery report:
  production `B1A0481C605ED16880CA52990D22C3F754530AFB790565D07E09EA61EE45CF8D`, test
  `68EFA9581B056D9E0199899B8135585EB1A32C4E650372E0392CD139C6306B7D`.
- No Task caller was added; real Xiuluo consumption remains TURN-37. The ignored Cloud test tree remains the shared
  TURN-T01 retention gate, not a TURN-17 source repair.
- Maven/JUnit/compile were not run while Java implementation writers remain active. This is not
  `CARD APPROVED/CLOSED`; named test and applicable Cloud compile remain cohort-pending.

**SOURCE+TEST SOURCE REVIEW PASSED / MAVEN+CLOUD COMPILE PENDING**

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-17 parent source and test-source review -->
