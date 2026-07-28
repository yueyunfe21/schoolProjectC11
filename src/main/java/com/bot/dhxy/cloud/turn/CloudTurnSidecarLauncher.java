package com.bot.dhxy.cloud.turn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ensures the configured local Cloud Brain listener is ready before a turn loop is registered. */
@Component
@Slf4j
public class CloudTurnSidecarLauncher {

    private static final Pattern WINDOWS_SID = Pattern.compile("S-1-(?:\\d+-)+\\d+");
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500L);

    private final TurnClientProperties turnProperties;
    private final CloudTurnSidecarProperties sidecarProperties;
    private final Object startupMonitor = new Object();
    private volatile Process ownedProcess;

    public CloudTurnSidecarLauncher(TurnClientProperties turnProperties,
                                    CloudTurnSidecarProperties sidecarProperties) {
        this.turnProperties = Objects.requireNonNull(turnProperties, "turnProperties");
        this.sidecarProperties = Objects.requireNonNull(sidecarProperties, "sidecarProperties");
    }

    /**
     * Starts the external local Cloud Brain when needed and waits until its configured TCP listener is ready.
     * This gate performs no task action, capture, or input.
     */
    public Readiness ensureReady() {
        return ensureReady(() -> false);
    }

    /**
     * Starts the local Cloud Brain if needed while allowing the owning start command to be cancelled.
     *
     * @param cancelled true once the exact start command has been superseded by pause/stop
     * @return readiness or a typed unavailable result when the start command was cancelled
     */
    public Readiness ensureReady(BooleanSupplier cancelled) {
        BooleanSupplier cancellation = Objects.requireNonNull(cancelled, "cancelled");
        if (cancellation.getAsBoolean()) {
            return Readiness.unavailable("远程启动已取消");
        }
        URI endpoint = turnProperties.getBaseUri();
        if (!isLoopback(endpoint)) {
            return Readiness.ready("远程 Cloud 地址不需要本地 sidecar");
        }
        if (isListening(endpoint)) {
            return Readiness.ready("Cloud Brain 已就绪");
        }
        if (!sidecarProperties.isAutoStartEnabled()) {
            return Readiness.unavailable("Cloud Brain 未运行，且自动启动已禁用");
        }

        synchronized (startupMonitor) {
            if (cancellation.getAsBoolean()) {
                return Readiness.unavailable("远程启动已取消");
            }
            if (isListening(endpoint)) {
                return Readiness.ready("Cloud Brain 已就绪");
            }
            try {
                if (ownedProcess == null || !ownedProcess.isAlive()) {
                    ownedProcess = startSidecar();
                }
                long timeoutMs = sidecarProperties.getStartupTimeoutMs();
                if (timeoutMs <= 0L) {
                    return Readiness.unavailable("Cloud Brain 启动超时配置无效");
                }
                long deadline = System.nanoTime() + Duration.ofMillis(timeoutMs).toNanos();
                while (System.nanoTime() < deadline) {
                    if (cancellation.getAsBoolean()) {
                        return Readiness.unavailable("远程启动已取消；Cloud Brain 可继续在后台就绪");
                    }
                    if (isListening(endpoint)) {
                        log.info("Cloud Brain sidecar ready: endpoint={} pid={}", endpoint, ownedProcess.pid());
                        return Readiness.ready("Cloud Brain 已自动启动");
                    }
                    if (!ownedProcess.isAlive()) {
                        return Readiness.unavailable("Cloud Brain 启动进程提前退出，exit=" + ownedProcess.exitValue()
                                + "，请查看 " + absolute(sidecarProperties.getLogPath()));
                    }
                    Thread.sleep(POLL_INTERVAL.toMillis());
                }
                return Readiness.unavailable("Cloud Brain 在 " + timeoutMs + "ms 内未就绪，请查看 "
                        + absolute(sidecarProperties.getLogPath()));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return Readiness.unavailable("等待 Cloud Brain 启动时被中断");
            } catch (Exception failure) {
                log.error("Cloud Brain sidecar start failed", failure);
                return Readiness.unavailable("Cloud Brain 自动启动失败：" + failure.getMessage());
            }
        }
    }

    private Process startSidecar() throws IOException, InterruptedException {
        Path script = absolute(sidecarProperties.getScriptPath());
        Path brainProject = absolute(sidecarProperties.getBrainProjectPath());
        Path logPath = absolute(sidecarProperties.getLogPath());
        String tenantId = requireText(sidecarProperties.getTenantId(), "cloud.turn.sidecar.tenant-id");
        String userId = configuredOrCurrentWindowsSid();
        Path stateRoot = configuredOrDefaultStateRoot();
        int ocrPort = sidecarProperties.getOcrPort();
        if (ocrPort <= 0 || ocrPort > 65_535) {
            throw new IOException("cloud.turn.sidecar.ocr-port 无效: " + ocrPort);
        }

        if (!Files.isRegularFile(script)) {
            throw new IOException("启动脚本不存在: " + script);
        }
        if (!Files.isDirectory(brainProject)) {
            throw new IOException("Cloud Brain 项目不存在: " + brainProject);
        }
        if (logPath.getParent() != null) {
            Files.createDirectories(logPath.getParent());
        }

        List<String> command = List.of(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden",
                "-File", script.toString(),
                "-BrainProjectPath", brainProject.toString(),
                "-TenantId", tenantId,
                "-UserId", userId,
                "-StateRoot", stateRoot.toString(),
                "-OcrPort", Integer.toString(ocrPort));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(Path.of(System.getProperty("user.dir")).toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()));
        Process process = builder.start();
        log.info("Cloud Brain sidecar launch requested: pid={} script={} tenantId={} userId={} stateRoot={} "
                        + "ocrPort={} log={}",
                process.pid(), script, tenantId, userId, stateRoot, ocrPort, logPath);
        return process;
    }

    private String configuredOrCurrentWindowsSid() throws IOException, InterruptedException {
        String configured = sidecarProperties.getUserId();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        Process process = new ProcessBuilder("whoami.exe", "/user", "/fo", "csv", "/nh")
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IOException("无法读取当前 Windows SID: " + output.trim());
        }
        Matcher matcher = WINDOWS_SID.matcher(output);
        if (!matcher.find()) {
            throw new IOException("当前 Windows SID 输出无法识别: " + output.trim());
        }
        return matcher.group();
    }

    private Path configuredOrDefaultStateRoot() throws IOException {
        Path configured = sidecarProperties.getStateRoot();
        if (configured != null) {
            return absolute(configured);
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            throw new IOException("LOCALAPPDATA 未配置，无法确定 Cloud state root");
        }
        return Path.of(localAppData, "DHXY", "cloud-brain", "state").toAbsolutePath().normalize();
    }

    private boolean isListening(URI endpoint) {
        int port = endpoint.getPort() > 0 ? endpoint.getPort() : 80;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), port), 500);
            return true;
        } catch (IOException unavailable) {
            return false;
        }
    }

    private static boolean isLoopback(URI endpoint) {
        if (endpoint == null || endpoint.getHost() == null) {
            return false;
        }
        String host = endpoint.getHost();
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "::1".equals(host);
    }

    private static Path absolute(Path path) {
        Objects.requireNonNull(path, "path");
        return path.isAbsolute() ? path.normalize() : Path.of(System.getProperty("user.dir")).resolve(path).normalize();
    }

    private static String requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must be configured");
        }
        return value.trim();
    }

    public record Readiness(boolean ready, String message) {
        static Readiness ready(String message) {
            return new Readiness(true, message);
        }

        static Readiness unavailable(String message) {
            return new Readiness(false, message);
        }
    }
}
