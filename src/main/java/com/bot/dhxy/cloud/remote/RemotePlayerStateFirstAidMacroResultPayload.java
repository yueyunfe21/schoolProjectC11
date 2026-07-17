package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.service.playerstate.PlayerStateFirstAidLocalMacroMechanics.CachedPlanStatus;
import com.bot.dhxy.service.playerstate.PlayerStateFirstAidLocalMacroMechanics.HealSnapshotStatus;
import com.bot.dhxy.service.playerstate.PlayerStateFirstAidLocalMacroMechanics.HealStatus;
import com.bot.dhxy.service.playerstate.PlayerStateFirstAidLocalMacroMechanics.ProbeSnapshotStatus;
import com.bot.dhxy.service.playerstate.PlayerStateFirstAidLocalMacroMechanics.ProbeStatus;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

/**
 * Strict typed EXECUTED result for {@code LOCAL_MACRO / PLAYER_STATE_FIRST_AID}. Losslessly mirrors the
 * committed mechanics, reusing the existing status enums verbatim and keeping window-client / screen-
 * absolute coordinate spaces unchanged. Exactly one operation variant is populated:
 *
 * <ul>
 *   <li>{@code PROBE_SUPPLY_NO_FOCUS}: {@code probeSnapshotStatus} (READABLE/CAPTURE_UNAVAILABLE) plus
 *       the ordered per-bar {@link RemoteProbeObservation} list.</li>
 *   <li>{@code HEAL_ALL}: {@code healSnapshotStatus} (CAPTURED/CAPTURE_FAILED) plus the ordered per-bar
 *       {@link RemoteHealOutcome} list.</li>
 *   <li>{@code EXECUTE_CACHED_PLAN}: {@code cachedPlanStatus} (COMPLETED/INTERRUPTED).</li>
 * </ul>
 *
 * <p>Fields of the other two variants must be explicitly {@code null}; the constructor rejects any mixed
 * shape. A non-EXECUTED transport terminal carries no typed result at all.</p>
 */
@Value
@Jacksonized
public class RemotePlayerStateFirstAidMacroResultPayload implements RemoteLocalMacroResultPayload {
    RemoteLocalMacroKind macroKind;
    RemotePlayerStateFirstAidMacroCommandPayload.Operation operation;
    ProbeSnapshotStatus probeSnapshotStatus;
    List<RemoteProbeObservation> probeObservations;
    HealSnapshotStatus healSnapshotStatus;
    List<RemoteHealOutcome> healOutcomes;
    CachedPlanStatus cachedPlanStatus;
    Integer observedBaseX;
    Integer observedBaseY;

    private static final List<String> FIXED_BAR_NAMES =
            List.of("人物血量", "人物法力", "宝宝血量", "宝宝法力");

