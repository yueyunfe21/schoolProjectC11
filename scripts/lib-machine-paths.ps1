# 换机契约（G106）：正式启动链上所有"随机器变"的路径只在这里解析一次。
#
# 解析顺序对每一项都相同：显式参数 → 环境变量 → 本机配置文件 → 推导默认值。
# 本机配置文件 = config/application.properties（不入 Git）。用这个文件名是因为它正好落在
# Spring Boot 的默认外部配置位置 optional:file:./config/ 上：客户端进程自动加载且优先级
# 明确高于 classpath 的 application.properties，这些脚本再显式读同一个文件。
# 一个文件、一批键、两边共用，避免"几个地方各写一份真源"。

$script:DhxyLocalMachineConfigCache = @{}

<#
.SYNOPSIS
把一行体检记录追加到落盘日志（只落盘，绝不参与判定）。

.DESCRIPTION
2026-08-29：体检此前只写交互控制台，所以"停进程前一次报全 + 打印解析值"这条契约在日志里
零样本、无法验收。这里补的是唯一的落盘出口：带 ISO 时间戳追加，行内容与控制台逐字一致，
时间戳本身就是"体检早于任何 Stop-Process"的文件证据。

写盘失败（目录只读、盘满、文件被独占）一律吞掉：日志是证据面，不是判定面——绝不能让
落盘故障改变体检结论，也不能让它把当前运行中的进程连累停掉。
#>
function Add-DhxyPreflightLogLine {
    param(
        [Parameter(Mandatory)][AllowEmptyString()][string]$LogPath,
        [Parameter(Mandatory)][AllowEmptyString()][string]$Line
    )
    if ([string]::IsNullOrWhiteSpace($LogPath)) {
        return
    }
    try {
        $directory = Split-Path -Parent $LogPath
        if ($directory -and -not (Test-Path -LiteralPath $directory -PathType Container)) {
            New-Item -ItemType Directory -Force -Path $directory | Out-Null
        }
        $stamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss.fff")
        Add-Content -LiteralPath $LogPath -Value "$stamp $Line" -Encoding UTF8 -ErrorAction Stop
    } catch {
        # 证据面故障不得反向影响判定面，什么都不做。
    }
}

function Get-DhxyLocalMachineConfigPath {
    param([Parameter(Mandatory)][string]$ClientRoot)
    return (Join-Path $ClientRoot "config\application.properties")
}

<#
.SYNOPSIS
读取本机配置文件（不存在时返回空表）。Java .properties 语法的最小子集：key=value、# 或 ! 注释。
#>
function Get-DhxyLocalMachineConfig {
    param([Parameter(Mandatory)][string]$ClientRoot)

    $configPath = Get-DhxyLocalMachineConfigPath -ClientRoot $ClientRoot
    if ($script:DhxyLocalMachineConfigCache.ContainsKey($configPath)) {
        return $script:DhxyLocalMachineConfigCache[$configPath]
    }

    $settings = @{}
    if (Test-Path -LiteralPath $configPath -PathType Leaf) {
        foreach ($line in (Get-Content -LiteralPath $configPath)) {
            $trimmed = $line.Trim()
            if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#") -or $trimmed.StartsWith("!")) {
                continue
            }
            $separator = $trimmed.IndexOfAny([char[]]@('=', ':'))
            if ($separator -le 0) {
                continue
            }
            $key = $trimmed.Substring(0, $separator).Trim()
            # .properties 里 Windows 路径要写成 D:\\foo 或 D:/foo；两种都收下。
            $value = $trimmed.Substring($separator + 1).Trim().Replace("\\", "\")
            if ($key.Length -gt 0) {
                $settings[$key] = $value
            }
        }
    }
    $script:DhxyLocalMachineConfigCache[$configPath] = $settings
    return $settings
}

