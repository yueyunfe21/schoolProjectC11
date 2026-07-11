package com.bot.dhxy.ui;

import com.bot.dhxy.cloud.decision.CloudDecisionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Starts the configured local cloud decision endpoint only when a UI task start needs it.
 *
 * <p>The production default launches the external {@code D:\mavenProject\dhxy-cloud-brain} server.
 * The DHXY test-sidecar is reserved for unit tests or explicit debug scripts only. The caller must
 * block task startup when this gate reports unavailable; otherwise tasks may silently reuse a stale
 * external brain and make the runtime evidence misleading.</p>
 */
@Slf4j
@Component
public class CloudDecisionDevSidecarService {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:18080";
    private static final String DEFAULT_ENDPOINT_PATH = "/api/cloud/decision";
    private static final String DEFAULT_TOKEN = "local-dev-token";
    private static final Duration READINESS_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration STARTUP_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(5);
    private static final String REQUIRED_XIULUO_RESET_PROTOCOL = "RESET_REQUIRED_ACTION_OUTCOME_V1";
    private static final String REQUIRED_DEV_ARTIFACT_MODE = "classpath";

    private final CloudDecisionProperties properties;
    private final EndpointProbe endpointProbe;
    private final ProcessStarter processStarter;
    private final Duration startupPollInterval;
    private final Object startupLock = new Object();
    private volatile Process ownedProcess;

    @Autowired
    public CloudDecisionDevSidecarService(CloudDecisionProperties properties) {
        this(properties, new HttpEndpointProbe(), null, STARTUP_POLL_INTERVAL);
    }

    CloudDecisionDevSidecarService(
            CloudDecisionProperties properties,
            EndpointProbe endpointProbe,
            ProcessStarter processStarter,
            Duration startupPollInterval
    ) {
        this.properties = properties;
        this.endpointProbe = endpointProbe;
        this.processStarter = processStarter == null ? this::startProcess : processStarter;
        this.startupPollInterval = startupPollInterval == null || startupPollInterval.isNegative()
                || startupPollInterval.isZero()
                ? STARTUP_POLL_INTERVAL
                : startupPollInterval;
    }

