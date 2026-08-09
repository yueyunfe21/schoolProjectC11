package com.bot.dhxy.capture;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Preserves leader and solo raw captures for later incident replay without blocking the capture caller on PNG IO.
 *
 * <p>The caller freezes the pixels and capture metadata, then a small low-priority writer pool performs PNG encoding
 * and disk IO. The store never changes the capture result when evidence persistence fails. A failed evidence write is
 * logged because it leaves a diagnostic gap that must be
 * visible. Member captures are intentionally excluded because those windows only run local combat maintenance and
 * their high-frequency evidence must not compete with the leader's task workflow.</p>
 */
@Slf4j
@Component
public class WindowCaptureEvidenceStore {

    private static final Path DEFAULT_ROOT = Path.of("images", "captures");
    private static final int WRITER_THREADS = 2;
    private static final long SHUTDOWN_DRAIN_SECONDS = 30L;
    private static final long BACKLOG_WARNING_STEP = 100L;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final WindowTaskContextHolder contextHolder;
    private final Path root;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong pendingWrites = new AtomicLong();
    private final AtomicLong lastWarnedBacklog = new AtomicLong();
    private final AtomicBoolean retentionSweepScheduled = new AtomicBoolean();
    private final ExecutorService evidenceWriteExecutor;
    private final ExecutorService retentionSweepExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dhxy-capture-retention");
        thread.setDaemon(true);
        return thread;
    });
    private volatile LocalDate lastRetentionSweepDay;

    @Autowired
    public WindowCaptureEvidenceStore(WindowTaskContextHolder contextHolder) {
        this(contextHolder, DEFAULT_ROOT, createEvidenceWriteExecutor());
    }

    WindowCaptureEvidenceStore(WindowTaskContextHolder contextHolder,
                               Path root,
                               ExecutorService evidenceWriteExecutor) {
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.root = Objects.requireNonNull(root, "root");
        this.evidenceWriteExecutor = Objects.requireNonNull(evidenceWriteExecutor, "evidenceWriteExecutor");
    }

    /**
     * Freeze one unmodified leader/solo raw capture and enqueue its PNG/meta persistence before returning to the caller.
     *
     * @param image captured pixels; null is ignored because no successful capture exists.
     * @param binding exact HWND binding used for the capture when available.
     * @param provider capture provider label, for example {@code HWND_PRINTWINDOW} or {@code ROBOT}.
     * @param x1 screen-absolute left edge of the captured ROI.
     * @param y1 screen-absolute top edge of the captured ROI.
     * @param x2 screen-absolute right edge of the captured ROI.
     * @param y2 screen-absolute bottom edge of the captured ROI.
     */
    public void persist(BufferedImage image,
                        WindowNativeBinding binding,
                        String provider,
                        int x1,
                        int y1,
                        int x2,
                        int y2) {
        if (image == null) {
            return;
        }
        if (contextHolder.rawCurrent()
                .map(WindowRuntimeContext::getRole)
                .filter(WindowRole.MEMBER::equals)
                .isPresent()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        scheduleRetentionSweep(now.toLocalDate());
        String windowId = currentWindowId(binding);
        String safeProvider = sanitize(provider == null ? "unknown" : provider);
        long captureSequence = sequence.incrementAndGet();
        int width = Math.max(0, x2 - x1);
        int height = Math.max(0, y2 - y1);
        Path output = root.resolve(DAY.format(now)).resolve(windowId).resolve(
                STAMP.format(now) + "_" + captureSequence + "_" + safeProvider
                        + "_" + x1 + "_" + y1 + "_" + width + "x" + height + ".png");
        BufferedImage frozenImage = copyImage(image);
        EvidenceMetadata metadata = currentMetadata(now, windowId, binding, safeProvider, x1, y1, x2, y2,
                frozenImage.getWidth(), frozenImage.getHeight());
        long queued = pendingWrites.incrementAndGet();
        warnOnBacklog(queued);
        try {
            evidenceWriteExecutor.execute(() -> writeEvidence(output, frozenImage, metadata));
        } catch (RuntimeException schedulingFailure) {
            pendingWrites.decrementAndGet();
            frozenImage.flush();
            log.warn("[capture-evidence] save scheduling failed: path={} provider={} windowId={} reason={}",
                    output, safeProvider, windowId, schedulingFailure.getMessage(), schedulingFailure);
        }
    }

    private void writeEvidence(Path output, BufferedImage image, EvidenceMetadata metadata) {
        long startedAt = System.nanoTime();
        try {
            Files.createDirectories(output.getParent());
            if (!ImageIO.write(image, "png", output.toFile())) {
                log.warn("[capture-evidence] PNG writer unavailable: path={} provider={} windowId={}",
                        output, metadata.provider(), metadata.windowId());
                return;
            }
            writeMetadata(output, metadata);
            log.debug("[capture-evidence] saved: path={} provider={} windowId={} rect=[{},{},{},{}] elapsedMs={}",
                    output, metadata.provider(), metadata.windowId(), metadata.x1(), metadata.y1(),
                    metadata.x2(), metadata.y2(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        } catch (Exception e) {
            log.warn("[capture-evidence] save failed: path={} provider={} windowId={} reason={}",
                    output, metadata.provider(), metadata.windowId(), e.getMessage(), e);
        } finally {
            image.flush();
            pendingWrites.decrementAndGet();
        }
    }

    /**
     * Starts the once-per-day retention sweep away from capture and observation threads.
     *
     * <p>Saving raw evidence is on the critical Runner path. Retention must never delay the first capture of a
     * task run, even when several old daily directories contain many PNG files.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleStartupRetentionSweep() {
        scheduleRetentionSweep(LocalDate.now());
    }

    private void scheduleRetentionSweep(LocalDate today) {
        if (today.equals(lastRetentionSweepDay) || !retentionSweepScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            retentionSweepExecutor.execute(() -> {
                try {
                    purgePreviousDays(today);
                } finally {
                    retentionSweepScheduled.set(false);
                }
            });
        } catch (RuntimeException e) {
            retentionSweepScheduled.set(false);
            log.warn("[capture-evidence] retention sweep scheduling failed: root={} reason={}", root, e.getMessage(), e);
        }
    }

    /**
     * Retain every image from the current local calendar day and remove only parseable evidence-day directories
     * that are older. A failed cleanup never affects a capture write.
     */
    private synchronized void purgePreviousDays(LocalDate today) {
        if (today.equals(lastRetentionSweepDay)) {
            return;
        }
        try {
            if (!Files.isDirectory(root)) {
                lastRetentionSweepDay = today;
                return;
            }
            try (var days = Files.list(root)) {
                days.filter(Files::isDirectory)
                        .filter(day -> isOlderEvidenceDay(day, today))
                        .forEach(this::deleteEvidenceDay);
            }
            lastRetentionSweepDay = today;
        } catch (Exception e) {
            log.warn("[capture-evidence] previous-day cleanup failed: root={} reason={}", root, e.getMessage(), e);
        }
    }

    private boolean isOlderEvidenceDay(Path candidate, LocalDate today) {
        try {
            return LocalDate.parse(candidate.getFileName().toString(), DAY).isBefore(today);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void deleteEvidenceDay(Path day) {
        try (var paths = Files.walk(day)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    throw new EvidenceCleanupException(path, e);
                }
            });
            log.info("[capture-evidence] removed expired evidence day: {}", day);
        } catch (EvidenceCleanupException e) {
            log.warn("[capture-evidence] failed to remove expired evidence: path={} reason={}",
                    e.path, e.getCause().getMessage(), e.getCause());
        } catch (Exception e) {
            log.warn("[capture-evidence] failed to scan expired evidence: path={} reason={}",
                    day, e.getMessage(), e);
        }
    }

    /** Writes one sidecar per raw PNG so a screenshot remains independently inspectable after a long run. */
    private void writeMetadata(Path imagePath, EvidenceMetadata evidence) {
        String metadata = "capturedAt=" + evidence.capturedAt() + System.lineSeparator()
                + "windowId=" + evidence.windowId() + System.lineSeparator()
                + "task=" + evidence.task() + System.lineSeparator()
                + "role=" + evidence.role() + System.lineSeparator()
                + "status=" + evidence.status() + System.lineSeparator()
                + "provider=" + evidence.provider() + System.lineSeparator()
                + "hwnd=" + evidence.hwnd() + System.lineSeparator()
                + "title=" + evidence.title() + System.lineSeparator()
                + "rect=" + evidence.x1() + "," + evidence.y1() + "," + evidence.x2() + "," + evidence.y2()
                + System.lineSeparator()
                + "image=" + evidence.imageWidth() + "x" + evidence.imageHeight() + System.lineSeparator();
        Path sidecar = imagePath.resolveSibling(imagePath.getFileName().toString().replaceFirst("\\.png$", ".meta"));
        try {
            Files.writeString(sidecar, metadata, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (Exception e) {
            log.warn("[capture-evidence] metadata save failed: image={} sidecar={} reason={}",
                    imagePath, sidecar, e.getMessage(), e);
        }
    }

    private String currentWindowId(WindowNativeBinding binding) {
        Optional<WindowRuntimeContext> current = contextHolder.rawCurrent();
        if (current.isPresent()) {
            return sanitize(current.get().getWindowId());
        }
        if (binding != null && binding.hasNativeHandle()) {
            return "unbound_" + sanitize(binding.getNativeHandle());
        }
        return "unbound";
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @PreDestroy
    public void shutdownExecutors() {
        evidenceWriteExecutor.shutdown();
        boolean interrupted = false;
        try {
            if (!evidenceWriteExecutor.awaitTermination(SHUTDOWN_DRAIN_SECONDS, TimeUnit.SECONDS)) {
                log.warn("[capture-evidence] writer did not drain before shutdown: pending={}", pendingWrites.get());
                evidenceWriteExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            interrupted = true;
            evidenceWriteExecutor.shutdownNow();
        }
        retentionSweepExecutor.shutdownNow();
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private EvidenceMetadata currentMetadata(LocalDateTime capturedAt,
                                             String windowId,
                                             WindowNativeBinding binding,
                                             String provider,
                                             int x1,
                                             int y1,
                                             int x2,
                                             int y2,
                                             int imageWidth,
                                             int imageHeight) {
        Optional<WindowRuntimeContext> current = contextHolder.rawCurrent();
        return new EvidenceMetadata(
                capturedAt,
                windowId,
                current.map(context -> String.valueOf(context.getSelectedTaskType())).orElse("UNKNOWN"),
                current.map(context -> String.valueOf(context.getRole())).orElse("UNKNOWN"),
                current.map(context -> String.valueOf(context.getStatus())).orElse("UNKNOWN"),
                provider,
                binding == null || binding.getNativeHandle() == null ? "" : binding.getNativeHandle(),
                binding == null ? "" : binding.getTitle(),
                x1,
                y1,
                x2,
                y2,
                imageWidth,
                imageHeight);
    }

    private void warnOnBacklog(long pending) {
        if (pending < BACKLOG_WARNING_STEP) {
            return;
        }
        long bucket = pending / BACKLOG_WARNING_STEP;
        long previous = lastWarnedBacklog.get();
        if (bucket > previous && lastWarnedBacklog.compareAndSet(previous, bucket)) {
            log.warn("[capture-evidence] writer backlog growing: pending={}", pending);
        }
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static ExecutorService createEvidenceWriteExecutor() {
        AtomicLong threadSequence = new AtomicLong();
        return Executors.newFixedThreadPool(WRITER_THREADS, runnable -> {
            Thread thread = new Thread(runnable,
                    "dhxy-capture-evidence-" + threadSequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    private record EvidenceMetadata(
            LocalDateTime capturedAt,
            String windowId,
            String task,
            String role,
            String status,
            String provider,
            String hwnd,
            String title,
            int x1,
            int y1,
            int x2,
            int y2,
            int imageWidth,
            int imageHeight) {
    }

    private static final class EvidenceCleanupException extends RuntimeException {
        private final Path path;

        private EvidenceCleanupException(Path path, Exception cause) {
            super(cause);
            this.path = path;
        }
    }
}
