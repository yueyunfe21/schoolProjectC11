C `LeftTopStatusSwitchService::resolveTaskCode`：具体风险证据：与 matrix 约 1321 行附近既有同链路计数边界重复，不应另计。
I9 `CommonBoxService::detectBox`：具体风险证据：与 matrix 约 1328 行附近既有 member-box 检测链路计数边界重复，不应另计。
A `BattleRadarService::discardStaleCombatExitSignalIfInCombat`：CLEAR-TO-PARENT-REVIEW；matrix 约 1332 行附近为独立 stale-discard 状态消费项。
D `AutoCombatService::runPendingFollowerFirstAidIfAllowed`：CLEAR-TO-PARENT-REVIEW；matrix 约 1332 行附近为独立 follower-first-aid 项。
I10 `TaskMaintenanceService::maybeCleanSummonSkill`：具体风险证据：与 matrix 约 1379 行附近 `runOpportunisticMaintenance` 属同一 matrix bullet，不应另计。
