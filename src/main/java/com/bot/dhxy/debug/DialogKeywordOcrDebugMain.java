package com.bot.dhxy.debug;


import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bot.dhxy.config.TeleportConfig.MAP_ALIASES;

/**
 * Captures the current option-dialog rectangle and checks whether OCR can match a target keyword.
 *
 * <p>This diagnostic entry point intentionally sends no keyboard or mouse input. It binds the
 * foreground registered game window, captures the same dialog rectangle used by
 * {@code DialogService}, runs the production OCR matching route, and prints the matched alias and
 * OCR boxes. Use it when validating {@link com.bot.dhxy.config.TeleportConfig} aliases against a
 * dialog that is already open in the game client.</p>
 */
public class DialogKeywordOcrDebugMain {

    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;
    private static final DateTimeFormatter STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String DEFAULT_TARGET_KEYWORD = "\u7075\u517d\u6751";

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
            DebugContext debug = DebugContext.from(app);
            debug.windowHolder.runWith(debug.window,
                    () -> debug.gameContext.runWithState(debug.window.getGameState(),
                            () -> runOcrProbe(debug)));
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

    private static void runOcrProbe(DebugContext debug) {
        String targetKeyword = System.getProperty("dialog.keyword.target", DEFAULT_TARGET_KEYWORD);
        List<String> aliases = MAP_ALIASES.getOrDefault(targetKeyword, Collections.singletonList(targetKeyword));
        int[] rect = debug.coordinateHelper.getScaledRect(DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
        Path outputDir = Path.of("images", "temp", "dialog_keyword_debug").toAbsolutePath().normalize();
        try {
            Files.createDirectories(outputDir);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create debug output directory: " + outputDir, e);
        }

        String windowId = debug.window.getWindowId().replaceAll("[^A-Za-z0-9_.-]", "_");
        String stamp = LocalDateTime.now().format(STAMP_FORMAT);
        Path rawPath = outputDir.resolve("dialog_" + windowId + "_" + stamp + "_raw.png");
        Path washedPath = outputDir.resolve("dialog_" + windowId + "_" + stamp + "_green_washed.png");

        boolean captured = debug.tracker.captureToFile("dialog-keyword-debug", rawPath.toString(),
                rect[0], rect[1], rect[2], rect[3]);
        log("selectedWindowId=" + debug.window.getWindowId()
                + " handle=" + debug.window.getNativeBinding().getNativeHandle()
                + " title=" + debug.window.getNativeBinding().getTitle());
        log("target=" + targetKeyword + " aliases=" + aliases);
        log("rect=(" + rect[0] + "," + rect[1] + ")-(" + rect[2] + "," + rect[3] + ")"
                + " captured=" + captured + " rawPath=" + rawPath);
        if (!captured) {
            return;
        }

        runOcrForImage(debug, targetKeyword, aliases, rawPath, "raw");
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath.toString(), washedPath.toString());
        log("washedPath=" + washedPath);
        runOcrForImage(debug, targetKeyword, aliases, washedPath, "green-washed");
    }

