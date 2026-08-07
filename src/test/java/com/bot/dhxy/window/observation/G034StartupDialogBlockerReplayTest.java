package com.bot.dhxy.window.observation;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Replays the exact five G034 startup-preflight frames and writes marked click evidence. */
public final class G034StartupDialogBlockerReplayTest {

    private static final int PANEL_LEFT = 142;
    private static final int PANEL_TOP = 229;
    private static final int DIALOG_LEFT = 250 - PANEL_LEFT;
    private static final int DIALOG_TOP = 312 - PANEL_TOP;
    private static final int DIALOG_WIDTH = 529;
    private static final int DIALOG_HEIGHT = 208;
    private static final int STORY_CLICK_X = 250 + 529 / 2 - PANEL_LEFT;
    private static final int STORY_CLICK_Y = 312 + 208 - 40 - PANEL_TOP;

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path input = root.resolve("images/test-cases/team-role-startup-blocker");
        Path output = root.resolve("images/test-output/g034-team-role-startup-blocker");
        Files.createDirectories(output);

        Map<String, Boolean> cases = new LinkedHashMap<>();
        cases.put("hwnd-93B12C4-raw.png", false);
        cases.put("hwnd-E5F0A78-raw.png", true);
        cases.put("hwnd-3841CC8-raw.png", true);
        cases.put("hwnd-9970D92-raw.png", true);
        cases.put("hwnd-49C017C-raw.png", true);

        DialogFramePresenceMechanics mechanics = new DialogFramePresenceMechanics();
        int passed = 0;
        for (Map.Entry<String, Boolean> testCase : cases.entrySet()) {
            BufferedImage frame = ImageIO.read(input.resolve(testCase.getKey()).toFile());
            require(frame != null, "missing testcase " + testCase.getKey());
            BufferedImage roi = frame.getSubimage(DIALOG_LEFT, DIALOG_TOP, DIALOG_WIDTH, DIALOG_HEIGHT);
            boolean actual = mechanics.isPresent(roi);
            require(actual == testCase.getValue(),
                    testCase.getKey() + " expected=" + testCase.getValue() + " actual=" + actual);

            BufferedImage marked = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = marked.createGraphics();
            try {
                graphics.drawImage(frame, 0, 0, null);
                graphics.setStroke(new BasicStroke(3));
                graphics.setColor(actual ? Color.RED : Color.GREEN);
                graphics.drawRect(DIALOG_LEFT, DIALOG_TOP, DIALOG_WIDTH - 1, DIALOG_HEIGHT - 1);
                if (actual) {
                    graphics.fillOval(STORY_CLICK_X - 6, STORY_CLICK_Y - 6, 13, 13);
                }
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                graphics.drawString(actual ? "DIALOG BLOCKER / STORY CLICK" : "NO DIALOG BLOCKER", 12, 24);
            } finally {
                graphics.dispose();
            }
            ImageIO.write(marked, "png", output.resolve(testCase.getKey().replace("-raw", "-marked")).toFile());
            roi.flush();
            frame.flush();
            marked.flush();
            passed++;
        }
        System.out.println("G034_STARTUP_DIALOG_REPLAY_PASS=" + passed + "/" + cases.size());
        System.out.println("G034_MARKED_OUTPUT=" + output);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