function Resolve-DhxyMachineValue {
    param(
        [Parameter(Mandatory)][string]$ClientRoot,
        [string]$Explicit,
        [string]$EnvironmentName,
        [string]$ConfigKey
    )

    if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
        return [pscustomobject]@{ Value = $Explicit.Trim(); Source = "显式参数" }
    }
    if (-not [string]::IsNullOrWhiteSpace($EnvironmentName)) {
        $fromEnvironment = [Environment]::GetEnvironmentVariable($EnvironmentName)
        if (-not [string]::IsNullOrWhiteSpace($fromEnvironment)) {
            return [pscustomobject]@{ Value = $fromEnvironment.Trim(); Source = "环境变量 $EnvironmentName" }
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($ConfigKey)) {
        $settings = Get-DhxyLocalMachineConfig -ClientRoot $ClientRoot
        if ($settings.ContainsKey($ConfigKey) -and -not [string]::IsNullOrWhiteSpace($settings[$ConfigKey])) {
            $configPath = Get-DhxyLocalMachineConfigPath -ClientRoot $ClientRoot
            return [pscustomobject]@{ Value = $settings[$ConfigKey]; Source = "本机配置 $configPath ($ConfigKey)" }
        }
    }
    return $null
}

<#
.SYNOPSIS
把可能是相对路径的配置值规范化成绝对路径（相对值按客户端仓解析）。
#>
function Resolve-DhxyAbsolutePath {
    param(
        [Parameter(Mandatory)][string]$BasePath,
        [Parameter(Mandatory)][string]$Value
    )

    $candidate = $Value.Trim()
    if (-not [IO.Path]::IsPathRooted($candidate)) {
        $candidate = Join-Path $BasePath $candidate
    }
    return [IO.Path]::GetFullPath($candidate)
}

<#
.SYNOPSIS
解析 dhxy-cloud-brain 仓路径。默认值 = 客户端仓的同级目录，不再钉死 D:\mavenProject。
#>
function Resolve-DhxyCloudProjectRoot {
    param(
        [Parameter(Mandatory)][string]$ClientRoot,
        [string]$Explicit
    )

    $resolved = Resolve-DhxyMachineValue -ClientRoot $ClientRoot -Explicit $Explicit `
        -EnvironmentName "DHXY_CLOUD_BRAIN_ROOT" -ConfigKey "cloud.turn.sidecar.brain-project-path"
    if ($null -ne $resolved) {
        # 覆盖值可能是相对路径或带 / 混写：一律按客户端仓规范化成绝对路径，
        # 否则"restart 已把绝对路径传给子启动器"这句报告是不成立的，编译仓与运行仓可能分叉。
        return [pscustomobject]@{
            Path = (Resolve-DhxyAbsolutePath -BasePath $ClientRoot -Value $resolved.Value)
            Source = $resolved.Source
        }
    }
    $sibling = Join-Path (Split-Path -Parent $ClientRoot) "dhxy-cloud-brain"
    return [pscustomobject]@{
        Path = (Resolve-DhxyAbsolutePath -BasePath $ClientRoot -Value $sibling)
        Source = "推导（客户端仓同级 dhxy-cloud-brain）"
    }
}

<#
.SYNOPSIS
解析 Java 运行时。旧版把 JDK 补丁版本号写死在脚本里，换机必炸；这里改为可覆盖 + JAVA_HOME + PATH。
#>
function Resolve-DhxyJavaRuntime {
    param(
        [Parameter(Mandatory)][string]$ClientRoot,
        [string]$Explicit
    )

    # 候选按优先级排列，但"存在"才算数：某一级配错（比如机器上留了指向已卸载 JDK 的 JAVA_HOME）
    # 要能继续下探到 PATH——旧脚本对写死路径就是这么兜的，这里保留同样的韧性。
    $candidates = [System.Collections.Generic.List[object]]::new()
    $configured = Resolve-DhxyMachineValue -ClientRoot $ClientRoot -Explicit $Explicit `
        -EnvironmentName "DHXY_JAVA_HOME" -ConfigKey "dhxy.machine.java-home"
    if ($null -ne $configured) {
        $candidates.Add([pscustomobject]@{ Home = $configured.Value; Source = $configured.Source })
    }
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidates.Add([pscustomobject]@{ Home = $env:JAVA_HOME.Trim(); Source = "环境变量 JAVA_HOME" })
    }
    $fromPath = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($null -ne $fromPath) {
        $pathBinDirectory = Split-Path -Parent $fromPath.Source
        $candidates.Add([pscustomobject]@{ Home = (Split-Path -Parent $pathBinDirectory); Source = "PATH 上的 java.exe" })
    }

    $firstMissing = $null
    foreach ($candidate in $candidates) {
        $javaExe = Join-Path $candidate.Home "bin\java.exe"
        if (Test-Path -LiteralPath $javaExe -PathType Leaf) {
            $source = $candidate.Source
            if ($null -ne $firstMissing) {
                $source += "（已跳过无效的 $($firstMissing.Source)：$($firstMissing.Home)）"
            }
            return [pscustomobject]@{
                JavaHome = $candidate.Home
                JavaExe = $javaExe
                JavawExe = (Join-Path $candidate.Home "bin\javaw.exe")
                Source = $source
            }
        }
        if ($null -eq $firstMissing) {
            $firstMissing = $candidate
        }
    }

    if ($null -ne $firstMissing) {
        # 全都不存在时，把最高优先级那个原样报出来，错误信息才指得到人配错的地方。
        return [pscustomobject]@{
            JavaHome = $firstMissing.Home
            JavaExe = (Join-Path $firstMissing.Home "bin\java.exe")
            JavawExe = (Join-Path $firstMissing.Home "bin\javaw.exe")
            Source = "$($firstMissing.Source)（该路径下没有 bin\java.exe）"
        }
    }
    return [pscustomobject]@{ JavaHome = ""; JavaExe = ""; JavawExe = ""; Source = "未解析到" }
}

