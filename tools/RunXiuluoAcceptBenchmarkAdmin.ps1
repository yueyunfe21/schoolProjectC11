$ErrorActionPreference = "Continue"
$workdir = "D:\mavenProject\DHXY"
$log = Join-Path $workdir "logs\xiuluo-accept-admin-run.log"

Set-Location $workdir
"[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')] admin runner script started cwd=$workdir user=$([Environment]::UserName)" | Out-File -FilePath $log -Encoding UTF8

try {
    $cp = Get-Content "target\classpath.txt" -Raw
    & java "-Dspring.main.banner-mode=off" -cp "target\classes;.codex-tools-classes;$cp" XiuluoAcceptBenchmarkRunner *>> $log
    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')] admin runner script finished exit=$LASTEXITCODE" | Add-Content -Path $log -Encoding UTF8
} catch {
    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')] admin runner script failed: $($_.Exception.Message)" | Add-Content -Path $log -Encoding UTF8
    throw
}
