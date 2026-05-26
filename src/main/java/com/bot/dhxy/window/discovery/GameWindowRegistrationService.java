package com.bot.dhxy.window.discovery;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.control.WindowTaskCommandDetail;
import com.bot.dhxy.window.control.WindowTaskCommandResult;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameWindowRegistrationService {

    private final NativeWindowScanner nativeWindowScanner;
    private final NativeWindowRegistrationMapper registrationMapper;
    private final WindowTaskControlService windowTaskControlService;

    public GameWindowRegistrationService(NativeWindowScanner nativeWindowScanner,
                                         NativeWindowRegistrationMapper registrationMapper,
                                         WindowTaskControlService windowTaskControlService) {
        this.nativeWindowScanner = nativeWindowScanner;
        this.registrationMapper = registrationMapper;
        this.windowTaskControlService = windowTaskControlService;
    }

    public List<NativeWindowInfo> scanGameWindows() {
        log.info("Game window scan requested");
        List<NativeWindowInfo> windows = nativeWindowScanner.scanGameWindows();
        log.info("Game window scan returned count={} windows={}", windows.size(), describeWindows(windows));
        return windows;
    }

    /**
     * 正式扫描注册逻辑：扫描到的每个窗口都是独立窗口，全部使用当前选择的任务。
     */
    public WindowTaskCommandResult registerDetectedGameWindows(TaskType taskType) {
        log.info("Register detected game windows start: taskType={}", taskType);
        List<NativeWindowInfo> windows = scanGameWindows();
        if (windows.isEmpty()) {
            log.warn("Register detected game windows found no game windows: taskType={}", taskType);
            return WindowTaskCommandResult.empty("没有扫描到游戏窗口", windowTaskControlService.getSnapshots());
        }
        pruneIdleStaleRegistrations(windows);
        List<WindowRegistrationRequest> requests = registrationMapper.toIndependentRegistrationRequests(windows, taskType);
        log.info("Register detected game windows mapped requests: count={} ids={}",
                requests.size(), requests.stream().map(WindowRegistrationRequest::getWindowId).toList());
        WindowTaskCommandResult result = windowTaskControlService.registerWindows(requests);
        log.info("Register detected game windows done: requested={} success={} failed={} message={}",
                result.getRequestedCount(), result.getSuccessCount(), result.getFailedCount(), result.getMessage());
        return result;
    }

    /**
     * 一键独立启动：清理旧窗口 -> 扫描真实游戏窗口 -> 独立注册 -> 启动所有窗口已选任务。
     */
    public WindowTaskCommandResult scanRegisterAndStartIndependentWindows(TaskType taskType) {
        List<NativeWindowInfo> windows = scanGameWindows();
        if (windows.isEmpty()) {
            return WindowTaskCommandResult.empty("没有扫描到游戏窗口，无法启动独立窗口任务", windowTaskControlService.getSnapshots());
        }

        pruneIdleStaleRegistrations(windows);
        List<WindowRegistrationRequest> requests = registrationMapper.toIndependentRegistrationRequests(windows, taskType);
        WindowTaskCommandResult registerResult = windowTaskControlService.registerWindows(requests);
        List<String> windowIds = requests.stream()
                .map(WindowRegistrationRequest::getWindowId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        WindowTaskCommandResult startResult = windowTaskControlService.startSelectedTasks(windowIds);

        List<WindowTaskCommandDetail> details = new ArrayList<>();
        details.addAll(registerResult.getDetails());
        details.addAll(startResult.getDetails());

        String taskText = taskType == null ? "UNKNOWN" : taskType.getDisplayName();
        String message = "一键独立窗口启动完成：检测到 " + windows.size()
                + " 个游戏窗口，任务=" + taskText
                + "，启动=" + startResult.getSuccessCount() + "/" + startResult.getRequestedCount();

        return WindowTaskCommandResult.of(
                startResult.getRequestedCount(),
                startResult.getSuccessCount(),
                message,
                windowTaskControlService.getSnapshots(),
                List.of(),
                details
        );
    }

    /**
     * @deprecated 仅保留给旧的测试按身份流程。正式流程不应该由 window 层判断队长/队员。
     */
    @Deprecated
    public WindowTaskCommandResult registerDetectedGameWindowsByRoleForTest(TaskType leaderTaskType) {
        List<NativeWindowInfo> windows = scanGameWindows();
        if (windows.isEmpty()) {
            return WindowTaskCommandResult.empty("没有扫描到游戏窗口", windowTaskControlService.getSnapshots());
        }
        List<WindowRegistrationRequest> requests = registrationMapper.toRegistrationRequests(windows, leaderTaskType);
        return windowTaskControlService.registerWindows(requests);
    }

    private void pruneIdleStaleRegistrations(List<NativeWindowInfo> scannedWindows) {
        Set<String> scannedWindowIds = scannedWindows == null ? Set.of() : scannedWindows.stream()
                .filter(window -> window != null)
                .map(NativeWindowInfo::toWindowId)
                .filter(windowId -> windowId != null && !windowId.isBlank())
                .collect(Collectors.toSet());

        List<String> staleWindowIds = windowTaskControlService.getSnapshots().stream()
                .filter(snapshot -> snapshot != null && !snapshot.isBusy())
                .filter(snapshot -> !snapshot.hasNativeBinding()
                        || !scannedWindowIds.contains(snapshot.getWindowId()))
                .map(WindowTaskSnapshot::getWindowId)
                .filter(windowId -> windowId != null && !windowId.isBlank())
                .distinct()
                .toList();
        if (!staleWindowIds.isEmpty()) {
            log.info("Prune idle stale window registrations: {}", staleWindowIds);
            windowTaskControlService.unregisterWindows(staleWindowIds);
        }
    }

    private String describeWindows(List<NativeWindowInfo> windows) {
        if (windows == null || windows.isEmpty()) {
            return "[]";
        }
        return windows.stream()
                .map(window -> window.toWindowId() + "|" + window.getTitle()
                        + "|pid=" + window.getProcessId()
                        + "|rect=" + window.getX() + "," + window.getY() + "," + window.getWidth() + "x" + window.getHeight())
                .toList()
                .toString();
    }
}
