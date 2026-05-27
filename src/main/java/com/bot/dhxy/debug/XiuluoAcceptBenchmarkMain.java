//package com.bot.dhxy.debug;
//
//
//import com.bot.dhxy.model.ocr.LocationInfo;
//import com.bot.dhxy.core.GameClientTracker;
//import com.bot.dhxy.core.GameContext;
//import com.bot.dhxy.core.TextRecognizer;
//import com.bot.dhxy.driver.BoundWindowKeyboardService;
//import com.bot.dhxy.input.InputProvider;
//import com.bot.dhxy.input.InputSequences;
//import com.bot.dhxy.input.action.InputAction;
//import com.bot.dhxy.model.MapCoordinate;
//import com.bot.dhxy.model.PlayerCharacter;
//import com.bot.dhxy.model.dialog.DialogType;
//import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
//import com.bot.dhxy.model.navigation.NpcNavigationRequest;
//import com.bot.dhxy.model.navigation.ObjectiveTextResult;
//import com.bot.dhxy.model.npc.NpcClickRequest;
//import com.bot.dhxy.model.quest.QuestDetailCapture;
//import com.bot.dhxy.runner.context.TaskExecutionContext;
//import com.bot.dhxy.service.DialogService;
//import com.bot.dhxy.service.NavigationService;
//import com.bot.dhxy.service.NpcClickService;
//import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
//import com.bot.dhxy.vision.LocationVisionService;
//import com.bot.dhxy.service.QuestManagerService;
//import com.bot.dhxy.task.model.TaskType;
//import com.bot.dhxy.tools.CoordinateHelper;
//import com.bot.dhxy.tools.GameStateUtil;
//import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
//import com.bot.dhxy.window.diagnostics.WindowMessageInputExperimentService;
//import com.bot.dhxy.window.execution.MultiWindowTaskManager;
//import com.bot.dhxy.window.execution.WindowTaskRunner;
//import com.bot.dhxy.window.interaction.WindowFocusService;
//import com.bot.dhxy.window.model.WindowNativeBinding;
//import com.bot.dhxy.window.runtime.WindowRuntimeContext;
//import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.WebApplicationType;
//import org.springframework.boot.SpringBootConfiguration;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.builder.SpringApplicationBuilder;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.context.annotation.FilterType;
//
//import java.awt.Point;
//import java.awt.image.BufferedImage;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.atomic.AtomicReference;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardOpenOption;
//
///**
// * Standalone Xiuluo accept-route benchmark.
// *
// * <p>This is intentionally kept under {@code tools/} so route experiments do not pollute the real
// * {@code XiuluoTask}. It copies the production acceptance route shape: inspect any current dialog,
// * navigate to the accept NPC if needed, click the NPC, accept the option, and read the objective
// * from the resulting story dialog or task panel fallback.</p>
// */
//public class XiuluoAcceptBenchmarkMain {
//
//    private static final String START_MAP_NAME = "灵兽村";
//    private static final String ACCEPT_NPC_NAME = "灵兽村使者";
//    private static final int ACCEPT_NPC_X = 112;
//    private static final int ACCEPT_NPC_Y = 93;
//    private static final int BENCHMARK_START_X = 113;
//    private static final int BENCHMARK_START_Y = 76;
//    private static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo_accept_xianlaiwu.png";
//    private static final Path BENCHMARK_LOG = Path.of("logs", "xiuluo-accept-copy.log");
//    private static final DateTimeFormatter TRACE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//    private static final boolean ACCEPT_ONLY =
//            Boolean.parseBoolean(System.getProperty("xiuluo.benchmark.acceptOnly", "true"));
//    private static final int COMBAT_APPROACH_TARGET_X =
//            Integer.getInteger("xiuluo.benchmark.combatTargetX", 116);
//    private static final int COMBAT_APPROACH_TARGET_Y =
//            Integer.getInteger("xiuluo.benchmark.combatTargetY", 71);
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
//            BenchmarkContext benchmark = BenchmarkContext.from(app);
//            AtomicReference<XiuObjective> resultRef = new AtomicReference<>();
//            AtomicReference<Long> routeStartedAtRef = new AtomicReference<>(0L);
//            benchmark.windowHolder.runWith(benchmark.window,
//                    () -> benchmark.gameContext.runWithState(benchmark.window.getGameState(), () -> {
//                        if (Boolean.getBoolean("xiuluo.benchmark.onlyCombatApproach")) {
//                            long startedAt = System.currentTimeMillis();
//                            routeStartedAtRef.set(startedAt);
//                            runCombatApproachProbe(benchmark, COMBAT_APPROACH_TARGET_X,
//                                    COMBAT_APPROACH_TARGET_Y, startedAt);
//                            return;
//                        }
//
//                        long prepositionStartedAt = System.currentTimeMillis();
//                        boolean prepositionOk = moveToBenchmarkStartPoint(benchmark, prepositionStartedAt);
//                        if (!prepositionOk) {
//                            trace("result=null reason=prepositionFailed elapsedMs="
//                                    + (System.currentTimeMillis() - prepositionStartedAt));
//                            return;
//                        }
//
//                        long routeStartedAt = System.currentTimeMillis();
//                        routeStartedAtRef.set(routeStartedAt);
//                        resultRef.set(runCopiedAcceptRoute(benchmark, routeStartedAt));
//                    }));
//
//            long routeStartedAt = routeStartedAtRef.get();
//            long elapsedMs = routeStartedAt > 0 ? System.currentTimeMillis() - routeStartedAt : 0;
//            trace("result=" + resultRef.get() + " elapsedMs=" + elapsedMs);
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
//     * Copy of the Xiuluo objective-acquisition route, kept local to this benchmark.
//     */
//    private static XiuObjective runCopiedAcceptRoute(BenchmarkContext ctx, long startedAt) {
//        if (Boolean.getBoolean("xiuluo.benchmark.onlyWindowMessageAlt1")) {
//            runWindowMessageAlt1Experiment(ctx, startedAt);
//            return null;
//        }
//        if (Boolean.getBoolean("xiuluo.benchmark.onlyMiniMapProbe")) {
//            debugMiniMapClickProbe(ctx, "only-mini-map-probe", startedAt);
//            return null;
//        }
//
//        for (int attempt = 1; attempt <= 2; attempt++) {
//            XiuObjective current = tryObjectiveFromVisibleDialog(ctx, "current-dialog-attempt" + attempt, startedAt);
//            if (current != null) {
//                return current;
//            }
//
//            long navStart = System.currentTimeMillis();
//            boolean navOk = ctx.navigationService.navigateToNPC(NpcNavigationRequest.builder()
//                    .targetMapName(START_MAP_NAME)
//                    .targetX(ACCEPT_NPC_X)
//                    .targetY(ACCEPT_NPC_Y)
//                    .targetName(ACCEPT_NPC_NAME)
//                    .keepTaskTurnUntilHandled(true)
//                    .source("xiuluo-benchmark:navigateToNPC")
//                    .build()).success();
//            trace("step=navigateToAcceptNpc ok=" + navOk
//                    + " elapsedMs=" + (System.currentTimeMillis() - navStart)
//                    + " totalMs=" + (System.currentTimeMillis() - startedAt));
//            if (!navOk) {
//                continue;
//            }
//
//            XiuObjective afterNavigation = clickAcceptOptionFromKnownVisibleDialog(
//                    ctx, "after-navigation-attempt" + attempt, startedAt);
//            if (afterNavigation != null) {
//                return afterNavigation;
//            }
//
//            long clickStart = System.currentTimeMillis();
//            boolean clickOk = clickAcceptNpcAndOpenDialog(ctx);
//            trace("step=clickAcceptNpc ok=" + clickOk
//                    + " elapsedMs=" + (System.currentTimeMillis() - clickStart)
//                    + " totalMs=" + (System.currentTimeMillis() - startedAt));
//            if (!clickOk) {
//                continue;
//            }
//
//            XiuObjective afterNpcClick = tryObjectiveFromVisibleDialog(
//                    ctx, "after-npc-click-attempt" + attempt, startedAt);
//            if (afterNpcClick != null) {
//                return afterNpcClick;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Click the known Xiuluo accept option after NPC navigation has already verified an option dialog.
//     *
//     * <p>This skips the extra {@link DialogService#detectDialogTypeNoFocus(String)} pass that was
//     * previously performed immediately after {@code navigateToNPC}. The navigation
//     * service has already reported the expected NPC option dialog, so the benchmark can go straight to
//     * matching the "闲来无事" template. If the template does not match, the caller falls back to the
//     * older NPC click path.</p>
//     *
//     * @param ctx benchmark runtime context bound to the selected game window.
//     * @param source diagnostic source label for this accept attempt.
//     * @param startedAt route start time in milliseconds, used only for trace output.
//     * @return accepted marker when the template was clicked; null when no accept option was matched.
//     */
//    private static XiuObjective clickAcceptOptionFromKnownVisibleDialog(BenchmarkContext ctx, String source, long startedAt) {
//        long acceptStart = System.currentTimeMillis();
//        String matched = ctx.dialogService.clickFirstKnownOptionGreenTemplateDirectForExclusive(
//                List.of(new GreenTemplateClickSpec(
//                        "ACCEPT_TASK", ACCEPT_OPTION_TEMPLATE, -5, 80, 4)),
//                "xiuluo-copy:accept:" + source);
//        boolean accepted = "ACCEPT_TASK".equals(matched);
//        trace("step=clickAcceptOptionKnownDialog source=" + source
//                + " ok=" + accepted
//                + " matched=" + matched
//                + " elapsedMs=" + (System.currentTimeMillis() - acceptStart)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//        if (!accepted) {
//            return null;
//        }
//        fastClickPostAcceptDialog(ctx, source, startedAt);
//        if (ACCEPT_ONLY) {
//            trace("step=acceptOnlyDone source=" + source
//                    + " totalMs=" + (System.currentTimeMillis() - startedAt));
//            return new XiuObjective("__ACCEPTED__", 0, 0);
//        }
//        sleep(250);
//
//        XiuObjective story = readStoryObjective(ctx, source + ":after-accept", startedAt);
//        if (story != null) {
//            return story;
//        }
//        return readTaskPanelObjective(ctx, source + ":task-panel", startedAt);
//    }
//
//    private static boolean moveToBenchmarkStartPoint(BenchmarkContext ctx, long startedAt) {
//        ctx.gameContext.getMe().setCurrentMapName(START_MAP_NAME);
//        long stepStartedAt = System.currentTimeMillis();
//        boolean ok = ctx.navigationService.navigateInCurrentMap(BENCHMARK_START_X, BENCHMARK_START_Y).success();
//        trace("step=prepositionToStart target=(" + BENCHMARK_START_X + "," + BENCHMARK_START_Y + ")"
//                + " ok=" + ok
//                + " elapsedMs=" + (System.currentTimeMillis() - stepStartedAt)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//        sleep(500);
//        return ok;
//    }
//
//    /**
//     * Invoke the same service used by the JavaFX "鍚庡彴鎸夐敭 Alt+1" button, but from the no-UI
//     * benchmark process. This isolates whether a failure is in the production keyboard worker or in
//     * the raw Win32 PostMessage experiment itself.
//     */
//    private static void runWindowMessageAlt1Experiment(BenchmarkContext ctx, long startedAt) {
//        List<WindowMessageInputExperimentService.WindowMessageInputExperimentResult> results =
//                ctx.windowMessageInputExperimentService.postAlt1(List.of(ctx.runner.snapshot()));
//        for (WindowMessageInputExperimentService.WindowMessageInputExperimentResult result : results) {
//            trace("step=windowMessageAlt1"
//                    + " success=" + result.isPosted()
//                    + " detail=" + result.toDetailMessage()
//                    + " totalMs=" + (System.currentTimeMillis() - startedAt));
//        }
//    }
//
//    /**
//     * Copy the same Alt+1/minimap-click/Alt+1 shape used by current-map navigation, but keep it
//     * entirely inside this standalone runner and write before/after screenshots. This does not touch
//     * the formal Xiuluo route; it only proves whether the target window opens the minimap and where
//     * the calculated point lands.
//     */
//    private static void debugMiniMapClickProbe(BenchmarkContext ctx, String source, long startedAt) {
//        Point pixelPoint = ctx.coordinateHelper.getPhysicalMapPoint(START_MAP_NAME, ACCEPT_NPC_X, ACCEPT_NPC_Y);
//        if (pixelPoint == null) {
//            trace("step=miniMapProbe source=" + source + " ok=false reason=no-map-transform"
//                    + " totalMs=" + (System.currentTimeMillis() - startedAt));
//            return;
//        }
//
//        trace("step=miniMapProbe source=" + source
//                + " targetMap=" + START_MAP_NAME
//                + " target=(" + ACCEPT_NPC_X + "," + ACCEPT_NPC_Y + ")"
//                + " pixel=(" + pixelPoint.x + "," + pixelPoint.y + ")"
//                + " foregroundBefore=" + ctx.focusService.getForegroundNativeHandleText()
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//
//        captureFullWindow(ctx, source + "-before-alt1");
//        long actionStart = System.currentTimeMillis();
//        /*
//         * Keep the probe on the same production path as normal tasks:
//         * pure Alt+1 requests are submitted as keyboard-only queue items, so the input worker can use
//         * the verified HWND-background keyboard path. The actual map click remains a focused real
//         * mouse sequence, because background WM_MOUSE messages were tested and do not affect the game.
//         */
//        boolean opened = submitAlt1ShortcutForProbe(ctx, source + ":open-minimap", 800);
//        captureFullWindow(ctx, source + "-after-alt1-before-click");
//        boolean clicked = opened && ctx.inputSequences.submitAndWait("xiuluo-copy:" + source + ":minimap-move-click", List.of(
//                InputAction.moveMouse(pixelPoint.x, pixelPoint.y),
//                InputAction.sleep(120),
//                InputAction.clickLeft(pixelPoint.x, pixelPoint.y, 500)
//        ));
//        captureFullWindow(ctx, source + "-after-click-before-close");
//        boolean closed = submitAlt1ShortcutForProbe(ctx, source + ":close-minimap", 1200);
//        captureFullWindow(ctx, source + "-after-close");
//        boolean submitted = opened && clicked && closed;
//
//        if (submitted) {
//            ctx.gameStateUtil.recordMovementIntent("xiuluo-copy:" + source, 2500);
//        }
//        GameStateUtil.MovementState movementState = ctx.gameStateUtil.detectMovementState();
//        DialogType dialogType = ctx.dialogService.detectDialogTypeNoFocus(
//                "xiuluo-copy:" + source + ":after-probe");
//        trace("step=miniMapProbeResult source=" + source
//                + " submitted=" + submitted
//                + " movementState=" + movementState
//                + " dialogType=" + dialogType
//                + " foregroundAfter=" + ctx.focusService.getForegroundNativeHandleText()
//                + " elapsedMs=" + (System.currentTimeMillis() - actionStart)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//    }
//
//    /**
//     * Run only the production current-map approach-coordinate path.
//     *
//     * <p>The probe reads the current mini-map location first, derives the same approach coordinate
//     * through {@link CoordinateHelper#calculateApproachCoordinate(String, int, int)}, runs
//     * {@link NavigationService#navigateInCurrentMap(int, int)}, then reads the location again. It
//     * deliberately stops before any NPC/monster click.</p>
//     */
//    private static void runCombatApproachProbe(BenchmarkContext ctx, int targetX, int targetY, long startedAt) {
//        LocationInfo before = ctx.locationVisionService.scanCurrentLocation();
//        if (before == null) {
//            trace("step=combatApproachProbe ok=false reason=location-scan-failed"
//                    + " target=(" + targetX + "," + targetY + ")"
//                    + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
//            return;
//        }
//
//        PlayerCharacter me = ctx.gameContext.getMe();
//        me.setCurrentMapName(before.mapName);
//        me.setX(before.x);
//        me.setY(before.y);
//
//        MapCoordinate approach = ctx.coordinateHelper.calculateApproachCoordinate(before.mapName, targetX, targetY);
//        trace("step=combatApproachProbeStart"
//                + " currentMap=" + before.mapName
//                + " before=(" + before.x + "," + before.y + ")"
//                + " target=(" + targetX + "," + targetY + ")"
//                + " approach=(" + approach.getX() + "," + approach.getY() + ")"
//                + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
//
//        long navStartedAt = System.currentTimeMillis();
//        boolean ok = ctx.navigationService.navigateInCurrentMap(approach.getX(), approach.getY()).success();
//        LocationInfo after = ctx.locationVisionService.scanCurrentLocation();
//        trace("step=combatApproachProbeResult"
//                + " ok=" + ok
//                + " currentMap=" + before.mapName
//                + " target=(" + targetX + "," + targetY + ")"
//                + " approach=(" + approach.getX() + "," + approach.getY() + ")"
//                + " after=" + (after == null ? "null" : "(" + after.x + "," + after.y + ")")
//                + " navElapsedMs=" + (System.currentTimeMillis() - navStartedAt)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//    }
//
//    private static boolean submitAlt1ShortcutForProbe(BenchmarkContext ctx, String source, int afterDelayMs) {
//        long startedAt = System.currentTimeMillis();
//        boolean ok = ctx.inputSequences.submitAndWait("xiuluo-copy:" + source, List.of(
//                InputAction.pressAlt1(),
//                InputAction.sleep(afterDelayMs)
//        ));
//        trace("probeAlt1 source=" + source
//                + " via=inputQueueBackgroundKeyboard"
//                + " ok=" + ok
//                + " elapsedMs=" + (System.currentTimeMillis() - startedAt));
//        return ok;
//    }
//
//    /**
//     * Save a full bound-window screenshot with a stable filename so the probe can be reviewed
//     * without guessing whether Alt+1 opened the map or where the click landed.
//     */
//    private static void captureFullWindow(BenchmarkContext ctx, String label) {
//        WindowNativeBinding binding = ctx.window.getNativeBinding();
//        Path path = Path.of("images", "temp", ctx.window.getWindowId(), "xiuluo_accept_" + label + ".png");
//        boolean ok = ctx.tracker.captureToFile("xiuluo-accept-copy:" + label, path.toString(),
//                binding.getX(), binding.getY(),
//                binding.getX() + binding.getWidth(), binding.getY() + binding.getHeight());
//        trace("capture=" + label + " ok=" + ok + " path=" + path);
//    }
//
//    private static boolean clickAcceptNpcAndOpenDialog(BenchmarkContext ctx) {
//        PlayerCharacter me = ctx.gameContext.getMe();
//        return ctx.npcClickService.clickNpcSmart(NpcClickRequest.fixed(
//                me, START_MAP_NAME, ACCEPT_NPC_X, ACCEPT_NPC_Y,
//                ACCEPT_NPC_NAME, ACCEPT_OPTION_TEMPLATE));
//    }
//
//    private static XiuObjective tryObjectiveFromVisibleDialog(BenchmarkContext ctx, String source, long startedAt) {
//        long detectStart = System.currentTimeMillis();
//        DialogType type = ctx.dialogService.detectDialogTypeNoFocus("xiuluo-copy:" + source);
//        trace("step=dialogPrecheck source=" + source
//                + " type=" + type
//                + " elapsedMs=" + (System.currentTimeMillis() - detectStart)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//        if (type == DialogType.NONE) {
//            return null;
//        }
//
//        if (type == DialogType.STORY) {
//            return readStoryObjective(ctx, source, startedAt);
//        }
//
//        long acceptStart = System.currentTimeMillis();
//        String matched = ctx.dialogService.clickFirstKnownOptionGreenTemplateDirectForExclusive(
//                List.of(new GreenTemplateClickSpec(
//                        "ACCEPT_TASK", ACCEPT_OPTION_TEMPLATE, -5, 80, 4)),
//                "xiuluo-copy:accept:" + source);
//        boolean accepted = "ACCEPT_TASK".equals(matched);
//        trace("step=clickAcceptOption source=" + source
//                + " ok=" + accepted
//                + " matched=" + matched
//                + " elapsedMs=" + (System.currentTimeMillis() - acceptStart)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//        if (!accepted) {
//            return null;
//        }
//        fastClickPostAcceptDialog(ctx, source, startedAt);
//        if (ACCEPT_ONLY) {
//            trace("step=acceptOnlyDone source=" + source
//                    + " totalMs=" + (System.currentTimeMillis() - startedAt));
//            return new XiuObjective("__ACCEPTED__", 0, 0);
//        }
//        sleep(250);
//
//        XiuObjective story = readStoryObjective(ctx, source + ":after-accept", startedAt);
//        if (story != null) {
//            return story;
//        }
//        return readTaskPanelObjective(ctx, source + ":task-panel", startedAt);
//    }
//
//    /**
//     * Click through the story dialog that appears immediately after accepting the Xiuluo task.
//     *
//     * <p>This benchmark intentionally resets the visible dialog state after pressing the accept
//     * option so repeated timing runs start from a similar UI surface. The click is routed through
//     * {@link DialogService#fastClickStoryDialog()}, which uses the serialized input queue and clicks
//     * the lower part of the current story dialog. It is benchmark-only cleanup and must not be treated
//     * as formal Xiuluo task progression.</p>
//     *
//     * @param ctx benchmark runtime context bound to the selected game window.
//     * @param source diagnostic source label for the accept attempt.
//     * @param startedAt route start time in milliseconds, used only for trace output.
//     */
//    private static void fastClickPostAcceptDialog(BenchmarkContext ctx, String source, long startedAt) {
//        long fastClickStart = System.currentTimeMillis();
//        ctx.dialogService.fastClickStoryDialog();
//        trace("step=fastClickPostAcceptDialog source=" + source
//                + " elapsedMs=" + (System.currentTimeMillis() - fastClickStart)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//    }
//
//    private static XiuObjective readStoryObjective(BenchmarkContext ctx, String source, long startedAt) {
//        long storyStart = System.currentTimeMillis();
//        BufferedImage image = ctx.dialogService.captureCurrentStoryImage("xiuluo-copy:" + source);
//        XiuObjective result = recognize(ctx, image, "xiuluo-copy:story:" + source);
//        trace("step=readStoryObjective source=" + source
//                + " result=" + result
//                + " elapsedMs=" + (System.currentTimeMillis() - storyStart)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//        return result;
//    }
//
//    private static XiuObjective readTaskPanelObjective(BenchmarkContext ctx, String source, long startedAt) {
//        long panelStart = System.currentTimeMillis();
//        QuestDetailCapture capture =
//                ctx.questManagerService.captureCurrentQuestDetailForTask("xiuluo");
//        XiuObjective result = recognize(ctx, capture.image(), "xiuluo-copy:task-panel:" + source);
//        trace("step=readTaskPanelObjective source=" + source
//                + " result=" + result
//                + " elapsedMs=" + (System.currentTimeMillis() - panelStart)
//                + " totalMs=" + (System.currentTimeMillis() - startedAt));
//        return result;
//    }
//
//    private static XiuObjective recognize(BenchmarkContext ctx, BufferedImage image, String source) {
//        if (image == null) {
//            return null;
//        }
//        try {
//            Optional<ObjectiveTextResult> parsed =
//                    ctx.objectiveTextRecognitionService.recognize(image, source);
//            return parsed.map(value -> new XiuObjective(value.mapName(), value.x(), value.y())).orElse(null);
//        } finally {
//            image.flush();
//        }
//    }
//
//    private static void sleep(long ms) {
//        try {
//            Thread.sleep(ms);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    private static void trace(String message) {
//        String line = LocalDateTime.now().format(TRACE_TIME_FORMAT) + " [XIULUO_ACCEPT_COPY] " + message;
//        System.out.println(line);
//        try {
//            Files.createDirectories(BENCHMARK_LOG.getParent());
//            Files.writeString(BENCHMARK_LOG, line + System.lineSeparator(),
//                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//        } catch (Exception ignored) {
//            // Console output is still available if the diagnostic file cannot be written.
//        }
//    }
//
//    private record XiuObjective(String mapName, int x, int y) {
//    }
//
//    private record BenchmarkContext(
//            WindowTaskContextHolder windowHolder,
//            WindowTaskRunner runner,
//            WindowRuntimeContext window,
//            GameContext gameContext,
//            NavigationService navigationService,
//            NpcClickService npcClickService,
//            DialogService dialogService,
//            QuestManagerService questManagerService,
//            ObjectiveTextRecognitionService objectiveTextRecognitionService,
//            LocationVisionService locationVisionService,
//            GameClientTracker tracker,
//            InputSequences inputSequences,
//            InputProvider inputProvider,
//            CoordinateHelper coordinateHelper,
//            GameStateUtil gameStateUtil,
//            WindowFocusService focusService,
//            BoundWindowKeyboardService boundKeyboard,
//            WindowMessageInputExperimentService windowMessageInputExperimentService
//    ) {
//        private static BenchmarkContext from(ConfigurableApplicationContext app) {
//            GameWindowRegistrationService registration = app.getBean(GameWindowRegistrationService.class);
//            MultiWindowTaskManager manager = app.getBean(MultiWindowTaskManager.class);
//            WindowFocusService focusService = app.getBean(WindowFocusService.class);
//            WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);
//            GameContext gameContext = app.getBean(GameContext.class);
//            DialogService dialogService = app.getBean(DialogService.class);
//
//            registration.registerDetectedGameWindows(TaskType.XIULUO);
//            String foregroundHandle = focusService.getForegroundNativeHandleText();
//            List<WindowTaskRunner> runners = new ArrayList<>(manager.getAllRunners());
//            trace("registeredWindowCount=" + runners.size()
//                    + " foreground=" + foregroundHandle
//                    + " preferredHandle=" + normalizeHandle(System.getProperty("xiuluo.benchmark.windowHandle")));
//            for (WindowTaskRunner item : runners) {
//                WindowRuntimeContext itemWindow = item.getWindowContext();
//                trace("candidate windowId=" + itemWindow.getWindowId()
//                        + " handle=" + itemWindow.getNativeBinding().getNativeHandle()
//                        + " rect=" + itemWindow.getNativeBinding().getGeometryText()
//                        + " title=" + itemWindow.getNativeBinding().getTitle());
//            }
//            WindowTaskRunner runner = selectRunner(runners, foregroundHandle, windowHolder, gameContext, dialogService)
//                    .orElseThrow(() -> new IllegalStateException(
//                            "No target game window selected; focus the intended game window or pass -Dxiuluo.benchmark.windowHandle=<handle>"));
//            WindowRuntimeContext window = runner.getWindowContext();
//
//            TaskExecutionContext taskContext = TaskExecutionContext.builder()
//                    .taskCode("xiuluo-accept-copy")
//                    .taskName("xiuluo accept copied route")
//                    .requestedTaskCode(TaskType.XIULUO.getCode())
//                    .requestedTaskName(TaskType.XIULUO.getDisplayName())
//                    .windowId(window.getWindowId())
//                    .windowRole(window.getRole().name())
//                    .nativeWindowHandle(window.getNativeBinding().getNativeHandle())
//                    .nativeWindowTitle(window.getNativeBinding().getTitle())
//                    .nativeWindowClassName(window.getNativeBinding().getClassName())
//                    .nativeWindowProcessId(window.getNativeBinding().getProcessId())
//                    .nativeWindowX(window.getNativeBinding().getX())
//                    .nativeWindowY(window.getNativeBinding().getY())
//                    .nativeWindowWidth(window.getNativeBinding().getWidth())
//                    .nativeWindowHeight(window.getNativeBinding().getHeight())
//                    .startedAt(LocalDateTime.now())
//                    .build();
//            trace("foreground=" + foregroundHandle
//                    + " selected=" + window.getNativeBinding().getNativeHandle()
//                    + " title=" + window.getNativeBinding().getTitle()
//                    + " taskContext=" + taskContext.getTaskCode());
//
//            return new BenchmarkContext(
//                    windowHolder,
//                    runner,
//                    window,
//                    gameContext,
//                    app.getBean(NavigationService.class),
//                    app.getBean(NpcClickService.class),
//                    dialogService,
//                    app.getBean(QuestManagerService.class),
//                    app.getBean(ObjectiveTextRecognitionService.class),
//                    app.getBean(LocationVisionService.class),
//                    app.getBean(GameClientTracker.class),
//                    app.getBean(InputSequences.class),
//                    app.getBean(InputProvider.class),
//                    app.getBean(CoordinateHelper.class),
//                    app.getBean(GameStateUtil.class),
//                    focusService,
//                    app.getBean(BoundWindowKeyboardService.class),
//                    app.getBean(WindowMessageInputExperimentService.class)
//            );
//        }
//
//        private static Optional<WindowTaskRunner> selectRunner(List<WindowTaskRunner> runners,
//                                                              String foregroundHandle,
//                                                              WindowTaskContextHolder windowHolder,
//                                                              GameContext gameContext,
//                                                              DialogService dialogService) {
//            if (runners == null || runners.isEmpty()) {
//                return Optional.empty();
//            }
//
//            Optional<WindowTaskRunner> preferred = selectByHandle(
//                    runners, normalizeHandle(System.getProperty("xiuluo.benchmark.windowHandle")), "preferredHandle");
//            if (preferred.isPresent()) {
//                return preferred;
//            }
//
//            Optional<WindowTaskRunner> foreground = selectByHandle(runners, normalizeHandle(foregroundHandle), "foreground");
//            if (foreground.isPresent()) {
//                return foreground;
//            }
//
//            List<DialogProbe> dialogProbes = new ArrayList<>();
//            for (WindowTaskRunner runner : runners) {
//                WindowRuntimeContext probeWindow = runner.getWindowContext();
//                try {
//                    AtomicReference<DialogType> typeRef =
//                            new AtomicReference<>(DialogType.NONE);
//                    windowHolder.runWith(probeWindow,
//                            () -> gameContext.runWithState(probeWindow.getGameState(),
//                                    () -> typeRef.set(dialogService.detectDialogTypeNoFocus(
//                                            "xiuluo-copy:window-select:" + probeWindow.getWindowId()))));
//                    DialogType type = typeRef.get();
//                    dialogProbes.add(new DialogProbe(runner, type));
//                    trace("candidateDialog windowId=" + probeWindow.getWindowId() + " type=" + type);
//                } catch (Exception e) {
//                    trace("candidateDialog windowId=" + probeWindow.getWindowId()
//                            + " error=" + e.getClass().getSimpleName() + ":" + e.getMessage());
//                }
//            }
//            Optional<WindowTaskRunner> dialogWindow = dialogProbes.stream()
//                    .filter(probe -> probe.type() != DialogType.NONE)
//                    .map(DialogProbe::runner)
//                    .findFirst();
//            if (dialogWindow.isPresent()) {
//                trace("selectReason=currentDialog windowId=" + dialogWindow.get().getWindowContext().getWindowId());
//                return dialogWindow;
//            }
//
//            if (runners.size() == 1) {
//                trace("selectReason=singleRegisteredWindow windowId=" + runners.get(0).getWindowContext().getWindowId());
//                return Optional.of(runners.get(0));
//            }
//
//            trace("selectReason=none multipleWindowsNoForegroundNoDialog");
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
//            selected.ifPresent(runner -> trace("selectReason=" + reason
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
//    private record DialogProbe(WindowTaskRunner runner, DialogType type) {
//    }
//}
