package com.bot.dhxy.window.runtime;

import com.bot.dhxy.cloud.turn.WindowTurnLoop;
import com.bot.dhxy.cloud.turn.local.LocalTeamRolePreflightService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G148（G145 P7+M2）：可恢复终态重启熔断合同。
 * P7——SKIPPED/FAILED 连击必须驱动退避梯子升档，SKIPPED 连击有上限；
 * M2——窗口尺寸失明态（出生偏基线/中途 live 漂移）禁止自动重启。
 */
class G148RecoveryFuseContractTest {

    private static final Path CONTROL_SERVICE = Path.of(
            "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java");

    @Test
    void geometryDriftFuseTripsAfterThreeConsecutiveMismatchesAndResetsOnMatch() {
        WindowNativeBinding registered = new WindowNativeBinding(
                "12345", "title", "class", 77L, 10, 20, 1036, 783);
        LiveSizeProbe probe = new LiveSizeProbe();
        WindowNativeBindingRefreshService service = new WindowNativeBindingRefreshService(probe);

        probe.width = 1117;
        probe.height = 840;
        for (int i = 0; i < 2; i++) {
            assertTrue(service.refreshGeometry(registered).isPresent());
            assertFalse(service.isGeometryDriftFused("12345"),
                    "两次不一致还不许熔断（容忍抖动）");
        }
        assertTrue(service.refreshGeometry(registered).isPresent());
        assertTrue(service.isGeometryDriftFused("12345"), "连续三次 live 尺寸不一致必须熔断");
        assertEquals("1117x840->1036x783", service.geometryDriftDetail("12345"));

        probe.width = 1036;
        probe.height = 783;
        assertTrue(service.refreshGeometry(registered).isPresent());
        assertFalse(service.isGeometryDriftFused("12345"), "尺寸恢复一致必须自动复位");
    }

    @Test
    void calibratedSizeGateAcceptsOnlyBaseline() {
        assertTrue(LocalTeamRolePreflightService.isAtCalibratedSize(new WindowNativeBinding(
                "1", "t", "c", 1L, 0, 0, 1036, 783)));
        assertFalse(LocalTeamRolePreflightService.isAtCalibratedSize(new WindowNativeBinding(
                "1", "t", "c", 1L, 0, 0, 1117, 840)));
        assertFalse(LocalTeamRolePreflightService.isAtCalibratedSize(null));
    }

    @Test
    void retryLadderEscalatesWithStreak() {
        // 抖动 ±20%，相邻档可能重叠；隔两档比较必然单调。
        long first = WindowTurnLoop.failureRetryDelayMs("w", 1);
        long third = WindowTurnLoop.failureRetryDelayMs("w", 3);
        long sixth = WindowTurnLoop.failureRetryDelayMs("w", 6);
        assertTrue(first < third, "attempt 1 (~250ms) 必须小于 attempt 3 (~1s)");
        assertTrue(third < sixth, "attempt 3 (~1s) 必须小于 attempt 6 (~5s)");
        assertTrue(first <= 300L, "首次重试保持在既有节奏（~250ms±20%）");
        assertTrue(sixth >= 4_000L, "梯子顶档必须到秒级");
    }

    @Test
    void controlServiceSourceKeepsStreakAndFuseWiring() throws Exception {
        String source = Files.readString(CONTROL_SERVICE, StandardCharsets.UTF_8);

        int branchStart = source.indexOf("Recoverable Cloud terminal retained");
        assertTrue(branchStart > 0, "找不到可恢复终态分支；改了结构就更新本合同");
        String beforeBranch = source.substring(0, branchStart);

        assertTrue(beforeBranch.contains("remoteRecoveryStreaks.merge(windowId, 1, Integer::sum)"),
                "P7：重启前必须累计连击数");
        assertTrue(beforeBranch.contains("SKIPPED_RECOVERY_MAX_STREAK"),
                "P7：SKIPPED 连击必须有上限熔断");
        assertTrue(beforeBranch.contains("isGeometryDriftFused")
                        && beforeBranch.contains("isAtCalibratedSize"),
                "M2：自动重启前必须过两道尺寸失明检查（中途漂移+出生偏基线）");

        assertTrue(source.contains("failureRetryDelayMs(windowId, recoveryStreak)"),
                "P7：重试延迟必须由连击数驱动退避梯子，不许退回常数 attempt=1");
        assertTrue(source.contains("recoverRemoteTerminal(remainingPlan, terminal, recoveryStreak)"),
                "P7：递归重启必须携带连击数");

        assertTrue(source.contains("// G148-P7：手动新启=新生命周期，可恢复终态连击账清零。"),
                "P7：手动新启必须清连击账");
        assertTrue(source.contains("// G148-P7：成功=前置恢复，连击账清零。"),
                "P7：SUCCESS 终态必须清连击账");
    }
}

/** 只喂尺寸的活窗探针桩；其余字段恒定。 */
final class LiveSizeProbe implements WindowNativeBindingRefreshService.NativeWindowProbe {
    int width = 1036;
    int height = 783;

    @Override
    public Optional<WindowNativeBindingRefreshService.LiveWindowSnapshot> snapshot(long handle) {
        return Optional.of(WindowNativeBindingRefreshService.LiveWindowSnapshot.available(
                "title", "class", 77L, 10, 20, width, height));
    }
}
