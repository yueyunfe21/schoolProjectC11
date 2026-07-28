param(
    [int]$Port = 18080,
    [string]$Path = "/api/cloud/decision",
    [string]$Token = "local-dev-token",
    [string]$TenantId = "",
    [string]$UserId = "",
    [string]$StateRoot = "",
    [string]$ForcedDecision = ""
)

$ErrorActionPreference = "Stop"

if ($ForcedDecision -ne "") {
    throw "ForcedDecision belongs to the DHXY test-sidecar only. Use scripts/run-dhxy-test-cloud-decision-stub.ps1 -AllowDhxyTestSidecar for explicit debug."
}

$launcher = Join-Path $PSScriptRoot "run-cloud-brain-server.ps1"
& $launcher -Port $Port -Path $Path -Token $Token `
    -TenantId $TenantId -UserId $UserId -StateRoot $StateRoot
