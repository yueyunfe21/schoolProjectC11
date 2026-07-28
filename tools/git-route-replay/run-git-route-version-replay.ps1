param(
    [string]$Repository = "D:\mavenProject\DHXY-cr271",
    [string]$OutputRoot = "D:\mavenProject\DHXY-cr271\images\test-cases\world-map-route\git-version-replay"
)

$ErrorActionPreference = "Stop"

$versions = @(
    @{ Commit = "13fc6638"; Blob = "4cbb9f82"; Label = "v01-13fc6638" },
    @{ Commit = "8ed3335a"; Blob = "7e8874bc"; Label = "v02-8ed3335a" },
    @{ Commit = "e4b9e5b6"; Blob = "2344a787"; Label = "v03-e4b9e5b6" },
    @{ Commit = "5d6c2e0a"; Blob = "8fedec77"; Label = "v04-5d6c2e0a" },
    @{ Commit = "dcb574e6"; Blob = "1fbe8cbd"; Label = "v05-dcb574e6" },
    @{ Commit = "dc0ac9ed"; Blob = "667d9ffc"; Label = "v06-dc0ac9ed" },
    @{ Commit = "66987933"; Blob = "19e4e793"; Label = "v07-66987933" },
    @{ Commit = "95fe4886"; Blob = "fe4a70a3"; Label = "v08-95fe4886" },
    @{ Commit = "2a97dd43"; Blob = "0c0d405a"; Label = "v09-2a97dd43" },
    @{ Commit = "dc4394f4"; Blob = "c6ed0603"; Label = "v10-dc4394f4" },
    @{ Commit = "ba9c5b7e"; Blob = "bc0a8037"; Label = "v11-ba9c5b7e" }
)

$inputRoot = Join-Path $OutputRoot "inputs"
$archiveRoot = Join-Path $env:TEMP "dhxy-route-git-replay"
$sourceA = "D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\test\Snipaste_2026-07-09_15-28-25.png"
$sourceB = "D:\mavenProject\DHXY-cr271\images\temp\hwnd-147181C\latest_vision.png"
$inputA = Join-Path $inputRoot "case-a-longgong-to-changan.png"
$inputB = Join-Path $inputRoot "case-b-lingshou-to-changan.png"
$inputC = Join-Path $inputRoot "case-c-fengchao-to-fengchao-seven.png"
$testSource = Join-Path $Repository "tools\git-route-replay\GitRouteVersionReplayTest.java"
$cloudArchive = Join-Path $archiveRoot "cloud-3b988caa.zip"
$cloudSnapshot = Join-Path $archiveRoot "cloud-3b988caa"

New-Item -ItemType Directory -Force -Path $inputRoot, $archiveRoot | Out-Null
Copy-Item -LiteralPath $sourceA -Destination $inputA -Force

