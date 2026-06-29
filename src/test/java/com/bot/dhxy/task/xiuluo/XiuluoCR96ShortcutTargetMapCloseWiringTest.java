package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR96 shortcut target-map arrival maintenance close.
 */
public class XiuluoCR96ShortcutTargetMapCloseWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);

        String shortcutClick = between(task,
                "private XiuluoStepOutcome tryTrackerShortcutWithPanel(",
                "private TaskTrackerPanelReadResult waitForAcceptTrackerPanelResult(");
        require(shortcutClick.indexOf("inputSequences.moveAndClickLeft(")
                        < shortcutClick.indexOf("resolveReadyShortcutObjectiveTargetMap("),
                "CR96 must not wait for objective parse before clicking tracker green");
        require(shortcutClick.contains("registerTrackerShortcutPathingIntent(runtime, intentSource,\n                shortcutTargetMap)"),
                "Shortcut green click must pass the ready accept-time objective target map directly to Runner");
        require(shortcutClick.contains("attachShortcutTargetMapUpgrade(runtime, state, pathingIntent)"),
                "Shortcut must attach a late objective target-map upgrade after registering an untargeted tracker intent");

        String resolveMethod = between(task,
                "private String resolveReadyShortcutObjectiveTargetMap(",
                "private WindowPathingIntent registerTrackerShortcutPathingIntent(");
        require(resolveMethod.contains("!future.isDone()"),
                "CR96 must not block shortcut green click waiting for objective parse");
        require(resolveMethod.contains("future.getNow(Optional.empty())"),
                "CR96 may only use an already completed accept-time objective future");
        require(resolveMethod.contains("NpcTarget::getMapName"),
                "CR96 must use the accept-time parsed objective map, not tracker green targetMapName");

        String registerMethod = between(task,
                "private WindowPathingIntent registerTrackerShortcutPathingIntent(",
                "private XiuluoStepOutcome waitTrackerShortcutPathing(");
        require(registerMethod.contains("WindowPathingIntentType.TARGETED"),
                "CR96 must give Runner a targeted pathing intent when target map is ready");
        require(registerMethod.contains("WindowPathingIntentType.UNTARGETED_TRACKER"),
                "CR96 must preserve old untargeted fallback when objective map is not ready");
        require(registerMethod.contains(".targetMapName(hasTargetMap ? targetMapName : null)"),
                "CR96 must pass the parsed target map into the Runner intent");
        require(registerMethod.contains(".targetX(null)") && registerMethod.contains(".targetY(null)"),
                "CR96 shortcut arrival is map-only; coordinates must not delay team-window close");

        String lateUpgradeMethod = between(task,
                "private void attachShortcutTargetMapUpgrade(",
                "private XiuluoStepOutcome waitTrackerShortcutPathing(");
        require(lateUpgradeMethod.contains("future.thenAccept("),
                "Late shortcut target-map upgrade must run from the background objective future completion");
        require(lateUpgradeMethod.contains("runtime.upgradeActivePathingIntentTargetMap("),
                "Late shortcut target-map upgrade must update the current active Runner intent");
        require(lateUpgradeMethod.contains("pathingIntent.getIntentId()"),
                "Late shortcut target-map upgrade must be scoped to the exact tracker intent id");
        require(lateUpgradeMethod.contains("NpcTarget::getMapName"),
                "Late shortcut target-map upgrade must use the story objective map");

        String waitMethod = between(task,
                "private XiuluoStepOutcome waitTrackerShortcutPathing(",
                "private XiuluoStepOutcome fallbackFromShortcut(");
        require(waitMethod.contains("WindowPathingState.ARRIVED"),
                "Shortcut wait must handle target-map ARRIVED separately");
        require(waitMethod.contains("isShortcutTargetMapArrival(snapshot)"),
                "Shortcut ARRIVED close must be scoped to CR96 target-map arrival");
        require(waitMethod.contains("shortcut-target-map-arrived"),
                "Shortcut target-map arrival close must use the CR96 log source");
        require(waitMethod.contains("return waitForTrackerShortcutWake("),
                "After target-map close, shortcut must keep waiting for prepared enter-battle/combat");
        require(task.contains("shortcut-enter-battle-prepared"),
                "Existing prepared enter-battle close fallback must remain");

        String arrivalHelper = between(task,
                "private boolean isShortcutTargetMapArrival(",
                "private XiuluoStepOutcome fallbackFromShortcut(");
        require(arrivalHelper.contains("WindowPathingIntentType.TARGETED"),
                "Target-map arrival helper must require the targeted shortcut intent");
        require(arrivalHelper.contains("intent.getTargetMapName()"),
                "Target-map arrival helper must require a parsed objective target map");

        require(WindowPathingIntentType.TARGETED != WindowPathingIntentType.UNTARGETED_TRACKER,
                "Sanity: targeted and untargeted pathing types must remain distinct");
        require(WindowPathingState.ARRIVED != WindowPathingState.STOPPED_AWAY,
                "Sanity: ARRIVED and STOPPED_AWAY terminal states must remain distinct");

        String runtime = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java"), StandardCharsets.UTF_8);
        String runtimeUpgradeMethod = between(runtime,
                "public boolean upgradeActivePathingIntentTargetMap(",
                "    /**\n     * Update the latest background observation");
        require(runtimeUpgradeMethod.contains("expectedIntentId"),
                "Runtime target-map upgrade must require the expected active intent id");
        require(runtimeUpgradeMethod.contains("WindowPathingIntentType.UNTARGETED_TRACKER"),
                "Runtime target-map upgrade must only upgrade untargeted tracker intents");
        require(runtimeUpgradeMethod.contains("WindowPathingIntentType.TARGETED"),
                "Runtime target-map upgrade must convert the intent to TARGETED");
        require(runtimeUpgradeMethod.contains("targetMapName(normalizedTargetMap)"),
                "Runtime target-map upgrade must write the late story target map into the active intent");

        System.out.println("XiuluoCR96ShortcutTargetMapCloseWiringTest passed");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
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
