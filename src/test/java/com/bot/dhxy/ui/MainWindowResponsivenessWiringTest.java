package com.bot.dhxy.ui;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainWindowResponsivenessWiringTest {

    @Test
    void dashboardSnapshotRunsOncePerBackgroundRoundAndUiUsesCachedResult() throws Exception {
        String source = read("src/main/java/com/bot/dhxy/ui/MainWindowController.java");

        assertEquals(1, occurrences(source, "windowTaskControlService.getSystemSnapshot()"));
        assertTrue(method(source, "private void loadDashboardRefresh(")
                .contains("windowTaskControlService.getSystemSnapshot()"));
        assertFalse(method(source, "private void refreshControlStates()")
                .contains("getSystemSnapshot()"));
        assertFalse(method(source, "private boolean isSettingsEditLocked()")
                .contains("getSystemSnapshot()"));
        assertFalse(method(source, "private List<WindowTaskSnapshot> getSelectedWindowSnapshots()")
                .contains("getSystemSnapshot()"));
        assertTrue(source.contains("uiRefreshExecutor.submit(() -> loadDashboardRefresh(request))"));
        assertTrue(source.contains("dashboard refresh coalesced"));
    }

    @Test
    void firstScreenAndLifecycleExposeResponsivenessBoundaries() throws Exception {
        String controller = read("src/main/java/com/bot/dhxy/ui/MainWindowController.java");
        String service = read("src/main/java/com/bot/dhxy/ui/MainWindowService.java");

        String buildView = method(controller, "public Parent buildView()");
        assertTrue(buildView.contains("showDashboardInitializingState();"));
        assertTrue(buildView.contains("requestDashboardRefresh(\"initial\");"));
        assertFalse(buildView.contains("getSystemSnapshot()"));
        assertTrue(controller.contains("正在初始化窗口状态..."));
        assertTrue(controller.contains("first interactive dashboard rendered"));
        assertTrue(controller.contains("slow dashboard refresh"));
        assertTrue(controller.contains("uiRefreshExecutor.shutdown();"));
        assertTrue(service.contains("stage.setOnShown"));
        assertTrue(service.contains("mainWindowController.onStageShown();"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Missing method: " + signature);
        }
        int nextMethod = source.indexOf("\n    private ", start + signature.length());
        int nextPublicMethod = source.indexOf("\n    public ", start + signature.length());
        int end = minPositive(nextMethod, nextPublicMethod, source.length());
        return source.substring(start, end);
    }

    private static int minPositive(int first, int second, int fallback) {
        if (first < 0) {
            return second < 0 ? fallback : second;
        }
        if (second < 0) {
            return first;
        }
        return Math.min(first, second);
    }

    private static int occurrences(String source, String value) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }
}
