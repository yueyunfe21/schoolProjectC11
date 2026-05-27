package com.bot.dhxy.debug;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.vision.GameTextLineOcrService;
import com.bot.dhxy.vision.OcrRoiMemoryService;
import com.bot.dhxy.vision.OcrWindowScanService;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.ocr.TextCandidate;
import com.bot.dhxy.model.ocr.TextCandidateScanResult;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Captures the selected game window content area and runs yellow text candidate detection.
 *
 * <p>This debug main does not capture the desktop, does not send keyboard/mouse input, and does not
 * start the JavaFX UI. It registers visible game windows, selects an explicit native handle when
 * {@code -Dnpc.text.windowHandle=<handle>} is provided, otherwise prefers the foreground registered
 * game window, asks {@link OcrRoiMemoryService} for learned/seed OCR regions, then captures each
 * recommended crop through {@link GameClientTracker}. Explicit
 * {@code -Dnpc.text.region=x1,y1,x2,y2} remains available when a human wants to bypass memory for
 * one run.</p>
 */
public class NpcTextCandidateGameWindowDebugMain {

    private static final int GAME_WIDTH = 1024;
    private static final int GAME_HEIGHT = 768;
    private static final OcrWindowRegion XIULUO_TARGET_PRIMARY_REGION = new OcrWindowRegion(520, 160, 1024, 680);
    private static final OcrWindowRegion XIULUO_TARGET_FALLBACK_REGION = new OcrWindowRegion(258, 120, 1024, 700);
    private static final DateTimeFormatter STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String DEFAULT_MAP_NAME = "\u7075\u517d\u6751";
    private static final String DEFAULT_TARGET_NAME = "\u4fee\u7f57";
    private static final int DEFAULT_TARGET_X = 112;
    private static final int DEFAULT_TARGET_Y = 93;

    /**
     * Capture one selected game window and print top yellow text candidates.
     *
     * @param args optional first argument is output directory. When omitted, files are written under
     * {@code images/temp/npc_text_candidate_game}.
     */
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
            Path outputDir = args.length >= 1 && args[0] != null && !args[0].isBlank()
                    ? Path.of(args[0]).toAbsolutePath().normalize()
                    : Path.of("images/temp/npc_text_candidate_game").toAbsolutePath().normalize();
            DebugContext debug = DebugContext.from(app);
            debug.windowHolder.runWith(debug.window,
                    () -> debug.gameContext.runWithState(debug.window.getGameState(),
                            () -> runCapture(debug, outputDir)));
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

