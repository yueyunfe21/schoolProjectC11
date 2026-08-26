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
    /** Completion verdict literals carried by the WUHUAN_COMPLETION_PRESENCE fact. */
    static final String COMPLETION_FINISHED = "finished";
    static final String COMPLETION_FINISHED_ONCE = "finishedOnce";
    static final String COMPLETION_ABSENT = "absent";
    private static final double TITLE_THRESHOLD = 0.82D;
    /** Raw-vs-raw like the 天庭 dialog matcher; the sources are crops of the real story text. */
    private static final double COMPLETION_THRESHOLD = 0.85D;
    private static final String TITLE_TEMPLATE = "images/template/wuhuan/panel_title_yellow.png";
    /**
     * Raw source crops of the two completion stories. Order matters: {@code 恭喜你完} is the shared
     * prefix of BOTH stories, so the unique {@code 了一次五} fragment must be tried first — a
     * finished hit is only trustworthy after finishedOnce missed.
     */
    private static final String COMPLETION_ONCE_TEMPLATE =
            "images/template/wuhuan/source_wuhuan_task_finished_once_story.png";
    private static final String COMPLETION_FINISHED_TEMPLATE =
            "images/template/wuhuan/source_wuhuan_task_finished_story.png";
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
    private BufferedImage completionOnceTemplate;
    private BufferedImage completionFinishedTemplate;

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
        String completionVerdict = null;
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
            // Title absent is the only moment a completion story can be the answer; matching raw
            // source crops in the dialog ROI settles it locally without any Cloud frame round trip.
            if (titleSampled && !titlePresent) {
                completionVerdict = sampleCompletionVerdict();
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
                trackerPng, trackerMissPng, titleScore, completionVerdict);
    }

    /**
     * Matches the two raw completion story crops in the dialog ROI. finishedOnce first — its
     * {@code 了一次五} fragment is unique, while the finished crop is a prefix both stories share.
     */
    private String sampleCompletionVerdict() {
        BufferedImage dialog = crop(DIALOG_LEFT, DIALOG_TOP, DIALOG_WIDTH, DIALOG_HEIGHT);
        if (dialog == null) {
            return null;
        }
        try {
            double onceScore = Double.NaN;
            double finishedScore = Double.NaN;
            String verdict = null;
            BufferedImage once = completionOnceTemplate();
            if (once != null) {
                onceScore = ImageFinder.bestMatchScore(dialog, once);
                if (Double.isFinite(onceScore) && onceScore >= COMPLETION_THRESHOLD) {
                    verdict = COMPLETION_FINISHED_ONCE;
                }
            }
            BufferedImage finished = completionFinishedTemplate();
            if (verdict == null && finished != null) {
                finishedScore = ImageFinder.bestMatchScore(dialog, finished);
                if (Double.isFinite(finishedScore) && finishedScore >= COMPLETION_THRESHOLD) {
                    verdict = COMPLETION_FINISHED;
                }
            }
            if (verdict == null) {
                verdict = once == null && finished == null ? null : COMPLETION_ABSENT;
            }
            saveCompletionEvidence(dialog, verdict, onceScore, finishedScore);
            return verdict;
        } finally {
            dialog.flush();
        }
    }

    /**
     * Every completion-story match call writes its exact judged crop to disk (rolling pool of 60):
     * a disputed verdict must always be answerable with the very pixels it was judged on.
     */
    private void saveCompletionEvidence(
            BufferedImage dialog, String verdict, double onceScore, double finishedScore) {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("logs", "frames", "wuhuan-completion");
            java.nio.file.Files.createDirectories(dir);
            int slot = COMPLETION_EVIDENCE_SEQ.getAndIncrement() % 60;
            java.nio.file.Path file = dir.resolve(String.format("completion-%02d-%s.png",
                    slot, verdict == null ? "unsampled" : verdict));
            java.nio.file.Path tmp = dir.resolve(String.format("completion-%02d.tmp.png", slot));
            try (java.util.stream.Stream<java.nio.file.Path> stale = java.nio.file.Files.list(dir)) {
                stale.filter(existing -> existing.getFileName().toString()
                                .startsWith(String.format("completion-%02d-", slot)))
                        .forEach(existing -> existing.toFile().delete());
            }
            ImageIO.write(dialog, "png", tmp.toFile());
            java.nio.file.Files.move(tmp, file,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            org.slf4j.LoggerFactory.getLogger(WuhuanPresenceLocalMechanics.class).info(
                    "[wuhuan-completion-evidence] verdict={} onceScore={} finishedScore={} threshold={} saved={}",
                    verdict, onceScore, finishedScore, COMPLETION_THRESHOLD, file);
        } catch (IOException | RuntimeException failure) {
            org.slf4j.LoggerFactory.getLogger(WuhuanPresenceLocalMechanics.class).warn(
                    "[wuhuan-completion-evidence] save failed: {}", failure.getMessage());
        }
    }

    private static final java.util.concurrent.atomic.AtomicInteger COMPLETION_EVIDENCE_SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    void reset() {
        if (titleTemplate != null) {
            titleTemplate.flush();
            titleTemplate = null;
        }
        if (completionOnceTemplate != null) {
            completionOnceTemplate.flush();
            completionOnceTemplate = null;
        }
        if (completionFinishedTemplate != null) {
            completionFinishedTemplate.flush();
            completionFinishedTemplate = null;
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

    private BufferedImage completionOnceTemplate() {
        if (completionOnceTemplate != null) {
            return completionOnceTemplate;
        }
        try {
            completionOnceTemplate = ImageIO.read(Path.of(COMPLETION_ONCE_TEMPLATE).toFile());
        } catch (IOException ignored) {
            completionOnceTemplate = null;
        }
        return completionOnceTemplate;
    }

    private BufferedImage completionFinishedTemplate() {
        if (completionFinishedTemplate != null) {
            return completionFinishedTemplate;
        }
        try {
            completionFinishedTemplate = ImageIO.read(Path.of(COMPLETION_FINISHED_TEMPLATE).toFile());
        } catch (IOException ignored) {
            completionFinishedTemplate = null;
        }
        return completionFinishedTemplate;
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
                  double titleScore,
                  String completionVerdict) {
    }
}
