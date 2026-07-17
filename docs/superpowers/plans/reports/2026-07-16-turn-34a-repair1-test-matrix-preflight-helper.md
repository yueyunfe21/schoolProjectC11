# CR271 TURN-34A Repair #1 named-test gap matrix PRECHECK

## Role and scope

- Role: CR271 Internal helper preflight only; not TURN-34A implementation owner, not reviewer, not parent approval.
- Read scope: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md` CR271 head, authoritative plan sections 14-19,
  HTTPS turn protocol, `docs/业务逻辑.md`, TURN-34A through Parent Review #1 true EOF, current
  `AutoCombatService.java`, current sole `AutoCombatServiceTurnContractTest.java`, and the four production callers
  `AutoBattleTask`, `FiveRingTaskV2`, `WubeiTask`, `XiuluoTaskV2`.
- Only this report is written. No Java/card/other-doc edit, no Maven/JUnit/compile/runtime/input, and no Git mutation.

## Snapshot

| Artifact | Current evidence |
|---|---|
| Cloud `AutoCombatService.java` | Parent-passed production SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`; state ownership and fingerprint replacement at `:728-781` |
| Cloud `AutoCombatServiceTurnContractTest.java` | Stable snapshot at `2026-07-16T08:46:03-04:00`: Repair-in-progress SHA-256 `60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6`, 763 lines; the 17 current `@Test` methods still end at `frozenPublicSurfaceIsPresent` (`:472`) |
| Parent Review #1 | `P0/P1/P2=0/1/0`; production source passed, named-test source blocked; Repair #1 is test-only |
| Four real callers | `AutoBattleTask:141-163`; `FiveRingTaskV2:1853-1859`; `WubeiTask:3595-3610,3756-3770`; `XiuluoTaskV2:1828-1831,2063-2084` |

The test now constructs real production collaborators and `PackagedTemplateAssets` (`:652-693`) plus a scripted
`CloudTurnCommandPort` (`:717-752`). Repair-in-progress scaffolding also loads the committed battle template and can compose
blank/positive PNG capture replies (`:500-611`). None of the current 17 `@Test` methods invokes that new positive fixture yet:
the executable cases still cover state isolation, STOP, polling, invalid-ROI uncertainty, public surface and the 30-second
team gate. There is therefore still no positive scripted capture/template/action or caller-consumption proof at this snapshot.

## Gap matrix

