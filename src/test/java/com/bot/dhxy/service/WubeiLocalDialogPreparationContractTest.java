package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.OpenCvNativeLoader;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.task.wubei.WubeiDialogCatalog;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WubeiLocalDialogPreparationContractTest {

    private static final int[] DIALOG_RECT = {250, 312, 779, 520};

    @Test
    void allFrozenEnterBattleTemplatesPrepareTheirExactActionWithoutInputOrDisk() {
        OpenCvNativeLoader.ensureLoaded();
        DialogService service = new DialogService(
                new GameClientTracker(null, null, null, null, null, null, null, null, null, null, null),
                new CoordinateHelper(null, null) {
                    @Override
                    public int[] getScaledRect(int x, int y, int width, int height) {
                        return new int[]{x, y, x + width, y + height};
                    }
                });

        for (GreenTemplateClickSpec spec : WubeiDialogCatalog.enterBattleSpecs()) {
            BufferedImage template = ImagePreprocessor.pathToBufferedImage(spec.templatePath());
            assertFalse(template == null, "frozen template must be loadable: " + spec.templatePath());
            BufferedImage raw = optionFrameContaining(template);
            try {
                DialogService.LocalPreparedDialogMatch match = service
                        .prepareWubeiEnterBattleFromFrame(raw, DIALOG_RECT, "contract-test")
                        .orElseThrow();
                assertEquals(spec.name(), match.action().getTargetKeyword());
                assertEquals(spec.templatePath(), match.action().getMatchedText());
                int centerX = (match.matchLeft() + match.matchRight()) / 2;
                int centerY = (match.matchTop() + match.matchBottom()) / 2;
                int offsetX = match.action().getAbsoluteX() - centerX;
                int offsetY = match.action().getAbsoluteY() - centerY;
                assertTrue(offsetX >= spec.minOffsetX() && offsetX <= spec.maxOffsetX());
                assertTrue(Math.abs(offsetY) <= spec.randomRadiusY());
                assertTrue(match.action().isClickRequired());
            } finally {
                raw.flush();
                template.flush();
            }
        }
    }

    private static BufferedImage optionFrameContaining(BufferedImage template) {
        BufferedImage raw = new BufferedImage(529, 208, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = raw.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, raw.getWidth(), raw.getHeight());
        } finally {
            graphics.dispose();
        }
        int originX = 80;
        int originY = 120;
        for (int y = 80; y < 90; y++) {
            for (int x = 10; x < 30; x++) {
                raw.setRGB(x, y, Color.GREEN.getRGB());
            }
        }
        for (int y = 0; y < template.getHeight(); y++) {
            for (int x = 0; x < template.getWidth(); x++) {
                int rgb = template.getRGB(x, y);
                int brightness = ((rgb >>> 16) & 0xff) + ((rgb >>> 8) & 0xff) + (rgb & 0xff);
                if (brightness > 0) {
                    raw.setRGB(originX + x, originY + y, Color.GREEN.getRGB());
                }
            }
        }
        return raw;
    }
}
