package com.bot.dhxy.window.execution;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.service.LeftTopStatusSwitchService;
import com.bot.dhxy.service.PlayerStateService;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.startup.TaskStartupWindowPreparationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default per-window startup preparation before a task begins.
 *
 * <p>Every submitted task first synchronizes the bound window's player identity and current
 * position. Only leader/main-task windows then run the heavier UI preparation: map tracking option
 * check and Alt+5/Alt+6 visibility checks. Member windows and debug/auto-battle tasks skip the hotkey/UI
 * work so they do not focus the game client or press unnecessary keys before quietly waiting for
 * combat.</p>
 */
@Slf4j
@Component
public class DefaultWindowTaskStartupInitializer implements WindowTaskStartupInitializer {

    private final TaskStartupWindowPreparationService startupWindowPreparationService;
    private final PlayerStateService playerStateService;
    private final BotProperties botProperties;
    private final LeftTopStatusSwitchService leftTopStatusSwitchService;

    /**
     * @param startupWindowPreparationService service that owns startup map/Alt+5/Alt+6 preparation.
     * @param playerStateService player-state service used to synchronize identity and position into
     *                           the current window-bound {@code GameContext.State}.
     * @param botProperties runtime switches that decide whether heavy startup preparation is enabled.
     * @param leftTopStatusSwitchService left-top status switch guard for task startup/maintenance.
     */
    public DefaultWindowTaskStartupInitializer(TaskStartupWindowPreparationService startupWindowPreparationService,
                                               PlayerStateService playerStateService,
                                               BotProperties botProperties,
                                               LeftTopStatusSwitchService leftTopStatusSwitchService) {
        this.startupWindowPreparationService = startupWindowPreparationService;
        this.playerStateService = playerStateService;
        this.botProperties = botProperties;
        this.leftTopStatusSwitchService = leftTopStatusSwitchService;
    }

    /**
     * Run startup preparation for the given window when policy allows it.
     *
     * @param windowContext registered window runtime context; may be null in legacy paths.
     * @param executionContext task execution context; may be null before a task is fully attached.
     * @return true unless the thread is interrupted. A failed map-tracking confirmation is logged as
     * a warning but does not hard-stop the task because foreground/focus APIs can be best-effort.
     */
    @Override
    public boolean beforeTask(WindowRuntimeContext windowContext, TaskExecutionContext executionContext) {
        String prefix = executionContext == null ? "window-startup" : executionContext.getLogPrefix();
        String windowId = windowContext == null ? "-" : windowContext.getWindowId();
        String taskCode = executionContext == null ? null : executionContext.getTaskCode();
        if ("debug_navigation_stress".equals(taskCode)) {
            /*
             * The navigation stress task measures turn handoff latency. A full startup position OCR
             * can spend several seconds before the window even asks for the task turn, which hides
             * the latency being tested. Navigation itself still performs its own map checks.
             */
            log.info("{} window [{}] startup sync: identity only for navigation stress; position deferred", prefix, windowId);
            playerStateService.syncMyIdentity();
            return !Thread.currentThread().isInterrupted();
        }
        /*
         * This is the common UI-start path. Keep identity/position sync here instead of inside each
         * task so Five Ring, Xiuluo, future team tasks, and member auto-battle windows all start from
         * a complete per-window state snapshot.
         */
        log.info("{} window [{}] startup sync: identity and position", prefix, windowId);
        playerStateService.syncAll();
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }

        if (executionContext != null
                && executionContext.isCleanQueueTransitionStartup()
                && windowContext != null
                && windowContext.isTaskQueueStartupPreparationDone()) {
            log.info("{} window [{}] startup init skipped: clean queued task transition reused common startup preparation taskCode={}",
                    prefix, windowId, taskCode);
            return true;
        }

        if (isFiveRingTask(taskCode)
                && windowContext != null
                && windowContext.isTaskQueueStartupPreparationDone(taskCode)) {
            log.info("{} window [{}] startup init skipped: five-ring queue startup preparation already completed taskCode={}",
                    prefix, windowId, taskCode);
            return true;
        }

