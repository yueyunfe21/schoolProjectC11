package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.tools.CoordinateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Baseline-equivalent raw-template handler for passive member maintenance broadcasts. */
@Component
public class OpenCvLocalMaintenanceBroadcastHandler implements LocalMaintenanceBroadcastHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenCvLocalMaintenanceBroadcastHandler.class);

    static final int ROI_LEFT = 260;
    static final int ROI_TOP = 373;
    static final int ROI_WIDTH = 118;
    static final int ROI_HEIGHT = 40;
    static final double MATCH_THRESHOLD = 0.85;
    private static final List<TemplateSpec> TEMPLATE_SPECS = List.of(
            new TemplateSpec("heal-all-repair",
                    "images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png"),
            new TemplateSpec("repair-confirm",
                    "images/template/dialog/maintenance/maintenance_repair_confirm_raw.png"));

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private volatile List<LoadedTemplate> templates;

    public OpenCvLocalMaintenanceBroadcastHandler(GameClientTracker tracker,
                                                   CoordinateHelper coordinateHelper,
                                                   InputSequences inputSequences) {
        this.tracker = tracker;
        this.coordinateHelper = coordinateHelper;
        this.inputSequences = inputSequences;
    }

    @Override
    public boolean handleIfPresent() {
        int[] rect = coordinateHelper.getScaledRect(ROI_LEFT, ROI_TOP, ROI_WIDTH, ROI_HEIGHT);
        BufferedImage roi = tracker.captureToMemory(
                "local maintenance broadcast", rect[0], rect[1], rect[2], rect[3]);
        if (roi == null) {
            return false;
        }
        try {
            LocalMatch match = findMatch(roi, loadedTemplates());
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
                log.info("Local maintenance broadcast handled: actionKey={} score={} click=({}, {}) roi=({}, {})-({}, {})",
                        match.actionKey(), match.score(), absolute.x, absolute.y,
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
            if (match != null && match.length >= 3) {
                return new LocalMatch(template.actionKey(), match[0], match[1], match[2]);
            }
        }
        return null;
    }

    private List<LoadedTemplate> loadedTemplates() {
        List<LoadedTemplate> current = templates;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (templates == null) {
                templates = TEMPLATE_SPECS.stream().map(this::loadTemplate).toList();
            }
            return templates;
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
