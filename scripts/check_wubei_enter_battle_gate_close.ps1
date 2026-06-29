param(
    [string]$Path = "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Path)) {
    throw "WubeiTask.java not found: $Path"
}

$content = Get-Content -LiteralPath $Path -Raw
$methodMatch = [regex]::Match(
    $content,
    '(?s)private WubeiStepOutcome consumePreparedEnterBattleBeforeNormalPhase\(.*?\n    \}'
)
if (-not $methodMatch.Success) {
    throw "consumePreparedEnterBattleBeforeNormalPhase method not found"
}

$method = $methodMatch.Value
$closeIndex = $method.IndexOf("taskMaintenanceService.closeTeamMaintenanceWindow")
$initIndex = $method.IndexOf("autoCombatService.initializeForCurrentWindow")
$waitIndex = $method.IndexOf("WubeiPhase.WAIT_BATTLE_FINISH")

if ($closeIndex -lt 0) {
    throw "priority enter-battle path does not close the team maintenance window"
}
if ($initIndex -lt 0 -or $waitIndex -lt 0) {
    throw "priority enter-battle path missing expected combat initialization / WAIT_BATTLE_FINISH transition"
}
if ($closeIndex -gt $initIndex -or $closeIndex -gt $waitIndex) {
    throw "team maintenance window is closed too late in priority enter-battle path"
}

Write-Output "OK: priority enter-battle path closes team maintenance window before WAIT_BATTLE_FINISH"
