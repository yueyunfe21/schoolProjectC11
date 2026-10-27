<#
Publish an accepted, unchanged Gamma package to private R2 authorization distribution and keep a
GitHub Release archive. During the bridge period a public repository also keeps old clients alive;
after migration the same release becomes private without changing the R2 client path.
#>
param([Parameter(Mandatory)][string]$PromotionDirectory)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ($env:GITHUB_ACTIONS -ne 'true' -or $env:GITHUB_REF -ne 'refs/heads/master' -or
    $env:PRODUCTION_PACKAGE_APPROVED -ne 'true' -or $env:PRODUCTION_CONFIRMATION -cne 'PUBLISH-ACCEPTED-GAMMA') {
    throw 'Publication requires exact confirmation on the master Actions workflow.'
}
$repo = $env:GITHUB_REPOSITORY
if ($repo -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$' -or [string]::IsNullOrWhiteSpace($env:GH_TOKEN)) {
    throw 'Repository or Actions token missing.'
}
$requestPath = Join-Path $PromotionDirectory 'promotion-request.json'
$candidatePath = Join-Path $PromotionDirectory 'candidate.json'
foreach ($path in @($requestPath, $candidatePath)) {
    if ((Get-Item -LiteralPath $path).Length -gt 65536) { throw 'Release metadata exceeds size limit.' }
}
$request = Get-Content -LiteralPath $requestPath -Raw | ConvertFrom-Json
$candidate = Get-Content -LiteralPath $candidatePath -Raw | ConvertFrom-Json
if ($request.schemaVersion -ne 1 -or $request.status -cne 'PREPARED_ONLY' -or
    $request.product -cne 'dhxy-client' -or $request.channel -cne 'prod' -or
    $request.productionActivated -ne $false -or [string]$request.promotionRunId -cne $env:GITHUB_RUN_ID -or
    $request.requestedUpdateMode -cne 'optional') {
    throw "Only this run's optional publication request is supported. Required updates need server admission first."
}
if ($candidate.schemaVersion -ne 1 -or $candidate.product -cne 'dhxy-client' -or
    $candidate.platform -cne 'windows-x64' -or $candidate.format -cne 'portable-app-image' -or
    $candidate.dirty -ne $false -or $candidate.sourceRepository -cne $repo -or
    $candidate.sourceCommit -notmatch '^[a-f0-9]{40}$' -or
    [string]$candidate.sourceRunId -cne $env:CANDIDATE_RUN_ID -or
    [string]$candidate.sourceRunAttempt -cne $env:CANDIDATE_RUN_ATTEMPT -or
    $candidate.buildId -cne "$env:CANDIDATE_RUN_ID.$env:CANDIDATE_RUN_ATTEMPT" -or
    $candidate.buildId -notmatch '^[1-9][0-9]{0,17}\.[1-9][0-9]{0,3}$' -or
    $candidate.version -notmatch '^\d+\.\d+\.\d+$' -or $candidate.sha256 -notmatch '^[a-f0-9]{64}$') {
    throw 'Candidate identity mismatch; no publication.'
}
$tag = "client-$($candidate.version)-b$($candidate.buildId)"
$filename = "$tag-windows-x64.zip"
if ($candidate.candidate -cne $tag -or $candidate.packageFile -cne $filename -or
    $candidate.sizeBytes -le 0 -or $candidate.sizeBytes -gt 1073741824) { throw 'Invalid package identity/size.' }
