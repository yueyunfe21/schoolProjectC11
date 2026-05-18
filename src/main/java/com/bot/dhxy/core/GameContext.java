package com.bot.dhxy.core;

import com.bot.dhxy.model.PlayerCharacter;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 游戏运行上下文。
 *
 * 单窗口兼容模式下使用默认状态。
 * 多窗口任务线程中由 WindowTaskRunner 绑定窗口专属 State，避免多个窗口共享同一份角色/任务状态。
 */
@Component
public class GameContext {

    private final State defaultState = new State();
    private final ThreadLocal<State> threadLocalState = ThreadLocal.withInitial(() -> defaultState);

    public enum BotStatus {
        IDLE,
        RUNNING,
        PAUSED,
        ERROR
    }

    public enum ActionState {
        FREE,
        NAVIGATING,
        INTERACTING,
        IN_COMBAT,
        TASK_VERIFYING
    }

    public static class State {
        private PlayerCharacter me = new PlayerCharacter();
        private BotStatus botStatus = BotStatus.IDLE;
        private ActionState currentActionState = ActionState.FREE;
        private String currentTaskName = "";
        private int currentTaskProgress = 0;

        public PlayerCharacter getMe() {
            return me;
        }

        public void setMe(PlayerCharacter me) {
            this.me = me == null ? new PlayerCharacter() : me;
        }

        public BotStatus getBotStatus() {
            return botStatus;
        }

        public void setBotStatus(BotStatus botStatus) {
            this.botStatus = botStatus == null ? BotStatus.IDLE : botStatus;
        }

        public ActionState getCurrentActionState() {
            return currentActionState;
        }

        public void setCurrentActionState(ActionState currentActionState) {
            this.currentActionState = currentActionState == null ? ActionState.FREE : currentActionState;
        }

        public String getCurrentTaskName() {
            return currentTaskName;
        }

        public void setCurrentTaskName(String currentTaskName) {
            this.currentTaskName = currentTaskName == null ? "" : currentTaskName;
        }

        public int getCurrentTaskProgress() {
            return currentTaskProgress;
        }

        public void setCurrentTaskProgress(int currentTaskProgress) {
            this.currentTaskProgress = currentTaskProgress;
        }

        public void resetRuntimeState() {
            botStatus = BotStatus.IDLE;
            currentActionState = ActionState.FREE;
            currentTaskName = "";
            currentTaskProgress = 0;
        }
    }

    public State newState() {
        return new State();
    }

    public State currentState() {
        return threadLocalState.get();
    }

    public void bindState(State state) {
        threadLocalState.set(Objects.requireNonNull(state, "game context state must not be null"));
    }

    public void clearBoundState() {
        threadLocalState.remove();
    }

    public <T> T callWithState(State state, Supplier<T> action) {
        bindState(state);
        try {
            return action.get();
        } finally {
            clearBoundState();
        }
    }

    public void runWithState(State state, Runnable action) {
        bindState(state);
        try {
            action.run();
        } finally {
            clearBoundState();
        }
    }

    public PlayerCharacter getMe() {
        return currentState().getMe();
    }

    public void setMe(PlayerCharacter me) {
        currentState().setMe(me);
    }

    public BotStatus getBotStatus() {
        return currentState().getBotStatus();
    }

    public void setBotStatus(BotStatus botStatus) {
        currentState().setBotStatus(botStatus);
    }

    public ActionState getCurrentActionState() {
        return currentState().getCurrentActionState();
    }

    public void setCurrentActionState(ActionState currentActionState) {
        currentState().setCurrentActionState(currentActionState);
    }

    public String getCurrentTaskName() {
        return currentState().getCurrentTaskName();
    }

    public void setCurrentTaskName(String currentTaskName) {
        currentState().setCurrentTaskName(currentTaskName);
    }

    public int getCurrentTaskProgress() {
        return currentState().getCurrentTaskProgress();
    }

    public void setCurrentTaskProgress(int currentTaskProgress) {
        currentState().setCurrentTaskProgress(currentTaskProgress);
    }

    public void resetRuntimeState() {
        currentState().resetRuntimeState();
    }
}
