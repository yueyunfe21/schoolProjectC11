param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("start-wuhuan", "start-tianting", "start-catch-ghost", "start-ghost-king", "status", "pause", "resume", "stop", "shutdown", "monitor")]
    [string]$Action,

    [ValidateRange(1, 2147483647)]
    [int]$MaxRuns = 100,

    [switch]$SkipCompile,

    [switch]$ElevatedHost
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "lib-machine-paths.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$controlDirectory = Join-Path $projectRoot "logs\background-task-test"
$hostFile = Join-Path $controlDirectory "host.properties"
$classpathFile = Join-Path $projectRoot "target\client-dependency-classpath.txt"
$businessLog = Join-Path $projectRoot "logs\dhxy-console.log"
# 换机契约（G106）：这里曾是第五处写死的 JDK 补丁版本，与主启动链走同一条解析链。
$javaRuntime = Resolve-DhxyJavaRuntime -ClientRoot $projectRoot
$javaExe = $javaRuntime.JavaExe
# 本脚本自己也会 mvn compile：Maven 必须与运行 JVM 同一个 JDK。
[void](Use-DhxyJavaHomeForMaven -JavaHome $javaRuntime.JavaHome)

function Read-PropertiesFile {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $values
    }
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            continue
        }
        $parts = $line.Split(@("="), 2, [System.StringSplitOptions]::None)
        if ($parts.Count -eq 2) {
            $values[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $values
}

function Get-LiveHost {
    $hostState = Read-PropertiesFile -Path $hostFile
    if (-not $hostState.ContainsKey("pid") -or -not $hostState.ContainsKey("sessionId")) {
        return $null
    }
    $hostProcess = Get-Process -Id ([int]$hostState["pid"]) -ErrorAction SilentlyContinue
    if ($null -eq $hostProcess -or $hostProcess.ProcessName -notin @("java", "javaw")) {
        return $null
    }
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$($hostProcess.Id)" -ErrorAction SilentlyContinue
    if ($null -ne $processInfo -and -not [string]::IsNullOrWhiteSpace($processInfo.CommandLine)) {
        if ($processInfo.CommandLine -notmatch "com\.bot\.dhxy\.AutoBot" `
                -or $processInfo.CommandLine -notmatch "bot\.background-test\.enabled=true") {
            return $null
        }
    } else {
        # A Medium controller cannot read the command line of the High-integrity host it just launched.
        # Bind that exact process to the fresh host file instead of treating the access boundary as no host.
        $hostFileInfo = Get-Item -LiteralPath $hostFile -ErrorAction SilentlyContinue
        if (-not $hostState.ContainsKey("startedAt") -or $null -eq $hostFileInfo `
                -or $hostFileInfo.LastWriteTimeUtc -lt $hostProcess.StartTime.ToUniversalTime().AddSeconds(-2)) {
            return $null
        }
    }
    return [pscustomobject]@{
        Process = $hostProcess
        SessionId = $hostState["sessionId"]
    }
}

function Start-ControlHost {
    $existing = Get-LiveHost
    if ($null -ne $existing) {
        return $existing
    }

    $otherClients = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -in @("java.exe", "javaw.exe") -and
            $_.CommandLine -match "com\.bot\.dhxy\.AutoBot"
        })
    if ($otherClients.Count -gt 0) {
        $descriptions = $otherClients | ForEach-Object { "pid=$($_.ProcessId) command=$($_.CommandLine)" }
        throw "Existing DHXY Client detected; refusing to start a second window owner: $($descriptions -join '; ')"
    }

    # PATH 兜底已并入 Resolve-DhxyJavaRuntime 的逐级下探；这里只做最终存在性断言。
    if ([string]::IsNullOrWhiteSpace($javaExe) -or -not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
        throw "没有解析到可用的 java.exe [$($javaRuntime.Source)]"
    }
    New-Item -ItemType Directory -Force -Path $controlDirectory | Out-Null

    if (-not $SkipCompile) {
        Write-Host "[G033] Compiling Client and refreshing the runtime classpath."
        Push-Location $projectRoot
        try {
            & mvn -q compile dependency:build-classpath "-Dmdep.includeScope=runtime" `
                "-Dmdep.outputFile=$classpathFile" "-Dmdep.pathSeparator=;"
            if ($LASTEXITCODE -ne 0) {
                throw "Client compilation failed, exit=$LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
    }
    if (-not (Test-Path -LiteralPath $classpathFile -PathType Leaf)) {
        throw "Runtime classpath does not exist: $classpathFile"
    }

    $dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
    $classpath = (Join-Path $projectRoot "target\classes") + ";" + $dependencies
    $argumentLine = '-cp "' + $classpath.Replace('"', '\"') + '" com.bot.dhxy.AutoBot ' +
        '--bot.run.show-ui=false --bot.run.auto-start=false ' +
        '--bot.background-test.enabled=true --bot.background-test.default-max-runs=100'

    $startParameters = @{
        FilePath = $javaExe
        ArgumentList = $argumentLine
        WorkingDirectory = $projectRoot
        WindowStyle = "Hidden"
        PassThru = $true
    }
    if ($ElevatedHost) {
        Write-Host "[G036] Requesting a High-integrity no-UI host. Approve the Windows UAC prompt."
        $startParameters["Verb"] = "RunAs"
    } else {
        Write-Host "[G033] Starting the no-UI control host."
    }
    $process = Start-Process @startParameters

    $deadline = (Get-Date).AddSeconds(45)
    while ((Get-Date) -lt $deadline) {
        if ($process.HasExited) {
            throw "Background control host exited early, exit=$($process.ExitCode). See $businessLog"
        }
        $controlHost = Get-LiveHost
        if ($null -ne $controlHost -and $controlHost.Process.Id -eq $process.Id) {
            Write-Host "[G033] Control host ready: pid=$($process.Id) session=$($controlHost.SessionId)"
            return $controlHost
        }
        Start-Sleep -Milliseconds 250
    }
    throw "Background control host was not ready within 45 seconds. See $businessLog"
}

function Send-ControlCommand {
    param(
        [Parameter(Mandatory = $true)]$HostState,
        [Parameter(Mandatory = $true)][string]$CommandAction,
        [int]$RequestedMaxRuns = 100,
        [int]$TimeoutSeconds = 180
    )

    $requestId = [Guid]::NewGuid().ToString("N")
    $temporary = Join-Path $controlDirectory ($requestId + ".tmp")
    $request = Join-Path $controlDirectory ($requestId + ".command")
    $result = Join-Path $controlDirectory ($requestId + ".result")
    $requestLines = @(
        "sessionId=$($HostState.SessionId)",
        "action=$CommandAction",
        "maxRuns=$RequestedMaxRuns"
    )
    [IO.File]::WriteAllLines($temporary, $requestLines, [Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temporary -Destination $request

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Path -LiteralPath $result -PathType Leaf) {
            $response = Read-PropertiesFile -Path $result
            Get-Content -LiteralPath $result
            if ($response["success"] -ne "true") {
                throw "Background control command failed: $($response['message'])"
            }
            return $response
        }
        if ($HostState.Process.HasExited) {
            throw "Background control host exited before command completion, exit=$($HostState.Process.ExitCode)"
        }
        Start-Sleep -Milliseconds 200
    }
    throw "Background control command timed out: action=$CommandAction timeout=${TimeoutSeconds}s"
}

if ($Action -eq "monitor") {
    $controlHost = Get-LiveHost
    if ($null -eq $controlHost) {
        throw "No running G033 control host. Start a Wuhuan or Tianting test first."
    }
    if (-not (Test-Path -LiteralPath $businessLog -PathType Leaf)) {
        throw "Business log does not exist: $businessLog"
    }
    Write-Host "[G033] Monitoring new log lines only; an ERROR invokes the formal stopAll() path."
    Get-Content -LiteralPath $businessLog -Tail 0 -Wait | ForEach-Object {
        Write-Host $_
        if ($_ -match "\sERROR\s") {
            Write-Host "[G033] ERROR detected; stopping all window tasks."
            Send-ControlCommand -HostState $controlHost -CommandAction "stop" -TimeoutSeconds 60 | Out-Null
            break
        }
    }
    exit 0
}

$controlHost = Get-LiveHost
if ($Action -in @("start-wuhuan", "start-tianting", "start-catch-ghost", "start-ghost-king", "status") -and $null -eq $controlHost) {
    $controlHost = Start-ControlHost
}
if ($null -eq $controlHost) {
    throw "No running G033 control host."
}

$response = Send-ControlCommand -HostState $controlHost -CommandAction $Action -RequestedMaxRuns $MaxRuns
$response | Out-String | Write-Host
if ($Action -eq "shutdown") {
    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline -and -not $controlHost.Process.HasExited) {
        Start-Sleep -Milliseconds 250
    }
    if (-not $controlHost.Process.HasExited) {
        throw "Tasks stopped, but the background host did not exit within 30 seconds; refusing to force-kill it."
    }
    Write-Host "[G033] Background host exited safely."
}
