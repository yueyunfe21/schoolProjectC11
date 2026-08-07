package com.bot.dhxy.input.action;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G006 对话框鼠标禁停区。
 *
 * <p>规则放在鼠标被<em>写</em>的地方，不是被<em>读</em>的地方：读像素的地方很多（观察面本地模板匹配、
 * 云端 OCR 上传），写鼠标的地方只有输入 worker 一个。</p>
 */
class DialogMouseNoParkZoneContractTest {

    private static final Path WORKER = Path.of(
            "src/main/java/com/bot/dhxy/input/action/InputActionWorker.java");

    private static final int WINDOW_LEFT = 1000;
    private static final int WINDOW_TOP = 500;
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;

    @Test
    void theZoneIsExactlyTheUserApprovedRectangle() {
        assertEquals(200, DialogMouseNoParkZone.ZONE_LEFT_REL_X);
        assertEquals(379, DialogMouseNoParkZone.ZONE_TOP_REL_Y);
        assertEquals(520, DialogMouseNoParkZone.ZONE_RIGHT_REL_X);
        assertEquals(488, DialogMouseNoParkZone.ZONE_BOTTOM_REL_Y);
    }

    @Test
    void theZoneTracksTheWindowRatherThanTheScreen() {
        assertTrue(DialogMouseNoParkZone.contains(
                WINDOW_LEFT + 300, WINDOW_TOP + 420, WINDOW_LEFT, WINDOW_TOP));
        // Same screen point, window moved: the rectangle moves with the window.
        assertFalse(DialogMouseNoParkZone.contains(
                WINDOW_LEFT + 300, WINDOW_TOP + 420, WINDOW_LEFT + 400, WINDOW_TOP));
    }

    @Test
    void allFourEdgesAreInsideAndOneStepBeyondEachIsOutside() {
        assertTrue(inside(200, 379));
        assertTrue(inside(520, 488));
        assertTrue(inside(520, 379));
        assertTrue(inside(200, 488));

        assertFalse(inside(199, 420));
        assertFalse(inside(521, 420));
        assertFalse(inside(300, 378));
        assertFalse(inside(300, 489));
    }

    private static boolean inside(int relX, int relY) {
        return DialogMouseNoParkZone.contains(
                WINDOW_LEFT + relX, WINDOW_TOP + relY, WINDOW_LEFT, WINDOW_TOP);
    }

    @Test
    void everyParkTargetIsInsideTheUserSpecifiedRegionAndOffEveryHoverSensitiveArea() {
        // Randomized landing point: every draw must be safe, not just the average one.
        for (int draw = 0; draw < 300; draw++) {
            Point target = DialogMouseNoParkZone.parkTarget(
                    WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, WINDOW_HEIGHT);
            int relX = target.x - WINDOW_LEFT;
            int relY = target.y - WINDOW_TOP;

            // 用户指定的停靠区：屏幕绝对 (969,463)-(1264,681)，按 base(254,23) 换算即窗口相对下矩形。
            assertTrue(relX >= 715 && relX <= 1010 && relY >= 440 && relY <= 658,
                    "落点必须在用户指定的停靠区内，实际 rel=(" + relX + "," + relY + ")");
            assertFalse(DialogMouseNoParkZone.contains(target.x, target.y, WINDOW_LEFT, WINDOW_TOP),
                    "parking inside the zone would make the sweep fire forever");

            // Task tracker search area: its entries highlight on hover and title matching then misses.
            assertFalse(relX >= 6 && relX <= 207 && relY >= 196 && relY <= 551,
                    "park target must not land on the task tracker panel");
            // Minimap, the forbidden area CloudPlayerStateIncenseStatusPort already refuses.
            assertFalse(relX >= 761 && relY <= 147,
                    "park target must not land on the minimap");
        }
    }

    /**
     * 用户合同（2026-08-02）：落点必须随机。每次停在同一个像素上是机器人指纹，反作弊一查一个准。
     */
    @Test
    void theParkTargetVariesInsteadOfBeingAFixedPixel() {
        java.util.Set<String> distinct = new java.util.HashSet<>();
        for (int draw = 0; draw < 300; draw++) {
            Point target = DialogMouseNoParkZone.parkTarget(
                    WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, WINDOW_HEIGHT);
            distinct.add(target.x + "," + target.y);
        }
        assertTrue(distinct.size() >= 50,
                "300 次抽样必须出现大量不同落点，实际只有 " + distinct.size() + " 个——固定落点是可检测特征");
    }

    @Test
    void aWindowTooSmallForTheRegionFallsBackToTheCornerInsideTheWindow() {
        int smallHeight = 400;
        for (int draw = 0; draw < 50; draw++) {
            Point target = DialogMouseNoParkZone.parkTarget(
                    WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, smallHeight);
            assertTrue(DialogMouseNoParkZone.insideWindow(
                            target, WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, smallHeight),
                    "degenerate geometry must still park inside the window");
        }
    }

