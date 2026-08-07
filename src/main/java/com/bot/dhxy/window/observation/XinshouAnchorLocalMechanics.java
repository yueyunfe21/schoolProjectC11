package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only 新手 template sensing over the G002 shared frame.
 *
 * <p>The Cloud receives only a matched filename fact. Template pixels, thresholding and all ROI
 * geometry remain local; a missing shared frame simply produces no fact and is never interpreted as
 * a business negative.</p>
 */
final class XinshouAnchorLocalMechanics {

    static final String INTEREST_KEY = "xinshou-anchor";
    static final long SAMPLE_PERIOD_MS = 1_000L;
    private static final double THRESHOLD = 0.85D;
    private static final Region TRACKER_TITLE_REGION = new Region(6, 196, 201, 355);
    // ESC and 跳过 share the same top-right origin; the union keeps the lower skip button visible.
    private static final Region TOP_RIGHT_CONTROL_REGION = new Region(870, 57, 128, 49);
    private static final Region ESC_BOT_REGION = new Region(549, 667, 22, 16);
    private static final Region ADOPTION_REGION = new Region(469, 592, 103, 29);
    private static final Region CONFIRM_REGION = new Region(569, 365, 160, 132);
    private static final List<Probe> TITLE_PROBES = List.of(
            new Probe("zhua_title.png", "images/template/xinshou/zhua_title.png", TRACKER_TITLE_REGION),
            new Probe("shanhuhaidao.png", "images/template/xinshou/shanhuhaidao.png", TRACKER_TITLE_REGION),
            new Probe("guiqi.png", "images/template/xinshou/guiqi.png", TRACKER_TITLE_REGION),
            new Probe("xunren.png", "images/template/xinshou/xunren.png", TRACKER_TITLE_REGION),
            new Probe("lunhui_title.png", "images/template/xinshou/lunhui_title.png", TRACKER_TITLE_REGION),
            new Probe("jiao_wuzi_title.png", "images/template/xinshou/jiao_wuzi_title.png", TRACKER_TITLE_REGION),
            new Probe("xiufu_title.png", "images/template/xinshou/xiufu_title.png", TRACKER_TITLE_REGION),
            new Probe("lingyang_title.png", "images/template/xinshou/lingyang_title.png", TRACKER_TITLE_REGION));
    private static final Probe ADOPTION =
            new Probe("lingyang.png", "images/template/xinshou/lingyang.png", ADOPTION_REGION);
    private static final Probe ESC =
            new Probe("ESC.png", "images/template/xinshou/ESC.png", TOP_RIGHT_CONTROL_REGION);
    private static final Probe SKIP =
            new Probe("tiaoguo.png", "images/template/xinshou/tiaoguo.png", TOP_RIGHT_CONTROL_REGION);
    private static final Probe ESC_BOT =
            new Probe("esc_bot.png", "images/template/xinshou/esc_bot.png", ESC_BOT_REGION);
    private static final Probe CONFIRM_GATE =
            new Probe("quedingguan_.png", "images/template/xinshou/quedingguan_.png", CONFIRM_REGION);
    private static final Probe CONFIRM =
            new Probe("confirm.png", "images/template/xinshou/confirm.png", CONFIRM_REGION);

    private final CoordinateHelper coordinateHelper;
    private final Map<String, BufferedImage> templates = new LinkedHashMap<>();
    private LocalCombatSignalMechanics.CycleFrameCropper cycleFrameCropper;

    XinshouAnchorLocalMechanics(GameClientTracker tracker, CoordinateHelper coordinateHelper) {
        this.coordinateHelper = coordinateHelper;
    }

    void bindCycleFrameCropper(LocalCombatSignalMechanics.CycleFrameCropper cropper) {
        this.cycleFrameCropper = cropper;
    }

    /**
     * Samples every independently reportable 新手 anchor in one shared-frame cycle.
     *
     * <p>Title/ESC retain their mutually-exclusive business priority. 领养 is a separate
     * local action target and is sampled independently, so a same-frame title cannot hide it. A full
     * {@code TaskTrackerPanelService} visibility check is intentionally not run in this local duty:
     * for the 新手 flow, all title probes missing is the current lightweight approximation of
     * "Tracker not visible". {@code esc_bot}, however, is a separate recovery anchor and must be
     * reported even when a title is visible in the same frame.</p>
     */
    List<String> sampleAnchors() {
        AnchorSample sample = sample();
        String primary = sample.primaryAnchor();
        boolean escBotVisible = sample.escBotVisible();
        if (primary == null) {
            return escBotVisible ? List.of(ESC_BOT.wireValue()) : List.of();
        }
        return escBotVisible ? List.of(primary, ESC_BOT.wireValue()) : List.of(primary);
    }

