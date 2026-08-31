param(
    [string]$LogPath,
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogPath)) {
    $LogPath = Join-Path $repoRoot 'logs\dhxy-console.log'
}
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repoRoot 'artifacts\g135-fengyaofu-20260831-162041'
}

$windowId = 'hwnd-6F098A'
$windowLeft = 1399
$windowTop = 4
$start = [datetime]'2026-08-31 16:20:41.000'
$end = [datetime]'2026-08-31 16:31:39.999'

$originalDir = Join-Path $OutputPath 'original'
$markedDir = Join-Path $OutputPath 'marked'
New-Item -ItemType Directory -Force -Path $OutputPath, $originalDir, $markedDir | Out-Null

function Parse-Time([string]$line) {
    if ($line -match '^(?<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})') {
        return [datetime]::ParseExact($Matches.time, 'yyyy-MM-dd HH:mm:ss.fff', [Globalization.CultureInfo]::InvariantCulture)
    }
    return $null
}

function In-Run([string]$line) {
    $time = Parse-Time $line
    return $null -ne $time -and $time -ge $start -and $time -le $end
}

function Draw-Cross([System.Drawing.Graphics]$graphics, [int]$x, [int]$y, [System.Drawing.Color]$color) {
    $pen = New-Object System.Drawing.Pen($color, 4)
    try {
        $graphics.DrawLine($pen, $x - 12, $y, $x + 12, $y)
        $graphics.DrawLine($pen, $x, $y - 12, $x, $y + 12)
        $graphics.DrawEllipse($pen, $x - 7, $y - 7, 14, 14)
    } finally {
        $pen.Dispose()
    }
}

function Save-MarkedImage(
    [string]$source,
    [string]$destination,
    [string]$title,
    [Nullable[int]]$clickX,
    [Nullable[int]]$clickY,
    [string]$note,
    [Nullable[int]]$boxX,
    [Nullable[int]]$boxY,
    [Nullable[int]]$boxWidth,
    [Nullable[int]]$boxHeight
) {
    $input = [System.Drawing.Bitmap]::FromFile($source)
    $headerHeight = 78
    $canvasWidth = [Math]::Max($input.Width, 720)
    $output = New-Object System.Drawing.Bitmap($canvasWidth, ($input.Height + $headerHeight))
    $graphics = [System.Drawing.Graphics]::FromImage($output)
    $font = New-Object System.Drawing.Font('Microsoft YaHei UI', 12, [System.Drawing.FontStyle]::Bold)
    $smallFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 9, [System.Drawing.FontStyle]::Regular)
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $grayBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(225, 225, 225))
    $cyanPen = New-Object System.Drawing.Pen([System.Drawing.Color]::Cyan, 3)
    try {
        $graphics.Clear([System.Drawing.Color]::FromArgb(25, 25, 25))
        $graphics.DrawString($title, $font, $whiteBrush, 8, 7)
        $graphics.DrawString($note, $smallFont, $grayBrush, 8, 38)
        $graphics.DrawImage($input, 0, $headerHeight, $input.Width, $input.Height)
        if ($null -ne $boxX -and $null -ne $boxY -and $null -ne $boxWidth -and $null -ne $boxHeight) {
            $graphics.DrawRectangle($cyanPen, $boxX.Value, $boxY.Value + $headerHeight, $boxWidth.Value, $boxHeight.Value)
        }
        if ($null -ne $clickX -and $null -ne $clickY) {
            Draw-Cross $graphics $clickX.Value ($clickY.Value + $headerHeight) ([System.Drawing.Color]::Red)
        }
        $output.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $cyanPen.Dispose()
        $grayBrush.Dispose()
        $whiteBrush.Dispose()
        $smallFont.Dispose()
        $font.Dispose()
        $graphics.Dispose()
        $output.Dispose()
        $input.Dispose()
    }
}

