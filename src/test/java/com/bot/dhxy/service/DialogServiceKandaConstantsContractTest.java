package com.bot.dhxy.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40G §6.1/§10.1: the restored local-kanda matcher constants are byte-equivalent to the verified Git
 * {@code 59b85e0b} (CR232/CR253/CR256) implementation — exact window-relative ROI {@code (264,376)-(305,397)}
 * ({@code 41x21}), the exact unreplaced template file, and the exact {@code 0.82} threshold.
 */
class DialogServiceKandaConstantsContractTest {

    @Test
    void restoredConstantsAreByteEquivalentToTheVerifiedBaseline() {
        assertEquals("images/template/dialog/xiuluo/xiuluo_enter_battle_kanda2.png",
                DialogService.XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE);
        assertEquals(264, DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT);
        assertEquals(376, DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP);
        assertEquals(305, DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT);
        assertEquals(397, DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM);
        assertEquals(41, DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_RIGHT
                - DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_LEFT);
        assertEquals(21, DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_BOTTOM
                - DialogService.XIULUO_ENTER_BATTLE_LOCAL_ROI_TOP);
        assertEquals(0.82D, DialogService.XIULUO_ENTER_BATTLE_LOCAL_MATCH_RATE, 0.0D);
    }

    @Test
    void theExistingTemplateAssetIsPresentAndReused() {
        assertTrue(Files.exists(Path.of(DialogService.XIULUO_ENTER_BATTLE_LOCAL_TEMPLATE)),
                "the frozen kanda2 template must be the existing repository asset, never replaced");
    }

    @Test
    void jianghuLilianTemplateAlwaysFitsItsOwnLocalProbeRoi() throws Exception {
        Path templatePath = Path.of("images/template/dialog/a3/kaida.png");
        BufferedImage template = ImageIO.read(templatePath.toFile());
        assertTrue(template != null, "jianghu-lilian kaida template must decode");
        assertEquals(261, DialogService.XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_LEFT);
        assertEquals(374, DialogService.XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_TOP);
        assertEquals(186, DialogService.XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_WIDTH);
        assertEquals(27, DialogService.XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_HEIGHT);
        assertTrue(DialogService.XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_WIDTH >= template.getWidth(),
                "the probe ROI must not be narrower than kaida.png");
        assertTrue(DialogService.XINSHOU_TRAINING_ENTER_BATTLE_LOCAL_ROI_HEIGHT >= template.getHeight(),
                "the probe ROI must not be shorter than kaida.png");
    }
}
