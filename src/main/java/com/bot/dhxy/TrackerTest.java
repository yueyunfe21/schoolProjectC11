//package com.bot.dhxy;
//
//import com.bot.dhxy.config.VisionProvider;
//import com.bot.dhxy.core.GameClientTracker;
//import com.bot.dhxy.core.TextRecognizer;
//import com.bot.dhxy.driver.AWTScreenCapture;
//
//public class TrackerTest {
//
//    public static void main(String[] args) {
//        System.out.println("--- 🚀 全自动空间定位与视觉识别测试启动 ---");
//
//        // =======================================================
//        // 1. 手动组装微服务组件 (因为不在 Spring 启动环境里)
//        // =======================================================
//        VisionProvider camera = new AWTScreenCapture();
//        GameClientTracker tracker = new GameClientTracker(camera);
//        TextRecognizer ocr = new TextRecognizer(); // ⚠️ 跑之前确认里面填了百度云的 Key！
//
//        // =======================================================
//        // 2. 启动定位雷达，寻找游戏窗口
//        // =======================================================
//        boolean located = tracker.locateWindow();
//        if (!located) {
//            System.out.println("❌ 测试终止：无法找到游戏窗口，请把大话西游露出来！");
//            return;
//        }
//
//        // =======================================================
//        // 3. 测试你的“绝对坐标无脑截图”绝技
//        // =======================================================
//        // 假设你今天用工具抓到了坐标区域的绝对物理坐标是下面这四个数字：
//        int absX1 = 1802;
//        int absY1 = 712;
//        int absX2 = 1907;
//        int absY2 = 885;
//
//        String savePath = "images/test_dhxy_coor.png";
//
//        System.out.println("\n📸 正在发送绝对坐标给定位器，请求截图...");
//
//        // 见证奇迹的时刻：只传绝对坐标，它内部会自动算好相对坐标并完成截图
//        boolean captured = tracker.captureRegionByAbsoluteToFile(
//                "左上角地图坐标", savePath, absX1, absY1, absX2, absY2
//        );
//
//        if (!captured) {
//            System.out.println("❌ 截图失败，请检查路径权限！");
//            return;
//        }
//        System.out.println("✅ 完美截取！图片已保存至: " + savePath);
//
//        // =======================================================
//        // 4. 将截好的图送入 OCR 大脑
//        // =======================================================
//        System.out.println("\n🧠 正在呼叫百度云 OCR 识别坐标文字...");
//        TextRecognizer.LocationInfo loc = ocr.parseLocation(savePath);
//
//        System.out.println("\n=============================================");
//        if (loc != null) {
//            System.out.println("🎉 测试完美通关！最终识别结果：");
//            System.out.println("👉 " + loc.toString());
//        } else {
//            System.out.println("⚠️ OCR 没认出来！请打开 " + savePath + " 看看截到的图是不是对的。");
//        }
//        System.out.println("=============================================");
//    }
//}