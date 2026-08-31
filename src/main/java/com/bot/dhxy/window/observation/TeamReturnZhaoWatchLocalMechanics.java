package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.tools.CoordinateHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * G108: read-only leader-HUD 召 watch for the local Runner.
 *
 * <p>This class deliberately reports only mechanical presence of the existing {@code status/zhao.png}
 * template inside the frozen HUD ROI {@code (342,57,272x69)} at the frozen threshold {@code 0.75}. It
 * never decides whether a task may be accepted, never opens or closes the observation window (the
 * Cloud does, through the {@code team-return-zhao-watch} interest) and never produces input. The only
 * client-side accumulation is the per-combat-generation seen-marker folded into the published value so
 * that a Cloud reading the level fact as latest-only can never lose a PRESENT sample between polls.</p>
 */
final class TeamReturnZhaoWatchLocalMechanics {

    static final String INTEREST_KEY = "team-return-zhao-watch";
    /** G108 frozen cadence: one sample per second regardless of any faster interest period. */
    static final long SAMPLE_PERIOD_MS = 1_000L;
    static final String STATE_PRESENT = "PRESENT";
    static final String STATE_ABSENT = "ABSENT";
    static final String STATE_UNKNOWN = "UNKNOWN";
    /** Same ROI and threshold as the Cloud one-shot leader-signal check; G108 must not retune them. */
    private static final int ROI_LEFT = 342;
    private static final int ROI_TOP = 57;
    private static final int ROI_WIDTH = 272;
    private static final int ROI_HEIGHT = 69;
    private static final double TEMPLATE_THRESHOLD = 0.75D;
    private static final String TEMPLATE = "images/template/status/zhao.png";
    private static final int EVIDENCE_POOL_SIZE = 40;

    private final CoordinateHelper coordinateHelper;
    private LocalCombatSignalMechanics.CycleFrameCropper cropper;
    private BufferedImage template;
    private boolean templateLoadFailed;
    private String lastSavedState;
    private long lastSavedGeneration = Long.MIN_VALUE;

    TeamReturnZhaoWatchLocalMechanics(CoordinateHelper coordinateHelper) {
        this.coordinateHelper = coordinateHelper;
    }

    void bindCycleFrameCropper(LocalCombatSignalMechanics.CycleFrameCropper cropper) {
        this.cropper = cropper;
    }

    /**
     * Samples one shared exact-HWND frame without invoking another capture.
     *
     * @param generation current local combat generation, used only to label edge evidence
     * @return the mechanical sample; UNKNOWN when no shared frame or no template is available
     */
    Sample sample(long generation) {
        BufferedImage template = template();
        LocalCombatSignalMechanics.CycleFrameCropper current = cropper;
        BufferedImage roi = current == null
                ? null
                : current.crop(coordinateHelper.getScaledRect(ROI_LEFT, ROI_TOP, ROI_WIDTH, ROI_HEIGHT));
        if (roi == null || template == null) {
            return new Sample(STATE_UNKNOWN, Double.NaN);
        }
        try {
            double score = ImageFinder.bestMatchScore(roi, template);
            String state = Double.isFinite(score) && score >= TEMPLATE_THRESHOLD
                    ? STATE_PRESENT
                    : STATE_ABSENT;
            saveEdgeEvidence(roi, state, score, generation);
            return new Sample(state, score);
        } finally {
            roi.flush();
        }
    }

    /**
     * Persists the judged ROI only on state edges or the first sample of a new combat generation
     * (rolling pool of {@value #EVIDENCE_POOL_SIZE}); a per-second write would recreate the disk
     * churn G103 just removed while an edge-only pool still answers every disputed flip with the
     * exact pixels it was judged on.
     */
    private void saveEdgeEvidence(BufferedImage roi, String state, double score, long generation) {
        if (state.equals(lastSavedState) && generation == lastSavedGeneration) {
            return;
        }
        lastSavedState = state;
        lastSavedGeneration = generation;
        try {
            Path dir = Path.of("logs", "frames", "team-return-zhao-watch");
            java.nio.file.Files.createDirectories(dir);
            int slot = EVIDENCE_SEQ.getAndIncrement() % EVIDENCE_POOL_SIZE;
            Path file = dir.resolve(String.format(Locale.ROOT,
                    "zhao-%02d-gen%d-%s-score%.3f.png", slot, generation, state, score));
            Path tmp = dir.resolve(String.format(Locale.ROOT, "zhao-%02d.tmp.png", slot));
            try (java.util.stream.Stream<Path> stale = java.nio.file.Files.list(dir)) {
                stale.filter(existing -> existing.getFileName().toString()
                                .startsWith(String.format(Locale.ROOT, "zhao-%02d-", slot)))
                        .forEach(existing -> existing.toFile().delete());
            }
            ImageIO.write(roi, "png", tmp.toFile());
            java.nio.file.Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            org.slf4j.LoggerFactory.getLogger(TeamReturnZhaoWatchLocalMechanics.class).info(
                    "[team-return-zhao-watch] edge evidence: state={} score={} generation={} saved={}",
                    state, String.format(Locale.ROOT, "%.3f", score), generation, file);
        } catch (IOException | RuntimeException failure) {
            org.slf4j.LoggerFactory.getLogger(TeamReturnZhaoWatchLocalMechanics.class).warn(
                    "[team-return-zhao-watch] edge evidence save failed: {}", failure.getMessage());
        }
    }

    private static final java.util.concurrent.atomic.AtomicInteger EVIDENCE_SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    void reset() {
        if (template != null) {
            template.flush();
            template = null;
        }
        templateLoadFailed = false;
        lastSavedState = null;
        lastSavedGeneration = Long.MIN_VALUE;
    }

    private BufferedImage template() {
        if (template != null || templateLoadFailed) {
            return template;
        }
        try {
            template = ImageIO.read(Path.of(TEMPLATE).toFile());
        } catch (IOException ignored) {
            template = null;
        }
        templateLoadFailed = template == null;
        return template;
    }

    record Sample(String state, double score) {
    }

    /**
     * Pure per-run duty state: monotonic sequence plus the per-generation seen-marker. Kept free of
     * imaging so the G108 contracts can drive the exact production accumulation rules.
     */
    static final class Duty {

        private long sequence;
        private long markerGeneration = Long.MIN_VALUE;
        private boolean everPresentInGeneration;

        /**
         * Folds one sample into the published wire value.
         *
         * @param state mechanical sample state literal
         * @param generation current local combat generation
         * @param score template match score, NaN when UNKNOWN
         * @return the exact fact value: {@code STATE|gen=G|seq=S|ever=BOOL|score=X}
         */
        String fold(String state, long generation, double score) {
            if (generation != markerGeneration) {
                // A new combat generation starts a new post-combat observation window: the previous
                // generation's seen-marker must never leak into it (G108 contract (5)).
                markerGeneration = generation;
                everPresentInGeneration = false;
            }
            if (STATE_PRESENT.equals(state)) {
                everPresentInGeneration = true;
            }
            sequence++;
            return state
                    + "|gen=" + generation
                    + "|seq=" + sequence
                    + "|ever=" + everPresentInGeneration
                    + "|score=" + (Double.isFinite(score)
                            ? String.format(Locale.ROOT, "%.3f", score)
                            : "nan");
        }

        void reset() {
            sequence = 0L;
            markerGeneration = Long.MIN_VALUE;
            everPresentInGeneration = false;
        }
    }
}
