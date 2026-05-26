param(
    [ValidateSet("windowMessageAlt1", "miniMapProbe")]
    [string] $Mode = "windowMessageAlt1"
)

$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$logPath = Join-Path $repo "logs\admin-alt1-postmessage-test.log"
New-Item -ItemType Directory -Force (Split-Path -Parent $logPath) | Out-Null

$cp = Get-Content "target\classpath.txt" -Raw
$toolsClasses = Join-Path $repo ".codex-tools-classes"
New-Item -ItemType Directory -Force $toolsClasses | Out-Null

& javac -encoding UTF-8 -proc:none -cp "target\classes;$cp" -d $toolsClasses "tools\XiuluoAcceptBenchmarkRunner.java"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed for tools\XiuluoAcceptBenchmarkRunner.java"
}

$modeSwitch = if ($Mode -eq "miniMapProbe") {
    "-Dxiuluo.benchmark.onlyMiniMapProbe=true"
} else {
    "-Dxiuluo.benchmark.onlyWindowMessageAlt1=true"
}

& java "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" `
    "-Dspring.main.banner-mode=off" $modeSwitch `
    -cp "target\classes;.codex-tools-classes;$cp" XiuluoAcceptBenchmarkRunner *>&1 |
    Tee-Object -FilePath $logPath
