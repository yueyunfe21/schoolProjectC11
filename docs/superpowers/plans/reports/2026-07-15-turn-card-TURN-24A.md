# TURN-24A - BattleRadar primary runnable caller HTTPS turn cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15 21:05 EDT

- 状态：`READY`；类型：`COUNT caller-cutover subcard`；startDependsOn：`TURN-13C`、`TURN-18` source/test-source
  review passed。
- 唯一旧路径覆盖键 `countUnit`：
  `AutoCombatService -> BattleRadarService::checkAndSyncCombatState`；`countDelta=+1` 只在父级源码/测试源码审查、
  named test 和适用 build gate 全部通过后登记。该 Service 其余 public methods 是同文件闭环所需 integration
  surface，不得另行重复计数。历史 `189/407` 不是运行时 ledger，也不是本卡 heartbeat 主进度。
- Worker 是 implementation Worker，不是 reviewer；父级是唯一 manager/final reviewer。
- 唯一 production write set：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java`。
  新算法/model 只能是该文件 private nested type，不得新建 production 文件。
- 唯一 test write set：
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/BattleRadarTurnContractTest.java`。
  本固定报告可写；其余两仓文件全部只读。
- 保留 `696a12b0` 全部 public API、返回值、日志语义与状态顺序，尤其：
  `REQUIRED_COMBAT_EXIT_MISSES=2`；auto -> selection -> top -> repeated-miss+minimap 四阶段优先级；在 combat
  capture/mechanics 不可用时保守保持 IN_COMBAT；avatar baseline/probe/refresh；15s 首次门、1s probe、4s full
  radar fallback、20x20 ROI、0.35 diff；enter/exit pending、armed timestamp、battle count 和动态 polling cadence。
- 迁移边界：exact window rect/ROI 只从当前 turn metadata 取得；截图只用 `TurnGameClient.capture(...)` 返回的同一
  raw PNG；template matching、minimap 可读性、avatar baseline/diff 全在 Cloud 本 Service 内完成。不得调用 DHXY
  tracker、temp path、本地 template/OCR/image helper 或旧 `WindowFact`。
- 生产源码交付后以下七个旧 fact 必须零引用：`BATTLE_RADAR_AUTO_FLAG`、
  `BATTLE_RADAR_SELECTION_SIGNAL`、`BATTLE_RADAR_TOP_SIGNAL`、`BATTLE_RADAR_MINIMAP_READABLE`、
  `BATTLE_RADAR_AVATAR_BASELINE`、`BATTLE_RADAR_AVATAR_PROBE`、`BATTLE_RADAR_AVATAR_REFRESH`；同时
  `readWindowFact`/`WindowFact*`/旧 fact timeout 零引用。
- Radar 是 capture/Cloud calculation 链，不得为凑 BC4 人造 INPUT action。每次 capture 调用一个 UUID/command；
  COMPLETED 才可消费 exact action/window/step/raw PNG，FAILED/STOPPED/uncertain、错 identity/step/frame/坏 PNG
  均 typed fail-closed，不折叠为成功且无自动 retry、第二 exchange 或旧 fact fallback。
- Named test `BattleRadarTurnContractTest` 至少覆盖：四阶段 short-circuit 与调用次数；两次 miss+minimap exit；
  combat capture failure 保守保持；baseline/probe/refresh raw ROI 与 diff；15s/1s/4s cadence；enter/exit signal、armed
  stale filtering、battle count/polling；COMPLETED/FAILED/STOPPED/uncertain、错 identity/step/frame；每次 client
  调用单 UUID/command且任一状态无自动 retry。
- 禁止修改 `AutoCombatService`/`NpcClickService`/Task/caller、协议、Server/routes、DHXY、POM 或其它报告；禁止
  owner/session/ledger/TTL/durable workflow/自动 retry。Worker 不运行 Maven/JUnit/compile/runtime/application/
  server/Task/UI/capture/input，不执行 Git mutation；父级在所有 Java writers 稳定后运行 named test 与适用 compile。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-24A parent frozen brief -->

