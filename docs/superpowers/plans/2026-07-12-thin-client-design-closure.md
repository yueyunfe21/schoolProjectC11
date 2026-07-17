# Thin Client Design Closure Implementation Plan

> **Status: Superseded.** The user chose direct lift-and-shift on 2026-07-12. The bytecode/method-inventory gate and pre-implementation Final Proposed requirement in this document are no longer active. Retained only as design-history context; implementation follows the direct migration plan.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. This plan is intentionally executed inline; do not create or wait for Agent A.

**Goal:** Close every B Final #11 blocker, complete A-1 design-time gates 1-5, and produce a self-contained `Final Proposed` architecture plus an executable migration plan without changing Java business behavior.

**Architecture:** Preserve the approved cloud-authoritative/Thin Client boundary and turn every security-critical protocol and persistence rule into a unique, mechanically checkable contract. Build a reproducible inventory from the current source tree, then integrate protocol, data, inventory, acceptance, and migration sequencing into one frozen design package.

**Tech Stack:** Markdown, PowerShell 7, `rg`, Git, JDK `javap`/compiler metadata, Maven compile gate where bytecode inventory is required.

## Global Constraints

- Do not edit Java business implementation during design closure.
- Do not change §§3-10 hard boundaries or user-approved baseline behavior.
- Do not call unfinished items `Final Design`, `Final Proposed`, `PASS`, or `CLOSED`.
- A-1 gates 1-5 are design-time requirements; build allowlist evidence and fresh runtime remain implementation-time gates.
- No local automated tests or source guards are introduced; verification uses static analysis, hashes, manifests, `git diff --check`, and the mandatory compile gate only when bytecode inspection is used.
- Preserve unrelated dirty-worktree changes and never revert user files.

---

### Task 1: Retire the A/B Process and Freeze the Solo Acceptance Checklist

**Files:**
- Modify: `docs/superpowers/specs/2026-07-12-full-cloud-thin-client-architecture-draft.md`
- Create: `docs/superpowers/plans/2026-07-12-thin-client-design-closure.md`

**Interfaces:**
- Consumes: B Final #11 P1/P2 findings and the user's single-thread override.
- Produces: One ordered checklist that later tasks must close without waiting for Agent A.

- [x] **Step 1: Record the process override in the shared design history.**
- [x] **Step 2: Convert B Final #11 into the six task groups in this plan.**
- [ ] **Step 3: Verify document formatting.**

Run:

```powershell
git diff --check -- docs/superpowers/specs/2026-07-12-full-cloud-thin-client-architecture-draft.md docs/superpowers/plans/2026-07-12-thin-client-design-closure.md
```

Expected: exit code `0`; line-ending warnings are allowed, whitespace errors are not.

### Task 2: Close A-2 Protocol, Authentication, Transport, and Execution Safety

**Files:**
- Modify: `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`
- Modify: `docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md`
- Modify: `docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md`

**Interfaces:**
- Consumes: B Final #11 items 2-3 and the existing Envelope/ActionPlan model.
- Produces: Unique wire schemas and constants sufficient to generate codecs, validators, and executor safety checks.

- [ ] **Step 1: Define the complete cryptographic registry and transcript.**

Freeze domain/algorithm identifiers, `signerKeyId`, trust-anchor selection, TLS exporter label/context/length/hash, HKDF inputs/outputs, session key confirmation, and separate frame-signer versus fenced-session algorithm fields. Reject unknown or downgraded identifiers before payload parsing.

- [ ] **Step 2: Remove transport ambiguity.**

Forbid BULK payloads on the control WebSocket in V1; carry frames/ROI through signed HTTPS upload grants. Bound NORMAL control frames to `65536` signed bytes, schedule CRITICAL before NORMAL, cap a non-preemptible write at `250ms`, and reset/resync the connection if the cap is exceeded. Set the CRITICAL accepted-to-write SLO to `p99 <= 100ms` when no write is already in progress and record the single-frame `250ms` hard ceiling.

- [ ] **Step 3: Freeze execution-budget policy.**

Add `executionBudgetPolicyVersion` and `executionBudgetPolicyHash` to HELLO, FENCE_GRANT, FENCE_ACK, and every ActionPlan. Define conservative local worst-case constants for focus, hotkey/key, move, click, callback overhead, per-step checks, and fixed MATCH micro-sleep; set `MIN_CLOCK_SKEW_SAFETY_MS=5000` and require `negotiatedSafety >= max(5000, measuredUncertaintyMs)`.

