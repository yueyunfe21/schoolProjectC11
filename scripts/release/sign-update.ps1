# Sign raw payload bytes to avoid JSON canonicalization ambiguity between PowerShell and Java.
# CI secret is scoped to this step. No candidate code is executed and no private key is written.
param([Parameter(Mandatory)][string]$CandidateDirectory)
$ErrorActionPreference = 'Stop'
if ($env:GITHUB_ACTIONS -ne 'true' -or $env:GITHUB_REF -ne 'refs/heads/master') { throw 'Signing is restricted to the production workflow on master.' }
if ([string]::IsNullOrWhiteSpace($env:CLIENT_UPDATE_SIGNING_KEY)) { throw 'CLIENT_UPDATE_SIGNING_KEY is not configured.' }
$candidate = Get-Content -LiteralPath (Join-Path $CandidateDirectory 'candidate.json') -Raw | ConvertFrom-Json
$publicPath = Join-Path $PSScriptRoot '../../src/main/resources/update-public-key.pem'
if ($candidate.updateKeySha256 -cne (Get-FileHash -LiteralPath $publicPath -Algorithm SHA256).Hash.ToLowerInvariant()) {
    throw 'Gamma candidate does not embed the current trusted public key; rebuild and accept a new candidate.'
}
$version = "$($candidate.version)-b$($candidate.buildId)"
$payload = [ordered]@{
    schemaVersion=1; product='dhxy-client'; channel='prod'; platform='windows-x64'
    version=$version; candidate=$candidate.candidate; packageFile=$candidate.packageFile
    sizeBytes=$candidate.sizeBytes; sha256=$candidate.sha256; sourceCommit=$candidate.sourceCommit
    repository='yueyunfe21/schoolProjectC11'
}
$bytes = [Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json -Compress))
$rsa = [Security.Cryptography.RSA]::Create()
$trusted = [Security.Cryptography.RSA]::Create()
try {
    $rsa.ImportFromPem($env:CLIENT_UPDATE_SIGNING_KEY)
    $trusted.ImportFromPem([IO.File]::ReadAllText($publicPath))
    if ([Convert]::ToBase64String($rsa.ExportSubjectPublicKeyInfo()) -cne [Convert]::ToBase64String($trusted.ExportSubjectPublicKeyInfo())) {
        throw 'CI signing key does not match the client trust key.'
    }
    $envelope = @{ schemaVersion=1; algorithm='SHA256withRSA'; payload=[Convert]::ToBase64String($bytes)
        signature=[Convert]::ToBase64String($rsa.SignData($bytes, [Security.Cryptography.HashAlgorithmName]::SHA256, [Security.Cryptography.RSASignaturePadding]::Pkcs1)) }
    [IO.File]::WriteAllText((Join-Path $CandidateDirectory 'update-manifest.json'), ($envelope | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
} finally { $rsa.Dispose(); $trusted.Dispose(); $env:CLIENT_UPDATE_SIGNING_KEY = $null }
