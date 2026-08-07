package com.bot.dhxy.capture;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronously preserves every successful local raw capture for later incident replay.
 *
 * <p>The store is deliberately mechanical: it neither filters screenshots nor changes the capture result when
 * evidence persistence fails. The caller still owns capture success/failure semantics; a failed evidence write is
 * logged because it leaves a diagnostic gap that must be visible.</p>
 */
@Slf4j
@Component
public class WindowCaptureEvidenceStore {

    private static final Path ROOT = Path.of("images", "captures");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final WindowTaskContextHolder contextHolder;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean retentionSweepScheduled = new AtomicBoolean();
    private final ExecutorService retentionSweepExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dhxy-capture-retention");
        thread.setDaemon(true);
        return thread;
    });
    private volatile LocalDate lastRetentionSweepDay;

    public WindowCaptureEvidenceStore(WindowTaskContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    /**
     * Persist one unmodified raw capture before its caller returns it to a task or HTTPS turn.
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
        LocalDateTime now = LocalDateTime.now();
        scheduleRetentionSweep(now.toLocalDate());
        String windowId = currentWindowId(binding);
        String safeProvider = sanitize(provider == null ? "unknown" : provider);
        long captureSequence = sequence.incrementAndGet();
        int width = Math.max(0, x2 - x1);
        int height = Math.max(0, y2 - y1);
        Path output = ROOT.resolve(DAY.format(now)).resolve(windowId).resolve(
                STAMP.format(now) + "_" + captureSequence + "_" + safeProvider
                        + "_" + x1 + "_" + y1 + "_" + width + "x" + height + ".png");
        try {
            Files.createDirectories(output.getParent());
            if (!ImageIO.write(image, "png", output.toFile())) {
                log.warn("[capture-evidence] PNG writer unavailable: path={} provider={} windowId={}",
                        output, safeProvider, windowId);
                return;
            }
            writeMetadata(output, now, windowId, binding, safeProvider, x1, y1, x2, y2, image);
            log.debug("[capture-evidence] saved: path={} provider={} windowId={} rect=[{},{},{},{}]",
                    output, safeProvider, windowId, x1, y1, x2, y2);
        } catch (Exception e) {
            log.warn("[capture-evidence] save failed: path={} provider={} windowId={} reason={}",
                    output, safeProvider, windowId, e.getMessage(), e);
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
            log.warn("[capture-evidence] retention sweep scheduling failed: root={} reason={}", ROOT, e.getMessage(), e);
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
            if (!Files.isDirectory(ROOT)) {
                lastRetentionSweepDay = today;
                return;
            }
            try (var days = Files.list(ROOT)) {
                days.filter(Files::isDirectory)
                        .filter(day -> isOlderEvidenceDay(day, today))
                        .forEach(this::deleteEvidenceDay);
            }
            lastRetentionSweepDay = today;
        } catch (Exception e) {
            log.warn("[capture-evidence] previous-day cleanup failed: root={} reason={}", ROOT, e.getMessage(), e);
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
    private void writeMetadata(Path imagePath,
                               LocalDateTime capturedAt,
                               String windowId,
                               WindowNativeBinding binding,
                               String provider,
                               int x1,
                               int y1,
                               int x2,
                               int y2,
                               BufferedImage image) {
        Optional<WindowRuntimeContext> current = contextHolder.rawCurrent();
        String task = current.map(context -> String.valueOf(context.getSelectedTaskType())).orElse("UNKNOWN");
        String role = current.map(context -> String.valueOf(context.getRole())).orElse("UNKNOWN");
        String status = current.map(context -> String.valueOf(context.getStatus())).orElse("UNKNOWN");
        String metadata = "capturedAt=" + capturedAt + System.lineSeparator()
                + "windowId=" + windowId + System.lineSeparator()
                + "task=" + task + System.lineSeparator()
                + "role=" + role + System.lineSeparator()
                + "status=" + status + System.lineSeparator()
                + "provider=" + provider + System.lineSeparator()
                + "hwnd=" + (binding == null ? "" : binding.getNativeHandle()) + System.lineSeparator()
                + "title=" + (binding == null ? "" : binding.getTitle()) + System.lineSeparator()
                + "rect=" + x1 + "," + y1 + "," + x2 + "," + y2 + System.lineSeparator()
                + "image=" + image.getWidth() + "x" + image.getHeight() + System.lineSeparator();
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
    public void shutdownRetentionSweepExecutor() {
        retentionSweepExecutor.shutdownNow();
    }

    private static final class EvidenceCleanupException extends RuntimeException {
        private final Path path;

        private EvidenceCleanupException(Path path, Exception cause) {
            super(cause);
            this.path = path;
        }
    }
}