foreach ($field in @('sourceCommit','sourceRepository','buildId','version','packageFile','sha256','sizeBytes')) {
    if ([string]$request.candidate.$field -cne [string]$candidate.$field) { throw "Request/candidate mismatch: $field" }
}
$packagePath = Join-Path $PromotionDirectory $filename
if ((Get-Item -LiteralPath $packagePath).Length -ne $candidate.sizeBytes -or
    (Get-FileHash -LiteralPath $packagePath -Algorithm SHA256).Hash.ToLowerInvariant() -cne $candidate.sha256) {
    throw 'Package differs from the accepted Gamma bytes.'
}
if ([string]::IsNullOrWhiteSpace($request.releaseNotes) -or $request.releaseNotes.Length -gt 10000) {
    throw 'Invalid release notes.'
}
$headers = @{ Authorization="Bearer $env:GH_TOKEN"; Accept='application/vnd.github+json'; 'X-GitHub-Api-Version'='2022-11-28' }
$api = "https://api.github.com/repos/$repo"
$repository = Invoke-RestMethod -Uri $api -Headers $headers -TimeoutSec 30 -MaximumRedirection 0
# Independently resolve the accepted run before creating any tag/release. The prepared artifact
# belongs to this workflow, but it is not by itself proof of the Gamma source branch and attempt.
$source = Invoke-RestMethod -Uri "$api/actions/runs/$env:CANDIDATE_RUN_ID/attempts/$env:CANDIDATE_RUN_ATTEMPT" -Headers $headers -TimeoutSec 30 -MaximumRedirection 0
if ($source.status -cne 'completed' -or $source.conclusion -cne 'success' -or
    $source.head_branch -cne 'master' -or $source.head_sha -cne $candidate.sourceCommit -or
    $source.head_repository.full_name -cne $repo -or $source.path -cne '.github/workflows/client-gamma.yml' -or
    $source.event -notin @('push','workflow_dispatch')) { throw 'Gamma provenance verification failed.' }

& (Join-Path $PSScriptRoot 'activate-version-policy.ps1') -Phase Check -PromotionDirectory $PromotionDirectory
& (Join-Path $PSScriptRoot 'sign-update.ps1') -CandidateDirectory $PromotionDirectory

# Do not reuse a tag or a previous draft implicitly: that could bind accepted bytes to a different
# source or replace an already published release. Read status codes; network/auth errors are not 404.
foreach ($uri in @("$api/git/ref/tags/$tag", "$api/releases/tags/$tag")) {
    $existing = Invoke-WebRequest -Uri $uri -Headers $headers -TimeoutSec 30 -MaximumRedirection 0 -SkipHttpErrorCheck
    if ($existing.StatusCode -eq 200) { throw "Tag/release already exists for $tag; inspect it, do not overwrite or republish." }
    if ($existing.StatusCode -ne 404) { throw "Unable to establish release absence: HTTP $($existing.StatusCode)." }
}
# R2 objects are immutable and versioned. A retry may reuse an exact remotely verified object,
# but a size/hash mismatch at the same key is never overwritten.
$r2Prefix = "prod/$tag"
& (Join-Path $PSScriptRoot 'publish-r2-object.ps1') -File $packagePath -ObjectKey "$r2Prefix/$filename"
& (Join-Path $PSScriptRoot 'publish-r2-object.ps1') -File (Join-Path $PromotionDirectory 'update-manifest.json') `
    -ObjectKey "$r2Prefix/update-manifest.json"
$body = $request.releaseNotes + "`n`nClient package archive. User update delivery is license-authorized through private R2. Runtime still requires authorization.`n`nGamma: $($candidate.sourceRunId).$($candidate.sourceRunAttempt)`nSHA256: $($candidate.sha256)"
$draftBody = @{ tag_name=$tag; target_commitish=$candidate.sourceCommit; name=$tag; body=$body;
    draft=$true; prerelease=$false; make_latest='false' } | ConvertTo-Json
$draft = Invoke-RestMethod -Method Post -Uri "$api/releases" -Headers $headers -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($draftBody)) -TimeoutSec 30 -MaximumRedirection 0
$releaseId = [long]$draft.id
if ($releaseId -le 0 -or -not $draft.draft -or $draft.tag_name -cne $tag) { throw 'Unexpected draft response; do not publish.' }
Write-Host "Created private draft $releaseId. Failure from here leaves the draft unpublished for inspection."

