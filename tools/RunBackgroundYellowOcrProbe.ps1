param(
    [string] $Expected = "",
    [switch] $Full
)

$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$probeRoot = Join-Path $repo "images\temp\yellow_probe"
New-Item -ItemType Directory -Force $probeRoot | Out-Null
$logPath = Join-Path $probeRoot "background-yellow-ocr-probe.log"

$classpathPath = Join-Path $repo "target\classpath.txt"
if (-not (Test-Path $classpathPath)) {
    throw "target\classpath.txt is missing. Run: mvn -q -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target\classpath.txt"
}

$localOcrHealth = "http://127.0.0.1:18761/health"
try {
    Invoke-RestMethod -Uri $localOcrHealth -TimeoutSec 2 | Out-Null
} catch {
    throw "Local OCR sidecar is not reachable at $localOcrHealth. Start it first: python scripts/local_ocr_server.py --host 127.0.0.1 --port 18761"
}

$cp = Get-Content $classpathPath -Raw
$toolsClasses = Join-Path $repo ".codex-tools-classes"
New-Item -ItemType Directory -Force $toolsClasses | Out-Null

& javac -encoding UTF-8 -proc:none -cp "target\classes;$cp" -d $toolsClasses "scripts\YellowOcrProbe.java"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed for scripts\YellowOcrProbe.java"
}

$argsList = @()
if ($Full) {
    $argsList += "--full"
}
if ($Expected -and -not [string]::IsNullOrWhiteSpace($Expected)) {
    $argsList += $Expected
}

& java "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" `
    -cp ".codex-tools-classes;target\classes;$cp" YellowOcrProbe @argsList *>&1 |
    Tee-Object -FilePath $logPath
