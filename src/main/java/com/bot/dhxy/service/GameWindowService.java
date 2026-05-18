package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游戏窗口初始化服务。
 *
 * 作用：统一负责定位游戏窗口、拉到前台、确认地图追踪设置。
 * 为什么加：AutoBot 自动启动任务和 UI 点击开始任务都需要同一套初始化逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameWindowService {

    private final GameClientTracker tracker;
    private final NavigationService navigationService;

    public boolean initGameWindow() {
        log.info("🎮 准备初始化游戏窗口...");
        sleep(1000);

        boolean success = tracker.locateWindow();
        if (!success) {
            log.error("❌ 定位失败，请确认大话西游没有被最小化，并且 GAME_WINDOW_KEYWORD 配置正确。");
            return false;
        }

        log.info("🎉 Win32 API 成功抓到大话西游窗口。");
        boolean ready = tracker.bringWindowToFront();
        if (!ready) {
            log.error("❌ 无法唤醒游戏窗口，停止任务。");
            return false;
        }

        navigationService.ensureMapTrackingOption();
        return true;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("游戏窗口初始化等待被中断", e);
        }
    }
}
