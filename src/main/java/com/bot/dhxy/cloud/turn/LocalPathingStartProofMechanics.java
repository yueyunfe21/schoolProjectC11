package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnPathingIntent;
import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.navigation.TemplateLocationInfo;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
 * step, copies no business retry/fallback, and does not modify the {@link GameStateUtil} detector,
 * {@link MiniMapCoordinateReader} reader, or the runner watcher algorithms. The proof is: read the
 * mini-map coordinate baseline once before the action input; after the action reaches a
 * {@code COMPLETED} terminal, run the fast-edge {@link GameStateUtil#isMovingByPixelDiff} first and,
 * only when it is false, the coordinate-change fallback; any positive registers the intent, while a
 * double-negative or a non-{@code COMPLETED} terminal registers nothing.</p>
 */
@Slf4j
@Component
public final class LocalPathingStartProofMechanics {

    private static final long COORD_CONFIRM_TIMEOUT_MS = 1000L;
    private static final long COORD_CONFIRM_POLL_MS = 200L;

    private final GameStateUtil gameStateUtil;
    private final MiniMapCoordinateReader miniMapCoordinateReader;

    public LocalPathingStartProofMechanics(GameStateUtil gameStateUtil,
                                           MiniMapCoordinateReader miniMapCoordinateReader) {
        this.gameStateUtil = Objects.requireNonNull(gameStateUtil, "gameStateUtil");
        this.miniMapCoordinateReader = Objects.requireNonNull(miniMapCoordinateReader, "miniMapCoordinateReader");
    }

    /**
     * Read the current mini-map coordinate baseline once, before the start action input runs.
     *
     * @return the pre-action logical coordinate, or null when it cannot be read.
     */
    public MapCoordinate readBaseline() {
        return currentCoordinate();
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
    public void proveAndRegister(WindowRuntimeContext context, TurnPathingIntent intent, MapCoordinate baseline) {
        if (context == null || intent == null) {
            return;
        }
        String source = "turn:pathing-start-proof:" + intent.intentId();
        if (gameStateUtil.isMovingByPixelDiff(source + ":fast-edge")) {
            register(context, intent, source + ":fast-edge");
            return;
        }
        if (confirmByCoordinateChange(baseline, source)) {
            register(context, intent, source + ":coord-fallback");
            return;
        }
        log.info("local pathing start proof negative; no registration: source={} baseline={}",
                source, formatCoordinate(baseline));
    }

    private boolean confirmByCoordinateChange(MapCoordinate baseline, String source) {
        long deadline = System.currentTimeMillis() + COORD_CONFIRM_TIMEOUT_MS;
        MapCoordinate previous = null;
        while (System.currentTimeMillis() < deadline) {
            MapCoordinate current = currentCoordinate();
            if (current != null) {
                if (isCoordinateChanged(baseline, current) || isCoordinateChanged(previous, current)) {
                    log.info("local pathing start proof coordinate change: source={} baseline={} previous={} current={}",
                            source, formatCoordinate(baseline), formatCoordinate(previous), formatCoordinate(current));
                    return true;
                }
                previous = current;
            }
            if (!TaskSleep.sleep(COORD_CONFIRM_POLL_MS)) {
                return false;
            }
        }
        return false;
    }

    private MapCoordinate currentCoordinate() {
        MapCoordinate fromTemplate = miniMapCoordinateReader.readCurrentTemplateLocation()
                .map(TemplateLocationInfo::coordinate)
                .orElse(null);
        if (fromTemplate != null) {
            return fromTemplate;
        }
        return miniMapCoordinateReader.readCurrentCoordinate().orElse(null);
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

    private static boolean isCoordinateChanged(MapCoordinate baseline, MapCoordinate current) {
        return baseline != null
                && current != null
                && (baseline.getX() != current.getX() || baseline.getY() != current.getY());
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

    private static String formatCoordinate(MapCoordinate coordinate) {
        return coordinate == null ? "null" : "(" + coordinate.getX() + "," + coordinate.getY() + ")";
    }
}
