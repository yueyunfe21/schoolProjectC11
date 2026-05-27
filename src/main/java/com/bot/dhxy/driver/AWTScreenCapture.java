package com.bot.dhxy.driver;

import com.bot.dhxy.config.VisionProvider;
import org.springframework.stereotype.Component;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

@Component
public class AWTScreenCapture implements VisionProvider {

    @Override
    public boolean captureScreen(String savePath) {
        try {
            // 1. 初始化底层硬件接口 (相当于召唤一个虚拟的鼠标/键盘/显示器管家)
            Robot robot = new Robot();

            // 2. 自动获取当前屏幕的实际分辨率
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            // 3. 瞬间捕捉屏幕画面，存在内存里
            BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

            // 4. 智能处理文件路径与目录 (如果没有 images 文件夹，这里会自动建好)
            File outputFile = new File(savePath);
            File folder = outputFile.getParentFile();
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }

            // 5. 将内存里的画面写到硬盘上，格式为 PNG
            return ImageIO.write(screenFullImage, "png", outputFile);

        } catch (Exception e) {
            System.err.println("❌ 底层截图模块发生硬件级错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 【新增实现】：极其轻量级的内存截图
    @Override
    public BufferedImage captureRegionInMemory(int x, int y, int width, int height) {
        try {
            Robot robot = new Robot();
            Rectangle region = new Rectangle(x, y, width, height);
            // 咔嚓！直接返回内存中的图像
            return robot.createScreenCapture(region);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ... (保留之前的 captureScreen 方法) ...

    @Override
    public BufferedImage captureRegionByCoordinates(int x1, int y1, int x2, int y2) {
        try {
            Robot robot = new Robot();

            // 工业级防御：自动计算真正的左上角起点，以防坐标传反
            int startX = Math.min(x1, x2);
            int startY = Math.min(y1, y2);

            // 计算截图区域的真正宽度和高度
            int width = Math.abs(x2 - x1);
            int height = Math.abs(y2 - y1);

            // 构造截图矩形
            Rectangle region = new Rectangle(startX, startY, width, height);

            // 瞬间返回内存图片
            return robot.createScreenCapture(region);

        } catch (Exception e) {
            System.err.println("❌ 局部截图失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean captureRegionToFile(String savePath, int x1, int y1, int x2, int y2) {
        try {
            Robot robot = new Robot();

            // 1. 自动计算矩形的起点和长宽 (增加健壮性)
            int startX = Math.min(x1, x2);
            int startY = Math.min(y1, y2);
            int width = Math.abs(x2 - x1);
            int height = Math.abs(y2 - y1);

            // 2. 局部捕捉
            Rectangle region = new Rectangle(startX, startY, width, height);
            BufferedImage img = robot.createScreenCapture(region);

            // 3. 智能处理目录
            File outputFile = new File(savePath);
            File folder = outputFile.getParentFile();
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }

            // 4. 写入硬盘
            return ImageIO.write(img, "png", outputFile);

        } catch (Exception e) {
            System.err.println("❌ 局部截图存盘失败: " + e.getMessage());
            return false;
        }
    }
}
