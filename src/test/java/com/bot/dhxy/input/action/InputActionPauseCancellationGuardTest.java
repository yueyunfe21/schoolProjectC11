package com.bot.dhxy.input.action;

import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.runner.context.TaskExecutionContextHolder;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Focused guard for CR100 pause-aware input request cancellation.
 */
public class InputActionPauseCancellationGuardTest {

    public static void main(String[] args) throws Exception {
        String queue = source("src", "main", "java", "com", "bot", "dhxy", "input", "action",
                "InputActionQueue.java");
        String request = source("src", "main", "java", "com", "bot", "dhxy", "input", "action",
                "InputActionRequest.java");
        String worker = source("src", "main", "java", "com", "bot", "dhxy", "input", "action",
                "InputActionWorker.java");
        String scope = source("src", "main", "java", "com", "bot", "dhxy", "input", "action",
                "InputActionScope.java");

        assertSourceWiring(queue, request, worker, scope);
        verifyQueueCapturesPauseTokenForActionList();
        verifyQueueCapturesPauseTokenForExclusiveCallback();
        verifyScopeCancellationIncludesPausedToken();
        verifyWorkerAbortsRemainingActionsWhenPauseArrivesMidRequest();

        System.out.println("InputActionPauseCancellationGuardTest passed");
    }

    private static void assertSourceWiring(String queue, String request, String worker, String scope) {
        assertContains(queue, "TaskExecutionContextHolder");
        assertContains(queue, "TaskPauseToken");
        assertContains(queue, "private TaskPauseToken capturePauseToken()");
        assertContains(queue, "TaskPauseToken pauseToken = capturePauseToken();");
        assertOrder(queue, "TaskPauseToken pauseToken = capturePauseToken();",
                "new InputActionRequest(context, description, actions, pauseToken)");
        assertOrder(queue, "TaskPauseToken pauseToken = capturePauseToken();",
                "new InputActionRequest(context, description, callback, pauseToken)");

        assertContains(request, "private final TaskPauseToken pauseToken;");
        assertContains(request, "public TaskPauseToken getPauseToken()");
        assertContains(request, "public String getCancellationReason()");
        assertContains(request, "public boolean isPauseRequested()");
        assertContains(request, "pauseToken.isPauseRequested()");

        assertContains(worker, "private boolean isPauseRequested(InputActionRequest request, String stage)");
        assertOrder(worker, "isPauseRequested(request, \"before-focus\")",
                "boolean preferBackgroundKeyboard = canUseBackgroundKeyboard(request)");
        assertContains(worker, "isPauseRequested(request, \"before-actions\")");
        assertContains(worker, "isPauseRequested(request, \"before-exclusive-callback\")");
        assertContains(worker, "isPauseRequested(request, \"action-\" + actionIndex)");

        assertContains(scope, "request.isPauseRequested()");
    }

    private static void verifyQueueCapturesPauseTokenForActionList() throws Exception {
        Fixture fixture = new Fixture();
        TaskPauseToken pauseToken = new TaskPauseToken();
        InputActionQueue queue = fixture.newQueue();

        AtomicBoolean submitResult = new AtomicBoolean(false);
        Submitter submitter = fixture.submitWithTask(queue, pauseToken,
                () -> submitResult.set(queue.submitAndWait("cr100-list",
                        List.of(InputAction.moveMouse(10, 20)))));

        InputActionRequest request = queue.take();
        require(samePauseToken(request, pauseToken), "action-list request did not capture the submitting pause token");
        request.getResult().complete(true);
        submitter.join(1000L);
        require(!submitter.isAlive(), "action-list submitter did not finish");
        require(submitResult.get(), "action-list submitter should receive worker result");
    }

    private static void verifyQueueCapturesPauseTokenForExclusiveCallback() throws Exception {
        Fixture fixture = new Fixture();
        TaskPauseToken pauseToken = new TaskPauseToken();
        InputActionQueue queue = fixture.newQueue();

        AtomicBoolean submitResult = new AtomicBoolean(false);
        Submitter submitter = fixture.submitWithTask(queue, pauseToken,
                () -> submitResult.set(queue.submitExclusiveAndWait("cr100-exclusive", () -> true)));

        InputActionRequest request = queue.take();
        require(samePauseToken(request, pauseToken), "exclusive request did not capture the submitting pause token");
        request.getResult().complete(true);
        submitter.join(1000L);
        require(!submitter.isAlive(), "exclusive submitter did not finish");
        require(submitResult.get(), "exclusive submitter should receive worker result");
    }

