# CR271 TURN-22C1 Independent Delivery Review R2

## Verdict

**APPROVED**

- `P0 = 0`
- `P1 = 0`
- `P2 = 0`
- Review snapshot: `2026-07-16T09:23:20.682-04:00`
- Role: TURN-22C1 independent delivery reviewer R2, not implementer and not parent final reviewer.

No blocking or non-blocking defect was found in the frozen TURN-22C1 one-file cleanup scope.

## Independent Basis

Read before judgment:

1. `AGENTS.md` in full, `docs/DHXY_CONTEXT.md` in full, and the current CR271 material at the top of `docs/ACTIVE_WORK.md`.
2. `docs/业务逻辑.md` applicable team-return rules: local-team boundary, verified-return snapshot preservation, hot-start team-return priority, stop/pause handling, and the `WAIT_TEAM_RETURN` baseline row.
3. TURN-22C1 child card through its physical true EOF and TURN-22 parent card through its physical true EOF.
4. Current Cloud `TeamReturnTurnContractTest.java` in full, all 1612 lines, plus the production `TeamReturnService` and relevant `CloudTeamReturnPortAssembly` path used by that test.
5. Authoritative `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` `TeamReturnService.java` in full and its AutoBattle/Wubei/Xiuluo caller sites.

I did not read or reuse another independent reviewer's report. The required full child-card read necessarily exposed the parent review text at its EOF; that conclusion was ignored and the evidence below was re-derived from current bytes, the authoritative git object, resources, POM, and raw edit records.

## Snapshot And Write Set

| Artifact | Current evidence |
|---|---|
| Cloud test | 1612 lines, SHA-256 `D270D7DCACB73BC66B50AF7BE9D2DBC3F53098587F430FB6EBDCDE7F66E07FAB` |
| TURN-22C1 child card | 111 lines, SHA-256 `9CECEF6CCFD3280EFBC738E57BC0A1AE3A758FE91E0FE771EE29093C88827754` |
| DHXY baseline | HEAD `0114604e1ff5f15491d2910959c45252e893d04f`; business authority remains `696a12b0...` |
| Cloud baseline | HEAD `3b988caa010254973e03342272e6d1d6a9685b01`; target test is ignored by `.gitignore:15` (`src/test/`) |

The raw implementation transcript at
`C:/Users/Yunfeng Yue/.claude/projects/D--mavenProject/aa951b1e-8f04-4f92-b6e0-de08af49c39a.jsonl`
contains exactly three `Edit` calls against the target test:

1. Transcript line 24070 replaces only the old method prefix containing Java reads, forbidden substring checks, assembly occurrence counts, and service-order `indexOf` checks. The splice resumes at the existing `byte[] cloudMember = Files.readAllBytes(...)`, so the resource and enum body is retained. The replacement is only the explanatory JavaDoc plus the required method rename.
2. Transcript line 24073 removes only the Markdown plan `Files.readString(...)` and four `plan.contains(...)` assertions.
3. Transcript line 24088 removes only the now-unreferenced `occurrences(String,String)` helper.

The only other writes in the assignment interval are the append-only CLAIM/delivery additions to the child card at transcript lines 24053 and 24101. A two-repository mtime scan for `09:04:00..09:11:00 -04:00` found the target as the only Java file written in that interval. The two other files in the interval were unrelated TURN-28S1 reviewer reports. No Cloud production, protocol, POM, DHXY Java/test, sibling class, or second TeamReturn test was written by this slice.

## Cleanup Proof

Executable occurrences in the current test are all zero for:

- `Files.readString`, `TeamReturnService.java`, `CloudTeamReturnPortAssembly.java`, and the authoritative plan path/name;
- `.contains(`, `indexOf(`, `substring(`, and `occurrences(`;
- `serviceSource`, `assemblySource`, and the old `sourceGateTemplateParityAndPermanentLocalServiceAllowlistStayClosed` name.

The JavaDoc word "substrings" is explanatory text, not a replacement source guard. Current imports remain genuinely used: `Files`/`Path` serve the four PNG reads, `IOException` serves the resource test and PNG fixture, and `List`/`Set` remain broadly used. The pre-existing constructor/method/field reflection fixtures and matcher-failure stack fixture are still present; none is a source scan.

## Resource And Enum Proof

The retained method at current test lines 649-674 performs four real `Files.readAllBytes(...)` calls, two `assertArrayEquals(...)` checks, both fixed SHA assertions, and the exact enum-set assertion.

Member `gui.png`, Cloud copy, actual template rather than a live/historical incident image:

![Cloud member return template](D:/mavenProject/dhxy-cloud-brain/src/main/resources/images/template/status/gui.png)

Member `gui.png`, DHXY parity counterpart, actual template:

![DHXY member return template](D:/mavenProject/DHXY/images/template/status/gui.png)

Both member files are 389 bytes, `14x14`, SHA-256
`5B4C2C43F84A9FF9CEF26F8BE22BE40872C192698244A9840D01C3DEA25E4E21`.

