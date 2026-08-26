package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Baseline-equivalent raw-template handler for passive member maintenance broadcasts. */
@Component
public class OpenCvLocalMaintenanceBroadcastHandler implements LocalMaintenanceBroadcastHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenCvLocalMaintenanceBroadcastHandler.class);

    static final int ROI_LEFT = 260;
    static final int ROI_TOP = 373;
    static final int ROI_WIDTH = 118;
    static final int ROI_HEIGHT = 40;
    static final int SMALL_DIALOG_ROI_LEFT = 250;
    static final int SMALL_DIALOG_ROI_TOP = 312;
    static final int SMALL_DIALOG_ROI_WIDTH = 529;
    static final int SMALL_DIALOG_ROI_HEIGHT = 208;
    static final double MATCH_THRESHOLD = 0.85;
    private static final String DOUBLE_EXPERIENCE_ACTION = "double-experience-two-hours";
    private static final List<TemplateSpec> NARROW_TEMPLATE_SPECS = List.of(
            new TemplateSpec("heal-all-repair",
                    "images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png"),
            new TemplateSpec("repair-confirm",
                    "images/template/dialog/maintenance/maintenance_repair_confirm_raw.png"));
    private static final List<TemplateSpec> SMALL_DIALOG_TEMPLATE_SPECS = List.of(
            new TemplateSpec(DOUBLE_EXPERIENCE_ACTION,
                    "images/template/dialog/maintenance/lingshuang.png"));

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder contextHolder;
    private volatile List<LoadedTemplate> narrowTemplates;
    private volatile List<LoadedTemplate> smallDialogTemplates;

    public OpenCvLocalMaintenanceBroadcastHandler(GameClientTracker tracker,
                                                   CoordinateHelper coordinateHelper,
                                                   InputSequences inputSequences,
                                                   WindowTaskContextHolder contextHolder) {
        this.tracker = tracker;
        this.coordinateHelper = coordinateHelper;
        this.inputSequences = inputSequences;
        this.contextHolder = contextHolder;
    }

    @Override
    public boolean handleIfPresent() {
        if (handleInRegion("maintenance broadcast", ROI_LEFT, ROI_TOP, ROI_WIDTH, ROI_HEIGHT,
                loadedNarrowTemplates())) {
            return true;
        }
        return handleInRegion("double-experience broadcast", SMALL_DIALOG_ROI_LEFT,
                SMALL_DIALOG_ROI_TOP, SMALL_DIALOG_ROI_WIDTH, SMALL_DIALOG_ROI_HEIGHT,
                loadedSmallDialogTemplates());
    }

    /**
     * Wait briefly for the 一品侍卫 small dialog to render, then click only the raw “领取二小时”
     * template. This path never OCRs, washes, or uploads the captured dialog image.
     *
     * @param timeout maximum local render wait; null or non-positive performs one immediate sample
     * @return true only when the bound-window input queue completed the exact template click
     */
    public boolean handleDoubleExperienceIfPresent(Duration timeout) {
        long timeoutMs = timeout == null ? 0L : Math.max(0L, timeout.toMillis());
        long deadline = System.currentTimeMillis() + timeoutMs;
        do {
            if (handleInRegion("double-experience broadcast", SMALL_DIALOG_ROI_LEFT,
                    SMALL_DIALOG_ROI_TOP, SMALL_DIALOG_ROI_WIDTH, SMALL_DIALOG_ROI_HEIGHT,
                    loadedSmallDialogTemplates())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(Math.min(250L, Math.max(1L, deadline - System.currentTimeMillis())));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (true);
    }

    private boolean handleInRegion(String capturePurpose,
                                   int left,
                                   int top,
                                   int width,
                                   int height,
                                   List<LoadedTemplate> candidates) {
        int[] rect = coordinateHelper.getScaledRect(left, top, width, height);
        BufferedImage roi = tracker.captureToMemory(
                capturePurpose, rect[0], rect[1], rect[2], rect[3]);
        if (roi == null) {
            return false;
        }
        try {
            LocalMatch match = findMatch(roi, candidates);
            if (match == null) {
                return false;
            }
            Point absolute = new Point(
                    rect[0] + (int) Math.round(match.centerX()),
                    rect[1] + (int) Math.round(match.centerY()));
            boolean clicked = inputSequences.moveAndClickLeft(
                    "maintenance:broadcast:local:" + match.actionKey(),
                    absolute.x, absolute.y, 80, 150);
            if (clicked) {
                WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
                if (context != null) {
                    context.recordLocalMaintenanceBroadcastHandled(match.actionKey(), System.currentTimeMillis());
                }
                log.info("Local maintenance broadcast handled: windowId={} actionKey={} score={} click=({}, {}) roi=({}, {})-({}, {})",
                        context == null ? null : context.getWindowId(), match.actionKey(), match.score(), absolute.x, absolute.y,
                        rect[0], rect[1], rect[2], rect[3]);
            }
            return clicked;
        } finally {
            roi.flush();
        }
    }

    static LocalMatch findMatch(BufferedImage roi, List<LoadedTemplate> templates) {
        if (roi == null || templates == null) {
            return null;
        }
        for (LoadedTemplate template : templates) {
            double[] match = ImageFinder.find(roi, template.image(), MATCH_THRESHOLD);
            // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
            MatchEvidenceStore.saveOnChange(
                    "maintenance-broadcast-" + template.actionKey(), null, roi, template.image(), match);
            if (match != null && match.length >= 3) {
                return new LocalMatch(template.actionKey(), match[0], match[1], match[2]);
            }
        }
        return null;
    }

    private List<LoadedTemplate> loadedNarrowTemplates() {
        List<LoadedTemplate> current = narrowTemplates;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (narrowTemplates == null) {
                narrowTemplates = NARROW_TEMPLATE_SPECS.stream().map(this::loadTemplate).toList();
            }
            return narrowTemplates;
        }
    }

    private List<LoadedTemplate> loadedSmallDialogTemplates() {
        List<LoadedTemplate> current = smallDialogTemplates;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (smallDialogTemplates == null) {
                smallDialogTemplates = SMALL_DIALOG_TEMPLATE_SPECS.stream().map(this::loadTemplate).toList();
            }
            return smallDialogTemplates;
        }
    }

    private LoadedTemplate loadTemplate(TemplateSpec spec) {
        try {
            BufferedImage image = ImageIO.read(Path.of(spec.path()).toFile());
            if (image == null) {
                throw new IllegalStateException("maintenance template unreadable: " + spec.path());
            }
            return new LoadedTemplate(spec.actionKey(), image);
        } catch (IOException e) {
            throw new IllegalStateException("maintenance template load failed: " + spec.path(), e);
        }
    }

    record LocalMatch(String actionKey, double centerX, double centerY, double score) {
    }

    record LoadedTemplate(String actionKey, BufferedImage image) {
    }

    private record TemplateSpec(String actionKey, String path) {
    }
}
