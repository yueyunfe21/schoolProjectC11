package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Executes one Cloud-authorized Xinshou combat mechanic against one frozen exact window.
 *
 * <p>This class owns only local input, bounded waits and the local {@code auto_remaining.png}
 * template check. It has no task phase, retry loop, scheduler, Cloud decision or background thread.
 * Every public call submits exactly one frozen-window transaction through {@link InputSequences}.</p>
 */
@Component
public final class XinshouCombatLocalMechanics {

    private static final Logger log = LoggerFactory.getLogger(XinshouCombatLocalMechanics.class);

    public static final String AUTO_REMAINING_TEMPLATE_PATH =
            "images/template/battle/auto_remaining.png";
    public static final double AUTO_REMAINING_THRESHOLD = 0.80d;
    static final int PANEL_SETTLE_MS = 1_000;
    static final int RESTORE_ALT_A_GAP_MS = 1_000;
    static final int CLICK_SETTLE_MS = 80;
    static final int POST_CLICK_DELAY_MS = 250;
    private static final CombatTemplateSpec[] COMBAT_TEMPLATE_STAGES = {
            new CombatTemplateSpec(974, 630, 51, 20, 0.85d, false,
                    "images/template/battle/flag_battle.png"),
            new CombatTemplateSpec(927, 302, 100, 225, 0.80d, false,
                    "images/template/battle/zhaohuan.png",
                    "images/template/battle/chehui.png"),
            new CombatTemplateSpec(456, 62, 123, 39, 0.80d, true,
                    "images/template/battle/nu.png",
                    "images/template/battle/yuan.png")
    };

    private final ExactWindowPort exactWindowPort;

    /**
     * @param inputSequences frozen exact-window input transaction boundary
     * @param contextHolder current task window binding source
     * @param keyboard exact-HWND keyboard delivery used inside the frozen transaction
     * @param inputProvider direct mouse provider, used only while the input worker owns the transaction
     * @param tracker exact-window screenshot provider used by the local panel probe
     */
    @Autowired
    public XinshouCombatLocalMechanics(
            InputSequences inputSequences,
            WindowTaskContextHolder contextHolder,
            BoundWindowKeyboardService keyboard,
            InputProvider inputProvider,
            GameClientTracker tracker) {
        this(new InputSequencesExactWindowPort(
                inputSequences,
                contextHolder,
                keyboard,
                inputProvider,
                tracker));
    }

    XinshouCombatLocalMechanics(ExactWindowPort exactWindowPort) {
        this.exactWindowPort = Objects.requireNonNull(exactWindowPort, "exactWindowPort");
    }

    /**
     * Presses {@code Alt+A} twice in one focused exact-window transaction for an ordinary
     * Xinshou combat tick.
     *
     * @return one mechanical terminal; success is not a task or combat-state verdict
     */
    public Result pressOrdinaryAutoCombatOnce() {
        return exactWindowPort.executeFocused("xinshou:combat:ordinary-double-alt-a", session -> {
            CombatVisibility combatVisibility = session.probeCombatVisible();
            if (combatVisibility != CombatVisibility.VISIBLE) {
                return Result.failed(
                        combatVisibility == CombatVisibility.UNAVAILABLE
                                ? Status.COMBAT_STATE_UNAVAILABLE
                                : Status.COMBAT_NOT_VISIBLE,
                        null,
                        "ordinary-alt-a-gated-by-current-combat-" + combatVisibility.name().toLowerCase());
            }
            if (!session.pressAltA()) {
                return Result.failed(Status.INPUT_FAILED, null, "first-alt-a-failed");
            }
            return session.pressAltA()
                    ? Result.completed(null)
                    : Result.failed(Status.INPUT_FAILED, null, "second-alt-a-failed");
        });
    }

