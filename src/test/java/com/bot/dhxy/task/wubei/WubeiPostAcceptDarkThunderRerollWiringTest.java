package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for post-accept 五倍 flow.
 *
 * <p>All accept-success paths must share the same post-accept behavior: start the prepath
 * immediately, wait for the tracker refresh window, then read the tracker. If the refreshed title
 * template says 殿前献艺/暗雷怪, the task must immediately send a current-map mini-map
 * reroute toward the accept NPC, then clear the post-accept prepath signal and continue the
 * normal reroll flow without the old fixed 4s delay.</p>
 */
public final class WubeiPostAcceptDarkThunderRerollWiringTest {

    private WubeiPostAcceptDarkThunderRerollWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String service = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java"), StandardCharsets.UTF_8);
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        darkThunderTemplateIsCheckedFirst(service);
        acceptSuccessPathsSharePostAcceptFlow(wubei);
        darkThunderRerollStartsImmediateAcceptNpcPrepath(wubei);
        routePhaseDoesNotDuplicateImmediateDarkThunderReroute(wubei);

        System.out.println("WubeiPostAcceptDarkThunderRerollWiringTest passed");
    }

    private static void darkThunderTemplateIsCheckedFirst(String service) {
        String templates = fieldInitializer(service, "WUBEI_TRACKER_TITLE_TEMPLATES");
        int darkThunder = indexOf(templates, "WUBEI_TASK_KEY_DIANQIAN_XIANYI");
        int sancang = indexOf(templates, "WUBEI_TASK_KEY_SANCANG_FENGMO");
        int baoxiang = indexOf(templates, "WUBEI_TASK_KEY_BAOXIANG_MIQING");
        int huangpao = indexOf(templates, "WUBEI_TASK_KEY_ZHIDOU_HUANGPAO");
        int kuixing = indexOf(templates, "WUBEI_TASK_KEY_KUIXING_GUIWEI");

        require(darkThunder < sancang && darkThunder < baoxiang
                        && darkThunder < huangpao && darkThunder < kuixing,
                "殿前献艺/暗雷怪 title template must be tried first");
    }

    private static void acceptSuccessPathsSharePostAcceptFlow(String wubei) {
        String normal = methodBody(wubei, "private WubeiStepOutcome runAcceptTaskPhase(");
        String priority = methodBody(wubei, "private WubeiStepOutcome consumePreparedAcceptBeforeNormalPhase(");
        String shared = methodBody(wubei, "private WubeiStepOutcome afterAcceptTaskSucceeded(");

        require(normal.contains("return afterAcceptTaskSucceeded("),
                "normal accept success must delegate to the shared post-accept flow");
        require(priority.contains("return afterAcceptTaskSucceeded("),
                "priority accept success must delegate to the shared post-accept flow");
        require(!priority.contains("TaskSleep.sleepOrStop(context, TRACKER_REFRESH_AFTER_ACCEPT_MS"),
                "priority accept must not keep a separate wait-only post-accept path");

        int confirm = indexOf(shared, "npcClickService.confirmPendingSmartClick");
        int schedule = indexOf(shared, "postAcceptTrackerPanelFuture = schedulePostAcceptTrackerPanelRead(state)");
        int prepath = indexOf(shared, "startPostAcceptPrepath(context, state)");
        int consumeRecovery = indexOf(shared, "consumePendingLeaderPostCombatRecoveryIfAllowed");
        int clearTracker = indexOf(shared, "currentTrackerPanel = null");
        int readTracker = indexOf(shared, "WubeiPhase.READ_TRACKER");

        require(confirm < schedule, "shared accept flow must confirm the NPC click before scheduling tracker read");
        require(schedule < prepath, "shared accept flow must schedule background tracker read before prepath");
        require(prepath < consumeRecovery, "shared accept flow must start prepath before recovery consumption");
        require(consumeRecovery < clearTracker, "shared accept flow should consume recovery before clearing tracker");
        require(clearTracker < readTracker, "shared accept flow must clear stale tracker before READ_TRACKER");
        require(!shared.contains("TaskSleep.sleepOrStop(context, TRACKER_REFRESH_AFTER_ACCEPT_MS"),
                "shared accept flow must not wait serially after prepath; background read owns the 1s delay");

        String scheduleBody = methodBody(wubei, "private CompletableFuture<TaskTrackerPanelReadResult> schedulePostAcceptTrackerPanelRead(");
        require(scheduleBody.contains("Thread.sleep(TRACKER_REFRESH_AFTER_ACCEPT_MS)"),
                "background tracker read must own the 1s post-accept delay");
        require(scheduleBody.contains("windowTaskContextHolder.callWith(runtime"),
                "background tracker read must bind the current window runtime before screenshot");
    }

    private static void darkThunderRerollStartsImmediateAcceptNpcPrepath(String wubei) {
        String readTracker = methodBody(wubei, "private WubeiStepOutcome runReadTrackerPhase(");
        String rerouteHelper = methodBody(wubei, "private NavigationResult startDarkThunderAcceptNpcReroute(");
        int darkThunder = indexOf(readTracker, "if (isTrackerDarkThunderTask(currentTrackerPanel))");
        int immediateReroute = indexOf(readTracker, "startDarkThunderAcceptNpcReroute(context, state)");
        int clearTracker = indexOf(readTracker, "currentTrackerPanel = null");
        int clearPrepath = indexOf(readTracker, "clearPostAcceptPrepathSignal(");
        int reroute = indexOf(readTracker, "state.next(WubeiPhase.ROUTE_TO_MAIN_TASK, \"dark-thunder-reroll\")");

        require(darkThunder < immediateReroute,
                "dark thunder branch must immediately start the accept-NPC mini-map reroute");
        require(immediateReroute < clearTracker,
                "dark thunder branch must click/reroute before cleanup so correction is not delayed");
        require(clearTracker < clearPrepath, "dark thunder branch must clear prepath after clearing tracker");
        require(clearPrepath < reroute, "dark thunder branch must clear prepath before rerouting");
        require(!readTracker.contains("TaskSleep.sleepOrStop(context, 4_000L"),
                "dark thunder reroll must not keep the old fixed 4s delay");
        require(wubei.contains("clearPathingSignalIfSourcePrefix(\"wubei:post-accept-prepath:\""),
                "dark thunder reroll must clear only the post-accept prepath signal");
        require(rerouteHelper.contains("navigationService.navigateInCurrentMap"),
                "dark thunder immediate reroute must use current-map mini-map navigation");
        require(rerouteHelper.contains(".targetMapName(START_MAP_NAME)")
                        && rerouteHelper.contains(".targetX(ACCEPT_NPC_X)")
                        && rerouteHelper.contains(".targetY(ACCEPT_NPC_Y)")
                        && rerouteHelper.contains(".targetName(ACCEPT_NPC_NAME)"),
                "dark thunder immediate reroute must target the accept NPC coordinate");
        require(wubei.contains("DARK_THUNDER_REROLL_PREPATH_SOURCE_PREFIX = \"wubei:dark-thunder-reroll-prepath:\""),
                "dark thunder immediate reroute must use a source prefix distinct from post-accept prepath");
        require(rerouteHelper.contains(".source(DARK_THUNDER_REROLL_PREPATH_SOURCE_PREFIX + state.round())"),
                "dark thunder immediate reroute must use a distinct pathing source");
    }

    private static void routePhaseDoesNotDuplicateImmediateDarkThunderReroute(String wubei) {
        String route = methodBody(wubei, "private WubeiStepOutcome runRouteToNPC(");
        String wait = methodBody(wubei, "private WubeiStepOutcome waitForAcceptNpcPathingIfStillActive(");

        int activePathing = indexOf(route, "waitForAcceptNpcPathingIfStillActive(state)");
        int navigateToNpc = indexOf(route, "navigationService.navigateToNPC");
        require(activePathing < navigateToNpc,
                "ROUTE_TO_MAIN_TASK must check active accept-NPC pathing before submitting another map click");
        require(wait.contains("intent.getTargetMapName(), START_MAP_NAME"),
                "accept-NPC active pathing gate must recognize same-map reroutes to the accept NPC");
        require(wait.contains("snapshot.getState() == WindowPathingState.ACTIVE"),
                "accept-NPC active pathing gate must yield while the immediate reroute is still moving");
        require(!wait.contains("startsWith(\"wubei:accept-npc\")")
                        && !wait.contains("equals(\"wubei:accept-npc\")"),
                "accept-NPC active pathing gate must not reject the dark-thunder reroute source");
    }

    private static String fieldInitializer(String source, String fieldName) {
        int start = source.indexOf(fieldName);
        if (start < 0) {
            throw new AssertionError("Missing field: " + fieldName);
        }
        int end = source.indexOf(");", start);
        if (end < 0) {
            throw new AssertionError("Missing initializer end for: " + fieldName);
        }
        return source.substring(start, end + 2);
    }

    private static String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0) {
            throw new AssertionError("Missing method signature: " + signature);
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        if (bodyStart < 0) {
            throw new AssertionError("Missing method body for: " + signature);
        }
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body for: " + signature);
    }

    private static int indexOf(String source, String needle) {
        int index = source.indexOf(needle);
        if (index < 0) {
            throw new AssertionError("Missing source marker: " + needle);
        }
        return index;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
