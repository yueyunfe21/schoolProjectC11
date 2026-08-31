package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G113-4: fixed-corpus replay of the dialog frame detector (the single presence authority).
 *
 * <p>Two failure shapes are pinned. First, the darkened wrong-answer overlay turns large scene
 * areas neutral: a single scene row at y=0 used to qualify as the top edge (top=0 instead of 97)
 * and the darkened right screen edge leaked the right bound to 620. Real panel edges span multiple
 * consecutive qualifying rows (13 measured) while fake scene lines are single rows, and the true
 * top/bottom pair has the widest run overlap. Second, a bare scene without any dialog must yield
 * {@code present=false} (fail-closed) — the old detector reported a frame and the quiz task spun
 * for 30 seconds clicking into an empty scene.</p>
 */
class G113DialogFrameBoundsReplayTest {

    private static final String CORPUS = "images/test-cases/g113/";
    private final DialogFramePresenceMechanics detector = new DialogFramePresenceMechanics();

    @Test
    void darkenedWrongAnswerFramesAnchorOnTheRealPanelEdges() throws Exception {
        for (String name : new String[] {"quiz_dialog_wrong_popup_q1.png", "quiz_dialog_wrong_popup_q10.png"}) {
            DialogFramePresenceMechanics.FramePresence frame = analyze(name);
            assertTrue(frame.present(), name + ": frame must be present");
            assertEquals(97, frame.top(), name + ": top must anchor on the real panel edge, not a scene row");
            assertTrue(frame.left() >= 44 && frame.left() <= 55,
                    name + ": left must be the real panel border (~48/51), got " + frame.left());
            assertEquals(238, frame.bottom(), name + ": bottom must anchor on the real panel edge");
            assertTrue(frame.right() <= 585,
                    name + ": right must not leak into the darkened screen edge, got " + frame.right());
        }
    }

    @Test
    void referenceFrameKeepsItsKnownBounds() throws Exception {
        DialogFramePresenceMechanics.FramePresence frame = analyze("quiz_dialog_example.png");
        assertTrue(frame.present());
        assertEquals(51, frame.left());
        assertEquals(97, frame.top());
        assertEquals(238, frame.bottom());
    }

    @Test
    void bareSceneYieldsNoFrame() throws Exception {
        DialogFramePresenceMechanics.FramePresence frame = analyze("npc_3465_clean_frame.png");
        assertFalse(frame.present(), "a bare scene must never produce a dialog frame");
    }

    private DialogFramePresenceMechanics.FramePresence analyze(String name) throws Exception {
        BufferedImage image = ImageIO.read(new File(CORPUS + name));
        BufferedImage roi = (image.getWidth() > 640 || image.getHeight() > 300)
                ? image.getSubimage(200, 250, 640, 300) : image;
        return detector.analyze(roi);
    }
}
