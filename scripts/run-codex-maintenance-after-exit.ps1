[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$python = (Get-Command python.exe -ErrorAction Stop).Source
$maintenanceScript = 'F:\CodexHome\.codex\skills\keep-codex-fast\scripts\keep_codex_fast.py'
$outputRoot = 'C:\CBK\kcf-apply-20260825'
$stdoutPath = Join-Path $outputRoot 'maintenance.stdout.log'
$stderrPath = Join-Path $outputRoot 'maintenance.stderr.log'
$exitPath = Join-Path $outputRoot 'maintenance.exit.txt'

New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null

$arguments = @(
    $maintenanceScript,
    '--apply',
    '--wait-for-codex-exit',
    '--codex-home', 'F:\CodexHome\.codex',
    '--backup-root', $outputRoot,
    '--archive-older-than-days', '10',
    '--worktree-older-than-days', '7',
    '--rotate-logs-above-mb', '64',
    '--repair-thread-metadata-bloat'
)

& $python @arguments 1> $stdoutPath 2> $stderrPath
$exitCode = $LASTEXITCODE
Set-Content -LiteralPath $exitPath -Value "exitCode=$exitCode completedAt=$(Get-Date -Format o)" -Encoding ASCII
exit $exitCode
