package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G005 WP3: the client-local 天庭 option matcher.
 *
 * <p>The pastes use the real option templates, so what is under test is whether these four crops can
 * be told apart at the production threshold — they are same-font, same-background, similar-length
 * Chinese phrases, which is exactly the case where a template match quietly claims the wrong one.</p>
 */
class TiantingDialogLocalMechanicsTest {

    private static final int OPTION_X = 60;
    private static final int OPTION_Y = 70;

    @Test
    void eachOptionOnItsOwnIsIdentifiedAsItself() throws IOException {
        // The priority list would mask a mis-identification if several options were on screen: asking
        // for 看打 first means a 看打 template that also matches 卓越's pixels still "passes". One
        // option at a time is the only way to see that.
        for (String templatePath : List.of(
                TiantingDialogLocalMechanics.KAIDA,
                TiantingDialogLocalMechanics.DUOXIE,
                TiantingDialogLocalMechanics.ZHUOYUE,
                TiantingDialogLocalMechanics.YAOWANG)) {
            BufferedImage roi = dialogCanvas();
            paste(roi, templatePath, OPTION_X, OPTION_Y);

            Optional<TiantingDialogLocalMechanics.OptionHit> hit =
                    TiantingDialogLocalMechanics.matchResidentOption(roi);

            assertTrue(hit.isPresent(), templatePath + " must match itself");
            assertEquals(templatePath, hit.get().templatePath(),
                    "a frame showing only " + name(templatePath) + " was read as "
                            + name(hit.get().templatePath()));
        }
    }

    @Test
    void kaidaWinsWhenSeveralCombatOptionsAreOnScreen() throws IOException {
        // 看打 and 卓越 both lead into a fight, but 看打 is the one the flow expects; picking 卓越 here
        // would silently take the two-experience branch whenever both happen to be offered.
        BufferedImage roi = dialogCanvas();
        paste(roi, TiantingDialogLocalMechanics.ZHUOYUE, OPTION_X, OPTION_Y + 50);
        paste(roi, TiantingDialogLocalMechanics.KAIDA, OPTION_X, OPTION_Y);

        Optional<TiantingDialogLocalMechanics.OptionHit> hit =
                TiantingDialogLocalMechanics.matchResidentOption(roi);

        assertTrue(hit.isPresent());
        assertEquals(TiantingDialogLocalMechanics.KAIDA, hit.get().templatePath());
        assertClickCentre(hit.get(), OPTION_X, OPTION_Y, TiantingDialogLocalMechanics.KAIDA);
    }

    @Test
    void fengyaoIsUnreachableThroughTheResidentProbe() throws IOException {
        // 使用封妖符 is only offered right after 多谢 is clicked. If the resident poll could see it, a
        // frame arriving at the wrong moment would jump straight into the coordinate branch.
        BufferedImage roi = dialogCanvas();
        paste(roi, TiantingDialogLocalMechanics.FENGYAO, OPTION_X, OPTION_Y);

        assertFalse(TiantingDialogLocalMechanics.matchResidentOption(roi).isPresent(),
                "封妖符 must not be reachable through the resident probe even when it is visible");
    }

    @Test
    void noMovementRecoveryRecognisesEveryKnownOptionWithoutWideningTheResidentProbe() throws IOException {
        for (String templatePath : List.of(
                TiantingDialogLocalMechanics.KAIDA,
                TiantingDialogLocalMechanics.DUOXIE,
                TiantingDialogLocalMechanics.ZHUOYUE,
                TiantingDialogLocalMechanics.YAOWANG,
                TiantingDialogLocalMechanics.FENGYAO,
                TiantingDialogLocalMechanics.ACCEPT,
                TiantingDialogLocalMechanics.YINYAO)) {
            BufferedImage roi = dialogCanvas();
            paste(roi, templatePath, OPTION_X, OPTION_Y);

            Optional<TiantingDialogLocalMechanics.OptionHit> hit =
                    TiantingDialogLocalMechanics.matchRecoveryOption(roi);

            assertTrue(hit.isPresent(), templatePath + " must be reachable through recovery");
            assertEquals(templatePath, hit.get().templatePath());
            assertClickCentre(hit.get(), OPTION_X, OPTION_Y, templatePath);
        }
    }

    @Test
    void acceptedCycleRecoveryCanExcludeYinyaoWithoutLosingOtherOptions() throws IOException {
        BufferedImage yinyao = dialogCanvas();
        paste(yinyao, TiantingDialogLocalMechanics.YINYAO, OPTION_X, OPTION_Y);
        assertTrue(TiantingDialogLocalMechanics.matchRecoveryOptionWithoutYinyao(yinyao).isEmpty(),
                "an accepted cycle that consumed 引妖香 must not match it again");

        BufferedImage kaida = dialogCanvas();
        paste(kaida, TiantingDialogLocalMechanics.KAIDA, OPTION_X, OPTION_Y);
        assertEquals(TiantingDialogLocalMechanics.ACTION_ENTER_BATTLE_KAIDA,
                TiantingDialogLocalMechanics.matchRecoveryOptionWithoutYinyao(kaida)
                        .orElseThrow().actionKey(),
                "excluding 引妖香 must not disable the rest of 天庭 recovery");
    }