## CLAIMED - 2026-07-15 21:07:08 EDT

- 身份：`CR271 TURN-24A implementation Worker`（不是 reviewer；父级为唯一 manager/final reviewer）。
- exact production write set：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java`。
- exact test write set：`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/BattleRadarTurnContractTest.java`。
- exact report write set：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-24A.md`。
- 除以上三个文件外，`DHXY` 与 `dhxy-cloud-brain` 两仓全部只读；不回滚、覆盖或清理其他 worker 的 dirty/untracked。

<!-- TRUE_EOF: TURN-24A CLAIMED -->

## SOURCE+TEST SOURCES DELIVERED - 2026-07-15 21:51:23 EDT

- 身份边界：`CR271 TURN-24A implementation Worker`；未承担 reviewer/manager/final reviewer 职责。
- production source：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java`；
  写前 SHA-256 `E90E99FB9444BAD960BC5C0B648EEA51501CED1AAA8ED26B8061F53B46B86405`；
  delivered SHA-256 `88806B03D84EDA04D2916C8528ABE14559344B7A45860CD9F47E7A4B63C2320A`。
- named test source：
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/BattleRadarTurnContractTest.java`；
  写前不存在；delivered SHA-256 `9D0A208BF989FBADCEE46E3375204DB78DCE6862605BD672350895228585E088`。
- production evidence：`checkAndSyncCombatState()` 在 source line 116；四阶段顺序在 lines 117/133/150/166；
  唯一 `client.capture(...)` 调用点在 line 559，固定 `fullWindowFailureEvidence=false`，每次调用由现有
  `TurnGameClient` 生成一个 UUID/command。exact latest metadata/ROI、COMPLETED action/window/step/frame、
  raw PNG signature/SHA/dimensions 校验均在同一 capture boundary 内；无 INPUT/execute/第二 exchange/retry/fallback。
- vision/state evidence：avatar RGB tolerance/diff 在 line 667 起，Cloud 内 template-only minimap coordinate
  readability 在 line 724 起；`2 misses`、15s/1s/4s、20x20、0.35、enter/exit pending、armed timestamp、
  battle count 与 4000/2000/10000 polling 均保留。新 enum/record/helper model 全部是 source EOF 的
  `private` nested types。
- zero-reference evidence：对 production source 静态精确扫描，七个 `BATTLE_RADAR_*` 旧 fact、
  `readWindowFact`、`WindowFact*`、`BATTLE_RADAR_FACT_TIMEOUT` 合计 `0`；`TurnStepType.INPUT`/`.execute(` 合计
  `0`；DHXY tracker/temp path/template/OCR/image helper 均未引用。
- named test source evidence：实际构造并调用 production `BattleRadarService`；test methods 从 lines
  109/135/163/193/233/305 起覆盖四阶段 short-circuit 与 capture 次数、latest exact metadata ROI、
  2 misses+minimap、COMPLETED 与 FAILED/STOPPED/command uncertain/错 action-window-step-frame/坏 PNG、
  combat fail-closed、avatar baseline/probe/refresh 与严格 `0.35` 边界、15s/1s/4s、事件/armed stale
  filtering/battle count/polling，以及每次单 UUID/command、无自动 retry。
