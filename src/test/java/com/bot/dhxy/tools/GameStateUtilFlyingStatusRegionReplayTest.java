package com.bot.dhxy.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replay guard for the Alt+U flying-state probe rectangle.
 */
public class GameStateUtilFlyingStatusRegionReplayTest {

    private static final Path GAME_STATE_UTIL = Path.of("src/main/java/com/bot/dhxy/tools/GameStateUtil.java");
    private static final File RAW_STATUS_PANEL =
            new File("images/test-cases/status/flying_status_altu_67555_raw.png");
    private static final File UNFLYING_TEMPLATE =
            new File("images/template/status/unflying.png");

    public static void main(String[] args) throws Exception {
        String source = Files.readString(GAME_STATE_UTIL, StandardCharsets.UTF_8);
        int relX = readIntConstant(source, "FLYING_STATUS_REL_X");
        int relY = readIntConstant(source, "FLYING_STATUS_REL_Y");
        int width = readIntConstant(source, "FLYING_STATUS_WIDTH");
        int height = readIntConstant(source, "FLYING_STATUS_HEIGHT");

        require(relX == 660, "flying-state probe must use the user-confirmed left x=660");
        require(relY == 573, "flying-state probe must use the user-confirmed top y=573");
        require(width == 52, "flying-state probe must use the user-confirmed width=52");
        require(height == 24, "flying-state probe must use the user-confirmed height=24");

        BufferedImage raw = ImageIO.read(RAW_STATUS_PANEL);
        BufferedImage template = ImageIO.read(UNFLYING_TEMPLATE);
        BufferedImage crop = raw.getSubimage(relX, relY, width, height);
        require(containsTemplate(crop, template, 0.03),
                "user-confirmed flying-state probe rect must match the visible 飞行 template");
    }

    private static int readIntConstant(String source, String name) {
        Pattern pattern = Pattern.compile("private static final int\\s+" + name + "\\s*=\\s*(\\d+)\\s*;");
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new AssertionError("Missing int constant: " + name);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static boolean containsTemplate(BufferedImage source, BufferedImage template, double maxDiffRatio) {
        if (source.getWidth() < template.getWidth() || source.getHeight() < template.getHeight()) {
            return false;
        }
        int maxDiffs = (int) Math.floor(template.getWidth() * template.getHeight() * maxDiffRatio);
        for (int y = 0; y <= source.getHeight() - template.getHeight(); y++) {
            for (int x = 0; x <= source.getWidth() - template.getWidth(); x++) {
                if (diffsAt(source, template, x, y) <= maxDiffs) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int diffsAt(BufferedImage source, BufferedImage template, int x, int y) {
        int diffs = 0;
        for (int ty = 0; ty < template.getHeight(); ty++) {
            for (int tx = 0; tx < template.getWidth(); tx++) {
                int src = source.getRGB(x + tx, y + ty);
                int tpl = template.getRGB(tx, ty);
                if (Math.abs(((src >> 16) & 0xff) - ((tpl >> 16) & 0xff)) > 15
                        || Math.abs(((src >> 8) & 0xff) - ((tpl >> 8) & 0xff)) > 15
                        || Math.abs((src & 0xff) - (tpl & 0xff)) > 15) {
                    diffs++;
                }
            }
        }
        return diffs;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
