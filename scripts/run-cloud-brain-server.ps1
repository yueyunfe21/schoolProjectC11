param(
    [int]$Port = 18080,
    [string]$Path = "/api/cloud/decision",
    [string]$Token = "local-dev-token",
    [string]$BrainProjectPath = "D:\mavenProject\dhxy-cloud-brain",
    [int]$OcrPort = 18761,
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

if ($Path -ne "/api/cloud/decision") {
    throw "External dhxy-cloud-brain currently serves /api/cloud/decision; unsupported Path=$Path"
}

if (-not (Test-Path -LiteralPath $BrainProjectPath -PathType Container)) {
    throw "External dhxy-cloud-brain project not found: $BrainProjectPath"
}

$classesPath = Join-Path $BrainProjectPath "target\classes"
$classpathFile = Join-Path $BrainProjectPath "target\dependency-classpath.txt"
$mainClassName = "com.yueyunfe.dhxy.cloudbrain.CloudBrainApplication"
$mainClassFile = Join-Path $classesPath "com\yueyunfe\dhxy\cloudbrain\CloudBrainApplication.class"

function Get-NewestFileTimeUtc {
    param(
        [string[]]$Paths,
        [string[]]$IgnoredPathPrefixes = @()
    )

    $latest = [DateTime]::MinValue.ToUniversalTime()
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }
        $item = Get-Item -LiteralPath $path
        if (-not $item.PSIsContainer) {
            if ($item.LastWriteTimeUtc -gt $latest) {
                $latest = $item.LastWriteTimeUtc
            }
            continue
        }
        Get-ChildItem -LiteralPath $path -Recurse -File -Force | ForEach-Object {
            $ignored = $false
            foreach ($ignoredPrefix in $IgnoredPathPrefixes) {
                if ($_.FullName.StartsWith($ignoredPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
                    $ignored = $true
                    break
                }
            }
            if ($ignored) {
                return
            }
            if ($_.LastWriteTimeUtc -gt $latest) {
                $latest = $_.LastWriteTimeUtc
            }
        }
    }
    return $latest
}

function Format-Utc {
    param([DateTime]$Value)
    if ($Value -eq [DateTime]::MinValue.ToUniversalTime()) {
        return "missing"
    }
    return $Value.ToString("o")
}

function Get-CloudBrainRuntimeState {
    param([string]$ProjectPath)

    $sourcePaths = @(
        (Join-Path $ProjectPath "pom.xml"),
        (Join-Path $ProjectPath "src\main")
    )
    # Screenshots used only by manual cloud-brain experiments are not runtime inputs and Maven does not compile them.
    $sourceIgnoredPaths = @(
        (Join-Path $ProjectPath "src\main\java\com\yueyunfe\dhxy\cloudbrain\test")
    )
    $sourceMaxUtc = Get-NewestFileTimeUtc -Paths $sourcePaths -IgnoredPathPrefixes $sourceIgnoredPaths
    $classesMaxUtc = Get-NewestFileTimeUtc -Paths @($classesPath)
    $classpathUtc = if (Test-Path -LiteralPath $classpathFile -PathType Leaf) {
        (Get-Item -LiteralPath $classpathFile).LastWriteTimeUtc
    } else {
        [DateTime]::MinValue.ToUniversalTime()
    }
    $classpathSha256 = if (Test-Path -LiteralPath $classpathFile -PathType Leaf) {
        (Get-FileHash -LiteralPath $classpathFile -Algorithm SHA256).Hash.ToLowerInvariant()
    } else {
        ""
    }
    $pomUtc = (Get-Item -LiteralPath (Join-Path $ProjectPath "pom.xml")).LastWriteTimeUtc

    [pscustomobject]@{
        SourceMaxUtc = $sourceMaxUtc
        ClassesMaxUtc = $classesMaxUtc
        ClasspathUtc = $classpathUtc
        ClasspathSha256 = $classpathSha256
        PomUtc = $pomUtc
        MainClassPresent = Test-Path -LiteralPath $mainClassFile -PathType Leaf
        ClasspathPresent = Test-Path -LiteralPath $classpathFile -PathType Leaf
    }
}

