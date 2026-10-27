# Stable packaged entry. Development stack launcher is separate; never starts Cloud/game tasks.
param([switch]$UpdateOnly)
$ErrorActionPreference = 'Stop'
$installRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$session = [IO.File]::Open((Join-Path $installRoot 'client-session.lock'), 'OpenOrCreate', 'ReadWrite', 'ReadWrite')
$locked = $false
$captureProcess = $null
try {
    # Interoperable byte-range lock: the Java installer locks this same file before any switch.
    $session.Lock(0, 1)
    $locked = $true
    $clientRoot = $installRoot
    $pointerPath = Join-Path $installRoot 'current-version.json'
    if (Test-Path -LiteralPath $pointerPath) {
        if ((Get-Item -LiteralPath $pointerPath).Length -gt 4096) { throw 'Invalid version pointer.' }
        $pointer = Get-Content -LiteralPath $pointerPath -Raw | ConvertFrom-Json
        $relative = [string]$pointer.application
        if ($pointer.schemaVersion -ne 1 -or ($relative -ne '.' -and
            $relative -notmatch '^versions/client-[0-9]+\.[0-9]+\.[0-9]+-b[0-9]+\.[0-9]+-[0-9a-f-]{36}/DHXY$')) {
            throw 'Invalid active application path.'
        }
        $clientRoot = [IO.Path]::GetFullPath((Join-Path $installRoot $relative))
    }
    # Reject reparse points at every version-directory component.
    $component = Get-Item -LiteralPath $clientRoot
    while ($null -ne $component -and $component.FullName.Length -ge $installRoot.Length) {
        if (($component.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'Linked installation paths are not supported.' }
        $component = $component.Parent
    }
    $configFile = Join-Path $clientRoot 'config/application.properties'
    $configText = ''
    if (Test-Path -LiteralPath $configFile -PathType Leaf) { $configText = Get-Content -LiteralPath $configFile -Raw }
    # Missing brain setup must not prevent installing/updating the desktop UI. This mode
    # carries no runtime authority, never starts capture and never supplies a fake token.
    $updateOnlyMode = $UpdateOnly -or
        $configText -notmatch '(?m)^cloud\.turn\.base-uri=https?://[^\r\n]+' -or
        $configText -notmatch '(?m)^cloud\.turn\.bearer-token=\S+' -or
        $configText -notmatch '(?m)^cloud\.turn\.device-id=[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\r?$' -or
        $configText -match 'YOUR-CLOUD-HOST|YOUR-USER-TOKEN|YOUR-CLIENT-UUID'
    if ($updateOnlyMode) { Write-Host 'Update UI only: Cloud is not configured; game tasks, capture and input are disabled.' }
    $logDir = Join-Path $clientRoot 'logs'
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    if (-not $updateOnlyMode) {
    $probe = [Net.Sockets.TcpClient]::new()
    try { $captureOnline = $probe.ConnectAsync('127.0.0.1', 47831).Wait(500) -and $probe.Connected }
    catch { $captureOnline = $false }
    finally { $probe.Dispose() }
    if (-not $captureOnline) {
        $captureProcess = Start-Process -FilePath (Join-Path $clientRoot 'capture/dhxy-capture.exe') `
            -WorkingDirectory $clientRoot -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logDir 'capture.out.log') `
            -RedirectStandardError (Join-Path $logDir 'capture.err.log')
    }
    }
    # Start-Process joins arguments into a Windows command line; quote path-valued options.
    $clientArguments = @('--cloud.turn.sidecar.auto-start-enabled=false','--bot.run.auto-start=false','--bot.run.show-ui=true',
        ('"--dhxy.install-root=' + $installRoot + '"'), ('"--dhxy.application-root=' + $clientRoot + '"'))
    $clientArguments += ('--dhxy.update-only=' + $updateOnlyMode.ToString().ToLowerInvariant())
    if ($updateOnlyMode) {
        # Override incomplete external examples; inert origin is never contacted in this mode.
        $clientArguments += @('--cloud.turn.base-uri=https://unconfigured.invalid', '--cloud.turn.bearer-token=',
            '--cloud.turn.device-id=unconfigured')
    }
    $client = Start-Process -FilePath (Join-Path $clientRoot 'DHXY.exe') -WorkingDirectory $clientRoot `
        -ArgumentList $clientArguments -WindowStyle Hidden -PassThru
    $client.WaitForExit()
    if ($client.ExitCode -ne 0) { throw "Client exited with code $($client.ExitCode); inspect logs." }
} finally {
    if ($null -ne $captureProcess -and -not $captureProcess.HasExited) {
        $captureProcess.Kill()
        $captureProcess.WaitForExit()
    }
    if ($locked) { $session.Unlock(0, 1) }
    $session.Dispose()
}
