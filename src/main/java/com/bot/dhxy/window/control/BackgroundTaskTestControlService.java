package com.bot.dhxy.window.control;

import com.bot.dhxy.config.BackgroundTaskTestProperties;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.ui.GameUiSettingsStore;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in control plane for real Wuhuan, Tianting, and Catch Ghost test runs without JavaFX interaction.
 *
 * <p>The service never implements task mechanics. Start commands scan/register the real bound game windows and then
 * enter the same {@link WindowTaskControlService} path used by the UI. Control files are accepted only for this JVM's
 * random session, preventing a stale request from a previous debug host from starting physical input.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackgroundTaskTestControlService {

    static final String HOST_FILE_NAME = "host.properties";
    static final String COMMAND_SUFFIX = ".command";
    static final String RESULT_SUFFIX = ".result";

    private final BackgroundTaskTestProperties properties;
    private final BotProperties botProperties;
    private final GameUiSettingsStore gameUiSettingsStore;
    private final GameWindowRegistrationService registrationService;
    private final WindowTaskControlService windowTaskControlService;
    private final ConfigurableApplicationContext applicationContext;

    private final String sessionId = UUID.randomUUID().toString();
    private final AtomicBoolean controlRunning = new AtomicBoolean(false);

    private volatile Path controlDirectory;
    private volatile WatchService watchService;
    private volatile FileChannel hostLockChannel;
    private volatile FileLock hostLock;
    private volatile Thread controlThread;

    /** Start a real Wuhuan run through the production window/task path, using the configured default of 100 runs. */
    public WindowTaskCommandResult startWuhuanTest() {
        return startWuhuanTest(properties.getDefaultMaxRuns());
    }

    /**
     * Start a real Wuhuan run through the production window/task path.
     *
     * @param maxRuns number of requested task rounds; must be positive for this bounded test entry.
     * @return the normal multi-window start result returned by the production control service.
     */
    public WindowTaskCommandResult startWuhuanTest(int maxRuns) {
        return startTest(TaskType.WUHuan_V2, maxRuns);
    }

    /** Start a real Tianting run through the production window/task path, using the configured default of 100 runs. */
    public WindowTaskCommandResult startTiantingTest() {
        return startTiantingTest(properties.getDefaultMaxRuns());
    }

    /**
     * Start a real Tianting run through the production window/task path.
     *
     * @param maxRuns number of requested task rounds; must be positive for this bounded test entry.
     * @return the normal multi-window start result returned by the production control service.
     */
    public WindowTaskCommandResult startTiantingTest(int maxRuns) {
        return startTest(TaskType.TIANTING, maxRuns);
    }

    /** Start a real Catch Ghost run through the production path, using the configured default run count. */
    public WindowTaskCommandResult startCatchGhostTest() {
        return startCatchGhostTest(properties.getDefaultMaxRuns());
    }

    /**
     * Start a real Catch Ghost run through the production window/task path.
     *
     * @param maxRuns number of requested task rounds; must be positive for this bounded test entry.
     * @return the normal multi-window start result returned by the production control service.
     */
    public WindowTaskCommandResult startCatchGhostTest(int maxRuns) {
        return startTest(TaskType.CATCH_GHOST, maxRuns);
    }

    /** Start a real Ghost King run through the production path, using the configured default run count. */
    public WindowTaskCommandResult startGhostKingTest() {
        return startGhostKingTest(properties.getDefaultMaxRuns());
    }

    /**
     * Start a real Ghost King run through the production window/task path.
     *
     * @param maxRuns number of requested task rounds; must be positive for this bounded test entry.
     * @return the normal multi-window start result returned by the production control service.
     */
    public WindowTaskCommandResult startGhostKingTest(int maxRuns) {
        return startTest(TaskType.GHOST_KING, maxRuns);
    }

    /** Return the current production window snapshots without taking input ownership. */
    public WindowSystemSnapshot status() {
        return windowTaskControlService.getSystemSnapshot();
    }

    /** Pause every active test window through the same production pause contract used by JavaFX. */
    public WindowTaskCommandResult pause() {
        return windowTaskControlService.pauseAll();
    }

    /** Resume every paused test window through the production hot-resume contract used by JavaFX. */
    public WindowTaskCommandResult resume() {
        return windowTaskControlService.resumeAll();
    }

    /** Stop every active test window through the production stop contract; this does not kill the JVM. */
    public WindowTaskCommandResult stop() {
        return windowTaskControlService.stopAll();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startControlHost() {
        if (!properties.isEnabled() || !controlRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            controlDirectory = Path.of(properties.getControlDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(controlDirectory);
            hostLockChannel = FileChannel.open(controlDirectory.resolve("host.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            hostLock = hostLockChannel.tryLock();
            if (hostLock == null) {
                throw new IllegalStateException("Another background task test control host owns " + controlDirectory);
            }
            watchService = FileSystems.getDefault().newWatchService();
            controlDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            writeHostFile();
        } catch (IOException startupFailure) {
            controlRunning.set(false);
            throw new IllegalStateException("Could not start background task test control host", startupFailure);
        }

        controlThread = new Thread(this::runControlLoop, "background-task-test-control");
        // This is the lifecycle owner for show-ui=false/auto-start=false control-host mode.
        controlThread.setDaemon(false);
        controlThread.start();
        log.info("G033 background task test control ready: sessionId={} directory={} defaultMaxRuns={}",
                sessionId, controlDirectory, properties.getDefaultMaxRuns());
    }

    @PreDestroy
    public void closeControlHost() {
        controlRunning.set(false);
        WatchService currentWatchService = watchService;
        if (currentWatchService != null) {
            try {
                currentWatchService.close();
            } catch (IOException ignored) {
                // Context shutdown has already made this host unavailable.
            }
        }
        Thread currentThread = controlThread;
        if (currentThread != null && currentThread != Thread.currentThread()) {
            currentThread.interrupt();
        }
        deleteOwnHostFile();
        FileLock currentLock = hostLock;
        if (currentLock != null) {
            try {
                currentLock.release();
            } catch (IOException ignored) {
                // Process shutdown releases the OS lock even when explicit release is unavailable.
            }
        }
        FileChannel currentChannel = hostLockChannel;
        if (currentChannel != null) {
            try {
                currentChannel.close();
            } catch (IOException ignored) {
                // Nothing remains to own after context shutdown.
            }
        }
    }

    private synchronized WindowTaskCommandResult startTest(TaskType taskType, int maxRuns) {
        if (maxRuns <= 0) {
            throw new IllegalArgumentException("maxRuns must be positive for a bounded background test");
        }
        List<WindowTaskSnapshot> snapshots = windowTaskControlService.getSnapshots();
        if (snapshots.stream().anyMatch(WindowTaskSnapshot::isBusy)) {
            return WindowTaskCommandResult.empty(
                    "已有窗口任务正在运行；请先 pause/resume 或 stop，不得叠加新的实机测试", snapshots);
        }

        // The no-UI host never builds MainWindowController, so load the same persisted settings explicitly.
        gameUiSettingsStore.loadInto(botProperties);

        int previousMaxRuns;
        if (taskType == TaskType.WUHuan_V2) {
            previousMaxRuns = botProperties.getWuhuanMaxRuns();
            botProperties.setWuhuanMaxRuns(maxRuns);
        } else if (taskType == TaskType.TIANTING) {
            previousMaxRuns = botProperties.getTiantingMaxRuns();
            botProperties.setTiantingMaxRuns(maxRuns);
        } else if (taskType == TaskType.CATCH_GHOST) {
            previousMaxRuns = botProperties.getCatchGhostMaxRuns();
            botProperties.setCatchGhostMaxRuns(maxRuns);
        } else if (taskType == TaskType.GHOST_KING) {
            previousMaxRuns = botProperties.getGhostKingMaxRuns();
            botProperties.setGhostKingMaxRuns(maxRuns);
        } else {
            throw new IllegalArgumentException("Unsupported background test task: " + taskType);
        }

        try {
            log.info("G065 starting no-UI live task test: task={} maxRuns={} doubleExperience={} "
                            + "healPetIntervalMs={} repairEquipmentIntervalMs={}",
                    taskType, maxRuns, botProperties.isDoubleExperienceClaimEnabled(),
                    botProperties.getXiuluoHealPetMaintenanceIntervalMs(),
                    botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs());
            return registrationService.scanRegisterAndStartIndependentWindows(taskType);
        } finally {
            if (taskType == TaskType.WUHuan_V2) {
                botProperties.setWuhuanMaxRuns(previousMaxRuns);
            } else if (taskType == TaskType.TIANTING) {
                botProperties.setTiantingMaxRuns(previousMaxRuns);
            } else if (taskType == TaskType.GHOST_KING) {
                botProperties.setGhostKingMaxRuns(previousMaxRuns);
            } else {
                botProperties.setCatchGhostMaxRuns(previousMaxRuns);
            }
        }
    }

    private void runControlLoop() {
        while (controlRunning.get()) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE
                            || !(event.context() instanceof Path relative)) {
                        continue;
                    }
                    Path request = controlDirectory.resolve(relative).normalize();
                    if (request.getParent().equals(controlDirectory)
                            && request.getFileName().toString().endsWith(COMMAND_SUFFIX)) {
                        processCommand(request);
                    }
                }
                if (!key.reset()) {
                    break;
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            } catch (java.nio.file.ClosedWatchServiceException closed) {
                break;
            } catch (RuntimeException commandLoopFailure) {
                log.error("G033 background control loop failure", commandLoopFailure);
            }
        }
        controlRunning.set(false);
    }

    private void processCommand(Path requestPath) {
        String fileName = requestPath.getFileName().toString();
        String requestId = fileName.substring(0, fileName.length() - COMMAND_SUFFIX.length());
        Path resultPath = controlDirectory.resolve(requestId + RESULT_SUFFIX);
        Properties request = new Properties();
        try (Reader reader = Files.newBufferedReader(requestPath, StandardCharsets.UTF_8)) {
            request.load(reader);
        } catch (IOException readFailure) {
            writeResult(resultPath, ControlResult.failed("无法读取控制命令：" + readFailure.getMessage()));
            return;
        }

        ControlResult result;
        boolean shutdown = false;
        try {
            if (!Objects.equals(sessionId, request.getProperty("sessionId", "").trim())) {
                result = ControlResult.failed("拒绝旧 session 或未知控制命令");
            } else {
                String action = request.getProperty("action", "").trim().toLowerCase(Locale.ROOT);
                int maxRuns = parseMaxRuns(request.getProperty("maxRuns"));
                switch (action) {
                    case "start-wuhuan" -> result = ControlResult.from(startWuhuanTest(maxRuns));
                    case "start-tianting" -> result = ControlResult.from(startTiantingTest(maxRuns));
                    case "start-catch-ghost" -> result = ControlResult.from(startCatchGhostTest(maxRuns));
                    case "start-ghost-king" -> result = ControlResult.from(startGhostKingTest(maxRuns));
                    case "pause" -> result = ControlResult.from(pause());
                    case "resume" -> result = ControlResult.from(resume());
                    case "stop" -> result = ControlResult.from(stop());
                    case "status" -> result = ControlResult.from(status());
                    case "shutdown" -> {
                        result = ControlResult.from(stop());
                        shutdown = true;
                    }
                    default -> result = ControlResult.failed("未知控制动作：" + action);
                }
            }
        } catch (RuntimeException commandFailure) {
            log.error("G033 background control command failed: request={}", requestPath, commandFailure);
            result = ControlResult.failed(commandFailure.getClass().getSimpleName() + ": " + commandFailure.getMessage());
        }

        writeResult(resultPath, result);
        try {
            Files.deleteIfExists(requestPath);
        } catch (IOException deleteFailure) {
            log.warn("G033 could not remove consumed control request: path={}", requestPath, deleteFailure);
        }
        if (shutdown) {
            controlRunning.set(false);
            Thread closer = new Thread(applicationContext::close, "background-task-test-shutdown");
            closer.setDaemon(false);
            closer.start();
        }
    }

    private int parseMaxRuns(String configured) {
        if (configured == null || configured.isBlank()) {
            return properties.getDefaultMaxRuns();
        }
        return Integer.parseInt(configured.trim());
    }

    private void writeHostFile() throws IOException {
        Properties host = new Properties();
        host.setProperty("sessionId", sessionId);
        host.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        host.setProperty("startedAt", Instant.now().toString());
        host.setProperty("directory", controlDirectory.toString());
        writePropertiesAtomically(controlDirectory.resolve(HOST_FILE_NAME), host);
    }

    private void writeResult(Path resultPath, ControlResult result) {
        Properties response = new Properties();
        response.setProperty("sessionId", sessionId);
        response.setProperty("completedAt", Instant.now().toString());
        response.setProperty("success", Boolean.toString(result.success()));
        response.setProperty("message", result.message());
        response.setProperty("requestedCount", Integer.toString(result.requestedCount()));
        response.setProperty("successCount", Integer.toString(result.successCount()));
        response.setProperty("registeredWindowCount", Integer.toString(result.registeredWindowCount()));
        response.setProperty("runningWindowCount", Integer.toString(result.runningWindowCount()));
        response.setProperty("windowCount", Integer.toString(result.snapshots().size()));
        for (int index = 0; index < result.snapshots().size(); index++) {
            WindowTaskSnapshot snapshot = result.snapshots().get(index);
            String prefix = "window." + index + ".";
            response.setProperty(prefix + "id", snapshot.getWindowId());
            response.setProperty(prefix + "roleName", snapshot.getRoleName());
            response.setProperty(prefix + "status", snapshot.getStatus().name());
            response.setProperty(prefix + "selectedTask", snapshot.getSelectedTaskType().name());
            response.setProperty(prefix + "runningTask", snapshot.getRunningTaskType().name());
            response.setProperty(prefix + "lastResult", Objects.toString(snapshot.getLastResult(), ""));
        }
        try {
            writePropertiesAtomically(resultPath, response);
        } catch (IOException writeFailure) {
            log.error("G033 could not write control result: path={}", resultPath, writeFailure);
        }
    }

    private static void writePropertiesAtomically(Path target, Properties values) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp-" + UUID.randomUUID());
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            values.store(writer, null);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteOwnHostFile() {
        Path directory = controlDirectory;
        if (directory == null) {
            return;
        }
        Path hostPath = directory.resolve(HOST_FILE_NAME);
        Properties host = new Properties();
        try (Reader reader = Files.newBufferedReader(hostPath, StandardCharsets.UTF_8)) {
            host.load(reader);
            if (sessionId.equals(host.getProperty("sessionId"))) {
                Files.deleteIfExists(hostPath);
            }
        } catch (IOException ignored) {
            // A missing or replaced host marker belongs to no live responsibility of this session.
        }
    }

    private record ControlResult(
            boolean success,
            String message,
            int requestedCount,
            int successCount,
            int registeredWindowCount,
            int runningWindowCount,
            List<WindowTaskSnapshot> snapshots) {

        private static ControlResult from(WindowTaskCommandResult result) {
            List<WindowTaskSnapshot> snapshots = result.getSnapshots();
            return new ControlResult(
                    result.getFailedCount() == 0,
                    result.getMessage(),
                    result.getRequestedCount(),
                    result.getSuccessCount(),
                    snapshots.size(),
                    (int) snapshots.stream().filter(WindowTaskSnapshot::isRunning).count(),
                    snapshots);
        }

        private static ControlResult from(WindowSystemSnapshot snapshot) {
            return new ControlResult(
                    true,
                    "状态读取完成",
                    0,
                    0,
                    snapshot.getRegisteredWindowCount(),
                    snapshot.getRunningWindowCount(),
                    snapshot.getWindows());
        }

        private static ControlResult failed(String message) {
            return new ControlResult(false, Objects.toString(message, ""), 0, 0, 0, 0, List.of());
        }
    }
}
