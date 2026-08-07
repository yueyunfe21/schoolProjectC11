param(
    [int]$Port = 18080,
    [string]$Path = "/api/cloud/decision",
    [string]$Token = "local-dev-token",
    [string]$BrainProjectPath = "D:\mavenProject\dhxy-cloud-brain",
    [string]$BusinessLogPath = "",
    [string]$TenantId = "",
    [string]$UserId = "",
    [string]$StateRoot = "",
    [int]$OcrPort = 18761,
    # CR257 review P1-1: a fresh/empty vision memory must be an explicit operator decision, never
    # an accident of a missing canonical file (the DHXY-side copy is deleted by C1).
    [switch]$AllowEmptyVisionMemory,
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

if ($Path -ne "/api/cloud/decision") {
    throw "External dhxy-cloud-brain currently serves /api/cloud/decision; unsupported Path=$Path"
}

if (-not (Test-Path -LiteralPath $BrainProjectPath -PathType Container)) {
    throw "External dhxy-cloud-brain project not found: $BrainProjectPath"
}

if ([string]::IsNullOrWhiteSpace($TenantId) -or
        [string]::IsNullOrWhiteSpace($UserId) -or
        [string]::IsNullOrWhiteSpace($StateRoot)) {
    throw "TURN-41 scope is required: pass explicit -TenantId, -UserId, and -StateRoot"
}
if (-not [System.IO.Path]::IsPathRooted($StateRoot)) {
    throw "TURN-41 StateRoot must be an absolute path: $StateRoot"
}
$StateRoot = [System.IO.Path]::GetFullPath($StateRoot)

if ([string]::IsNullOrWhiteSpace($BusinessLogPath)) {
    $BusinessLogPath = Join-Path (Split-Path -Parent $PSScriptRoot) "logs\cloud-brain-console.log"
}
$BusinessLogPath = [System.IO.Path]::GetFullPath($BusinessLogPath)
$businessLogDirectory = Split-Path -Parent $BusinessLogPath
if (-not (Test-Path -LiteralPath $businessLogDirectory -PathType Container)) {
    New-Item -ItemType Directory -Force -Path $businessLogDirectory | Out-Null
}

function Get-CloudServiceScopeHash {
    param([string]$Tenant, [string]$User)
    $tenantBytes = [System.Text.Encoding]::UTF8.GetBytes($Tenant.Trim())
    $userBytes = [System.Text.Encoding]::UTF8.GetBytes($User.Trim())
    $frame = New-Object byte[] (8 + $tenantBytes.Length + $userBytes.Length)
    $offset = 0
    foreach ($bytes in @($tenantBytes, $userBytes)) {
        $length = $bytes.Length
        $frame[$offset++] = [byte]($length -shr 24)
        $frame[$offset++] = [byte]($length -shr 16)
        $frame[$offset++] = [byte]($length -shr 8)
        $frame[$offset++] = [byte]$length
        [Array]::Copy($bytes, 0, $frame, $offset, $length)
        $offset += $length
    }
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([System.BitConverter]::ToString($sha.ComputeHash($frame))).Replace("-", "").ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Get-FileSha256 {
    param([string]$LiteralPath)
    $stream = [System.IO.File]::OpenRead($LiteralPath)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([System.BitConverter]::ToString($sha.ComputeHash($stream))).Replace("-", "").ToLowerInvariant()
    } finally {
        $sha.Dispose()
        $stream.Dispose()
    }
}

$scopeHash = Get-CloudServiceScopeHash -Tenant $TenantId -User $UserId
$scopeRoot = Join-Path $StateRoot $scopeHash
$visionMemoryPath = Join-Path $scopeRoot "vision_memory.json"

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
        Get-FileSha256 -LiteralPath $classpathFile
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
# CR257 review P1-1: the NPC click memory canonical is the cloud-brain-owned file below (CR181/
# CR185 lineage; the DHXY-side config/vision_memory.json copy is deleted by C1). Pin it explicitly
# via sysprop so the store never depends on the java process working directory, and refuse to
# start with a silently missing canonical unless the operator explicitly allows an empty memory.
# This preflight runs BEFORE the OCR sidecar lifecycle (review P2): a missing-memory abort must
# not leave a freshly launched/registered sidecar behind.
# ---------------------------------------------------------------------------
if (Test-Path -LiteralPath $visionMemoryPath -PathType Leaf) {
    $visionMemoryItem = Get-Item -LiteralPath $visionMemoryPath
    $visionMemorySha256 = Get-FileSha256 -LiteralPath $visionMemoryPath
    Write-Host "CR257 vision memory canonical: path=$visionMemoryPath bytes=$($visionMemoryItem.Length) sha256=$visionMemorySha256 lastWriteUtc=$($visionMemoryItem.LastWriteTimeUtc.ToString('o'))"
} elseif ($AllowEmptyVisionMemory) {
    Write-Host "CR257 vision memory canonical: path=$visionMemoryPath MISSING — starting with empty memory (explicit -AllowEmptyVisionMemory)"
} else {
    throw "vision-memory-missing: canonical NPC click memory not found at $visionMemoryPath; provision it (copy from the live cloud-brain data dir) or pass -AllowEmptyVisionMemory to start with an empty store"
}

# ---------------------------------------------------------------------------
# CR257 A.1-A.4: cloud-brain owns its OCR sidecar lifecycle.
#   A.1 identity-bearing health gates every accept: protocolVersion + sidecarBuild (parsed from
#       the owned sidecar script, so accept binds to the exact artifact this launcher starts) +
#       loopback bindHost + exact bindPort.
#   A.2 registry-based stop: only a PID this launcher itself launched (mode=launched) AND whose
#       FULL identity (pid + startedAtMs + build + model fingerprint + port + command line) still
#       matches may be terminated; never kill by port. mode=reused can never enter the stop branch.
#   A.3 occupied-but-mismatched port fails the launch with structured ocr-sidecar-conflict.
#   A.4 orphan with matching identity is reused (recorded as mode=reused, no ownership); a sidecar
#       dying later is NOT relaunched here (OCR calls fail closed until the next launcher cycle).
# Logs carry runId/pid/version fingerprints and reasons only — never image data or OCR text.
# ---------------------------------------------------------------------------
$ocrExpectedProtocol = "dhxy-ocr-v2"
$ocrEndpoint = "http://127.0.0.1:$OcrPort"
$ocrServerScript = Join-Path $BrainProjectPath "ocr\local_ocr_server.py"
$ocrRegistryFile = Join-Path $BrainProjectPath "data\ocr-sidecar-registry.json"
$ocrLogDir = Join-Path $BrainProjectPath "logs"
$ocrRunId = [guid]::NewGuid().ToString()

if (-not (Test-Path -LiteralPath $ocrServerScript -PathType Leaf)) {
    throw "ocr-sidecar-missing: sidecar script not found at $ocrServerScript (CR257 A: OCR ships with dhxy-cloud-brain)"
}
# Review P1-2: the expected build comes from the sidecar script this launcher owns, so identity
# acceptance can never drift from the artifact we would actually start.
$ocrBuildMatch = Select-String -LiteralPath $ocrServerScript -Pattern 'SIDECAR_BUILD\s*=\s*"([^"]+)"' | Select-Object -First 1
if ($null -eq $ocrBuildMatch) {
    throw "ocr-sidecar-missing: SIDECAR_BUILD marker not found in $ocrServerScript"
}
$ocrExpectedBuild = $ocrBuildMatch.Matches[0].Groups[1].Value

# Review P1-2 (round 2): the expected model fingerprint is derived from the SAME python runtime
# this launcher would start the sidecar with — never taken from a running sidecar's self-report.
if (Get-Command "py" -ErrorAction SilentlyContinue) {
    $ocrPythonLauncher = "py"
    $ocrPythonBaseArgs = @("-3")
} elseif (Get-Command "python" -ErrorAction SilentlyContinue) {
    $ocrPythonLauncher = "python"
    $ocrPythonBaseArgs = @()
} else {
    throw "ocr-sidecar-launch-failed: neither 'py' nor 'python' is available for the OCR sidecar"
}
$ocrFingerprintCode = "import rapidocr; print('rapidocr-' + getattr(rapidocr, '__version__', 'unknown'))"
# Collect the full output BEFORE taking the first line: piping a native command straight into
# Select-Object -First 1 truncates the pipeline and leaves $LASTEXITCODE unset ($null), which
# falsely fails the -ne 0 check even when the import succeeded.
$ocrExpectedModelRaw = & $ocrPythonLauncher @($ocrPythonBaseArgs + @("-c", $ocrFingerprintCode)) 2>$null
$ocrDeriveExit = $LASTEXITCODE
$ocrExpectedModel = [string](@($ocrExpectedModelRaw) | Select-Object -First 1)
if ($ocrDeriveExit -ne 0 -or [string]::IsNullOrWhiteSpace($ocrExpectedModel)) {
    throw "ocr-model-fingerprint-unknown: cannot derive the expected model fingerprint (rapidocr not importable via $ocrPythonLauncher, exit=$ocrDeriveExit); install it: $ocrPythonLauncher $($ocrPythonBaseArgs -join ' ') -m pip install -r $BrainProjectPath\ocr\requirements.txt"
}
$ocrExpectedModel = $ocrExpectedModel.Trim()

# Resolve the real python.exe behind the launcher: starting the sidecar via py.exe makes
# Start-Process return the SHIM's pid while the sidecar reports the child python.exe pid via
# os.getpid(), which falsely trips the own-pid conflict check during the ready poll. Launching
# python.exe directly keeps $process.Id == health.pid.
$ocrPythonExeRaw = & $ocrPythonLauncher @($ocrPythonBaseArgs + @("-c", "import sys; print(sys.executable)")) 2>$null
$ocrPythonExeExit = $LASTEXITCODE
$ocrPythonExe = [string](@($ocrPythonExeRaw) | Select-Object -First 1)
if ($ocrPythonExeExit -ne 0 -or [string]::IsNullOrWhiteSpace($ocrPythonExe) -or -not (Test-Path -LiteralPath $ocrPythonExe.Trim() -PathType Leaf)) {
    throw "ocr-sidecar-launch-failed: cannot resolve the real python executable via $ocrPythonLauncher (exit=$ocrPythonExeExit, path='$ocrPythonExe')"
}
$ocrPythonExe = $ocrPythonExe.Trim()
Write-Host "CR257 OCR python runtime: exe=$ocrPythonExe"
Write-Host "CR257 OCR expected identity: protocol=$ocrExpectedProtocol build=$ocrExpectedBuild model=$ocrExpectedModel port=$OcrPort"

function Get-OcrHealth {
    try {
        return Invoke-RestMethod -Uri "$ocrEndpoint/health" -Method Get -TimeoutSec 3
    } catch {
        return $null
    }
}

function Test-OcrIdentityMatch {
    param($Health)
    # A.1 (review P1-2): full identity — protocol, exact expected build, expected model
    # fingerprint (derived locally, never trusted from the sidecar), loopback host, exact port.
    return ($null -ne $Health) -and ($Health.ok -eq $true) `
        -and ($Health.protocolVersion -eq $ocrExpectedProtocol) `
        -and ([string]$Health.sidecarBuild -eq $ocrExpectedBuild) `
        -and ([string]$Health.modelFingerprint -eq $ocrExpectedModel) `
        -and (@("127.0.0.1", "localhost", "::1") -contains [string]$Health.bindHost) `
        -and ([int]$Health.bindPort -eq $OcrPort)
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

function Write-OcrRegistry {
    param($Health, [string]$Mode, [string]$Command)
    $registryDir = Split-Path -Parent $ocrRegistryFile
    if (-not (Test-Path -LiteralPath $registryDir -PathType Container)) {
        New-Item -ItemType Directory -Force -Path $registryDir | Out-Null
    }
    [pscustomobject]@{
        runId = $ocrRunId
        pid = [long]$Health.pid
        startedAtMs = [long]$Health.startedAtMs
        protocolVersion = [string]$Health.protocolVersion
        sidecarBuild = [string]$Health.sidecarBuild
        modelFingerprint = [string]$Health.modelFingerprint
        bindHost = [string]$Health.bindHost
        bindPort = [int]$Health.bindPort
        registeredAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        command = $Command
        mode = $Mode
    } | ConvertTo-Json | Set-Content -LiteralPath $ocrRegistryFile -Encoding UTF8
}

function Test-OcrStopEligible {
    param($Registry, $Health)
    # A.2 (review P1-2): stop is allowed ONLY for a process this launcher itself launched, whose
    # full recorded identity still matches the live health, and whose OS command line still runs
    # the sidecar script. mode=reused (adopted orphan) is never stop-eligible.
    if ($null -eq $Registry -or $null -eq $Health) {
        return $false
    }
    if ([string]$Registry.mode -ne "launched") {
        return $false
    }
    if ([long]$Registry.pid -ne [long]$Health.pid) {
        return $false
    }
    if ([long]$Registry.startedAtMs -ne [long]$Health.startedAtMs) {
        return $false
    }
    if ([string]$Registry.sidecarBuild -ne [string]$Health.sidecarBuild) {
        return $false
    }
    if ([string]$Registry.modelFingerprint -ne [string]$Health.modelFingerprint) {
        return $false
    }
    if ([int]$Registry.bindPort -ne [int]$Health.bindPort) {
        return $false
    }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($Registry.pid)" -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return $false
    }
    return ([string]$process.CommandLine) -like "*local_ocr_server.py*"
}

function Start-OcrSidecar {
    if (-not (Test-Path -LiteralPath $ocrLogDir -PathType Container)) {
        New-Item -ItemType Directory -Force -Path $ocrLogDir | Out-Null
    }
    $ocrLog = Join-Path $ocrLogDir "ocr-sidecar.log"
    # Launch the resolved python.exe directly (never the py shim) so $process.Id matches the
    # sidecar's own os.getpid() in /health.
    $launcher = $ocrPythonExe
    $launcherArgs = @($ocrServerScript, "--host", "127.0.0.1", "--port", "$OcrPort")
    $command = "$launcher $($launcherArgs -join ' ')"
    $process = Start-Process -FilePath $launcher -ArgumentList $launcherArgs `
        -RedirectStandardOutput $ocrLog -RedirectStandardError "$ocrLog.err" `
        -WindowStyle Hidden -PassThru
    Write-Host "CR257 OCR sidecar launched: runId=$ocrRunId pid=$($process.Id) port=$OcrPort log=$ocrLog"
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 750
        $health = Get-OcrHealth
        if ((Test-OcrIdentityMatch -Health $health) -and ([long]$health.pid -eq [long]$process.Id)) {
            # Review P1-2: the registry records the FULL live identity (pid + startedAtMs + build
            # + model + bind) only after health confirms it is OUR process answering on the port.
            Write-OcrRegistry -Health $health -Mode "launched" -Command $command
            Write-Host "CR257 OCR sidecar ready: pid=$($health.pid) startedAtMs=$($health.startedAtMs) protocolVersion=$($health.protocolVersion) build=$($health.sidecarBuild) model=$($health.modelFingerprint)"
            return
        }
        if (($null -ne $health) -and ($health.ok -eq $true) -and ([long]$health.pid -ne [long]$process.Id)) {
            # Someone else answers the port while our process is alive — conflict; stop only our
            # own just-started PID and fail closed.
            try { Stop-Process -Id $process.Id -Force -Confirm:$false -ErrorAction SilentlyContinue } catch {}
            throw "ocr-sidecar-conflict: port $OcrPort is answered by pid=$($health.pid) while this launcher's own sidecar pid=$($process.Id) is starting; refusing to start"
        }
        if ($process.HasExited) {
            throw "ocr-sidecar-launch-failed: sidecar process exited early (exitCode=$($process.ExitCode)); see $ocrLog"
        }
    }
    try { Stop-Process -Id $process.Id -Force -Confirm:$false -ErrorAction SilentlyContinue } catch {}
    throw "ocr-sidecar-launch-failed: health did not become ready within 90s; own pid=$($process.Id) stopped"
}

$ocrHealth = Get-OcrHealth
if ($null -ne $ocrHealth) {
    if (-not (Test-OcrIdentityMatch -Health $ocrHealth)) {
        # A.3: occupied but identity mismatch (old script, wrong build, wrong port claim, unknown
        # service) — fail closed; never take over, overwrite, silently reuse, or stop it.
        throw "ocr-sidecar-conflict: port $OcrPort is serving a non-matching health (protocolVersion='$($ocrHealth.protocolVersion)' build='$($ocrHealth.sidecarBuild)' model='$($ocrHealth.modelFingerprint)' bindHost='$($ocrHealth.bindHost)' bindPort='$($ocrHealth.bindPort)' expectedProtocol='$ocrExpectedProtocol' expectedBuild='$ocrExpectedBuild' expectedModel='$ocrExpectedModel' expectedPort='$OcrPort'); refusing to start"
    }
    $ocrRegistry = Get-OcrRegistry
    if (Test-OcrStopEligible -Registry $ocrRegistry -Health $ocrHealth) {
        # A.2/A.4 normal restart: this is OUR previously launched instance (mode=launched, full
        # identity verified) — stop it (never by port) and launch a fresh one.
        Write-Host "CR257 OCR sidecar restart: stopping own registered instance pid=$($ocrRegistry.pid) startedAtMs=$($ocrRegistry.startedAtMs) runId=$($ocrRegistry.runId)"
        Stop-Process -Id $ocrRegistry.pid -Force -Confirm:$false
        Start-Sleep -Milliseconds 1500
        Start-OcrSidecar
    } else {
        # A.4 orphan with matching identity: reuse WITHOUT adopting ownership. mode=reused is
        # never stop-eligible on later cycles (review P1-2), so this process can only ever be
        # reused again or conflict-fail — never killed by this launcher.
        Write-Host "CR257 OCR sidecar reuse: identity-matched orphan pid=$($ocrHealth.pid) startedAtMs=$($ocrHealth.startedAtMs) build=$($ocrHealth.sidecarBuild) model=$($ocrHealth.modelFingerprint) (not launched by this launcher; reuse only, never stopped)"
        Write-OcrRegistry -Health $ocrHealth -Mode "reused" -Command "(reused orphan; not launched by this runId)"
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
Write-Host "Cloud business log: $BusinessLogPath"
& java `
    "-Dorg.slf4j.simpleLogger.logFile=$BusinessLogPath" `
    "-Ddhxy.cloud.brain.localOcrEndpoint=$ocrEndpoint" `
    "-Ddhxy.cloud.brain.localOcrTimeoutMs=10000" `
    "-Ddhxy.cloud.brain.localOcrExpectedModelFingerprint=$ocrExpectedModel" `
    "-Ddhxy.cloud.brain.visionMemoryPath=$visionMemoryPath" `
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
    "--token=$Token" `
    "--tenant=$($TenantId.Trim())" `
    "--user=$($UserId.Trim())" `
    "--state-root=$StateRoot"
