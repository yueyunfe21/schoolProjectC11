# Worker 2 Brief: A-3 Data Model Closure

## Goal

Turn `2026-07-12-thin-client-state-data-model.md` into a self-contained semantic DDL contract that closes B Final #11 items 4-5 and the data-model P2.

## Exclusive Write Scope

- `docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md`
- `docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md`
- `docs/superpowers/plans/reports/2026-07-12-a3-data-closure-report.md`

Do not edit Java, the shared draft, final design, migration matrix, protocol/security artifacts, CR files, dashboard data, or any other file. Other workers are active; do not revert their changes.

## Binding Requirements

1. Preserve tenant RLS and cloud-authoritative semantics; no local business fallback.
2. Remove every `同 vN`/`same vN` dependency and make the data contract self-contained.
3. Explicitly list identity columns and exact composite keys/FKs. `window_registration` must contain `tenant_id`, `device_id`, `window_registration_id`, `window_id`, `incarnation`, and active uniqueness on `(tenant_id,device_id,window_id)`. `task_run`, current lease, lease entity, outstanding action, and action ledger must prove same tenant/device/window/lane/lease/epoch with composite unique keys and FKs.
4. Use one mutable `authority_current` row per scope with CAS and append-only authority events/transfers; no impossible append-only partial-unique active-state design.
5. Persist byte-exact outbox replay material: complete immutable wire-frame bytes plus signer key id, frame signer algorithm, session algorithm, detached signature bytes, signed-envelope digest, and issued fence/class/stream/sequence identity. Retry sends stored bytes verbatim.
6. Upload grant must bind tenant/device/window registration/incarnation/binding generation/frame/CaptureSpec/expiry and atomically consume once. Upload completion validates content hash, encoded byte count, decoded dimensions, pixel format, and encoding before FRAME_META.
7. Memory verdict must use exact composite FK to its memory use/version. Publication requires trusted publisher, immutable parent lineage, owner/scope/kind/context consistency, verifier policy, and insert-only promotion.
8. UNKNOWN stays historical. The first correctly signed/fenced late outcome is atomically recorded; same digest is idempotent and different digest conflicts.
9. Enumerate every `protocol_fact` kind with exact natural-identity fields, canonicalization version, digest input, scope, and one-to-one/causation rules. Do not use `例如` as the specification.
10. Enumerate task terminal states, command values, frame-basis required/forbidden checks, memory lineage checks, object-reference legal referrer tables/FKs, reference lock order, and GC zero-reference/legal-hold transaction.
11. Fully define immutable policy/quota/SLO/verifier/budget tables, audit events, evidence manifests, tenant/global scope rules, FKs, unique constraints, and transaction boundaries.
12. Redis keys and object prefixes must always include authenticated tenant/device/window scope; payload cannot choose namespaces. DR restoration must preserve correctness RPO=0 and exact replay/fence invariants.
13. Physical SQL types, fillfactor, and measured quota/SLO values may remain `impl-tuning-only`; semantic fields/keys/transitions may not.

## Verification

Run:

```powershell
rg -n "same v|同 v|TBD|TODO|复合 FK 到|例如|实现者|按需|以后决定" docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md
git diff --check -- docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md docs/superpowers/plans/reports/2026-07-12-a3-data-closure-report.md
```

The first command must have no unresolved-placeholder/example-as-contract matches; explain any historical match in the report. The second must exit `0`.

## Report Contract

Write the report file with: status `DONE`, exact changed files, closed requirement mapping 1-13, semantic DDL choices, verification commands/results, current hashes, and any remaining concern. Return only a short status and report path.

