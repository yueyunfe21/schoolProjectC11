package com.bot.dhxy;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.service.AutoGridCalibrator;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.NavigationService;
import com.bot.dhxy.service.QuestManagerService;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.task.FiveRingTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.File;

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
    private final BagService bagService;
    private final QuestManagerService questManagerService;

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
            testTaskSelected();
            // ==========================================
            // 🎯 首领专属：洗图匹配沙盒测试专场！
            // ==========================================

            // ⚠️ 测试阶段，把主流程注释掉，专心看洗图结果
             //fiveRingTask.execute();

        } else {
            System.err.println("❌ 定位失败，请确认你的大话西游没被最小化，且 GAME_WINDOW_KEYWORD 填对了！");
        }
    }

    private void testDialog() {
        // ==========================================
        // 🧪 循环判定实验室：请在游戏里不断切换场景
        // ==========================================
        for (int i = 0; i < 50; i++) {
            log.info("------------------------------------------");
            log.info("🔍 [第 {} 次扫描] 正在探测 UI 状态...", i + 1);

            long start = System.currentTimeMillis();

            // 🌟 调用我们重构后的三道防线逻辑
            tracker.bringWindowToFront();
            DialogService.DialogType type = dialogService.detectDialogType();

            long cost = System.currentTimeMillis() - start;

            switch (type) {
                case OPTION:
                    log.info("🟢【判定结果】：OPTION (发现选项框) | 耗时: {}ms", cost);
                    break;
                case STORY:
                    log.info("⚪【判定结果】：STORY (发现纯剧情) | 耗时: {}ms", cost);
                    break;
                case NONE:
                    log.info("🌑【判定结果】：NONE (无对话框) | 耗时: {}ms", cost);
                    break;
            }

            log.info("💡 提示：此时可以打开 images/temp/ 观察 debug_thin_white_text.png 的过滤效果");
            try {
                Thread.sleep(5000); // 每2秒测一次，方便您跑位测试
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void testTaskSelected() {

        for (int i = 0; i < 1; i++) {
            log.info("------------------------------------------");
            log.info("🔍 [第 {} 次扫描] 正在探测 UI 状态...", i + 1);

            if(questManagerService.activateTaskIfPresent("qianling",true)) {
                log.info("found task: qianling");
            } else {
                log.info("not found task: qianling");
            }

        }

    }


}