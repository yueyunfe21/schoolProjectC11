package com.bot.dhxy.task;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR105 maintenance broadcast handoff timing.
 */
public class MaintenanceBroadcastFixedHandoffDelayWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        requireFixedDelayConstant(xiuluo, "XiuluoTaskV2");
        requireFixedDelayConstant(wubei, "WubeiTask");

        String xiuluoHandoff = between(xiuluo,
                "private long handoffDelayMs(",
                "private XiuluoStepOutcome compensateMaintenanceHandoffDelay(");
        requireFixedHandoffBlock(xiuluoHandoff, "XiuluoTaskV2 handoffDelayMs");

        String xiuluoDelayHelper = between(xiuluo,
                "private long maintenanceBroadcastHandoffDelayMs()",
                "private String maintenanceBroadcastHandoffSource(");
        require(xiuluoDelayHelper.contains("return MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS;"),
                "Xiuluo maintenance handoff helper must return the fixed 3s constant");
        require(!xiuluoDelayHelper.contains("getRegisteredWindowCount"),
                "Xiuluo maintenance handoff helper must not depend on registered window count");

        String wubeiHandoff = between(wubei,
                "private long handoffDelayMs(",
                "private boolean isChainedPostBattleBroadcastSource(");
        requireFixedHandoffBlock(wubeiHandoff, "WubeiTask handoffDelayMs");
        require(wubeiHandoff.contains("compensateFormalMaintenanceTimers(delayMs"),
                "Wubei fixed maintenance handoff delay must still be compensated");
    }

    private static void requireFixedDelayConstant(String source, String name) {
        require(source.contains("MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS = 3_000L"),
                name + " must define the fixed 3s maintenance handoff delay");
        require(!source.contains("MAINTENANCE_BROADCAST_HANDOFF_PER_WINDOW_MS"),
                name + " must remove the old per-window maintenance handoff constant");
    }

    private static void requireFixedHandoffBlock(String source, String name) {
        require(source.contains("maintenance broadcast handoff delay"),
                name + " must still log maintenance broadcast handoff delays");
        require(source.contains("delayMs={}"),
                name + " log must include the final fixed delay");
        require(!source.contains("getRegisteredWindowCount"),
                name + " must not read registered window count for maintenance handoff");
        require(!source.contains("windowCount"),
                name + " must not log or compute a per-window maintenance handoff");
        require(!source.contains("perWindowMs"),
                name + " must not log or compute a per-window maintenance handoff");
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
