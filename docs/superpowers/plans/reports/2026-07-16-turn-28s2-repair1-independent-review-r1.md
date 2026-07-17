# TURN-28S2 Repair #1 Independent Whole-Card Delivery Review R1

## Verdict

- Role: independent whole-card delivery reviewer R1; not the implementation Worker and not the parent final reviewer.
- Verdict: **APPROVED**.
- Severity: `P0/P1/P2=0/0/0`.
- Reviewed frozen production: Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`, 3527 lines,
  SHA-256 `aa50ae7cb6fd9fe5c494225090ec123742d67c1faea9d154e7e01bafb1a72862`.
- Business baseline: strict `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no approved business difference.

## Review Scope And Authority

Read in full or in the card-mandated relevant scope: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current CR271 top
block in `docs/ACTIVE_WORK.md`, authoritative plan sections 14-19, the HTTPS turn protocol design,
`docs/业务逻辑.md`, the complete TURN-28S2 card through its physical true EOF, current Cloud production, the
strict-696 mirror, and both repositories' status. The worktrees were already dirty/untracked and were treated as
protected. This review modified no Java, card, plan or status document and performed no Git mutation.

## Evidence

### Exact strict-696 cutover boundary

- The current file differs from the byte-identical strict-696 starting mirror only by the imports/timeout and one
  private HTTPS mechanics boundary needed for this card, plus replacement of exactly four active top-level sites.
- `NpcClickService.java:638` preserves generic second-pipeline ordering and fallback while replacing only
  `ALT_C -> WAIT 700ms` with `executeAltShortcutTurn(...)`.
- `NpcClickService.java:678-695` preserves the confirmed-`FLYING` gate, `UNKNOWN` branch, direct-combat ordering and
  existing skip paths while replacing only `ALT_C -> WAIT 700ms` and `ALT_A -> WAIT 350ms`.
- `NpcClickService.java:950-960` preserves the ordinary pipeline name-layer preparation boundary and existing
  `400ms` constant while replacing only `ALT_4 -> WAIT 400ms`.
- `executeAltShortcutTurn(...)` has exactly four call sites. The two excluded legacy private
  `inputProvider.pressAlt4()` sites remain present and unchanged in the clean-name helpers.

### One public HTTPS action, one UUID, ordered mechanics

- `NpcClickService.java:3281-3307` builds exactly two ordered steps: index 0 `INPUT/KEY_TAP` with only the requested
  Alt key, then index 1 `WAIT` with the frozen baseline delay; it requests no frame and calls public
  `TurnGameClient.execute(...)` exactly once per reached site.
- `TurnGameClient.execute(...)` delegates to one `invoke(...)`; `invoke(...)` obtains one
  `actionIdSupplier.get()` UUID and calls `commandPort.execute(...)` once. It owns no retry, cache, lifecycle or
  business-success interpretation.
- No retry/replay/resend/session/ledger/TTL/durable workflow, second command, local OCR truth or new business
  decision was introduced by the TURN-28S2 diff.

### Exact current metadata and closed terminal projection

- `latestExactWindowMetadata(...)` reads the latest metadata for the bound `deviceId/windowId`, rejects missing or
  mismatched identity and invalid rectangles. The returned outcome must equal that complete immutable
  `TurnWindowMetadata` record, thereby correlating device/window/title/native HWND/process/rect and pause/stop
  bits to the immediately read snapshot.
- Non-`COMPLETED` command statuses, including BUSY, duplicate action id, timeout uncertainty and interruption
  uncertainty, cannot return to a caller. An interruption first checks the real task checkpoint; an unconfirmed
  case is fatal.
- `STOPPED` checks the real task context and is fatal when stop is not confirmed.
  `DUPLICATE_OR_UNCERTAIN`, unsupported status, metadata drift, step count/index/type/status mismatch and an
  unexpected frame are fatal. Thus none can reach a later strict-696 action.
- `TurnInvocationResult` validates protocol shape, action-id correlation, submitted device/window identity,
  exact step correlation and frame pairing before this service receives a completed invocation.
- Repair #1 retains legal `FAILED` shape validation (`COMPLETED* -> FAILED -> NOT_RUN`) and then, at
  `NpcClickService.java:3384-3387`, always throws `npcClickFatal(...)`. `executeAltShortcutTurn(...)` therefore
  returns only after correlated `COMPLETED/COMPLETED`; it never converts a remote failure into a legacy boolean
  skip/fallback. JavaDoc now states this exact contract.

## Residual Gates

- This source-only card has no test write set and does not by itself satisfy the TURN-28 parent named-test gate.
- Per instruction, no Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input, or Git
  mutation was run. Build and authorized named-test gates remain the parent's stable-writer responsibility.
- No reviewer-side repair is requested. Parent final judgment and the second independent whole-card review remain
  separate gates.

<!-- TRUE_EOF REVIEW_COMPLETE -->
