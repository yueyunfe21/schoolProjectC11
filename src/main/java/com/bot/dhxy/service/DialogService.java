package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.wubei.WubeiDialogCatalog;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.List;
import java.util.Optional;

/**
 * TURN-40G: restored xiuluo local-kanda mechanics, byte-faithful to the verified Git {@code 59b85e0b}
 * (CR232/CR253/CR256) implementation — and deliberately nothing else. This class carries only the raw-image
 * enter-battle template matcher and its consume-time revalidation: the exact window-relative ROI, the exact
 * template file, the exact 0.82 threshold, direct raw matching (no washing, no OCR, no generic dialog detection),
 * and the exact click-point computation (ROI origin + template match center). A single miss is only "this sample
 * missed" — it triggers no Cloud request, no ready event, no interest change and no input.
 */
@Service
public class DialogService {

    private static final Logger log = LoggerFactory.getLogger(DialogService.class);

    // Frozen local-kanda constants from 59b85e0b DialogService (user-sampled button at screen (416,474)-(457,495)
    // with window base (152,98); only window-relative offsets are stored — CoordinateHelper adds the live base).
    static final String XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE =
            "images/template/dialog/xiuluo/xiuluo_enter_battle_kanda2.png";
    static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT = 264;
    static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP = 376;
    static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT = 305;
    static final int XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM = 397;
    static final double XIULUO_ENTER_BATTLE_LOCAL_MATCH_RATE = 0.82;
    // 江湖历练“开打”模板的实测屏幕 ROI 是 (1513,544)-(1699,571)。以当前 1024x768 窗口
    // base=(1252,170) 换算为 (261,374,186,27)；它能完整容纳 61x20 的 kaida.png，且不改修罗 ROI。
    static final int XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_LEFT = 261;
    static final int XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_TOP = 374;
    static final int XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_WIDTH = 186;
    static final int XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_HEIGHT = 27;
    // 抓鬼看打按钮会落在既有的 small dialog 内。不要再以窄条按钮 ROI 截图：不同对话框绘制状态
    // 会让窄条遗漏模板；固定复用已验证的 DIALOG_SMALL 全区域，再由模板匹配确定实际点击中心。
    static final int CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_LEFT = 250;
    static final int CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_TOP = 345;
    static final int CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_WIDTH = 529;
    static final int CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_HEIGHT = 143;
    private static final int DIALOG_LEFT = 250;
    private static final int DIALOG_TOP = 312;
    private static final int DIALOG_WIDTH = 529;
    private static final int DIALOG_HEIGHT = 208;

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    public DialogService(GameClientTracker tracker, CoordinateHelper coordinateHelper) {
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
    }

    /** One raw local template hit: the scaled ROI rectangle, the screen-absolute click point and the score. */
    public record LocalDialogTemplateMatch(int[] rect, int absoluteX, int absoluteY, double score) {
    }

    /** A baseline-equivalent prepared action plus the template box used to validate its catalog identity. */
    public record LocalPreparedDialogMatch(
            PreparedDialogAction action,
            int matchLeft,
            int matchTop,
            int matchRight,
            int matchBottom) {
    }

    /** Typed result used by the restored NPC smart-click queue safety shell. */
    public record NpcClickVerification(
            boolean verified,
            boolean optionDialogVisible,
            String status,
            DialogType dialogType) {
    }

