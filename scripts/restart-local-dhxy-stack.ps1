param(
    [switch]$SkipClientCompile,
    # 应急逃生口：体检照跑照报，只是不阻断启动。刻意不提供"整段跳过"——
    # 绕过安全门时更需要看到缺了什么，静默跳过等于把换机故障推迟到停机之后才暴露。
    [switch]$IgnorePreflightFailures
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "lib-process-tree.ps1")
. (Join-Path $PSScriptRoot "lib-machine-paths.ps1")

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
# G106 证据面（2026-08-29）：体检此前只写交互控制台，日志里零样本，"停进程前一次报全 +
# 打印解析值"无法验收。这里给它一个落盘出口——行内时间戳与下面的"停止旧进程"标记同文件，
# 顺序本身就是证据。只落盘，不参与任何判定。
$preflightLog = Join-Path $projectRoot "logs\startup-preflight.log"
$cloudLauncher = Join-Path $PSScriptRoot "run-cloud-brain-server.ps1"
$clientClasspathFile = Join-Path $projectRoot "target\client-dependency-classpath.txt"
# 换机契约（G106）：云端仓与 Java 运行时都不再写死在脚本里，统一走 lib-machine-paths.ps1
# 的"环境变量 → 本机配置 → 推导默认值"三级解析。
$cloudProjectRootResolution = Resolve-DhxyCloudProjectRoot -ClientRoot $projectRoot
$cloudProjectRoot = $cloudProjectRootResolution.Path
$javaRuntime = Resolve-DhxyJavaRuntime -ClientRoot $projectRoot
$javaExe = $javaRuntime.JavaExe
$javawExe = $javaRuntime.JavawExe
# Maven 按 JAVA_HOME 挑 JDK，与上面的解析链是两个口径：不钉住就会出现"体检全绿、
# 停掉旧进程之后才在 mvn compile 阶段炸"。preflight 随后会从 mvn -v 取证确认钉住了。
$previousJavaHomeForMaven = Use-DhxyJavaHomeForMaven -JavaHome $javaRuntime.JavaHome
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
        # 提权分支也要过同一道体检：它同样会起客户端 JVM，早退会让换机缺件绕过安全门。
        Invoke-DhxyStartupPreflight -ClientRoot $projectRoot `
            -CloudRoot $cloudProjectRoot -CloudRootSource $cloudProjectRootResolution.Source `
            -JavaExe $javaExe -JavaSource $javaRuntime.Source `
            -ReportOnly:$IgnorePreflightFailures -LogPath $preflightLog
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
    # 2026-08-31 事故:90 秒按"无改动秒起"标定;大合并后云端启动器要先重编译,端口开出来
    # 晚于上界,脚本超时自杀而分离的云端子进程继续起完->云端在跑、客户端永远没被启动。
    param([int]$Port, [int]$TimeoutSeconds = 420)
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
                [void](Stop-ProcessTreeSafely -ProcessId $registeredProcess.Id)
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
        [void](Stop-ProcessTreeSafely -ProcessId $client.Id)
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
        [void](Stop-ProcessTreeSafely -ProcessId $process.Id)
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
        # 树式收尾：RapidOCR 的 multiprocessing worker 是它的子进程，单杀父进程会把它们永久孤儿化。
        [void](Stop-ProcessTreeSafely -ProcessId $process.Id)
    }
}

New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot "logs") | Out-Null

# 体检必须跑在停止旧进程之前：换机缺件要在这里一次报全，而不是把当前运行停掉之后再逐条失败。
Invoke-DhxyStartupPreflight -ClientRoot $projectRoot `
    -CloudRoot $cloudProjectRoot -CloudRootSource $cloudProjectRootResolution.Source `
    -JavaExe $javaExe -JavaSource $javaRuntime.Source `
    -ReportOnly:$IgnorePreflightFailures -LogPath $preflightLog

# 同一文件里的这一行是"体检早于任何停进程动作"的文件证据：它的时间戳必须晚于上面每一行
# 体检记录，且早于下面任何 Stop-*。
Add-DhxyPreflightLogLine -LogPath $preflightLog -Line "[DHXY restart] 体检完成，开始停止旧进程（此行之后才有任何 Stop-Process）。"
Write-Stage "停止旧客户端与 Cloud Brain（不会关闭 IntelliJ、Node/8080 或其他 Java）。"
Stop-DhxyClientWindow
Stop-CloudBrainListener
Stop-DhxyOcrSidecar
# 树式收尾只覆盖“由本脚本关闭”的路径。sidecar 自己崩溃、被任务管理器杀掉、或机器休眠导致
# 父进程消失时同样会留下孤儿，所以每轮启动前再扫一次，避免像 2026-08-21 那样攒到 115 个。
[void](Remove-OrphanedOcrWorkers)
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
    # 显式传本轮已解析的绝对路径：子启动器不再自己重解析一遍，两边不可能得出不同答案。
    "-BrainProjectPath $(Quote-PowerShellLiteral $cloudProjectRoot) " +
    # 同一个 java.exe：子脚本不再自己解析，preflight 验过的那个就是云端真正执行的那个。
    "-JavaExe $(Quote-PowerShellLiteral $javaExe) " +
    # 同一个模板根：preflight 刚做过存在性与可写探针的那一个，就是云端 JVM 真正读写的那一个。
    "-TemplateRoot $(Quote-PowerShellLiteral (Join-Path $projectRoot 'images\template')) " +
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
# 整窗 BufferedImage 约 3MB；G1 默认 region 4MB 会把它们全判为 humongous 直进老年代，
# 引发并发 GC 连轴转（实测 2 次 YGC/秒、1830 个并发周期/20 分钟）。region 提到 16MB 让
# 帧对象走正常年轻代，-Xms 预热避免堆反复伸缩。
$clientJvmOptions = "-Xms2g -XX:G1HeapRegionSize=16m"
$clientArguments = @("-Xms2g", "-XX:G1HeapRegionSize=16m", "-cp", $clientClasspath, "com.bot.dhxy.AutoBot")
$clientArgumentLine = "$clientJvmOptions -cp $(Quote-ProcessArgument $clientClasspath) com.bot.dhxy.AutoBot"

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