- 业务基线核对：已核对 `docs/业务逻辑.md` lines 213-342、651-1031、1087-1300 所覆盖的 expected-combat
  exit/回家纠正、五倍/修罗战斗流与 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` phase baseline。
  **无已批准业务差异；按 `696a12b0` 基线等价迁移。**
- 未运行门：严格按父卡禁止项，Worker **未运行** Maven、JUnit、compile、runtime、application、server、
  Task、UI、capture 或 input；未执行任何 Git mutation。named test 与适用 compile/build gate 留给父级在所有
  Java writers 稳定后运行并 final review。
- scope：除已 CLAIMED 的 production source、named test source 与本固定报告外，没有写入、回滚、覆盖或清理
  两仓任何其它 dirty/untracked 文件。

<!-- TRUE_EOF: TURN-24A SOURCE+TEST SOURCES DELIVERED -->

## PARENT SOURCE/TEST-SOURCE REVIEW #1 - 2026-07-15 22:05 EDT

- 结论：**不通过 / P0/P1/P2=0/1/0**。父级独立逐段核对 production、named test、`TaskCheckpoint`、
  `696a12b0` BattleRadar 与本卡 frozen brief。四阶段 short-circuit、2 misses+minimap、20x20/0.35、
  15s/1s/4s、state/signal/polling、exact raw PNG 与一 capture 一 UUID/command 主链未发现其它阻断。
- **P1-1，STOPPED 被包装成普通 capture unavailable，且 stop exception 会被宽 catch 吞掉。** 当前
  `BattleRadarService.java:559-568` 将 command/outcome `STOPPED` 直接投影成 unavailable；
  `BattleRadarService.java:615-618` 又捕获全部 `RuntimeException`。named test
  `BattleRadarTurnContractTest.java:192-222` 明确要求 STOPPED 与 FAILED 一样只“保持 IN_COMBAT”。这会让已经随 exact
  metadata 确认的 task stop 继续返回业务值，违反 stop/pause 不得包装成业务 miss/false 的基线门。

### Repair #1 - 原 Pauli 继续持有

- 写集不扩张：只改原 `BattleRadarService.java`、原 `BattleRadarTurnContractTest.java` 与原报告。
- 对 command `INTERRUPTED_UNCERTAIN` 和 outcome `STOPPED` 先调用
  `TaskCheckpoint.throwIfStopRequested(currentTaskContext, ...)`；确认 stop 必须抛
  `TaskStopRequestedException`，且该异常不得被 `captureRoi` 的宽 catch 吞掉。若 metadata 未确认 stop，仍作为 typed
  unavailable/fail-closed，不伪造 success，也不追加 capture/retry。
- named test 将 STOPPED 拆成两路：`stopRequested=true` 断言 checkpoint exception、单 capture、零 retry；
  `stopRequested=false` 断言保守保持 IN_COMBAT。另补 interrupted command + confirmed stop 同样传播。
- 不改四阶段、阈值、计时、ROI、模板算法、state/event 或 caller；返修期间不运行 Maven/JUnit/compile/runtime/
  application/server/Task/UI/capture/input，不做 Git mutation。

<!-- TRUE_EOF: TURN-24A PARENT REVIEW #1 BLOCKED P1=1; REPAIR #1 TO ORIGINAL WORKER -->

## REPAIR #1 CLAIMED - 2026-07-15 22:06:43 EDT

- 身份：原 `CR271 TURN-24A implementation Worker` 领取 Parent Review #1 的 P1 repair；不是 reviewer。
- exact production write set：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java`。
- exact test write set：`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/BattleRadarTurnContractTest.java`。
- exact report write set：`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-24A.md`。
- repair scope：仅让 command `INTERRUPTED_UNCERTAIN` 与 outcome `STOPPED` 先走 `TaskCheckpoint`；confirmed stop
  传播 `TaskStopRequestedException` 且不被宽 catch 吞；unconfirmed stop 继续 typed unavailable/保守 IN_COMBAT；
  保持单 capture、零 retry。其余算法、阈值、ROI、计时、state/event、caller 均不改。
- 两仓其余 dirty/untracked 全部只读，不回滚、覆盖或清理；不运行卡片禁止的任何门或运行路径，不做 Git mutation。

<!-- TRUE_EOF: TURN-24A REPAIR #1 CLAIMED -->

## REPAIR #1 SOURCE+TEST SOURCES DELIVERED - 2026-07-15 22:11:00 EDT

- 身份：原 implementation Worker 完成 Parent Review #1 P1 repair；不是 reviewer，未代替父级复审。
- production source：`BattleRadarService.java` repair 前 SHA-256
  `88806B03D84EDA04D2916C8528ABE14559344B7A45860CD9F47E7A4B63C2320A`；repair delivered SHA-256
  `FB606FC590A9A33DBD9FD1E4F5F2B67AA1E1B10612E908379C37EC792B276202`。
