package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the exact-window leader Alt+T and local-only team-return panel matcher. */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TeamReturnPanelLocalOperation {

    private static final int ROI_X = 314;
    private static final int ROI_Y = 279;
    private static final int ROI_WIDTH = 561;
    private static final int ROI_HEIGHT = 40;
    private static final double MATCH_THRESHOLD = 0.85D;
    private static final Path NOT_RETURNED_TEMPLATE =
            Path.of("images", "template", "team", "not_returned_yet.png");

    private final WindowTaskContextHolder contextHolder;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final BoundWindowCaptureService captureService;
    private final InputSequences inputSequences;
    private final WindowScopedTempPath windowScopedTempPath;
    private final Map<WindowBindingKey, PanelOwner> locallyOpenedPanels = new ConcurrentHashMap<>();

    /**
     * Opens the bound leader's team panel without capturing a frame.
     *
     * @return {@code OPENED} only after the serialized Alt+T input completed
     */
    public synchronized OpenResult openPanel(String businessTaskRunId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null
                ? null : bindingRefreshService.refreshAndCommit(context).orElse(null);
        PanelOwner requested = owner(context, binding, businessTaskRunId);
        if (requested == null) {
            return OpenResult.UNKNOWN;
        }
        PanelOwner existing = locallyOpenedPanels.get(requested.binding());
        if (requested.equals(existing)) {
            log.info("[team-return-panel] open already acknowledged: owner={}", requested);
            return OpenResult.OPENED;
        }
        if (existing != null) {
            if (!closePhysical(existing, "run-replaced")) {
                return OpenResult.UNKNOWN;
            }
            locallyOpenedPanels.remove(requested.binding(), existing);
        }
        boolean opened = inputSequences.submitAndWait(
                "team-return:leader-panel-open", List.of(InputAction.pressAltT()));
        if (opened) {
            locallyOpenedPanels.put(requested.binding(), requested);
        }
        log.info("[team-return-panel] open input: owner={} result={}", requested, opened);
        return opened ? OpenResult.OPENED : OpenResult.UNKNOWN;
    }

    /**
     * Probes the already-open exact-window panel and closes it only after a confirmed marker miss.
     *
     * @return fail-closed panel result; no image bytes cross the Client/Cloud boundary
     */
    public synchronized ProbeResult probeAndCloseIfComplete(String businessTaskRunId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null
                ? null : bindingRefreshService.refreshAndCommit(context).orElse(null);
        PanelOwner requested = owner(context, binding, businessTaskRunId);
        if (requested == null || !requested.equals(locallyOpenedPanels.get(requested.binding()))
                || !binding.hasGeometry()) {
            return ProbeResult.UNKNOWN;
        }
        BufferedImage roi = captureService.captureRegion(
                        binding, binding.getX(), binding.getY(),
                        binding.getX() + ROI_X, binding.getY() + ROI_Y,
                        binding.getX() + ROI_X + ROI_WIDTH, binding.getY() + ROI_Y + ROI_HEIGHT)
                .map(BoundWindowCaptureService.CaptureResult::image)
                .orElse(null);
        if (roi == null) {
            return ProbeResult.UNKNOWN;
        }
        BufferedImage template = null;
        try {
            template = ImageIO.read(NOT_RETURNED_TEMPLATE.toFile());
            if (template == null || template.getWidth() <= 0 || template.getHeight() <= 0
                    || template.getWidth() > roi.getWidth() || template.getHeight() > roi.getHeight()) {
                return ProbeResult.UNKNOWN;
            }
            double[] strongest = ImageFinder.find(roi, template, -1.0D);
            if (strongest == null || strongest.length < 3 || !Double.isFinite(strongest[2])) {
                return ProbeResult.UNKNOWN;
            }
            boolean notReturnedYet = strongest[2] >= MATCH_THRESHOLD;
            saveEvidence(roi, template, strongest, notReturnedYet);
            log.info("[team-return-panel] probe: windowId={} result={} score={} threshold={} roi=({},{} {}x{})",
                    context.getWindowId(),
                    notReturnedYet ? ProbeResult.NOT_RETURNED_YET : "MISS_PENDING_CLOSE",
                    strongest[2], MATCH_THRESHOLD, ROI_X, ROI_Y, ROI_WIDTH, ROI_HEIGHT);
            if (notReturnedYet) {
                return ProbeResult.NOT_RETURNED_YET;
            }
            boolean closed = closePhysical(requested, "all-returned");
            if (closed) {
                locallyOpenedPanels.remove(requested.binding(), requested);
            }
            return closed ? ProbeResult.ALL_RETURNED : ProbeResult.UNKNOWN;
        } catch (IOException | RuntimeException failure) {
            log.warn("[team-return-panel] probe failed: windowId={} reason={}",
                    context.getWindowId(), failure.getMessage(), failure);
            return ProbeResult.UNKNOWN;
        } finally {
            if (template != null) {
                template.flush();
            }
            roi.flush();
        }
    }

    /**
     * Closes only the panel physically owned by the exact window/HWND/task run.
     *
     * @param businessTaskRunId exact Cloud business task-run identity; never null or blank
     * @return idempotent close result; another run's panel is never toggled
     */
    public synchronized CloseResult closePanel(String businessTaskRunId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null
                ? null : bindingRefreshService.refreshAndCommit(context).orElse(null);
        PanelOwner requested = owner(context, binding, businessTaskRunId);
        if (requested == null) {
            return CloseResult.UNKNOWN;
        }
        PanelOwner existing = locallyOpenedPanels.get(requested.binding());
        if (existing == null) {
            return CloseResult.CLOSED;
        }
        if (!requested.equals(existing)) {
            log.info("[team-return-panel] close ignored for non-owner: requested={} actual={}",
                    requested, existing);
            return CloseResult.NOT_OWNED;
        }
        if (!closePhysical(requested, "lifecycle-release")) {
            return CloseResult.UNKNOWN;
        }
        locallyOpenedPanels.remove(requested.binding(), requested);
        return CloseResult.CLOSED;
    }

    private boolean closePhysical(PanelOwner owner, String reason) {
        boolean closed = inputSequences.submitAndWait(
                "team-return:leader-panel-close:" + reason, List.of(InputAction.pressAltT()));
        log.info("[team-return-panel] close input: owner={} reason={} result={}", owner, reason, closed);
        return closed;
    }

    private static PanelOwner owner(WindowRuntimeContext context,
                                    WindowNativeBinding binding,
                                    String businessTaskRunId) {
        if (context == null || binding == null || !binding.hasNativeHandle()
                || businessTaskRunId == null || businessTaskRunId.isBlank()) {
            return null;
        }
        return new PanelOwner(
                new WindowBindingKey(context.getWindowId(), binding.getNativeHandle()),
                businessTaskRunId.trim());
    }

    private void saveEvidence(BufferedImage roi, BufferedImage template, double[] strongest, boolean matched) {
        Path raw = Path.of(windowScopedTempPath.resolve("team_return_roi_latest.png"));
        Path marked = Path.of(windowScopedTempPath.resolve("team_return_roi_latest_marked.png"));
        try {
            Files.createDirectories(raw.getParent());
            ImageIO.write(roi, "png", raw.toFile());
            BufferedImage evidence = new BufferedImage(roi.getWidth(), roi.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = evidence.createGraphics();
            try {
                graphics.drawImage(roi, 0, 0, null);
                graphics.setColor(matched ? Color.RED : Color.ORANGE);
                int left = (int) Math.round(strongest[0] - template.getWidth() / 2.0D);
                int top = (int) Math.round(strongest[1] - template.getHeight() / 2.0D);
                graphics.drawRect(left, top, template.getWidth(), template.getHeight());
                graphics.fillOval((int) Math.round(strongest[0]) - 2,
                        (int) Math.round(strongest[1]) - 2, 5, 5);
            } finally {
                graphics.dispose();
            }
            try {
                ImageIO.write(evidence, "png", marked.toFile());
            } finally {
                evidence.flush();
            }
        } catch (IOException evidenceFailure) {
            log.warn("[team-return-panel] evidence save failed: path={} reason={}",
                    raw, evidenceFailure.getMessage());
        }
    }

    public enum OpenResult { OPENED, UNKNOWN }

    public enum ProbeResult { NOT_RETURNED_YET, ALL_RETURNED, UNKNOWN }

    public enum CloseResult { CLOSED, NOT_OWNED, UNKNOWN }

    private record WindowBindingKey(String windowId, String hwnd) {}

    private record PanelOwner(WindowBindingKey binding, String businessTaskRunId) {}
}