function Test-CloudBrainClassesStale {
    param($State)

    if (-not $State.MainClassPresent) {
        Write-Host "Cloud brain classes are missing; main class not found: $mainClassFile"
        return $true
    }
    if (-not $State.ClasspathPresent) {
        Write-Host "Cloud brain dependency classpath file is missing: $classpathFile"
        return $true
    }
    if ($State.SourceMaxUtc -gt $State.ClassesMaxUtc) {
        Write-Host "Cloud brain classes are stale; sourceMaxUtc=$(Format-Utc $State.SourceMaxUtc) classesMaxUtc=$(Format-Utc $State.ClassesMaxUtc)"
        return $true
    }
    if ($State.PomUtc -gt $State.ClasspathUtc) {
        Write-Host "Cloud brain dependency classpath is stale; pomUtc=$(Format-Utc $State.PomUtc) classpathUtc=$(Format-Utc $State.ClasspathUtc)"
        return $true
    }
    return $false
}

function Invoke-CloudBrainClasspathPrepare {
    Push-Location $BrainProjectPath
    try {
        & mvn -q compile dependency:build-classpath "-Dmdep.includeScope=runtime" "-Dmdep.outputFile=$classpathFile" "-Dmdep.pathSeparator=;"
        $prepareExitCode = $LASTEXITCODE
        if ($prepareExitCode -ne 0) {
            throw "Cloud brain classpath prepare failed with exit code $prepareExitCode; refuse to start stale classes: $classesPath"
        }
    } finally {
        Pop-Location
    }
}

$state = Get-CloudBrainRuntimeState -ProjectPath $BrainProjectPath
if ($Rebuild -or (Test-CloudBrainClassesStale -State $state)) {
    Invoke-CloudBrainClasspathPrepare
    $state = Get-CloudBrainRuntimeState -ProjectPath $BrainProjectPath
}

if (Test-CloudBrainClassesStale -State $state) {
    throw "External dhxy-cloud-brain classes/resources are still stale after compile; sourceMaxUtc=$(Format-Utc $state.SourceMaxUtc) classesMaxUtc=$(Format-Utc $state.ClassesMaxUtc)"
}

$dependencyClasspath = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($dependencyClasspath)) {
    throw "External dhxy-cloud-brain dependency classpath is empty: $classpathFile"
}
$runtimeClasspath = "$classesPath;$dependencyClasspath"
$launchUtc = (Get-Date).ToUniversalTime().ToString("o")

# ---------------------------------------------------------------------------
# CR257 A.1-A.4: cloud-brain owns its OCR sidecar lifecycle.
#   A.1 identity-bearing health (protocolVersion/build/bind/pid) gates every accept.
#   A.2 registry-based stop: only a PID this launcher registered AND whose identity still
#       matches may be terminated; never kill by port.
#   A.3 occupied-but-mismatched port fails the launch with structured ocr-sidecar-conflict.
#   A.4 orphan with matching identity is reused; a sidecar dying later is NOT relaunched here
#       (OCR calls fail closed until the next launcher cycle).
# Logs carry runId/pid/version fingerprints and reasons only — never image data or OCR text.
# ---------------------------------------------------------------------------
$ocrExpectedProtocol = "dhxy-ocr-v2"
$ocrEndpoint = "http://127.0.0.1:$OcrPort"
$ocrServerScript = Join-Path $BrainProjectPath "ocr\local_ocr_server.py"
$ocrRegistryFile = Join-Path $BrainProjectPath "data\ocr-sidecar-registry.json"
$ocrLogDir = Join-Path $BrainProjectPath "logs"
$ocrRunId = [guid]::NewGuid().ToString()

function Get-OcrHealth {
    try {
        return Invoke-RestMethod -Uri "$ocrEndpoint/health" -Method Get -TimeoutSec 3
    } catch {
        return $null
    }
}

