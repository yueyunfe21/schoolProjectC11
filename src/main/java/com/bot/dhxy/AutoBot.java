package com.bot.dhxy;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.task.FiveRingTask;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@RequiredArgsConstructor // 🌟 Lombok 神器：自动生成包含所有 final 变量的依赖注入构造函数
public class AutoBot implements CommandLineRunner {

    private final FiveRingTask fiveRingTask;
    private final GameStateUtil brain;
    // 1. 把我们的雷达注入进来！
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final NavigationService navigationService;

    public static void main(String[] args) {
// 告诉 Spring Boot 关闭“无头模式”，允许调用真实的物理屏幕和键鼠！
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AutoBot.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Spring 容器装配完毕！准备测试底层的 Win32 窗口雷达...");
        Thread.sleep(1000);

        // ==========================================
        // 🎯 核心测试：直接调雷达定位！
        // ==========================================
        boolean success = tracker.locateWindow();

        if (success) {
            System.out.println("🎉 太棒了！Win32 API 成功抓到了大话西游的基址！");
            boolean ready = tracker.bringWindowToFront();
            if (!ready) {
                System.out.println("❌ 无法唤醒游戏，停止任务。");
                return;
            }

            //fiveRingTask.execute();

            // 同步身份和位置 (保留你之前的代码)
//            fiveRingTask.syncMyIdentity();
//            fiveRingTask.syncMyPosition();

            // ==========================================
            // 🏃‍♂️ 移动侦测专项测试 (循环监控)
            // ==========================================
            System.out.println("\n==========================================");
            System.out.println("🎬 准备就绪！请在游戏里让角色跑起来！");
            System.out.println("==========================================");

             //给自己留 2 秒钟时间去握住鼠标
            Thread.sleep(2000);


            //boolean isMoving = brain.isMovingByPixelDiff();


        } else {
            System.err.println("❌ 定位失败，请确认你的大话西游没被最小化，且 GAME_WINDOW_KEYWORD 填对了！");
        }
    }
}