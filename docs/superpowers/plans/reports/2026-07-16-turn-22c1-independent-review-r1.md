# CR271 TURN-22C1 independent delivery review R1

- Reviewer role: independent delivery reviewer; not the implementation Worker and not the parent final reviewer.
- Review time: `2026-07-16T09:23:31.564-04:00`.
- Review mode: source/test-source and filesystem evidence only. No Maven, JUnit, compile, package, runtime,
  application/server, Task, UI, capture, input, or Git mutation was performed.

## Findings first

| Severity | Count | Independent conclusion |
| --- | ---: | --- |
| P0 | 0 | No delivery blocker found. |
| P1 | 0 | No contract, scope, or retained-test regression found. |
| P2 | 0 | No actionable test-source or evidence gap found in this slice. |

**Verdict: `APPROVED` for TURN-22C1 independent delivery review, with `P0/P1/P2=0/0/0`.** This is not
TURN-22 parent approval and does not replace the separate build/test gates.

## Authority and bytes independently read

- Read all of `AGENTS.md` and all 1,349 physical lines of `docs/DHXY_CONTEXT.md`.
- Read the current top CR271 entry in `docs/ACTIVE_WORK.md` (`:3-37`) and the applicable
  `docs/业务逻辑.md:215-224` baseline gate plus the `696a12b0` Xiuluo baseline declaration at `:1253-1266`.
- Read all 668 physical lines of the TURN-22 parent card through its latest true EOF. Current card SHA-256 is
  `E7787294811D8D668D2EF3B0240EC848C022505552ACAF17BB1B7985E3D1DCF3`.
- Read all 111 physical lines of the TURN-22C1 child card through its latest true EOF. Current child-card SHA-256
  is `9CECEF6CCFD3280EFBC738E57BC0A1AE3A758FE91E0FE771EE29093C88827754`.
- Read all 1,612 physical lines of current Cloud `TeamReturnTurnContractTest.java`. Recomputed SHA-256 is
  `D270D7DCACB73BC66B50AF7BE9D2DBC3F53098587F430FB6EBDCDE7F66E07FAB`, matching the delivered byte identity.
- Independently read the complete baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
  `TeamReturnService.java` and the TeamReturn caller blocks in baseline `AutoBattleTask`, `WubeiTask`, and
  `XiuluoTaskV2`. The relevant baseline remains member observe -> incense -> member refresh -> click, with one
  queue submission containing `clickLeft(..., 150)` then `sleep(500)`.

No parent or other reviewer verdict was reused as this report's conclusion.

## One-file cleanup and removed source guards

Current `TeamReturnTurnContractTest.java` contains no executable occurrence of any of the removed mechanisms:

| Removed mechanism | Current evidence |
| --- | --- |
| Java source reads | No `Files.readString`, `readString(`, `TeamReturnService.java`, `CloudTeamReturnPortAssembly.java`, `serviceSource`, or `assemblySource`. |
| Markdown plan read | No authority-plan path, `card-plan.md`, `https-turn-complete`, `plan.contains`, or equivalent plan text lookup. |
| Substring/source assertions | No executable `.contains(`, `.indexOf(`, `substring(`, production-string loop, or `sourceGateTemplateParity...` method. |
| Count helper | No `occurrences(` call or definition remains. |

The prose at `:639-646` explains why source text counting was removed; it does not read a source file or enforce a
replacement string guard. The retained test is renamed
`memberAndLeaderTemplateBytesMatchDhxyAndLocalOperationAllowlistStaysClosed` (`:649`) and now owns only resource
bytes/SHA and the closed enum set.

The frozen child card records the pre-cleanup snapshot as 1,658 lines / SHA-256
`2D2907592E96D3C44E4AE239A8F569ADBA785568B19309D3F35CE90CB49E9496`; the current file is 1,612 lines, a net
46-line removal. The resulting structure and zero-count checks agree with removal of the targeted source/plan
guards and the now-unused helper, with no unrelated test method removed.

## Retained real PNG parity

`TeamReturnTurnContractTest.java:650-673` still performs four real `Files.readAllBytes` calls, two
`assertArrayEquals` checks, and both fixed SHA assertions.

These are current production templates, not incident screenshots or historical examples:

**Cloud member `归` template, current production template**

![Cloud member return template](D:/mavenProject/dhxy-cloud-brain/src/main/resources/images/template/status/gui.png)

**DHXY member `归` template, current production template and direct parity peer**

![DHXY member return template](D:/mavenProject/DHXY/images/template/status/gui.png)

**Cloud leader `招` signal template, current production template**

![Cloud leader return signal template](D:/mavenProject/dhxy-cloud-brain/src/main/resources/images/template/status/zhao.png)

**DHXY leader `招` signal template, current production template and direct parity peer**

![DHXY leader return signal template](D:/mavenProject/DHXY/images/template/status/zhao.png)

| Pair | Dimensions | Bytes | Independent byte comparison | SHA-256 on both sides |
| --- | --- | ---: | --- | --- |
| member `gui.png` | `14x14` / `14x14` | `389` / `389` | byte-identical | `5B4C2C43F84A9FF9CEF26F8BE22BE40872C192698244A9840D01C3DEA25E4E21` |
| leader `zhao.png` | `13x15` / `13x15` | `410` / `410` | byte-identical | `2468C531D25C980061473BE7BAF5918D910499E51D096C5417C4652E880ECBD3` |

## Retained exact local-operation set

At `TeamReturnTurnContractTest.java:663-673`, the expected set remains exactly:

`BAG_RETURN_ITEM`, `BAG_USE_INCENSE`, `UI_CLEAN_ALL`, `UI_CLOSE_GENERIC_WINDOWS`,
`UI_CLEAN_LIGHTWEIGHT`, `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2`, `GIVE_ITEM_FROM_OPEN_DIALOG`,
`QUEST_ACTIVATE`, `QUEST_CAPTURE_DETAIL`.