<#
.SYNOPSIS
把 Maven 钉到与 preflight/运行 JVM 同一个 JDK。
.DESCRIPTION
Maven 自己按 JAVA_HOME 挑 JDK，跟 DHXY_JAVA_HOME/本机配置这条链是两个口径：解析链可以让体检和
运行 JVM 都用 21，Maven 却仍可能用机器上残留的旧 JAVA_HOME——体检全绿、旧进程停掉之后才在编译阶段炸。
每个会调用 mvn 的脚本都必须先调用它。返回被覆盖前的旧值，纯粹为了记录。
#>
function Use-DhxyJavaHomeForMaven {
    param([Parameter(Mandatory)][AllowEmptyString()][string]$JavaHome)

    $previous = $env:JAVA_HOME
    if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
        $env:JAVA_HOME = $JavaHome
    }
    return $previous
}

<#
.SYNOPSIS
按 Spring 的实际生效顺序求 bot.input.backend，用于决定要不要查 FakerInput 驱动门。
.DESCRIPTION
顺序必须与 Spring 一致，否则体检会查错门：环境变量（Spring relaxed binding 把 BOT_INPUT_BACKEND
映射到 bot.input.backend，优先级高于所有配置文件）→ config/application.properties（默认外部位置）
→ classpath 里的 application.properties。少读一层就会出现"环境变量已切 SEND_INPUT，体检还在拦
FakerInput 驱动"或反过来漏掉驱动门。
#>
function Get-DhxyEffectiveInputBackend {
    param([Parameter(Mandatory)][string]$ClientRoot)

    foreach ($environmentName in @("BOT_INPUT_BACKEND", "bot.input.backend")) {
        $fromEnvironment = [Environment]::GetEnvironmentVariable($environmentName)
        if (-not [string]::IsNullOrWhiteSpace($fromEnvironment)) {
            return [pscustomobject]@{
                Value = $fromEnvironment.Trim().ToUpperInvariant()
                Source = "环境变量 $environmentName"
            }
        }
    }
    $settings = Get-DhxyLocalMachineConfig -ClientRoot $ClientRoot
    if ($settings.ContainsKey("bot.input.backend") -and -not [string]::IsNullOrWhiteSpace($settings["bot.input.backend"])) {
        return [pscustomobject]@{
            Value = $settings["bot.input.backend"].Trim().ToUpperInvariant()
            Source = "本机配置 $(Get-DhxyLocalMachineConfigPath -ClientRoot $ClientRoot)"
        }
    }
    $applicationProperties = Join-Path $ClientRoot "src\main\resources\application.properties"
    if (Test-Path -LiteralPath $applicationProperties -PathType Leaf) {
        foreach ($line in (Get-Content -LiteralPath $applicationProperties)) {
            $trimmed = $line.Trim()
            if ($trimmed -match '^bot\.input\.backend\s*=\s*(\S+)') {
                return [pscustomobject]@{
                    Value = $Matches[1].Trim().ToUpperInvariant()
                    Source = "classpath application.properties"
                }
            }
        }
    }
    return [pscustomobject]@{ Value = ""; Source = "未配置" }
}

