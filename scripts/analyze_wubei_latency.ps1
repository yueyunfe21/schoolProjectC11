param(
    [string]$LogPath = "logs/dhxy-console.log",
    [string]$StartTime = "",
    [string]$EndTime = "",
    [string]$Contains = "",
    [string]$JsonOut = "",
    [string]$BaselineJson = ""
)

$ErrorActionPreference = "Stop"

function Parse-LogTime {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $null
    }
    return [datetime]::ParseExact(
        $Value,
        "yyyy-MM-dd HH:mm:ss.fff",
        [System.Globalization.CultureInfo]::InvariantCulture
    )
}

function Add-NumericField {
    param(
        [hashtable]$Map,
        [string]$Name,
        [string]$Line
    )
    if ($Line -match "(?:^|\s)$([regex]::Escape($Name))=(?<value>-?\d+)") {
        if (-not $Map.ContainsKey($Name)) {
            $Map[$Name] = New-Object System.Collections.Generic.List[int64]
        }
        [void]$Map[$Name].Add([int64]$Matches["value"])
    }
}

function Add-Count {
    param(
        [hashtable]$Map,
        [string]$Name,
        [int]$Amount = 1
    )
    if (-not $Map.ContainsKey($Name)) {
        $Map[$Name] = 0
    }
    $Map[$Name] = [int64]$Map[$Name] + $Amount
}

function Get-Percentile {
    param(
        [int64[]]$Values,
        [double]$Percent
    )
    if ($Values.Count -eq 0) {
        return $null
    }
    $sorted = $Values | Sort-Object
    $index = [math]::Ceiling(($Percent / 100.0) * $sorted.Count) - 1
    if ($index -lt 0) {
        $index = 0
    }
    if ($index -ge $sorted.Count) {
        $index = $sorted.Count - 1
    }
    return $sorted[$index]
}

