package com.bot.dhxy.window.observation;

import java.util.concurrent.atomic.AtomicReference;

/**
 * TURN-40G: process-wide dependency bridge between the Spring-managed observation wiring and the non-Spring turn
 * loop factory (whose bean construction signature is outside this card's write set). This holds construction glue
 * only — a single factory reference — never business or observation state; when nothing is registered (tests, or a
 * process without the HTTPS transport) the turn loops simply run without observation.
 */
public final class ObservationRunnerWiring {

    private static final AtomicReference<WindowObservationRunnerFactory> CURRENT = new AtomicReference<>();

    private ObservationRunnerWiring() {
    }

    /** Registers the process-wide runner factory (Spring wiring calls this once at startup). */
    public static void register(WindowObservationRunnerFactory factory) {
        CURRENT.set(factory);
    }

    /** Clears the registration (test isolation). */
    public static void clear() {
        CURRENT.set(null);
    }

    /** Returns the registered factory, or {@code null} when this process has no observation plane. */
    public static WindowObservationRunnerFactory current() {
        return CURRENT.get();
    }
}
