package com.bot.dhxy.window.observation;

import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * G002 W1, in-process form: the leader window's local combat edge is fanned out to same-process
 * member window contexts, so member samplers never run their own combat-signal capture loop.
 *
 * <p>The Cloud half of G002 W1 is unchanged and still authoritative for lifecycle: it suppresses
 * the member {@code combat-signal} interest while a locally controlled leader is present and
 * republishes it when the leader goes away, at which point member self-detection resumes on the
 * interest-driven path. This class only carries the leader's already-confirmed local edge to the
 * member runtime flag that gates combat-only local duties (auto-panel watch, combat input fences).</p>
 */
public final class LocalLeaderCombatBroadcast {

    private static final Logger log = LoggerFactory.getLogger(LocalLeaderCombatBroadcast.class);

    private final Supplier<List<WindowRuntimeContext>> registeredContexts;

    public LocalLeaderCombatBroadcast(Supplier<List<WindowRuntimeContext>> registeredContexts) {
        this.registeredContexts = Objects.requireNonNull(registeredContexts, "registeredContexts");
    }

    /**
     * Applies one leader combat edge to every registered member window.
     *
     * @param leader the window whose sampler confirmed the edge; ignored unless its role is LEADER
     * @param entered {@code true} for combat entry, {@code false} for combat exit
     */
    public void publishLeaderCombatEdge(WindowRuntimeContext leader, boolean entered) {
        if (leader == null || leader.getRole() != WindowRole.LEADER) {
            return;
        }
        for (WindowRuntimeContext candidate : registeredContexts.get()) {
            if (candidate == null || candidate == leader || candidate.getRole() != WindowRole.MEMBER) {
                continue;
            }
            candidate.updateLocalCombatGeneration(0L, entered);
            log.info("[local-runner] leader-broadcast:{} applied to member window: leader={} member={}",
                    entered ? "IN_COMBAT" : "COMBAT_EXITED",
                    leader.getWindowId(), candidate.getWindowId());
        }
    }
}
