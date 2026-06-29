param(
    [string]$WubeiTaskPath = "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java",
    [string]$RunnerPath = "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java",
    [string]$RuntimePath = "src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java"
)

$ErrorActionPreference = "Stop"

foreach ($path in @($WubeiTaskPath, $RunnerPath, $RuntimePath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "required source file not found: $path"
    }
}

$wubei = Get-Content -LiteralPath $WubeiTaskPath -Raw
$runner = Get-Content -LiteralPath $RunnerPath -Raw
$runtime = Get-Content -LiteralPath $RuntimePath -Raw

if ($wubei -match "NORMAL_ENTER_BATTLE_INTEREST_DELAY_MS" -or
    $wubei -match "normal-enter-battle-delayed" -or
    $wubei -match "CompletableFuture\.delayedExecutor") {
    throw "fixed delayed ordinary enter-battle interest path still exists"
}

foreach ($token in @(
    "startOrdinaryEnterBattleTargetMapGate",
    "getOrdinaryEnterBattleTargetMapName",
    "clearOrdinaryEnterBattleTargetMapGate"
)) {
    if ($runtime -notmatch [regex]::Escape($token)) {
        throw "runtime target-map gate token missing: $token"
    }
}

foreach ($token in @(
    "openWubeiOrdinaryEnterBattleInterestIfTargetMapMatched",
    "wubei:normal-enter-battle-map-matched",
    "MapNameCanonicalizer"
)) {
    if ($runner -notmatch [regex]::Escape($token)) {
        throw "runner target-map gate token missing: $token"
    }
}

foreach ($token in @(
    "startOrdinaryEnterBattleTargetMapGate",
    "getTargetMapName()",
    "ordinary enter-battle target map gate"
)) {
    if ($wubei -notmatch [regex]::Escape($token)) {
        throw "wubei target-map gate token missing: $token"
    }
}

Write-Output "OK: Wubei ordinary enter-battle interest is target-map gated, not fixed-delay gated"
