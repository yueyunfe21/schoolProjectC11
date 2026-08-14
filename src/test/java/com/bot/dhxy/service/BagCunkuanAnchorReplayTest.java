package com.bot.dhxy.service;

import com.bot.dhxy.core.ImageFinder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BagCunkuanAnchorReplayTest {

    private static final Path RAW_FRAME = Path.of(
            "images/test-cases/bag/wuhuan-6056-main-bag-zhengli-anchor.png");
    private static final Path TEMPLATE = Path.of("images/template/bag/anchor_cunkuan.png");
    private static final Path MARKED_OUTPUT = Path.of(
            "images/test-cases/bag/wuhuan-6056-main-bag-cunkuan-anchor-marked.png");
    private static final Path STALE_CACHE_MARKED_OUTPUT = Path.of(
            "images/test-cases/bag/wuhuan-6056-main-bag-stale-cache-fallback-marked.png");

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

    @Test
    void staleCachedRoiMissesBeforeTheFullWindowRecoversTheRealMovableAnchor() throws Exception {
        BufferedImage raw = ImageIO.read(RAW_FRAME.toFile());
        BufferedImage template = ImageIO.read(TEMPLATE.toFile());
        assertNotNull(raw);
        assertNotNull(template);

        int staleAnchorX = 600;
        int staleAnchorY = 300;
        int roiHalfWidth = 45;
        int roiHalfHeight = 35;
        BufferedImage staleRoi = raw.getSubimage(
                staleAnchorX - roiHalfWidth,
                staleAnchorY - roiHalfHeight,
                roiHalfWidth * 2,
                roiHalfHeight * 2);
        assertNull(ImageFinder.find(staleRoi, template, 0.8d),
                "缓存位置已经失效时，小 ROI 不得伪造包裹锚点");

        double[] recovered = ImageFinder.find(raw, template, 0.8d);
        assertNotNull(recovered, "缓存 ROI miss 后，整窗回退必须重新找到可移动包裹的真实锚点");
        int recoveredX = (int) Math.round(recovered[0]);
        int recoveredY = (int) Math.round(recovered[1]);
        assertEquals(218, recoveredX);
        assertEquals(394, recoveredY);

        Graphics2D graphics = raw.createGraphics();
        try {
            graphics.setStroke(new BasicStroke(3.0f));
            graphics.setColor(Color.RED);
            graphics.drawRect(
                    staleAnchorX - roiHalfWidth,
                    staleAnchorY - roiHalfHeight,
                    roiHalfWidth * 2,
                    roiHalfHeight * 2);
            graphics.setColor(Color.GREEN);
            graphics.drawOval(recoveredX - 7, recoveredY - 7, 14, 14);
            graphics.drawLine(staleAnchorX, staleAnchorY, recoveredX, recoveredY);
        } finally {
            graphics.dispose();
            staleRoi.flush();
        }
        ImageIO.write(raw, "png", STALE_CACHE_MARKED_OUTPUT.toFile());
    }

    @Test
    void bagOpenCheckUsesPerWindowCacheAndNeverAWindowRelativeFixedBagPosition() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/BagService.java"));

        assertTrue(source.contains("lastMainBagAnchorCache.get(bagCacheKey(layout))"));
        assertTrue(source.contains("tracker.updateGlobalVision()"));
        assertTrue(source.contains("cached main bag anchor missed, search full window"));
        assertFalse(source.contains("MAIN_BAG_ANCHOR_RELATIVE_X"));
        assertFalse(source.contains("MAIN_BAG_ANCHOR_RELATIVE_Y"));
    }
}