function Test-OcrIdentityMatch {
    param($Health)
    return ($null -ne $Health) -and ($Health.ok -eq $true) `
        -and ($Health.protocolVersion -eq $ocrExpectedProtocol) `
        -and (@("127.0.0.1", "localhost", "::1") -contains [string]$Health.bindHost)
}

function Get-OcrRegistry {
    if (-not (Test-Path -LiteralPath $ocrRegistryFile -PathType Leaf)) {
        return $null
    }
    try {
        return Get-Content -LiteralPath $ocrRegistryFile -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Test-RegisteredOcrProcess {
    param($Registry, $Health)
    if ($null -eq $Registry -or $null -eq $Health) {
        return $false
    }
    if ([long]$Registry.pid -ne [long]$Health.pid) {
        return $false
    }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($Registry.pid)" -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return $false
    }
    return ([string]$process.CommandLine) -like "*local_ocr_server.py*"
}

function Start-OcrSidecar {
    if (-not (Test-Path -LiteralPath $ocrServerScript -PathType Leaf)) {
        throw "ocr-sidecar-missing: sidecar script not found at $ocrServerScript (CR257 A: OCR ships with dhxy-cloud-brain)"
    }
    if (-not (Test-Path -LiteralPath $ocrLogDir -PathType Container)) {
        New-Item -ItemType Directory -Force -Path $ocrLogDir | Out-Null
    }
    $registryDir = Split-Path -Parent $ocrRegistryFile
    if (-not (Test-Path -LiteralPath $registryDir -PathType Container)) {
        New-Item -ItemType Directory -Force -Path $registryDir | Out-Null
    }
    $ocrLog = Join-Path $ocrLogDir "ocr-sidecar.log"
    $launcher = $null
    $launcherArgs = $null
    if (Get-Command "py" -ErrorAction SilentlyContinue) {
        $launcher = "py"
        $launcherArgs = @("-3", $ocrServerScript, "--host", "127.0.0.1", "--port", "$OcrPort")
    } elseif (Get-Command "python" -ErrorAction SilentlyContinue) {
        $launcher = "python"
        $launcherArgs = @($ocrServerScript, "--host", "127.0.0.1", "--port", "$OcrPort")
    } else {
        throw "ocr-sidecar-launch-failed: neither 'py' nor 'python' is available to start the OCR sidecar"
    }
    $process = Start-Process -FilePath $launcher -ArgumentList $launcherArgs `
        -RedirectStandardOutput $ocrLog -RedirectStandardError "$ocrLog.err" `
        -WindowStyle Hidden -PassThru
    $registry = [pscustomobject]@{
        runId = $ocrRunId
        pid = $process.Id
        startedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        command = "$launcher $($launcherArgs -join ' ')"
        expectedProtocol = $ocrExpectedProtocol
        port = $OcrPort
        mode = "launched"
    }
    $registry | ConvertTo-Json | Set-Content -LiteralPath $ocrRegistryFile -Encoding UTF8
    Write-Host "CR257 OCR sidecar launched: runId=$ocrRunId pid=$($process.Id) port=$OcrPort log=$ocrLog"
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 750
        $health = Get-OcrHealth
        if (Test-OcrIdentityMatch -Health $health) {
            Write-Host "CR257 OCR sidecar ready: pid=$($health.pid) protocolVersion=$($health.protocolVersion) build=$($health.sidecarBuild) model=$($health.modelFingerprint)"
            return
        }
        if ($process.HasExited) {
            throw "ocr-sidecar-launch-failed: sidecar process exited early (exitCode=$($process.ExitCode)); see $ocrLog"
        }
    }
    try { Stop-Process -Id $process.Id -Force -Confirm:$false -ErrorAction SilentlyContinue } catch {}
    throw "ocr-sidecar-launch-failed: health did not become ready within 90s; registered pid=$($process.Id) stopped"
}