    @Builder
    public RemotePlayerStateFirstAidMacroResultPayload(
            RemoteLocalMacroKind macroKind,
            RemotePlayerStateFirstAidMacroCommandPayload.Operation operation,
            ProbeSnapshotStatus probeSnapshotStatus,
            List<RemoteProbeObservation> probeObservations,
            HealSnapshotStatus healSnapshotStatus,
            List<RemoteHealOutcome> healOutcomes,
            CachedPlanStatus cachedPlanStatus,
            Integer observedBaseX,
            Integer observedBaseY) {
        if (macroKind != RemoteLocalMacroKind.PLAYER_STATE_FIRST_AID) {
            throw new IllegalArgumentException("macroKind must be PLAYER_STATE_FIRST_AID");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        switch (operation) {
            case PROBE_SUPPLY_NO_FOCUS -> {
                if (probeSnapshotStatus == null || probeObservations == null) {
                    throw new IllegalArgumentException(
                            "PROBE_SUPPLY_NO_FOCUS result requires probeSnapshotStatus and probeObservations");
                }
                if (healSnapshotStatus != null || healOutcomes != null || cachedPlanStatus != null) {
                    throw new IllegalArgumentException(
                            "PROBE_SUPPLY_NO_FOCUS result must not carry heal or cached-plan fields");
                }
                if ((observedBaseX == null) != (observedBaseY == null)) {
                    throw new IllegalArgumentException(
                            "observed base coordinates must be present or absent as a pair");
                }
                boolean readable = probeSnapshotStatus == ProbeSnapshotStatus.READABLE;
                if (readable != (observedBaseX != null)) {
                    throw new IllegalArgumentException("observed base must be present only for a READABLE probe");
                }
                if (readable) {
                    if (observedBaseX == -1) {
                        throw new IllegalArgumentException(
                                "READABLE probe must not carry the -1 unavailable base sentinel");
                    }
                    requireOrderedBarNames(probeNames(probeObservations),
                            "PROBE_SUPPLY_NO_FOCUS READABLE observations");
                } else if (!probeObservations.isEmpty()) {
                    throw new IllegalArgumentException(
                            "CAPTURE_UNAVAILABLE probe must carry an empty observation list");
                }
            }
            case HEAL_ALL -> {
                if (healSnapshotStatus == null || healOutcomes == null) {
                    throw new IllegalArgumentException(
                            "HEAL_ALL result requires healSnapshotStatus and healOutcomes");
                }
                if (probeSnapshotStatus != null || probeObservations != null || cachedPlanStatus != null) {
                    throw new IllegalArgumentException(
                            "HEAL_ALL result must not carry probe or cached-plan fields");
                }
                if (observedBaseX != null || observedBaseY != null) {
                    throw new IllegalArgumentException("HEAL_ALL result must not carry an observed base");
                }
                if (healSnapshotStatus == HealSnapshotStatus.CAPTURED) {
                    requireOrderedBarNames(healNames(healOutcomes), "HEAL_ALL CAPTURED outcomes");
                } else if (!healOutcomes.isEmpty()) {
                    throw new IllegalArgumentException("CAPTURE_FAILED heal must carry an empty outcome list");
                }
            }
            case EXECUTE_CACHED_PLAN -> {
                if (cachedPlanStatus == null) {
                    throw new IllegalArgumentException("EXECUTE_CACHED_PLAN result requires cachedPlanStatus");
                }
                if (probeSnapshotStatus != null || probeObservations != null
                        || healSnapshotStatus != null || healOutcomes != null
                        || observedBaseX != null || observedBaseY != null) {
                    throw new IllegalArgumentException(
                            "EXECUTE_CACHED_PLAN result must not carry probe, heal, or observed-base fields");
                }
            }
        }
        this.macroKind = macroKind;
        this.operation = operation;
        this.probeSnapshotStatus = probeSnapshotStatus;
        this.probeObservations = probeObservations == null ? null : List.copyOf(probeObservations);
        this.healSnapshotStatus = healSnapshotStatus;
        this.healOutcomes = healOutcomes == null ? null : List.copyOf(healOutcomes);
        this.cachedPlanStatus = cachedPlanStatus;
        this.observedBaseX = observedBaseX;
        this.observedBaseY = observedBaseY;
    }

    private static List<String> probeNames(List<RemoteProbeObservation> observations) {
        List<String> names = new ArrayList<>(observations.size());
        for (RemoteProbeObservation observation : observations) {
            names.add(observation.getName());
        }
        return names;
    }

    private static List<String> healNames(List<RemoteHealOutcome> outcomes) {
        List<String> names = new ArrayList<>(outcomes.size());
        for (RemoteHealOutcome outcome : outcomes) {
            names.add(outcome.getName());
        }
        return names;
    }

    private static void requireOrderedBarNames(List<String> names, String label) {
        if (!FIXED_BAR_NAMES.equals(names)) {
            throw new IllegalArgumentException(label + " must be exactly " + FIXED_BAR_NAMES + " in order");
        }
    }

    /** One closed per-bar no-focus observation; sample coordinates are window-client pixels. */
    @Value
    @Jacksonized
    public static class RemoteProbeObservation {
        String name;
        ProbeStatus status;
        Integer sampleRelX;
        Integer sampleRelY;

        @Builder
        public RemoteProbeObservation(String name, ProbeStatus status, Integer sampleRelX, Integer sampleRelY) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("observation name must not be blank");
            }
            if (status == null) {
                throw new IllegalArgumentException("observation status must not be null");
            }
            this.name = name;
            this.status = status;
            this.sampleRelX = sampleRelX;
            this.sampleRelY = sampleRelY;
        }
    }

    /**
     * One closed per-bar heal outcome. Sample coordinates are window-client pixels; click coordinates are
     * screen-absolute pixels and present only for {@link HealStatus#EXECUTED}.
     */
    @Value
    @Jacksonized
    public static class RemoteHealOutcome {
        String name;
        HealStatus status;
        Integer sampleRelX;
        Integer sampleRelY;
        Integer clickAbsX;
        Integer clickAbsY;

        @Builder
        public RemoteHealOutcome(String name, HealStatus status, Integer sampleRelX, Integer sampleRelY,
                                 Integer clickAbsX, Integer clickAbsY) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("heal outcome name must not be blank");
            }
            if (status == null) {
                throw new IllegalArgumentException("heal outcome status must not be null");
            }
            if ((clickAbsX == null) != (clickAbsY == null)) {
                throw new IllegalArgumentException("click coordinates must be present or absent as a pair");
            }
            boolean executed = status == HealStatus.EXECUTED;
            if (executed != (clickAbsX != null)) {
                throw new IllegalArgumentException("click coordinates must be present only for EXECUTED");
            }
            this.name = name;
            this.status = status;
            this.sampleRelX = sampleRelX;
            this.sampleRelY = sampleRelY;
            this.clickAbsX = clickAbsX;
            this.clickAbsY = clickAbsY;
        }
    }
}
