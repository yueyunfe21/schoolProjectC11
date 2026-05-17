package com.bot.dhxy;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.driver.WinApiMouseController;
import com.bot.dhxy.service.*;
import com.bot.dhxy.service.AutoGridCalibrator;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.task.FiveRingTask;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.awt.image.BufferedImage;
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
    private final LocationVisionService locationVisionService;
    private final QuestManagerService questManagerService;
    private final BattleRadarService battleRadarService;

    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;
    private final WinApiMouseController winApiMouseController;
    private final PlayerStateService playerStateService;

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
            navigationService.ensureMapTrackingOption();
            if (!ready) {
                System.out.println("❌ 无法唤醒游戏，停止任务。");
                return;
            }
            // ==========================================
            // 🎯 首领专属：洗图匹配沙盒测试专场！
            // ==========================================

            // ⚠️ 测试阶段，把主流程注释掉，专心看洗图结果
            fiveRingTask.execute();

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

    public void testStoryDialog(){
        BufferedImage frame = ImagePreprocessor.pathToBufferedImage("images/temp/story_scan.png");
        if (frame == null) return;

        // 🌟 换用带有【腐蚀滤网】的测算器，彻底无视白衣服和雪地的干扰！
        int thinWhiteCount = ImagePreprocessor.countThinWhitePixelsHSV(frame);
        int greenCount = ImagePreprocessor.countGreenPixelsHSV(frame);
        frame.flush();

        int totalTextPixels = thinWhiteCount + greenCount;
        log.info("thinWhiteCount: {}", thinWhiteCount);
        log.info("greenCount: {}", greenCount);
        // 经过腐蚀过滤后，剩下的绝对是纯净的文字，不再有胖白色的干扰
        log.info("totalTextPixels: {}", totalTextPixels);
    }

    public void testbattle(){
        tracker.bringWindowToFront();
        tracker.updateGlobalVision();
        String path = "images/temp/latest_vision.png";
        BufferedImage frame = ImagePreprocessor.pathToBufferedImage(path);
        if (frame == null) return;

        // 🌟 换用带有【腐蚀滤网】的测算器，彻底无视白衣服和雪地的干扰！
        int greenCount = ImagePreprocessor.countThinWhitePixelsHSV(frame);
        frame.flush();


        log.info("greenCount: {}", greenCount);

    }

}