<#
.SYNOPSIS
启动前一次性体检。必须在停掉旧进程之前调用：把所有缺失项一次全查出来一次全报，
避免"停掉当前程序之后才逐条失败"，人被晾在半停状态。
.DESCRIPTION
除了通过/失败，还会打印每一项最终解析到的实际值——换机时才分得清"没装"和"解析到了错的那个"。
#>
function Get-DhxyNativeOutput {
    <#
    .SYNOPSIS
        运行一个外部命令并把 stdout+stderr 合成一个字符串。
    .DESCRIPTION
        `java -version` / `mvn -v` 都把版本写进 stderr。调用方（restart 脚本）跑在
        $ErrorActionPreference = "Stop" 下，而 Windows PowerShell 5.1 会把 2>&1 收到的
        原生 stderr 行变成 NativeCommandError 直接抛出——preflight 于是在"读 Java 版本"
        这一步炸掉，而不是把版本读出来。pwsh 7 不这样，所以只按 pwsh 调试时看不见。
        正式启动链走的是 powershell.exe(5.1)，这里必须把偏好降回 Continue 再收集。
    #>
    param(
        [Parameter(Mandatory)][string]$Command,
        [string[]]$Arguments = @()
    )
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        return (& $Command @Arguments 2>&1 | Out-String)
    } catch {
        return ""
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Invoke-DhxyStartupPreflight {
    param(
        [Parameter(Mandatory)][string]$ClientRoot,
        [Parameter(Mandatory)][string]$CloudRoot,
        [Parameter(Mandatory)][string]$CloudRootSource,
        [Parameter(Mandatory)][AllowEmptyString()][string]$JavaExe,
        [Parameter(Mandatory)][AllowEmptyString()][string]$JavaSource,
        # 应急路径：照查照报，只是不阻断。刻意不提供"整段跳过"。
        [switch]$ReportOnly,
        # 纯证据面：给了就把报告逐行追加到该文件。判定逻辑与它完全无关（空=只写控制台）。
        [string]$LogPath = ""
    )

    $failures = [System.Collections.Generic.List[string]]::new()
    $resolvedLines = [System.Collections.Generic.List[string]]::new()

    # 控制台与落盘的唯一出口：两边逐字一致，日志不得是控制台的删节版。
    function Write-PreflightLine {
        param([AllowEmptyString()][string]$Text)
        Write-Host $Text
        Add-DhxyPreflightLogLine -LogPath $LogPath -Line $Text
    }

    Write-PreflightLine "[DHXY preflight] 开始（停止任何旧进程之前）：reportOnly=$([bool]$ReportOnly) pid=$PID"

    function Add-Resolved {
        param([string]$Label, [string]$Value)
        $resolvedLines.Add(("  {0,-22} {1}" -f $Label, $Value))
    }

    # 1) 客户端仓：正式入口强制以项目根为工作目录，相对图片路径全靠它。
    Add-Resolved "客户端仓" $ClientRoot
    if (-not (Test-Path -LiteralPath (Join-Path $ClientRoot "pom.xml") -PathType Leaf)) {
        $failures.Add("客户端仓不是 Maven 项目根（缺 pom.xml）：$ClientRoot")
    }

    # 2) 云端仓
    Add-Resolved "云端仓" "$CloudRoot   [$CloudRootSource]"
    if (-not (Test-Path -LiteralPath $CloudRoot -PathType Container)) {
        $failures.Add("云端仓不存在：$CloudRoot（用环境变量 DHXY_CLOUD_BRAIN_ROOT 或本机配置 config/application.properties 的 cloud.turn.sidecar.brain-project-path 指定）")
    } elseif (-not (Test-Path -LiteralPath (Join-Path $CloudRoot "pom.xml") -PathType Leaf)) {
        $failures.Add("云端仓不是 Maven 项目根（缺 pom.xml）：$CloudRoot")
    }

    # 3) images/template：运行时可写的唯一资产真源（map-label 直接写回这里），只读也算故障。
    $templateRoot = Join-Path $ClientRoot "images\template"
    Add-Resolved "模板根(可写)" $templateRoot
    if (-not (Test-Path -LiteralPath $templateRoot -PathType Container)) {
        $failures.Add("模板根不存在：$templateRoot（运行时资产真源，不能缺）")
    } else {
        $probeFile = Join-Path $templateRoot ".preflight-write-probe"
        try {
            Set-Content -LiteralPath $probeFile -Value "probe" -Encoding UTF8 -ErrorAction Stop
            Remove-Item -LiteralPath $probeFile -Force -ErrorAction Stop
        } catch {
            $failures.Add("模板根不可写：$templateRoot（map-label 需要写回，只读会静默丢学习结果）")
        }
    }

    # 4) Java
    Add-Resolved "Java" "$JavaExe   [$JavaSource]"
    if ([string]::IsNullOrWhiteSpace($JavaExe) -or -not (Test-Path -LiteralPath $JavaExe -PathType Leaf)) {
        $failures.Add("Java 未解析到（设置 JAVA_HOME、环境变量 DHXY_JAVA_HOME 或本机配置 config/application.properties 的 dhxy.machine.java-home）")
    } else {
        $javaVersionRaw = Get-DhxyNativeOutput -Command $JavaExe -Arguments @("-version")
        $javaVersionMatch = [regex]::Match($javaVersionRaw, 'version "(\d+)')
        if (-not $javaVersionMatch.Success) {
            $failures.Add("无法读取 Java 版本：$JavaExe")
        } else {
            $javaMajor = [int]$javaVersionMatch.Groups[1].Value
            Add-Resolved "Java 版本" "$javaMajor"
            if ($javaMajor -lt 21) {
                $failures.Add("Java 版本过低：需要 21+，实测 $javaMajor（$JavaExe）")
            }
        }
    }

    # 5) Maven：正式入口是 mvn compile + java -cp，没有可分发的 fat-JAR，Maven 是硬依赖。
    #    并且 Maven 自己按 JAVA_HOME 挑 JDK——与本文件的解析链是两个口径。脚本会先用
    #    Use-DhxyJavaHomeForMaven 把 JAVA_HOME 钉过去，这里再从 `mvn -v` 的 runtime 行取证，
    #    确认钉住了：不取证就会出现"体检全绿、停掉旧进程后编译才炸"。
    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($null -eq $mavenCommand) {
        Add-Resolved "Maven" "未解析到"
        $failures.Add("PATH 上找不到 mvn（正式启动链每次都要 mvn compile）")
    } else {
        $mavenVersionRaw = Get-DhxyNativeOutput -Command "mvn" -Arguments @("-v")
        $mavenVersionMatch = [regex]::Match($mavenVersionRaw, 'Apache Maven (\S+)')
        $mavenVersion = if ($mavenVersionMatch.Success) { $mavenVersionMatch.Groups[1].Value } else { "版本未知" }
        Add-Resolved "Maven" "$($mavenCommand.Source)   [$mavenVersion]"
        if (-not $mavenVersionMatch.Success) {
            $failures.Add("mvn 存在但无法执行 mvn -v：$($mavenCommand.Source)")
        }
        $mavenRuntimeMatch = [regex]::Match($mavenVersionRaw, 'runtime:\s*(.+)')
        $mavenJavaMatch = [regex]::Match($mavenVersionRaw, 'Java version:\s*(\d+)')
        $mavenRuntime = if ($mavenRuntimeMatch.Success) { $mavenRuntimeMatch.Groups[1].Value.Trim() } else { "" }
        $mavenJavaMajor = if ($mavenJavaMatch.Success) { [int]$mavenJavaMatch.Groups[1].Value } else { -1 }
        Add-Resolved "Maven 的 JDK" "$(if ($mavenRuntime) { $mavenRuntime } else { '未能读出' })   [Java $mavenJavaMajor]"
        if ([string]::IsNullOrWhiteSpace($mavenRuntime)) {
            $failures.Add("无法从 mvn -v 读出 Maven 实际使用的 JDK 路径：$($mavenCommand.Source)")
        } elseif ($mavenJavaMajor -gt 0 -and $mavenJavaMajor -lt 21) {
            $failures.Add("Maven 在用 Java $mavenJavaMajor（需要 21+）：$mavenRuntime。JAVA_HOME 与解析链不一致——编译会在旧进程停掉之后才失败")
        } elseif (-not [string]::IsNullOrWhiteSpace($JavaExe)) {
            # runtime 行给的是 JDK 根（部分发行版给到 jre 子目录），两边都规范化后比对前缀。
            $expectedHome = [IO.Path]::GetFullPath((Split-Path -Parent (Split-Path -Parent $JavaExe)))
            $actualHome = [IO.Path]::GetFullPath($mavenRuntime.TrimEnd('\'))
            if (-not $actualHome.StartsWith($expectedHome, [StringComparison]::OrdinalIgnoreCase) -and
                    -not $expectedHome.StartsWith($actualHome, [StringComparison]::OrdinalIgnoreCase)) {
                $failures.Add("Maven 用的 JDK 与运行 JVM 不是同一个：Maven=$actualHome，运行=$expectedHome。两个口径不一致时体检会假绿")
            }
        }
    }

    # 6) Python + RapidOCR：OCR sidecar 的运行时，缺了云端起不来。
    $pythonLauncher = ""
    $pythonBaseArgs = @()
    if (Get-Command "py" -ErrorAction SilentlyContinue) {
        $pythonLauncher = "py"
        $pythonBaseArgs = @("-3")
    } elseif (Get-Command "python" -ErrorAction SilentlyContinue) {
        $pythonLauncher = "python"
    }
    if ([string]::IsNullOrWhiteSpace($pythonLauncher)) {
        Add-Resolved "Python" "未解析到"
        $failures.Add("PATH 上既没有 py 也没有 python（OCR sidecar 必需）")
    } else {
        $pythonExe = [string](@(& $pythonLauncher @($pythonBaseArgs + @("-c", "import sys; print(sys.executable)")) 2>$null) | Select-Object -First 1)
        Add-Resolved "Python" "$pythonExe   [$pythonLauncher]"
        # 两个都要打：真实版本取自包元数据（人能据此判断装对没有），指纹取自
        # run-cloud-brain-server.ps1 求 localOcrExpectedModelFingerprint 的同一段代码。
        # 探针只输出 ASCII（版本<TAB>指纹），中文标注一律由 PowerShell 加：python -c 的输出要
        # 穿过控制台代码页，让它打中文会变乱码（Review #2 P2-3 实锤）。
        $ocrProbe = "import importlib.metadata as m, sys" +
            "`ntry: v = m.version('rapidocr')" +
            "`nexcept m.PackageNotFoundError: sys.exit(3)" +
            "`nif not v or not v.strip(): sys.exit(4)" +
            "`nprint(v.strip() + chr(9) + 'rapidocr-' + v.strip())"
        $ocrVersionRaw = & $pythonLauncher @($pythonBaseArgs + @("-c", $ocrProbe)) 2>$null
        $ocrProbeExit = $LASTEXITCODE
        $ocrVersionLine = [string](@($ocrVersionRaw) | Select-Object -First 1)
        if ($ocrProbeExit -ne 0 -or [string]::IsNullOrWhiteSpace($ocrVersionLine)) {
            $ocrHint = switch ($ocrProbeExit) {
                3 { "rapidocr 发行版元数据缺失" }
                4 { "rapidocr 元数据版本为空" }
                default { "rapidocr 未安装或不可导入" }
            }
            $failures.Add("$ocrHint（Python：$pythonExe，exit=$ocrProbeExit）。重装：$pythonLauncher $($pythonBaseArgs -join ' ') -m pip install -r $CloudRoot\ocr
equirements.txt")
        } else {
            $ocrParts = $ocrVersionLine.Trim() -split "`t"
            Add-Resolved "RapidOCR" "$($ocrParts[0])   (云端身份指纹=$($ocrParts[-1]))"
        }
    }

    # 7) FakerInput 虚拟 HID 驱动：backend=FAKER_INPUT 时是 fail-closed，缺驱动等于起不来。
    $inputBackendResolution = Get-DhxyEffectiveInputBackend -ClientRoot $ClientRoot
    $inputBackend = $inputBackendResolution.Value
    Add-Resolved "输入后端" "$(if ([string]::IsNullOrWhiteSpace($inputBackend)) { '未配置' } else { $inputBackend })   [$($inputBackendResolution.Source)]"
    if ($inputBackend -eq "FAKER_INPUT") {
        # 判据 = 驱动自己的根设备节点（ROOT\SYSTEM\xxxx，FriendlyName "FakerInput Device"）。
        # 本机实测（2026-08-27）：FakerInput 的 HID 子设备枚举成 HID\SYSTEM&Col01..Col06，
        # 整张设备表和 HID 接口注册表里都没有 VID_FE0F/PID_00FF——按 VID/PID 判在这里必然误报。
        # （FakerInputDevice.isFakerInputPath 里的 vid_fe0f 只用于挑错误文案，端点是按 usage 选的。）
        $fakerDevice = @(Get-PnpDevice -PresentOnly -ErrorAction SilentlyContinue |
            Where-Object { $_.FriendlyName -match 'FakerInput' })
        if ($fakerDevice.Count -eq 0) {
            $failures.Add("FakerInput 虚拟 HID 驱动未安装（设备表里没有 FakerInput 节点）。装驱动，或在 config/application.properties 里写 bot.input.backend=SEND_INPUT 换后端")
        } else {
            $fakerStatus = $fakerDevice[0].Status
            Add-Resolved "FakerInput 驱动" "$($fakerDevice[0].InstanceId)   [$fakerStatus]"
            if ($fakerStatus -ne "OK") {
                $failures.Add("FakerInput 驱动状态异常：$fakerStatus（$($fakerDevice[0].InstanceId)）")
            }
        }
    }

    Write-PreflightLine "[DHXY preflight] 最终解析结果："
    foreach ($line in $resolvedLines) {
        Write-PreflightLine $line
    }

    if ($failures.Count -gt 0) {
        Write-PreflightLine "[DHXY preflight] 缺失项 $($failures.Count) 条（旧进程尚未停止，当前运行不受影响）："
        foreach ($failure in $failures) {
            Write-PreflightLine "  - $failure"
        }
        if ($ReportOnly) {
            Write-PreflightLine "[DHXY preflight] 已按 -IgnorePreflightFailures 继续启动（应急路径，上述缺失项仍然存在）。"
            return
        }
        # 结论先落盘再抛：抛出后调用方可能直接退出，晚落盘等于没有证据。
        Write-PreflightLine "[DHXY preflight] 结论=阻断（$($failures.Count) 条缺失项）。旧进程未被停止。"
        throw "启动前体检未通过：$($failures.Count) 条缺失项（见上方清单）。"
    }
    Write-PreflightLine "[DHXY preflight] 全部通过。"
}
