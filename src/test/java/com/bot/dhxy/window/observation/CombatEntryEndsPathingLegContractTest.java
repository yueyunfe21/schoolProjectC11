package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.model.WindowPathingState;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.config.WindowIsolationProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * G005: a fight starting is the end of whatever walk was under way.
 *
 * <p>The ordinary 天庭 leg walks into its own fight — green link, walk, dialog answered, combat. The
 * walking intent used to stay ACTIVE through the whole battle and for a minute after it: the coordinate
 * strip is hidden in combat so the arrival check cannot classify anything, and post-combat re-rendering
 * kept resetting its stability window (a real run held it 61 seconds past the exit). All that time the
 * Cloud correctly refused to release an ACTIVE intent and the observer correctly published nothing while
 * an intent was on record — so post-combat recovery sat behind a walk that had physically ended the moment
 * the fight began. The combat entry edge is the sampler's own fact; the leg closes on it.</p>
 */
class CombatEntryEndsPathingLegContractTest {

    private static final String WINDOW = "window-9";
    private static final String HWND = "54321";

    private WindowRuntimeContext contextWithIntent(WindowPathingState state) {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "t", "c", 99L, 0, 0, 1024, 768));
        context.updatePathingSnapshot(WindowPathingSnapshot.builder()
                .intent(WindowPathingIntent.builder()
                        .source("tianting:tracker-green-click:advance")
                        .intentId("leg-under-test")
                        .build())
                .state(state)
                .updatedAtMs(System.currentTimeMillis())
                .build());
        return context;
    }

    private WindowObservationSampler sampler(WindowRuntimeContext context) {
        GameClientTracker tracker =
                new GameClientTracker(null, null, null, null, null, null, null, null, null, null, null);
        CoordinateHelper coordinateHelper = new CoordinateHelper(null, null);
        return new WindowObservationSampler(
                context,
                new WindowTaskContextHolder(new WindowIsolationProperties()),
                tracker,
                coordinateHelper,
                new DialogService(tracker, coordinateHelper),
                new InputSequences(null),
                "run-9",
                false);
    }

    @Test
    void combatEntryClassifiesAnActiveLegAsStopped() {
        WindowRuntimeContext context = contextWithIntent(WindowPathingState.ACTIVE);

        sampler(context).observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("test"),
                System.currentTimeMillis(),
                new ArrayList<>());

        WindowPathingSnapshot after = context.getPathingSnapshot();
        assertEquals(WindowPathingState.STOPPED_AWAY, after.getState(),
                "the character cannot walk in combat; the leg ends when the fight begins");
        assertEquals("leg-under-test", after.getIntent().getIntentId(),
                "the intent stays on record for the Cloud to release; only its state is terminal");
    }

    @Test
    void combatEntryLeavesAnAlreadyTerminalLegAlone() {
        WindowRuntimeContext context = contextWithIntent(WindowPathingState.ARRIVED);
        WindowPathingSnapshot before = context.getPathingSnapshot();

        sampler(context).observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("test"),
                System.currentTimeMillis(),
                new ArrayList<>());

        assertSame(before, context.getPathingSnapshot(),
                "an ARRIVED terminal already tells the truth; combat entry must not rewrite it");
    }

    @Test
    void combatEntryWithNoIntentChangesNothing() {
        WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        context.setNativeBinding(new WindowNativeBinding(HWND, "t", "c", 99L, 0, 0, 1024, 768));

        sampler(context).observeLocalCombatTransition(
                LocalCombatSignalMechanics.Signal.visible("test"),
                System.currentTimeMillis(),
                new ArrayList<>());

        assertEquals(null, context.getPathingSnapshot() == null
                        ? null
                        : context.getPathingSnapshot().getIntent(),
                "no walk, nothing to end — dark-thunder patrol clicks carry no intent and must stay silent");
    }
}