    /**
     * Attempts the capture-combat sequence once.
     *
     * <p>The caller supplies the source-frame screen point and source window rectangle. Inside the
     * frozen exact-window callback this method rejects size changes, translates window-only
     * movement by {@code currentOrigin - sourceOrigin}, verifies the translated point, then runs
     * {@code Alt+B -> 1000ms bounded wait -> panel absent check -> move/click}.</p>
     *
     * @param sourceScreenX screen-absolute X pixel in the source frame
     * @param sourceScreenY screen-absolute Y pixel in the source frame
     * @param sourceWindowLeft source capture window screen-absolute left
     * @param sourceWindowTop source capture window screen-absolute top
     * @param sourceWindowWidth source capture window width in pixels
     * @param sourceWindowHeight source capture window height in pixels
     * @return completed only when the panel is absent and the final click input succeeds
     */
    public Result captureCombatOnce(
            int sourceScreenX,
            int sourceScreenY,
            int sourceWindowLeft,
            int sourceWindowTop,
            int sourceWindowWidth,
            int sourceWindowHeight) {
        return exactWindowPort.executeFocused("xinshou:combat:capture", session -> {
            long sourceRight = (long) sourceWindowLeft + sourceWindowWidth;
            long sourceBottom = (long) sourceWindowTop + sourceWindowHeight;
            if (sourceWindowWidth <= 0
                    || sourceWindowHeight <= 0
                    || sourceScreenX < sourceWindowLeft
                    || sourceScreenY < sourceWindowTop
                    || sourceScreenX >= sourceRight
                    || sourceScreenY >= sourceBottom) {
                return Result.failed(
                        Status.INVALID_CLICK_POINT,
                        null,
                        "source-click-outside-source-window");
            }
            if (session.windowWidth() != sourceWindowWidth
                    || session.windowHeight() != sourceWindowHeight) {
                return Result.failed(
                        Status.WINDOW_SIZE_CHANGED,
                        null,
                        "source-window-size-changed");
            }
            long translatedX = (long) sourceScreenX
                    + session.windowLeft() - sourceWindowLeft;
            long translatedY = (long) sourceScreenY
                    + session.windowTop() - sourceWindowTop;
            if (translatedX < Integer.MIN_VALUE
                    || translatedX > Integer.MAX_VALUE
                    || translatedY < Integer.MIN_VALUE
                    || translatedY > Integer.MAX_VALUE
                    || !session.containsScreenPoint((int) translatedX, (int) translatedY)) {
                return Result.failed(
                        Status.INVALID_CLICK_POINT,
                        null,
                        "translated-click-outside-exact-window");
            }
            if (!session.pressAltB()) {
                return Result.failed(Status.INPUT_FAILED, null, "alt-b-failed");
            }
            if (!session.waitMillis(PANEL_SETTLE_MS)) {
                return Result.failed(Status.INPUT_FAILED, null, "panel-settle-interrupted");
            }
            PanelVisibility visibility = session.probeAutoRemaining();
            if (visibility == PanelVisibility.UNAVAILABLE) {
                return Result.failed(
                        Status.CAPTURE_UNAVAILABLE,
                        visibility,
                        "auto-panel-capture-unavailable");
            }
            if (visibility == PanelVisibility.VISIBLE) {
                return Result.failed(
                        Status.PANEL_STILL_VISIBLE,
                        visibility,
                        "auto-panel-still-visible");
            }
            if (!session.clickAbsolute((int) translatedX, (int) translatedY)) {
                return Result.failed(Status.INPUT_FAILED, visibility, "capture-click-failed");
            }
            return Result.completed(visibility);
        });
    }

    /**
     * Attempts the post-capture auto-combat restoration once.
     *
     * <p>The exact sequence is
     * {@code Alt+A -> 1000ms -> Alt+A -> 1000ms bounded wait -> panel visible check}.
     * A missing panel is returned to the caller; this method does not retry.</p>
     *
     * @return completed only when both inputs succeed and the panel is visible afterward
     */
    public Result restoreAutoCombatOnce() {
        return exactWindowPort.executeFocused("xinshou:combat:restore", session -> {
            if (!session.pressAltA()) {
                return Result.failed(Status.INPUT_FAILED, null, "first-alt-a-failed");
            }
            if (!session.waitMillis(RESTORE_ALT_A_GAP_MS)) {
                return Result.failed(Status.INPUT_FAILED, null, "restore-gap-interrupted");
            }
            if (!session.pressAltA()) {
                return Result.failed(Status.INPUT_FAILED, null, "second-alt-a-failed");
            }
            if (!session.waitMillis(PANEL_SETTLE_MS)) {
                return Result.failed(Status.INPUT_FAILED, null, "panel-settle-interrupted");
            }
            PanelVisibility visibility = session.probeAutoRemaining();
            if (visibility == PanelVisibility.UNAVAILABLE) {
                return Result.failed(
                        Status.CAPTURE_UNAVAILABLE,
                        visibility,
                        "auto-panel-capture-unavailable");
            }
            if (visibility == PanelVisibility.ABSENT) {
                return Result.failed(
                        Status.PANEL_NOT_VISIBLE,
                        visibility,
                        "auto-panel-not-visible");
            }
            return Result.completed(visibility);
        });
    }