$ocrHealth = Get-OcrHealth
if ($null -ne $ocrHealth) {
    if (-not (Test-OcrIdentityMatch -Health $ocrHealth)) {
        # A.3: occupied but identity mismatch (old script, wrong version, unknown service) —
        # fail closed; never take over, overwrite, or silently reuse. The foreign process stays.
        throw "ocr-sidecar-conflict: port $OcrPort is serving a non-matching health (protocolVersion='$($ocrHealth.protocolVersion)' expected='$ocrExpectedProtocol'); refusing to start"
    }
    $ocrRegistry = Get-OcrRegistry
    if (Test-RegisteredOcrProcess -Registry $ocrRegistry -Health $ocrHealth) {
        # A.2/A.4 normal restart: this is OUR registered previous instance — stop it (verified
        # PID + command line, never by port) and launch a fresh one.
        Write-Host "CR257 OCR sidecar restart: stopping registered previous instance pid=$($ocrRegistry.pid) runId=$($ocrRegistry.runId)"
        Stop-Process -Id $ocrRegistry.pid -Force -Confirm:$false
        Start-Sleep -Milliseconds 1500
        Start-OcrSidecar
    } else {
        # A.4 orphan with matching identity: reuse without adopting ownership.
        Write-Host "CR257 OCR sidecar reuse: identity-matched orphan pid=$($ocrHealth.pid) build=$($ocrHealth.sidecarBuild) model=$($ocrHealth.modelFingerprint) (not registered by this launcher; reuse only)"
        [pscustomobject]@{
            runId = $ocrRunId
            pid = $ocrHealth.pid
            startedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
            command = "(reused orphan; not launched by this runId)"
            expectedProtocol = $ocrExpectedProtocol
            port = $OcrPort
            mode = "reused"
        } | ConvertTo-Json | Set-Content -LiteralPath $ocrRegistryFile -Encoding UTF8
    }
} else {
    $portOwner = Get-NetTCPConnection -LocalPort $OcrPort -State Listen -ErrorAction SilentlyContinue
    if ($null -ne $portOwner) {
        # A.3: port is held by something that does not answer our health contract.
        throw "ocr-sidecar-conflict: port $OcrPort is occupied by pid=$(@($portOwner)[0].OwningProcess) but does not answer the dhxy-ocr health contract; refusing to start"
    }
    Start-OcrSidecar
}

Write-Host "Starting external DHXY cloud brain from fresh classpath on port $Port"
Write-Host "devArtifactMode=classpath launchUtc=$launchUtc projectPath=$BrainProjectPath classesPath=$classesPath classpathFile=$classpathFile sourceMaxUtc=$(Format-Utc $state.SourceMaxUtc) classesMaxUtc=$(Format-Utc $state.ClassesMaxUtc) classpathUtc=$(Format-Utc $state.ClasspathUtc) classpathSha256=$($state.ClasspathSha256)"
& java `
    "-Ddhxy.cloud.brain.localOcrEndpoint=$ocrEndpoint" `
    "-Ddhxy.cloud.brain.localOcrTimeoutMs=10000" `
    "-Ddhxy.cloudbrain.devArtifactMode=classpath" `
    "-Ddhxy.cloudbrain.launchUtc=$launchUtc" `
    "-Ddhxy.cloudbrain.projectPath=$BrainProjectPath" `
    "-Ddhxy.cloudbrain.classesPath=$classesPath" `
    "-Ddhxy.cloudbrain.classpathFile=$classpathFile" `
    "-Ddhxy.cloudbrain.launchSourceMaxUtc=$(Format-Utc $state.SourceMaxUtc)" `
    "-Ddhxy.cloudbrain.launchClassesMaxUtc=$(Format-Utc $state.ClassesMaxUtc)" `
    "-Ddhxy.cloudbrain.launchClasspathUtc=$(Format-Utc $state.ClasspathUtc)" `
    "-Ddhxy.cloudbrain.launchClasspathSha256=$($state.ClasspathSha256)" `
    -cp $runtimeClasspath `
    $mainClassName `
    "--port=$Port" `
    "--token=$Token"