    private static void verifyScopeCancellationIncludesPausedToken() throws Exception {
        TaskPauseToken pauseToken = new TaskPauseToken();
        pauseToken.requestPause("scope paused");
        InputActionRequest request = newRequest(null, "scope",
                List.of(InputAction.moveMouse(1, 1)), pauseToken);

        Boolean cancelled = InputActionScope.callWith(request, InputActionScope::isCancelled);

        require(Boolean.TRUE.equals(cancelled), "InputActionScope.isCancelled() must include paused request token");
    }

    private static void verifyWorkerAbortsRemainingActionsWhenPauseArrivesMidRequest() throws Exception {
        Fixture fixture = new Fixture();
        TaskPauseToken pauseToken = new TaskPauseToken();
        RecordingInputProvider inputProvider = new RecordingInputProvider(pauseToken);
        InputActionDeadLetter deadLetter = new InputActionDeadLetter();
        InputActionRequest request = newRequest(fixture.windowContext, "cr100-move-pause-click",
                List.of(
                        InputAction.moveMouse(10, 20),
                        InputAction.sleep(1),
                        InputAction.clickLeft(10, 20, 1)
                ),
                pauseToken);
        InputActionWorker worker = fixture.newWorker(inputProvider, deadLetter);

        invokeHandle(worker, request);

        require(inputProvider.events().equals(List.of("MOVE_MOUSE")),
                "pause after move must abort before sleep/click; actual events=" + inputProvider.events());
        require(Boolean.FALSE.equals(request.getResult().getNow(null)),
                "paused request should complete false");
        require("task-paused:action-2".equals(request.getCancellationReason()),
                "paused request should record the precise cancellation stage");
        require(deadLetter.snapshot().contains(request), "paused request should be dead-lettered for diagnostics");
    }

    private static InputActionRequest newRequest(WindowRuntimeContext context,
                                                 String description,
                                                 List<InputAction> actions,
                                                 TaskPauseToken pauseToken) throws Exception {
        Constructor<InputActionRequest> constructor = InputActionRequest.class.getConstructor(
                WindowRuntimeContext.class, String.class, List.class, TaskPauseToken.class);
        return constructor.newInstance(context, description, actions, pauseToken);
    }

    private static boolean samePauseToken(InputActionRequest request, TaskPauseToken pauseToken) throws Exception {
        Method method = InputActionRequest.class.getMethod("getPauseToken");
        return method.invoke(request) == pauseToken;
    }

    private static void invokeHandle(InputActionWorker worker, InputActionRequest request) throws Exception {
        Method handle = InputActionWorker.class.getDeclaredMethod("handle", InputActionRequest.class);
        handle.setAccessible(true);
        handle.invoke(worker, request);
    }

