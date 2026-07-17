# Cloud TaskMaintenance Summon Window State Count Unit - Worker I13

`CLAIMED | task=W-COUNT-TASK-MAINTENANCE-SUMMON-WINDOW-STATE-1 | worker=Internal I13 | role=implementation-only | claimedAt=2026-07-15T03:04:50.9112153-04:00 | countUnit=TaskMaintenanceService::isSummonSkillCleanDueForCurrentWindow/updateSummonSkillWindowState | requestedCountDelta=+1 | writeSet=[D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java; this-report]`

## Delivery

- Status: `NO_CODE_CHANGE / SOURCE CLOSED / PARENT REVIEW PENDING`.
- The active Cloud chain already closes the matrix's independent summon window-state/cache bullet from the real caller:
  `AutoBattleTask` FREE idle -> `maybeRunIdleMaintenance` -> `runOpportunisticMaintenance` ->
  `maybeCleanSummonSkill` -> typed summon whole-pass result -> due/cache policy and
  `updateSummonSkillWindowState` -> closed `TaskMaintenanceResult`.
- Cloud `TaskMaintenanceService.java` stayed unchanged at SHA-256
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`.
- `requestedCountDelta=+1` is delivered only as `countCandidate=+1`; this Worker does not apply the ledger and does not
  claim reviewer approval. Parent source review and the parent's unified build gate remain pending.
- No approved business difference; migrate behavior-equivalently from
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

## Required Reading And Baseline

- Fully read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and `docs/业务逻辑.md`.
- Read the whole-service migration plan and the applicable method-level matrix row in
  `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`.
- Checked the business contract section `召唤兽三技能维护 / 技能格静态边界识别`: 6/8 layout, static
  `LOCKED_SLOT/EMPTY_SLOT`, occupied-slot interpretation, `UNKNOWN` only for mechanism failure, and UNKNOWN fail-closed.
- Authoritative Service baseline:
  `D:/mavenProject/dhxy-cloud-brain/migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`,
  1123 lines, SHA-256 `4BEAFFD08314F694B41A841DFF236C4CE00DC335CBE75DE74A9F667A53803EDA`.
- Active Cloud Service: 1130 lines. Its only relevant `maybeCleanSummonSkill` difference from the 696 block is the
  already accepted Cloud context projection `summonSkillState(windowKey, context)` in place of
  `summonSkillState(windowKey)`; the due/cache/state branches are unchanged.

## Active Caller-To-Result Evidence

1. `AutoBattleTask.java:111-113` resolves one authority-minted context and binds the entire patrol with
   `TaskExecutionContextHolder.callWith`. `:139-148` enters idle maintenance only when the combat tick returns `NONE`
   and `GameContext.ActionState` is `FREE`.
2. `AutoBattleTask.java:182-228` preserves return-team/session/capability priority, then builds the existing
   `auto-battle` maintenance request with `cleanSummonSkill=true` and calls
   `TaskMaintenanceService.runOpportunisticMaintenance` exactly once.
3. `TaskMaintenanceService.java:578-597` preserves the already-counted entry behavior: normalize/checkpoint,
   broadcast-first short-circuit, then dispatch to `maybeCleanSummonSkill`. This entry is reachability evidence only
   and is not counted again by this report.
4. `TaskMaintenanceService.java:624-669` closes the per-window due decision: enabled and positive interval gates,
   optional FREE-state gate, exact window/identity state, ordinary clean cooldown, and existing UNKNOWN backoff.
5. `TaskMaintenanceService.java:670-693` closes the tail-safe due-cache policy. Expired cache clears only
   `lastConfirmedEffectiveSlotIndex`, `tailSafeCachedAt`, `nextStartIndex`, and observed slot state. Fresh cache
   refreshes the ordinary clean timestamp and returns closed `SUMMON_SKILL_NOT_DUE` without running mechanics.
6. `TaskMaintenanceService.java:743-784` builds the four-field cleanup request, executes one
   `SummonSkillService.cleanSummonSkillsOnce` call, and handles its result in `finally`. Success calls
   `updateSummonSkillWindowState` and refreshes the clean timestamp; a failed pass does not refresh that timestamp.
   Ultimate success before a later cleanup failure retains only its existing ultimate cooldown timestamp.
7. `SummonSkillService.java:194-247` uses the same bound context for one typed whole-pass call and maps the closed
   cleanup value field-for-field, including insertion-ordered slot statuses. `Executed/NotExecuted` return a cleanup
   result; transport `Stopped/Unknown` unwind without auto-resend. A business cleanup value containing
   `SummonSkillSlotStatus.UNKNOWN` remains visible to the maintenance policy.
8. `TaskMaintenanceService.java:786-796` returns closed `SUMMON_SKILL_CLEANED` or
   `SUMMON_SKILL_FAILED_RETRY_LATER`; disabled/not-due/deferred/claimed paths already return their own closed status.
   `AutoBattleTask.java:229-232` consumes the structured result without inventing a new business transition.

## Cache-State Baseline Comparison

Balanced method-block comparison against the 696 mirror produced the following exact matches:

| Active method | 696 equality | SHA-256 |
|---|---:|---|
| `buildSummonSkillCleanupRequest` | exact | `465f1ed15539357545a2396759be8224715339d844c5179292aa1efd955a59cb` |
| `updateSummonSkillWindowState` | exact | `e590611b2b424d7be307e8542698b434434f62c71509a6fd983bf2fc5bf34f20` |
| `isSummonSkillTailSafeCacheExpired` | exact | `58f0705bcee9c43fce27345a9962afae3a9bb5faad739c70ee3a7bca47784ba7` |
| `isSummonSkillTailSafeCacheFresh` | exact | `c9c07d09dad2c4cb593338019ebc72a9fd5879184d9d10686e92393a6d877e47` |
| `findLastConfirmedEffectiveSlotIndex` | exact | `fea238f7e8fecf836a6ea6885347667ccc70df4f178a3687cf043ceeda1177ad` |
| `isEffectiveSummonSkillSlot` | exact | `cd01c0ce49d896223cf2dbc55a5093457a3a50ecf47f032ae26f50febbd15635` |
| `isUnknownSummonSkillFailure` | exact | `ded9273f15f53fcb03fbe6220481da166afa01065ecffe7155354d3e5bd49f65` |
| `invalidateSummonSkillLayoutCache` | exact | `cbe85715b588744ae184c0ec368b7e719dd659c129e971033d703bfd780d5dcb` |

The preserved semantics are:

- skill-count trust cache is exactly 2 hours; the cleanup request keeps expected count, trust flag, next start slot,
  and ultimate-corner skip flag;
- tail-safe cache is exactly 2 hours and is valid only when `nextStartIndex > lastConfirmedEffectiveSlotIndex`;
- effective slots remain `NORMAL_SKILL`, `KEEP_SKILL`, and `EMPTY_SLOT`; `LOCKED_SLOT` and `UNKNOWN` are not promoted
  into effective-tail truth;
- a skill-count change clears stale slot/ultimate/tail state before storing the new result;
- success stores count, cache time, next slot, observed slots, tail-safe point, and optional ultimate success time;
- UNKNOWN failure records the existing backoff and invalidates count/start/tail/observed layout cache. It is not
  treated as success and does not refresh the normal clean cooldown.

## Count Boundary

- Included `+1`: the matrix row
  `TaskMaintenanceService::isSummonSkillCleanDueForCurrentWindow/updateSummonSkillWindowState`, interpreted at its
  documented boundary as the independently reachable due decision plus tail-safe/slot-count cache maintenance.
- Excluded `0`: `runOpportunisticMaintenance/handleMaintenanceBroadcast/maybeCleanSummonSkill`. That entry bullet was
  already source-approved by Worker I3 and is used here only to prove reachability.
- Excluded `0`: the earlier I10 private `maybeCleanSummonSkill` task. It reused the same entry/whole-pass chain and was
  correctly reduced to `countDelta=0`; this report does not revive it.
- The literal public `isSummonSkillCleanDueForCurrentWindow` method is absent from both the 696 Service and the active
  Cloud Service. The prescribed AutoBattle idle chain does not call that later CR253 read-only Runner probe; its 696
  due decision is already in `maybeCleanSummonSkill:648-693`. Adding a duplicate public method would create an
  unreachable second decision/wrapper and could import post-696 lead-time/publisher semantics, so no such code was
  added.

## Scope QA

- Java write set: `NO_CODE_CHANGE`; pre/post SHA-256 is unchanged.
- Report write set: only this file was added.
- No External or other Internal Java/Markdown/config/resource was modified; no dirty/untracked work was reverted,
  overwritten, normalized, or cleaned.
- No owner/session/TTL/retry/wrapper, new gate, extra verification, park/yield, cleanup, or fallback was added.
- No build, Maven, test, runtime/application/server/host/task/poller/UI/capture/input, or Git command/mutation was run.
- This is implementation-worker source evidence only, not a reviewer decision and not an `Approved` claim.

## Immediate Count-Unit Existence/Coverage Checkpoint - 2026-07-15T03:07:45.1464069-04:00

`PROGRESS | task=W-COUNT-TASK-MAINTENANCE-SUMMON-WINDOW-STATE-1 | state=CLAIMED_NO_CODE_CHANGE_SOURCE_EVIDENCE_DELIVERED | countUnitExists=true | alreadyApprovedCountCoverage=false | blocked=false | parentReview=PENDING`

1. **The exact count unit exists.** The authoritative method-level matrix has two separate adjacent rows:
   - `docs/superpowers/specs/2026-07-12-service-migration-matrix.md:1389`:
     `TaskMaintenanceService::runOpportunisticMaintenance/handleMaintenanceBroadcast/maybeCleanSummonSkill`;
   - `docs/superpowers/specs/2026-07-12-service-migration-matrix.md:1391`:
     `TaskMaintenanceService::isSummonSkillCleanDueForCurrentWindow/updateSummonSkillWindowState`.
   The latter is therefore a real independent cache-state unit, not a worker-invented helper slice.
2. **The exact unit has not already been assigned or count-approved.** A repository-wide Markdown search for the
   complete exact count-unit string, excluding this fixed I13 report, returns only matrix line 1391. There is no prior
   `countUnit=TaskMaintenanceService::isSummonSkillCleanDueForCurrentWindow/updateSummonSkillWindowState` CLAIMED,
   delivered, parent count review, or ledger application record.
3. **I3 does not consume this row.** I3's CLAIMED, DELIVERED, and Parent Source Review all name exactly
   `countUnit=TaskMaintenanceService::runOpportunisticMaintenance`. Its review inspected the surrounding cache body
   to prove that entry's behavior, but the strict count gate assigns one named matrix unit per task; inspection of a
   second row is not a second ledger application.
4. **I10's duplicate ruling is confined to the entry row.** Its Parent Count Boundary Review says the matrix groups
   `runOpportunisticMaintenance/handleMaintenanceBroadcast/maybeCleanSummonSkill` as one unit, so a second count for
   private `maybeCleanSummonSkill` is blocked. It does not mention or consume the separate line-1391 window-state row.
5. **The earlier cache-method approval was explicitly non-counting.** Parent Source Review #6 for
   `W-TMS-SUMMON-CACHE-COHORT-IMP1` approved dormant method blocks but states:
   `本 dormant prerequisite 暂不单独增加 189/407`. That work supplied reusable source evidence before the strict
   count gate; it did not close or spend this exact count unit. The current real AutoBattle idle reachability is what
   allows the independent cache-state row to be submitted now.
6. **Disposition:** no duplicate/nonexistent blocker exists. Keep `requestedCountDelta=+1`, `countCandidate=+1`, and
   `countApplied=0`; do not add a public due wrapper or any Java churn. Parent review remains the only authority that
   can apply or reject the count.

`DELIVERED_NO_CODE_CHANGE | task=W-COUNT-TASK-MAINTENANCE-SUMMON-WINDOW-STATE-1 | countUnit=TaskMaintenanceService::isSummonSkillCleanDueForCurrentWindow/updateSummonSkillWindowState | requestedCountDelta=+1 | countCandidate=+1 | countApplied=0 | javaChange=NONE | parentSourceReview=PENDING | unifiedBuild=PENDING_BY_PARENT | workerApprovedClaim=NONE`

## Parent Source Review #1 - 2026-07-15T03:10:00-04:00

父级独立核对矩阵独立行 `:1391` 与 active `TaskMaintenanceService:624-796`。该单位只计算 due 决策、tail-safe/
slot-count cache 与 `updateSummonSkillWindowState`，不重复计算已批准的 opportunistic entry；不存在的 later public
probe 未被补造。active cache helper 与 `696a12b0` 方法块一致，UNKNOWN 不刷成功冷却且失效旧 layout cache。
结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**。fresh Cloud package 前不记账。
