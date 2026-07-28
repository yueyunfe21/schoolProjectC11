# CR271 PATHING-FACT-TIMELINE-P1

## Status

`SOURCE+TEST REVIEW PASSED / P0-P1-P2=0/0/0 / FRESH CLIENT RESTART REQUIRED`

## Fresh Evidence

- Task run: `remote-turn-2cd4bcf2-2cc2-43b1-a776-668c8f714f41:0:XIULUO_V2`
- Pathing intent: `274832d7-30d4-4de6-b7be-109a38ba430f`
- `10:31:57.839`: local pathing start proof registered for `灵兽村 (112,93)`.
- `10:31:59.767`: Cloud rejected observation because
  `pathingFact.pathingUpdatedAtMs must not precede pathingStartedAtMs`.
- `10:32:01.281`: Client Runner observed `STOPPED_AWAY`, stable for `2938ms`.
- Later requests were still rejected because
  `pathingFact.locationChangedAtMs must be zero or within the pathing interval`.

The Client reached the correct terminal state, but Cloud never received it and therefore never advanced
to NPC click.

## Root Cause

`WindowObservationSampler.collectBound()` captured the observation-cycle timestamp before the task thread
registered a new pathing intent. `refreshLocalPathingTerminal(...)` then used that stale cycle timestamp
for the newer intent, creating a pathing fact whose timestamps ran backwards.

## Repair

- Acquire a fresh observation timestamp after reading the exact active intent.
- Never stamp an intent earlier than its `createdAtMs`.
- Preserve one stable `pathingStartedAtMs` for the same intent.
- Normalize updated, movement, location and blocking timestamps into the same pathing interval.
- Keep clear transitions monotonic.
- Preserve the existing stop threshold, capture ROI, navigation and NPC business behavior.

## Verification

- `mvn -q "-Dtest=WindowObservationRunnerContractTest" test`: `11/11`, exit `0`.
- `mvn -q -DskipTests compile`: exit `0`.
- No runtime, UI, capture or physical input was started.

## Fresh Runtime Gate

Restart the Client. Cloud has no source change for this repair. After local `STOPPED_AWAY`, the observation
request must be accepted for the same intent and Cloud must advance to NPC click without either pathing
timeline validator error.
