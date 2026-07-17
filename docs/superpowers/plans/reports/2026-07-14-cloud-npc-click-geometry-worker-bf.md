# W-NPCCLICK-GEOMETRY-CPU-IMP1 - Internal Worker BF

## CLAIMED

- Worker: `Internal Worker BF`
- Claimed at: `2026-07-14T03:21:49.6416872-04:00`
- Role: implementation only; no review or approval performed.
- Business baseline: DHXY committed `0114604e:src/main/java/com/bot/dhxy/service/NpcClickService.java`.
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`.
- Report write set: this file only.
- Initial repositories were already dirty and were preserved:
  - DHXY: branch `thin-client-design`, 34 tracked dirty entries, 16 untracked entries.
  - Cloud: branch `navigation-migration`, 7 tracked dirty entries, 15 untracked entries.
- No Git mutation, cleanup, rollback, overwrite of unrelated work, runtime start, capture, or input was performed.

## Implementation #1

- Added only the directly required imports: `Point`, `List`, `ArrayList`,
  `NpcClickSmartCloudRequest`, `OcrWindowRegion`, and `ResolvedNpcClickRegion`.
- Mechanically copied the two `1024x768` constants and six requested private static
  geometry/scan-region helpers from committed `0114604e`.
- Updated only the class JavaDoc to state that the copied constants/helpers are not wired to any caller.
- Added no caller, public API, wrapper, owner/session/ledger/TTL/retry, remote dispatch,
  capture/template/OCR/input/pathing mechanics, or executable integration.
- Existing approved methods/types retained their executable tokens unchanged.

## File SHA

- Baseline Git blob SHA-1: `cc858482e31ee4a352f59895054452ca28b61d6a`.
- Cloud target SHA-256 before Implementation #1:
  `8F67CE008B0E62F21458C506C4DCB3BA9BE6DAB945701E9A25A2E8A68C7B6560`.
- Cloud target SHA-256 after Implementation #1:
  `4CDC0FB63067ACD28D6DE1D54AC8EF88B67C8EF2E750944077A9FFE0E500E888`.

## Eight Exact Source/Target Blocks

Raw LF-preserving source/target block extraction was compared case-sensitively. All eight blocks have
identical source and target SHA-256 values, so each exact diff is `0`.

| Block | Source SHA-256 | Target SHA-256 | Exact diff |
|---|---|---|---|
| `WINDOW_WIDTH` | `6A3E8347955B824D69C5E833D796A405AC397E0637A9FD226C648821908F6794` | `6A3E8347955B824D69C5E833D796A405AC397E0637A9FD226C648821908F6794` | `0` |
| `WINDOW_HEIGHT` | `7AC2C84857C7A1A683F73949ADD060E16227EA728AB9E7B17C17C8787F1A39C5` | `7AC2C84857C7A1A683F73949ADD060E16227EA728AB9E7B17C17C8787F1A39C5` | `0` |
| `isWindowRelativePointInside` | `D161BA68DCECE17C6ADD1BA4D74ACCAB71832BB02434E55B1C1049E13CD1CE38` | `D161BA68DCECE17C6ADD1BA4D74ACCAB71832BB02434E55B1C1049E13CD1CE38` | `0` |
| `isWindowRelativePointInsideAllowedRegion` | `8F5C15775B95E008DF945859188A2182D37C9D1C79B16A5C61E32DCD5D9A4E5A` | `8F5C15775B95E008DF945859188A2182D37C9D1C79B16A5C61E32DCD5D9A4E5A` | `0` |
| `insideScanRegion` | `B76B7FEB34D7C42F92886F1113CE47C0ACE0FD2473F2D9CAB5727A86C6BF418C` | `B76B7FEB34D7C42F92886F1113CE47C0ACE0FD2473F2D9CAB5727A86C6BF418C` | `0` |
| `defaultNpcClickScanRegions` | `9E7796AB198632CDA1D82315BC328C30146208AB911E8799016EF566DC595E91` | `9E7796AB198632CDA1D82315BC328C30146208AB911E8799016EF566DC595E91` | `0` |
| `primaryScanRegionRoi` | `BD495D6209B999B799F78E7CEE913662AE28FC50CFEA9F4589A15466497B2410` | `BD495D6209B999B799F78E7CEE913662AE28FC50CFEA9F4589A15466497B2410` | `0` |
| `toCloudScanRegions` | `6F271C8390B1EB9E0D7373B4E6FD1B9EC63EF6569D35205A6D471A9622611AD0` | `6F271C8390B1EB9E0D7373B4E6FD1B9EC63EF6569D35205A6D471A9622611AD0` | `0` |

## Existing Blocks Unchanged

- Existing helpers unchanged: `safeDebugName`, `safeValue`, `clamp`, `hasText`, `equalsText`.
- Existing types unchanged: `NpcClickSmartSessionResult`, `NpcClickSmartExecutionResult`,
  `NpcClickVerificationResult`, `WindowBase`.
- Verification result: all nine protected block comparisons are `true`.
- Whole-file reconstruction result: target equals the pre-edit file plus only the authorized imports,
  JavaDoc update, two constants, and six methods; exact comparison is `true`.
- Source remained unchanged during Maven compilation.

## Compile

- Working directory: `D:\mavenProject\dhxy-cloud-brain`.
- Command: `mvn -q compile`.
- Result: exit code `0` in approximately `23.1s`.
- No `clean` and no tests were run.
- No application, host, Task, poller, UI, capture, or input path was started.

## Parent Source Review #1 - APPROVED - 2026-07-14T03:30:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取两个常量及六个完整方法，按 LF 归一化逐字符复核，
八块全部 `exact=True`：`WINDOW_WIDTH/WINDOW_HEIGHT` 均 `45/45`；三个 point/region predicate 分别
`513/513`、`500/500`、`408/408`；三个 region builder 分别 `365/365`、`930/930`、`1355/1355`。
坐标边界、扫描区域顺序、fallback ROI 和 Cloud DTO 字段均无漂移。

父级复算目标 SHA-256 为
`4cdc0fb63067acd28d6de1d54ac8ef88b67c8ef2e750944077a9ffe0e500e888`，与 BF 交付一致；
BF 的 Cloud `mvn -q compile` exit 0。没有 caller、capture/OCR/template 执行、remote/input 或本地 mechanics。
结论 `P0=0 / P1=0 / P2=0`，本 NpcClick geometry cohort `SOURCE APPROVED`。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**
