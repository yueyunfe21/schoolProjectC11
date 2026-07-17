# W-COUNT-SUMMON-TAIL-CLEAN-DIRECT-1 - Worker I32

EOF CLAIMED

CLAIMED_CORRECTION(countDelta=+1)

- worker: Internal implementation Worker I32
- countUnit: `SummonSkillService::cleanTailNormalSkillsDirect`
- countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- scope: tail normal-skill scan/delete core only; excludes I5 whole-pass, I17 scanner, and I27 ultimate corner
- constraints: no Git, build, test, runtime, or input action; no write outside the assigned Java file and this report

## 交付结论

- 状态：`NO_CODE_CHANGE / SOURCE CLOSED / DELIVERED`。
- `countDelta=+1`，本单计数边界固定为 `SummonSkillService::cleanTailNormalSkillsDirect` 的 tail normal-skill scan/delete core。
- 当前真实 typed chain 已闭合；指定 Cloud Java 无需重复修改，当前 SHA-256 为
  `2EE437F1B82470DA43FD94C02E934E4EB757E3CF04A43535AEE3E0E36CCBD1F5`，与 I5/I27 交付记录一致。
- 无 owner/session/TTL/retry/wrapper；未修改 `TaskMaintenanceService`、DHXY、shared contract、handler、wire 或其它文件。
- 已核对 `docs/业务逻辑.md:170-211` 的 6/8 布局、静态槽状态、最后有效格倒扫与 core 不变边界。
  无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

## 单方法矩阵

| countUnit | active callers | owned behavior | excluded count | result |
|---|---|---|---|---|
| `SummonSkillService::cleanTailNormalSkillsDirect` | whole pass 的 `(deadlineAtMs, request)` caller；显式 tail/debug path 的无参 caller | 选定 tail 起点后的有序槽检查、普通技能删除、删除后状态消费、locked/empty/ultimate handoff、计数与九字段 cleanup 构造 | I5 `cleanSummonSkillsOnce` whole-pass；I17 locked-boundary scanner 算法；I27 ultimate-corner 算法 | `NO_CODE_CHANGE`, `countDelta=+1` |

## Cloud 到 DHXY 真实链

1. Cloud `AutoBattleTask.java:111-147` 用 `TaskExecutionContextHolder.callWith(context, ...)` 绑定当前
   task run/window；仅在 combat tick 为 `NONE` 且 action state 为 `FREE` 时进入 idle maintenance。
2. Cloud `AutoBattleTask.java:182-228` 构造 `cleanSummonSkill=true` 的请求，并在 `:208` 调用
   `TaskMaintenanceService.runOpportunisticMaintenance(context, request)`。
3. Cloud `TaskMaintenanceService.java:578-593` 进入既有 summon 分支；`:624-755` 保留 due/free-state、team
   round/capability/claim、cache/cooldown 与 `INTERACTING` 所有权，在 `:755` 恰一次调用
   `summonSkillService.cleanSummonSkillsOnce(cleanupRequest)`。
4. Cloud `SummonSkillService.java:172-223` 从同一 current context 组装
   `expectedSkillCount/trustExpectedSkillCount/startSlotIndex/skipUltimateCornerCheck` 四字段 intent，并在 `:206`
   恰一次调用现有 `summonSkillWholePass().execute(intent)`。`Stopped/Unknown/Interrupted` typed unwind；没有自动重发、
   TTL、retry 或第二套 owner。
5. Cloud existing authority 从 `CloudTaskExclusiveInteractionAuthority.java:792` 进入 retained
   `SUMMON_SKILL_WHOLE_PASS`；DHXY `LocalRemoteGameCommandHandler.java:2655-2782` 在 exact task-run/window admission
   下打开既有 in-flight exclusive，并由 `submitRemoteExclusiveAndWaitDetailed` 的唯一 input-worker callback 在
   `:2687` 调用 DHXY `SummonSkillService.cleanSummonSkillsOnce(request)`。
6. DHXY `SummonSkillService.java:262-301` 因当前线程已是 input worker，直接进入
   `cleanSummonSkillsOnceDirect`，不会 queue-in-queue；whole pass 在 `:301` 调用目标
   `cleanTailNormalSkillsDirect(deadlineAtMs, request)`。显式 tail/debug 路径另由 `:392-413` 到同一参数化方法。

Cloud 指定文件保留同构 baseline method graph：`cleanSummonSkillsOnceDirect` 在 Cloud `:276` 调用参数化
`cleanTailNormalSkillsDirect`，public tail path 在 `:336-357` 经无参 overload 到同一 core。正常 Cloud caller 的生产
执行点是上述 DHXY exact-window exclusive mechanics；Cloud copy 不另起本地输入路径。

## Tail Core 静态核验

- **入口与起点：** DHXY `SummonSkillService.java:423-464` 读取可信 6/8 slot count，否则按用户批准的静态布局
  识别；skill-count 变化时仍强制取消 `skipUltimateCornerCheck`。当前静态边界只替代 tail 起点来源，未改变本单
  普通技能删除和结果消费顺序。