    /**
     * Ensure the configured local cloud decision endpoint is ready for a UI-triggered task start.
     *
     * @return readiness result; UI callers must block task startup when the result is unavailable
     */
    public StartupResult ensureReadyForTaskStart() {
        try {
            Endpoint endpoint = endpointFromProperties();
            CloudDecisionProperties.DevSidecar devSidecar = properties.getDevSidecar();
            if (!properties.isEnabled() || !properties.isRealTransportEnabled()) {
                return StartupResult.skipped("cloud decision transport disabled; skip local dev sidecar");
            }
            if (!devSidecar.isAutoStartEnabled()) {
                return StartupResult.skipped("cloud dev sidecar auto-start disabled");
            }
            if (!isLocalEndpoint(endpoint.baseUri())) {
                log.info("cloud decision sidecar skipped for non-local endpoint: endpoint={}", endpoint.decisionUri());
                return StartupResult.skipped("cloud endpoint is not local: " + endpoint.decisionUri());
            }

            synchronized (startupLock) {
                ReadinessResult readiness = endpointProbe.probe(endpoint);
                if (readiness.ready()) {
                    log.info("cloud decision endpoint already available: endpoint={} restartOnTaskStart={} "
                                    + "sidecarPid={} sidecarVersion={} xiuluoResetProtocol={} "
                                    + "devArtifact={} reason=reuse-healthy-endpoint-to-preserve-cloud-sessions",
                            endpoint.decisionUri(),
                            devSidecar.isRestartOnTaskStart(),
                            readiness.sidecarPid(),
                            readiness.sidecarVersion(),
                            readiness.xiuluoResetProtocol(),
                            readiness.devArtifactSummary());
                    if (devSidecar.isRestartOnTaskStart()) {
                        appendSidecarLog("reuse healthy endpoint despite restart-on-task-start endpoint="
                                + endpoint.decisionUri()
                                + " sidecarPid=" + readiness.sidecarPid()
                                + " sidecarVersion=" + readiness.sidecarVersion()
                                + " xiuluoResetProtocol=" + readiness.xiuluoResetProtocol()
                                + " devArtifact=" + readiness.devArtifactSummary()
                                + " reason=preserve active cloud sessions");
                    }
                    return StartupResult.available("本地 Cloud 决策端点已就绪", false);
                }
                if (readiness.isRecoverableStaleClasspathArtifact()) {
                    log.warn("cloud decision endpoint artifact is stale; restart and wait before task start: endpoint={} "
                                    + "sidecarPid={} devArtifact={} reason={}",
                            endpoint.decisionUri(), readiness.sidecarPid(), readiness.devArtifactSummary(),
                            readiness.reason());
                    appendSidecarLog("restart stale artifact before task start endpoint=" + endpoint.decisionUri()
                            + " sidecarPid=" + readiness.sidecarPid()
                            + " devArtifact=" + readiness.devArtifactSummary());
                    restartLocalSidecarForTaskStart(endpoint);
                    Process process = startOwnedProcess(endpoint, devSidecar);
                    if (process == null) {
                        return StartupResult.unavailable("本地 Cloud 决策端点过期后重启失败：脚本缺失或进程无法启动");
                    }
                    ownedProcess = process;
                    return waitUntilReady(endpoint, startupTimeout(devSidecar), true);
                }
                if (readiness.responded()) {
                    log.warn("cloud decision endpoint version/protocol mismatch; refuse task start: endpoint={} "
                                    + "sidecarPid={} sidecarVersion={} xiuluoResetProtocol={} requiredProtocol={} reason={}",
                            endpoint.decisionUri(),
                            readiness.sidecarPid(),
                            readiness.sidecarVersion(),
                            readiness.xiuluoResetProtocol(),
                            REQUIRED_XIULUO_RESET_PROTOCOL,
                            readiness.reason());
                    appendSidecarLog("refuse endpoint due version/protocol mismatch endpoint=" + endpoint.decisionUri()
                            + " sidecarPid=" + readiness.sidecarPid()
                            + " sidecarVersion=" + readiness.sidecarVersion()
                            + " xiuluoResetProtocol=" + readiness.xiuluoResetProtocol()
                            + " requiredProtocol=" + REQUIRED_XIULUO_RESET_PROTOCOL
                            + " reason=" + readiness.reason());
                    return StartupResult.unavailable("本地 Cloud 决策端点版本/协议不匹配："
                            + readiness.reason() + "，请先重启/重建 external cloud-brain");
                }

                if (devSidecar.isRestartOnTaskStart()) {
                    restartLocalSidecarForTaskStart(endpoint);
                    readiness = endpointProbe.probe(endpoint);
                    if (readiness.ready()) {
                        log.info("cloud decision endpoint ready after restart cleanup: endpoint={} "
                                        + "sidecarPid={} sidecarVersion={} xiuluoResetProtocol={}",
                                endpoint.decisionUri(),
                                readiness.sidecarPid(),
                                readiness.sidecarVersion(),
                                readiness.xiuluoResetProtocol());
                        return StartupResult.available("本地 Cloud 决策端点已就绪", false);
                    }
                    if (readiness.responded()) {
                        return StartupResult.unavailable("本地 Cloud 决策端点重启后版本/协议仍不匹配："
                                + readiness.reason());
                    }
                }

                if (ownedProcess != null && !ownedProcess.isAlive()) {
                    log.warn("cloud decision sidecar owned process exited before readiness: endpoint={}",
                            endpoint.decisionUri());
                    appendSidecarLog("owned process exited before readiness endpoint=" + endpoint.decisionUri());
                    ownedProcess = null;
                }

                boolean startedNow = false;
                if (ownedProcess == null) {
                    Process process = startOwnedProcess(endpoint, devSidecar);
                    if (process == null) {
                        return StartupResult.unavailable("本地 Cloud 决策端点启动失败：脚本缺失或进程无法启动");
                    }
                    ownedProcess = process;
                    startedNow = true;
                } else {
                    log.info("cloud decision sidecar owned process already running, waiting: endpoint={}",
                            endpoint.decisionUri());
                }

                return waitUntilReady(endpoint, startupTimeout(devSidecar), startedNow);
            }
        } catch (Exception e) {
            log.warn("cloud decision sidecar gate failed; cloud-required services remain fail-closed: reason={}",
                    e.getMessage(), e);
            appendSidecarLog("gate failed cloud-required services remain fail-closed reason=" + e.getMessage());
            return StartupResult.unavailable("本地 Cloud 决策端点检查失败：" + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        Process process = ownedProcess;
        if (process == null) {
            stopConfiguredLocalCloudBrainOnShutdown();
            return;
        }
        ownedProcess = null;
        if (!process.isAlive()) {
            stopConfiguredLocalCloudBrainOnShutdown();
            return;
        }
        log.info("cloud decision sidecar stopping owned process tree");
        appendSidecarLog("stopping owned process tree");
        destroyProcessTree(process, "owned process");
        stopConfiguredLocalCloudBrainOnShutdown();
    }

    private void restartLocalSidecarForTaskStart(Endpoint endpoint) {
        log.info("cloud decision sidecar force restart before task start: endpoint={}", endpoint.decisionUri());
        appendSidecarLog("force restart before task start endpoint=" + endpoint.decisionUri());
        Process process = ownedProcess;
        ownedProcess = null;
        if (process != null && process.isAlive()) {
            destroyProcessTree(process, "owned process before task start");
        }
        stopExternalCloudBrainForPort(endpoint.port());
    }

    private void destroyProcessTree(Process process, String reason) {
        List<ProcessHandle> descendants;
        try {
            descendants = process.toHandle().descendants().toList();
        } catch (UnsupportedOperationException | SecurityException e) {
            log.warn("cloud decision sidecar could not inspect process tree, stopping root only: reason={} detail={}",
                    reason, e.getMessage());
            appendSidecarLog("process tree inspect failed, stopping root only reason=" + reason
                    + " detail=" + e.getMessage());
            descendants = List.of();
        }
        descendants.forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            waitForProcessTreeExit(descendants, process);
            List<ProcessHandle> aliveDescendants = descendants.stream()
                    .filter(ProcessHandle::isAlive)
                    .toList();
            if (!aliveDescendants.isEmpty() || process.isAlive()) {
                log.warn("cloud decision sidecar owned process tree did not exit, destroying forcibly: descendants={}",
                        aliveDescendants.size());
                appendSidecarLog("owned process tree did not exit, destroying forcibly descendants="
                        + aliveDescendants.size());
                aliveDescendants.forEach(ProcessHandle::destroyForcibly);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            descendants.stream()
                    .filter(ProcessHandle::isAlive)
                    .forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }

    private void stopExternalCloudBrainForPort(int port) {
        List<Long> stoppedPids = stopExternalCloudBrainByPortOwner(port);
        if (stoppedPids.isEmpty()) {
            log.info("cloud decision sidecar force restart found no external cloud brain process: port={}", port);
            appendSidecarLog("force restart found no external cloud brain process port=" + port);
            return;
        }
        log.info("cloud decision sidecar stopped external cloud brain port owners: port={} pids={}",
                port, stoppedPids);
        appendSidecarLog("stopped external cloud brain port owners port=" + port + " pids=" + stoppedPids);
    }

    private List<Long> stopExternalCloudBrainByPortOwner(int port) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            log.warn("cloud decision sidecar port-owner cleanup is only implemented on Windows: port={}", port);
            appendSidecarLog("port-owner cleanup unsupported os=" + System.getProperty("os.name") + " port=" + port);
            return List.of();
        }
        String script = """
                $ErrorActionPreference = 'Stop'
                $port = %d
                $portArg = '--port=' + $port
                $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
                foreach ($pidValue in ($listeners | Select-Object -ExpandProperty OwningProcess -Unique)) {
                    if (-not $pidValue) { continue }
                    $proc = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
                        Where-Object { $_.ProcessId -eq [int]$pidValue } |
                        Select-Object -First 1
                    if (-not $proc) { continue }
                    $cmd = [string]$proc.CommandLine
                    $legacyJarSidecar = $cmd.Contains('dhxy-cloud-brain-0.1.0-SNAPSHOT.jar') -and $cmd.Contains($portArg)
                    $classpathSidecar = $cmd.Contains('com.yueyunfe.dhxy.cloudbrain.CloudBrainApplication') `
                        -and $cmd.Contains('-Ddhxy.cloudbrain.devArtifactMode=classpath') `
                        -and $cmd.Contains($portArg)
                    if ($legacyJarSidecar -or $classpathSidecar) {
                        Stop-Process -Id $pidValue -Force -ErrorAction Stop
                        Write-Output "STOPPED $pidValue"
                    } else {
                        Write-Output "SKIPPED $pidValue $cmd"
                    }
                }
                """.formatted(port);
        List<String> command = List.of(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command", script);
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(SHUTDOWN_WAIT.toMillis(), TimeUnit.MILLISECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!finished) {
                process.destroyForcibly();
                log.warn("cloud decision sidecar port-owner cleanup timed out: port={} output={}", port, output);
                appendSidecarLog("port-owner cleanup timed out port=" + port + " output=" + output);
                return List.of();
            }
            if (process.exitValue() != 0) {
                log.warn("cloud decision sidecar port-owner cleanup failed: port={} exit={} output={}",
                        port, process.exitValue(), output);
                appendSidecarLog("port-owner cleanup failed port=" + port + " exit=" + process.exitValue()
                        + " output=" + output);
                return List.of();
            }
            if (!output.isBlank()) {
                log.info("cloud decision sidecar port-owner cleanup output: port={} output={}", port, output);
                appendSidecarLog("port-owner cleanup output port=" + port + " output=" + output);
            }
            List<Long> stopped = new ArrayList<>();
            for (String line : output.split("\\R")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("STOPPED ")) {
                    continue;
                }
                try {
                    stopped.add(Long.parseLong(trimmed.substring("STOPPED ".length()).trim()));
                } catch (NumberFormatException ignored) {
                    log.debug("cloud decision sidecar ignored unparsable stopped pid line: {}", trimmed);
                }
            }
            return stopped;
        } catch (IOException e) {
            log.warn("cloud decision sidecar port-owner cleanup could not start PowerShell: port={} reason={}",
                    port, e.getMessage());
            appendSidecarLog("port-owner cleanup start failed port=" + port + " reason=" + e.getMessage());
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            appendSidecarLog("port-owner cleanup interrupted port=" + port);
            return List.of();
        }
    }

