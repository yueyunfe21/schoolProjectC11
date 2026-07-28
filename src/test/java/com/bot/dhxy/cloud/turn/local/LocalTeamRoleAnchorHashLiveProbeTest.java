package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.capture.WindowCaptureEvidenceStore;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.discovery.NativeWindowInfo;
import com.bot.dhxy.window.discovery.WindowsNativeWindowScanner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Manual live probe for the team-role preflight anchor experiment.
 *
 * <p>The probe captures a 10x10 window-relative ROI at the normal tooltip hover anchor from each visible
 * game HWND. It never focuses a window, moves the mouse, or sends input. The hash is calculated from the
 * raw ARGB pixels in row-major order, rather than from PNG bytes, so it is an exact comparison of the
 * captured pixels.</p>
 */
class LocalTeamRoleAnchorHashLiveProbeTest {

    private static final int HOVER_X = 644;
    private static final int HOVER_Y = 91;
    private static final int ANCHOR_SIZE = 10;
    private static final Path OUTPUT_ROOT = Path.of("images", "test-cases", "team-role-anchor-hash-live");
    private static final DateTimeFormatter OUTPUT_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Test
    void sameTeamWindowsHaveIdenticalRawAnchorHashes() throws Exception {
        List<NativeWindowInfo> windows = new WindowsNativeWindowScanner().scanGameWindows().stream()
                .sorted(Comparator.comparing(NativeWindowInfo::toWindowId))
                .toList();
        assertEquals(5, windows.size(), () -> "expected exactly five visible game windows, actual="
                + windows.stream().map(NativeWindowInfo::toWindowId).toList());

        BoundWindowCaptureService captureService = new BoundWindowCaptureService(
                new WindowCaptureEvidenceStore(new WindowTaskContextHolder(new WindowIsolationProperties())));
        Path outputDirectory = OUTPUT_ROOT.resolve(OUTPUT_STAMP.format(LocalDateTime.now()));
        Files.createDirectories(outputDirectory);

        List<AnchorCapture> captures = new ArrayList<>();
        for (NativeWindowInfo window : windows) {
            WindowNativeBinding binding = new WindowNativeBinding(
                    "0x" + window.getHandle(), window.getTitle(), window.getClassName(), window.getProcessId(),
                    window.getX(), window.getY(), window.getWidth(), window.getHeight());
            Optional<BoundWindowCaptureService.CaptureResult> result = captureService.captureRegion(
                    binding,
                    window.getX(),
                    window.getY(),
                    window.getX() + HOVER_X,
                    window.getY() + HOVER_Y,
                    window.getX() + HOVER_X + ANCHOR_SIZE,
                    window.getY() + HOVER_Y + ANCHOR_SIZE);
            if (result.isEmpty()) {
                throw new AssertionError("anchor capture failed: " + window.toWindowId());
            }

            BufferedImage image = result.get().image();
            String hash = rawArgbSha256(image);
            Path rawOutput = outputDirectory.resolve(safeFileName(window.toWindowId()) + "_anchor_10x10.png");
            ImageIO.write(image, "png", rawOutput.toFile());
            captures.add(new AnchorCapture(window.toWindowId(), window.getTitle(), hash, rawOutput, image));
        }

        Path contactSheet = outputDirectory.resolve("anchor_10x10_contact-sheet.png");
        writeContactSheet(captures, contactSheet);
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (AnchorCapture capture : captures) {
            groups.computeIfAbsent(capture.hash(), ignored -> new ArrayList<>()).add(capture.windowId());
            System.out.printf("[anchor-hash] window=%s hash=%s image=%s%n",
                    capture.windowId(), capture.hash(), capture.imagePath());
        }
        System.out.printf("[anchor-hash] groups=%s contactSheet=%s%n", groups, contactSheet);

        assertEquals(1, groups.size(), () -> "same-team anchor pixels differ: " + groups
                + "; inspect=" + contactSheet);
    }

    private static String rawArgbSha256(BufferedImage image) throws Exception {
        ByteBuffer pixels = ByteBuffer.allocate(image.getWidth() * image.getHeight() * Integer.BYTES);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels.putInt(image.getRGB(x, y));
            }
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pixels.array()));
    }

    private static void writeContactSheet(List<AnchorCapture> captures, Path output) throws Exception {
        int tileScale = 20;
        int tileSize = ANCHOR_SIZE * tileScale;
        int labelHeight = 38;
        int width = Math.max(1, captures.size()) * tileSize;
        BufferedImage sheet = new BufferedImage(width, tileSize + labelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, sheet.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            for (int index = 0; index < captures.size(); index++) {
                AnchorCapture capture = captures.get(index);
                int x = index * tileSize;
                graphics.drawImage(capture.image(), x, 0, tileSize, tileSize, null);
                graphics.setColor(Color.BLACK);
                graphics.drawString(capture.windowId(), x + 2, tileSize + 12);
                graphics.drawString(capture.hash().substring(0, 12), x + 2, tileSize + 26);
            }
        } finally {
            graphics.dispose();
        }
        try {
            ImageIO.write(sheet, "png", output.toFile());
        } finally {
            sheet.flush();
        }
    }

    private static String safeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private record AnchorCapture(String windowId, String title, String hash, Path imagePath, BufferedImage image) {
    }
}
