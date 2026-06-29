package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.service.dialog.DialogOperation;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source/model guard for CR97 prepared 修罗 enter-battle retry.
 *
 * <p>Live validation needs a real prepared dialog that is clicked but does not enter combat. This
 * guard protects the business invariant: a prepared "看打" click is only a pending confirmation,
 * not proof of combat, and WAIT_COMBAT must be able to re-register the same visible option.</p>
 */
public class XiuluoPreparedEnterBattleRetryWiringTest {

    public static void main(String[] args) throws Exception {
        pendingConfirmDoesNotMarkBattleEntered();
        waitCombatCanReregisterShortcutEnterBattleInterest();
    }

    private static void pendingConfirmDoesNotMarkBattleEntered() {
        XiuluoRoundContext pending = XiuluoRoundContext.start(97)
                .withShortcutTrackerClick(
                        XiuluoPhase.WAIT_TRACKER_SHORTCUT_PATHING,
                        "detail.png",
                        new Point(431, 462),
                        "intent-97",
                        "shortcut-click")
                .withPendingEnterBattleConfirm(
                        XiuluoPhase.WAIT_COMBAT,
                        XiuluoCombatSource.TRACKER_CONFIRM,
                        "prepared-clicked");

        require(!pending.enteredBattleByXiuluo(),
                "prepared enter-battle click must not mark the battle as entered before combat radar");
        require(pending.combatSource() == XiuluoCombatSource.TRACKER_CONFIRM,
                "pending prepared click must preserve TRACKER_CONFIRM source for later combat entry");

        XiuluoRoundContext retry = pending.incrementEnterBattleConfirmRetry(
                XiuluoPhase.WAIT_COMBAT,
                "retry-visible-option");
        require(retry.enterBattleConfirmRetryCount() == 1,
                "retry count must advance when WAIT_COMBAT re-registers the visible option");
        require(!retry.enteredBattleByXiuluo(),
                "retrying the visible option must still not claim combat entry");
    }

    private static void waitCombatCanReregisterShortcutEnterBattleInterest() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String waitCombat = between(task,
                "private XiuluoStepOutcome waitCombat(",
                "private XiuluoStepOutcome waitForTargetPathingWake(");
        String consumePrepared = between(task,
                "private XiuluoStepOutcome consumePreparedXiuluoEnterBattle(",
                "private XiuluoStepOutcome waitForCombatStateWake(");

        require(consumePrepared.contains("withPendingEnterBattleConfirm("),
                "prepared enter-battle consume must enter WAIT_COMBAT as pending confirmation");
        require(waitCombat.contains("consumePreparedXiuluoEnterBattle(context, state,"),
                "WAIT_COMBAT must be able to consume a re-prepared shortcut enter-battle action");
        require(waitCombat.contains("registerXiuluoDialogInterest(runtime, DialogOperation.XIULUO_ENTER_BATTLE"),
                "WAIT_COMBAT must re-register XIULUO_ENTER_BATTLE interest when prepared click did not enter combat");
        require(waitCombat.contains("incrementEnterBattleConfirmRetry("),
                "WAIT_COMBAT must bound re-registration with an explicit retry counter");
        require(waitCombat.contains("MAX_ENTER_BATTLE_CONFIRM_RETRIES"),
                "prepared enter-battle retry must have an explicit maximum retry count");
        require(task.contains("DialogOperation.XIULUO_ENTER_BATTLE"),
                "source guard sanity check: task must still use the Xiuluo enter-battle operation");
        require(DialogOperation.XIULUO_ENTER_BATTLE.name().equals("XIULUO_ENTER_BATTLE"),
                "source guard sanity check: operation enum name changed unexpectedly");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex + start.length());
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