    private void stopConfiguredLocalCloudBrainOnShutdown() {
        try {
            if (!properties.isEnabled() || !properties.isRealTransportEnabled()) {
                return;
            }
            CloudDecisionProperties.DevSidecar devSidecar = properties.getDevSidecar();
            if (!devSidecar.isAutoStartEnabled() || !devSidecar.isRestartOnTaskStart()) {
                return;
            }
            Endpoint endpoint = endpointFromProperties();
            if (!isLocalEndpoint(endpoint.baseUri())) {
                return;
            }
            log.info("cloud decision sidecar shutdown cleanup for local cloud brain: endpoint={}",
                    endpoint.decisionUri());
            appendSidecarLog("shutdown cleanup endpoint=" + endpoint.decisionUri());
            stopExternalCloudBrainForPort(endpoint.port());
        } catch (Exception e) {
            log.debug("cloud decision sidecar shutdown cleanup skipped: reason={}", e.getMessage());
        }
    }

    private void waitForProcessTreeExit(List<ProcessHandle> descendants, Process process) throws InterruptedException {
        long deadline = System.nanoTime() + SHUTDOWN_WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            boolean descendantsAlive = descendants.stream().anyMatch(ProcessHandle::isAlive);
            if (!descendantsAlive && !process.isAlive()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    private Process startOwnedProcess(Endpoint endpoint, CloudDecisionProperties.DevSidecar devSidecar) {
        Path scriptPath = Path.of(nonBlank(devSidecar.getScriptPath(),
                "scripts/run-cloud-brain-server.ps1")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(scriptPath)) {
            log.warn("cloud decision dev sidecar script missing: path={}", scriptPath.toAbsolutePath());
            appendSidecarLog("script missing path=" + scriptPath.toAbsolutePath());
            return null;
        }
        Path brainProjectPath = cloudBrainProjectPath(devSidecar);
        if (!Files.isDirectory(brainProjectPath)) {
            log.warn("cloud decision dev sidecar brain project missing: path={}", brainProjectPath);
            appendSidecarLog("brain project missing path=" + brainProjectPath);
            return null;
        }

        List<String> command = new ArrayList<>(List.of(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", scriptPath.toString(),
                "-Port", String.valueOf(endpoint.port()),
                "-Path", endpoint.endpointPath(),
                "-Token", endpoint.token(),
                "-BrainProjectPath", brainProjectPath.toString()));
        if (devSidecar.isRebuildOnStart()) {
            command.add("-Rebuild");
        }
        log.info("cloud decision endpoint not available, starting sidecar: endpoint={} script={} brainProject={} rebuild={} log={}",
                endpoint.decisionUri(), scriptPath, brainProjectPath, devSidecar.isRebuildOnStart(),
                logPath(devSidecar).toAbsolutePath());
        appendSidecarLog("starting endpoint=" + endpoint.decisionUri()
                + " script=" + scriptPath
                + " brainProject=" + brainProjectPath
                + " rebuild=" + devSidecar.isRebuildOnStart());
        try {
            return processStarter.start(command);
        } catch (IOException e) {
            log.warn("cloud decision sidecar process start failed: endpoint={} reason={}",
                    endpoint.decisionUri(), e.getMessage());
            appendSidecarLog("process start failed endpoint=" + endpoint.decisionUri()
                    + " reason=" + e.getMessage());
            return null;
        }
    }

    private Process startProcess(List<String> command) throws IOException {
        Path logPath = logPath(properties.getDevSidecar());
        Path parent = logPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path brainProjectPath = cloudBrainProjectPath(properties.getDevSidecar());
        return new ProcessBuilder(command)
                .directory(brainProjectPath.toFile())
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()))
                .redirectError(ProcessBuilder.Redirect.appendTo(logPath.toFile()))
                .start();
    }

