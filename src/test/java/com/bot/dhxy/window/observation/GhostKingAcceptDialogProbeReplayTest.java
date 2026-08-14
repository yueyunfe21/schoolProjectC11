package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Replays the real 鬼王 accept frame through the shared 天庭 local-probe geometry. */
class GhostKingAcceptDialogProbeReplayTest {

    private static final Path CASE_DIR = Path.of("images", "test-cases", "guiwang");

    @Test
    void realGhostKingAcceptDialogMatchesInSharedRoiAndMarksFinalClick() throws Exception {
        Path rawPath = CASE_DIR.resolve("live-leader-current-raw.png");
        BufferedImage raw = ImageIO.read(rawPath.toFile());
        BufferedImage template = ImageIO.read(Path.of(
                TiantingDialogLocalMechanics.GHOST_KING_ACCEPT).toFile());
        assertTrue(raw != null && template != null, "real frame and template must decode");

        BufferedImage roi = raw.getSubimage(
                TiantingDialogLocalMechanics.DIALOG_ROI_LEFT,
                TiantingDialogLocalMechanics.DIALOG_ROI_TOP,
                TiantingDialogLocalMechanics.DIALOG_ROI_WIDTH,
                TiantingDialogLocalMechanics.DIALOG_ROI_HEIGHT);
        TiantingDialogLocalMechanics.OptionHit hit =
                TiantingDialogLocalMechanics.matchGhostKingAcceptOption(roi).orElseThrow();

        assertEquals(TiantingDialogLocalMechanics.ACTION_GHOST_KING_ACCEPT_TASK, hit.actionKey());
        assertTrue(hit.score() >= TiantingDialogLocalMechanics.MATCH_RATE,
                "real frame must clear the production threshold");

        int clickX = TiantingDialogLocalMechanics.DIALOG_ROI_LEFT + hit.roiOffsetX();
        int clickY = TiantingDialogLocalMechanics.DIALOG_ROI_TOP + hit.roiOffsetY();
        int boxX = clickX - template.getWidth() / 2;
        int boxY = clickY - template.getHeight() / 2;
        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = marked.createGraphics();
        try {
            graphics.drawImage(raw, 0, 0, null);
            graphics.setColor(Color.RED);
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.drawRect(
                    TiantingDialogLocalMechanics.DIALOG_ROI_LEFT,
                    TiantingDialogLocalMechanics.DIALOG_ROI_TOP,
                    TiantingDialogLocalMechanics.DIALOG_ROI_WIDTH - 1,
                    TiantingDialogLocalMechanics.DIALOG_ROI_HEIGHT - 1);
            graphics.drawRect(boxX, boxY, template.getWidth() - 1, template.getHeight() - 1);
            graphics.drawLine(clickX - 8, clickY, clickX + 8, clickY);
            graphics.drawLine(clickX, clickY - 8, clickX, clickY + 8);
            graphics.drawString(String.format("score=%.6f click=(%d,%d)", hit.score(), clickX, clickY),
                    boxX, Math.max(14, boxY - 6));
        } finally {
            graphics.dispose();
        }
        Files.createDirectories(CASE_DIR);
        Path markedPath = CASE_DIR.resolve("live-leader-current-tianting-pipeline-marked.png");
        ImageIO.write(marked, "png", markedPath.toFile());

        marked.flush();
        roi.flush();
        template.flush();
        raw.flush();
    }
}
