param(
    [int]$Port = 18080,
    [string]$Path = "/api/cloud/decision",
    [string]$Token = "local-dev-token",
    [string]$BrainProjectPath = "D:\mavenProject\dhxy-cloud-brain",
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

if ($Path -ne "/api/cloud/decision") {
    throw "External dhxy-cloud-brain currently serves /api/cloud/decision; unsupported Path=$Path"
}

if (-not (Test-Path -LiteralPath $BrainProjectPath -PathType Container)) {
    throw "External dhxy-cloud-brain project not found: $BrainProjectPath"
}

$classesPath = Join-Path $BrainProjectPath "target\classes"
$classpathFile = Join-Path $BrainProjectPath "target\dependency-classpath.txt"
$mainClassName = "com.yueyunfe.dhxy.cloudbrain.CloudBrainApplication"
$mainClassFile = Join-Path $classesPath "com\yueyunfe\dhxy\cloudbrain\CloudBrainApplication.class"

function Get-NewestFileTimeUtc {
    param(
        [string[]]$Paths,
        [string[]]$IgnoredPathPrefixes = @()
    )

    $latest = [DateTime]::MinValue.ToUniversalTime()
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }
        $item = Get-Item -LiteralPath $path
        if (-not $item.PSIsContainer) {
            if ($item.LastWriteTimeUtc -gt $latest) {
                $latest = $item.LastWriteTimeUtc
            }
            continue
        }
        Get-ChildItem -LiteralPath $path -Recurse -File -Force | ForEach-Object {
            $ignored = $false
            foreach ($ignoredPrefix in $IgnoredPathPrefixes) {
                if ($_.FullName.StartsWith($ignoredPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
                    $ignored = $true
                    break
                }
            }
            if ($ignored) {
                return
            }
            if ($_.LastWriteTimeUtc -gt $latest) {
                $latest = $_.LastWriteTimeUtc
            }
        }
    }
    return $latest
}

function Format-Utc {
    param([DateTime]$Value)
    if ($Value -eq [DateTime]::MinValue.ToUniversalTime()) {
        return "missing"
    }
    return $Value.ToString("o")
}

function Get-CloudBrainRuntimeState {
    param([string]$ProjectPath)

    $sourcePaths = @(
        (Join-Path $ProjectPath "pom.xml"),
        (Join-Path $ProjectPath "src\main")
    )
    # Screenshots used only by manual cloud-brain experiments are not runtime inputs and Maven does not compile them.
    $sourceIgnoredPaths = @(
        (Join-Path $ProjectPath "src\main\java\com\yueyunfe\dhxy\cloudbrain\test")
    )
    $sourceMaxUtc = Get-NewestFileTimeUtc -Paths $sourcePaths -IgnoredPathPrefixes $sourceIgnoredPaths
    $classesMaxUtc = Get-NewestFileTimeUtc -Paths @($classesPath)
    $classpathUtc = if (Test-Path -LiteralPath $classpathFile -PathType Leaf) {
        (Get-Item -LiteralPath $classpathFile).LastWriteTimeUtc
    } else {
        [DateTime]::MinValue.ToUniversalTime()
    }
    $classpathSha256 = if (Test-Path -LiteralPath $classpathFile -PathType Leaf) {
        (Get-FileHash -LiteralPath $classpathFile -Algorithm SHA256).Hash.ToLowerInvariant()
    } else {
        ""
    }
    $pomUtc = (Get-Item -LiteralPath (Join-Path $ProjectPath "pom.xml")).LastWriteTimeUtc

    [pscustomobject]@{
        SourceMaxUtc = $sourceMaxUtc
        ClassesMaxUtc = $classesMaxUtc
        ClasspathUtc = $classpathUtc
        ClasspathSha256 = $classpathSha256
        PomUtc = $pomUtc
        MainClassPresent = Test-Path -LiteralPath $mainClassFile -PathType Leaf
        ClasspathPresent = Test-Path -LiteralPath $classpathFile -PathType Leaf
    }
}

