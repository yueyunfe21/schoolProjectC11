package com.bot.dhxy.capture;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
            log.debug("[capture-evidence] saved: path={} provider={} windowId={} rect=[{},{},{},{}]",
                    output, safeProvider, windowId, x1, y1, x2, y2);
        } catch (Exception e) {
            log.warn("[capture-evidence] save failed: path={} provider={} windowId={} reason={}",
                    output, safeProvider, windowId, e.getMessage(), e);
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
}