    private Path cloudBrainProjectPath(CloudDecisionProperties.DevSidecar devSidecar) {
        return Path.of(nonBlank(devSidecar.getBrainProjectPath(),
                "D:\\mavenProject\\dhxy-cloud-brain")).toAbsolutePath().normalize();
    }

    private StartupResult waitUntilReady(Endpoint endpoint, Duration timeout, boolean startedNow) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            ReadinessResult readiness = endpointProbe.probe(endpoint);
            if (readiness.ready()) {
                    log.info("cloud decision endpoint ready: endpoint={} startedBySidecar={} "
                                + "sidecarPid={} sidecarVersion={} xiuluoResetProtocol={} devArtifact={}",
                        endpoint.decisionUri(),
                        startedNow,
                        readiness.sidecarPid(),
                        readiness.sidecarVersion(),
                        readiness.xiuluoResetProtocol(),
                        readiness.devArtifactSummary());
                appendSidecarLog("ready endpoint=" + endpoint.decisionUri()
                        + " startedBySidecar=" + startedNow
                        + " sidecarPid=" + readiness.sidecarPid()
                        + " sidecarVersion=" + readiness.sidecarVersion()
                        + " xiuluoResetProtocol=" + readiness.xiuluoResetProtocol()
                        + " devArtifact=" + readiness.devArtifactSummary());
                return StartupResult.available("本地 Cloud 决策端点启动完成", startedNow);
            }
            if (readiness.responded()) {
                log.warn("cloud decision endpoint became reachable but protocol is not current: endpoint={} "
                                + "startedBySidecar={} sidecarPid={} sidecarVersion={} xiuluoResetProtocol={} "
                                + "requiredProtocol={} reason={}",
                        endpoint.decisionUri(),
                        startedNow,
                        readiness.sidecarPid(),
                        readiness.sidecarVersion(),
                        readiness.xiuluoResetProtocol(),
                        REQUIRED_XIULUO_RESET_PROTOCOL,
                        readiness.reason());
                appendSidecarLog("reachable endpoint rejected by readiness version gate endpoint="
                        + endpoint.decisionUri()
                        + " startedBySidecar=" + startedNow
                        + " sidecarPid=" + readiness.sidecarPid()
                        + " sidecarVersion=" + readiness.sidecarVersion()
                        + " xiuluoResetProtocol=" + readiness.xiuluoResetProtocol()
                        + " requiredProtocol=" + REQUIRED_XIULUO_RESET_PROTOCOL
                        + " reason=" + readiness.reason());
                return StartupResult.unavailable("本地 Cloud 决策端点版本/协议不匹配："
                        + readiness.reason());
            }
            if (ownedProcess != null && !ownedProcess.isAlive()) {
                log.warn("cloud decision sidecar process exited before endpoint became ready: endpoint={}",
                        endpoint.decisionUri());
                appendSidecarLog("process exited before readiness endpoint=" + endpoint.decisionUri());
                return StartupResult.unavailable("本地 Cloud 决策端点进程提前退出，请查看 "
                        + logPath(properties.getDevSidecar()).toAbsolutePath());
            }
            if (!sleep(startupPollInterval)) {
                return StartupResult.unavailable("本地 Cloud 决策端点检查被中断");
            }
        }
        log.warn("cloud decision endpoint startup timed out: endpoint={} timeoutMs={} log={}",
                endpoint.decisionUri(), timeout.toMillis(), logPath(properties.getDevSidecar()).toAbsolutePath());
        appendSidecarLog("startup timeout endpoint=" + endpoint.decisionUri()
                + " timeoutMs=" + timeout.toMillis());
        return StartupResult.unavailable("本地 Cloud 决策端点未就绪：启动超时，请查看 "
                + logPath(properties.getDevSidecar()).toAbsolutePath());
    }

    private Endpoint endpointFromProperties() {
        String baseUrl = nonBlank(properties.getBaseUrl(), DEFAULT_BASE_URL);
        String endpointPath = normalizePath(nonBlank(properties.getEndpointPath(), DEFAULT_ENDPOINT_PATH));
        String token = nonBlank(properties.getToken(), DEFAULT_TOKEN);
        URI baseUri = URI.create(stripTrailingSlash(baseUrl));
        URI decisionUri = URI.create(joinUrl(baseUri.toString(), endpointPath));
        int port = baseUri.getPort();
        if (port <= 0) {
            port = "https".equalsIgnoreCase(baseUri.getScheme()) ? 443 : 80;
        }
        return new Endpoint(baseUri, decisionUri, endpointPath, token, port);
    }

    private Duration startupTimeout(CloudDecisionProperties.DevSidecar devSidecar) {
        long timeoutMs = devSidecar.getStartupTimeoutMs();
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
        return Duration.ofMillis(timeoutMs);
    }

    private Path logPath(CloudDecisionProperties.DevSidecar devSidecar) {
        return Path.of(nonBlank(devSidecar.getLogPath(), "logs/cloud-decision-dev-sidecar.log"));
    }

    private void appendSidecarLog(String message) {
        try {
            Path path = logPath(properties.getDevSidecar());
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path,
                    LocalDateTime.now() + " " + message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.debug("cloud decision sidecar log append failed: reason={}", e.getMessage());
        }
    }

    private static boolean isLocalEndpoint(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized);
    }

    private static boolean sleep(Duration duration) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.max(1L, duration.toMillis()));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String joinUrl(String baseUrl, String endpointPath) {
        if (baseUrl.endsWith("/") && endpointPath.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpointPath;
        }
        if (!baseUrl.endsWith("/") && !endpointPath.startsWith("/")) {
            return baseUrl + "/" + endpointPath;
        }
        return baseUrl + endpointPath;
    }

    private static String stripTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String normalizePath(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_ENDPOINT_PATH;
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @FunctionalInterface
    interface EndpointProbe {
        ReadinessResult probe(Endpoint endpoint);
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(List<String> command) throws IOException;
    }

    record Endpoint(URI baseUri, URI decisionUri, String endpointPath, String token, int port) {
    }

    record ReadinessResult(
            boolean ready,
            boolean responded,
            String policyVersion,
            String sidecarPid,
            String sidecarVersion,
            String xiuluoResetProtocol,
            String devArtifactMode,
            String devArtifactFresh,
            String devArtifactSourceMaxUtc,
            String devArtifactClassesMaxUtc,
            String devArtifactClasspathUtc,
            String devArtifactClasspathSha256,
            String devArtifactLaunchUtc,
            String devArtifactLaunchSourceMaxUtc,
            String devArtifactLaunchClassesMaxUtc,
            String devArtifactLaunchClasspathUtc,
            String devArtifactLaunchClasspathSha256,
            String devArtifactFreshReason,
            String reason) {
        static ReadinessResult unavailable(String reason) {
            return new ReadinessResult(false, false, "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", reason);
        }

        static ReadinessResult incompatible(
                String policyVersion,
                String sidecarPid,
                String sidecarVersion,
                String xiuluoResetProtocol,
                String devArtifactMode,
                String devArtifactFresh,
                String devArtifactSourceMaxUtc,
                String devArtifactClassesMaxUtc,
                String devArtifactClasspathUtc,
                String devArtifactClasspathSha256,
                String devArtifactLaunchUtc,
                String devArtifactLaunchSourceMaxUtc,
                String devArtifactLaunchClassesMaxUtc,
                String devArtifactLaunchClasspathUtc,
                String devArtifactLaunchClasspathSha256,
                String devArtifactFreshReason,
                String reason) {
            return new ReadinessResult(false, true, policyVersion, sidecarPid, sidecarVersion,
                    xiuluoResetProtocol, devArtifactMode, devArtifactFresh, devArtifactSourceMaxUtc,
                    devArtifactClassesMaxUtc, devArtifactClasspathUtc, devArtifactClasspathSha256,
                    devArtifactLaunchUtc, devArtifactLaunchSourceMaxUtc, devArtifactLaunchClassesMaxUtc,
                    devArtifactLaunchClasspathUtc, devArtifactLaunchClasspathSha256, devArtifactFreshReason, reason);
        }

        static ReadinessResult ready(
                String policyVersion,
                String sidecarPid,
                String sidecarVersion,
                String xiuluoResetProtocol,
                String devArtifactMode,
                String devArtifactFresh,
                String devArtifactSourceMaxUtc,
                String devArtifactClassesMaxUtc,
                String devArtifactClasspathUtc,
                String devArtifactClasspathSha256,
                String devArtifactLaunchUtc,
                String devArtifactLaunchSourceMaxUtc,
                String devArtifactLaunchClassesMaxUtc,
                String devArtifactLaunchClasspathUtc,
                String devArtifactLaunchClasspathSha256,
                String devArtifactFreshReason) {
            return new ReadinessResult(true, true, policyVersion, sidecarPid, sidecarVersion,
                    xiuluoResetProtocol, devArtifactMode, devArtifactFresh, devArtifactSourceMaxUtc,
                    devArtifactClassesMaxUtc, devArtifactClasspathUtc, devArtifactClasspathSha256,
                    devArtifactLaunchUtc, devArtifactLaunchSourceMaxUtc, devArtifactLaunchClassesMaxUtc,
                    devArtifactLaunchClasspathUtc, devArtifactLaunchClasspathSha256, devArtifactFreshReason, "ready");
        }

        String devArtifactSummary() {
            return "mode=" + devArtifactMode
                    + ",fresh=" + devArtifactFresh
                    + ",sourceMaxUtc=" + devArtifactSourceMaxUtc
                    + ",classesMaxUtc=" + devArtifactClassesMaxUtc
                    + ",classpathUtc=" + devArtifactClasspathUtc
                    + ",classpathSha256=" + devArtifactClasspathSha256
                    + ",launchUtc=" + devArtifactLaunchUtc
                    + ",launchSourceMaxUtc=" + devArtifactLaunchSourceMaxUtc
                    + ",launchClassesMaxUtc=" + devArtifactLaunchClassesMaxUtc
                    + ",launchClasspathUtc=" + devArtifactLaunchClasspathUtc
                    + ",launchClasspathSha256=" + devArtifactLaunchClasspathSha256
                    + ",reason=" + devArtifactFreshReason;
        }

        private boolean isRecoverableStaleClasspathArtifact() {
            return responded
                    && REQUIRED_XIULUO_RESET_PROTOCOL.equals(xiuluoResetProtocol)
                    && REQUIRED_DEV_ARTIFACT_MODE.equals(devArtifactMode)
                    && "false".equalsIgnoreCase(devArtifactFresh);
        }
    }

    public record StartupResult(boolean available, boolean startedProcess, boolean skipped, String message) {
        static StartupResult available(String message, boolean startedProcess) {
            return new StartupResult(true, startedProcess, false, message);
        }

        static StartupResult skipped(String message) {
            return new StartupResult(true, false, true, message);
        }

        static StartupResult unavailable(String message) {
            return new StartupResult(false, false, false, message);
        }
    }

    private static class HttpEndpointProbe implements EndpointProbe {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(READINESS_TIMEOUT)
                .build();

        @Override
        public ReadinessResult probe(Endpoint endpoint) {
            try {
                String body = """
                        {"serviceId":"TASK_CLASSIFIER","traceId":"cloud-dev-sidecar-readiness","localDecision":"READY"}
                        """;
                HttpRequest request = HttpRequest.newBuilder(endpoint.decisionUri())
                        .timeout(READINESS_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + endpoint.token())
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return ReadinessResult.unavailable("http status=" + response.statusCode());
                }
                JsonNode root = objectMapper.readTree(response.body());
                String policyVersion = text(root, "policyVersion");
                JsonNode diagnostics = root.path("diagnostics");
                String sidecarPid = text(diagnostics, "sidecarPid");
                String sidecarVersion = text(diagnostics, "sidecarVersion");
                String xiuluoResetProtocol = text(diagnostics, "xiuluoResetProtocol");
                String devArtifactMode = text(diagnostics, "devArtifactMode");
                String devArtifactFresh = text(diagnostics, "devArtifactFresh");
                String devArtifactSourceMaxUtc = text(diagnostics, "devArtifactSourceMaxUtc");
                String devArtifactClassesMaxUtc = text(diagnostics, "devArtifactClassesMaxUtc");
                String devArtifactClasspathUtc = text(diagnostics, "devArtifactClasspathUtc");
                String devArtifactClasspathSha256 = text(diagnostics, "devArtifactClasspathSha256");
                String devArtifactLaunchUtc = text(diagnostics, "devArtifactLaunchUtc");
                String devArtifactLaunchSourceMaxUtc = text(diagnostics, "devArtifactLaunchSourceMaxUtc");
                String devArtifactLaunchClassesMaxUtc = text(diagnostics, "devArtifactLaunchClassesMaxUtc");
                String devArtifactLaunchClasspathUtc = text(diagnostics, "devArtifactLaunchClasspathUtc");
                String devArtifactLaunchClasspathSha256 = text(diagnostics, "devArtifactLaunchClasspathSha256");
                String devArtifactFreshReason = text(diagnostics, "devArtifactFreshReason");
                if (!REQUIRED_XIULUO_RESET_PROTOCOL.equals(xiuluoResetProtocol)) {
                    return ReadinessResult.incompatible(
                            policyVersion,
                            sidecarPid,
                            sidecarVersion,
                            xiuluoResetProtocol,
                            devArtifactMode,
                            devArtifactFresh,
                            devArtifactSourceMaxUtc,
                            devArtifactClassesMaxUtc,
                            devArtifactClasspathUtc,
                            devArtifactClasspathSha256,
                            devArtifactLaunchUtc,
                            devArtifactLaunchSourceMaxUtc,
                            devArtifactLaunchClassesMaxUtc,
                            devArtifactLaunchClasspathUtc,
                            devArtifactLaunchClasspathSha256,
                            devArtifactFreshReason,
                            "xiuluoResetProtocol expected=" + REQUIRED_XIULUO_RESET_PROTOCOL
                                    + " actual=" + nonBlank(xiuluoResetProtocol, "<missing>"));
                }
                if (!REQUIRED_DEV_ARTIFACT_MODE.equals(devArtifactMode)) {
                    return ReadinessResult.incompatible(
                            policyVersion,
                            sidecarPid,
                            sidecarVersion,
                            xiuluoResetProtocol,
                            devArtifactMode,
                            devArtifactFresh,
                            devArtifactSourceMaxUtc,
                            devArtifactClassesMaxUtc,
                            devArtifactClasspathUtc,
                            devArtifactClasspathSha256,
                            devArtifactLaunchUtc,
                            devArtifactLaunchSourceMaxUtc,
                            devArtifactLaunchClassesMaxUtc,
                            devArtifactLaunchClasspathUtc,
                            devArtifactLaunchClasspathSha256,
                            devArtifactFreshReason,
                            "devArtifactMode expected=" + REQUIRED_DEV_ARTIFACT_MODE
                                    + " actual=" + nonBlank(devArtifactMode, "<missing>"));
                }
                if (!"true".equalsIgnoreCase(devArtifactFresh)) {
                    return ReadinessResult.incompatible(
                            policyVersion,
                            sidecarPid,
                            sidecarVersion,
                            xiuluoResetProtocol,
                            devArtifactMode,
                            devArtifactFresh,
                            devArtifactSourceMaxUtc,
                            devArtifactClassesMaxUtc,
                            devArtifactClasspathUtc,
                            devArtifactClasspathSha256,
                            devArtifactLaunchUtc,
                            devArtifactLaunchSourceMaxUtc,
                            devArtifactLaunchClassesMaxUtc,
                            devArtifactLaunchClasspathUtc,
                            devArtifactLaunchClasspathSha256,
                            devArtifactFreshReason,
                            "devArtifactFresh expected=true actual=" + nonBlank(devArtifactFresh, "<missing>")
                                    + " sourceMaxUtc=" + devArtifactSourceMaxUtc
                                    + " classesMaxUtc=" + devArtifactClassesMaxUtc
                                    + " classpathUtc=" + devArtifactClasspathUtc
                                    + " classpathSha256=" + devArtifactClasspathSha256
                                    + " launchUtc=" + devArtifactLaunchUtc
                                    + " launchSourceMaxUtc=" + devArtifactLaunchSourceMaxUtc
                                    + " launchClassesMaxUtc=" + devArtifactLaunchClassesMaxUtc
                                    + " launchClasspathUtc=" + devArtifactLaunchClasspathUtc
                                    + " launchClasspathSha256=" + devArtifactLaunchClasspathSha256
                                    + " reason=" + devArtifactFreshReason);
                }
                return ReadinessResult.ready(
                        policyVersion,
                        sidecarPid,
                        sidecarVersion,
                        xiuluoResetProtocol,
                        devArtifactMode,
                        devArtifactFresh,
                        devArtifactSourceMaxUtc,
                        devArtifactClassesMaxUtc,
                        devArtifactClasspathUtc,
                        devArtifactClasspathSha256,
                        devArtifactLaunchUtc,
                        devArtifactLaunchSourceMaxUtc,
                        devArtifactLaunchClassesMaxUtc,
                        devArtifactLaunchClasspathUtc,
                        devArtifactLaunchClasspathSha256,
                        devArtifactFreshReason);
            } catch (Exception e) {
                return ReadinessResult.unavailable(e.getClass().getSimpleName());
            }
        }

        private static String text(JsonNode node, String field) {
            JsonNode value = node == null ? null : node.get(field);
            return value == null || value.isNull() ? "" : value.asText("");
        }
    }
}