    /**
     * Samples the primary anchor and esc_bot independently from the one shared cycle frame.
     *
     * <p>The caller sends esc_bot as a separate present/absent fact because ordinary observation
     * facts are latest-wins per kind. Sending both hits as the same kind would silently discard one
     * of them before the Cloud state machine could observe their coexistence.</p>
     */
    AnchorSample sample() {
        AnchorTarget title = sampleTitleAnchor();
        AnchorTarget adoption = sampleTarget(ADOPTION);
        AnchorTarget primary = title != null ? title : adoption;
        if (primary == null) {
            // ESC retains its original no-title/no-adoption rule; esc_bot is sampled independently.
            primary = sampleTarget(ESC);
        }
        boolean escVisible = primary != null && ESC.wireValue().equals(primary.templateName());
        return new AnchorSample(primary == null ? null : primary.templateName(), primary, adoption,
                escVisible, matches(SKIP), matches(ESC_BOT), sampleRecoveryTarget());
    }

    private AnchorTarget sampleTitleAnchor() {
        for (Probe probe : TITLE_PROBES) {
            AnchorTarget target = sampleTarget(probe);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private AnchorTarget sampleTarget(Probe probe) {
        Match match = find(probe);
        return match == null ? null
                : new AnchorTarget(probe.wireValue(), match.absolutePoint().x, match.absolutePoint().y);
    }

    private boolean matches(Probe probe) {
        return find(probe) != null;
    }

    private RecoveryTarget sampleRecoveryTarget() {
        Match gate = find(CONFIRM_GATE);
        if (gate != null) {
            return new RecoveryTarget(CONFIRM_GATE.wireValue(), gate.absolutePoint().x, gate.absolutePoint().y);
        }
        Match confirm = find(CONFIRM);
        return confirm == null ? null
                : new RecoveryTarget(CONFIRM.wireValue(), confirm.absolutePoint().x, confirm.absolutePoint().y);
    }

    static RecoveryTemplateSpec recoveryTemplateSpec(String templateName) {
        Probe probe;
        if (SKIP.wireValue().equals(templateName)) {
            probe = SKIP;
        } else if (CONFIRM_GATE.wireValue().equals(templateName)) {
            probe = CONFIRM_GATE;
        } else if (CONFIRM.wireValue().equals(templateName)) {
            probe = CONFIRM;
        } else {
            return null;
        }
        Region region = probe.region();
        return new RecoveryTemplateSpec(
                probe.wireValue(),
                probe.path(),
                region.left(),
                region.top(),
                region.width(),
                region.height());
    }

    static double recoveryMatchThreshold() {
        return THRESHOLD;
    }

    /**
     * Resolves one local template point from the shared cycle frame into screen-absolute input space.
     *
     * <p>The raw pixels and template stay local. Only a matched center point is allowed across the
     * observation boundary, and the Cloud command remains fenced to this exact window turn.</p>
     */
    private Match find(Probe probe) {
        LocalCombatSignalMechanics.CycleFrameCropper cropper = cycleFrameCropper;
        if (cropper == null) {
            return null;
        }
        int[] scaled = coordinateHelper.getScaledRect(
                probe.region().left(), probe.region().top(), probe.region().width(), probe.region().height());
        BufferedImage source = cropper.crop(scaled);
        if (source == null) {
            return null;
        }
        try {
            BufferedImage template = templates.computeIfAbsent(probe.path(), this::loadTemplate);
            double[] match = template == null ? null : ImageFinder.find(source, template, THRESHOLD);
            Point point = coordinateHelper.resolveMatchedPointInRect(scaled, match);
            return point == null ? null : new Match(point);
        } finally {
            source.flush();
        }
    }

    private BufferedImage loadTemplate(String path) {
        try {
            return ImageIO.read(Path.of(path).toFile());
        } catch (IOException ignored) {
            return null;
        }
    }

    void reset() {
        templates.values().forEach(image -> {
            if (image != null) {
                image.flush();
            }
        });
        templates.clear();
    }

    private record Region(int left, int top, int width, int height) {
    }

    private record Probe(String wireValue, String path, Region region) {
    }

    private record Match(Point absolutePoint) {
    }

    record RecoveryTarget(String templateName, int absoluteX, int absoluteY) {
    }

    record RecoveryTemplateSpec(
            String templateName,
            String path,
            int left,
            int top,
            int width,
            int height) {
    }

    record AnchorTarget(String templateName, int absoluteX, int absoluteY) {
    }

    record AnchorSample(String primaryAnchor, AnchorTarget primaryTarget, AnchorTarget adoptionTarget,
                         boolean escVisible,
                         boolean skipVisible,
                         boolean escBotVisible,
                         RecoveryTarget recoveryTarget) {
    }
}
