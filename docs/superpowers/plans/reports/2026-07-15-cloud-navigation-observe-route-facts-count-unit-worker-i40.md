# I40 - NavigationService::observeRoutePlanFacts

## CLAIMED

- task: `W-COUNT-NAV-OBSERVE-ROUTE-FACTS-1`
- claimedAt: `2026-07-15T05:11:52-04:00`
- countUnit: `NavigationService::observeRoutePlanFacts`
- requestedCountDelta: `+1`
- uniqueJavaWriteSet: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NavigationService.java`
- report: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-navigation-observe-route-facts-count-unit-worker-i40.md`

## IMPLEMENTATION RESULT

- status: `BLOCKED_MISSING_ACTIVE_CALLER_AND_TYPED_ROUTE_PLAN_BOUNDARY`
- countDelta: `0`
- Java changes: `NO_CODE_CHANGE`
- build/test/runtime: not run, as required while shared Java writers are active
- business difference: `无已批准业务差异；按基线等价迁移`

## Exact evidence

1. The requested active Cloud caller does not exist.
   - Active Cloud `NavigationService.java:272-484` still contains the `696a12b0` `navigateToMap(...)` body directly.
   - Repository-wide Cloud search returns no `navigateToMapCloudPlan`, no `observeRoutePlanFacts`, no `RoutePlanObservation`, and no `NavigationRoutePlanCloudDecisionService`.
   - Therefore there is no active Cloud route-plan request in the same round that can consume the eleven facts or produce the required terminal.

2. The named method is not part of the declared `696a12b0` source baseline.
   - `git show 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/NavigationService.java` contains none of `observeRoutePlanFacts`, `navigateToMapCloudPlan`, or `RoutePlanObservation`.
   - The method appears in the current DHXY working tree at `NavigationService.java:527-567` and is called by the later CR260 route-plan shell at `NavigationService.java:308-465`, specifically `:338`.
   - Mechanically inserting only this later helper into the 696-exact Cloud class would create an unreachable private helper, not the required count unit.

3. The eleven facts depend on runtime/local authorities that have no closed typed Cloud fact port in the allowed write set.
   - Current DHXY `observeRoutePlanFacts` reads `WindowRuntimeContext`, `WindowPathingSnapshot`, `WindowPathingIntent`, dialog preparation/status/action state, current player map, fresh caller map, and route-dialog yield/pending predicates at `NavigationService.java:527-567`.
   - The same-round consumer builds `NavigationRoutePlanCloudDecisionService.RoutePlanStepRequest` at `NavigationService.java:354-376` and later applies a terminal fact gate.
   - Active Cloud has neither that decision contract/service nor a typed DHXY route-fact observation port. Adding either requires Java files outside I40's unique write set.

4. The migration matrix confirms both halves are separate required units.
   - `service-migration-matrix.md:1114` lists `NavigationService::navigateToMapCloudPlan` as the route-plan shell.
   - `service-migration-matrix.md:1392` lists `NavigationService::observeRoutePlanFacts` as the eleven-fact reader.
   - The shell prerequisite is absent, so `observeRoutePlanFacts` cannot close `active caller -> facts -> plan request -> terminal` by itself.

## Impact

- Copying the helper alone would be dead code and cannot justify `countDelta=+1`.
- Replacing the active 696 `navigateToMap` body with the later CR260 shell inside this task would require missing decision/transport types and would exceed the unique Java write set.
- Synthesizing local placeholder facts or a fake terminal would break the exact snapshot/current-map/intent/geometry order and violate the no-stub rule.

## Exact unblock condition

Reissue the same count unit only after one of these equivalent prerequisites is present in active Cloud:

1. `NavigationService::navigateToMapCloudPlan` plus its real `NavigationRoutePlanCloudDecisionService` request/result contract and terminal fact gate are already active and reachable; or
2. an approved typed DHXY route-plan fact port supplies the exact eleven observations and the active Cloud route-plan caller consumes them in the same round.

The reissued scope must then preserve the current DHXY order and predicates exactly: recent snapshot check; active-intent compatibility; current-map match; route-transfer preparation; stale preparation; prepared action usability; yield; same-target pending; caller fresh-map availability/match; then one `RoutePlanObservation` consumed by the same-round plan request. No new TTL, retry, wrapper, runtime owner, or fallback may be introduced.

## Parent Source Review #1 - 2026-07-15T05:18:00-04:00

父级独立核对 active Cloud、DHXY 当前实现、`696a12b0` 与迁移矩阵：`observeRoutePlanFacts` 及其
`navigateToMapCloudPlan` caller 均不属于 `696a12b0`，active Cloud 也没有 route-plan decision contract 或
typed route-fact producer。单文件补 helper 只能形成不可达死代码，不能闭合计数链。

结论：**P0=0/P1=2/P2=0，BLOCKED_POST_BASELINE_MISSING_CALLER_AND_TYPED_FACTS，countDelta=0**。
无 Java 改动；不得复制本地 runtime、伪造 observation/terminal 或以 helper 自洽计数。精确返修条件沿用上文
“Exact unblock condition”。本内部实现槽立即释放并续派互斥 `+1` 整链。