The assertion still compares that set with `Set.of(TurnLocalOperation.values())`. Current Cloud and DHXY
`TurnLocalOperation.java` are byte-identical with SHA-256
`A70DBFA3B60F681776D70D9DEAC518BD4AB3B0B69F5B12E379DD19197583FBD8`, and both contain exactly those nine values.

## Retained assembly and negative contracts

The class still has all 14 `@Test` methods. The cleanup did not remove or weaken the following production-path
assertions:

| Required retained behavior | Current direct evidence |
| --- | --- |
| Real production assembly/service | `harness(...)` at `:676-701` constructs production `CloudTeamReturnPortAssembly`, production `TeamReturnService`, and a bound production `TurnGameClient`; the scripted port records the resulting `TurnAction`. |
| One typed click | `clickUsesOneTypedClickActionAndMapsAllClosedTerminals` at `:441-478` invokes production click assembly for each closed terminal case. |
| Exact `150/500` | `assertAtomicClick` at `:1266-1288` requires one step, `INPUT/CLICK_LEFT`, `clickDelayMs=150`, and `queueHoldMs=500`. |
| No WAIT or frame | The same helper requires exactly one step, `waitMs=null`, `capture=null`, `match=null`, `localService=null`, and `fullWindowFailureEvidence=false`; `inputWithFrame` is rejected at `:481-500`. |
| One command and one UUID | Every click case requires `executeCalls==1` and `uuids.calls==1` at `:475-477`; `UUID.fromString` and unique-per-command checks remain at `:1267` and `:1295-1303`. |
| Terminal/uncertain fail-closed | Completed, failed, stopped, duplicate/outcome-uncertain, and transport-uncertain cases remain at `:441-478`; wrong input step/frame/metadata/stopped shape must throw at `:481-500`. |
| Zero retry | Each isolated click case has exactly one execute and one UUID; capture malformed/correlation and uncertain cases likewise assert fixed execute counts rather than allowing a resend (`:188-213`, `:353-387`). |
| Baseline member order | `:390-415` still proves capture -> incense -> capture -> click; `:418-438` keeps first-miss and refresh-miss no-click boundaries. |
| Leader wait/precheck | `:504-637` retains initial miss, disappearance, timeout/poll constants, capture-before-return, consume-with-zero-new-command, and inconclusive live fallback. |

Current production SHA-256 values are unchanged from the pre-C1 frozen TURN-22 bytes:

- `CloudTeamReturnPortAssembly.java`:
  `4435B30C4BFC923E222B12DE3CDA5BE9AEEC766AA1F826F26EA534BC1A5CFD66`.
- Cloud `TeamReturnService.java`:
  `CD1CD365BFF90B16817C15831A2685F2FEAE84E2D49893B9B975362D4EC4EDAF`.

Thus this slice did not substitute test cleanup with a production semantic change. The retained tests still align
with `696a12b0:TeamReturnService.java:65-91` and its one-queue `150+500` member click contract.

## Write set and prohibited alternatives

- Filesystem audit of the claim-to-delivery interval `09:04:43..09:10:29 -04:00` found exactly one Cloud file
  with a write timestamp in that interval: `TeamReturnTurnContractTest.java` at `09:09:25`. No Cloud production,
  protocol, POM, or sibling test file was written in that interval. No DHXY Java file was written in that interval.
- The authorized child card was append-only during delivery and later received the parent review append; its current
  later mtime does not turn it into a Java/source write-set expansion.
- Current Cloud `pom.xml` contains no sibling DHXY dependency, `systemPath`, build-helper/add-test-source,
  additional classpath, or sibling `src/target` path. The only `../DHXY` references in the named test are the two
  explicitly required PNG parity reads at `:655` and `:657`.
- Repository-wide duplicate search finds one `TeamReturnTurnContractTest` class only. The named test contains no
  copied `TurnInputStepExecutor`, mapper, queue, worker, binding, runtime-context, or keyboard-service class/fixture.
- No TeamReturn-specific replacement source guard exists elsewhere in Cloud tests. Other older, unrelated test
  classes still contain pre-existing source scans; none was created or modified by this slice and none targets the
  removed TURN-22C1 checks.
- No wrapper, transport retry, session, owner, ledger, TTL, or durable workflow was added. The only retry-related
  construction in the current named test is the pre-existing explicit `TaskRetryPolicy.none()` context fixture at
  `:730`, which enforces no retry rather than introducing one.
- Existing reflection fixtures at `:742-751`, `:1305-1347`, and related helper types remain present and were not
  replaced or broadly cleaned.

## Baseline judgment and gate boundary

No approved business difference is introduced. This delivery is a behavioral-test cleanup only and remains
equivalent to the `696a12b0` TeamReturn order, exact template behavior, one typed click, one command/UUID,
same-queue `150+500` mechanics, and fail-closed uncertainty policy.

Per the explicit review instruction, no Maven/JUnit/compile/runtime evidence was generated or claimed here. This
`APPROVED` verdict closes this independent source/test-source delivery review only; parent aggregation and any
separate test/build/final TURN-22 gates remain outside this report.

<!-- TRUE_EOF: CR271 TURN-22C1 INDEPENDENT DELIVERY REVIEW R1 APPROVED P0P1P2=0/0/0 evidence=current-test-sha-D270D7DC png-byte-sha-parity exact-enum-set assembly-json-150-500-one-command-uuid-no-frame-terminal-uncertain-fail-closed-zero-retry write-set-clean no-replacement-source-guard-sibling-classpath-copy-retry-session-ledger-ttl 2026-07-16T09:23:31.564-04:00 -->