    /**
     * Verifies the post-NPC-click option dialog with the baseline generic/raw/green precedence.
     * This method never clicks an option.
     */
    public NpcClickVerification verifyNpcArrivalExpectedDialog(
            List<String> expectedGreenTemplatePaths,
            String expectedRawTemplatePath,
            boolean deferToTask,
            String reason) {
        if (deferToTask) {
            return new NpcClickVerification(true, false, "DEFERRED_TO_TASK", DialogType.NONE);
        }
        int[] rect = coordinateHelper.getScaledRect(
                DIALOG_LEFT, DIALOG_TOP, DIALOG_WIDTH, DIALOG_HEIGHT);
        BufferedImage raw = tracker.captureToMemory(
                "npc-click:expected-dialog:" + safeDebugName(reason),
                rect[0], rect[1], rect[2], rect[3]);
        if (raw == null) {
            return new NpcClickVerification(false, false, "CAPTURE_FAILED", DialogType.NONE);
        }
        try {
            boolean optionVisible = isOptionDialog(raw, rect);
            if (!optionVisible) {
                return new NpcClickVerification(false, false, "NO_DIALOG", DialogType.NONE);
            }
            if (expectedRawTemplatePath != null && !expectedRawTemplatePath.isBlank()) {
                boolean matched = matchesTemplate(raw, expectedRawTemplatePath, 0.85d);
                return new NpcClickVerification(
                        matched, true,
                        matched ? "GREEN_TEMPLATE_VISIBLE" : "OPTION_VISIBLE",
                        DialogType.OPTION);
            }
            List<String> templates = expectedGreenTemplatePaths == null
                    ? List.of() : expectedGreenTemplatePaths;
            if (templates.isEmpty()) {
                return new NpcClickVerification(true, true, "OPTION_VISIBLE", DialogType.OPTION);
            }
            BufferedImage washed = ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(raw);
            try {
                for (String templatePath : templates) {
                    if (templatePath != null
                            && !templatePath.isBlank()
                            && matchesTemplate(washed, templatePath, 0.85d)) {
                        return new NpcClickVerification(
                                true, true, "GREEN_TEMPLATE_VISIBLE", DialogType.OPTION);
                    }
                }
            } finally {
                washed.flush();
            }
            return new NpcClickVerification(false, true, "OPTION_VISIBLE", DialogType.OPTION);
        } finally {
            raw.flush();
        }
    }

    private boolean matchesTemplate(BufferedImage image, String templatePath, double threshold) {
        BufferedImage template = ImagePreprocessor.pathToBufferedImage(templatePath);
        if (template == null) {
            return false;
        }
        try {
            return ImageFinder.find(image, template, threshold) != null;
        } finally {
            template.flush();
        }
    }

