chcp 65001 | Out-Null
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$global:OutputEncoding = [System.Text.Encoding]::UTF8

$utf8Options = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
    $env:JAVA_TOOL_OPTIONS = $utf8Options
} elseif ($env:JAVA_TOOL_OPTIONS -notlike "*-Dfile.encoding=UTF-8*") {
    $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS $utf8Options"
}

Write-Host "DHXY dev shell encoding set to UTF-8"
