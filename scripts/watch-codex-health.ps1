[CmdletBinding()]
param(
    [switch]$Once,
    [switch]$ShowAlert,
    [int]$SampleSeconds = 2,
    [double]$PrivateMemoryThresholdGB = 6,
    [double]$DiskQueueThreshold = 4,
    [int]$ConsecutiveSamples = 5,
    [int]$AlertCooldownSeconds = 300,
    [string]$LogPath = "F:\CodexHome\.codex\logs\codex-health-watch.log"
)

$ErrorActionPreference = 'SilentlyContinue'
$memoryBreaches = 0
$diskBreaches = 0
$lastAlertAt = @{}

function Write-BoundedLog {
    param([string]$Message)

    if ([string]::IsNullOrWhiteSpace($LogPath)) { return }
    $parent = Split-Path -Parent $LogPath
    if ($parent -and -not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    if ((Test-Path -LiteralPath $LogPath) -and (Get-Item -LiteralPath $LogPath).Length -gt 1MB) {
        Move-Item -LiteralPath $LogPath -Destination "$LogPath.1" -Force
    }
    Add-Content -LiteralPath $LogPath -Value "$(Get-Date -Format o) $Message"
}

function Send-HealthAlert {
    param([string]$Key, [string]$Message)

    $now = Get-Date
    if ($lastAlertAt.ContainsKey($Key) -and ($now - $lastAlertAt[$Key]).TotalSeconds -lt $AlertCooldownSeconds) {
        return
    }
    $lastAlertAt[$Key] = $now
    Write-BoundedLog "ALERT $Message"
    if ($ShowAlert) {
        & "$env:SystemRoot\System32\msg.exe" $env:USERNAME "Codex health alert: $Message. Monitor does not terminate processes." 2>$null
    }
}

function Test-CodexDescendant {
    param($Process, [hashtable]$ByPid)

    $cursor = $Process
    for ($depth = 0; $depth -lt 8 -and $null -ne $cursor; $depth++) {
        if ($cursor.Name -ieq 'codex.exe') { return $true }
        $parentId = [int]$cursor.ParentProcessId
        if ($parentId -le 0 -or -not $ByPid.ContainsKey($parentId)) { break }
        $cursor = $ByPid[$parentId]
    }
    return $false
}

do {
    $processes = @(Get-CimInstance Win32_Process)
    $byPid = @{}
    foreach ($process in $processes) { $byPid[[int]$process.ProcessId] = $process }

    $codexProcesses = @($processes | Where-Object { $_.Name -ieq 'codex.exe' })
    $privateBytes = [double](($codexProcesses | Measure-Object -Property PrivatePageCount -Sum).Sum)
    $privateGB = $privateBytes / 1GB

    $diskSamples = @(Get-CimInstance Win32_PerfFormattedData_PerfDisk_LogicalDisk |
        Where-Object { $_.Name -match '(^|\s)D:' })
    $diskQueue = [double](($diskSamples | Measure-Object -Property CurrentDiskQueueLength -Maximum).Maximum)

    $dangerousChildren = @($processes | Where-Object {
        $_.Name -match '^(powershell|pwsh|cmd)\.exe$' -and
        (Test-CodexDescendant $_ $byPid) -and
        $_.CommandLine -match '(?i)(Get-ChildItem\s+[^\r\n]*-Recurse|-Recurse[^\r\n]*Get-ChildItem|\bdir\s+/s\b|\btree\s+/f\b)'
    })

    if ($privateGB -ge $PrivateMemoryThresholdGB) { $memoryBreaches++ } else { $memoryBreaches = 0 }
    if ($diskQueue -ge $DiskQueueThreshold) { $diskBreaches++ } else { $diskBreaches = 0 }

    if ($memoryBreaches -ge $ConsecutiveSamples) {
        Send-HealthAlert 'memory' ("Codex private memory {0:N1} GB for {1} samples; threshold {2:N1} GB" -f $privateGB, $memoryBreaches, $PrivateMemoryThresholdGB)
    }
    if ($diskBreaches -ge $ConsecutiveSamples) {
        Send-HealthAlert 'disk-d' ("D drive queue {0:N1} for {1} samples; threshold {2:N1}" -f $diskQueue, $diskBreaches, $DiskQueueThreshold)
    }
    foreach ($child in $dangerousChildren) {
        Send-HealthAlert "recursive-$($child.ProcessId)" "Codex child PID $($child.ProcessId) is running an unbounded recursive scan"
    }

    $status = "STATUS codexProcesses=$($codexProcesses.Count) privateGB=$([math]::Round($privateGB, 2)) dQueue=$([math]::Round($diskQueue, 2)) recursiveChildren=$($dangerousChildren.Count)"
    if ($Once) { Write-Output $status }
    if (-not $Once) { Start-Sleep -Seconds ([math]::Max(1, $SampleSeconds)) }
} while (-not $Once)
