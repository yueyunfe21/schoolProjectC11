param(
    [string]$ClientRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$CloudRoot = "D:\mavenProject\dhxy-cloud-brain",
    [string]$ArtifactDirectory
)

$ErrorActionPreference = "Stop"

function Invoke-MavenTest {
    param(
        [string]$Repository,
        [string]$TestSelector,
        [string]$Label
    )

    Write-Host "[$Label] $TestSelector"
    Push-Location $Repository
    try {
        & mvn -q -Dtests-enabled=true "-Dtest=$TestSelector" test
        if ($LASTEXITCODE -ne 0) {
            throw "$Label failed with Maven exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

$client = (Resolve-Path -LiteralPath $ClientRoot).Path
$cloud = (Resolve-Path -LiteralPath $CloudRoot).Path
$clientTarget = Join-Path $client "target"

if ([string]::IsNullOrWhiteSpace($ArtifactDirectory)) {
    $ArtifactDirectory = Join-Path $clientTarget "connectivity"
}
New-Item -ItemType Directory -Force -Path $ArtifactDirectory | Out-Null
$artifacts = (Resolve-Path -LiteralPath $ArtifactDirectory).Path

if (-not $artifacts.StartsWith($clientTarget, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Connectivity artifacts must stay under the Client target directory: $clientTarget"
}

$requiredArtifacts = @(
    "xinshou-in-combat-observation.json",
    "xinshou-combat-exited-observation.json",
    "xinshou-combat-command.json"
)
foreach ($name in $requiredArtifacts) {
    $path = Join-Path $artifacts $name
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path
    }
}

$env:DHXY_CONNECTIVITY_DIR = $artifacts

Invoke-MavenTest `
    -Repository $client `
    -TestSelector "XinshouConnectivityWireProducerTest" `
    -Label "1/3 Client Runner -> observation wire"

foreach ($name in $requiredArtifacts[0..1]) {
    $path = Join-Path $artifacts $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Client producer did not create $path"
    }
}

Invoke-MavenTest `
    -Repository $cloud `
    -TestSelector "CloudXinshouPreparedActionObserverContractTest#clientWireCrossesHttpInboxObserverTaskAndProducesExactClientCommand" `
    -Label "2/3 Cloud HTTP -> Inbox -> Observer -> Task -> command wire"

$commandArtifact = Join-Path $artifacts $requiredArtifacts[2]
if (-not (Test-Path -LiteralPath $commandArtifact -PathType Leaf)) {
    throw "Cloud phase did not create $commandArtifact"
}

Invoke-MavenTest `
    -Repository $client `
    -TestSelector "XinshouCombatMechanicalDispatcherContractTest#cloudWireCommandCrossesProtocolValidatorAndProductionDispatcher" `
    -Label "3/3 Client command wire -> validator -> dispatcher -> fake input"

Write-Host ""
Write-Host "Xinshou Client-Cloud connectivity PASSED"
foreach ($name in $requiredArtifacts) {
    $path = Join-Path $artifacts $name
    $item = Get-Item -LiteralPath $path
    Write-Host ("  {0} ({1} bytes)" -f $item.FullName, $item.Length)
}
