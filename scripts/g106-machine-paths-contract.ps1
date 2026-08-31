# G106 换机契约的脚本侧合同。Java 侧的 CloudTurnSidecarLauncherTest 只覆盖 Java 推导分支，
# 这里覆盖脚本链：四级优先级、逐级下探、体检聚合与 ReportOnly、input backend 的 Spring 生效顺序。
#
# 运行：pwsh -NoProfile -File scripts\g106-machine-paths-contract.ps1
# 全程只读真实仓库，可写操作一律落在临时目录，不启动任何进程。

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib-machine-paths.ps1")

$script:Passed = 0
$script:Failed = 0

function Assert-Equal {
    param([string]$Name, $Expected, $Actual)
    if ($Expected -eq $Actual) {
        $script:Passed++
        Write-Host "  PASS  $Name"
    } else {
        $script:Failed++
        Write-Host "  FAIL  $Name"
        Write-Host "        expected: $Expected"
        Write-Host "        actual  : $Actual"
    }
}

function Assert-Match {
    param([string]$Name, [string]$Pattern, [string]$Actual)
    if ($Actual -match $Pattern) {
        $script:Passed++
        Write-Host "  PASS  $Name"
    } else {
        $script:Failed++
        Write-Host "  FAIL  $Name"
        Write-Host "        pattern: $Pattern"
        Write-Host "        actual : $Actual"
    }
}

# 落盘断言专用：文件不在就返回空串，让每条断言各自报 FAIL，而不是让合同崩在第一处。
function Read-TextOrEmpty {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return ""
    }
    $raw = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    if ($null -eq $raw) { return "" }
    return [string]$raw
}

# 每个用例一个干净的假客户端根，互不串味（配置读取带进程内缓存）。
$caseIndex = 0
function New-CaseRoot {
    param([hashtable]$Settings)
    $script:caseIndex++
    $root = Join-Path ([IO.Path]::GetTempPath()) "g106-contract-$PID-$script:caseIndex"
    New-Item -ItemType Directory -Force -Path (Join-Path $root "config") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $root "images\template") | Out-Null
    Set-Content -LiteralPath (Join-Path $root "pom.xml") -Value "<project/>" -Encoding UTF8
    if ($null -ne $Settings -and $Settings.Count -gt 0) {
        $lines = foreach ($key in $Settings.Keys) { "$key=$($Settings[$key])" }
        Set-Content -LiteralPath (Join-Path $root "config\application.properties") -Value $lines -Encoding UTF8
    }
    return $root
}

$createdRoots = [System.Collections.Generic.List[string]]::new()
$savedCloudEnvironment = $env:DHXY_CLOUD_BRAIN_ROOT
$savedJavaEnvironment = $env:DHXY_JAVA_HOME
$savedBackendEnvironment = $env:BOT_INPUT_BACKEND