- named test source：`BattleRadarTurnContractTest.java` repair 前 SHA-256
  `9D0A208BF989FBADCEE46E3375204DB78DCE6862605BD672350895228585E088`；repair delivered SHA-256
  `C353DFE92E9F122CEE826F770C6967C071E3A04296615CCB052766DE51CEC8A0`。
- production repair evidence：command `INTERRUPTED_UNCERTAIN` 在 source lines 563-565 先调用
  `TaskCheckpoint.throwIfStopRequested(taskContext, ...)`；outcome `STOPPED` 在 lines 571-573 同样先 checkpoint；
  lines 625-626 单独捕获并原样重抛 `TaskStopRequestedException`，位于原 `IOException | RuntimeException` fail-closed
  catch 之前。checkpoint 返回 active 或抛未确认 transition 时仍沿原 typed unavailable/保守 IN_COMBAT 路径。
- named test repair evidence：`stoppedAndInterruptedCheckpointConfirmedStopWithoutRetry()` 从 test line 232 起，
  独立覆盖 STOPPED unconfirmed/confirmed 与 INTERRUPTED unconfirmed/confirmed。confirmed 两路 metadata 脚本均先返回
  active、在 checkpoint 第二读返回 `stopRequested=true`，并断言传播 `TaskStopRequestedException`；四路均断言
  `1 UUID + 1 command + 2 metadata reads`，没有第二 capture 或 retry。
- preserved evidence：production `client.capture(...)` 调用点仍为 `1`；`TurnStepType.INPUT`/`.execute(` 为 `0`；
  七个旧 `BATTLE_RADAR_*` fact、`readWindowFact`、`WindowFact*`、旧 fact timeout 合计 `0`。未修改四阶段、
  2 misses+minimap、模板/minimap/avatar 算法、阈值、15s/1s/4s、ROI、state/event/polling 或 caller。
- 业务结论：**无已批准业务差异；仅修复 confirmed stop 传播，未确认 terminal 仍按原保守基线处理。**
- 未运行门：严格按 Repair #1 禁令，Worker 未运行 Maven、JUnit、compile、runtime、application、server、Task、
  UI、capture 或 input；未执行 Git mutation。named test 与适用 build gate 留给父级复审阶段运行。
- scope：只写入已领取的 production source、named test source 与本固定报告；其余 dirty/untracked 未回滚、覆盖或清理。

<!-- TRUE_EOF: TURN-24A REPAIR #1 SOURCE+TEST SOURCES DELIVERED -->

## PARENT RE-REVIEW #1 - 2026-07-15T22:27:36-04:00

- 结论：`SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD BUILD PENDING`；父级独立复审
  `P0/P1/P2=0/0/0`。`countDelta=+1` 仍待点名测试和适用 build gate，不在本轮登记。
- 原 P1 已关闭：`BattleRadarService.java:530-631` 对 command `INTERRUPTED_UNCERTAIN` 与 outcome `STOPPED`
  先调用 `TaskCheckpoint.throwIfStopRequested(...)`；确认的 `TaskStopRequestedException` 在宽 catch 前单独原样抛出，
  不再被包装为 radar unavailable。checkpoint 未确认 stop 时仍返回 typed unavailable，沿基线保守保持
  `IN_COMBAT`；capture sink 仍只有一次，零 retry/第二 exchange。
- named test `BattleRadarTurnContractTest.java:232-270` 分别覆盖 STOPPED/INTERRUPTED 的 confirmed 与
  unconfirmed 四路，并断言一 UUID/一 command、两次 metadata read、无第二 capture；四阶段、两次 miss+minimap、
  20x20/0.35、15s/1s/4s、state/event/polling 覆盖未漂移。
- SHA-256 与 Repair #1 交付一致：Service `FB60...6202`、test `C353...C8A0`。owner 已释放；所有 Java
  writers 稳定后由父级运行本卡 named test 与适用 Cloud build，门通过前不得写 `CARD APPROVED/CLOSED`。

<!-- TRUE_EOF: TURN-24A REPAIR #1 PARENT SOURCE+TEST SOURCE REVIEW PASSED -->
