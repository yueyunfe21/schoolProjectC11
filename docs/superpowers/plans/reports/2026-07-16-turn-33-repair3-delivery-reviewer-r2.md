# CR271 TURN-33 Repair #3 Independent Delivery Review R2

## Decision

**APPROVED - P0/P1/P2 = 0/0/0.**

This is an independent source-and-test-source delivery review only. It is not implementation work,
not the parent/final review, and not evidence that the named test or Cloud compile has run. No current
Repair #3 source defect or test-source defect requires return to the implementation Worker.

## Findings

- P0: 0.
- P1: 0. The Repair #2 fifth-generated-delete early-success defect is closed.
- P2: 0. The new production-path fixtures exercise the repaired branch and fail against the recorded
  pre-Repair #3 branch for independent, behavior-level reasons.

## Production Evidence

1. **The generated NORMAL deletion cannot exit early at the fifth delete.** Current Cloud
   `src/main/java/com/bot/dhxy/service/SummonSkillService.java:823-829` marks the generated result,
   performs the generated NORMAL delete, and increments the whole-pass `deletedCount`. There is no
   `MAX_DELETE_SKILL_COUNT_PER_RUN` return between that increment and the following observation.
2. **Exactly one post-generated-delete observation remains mandatory.** The only call is
   `SummonSkillService.java:837-839`, with one `inspectSkillSlot(...)` action named
   `post-generated-delete-slot-*`; `:840-841` records that single result. `inspectSkillSlot(...)` at
   `:571-625` performs one preflight and one `invokeAction(...)`, with no internal retry loop.
3. **Only stable EMPTY/KEEP succeeds.** `SummonSkillService.java:842-847` accepts
   `EMPTY_SLOT` or `KEEP_SKILL`; `:849` rejects every other result, including `NORMAL_SKILL`,
   `UNKNOWN`, and non-stable states, with `generated normal delete did not leave a stable slot`.
4. **The observation terminates the ultimate path.** The selected-static EMPTY caller returns the
   corner result directly at `SummonSkillService.java:299-303`; the dynamically observed EMPTY caller
   does the same at `:361-365`; the locked-boundary caller returns it at `:727-731`; and the ordinary
   post-delete EMPTY caller returns on failure or on the real-click flag at `:408-415`. A successful
   click sets `ultimateGenerateClicked` only at `:789-805`; the existing guards at
   `:313-315`, `:375-377`, `:414-415`, and `:438-440` prevent a fresh static scan after that click.
   Therefore the fifth generated delete cannot be followed by a sixth delete, another static scan,
   another ultimate action, or another summon-business UUID.
5. **The repair matches the approved baseline.** Baseline
   `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/SummonSkillService.java:584-604`
   performs the generated NORMAL delete, increments the counter, unconditionally observes the slot,
   accepts only `EMPTY_SLOT/KEEP_SKILL`, and otherwise fails. The ordinary-delete budget remains at
   baseline `:359-364`; current ordinary-delete budget behavior remains at Cloud `:392-399` and was
   not moved into the generated branch.

## Test-Source Evidence

1. `SummonSkillTurnContractTest.java:420-432` covers four ordinary deletes followed by a generated
   NORMAL fifth delete and a stable EMPTY observation. It asserts success, click/generation flags,
   `deletedCount=5`, the exact success message, and the final `EMPTY_SLOT` state.
2. `SummonSkillTurnContractTest.java:441-452` runs the same fifth-delete chain with a still-NORMAL
   post-delete observation. It asserts failure, `deletedCount=5`, the exact failure message, and the
   final `NORMAL_SKILL` state.
3. The shared fixture at `SummonSkillTurnContractTest.java:461-496` scripts exactly four ordinary
   deletes. The first three ultimate probes miss, the fourth hits, and the generated NORMAL delete is
   therefore the fifth. Its final queued reply is the one post-generated-delete observation.
4. `SummonSkillTurnContractTest.java:504-540` asserts five delete-prepare actions, one ultimate click,
   four static scans, one cleanup call, complete consumption of scripted replies, one UUID per action,
   unique action ids, and that the last summon-business action is the post-delete
   `INPUT -> WAIT -> CAPTURE` observation. `ScriptedCommandPort.execute(...)` at `:1584-1593` throws on
   any unscripted extra action, so a sixth delete or second observation cannot pass silently.
