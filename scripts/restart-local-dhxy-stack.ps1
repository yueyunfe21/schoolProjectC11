param(
    [switch]$SkipClientCompile
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$cloudPort = 18080
$ocrPort = 18762
$clientWindowTitle = "DHXY Robot 控制台"
$clientLog = Join-Path $projectRoot "logs\local-stack-client.out.log"
$clientErrorLog = Join-Path $projectRoot "logs\local-stack-client.err.log"
$cloudLog = Join-Path $projectRoot "logs\local-stack-cloud.out.log"
$cloudErrorLog = Join-Path $projectRoot "logs\local-stack-cloud.err.log"
$cloudBusinessLog = Join-Path $projectRoot "logs\cloud-brain-console.log"
$processRegistryFile = Join-Path $projectRoot "logs\local-stack-processes.json"
$cloudLauncher = Join-Path $PSScriptRoot "run-cloud-brain-server.ps1"
$cloudProjectRoot = "D:\mavenProject\dhxy-cloud-brain"
$clientClasspathFile = Join-Path $projectRoot "target\client-dependency-classpath.txt"
$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$javaExe = Join-Path $javaHome "bin\java.exe"
$javawExe = Join-Path $javaHome "bin\javaw.exe"
$backgroundTestDirectory = Join-Path $projectRoot "logs\background-task-test"
$backgroundTestRequestFile = Join-Path $backgroundTestDirectory "elevated-request.json"
$backgroundTestResultFile = Join-Path $backgroundTestDirectory "elevated-request-result.json"

# The existing highest-privilege scheduled task enters through this script. A one-shot request lets
# the background test host inherit that privilege without changing normal desktop restart behavior.
if (Test-Path -LiteralPath $backgroundTestRequestFile -PathType Leaf) {
    try {
        $backgroundRequest = Get-Content -LiteralPath $backgroundTestRequestFile -Raw | ConvertFrom-Json
        Remove-Item -LiteralPath $backgroundTestRequestFile -Force
        $backgroundAction = [string]$backgroundRequest.action
        if ($backgroundAction -notin @("start-wuhuan", "start-tianting", "start-catch-ghost", "start-ghost-king")) {
            throw "Unsupported elevated background action: $backgroundAction"
        }
        $backgroundMaxRuns = [Math]::Max(1, [int]$backgroundRequest.maxRuns)
        $backgroundScript = Join-Path $PSScriptRoot "background-task-test.ps1"
        $global:LASTEXITCODE = 0
        & $backgroundScript $backgroundAction -MaxRuns $backgroundMaxRuns -SkipCompile
        if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) {
            throw "Elevated background task launcher failed, exit=$LASTEXITCODE"
        }
        [pscustomobject]@{
            success = $true
            action = $backgroundAction
            maxRuns = $backgroundMaxRuns
            completedAt = (Get-Date).ToUniversalTime().ToString("o")
        } | ConvertTo-Json | Set-Content -LiteralPath $backgroundTestResultFile -Encoding UTF8
        exit 0
    } catch {
        [pscustomobject]@{
            success = $false
            error = $_.Exception.Message
            completedAt = (Get-Date).ToUniversalTime().ToString("o")
        } | ConvertTo-Json | Set-Content -LiteralPath $backgroundTestResultFile -Encoding UTF8
        throw
    }
}

function Write-Stage {
    param([string]$Text)
    Write-Host "[DHXY restart] $Text"
}

function Quote-ProcessArgument {
    param([string]$Value)
    return '"' + $Value.Replace('"', '\"') + '"'
}

