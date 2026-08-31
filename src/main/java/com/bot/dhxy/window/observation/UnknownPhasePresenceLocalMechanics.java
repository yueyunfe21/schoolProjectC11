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
    /** Fact value format published alongside the presence word: {@code left,top,right,bottom}. */
    static final String FRAME_BOUNDS_FORMAT = "left,top,right,bottom";
    static final long SAMPLE_PERIOD_MS = 500L;
    private static final double TITLE_THRESHOLD = 0.82D;
    private static final String TIANTING_TITLE_TEMPLATE = "images/template/tianting/tianting_title.png";
    /**
     * G126（用户拍板）：标题里带"完成"两个字的一律作废。新手号常驻元任务 完成天庭任务[新手任务]
     * 含与标题模板相同的"天庭任务"四字（裸模板 0.9662 假阳性），presence 因此恒报 present、
     * 云端永远不去接任务。真任务标题都不含"完成"；命中一处遮一行后再判 presence。
     */
    private static final String WANCHENG_VOID_TEMPLATE = "images/template/tracker/wancheng_title_void.png";
    private static final int WANCHENG_MASK_HALF_HEIGHT = 10;
    private static final int WANCHENG_MASK_MAX_PASSES = 8;
    private static final Rectangle TRACKER_RECT = new Rectangle(0, 100, 280, 604);
    private static final Rectangle DIALOG_RECT = new Rectangle(200, 250, 640, 300);

    private final CoordinateHelper coordinateHelper;
    private final DialogFramePresenceMechanics dialogPresence = new DialogFramePresenceMechanics();
    private LocalCombatSignalMechanics.CycleFrameCropper cropper;
    private BufferedImage titleTemplate;
    private BufferedImage wanchengVoidTemplate;
    private boolean wanchengVoidLoaded;

    UnknownPhasePresenceLocalMechanics(CoordinateHelper coordinateHelper) {
        this.coordinateHelper = coordinateHelper;
    }

    UnknownPhasePresenceLocalMechanics(BufferedImage titleTemplate) {
        this(titleTemplate, null);
    }

    /** G126 测试缝：标题模板与"完成"作废模板都可注入。 */
    UnknownPhasePresenceLocalMechanics(BufferedImage titleTemplate, BufferedImage wanchengVoidTemplate) {
        this.coordinateHelper = null;
        this.titleTemplate = titleTemplate;
        this.wanchengVoidTemplate = wanchengVoidTemplate;
        this.wanchengVoidLoaded = true;
    }

    void bindCycleFrameCropper(LocalCombatSignalMechanics.CycleFrameCropper cropper) {
        this.cropper = cropper;
    }

    Sample sample() {
        BufferedImage tracker = crop(TRACKER_RECT);
        BufferedImage dialog = crop(DIALOG_RECT);
        try {
            String title = presence(maskWanchengVoidTitles(tracker), titleTemplate());
            DialogFramePresenceMechanics.FramePresence frame = dialogPresence.analyze(dialog);
            boolean dialogFramePresent = dialog != null && frame.present();
            String dialogValue = dialog == null
                    ? "unknown"
                    : dialogFramePresent ? "unknown" : "none";
            /*
             * 框在时把矩形一起报上去：presence 归本地判，云端只消费，不许再对同一张图做第二次
             * 视觉判断（2026-08-28 实锤：云端那套 DialogFrameClassifier 与本地结论相反）。
             */
            String bounds = dialogFramePresent
                    ? frame.left() + "," + frame.top() + "," + frame.right() + "," + frame.bottom()
                    : null;
            return new Sample(title, dialogValue, bounds,
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

    private BufferedImage wanchengVoidTemplate() {
        if (wanchengVoidLoaded) {
            return wanchengVoidTemplate;
        }
        wanchengVoidLoaded = true;
        try {
            wanchengVoidTemplate = ImageIO.read(Path.of(WANCHENG_VOID_TEMPLATE).toFile());
        } catch (IOException ignored) {
            wanchengVoidTemplate = null;
        }
        return wanchengVoidTemplate;
    }

    /** G126：含"完成"的标题行一处命中遮一行，循环到找不到；遮不到原帧原样返回。 */
    BufferedImage maskWanchengVoidTitles(BufferedImage tracker) {
        BufferedImage voidTemplate = wanchengVoidTemplate();
        if (tracker == null || voidTemplate == null) {
            return tracker;
        }
        BufferedImage work = tracker;
        java.awt.Graphics2D graphics = null;
        for (int pass = 0; pass < WANCHENG_MASK_MAX_PASSES; pass++) {
            double[] hit = ImageFinder.find(work, voidTemplate, TITLE_THRESHOLD);
            if (hit == null || hit.length < 2) {
                break;
            }
            if (graphics == null) {
                BufferedImage copy = new BufferedImage(
                        tracker.getWidth(), tracker.getHeight(), BufferedImage.TYPE_INT_RGB);
                graphics = copy.createGraphics();
                graphics.drawImage(tracker, 0, 0, null);
                graphics.setColor(java.awt.Color.BLACK);
                work = copy;
            }
            int bandTop = Math.max(0, (int) Math.round(hit[1]) - WANCHENG_MASK_HALF_HEIGHT);
            graphics.fillRect(0, bandTop, work.getWidth(), WANCHENG_MASK_HALF_HEIGHT * 2);
        }
        if (graphics != null) {
            graphics.dispose();
        }
        return work;
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

    record Sample(String titlePresence, String dialogPresence, String dialogFrameBounds, byte[] dialogPng) {
        Sample {
            dialogPng = dialogPng == null ? null : dialogPng.clone();
        }

        @Override
        public byte[] dialogPng() {
            return dialogPng == null ? null : dialogPng.clone();
        }
    }
}