    /** Result detail marking that this call physically pressed Alt+8 and verified the panel after. */
    public static final String DETAIL_ALT8_PRESSED = "alt8-pressed";

    /**
     * Maintains the auto-combat panel once as a dumb mechanic; every decision about WHEN to call
     * this lives elsewhere (local panel watcher on visibility loss, Cloud rounds ledger on command).
     *
     * <p>{@code forcePress=false}: a visible panel completes without input (watcher path — the panel
     * came back on its own or another actor repaired it first). {@code forcePress=true}: Alt+8 is
     * pressed even over a visible panel (Cloud-commanded rounds refresh). A completed result whose
     * {@link Result#detail()} is {@link #DETAIL_ALT8_PRESSED} means the key was physically sent and
     * the panel verified visible afterwards.</p>
     *
     * @param forcePress press Alt+8 even when the panel is already visible
     * @return completed when the panel is visible at the end of the transaction
     */
    public Result maintainAutoPanelOnce(boolean forcePress) {
        return exactWindowPort.executeBackground("runner:combat:auto-panel-maintenance", session -> {
            CombatVisibility combatVisibility = session.probeCombatVisible();
            if (combatVisibility != CombatVisibility.VISIBLE) {
                log.warn("[panel-verify] Alt+8 refused: combat not visible on this window: windowId={} force={} combat={}",
                        session.sessionWindowId(), forcePress, combatVisibility);
                return Result.failed(
                        combatVisibility == CombatVisibility.UNAVAILABLE
                                ? Status.COMBAT_STATE_UNAVAILABLE
                                : Status.COMBAT_NOT_VISIBLE,
                        null,
                        "auto-panel-maintenance-gated-by-current-combat-"
                                + combatVisibility.name().toLowerCase());
            }
            PanelVisibility visibility = session.probeAutoRemaining();
            if (visibility == PanelVisibility.UNAVAILABLE) {
                return Result.failed(
                        Status.CAPTURE_UNAVAILABLE,
                        visibility,
                        "auto-panel-capture-unavailable");
            }
            if (visibility == PanelVisibility.VISIBLE && !forcePress) {
                return Result.completed(visibility);
            }
            log.info("[panel-verify] pressing Alt+8: windowId={} force={} combatVisible=true panelBefore={}",
                    session.sessionWindowId(), forcePress, visibility);
            session.saveEvidenceFrame("before-alt8");
            if (!session.pressAlt8()) {
                return Result.failed(Status.INPUT_FAILED, visibility, "alt-8-failed");
            }
            if (!session.waitMillis(PANEL_SETTLE_MS)) {
                return Result.failed(Status.INPUT_FAILED, visibility, "panel-settle-interrupted");
            }
            PanelVisibility verified = session.probeAutoRemaining();
            session.saveEvidenceFrame("after-alt8");
            log.info("[panel-verify] Alt+8 pressed and settled: windowId={} force={} panelBefore={} panelAfter={}",
                    session.sessionWindowId(), forcePress, visibility, verified);
            if (verified == PanelVisibility.UNAVAILABLE) {
                return Result.failed(
                        Status.CAPTURE_UNAVAILABLE,
                        verified,
                        "auto-panel-recheck-unavailable");
            }
            return verified == PanelVisibility.VISIBLE
                    ? new Result(Status.COMPLETED, verified, DETAIL_ALT8_PRESSED)
                    : Result.failed(Status.PANEL_NOT_VISIBLE, verified, "auto-panel-not-visible-after-alt-8");
        });
    }