    /**
     * Runs the 696a12b0 WUBEI_ENTER_BATTLE prepare path entirely in memory.
     *
     * <p>This preserves OPTION verification, ordered templates, threshold 0.85, randomized offsets
     * and the prepared fingerprint. It never sends input and an ordinary miss has no side effect.</p>
     */
    public Optional<LocalPreparedDialogMatch> prepareWubeiEnterBattleLocal(String source) {
        int[] rect = coordinateHelper.getScaledRect(DIALOG_LEFT, DIALOG_TOP, DIALOG_WIDTH, DIALOG_HEIGHT);
        BufferedImage raw = tracker.captureToMemory("wubei-enter-battle-local-prepare",
                rect[0], rect[1], rect[2], rect[3]);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return prepareWubeiEnterBattleFromFrame(raw, rect, source);
        } finally {
            raw.flush();
        }
    }

    Optional<LocalPreparedDialogMatch> prepareWubeiEnterBattleFromFrame(
            BufferedImage raw, int[] rect, String source) {
        if (raw == null || rect == null || rect.length < 4 || !isOptionDialog(raw, rect)) {
            return Optional.empty();
        }
        BufferedImage washed = ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite(raw);
        try {
            for (GreenTemplateClickSpec spec : WubeiDialogCatalog.enterBattleSpecs()) {
                BufferedImage template = ImagePreprocessor.pathToBufferedImage(spec.templatePath());
                if (template == null) {
                    continue;
                }
                try {
                    double[] result = ImageFinder.find(washed, template, 0.85);
                    if (result == null || result.length < 2) {
                        continue;
                    }
                    int centerX = (int) Math.round(result[0]);
                    int centerY = (int) Math.round(result[1]);
                    Point safeClick = coordinateHelper.getRandomizedPoint(
                            new Point(rect[0] + centerX, rect[1] + centerY),
                            spec.minOffsetX(), spec.maxOffsetX(), spec.randomRadiusY());
                    int relativeX = safeClick.x - rect[0];
                    int relativeY = safeClick.y - rect[1];
                    int left = Math.max(0, relativeX - 44);
                    int top = Math.max(0, relativeY - 18);
                    int right = Math.min(washed.getWidth(), relativeX + 44);
                    int bottom = Math.min(washed.getHeight(), relativeY + 18);
                    BufferedImage validation = ImagePreprocessor.cropCopy(
                            washed, left, top, right - left, bottom - top);
                    if (validation == null) {
                        return Optional.empty();
                    }
                    try {
                        String fingerprint = ImagePreprocessor.buildBinaryFingerprint(validation);
                        if (fingerprint.isBlank()) {
                            return Optional.empty();
                        }
                        long now = System.currentTimeMillis();
                        PreparedDialogAction action = PreparedDialogAction.builder()
                                .dialogType(DialogType.OPTION)
                                .operation(DialogOperation.WUBEI_ENTER_BATTLE)
                                .targetKeyword(spec.name())
                                .matchedText(spec.templatePath())
                                .relativeX(relativeX)
                                .relativeY(relativeY)
                                .absoluteX(safeClick.x)
                                .absoluteY(safeClick.y)
                                .validationLeft(rect[0] + left)
                                .validationTop(rect[1] + top)
                                .validationRight(rect[0] + right)
                                .validationBottom(rect[1] + bottom)
                                .washMode(DialogFingerprintWashMode.TEMPLATE_SPECIFIC)
                                .fingerprint(fingerprint)
                                .clickRequired(true)
                                .preparedAtMs(now)
                                .lastVerifiedAtMs(now)
                                .source(source)
                                .build();
                        return Optional.of(new LocalPreparedDialogMatch(
                                action,
                                DIALOG_LEFT + centerX - template.getWidth() / 2,
                                DIALOG_TOP + centerY - template.getHeight() / 2,
                                DIALOG_LEFT + centerX - template.getWidth() / 2 + template.getWidth(),
                                DIALOG_TOP + centerY - template.getHeight() / 2 + template.getHeight()));
                    } finally {
                        validation.flush();
                    }
                } finally {
                    template.flush();
                }
            }
            return Optional.empty();
        } finally {
            washed.flush();
        }
    }

    private boolean isOptionDialog(BufferedImage raw, int[] dialogRect) {
        int[] maskArea = coordinateHelper.getScaledRect(411, 403, 368, 85);
        BufferedImage mask = ImagePreprocessor.cropAbsoluteRect(raw, dialogRect, maskArea);
        if (mask == null) {
            return false;
        }
        try {
            if (ImagePreprocessor.getImageStandardDeviation(mask, null) >= 30.0) {
                return false;
            }
        } finally {
            mask.flush();
        }
        int[] optionArea = coordinateHelper.getScaledRect(250, 387, 529, 101);
        BufferedImage option = ImagePreprocessor.cropAbsoluteRect(raw, dialogRect, optionArea);
        if (option == null) {
            return false;
        }
        try {
            return ImagePreprocessor.countGreenPixelsHSV(option, null) > 150;
        } finally {
            option.flush();
        }
    }

    /**
     * Runs one raw local-kanda probe over the exact scaled ROI of the thread-bound window. Restored verbatim from
     * {@code 59b85e0b}: raw ROI capture, raw template load, direct {@code TM_CCOEFF_NORMED}-equivalent matching at
     * threshold {@code 0.82}, click point = ROI origin + match center.
     */
    public Optional<LocalDialogTemplateMatch> findXiuluoEnterBattleLocalTemplate(String source, String phase) {
        return findTaskEnterBattleLocalTemplate(TaskType.XIULUO_V2, source, phase);
    }

    /**
     * Matches the task-owned local "看打" button on the bound window. Both 修罗 and 江湖历练 use
     * the same Runner probe/click lifecycle; only their button bitmap differs.
     *
     * @param taskType task that owns the currently armed local probe; only 修罗/江湖历练 are valid.
     * @param source diagnostic source, never used to choose a window.
     * @param phase diagnostic probe phase.
     * @return fresh window-relative template hit converted to an absolute input point, or empty on a miss.
     */
    public Optional<LocalDialogTemplateMatch> findTaskEnterBattleLocalTemplate(
            TaskType taskType, String source, String phase) {
        return findTaskEnterBattleLocalTemplate(taskType, source, phase, null, null);
    }

    /**
     * Matches a local "看打" button from the observation cycle's already-captured full frame when available.
     * The full frame is only used for the in-memory ROI crop; it never changes match
     * thresholds, click coordinates or task decisions.
     *
     * @param taskType task that owns the probe.
     * @param source diagnostic source.
     * @param phase diagnostic phase.
     * @param fullWindowFrame shared bound-window frame, or null when a fresh standalone capture is required.
     * @param fullWindowRect absolute [left, top, right, bottom] rectangle of {@code fullWindowFrame}, or null.
     * @return the fresh match result, or empty on a miss.
     */
    public Optional<LocalDialogTemplateMatch> findTaskEnterBattleLocalTemplate(
            TaskType taskType,
            String source,
            String phase,
            BufferedImage fullWindowFrame,
            int[] fullWindowRect) {
        if (taskType != TaskType.XIULUO_V2 && taskType != TaskType.XINSHOU_TRAINING
                && taskType != TaskType.CATCH_GHOST) {
            return Optional.empty();
        }
        String templatePath = taskType == TaskType.CATCH_GHOST
                ? "images/template/dialog/zhuagui/jinzhan.png"
                : taskType == TaskType.XINSHOU_TRAINING
                ? "images/template/dialog/a3/kaida.png"
                : XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE;
        boolean catchGhost = taskType == TaskType.CATCH_GHOST;
        boolean xinshouTraining = taskType == TaskType.XINSHOU_TRAINING;
        int[] rect = coordinateHelper.getScaledRect(
                catchGhost ? CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_LEFT
                        : xinshouTraining ? XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_LEFT
                        : XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT,
                catchGhost ? CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_TOP
                        : xinshouTraining ? XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_TOP
                        : XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP,
                catchGhost ? CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_WIDTH
                        : xinshouTraining ? XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_WIDTH
                        : XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT - XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT,
                catchGhost ? CATCH_GHOST_ENTER_BATTLE_LOCAL_ROI_HEIGHT
                        : xinshouTraining ? XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_HEIGHT
                        : XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM - XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP);
        BufferedImage roi = cropBoundWindowFrame(fullWindowFrame, fullWindowRect, rect);
        if (roi == null) {
            roi = tracker.captureToMemory(
                    "xiuluo-enter-battle-local-template:" + safeDebugName(source) + ":" + safeDebugName(phase),
                    rect[0], rect[1], rect[2], rect[3]);
        }
        BufferedImage template = ImagePreprocessor.pathToBufferedImage(templatePath);
        if (roi == null || template == null) {
            if (roi != null) {
                roi.flush();
            }
            if (template != null) {
                template.flush();
            }
            log.debug("[local-kanda] probe unavailable: roi={} template={}", roi != null, template != null);
            return Optional.empty();
        }
        try {
            double[] result = ImageFinder.find(roi, template, XIULUO_ENTER_BATTLE_LOCAL_MATCH_RATE);
            if (result == null || result.length < 3) {
                return Optional.empty();
            }
            int absoluteX = rect[0] + (int) Math.round(result[0]);
            int absoluteY = rect[1] + (int) Math.round(result[1]);
            return Optional.of(new LocalDialogTemplateMatch(rect, absoluteX, absoluteY, result[2]));
        } finally {
            roi.flush();
            template.flush();
        }
    }

    /**
     * Consume-time revalidation: re-runs the identical matcher on a fresh frame immediately before the click and
     * returns the refreshed click point, or empty to abort — a stale hit is never clicked.
     */
    public Optional<LocalDialogTemplateMatch> revalidateXiuluoEnterBattleLocalTemplate(String source, String reason) {
        return findXiuluoEnterBattleLocalTemplate(source, "consume-validate:" + reason);
    }

    /** Re-runs the task-owned local 看打 matcher immediately before Runner input. */
    public Optional<LocalDialogTemplateMatch> revalidateTaskEnterBattleLocalTemplate(
            TaskType taskType, String source, String reason) {
        return findTaskEnterBattleLocalTemplate(taskType, source, "consume-validate:" + reason);
    }

    private static String safeDebugName(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private BufferedImage cropBoundWindowFrame(BufferedImage frame, int[] frameRect, int[] targetRect) {
        if (frame == null || frameRect == null || frameRect.length < 4 || targetRect == null || targetRect.length < 4) {
            return null;
        }
        int cropX = targetRect[0] - frameRect[0];
        int cropY = targetRect[1] - frameRect[1];
        int cropWidth = targetRect[2] - targetRect[0];
        int cropHeight = targetRect[3] - targetRect[1];
        if (cropX < 0 || cropY < 0 || cropWidth <= 0 || cropHeight <= 0
                || cropX + cropWidth > frame.getWidth() || cropY + cropHeight > frame.getHeight()) {
            return null;
        }
        BufferedImage crop = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = crop.createGraphics();
        try {
            graphics.drawImage(frame, 0, 0, cropWidth, cropHeight,
                    cropX, cropY, cropX + cropWidth, cropY + cropHeight, null);
            return crop;
        } finally {
            graphics.dispose();
        }
    }

}