    @Test
    void everyKnownTemplateMapsToItsStableBusinessActionKey() {
        assertEquals(TiantingDialogLocalMechanics.ACTION_ACCEPT_TASK,
                TiantingDialogLocalMechanics.actionKeyForTemplate(TiantingDialogLocalMechanics.ACCEPT).orElseThrow());
        assertEquals(TiantingDialogLocalMechanics.ACTION_ENTER_BATTLE_KAIDA,
                TiantingDialogLocalMechanics.actionKeyForTemplate(TiantingDialogLocalMechanics.KAIDA).orElseThrow());
        assertEquals(TiantingDialogLocalMechanics.ACTION_DUOXIE,
                TiantingDialogLocalMechanics.actionKeyForTemplate(TiantingDialogLocalMechanics.DUOXIE).orElseThrow());
        assertEquals(TiantingDialogLocalMechanics.ACTION_FENGYAO,
                TiantingDialogLocalMechanics.actionKeyForTemplate(TiantingDialogLocalMechanics.FENGYAO).orElseThrow());
        assertEquals(TiantingDialogLocalMechanics.ACTION_YINYAO,
                TiantingDialogLocalMechanics.actionKeyForTemplate(TiantingDialogLocalMechanics.YINYAO).orElseThrow());
        assertEquals(TiantingDialogLocalMechanics.ACTION_ENTER_BATTLE_ZHUOYUE,
                TiantingDialogLocalMechanics.actionKeyForTemplate(TiantingDialogLocalMechanics.ZHUOYUE).orElseThrow());
        assertEquals(TiantingDialogLocalMechanics.ACTION_ENTER_BATTLE_YAOWANG,
                TiantingDialogLocalMechanics.actionKeyForTemplate(TiantingDialogLocalMechanics.YAOWANG).orElseThrow());
        assertTrue(TiantingDialogLocalMechanics.actionKeyForTemplate("images/unknown.png").isEmpty());
    }

    @Test
    void aDialogCarryingOtherGreenOptionsMatchesNothing() throws IOException {
        // The cloud fallback exists for dialogs the client does not know. 引妖 and 为民除害 are real
        // 天庭 options that simply do not belong to the combat-entry poll, so they make a far better
        // negative than a drawn rectangle would.
        for (String templatePath : List.of(
                "images/template/dialog/tianting/yinyao.png",
                "images/template/dialog/tianting/accept.png")) {
            BufferedImage roi = dialogCanvas();
            paste(roi, templatePath, OPTION_X, OPTION_Y);

            assertFalse(TiantingDialogLocalMechanics.matchResidentOption(roi).isPresent(),
                    name(templatePath) + " must not be claimed by the combat-entry poll");
        }
    }

    @Test
    void aMissingFrameIsAMissRatherThanACrash() {
        assertFalse(TiantingDialogLocalMechanics.matchResidentOption(null).isPresent());
    }

    /** The matcher reports a centre point, so the click must land on the middle of the pasted option. */
    private static void assertClickCentre(TiantingDialogLocalMechanics.OptionHit hit,
                                          int pastedLeft,
                                          int pastedTop,
                                          String templatePath) throws IOException {
        BufferedImage template = ImageIO.read(new File(templatePath));
        int expectedX = pastedLeft + template.getWidth() / 2;
        int expectedY = pastedTop + template.getHeight() / 2;
        assertTrue(Math.abs(hit.roiOffsetX() - expectedX) <= 1,
                "click x " + hit.roiOffsetX() + " is not the option centre " + expectedX);
        assertTrue(Math.abs(hit.roiOffsetY() - expectedY) <= 1,
                "click y " + hit.roiOffsetY() + " is not the option centre " + expectedY);
    }

    private static String name(String templatePath) {
        return templatePath.substring(templatePath.lastIndexOf('/') + 1);
    }

    private static BufferedImage dialogCanvas() {
        BufferedImage roi = new BufferedImage(
                TiantingDialogLocalMechanics.DIALOG_ROI_WIDTH,
                TiantingDialogLocalMechanics.DIALOG_ROI_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = roi.createGraphics();
        graphics.setColor(new Color(30, 26, 22));
        graphics.fillRect(0, 0, roi.getWidth(), roi.getHeight());
        graphics.dispose();
        return roi;
    }

    private static void paste(BufferedImage roi, String templatePath, int left, int top)
            throws IOException {
        BufferedImage template = ImageIO.read(new File(templatePath));
        Graphics2D graphics = roi.createGraphics();
        graphics.drawImage(template, left, top, null);
        graphics.dispose();
    }
}