Add-Type -AssemblyName System.Drawing
$full = [System.Drawing.Bitmap]::FromFile($sourceB)
try {
    $cropRect = [System.Drawing.Rectangle]::new(348, 376, 323, 138)
    $crop = $full.Clone($cropRect, $full.PixelFormat)
    try {
        $crop.Save($inputB, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $crop.Dispose()
    }
} finally {
    $full.Dispose()
}

# The final Client Git blob delegates route preprocessing to the Cloud Git snapshot current on
# that date. Run that exact Cloud source on an isolated port for the duration of the replay.
if (Test-Path -LiteralPath $cloudSnapshot) {
    Remove-Item -LiteralPath $cloudSnapshot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $cloudSnapshot | Out-Null
git -C "D:\mavenProject\dhxy-cloud-brain" archive --format=zip --output=$cloudArchive 3b988caa
Expand-Archive -LiteralPath $cloudArchive -DestinationPath $cloudSnapshot -Force
Push-Location $cloudSnapshot
try {
    & mvn.cmd -q compile
    if ($LASTEXITCODE -ne 0) {
        throw "Cloud Git snapshot compile failed"
    }
    $cloudLog = Join-Path $OutputRoot "cloud-3b988caa.log"
    $cloudProcess = Start-Process -FilePath "mvn.cmd" `
            -ArgumentList @("-q", "-Dcloud.port=18082", "-Dcloud.token=local-dev-token", "exec:java") `
            -WorkingDirectory $cloudSnapshot `
            -RedirectStandardOutput $cloudLog `
            -RedirectStandardError ($cloudLog + ".err") `
            -WindowStyle Hidden `
            -PassThru
} finally {
    Pop-Location
}

$cloudReady = $false
for ($attempt = 0; $attempt -lt 40; $attempt++) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort 18082 -ErrorAction SilentlyContinue
    if ($listener) {
        $cloudReady = $true
        break
    }
    Start-Sleep -Milliseconds 500
}
if (-not $cloudReady) {
    throw "Cloud Git snapshot did not become ready on port 18082"
}

$summary = @()
try {
foreach ($version in $versions) {
    $snapshot = Join-Path $archiveRoot $version.Label
    $versionOutput = Join-Path $OutputRoot $version.Label
    if (Test-Path -LiteralPath $snapshot) {
        Remove-Item -LiteralPath $snapshot -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $snapshot, $versionOutput | Out-Null

    $archive = Join-Path $archiveRoot ($version.Label + ".zip")
    git -C $Repository archive --format=zip --output=$archive $version.Commit
    Expand-Archive -LiteralPath $archive -DestinationPath $snapshot -Force

    $testTarget = Join-Path $snapshot "src\main\java\com\bot\dhxy\vision\GitRouteVersionReplayTest.java"
    New-Item -ItemType Directory -Force -Path (Split-Path $testTarget) | Out-Null
    Copy-Item -LiteralPath $testSource -Destination $testTarget -Force

    $env:ROUTE_REPLAY_INPUT_A = $inputA
    $env:ROUTE_REPLAY_INPUT_B = $inputB
    $env:ROUTE_REPLAY_INPUT_C = $inputC
    $env:ROUTE_REPLAY_OUTPUT = $versionOutput
    $log = Join-Path $versionOutput "maven.log"
    Push-Location $snapshot
    try {
        & mvn.cmd -q compile "org.codehaus.mojo:exec-maven-plugin:3.1.0:java" `
                "-Dexec.mainClass=com.bot.dhxy.vision.GitRouteVersionReplayTest" *> $log
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    if ($exitCode -ne 0) {
        foreach ($case in @("case-a", "case-b", "case-c")) {
            $fallback = Join-Path $versionOutput ($case + "-marked.png")
            $fallbackInput = switch ($case) {
                "case-a" { $inputA }
                "case-b" { $inputB }
                default { $inputC }
            }
            Copy-Item -LiteralPath $fallbackInput -Destination $fallback -Force
            Add-Type -AssemblyName System.Drawing
            $bitmap = [System.Drawing.Bitmap]::FromFile($fallback)
            try {
                $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
                try {
                    $graphics.DrawRectangle([System.Drawing.Pens]::Red, 2, 2, $bitmap.Width - 5, $bitmap.Height - 5)
                    $font = [System.Drawing.Font]::new("Arial", 13, [System.Drawing.FontStyle]::Bold)
                    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::Red)
                    $graphics.DrawString("BUILD_OR_TEST_ERROR", $font, $brush, 5, 5)
                    $font.Dispose()
                    $brush.Dispose()
                } finally {
                    $graphics.Dispose()
                }
                $bitmap.Save(($fallback + ".tmp.png"), [System.Drawing.Imaging.ImageFormat]::Png)
            } finally {
                $bitmap.Dispose()
            }
            Move-Item -LiteralPath ($fallback + ".tmp.png") -Destination $fallback -Force
            Set-Content -LiteralPath (Join-Path $versionOutput ($case + ".tsv")) `
                    -Value "$case`tBUILD_OR_TEST_ERROR`t`t`t`t$fallback"
        }
    }

    $caseA = Get-Content -LiteralPath (Join-Path $versionOutput "case-a.tsv") -Raw
    $caseB = Get-Content -LiteralPath (Join-Path $versionOutput "case-b.tsv") -Raw
    $caseC = Get-Content -LiteralPath (Join-Path $versionOutput "case-c.tsv") -Raw
    $summary += [pscustomobject]@{
        Version = $version.Label
        Commit = $version.Commit
        Blob = $version.Blob
        MavenExit = $exitCode
        CaseA = ($caseA -split "`t")[1]
        CaseB = ($caseB -split "`t")[1]
        CaseC = ($caseC -split "`t")[1]
    }
}
} finally {
    if ($cloudProcess -and -not $cloudProcess.HasExited) {
        Stop-Process -Id $cloudProcess.Id -Force
        $cloudProcess.WaitForExit()
    }
    $cloudListener = Get-NetTCPConnection -State Listen -LocalPort 18082 -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($cloudListener) {
        Stop-Process -Id $cloudListener.OwningProcess -Force
    }
}

$summary | Export-Csv -LiteralPath (Join-Path $OutputRoot "summary.csv") -NoTypeInformation -Encoding UTF8
$summary | Format-Table -AutoSize
