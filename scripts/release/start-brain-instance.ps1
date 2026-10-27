#requires -Version 7.3
# Explicit operator invocation only. No compilation, copying of Cloud sources, or game input.
# Until license/session admission is complete this may only be used in a closed development network.
param([Parameter(Mandatory)][string]$InstanceDirectory)
$ErrorActionPreference = 'Stop'
$instance = (Get-Item -LiteralPath $InstanceDirectory -ErrorAction Stop).FullName
$configuration = Join-Path $instance 'instance.properties'
$launchPath = Join-Path $instance 'launch.json'
if ((Get-Item -LiteralPath $launchPath).Length -gt 16384) { throw 'Launch configuration too large.' }
$launch = Get-Content -LiteralPath $launchPath -Raw | ConvertFrom-Json
if ($launch.schemaVersion -ne 1 -or $launch.clientId -cne (Split-Path $instance -Leaf)) { throw 'Invalid instance launch identity.' }
if ((Get-FileHash -LiteralPath $launch.sharedJar -Algorithm SHA256).Hash.ToLowerInvariant() -cne $launch.programSha256) {
    throw 'Shared program bytes changed. Use a new immutable release path and update this instance intentionally.'
}
$work = Join-Path $instance 'work'
$temporary = Join-Path $work 'tmp'
$arguments = @("-Djava.io.tmpdir=$temporary", '-jar', [string]$launch.sharedJar, '--instance-config', $configuration)
if ($IsWindows) {
    $attempt = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N')
    # Start-Process requires Windows command-line quoting; no token or license appears in argv.
    $quoted = $arguments | ForEach-Object { if ($_ -match '"|[\r\n]') { throw 'Invalid command argument.' }; '"' + $_ + '"' }
    $process = Start-Process -FilePath $launch.java -ArgumentList $quoted -WorkingDirectory $work -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $work "logs/brain-$attempt.out.log") -RedirectStandardError (Join-Path $work "logs/brain-$attempt.err.log")
    Write-Host "Brain process created for $($launch.clientId), PID=$($process.Id). This is not a readiness or license-acceptance result."
} else {
    # Foreground lifetime works with a service manager; Java owns the persistent instance lock.
    Push-Location -LiteralPath $work
    try { & $launch.java @arguments; if ($LASTEXITCODE -ne 0) { throw "Brain exited with code $LASTEXITCODE" } }
    finally { Pop-Location }
}
