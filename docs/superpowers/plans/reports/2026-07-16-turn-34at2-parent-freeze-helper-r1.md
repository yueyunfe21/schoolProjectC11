CR271 INTERNAL HELPER CLAIMED | uuid=`019f6b6d-a1e9-7833-96ef-0c304e87d2ae` | nickname=`Hopper` | role=`TURN-34AT2 internal readiness helper (not implementation owner, not reviewer)` | claimedAt=`2026-07-16T11:09:51.2553151-04:00`

# TURN-34AT2 Parent-Freeze Helper R1

## 1. Role, Scope, And Current Decision

- This report is readiness/preflight material only. It is not an implementation claim, delivery, independent
  review, approval, CR status change, or parent-final judgment.
- This helper did not create or modify `TURN-34AT2`, `TURN-34AT1`, `TURN-34A`, Java, tests, plans, dashboards, or
  active-work records. The only write is this new report.
- The next bounded AT2 contract is technically well-defined: two test-only cases can close the normal completed
  blank Stage1 -> Stage2 -> Stage3 chain, two-round exit debounce, and readable/unreadable minimap fork.
- **Current source-start decision: AT2 is not safe to claim yet.** The latest AT1 true EOF is Parent Review #4,
  `P0/P1/P2=0/3/0 / REPAIR #3 REQUIRED`; a fresh External D claim, repair, parent re-review, and two latest
  independent reviews are still required on the same physical test file.
- This is not an AT2 rejection. It is a same-file serialization gate. Once Section 11 is satisfied, the parent can
  freeze the tranche described below without reopening its business scope.

## 2. Authority And Snapshot Read

The helper fully read the requested authority set before writing:

