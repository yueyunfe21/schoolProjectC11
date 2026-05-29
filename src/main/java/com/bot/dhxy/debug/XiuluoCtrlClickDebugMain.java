//package com.bot.dhxy.debug;
//
//import com.bot.dhxy.core.GameContext;
//import com.bot.dhxy.input.InputSequences;
//import com.bot.dhxy.model.dialog.DialogResult;
//import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
//import com.bot.dhxy.model.npc.NpcClickRequest;
//import com.bot.dhxy.model.ocr.OcrWindowRegion;
//import com.bot.dhxy.service.DialogService;
//import com.bot.dhxy.service.NpcClickService;
//import com.bot.dhxy.service.PlayerStateService;
//import com.bot.dhxy.service.dialog.DialogHandleRequest;
//import com.bot.dhxy.task.model.TaskType;
//import com.bot.dhxy.tools.CoordinateHelper;
//import com.bot.dhxy.vision.OcrRoiMemoryService;
//import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
//import com.bot.dhxy.window.execution.MultiWindowTaskManager;
//import com.bot.dhxy.window.execution.WindowTaskRunner;
//import com.bot.dhxy.window.interaction.WindowFocusService;
//import com.bot.dhxy.window.runtime.WindowRuntimeContext;
//import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.WebApplicationType;
//import org.springframework.boot.SpringBootConfiguration;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.builder.SpringApplicationBuilder;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.FilterType;
//
//import java.awt.Point;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
///**
// * Debug-only probe for the Xiuluo accept NPC Ctrl-menu click path.
// *
// * <p>This main intentionally skips Xiuluo navigation, task-panel parsing, and objective reading. It
// * assumes the leader is already near the Ling Shou Village accept NPC, then tests only this chain:
// * hold Ctrl, scan the yellow NPC menu, click {@code 灵兽村使者}, verify the Xiuluo accept option dialog,
// * click {@code 闲来无事}, and exit. It uses the normal bound-window/input-queue services so screenshots
// * are written to the same window-scoped temp folder as production.</p>
// *
// * <p>Optional JVM property: {@code -Dxiuluo.ctrl.windowHandle=<native handle>} selects a registered
// * game window explicitly. Without it, the current foreground game window is preferred.</p>
// */
//public class XiuluoCtrlClickDebugMain {
//
//    private static final String START_MAP_NAME = "灵兽村";
//    private static final String ACCEPT_NPC_NAME = "灵兽村使者";
//    private static final int ACCEPT_NPC_X = 112;
//    private static final int ACCEPT_NPC_Y = 93;
//    private static final String NPC_TAG_TEMPLATE = "images/template/npc/npc_tag.png";
//    private static final String NPC_TASK_TOOLTIP_TEMPLATE = "images/template/npc/npc_task_tooltip.png";
//    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo_accept_xianlaiwu.png";
//    private static final String OPTION_ACCEPT_TASK = "accept-task";
//    private static final String MODE_SMART_CLICK = "smart";
//    private static final String MODE_TOOLTIP_CLICK = "tooltip";
//    private static final String MODE_PURPLE_CLICK = "purple";
//
//    public static void main(String[] args) {
//        ConfigurableApplicationContext app = new SpringApplicationBuilder(ToolSpringConfig.class)
//                .headless(false)
//                .web(WebApplicationType.NONE)
//                .properties(
//                        "bot.run.show-ui=false",
//                        "bot.run.auto-start=false",
//                        "bot.run.init-game-window=false"
//                )
//                .run(args);
//
//        try {
//            DebugContext debug = DebugContext.from(app);
//            debug.windowHolder.runWith(debug.window,
//                    () -> debug.gameContext.runWithState(debug.window.getGameState(), () -> runProbe(debug)));
//        } finally {
//            app.close();
//            System.exit(0);
//        }
//    }
//
//    @SpringBootConfiguration
//    @EnableAutoConfiguration
//    @ComponentScan(
//            basePackages = "com.bot.dhxy",
//            excludeFilters = {
//                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bot\\.dhxy\\.AutoBot"),
//                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bot\\.dhxy\\.ui\\..*"),
//                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CommandLineRunner.class)
//            }
//    )
//    static class ToolSpringConfig {
//    }
//
//    /**
//     * Execute the minimal NPC click probe against the bound window.
//     *
//     * @param debug already-selected debug context. The default purple mode sends one real click
//     *              through {@link NpcClickService#debugClickNpcByPurpleAnchorOnly(NpcClickRequest)}
//     *              and does not run tooltip, yellow OCR, or Ctrl fallback.
//     */
//    private static void runProbe(DebugContext debug) {
//        log("start npc-click probe windowId=" + debug.window.getWindowId()
//                + " handle=" + debug.window.getNativeBinding().getNativeHandle()
//                + " title=" + debug.window.getNativeBinding().getTitle());
//
//        String mode = System.getProperty("xiuluo.ctrl.mode", MODE_PURPLE_CLICK);
//        NpcClickRequest request = debugRequest(debug);
//
//        if (MODE_PURPLE_CLICK.equalsIgnoreCase(mode)) {
//            syncPlayerState(debug);
//            boolean clicked = debug.npcClickService.debugClickNpcByPurpleAnchorOnly(request);
//            log("purple-mode clickedExpectedDialog=" + clicked);
//            return;
//        }
//
//        if (MODE_SMART_CLICK.equalsIgnoreCase(mode)) {
//            syncPlayerState(debug);
//            boolean clicked = debug.npcClickService.clickNpcSmart(request);
//            log("smart-mode clickedExpectedDialog=" + clicked);
//            if (clicked) {
//                DialogResult result = debug.dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
//                        "xiuluo-ctrl-debug:smart-accept-option",
//                        List.of(new GreenTemplateClickSpec(
//                                OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 80, 4)),
//                        false));
//                String matched = result.getActionKey();
//                log("smart-mode acceptOption matched=" + matched);
//            }
//            return;
//        }
//        if (MODE_TOOLTIP_CLICK.equalsIgnoreCase(mode)) {
//            boolean clicked = clickTaskTooltipFromRecommendedRegions(debug);
//            log("tooltip-mode clickedExpectedDialog=" + clicked);
//            if (clicked) {
//                DialogResult result = debug.dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
//                        "xiuluo-ctrl-debug:tooltip-accept-option",
//                        List.of(new GreenTemplateClickSpec(
//                                OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 80, 4)),
//                        false));
//                String matched = result.getActionKey();
//                log("tooltip-mode acceptOption matched=" + matched);
//            }
//            return;
//        }
//
//        List<Point> probePoints = resolveProbePoints(debug.window);
//        log("probePoints=" + probePoints);
//        boolean opened = debug.npcClickService.debugClickNpcCtrlMenuAtPoints(
//                ACCEPT_NPC_NAME, ACCEPT_OPTION_TEMPLATE, probePoints, false);
//        log("smartClick openedExpectedDialog=" + opened);
//
//        if (!opened) {
//            log("result=false stage=ctrlMenuClick");
//            return;
//        }
//
//        DialogResult result = debug.dialogService.handleDialog(DialogHandleRequest.handleGreenTemplateOption(
//                "xiuluo-ctrl-debug:accept-option",
//                List.of(new GreenTemplateClickSpec(
//                        OPTION_ACCEPT_TASK, ACCEPT_OPTION_TEMPLATE, -5, 80, 4)),
//                false));
//        String matched = result.getActionKey();
//        log("acceptOption matched=" + matched);
//        log("result=" + OPTION_ACCEPT_TASK.equals(matched));
//    }
//
//    /**
//     * Build the probe target from JVM properties while preserving the old Xiuluo defaults.
//     *
//     * <p>For Zhang Wen, run with:
//     * {@code -Dxiuluo.ctrl.map=长安 -Dxiuluo.ctrl.name=张闻 -Dxiuluo.ctrl.x=224 -Dxiuluo.ctrl.y=100 -Dxiuluo.ctrl.expected=}.</p>
//     */
//    private static NpcClickRequest debugRequest(DebugContext debug) {
//        String mapName = System.getProperty("xiuluo.ctrl.map", START_MAP_NAME);
//        String npcName = System.getProperty("xiuluo.ctrl.name", ACCEPT_NPC_NAME);
//        int x = Integer.getInteger("xiuluo.ctrl.x", ACCEPT_NPC_X);
//        int y = Integer.getInteger("xiuluo.ctrl.y", ACCEPT_NPC_Y);
//        int tuneX = Integer.getInteger("xiuluo.ctrl.tuneX", -10);
//        int tuneY = Integer.getInteger("xiuluo.ctrl.tuneY", 0);
//        String expected = System.getProperty("xiuluo.ctrl.expected", ACCEPT_OPTION_TEMPLATE);
//        if (expected != null && expected.isBlank()) {
//            expected = null;
//        }
//        log("target map=" + mapName + " name=" + npcName + " coord=(" + x + "," + y + ") tune=("
//                + tuneX + "," + tuneY + ") expected=" + expected);
//        return NpcClickRequest.fixedWithTune(
//                debug.window.getGameState().getMe(), mapName, x, y, npcName, tuneX, tuneY, expected);
//    }
//
//    private static void syncPlayerState(DebugContext debug) {
//        debug.playerStateService.syncMyIdentity();
//        debug.playerStateService.syncMyPosition();
//        log("player=" + debug.window.getGameState().getMe().getName()
//                + " map=" + debug.window.getGameState().getMe().getCurrentMapName()
//                + " coord=(" + debug.window.getGameState().getMe().getX()
//                + "," + debug.window.getGameState().getMe().getY() + ")");
//    }
//
//    /**
//     * Debug the proposed fast accept-NPC path by matching the visible task tooltip template inside
//     * the same OCR ROI recommendation list used by smart NPC clicking.
//     *
//     * <p>Regions from {@link OcrRoiMemoryService} are window-relative 1024x768 client pixels. This
//     * method converts each one to a screen-absolute rectangle, asks {@link CoordinateHelper} to
//     * match {@code npc_task_tooltip.png} inside that rectangle, then sends one queued left click on
//     * the matched center. It deliberately does not use Ctrl; Ctrl should remain the later fallback
//     * if this direct tooltip path misses.</p>
//     *
//     * @param debug selected bound window and shared services.
//     * @return true when clicking the tooltip opens the Xiuluo accept option dialog.
//     */
//    private static boolean clickTaskTooltipFromRecommendedRegions(DebugContext debug) {
//        List<OcrWindowRegion> regions = debug.roiMemoryService.recommendNpcClickWindowRegions(
//                START_MAP_NAME, ACCEPT_NPC_X, ACCEPT_NPC_Y, null, null, ACCEPT_NPC_NAME, false);
//        int baseX = debug.window.getNativeBinding().getX();
//        int baseY = debug.window.getNativeBinding().getY();
//        log("tooltip-mode regions=" + regions);
//        for (int i = 0; i < regions.size(); i++) {
//            OcrWindowRegion region = regions.get(i).clamp(1024, 768);
//            if (!region.isValid()) {
//                continue;
//            }
//            int[] rect = new int[]{
//                    baseX + region.x1(),
//                    baseY + region.y1(),
//                    baseX + region.x2(),
//                    baseY + region.y2()
//            };
//            Point point = debug.coordinateHelper.findImageInRegion(NPC_TASK_TOOLTIP_TEMPLATE, rect, 0.82);
//            log("tooltip-mode region#" + (i + 1) + "=" + region.toShortText()
//                    + " rect=[" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3] + "]"
//                    + " matchedPoint=" + point);
//            if (point == null) {
//                continue;
//            }
//            boolean clickSent = debug.inputSequences.clickLeft(
//                    "xiuluo-debug:tooltipNpcClick", point.x, point.y, 1200);
//            log("tooltip-mode clicked=" + clickSent + " point=" + point);
//            /*
//             * DialogService cleanup is moving formal task code to handleDialog(...). Keep this
//             * throwaway debug path from depending on the old visibility-only API while that cleanup
//             * is in progress.
//             */
//            return clickSent;
//        }
//        return false;
//    }
//
//    /**
//     * Resolve screen-absolute Ctrl origins for the debug run.
//     *
//     * <p>Explicit {@code xiuluo.ctrl.points} values use the {@code x,y;x,y} format and are already
//     * screen-absolute. The fallback points are based on the recent failing log: the yellow target
//     * text was around window-relative {@code (171,79)}, while the production click tested an
//     * above-text point. Trying above/text/below plus the center gives a quick yes/no answer about
//     * whether Ctrl wants a different origin without running the full Xiuluo task.</p>
//     *
//     * @param window selected bound runtime window; used to translate fallback window-relative points.
//     * @return ordered screen-absolute Ctrl origins.
//     */
//    private static List<Point> resolveProbePoints(WindowRuntimeContext window) {
//        List<Point> explicit = parseScreenAbsolutePoints(System.getProperty("xiuluo.ctrl.points"));
//        if (!explicit.isEmpty()) {
//            return explicit;
//        }
//        int baseX = window.getNativeBinding().getX();
//        int baseY = window.getNativeBinding().getY();
//        return List.of(
//                new Point(baseX + 171, baseY + 29),
//                new Point(baseX + 171, baseY + 79),
//                new Point(baseX + 171, baseY + 129),
//                new Point(baseX + 512, baseY + 404)
//        );
//    }
//
//    /**
//     * Parse a semicolon-separated list of screen-absolute Ctrl origins.
//     *
//     * @param raw raw JVM property value, for example {@code 625,173;625,223}.
//     * @return parsed points. Malformed entries are skipped and logged to stdout.
//     */
//    private static List<Point> parseScreenAbsolutePoints(String raw) {
//        List<Point> points = new ArrayList<>();
//        if (raw == null || raw.isBlank()) {
//            return points;
//        }
//        for (String item : raw.split(";")) {
//            String[] parts = item.trim().split(",");
//            if (parts.length != 2) {
//                log("skip malformed point=" + item);
//                continue;
//            }
//            try {
//                points.add(new Point(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
//            } catch (NumberFormatException e) {
//                log("skip malformed point=" + item + " reason=" + e.getMessage());
//            }
//        }
//        return points;
//    }
//
//    /**
//     * Resolve and bind the game window used by the debug probe.
//     *
//     * <p>The selection order matches other local debug mains: explicit native handle, foreground
//     * game window, then the single registered window. This keeps the tool usable from IntelliJ
//     * without opening the JavaFX UI.</p>
//     */
//    private record DebugContext(
//            WindowTaskContextHolder windowHolder,
//            WindowRuntimeContext window,
//            GameContext gameContext,
//            NpcClickService npcClickService,
//            DialogService dialogService,
//            OcrRoiMemoryService roiMemoryService,
//            CoordinateHelper coordinateHelper,
//            InputSequences inputSequences,
//            PlayerStateService playerStateService
//    ) {
//        private static DebugContext from(ConfigurableApplicationContext app) {
//            GameWindowRegistrationService registration = app.getBean(GameWindowRegistrationService.class);
//            MultiWindowTaskManager manager = app.getBean(MultiWindowTaskManager.class);
//            WindowFocusService focusService = app.getBean(WindowFocusService.class);
//            WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);
//
//            registration.registerDetectedGameWindows(TaskType.XIULUO);
//            List<WindowTaskRunner> runners = new ArrayList<>(manager.getAllRunners());
//            String preferredHandle = normalizeHandle(System.getProperty("xiuluo.ctrl.windowHandle"));
//            String foregroundHandle = normalizeHandle(focusService.getForegroundNativeHandleText());
//
//            log("registeredWindowCount=" + runners.size()
//                    + " preferredHandle=" + preferredHandle
//                    + " foregroundHandle=" + foregroundHandle);
//            for (WindowTaskRunner runner : runners) {
//                WindowRuntimeContext item = runner.getWindowContext();
//                log("candidate windowId=" + item.getWindowId()
//                        + " handle=" + item.getNativeBinding().getNativeHandle()
//                        + " rect=" + item.getNativeBinding().getGeometryText()
//                        + " title=" + item.getNativeBinding().getTitle());
//            }
//
//            WindowTaskRunner selected = selectRunner(runners, preferredHandle, foregroundHandle)
//                    .orElseThrow(() -> new IllegalStateException(
//                            "No game window selected; focus one DHXY client or pass -Dxiuluo.ctrl.windowHandle=<handle>"));
//            WindowRuntimeContext window = selected.getWindowContext();
//            log("selected windowId=" + window.getWindowId()
//                    + " handle=" + window.getNativeBinding().getNativeHandle());
//
//            return new DebugContext(
//                    windowHolder,
//                    window,
//                    app.getBean(GameContext.class),
//                    app.getBean(NpcClickService.class),
//                    app.getBean(DialogService.class),
//                    app.getBean(OcrRoiMemoryService.class),
//                    app.getBean(CoordinateHelper.class),
//                    app.getBean(InputSequences.class),
//                    app.getBean(PlayerStateService.class)
//            );
//        }
//
//        private static Optional<WindowTaskRunner> selectRunner(List<WindowTaskRunner> runners,
//                                                              String preferredHandle,
//                                                              String foregroundHandle) {
//            Optional<WindowTaskRunner> preferred = selectByHandle(runners, preferredHandle, "preferredHandle");
//            if (preferred.isPresent()) {
//                return preferred;
//            }
//            Optional<WindowTaskRunner> foreground = selectByHandle(runners, foregroundHandle, "foreground");
//            if (foreground.isPresent()) {
//                return foreground;
//            }
//            if (runners.size() == 1) {
//                log("selectReason=singleRegisteredWindow");
//                return Optional.of(runners.get(0));
//            }
//            return Optional.empty();
//        }
//
//        private static Optional<WindowTaskRunner> selectByHandle(List<WindowTaskRunner> runners,
//                                                                String handle,
//                                                                String reason) {
//            if (handle == null || handle.isBlank()) {
//                return Optional.empty();
//            }
//            Optional<WindowTaskRunner> selected = runners.stream()
//                    .filter(item -> handle.equals(item.getWindowContext().getNativeBinding().getNativeHandle()))
//                    .findFirst();
//            selected.ifPresent(runner -> log("selectReason=" + reason
//                    + " windowId=" + runner.getWindowContext().getWindowId()
//                    + " handle=" + handle));
//            return selected;
//        }
//
//        private static String normalizeHandle(String handle) {
//            Long parsed = com.bot.dhxy.window.runtime.WindowHandleParser.parseHandle(handle);
//            return parsed == null ? null : Long.toUnsignedString(parsed);
//        }
//    }
//
//    private static void log(String message) {
//        System.out.println("[xiuluo-ctrl-debug] " + message);
//    }
//}