# Metadata is deliberately marked distribution-only, not a trusted auto-install policy. Future
# signed update manifests must have a distinct schema rather than silently trusting this file.
$downloadUrl = "https://github.com/$repo/releases/download/$tag/$filename"
$distribution = [ordered]@{
    schemaVersion=1; product='dhxy-client'; channel='prod'; purpose='private-r2-authorized-update'
    version=$candidate.version; buildId=$candidate.buildId; sourceCommit=$candidate.sourceCommit
    packageFile=$filename; downloadUrl=$downloadUrl; sizeBytes=$candidate.sizeBytes; sha256=$candidate.sha256
    automaticInstallationAvailable=$true; serverVersionPolicyActivated=$false; artifactSigned=$true
}
$distributionPath = Join-Path $PromotionDirectory 'distribution.json'
[IO.File]::WriteAllText($distributionPath, ($distribution | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
$files = @($packagePath, $candidatePath, $distributionPath, (Join-Path $PromotionDirectory 'update-manifest.json'))
foreach ($file in $files) {
    $name = [IO.Path]::GetFileName($file)
    $size = (Get-Item -LiteralPath $file).Length
    $hash = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
    # Fixed GitHub upload host, never a URL supplied by the artifact. Never follow credential redirects.
    $upload = "https://uploads.github.com/repos/$repo/releases/$releaseId/assets?name=$([Uri]::EscapeDataString($name))"
    $asset = Invoke-RestMethod -Method Post -Uri $upload -Headers $headers -ContentType 'application/octet-stream' `
        -InFile $file -TimeoutSec 600 -MaximumRedirection 0
    if ($asset.name -cne $name -or $asset.state -cne 'uploaded' -or $asset.size -ne $size -or
        $asset.digest -cne "sha256:$hash") { throw "Uploaded asset verification failed: $name. Draft remains unpublished." }
}
# Re-read the draft once all assets are uploaded. A partial/extra set must never become latest.
$ready = Invoke-RestMethod -Uri "$api/releases/$releaseId" -Headers $headers -TimeoutSec 30 -MaximumRedirection 0
if (-not $ready.draft -or $ready.tag_name -cne $tag -or @($ready.assets).Count -ne $files.Count) {
    throw 'Draft identity or asset set changed during upload.'
}
foreach ($file in $files) {
    $name = [IO.Path]::GetFileName($file)
    $matching = @($ready.assets | Where-Object { $_.name -ceq $name })
    $hash = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($matching.Count -ne 1 -or $matching[0].state -cne 'uploaded' -or
        $matching[0].size -ne (Get-Item -LiteralPath $file).Length -or $matching[0].digest -cne "sha256:$hash") {
        throw "Final asset check failed: $name. Draft remains unpublished."
    }
}
$published = Invoke-RestMethod -Method Patch -Uri "$api/releases/$releaseId" -Headers $headers -ContentType 'application/json' `
    -Body '{"draft":false,"prerelease":false,"make_latest":"true"}' -TimeoutSec 30 -MaximumRedirection 0
if ($published.draft -or $published.prerelease -or $published.tag_name -cne $tag) {
    throw 'Publication confirmation is uncertain; inspect the GitHub release before retrying.'
}
& (Join-Path $PSScriptRoot 'activate-version-policy.ps1') -Phase Activate -PromotionDirectory $PromotionDirectory
@"
## 客户端 Prod 已晋级
- 版本：$tag
- GitHub 归档：https://github.com/$repo/releases/tag/$tag
- 用户分发：私有 R2 授权下载（客户端不展示对象 URL）
- SHA256：$($candidate.sha256)
- 同一 Gamma 原包，未重新构建。软件运行仍需授权。
- exact Gamma 与签名清单已上传到版本化 R2 键并核验，推荐版本公告已激活；原有允许/撤销/最低版本规则未改。
"@ | Out-File -LiteralPath $env:GITHUB_STEP_SUMMARY -Append -Encoding utf8