Leader `zhao.png`, Cloud copy, actual template rather than a live/historical incident image:

![Cloud leader return template](D:/mavenProject/dhxy-cloud-brain/src/main/resources/images/template/status/zhao.png)

Leader `zhao.png`, DHXY parity counterpart, actual template:

![DHXY leader return template](D:/mavenProject/DHXY/images/template/status/zhao.png)

Both leader files are 410 bytes, `13x15`, SHA-256
`2468C531D25C980061473BE7BAF5918D910499E51D096C5417C4652E880ECBD3`.

The test's expected `TurnLocalOperation` set is exactly these nine values:
`BAG_RETURN_ITEM`, `BAG_USE_INCENSE`, `UI_CLEAN_ALL`, `UI_CLOSE_GENERIC_WINDOWS`,
`UI_CLEAN_LIGHTWEIGHT`, `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2`, `GIVE_ITEM_FROM_OPEN_DIALOG`,
`QUEST_ACTIVATE`, and `QUEST_CAPTURE_DETAIL`. It is compared against
`Set.of(TurnLocalOperation.values())`. Cloud and DHXY enum source bytes also match at SHA-256
`A70DBFA3B60F681776D70D9DEAC518BD4AB3B0B69F5B12E379DD19197583FBD8`.

## Preserved Behavioral Tests

All 14 `@Test` methods remain. The former source-gate method was renamed, not deleted as a test.

- Current lines 390-414 invoke production `TeamReturnService` and production assembly and retain the baseline order `capture -> incense -> capture -> click`, one incense call, random `+-3` bounds, and unique UUID per command. This matches `696a12b0:TeamReturnService.java:65-91`, including one queue-owned `CLICK_LEFT(150) + SLEEP(500)` sequence at baseline lines 86-89.
- Current lines 441-478 retain completed, failed, stopped, outcome-uncertain, and transport-uncertain click cases. Every case asserts exactly one command and one UUID, so no transport retry can hide in a terminal branch.
- Current lines 482-499 retain fail-closed rejection of wrong input step, returned input frame, wrong outcome metadata, and malformed stopped shape.
- Current `assertAtomicClick` at lines 1266-1287 requires exactly one step: index 0, `INPUT`, `CLICK_LEFT`, `clickDelayMs=150`, `queueHoldMs=500`, with `waitMs`, capture, match, and local-service payloads all null. Thus there is no trailing WAIT step or frame-bearing action.
- Current lines 353-385 retain capture FAILED/STOPPED/outcome uncertainty/transport uncertainty/busy projection and exact command/UUID counts; lines 504-636 retain leader initial miss, disappearance, timeout/poll cadence, one-capture precheck, zero-command consume, and inconclusive live fallback.

The cleaned method therefore no longer pretends source text is behavior, while the real assembly/service paths still prove the 150/500, one-command/UUID, no-frame, terminal/uncertain fail-closed, and zero-retry contracts.

## Forbidden Alternatives

- No replacement source-string or SHA-only production guard exists. The only SHA assertions are the explicitly retained real PNG resource hashes.
- Cloud `pom.xml` has no DHXY artifact, sibling source/test root, `systemPath`, build-helper source injection, or additional classpath element. The test has no classloader/compiler/`target/classes` seam. Its only `../DHXY` access is the explicitly required PNG byte-parity read, not class loading.
- The edit transcript and interval scan show no copied DHXY class or sibling Java file. Current test-private nested types are the pre-existing scripted fixtures and were not edited by TURN-22C1.
- Case-insensitive search finds no `session`, `owner`, `ledger`, `TTL`, durable state, or wrapper addition. `retry` appears only in test names/JavaDoc describing zero retry and in `TaskRetryPolicy.none()` for the existing context fixture.

## Review Boundaries

No Java, original card, parent card, plan, `ACTIVE_WORK`, CR271 matrix/dashboard, POM, runtime file, or Git state was modified. Only this R2 report was created. Per the explicit assignment, no Maven, JUnit, compile, package, runtime, application/server, Task, UI, capture, or physical input was run, and no Git mutation was performed.

## Final Judgment

**APPROVED / P0/P1/P2 = 0/0/0.** TURN-22C1 is the authorized one-file behavioral cleanup: it removes only Java/Markdown source scans, substring/count/order/plan guards, and the dead occurrence helper; it preserves real member/leader PNG byte+SHA parity, the exact local-operation enum set, and every required assembly/JSON terminal and atomic-click test. No substitute source guard, sibling classpath, copied class, retry, session, ledger, TTL, or durable workflow was introduced.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: CR271 TURN-22C1 INDEPENDENT DELIVERY REVIEW R2 APPROVED P0/P1/P2=0/0/0 TEST_SHA256=d270d7dcacb73bc66b50af7be9d2dbc3f53098587f430fb6ebdcde7f66e07fab 2026-07-16T09:23:20.682-04:00 -->
