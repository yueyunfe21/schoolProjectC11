#requires -Version 7.3
# Operator-only registration. Creates configuration, NOT a running process or a public endpoint.
# One shared JAR/interpreter/script may be referenced by every independently owned instance.
param(
    [Parameter(Mandatory)][string]$RegistryRoot,
    [Parameter(Mandatory)][string]$SharedJar,
    [Parameter(Mandatory)][string]$Java,
    [Parameter(Mandatory)][string]$OcrPython,
    [Parameter(Mandatory)][string]$OcrScript,
    [Parameter(Mandatory)][ValidateRange(1024,65535)][int]$Port,
    [Parameter(Mandatory)][ValidateRange(1024,65535)][int]$OcrPort,
    [Parameter(Mandatory)][uri]$PublicOrigin
)
$ErrorActionPreference = 'Stop'
if ($Port -eq $OcrPort) { throw 'Brain and OCR ports must differ.' }
if ($PublicOrigin.Scheme -cne 'https' -or $PublicOrigin.UserInfo -or $PublicOrigin.Query -or
    $PublicOrigin.Fragment -or $PublicOrigin.AbsolutePath -ne '/' -or $PublicOrigin.Host -notmatch '^[a-zA-Z0-9.-]+$') {
    throw 'PublicOrigin must be an operator-owned HTTPS origin without path/query/credentials.'
}
$root = [IO.Path]::GetFullPath($RegistryRoot)
if ($root -eq [IO.Path]::GetPathRoot($root)) { throw 'Use a dedicated instance registry, never a drive root.' }
$files = @{}
foreach ($entry in @{sharedJar=$SharedJar; java=$Java; ocrPython=$OcrPython; ocrScript=$OcrScript}.GetEnumerator()) {
    $resolved = (Get-Item -LiteralPath $entry.Value -ErrorAction Stop).FullName
    if (-not (Test-Path -LiteralPath $resolved -PathType Leaf) -or $resolved -match '[\r\n]') { throw "Invalid program input: $($entry.Key)" }
    $files[$entry.Key] = $resolved.Replace('\','/')
}
New-Item -ItemType Directory -Path $root -Force | Out-Null
if (((Get-Item -LiteralPath $root).Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'Registry cannot be a link.' }
$registryLock = [IO.File]::Open((Join-Path $root 'registry.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
try {
    $count = 0
    # Bounded, one-level enumeration of this explicit registration directory, not a source/log scan.
    foreach ($directory in Get-ChildItem -LiteralPath $root -Directory) {
        if (++$count -gt 1000) { throw 'Registry exceeds this tool capacity; use a reviewed allocator.' }
        if ($directory.Name -notmatch '^[0-9a-f-]{36}$') { continue }
        $existing = Join-Path $directory.FullName 'instance.properties'
        if (Test-Path -LiteralPath $existing) {
            foreach ($line in Get-Content -LiteralPath $existing) {
                if ($line -match '^(?:port|ocrPort)=([0-9]+)$' -and [int]$Matches[1] -in @($Port,$OcrPort)) {
                    throw 'A registered instance already owns one of these ports.'
                }
            }
        }
    }
    $id = [Guid]::NewGuid().ToString()
    $instance = Join-Path $root $id
    New-Item -ItemType Directory -Path $instance | Out-Null
    # The registration contains bearer credentials. Restrict it before writing any secret.
    if ($IsWindows) {
        $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
        $acl = [Security.AccessControl.DirectorySecurity]::new()
        $acl.SetOwner($sid)
        $acl.SetAccessRuleProtection($true,$false)
        $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))
        Set-Acl -LiteralPath $instance -AclObject $acl
    } else {
        [IO.File]::SetUnixFileMode($instance, [IO.UnixFileMode]::UserRead -bor [IO.UnixFileMode]::UserWrite -bor [IO.UnixFileMode]::UserExecute)
    }
    $work = Join-Path $instance 'work'
    foreach ($name in @('config','logs','data','state','tmp','images')) { New-Item -ItemType Directory -Path (Join-Path $work $name) -Force | Out-Null }
    $random = [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
    $token = [Convert]::ToBase64String($random).TrimEnd('=').Replace('+','-').Replace('/','_')
    $properties = "clientId=$id`nport=$Port`ntoken=$token`nocrPort=$OcrPort`nocrPython=$($files.ocrPython)`nocrScript=$($files.ocrScript)`n"
    [IO.File]::WriteAllText((Join-Path $instance 'instance.properties'), $properties, [Text.UTF8Encoding]::new($false))
    $launch = @{schemaVersion=1; clientId=$id; sharedJar=$files.sharedJar; java=$files.java; programSha256=(Get-FileHash -LiteralPath $files.sharedJar -Algorithm SHA256).Hash.ToLowerInvariant()}
    [IO.File]::WriteAllText((Join-Path $instance 'launch.json'), ($launch | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
    $client = "cloud.turn.base-uri=$($PublicOrigin.GetLeftPart([UriPartial]::Authority))`ncloud.turn.device-id=$id`ncloud.turn.bearer-token=$token`ncloud.turn.sidecar.auto-start-enabled=false`nbot.run.auto-start=false`nbot.run.show-ui=true`n"
    [IO.File]::WriteAllText((Join-Path $instance 'client-connection.properties'), $client, [Text.UTF8Encoding]::new($false))
    $proxy = "# DRAFT only. Do not expose publicly until license/session admission is complete.`n$($PublicOrigin.Authority) {`n    reverse_proxy 127.0.0.1:$Port`n}`n"
    [IO.File]::WriteAllText((Join-Path $instance 'Caddyfile.draft'), $proxy, [Text.UTF8Encoding]::new($false))
    Write-Host "Registered client $id in $instance. No process started, no endpoint exposed. Connection file is confidential."
} finally { $registryLock.Dispose(); $token=$null; $properties=$null; $client=$null }
