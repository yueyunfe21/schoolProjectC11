#!/usr/bin/env pwsh
# G106 证据面（2026-08-29）：只读的换机体检报告入口。
#
# 为什么单独一个脚本：正式重启链 restart-local-dhxy-stack.ps1 里的体检确实跑在停进程之前，
# 但它只写交互控制台，日志里零样本，"停进程前一次报全 + 打印解析值"这条契约无法验收。
# 这个入口用 -ReportOnly 跑同一个 Invoke-DhxyStartupPreflight，把逐行报告 tee 到
# logs\startup-preflight.log，随时可以拿文件当证据。
#
# 本脚本刻意【不含任何 Stop-Process / Stop-DhxyClientWindow / Stop-ProcessTreeSafely】，
# 也不启动任何进程：它只解析路径、读版本、做一次模板根可写探针。判定逻辑与正式链逐字相同
# （同一个函数、同一批参数），这里只是不阻断、并且落盘。

param(
    # 报告落盘位置；默认与正式重启链同一个文件，两边的时间戳可以直接对排。
    [string]$LogPath = ""
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "lib-machine-paths.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogPath)) {
    $LogPath = Join-Path $projectRoot "logs\startup-preflight.log"
}

$cloudProjectRootResolution = Resolve-DhxyCloudProjectRoot -ClientRoot $projectRoot
$javaRuntime = Resolve-DhxyJavaRuntime -ClientRoot $projectRoot
# 与正式链同口径：Maven 按 JAVA_HOME 挑 JDK，不钉住的话体检会报一条与真实启动无关的
# 假不一致。钉住只影响本进程的环境变量，退出即消失。
$previousJavaHomeForMaven = Use-DhxyJavaHomeForMaven -JavaHome $javaRuntime.JavaHome
try {
    Add-DhxyPreflightLogLine -LogPath $LogPath `
        -Line "[DHXY preflight-report] 只读报告入口（本脚本不停止、也不启动任何进程）。"
    Invoke-DhxyStartupPreflight -ClientRoot $projectRoot `
        -CloudRoot $cloudProjectRootResolution.Path `
        -CloudRootSource $cloudProjectRootResolution.Source `
        -JavaExe $javaRuntime.JavaExe -JavaSource $javaRuntime.Source `
        -ReportOnly -LogPath $LogPath
} finally {
    [void](Use-DhxyJavaHomeForMaven -JavaHome $previousJavaHomeForMaven)
}

Write-Host "[DHXY preflight-report] 报告已落盘：$LogPath"
