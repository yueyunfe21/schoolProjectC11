package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.input.action.InputAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** Maps closed turn mouse actions to the existing serialized physical-input vocabulary. */
@Component
public final class TurnInputActionMapper {

    /**
     * Map one mouse input without scaling its screen-absolute coordinates.
     *
     * @param action closed turn input action; must be one of the seven mouse actions.
     * @param input typed input fields containing screen-absolute points and signed wheel delta.
     * @param windowRect current bound-window rectangle in screen-absolute pixels.
     * @return one ordered action list that must be submitted as a single queue request.
     */
    public List<InputAction> mapMouse(TurnInputAction action,
                                      TurnInputSpec input,
                                      TurnWindowRect windowRect) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(windowRect, "windowRect");
        if (action != TurnInputAction.CLICK_LEFT
                && action != TurnInputAction.CLICK_RIGHT
                && (input.clickDelayMs() != null || input.queueHoldMs() != null)) {
            throw new IllegalArgumentException("click timing is allowed only for CLICK_LEFT/CLICK_RIGHT");
        }

        return switch (action) {
            case MOVE_MOUSE -> List.of(InputAction.moveMouse(
                    requirePointX(input, windowRect), requirePointY(input, windowRect)));
            case CLICK_LEFT -> {
                InputAction click = InputAction.clickLeft(
                        requirePointX(input, windowRect),
                        requirePointY(input, windowRect),
                        requireClickTiming(input.clickDelayMs(), "input.clickDelayMs"));
                int queueHoldMs = requireClickTiming(input.queueHoldMs(), "input.queueHoldMs");
                yield queueHoldMs > 0
                        ? List.of(click, InputAction.sleep(queueHoldMs))
                        : List.of(click);
            }
            case CLICK_RIGHT -> {
                InputAction click = InputAction.clickRight(
                        requirePointX(input, windowRect),
                        requirePointY(input, windowRect),
                        requireClickTiming(input.clickDelayMs(), "input.clickDelayMs"));
                int queueHoldMs = requireClickTiming(input.queueHoldMs(), "input.queueHoldMs");
                yield queueHoldMs > 0
                        ? List.of(click, InputAction.sleep(queueHoldMs))
                        : List.of(click);
            }
            case DOUBLE_CLICK_LEFT -> {
                int x = requirePointX(input, windowRect);
                int y = requirePointY(input, windowRect);
                yield List.of(
                        InputAction.clickLeft(x, y, 0),
                        InputAction.clickLeft(x, y, 0));
            }
            case DOUBLE_CLICK_RIGHT -> List.of(InputAction.doubleRightClick(
                    requirePointX(input, windowRect), requirePointY(input, windowRect), 0, 0));
            case DRAG_LEFT -> {
                int startX = requirePointX(input, windowRect);
                int startY = requirePointY(input, windowRect);
                int endX = requireCoordinate(input.endX(), "input.endX");
                int endY = requireCoordinate(input.endY(), "input.endY");
                requireInsideWindow(endX, endY, windowRect, "input drag end");
                yield List.of(InputAction.dragAndDrop(startX, startY, endX, endY));
            }
            case SCROLL -> {
                int x = requirePointX(input, windowRect);
                int y = requirePointY(input, windowRect);
                int delta = requireCoordinate(input.scrollDelta(), "input.scrollDelta");
                if (delta == 0) {
                    throw new IllegalArgumentException("input.scrollDelta must not be zero");
                }
                if (delta == Integer.MIN_VALUE) {
                    throw new IllegalArgumentException("input.scrollDelta magnitude is too large");
                }
                InputAction wheel = delta > 0
                        ? InputAction.scrollDown(delta)
                        : InputAction.scrollUp(-delta);
                yield List.of(InputAction.moveMouse(x, y), wheel);
            }
            case KEY_TAP, KEY_DOWN, KEY_UP, TEXT_INPUT ->
                    throw new IllegalArgumentException("not a mouse input action: " + action);
        };
    }

    public boolean isMouse(TurnInputAction action) {
        return action == TurnInputAction.MOVE_MOUSE
                || action == TurnInputAction.CLICK_LEFT
                || action == TurnInputAction.CLICK_RIGHT
                || action == TurnInputAction.DOUBLE_CLICK_LEFT
                || action == TurnInputAction.DOUBLE_CLICK_RIGHT
                || action == TurnInputAction.DRAG_LEFT
                || action == TurnInputAction.SCROLL;
    }

    private int requirePointX(TurnInputSpec input, TurnWindowRect windowRect) {
        int x = requireCoordinate(input.x(), "input.x");
        int y = requireCoordinate(input.y(), "input.y");
        requireInsideWindow(x, y, windowRect, "input point");
        return x;
    }

    private int requirePointY(TurnInputSpec input, TurnWindowRect windowRect) {
        int x = requireCoordinate(input.x(), "input.x");
        int y = requireCoordinate(input.y(), "input.y");
        requireInsideWindow(x, y, windowRect, "input point");
        return y;
    }

    private int requireCoordinate(Integer value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private int requireClickTiming(Integer value, String field) {
        if (value == null) {
            return 0;
        }
        if (value < 0 || value > 5_000) {
            throw new IllegalArgumentException(field + " must be in [0,5000]");
        }
        return value;
    }

    private void requireInsideWindow(int x, int y, TurnWindowRect window, String field) {
        long right = (long) window.left() + window.width();
        long bottom = (long) window.top() + window.height();
        if (window.width() <= 0
                || window.height() <= 0
                || x < window.left()
                || y < window.top()
                || x >= right
                || y >= bottom) {
            throw new IllegalArgumentException(field + " is outside the refreshed window rectangle");
        }
    }
}
