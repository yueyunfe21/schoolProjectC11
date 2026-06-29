package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR79 修罗 wild-monster cancel dialog recovery.
 *
 * <p>The cancel dialog is only a blocker after the normal "看打" enter-battle template misses. A
 * matched cancel option must close that dialog and return to the existing target-click retry flow;
 * it must not be treated as battle entry proof.</p>
 */
public class XiuluoWildMonsterCancelRecoveryWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);

        require(task.contains("XIULUO_WILD_MONSTER_CANCEL_TEMPLATE"),
                "CR79 requires a dedicated 修罗 wild-monster cancel template constant");
        require(task.contains("images/template/dialog/xiuluo/xiuluo_wild_monster_cancel.png"),
                "CR79 cancel template must live beside the 修罗 dialog templates");
        require(task.contains("OPTION_WILD_MONSTER_CANCEL"),
                "CR79 requires a distinct action key so cancel is never confused with enter-battle");

        String recovery = between(task,
                "private XiuluoStepOutcome recoverTargetClickFailure(",
                "private XiuluoStepOutcome enterBattleFromDirectCombatClick(");
        int enterTemplateIndex = recovery.indexOf("ENTER_BATTLE_TEMPLATE");
        int cancelTemplateIndex = recovery.indexOf("XIULUO_WILD_MONSTER_CANCEL_TEMPLATE");
        int ocrIndex = recovery.indexOf("xiuluo-v2:enter-battle-ocr:");
        require(enterTemplateIndex >= 0, "normal 看打 template must remain first in recovery");
        require(cancelTemplateIndex > enterTemplateIndex,
                "wild-monster cancel template must only be tried after normal 看打 template misses");
        require(ocrIndex > cancelTemplateIndex,
                "wild-monster cancel template must be tried before the old OCR/direct-combat recovery");

        String cancelBranch = between(recovery,
                "OPTION_WILD_MONSTER_CANCEL.equals(",
                "DialogResult keywordResult");
        require(cancelBranch.contains("retryCurrentOrRecover(state, XiuluoPhase.CLICK_TARGET_NPC"),
                "cancel hit must return to the existing target-click retry/recovery flow");
        require(!cancelBranch.contains("enterBattleFromRecoveredDialog("),
                "cancel hit must not enter WAIT_COMBAT as recovered battle entry");
        require(!cancelBranch.contains("confirmPendingSmartClick("),
                "cancel hit must not commit smart-click success evidence");
        require(cancelBranch.contains("wild-monster cancel"),
                "cancel hit/miss logs must be distinguishable in runtime evidence");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
