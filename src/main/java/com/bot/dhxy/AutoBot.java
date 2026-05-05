package com.bot.dhxy;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.service.AutoGridCalibrator;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.task.FiveRingTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class AutoBot implements CommandLineRunner {

    private final FiveRingTask fiveRingTask;
    private final GameStateUtil brain;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final NavigationService navigationService;
    private final DialogService dialogService;




    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AutoBot.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Spring 容器装配完毕！准备测试底层的 Win32 窗口雷达...");
        Thread.sleep(1000);

        boolean success = tracker.locateWindow();

        if (success) {
            System.out.println("🎉 太棒了！Win32 API 成功抓到了大话西游的基址！");
            boolean ready = tracker.bringWindowToFront();
            if (!ready) {
                System.out.println("❌ 无法唤醒游戏，停止任务。");
                return;
            }
            //log.info(dialogService.hasStoryDialog() ? "found dialog" : "not found");
            // ==========================================
            // 🎯 雷达测绘专场：暂时屏蔽业务逻辑
            // ==========================================



            // 🌟 4. 把做任务的代码注释掉，专心跑测绘
            fiveRingTask.execute();

        } else {
            System.err.println("❌ 定位失败，请确认你的大话西游没被最小化，且 GAME_WINDOW_KEYWORD 填对了！");
        }
    }
}