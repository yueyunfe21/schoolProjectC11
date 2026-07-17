# Worker 1 Brief: A-2 Protocol Closure

## Goal

Turn `2026-07-12-thin-client-protocol-schema.md` and `2026-07-12-thin-client-security-key-lifecycle.md` into a self-contained, uniquely implementable protocol/security contract that closes B Final #11 items 2-3 and the protocol P2.

## Exclusive Write Scope

- `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`
- `docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md`
- `docs/superpowers/plans/reports/2026-07-12-a2-protocol-closure-report.md`

Do not edit Java, the shared draft, final design, migration matrix, data model, CR files, dashboard data, or any other file. Other workers are active; do not revert their changes.

## Binding Requirements

1. Preserve the approved boundary: cloud owns business decisions; local only validates and mechanically executes/rejects.
2. Remove every `同 vN`/`same vN` dependency and make both artifacts self-contained.
3. Freeze separate registries for frame signer algorithm and fenced-session algorithm. Include `signerKeyId`, trust-anchor selection, domain tag, algorithm ids, canonical signed bytes, and rejection behavior.
4. Define TLS exporter exactly: label `EXPORTER-DHXY-THIN-CLIENT-V1`, context input and SHA-256 canonicalization, output length `32`, hash `SHA-256`.
5. Define HKDF-SHA256 session key derivation inputs/outputs and explicit FENCE_ACK key confirmation. HELLO/FENCE_GRANT/FENCE_ACK must bind client/server nonce, device/session, connection fence, selected algorithms, protocol/build/allowlist/budget-policy hashes, and the TLS exporter.
6. V1 control WebSocket carries only CRITICAL and NORMAL. BULK image/ROI bytes use signed HTTPS upload grants. NORMAL signed bytes cap=`65536`; strict CRITICAL priority before a new NORMAL write; one already-started non-preemptible write hard cap=`250ms`, otherwise reconnect/resync. CRITICAL accepted-to-write target=`p99 <= 100ms` when no write is in progress; hard ceiling=`250ms` behind one in-progress frame.
7. Add `executionBudgetPolicyVersion` and `executionBudgetPolicyHash` to HELLO, FENCE_GRANT, FENCE_ACK, and ActionPlan. Freeze `MIN_CLOCK_SKEW_SAFETY_MS=5000` and `negotiatedSafety >= max(5000, measuredUncertaintyMs)`.
8. Define a versioned provider worst-case table with conservative numeric caps for focus, key/hotkey, move, click, callback overhead, per-step guard, and MATCH micro-sleep. Values must allow current real input sequencing; they are safety envelopes, not business timing changes.
9. Define `matchRecipeVersion=1`, canonical derived bundle bytes, SHA-256 `derivedBundleDigest`, exact CLICK/REPORT_ONLY outcome fields, and prohibit click derivation for REPORT_ONLY.
10. Give every message and lease ACK a unique wire type, exact payload, channel, scope, idempotency key, required/forbidden fields, and legal state edge. Successor remains a separate signed outbox ActionPlan.
11. No placeholders such as `TBD`, `TODO`, `实现者决定`, examples in place of complete registries, or conditional safety semantics.

## Verification

Run:

```powershell
rg -n "same v|同 v|TBD|TODO|实现者|按需|以后决定" docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md
git diff --check -- docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md docs/superpowers/plans/reports/2026-07-12-a2-protocol-closure-report.md
```

The first command must have no unresolved-placeholder matches; explain any literal compatibility-history match in the report. The second must exit `0`.

## Report Contract

Write the report file with: status `DONE`, exact changed files, closed requirement mapping 1-11, design choices, verification commands/results, current hashes, and any remaining concern. Return only a short status and report path.

