package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.tools.CoordinateHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Performs the 59b baseline combat image mechanics for the existing per-window observation runner.
 * Business-task phase changes remain Cloud-owned. This class owns the local three-stage combat signal and the
 * fixed normal-world mini-map anchor; {@link WindowObservationSampler} uses those local facts to produce exact-run
 * combat edges without a Cloud image round trip.
 */
final class LocalCombatSignalMechanics {

    static final String INTEREST_KEY = "combat-signal";
    static final long SAMPLE_PERIOD_MS = 1_000L;

    private static final List<Stage> STAGES = List.of(
            new Stage("combat-flag", 974, 630, 51, 20, Policy.ANY, 0.85,
                    List.of("images/template/battle/flag_battle.png")),
            new Stage("combat-selection", 927, 302, 100, 225, Policy.ANY, 0.8,
                    List.of("images/template/battle/zhaohuan.png",
                            "images/template/battle/chehui.png")),
            new Stage("combat-top", 456, 62, 123, 39, Policy.ALL, 0.8,
                    List.of("images/template/battle/nu.png",
                            "images/template/battle/yuan.png")));
    private static final Stage MINIMAP_VISIBLE = new Stage(
            "minimap-visible", 196, 65, 20, 22, Policy.ANY, 0.85,
            List.of("images/template/map/minimap_visible_anchor.png"));

    private final FrameCapture frameCapture;
    private final TemplateLoader templateLoader;
    private final TemplateMatcher templateMatcher;
    /** Successful template loads live only for this sampler/run and are released by {@link #reset()}. */
    private final Map<String, BufferedImage> templateCache = new LinkedHashMap<>();

    /** G002: crops one scaled window-absolute rect from the sampler's shared per-cycle frame. */
    interface CycleFrameCropper {
        BufferedImage crop(int[] scaledRect);
    }

    private volatile CycleFrameCropper cycleFrameCropper;

    /**
     * G002: once bound, every stage capture is a memory crop from the shared cycle frame — zero
     * extra {@code PrintWindow} calls. A missing shared frame yields {@code null} and therefore
     * UNAVAILABLE, never ABSENT.
     */
    void bindCycleFrameCropper(CycleFrameCropper cropper) {
        this.cycleFrameCropper = cropper;
    }

    LocalCombatSignalMechanics(GameClientTracker tracker,
                               CoordinateHelper coordinateHelper) {
        this.frameCapture = stage -> {
            int[] rect = coordinateHelper.getScaledRect(
                    stage.left(), stage.top(), stage.width(), stage.height());
            CycleFrameCropper cropper = cycleFrameCropper;
            if (cropper != null) {
                return cropper.crop(rect);
            }
            return tracker.captureToMemory(
                    "observe:" + stage.key(), rect[0], rect[1], rect[2], rect[3]);
        };
        this.templateLoader = path -> {
            try {
                return ImageIO.read(Path.of(path).toFile());
            } catch (IOException failure) {
                throw new IllegalStateException("combat template load failed: " + path, failure);
            }
        };
        this.templateMatcher = (source, template, threshold) -> {
            double[] match = ImageFinder.find(source, template, threshold);
            // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
            MatchEvidenceStore.saveOnChange(
                    "combat-signal-probe-" + template.getWidth() + "x" + template.getHeight(),
                    null, source, template, match);
            return match != null;
        };
    }

    LocalCombatSignalMechanics(FrameCapture frameCapture,
                               TemplateLoader templateLoader,
                               TemplateMatcher templateMatcher) {
        this.frameCapture = Objects.requireNonNull(frameCapture, "frameCapture");
        this.templateLoader = Objects.requireNonNull(templateLoader, "templateLoader");
        this.templateMatcher = Objects.requireNonNull(templateMatcher, "templateMatcher");
    }

