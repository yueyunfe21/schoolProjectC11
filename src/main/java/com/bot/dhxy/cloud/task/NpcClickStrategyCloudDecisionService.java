package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.npc.NpcClickRequest;
import com.bot.dhxy.task.model.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Legacy NPC_CLICK_STRATEGY bridge retained as an explicit no-click guard.
 *
 * <p>This bridge must not be used as a production cloud success path. CR165 covered ordinary dialog
 * NPC clicks now use {@code NPC_CLICK_SMART}, where cloud owns the click action and this process only
 * validates coordinates and executes input. Active and inactive bridge states both block local
 * strategy execution instead of accepting any legacy local-strategy authorization response or
 * silently authorizing local passthrough.</p>
 */
@Service
public class NpcClickStrategyCloudDecisionService {

    private static final Logger log = LoggerFactory.getLogger(NpcClickStrategyCloudDecisionService.class);
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String LOCAL_CANDIDATE_ID = "local-strategy";

    private final CloudDecisionCoordinator coordinator;

    public NpcClickStrategyCloudDecisionService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Reject legacy local NPC strategy execution.
     *
     * @param request local smart-click request; target coordinates remain local diagnostics and do
     *                not become executable click coordinates.
     * @param strategy stable local strategy name, for example {@code NPC_YELLOW_TARGET}.
     * @param verificationMode local verifier mode such as {@code dialog} or {@code direct-combat}.
     * @return explicit no-click rejection. The cloud client is not called because this service is no
     *         longer a production authority.
     */
    public NpcClickStrategyCloudDecision authorizeStrategy(
            NpcClickRequest request,
            String strategy,
            String verificationMode) {
        String normalizedStrategy = safe(strategy);
        String normalizedVerification = safe(verificationMode);
        String localDecision = localDecision(normalizedStrategy, normalizedVerification);
        String taskCode = taskCode(request == null ? null : request.sourceTask());
        String rejectReason = "NPC_CLICK_STRATEGY production bridge disabled; use NPC_CLICK_SMART";
        log.warn("cloud.execute serviceId=NPC_CLICK_STRATEGY accepted=false no-click task={} npc={} strategy={} verification={} rejectReason={}",
                taskCode,
                request == null ? null : request.npcName(),
                normalizedStrategy,
                normalizedVerification,
                rejectReason);
        return NpcClickStrategyCloudDecision.cloudRejectedNoClick(
                null, localDecision, normalizedStrategy, rejectReason);
    }

    private static String localDecision(String strategy, String verificationMode) {
        return "candidateId=" + LOCAL_CANDIDATE_ID
                + ";strategy=" + strategy
                + ";verification=" + verificationMode;
    }

    private static String taskCode(TaskType taskType) {
        return taskType == null ? DEFAULT_TASK_CODE : taskType.getCode();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