- **deadline：** whole pass 只创建一个 `40_000ms` absolute deadline（`:289`）；core 在 inspect loop（`:473`）
  和 delete 前（`:493`）复用它，locked/ultimate handoff继续传同一 deadline，没有新 timeout 或续期。
- **有序检查：** 从选定 index 读取该槽一次并记录 `inspectedCount/observedStatuses`（`:466-490`）。
  `NORMAL_SKILL` 才进入删除；`UNKNOWN` 或机制失败立即九字段 `success=false`，不把负信号转成成功业务事实。
- **普通删除：** `:492-513` 固定为 deadline check -> `deleteSkillAtSlot` -> `deletedCount++` ->
  `MAX_DELETE=5` gate -> 删除后单次状态读取。底层顺序仍是槽点击 120ms -> 300ms -> 删除点击 120ms ->
  600ms -> 确认点击 120ms -> 900ms -> story dialog，失败原位返回 false，无附加 retry/fallback。
- **删除后分流：** `EMPTY` 交 I27 ultimate corner；`KEEP` 安全停止；`LOCKED` 交 I17 scanner并完整消费
  inspected/deleted/nextStart/failure/ultimateCheckIndex；其它状态 fail closed（`:514-564`）。
- **当前槽分流：** `KEEP` 安全停止，`EMPTY` 交 ultimate，`LOCKED` 交 boundary scanner，其它未知失败
  （`:567-625`）。静态 scan 已选择最后 actionable tail slot，因此 `KEEP` 停止不会跳过后续 opened slot，保留
  696 core 的有效槽顺序。
- **delay/fallback：** 本方法未新增 sleep、verification、park/yield 或 retry；slot hover 仍为 700ms，普通删除
  helper 的既有 delay/fallback 顺序不变。locked scanner 与 ultimate corner 只作为已有 handoff，不计入本单。
- **ultimate-success-before-later-failure：** 四个 handoff 都先把 `cornerResult.clicked/succeeded` 写回局部状态，
  再判断 `completed` 并可能构造整体失败；因此成功生成后的后续失败仍保留 `ultimateGenerateSucceeded=true`。

## 九字段 Terminal 与 Cloud State

- DHXY `buildCleanupResult`（`SummonSkillService.java:786-805`）闭合九字段：`success`、`skillCount`、
  `nextStartIndex`、slot statuses、`ultimateGenerateClicked`、`ultimateGenerateSucceeded`、`inspectedCount`、
  `deletedCount`、`message`。
- DHXY handler `LocalRemoteGameCommandHandler.java:2834-2855` 用 `LinkedHashMap` 逐字段构造 typed cleanup；
  payload codec `RemoteOperationPayloadCodec.java:143-145` 明确校验同一九字段。
- Cloud authority `CloudTaskExclusiveInteractionAuthority.java:1008-1089` 只 final-consume
  `EXECUTED/NOT_EXECUTED/STOPPED`，`UNKNOWN` 不进入 final consumption；`:1305-1315` 保序映射九字段。
- Cloud `SummonSkillService.java:233-247` 再逐字段映回 `SummonSkillCleanupResult`。Cloud
  `TaskMaintenanceService.java:759-795` 仅在整体 success 时更新 scan state/clean timestamp；整体失败但
  `ultimateGenerateSucceeded=true` 时仍先记录既有 ultimate cooldown，保持 success-before-later-failure 语义。

## 验证纪律

- Java change：`NO_CODE_CHANGE`；唯一实际写入为本报告。
- 按用户指令未运行 Git、build、Maven、test、runtime/application/server/host/poller、UI/capture/input。
- 未执行 reset/checkout/clean/delete/stage/commit/branch/worktree；未覆盖或整理共享 dirty 内容。
- 本结论是当前源码的逐跳静态核验，不声称 fresh build/test/runtime 已通过。

`DELIVERED | task=W-COUNT-SUMMON-TAIL-CLEAN-DIRECT-1 | worker=I32 | countUnit=SummonSkillService::cleanTailNormalSkillsDirect | countDelta=+1 | Java=NO_CODE_CHANGE | businessDifference=NONE | verification=STATIC_SOURCE_ONLY`

## Parent Source Review #1 - 2026-07-15T04:22:00-04:00

父级独立读取 Cloud `AutoBattleTask:111-228`、`TaskMaintenanceService:578-795`、
`SummonSkillService:172-357`，以及 DHXY handler/exclusive mechanics 与
`SummonSkillService:262-625,786-805`，确认矩阵单列的 tail normal-skill core 保持 40s absolute deadline、
有序 slot inspect/delete、删除后单次复检、locked/empty/ultimate handoff 与九字段 terminal；未重复计算 I5
whole-pass、I17 scanner 或 I27 ultimate corner。结论 **P0=0/P1=0/P2=0，SOURCE APPROVED /
COUNT PENDING BUILD**，`countDelta=+1`；Java 未改，本 Worker 关闭。
