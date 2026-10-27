<# Upload one immutable release object through R2's S3 API and verify remote size/hash metadata. #>
param([Parameter(Mandatory)][string]$File,
      [Parameter(Mandatory)][ValidatePattern('^prod/client-[0-9]+\.[0-9]+\.[0-9]+-b[0-9]+\.[0-9]+/[A-Za-z0-9._-]+$')][string]$ObjectKey)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ($env:GITHUB_ACTIONS -ne 'true' -or $env:GITHUB_REF -ne 'refs/heads/master' -or
    $env:PRODUCTION_PACKAGE_APPROVED -ne 'true' -or $env:PRODUCTION_CONFIRMATION -cne 'PUBLISH-ACCEPTED-GAMMA') {
    throw 'R2 upload is restricted to the confirmed production workflow.'
}
foreach ($name in @('CLOUDFLARE_ACCOUNT_ID','R2_ACCESS_KEY_ID','R2_SECRET_ACCESS_KEY','R2_BUCKET_NAME')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) { throw "Missing R2 secret/config: $name" }
}
if ($env:CLOUDFLARE_ACCOUNT_ID -notmatch '^[a-f0-9]{32}$' -or $env:R2_BUCKET_NAME -notmatch '^[A-Za-z0-9._-]{3,63}$') {
    throw 'Invalid fixed R2 account/bucket configuration.'
}
$item = Get-Item -LiteralPath $File
if (-not $item -or $item.Length -le 0 -or $item.Length -gt 1073741824) { throw 'Invalid R2 release object.' }
$payloadHash = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
$hostName = "$($env:CLOUDFLARE_ACCOUNT_ID).r2.cloudflarestorage.com"
function Encode-Segment([string]$value) { [Uri]::EscapeDataString($value).Replace('%7E','~') }
$encodedKey = (($ObjectKey -split '/') | ForEach-Object { Encode-Segment $_ }) -join '/'
$canonicalUri = "/$(Encode-Segment $env:R2_BUCKET_NAME)/$encodedKey"
$uri = "https://$hostName$canonicalUri"

function Get-Hmac([byte[]]$key, [string]$text) {
    $hmac = [Security.Cryptography.HMACSHA256]::new($key)
    try { return $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($text)) }
    finally { $hmac.Dispose() }
}
function Get-Sha256Hex([string]$text) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return [Convert]::ToHexString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))).ToLowerInvariant() }
    finally { $sha.Dispose() }
}
function New-R2Headers([string]$method, [string]$contentHash, [bool]$withMetadata) {
    $now = [DateTime]::UtcNow
    $amzDate = $now.ToString('yyyyMMddTHHmmssZ')
    $dateStamp = $now.ToString('yyyyMMdd')
    $canonicalHeaders = "host:$hostName`n" + "x-amz-content-sha256:$contentHash`n" + "x-amz-date:$amzDate`n"
    $signedHeaders = 'host;x-amz-content-sha256;x-amz-date'
    if ($withMetadata) {
        $canonicalHeaders += "x-amz-meta-sha256:$payloadHash`n"
        $signedHeaders += ';x-amz-meta-sha256'
    }
    $canonicalRequest = "$method`n$canonicalUri`n`n$canonicalHeaders`n$signedHeaders`n$contentHash"
    $scope = "$dateStamp/auto/s3/aws4_request"
    $stringToSign = "AWS4-HMAC-SHA256`n$amzDate`n$scope`n$(Get-Sha256Hex $canonicalRequest)"
    $dateKey = Get-Hmac ([Text.Encoding]::UTF8.GetBytes("AWS4$($env:R2_SECRET_ACCESS_KEY)")) $dateStamp
    $regionKey = Get-Hmac $dateKey 'auto'
    $serviceKey = Get-Hmac $regionKey 's3'
    $signingKey = Get-Hmac $serviceKey 'aws4_request'
    $signature = [Convert]::ToHexString((Get-Hmac $signingKey $stringToSign)).ToLowerInvariant()
    $headers = @{
        'x-amz-date'=$amzDate
        'x-amz-content-sha256'=$contentHash
        Authorization="AWS4-HMAC-SHA256 Credential=$($env:R2_ACCESS_KEY_ID)/$scope, SignedHeaders=$signedHeaders, Signature=$signature"
    }
    if ($withMetadata) { $headers['x-amz-meta-sha256']=$payloadHash }
    return $headers
}

# HEAD first: immutable keys are never overwritten. An exact existing object makes a retry
# idempotent after an unknown network outcome; a mismatch requires owner investigation.
$head = Invoke-WebRequest -Method Head -Uri $uri -Headers (New-R2Headers 'HEAD' 'UNSIGNED-PAYLOAD' $false) `
    -SkipHttpErrorCheck -MaximumRedirection 0 -TimeoutSec 60
if ($head.StatusCode -eq 200) {
    $existingSize = [long]$head.Headers['Content-Length']
    $existingHash = [string]$head.Headers['x-amz-meta-sha256']
    if ($existingSize -eq $item.Length -and $existingHash -ceq $payloadHash) {
        Write-Host "R2 immutable object already verified: $ObjectKey ($existingSize bytes)"
        return
    }
    throw "R2 object key already exists with different identity: $ObjectKey"
}
if ($head.StatusCode -ne 404) { throw "Cannot establish R2 object absence: HTTP $($head.StatusCode)" }
$uploaded = Invoke-WebRequest -Method Put -Uri $uri -Headers (New-R2Headers 'PUT' $payloadHash $true) `
    -ContentType 'application/octet-stream' -InFile $item.FullName -MaximumRedirection 0 -TimeoutSec 1200
if ($uploaded.StatusCode -notin @(200,201)) { throw "R2 upload failed: HTTP $($uploaded.StatusCode)" }
$verified = Invoke-WebRequest -Method Head -Uri $uri -Headers (New-R2Headers 'HEAD' 'UNSIGNED-PAYLOAD' $false) `
    -MaximumRedirection 0 -TimeoutSec 60
$remoteSize = [long]$verified.Headers['Content-Length']
$remoteHash = [string]$verified.Headers['x-amz-meta-sha256']
if ($verified.StatusCode -ne 200 -or $remoteSize -ne $item.Length -or $remoteHash -cne $payloadHash) {
    throw "R2 remote verification failed for immutable object: $ObjectKey"
}
Write-Host "R2 immutable object verified: $ObjectKey ($remoteSize bytes)"
