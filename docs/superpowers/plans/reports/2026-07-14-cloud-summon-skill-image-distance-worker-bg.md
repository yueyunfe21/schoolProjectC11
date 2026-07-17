# W-SUMMONSKILL-IMAGE-DISTANCE-CPU-IMP1

## CLAIMED

- Worker: `Internal Worker BG`
- Role: implementation only; no review or approval.
- Business baseline: DHXY committed `0114604e:src/main/java/com/bot/dhxy/service/SummonSkillService.java`.
- Preflight: Cloud target `src/main/java/com/bot/dhxy/service/SummonSkillService.java` did not exist.
- Both repositories had pre-existing dirty/untracked work; all of it was preserved. No Git mutation was performed.

## Implementation #1

- Created the minimal public same-path Cloud class:
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`.
- The class contains only its package declaration, `java.awt.image.BufferedImage` import, class JavaDoc, and the two
  requested `private static` pure CPU methods.
- Did not copy a constructor, Spring wiring, local window/capture/template-loading/input/UI/queue code, caller,
  wrapper, owner/session/ledger, TTL, or retry behavior.
- No threshold, loop bound, branch, channel calculation, Euclidean-distance calculation, or return behavior changed.

## File SHA

- `SummonSkillService.java`: `SHA-256 7b732678bae12502bf9d6a6d4191d02bd12c3cb691750615e2c4b66538a0e66d`
  (`2171` bytes).

## Source/Target Exact Diff

Comparison used the complete source and target method blocks with line endings normalized to LF.

| Method | Source SHA-256 | Target SHA-256 | Exact diff |
|---|---|---|---:|
| `lowTextureTemplateMatchesByColorDistance` | `601d24c580463c9c546b7ab20bc8a782dc9fb162f7497ff73d55c3ac9d3486f4` | `601d24c580463c9c546b7ab20bc8a782dc9fb162f7497ff73d55c3ac9d3486f4` | `0` |
| `averageColorDistance` | `eaec431ddc5d0f65c5a65ac213f6e74e22cce5c4b02f015a2a45dec72ef20b2d` | `eaec431ddc5d0f65c5a65ac213f6e74e22cce5c4b02f015a2a45dec72ef20b2d` | `0` |

Both comparisons returned `Exact=True`; the two methods are mechanically identical to committed `0114604e`.

## Compile Result

- Repository: `D:\mavenProject\dhxy-cloud-brain`
- Command: `mvn -q compile`
- Result: `exit 0`
- Elapsed: `16.1s`
- Final verification rerun: `exit 0` (`22.1s`).
- No `clean` and no tests were run.
- No application, server, host, Task, poller, UI, capture, or input path was started.

无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Source Review #1

- reviewedAt: `2026-07-14T03:20:00-04:00`
- verdict: **APPROVED**
- severity: `P0=0 / P1=0 / P2=0`
- Parent independently extracted the committed `0114604e` and Cloud method blocks with LF-normalized
  endings. `lowTextureTemplateMatchesByColorDistance` is exact at `819/819` characters and
  `averageColorDistance` is exact at `1029/1029` characters.
- Parent SHA-256 for the new Cloud source is
  `7b732678bae12502bf9d6a6d4191d02bd12c3cb691750615e2c4b66538a0e66d`, matching the delivery.
- The new class contains no constructor, Spring/caller, local capture/template loading, input/UI/queue,
  owner/session/ledger/TTL or retry behavior. Worker Cloud compile exit 0 is accepted for this bounded
  source review; parent fresh clean package remains deferred until concurrent Java writes are stable.

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
