package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.util.List;

/**
 * Detects and handles the in-game "return to team" signal after a member leaves the team.
 *
 * <p>Members may click the return button when they see it. Leaders do not click this signal; after
 * confirmed return-to-town flows they wait for the signal to disappear so dead members have time to
 * rejoin. Template matching is screenshot-only, while clicking is submitted through the global input
 * queue.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamReturnService {
    private static final String MEMBER_RETURN_BUTTON_PATH = "images/template/status/gui.png";
    private static final String LEADER_RETURN_SIGNAL_PATH = "images/template/status/zhao.png";
    private static final long DEFAULT_LEADER_WAIT_TIMEOUT_MS = 120_000L;
    private static final long DEFAULT_LEADER_WAIT_POLL_MS = 3_000L;

    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final BotProperties botProperties;

    /**
     * Click the return-team button if it is visible for this window.
     *
     * @param context current window execution context; used for logs and stop ownership.
     * @param source caller label for logs.
     * @return true if the button was found and a click was queued; false when no signal is visible.
     */
    public boolean clickReturnTeamIfPresent(TaskExecutionContext context, String source) {
        Point buttonPoint = findReturnTeamButton();
        if (buttonPoint == null) {
            return false;
        }

        Point clickPoint = coordinateHelper.getRandomizedPoint(buttonPoint, 3, 3);
        log.info("{} team return: return button found by {}, click=({}, {})",
                context.getLogPrefix(), source, clickPoint.x, clickPoint.y);
        inputSequences.submitAndWait("teamReturn:" + source, List.of(
                InputAction.clickLeft(clickPoint.x, clickPoint.y, 150),
                InputAction.sleep(500)
        ));
        return true;
    }

    /**
     * Leader-side wait when a return-team signal is visible after returning to town.
     *
     * @param context current leader execution context. Stop requests are honored during polling.
     * @param source caller label for logs.
     * @return false when there was no signal and no wait was needed; true after the signal disappears
     * or the configured timeout expires.
     */
    public boolean waitForMembersReturnIfNeeded(TaskExecutionContext context, String source) {
        if (!isReturnTeamSignalPresent()) {
            return false;
        }

        long timeoutMs = leaderWaitTimeoutMs();
        long pollMs = leaderWaitPollMs();
        long deadlineAtMs = System.currentTimeMillis() + timeoutMs;
        log.warn("{} team return: leader sees return signal after {}, wait up to {} ms for member return",
                context.getLogPrefix(), source, timeoutMs);

        while (System.currentTimeMillis() < deadlineAtMs) {
            context.throwIfStopRequested();
            sleep(pollMs);
            if (!isReturnTeamSignalPresent()) {
                log.info("{} team return: return signal disappeared, continue leader task",
                        context.getLogPrefix());
                return true;
            }
        }

        log.warn("{} team return: return signal still present after {} ms, let task-specific flow continue",
                context.getLogPrefix(), timeoutMs);
        return true;
    }

    /**
     * Check whether the leader-side return-team signal is visible.
     *
     * <p>Members click the {@code 归} button, but leaders wait on the teammate status marker in the
     * same right-side team area. The leader marker currently uses the {@code 招} template because
     * that is what appears in the leader's team-status panel when a member needs to return.</p>
     *
     * @return true when the leader signal template is found inside the configured team-status area.
     */
    public boolean isReturnTeamSignalPresent() {
        return findLeaderReturnSignal() != null;
    }

    /**
     * Template-match the return-team button in the configured client area.
     *
     * @return screen-absolute center point returned by {@link CoordinateHelper}, or null when the
     * template is not present.
     */
    private Point findReturnTeamButton() {
        int[] rect = coordinateHelper.getScaledRect(
                botProperties.getReturnTeamAreaX(),
                botProperties.getReturnTeamAreaY(),
                botProperties.getReturnTeamAreaW(),
                botProperties.getReturnTeamAreaH()
        );
        return coordinateHelper.findImageInRegion(MEMBER_RETURN_BUTTON_PATH, rect, botProperties.getReturnTeamMatchRate());
    }

    /**
     * Template-match the leader-side team-return signal in the configured client area.
     *
     * @return screen-absolute center point for the signal, or null when no waiting member marker is
     * visible. This method is screenshot-only and never sends input.
     */
    private Point findLeaderReturnSignal() {
        int[] rect = coordinateHelper.getScaledRect(
                botProperties.getReturnTeamAreaX(),
                botProperties.getReturnTeamAreaY(),
                botProperties.getReturnTeamAreaW(),
                botProperties.getReturnTeamAreaH()
        );
        return coordinateHelper.findImageInRegion(LEADER_RETURN_SIGNAL_PATH, rect, botProperties.getReturnTeamMatchRate());
    }

    /**
     * @return configured leader wait timeout in milliseconds, or the production default.
     */
    private long leaderWaitTimeoutMs() {
        long configured = botProperties.getReturnTeamLeaderWaitTimeoutMs();
        return configured > 0 ? configured : DEFAULT_LEADER_WAIT_TIMEOUT_MS;
    }

    /**
     * @return configured signal polling interval in milliseconds, or the production default.
     */
    private long leaderWaitPollMs() {
        long configured = botProperties.getReturnTeamLeaderWaitPollMs();
        return configured > 0 ? configured : DEFAULT_LEADER_WAIT_POLL_MS;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
