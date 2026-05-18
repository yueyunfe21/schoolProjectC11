package com.bot.dhxy.window.discovery;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.control.WindowTaskCommandResult;
import com.bot.dhxy.window.control.WindowTaskControlService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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
        return nativeWindowScanner.scanGameWindows();
    }

    public WindowTaskCommandResult registerDetectedGameWindows(TaskType leaderTaskType) {
        List<NativeWindowInfo> windows = scanGameWindows();
        if (windows.isEmpty()) {
            return WindowTaskCommandResult.empty("没有扫描到游戏窗口", windowTaskControlService.getSnapshots());
        }
        List<WindowRegistrationRequest> requests = registrationMapper.toRegistrationRequests(windows, leaderTaskType);
        return windowTaskControlService.registerWindows(requests);
    }
}
