package com.bot.dhxy.service;

import com.bot.dhxy.core.ImageFinder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BagCunkuanAnchorReplayTest {

    private static final Path RAW_FRAME = Path.of(
            "images/test-cases/bag/wuhuan-6056-main-bag-zhengli-anchor.png");
    private static final Path TEMPLATE = Path.of("images/template/bag/anchor_cunkuan.png");
    private static final Path MARKED_OUTPUT = Path.of(
            "images/test-cases/bag/wuhuan-6056-main-bag-cunkuan-anchor-marked.png");

    @Test
    void cunkuanAnchorResolvesTheFirstMainBagPage() throws Exception {
        BufferedImage raw = ImageIO.read(RAW_FRAME.toFile());
        BufferedImage template = ImageIO.read(TEMPLATE.toFile());
        assertNotNull(raw);
        assertNotNull(template);

        double[] match = ImageFinder.find(raw, template, 0.8d);
        assertNotNull(match, "存款模板必须在包裹画面中命中");
        int anchorX = (int) Math.round(match[0]);
        int anchorY = (int) Math.round(match[1]);
        int firstPageX = anchorX + 152;
        int firstPageY = anchorY + 57;
        assertEquals(218, anchorX);
        assertEquals(394, anchorY);
        assertEquals(370, firstPageX);
        assertEquals(451, firstPageY);

        Graphics2D graphics = raw.createGraphics();
        try {
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.setColor(Color.GREEN);
            graphics.drawOval(anchorX - 5, anchorY - 5, 10, 10);
            graphics.setColor(Color.RED);
            graphics.drawOval(firstPageX - 6, firstPageY - 6, 12, 12);
            graphics.drawLine(anchorX, anchorY, firstPageX, firstPageY);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(raw, "png", MARKED_OUTPUT.toFile());
    }
}