- [ ] **Step 4: Complete MATCH canonicalization.**

Define `matchRecipeVersion=1`, fixed move/sleep/click derivation, canonical encoded bundle fields, SHA-256 `derivedBundleDigest`, and required outcome fields for CLICK and REPORT_ONLY. REPORT_ONLY must not derive a click bundle.

- [ ] **Step 5: Complete the message registry.**

Give each lease command and ACK a unique type, exact payload, request/message idempotency key, channel class, scope, required/forbidden fields, and legal state edge. Define all rejection codes and successor delivery as a separate signed outbox message.

- [ ] **Step 6: Cross-check A-2 and A-6.**

Run:

```powershell
rg -n "same v|同 v|TBD|TODO|实现期确定|SIG_ALG|sessionAlg|signerKeyId|TLS exporter|HKDF|key confirmation|executionBudgetPolicy|LEASE_.*ACK|matchRecipeVersion|derivedBundleDigest" docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md
```

Expected: no inherited-version placeholders; every required identifier appears consistently in all three artifacts.

### Task 3: Close A-3 Relational Constraints and Durable Fact Semantics

**Files:**
- Modify: `docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md`
- Modify: `docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md`

**Interfaces:**
- Consumes: Task 2 envelope/action identifiers and B Final #11 items 4-5/P2.
- Produces: A semantic DDL contract that prevents cross-tenant/window/device/lane/epoch references and supports exact replay/recovery.

- [ ] **Step 1: Expand identity and lease columns.**

List `tenant_id`, `device_id`, `window_registration_id`, `window_incarnation`, `binding_generation`, `lane_id`, `lease_id`, and `lease_epoch` explicitly wherever referenced. Add the active-window partial unique constraint and the composite lease unique key/FKs required to prove same device/lane/epoch.

- [ ] **Step 2: Make outbox replay byte-exact.**

Persist complete immutable wire-frame bytes plus signer key id, signer algorithm, session algorithm, detached signature bytes, signed-envelope digest, and issued sequence identity. Retries must resend the stored frame verbatim and may not rebuild expiry or signatures.

- [ ] **Step 3: Close upload-grant consumption.**

Bind tenant/device/window registration/incarnation/binding generation/frame/CaptureSpec/expiry, atomically consume once, and validate returned hash, encoded byte count, decoded dimensions, pixel format, and encoding before creating FRAME_META.

- [ ] **Step 4: Close memory publication and late-resolution rules.**

Require trusted publisher, parent lineage, owner/scope, kind/context, exact memory-use/version composite FK, and immutable promotion inserts. Preserve UNKNOWN while atomically accepting only the first correctly fenced late outcome; same digest is idempotent and a different digest conflicts.

- [ ] **Step 5: Define every protocol fact and object reference.**

Create a complete fact-kind table containing natural identity fields, canonicalization version, digest input, scope, and one-to-one causation rules. Enumerate task terminal states, command values, frame-basis required/forbidden checks, legal object referrer tables/FKs, and GC lock order.

- [ ] **Step 6: Expand configuration, audit, and evidence entities.**

Give policy/quota/SLO/verifier/budget tables immutable version keys and tenant/global scope rules; define audit/evidence manifest FKs, uniqueness, signing identity, and transaction boundaries.

- [ ] **Step 7: Scan for unresolved schema prose.**

Run:

```powershell
rg -n "例如|等|同 v|same v|TBD|TODO|复合 FK 到|按需|实现者" docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md
```

Expected: security/identity/state constraints do not rely on examples or unspecified implementer choices.

### Task 4: Complete A-1 Design-Time Gates 1-5 Mechanically

**Files:**
- Create: `scripts/generate-thin-client-inventory.ps1`
- Modify: `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`
- Create: `docs/superpowers/specs/2026-07-12-thin-client-source-manifest.json`
- Create: `docs/superpowers/specs/2026-07-12-thin-client-resource-manifest.md`

**Interfaces:**
- Consumes: Current source/resource tree and the target-owner/disposition vocabulary from the final design.
- Produces: Complete production-reachable method/resource inventory with zero unknown nodes and objective row-level acceptance conditions.

- [ ] **Step 1: Freeze the analyzed source identity.**

Record branch, HEAD, dirty file list, and SHA-256 for every `src/main/java` and `src/main/resources` file in the JSON manifest. The baseline identity is the manifest hash, not HEAD alone.

