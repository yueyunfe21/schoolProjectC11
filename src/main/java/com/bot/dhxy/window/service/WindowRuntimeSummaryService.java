package com.bot.dhxy.window.service;

import com.bot.dhxy.window.interaction.WindowInteractionDiagnostics;
import com.bot.dhxy.window.runner.WindowTaskSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 多窗口运行摘要服务，主要用于日志、调试和后续 UI 展示。
 */
@Service
public class WindowRuntimeSummaryService {

    private final WindowTaskControlService windowTaskControlService;
    private final WindowInteractionDiagnostics interactionDiagnostics;

    public WindowRuntimeSummaryService(WindowTaskControlService windowTaskControlService,
                                       WindowInteractionDiagnostics interactionDiagnostics) {
        this.windowTaskControlService = windowTaskControlService;
        this.interactionDiagnostics = interactionDiagnostics;
    }

    public String summarize() {
        WindowSystemSnapshot system = windowTaskControlService.getSystemSnapshot();
        List<WindowTaskSnapshot> windows = system.getWindows();
        long nativeReadyCount = interactionDiagnostics.countReady(windows);
        return "窗口摘要：registered=" + system.getRegisteredWindowCount()
                + ", running=" + system.getRunningWindowCount()
                + ", idle=" + system.getIdleWindowCount()
                + ", capacity=" + system.getRegisteredWindowCount() + "/" + system.getMaxWindowCount()
                + ", nativeReady=" + nativeReadyCount + "/" + windows.size();
    }

    public String summarizeWindow(String windowId) {
        return windowTaskControlService.getSnapshots().stream()
                .filter(snapshot -> windowId != null && windowId.equals(snapshot.getWindowId()))
                .findFirst()
                .map(this::summarizeWindow)
                .orElse("窗口不存在：" + windowId);
    }

    public String summarizeWindow(WindowTaskSnapshot snapshot) {
        if (snapshot == null) {
            return "窗口为空";
        }
        return "窗口=" + snapshot.getWindowId()
                + ", role=" + snapshot.getRoleDisplayName()
                + ", status=" + snapshot.getStatusDisplayName()
                + ", selectedTask=" + snapshot.getSelectedTaskDisplayName()
                + ", runningTask=" + snapshot.getRunningTaskDisplayName()
                + ", hwnd=" + safe(snapshot.getNativeHandle())
                + ", geometry=" + snapshot.getGeometryText()
                + ", message=" + safe(snapshot.getLastMessage());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
