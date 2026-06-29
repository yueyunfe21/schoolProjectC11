package com.bot.dhxy.service;

import java.awt.Point;

public class TaskTrackerExpandedAnchorGeometryTest {

    public static void main(String[] args) {
        Point actual = TaskTrackerPanelService.expandedVisionAnchorToScreenAnchor(
                new Point(102, 201), 398, 255);
        assertPoint("expanded anchor must add bound window base",
                new Point(500, 456), actual);
    }

    private static void assertPoint(String caseName, Point expected, Point actual) {
        if (actual == null || actual.x != expected.x || actual.y != expected.y) {
            throw new AssertionError(caseName + ": expected=" + expected + " actual=" + actual);
        }
    }
}
