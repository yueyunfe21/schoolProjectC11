package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Read-only 五环 scene sensing for the local Runner.
 *
 * <p>This class deliberately reports only mechanical presence. It never decides what a title or dialog means,
 * never chooses a link/option, and never produces input. Cloud remains the owner of all 五环 recognition and
 * phase decisions; a title hit merely authorizes one current Tracker frame to travel with the fact.</p>
 */
final class WuhuanPresenceLocalMechanics {

    static final String TITLE_INTEREST = "wuhuan-title-presence";
    static final String DIALOG_INTEREST = "wuhuan-dialog-presence";
    static final String TITLE_FRAME_ROI = "wuhuan-title-frame";
    static final long SAMPLE_PERIOD_MS = 1_000L;
    private static final double TITLE_THRESHOLD = 0.82D;
    private static final String TITLE_TEMPLATE = "images/template/wuhuan/panel_title_yellow.png";
    private static final int TRACKER_LEFT = 0;
    private static final int TRACKER_TOP = 100;
    private static final int TRACKER_WIDTH = 280;
    private static final int TRACKER_HEIGHT = 604;
    private static final int DIALOG_LEFT = 200;
    private static final int DIALOG_TOP = 250;
    private static final int DIALOG_WIDTH = 640;
    private static final int DIALOG_HEIGHT = 300;

    private final CoordinateHelper coordinateHelper;
    private final DialogFramePresenceMechanics dialogPresence = new DialogFramePresenceMechanics();
    private LocalCombatSignalMechanics.CycleFrameCropper cropper;
    private BufferedImage titleTemplate;

    WuhuanPresenceLocalMechanics(CoordinateHelper coordinateHelper) {
        this.coordinateHelper = coordinateHelper;
    }

    void bindCycleFrameCropper(LocalCombatSignalMechanics.CycleFrameCropper cropper) {
        this.cropper = cropper;
    }

    /** Samples one shared exact-HWND frame without invoking another capture. */
    Sample sample(boolean sampleTitle, boolean sampleDialog) {
        boolean titleSampled = false;
        boolean titlePresent = false;
        byte[] trackerPng = null;
        byte[] trackerMissPng = null;
        double titleScore = Double.NaN;
        if (sampleTitle) {
            BufferedImage tracker = crop(TRACKER_LEFT, TRACKER_TOP, TRACKER_WIDTH, TRACKER_HEIGHT);
            if (tracker != null) {
                try {
                    titleSampled = true;
                    BufferedImage template = titleTemplate();
                    titleScore = ImageFinder.bestMatchScore(tracker, template);
                    titlePresent = Double.isFinite(titleScore) && titleScore >= TITLE_THRESHOLD;
                    if (titlePresent) {
                        trackerPng = encodePng(tracker);
                    } else if (template != null) {
                        trackerMissPng = encodePng(tracker);
                    }
                } finally {
                    tracker.flush();
                }
            }
        }
        boolean dialogSampled = false;
        boolean dialogPresent = false;
        if (sampleDialog) {
            BufferedImage dialog = crop(DIALOG_LEFT, DIALOG_TOP, DIALOG_WIDTH, DIALOG_HEIGHT);
            if (dialog != null) {
                try {
                    dialogSampled = true;
                    dialogPresent = dialogPresence.isPresent(dialog);
                } finally {
                    dialog.flush();
                }
            }
        }
        return new Sample(titleSampled, titlePresent, dialogSampled, dialogPresent,
                trackerPng, trackerMissPng, titleScore);
    }

    void reset() {
        if (titleTemplate != null) {
            titleTemplate.flush();
            titleTemplate = null;
        }
    }

    private BufferedImage crop(int left, int top, int width, int height) {
        LocalCombatSignalMechanics.CycleFrameCropper current = cropper;
        return current == null ? null : current.crop(coordinateHelper.getScaledRect(left, top, width, height));
    }

    private BufferedImage titleTemplate() {
        if (titleTemplate != null) {
            return titleTemplate;
        }
        try {
            titleTemplate = ImageIO.read(Path.of(TITLE_TEMPLATE).toFile());
        } catch (IOException ignored) {
            titleTemplate = null;
        }
        return titleTemplate;
    }

    private static byte[] encodePng(BufferedImage image) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(16_384)) {
            ImageIO.write(image, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException ignored) {
            return null;
        }
    }

    record Sample(boolean titleSampled,
                  boolean titlePresent,
                  boolean dialogSampled,
                  boolean dialogPresent,
                  byte[] trackerPng,
                  byte[] trackerMissPng,
                  double titleScore) {
    }
}
