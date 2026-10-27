<#
Build a client-only Windows x64 candidate into a new isolated directory. No tests or application
startup. CaptureBundle is an already built PyInstaller onedir output, never an arbitrary URL.
Only tracked templates and a short explicit config allowlist are copied; runtime data is excluded.
#>
param(
    [Parameter(Mandatory)][ValidatePattern('^[1-9][0-9]*\.[1-9][0-9]*$')][string]$BuildId,
    [Parameter(Mandatory)][string]$CaptureBundle,
    [string]$OutputRoot,
    [switch]$AllowDirty
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location -LiteralPath $repoRoot
if (-not [Environment]::Is64BitProcess -or $env:OS -ne 'Windows_NT') { throw 'Windows x64 build host required.' }
if (-not $AllowDirty -and (git status --porcelain)) { throw 'Candidate builds require a clean checkout.' }
if ($AllowDirty -and $env:GITHUB_ACTIONS -eq 'true') { throw 'CI cannot publish a dirty candidate.' }
$sourceCommit = (git rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve source commit.' }
$version = (Get-Content -LiteralPath (Join-Path $repoRoot 'release/version.txt') -Raw).Trim()
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'release/version.txt must contain MAJOR.MINOR.PATCH.' }
$candidate = "client-$version-b$BuildId"
if (-not $OutputRoot) { $OutputRoot = Join-Path $repoRoot ('.release-build/' + [Guid]::NewGuid().ToString('N')) }
$buildRoot = [IO.Path]::GetFullPath($OutputRoot)
if ([IO.Path]::GetPathRoot($buildRoot) -ne [IO.Path]::GetPathRoot($repoRoot)) {
    throw 'Maven compiler requires isolated output on the SAME drive as source. Use .release-build/<new-id>.'
}
if (Test-Path -LiteralPath $buildRoot) { throw 'Build output must be a NEW directory; no cleanup/overwrite is performed.' }
if (-not (Test-Path -LiteralPath (Join-Path $CaptureBundle 'dhxy-capture.exe') -PathType Leaf)) {
    throw 'CaptureBundle must be a complete dhxy-capture onedir build.'
}
New-Item -ItemType Directory -Path $buildRoot | Out-Null
$mavenRoot = Join-Path $buildRoot 'maven'
$inputRoot = Join-Path $buildRoot 'input'
$output = Join-Path $buildRoot 'artifacts'
New-Item -ItemType Directory -Path $inputRoot,$output | Out-Null
$mavenArgs = @('-B','-ntp','-Pclient-release',"-Ddhxy.build.directory=$mavenRoot",'-Dmaven.test.skip=true')
& mvn @mavenArgs compile dependency:copy-dependencies '-DincludeScope=runtime' "-DoutputDirectory=$inputRoot/lib"
if ($LASTEXITCODE -ne 0) { throw 'Isolated Maven compilation/dependency resolution failed.' }

# Rewrite ONLY generated build resources. Preserve ordinary task defaults, remove all tracked
# credential values, forbid automatic local Cloud startup, stamp one client build version.
$propertiesFile = Join-Path $mavenRoot 'classes/application.properties'
$properties = Get-Content -LiteralPath $propertiesFile
$properties = $properties | Where-Object { $_ -notmatch '^(bot\.app-version|license\.worker\.app-version|cloud\.turn\.base-uri|cloud\.turn\.sidecar\.auto-start-enabled|bot\.run\.auto-start)=' }
$properties = $properties | ForEach-Object {
    if ($_ -match '(?i)^([^#=]*(?:token|password|secret|api-key|api\.key|credential))\s*=') { $Matches[1] + '=' }
    else { $_ }
}
$properties += @("bot.app-version=$version-b$BuildId", "license.worker.app-version=$version-b$BuildId",
    'cloud.turn.base-uri=https://unconfigured.invalid', 'cloud.turn.sidecar.auto-start-enabled=false', 'bot.run.auto-start=false')
[IO.File]::WriteAllLines($propertiesFile, $properties, [Text.UTF8Encoding]::new($false))
& mvn @mavenArgs jar:jar
if ($LASTEXITCODE -ne 0) { throw 'Client jar creation failed.' }
Copy-Item -LiteralPath (Join-Path $mavenRoot 'dhxy-client.jar') -Destination $inputRoot

# jpackage binds a runtime to the application. No compile/test/run of Cloud and no Maven on users' PCs.
$jpackage = Join-Path $env:JAVA_HOME 'bin/jpackage.exe'
$jvmOptionsFile = Join-Path $repoRoot 'config/client-jvm.options'
if (-not (Test-Path -LiteralPath $jvmOptionsFile -PathType Leaf)) {
    throw "Missing shared client JVM options: $jvmOptionsFile"
}
$jvmOptions = @(Get-Content -LiteralPath $jvmOptionsFile -Encoding UTF8 |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and -not $_.StartsWith('#') })
if ($jvmOptions.Count -eq 0 -or ($jvmOptions | Where-Object { $_ -notmatch '^-[^\s]+$' })) {
    throw "Client JVM options must contain one whitespace-free argument per line: $jvmOptionsFile"
}
$jvmOptionArgs = @()
foreach ($jvmOption in $jvmOptions) {
    $jvmOptionArgs += @('--java-options', $jvmOption)
}
& $jpackage --type app-image --name DHXY --app-version $version --vendor DHXY `
    --input $inputRoot --main-jar dhxy-client.jar --main-class com.bot.dhxy.AutoBot `
    --jlink-options '--strip-debug --no-man-pages --no-header-files' `
    --dest (Join-Path $buildRoot 'image') @jvmOptionArgs
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }
$appRoot = Join-Path $buildRoot 'image/DHXY'
# jpackage defaults strip native commands; the external updater needs the bundled javaw.exe.
# Keep those launchers and refuse a package that could download but never start installation.
foreach ($launcher in @('java.exe', 'javaw.exe')) {
    if (-not (Test-Path -LiteralPath (Join-Path $appRoot "runtime/bin/$launcher") -PathType Leaf)) {
        throw "Packaged updater runtime is missing $launcher."
    }
}
# Paths returned by git are scoped to this explicit production template folder, not a root scan.
git -c core.quotepath=false ls-files images/template | ForEach-Object {
    $relative = $_
    $destination = Join-Path $appRoot $relative
    New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot $relative) -Destination $destination
}
if ($LASTEXITCODE -ne 0) { throw 'Cannot enumerate tracked templates.' }
New-Item -ItemType Directory -Path (Join-Path $appRoot 'config') -Force | Out-Null
foreach ($name in @('dialog_choice_memory.json','map_camera_bounds.json','transfer_choice_memory.json')) {
    Copy-Item -LiteralPath (Join-Path $repoRoot "config/$name") -Destination (Join-Path $appRoot 'config')
}
Copy-Item -LiteralPath (Join-Path $repoRoot 'release/client-config.example.properties') -Destination (Join-Path $appRoot 'config/application.properties.example')
foreach ($name in @('Start-DHXY.cmd','Start-DHXY.ps1','README-client.txt')) {
    Copy-Item -LiteralPath (Join-Path $repoRoot "release/$name") -Destination $appRoot
}
# Only the caller-specified narrow build output is copied, not an installed Python or home folder.
Copy-Item -LiteralPath $CaptureBundle -Destination (Join-Path $appRoot 'capture') -Recurse
$metadata = [ordered]@{
    schemaVersion=1; product='dhxy-client'; version=$version; buildId=$BuildId; candidate=$candidate
    sourceCommit=$sourceCommit; sourceRepository=$env:GITHUB_REPOSITORY
    sourceRunId=$env:GITHUB_RUN_ID; sourceRunAttempt=$env:GITHUB_RUN_ATTEMPT
    platform='windows-x64'; builtAt=[DateTime]::UtcNow.ToString('o'); dirty=[bool]$AllowDirty
    validation='NOT_TESTED'; format='portable-app-image'; requires=@('signed-FakerInput-driver','external-Cloud-configuration')
    updateKeySha256=(Get-FileHash -LiteralPath (Join-Path $repoRoot 'src/main/resources/update-public-key.pem') -Algorithm SHA256).Hash.ToLowerInvariant()
}
[IO.File]::WriteAllText((Join-Path $appRoot 'build-info.json'), ($metadata | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
$zip = Join-Path $output "$candidate-windows-x64.zip"
Compress-Archive -LiteralPath $appRoot -DestinationPath $zip -CompressionLevel Optimal
$metadata['packageFile'] = [IO.Path]::GetFileName($zip)
$metadata['sha256'] = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
$metadata['sizeBytes'] = (Get-Item -LiteralPath $zip).Length
[IO.File]::WriteAllText((Join-Path $output 'candidate.json'), ($metadata | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
if ($env:GITHUB_OUTPUT) {
    "artifact_path=$output" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Append -Encoding utf8
    "candidate=$candidate" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Append -Encoding utf8
}
Write-Host "Candidate built (NOT TESTED, NOT RELEASED): $output"