    public enum Status {
        COMPLETED,
        WINDOW_UNAVAILABLE,
        WINDOW_SIZE_CHANGED,
        INVALID_CLICK_POINT,
        INPUT_FAILED,
        CAPTURE_UNAVAILABLE,
        PANEL_STILL_VISIBLE,
        PANEL_NOT_VISIBLE,
        COMBAT_NOT_VISIBLE,
        COMBAT_STATE_UNAVAILABLE
    }

    public enum CombatVisibility {
        VISIBLE,
        ABSENT,
        UNAVAILABLE
    }

    public enum PanelVisibility {
        VISIBLE,
        ABSENT,
        UNAVAILABLE
    }

    public record Result(Status status, PanelVisibility observedPanel, String detail) {
        public Result {
            Objects.requireNonNull(status, "status");
        }

        static Result completed(PanelVisibility observedPanel) {
            return new Result(Status.COMPLETED, observedPanel, null);
        }

        static Result failed(Status status, PanelVisibility observedPanel, String detail) {
            return new Result(status, observedPanel, detail);
        }
    }

    @FunctionalInterface
    interface ExactWindowAction {
        Result execute(ExactWindowSession session);
    }

    interface ExactWindowPort {
        Result executeBackground(String description, ExactWindowAction action);

        Result executeFocused(String description, ExactWindowAction action);
    }

    interface ExactWindowSession {
        /** @return diagnostic window id for evidence logs; production sessions return the bound window id. */
        default String sessionWindowId() {
            return "unknown";
        }

        /** Best-effort full-window evidence screenshot; never fails the mechanic. */
        default void saveEvidenceFrame(String tag) {
        }

        /**
         * Rechecks the current exact window just before an ordinary-combat key sequence.
         * Test sessions default to visible so unrelated mechanical tests retain their existing setup.
         */
        default CombatVisibility probeCombatVisible() {
            return CombatVisibility.VISIBLE;
        }

        boolean pressAltA();

        boolean pressAltB();

        boolean pressAlt8();

        boolean waitMillis(int millis);

        PanelVisibility probeAutoRemaining();

        int windowLeft();

        int windowTop();

        int windowWidth();

        int windowHeight();

        boolean containsScreenPoint(int screenX, int screenY);

        boolean clickAbsolute(int screenX, int screenY);
    }

    private static final class InputSequencesExactWindowPort implements ExactWindowPort {

        private static final Logger log =
                LoggerFactory.getLogger(InputSequencesExactWindowPort.class);

        private final InputSequences inputSequences;
        private final WindowTaskContextHolder contextHolder;
        private final BoundWindowKeyboardService keyboard;
        private final InputProvider inputProvider;
        private final GameClientTracker tracker;

        private InputSequencesExactWindowPort(
                InputSequences inputSequences,
                WindowTaskContextHolder contextHolder,
                BoundWindowKeyboardService keyboard,
                InputProvider inputProvider,
                GameClientTracker tracker) {
            this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
            this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
            this.keyboard = Objects.requireNonNull(keyboard, "keyboard");
            this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
            this.tracker = Objects.requireNonNull(tracker, "tracker");
        }

        @Override
        public Result executeBackground(String description, ExactWindowAction action) {
            return execute(description, action, false);
        }

        @Override
        public Result executeFocused(String description, ExactWindowAction action) {
            return execute(description, action, true);
        }

