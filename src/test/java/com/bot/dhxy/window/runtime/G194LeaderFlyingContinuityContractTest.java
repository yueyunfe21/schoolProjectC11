package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.model.WindowFlyingState;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** G194: one flying-continuity fact belongs to one exact team leader, never to each member. */
class G194LeaderFlyingContinuityContractTest {

    private static final Path INPUT_WORKER = Path.of(
            "src/main/java/com/bot/dhxy/input/action/InputActionWorker.java");
    private static final Path OBSERVATION_SAMPLER = Path.of(
            "src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java");
    private static final Path CLOUD_ADJUSTMENT = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudFlightAdjustmentService.java");

    @Test
    void memberCannotPublishOrReadAnIndependentFlyingState() {
        WindowRuntimeContext member = runtime("member", "101", WindowRole.MEMBER);

        assertFalse(member.updateLeaderFlyingState(
                WindowFlyingState.FLYING, "member-must-not-own"));

        assertEquals(WindowFlyingState.UNKNOWN,
                member.readLeaderFlyingState("member-read"));
    }

    @Test
    void independentLeadersDoNotShareTeamFlyingState() {
        WindowRuntimeContext firstLeader = runtime("leader-a", "201", WindowRole.LEADER);
        WindowRuntimeContext secondLeader = runtime("leader-b", "202", WindowRole.LEADER);

        firstLeader.updateLeaderFlyingState(WindowFlyingState.FLYING, "first-team");
        secondLeader.updateLeaderFlyingState(WindowFlyingState.NOT_FLYING, "second-team");

        assertEquals(WindowFlyingState.FLYING,
                firstLeader.readLeaderFlyingState("first-team-read"));
        assertEquals(WindowFlyingState.NOT_FLYING,
                secondLeader.readLeaderFlyingState("second-team-read"));
    }

    @Test
    void roleBindingAndRunBoundariesInvalidateLeaderContinuity() {
        WindowRuntimeContext leader = runtime("leader", "301", WindowRole.LEADER);
        leader.updateLeaderFlyingState(WindowFlyingState.FLYING, "confirmed");
        leader.setRole(WindowRole.MEMBER);
        assertEquals(WindowFlyingState.UNKNOWN, leader.readLeaderFlyingState("after-role-change"));

        leader.setRole(WindowRole.LEADER);
        leader.updateLeaderFlyingState(WindowFlyingState.FLYING, "confirmed-again");
        leader.setNativeBinding(binding("302"));
        assertEquals(WindowFlyingState.UNKNOWN, leader.readLeaderFlyingState("after-hwnd-change"));

        leader.updateLeaderFlyingState(WindowFlyingState.FLYING, "confirmed-third-time");
        leader.clearTaskExecutionState("new-run");
        assertEquals(WindowFlyingState.UNKNOWN, leader.readLeaderFlyingState("after-run-reset"));
    }

    @Test
    void lateFlyingWriteCannotOverwriteConfirmedCombatGrounding() {
        WindowRuntimeContext leader = runtime("leader-combat", "401", WindowRole.LEADER);
        leader.updateLocalCombatGeneration(7L, true);

        assertFalse(leader.updateLeaderFlyingState(
                WindowFlyingState.FLYING, "late-g181-success"));
        assertEquals(WindowFlyingState.UNKNOWN,
                leader.readLeaderFlyingState("after-late-write"));
        assertTrue(leader.updateLeaderFlyingState(
                WindowFlyingState.NOT_FLYING, "runner-combat-entry"));
        assertEquals(WindowFlyingState.NOT_FLYING,
                leader.readLeaderFlyingState("combat-grounded"));
    }

    @Test
    void commonAdjustmentOwnsReuseAndCentralInvalidatorsOwnPhysicalTruth() throws Exception {
        String adjustment = Files.readString(CLOUD_ADJUSTMENT, StandardCharsets.UTF_8);
        String worker = Files.readString(INPUT_WORKER, StandardCharsets.UTF_8);
        String sampler = Files.readString(OBSERVATION_SAMPLER, StandardCharsets.UTF_8);

        assertTrue(adjustment.contains("runtimeFacts.readLeaderFlyingState("));
        assertTrue(adjustment.contains("runtimeFacts.updateLeaderFlyingState("));
        assertTrue(adjustment.contains("skipPixels=true skipAltC=true"));
        assertFalse(adjustment.contains("taskCode == \"wubei\"")
                        || adjustment.contains("TASK_CODE.equals(\"wubei\")"),
                "公共飞行服务不得把连续状态写成五倍专用分支");
        assertTrue(worker.contains("context.clearLeaderFlyingState(\"physical Alt+C:"),
                "队长真实 Alt+C 必须在物理执行点让旧状态失效");
        assertTrue(sampler.contains("context.updateLeaderFlyingState(")
                        && sampler.contains("WindowFlyingState.NOT_FLYING"),
                "Runner 精确入战必须把队伍状态记为落地");
    }

    private static WindowRuntimeContext runtime(String windowId, String hwnd, WindowRole role) {
        WindowRuntimeContext context = new WindowRuntimeContext(windowId, new GameContext());
        context.setNativeBinding(binding(hwnd));
        context.setRole(role);
        return context;
    }

    private static WindowNativeBinding binding(String hwnd) {
        return new WindowNativeBinding(hwnd, "测试窗口", "test", 1L,
                100, 100, 1024, 768);
    }
}
