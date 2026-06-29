package com.bot.dhxy.service;

import com.bot.dhxy.model.ocr.OcrWindowRegion;

import java.awt.Point;

public class NpcClickTooltipGeometryTest {

    public static void main(String[] args) {
        assertPoint("direct NPC point keeps tooltip X and adds 90 Y",
                new Point(600, 490),
                NpcClickService.directNpcPointFromTooltipCenter(new Point(600, 400)));

        OcrWindowRegion roi = NpcClickService.tooltipLearnedRoiFromTooltipCenter(
                new Point(600, 400),
                new Point(100, 50));
        assertRegion("tooltip ROI converts to window-relative rectangle",
                350, 250, 650, 550, roi);

        OcrWindowRegion clamped = NpcClickService.tooltipLearnedRoiFromTooltipCenter(
                new Point(110, 80),
                new Point(100, 50));
        assertRegion("tooltip ROI clamps to game window",
                0, 0, 160, 230, clamped);
    }

    private static void assertPoint(String caseName, Point expected, Point actual) {
        if (actual == null || actual.x != expected.x || actual.y != expected.y) {
            throw new AssertionError(caseName + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertRegion(String caseName,
                                     int x1,
                                     int y1,
                                     int x2,
                                     int y2,
                                     OcrWindowRegion actual) {
        if (actual == null
                || actual.x1() != x1
                || actual.y1() != y1
                || actual.x2() != x2
                || actual.y2() != y2) {
            String actualText = actual == null ? "null" : actual.toShortText();
            throw new AssertionError(caseName + ": expected=" + x1 + "," + y1
                    + " -> " + x2 + "," + y2 + " actual=" + actualText);
        }
    }
}