try {
    Write-Host "[G106 contract] Cloud 路径优先级"

    $root = New-CaseRoot @{}; $createdRoots.Add($root)
    $env:DHXY_CLOUD_BRAIN_ROOT = $null
    $derived = Resolve-DhxyCloudProjectRoot -ClientRoot $root
    Assert-Equal "未配置时按同级目录推导" `
        ([IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $root) "dhxy-cloud-brain"))) $derived.Path
    Assert-Match "推导来源可辨识" "推导" $derived.Source

    $root = New-CaseRoot @{ "cloud.turn.sidecar.brain-project-path" = "E:/from-config/dhxy-cloud-brain" }
    $createdRoots.Add($root)
    Assert-Equal "本机配置压过推导" ([IO.Path]::GetFullPath("E:/from-config/dhxy-cloud-brain")) `
        (Resolve-DhxyCloudProjectRoot -ClientRoot $root).Path

    $env:DHXY_CLOUD_BRAIN_ROOT = "E:/from-env/dhxy-cloud-brain"
    Assert-Equal "环境变量压过本机配置" ([IO.Path]::GetFullPath("E:/from-env/dhxy-cloud-brain")) `
        (Resolve-DhxyCloudProjectRoot -ClientRoot $root).Path
    Assert-Equal "显式参数压过环境变量" ([IO.Path]::GetFullPath("E:/explicit/dhxy-cloud-brain")) `
        (Resolve-DhxyCloudProjectRoot -ClientRoot $root -Explicit "E:/explicit/dhxy-cloud-brain").Path
    $env:DHXY_CLOUD_BRAIN_ROOT = $null

    Write-Host "[G106 contract] Java 运行时解析"

    $root = New-CaseRoot @{ "dhxy.machine.java-home" = "E:/no-such-jdk" }; $createdRoots.Add($root)
    $env:DHXY_JAVA_HOME = $null
    $java = Resolve-DhxyJavaRuntime -ClientRoot $root
    Assert-Match "配置项指向不存在的 JDK 时继续下探" "已跳过无效的" $java.Source
    Assert-Equal "下探结果必须真实存在" $true (Test-Path -LiteralPath $java.JavaExe -PathType Leaf)

    $root = New-CaseRoot @{}; $createdRoots.Add($root)
    $java = Resolve-DhxyJavaRuntime -ClientRoot $root
    Assert-Equal "无任何覆盖时解析到可用 java" $true (Test-Path -LiteralPath $java.JavaExe -PathType Leaf)
    Assert-Equal "javaw 与 java 同目录" `
        (Split-Path -Parent $java.JavaExe) (Split-Path -Parent $java.JavawExe)

    Write-Host "[G106 contract] input backend 的 Spring 生效顺序"

    $root = New-CaseRoot @{ "bot.input.backend" = "SEND_INPUT" }; $createdRoots.Add($root)
    $env:BOT_INPUT_BACKEND = $null
    Assert-Equal "本机配置生效" "SEND_INPUT" (Get-DhxyEffectiveInputBackend -ClientRoot $root).Value
    $env:BOT_INPUT_BACKEND = "faker_input"
    $backend = Get-DhxyEffectiveInputBackend -ClientRoot $root
    Assert-Equal "环境变量压过配置文件（Spring relaxed binding）" "FAKER_INPUT" $backend.Value
    Assert-Match "环境变量来源可辨识" "环境变量" $backend.Source
    $env:BOT_INPUT_BACKEND = $null

    Write-Host "[G106 contract] 体检聚合与 ReportOnly"

    $root = New-CaseRoot @{ "bot.input.backend" = "SEND_INPUT" }; $createdRoots.Add($root)
    $aggregated = $null
    try {
        Invoke-DhxyStartupPreflight -ClientRoot $root -CloudRoot "E:/missing-cloud" `
            -CloudRootSource "contract" -JavaExe "" -JavaSource "contract" | Out-Null
    } catch {
        $aggregated = $_.Exception.Message
    }
    Assert-Match "缺失项一次报全而不是遇错即停" "2 条缺失项" $aggregated

    $reportOnlyThrew = $false
    try {
        Invoke-DhxyStartupPreflight -ClientRoot $root -CloudRoot "E:/missing-cloud" `
            -CloudRootSource "contract" -JavaExe "" -JavaSource "contract" -ReportOnly | Out-Null
    } catch {
        $reportOnlyThrew = $true
    }
    Assert-Equal "ReportOnly 只报不阻断" $false $reportOnlyThrew

    Write-Host "[G106 contract] 体检落盘（证据面）"

    # 2026-08-29：体检此前只写交互控制台，日志里零样本，"停进程前一次报全 + 打印解析值"
    # 无法验收。落盘是纯附加：同一批行、判定不变、给不给 LogPath 结论必须逐字一致。
    $root = New-CaseRoot @{ "bot.input.backend" = "SEND_INPUT" }; $createdRoots.Add($root)
    $preflightLogPath = Join-Path $root "logs\startup-preflight.log"
    $teedConsole = (Invoke-DhxyStartupPreflight -ClientRoot $root -CloudRoot "E:/missing-cloud" `
        -CloudRootSource "contract" -JavaExe "" -JavaSource "contract" `
        -ReportOnly -LogPath $preflightLogPath *>&1 | Out-String)
    Assert-Equal "给了 LogPath 就必须落盘（目录会被自动建出来）" `
        $true (Test-Path -LiteralPath $preflightLogPath -PathType Leaf)
    # 读不到就当空串：落盘一旦被改坏，后面每条断言都要照常报 FAIL，而不是让合同自己崩掉。
    $teedFile = Read-TextOrEmpty $preflightLogPath
    Assert-Match "落盘每行带 ISO 时间戳（体检早于停进程的文件证据）" `
        '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} \[DHXY preflight\] 开始' $teedFile
    Assert-Match "解析值必须进文件，不能只留在控制台" "客户端仓" $teedFile
    Assert-Match "缺失项必须一次全进文件" "缺失项 2 条" $teedFile
    # 控制台每一行（去掉行首时间戳后）都必须在文件里找得到——日志不得是控制台的删节版。
    $consoleLines = @($teedConsole -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 })
    # 用 Contains 而不是 -like：解析值里带 [来源] 方括号，会被通配符当字符集吃掉。
    $missingFromFile = @($consoleLines | Where-Object { -not $teedFile.Contains($_) })
    Assert-Equal "文件与控制台逐行一致（无删节）" 0 $missingFromFile.Count

    # 判定面不得被证据面影响：不给 LogPath 时不落任何文件，阻断/不阻断的结论也完全一样。
    $noLogRoot = New-CaseRoot @{ "bot.input.backend" = "SEND_INPUT" }; $createdRoots.Add($noLogRoot)
    $withoutLogFailure = $null
    try {
        Invoke-DhxyStartupPreflight -ClientRoot $noLogRoot -CloudRoot "E:/missing-cloud" `
            -CloudRootSource "contract" -JavaExe "" -JavaSource "contract" *>&1 | Out-Null
    } catch {
        $withoutLogFailure = $_.Exception.Message
    }
    $withLogFailure = $null
    try {
        Invoke-DhxyStartupPreflight -ClientRoot $noLogRoot -CloudRoot "E:/missing-cloud" `
            -CloudRootSource "contract" -JavaExe "" -JavaSource "contract" `
            -LogPath (Join-Path $noLogRoot "logs\startup-preflight.log") *>&1 | Out-Null
    } catch {
        $withLogFailure = $_.Exception.Message
    }
    Assert-Equal "落盘与否不改变阻断结论" $withoutLogFailure $withLogFailure
    $blockedLog = Read-TextOrEmpty (Join-Path $noLogRoot "logs\startup-preflight.log")
    Assert-Equal "阻断路径同样落盘（抛异常前先写结论）" $true ($blockedLog -match "结论=阻断")
    # 写盘失败绝不能反向影响判定：把日志路径指向一个目录名，Add-Content 必然失败。
    $unwritable = Join-Path $noLogRoot "logs"
    $brokenLogFailure = $null
    try {
        Invoke-DhxyStartupPreflight -ClientRoot $noLogRoot -CloudRoot "E:/missing-cloud" `
            -CloudRootSource "contract" -JavaExe "" -JavaSource "contract" `
            -LogPath $unwritable *>&1 | Out-Null
    } catch {
        $brokenLogFailure = $_.Exception.Message
    }
    Assert-Equal "落盘失败被吞掉，结论不变" $withoutLogFailure $brokenLogFailure

    Write-Host "[G106 contract] Maven 与运行 JVM 必须同一个 JDK"

    # Review #2 P1-1 的 false-green：解析链让体检和运行 JVM 用 21，Maven 却按 JAVA_HOME 另挑一个。
    $root = New-CaseRoot @{ "bot.input.backend" = "SEND_INPUT" }; $createdRoots.Add($root)
    $java = Resolve-DhxyJavaRuntime -ClientRoot $root
    $savedForMaven = Use-DhxyJavaHomeForMaven -JavaHome $java.JavaHome
    Assert-Equal "钉住后 JAVA_HOME 等于解析出的 JavaHome" $java.JavaHome $env:JAVA_HOME
    $mavenOutput = (& mvn -v 2>&1 | Out-String)
    $mavenRuntime = [regex]::Match($mavenOutput, 'runtime:\s*(.+)').Groups[1].Value.Trim()
    $expectedHome = [IO.Path]::GetFullPath((Split-Path -Parent (Split-Path -Parent $java.JavaExe)))
    $actualHome = [IO.Path]::GetFullPath($mavenRuntime.TrimEnd('\'))
    $sameJdk = $actualHome.StartsWith($expectedHome, [StringComparison]::OrdinalIgnoreCase) -or
        $expectedHome.StartsWith($actualHome, [StringComparison]::OrdinalIgnoreCase)
    Assert-Equal "mvn -v 的 runtime 与运行 JVM 同一 JDK" $true $sameJdk
    Assert-Match "mvn -v 报的 Java 主版本 >= 21" '^(2[1-9]|[3-9][0-9])$' ([regex]::Match($mavenOutput, 'Java version:\s*(\d+)').Groups[1].Value)

    # 反向用例：JAVA_HOME 指向机器上另一个真实 JDK——mvn 照样能跑，只是用了别的 Java。
    # 这才是 Review #2 P1-1 的真实形态（体检全绿、停机后编译才炸），比"无效路径"更贴近现场。
    # 找一个与运行 JVM 不同的真实 JDK；找不到就退回"无效 JAVA_HOME"，两种都必须被拦。
    $otherJdk = @(
        Get-ChildItem "C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium",
            "C:\Program Files\Microsoft" -Directory -ErrorAction SilentlyContinue |
            Where-Object {
                (Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe")) -and
                $_.FullName -ne $java.JavaHome
            }
    ) | Select-Object -First 1
    $mismatchLabel = if ($null -ne $otherJdk) { "另一个真实 JDK：$($otherJdk.Name)" } else { "无效 JAVA_HOME" }
    $env:JAVA_HOME = if ($null -ne $otherJdk) { $otherJdk.FullName } else { Join-Path ([IO.Path]::GetTempPath()) "not-a-jdk" }

    # 先用 ReportOnly 拿到完整清单（不抛异常），证明失败原因确实点名 Maven——
    # 只断言"抛了异常"会让这条合同自己变成 false-green。
    $mismatchOutput = (Invoke-DhxyStartupPreflight -ClientRoot $root -CloudRoot $root `
        -CloudRootSource "contract" -JavaExe $java.JavaExe -JavaSource "contract" -ReportOnly *>&1 | Out-String)
    Assert-Match "失败原因点名 Maven 的 JDK（$mismatchLabel）" "Maven" $mismatchOutput

    # 再确认阻断模式下确实拦住启动。
    $mismatchFailure = $null
    try {
        Invoke-DhxyStartupPreflight -ClientRoot $root -CloudRoot $root `
            -CloudRootSource "contract" -JavaExe $java.JavaExe -JavaSource "contract" *>&1 | Out-Null
    } catch {
        $mismatchFailure = $_.Exception.Message
    }
    Assert-Match "Maven 与运行 JVM 不一致时体检必须阻断" "缺失项" $mismatchFailure
    [void](Use-DhxyJavaHomeForMaven -JavaHome $savedForMaven)

    Write-Host "[G106 contract] Cloud root 规范化与 OCR 探针输出"

    $root = New-CaseRoot @{ "cloud.turn.sidecar.brain-project-path" = "..\sibling-brain" }
    $createdRoots.Add($root)
    $relative = Resolve-DhxyCloudProjectRoot -ClientRoot $root
    Assert-Equal "相对覆盖值必须规范化为绝对路径" $true ([IO.Path]::IsPathRooted($relative.Path))
    Assert-Equal "规范化后不得残留 .." $false ($relative.Path -like "*..*")

    # Review #2 P2-3：python -c 打中文会穿过控制台代码页变乱码，探针输出必须是可读 ASCII。
    $ocrProbeOutput = & py -3 -c "import importlib.metadata as m, sys
try: v = m.version('rapidocr')
except m.PackageNotFoundError: sys.exit(3)
if not v or not v.strip(): sys.exit(4)
print(v.strip() + chr(9) + 'rapidocr-' + v.strip())" 2>$null
    $ocrProbeLine = [string](@($ocrProbeOutput) | Select-Object -First 1)
    Assert-Match "OCR 探针输出为可读 ASCII（版本<TAB>指纹）" '^[ -~]+	[ -~]+$' $ocrProbeLine
    Assert-Match "指纹必须是真实版本而不是 unknown 占位" '^rapidocr-\d' (($ocrProbeLine -split "`t")[-1])


    $root = New-CaseRoot @{ "bot.input.backend" = "SEND_INPUT" }; $createdRoots.Add($root)
    Remove-Item -Recurse -Force (Join-Path $root "images\template")
    $templateFailure = $null
    try {
        Invoke-DhxyStartupPreflight -ClientRoot $root -CloudRoot $PSScriptRoot `
            -CloudRootSource "contract" -JavaExe "" -JavaSource "contract" | Out-Null
    } catch {
        $templateFailure = $_.Exception.Message
    }
    Assert-Match "模板根缺失必须被拦" "缺失项" $templateFailure
} finally {
    $env:DHXY_CLOUD_BRAIN_ROOT = $savedCloudEnvironment
    $env:DHXY_JAVA_HOME = $savedJavaEnvironment
    $env:BOT_INPUT_BACKEND = $savedBackendEnvironment
    foreach ($createdRoot in $createdRoots) {
        Remove-Item -Recurse -Force -LiteralPath $createdRoot -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "[G106 contract] passed=$script:Passed failed=$script:Failed"
if ($script:Failed -gt 0) {
    exit 1
}
