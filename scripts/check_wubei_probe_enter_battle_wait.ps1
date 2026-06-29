$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "..\src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java"
$source = Get-Content $sourcePath -Raw

if ($source -notmatch 'waitForProbeEnterBattlePreparedDialog') {
    Write-Error "missing probe-specific event-driven enter-battle wait"
}

if ($source -notmatch 'PROBE_ENTER_BATTLE_EVENT_RECHECK_MS') {
    Write-Error "missing bounded probe enter-battle event recheck interval"
}

if ($source -notmatch 'isProbeEnterBattleSource\(source\)\s*\?\s*waitForProbeEnterBattlePreparedDialog\(') {
    Write-Error "tryClickKnownEnterBattleDialog does not route probe sources to the event-driven wait"
}

$helperMatch = [regex]::Match($source, 'private DialogResult waitForProbeEnterBattlePreparedDialog[\s\S]*?private void throwProbeEnterBattleTimeoutIfNeeded')
if (!$helperMatch.Success) {
    Write-Error "cannot isolate waitForProbeEnterBattlePreparedDialog helper"
}

$helper = $helperMatch.Value
if ($helper -match 'waitForPreparedWubeiDialogReply') {
    Write-Error "probe event-driven wait delegates to the old prepared-dialog reply loop"
}
if ($helper -match 'WUBEI_PREPARED_DIALOG_POLL_MS') {
    Write-Error "probe event-driven wait still uses the old 80ms prepared-dialog poll interval"
}
if ($helper -match 'tryConsumePreparedWubeiDialog\([\s\S]*?true\)') {
    Write-Error "probe event-driven wait still refreshes interest while consuming"
}

Write-Output "OK: Wubei probe targetReady enter-battle wait is event-driven, not absent-consume hot loop"
