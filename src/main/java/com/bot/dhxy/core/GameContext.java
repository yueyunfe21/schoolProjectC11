package com.bot.dhxy.core;

import com.bot.dhxy.model.PlayerCharacter;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 🧠 游戏全局上下文（外挂的大脑与记忆区）
 * 绝对不要把业务逻辑写在这里，这里只存状态！
 */
@Data
@Component
public class GameContext {

    // ==========================================
    // 1. 身体档案：我是谁，我在哪
    // ==========================================
    private PlayerCharacter me = new PlayerCharacter();

    // ==========================================
    // 2. 宏观运行状态：脚本的电源开关
    // ==========================================
    public enum BotStatus {
        IDLE,       // 待机中（尚未启动任何主线）
        RUNNING,    // 疯狂印钞中
        PAUSED,     // 玩家按下了暂停键（挂起所有动作）
        ERROR       // 发生死机级错误（如断线）
    }
    private BotStatus botStatus = BotStatus.IDLE;

    // ==========================================
    // 3. 微观行为状态：角色当前到底在干嘛？
    // ==========================================
    public enum ActionState {
        FREE,           // 闲置发呆（随时可以接新任务）
        NAVIGATING,     // 🏃 赶路中（此时遇敌是意外暗雷）
        INTERACTING,    // 💬 交互中（正在点NPC或弹对话框）
        IN_COMBAT,      // ⚔️ 战斗中！（最高优先级，挂起一切操作）
        TASK_VERIFYING  // 🔍 核验中（战斗结束或交完任务，正在等Alt+Q的情报）
    }
    // 默认处于发呆状态
    private ActionState currentActionState = ActionState.FREE;

    // ==========================================
    // 4. 当前任务记忆 (Task Memory)
    // ==========================================
    private String currentTaskName = "";    // 例："五环"
    private int currentTaskProgress = 0;    // 例：3 (代表当前跑到了第3环)
}