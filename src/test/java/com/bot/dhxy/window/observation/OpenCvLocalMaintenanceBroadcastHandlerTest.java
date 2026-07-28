package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenCvLocalMaintenanceBroadcastHandlerTest {

    @Test
    void rawBaselineTemplateResolvesItsCenterInsideTheFixedRoi() throws Exception {
        BufferedImage template = ImageIO.read(Path.of(
                "images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png").toFile());
        assertNotNull(template);
        BufferedImage roi = new BufferedImage(
                OpenCvLocalMaintenanceBroadcastHandler.ROI_WIDTH,
                OpenCvLocalMaintenanceBroadcastHandler.ROI_HEIGHT,
                BufferedImage.TYPE_3BYTE_BGR);
        int templateX = 6;
        int templateY = 11;
        Graphics2D graphics = roi.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, roi.getWidth(), roi.getHeight());
        graphics.drawImage(template, templateX, templateY, null);
        graphics.dispose();

        OpenCvLocalMaintenanceBroadcastHandler.LocalMatch match =
                OpenCvLocalMaintenanceBroadcastHandler.findMatch(roi, List.of(
                        new OpenCvLocalMaintenanceBroadcastHandler.LoadedTemplate("heal-all-repair", template)));

        assertNotNull(match);
        assertEquals("heal-all-repair", match.actionKey());
        assertEquals(templateX + template.getWidth() / 2.0, match.centerX(), 0.01);
        assertEquals(templateY + template.getHeight() / 2.0, match.centerY(), 0.01);
        assertTrue(match.score() >= OpenCvLocalMaintenanceBroadcastHandler.MATCH_THRESHOLD);

        Graphics2D marked = roi.createGraphics();
        marked.setColor(Color.RED);
        marked.setStroke(new BasicStroke(1.5f));
        marked.drawRect(templateX, templateY, template.getWidth() - 1, template.getHeight() - 1);
        int clickX = (int) Math.round(match.centerX());
        int clickY = (int) Math.round(match.centerY());
        marked.drawLine(clickX - 4, clickY, clickX + 4, clickY);
        marked.drawLine(clickX, clickY - 4, clickX, clickY + 4);
        marked.dispose();
        Path output = Path.of("target/test-artifacts/maintenance-broadcast-replay-marked.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(roi, "png", output.toFile());
        roi.flush();
        template.flush();
    }
}