- [ ] **Step 2: Enumerate all production methods and dynamic entry points.**

The generator must include constructors, private/public/protected/package methods, inherited/default implementations, lambdas, anonymous listeners, scheduled methods, Spring bean/conditional/event handlers, JavaFX/FXML handlers, reflection strings, task factories, runner entries, and native callbacks.

- [ ] **Step 3: Build the production-reachability closure.**

Seed roots from Spring Boot/JavaFX main classes, controllers/FXML, schedulers/listeners, task factories/runners, public cloud/UI command entries, and native callbacks. Every unresolved dynamic edge must map to an explicit registration/config/resource row; unknown nodes must equal zero.

- [ ] **Step 4: Run the reverse semantic scan.**

Map every local OCR/template/threshold/ROI/business enum/phase/retry/fallback/timer/memory/navigation/dialog/task-choice hit to a method or resource row. A hit may remain local only when its disposition is safety/mechanical and cites the hard-boundary rule.

- [ ] **Step 5: Inventory all resources.**

Assign owner/disposition/consumer to Spring properties/YAML, FXML, images/templates, JSON, ROI/threshold tables, aliases, reflection strings, scripts, native resources, and persistence files. No resource may remain unowned.

- [ ] **Step 6: Require complete row fields.**

Each matrix row must contain current owner, target owner, local disposition, dependencies, baseline identity, implicit state, production roots, inbound/outbound edges, and an objective deletion/acceptance predicate.

- [ ] **Step 7: Verify gate counts.**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-thin-client-inventory.ps1 -Verify
```

Expected: `unknownNodes=0`, `unownedResources=0`, `unmappedSemanticHits=0`, `rowsMissingAcceptance=0`; method/resource totals and manifest hash are printed and copied into A-1.

### Task 5: Integrate and Self-Review the Final Proposed Design

**Files:**
- Modify: `docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md`
- Modify: `docs/superpowers/specs/2026-07-12-thin-client-master-acceptance-matrix.md`
- Modify: `docs/superpowers/specs/2026-07-12-full-cloud-thin-client-architecture-draft.md`

**Interfaces:**
- Consumes: Completed Tasks 2-4.
- Produces: One self-contained design package with design-time gates PASS and implementation-time gates explicitly NOT_EVALUATED.

- [ ] **Step 1: Import exact protocol/data/inventory contracts into the final design or content-hash referenced appendices.**
- [ ] **Step 2: Populate every REQ-M row in the master acceptance matrix.**
- [ ] **Step 3: Mark only A-1 gates 1-5 as PASS; retain allowlist build and fresh runtime as NOT_EVALUATED.**
- [ ] **Step 4: Run placeholder, cross-reference, and whitespace checks.**

Run:

```powershell
rg -n "TBD|TODO|same v|同 v|以后决定|实现者自行|A/B 双代理终审全部修正" docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md docs/superpowers/specs/2026-07-12-thin-client-*.md
git diff --check -- docs/superpowers/specs docs/superpowers/plans scripts/generate-thin-client-inventory.ps1
```

Expected: no unresolved design placeholders and no whitespace errors.

- [ ] **Step 5: Freeze artifact hashes and record the solo final-review conclusion.**

Do not claim `Final Proposed` unless Tasks 2-5 have no unresolved P0/P1/P2.

### Task 6: Produce the Migration Implementation Plan

**Files:**
- Create: `docs/superpowers/plans/2026-07-12-thin-client-migration-implementation.md`

**Interfaces:**
- Consumes: Final Proposed architecture and complete A-1 dependency graph.
- Produces: Ordered S1-S9 implementation tasks with exact local/cloud file ownership, contracts, compile gates, shadow/replay evidence, cutover, rollback, and deletion prerequisites.

- [ ] **Step 1: Split implementation into protocol/foundation, state store, Thin Client executors, cloud service moves, vision/memory, scheduler, observability, validation, cutover, and deletion tasks.**
- [ ] **Step 2: Give every task exact files/interfaces/dependencies and mandatory compile commands.**
- [ ] **Step 3: Preserve baseline phase order, retry/fallback, turn ownership, stop/pause, and click/navigation semantics unless a separately approved behavior CR exists.**
- [ ] **Step 4: Bind each task to acceptance requirement ids and rollback evidence.**
- [ ] **Step 5: Run `git diff --check` and a requirement-to-task coverage scan before handoff.**
