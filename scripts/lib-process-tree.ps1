# 共享进程树收尾helper。两个启动脚本都 dot-source 它，避免各写一份产生漂移。

function Stop-ProcessTreeSafely {
    <#
    .SYNOPSIS
        杀掉一个进程连同它的全部后代，最深的先杀。

    .DESCRIPTION
        Windows 的 TerminateProcess（Stop-Process -Force 底层）只终止被点名的那一个进程，
        子进程会被重新挂载并继续存活。RapidOCR sidecar 用 multiprocessing 派生 worker，
        正常退出时靠 resource-tracker 握手让子进程自行结束——强杀跳过这整套收尾，
        worker 就永久卡在一个再也不会来数据的管道上。

        2026-08-21 取证：11 天里这样漏出 115 个孤儿 python，占 9.8GB 提交内存、
        3133 线程、45160 句柄，把整机提交量顶到上限的 97%，全系统发卡。

        PID 复用防护：只有创建时间不早于父进程的子进程才算它的后代。否则一个恰好
        复用了已死 PID 的无关新进程会被误杀。
    #>
    param(
        [Parameter(Mandatory = $true)][int]$ProcessId,
        [int]$MaxDepth = 6
    )

    $root = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue
    if ($null -eq $root) {
        return 0
    }

    $descendants = New-Object System.Collections.Generic.List[object]
    $frontier = @($root)
    for ($depth = 0; $depth -lt $MaxDepth -and $frontier.Count -gt 0; $depth++) {
        $next = @()
        foreach ($parent in $frontier) {
            $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $($parent.ProcessId)" -ErrorAction SilentlyContinue |
                Where-Object { $_.ProcessId -ne $parent.ProcessId -and $_.CreationDate -ge $parent.CreationDate }
            foreach ($child in $children) {
                $descendants.Add($child)
                $next += $child
            }
        }
        $frontier = $next
    }

    # 倒序 = 最深的先杀：先杀父再杀子等于把孙子辈重新变成孤儿。
    $killed = 0
    for ($index = $descendants.Count - 1; $index -ge 0; $index--) {
        try {
            Stop-Process -Id $descendants[$index].ProcessId -Force -Confirm:$false -ErrorAction Stop
            $killed++
        } catch { }
    }
    try {
        Stop-Process -Id $ProcessId -Force -Confirm:$false -ErrorAction Stop
        $killed++
    } catch { }

    if ($descendants.Count -gt 0) {
        Write-Host "[process-tree] 关闭 pid=$ProcessId 连同 $($descendants.Count) 个后代进程（共终止 $killed 个）"
    }
    return $killed
}

function Remove-OrphanedOcrWorkers {
    <#
    .SYNOPSIS
        清扫历史遗留的孤儿 RapidOCR multiprocessing worker。

    .DESCRIPTION
        树式收尾只能覆盖“由本脚本关闭”的那条路径。sidecar 自己崩溃、被任务管理器杀掉、
        或机器休眠导致父进程消失时，同样会留下孤儿。所以每次启动前扫一次：
        只清理父进程已死、且命令行确实是 multiprocessing.spawn 的 python，
        任何还有活父进程的 worker 一律不碰。
    #>
    $orphans = Get-CimInstance Win32_Process -Filter "Name='python.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -match 'multiprocessing\.spawn' -and
            ($null -eq (Get-Process -Id $_.ParentProcessId -ErrorAction SilentlyContinue))
        }
    if ($null -eq $orphans -or @($orphans).Count -eq 0) {
        return 0
    }
    $killed = 0
    foreach ($orphan in @($orphans)) {
        try {
            Stop-Process -Id $orphan.ProcessId -Force -Confirm:$false -ErrorAction Stop
            $killed++
        } catch { }
    }
    Write-Host "[process-tree] 清扫孤儿 OCR worker：$killed 个（父进程已死）"
    return $killed
}
