# Map Label Template Size Report

Updated: 2026-06-05

Scope: `images/template/map_label/*.png`

## Runtime Capture Contract

`syncMyPosition()` reaches the mini-map fast path through:

`PlayerStateService.syncMyPosition()` -> `LocationVisionService.scanCurrentLocation()` -> `MiniMapCoordinateReader.readCurrentTemplateLocation()`.

The fast path captures one mini-map coordinate strip:

| Owner | Window-relative rect |
| --- | --- |
| `MiniMapCoordinateReader.captureCoordinateStrip()` | `x=46, y=59, w=178, h=35` |
| `LocationVisionService.captureCurrentLocationStrip()` OCR fallback | `x=46, y=59, w=178, h=35` |
| `MapSurveyService.captureCoordinateStrip()` sample saving/testing | `x=46, y=59, w=178, h=35` |

This means runtime matching, OCR fallback learning, and UI sample saving must all produce templates from the same coordinate strip. Do not introduce a second map-label crop area unless this contract is changed deliberately.

## Canonical Template Sizes

Map-label templates are normalized by `MiniMapCoordinateReader.normalizeMapLabelTemplateImage(...)` and runtime crops go through the same normalization path.

| Chinese characters | Canonical size |
| ---: | --- |
| 2 | `30x18` |
| 3 | `43x18` |
| 4 | `56x18` |
| 5 | `69x18` |

The width buckets still accept small natural extraction variance before padding, but saved templates should end at exactly the canonical size above.

## Current Audit

Total files: 47

| Size | Count |
| --- | ---: |
| `30x18` | 4 |
| `43x18` | 19 |
| `56x18` | 18 |
| `69x18` | 6 |

Current check result:

- `ALL_MAP_LABEL_SIZES_OK`
- `ALL_SELF_MATCH_OK`
- `mvn -q -DskipTests compile` passed

## Archive

Before canonicalizing existing templates, the previous files were copied to:

`images/archive/map_label_legacy/20260605-124217/`

An earlier single-file `北俱芦洲` backup also exists under:

`images/archive/map_label_legacy/20260605/`

## Important Notes

- `北俱芦洲.png` is now a 4-character template at `56x18`. Old reports showing it as `69x18` were from the bracket-leak bug before normalization.
- `readMapLabelTemplates()` loads only PNG files directly under `images/template/map_label`, so archive directories outside that folder are safe.
- If new map-label templates are learned automatically from OCR fallback, `LocationVisionService.learnMissingMapLabelTemplate(...)` now routes them through the same normalization and exact-size plausibility check.
