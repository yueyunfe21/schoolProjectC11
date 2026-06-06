package com.bot.dhxy.ui;

import com.bot.dhxy.config.BotProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts the local OCR sidecar from the JavaFX UI when a task run needs it.
 *
 * <p>OCR-dependent task startup must use the blocking readiness check. A cold RapidOCR startup can
 * take a while, but the controller must not scan/control game windows until the local endpoint is
 * confirmed healthy.</p>
 */
@Slf4j
@Component
public class LocalOcrSidecarService {

    private static final Path SCRIPT_PATH = Path.of("scripts", "local_ocr_server.py");
    private static final Path LOG_PATH = Path.of("logs", "local-ocr-sidecar.log");
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration STARTUP_POLL_TIMEOUT = Duration.ofSeconds(60);

    private final BotProperties botProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HEALTH_TIMEOUT)
            .build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "local-ocr-sidecar-starter");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean startInProgress = new AtomicBoolean(false);

    public LocalOcrSidecarService(BotProperties botProperties) {
        this.botProperties = botProperties;
    }

    /**
     * Ensure the configured local OCR endpoint is alive, starting the Python sidecar if needed.
     */
    public void ensureRunningAsync() {
        if (isHealthy()) {
            log.debug("local OCR sidecar already healthy: endpoint={}", localEndpoint());
            return;
        }
        if (!startInProgress.compareAndSet(false, true)) {
            log.info("local OCR sidecar start already in progress: endpoint={}", localEndpoint());
            return;
        }
        executor.submit(() -> {
            try {
                if (!ensureProcessStarted()) {
                    log.warn("local OCR sidecar async start failed: python/py command unavailable or rejected");
                    return;
                }
                StartupResult result = waitUntilHealthy("本地OCR启动完成");
                if (!result.healthy()) {
                    log.warn("local OCR sidecar async ensure failed: {}", result.message());
                }
            } finally {
                startInProgress.set(false);
            }
        });
    }

    /**
     * Ensure OCR is healthy before a task start is allowed to touch game windows.
     *
     * @return startup result; callers must abort task startup when {@code healthy=false}.
     */
    public StartupResult ensureRunningBlocking() {
        if (isHealthy()) {
            return StartupResult.healthy("本地OCR已就绪");
        }
        if (!startInProgress.compareAndSet(false, true)) {
            log.info("local OCR sidecar start already in progress, waiting: endpoint={}", localEndpoint());
            return waitUntilHealthy("本地OCR已就绪");
        }
        try {
            if (!ensureProcessStarted()) {
                return StartupResult.failed("本地OCR启动失败：脚本缺失，或 python/py 无法启动");
            }
            return waitUntilHealthy("本地OCR启动完成");
        } finally {
            startInProgress.set(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private boolean ensureProcessStarted() {
        if (isHealthy()) {
            return true;
        }
        if (!Files.isRegularFile(SCRIPT_PATH)) {
            log.warn("local OCR sidecar script missing: path={}", SCRIPT_PATH.toAbsolutePath());
            return false;
        }

        LocalEndpoint endpoint = parseLocalEndpoint();
        log.info("local OCR sidecar not reachable, starting: endpoint={} script={}",
                localEndpoint(), SCRIPT_PATH.toAbsolutePath());
        // Prefer the Windows Python launcher. On this machine, plain "python" can resolve to
        // the WindowsApps store alias, which starts successfully as a process but exits without
        // running the OCR server.
        boolean processStarted = startProcess(List.of(
                "py", "-3",
                SCRIPT_PATH.toString(),
                "--host", endpoint.host(),
                "--port", String.valueOf(endpoint.port())));
        if (!processStarted) {
            processStarted = startProcess(List.of(
                    "python",
                    SCRIPT_PATH.toString(),
                    "--host", endpoint.host(),
                    "--port", String.valueOf(endpoint.port())));
        }
        if (!processStarted) {
            log.warn("local OCR sidecar start failed: python/py command unavailable or rejected");
            return false;
        }
        return true;
    }

    private StartupResult waitUntilHealthy(String successMessage) {
        long deadline = System.nanoTime() + STARTUP_POLL_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (isHealthy()) {
                log.info("local OCR sidecar healthy: endpoint={}", localEndpoint());
                return StartupResult.healthy(successMessage);
            }
            sleep(500);
        }
        log.warn("local OCR sidecar health check is still unavailable: endpoint={} log={}",
                localEndpoint(), LOG_PATH.toAbsolutePath());
        return StartupResult.failed("本地OCR未就绪：健康检查超时，请查看 " + LOG_PATH.toAbsolutePath());
    }

    private boolean startProcess(List<String> command) {
        try {
            Path parent = LOG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            new ProcessBuilder(command)
                    .directory(Path.of("").toAbsolutePath().toFile())
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(LOG_PATH.toFile()))
                    .redirectError(ProcessBuilder.Redirect.appendTo(LOG_PATH.toFile()))
                    .start();
            return true;
        } catch (IOException e) {
            log.warn("local OCR sidecar process start failed: command={} reason={}", command, e.getMessage());
            return false;
        }
    }

    private boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(localEndpoint() + "/health"))
                    .timeout(HEALTH_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private LocalEndpoint parseLocalEndpoint() {
        try {
            URI uri = URI.create(localEndpoint());
            String host = uri.getHost() == null || uri.getHost().isBlank() ? "127.0.0.1" : uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 18761;
            return new LocalEndpoint(host, port);
        } catch (Exception e) {
            return new LocalEndpoint("127.0.0.1", 18761);
        }
    }

    private String localEndpoint() {
        String endpoint = botProperties.getOcr() == null ? null : botProperties.getOcr().getLocalEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://127.0.0.1:18761";
        }
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record LocalEndpoint(String host, int port) {
    }

    public record StartupResult(boolean healthy, String message) {
        private static StartupResult healthy(String message) {
            return new StartupResult(true, message);
        }

        private static StartupResult failed(String message) {
            return new StartupResult(false, message);
        }
    }
}