| Required proof | Production/API seam to drive | Reusable fixture in the same named test | Current gap / false-proof risk |
|---|---|---|---|
| `NONE -> IN_COMBAT -> EXIT_RECOVERED` | `handleCombatTick` (`:126-175`), `BattleRadarService.checkAndSyncCombatState`, enter/exit signal consumers (`:332-420`) | Existing real `BattleRadarService`, `PackagedTemplateAssets`, `battleFlagRoiPng`, `completedCapture`, `ScriptedCommandPort.enqueueCaptures` and exact metadata context | The positive fixture is now scaffolded but unused by every `@Test`; invalid ROI tests still only prove uncertainty. Mutating `GameContext` or asserting enum presence would bypass real template-driven signal creation and falsely prove the transition. |
| AutoBattle caller phase | `AutoBattleTask:141-163` consumes any non-`NONE` result before idle maintenance | Construct the real caller with current harness collaborators, or a test-private caller fixture that invokes the public task phase without copying its branch | Direct service calls cannot prove the task stops the same loop iteration or suppresses idle maintenance. |
| FiveRing caller phase | `FiveRingTaskV2:1853-1859` distinguishes `IN_COMBAT`, `NONE`, and recovered continuation | Use the real Task public phase with scripted service outputs produced through actual radar frames | A copied switch or mocked service return would not lock the real phase transition. |
| Wubei full/fast phases | `WubeiTask:3595-3610` full recovery and `:3756-3770` fast expected-exit path | Reuse one production harness and separate scripted frame sequences; assert the real phase result and deferred recovery state | One generic `handleCombatTick` case misses FAST policy and caller-specific `EXIT_RECOVERED` consumption. |
| Xiuluo full/fast phases | `XiuluoTaskV2:1828-1831,2063-2084`, policy selection `:2155-2164` | Reuse the same frame/command fixture with Xiuluo contexts and real phase methods | Testing only policy enum values is a surface check, not phase proof. |
| FAST `15s / 1s / 4s` cadence | `BattleRadarService` expected-exit watch + `nextCombatWakeDelayMs` (`AutoCombatService:301-313`) | Test-private controllable clock/scheduled timestamps already exposed by production APIs; scripted avatar-diff then full-radar fallback frames | Current `500..10000` clamp and `4000/2000/10000` dynamic polling tests do not prove 15s arm, 1s fast probe, or 4s full fallback. Sleeping would be flaky and is forbidden. |
| Enter `+4s` maintenance | `COMBAT_ENTRY_MAINTENANCE_DELAY_MS=4000`, signal handling `:332-341`, maintenance `:597-639` | Enter frame, then deterministic clock advance; record cleaner/panel calls | Setting timestamps by reflection or calling maintenance directly would bypass the signal-to-deadline contract. |
| Recovery policies | `FULL_RECOVERY`, `FULL_RECOVERY_WITH_LEADER_INCENSE`, `FAST_EXPECTED_EXIT`; `consumeExitAndRecover` (`:345-420`) and deferred consume (`:442-474`) | Script first-aid/incense/CommonBox collaborators and exact context; assert ordered collaborator events | Enum-name assertion does not prove action order, deferred leader recovery, or strong-terminal short-circuit. |
| CommonBox before first-aid | `runPendingMemberCommonBoxIfAllowed` (`:476-508`) precedes follower first-aid (`:510-566`) | Event recorder shared by existing real `CommonBoxService`, `PlayerStateService`, and scripted command port | Independent call counts can pass even if order regresses; assert one ordered event list and zero first-aid after terminal/uncertain CommonBox. |
| One re-probe and deferred leader clear | First-aid UNKNOWN/pending and deferred leader recovery branches (`:442-474,510-566`) | Script exact sequence UNKNOWN -> one re-probe -> confirmed result; then invoke public deferred consume API | Looping until success or manually clearing state would invent retry/cleanup and hide extra probes. |
| Maintenance `4s / 40s / 30s / 10s` | constants `:30-35`, scheduling `:252-291`, maintenance `:597-724`, team verify gate `:802-846` | Deterministic clock/state fixture plus cleaner/panel recorders | The current 30s gate test covers only sharing semantics; it does not prove 4s entry, 40s cleanup, 10s deferred-log throttle, or urgent 30s retry. |
| Panel `(489,726)` and `>20px` | `AutoCombatPanelService` target offsets `:69-70` and distance branch `:233` | Existing real panel service, exact window rect, packaged panel template and scripted CAPTURE/action replies | Reflection of constants or source scanning is false proof. Drive public panel verify/refresh and inspect emitted action coordinates/order. |
| Real battle/template frames | Real `PackagedTemplateAssets` and `BattleRadarService` in harness (`test:528-537`) | Load committed template bytes, compose deterministic full frame/ROI, return raw PNG from scripted CAPTURE | Merely constructing `PackagedTemplateAssets` proves nothing. At least one positive enter frame and one positive exit/non-battle frame must match production template code. |
| UUID/command 1:1 | `TurnGameClient` + `ScriptedCommandPort.execute` (`test:521-525,584-620`) | Extend recorder with action IDs and queued typed results; assert unique IDs and exact action count | `executeCalls==0` cases cannot prove per-command UUID freshness. Do not count candidate IDs or metadata reads as commands. |
| Terminal/uncertain projection | typed `COMPLETED`, `FAILED`, `STOPPED`, `DUPLICATE_OR_UNCERTAIN`, correlation/frame mismatch | Queue one typed result per representative action-capable branch; assert no later command/recovery/compensation | Throwing because no scripted reply remains only detects extras; it does not prove explicit failed/STOP/uncertain handling or zero retry. |

## Recommended fixture extensions inside the existing test only

1. Keep the current `Harness`, but add a test-private deterministic clock and ordered `events` recorder; do not add a
   production clock/test hook unless the existing public API cannot expose the boundary and the parent explicitly amends scope.
2. Extend `ScriptedCommandPort` with named typed replies and UUID collection. Preserve `latestWindowMetadata` as a
   read, not a command.
3. Reuse the newly added test-private `battleFlagRoiPng` / `completedCapture` frame composer and add the corresponding panel
   fixture with the actual packaged template. The helper methods alone are not coverage until a public production path consumes
   them. Do not stub template matching and do not read a sibling-repository fixture.
4. Use real public service/caller entry points. A copied reducer, mocked `AutoCombatService` return, reflection of private
   state/constants, source-string guard, or direct `GameContext` mutation as the transition trigger is not acceptance evidence.
5. Group cases by one production transition each so each failure identifies command order, frame, terminal, timing, or caller
   consumption. Do not create a second test file or modify the parent-passed production SHA.

## PRECHECK conclusion

- The current fixture is a usable base, but the named test still lacks the positive action/template path and all four real caller
  consumption proofs required by Parent Review #1.
- The highest false-proof risks are: enum/public-surface checks standing in for runtime transitions, invalid ROI standing in for
  real template frames, mocked service results standing in for caller phases, and zero-command cases standing in for UUID/terminal
  contracts.
- Repair #1 can remain inside the sole `AutoCombatServiceTurnContractTest.java`; no production or second-test write-set expansion
  is justified by this precheck.
- This report does not approve, block, close, or own TURN-34A.

<!-- TRUE_EOF: PRECHECK_COMPLETE CR271 TURN-34A REPAIR-1 NAMED-TEST GAP-MATRIX INTERNAL-HELPER NON-REVIEWER NON-OWNER SNAPSHOT-08:46 2026-07-16T08:47:00-04:00 -->
