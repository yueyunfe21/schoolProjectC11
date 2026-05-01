package com.bot.dhxy; // 注意包名

import com.bot.dhxy.config.VisionProvider;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.driver.AWTScreenCapture;
import com.bot.dhxy.core.ImageFinder;

public class ImageFinderTest {

    // ================= 配置区 =================
    private static final String SOURCE_IMAGE_PATH = "images/test_screen.png";
    private static final String DHXY_IMAGE_PATH = "images/dhxy_screen.png"; // 临时大图 (相机自动生成)
    private static final String TARGET_IMAGE_PATH = "images/dhxyjdb.png"; // 【修改这里】换成你要测试的特征小图
    private static final double SIMILARITY_THRESHOLD = 0.60;
    private static final String DHXY_COORDINATE_PATH = "images/dhxy_coor.png";// 匹配阈值
    // ===============================================================
    private static int diffX = 68;
    private static int diffY = 15;
    private static int offsetXY = 10;
    public static void main(String[] args) {
        System.out.println("--- 🧪 视觉神经实机测试启动 ---");



        // 1. 手动组装一台“相机” (因为不在 Spring 容器里，我们需要自己 new)
        VisionProvider camera = new AWTScreenCapture();

        System.out.println("📸 正在捕获当前屏幕...");
        // 2. 咔嚓！截取全屏并保存到 SOURCE_IMAGE_PATH
        boolean captured = camera.captureScreen(SOURCE_IMAGE_PATH);

        if (!captured) {
            System.err.println("❌ 截图失败，请检查权限或路径！");
            return;
        }



        System.out.println("⏳ 截图成功！正在画面中搜索目标: " + TARGET_IMAGE_PATH);

        // 3. 调用核心视觉库进行匹配
        double[] result = ImageFinder.find(SOURCE_IMAGE_PATH, TARGET_IMAGE_PATH, SIMILARITY_THRESHOLD);

        // 4. 解析并打印结果
        if (result != null) {
            int x = (int) result[0];
            int y = (int) result[1];
            double similarity = result[2];

            System.out.println("\n✅ 找到目标了！");
            System.out.printf("🎯 相似度: %.2f%%\n", similarity * 100);
            System.out.println("📍 目标中心点坐标: X=" + x + ", Y=" + y);
            System.out.println("💡 提示：你可以把这个坐标传给鼠标驱动去点击了！");

            int sSX = x - diffX;
            int sSy = y - diffY;

            int eSX = sSX + 1024 + offsetXY;
            int eSY = sSy + 768 + offsetXY;

            int c1 = 1500 - sSX;
            int c2 = 471 - sSy;

            int coorSX = sSX + 46;
            int coorSY = sSy + 59;

            int ecoorSX = coorSX+ 178;
            int ecoorSY = coorSY + 35;
            //46 59

            System.out.println("📍: X=" + coorSX + ", Y=" + coorSY);
            System.out.println("📍: X=" + ecoorSX + ", Y=" + ecoorSY);

            //1500 471 -> 1678 506

            camera.captureRegionToFile(DHXY_IMAGE_PATH, sSX, sSy, eSX, eSY);
            camera.captureRegionToFile(DHXY_COORDINATE_PATH, coorSX, coorSY, ecoorSX, ecoorSY);
            // 由于不在 Spring 启动环境里测试，我们手动 new 一个出来

        } else {
            System.out.println("\n⚠️ 没找到目标！");
            System.out.println("👉 可能原因：1. 画面里确实没有这个 UI； 2. 相似度低于你设定的阈值 (" + (SIMILARITY_THRESHOLD * 100) + "%)");
        }

        TextRecognizer ocr = new TextRecognizer();
        // 假设你用之前的局部截图工具，把左上角截下来存成了 coord.png
        TextRecognizer.LocationInfo loc = ocr.parseLocation(DHXY_COORDINATE_PATH);
        if (loc != null) {
            System.out.println(loc.toString());
        }

    }
}