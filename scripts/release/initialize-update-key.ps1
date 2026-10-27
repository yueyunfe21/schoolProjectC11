# Owner-only, explicit one-time key provisioning. Never runs in the product or release workflow.
# The private PEM is DPAPI encrypted for this Windows user; only the public key enters Git.
param([Parameter(Mandatory)][string]$PrivateKeyFile)
$ErrorActionPreference = 'Stop'
if ($env:GITHUB_ACTIONS -eq 'true') { throw 'Provision the signing identity on the owner machine, not ephemeral CI.' }
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$secretPath = [IO.Path]::GetFullPath($PrivateKeyFile)
$publicPath = Join-Path $repoRoot 'src/main/resources/update-public-key.pem'
if ($secretPath.StartsWith($repoRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Private identity must be outside the repository.'
}
if ((Test-Path -LiteralPath $secretPath) -or (Test-Path -LiteralPath $publicPath)) {
    throw 'An identity already exists. Never rotate an installed trust key implicitly.'
}
$rsa = [Security.Cryptography.RSA]::Create(3072)
try {
    $privatePem = $rsa.ExportPkcs8PrivateKeyPem()
    $encrypted = ConvertTo-SecureString -String $privatePem -AsPlainText -Force | ConvertFrom-SecureString
    New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($secretPath)) -Force | Out-Null
    [IO.File]::WriteAllText($secretPath, $encrypted, [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText($publicPath, $rsa.ExportSubjectPublicKeyInfoPem(), [Text.UTF8Encoding]::new($false))
    $fingerprint = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($rsa.ExportSubjectPublicKeyInfo())).ToLowerInvariant()
    Write-Host "Created owner-encrypted signing identity. Public SHA256: $fingerprint"
} finally { $rsa.Dispose(); $privatePem = $null; $encrypted = $null }
