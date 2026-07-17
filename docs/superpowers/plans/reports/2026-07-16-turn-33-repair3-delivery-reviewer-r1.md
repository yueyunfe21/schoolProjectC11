# CR271 TURN-33 Repair #3 Independent Delivery Review R1

## Review Boundary

- Role: independent delivery reviewer R1; not the implementation Worker and not the parent/final reviewer.
- Scope: current TURN-33 Repair #3 Cloud production/test delivery, independently checked against
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`, the approved three-skill static-tail rules, and the HTTPS turn contract.
- Read basis: complete `AGENTS.md`, complete `docs/DHXY_CONTEXT.md`, latest `docs/ACTIVE_WORK.md` top, authoritative plan
  Sections 14-19, complete HTTPS turn protocol specification, `docs/业务逻辑.md:170-211`, the complete TURN-33 card through
  Parent Review #5 and the later R2-adjudication append, and all four current TURN-33 Cloud production/test files.
- Mutation boundary: this report is the only written file. All Java, tests, original card, plans, CR material, matrix,
  dashboard, and both dirty/untracked repositories remained read-only. The paused TURN-40C report did not exist, so no
  `PAUSED` append was required.
- Verification boundary: source/test-source review only. No Maven, JUnit, compile/package, runtime, application/server,
  Task/UI, capture/input, staging/commit, or other Git mutation was performed.
- Independence: Worker delivery text, Parent Review #5, and the subsequently appended R2/parent result were treated as
  context only, not as approval evidence for this R1 conclusion.

## Decision

**APPROVED - P0/P1/P2 = 0/0/0.**

Repair #3 closes the generated-normal fifth-delete defect without changing the approved business order. The fifth
generated delete still receives exactly one post-delete observation; only stable `EMPTY_SLOT` or `KEEP_SKILL` succeeds;
all other states fail; and that observation terminates the pass before any sixth delete, fresh static scan, later action,
or later UUID. The named fixtures traverse the production public API and would fail against the Repair #2 early-return
code. This is an independent source/test-source delivery approval, not parent `CARD APPROVED/CLOSED`; named-test execution
and the applicable Cloud build remain separate parent gates.

## Severity Findings

- P0: none.
- P1: none.
- P2: none.

## Production Evidence

1. **Baseline requires the observation unconditionally.** Read-only
   `git show 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/SummonSkillService.java`
   shows the generated `NORMAL_SKILL` delete at baseline `:584-589`, the unconditional post-delete inspection at
   `:590-594`, stable `EMPTY_SLOT/KEEP_SKILL` success at `:595-601`, and failure for every other state at `:603-604`.
   There is no delete-budget return between increment and observation.
2. **The fifth generated delete cannot bypass observation now.** Current Cloud
   `src/main/java/com/bot/dhxy/service/SummonSkillService.java:823-849` performs the generated delete and increments
   `deletedCount` at `:824-829`; the Repair #2 `deletedCount >= 5` success return is absent. There is exactly one
   `inspectSkillSlot(..., "post-generated-delete-slot-*")` call at `:837-839`.
3. **Only stable states succeed.** The single observation is recorded at `SummonSkillService.java:840-841`.
   `EMPTY_SLOT` succeeds at `:842-843`; `KEEP_SKILL` advances `nextStartIndex` and succeeds at `:845-847`; the common
   fallback at `:849` fails `NORMAL_SKILL`, `UNKNOWN`, `LOCKED_SLOT`, or any other non-stable state. This is equivalent
   to baseline `:590-604` and to `docs/业务逻辑.md:208-211`.
4. **No action can follow a real ultimate click.** The four production call sites are
   `SummonSkillService.java:300-303`, `:362-365`, `:408-424`, and `:728-731`. The first two and the locked-boundary
   helper return the corner result directly. The continuing ordinary-delete path returns on corner failure at
   `:411-413` and, after a real click sets `ultimateGenerateClicked=true` at `:805`, returns at `:414-415`.
   Equivalent locked-boundary gates are at `:313-315`, `:375-377`, and `:438-440`. Only a hover/miss with no real click
   can reach a later approved fresh scan.
5. **The fifth generated observation is terminal.** `maybeClickUltimateCorner` rejects entry when the shared budget is
   already exhausted at `SummonSkillService.java:742-744`; a generated delete entered with at most four earlier deletes
   increments to five at `:829`, observes once at `:837-841`, then returns at `:842-849`. There is no loop edge after
   that return, so delete #6, static rescan, action, and UUID are all zero.
6. **Real caller shape remains unchanged.** `TaskMaintenanceService.java:755` still calls
   `summonSkillService.cleanSummonSkillsOnce(cleanupRequest)`, and its success/cooldown projection remains at `:761-779`.
   Repair #3 did not write this caller or any Task.

## Named-Test Evidence

- `SummonSkillTurnContractTest.java:420-433` drives four ordinary deletes plus a generated-normal fifth delete and
  requires stable EMPTY success, `deletedCount=5`, the baseline message, and observed slot 5=`EMPTY_SLOT`.
- `SummonSkillTurnContractTest.java:441-452` drives the same fifth-delete edge with the observed slot still NORMAL and
  requires failure, the exact unstable-state message, `deletedCount=5`, and slot 5=`NORMAL_SKILL`.
- The fixture at `SummonSkillTurnContractTest.java:461-496` scripts four fresh static scans, four ordinary delete chains,
  one ultimate click, one generated NORMAL inspection, generated delete/confirm, and one final post-delete observation.
- The shared assertions at `SummonSkillTurnContractTest.java:504-540` require exactly five delete-prepare actions, one
  ultimate click, four static scans, one cleanup, all scripted replies consumed, a 1:1 action/UUID count, unique action
  IDs, and the final action to be the sole `INPUT/WAIT/CAPTURE` post-delete observation. The scripted production command
  port throws on any unscripted later action at `:1584-1592`, so a sixth delete or later scan/action cannot hide.
- These are not private-helper behavior tests. `SummonSkillTurnContractTest.java:1275-1281` binds a real production
  `TaskExecutionContext` and calls the public production API `SummonSkillService.cleanSummonSkillsOnce(request)`; the
  service uses the production `TurnGameClient` path assembled at `:1208-1232` and `:1258-1272`.
- **Old-code failure proof:** Repair #2 returned success immediately after `deletedCount` became five, before consuming
  the final scripted observation. The stable fixture would fail its exact message/EMPTY status/replies-empty assertions
  at `:425-432`; the unstable fixture would immediately fail `assertFalse(result.isSuccess())` at `:446`. Both therefore
  detect the old defect without relying on a source guard or copied mapper.

## Write Set And Forbidden Machinery

- Repair #3 exact write set is only Cloud
  `src/main/java/com/bot/dhxy/service/SummonSkillService.java`,
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java`, and append-only TURN-33 card
  evidence. In the External C claim-to-delivery time window, these were the only two Cloud source/test files whose
  filesystem mtimes changed.