function Get-ImageMeaning([System.IO.FileInfo]$file) {
    $path = $file.FullName.Replace('/', '\')
    $name = $file.Name
    if ($path -like '*tianting-dialog-duoxie*PRESENT*') { return '16:20:41 多谢选项命中证据；红点是实际点击' }
    if ($path -like '*tianting-dialog-duoxie*ABSENT*' -or ($path -like '*tianting-dialog-duoxie*latest*')) { return '点击多谢后的过渡帧；模板已不在' }
    if ($path -like '*tianting-dialog-fengyao-score*') { return '16:20:42 封妖符低分候选证据；不是最终点击帧' }
    if ($path -like '*tianting-dialog-fengyao*162042*') { return '16:20:42 封妖符选项尚未出现' }
    if ($path -like '*tianting-dialog-fengyao*162050*') { return '16:20:50 使用封妖符命中证据；红点是实际点击' }
    if ($path -like '*tianting-dialog-fengyao*162409*' -or ($path -like '*tianting-dialog-fengyao*latest*')) { return '16:24:09 第三坐标首次点击前的大对话框探测；红点是随后实际坐标点击' }
    if ($name -eq 'latest_vision.png') { return '16:23:47 战斗中的整窗视觉帧；青框是故事框，红点为战后实际关闭位置' }
    if ($name -eq 'ui_cleanup_cached_roi_scan.png') { return '16:23:47 UI 清理缓存裁图；本帧没有同步点击' }
    if ($path -like '*match-miss\tracker-anchor*') { return '任务追踪器遮罩证据；红点为该阶段关联的实际点击位置' }
    if ($name -like 'dialog_detect_*') { return 'Dialog 绿色像素分割掩码，不是整窗原图；本帧没有点击坐标' }
    return '本轮磁盘留存图'
}

$lines = Get-Content -LiteralPath $LogPath
$runLines = $lines | Where-Object { $_ -like "*$windowId*" -and (In-Run $_) }

$captureRows = New-Object System.Collections.Generic.List[object]
foreach ($line in $runLines) {
    if ($line -match 'completed actionId=(?<actionId>[0-9a-f-]+).*frameEvidence=TurnFrameMetadata\[purpose=(?<purpose>[^,]+), contentType=(?<contentType>[^,]+), sha256=(?<sha>[0-9a-f]+), width=(?<width>\d+), height=(?<height>\d+), region=TurnRegion\[x=(?<x>-?\d+), y=(?<y>-?\d+), width=(?<regionWidth>\d+), height=(?<regionHeight>\d+)\], sourceStepIndex=(?<sourceStepIndex>[^\]]+)\]') {
        $time = Parse-Time $line
        $captureRows.Add([pscustomobject]@{
            Time = $time.ToString('yyyy-MM-dd HH:mm:ss.fff')
            ActionId = $Matches.actionId
            Purpose = $Matches.purpose
            Sha256 = $Matches.sha
            Width = [int]$Matches.width
            Height = [int]$Matches.height
            ScreenX = [int]$Matches.x
            ScreenY = [int]$Matches.y
            RegionWidth = [int]$Matches.regionWidth
            RegionHeight = [int]$Matches.regionHeight
            SourceStepIndex = $Matches.sourceStepIndex
            PixelRetention = 'MEMORY_ONLY_NOT_PERSISTED_BY_TURN_PROTOCOL'
        })
    }
}
$captureRows | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath (Join-Path $OutputPath 'all-capture-frames.csv')

$actionRows = New-Object System.Collections.Generic.List[object]
foreach ($line in $runLines) {
    if ($line -match 'received actionId=(?<actionId>[0-9a-f-]+).*steps=\[(?<steps>.*)\]$') {
        $steps = $Matches.steps
        if ($steps -match 'CLICK|KEYBOARD|MOVE_MOUSE') {
            $time = Parse-Time $line
            $clicks = [regex]::Matches($steps, 'CLICK_LEFT@(?<x>\d+),(?<y>\d+)')
            if ($clicks.Count -eq 0 -and $steps -match 'KEYBOARD') {
                $actionRows.Add([pscustomobject]@{
                    Time = $time.ToString('yyyy-MM-dd HH:mm:ss.fff'); ActionId = $Matches.actionId
                    Kind = 'KEYBOARD'; ScreenX = ''; ScreenY = ''; WindowX = ''; WindowY = ''; Steps = $steps
                })
            } else {
                foreach ($click in $clicks) {
                    $screenX = [int]$click.Groups['x'].Value
                    $screenY = [int]$click.Groups['y'].Value
                    $actionRows.Add([pscustomobject]@{
                        Time = $time.ToString('yyyy-MM-dd HH:mm:ss.fff'); ActionId = $Matches.actionId
                        Kind = 'CLICK_LEFT'; ScreenX = $screenX; ScreenY = $screenY
                        WindowX = $screenX - $windowLeft; WindowY = $screenY - $windowTop; Steps = $steps
                    })
                }
            }
        }
    }
}
foreach ($line in $runLines) {
    if ($line -match 'queued-action request=(?<request>[^ ]+).*action=InputAction\{type=CLICK_LEFT, x=(?<x>\d+), y=(?<y>\d+)') {
        $time = Parse-Time $line
        $screenX = [int]$Matches.x
        $screenY = [int]$Matches.y
        if (-not ($actionRows | Where-Object { $_.Time -eq $time.ToString('yyyy-MM-dd HH:mm:ss.fff') -and $_.ScreenX -eq $screenX -and $_.ScreenY -eq $screenY })) {
            $actionRows.Add([pscustomobject]@{
                Time = $time.ToString('yyyy-MM-dd HH:mm:ss.fff'); ActionId = ''; Kind = 'CLICK_LEFT_QUEUED'
                ScreenX = $screenX; ScreenY = $screenY; WindowX = $screenX - $windowLeft; WindowY = $screenY - $windowTop
                Steps = $Matches.request
            })
        }
    }
}
$actionRows | Sort-Object Time | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath (Join-Path $OutputPath 'all-input-actions.csv')

$cycles = @{}
foreach ($line in $lines) {
    if (-not (In-Run $line)) { continue }
    if ($line -match 'coordinate 2 clicked at .*intentId=(?<intent>[0-9a-f-]+)') {
        $intent = $Matches.intent
        $cycles[$intent] = [ordered]@{IntentId=$intent; ClickTime=(Parse-Time $line).ToString('yyyy-MM-dd HH:mm:ss.fff'); StableTime=''; StableValue=''; TerminalTime=''; MovementObserved=''; RetryTime=''; StoryClosed=''}
    } elseif ($line -match 'Value stability ENTERED: windowId=hwnd-6F098A intentId=(?<intent>[0-9a-f-]+) value=\((?<value>[^\)]+)\)') {
        $intent = $Matches.intent
        if ($cycles.ContainsKey($intent)) { $cycles[$intent].StableTime=(Parse-Time $line).ToString('yyyy-MM-dd HH:mm:ss.fff'); $cycles[$intent].StableValue=$Matches.value }
    } elseif ($line -match 'coordinate terminal latched: index=2 intentId=(?<intent>[0-9a-f-]+).*coordinateMovementObserved=(?<movement>true|false)') {
        $intent = $Matches.intent
        if ($cycles.ContainsKey($intent)) { $cycles[$intent].TerminalTime=(Parse-Time $line).ToString('yyyy-MM-dd HH:mm:ss.fff'); $cycles[$intent].MovementObserved=$Matches.movement }
    } elseif ($line -match 'coordinate 2 exact terminal had no movement proof; intentId=(?<intent>[0-9a-f-]+) storyClosed=(?<story>true|false)') {
        $intent = $Matches.intent
        if ($cycles.ContainsKey($intent)) { $cycles[$intent].RetryTime=(Parse-Time $line).ToString('yyyy-MM-dd HH:mm:ss.fff'); $cycles[$intent].StoryClosed=$Matches.story }
    }
}
$cycleRows = $cycles.Values | ForEach-Object { [pscustomobject]$_ } | Sort-Object ClickTime
$cycleRows | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath (Join-Path $OutputPath 'coordinate2-cycles.csv')

$persistedFiles = New-Object System.Collections.Generic.List[System.IO.FileInfo]
$smallDirs = @(
    (Join-Path $repoRoot 'images\temp\match-evidence\tianting-dialog-duoxie'),
    (Join-Path $repoRoot 'images\temp\match-evidence\tianting-dialog-fengyao'),
    (Join-Path $repoRoot 'images\temp\match-evidence\tianting-dialog-fengyao-score'),
    (Join-Path $repoRoot 'images\temp\match-miss\tracker-anchor'),
    (Join-Path $repoRoot 'images\temp\hwnd-6F098A')
)
foreach ($dir in $smallDirs) {
    if (Test-Path -LiteralPath $dir) {
        foreach ($file in Get-ChildItem -LiteralPath $dir -File) {
            if ($file.LastWriteTime -ge $start.AddMinutes(-1) -and $file.LastWriteTime -le $end.AddMinutes(1)) { $persistedFiles.Add($file) }
        }
    }
}
foreach ($file in Get-ChildItem -LiteralPath (Join-Path $repoRoot 'images\temp') -File -Filter 'dialog_detect_*') {
    if ($file.LastWriteTime -ge $start.AddMinutes(-1) -and $file.LastWriteTime -le $end.AddMinutes(1)) { $persistedFiles.Add($file) }
}

$manifest = New-Object System.Collections.Generic.List[object]
$sequence = 0
foreach ($file in $persistedFiles | Sort-Object LastWriteTime, FullName) {
    $sequence++
    $safeName = ('{0:D3}-{1}' -f $sequence, $file.Name)
    $copyPath = Join-Path $originalDir $safeName
    Copy-Item -LiteralPath $file.FullName -Destination $copyPath -Force
    $meaning = Get-ImageMeaning $file
    $clickX = $null; $clickY = $null; $boxX = $null; $boxY = $null; $boxWidth = $null; $boxHeight = $null
    $path = $file.FullName.Replace('/', '\')
    if ($path -like '*tianting-dialog-duoxie*PRESENT*') { $clickX=83; $clickY=137 }
    elseif ($path -like '*tianting-dialog-fengyao*162050*') { $clickX=103; $clickY=137 }
    elseif ($path -like '*tianting-dialog-fengyao*162409*' -or ($path -like '*tianting-dialog-fengyao*latest*')) { $clickX=254; $clickY=267 }
    elseif ($file.Name -eq 'latest_vision.png') { $clickX=742; $clickY=466; $boxX=251; $boxY=346; $boxWidth=533; $boxHeight=144 }
    elseif ($path -like '*match-miss\tracker-anchor*') {
        if ($file.Name -match '^(04|05)-') { $clickX=523; $clickY=271 }
        elseif ($file.Name -match '^(06|07)-') { $clickX=741; $clickY=471 }
        else { $clickX=454; $clickY=517 }
    }
    $markedPath = Join-Path $markedDir $safeName
    Save-MarkedImage $copyPath $markedPath ('G135 #' + $sequence + '  ' + $file.LastWriteTime.ToString('HH:mm:ss.fff')) $clickX $clickY $meaning $boxX $boxY $boxWidth $boxHeight
    $image = [System.Drawing.Image]::FromFile($copyPath)
    try { $width=$image.Width; $height=$image.Height } finally { $image.Dispose() }
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $copyPath).Hash.ToLowerInvariant()
    $manifest.Add([pscustomobject]@{
        Sequence=$sequence; Time=$file.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss.fff'); Width=$width; Height=$height
        Sha256=$hash; Meaning=$meaning; Source=$file.FullName; Original=('original/' + $safeName); Marked=('marked/' + $safeName)
    })
}
$manifest | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath (Join-Path $OutputPath 'persisted-image-manifest.csv')

$map = New-Object System.Drawing.Bitmap(1024, 846)
$mapGraphics = [System.Drawing.Graphics]::FromImage($map)
$mapFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 12, [System.Drawing.FontStyle]::Bold)
$mapSmall = New-Object System.Drawing.Font('Microsoft YaHei UI', 10, [System.Drawing.FontStyle]::Regular)
$mapWhite = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$mapGray = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220,220,220))
try {
    $mapGraphics.Clear([System.Drawing.Color]::FromArgb(32, 36, 45))
    $mapGraphics.DrawString('G135 点击坐标总图（坐标示意，不是现场截图）', $mapFont, $mapWhite, 12, 10)
    $mapGraphics.DrawString('红点均来自本轮输入日志；坐标为 1024x768 游戏窗口内坐标。', $mapSmall, $mapGray, 12, 42)
    $points = @(
        @{X=283;Y=387;Text='多谢'}, @{X=303;Y=387;Text='使用封妖符'}, @{X=94;Y=237;Text='追踪器重开'},
        @{X=454;Y=383;Text='坐标0'}, @{X=584;Y=383;Text='坐标1'}, @{X=454;Y=517;Text='坐标2 ×85'},
        @{X=523;Y=271;Text='目标0'}, @{X=741;Y=471;Text='目标1'}, @{X=748;Y=468;Text='故事关闭0'}, @{X=742;Y=466;Text='战后关闭'}
    )
    foreach ($point in $points) {
        Draw-Cross $mapGraphics $point.X ($point.Y + 78) ([System.Drawing.Color]::Red)
        $mapGraphics.DrawString($point.Text + " ($($point.X),$($point.Y))", $mapSmall, $mapWhite, $point.X + 10, $point.Y + 82)
    }
    $map.Save((Join-Path $markedDir '000-click-coordinate-map.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $mapGray.Dispose(); $mapWhite.Dispose(); $mapSmall.Dispose(); $mapFont.Dispose(); $mapGraphics.Dispose(); $map.Dispose()
}

$timeline = @'
# G135 油壶壶本次天庭「风妖府 / 封妖符」逐帧取证

## 现场范围

- 角色：`油壶壶`
- 窗口：`hwnd-6F098A`，native hwnd `7276938`
- 任务 run：`remote-turn-3aec3a77-880d-4088-8bd3-189d403d9b5d`
- 区间：`2026-08-31 16:20:41.000`—`16:31:39.999`
- 窗口屏幕原点：`(1399,4)`，窗口大小 `1024×768`

## 结论

这次不是没有点。第三坐标从 `16:24:09.332` 到 `16:31:36.101` 共点击 **85 次**，每次都是屏幕 `(1853,521)`、窗口内 `(454,517)`。每次点击后坐标仍稳定在 `(103,80)`；旧逻辑把“已经在目的地，所以没有产生位移”误判为“点击没有生效”，没有继续执行 `Alt+A`，而是重新打开同一框并再次点击第三坐标，于是形成死循环。`16:31:39` 的全局暂停结束了本轮。

## 完整关键时间线

| 时间 | 动作 / 判据 | 屏幕坐标 | 窗口内坐标 | 结果 |
|---|---|---:|---:|---|
| 16:20:41.506 | 点击 `多谢` | (1682,391) | (283,387) | 选项已送达 |
| 16:20:49.963 | 点击任务追踪器重开对话 | (2135,474) | (736,470) | 出现封妖符对话 |
| 16:20:51.168 | 点击 `使用封妖符` | (1702,391) | (303,387) | 进入风妖府坐标流程 |
| 16:20:54.865 | 点击坐标 0 | (1853,387) | (454,383) | 发生移动，终态接受 |
| 16:21:10.859 | 点击妖王目标 0 | (1922,275) | (523,271) | 未进战斗 |
| 16:21:13.782 | 关闭故事框 | (2147,472) | (748,468) | 继续坐标 1 |
| 16:21:16.377 | 点击坐标 1 | (1983,387) | (584,383) | 移动到 `(103,80)` |
| 16:21:34.496 | 点击妖王目标 1 | (2140,475) | (741,471) | 进入战斗 |
| 16:23:57.625 | 战斗结束 | — | — | 等待战后故事框 |
| 16:24:05.916 | 关闭战后故事框 | (2141,470) | (742,466) | 坐标 1 完成 |
| 16:24:07.706 | 点击追踪器重开坐标框 | (1493,241) | (94,237) | 第一次重开 |
| 16:24:08.563 | 再点追踪器 | (1493,241) | (94,237) | 封妖符锚点命中 1.0 |
| 16:24:09.332 | 第一次点击坐标 2 | (1853,521) | (454,517) | 坐标仍为 `(103,80)` |
| 16:24:13.602 | 第一次错误重试 | — | — | `movement=false` 被当成失败 |
| 16:24:14—16:31:36 | 同一点重复点击 | (1853,521) | (454,517) | 合计 85 次，始终未执行 Alt+A |
| 16:31:38.622 | 最后一次重试判定 | — | — | 仍回到同坐标 |
| 16:31:39.379 | 全局暂停 | — | — | 五个窗口一并停止 |

## 文件说明

- `all-capture-frames.csv`：本轮 turn 协议记录到的每一次 CAPTURE，含时间、区域、尺寸、SHA-256。协议只把 PNG 放在内存里，日志只保存元数据，因此这些像素多数已不可恢复，表中明确标为 `MEMORY_ONLY_NOT_PERSISTED_BY_TURN_PROTOCOL`。
- `all-input-actions.csv`：本轮全部有坐标的点击以及键盘输入动作。
- `coordinate2-cycles.csv`：第三坐标 85 轮逐轮明细，每轮含 intent、点击时间、稳定坐标、终态、重试时间。
- `persisted-image-manifest.csv`：本轮仍留在磁盘上的每一张图，逐张列出原路径、尺寸、哈希、含义、原图副本和标注图。
- `original/`：不改像素的原图副本。
- `marked/`：与 `original/` 一一对应的标注图。红十字是日志中的实际点击；青框是结构区域；没有同步点击的掩码图只加说明，不伪造点击。
- `marked/000-click-coordinate-map.png`：所有关键点击的窗口内坐标总图；它是示意图，不是现场截图。

## 证据保留边界

“每一张图片”分成两类：

1. **当时落盘的图片**：全部复制并逐张标注，见 `persisted-image-manifest.csv` 与 `marked/`。
2. **只经 turn 协议在内存中传输的图片**：完整逐帧元数据全部列在 `all-capture-frames.csv`，但原 PNG 没有落盘，进程结束后无法从 SHA 反推像素。报告不会用相邻帧冒充它们。
'@
Set-Content -LiteralPath (Join-Path $OutputPath 'README.md') -Value $timeline -Encoding UTF8

$htmlRows = foreach ($row in $manifest) {
    '<section><h3>#{0} · {1}</h3><p>{2}</p><a href="{3}"><img src="{3}" loading="lazy"></a><details><summary>原图</summary><img src="{4}" loading="lazy"></details></section>' -f $row.Sequence, $row.Time, [System.Net.WebUtility]::HtmlEncode($row.Meaning), $row.Marked.Replace('\','/'), $row.Original.Replace('\','/')
}
$html = @"
<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><title>G135 油壶壶风妖府逐帧证据</title>
<style>body{font-family:'Microsoft YaHei UI',sans-serif;background:#111827;color:#e5e7eb;max-width:1200px;margin:auto;padding:24px}a{color:#67e8f9}section{border:1px solid #374151;border-radius:10px;padding:14px;margin:18px 0;background:#1f2937}img{max-width:100%;border:1px solid #6b7280}code{color:#fbbf24}</style></head><body>
<h1>G135 油壶壶风妖府逐帧证据</h1><p>红十字=日志中的实际点击；青框=结构区域。先看 <a href="README.md">完整时间线</a>、<a href="coordinate2-cycles.csv">85轮循环</a>、<a href="all-capture-frames.csv">全部抓帧</a>。</p>
<section><h2>点击坐标总图（示意图）</h2><img src="marked/000-click-coordinate-map.png"></section>
$($htmlRows -join "`n")
</body></html>
"@
Set-Content -LiteralPath (Join-Path $OutputPath 'index.html') -Value $html -Encoding UTF8

[pscustomobject]@{
    OutputPath = $OutputPath
    CaptureFrames = $captureRows.Count
    InputActions = $actionRows.Count
    Coordinate2Cycles = @($cycleRows).Count
    PersistedImages = $manifest.Count
} | Format-List