function Test-CloudBrainClassesStale {
    param($State)

    if (-not $State.MainClassPresent) {
        Write-Host "Cloud brain classes are missing; main class not found: $mainClassFile"
        return $true
    }
    if (-not $State.ClasspathPresent) {
        Write-Host "Cloud brain dependency classpath file is missing: $classpathFile"
        return $true
    }
    if ($State.SourceMaxUtc -gt $State.ClassesMaxUtc) {
        Write-Host "Cloud brain classes are stale; sourceMaxUtc=$(Format-Utc $State.SourceMaxUtc) classesMaxUtc=$(Format-Utc $State.ClassesMaxUtc)"
        return $true
    }
    if ($State.PomUtc -gt $State.ClasspathUtc) {
        Write-Host "Cloud brain dependency classpath is stale; pomUtc=$(Format-Utc $State.PomUtc) classpathUtc=$(Format-Utc $State.ClasspathUtc)"
        return $true
    }
    return $false
}

function Invoke-CloudBrainClasspathPrepare {
    Push-Location $BrainProjectPath
    try {
        & mvn -q compile dependency:build-classpath "-Dmdep.includeScope=runtime" "-Dmdep.outputFile=$classpathFile" "-Dmdep.pathSeparator=;"
        $prepareExitCode = $LASTEXITCODE
        if ($prepareExitCode -ne 0) {
            throw "Cloud brain classpath prepare failed with exit code $prepareExitCode; refuse to start stale classes: $classesPath"
        }
    } finally {
        Pop-Location
    }
}

$state = Get-CloudBrainRuntimeState -ProjectPath $BrainProjectPath
if ($Rebuild -or (Test-CloudBrainClassesStale -State $state)) {
    Invoke-CloudBrainClasspathPrepare
    $state = Get-CloudBrainRuntimeState -ProjectPath $BrainProjectPath
}

if (Test-CloudBrainClassesStale -State $state) {
    throw "External dhxy-cloud-brain classes/resources are still stale after compile; sourceMaxUtc=$(Format-Utc $state.SourceMaxUtc) classesMaxUtc=$(Format-Utc $state.ClassesMaxUtc)"
}

$dependencyClasspath = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($dependencyClasspath)) {
    throw "External dhxy-cloud-brain dependency classpath is empty: $classpathFile"
}
$runtimeClasspath = "$classesPath;$dependencyClasspath"
$launchUtc = (Get-Date).ToUniversalTime().ToString("o")

Write-Host "Starting external DHXY cloud brain from fresh classpath on port $Port"
Write-Host "devArtifactMode=classpath launchUtc=$launchUtc projectPath=$BrainProjectPath classesPath=$classesPath classpathFile=$classpathFile sourceMaxUtc=$(Format-Utc $state.SourceMaxUtc) classesMaxUtc=$(Format-Utc $state.ClassesMaxUtc) classpathUtc=$(Format-Utc $state.ClasspathUtc) classpathSha256=$($state.ClasspathSha256)"
& java `
    "-Ddhxy.cloudbrain.devArtifactMode=classpath" `
    "-Ddhxy.cloudbrain.launchUtc=$launchUtc" `
    "-Ddhxy.cloudbrain.projectPath=$BrainProjectPath" `
    "-Ddhxy.cloudbrain.classesPath=$classesPath" `
    "-Ddhxy.cloudbrain.classpathFile=$classpathFile" `
    "-Ddhxy.cloudbrain.launchSourceMaxUtc=$(Format-Utc $state.SourceMaxUtc)" `
    "-Ddhxy.cloudbrain.launchClassesMaxUtc=$(Format-Utc $state.ClassesMaxUtc)" `
    "-Ddhxy.cloudbrain.launchClasspathUtc=$(Format-Utc $state.ClasspathUtc)" `
    "-Ddhxy.cloudbrain.launchClasspathSha256=$($state.ClasspathSha256)" `
    -cp $runtimeClasspath `
    $mainClassName `
    "--port=$Port" `
    "--token=$Token"
