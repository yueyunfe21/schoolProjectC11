$ErrorActionPreference = "Stop"

$path = Join-Path $PSScriptRoot "..\src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java"
$source = Get-Content -LiteralPath $path -Raw

function Get-MethodBody {
    param(
        [Parameter(Mandatory=$true)][string]$Text,
        [Parameter(Mandatory=$true)][string]$Signature
    )
    $start = $Text.IndexOf($Signature)
    if ($start -lt 0) {
        throw "method signature not found: $Signature"
    }
    $brace = $Text.IndexOf("{", $start)
    if ($brace -lt 0) {
        throw "method body not found: $Signature"
    }
    $depth = 0
    for ($i = $brace; $i -lt $Text.Length; $i++) {
        $ch = $Text[$i]
        if ($ch -eq "{") {
            $depth++
        } elseif ($ch -eq "}") {
            $depth--
            if ($depth -eq 0) {
                return $Text.Substring($start, $i - $start + 1)
            }
        }
    }
    throw "method body did not close: $Signature"
}

$consume = Get-MethodBody $source "private WubeiStepOutcome consumePreparedEnterBattleBeforeNormalPhase"
$closePos = $consume.IndexOf("closeTeamMaintenanceWindow")
$clickDecisionPos = $consume.IndexOf("boolean clicked")
if ($closePos -lt 0 -or $clickDecisionPos -lt 0 -or $closePos -gt $clickDecisionPos) {
    throw "WUBEI_ENTER_BATTLE prepared dialog must close team maintenance before click/status handling"
}

$postBattle = Get-MethodBody $source "private WubeiStepOutcome runPostBattleRecoverPhase"
if ($postBattle -notmatch "openChainedPostBattleFirstAidWindowAndProbeLeader\(context,\s*state,\s*combatCount\)") {
    throw "chained POST_BATTLE_RECOVER must start the 5s follower HP/MP window before yielding"
}
$chainedBranchPos = $postBattle.IndexOf("if (currentRoundChainedCombatExpected)")
$settlePos = $postBattle.IndexOf("TaskSleep.sleepOrStop(context, 800L")
if ($chainedBranchPos -lt 0) {
    throw "chained POST_BATTLE_RECOVER branch not found"
}
if ($settlePos -ge 0 -and $settlePos -lt $chainedBranchPos) {
    throw "chained POST_BATTLE_RECOVER must not keep the old fixed 800ms settle before the 5s window"
}
if ($source -notmatch "CHAINED_POST_BATTLE_FIRST_AID_BROADCAST_MS\s*=\s*5_000L") {
    throw "chained post-battle follower HP/MP window must be 5 seconds"
}
$precheck = Get-MethodBody $source "private void openChainedPostBattleFirstAidWindowAndProbeLeader"
if ($precheck -notmatch "openTeamFirstAidMaintenanceWindow\(context,\s*TASK_CODE,\s*currentRoundNumber") {
    throw "chained POST_BATTLE_RECOVER helper must open the 5s follower HP/MP window"
}
if ($precheck -notmatch "probeFirstAidSupplyNoFocus") {
    throw "leader HP/MP no-focus precheck must run during the chained 5s follower window"
}

$returnHome = Get-MethodBody $source "private WubeiStepOutcome returnHomeAfterCombatOrContinueSpecialTarget"
if ($returnHome -match "openTeamFirstAidMaintenanceWindow") {
    throw "RETURN_HOME chained continuation must not open the first-aid window after tracker detection"
}
if ($returnHome -notmatch "consumeChainedLeaderCachedFirstAidBeforeClick") {
    throw "leader cached HP/MP plan helper must run before clicking the prepared chained green link"
}
$leaderRecovery = Get-MethodBody $source "private void consumeChainedLeaderCachedFirstAidBeforeClick"
if ($leaderRecovery -notmatch "performCachedFirstAidPlanNow") {
    throw "leader must consume the cached HP/MP plan before clicking the prepared chained green link"
}

Write-Host "OK: Wubei chained combat first-aid window lasts 5s before tracker detection and leader uses cached recovery"
