# DHXY Package Architecture

This document records the target package layout for the DHXY Java codebase. The goal is to make
request/result/model types easy to find without doing a full rewrite of the running automation
services.

## Package Rules

- `model`: cross-domain value objects and shared public request/result/spec types.
- `model.<domain>`: public DTOs for one domain, such as `model.npc` or `model.dialog`.
- `service`: Spring services and business orchestration only. Do not hide public request/result
  classes inside a service when other packages construct or consume them.
- `service.<domain>`: service-specific policies or service API objects that are tightly coupled to
  that domain's service boundary, such as dialog handle policy/result types.
- `vision`: screenshot, OCR, template matching, and visual learning services.
- `input`: physical input queue, input actions, and input serialization.
- `window`: window discovery, binding, runtime state, control, execution, diagnostics, and policies.
- `task`: task flows and task-local state machines.
- `debug`: standalone debug entry points only. Debug tools may call services but should not define
  reusable business models.
- `tools`: stateless helpers and calibration utilities. Spring components that mutate game state,
  capture active windows, or call OCR should eventually move to a service/domain package.

## Migration Policy

- Move public nested request/result/spec types out first when they are constructed outside their
  owning service.
- New public request/result/value objects should normally follow the repository convention in
  `AGENTS.md`: immutable data object, Lombok builder when callers need named construction, and
  enum values instead of cross-boundary strings. Existing records can be migrated separately when
  changing their accessor style will not distract from the behavior fix being tested.
- Keep private helper records/classes at the bottom of their enclosing file unless another package
  needs to reference them.
- Avoid package moves in the same edit as behavior changes. Each migration should compile by itself.
- Prefer domain model packages such as `model.npc`, `model.dialog`, and `model.navigation` over a
  flat pile of unrelated classes under `model`.
- Existing packages like `window.control`, `window.runtime`, and `input.action` may keep their
  domain-local request/result types because those packages are already cohesive.

## First Cleanup Targets

- `NpcClickRequest` -> `model.npc`
- `GreenTemplateClickSpec` -> `model.dialog`
- `OcrWordResult` / `LocationInfo` -> `model.ocr`
- `OcrLineResult`, `TargetOcrResult`, `TextCandidate`, `TextCandidateScanResult`,
  `TextCandidateScanStatus`, and `OcrWindowRegion` -> `model.ocr`
- `RecordResult`, `ResolvedNpcClickRegion`, and `LearnedNpcClickPoint` -> `model.ocr`
- `PlayerAnchorMatch` -> `model.ocr`
- `MiniMapSnapshot`, `TemplateLocationInfo`, `MapLabelTemplateMatch`, `ObjectiveTextResult`,
  and `PathingResult` -> `model.navigation`
- `DialogType` -> `model.dialog`
- `QuestDetailCapture` -> `model.quest`
- Later: internal persisted vision-memory classes currently nested in `OcrRoiMemoryService`
