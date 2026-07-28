package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTeamRolePreflightServiceContractTest {

    @Test
    void cr212MaskKeepsAllFourTooltipTextColorFamilies() {
        assertTrue(LocalTeamRolePreflightService.isTooltipSignaturePixel(rgb(170, 170, 170)));
        assertTrue(LocalTeamRolePreflightService.isTooltipSignaturePixel(rgb(70, 125, 70)));
        assertTrue(LocalTeamRolePreflightService.isTooltipSignaturePixel(rgb(110, 70, 125)));
        assertTrue(LocalTeamRolePreflightService.isTooltipSignaturePixel(rgb(145, 120, 80)));
        assertFalse(LocalTeamRolePreflightService.isTooltipSignaturePixel(rgb(80, 80, 80)));
    }

    @Test
    void rawAnchorHashUsesExactArgbPixelsRatherThanPngEncoding() {
        BufferedImage first = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage samePixels = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage differentPixels = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        try {
            first.setRGB(4, 7, 0xff12ab34);
            samePixels.setRGB(4, 7, 0xff12ab34);
            differentPixels.setRGB(4, 7, 0xff12ab35);

            assertEquals(LocalTeamRolePreflightService.rawArgbSha256(first),
                    LocalTeamRolePreflightService.rawArgbSha256(samePixels));
            assertNotEquals(LocalTeamRolePreflightService.rawArgbSha256(first),
                    LocalTeamRolePreflightService.rawArgbSha256(differentPixels));
        } finally {
            first.flush();
            samePixels.flush();
            differentPixels.flush();
        }
    }

    private static int rgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }
}
