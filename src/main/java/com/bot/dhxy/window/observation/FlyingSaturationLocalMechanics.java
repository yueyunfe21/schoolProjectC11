package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.tools.CoordinateHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Read-only Changshou map-label and flying-state classification from one exact-window shared frame.
 *
 * <p>The detector deliberately has an UNKNOWN band. A frame whose saturation sits between the two
 * live-validated clusters is not evidence that the character is grounded, so callers must wait for
 * another shared observation frame instead of toggling flight speculatively.</p>
 */
final class FlyingSaturationLocalMechanics {

    static final String TIANTING_DARK_THUNDER_INTEREST_KEY =
            "tianting-dark-thunder-flight-saturation";
    static final long TIANTING_DARK_THUNDER_SAMPLE_PERIOD_MS = 500L;
    static final int MAP_LABEL_ROI_LEFT = 46;
    static final int MAP_LABEL_ROI_TOP = 59;
    static final int MAP_LABEL_ROI_WIDTH = 178;
    static final int MAP_LABEL_ROI_HEIGHT = 35;
    static final double MAP_LABEL_THRESHOLD = 0.85D;
    static final Path CHANGSHOU_MAP_LABEL_TEMPLATE =
            Path.of("images", "template", "map_label", "长寿村.png");
    static final int ROI_LEFT = 688;
    static final int ROI_TOP = 71;
    static final int ROI_WIDTH = 67;
    static final int ROI_HEIGHT = 38;
    static final double FLYING_MAX_MEAN_SATURATION = 0.10D;
    static final double NOT_FLYING_MIN_MEAN_SATURATION = 0.20D;

    private final CoordinateHelper coordinateHelper;
    private LocalCombatSignalMechanics.CycleFrameCropper cropper;
    private BufferedImage changshouMapLabelTemplate;
    private boolean changshouMapLabelTemplateUnavailable;

    FlyingSaturationLocalMechanics(CoordinateHelper coordinateHelper) {
        this.coordinateHelper = coordinateHelper;
    }

    void bindCycleFrameCropper(LocalCombatSignalMechanics.CycleFrameCropper cropper) {
        this.cropper = cropper;
    }

    /**
     * Matches the current-map label while pathing is still active. This deliberately does not wait for a
     * stationary coordinate result because Changshou Village is an in-transit map on the Ghost King route.
     *
     * @return VISIBLE only for a template hit in the current G002 shared frame; unavailable input remains
     *         distinct from a real miss.
     */
    MapLabelSample sampleChangshouMapLabel() {
        LocalCombatSignalMechanics.CycleFrameCropper current = cropper;
        BufferedImage template = loadChangshouMapLabelTemplate();
        if (current == null || template == null) {
            return MapLabelSample.unavailable();
        }
        BufferedImage roi = current.crop(coordinateHelper.getScaledRect(
                MAP_LABEL_ROI_LEFT, MAP_LABEL_ROI_TOP, MAP_LABEL_ROI_WIDTH, MAP_LABEL_ROI_HEIGHT));
        if (roi == null) {
            return MapLabelSample.unavailable();
        }
        try {
            double[] match = ImageFinder.find(roi, template, MAP_LABEL_THRESHOLD);
            // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
            MatchEvidenceStore.saveOnChange("flying-changshou-map-label", null, roi, template, match);
            return match == null
                    ? MapLabelSample.absent()
                    : MapLabelSample.visible(match[2]);
        } catch (RuntimeException matchFailure) {
            return MapLabelSample.unavailable();
        } finally {
            roi.flush();
        }
    }

    /**
     * Samples the approved window-relative ROI from the current G002 shared frame.
     *
     * @return flying classification and mean HSV saturation; UNKNOWN with no pixels when the shared
     *         frame is unavailable.
     */
    Sample sample() {
        LocalCombatSignalMechanics.CycleFrameCropper current = cropper;
        if (current == null) {
            return Sample.unavailable();
        }
        BufferedImage roi = current.crop(coordinateHelper.getScaledRect(
                ROI_LEFT, ROI_TOP, ROI_WIDTH, ROI_HEIGHT));
        if (roi == null) {
            return Sample.unavailable();
        }
        try {
            return classify(roi);
        } finally {
            roi.flush();
        }
    }

    /**
     * Classifies one already-cropped ROI using mean HSV saturation.
     *
     * @param roi RGB image for the window-relative {@code (688,71,67x38)} area; null is unavailable.
     * @return three-state result. Pixel count is zero only when no usable image was supplied.
     */
    static Sample classify(BufferedImage roi) {
        if (roi == null || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
            return Sample.unavailable();
        }
        double saturationTotal = 0D;
        int pixelCount = roi.getWidth() * roi.getHeight();
        for (int y = 0; y < roi.getHeight(); y++) {
            for (int x = 0; x < roi.getWidth(); x++) {
                int rgb = roi.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;
                int maximum = Math.max(red, Math.max(green, blue));
                int minimum = Math.min(red, Math.min(green, blue));
                saturationTotal += maximum == 0 ? 0D : (maximum - minimum) / (double) maximum;
            }
        }
        double meanSaturation = saturationTotal / pixelCount;
        State state = meanSaturation <= FLYING_MAX_MEAN_SATURATION
                ? State.FLYING
                : meanSaturation >= NOT_FLYING_MIN_MEAN_SATURATION
                ? State.NOT_FLYING
                : State.UNKNOWN;
        return new Sample(state, meanSaturation, pixelCount);
    }

    void reset() {
        if (changshouMapLabelTemplate != null) {
            changshouMapLabelTemplate.flush();
            changshouMapLabelTemplate = null;
        }
        changshouMapLabelTemplateUnavailable = false;
    }

    private BufferedImage loadChangshouMapLabelTemplate() {
        if (changshouMapLabelTemplate != null) {
            return changshouMapLabelTemplate;
        }
        if (changshouMapLabelTemplateUnavailable) {
            return null;
        }
        try {
            changshouMapLabelTemplate = ImageIO.read(CHANGSHOU_MAP_LABEL_TEMPLATE.toFile());
        } catch (IOException loadFailure) {
            changshouMapLabelTemplateUnavailable = true;
        }
        if (changshouMapLabelTemplate == null) {
            changshouMapLabelTemplateUnavailable = true;
        }
        return changshouMapLabelTemplate;
    }

    enum State {
        FLYING,
        NOT_FLYING,
        UNKNOWN
    }

    record Sample(State state, double meanSaturation, int pixelCount) {
        private static Sample unavailable() {
            return new Sample(State.UNKNOWN, Double.NaN, 0);
        }
    }

    enum MapLabelState {
        VISIBLE,
        ABSENT,
        UNAVAILABLE
    }

    record MapLabelSample(MapLabelState state, double score) {
        private static MapLabelSample visible(double score) {
            return new MapLabelSample(MapLabelState.VISIBLE, score);
        }

        private static MapLabelSample absent() {
            return new MapLabelSample(MapLabelState.ABSENT, Double.NaN);
        }

        private static MapLabelSample unavailable() {
            return new MapLabelSample(MapLabelState.UNAVAILABLE, Double.NaN);
        }
    }
}
