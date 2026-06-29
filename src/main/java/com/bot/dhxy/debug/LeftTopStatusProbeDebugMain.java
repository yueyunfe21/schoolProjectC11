package com.bot.dhxy.debug;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.OpenCvNativeLoader;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.LeftTopStatusSwitchService;
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
import org.opencv.core.Core;
import org.opencv.core.Mat;
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
 * Manual no-UI live probe for the CR107 left-top status switch.
 *
 * <p>The probe binds one detected game HWND by title/window filters, captures the narrow
 * window-relative ROI, writes raw/marked images, and prints open/closed scores. It never clicks by
 * default; click testing requires {@code -DleftTopStatus.debug.click=true} from an elevated CLI when
 * the game client is elevated too.</p>
 */
public class LeftTopStatusProbeDebugMain {

    private static final int LEFT_TOP_STATUS_RECT_X_OFFSET = 8;
    private static final int LEFT_TOP_STATUS_RECT_Y_OFFSET = 147;
    private static final int LEFT_TOP_STATUS_RECT_WIDTH = 11;
    private static final int LEFT_TOP_STATUS_RECT_HEIGHT = 19;
    private static final double LEFT_TOP_STATUS_MATCH_RATE = 0.90;
    private static final double LEFT_TOP_STATUS_MATCH_MARGIN = 0.02;
    private static final int MARKED_SCALE = 10;
    private static final String OUTPUT_ROOT = "images/temp/left_top_status_probe";
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
            System.out.println("leftTopStatusSelectedWindow=" + result.selectedWindow());
            System.out.println("leftTopStatusRect=" + result.rectText());
            System.out.println("leftTopStatusRaw=" + result.rawPath());
            System.out.println("leftTopStatusMarked=" + result.markedPath());
            System.out.println("leftTopStatusProbe captured=" + result.captured()
                    + " state=" + result.state()
                    + " openScore=" + result.openScore()
                    + " closedScore=" + result.closedScore()
                    + " openCenter=" + result.openCenterText());
            System.out.println("leftTopStatusDebugClick enabled=" + result.clickEnabled()
                    + " attempted=" + result.clickAttempted()
                    + " clicked=" + result.clicked());
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
        InputSequences inputSequences = app.getBean(InputSequences.class);

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
                    LEFT_TOP_STATUS_RECT_X_OFFSET,
                    LEFT_TOP_STATUS_RECT_Y_OFFSET,
                    LEFT_TOP_STATUS_RECT_WIDTH,
                    LEFT_TOP_STATUS_RECT_HEIGHT);
            Path outputDir = Path.of(OUTPUT_ROOT,
                    LocalDateTime.now().format(FILE_TIME) + "_" + selectedSnapshot.getWindowId());
            createDirectories(outputDir);

            Path rawPath = outputDir.resolve("left_top_status_raw.png");
            Path markedPath = outputDir.resolve("left_top_status_marked.png");
            boolean captured = tracker.captureToFile("left top status probe",
                    rawPath.toString(), rect[0], rect[1], rect[2], rect[3]);
            if (!captured) {
                return new ProbeResult(selectedWindow, rectText(rect), false, "CAPTURE_FAILED",
                        -1.0, -1.0, "-", rawPath.toString(), markedPath.toString(),
                        Boolean.getBoolean("leftTopStatus.debug.click"), false, false);
            }

            TemplateMatch open = match(rawPath, LeftTopStatusSwitchService.LEFT_TOP_OPEN_TEMPLATE);
            TemplateMatch closed = match(rawPath, LeftTopStatusSwitchService.LEFT_TOP_CLOSED_TEMPLATE);
            String state = resolveState(open.score(), closed.score());
            writeMarked(rawPath, markedPath, open, closed, state);

            boolean clickEnabled = Boolean.getBoolean("leftTopStatus.debug.click");
            boolean clickAttempted = clickEnabled && "OPEN".equals(state) && open.hasPoint();
            boolean clicked = false;
            if (clickAttempted) {
                int clickX = rect[0] + (int) Math.round(open.centerX());
                int clickY = rect[1] + (int) Math.round(open.centerY());
                clicked = inputSequences.moveAndClickLeft("leftTopStatusProbe:debugClick", clickX, clickY, 120, 250);
            }
            String openCenter = open.hasPoint()
                    ? (rect[0] + (int) Math.round(open.centerX())) + "," + (rect[1] + (int) Math.round(open.centerY()))
                    : "-";
            return new ProbeResult(selectedWindow, rectText(rect), true, state,
                    open.score(), closed.score(), openCenter, rawPath.toString(), markedPath.toString(),
                    clickEnabled, clickAttempted, clicked);
        });
    }

    private static WindowTaskSnapshot selectWindow(List<WindowTaskSnapshot> snapshots,
                                                   WindowFocusService focusService) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalStateException("no registered game window");
        }

        String explicitWindowId = System.getProperty("leftTopStatus.debug.windowId", "").trim();
        String titleContains = System.getProperty("leftTopStatus.debug.windowTitleContains", "67555").trim();
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

    private static TemplateMatch match(Path rawPath, String templatePath) {
        OpenCvNativeLoader.ensureLoaded();
        Mat source = Imgcodecs.imread(rawPath.toString(), Imgcodecs.IMREAD_COLOR);
        Mat template = Imgcodecs.imread(templatePath, Imgcodecs.IMREAD_COLOR);
        try {
            if (source.empty() || template.empty()
                    || source.width() < template.width()
                    || source.height() < template.height()) {
                return TemplateMatch.miss();
            }
            Mat result = new Mat();
            try {
                Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED);
                Core.MinMaxLocResult minMax = Core.minMaxLoc(result);
                return new TemplateMatch(minMax.maxVal,
                        minMax.maxLoc.x,
                        minMax.maxLoc.y,
                        template.width(),
                        template.height());
            } finally {
                result.release();
            }
        } finally {
            source.release();
            template.release();
        }
    }

    private static String resolveState(double openScore, double closedScore) {
        if (openScore >= LEFT_TOP_STATUS_MATCH_RATE
                && openScore >= closedScore + LEFT_TOP_STATUS_MATCH_MARGIN) {
            return "OPEN";
        }
        if (closedScore >= LEFT_TOP_STATUS_MATCH_RATE
                && closedScore > openScore) {
            return "CLOSED";
        }
        return "UNKNOWN";
    }

    private static void writeMarked(Path rawPath,
                                    Path markedPath,
                                    TemplateMatch open,
                                    TemplateMatch closed,
                                    String state) {
        Mat source = Imgcodecs.imread(rawPath.toString(), Imgcodecs.IMREAD_COLOR);
        if (source.empty()) {
            return;
        }
        Mat enlarged = new Mat();
        try {
            Imgproc.resize(source, enlarged,
                    new org.opencv.core.Size(source.width() * MARKED_SCALE, source.height() * MARKED_SCALE),
                    0, 0, Imgproc.INTER_NEAREST);
            TemplateMatch chosen = "OPEN".equals(state) ? open : closed;
            if (chosen.hasPoint()) {
                org.opencv.core.Point topLeft = new org.opencv.core.Point(
                        chosen.x() * MARKED_SCALE,
                        chosen.y() * MARKED_SCALE);
                org.opencv.core.Point bottomRight = new org.opencv.core.Point(
                        (chosen.x() + chosen.width()) * MARKED_SCALE,
                        (chosen.y() + chosen.height()) * MARKED_SCALE);
                Scalar color = "OPEN".equals(state) ? new Scalar(0, 0, 255) : new Scalar(255, 0, 0);
                Imgproc.rectangle(enlarged, new Rect(topLeft, bottomRight), color, 2);
                org.opencv.core.Point center = new org.opencv.core.Point(
                        (chosen.x() + chosen.width() / 2.0) * MARKED_SCALE,
                        (chosen.y() + chosen.height() / 2.0) * MARKED_SCALE);
                Imgproc.circle(enlarged, center, 4, color, -1);
            }
            Imgcodecs.imwrite(markedPath.toString(), enlarged);
        } finally {
            enlarged.release();
            source.release();
        }
    }

    private static String rectText(int[] rect) {
        return rect[0] + "," + rect[1] + " -> " + rect[2] + "," + rect[3];
    }

    private static void createDirectories(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create output dir: " + outputDir, e);
        }
    }

    private record ProbeResult(String selectedWindow,
                               String rectText,
                               boolean captured,
                               String state,
                               double openScore,
                               double closedScore,
                               String openCenterText,
                               String rawPath,
                               String markedPath,
                               boolean clickEnabled,
                               boolean clickAttempted,
                               boolean clicked) {
    }

    private record TemplateMatch(double score,
                                 double x,
                                 double y,
                                 int width,
                                 int height) {

        static TemplateMatch miss() {
            return new TemplateMatch(-1.0, -1.0, -1.0, 0, 0);
        }

        boolean hasPoint() {
            return x >= 0 && y >= 0 && width > 0 && height > 0;
        }

        double centerX() {
            return x + width / 2.0;
        }

        double centerY() {
            return y + height / 2.0;
        }
    }
}
