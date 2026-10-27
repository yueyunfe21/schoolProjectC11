# Owner provisioning only. Git credentials stay in memory; private PEM goes to Python over stdin
# and only libsodium ciphertext goes to the fixed GitHub API. Never print any credential/value.
param([Parameter(Mandatory)][string]$PrivateKeyFile,
      [Parameter(Mandatory)][string]$Python)
$ErrorActionPreference = 'Stop'
$api = 'https://api.github.com/repos/yueyunfe21/schoolProjectC11'
$oldPrompt = $env:GIT_TERMINAL_PROMPT
$oldInteractive = $env:GCM_INTERACTIVE
try {
    $env:GIT_TERMINAL_PROMPT = '0'
    $env:GCM_INTERACTIVE = 'never'
    $credential = "protocol=https`nhost=github.com`n`n" | git -c credential.interactive=false credential fill
    if ($LASTEXITCODE -ne 0) { throw 'GitHub administrative login required.' }
    $fields = @{}
    foreach ($line in $credential) { $pair=$line -split '=',2; if ($pair.Count -eq 2) { $fields[$pair[0]]=$pair[1] } }
    if (-not $fields['password']) { throw 'GitHub token unavailable.' }
    $headers = @{ Authorization=('Bearer '+$fields['password']); Accept='application/vnd.github+json'; 'X-GitHub-Api-Version'='2022-11-28' }
    $repository = Invoke-RestMethod -Uri $api -Headers $headers -MaximumRedirection 0 -TimeoutSec 20
    if (-not $repository.permissions.admin) { throw 'Repository administration permission required.' }
    # GitHub Free private repositories do not provide the Environment protection used by the old
    # public pipeline. Repository secrets plus the workflow's actor/master/confirmation gates are
    # the supported boundary; values are still encrypted before leaving this process.
    $secretApi = "$api/actions/secrets"
    $existing = Invoke-WebRequest -Uri "$secretApi/CLIENT_UPDATE_SIGNING_KEY" -Headers $headers -SkipHttpErrorCheck -MaximumRedirection 0 -TimeoutSec 20
    if ($existing.StatusCode -eq 200) { throw 'Signing secret already exists; no implicit replacement.' }
    if ($existing.StatusCode -ne 404) { throw 'Cannot establish signing-secret absence.' }
    $publicKey = Invoke-RestMethod -Uri "$secretApi/public-key" -Headers $headers -MaximumRedirection 0 -TimeoutSec 20
    $secure = Get-Content -LiteralPath $PrivateKeyFile -Raw | ConvertTo-SecureString
    $plain = [Net.NetworkCredential]::new('', $secure).Password
    $inputJson = @{ key=$publicKey.key; value=$plain } | ConvertTo-Json -Compress
    $cipher = $inputJson | & $Python -c 'import sys,json,base64; from nacl.public import PublicKey,SealedBox; p=json.load(sys.stdin); print(base64.b64encode(SealedBox(PublicKey(base64.b64decode(p["key"]))).encrypt(p["value"].encode("utf-8"))).decode("ascii"))'
    if ($LASTEXITCODE -ne 0 -or -not $cipher) { throw 'Secret encryption failed.' }
    $body = @{ encrypted_value=$cipher.Trim(); key_id=$publicKey.key_id } | ConvertTo-Json -Compress
    Invoke-RestMethod -Method Put -Uri "$secretApi/CLIENT_UPDATE_SIGNING_KEY" -Headers $headers -ContentType 'application/json' -Body $body -MaximumRedirection 0 -TimeoutSec 20 | Out-Null
    $metadata = Invoke-RestMethod -Uri "$secretApi/CLIENT_UPDATE_SIGNING_KEY" -Headers $headers -MaximumRedirection 0 -TimeoutSec 20
    Write-Host "Configured repository secret $($metadata.name). Production workflow remains manual and master-only."
} finally {
    $env:GIT_TERMINAL_PROMPT=$oldPrompt
    $env:GCM_INTERACTIVE=$oldInteractive
    $credential=$null; $fields=$null; $headers=$null; $plain=$null; $secure=$null; $inputJson=$null; $body=$null
}
