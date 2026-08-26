package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFactType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEventType;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowExpectedCombatEnterClaim;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalCombatSignalMechanicsTest {

    @Test
    void flagVisibleShortCircuitsAfterOneCaptureAtFrozenThreshold() {
        Fixture fixture = new Fixture(true);

        LocalCombatSignalMechanics.Signal signal = fixture.mechanics.sample();

        assertEquals("VISIBLE:combat-flag", signal.wireValue());
        assertEquals(1, fixture.captureCount.get());
        assertEquals(List.of(0.85), fixture.thresholds);
    }

    @Test
    void selectionUsesAnyAndShortCircuitsBeforeTop() {
        Fixture fixture = new Fixture(false, false, true);

        LocalCombatSignalMechanics.Signal signal = fixture.mechanics.sample();

        assertEquals("VISIBLE:combat-selection", signal.wireValue());
        assertEquals(2, fixture.captureCount.get());
        assertEquals(List.of(0.85, 0.8, 0.8), fixture.thresholds);
    }

    @Test
    void topRequiresAllTemplates() {
        Fixture fixture = new Fixture(false, false, false, true, true);

        LocalCombatSignalMechanics.Signal signal = fixture.mechanics.sample();

        assertEquals("VISIBLE:combat-top", signal.wireValue());
        assertEquals(3, fixture.captureCount.get());
        assertEquals(List.of(0.85, 0.8, 0.8, 0.8, 0.8), fixture.thresholds);
    }

    @Test
    void captureFailureIsUnavailableAndNeverAbsent() {
        LocalCombatSignalMechanics mechanics = new LocalCombatSignalMechanics(
                stage -> null,
                path -> image(),
                (source, template, threshold) -> false);

        assertEquals("UNAVAILABLE:combat-flag", mechanics.sample().wireValue());
    }

    @Test
    void unavailableFlagDoesNotHideVisibleSelectionStage() {
        AtomicInteger captures = new AtomicInteger();
        LocalCombatSignalMechanics mechanics = new LocalCombatSignalMechanics(
                stage -> captures.incrementAndGet() == 1 ? null : image(),
                path -> image(),
                (source, template, threshold) -> true);

        assertEquals("VISIBLE:combat-selection", mechanics.sample().wireValue());
        assertEquals(2, captures.get());
    }

    @Test
    void sharedFrameCropClipsAllFourEdges() throws ReflectiveOperationException {
        WindowObservationSampler sampler = sampler(context());
        Field frameField = WindowObservationSampler.class.getDeclaredField("sharedCycleFrame");
        Field rectField = WindowObservationSampler.class.getDeclaredField("sharedCycleFrameRect");
        frameField.setAccessible(true);
        rectField.setAccessible(true);
        frameField.set(sampler, new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));
        rectField.set(sampler, new int[]{10, 20, 110, 120});

        assertCropSize(sampler, new int[]{5, 30, 30, 50}, 20, 20);
        assertCropSize(sampler, new int[]{20, 15, 40, 40}, 20, 20);
        assertCropSize(sampler, new int[]{90, 30, 120, 50}, 20, 20);
        assertCropSize(sampler, new int[]{20, 100, 40, 130}, 20, 20);
    }

    @Test
    void successfulTemplatesAreCachedPerRunAndReleasedByReset() {
        AtomicInteger loads = new AtomicInteger();
        LocalCombatSignalMechanics mechanics = new LocalCombatSignalMechanics(
                stage -> image(),
                path -> {
                    loads.incrementAndGet();
                    return image();
                },
                (source, template, threshold) -> true);

        mechanics.sample();
        mechanics.sample();
        assertEquals(1, loads.get(), "the visible flag template is loaded once for this run");

        mechanics.reset();
        mechanics.sample();
        assertEquals(2, loads.get(), "runner stop/reset releases the per-run template cache");
    }

    @Test
    void savedWorldFrameMatchesTheProductionMinimapAnchor() throws IOException {
        BufferedImage frame = ImageIO.read(Path.of(
                "images/test-cases/combat-exit/minimap-visible-world.png").toFile());
        BufferedImage template = ImageIO.read(Path.of(
                "images/template/map/minimap_visible_anchor.png").toFile());
        BufferedImage roi = frame.getSubimage(196, 65, 20, 22);
        try {
            assertNotNull(ImageFinder.find(roi, template, 0.85),
                    "the saved normal-world frame must satisfy the production exit condition");
        } finally {
            roi.flush();
            template.flush();
            frame.flush();
        }
    }

    @Test
    void savedPartialHudFrameReplaysAbsenceBeforeRecoveredCombatFrame() throws IOException {
        BufferedImage partial = ImageIO.read(Path.of(
                "images/test-cases/g098/partial-hud-frame.png").toFile());
        BufferedImage recovered = ImageIO.read(Path.of(
                "images/test-cases/g098/recovered-combat-frame.png").toFile());
        LocalCombatSignalMechanics partialMechanics = savedFrameMechanics(partial);
        LocalCombatSignalMechanics recoveredMechanics = savedFrameMechanics(recovered);
        try {
            assertEquals("ABSENT:none", partialMechanics.sample().wireValue());
            assertEquals("ABSENT:minimap-visible", partialMechanics.sampleMinimap().wireValue());
            assertEquals("VISIBLE:combat-flag", recoveredMechanics.sample().wireValue());
        } finally {
            partialMechanics.reset();
            recoveredMechanics.reset();
            partial.flush();
            recovered.flush();
        }
    }

    @Test
    void samplerCarriesExactlyOneLatestWinsCombatFactWithoutRoi() {
        Fixture fixture = new Fixture(false, false, false, false);
        WindowRuntimeContext context = new WindowRuntimeContext("window", new GameContext());
        context.setNativeBinding(new WindowNativeBinding(
                "0x40f", "title", "class", 1L, 0, 0, 1024, 768));
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        GameClientTracker tracker = new GameClientTracker(
                null, null, null, null, null, null, null, null, null, null, null);
        CoordinateHelper coordinates = new CoordinateHelper(null, null);
        WindowObservationSampler sampler = new WindowObservationSampler(
                context, holder, tracker, coordinates,
                new DialogService(tracker, coordinates), new InputSequences(null),
                "run", false, fixture.mechanics);

        WindowObservationSampler.SampleBatch batch = sampler.collect(List.of(
                new ObservationInterest(LocalCombatSignalMechanics.INTEREST_KEY, 1_000L, null)));

        assertEquals(1, batch.facts().size());
        assertEquals(ObservationFactType.COMBAT_SIGNAL, batch.facts().getFirst().factType());
        assertEquals("ABSENT:none", batch.facts().getFirst().value());
        assertEquals(0, batch.rois().size());
    }

    @Test
    void lateExactClaimPublishesEntryOnceWithinTheExistingVisibleGeneration() {
        WindowRuntimeContext context = context();
        WindowObservationSampler sampler = sampler(context);
        List<ObservationKeyEvent> events = new ArrayList<>();

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 1_000L, events);
        assertEquals(0, events.size(), "the first visible sample has no exact claim");
        assertEquals(true, context.registerExpectedCombatEnterClaim(
                claim(context, "claim-late", "run", "business-late", null)));

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 2_000L, events);
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 3_000L, events);

        assertEquals(1, events.size());
        assertEquals(ObservationKeyEventType.IN_COMBAT, events.getFirst().eventType());
        assertEquals("combat-enter:claim-late:1", events.getFirst().eventId());
        assertEquals(1L, events.getFirst().combatGeneration());
    }

    @Test
    void staleGenerationClaimNeverPublishesEntry() {
        WindowRuntimeContext context = context();
        assertEquals(true, context.registerExpectedCombatEnterClaim(
                claim(context, "claim-stale", "run", "business-stale", 99L)));
        WindowObservationSampler sampler = sampler(context);
        List<ObservationKeyEvent> events = new ArrayList<>();

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 1_000L, events);
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 2_000L, events);

        assertEquals(0, events.size());
    }

    @Test
    void wrongObservationRunClaimIsIgnoredAndDoesNotBlockLaterExactClaim() {
        WindowRuntimeContext context = context();
        WindowObservationSampler sampler = sampler(context);
        List<ObservationKeyEvent> events = new ArrayList<>();

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 1_000L, events);
        assertEquals(true, context.registerExpectedCombatEnterClaim(
                claim(context, "claim-wrong-run", "other-run", "business-wrong", null)));
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 2_000L, events);
        assertEquals(0, events.size());

        assertEquals(true, context.registerExpectedCombatEnterClaim(
                claim(context, "claim-current", "run", "business-current", null)));
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 3_000L, events);

        assertEquals(1, events.size());
        assertEquals("combat-enter:claim-current:1", events.getFirst().eventId());
    }

    @Test
    void visibleLocalMinimapImmediatelyPublishesOneExitAndSchedulesSceneReproof() throws Exception {
        WindowRuntimeContext context = context();
        assertEquals(true, context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "claim-ordinary", "run", "business-ordinary", "XIULUO_V2", "attempt-ordinary",
                "window", context.getNativeBinding().getNativeHandle(), "local-template", null)));
        LocalCombatSignalMechanics mechanics = new LocalCombatSignalMechanics(
                stage -> image(),
                path -> image(),
                (source, template, threshold) -> true);
        WindowObservationSampler sampler = sampler(context, mechanics);
        List<ObservationKeyEvent> events = new ArrayList<>();

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 1_000L, 1L, events);
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.absent(), 2_000L, 2L, events);
        assertEquals(List.of(ObservationKeyEventType.IN_COMBAT, ObservationKeyEventType.COMBAT_EXITED),
                events.stream().map(ObservationKeyEvent::eventType).toList());
        Field refreshPending = WindowObservationSampler.class
                .getDeclaredField("xinshouRefreshPending");
        refreshPending.setAccessible(true);
        assertEquals(true, refreshPending.getBoolean(sampler),
                "the exact exit edge must schedule the post-combat scene reproof");
    }

    @Test
    void absentMinimapAndAbsentCombatTemplatesRequireTwoConsecutiveFrames() {
        WindowRuntimeContext context = context();
        assertEquals(true, context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "claim-reciprocal", "run", "business-reciprocal", "XIULUO_V2", "attempt-reciprocal",
                "window", context.getNativeBinding().getNativeHandle(), "local-template", null)));
        LocalCombatSignalMechanics mechanics = new LocalCombatSignalMechanics(
                stage -> image(),
                path -> image(),
                (source, template, threshold) -> false);
        WindowObservationSampler sampler = sampler(context, mechanics);
        List<ObservationKeyEvent> events = new ArrayList<>();

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 1_000L, 1L, events);
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.absent(), 2_000L, 2L, events);

        assertEquals(List.of(ObservationKeyEventType.IN_COMBAT),
                events.stream().map(ObservationKeyEvent::eventType).toList());
        assertEquals(true, context.isLocalCombatVisible());

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.absent(), 3_000L, 3L, events);

        assertEquals(List.of(ObservationKeyEventType.IN_COMBAT, ObservationKeyEventType.COMBAT_EXITED),
                events.stream().map(ObservationKeyEvent::eventType).toList());
        assertEquals(false, context.isLocalCombatVisible());
    }

    @Test
    void combatEvidenceAfterOneDualAbsentResetsExitConfirmation() {
        WindowRuntimeContext context = context();
        assertEquals(true, context.registerExpectedCombatEnterClaim(new WindowExpectedCombatEnterClaim(
                "claim-partial-frame", "run", "business-partial-frame", "XIULUO_V2", "attempt-partial-frame",
                "window", context.getNativeBinding().getNativeHandle(), "local-template", null)));
        LocalCombatSignalMechanics mechanics = new LocalCombatSignalMechanics(
                stage -> image(), path -> image(), (source, template, threshold) -> false);
        WindowObservationSampler sampler = sampler(context, mechanics);
        List<ObservationKeyEvent> events = new ArrayList<>();

        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 1_000L, 1L, events);
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.absent(), 2_000L, 2L, events);
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("combat-flag"), 3_000L, 3L, events);
        sampler.observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.absent(), 4_000L, 4L, events);

        assertEquals(List.of(ObservationKeyEventType.IN_COMBAT),
                events.stream().map(ObservationKeyEvent::eventType).toList());
        assertEquals(true, context.isLocalCombatVisible());
    }

    private static WindowExpectedCombatEnterClaim claim(
            WindowRuntimeContext context,
            String claimId,
            String observationRunId,
            String businessTaskRunId,
            Long combatGeneration) {
        return new WindowExpectedCombatEnterClaim(
                claimId, observationRunId, businessTaskRunId, "XIULUO_V2", "attempt-1",
                "window", context.getNativeBinding().getNativeHandle(), "cloud-fallback", combatGeneration);
    }

    private static WindowRuntimeContext context() {
        WindowRuntimeContext context = new WindowRuntimeContext("window", new GameContext());
        context.setNativeBinding(new WindowNativeBinding(
                "0x40f", "title", "class", 1L, 0, 0, 1024, 768));
        return context;
    }

    private static WindowObservationSampler sampler(WindowRuntimeContext context) {
        LocalCombatSignalMechanics mechanics = new LocalCombatSignalMechanics(
                stage -> image(), path -> image(), (source, template, threshold) -> false);
        return sampler(context, mechanics);
    }

    private static WindowObservationSampler sampler(
            WindowRuntimeContext context,
            LocalCombatSignalMechanics mechanics) {
        WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        GameClientTracker tracker = new GameClientTracker(
                null, null, null, null, null, null, null, null, null, null, null);
        CoordinateHelper coordinates = new CoordinateHelper(null, null);
        return new WindowObservationSampler(
                context, holder, tracker, coordinates,
                new DialogService(tracker, coordinates), new InputSequences(null),
                "run", false, mechanics);
    }

    private static BufferedImage image() {
        return new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
    }

    private static LocalCombatSignalMechanics savedFrameMechanics(BufferedImage frame) {
        return new LocalCombatSignalMechanics(
                stage -> {
                    int width = Math.min(stage.width(), frame.getWidth() - stage.left());
                    int height = Math.min(stage.height(), frame.getHeight() - stage.top());
                    return width <= 0 || height <= 0
                            ? null
                            : frame.getSubimage(stage.left(), stage.top(), width, height);
                },
                path -> {
                    try {
                        return ImageIO.read(Path.of(path).toFile());
                    } catch (IOException failure) {
                        throw new IllegalStateException(failure);
                    }
                },
                (source, template, threshold) -> ImageFinder.find(source, template, threshold) != null);
    }

    private static void assertCropSize(
            WindowObservationSampler sampler,
            int[] rect,
            int expectedWidth,
            int expectedHeight) {
        BufferedImage cropped = sampler.cropSharedCycleFrame(rect);
        assertNotNull(cropped);
        try {
            assertEquals(expectedWidth, cropped.getWidth());
            assertEquals(expectedHeight, cropped.getHeight());
        } finally {
            cropped.flush();
        }
    }

    private static final class Fixture {
        private final AtomicInteger captureCount = new AtomicInteger();
        private final ArrayDeque<Boolean> matches = new ArrayDeque<>();
        private final List<Double> thresholds = new ArrayList<>();
        private final LocalCombatSignalMechanics mechanics;

        private Fixture(boolean... matches) {
            for (boolean match : matches) {
                this.matches.add(match);
            }
            mechanics = new LocalCombatSignalMechanics(
                    stage -> {
                        captureCount.incrementAndGet();
                        return image();
                    },
                    path -> image(),
                    (source, template, threshold) -> {
                        thresholds.add(threshold);
                        return !this.matches.isEmpty() && this.matches.removeFirst();
                    });
        }
    }

}