        private Result execute(
                String description,
                ExactWindowAction action,
                boolean focusRequired) {
            WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
            WindowNativeBinding binding = context == null ? null : context.getNativeBinding();
            if (context == null
                    || binding == null
                    || !binding.hasNativeHandle()
                    || !binding.hasGeometry()) {
                return Result.failed(
                        Status.WINDOW_UNAVAILABLE,
                        null,
                        "exact-window-binding-unavailable");
            }

            AtomicReference<Result> mechanicalResult = new AtomicReference<>();
            try {
                Supplier<Boolean> callback = () -> {
                    try {
                        Result result = action.execute(new ProductionSession(
                                context.getWindowId(),
                                 binding,
                                 keyboard,
                                 inputProvider,
                                 tracker));
                        mechanicalResult.set(Objects.requireNonNull(
                                result,
                                "exact-window action result"));
                    } catch (RuntimeException error) {
                        log.warn(
                                "Xinshou combat local mechanic failed without retry: "
                                        + "windowId={} description={} reason={}",
                                context.getWindowId(),
                                description,
                                error.getMessage(),
                                error);
                        mechanicalResult.set(Result.failed(
                                Status.INPUT_FAILED,
                                null,
                                "mechanical-exception"));
                    }
                    // The callback itself completed and cleanup can run. The mechanical
                    // terminal remains explicit in mechanicalResult.
                    return true;
                };
                InputActionExecutionResult inputResult = focusRequired
                        ? inputSequences.submitFrozenExactWindowExclusiveAndWait(
                                description, context, binding, callback)
                        : inputSequences.submitFrozenExactWindowBackgroundExclusiveAndWait(
                                description, context, binding, callback);
                if (inputResult == null || !inputResult.isCompleted()) {
                    return Result.failed(
                            Status.INPUT_FAILED,
                            null,
                            inputResult == null
                                    ? "exact-window-input-result-missing"
                                    : inputResult.getReason());
                }
                Result result = mechanicalResult.get();
                return result == null
                        ? Result.failed(
                                Status.INPUT_FAILED,
                                null,
                                "exact-window-callback-not-run")
                        : result;
            } catch (RuntimeException error) {
                log.warn(
                        "Xinshou combat exact-window submission failed without retry: "
                                + "windowId={} description={} reason={}",
                        context.getWindowId(),
                        description,
                        error.getMessage(),
                        error);
                return Result.failed(Status.INPUT_FAILED, null, "exact-window-submit-failed");
            }
        }
    }

    private static final class ProductionSession implements ExactWindowSession {

        private final String windowId;
        private final WindowNativeBinding binding;
        private final BoundWindowKeyboardService keyboard;
        private final InputProvider inputProvider;
        private final GameClientTracker tracker;

        private ProductionSession(
                String windowId,
                WindowNativeBinding binding,
                BoundWindowKeyboardService keyboard,
                InputProvider inputProvider,
                GameClientTracker tracker) {
            this.windowId = windowId;
            this.binding = binding;
            this.keyboard = keyboard;
            this.inputProvider = inputProvider;
            this.tracker = tracker;
        }

        @Override
        public String sessionWindowId() {
            return windowId;
        }

        @Override
        public void saveEvidenceFrame(String tag) {
            BufferedImage frame = null;
            try {
                frame = tracker.captureToMemory(
                        "panel-verify-evidence",
                        binding.getX(),
                        binding.getY(),
                        binding.getX() + binding.getWidth(),
                        binding.getY() + binding.getHeight());
                if (frame == null) {
                    log.warn("[panel-verify] evidence capture unavailable: windowId={} tag={}", windowId, tag);
                    return;
                }
                java.nio.file.Path dir = Path.of("images", "temp", "panel-verify");
                java.nio.file.Files.createDirectories(dir);
                String name = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
                        + "_" + windowId + "_" + tag + ".png";
                ImageIO.write(frame, "png", dir.resolve(name).toFile());
                log.info("[panel-verify] evidence saved: windowId={} tag={} file={}", windowId, tag, name);
            } catch (IOException | RuntimeException error) {
                log.warn("[panel-verify] evidence save failed: windowId={} tag={} reason={}",
                        windowId, tag, error.toString());
            } finally {
                if (frame != null) {
                    frame.flush();
                }
            }
        }