    /**
     * 用户合同：扫尾移动必须落在当前窗口坐标范围内。窗口外的移动等于没移——光标停在原地、
     * 仍然压着对话框——却会打出一行"已挪走"的日志。
     */
    @Test
    void theMoveMustStayInsideTheCurrentWindow() throws Exception {
        for (int draw = 0; draw < 100; draw++) {
            Point target = DialogMouseNoParkZone.parkTarget(
                    WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, WINDOW_HEIGHT);
            assertTrue(DialogMouseNoParkZone.insideWindow(
                            target, WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, WINDOW_HEIGHT),
                    "常规几何下每一次抽样的落点都必须在窗口内");
        }

        // 边界语义：窗口右/下边界是开区间，任何贴线之外的点都算窗口外。
        assertFalse(DialogMouseNoParkZone.insideWindow(
                new Point(WINDOW_LEFT + WINDOW_WIDTH, WINDOW_TOP + 10),
                WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, WINDOW_HEIGHT));
        assertFalse(DialogMouseNoParkZone.insideWindow(
                new Point(WINDOW_LEFT - 1, WINDOW_TOP + 10),
                WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, WINDOW_HEIGHT));
        assertFalse(DialogMouseNoParkZone.insideWindow(
                null, WINDOW_LEFT, WINDOW_TOP, WINDOW_WIDTH, WINDOW_HEIGHT));

        // worker 必须在发出移动之前做窗口内校验，几何异常时跳过而不是发出一个假移动。
        String worker = Files.readString(WORKER, StandardCharsets.UTF_8);
        int sweep = worker.indexOf("private void parkPointerOutOfNoParkZone(");
        String body = worker.substring(sweep, worker.indexOf("\n    }", sweep));
        int guard = body.indexOf("DialogMouseNoParkZone.insideWindow(");
        int move = body.indexOf("inputProvider.moveMouse(");
        assertTrue(guard > 0 && guard < move,
                "窗口内校验必须发生在 moveMouse 之前：窗口外的移动是 no-op，发出去只会产生假日志");
    }

    @Test
    void theSweepIsTheTailOfTheWholeRequestNotOfEachAction() throws Exception {
        String worker = Files.readString(WORKER, StandardCharsets.UTF_8);

        int transaction = worker.indexOf("private boolean runInputTransaction(");
        int sweepCall = worker.indexOf("parkPointerOutOfNoParkZone(request)", transaction);
        assertTrue(transaction > 0, "transaction body extraction is gone; the sweep lost its single tail");
        assertTrue(sweepCall > transaction, "the sweep must be the tail of runInputTransaction");

        // moveAndClickLeft is MOVE then CLICK. A sweep inside execute() would move the click itself.
        int execute = worker.indexOf("private boolean execute(InputActionRequest request");
        String executeBody = worker.substring(execute, Math.min(worker.length(), execute + 2000));
        assertEquals(-1, executeBody.indexOf("parkPointerOutOfNoParkZone"),
                "a per-action sweep would relocate the click that follows its own move");
    }

    @Test
    void theSweepStaysOffTheCombatGatedInputPath() throws Exception {
        String worker = Files.readString(WORKER, StandardCharsets.UTF_8);
        int sweep = worker.indexOf("private void parkPointerOutOfNoParkZone(");
        assertTrue(sweep > 0);
        String body = worker.substring(sweep, worker.indexOf("\n    }", sweep));

        // Dialogs appear during combat too. Going through the turn input executor would be refused
        // as COMBAT_ACTIVE, so the sweep drives the provider inside the already-open transaction.
        assertTrue(body.contains("inputProvider.moveMouse("),
                "the sweep must drive the provider directly inside the open input transaction");
        assertEquals(-1, body.indexOf("submitMouseActions"),
                "routing the sweep through the combat-gated executor would kill it during combat");
        assertEquals(-1, body.indexOf("submitAndWait"),
                "the sweep already runs on the worker thread; re-queueing would deadlock the worker");
    }

    @Test
    void aPointerOutsideTheZoneProducesNoInputAtAll() throws Exception {
        String worker = Files.readString(WORKER, StandardCharsets.UTF_8);
        int sweep = worker.indexOf("private void parkPointerOutOfNoParkZone(");
        String body = worker.substring(sweep, worker.indexOf("\n    }", sweep));

        int guard = body.indexOf("DialogMouseNoParkZone.contains(");
        int move = body.indexOf("inputProvider.moveMouse(");
        assertTrue(guard > 0 && move > guard,
                "the zone check must gate the move: an unconditional sweep would fight the task's own mouse "
                        + "and take the global input lock on every request");
    }

    @Test
    void pureRightClickPatrolSkipsTheDialogSweepButLeftClickStillKeepsIt() throws Exception {
        String worker = Files.readString(WORKER, StandardCharsets.UTF_8);
        int sweep = worker.indexOf("private void parkPointerOutOfNoParkZone(");
        String body = worker.substring(sweep, worker.indexOf("\n    }", sweep));

        int rightClick = body.indexOf("InputActionType.CLICK_RIGHT");
        int leftClick = body.indexOf("InputActionType.CLICK_LEFT");
        int pureRightGuard = body.indexOf("hasRightClick && !hasLeftClick");
        int pointerRead = body.indexOf("MouseInfo.getPointerInfo()");
        assertTrue(rightClick > 0 && leftClick > rightClick,
                "the policy must distinguish patrol right-clicks from dialog left-clicks");
        assertTrue(pureRightGuard > leftClick && pureRightGuard < pointerRead,
                "pure right-click patrol must return before reading or moving the pointer");
    }
}
