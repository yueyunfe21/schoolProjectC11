package com.bot.dhxy.core; // 根据你的实际路径修改

import com.bot.dhxy.runner.stop.TaskSleep;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

/**
 * 屏幕坐标嗅探器 (手搓版微信截图)
 * 用于辅助开发者用鼠标框选区域，并获取精准的 X, Y, Width, Height
 */
public class CoordinateSniffer {

    public static void main(String[] args) throws Exception {
        System.out.println("⏳ 3秒后将冻结屏幕，请切回游戏画面准备框选...");
        TaskSleep.sleep(3000);

        // 1. 咔嚓！截取全屏
        Robot robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        BufferedImage background = robot.createScreenCapture(new Rectangle(screenSize));

        // 2. 召唤全屏无边框画板
        SwingUtilities.invokeLater(() -> createAndShowGUI(background, screenSize));
    }

    private static void createAndShowGUI(BufferedImage background, Dimension screenSize) {
        JFrame frame = new JFrame();
        frame.setUndecorated(true); // 无边框
        frame.setSize(screenSize);
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 自定义画板
        JPanel panel = new JPanel() {
            Point startPoint = null;
            Point currentPoint = null;

            {
                // 添加鼠标点击监听
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        startPoint = e.getPoint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (startPoint != null && currentPoint != null) {
                            // 计算最终的矩形区域
                            int x = Math.min(startPoint.x, currentPoint.x);
                            int y = Math.min(startPoint.y, currentPoint.y);
                            int w = Math.abs(startPoint.x - currentPoint.x);
                            int h = Math.abs(startPoint.y - currentPoint.y);

                            System.out.println("\n✅ 框选成功！请将以下参数复制到代码中：");
                            System.out.println("----------------------------------------");
                            System.out.printf("X 坐标: %d\n", x);
                            System.out.printf("Y 坐标: %d\n", y);
                            System.out.printf("宽 (Width): %d\n", w);
                            System.out.printf("高 (Height): %d\n", h);
                            System.out.println("----------------------------------------");
                            System.out.println("调用示例: eyes.captureRegionInMemory(" + x + ", " + y + ", " + w + ", " + h + ");");

                            // 完成任务，销毁窗口结束程序
                            frame.dispose();
                            System.exit(0);
                        }
                    }
                });

                // 添加鼠标拖动监听
                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        currentPoint = e.getPoint();
                        repaint(); // 触发重绘，让你看到红框在动
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // 1. 先把截好的全屏大图画在最底层
                g2d.drawImage(background, 0, 0, null);

                // 2. 加上一层半透明的黑色遮罩，模仿截图工具的变暗效果
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // 3. 画你用鼠标拖出来的选择框
                if (startPoint != null && currentPoint != null) {
                    int x = Math.min(startPoint.x, currentPoint.x);
                    int y = Math.min(startPoint.y, currentPoint.y);
                    int w = Math.abs(startPoint.x - currentPoint.x);
                    int h = Math.abs(startPoint.y - currentPoint.y);

                    // 把框选区域的黑色遮罩“挖空”，露出原本明亮的画面
                    g2d.setClip(x, y, w, h);
                    g2d.drawImage(background, 0, 0, null);
                    g2d.setClip(null);

                    // 画一个显眼的红色边框
                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRect(x, y, w, h);
                }
            }
        };

        // 更换鼠标指针为“十字瞄准星”
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        frame.add(panel);
        frame.setVisible(true);
    }
}
