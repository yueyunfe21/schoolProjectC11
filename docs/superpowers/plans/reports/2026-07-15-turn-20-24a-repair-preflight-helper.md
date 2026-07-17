# CR271 TURN-20 / TURN-24A Repair #1 Delivery Preflight Helper

## Role and result

- 身份：CR271 非绑定 Delivery Preflight Helper，不是 reviewer；本文不写卡片终局裁决，也不替代父级源码、断言、named-test 或 build gate。
- 预检结果：`PRECHECK_CLEAR`，仅表示当前磁盘上 TURN-20 Repair #1 与 TURN-24A Repair #1 的 production/test-source 静态内容精确覆盖本轮父级退修点。
- 两张原卡在本次读取期间均追加了父级段落；本文按追加后的真实 EOF 复核。下文引用现有父级 EOF 只用于标识现场，不是本 helper 自行作出的终局结论。
- 本轮未运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input；未执行 Git mutation；未修改 Java、测试、权威计划、CR 卡、`docs/ACTIVE_WORK.md` 或其它文档。

## Authority and baseline read

- 已完整读取 `D:/mavenProject/DHXY/AGENTS.md` 与 `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`。
- 已读取权威计划的卡片生命周期/Helper 边界、TURN-20、TURN-24/24A、审计后注册表、精确 write set、`BC4+BASE` profile 与 named-test gate：
  `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:104-148,539-600,938-1002,1044-1140,1299-1528`。
- 已完整读取两张原卡报告及其最新真实 EOF：
  - TURN-20：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-20.md:1-263`；EOF 为
    `TRUE_EOF: TURN-20 REPAIR #1 PARENT SOURCE+TEST SOURCE REVIEW PASSED`。
  - TURN-24A：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-24A.md:1-170`；EOF 为
    `TRUE_EOF: TURN-24A REPAIR #1 PARENT SOURCE+TEST SOURCE REVIEW PASSED`。
- 已读取 `docs/业务逻辑.md:213-342,651-700,1253-1300`，其中 stop/pause/interruption 不得包装为业务失败，以及未获批准不得改变 fallback、输入、验证和时序顺序，是本轮基线门。
- 已完整读取当前五个 production/test-source 文件、TURN-24A 所用 `TaskCheckpoint`/turn-native checkpoint 路径，并完整读取以下 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线：
  - `AutoCombatPanelService.java`，556 行，blob `bf63d2c78873afd8a0781d97f080a59b2b327942`；
  - `BattleRadarService.java`，506 行，blob `c5840e599795f9c6905d692884cd38265e653b6f`。

## True EOF and SHA reconciliation

| Card | Current artifact | Current SHA-256 | Repair report SHA | Result |
|---|---|---|---|---|
| TURN-20 | `AutoCombatPanelService.java` | `e32c1aa9ea9def6f99fb64552e058123d1e03c420b05919cb18e5e547ce50982` | same | exact |
| TURN-20 | `LocalOcrClient.java` | `f706e58b83cd4dfe9dd296bb41772a59f678e9181483c6d304371a37b65f934d` | same | exact |
| TURN-20 | `AutoCombatPanelTurnContractTest.java` | `d6016392377e2bd5353db0bc6af9ecdbe588f5dd4437e33499144c0f2da61dd8` | same | exact |
| TURN-24A | `BattleRadarService.java` | `fb606fc590a9a33dbd9fd1e4f5f2b67aa1e1b10612e908379c37ec792b276202` | same | exact |
| TURN-24A | `BattleRadarTurnContractTest.java` | `c353dfe92e9f122cee826f770c6967c071e3a04296615ccb052766de51cec8a0` | same | exact |

- Cloud scoped status remains read-only dirty/untracked: `LocalOcrClient.java` is modified; both Service files are untracked; both named tests exist on disk but are hidden from ordinary status by `.gitignore:15` (`src/test/`).
- The source/test hashes are unchanged from each Repair #1 redelivery and from the hashes quoted by the newly appended parent sections. No later Java/test drift was visible at this preflight snapshot.

## TURN-20 Repair #1

### Known FAILED baseline restoration

- Baseline open path `696a12b0:AutoCombatPanelService.java:95-130` maps `submitAndWait=false` to `recordAutoPanelMissing(...:input-failed) -> null`, with no second observation. Current production
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java:173-217`
  returns `null` at `:199-201` only after `requireTerminalOutcome(...)` has accepted a structurally exact known `FAILED`; command uncertainty, malformed terminal and capture failure do not enter that boolean mapping.
- Baseline refresh path `696a12b0:AutoCombatPanelService.java:174-209` returns `false` on input failure and only resets estimate/timestamp after successful input. Current production `:294-338` returns `false` at `:332-335`; `recordAutoCombatRefresh(...)` remains after that branch at `:337`, so visible estimate and prior refresh timestamp are not falsely reset.
- Baseline drag path `696a12b0:AutoCombatPanelService.java:133-155` ignores the input boolean, performs one re-observation, then uses the drag-target fallback if still missing. Current production `:220-266` records known `FAILED` at `:241-243`, still performs the single `findAutoCombatBox(...)` at `:244`, and retains the fallback at `:247-253`; there is no second input command or retry.
- The common terminal boundary `:590-676` returns private `FAILED` only for an exact, fully correlated `TurnOutcome.Status.FAILED` (`:647-670`). `STOPPED` checkpoints at `:630-632`; duplicate/other uncertainty remains fatal at `:634-639`. This prevents the repaired baseline boolean branches from absorbing uncertain terminals.

### Canonical LocalOcrClient

- The default production constructor calls only `LocalOcrClient.readJoinedText(...)` at
  `AutoCombatPanelService.java:98-112`.
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java:57-77`
  implements that minimal API by directly invoking existing `readWords(...)` and joining nonblank words in response order.
