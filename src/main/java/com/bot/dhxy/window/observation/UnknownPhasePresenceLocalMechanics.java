package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/** Produces BR-DIALOG-001's paired Title/Dialog facts from one shared exact-HWND cycle frame. */
final class UnknownPhasePresenceLocalMechanics {

    static final String TITLE_INTEREST = "unknown-phase-title-presence";
    static final String DIALOG_INTEREST = "unknown-phase-dialog-presence";
    static final String DIALOG_FRAME_ROI = "unknown-phase-dialog-frame";
    static final long SAMPLE_PERIOD_MS = 500L;
    private static final double TITLE_THRESHOLD = 0.82D;
    private static final String TIANTING_TITLE_TEMPLATE = "images/template/tianting/tianting_title.png";
    private static final Rectangle TRACKER_RECT = new Rectangle(0, 100, 280, 604);
    private static final Rectangle DIALOG_RECT = new Rectangle(200, 250, 640, 300);

    private final CoordinateHelper coordinateHelper;
    private final DialogFramePresenceMechanics dialogPresence = new DialogFramePresenceMechanics();
    private LocalCombatSignalMechanics.CycleFrameCropper cropper;
    private BufferedImage titleTemplate;

    UnknownPhasePresenceLocalMechanics(CoordinateHelper coordinateHelper) {
        this.coordinateHelper = coordinateHelper;
    }

    UnknownPhasePresenceLocalMechanics(BufferedImage titleTemplate) {
        this.coordinateHelper = null;
        this.titleTemplate = titleTemplate;
    }

    void bindCycleFrameCropper(LocalCombatSignalMechanics.CycleFrameCropper cropper) {
        this.cropper = cropper;
    }

    Sample sample() {
        BufferedImage tracker = crop(TRACKER_RECT);
        BufferedImage dialog = crop(DIALOG_RECT);
        try {
            String title = presence(tracker, titleTemplate());
            boolean dialogFramePresent = dialog != null && dialogPresence.isPresent(dialog);
            String dialogValue = dialog == null
                    ? "unknown"
                    : dialogFramePresent ? "unknown" : "none";
            return new Sample(title, dialogValue,
                    dialogFramePresent ? encodePng(dialog) : null);
        } finally {
            if (tracker != null) tracker.flush();
            if (dialog != null) dialog.flush();
        }
    }

    void reset() {
        if (titleTemplate != null) {
            titleTemplate.flush();
            titleTemplate = null;
        }
    }

    private BufferedImage crop(Rectangle logicalRect) {
        if (cropper == null) return null;
        int[] rect = coordinateHelper == null
                ? new int[]{logicalRect.x, logicalRect.y, logicalRect.width, logicalRect.height}
                : coordinateHelper.getScaledRect(
                        logicalRect.x, logicalRect.y, logicalRect.width, logicalRect.height);
        return cropper.crop(rect);
    }

    private BufferedImage titleTemplate() {
        if (titleTemplate != null) return titleTemplate;
        try {
            titleTemplate = ImageIO.read(Path.of(TIANTING_TITLE_TEMPLATE).toFile());
        } catch (IOException ignored) {
            titleTemplate = null;
        }
        return titleTemplate;
    }

    private static String presence(BufferedImage frame, BufferedImage template) {
        if (frame == null || template == null) return "unknown";
        double score = ImageFinder.bestMatchScore(frame, template);
        return Double.isFinite(score) && score >= TITLE_THRESHOLD ? "present" : "absent";
    }

    private static byte[] encodePng(BufferedImage image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(16_384);
            return ImageIO.write(image, "png", output) ? output.toByteArray() : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    record Sample(String titlePresence, String dialogPresence, byte[] dialogPng) {
        Sample {
            dialogPng = dialogPng == null ? null : dialogPng.clone();
        }

        @Override
        public byte[] dialogPng() {
            return dialogPng == null ? null : dialogPng.clone();
        }
    }
}