    private static void runOcrForImage(DebugContext debug,
                                       String targetKeyword,
                                       List<String> aliases,
                                       Path imagePath,
                                       String label) {
        long startedAt = System.currentTimeMillis();
        List<OcrWordResult> words = debug.ocr.getAllTextResultsForMatch(
                imagePath.toString(),
                "dialog-keyword-debug:" + label + ":" + targetKeyword,
                found -> hasAnyKeyword(found, aliases));
        long elapsedMs = System.currentTimeMillis() - startedAt;
        MatchResult match = findFirstMatch(words, aliases);
        log("label=" + label
                + " ocrElapsedMs=" + elapsedMs
                + " wordCount=" + words.size()
                + " matched=" + match.matched()
                + " alias=" + nullToDash(match.alias())
                + " text=" + nullToDash(match.text()));
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            log("word text=" + nullToDash(word.getText())
                    + " center=(" + word.getX() + "," + word.getY() + ")"
                    + " box=(" + word.getLeft() + "," + word.getTop() + ","
                    + word.getWidth() + "x" + word.getHeight() + ")"
                    + " score=" + word.getScore());
        }
    }

    private static boolean hasAnyKeyword(List<OcrWordResult> words, List<String> aliases) {
        return findFirstMatch(words, aliases).matched();
    }

    private static MatchResult findFirstMatch(List<OcrWordResult> words, List<String> aliases) {
        if (words == null || aliases == null) {
            return MatchResult.none();
        }
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null) {
                continue;
            }
            for (String alias : aliases) {
                if (alias != null && !alias.isBlank() && word.getText().contains(alias)) {
                    return new MatchResult(true, alias, word.getText());
                }
            }
        }
        return MatchResult.none();
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record MatchResult(boolean matched, String alias, String text) {
        private static MatchResult none() {
            return new MatchResult(false, null, null);
        }
    }

    private record DebugContext(
            WindowTaskContextHolder windowHolder,
            WindowRuntimeContext window,
            GameContext gameContext,
            GameClientTracker tracker,
            CoordinateHelper coordinateHelper,
            TextRecognizer ocr
    ) {
        private static DebugContext from(ConfigurableApplicationContext app) {
            GameWindowRegistrationService registration = app.getBean(GameWindowRegistrationService.class);
            MultiWindowTaskManager manager = app.getBean(MultiWindowTaskManager.class);
            WindowFocusService focusService = app.getBean(WindowFocusService.class);
            WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);

            registration.registerDetectedGameWindows(TaskType.XIULUO);
            List<WindowTaskRunner> runners = new ArrayList<>(manager.getAllRunners());
            String preferredHandle = normalizeHandle(System.getProperty("dialog.keyword.windowHandle"));
            String foregroundHandle = normalizeHandle(focusService.getForegroundNativeHandleText());

            log("registeredWindowCount=" + runners.size()
                    + " preferredHandle=" + nullToDash(preferredHandle)
                    + " foregroundHandle=" + nullToDash(foregroundHandle));
            for (WindowTaskRunner runner : runners) {
                WindowRuntimeContext item = runner.getWindowContext();
                log("candidate windowId=" + item.getWindowId()
                        + " handle=" + item.getNativeBinding().getNativeHandle()
                        + " rect=" + item.getNativeBinding().getGeometryText()
                        + " title=" + item.getNativeBinding().getTitle());
            }

            WindowTaskRunner selected = selectRunner(runners, preferredHandle, foregroundHandle)
                    .orElseThrow(() -> new IllegalStateException(
                            "No game window selected; focus one DHXY client or pass -Ddialog.keyword.windowHandle=<handle>"));
            return new DebugContext(
                    windowHolder,
                    selected.getWindowContext(),
                    app.getBean(GameContext.class),
                    app.getBean(GameClientTracker.class),
                    app.getBean(CoordinateHelper.class),
                    app.getBean(TextRecognizer.class)
            );
        }

        private static Optional<WindowTaskRunner> selectRunner(List<WindowTaskRunner> runners,
                                                              String preferredHandle,
                                                              String foregroundHandle) {
            Optional<WindowTaskRunner> preferred = selectByHandle(runners, preferredHandle, "preferredHandle");
            if (preferred.isPresent()) {
                return preferred;
            }
            Optional<WindowTaskRunner> foreground = selectByHandle(runners, foregroundHandle, "foreground");
            if (foreground.isPresent()) {
                return foreground;
            }
            if (runners.size() == 1) {
                log("selectReason=singleRegisteredWindow");
                return Optional.of(runners.get(0));
            }
            return Optional.empty();
        }

        private static Optional<WindowTaskRunner> selectByHandle(List<WindowTaskRunner> runners,
                                                                String handle,
                                                                String reason) {
            if (handle == null || handle.isBlank()) {
                return Optional.empty();
            }
            Optional<WindowTaskRunner> selected = runners.stream()
                    .filter(item -> handle.equals(item.getWindowContext().getNativeBinding().getNativeHandle()))
                    .findFirst();
            selected.ifPresent(runner -> log("selectReason=" + reason
                    + " windowId=" + runner.getWindowContext().getWindowId()
                    + " handle=" + handle));
            return selected;
        }

        private static String normalizeHandle(String handle) {
            Long parsed = WindowHandleParser.parseHandle(handle);
            return parsed == null ? null : Long.toUnsignedString(parsed);
        }
    }

    private static void log(String message) {
        System.out.println("[dialog-keyword-debug] " + message);
    }
}
