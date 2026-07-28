# TURN-40F Post-Delivery Baseline Equivalence Gap Audit

## Scope

- Read-only baseline: `D:\mavenProject\DHXY`, branch `codex/baseline-696a12b0`, dirty local logic protected.
- Compared against CR client `D:\mavenProject\DHXY-cr271` and Cloud `D:\mavenProject\dhxy-cloud-brain`.
- No runtime/application/server/Task/UI/live capture/input was executed. Baseline was not written or built.

## Inventory

- Baseline production Java: 268 files.
- Baseline files whose basename exists in neither target repository: 30 files / 13,362 lines.
- Renamed/recomposed coverage exists for OCR, location, role metadata, remote lifecycle and running handles; basename absence
  alone is not treated as a defect.

## Confirmed Gaps

1. `PATHING_TERMINAL` settlement retry is missing after a transient HTTPS failure. Event transition dedupe is correct;
   pending transfer/world-map settlement must still retry on later terminal observations.
2. Enabled startup-window preparation is gone. Baseline map tracking, Alt+5, Alt+6 and startup flying/visibility guards have
   no new-turn production owner while the enabled property remains configured.
3. Manual map survey was removed from `MainWindowController` and has no Cloud replacement. Missing capabilities include map
   label sample, boundary/center/correction recording, projection tests, undo and calibration persistence.
4. Observer tests still exercise static helpers rather than the full production observer chain.

## Non-Gaps / Expected Renames

- Team role detection is now projected through Cloud startup authority/metadata rather than local OCR at task start.
- Local OCR sidecar startup is intentionally retired because Cloud owns OCR; it must not be restored as a second OCR owner.
- Local ready-event bus, thick task factory/runner and legacy running handle are intentionally retired.
- Dead window diagnostics classes with no baseline UI caller need not be restored.

## Required Boundary

- Cloud owns startup policy/order, OCR interpretation, map-survey calibration math/persistence and task truth.
- Client owns exact-HWND raw capture, fixed preprocessing, physical input, live mouse/manual samples and typed local execution.
- Existing HTTPS turn v1 remains the only task/action protocol. Permanent local Service count remains exactly four.

## Fresh-Runtime Data Cutover Gate

- The read-only baseline currently contains six learned-memory JSON files plus
  `config/map_camera_bounds.json`; the largest current file is `config/vision_memory.json` (4,982,770 bytes).
- TURN-40E deliberately performed zero copy/overwrite/import. Cloud now owns tenant-scoped memory files, but no evidence
  yet proves that these exact current baseline bytes have been imported into the selected tenant/user scope.
- This is not permission to copy workspace files during source repair. Before TURN-41 can become READY, the parent must
  verify schema compatibility, back up both sides, resolve the exact Cloud tenant/user storage root, perform a one-time
  import without overwriting the baseline, and verify record counts/hashes or a documented semantic projection.
- Map-survey Repair #4 must accept the existing `map_camera_bounds.json` schema; its actual current data import remains
  part of the same TURN-41 cutover gate.

<!-- TRUE_EOF: TURN-40F POST-DELIVERY-EQUIVALENCE-GAP-AUDIT COMPLETE REPAIR-4-REQUIRED 2026-07-20 -->
