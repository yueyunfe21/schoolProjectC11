package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalAction;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionType;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouTitleMechanicalExecutorTest {

    @Test
    void adoptionExecutesTheValidatedTwoClickGeometryInOneAtomicSubmission() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.regionMatches.put("images/template/xinshou/lingyang.png", new Point(640, 430));
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.CONFIRM_ADOPTION);

        assertTrue(result.completed());
        assertEquals(1, port.regionFindCount("images/template/xinshou/lingyang.png"));
        assertEquals(1, port.inputSubmissions.size());
        List<InputAction> actions = port.inputSubmissions.getFirst();
        assertEquals(List.of(
                        InputActionType.MOVE_MOUSE,
                        InputActionType.SLEEP,
                        InputActionType.CLICK_LEFT,
                        InputActionType.MOVE_MOUSE,
                        InputActionType.SLEEP,
                        InputActionType.CLICK_LEFT),
                actions.stream().map(InputAction::getType).toList());
        assertEquals(610, actions.get(0).getX());
        assertEquals(230, actions.get(0).getY());
        assertEquals(610, actions.get(2).getX());
        assertEquals(230, actions.get(2).getY());
        assertEquals(640, actions.get(3).getX());
        assertEquals(430, actions.get(3).getY());
        assertEquals(640, actions.get(5).getX());
        assertEquals(430, actions.get(5).getY());
        assertTrue(port.clicks.isEmpty(), "adoption must stay one atomic queued sequence");
    }

    @Test
    void adoptionTemplateMissReturnsFailureWithoutInputOrRetry() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.CONFIRM_ADOPTION);

        assertFalse(result.completed());
        assertEquals(XinshouTitleMechanicalExecutor.ResultCode.ADOPTION_TARGET_NOT_FOUND, result.code());
        assertEquals(1, port.regionFindCount("images/template/xinshou/lingyang.png"));
        assertTrue(port.inputSubmissions.isEmpty());
        assertTrue(port.clicks.isEmpty());
    }

    @Test
    void upgradeUsesOneItemAndCallsGenericCleanupOnce() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/shengji.png", true);
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS);

        assertTrue(result.completed());
        assertEquals(List.of("xinshou/shengji.png"), port.usedItems);
        assertEquals(1, port.cleanupCalls);
    }

    @Test
    void failedUpgradeDoesNotRetryOrContinueToCleanup() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/shengji.png", false);
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS);

        assertFalse(result.completed());
        assertEquals(XinshouTitleMechanicalExecutor.ResultCode.ITEM_NOT_USED, result.code());
        assertEquals(List.of("xinshou/shengji.png"), port.usedItems);
        assertEquals(0, port.cleanupCalls);
    }

    @Test
    void shellAndMaterialsEachRunOnePreparedMechanicalSequence() {
        FakeMechanicalPort shellPort = new FakeMechanicalPort();
        shellPort.itemUseResult.put("xinshou/hailuo.png", true);
        shellPort.regionMatches.put("images/template/xinshou/chuixiang.png", new Point(630, 520));
        XinshouTitleMechanicalExecutor shellExecutor = new XinshouTitleMechanicalExecutor(shellPort);

        assertTrue(shellExecutor.execute(
                TurnXinshouMechanicalAction.USE_SHELL_AND_BLOW).completed());
        assertEquals(List.of("xinshou/hailuo.png"), shellPort.usedItems);
        assertEquals(List.of(new Click(630, 520)), shellPort.clicks);
        assertEquals(1, shellPort.regionFindCount("images/template/xinshou/chuixiang.png"));

        FakeMechanicalPort materialPort = new FakeMechanicalPort();
        materialPort.regionMatches.put("images/template/xinshou/wuzi.png", new Point(610, 510));
        XinshouTitleMechanicalExecutor materialExecutor = new XinshouTitleMechanicalExecutor(materialPort);

        assertTrue(materialExecutor.execute(
                TurnXinshouMechanicalAction.HAND_IN_MATERIALS).completed());
        assertEquals(List.of(new Click(610, 510)), materialPort.clicks);
        assertEquals(1, materialPort.giveCalls);
        assertEquals(1, materialPort.regionFindCount("images/template/xinshou/wuzi.png"));
    }

    @Test
    void dialogTargetInputFailureIsNotMisreportedAsAMissingTarget() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/hailuo.png", true);
        port.regionMatches.put("images/template/xinshou/chuixiang.png", new Point(630, 520));
        port.moveAndClickResult = false;
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.USE_SHELL_AND_BLOW);

        assertFalse(result.completed());
        assertEquals(XinshouTitleMechanicalExecutor.ResultCode.INPUT_FAILED, result.code());
        assertEquals(List.of("xinshou/hailuo.png"), port.usedItems);
        assertEquals(List.of(new Click(630, 520)), port.clicks);
    }

    @Test
    void repairFirstPassSuccessNeverReselectsAnItem() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/xiufu_item.png", true);
        port.absoluteMatches.put("images/template/xinshou/xiufu_opened.png", new Point(1, 1));
        port.absoluteMatches.put("images/template/xinshou/xiufu_fangru_suc.png", new Point(1, 1));
        port.absoluteMatches.put("images/template/xinshou/xiufu_alldone.png", new Point(1, 1));
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.REPAIR_ITEMS_ONCE);

        assertTrue(result.completed());
        assertEquals(XinshouTitleMechanicalExecutor.ResultCode.REPAIR_DONE_VISIBLE, result.code());
        assertEquals(List.of("xinshou/xiufu_item.png"), port.usedItems);
        assertEquals(6, port.clicks.size(), "three items plus one successful slot per item");
        assertEquals(3, port.absoluteFindCount("images/template/xinshou/xiufu_fangru_suc.png"));
        assertEquals(1, port.absoluteFindCount("images/template/xinshou/xiufu_alldone.png"));
    }

    @Test
    void repairReselectsOnlyAfterTheFirstThreeSlotsAllMiss() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/xiufu_item.png", true);
        port.absoluteMatches.put("images/template/xinshou/xiufu_opened.png", new Point(1, 1));
        port.absoluteMatchSequence.put(
                "images/template/xinshou/xiufu_fangru_suc.png",
                new ArrayDeque<>(List.of(
                        Match.miss(), Match.miss(), Match.miss(),
                        Match.hit(), Match.hit(), Match.hit())));
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.REPAIR_ITEMS_ONCE);

        assertTrue(result.completed());
        assertEquals(XinshouTitleMechanicalExecutor.ResultCode.REPAIR_ITEMS_PLACED, result.code());
        assertEquals(10, port.clicks.size(),
                "only the first item is reselected after its complete first-pass miss");
        assertEquals(2, port.clicks.stream().filter(new Click(491, 604)::equals).count());
        assertEquals(1, port.clicks.stream().filter(new Click(542, 595)::equals).count());
        assertEquals(1, port.clicks.stream().filter(new Click(595, 581)::equals).count());
        assertEquals(6, port.absoluteFindCount("images/template/xinshou/xiufu_fangru_suc.png"));
    }

    @Test
    void failedRepairItemStopsAfterExactlyTwoCompletePasses() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/xiufu_item.png", true);
        port.absoluteMatches.put("images/template/xinshou/xiufu_opened.png", new Point(1, 1));
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.REPAIR_ITEMS_ONCE);

        assertFalse(result.completed());
        assertEquals(XinshouTitleMechanicalExecutor.ResultCode.REPAIR_ITEM_NOT_PLACED, result.code());
        assertEquals(List.of("xinshou/xiufu_item.png"), port.usedItems);
        assertEquals(8, port.clicks.size(), "two item selections plus two fixed three-slot passes");
        assertEquals(6, port.absoluteFindCount("images/template/xinshou/xiufu_fangru_suc.png"));
        assertEquals(0, port.absoluteFindCount("images/template/xinshou/xiufu_alldone.png"));
    }

    @Test
    void closeRepairWindowIsExactlyOnePreparedClick() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.CLOSE_REPAIR_WINDOW);

        assertTrue(result.completed());
        assertEquals(List.of(new Click(672, 220)), port.clicks);
        assertTrue(port.inputSubmissions.isEmpty());
    }

    @Test
    void lunhuiUsesAndVerifiesOnceBeforeTheFixedDoubleStartClick() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/lunhui_item.png", true);
        port.regionMatches.put("images/template/xinshou/shengsi.png", new Point(1, 1));
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.USE_LUNHUI_ITEM_AND_START);

        assertTrue(result.completed());
        assertEquals(List.of("xinshou/lunhui_item.png"), port.usedItems);
        assertEquals(1, port.regionFindCount("images/template/xinshou/shengsi.png"));
        assertEquals(List.of(new Click(785, 469), new Click(785, 469)), port.clicks);
        assertEquals(List.of(1_000L), port.sleeps);
    }

    @Test
    void lunhuiVerificationFailureDoesNotUseTheItemAgain() {
        FakeMechanicalPort port = new FakeMechanicalPort();
        port.itemUseResult.put("xinshou/lunhui_item.png", true);
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        XinshouTitleMechanicalExecutor.ExecutionResult result = executor.execute(
                TurnXinshouMechanicalAction.USE_LUNHUI_ITEM_AND_START);

        assertFalse(result.completed());
        assertEquals(XinshouTitleMechanicalExecutor.ResultCode.LUNHUI_VERIFY_NOT_VISIBLE, result.code());
        assertEquals(List.of("xinshou/lunhui_item.png"), port.usedItems);
        assertTrue(port.clicks.isEmpty());
        assertTrue(port.sleeps.isEmpty());
    }

    @Test
    void constructionWithoutInvocationProducesNoInputAndOwnsNoWorker() throws IOException {
        FakeMechanicalPort port = new FakeMechanicalPort();
        XinshouTitleMechanicalExecutor executor = new XinshouTitleMechanicalExecutor(port);

        assertNotNull(executor);
        assertTrue(port.inputSubmissions.isEmpty());
        assertTrue(port.clicks.isEmpty());
        assertTrue(port.usedItems.isEmpty());

        assertTrue(XinshouTitleMechanicalExecutor.class.isAnnotationPresent(Component.class));
        for (Field field : XinshouTitleMechanicalExecutor.class.getDeclaredFields()) {
            assertFalse(Thread.class.isAssignableFrom(field.getType()));
            assertFalse(Executor.class.isAssignableFrom(field.getType()));
        }

        String executorSource = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/XinshouTitleMechanicalExecutor.java"));
        assertFalse(executorSource.contains("new Thread"));
        assertFalse(executorSource.contains("Executors."));
        assertFalse(executorSource.contains("@Async"));
    }

    @Test
    void observationSamplerAndFactoriesDoNotReferenceThePassiveExecutor() throws IOException {
        for (String source : List.of(
                "src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java",
                "src/main/java/com/bot/dhxy/window/observation/WindowObservationRunner.java",
                "src/main/java/com/bot/dhxy/window/observation/WindowObservationRunnerFactory.java",
                "src/main/java/com/bot/dhxy/window/observation/SpringObservationRunnerFactory.java")) {
            assertFalse(Files.readString(Path.of(source)).contains("XinshouTitleMechanicalExecutor"),
                    () -> source + " must not wire title mechanics");
        }
    }

    private record Click(int x, int y) {
    }

    private static final class FakeMechanicalPort
            implements XinshouTitleMechanicalExecutor.MechanicalPort {
        private final List<List<InputAction>> inputSubmissions = new ArrayList<>();
        private final List<String> usedItems = new ArrayList<>();
        private final Map<String, Boolean> itemUseResult = new HashMap<>();
        private final Map<String, Point> regionMatches = new HashMap<>();
        private final Map<String, Point> absoluteMatches = new HashMap<>();
        private final Map<String, Deque<Match>> absoluteMatchSequence = new HashMap<>();
        private final Map<String, Integer> regionFindCounts = new HashMap<>();
        private final Map<String, Integer> absoluteFindCounts = new HashMap<>();
        private final List<Click> clicks = new ArrayList<>();
        private final List<Long> sleeps = new ArrayList<>();
        private boolean moveAndClickResult = true;
        private int cleanupCalls;
        private int giveCalls;

        @Override
        public boolean submitInput(String description, List<InputAction> actions) {
            inputSubmissions.add(List.copyOf(actions));
            return true;
        }

        @Override
        public boolean useFirstTabItem(String source, String template) {
            usedItems.add(template);
            return itemUseResult.getOrDefault(template, false);
        }

        @Override
        public void closeAllGenericWindows() {
            cleanupCalls++;
        }

        @Override
        public boolean clickGiveButtonAfterLocalSelection() {
            giveCalls++;
            return true;
        }

        @Override
        public int[] scaledRect(int relativeX, int relativeY, int width, int height) {
            return new int[]{relativeX, relativeY, width, height};
        }

        @Override
        public Point findImageInRegion(String template, int[] roi, double matchRate) {
            regionFindCounts.merge(template, 1, Integer::sum);
            return regionMatches.get(template);
        }

        @Override
        public Point findImageAbsoluteCoordinate(String template, double matchRate) {
            absoluteFindCounts.merge(template, 1, Integer::sum);
            Deque<Match> scripted = absoluteMatchSequence.get(template);
            if (scripted != null && !scripted.isEmpty()) {
                return scripted.removeFirst().point();
            }
            return absoluteMatches.get(template);
        }

        @Override
        public boolean moveAndClickLeft(
                String description, int screenX, int screenY, int settleMs, int delayMs) {
            clicks.add(new Click(screenX, screenY));
            return moveAndClickResult;
        }

        @Override
        public boolean sleep(long millis) {
            sleeps.add(millis);
            return true;
        }

        private int regionFindCount(String template) {
            return regionFindCounts.getOrDefault(template, 0);
        }

        private int absoluteFindCount(String template) {
            return absoluteFindCounts.getOrDefault(template, 0);
        }
    }

    private record Match(Point point) {
        private static Match hit() {
            return new Match(new Point(1, 1));
        }

        private static Match miss() {
            return new Match(null);
        }
    }
}
