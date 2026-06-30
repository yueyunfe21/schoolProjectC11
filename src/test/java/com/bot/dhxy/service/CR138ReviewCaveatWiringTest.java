package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR138 review caveats that are hard to exercise without the JavaFX UI.
 */
public final class CR138ReviewCaveatWiringTest {

    private CR138ReviewCaveatWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        assertCommonBoxDeferredLogRequiresPendingBox();
        assertLocalTeamSessionRequiresAcceptedLeader();
        assertNonLocalAutoBattleDoesNotWaitStaleRequestedTaskGate();
        assertCombatLeftTopMaintenanceUsesLocalCapabilityGate();
        assertLocalSupportQueueIsNotCollapsedBeforeLiveRole();
        assertCandidateMembersDoNotUseLegacyGatesBeforeLeaderDetection();
        assertStaleSnapshotLeaderDoesNotCountAsDetectedLeader();
        assertRunnerReportsRawLiveRoleOnlyForLocalSessionEvidence();
    }

    private static void assertCommonBoxDeferredLogRequiresPendingBox() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int methodStart = source.indexOf("private boolean runPendingMemberCommonBoxIfAllowed");
        require(methodStart >= 0, "AutoCombatService must keep pending member common-box hook");
        int methodEnd = source.indexOf("private boolean runPendingFollowerFirstAidIfAllowed", methodStart);
        require(methodEnd > methodStart, "pending member common-box method boundary must be readable");
        String method = source.substring(methodStart, methodEnd);

        int pendingCheck = method.indexOf("hasPendingBoxForCurrentWindow");
        int localGate = method.indexOf("isLocalSupportMemberSession");
        int deferredLog = method.indexOf("pending member common-box deferred");
        require(pendingCheck >= 0, "common-box hook must check whether a box is actually pending");
        require(localGate >= 0, "common-box hook must still respect local support session gate");
        require(deferredLog >= 0, "common-box hook must keep local gate deferred diagnostics");
        require(pendingCheck < localGate && pendingCheck < deferredLog,
                "CR138 review caveat: do not log local COMMON_BOX deferred before confirming a pending box");
    }

    private static void assertLocalTeamSessionRequiresAcceptedLeader() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java"));
        int methodStart = source.indexOf("public WindowTaskCommandResult startSameQueue");
        require(methodStart >= 0, "WindowTaskControlService must keep startSameQueue");
        int methodEnd = source.indexOf("public WindowTaskCommandResult startSelectedTasks", methodStart);
        require(methodEnd > methodStart, "startSameQueue method boundary must be readable");
        String method = source.substring(methodStart, methodEnd);

        int roleQueue = method.indexOf("taskTeamAssignmentPolicy::shouldDetectRoleBeforeStart");
        int multiWindow = method.indexOf("ids.size() > 1");
        int candidate = method.indexOf("boolean localTeamSessionCandidate = supportsLocalTeamSession");
        int leaderSubmit = method.indexOf("taskManager.submitQueueWithResult(\n                    localLeaderWindowId");
        int leaderSuccess = method.indexOf("localTeamSessionActive = leaderSubmitResult.isSuccess()");
        int unknownLeaderCandidate = method.indexOf("localTeamSessionActive = localTeamSessionCandidate");
        int loop = method.indexOf("for (String windowId : ids)");
        int leaderReuse = method.indexOf("windowId.equals(localLeaderWindowId)");
        int memberSession = method.indexOf("localTeamSessionActive ? localTeamSessionKey : null");
        int disabledLog = method.indexOf("local-team session disabled because leader submit failed");
        int candidateLog = method.indexOf("local-team session candidate without known leader");

        require(roleQueue >= 0 && multiWindow >= 0 && candidate > roleQueue,
                "local team session must be created for multi-window team-role queues even when snapshots are UNKNOWN");
        require(leaderSubmit >= 0, "local team batch must submit the leader before members");
        require(leaderSuccess > leaderSubmit, "local session readiness must come from leader submit result");
        require(unknownLeaderCandidate > leaderSuccess,
                "unknown-leader batches must keep a candidate local session until live role detection confirms a leader");
        require(loop > unknownLeaderCandidate, "member submits must run only after local session readiness is known");
        require(leaderReuse > loop, "loop must reuse the already-submitted leader result");
        require(memberSession > leaderReuse,
                "member submit must receive session metadata only when the leader submit succeeded");
        require(disabledLog > leaderSuccess,
                "leader submit failure must log that the local-team session was disabled");
        require(candidateLog > unknownLeaderCandidate,
                "unknown-leader local-team session candidates must be visible in logs");
    }

    private static void assertNonLocalAutoBattleDoesNotWaitStaleRequestedTaskGate() throws Exception {
        String autoCombat = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int firstAidStart = autoCombat.indexOf("private boolean runPendingFollowerFirstAidIfAllowed");
        require(firstAidStart >= 0, "AutoCombatService must keep pending follower first-aid hook");
        int firstAidEnd = autoCombat.indexOf("private boolean shouldDeferFollowerFirstAid", firstAidStart);
        require(firstAidEnd > firstAidStart, "pending follower first-aid method boundary must be readable");
        String firstAidMethod = autoCombat.substring(firstAidStart, firstAidEnd);
        int localLeaderGuard = firstAidMethod.indexOf("context.isLocalLeaderPresent()");
        int oldRequestedGate = firstAidMethod.indexOf("awaitTeamFirstAidMaintenanceWindowOpen");
        require(localLeaderGuard >= 0, "legacy requested-task gate must require a local leader context");
        require(oldRequestedGate > localLeaderGuard,
                "non-local auto-battle must not wait on stale requestedTaskCode first-aid gates");

        String autoBattle = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/task/AutoBattleTask.java"));
        int maintenanceStart = autoBattle.indexOf("private void maybeRunIdleMaintenance");
        require(maintenanceStart >= 0, "AutoBattleTask must keep idle maintenance hook");
        int maintenanceEnd = autoBattle.indexOf("private boolean tryRunLocalTeamReturnRelease", maintenanceStart);
        require(maintenanceEnd > maintenanceStart, "idle maintenance method boundary must be readable");
        String maintenanceMethod = autoBattle.substring(maintenanceStart, maintenanceEnd);
        int localSession = maintenanceMethod.indexOf("boolean localSupportSession = taskMaintenanceService.isLocalSupportMemberSession(context)");
        int leftTopLocalCapability = maintenanceMethod.indexOf("TeamSupportCapability.LEFT_TOP_STATUS");
        int leftTopOldGate = maintenanceMethod.indexOf("isTeamPathingMaintenanceWindowOpen");
        int summonLocalCapability = maintenanceMethod.indexOf("TeamSupportCapability.SUMMON_SKILL");
        int requiredLocalCapability = maintenanceMethod.indexOf(".requiredLocalSupportCapability(");
        int legacyTeamGate = maintenanceMethod.indexOf("boolean requireLegacyTeamPathingGate");
        int legacyTeamKey = maintenanceMethod.indexOf(".teamMaintenanceKey(requireLegacyTeamPathingGate");
        int legacyOpenWindow = maintenanceMethod.indexOf(".requireOpenTeamMaintenanceWindow(requireLegacyTeamPathingGate)");
        require(localSession >= 0, "idle maintenance must distinguish local support session from standalone auto-battle");
        require(leftTopLocalCapability > localSession,
                "local support left-top status must use local LEFT_TOP_STATUS capability");
        require(leftTopOldGate < 0,
                "local support left-top status must not wait on stale requestedTaskCode pathing window");
        require(summonLocalCapability > localSession && requiredLocalCapability > localSession,
                "local support summon skill must use local SUMMON_SKILL capability");
        require(legacyTeamGate > localSession,
                "non-local follower support for team tasks must keep the legacy team pathing gate");
        require(legacyTeamKey > legacyTeamGate && legacyOpenWindow > legacyTeamGate,
                "requested xiuluo/wubei followers without local session must wait for the old team pathing window");
        require(maintenanceMethod.contains("requireLocalSupportGate || requireLegacyTeamPathingGate"),
                "summon skill round gate must apply to both local capability and legacy team pathing modes");
        require(!maintenanceMethod.contains(".teamMaintenanceKey(requireLocalSupportGate"),
                "local support summon skill must not use stale requestedTaskCode as team maintenance key");

        String request = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceRequest.java"));
        require(request.contains("TeamSupportCapability requiredLocalSupportCapability"),
                "TaskMaintenanceRequest must expose an explicit local support capability gate");

        String maintenance = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java"));
        require(maintenance.contains("TeamSupportCapability.SUMMON_SKILL")
                        && maintenance.contains("TeamSupportCapability.LEFT_TOP_STATUS"),
                "pathing release must publish local SUMMON_SKILL and LEFT_TOP_STATUS capabilities");
    }

    private static void assertCombatLeftTopMaintenanceUsesLocalCapabilityGate() throws Exception {
        String autoCombat = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int methodStart = autoCombat.indexOf("private void maybeRunCombatMaintenance");
        require(methodStart >= 0, "AutoCombatService must keep sparse combat maintenance hook");
        int methodEnd = autoCombat.indexOf("private AutoCombatRuntimeState state", methodStart);
        require(methodEnd > methodStart, "combat maintenance method boundary must be readable");
        String method = autoCombat.substring(methodStart, methodEnd);

        int localSupportGate = method.indexOf("taskMaintenanceService.isLocalSupportMemberSession(context)");
        int capability = method.indexOf("TeamSupportCapability.LEFT_TOP_STATUS");
        int capabilityOpen = method.indexOf("isLocalTeamSupportCapabilityOpen");
        int combatMaintenance = method.indexOf("leftTopStatusSwitchService.handleCombatMaintenance");
        require(combatMaintenance >= 0, "combat maintenance must still run left-top status close when allowed");
        require(localSupportGate >= 0 && localSupportGate < combatMaintenance,
                "local support combat left-top maintenance must first check local support session");
        require(capability >= 0 && capabilityOpen >= 0 && capabilityOpen < combatMaintenance,
                "local support combat left-top maintenance must wait for LEFT_TOP_STATUS capability");
        require(method.contains("local support combat left-top deferred"),
                "local support combat left-top maintenance must log when capability is closed");
    }

    private static void assertLocalSupportQueueIsNotCollapsedBeforeLiveRole() throws Exception {
        String runner = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"));
        int submitStart = runner.indexOf("public synchronized boolean submit(WindowTaskQueue queue,");
        require(submitStart >= 0, "WindowTaskRunner must keep local-team queue submit overload");
        int submitEnd = runner.indexOf("public void refreshRegistration", submitStart);
        require(submitEnd > submitStart, "local-team submit overload boundary must be readable");
        String submit = runner.substring(submitStart, submitEnd);

        require(!submit.contains("collapseLocalSupportQueue"),
                "submit must not irreversibly collapse a queue before live role preflight");
        require(!submit.contains("windowContext.getRole().isMember()"),
                "submit must not trust stale MEMBER role snapshots for CR138 support routing");
        require(!runner.contains("private WindowTaskQueue collapseLocalSupportQueue"),
                "local support queue collapse must not happen before live role confirmation");
        require(runner.contains("taskTeamAssignmentPolicy.resolveTaskForRole(requestedTaskType, assignmentRole)"),
                "live role preflight must remain the owner of member-to-auto-battle reassignment");
        String policy = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/task/startup/TaskTeamAssignmentPolicy.java"));
        require(policy.contains("safeRole.isMember()") && policy.contains("return TaskType.AUTO_BATTLE"),
                "member support routing must still resolve to AUTO_BATTLE after live role detection");

        int leaderPresent = submit.indexOf("boolean leaderPresent = localLeaderPresent && sessionKey != null");
        int leaderWindowRequired = submit.indexOf("leaderWindowId != null");
        require(leaderPresent >= 0,
                "candidate local sessions must not be dropped only because leaderWindowId is not known yet");
        require(leaderWindowRequired < 0,
                "runner submit must allow live role detection to discover the local leader later");

        require(runner.contains("taskMaintenanceService.markLocalTeamWindowRoleDetected"),
                "runner must report live role evidence so candidate sessions can resolve leaders");
        require(runner.contains("resolveLocalLeaderWindowId()"),
                "leader contexts should expose their own window id when the UI did not know it at submit time");

        String maintenance = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java"));
        require(maintenance.contains("localTeamSessions") && maintenance.contains("leaderWindowId"),
                "local support member sessions must wait for live-detected leader evidence stored in session state");
        require(maintenance.contains("markLocalTeamLeaderDetected"),
                "maintenance service must expose live leader registration for candidate sessions");
        require(maintenance.contains("&& hasDetectedLocalLeader(context)"),
                "members must not be treated as local support until a local leader is detected");
    }

    private static void assertCandidateMembersDoNotUseLegacyGatesBeforeLeaderDetection() throws Exception {
        String maintenance = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java"));
        require(maintenance.contains("registerLocalTeamSessionCandidate"),
                "unknown-leader local-team batches must register candidate windows");
        require(maintenance.contains("markLocalTeamWindowRoleDetected"),
                "runner preflight must tell maintenance when each candidate window has live role evidence");
        require(maintenance.contains("isPendingLocalSupportLeaderDetection"),
                "candidate members need a pending-leader state distinct from standalone auto-battle");
        require(maintenance.contains("isLocalSupportMemberCandidate"),
                "legacy gates must be able to exclude local support candidates even before leader detection");
        require(maintenance.contains("leaderAbsent"),
                "candidate sessions must be able to fall back only after no local leader is confirmed");
        require(maintenance.contains("completeLocalTeamSessionWindow"),
                "local-team session state must have an explicit end-of-session cleanup path");
        require(maintenance.contains("resolvedWindows.addAll(state.completedWindows)"),
                "leader-absent confirmation must count submit-failed/completed candidates as resolved");

        String control = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java"));
        require(control.contains("taskMaintenanceService.registerLocalTeamSessionCandidate"),
                "UI same-queue start must register all candidate windows for no-leader confirmation");

        String runner = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"));
        require(runner.contains("taskMaintenanceService.markLocalTeamWindowRoleDetected"),
                "runner live role preflight must mark every candidate window, not only leaders");
        require(runner.contains("taskMaintenanceService.completeLocalTeamSessionWindow"),
                "runner queue finish must release local-team session state");

        String autoBattle = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/task/AutoBattleTask.java"));
        int idleStart = autoBattle.indexOf("private void maybeRunIdleMaintenance");
        int idleEnd = autoBattle.indexOf("private boolean tryRunLocalTeamReturnRelease", idleStart);
        require(idleStart >= 0 && idleEnd > idleStart, "AutoBattleTask idle method boundary must be readable");
        String idle = autoBattle.substring(idleStart, idleEnd);
        int pendingLeader = idle.indexOf("isPendingLocalSupportLeaderDetection");
        int legacyReturn = idle.indexOf("clickReturnTeamIfPresent(context, \"auto-battle\")");
        require(pendingLeader >= 0 && pendingLeader < legacyReturn,
                "candidate members must not use legacy return-team before local leader detection finishes");

        String autoCombat = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"));
        int firstAidStart = autoCombat.indexOf("private boolean runPendingFollowerFirstAidIfAllowed");
        int firstAidEnd = autoCombat.indexOf("private boolean shouldDeferFollowerFirstAid", firstAidStart);
        require(firstAidStart >= 0 && firstAidEnd > firstAidStart,
                "AutoCombatService first-aid method boundary must be readable");
        String firstAid = autoCombat.substring(firstAidStart, firstAidEnd);
        int pendingFirstAid = firstAid.indexOf("isPendingLocalSupportLeaderDetection");
        int legacyFirstAid = firstAid.indexOf("awaitTeamFirstAidMaintenanceWindowOpen");
        int candidateExclusion = firstAid.indexOf("!taskMaintenanceService.isLocalSupportMemberCandidate(context)");
        require(pendingFirstAid >= 0 && pendingFirstAid < legacyFirstAid,
                "candidate members must defer first-aid before falling back to requested-task gates");
        require(candidateExclusion >= 0 && candidateExclusion < legacyFirstAid,
                "legacy requested-task first-aid gate must exclude local support candidates");
    }

    private static void assertStaleSnapshotLeaderDoesNotCountAsDetectedLeader() throws Exception {
        String maintenance = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java"));
        int methodStart = maintenance.indexOf("private boolean hasDetectedLocalLeader");
        require(methodStart >= 0, "TaskMaintenanceService must keep an explicit live-leader detector");
        int methodEnd = maintenance.indexOf("private void openLocalTeamSupportCapability", methodStart);
        require(methodEnd > methodStart, "hasDetectedLocalLeader method boundary must be readable");
        String method = maintenance.substring(methodStart, methodEnd);
        require(method.contains("state != null && state.leaderWindowId != null"),
                "local support sessions must require live leader evidence stored by session");
        require(!method.contains("getLocalLeaderWindowId"),
                "submit-time localLeaderWindowId is only an expected/snapshot id and must not prove a live leader");

        String behaviorGuard = Files.readString(Path.of(
                "src/test/java/com/bot/dhxy/service/TaskMaintenanceCR138LocalSupportCapabilityTest.java"));
        require(behaviorGuard.contains("stale submit-time leader id must not count as a live-detected local leader"),
                "behavior guard must cover stale leader snapshots before fresh runtime acceptance");
    }

    private static void assertRunnerReportsRawLiveRoleOnlyForLocalSessionEvidence() throws Exception {
        String runner = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"));
        int methodStart = runner.indexOf("private TaskType resolveTaskTypeBeforeStart");
        require(methodStart >= 0, "WindowTaskRunner must keep role preflight task resolution");
        int methodEnd = runner.indexOf("private TeamRoleStatus toTeamRoleStatus", methodStart);
        require(methodEnd > methodStart, "role preflight method boundary must be readable");
        String method = runner.substring(methodStart, methodEnd);

        require(method.contains("TeamRoleStatus liveRole = teamRoleDetectionService.detectCurrentRole"),
                "runner must keep raw live role separate from assignment fallback role");
        require(method.contains("TeamRoleStatus assignmentRole = liveRole"),
                "runner must resolve task assignment from a separate effective role");
        require(method.contains("syncWindowRole(liveRole)"),
                "cached window role must only be synced from raw live role evidence");
        require(method.contains("liveRole == null ? null : liveRole.name()"),
                "local-team session evidence must report raw live role, not cached assignment fallback role");
        require(method.contains("taskTeamAssignmentPolicy.resolveTaskForRole(requestedTaskType, assignmentRole)"),
                "assignment fallback may only affect task routing");
        require(!method.contains("use existing window role after live role unknown"),
                "logs must not present cached assignment fallback as live role evidence");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
