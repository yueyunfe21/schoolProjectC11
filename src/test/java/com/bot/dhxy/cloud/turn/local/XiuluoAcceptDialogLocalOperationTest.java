package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XiuluoAcceptDialogLocalOperationTest {

    @Test
    void matchesBaselineRoiThresholdAndAtomicClick() {
        AtomicBoolean clicked = new AtomicBoolean();
        XiuluoAcceptDialogLocalOperation operation = new XiuluoAcceptDialogLocalOperation(
                new XiuluoAcceptDialogLocalOperation.MechanicalPort() {
                    @Override
                    public int[] scaledRect(int x, int y, int width, int height) {
                        assertEquals(250, x);
                        assertEquals(312, y);
                        assertEquals(529, width);
                        assertEquals(208, height);
                        return new int[] {1000, 500, 1529, 708};
                    }

                    @Override
                    public Point findImageInRegion(String template, int[] roi, double threshold) {
                        assertEquals(XiuluoAcceptDialogLocalOperation.ACCEPT_OPTION_TEMPLATE, template);
                        assertArrayEquals(new int[] {1000, 500, 1529, 708}, roi);
                        assertEquals(0.82D, threshold);
                        return new Point(1200, 620);
                    }

                    @Override
                    public boolean moveAndClickLeft(String source, int x, int y, int settleMs, int delayMs) {
                        assertEquals("xiuluo:acceptOptionTemplate", source);
                        assertEquals(1248, x);
                        assertEquals(620, y);
                        assertEquals(150, settleMs);
                        assertEquals(650, delayMs);
                        clicked.set(true);
                        return true;
                    }
                });

        assertEquals(XiuluoAcceptDialogLocalOperation.Result.ACCEPTED, operation.execute());
        assertTrue(clicked.get());
    }

    @Test
    void templateMissDoesNotFabricateAcceptanceOrClick() {
        AtomicBoolean clicked = new AtomicBoolean();
        XiuluoAcceptDialogLocalOperation operation = new XiuluoAcceptDialogLocalOperation(
                new XiuluoAcceptDialogLocalOperation.MechanicalPort() {
                    @Override public int[] scaledRect(int x, int y, int width, int height) {
                        return new int[] {x, y, width, height};
                    }
                    @Override public Point findImageInRegion(String template, int[] roi, double threshold) {
                        return null;
                    }
                    @Override public boolean moveAndClickLeft(String source, int x, int y, int settleMs, int delayMs) {
                        clicked.set(true);
                        return true;
                    }
                });

        assertEquals(XiuluoAcceptDialogLocalOperation.Result.NOT_MATCHED, operation.execute());
        assertFalse(clicked.get());
    }
}
