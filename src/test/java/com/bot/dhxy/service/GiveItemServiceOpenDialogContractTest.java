package com.bot.dhxy.service;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.service.GiveItemService.OpenDialogGiveState;
import com.bot.dhxy.tools.CoordinateHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiveItemServiceOpenDialogContractTest {

    private static final String GIVE_ENTRY_TEMPLATE =
            "images/template/dialog/maintenance/dialog_opt_give.png";
    private static final String GIVE_BUTTON_TEMPLATE = "images/template/300huan/btn_give.png";
    private static final int[] SCALED_DIALOG_RECT = {1250, 2345, 1779, 2488};

    private List<String> events;
    private FakeCoordinateHelper coordinateHelper;
    private RecordingInputProvider inputProvider;
    private FakeBagService bagService;
    private GiveItemService service;

    @BeforeEach
    void setUp() {
        events = new ArrayList<>();
        coordinateHelper = new FakeCoordinateHelper(events);
        inputProvider = new RecordingInputProvider(events);
        bagService = new FakeBagService(events);
        service = new GiveItemService(null, inputProvider, coordinateHelper, bagService);
    }

    @Test
    void successPreservesExactOpenDialogEntrySelectAndGiveOrder() {
        OpenDialogGiveState result = runAsInputWorker(
                () -> service.executeGiveFromOpenDialogDirectForExclusive("items/shoe.png", 3));

        assertEquals(OpenDialogGiveState.GIVEN, result);
        assertEntryMatchContract();
        assertEquals(List.of(
                "scaled-rect",
                "match-give-entry",
                "randomize-give-entry",
                "click-give-entry",
                "select-item",
                "match-give-button",
                "randomize-give-button",
                "click-give-button"), events);
        assertEquals("items/shoe.png", bagService.lastTemplate);
        assertEquals(3, bagService.lastBagIndex);
        assertSame(BagService.GIVE_BAG, bagService.lastLayout);
        assertEquals(1410, inputProvider.clicks.get(0).x());
        assertEquals(2403, inputProvider.clicks.get(0).y());
        assertEquals(150, inputProvider.clicks.get(0).delayMs());
        assertEquals(1610, inputProvider.clicks.get(1).x());
        assertEquals(2504, inputProvider.clicks.get(1).y());
        assertEquals(100, inputProvider.clicks.get(1).delayMs());
    }

    @Test
    void giveEntryMissStopsBeforeInputAndBagSelection() {
        coordinateHelper.giveEntryPoint = null;

        OpenDialogGiveState result = runAsInputWorker(
                () -> service.executeGiveFromOpenDialogDirectForExclusive("items/shoe.png", null));

        assertEquals(OpenDialogGiveState.GIVE_OPTION_NOT_FOUND, result);
        assertEntryMatchContract();
        assertEquals(List.of("scaled-rect", "match-give-entry"), events);
        assertTrue(inputProvider.clicks.isEmpty());
        assertEquals(0, bagService.selectCalls);
        assertNull(coordinateHelper.lastGiveButtonTemplate);
    }

    @Test
    void directGiveFalseMapsToGiveItemFailedAndStopsBeforeFinalGiveButton() {
        bagService.selectResult = false;

        OpenDialogGiveState result = runAsInputWorker(
                () -> service.executeGiveFromOpenDialogDirectForExclusive("items/missing.png", 4));

        assertEquals(OpenDialogGiveState.GIVE_ITEM_FAILED, result);
        assertEquals(List.of(
                "scaled-rect",
                "match-give-entry",
                "randomize-give-entry",
                "click-give-entry",
                "select-item"), events);
        assertEquals(1, inputProvider.clicks.size());
        assertEquals(1, bagService.selectCalls);
        assertNull(coordinateHelper.lastGiveButtonTemplate);
    }

    @Test
    void interruptedEntryWaitStopsBeforeBagSelectionAndFinalGiveButton() {
        OpenDialogGiveState result = runInterruptedAsInputWorker(
                () -> service.executeGiveFromOpenDialogDirectForExclusive("items/shoe.png", 1));

        assertEquals(OpenDialogGiveState.INTERRUPTED, result);
        assertEquals(List.of(
                "scaled-rect",
                "match-give-entry",
                "randomize-give-entry",
                "click-give-entry"), events);
        assertEquals(1, inputProvider.clicks.size());
        assertEquals(0, bagService.selectCalls);
        assertNull(coordinateHelper.lastGiveButtonTemplate);
    }

    private void assertEntryMatchContract() {
        assertEquals(GIVE_ENTRY_TEMPLATE, coordinateHelper.lastGiveEntryTemplate);
        assertArrayEquals(SCALED_DIALOG_RECT, coordinateHelper.lastGiveEntryRect);
        assertEquals(0.85, coordinateHelper.lastGiveEntryThreshold, 0.0);
        assertArrayEquals(new int[]{250, 345, 529, 143}, coordinateHelper.lastScaledRectArguments);
    }

    private static OpenDialogGiveState runAsInputWorker(ResultCall call) {
        Thread thread = Thread.currentThread();
        String originalName = thread.getName();
        thread.setName("dhxy-input-action-worker-contract-test");
        try {
            return call.run();
        } finally {
            thread.setName(originalName);
        }
    }

    private static OpenDialogGiveState runInterruptedAsInputWorker(ResultCall call) {
        Thread thread = Thread.currentThread();
        String originalName = thread.getName();
        thread.setName("dhxy-input-action-worker-contract-test");
        thread.interrupt();
        try {
            return call.run();
        } finally {
            Thread.interrupted();
            thread.setName(originalName);
        }
    }

    @FunctionalInterface
    private interface ResultCall {
        OpenDialogGiveState run();
    }

    private static final class FakeCoordinateHelper extends CoordinateHelper {
        private final List<String> events;
        private Point giveEntryPoint = new Point(1400, 2400);
        private Point giveButtonPoint = new Point(1600, 2500);
        private String lastGiveEntryTemplate;
        private int[] lastGiveEntryRect;
        private double lastGiveEntryThreshold;
        private int[] lastScaledRectArguments;
        private String lastGiveButtonTemplate;

        private FakeCoordinateHelper(List<String> events) {
            super(null, null, null);
            this.events = events;
        }

        @Override
        public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
            events.add("scaled-rect");
            lastScaledRectArguments = new int[]{offsetX, offsetY, width, height};
            return Arrays.copyOf(SCALED_DIALOG_RECT, SCALED_DIALOG_RECT.length);
        }

        @Override
        public Point findGreenTextInRegion(String templatePath, int[] rect, double matchRate) {
            events.add("match-give-entry");
            lastGiveEntryTemplate = templatePath;
            lastGiveEntryRect = Arrays.copyOf(rect, rect.length);
            lastGiveEntryThreshold = matchRate;
            return giveEntryPoint;
        }

        @Override
        public Point findImageAbsoluteCoordinate(String templatePath, double matchRate) {
            events.add("match-give-button");
            lastGiveButtonTemplate = templatePath;
            assertEquals(GIVE_BUTTON_TEMPLATE, templatePath);
            assertEquals(0.85, matchRate, 0.0);
            return giveButtonPoint;
        }

        @Override
        public Point getRandomizedPoint(Point base, int maxRadiusX, int maxRadiusY) {
            if (maxRadiusY == 5) {
                events.add("randomize-give-entry");
                assertSame(giveEntryPoint, base);
                assertEquals(20, maxRadiusX);
                return new Point(1410, 2403);
            }
            events.add("randomize-give-button");
            assertSame(giveButtonPoint, base);
            assertEquals(20, maxRadiusX);
            assertEquals(8, maxRadiusY);
            return new Point(1610, 2504);
        }
    }

    private static final class FakeBagService extends BagService {
        private final List<String> events;
        private boolean selectResult = true;
        private int selectCalls;
        private BagLayout lastLayout;
        private String lastTemplate;
        private Integer lastBagIndex;

        private FakeBagService(List<String> events) {
            super(null, null, null, null, null, null);
            this.events = events;
        }

        @Override
        public boolean findAndSelectItemDirectForExclusive(BagLayout layout,
                                                            String targetItemTemplate,
                                                            Integer knownBagIndex) {
            events.add("select-item");
            selectCalls++;
            lastLayout = layout;
            lastTemplate = targetItemTemplate;
            lastBagIndex = knownBagIndex;
            return selectResult;
        }
    }

    private static final class RecordingInputProvider implements InputProvider {
        private final List<String> events;
        private final List<Click> clicks = new ArrayList<>();

        private RecordingInputProvider(List<String> events) {
            this.events = events;
        }

        @Override
        public void clickLeft(int x, int y, int delayMs) {
            events.add(clicks.isEmpty() ? "click-give-entry" : "click-give-button");
            clicks.add(new Click(x, y, delayMs));
        }

        @Override public void clickRight(int x, int y, int delayMs) { }
        @Override public void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) { }
        @Override public void moveMouse(int x, int y) { }
        @Override public void holdCtrl() { }
        @Override public void releaseCtrl() { }
        @Override public void pressCtrlU() { }
        @Override public void pressCtrlA() { }
        @Override public void pressAlt1() { }
        @Override public void pressAlt2() { }
        @Override public void pressAlt4() { }
        @Override public void pressAlt6() { }
        @Override public void pressAltE() { }
        @Override public void pressAltQ() { }
        @Override public void pressAltA() { }
        @Override public void pressAltC() { }
        @Override public void pressEnter() { }
        @Override public void pasteText(String text) { }
        @Override public void typeTextUnicode(String text) { }
        @Override public void scrollDown(int clicks) { }
        @Override public void pressAlt8() { }
        @Override public void pressAltT() { }
        @Override public void pressAltU() { }
        @Override public void pressAltO() { }
        @Override public void dragAndDrop(int startX, int startY, int endX, int endY) { }
        @Override public void scrollUp(int clicks) { }

        private record Click(int x, int y, int delayMs) {
        }
    }
}
