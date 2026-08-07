package com.bot.dhxy.window.observation;

import com.bot.dhxy.tools.CoordinateHelper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XinshouAnchorLocalMechanicsTest {

    @Test
    void titleAndEscBotInOneSharedFrameProduceTwoIndependentFacts() throws IOException {
        BufferedImage title = load("xunren.png");
        BufferedImage escBot = load("esc_bot.png");
        XinshouAnchorLocalMechanics mechanics = mechanics(rect -> {
            if (rect[0] == 6 && rect[1] == 196) {
                return imageWith(title, 201, 355);
            }
            if (rect[0] == 549 && rect[1] == 667) {
                return imageWith(escBot, 22, 16);
            }
            return blank(rect[2] - rect[0], rect[3] - rect[1]);
        });

        assertEquals(List.of("xunren.png", "esc_bot.png"), mechanics.sampleAnchors());
    }

    @Test
    void missingTemplatesProduceNoFact() {
        XinshouAnchorLocalMechanics mechanics = mechanics(
                rect -> blank(rect[2] - rect[0], rect[3] - rect[1]));

        assertEquals(List.of(), mechanics.sampleAnchors());
    }

    @Test
    void skipAndEscUseTheSharedTopRightUnionWithoutSharingTheirVerdict() throws IOException {
        BufferedImage skip = load("tiaoguo.png");
        XinshouAnchorLocalMechanics mechanics = mechanics(rect -> {
            if (rect[0] == 870 && rect[1] == 57 && rect[2] == 998 && rect[3] == 106) {
                return imageWith(skip, 128, 49);
            }
            return blank(rect[2] - rect[0], rect[3] - rect[1]);
        });

        XinshouAnchorLocalMechanics.AnchorSample sample = mechanics.sample();

        assertEquals(true, sample.skipVisible());
        assertEquals(false, sample.escVisible());
    }

    private static XinshouAnchorLocalMechanics mechanics(
            LocalCombatSignalMechanics.CycleFrameCropper cropper) {
        XinshouAnchorLocalMechanics mechanics = new XinshouAnchorLocalMechanics(null,
                new CoordinateHelper(null, null) {
                    @Override
                    public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
                        return new int[]{offsetX, offsetY, offsetX + width, offsetY + height};
                    }
                });
        mechanics.bindCycleFrameCropper(cropper);
        return mechanics;
    }

    private static BufferedImage load(String filename) throws IOException {
        return ImageIO.read(Path.of("images/template/xinshou", filename).toFile());
    }

    private static BufferedImage imageWith(BufferedImage template, int width, int height) {
        BufferedImage source = blank(width, height);
        Graphics2D graphics = source.createGraphics();
        graphics.drawImage(template, 0, 0, null);
        graphics.dispose();
        return source;
    }

    private static BufferedImage blank(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }
}