    private static void runCapture(DebugContext debug, Path outputDir) {
        try {
            Files.createDirectories(outputDir);
            String stamp = LocalDateTime.now().format(STAMP_FORMAT);
            String windowId = debug.window.getWindowId().replaceAll("[^A-Za-z0-9_.-]", "_");

            DebugTarget target = DebugTarget.fromProperties();
            List<OcrWindowRegion> regions = selectedRegions(debug, target);
            log("selectedWindowId=" + debug.window.getWindowId()
                    + " handle=" + debug.window.getNativeBinding().getNativeHandle()
                    + " title=" + debug.window.getNativeBinding().getTitle());
            log("target map=" + target.mapName()
                    + " name=" + target.targetName()
                    + " coord=(" + target.targetX() + "," + target.targetY() + ")"
                    + " roaming=" + target.roaming());
            log("recommendedRegionCount=" + regions.size());

            for (int i = 0; i < regions.size(); i++) {
                OcrWindowRegion region = regions.get(i);
                String prefix = "game_" + windowId + "_" + stamp + "_r" + (i + 1);
                Path rawPath = outputDir.resolve(prefix + "_raw.png");
                Path washedPath = outputDir.resolve(prefix + "_yellow_washed.png");
                Path overlayPath = outputDir.resolve(prefix + "_candidates.png");
                runRegionCapture(debug, region, rawPath, washedPath, overlayPath, i + 1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("NPC text candidate game-window debug failed", e);
        }
    }

    private static void runRegionCapture(DebugContext debug,
                                         OcrWindowRegion region,
                                         Path rawPath,
                                         Path washedPath,
                                         Path overlayPath,
                                         int index) throws Exception {
        int baseX = debug.window.getNativeBinding().getX();
        int baseY = debug.window.getNativeBinding().getY();
        BufferedImage raw = debug.tracker.captureToMemory(
                "npc-text-candidate-game-window-r" + index,
                baseX + region.x1(), baseY + region.y1(), baseX + region.x2(), baseY + region.y2());
        if (raw == null) {
            log("captureFailed regionIndex=" + index + " region=" + region.toShortText());
            return;
        }
        BufferedImage scanImage = prepareScanImage(raw, region);
        if (scanImage == null) {
            log("captureFailed regionIndex=" + index + " reason=MASKED_COPY_FAILED region=" + region.toShortText());
            raw.flush();
            return;
        }
        try {
            ImageIO.write(scanImage, "png", rawPath.toFile());
            TextCandidateScanResult result =
                    debug.textLineService.findYellowTextCandidateResult(scanImage, washedPath, overlayPath);
            List<TextCandidate> candidates = result.candidates();

            log("regionIndex=" + index + " scanRegionWindowRelative=" + region.toShortText());
            log("regionIndex=" + index + " defaultMaskedWindow="
                    + OcrWindowScanService.isDefaultMaskedWindowRegion(region));
            log("raw=" + rawPath);
            log("washed=" + washedPath);
            log("overlay=" + overlayPath);
            log("status=" + result.status() + " message=" + result.message()
                    + " candidateCount=" + candidates.size());
            for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
                log("region=" + index + " candidate=" + (candidateIndex + 1)
                        + " " + candidates.get(candidateIndex).toSummaryText());
            }
        } finally {
            if (scanImage != raw) {
                scanImage.flush();
            }
            raw.flush();
        }
    }

    /**
     * Apply the shared full-window masks when the recommended region is the masked fallback source.
     *
     * @param raw captured region image; ownership stays with the caller.
     * @param region window-relative region that produced {@code raw}.
     * @return image used for candidate detection. The caller owns and must flush it when it is not
     * the same object as {@code raw}.
     */
    private static BufferedImage prepareScanImage(BufferedImage raw, OcrWindowRegion region) {
        if (OcrWindowScanService.isDefaultMaskedWindowRegion(region)) {
            return OcrWindowScanService.copyWithDefaultMasks(raw);
        }
        return raw;
    }

    /**
     * Select the window-relative OCR scan regions for this debug run.
     *
     * <p>Use {@code -Dnpc.text.region=fallback} for Xiuluo's old wider task area, or
     * {@code -Dnpc.text.region=x1,y1,x2,y2} for an explicit window-relative crop. In recommended
     * mode this debug tool consumes the regions returned by {@link OcrRoiMemoryService}; it does
     * not add its own fallback.</p>
     */
    private static List<OcrWindowRegion> selectedRegions(DebugContext debug, DebugTarget target) {
        String value = System.getProperty("npc.text.region", "recommended");
        if (value == null || value.isBlank() || "recommended".equalsIgnoreCase(value)) {
            return debug.roiMemoryService.recommendNpcClickWindowRegions(
                    target.mapName(), target.targetX(), target.targetY(), target.targetName(), target.roaming());
        }
        if ("fallback".equalsIgnoreCase(value)) {
            return List.of(XIULUO_TARGET_FALLBACK_REGION);
        }
        if ("primary".equalsIgnoreCase(value)) {
            return List.of(XIULUO_TARGET_PRIMARY_REGION);
        }
        String[] parts = value.split(",");
        if (parts.length == 4) {
            try {
                return List.of(new OcrWindowRegion(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim()))
                        .clamp(GAME_WIDTH, GAME_HEIGHT));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid npc.text.region=" + value
                        + "; expected recommended, primary, fallback, or x1,y1,x2,y2", e);
            }
        }
        throw new IllegalArgumentException("Invalid npc.text.region=" + value
                + "; expected recommended, primary, fallback, or x1,y1,x2,y2");
    }

    private record DebugTarget(String mapName, int targetX, int targetY, String targetName, boolean roaming) {
        private static DebugTarget fromProperties() {
            return new DebugTarget(
                    System.getProperty("npc.text.mapName", DEFAULT_MAP_NAME),
                    parseIntProperty("npc.text.targetX", DEFAULT_TARGET_X),
                    parseIntProperty("npc.text.targetY", DEFAULT_TARGET_Y),
                    System.getProperty("npc.text.targetName", DEFAULT_TARGET_NAME),
                    Boolean.parseBoolean(System.getProperty("npc.text.roaming", "true")));
        }

        private static int parseIntProperty(String key, int defaultValue) {
            String value = System.getProperty(key);
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid " + key + "=" + value, e);
            }
        }
    }

    private record DebugContext(
            WindowTaskContextHolder windowHolder,
            WindowRuntimeContext window,
            GameContext gameContext,
            GameClientTracker tracker,
            GameTextLineOcrService textLineService,
            OcrRoiMemoryService roiMemoryService
    ) {
        private static DebugContext from(ConfigurableApplicationContext app) {
            GameWindowRegistrationService registration = app.getBean(GameWindowRegistrationService.class);
            MultiWindowTaskManager manager = app.getBean(MultiWindowTaskManager.class);
            WindowFocusService focusService = app.getBean(WindowFocusService.class);
            WindowTaskContextHolder windowHolder = app.getBean(WindowTaskContextHolder.class);

            registration.registerDetectedGameWindows(TaskType.XIULUO);
            List<WindowTaskRunner> runners = new ArrayList<>(manager.getAllRunners());
            String preferredHandle = normalizeHandle(System.getProperty("npc.text.windowHandle"));
            String foregroundHandle = normalizeHandle(focusService.getForegroundNativeHandleText());

            log("registeredWindowCount=" + runners.size()
                    + " preferredHandle=" + preferredHandle
                    + " foregroundHandle=" + foregroundHandle);
            for (WindowTaskRunner runner : runners) {
                WindowRuntimeContext item = runner.getWindowContext();
                log("candidate windowId=" + item.getWindowId()
                        + " handle=" + item.getNativeBinding().getNativeHandle()
                        + " rect=" + item.getNativeBinding().getGeometryText()
                        + " title=" + item.getNativeBinding().getTitle());
            }

            WindowTaskRunner selected = selectRunner(runners, preferredHandle, foregroundHandle)
                    .orElseThrow(() -> new IllegalStateException(
                            "No game window selected; focus one DHXY client or pass -Dnpc.text.windowHandle=<handle>"));
            return new DebugContext(
                    windowHolder,
                    selected.getWindowContext(),
                    app.getBean(GameContext.class),
                    app.getBean(GameClientTracker.class),
                    app.getBean(GameTextLineOcrService.class),
                    app.getBean(OcrRoiMemoryService.class)
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
        System.out.println("[npc-text-candidate-game-debug] " + message);
    }
}