        @Override
        public CombatVisibility probeCombatVisible() {
            for (CombatTemplateSpec stage : COMBAT_TEMPLATE_STAGES) {
                BufferedImage source = null;
                try {
                    source = tracker.captureToMemory(
                            "xinshou-combat-gate-" + stage.left(),
                            binding.getX() + stage.left(),
                            binding.getY() + stage.top(),
                            binding.getX() + stage.left() + stage.width(),
                            binding.getY() + stage.top() + stage.height());
                    if (source == null) {
                        return CombatVisibility.UNAVAILABLE;
                    }
                    boolean allMatched = true;
                    for (String templatePath : stage.templatePaths()) {
                        BufferedImage template = ImageIO.read(Path.of(templatePath).toFile());
                        if (template == null) {
                            return CombatVisibility.UNAVAILABLE;
                        }
                        try {
                            boolean matched = ImageFinder.find(source, template, stage.threshold()) != null;
                            if (matched && !stage.requireAll()) {
                                return CombatVisibility.VISIBLE;
                            }
                            if (!matched && stage.requireAll()) {
                                allMatched = false;
                                break;
                            }
                        } finally {
                            template.flush();
                        }
                    }
                    if (stage.requireAll() && allMatched) {
                        return CombatVisibility.VISIBLE;
                    }
                } catch (IOException | RuntimeException error) {
                    return CombatVisibility.UNAVAILABLE;
                } finally {
                    if (source != null) {
                        source.flush();
                    }
                }
            }
            return CombatVisibility.ABSENT;
        }

        @Override
        public boolean pressAltA() {
            try {
                inputProvider.pressAltA();
                return true;
            } catch (RuntimeException error) {
                return false;
            }
        }

        @Override
        public boolean pressAltB() {
            if (inputProvider.requiresForegroundKeyboard()) {
                try {
                    inputProvider.pressAltB();
                    return true;
                } catch (RuntimeException error) {
                    return false;
                }
            }
            BoundWindowKeyboardService.ShortcutAttempt attempt = keyboard.pressShortcut(
                    binding,
                    windowId,
                    BoundWindowKeyboardService.AltShortcut.ALT_B);
            return attempt.attempted() && attempt.success();
        }

        @Override
        public boolean pressAlt8() {
            BoundWindowKeyboardService.ShortcutAttempt attempt = keyboard.pressShortcut(
                    binding,
                    windowId,
                    BoundWindowKeyboardService.AltShortcut.ALT_8);
            return attempt.attempted() && attempt.success();
        }

        @Override
        public boolean waitMillis(int millis) {
            return TaskSleep.sleep(millis);
        }

        @Override
        public PanelVisibility probeAutoRemaining() {
            BufferedImage frame = null;
            BufferedImage template = null;
            try {
                frame = tracker.captureToMemory(
                        "xinshou-combat-auto-panel",
                        binding.getX(),
                        binding.getY(),
                        binding.getX() + binding.getWidth(),
                        binding.getY() + binding.getHeight());
                if (frame == null) {
                    return PanelVisibility.UNAVAILABLE;
                }
                template = ImageIO.read(Path.of(AUTO_REMAINING_TEMPLATE_PATH).toFile());
                if (template == null) {
                    return PanelVisibility.UNAVAILABLE;
                }
                return ImageFinder.find(frame, template, AUTO_REMAINING_THRESHOLD) == null
                        ? PanelVisibility.ABSENT
                        : PanelVisibility.VISIBLE;
            } catch (IOException error) {
                return PanelVisibility.UNAVAILABLE;
            } finally {
                if (frame != null) {
                    frame.flush();
                }
                if (template != null) {
                    template.flush();
                }
            }
        }

        @Override
        public int windowLeft() {
            return binding.getX();
        }

        @Override
        public int windowTop() {
            return binding.getY();
        }

        @Override
        public int windowWidth() {
            return binding.getWidth();
        }

        @Override
        public int windowHeight() {
            return binding.getHeight();
        }

        @Override
        public boolean containsScreenPoint(int screenX, int screenY) {
            return screenX >= binding.getX()
                    && screenY >= binding.getY()
                    && screenX < (long) binding.getX() + binding.getWidth()
                    && screenY < (long) binding.getY() + binding.getHeight();
        }

        @Override
        public boolean clickAbsolute(int screenX, int screenY) {
            try {
                inputProvider.moveMouse(screenX, screenY);
                if (!TaskSleep.sleep(CLICK_SETTLE_MS)) {
                    return false;
                }
                inputProvider.clickLeft(screenX, screenY, POST_CLICK_DELAY_MS);
                return true;
            } catch (RuntimeException error) {
                return false;
            }
        }
    }

    private record CombatTemplateSpec(
            int left,
            int top,
            int width,
            int height,
            double threshold,
            boolean requireAll,
            String... templatePaths) {
    }
}
