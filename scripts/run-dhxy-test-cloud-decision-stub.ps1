param(
    [int]$Port = 18080,
    [string]$Path = "/api/cloud/decision",
    [string]$Token = "local-dev-token",
    [string]$ForcedDecision = "",
    [switch]$AllowDhxyTestSidecar
)

$ErrorActionPreference = "Stop"

if (-not $AllowDhxyTestSidecar) {
    throw "DHXY CloudDecisionDevServer is a test-sidecar/stub. Re-run with -AllowDhxyTestSidecar only for explicit debug or unit-test support."
}

mvn -q -DskipTests test-compile

$execArgs = "--port $Port --path $Path --token $Token"
if ($ForcedDecision -ne "") {
    $execArgs = "$execArgs --forced-decision $ForcedDecision"
}

mvn -q -DskipTests `
    "-Dexec.classpathScope=test" `
    "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServer" `
    "-Dexec.args=$execArgs" `
    org.codehaus.mojo:exec-maven-plugin:3.1.0:java