        if (isFiveRingTask(taskCode)) {
            /*
             * 五环多窗口启动必须保持后台探测语义：这里最多用 HWND 截图记录 pending，
             * 不在 startup initializer 里为每个窗口串行抢前台点左上角开关。
             */
            leftTopStatusSwitchService.probeMemberStartup(executionContext, requestedTaskCode(executionContext, taskCode));
        } else if (isMemberWindow(windowContext, executionContext)) {
            leftTopStatusSwitchService.probeMemberStartup(executionContext, requestedTaskCode(executionContext, taskCode));
        } else {
            leftTopStatusSwitchService.handleLeaderStartup(executionContext, taskCode);
        }
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }

        if (executionContext != null && isStartupInitSkippedTask(executionContext.getTaskCode())) {
            log.info("{} window [{}] startup init skipped for debug task: {}", prefix, windowId, executionContext.getTaskCode());
            return !Thread.currentThread().isInterrupted();
        }
        if (isFiveRingTask(taskCode)) {
            /*
             * 五环五开启动仍要做完整启动 UI 检查，但正常路径必须是后台 HWND probe：
             * Alt+1/Alt+U 打开面板、截图判断；只有明确发现需要点击修正时才进前台事务。
             */
            log.info("{} window [{}] startup init: five-ring run background-first startup preparation taskCode={}",
                    prefix, windowId, taskCode);
            boolean ready = startupWindowPreparationService.prepareTaskStartupWindowBackgroundFirst();
            if (ready && windowContext != null && !Thread.currentThread().isInterrupted()) {
                log.info("{} window [{}] startup init: five-ring mark queue startup preparation done taskCode={} ready={}",
                        prefix, windowId, taskCode, ready);
                windowContext.markTaskQueueStartupPreparationDone(taskCode);
            }
            if (!ready) {
                log.warn("{} window [{}] startup init warning: five-ring background-first startup preparation was not fully confirmed",
                        prefix, windowId);
            }
            return !Thread.currentThread().isInterrupted();
        }
        if (isMemberWindow(windowContext, executionContext)) {
            log.info("{} window [{}] startup init skipped for member/auto-battle window: taskCode={} role={}",
                    prefix, windowId, executionContext == null ? "-" : executionContext.getTaskCode(),
                    executionContext == null ? "-" : executionContext.getWindowRole());
            return !Thread.currentThread().isInterrupted();
        }
        if (isWubeiTask(taskCode)) {
            /*
             * 五倍 leader also depends on hidden stalls/player overlays for task/NPC screenshots. When the
             * full startup preparation is enabled, prepareTaskStartupWindow() below already owns the
             * Alt+5/Alt+6 checks. Run the narrow guard only as the fast-debug fallback when the full
             * preparation chain is disabled.
             */
            if (!botProperties.isTaskStartupPreparationEnabled()) {
                log.info("{} window [{}] startup init: wubei ensure Alt+5/Alt+6 visibility only because full preparation is disabled taskCode={}",
                        prefix, windowId, taskCode);
                boolean visibilityReady = startupWindowPreparationService.ensureStartupVisibilityOverlays();
                if (!visibilityReady) {
                    log.warn("{} window [{}] startup init warning: wubei startup visibility overlays were not confirmed",
                            prefix, windowId);
                }
            } else {
                log.info("{} window [{}] startup init: wubei full preparation will perform Alt+5/Alt+6 visibility checks taskCode={}",
                        prefix, windowId, taskCode);
            }
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
        }

        log.info("{} window [{}] startup init: leader ensure map tracking option and Alt+5/Alt+6 visibility", prefix, windowId);
        boolean ready = startupWindowPreparationService.prepareTaskStartupWindow();
        if (!ready) {
            log.warn("{} window [{}] startup init warning: map tracking option was not confirmed", prefix, windowId);
        }
        if (ready && windowContext != null && !Thread.currentThread().isInterrupted()) {
            log.info("{} window [{}] startup init: mark queue common startup preparation done taskCode={} ready={}",
                    prefix, windowId, taskCode, ready);
            windowContext.markTaskQueueStartupPreparationDone(taskCode);
        }
        return !Thread.currentThread().isInterrupted();
    }

    /**
     * @return true for tasks that either do not need startup UI preparation or are explicit debug paths.
     */
    private boolean isStartupInitSkippedTask(String taskCode) {
        return "auto_battle".equals(taskCode)
                || "debug_coordinate".equals(taskCode)
                || "debug_map_calibrator".equals(taskCode)
                || "debug_team_role".equals(taskCode)
                || "debug_xiuluo_story_objective".equals(taskCode)
                || "debug_xiuluo_task_panel_objective".equals(taskCode);
    }

    private boolean isFiveRingTask(String taskCode) {
        return "wuhuan_v2".equals(taskCode);
    }

    private boolean isWubeiTask(String taskCode) {
        return "wubei".equals(taskCode);
    }

    private String requestedTaskCode(TaskExecutionContext executionContext, String fallbackTaskCode) {
        if (executionContext != null
                && executionContext.getRequestedTaskCode() != null
                && !executionContext.getRequestedTaskCode().isBlank()) {
            return executionContext.getRequestedTaskCode();
        }
        return fallbackTaskCode;
    }

    /**
     * @return true when the current window should behave as a quiet member/auto-battle window.
     */
    private boolean isMemberWindow(WindowRuntimeContext windowContext, TaskExecutionContext executionContext) {
        if (executionContext != null) {
            if ("auto_battle".equals(executionContext.getTaskCode())) {
                return true;
            }
            if ("MEMBER".equalsIgnoreCase(executionContext.getWindowRole())) {
                return true;
            }
        }
        return windowContext != null && windowContext.getRole() == WindowRole.MEMBER;
    }
}
