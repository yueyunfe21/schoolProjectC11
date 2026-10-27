<#
Prepare (not activate) production publication of an exact Gamma artifact. Source validates GitHub
run provenance; Package validates the downloaded bytes without executing or extracting candidate code.
No release, token upload, version policy mutation or signing is performed by this foundation phase.
#>
param(
    [Parameter(Mandatory)][ValidateSet('Source','Package')][string]$Phase,
    [string]$CandidateDirectory,
    [string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$runId = $env:CANDIDATE_RUN_ID
$attempt = $env:CANDIDATE_RUN_ATTEMPT
if ($runId -notmatch '^[1-9][0-9]{0,17}$' -or $attempt -notmatch '^[1-9][0-9]{0,3}$') { throw 'Invalid candidate run/attempt.' }
if ($env:GAMMA_ACCEPTED -ne 'true') { throw 'Explicit acceptance of this exact Gamma candidate is required.' }
if ($Phase -eq 'Source') {
    $repo = $env:GITHUB_REPOSITORY
    if ($repo -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') { throw 'Invalid source repository.' }
    $headers = @{ Authorization="Bearer $env:GH_TOKEN"; Accept='application/vnd.github+json'; 'X-GitHub-Api-Version'='2022-11-28' }
    # Fixed GitHub origin; credentials must not be sent to a user-supplied host/redirect target.
    $base = "https://api.github.com/repos/$repo/actions/runs/$runId"
    $run = Invoke-RestMethod -Uri "$base/attempts/$attempt" -Headers $headers -TimeoutSec 30 -MaximumRedirection 0
    if ($run.status -ne 'completed' -or $run.conclusion -ne 'success' -or
        $run.head_branch -ne 'master' -or $run.path -ne '.github/workflows/client-gamma.yml' -or
        $run.head_repository.full_name -ne $repo -or $run.event -notin @('push','workflow_dispatch')) {
        throw 'Only a successful same-repository master Gamma build can be promoted.'
    }
    $artifacts = Invoke-RestMethod -Uri "$base/artifacts?per_page=100" -Headers $headers -TimeoutSec 30 -MaximumRedirection 0
    $candidateArtifacts = @($artifacts.artifacts | Where-Object { $_.name -eq "client-candidate-$attempt" -and -not $_.expired })
    if ($candidateArtifacts.Count -ne 1) { throw 'Exact candidate artifact is missing, ambiguous or expired; rebuild and re-accept.' }
    "artifact_id=$($candidateArtifacts[0].id)" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Append -Encoding utf8
    "source_sha=$($run.head_sha)" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Append -Encoding utf8
    return
}
$manifestPath = Join-Path $CandidateDirectory 'candidate.json'
if ((Get-Item -LiteralPath $manifestPath).Length -gt 65536) { throw 'Candidate metadata exceeds size limit.' }
$candidate = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
if ($candidate.schemaVersion -ne 1 -or $candidate.product -ne 'dhxy-client' -or
    $candidate.platform -ne 'windows-x64' -or $candidate.format -ne 'portable-app-image' -or
    $candidate.dirty -ne $false -or $candidate.sourceRepository -ne $env:GITHUB_REPOSITORY -or
    $candidate.sourceCommit -cne $env:CANDIDATE_SOURCE_SHA -or
    [string]$candidate.sourceRunId -cne $runId -or [string]$candidate.sourceRunAttempt -cne $attempt -or
    [string]$candidate.buildId -cne "$runId.$attempt") { throw 'Candidate provenance mismatch.' }
if ($candidate.version -notmatch '^\d+\.\d+\.\d+$' -or $candidate.sha256 -notmatch '^[a-f0-9]{64}$') {
    throw 'Invalid candidate version or SHA256.'
}
$expectedName = "client-$($candidate.version)-b$($candidate.buildId)-windows-x64.zip"
if ($candidate.packageFile -cne $expectedName) { throw 'Unexpected package path/name.' }
$packagePath = Join-Path $CandidateDirectory $expectedName
if ((Get-Item -LiteralPath $packagePath).Length -ne $candidate.sizeBytes -or
    (Get-FileHash -LiteralPath $packagePath -Algorithm SHA256).Hash.ToLowerInvariant() -cne $candidate.sha256) {
    throw 'Package bytes differ from the Gamma manifest. Nothing will be published.'
}
if ($env:UPDATE_MODE -notin @('optional','required') -or [string]::IsNullOrWhiteSpace($env:RELEASE_NOTES) -or $env:RELEASE_NOTES.Length -gt 10000) {
    throw 'Valid update mode and 1..10000 character release notes are required.'
}
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Promotion output must be new; no overwrite.' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
Copy-Item -LiteralPath $packagePath,$manifestPath -Destination $OutputDirectory
$request = [ordered]@{
    schemaVersion=1; status='PREPARED_ONLY'; channel='prod'; product='dhxy-client'
    candidate=$candidate; requestedUpdateMode=$env:UPDATE_MODE; releaseNotes=$env:RELEASE_NOTES
    acceptedBy=$env:GITHUB_ACTOR; preparedAt=[DateTime]::UtcNow.ToString('o'); promotionRunId=$env:GITHUB_RUN_ID
    productionActivated=$false; missingCapabilities=@('distribution','artifact-signing','client-updater','server-version-admission')
}
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'promotion-request.json'), ($request | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
Write-Host "PREPARED_ONLY $($candidate.candidate); original package SHA256=$($candidate.sha256)"