5. The tests traverse the production public API: the harness constructs the production
   `SummonSkillService` and production `TurnGameClient` at `:1228-1271`, while `run(...)` invokes
   `service.cleanSummonSkillsOnce(request)` under the production task context at `:1275-1280`.
6. The old Repair #2 branch would fail both new tests. Its early success would leave the final scripted
   observation unconsumed, failing `:511` and the final-action assertion at `:538-540`; the unstable
   fixture would also fail `:446-451` because the old branch returned success with the delete-limit
   message before reading the NORMAL observation.

## Protocol And Business Boundaries

- `docs/业务逻辑.md:170-211` authorizes live `if8`, static LOCKED/EMPTY/OCCUPIED classification, and
  reverse-tail selection while explicitly preserving deletion, ultimate-corner, cooldown, queue, and
  cleanup semantics. No additional behavior difference was introduced by Repair #3.
- The authority plan `:1047-1069` and HTTPS turn protocol require one fresh action id per explicit
  Cloud business action and prohibit automatic business replay. Current
  `SummonSkillService.java:912-995` calls `TurnGameClient.execute(...)` once per action and projects
  busy/duplicate/timeout/interrupted uncertainty or correlation defects upward; it contains no retry.
- No Summon implementation state for session, owner, ledger, TTL, or durable workflow was found.
  `CloudSummonSkillWholePassCapability.java:19-35` remains a zero-command/zero-UUID fail-closed
  tombstone, and `CloudTaskExclusiveInteractionAuthority.java` has zero `SummonSkill` production
  matches at its unchanged delivery hash.

## Exact Write Set And Hash Evidence

- Repair #3 delivery files:
  - `src/main/java/com/bot/dhxy/service/SummonSkillService.java` - 1431 lines,
    SHA-256 `991db945f7d621e86287d7dadb121bc9154dfe7375f6176cb4ca71434bcaed04`.
  - `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java` - 1683 lines,
    SHA-256 `6a755b0fb36152afd90ff59244c74cbeae086360dd8b53bf2f492cc83f968998`.
- The other two frozen TURN-33 production files retain their pre-Repair #3 delivery hashes:
  - `CloudSummonSkillWholePassCapability.java` -
    `3ee97295b2d50b052e56347e420eb04c35bea5472b327aec48e02fb015e20a6d`.
  - `CloudTaskExclusiveInteractionAuthority.java` -
    `91349697592cd33cf32870e5b6732a21470480c2ce6ef16bca90a3444297abcc`.
- The original append-only TURN-33 card was read through Parent Review #5 true EOF at SHA-256
  `079ef502ff9e8394cbe85af6122e0047bee3619cfc0ca066ba4200fb188b2313`; this reviewer did not modify it.
- DHXY remained on `thin-client-design` with 85 status entries; Cloud remained on
  `navigation-migration` at `3b988caa010254973e03342272e6d1d6a9685b01` with 28 status entries.
  All existing dirty, deleted, ignored, and untracked content was preserved. The Cloud service is
  untracked and the named test is ignored by the existing repository rules; that remains a later
  delivery/commit gate, not a new Repair #3 source severity.

## Impact And Repair Conditions

- Current impact: the prior false-success window is closed. A generated NORMAL fifth delete now has
  the same stable-state terminal as `696a12b0`; an unstable slot cannot refresh success cooldown through
  this result.
- Current repair requirement: none.
- Re-review is required if either reviewed SHA changes, if the generated branch regains a budget return
  before `post-generated-delete-slot-*`, if a new caller can continue after a real ultimate click, or if
  the named test/Cloud compile later reports a source or test-source failure. Any such failure must be
  repaired in the frozen TURN-33 write set and returned to independent review.

## Verification Boundary

- Fully read for this judgment: repository instructions and context, current active-work top, authority
  plan Sections 14-19, HTTPS turn protocol, approved three-skill static-tail rules, TURN-33 original card
  through Parent Review #5, current Cloud production/test sources, and the applicable `696a12b0` source.
- Per the explicit restriction, no Maven, JUnit, compile/package, runtime, application/server, Task/UI,
  capture/input, or Git mutation was performed. No Java, test, original card, plan, or CR document was
  edited. This report is the only write.

<!-- TRUE_EOF: CR271 TURN-33 REPAIR-3 INDEPENDENT DELIVERY REVIEW R2 APPROVED P0=0 P1=0 P2=0 2026-07-16T06:22:24.038-04:00 -->
REVIEW_COMPLETE true EOF