- `AGENTS.md` and all of `docs/DHXY_CONTEXT.md`.
- The latest top CR271 block in `docs/ACTIVE_WORK.md`, now headed `2026-07-16 / CR271 11:03`.
- Sections 14-19 of
  `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`.
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`.
- All of `docs/业务逻辑.md`.
- The complete TURN-34A parent card and the latest TURN-34AT1 true EOF.
- Both latest independent AT1 reviews R1/R2 and the parent Review #4 adjudication that accepted three P1s.
- All existing AT2-specific readiness/preflight material: currently the single
  `2026-07-16-turn-34at2-readiness-preflight-helper.md`.
- Current `AutoCombatService.java`, the fixed AT1 test snapshot, current `BattleRadarService.java`, the existing
  read-only `BattleRadarTurnContractTest.java`, and the complete relevant `696a12b0` AutoCombat/BattleRadar
  baseline source.

Repository snapshot, read-only:

| Repo | Branch / HEAD | Protected status snapshot |
|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 722 entries with `--untracked-files=all` |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 entries with `--untracked-files=all` |

All pre-existing dirty/untracked content remains protected. The status counts are only a snapshot, not a cleanup
list or migration baseline.

## 3. Frozen Identities At This Snapshot

| Artifact | SHA-256 | Lines | Role |
|---|---|---:|---|
| Cloud `AutoCombatService.java` | `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` | 852 | production, read-only |
| Cloud `AutoCombatServiceTurnContractTest.java` | `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292` | 1026 | current 22-test AT1 snapshot, superseded for claim by Repair #3 |
| Cloud `BattleRadarService.java` | `fb606fc590a9a33dbd9fd1e4f5f2b67aa1e1b10612e908379c37ec792b276202` | 1573 | read-only collaborator |
| Cloud `BattleRadarTurnContractTest.java` | `c353dfe92e9f122cee826f770c6967c071e3a04296615ccb052766de51cec8a0` | 896 | read-only fixture evidence |

Important snapshot facts:

1. The current test hash still equals the snapshot reviewed by AT1 R1/R2. It has not yet received Repair #3.
2. Cloud `.gitignore:15` ignores `src/test/`. The AT1 test bytes therefore exist as a protected physical working
   file plus recorded SHA, not as a recoverable tracked commit snapshot. Starting AT2 now would overwrite the
   exact bytes that Repair #3 and its reviewers must inspect.
3. `2026-07-16-turn-card-TURN-34AT2.md` does not exist. This helper does not create it.
4. The older AT2 preflight's observed AT1 SHA `04be925e...` was an in-flight snapshot and is obsolete. The current
   `b5438da...` snapshot is also **not** a valid future AT2 initial anchor because Parent Review #4 requires new
   bytes and a new accepted SHA.

## 4. Baseline Rule Being Frozen

The only business authority is strict
`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`:

1. Radar order stays Stage1 auto flag -> Stage2 summon/withdraw -> Stage3 anger/origin.
2. A visible stage stops the later stages. A completed blank frame is a trusted miss and advances to the next
   existing stage; terminal/uncertain/malformed evidence is unavailable, not a business miss.
3. While remembered `IN_COMBAT`, one complete three-stage miss only increments the existing miss count to 1 and
   keeps combat. It must not read the minimap.
4. The second consecutive complete three-stage miss reaches `REQUIRED_COMBAT_EXIT_MISSES=2` and performs exactly
   one minimap-coordinate observation.
5. A readable minimap changes `IN_COMBAT` to `FREE` and publishes one exit signal. An unreadable/unavailable
   minimap keeps `IN_COMBAT`; it does not fabricate exit, fallback, cleanup, or retry.
6. `probeWindowCombatStateReadOnly(...)` reports `IN_COMBAT` or `NONE` and deliberately does not consume the exit
   signal or run recovery. Recovery, maintenance, timing, and Task caller phase behavior remain AT3+.

There is no approved business difference. This tranche tests the existing decision; it does not add a new read,
retry, TTL, park/yield, cleanup, or Cloud gate.

## 5. Future Exact Write Set

After the parent creates and freezes a real child card, the AT2 implementation owner may write exactly:

1. Modify Cloud
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`.
2. Append claim/delivery evidence only to future
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT2.md`.

Everything else is read-only, including both production Services, `BattleRadarTurnContractTest.java`, all
resources/templates, POMs, callers, protocol/client/action-factory/command-port code, TURN-34A/AT0/AT1 cards, the
older AT2 preflight, and this report.

The tranche creates no production Java, second test class, resource, fixture file, marked image, wrapper/facade,
session, owner, lease, ledger, TTL, queue, durable workflow, or automatic retry.

## 6. Exact Test Delta

Let `N` be the parent-accepted AT1 Repair #3 test count. AT2 adds **exactly two** `@Test` methods, so its delivered
count must be `N+2`. Do not hardcode the current 22-test count into the future card until Repair #3 and both latest
reviews freeze the actual `N` and SHA.

Allowed supporting edits inside the same test file:

- Add exact Stage2, Stage3, and minimap screen-region constants.
- Construct one `BattleRadarService` local in `harness()`, pass that exact instance to production
  `AutoCombatService`, and retain it as a `Harness` record component so the tests can inspect the public one-shot
  exit signal. Do not instantiate a second radar.
- Reuse the existing `blankRoiPng`, `completedCapture`, `decode`, `sha256`, scripted port, UUID helpers, real
  `PackagedTemplateAssets`, real `TurnGameClient`, and production holder binding.
- Add one in-memory readable-minimap PNG helper and one direct seven-command assertion helper if needed. Avoid a
  helper chain; assertions must still expose the actual command/result contract.

The two tests are:

### 6.1 Readable Minimap Confirms Exit

Recommended exact name:

`twoCompletedBlankRadarRoundsAndReadableMinimapConfirmExitThroughReadOnlyProbe`

Arrange:

- New independent harness; exact live metadata; `GameContext.ActionState.IN_COMBAT`.
- Queue six dynamically correlated `COMPLETED` blank raw-PNG captures, then one dynamically correlated
  `COMPLETED` readable-minimap raw-PNG capture.
- Every reply derives actionId, window, requested region, dimensions, source step, PNG bytes, and SHA from the
  action it answers. No static cross-action result may be reused.

Act/assert after public probe 1:

- Call `holder.callWith(context, () -> service.probeWindowCombatStateReadOnly(context, "fivering"))` once.
- Return is `IN_COMBAT`; `GameContext` remains `IN_COMBAT`.
- Exactly three commands exist in Stage1/Stage2/Stage3 order.
- `battleRadarService.consumeCombatExitSignal()` is false; no minimap command exists.

Act/assert after public probe 2:

- Call the same public read-only probe a second time.
- Return is `NONE`; `GameContext` is `FREE`.
- Total is exactly seven commands: second Stage1/Stage2/Stage3, then one minimap capture.
- `battleRadarService.consumeCombatExitSignal()` returns true once and false immediately after. This proves the
  read-only probe left the signal pending; it does not test recovery consumption.
- Scripted replies are exhausted and an eighth command does not exist.

### 6.2 Unreadable Minimap Keeps Combat

Recommended exact name:

`twoCompletedBlankRadarRoundsAndUnreadableMinimapKeepInCombatWithoutExitSignal`

Arrange/act:

- New independent harness; exact live metadata; initial `IN_COMBAT`.
- Queue seven dynamically correlated `COMPLETED` blank raw-PNG captures.
- Call the same public read-only probe twice under the holder binding.

Assert:

- Both calls return `IN_COMBAT`; `GameContext` remains `IN_COMBAT` after the second call.
- Commands 1-6 are the two ordered Stage1/Stage2/Stage3 rounds; command 7 is the minimap ROI.
- `battleRadarService.consumeCombatExitSignal()` is false.
- Exactly seven commands/UUIDs/results/timeouts exist, replies are exhausted, and there is no eighth command,
  retry, extra verification read, compensation, or fallback action.

## 7. Exact Seven-Invocation Sequence

With the accepted exact window `TurnWindowRect(100,50,1280,800)`, both tests must observe this screen-absolute
sequence:

| Invocation | Observation | Exact ROI `(x,y,w,h)` |
|---:|---|---|
| 1 | round 1 Stage1 auto flag | `(1074,680,51,20)` |
| 2 | round 1 Stage2 summon/withdraw | `(1027,352,100,225)` |
| 3 | round 1 Stage3 anger/origin | `(556,112,123,39)` |
| 4 | round 2 Stage1 auto flag | `(1074,680,51,20)` |
| 5 | round 2 Stage2 summon/withdraw | `(1027,352,100,225)` |
| 6 | round 2 Stage3 anger/origin | `(556,112,123,39)` |
| 7 | minimap coordinate scan | `(146,109,178,35)` |

Here, one **invocation** means one real `TurnGameClient.capture(...)` business observation. Each invocation must
mint one canonical UUID and publish one command. Public probe 1 therefore owns three observation invocations;
public probe 2 owns four. No test-supplied/cached actionId is allowed.

## 8. Per-Invocation Protocol Assertions

For all seven actions in each new test, assert directly:

1. `contractVersion==1`; exact `deviceId/windowId`; `fullWindowFailureEvidence==false`.
2. `actionId` round-trips through `UUID.fromString(...)`, and the seven IDs are pairwise distinct.
3. Exactly one step: index `0`, type `CAPTURE`, `UPLOAD_IMAGE`, and the exact ROI from Section 7.
4. Outer non-capture fields are null: `inputAction`, `input`, `waitMs`, `match`, and `localService`.
5. Inner optional mechanics are also null: `clearPointerIfOverRegion` and `pixelChangeProbe`. AT2 must inherit
   the repaired AT1 minimal-capture guard, not reintroduce pointer input or Ctrl-probe mechanics.
6. Timeout is exactly `Duration.ofSeconds(120)`.
7. Command status and outcome are `COMPLETED`; outcome action/window correlation is exact; no failed step.
8. Exactly one completed CAPTURE step result exists and has no match/local-service payload.
9. Raw frame purpose/content type/region/sourceStepIndex/width/height/action correlation are exact; SHA-256
   matches the actual PNG bytes.
10. Decode every PNG and assert decoded dimensions equal its requested ROI. Non-empty bytes or PNG magic alone
    are insufficient.
11. Port `executeCalls`, actions, results, and timeouts are all exactly 7; scripted replies are empty.

This is one UUID/one command per invocation, not one UUID for the whole seven-frame chain and not seven steps in
one command.

## 9. Readable Minimap Fixture

Build the 178x35 minimap frame entirely in memory; do not add or write a resource/output image.

- Fill the background with a known non-text dark color such as `(9,17,25)`.
- Draw the proven white bracket geometry used by the existing read-only radar contract: left vertical at `x=60`,
  `y=8..20` with 2-pixel caps; right vertical at `x=114`, `y=8..20` with mirrored caps.
- Draw a 2x2 white comma component at approximately `(86,17)`.
- Load the real committed `images/template/coord_digits/1.png` and `2.png` pixels and draw them at approximately
  `(68,9)` and `(94,9)`, yielding a real `[1,2]`-shaped coordinate.
- Production loads only digit templates `0..9`; comma recognition is morphological. Do not carry forward the old
  preflight's stronger claim that `comma.png` must be pasted, and do not use a system font or reverse-engineered
  threshold-colored fake digit.
- Keep all placement inside the requested 178x35 ROI and flush temporary images after encoding.

The fixture is accepted only when the public production recognizer traverses segmentation, bracket span, comma,
left/right digit matching, and coordinate plausibility. Do not call or reflect private recognizer helpers.

## 10. AT1 Snapshot Conflict And Terminal Retention Gate

Parent Review #4 supersedes the earlier parent pass and accepts these three Repair #3 findings on current
`b5438da...`:

1. **Legal FAILED outcome:** current `nonCompletedOutcome(...)` uses `failedStepIndex=null` and step `NOT_RUN` for
   `FAILED`, so production reaches generic protocol-exception fail-closed rather than the legal FAILED terminal
   branch. Repair must use `failedStepIndex=0` and step 0 `FAILED` for FAILED only.
2. **Strict 30-second same-team gate:** current same-team/same-window `now+10ms` assertion expects allowed, but
   production and `696a12b0` require deferred. Repair the test expectation; do not change production without a
   separate user-approved behavior CR.
3. **Minimal CAPTURE inner fields:** current positive case lacks direct null assertions for
   `clearPointerIfOverRegion` and `pixelChangeProbe`.

AT2 must not duplicate or enlarge the terminal matrix. Its two new tests use only trusted `COMPLETED` captures.
Instead, final AT2 acceptance retains the repaired AT1 tests byte-equivalently and the whole named class continues
to prove:

- command `BUSY`, `DUPLICATE_ACTION_ID`, `TIMED_OUT_UNCERTAIN`, `INTERRUPTED_UNCERTAIN`;
- legal outcome `FAILED`, `STOPPED`, `DUPLICATE_OR_UNCERTAIN`;
- one real Stage1 completed positive;
- one invocation/one command/one fresh UUID per case, zero Stage2/3, retry, resend, replay, compensation, or
  business fallback after every terminal/uncertain case.

The repaired AT1 eight-call shared-service sequence remains the owner of terminal/uncertain zero-fallback proof.
The AT2 seven-call sequences own normal completed blank-stage and exit/minimap proof. Combining those ownerships
in the same final named-test class satisfies the requested coverage without turning AT2 back into an unbounded
terminal matrix.

## 11. Conditions For A Safe AT2 Claim

The parent may mark a future AT2 child source-start ready only when **all** conditions are true:

1. External D (or a parent-recorded replacement) has a real true-EOF AT1 Repair #3 claim before editing.
2. Repair #3 is delivered on the same test file with all three Parent Review #4 P1s closed; production remains
   exactly `532e6f84...`.
3. Parent re-review records `P0/P1/P2=0/0/0` on the latest repaired test SHA.
4. Two latest independent reviewers each explicitly approve that same repaired SHA with no unresolved P0/P1/P2.
   AT2 must not change the ignored physical test file while either reviewer still owns its snapshot.
5. AT1 implementation and reviewer ownership is explicitly released. No AT3 or other owner is writing the same
   test file.
6. The parent records the actual repaired test SHA, line count, and `N` tests in a newly created AT2 child card;
   neither `04be925e...` nor current blocked `b5438da...` may be used as the claim anchor.
7. Current `AutoCombatService.java` still hashes to `532e6f84...`; current `BattleRadarService.java` still matches
   the parent-accepted TURN-24A contract, or the parent re-reads and re-freezes any drift.
8. The future child card freezes the exact two-file write set, two tests, seven-ROI order, per-invocation UUID/
   command assertions, AT1 retention gate, and AT3+ exclusions in this report.
9. The assigned implementation worker appends its own true-EOF claim to that child card before modifying the
   test, and rechecks that the physical file still equals the recorded initial SHA.

Unrelated writer activity may postpone Maven/build execution, but it is not permission to weaken these source
gates. Any hash/API/owner drift returns to the parent for re-freeze; the worker must not restore, merge, or overwrite
protected bytes.

## 12. Explicit AT3+ Exclusions

AT2 does not cover or modify:

- Stage2/Stage3 positive-hit matrices or later-stage terminal/correlation matrices.
- `handleCombatTick(...)` recovery consumption, `EXIT_RECOVERED`, CommonBox, follower/leader first aid, incense,
  deferred leader recovery, or panel work.
- FAST `15s/1s/4s`, entry `+4s`, maintenance `40s/30s/10s`, wake clamps, or wall-clock scheduling.
- AutoBattle/FiveRing/Wubei/Xiuluo caller phase tests.
- Pointer-clear, pixel-change probe, local template matching, input, local Service steps, retry, compensation,
  extra validation reads, cleanup, TTL, park/yield, session, owner, ledger, or durable workflow.
- Production, resources, POM/config, network/runtime, application/server/Task/UI, real capture, or physical input.

## 13. Future Verification Gate, Not Run Here

After all Java writers release and under the already authorized `HTTPS_TURN_CONTRACT_TEST_FAMILY`, the parent
owns the eventual command:

```text
cd D:/mavenProject/dhxy-cloud-brain
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

This helper did not run Maven, JUnit, compile/package, runtime, application, server, Task, UI, capture, input, or
any Git mutation. It does not claim that the current blocked AT1 snapshot compiles or passes.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

TRUE_EOF PRECHECK_COMPLETE