- Reviewed SHA-256 values are stable and match delivery:
  `SummonSkillService.java`=`991db945f7d621e86287d7dadb121bc9154dfe7375f6176cb4ca71434bcaed04`
  (1431 lines) and `SummonSkillTurnContractTest.java`=
  `6a755b0fb36152afd90ff59244c74cbeae086360dd8b53bf2f492cc83f968998` (1683 lines).
- The other two TURN-33 production files were not changed by Repair #3:
  `CloudSummonSkillWholePassCapability.java`=
  `3ee97295b2d50b052e56347e420eb04c35bea5472b327aec48e02fb015e20a6d` (123 lines), and
  `CloudTaskExclusiveInteractionAuthority.java`=
  `91349697592cd33cf32870e5b6732a21470480c2ce6ef16bca90a3444297abcc` (1198 lines).
- `SummonSkillService.java` contains no operational retry/session/owner/ledger/TTL/durable-workflow machinery; the only
  matches are the explicit negative class contract at `:61-62`. Each business observation/input remains one explicit
  action/UUID and uncertainty remains fail-closed without automatic retry.
- `CloudSummonSkillWholePassCapability.java:17-35` remains a zero-command/zero-UUID fail-closed compatibility tombstone.
  `CloudTaskExclusiveInteractionAuthority.java` has zero `SummonSkill` references. That unchanged generic authority does
  contain pre-existing generic owner/session/ledger machinery for unrelated legacy interactions; its stable SHA proves
  Repair #3 did not add or reintroduce any of it into the Summon path. The test's `TaskRetryPolicy.none()` at `:98-109`
  is static context metadata, not retry behavior.
- Both repositories remain dirty/untracked exactly as found. The Cloud production files are untracked and the named test
  is retained but ignored by existing `.gitignore:15`; no staging, cleanup, deletion, reset, commit, or other Git
  mutation was performed.

## Impact And Re-Review Conditions

- No source or test-source repair is requested by R1.
- Approval is bound to the four SHA values above. Any drift in either reviewed Repair #3 file invalidates this decision
  and requires a fresh independent review of the new bytes.
- Per the explicit prohibition, `SummonSkillTurnContractTest` and applicable Cloud compile/build were not run. Their
  pending execution is a parent delivery gate, not evidence of a source defect and not part of this R1 approval.
- No approved business difference was found; Repair #3 is equivalent to `696a12b0:584-604` plus the approved live
  static-tail selection rule.

<!-- TRUE_EOF: CR271 TURN-33 REPAIR-3 INDEPENDENT DELIVERY REVIEW R1 APPROVED P0=0 P1=0 P2=0 2026-07-16T06:26:45.2432561-04:00 -->
