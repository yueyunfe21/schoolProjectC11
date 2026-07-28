package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent;
import com.bot.dhxy.cloud.turn.local.LocalMovementFactMechanics;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.awt.image.BufferedImage;

/**
 * Narrow exact-window local proof observer for the Local Pathing Fact Bridge (TURN-27 Amendment #2).
 *
 * <p>The Cloud business layer keeps ownership of the target, ordering, fallback/retry/timeout, and the
 * next JSON action; it only attaches a typed {@link TurnPathingIntent} to a start action. This class is
 * the DHXY-local side that decides whether a completed start action actually began movement, using the
 * existing {@code 696a12b0} proof order, and registers the intent against the authoritative
 * {@link WindowRuntimeContext} on a positive proof.</p>
 *
 * <p>It is <b>not</b> a fifth permanent local Service. It sends no input, chooses no business next
 * step and copies no business retry/fallback. The proof captures a fixed mini-map strip baseline
 * before input; after a {@code COMPLETED} terminal it checks fixed edge-frame change first and then
 * fixed strip pixel change. It performs no OCR, map-name or numeric-coordinate interpretation. Any
 * positive registers the intent, while a
 * double-negative or a non-{@code COMPLETED} terminal registers nothing.</p>
 */
@Slf4j
@Component
public class LocalPathingStartProofMechanics {

    private final LocalMovementFactMechanics movementFacts;

    public LocalPathingStartProofMechanics(LocalMovementFactMechanics movementFacts) {
        this.movementFacts = Objects.requireNonNull(movementFacts, "movementFacts");
    }

    /**
     * Read the current mini-map coordinate baseline once, before the start action input runs.
     *
     * @return the pre-action logical coordinate, or null when it cannot be read.
     */
    public BufferedImage readBaseline() {
        return movementFacts.captureCoordinateStrip();
    }

    /**
     * Prove the completed start action actually started movement and, only on a positive proof,
     * register the intent against the authoritative local pathing slot.
     *
     * <p>Baseline order: fast-edge pixel movement first; only when false, poll the existing coordinate
     * reader for a real change against the pre-action baseline within the confirm timeout. A
     * double-negative registers nothing. Callers must invoke this only after a {@code COMPLETED}
     * terminal; a stopped/failed/uncertain action must never reach here.</p>
     *
     * @param context authoritative window runtime context that owns {@code markPathingStarted}.
     * @param intent typed start-action pathing intent attached by Cloud.
     * @param baseline pre-action coordinate baseline captured by {@link #readBaseline()}.
     */
    public void proveAndRegister(WindowRuntimeContext context, TurnPathingIntent intent, BufferedImage baseline) {
        if (context == null || intent == null) {
            return;
        }
        String source = "turn:pathing-start-proof:" + intent.intentId();
        try {
            if (movementFacts.edgePixelsChanged(source + ":fast-edge")) {
                register(context, intent, source + ":fast-edge");
                return;
            }
            if (movementFacts.coordinateStripChanged(baseline, source)) {
                register(context, intent, source + ":coordinate-strip");
                return;
            }
        } finally {
            if (baseline != null) {
                baseline.flush();
            }
        }
        log.info("local pathing start proof negative; no registration: source={} baseline={}",
                source, baseline == null ? "missing" : baseline.getWidth() + "x" + baseline.getHeight());
    }

    private void register(WindowRuntimeContext context, TurnPathingIntent intent, String source) {
        WindowPathingIntent windowIntent = WindowPathingIntent.builder()
                .source(intent.source())
                .intentId(intent.intentId())
                .targetMapName(intent.targetMapName())
                .targetX(intent.targetX())
                .targetY(intent.targetY())
                .tolerance(intent.tolerance())
                .type(parseIntentType(intent.type()))
                .build();
        context.markPathingStarted(windowIntent);
        log.info("local pathing start proof positive; intent registered: source={} intentId={} targetMap={} target=({}, {})",
                source, windowIntent.getIntentId(), windowIntent.getTargetMapName(),
                windowIntent.getTargetX(), windowIntent.getTargetY());
    }

    private static WindowPathingIntentType parseIntentType(String name) {
        if (name == null) {
            return WindowPathingIntentType.TARGETED;
        }
        try {
            return WindowPathingIntentType.valueOf(name);
        } catch (IllegalArgumentException unknownType) {
            return WindowPathingIntentType.TARGETED;
        }
    }

}
