package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnInputAction;
import com.bot.dhxy.cloud.turn.protocol.TurnInputCoordinateSpace;
import com.bot.dhxy.cloud.turn.protocol.TurnInputSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.input.action.InputAction;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.List;
import java.util.Objects;

/** Maps closed turn mouse actions to the existing serialized physical-input vocabulary. */
@Component
public final class TurnInputActionMapper {

    /**
     * Map one mouse input using either its legacy screen-absolute point or a point relative to the
     * freshly resolved exact HWND. Window-relative input is reserved for UI surfaces, such as the
     * mini-map, whose logical point must survive an older Cloud-side window rectangle.
     *
     * @param action closed turn input action; must be one of the seven mouse actions.
     * @param input typed input fields containing points in {@link TurnInputCoordinateSpace} and signed wheel delta.
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
            case MOVE_MOUSE -> {
                Point point = requirePoint(input.x(), input.y(), input.coordinateSpace(), windowRect, "input point");
                yield List.of(InputAction.moveMouse(point.x, point.y));
            }
            case CLICK_LEFT -> {
                Point point = requirePoint(input.x(), input.y(), input.coordinateSpace(), windowRect, "input point");
                InputAction click = InputAction.clickLeft(
                        point.x,
                        point.y,
                        requireClickTiming(input.clickDelayMs(), "input.clickDelayMs"));
                int queueHoldMs = requireClickTiming(input.queueHoldMs(), "input.queueHoldMs");
                yield queueHoldMs > 0
                        ? List.of(click, InputAction.sleep(queueHoldMs))
                        : List.of(click);
            }
            case CLICK_RIGHT -> {
                Point point = requirePoint(input.x(), input.y(), input.coordinateSpace(), windowRect, "input point");
                InputAction click = InputAction.clickRight(
                        point.x,
                        point.y,
                        requireClickTiming(input.clickDelayMs(), "input.clickDelayMs"));
                int queueHoldMs = requireClickTiming(input.queueHoldMs(), "input.queueHoldMs");
                yield queueHoldMs > 0
                        ? List.of(click, InputAction.sleep(queueHoldMs))
                        : List.of(click);
            }
            case DOUBLE_CLICK_LEFT -> {
                Point point = requirePoint(input.x(), input.y(), input.coordinateSpace(), windowRect, "input point");
                yield List.of(
                        InputAction.clickLeft(point.x, point.y, 0),
                        InputAction.clickLeft(point.x, point.y, 0));
            }
            case DOUBLE_CLICK_RIGHT -> {
                Point point = requirePoint(input.x(), input.y(), input.coordinateSpace(), windowRect, "input point");
                yield List.of(InputAction.doubleRightClick(point.x, point.y, 0, 0));
            }
            case DRAG_LEFT -> {
                Point start = requirePoint(input.x(), input.y(), input.coordinateSpace(), windowRect, "input drag start");
                Point end = requirePoint(input.endX(), input.endY(), input.coordinateSpace(), windowRect, "input drag end");
                yield List.of(InputAction.dragAndDrop(start.x, start.y, end.x, end.y));
            }
            case SCROLL -> {
                Point point = requirePoint(input.x(), input.y(), input.coordinateSpace(), windowRect, "input point");
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
                yield List.of(InputAction.moveMouse(point.x, point.y), wheel);
            }
            case KEY_TAP, KEY_DOWN, KEY_UP, TEXT_INPUT, ASCII_TEXT_INPUT ->
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

    private Point requirePoint(Integer rawX,
                               Integer rawY,
                               TurnInputCoordinateSpace declaredSpace,
                               TurnWindowRect windowRect,
                               String field) {
        int x = requireCoordinate(rawX, field + ".x");
        int y = requireCoordinate(rawY, field + ".y");
        TurnInputCoordinateSpace coordinateSpace = declaredSpace == null
                ? TurnInputCoordinateSpace.SCREEN_ABSOLUTE
                : declaredSpace;
        if (coordinateSpace == TurnInputCoordinateSpace.WINDOW_RELATIVE) {
            if (windowRect.width() <= 0 || windowRect.height() <= 0
                    || x < 0 || y < 0 || x >= windowRect.width() || y >= windowRect.height()) {
                throw new IllegalArgumentException(field + " is outside the refreshed window-relative rectangle");
            }
            return new Point(windowRect.left() + x, windowRect.top() + y);
        }
        requireInsideWindow(x, y, windowRect, field);
        return new Point(x, y);
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