- Scoped symbol scan found `HttpClient`/request/response/Jackson/Base64/endpoint codec only in `LocalOcrClient`; `AutoCombatPanelService` has none of those symbols. `HttpClient.newBuilder(...)` remains exactly once at `LocalOcrClient.java:36-38`, so no second OCR transport/config/diagnostics authority remains in the Service.

### Named test source

- `AutoCombatPanelTurnContractTest.java:153-181` directly asserts open `null`, refresh `false`, unchanged estimate/timestamp and no extra command.
- `AutoCombatPanelTurnContractTest.java:215-249` asserts drag known `FAILED` still performs exactly one full-window re-observation, continues through the fallback rounds ROI, emits one input command total and does not retry.
- `AutoCombatPanelTurnContractTest.java:445-491` keeps confirmed `STOPPED`/interrupted propagation and unconfirmed/uncertain fatal behavior separate from known `FAILED`, with exact command counts and unique UUID checks.

## TURN-24A Repair #1

### Confirmed stop propagation

- Current production `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java:528-632`
  calls `TaskCheckpoint.throwIfStopRequested(...)` for command `INTERRUPTED_UNCERTAIN` at `:562-568` and outcome `STOPPED` at `:570-577` before projecting the terminal to an unavailable observation.
- `TaskStopRequestedException` is caught and rethrown unchanged at `:625-626`, before the broad `IOException | RuntimeException` catch at `:627-630`; confirmed stop therefore escapes `captureRoi`, `probeTemplates`, `captureFastExpectedExitAvatar` and the top-level public caller.
- `TaskExecutionContext.java:385-429` confirms the checkpoint performs a fresh exact device/window metadata read and throws the stop exception only for `stopRequested=true` (or the standard interruption signal). Missing/mismatched checkpoint metadata raises a transition exception instead.

### Unconfirmed terminal remains conservative

- When the checkpoint returns active, both command interruption and outcome `STOPPED` continue to `CaptureObservation.unavailable(...)` at `BattleRadarService.java:567-568,575-577`.
- A checkpoint transition/metadata uncertainty is caught by the broad fail-closed catch and also becomes unavailable at `:627-630`; it is not promoted to a combat-exit fact.
- Public radar flow `:118-187` maps unavailable capture/mechanics to “keep `IN_COMBAT`” through `keepCombatForUnavailableProbe(...)` at `:369-377`. This matches the conservative baseline capture-failure branches at
  `696a12b0:BattleRadarService.java:89-94,106-110` and preserves the repeated-miss+minimap exit gate at baseline `:120-139`.
- The production source has one textual `client.capture(...)` sink at `:561`, no `TurnStepType.INPUT`, no `client.execute(...)`, and no retry/second-exchange loop.

### Named test source

- `BattleRadarTurnContractTest.java:232-270` separates STOPPED and INTERRUPTED into unconfirmed and confirmed paths. The confirmed paths script active metadata for the capture and `stopRequested=true` for the checkpoint read, then assert `TaskStopRequestedException` propagation.
- All four paths call `assertExactCaptureCommands(..., 1, 2)`, whose implementation at `:736-755` proves one UUID, one command, one capture action and exactly two metadata reads. `ScriptedCommandPort:868-893` supplies the second metadata snapshot without creating a second capture.
- The general unavailable/correlation matrix at `:195-229` keeps FAILED, BUSY, timed-out and duplicate uncertainty conservative while already `IN_COMBAT`, with one command and zero retry.

## Parent recheck points

1. Recompute the five hashes immediately before named-test/build collection; shared dirty trees remain live even though this snapshot matched both Repair reports exactly.
2. Explicitly include both ignored named-test paths when collecting the Cloud delivery. Ordinary `git status` omits them because `D:/mavenProject/dhxy-cloud-brain/.gitignore:15` ignores all `src/test/`.
3. Preserve the current distinction in TURN-20: only exact known input `FAILED` reaches `null/false/re-observe`; STOPPED/uncertain must not be unified with that enum branch during cleanup.
4. Preserve the current catch order in TURN-24A: the dedicated `TaskStopRequestedException` rethrow must stay before every broad runtime catch; unconfirmed transition remains unavailable and cannot become a combat-exit fact.
5. The authoritative registry is current at plan `:996` and `:1001`, but the plan header at `:3` still says TURN-20/24A are claimed. Synchronize that summary when the parent next edits the plan; do not use the stale header to override the registry or true EOF reports.
6. Named tests and applicable Cloud build remain separate parent gates. This helper intentionally provides no execution evidence and does not authorize `countDelta`, card closure or runtime use.

<!-- TRUE_EOF: TURN-20+TURN-24A REPAIR #1 NON-BINDING PRECHECK_CLEAR -->