function Quote-PowerShellLiteral {
    param([string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

function Wait-PortReleased {
    param([int]$Port, [int]$TimeoutSeconds = 20)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($null -eq $listener) {
            return
        }
        Start-Sleep -Milliseconds 250
    }
    throw "端口 $Port 在 $TimeoutSeconds 秒内未释放。"
}

function Wait-PortListening {
    param([int]$Port, [int]$TimeoutSeconds = 90)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($null -ne $listener) {
            return
        }
        Start-Sleep -Milliseconds 500
    }
    throw "端口 $Port 在 $TimeoutSeconds 秒内没有开始监听。请查看 $cloudLog 和 $cloudErrorLog。"
}

function Stop-DhxyClientWindow {
    $stopped = [System.Collections.Generic.HashSet[int]]::new()
    if (Test-Path -LiteralPath $processRegistryFile -PathType Leaf) {
        try {
            $registered = Get-Content -LiteralPath $processRegistryFile -Raw | ConvertFrom-Json
            $registeredProcess = Get-Process -Id ([int]$registered.clientPid) -ErrorAction SilentlyContinue
            if ($null -ne $registeredProcess -and $registeredProcess.ProcessName -in @("java", "javaw")) {
                Write-Stage "关闭上次由重启入口启动的客户端 pid=$($registeredProcess.Id)"
                Stop-Process -Id $registeredProcess.Id -Force -Confirm:$false
                [void]$stopped.Add([int]$registeredProcess.Id)
            }
        } catch {
            Write-Stage "忽略不可读取的旧客户端 PID 记录：$processRegistryFile"
        }
    }
    $clients = Get-Process java, javaw -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -eq $clientWindowTitle }
    foreach ($client in $clients) {
        if ($stopped.Contains([int]$client.Id)) {
            continue
        }
        Write-Stage "关闭 DHXY 客户端 pid=$($client.Id)"
        Stop-Process -Id $client.Id -Force -Confirm:$false
    }
}

function Stop-CloudBrainListener {
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $cloudPort -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction Stop
        if ($process.ProcessName -notin @("java", "javaw")) {
            throw "拒绝关闭端口 $cloudPort：pid=$($process.Id) 不是 Java 云端进程，而是 $($process.ProcessName)。"
        }
        Write-Stage "关闭 Cloud Brain pid=$($process.Id) port=$cloudPort"
        Stop-Process -Id $process.Id -Force -Confirm:$false
    }
}

function Stop-DhxyOcrSidecar {
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $ocrPort -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction Stop
        if ($process.ProcessName -notin @("python", "pythonw")) {
            throw "拒绝关闭端口 $ocrPort：pid=$($process.Id) 不是 Python OCR sidecar，而是 $($process.ProcessName)。"
        }
        Write-Stage "关闭旧 OCR sidecar pid=$($process.Id) port=$ocrPort"
        Stop-Process -Id $process.Id -Force -Confirm:$false
    }
}

if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    $javaCommand = Get-Command java.exe -ErrorAction Stop
    $javaExe = $javaCommand.Source
}

New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot "logs") | Out-Null

Write-Stage "停止旧客户端与 Cloud Brain（不会关闭 IntelliJ、Node/8080 或其他 Java）。"
Stop-DhxyClientWindow
Stop-CloudBrainListener
Stop-DhxyOcrSidecar
Wait-PortReleased -Port $cloudPort
Wait-PortReleased -Port $ocrPort

if (-not $SkipClientCompile) {
    Write-Stage "编译最新 DHXY 客户端类并刷新运行时 classpath。"
    Push-Location $projectRoot
    try {
        & mvn -q compile dependency:build-classpath "-Dmdep.includeScope=runtime" `
            "-Dmdep.outputFile=$clientClasspathFile" "-Dmdep.pathSeparator=;"
        if ($LASTEXITCODE -ne 0) {
            throw "DHXY 客户端编译失败，exit=$LASTEXITCODE。"
        }
    } finally {
        Pop-Location
    }
}

Write-Stage "编译最新 Cloud Brain 类并刷新运行时 classpath。"
Push-Location $cloudProjectRoot
try {
    & mvn -q compile dependency:build-classpath "-Dmdep.includeScope=runtime" `
        "-Dmdep.outputFile=target\dependency-classpath.txt" "-Dmdep.pathSeparator=;"
    if ($LASTEXITCODE -ne 0) {
        throw "Cloud Brain 编译失败，exit=$LASTEXITCODE。"
    }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $clientClasspathFile -PathType Leaf)) {
    throw "客户端依赖 classpath 不存在：$clientClasspathFile"
}

$sidOutput = (whoami.exe /user /fo csv /nh | Out-String)
$sidMatch = [regex]::Match($sidOutput, 'S-1-(?:\d+-)+\d+')
$sid = if ($sidMatch.Success) { $sidMatch.Value } else { "" }
if ([string]::IsNullOrWhiteSpace($sid)) {
    throw "无法读取当前 Windows SID，拒绝启动没有 scope 的 Cloud Brain。"
}
$stateRoot = Join-Path $env:LOCALAPPDATA "DHXY\cloud-brain\state"
$cloudWrapperCommand = "& $(Quote-PowerShellLiteral $cloudLauncher) " +
    "-Port $cloudPort " +
    "-BusinessLogPath $(Quote-PowerShellLiteral $cloudBusinessLog) " +
    "-TenantId 'dhxy-local' " +
    "-UserId $(Quote-PowerShellLiteral $sid.Trim()) " +
    "-StateRoot $(Quote-PowerShellLiteral $stateRoot) " +
    "-OcrPort $ocrPort " +
    "3>&1 4>&1 5>&1 6>&1 1>$(Quote-PowerShellLiteral $cloudLog) 2>$(Quote-PowerShellLiteral $cloudErrorLog)"
$cloudEncodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($cloudWrapperCommand))
$cloudArgumentLine = "-NoProfile -ExecutionPolicy Bypass -EncodedCommand $cloudEncodedCommand"

# 先归档上一轮日志再启动。Cloud 正常业务日志固定写入 cloud-brain-console.log；
# local-stack-cloud.out/err.log 只保留启动器输出和日志初始化前的 JVM 故障。所有文件都必须先归档，
# 避免 Start-Process 重定向截断或新 JVM 覆盖上一轮现场。
$stackLogArchiveDir = Join-Path $projectRoot "logs\archive"
if (-not (Test-Path -LiteralPath $stackLogArchiveDir)) {
    New-Item -ItemType Directory -Path $stackLogArchiveDir | Out-Null
}
$stackLogStamp = (Get-Date).ToString("yyyyMMdd-HHmmss")
foreach ($stackLog in @($cloudLog, $cloudErrorLog, $cloudBusinessLog, $clientLog, $clientErrorLog)) {
    if ((Test-Path -LiteralPath $stackLog) -and ((Get-Item -LiteralPath $stackLog).Length -gt 0)) {
        $stackLogArchiveName = "{0}.{1}{2}" -f `
            [IO.Path]::GetFileNameWithoutExtension($stackLog), $stackLogStamp, [IO.Path]::GetExtension($stackLog)
        Move-Item -LiteralPath $stackLog -Destination (Join-Path $stackLogArchiveDir $stackLogArchiveName) -Force
    }
}
Get-ChildItem -LiteralPath $stackLogArchiveDir -Filter "local-stack-*" |
    Sort-Object LastWriteTime -Descending | Select-Object -Skip 30 | Remove-Item -Force
Get-ChildItem -LiteralPath $stackLogArchiveDir -Filter "cloud-brain-console.*.log" |
    Sort-Object LastWriteTime -Descending | Select-Object -Skip 30 | Remove-Item -Force

Write-Stage "启动 Cloud Brain。"
# PowerShell 7 会在 Start-Process 自身使用 RedirectStandardOutput/Error 时等待长驻子进程退出。
# 重定向必须由分离的子 PowerShell 自己完成，否则此处永远到不了 JavaFX 客户端启动。
$cloudProcess = Start-Process -FilePath "powershell.exe" -ArgumentList $cloudArgumentLine `
    -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru
Wait-PortListening -Port $cloudPort
Write-Stage "Cloud Brain 已就绪：port=$cloudPort launcherPid=$($cloudProcess.Id)"
Write-Stage "Cloud 业务日志：$cloudBusinessLog"

$dependencyClasspath = (Get-Content -LiteralPath $clientClasspathFile -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($dependencyClasspath)) {
    throw "客户端依赖 classpath 为空：$clientClasspathFile"
}
$clientClasspath = (Join-Path $projectRoot "target\classes") + ";" + $dependencyClasspath
$clientArguments = @("-cp", $clientClasspath, "com.bot.dhxy.AutoBot")
$clientArgumentLine = "-cp $(Quote-ProcessArgument $clientClasspath) com.bot.dhxy.AutoBot"

Write-Stage "启动 DHXY JavaFX 客户端。"
$clientLauncher = if (Test-Path -LiteralPath $javawExe -PathType Leaf) { $javawExe } else { $javaExe }
$clientProcess = Start-Process -FilePath $clientLauncher -ArgumentList $clientArgumentLine `
    -WorkingDirectory $projectRoot -PassThru `
    -RedirectStandardOutput $clientLog -RedirectStandardError $clientErrorLog
$registry = [pscustomobject]@{
    clientPid = $clientProcess.Id
    clientStartedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    cloudPort = $cloudPort
    ocrPort = $ocrPort
} | ConvertTo-Json
Set-Content -LiteralPath $processRegistryFile -Value $registry -Encoding UTF8
Write-Stage "已发起客户端启动：pid=$($clientProcess.Id)。日志：$clientLog"