    Signal sample() {
        String firstUnavailableStage = null;
        for (Stage stage : STAGES) {
            BufferedImage source;
            try {
                source = frameCapture.capture(stage);
            } catch (RuntimeException failure) {
                if (firstUnavailableStage == null) {
                    firstUnavailableStage = stage.key();
                }
                continue;
            }
            if (source == null) {
                if (firstUnavailableStage == null) {
                    firstUnavailableStage = stage.key();
                }
                continue;
            }
            try {
                MatchState matched = matchStage(source, stage);
                if (matched == MatchState.UNAVAILABLE) {
                    if (firstUnavailableStage == null) {
                        firstUnavailableStage = stage.key();
                    }
                    continue;
                }
                if (matched == MatchState.VISIBLE) {
                    return Signal.visible(stage.key());
                }
            } finally {
                source.flush();
            }
        }
        return firstUnavailableStage == null
                ? Signal.absent()
                : Signal.unavailable(firstUnavailableStage);
    }

    /**
     * Detects the fixed magnifier rendered beside the normal-world mini-map.
     * The anchor is absent from the battle layout, so no coordinate OCR or Cloud round trip is needed.
     */
    Signal sampleMinimap() {
        BufferedImage source;
        try {
            source = frameCapture.capture(MINIMAP_VISIBLE);
        } catch (RuntimeException failure) {
            return Signal.unavailable(MINIMAP_VISIBLE.key());
        }
        if (source == null) {
            return Signal.unavailable(MINIMAP_VISIBLE.key());
        }
        try {
            MatchState matchState = matchStage(source, MINIMAP_VISIBLE);
            return switch (matchState) {
                case VISIBLE -> Signal.visible(MINIMAP_VISIBLE.key());
                case ABSENT -> Signal.absent(MINIMAP_VISIBLE.key());
                case UNAVAILABLE -> Signal.unavailable(MINIMAP_VISIBLE.key());
            };
        } finally {
            source.flush();
        }
    }

    private MatchState matchStage(BufferedImage source, Stage stage) {
        boolean anyVisible = false;
        for (String path : stage.templatePaths()) {
            BufferedImage template;
            try {
                template = loadedTemplate(path);
            } catch (RuntimeException failure) {
                return MatchState.UNAVAILABLE;
            }
            if (template == null) {
                return MatchState.UNAVAILABLE;
            }
            boolean visible;
            try {
                visible = templateMatcher.matches(source, template, stage.threshold());
            } catch (RuntimeException failure) {
                return MatchState.UNAVAILABLE;
            }
            if (visible) {
                anyVisible = true;
                if (stage.policy() == Policy.ANY) {
                    return MatchState.VISIBLE;
                }
            } else if (stage.policy() == Policy.ALL) {
                return MatchState.ABSENT;
            }
        }
        return anyVisible ? MatchState.VISIBLE : MatchState.ABSENT;
    }

    private BufferedImage loadedTemplate(String path) {
        BufferedImage cached = templateCache.get(path);
        if (cached != null) {
            return cached;
        }
        BufferedImage loaded = templateLoader.load(path);
        if (loaded != null) {
            templateCache.put(path, loaded);
        }
        return loaded;
    }

    void reset() {
        templateCache.values().forEach(BufferedImage::flush);
        templateCache.clear();
    }

    record Signal(State state, String source) {
        static Signal visible(String source) {
            return new Signal(State.VISIBLE, source);
        }

        static Signal absent() {
            return new Signal(State.ABSENT, "none");
        }

        static Signal absent(String source) {
            return new Signal(State.ABSENT, source);
        }

        static Signal unavailable(String source) {
            return new Signal(State.UNAVAILABLE, source);
        }

        String wireValue() {
            return state.name() + ":" + source;
        }
    }

    enum State {
        VISIBLE,
        ABSENT,
        UNAVAILABLE
    }

    @FunctionalInterface
    interface FrameCapture {
        BufferedImage capture(Stage stage);
    }

    @FunctionalInterface
    interface TemplateLoader {
        BufferedImage load(String path);
    }

    @FunctionalInterface
    interface TemplateMatcher {
        boolean matches(BufferedImage source, BufferedImage template, double threshold);
    }

    record Stage(String key,
                 int left,
                 int top,
                 int width,
                 int height,
                 Policy policy,
                 double threshold,
                 List<String> templatePaths) {
    }

    private enum Policy {
        ANY,
        ALL
    }

    private enum MatchState {
        VISIBLE,
        ABSENT,
        UNAVAILABLE
    }
}
