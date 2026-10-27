# Final release step. Never turns enforcement off or replaces owner allow/revoke lists.
# Cloudflare token is a CI secret with D1 Edit, not a credential shipped to clients.
param([Parameter(Mandatory)][ValidateSet('Check','Activate')][string]$Phase,
      [Parameter(Mandatory)][string]$PromotionDirectory)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ($env:GITHUB_ACTIONS -ne 'true' -or $env:GITHUB_REF -ne 'refs/heads/master' -or
    $env:PUBLIC_PACKAGE_APPROVED -ne 'true') { throw 'Policy activation requires explicit production workflow approval.' }
if ([string]::IsNullOrWhiteSpace($env:CLIENT_POLICY_API_TOKEN) -or
    $env:EXPECTED_POLICY_REVISION -notmatch '^(0|[1-9][0-9]{0,14})$') { throw 'Policy token or expected revision missing.' }
$candidate = Get-Content -LiteralPath (Join-Path $PromotionDirectory 'candidate.json') -Raw | ConvertFrom-Json
$version = "$($candidate.version)-b$($candidate.buildId)"
if ($version -notmatch '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)-b[1-9][0-9]*\.[1-9][0-9]*$') { throw 'Invalid candidate version.' }
$releaseUrl = "https://github.com/yueyunfe21/schoolProjectC11/releases/tag/client-$version"
$api = 'https://api.cloudflare.com/client/v4/accounts/7d67dcb6bec8cdfe2fe1cc399801c55f/d1/database/95fc2c6b-ce9e-4c5b-8d10-8a21581012e4/query'
$headers = @{ Authorization="Bearer $env:CLIENT_POLICY_API_TOKEN" }
function Invoke-PolicyQuery([string]$Sql, [object[]]$Parameters) {
    $body = @{ sql=$Sql; params=$Parameters } | ConvertTo-Json -Depth 8 -Compress
    $response = Invoke-RestMethod -Method Post -Uri $api -Headers $headers -ContentType 'application/json' -Body $body -TimeoutSec 30 -MaximumRedirection 0
    if (-not $response.success -or @($response.result).Count -ne 1 -or -not $response.result[0].success) {
        throw 'D1 policy query failed. Inspect current policy before retrying a mutation.'
    }
    return $response.result[0]
}
$read = Invoke-PolicyQuery 'SELECT * FROM client_version_policies WHERE app_id=?' @('dhxy')
if (@($read.results).Count -ne 1) { throw 'Application policy missing.' }
$policy = $read.results[0]
if ([string]$policy.revision -cne $env:EXPECTED_POLICY_REVISION) { throw 'Policy revision changed; owner must review it before publishing.' }
$allowed = @($policy.allowed_versions | ConvertFrom-Json)
$revoked = @($policy.revoked_versions | ConvertFrom-Json)
if ($version -cin $revoked -or ($allowed.Count -gt 0 -and $version -cnotin $allowed)) {
    throw 'New version is forbidden by the current owner policy. Publication cannot override it.'
}
if ($policy.minimum_version) {
    $pattern = '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:-b([1-9][0-9]*)\.([1-9][0-9]*))?$'
    $left = [regex]::Match($version, $pattern)
    $right = [regex]::Match($policy.minimum_version, $pattern)
    if (-not $right.Success) { throw 'Stored minimum version invalid.' }
    for ($i=1; $i -le 5; $i++) {
        $a = [Numerics.BigInteger]::Parse($(if ($left.Groups[$i].Success) {$left.Groups[$i].Value} else {'0'}))
        $b = [Numerics.BigInteger]::Parse($(if ($right.Groups[$i].Success) {$right.Groups[$i].Value} else {'0'}))
        if ($a -lt $b) { throw 'Candidate below the current minimum version.' }
        if ($a -gt $b) { break }
    }
}
if ($Phase -eq 'Check') { Write-Host "Policy preflight ready, revision=$($policy.revision). No write."; return }
# Public release must already be confirmed. A draft or partial release never becomes recommended.
$release = Invoke-RestMethod -Uri "https://api.github.com/repos/yueyunfe21/schoolProjectC11/releases/tags/client-$version" -Headers @{
    Authorization="Bearer $env:GH_TOKEN"; Accept='application/vnd.github+json'; 'X-GitHub-Api-Version'='2022-11-28'
} -TimeoutSec 30 -MaximumRedirection 0
if ($release.draft -or $release.prerelease -or $release.html_url -cne $releaseUrl) { throw 'Exact public release is not available.' }
foreach ($name in @($candidate.packageFile, 'update-manifest.json')) {
    $asset = @($release.assets | Where-Object { $_.name -ceq $name })
    $local = Join-Path $PromotionDirectory $name
    $hash = (Get-FileHash -LiteralPath $local -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($asset.Count -ne 1 -or $asset[0].state -cne 'uploaded' -or $asset[0].digest -cne "sha256:$hash") {
        throw 'Release asset mismatch; policy not activated.'
    }
}
# RETURNING is the exact mutated row; do not use meta.changes (audit triggers may add to it).
$written = Invoke-PolicyQuery 'UPDATE client_version_policies SET revision=revision+1, latest_version=?, release_url=?, updated_at=CURRENT_TIMESTAMP WHERE app_id=? AND revision=? RETURNING revision,latest_version,release_url' @($version,$releaseUrl,'dhxy',[long]$env:EXPECTED_POLICY_REVISION)
if (@($written.results).Count -ne 1 -or $written.results[0].latest_version -cne $version -or
    $written.results[0].revision -ne ([long]$env:EXPECTED_POLICY_REVISION + 1)) {
    throw 'Release is public but recommendation CAS failed. Read the policy; do not blindly republish.'
}
"正式版本已公告：$version。推荐政策 revision=$($written.results[0].revision)，未改变已有禁版/最低版本规则。" |
    Out-File -LiteralPath $env:GITHUB_STEP_SUMMARY -Append -Encoding utf8
