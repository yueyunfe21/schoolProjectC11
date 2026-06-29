package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small source tripwire for CR120 common-box wiring.
 *
 * <p>Behavior details such as pending lifetime, identity drift, and first-aid ordering are covered
 * by focused behavior tests. This guard only checks the broad architecture boundaries that are hard
 * to exercise without a live client.</p>
 */
public final class CommonBoxLogicWiringGuard {

    private static final Path BOT_PROPERTIES = Path.of(
            "src/main/java/com/bot/dhxy/config/BotProperties.java");
    private static final Path MAIN_WINDOW = Path.of(
            "src/main/java/com/bot/dhxy/ui/MainWindowController.java");
    private static final Path COMMON_BOX = Path.of(
            "src/main/java/com/bot/dhxy/service/CommonBoxService.java");
    private static final Path WINDOW_RUNNER = Path.of(
            "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java");
    private static final Path MAINTENANCE_REQUEST = Path.of(
            "src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceRequest.java");
    private static final Path MAINTENANCE_SERVICE = Path.of(
            "src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java");
    private static final Path AUTO_COMBAT = Path.of(
            "src/main/java/com/bot/dhxy/service/AutoCombatService.java");
    private static final Path XIULUO = Path.of(
            "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");
    private static final Path WUBEI = Path.of(
            "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");
    private static final Path TEMPLATE = Path.of(
            "images/template/common/leader_box_marker.png");

    private CommonBoxLogicWiringGuard() {
    }

    public static void main(String[] args) throws Exception {
        require(Files.exists(TEMPLATE), "CR120 template must exist");

        String botProperties = read(BOT_PROPERTIES);
        require(botProperties.contains("leaderCommonBoxEnabled = true"),
                "CR120 leader box switch must default on");
        require(botProperties.contains("memberCommonBoxEnabled = false"),
                "CR120 member box switch must default off");

        String mainWindow = read(MAIN_WINDOW);
        require(mainWindow.contains("leaderCommonBoxEnabledCheckBox")
                        && mainWindow.contains("memberCommonBoxEnabledCheckBox"),
                "CR120 UI must expose independent leader/member switches");

        String commonBox = read(COMMON_BOX);
        require(commonBox.contains("ROI_LEFT = 623")
                        && commonBox.contains("ROI_TOP = 590")
                        && commonBox.contains("ROI_RIGHT = 682")
                        && commonBox.contains("ROI_BOTTOM = 618"),
                "CR120 detector must keep the agreed window-relative ROI");
        require(commonBox.contains("cachedTemplate")
                        && commonBox.contains("template-unavailable")
                        && commonBox.contains("ImageFinder.find(raw, template, TEMPLATE_THRESHOLD)"),
                "CR120 detector must use cached in-memory template matching and fail closed");
        require(commonBox.contains("detectLeaderBoxAfterReturnHome")
                        && commonBox.contains("detectMemberBoxAfterCombatExit")
                        && commonBox.contains("consumePendingBoxIfAllowed"),
                "CR120 service must keep explicit detect/consume entry points");

        String request = read(MAINTENANCE_REQUEST);
        String maintenance = read(MAINTENANCE_SERVICE);
        require(!request.contains("consumeCommonBox") && !maintenance.contains("CommonBoxService"),
                "CR120 common-box must not be wired into generic maintenance request/service");

        String autoCombat = read(AUTO_COMBAT);
        require(autoCombat.contains("detectMemberBoxAfterCombatExit")
                        && autoCombat.contains("runPendingMemberCommonBoxIfAllowed"),
                "CR120 member box must keep auto-combat detect/consume hooks");

        String xiuluo = read(XIULUO);
        String wubei = read(WUBEI);
        require(xiuluo.contains("detectLeaderBoxAfterReturnHome(context, \"xiuluo_v2\"")
                        && xiuluo.contains("commonBoxService.consumePendingBoxIfAllowed(context, \"xiuluo_v2\""),
                "CR120 Xiuluo leader hooks must remain explicit");
        require(wubei.contains("detectLeaderBoxAfterReturnHome(context, \"wubei\"")
                        && wubei.contains("commonBoxService.consumePendingBoxIfAllowed(context, TASK_CODE"),
                "CR120 Wubei leader hooks must remain explicit");

        String windowRunner = read(WINDOW_RUNNER);
        require(windowRunner.contains("private static final AtomicLong GLOBAL_TASK_RUN_SEQUENCE")
                        && windowRunner.contains(".taskRunId(GLOBAL_TASK_RUN_SEQUENCE.incrementAndGet())"),
                "CR120 taskRunId must come from an app-global monotonic sequence");
    }

    private static String read(Path path) throws Exception {
        if (!Files.exists(path)) {
            throw new AssertionError("Missing expected file: " + path);
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