function Format-TimingSummary {
    param(
        [string]$Name,
        [System.Collections.Generic.List[int64]]$Values
    )
    if ($null -eq $Values -or $Values.Count -eq 0) {
        return "{0,-28} count={1,6}" -f $Name, 0
    }
    $array = [int64[]]$Values.ToArray()
    $avg = [math]::Round(($array | Measure-Object -Average).Average, 1)
    $max = ($array | Measure-Object -Maximum).Maximum
    $p50 = Get-Percentile $array 50
    $p95 = Get-Percentile $array 95
    $p99 = Get-Percentile $array 99
    return "{0,-28} count={1,6} avg={2,8} p50={3,6} p95={4,6} p99={5,6} max={6,6}" -f `
        $Name, $array.Count, $avg, $p50, $p95, $p99, $max
}

function New-TimingSummary {
    param([System.Collections.Generic.List[int64]]$Values)
    if ($null -eq $Values -or $Values.Count -eq 0) {
        return [ordered]@{
            count = 0
            avg = $null
            p50 = $null
            p95 = $null
            p99 = $null
            max = $null
        }
    }
    $array = [int64[]]$Values.ToArray()
    return [ordered]@{
        count = $array.Count
        avg = [math]::Round(($array | Measure-Object -Average).Average, 1)
        p50 = Get-Percentile $array 50
        p95 = Get-Percentile $array 95
        p99 = Get-Percentile $array 99
        max = ($array | Measure-Object -Maximum).Maximum
    }
}

function Get-ObjectValue {
    param(
        [object]$Object,
        [string]$Name
    )
    if ($null -eq $Object) {
        return $null
    }
    if ($Object -is [hashtable] -or $Object -is [System.Collections.Specialized.OrderedDictionary]) {
        if ($Object.Contains($Name)) {
            return $Object[$Name]
        }
        return $null
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

if (-not (Test-Path -LiteralPath $LogPath)) {
    throw "Log file not found: $LogPath"
}

$start = Parse-LogTime $StartTime
$end = Parse-LogTime $EndTime
$timestampPattern = "^(?<ts>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})"

$counters = [ordered]@{
    "task.turn.handoff" = 0
    "task.turn.handoff.sameAsPrevious" = 0
    "task.turn.release" = 0
    "task.turn.release.priorityYield" = 0
    "consumePrepared.absent" = 0
    "consumePrepared.consumed" = 0
    "dialog.interest.update" = 0
    "window.ready.publish" = 0
    "window.ready.await" = 0
    "window.ready.await.event" = 0
    "window.ready.await.timeout" = 0
    "PATHING_TERMINAL" = 0
    "TASK_ATTENTION_REQUIRED" = 0
    "COMBAT_STATE_CHANGED" = 0
    "window.combat.state.changed" = 0
    "input.request" = 0
    "input.trace.queuedAction" = 0
    "input.trace.clickAction" = 0
    "input.trace.moveAction" = 0
    "input.trace.physical" = 0
    "input.trace.physicalClick" = 0
    "input.trace.physicalMove" = 0
    "input.trace.physicalKeyboard" = 0
    "wubei.wait.parkFinished" = 0
    "wubei.wait.wakeEvent" = 0
    "wubei.wait.wakeTimeout" = 0
    "wubei.wait.wakeNone" = 0
    "wubei.wait.skipAlreadyReady" = 0
    "wubei.wait.skipNoRuntime" = 0
    "slow.observer" = 0
    "timeout" = 0
}

$values = @{}
$transactionHandoffs = @{}
$transactionSameWindow = @{}
$transactionReleases = @{}
$parkByReason = @{}
$readyAwaitByResult = @{}
$totalLines = 0
$matchedLines = 0

Get-Content -LiteralPath $LogPath | ForEach-Object {
    $line = $_
    $totalLines++

    if ($line -match $timestampPattern) {
        $lineTime = Parse-LogTime $Matches["ts"]
        if ($null -ne $start -and $lineTime -lt $start) {
            return
        }
        if ($null -ne $end -and $lineTime -gt $end) {
            return
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($Contains) -and $line -notlike "*$Contains*") {
        return
    }

    $matchedLines++

    if ($line -like "*task.turn.handoff*") {
        $counters["task.turn.handoff"]++
        if ($line -match "\btransaction=(?<transaction>\S+)") {
            Add-Count $transactionHandoffs $Matches["transaction"]
        }
    }
    if ($line -like "*task.turn.handoff*" -and $line -like "*sameAsPrevious=true*") {
        $counters["task.turn.handoff.sameAsPrevious"]++
        if ($line -match "\btransaction=(?<transaction>\S+)") {
            Add-Count $transactionSameWindow $Matches["transaction"]
        }
    }
    if ($line -like "*event=task.turn.release*") {
        $counters["task.turn.release"]++
        if ($line -match "\btransaction=(?<transaction>\S+)") {
            Add-Count $transactionReleases $Matches["transaction"]
        }
        if ($line -match "\bresult=(?<result>SHARED_STATE_TRIGGERED|PATHING_STARTED|MUST_YIELD)") {
            $counters["task.turn.release.priorityYield"]++
        }
    }
    if ($line -like "*consumePrepared*" -and $line -like "*result=absent*") {
        $counters["consumePrepared.absent"]++
    }
    if ($line -like "*consumePrepared*" -and $line -like "*result=consumed*") {
        $counters["consumePrepared.consumed"]++
    }
    if ($line -like "*dialog.interest.update*") { $counters["dialog.interest.update"]++ }
    if ($line -like "*event=window.ready.publish*") { $counters["window.ready.publish"]++ }
    if ($line -like "*event=window.ready.await*") {
        $counters["window.ready.await"]++
        if ($line -match "\bresult=(?<result>\S+)") {
            Add-Count $readyAwaitByResult $Matches["result"]
        }
        if ($line -like "*wokeByEvent=true*") {
            $counters["window.ready.await.event"]++
        }
        if ($line -like "*wokeByTimeout=true*") {
            $counters["window.ready.await.timeout"]++
        }
    }
    if ($line -like "*PATHING_TERMINAL*") { $counters["PATHING_TERMINAL"]++ }
    if ($line -like "*TASK_ATTENTION_REQUIRED*") { $counters["TASK_ATTENTION_REQUIRED"]++ }
    if ($line -like "*COMBAT_STATE_CHANGED*") { $counters["COMBAT_STATE_CHANGED"]++ }
    if ($line -like "*event=window.combat.state.changed*") { $counters["window.combat.state.changed"]++ }
    if ($line -like "*input.request*") { $counters["input.request"]++ }
    if ($line.Contains("[INPUT_TRACE] queued-action")) {
        $counters["input.trace.queuedAction"]++
        if ($line -match "\baction=(?<action>\S+)") {
            if ($Matches["action"] -like "*click*") {
                $counters["input.trace.clickAction"]++
            }
            if ($Matches["action"] -like "*move*") {
                $counters["input.trace.moveAction"]++
            }
        }
    }
    if ($line.Contains("[INPUT_TRACE] physical")) {
        $counters["input.trace.physical"]++
        if ($line -match "\boperation=(?<operation>\S+)") {
            $operation = $Matches["operation"]
            if ($operation -like "*click*") {
                $counters["input.trace.physicalClick"]++
            }
            if ($operation -like "*move*") {
                $counters["input.trace.physicalMove"]++
            }
            if ($operation -like "*press*" -or $operation -like "*type*" -or $operation -like "*key*") {
                $counters["input.trace.physicalKeyboard"]++
            }
        }
    }
    if ($line.Contains("[wubei wait] park finished")) {
        $counters["wubei.wait.parkFinished"]++
        if ($line -match "\breason=(?<reason>\S+)") {
            Add-Count $parkByReason $Matches["reason"]
        }
        if ($line -like "*wakeResult=event*") {
            $counters["wubei.wait.wakeEvent"]++
        } elseif ($line -like "*wakeResult=timeout*") {
            $counters["wubei.wait.wakeTimeout"]++
        } elseif ($line -like "*wakeResult=none*") {
            $counters["wubei.wait.wakeNone"]++
        }
    }
    if ($line.Contains("[wubei wait] skip park; runtime already has wake state")) {
        $counters["wubei.wait.skipAlreadyReady"]++
    }
    if ($line.Contains("[wubei wait] skip park: no window runtime")) {
        $counters["wubei.wait.skipNoRuntime"]++
    }
    if ($line -like "*slow*" -and $line -like "*observer*") { $counters["slow.observer"]++ }
    if ($line -like "*timeout*" -or $line -like "*wokeByTimeout*") { $counters["timeout"]++ }

    foreach ($field in @(
            "waitMs",
            "afterReleaseMs",
            "elapsedMs",
            "preparedAgeMs",
            "verifiedAgeMs",
            "ageMs",
            "heldMs",
            "timeoutMs",
            "minParkMs",
            "requestAgeMs",
            "preparingAgeMs",
            "expiresInMs",
            "attentionDetectMs",
            "attentionRoutePrepareMs",
            "routePrepareMs",
            "taskTrackerPrepareMs",
            "focusTotal",
            "hwndCapture",
            "robotCapture",
            "captureFailure",
            "hwndKeyboardSuccess",
            "hwndKeyboardFailure")) {
        Add-NumericField $values $field $line
    }
}

$timingSummaries = [ordered]@{}
foreach ($key in ($values.Keys | Sort-Object)) {
    $timingSummaries[$key] = New-TimingSummary $values[$key]
}

$result = [ordered]@{
    logPath = (Resolve-Path -LiteralPath $LogPath).Path
    startTime = $(if ($StartTime) { $StartTime } else { $null })
    endTime = $(if ($EndTime) { $EndTime } else { $null })
    contains = $(if ($Contains) { $Contains } else { $null })
    totalLines = $totalLines
    matchedLines = $matchedLines
    counters = $counters
    transactionHandoffs = $transactionHandoffs
    transactionSameWindow = $transactionSameWindow
    transactionReleases = $transactionReleases
    parkByReason = $parkByReason
    readyAwaitByResult = $readyAwaitByResult
    timings = $timingSummaries
}

Write-Output "DHXY Wubei latency/log summary"
Write-Output ("logPath      : {0}" -f (Resolve-Path -LiteralPath $LogPath))
Write-Output ("startTime    : {0}" -f ($(if ($StartTime) { $StartTime } else { "<begin>" })))
Write-Output ("endTime      : {0}" -f ($(if ($EndTime) { $EndTime } else { "<end>" })))
Write-Output ("contains     : {0}" -f ($(if ($Contains) { $Contains } else { "<none>" })))
Write-Output ("totalLines   : {0}" -f $totalLines)
Write-Output ("matchedLines : {0}" -f $matchedLines)
Write-Output ""
Write-Output "Counters:"
foreach ($key in $counters.Keys) {
    Write-Output ("  {0,-36} {1,8}" -f $key, $counters[$key])
}
Write-Output ""
Write-Output "Top same-window reacquire transactions:"
foreach ($entry in ($transactionSameWindow.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 10)) {
    $total = 0
    if ($transactionHandoffs.ContainsKey($entry.Key)) {
        $total = $transactionHandoffs[$entry.Key]
    }
    Write-Output ("  {0,-42} same={1,8} total={2,8}" -f $entry.Key, $entry.Value, $total)
}
Write-Output ""
Write-Output "Wubei park by reason:"
foreach ($entry in ($parkByReason.GetEnumerator() | Sort-Object Value -Descending)) {
    Write-Output ("  {0,-42} {1,8}" -f $entry.Key, $entry.Value)
}
Write-Output ""
Write-Output "Window ready await by result:"
foreach ($entry in ($readyAwaitByResult.GetEnumerator() | Sort-Object Value -Descending)) {
    Write-Output ("  {0,-42} {1,8}" -f $entry.Key, $entry.Value)
}
Write-Output ""
Write-Output "Timing fields (ms):"
foreach ($key in ($values.Keys | Sort-Object)) {
    Write-Output ("  " + (Format-TimingSummary $key $values[$key]))
}
Write-Output ""
Write-Output "Pair/proxy coverage:"
Write-Output "  PATHING_TERMINAL publish -> task wake : use window.ready.await event/timeout plus wubei.wait park finished."
Write-Output "  prepared action publish -> consume    : use preparedAgeMs/ageMs from consumePrepared lines when present."
Write-Output "  consume -> input queued               : use [INPUT_TRACE] queued-action and input.request coverage; exact delta needs shared action id."
Write-Output "  input queued -> input completion      : use event=input.request elapsedMs from InputActionWorker."
Write-Output "  click/state transition                : current logs do not emit click.done/state.changed as stable events; script no longer treats them as coverage."

if (-not [string]::IsNullOrWhiteSpace($BaselineJson)) {
    if (-not (Test-Path -LiteralPath $BaselineJson)) {
        throw "Baseline JSON not found: $BaselineJson"
    }
    $baseline = Get-Content -LiteralPath $BaselineJson -Raw | ConvertFrom-Json
    Write-Output ""
    Write-Output "Comparison vs baseline:"
    foreach ($key in $counters.Keys) {
        $baseCounters = Get-ObjectValue $baseline "counters"
        $baseValue = Get-ObjectValue $baseCounters $key
        if ($null -ne $baseValue) {
            $delta = [int64]$counters[$key] - [int64]$baseValue
            Write-Output ("  counter {0,-34} baseline={1,8} current={2,8} delta={3,8}" -f `
                $key, $baseValue, $counters[$key], $delta)
        }
    }
    foreach ($key in @("waitMs", "afterReleaseMs", "heldMs", "preparedAgeMs", "verifiedAgeMs", "attentionDetectMs", "elapsedMs")) {
        $baseTimings = Get-ObjectValue $baseline "timings"
        $baseTiming = Get-ObjectValue $baseTimings $key
        $currentTiming = Get-ObjectValue $timingSummaries $key
        if ($null -ne $baseTiming -and $null -ne $currentTiming) {
            $baseP95 = Get-ObjectValue $baseTiming "p95"
            $currentP95 = Get-ObjectValue $currentTiming "p95"
            if ($null -ne $baseP95 -and $null -ne $currentP95) {
                $delta = [int64]$currentP95 - [int64]$baseP95
                Write-Output ("  p95 {0,-38} baseline={1,8} current={2,8} delta={3,8}" -f `
                    $key, $baseP95, $currentP95, $delta)
            }
        }
    }
}

if (-not [string]::IsNullOrWhiteSpace($JsonOut)) {
    $json = $result | ConvertTo-Json -Depth 8
    Set-Content -LiteralPath $JsonOut -Value $json -Encoding UTF8
    Write-Output ""
    Write-Output ("JSON written : {0}" -f (Resolve-Path -LiteralPath $JsonOut))
}
