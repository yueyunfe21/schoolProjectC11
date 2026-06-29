package com.bot.dhxy.debug;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.OpenCvNativeLoader;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.startup.TaskStartupWindowPreparationService;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Manual no-UI probe for the startup Alt+5/Alt+6 status overlays.
 *
 * <p>This debug entry registers existing game windows, binds one selected window, sends only the
 * explicit Alt+5 shortcut through {@link BoundWindowKeyboardService}, then runs the production Alt+6
 * visibility check. It is intentionally not wired into JavaFX or normal task startup, so it cannot
 * make production tasks press Alt+5 automatically.</p>
 */
public class StartupAlt5ShoppingProbeDebugMain {

    private static final String SHOPPING_TEMPLATE = "images/template/status/blacklist_shopping.png";
    private static final int ALT5_SHOPPING_RECT_X_OFFSET = 359;
    private static final int ALT5_SHOPPING_RECT_Y_OFFSET = 271;
    private static final int ALT5_SHOPPING_RECT_WIDTH = 317;
    private static final int ALT5_SHOPPING_RECT_HEIGHT = 288;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void main(String[] args) {
        ConfigurableApplicationContext app = new SpringApplicationBuilder(ToolSpringConfig.class)
                .headless(false)
                .web(WebApplicationType.NONE)
                .properties(
                        "bot.run.show-ui=false",
                        "bot.run.auto-start=false",
                        "bot.run.init-game-window=false"
                )
                .run(args);

        try {
            ProbeResult result = run(app);
            System.out.println("alt5ShoppingProbeSelectedWindow=" + result.selectedWindow());
            System.out.println("alt5ShoppingProbeShortcut attempted=" + result.alt5Attempt().attempted()
                    + " success=" + result.alt5Attempt().success()
                    + " reason=" + result.alt5Attempt().reason());
            System.out.println("alt6CrowdProbeShortcut attempted=" + result.alt6Attempt().attempted()
                    + " success=" + result.alt6Attempt().success()
                    + " reason=" + result.alt6Attempt().reason());
            System.out.println("alt5ShoppingProbeRect=" + result.rectText());
            printMatch("alt5ShoppingProbeBefore", result.before());
            printMatch("alt5ShoppingProbeAfter", result.afterAlt5());
            System.out.println("alt6VisibilityExistingLogic=" + result.alt6VisibilityConfirmed());
            System.out.println("alt5ShoppingProbeAfterMarked=" + result.afterAlt5().markedPath());
        } finally {
            app.close();
            System.exit(0);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = "com.bot.dhxy",
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bot\\.dhxy\\.AutoBot"),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bot\\.dhxy\\.ui\\..*"),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CommandLineRunner.class)
            }
    )
    static class ToolSpringConfig {
    }

    private static ProbeResult run(ConfigurableApplicationContext app) {
        GameWindowRegistrationService registrationService = app.getBean(GameWindowRegistrationService.class);
        MultiWindowTaskManager taskManager = app.getBean(MultiWindowTaskManager.class);
        WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);
        WindowFocusService focusService = app.getBean(WindowFocusService.class);
        GameClientTracker tracker = app.getBean(GameClientTracker.class);
        CoordinateHelper coordinateHelper = app.getBean(CoordinateHelper.class);
        BoundWindowKeyboardService keyboardService = app.getBean(BoundWindowKeyboardService.class);
        TaskStartupWindowPreparationService startupPreparationService =
                app.getBean(TaskStartupWindowPreparationService.class);

        registrationService.registerDetectedGameWindows(TaskType.DEBUG_COORDINATE);
        WindowTaskSnapshot selectedSnapshot = selectWindow(taskManager.getAllSnapshots(), focusService);
        WindowRuntimeContext window = taskManager.getRunner(selectedSnapshot.getWindowId())
                .map(WindowTaskRunner::getWindowContext)
                .orElseThrow(() -> new IllegalStateException("runner not found: " + selectedSnapshot.getWindowId()));

        String selectedWindow = selectedSnapshot.getWindowId()
                + " title=" + selectedSnapshot.getNativeTitle()
                + " hwnd=" + selectedSnapshot.getNativeHandle()
                + " geometry=" + selectedSnapshot.getGeometryText();

        return windowHolder.callWith(window, () -> {
            int[] rect = coordinateHelper.getScaledRect(
                    ALT5_SHOPPING_RECT_X_OFFSET,
                    ALT5_SHOPPING_RECT_Y_OFFSET,
                    ALT5_SHOPPING_RECT_WIDTH,
                    ALT5_SHOPPING_RECT_HEIGHT);
            Path outputDir = Path.of("images", "temp", "alt5_shopping_probe",
                    LocalDateTime.now().format(FILE_TIME) + "_" + selectedSnapshot.getWindowId());
            createDirectories(outputDir);

            MatchOutput before = captureAndMatchShopping(tracker, rect, outputDir, "before_alt5");
            BoundWindowKeyboardService.ShortcutAttempt alt5Attempt =
                    keyboardService.pressShortcut(BoundWindowKeyboardService.AltShortcut.ALT_5);
            long waitMs = Long.getLong("alt5.debug.afterDelayMs", 650L);
            TaskSleep.sleep(waitMs);
            MatchOutput afterAlt5 = captureAndMatchShopping(tracker, rect, outputDir, "after_alt5_" + waitMs + "ms");
            boolean alt6VisibilityConfirmed = startupPreparationService.ensureAlt6Visibility();

            return new ProbeResult(
                    selectedWindow,
                    AttemptSummary.from(alt5Attempt),
                    new AttemptSummary(true, alt6VisibilityConfirmed, alt6VisibilityConfirmed ? "OK" : "not-confirmed"),
                    rect[0] + "," + rect[1] + " -> " + rect[2] + "," + rect[3],
                    before,
                    afterAlt5,
                    alt6VisibilityConfirmed);
        });
    }

    private static WindowTaskSnapshot selectWindow(List<WindowTaskSnapshot> snapshots,
                                                   WindowFocusService focusService) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalStateException("no registered game window");
        }

        String explicitWindowId = System.getProperty("alt5.debug.windowId", "").trim();
        String titleContains = System.getProperty("alt5.debug.windowTitleContains", "67555").trim();
        String foregroundHandle = focusService.getForegroundNativeHandleText();
        Long foreground = WindowHandleParser.parseHandle(foregroundHandle);

        return snapshots.stream()
                .filter(snapshot -> explicitWindowId.isBlank()
                        || explicitWindowId.equals(snapshot.getWindowId()))
                .filter(snapshot -> titleContains.isBlank()
                        || (snapshot.getNativeTitle() != null && snapshot.getNativeTitle().contains(titleContains)))
                .filter(snapshot -> foreground == null
                        || Objects.equals(foreground, WindowHandleParser.parseHandle(snapshot.getNativeHandle())))
                .findFirst()
                .or(() -> snapshots.stream()
                        .filter(snapshot -> explicitWindowId.isBlank()
                                || explicitWindowId.equals(snapshot.getWindowId()))
                        .filter(snapshot -> titleContains.isBlank()
                                || (snapshot.getNativeTitle() != null && snapshot.getNativeTitle().contains(titleContains)))
                        .findFirst())
                .orElseGet(() -> snapshots.stream()
                        .sorted(Comparator.comparing(WindowTaskSnapshot::getWindowId,
                                Comparator.nullsLast(String::compareTo)))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("no registered game window")));
    }

    private static MatchOutput captureAndMatchShopping(GameClientTracker tracker, int[] rect, Path outputDir, String label) {
        Path rawPath = outputDir.resolve(label + "_raw.png");
        Path markedPath = outputDir.resolve(label + "_marked.png");
        boolean captured = tracker.captureToFile("alt5 shopping probe " + label,
                rawPath.toString(), rect[0], rect[1], rect[2], rect[3]);
        if (!captured) {
            return new MatchOutput(false, -1.0, "-", rawPath.toString(), markedPath.toString());
        }
        return matchTemplate(rawPath, markedPath, SHOPPING_TEMPLATE);
    }

    private static MatchOutput matchTemplate(Path rawPath, Path markedPath, String templatePath) {
        OpenCvNativeLoader.ensureLoaded();
        Mat source = Imgcodecs.imread(rawPath.toString(), Imgcodecs.IMREAD_COLOR);
        Mat template = Imgcodecs.imread(templatePath, Imgcodecs.IMREAD_COLOR);
        if (source.empty() || template.empty()) {
            source.release();
            template.release();
            return new MatchOutput(false, -1.0, "-", rawPath.toString(), markedPath.toString());
        }
        if (source.width() < template.width() || source.height() < template.height()) {
            Imgcodecs.imwrite(markedPath.toString(), source);
            source.release();
            template.release();
            return new MatchOutput(true, -1.0, "template-larger-than-source", rawPath.toString(), markedPath.toString());
        }

        Mat result = new Mat();
        Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult minMax = Core.minMaxLoc(result);
        Point topLeft = minMax.maxLoc;
        Point bottomRight = new Point(topLeft.x + template.width(), topLeft.y + template.height());
        Imgproc.rectangle(source, new Rect(topLeft, bottomRight), new Scalar(0, 0, 255), 2);
        Imgcodecs.imwrite(markedPath.toString(), source);

        double score = minMax.maxVal;
        String pointText = Math.round(topLeft.x) + "," + Math.round(topLeft.y);
        result.release();
        source.release();
        template.release();
        return new MatchOutput(true, score, pointText, rawPath.toString(), markedPath.toString());
    }

    private static void printMatch(String prefix, MatchOutput match) {
        System.out.println(prefix + " score=" + match.score()
                + " point=" + match.pointText()
                + " raw=" + match.rawPath()
                + " marked=" + match.markedPath());
    }

    private static void createDirectories(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create output dir: " + outputDir, e);
        }
    }

    private record ProbeResult(String selectedWindow,
                               AttemptSummary alt5Attempt,
                               AttemptSummary alt6Attempt,
                               String rectText,
                               MatchOutput before,
                               MatchOutput afterAlt5,
                               boolean alt6VisibilityConfirmed) {
    }

    private record AttemptSummary(boolean attempted,
                                  boolean success,
                                  String reason) {

        private static AttemptSummary from(BoundWindowKeyboardService.ShortcutAttempt attempt) {
            return new AttemptSummary(attempt.attempted(), attempt.success(), attempt.reason());
        }
    }

    private record MatchOutput(boolean captured,
                               double score,
                               String pointText,
                               String rawPath,
                               String markedPath) {
    }
}
