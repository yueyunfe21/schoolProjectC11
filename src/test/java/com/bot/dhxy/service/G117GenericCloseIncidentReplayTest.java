package com.bot.dhxy.service;

import com.bot.dhxy.core.ImageFinder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G117: fixed-corpus replay of incident 3519 (2026-08-29 00:34, window hwnd-2CA0ECE).
 *
 * <p>The frame shows the task panel with its top-right X, a confirmation popup with 确定/取消, and
 * a 取消任务 button — all at once. The generic close scan used to load every PNG in
 * {@code images/template/cancel/} in filename order, so {@code quxiao.png} matched before any X skin
 * and the cleanup cancelled the Ghost King task (21 clicks in the 取消任务 area over 7 bursts that
 * night). These contracts pin both directions: the scan's first hit must land on the top-right X, and
 * the semantic 取消 template — although it still matches this frame perfectly — must never be part of
 * the scan.</p>
 *
 * <p><b>Marked evidence is produced by this test, not by hand.</b> Every run rewrites
 * {@link #MARKED} with the matched template box, the 取消任务 trap box, the click-jitter envelope and
 * the final red click dot, then reads the file back and verifies the dot is really on disk at the
 * click coordinate. The PNG committed next to the fixture is therefore an output of the current code:
 * if the scan ever moves, the checked-in image moves with it and the diff is visible.</p>
 *
 * <p>Coordinates are frame-local. Production adds the window origin and the DPI scale in
 * {@code CoordinateHelper.findImageAbsoluteCoordinateByImagePath}; the incident frame is a full
 * 1024x768 client capture, so for the replay both are identity and the geometry below is exactly what
 * production computed that night.</p>
 */
class G117GenericCloseIncidentReplayTest {

    private static final String FRAME = "images/test-cases/g117/incident-3519-cancel-task-frame.png";
    private static final String MARKED = "images/test-cases/g117/incident-3519-marked-click.png";

    /**
     * The semantic 取消 template that caused the incident. It has zero production readers — the
     * GhostKing and Tianting flows read {@code images/template/dialog/{guiwang,tianting}/quxiao.png} —
     * and was an untracked file dropped into the scan directory on 2026-08-28 18:01. It is kept here,
     * out of the flat scan, purely as the forensic fixture this negative contract needs.
     */
    private static final String QUARANTINED_CANCEL = "images/template/cancel/quarantine/quxiao.png";

    /**
     * Ground truth for "the top-right X of the task panel", measured once on the incident frame and
     * deliberately looser than any single X skin so it does not merely restate the match. It is the
     * only hand-supplied number in this file, and the marked PNG draws it (cyan) next to the actual
     * match (green) so the measurement can be re-checked by eye rather than trusted.
     */
    private static final int X_LEFT = 745;
    private static final int X_RIGHT = 785;
    private static final int X_TOP = 278;
    private static final int X_BOTTOM = 312;

    /** Production jitter in {@code clickCloseButtonOnceDirect}: {@code point + 4 + random.nextInt(5)}. */
    private static final int JITTER_MIN = 4;
    private static final int JITTER_MAX = 8;

    private static final Color MATCH_BOX = new Color(0, 220, 90);
    private static final Color ORACLE_BOX = new Color(0, 190, 255);
    private static final Color TRAP_BOX = new Color(255, 70, 70);
    private static final Color JITTER_BOX = new Color(255, 210, 0);
    private static final Color CLICK_DOT = new Color(255, 0, 0);

    /**
     * Positive: replaying the production scan order, the first hit is the top-right X — and every
     * click the production jitter can produce from it stays on that X button.
     */
    @Test
    void genericScanFirstHitLandsOnTheTopRightX() throws IOException {
        List<String> templates = UICleanerService.genericCloseButtonTemplates();
        assertTrue(!templates.isEmpty(), "x-skin templates must be discovered");

        double[] first = null;
        String firstTemplate = null;
        for (String template : templates) {
            double[] hit = ImageFinder.find(FRAME, template, 0.8);
            if (hit != null && hit.length >= 2) {
                first = hit;
                firstTemplate = template;
                break;
            }
        }
        assertNotNull(first, "an X skin must match the incident frame");

        int centerX = (int) Math.round(first[0]);
        int centerY = (int) Math.round(first[1]);
        String where = "(" + centerX + "," + centerY + ") via " + firstTemplate;
        assertTrue(centerX >= X_LEFT && centerX <= X_RIGHT && centerY >= X_TOP && centerY <= X_BOTTOM,
                "first hit must be the top-right X, got " + where);

        // The whole jitter envelope, not one sampled draw: production can click any of the 25 offsets.
        for (int dx = JITTER_MIN; dx <= JITTER_MAX; dx++) {
            for (int dy = JITTER_MIN; dy <= JITTER_MAX; dy++) {
                int clickX = centerX + dx;
                int clickY = centerY + dy;
                assertTrue(clickX >= X_LEFT && clickX <= X_RIGHT && clickY >= X_TOP && clickY <= X_BOTTOM,
                        "jitter offset (" + dx + "," + dy + ") left the X button: click=("
                                + clickX + "," + clickY + ") from " + where);
            }
        }

        int clickX = centerX + JITTER_MIN;
        int clickY = centerY + JITTER_MIN;
        writeMarkedEvidence(firstTemplate, centerX, centerY, clickX, clickY);

        // The committed PNG must be this run's output, not a hand-drawn picture: read it back and
        // confirm the red dot really sits at the click coordinate.
        BufferedImage reloaded = ImageIO.read(new File(MARKED));
        assertNotNull(reloaded, "marked evidence must be readable: " + MARKED);
        assertEquals(CLICK_DOT.getRGB(), reloaded.getRGB(clickX, clickY) | 0xFF000000,
                "marked evidence must carry the click dot at (" + clickX + "," + clickY + ")");
    }

    /**
     * Negative: 取消 still matches this frame perfectly — proving the fix is the exclusion, not a
     * lucky miss — and the trap sits far from the X, so the old behaviour was a different button
     * entirely rather than a near-miss.
     */
    @Test
    void semanticCancelStillMatchesButIsExcludedFromThePool() {
        assertTrue(Files.exists(Path.of(QUARANTINED_CANCEL)),
                "the quarantined 取消 template is this contract's fixture and must be kept: "
                        + QUARANTINED_CANCEL);
        double[] quxiao = ImageFinder.find(FRAME, QUARANTINED_CANCEL, 0.8);
        assertNotNull(quxiao, "quxiao.png must still match the incident frame (that is the trap)");
        assertTrue(quxiao[1] > X_BOTTOM,
                "the 取消 trap must be a different button, well below the top-right X, got y=" + quxiao[1]);

        List<String> templates = UICleanerService.genericCloseButtonTemplates();
        assertTrue(templates.stream().noneMatch(path -> path.endsWith("quxiao.png")),
                "the semantic cancel template must be excluded from the generic pool");
        assertTrue(templates.stream().noneMatch(path -> path.contains("quarantine")),
                "the quarantine directory must never feed the flat generic scan");
    }

    private void writeMarkedEvidence(String template, int centerX, int centerY, int clickX, int clickY)
            throws IOException {
        BufferedImage frame = ImageIO.read(new File(FRAME));
        assertNotNull(frame, "incident frame must be readable: " + FRAME);
        BufferedImage template_ = ImageIO.read(new File(template));
        assertNotNull(template_, "matched template must be readable: " + template);

        BufferedImage marked = new BufferedImage(
                frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = marked.createGraphics();
        try {
            g.drawImage(frame, 0, 0, null);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setStroke(new BasicStroke(1f));

            // What the fix protects: the hand-measured X-button oracle this test asserts against.
            g.setColor(ORACLE_BOX);
            g.drawRect(X_LEFT, X_TOP, X_RIGHT - X_LEFT, X_BOTTOM - X_TOP);

            // What actually matched, drawn from the winning template's own size.
            int boxLeft = centerX - template_.getWidth() / 2;
            int boxTop = centerY - template_.getHeight() / 2;
            g.setColor(MATCH_BOX);
            g.drawRect(boxLeft, boxTop, template_.getWidth(), template_.getHeight());

            // Every click production could emit from this match.
            g.setColor(JITTER_BOX);
            g.drawRect(centerX + JITTER_MIN, centerY + JITTER_MIN,
                    JITTER_MAX - JITTER_MIN, JITTER_MAX - JITTER_MIN);

            // The trap the excluded template still matches, so the marked image shows both buttons.
            double[] trap = ImageFinder.find(FRAME, QUARANTINED_CANCEL, 0.8);
            if (trap != null && trap.length >= 2) {
                BufferedImage trapTemplate = ImageIO.read(new File(QUARANTINED_CANCEL));
                if (trapTemplate != null) {
                    g.setColor(TRAP_BOX);
                    g.drawRect((int) Math.round(trap[0]) - trapTemplate.getWidth() / 2,
                            (int) Math.round(trap[1]) - trapTemplate.getHeight() / 2,
                            trapTemplate.getWidth(), trapTemplate.getHeight());
                }
            }

            // The final click, last so nothing paints over it.
            g.setColor(CLICK_DOT);
            g.fillRect(clickX - 1, clickY - 1, 3, 3);
            g.drawLine(clickX - 10, clickY, clickX - 3, clickY);
            g.drawLine(clickX + 3, clickY, clickX + 10, clickY);
            g.drawLine(clickX, clickY - 10, clickX, clickY - 3);
            g.drawLine(clickX, clickY + 3, clickX, clickY + 10);
        } finally {
            g.dispose();
        }

        // The dot must survive whatever the crosshair drew around it.
        marked.setRGB(clickX, clickY, CLICK_DOT.getRGB());

        File out = new File(MARKED);
        Files.createDirectories(out.toPath().toAbsolutePath().getParent());
        assertTrue(ImageIO.write(marked, "png", out), "marked evidence must be written: " + MARKED);
    }
}
