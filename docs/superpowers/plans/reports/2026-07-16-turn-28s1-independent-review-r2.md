# CR271 TURN-28S1 Independent Delivery Review R2

## Verdict

**APPROVED**

- `P0 = 0`
- `P1 = 0`
- `P2 = 0`
- Review snapshot: `2026-07-16T09:04:14.013-04:00`
- Role: independent delivery reviewer R2; independent of the implementation worker, parent, and R1. I did not read or rely on the R1 report body.

No blocking or non-blocking defect was found in the frozen TURN-28S1 delivery scope.

## Authority And Scope Read

Read before judgment:

1. `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the mandatory baseline gate in `docs/业务逻辑.md:215-224`.
2. The applicable NPC Click contract in `docs/业务逻辑.md:1301-1380`, including local mechanics, Cloud strategy ownership, verifier-only success, and the prohibition on an implicit fallback/retry decision.
3. Parent card `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28.md`, including the strict-696 public API freeze, Frozen 696 behavior item 8, and TURN-28S1 decomposition.
4. Frozen slice card `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S1.md`, including all five implementation conditions and the delivery true EOF.
5. `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` in full and the relevant TURN-28/protocol sections of `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:92-230,761-767,1295-1297,1639`.
6. Cloud `NpcClickService.java` in full. Its bytes are identical to both the Cloud migration mirror and the authoritative DHXY git object at `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

The review covered only TURN-28S1. It does not approve the remaining TURN-28 HTTPS cutover, named test, integration, build, or runtime gates.

## SHA And Baseline Proof

| Artifact | Lines | SHA-256 | Git blob |
|---|---:|---|---|
| Current Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java` | 3374 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` | `74d9b26b76b84052718d5679529f7ffeb46e3273` |
| Cloud `migration-baseline/696a12b0/.../NpcClickService.java` | 3374 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` | `74d9b26b76b84052718d5679529f7ffeb46e3273` |
| DHXY git `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/NpcClickService.java` | authoritative object | n/a | `74d9b26b76b84052718d5679529f7ffeb46e3273` |

Independent byte comparison returned `BYTE_EQUAL=True`. Therefore current Cloud production is not merely behaviorally similar: it is the exact authoritative 696 NpcClick blob. This also proves TURN-28S1 did not begin the remaining TURN-28 migration.

## Line-Level Evidence

### 1. Public `confirmExpectedOptionProof` is preserved

- `NpcClickService.java:2270-2276` retains `@Override` and the exact six-argument public signature:
  `String sourceTask, String actionKey, String matchedText, String proofToken, String verificationStrength, String reason`.
- `SmartClickEvidenceConfirmationService.java:8-13` declares the same parameter order.
- Current Cloud `DialogService.java:1541-1550` still reads the proof token from the bound `WindowRuntimeContext` and calls the public method in that exact order.
- In `NpcClickService.java:2277-2294`, `sourceTask` is used only in diagnostics at `:2284` and `:2290`; there is no equality gate or early rejection based on it.

### 2. Pending-proof fences remain intact

- Pending creation remains gated by a real click sample at `NpcClickService.java:1294-1344`; `:1344-1354` creates the evidence, stores it under the current-window key, and publishes its fresh UUID proof token.
- `PendingSmartClickEvidence` at `:2056-2070` retains source strategy, map, player coordinates, NPC name, target coordinates, window base, click point, tune values, expected templates, proof token, and message. It has no `sourceTask` field or timestamp/expiry state.
- Constructor/from wiring at `:2072-2124` retains those facts and creates one fresh `UUID` proof token at `:2123`; no normalized task source is wired.
- Exact map/name/coordinate comparison remains at `:2141-2145`.
- Expected option proof remains at `:2148-2155`: requests with expected templates require an exact normalized matched template; template-less requests retain the baseline nonblank action/text rule.
- Proof-token equality and nonblank checks remain at `:2157-2161`.
- Explicit map/name/coordinate confirmation at `:2253-2267` still removes mismatched pending evidence and records only a match.
- Public expected-option confirmation at `:2277-2294` keeps the order `current-window pending -> exact proof token -> expected option proof -> remove -> record`. Token mismatch returns without removal (`:2282-2286`); option mismatch removes the pending record (`:2287-2291`); a match removes and records (`:2293-2294`).
- Token publication/clear remains bound to the current `WindowRuntimeContext` at `:2297-2305`.
- Confirmed memory still records the pending map/name/player coordinates/target coordinates/window base/click point at `:2308-2341`.
- The pending map key remains the bound `windowId` at `:2344-2347`, preserving the baseline window fence.

### 3. All 16 `request.sourceTask()` uses remain

Exact occurrence count is `16`, byte-identical to 696:

| Current lines | Role |
|---|---|
| `:615`, `:620`, `:665`, `:678`, `:707`, `:804` | Existing task-context diagnostics around generic retry, direct-combat preflight/skip/exit, and pipeline entry. |
| `:812` | Business branch preserved: Wubei does not take the early learned-memory-before-name-layer path. |
| `:849` | Business branch preserved: Wubei executes tooltip-first. |
| `:893` | Business branch preserved: only non-Wubei executes the later normal-tooltip position. |
| `:1155`, `:1164`, `:1194`, `:1215`, `:1228`, `:1250`, `:1255` | Existing tooltip skip/scan/click/result/exhaustion/not-found diagnostics. |

The three Wubei decisions are unchanged; none was confused with or removed alongside the post-baseline pending-evidence symbol of the same name.

### 4. Removed symbols and no replacement gate

Current production counts are all zero:

- `PendingSmartClickEvidence.sourceTask = 0`
- `matchesSourceTask = 0`
- `normalizeSourceTask = 0`
- `pendingSourceTask = 0`
- `java.util.Locale = 0`
- `Locale.ROOT = 0`

Within the complete pending-proof subsystem `NpcClickService.java:2056-2306`, case-insensitive counts are also zero for `ttl`, `expiry/expire`, `session`, `owner`, `permit`, `ledger`, `retry`, `schedule`, `timer`, `createdAt`, and `timestamp`. The three `sourceTask` tokens in that slice are only the preserved public parameter and two log arguments. Existing retries elsewhere in the 696 service are baseline click mechanics/business behavior, not a TURN-28S1 replacement fence.

## Review Boundaries

- No Java, original CR/card, protocol, plan, dashboard, baseline mirror, test, or other documentation was modified.
- Only this R2 report was created.
- No Maven, JUnit, compile, package, runtime, application/server, Task, UI, capture, or physical input was run.
- No Git mutation was performed. Git usage was read-only (`status`, `rev-parse`, and `hash-object` without `-w`) for provenance checks.

## Final Judgment

**APPROVED / P0/P1/P2 = 0/0/0.** TURN-28S1 exactly restores the `696a12b0` pending-proof condition, preserves the public API and all 16 request-level task-source uses, retains every baseline pending-proof fence, and introduces no substitute TTL/session/retry or other unapproved state gate.

**无已批准业务差异；按 `696a12b0` 等价恢复 pending-proof 条件。**

<!-- TRUE_EOF: CR271 TURN-28S1 INDEPENDENT DELIVERY REVIEW R2 APPROVED P0/P1/P2=0/0/0 SHA256=cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441 BLOB=74d9b26b76b84052718d5679529f7ffeb46e3273 2026-07-16T09:04:14.013-04:00 -->