    private static String source(String first, String... more) throws Exception {
        return Files.readString(Path.of(first, more), StandardCharsets.UTF_8);
    }

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }

    private static void assertOrder(String value, String firstToken, String secondToken) {
        int first = value.indexOf(firstToken);
        int second = value.indexOf(secondToken);
        if (first < 0 || second < 0 || first >= second) {
            throw new AssertionError("Expected token order missing: " + firstToken + " before " + secondToken);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class Fixture {
        private final WindowIsolationProperties properties = new WindowIsolationProperties();
        private final WindowTaskContextHolder windowHolder = new WindowTaskContextHolder(properties);
        private final TaskExecutionContextHolder taskHolder = new TaskExecutionContextHolder();
        private final StableBindingRefreshService refreshService = new StableBindingRefreshService();
        private final WindowRuntimeContext windowContext = newWindowContext();

        private InputActionQueue newQueue() throws Exception {
            Constructor<InputActionQueue> constructor = InputActionQueue.class.getConstructor(
                    WindowTaskContextHolder.class,
                    WindowNativeBindingRefreshService.class,
                    TaskExecutionContextHolder.class);
            return constructor.newInstance(windowHolder, refreshService, taskHolder);
        }

        private InputActionWorker newWorker(InputProvider inputProvider, InputActionDeadLetter deadLetter)
                throws Exception {
            WindowInteractionMetricsService metricsService = new WindowInteractionMetricsService();
            GlobalInputLock inputLock = new GlobalInputLock();
            WindowAwareInputCoordinator coordinator = new WindowAwareInputCoordinator(
                    inputLock,
                    windowHolder,
                    new WindowFocusService(inputLock),
                    properties,
                    metricsService,
                    refreshService);
            BoundWindowKeyboardService keyboardService = new BoundWindowKeyboardService(
                    windowHolder,
                    properties,
                    metricsService,
                    refreshService);
            return new InputActionWorker(newQueue(), deadLetter, inputProvider, coordinator, windowHolder,
                    keyboardService);
        }

        private Submitter submitWithTask(InputActionQueue queue, TaskPauseToken pauseToken, Runnable action) {
            TaskExecutionContext taskContext = TaskExecutionContext.builder()
                    .taskCode("cr100")
                    .taskName("CR100")
                    .windowId(windowContext.getWindowId())
                    .pauseToken(pauseToken)
                    .build();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    windowHolder.runWith(windowContext, () -> taskHolder.callWith(taskContext, () -> {
                        action.run();
                        return true;
                    }));
                } catch (Throwable e) {
                    failure.set(e);
                }
            }, "cr100-submit-test");
            thread.start();
            return new Submitter(thread, failure);
        }

        private WindowRuntimeContext newWindowContext() {
            WindowRuntimeContext context = new WindowRuntimeContext("cr100-window", new GameContext());
            context.setNativeBinding(new WindowNativeBinding("1", "cr100", "game", 100L, 0, 0, 800, 600));
            return context;
        }
    }

    private record Submitter(Thread thread, AtomicReference<Throwable> failure) {
        private void join(long millis) throws InterruptedException {
            thread.join(millis);
            if (failure.get() != null) {
                throw new AssertionError("submitter failed", failure.get());
            }
        }

        private boolean isAlive() {
            return thread.isAlive();
        }
    }

    private static class StableBindingRefreshService extends WindowNativeBindingRefreshService {
        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
            if (context == null || context.getNativeBinding() == null || !context.getNativeBinding().hasNativeHandle()) {
                return Optional.empty();
            }
            return Optional.of(context.getNativeBinding());
        }
    }

    private static class RecordingInputProvider implements InputProvider {
        private final TaskPauseToken pauseToken;
        private final List<String> events = new ArrayList<>();

        private RecordingInputProvider(TaskPauseToken pauseToken) {
            this.pauseToken = pauseToken;
        }

        private List<String> events() {
            return List.copyOf(events);
        }

        @Override
        public void clickLeft(int x, int y, int delayMs) {
            events.add("CLICK_LEFT");
        }

        @Override
        public void clickRight(int x, int y, int delayMs) {
            events.add("CLICK_RIGHT");
        }

        @Override
        public void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) {
            events.add("DOUBLE_RIGHT_CLICK");
        }

        @Override
        public void moveMouse(int x, int y) {
            events.add("MOVE_MOUSE");
            pauseToken.requestPause("pause after move");
        }

        @Override
        public void holdCtrl() {
            events.add("HOLD_CTRL");
        }

        @Override
        public void releaseCtrl() {
            events.add("RELEASE_CTRL");
        }

        @Override
        public void pressCtrlU() {
            events.add("PRESS_CTRL_U");
        }

        @Override
        public void pressCtrlA() {
            events.add("PRESS_CTRL_A");
        }

        @Override
        public void pressAlt1() {
            events.add("PRESS_ALT_1");
        }

        @Override
        public void pressAlt2() {
            events.add("PRESS_ALT_2");
        }

        @Override
        public void pressAlt4() {
            events.add("PRESS_ALT_4");
        }

        @Override
        public void pressAlt6() {
            events.add("PRESS_ALT_6");
        }

        @Override
        public void pressAltE() {
            events.add("PRESS_ALT_E");
        }

        @Override
        public void pressAltQ() {
            events.add("PRESS_ALT_Q");
        }

        @Override
        public void pressAltA() {
            events.add("PRESS_ALT_A");
        }

        @Override
        public void pressAltC() {
            events.add("PRESS_ALT_C");
        }

        @Override
        public void pressEnter() {
            events.add("PRESS_ENTER");
        }

        @Override
        public void pasteText(String text) {
            events.add("PASTE_TEXT");
        }

        @Override
        public void typeTextUnicode(String text) {
            events.add("TYPE_TEXT_UNICODE");
        }

        @Override
        public void scrollDown(int clicks) {
            events.add("SCROLL_DOWN");
        }

        @Override
        public void pressAlt8() {
            events.add("PRESS_ALT_8");
        }

        @Override
        public void pressAltT() {
            events.add("PRESS_ALT_T");
        }

        @Override
        public void pressAltU() {
            events.add("PRESS_ALT_U");
        }

        @Override
        public void pressAltO() {
            events.add("PRESS_ALT_O");
        }

        @Override
        public void dragAndDrop(int startX, int startY, int endX, int endY) {
            events.add("DRAG_AND_DROP");
        }

        @Override
        public void scrollUp(int clicks) {
            events.add("SCROLL_UP");
        }
    }
}
