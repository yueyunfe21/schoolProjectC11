# TURN-34BP2 Repair #1 独立整卡审查 R1

- Reviewer：R1 / Hooke。
- 审查时间：`2026-07-16T14:17:26.588-04:00`。
- 审查对象：Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`，1,365 行。
- 冻结 SHA-256：`d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219`。
- 对照：TURN-34BP2 原卡 true EOF、CR271 顶部状态、权威计划第 14-19 节、HTTPS turn 协议、`docs/业务逻辑.md` 与 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线。

## 结论

**APPROVED**

`P0/P1/P2 = 0/0/0`。完整既有 TURN-34BP2 Repair #1 无待返修项；无已批准业务差异，按 `696a12b0` 等价迁移。该结论只批准冻结源码的独立整卡 review，不替代后续 stable-writer Cloud compile/build 门。

## 整卡证据

1. 四个共享 map 已全部 typed 化：`TaskMaintenanceService.java:59-65` 分别为 `ScopedTeamKey`、`TeamRoundKey`、`ScopedLocalSessionKey`、`MaintenanceClaimKey -> Set<ScopedWindowKey>`；对应读写点见 `:98-102`、`:119-123`、`:152-156`、`:177-180`、`:230-236`、`:350-351`、`:378-383`、`:450-455`、`:504-509`、`:558-568`、`:713-768`、`:968-983`、`:1155-1185`、`:1283-1289`。
2. 四个 BP3 per-window map 保持 String key，未被 BP2 typed-key 工作侵入：`:55-58`。`currentWindowKey`、scope-prefix/fingerprint/identity/cache 仍位于 BP3 保留面 `:1034-1144`，本卡未把它们并入四个共享 typed map。
3. public 业务方法恰为 19 个，声明位于 `:73`、`:94`、`:115`、`:148`、`:173`、`:200`、`:214`、`:228`、`:254`、`:292`、`:328`、`:344`、`:370`、`:414`、`:426`、`:443`、`:481`、`:554`、`:597`；五个构造 collaborator 仍为 `:49-53`。
4. supplied context 优先成立：`effectiveContext` 在 `:1021-1025` 先返回 supplied context，仅 supplied null 才读 holder。`executionScope` 在 `:1239-1256` 仅 `effective == null` 的 `:1244` 返回 `ExecutionScope.NONE`；非空 context 缺 scope/invocation authority 在 `:1246-1254` 明确抛 `IllegalStateException`，accessor 异常无 broad catch、无 unscoped 降级。
5. scope/session/window 隔离成立：`ScopedLocalSessionKey` 在 `:1215-1224` 仅为显式非空 session 构造；同 scope+同 session 共享，scope 字段不同则 record equality 隔离。无 session 返回 null，不建立共享 session；claim window 在 `:1202-1207` 使用 `ExecutionScope + exact windowId`。无 context 的两个冻结 public lifecycle API 统一通过 `suppliedLocalSessionKey` `:1193-1195` 取 holder/no-context 单一路径，未做双查或兼容回退。
6. formal/local claim 不解析字符串：sealed `MaintenanceClaimKey` 与两个 record kind 位于 `:1343-1355`；formal 构造在 `:1171`，local 构造在 `:1185`，prune 在 `:1283-1289` 仅按 record 字段和类型判断。源码无 `team + "#"`、`local-team:`、prefix/substring/`Integer.parseInt` 兼容路径。
7. 业务顺序未变：`runOpportunisticMaintenance` `:597-615` 仍是 normalize -> first checkpoint -> optional broadcast -> handled/failed/interrupted 短路 -> 至多一次 Summon delegate -> no-action。Summon 既有 gate、static-tail/UNKNOWN、claim acquire/release/retain 与 `GameContext.ActionState` 恢复位于 `:643-826`；capability 开关集仍为 pathing `5`（`:124-128`）、first-aid `1`（`:157`）、close team `5`（`:181-186`）、return open/close `2/2`（`:200-217`）。
8. terminal/UUID/retry 边界未扩张：本文件没有 command/action/UUID/queue/ledger/owner/lease/durable-workflow 新表面；异常沿现有调用路径传播。与 `696a12b0` 对照，维护广播优先级、Summon 单次 delegate、claim 失败释放条件、成功/UNKNOWN 状态更新及既有 retry-backoff/TTL 业务字节未发现改变。

## 严重级别

- P0：0。
- P1：0。Repair #1 的“非空 effective context 不得降级为 `ExecutionScope.NONE`”已在 `:1239-1256` 闭合。
- P2：0。

## 未运行门与工作区保护

按 reviewer 禁令，未运行 Maven、JUnit、compile、package、runtime、application/server、Task、UI、capture 或 input；未做 Git mutation。两仓 review 前已有 dirty/untracked 均保持保护，本报告是唯一写入。后续 Cloud compile/build 仍为独立 pending gate，不能由本次源码 APPROVED 推断为已通过。

<!-- TRUE_EOF: TURN-34BP2 REPAIR-1 INDEPENDENT-WHOLE-CARD-REVIEW-R1 APPROVED P0-0-P1-0-P2-0 FROZEN-SHA=d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219 NO-PENDING-REPAIR NO-MAVEN-JUNIT-COMPILE-RUNTIME-INPUT-GIT-MUTATION 2026-07-16T14:17:26.588-04:00 -->
