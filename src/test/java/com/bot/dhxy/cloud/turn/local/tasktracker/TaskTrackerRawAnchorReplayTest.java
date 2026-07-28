package com.bot.dhxy.cloud.turn.local.tasktracker;

import com.bot.dhxy.core.ImageFinder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTrackerRawAnchorReplayTest {

    private static final Path RAW_FRAME = Path.of(
            "images/test-cases/task-tracker/xiuluo-startup-anchor/raw/anchor-present.png");
    private static final Path ANCHOR_TEMPLATE = Path.of(
            "images/template/task/wubei_tracker_anchor.png");

    @Test
    void currentXiuluoFrameFindsTheGenericTrackerAnchorWithoutPreprocessing() throws Exception {
        BufferedImage raw = ImageIO.read(RAW_FRAME.toFile());
        BufferedImage template = ImageIO.read(ANCHOR_TEMPLATE.toFile());
        assertNotNull(raw);
        assertNotNull(template);
        try {
            double[] match = ImageFinder.find(raw, template, 0.82d);
            assertNotNull(match, "the raw frame must resolve the generic task-tracker anchor");
            assertEquals(121.5d, match[0], 0.01d);
            assertEquals(216.0d, match[1], 0.01d);
            assertTrue(match[2] >= 0.82d, "the production threshold must remain unchanged");
        } finally {
            raw.flush();
            template.flush();
        }
    }
}
